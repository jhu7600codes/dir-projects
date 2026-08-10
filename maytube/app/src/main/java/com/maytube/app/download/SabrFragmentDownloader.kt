package com.maytube.app.download

import android.content.Context
import android.webkit.CookieManager
import com.maytube.app.data.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads a video by pulling the same 5-second SABR fragments the live
 * WebView player consumes from `/sabr_playback` (see MobileInjector's
 * kdoc / html5-player.js's `requestSabr`), fetched with our own
 * concurrency, then remuxed on-device with Mp4TrackMuxer.
 *
 * This exists because the "obvious" approach -- yt2009's /exp_hd and
 * /get_480 endpoints -- makes the *server* rebuild the entire file with
 * ffmpeg, one 5-second window at a time with a hardcoded 150ms pause
 * between each, and only responds once that whole serial process is
 * finished (back/yt2009sabr.js "download", back/yt2009utils.js
 * saveMp4_android). For a 30 minute video that is hundreds of sequential
 * round trips to Google's video CDN before a single byte reaches the
 * device, with zero progress feedback in between. Pulling fragments
 * ourselves lets us parallelize that fetch and show real progress, which
 * is the whole point of doing this instead of just pointing
 * DownloadManager at /exp_hd.
 */
class SabrFragmentDownloader(private val context: Context) {

    data class Progress(val fetchedMs: Long, val totalMs: Long?)

    class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun download(
        config: ServerConfig,
        videoId: String,
        concurrency: Int = 4,
        onProgress: (Progress) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val session = resolveSession(config, videoId)

        val cacheDir = File(context.cacheDir, "maytube_sabr_dl").apply { mkdirs() }
        val videoTrackFile = File(cacheDir, "$videoId-video.tmp.mp4")
        val audioTrackFile = File(cacheDir, "$videoId-audio.tmp.mp4")
        val outputDir = VideoDownloader.downloadsDir(context).apply { mkdirs() }
        val outputFile = File(outputDir, "$videoId.mp4")

        try {
            BufferedOutputStream(FileOutputStream(videoTrackFile)).use { videoOut ->
                BufferedOutputStream(FileOutputStream(audioTrackFile)).use { audioOut ->
                    fetchAllFragments(config, session, concurrency, videoOut, audioOut, onProgress)
                }
            }

            if (videoTrackFile.length() == 0L || audioTrackFile.length() == 0L) {
                throw DownloadException("no video/audio data was returned for this video")
            }

            Mp4TrackMuxer.mux(videoTrackFile, audioTrackFile, outputFile)
            outputFile
        } finally {
            videoTrackFile.delete()
            audioTrackFile.delete()
        }
    }

    // -- session resolution -------------------------------------------------

    private data class Session(val sabrPath: String, val totalMs: Long?)

    /**
     * yt2009's watch page embeds the SABR session inline as plain JS
     * (back/yt2009html.js: `var sabrBase = "/sabr_playback?pid=...";`) when
     * SABR is enabled for the request. We force that on for this one
     * request regardless of the user's live-playback SABR setting --
     * downloading and live playback are independent choices.
     */
    private fun resolveSession(config: ServerConfig, videoId: String): Session {
        val url = "${config.baseUrl}/watch?v=$videoId"
        val request = Request.Builder()
            .url(url)
            .header("Cookie", cookieHeader(config))
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw DownloadException("could not open the watch page (HTTP ${response.code})")
            }
            response.body?.string().orEmpty()
        }

        val sabrPath = Regex("""var sabrBase = "(/sabr_playback\?pid=[^"]+)"""")
            .find(html)?.groupValues?.get(1)
            ?: throw DownloadException(
                "this instance didn't return a SABR session for this video " +
                    "(is exp_sabr available / is this actually a yt2009 watch page?)"
            )

        // back/yt2009html.js only takes the SABR branch's "initAsSabr()" path
        // for non-live videos; live videos get "initLiveChat"/"initAsLive()"
        // instead and never get a fixed duration, which this downloader
        // (and the underlying SABR session) isn't meant to handle.
        if (html.contains("initAsLive()")) {
            throw DownloadException("this is a live stream, which can't be downloaded")
        }

        // yt2009utils.seconds_to_time formats duration as [H:]M:SS and the
        // page renders it as "0:00 / <duration>" next to the player
        val totalMs = Regex("""0:00\s*/\s*(\d+(?::\d{2}){1,2})""").find(html)?.groupValues?.get(1)
            ?.let { parseClock(it) }

        return Session(sabrPath, totalMs)
    }

    private fun parseClock(clock: String): Long? {
        val parts = clock.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return null
        var seconds = 0L
        for (p in parts) seconds = seconds * 60 + p
        return seconds * 1000
    }

    // -- fragment fetching ---------------------------------------------------

    private suspend fun fetchAllFragments(
        config: ServerConfig,
        session: Session,
        concurrency: Int,
        videoOut: BufferedOutputStream,
        audioOut: BufferedOutputStream,
        onProgress: (Progress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val stepMs = 5000L
        var nextOffset = 0L
        var fetchedMs = 0L
        var reachedEnd = false
        val hardCapMs = 6L * 60 * 60 * 1000 // 6 hours safety valve if duration couldn't be parsed

        while (!reachedEnd) {
            val batchOffsets = (0 until concurrency)
                .map { nextOffset + it * stepMs }
                .filter { session.totalMs == null || it < session.totalMs + stepMs }
                .filter { it < hardCapMs }

            if (batchOffsets.isEmpty()) break

            val results = batchOffsets
                .map { offset -> async { offset to fetchFragment(config, session.sabrPath, offset) } }
                .awaitAll()
                .sortedBy { it.first }

            var batchHadAnyData = false
            for ((offset, parts) in results) {
                if (parts.isNotEmpty()) batchHadAnyData = true
                for (part in parts) {
                    if (part.isAudio) audioOut.write(part.data) else videoOut.write(part.data)
                }
                fetchedMs = maxOf(fetchedMs, offset + stepMs)
            }

            onProgress(Progress(fetchedMs, session.totalMs))

            nextOffset += batchOffsets.size * stepMs

            reachedEnd = when {
                session.totalMs != null -> nextOffset >= session.totalMs
                else -> !batchHadAnyData // no known duration: stop once a whole batch is empty
            }
        }
    }

    private fun fetchFragment(config: ServerConfig, sabrPath: String, offsetMs: Long): List<SabrFragmentParser.Part> {
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val forceReplayer = if (attempt > 0) "&force_replayer=1" else ""
                val url = "${config.baseUrl}$sabrPath&offset=$offsetMs&hd=1$forceReplayer"
                val request = Request.Builder()
                    .url(url)
                    .header("Cookie", cookieHeader(config))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw DownloadException("HTTP ${response.code} fetching offset ${offsetMs}ms")
                    }
                    val partCount = response.header("x-part-count")?.toIntOrNull() ?: 0
                    val body = response.body?.bytes() ?: ByteArray(0)
                    return SabrFragmentParser.parse(body, partCount)
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw DownloadException("giving up on offset ${offsetMs}ms after 3 attempts", lastError)
    }

    private fun cookieHeader(config: ServerConfig): String {
        val existing = CookieManager.getInstance().getCookie(config.baseUrl)
        // make sure exp_sabr is present regardless of the user's live
        // playback preference -- see class kdoc
        return if (existing.isNullOrBlank()) {
            "maytube_dl_flags=exp_sabr"
        } else {
            "$existing; maytube_dl_flags=exp_sabr"
        }
    }
}

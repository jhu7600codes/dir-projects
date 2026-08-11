package com.maytube.app.download

import android.content.Context
import com.maytube.app.data.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
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
 *
 * Session resolution and per-fragment fetching are shared with
 * com.maytube.app.player.StreamingPlayer (see SabrSession/SabrFragmentFetcher)
 * -- this class's own job is purely buffer-the-whole-thing-then-mux, unlike
 * the streaming player which feeds a player as fragments arrive.
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
        val session = try {
            SabrSession.resolve(client, config, videoId)
        } catch (e: SabrSession.ResolveException) {
            throw DownloadException(e.message ?: "could not resolve SABR session", e)
        }

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

    // -- fragment fetching ---------------------------------------------------

    private suspend fun fetchAllFragments(
        config: ServerConfig,
        session: SabrSession.Session,
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
        return try {
            SabrFragmentFetcher.fetch(client, config, sabrPath, offsetMs)
        } catch (e: SabrFragmentFetcher.FetchException) {
            throw DownloadException(e.message ?: "fragment fetch failed", e)
        }
    }
}

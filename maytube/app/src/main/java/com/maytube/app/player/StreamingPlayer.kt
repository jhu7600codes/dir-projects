package com.maytube.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.maytube.app.data.ServerConfig
import com.maytube.app.download.SabrFragmentFetcher
import com.maytube.app.download.SabrSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * True live-streaming playback for the native player: feeds ExoPlayer from
 * the same SABR fragments MaytubeWebViewClient/SabrFragmentDownloader
 * consume, writing each one to a local file the instant it arrives and
 * letting ExoPlayer read that file *while it's still being written* (see
 * [GrowingFileDataSource]) -- unlike the original VideoView-based
 * PlayerActivity, playback can start as soon as the first fragment lands
 * instead of waiting for the whole video to download and get muxed.
 *
 * No Mp4TrackMuxer step here at all: SABR delivers real fragmented MP4
 * (CMAF-style init segment + moof/mdat media segments, the same format
 * DASH/HLS use for exactly this kind of progressive delivery), so the two
 * track dumps are independently streamable by ExoPlayer's own MP4
 * extractor without remuxing -- muxing was only ever needed for
 * VideoView/MediaPlayer, which requires a single finished, seekable
 * container. Video-only and audio-only tracks are still separate SABR
 * streams (same as the downloader), so ExoPlayer plays them as two
 * ProgressiveMediaSources combined with MergingMediaSource, synced by
 * presentation timestamp the same way it'd sync any two-track DASH
 * manifest.
 */
@UnstableApi
class StreamingPlayer(context: Context) {

    class FetchState(val videoFile: File, val audioFile: File) {
        @Volatile var videoBytesWritten = 0L
        @Volatile var audioBytesWritten = 0L
        @Volatile var videoDone = false
        @Volatile var audioDone = false
        @Volatile var error: Throwable? = null
    }

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var fetchJob: Job? = null
    var state: FetchState? = null
        private set

    /** The itag [start] was last called with, if any -- null means "server default." */
    var currentItag: Int? = null
        private set

    /**
     * @param itag Pins a specific quality (see SabrSession.QualityOption /
     *   [SabrFragmentFetcher]'s `user_video_itag` param) instead of
     *   whatever yt2009 picks by default. Available options for the
     *   current video arrive via [onQualities] once the session resolves
     *   -- pass one of their itags back in on a later [start] call to
     *   switch. Switching always restarts from the beginning: SABR
     *   fragments only carry a full MP4 init segment (ftyp/moov) on the
     *   very first one, and there's no way to verify without a real
     *   device that requesting a later offset on its own still includes
     *   it, so resuming mid-video on a quality switch is deliberately not
     *   attempted here.
     */
    fun start(
        config: ServerConfig,
        videoId: String,
        itag: Int? = null,
        onProgress: (fetchedMs: Long, totalMs: Long?) -> Unit = { _, _ -> },
        onQualities: (List<SabrSession.QualityOption>) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        stop()
        currentItag = itag

        val cacheDir = File(appContext.cacheDir, "maytube_stream").apply { mkdirs() }
        val videoFile = File(cacheDir, "$videoId-video.stream.mp4")
        val audioFile = File(cacheDir, "$videoId-audio.stream.mp4")
        videoFile.delete()
        audioFile.delete()
        videoFile.createNewFile()
        audioFile.createNewFile()

        val fetchState = FetchState(videoFile, audioFile)
        state = fetchState

        val videoSource = ProgressiveMediaSource.Factory(GrowingFileDataSource.Factory(fetchState, isAudio = false))
            .createMediaSource(MediaItem.fromUri(Uri.parse("maytube-stream://$videoId/video")))
        val audioSource = ProgressiveMediaSource.Factory(GrowingFileDataSource.Factory(fetchState, isAudio = true))
            .createMediaSource(MediaItem.fromUri(Uri.parse("maytube-stream://$videoId/audio")))

        player.setMediaSource(MergingMediaSource(videoSource, audioSource))
        player.prepare()
        player.playWhenReady = true

        fetchJob = scope.launch {
            try {
                val session = SabrSession.resolve(client, config, videoId)
                withContext(Dispatchers.Main) { onQualities(session.qualities) }
                fetchAllFragments(config, session, itag, fetchState, onProgress)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fetchState.error = e
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    /**
     * Same batched-concurrent fetch shape as SabrFragmentDownloader (see
     * its kdoc for why: parallelizing beats yt2009's own serial
     * server-side rebuild), except every fragment is flushed to disk and
     * [FetchState]'s counters updated immediately, rather than only at the
     * very end -- that's the entire difference between "buffer then play"
     * and "play while buffering."
     */
    private suspend fun fetchAllFragments(
        config: ServerConfig,
        session: SabrSession.Session,
        itag: Int?,
        fetchState: FetchState,
        onProgress: (fetchedMs: Long, totalMs: Long?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val stepMs = 5000L
        val concurrency = 3
        var nextOffset = 0L
        var fetchedMs = 0L
        var reachedEnd = false
        val hardCapMs = 6L * 60 * 60 * 1000

        FileOutputStream(fetchState.videoFile).use { videoOut ->
            FileOutputStream(fetchState.audioFile).use { audioOut ->
                while (!reachedEnd) {
                    currentCoroutineContext().ensureActive()

                    val batchOffsets = (0 until concurrency)
                        .map { nextOffset + it * stepMs }
                        .filter { session.totalMs == null || it < session.totalMs + stepMs }
                        .filter { it < hardCapMs }
                    if (batchOffsets.isEmpty()) break

                    val results = batchOffsets
                        .map { offset -> async { offset to SabrFragmentFetcher.fetch(client, config, session.sabrPath, offset, itag) } }
                        .awaitAll()
                        .sortedBy { it.first }

                    var batchHadAnyData = false
                    for ((offset, parts) in results) {
                        if (parts.isNotEmpty()) batchHadAnyData = true
                        for (part in parts) {
                            if (part.isAudio) {
                                audioOut.write(part.data)
                                audioOut.flush()
                                fetchState.audioBytesWritten = fetchState.audioFile.length()
                            } else {
                                videoOut.write(part.data)
                                videoOut.flush()
                                fetchState.videoBytesWritten = fetchState.videoFile.length()
                            }
                        }
                        fetchedMs = maxOf(fetchedMs, offset + stepMs)
                    }

                    withContext(Dispatchers.Main) { onProgress(fetchedMs, session.totalMs) }
                    nextOffset += batchOffsets.size * stepMs
                    reachedEnd = when {
                        session.totalMs != null -> nextOffset >= session.totalMs
                        else -> !batchHadAnyData
                    }
                }
            }
        }
        fetchState.videoDone = true
        fetchState.audioDone = true
    }

    /** Stops playback/fetching and drops the current session's temp files, if any. */
    fun stop() {
        fetchJob?.cancel()
        fetchJob = null
        player.stop()
        player.clearMediaItems()
        state?.let {
            it.videoFile.delete()
            it.audioFile.delete()
        }
        state = null
    }

    fun release() {
        stop()
        scope.cancel()
        player.release()
    }
}

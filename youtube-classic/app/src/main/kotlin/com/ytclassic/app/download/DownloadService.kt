package com.ytclassic.app.download

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ytclassic.app.R
import com.ytclassic.app.YtClassicApp
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Foreground service that downloads a video for offline playback. YouTube
 * serves video and audio as separate adaptive streams for anything above
 * the lowest qualities, so this pulls both to temp files and muxes them
 * with [Mp4TrackMuxer]; a plain progressive (video+audio-in-one) stream is
 * just copied straight through.
 */
class DownloadService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val client = OkHttpClient.Builder().build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = intent?.toDownloadRequest() ?: return START_NOT_STICKY

        startForeground(NOTIFICATION_ID_BASE + startId, buildNotification(request.title, 0))
        scope.launch { runDownload(request, startId) }
        return START_REDELIVER_INTENT
    }

    private data class DownloadRequest(
        val videoId: String,
        val title: String,
        val thumbnailUrl: String?,
        val videoUrl: String?,
        val audioUrl: String?,
        val progressiveUrl: String?,
    )

    private fun Intent.toDownloadRequest(): DownloadRequest? {
        val videoId = getStringExtra(EXTRA_VIDEO_ID) ?: return null
        return DownloadRequest(
            videoId = videoId,
            title = getStringExtra(EXTRA_TITLE) ?: videoId,
            thumbnailUrl = getStringExtra(EXTRA_THUMBNAIL),
            videoUrl = getStringExtra(EXTRA_VIDEO_URL),
            audioUrl = getStringExtra(EXTRA_AUDIO_URL),
            progressiveUrl = getStringExtra(EXTRA_PROGRESSIVE_URL),
        )
    }

    private suspend fun runDownload(request: DownloadRequest, startId: Int) {
        DownloadsStore.upsert(
            DownloadEntry(request.videoId, request.title, request.thumbnailUrl, DownloadStatus.DOWNLOADING),
        )

        val downloadsDir = File(getExternalFilesDir(null), "downloads").apply { mkdirs() }
        val outputFile = File(downloadsDir, "${request.videoId}.mp4")
        val tempDir = File(cacheDir, "downloads_tmp").apply { mkdirs() }

        try {
            if (request.progressiveUrl != null) {
                downloadTo(request.progressiveUrl, outputFile, startId, request.title) { percent ->
                    reportProgress(request, percent)
                }
            } else {
                val videoTemp = File(tempDir, "${request.videoId}_video.tmp")
                val audioTemp = File(tempDir, "${request.videoId}_audio.tmp")
                val videoUrl = request.videoUrl
                    ?: throw IOException("no video stream available")
                val audioUrl = request.audioUrl
                    ?: throw IOException("no audio stream available")

                downloadTo(videoUrl, videoTemp, startId, request.title) { percent ->
                    reportProgress(request, percent / 2)
                }
                downloadTo(audioUrl, audioTemp, startId, request.title) { percent ->
                    reportProgress(request, 50 + percent / 2)
                }

                DownloadsStore.upsert(
                    DownloadEntry(request.videoId, request.title, request.thumbnailUrl, DownloadStatus.MUXING, 100),
                )
                Mp4TrackMuxer.mux(videoTemp, audioTemp, outputFile)
                videoTemp.delete()
                audioTemp.delete()
            }

            DownloadsStore.upsert(
                DownloadEntry(
                    videoId = request.videoId,
                    title = request.title,
                    thumbnailUrl = request.thumbnailUrl,
                    status = DownloadStatus.COMPLETE,
                    progressPercent = 100,
                    filePath = outputFile.absolutePath,
                    fileSizeBytes = outputFile.length(),
                ),
            )
        } catch (e: Exception) {
            outputFile.delete()
            DownloadsStore.upsert(
                DownloadEntry(request.videoId, request.title, request.thumbnailUrl, DownloadStatus.FAILED),
            )
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
    }

    private fun reportProgress(request: DownloadRequest, percent: Int) {
        DownloadsStore.upsert(
            DownloadEntry(request.videoId, request.title, request.thumbnailUrl, DownloadStatus.DOWNLOADING, percent),
        )
    }

    private fun downloadTo(url: String, target: File, startId: Int, title: String, onProgress: (Int) -> Unit) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} downloading $url")
            val body = response.body ?: throw IOException("empty body downloading $url")
            val total = body.contentLength()
            var read = 0L
            var lastReportedPercent = -1

            target.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = ((read * 100) / total).toInt()
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                onProgress(percent)
                                updateNotification(startId, title, percent)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateNotification(startId: Int, title: String, percent: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + startId, buildNotification(title, percent))
    }

    private fun buildNotification(title: String, percent: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName) ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, YtClassicApp.CHANNEL_DOWNLOADS)
            .setContentTitle(title)
            .setContentText(getString(R.string.downloading))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, percent <= 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID_BASE = 5000
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_THUMBNAIL = "thumbnail"
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_AUDIO_URL = "audio_url"
        const val EXTRA_PROGRESSIVE_URL = "progressive_url"
    }
}

package com.maytube.app.download

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.webkit.CookieManager
import androidx.core.net.toUri
import com.maytube.app.data.ServerConfig
import java.io.File

/**
 * "Downloads" a video for offline playback.
 *
 * yt2009 doesn't expose a dedicated download endpoint, but /exp_hd and
 * /get_480 (back/backend.js) do the same server-side SABR/DASH
 * reconstruction the live player uses (yt2009_utils.saveMp4_android, see
 * back/yt2009sabr.js) and then 302-redirect to a plain static
 * /assets/<id>-<quality>.mp4 file once it's ready. That's a normal
 * progressive MP4 -- exactly what DownloadManager (and any local video
 * player) needs, unlike the fragmented-MSE stream the live player consumes.
 *
 * DownloadManager follows the redirect itself, so we just point it at the
 * resolver endpoint and let the server do the work; this call can take a
 * while to respond (the file is being built server-side) which is fine,
 * DownloadManager just waits it out in the background.
 */
object VideoDownloader {

    sealed class Result {
        data class Started(val downloadId: Long, val fileName: String) : Result()
        data class Error(val message: String) : Result()
    }

    fun downloadsDir(context: Context): File =
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir

    fun resolverUrl(config: ServerConfig, videoId: String): String {
        val quality = if (config.prefer1080p) "?video_id=$videoId&fhd=1" else "?video_id=$videoId"
        return "${config.baseUrl}/exp_hd$quality"
    }

    fun startDownload(context: Context, config: ServerConfig, videoId: String, title: String?): Result {
        return try {
            val url = resolverUrl(config, videoId)
            val request = DownloadManager.Request(url.toUri())

            val cookies = CookieManager.getInstance().getCookie(config.baseUrl)
            if (!cookies.isNullOrBlank()) {
                request.addRequestHeader("Cookie", cookies)
            }

            val safeTitle = (title?.takeIf { it.isNotBlank() } ?: videoId)
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(60)
            val fileName = "$videoId.mp4"

            request.setTitle(safeTitle)
            request.setDescription("Downloading from ${config.hostAndPort}")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setMimeType("video/mp4")
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            Result.Started(id, fileName)
        } catch (e: Exception) {
            Result.Error(e.message ?: "download failed")
        }
    }

    fun listDownloaded(context: Context): List<File> {
        val dir = downloadsDir(context)
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}

package com.maytube.app.download

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.webkit.CookieManager
import androidx.core.net.toUri
import com.maytube.app.data.ServerConfig
import java.io.File

/**
 * Downloads a video for offline playback.
 *
 * The preferred path is [SabrFragmentDownloader]: it pulls the same
 * fragments the live WebView player consumes, in parallel, and remuxes
 * them on-device -- fast, and with real progress. If that fails for any
 * reason (older/customized instance, network hiccup mid-download, etc.)
 * this falls back to yt2009's /exp_hd or /get_480 resolver endpoints,
 * which redirect to a plain static MP4 once the *server* has finished
 * rebuilding it -- much simpler, but serial and silent while it works, see
 * SabrFragmentDownloader's kdoc for why that's not the default anymore.
 */
object VideoDownloader {

    sealed class Result {
        data class Progress(val fetchedMs: Long, val totalMs: Long?) : Result()
        data class Completed(val file: File) : Result()
        data class FallbackStarted(val downloadId: Long, val fileName: String) : Result()
        data class Error(val message: String, val cause: Throwable? = null) : Result()
    }

    fun downloadsDir(context: Context): File =
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir

    /**
     * Runs the fast fragment-based download; falls back to the legacy
     * resolver-endpoint approach on failure. Must be called from a
     * coroutine -- this suspends for the whole download.
     */
    suspend fun download(
        context: Context,
        config: ServerConfig,
        videoId: String,
        title: String?,
        onProgress: (Result.Progress) -> Unit
    ): Result {
        return try {
            val file = SabrFragmentDownloader(context).download(config, videoId) { p ->
                onProgress(Result.Progress(p.fetchedMs, p.totalMs))
            }
            Result.Completed(file)
        } catch (fastPathError: Exception) {
            when (val fallback = startLegacyDownload(context, config, videoId, title)) {
                is LegacyResult.Started -> Result.FallbackStarted(fallback.downloadId, fallback.fileName)
                is LegacyResult.Error -> Result.Error(
                    "fast download failed (${fastPathError.message}); fallback also failed (${fallback.message})",
                    fastPathError
                )
            }
        }
    }

    fun listDownloaded(context: Context): List<File> {
        val dir = downloadsDir(context)
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    // -- legacy fallback: /exp_hd + /get_480, server rebuilds then redirects --

    private sealed class LegacyResult {
        data class Started(val downloadId: Long, val fileName: String) : LegacyResult()
        data class Error(val message: String) : LegacyResult()
    }

    private fun resolverUrl(config: ServerConfig, videoId: String): String {
        val quality = if (config.prefer1080p) "?video_id=$videoId&fhd=1" else "?video_id=$videoId"
        return "${config.baseUrl}/exp_hd$quality"
    }

    private fun startLegacyDownload(
        context: Context,
        config: ServerConfig,
        videoId: String,
        title: String?
    ): LegacyResult {
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
            request.setDescription("Downloading from ${config.hostAndPort} (slow fallback)")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setMimeType("video/mp4")
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            LegacyResult.Started(id, fileName)
        } catch (e: Exception) {
            LegacyResult.Error(e.message ?: "download failed")
        }
    }
}

package com.maytube.app.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.maytube.app.R
import com.maytube.app.data.ServerConfig
import com.maytube.app.download.VideoDownloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * maytube's own player, offered as an alternative to WebView-embedded
 * playback (Settings > native player). Exists because
 * MaytubeWebChromeClient's HTML5-fullscreen path -- swapping in Chromium's
 * own custom view via onShowCustomView/onHideCustomView -- turned out to be
 * a genuinely fragile piece of API surface on real devices: fighting sizing
 * races across orientation changes (see the "pillarboxed" fix) and a
 * teardown race that could wedge Chromium's fullscreen state entirely (see
 * the "doesn't even fullscreen anymore" fix). This sidesteps all of that by
 * not using WebView/Chromium fullscreen at all -- just a plain
 * android.widget.VideoView, whose "fit the frame, preserve aspect ratio,
 * center it, never crop/zoom" behavior is exactly what was asked for, and
 * is a decade-old, well-understood part of the platform rather than a
 * Chromium implementation detail this app has no control over.
 *
 * The tradeoff: VideoView needs a real, fully-muxed local MP4 to play --
 * unlike the WebView path, where html5-player.js feeds MSE incrementally as
 * SABR fragments arrive, there's no equivalent incremental path here
 * without writing a custom fragmented-MP4 muxer or a MediaCodec-driven
 * player. So this reuses [VideoDownloader]'s existing SABR fragment
 * pipeline exactly as-is (same fast parallel fetch the "Download this
 * video" action uses) and waits for the whole video before playback can
 * start -- buffer-then-play, not live streaming. If the video was already
 * downloaded, playback starts immediately with no re-fetch.
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var titleView: TextView
    private lateinit var bufferingOverlay: android.view.View
    private lateinit var bufferingText: TextView
    private lateinit var bufferingSpinner: ProgressBar
    private lateinit var errorOverlay: android.view.View
    private lateinit var errorText: TextView
    private lateinit var fullscreenButton: ImageButton

    private var fetchJob: Job? = null
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        videoView = findViewById(R.id.playerVideoView)
        titleView = findViewById(R.id.playerTitle)
        bufferingOverlay = findViewById(R.id.playerBufferingOverlay)
        bufferingText = findViewById(R.id.playerBufferingText)
        bufferingSpinner = findViewById(R.id.playerBufferingSpinner)
        errorOverlay = findViewById(R.id.playerErrorOverlay)
        errorText = findViewById(R.id.playerErrorText)
        fullscreenButton = findViewById(R.id.playerFullscreenButton)

        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() }
        titleView.text = title ?: intent.getStringExtra(EXTRA_VIDEO_ID)

        findViewById<ImageButton>(R.id.playerCloseButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.playerCancelButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.playerRetryButton).setOnClickListener { startPlayback() }
        fullscreenButton.setOnClickListener { toggleFullscreen() }

        videoView.setOnErrorListener { _, what, extra ->
            showError(getString(R.string.player_error, "playback error ($what/$extra)"))
            true
        }
        videoView.setMediaController(MediaController(this).also { it.setAnchorView(videoView) })

        startPlayback()
    }

    override fun onDestroy() {
        fetchJob?.cancel()
        super.onDestroy()
    }

    private fun startPlayback() {
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        val host = intent.getStringExtra(EXTRA_HOST)
        val port = intent.getIntExtra(EXTRA_PORT, -1)
        if (videoId == null || host == null || port <= 0) {
            showError(getString(R.string.player_error, "missing video/server info"))
            return
        }
        val config = ServerConfig(host = host, port = port, useHttps = intent.getBooleanExtra(EXTRA_HTTPS, false))

        errorOverlay.visibility = android.view.View.GONE

        // already downloaded (either from a previous native-player watch, or
        // the "Download this video" action) -- play the cached file directly,
        // no need to fetch it all over again
        val existing = File(VideoDownloader.downloadsDir(this), "$videoId.mp4")
        if (existing.exists() && existing.length() > 0) {
            play(existing)
            return
        }

        bufferingOverlay.visibility = android.view.View.VISIBLE
        bufferingText.text = getString(R.string.player_buffering, titleView.text)
        bufferingSpinner.isIndeterminate = true

        fetchJob = lifecycleScope.launch {
            val result = VideoDownloader.download(this@PlayerActivity, config, videoId, titleView.text?.toString()) { progress ->
                runOnUiThread {
                    val total = progress.totalMs
                    if (total != null && total > 0) {
                        bufferingSpinner.isIndeterminate = false
                        bufferingSpinner.progress =
                            ((progress.fetchedMs.toFloat() / total) * 1000).toInt().coerceIn(0, 1000)
                        bufferingText.text = getString(
                            R.string.player_buffering_progress, formatMs(progress.fetchedMs), formatMs(total)
                        )
                    } else {
                        bufferingText.text = getString(R.string.player_buffering_progress_unknown, formatMs(progress.fetchedMs))
                    }
                }
            }
            when (result) {
                is VideoDownloader.Result.Completed -> play(result.file)
                is VideoDownloader.Result.FallbackStarted ->
                    // the slow, serial, server-side-rebuild fallback -- no
                    // local file to play yet, it lands in the system
                    // Download Manager on its own schedule. Nothing this
                    // screen can play right now.
                    showError(getString(R.string.player_error, "this instance needs the slower built-in download; check Downloads once \"${result.fileName}\" finishes"))
                is VideoDownloader.Result.Error -> showError(getString(R.string.player_error, result.message))
                is VideoDownloader.Result.Progress -> Unit // only ever surfaced via onProgress above
            }
        }
    }

    private fun play(file: File) {
        bufferingOverlay.visibility = android.view.View.GONE
        videoView.setVideoURI(Uri.fromFile(file))
        videoView.setOnPreparedListener { it.isLooping = false; videoView.start() }
        videoView.start()
    }

    private fun showError(message: String) {
        bufferingOverlay.visibility = android.view.View.GONE
        errorOverlay.visibility = android.view.View.VISIBLE
        errorText.text = message
    }

    private fun formatMs(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    /**
     * Plain Android orientation + system-bar toggling -- no WebView/Chromium
     * custom view involved, so none of MaytubeWebChromeClient's fragility
     * applies here. VideoView already fits/centers/letterboxes the frame on
     * its own regardless of orientation; this only changes how much screen
     * it has to do that in.
     */
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        requestedOrientation = if (isFullscreen) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        setSystemChromeHidden(isFullscreen)
    }

    private fun setSystemChromeHidden(hidden: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(!hidden)
            val controller = window.insetsController
            if (hidden) {
                controller?.hide(WindowInsets.Type.systemBars())
                controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller?.show(WindowInsets.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (hidden) {
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                0
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // no WebView/Chromium fullscreen surface here to worry about (see
        // class kdoc) -- VideoView re-measures itself correctly on its own
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "video_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_HOST = "host"
        private const val EXTRA_PORT = "port"
        private const val EXTRA_HTTPS = "https"

        fun intent(context: android.content.Context, config: ServerConfig, videoId: String, title: String?): Intent {
            return Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_HOST, config.host)
                .putExtra(EXTRA_PORT, config.port)
                .putExtra(EXTRA_HTTPS, config.useHttps)
        }
    }
}

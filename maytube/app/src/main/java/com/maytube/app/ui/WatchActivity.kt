package com.maytube.app.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maytube.app.BuildConfig
import com.maytube.app.MaytubeApp
import com.maytube.app.R
import com.maytube.app.browse.CommentPage
import com.maytube.app.browse.VideoDetails
import com.maytube.app.browse.VideoSummary
import com.maytube.app.browse.WatchAdapter
import com.maytube.app.browse.WatchHistory
import com.maytube.app.browse.WatchRow
import com.maytube.app.browse.Yt2009Api
import com.maytube.app.data.ServerConfig
import com.maytube.app.download.SabrSession
import com.maytube.app.player.StreamingPlayer
import com.maytube.app.webview.MobileInjector
import kotlinx.coroutines.launch

/**
 * The native watch screen: title/channel/description, related videos, and
 * comments in one flat RecyclerView (see WatchAdapter), with the actual
 * player pinned above it. Which player depends on the build flavor:
 *
 * - mobile: [StreamingPlayer] (true live-streaming ExoPlayer fed from
 *   maytube's own SABR fragment fetch, see its kdoc), same as always.
 * - tv (BuildConfig.IS_TV_FLAVOR): requested directly ("just use the
 *   fucking WEBVIEW for the TV ver also") -- [watchPlayerWebView] loads
 *   yt2009's own real embed player (`/embed/<id>`, the exact page its own
 *   generated `<iframe>` embed codes already point at) instead. Real
 *   Chromium MSE playback again, same reason MainActivity's WebView was
 *   the mobile flavor's approach from the very start of this project,
 *   rather than maytube's own SABR-fragment-parsing/ExoPlayer pipeline
 *   for this one screen specifically.
 */
@OptIn(markerClass = [UnstableApi::class])
class WatchActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var playerWebView: WebView
    private lateinit var bufferingSpinner: ProgressBar
    private lateinit var qualityButton: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: WatchAdapter
    private var streamingPlayer: StreamingPlayer? = null
    private lateinit var watchHistory: WatchHistory

    private var config: ServerConfig? = null
    private var videoId: String? = null
    private var details: VideoDetails? = null
    private var comments: List<WatchRow> = emptyList()
    private var nextContinuation: String? = null
    private var nextPage: Int? = null
    private var loadingMoreComments = false
    private var isFullscreen = false
    private var availableQualities: List<SabrSession.QualityOption> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch)

        config = (application as MaytubeApp).serverConfigRepository.get()
        watchHistory = WatchHistory(this)
        videoId = intent.getStringExtra(EXTRA_VIDEO_ID)

        playerView = findViewById(R.id.watchPlayerView)
        playerWebView = findViewById(R.id.watchPlayerWebView)
        bufferingSpinner = findViewById(R.id.watchBufferingSpinner)
        qualityButton = findViewById(R.id.watchQualityButton)
        list = findViewById(R.id.watchList)

        findViewById<ImageButton>(R.id.watchCloseButton).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.watchFullscreenButton).setOnClickListener { toggleFullscreen() }

        adapter = WatchAdapter(
            onRelatedClick = ::openVideo,
            onChannelClick = { channelUrl -> startActivity(ChannelActivity.intent(this, channelUrl)) },
            onLoadMoreComments = ::loadMoreComments
        )
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        val cfg = config
        val id = videoId
        if (cfg == null || id == null) {
            finish()
            return
        }

        watchHistory.recordWatched(id)
        if (BuildConfig.IS_TV_FLAVOR) {
            startEmbedPlayback(cfg, id)
        } else {
            playerView.player = StreamingPlayer(this).also { streamingPlayer = it }.player
            qualityButton.setOnClickListener { showQualityPicker() }
            startPlayback(itag = null)
        }
        loadDetails(cfg, id)
    }

    override fun onDestroy() {
        streamingPlayer?.release()
        // harmless even on the mobile flavor, where this WebView never
        // actually loads anything -- destroy()ing an unused WebView is a
        // documented no-op, not an error
        playerWebView.destroy()
        super.onDestroy()
    }

    /**
     * tv flavor only -- see class kdoc. Same maytube_flags cookie
     * MainActivity's applyFlagCookie sets before every WebView page load
     * (SABR/1080p preference, MobileInjector.flagCookieValue), just fired
     * once here instead of on every navigation, since this WebView only
     * ever loads the one embed page for this Activity's lifetime.
     * yt2009_embed.js does also accept `?sabr=1` as a request-scoped
     * override, but not an equivalent for 1080p, so the cookie is still
     * the only way to carry that preference in.
     */
    @Suppress("SetJavaScriptEnabled")
    private fun startEmbedPlayback(cfg: ServerConfig, id: String) {
        playerView.visibility = View.GONE
        qualityButton.visibility = View.GONE
        playerWebView.visibility = View.VISIBLE

        val settings = playerWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(playerWebView, true)
        cookieManager.setCookie(
            cfg.baseUrl,
            "maytube_flags=${MobileInjector.flagCookieValue(cfg)}; Path=/; Max-Age=63072000"
        ) {
            cookieManager.flush()
            bufferingSpinner.visibility = View.GONE
            playerWebView.loadUrl("${cfg.baseUrl}/embed/$id")
        }
    }

    private fun openVideo(video: VideoSummary) {
        startActivity(intent(this, video.videoId))
        finish()
    }

    private fun startPlayback(itag: Int?) {
        // mobile flavor only -- see onCreate's flavor branch
        val player = streamingPlayer ?: return
        val cfg = config ?: return
        val id = videoId ?: return
        bufferingSpinner.visibility = View.VISIBLE
        player.start(
            cfg,
            id,
            itag = itag,
            onProgress = { _, _ -> bufferingSpinner.visibility = View.GONE },
            onQualities = { qualities ->
                availableQualities = qualities
                qualityButton.visibility = if (qualities.isEmpty()) View.GONE else View.VISIBLE
                qualityButton.text = qualities.firstOrNull { it.itag == itag }?.label
                    ?: itag?.let { "itag $it" }
                    ?: "Auto"
            },
            onError = { }
        )
    }

    /**
     * Quality options come straight from yt2009's own `sabrExactRes` data
     * (see SabrSession.parseQualities) -- the exact same list
     * html5-player.js's "Quality" flyout on the HD button shows. Switching
     * restarts playback from the beginning; see StreamingPlayer.start's
     * kdoc for why resuming mid-video on a quality switch isn't attempted.
     */
    private fun showQualityPicker() {
        if (availableQualities.isEmpty()) return
        val labels = (listOf("Auto") + availableQualities.map { it.label }).toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.watch_quality_title)
            .setItems(labels) { _, which ->
                val itag = if (which == 0) null else availableQualities[which - 1].itag
                startPlayback(itag)
            }
            .show()
    }

    private fun loadDetails(config: ServerConfig, id: String) {
        lifecycleScope.launch {
            try {
                val (watchDetails, commentPage) = Yt2009Api.fetchWatchPageWithComments(config, id)
                details = watchDetails
                applyComments(commentPage)
                renderRows()
            } catch (e: Exception) {
                // leave the player running even if metadata/comments fail
                // to load -- video is the part that matters most
            }
        }
    }

    private fun applyComments(page: CommentPage) {
        nextContinuation = page.nextContinuation
        nextPage = page.nextPage
        comments = if (page.comments.isEmpty()) {
            listOf(WatchRow.CommentsEmpty)
        } else {
            page.comments.map { WatchRow.CommentRow(it) }
        }
    }

    private fun renderRows() {
        val d = details ?: return
        val rows = mutableListOf<WatchRow>()
        rows += WatchRow.Header(
            title = d.title,
            meta = listOfNotNull(d.viewCountText, d.uploadedText, d.ratingText?.let { "★$it" })
                .joinToString(" • "),
            channelName = d.channelName,
            channelUrl = d.channelUrl,
            channelAvatarUrl = d.channelAvatarUrl,
            description = d.description
        )
        if (d.related.isNotEmpty()) {
            rows += WatchRow.SectionHeader(getString(R.string.watch_related_header))
            rows += d.related.map { WatchRow.RelatedItem(it) }
        }
        rows += WatchRow.SectionHeader(getString(R.string.watch_comments_header))
        rows += comments
        if (nextContinuation != null || nextPage != null) {
            rows += WatchRow.LoadMoreComments
        }
        adapter.submit(rows)
    }

    private fun loadMoreComments() {
        val cfg = config ?: return
        val id = videoId ?: return
        if (loadingMoreComments) return
        if (nextContinuation == null && nextPage == null) return
        loadingMoreComments = true
        lifecycleScope.launch {
            try {
                val page = Yt2009Api.fetchMoreComments(cfg, id, nextContinuation, nextPage)
                nextContinuation = page.nextContinuation
                nextPage = page.nextPage
                comments = comments.filterNot { it == WatchRow.CommentsEmpty } + page.comments.map { WatchRow.CommentRow(it) }
                renderRows()
            } catch (e: Exception) {
                nextContinuation = null
                nextPage = null
                renderRows()
            } finally {
                loadingMoreComments = false
            }
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        list.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        setSystemChromeHidden(isFullscreen)
    }

    /**
     * Two different APIs depending on minSdk (21) vs. what's actually
     * current: WindowInsetsController is R+ only. Below that, the
     * pre-AndroidX systemUiVisibility flags (same ones
     * PlayerActivity/MaytubeWebChromeClient already use for the exact same
     * purpose) are the only way to hide the system bars at all -- without
     * this branch, "fullscreen" on a pre-R device only did the
     * orientation-lock/hide-the-list part above, silently leaving the
     * status/nav bars on screen the whole time.
     */
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
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                0
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // mobile flavor: plain PlayerView, no WebView/Chromium custom-view
        // fullscreen involved (see PlayerActivity/MaytubeWebChromeClient's
        // history with that) -- re-measures itself correctly on its own.
        //
        // tv flavor: playerWebView has no WebChromeClient at all (see
        // startEmbedPlayback), so there's no real native fullscreen custom
        // view here either -- Chromium's video-fullscreen surface is
        // exactly what dropped html5-player.js's own controls on the
        // mobile flavor's full watch page (see MobileInjector's
        // requestFullscreen-patch history), and this deliberately doesn't
        // opt into that risk a second time for a screen whose "make it
        // bigger" story is already this Activity's own
        // toggleFullscreen()/watchFullscreenButton, not the embed page's
        // own fullscreen button.
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "video_id"

        fun intent(context: android.content.Context, videoId: String): Intent =
            Intent(context, WatchActivity::class.java).putExtra(EXTRA_VIDEO_ID, videoId)
    }
}

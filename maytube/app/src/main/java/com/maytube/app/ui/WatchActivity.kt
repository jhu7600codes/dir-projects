package com.maytube.app.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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
import kotlinx.coroutines.launch

/**
 * The native watch screen: [StreamingPlayer] (true live-streaming, see its
 * kdoc) pinned at top, everything else -- title/channel/description,
 * related videos, comments -- in one flat RecyclerView below (see
 * WatchAdapter). Replaces the old buffer-then-play VideoView-based
 * PlayerActivity for Settings > native player.
 */
@OptIn(markerClass = [UnstableApi::class])
class WatchActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var bufferingSpinner: ProgressBar
    private lateinit var qualityButton: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: WatchAdapter
    private lateinit var streamingPlayer: StreamingPlayer
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
        bufferingSpinner = findViewById(R.id.watchBufferingSpinner)
        qualityButton = findViewById(R.id.watchQualityButton)
        list = findViewById(R.id.watchList)

        streamingPlayer = StreamingPlayer(this)
        playerView.player = streamingPlayer.player

        findViewById<ImageButton>(R.id.watchCloseButton).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.watchFullscreenButton).setOnClickListener { toggleFullscreen() }
        qualityButton.setOnClickListener { showQualityPicker() }

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
        startPlayback(itag = null)
        loadDetails(cfg, id)
    }

    override fun onDestroy() {
        streamingPlayer.release()
        super.onDestroy()
    }

    private fun openVideo(video: VideoSummary) {
        startActivity(intent(this, video.videoId))
        finish()
    }

    private fun startPlayback(itag: Int?) {
        val cfg = config ?: return
        val id = videoId ?: return
        bufferingSpinner.visibility = View.VISIBLE
        streamingPlayer.start(
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(!isFullscreen)
            val controller = window.insetsController
            if (isFullscreen) {
                controller?.hide(WindowInsets.Type.systemBars())
                controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller?.show(WindowInsets.Type.systemBars())
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // plain PlayerView, no WebView/Chromium custom-view fullscreen
        // involved (see PlayerActivity/MaytubeWebChromeClient's history
        // with that) -- re-measures itself correctly on its own
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "video_id"

        fun intent(context: android.content.Context, videoId: String): Intent =
            Intent(context, WatchActivity::class.java).putExtra(EXTRA_VIDEO_ID, videoId)
    }
}

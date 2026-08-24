package com.jhulian.android.youtube.classic.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ListenableFuture
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.YtClassicApp
import com.jhulian.android.youtube.classic.data.model.CommentUi
import com.jhulian.android.youtube.classic.databinding.ActivityPlayerBinding
import com.jhulian.android.youtube.classic.download.DownloadService
import com.jhulian.android.youtube.classic.network.LikeStatus
import com.jhulian.android.youtube.classic.playback.PlaybackService
import com.jhulian.android.youtube.classic.playback.SponsorBlockController
import com.jhulian.android.youtube.classic.playback.StreamSelector
import com.jhulian.android.youtube.classic.util.Formatters
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.StreamInfo

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var commentsAdapter: CommentsAdapter

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller get() = controllerFuture?.takeIf { it.isDone }?.get()

    private var sponsorBlockController: SponsorBlockController? = null
    private var mediaItemLoaded = false
    private var localFilePath: String? = null

    private var playerTitleText: TextView? = null
    private var playerChannelText: TextView? = null
    private var playerQualityText: TextView? = null
    private lateinit var seekGestureDetector: GestureDetector
    private var isFullscreen = false
    private var currentMaxHeight = 0
    private var captionsEnabled = false

    private val fullscreenBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = setFullscreen(false)
    }

    private val sessionManager get() = (application as YtClassicApp).sessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, fullscreenBackCallback)
        setUpPlayerTopBarAndGestures()

        localFilePath = intent.getStringExtra(EXTRA_LOCAL_FILE_PATH)
        if (localFilePath != null) {
            // Offline playback of a file DownloadService already produced -
            // no extraction, no comments/sponsor lookups, no innertube calls.
            val title = intent.getStringExtra(EXTRA_LOCAL_TITLE)
            binding.videoTitle.text = title
            playerTitleText?.text = title
            binding.videoMetadata.text = ""
            binding.channelRow.visibility = View.GONE
            binding.descriptionContainer.visibility = View.GONE
            binding.commentsHeader.visibility = View.GONE
            binding.addCommentRow.visibility = View.GONE
            binding.commentsRecycler.visibility = View.GONE
            binding.commentsProgress.visibility = View.GONE
            connectToPlaybackService()
            return
        }

        val url = intent.getStringExtra(EXTRA_VIDEO_URL)
        if (url == null) {
            finish()
            return
        }

        setupCommentsRecycler()
        setupStaticClickListeners(url)
        connectToPlaybackService()
        observeState()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sponsorEnabled = prefs.getBoolean("pref_sponsorblock_enabled", true)
        val category = prefs.getString("pref_sponsorblock_categories", "sponsor") ?: "sponsor"
        viewModel.load(url, sponsorEnabled, listOf(category))
    }

    /**
     * The controller layout's top row (back/title-channel/captions/settings),
     * the fullscreen button, and the double-tap-to-seek zones - there's no
     * dedicated rewind/forward button in the real app, just double-tap
     * (confirmed against a real device screenshot: fullscreen, controls up,
     * and the only control on screen is the single center play/pause).
     * `playerView.findViewById` works here because PlayerView inflates its
     * controller layout synchronously as part of its own view
     * construction, which has already happened by the time
     * `setContentView`/binding.inflate returns.
     *
     * Settings opens a quality picker, not a share/download menu - those
     * already have their own home in the action-chip row below the video
     * (`actionShare`/`actionDownload`), matching the real app rather than
     * duplicating them up here.
     */
    private fun setUpPlayerTopBarAndGestures() {
        binding.playerView.findViewById<ImageButton>(R.id.playerBackButton)?.setOnClickListener { finish() }
        playerTitleText = binding.playerView.findViewById(R.id.playerTitleText)
        playerChannelText = binding.playerView.findViewById(R.id.playerChannelText)
        playerQualityText = binding.playerView.findViewById(R.id.playerQualityText)

        binding.playerView.findViewById<ImageButton>(R.id.playerSettingsButton)?.setOnClickListener { anchor ->
            showQualityMenu(anchor)
        }
        binding.playerView.findViewById<ImageButton>(R.id.playerCaptionsButton)?.setOnClickListener {
            toggleCaptions()
        }

        binding.playerView.setFullscreenButtonClickListener { fullscreen -> setFullscreen(fullscreen) }

        seekGestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val player = controller ?: return false
                    val isRightSide = e.x > binding.playerView.width / 2f
                    if (isRightSide) {
                        player.seekForward()
                        Toast.makeText(this@PlayerActivity, "+10s »", Toast.LENGTH_SHORT).show()
                    } else {
                        player.seekBack()
                        Toast.makeText(this@PlayerActivity, "« -10s", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            },
        )
        binding.playerView.setOnTouchListener { _, event ->
            seekGestureDetector.onTouchEvent(event)
            false // Let PlayerView still handle single taps to show/hide its controller.
        }
    }

    /**
     * There's no automatic "fullscreen" behavior from PlayerView/PlayerControlView
     * beyond forwarding the button tap (confirmed via javap on media3-ui:
     * `setFullscreenButtonClickListener` exists, but there's no matching
     * "do the resize for me" call) - everything here (orientation, system
     * bars, and swapping the video from a 16:9 box to filling the screen)
     * is this app's own job.
     */
    private fun setFullscreen(fullscreen: Boolean) {
        isFullscreen = fullscreen
        fullscreenBackCallback.isEnabled = fullscreen
        requestedOrientation = if (fullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setImmersiveMode(fullscreen)

        binding.detailsScrollView.visibility = if (fullscreen) View.GONE else View.VISIBLE

        val containerParams = binding.playerContainer.layoutParams as LinearLayout.LayoutParams
        containerParams.height = if (fullscreen) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
        containerParams.weight = if (fullscreen) 1f else 0f
        binding.playerContainer.layoutParams = containerParams

        val wrapperParams = binding.playerViewWrapper.layoutParams
        wrapperParams.height = if (fullscreen) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        binding.playerViewWrapper.layoutParams = wrapperParams

        val playerViewParams = binding.playerView.layoutParams as ConstraintLayout.LayoutParams
        if (fullscreen) {
            playerViewParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            playerViewParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            playerViewParams.dimensionRatio = null
        } else {
            playerViewParams.width = 0
            playerViewParams.height = 0
            playerViewParams.dimensionRatio = "16:9"
        }
        binding.playerView.layoutParams = playerViewParams

        // exo_fullscreen is a media3-ui id, not one of this app module's own -
        // with nonTransitiveRClass, that means androidx.media3.ui.R.id here,
        // not (this file's already-imported) com.jhulian.android.youtube.classic.R.id.
        binding.playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_fullscreen)?.setImageResource(
            if (fullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen,
        )

        // The top-left icon means two different things depending on mode,
        // same as the real app: a down-chevron that just collapses out of
        // fullscreen, vs a back arrow that exits the screen entirely.
        binding.playerView.findViewById<ImageButton>(R.id.playerBackButton)?.apply {
            if (fullscreen) {
                setImageResource(R.drawable.ic_expand_more)
                setOnClickListener { setFullscreen(false) }
            } else {
                setImageResource(R.drawable.ic_arrow_back)
                setOnClickListener { finish() }
            }
        }
    }

    private fun setImmersiveMode(immersive: Boolean) {
        val insetsController = WindowCompat.getInsetsController(window, binding.root)
        if (immersive) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Manually rotating the device (rather than tapping the fullscreen
        // button) should behave the same way the real app does - landscape
        // means fullscreen, portrait means the normal embedded player.
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> if (!isFullscreen) setFullscreen(true)
            Configuration.ORIENTATION_PORTRAIT -> if (isFullscreen) setFullscreen(false)
        }
    }

    private fun connectToPlaybackService() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val player = future.get()
                binding.playerView.player = player
                // The quality label reads the height ExoPlayer is *actually*
                // decoding, not a guess made before playback starts - that
                // guess previously didn't account for the HLS branch in
                // StreamSelector.buildMediaItem() at all (it only checked
                // progressive/video-only streams), so on any video that
                // played back via an HLS manifest the label showed whatever
                // unrelated progressive-stream height happened to resolve
                // (reported as a fixed, wrong "360p" regardless of actual
                // resolution). This is correct for every path, including
                // HLS's own adaptive quality switching mid-playback.
                player.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        if (videoSize.height > 0) updateQualityLabel(videoSize.height)
                    }
                })
                maybeStartPlayback()
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun maybeStartPlayback() {
        if (mediaItemLoaded) return
        val player = controller ?: return

        localFilePath?.let { path ->
            mediaItemLoaded = true
            player.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
            player.prepare()
            player.play()
            binding.playerProgress.visibility = View.GONE
            return
        }

        val info = viewModel.state.value.streamInfo ?: return

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val maxHeight = StreamSelector.heightForQualityPref(prefs.getString("pref_default_quality", "auto"))
        val mediaItem = StreamSelector.buildMediaItem(info, maxHeight)
        if (mediaItem == null) {
            Toast.makeText(this, "No playable stream found", Toast.LENGTH_SHORT).show()
            return
        }

        mediaItemLoaded = true
        currentMaxHeight = maxHeight
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        binding.playerProgress.visibility = View.GONE
        // The quality label itself updates from the real decoded video size
        // (see the Player.Listener in connectToPlaybackService()), not a
        // guess made here.

        sponsorBlockController = SponsorBlockController(
            player = player,
            autoSkip = prefs.getBoolean("pref_sponsorblock_auto_skip", true),
            onAutoSkipped = { showSponsorSkippedMessage() },
            onManualSkipAvailable = { segment ->
                binding.sponsorSkipChip.visibility = if (segment != null) View.VISIBLE else View.GONE
                binding.sponsorSkipChip.setOnClickListener {
                    segment?.let { sponsorBlockController?.skipManually(it) }
                }
            },
        ).also { it.start() }
    }

    private fun showSponsorSkippedMessage() {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, R.string.sponsor_segment_skipped, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun showQualityMenu(anchor: View) {
        val info = viewModel.state.value.streamInfo ?: return
        val heights = (info.videoStreams.orEmpty() + info.videoOnlyStreams.orEmpty())
            .map { it.height }
            .filter { it > 0 }
            .distinct()
            .sortedDescending()

        PopupMenu(this, anchor).apply {
            menu.add(0, 0, 0, "Auto")
            heights.forEachIndexed { index, height -> menu.add(0, height, index + 1, "${height}p") }
            menu.setGroupCheckable(0, true, true)
            menu.findItem(currentMaxHeight)?.isChecked = true
            setOnMenuItemClickListener { item -> switchQuality(item.itemId); true }
        }.show()
    }

    /** menuItemId doubles as the target height here - 0 means "Auto" (no cap). */
    private fun switchQuality(maxHeight: Int) {
        val player = controller ?: return
        val info = viewModel.state.value.streamInfo ?: return
        val mediaItem = StreamSelector.buildMediaItem(info, maxHeight) ?: return

        currentMaxHeight = maxHeight
        val resumePosition = player.currentPosition
        val wasPlaying = player.isPlaying
        player.setMediaItem(mediaItem, resumePosition)
        player.prepare()
        if (wasPlaying) player.play()
        // The label itself updates from the real decoded video size once
        // playback resumes at the new quality (the Player.Listener in
        // connectToPlaybackService()) - not computed here, since that
        // requires re-deriving exactly which branch of
        // StreamSelector.buildMediaItem() actually got used (HLS vs.
        // progressive vs. merged), and getting that wrong is exactly how
        // this label ended up permanently stuck on a wrong "360p" before.
    }

    private fun updateQualityLabel(height: Int) {
        playerQualityText?.text = if (height > 0) "${height}p" else ""
    }

    /**
     * Toggles the text track on/off via `trackSelectionParameters` rather
     * than rebuilding the MediaItem - the subtitle configs are already
     * attached (see `StreamSelector.subtitleConfigurations`), just not
     * selected by default.
     */
    private fun toggleCaptions() {
        val player = controller ?: return
        val hasTextTrack = player.currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT }
        if (!hasTextTrack) {
            Toast.makeText(this, "No captions available for this video", Toast.LENGTH_SHORT).show()
            return
        }
        captionsEnabled = !captionsEnabled
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsEnabled)
            .build()
        binding.playerView.findViewById<ImageButton>(R.id.playerCaptionsButton)?.imageTintList = ColorStateList.valueOf(
            resources.getColor(if (captionsEnabled) R.color.yt_red else android.R.color.white, theme),
        )
    }

    private fun setupCommentsRecycler() {
        commentsAdapter = CommentsAdapter(
            onLike = { /* comment like via innertube not yet wired - read-only likes for now */ },
            onToggleReplies = { comment -> onToggleReplies(comment) },
        )
        binding.commentsRecycler.layoutManager = LinearLayoutManager(this)
        binding.commentsRecycler.adapter = commentsAdapter

        // The comments list lives inside the screen's single NestedScrollView;
        // load more when that outer scroll nears the bottom.
        findNestedScrollView()?.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val child = v.getChildAt(0)
                if (child != null && scrollY >= child.height - v.height - 200) {
                    viewModel.loadMoreComments()
                }
            },
        )
    }

    private fun findNestedScrollView(): NestedScrollView? {
        var parent = binding.commentsRecycler.parent
        while (parent != null && parent !is NestedScrollView) {
            parent = parent.parent
        }
        return parent as? NestedScrollView
    }

    private fun onToggleReplies(comment: CommentUi) {
        if (commentsAdapter.isExpanded(comment.commentId)) {
            commentsAdapter.collapseReplies(comment.commentId)
        } else {
            viewModel.loadReplies(comment) { replies ->
                commentsAdapter.setReplies(comment.commentId, replies)
            }
        }
    }

    private fun setupStaticClickListeners(url: String) {
        binding.likeButton.setOnClickListener { requireCookieOrPromptLogin { cookie -> viewModel.toggleLike(LikeStatus.LIKE, cookie) } }
        binding.dislikeButton.setOnClickListener { requireCookieOrPromptLogin { cookie -> viewModel.toggleLike(LikeStatus.DISLIKE, cookie) } }
        binding.subscribeButton.setOnClickListener { requireCookieOrPromptLogin { cookie -> viewModel.toggleSubscribe(cookie) } }

        binding.actionShare.chipIcon.setImageResource(R.drawable.ic_share)
        binding.actionShare.chipLabel.text = "Share"
        binding.actionShare.root.setOnClickListener { shareVideo(url) }

        binding.actionDownload.chipIcon.setImageResource(R.drawable.ic_download)
        binding.actionDownload.chipLabel.text = getString(R.string.download_video)
        binding.actionDownload.root.setOnClickListener { startDownload() }

        binding.actionSave.chipIcon.setImageResource(R.drawable.ic_playlist_add)
        binding.actionSave.chipLabel.text = "Save"
        binding.actionSave.root.setOnClickListener {
            Toast.makeText(this, "Saved to Watch later", Toast.LENGTH_SHORT).show()
        }

        binding.descriptionContainer.setOnClickListener {
            val expanded = binding.descriptionText.maxLines != Int.MAX_VALUE
            binding.descriptionText.maxLines = if (expanded) Int.MAX_VALUE else 3
        }

        binding.commentSendButton.setOnClickListener {
            val text = binding.commentInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            requireCookieOrPromptLogin { cookie ->
                viewModel.postComment(text, cookie) { success ->
                    if (success) {
                        binding.commentInput.text.clear()
                    } else {
                        Toast.makeText(this, "Couldn't post comment", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.channelRow.setOnClickListener {
            val channelUrl = viewModel.state.value.streamInfo?.uploaderUrl ?: return@setOnClickListener
            startActivity(
                Intent(this, com.jhulian.android.youtube.classic.ui.channel.ChannelActivity::class.java)
                    .putExtra(com.jhulian.android.youtube.classic.ui.channel.ChannelActivity.EXTRA_CHANNEL_URL, channelUrl),
            )
        }
    }

    private fun requireCookieOrPromptLogin(action: (String) -> Unit) {
        val cookie = sessionManager.cookie
        if (cookie.isNullOrBlank()) {
            Toast.makeText(this, R.string.sign_in_prompt, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, com.jhulian.android.youtube.classic.auth.LoginActivity::class.java))
        } else {
            action(cookie)
        }
    }

    private fun shareVideo(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_more)))
    }

    private fun startDownload() {
        val info = viewModel.state.value.streamInfo ?: return
        val videoId = PlayerViewModel.extractVideoId(info.url) ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val maxHeight = StreamSelector.heightForQualityPref(prefs.getString("pref_download_quality", "720p"))

        val progressive = StreamSelector.bestProgressive(info, maxHeight)
        val intent = Intent(this, DownloadService::class.java).apply {
            putExtra(DownloadService.EXTRA_VIDEO_ID, videoId)
            putExtra(DownloadService.EXTRA_TITLE, info.name)
            putExtra(DownloadService.EXTRA_THUMBNAIL, info.thumbnails.maxByOrNull { it.height }?.url)
            if (progressive != null) {
                putExtra(DownloadService.EXTRA_PROGRESSIVE_URL, progressive.content)
            } else {
                val videoOnly = StreamSelector.bestVideoOnly(info, maxHeight)
                val audio = StreamSelector.bestAudio(info)
                putExtra(DownloadService.EXTRA_VIDEO_URL, videoOnly?.content)
                putExtra(DownloadService.EXTRA_AUDIO_URL, audio?.content)
            }
        }
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, R.string.downloading, Toast.LENGTH_SHORT).show()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state -> render(state) }
        }
    }

    private fun render(state: PlayerUiState) {
        val info = state.streamInfo
        if (info != null) {
            renderStreamInfo(info)
            maybeStartPlayback()
            sponsorBlockController?.setSegments(state.sponsorSegments)
        }

        binding.likeButton.setImageResource(
            if (state.likeStatus == LikeStatus.LIKE) R.drawable.ic_thumb_up_filled else R.drawable.ic_thumb_up,
        )
        binding.dislikeButton.setImageResource(
            if (state.likeStatus == LikeStatus.DISLIKE) R.drawable.ic_thumb_down_filled else R.drawable.ic_thumb_down,
        )
        binding.subscribeButton.text = if (state.isSubscribed) getString(R.string.subscribed) else getString(R.string.subscribe)
        binding.subscribeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            resources.getColor(if (state.isSubscribed) R.color.yt_chip_background else R.color.yt_text_primary, theme),
        )

        commentsAdapter.submitComments(state.comments)
        binding.commentsHeader.text = if (state.comments.isNotEmpty() || !state.commentsLoading) {
            getString(R.string.comments_header, Formatters.compactCount(state.comments.size.toLong()))
        } else {
            ""
        }
        binding.commentsProgress.visibility = if (state.commentsLoading) View.VISIBLE else View.GONE
    }

    private fun renderStreamInfo(info: StreamInfo) {
        binding.videoTitle.text = info.name
        playerTitleText?.text = info.name
        playerChannelText?.text = info.uploaderName
        val metadataParts = listOfNotNull(
            if (info.viewCount >= 0) Formatters.viewCount(info.viewCount) else null,
            info.uploadDate?.offsetDateTime()?.let { Formatters.relativeTime(it.toEpochSecond()) }
                ?: info.textualUploadDate,
        )
        binding.videoMetadata.text = metadataParts.joinToString("  •  ")

        binding.likeCount.text = if (info.likeCount > 0) Formatters.compactCount(info.likeCount) else ""

        binding.channelName.text = info.uploaderName
        binding.channelSubs.text = if (info.uploaderSubscriberCount > 0) {
            "${Formatters.compactCount(info.uploaderSubscriberCount)} subscribers"
        } else {
            ""
        }
        // Descriptions come back as HTML (line breaks are literal <br> tags,
        // links are <a> tags) - render it, don't just dump the markup.
        val descriptionHtml = info.description?.content ?: ""
        binding.descriptionText.text = androidx.core.text.HtmlCompat.fromHtml(
            descriptionHtml,
            androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY,
        )

        Glide.with(binding.channelAvatar)
            .load(info.uploaderAvatars.maxByOrNull { it.height }?.url)
            .placeholder(R.drawable.ic_account_circle)
            .transform(CircleCrop())
            .into(binding.channelAvatar)
    }

    override fun onStop() {
        super.onStop()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val backgroundPlayEnabled = prefs.getBoolean("pref_background_play", true)
        if (!backgroundPlayEnabled) {
            controller?.pause()
        }
        // If background play is enabled, we deliberately leave the
        // MediaController/session running - PlaybackService keeps the
        // underlying ExoPlayer + notification alive on its own.
    }

    override fun onDestroy() {
        sponsorBlockController?.stop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_LOCAL_FILE_PATH = "local_file_path"
        private const val EXTRA_LOCAL_TITLE = "local_title"

        fun start(context: Context, videoUrl: String) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java).putExtra(EXTRA_VIDEO_URL, videoUrl),
            )
        }

        fun startLocal(context: Context, filePath: String, title: String) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putExtra(EXTRA_LOCAL_FILE_PATH, filePath)
                    .putExtra(EXTRA_LOCAL_TITLE, title),
            )
        }
    }
}

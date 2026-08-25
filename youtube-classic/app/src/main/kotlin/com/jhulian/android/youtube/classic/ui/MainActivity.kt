package com.jhulian.android.youtube.classic.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.YtClassicApp
import com.jhulian.android.youtube.classic.auth.LoginActivity
import com.jhulian.android.youtube.classic.databinding.ActivityMainBinding
import com.jhulian.android.youtube.classic.playback.PlaybackService
import com.jhulian.android.youtube.classic.ui.common.VideoListFragment
import com.jhulian.android.youtube.classic.ui.common.VideoListSource
import com.jhulian.android.youtube.classic.ui.library.LibraryFragment
import com.jhulian.android.youtube.classic.ui.player.PlayerActivity
import com.jhulian.android.youtube.classic.ui.search.SearchActivity
import com.jhulian.android.youtube.classic.ui.shorts.ShortsFragment

/**
 * Hosts the four-tab bottom nav: Home/Shorts/Subscriptions/Library. The
 * original 2018-2019 app's fourth tab was Trending, not Shorts (Shorts
 * didn't exist yet) - swapped in on request, since a personalized-feed
 * Home plus a public Trending kiosk already covers what that tab did.
 * Tabs are added once and shown/hidden rather than replaced, so each keeps
 * its scroll position and loaded data when you switch away and back - the
 * same "app remembers where you left off" behaviour that shipped alongside
 * this era's tab layout.
 */
@UnstableApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragments = mutableMapOf<Int, Fragment>()
    private var activeTabId: Int = R.id.nav_home

    private var miniPlayerControllerFuture: ListenableFuture<MediaController>? = null
    private val miniPlayerController get() = miniPlayerControllerFuture?.takeIf { it.isDone }?.get()

    private val sessionManager get() = (application as YtClassicApp).sessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This Activity is still the app's actual launcher - MainActivity2016
        // is a whole separate chrome (red top bar + tab strip instead of a
        // bottom nav), not something this class's own layout branches into,
        // so switching eras just means redirecting to it here before this
        // Activity's own layout ever inflates.
        if (PreferenceManager.getDefaultSharedPreferences(this).getString("pref_ui_era", "2019") == "2016") {
            startActivity(Intent(this, MainActivity2016::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.actionCast.setOnClickListener {
            Toast.makeText(this, "No cast devices found", Toast.LENGTH_SHORT).show()
        }
        binding.actionNotifications.setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }
        binding.actionAccount.setOnClickListener { showAccountMenu() }
        setUpLogoEasterEgg()
        connectMiniPlayer()

        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            showTab(R.id.nav_home)
        }

        // BottomNavigationView auto-selects its first menu item (Home) on
        // construction, before this Activity ever runs a line of code -
        // that item's icon (a per-item state-list drawable, see
        // ic_tab_home.xml) never goes through an actual selection *change*
        // to trigger the state-list-drawable-to-icon-view state push, so
        // it renders blank until some later tab switch happens to touch
        // it again. jumpDrawablesToCurrentState() forces every stateful
        // drawable under this view to immediately apply its current state
        // once, which is the standard fix for exactly this "already-
        // selected-at-construction-time" class of blank-icon bug.
        binding.bottomNav.jumpDrawablesToCurrentState()
    }

    private fun showTab(itemId: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        fragments[activeTabId]?.let { transaction.hide(it) }

        var fragment = fragments[itemId]
        if (fragment == null) {
            fragment = createFragment(itemId)
            fragments[itemId] = fragment
            transaction.add(binding.fragmentContainer.id, fragment)
        } else {
            transaction.show(fragment)
        }
        transaction.commit()

        activeTabId = itemId
        updateToolbarForTab(itemId)
        updateMiniPlayer()
    }

    /**
     * Docked mini player - the thing that's missing when you back out of
     * `PlayerActivity` while a video keeps playing in the background
     * (that background playback itself already worked; there was just no
     * on-screen way to see or control it without reopening the full
     * player). Connects its own `MediaController` to the same
     * [PlaybackService] session `PlayerActivity`/`ShortsFragment` use, so
     * it reflects whatever any of them started playing.
     */
    private fun connectMiniPlayer() {
        binding.miniPlayer.setOnClickListener {
            val url = miniPlayerController?.currentMediaItem?.mediaId
            if (!url.isNullOrBlank()) PlayerActivity.start(this, url)
        }
        binding.miniPlayerPlayPause.setOnClickListener {
            val controller = miniPlayerController ?: return@setOnClickListener
            if (controller.isPlaying) controller.pause() else controller.play()
        }
        binding.miniPlayerClose.setOnClickListener {
            miniPlayerController?.stop()
            miniPlayerController?.clearMediaItems()
            updateMiniPlayer()
        }

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        miniPlayerControllerFuture = future
        future.addListener(
            {
                future.get().addListener(
                    object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateMiniPlayer()
                        override fun onIsPlayingChanged(isPlaying: Boolean) = updateMiniPlayer()
                        override fun onPlaybackStateChanged(playbackState: Int) = updateMiniPlayer()
                    },
                )
                updateMiniPlayer()
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun updateMiniPlayer() {
        val controller = miniPlayerController
        val item = controller?.currentMediaItem
        // Shorts already shows its video full-bleed on its own tab - a
        // docked bar on top of that would just be a redundant duplicate.
        val show = item != null && controller.playbackState != Player.STATE_IDLE && activeTabId != R.id.nav_shorts
        binding.miniPlayer.visibility = if (show) View.VISIBLE else View.GONE
        if (!show || item == null) return

        binding.miniPlayerTitle.text = item.mediaMetadata.title
        Glide.with(binding.miniPlayerThumbnail).load(item.mediaMetadata.artworkUri).into(binding.miniPlayerThumbnail)
        binding.miniPlayerPlayPause.setImageResource(if (controller.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
    }

    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.nav_home -> VideoListFragment.newInstance(VideoListSource.Home)
        R.id.nav_shorts -> ShortsFragment()
        R.id.nav_subscriptions -> VideoListFragment.newInstance(VideoListSource.Subscriptions)
        R.id.nav_library -> LibraryFragment()
        else -> VideoListFragment.newInstance(VideoListSource.Home)
    }

    private fun updateToolbarForTab(itemId: Int) {
        // Shorts is full-screen/immersive like the real app - no top bar at
        // all, not even a title, so the vertical feed can use the whole
        // screen above the bottom nav.
        if (itemId == R.id.nav_shorts) {
            binding.toolbar.visibility = View.GONE
            binding.toolbarDivider.visibility = View.GONE
            return
        }
        binding.toolbar.visibility = View.VISIBLE
        binding.toolbarDivider.visibility = View.VISIBLE

        val isHome = itemId == R.id.nav_home
        binding.toolbarLogo.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.toolbarTitle.visibility = if (isHome) View.GONE else View.VISIBLE
        binding.toolbarTitle.text = when (itemId) {
            R.id.nav_subscriptions -> getString(R.string.tab_subscriptions)
            R.id.nav_library -> getString(R.string.tab_library)
            else -> ""
        }
    }

    // Spam the wordmark: 7 taps within a couple seconds, same "how many
    // taps to become a developer" pattern as Android's own build-number
    // easter egg, aimed at the one bit of chrome on this screen that's
    // ours rather than a recreation of something real.
    private var logoTapCount = 0
    private var lastLogoTapAt = 0L

    private fun setUpLogoEasterEgg() {
        binding.toolbarLogo.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastLogoTapAt > EASTER_EGG_TAP_WINDOW_MS) logoTapCount = 0
            lastLogoTapAt = now
            logoTapCount++
            if (logoTapCount >= EASTER_EGG_TAP_COUNT) {
                logoTapCount = 0
                triggerLogoEasterEgg()
            }
        }
    }

    private fun triggerLogoEasterEgg() {
        binding.toolbarLogo.animate()
            .rotationBy(360f)
            .setDuration(500)
            .start()
        Snackbar.make(binding.root, EASTER_EGG_MESSAGES.random(), Snackbar.LENGTH_SHORT).show()
    }

    private fun showAccountMenu() {
        val popup = PopupMenu(this, binding.actionAccount)
        if (sessionManager.isSignedIn) {
            popup.menu.add(getString(R.string.signed_in_as, sessionManager.accountName ?: "you"))
            popup.menu.add(getString(R.string.sign_out))
            popup.setOnMenuItemClickListener { item ->
                if (item.title == getString(R.string.sign_out)) {
                    sessionManager.signOut()
                    recreate()
                }
                true
            }
        } else {
            popup.menu.add(getString(R.string.sign_in))
            popup.setOnMenuItemClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
                true
            }
        }
        popup.show()
    }

    override fun onResume() {
        super.onResume()
        // Catches switching the UI-era pref from Settings and backing out
        // here rather than relaunching the app - onCreate()'s check alone
        // only covers a cold start, since this Activity instance survives
        // Settings and never re-runs onCreate() on its own. isFinishing
        // guards against onCreate()'s own finish() call still letting this
        // fire once more before the Activity actually goes away, which
        // would otherwise launch a second MainActivity2016 on top.
        if (!isFinishing && PreferenceManager.getDefaultSharedPreferences(this).getString("pref_ui_era", "2019") == "2016") {
            startActivity(Intent(this, MainActivity2016::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        miniPlayerControllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }

    companion object {
        private const val EASTER_EGG_TAP_COUNT = 7
        private const val EASTER_EGG_TAP_WINDOW_MS = 800L
        private val EASTER_EGG_MESSAGES = listOf(
            "🎉 You found the easter egg!",
            "📼 Buffering... buffering... buffering...",
            "🥚 There is no algorithm here, just vibes.",
            "▶️ This video will play after one (1) ad. Just kidding.",
            "😎 Nice try. There's nothing behind this logo. Except this.",
        )
    }
}

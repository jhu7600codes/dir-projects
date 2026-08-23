package com.jhulian.android.youtube.classic.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.YtClassicApp
import com.jhulian.android.youtube.classic.auth.LoginActivity
import com.jhulian.android.youtube.classic.databinding.ActivityMainBinding
import com.jhulian.android.youtube.classic.ui.common.VideoListFragment
import com.jhulian.android.youtube.classic.ui.common.VideoListSource
import com.jhulian.android.youtube.classic.ui.library.LibraryFragment
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
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragments = mutableMapOf<Int, Fragment>()
    private var activeTabId: Int = R.id.nav_home

    private val sessionManager get() = (application as YtClassicApp).sessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            showTab(R.id.nav_home)
        }
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

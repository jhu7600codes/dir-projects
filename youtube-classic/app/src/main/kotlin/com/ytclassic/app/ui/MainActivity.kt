package com.ytclassic.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ytclassic.app.R
import com.ytclassic.app.YtClassicApp
import com.ytclassic.app.auth.LoginActivity
import com.ytclassic.app.databinding.ActivityMainBinding
import com.ytclassic.app.ui.common.VideoListFragment
import com.ytclassic.app.ui.common.VideoListSource
import com.ytclassic.app.ui.library.LibraryFragment
import com.ytclassic.app.ui.search.SearchActivity

/**
 * Hosts the four-tab bottom nav (Home/Trending/Subscriptions/Library) from
 * the 2018-2019 YouTube app. Tabs are added once and shown/hidden rather
 * than replaced, so each keeps its scroll position and loaded data when you
 * switch away and back - the same "app remembers where you left off"
 * behaviour that shipped alongside this exact tab layout.
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
        R.id.nav_trending -> VideoListFragment.newInstance(VideoListSource.Trending)
        R.id.nav_subscriptions -> VideoListFragment.newInstance(VideoListSource.Subscriptions)
        R.id.nav_library -> LibraryFragment()
        else -> VideoListFragment.newInstance(VideoListSource.Home)
    }

    private fun updateToolbarForTab(itemId: Int) {
        val isHome = itemId == R.id.nav_home
        binding.toolbarLogo.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.toolbarTitle.visibility = if (isHome) View.GONE else View.VISIBLE
        binding.toolbarTitle.text = when (itemId) {
            R.id.nav_trending -> getString(R.string.tab_trending)
            R.id.nav_subscriptions -> getString(R.string.tab_subscriptions)
            R.id.nav_library -> getString(R.string.tab_library)
            else -> ""
        }
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
}

package com.jhulian.android.youtube.classic.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.YtClassicApp
import com.jhulian.android.youtube.classic.auth.LoginActivity
import com.jhulian.android.youtube.classic.databinding.ActivityMain2016Binding
import com.jhulian.android.youtube.classic.ui.common.VideoListFragment
import com.jhulian.android.youtube.classic.ui.common.VideoListSource
import com.jhulian.android.youtube.classic.ui.library.LibraryFragment
import com.jhulian.android.youtube.classic.ui.search.SearchActivity
import com.jhulian.android.youtube.classic.ui.settings.SettingsActivity

/**
 * The pre-Sept-2016 main screen: a solid red app bar with a tab-icon strip
 * directly under it (Home / Trending / Subscriptions / a star tab), rather
 * than [MainActivity]'s white bar + bottom nav. Picked over building this
 * as a branch inside [MainActivity] itself because the two chrome styles
 * differ enough (top strip vs. bottom nav, a dynamic per-tab title vs. a
 * fixed one, no Shorts tab at all) that keeping them as separate Activities
 * sharing the same Fragments/ViewModels stays far more readable than one
 * Activity's layout/logic branching everywhere on which era is active.
 * [MainActivity] itself is still the app's actual launcher Activity - it
 * redirects here on startup when `pref_ui_era` is "2016" (see its onCreate).
 *
 * Every real detail here - the app bar red (`color/red` = `#DD0000`), the
 * tab strip sitting *under* the toolbar instead of at the bottom, and the
 * Home/Trending/Subscriptions/star icon shapes themselves - was pulled
 * from two real sources, not guessed: a GSMArena article's screenshot of
 * this exact redesign, and a teardown of a real YouTube v6.0.13 (2015-2016)
 * APK downloaded from the Internet Archive's "YoutubePreV10" collection.
 * That APK's `ic_tab_library.png` is genuinely a star, not a folder - see
 * `drawable/ic_2016_tab_library.xml` for why the fourth tab uses one here
 * even though [MainActivity]'s Library tab (a 2018-2019 feature) doesn't.
 * One deliberate scope cut: the real Trending tab of this era also showed
 * a row of colored category chips (Music/Gaming/News/Live) under the tab
 * strip - there's no real "trending by category" surface to back that
 * with here, so it's left out rather than built as a decoration with
 * nothing behind it.
 */
class MainActivity2016 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2016Binding
    private val fragments = mutableMapOf<Int, Fragment>()
    private var activeTabId: Int = R.id.navHome2016

    private val sessionManager get() = (application as YtClassicApp).sessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2016Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionSearch2016.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.actionOverflow2016.setOnClickListener { showOverflowMenu(it as ImageButton) }

        tabButtons().forEach { (id, button) -> button.setOnClickListener { showTab(id) } }

        if (savedInstanceState == null) {
            showTab(R.id.navHome2016)
        }
    }

    private fun tabButtons(): Map<Int, ImageButton> = mapOf(
        R.id.navHome2016 to binding.navHome2016,
        R.id.navTrending2016 to binding.navTrending2016,
        R.id.navSubscriptions2016 to binding.navSubscriptions2016,
        R.id.navLibrary2016 to binding.navLibrary2016,
    )

    private fun showTab(itemId: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        fragments[activeTabId]?.let { transaction.hide(it) }

        var fragment = fragments[itemId]
        if (fragment == null) {
            fragment = createFragment(itemId)
            fragments[itemId] = fragment
            transaction.add(binding.fragmentContainer2016.id, fragment)
        } else {
            transaction.show(fragment)
        }
        transaction.commit()

        activeTabId = itemId
        binding.toolbarTitle2016.text = titleFor(itemId)
        updateTabTints(itemId)
    }

    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.navHome2016 -> VideoListFragment.newInstance(VideoListSource.Home)
        R.id.navTrending2016 -> VideoListFragment.newInstance(VideoListSource.Trending)
        R.id.navSubscriptions2016 -> VideoListFragment.newInstance(VideoListSource.Subscriptions)
        R.id.navLibrary2016 -> LibraryFragment()
        else -> VideoListFragment.newInstance(VideoListSource.Home)
    }

    private fun titleFor(itemId: Int): String = when (itemId) {
        R.id.navHome2016 -> getString(R.string.tab_home)
        R.id.navTrending2016 -> getString(R.string.tab_trending)
        R.id.navSubscriptions2016 -> getString(R.string.tab_subscriptions)
        R.id.navLibrary2016 -> getString(R.string.tab_library)
        else -> getString(R.string.app_name)
    }

    /**
     * The real tab icons are one plain white alpha-mask PNG each, tinted
     * red/gray by selection state rather than swapped for a different
     * drawable - same approach [MainActivity]'s tab icons now use (see
     * git history), just applied via `imageTintList` here since these are
     * plain ImageButtons rather than a BottomNavigationView with its own
     * ColorStateList support.
     */
    private fun updateTabTints(selectedId: Int) {
        val selectedColor = ColorStateList.valueOf(resources.getColor(R.color.yt_2016_red, theme))
        val unselectedColor = ColorStateList.valueOf(resources.getColor(R.color.yt_2016_nav_unselected, theme))
        tabButtons().forEach { (id, button) ->
            button.imageTintList = if (id == selectedId) selectedColor else unselectedColor
        }
    }

    private fun showOverflowMenu(anchor: ImageButton) {
        val popup = PopupMenu(this, anchor)
        if (sessionManager.isSignedIn) {
            popup.menu.add(getString(R.string.signed_in_as, sessionManager.accountName ?: "you"))
            popup.menu.add(getString(R.string.sign_out))
        } else {
            popup.menu.add(getString(R.string.sign_in))
        }
        popup.menu.add(getString(R.string.settings_title))
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                getString(R.string.sign_out) -> {
                    sessionManager.signOut()
                    Toast.makeText(this, R.string.sign_out, Toast.LENGTH_SHORT).show()
                    recreate()
                }
                getString(R.string.sign_in) -> startActivity(Intent(this, LoginActivity::class.java))
                getString(R.string.settings_title) -> startActivity(Intent(this, SettingsActivity::class.java))
                else -> Unit // the "Signed in as X" row itself - not actionable.
            }
            true
        }
        popup.show()
    }

    override fun onResume() {
        super.onResume()
        // Mirror of MainActivity's own onResume() check (see there for why
        // the isFinishing guard matters) - lets switching the pref back to
        // "2019" from Settings and backing out here redirect immediately
        // too, in either direction.
        if (!isFinishing && PreferenceManager.getDefaultSharedPreferences(this).getString("pref_ui_era", "2019") != "2016") {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

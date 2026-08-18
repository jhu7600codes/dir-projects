package com.maytube.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.maytube.app.BuildConfig
import com.maytube.app.MaytubeApp
import com.maytube.app.R
import com.maytube.app.browse.HairlineDividerDecoration
import com.maytube.app.browse.VideoSummary
import com.maytube.app.browse.VideoSummaryAdapter
import com.maytube.app.browse.WatchHistory
import com.maytube.app.browse.Yt2009Api
import com.maytube.app.data.ServerConfig
import com.maytube.app.data.ServerConfigRepository
import com.maytube.app.util.isTv
import kotlinx.coroutines.launch

/**
 * Settings > native player's entry point when it's on: MainActivity
 * redirects here instead of loading its WebView at all (see
 * MainActivity.maybeRedirectToNativeMode). Parses yt2009's homepage HTML
 * directly -- see Yt2009Api's kdoc for why, and for exactly which
 * selectors -- rather than embedding a WebView anywhere in this screen.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var repository: ServerConfigRepository
    private lateinit var watchHistory: WatchHistory
    private lateinit var list: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var adapter: VideoSummaryAdapter

    private var config: ServerConfig? = null

    private val settingsLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
            val newConfig = repository.get()
            if (newConfig == null) {
                finish()
                return@registerForActivityResult
            }
            if (!newConfig.nativePlayer && !BuildConfig.IS_TV_FLAVOR && !isTv(this)) {
                // switched back to WebView mode: hand off to MainActivity
                // and get out of the way. Never on the tv flavor --
                // MainActivity/WebView isn't even a component in that APK
                // (see its AndroidManifest.xml), so this has to be a
                // compile-time-certain skip (BuildConfig.IS_TV_FLAVOR), not
                // just a skip when the runtime isTv() heuristic happens to
                // agree -- see build.gradle.kts's productFlavors kdoc for
                // exactly why that distinction matters here.
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return@registerForActivityResult
            }
            config = newConfig
            loadHome()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        repository = (application as MaytubeApp).serverConfigRepository
        watchHistory = WatchHistory(this)

        setSupportActionBar(findViewById<Toolbar>(R.id.homeToolbar))

        list = findViewById(R.id.homeList)
        swipeRefresh = findViewById(R.id.homeSwipeRefresh)
        progress = findViewById(R.id.homeProgress)
        errorView = findViewById(R.id.homeError)

        adapter = VideoSummaryAdapter(onClick = ::openVideo, onLoadMore = {})
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.addItemDecoration(HairlineDividerDecoration(this))
        swipeRefresh.setOnRefreshListener { loadHome() }

        val existing = repository.get()
        if (existing == null || (!existing.nativePlayer && !BuildConfig.IS_TV_FLAVOR && !isTv(this))) {
            // shouldn't normally happen (MainActivity gates this, or this
            // Activity IS the entry point at all on the tv flavor -- see
            // BuildConfig.IS_TV_FLAVOR's kdoc in build.gradle.kts), but
            // don't strand the user on a broken screen if it does. A null
            // config always goes to Settings regardless of device/flavor --
            // there's nothing to browse yet either way, and SettingsActivity
            // exists in both flavors' manifests.
            startActivity(Intent(this, if (existing == null) SettingsActivity::class.java else MainActivity::class.java))
            finish()
            return
        }
        config = existing
        loadHome()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            R.id.action_downloads -> {
                startActivity(Intent(this, DownloadsActivity::class.java))
                true
            }
            R.id.action_settings -> {
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openVideo(video: VideoSummary) {
        startActivity(WatchActivity.intent(this, video.videoId))
    }

    private fun loadHome() {
        val cfg = config ?: return
        progress.visibility = View.VISIBLE
        errorView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val page = Yt2009Api.fetchHome(cfg, watchHistory.getRecent())
                adapter.submit(page.videos, hasMorePages = false)
                if (page.videos.isEmpty()) {
                    errorView.text = getString(R.string.home_error, "empty response")
                    errorView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                errorView.text = getString(R.string.home_error, e.message ?: e.toString())
                errorView.visibility = View.VISIBLE
            } finally {
                progress.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }
}

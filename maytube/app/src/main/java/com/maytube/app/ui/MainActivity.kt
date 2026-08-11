package com.maytube.app.ui

import android.app.DownloadManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.maytube.app.MaytubeApp
import com.maytube.app.R
import com.maytube.app.data.ServerConfig
import com.maytube.app.data.ServerConfigRepository
import com.maytube.app.download.VideoDownloader
import com.maytube.app.webview.MaytubeWebChromeClient
import com.maytube.app.webview.MaytubeWebViewClient
import com.maytube.app.webview.MobileInjector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Single-WebView shell around a yt2009 instance. Loads the configured
 * server's root page, re-applies the mobile CSS/JS adaption on every
 * navigation, and keeps SABR-related cookies in sync with Settings.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var repository: ServerConfigRepository
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var pageProgress: ProgressBar
    private lateinit var notConfiguredView: TextView
    private lateinit var appBar: com.google.android.material.appbar.AppBarLayout
    private lateinit var playNativelyFab: ExtendedFloatingActionButton
    private lateinit var maytubeWebViewClient: MaytubeWebViewClient
    private lateinit var webChromeClient: MaytubeWebChromeClient

    private var config: ServerConfig? = null
    private var activeDownloadJob: Job? = null

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val newConfig = repository.get()
            if (newConfig == null) {
                if (config == null) {
                    // first run, user backed out without saving anything
                    finish()
                }
                return@registerForActivityResult
            }
            if (newConfig.nativePlayer) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                return@registerForActivityResult
            }
            val previous = config
            val hostChanged = previous == null ||
                newConfig.host != previous.host ||
                newConfig.port != previous.port ||
                newConfig.useHttps != previous.useHttps
            val changed = newConfig != previous
            config = newConfig
            maytubeWebViewClient.updateConfig(newConfig)
            when {
                hostChanged -> loadHome()
                // a flag-only change (SABR/1080p/dark mode): reload the
                // current page in place instead of yanking the user back
                // to the homepage for something that isn't a navigation
                changed -> {
                    applyFlagCookie(newConfig) {
                        webView.reload()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = (application as MaytubeApp).serverConfigRepository

        // Settings > native player: hand off to the fully-native
        // browse/watch/comments shell (HomeActivity) instead of ever
        // creating/loading this Activity's WebView at all -- checked
        // before setContentView so a native-mode launch never even
        // inflates the WebView layout.
        val existingConfig = repository.get()
        if (existingConfig?.nativePlayer == true) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)
        // "hide the toolbar" meant gone for good, not just during
        // fullscreen -- long-press (showQuickAccessMenu) is the intended
        // way to reach Settings/Downloads/Reload/Home/Download now. Only
        // the toolbar itself goes away; appBar (its container, which also
        // holds the load-progress bar) still hides during fullscreen so
        // that doesn't overlay the video either.
        toolbar.visibility = android.view.View.GONE

        pageProgress = findViewById(R.id.pageProgress)
        notConfiguredView = findViewById(R.id.notConfiguredView)
        appBar = findViewById(R.id.appBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        webView = findViewById(R.id.webView)
        playNativelyFab = findViewById(R.id.playNativelyFab)
        playNativelyFab.setOnClickListener { playCurrentVideoNatively() }

        setupWebView()

        swipeRefresh.setOnRefreshListener { webView.reload() }

        onBackPressedDispatcher.addCallback(this) {
            when {
                webChromeClient.isFullscreen -> webChromeClient.exitFullscreenIfNeeded(webView)
                webView.canGoBack() -> webView.goBack()
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        if (existingConfig == null) {
            notConfiguredView.visibility = android.view.View.VISIBLE
            openSettings()
        } else {
            config = existingConfig
            maytubeWebViewClient.updateConfig(existingConfig)
            loadHome()
        }
    }

    override fun onResume() {
        super.onResume()
        // Settings > native player: onCreate redirects to HomeActivity and
        // returns before webView (or anything else past that point) ever
        // gets initialized -- but this Activity still goes through its
        // normal lifecycle on the way out (crashed on a real device:
        // UninitializedPropertyAccessException from onDestroy touching
        // webView unconditionally). Every callback below that touches
        // webView has to check it was actually set up first.
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    /**
     * MainActivity declares configChanges="orientation|screenSize|..." so
     * rotating for fullscreen video doesn't recreate the Activity (which
     * would reload the page and kill playback every single time). The
     * tradeoff: this callback is now the only signal we get when a
     * rotation actually lands, and WebView/Chromium's fullscreen video
     * surface is known to not always re-layout itself correctly across a
     * config change the hosting Activity survives -- reported directly
     * from a real device (Pixel 3a, 1080x2200) as the video rendering
     * pillarboxed into a narrow center column after rotating into
     * fullscreen, far narrower than ordinary object-fit:contain
     * letterboxing on a 16:9 video would ever produce. See
     * MaytubeWebChromeClient.onShowCustomView for the fuller writeup and
     * the delayed relayout it already schedules for the initial
     * transition; this covers the same class of bug for every later
     * config change too (e.g. flipping the device while already
     * fullscreen), the moment it's actually reported instead of guessing
     * at a delay.
     *
     * While fullscreen is active, only the fullscreen surface itself
     * needs this -- the underlying WebView is fully hidden behind it
     * (see onFullscreenChanged), so there's no reason to also nudge its
     * layout, and one less thing poking at Chromium's fullscreen state
     * machine during a transition is one less thing that could interfere
     * with it.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // same native-mode-never-set-up-webView guard as onResume/onPause/
        // onDestroy -- a config change can still land on a finishing
        // activity
        if (!::webChromeClient.isInitialized || !::webView.isInitialized) return
        if (webChromeClient.isFullscreen) {
            webChromeClient.relayoutFullscreenView()
        } else {
            webView.requestLayout()
            webView.invalidate()
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        maytubeWebViewClient = MaytubeWebViewClient(
            context = this,
            config = config ?: ServerConfig(host = "", port = ServerConfigRepository.DEFAULT_PORT)
        ) { url -> onPageFinished(url) }
        webView.webViewClient = maytubeWebViewClient

        webChromeClient = MaytubeWebChromeClient(
            activity = this,
            onProgress = ::onProgress,
            onTitle = { title -> supportActionBar?.subtitle = title },
            onFullscreenChanged = { isFullscreen ->
                appBar.visibility = if (isFullscreen) android.view.View.GONE else android.view.View.VISIBLE
            },
            onLongPress = { showQuickAccessMenu() }
        )
        webView.webChromeClient = webChromeClient

        webView.setOnLongClickListener {
            showQuickAccessMenu()
            true
        }

        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            downloadArbitraryUrl(url, contentDisposition, mimeType)
        }

        // registers once, then runs on every navigation before the page's
        // own scripts execute -- unlike evaluateJavascript() called from
        // onPageStarted, which has no such guarantee. See
        // MobileInjector.buildSabrDiagnosticScript() for what/why.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                MobileInjector.buildSabrDiagnosticScript(),
                setOf("*")
            )
        }
    }

    private fun onProgress(progress: Int) {
        if (progress >= 100) {
            pageProgress.visibility = android.view.View.GONE
            swipeRefresh.isRefreshing = false
        } else {
            pageProgress.visibility = android.view.View.VISIBLE
            pageProgress.progress = progress
        }
    }

    private fun onPageFinished(url: String?) {
        swipeRefresh.isRefreshing = false
        notConfiguredView.visibility = android.view.View.GONE
        // Settings > native player: offer the native player specifically on
        // watch pages, where there's actually something to play -- the
        // WebView's own copy of the video is paused/blanked by
        // MobileInjector whenever this is on (see its kdoc), this FAB is
        // the way to actually start playback in that mode.
        val showFab = config?.nativePlayer == true && MobileInjector.extractVideoId(url) != null
        playNativelyFab.visibility = if (showFab) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun openSettings() {
        settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
    }

    /**
     * The toolbar is gone for good now (not just during fullscreen), so
     * this long-press menu -- reachable on the page itself, or on the
     * fullscreen video surface (see MaytubeWebChromeClient.onShowCustomView)
     * -- is the only way to reach Settings/Downloads/Reload/Home/Download,
     * not just a fullscreen-specific convenience.
     */
    private fun showQuickAccessMenu() {
        val items = arrayOf(
            getString(R.string.action_settings),
            getString(R.string.action_downloads),
            getString(R.string.action_reload),
            getString(R.string.action_home),
            getString(R.string.action_download_this_video)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.quick_access_title)
            .setItems(items) { _, which ->
                if (webChromeClient.isFullscreen) {
                    webChromeClient.exitFullscreenIfNeeded(webView)
                }
                when (which) {
                    0 -> openSettings()
                    1 -> startActivity(Intent(this, DownloadsActivity::class.java))
                    2 -> webView.reload()
                    3 -> loadHome()
                    4 -> downloadCurrentVideo()
                }
            }
            .show()
    }

    private fun loadHome() {
        val cfg = config ?: return
        applyFlagCookie(cfg) {
            webView.loadUrl(cfg.baseUrl + "/")
        }
    }

    /**
     * Sets the exp_sabr/hd_1080 flag cookie and only calls [onDone] once
     * it's actually committed.
     *
     * CookieManager.setCookie(url, value) (no callback) is fire-and-forget
     * -- it does not guarantee the cookie is visible to the *very next*
     * request. Reported directly from a real device: toggling SABR/1080p
     * in Settings had no effect on playback until the flags were set by
     * hand through yt2009's own /yt2009_flags.htm. Since yt2009 decides
     * whether to use SABR by sniffing the raw Cookie header of that exact
     * request (back/yt2009html.js), a cookie that loses this race is
     * silently as if the setting were never touched -- calling
     * loadUrl()/reload() immediately after firing setCookie(), like this
     * used to, was exactly that race. The 3-arg overload's callback fires
     * once the write is actually committed; only navigate after that.
     */
    private fun applyFlagCookie(cfg: ServerConfig, onDone: () -> Unit) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie(
            cfg.baseUrl,
            "maytube_flags=${MobileInjector.flagCookieValue(cfg)}; Path=/; Max-Age=63072000"
        ) {
            cookieManager.flush()
            onDone()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reload -> {
                webView.reload()
                true
            }
            R.id.action_home -> {
                loadHome()
                true
            }
            R.id.action_download_this_video -> {
                downloadCurrentVideo()
                true
            }
            R.id.action_downloads -> {
                startActivity(Intent(this, DownloadsActivity::class.java))
                true
            }
            R.id.action_settings -> {
                openSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun playCurrentVideoNatively() {
        val cfg = config
        val videoId = MobileInjector.extractVideoId(webView.url)
        if (cfg == null || videoId == null) {
            Snackbar.make(webView, R.string.download_no_video, Snackbar.LENGTH_LONG).show()
            return
        }
        val title = webView.title?.removePrefix("YouTube - ")?.takeIf { it.isNotBlank() }
        startActivity(PlayerActivity.intent(this, cfg, videoId, title))
    }

    private fun downloadCurrentVideo() {
        val cfg = config
        val videoId = MobileInjector.extractVideoId(webView.url)
        if (cfg == null || videoId == null) {
            Snackbar.make(webView, R.string.download_no_video, Snackbar.LENGTH_LONG).show()
            return
        }
        if (activeDownloadJob?.isActive == true) {
            Snackbar.make(webView, R.string.download_already_running, Snackbar.LENGTH_SHORT).show()
            return
        }

        val title = webView.title?.removePrefix("YouTube - ")?.takeIf { it.isNotBlank() } ?: videoId

        val dialogView = layoutInflater.inflate(R.layout.dialog_download_progress, null)
        val titleView = dialogView.findViewById<TextView>(R.id.downloadTitle)
        val statusView = dialogView.findViewById<TextView>(R.id.downloadStatus)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.downloadProgressBar)
        titleView.text = title
        statusView.text = getString(R.string.download_resolving)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton(R.string.download_cancel) { _, _ -> activeDownloadJob?.cancel() }
            .show()

        val job = lifecycleScope.launch {
            val result = VideoDownloader.download(this@MainActivity, cfg, videoId, title) { progress ->
                runOnUiThread {
                    val total = progress.totalMs
                    if (total != null && total > 0) {
                        progressBar.isIndeterminate = false
                        progressBar.progress =
                            ((progress.fetchedMs.toFloat() / total) * 1000).toInt().coerceIn(0, 1000)
                        statusView.text = getString(
                            R.string.download_progress_known, formatMs(progress.fetchedMs), formatMs(total)
                        )
                    } else {
                        progressBar.isIndeterminate = true
                        statusView.text = getString(R.string.download_progress_unknown, formatMs(progress.fetchedMs))
                    }
                }
            }
            when (result) {
                is VideoDownloader.Result.Completed ->
                    Snackbar.make(webView, getString(R.string.download_complete, result.file.name), Snackbar.LENGTH_LONG)
                        .show()
                is VideoDownloader.Result.FallbackStarted ->
                    Snackbar.make(
                        webView,
                        getString(R.string.download_fallback_started, result.fileName),
                        Snackbar.LENGTH_LONG
                    ).show()
                is VideoDownloader.Result.Error ->
                    Snackbar.make(webView, result.message, Snackbar.LENGTH_LONG).show()
                is VideoDownloader.Result.Progress -> Unit // only ever surfaced via onProgress above
            }
        }
        job.invokeOnCompletion { cause ->
            runOnUiThread {
                dialog.dismiss()
                if (cause is CancellationException) {
                    Snackbar.make(webView, R.string.download_cancelled, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        activeDownloadJob = job
    }

    private fun formatMs(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val locale = java.util.Locale.US
        return if (h > 0) String.format(locale, "%d:%02d:%02d", h, m, s) else String.format(locale, "%d:%02d", m, s)
    }

    /** Catches native browser-style download triggers (e.g. an <a download> link). */
    private fun downloadArbitraryUrl(url: String, contentDisposition: String?, mimeType: String?) {
        val cfg = config ?: return
        try {
            val request = DownloadManager.Request(url.toUri())
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrBlank()) {
                request.addRequestHeader("Cookie", cookies)
            }
            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
            request.setTitle(fileName)
            request.setDescription("Downloading from ${cfg.hostAndPort}")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalFilesDir(
                this, android.os.Environment.DIRECTORY_MOVIES, fileName
            )
            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Snackbar.make(webView, getString(R.string.download_started, fileName), Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            Snackbar.make(webView, R.string.download_failed, Snackbar.LENGTH_LONG).show()
        }
    }
}

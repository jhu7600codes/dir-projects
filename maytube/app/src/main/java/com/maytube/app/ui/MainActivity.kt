package com.maytube.app.ui

import android.app.DownloadManager
import android.content.Intent
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.maytube.app.MaytubeApp
import com.maytube.app.R
import com.maytube.app.data.ServerConfig
import com.maytube.app.data.ServerConfigRepository
import com.maytube.app.download.VideoDownloader
import com.maytube.app.webview.MaytubeWebChromeClient
import com.maytube.app.webview.MaytubeWebViewClient
import com.maytube.app.webview.MobileInjector

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
    private lateinit var maytubeWebViewClient: MaytubeWebViewClient
    private lateinit var webChromeClient: MaytubeWebChromeClient

    private var config: ServerConfig? = null

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
            val changed = newConfig != config
            config = newConfig
            maytubeWebViewClient.updateConfig(newConfig)
            if (changed) {
                loadHome()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repository = (application as MaytubeApp).serverConfigRepository

        setSupportActionBar(findViewById<Toolbar>(R.id.mainToolbar))

        pageProgress = findViewById(R.id.pageProgress)
        notConfiguredView = findViewById(R.id.notConfiguredView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        webView = findViewById(R.id.webView)

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

        val existing = repository.get()
        if (existing == null) {
            notConfiguredView.visibility = android.view.View.VISIBLE
            openSettings()
        } else {
            config = existing
            maytubeWebViewClient.updateConfig(existing)
            loadHome()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
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
            onTitle = { title -> supportActionBar?.subtitle = title }
        )
        webView.webChromeClient = webChromeClient

        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            downloadArbitraryUrl(url, contentDisposition, mimeType)
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

    private fun onPageFinished(@Suppress("UNUSED_PARAMETER") url: String?) {
        swipeRefresh.isRefreshing = false
        notConfiguredView.visibility = android.view.View.GONE
    }

    private fun openSettings() {
        settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
    }

    private fun loadHome() {
        val cfg = config ?: return
        applyFlagCookie(cfg)
        webView.loadUrl(cfg.baseUrl + "/")
    }

    private fun applyFlagCookie(cfg: ServerConfig) {
        CookieManager.getInstance().setCookie(
            cfg.baseUrl,
            "maytube_flags=${MobileInjector.flagCookieValue(cfg)}; Path=/; Max-Age=63072000"
        )
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

    private fun downloadCurrentVideo() {
        val cfg = config
        val videoId = MobileInjector.extractVideoId(webView.url)
        if (cfg == null || videoId == null) {
            Snackbar.make(webView, R.string.download_no_video, Snackbar.LENGTH_LONG).show()
            return
        }
        val title = webView.title?.removePrefix("YouTube - ")
        when (val result = VideoDownloader.startDownload(this, cfg, videoId, title)) {
            is VideoDownloader.Result.Started ->
                Snackbar.make(webView, getString(R.string.download_started, result.fileName), Snackbar.LENGTH_LONG)
                    .show()
            is VideoDownloader.Result.Error ->
                Snackbar.make(webView, R.string.download_failed, Snackbar.LENGTH_LONG).show()
        }
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

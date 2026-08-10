package com.maytube.app.webview

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * Handles HTML5 fullscreen video requests (video.requestFullscreen() from
 * html5-player.js / the browser's native video controls) by swapping in a
 * full-window custom view, and reports load progress back to the host
 * Activity for a progress indicator.
 */
class MaytubeWebChromeClient(
    private val activity: Activity,
    private val onProgress: (Int) -> Unit,
    private val onTitle: (String?) -> Unit
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var originalSystemUiVisibility = 0

    private val decorView: ViewGroup
        get() = activity.window.decorView as ViewGroup

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onProgress(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        onTitle(title)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }

        customView = view
        customViewCallback = callback
        originalOrientation = activity.requestedOrientation
        @Suppress("DEPRECATION")
        originalSystemUiVisibility = decorView.systemUiVisibility

        decorView.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        hideSystemChrome()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onHideCustomView() {
        val view = customView ?: return
        decorView.removeView(view)
        customView = null
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = originalSystemUiVisibility
        activity.requestedOrientation = originalOrientation
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    private fun hideSystemChrome() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        } else {
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    /**
     * yt2009's footer has a handful of target="_blank" links (real
     * youtube.com support pages, the github sponsor link). Without this,
     * WebView silently swallows those clicks; instead spin up a disposable
     * WebView just to catch the URL and hand it to the system browser.
     */
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        val throwaway = WebView(activity)
        throwaway.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                openExternally(request.url.toString())
                return true
            }
        }
        val transport = resultMsg.obj as? WebView.WebViewTransport
        transport?.webView = throwaway
        resultMsg.sendToTarget()
        return true
    }

    private fun openExternally(url: String) {
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            // nothing sensible to fall back to
        }
    }

    val isFullscreen: Boolean get() = customView != null

    fun exitFullscreenIfNeeded(webView: WebView) {
        if (isFullscreen) {
            webView.evaluateJavascript(
                "(function(){ if (document.fullscreenElement) { document.exitFullscreen(); } })();",
                null
            )
            onHideCustomView()
        }
    }
}

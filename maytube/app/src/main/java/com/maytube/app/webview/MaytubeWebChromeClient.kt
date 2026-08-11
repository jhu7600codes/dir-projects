package com.maytube.app.webview

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.maytube.app.BuildConfig

/**
 * Handles HTML5 fullscreen video requests (video.requestFullscreen() from
 * html5-player.js / the browser's native video controls) by swapping in a
 * full-window custom view, and reports load progress back to the host
 * Activity for a progress indicator.
 */
class MaytubeWebChromeClient(
    private val activity: Activity,
    private val onProgress: (Int) -> Unit,
    private val onTitle: (String?) -> Unit,
    private val onFullscreenChanged: (Boolean) -> Unit = {},
    private val onLongPress: () -> Unit = {}
) : WebChromeClient() {

    private var customView: View? = null
    private var fullscreenContainer: FrameLayout? = null
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

    /**
     * Overriding this replaces WebView's own default "print to logcat"
     * behavior with this one -- so this IS how you get the page's console
     * output into logcat, not something extra on top of it. Debug builds
     * only. Filter for it on-device with (no PC/adb needed):
     *   su -c "logcat -s MaytubeWebConsole"
     * See also MobileInjector's SABR request/response diagnostic hook,
     * which logs through here with a "[maytube-sabr]" prefix.
     */
    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        if (BuildConfig.DEBUG) {
            val text = "${consoleMessage.message()} " +
                "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
            when (consoleMessage.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> Log.e(CONSOLE_TAG, text)
                ConsoleMessage.MessageLevel.WARNING -> Log.w(CONSOLE_TAG, text)
                else -> Log.d(CONSOLE_TAG, text)
            }
        }
        return true
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

        // belt-and-suspenders: this view fully covers decorView (which
        // includes our own Toolbar underneath it) on its own, but hiding
        // the native app chrome explicitly too means there's no gap for it
        // to peek through on any device/WebView-version quirk, and lets
        // MainActivity stop reserving layout space for it while fullscreen
        onFullscreenChanged(true)
        // long-press works during fullscreen too -- this view (not our own
        // WebView) is what's actually on screen and receiving touches now
        view.setOnLongClickListener {
            onLongPress()
            true
        }

        // Reported directly from a real device: fullscreen video filled
        // the screen edge to edge with no black bars, even for content
        // whose aspect ratio doesn't match the (often wider-than-16:9,
        // e.g. 20:9) screen. Chromium's own fullscreen video surface does
        // letterbox internally by default -- the actual bug was that
        // `view` was being added straight onto decorView, whose
        // background is whatever Theme.Maytube's default is (never set to
        // black), so any letterboxed gap showed as the app's light theme
        // background instead of black -- easy to miss entirely against
        // dark video content, reads as "no bars" even though letterboxing
        // was happening. Wrapping it in an explicitly black container
        // guarantees a real black backdrop regardless of decorView's own
        // background.
        val container = FrameLayout(activity).apply {
            setBackgroundColor(Color.BLACK)
        }
        container.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        fullscreenContainer = container

        decorView.addView(
            container,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        hideSystemChrome()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Reported directly from a real device (Pixel 3a, 1080x2200): even
        // with the black backdrop above, the video itself rendered
        // squeezed into a narrow center column -- far too narrow to be
        // ordinary object-fit:contain letterboxing (a 16:9 video on this
        // phone's ~20:9 landscape screen should fill ~87% of the width,
        // not a small fraction of it). requestedOrientation is only a
        // request -- the actual rotation happens asynchronously, well
        // after this function returns. Chromium's fullscreen video surface
        // is laid out (and computes its own internal letterbox sizing)
        // against whatever the window's dimensions are *at that moment*;
        // if that first layout lands before the rotation has actually
        // finished, it bakes in sizing computed against the old portrait
        // window, and nothing here was forcing a second layout pass once
        // the rotation genuinely completed. This is a known class of bug,
        // not specific to this app -- WebView/Chromium fullscreen video
        // failing to resize correctly across a rotation that the hosting
        // Activity doesn't get recreated for is widely reported. Force an
        // explicit re-layout shortly after, once the rotation animation
        // has almost certainly settled; see also MainActivity's
        // onConfigurationChanged, which does the same the moment the
        // config change callback actually lands (covers rotating while
        // already fullscreen too, not just the initial transition).
        container.postDelayed({ relayoutFullscreenView() }, RELAYOUT_SETTLE_DELAY_MS)
    }

    /**
     * Forces a fresh measure/layout pass on the active fullscreen view (and
     * its container), for the pillarboxing-after-rotation bug described in
     * [onShowCustomView]. Safe to call even when not fullscreen -- no-ops.
     */
    fun relayoutFullscreenView() {
        val container = fullscreenContainer ?: return
        val view = customView ?: return
        container.requestLayout()
        container.invalidate()
        view.requestLayout()
        view.invalidate()
    }

    override fun onHideCustomView() {
        customView ?: return
        val container = fullscreenContainer
        if (container != null) {
            decorView.removeView(container)
        }
        fullscreenContainer = null
        customView = null
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = originalSystemUiVisibility
        activity.requestedOrientation = originalOrientation
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        onFullscreenChanged(false)
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

    companion object {
        private const val CONSOLE_TAG = "MaytubeWebConsole"
        private const val RELAYOUT_SETTLE_DELAY_MS = 400L
    }
}

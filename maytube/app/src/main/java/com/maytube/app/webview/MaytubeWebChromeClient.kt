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
    // yt2009's own CSS-driven fake-fullscreen mode (see setPseudoFullscreen's kdoc) --
    // distinct from customView above, which is real native WebView fullscreen. Kept
    // separate rather than folded into one enum: nothing here should ever try to route
    // real native fullscreen through the pseudo path or vice versa, and this makes the
    // two impossible to conflate by construction.
    private var pseudoFullscreen = false
    // guards applyFullscreenChrome()/revertFullscreenChrome() against being applied
    // twice in a row (would clobber the saved "original" values with already-fullscreen
    // ones) regardless of which of the two paths above triggered it
    private var chromeApplied = false
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

        // belt-and-suspenders: this view fully covers decorView (which
        // includes our own Toolbar underneath it) on its own, but hiding
        // the native app chrome explicitly too means there's no gap for it
        // to peek through on any device/WebView-version quirk, and lets
        // MainActivity stop reserving layout space for it while fullscreen
        applyFullscreenChrome()
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
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        revertFullscreenChrome()
    }

    /**
     * Toggles the same system-chrome-hiding/landscape-lock/keep-screen-on
     * treatment [onShowCustomView]/[onHideCustomView] give real native
     * fullscreen, but for yt2009's own CSS-driven "fullscreen-unsupported"
     * fallback instead (see MobileInjector.buildInjectionScript's
     * requestFullscreen patch for why real native fullscreen is
     * deliberately disabled for the player: it drops yt2009's own custom
     * controls, which this fallback -- the exact one yt2009 already ships
     * for browsers without a working Fullscreen API -- keeps intact).
     *
     * Driven by [MaytubeFullscreenBridge], which the injected script calls
     * whenever a MutationObserver sees #baseDiv gain/lose that CSS class.
     * No native View is added/removed here (unlike the real path) --
     * yt2009's own CSS already handles making the player cover the
     * viewport within the WebView itself; this only handles the
     * surrounding native chrome.
     *
     * If real native fullscreen is somehow *also* active ([customView] !=
     * null), that takes precedence and this is a no-op -- the two should
     * never both be genuinely engaged at once given the player is what's
     * being toggled, but this keeps [revertFullscreenChrome] from firing
     * early out from under a real fullscreen session, or vice versa.
     */
    fun setPseudoFullscreen(active: Boolean) {
        if (customView != null) return
        if (active == pseudoFullscreen) return
        pseudoFullscreen = active
        if (active) applyFullscreenChrome() else revertFullscreenChrome()
    }

    private fun applyFullscreenChrome() {
        if (chromeApplied) return
        chromeApplied = true
        originalOrientation = activity.requestedOrientation
        @Suppress("DEPRECATION")
        originalSystemUiVisibility = decorView.systemUiVisibility
        onFullscreenChanged(true)
        hideSystemChrome()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun revertFullscreenChrome() {
        if (!chromeApplied) return
        chromeApplied = false
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = originalSystemUiVisibility
        activity.requestedOrientation = originalOrientation
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    // covers both real native fullscreen (customView) and yt2009's own
    // CSS-driven fallback (pseudoFullscreen, see setPseudoFullscreen) --
    // callers that just need "is something fullscreen right now" (the
    // quick-access menu, back-button handling) shouldn't have to care
    // which kind
    val isFullscreen: Boolean get() = customView != null || pseudoFullscreen

    // MainActivity.onConfigurationChanged needs to tell these apart:
    // relayoutFullscreenView()'s pillarboxing-after-rotation workaround is
    // specific to the real native surface below, not yt2009's own CSS
    // fallback, which is ordinary WebView content and just needs the
    // WebView itself nudged the normal way instead
    val hasNativeFullscreenSurface: Boolean get() = customView != null

    /**
     * Reported directly from a real device: after backing out of
     * fullscreen (Android back button, or the quick-access menu) a few
     * times across a session, the page's own fullscreen button stopped
     * entering fullscreen at all -- every tap logged a rejected
     * "Failed to execute 'exitFullscreen' on 'Document': Document not
     * active" promise instead.
     *
     * Root cause: `onHideCustomView()` is meant to be invoked BY
     * Chromium, as the callback for the page's own document.exitFullscreen()
     * actually completing -- that round trip (JS asks to exit -> Chromium
     * finishes the transition -> Chromium calls WebChromeClient back) is
     * the entire contract of CustomViewCallback. This used to fire the JS
     * exit request and then call onHideCustomView() itself immediately
     * afterward, synchronously, in the same tick -- tearing the native
     * surface down and reporting "already hidden" via
     * customViewCallback.onCustomViewHidden() before Chromium's own
     * exitFullscreen() transition had actually resolved. Interrupting
     * that transition mid-flight is exactly what a rejected "Document not
     * active" promise describes, and left Chromium's internal fullscreen
     * bookkeeping wedged often enough that later requestFullscreen()
     * calls silently did nothing -- explains a bug that only showed up
     * after repeated fullscreen enter/exit cycles, not on the very first
     * one.
     *
     * Fix: only ask the page to exit here, then let onHideCustomView (the
     * override above) run when Chromium genuinely calls it back. The
     * short delayed fallback covers the case where there's no real
     * fullscreenElement to exit (nothing for Chromium to call back for)
     * or the callback genuinely never arrives.
     *
     * Also covers yt2009's own CSS-driven fallback (pseudoFullscreen):
     * there's no real fullscreenElement to ask Chromium to exit in that
     * case (document.fullscreenElement is null the whole time real native
     * fullscreen is disabled for the player -- see MobileInjector), so
     * this clicks yt2009's own fullscreen button instead, taking its exit
     * branch exactly as if the user had tapped it themselves. The delayed
     * fallback below still applies if that click doesn't land for
     * whatever reason (button not found, page navigated away, etc).
     */
    fun exitFullscreenIfNeeded(webView: WebView) {
        if (!isFullscreen) return
        webView.evaluateJavascript(EXIT_FULLSCREEN_JS, null)
        webView.postDelayed({
            if (customView != null) onHideCustomView()
            if (pseudoFullscreen) setPseudoFullscreen(false)
        }, EXIT_FALLBACK_DELAY_MS)
    }

    /**
     * Companion to [exitFullscreenIfNeeded], for MainActivity's
     * rotate-to-landscape-means-fullscreen behavior: clicks yt2009's own
     * fullscreen button (assets/site-assets/html5-player.js,
     * `.video_controls .fullscreen`) exactly as if the user had tapped it
     * themselves, taking its ENTER branch. That's deliberate, not a
     * shortcut -- routing through the same button the user would press
     * means this takes the exact same forced-CSS-fallback path every other
     * entry into fullscreen in this app already does (see
     * MobileInjector's requestFullscreen patch); there's no separate
     * "enter fullscreen" concept to keep in sync with that one.
     *
     * No-ops if already fullscreen (real or pseudo -- nothing to enter) or
     * if the button isn't on the page at all: not a watch page, or the
     * player hasn't finished setting up its controls yet.
     * querySelector returning null in that case is harmless, same as
     * [exitFullscreenIfNeeded]'s button lookup.
     */
    fun enterFullscreenIfNeeded(webView: WebView) {
        if (isFullscreen) return
        webView.evaluateJavascript(ENTER_FULLSCREEN_JS, null)
    }

    companion object {
        private const val CONSOLE_TAG = "MaytubeWebConsole"
        private const val RELAYOUT_SETTLE_DELAY_MS = 400L
        private const val EXIT_FALLBACK_DELAY_MS = 600L

        // .video_controls .fullscreen / "opened" class: exactly how
        // assets/site-assets/html5-player.js selects and marks its own
        // fullscreen button (fullscreen_btn = $(".video_controls .fullscreen"),
        // fullscreen_btn.className = "fullscreen opened" while active)
        private const val EXIT_FULLSCREEN_JS = """
            (function() {
                if (document.fullscreenElement) { document.exitFullscreen(); return; }
                var btn = document.querySelector('.video_controls .fullscreen');
                if (btn && btn.classList.contains('opened')) { btn.click(); }
            })();
        """

        // mirror image of EXIT_FULLSCREEN_JS above -- click the same
        // button, but only when it's NOT already marked "opened", taking
        // html5-player.js's enter branch instead of its exit one
        private const val ENTER_FULLSCREEN_JS = """
            (function() {
                var btn = document.querySelector('.video_controls .fullscreen');
                if (btn && !btn.classList.contains('opened')) { btn.click(); }
            })();
        """
    }
}

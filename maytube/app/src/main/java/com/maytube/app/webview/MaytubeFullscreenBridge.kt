package com.maytube.app.webview

import android.app.Activity
import android.webkit.JavascriptInterface

/**
 * Bridges yt2009's own CSS-driven "fullscreen-unsupported" mode back to
 * native code. MobileInjector.buildInjectionScript() attaches a
 * MutationObserver to #baseDiv that calls onEnter()/onExit() whenever
 * yt2009's own html5-player.js adds/removes that class -- which, per
 * MobileInjector's own kdoc, is now the *only* way fullscreen ever
 * actually engages here (real native WebView fullscreen is deliberately
 * disabled for the player, since it drops yt2009's own custom controls).
 *
 * Without this, that fake fullscreen would just be a same-size CSS
 * overlay with the Android status bar/app toolbar still on top of it --
 * this is what lets MaytubeWebChromeClient still apply the same
 * system-chrome-hiding/landscape-lock/keep-screen-on treatment real
 * native fullscreen (onShowCustomView) would have given it.
 *
 * @JavascriptInterface methods are invoked on a background thread, not
 * the UI thread that touching window/orientation/decorView flags
 * requires -- both methods hop back via runOnUiThread.
 */
class MaytubeFullscreenBridge(
    private val activity: Activity,
    private val onChanged: (Boolean) -> Unit
) {
    @JavascriptInterface
    fun onEnter() {
        activity.runOnUiThread { onChanged(true) }
    }

    @JavascriptInterface
    fun onExit() {
        activity.runOnUiThread { onChanged(false) }
    }
}

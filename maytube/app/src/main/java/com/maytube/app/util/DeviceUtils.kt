package com.maytube.app.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

/**
 * TV support: maytube's default launcher UI (MainActivity's WebView, loading
 * yt2009's own touch-oriented desktop-reflowed site) is fundamentally
 * unusable with a D-pad/remote -- there's no cursor, no touch, and yt2009's
 * own JS assumes mouse hover for things like the volume/captions menus (see
 * MobileInjector's touchstart-dismiss fix, itself only a workaround for
 * *touch*, not "no pointer at all").
 *
 * This is a defensive fallback now, not the app's real TV story -- an
 * earlier version of this feature routed a TV device into the native
 * browse/watch/comments shell purely by this runtime check, in the same
 * mobile APK as the WebView, and it visibly failed to trigger on a real
 * device (reported directly: "MAKE A NEW TV ONLY APK. bad concept of it").
 * The actual mechanism is now the separate `tv` build flavor
 * (build.gradle.kts's productFlavors kdoc): its own AndroidManifest.xml
 * removes MainActivity outright, so there's no launch-time detection able
 * to get it wrong -- the WebView simply isn't a reachable component in
 * that APK. [isTv] still gets called from the *mobile* flavor's
 * MainActivity/HomeActivity as a belt-and-suspenders check, purely in case
 * that flavor's own APK (the one that still has the WebView) somehow ends
 * up running on a TV some other way.
 */
fun isTv(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

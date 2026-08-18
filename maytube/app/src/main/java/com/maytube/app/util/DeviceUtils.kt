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
 * *touch*, not "no pointer at all"). [isTv] lets MainActivity/HomeActivity
 * route a TV device straight into the native browse/watch/comments shell
 * (HomeActivity/WatchActivity/PlayerActivity) unconditionally, the same
 * shell Settings > native player already offers on phones -- regardless of
 * whether that setting happens to be on, since on a TV it isn't really
 * optional the way it is on a phone.
 */
fun isTv(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

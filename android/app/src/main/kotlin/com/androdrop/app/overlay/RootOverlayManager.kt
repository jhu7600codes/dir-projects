package com.androdrop.app.overlay

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.topjohnwu.superuser.Shell

/**
 * Root is entirely optional for androdrop — every feature works without it
 * via standard Android APIs (the share-sheet entry, the full-screen-intent
 * notification). Where root IS available, this silently grants the
 * "draw over other apps" permission via `appops` so the instant WindowManager
 * overlay popup (OverlayPopupService) can show without the user ever seeing
 * the manual Settings > Special app access prompt.
 */
object RootOverlayManager {
    private const val TAG = "RootOverlayManager"

    // Shell.getShell() blocks on the su handshake — only touch this from a
    // background thread, never the main thread.
    val isRooted: Boolean by lazy {
        runCatching { Shell.getShell().isRoot }.getOrDefault(false)
    }

    suspend fun ensureOverlayPermission(context: Context) {
        if (!isRooted) return
        if (Settings.canDrawOverlays(context)) return

        val result = runCatching {
            Shell.cmd("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow").exec()
        }.getOrNull()

        if (result?.isSuccess != true) {
            Log.w(TAG, "failed to self-grant SYSTEM_ALERT_WINDOW: ${result?.out}")
        }
    }
}

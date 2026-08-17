package com.androdrop.app

import android.app.Application
import com.androdrop.app.notify.Notifications
import com.androdrop.app.overlay.RootOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AndrodropApp : Application() {
    // Not tied to any Activity/Service lifecycle — used for app-wide,
    // fire-and-forget setup like the root probe below.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)

        // Best-effort, silent: on a non-rooted device this just no-ops.
        appScope.launch { RootOverlayManager.ensureOverlayPermission(this@AndrodropApp) }
    }
}

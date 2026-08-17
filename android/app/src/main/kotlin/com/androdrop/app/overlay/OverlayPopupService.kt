package com.androdrop.app.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.androdrop.app.data.ApiClient
import com.androdrop.app.data.IncomingTransferSummary
import com.androdrop.app.data.RespondRequest
import com.androdrop.app.ui.IncomingTransferCard
import com.androdrop.app.ui.theme.AndrodropTheme
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Root-enhanced entry point: a true `TYPE_APPLICATION_OVERLAY` window drawn
 * directly on screen, appearing instantly without an activity-launch
 * animation or the lock screen full-screen-intent dance. Only started when
 * RootOverlayManager has already self-granted SYSTEM_ALERT_WINDOW; on a
 * non-rooted device this class is simply never invoked and the app relies
 * entirely on the standard full-screen-intent notification instead.
 */
class OverlayPopupService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var windowManager: WindowManager? = null
    private var popupView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID)
        val json = intent?.getStringExtra(EXTRA_TRANSFER_JSON)
        val transfer = json?.let { Gson().fromJson(it, IncomingTransferSummary::class.java) }
        if (deviceId == null || transfer == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        showPopup(deviceId, transfer)
        return START_NOT_STICKY
    }

    private fun showPopup(deviceId: String, transfer: IncomingTransferSummary) {
        removePopup()

        var busy by mutableStateOf(false)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayPopupService)
            setViewTreeViewModelStoreOwner(this@OverlayPopupService)
            setViewTreeSavedStateRegistryOwner(this@OverlayPopupService)
            setContent {
                AndrodropTheme {
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
                        IncomingTransferCard(
                            transfer = transfer,
                            busy = busy,
                            onAccept = {
                                busy = true
                                scope.launch { respond(transfer.id, deviceId, "accept") }
                            },
                            onDecline = {
                                busy = true
                                scope.launch { respond(transfer.id, deviceId, "decline") }
                            },
                        )
                    }
                }
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        windowManager?.addView(view, params)
        popupView = view
    }

    private suspend fun respond(transferId: String, deviceId: String, action: String) {
        try {
            ApiClient.service.respond(transferId, RespondRequest(deviceId, action))
        } catch (_: Exception) {
            // The persistent notification (posted alongside this overlay)
            // remains as a fallback if this call fails.
        } finally {
            removePopup()
            stopSelf()
        }
    }

    private fun removePopup() {
        popupView?.let { runCatching { windowManager?.removeView(it) } }
        popupView = null
    }

    override fun onDestroy() {
        removePopup()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_DEVICE_ID = "device_id"
        private const val EXTRA_TRANSFER_JSON = "transfer_json"

        fun show(context: Context, deviceId: String, transfer: IncomingTransferSummary) {
            val intent = Intent(context, OverlayPopupService::class.java).apply {
                putExtra(EXTRA_DEVICE_ID, deviceId)
                putExtra(EXTRA_TRANSFER_JSON, Gson().toJson(transfer))
            }
            context.startService(intent)
        }
    }
}

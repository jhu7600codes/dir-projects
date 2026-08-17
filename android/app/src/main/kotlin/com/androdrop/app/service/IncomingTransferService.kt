package com.androdrop.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.androdrop.app.data.ApiClient
import com.androdrop.app.data.DeviceStore
import com.androdrop.app.notify.Notifications
import com.androdrop.app.overlay.OverlayPopupService
import com.androdrop.app.overlay.RootOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that watches for incoming transfers. This is the
 * Android equivalent of the web app's Realtime subscription + polling
 * fallback (androdrop's IncomingTransferOverlay.tsx) — Android has no
 * lightweight always-on push channel here (that's what FCM would be for,
 * deliberately not used — see ApiService), so polling is the primary path.
 */
class IncomingTransferService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val seenTransferIds = mutableSetOf<String>()
    private lateinit var deviceStore: DeviceStore

    override fun onCreate() {
        super.onCreate()
        deviceStore = DeviceStore(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID_SERVICE, Notifications.serviceNotification(this))
        scope.launch { pollLoop() }
        return START_STICKY
    }

    private suspend fun pollLoop() {
        val device = deviceStore.current() ?: return

        while (true) {
            try {
                val incoming = ApiClient.service.incoming(device.deviceId)
                for (transfer in incoming) {
                    if (seenTransferIds.add(transfer.id)) {
                        notifyIncoming(device.deviceId, transfer)
                    }
                }
                // Once a transfer leaves the pending list (responded to from
                // another client, or expired), forget it so the seen-set
                // doesn't grow forever across a long-running service.
                seenTransferIds.retainAll(incoming.map { it.id }.toSet())
            } catch (_: Exception) {
                // Network hiccup — just try again next tick.
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun notifyIncoming(
        deviceId: String,
        transfer: com.androdrop.app.data.IncomingTransferSummary,
    ) {
        val notification = Notifications.incomingTransferNotification(this, deviceId, transfer)
        NotificationManagerCompat.from(this).notify(transfer.id.hashCode(), notification)

        if (RootOverlayManager.isRooted) {
            OverlayPopupService.show(this, deviceId, transfer)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID_SERVICE = 1
        const val POLL_INTERVAL_MS = 5000L
    }
}

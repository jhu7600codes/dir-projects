package com.androdrop.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import com.androdrop.app.data.ApiClient
import com.androdrop.app.data.RespondRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the Accept/Decline actions on the incoming-transfer notification. */
class TransferActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val transferId = intent.getStringExtra(Notifications.EXTRA_TRANSFER_ID) ?: return
        val deviceId = intent.getStringExtra(Notifications.EXTRA_DEVICE_ID) ?: return
        val action = when (intent.action) {
            Notifications.ACTION_ACCEPT -> "accept"
            Notifications.ACTION_DECLINE -> "decline"
            else -> return
        }

        // Dismiss the notification immediately for snappy feedback; the
        // actual API call happens in the background via goAsync().
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(transferId.hashCode())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.service.respond(transferId, RespondRequest(deviceId, action))
            } catch (_: Exception) {
                // Best-effort: if this fails the user can still respond from
                // IncomingTransferActivity (poll will surface it again).
            } finally {
                pendingResult.finish()
            }
        }
    }
}

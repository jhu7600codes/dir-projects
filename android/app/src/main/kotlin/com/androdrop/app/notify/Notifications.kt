package com.androdrop.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.androdrop.app.R
import com.androdrop.app.data.IncomingTransferSummary
import com.androdrop.app.ui.IncomingTransferActivity

object Notifications {
    const val CHANNEL_SERVICE = "androdrop_service"
    const val CHANNEL_INCOMING = "androdrop_incoming"

    const val ACTION_ACCEPT = "com.androdrop.app.action.ACCEPT"
    const val ACTION_DECLINE = "com.androdrop.app.action.DECLINE"
    const val EXTRA_TRANSFER_ID = "transfer_id"
    const val EXTRA_DEVICE_ID = "device_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.notification_channel_service_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = context.getString(R.string.notification_channel_service_desc)
                setShowBadge(false)
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INCOMING,
                context.getString(R.string.notification_channel_incoming_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_incoming_desc)
                enableVibration(true)
            },
        )
    }

    fun serviceNotification(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_channel_service_name))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * High-priority, actionable alert for one incoming transfer. Uses a
     * full-screen intent so it can interrupt the lock screen like an
     * incoming call — no root required, this is a standard Android API. The
     * root-enhanced path (RootOverlayManager) additionally shows a true
     * WindowManager overlay on top of this for an even faster response.
     */
    fun incomingTransferNotification(
        context: Context,
        deviceId: String,
        transfer: IncomingTransferSummary,
    ): android.app.Notification {
        val fileSummary = if (transfer.files.size == 1) {
            transfer.files.first().name
        } else {
            "${transfer.files.size} files"
        }

        val fullScreenIntent = Intent(context, IncomingTransferActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSFER_ID, transfer.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            transfer.id.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val acceptIntent = actionIntent(context, ACTION_ACCEPT, transfer.id, deviceId)
        val declineIntent = actionIntent(context, ACTION_DECLINE, transfer.id, deviceId)

        return NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${transfer.senderName} wants to send you a file")
            .setContentText(fileSummary)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "Decline", declineIntent)
            .addAction(0, "Accept", acceptIntent)
            .build()
    }

    private fun actionIntent(
        context: Context,
        action: String,
        transferId: String,
        deviceId: String,
    ): PendingIntent {
        val intent = Intent(context, TransferActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TRANSFER_ID, transferId)
            putExtra(EXTRA_DEVICE_ID, deviceId)
        }
        return PendingIntent.getBroadcast(
            context,
            (action + transferId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

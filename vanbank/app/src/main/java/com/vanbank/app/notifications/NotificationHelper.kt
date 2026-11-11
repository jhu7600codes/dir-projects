package com.vanbank.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.vanbank.app.MainActivity
import com.vanbank.app.R
import com.vanbank.app.data.local.entity.AiPaymentRequestEntity
import com.vanbank.core.model.Money

private const val AI_CHANNEL_ID = "dir_ai_assistant"
private const val BILL_CHANNEL_ID = "bill_pay"
private const val AI_NOTIFICATION_ID_BASE = 9_000
private const val BILL_NOTIFICATION_ID_BASE = 8_000

/**
 * Surfaces the DIR AI Assistant's payment requests (and bill-pay results) as
 * real Android notifications, so they show up even when VANBank is
 * backgrounded -- an approval/decline flow that feels like a genuine
 * invoice, not a toast that only appears while the app happens to be open.
 */
class NotificationHelper(private val context: Context) {

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                AI_CHANNEL_ID,
                context.getString(R.string.ai_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.ai_notification_channel_desc)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                BILL_CHANNEL_ID,
                context.getString(R.string.bill_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.bill_notification_channel_desc)
            },
        )
    }

    fun showAiPaymentRequest(request: AiPaymentRequestEntity) {
        if (!hasPostPermission()) return
        val notification = NotificationCompat.Builder(context, AI_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("DIR AI Assistant — payment request")
            .setContentText("${request.title} — ${Money.format(request.amountMinor)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${request.title}\n${request.detail}\n\nAmount due: ${Money.format(request.amountMinor)}"),
            )
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(AI_NOTIFICATION_ID_BASE + request.id.toInt(), notification)
    }

    fun showBillPayResult(billName: String, amountMinor: Long, success: Boolean) {
        if (!hasPostPermission()) return
        val title = if (success) "Bill paid" else "Bill payment failed"
        val text = if (success) {
            "$billName — ${Money.format(amountMinor)} auto-deducted"
        } else {
            "$billName — ${Money.format(amountMinor)} due, insufficient funds"
        }
        val notification = NotificationCompat.Builder(context, BILL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(BILL_NOTIFICATION_ID_BASE + billName.hashCode() % 1000, notification)
    }

    private fun contentIntent(): android.app.PendingIntent {
        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        return android.app.PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

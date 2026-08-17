package com.androdrop.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.androdrop.app.data.DeviceStore
import com.androdrop.app.service.IncomingTransferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Only restart the watcher if this device was already paired
                // — don't silently register a new device just from a reboot.
                if (DeviceStore(context.applicationContext).current() != null) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, IncomingTransferService::class.java),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

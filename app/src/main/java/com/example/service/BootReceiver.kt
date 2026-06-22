package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.PrefsManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context ?: return
            val prefs = PrefsManager(context)
            if (prefs.isBlockingEnabled) {
                FocusNotificationHelper.showDisabledNotification(context)
            }
        }
    }
}

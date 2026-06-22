package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object FocusNotificationHelper {
    private const val CHANNEL_ID = "focusguard_channel"
    private const val DISABLED_CHANNEL_ID = "focusguard_disabled_channel"
    private const val NOTIFICATION_ID = 1001
    private const val WARNING_ID = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Channel for active blocker
            val activeChannel = NotificationChannel(
                CHANNEL_ID,
                "FocusGuard Guard Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows static indicator that WhatsApp update blocking is active"
            }
            manager.createNotificationChannel(activeChannel)

            // Channel for security warnings (high importance alert)
            val warningChannel = NotificationChannel(
                DISABLED_CHANNEL_ID,
                "FocusGuard Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts you if FocusGuard blocking is disabled"
            }
            manager.createNotificationChannel(warningChannel)
        }
    }

    fun showDisabledNotification(context: Context) {
        createNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DISABLED_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🛡️ FocusGuard Warning")
            .setContentText("FocusGuard was disabled — re-enable to stay protected!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(WARNING_ID, notification)
    }

    fun showActiveNotification(context: Context) {
        createNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🛡️ FocusGuard Guard on Duty")
            .setContentText("FocusGuard is active — WhatsApp updates are blocked.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelActiveNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}

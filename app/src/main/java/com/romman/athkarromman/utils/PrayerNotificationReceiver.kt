package com.romman.athkarromman.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.romman.athkarromman.R

/**
 * Created By Batool Mofeed - 15/04/2024.
 **/
class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        // Check if the intent action matches the action we defined when scheduling the notification
        if (intent?.action == "com.romman.athkarromman.SHOW_NOTIFICATION") {
            showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "prayer_notification_channel",
                "Prayer Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, "prayer_notification_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Prayer Time")
            .setContentText("It's time for prayer!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify(0, builder.build())
        }
    }
}
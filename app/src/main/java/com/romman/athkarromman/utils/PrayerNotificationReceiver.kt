package com.romman.athkarromman.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.romman.athkarromman.fcm.NotificationBuilder
import com.romman.athkarromman.ui.MainActivity

/**
 * Created By Batool Mofeed - 15/04/2024.
 **/
class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        // Check if the intent action matches the action we defined when scheduling the notification
            showNotification(context)

    }

    private fun showNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java)

        pushNotification(
            context,
            "Prayer Time", "It's time for prayer!", intent,
        )

    }

    private fun pushNotification(
        context: Context,
        title: String,
        body: String,
        intent: Intent?,
    ) {
        NotificationBuilder.Builder().withContext(context)
            .withPendingIntent(intent)
            .withTitle(title)
            .withContentText(body)
            .show()
    }
}
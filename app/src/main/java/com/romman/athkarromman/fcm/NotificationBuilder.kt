package com.romman.athkarromman.fcm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.romman.athkarromman.R
import kotlin.random.Random


object NotificationBuilder {
    class Builder {
        private lateinit var notificationManager: NotificationManager
        private lateinit var notificationBuilder: NotificationCompat.Builder
        private var context: Context? = null
        private var pendingIntent: PendingIntent? = null
        private var title: String? = null
        private var content: String? = null
        private var largeIcon: String? = ""

        //unique id for each pushed notification
        private val notificationId = generateRandomID()

        /** set context **/
        fun withContext(context: Context) = apply {
            this.context = context
        }

        /** intent to identify distention **/
        fun withPendingIntent(intent: Intent?) = apply {
            if (intent != null) {
                this.pendingIntent = PendingIntent.getActivity(
                    context, notificationId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }

        /**title of notification**/
        fun withTitle(title: String) = apply {
            this.title = title
        }

        /**body of notification**/
        fun withContentText(content: String) = apply {
            this.content = content
        }


        /**large icon  of notification**/
        fun withLargeIcon(icon: String) = apply {
            this.largeIcon = icon
        }

        /**Build and show the notification with specified properties**/
        @Suppress("DEPRECATION")
        fun show(): NotificationCompat.Builder {
            if (context != null) {
                notificationManager =
                    context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationBuilder =
                    NotificationCompat.Builder(context!!, NOTIFICATION_CHANNEL_ID)
                        .setColorized(true)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(
                            if (title != null) title else context?.getString(R.string.app_name)
                        )
                        .setContentText(
                            if (content != null) content else context?.getString(R.string.app_name)
                        )
                        .setOngoing(false)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_SOUND)

//                val futureTarget = Glide.with(context!!)
//                    .asBitmap()
//                    .load(largeIcon)
//                    .submit()
//
//                val bitmap = futureTarget.get()
                notificationBuilder.setSmallIcon(R.drawable.ic_launcher_background)

                if (pendingIntent != null) {
                    notificationBuilder.setContentIntent(pendingIntent)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationManager.createNotificationChannel(createNotificationChannel())
                } else {
                    notificationBuilder.priority = Notification.PRIORITY_MAX
                }
                notificationManager.notify(notificationId, notificationBuilder.build())

            } else {
                throw IllegalStateException("Notification Builder must have Context object to work !!")
            }
            return notificationBuilder
        }

        /**
         * generate unique id for each pushed notification
         * */
        private fun generateRandomID() = (Random.nextInt(1000 - 65) + 65)

        /**
         * create notification channel if device running on
         * Android [Build.VERSION_CODES.O] version or above
         * */
        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.O)
        private fun createNotificationChannel() = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = NOTIFICATION_CHANNEL_DESC
            enableLights(true)
            enableVibration(true)
        }

        companion object {
            private const val NOTIFICATION_CHANNEL_ID = "athkar_romman_app"
            private const val NOTIFICATION_CHANNEL_NAME = "Athkar Notifications"
            private const val NOTIFICATION_CHANNEL_DESC = "Athkar Notification Channel"
        }
    }
}
package com.curio.app.infrastructure

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.curio.app.MainActivity
import com.curio.app.R
import com.curio.app.data.AppPreferences
import com.curio.app.data.DailyReminderScheduler
import com.curio.app.navigation.PendingSpinOpen

/** Delivers the user's daily shuffle reminder alarm. */
class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (AppPreferences.isReminderEnabled(context)) {
            DailyReminderScheduler.schedule(
                context,
                AppPreferences.getReminderHour(context),
                AppPreferences.getReminderMinute(context)
            )
        }
        createChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            4108,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // The reminder nudges the user to shuffle — make the tap land
                // on the Spin deck (shuffle page), not plain Home.
                putExtra(PendingSpinOpen.EXTRA_OPEN_SPIN, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("A little curiosity awaits")
            .setContentText("Take a moment to shuffle into something new.")
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Daily shuffle reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "A gentle daily nudge to discover a new topic."
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "daily_spin_reminders"
        const val NOTIFICATION_ID = 4109
    }
}

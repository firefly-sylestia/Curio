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
import com.curio.app.data.JournalGoalReminderScheduler

/**
 * v440 — THE EVENING NUDGE BEHIND THE JOURNAL GOAL.
 *
 * The member: *"Word count goal with a daily reminder"*. This is the reminder —
 * its own channel and its own notification, so a member can keep the shuffle
 * nudge and drop the writing one (or the other way round) without losing both.
 *
 * **IT DOES NOT READ THE DATABASE, ON PURPOSE.** A broadcast can arrive in a cold
 * process, and the personal store is initialized by `MainActivity` (see
 * `PersonalRepositoryHolder`), whose own contract is to fail loudly when it has
 * not been — so a receiver that reached for the day's word count would either
 * need to bring the database up in the background or crash on a quiet phone. The
 * goal itself is a PREFERENCE and is read synchronously here; the day's progress
 * is a fact the APP shows the member (the journals head), not something this
 * nudge pretends to know.
 *
 * If the goal has been removed between arming and firing, nothing is posted at
 * all: a nudge about a goal the member no longer has is exactly the notification
 * people turn off for good.
 */
class JournalGoalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val goal = AppPreferences.getJournalGoal(context)
        if (goal > 0 && AppPreferences.isJournalGoalReminderEnabled(context)) {
            JournalGoalReminderScheduler.schedule(
                context,
                AppPreferences.getJournalGoalTime().first,
                AppPreferences.getJournalGoalTime().second
            )
        }
        if (goal <= 0) return
        createChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            4111,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Today's page is waiting")
            .setContentText("A day for your journal. Your goal is $goal words.")
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
                "Writing goal",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "An evening nudge when a journal goal is set."
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "journal_goal_reminder"
        const val NOTIFICATION_ID = 4112
    }
}

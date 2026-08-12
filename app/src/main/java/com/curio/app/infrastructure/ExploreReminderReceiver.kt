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
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.reflectionQuestion
import com.curio.app.navigation.PendingEntryOpen

/**
 * Two jobs for the explore-session flow:
 *  1. When the reminder alarm fires — a nudge that the recommended explore
 *     time is up: "Done exploring <topic>? If you are, write it down."
 *  2. When a notification action is tapped — "Done exploring" clears the
 *     session and hands the user to the write-it-down page; "Cancel"
 *     clears it quietly (no navigation). Both cancel the alarm and stop
 *     the timer service.
 *
 * The notification body tap also opens the write-it-down page: the nudge
 * promises "come back and write it down", so tapping it lands on that
 * action for the topic it is nudging about (not plain Home).
 */
class ExploreReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_STOP || intent?.action == ACTION_CANCEL) {
            // "Done exploring" needs the session for its navigation, so grab
            // it BEFORE the teardown clears it (a cleared session would make
            // the write-it-down jump a silent no-op). Shared teardown: clear
            // the session, cancel the reminder alarm and stop the timer
            // service. The two actions differ only in whether the user is
            // handed to the write-it-down page (only "Done exploring"
            // navigates).
            val session = ExploreSessionStore.getActiveSession(context)
            // v27 — the shared note + captured screenshots must survive the
            // teardown: hand them off (with the pause-aware elapsed time) to
            // the write package BEFORE clearing the session, so the
            // write-it-down page can attach them to the entry.
            if (session != null && intent.action == ACTION_STOP) {
                ExploreSessionStore.handoffWriteSession(
                    context = context,
                    categoryId = session.categoryId,
                    topicName = session.topicName,
                    elapsedMillis = session.elapsedMillis(),
                    note = session.note,
                    screenshots = session.screenshotPaths
                )
            }
            ExploreSessionStore.clearSession(context)
            ExploreReminderScheduler.cancel(context)
            ExploreSessionService.stop(context)
            if (intent.action == ACTION_STOP) {
                if (session != null) {
                    // "Done exploring" — hand the user straight to the
                    // write-it-down entry page for the topic, so the action
                    // lands somewhere useful instead of just dismissing the
                    // shade. The NavHost opens the page with HOME anchored
                    // beneath it, so Back from the entry page returns to the
                    // app instead of exiting it.
                    val open = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(PendingEntryOpen.EXTRA_CATEGORY_SLUG, session.categoryId.routeSlug)
                        putExtra(PendingEntryOpen.EXTRA_TOPIC_NAME, session.topicName)
                    }
                    context.startActivity(open)
                }
            }
            // ACTION_CANCEL: teardown only — no navigation. The notification
            // disappears with the service stop, so the shade is clean.
            return
        }

        // No active session → the reminder is stale, drop it silently.
        val session = ExploreSessionStore.getActiveSession(context) ?: return
        createChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            4212,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // The nudge text says "come back and write it down" — make the
                // tap land on that action: the write-it-down entry page for
                // this topic, not plain Home.
                putExtra(PendingEntryOpen.EXTRA_CATEGORY_SLUG, session.categoryId.routeSlug)
                putExtra(PendingEntryOpen.EXTRA_TOPIC_NAME, session.topicName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Done exploring ${session.topicName}?")
            .setContentText("If you're finished, come back and write it down: ${session.verb} ${session.targetName}.")
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // The category-flavored reflection question — "Finished listening?
            // What track or lyric landed hardest?" — rides under the nudge
            // when the reminder is expanded, so the wrap-up prompt leaves the
            // user with something concrete to write about.
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "If you're finished, come back and write it down: ${session.verb} ${session.targetName}.\n" +
                            session.reflectionQuestion()
                    )
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Explore reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you when the recommended explore time is up."
            }
        )
    }

    companion object {
        const val ACTION_STOP = "com.curio.app.action.STOP_EXPLORE_SESSION"
        const val ACTION_CANCEL = "com.curio.app.action.CANCEL_EXPLORE_SESSION"
        const val CHANNEL_ID = "explore_reminders"
        const val NOTIFICATION_ID = 4213
    }
}

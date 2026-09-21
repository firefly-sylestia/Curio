package com.curio.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.curio.app.infrastructure.JournalGoalReminderReceiver
import java.util.Calendar

/**
 * v440 — THE ALARM BEHIND THE JOURNAL GOAL'S NUDGE.
 *
 * The member: *"Word count goal with a daily reminder"*. The goal itself is a
 * preference (see `AppPreferences.getJournalGoal`), and a goal nobody is reminded
 * of is a number the member has to remember to go and check — so it gets its own
 * nudge, on the same terms as the daily shuffle reminder beside it.
 *
 * It is a SECOND alarm rather than a second line in the shuffle reminder's
 * notification, and deliberately so: the two nudge different acts (go and
 * shuffle / sit down and write), and a member who wants one of them is not
 * thereby asking for the other. Its request code and its receiver are its own,
 * which is what keeps the two cancellable independently — sharing a request code
 * would have made cancelling one cancel both.
 *
 * One inexact alarm at a time, and the receiver re-arms the next local day, so a
 * DST change or a timezone move cannot make it drift (the same shape
 * [DailyReminderScheduler] uses).
 */
object JournalGoalReminderScheduler {
    private const val REQUEST_CODE = 4110

    fun schedule(context: Context, hour: Int, minute: Int = 0) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context))

        val firstTrigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            firstTrigger.timeInMillis,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, JournalGoalReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

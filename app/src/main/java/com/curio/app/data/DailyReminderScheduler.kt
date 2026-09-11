package com.curio.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import com.curio.app.infrastructure.DailyReminderReceiver

/**
 * Owns the alarm behind Curio's daily spin reminder.
 *
 * The preference alone is not enough: Android alarms are separately managed
 * system state, so every toggle/time change must update both stores. This uses
 * one inexact alarm at a time; the receiver schedules the next local-day alarm
 * after delivery so DST and timezone changes cannot make the reminder drift.
 */
object DailyReminderScheduler {
    private const val REQUEST_CODE = 4107

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
            Intent(context, DailyReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

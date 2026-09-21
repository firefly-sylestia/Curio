package com.curio.app.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.curio.app.data.AppPreferences
import com.curio.app.data.DailyReminderScheduler
import com.curio.app.data.JournalGoalReminderScheduler

/** Re-schedules the daily nudge after reboot or a local clock/timezone change. */
class DailyReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (AppPreferences.isReminderEnabled(context)) {
            DailyReminderScheduler.schedule(
                context,
                AppPreferences.getReminderHour(context),
                AppPreferences.getReminderMinute(context)
            )
        }
        // v440 — and the writing goal's own nudge, which is a SEPARATE alarm and so
        // has to be re-armed separately: a reboot drops every alarm the app holds,
        // and a goal that stopped nudging after a restart would read as a broken
        // setting rather than as a reboot.
        if (AppPreferences.isJournalGoalReminderEnabled(context)) {
            val (hour, minute) = AppPreferences.getJournalGoalTime()
            JournalGoalReminderScheduler.schedule(context, hour, minute)
        }
    }
}

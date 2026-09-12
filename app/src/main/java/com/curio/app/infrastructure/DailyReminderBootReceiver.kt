package com.curio.app.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.curio.app.data.AppPreferences
import com.curio.app.data.DailyReminderScheduler

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
    }
}

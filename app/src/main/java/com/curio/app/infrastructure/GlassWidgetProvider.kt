package com.curio.app.infrastructure

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.curio.app.MainActivity
import com.curio.app.R
import com.curio.app.data.StreakTracker

/**
 * v267 — THE LIQUID-GLASS HOME-SCREEN WIDGET. A frosted glass tile that
 * lives on the launcher over the wallpaper: light catches the top of the
 * pane, a bright rim defines it, and it shows the user's live streak.
 * Tapping it opens Curio.
 *
 * Honest scope: RemoteViews widgets render in the LAUNCHER's process with no
 * backdrop API — per-pixel wallpaper refraction is impossible for a real
 * widget (that stays a lab-only trick; see GlassWidgetLabScreen). So this
 * widget bakes the glass LOOK into layered drawables and carries real data.
 *
 * Updates: on the system's periodic tick (~3h), on every app open (the
 * provider receives APPWIDGET_UPDATE via the standard dispatch), and any
 * time [pushAll] is called after data changes.
 */
class GlassWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateAppWidget(context, manager, id) }
    }

    override fun onEnabled(context: Context) = pushAll(context)

    override fun onDisabled(context: Context) { /* nothing to clean up */ }

    companion object {
        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.glass_widget_layout)
            val streak = runCatching { StreakTracker.getStreak(context) }.getOrDefault(0)
            views.setTextViewText(
                R.id.glass_widget_stats,
                if (streak > 0) "$streak-day streak" else "start exploring"
            )
            views.setOnClickPendingIntent(
                R.id.glass_widget_root,
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            manager.updateAppWidget(id, views)
        }

        /** Re-renders every placed glass widget (call after data changes). */
        fun pushAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GlassWidgetProvider::class.java))
            ids.forEach { id -> updateAppWidget(context, manager, id) }
        }
    }
}

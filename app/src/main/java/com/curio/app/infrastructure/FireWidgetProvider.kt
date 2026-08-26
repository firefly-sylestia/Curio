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
 * v292 — FIRE (streak) home widget, straight from the Glass Widget Lab's
 * streak shape: a small round frost tile with the flame glyph and the
 * live explore count under it. Same One UI blur contract as
 * [GlassWidgetProvider] / [AnalogClockWidgetProvider]: plain translucent
 * root color for Samsung's wallpaper-blur detection + circular outline
 * clip; circular drawable bucket on other launchers.
 */
class FireWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateAppWidget(context, manager, id) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: android.os.Bundle
    ) {
        updateAppWidget(context, manager, id)
    }

    companion object {
        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.fire_widget_layout)

            val opts = manager.getAppWidgetOptions(id)
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 56)
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56)
            AnalogClockWidgetProvider.applyCircleShape(views, minOf(wDp, hDp) / 2f)

            views.setImageViewBitmap(
                R.id.fire_widget_icon,
                GlassWidgetPane.renderIcon(context, "local_fire_department")
            )
            val streak = runCatching { StreakTracker.getStreak(context) }.getOrDefault(0)
            views.setTextViewText(R.id.fire_widget_count, if (streak > 0) "$streak" else "0")

            views.setOnClickPendingIntent(
                android.R.id.background,
                PendingIntent.getActivity(
                    context,
                    2,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            manager.updateAppWidget(id, views)
        }

        /** Re-renders every placed fire widget (call after data changes). */
        fun pushAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FireWidgetProvider::class.java))
            ids.forEach { id -> updateAppWidget(context, manager, id) }
        }
    }
}

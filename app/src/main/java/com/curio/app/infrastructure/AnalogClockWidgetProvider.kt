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

/**
 * v292 — ROUND ANALOG CLOCK home widget, straight from the Glass Widget
 * Lab's analog shape: a circular frost tile with real ticking hands.
 *
 * The clock face is the system's remote-rendered <AnalogClock> view — it
 * ticks in the LAUNCHER's process with zero per-minute updates from us.
 * The Samsung blur comes from the same contract as [GlassWidgetProvider]:
 * the ROOT view keeps a plain translucent color background (that is what
 * One UI Home's wallpaper-blur detection triggers on) and we clip the root
 * to a CIRCLE outline so the blur tile reads round. On non-Samsung
 * launchers (no native blur to lose) the square tint swaps for a circular
 * drawable bucket instead.
 */
class AnalogClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateAppWidget(context, manager, id) }
    }

    /** Re-clip to a circle whenever the user resizes the widget. */
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
            val views = RemoteViews(context.packageName, R.layout.glass_analog_clock_layout)

            // Circle clip: outline radius = half of the smaller cell side.
            val opts = manager.getAppWidgetOptions(id)
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            applyCircleShape(views, minOf(wDp, hDp) / 2f)

            views.setOnClickPendingIntent(
                android.R.id.background,
                PendingIntent.getActivity(
                    context,
                    1,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            manager.updateAppWidget(id, views)
        }

        /**
         * Round the WIDGET ITSELF. Same contract as
         * [GlassWidgetProvider.applyCornerShape]: on Samsung keep the plain
         * translucent root (One UI blur detection) and clip via outline;
         * elsewhere swap in the circular drawable bucket.
         */
        fun applyCircleShape(views: RemoteViews, radiusDp: Float) {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                views.setBoolean(android.R.id.background, "setClipToOutline", true)
                views.setViewOutlinePreferredRadius(
                    android.R.id.background,
                    radiusDp,
                    android.util.TypedValue.COMPLEX_UNIT_DIP
                )
            }
            val samsung = android.os.Build.MANUFACTURER.contains("samsung", true)
            if (!samsung) {
                views.setInt(
                    android.R.id.background,
                    "setBackgroundResource",
                    R.drawable.glass_widget_root_circle
                )
            }
        }

        /** Re-renders every placed analog clock widget. */
        fun pushAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AnalogClockWidgetProvider::class.java))
            ids.forEach { id -> updateAppWidget(context, manager, id) }
        }
    }
}

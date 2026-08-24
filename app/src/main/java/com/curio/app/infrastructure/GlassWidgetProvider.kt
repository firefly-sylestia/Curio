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
import com.curio.app.data.CurioDatabase
import com.curio.app.data.CurioQuests
import com.curio.app.data.StreakTracker
import kotlinx.coroutines.runBlocking

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
/** v272 — what a glass widget shows. Persisted PER WIDGET ID. */
enum class GlassWidgetMode(val label: String, val description: String) {
    STREAK("Streak", "Your live explore streak."),
    QUESTS("Quests", "Level and XP progress."),
    CABINET("Cabinet", "How many discoveries you've saved."),
    SESSIONS("Sessions", "Live or queued explore sessions.")
}

class GlassWidgetProvider : AppWidgetProvider() {

    /** Clean up per-widget config when a widget is removed from the host. */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
        appWidgetIds.forEach { id ->
            prefs.edit().remove("mode_$id").remove("style_$id")
                .remove("customColor_$id").remove("customOpacity_$id").apply()
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateAppWidget(context, manager, id) }
    }

    override fun onEnabled(context: Context) = pushAll(context)

    override fun onDisabled(context: Context) { /* nothing to clean up */ }

    companion object {
        internal const val CONFIG_PREFS = "glass_widget_config"

        fun readMode(context: Context, id: Int): GlassWidgetMode =
            runCatching {
                GlassWidgetMode.valueOf(
                    context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
                        .getString("mode_$id", null) ?: GlassWidgetMode.STREAK.name
                )
            }.getOrDefault(GlassWidgetMode.STREAK)

        fun writeMode(context: Context, id: Int, mode: GlassWidgetMode) {
            context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
                .edit().putString("mode_$id", mode.name).apply()
        }

        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.glass_widget_layout)
            // v274 - per-widget CUSTOMIZABLE pane: rendered bitmap from the
            // user's preset / custom color + opacity, sized to the placed
            // widget via the launcher's options.
            val opts = manager.getAppWidgetOptions(id)
            val density = context.resources.displayMetrics.density
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56)
            val style = GlassWidgetPane.readStyle(context, id)
            if (style == GlassWidgetPane.STYLE_DEFAULT) {
                // v275 - pure One UI look: launcher wallpaper blur + the root
                // tint only. No pane bitmap in the way.
                views.setViewVisibility(R.id.glass_widget_pane, android.view.View.GONE)
            } else {
                views.setViewVisibility(R.id.glass_widget_pane, android.view.View.VISIBLE)
                val (top, bottom) = GlassWidgetPane.resolveColors(context, id, style)
                views.setImageViewBitmap(
                    R.id.glass_widget_pane,
                    GlassWidgetPane.render(
                        widthPx = (wDp * density).toInt(),
                        heightPx = (hDp * density * 0.9f).toInt(),
                        cornerPx = 28f * density,
                        top = top, bottom = bottom
                    )
                )
            }
            val (title, stats) = resolveContent(context, readMode(context, id))
            views.setTextViewText(R.id.glass_widget_title, title)
            views.setTextViewText(R.id.glass_widget_stats, stats)
            views.setOnClickPendingIntent(
                // The layout root IS @android:id/background (required by the
                // One UI wallpaper-blur spec) — so that's the clickable target.
                android.R.id.background,
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

        /**
         * v272 — per-mode content. Reads raw prefs (NOT hydrated singletons)
         * so values are correct even when the widget updates in a cold
         * process where the app UI never ran.
         */
        private fun resolveContent(context: Context, mode: GlassWidgetMode): Pair<String, String> =
            runCatching {
                when (mode) {
                    GlassWidgetMode.STREAK -> {
                        val streak = StreakTracker.getStreak(context)
                        "Curio" to if (streak > 0) "$streak-day streak" else "start exploring"
                    }
                    GlassWidgetMode.QUESTS -> {
                        val xp = context.getSharedPreferences("curio_quests", Context.MODE_PRIVATE)
                            .getInt("xp", 0)
                        val level = CurioQuests.levelForXp(xp)
                        "Quests" to "Lv $level \u00b7 $xp XP"
                    }
                    GlassWidgetMode.CABINET -> {
                        // Room call off the main thread; widget updates are
                        // broadcast-receiver work so a short blocking read is
                        // the pragmatic pattern here (GoAsync overkill).
                        val count = runBlocking {
                            CurioDatabase.getInstance(context).captureDao().count()
                        }
                        "Cabinet" to "$count saved"
                    }
                    GlassWidgetMode.SESSIONS -> {
                        val prefs = context.getSharedPreferences("curio_prefs", Context.MODE_PRIVATE)
                        val active = prefs.getString("explore_active_session", null) != null
                        val queued = prefs.getString("explore_queued_sessions", null)?.let { raw ->
                            runCatching {
                                org.json.JSONArray(raw).length()
                            }.getOrDefault(0)
                        } ?: 0
                        "Explore" to when {
                            active -> "session live"
                            queued > 0 -> "$queued queued"
                            else -> "no session"
                        }
                    }
                }
            }.getOrDefault("Curio" to "")
    }
}

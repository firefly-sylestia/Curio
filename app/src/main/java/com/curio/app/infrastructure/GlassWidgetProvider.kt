package com.curio.app.infrastructure

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
enum class GlassWidgetMode(val label: String, val description: String, val glyph: String) {
    STREAK("Streak", "Your live explore streak.", "local_fire_department"),
    QUESTS("Quests", "Level and XP progress.", "emoji_events"),
    CABINET("Cabinet", "How many discoveries you've saved.", "auto_stories"),
    SESSIONS("Sessions", "Live or queued explore sessions.", "explore")
}

class GlassWidgetProvider : AppWidgetProvider() {

    /** Clean up per-widget config when a widget is removed from the host. */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
        appWidgetIds.forEach { id ->
            prefs.edit().remove("mode_$id").remove("style_$id").remove("corner_$id")
                .remove("customColor_$id").remove("customOpacity_$id").apply()
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateAppWidget(context, manager, id) }
    }

    /**
     * v285 — RE-RENDER ON RESIZE. Without this the pane bitmap stayed at
     * the placement-time size and the host stretched it (fitXY), which is
     * what made an expanded pill's fill look square/wrong.
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: android.os.Bundle
    ) {
        updateAppWidget(context, manager, id)
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
            val opts = manager.getAppWidgetOptions(id)
            val density = context.resources.displayMetrics.density
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56)
            val wPx = (wDp * density).toInt()
            val hPx = (hDp * density).toInt()
            val style = GlassWidgetPane.readStyle(context, id)
            val cornerDp = GlassWidgetPane.readCorner(context, id)
            applyCornerShape(views, cornerDp)

            // Default style = pure One UI look: launcher blur + root tint only.
            // No pane bitmap — the native blur shines through.
            if (style == GlassWidgetPane.STYLE_DEFAULT) {
                views.setViewVisibility(R.id.glass_widget_pane, android.view.View.GONE)
                // Still apply corner rounding so the widget outline matches
                applyCornerShape(views, cornerDp)
                val (glyph, title, stats) = resolveContent(context, readMode(context, id))
                views.setImageViewBitmap(R.id.glass_widget_icon, GlassWidgetPane.renderIcon(context, glyph))
                views.setTextViewText(R.id.glass_widget_title, title)
                views.setTextViewText(R.id.glass_widget_stats, stats)
                views.setOnClickPendingIntent(
                    android.R.id.background,
                    PendingIntent.getActivity(context, 0,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
                manager.updateAppWidget(id, views)
                return
            }

            views.setViewVisibility(R.id.glass_widget_pane, android.view.View.VISIBLE)

            val (top, bottom) = GlassWidgetPane.resolveColors(context, id, style)
            val paneBmp = GlassWidgetPane.render(
                widthPx = wPx, heightPx = hPx,
                cornerPx = cornerDp * density,
                top = top, bottom = bottom
            )

            views.setImageViewBitmap(R.id.glass_widget_pane, paneBmp)

            val (glyph, title, stats) = resolveContent(context, readMode(context, id))
            views.setImageViewBitmap(
                R.id.glass_widget_icon,
                GlassWidgetPane.renderIcon(context, glyph)
            )
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

        /**
         * v285 — round the WIDGET ITSELF, not just the pane bitmap.
         * The root must keep its plain translucent color for One UI's blur
         * detection, so on Samsung we clip the view to an outline of the
         * chosen radius (API 31+, which every One UI 7 device is). On other
         * launchers there is no blur to lose, so we additionally swap the
         * square tint for a rounded drawable bucket — that's what fixes
         * "the radius only changes the color fill, not the widget" there.
         */
        fun applyCornerShape(views: RemoteViews, cornerDp: Float) {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                views.setBoolean(
                    android.R.id.background, "setClipToOutline", true
                )
                views.setViewOutlinePreferredRadius(
                    android.R.id.background,
                    cornerDp,
                    android.util.TypedValue.COMPLEX_UNIT_DIP
                )
            }
            val samsung = android.os.Build.MANUFACTURER.contains("samsung", true)
            if (!samsung) {
                val res = when {
                    cornerDp <= 16f -> R.drawable.glass_widget_root_r12
                    cornerDp <= 24f -> R.drawable.glass_widget_root_r20
                    cornerDp <= 32f -> R.drawable.glass_widget_root_r28
                    else -> R.drawable.glass_widget_root_r36
                }
                views.setInt(android.R.id.background, "setBackgroundResource", res)
            }
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
        private fun resolveContent(
            context: Context,
            mode: GlassWidgetMode
        ): Triple<String, String, String> =
            runCatching {
                when (mode) {
                    GlassWidgetMode.STREAK -> {
                        val streak = StreakTracker.getStreak(context)
                        Triple(mode.glyph, "Curio", if (streak > 0) "$streak-day explore streak" else "start exploring today")
                    }
                    GlassWidgetMode.QUESTS -> {
                        val xp = context.getSharedPreferences("curio_quests", Context.MODE_PRIVATE)
                            .getInt("xp", 0)
                        val level = CurioQuests.levelForXp(xp)
                        Triple(mode.glyph, "Level $level", "$xp quest XP earned")
                    }
                    GlassWidgetMode.CABINET -> {
                        // Room call off the main thread; widget updates are
                        // broadcast-receiver work so a short blocking read is
                        // the pragmatic pattern here (GoAsync overkill).
                        val count = runBlocking {
                            CurioDatabase.getInstance(context).captureDao().count()
                        }
                        Triple(mode.glyph, "Cabinet", if (count > 0) "$count saved discoveries" else "nothing saved yet")
                    }
                    GlassWidgetMode.SESSIONS -> {
                        val prefs = context.getSharedPreferences("curio_prefs", Context.MODE_PRIVATE)
                        val active = prefs.getString("explore_active_session", null) != null
                        val queued = prefs.getString("explore_queued_sessions", null)?.let { raw ->
                            runCatching {
                                org.json.JSONArray(raw).length()
                            }.getOrDefault(0)
                        } ?: 0
                        Triple(mode.glyph, "Explore", when {
                            active -> "session live right now"
                            queued > 0 -> "$queued queued sessions"
                            else -> "no sessions yet"
                        })
                    }
                }
            }.getOrDefault(Triple("local_fire_department", "Curio", ""))
    }
}

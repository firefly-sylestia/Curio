package com.curio.app.infrastructure

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v279 — PERSISTED COMPOSITION for the Glass Widget Lab live wallpaper.
 * The lab serializes every visible shape (relative position, size, blur,
 * text color); [GlassLabWallpaperService] reads it back in the wallpaper
 * process and bakes the same frosted panes over the wallpaper.
 */
object GlassLabComposition {

    data class Shape(
        val id: String,
        val xFrac: Float,
        val yFrac: Float,
        val scale: Float,
        val textArgb: Int,
        val blurDp: Float = 8f,
        val visible: Boolean = true
    )

    private const val PREFS = "glass_lab"
    private const val KEY = "composition"

    fun saveCanvasSize(context: Context, w: Int, h: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("canvasW", w).putInt("canvasH", h).apply()
    }

    fun canvasSize(context: Context): Pair<Int, Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getInt("canvasW", 0) to p.getInt("canvasH", 0)
    }

    fun save(context: Context, shapes: List<Shape>) {
        val arr = JSONArray()
        shapes.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("x", s.xFrac.toDouble())
                    .put("y", s.yFrac.toDouble())
                    .put("scale", s.scale.toDouble())
                    .put("text", s.textArgb)
                    .put("blur", s.blurDp.toDouble())
                    .put("visible", s.visible)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun load(context: Context): List<Shape> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Shape(
                    id = o.getString("id"),
                    xFrac = o.getDouble("x").toFloat(),
                    yFrac = o.getDouble("y").toFloat(),
                    scale = o.getDouble("scale").toFloat(),
                    textArgb = o.getInt("text"),
                    blurDp = if (o.has("blur")) o.getDouble("blur").toFloat() else 8f,
                    visible = if (o.has("visible")) o.getBoolean("visible") else true
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Display content each shape id renders in the baked wallpaper. */
    fun titleFor(id: String): String = when (id) {
        "clock" -> "12:34"
        "timer" -> "Exploring"
        "streak" -> "5"
        "battery" -> "80%"
        "date" -> "Wed · Aug 24"
        // Analog draws real hands — no text.
        else -> ""
    }
}

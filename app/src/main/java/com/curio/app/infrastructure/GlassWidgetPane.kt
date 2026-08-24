package com.curio.app.infrastructure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * v274 — USER-CUSTOMIZABLE widget pane. Replaces the two static drawables
 * with ONE programmatic renderer so every visual knob lives in prefs:
 *
 *   "style_$id"         preset name or "custom"
 *   "customColor_$id"   ARGB chosen by hue slider (custom mode)
 *   "customOpacity_$id" 0..1 slider value    (custom mode)
 *
 * The pane bitmap is drawn into an ImageView inside the widget layout; the
 * ROOT view keeps a near-invisible rounded tint purely to satisfy One UI's
 * wallpaper-blur detection (root background alpha must be 1..254).
 */
object GlassWidgetPane {

    private const val CONFIG_PREFS = "glass_widget_config"

    enum class Preset(val label: String, val top: Int, val bottom: Int, val rim: Int) {
        LIGHT("Light", 0x59FFFFFF.toInt(), 0x2E3A3A44.toInt(), 0xA6FFFFFF.toInt()),
        DARK("Dark", 0x73232430.toInt(), 0x99101118.toInt(), 0x8CFFFFFF.toInt()),
        CLEAR("Clear", 0x26FFFFFF.toInt(), 0x1FFFFFFF.toInt(), 0x66FFFFFF.toInt()),
        ROSE("Rose", 0x8CFF7BAF.toInt(), 0x737C4D8C.toInt(), 0xB3FFFFFF.toInt()),
        SKY("Sky", 0x8C81D4FA.toInt(), 0x734A90D9.toInt(), 0xB3FFFFFF.toInt())
    }

    const val STYLE_CUSTOM = "custom"

    /**
     * v275 - DEFAULT: no pane bitmap at all. The widget shows only its
     * translucent root tint over One UI's launcher-rendered wallpaper blur -
     * the original device-verified look. Any preset/custom pane still lets
     * the blur show through (all fills stay translucent).
     */
    const val STYLE_DEFAULT = "default"

    fun readStyle(context: Context, id: Int): String =
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .getString("style_$id", null) ?: STYLE_DEFAULT

    fun writeStyle(context: Context, id: Int, style: String) {
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .edit().putString("style_$id", style).apply()
    }

    fun readCustomColor(context: Context, id: Int): Int =
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .getInt("customColor_$id", 0xFF7BAFFF.toInt())

    fun writeCustomColor(context: Context, id: Int, argb: Int) {
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .edit().putInt("customColor_$id", argb).apply()
    }

    fun readCorner(context: Context, id: Int): Float =
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .getFloat("corner_$id", 28f)

    fun writeCorner(context: Context, id: Int, cornerDp: Float) {
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .edit().putFloat("corner_$id", cornerDp).apply()
    }

    fun readCustomOpacity(context: Context, id: Int): Float =
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .getFloat("customOpacity_$id", 0.45f)

    fun writeCustomOpacity(context: Context, id: Int, opacity: Float) {
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .edit().putFloat("customOpacity_$id", opacity).apply()
    }

    /** Resolve (top, bottom) gradient colors for the current style. */
    fun resolveColors(
        context: Context,
        id: Int,
        style: String
    ): Pair<Int, Int> {
        if (style == STYLE_CUSTOM) {
            return gradientColors(
                readCustomColor(context, id),
                readCustomOpacity(context, id)
            )
        }
        val preset = runCatching { Preset.valueOf(style) }.getOrDefault(Preset.LIGHT)
        return Pair(preset.top, preset.bottom)
    }

    /**
     * v276 - renders a MODE ICON tile: translucent circle + a white Material
     * Symbols glyph (ligature drawn straight from the bundled symbols font),
     * so the widget carries a visual anchor next to its text.
     */
    fun renderIcon(context: Context, glyph: String, sizePx: Int = 160): android.graphics.Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Translucent circle backing.
        paint.color = 0x40FFFFFF
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - sizePx * 0.04f, paint)
        // Glyph, centered.
        val typeface = androidx.core.content.res.ResourcesCompat.getFont(
            context, com.curio.app.R.font.material_symbols_outlined
        )
        paint.color = Color.WHITE
        paint.typeface = typeface
        paint.textSize = sizePx * 0.52f
        paint.textAlign = Paint.Align.CENTER
        val fm = paint.fontMetrics
        val baseline = sizePx / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(glyph, sizePx / 2f, baseline, paint)
        return bmp
    }

    /**
     * v277 - SINGLE source of truth for custom-pane gradient stops: top =
     * base at full opacity alpha; bottom = same hue darkened 30% with
     * slightly lower alpha. The config preview calls this too, so what you
     * see while picking is identical to the rendered widget.
     */
    fun gradientColors(baseRgb: Int, opacity: Float): Pair<Int, Int> {
        val base = baseRgb or 0xFF000000.toInt()
        val alpha = (opacity * 255).toInt().coerceIn(8, 235)
        val top = (alpha shl 24) or (base and 0x00FFFFFF.toInt())
        val r = (Color.red(base) * 0.70f).toInt()
        val g = (Color.green(base) * 0.70f).toInt()
        val b = (Color.blue(base) * 0.70f).toInt()
        val bottom = ((alpha * 0.85f).toInt() shl 24) or (r shl 16) or (g shl 8) or b
        return Pair(top, bottom)
    }

    /**
     * Renders the frosted pane. [cornerPx] follows the layout's 28dp recipe;
     * [widthPx]/[heightPx] come from the launcher's widget options so the
     * pane matches the placed size exactly.
     */
    fun render(
        widthPx: Int,
        heightPx: Int,
        cornerPx: Float,
        top: Int,
        bottom: Int
    ): Bitmap {
        val w = widthPx.coerceIn(64, 1200)
        val h = heightPx.coerceIn(48, 600)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        }
        // v275 - NO rim stroke: the baked white border read as a harsh box
        // edge on-device. The gradient fill alone reads as clean glass.
        val r = cornerPx.coerceAtMost(w / 2f).coerceAtMost(h / 2f)
        canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, paint)
        return bmp
    }
}

package com.curio.app.infrastructure

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.min

/**
 * v280 — CUSTOM BLUR ENGINE for Curio.
 *
 * A self-contained blur API that does NOT depend on:
 * - Samsung One UI launcher blur
 * - Android RenderEffect view-level API
 * - Any window-level or system-level API
 *
 * Usage:
 *   val blurred = CurioBlur.blur(wallpaperBitmap, 16f)
 *
 * Implementation: two-pass separable box blur, 3 iterations ≈ gaussian.
 * ~80ms for a 1080×1920 bitmap on a modern CPU.
 */
object CurioBlur {

    /**
     * Blur a bitmap with a gaussian-like kernel of the given [radius] (in pixels).
     * Radius is clamped to 30. Returns a new [Bitmap]; the source is untouched.
     */
    fun blur(src: Bitmap, radius: Float): Bitmap {
        val r = radius.toInt().coerceIn(1, 30)
        if (r <= 0) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        return boxBlur(src, r)
    }

    // ── Two-pass separable box blur (3× ≈ gaussian) ──────────────────

    /**
     * Separable box blur: horizontal pass then vertical pass, repeated 3×.
     * The sliding-window approach avoids recomputing the sum per pixel.
     * Each edge pixel is clamped to the nearest valid coordinate (mirrors
     * the behavior of CLAMP tile mode).
     */
    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src
        val wm = w - 1
        val hm = h - 1
        val div = radius * 2 + 1

        val a = IntArray(w * h)
        src.getPixels(a, 0, w, 0, 0, w, h)
        val b = IntArray(w * h)

        repeat(3) {
            // ── Horizontal: a → b ──
            for (y in 0 until h) {
                var aS = 0; var rS = 0; var gS = 0; var bS = 0
                // Seed window for x = 0
                for (k in -radius..radius) {
                    val p = a[y * w + k.coerceIn(0, wm)]
                    aS += (p shr 24) and 0xff
                    rS += (p shr 16) and 0xff
                    gS += (p shr 8) and 0xff
                    bS += p and 0xff
                }
                for (x in 0 until w) {
                    b[y * w + x] = ((aS / div) shl 24) or
                        ((rS / div) shl 16) or ((gS / div) shl 8) or (bS / div)
                    // Slide window: drop leftmost, add rightmost
                    val drop = a[y * w + (x - radius).coerceIn(0, wm)]
                    val add = a[y * w + (x + radius + 1).coerceIn(0, wm)]
                    aS += ((add shr 24) and 0xff) - ((drop shr 24) and 0xff)
                    rS += ((add shr 16) and 0xff) - ((drop shr 16) and 0xff)
                    gS += ((add shr 8) and 0xff) - ((drop shr 8) and 0xff)
                    bS += (add and 0xff) - (drop and 0xff)
                }
            }
            // ── Vertical: b → a ──
            for (x in 0 until w) {
                var aS = 0; var rS = 0; var gS = 0; var bS = 0
                for (k in -radius..radius) {
                    val p = b[k.coerceIn(0, hm) * w + x]
                    aS += (p shr 24) and 0xff
                    rS += (p shr 16) and 0xff
                    gS += (p shr 8) and 0xff
                    bS += p and 0xff
                }
                for (y in 0 until h) {
                    a[y * w + x] = ((aS / div) shl 24) or
                        ((rS / div) shl 16) or ((gS / div) shl 8) or (bS / div)
                    val drop = b[(y - radius).coerceIn(0, hm) * w + x]
                    val add = b[(y + radius + 1).coerceIn(0, hm) * w + x]
                    aS += ((add shr 24) and 0xff) - ((drop shr 24) and 0xff)
                    rS += ((add shr 16) and 0xff) - ((drop shr 16) and 0xff)
                    gS += ((add shr 8) and 0xff) - ((drop shr 8) and 0xff)
                    bS += (add and 0xff) - (drop and 0xff)
                }
            }
        }

        val result = Bitmap.createBitmap(w, h, src.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(a, 0, w, 0, 0, w, h)
        return result
    }

    // ── Wallpaper reading helper ──────────────────────────────────────

    /** Read the device wallpaper as a Bitmap, or null on failure. */
    fun readDeviceWallpaper(context: android.content.Context): Bitmap? {
        return try {
            val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
            @Suppress("DEPRECATION")
            val d = wm.defaultDisplay
            @Suppress("DEPRECATION")
            val w = d?.width ?: 1080
            @Suppress("DEPRECATION")
            val h = d?.height ?: 1920
            android.app.WallpaperManager.getInstance(context).drawable?.let { drawable ->
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(c)
                bmp
            }
        } catch (_: Exception) { null }
    }

    /**
     * Extract the region of [wallpaper] behind the widget and blur it.
     * Returns the blurred crop, or null if the wallpaper is unavailable.
     */
    fun blurWallpaperRegion(
        wallpaper: Bitmap,
        widgetLeft: Int, widgetTop: Int,
        widgetRight: Int, widgetBottom: Int,
        screenW: Int, screenH: Int,
        blurRadius: Float,
        density: Float
    ): Bitmap? {
        try {
            val wpScale = maxOf(screenW.toFloat() / wallpaper.width, screenH.toFloat() / wallpaper.height)
            val scaledW = (wallpaper.width * wpScale).toInt()
            val scaledH = (wallpaper.height * wpScale).toInt()
            val offX = (scaledW - screenW) / 2
            val offY = (scaledH - screenH) / 2
            val left = maxOf(0, (widgetLeft * wpScale).toInt() - offX)
            val top = maxOf(0, (widgetTop * wpScale).toInt() - offY)
            val right = minOf(wallpaper.width, (widgetRight * wpScale).toInt() - offX)
            val bottom = minOf(wallpaper.height, (widgetBottom * wpScale).toInt() - offY)
            if (right <= left || bottom <= top) return null
            val crop = Bitmap.createBitmap(wallpaper, left, top, right - left, bottom - top)
            val blurred = blur(crop, blurRadius * density)
            if (blurred !== crop) crop.recycle()
            return blurred
        } catch (_: Exception) { return null }
    }
}

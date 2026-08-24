package com.curio.app.infrastructure

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.exp
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
 * API tier:
 * - All API levels: CPU StackBlur — O(n) gaussian approximation, ~50ms for 1080p.
 *   (AGSL RuntimeShader was considered but requires a hardware-accelerated
 *    canvas, which bitmap-level blur can't provide.)
 */
object CurioBlur {

    /**
     * Blur a bitmap with a gaussian-like kernel of the given [radius] (in pixels).
     * Radius is clamped to 25 (the RenderScript / AGSL practical max).
     * Returns a new [Bitmap]; the source is untouched.
     */
    fun blur(src: Bitmap, radius: Float): Bitmap {
        val r = min(radius, 25f)
        if (r <= 0f) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        // AGSL RuntimeShader requires a hardware-accelerated canvas, but
        // bitmap blur always uses Canvas(bitmap) which is software-rendered.
        // CPU StackBlur is fast (~50ms for 1080p) and works everywhere.
        return stackBlur(src, r)
    }


    // ── CPU StackBlur (any API) ──────────────────────────────────────

    /**
     * StackBlur — a fast O(n) approximation of gaussian blur.
     * Produces visually identical results to gaussian at radius ≥ 3.
     * ~50ms for a 1080×1920 bitmap on a modern CPU.
     */
    private fun stackBlur(src: Bitmap, radius: Float): Bitmap {
        val r = radius.toInt()
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val div = r + r + 1
        val divSum = (div * div + 1) shr 1
        val dv = IntArray(256 * divSum)
        for (i in dv.indices) dv[i] = i / divSum

        val vMin = IntArray(maxOf(w, h))
        val vMax = IntArray(maxOf(w, h))

        val pix = IntArray(w * h)
        System.arraycopy(pixels, 0, pix, 0, w * h)

        // Stack arrays for the sliding window
        val stackSize = div
        val stack = Array(stackSize) { IntArray(3) }

        for (y in 0 until h) {
            var rSum = 0; var gSum = 0; var bSum = 0
            var rOutSum = 0; var gOutSum = 0; var bOutSum = 0
            var rInSum = 0; var gInSum = 0; var bInSum = 0

            for (i in -r..r) {
                val p = pix[minOf(y * w + minOf(wm, maxOf(i + 0, 0)), w * h - 1)]
                val stackP = stack[i + r]
                stackP[0] = (p shr 16) and 0xff
                stackP[1] = (p shr 8) and 0xff
                stackP[2] = p and 0xff
                val bs = minOf(r + 1, maxOf(0, r - i + 1))
                rSum += stackP[0] * bs; gSum += stackP[1] * bs; bSum += stackP[2] * bs
                if (i > 0) { rInSum += stackP[0]; gInSum += stackP[1]; bInSum += stackP[2] }
                if (i < wm) { rOutSum += stackP[0]; gOutSum += stackP[1]; bOutSum += stackP[2] }
            }
            var stackPointer = r
            for (x in 0 until w) {
                pix[y * w + x] = (0xff000000.toInt()) or
                    (dv[rSum] shl 16) or (dv[gSum] shl 8) or dv[bSum]

                rSum -= rOutSum; gSum -= gOutSum; bSum -= bOutSum
                val stackP = stack[(stackPointer - r + stackSize) % stackSize]
                rOutSum -= stackP[0]; gOutSum -= stackP[1]; bOutSum -= stackP[2]

                if (y == 0) { vMin[x] = minOf(x + r + 1, wm); vMax[x] = maxOf(x - r, 0) }
                val p = pix[minOf(vMax[x] + y * w, w * h - 1)]
                stackP[0] = (p shr 16) and 0xff
                stackP[1] = (p shr 8) and 0xff
                stackP[2] = p and 0xff

                rInSum += stackP[0]; gInSum += stackP[1]; bInSum += stackP[2]
                rSum += rInSum; gSum += gInSum; bSum += bInSum

                stackPointer = (stackPointer + 1) % stackSize
                val stackP2 = stack[stackPointer]
                rOutSum += stackP2[0]; gOutSum += stackP2[1]; bOutSum += stackP2[2]
                rInSum -= stackP2[0]; gInSum -= stackP2[1]; bInSum -= stackP2[2]
            }
        }

        // Vertical pass
        for (x in 0 until w) {
            var rSum = 0; var gSum = 0; var bSum = 0
            var rOutSum = 0; var gOutSum = 0; var bOutSum = 0
            var rInSum = 0; var gInSum = 0; var bInSum = 0

            for (i in -r..r) {
                val yy = minOf(hm, maxOf(0, i))
                val p = pix[yy * w + x]
                val stackP = stack[i + r]
                stackP[0] = (p shr 16) and 0xff
                stackP[1] = (p shr 8) and 0xff
                stackP[2] = p and 0xff
                val bs = minOf(r + 1, maxOf(0, r - i + 1))
                rSum += stackP[0] * bs; gSum += stackP[1] * bs; bSum += stackP[2] * bs
                if (i > 0) { rInSum += stackP[0]; gInSum += stackP[1]; bInSum += stackP[2] }
                if (i < hm) { rOutSum += stackP[0]; gOutSum += stackP[1]; bOutSum += stackP[2] }
            }
            var yp = x
            var stackPointer = r
            for (y in 0 until h) {
                pix[yp] = (0xff000000.toInt()) or
                    (dv[rSum] shl 16) or (dv[gSum] shl 8) or dv[bSum]

                rSum -= rOutSum; gSum -= gOutSum; bSum -= bOutSum
                val stackP = stack[(stackPointer - r + stackSize) % stackSize]
                rOutSum -= stackP[0]; gOutSum -= stackP[1]; bOutSum -= stackP[2]

                if (x == 0) { vMin[y] = minOf(y + r + 1, hm); vMax[y] = maxOf(y - r, 0) }
                val p = pix[minOf(x + vMax[y] * w, w * h - 1)]
                stackP[0] = (p shr 16) and 0xff
                stackP[1] = (p shr 8) and 0xff
                stackP[2] = p and 0xff

                rInSum += stackP[0]; gInSum += stackP[1]; bInSum += stackP[2]
                rSum += rInSum; gSum += gInSum; bSum += bInSum

                stackPointer = (stackPointer + 1) % stackSize
                val stackP2 = stack[stackPointer]
                rOutSum += stackP2[0]; gOutSum += stackP2[1]; bOutSum += stackP2[2]
                rInSum -= stackP2[0]; gInSum -= stackP2[1]; bInSum -= stackP2[2]

                yp += w
            }
        }

        val result = Bitmap.createBitmap(w, h, src.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(pix, 0, w, 0, 0, w, h)
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
            android.app.WallpaperManager.getInstance(context).getDrawable()?.let { drawable ->
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
            // Map widget coords to wallpaper coords (cover-fit)
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

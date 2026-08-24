package com.curio.app.infrastructure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * CurioBlur — unified blur engine. Zero system/window dependency.
 *
 * GPU path (API 33+): two-pass separable gaussian via AGSL RuntimeShader.
 * CPU path (all APIs): stack blur — O(n) per pixel, pure Kotlin.
 *
 * Usage:
 *   CurioBlur.blur(bitmap, radiusPx)  → blurred bitmap (auto-selects GPU/CPU)
 *   CurioBlur.blurWallpaperRegion(…)  → self-contained widget blur from wallpaper
 */
object CurioBlur {

    // ══════════════════════════════════════════════════════════════════
    //  GPU: AGSL separable gaussian (API 33+)
    // ══════════════════════════════════════════════════════════════════

    private const val GAUSSIAN_H = """
        uniform shader input;
        uniform float2 iResolution;
        uniform float iRadius;

        half4 main(float2 fragCoord) {
            float sigma = max(iRadius, 0.5);
            float twosigma2 = 2.0 * sigma * sigma;
            int rad = int(ceil(sigma * 2.0));
            half4 sum = half4(0.0);
            float wSum = 0.0;
            for (int i = -rad; i <= rad; i++) {
                float w = exp(-float(i * i) / twosigma2);
                sum += input.eval(float2(fragCoord.x + float(i), fragCoord.y)) * w;
                wSum += w;
            }
            return sum / wSum;
        }
    """

    private const val GAUSSIAN_V = """
        uniform shader input;
        uniform float2 iResolution;
        uniform float iRadius;

        half4 main(float2 fragCoord) {
            float sigma = max(iRadius, 0.5);
            float twosigma2 = 2.0 * sigma * sigma;
            int rad = int(ceil(sigma * 2.0));
            half4 sum = half4(0.0);
            float wSum = 0.0;
            for (int i = -rad; i <= rad; i++) {
                float w = exp(-float(i * i) / twosigma2);
                sum += input.eval(float2(fragCoord.x, fragCoord.y + float(i))) * w;
                wSum += w;
            }
            return sum / wSum;
        }
    """

    private fun blurGpu(src: Bitmap, radius: Float): Bitmap? {
        if (Build.VERSION.SDK_INT < 33) return null
        return runCatching {
            val w = src.width; val h = src.height
            // Pass 1: horizontal
            val hShader = RuntimeShader(GAUSSIAN_H)
            hShader.setInputShader("input",
                BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            hShader.setFloatUniform("iResolution", w.toFloat(), h.toFloat())
            hShader.setFloatUniform("iRadius", radius)
            val hBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(hBmp).drawRect(0f, 0f, w.toFloat(), h.toFloat(),
                Paint(Paint.FILTER_BITMAP_FLAG).apply { shader = hShader })

            // Pass 2: vertical
            val vShader = RuntimeShader(GAUSSIAN_V)
            vShader.setInputShader("input",
                BitmapShader(hBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            vShader.setFloatUniform("iResolution", w.toFloat(), h.toFloat())
            vShader.setFloatUniform("iRadius", radius)
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(out).drawRect(0f, 0f, w.toFloat(), h.toFloat(),
                Paint(Paint.FILTER_BITMAP_FLAG).apply { shader = vShader })

            hBmp.recycle()
            out
        }.getOrNull()
    }

    // ══════════════════════════════════════════════════════════════════
    //  CPU: StackBlur (Mario Klingemann, Apache-2.0 licensed)
    //  O(n) per pixel, ~4-8ms for 1080p on modern CPUs.
    // ══════════════════════════════════════════════════════════════════

    private fun blurCpu(src: Bitmap, radius: Int): Bitmap? {
        if (radius <= 0) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val r = min(radius, 250)
        val w = src.width; val h = src.height
        val bmp = src.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        val pix = IntArray(w * h)
        bmp.getPixels(pix, 0, w, 0, 0, w, h)

        val div = r + r + 1
        val divSum = div * div
        val dv = IntArray(256 * divSum)
        for (i in dv.indices) dv[i] = i / divSum

        val stack = IntArray(div)

        var p: Int
        var stackPointer: Int
        var stackStart: Int
        var rSum: Int; var gSum: Int; var bSum: Int; var aSum: Int
        var rOutSum: Int; var gOutSum: Int; bOutSum: Int; aOutSum: Int
        var rInSum: Int; var gInSum: Int; var bInSum: Int; var aInSum: Int

        // ── Horizontal pass ──
        for (y in 0 until h) {
            rInSum = 0; gInSum = 0; bInSum = 0; aInSum = 0
            rOutSum = 0; gOutSum = 0; bOutSum = 0; aOutSum = 0
            rSum = 0; gSum = 0; bSum = 0; aSum = 0

            p = y * w
            // Fill stack with initial values
            for (i in -r..r) {
                val xi = min(w - 1, max(0, i))
                val c = pix[p + xi]
                stack[i + r] = c
                val cr = (c ushr 16) and 0xFF
                val cg = (c ushr 8) and 0xFF
                val cb = c and 0xFF
                val ca = (c ushr 24) and 0xFF
                val mul = r + 1 - max(0, i)
                rSum += cr * mul; gSum += cg * mul; bSum += cb * mul; aSum += ca * mul
                rOutSum += cr; gOutSum += cg; bOutSum += cb; aOutSum += ca
            }
            stackPointer = r

            for (x in 0 until w) {
                pix[p + x] = (dv[aSum] shl 24) or (dv[rSum] shl 16) or
                        (dv[gSum] shl 8) or dv[bSum]

                // Remove outgoing pixel from sums
                val spOut = stack[stackPointer % div]
                rOutSum -= (spOut ushr 16) and 0xFF
                gOutSum -= (spOut ushr 8) and 0xFF
                bOutSum -= spOut and 0xFF
                aOutSum -= (spOut ushr 24) and 0xFF

                // New incoming x
                val xiIn = min(w - 1, x + r + 1)
                val cIn = pix[p + xiIn]
                stack[stackPointer] = cIn

                val ciR = (cIn ushr 16) and 0xFF
                val ciG = (cIn ushr 8) and 0xFF
                val ciB = cIn and 0xFF
                val ciA = (cIn ushr 24) and 0xFF
                rInSum += ciR; gInSum += ciG; bInSum += ciB; aInSum += ciA

                rSum += rInSum; gSum += gInSum; bSum += bInSum; aSum += aInSum

                stackPointer = (stackPointer + 1) % div
                val spIn = stack[stackPointer]
                rOutSum += (spIn ushr 16) and 0xFF
                gOutSum += (spIn ushr 8) and 0xFF
                bOutSum += spIn and 0xFF
                aOutSum += (spIn ushr 24) and 0xFF
                rInSum -= (spIn ushr 16) and 0xFF
                gInSum -= (spIn ushr 8) and 0xFF
                bInSum -= spIn and 0xFF
                aInSum -= (spIn ushr 24) and 0xFF
            }
        }

        // ── Vertical pass ──
        for (x in 0 until w) {
            rInSum = 0; gInSum = 0; bInSum = 0; aInSum = 0
            rOutSum = 0; gOutSum = 0; bOutSum = 0; aOutSum = 0
            rSum = 0; gSum = 0; bSum = 0; aSum = 0

            // Fill stack
            for (i in -r..r) {
                val yi = min(h - 1, max(0, i))
                val c = pix[yi * w + x]
                stack[i + r] = c
                val cr = (c ushr 16) and 0xFF
                val cg = (c ushr 8) and 0xFF
                val cb = c and 0xFF
                val ca = (c ushr 24) and 0xFF
                val mul = r + 1 - max(0, i)
                rSum += cr * mul; gSum += cg * mul; bSum += cb * mul; aSum += ca * mul
                rOutSum += cr; gOutSum += cg; bOutSum += cb; aOutSum += ca
            }
            stackPointer = r

            for (y1 in 0 until h) {
                pix[y1 * w + x] = (dv[aSum] shl 24) or (dv[rSum] shl 16) or
                        (dv[gSum] shl 8) or dv[bSum]

                val spOut = stack[stackPointer % div]
                rOutSum -= (spOut ushr 16) and 0xFF
                gOutSum -= (spOut ushr 8) and 0xFF
                bOutSum -= spOut and 0xFF
                aOutSum -= (spOut ushr 24) and 0xFF

                val yiIn = min(h - 1, y1 + r + 1)
                val cIn = pix[yiIn * w + x]
                stack[stackPointer] = cIn

                val ciR = (cIn ushr 16) and 0xFF
                val ciG = (cIn ushr 8) and 0xFF
                val ciB = cIn and 0xFF
                val ciA = (cIn ushr 24) and 0xFF
                rInSum += ciR; gInSum += ciG; bInSum += ciB; aInSum += ciA

                rSum += rInSum; gSum += gInSum; bSum += bInSum; aSum += aInSum

                stackPointer = (stackPointer + 1) % div
                val spIn = stack[stackPointer]
                rOutSum += (spIn ushr 16) and 0xFF
                gOutSum += (spIn ushr 8) and 0xFF
                bOutSum += spIn and 0xFF
                aOutSum += (spIn ushr 24) and 0xFF
                rInSum -= (spIn ushr 16) and 0xFF
                gInSum -= (spIn ushr 8) and 0xFF
                bInSum -= spIn and 0xFF
                aInSum -= (spIn ushr 24) and 0xFF
            }
        }

        bmp.setPixels(pix, 0, w, 0, 0, w, h)
        return bmp
    }

    // ══════════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════════

    /**
     * Blur a bitmap. Auto-selects GPU (AGSL, API 33+) or CPU (StackBlur).
     * @param src Source bitmap (not modified).
     * @param radius Blur radius in pixels.
     */
    fun blur(src: Bitmap, radius: Float): Bitmap? {
        if (radius <= 0f) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        if (Build.VERSION.SDK_INT >= 33) {
            blurGpu(src, radius)?.let { return it }
        }
        return runCatching {
            blurCpu(src, radius.roundToInt().coerceIn(1, 250))
        }.getOrNull()
    }

    /**
     * Self-contained widget blur: read the region of the device wallpaper
     * behind a widget and blur it. Works on EVERY launcher — no Samsung
     * One UI cooperation needed.
     */
    fun blurWallpaperRegion(
        wallpaper: Bitmap,
        widgetLeft: Int, widgetTop: Int,
        widgetRight: Int, widgetBottom: Int,
        screenW: Int, screenH: Int,
        blurRadius: Float,
        density: Float
    ): Bitmap? {
        val scale = max(
            screenW.toFloat() / wallpaper.width,
            screenH.toFloat() / wallpaper.height
        )
        val offX = (screenW - wallpaper.width * scale) / 2f
        val offY = (screenH - wallpaper.height * scale) / 2f

        val wpL = ((widgetLeft - offX) / scale).roundToInt().coerceIn(0, wallpaper.width - 1)
        val wpT = ((widgetTop - offY) / scale).roundToInt().coerceIn(0, wallpaper.height - 1)
        val wpR = ((widgetRight - offX) / scale).roundToInt().coerceIn(wpL + 1, wallpaper.width)
        val wpB = ((widgetBottom - offY) / scale).roundToInt().coerceIn(wpT + 1, wallpaper.height)

        val rw = wpR - wpL; val rh = wpB - wpT
        if (rw <= 0 || rh <= 0) return null

        val widgetW = widgetRight - widgetLeft
        val widgetH = widgetBottom - widgetTop

        // Downscale for blur perf, then scale back
        val factor = 0.5f
        val smallW = (widgetW * factor).roundToInt().coerceAtLeast(8)
        val smallH = (widgetH * factor).roundToInt().coerceAtLeast(8)

        val region = Bitmap.createBitmap(wallpaper, wpL, wpT, rw, rh)
        val small = Bitmap.createScaledBitmap(region, smallW, smallH, true)
        region.recycle()

        val blurred = blur(small, blurRadius * density * factor)
        small.recycle()

        return blurred?.let {
            val result = Bitmap.createScaledBitmap(it, widgetW, widgetH, true)
            if (result !== it) it.recycle()
            result
        }
    }

    /**
     * Read the device wallpaper as a Bitmap. Returns null if unavailable.
     */
    fun readDeviceWallpaper(context: Context): Bitmap? {
        return runCatching {
            val wm = android.app.WallpaperManager.getInstance(context)
            wm.getWallpaperFile(android.app.WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { fis ->
                    val bytes = fis.readBytes()
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    var sample = 1; var dim = maxOf(opts.outWidth, opts.outHeight)
                    while (dim / 2 >= 1920) { sample *= 2; dim /= 2 }
                    BitmapFactory.decodeByteArray(
                        bytes, 0, bytes.size,
                        opts.apply { inJustDecodeBounds = false; inSampleSize = sample }
                    )
                }
            }
        }.getOrNull() ?: runCatching {
            val wm = android.app.WallpaperManager.getInstance(context)
            (wm.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        }.getOrNull()
    }
}

package com.curio.app.infrastructure

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import com.curio.app.R
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * GLASS LAB LIVE WALLPAPER. Draws the composition designed in the lab over
 * the chosen wallpaper.
 *
 * v285 FIDELITY PASS — parity with the lab's Kyant-rendered shapes:
 *  • PROGRESSIVE PYRAMID BLUR — repeated bilinear halving down / doubling up
 *    (never one giant jump, which is what produced the old pixelated frost),
 *    built PER SHAPE BLUR LEVEL from the lab's per-widget blur slider.
 *  • BACKDROP CACHED & DECODED BOUNDED — the chosen image decodes once,
 *    sampled to a sane size, keyed by its URI; a failed/OOM decode of a huge
 *    image no longer silently falls back to the wrong wallpaper.
 *  • RIM + CONTENT CLIPPED INSIDE THE CAPSULE — the old rim was stroked
 *    half-outside the pane after unclip, and unclipped text leaked out.
 *  • ICONS DRAWN AS CODEPOINTS — legacy Canvas.drawText does not reliably
 *    apply the subset font's ligatures on every OEM, which drew literal
 *    "battery_full" text; we now draw U+E1A5 / U+EF55 directly.
 *  • TYPEFACES MATCH THE LAB — bold values, medium pills, no shadow.
 *
 * LOCK SCREEN MODE: while the keyguard is up ONLY the clock shapes render;
 * everything else pops back with a soft 450ms fade on unlock.
 */
class GlassLabWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = GlassEngine()

    private inner class GlassEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var wasLocked = true
        @Volatile private var nonClockAlpha = if (wasLocked) 0f else 1f
        private var fadeRunning = false

        private val fadeRunnable = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.elapsedRealtime() - fadeStart
                nonClockAlpha = (elapsed.toFloat() / FADE_MS).coerceIn(0f, 1f)
                surfaceHolder?.let { drawFrame(it) }
                if (nonClockAlpha < 1f) handler.postDelayed(this, 16) else fadeRunning = false
            }
        }
        private var fadeStart = 0L

        /** 1-second tick while visible so baked clocks/dates stay live. */
        private var tickRunning = false
        private val tickRunnable = object : Runnable {
            override fun run() {
                surfaceHolder?.let { drawFrame(it) }
                if (tickRunning) handler.postDelayed(this, 1000)
            }
        }

        // ── Backdrop caches ────────────────────────────────────────────
        private var backdropKey: String? = null      // uri string or "auto"
        private var sharpBackdrop: Bitmap? = null
        private var drawScale = 1f                    // cover-fit scale
        private var drawOffX = 0f
        private var drawOffY = 0f

        /** Progressive-pyramid blur results, one per quantized blur level. */
        private val blurCache = HashMap<Int, Bitmap>()

        /**
         * v286 — TRUE GAUSSIAN FROST, the exact algorithm family the lab's
         * Kyant backdrop runs (RenderEffect blur): ScriptIntrinsicBlur on a
         * downscaled copy, scaled so the requested dp radius maps inside the
         * kernel's 25px cap, then smooth progressive doubling back up.
         * Cached per quantized level (one RS pass per distinct blur value).
         */
        private fun blurredFor(src: Bitmap, blurDp: Float): Bitmap? {
            val density = this@GlassLabWallpaperService.resources.displayMetrics.density
            val radiusPx = blurDp.coerceIn(2f, 20f) * density
            val factor = (radiusPx / 25f).coerceIn(1f, 12f)
            val key = (factor * 10f).toInt()
            blurCache[key]?.let { return it }
            val result = runCatching {
                val sw = max(8, (src.width / factor).toInt())
                val sh = max(8, (src.height / factor).toInt())
                val input = Bitmap.createScaledBitmap(src, sw, sh, true)
                val out = Bitmap.createBitmap(input)
                val rs = android.renderscript.RenderScript.create(this@GlassLabWallpaperService)
                try {
                    val ain = android.renderscript.Allocation.createFromBitmap(rs, input)
                    val aout = android.renderscript.Allocation.createFromBitmap(rs, out)
                    val script = android.renderscript.ScriptIntrinsicBlur.create(
                        rs, android.renderscript.Element.U8_4(rs)
                    )
                    script.setRadius(25f)
                    script.setInput(ain)
                    script.forEach(aout)
                    aout.copyTo(out)
                } finally {
                    rs.destroy()
                }
                var cur = out
                while (cur.width < src.width / 2) {
                    val nw = min(cur.width * 2, src.width)
                    val nh = (nw.toLong() * src.height / src.width).toInt()
                    cur = Bitmap.createScaledBitmap(cur, nw, nh, true)
                }
                cur
            }.getOrNull()
            if (result != null) blurCache[key] = result
            return result
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            refreshLockState()
            surfaceHolder?.let { drawFrame(it) }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder, format: Int, width: Int, height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            refreshLockState()
            surfaceHolder?.let { drawFrame(it) }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                refreshLockState()
                surfaceHolder?.let { drawFrame(it) }
                if (!tickRunning) {
                    tickRunning = true
                    handler.postDelayed(tickRunnable, 1000)
                }
            } else {
                tickRunning = false
                handler.removeCallbacks(tickRunnable)
            }
        }

        /** Updates [wasLocked]; on fresh unlock starts the pop-back fade. */
        private fun refreshLockState() {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val locked = km?.isKeyguardLocked ?: false
            if (locked == wasLocked) return
            wasLocked = locked
            if (!locked && nonClockAlpha < 1f) {
                fadeStart = SystemClock.elapsedRealtime()
                if (!fadeRunning) {
                    fadeRunning = true
                    handler.post(fadeRunnable)
                }
            } else if (locked) {
                nonClockAlpha = 0f
            }
        }

        private fun drawFrame(holder: SurfaceHolder) {
            val canvas = holder.lockCanvas() ?: return
            try {
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()

                val context = this@GlassLabWallpaperService
                val key = com.curio.app.data.AppPreferences.getGlassLabWallpaperUri(context)
                if (key != backdropKey || sharpBackdrop == null) {
                    backdropKey = key
                    blurCache.clear()
                    sharpBackdrop = decodeBackdropBounded(context, key)
                }
                val bmp = sharpBackdrop
                if (bmp != null) {
                    drawScale = max(w / bmp.width, h / bmp.height)
                    drawOffX = (w - bmp.width * drawScale) / 2f
                    drawOffY = (h - bmp.height * drawScale) / 2f
                    canvas.drawBitmap(bmp, drawOffX, drawOffY, null)
                } else {
                    drawScale = 1f; drawOffX = 0f; drawOffY = 0f
                    drawGradientFallback(canvas, w, h)
                }

                val density = context.resources.displayMetrics.density
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = ResourcesCompat.getFont(context, R.font.material_symbols_outlined)
                    textAlign = Paint.Align.CENTER
                }
                // Match the lab's Compose text styles exactly: bold values,
                // medium pills, plain default family, NO shadow layer.
                val boldText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val mediumText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }

                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val locked = km?.isKeyguardLocked ?: false

                GlassLabComposition.load(context)
                    .filter { !locked || it.id == "clock" || it.id == "analog" }
                    .forEach { shape ->
                        val alpha = when {
                            locked -> 1f
                            shape.id == "clock" || shape.id == "analog" -> 1f
                            else -> nonClockAlpha
                        }
                        drawShape(
                            canvas, context, shape, w, h, alpha,
                            paint, glyphPaint, boldText, mediumText, density
                        )
                    }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        /** LIVE data so the baked widgets match the real ones. */
        private fun liveTitle(context: Context, id: String): String = when (id) {
            "clock" -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
            "timer" -> {
                val active = com.curio.app.data.ExploreSessionStore.getActiveSession(context)
                if (active != null) {
                    val mins = active.elapsedMillis(System.currentTimeMillis()) / 60000L
                    "Exploring · ${mins}m"
                } else "Explored"
            }
            "streak" -> "${com.curio.app.data.StreakTracker.getStreak(context)}"
            "battery" -> {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                "${bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it > 0 } ?: 80}%"
            }
            "date" -> java.text.SimpleDateFormat("EEE · MMM d", java.util.Locale.getDefault())
                .format(java.util.Date())
            else -> ""
        }

        /**
         * One baked liquid-glass pane:
         *  1. clip to the rounded capsule
         *  2. re-draw the BLURRED backdrop aligned under the pane
         *     (per-shape blur strength from the lab slider)
         *  3. top gloss + bottom depth gradients + 12% surface veil
         *  4. thin rim highlight INSIDE the clip (never outside the pane)
         *  5. content (hands / icon glyph / text), still clipped.
         */
        private fun drawShape(
            canvas: Canvas,
            context: Context,
            shape: GlassLabComposition.Shape,
            w: Float,
            h: Float,
            alpha: Float,
            paint: Paint,
            glyphPaint: Paint,
            boldText: Paint,
            mediumText: Paint,
            density: Float
        ) {
            // ── Geometry — EXACTLY the lab's fixed dp sizes ─────────────
            val dw: Float; val dh: Float
            when (shape.id) {
                "clock", "analog" -> { dw = 112f; dh = 112f }
                "streak", "battery" -> { dw = 92f; dh = 92f }
                else -> { dw = 196f; dh = 58f }
            }
            val pw = dw * density * shape.scale
            val ph = dh * density * shape.scale
            val left = shape.xFrac * w
            val top = shape.yFrac * h
            val radius = ph.coerceAtMost(pw) / 2f
            val rect = RectF(left, top, left + pw, top + ph)

            val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }

            // Explicit receivers throughout: a bare `alpha` here would hit
            // this function's Float parameter, not any Paint property.
            val satPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            satPaint.colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply { setSaturation(1.25f) }   // vibrancy()
            )
            satPaint.alpha = (alpha * 255).toInt()

            canvas.save()
            canvas.clipPath(clip)

            val blurBmp = sharpBackdrop?.let { blurredFor(it, shape.blurDp) }
            if (blurBmp != null) {
                // Map the pane's screen rect back into backdrop coords so
                // the blur lines up pixel-perfectly behind the pane.
                val sx = (left - drawOffX) / drawScale
                val sy = (top - drawOffY) / drawScale
                // drawBitmap's SRC arg must be an integer Rect (or null).
                val sRect = Rect(
                    sx.coerceAtLeast(0f).toInt(),
                    sy.coerceAtLeast(0f).toInt(),
                    ((sx + pw / drawScale).coerceAtMost(blurBmp.width.toFloat())
                        .toInt().coerceAtLeast(1)).coerceAtMost(blurBmp.width),
                    ((sy + ph / drawScale).coerceAtMost(blurBmp.height.toFloat())
                        .toInt().coerceAtLeast(1)).coerceAtMost(blurBmp.height)
                )
                canvas.drawBitmap(blurBmp, sRect, rect, satPaint)
            } else {
                // No bitmap backdrop → tinted glass over the fallback gradient.
                paint.shader = LinearGradient(
                    0f, top, 0f, top + ph,
                    0x59FFFFFF, 0x2E3A3A44.toInt(), Shader.TileMode.CLAMP
                )
                paint.alpha = (alpha * 255).toInt()
                canvas.drawRect(rect, paint)
                paint.shader = null
            }

            // 12% surface veil (the lab's onDrawSurface) + top gloss +
            // bottom depth — same three layers the Kyant recipe stacks.
            paint.color = Color.WHITE
            paint.alpha = (alpha * 31).toInt()
            canvas.drawRect(rect, paint)
            paint.shader = LinearGradient(
                0f, top, 0f, top + ph * 0.55f,
                0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
            )
            paint.alpha = 255
            canvas.drawRect(rect, paint)
            paint.shader = LinearGradient(
                0f, top + ph * 0.6f, 0f, top + ph,
                0x00000000, 0x24000000, Shader.TileMode.CLAMP
            )
            canvas.drawRect(rect, paint)
            paint.shader = null

            // Rim highlight INSIDE the capsule — the old version stroked the
            // rect edge after unclipping, putting half the stroke outside.
            val rimW = density * 1.5f
            val rimRect = RectF(
                left + rimW / 2f, top + rimW / 2f,
                left + pw - rimW / 2f, top + ph - rimW / 2f
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = rimW
            paint.alpha = (alpha * 110).toInt()
            paint.color = Color.WHITE
            canvas.drawRoundRect(rimRect, radius - rimW / 2f, radius - rimW / 2f, paint)
            paint.style = Paint.Style.FILL
            canvas.restore()

            // ── Content — clipped again so nothing leaks past the pane ──
            val cx = left + pw / 2f
            val cy = top + ph / 2f

            canvas.save()
            canvas.clipPath(clip)

            if (shape.id == "analog") {
                val cal = java.util.Calendar.getInstance()
                val hourAngle =
                    ((cal.get(java.util.Calendar.HOUR_OF_DAY) % 12) + cal.get(java.util.Calendar.MINUTE) / 60f) * 30f
                val minuteAngle =
                    (cal.get(java.util.Calendar.MINUTE) + cal.get(java.util.Calendar.SECOND) / 60f) * 6f
                val r = ph / 2f - ph * 0.08f
                // Faint dial outline, like the lab's Canvas shape.
                paint.strokeCap = Paint.Cap.ROUND
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = density * 2f
                paint.color = shape.textArgb
                paint.alpha = (alpha * 90).toInt()
                canvas.drawCircle(cx, cy, r, paint)
                paint.style = Paint.Style.FILL
                paint.alpha = (alpha * 255).toInt()
                fun hand(angleDeg: Float, lenFrac: Float, widthPx: Float) {
                    val rad = Math.toRadians((angleDeg - 90).toDouble())
                    paint.strokeWidth = widthPx
                    canvas.drawLine(
                        cx, cy,
                        cx + (lenFrac * r * cos(rad)).toFloat(),
                        cy + (lenFrac * r * sin(rad)).toFloat(),
                        paint
                    )
                }
                hand(hourAngle, 0.52f, density * 4f)
                hand(minuteAngle, 0.78f, density * 2.5f)
                paint.color = 0xFFFF8A3C.toInt()
                canvas.drawCircle(cx, cy, density * 3.5f, paint)
                paint.strokeCap = Paint.Cap.BUTT
                canvas.restore()
                return
            }

            boldText.alpha = (alpha * 255).toInt()
            boldText.color = shape.textArgb
            glyphPaint.alpha = (alpha * 255).toInt()
            glyphPaint.color = shape.textArgb
            mediumText.alpha = (alpha * 255).toInt()
            mediumText.color = shape.textArgb
            when (shape.id) {
                "clock" -> {
                    boldText.textSize = 26f * density * shape.scale
                    canvas.drawText(liveTitle(context, shape.id), cx, cy + boldText.textSize / 3f, boldText)
                }
                "streak", "battery" -> {
                    // Real Material Symbols CODEPOINTS — legacy drawText does
                    // not reliably apply ligatures, which leaked raw names.
                    val glyphCp = if (shape.id == "streak") 0xEF55 /* local_fire_department */ else 0xE1A5 /* battery_full */
                    glyphPaint.textSize = 22f * density * shape.scale
                    canvas.drawText(
                        String(Character.toChars(glyphCp)),
                        cx, cy - ph * 0.10f, glyphPaint
                    )
                    boldText.textSize = (if (shape.id == "streak") 17f else 15f) * density * shape.scale
                    canvas.drawText(liveTitle(context, shape.id), cx, cy + ph * 0.20f, boldText)
                }
                else -> {
                    // Pills (timer/date) — the lab renders these SemiBold.
                    mediumText.textSize = 14f * density * shape.scale
                    canvas.drawText(liveTitle(context, shape.id), cx, cy + mediumText.textSize / 3f, mediumText)
                }
            }
            canvas.restore()
        }
    }

    companion object {
        const val FADE_MS = 450L

        /**
         * Backdrop resolution: the lab's picked image first; when the user
         * left it on auto, try the DEVICE wallpaper ladder
         * (getWallpaperFile -> getDrawable) before giving up to the gradient.
         *
         * v286 BUGFIX: both sources are read into a byte array FIRST and
         * decoded from that buffer. The previous version ran a bounds pass
         * and a full pass over the SAME stream/file-descriptor — the first
         * pass consumed it, the second decoded null, and the wallpaper
         * silently vanished into the gradient fallback.
         */
        fun decodeBackdropBounded(context: Context, key: String?): Bitmap? = runCatching {
            fun bytesToBmp(bytes: ByteArray?): Bitmap? {
                if (bytes == null || bytes.isEmpty()) return null
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                if (opts.outWidth <= 0) return null
                var sample = 1
                var dim = maxOf(opts.outWidth, opts.outHeight)
                while (dim / 2 >= 1600) { sample *= 2; dim /= 2 }
                return android.graphics.BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    opts.apply { inJustDecodeBounds = false; inSampleSize = sample }
                )
            }
            if (key != null && key != "auto" && key.isNotBlank()) {
                return runCatching {
                    context.contentResolver.openInputStream(Uri.parse(key))
                        ?.use { it.readBytes() }
                        .let(::bytesToBmp)
                }.getOrNull()
            }
            val wm = WallpaperManager.getInstance(context)
            runCatching {
                wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                    java.io.FileInputStream(pfd.fileDescriptor).use { fis ->
                        bytesToBmp(fis.readBytes())
                    }
                }
            }.getOrNull()
                ?: runCatching {
                    (wm.getDrawable() as? android.graphics.drawable.BitmapDrawable)?.bitmap
                }.getOrNull()
        }.getOrNull()
    }
}

/** Fallback backdrop when no bitmap is available. */
internal fun drawGradientFallback(canvas: Canvas, w: Float, h: Float) {
    val paint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, w, h,
            Color.parseColor("#7E57C2"), Color.parseColor("#80DEEA"),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w, h, paint)
}

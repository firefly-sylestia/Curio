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
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import com.curio.app.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * GLASS LAB LIVE WALLPAPER. Draws the composition designed in the lab over
 * the chosen wallpaper.
 *
 * REAL BAKED LIQUID GLASS (v284): every pane samples the backdrop ALIGNED
 * UNDERNEATH IT — the region of wallpaper exactly behind the pane is drawn
 * back through a downscale/upscale gaussian-style blur with a saturation
 * boost (vibrancy) — then a top gloss gradient and a bright rim are layered
 * on top. This is the same pipeline (blur + vibrancy) the in-app Kyant
 * recipe runs; per-pixel lens refraction still has no wallpaper-space API,
 * but the frost now genuinely shows the blurred wallpaper behind each pane
 * instead of a flat translucent veil.
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
                if (nonClockAlpha < 1f) {
                    handler.postDelayed(this, 16)
                } else {
                    fadeRunning = false
                }
            }
        }
        private var fadeStart = 0L

        // ── Backdrop caches ────────────────────────────────────────────
        private var sharpBackdrop: Bitmap? = null
        private var blurredBackdrop: Bitmap? = null   // blurred + saturated
        private var drawScale = 1f                    // cover-fit scale
        private var drawOffX = 0f
        private var drawOffY = 0f

        /** Downscale→upscale gaussian-ish blur, cached against the source. */
        private fun ensureBlurred(src: Bitmap?) {
            if (src == null) {
                sharpBackdrop = null
                blurredBackdrop = null
                return
            }
            if (sharpBackdrop === src && blurredBackdrop != null) return
            sharpBackdrop = src
            blurredBackdrop = runCatching {
                val smallW = (src.width / 14).coerceIn(8, 400)
                val smallH = (src.height / 14).coerceIn(8, 800)
                val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
                // Two bilinear round-trips soften further without RenderScript.
                val mid = Bitmap.createScaledBitmap(small, smallW / 2, smallH / 2, true)
                Bitmap.createScaledBitmap(mid, src.width, src.height, true)
            }.getOrNull()
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
            if (!visible) return
            refreshLockState()
            surfaceHolder?.let { drawFrame(it) }
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
                val bmp = loadBackdrop(context)
                ensureBlurred(bmp)
                if (bmp != null) {
                    drawScale = maxOf(w / bmp.width, h / bmp.height)
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
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(4f, 0f, 2f, 0x66000000)
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
                            paint, glyphPaint, textPaint, density
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
         *  2. re-draw the BLURRED + SATURATED backdrop aligned under the pane
         *     (real frosted glass, not a flat tint)
         *  3. top gloss + bottom depth gradients
         *  4. bright rim stroke
         *  5. content (hands / icon glyph / text) sized from the same dp
         *     dims the lab uses.
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
            textPaint: Paint,
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

            // ── 1+2+3: clipped glass body ───────────────────────────────
            val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clip)

            // NOTE: explicit receivers — inside `apply { }` a bare `alpha`
            // would resolve to drawShape's Float parameter, not Paint.alpha.
            val satPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            satPaint.colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply { setSaturation(1.25f) }
            )
            satPaint.alpha = (alpha * 255).toInt()
            val blurBmp = blurredBackdrop
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

            // Gloss (top → transparent) + depth (bottom darkening).
            paint.shader = LinearGradient(
                0f, top, 0f, top + ph * 0.55f,
                0x38FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
            )
            paint.alpha = (alpha * 255).toInt()
            canvas.drawRect(rect, paint)
            paint.shader = LinearGradient(
                0f, top + ph * 0.6f, 0f, top + ph,
                0x00000000, 0x24000000, Shader.TileMode.CLAMP
            )
            canvas.drawRect(rect, paint)
            paint.shader = null
            canvas.restore()

            // ── 4: rim highlight ────────────────────────────────────────
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = density * 1.4f
            paint.alpha = (alpha * 90).toInt()
            paint.color = Color.WHITE
            canvas.drawRoundRect(rect, radius, radius, paint)
            paint.style = Paint.Style.FILL

            // ── 5: content ──────────────────────────────────────────────
            val cx = left + pw / 2f
            val cy = top + ph / 2f

            if (shape.id == "analog") {
                val cal = java.util.Calendar.getInstance()
                val hourAngle =
                    ((cal.get(java.util.Calendar.HOUR_OF_DAY) % 12) + cal.get(java.util.Calendar.MINUTE) / 60f) * 30f
                val minuteAngle =
                    (cal.get(java.util.Calendar.MINUTE) + cal.get(java.util.Calendar.SECOND) / 60f) * 6f
                val r = ph / 2f - ph * 0.08f
                paint.strokeCap = Paint.Cap.ROUND
                fun hand(angleDeg: Float, lenFrac: Float, widthPx: Float) {
                    val rad = Math.toRadians((angleDeg - 90).toDouble())
                    paint.color = shape.textArgb
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
                return
            }

            textPaint.alpha = (alpha * 255).toInt()
            textPaint.color = shape.textArgb
            when (shape.id) {
                "clock" -> {
                    textPaint.textSize = 26f * density * shape.scale
                    canvas.drawText(liveTitle(context, shape.id), cx, cy + textPaint.textSize / 3f, textPaint)
                }
                "streak", "battery" -> {
                    // Glyph above the value, both centered like the lab Column.
                    glyphPaint.alpha = (alpha * 255).toInt()
                    glyphPaint.color = shape.textArgb
                    glyphPaint.textSize = 22f * density * shape.scale
                    val glyph = if (shape.id == "streak") "local_fire_department" else "battery_full"
                    canvas.drawText(glyph, cx, cy - ph * 0.06f, glyphPaint)
                    textPaint.textSize = 16f * density * shape.scale
                    canvas.drawText(liveTitle(context, shape.id), cx, cy + textPaint.textSize * 1.15f, textPaint)
                }
                else -> {
                    // Pills (timer/date).
                    textPaint.textSize = 14f * density * shape.scale
                    canvas.drawText(liveTitle(context, shape.id), cx, cy + textPaint.textSize / 3f, textPaint)
                }
            }
        }
    }

    companion object {
        const val FADE_MS = 450L

        /**
         * Backdrop resolution: the lab's picked image first; when the user
         * left it on auto, try the DEVICE wallpaper ladder
         * (getWallpaperFile -> getDrawable) before giving up to the gradient.
         */
        fun loadBackdrop(context: Context): Bitmap? = runCatching {
            val uriStr = com.curio.app.data.AppPreferences.getGlassLabWallpaperUri(context)
            if (uriStr != "auto" && uriStr.isNotBlank()) {
                return runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    }
                }.getOrNull()
            }
            val wm = WallpaperManager.getInstance(context)
            runCatching {
                wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                    android.graphics.BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
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

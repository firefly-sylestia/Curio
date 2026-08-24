package com.curio.app.infrastructure

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
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
 * v279 — GLASS LAB LIVE WALLPAPER. Draws the composition designed in the
 * lab (wallpaper image + frosted panes with their text) as a live wallpaper.
 *
 * v280 — LOCK SCREEN MODE: while the keyguard is up ONLY the clock shapes
 * render; every other shape pops back with a soft 450ms fade the moment the
 * device unlocks.
 *
 * Honest scope: RemoteViews-style per-pixel refraction doesn't exist for
 * wallpapers either, so panes are baked frost (gradient + rounded corners)
 * — the One UI frost look, not liquid-glass refraction.
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
                // Freshly unlocked — start the beautiful pop-back fade.
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
                drawBackdrop(canvas, this@GlassLabWallpaperService, w, h)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = ResourcesCompat.getFont(
                        this@GlassLabWallpaperService,
                        R.font.material_symbols_outlined
                    )
                    textAlign = Paint.Align.CENTER
                }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(4f, 0f, 2f, 0x66000000)
                }

                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val locked = km?.isKeyguardLocked ?: false

                GlassLabComposition.load(this@GlassLabWallpaperService)
                    .filter { !locked || it.id == "clock" || it.id == "analog" }
                    .forEach { shape ->
                        val alpha = when {
                            locked -> 1f
                            shape.id == "clock" || shape.id == "analog" -> 1f
                            else -> nonClockAlpha
                        }
                        drawShape(canvas, this@GlassLabWallpaperService, shape, w, h, alpha, paint, glyphPaint, textPaint)
                    }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawShape(
            canvas: Canvas,
            context: Context,
            shape: GlassLabComposition.Shape,
            w: Float,
            h: Float,
            alpha: Float,
            paint: Paint,
            glyphPaint: Paint,
            textPaint: Paint
        ) {
            val base = minOf(w, h)
            val round = shape.id == "clock" || shape.id == "analog" ||
                shape.id == "streak" || shape.id == "battery"
            val pw = (if (round) 112f else 196f) / 360f * base * shape.scale
            val ph = pw.coerceAtMost((if (round) 112f else 58f) / 360f * base * shape.scale)
            val left = shape.xFrac * w
            val top = shape.yFrac * h

            paint.alpha = (alpha * 255).toInt()
            paint.shader = LinearGradient(
                0f, top, 0f, top + ph,
                0x59FFFFFF, 0x2E3A3A44.toInt(), Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(left, top, left + pw, top + ph, ph / 2f, ph / 2f, paint)
            paint.shader = null

            val cx = left + pw / 2f
            val cy = top + ph / 2f

            if (shape.id == "analog") {
                // Live analog hands.
                val cal = java.util.Calendar.getInstance()
                val hourAngle =
                    ((cal.get(java.util.Calendar.HOUR_OF_DAY) % 12) + cal.get(java.util.Calendar.MINUTE) / 60f) * 30f
                val minuteAngle =
                    (cal.get(java.util.Calendar.MINUTE) + cal.get(java.util.Calendar.SECOND) / 60f) * 6f
                val r = ph / 2f - ph * 0.08f
                paint.color = Color.WHITE
                canvas.drawCircle(cx, cy, r, paint.apply {
                    this.alpha = (alpha * 64).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = ph * 0.04f
                })
                paint.style = Paint.Style.FILL
                fun hand(angleDeg: Float, lenFrac: Float, widthPx: Float) {
                    val rad = Math.toRadians((angleDeg - 90).toDouble())
                    paint.color = Color.WHITE
                    paint.strokeWidth = widthPx
                    paint.strokeCap = Paint.Cap.ROUND
                    canvas.drawLine(
                        cx, cy,
                        cx + (lenFrac * r * cos(rad)).toFloat(),
                        cy + (lenFrac * r * sin(rad)).toFloat(),
                        paint
                    )
                }
                hand(hourAngle, 0.5f, ph * 0.055f)
                hand(minuteAngle, 0.78f, ph * 0.035f)
                paint.color = 0xFFFF8A3C.toInt()
                canvas.drawCircle(cx, cy, ph * 0.03f, paint)
                paint.strokeCap = Paint.Cap.BUTT
                return
            }

            textPaint.alpha = (alpha * 255).toInt()
            textPaint.color = shape.textArgb
            textPaint.textSize = ph * 0.30f
            canvas.drawText(GlassLabComposition.titleFor(shape.id), cx, cy + textPaint.textSize / 3f, textPaint)
            if (shape.id == "streak" || shape.id == "battery") {
                glyphPaint.alpha = (alpha * 255).toInt()
                glyphPaint.color = shape.textArgb
                glyphPaint.textSize = ph * 0.34f
                canvas.drawText(
                    if (shape.id == "streak") "local_fire_department" else "battery_full",
                    cx, cy - ph * 0.16f, glyphPaint
                )
            }
        }
    }

    companion object {
        const val FADE_MS = 450L

        /** Wallpaper bitmap via persisted lab URI; rich gradient fallback. */
        fun loadBackdrop(context: Context): android.graphics.Bitmap? = runCatching {
            val uriStr = com.curio.app.data.AppPreferences.getGlassLabWallpaperUri(context)
            if (uriStr == "auto" || uriStr.isBlank()) return null
            context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
    }
}

/** Backdrop: user wallpaper cropped to fill, or the lab's gradient fallback. */
internal fun drawBackdrop(
    canvas: Canvas,
    context: Context,
    w: Float,
    h: Float
) {
    val bmp = GlassLabWallpaperService.loadBackdrop(context)
    if (bmp != null) {
        val scale = maxOf(w / bmp.width, h / bmp.height)
        val bw = bmp.width * scale
        val bh = bmp.height * scale
        canvas.drawBitmap(bmp, (w - bw) / 2f, (h - bh) / 2f, null)
        return
    }
    val paint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, w, h,
            Color.parseColor("#7E57C2"), Color.parseColor("#80DEEA"),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w, h, paint)
}

package com.curio.app.infrastructure

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import com.curio.app.R

/**
 * v279 — GLASS LAB LIVE WALLPAPER. Draws the composition the user designed
 * in the lab (wallpaper image + frosted panes with their text) as a live
 * wallpaper. The scene is static by design — the value is having YOUR
 * wallpaper + YOUR widget layout behind every home screen page; live data
 * in real widgets stays with the real app widgets.
 *
 * Honest scope: RemoteViews-style per-pixel refraction doesn't exist for
 * wallpapers either, so panes are baked frost (gradient + rounded corners)
 * — the One UI frost look, not liquid-glass refraction.
 */
class GlassLabWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = GlassEngine()

    private inner class GlassEngine : Engine() {

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            drawFrame(holder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder, format: Int, width: Int, height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) surfaceHolder?.let { drawFrame(it) }
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
                GlassLabComposition.load(this@GlassLabWallpaperService).forEach { shape ->
                    val base = minOf(w, h)
                    val pw = when (shape.id) {
                        "clock", "streak", "battery" -> 112f / 360f * base * shape.scale
                        else -> 196f / 360f * base * shape.scale
                    }
                    val ph = when (shape.id) {
                        "clock", "streak", "battery" -> 112f / 360f * base * shape.scale
                        else -> 58f / 360f * base * shape.scale
                    }
                    val left = shape.xFrac * w
                    val top = shape.yFrac * h
                    // Frost pane.
                    paint.shader = LinearGradient(
                        0f, top, 0f, top + ph,
                        0x59FFFFFF, 0x2E3A3A44.toInt(), Shader.TileMode.CLAMP
                    )
                    canvas.drawRoundRect(left, top, left + pw, top + ph, ph / 2f, ph / 2f, paint)
                    // Content.
                    val cx = left + pw / 2f
                    val cy = top + ph / 2f
                    textPaint.color = shape.textArgb
                    textPaint.textSize = ph * 0.30f
                    canvas.drawText(GlassLabComposition.titleFor(shape.id), cx, cy + textPaint.textSize / 3f, textPaint)
                    if (shape.id == "streak" || shape.id == "battery") {
                        glyphPaint.color = shape.textArgb
                        glyphPaint.textSize = ph * 0.34f
                        canvas.drawText(
                            if (shape.id == "streak") "local_fire_department" else "battery_full",
                            cx, cy - ph * 0.16f, glyphPaint
                        )
                    }
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    companion object {
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
        canvas.drawBitmap(
            bmp, (w - bw) / 2f, (h - bh) / 2f, null
        )
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

package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * v27 — experimental paper accents for the torn-rose heroes, OFF by default
 * (toggle in Settings → Experiments → Paper & headers). Callers draw this as
 * the FIRST child of the hero banner so the marks read as printed/embossed
 * on the paper, behind the watermark collage and the hero content (the
 * banner's torn shape clips them at the seam).
 *
 *  - [pinHoles] — a column of stamped punch-hole circles down the left edge
 *    (the diary/binder hole look): a pressed rim + a deeper inner disc, so
 *    each reads as a hole punched INTO the paper instead of a dot on it.
 *  - [cornerLines] — two short tilted strokes at the bottom-left corner
 *    (the paper-title underline family), only at the corner, gently tilted.
 *  - [topTicks] — three small lines stacked at the top-right corner, fading
 *    toward the edge like tiny lorem-ipsum ticks.
 */
@Composable
fun PaperHeaderAccents(
    ink: Color,
    pinHoles: Boolean = true,
    cornerLines: Boolean = true,
    topTicks: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (pinHoles) {
            val holeR = 5.dp.toPx()
            val holeX = 14.dp.toPx()
            val nHoles = (h / 46.dp.toPx()).toInt().coerceIn(3, 6)
            for (i in 0 until nHoles) {
                val c = Offset(holeX, h * (i + 1) / (nHoles + 1))
                // Pressed rim — the paper bulges around the punch.
                drawCircle(
                    color = ink.copy(alpha = 0.13f),
                    radius = holeR + 2.dp.toPx(),
                    center = c,
                    style = Stroke(width = 2.dp.toPx())
                )
                // The hole itself.
                drawCircle(color = ink.copy(alpha = 0.22f), radius = holeR, center = c)
                // Bottom edge catches the light of the pressed depression.
                drawCircle(
                    color = ink.copy(alpha = 0.06f),
                    radius = holeR - 1.5.dp.toPx(),
                    center = Offset(c.x, c.y + 1.5.dp.toPx())
                )
            }
        }

        if (cornerLines) {
            val startX = 16.dp.toPx()
            val baseY = h - 22.dp.toPx()
            rotate(degrees = -8f, pivot = Offset(startX, baseY)) {
                drawLine(
                    color = ink.copy(alpha = 0.30f),
                    start = Offset(startX, baseY),
                    end = Offset(startX + 40.dp.toPx(), baseY),
                    strokeWidth = 2.4.dp.toPx()
                )
                drawLine(
                    color = ink.copy(alpha = 0.20f),
                    start = Offset(startX, baseY + 7.dp.toPx()),
                    end = Offset(startX + 26.dp.toPx(), baseY + 7.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        if (topTicks) {
            val tickX = w - 26.dp.toPx()
            for (i in 0 until 3) {
                val ty = 14.dp.toPx() + i * 8.dp.toPx()
                val len = (18 - i * 3).dp.toPx()
                drawLine(
                    color = ink.copy(alpha = 0.30f),
                    start = Offset(tickX, ty),
                    end = Offset(tickX + len, ty),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

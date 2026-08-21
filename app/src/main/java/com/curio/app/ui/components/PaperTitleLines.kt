package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * v27 — experimental paper-title underline: two short, slightly curved pen
 * strokes under a hero title, like the double underline drawn under a title
 * on paper (toggle in Settings → Experiments → Paper & headers →
 * "Title cut lines").
 *
 * v27u — rework after user feedback: the strokes are SHORTER (they span
 * ~88% of the title width instead of stretching past the text end), and the
 * two lines are drawn the way a hand draws a double underline — gently wavy
 * cubic strokes that CONVERGE slightly toward the right (one continuous pen
 * motion, never crossing), the lower line a touch longer and offset right,
 * separated by a steady gap, with a felt-pen edge (wide soft pass under a
 * narrow dark pass, round caps). Place directly BELOW the title text;
 * alignment follows the parent column.
 */
@Composable
fun PaperTitleLines(
    ink: Color,
    /** The title text being underlined — drives the stroke length. */
    title: String,
    /** The title's font size — stroke length + weight scale with it. */
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textPx = with(density) { fontSize.toPx() }
    // v27u — shorter: the underlines run ~88% of the title's width (the old
    // +3-char stretch ran visibly past the text). The floor keeps one-word
    // titles readable; the em + dp caps keep long titles from overflowing.
    val textWidthPx = title.length * textPx * 0.62f
    val linePx = (textWidthPx * 0.88f)
        .coerceAtLeast(textPx * 1.8f)
        .coerceAtMost(textPx * 11f)
    val cappedPx = min(linePx, with(density) { 220.dp.toPx() })
    val width = with(density) { cappedPx.toDp() }
    val height = with(density) { (textPx * 0.58f).toDp() }
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
            // Hand-written tilt: the right end rides slightly higher.
            .rotate(-2f)
    ) {
        val w = size.width
        val h = size.height
        val main = ink.copy(alpha = 0.34f)
        val faint = ink.copy(alpha = 0.15f)
        val cap = StrokeCap.Round

        fun stroke(p0: Offset, c1: Offset, c2: Offset, p3: Offset, weightPx: Float) {
            val path = Path().apply {
                moveTo(p0.x, p0.y)
                cubicTo(c1.x, c1.y, c2.x, c2.y, p3.x, p3.y)
            }
            // Felt-pen edge: a wide soft pass under a narrow dark pass with
            // round caps — the ends read soft, like a pen lifting off.
            drawPath(path, faint, style = Stroke(width = weightPx * 1.6f, cap = cap))
            drawPath(path, main, style = Stroke(width = weightPx, cap = cap))
        }

        // Two hand-drawn strokes in the lower half, SHORTER and shifted
        // toward the RIGHT of the header text (v27w — user: "make it little
        // more shorter and more to the right of the header text"): they now
        // start ~a quarter in from the title's left edge and span only the
        // right ~70% of the line — a partial right-side underline instead of
        // a full-width one. The top line runs a touch shorter with a gentle
        // sag; the bottom line starts a little further right, ends further
        // right, and rises gently — a single-pen-motion double underline
        // that never crosses itself.
        stroke(
            p0 = Offset(w * 0.22f, h * 0.34f),
            c1 = Offset(w * 0.44f, h * 0.47f),
            c2 = Offset(w * 0.72f, h * 0.45f),
            p3 = Offset(w * 0.90f, h * 0.40f),
            weightPx = textPx * 0.10f
        )
        stroke(
            p0 = Offset(w * 0.26f, h * 0.74f),
            c1 = Offset(w * 0.50f, h * 0.64f),
            c2 = Offset(w * 0.76f, h * 0.67f),
            p3 = Offset(w * 0.94f, h * 0.68f),
            weightPx = textPx * 0.09f
        )
    }
}

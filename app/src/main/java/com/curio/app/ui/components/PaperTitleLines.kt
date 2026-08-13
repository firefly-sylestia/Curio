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
 * v27t — the strokes now size to the title: their length scales with the
 * title's text length AND font size (reaching just past the end of the
 * text, capped so long titles don't overflow), and they sit at a slight
 * hand-written tilt with a gentle curve and a felt-pen edge (a wide soft
 * pass under a narrow dark pass per stroke). Place directly BELOW the title
 * text; alignment follows the parent column.
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
    // ~0.62em average glyph width for the ExtraBold hero font; stretch ~3
    // chars past the end of the text, cap at 16em and 300dp, floor at 5em
    // so even one-word titles get a visible underline.
    val linePx = min((title.length + 3) * textPx * 0.62f, textPx * 16f)
        .coerceAtLeast(textPx * 5f)
    val cappedPx = min(linePx, with(density) { 300.dp.toPx() })
    val width = with(density) { cappedPx.toDp() }
    val height = with(density) { (textPx * 0.62f).toDp() }
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

        fun stroke(start: Offset, end: Offset, control: Offset, weightPx: Float) {
            val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticBezierTo(control.x, control.y, end.x, end.y)
            }
            // Felt-pen edge: a wide soft pass under a narrow dark pass.
            drawPath(path, faint, style = Stroke(width = weightPx * 1.5f, cap = cap))
            drawPath(path, main, style = Stroke(width = weightPx, cap = cap))
        }

        // Top stroke: long, gentle sag toward the end. Second stroke: shorter,
        // starts offset and rises slightly — a hand-written double underline
        // never runs perfectly parallel.
        stroke(
            start = Offset(0f, h * 0.40f),
            end = Offset(w, h * 0.47f),
            control = Offset(w * 0.55f, h * 0.64f),
            weightPx = textPx * 0.11f
        )
        stroke(
            start = Offset(w * 0.08f, h * 0.74f),
            end = Offset(w * 0.70f, h * 0.67f),
            control = Offset(w * 0.36f, h * 0.55f),
            weightPx = textPx * 0.09f
        )
    }
}

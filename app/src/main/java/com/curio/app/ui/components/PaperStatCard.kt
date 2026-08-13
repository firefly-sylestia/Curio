package com.curio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v27u — the shared "paper stat card" surface used by the Home Streak ·
 * Cabinet · Topics bar and the Profile Level · Saved · Lanes pane when the
 * "Paper stat card" experiment is on.
 *
 * The surface is an OPAQUE paper fill clipped to [shape]. With [holesOn] the
 * classic 3-hole column is punched through the LEFT edge as an EvenOdd path
 * (the page behind shows through the holes) and each hole wears either the
 * pressed two-tone diary-spiral rim ([ringsOn] = false) or a tilted metal
 * book ring through the hole ([ringsOn] = true). With holes off it is simply
 * the opaque paper fill in the card's shape — no translucent fills anywhere
 * (v27n shadow rule).
 */
fun Modifier.paperStatCardFill(
    shape: Shape,
    fill: Color,
    holesOn: Boolean,
    ringsOn: Boolean,
    ink: Color
): Modifier = if (!holesOn) {
    background(fill, shape)
} else {
    drawWithCache {
        val holeR = PAPER_HOLE_RADIUS.dp.toPx()
        val holeX = PAPER_HOLE_X.dp.toPx()
        // Punch through the SAME outline the Surface wears (torn or rounded),
        // so the card edge and the holes read as one piece of paper.
        val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
        val basePath = (outline as? Outline.Generic)?.path
        val path = Path().apply {
            if (basePath != null) {
                addPath(basePath)
            } else {
                addRoundRect(
                    RoundRect(Rect(Offset.Zero, size), CornerRadius(20.dp.toPx()))
                )
            }
            repeat(3) { i ->
                val cy = size.height * (i + 1) / 4f
                addOval(Rect(Offset(holeX, cy), holeR), Path.Direction.Clockwise)
            }
            fillType = PathFillType.EvenOdd
        }
        onDrawBehind {
            drawPath(path, fill)
            repeat(3) { i ->
                val cy = size.height * (i + 1) / 4f
                val center = Offset(holeX, cy)
                if (ringsOn) {
                    drawBookRing(center, holeR, ink, index = i)
                } else {
                    drawPressedRim(center, holeR, ink)
                }
            }
        }
    }
}

/** The paper card's warm cream in light mode, warm rose-brown in dark. */
@Composable
fun paperStatCardColor(base: Color): Color =
    if (isCurioDarkTheme()) lerp(base, Color(0xFF2A211C), 0.50f)
    else lerp(base, Color(0xFFFFF6EB), 0.62f)

/** The pressed two-tone rim around a punch hole (light top-left, shadow bottom-right). */
private fun DrawScope.drawPressedRim(center: Offset, holeR: Float, ink: Color) {
    val ringR = holeR + 1.7.dp.toPx()
    val ringTopLeft = Offset(center.x - ringR, center.y - ringR)
    val ringSize = Size(ringR * 2f, ringR * 2f)
    val ringStroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    // Faint full edge — the punched paper lip.
    drawCircle(
        color = ink.copy(alpha = 0.10f),
        radius = ringR,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
    // Highlight arc (top-left).
    drawArc(
        color = Color.White.copy(alpha = 0.40f),
        startAngle = 160f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = ringTopLeft,
        size = ringSize,
        style = ringStroke
    )
    // Shadow arc (bottom-right).
    drawArc(
        color = ink.copy(alpha = 0.22f),
        startAngle = 340f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = ringTopLeft,
        size = ringSize,
        style = ringStroke
    )
}

/**
 * A tilted metal book ring through the hole — like a ring-bound notebook.
 * The ring is an ellipse (a circle foreshortened by perspective), tilted a
 * few degrees with a hand-set variation per ring, drawn as a metal tube
 * (bright top → shaded where it slips into the hole) with a specular
 * highlight and a contact shade at the hole.
 */
private fun DrawScope.drawBookRing(center: Offset, holeR: Float, ink: Color, index: Int) {
    val ringR = holeR + 0.8.dp.toPx() // ring centerline hugs the hole edge
    val metalW = 3.dp.toPx()
    val rx = ringR
    val ry = ringR * 0.78f // foreshortened — viewed at an angle
    val topLeft = Offset(center.x - rx, center.y - ry)
    val ringSize = Size(rx * 2f, ry * 2f)
    // Each ring tilts a little differently (-9°, -3°, 3°) — hand-set binder.
    val tilt = -9f + index * 6f
    rotate(degrees = tilt, pivot = center) {
        // Metal tube: light catches the top, darkens toward the hole.
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF6F3ED),
                    Color(0xFFCDC7BD),
                    Color(0xFF8F8980)
                ),
                start = Offset(center.x, center.y - ry),
                end = Offset(center.x, center.y + ry)
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = Stroke(width = metalW, cap = StrokeCap.Round)
        )
        // Specular highlight on the ring's top edge.
        drawArc(
            color = Color.White.copy(alpha = 0.85f),
            startAngle = 245f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = Stroke(width = metalW * 0.32f, cap = StrokeCap.Round)
        )
        // Contact shade where the ring enters the hole (bottom-right).
        drawArc(
            color = ink.copy(alpha = 0.28f),
            startAngle = 95f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = Stroke(width = metalW * 0.8f, cap = StrokeCap.Round)
        )
    }
}

private val PAPER_HOLE_RADIUS = 5.5f
private val PAPER_HOLE_X = 14f

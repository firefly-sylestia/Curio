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
 * pressed two-tone diary-spiral rim ([ringsOn] = false) or a 3D steel ring
 * through the hole ([ringsOn] = true). The [ringStyle] selects the look:
 * "coil" — a spring-like wire coil through the hole (front arc over the
 * paper, back arc receding into the hole); "split" — a closed metal torus
 * whose top arc sits proud above the paper; "oblique" — a few short coil
 * segments angled out of the hole toward the viewer. With holes off it is
 * simply the opaque paper fill in the card's shape — no translucent fills
 * anywhere (v27n shadow rule).
 */
fun Modifier.paperStatCardFill(
    shape: Shape,
    fill: Color,
    holesOn: Boolean,
    ringsOn: Boolean,
    ringStyle: String = "coil",
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
                    when (ringStyle) {
                        "split" -> drawSplitRing(center, holeR, ink)
                        "oblique" -> drawObliqueCoil(center, holeR, ink, index = i)
                        else -> drawCoilRing(center, holeR, ink)
                    }
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

/** Steel gradient stops shared by all three ring styles — bright top,
 *  mid tone, dark bottom — so they read as the same polished metal. */
private val steelGradient = listOf(
    Color(0xFFFAF8F2),
    Color(0xFFD8D2C6),
    Color(0xFFA8A094),
    Color(0xFF7A7268)
)

/** The shadow a ring casts on the paper, shared by all styles. */
private fun DrawScope.drawRingContactShadow(center: Offset, holeR: Float, ink: Color) {
    // A soft dark bloom just under the hole where the ring presses the paper.
    drawCircle(
        color = ink.copy(alpha = 0.16f),
        radius = holeR + 2.4.dp.toPx(),
        center = Offset(center.x, center.y + 1.4.dp.toPx())
    )
    // Crisp hairline where the metal touches the paper at the hole rim.
    drawCircle(
        color = ink.copy(alpha = 0.30f),
        radius = holeR + 0.6.dp.toPx(),
        center = center,
        style = Stroke(width = 0.9.dp.toPx())
    )
}

/**
 * "coil" — a spring-like wire coil through the hole (the closest match to
 * a spiral notebook binding). The ring is a foreshortened ellipse whose
 * FRONT arc tubes over the paper in bright metal while the BACK arc
 * recedes darkly INTO the hole, with a specular highlight on top and the
 * contact shadow where the wire leaves the paper.
 */
private fun DrawScope.drawCoilRing(center: Offset, holeR: Float, ink: Color) {
    val rx = holeR + 1.6.dp.toPx() // wire centerline — pokes past the hole
    val ry = rx * 0.82f            // mild foreshortening
    val metalW = 3.1.dp.toPx()
    val topLeft = Offset(center.x - rx, center.y - ry)
    val ringSize = Size(rx * 2f, ry * 2f)
    val steel = Brush.linearGradient(
        colors = steelGradient,
        start = Offset(center.x, center.y - ry),
        end = Offset(center.x, center.y + ry)
    )
    // ── Back arc (bottom half) — recedes into the hole, darkened. ──────
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF6E675E), Color(0xFF4A453E)),
            start = Offset(center.x, center.y),
            end = Offset(center.x, center.y + ry)
        ),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = topLeft,
        size = ringSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )
    // ── Front arc (top half) — bright metal tubing OVER the paper. ─────
    drawArc(
        brush = steel,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = topLeft,
        size = ringSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )
    // Specular highlight riding the front arc's upper edge.
    drawArc(
        color = Color.White.copy(alpha = 0.92f),
        startAngle = 200f,
        sweepAngle = 45f,
        useCenter = false,
        topLeft = topLeft,
        size = ringSize,
        style = Stroke(width = metalW * 0.28f, cap = StrokeCap.Round)
    )
    // Darken the wire where it dives into the hole on both sides.
    drawArc(
        color = Color(0xFF3A362F).copy(alpha = 0.55f),
        startAngle = 88f,
        sweepAngle = 22f,
        useCenter = false,
        topLeft = topLeft,
        size = ringSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFF3A362F).copy(alpha = 0.55f),
        startAngle = 268f,
        sweepAngle = 22f,
        useCenter = false,
        topLeft = topLeft,
        size = ringSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )
    drawRingContactShadow(center, holeR, ink)
}

/**
 * "split" — a closed metal torus (keyring / split-ring binder look). The
 * whole loop is a steel tube with a bright top edge; the bottom arc dips
 * into the hole where the paper rim shades it, and a specular glint rides
 * the top-left curve. Reads as a solid ring rather than a spring.
 */
private fun DrawScope.drawSplitRing(center: Offset, holeR: Float, ink: Color) {
    val rx = holeR + 1.5.dp.toPx()
    val ry = rx * 0.90f
    val metalW = 3.4.dp.toPx()
    val topLeft = Offset(center.x - rx, center.y - ry)
    val ringSize = Size(rx * 2f, ry * 2f)
    // Full tube with a vertical steel gradient (bright top → dark bottom).
    drawArc(
        brush = Brush.linearGradient(
            colors = steelGradient,
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
    // The split: a tiny dark gap at the top-right where the ring opens.
    drawArc(
        color = Color(0xFF3A362F),
        startAngle = 315f,
        sweepAngle = 14f,
        useCenter = false,
        topLeft = topLeft,
        size = ringSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )
    // Specular glint along the top-left curve.
    drawArc(
        color = Color.White.copy(alpha = 0.90f),
        startAngle = 230f,
        sweepAngle = 42f,
        useCenter = false,
        topLeft = topLeft,
        size = ringSize,
        style = Stroke(width = metalW * 0.30f, cap = StrokeCap.Round)
    )
    // Paper rim shades the bottom arc where it dips into the hole.
    drawArc(
        color = ink.copy(alpha = 0.30f),
        startAngle = 92f,
        sweepAngle = 56f,
        useCenter = false,
        topLeft = topLeft,
        size = ringSize,
        style = Stroke(width = metalW * 0.75f, cap = StrokeCap.Round)
    )
    drawRingContactShadow(center, holeR, ink)
}

/**
 * "oblique" — a few short coil segments angled out of the hole toward the
 * viewer (a chunky stylized "sticking out" spring). Three short tube
 * segments step diagonally across the hole, each a bright-to-dark steel
 * arc with its own specular, so the coil reads as springing OUT of the
 * paper toward the camera.
 */
private fun DrawScope.drawObliqueCoil(center: Offset, holeR: Float, ink: Color, index: Int) {
    // Per-ring phase shift so the three holes don't all tilt identically.
    val phase = index * 30f
    val rx = holeR + 1.2.dp.toPx()
    val ry = rx * 0.55f
    val metalW = 3.0.dp.toPx()
    // Three segments stepping along a diagonal — like a spring seen at an
    // angle, each turn poking slightly out of the hole toward the viewer.
    repeat(3) { s ->
        val t = (s - 1) * 0.42f
        val cx = center.x + t * rx * 0.55f
        val cy = center.y + t * ry * 0.75f
        val topLeft = Offset(cx - rx, cy - ry)
        val ringSize = Size(rx * 2f, ry * 2f)
        // Alternate light/dark turns so depth reads through the coil.
        val colors = if (s % 2 == 0) {
            listOf(Color(0xFFF6F2EA), Color(0xFFC9C2B4), Color(0xFF8E877B))
        } else {
            listOf(Color(0xFFB9B2A6), Color(0xFF8E877B), Color(0xFF6A645A))
        }
        rotate(degrees = phase + s * 9f, pivot = Offset(cx, cy)) {
            drawArc(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset(cx, cy - ry),
                    end = Offset(cx, cy + ry)
                ),
                startAngle = 0f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = metalW, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.85f),
                startAngle = 235f + s * 14f,
                sweepAngle = 30f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = metalW * 0.26f, cap = StrokeCap.Round)
            )
        }
    }
    drawRingContactShadow(center, holeR, ink)
}

private val PAPER_HOLE_RADIUS = 5.5f
private val PAPER_HOLE_X = 14f

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
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.toHsl

/**
 * v27u — the shared "paper stat card" surface used by the Home Streak ·
 * Cabinet · Topics bar and the Profile Level · Saved · Lanes pane when the
 * "Paper stat card" experiment is on.
 *
 * The surface is an OPAQUE paper fill clipped to [shape]. With [holesOn] the
 * classic 3-hole column is punched through the LEFT edge as an EvenOdd path
 * (the page behind shows through the holes) and each hole wears either the
 * pressed two-tone diary-spiral rim ([ringsOn] = false) or a 3D steel ring
 * through the hole ([ringsOn] = true) — each style draws the wire as a
 * real THREADED ring: a dark shaded hole interior, a dark back arc
 * receding inside the hole (behind the paper), a bright front arc riding
 * the hole rim over the paper, and darkened dives where the wire sinks
 * back in. The [ringStyle] selects the look: "coil" — a spiral-notebook
 * binding wire (front arc over the rim, back arc recessed in the hole);
 * "split" — a closed keyring / split-ring loop (top half over paper,
 * bottom half inside the hole, split gap near the top); "oblique" — the
 * coil foreshortened at an angle, bulging out of the hole toward the
 * viewer. With holes off it is simply the opaque paper fill in the card's
 * shape — no translucent fills anywhere (v27n shadow rule).
 */
fun Modifier.paperStatCardFill(
    shape: Shape,
    fill: Color,
    holesOn: Boolean,
    ringsOn: Boolean,
    ringStyle: String = "coil",
    ink: Color,
    // v81 — dark mode: the wire's DARK metal tones would vanish on the
    // near-black paper, so they flip to light metal (the reversal).
    dark: Boolean = false
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
                        "split" -> drawSplitRing(center, holeR, ink, dark)
                        "oblique" -> drawObliqueCoil(center, holeR, ink, dark, index = i)
                        else -> drawCoilRing(center, holeR, ink, dark)
                    }
                } else {
                    drawPressedRim(center, holeR, ink)
                }
            }
        }
    }
}

/**
 * The paper card's fill. LIGHT: the warm cream, built from the [base]
 * (the hero/fill it sits on), so the Home rose, a Profile hero or a
 * detail page's category color each get a cream of their own shade —
 * theme- and color-aware; a whisper of warm brown keeps it reading as
 * paper, not paint. DARK (v81): a deep near-black warm tint of the base
 * hue so the paper reads as dark paper on the pitch-black page instead
 * of a glaring cream block (elevation via lightness, not brightness).
 */
@Composable
fun paperStatCardColor(base: Color): Color {
    if (isCurioDarkTheme()) {
        val a = toHsl(base)
        return fromHsl(a.h, (a.s * 0.35f).coerceAtMost(0.28f), 0.20f)
    }
    return lerp(base, Color(0xFFFFF6EB), 0.62f)
}

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
 * The dark INTERIOR of a punch hole — the punched opening reads as a real
 * hole IN the paper (a deep shadowed pocket), so any wire drawn inside it
 * reads as passing BEHIND the sheet instead of lying flat on the hole.
 * Drawn for every ring style before the wire, so the back arc always has
 * a dark recess to recede into.
 */
private fun DrawScope.drawHoleInterior(center: Offset, holeR: Float, ink: Color) {
    // Soft dark fill across the whole opening + a deeper rim ring on the
    // inside edge (light from the top-left, like the pressed rim).
    drawCircle(
        color = ink.copy(alpha = 0.18f),
        radius = holeR,
        center = center
    )
    drawCircle(
        color = ink.copy(alpha = 0.34f),
        radius = holeR,
        center = Offset(center.x, center.y - holeR * 0.18f),
        style = Stroke(width = 1.1.dp.toPx())
    )
}

/**
 * "coil" — a spiral-notebook binding wire threaded THROUGH the hole. The
 * hole interior is shaded dark (a real opening); the wire's BACK arc loops
 * behind the paper, seen recessed inside the hole's bottom, while its
 * FRONT arc rides over the hole rim in bright steel — half its tube over
 * the opening, half on the paper — with darkened dives where it re-enters
 * the hole and a specular on top. Reads as a wire passing through, not a
 * ring drawn around the hole.
 */
private fun DrawScope.drawCoilRing(center: Offset, holeR: Float, ink: Color, dark: Boolean) {
    val frontR = holeR * 1.02f  // front wire rides the hole rim
    val backR = holeR * 0.72f   // back wire — visibly INSIDE the hole
    val metalW = 3.0.dp.toPx()
    val frontTopLeft = Offset(center.x - frontR, center.y - frontR)
    val frontSize = Size(frontR * 2f, frontR * 2f)
    val backTopLeft = Offset(center.x - backR, center.y - backR)
    val backSize = Size(backR * 2f, backR * 2f)

    drawHoleInterior(center, holeR, ink)

    // ── Back arc — the wire BEHIND the paper, seen through the hole. ───
    drawArc(
        brush = Brush.linearGradient(
            colors = if (dark) CoilBackDark else listOf(Color(0xFF5A554C), Color(0xFF332F29)),
            start = Offset(center.x, center.y - backR),
            end = Offset(center.x, center.y + backR)
        ),
        startAngle = 35f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = backTopLeft,
        size = backSize,
        style = Stroke(width = metalW * 0.9f, cap = StrokeCap.Round)
    )

    // ── Front arc — bright steel OVER the paper, riding the hole rim. ──
    drawArc(
        brush = Brush.linearGradient(
            colors = steelGradient,
            start = Offset(center.x, center.y - frontR),
            end = Offset(center.x, center.y + frontR)
        ),
        startAngle = 145f,
        sweepAngle = 250f,
        useCenter = false,
        topLeft = frontTopLeft,
        size = frontSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )

    // Darkened dives where the front wire sinks back INTO the hole.
    val dive = if (dark) DiveMetalDark else Color(0xFF3A362F)
    drawArc(
        color = dive.copy(alpha = 0.55f),
        startAngle = 145f,
        sweepAngle = 26f,
        useCenter = false,
        topLeft = frontTopLeft,
        size = frontSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )
    drawArc(
        color = dive.copy(alpha = 0.55f),
        startAngle = 9f,
        sweepAngle = 26f,
        useCenter = false,
        topLeft = frontTopLeft,
        size = frontSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )

    // Specular highlight riding the front arc's top.
    drawArc(
        color = Color.White.copy(alpha = 0.92f),
        startAngle = 252f,
        sweepAngle = 40f,
        useCenter = false,
        topLeft = frontTopLeft,
        size = frontSize,
        style = Stroke(width = metalW * 0.28f, cap = StrokeCap.Round)
    )

    drawRingContactShadow(center, holeR, ink)
}

/**
 * "split" — a closed steel keyring / split-ring binder loop threaded
 * through the hole. The ring's TOP half is a bright tube riding over the
 * paper; its BOTTOM half recedes behind the sheet, seen dark inside the
 * shaded hole, with the classic split gap near the top and a glint on the
 * upper-left curve.
 */
private fun DrawScope.drawSplitRing(center: Offset, holeR: Float, ink: Color, dark: Boolean) {
    val frontR = holeR * 1.05f
    val backR = holeR * 0.82f
    val metalW = 3.2.dp.toPx()
    val frontTopLeft = Offset(center.x - frontR, center.y - frontR)
    val frontSize = Size(frontR * 2f, frontR * 2f)
    val backTopLeft = Offset(center.x - backR, center.y - backR)
    val backSize = Size(backR * 2f, backR * 2f)

    drawHoleInterior(center, holeR, ink)

    // ── Back half — inside the hole, behind the paper. ──────────────────
    drawArc(
        brush = Brush.linearGradient(
            colors = if (dark) SplitBackDark else listOf(Color(0xFF575249), Color(0xFF322E28)),
            start = Offset(center.x, center.y - backR),
            end = Offset(center.x, center.y + backR)
        ),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = backTopLeft,
        size = backSize,
        style = Stroke(width = metalW * 0.92f, cap = StrokeCap.Round)
    )
    // The hole's edge passes in front of the back wire — shade it there.
    drawArc(
        color = ink.copy(alpha = 0.35f),
        startAngle = 30f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = backTopLeft,
        size = backSize,
        style = Stroke(width = metalW * 0.7f, cap = StrokeCap.Round)
    )

    // ── Front half — bright steel tube riding OVER the paper. ───────────
    drawArc(
        brush = Brush.linearGradient(
            colors = steelGradient,
            start = Offset(center.x, center.y - frontR),
            end = Offset(center.x, center.y + frontR)
        ),
        startAngle = 160f,
        sweepAngle = 200f,
        useCenter = false,
        topLeft = frontTopLeft,
        size = frontSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )
    // The split: a tiny dark gap near the top where the ring opens.
    drawArc(
        color = if (dark) DiveMetalDark else Color(0xFF3A362F),
        startAngle = 260f,
        sweepAngle = 13f,
        useCenter = false,
        topLeft = frontTopLeft,
        size = frontSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )
    // Specular glint along the upper-left curve.
    drawArc(
        color = Color.White.copy(alpha = 0.90f),
        startAngle = 205f,
        sweepAngle = 38f,
        useCenter = false,
        topLeft = frontTopLeft,
        size = frontSize,
        style = Stroke(width = metalW * 0.30f, cap = StrokeCap.Round)
    )

    drawRingContactShadow(center, holeR, ink)
}

/**
 * "oblique" — the binding coil seen at an angle, springing OUT of the
 * hole toward the viewer. A foreshortened, per-hole tilted ring whose
 * front arc bulges clearly onto the paper (bright steel) while its back
 * arc recedes darkly inside the shaded hole — the most pronounced
 * "through the hole" read of the three.
 */
private fun DrawScope.drawObliqueCoil(center: Offset, holeR: Float, ink: Color, dark: Boolean, index: Int) {
    // Per-ring phase shift so the three holes don't all tilt identically.
    val phase = index * 30f
    val frontR = holeR * 1.35f  // front bulge — clearly past the hole rim
    val ry = frontR * 0.62f     // foreshortened
    val backR = holeR * 0.72f
    val metalW = 2.9.dp.toPx()
    val topLeft = Offset(center.x - frontR, center.y - ry)
    val size = Size(frontR * 2f, ry * 2f)
    val backTopLeft = Offset(center.x - backR, center.y - backR)
    val backSize = Size(backR * 2f, backR * 2f)

    drawHoleInterior(center, holeR, ink)

    // ── Back arc — inside the hole, behind the paper. ───────────────────
    drawArc(
        brush = Brush.linearGradient(
            colors = if (dark) SplitBackDark else listOf(Color(0xFF575249), Color(0xFF322E28)),
            start = Offset(center.x, center.y - backR),
            end = Offset(center.x, center.y + backR)
        ),
        startAngle = 30f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = backTopLeft,
        size = backSize,
        style = Stroke(width = metalW, cap = StrokeCap.Round)
    )

    // ── Front arc — bright steel bulging OUT of the hole onto the paper.
    rotate(degrees = phase + 8f, pivot = center) {
        drawArc(
            brush = Brush.linearGradient(
                colors = steelGradient,
                start = Offset(center.x, center.y - ry),
                end = Offset(center.x, center.y + ry)
            ),
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = metalW, cap = StrokeCap.Round)
        )
        // Dives where the front wire sinks back into the hole.
        val dive = if (dark) DiveMetalDark else Color(0xFF3A362F)
        drawArc(
            color = dive.copy(alpha = 0.55f),
            startAngle = 150f,
            sweepAngle = 26f,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = metalW, cap = StrokeCap.Round)
        )
        drawArc(
            color = dive.copy(alpha = 0.55f),
            startAngle = 364f,
            sweepAngle = 26f,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = metalW, cap = StrokeCap.Round)
        )
        // Specular highlight riding the front arc's top.
        drawArc(
            color = Color.White.copy(alpha = 0.88f),
            startAngle = 252f,
            sweepAngle = 38f,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = metalW * 0.26f, cap = StrokeCap.Round)
        )
    }

    drawRingContactShadow(center, holeR, ink)
}

// v81 — dark-mode METAL tones for the ring shading: on the near-black
// paper the dark wire tones would vanish, so they flip to light greys
// (the wire catches light on the dark sheet). Light mode keeps the
// original dark metals unchanged.
private val CoilBackDark = listOf(Color(0xFF9A948A), Color(0xFF6E6A61))
private val SplitBackDark = listOf(Color(0xFF958F85), Color(0xFF69655C))
private val DiveMetalDark = Color(0xFF76726A)

private val PAPER_HOLE_RADIUS = 5.5f
private val PAPER_HOLE_X = 14f

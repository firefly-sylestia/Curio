package com.curio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.toHsl

/**
 * v209 — the shared "paper stat card" surface used by the Home Streak ·
 * Cabinet · Topics bar and the Profile Level · Saved · Lanes pane when the
 * "Paper stat card" experiment is on.
 *
 * The surface is an OPAQUE paper fill clipped to [shape]. With [holesOn] the
 * classic 3-hole column is punched through the LEFT edge as an EvenOdd path
 * (the page behind shows through the holes).
 *
 * The 3D steel coil ring (spiral-notebook binding wire) always draws on
 * the card's LEFT edge, independent of the pin holes — it decorates the
 * card regardless. When [holesOn] is true the coil THREADS THROUGH the
 * holes; when false it sits on the card edge alone.
 *
 * With holes off the card is simply the opaque paper fill + the coil —
 * no translucent fills anywhere (v27n shadow rule).
 */
fun Modifier.paperStatCardFill(
    shape: Shape,
    fill: Color,
    holesOn: Boolean,
    ink: Color,
    // v81 — dark mode: the wire's DARK metal tones would vanish on the
    // near-black paper, so they flip to light metal (the reversal).
    dark: Boolean = false
): Modifier = drawWithCache {
    val holeR = PAPER_HOLE_RADIUS.dp.toPx()
    val holeX = PAPER_HOLE_X.dp.toPx()
    val ringOffX = 1.dp.toPx()
    val ringOffY = 3.dp.toPx()
    if (holesOn) {
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
            // The coil rings thread through the punched holes.
            repeat(3) { i ->
                val cy = size.height * (i + 1) / 4f
                val ringCenter = Offset(holeX - ringOffX, cy - ringOffY)
                drawCoilRing(ringCenter, holeR, ink, dark)
            }
        }
    } else {
        // No holes — opaque fill + the coil as a standalone decoration
        // on the card's left edge.
        onDrawBehind {
            drawRect(fill)
            // Three coil rings at the same hole positions, drawn over the
            // fill so they read as the binding wire wrapping the left edge.
            repeat(3) { i ->
                val cy = size.height * (i + 1) / 4f
                val ringCenter = Offset(holeX - ringOffX, cy - ringOffY)
                drawCoilRing(ringCenter, holeR, ink, dark)
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
 * "coil" — the spiral-notebook binding wire THREADED THROUGH the hole, per
 * the user's reference SVG (v27w + v197): the wire is a FORESHORTENED ARCH
 * rising up the left side, over the top and down the right (the revised
 * SVG dropped the bottom curl — 73:38 aspect), and it PEEKS OUT of the
 * card's LEFT edge like a real spiral binding instead of sitting entirely
 * inside the card (user: "the ring should be come out from the left of it
 * like peek out from the left not entirely inside the stat card"). Drawn
 * OVER the shaded hole interior ([drawHoleInterior]) so the wire reads as
 * wrapping around the opening: a dark depth stroke behind the wire (its
 * shadowed underside — the SVG's 18px dark pass), the metal coil on top
 * (horizontal tube gradient — the SVG's 13px pass), and a white specular
 * along the upper-left curve (the SVG's 3px highlight). The metal stops
 * are tuned from the SVG's palette.
 */
private fun DrawScope.drawCoilRing(center: Offset, holeR: Float, ink: Color, dark: Boolean) {
    drawHoleInterior(center, holeR, ink)

    val wireW = 3.2.dp.toPx()
    // The arch is wider than the hole (still ~2.1× the hole diameter) so
    // the hole reads inside its opening, and it's pushed LEFT past the
    // card edge — its left arc + leg visibly protrude ~6.5dp like a real
    // spiral binding sticking out of the paper, instead of sitting
    // entirely inside the card (user: "the ring should be come out from
    // the left of it like peek out from the left not entirely inside the
    // stat card"). v208f — the path is MIRRORED to the SVG's own
    // `matrix(-1,0,0,1,0,0)` (the app was rendering it inverted vs the
    // author's art): the wire STARTS inside the hole at the right
    // (bottom-right corner of the box), arches up over the top, and the
    // LEFT leg dives down past the card's left edge to an open round-capped
    // end. Foreshortened to the reference SVG's 73:51 aspect.
    val coilW = holeR * 4.2f
    val coilH = coilW * (51f / 73f)
    val leftPeek = 9.dp.toPx()
    val topLeft = Offset(
        center.x - coilW / 2f - leftPeek,
        center.y - coilH / 2f
    )

    val metal = Brush.linearGradient(
        colors = if (dark) CoilMetalDark else CoilMetal,
        start = topLeft,
        end = Offset(topLeft.x + coilW, topLeft.y)
    )
    val outline = buildCoilPath(topLeft, coilW, coilH)

    // Dark depth — the coil's shadowed underside (a wider stroke behind).
    drawPath(
        path = outline,
        color = if (dark) Color(0xFF22282F) else Color(0xFF101B27),
        style = Stroke(width = wireW * 1.5f, cap = StrokeCap.Round)
    )
    // The metal wire.
    drawPath(
        path = outline,
        brush = metal,
        style = Stroke(width = wireW, cap = StrokeCap.Round)
    )
    // Specular along the upper-left curve.
    drawPath(
        path = buildCoilHighlightPath(topLeft, coilW, coilH),
        color = Color.White.copy(alpha = if (dark) 0.60f else 0.75f),
        style = Stroke(width = wireW * 0.22f, cap = StrokeCap.Round)
    )
}

/**
 * The coil's tube gradient — tuned from the user's reference SVG stops: a
 * cool polished steel, dark edges wrapping to a bright specular band.
 * Light mode uses the full metal; dark mode flips to light steel so the
 * wire reads on the near-black paper (the v81 dark reversal).
 */
private val CoilMetal = listOf(
    Color(0xFF1c2937),
    Color(0xFF52687d),
    Color(0xFFdce9f6),
    Color(0xFF8196aa),
    Color(0xFFFFFFFF),
    Color(0xFF9caec0),
    Color(0xFF43586c),
    Color(0xFF172432)
)

private val CoilMetalDark = listOf(
    Color(0xFF8E9AA8),
    Color(0xFFC4D0DD),
    Color(0xFFF4F8FC),
    Color(0xFFA9B6C4),
    Color(0xFFFFFFFF),
    Color(0xFFBCC8D5),
    Color(0xFFA0ADBB),
    Color(0xFF7C8896)
)

/**
 * The coil OUTLINE from the user's LATEST reference SVG (svgviewer-output
 * (15).svg), normalized to the 73×38 bounding box and MIRRORED to match
 * the SVG's own `matrix(-1,0,0,1,0,0)` transform (v208f — the app was
 * rendering the ring inverted vs the author's art: "see the svg its
 * inverted of what its in the app youre plaing it wrngly"). So the wire
 * starts at the bottom-RIGHT corner (inside the paper, near the hole),
 * rises up the right side, arches over the top and down the LEFT side,
 * then the LEFT leg DIVES below the box past the card's left edge and
 * ends OPEN with a round cap — the old curl-back is gone AND the leg no
 * longer stops blunt mid-air at y=0.737. Each cubic is c1/c2/end triples
 * of normalized coordinates.
 */
private val CoilOutlineNorm = floatArrayOf(
    1.000f, 1.000f,
    1.000f, 0.395f, 0.781f, 0.000f, 0.479f, 0.000f,
    0.178f, 0.000f, 0.000f, 0.342f, 0.000f, 0.737f,
    0.000f, 1.105f, 0.123f, 1.342f, 0.288f, 1.342f
)

/** The coil's specular line — the SVG's `M43 57 C45 39 58 29 76 29 C92
 *  29 103 37 106 48` mirrored horizontally (x → 1−x) to ride the flipped
 *  outline's upper-LEFT curve. */
private val CoilSpecularNorm = floatArrayOf(
    0.932f, 0.868f,
    0.904f, 0.395f, 0.726f, 0.132f, 0.479f, 0.132f,
    0.260f, 0.132f, 0.110f, 0.342f, 0.068f, 0.632f
)

/** Scales one of the normalized coil paths into [topLeft] + [w]×[h]. */
private fun buildCoilPath(topLeft: Offset, w: Float, h: Float): Path {
    val p = Path()
    p.moveTo(topLeft.x + CoilOutlineNorm[0] * w, topLeft.y + CoilOutlineNorm[1] * h)
    var i = 2
    while (i < CoilOutlineNorm.size) {
        p.cubicTo(
            topLeft.x + CoilOutlineNorm[i] * w, topLeft.y + CoilOutlineNorm[i + 1] * h,
            topLeft.x + CoilOutlineNorm[i + 2] * w, topLeft.y + CoilOutlineNorm[i + 3] * h,
            topLeft.x + CoilOutlineNorm[i + 4] * w, topLeft.y + CoilOutlineNorm[i + 5] * h
        )
        i += 6
    }
    return p
}

/** Scales the specular line into the same box. */
private fun buildCoilHighlightPath(topLeft: Offset, w: Float, h: Float): Path {
    val p = Path()
    p.moveTo(topLeft.x + CoilSpecularNorm[0] * w, topLeft.y + CoilSpecularNorm[1] * h)
    var i = 2
    while (i < CoilSpecularNorm.size) {
        p.cubicTo(
            topLeft.x + CoilSpecularNorm[i] * w, topLeft.y + CoilSpecularNorm[i + 1] * h,
            topLeft.x + CoilSpecularNorm[i + 2] * w, topLeft.y + CoilSpecularNorm[i + 3] * h,
            topLeft.x + CoilSpecularNorm[i + 4] * w, topLeft.y + CoilSpecularNorm[i + 5] * h
        )
        i += 6
    }
    return p
}

/**
 * "split" — a closed steel keyring / split-ring binder loop threaded
 * through the hole. The ring's TOP half is a bright tube riding over the
 * paper; its BOTTOM half recedes behind the sheet, seen dark inside the
 * shaded hole, with the classic split gap near the top and a glint on the


private val PAPER_HOLE_RADIUS = 5.5f
private val PAPER_HOLE_X = 14f

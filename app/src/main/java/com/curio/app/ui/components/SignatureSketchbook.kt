package com.curio.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CategoryFamily
import com.curio.app.ui.theme.PatrickHandFontFamily

/**
 * v428 — THE SIGNATURE SHARE CARD IS A SKETCHBOOK PAGE.
 *
 * The member: *\"make the design like collections style like hand drawn doodles\"*
 * and *\"do one unique per category for signature cards\"* — so the Signature
 * design stops being a set of quiet printed layouts and becomes a page from a
 * notebook: a sheet of warm paper, a wobbly pen outline, a patch of pencil
 * hatching for shading, a strip of washi tape holding a corner down, and ONE
 * hand-drawn motif that belongs to that category and to no other.
 *
 * HOW IT IS DRAWN, AND WHY IT IS DRAWN THIS WAY:
 *
 *  - **Nothing is an image.** Every line here is a [Path] with its points pushed
 *    around by a small deterministic hash ([jitter]) — a steady hand never made a
 *    straight line, and a drawing that re-jitters on every frame would shimmer.
 *    Same seed, same wobble, every time: the paper is identical on the editor, on
 *    the share and on the exported PNG.
 *  - **Hatching is the shading.** A sketcher does not fill, they cross-hatch, so
 *    [hatch] is how weight is added — never a rectangle of flat colour.
 *  - **The pen has a colour, the paper has a colour, the marker has a colour.**
 *    Each page carries three: [SketchPage.paper], [SketchPage.ink] and
 *    [SketchPage.accent] (the accent is the felt-tip the doodle is finished
 *    with, and it is also the card's badge and metadata ink).
 *  - **The hand is the type.** Titles and the footer are set in the app's own
 *    handwriting ([PatrickHandFontFamily]); the fact itself stays in a soft sans
 *    ([GeomFontFamily]) because a page you cannot read is not a nice page.
 *
 * ONE PAGE PER CATEGORY, and the page is chosen by NAME (see [sketchPageFor]) —
 * the same key the rest of the design system uses. A lane whose name is not in
 * the table yet falls to its FAMILY's page ([sketchFamilyPage]), and only a
 * nameless-and-familyless card reaches the grey [SKETCH_FALLBACK] — a lane added
 * to the app never loses its Signature card, it just shares its family's doodle
 * until it gets one of its own.
 *
 * The old per-category layouts are gone from `TopicShareCard.kt` on purpose: two
 * answers to \"what does this lane's Signature card look like\" is one answer too
 * many, and the classic designs the member kept (GAMES, FILMS) live on untouched
 * in `signatureDesignClassic`.
 */

// ═══════════════════════════════════════════════════════════════════════
// The pen
// ═══════════════════════════════════════════════════════════════════════

/**
 * A steady hand never made a straight line.
 *
 * A tiny integer hash (a 32-bit mix, no allocation, no Random instance) turns a
 * (seed, index) pair into a value in -1..1, so every point of every stroke can
 * be nudged by a hand that never moves twice the same way — but always the SAME
 * way for the same drawing, which is what makes the page stable.
 */
private fun jitter(seed: Int, index: Int): Float {
    var x = seed * 374761393 + index * 668265263
    x = (x xor (x shr 13)) * 1274126177
    x = x xor (x shr 16)
    return (x and 0x7FFF).toFloat() / 32767f * 2f - 1f
}

/** A hand-drawn stroke through [points] — the one primitive every doodle uses. */
private fun DrawScope.sketch(
    points: List<Offset>,
    ink: Color,
    width: Float = 1.5f,
    wobble: Float = 1.3f,
    seed: Int = 1,
    close: Boolean = false
) {
    if (points.size < 2) return
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = point.x + jitter(seed, index * 2) * wobble
        val y = point.y + jitter(seed, index * 2 + 1) * wobble
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (close) path.close()
    drawPath(path, ink, style = Stroke(width = width, cap = StrokeCap.Round))
}

/** A wobbly rectangle — a box drawn freehand, which is what a sketched box is. */
private fun DrawScope.sketchRect(
    left: Float, top: Float, right: Float, bottom: Float,
    ink: Color, width: Float = 1.5f, wobble: Float = 1.5f, seed: Int = 2
) = sketch(
    listOf(Offset(left, top), Offset(right, top), Offset(right, bottom), Offset(left, bottom)),
    ink, width, wobble, seed, close = true
)

/** A wobbly circle, drawn as a closed polygon of [steps] pushed-off points. */
private fun DrawScope.sketchCircle(
    centre: Offset, radius: Float, ink: Color,
    width: Float = 1.5f, wobble: Float = 1.1f, seed: Int = 3, steps: Int = 18
) {
    val points = List(steps) { step ->
        val angle = step / steps.toFloat() * 6.2831855f
        Offset(
            centre.x + kotlin.math.cos(angle) * radius,
            centre.y + kotlin.math.sin(angle) * radius
        )
    }
    sketch(points, ink, width, wobble, seed, close = true)
}

/**
 * A patch of pencil shading — the sketcher's fill.
 *
 * Lines run on a slant inside the box, and each one is a stroke like any other,
 * so the patch reads as work rather than as a texture.
 */
private fun DrawScope.hatch(
    left: Float, top: Float, width: Float, height: Float,
    ink: Color, lines: Int = 6, seed: Int = 8, strokeWidth: Float = 1.1f
) {
    for (index in 0 until lines) {
        val at = (index + 1f) / (lines + 1f)
        val x = left + width * at
        val drift = jitter(seed, index) * height * 0.16f
        sketch(
            listOf(
                Offset(x - width * 0.14f, top + height + drift),
                Offset(x + width * 0.14f, top + drift)
            ),
            ink.copy(alpha = 0.34f), strokeWidth, wobble = 0.8f, seed = seed + index
        )
    }
}

/** A strip of washi tape, pressed across a corner at a slight angle. */
private fun DrawScope.tape(centre: Offset, width: Float, height: Float, color: Color, seed: Int) {
    val half = width / 2f
    val tilt = jitter(seed, 1) * height * 0.35f
    val path = Path().apply {
        moveTo(centre.x - half, centre.y - height / 2f + tilt)
        lineTo(centre.x + half, centre.y - height / 2f - tilt)
        lineTo(centre.x + half, centre.y + height / 2f - tilt)
        lineTo(centre.x - half, centre.y + height / 2f + tilt)
        close()
    }
    drawPath(path, color.copy(alpha = 0.55f))
    drawPath(path, color.copy(alpha = 0.75f), style = Stroke(width = 1f))
}

/** A little hand-drawn arrow, the sketcher's way of pointing at things. */
private fun DrawScope.sketchArrow(from: Offset, to: Offset, ink: Color, seed: Int, width: Float = 1.3f) {
    sketch(listOf(from, to), ink.copy(alpha = 0.7f), width, wobble = 1.6f, seed = seed)
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val ux = dx / length
    val uy = dy / length
    val head = length * 0.22f
    sketch(
        listOf(
            Offset(to.x - ux * head - uy * head * 0.6f, to.y - uy * head + ux * head * 0.6f),
            to,
            Offset(to.x - ux * head + uy * head * 0.6f, to.y - uy * head - ux * head * 0.6f)
        ),
        ink.copy(alpha = 0.7f), width, wobble = 1.0f, seed = seed + 1
    )
}

/** The sheet itself: a warm wash, a grain of fibres, and the pen outline. */
private fun DrawScope.sketchPaper(page: SketchPage, w: Float, h: Float) {
    drawRect(page.paper, size = Size(w, h))
    // Two soft washes, off-centre, so no two corners of the sheet are the same
    // tone — the cheapest way a flat fill reads as paper instead of as a panel.
    drawRect(
        Brush.radialGradient(
            listOf(page.accent.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(w * 0.20f, h * 0.14f),
            radius = w * 1.05f
        ),
        size = Size(w, h)
    )
    drawRect(
        Brush.radialGradient(
            listOf(page.ink.copy(alpha = 0.07f), Color.Transparent),
            center = Offset(w * 0.88f, h * 0.94f),
            radius = w * 0.85f
        ),
        size = Size(w, h)
    )
    // Fibres: a light scatter, deterministic, sampled inside the sheet.
    for (index in 0 until 26) {
        val fx = (jitter(11, index * 3) * 0.5f + 0.5f) * w
        val fy = (jitter(23, index * 3) * 0.5f + 0.5f) * h
        drawCircle(page.ink.copy(alpha = 0.05f), radius = 1.1f, center = Offset(fx, fy))
    }
    // The pen outline: two passes, the second a touch off — a line gone over
    // twice, which is what makes a hand-drawn frame read as drawn.
    val inset = w * 0.035f
    sketchRect(inset, inset, w - inset, h - inset, page.ink.copy(alpha = 0.5f), 1.6f, 2.2f, seed = 5)
    sketchRect(
        inset + 3f, inset + 3.5f, w - inset - 2f, h - inset - 3f,
        page.ink.copy(alpha = 0.22f), 1.1f, 2.6f, seed = 9
    )
}

// ═══════════════════════════════════════════════════════════════════════
// The doodles — one per category, and none shared
// ═══════════════════════════════════════════════════════════════════════

/**
 * Every motif the sketchbook can draw.
 *
 * One entry per lane rather than a handful reused with a different colour: the
 * member's ask was *\"one unique per category\"*, and a lane whose doodle is
 * another lane's doodle is not unique however it is tinted.
 */
internal enum class SketchMotif {
    MIC, DICE, PAW, BOUNCE, SPEED, EASEL, QUILL, LEAF, BOOKSTACK, FLASK,
    CLAPPER, BULB, GRAPH, REEL, FORK, STRATA, COLUMN, GLOBE, BUBBLES, PANEL,
    PANEL_ARC, COMPASSES, BOLT, PALETTE, SPROUT, HEAD, QUOTES, ATOM, VINYL, NOTES,
    TELEVISION, GAMEPAD, TROPHY, CHIP, PLANET, PULSE, GEAR, WAVE, SQUIGGLE
}

/** Draw one motif, centred on [centre], sized to [size] (its longest side). */
private fun DrawScope.drawMotif(
    motif: SketchMotif, centre: Offset, size: Float, ink: Color, accent: Color, seed: Int
) {
    val s = size
    val cx = centre.x
    val cy = centre.y
    when (motif) {
        // A microphone on its stand, with a note coming off it.
        SketchMotif.MIC -> {
            sketchCircle(Offset(cx, cy - s * 0.18f), s * 0.16f, ink, 1.6f, 1.0f, seed, 16)
            sketch(listOf(Offset(cx - s * 0.05f, cy - s * 0.02f), Offset(cx - s * 0.05f, cy + s * 0.16f), Offset(cx - s * 0.18f, cy + s * 0.30f)), ink, 1.6f, 1.0f, seed + 1)
            sketch(listOf(Offset(cx - s * 0.14f, cy + s * 0.30f), Offset(cx + s * 0.14f, cy + s * 0.30f)), ink, 1.6f, 1.0f, seed + 2)
            sketchCircle(Offset(cx + s * 0.30f, cy - s * 0.26f), s * 0.07f, accent, 1.6f, 0.8f, seed + 3, 12)
            sketch(listOf(Offset(cx + s * 0.35f, cy - s * 0.24f), Offset(cx + s * 0.35f, cy - s * 0.46f)), accent, 1.5f, 0.8f, seed + 4)
        }
        // Two dice, one showing a four, one a three.
        SketchMotif.DICE -> {
            sketchRect(cx - s * 0.42f, cy - s * 0.34f, cx - s * 0.04f, cy + s * 0.04f, ink, 1.5f, 1.6f, seed)
            sketchRect(cx - s * 0.02f, cy - s * 0.12f, cx + s * 0.36f, cy + s * 0.34f, ink, 1.5f, 1.6f, seed + 1)
            listOf(-0.30f to -0.24f, -0.16f to -0.06f, -0.16f to -0.24f, -0.30f to -0.06f).forEach { (ox, oy) ->
                drawCircle(accent, radius = s * 0.035f, center = Offset(cx + s * ox, cy + s * oy))
            }
            listOf(0.08f to 0.02f, 0.08f to 0.18f, 0.24f to 0.10f).forEach { (ox, oy) ->
                drawCircle(ink, radius = s * 0.03f, center = Offset(cx + s * ox, cy + s * oy))
            }
        }
        // A paw print.
        SketchMotif.PAW -> {
            drawCircle(accent, radius = s * 0.17f, center = Offset(cx, cy + s * 0.12f))
            listOf(-0.24f to -0.16f, -0.06f to -0.26f, 0.14f to -0.24f, 0.28f to -0.08f).forEach { (ox, oy) ->
                sketchCircle(Offset(cx + s * ox, cy + s * oy), s * 0.075f, ink, 1.4f, 0.9f, seed + (ox * 10).toInt(), 12)
            }
        }
        // A ball caught mid-bounce, with the arc it came down on.
        SketchMotif.BOUNCE -> {
            for (index in 0..5) {
                val t = index / 5f
                drawCircle(ink.copy(alpha = 0.35f), radius = 1.4f, center = Offset(cx - s * 0.34f + s * 0.68f * t, cy - s * 0.28f + s * 0.4f * t * t))
            }
            sketchCircle(Offset(cx + s * 0.26f, cy + s * 0.16f), s * 0.15f, ink, 1.6f, 1.0f, seed, 16)
            sketch(listOf(Offset(cx - s * 0.44f, cy + s * 0.30f), Offset(cx + s * 0.44f, cy + s * 0.30f)), ink, 1.6f, 1.2f, seed + 1)
        }
        // Speed lines and a sparkle — the drawn shorthand for "movement".
        SketchMotif.SPEED -> {
            for (index in 0..3) {
                val y = cy - s * 0.22f + s * 0.14f * index
                sketch(listOf(Offset(cx - s * 0.44f, y), Offset(cx + s * 0.16f - s * 0.06f * index, y + s * 0.02f)), ink.copy(alpha = 0.7f), 1.5f, 1.6f, seed + index)
            }
            sketch(listOf(Offset(cx + s * 0.30f, cy - s * 0.26f), Offset(cx + s * 0.34f, cy + s * 0.02f)), accent, 1.6f, 1.0f, seed + 8)
            sketch(listOf(Offset(cx + s * 0.16f, cy - s * 0.10f), Offset(cx + s * 0.46f, cy - s * 0.14f)), accent, 1.6f, 1.0f, seed + 9)
        }
        // An easel holding a little landscape.
        SketchMotif.EASEL -> {
            sketchRect(cx - s * 0.30f, cy - s * 0.30f, cx + s * 0.30f, cy + s * 0.16f, ink, 1.5f, 1.5f, seed)
            sketch(listOf(Offset(cx - s * 0.30f, cy + s * 0.02f), Offset(cx - s * 0.06f, cy - s * 0.20f), Offset(cx + s * 0.16f, cy + s * 0.02f)), accent, 1.4f, 1.2f, seed + 1)
            hatch(cx - s * 0.26f, cy - s * 0.02f, s * 0.52f, s * 0.16f, ink, 4, seed + 2)
            sketch(listOf(Offset(cx, cy + s * 0.16f), Offset(cx - s * 0.02f, cy + s * 0.36f)), ink, 1.5f, 1.0f, seed + 3)
            sketch(listOf(Offset(cx - s * 0.16f, cy + s * 0.36f), Offset(cx + s * 0.18f, cy + s * 0.36f)), ink, 1.4f, 1.2f, seed + 4)
        }
        // A quill over a line of ink.
        SketchMotif.QUILL -> {
            sketch(listOf(Offset(cx - s * 0.30f, cy + s * 0.30f), Offset(cx + s * 0.22f, cy - s * 0.30f)), ink, 1.6f, 1.1f, seed)
            for (index in 0..4) {
                val t = 0.25f + index * 0.15f
                sketch(
                    listOf(
                        Offset(cx - s * 0.30f + s * 0.52f * t, cy + s * 0.30f - s * 0.60f * t),
                        Offset(cx - s * 0.08f + s * 0.30f * t, cy + s * 0.26f - s * 0.44f * t)
                    ),
                    ink.copy(alpha = 0.6f), 1.1f, 0.9f, seed + index
                )
            }
            sketch(listOf(Offset(cx - s * 0.34f, cy + s * 0.34f), Offset(cx - s * 0.26f, cy + s * 0.24f)), accent, 1.6f, 0.7f, seed + 7)
        }
        // A leaf with its veins.
        SketchMotif.LEAF -> {
            sketch(
                listOf(
                    Offset(cx - s * 0.28f, cy + s * 0.28f), Offset(cx - s * 0.34f, cy - s * 0.04f),
                    Offset(cx - s * 0.04f, cy - s * 0.34f), Offset(cx + s * 0.30f, cy - s * 0.24f),
                    Offset(cx + s * 0.16f, cy + s * 0.16f), Offset(cx - s * 0.28f, cy + s * 0.28f)
                ), ink, 1.6f, 1.4f, seed, close = true
            )
            sketch(listOf(Offset(cx - s * 0.28f, cy + s * 0.28f), Offset(cx + s * 0.12f, cy - s * 0.20f)), ink.copy(alpha = 0.7f), 1.2f, 1.0f, seed + 1)
            for (index in 0..2) {
                val t = 0.3f + index * 0.2f
                sketch(
                    listOf(
                        Offset(cx - s * 0.28f + s * 0.40f * t, cy + s * 0.28f - s * 0.48f * t),
                        Offset(cx - s * 0.10f + s * 0.36f * t, cy + s * 0.06f - s * 0.10f * t)
                    ), accent.copy(alpha = 0.8f), 1.1f, 0.9f, seed + index + 2
                )
            }
        }
        // Three books stacked, a ribbon in the top one.
        SketchMotif.BOOKSTACK -> {
            sketchRect(cx - s * 0.34f, cy - s * 0.30f, cx + s * 0.30f, cy - s * 0.14f, ink, 1.5f, 1.4f, seed)
            sketchRect(cx - s * 0.28f, cy - s * 0.10f, cx + s * 0.36f, cy + s * 0.06f, ink, 1.5f, 1.4f, seed + 1)
            sketchRect(cx - s * 0.36f, cy + s * 0.10f, cx + s * 0.24f, cy + s * 0.26f, ink, 1.5f, 1.4f, seed + 2)
            for (index in 0..2) {
                val x = cx - s * 0.32f + s * 0.16f * index
                sketch(listOf(Offset(x, cy - s * 0.13f), Offset(x, cy + s * 0.01f)), ink.copy(alpha = 0.45f), 1f, 0.7f, seed + index + 3)
            }
            sketch(listOf(Offset(cx + s * 0.22f, cy - s * 0.30f), Offset(cx + s * 0.22f, cy - s * 0.44f), Offset(cx + s * 0.14f, cy - s * 0.38f)), accent, 1.6f, 1.0f, seed + 6)
        }
        // A flask, three bubbles and a liquid line.
        SketchMotif.FLASK -> {
            sketch(
                listOf(
                    Offset(cx - s * 0.08f, cy - s * 0.32f), Offset(cx - s * 0.08f, cy),
                    Offset(cx - s * 0.30f, cy + s * 0.30f), Offset(cx + s * 0.30f, cy + s * 0.30f),
                    Offset(cx + s * 0.08f, cy), Offset(cx + s * 0.08f, cy - s * 0.32f)
                ), ink, 1.6f, 1.2f, seed, close = true
            )
            sketch(listOf(Offset(cx - s * 0.12f, cy - s * 0.34f), Offset(cx + s * 0.12f, cy - s * 0.34f)), ink, 1.5f, 0.9f, seed + 1)
            sketch(listOf(Offset(cx - s * 0.22f, cy + s * 0.12f), Offset(cx + s * 0.22f, cy + s * 0.12f)), accent, 1.5f, 0.9f, seed + 2)
            listOf(-0.10f to -0.04f, 0.04f to -0.14f, -0.02f to 0.04f).forEach { (ox, oy) ->
                sketchCircle(Offset(cx + s * ox, cy + s * oy), s * 0.05f, ink.copy(alpha = 0.7f), 1.2f, 0.7f, seed + 3, 10)
            }
        }
        // A clapperboard, hinged open.
        SketchMotif.CLAPPER -> {
            sketchRect(cx - s * 0.38f, cy - s * 0.02f, cx + s * 0.38f, cy + s * 0.30f, ink, 1.6f, 1.4f, seed)
            sketch(listOf(Offset(cx - s * 0.40f, cy - s * 0.06f), Offset(cx + s * 0.36f, cy - s * 0.26f)), ink, 1.5f, 1.2f, seed + 1)
            for (index in 0..3) {
                val x = cx - s * 0.34f + s * 0.20f * index
                sketch(listOf(Offset(x, cy - s * 0.10f), Offset(x + s * 0.10f, cy - s * 0.12f)), accent, 1.3f, 0.7f, seed + index + 2)
            }
        }
        // A bulb with its screw base and rays.
        SketchMotif.BULB -> {
            sketchCircle(Offset(cx, cy - s * 0.10f), s * 0.24f, ink, 1.6f, 1.1f, seed, 18)
            sketch(listOf(Offset(cx - s * 0.10f, cy + s * 0.12f), Offset(cx - s * 0.09f, cy + s * 0.28f)), ink, 1.5f, 0.9f, seed + 1)
            sketch(listOf(Offset(cx + s * 0.10f, cy + s * 0.12f), Offset(cx + s * 0.09f, cy + s * 0.28f)), ink, 1.5f, 0.9f, seed + 2)
            sketch(listOf(Offset(cx - s * 0.07f, cy + s * 0.30f), Offset(cx + s * 0.07f, cy + s * 0.30f)), ink, 1.4f, 0.8f, seed + 3)
            for (index in 0..5) {
                val angle = (-140f + index * 28f) * 0.0174533f
                sketch(
                    listOf(
                        Offset(cx + kotlin.math.cos(angle) * s * 0.34f, cy - s * 0.10f + kotlin.math.sin(angle) * s * 0.34f),
                        Offset(cx + kotlin.math.cos(angle) * s * 0.46f, cy - s * 0.10f + kotlin.math.sin(angle) * s * 0.46f)
                    ), accent.copy(alpha = 0.85f), 1.5f, 0.8f, seed + index + 4
                )
            }
        }
        // A climbing line, drawn twice over.
        SketchMotif.GRAPH -> {
            sketch(listOf(Offset(cx - s * 0.36f, cy - s * 0.32f), Offset(cx - s * 0.36f, cy + s * 0.32f), Offset(cx + s * 0.36f, cy + s * 0.32f)), ink, 1.6f, 1.2f, seed)
            sketch(
                listOf(
                    Offset(cx - s * 0.30f, cy + s * 0.22f), Offset(cx - s * 0.10f, cy + s * 0.06f),
                    Offset(cx + s * 0.06f, cy + s * 0.12f), Offset(cx + s * 0.30f, cy - s * 0.20f)
                ), accent, 1.8f, 1.2f, seed + 1
            )
            hatch(cx - s * 0.30f, cy - s * 0.18f, s * 0.52f, s * 0.44f, ink, 5, seed + 2)
        }
        // A film reel and a strip of frames.
        SketchMotif.REEL -> {
            sketchCircle(Offset(cx - s * 0.10f, cy), s * 0.30f, ink, 1.7f, 1.2f, seed, 20)
            listOf(-0.10f to -0.14f, 0.06f to -0.06f, -0.10f to 0.14f).forEach { (ox, oy) ->
                drawCircle(accent, radius = s * 0.05f, center = Offset(cx + s * ox, cy + s * oy))
            }
            sketchRect(cx + s * 0.20f, cy - s * 0.32f, cx + s * 0.42f, cy + s * 0.30f, ink, 1.4f, 1.2f, seed + 1)
            for (index in 0..2) {
                val y = cy - s * 0.24f + s * 0.20f * index
                sketchRect(cx + s * 0.22f, y, cx + s * 0.40f, y + s * 0.13f, ink.copy(alpha = 0.55f), 1.1f, 0.8f, seed + index + 2)
            }
        }
        // A fork, and a bowl with steam.
        SketchMotif.FORK -> {
            for (index in 0..2) {
                val x = cx - s * 0.34f + s * 0.10f * index
                sketch(listOf(Offset(x, cy - s * 0.32f), Offset(x, cy - s * 0.06f)), ink, 1.4f, 0.9f, seed + index)
            }
            sketch(listOf(Offset(cx - s * 0.36f, cy - s * 0.06f), Offset(cx - s * 0.22f, cy - s * 0.06f), Offset(cx - s * 0.28f, cy + s * 0.32f)), ink, 1.5f, 1.0f, seed + 4)
            sketch(listOf(Offset(cx + s * 0.06f, cy + s * 0.10f), Offset(cx + s * 0.42f, cy + s * 0.10f), Offset(cx + s * 0.28f, cy + s * 0.30f), Offset(cx + s * 0.20f, cy + s * 0.30f)), ink, 1.5f, 1.0f, seed + 5)
            for (index in 0..1) {
                val x = cx + s * (0.18f + index * 0.12f)
                sketch(listOf(Offset(x, cy - s * 0.24f), Offset(x + s * 0.04f, cy - s * 0.14f), Offset(x - s * 0.02f, cy - s * 0.04f)), accent.copy(alpha = 0.8f), 1.3f, 1.0f, seed + index + 6)
            }
        }
        // Strata: wavy bands with a hatch between them.
        SketchMotif.STRATA -> {
            for (index in 0..3) {
                val y = cy - s * 0.28f + s * 0.18f * index
                sketch(
                    listOf(
                        Offset(cx - s * 0.38f, y), Offset(cx - s * 0.12f, y - s * 0.05f),
                        Offset(cx + s * 0.14f, y + s * 0.04f), Offset(cx + s * 0.38f, y - s * 0.02f)
                    ), ink, 1.5f, 1.4f, seed + index
                )
            }
            hatch(cx - s * 0.30f, cy + s * 0.06f, s * 0.52f, s * 0.20f, accent, 5, seed + 5)
        }
        // A column and its capital.
        SketchMotif.COLUMN -> {
            sketchRect(cx - s * 0.26f, cy - s * 0.32f, cx + s * 0.26f, cy - s * 0.22f, ink, 1.5f, 1.3f, seed)
            sketch(listOf(Offset(cx - s * 0.16f, cy - s * 0.22f), Offset(cx - s * 0.16f, cy + s * 0.24f)), ink, 1.5f, 1.0f, seed + 1)
            sketch(listOf(Offset(cx + s * 0.16f, cy - s * 0.22f), Offset(cx + s * 0.16f, cy + s * 0.24f)), ink, 1.5f, 1.0f, seed + 2)
            sketch(listOf(Offset(cx, cy - s * 0.20f), Offset(cx, cy + s * 0.22f)), ink.copy(alpha = 0.5f), 1.1f, 0.8f, seed + 3)
            sketchRect(cx - s * 0.30f, cy + s * 0.24f, cx + s * 0.30f, cy + s * 0.34f, ink, 1.5f, 1.3f, seed + 4)
        }
        // A globe with a signal.
        SketchMotif.GLOBE -> {
            sketchCircle(Offset(cx - s * 0.04f, cy), s * 0.28f, ink, 1.6f, 1.1f, seed, 20)
            sketch(listOf(Offset(cx - s * 0.30f, cy - s * 0.08f), Offset(cx + s * 0.22f, cy - s * 0.08f)), ink.copy(alpha = 0.6f), 1.2f, 1.0f, seed + 1)
            sketch(listOf(Offset(cx - s * 0.26f, cy + s * 0.12f), Offset(cx + s * 0.18f, cy + s * 0.12f)), ink.copy(alpha = 0.6f), 1.2f, 1.0f, seed + 2)
            for (index in 0..2) {
                val r = s * (0.10f + index * 0.08f)
                sketch(
                    List(11) { step ->
                        val a = (-60f + step * 12f) * 0.0174533f
                        Offset(cx + s * 0.30f + kotlin.math.cos(a) * r, cy - s * 0.24f + kotlin.math.sin(a) * r)
                    }, accent, 1.3f, 0.9f, seed + index + 3
                )
            }
        }
        // Two speech bubbles.
        SketchMotif.BUBBLES -> {
            sketchRect(cx - s * 0.38f, cy - s * 0.32f, cx + s * 0.10f, cy - s * 0.02f, ink, 1.5f, 1.4f, seed)
            sketch(listOf(Offset(cx - s * 0.20f, cy - s * 0.02f), Offset(cx - s * 0.24f, cy + s * 0.12f), Offset(cx - s * 0.08f, cy - s * 0.02f)), ink, 1.4f, 1.0f, seed + 1)
            sketchRect(cx - s * 0.04f, cy + s * 0.06f, cx + s * 0.40f, cy + s * 0.32f, accent, 1.5f, 1.4f, seed + 2)
            for (index in 0..2) {
                val x = cx - s * 0.30f + s * 0.12f * index
                sketch(listOf(Offset(x, cy - s * 0.24f), Offset(x + s * 0.08f, cy - s * 0.24f)), ink.copy(alpha = 0.5f), 1.2f, 0.7f, seed + index + 3)
            }
        }
        // A panel, with the lines of a page and a sparkle.
        SketchMotif.PANEL -> {
            sketchRect(cx - s * 0.34f, cy - s * 0.34f, cx + s * 0.34f, cy + s * 0.30f, ink, 1.6f, 1.6f, seed)
            for (index in 0..2) {
                val y = cy - s * 0.18f + s * 0.14f * index
                sketch(listOf(Offset(cx - s * 0.22f, y), Offset(cx + s * 0.20f - s * 0.12f * index, y)), ink.copy(alpha = 0.5f), 1.2f, 0.9f, seed + index + 1)
            }
            sketch(listOf(Offset(cx + s * 0.18f, cy - s * 0.26f), Offset(cx + s * 0.24f, cy - s * 0.10f)), accent, 1.5f, 0.8f, seed + 5)
            sketch(listOf(Offset(cx + s * 0.06f, cy - s * 0.18f), Offset(cx + s * 0.36f, cy - s * 0.18f)), accent, 1.5f, 0.8f, seed + 6)
        }
        // A tall panel with a swoosh and a spark.
        SketchMotif.PANEL_ARC -> {
            sketchRect(cx - s * 0.30f, cy - s * 0.32f, cx + s * 0.30f, cy + s * 0.32f, ink, 1.6f, 1.5f, seed)
            sketch(
                listOf(
                    Offset(cx - s * 0.20f, cy + s * 0.20f), Offset(cx, cy - s * 0.02f),
                    Offset(cx + s * 0.20f, cy - s * 0.24f)
                ), accent, 2.2f, 1.6f, seed + 1
            )
            for (index in 0..1) {
                sketch(listOf(Offset(cx - s * 0.20f + s * 0.34f * index, cy - s * 0.18f), Offset(cx - s * 0.10f + s * 0.34f * index, cy - s * 0.06f)), ink.copy(alpha = 0.55f), 1.2f, 0.8f, seed + index + 2)
            }
        }
        // A pair of compasses over a ruled line.
        SketchMotif.COMPASSES -> {
            sketch(listOf(Offset(cx, cy - s * 0.32f), Offset(cx - s * 0.22f, cy + s * 0.28f)), ink, 1.6f, 1.1f, seed)
            sketch(listOf(Offset(cx, cy - s * 0.32f), Offset(cx + s * 0.22f, cy + s * 0.28f)), ink, 1.6f, 1.1f, seed + 1)
            sketchCircle(Offset(cx, cy - s * 0.34f), s * 0.05f, accent, 1.5f, 0.7f, seed + 2, 12)
            sketch(listOf(Offset(cx - s * 0.34f, cy + s * 0.34f), Offset(cx + s * 0.34f, cy + s * 0.34f)), ink.copy(alpha = 0.55f), 1.3f, 0.9f, seed + 3)
        }
        // A bolt, with a spark either side.
        SketchMotif.BOLT -> {
            sketch(
                listOf(
                    Offset(cx + s * 0.10f, cy - s * 0.36f), Offset(cx - s * 0.16f, cy - s * 0.02f),
                    Offset(cx + s * 0.04f, cy - s * 0.02f), Offset(cx - s * 0.12f, cy + s * 0.36f),
                    Offset(cx + s * 0.20f, cy - s * 0.08f), Offset(cx, cy - s * 0.08f),
                    Offset(cx + s * 0.10f, cy - s * 0.36f)
                ), accent, 1.8f, 1.4f, seed, close = true
            )
            sketch(listOf(Offset(cx - s * 0.34f, cy - s * 0.20f), Offset(cx - s * 0.22f, cy - s * 0.12f)), ink.copy(alpha = 0.6f), 1.3f, 0.9f, seed + 1)
            sketch(listOf(Offset(cx + s * 0.34f, cy + s * 0.16f), Offset(cx + s * 0.24f, cy + s * 0.10f)), ink.copy(alpha = 0.6f), 1.3f, 0.9f, seed + 2)
        }
        // A palette with its wells.
        SketchMotif.PALETTE -> {
            sketch(
                listOf(
                    Offset(cx - s * 0.34f, cy - s * 0.06f), Offset(cx - s * 0.20f, cy - s * 0.28f),
                    Offset(cx + s * 0.14f, cy - s * 0.30f), Offset(cx + s * 0.34f, cy - s * 0.06f),
                    Offset(cx + s * 0.20f, cy + s * 0.20f), Offset(cx - s * 0.14f, cy + s * 0.26f),
                    Offset(cx - s * 0.34f, cy - s * 0.06f)
                ), ink, 1.6f, 1.5f, seed, close = true
            )
            listOf(-0.18f to -0.12f, 0.02f to -0.18f, 0.20f to -0.06f).forEach { (ox, oy) ->
                drawCircle(accent, radius = s * 0.05f, center = Offset(cx + s * ox, cy + s * oy))
            }
            sketch(listOf(Offset(cx + s * 0.24f, cy + s * 0.20f), Offset(cx + s * 0.42f, cy + s * 0.06f)), ink, 1.5f, 1.0f, seed + 4)
        }
        // A sprout in a pot.
        SketchMotif.SPROUT -> {
            sketch(
                listOf(Offset(cx - s * 0.26f, cy + s * 0.06f), Offset(cx + s * 0.26f, cy + s * 0.06f),
                    Offset(cx + s * 0.18f, cy + s * 0.34f), Offset(cx - s * 0.18f, cy + s * 0.34f)),
                ink, 1.6f, 1.3f, seed, close = true
            )
            sketch(listOf(Offset(cx, cy + s * 0.06f), Offset(cx, cy - s * 0.24f)), ink, 1.5f, 1.0f, seed + 1)
            sketch(
                listOf(Offset(cx, cy - s * 0.10f), Offset(cx - s * 0.26f, cy - s * 0.22f), Offset(cx - s * 0.06f, cy - s * 0.30f)),
                accent, 1.5f, 1.2f, seed + 2
            )
            sketch(
                listOf(Offset(cx, cy - s * 0.18f), Offset(cx + s * 0.26f, cy - s * 0.30f), Offset(cx + s * 0.06f, cy - s * 0.36f)),
                accent, 1.5f, 1.2f, seed + 3
            )
        }
        // A head in profile, with a spiral for the thinking.
        SketchMotif.HEAD -> {
            sketch(
                listOf(
                    Offset(cx - s * 0.30f, cy + s * 0.04f), Offset(cx - s * 0.20f, cy - s * 0.26f),
                    Offset(cx + s * 0.10f, cy - s * 0.30f), Offset(cx + s * 0.26f, cy - s * 0.04f),
                    Offset(cx + s * 0.18f, cy + s * 0.16f), Offset(cx + s * 0.30f, cy + s * 0.30f),
                    Offset(cx - s * 0.06f, cy + s * 0.30f), Offset(cx - s * 0.30f, cy + s * 0.04f)
                ), ink, 1.6f, 1.6f, seed, close = true
            )
            sketch(
                List(22) { step ->
                    val a = step / 22f * 12.5f
                    val r = s * 0.03f + a * s * 0.012f
                    Offset(cx - s * 0.02f + kotlin.math.cos(a) * r, cy - s * 0.04f + kotlin.math.sin(a) * r)
                }, accent, 1.4f, 0.7f, seed + 1
            )
        }
        // Two quotation marks, drawn like commas.
        SketchMotif.QUOTES -> {
            for (index in 0..1) {
                val x = cx - s * 0.22f + s * 0.34f * index
                sketch(
                    listOf(
                        Offset(x + s * 0.10f, cy - s * 0.20f), Offset(x - s * 0.06f, cy - s * 0.06f),
                        Offset(x - s * 0.02f, cy + s * 0.10f), Offset(x + s * 0.12f, cy + s * 0.02f),
                        Offset(x + s * 0.10f, cy - s * 0.20f)
                    ), ink, 1.7f, 1.2f, seed + index, close = true
                )
            }
            sketch(listOf(Offset(cx - s * 0.30f, cy + s * 0.28f), Offset(cx + s * 0.30f, cy + s * 0.26f)), accent, 1.6f, 1.1f, seed + 3)
        }
        // An atom, its two orbits and the nucleus.
        SketchMotif.ATOM -> {
            for (index in 0..1) {
                val tilt = if (index == 0) 0f else s * 0.10f
                sketch(
                    List(24) { step ->
                        val a = step / 24f * 6.2831855f
                        Offset(cx + kotlin.math.cos(a) * s * 0.34f, cy + kotlin.math.sin(a) * s * 0.16f + tilt * kotlin.math.sin(a * 2f))
                    }, ink, 1.5f, 1.0f, seed + index, close = true
                )
            }
            drawCircle(accent, radius = s * 0.06f, center = Offset(cx, cy))
            for (index in 0..2) {
                val a = index * 2.1f
                drawCircle(ink, radius = s * 0.028f, center = Offset(cx + kotlin.math.cos(a) * s * 0.24f, cy + kotlin.math.sin(a) * s * 0.11f))
            }
        }
        // A record, a label and the arm.
        SketchMotif.VINYL -> {
            sketchCircle(Offset(cx - s * 0.06f, cy), s * 0.32f, ink, 1.7f, 1.2f, seed, 22)
            sketchCircle(Offset(cx - s * 0.06f, cy), s * 0.24f, ink.copy(alpha = 0.4f), 1.1f, 0.8f, seed + 1, 20)
            drawCircle(accent, radius = s * 0.08f, center = Offset(cx - s * 0.06f, cy))
            drawCircle(ink, radius = s * 0.02f, center = Offset(cx - s * 0.06f, cy))
            sketch(listOf(Offset(cx + s * 0.34f, cy - s * 0.32f), Offset(cx + s * 0.18f, cy - s * 0.10f), Offset(cx + s * 0.04f, cy - s * 0.14f)), ink, 1.5f, 1.0f, seed + 2)
        }
        // A staff, with two notes on it.
        SketchMotif.NOTES -> {
            for (index in 0..3) {
                val y = cy - s * 0.18f + s * 0.11f * index
                sketch(listOf(Offset(cx - s * 0.38f, y), Offset(cx + s * 0.38f, y)), ink.copy(alpha = 0.55f), 1.2f, 0.9f, seed + index)
            }
            listOf(-0.14f to 0.06f, 0.16f to -0.06f).forEachIndexed { index, (ox, oy) ->
                val x = cx + s * ox
                val y = cy + s * oy
                drawCircle(accent, radius = s * 0.06f, center = Offset(x, y))
                sketch(listOf(Offset(x + s * 0.055f, y), Offset(x + s * 0.055f, y - s * 0.24f)), ink, 1.5f, 0.8f, seed + index + 4)
            }
        }
        // A set with its antennae.
        SketchMotif.TELEVISION -> {
            sketchRect(cx - s * 0.36f, cy - s * 0.26f, cx + s * 0.36f, cy + s * 0.20f, ink, 1.6f, 1.5f, seed)
            sketchRect(cx - s * 0.28f, cy - s * 0.18f, cx + s * 0.28f, cy + s * 0.12f, accent.copy(alpha = 0.6f), 1.2f, 1.0f, seed + 1)
            sketch(listOf(Offset(cx - s * 0.12f, cy - s * 0.26f), Offset(cx - s * 0.30f, cy - s * 0.44f)), ink, 1.4f, 1.0f, seed + 2)
            sketch(listOf(Offset(cx + s * 0.12f, cy - s * 0.26f), Offset(cx + s * 0.30f, cy - s * 0.44f)), ink, 1.4f, 1.0f, seed + 3)
            sketch(listOf(Offset(cx - s * 0.10f, cy + s * 0.20f), Offset(cx - s * 0.06f, cy + s * 0.32f), Offset(cx + s * 0.06f, cy + s * 0.32f), Offset(cx + s * 0.10f, cy + s * 0.20f)), ink, 1.4f, 1.0f, seed + 4)
        }
        // A gamepad, arrows on the left, buttons on the right.
        SketchMotif.GAMEPAD -> {
            sketch(
                listOf(
                    Offset(cx - s * 0.34f, cy - s * 0.06f), Offset(cx - s * 0.20f, cy - s * 0.22f),
                    Offset(cx + s * 0.20f, cy - s * 0.22f), Offset(cx + s * 0.34f, cy - s * 0.06f),
                    Offset(cx + s * 0.26f, cy + s * 0.22f), Offset(cx - s * 0.26f, cy + s * 0.22f),
                    Offset(cx - s * 0.34f, cy - s * 0.06f)
                ), ink, 1.7f, 1.4f, seed, close = true
            )
            sketch(listOf(Offset(cx - s * 0.22f, cy), Offset(cx - s * 0.08f, cy)), ink, 1.4f, 0.8f, seed + 1)
            sketch(listOf(Offset(cx - s * 0.15f, cy - s * 0.07f), Offset(cx - s * 0.15f, cy + s * 0.07f)), ink, 1.4f, 0.8f, seed + 2)
            drawCircle(accent, radius = s * 0.045f, center = Offset(cx + s * 0.16f, cy - s * 0.06f))
            drawCircle(accent, radius = s * 0.045f, center = Offset(cx + s * 0.24f, cy + s * 0.04f))
        }
        // A cup, its handles and the star on the base.
        SketchMotif.TROPHY -> {
            sketch(
                listOf(
                    Offset(cx - s * 0.22f, cy - s * 0.32f), Offset(cx - s * 0.16f, cy + s * 0.06f),
                    Offset(cx + s * 0.16f, cy + s * 0.06f), Offset(cx + s * 0.22f, cy - s * 0.32f)
                ), ink, 1.7f, 1.2f, seed
            )
            sketch(listOf(Offset(cx - s * 0.22f, cy - s * 0.30f), Offset(cx + s * 0.22f, cy - s * 0.30f)), ink, 1.6f, 0.9f, seed + 1)
            sketch(listOf(Offset(cx - s * 0.22f, cy - s * 0.24f), Offset(cx - s * 0.36f, cy - s * 0.16f), Offset(cx - s * 0.20f, cy - s * 0.04f)), ink, 1.4f, 1.0f, seed + 2)
            sketch(listOf(Offset(cx + s * 0.22f, cy - s * 0.24f), Offset(cx + s * 0.36f, cy - s * 0.16f), Offset(cx + s * 0.20f, cy - s * 0.04f)), ink, 1.4f, 1.0f, seed + 3)
            sketch(listOf(Offset(cx, cy + s * 0.06f), Offset(cx, cy + s * 0.20f)), ink, 1.5f, 0.9f, seed + 4)
            sketchRect(cx - s * 0.24f, cy + s * 0.20f, cx + s * 0.24f, cy + s * 0.32f, ink, 1.5f, 1.2f, seed + 5)
            sketch(listOf(Offset(cx, cy + s * 0.12f), Offset(cx + s * 0.06f, cy + s * 0.04f), Offset(cx - s * 0.06f, cy + s * 0.04f)), accent, 1.4f, 0.8f, seed + 6, close = true)
        }
        // A chip and its pins.
        SketchMotif.CHIP -> {
            sketchRect(cx - s * 0.24f, cy - s * 0.24f, cx + s * 0.24f, cy + s * 0.24f, ink, 1.7f, 1.4f, seed)
            sketchRect(cx - s * 0.10f, cy - s * 0.10f, cx + s * 0.10f, cy + s * 0.10f, accent, 1.4f, 1.0f, seed + 1)
            for (index in 0..1) {
                val offset = s * (index * 0.18f - 0.09f)
                sketch(listOf(Offset(cx - s * 0.36f, cy + offset), Offset(cx - s * 0.24f, cy + offset)), ink, 1.3f, 0.7f, seed + index + 2)
                sketch(listOf(Offset(cx + s * 0.24f, cy + offset), Offset(cx + s * 0.36f, cy + offset)), ink, 1.3f, 0.7f, seed + index + 4)
                sketch(listOf(Offset(cx + offset, cy - s * 0.36f), Offset(cx + offset, cy - s * 0.24f)), ink, 1.3f, 0.7f, seed + index + 6)
                sketch(listOf(Offset(cx + offset, cy + s * 0.24f), Offset(cx + offset, cy + s * 0.36f)), ink, 1.3f, 0.7f, seed + index + 8)
            }
        }
        // A ringed planet and its stars.
        SketchMotif.PLANET -> {
            sketchCircle(Offset(cx, cy), s * 0.22f, ink, 1.6f, 1.1f, seed, 18)
            sketch(
                List(26) { step ->
                    val a = step / 26f * 6.2831855f
                    Offset(cx + kotlin.math.cos(a) * s * 0.42f, cy + kotlin.math.sin(a) * s * 0.13f)
                }, accent, 1.5f, 1.0f, seed + 1, close = true
            )
            listOf(-0.34f to -0.30f, 0.30f to -0.34f, 0.36f to 0.24f).forEachIndexed { index, (ox, oy) ->
                val x = cx + s * ox
                val y = cy + s * oy
                sketch(listOf(Offset(x - s * 0.05f, y), Offset(x + s * 0.05f, y)), ink, 1.2f, 0.6f, seed + index + 2)
                sketch(listOf(Offset(x, y - s * 0.05f), Offset(x, y + s * 0.05f)), ink, 1.2f, 0.6f, seed + index + 5)
            }
        }
        // A cross and a heartbeat line.
        SketchMotif.PULSE -> {
            sketchRect(cx - s * 0.26f, cy - s * 0.26f, cx + s * 0.26f, cy + s * 0.26f, ink, 1.6f, 1.4f, seed)
            sketch(listOf(Offset(cx - s * 0.06f, cy - s * 0.16f), Offset(cx + s * 0.06f, cy - s * 0.16f)), accent, 2.4f, 0.8f, seed + 1)
            sketch(listOf(Offset(cx, cy - s * 0.22f), Offset(cx, cy + s * 0.22f)), accent, 2.4f, 0.8f, seed + 2)
            sketch(
                listOf(
                    Offset(cx - s * 0.38f, cy + s * 0.34f), Offset(cx - s * 0.16f, cy + s * 0.34f),
                    Offset(cx - s * 0.08f, cy + s * 0.22f), Offset(cx, cy + s * 0.42f),
                    Offset(cx + s * 0.10f, cy + s * 0.34f), Offset(cx + s * 0.38f, cy + s * 0.34f)
                ), ink, 1.5f, 1.0f, seed + 3
            )
        }
        // A gear, with a ruler beside it.
        SketchMotif.GEAR -> {
            sketchCircle(Offset(cx - s * 0.06f, cy), s * 0.22f, ink, 1.6f, 1.0f, seed, 18)
            sketchCircle(Offset(cx - s * 0.06f, cy), s * 0.08f, accent, 1.4f, 0.7f, seed + 1, 12)
            for (index in 0 until 8) {
                val a = index / 8f * 6.2831855f
                sketch(
                    listOf(
                        Offset(cx - s * 0.06f + kotlin.math.cos(a) * s * 0.22f, cy + kotlin.math.sin(a) * s * 0.22f),
                        Offset(cx - s * 0.06f + kotlin.math.cos(a) * s * 0.32f, cy + kotlin.math.sin(a) * s * 0.32f)
                    ), ink, 1.5f, 0.8f, seed + index + 2
                )
            }
            sketchRect(cx + s * 0.26f, cy - s * 0.30f, cx + s * 0.40f, cy + s * 0.30f, ink, 1.3f, 1.1f, seed + 12)
        }
        // Two waves, and a shell at the foot.
        SketchMotif.WAVE -> {
            for (index in 0..1) {
                val y = cy - s * 0.20f + s * 0.22f * index
                sketch(
                    List(14) { step ->
                        val t = step / 13f
                        Offset(cx - s * 0.40f + s * 0.80f * t, y + kotlin.math.sin(t * 6.2831855f + index) * s * 0.06f)
                    }, ink, 1.6f, 1.0f, seed + index
                )
            }
            sketch(
                List(13) { step ->
                    val a = 3.14159f + step / 12f * 3.14159f
                    Offset(cx + s * 0.14f + kotlin.math.cos(a) * s * 0.14f, cy + s * 0.24f + kotlin.math.sin(a) * s * 0.10f)
                }, accent, 1.5f, 0.9f, seed + 3
            )
        }
        // The fallback: a loopy scribble and a star.
        SketchMotif.SQUIGGLE -> {
            sketch(
                List(28) { step ->
                    val a = step / 28f * 9.424778f
                    val r = s * 0.06f + step / 28f * s * 0.28f
                    Offset(cx - s * 0.10f + kotlin.math.cos(a) * r, cy + kotlin.math.sin(a) * r * 0.8f)
                }, ink, 1.6f, 1.0f, seed
            )
            sketch(listOf(Offset(cx + s * 0.26f, cy - s * 0.28f), Offset(cx + s * 0.32f, cy - s * 0.12f)), accent, 1.5f, 0.7f, seed + 1)
            sketch(listOf(Offset(cx + s * 0.14f, cy - s * 0.20f), Offset(cx + s * 0.44f, cy - s * 0.20f)), accent, 1.5f, 0.7f, seed + 2)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// The pages
// ═══════════════════════════════════════════════════════════════════════

/**
 * One category's paper: the sheet, the pen, the marker, the doodle, the layout.
 *
 * [layout] is picked per page rather than fixed: a row of hand-written cards all
 * set the same way stops reading as a notebook and starts reading as a
 * template, and the signature layouts are the app's own four flows.
 */
private data class SketchPage(
    val paper: Color,
    val ink: Color,
    val accent: Color,
    val layout: SignatureLayout,
    val motif: SketchMotif
)

/** The page a lane wears when nothing else can be said about it. */
private val SKETCH_FALLBACK = SketchPage(
    Color(0xFFF7F3E8), Color(0xFF2A2620), Color(0xFF8A6E45),
    SignatureLayout.CENTERED, SketchMotif.SQUIGGLE
)

/**
 * THE SECOND KEY: THE FAMILY.
 *
 * The page is chosen by category NAME, and a name is exactly the thing that
 * changes when a lane is renamed or a new one is added — so when the name means
 * nothing here yet, the FAMILY still does, and a lane lands on its family's
 * page (a new science lane gets the atom, a new music lane gets the staff)
 * instead of on the grey fallback. This is also why [sketchbookDesign] takes the
 * family at all.
 */
private fun sketchFamilyPage(family: CategoryFamily): SketchPage = when (family) {
    CategoryFamily.MUSIC -> SketchPage(Color(0xFFF9F2EA), Color(0xFF2A2420), Color(0xFFB5556A), SignatureLayout.CENTERED, SketchMotif.NOTES)
    CategoryFamily.MOVIES -> SketchPage(Color(0xFFF4F0E8), Color(0xFF26221C), Color(0xFF9A4B3C), SignatureLayout.POSTER, SketchMotif.REEL)
    CategoryFamily.BOOKS -> SketchPage(Color(0xFFF9F1E1), Color(0xFF2E261C), Color(0xFFB07C36), SignatureLayout.STANDARD, SketchMotif.BOOKSTACK)
    CategoryFamily.VISUAL_ART -> SketchPage(Color(0xFFF6F1E8), Color(0xFF2B241C), Color(0xFFBE6A4C), SignatureLayout.STANDARD, SketchMotif.PALETTE)
    CategoryFamily.SCIENCE -> SketchPage(Color(0xFFF3F4F6), Color(0xFF242A33), Color(0xFF3F7CA6), SignatureLayout.STANDARD, SketchMotif.ATOM)
    CategoryFamily.ANIME_COMICS -> SketchPage(Color(0xFFF8F4F0), Color(0xFF24211E), Color(0xFF4A4A55), SignatureLayout.CENTERED, SketchMotif.PANEL)
    CategoryFamily.GAMES -> SketchPage(Color(0xFFF2F5F2), Color(0xFF1F2A2A), Color(0xFF3F8B76), SignatureLayout.CENTERED, SketchMotif.GAMEPAD)
    CategoryFamily.MYTHOLOGY -> SketchPage(Color(0xFFFAF2E3), Color(0xFF322415), Color(0xFFC2802F), SignatureLayout.POSTER, SketchMotif.BOLT)
    CategoryFamily.SPORTS -> SketchPage(Color(0xFFF6F3EB), Color(0xFF26291F), Color(0xFFB4622F), SignatureLayout.POSTER, SketchMotif.TROPHY)
    CategoryFamily.FOOD -> SketchPage(Color(0xFFFBF2E5), Color(0xFF33261C), Color(0xFFCB6A3A), SignatureLayout.CENTERED, SketchMotif.FORK)
    CategoryFamily.INTERNET -> SketchPage(Color(0xFFF1F3F5), Color(0xFF222A33), Color(0xFF4C7FB0), SignatureLayout.STANDARD, SketchMotif.GLOBE)
    CategoryFamily.WILDCARD -> SKETCH_FALLBACK
}

/** Which page a lane wears — keyed on the category NAME, as the app keys it. */
private fun sketchPageFor(category: String): SketchPage? = when (category) {
    "ARTISTS" -> SketchPage(Color(0xFFF8F1E2), Color(0xFF2B2118), Color(0xFFC0873A), SignatureLayout.POSTER, SketchMotif.MIC)
    "WILDCARD" -> SketchPage(Color(0xFFF6F3EC), Color(0xFF2A2430), Color(0xFF7C6BC4), SignatureLayout.CENTERED, SketchMotif.DICE)
    "ANIMALS" -> SketchPage(Color(0xFFF3F3E6), Color(0xFF2A3128), Color(0xFF6E8A5A), SignatureLayout.CENTERED, SketchMotif.PAW)
    "ANIMATED FILMS", "ANIMATED MOVIES" -> SketchPage(Color(0xFFF8F0F4), Color(0xFF33272E), Color(0xFFB2689C), SignatureLayout.CENTERED, SketchMotif.BOUNCE)
    "ANIME" -> SketchPage(Color(0xFFF9F1F2), Color(0xFF2E2126), Color(0xFFD2455A), SignatureLayout.STANDARD, SketchMotif.SPEED)
    "ARTWORKS" -> SketchPage(Color(0xFFF5F3EC), Color(0xFF2A2721), Color(0xFF9A8B62), SignatureLayout.STANDARD, SketchMotif.EASEL)
    "AUTHORS" -> SketchPage(Color(0xFFF8F2E7), Color(0xFF2C2419), Color(0xFF8B5A3C), SignatureLayout.STANDARD, SketchMotif.QUILL)
    "BIOLOGY" -> SketchPage(Color(0xFFF2F5EB), Color(0xFF26301F), Color(0xFF5E8B4E), SignatureLayout.STANDARD, SketchMotif.LEAF)
    "BOOKS" -> SketchPage(Color(0xFFF9F1E1), Color(0xFF2E261C), Color(0xFFB07C36), SignatureLayout.CENTERED, SketchMotif.BOOKSTACK)
    "CHEMISTRY" -> SketchPage(Color(0xFFF0F4F5), Color(0xFF22303A), Color(0xFF3E8FA8), SignatureLayout.STANDARD, SketchMotif.FLASK)
    "DIRECTORS" -> SketchPage(Color(0xFFF4F1E9), Color(0xFF26241F), Color(0xFF7A6A55), SignatureLayout.POSTER, SketchMotif.CLAPPER)
    "DISCOVERIES" -> SketchPage(Color(0xFFFCF4E1), Color(0xFF33291A), Color(0xFFD8A032), SignatureLayout.CENTERED, SketchMotif.BULB)
    "ECONOMICS" -> SketchPage(Color(0xFFF3F4EB), Color(0xFF272B22), Color(0xFF6F8B3C), SignatureLayout.STANDARD, SketchMotif.GRAPH)
    "FILMS" -> SketchPage(Color(0xFFF4F0E8), Color(0xFF26221C), Color(0xFF9A4B3C), SignatureLayout.POSTER, SketchMotif.REEL)
    "FOOD" -> SketchPage(Color(0xFFFBF2E5), Color(0xFF33261C), Color(0xFFCB6A3A), SignatureLayout.CENTERED, SketchMotif.FORK)
    "GEOLOGY" -> SketchPage(Color(0xFFF4F1E5), Color(0xFF2E2A20), Color(0xFF9A7B45), SignatureLayout.STANDARD, SketchMotif.STRATA)
    "HISTORY" -> SketchPage(Color(0xFFF7F1E2), Color(0xFF2B2618), Color(0xFF8C7642), SignatureLayout.STANDARD, SketchMotif.COLUMN)
    "INTERNET" -> SketchPage(Color(0xFFF1F3F5), Color(0xFF222A33), Color(0xFF4C7FB0), SignatureLayout.STANDARD, SketchMotif.GLOBE)
    "LANGUAGE" -> SketchPage(Color(0xFFF8F3E9), Color(0xFF2C2620), Color(0xFFA2564E), SignatureLayout.CENTERED, SketchMotif.BUBBLES)
    "MANGA" -> SketchPage(Color(0xFFF8F4F0), Color(0xFF24211E), Color(0xFF4A4A55), SignatureLayout.STANDARD, SketchMotif.PANEL)
    "MANHWA" -> SketchPage(Color(0xFFF8F2F4), Color(0xFF2A2228), Color(0xFF8E5A96), SignatureLayout.CENTERED, SketchMotif.PANEL_ARC)
    "MATHEMATICS" -> SketchPage(Color(0xFFF2F4F1), Color(0xFF232A26), Color(0xFF4E7A6A), SignatureLayout.CENTERED, SketchMotif.COMPASSES)
    "MYTHOLOGY" -> SketchPage(Color(0xFFFAF2E3), Color(0xFF322415), Color(0xFFC2802F), SignatureLayout.POSTER, SketchMotif.BOLT)
    "PAINTERS" -> SketchPage(Color(0xFFF9F3E9), Color(0xFF2B241C), Color(0xFFBE6A4C), SignatureLayout.STANDARD, SketchMotif.PALETTE)
    "PLANTS" -> SketchPage(Color(0xFFF1F6EB), Color(0xFF25301E), Color(0xFF4F8A47), SignatureLayout.CENTERED, SketchMotif.SPROUT)
    "PSYCHOLOGY" -> SketchPage(Color(0xFFF7F1F3), Color(0xFF2A222A), Color(0xFF8B5A82), SignatureLayout.STANDARD, SketchMotif.HEAD)
    "QUOTES" -> SketchPage(Color(0xFFF9F3E5), Color(0xFF2C271C), Color(0xFF9E824A), SignatureLayout.CENTERED, SketchMotif.QUOTES)
    "SCIENTISTS" -> SketchPage(Color(0xFFF3F4F6), Color(0xFF242A33), Color(0xFF3F7CA6), SignatureLayout.STANDARD, SketchMotif.ATOM)
    "ALBUMS" -> SketchPage(Color(0xFFF6F0E9), Color(0xFF262020), Color(0xFF7B5A8E), SignatureLayout.STANDARD, SketchMotif.VINYL)
    "SONGS" -> SketchPage(Color(0xFFF9F2EA), Color(0xFF2A2420), Color(0xFFB5556A), SignatureLayout.CENTERED, SketchMotif.NOTES)
    "SERIES" -> SketchPage(Color(0xFFF5F2EE), Color(0xFF262420), Color(0xFF4F6E8A), SignatureLayout.STANDARD, SketchMotif.TELEVISION)
    "GAMES" -> SketchPage(Color(0xFFF2F5F2), Color(0xFF1F2A2A), Color(0xFF3F8B76), SignatureLayout.CENTERED, SketchMotif.GAMEPAD)
    "SPORTS" -> SketchPage(Color(0xFFF6F3EB), Color(0xFF26291F), Color(0xFFB4622F), SignatureLayout.POSTER, SketchMotif.TROPHY)
    "TECHNOLOGIES" -> SketchPage(Color(0xFFF2F4F6), Color(0xFF222933), Color(0xFF4A79B8), SignatureLayout.STANDARD, SketchMotif.CHIP)
    "ASTRONOMY" -> SketchPage(Color(0xFFF3F1F7), Color(0xFF272438), Color(0xFF6A5BA8), SignatureLayout.CENTERED, SketchMotif.PLANET)
    "MEDICINE" -> SketchPage(Color(0xFFF7F3F3), Color(0xFF2A2426), Color(0xFFB34A50), SignatureLayout.STANDARD, SketchMotif.PULSE)
    "ENGINEERING" -> SketchPage(Color(0xFFF5F2EB), Color(0xFF282521), Color(0xFF8A6B34), SignatureLayout.STANDARD, SketchMotif.GEAR)
    "OCEANS" -> SketchPage(Color(0xFFF0F5F6), Color(0xFF1F2C31), Color(0xFF3C7F94), SignatureLayout.STANDARD, SketchMotif.WAVE)
    else -> null
}

/**
 * The page, as the card's own design record.
 *
 * Every lane gets the SAME furniture — paper, pen outline, hatching, a strip of
 * tape, one doodle, handwriting for the title — and differs in what the page is
 * made of: its paper tone, its pen, its marker, its doodle, and where the words
 * sit. That is what makes twenty-one cards look like twenty-one pages of one
 * notebook rather than twenty-one unrelated posters.
 */
internal fun sketchbookDesign(categoryName: String, family: CategoryFamily): SignatureDesign {
    val page = sketchPageFor(categoryName.uppercase().trim()) ?: sketchFamilyPage(family)
    // A doodle is not a logo: it sits where a sketcher would have put it, so the
    // corner it is drawn in is the one the LAYOUT leaves free. POSTER and
    // STANDARD start their words at the top, so the drawing goes low; CENTERED
    // and BOTTOM keep their middle clear, so it goes high and to the right; SIDE
    // holds the left column, so it goes bottom-right.
    return SignatureDesign(
        bg = page.paper,
        // A page has a corner, not a pill.
        cornerRadius = 10f,
        drawBackground = { w, h ->
            sketchPaper(page, w, h)
            val motifSize = minOf(w, h) * 0.30f
            val motifCentre = when (page.layout) {
                SignatureLayout.POSTER, SignatureLayout.STANDARD ->
                    Offset(w * 0.74f, h * 0.78f)
                SignatureLayout.SIDE -> Offset(w * 0.76f, h * 0.80f)
                SignatureLayout.OVERLAY -> Offset(w * 0.78f, h * 0.24f)
                SignatureLayout.CENTERED, SignatureLayout.BOTTOM ->
                    Offset(w * 0.76f, h * 0.20f)
            }
            drawMotif(page.motif, motifCentre, motifSize, page.ink.copy(alpha = 0.72f), page.accent, seed = 31)
            // The shading: a patch of pencil under the drawing, the way a
            // sketcher drops the shape onto the page instead of leaving it
            // floating.
            hatch(
                motifCentre.x - motifSize * 0.52f, motifCentre.y + motifSize * 0.36f,
                motifSize * 1.04f, motifSize * 0.22f, page.ink, lines = 6, seed = 12
            )
            // Washi tape holding the top-left corner down — one strip, crossed
            // by a second, because a single strip reads as a sticker.
            tape(Offset(w * 0.075f, h * 0.10f), w * 0.20f, h * 0.055f, page.accent, seed = 41)
            tape(Offset(w * 0.13f, h * 0.055f), w * 0.16f, h * 0.045f, page.accent, seed = 47)
            // Two marks the hand left behind: a small star near the frame and an
            // arrow pointing at the drawing. Both are faint — they are the
            // margin, not the message. (Drawn inline rather than from the motif
            // table: this is not a category's doodle, so no lane can share it.)
            val star = Offset(w * 0.86f, h * 0.90f)
            val starR = minOf(w, h) * 0.045f
            for (index in 0 until 3) {
                val angle = index / 3f * 3.14159f
                sketch(
                    listOf(
                        Offset(star.x - kotlin.math.cos(angle) * starR, star.y - kotlin.math.sin(angle) * starR),
                        Offset(star.x + kotlin.math.cos(angle) * starR, star.y + kotlin.math.sin(angle) * starR)
                    ), page.ink.copy(alpha = 0.32f), 1.3f, 0.8f, seed = 57 + index
                )
            }
            sketchArrow(
                Offset(w * 0.58f, motifCentre.y - motifSize * 0.62f),
                Offset(motifCentre.x - motifSize * 0.42f, motifCentre.y - motifSize * 0.30f),
                page.accent, seed = 61
            )
        },
        padding = PaddingValues(22.dp),
        badgeColor = page.accent,
        badgeInk = page.paper,
        badgeRadius = 10.dp,
        badgeHPadding = 10.dp,
        badgeVPadding = 5.dp,
        badgeIconSize = 12.dp,
        badgeFontSize = 9.sp,
        badgeLetterSpacing = 1.4.sp,
        titleTopSpacer = 16.dp,
        // The hand writes the title and the credit; a fact stays legible.
        titleFont = PatrickHandFontFamily,
        titleSize = 36.sp,
        titleLineHeight = 38.sp,
        titleColor = page.ink,
        metaSpacer = 7.dp,
        metaSeparator = " · ",
        metaSize = 12.sp,
        metaColor = page.accent,
        bodySize = 11f,
        bodyLineHeight = 1.55f,
        bodyColor = page.ink.copy(alpha = 0.86f),
        footerSpacer = 8.dp,
        footerFont = PatrickHandFontFamily,
        footerColor = page.ink.copy(alpha = 0.62f),
        layout = page.layout
    )
}

package com.curio.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v81 — the One UI 9.5 "shiny glass edge": a faint whitish gradient along
 * the pill's top edge (and a whisper at the bottom), fading out inside the
 * shape — NOT a border. This is the user's "1% whitish edge" ask: extremely
 * subtle, and only rendered in dark mode (a no-op in light).
 *
 * v101 — the "Subtle pill glow" option: when ON (the default) the edge is
 * GENTLER and TOP-ONLY — lower alphas and no bottom whisper — so the pill
 * catches white only along its top edge.
 */
fun Modifier.curioGlassEdge(shape: Shape): Modifier = composed {
    val layoutDirection = LocalLayoutDirection.current
    if (!isCurioDarkTheme()) return@composed this
    this.drawWithContent {
        drawContent()
        // The top catch must follow the pill's CURVED contour, not the
        // bounding box: a full-width band on a capsule paints past the
        // pill's rounded ends (the classic "the glow peeks out from behind
        // the pill" look). The band is drawn full-width so it covers the
        // WHOLE button edge-to-edge, and it is clipped to the pill outline
        // so the pill's own curved rim trims it at the rounded ends — the
        // catch reads as sitting INSIDE the pill, spanning its full length.
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        val subtle = AppPreferences.pillGlowSubtleState
        val stops = if (subtle) arrayOf(
            0f to Color.White.copy(alpha = 0.05f),
            0.14f to Color.White.copy(alpha = 0.02f),
            0.35f to Color.Transparent
        ) else arrayOf(
            0f to Color.White.copy(alpha = 0.10f),
            0.16f to Color.White.copy(alpha = 0.04f),
            0.40f to Color.Transparent,
            0.92f to Color.Transparent,
            1f to Color.White.copy(alpha = 0.05f)
        )
        // Top catch — always drawn (the subtle stops only reach the top;
        // the full stops add the whisper of white at the bottom for the
        // "shiny glass" look). The single vertical gradient spans the full
        // button width and is clipped to the pill outline, so the bright
        // edge follows the pill's curved contour instead of painting past
        // its rounded ends or stopping short of them.
        clipPath(path) {
            drawRect(brush = Brush.verticalGradient(*stops), size = size)
        }
    }
}

/**
 * v81 — the One UI 9.5 "gradient inner glow": a soft radial highlight of the
 * accent's light twin pushed in from the top-left of the pill, clipped to
 * [shape], drawn OVER the fill. The Samsung floating-pill language — a
 * pushed-in radiant feel, NOT a border or an outer drop glow. Dark mode only
 * (no-op in light). [strength] keeps it a whisper by default ("not too much,
 * just a little").
 */
fun Modifier.curioInnerGlow(
    shape: Shape,
    accent: Color,
    strength: Float = 0.16f
): Modifier = composed {
    val layoutDirection = LocalLayoutDirection.current
    if (!isCurioDarkTheme()) return@composed this
    this.drawWithContent {
        drawContent()
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        val glow = lerp(accent, Color.White, 0.75f)
        // v101 — the "Subtle pill glow" option: when ON (the default) the
        // glow is HALVED and hugs the pill's top (radius tied to the SHORT
        // side) so it reads as a top catch instead of filling the pill;
        // the fuller pushed-in glow stays when the option is off.
        val subtle = AppPreferences.pillGlowSubtleState
        val effectiveStrength = if (subtle) strength * 0.5f else strength
        // The radial must stay INSIDE the pill's curved ends, not wash over
        // them: the glow's reach is capped at ~0.55 of the short side from
        // its top-left anchor, and the pill outline clips whatever would
        // cross the capsule's rim.
        val radius = if (subtle) size.minDimension * 0.55f
        else size.maxDimension * 0.95f
        clipPath(path) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.copy(alpha = effectiveStrength),
                        glow.copy(alpha = effectiveStrength * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.30f, size.height * 0.15f),
                    radius = radius
                ),
                size = size
            )
        }
    }
}

/** v81 — convenience: the glass edge + inner glow in one chain (dark only). */
fun Modifier.curioGlassGlow(shape: Shape, accent: Color, strength: Float = 0.16f): Modifier =
    this.curioGlassEdge(shape).curioInnerGlow(shape, accent, strength)

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRoundRect(RoundRect(rect)) }
}

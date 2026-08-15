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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v81 — the One UI 9.5 "shiny glass edge": a faint whitish gradient along
 * the pill's top edge (and a whisper at the bottom), fading out inside the
 * shape — NOT a border. This is the user's "1% whitish edge" ask: extremely
 * subtle, and only rendered in dark mode (a no-op in light).
 */
fun Modifier.curioGlassEdge(shape: Shape): Modifier = composed {
    val layoutDirection = LocalLayoutDirection.current
    if (!isCurioDarkTheme()) return@composed this
    this.drawWithContent {
        drawContent()
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        clipPath(path) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.16f to Color.White.copy(alpha = 0.04f),
                    0.40f to Color.Transparent,
                    0.92f to Color.Transparent,
                    1f to Color.White.copy(alpha = 0.05f)
                ),
                size = size
            )
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
        clipPath(path) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.copy(alpha = strength),
                        glow.copy(alpha = strength * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.30f, size.height * 0.15f),
                    radius = size.maxDimension * 0.95f
                ),
                size = size
            )
        }
    }
}

/** v81 — convenience: the glass edge + inner glow in one chain (dark only). */
fun Modifier.curioGlassGlow(shape: Shape, accent: Color, strength: Float = 0.16f): Modifier =
    this.curioGlassEdge(shape).curioInnerGlow(shape, accent, strength)

/**
 * v94 — the LIGHT-mode "lit from above" catch on the torn heroes (the
 * light twin of the dark [curioGlassEdge] + [tornSeamLight]): a soft warm-
 * white gradient along the banner's top edge fading down ~45% — the banner
 * reads as catching warm light instead of a flat paint fill. Light mode
 * only (a no-op in dark, where the shiny edge + seam light own the look).
 */
fun Modifier.curioLightCatch(shape: Shape, strength: Float = 0.10f): Modifier = composed {
    val layoutDirection = LocalLayoutDirection.current
    if (isCurioDarkTheme()) return@composed this
    this.drawWithContent {
        drawContent()
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        clipPath(path) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFFFFF8F0).copy(alpha = strength),
                    0.28f to Color(0xFFFFF8F0).copy(alpha = strength * 0.35f),
                    0.45f to Color.Transparent
                ),
                size = size
            )
        }
    }
}

/**
 * v93 — the One UI torn-edge light catch: a faint light rim tracing the
 * hero's torn outline (the same [shape], stroked ~1.5px in a whisper of
 * white) so the ragged paper seam reads as catching light — the shiny-edge
 * language on the tear, NOT a border. Dark mode only (no-op in light).
 * The stroke follows the whole closed outline; on the full-bleed tear
 * heroes the top/side strokes hide under the status bar / at the screen
 * edges, so the visible catch is the torn bottom edge.
 */
fun Modifier.tornSeamLight(shape: Shape, strength: Float = 0.10f): Modifier = composed {
    val layoutDirection = LocalLayoutDirection.current
    if (!isCurioDarkTheme()) return@composed this
    this.drawWithContent {
        drawContent()
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        drawPath(
            path = path,
            color = Color.White.copy(alpha = strength),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRoundRect(RoundRect(rect)) }
}

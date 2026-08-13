package com.curio.app.ui.components

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences

/**
 * v9.x — the theme-style edge shine: a faint hairline light border all
 * around a surface PLUS a slightly brighter top-edge shine that fades down
 * over the corners — the classic raised-surface look.
 *  - AMOLED — "black glass": white (or [accent]-tinted) shine on the pure
 *    black cards and buttons.
 *  - Material — the category accent shines at the edge: device-colored
 *    surfaces keep their category identity as a colored rim light (the
 *    "category accent shine"), so buttons/cards stay Material while the
 *    category still glows on the rim.
 * No-op in the default Curio style.
 *
 * @param intensity Scales the hairline + top-shine alphas (1f = full shine,
 *   lower = a quieter whisper — used for the main Spin card's subtle
 *   category tint).
 * @param amoledHairline Restores the full-edge hairline ring in AMOLED for
 *   this surface (the v28 audit removed the AMOLED ring app-wide; the main
 *   deck card opts back in so the hero card keeps a readable edge on pure
 *   black). Ignored outside AMOLED.
 */
@Composable
fun Modifier.categoryEdgeShine(
    shape: Shape,
    accent: Color? = null,
    intensity: Float = 1f,
    amoledHairline: Boolean = false,
): Modifier {
    val style = AppPreferences.themeStyleState
    if (style != AppPreferences.THEME_STYLE_AMOLED && style != AppPreferences.THEME_STYLE_MATERIAL) return this
    val amoled = style == AppPreferences.THEME_STYLE_AMOLED
    val effective = intensity.coerceIn(0f, 1f)
    return this.drawWithCache {
        // The draw scope itself implements Density — `density` alone would
        // resolve to the scale factor (Float), which createOutline rejects.
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Generic -> outline.path
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        }
        val shine = accent ?: Color.White
        // v28 — AMOLED is BORDER-FREE app-wide (the full border-removal
        // audit): the full-edge hairline RING is gone on AMOLED — white
        // rings around every pill/card read as clunky "borders" on pure
        // black. The raised look on AMOLED comes from the TOP-LIT GLASS
        // shine (strengthened — it's the sole edge cue) + the v28 soft glow
        // shadow. The main Spin deck card opts back in via amoledHairline so
        // the hero card keeps a readable edge on pure black. Material keeps
        // its accent rim (that's the Material identity); the default Curio
        // style stays border-free as always.
        val hairlineAlpha = if (amoled && !amoledHairline) 0f
            else (if (accent != null) 0.30f else 0.14f) * effective
        val topAlpha = (if (accent != null) 0.52f else 0.30f) * effective
        val hairlineW = 1.dp.toPx()
        val shineW = 1.4.dp.toPx()
        val shineBand = 18.dp.toPx()
        onDrawWithContent {
            // Draw the surface content first, then the edge shine on top so
            // the highlight never hides behind an opaque fill.
            drawContent()
            // 1. Faint hairline around the whole edge (skipped on AMOLED).
            if (hairlineAlpha > 0f) {
                drawPath(
                    path,
                    color = shine.copy(alpha = hairlineAlpha),
                    style = Stroke(width = hairlineW)
                )
            }
            // 2. Brighter top-edge shine, fading out over the top band.
            clipRect(top = 0f, bottom = shineBand) {
                drawPath(
                    path,
                    brush = Brush.verticalGradient(
                        0f to shine.copy(alpha = topAlpha),
                        1f to shine.copy(alpha = 0f),
                        startY = 0f,
                        endY = shineBand
                    ),
                    style = Stroke(width = shineW)
                )
            }
        }
    }
}

/**
 * v9.x — theme-style button colors:
 *  - AMOLED: buttons go PITCH BLACK with their light content kept readable
 *    (the edge shine is applied separately via [Modifier.categoryEdgeShine],
 *    tinted with the button's accent).
 *  - Otherwise the caller's colors pass through untouched (Material callers
 *    pass the device primary via [com.curio.app.ui.theme.CurioCategory.themedButtonFill]).
 */
@Composable
fun curioButtonColors(
    containerColor: Color,
    contentColor: Color,
    disabledContainerColor: Color = containerColor.copy(alpha = 0.35f),
    disabledContentColor: Color = contentColor.copy(alpha = 0.45f),
): ButtonColors {
    val isAmoled = AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED
    return ButtonDefaults.buttonColors(
        containerColor = if (isAmoled) Color.Black else containerColor,
        contentColor = contentColor,
        disabledContainerColor = if (isAmoled) Color.Black.copy(alpha = 0.55f) else disabledContainerColor,
        disabledContentColor = disabledContentColor
    )
}

package com.curio.app.ui.components

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import com.curio.app.ui.theme.isCurioDarkTheme

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
    // Light: the Curio style is border-free — no-op (parameters kept for
    // call-site compatibility).
    if (!isCurioDarkTheme()) return this
    // v81 — dark mode: the One UI 9.5 "shiny glass edge" — a faint whitish
    // top-lit gradient INSIDE the shape (the user's "1% whitish edge, not a
    // border"), scaled by [intensity] like the old shine.
    return this.drawWithCache {
        // `this` (the CacheDrawScope) is the Density — the `density`
        // property in this scope resolves to a Float on the resolved
        // Compose BOM.
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = outline.toPath()
        onDrawBehind {
            clipPath(path) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.09f * intensity),
                        0.15f to Color.White.copy(alpha = 0.03f * intensity),
                        0.42f to Color.Transparent
                    ),
                    size = size
                )
            }
        }
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRoundRect(RoundRect(rect)) }
}

/**
 * Theme-aware button colors — v78: the AMOLED pitch-black override is gone
 * with dark mode, so the caller's colors pass through untouched.
 */
@Composable
fun curioButtonColors(
    containerColor: Color,
    contentColor: Color,
    disabledContainerColor: Color = containerColor.copy(alpha = 0.35f),
    disabledContentColor: Color = contentColor.copy(alpha = 0.45f),
): ButtonColors =
    ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )

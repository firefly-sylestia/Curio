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
    // v78 — the AMOLED/Material edge-shine is gone with those styles: the
    // Curio style is border-free, so the modifier is a no-op (parameters
    // kept for call-site compatibility).
    return this
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

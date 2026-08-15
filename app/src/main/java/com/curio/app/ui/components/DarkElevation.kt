package com.curio.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v28 — dark-mode elevation visibility. Compose's black shadows are
 * INVISIBLE on the app's near-black midnight surfaces, so dark mode draws
 * elevation one extra way, off in light mode:
 *
 *  [curioDarkGlow] — a soft LIGHT glow shadow (black is invisible on
 *    dark, a light tint reads as lift). Default ON (Appearance → Glow
 *    shadows). The Surface's own black `shadowElevation` stays in place —
 *    it is invisible in dark mode anyway, so there is no double shadow.
 *
 * Theme-aware composable modifier: place it on the Surface's `modifier`
 * chain (glow BEFORE the fill — shadows must sit behind the fill, rule 11).
 * The v28 hairline outline was REMOVED — dark cards
 * rely on the glow + shine instead of a light edge ring.
 */

/** The soft light glow tint — a whisper of white so elevation reads as a
 *  gentle lift on midnight surfaces without glowing like a neon tube. */
@Composable
fun curioDarkGlowColor(): Color = Color.White.copy(alpha = 0.22f)

/**
 * Dark-mode-only color-lit shadow. Light themes rely on normal Material
 * shadows; dark themes add this soft white lift behind raised surfaces so
 * Profile/Settings and shared cards read as metallic depth without borders.
 */
@Composable
fun Modifier.curioDarkGlow(elevation: Dp, shape: Shape): Modifier {
    if (!isCurioDarkTheme() || elevation <= 0.dp) return this
    return this.shadow(
        elevation = elevation + 3.dp,
        shape = shape,
        ambientColor = curioDarkGlowColor(),
        spotColor = curioDarkGlowColor(),
        clip = false
    )
}

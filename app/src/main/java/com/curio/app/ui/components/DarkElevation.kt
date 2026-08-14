package com.curio.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
fun curioDarkGlowColor(): Color = Color.White.copy(alpha = 0.16f)

/**
 * RETIRED (v30) — the "Glow shadows" Appearance option was removed because
 * the light glow read as a poor dark-mode look. The modifier is now a no-op
 * identity so every existing call site keeps compiling without drawing
 * anything; the Surface's own black shadowElevation (invisible on midnight
 * anyway) is the only shadow. Kept as a pass-through so the shared elevated
 * components don't need per-file edits, and the dormant [darkGlowState] pref
 * (default false) no longer gates anything.
 */
@Composable
fun Modifier.curioDarkGlow(elevation: Dp, shape: Shape): Modifier = this

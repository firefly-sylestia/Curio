package com.curio.app.features.personal

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.curio.app.features.settings.settingsAccentInk
import com.curio.app.features.settings.settingsReadableInk
import com.curio.app.features.settings.settingsRoseAccent

/**
 * THE PERSONAL FAMILY'S ACCENT — ONE place, so the journals, the shelf, the
 * book page and the writing canvas can never drift apart.
 *
 * These pages used `MaterialTheme.colorScheme.primary` throughout, which is
 * the app's fixed rose however the member set their theme up: someone on the
 * azure hero — or "the hero follows the Spin lane" — saw a rose that belonged
 * to nobody. They now wear the SAME accent the settings hero wears.
 *
 * AND THEY WEAR IT BY ROLE, which is the second half of the rule:
 *
 *  · [personalAccent] — FILLS: buttons, pills, rails, borders. The hero's own
 *    airy tone, which is what a fill wants.
 *  · [personalAccentInk] — TEXT and GLYPHS: a DEEP shade of the same hue. The
 *    pale fill as ink is the washed-out, cheap-looking accent the member
 *    called out; a heading or a 16dp glyph takes the darker shade.
 *  · low-alpha washes — HIGHLIGHTS and tracks only, never a foreground.
 *
 * [personalOnAccent] stays the ink that reads ON an accent fill (dark on the
 * airy fill, cream on a deep lane accent), and [personalIconTint] is kept as
 * the name every call site already uses for a glyph's tone — it now returns
 * the accent's ink rather than a paler tone.
 */
@Composable
internal fun personalAccent(): Color = settingsRoseAccent()

/** The accent as TEXT/GLYPH ink — the darker shade of the same shade. */
@Composable
internal fun personalAccentInk(): Color = settingsAccentInk()

/** Ink for text sitting ON the accent fill. */
@Composable
internal fun personalOnAccent(): Color = settingsReadableInk(personalAccent())

/** Glyph tone on an accent wash or a bare accent surface. */
@Composable
internal fun personalIconTint(accent: Color): Color = settingsAccentInk()

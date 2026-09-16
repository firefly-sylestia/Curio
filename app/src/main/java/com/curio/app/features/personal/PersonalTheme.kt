package com.curio.app.features.personal

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.curio.app.features.settings.settingsReadableInk
import com.curio.app.features.settings.settingsRoseAccent

/**
 * THE PERSONAL FAMILY'S ACCENT — ONE place, so the journals, the shelf, the
 * book page and the writing canvas can never drift apart.
 *
 * These pages used `MaterialTheme.colorScheme.primary` throughout, which is
 * the app's fixed rose however the member set their theme up: someone on the
 * azure hero — or "the hero follows the Spin lane" — saw a rose that belonged
 * to nobody. They now wear the SAME accent the settings hero wears, so the
 * pages match the theme the member actually chose.
 *
 * [personalOnAccent] is the ink that reads on that fill; [personalIconTint]
 * is the glyph tone (in light mode the accent alone is too pale for a small
 * icon on an accent wash, so icons take the hero's deeper ink instead).
 */
@Composable
internal fun personalAccent(): Color = settingsRoseAccent()

/** Ink for text sitting ON the accent fill. */
@Composable
internal fun personalOnAccent(): Color = settingsReadableInk(personalAccent())

/** Glyph tone on an accent wash or a bare accent surface. */
@Composable
internal fun personalIconTint(accent: Color): Color = settingsReadableInk(accent)

package com.curio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.curioGlyphInkNudge
import com.curio.app.ui.theme.CurioIcons

/**
 * Shared top-bar primitives — see Curio surface contract.
 *
 * Currently houses the standardized [CurioBackButton] used by every
 * push-destination screen (Settings, ManageCategories, TopicHistory,
 * Lightbox, plus the 6 main feature screens). Kept here so the back-button
 * appearance + theming can be tuned in one place if the visual language
 * ever needs to change (e.g. swap the arrow glyph, change the size, or
 * add a haptic on press).
 */

/**
 * The canonical "‹ Back" button. Circular Surface in surfaceVariant
 * containing the ChevronLeft Material Symbol — the unified back arrow
 * that mirrors [CurioForwardArrow]'s chevron. Sized at 40dp total (24dp
 * glyph + 8dp padding on each side) to match the standard Android
 * touch-target minimum.
 */
@Composable
fun CurioBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Back",
    // Optional overrides — the entry-detail hero's frosted-glass controls
    // pass a transparent container with a frosted-plate modifier and the
    // hero ink; every other screen keeps the default surfaceVariant circle.
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    // Optional floating shadow — the entry-detail hero's scroll-reactive
    // sticky bar grows this with scroll progress so the popped pills visibly
    // float off the page; every other screen keeps the flat 0dp default.
    shadowElevation: Dp = 0.dp,
    // v7.34 — rippleless press for frosted-glass pills: on the detail
    // hero's small circular frosted buttons the default Material ripple
    // expands past the plate and reads as a circular visual glitch, so the
    // detail screen passes true and the press is conveyed by the frost and
    // shadow alone (the same fix Home's sticky pills use — clickable with
    // indication = null). Every other screen keeps the standard ripple.
    disableRipple: Boolean = false,
    // v240 — optional external source so callers can attach the press spec
    // to this button's presses (detail sticky back pill).
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null
) {
    val icon: @Composable () -> Unit = {
        CurioIcon(
            name = CurioIcons.ChevronLeft,
            contentDescription = contentDescription,
            tint = contentColor,
            size = 24.dp,
            // v115 — the chevron's optical weight reads a hair low in the
            // 40dp circular pill (the same optical-weight correction as the
            // Home top-bar pills and the Profile/Settings sticky pills): the
            // natural line box centers the ink, but the < glyph's visual
            // mass sits slightly below its ink box center.
            modifier = Modifier
                .padding(8.dp)
                .curioGlyphInkNudge(-1f)
        )
    }
    if (disableRipple) {
        val fallbackSource = remember { MutableInteractionSource() }
        val interactionSource = interactionSource ?: fallbackSource
        Surface(
            shape = RoundedCornerShape(50),
            color = containerColor,
            shadowElevation = shadowElevation,
            modifier = modifier
                // v28 — dark mode elevation visibility (glow + hairline).
                .curioDarkGlow(shadowElevation, RoundedCornerShape(50))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) { icon() }
    } else {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            color = containerColor,
            shadowElevation = shadowElevation,
            modifier = modifier
                // v28 — dark mode elevation visibility (glow + hairline).
                .curioDarkGlow(shadowElevation, RoundedCornerShape(50))
        ) { icon() }
    }
}

/**
 * The canonical "forward" disclosure arrow (› chevron). Used everywhere a
 * row/card leads deeper — Home sections, Profile rows, Settings items, the
 * Spin "Tap to open" hint. One component keeps the arrow language unified
 * (consistent glyph, weight, and default tint) instead of ad-hoc ArrowForward
 * glyphs at random sizes and alphas.
 */
@Composable
fun CurioForwardArrow(
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 18.dp
) {
    CurioIcon(
        name = CurioIcons.ChevronRight,
        contentDescription = contentDescription,
        tint = tint,
        size = size,
        modifier = modifier
    )
}

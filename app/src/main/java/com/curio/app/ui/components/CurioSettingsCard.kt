package com.curio.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.CurioIcon

/**
 * Shared paper-card primitives for Profile + Settings — one visual language
 * for both screens so they can never drift apart (28dp cards with icon-chip
 * headers, arrow rows for navigation, inset dividers).
 */

/** 28dp paper card — the shared container for Profile and Settings cards.
 *  Elevation (not an outline) defines the card on the page; pass a custom
 *  [shadowElevation] to lift or flatten it. AMOLED cards wear the scheme's
 *  faint grey step (real shadows are invisible on pure black, so the
 *  container step IS the elevation there). */
@Composable
fun CurioSettingsCard(
    shadowElevation: Dp = 4.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        // v27n — every theme wears the faint container step as its elevation
        // (the black-glass shine edge keeps AMOLED cards defined on pure
        // black; the old hairline outline is gone).
        // v31 — a small tint of the PAGE BACKGROUND shade (in every theme):
        // the card no longer reads as a stark cream block; it carries a
        // whisper of the background so Profile/Settings cards melt into the
        // page. The step stays large enough that text (onSurface roles)
        // keeps its contrast in light, dark, pastel and AMOLED.
        color = lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.background,
            0.30f
        ),
        // AMOLED: tonalElevation overlays the scheme's primary (the coral
        // brand color) onto the container, which washed the pitch-black cards
        // with a faint rose tint. The black-glass shine edge keeps them
        // defined, so drop the tonal lift in AMOLED only — shadowElevation
        // stays so the card reads as raised.
        tonalElevation = if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) 0.dp else 3.dp,
        shadowElevation = shadowElevation,
        modifier = modifier
            .fillMaxWidth()
            // v28 — dark mode: soft light glow + faint hairline so the
            // elevation reads on midnight (black shadows are invisible).
            .curioDarkGlow(shadowElevation, RoundedCornerShape(28.dp))
            .categoryEdgeShine(RoundedCornerShape(28.dp))
    ) { Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), content = content) }
}

/** Icon-chip card header — coral glyph chip + title + subtitle (v15: AMOLED swaps the coral chip for a neutral glass plate). */
@Composable
fun CurioCardHeader(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // v15 — AMOLED: the brand coral reads as a weird red accent on pure
        // black, so the header chip becomes a sleek neutral glass plate.
        val isAmoled = AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isAmoled) MaterialTheme.colorScheme.surfaceVariant
                    else CurioColors.CoralBlush.copy(alpha = 0.16f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CurioIcon(
                    icon, null,
                    tint = if (isAmoled) MaterialTheme.colorScheme.onSurface else curioRoseInk(),
                    size = 20.dp
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Navigable setting row — icon + label/subtitle + forward arrow. */
@Composable
fun CurioSettingsRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CurioIcon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 21.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            CurioForwardArrow(tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), size = 17.dp)
        }
    }
}

/** Non-interactive informational row — icon + label/subtitle. */
@Composable
fun CurioSettingsInfoRow(icon: String, title: String, subtitle: String) {
    Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CurioIcon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 21.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Inset divider aligned to row content (icon column), shared by both screens. */
@Composable
fun CurioSettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), modifier = Modifier.padding(start = 33.dp))
}

/** Shared 12-hour clock label ("9:00 AM") used by Profile + Settings reminder selectors. */
fun formatHour(hour: Int): String {
    val normalized = hour.coerceIn(0, 23)
    val suffix = if (normalized < 12) "AM" else "PM"
    val display = when (val h = normalized % 12) { 0 -> 12 else -> h }
    return "$display:00 $suffix"
}

package com.curio.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.features.settings.settingsCardAccentInk
import com.curio.app.features.settings.settingsCardChipTint
import com.curio.app.features.settings.settingsCardTintLift
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
        // v42 — the background tint is COLOR-TINTED now: light/pastel cards
        // carry a whisper of the brand rose instead of flat cream, and AMOLED
        // cards lift to a soft GREY GLASS instead of pure black (raised grey
        // plates, not black slabs).
        // v72 — the tint HUE follows the hero the page wears
        // ([settingsCardTintLift]): the lane accent when the shared hero
        // follows a Spin lane, the azure twin when the sky-azure hero is on,
        // the rose otherwise — same strength as before, matching hue.
        // v78 — light Curio only (the AMOLED grey-glass step + tonal-lift
        // drop are gone with dark mode).
        color = lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            settingsCardTintLift(),
            0.30f
        ),
        tonalElevation = 3.dp,
        shadowElevation = shadowElevation,
        modifier = modifier
            .fillMaxWidth()
            // v28 — dark mode: soft light glow + faint hairline so the
            // elevation reads on midnight (black shadows are invisible).
            .curioDarkGlow(shadowElevation, RoundedCornerShape(28.dp))
            .categoryEdgeShine(RoundedCornerShape(28.dp))
            // v81 — dark: a faint radial inner glow in the hero hue on the
            // card (One UI pushed-in glass; self-gating, light is a no-op).
            .curioInnerGlow(RoundedCornerShape(28.dp), settingsCardChipTint(), strength = 0.10f)
    ) { Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), content = content) }
}

/** Icon-chip card header — coral glyph chip + title + subtitle (v15: AMOLED swaps the coral chip for a neutral glass plate). */
@Composable
fun CurioCardHeader(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // v42 — the header chip is COLOR-TINTED in every theme now: light
        // keeps the soft coral wash, and AMOLED keeps a muted coral glass
        // plate with coral ink instead of a neutral grey chip — the
        // settings/profile cards keep their color identity on black.
        // v72 — the chip + glyph follow the hero the page wears
        // ([settingsCardChipTint] / [settingsCardAccentInk]): lane accent
        // under Adaptive Hero, the azure twin when the sky-azure hero is on,
        // the rose otherwise — never a mismatched fixed coral.
        val chipTint = settingsCardChipTint()
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = chipTint.copy(alpha = 0.16f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CurioIcon(
                    icon, null,
                    tint = settingsCardAccentInk(),
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

/** Navigable setting row — accent-tinted icon chip + label/subtitle + forward
 *  arrow. v42 — the icon sits in a soft coral-tinted chip (matching the
 *  card-header language) instead of a plain grey glyph, so every row reads
 *  as a tappable colored control rather than a flat cream line. */
@Composable
fun CurioSettingsRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    val chipTint = settingsCardChipTint()
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(icon, null, tint = settingsCardAccentInk(), size = 20.dp)
            }
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

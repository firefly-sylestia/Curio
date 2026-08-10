package com.curio.app.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon

/**
 * Shared paper-card primitives for Profile + Settings — one visual language
 * for both screens so they can never drift apart (28dp cards with icon-chip
 * headers, arrow rows for navigation, inset dividers).
 */

/** 28dp paper card — the shared container for Profile and Settings cards.
 *  [border] defaults to the soft hairline outline; pass `null` for a
 *  borderless box (the Settings-hub sections and Profile cards wear this). */
@Composable
fun CurioSettingsCard(
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        // v9.x — AMOLED settings cards are proper pitch black; the black-glass
        // shine edge keeps them readable on the pure-black page.
        color = if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .categoryEdgeShine(RoundedCornerShape(28.dp))
    ) { Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), content = content) }
}

/** Icon-chip card header — coral glyph chip + title + subtitle. */
@Composable
fun CurioCardHeader(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = CurioColors.CoralBlush.copy(alpha = 0.16f), modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(icon, null, tint = CurioColors.CoralBlush, size = 20.dp) }
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

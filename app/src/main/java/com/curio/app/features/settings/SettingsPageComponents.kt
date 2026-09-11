package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.ui.components.rememberCurioControlTick
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.PlayfairDisplayFontFamily
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * The settings-family section heading — the hub's group-heading language: a
 * small glyph + Playfair serif label + a short rule. Shared by every
 * settings sub-page so the whole family reads as one (the old plain
 * [androidx.compose.material3] labels are replaced by this everywhere in
 * Settings).
 */
@Composable
fun SettingsSectionHeading(
    label: String,
    glyph: String = "\u2726",
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 10.dp, start = 2.dp, end = 2.dp)
    ) {
        if (glyph.length == 1) {
            // Single-char glyphs are literal Unicode marks (the default ✦).
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
        } else {
            // Multi-char values are CurioIcons names (e.g. topic history's
            // Star / Bookmark) — render the REAL icon so the heading never
            // prints the literal name.
            CurioIcon(
                name = glyph,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                size = 18.dp
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(11.dp))
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        )
    }
}

/** The shared settings hairline — inset to the frosted tile column
 *  (40dp tile + 13dp gap), so it aligns under every row's text the way
 *  [CurioSettingsDivider] did under the old bare icons. */
@Composable
fun SettingsOptionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = modifier.padding(start = 53.dp)
    )
}

/** A non-interactive settings row — frosted tile + title + subtitle (the
 *  info variant of [SettingsOptionRow], no chevron). */
@Composable
fun SettingsOptionInfoRow(
    icon: String?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        SettingsOptionIconTile(icon, dark)
        SettingsOptionCopy(title, subtitle, Modifier.weight(1f))
    }
}

/** A soft glass option card for the settings sub-pages — the hub's
 *  secondary-card surface (frosted white / raised dark), rounded 20. Rows
 *  sit inside it, separated by the shared settings divider. */
@Composable
fun SettingsOptionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isCurioDarkTheme()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.68f)
            )
            .padding(horizontal = 15.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) { content() }
}

/** The frosted icon tile every settings row wears (the hub's secondary-card
 *  tile). Null [icon] → no tile, the row reads as plain text + control. */
@Composable
private fun SettingsOptionIconTile(icon: String?, dark: Boolean) {
    if (icon == null) return
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (dark) Color.White.copy(alpha = 0.09f) else Color(0xFFF2E8DC)),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(
            name = icon,
            contentDescription = null,
            tint = if (dark) Color(0xFFD7B8A9) else Color(0xFF755647),
            size = 20.dp
        )
    }
}

/** The shared title/subtitle text block of a settings row. Callers pass
 *  the row's remaining width via [modifier] (the row scope's weight —
 *  `Modifier.weight(1f)` cannot be written here: it resolves only inside
 *  the Row/Column content receiver, not in a modifier argument). */
@Composable
private fun SettingsOptionCopy(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One tappable option row — frosted icon tile, title + subtitle, chevron
 *  (the hub's secondary-card row language). */
@Composable
fun SettingsOptionRow(
    icon: String?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            // v3xx46 — every settings row squishes + ticks on press now (the
            // shared press primitive); the ripple rides along via LocalIndication.
            .curioPressClickable(pressedScale = 0.975f, onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        SettingsOptionIconTile(icon, dark)
        SettingsOptionCopy(title, subtitle, Modifier.weight(1f))
        CurioIcon(
            name = CurioIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            size = 18.dp
        )
    }
}

/** A settings row ending in a switch (icon tile optional). */
@Composable
fun SettingsOptionSwitchRow(
    icon: String?,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
    ) {
        SettingsOptionIconTile(icon, dark)
        SettingsOptionCopy(title, subtitle, Modifier.weight(1f))
        // v3xx46 — every settings switch ticks as it flips (this row is the
        // switch the whole settings family, Experiments included, is built on).
        val tick = rememberCurioControlTick()
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { wanted -> tick { onCheckedChange(wanted) } },
            colors = SwitchDefaults.colors()
        )
    }
}

/** A settings row with a segmented control (Theme, Hero, …). [icon] is
 *  optional; [disabledIndices] + [disabledHint] mirror the old
 *  [androidx.compose.material3.SegmentedButton] contract. */
@Composable
fun SettingsOptionSegmentedRow(
    icon: String?,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean = true,
    disabledIndices: Set<Int> = emptySet(),
    disabledHint: String? = null,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val dark = isCurioDarkTheme()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            SettingsOptionIconTile(icon, dark)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    enabled = enabled && index !in disabledIndices,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
            }
        }
        if (disabledHint != null && disabledIndices.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            ) {
                CurioIcon(CurioIcons.Schedule, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                Text(
                    disabledHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
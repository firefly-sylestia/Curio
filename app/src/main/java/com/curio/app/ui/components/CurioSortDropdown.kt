package com.curio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/** One selectable sort field for [CurioSortDropdown]. */
data class CurioSortOption(
    val key: String,
    val label: String
)

/**
 * v26 — shared sort control: a pill whose LABEL zone opens a dropdown of
 * [options], while the trailing ARROW is its own tap zone that toggles
 * ascending/descending universally (⬆ = ascending, ⬇ = descending) for
 * whichever field is selected. Used by the Cabinet hero and the Topic
 * Database so every sort in the app reads the same way.
 *
 * v29 — ONE pill with two tap zones: the label + chevron zone opens the
 * dropdown, a thin vertical divider separates it from the arrow zone that
 * toggles direction. Bigger hit areas, and the fill is a LIGHT frosted
 * glass (banner lifted toward white) instead of the old ink-leaned fill
 * that read too dark in light and pastel themes — full-ink glyphs on top
 * stay readable in every mode.
 *
 * @param ink the tint for the pill glass + glyphs — hero callers pass their
 *   hero ink; plain screens pass the theme primary.
 */
@Composable
fun CurioSortDropdown(
    options: List<CurioSortOption>,
    selectedKey: String,
    ascending: Boolean,
    onSelect: (String) -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = MaterialTheme.colorScheme.primary,
    emphasized: Boolean = false,
    // v27n — the banner fill behind the pill (the opaque-fill conversion
    // needs it to resolve the same perceived tint on the hero).
    backdrop: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.key == selectedKey }
    // v29 — light frosted glass: the banner lifted toward white. On the
    // deep dark-mode banner this reads as a brighter glass; in light and
    // pastel it reads creamy — either way the full-ink glyphs pop instead
    // of sinking into a dark mauve pill (the v27r 0.35/0.55 ink-lean fills
    // were too dark in light + pastel).
    val fill = lerp(backdrop, Color.White, if (emphasized) 0.24f else 0.38f)
    val pillShape = RoundedCornerShape(50)

    Box(modifier = modifier) {
        Surface(
            shape = pillShape,
            color = fill,
            shadowElevation = 3.dp,
            // v28 — dark mode elevation visibility (glow + hairline).
            modifier = Modifier
                .curioDarkGlow(3.dp, pillShape)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(44.dp)
            ) {
                // ── Label zone — opens the dropdown ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(pillShape)
                        .clickable { expanded = true }
                        .padding(start = 14.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
                ) {
                    Text(
                        text = selected?.label.orEmpty(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = ink
                    )
                    CurioIcon(
                        name = CurioIcons.KeyboardArrowDown,
                        contentDescription = "Choose sort field",
                        tint = ink,
                        size = 18.dp
                    )
                }
                // ── Divider between the two zones ──
                VerticalDivider(
                    color = ink.copy(alpha = 0.30f),
                    modifier = Modifier
                        .fillMaxHeight(0.55f)
                        .width(1.dp)
                )
                // ── Arrow zone — toggles ascending/descending ──
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToggleDirection)
                        .padding(horizontal = 10.dp)
                        .height(40.dp)
                ) {
                    CurioIcon(
                        name = if (ascending) CurioIcons.ArrowUpward else CurioIcons.ArrowDownward,
                        contentDescription = if (ascending) {
                            "Ascending. Tap for descending"
                        } else {
                            "Descending. Tap for ascending"
                        },
                        tint = ink,
                        size = 22.dp
                    )
                }
            }
        }
        // v29 — redesigned dropdown: taller rounded menu, tonal depth, a
        // header line, and a check on the active field.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 3.dp,
            shadowElevation = 14.dp
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            options.forEach { option ->
                val active = option.key == selectedKey
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (active) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    },
                    trailingIcon = if (active) {
                        {
                            CurioIcon(
                                name = CurioIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 18.dp
                            )
                        }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(option.key)
                    }
                )
            }
        }
    }
}

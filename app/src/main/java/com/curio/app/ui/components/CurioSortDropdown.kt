package com.curio.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    // v27 — match the deepened hero pill glass: the old 18% fill vanished
    // on the rose banner, and the sort control had no border at all — the
    // capsules now read clearly next to the other hero actions.
    // v27n — the fill is now OPAQUE (ink lerped into the banner at the old
    // glass alpha): a translucent fill let the elevation shadow bleed
    // through as a blurry broken background. v27r — the fills deepened
    // (0.45/0.70 -> 0.35/0.55) so the arrow + label capsules read as
    // clearly visible pills on the banner, and the glyphs stay FULL ink.
    val fill = if (emphasized) lerp(ink, backdrop, 0.35f) else lerp(ink, backdrop, 0.55f)

    Box(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ── Label zone — opens the dropdown ──
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(50),
                color = fill,
                shadowElevation = 2.dp,
                // v28 — dark mode elevation visibility (glow + hairline).
                modifier = Modifier
                    .curioDarkGlow(2.dp, RoundedCornerShape(50))
                    .curioDarkOutline(RoundedCornerShape(50))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
                ) {
                    Text(
                        text = selected?.label.orEmpty(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ink
                    )
                    CurioIcon(
                        name = CurioIcons.KeyboardArrowDown,
                        contentDescription = "Choose sort field",
                        // v27r — full-opacity ink (was 70%) so the chevron
                        // reads clearly on the deeper pill.
                        tint = ink,
                        size = 16.dp
                    )
                }
            }
            // ── Arrow zone — toggles ascending/descending ──
            Surface(
                onClick = onToggleDirection,
                shape = CircleShape,
                color = fill,
                shadowElevation = 2.dp,
                // v28 — dark mode elevation visibility (glow + hairline).
                modifier = Modifier
                    .curioDarkGlow(2.dp, CircleShape)
                    .curioDarkOutline(CircleShape)
            ) {
                CurioIcon(
                    name = if (ascending) CurioIcons.ArrowUpward else CurioIcons.ArrowDownward,
                    contentDescription = if (ascending) {
                        "Ascending. Tap for descending"
                    } else {
                        "Descending. Tap for ascending"
                    },
                    tint = ink,
                    // v27r — the sort arrow is bigger (was 18dp in a 20dp
                    // box) and the circle a touch larger so it reads as a
                    // real toggle control.
                    size = 22.dp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (option.key == selectedKey) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option.key)
                    }
                )
            }
        }
    }
}

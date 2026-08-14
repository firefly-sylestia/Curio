package com.curio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.curio.app.ui.theme.curioPillTintLift

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
 * v31 — the pill slims down: tighter horizontal padding and the frosted
 * fill now lifts toward the PAGE BACKGROUND in light mode (via
 * [curioPillLift]) so the pill carries a small tint of the background
 * shade instead of stark cream. v33 — the corner radius is back to the
 * fully-rounded 50dp capsule so the sort pill matches the other action
 * pills exactly (the v31 16dp corners read rectangular next to the
 * capsule search/select pills); the 42dp height + tight padding keep it
 * slim, so it never reads fat.
 *
 * v30 — the menu itself now runs through [CurioDropdownMenu]: an opaque
 * surface tinted toward the page's CATEGORY ACCENT, with the selected row
 * lit in that accent (the old stock menu never carried the page color).
 *
 * @param ink the tint for the pill glass + glyphs — hero callers pass their
 *   hero ink; plain screens pass the theme primary.
 * @param accent the page's category accent — tints the dropdown surface and
 *   lights the active row.
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
    backdrop: Color,
    // v30 — the page/category accent that lights the dropdown's selected row.
    accent: Color = MaterialTheme.colorScheme.primary
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.key == selectedKey }
    // v29 — light frosted glass: the banner lifted toward white. On the
    // deep dark-mode banner this reads as a brighter glass; in light and
    // pastel it reads creamy — either way the full-ink glyphs pop instead
    // of sinking into a dark mauve pill (the v27r 0.35/0.55 ink-lean fills
    // were too dark in light + pastel).
    // v42 — the fill lifts toward the COLOR-TINTED glass ([curioPillTintLift]
    // — a whisper of the brand rose instead of plain cream) so the sort pill
    // matches the hero pills' new tinted look; AMOLED gets grey glass.
    val fill = lerp(backdrop, curioPillTintLift(), if (emphasized) 0.24f else 0.38f)
    // v43 — match the hero Category pill exactly: the full 50dp capsule
    // (the v42 18dp corners read rectangular next to it) with the same
    // 42dp height and 14/10dp padding, so the sort pill reads as a true
    // sibling of the Category pill in the Cabinet + Topic Browser heroes.
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
                // v30 — uniform hero-pill height: 42dp like the action pills
                // (was 44dp — the sort pill read too thick next to them).
                modifier = Modifier.heightIn(min = 42.dp)
            ) {
                // ── Label zone — opens the dropdown ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(pillShape)
                        .clickable { expanded = true }
                        .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
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
                        .padding(horizontal = 8.dp)
                        .heightIn(min = 40.dp)
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
        // v30 — shared accent-themed menu: tinted surface, accent header,
        // accent-lit active row with check.
        CurioDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            accent = accent,
            header = {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    ),
                    color = accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        ) {
            options.forEach { option ->
                val active = option.key == selectedKey
                CurioDropdownItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (active) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    },
                    selected = active,
                    accent = accent,
                    trailingIcon = if (active) {
                        {
                            CurioIcon(
                                name = CurioIcons.Check,
                                contentDescription = null,
                                tint = accent,
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

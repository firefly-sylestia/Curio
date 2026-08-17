package com.curio.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioCategory
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.curioCorner
import com.curio.app.ui.theme.themedAccent

/**
 * A Curio category chip — used in Home's category chip row (§3), Category
 * Picker's tile row (§4), and Cabinet's filter chip row (§9).
 *
 * Visual rules:
 * - Unselected: outlined chip on `surface`, glyph + label in
 *   `onSurfaceVariant`.
 * - Selected: SOLID category-accent fill with on-accent glyph + label, no
 *   border; elevation stays a flat 2dp in both states (v27q — selection
 *   reads through the fill, not a raise).
 * - Single-select within a row — selection state is owned by the parent
 *   screen, this chip just renders.
 *
 * The chip height and shape follow Curio's shape tokens: 16dp corners
 * (chips are `small` per §0.3) and ~36dp height (M3 default).
 */
@Composable
fun CurioCategoryChip(
    category: CurioCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = category.displayName
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = {
            CurioIcon(
                name = category.iconGlyph,
                contentDescription = null,
                // categoryInk (the deep accent in light / light twin in dark),
                // NOT themedAccent — in pastel mode the pastel accent would
                // disappear on the light chip surface.
                tint = if (selected) category.onAccent()
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        },
        shape = curioCorner(16.dp, MaterialTheme.shapes.small),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (selected) category.themedAccent()
                             else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) category.onAccent()
                         else MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = if (selected) category.onAccent()
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = category.themedAccent(),
            selectedLabelColor = category.onAccent(),
            selectedLeadingIconColor = category.onAccent()
        ),
        // v27q — selection reads through the SOLID accent fill + on-accent
        // content; elevation stays a flat 2dp in both states so chips never
        // raise (the old 4/2 raise was the blurry-shadow bug class).
        border = BorderStroke(0.dp, Color.Transparent),
        elevation = FilterChipDefaults.filterChipElevation(elevation = 2.dp)
    )
}

/**
 * The "Surprise me" wildcard chip — pinned at the start of Home's category
 * row (§3) and Category Picker. Renders with the casino (die) glyph and
 * uses the coral primary as its accent rather than a tint, so it stands
 * apart from the named-category chips.
 */
@Composable
fun CurioWildcardChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Surprise me"
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = {
            CurioIcon(
                name = CurioIcons.Casino,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.primary,
                size = 18.dp
            )
        },
        shape = curioCorner(16.dp, MaterialTheme.shapes.small),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimary
                         else MaterialTheme.colorScheme.primary,
            iconColor = if (selected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.primary,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.primary,
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp
        )
    )
}

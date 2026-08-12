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
import com.curio.app.ui.theme.categoryInk

/**
 * A Curio category chip — used in Home's category chip row (§3), Category
 * Picker's tile row (§4), and Cabinet's filter chip row (§9).
 *
 * Visual rules:
 * - Unselected: outlined chip on `surface`, glyph + label in
 *   `onSurfaceVariant`.
 * - Selected: filled chip in the category's `tint` (accent @ 20% alpha),
 *   glyph + label in the category's `accent`, no border (clean filled look).
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
                tint = if (selected) category.categoryInk()
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (selected) category.tint
                             else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) category.categoryInk()
                         else MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = if (selected) category.categoryInk()
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = category.tint,
            selectedLabelColor = category.categoryInk(),
            selectedLeadingIconColor = category.categoryInk()
        ),
        // v27n — elevation replaces the outline: chips lift off the page
        // (higher when selected) instead of drawing a hairline ring.
        border = BorderStroke(0.dp, Color.Transparent),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = if (selected) 4.dp else 2.dp
        )
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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

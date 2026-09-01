package com.curio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v30 — the ONE dropdown language for the whole app. Every menu (sort
 * picker, detail more-menu, rich-text size picker) now renders through
 * this component so the menus match the page accent instead of the old
 * stock Material surface:
 *
 *  - an OPAQUE surface tinted toward the page's category accent
 *    (a 10% dark / 6% light pull — a whisper of color, never muddy),
 *  - the v29 rounded 20dp shape with tonal depth, no hardcoded light
 *    containers (the old EntryDetail menu baked in a near-white fill
 *    that clashed in dark mode),
 *  - an optional accent header line, then the rows.
 *
 * @param accent the page's category accent — tints the container, the
 *   optional header, and lights the selected row.
 */
/**
 * v30 — the ONE dropdown language for the whole app. Every menu (sort
 * picker, detail more-menu, rich-text size picker) now renders through
 * this component so the menus match the page accent instead of the old
 * stock Material surface.
 *
 * When [glassBackdrop] is provided, renders as an inline [Surface] with
 * liquid glass frost instead of a Popup — so the backdrop sampling works
 * and the menu reads as real frosted glass. The caller must position the
 * menu via [modifier] (e.g. Modifier.align(Alignment.TopEnd)).
 *
 * When [glassBackdrop] is null, falls back to Material3 [DropdownMenu]
 * (Popup-based, no real backdrop sampling).
 */
@Composable
fun CurioDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(20.dp),
    // v42 — menus default WIDER than the anchor pill (the old menu hugged
    // the pill's narrow width and read thin); callers can override.
    minWidth: Dp = 236.dp,
    header: (@Composable () -> Unit)? = null,
    glassBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return
    // Opaque accent-tinted surface — v78: light only (the stronger dark
    // pull is gone with dark mode).
    val container = lerp(
        MaterialTheme.colorScheme.surface,
        accent,
        0.06f
    )
    if (glassBackdrop != null && isLiquidGlassPillsActive()) {
        // Inline glass frost — real backdrop sampling, no Popup.
        Surface(
            shape = shape,
            color = Color.Transparent,
            shadowElevation = 0.dp,
            modifier = modifier
                .widthIn(min = minWidth)
                .liquidGlassCapsule(
                    container,
                    washAlpha = 0.45f,
                    backdrop = glassBackdrop,
                    shape = shape
                )
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                if (header != null) header()
                content()
            }
        }
    } else {
        // Standard Popup menu — no real backdrop sampling.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.widthIn(min = minWidth),
            containerColor = container,
            shape = shape,
            tonalElevation = 3.dp,
            shadowElevation = 14.dp
        ) {
            if (header != null) header()
            content()
        }
    }
}

/**
 * One row of [CurioDropdownMenu]. The selected row gets an accent wash +
 * accent text (the "category accent selected color" the old stock menus
 * never had); [danger] rows flip to the theme error color. Leading/trailing
 * icons and the text inherit the row's content color, so callers can pass
 * plain content and the theme decides readability in light/dark/pastel.
 */
@Composable
fun CurioDropdownItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    danger: Boolean = false,
    enabled: Boolean = true
) {
    val contentColor = when {
        danger -> MaterialTheme.colorScheme.error
        selected -> accent
        else -> MaterialTheme.colorScheme.onSurface
    }
    val rowFill = if (selected) {
        // v78 — light only (the stronger dark wash is gone with dark mode).
        accent.copy(alpha = 0.13f)
    } else {
        Color.Transparent
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .background(rowFill)
                // v42 — taller rows: 14dp vertical gives the menu presence
                // (was 12dp — the old menu read thin and cramped).
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) { text() }
            if (trailingIcon != null) {
                Spacer(Modifier.width(12.dp))
                trailingIcon()
            }
        }
    }
}

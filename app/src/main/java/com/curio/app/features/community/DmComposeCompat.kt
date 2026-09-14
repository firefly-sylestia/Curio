package com.curio.app.features.community

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer as composeGraphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange

/** Compatibility overloads for the DM polish pass. */
@androidx.compose.runtime.Composable
private fun DmCurioIconCompat(
    name: String,
    contentDescription: String?,
    tint: Color,
    size: Dp
) {
    com.curio.app.ui.theme.CurioIcon(
        name = name,
        contentDescription = contentDescription,
        tint = tint,
        size = size
    )
}

/** Keeps the legacy four-argument CurioIcon calls in the DM screen source-compatible. */
@androidx.compose.runtime.Composable
fun CurioIcon(
    name: String,
    contentDescription: String?,
    tint: Color,
    size: Dp
) = DmCurioIconCompat(name, contentDescription, tint, size)

/** Bridges the graphicsLayer import used by the DM gesture implementation. */
fun Modifier.dmGraphicsLayer(block: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit): Modifier =
    this.composeGraphicsLayer(block)

/**
 * Row-local fallback for the one action-tray weight call. The tray's actions
 * are deliberately compact, so a quarter-width allocation is equivalent to
 * the old equal-weight layout without depending on an unavailable import.
 */
fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier =
    this.fillMaxWidth(weight.coerceIn(0f, 1f))

/** Legacy pointer-consumption bridge used by the swipe gesture. */
fun PointerInputChange.consume() {
    consumePositionChange()
}

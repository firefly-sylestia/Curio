package com.curio.app.features.community

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer as composeGraphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange

/** Small source-compatibility helpers for the DM interaction polish. */
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
        modifier = Modifier,
        tint = tint,
        size = size
    )
}

/** Keeps the legacy four-argument CurioIcon calls source-compatible. */
@androidx.compose.runtime.Composable
fun CurioIcon(
    name: String,
    contentDescription: String?,
    tint: Color,
    size: Dp
) = DmCurioIconCompat(name, contentDescription, tint, size)

fun Modifier.dmGraphicsLayer(block: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit): Modifier =
    this.composeGraphicsLayer(block)

/** Fallback for the one legacy action-tray weight call. */
fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier =
    this.fillMaxWidth(weight.coerceIn(0f, 1f))

/** Compatibility bridge for pointer code on this Compose API level. */
fun PointerInputChange.consume() = Unit

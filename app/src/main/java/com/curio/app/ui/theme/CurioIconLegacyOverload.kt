package com.curio.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Source-compatibility overload for older positional icon calls. */
@Composable
fun CurioIcon(
    name: String,
    contentDescription: String?,
    tint: Color,
    size: Dp
) {
    CurioIcon(
        name = name,
        contentDescription = contentDescription,
        tint = tint,
        size = size
    )
}

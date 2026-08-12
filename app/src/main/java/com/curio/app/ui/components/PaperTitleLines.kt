package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * v27 — experimental paper-title underline: two short horizontal lines under
 * a hero title, like the double underline drawn under a title on paper
 * (toggle in Settings → Experiments → Paper & headers → "Title cut lines").
 *
 * Place it directly BELOW the title text; alignment follows the parent
 * column — left-aligned titles get left lines, centered titles get centered
 * lines (the box is a fixed 64dp wide and hugs the text start / center).
 */
@Composable
fun PaperTitleLines(
    ink: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.width(64.dp).height(12.dp)) {
        val y1 = 2.dp.toPx()
        val y2 = y1 + 7.dp.toPx()
        drawLine(
            color = ink.copy(alpha = 0.30f),
            start = Offset(0f, y1),
            end = Offset(size.width, y1),
            strokeWidth = 2.4.dp.toPx()
        )
        drawLine(
            color = ink.copy(alpha = 0.20f),
            start = Offset(0f, y2),
            end = Offset(size.width * 0.62f, y2),
            strokeWidth = 2.dp.toPx()
        )
    }
}

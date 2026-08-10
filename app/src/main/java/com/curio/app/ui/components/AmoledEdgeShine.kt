package com.curio.app.ui.components

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences

/**
 * v9.x — the AMOLED "black glass" card edge: a faint hairline light border
 * all around the surface PLUS a slightly brighter top-edge shine that fades
 * down over the corners — the classic pure-black card look. [accent] tints
 * the shine with a category color ("the category shine"), e.g. on category
 * cards and category buttons, so the identity stays even on pitch-black.
 * No-op outside the AMOLED theme style.
 */
@Composable
fun Modifier.amoledEdgeShine(shape: Shape, accent: Color? = null): Modifier {
    if (AppPreferences.themeStyleState != AppPreferences.THEME_STYLE_AMOLED) return this
    return this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, density)
        val path = when (outline) {
            is Outline.Generic -> outline.path
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        }
        val shine = accent ?: Color.White
        val hairlineW = 1.dp.toPx()
        val shineW = 1.4.dp.toPx()
        val shineBand = 18.dp.toPx()
        onDrawWithContent {
            // Draw the surface content first, then the edge shine on top so
            // the highlight never hides behind an opaque fill.
            drawContent()
            // 1. Faint hairline around the whole edge.
            drawPath(
                path,
                color = shine.copy(alpha = if (accent != null) 0.26f else 0.10f),
                style = Stroke(width = hairlineW)
            )
            // 2. Brighter top-edge shine, fading out over the top band.
            clipRect(top = 0f, bottom = shineBand) {
                drawPath(
                    path,
                    brush = Brush.verticalGradient(
                        0f to shine.copy(alpha = if (accent != null) 0.45f else 0.22f),
                        1f to shine.copy(alpha = 0f),
                        startY = 0f,
                        endY = shineBand
                    ),
                    style = Stroke(width = shineW)
                )
            }
        }
    }
}

/**
 * v9.x — AMOLED button colors: buttons go PITCH BLACK with their light
 * content kept readable (the edge shine is applied separately via
 * [Modifier.amoledEdgeShine], tinted with the button's accent). Outside the
 * AMOLED style the caller's colors pass through untouched.
 */
@Composable
fun curioButtonColors(
    containerColor: Color,
    contentColor: Color,
    disabledContainerColor: Color = containerColor.copy(alpha = 0.35f),
    disabledContentColor: Color = contentColor.copy(alpha = 0.45f),
): ButtonColors {
    val isAmoled = AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED
    return ButtonDefaults.buttonColors(
        containerColor = if (isAmoled) Color.Black else containerColor,
        contentColor = contentColor,
        disabledContainerColor = if (isAmoled) Color.Black.copy(alpha = 0.55f) else disabledContainerColor,
        disabledContentColor = disabledContentColor
    )
}

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
 * v9.x — the theme-style edge shine: a faint hairline light border all
 * around a surface PLUS a slightly brighter top-edge shine that fades down
 * over the corners — the classic raised-surface look.
 *  - AMOLED — "black glass": white (or [accent]-tinted) shine on the pure
 *    black cards and buttons.
 *  - Material — the category accent shines at the edge: device-colored
 *    surfaces keep their category identity as a colored rim light (the
 *    "category accent shine"), so buttons/cards stay Material while the
 *    category still glows on the rim.
 * No-op in the default Curio style.
 */
@Composable
fun Modifier.categoryEdgeShine(shape: Shape, accent: Color? = null): Modifier {
    val style = AppPreferences.themeStyleState
    if (style != AppPreferences.THEME_STYLE_AMOLED && style != AppPreferences.THEME_STYLE_MATERIAL) return this
    val amoled = style == AppPreferences.THEME_STYLE_AMOLED
    return this.drawWithCache {
        // The draw scope itself implements Density — `density` alone would
        // resolve to the scale factor (Float), which createOutline rejects.
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Generic -> outline.path
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        }
        val shine = accent ?: Color.White
        // AMOLED sits on pitch black, so the shine can stay quieter; Material
        // surfaces are mid-tone device colors, so the accent rim needs a touch
        // more presence to read as a rim light.
        val hairlineAlpha = if (accent != null) (if (amoled) 0.26f else 0.30f) else (if (amoled) 0.10f else 0.14f)
        val topAlpha = if (accent != null) (if (amoled) 0.45f else 0.52f) else (if (amoled) 0.22f else 0.30f)
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
                color = shine.copy(alpha = hairlineAlpha),
                style = Stroke(width = hairlineW)
            )
            // 2. Brighter top-edge shine, fading out over the top band.
            clipRect(top = 0f, bottom = shineBand) {
                drawPath(
                    path,
                    brush = Brush.verticalGradient(
                        0f to shine.copy(alpha = topAlpha),
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
 * v9.x — theme-style button colors:
 *  - AMOLED: buttons go PITCH BLACK with their light content kept readable
 *    (the edge shine is applied separately via [Modifier.categoryEdgeShine],
 *    tinted with the button's accent).
 *  - Otherwise the caller's colors pass through untouched (Material callers
 *    pass the device primary via [com.curio.app.ui.theme.CurioCategory.themedButtonFill]).
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

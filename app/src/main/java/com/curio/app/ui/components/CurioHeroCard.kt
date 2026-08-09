package com.curio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent

/**
 * Home screen's hero \"Shuffle the Deck\" card — see Curio Home contract.
 *
 * The single largest tap target on the home screen (~40% vertical space).
 *
 * Upgraded with:
 *  - Breathing ambient animation: subtle scale pulse on the background glyph
 *  - Shimmer sweep: gentle light pass across the card surface
 *  - Press animation: scales to 0.96 on press with spring back
 *  - Animated category transition: morph between category accent colors
 *  - Decorative particle: slowly orbiting sparkle near the title
 *
 * When [selectedCategory] is non-null, the card switches to that category's
 * accent color so the user feels their chip selection propagate visually.
 */
@Composable
fun CurioHeroShuffleCard(
    selectedCategory: com.curio.app.data.CurioCategory?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    wildcardSelected: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val isWildcard = wildcardSelected || selectedCategory?.id == CategoryId.WILDCARD
    val themeAccent = when (AppPreferences.themeStyleState) {
        AppPreferences.THEME_STYLE_MATERIAL,
        AppPreferences.THEME_STYLE_AMOLED -> MaterialTheme.colorScheme.primary
        else -> CurioColors.CoralBlush
    }
    val activeAccent: Color = when {
        isWildcard -> themeAccent
        selectedCategory != null -> selectedCategory.themedAccent()
        else -> themeAccent
    }
    val cardGradient = CurioGradients.cardGradient(activeAccent)
    // Pair hero content with the same semantic role that owns the fill:
    // Material uses onPrimary, AMOLED uses onSurface, and Curio category
    // heroes keep their category-aware onAccent ink. This avoids a fixed
    // white label becoming low-contrast on a dynamic or AMOLED card.
    val contentInk: Color = when {
        AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED ->
            MaterialTheme.colorScheme.onSurface
        AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL ->
            MaterialTheme.colorScheme.onPrimary
        selectedCategory != null -> selectedCategory.onAccent()
        AppPreferences.pastelColorsState -> CurioColors.DeepPlum
        else -> MaterialTheme.colorScheme.onPrimary
    }

    // ── Press scale animation ─────────────────────────────────────────────
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = CurioMotion.Springs.Press,
        label = "heroPress"
    )

    Surface(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .scale(pressScale),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Solid gradient background ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(cardGradient),
                        RoundedCornerShape(28.dp)
                    )
            )

            // ── Background wheel glyph — still, decorative ──────────────
            CurioIcon(
                name = CurioIcons.AutoAwesome,
                contentDescription = null,
                tint = contentInk.copy(alpha = 0.16f),
                size = 180.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )

            // ── Content ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "SHUFFLE",
                        style = MaterialTheme.typography.displayLarge.copy(
                            color = contentInk,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    CurioIcon(
                        name = CurioIcons.AutoAwesome,
                        contentDescription = null,
                        tint = contentInk.copy(alpha = 0.45f),
                        size = 20.dp
                    )
                }
                Column {
                    Text(
                        text = "the wheel",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = contentInk
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    MorphingContainer(
                        trigger = selectedCategory?.displayName ?: if (wildcardSelected) "surprise" else "default"
                    ) {
                        Text(
                            text = selectedCategory?.let { "Shuffle for ${it.displayName}" }
                                ?: if (wildcardSelected) "Surprise me"
                                else "Tap to discover something new",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = contentInk.copy(alpha = 0.92f)
                            )
                        )
                    }
                }
            }
        }
    }

    // Reset press state after navigation
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(300)
            pressed = false
        }
    }
}

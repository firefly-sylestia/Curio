package com.curio.app.ui.pet

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlin.math.sin

/**
 * Curie's fixed home scene: a small 2.5D room rendered as layered pixel-soft
 * shapes with the pet sprite placed on top. The room deliberately has open
 * floor and wall space so shelves, rooms, and props can expand later without
 * changing the home API or replacing the pet's 2D sprite renderer.
 *
 * The old editable flower-bed rows are intentionally not read here. Existing
 * saved rows remain harmless legacy preference data, while every install sees
 * the new fixed house scene.
 */
@Composable
fun CurioPetHome(
    petInside: Boolean,
    sleeping: Boolean,
    modifier: Modifier = Modifier,
    homeSize: Dp = 76.dp,
    onTap: (() -> Unit)? = null,
    celebrateKey: Int = 0,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val mood = if (sleeping) CurioPet.Mood.SLEEPY
    else CurioPet.mood(context, CurioQuests.categoriesState)
    val timeOfDay = CurioPet.timeOfDay()
    val evoPath = CurioPet.currentEvoPath()
    val accent = when (evoPath) {
        CurioPet.EvoPath.FIRE -> Color(0xFFFF765C)
        CurioPet.EvoPath.WATER -> Color(0xFF62B8EC)
        CurioPet.EvoPath.NATURE -> Color(0xFF82C96B)
        null -> Color(0xFFFFA58E)
    }
    val ink = Color(0xFF4A3426)
    val night = timeOfDay == CurioPet.TimeOfDay.NIGHT
    val twinkleSpec: InfiniteRepeatableSpec<Float> = remember {
        infiniteRepeatable(tween(2400, easing = LinearEasing))
    }
    val twinkle by rememberInfiniteTransition(label = "houseTwinkle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = twinkleSpec,
        label = "houseTwinklePhase"
    )

    val desc = contentDescription
    Box(
        modifier = modifier
            .size(homeSize)
            .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
            .then(if (desc != null) Modifier.semantics { this.contentDescription = desc } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val unit = size.width / 32f
            val wallTop = 4f * unit
            val wallBottom = 25f * unit
            val floorTop = 21f * unit

            val wallTopColor = if (night) Color(0xFF27304B) else Color(0xFFFFE7D2)
            val wallBottomColor = if (night) Color(0xFF3A4260) else Color(0xFFFFCFAE)
            val floorColor = if (night) Color(0xFF59445A) else Color(0xFFC98E66)
            val floorHighlight = if (night) Color(0xFF72566A) else Color(0xFFE2AE7F)
            val trim = if (night) Color(0xFFD5B5A5) else Color(0xFF8B5C45)
            val wood = if (night) Color(0xFF9B6B5B) else Color(0xFFB97855)
            val woodDark = if (night) Color(0xFF684654) else Color(0xFF774836)
            val warmLight = if (night) Color(0xFFFFD786) else Color(0xFFFFB96E)

            // Back wall and floor use a vertical split to suggest a room depth.
            drawRect(
                brush = Brush.verticalGradient(listOf(wallTopColor, wallBottomColor)),
                topLeft = Offset(0f, wallTop),
                size = Size(size.width, floorTop - wallTop)
            )
            drawRect(color = floorColor, topLeft = Offset(0f, floorTop), size = Size(size.width, size.height - floorTop))
            drawRect(
                color = floorHighlight.copy(alpha = 0.65f),
                topLeft = Offset(0f, floorTop),
                size = Size(size.width, unit * 1.1f)
            )

            // Framing beams create the 2.5D house shell.
            drawRoundRect(
                color = trim,
                topLeft = Offset(unit * 1.4f, wallTop - unit),
                size = Size(size.width - unit * 2.8f, unit * 2.2f),
                cornerRadius = CornerRadius(unit * 0.6f)
            )
            drawRect(color = trim, topLeft = Offset(unit * 1.4f, wallTop), size = Size(unit * 1.25f, unit * 18f))
            drawRect(color = trim, topLeft = Offset(size.width - unit * 2.65f, wallTop), size = Size(unit * 1.25f, unit * 18f))

            // Window: a recessed opening with a sill, moon/sun, and stars.
            drawRoundRect(
                color = woodDark,
                topLeft = Offset(unit * 19f, unit * 7f),
                size = Size(unit * 9f, unit * 8f),
                cornerRadius = CornerRadius(unit * 0.7f)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(if (night) Color(0xFF1A2340) else Color(0xFF9CD9E6), if (night) Color(0xFF35476B) else Color(0xFFFFC780))
                ),
                topLeft = Offset(unit * 19.8f, unit * 7.8f),
                size = Size(unit * 7.4f, unit * 6.4f)
            )
            drawRect(color = woodDark, topLeft = Offset(unit * 22.45f, unit * 7.8f), size = Size(unit * 0.55f, unit * 6.4f))
            drawRect(color = woodDark, topLeft = Offset(unit * 19.8f, unit * 10.7f), size = Size(unit * 7.4f, unit * 0.55f))
            if (night) {
                drawCircle(Color(0xFFFFF1C7), radius = unit * 1.1f, center = Offset(unit * 21.5f, unit * 9.3f))
                drawCircle(Color(0xFF1A2340), radius = unit * 0.85f, center = Offset(unit * 22.1f, unit * 8.9f))
                val sparkle = sin(twinkle * 2f * kotlin.math.PI.toFloat()) * 0.5f + 0.5f
                drawCircle(Color(0xFFFFF5D2).copy(alpha = 0.45f + sparkle * 0.45f), radius = unit * 0.18f, center = Offset(unit * 25.7f, unit * 9f))
                drawCircle(Color(0xFFFFF5D2).copy(alpha = 0.35f + (1f - sparkle) * 0.45f), radius = unit * 0.16f, center = Offset(unit * 24.3f, unit * 12.2f))
            } else {
                drawCircle(Color(0xFFFFE08A), radius = unit * 1.15f, center = Offset(unit * 25.7f, unit * 9f))
            }
            drawRect(color = wood, topLeft = Offset(unit * 18.2f, unit * 14.2f), size = Size(unit * 10.6f, unit * 1f))

            // Wall shelf with three future-friendly prop slots.
            drawRoundRect(
                color = wood,
                topLeft = Offset(unit * 4f, unit * 8f),
                size = Size(unit * 10f, unit * 0.9f),
                cornerRadius = CornerRadius(unit * 0.25f)
            )
            drawRoundRect(color = accent.copy(alpha = 0.85f), topLeft = Offset(unit * 5f, unit * 5.7f), size = Size(unit * 1.8f, unit * 2.3f), cornerRadius = CornerRadius(unit * 0.45f))
            drawRoundRect(color = warmLight, topLeft = Offset(unit * 8.2f, unit * 6.2f), size = Size(unit * 1.2f, unit * 1.8f), cornerRadius = CornerRadius(unit * 0.25f))
            drawCircle(Color(0xFF9CCB8B), radius = unit * 0.75f, center = Offset(unit * 12f, unit * 6.4f))
            drawRect(color = woodDark, topLeft = Offset(unit * 11.8f, unit * 6.8f), size = Size(unit * 0.4f, unit * 1.5f))

            // A small angled rug gives the foreground a layered, staged feel.
            val rug = Path().apply {
                moveTo(unit * 5f, unit * 25f)
                lineTo(unit * 27f, unit * 25f)
                lineTo(unit * 30f, unit * 31f)
                lineTo(unit * 2f, unit * 31f)
                close()
            }
            drawPath(rug, color = accent.copy(alpha = if (night) 0.45f else 0.72f))
            drawPath(
                Path().apply {
                    moveTo(unit * 7f, unit * 27f)
                    lineTo(unit * 25f, unit * 27f)
                    lineTo(unit * 27f, unit * 29f)
                    lineTo(unit * 5f, unit * 29f)
                    close()
                },
                color = Color.White.copy(alpha = 0.22f)
            )

            // A raised sleeping nook remains readable behind the 2D pet sprite.
            drawRoundRect(
                color = woodDark,
                topLeft = Offset(unit * 6f, unit * 17.5f),
                size = Size(unit * 13f, unit * 6f),
                cornerRadius = CornerRadius(unit * 1f)
            )
            drawRoundRect(
                color = if (night) Color(0xFFB47C8F) else Color(0xFFFFB7A7),
                topLeft = Offset(unit * 7f, unit * 18.4f),
                size = Size(unit * 11f, unit * 4.4f),
                cornerRadius = CornerRadius(unit * 0.9f)
            )
            drawRoundRect(
                color = if (night) Color(0xFF8B5D79) else Color(0xFFE88686),
                topLeft = Offset(unit * 12f, unit * 20.5f),
                size = Size(unit * 5f, unit * 2f),
                cornerRadius = CornerRadius(unit * 0.55f)
            )

            // Front ledge and a tiny doorway hint make the room feel boxed in.
            drawRect(color = trim, topLeft = Offset(0f, unit * 30.5f), size = Size(size.width, unit * 1.5f))
            drawRoundRect(
                color = woodDark.copy(alpha = 0.65f),
                topLeft = Offset(unit * 28.2f, unit * 22f),
                size = Size(unit * 2.4f, unit * 8.5f),
                cornerRadius = CornerRadius(unit * 0.65f)
            )
        }

        if (petInside) {
            CurioPetSprite(
                stage = CurioPet.currentStage(),
                mood = mood,
                spriteSize = homeSize * 0.52f * CurioPet.currentStage().sizeScale,
                celebrateKey = celebrateKey,
                contentDescription = if (sleeping) "Curie asleep in its home. Tap to wake" else null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = homeSize * 0.22f, bottom = homeSize * 0.25f)
            )
        }

        if (sleeping) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = homeSize * 0.18f, end = homeSize * 0.16f)
                    .size(homeSize * 0.16f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.Star,
                    contentDescription = null,
                    tint = accent,
                    size = homeSize * 0.09f
                )
            }
        }
    }
}

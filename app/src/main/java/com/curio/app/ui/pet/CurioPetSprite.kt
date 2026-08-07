package com.curio.app.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioPet
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * The Curio pet sprite — a tiny pixelated "spark-spirit" rendered entirely
 * in Compose (spec §10.2): small round cream body, big eyes, a gold
 * star-tipped antenna, and a scarf in the active category accent.
 *
 * No bitmap assets: the body is a 16×16 pixel grid drawn as rects on a
 * Canvas, so it is crisp at any size and theme-aware (the ink flips to a
 * light twin in dark mode). The face (eyes + mouth + cheeks) is drawn as
 * overlays so moods and the blink cycle swap without re-laying the body.
 * Growth accessories (spec §10.4) are stage-gated: a sprout leaf, a
 * satchel, a tiny book, an accent aura, a gold halo.
 *
 * Animations (spec §10.5): idle bob + periodic blink, a one-shot
 * celebration hop ([celebrateKey] increments to trigger), sleep breathing
 * with floating Z's, and an excited wiggle with sparkle eyes.
 */
@Composable
fun CurioPetSprite(
    stage: CurioPet.Stage,
    mood: CurioPet.Mood,
    accent: Color,
    modifier: Modifier = Modifier,
    // NOTE: named spriteSize (not `size`) — this function draws the sprite
    // on a Canvas, and a parameter named `size` would shadow DrawScope.size
    // inside the draw blocks (project compile-safety rule).
    spriteSize: Dp = 96.dp,
    celebrateKey: Int = 0,
    contentDescription: String? = null
) {
    val density = LocalDensity.current
    val dark = isCurioDarkTheme()
    val ink = if (dark) Color(0xFFEFE2D0) else Color(0xFF3B2E26)
    val body = Color(0xFFFFF3DC)
    val bodyShade = if (dark) Color(0xFFF0E0C2) else Color(0xFFF3E3C4)
    val blush = Color(0xFFF7AFAF)
    val gold = Color(0xFFFFD97D)
    val goldDeep = Color(0xFFE0B050)
    val leaf = Color(0xFF9CCB8B)
    val bookCover = Color(0xFFD98BA0)
    val white = Color.White

    // ── Animation state ────────────────────────────────────────────────
    val idle = rememberInfiniteTransition(label = "petIdle")
    val bobPhase by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "petBob"
    )
    val blinkPhase by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3800), RepeatMode.Restart),
        label = "petBlink"
    )
    val breathePhase by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "petBreathe"
    )
    // One-shot celebration hop — keyed so the Quests/Home screens can fire
    // it on quest claims and level-ups.
    val hop = remember { Animatable(0f) }
    LaunchedEffect(celebrateKey) {
        if (celebrateKey > 0) {
            hop.snapTo(0f)
            hop.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            hop.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 420f))
        }
    }

    val sleeping = mood == CurioPet.Mood.SLEEPY
    val excited = mood == CurioPet.Mood.EXCITED
    val proud = mood == CurioPet.Mood.PROUD

    // Body motion: idle bob (shallow when sleeping), celebration hop, and an
    // excited wiggle. Sleep adds a slow breathing scale.
    val bobWave = sin(bobPhase * 2f * PI.toFloat())
    val bobDp = bobWave * if (sleeping) 1.2f else 2.2f
    val breatheScale = 1f + (if (sleeping) 0.02f else 0f) * sin(breathePhase * 2f * PI.toFloat())
    val hopJump = if (hop.value > 0f) -hop.value * 10.dp * (1f - hop.value * 0.35f) else 0.dp
    val hopSquash = sin(hop.value * PI.toFloat())
    val wiggle = if (excited) sin(bobPhase * 6f * PI.toFloat()) * 3f else 0f

    // ── Face state ─────────────────────────────────────────────────────
    val eyes = when {
        sleeping -> EyeStyle.CLOSED
        blinkPhase > 0.93f && !excited && !proud -> EyeStyle.BLINK
        excited -> EyeStyle.STAR
        proud -> EyeStyle.HAPPY
        else -> EyeStyle.OPEN
    }
    val mouth = when {
        sleeping -> MouthStyle.NONE
        excited -> MouthStyle.WIDE
        else -> MouthStyle.SMILE
    }

    val desc = contentDescription
    Box(
        modifier = modifier
            .size(spriteSize)
            .then(if (desc != null) Modifier.semantics { this.contentDescription = desc } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // One motion layer carries the aura, the bob/hop/wiggle and the
        // breathing scale so the glow always moves with the sprite.
        val auraOn = stage == CurioPet.Stage.LANE_GUARDIAN || stage == CurioPet.Stage.SAGE
        val auraColor = if (stage == CurioPet.Stage.SAGE) gold else accent
        Box(
            modifier = Modifier
                .size(spriteSize * 0.92f)
                .graphicsLayer {
                    translationY = with(density) { bobDp.dp.toPx() } + with(density) { hopJump.toPx() }
                    val squash = hopSquash * 0.06f * (if (hop.value > 0f) 1f else 0f)
                    scaleX = (breatheScale + squash).coerceAtLeast(0.5f)
                    scaleY = (breatheScale - squash).coerceAtLeast(0.5f)
                    rotationZ = wiggle
                }
                .then(
                    if (auraOn) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = auraColor.copy(
                                    alpha = 0.16f + 0.05f * sin(breathePhase * 2f * PI.toFloat())
                                ),
                                cornerRadius = CornerRadius(size.width * 0.24f)
                            )
                        }
                    } else Modifier
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val px = size.width / GRID
                fun drawPx(col: Int, row: Int, color: Color, alpha: Float = 1f) {
                    if (col !in 0 until GRID || row !in 0 until GRID) return
                    drawRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(col * px, row * px),
                        size = Size(px + 0.02f, px + 0.02f)
                    )
                }

                // Body — static pattern with the accent scarf.
                BODY_ROWS.forEachIndexed { row, line ->
                    line.forEachIndexed { col, ch ->
                        when (ch) {
                            'b' -> drawPx(col, row, body)
                            'B' -> drawPx(col, row, bodyShade)
                            'o' -> drawPx(col, row, ink)
                            's' -> drawPx(col, row, accent)
                            'S' -> drawPx(col, row, shade(accent, dark))
                            'G' -> drawPx(col, row, gold)
                            'g' -> drawPx(col, row, goldDeep)
                        }
                    }
                }

                // Face overlays.
                when (eyes) {
                    EyeStyle.OPEN -> {
                        drawPx(4, 9, ink); drawPx(5, 9, ink)
                        drawPx(4, 10, ink); drawPx(5, 10, ink)
                        drawPx(10, 9, ink); drawPx(11, 9, ink)
                        drawPx(10, 10, ink); drawPx(11, 10, ink)
                        drawPx(4, 9, white); drawPx(10, 9, white)
                    }
                    EyeStyle.BLINK -> {
                        drawPx(4, 9, ink); drawPx(5, 9, ink)
                        drawPx(10, 9, ink); drawPx(11, 9, ink)
                    }
                    EyeStyle.CLOSED -> {
                        drawPx(4, 10, ink); drawPx(5, 10, ink)
                        drawPx(10, 10, ink); drawPx(11, 10, ink)
                    }
                    EyeStyle.STAR -> {
                        // Gold sparkle eyes with cross arms.
                        drawPx(4, 9, gold); drawPx(5, 9, gold)
                        drawPx(4, 10, gold); drawPx(5, 10, gold)
                        drawPx(10, 9, gold); drawPx(11, 9, gold)
                        drawPx(10, 10, gold); drawPx(11, 10, gold)
                        drawPx(3, 9, gold); drawPx(6, 9, gold)
                        drawPx(4, 8, gold); drawPx(5, 8, gold)
                        drawPx(9, 9, gold); drawPx(12, 9, gold)
                        drawPx(10, 8, gold); drawPx(11, 8, gold)
                    }
                    EyeStyle.HAPPY -> {
                        drawPx(4, 10, ink); drawPx(5, 9, ink); drawPx(5, 10, ink)
                        drawPx(10, 9, ink); drawPx(10, 10, ink); drawPx(11, 10, ink)
                    }
                }

                // Cheeks.
                drawPx(2, 10, blush, 0.55f)
                drawPx(3, 10, blush, 0.55f)
                drawPx(12, 10, blush, 0.55f)
                drawPx(13, 10, blush, 0.55f)

                // Mouth.
                when (mouth) {
                    MouthStyle.SMILE -> {
                        drawPx(6, 11, ink); drawPx(9, 11, ink)
                        drawPx(7, 10, ink); drawPx(8, 10, ink)
                    }
                    MouthStyle.WIDE -> {
                        drawPx(6, 11, ink); drawPx(7, 11, ink)
                        drawPx(8, 11, ink); drawPx(9, 11, ink)
                    }
                    MouthStyle.NONE -> Unit
                }

                // Growth accessories (spec §10.4).
                when (stage) {
                    CurioPet.Stage.SPROUT -> {
                        drawPx(5, 3, leaf); drawPx(6, 2, leaf); drawPx(6, 3, leaf)
                    }
                    CurioPet.Stage.TRAIL_BUDDY -> {
                        // Satchel on the right hip.
                        drawPx(13, 10, ink)
                        drawPx(14, 11, accent); drawPx(15, 11, accent)
                        drawPx(14, 12, accent); drawPx(15, 12, accent)
                        drawPx(14, 12, shade(accent, dark))
                    }
                    CurioPet.Stage.ARCHIVE_PAL -> {
                        // Tiny book at the bottom right.
                        drawPx(12, 13, bookCover); drawPx(13, 13, bookCover)
                        drawPx(12, 14, bookCover); drawPx(13, 14, bookCover)
                        drawPx(14, 13, white); drawPx(14, 14, white)
                    }
                    CurioPet.Stage.SAGE -> {
                        // Gold halo above the antenna star.
                        drawPx(5, 0, gold); drawPx(9, 0, gold)
                        drawPx(7, 1, goldDeep)
                    }
                    else -> Unit
                }

                // Sleep Z's — two tiny z shapes drifting up-right.
                if (sleeping) {
                    val drift = ((breathePhase * 2f) % 1f).toInt()
                    drawPx(13, 4 - drift, ink, 0.7f); drawPx(14, 5 - drift, ink, 0.7f); drawPx(13, 5 - drift, ink, 0.7f)
                    drawPx(14, 6 - drift, ink, 0.5f); drawPx(15, 7 - drift, ink, 0.5f); drawPx(14, 7 - drift, ink, 0.5f)
                }

                // Excited sparkles around the pet.
                if (excited) {
                    val twinkle = sin(bobPhase * 8f * PI.toFloat()) * 0.5f + 0.5f
                    drawPx(1, 2, gold, twinkle * 0.9f)
                    drawPx(14, 3, gold, (1f - twinkle) * 0.9f)
                    drawPx(2, 13, gold, twinkle * 0.8f)
                    drawPx(13, 2, gold, (1f - twinkle) * 0.8f)
                }
            }
        }
    }
}

/** Darkens a scarf accent for shading — theme-aware (light twin in dark). */
private fun shade(color: Color, dark: Boolean): Color =
    if (dark) androidx.compose.ui.graphics.lerp(color, Color.White, 0.35f)
    else androidx.compose.ui.graphics.lerp(color, Color.Black, 0.25f)

private enum class EyeStyle { OPEN, BLINK, CLOSED, STAR, HAPPY }
private enum class MouthStyle { SMILE, WIDE, NONE }

private const val GRID = 16

/**
 * The pet's body — a round cream blob with a gold star-tipped antenna and a
 * category-accent scarf. Keys: '.' empty, 'b' body, 'B' body shade,
 * 'o' ink outline, 's' scarf accent, 'S' scarf shade, 'G' gold, 'g' gold deep.
 */
private val BODY_ROWS: List<String> = listOf(
    "......GG........",
    "......GG........",
    ".......b.......",
    ".......o.......",
    "...oooooooo....",
    "..obbbbbbbbo...",
    ".obbbbbbbbbbo..",
    "obbbbbbbbbbbbo.",
    "obbbbbbbbbbbbo.",
    "obbbbbbbbbbbbo.",
    "obbbbbbbbbbbbo.",
    "obbbbbbbbbbbbo.",
    ".obbbbbbbbbbo..",
    "..osssssssssso..",
    "....oSSssSSo....",
    "......osso......"
)

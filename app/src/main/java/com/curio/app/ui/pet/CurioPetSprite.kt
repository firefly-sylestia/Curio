package com.curio.app.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
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
import kotlin.math.PI
import kotlin.math.sin

/**
 * The Curio pet sprite — a tiny pixelated "spark-spirit" rendered entirely
 * in Compose (spec §10.2): a round cream body, big eyes, a gold star-tipped
 * antenna, little ear nubs, feet, a waggy tail, and a scarf in the active
 * category accent.
 *
 * No bitmap assets: the body is a 16×16 pixel grid drawn as rects on a
 * Canvas, so it is crisp at any size. v8.8 — ONE fixed look in every theme
 * (cream body + warm ink outline); it no longer flips to a light twin in
 * dark mode. The face (eyes + mouth + cheeks) is drawn as overlays so moods
 * and the blink cycle swap without re-laying the body. Growth accessories
 * (spec §10.4) are stage-gated: a sprout leaf, a satchel, a tiny book, an
 * accent aura, a gold halo.
 *
 * Animations: idle bob + periodic blink, a fast walk bob with a lean into
 * the direction of travel, a one-shot celebration hop ([celebrateKey]), a
 * one-shot touch squish ([squishKey]), a stretched "lifted" pose while being
 * dragged, sleep breathing with floating Z's, an excited wiggle with
 * sparkle eyes, and a curious head tilt. [facing] mirrors the sprite (1 =
 * facing right, -1 = facing left).
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
    squishKey: Int = 0,
    moving: Boolean = false,
    dragged: Boolean = false,
    facing: Float = 1f,
    contentDescription: String? = null
) {
    val density = LocalDensity.current
    // v8.8 — fixed one-look palette: warm cream + ink on every theme.
    val ink = Color(0xFF4A3426)
    val body = Color(0xFFFFF3DC)
    val bodyShade = Color(0xFFF0DDBB)
    val bellyLight = Color(0xFFFFFBF0)
    val blush = Color(0xFFF7AFAF)
    val gold = Color(0xFFFFD97D)
    val goldDeep = Color(0xFFE0B050)
    val leaf = Color(0xFF9CCB8B)
    val bookCover = Color(0xFFD98BA0)
    val white = Color.White

    // ── Animation state ────────────────────────────────────────────────
    // Specs are hoisted with remember so InfiniteTransition does NOT restart
    // the loop on every recomposition (a fresh spec instance would). Walk
    // bob is quick and deep; idle bob is slow and shallow.
    val bobSpec: InfiniteRepeatableSpec<Float> = remember(moving) {
        infiniteRepeatable(tween(if (moving) 380 else 2400, easing = LinearEasing))
    }
    val blinkSpec: InfiniteRepeatableSpec<Float> = remember {
        infiniteRepeatable(tween(3800), RepeatMode.Restart)
    }
    val breatheSpec: InfiniteRepeatableSpec<Float> = remember {
        infiniteRepeatable(tween(2200, easing = LinearEasing))
    }
    val idle = rememberInfiniteTransition(label = "petIdle")
    val bobPhase by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = bobSpec,
        label = "petBob"
    )
    val blinkPhase by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = blinkSpec,
        label = "petBlink"
    )
    val breathePhase by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = breatheSpec,
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
    // One-shot touch squish — a quick happy squeeze on tap.
    val squish = remember { Animatable(1f) }
    LaunchedEffect(squishKey) {
        if (squishKey > 0) {
            squish.snapTo(1f)
            squish.animateTo(1.18f, spring(dampingRatio = 0.5f, stiffness = 620f))
            squish.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
        }
    }

    val sleeping = mood == CurioPet.Mood.SLEEPY && !moving && !dragged
    val excited = mood == CurioPet.Mood.EXCITED
    val proud = mood == CurioPet.Mood.PROUD
    val curious = mood == CurioPet.Mood.CURIOUS

    // Body motion: idle/walk bob, celebration hop, excited wiggle, walk lean
    // and curious tilt. Sleep adds a slow breathing scale.
    val bobWave = sin(bobPhase * 2f * PI.toFloat())
    val bobAmp = when {
        dragged -> 0.6f
        moving -> 3.4f
        sleeping -> 1.2f
        else -> 2.2f
    }
    val bobDp = bobWave * bobAmp
    val breatheScale = 1f + (if (sleeping) 0.02f else 0f) * sin(breathePhase * 2f * PI.toFloat())
    // Dp * Float only — this Compose version has no Float * Dp operator.
    val hopJump = if (hop.value > 0f) 10.dp * (-hop.value) * (1f - hop.value * 0.35f) else 0.dp
    val hopSquash = sin(hop.value * PI.toFloat())
    val wiggle = if (excited) sin(bobPhase * 6f * PI.toFloat()) * 3f else 0f
    // Lean into the walk direction, alternating with each step.
    val walkLean = if (moving) facing * 4f * sin(bobPhase * 2f * PI.toFloat()) else 0f
    val tilt = if (curious && !moving && !dragged) facing * 5f else 0f
    // Dragged: lifted + stretched like it's being picked up.
    val dragStretchX = if (dragged) 0.92f else 1f
    val dragLiftY = if (dragged) 1.08f else 1f

    // ── Face state ─────────────────────────────────────────────────────
    val eyes = when {
        dragged -> EyeStyle.WIDE
        sleeping -> EyeStyle.CLOSED
        blinkPhase > 0.93f && !excited && !proud -> EyeStyle.BLINK
        excited -> EyeStyle.STAR
        proud -> EyeStyle.HAPPY
        else -> EyeStyle.OPEN
    }
    val mouth = when {
        dragged -> MouthStyle.O
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
        // One motion layer carries the aura, the bob/hop/wiggle/lean and the
        // breathing + squish scales so the glow always moves with the sprite.
        val auraOn = stage == CurioPet.Stage.LANE_GUARDIAN || stage == CurioPet.Stage.SAGE
        val auraColor = if (stage == CurioPet.Stage.SAGE) gold else accent
        Box(
            modifier = Modifier
                .size(spriteSize * 0.92f)
                .graphicsLayer {
                    translationY = with(density) { bobDp.dp.toPx() } + with(density) { hopJump.toPx() }
                    val squash = hopSquash * 0.06f * (if (hop.value > 0f) 1f else 0f)
                    val squishScale = squish.value
                    scaleX = ((breatheScale + squash) * dragStretchX * squishScale).coerceAtLeast(0.4f)
                    scaleY = ((breatheScale - squash) * dragLiftY * squishScale).coerceAtLeast(0.4f)
                    rotationZ = wiggle + walkLean + tilt
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
            // Flip layer — mirrors the sprite horizontally so it faces the
            // way it walks without touching the motion transforms above.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = facing }
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
                                'S' -> drawPx(col, row, shade(accent))
                                'G' -> drawPx(col, row, gold)
                                'g' -> drawPx(col, row, goldDeep)
                            }
                        }
                    }

                    // Soft belly patch — a lighter tummy inside the blob.
                    drawRoundRect(
                        color = bellyLight.copy(alpha = 0.85f),
                        topLeft = Offset(5 * px, 9 * px),
                        size = Size(6 * px, 3 * px),
                        cornerRadius = CornerRadius(3 * px)
                    )

                    // Little tail on the sprite's right side — wags when
                    // walking or excited.
                    if (!dragged) {
                        val wag = if (moving || excited) sin(bobPhase * 10f * PI.toFloat()) else 0f
                        drawPx(14, 11, bodyShade)
                        drawPx(15, 11, bodyShade)
                        drawPx(15, 12, bodyShade)
                        if (wag > 0.4f) drawPx(15, 10, bodyShade)
                    }

                    // Face overlays.
                    when (eyes) {
                        EyeStyle.OPEN -> {
                            drawPx(4, 8, ink); drawPx(5, 8, ink)
                            drawPx(4, 9, ink); drawPx(5, 9, ink)
                            drawPx(10, 8, ink); drawPx(11, 8, ink)
                            drawPx(10, 9, ink); drawPx(11, 9, ink)
                            drawPx(4, 8, white); drawPx(10, 8, white)
                        }
                        EyeStyle.BLINK -> {
                            drawPx(4, 8, ink); drawPx(5, 8, ink)
                            drawPx(10, 8, ink); drawPx(11, 8, ink)
                        }
                        EyeStyle.CLOSED -> {
                            drawPx(4, 9, ink); drawPx(5, 9, ink)
                            drawPx(10, 9, ink); drawPx(11, 9, ink)
                        }
                        EyeStyle.WIDE -> {
                            // Big startled eyes while lifted.
                            drawPx(4, 7, ink); drawPx(5, 7, ink)
                            drawPx(4, 8, ink); drawPx(5, 8, ink)
                            drawPx(4, 9, ink); drawPx(5, 9, ink)
                            drawPx(10, 7, ink); drawPx(11, 7, ink)
                            drawPx(10, 8, ink); drawPx(11, 8, ink)
                            drawPx(10, 9, ink); drawPx(11, 9, ink)
                            drawPx(4, 8, white); drawPx(10, 8, white)
                        }
                        EyeStyle.STAR -> {
                            // Gold sparkle eyes with cross arms.
                            drawPx(4, 8, gold); drawPx(5, 8, gold)
                            drawPx(4, 9, gold); drawPx(5, 9, gold)
                            drawPx(10, 8, gold); drawPx(11, 8, gold)
                            drawPx(10, 9, gold); drawPx(11, 9, gold)
                            drawPx(3, 8, gold); drawPx(6, 8, gold)
                            drawPx(4, 7, gold); drawPx(5, 7, gold)
                            drawPx(9, 8, gold); drawPx(12, 8, gold)
                            drawPx(10, 7, gold); drawPx(11, 7, gold)
                        }
                        EyeStyle.HAPPY -> {
                            drawPx(4, 9, ink); drawPx(5, 8, ink); drawPx(5, 9, ink)
                            drawPx(10, 9, ink); drawPx(10, 8, ink); drawPx(11, 9, ink)
                        }
                    }

                    // Cheeks.
                    drawPx(2, 9, blush, 0.55f)
                    drawPx(3, 9, blush, 0.55f)
                    drawPx(12, 9, blush, 0.55f)
                    drawPx(13, 9, blush, 0.55f)
                    drawPx(2, 10, blush, 0.4f)
                    drawPx(13, 10, blush, 0.4f)

                    // Mouth.
                    when (mouth) {
                        MouthStyle.SMILE -> {
                            drawPx(6, 11, ink); drawPx(9, 11, ink)
                            drawPx(7, 10, ink); drawPx(8, 10, ink)
                        }
                        MouthStyle.WIDE -> {
                            drawPx(6, 11, ink); drawPx(7, 11, ink)
                            drawPx(8, 11, ink); drawPx(9, 11, ink)
                            drawPx(7, 10, ink); drawPx(8, 10, ink)
                        }
                        MouthStyle.O -> {
                            drawPx(7, 10, ink); drawPx(8, 10, ink)
                            drawPx(7, 11, ink); drawPx(8, 11, ink)
                        }
                        MouthStyle.NONE -> Unit
                    }

                    // Growth accessories (spec §10.4).
                    when (stage) {
                        CurioPet.Stage.SPROUT -> {
                            drawPx(4, 2, leaf); drawPx(5, 1, leaf); drawPx(5, 2, leaf); drawPx(5, 3, leaf)
                        }
                        CurioPet.Stage.TRAIL_BUDDY -> {
                            // Satchel on the right hip.
                            drawPx(13, 10, ink); drawPx(14, 10, accent); drawPx(15, 10, accent)
                            drawPx(13, 11, ink); drawPx(14, 11, accent); drawPx(15, 11, accent)
                        }
                        CurioPet.Stage.ARCHIVE_PAL -> {
                            // Tiny book beside the right foot.
                            drawPx(14, 13, bookCover); drawPx(15, 13, bookCover)
                            drawPx(14, 14, bookCover); drawPx(15, 14, bookCover)
                            drawPx(15, 13, white); drawPx(15, 14, white)
                        }
                        CurioPet.Stage.SAGE -> {
                            // Gold halo above the antenna star.
                            drawPx(4, 0, gold); drawPx(8, 0, gold)
                            drawPx(6, 1, goldDeep)
                        }
                        else -> Unit
                    }

                    // Sleep Z's — two tiny z shapes drifting up-right.
                    if (sleeping) {
                        val drift = ((breathePhase * 2f) % 1f).toInt()
                        drawPx(13, 2 - drift, ink, 0.7f); drawPx(14, 3 - drift, ink, 0.7f); drawPx(13, 3 - drift, ink, 0.7f)
                        drawPx(14, 4 - drift, ink, 0.5f); drawPx(15, 5 - drift, ink, 0.5f); drawPx(14, 5 - drift, ink, 0.5f)
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
}

/** Darkens a scarf accent for shading — one fixed shade on every theme. */
private fun shade(color: Color): Color =
    androidx.compose.ui.graphics.lerp(color, Color.Black, 0.25f)

private enum class EyeStyle { OPEN, BLINK, CLOSED, WIDE, STAR, HAPPY }
private enum class MouthStyle { SMILE, WIDE, O, NONE }

private const val GRID = 16

/**
 * The pet's body — a round cream blob with ear nubs, a gold star-tipped
 * antenna, feet, and a category-accent scarf. Keys: '.' empty, 'b' body,
 * 'B' body shade, 'o' ink outline, 's' scarf accent, 'S' scarf shade,
 * 'G' gold, 'g' gold deep.
 */
private val BODY_ROWS: List<String> = listOf(
    ".......GG.......",
    ".......GG.......",
    "..o...........o.",
    "..ob....o....bo.",
    "...oooooooooo...",
    "..obbbbbbbbbbo..",
    ".obbbbbbbbbbbbo.",
    "obbbbbbbbbbbbbbo",
    "obbbbbbbbbbbbbbo",
    "obbbbbbbbbbbbbbo",
    "obbbbbbbbbbbbbbo",
    ".obbbbbbbbbbbbo.",
    "..osssssssssso..",
    "...oSSssssSSo...",
    "..oo........oo..",
    "..oo........oo.."
)

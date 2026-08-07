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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioPet
import com.curio.app.ui.theme.CurioColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

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
 *
 * v8.10 — ONE fixed color on every theme: the scarf and aura always wear
 * the Curio light-theme brand coral ([CurioColors.CategoryCoral]); they no
 * longer react to the category pastel accents or to dark mode.
 */
@Composable
fun CurioPetSprite(
    stage: CurioPet.Stage,
    mood: CurioPet.Mood,
    modifier: Modifier = Modifier,
    // NOTE: named spriteSize (not `size`) — this function draws the sprite
    // on a Canvas, and a parameter named `size` would shadow DrawScope.size
    // inside the draw blocks (project compile-safety rule).
    spriteSize: Dp = 96.dp,
    celebrateKey: Int = 0,
    squishKey: Int = 0,
    playKey: Int = 0,
    spinKey: Int = 0,
    moving: Boolean = false,
    dragged: Boolean = false,
    facing: Float = 1f,
    thinking: Boolean = false,
    watching: Boolean = false,
    spinning: Boolean = false,
    /** v8.15 — the guided-tour pose: a raised paw pointing "here!". */
    pointing: Boolean = false,
    contentDescription: String? = null
) {
    val density = LocalDensity.current
    // v8.8 — fixed one-look palette: warm cream + ink on every theme.
    // v8.10 — the scarf/aura accent is hardcoded to the Curio light-theme
    // brand coral: one theme, one color, on every device (never the
    // category pastel, never a dark-mode twin).
    val accent = CurioColors.CategoryCoral
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
    // v8.14 — the nightcap: soft periwinkle with a cream trim; the gold
    // antenna star doubles as the pompom.
    val cap = Color(0xFF9DB6E8)
    val capTrim = Color(0xFFFFF3DC)

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
    val glanceSpec: InfiniteRepeatableSpec<Float> = remember {
        infiniteRepeatable(tween(7000), RepeatMode.Restart)
    }
    val flickSpec: InfiniteRepeatableSpec<Float> = remember {
        infiniteRepeatable(tween(5200), RepeatMode.Restart)
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
    // Slow glance — the eyes drift to one side for a moment every ~7s.
    val glancePhase by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = glanceSpec,
        label = "petGlance"
    )
    // Ear flick — a quick ear perk every ~5s.
    val flickPhase by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = flickSpec,
        label = "petFlick"
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
    // One-shot play bow (playKey, v8.11) — dips down, then pops up with a
    // little bounce: the "let's play!" invitation pose.
    val playBow = remember { Animatable(0f) }
    LaunchedEffect(playKey) {
        if (playKey > 0) {
            playBow.snapTo(0f)
            playBow.animateTo(1f, tween(210, easing = FastOutSlowInEasing))
            playBow.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 430f))
        }
    }
    // One-shot playful spin (spinKey, v8.11) — a full 360° twirl, like a
    // happy dog chasing its tail when the taps escalate to zoomies.
    val spinAngle = remember { Animatable(0f) }
    LaunchedEffect(spinKey) {
        if (spinKey > 0) {
            spinAngle.snapTo(0f)
            spinAngle.animateTo(360f, tween(540, easing = FastOutSlowInEasing))
            spinAngle.snapTo(0f)
        }
    }

    val sleeping = mood == CurioPet.Mood.SLEEPY && !moving && !dragged
    // v8.13 — mood faces for the new status moods + the spin cheer.
    val happy = mood == CurioPet.Mood.HAPPY
    val bouncy = mood == CurioPet.Mood.BOUNCY
    val spinningNow = spinning && !sleeping && !dragged && !moving
    // v8.12 — excited/proud are ONE-SHOT bursts tied to the celebration hop,
    // never sustained ambient moods: the pet reacts for a beat, then idles.
    // (The EXCITED/PROUD mood windows used to keep the wiggle + star eyes
    // for 60-90s, so the pet looked stuck reacting until the next level-up.)
    val celebrating = hop.value > 0f
    val excited = celebrating
    val proud = celebrating
    val curious = mood == CurioPet.Mood.CURIOUS
    // v8.11 — mid-play: the pet is bowing or twirling, so it wears its
    // excited face and wags faster regardless of the ambient mood.
    val playing = playBow.value > 0f || spinAngle.value != 0f
    // v8.13 — blush is a CELEBRATION thing, not a permanent feature: only
    // genuinely happy moments wear it (excited/proud one-shots, bouncy
    // post-play, mid-play, mid-spin). Plain idle HAPPY is the default state,
    // so it does NOT blush there — the face stays clean most of the time.
    // (Declared here, after excited/proud/playing — a compile-safety rule:
    // never reference vals before their declaration point.)
    val blushing = !sleeping && (excited || proud || bouncy || playing || spinningNow)
    // v8.14 — the rare sleep-STARTLE: a tiny jump with eyes flashing open,
    // then it settles right back (the "almost woke up" moment). Runs only
    // while asleep, on a random 9-22s beat.
    val startle = remember { Animatable(0f) }
    LaunchedEffect(sleeping) {
        if (!sleeping) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(9_000, 22_000))
            if (!sleeping) return@LaunchedEffect
            startle.snapTo(0f)
            startle.animateTo(1f, tween(90, easing = LinearEasing))
            startle.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 480f))
        }
    }
    val startling = startle.value > 0.35f
    val bowPhase = playBow.value * PI.toFloat()
    // v8.13 — gentler squash so the face never compresses into the scarf
    // (the old amplitudes let the eyes/mouth visually join the bottom rows).
    val bowDip = 4.dp * sin(bowPhase)          // dips down, then springs up
    val bowSquash = sin(bowPhase * 2f) * 0.045f // a little squeeze mid-dip
    val spinPulse = sin(spinAngle.value / 360f * PI.toFloat() * 6f) * 0.03f

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
    // v8.14 — more pronounced sleep breathing so the curled ball visibly swells.
    val breatheScale = 1f + (if (sleeping) 0.035f else 0f) * sin(breathePhase * 2f * PI.toFloat())
    // v8.14 — the startle flash: a little jump + squeeze before settling.
    val startleJump = if (startling) -5.dp else 0.dp
    val startleSquash = if (startling) 0.05f else 0f
    // Dp * Float only — this Compose version has no Float * Dp operator.
    val hopJump = if (hop.value > 0f) 10.dp * (-hop.value) * (1f - hop.value * 0.35f) else 0.dp
    val hopSquash = sin(hop.value * PI.toFloat())
    val wiggle = if (excited || spinningNow) sin(bobPhase * 6f * PI.toFloat()) * 3f else 0f
    // Lean into the walk direction, alternating with each step.
    val walkLean = if (moving) facing * 4f * sin(bobPhase * 2f * PI.toFloat()) else 0f
    val tilt = if (curious && !moving && !dragged && !thinking) facing * 5f else 0f
    // Dragged: lifted + stretched like it's being picked up.
    val dragStretchX = if (dragged) 0.92f else 1f
    val dragLiftY = if (dragged) 1.08f else 1f
    // Extra idle flourishes (v8.9): a slow glance, a periodic ear flick, a
    // "thinking" tilt with a little ?, and a "watching" tilt (Spin deck).
    val glanceWave = sin(glancePhase * 2f * PI.toFloat())
    val glanceShift = if (!sleeping && !dragged && !moving && !thinking) {
        when {
            glanceWave > 0.72f -> 1
            glanceWave < -0.72f -> -1
            else -> 0
        }
    } else 0
    val flickWave = sin(flickPhase * 2f * PI.toFloat())
    val flicking = flickWave > 0.92f && !sleeping && !dragged && !thinking
    val thinkingNow = thinking && !sleeping && !dragged && !moving
    val watchingNow = watching && !sleeping && !dragged && !moving
    // v8.15 — the tour guide pose: eager wide eyes + a raised pointing paw
    // on the facing side (mirrored by the flip layer).
    val pointingNow = pointing && !sleeping && !dragged && !moving
    val idleTilt = (if (thinkingNow) facing * 7f else 0f) +
        (if (watchingNow) facing * 2.5f else 0f)

    // ── Face state ─────────────────────────────────────────────────────
    val eyes = when {
        dragged -> EyeStyle.WIDE // lifted mid-play: startled wins
        playing -> EyeStyle.STAR
        sleeping -> EyeStyle.CLOSED
        pointingNow -> EyeStyle.WIDE // "over here!"
        spinningNow -> EyeStyle.STAR // cheering the reel on
        blinkPhase > 0.93f && !excited && !proud && !spinningNow -> EyeStyle.BLINK
        excited -> EyeStyle.STAR
        proud -> EyeStyle.HAPPY
        bouncy -> EyeStyle.HAPPY
        else -> EyeStyle.OPEN
    }
    val mouth = when {
        dragged -> MouthStyle.O
        playing -> MouthStyle.WIDE
        sleeping -> MouthStyle.NONE
        pointingNow -> MouthStyle.WIDE // "come on, tap it!"
        spinningNow -> MouthStyle.WIDE
        excited -> MouthStyle.WIDE
        bouncy -> MouthStyle.WIDE
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
                    translationY = with(density) { bobDp.dp.toPx() } +
                        with(density) { hopJump.toPx() } +
                        with(density) { bowDip.toPx() } +
                        with(density) { startleJump.toPx() }
                    val squash = hopSquash * 0.06f * (if (hop.value > 0f) 1f else 0f) + bowSquash
                    val squishScale = squish.value
                    scaleX = ((breatheScale + squash) * dragStretchX * squishScale *
                        (1f + spinPulse) * (1f + startleSquash))
                        .coerceAtLeast(0.4f)
                    scaleY = ((breatheScale - squash) * dragLiftY * squishScale *
                        (1f - spinPulse) * (1f - startleSquash))
                        .coerceAtLeast(0.4f)
                    rotationZ = wiggle + walkLean + tilt + idleTilt + spinAngle.value
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

                    // Body — static pattern with the accent scarf. v8.14 —
                    // asleep pets CURL UP into a cozy ball (CURLED_ROWS)
                    // instead of standing with closed eyes.
                    val bodyRows = if (sleeping) CURLED_ROWS else BODY_ROWS
                    bodyRows.forEachIndexed { row, line ->
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

                    if (sleeping) {
                        // ── Curled sleep pose (v8.14) ──────────────────────
                        // Happy closed-eye arcs on the ball.
                        drawPx(5, 4, ink); drawPx(6, 4, ink)
                        drawPx(9, 4, ink); drawPx(10, 4, ink)
                        // The rare startle flashes the eyes open for a beat.
                        if (startling) {
                            drawPx(5, 3, white); drawPx(6, 3, white)
                            drawPx(9, 3, white); drawPx(10, 3, white)
                        }
                        // Sleepy ear twitch — an occasional tiny perk.
                        if (flicking) {
                            drawPx(3, 1, ink, 0.85f)
                            drawPx(3, 2, ink, 0.6f)
                        }
                        // The nightcap — a pointed cap; the gold antenna star
                        // doubles as the pompom on top.
                        drawPx(7, 0, gold); drawPx(8, 0, gold)
                        drawPx(6, 1, cap); drawPx(7, 1, cap); drawPx(8, 1, cap); drawPx(9, 1, cap)
                        drawPx(6, 2, cap); drawPx(7, 2, cap); drawPx(8, 2, cap); drawPx(9, 2, cap)
                        drawPx(6, 3, capTrim); drawPx(7, 3, capTrim); drawPx(8, 3, capTrim); drawPx(9, 3, capTrim)
                    } else {
                        // Soft belly patch — a lighter tummy inside the blob.
                        drawRoundRect(
                            color = bellyLight.copy(alpha = 0.85f),
                            topLeft = Offset(5 * px, 9 * px),
                            size = Size(6 * px, 3 * px),
                            cornerRadius = CornerRadius(3 * px)
                        )

                        // Little tail on the sprite's right side — wags when
                        // walking, excited, or mid-play (faster while playing).
                        if (!dragged) {
                            val wagFreq = if (playing) 18f else 10f
                            val wag = if (moving || excited || playing) {
                                sin(bobPhase * wagFreq * PI.toFloat())
                            } else 0f
                            drawPx(14, 11, bodyShade)
                            drawPx(15, 11, bodyShade)
                            drawPx(15, 12, bodyShade)
                            if (wag > 0.4f) drawPx(15, 10, bodyShade)
                        }

                        // Ear flick — a tiny perk of the left ear.
                        if (flicking) {
                            drawPx(2, 1, ink)
                            drawPx(2, 0, ink)
                        }

                        // Face overlays — the eyes drift with the glance and
                        // lift while watching the Spin deck; cheeks and mouth
                        // stay put. v8.13 — the eyes sit one row HIGHER
                        // (rows 6-8 instead of 7-9) so there is a clear gap
                        // between them and the mouth — never joined.
                        translate(
                            left = glanceShift * px,
                            top = if (watchingNow) -px else 0f
                        ) {
                            when (eyes) {
                            EyeStyle.OPEN -> {
                                drawPx(4, 7, ink); drawPx(5, 7, ink)
                                drawPx(4, 8, ink); drawPx(5, 8, ink)
                                drawPx(10, 7, ink); drawPx(11, 7, ink)
                                drawPx(10, 8, ink); drawPx(11, 8, ink)
                                drawPx(4, 7, white); drawPx(10, 7, white)
                            }
                            EyeStyle.BLINK -> {
                                drawPx(4, 7, ink); drawPx(5, 7, ink)
                                drawPx(10, 7, ink); drawPx(11, 7, ink)
                            }
                            EyeStyle.CLOSED -> {
                                drawPx(4, 8, ink); drawPx(5, 8, ink)
                                drawPx(10, 8, ink); drawPx(11, 8, ink)
                            }
                            EyeStyle.WIDE -> {
                                // Big startled eyes while lifted.
                                drawPx(4, 6, ink); drawPx(5, 6, ink)
                                drawPx(4, 7, ink); drawPx(5, 7, ink)
                                drawPx(4, 8, ink); drawPx(5, 8, ink)
                                drawPx(10, 6, ink); drawPx(11, 6, ink)
                                drawPx(10, 7, ink); drawPx(11, 7, ink)
                                drawPx(10, 8, ink); drawPx(11, 8, ink)
                                drawPx(4, 7, white); drawPx(10, 7, white)
                            }
                            EyeStyle.STAR -> {
                                // Gold sparkle eyes with cross arms.
                                drawPx(4, 7, gold); drawPx(5, 7, gold)
                                drawPx(4, 8, gold); drawPx(5, 8, gold)
                                drawPx(10, 7, gold); drawPx(11, 7, gold)
                                drawPx(10, 8, gold); drawPx(11, 8, gold)
                                drawPx(3, 7, gold); drawPx(6, 7, gold)
                                drawPx(4, 6, gold); drawPx(5, 6, gold)
                                drawPx(9, 7, gold); drawPx(12, 7, gold)
                                drawPx(10, 6, gold); drawPx(11, 6, gold)
                            }
                            EyeStyle.HAPPY -> {
                                drawPx(4, 8, ink); drawPx(5, 7, ink); drawPx(5, 8, ink)
                                drawPx(10, 8, ink); drawPx(10, 7, ink); drawPx(11, 8, ink)
                            }
                            }
                        }

                        // Cheeks — only when the pet is happy/excited/proud/
                        // bouncy or mid-play/spin (v8.13: not a permanent
                        // feature, and the row-10 pair is gone).
                        if (blushing) {
                            drawPx(2, 9, blush, 0.5f)
                            drawPx(3, 9, blush, 0.5f)
                            drawPx(12, 9, blush, 0.5f)
                            drawPx(13, 9, blush, 0.5f)
                        }

                        // Mouth. v8.10 — the smile was drawn upside down (a
                        // frown); flipped: corners UP (row 10), middle DOWN
                        // (row 11) = a proper happy smile.
                        when (mouth) {
                            MouthStyle.SMILE -> {
                                drawPx(6, 10, ink); drawPx(9, 10, ink)
                                drawPx(7, 11, ink); drawPx(8, 11, ink)
                            }
                            MouthStyle.WIDE -> {
                                // Excited open smile — corners up, wide bottom.
                                drawPx(6, 10, ink); drawPx(9, 10, ink)
                                drawPx(6, 11, ink); drawPx(7, 11, ink)
                                drawPx(8, 11, ink); drawPx(9, 11, ink)
                            }
                            MouthStyle.O -> {
                                drawPx(7, 10, ink); drawPx(8, 10, ink)
                                drawPx(7, 11, ink); drawPx(8, 11, ink)
                            }
                            MouthStyle.NONE -> Unit
                        }

                        // v8.15 — the raised pointing paw: a little coral
                        // arm on the facing side that lifts a beat to say
                        // "here!". The flip layer mirrors it to the other
                        // side when the pet faces left.
                        if (pointingNow) {
                            val pawLift = if (sin(bobPhase * 4f * PI.toFloat()) > 0f) -1 else 0
                            drawPx(13, 9 + pawLift, accent)
                            drawPx(14, 9 + pawLift, accent)
                            drawPx(15, 9 + pawLift, accent)
                            drawPx(15, 10, accent)
                            // A tiny ink fingertip so the point reads.
                            drawPx(15, 9 + pawLift, ink)
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
                    }

                    // Sleep Z's — v8.14 pronounced: three z's drifting up-right
                    // above the curled ball.
                    if (sleeping) {
                        val drift = ((breathePhase * 2f) % 1f).toInt()
                        drawPx(11, 3 - drift, ink, 0.85f); drawPx(12, 4 - drift, ink, 0.85f); drawPx(11, 4 - drift, ink, 0.85f)
                        drawPx(12, 5 - drift, ink, 0.65f); drawPx(13, 6 - drift, ink, 0.65f); drawPx(12, 6 - drift, ink, 0.65f)
                        drawPx(14, 7 - drift, ink, 0.4f); drawPx(15, 8 - drift, ink, 0.4f); drawPx(14, 8 - drift, ink, 0.4f)
                    }

                    // Excited sparkles around the pet.
                    if (excited) {
                        val twinkle = sin(bobPhase * 8f * PI.toFloat()) * 0.5f + 0.5f
                        drawPx(1, 2, gold, twinkle * 0.9f)
                        drawPx(14, 3, gold, (1f - twinkle) * 0.9f)
                        drawPx(2, 13, gold, twinkle * 0.8f)
                        drawPx(13, 2, gold, (1f - twinkle) * 0.8f)
                    }

                    // A white glint twinkles on the antenna star.
                    if (sin(bobPhase * 2f * PI.toFloat()) > 0.78f) {
                        drawPx(7, 0, white, 0.9f)
                    }

                    // A tiny "?" hovers above the antenna while thinking.
                    if (thinkingNow) {
                        drawPx(9, 0, ink)
                        drawPx(8, 1, ink); drawPx(9, 1, ink)
                        drawPx(9, 2, ink)
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
    // v8.13 — cat ears: a tiny pointed ear on each side of the head (the
    // antenna star stays center). Rows 1-2; 'o' outline, 'b' body fill.
    ".......GG.......",
    "...o...GG...o...",
    "..ob........bo..",
    "..ob....o....bo.",
    "...oooooooooo...",
    ".obbbbbbbbbbbbo.",
    "obbbbbbbbbbbbbbo",
    "obbbbbbbbbbbbbbo",
    "obbbbbbbbbbbBBbo",
    "obbbbbbbbbbbBBbo",
    "obbbbbbbbbbbBBbo",
    "obbbbbbbbbbbbBBo",
    ".osssssssssssso.",
    "..oSSssssssSSo..",
    "..oo........oo..",
    "..oo........oo.."
)

/**
 * The pet CURLED UP asleep (v8.14) — a round ball with ears, the coral
 * scarf draped over it, and the tail wrapped around the front. The nightcap
 * (drawn as an overlay) covers the top center; the closed eyes + twitches
 * are overlays too. Same keys as [BODY_ROWS].
 */
private val CURLED_ROWS: List<String> = listOf(
    ".......GG.......",
    "...o...GG...o...",
    "..ob........bo..",
    "..obbbbbbbbbbbo..",
    "...obbbbbbbbbbo...",
    "..obbbbbbbbbbbbo..",
    ".obbbbbbbbbbbbbbo.",
    "obbbbbbbbbbbbbbo",
    "obssssssssssssbo",
    ".obbbbbbbbbbbbbbo.",
    "..obbbbbbbbbbbbo..",
    "...obbbbbbbbbbo...",
    ".....oBBbbbbBBo.",
    "......oooooo....",
    "................",
    "................"
)

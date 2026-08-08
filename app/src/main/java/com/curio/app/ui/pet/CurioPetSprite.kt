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
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.EyeStyle
import com.curio.app.data.MouthStyle
import com.curio.app.data.PetDesign
import com.curio.app.data.PetFace
import com.curio.app.data.PetFaceMoods
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

/** Converts an RRGGBB design color into Compose's packed ARGB representation. */
private fun petDesignColor(hex: String): Color =
    Color(android.graphics.Color.parseColor("#$hex"))

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
    /**
     * v8.21 — the pet is dizzy (being dragged around, or recovering right
     * after a drag): swirly eyes, a wobbly sway, and little whoosh marks.
     */
    dizzy: Boolean = false,
    contentDescription: String? = null,
    /**
     * v8.35 — a one-shot face override (a reaction rule's face, from the
     * reaction editor). Wins over the mood's face while set.
     */
    faceOverride: PetFace? = null,
    /**
     * v8.35 — the hide-and-peek pose: crouched with wide eyes peeking out
     * from behind a button or the screen edge.
     */
    peeking: Boolean = false,
    /**
     * v8.34 — a custom look from the Pet designer playground. When null
     * (the normal case) the sprite reads the SAVED design (if any) from
     * [AppPreferences.petDesignState]; the designer passes its working
     * copy here for a live preview. Null + no saved design = default look.
     */
    design: PetDesign? = null
) {
    val density = LocalDensity.current
    // v8.34 — resolve the active design: the explicit working copy wins;
    // otherwise the saved one (reactive — recomposes when a design is
    // saved in the playground). Parsing is cheap (16+16 rows) and cached
    // per text via remember.
    val savedText = AppPreferences.petDesignState
    val activeDesign = remember(savedText, design) {
        design ?: savedText?.let { PetDesign.DEFAULT.toParsedOr(it, PetDesign.DEFAULT) }
    } ?: PetDesign.DEFAULT
    // v8.8 — fixed one-look palette: warm cream + ink on every theme.
    // v8.10 — the scarf/aura accent is hardcoded to the Curio light-theme
    // brand coral: one theme, one color, on every device (never the
    // category pastel, never a dark-mode twin).
    // v8.34 — the active design's palette drives every body color (each key
    // falls back to the default look when the design doesn't recolor it).
    val accent = petDesignColor(activeDesign.colorOf('s'))
    val ink = petDesignColor(activeDesign.colorOf('o'))
    val bodyShade = petDesignColor(activeDesign.colorOf('B'))
    val bellyLight = Color(0xFFFFFBF0)
    // v8.35 — blush ('r') and eye ('y') colors come from the design palette,
    // so the Face editor and palette can recolor them.
    val blush = petDesignColor(activeDesign.colorOf('r'))
    val gold = petDesignColor(activeDesign.colorOf('G'))
    val goldDeep = petDesignColor(activeDesign.colorOf('g'))
    // v8.26 — excited eyes wear a NATURAL warm brown (the ink family, one
    // step lighter) instead of gold: the gold stars read orangish against
    // the cream body. Sparkles and the antenna keep the gold.
    val starEye = petDesignColor(activeDesign.colorOf('y'))
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
    // v8.21 — the dizzy sway: a fast, wobbly rock while the pet is flung
    // around or recovering, so it visibly reels.
    val dizzyWobble = if (dizzy) sin(bobPhase * 14f * PI.toFloat()) * 5f else 0f
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
    // v8.35 — faces are configurable: the base face for the ambient mood
    // comes from the design's face editor (or the built-in default), and
    // the one-shot states (celebration hop, spin cheer, mid-play) wear the
    // design's EXCITED face so a happy hop reads excited. A [faceOverride]
    // (a reaction rule's face, e.g. from petting) wins while set.
    val moodFace = faceOverride ?: activeDesign.faceFor(mood.name)
    val oneShotFace = activeDesign.faceFor(PetFaceMoods.EXCITED)
    val eyes = when {
        // v8.21 — being flung around spins the eyes first.
        dizzy -> EyeStyle.DIZZY
        dragged -> EyeStyle.WIDE // lifted mid-play: startled wins
        playing -> oneShotFace.eyes
        sleeping -> EyeStyle.CLOSED
        peeking -> EyeStyle.WIDE // peeking out from behind a button
        pointingNow -> EyeStyle.WIDE // "over here!"
        spinningNow -> oneShotFace.eyes // cheering the reel on
        blinkPhase > 0.93f && !excited && !proud && !spinningNow -> EyeStyle.BLINK
        excited -> oneShotFace.eyes
        proud -> activeDesign.faceFor(PetFaceMoods.PROUD).eyes
        else -> moodFace.eyes
    }
    val mouth = when {
        dizzy -> MouthStyle.O // "whoa…"
        dragged -> MouthStyle.O
        playing -> oneShotFace.mouth
        sleeping -> MouthStyle.NONE
        peeking -> MouthStyle.NONE // a quiet peek
        pointingNow -> MouthStyle.WIDE // "come on, tap it!"
        spinningNow -> oneShotFace.mouth
        excited -> oneShotFace.mouth
        proud -> activeDesign.faceFor(PetFaceMoods.PROUD).mouth
        else -> moodFace.mouth
    }
    // Blush + sparkles follow the same story: the ambient mood's face, with
    // the one-shots borrowing the EXCITED face's settings.
    val blushing = !sleeping && !dizzy && !dragged && when {
        excited || proud || playing || spinningNow -> oneShotFace.blush
        else -> moodFace.blush
    }
    val sparklesOn = when {
        excited || spinningNow -> oneShotFace.sparkles
        else -> moodFace.sparkles
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
                    val squash = hopSquash * 0.06f * (if (hop.value > 0f) 1f else 0f) + bowSquash +
                        (if (peeking) -0.10f else 0f) // v8.35 — a crouched hide-and-peek
                    val squishScale = squish.value
                    scaleX = ((breatheScale + squash) * dragStretchX * squishScale *
                        (1f + spinPulse) * (1f + startleSquash))
                        .coerceAtLeast(0.4f)
                    scaleY = ((breatheScale - squash) * dragLiftY * squishScale *
                        (1f - spinPulse) * (1f - startleSquash))
                        .coerceAtLeast(0.4f)
                    rotationZ = wiggle + walkLean + tilt + idleTilt + spinAngle.value + dizzyWobble
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
                    // v8.35 — the canvas adapts to the design's grid size.
                    // Design cells (drawGridPx) use one cell = px; the
                    // procedural face art (drawPx) is authored in a 16-grid
                    // space and scaled to the canvas (opx), so the face
                    // keeps its proportions on any grid.
                    val grid = activeDesign.gridSize
                    val px = size.width / grid
                    val opx = size.width / 16f
                    fun drawGridPx(col: Int, row: Int, color: Color, alpha: Float = 1f) {
                        if (col !in 0 until grid || row !in 0 until grid) return
                        // v8.21 — softer pixels: each cell is a slightly-
                        // ROUNDED, slightly-overlapping square so the sprite
                        // reads soft and plush instead of crunchy. The 6%
                        // overlap hides the seams between rounded corners.
                        drawRoundRect(
                            color = color.copy(alpha = alpha),
                            topLeft = Offset(col * px, row * px),
                            size = Size(px * 1.06f, px * 1.06f),
                            cornerRadius = CornerRadius(px * 0.16f)
                        )
                    }
                    // Procedural overlay cell — the 16-grid authoring space.
                    // NOTE: Int params — Kotlin has no implicit Int→Float
                    // conversion, and the face art passes integer coords.
                    // (Int * opx yields Float, so scaling works.)
                    fun drawPx(col: Int, row: Int, color: Color, alpha: Float = 1f) {
                        drawRoundRect(
                            color = color.copy(alpha = alpha),
                            topLeft = Offset(col * opx, row * opx),
                            size = Size(opx * 1.06f, opx * 1.06f),
                            cornerRadius = CornerRadius(opx * 0.16f)
                        )
                    }

                    // Body — static pattern with the accent scarf. v8.14 —
                    // asleep pets CURL UP into a cozy ball instead of
                    // standing with closed eyes. v8.34 — the active design's
                    // grids replace the default ones. v8.35 — every palette
                    // key renders (custom paint slots included).
                    val bodyRows = if (sleeping) activeDesign.curledRows else activeDesign.bodyRows
                    bodyRows.forEachIndexed { row, line ->
                        line.forEachIndexed { col, ch ->
                            val hex = activeDesign.colorFor(ch)
                            if (hex != null) drawGridPx(col, row, petDesignColor(hex))
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
                            topLeft = Offset(5 * opx, 9 * opx),
                            size = Size(6 * opx, 3 * opx),
                            cornerRadius = CornerRadius(3 * opx)
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
                            left = glanceShift * opx,
                            top = if (watchingNow) -opx else 0f
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
                                // v8.26 — natural warm-brown sparkle eyes
                                // with a white glint (the old gold read
                                // orangish on the cream body). Each eye is
                                // a 4×3 star with a highlight where the
                                // light catches.
                                drawPx(4, 6, starEye); drawPx(5, 6, starEye)
                                drawPx(3, 7, starEye); drawPx(4, 7, starEye); drawPx(5, 7, starEye); drawPx(6, 7, starEye)
                                drawPx(4, 8, starEye); drawPx(5, 8, starEye)
                                drawPx(4, 7, white)
                                drawPx(10, 6, starEye); drawPx(11, 6, starEye)
                                drawPx(9, 7, starEye); drawPx(10, 7, starEye); drawPx(11, 7, starEye); drawPx(12, 7, starEye)
                                drawPx(10, 8, starEye); drawPx(11, 8, starEye)
                                drawPx(10, 7, white)
                            }
                            EyeStyle.DIZZY -> {
                                // v8.21 — dizzy pinwheel swirls: a tall
                                // core with side arms, and glints that
                                // alternate diagonally so it reads as
                                // spinning. Drawn while the pet is flung
                                // around (dragged) or recovering.
                                drawPx(4, 6, ink); drawPx(5, 6, ink)
                                drawPx(4, 7, ink); drawPx(5, 7, ink)
                                drawPx(4, 8, ink); drawPx(5, 8, ink)
                                drawPx(3, 7, ink); drawPx(6, 7, ink)
                                drawPx(4, 7, white); drawPx(5, 6, white)
                                drawPx(10, 6, ink); drawPx(11, 6, ink)
                                drawPx(10, 7, ink); drawPx(11, 7, ink)
                                drawPx(10, 8, ink); drawPx(11, 8, ink)
                                drawPx(9, 7, ink); drawPx(12, 7, ink)
                                drawPx(10, 7, white); drawPx(11, 6, white)
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

                    // Excited sparkles around the pet (v8.35 — toggleable in
                    // the Face/Excitement editor via the design's face).
                    if (sparklesOn) {
                        val twinkle = sin(bobPhase * 8f * PI.toFloat()) * 0.5f + 0.5f
                        drawPx(1, 2, gold, twinkle * 0.9f)
                        drawPx(14, 3, gold, (1f - twinkle) * 0.9f)
                        drawPx(2, 13, gold, twinkle * 0.8f)
                        drawPx(13, 2, gold, (1f - twinkle) * 0.8f)
                    }

                    // v8.21 — while dizzy, little whoosh marks trail beside
                    // the pet so the spin feels like it's actually moving.
                    if (dizzy) {
                        val whoosh = sin(bobPhase * 10f * PI.toFloat())
                        val a = 0.45f + whoosh * 0.25f
                        drawPx(1, 6, ink, a); drawPx(0, 7, ink, a * 0.8f); drawPx(1, 8, ink, a)
                        drawPx(14, 6, ink, a); drawPx(15, 7, ink, a * 0.8f); drawPx(14, 8, ink, a)
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

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.DetailTransform
import com.curio.app.data.EyeStyle
import com.curio.app.data.PetViewAngle
import com.curio.app.data.MouthStyle
import com.curio.app.data.PetDesign
import com.curio.app.data.PetFace
import com.curio.app.data.PetFaceMoods
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * v27t — global pointer awareness for the pet's eyes: the NavHost root
 * feeds the window pointer position here, and every [CurioPetSprite]
 * reads it to aim its eyes at the cursor (Chromebook / desktop) and at
 * the point you tap/click. Scrolling deliberately does NOT move the eyes.
 * One tracker serves all pets (floating, flower bed, quests).
 */
object PetPointer {
    /** Last hover/press position in window coordinates (Unspecified = none). */
    var position by mutableStateOf(Offset.Unspecified)
    /** The current press point, or null when nothing is held. */
    var press by mutableStateOf<Offset?>(null)
    /**
     * v27v — increments on EVERY pointer event (hover, press, drag, scroll,
     * release). Sprites key their 2-second look-timeout on it: while events
     * keep arriving the eyes stay aimed, and ~2s after the last one they
     * ease back to the neutral glance.
     */
    var activityTick by mutableStateOf(0)
        private set

    /**
     * Attach to the app root: tracks hover and press so the pet's eyes can
     * follow the pointer everywhere (not just over the pet). Scrolling does
     * NOT move the eyes — the pet only looks at where you tap/click:
     * desktop hover aims the eyes at the cursor, a real tap aims at the tap
     * point, but once the finger STARTS DRAGGING (a touch-scroll) the aim is
     * cancelled so the eyes go neutral instead of following the scroll.
     */
    @Composable
    fun trackerModifier(): Modifier = Modifier.pointerInput(Unit) {
        var pressStart: Offset? = null
        var dragging = false
        awaitPointerEventScope {
            while (true) {
                // NOTE: use the EVENT's type (PointerEventType) for
                // scroll/press/release — PointerInputChange.type is a
                // different enum (PointerType: touch/mouse/stylus).
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                activityTick++
                when (event.type) {
                    PointerEventType.Press -> {
                        press = change.position
                        pressStart = change.position
                        dragging = false
                        position = change.position
                    }
                    PointerEventType.Move -> {
                        val start = pressStart
                        if (start != null) {
                            // The finger moved far enough to be a drag (a
                            // touch-scroll) — cancel the tap-aim so the eyes
                            // go NEUTRAL instead of following the scroll.
                            val dist = (change.position - start).getDistance()
                            if (dist > with(density) { 8.dp.toPx() }) {
                                dragging = true
                                press = null
                                pressStart = null
                                position = Offset.Unspecified
                            }
                        } else if (!dragging) {
                            // No press active (mouse hover) — follow the
                            // cursor; while a drag is underway, ignore moves
                            // so the eyes stay neutral through a scroll.
                            position = change.position
                        }
                    }
                    PointerEventType.Release -> {
                        press = null
                        pressStart = null
                        dragging = false
                    }
                    else -> Unit
                }
            }
        }
    }
}

/** Converts an RRGGBB design color into Compose's packed ARGB representation. */
private fun petDesignColor(hex: String): Color =
    Color(android.graphics.Color.parseColor("#$hex"))

/**
 * The Curio pet sprite — a tiny pixelated "spark-spirit" rendered entirely
 * in Compose (spec §10.2): a round cream body, big eyes, a gold star-tipped
 * antenna, little ear nubs, feet, a waggy tail, and a scarf in the active
 * category accent.
 *
 * No bitmap assets: the body is a pixel grid drawn as rects on a
 * Canvas, so it is crisp at any size. v8.8 — ONE fixed look in every theme
 * (cream body + warm ink outline); it no longer flips to a light twin in
 * dark mode. The face (eyes + mouth + cheeks) is drawn as overlays so moods
 * and the blink cycle swap without re-laying the body. Growth accessories
 * (spec §10.4) are stage-gated: a sprout leaf, a satchel, a tiny book, an
 * gold halo.
 *
 * Animations: idle bob + periodic blink, a fast walk bob with a lean into
 * the direction of travel, a one-shot celebration hop ([celebrateKey]), a
 * one-shot touch squish ([squishKey]), a stretched "lifted" pose while being
 * dragged, sleep breathing with floating Z's, an excited wiggle with
 * sparkle eyes, and a curious head tilt. [facing] mirrors the sprite (1 =
 * facing right, -1 = facing left).
 *
 * v8.10 — ONE fixed color on every theme: the scarf always wears
 * the Curio light-theme brand coral ([CurioColors.CategoryCoral]); they no
 * longer react to the category pastel accents or to dark mode.
 */

/**
 * v8.52 — the procedural eye art as data: one entry per [EyeStyle], each a
 * list of (col, row, slot) where slot is "ink" | "white" | "star". Shared by
 * the sprite renderer AND the Eyes editor's blueprint, so the reference the
 * editor draws always matches what the pet would have worn.
 */
internal val EYE_STYLE_PIXELS: Map<EyeStyle, List<Triple<Int, Int, String>>> = mapOf(
    EyeStyle.OPEN to listOf(
        Triple(4, 7, "ink"), Triple(5, 7, "ink"),
        Triple(4, 8, "ink"), Triple(5, 8, "ink"),
        Triple(10, 7, "ink"), Triple(11, 7, "ink"),
        Triple(10, 8, "ink"), Triple(11, 8, "ink"),
        Triple(4, 7, "white"), Triple(10, 7, "white")
    ),
    EyeStyle.BLINK to listOf(
        Triple(4, 7, "ink"), Triple(5, 7, "ink"),
        Triple(10, 7, "ink"), Triple(11, 7, "ink")
    ),
    EyeStyle.CLOSED to listOf(
        Triple(4, 8, "ink"), Triple(5, 8, "ink"),
        Triple(10, 8, "ink"), Triple(11, 8, "ink")
    ),
    EyeStyle.WIDE to listOf(
        Triple(4, 6, "ink"), Triple(5, 6, "ink"),
        Triple(4, 7, "ink"), Triple(5, 7, "ink"),
        Triple(4, 8, "ink"), Triple(5, 8, "ink"),
        Triple(10, 6, "ink"), Triple(11, 6, "ink"),
        Triple(10, 7, "ink"), Triple(11, 7, "ink"),
        Triple(10, 8, "ink"), Triple(11, 8, "ink"),
        Triple(4, 7, "white"), Triple(10, 7, "white")
    ),
    EyeStyle.STAR to listOf(
        // Natural warm-brown sparkle eyes with a white glint.
        Triple(4, 6, "star"), Triple(5, 6, "star"),
        Triple(3, 7, "star"), Triple(4, 7, "star"), Triple(5, 7, "star"), Triple(6, 7, "star"),
        Triple(4, 8, "star"), Triple(5, 8, "star"),
        Triple(4, 7, "white"),
        Triple(10, 6, "star"), Triple(11, 6, "star"),
        Triple(9, 7, "star"), Triple(10, 7, "star"), Triple(11, 7, "star"), Triple(12, 7, "star"),
        Triple(10, 8, "star"), Triple(11, 8, "star"),
        Triple(10, 7, "white")
    ),
    EyeStyle.DIZZY to listOf(
        // Dizzy pinwheel swirls with diagonal glints.
        Triple(4, 6, "ink"), Triple(5, 6, "ink"),
        Triple(4, 7, "ink"), Triple(5, 7, "ink"),
        Triple(4, 8, "ink"), Triple(5, 8, "ink"),
        Triple(3, 7, "ink"), Triple(6, 7, "ink"),
        Triple(4, 7, "white"), Triple(5, 6, "white"),
        Triple(10, 6, "ink"), Triple(11, 6, "ink"),
        Triple(10, 7, "ink"), Triple(11, 7, "ink"),
        Triple(10, 8, "ink"), Triple(11, 8, "ink"),
        Triple(9, 7, "ink"), Triple(12, 7, "ink"),
        Triple(10, 7, "white"), Triple(11, 6, "white")
    ),
    EyeStyle.HAPPY to listOf(
        Triple(4, 8, "ink"), Triple(5, 7, "ink"), Triple(5, 8, "ink"),
        Triple(10, 8, "ink"), Triple(10, 7, "ink"), Triple(11, 8, "ink")
    )
)

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
    design: PetDesign? = null,
    /**
     * v8.52 — per-frame pixel layers from the animation timeline editor.
     * When set, the sprite draws these rows INSTEAD of the active design's
     * body (or curled) grid, so each animation keyframe can be a different
     * pose. `null` keeps the base design pose.
     */
    bodyOverride: List<String>? = null,
    curledOverride: List<String>? = null,
    /** Authored viewpoint for a Pet Life frame; FRONT preserves old behavior. */
    viewAngle: PetViewAngle = PetViewAngle.FRONT,
    /**
      * v8.52 — per-frame EYE layer from the animation timeline editor: a

     * fixed 16×16 grid drawn instead of the mood's procedural eyes while
     * this frame plays. `null` keeps the procedural style.
     */
    eyeOverride: List<String>? = null,
    /**
     * v8.54 — render the pet as a STILL pose: disables the idle animation
     * (bob, blink, breathing, glance, ear flick, sleep startle) so a single
     * animation frame previews exactly as drawn — no moving eyes or body.
     * Used by the timeline editor while editing a frame.
     */
    staticPose: Boolean = false,
    /**
     * v27t — pointer-aware eyes: the pet aims its eyes at the cursor
     * (Chromebook / desktop), looks at the point you press while holding,
     * and rolls its eyes on wheel scrolls. Off for the designer's static
     * previews (there the pointer isn't aimed at the sprite).
     */
    pointerAware: Boolean = true
) {
    val density = LocalDensity.current
    // v8.34 — resolve the active design: the explicit working copy wins;
    // otherwise the saved one (reactive — recomposes when a design is
    // saved in the playground). Parsing is cheap (16+16 rows) and cached
    // per text via remember.
    val savedText = AppPreferences.petDesignState
    // v27v — a SAVED custom design ALWAYS wins, regardless of the pet's
    // growth stage: the old code forced `evolutionDesign(BABY, null)` for
    // any baby-stage pet (level < 15), so "Save" said success but the pet
    // never changed for the majority of users. The stage-based evolution
    // art now only applies when NO custom design exists. Animations, view
    // angles and the sleep (curled) pose all flow from the winning design
    // automatically, so a custom pet behaves like its own new pet.
    val activeDesign = remember(savedText, design, stage) {
        val base = design ?: savedText?.let { PetDesign.DEFAULT.toParsedOr(it, PetDesign.DEFAULT) }
        base ?: PetDesign.evolutionDesign(stage, CurioPet.currentEvoPath())
    }
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
    // v8.54 — static-pose freeze (timeline editor): when staticPose is true
    // the pet renders EXACTLY one frame — no idle bob, blink, breathing,
    // glance or ear flick — so per-frame edits (especially the eyes grid)
    // preview a still pose instead of a moving pet. The infinite transitions
    // aren't even created, so the frozen preview never re-triggers on them.
    val idle = if (staticPose) null else rememberInfiniteTransition(label = "petIdle")
    // Keep the two user-visible essentials (body motion + blink), but derive
    // the slower ambient flourishes from the same motion phase. This avoids
    // five independent per-frame state channels for every visible pet while
    // preserving the idle bob, blink, glance, breathing, and ear-flick cues.
    val bobPhase: Float
    val blinkPhase: Float
    if (idle != null) {
        bobPhase = idle.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = bobSpec,
            label = "petBob"
        ).value
        blinkPhase = idle.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = blinkSpec,
            label = "petBlink"
        ).value
    } else {
        bobPhase = 0f
        blinkPhase = 0f
    }
    val breathePhase = bobPhase
    // Slow glance and ear flick share the low-cost ambient phase. Their
    // existing guards still prevent them during movement, sleep, or thinking.
    val glancePhase = bobPhase
    val flickPhase = bobPhase
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
    LaunchedEffect(sleeping, staticPose) {
        if (!sleeping || staticPose) return@LaunchedEffect
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
    val idleTilt = (if (thinkingNow) facing * 7f else 0f) +
        (if (watchingNow) facing * 2.5f else 0f)
    // Pet Life angles are deliberate pose cues, not a global 3D rotation:
    // authored side/back moments use a restrained turn and eye placement so
    // the sprite stays readable while still presenting a different view.
    val angleTilt = when (viewAngle) {
        PetViewAngle.FRONT -> 0f
        PetViewAngle.THREE_QUARTER -> facing * 7f
        PetViewAngle.SIDE -> facing * 14f
        PetViewAngle.BACK -> facing * 18f
        PetViewAngle.LOOKING_UP -> facing * 2f
        PetViewAngle.LOOKING_DOWN -> facing * -3f
        PetViewAngle.CURLED -> 0f
    }
    val angleFaceShift = when (viewAngle) {
        PetViewAngle.FRONT -> 0
        PetViewAngle.THREE_QUARTER -> if (facing > 0f) -1 else 1
        PetViewAngle.SIDE, PetViewAngle.BACK -> if (facing > 0f) -2 else 2
        PetViewAngle.LOOKING_UP -> 0
        PetViewAngle.LOOKING_DOWN -> 0
        PetViewAngle.CURLED -> 0
    }

    // ── Face state ─────────────────────────────────────────────────────
    // v8.35 — faces are configurable: the base face for the ambient mood
    // comes from the design's face editor (or the built-in default), and
    // the one-shot states (celebration hop, spin cheer, mid-play) wear the
    // design's EXCITED face so a happy hop reads excited. A [faceOverride]
    // (a reaction rule's face, e.g. from petting) wins while set.
    val moodFace = faceOverride ?: activeDesign.faceFor(mood.name)
    val customMoodGrid = moodFace.gridRows.takeIf { it.isNotEmpty() }
    val oneShotFace = activeDesign.faceFor(PetFaceMoods.EXCITED)
    val customOneShotGrid = oneShotFace.gridRows.takeIf { it.isNotEmpty() }
    val activeCustomGrid = when {
        faceOverride != null -> customMoodGrid
        excited || spinningNow || playing -> customOneShotGrid
        else -> customMoodGrid
    }
    val eyes = when {
        // v8.21 — being flung around spins the eyes first.
        dizzy -> EyeStyle.DIZZY
        dragged -> EyeStyle.WIDE // lifted mid-play: startled wins
        playing -> oneShotFace.eyes
        sleeping -> EyeStyle.CLOSED
        peeking -> EyeStyle.WIDE // peeking out from behind a button
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

    // v27t — pointer-aware eyes: track this sprite's on-screen center and
    // compute where the cursor is relative to it, so the eyes aim at the
    // pointer (or the held press point), capped so they never leave the
    // head.
    // v27v — the look TIMES OUT: while pointer events keep arriving the eyes
    // aim at full strength, and ~2s after the last one (or once the press is
    // released) they ease back to the neutral glance — the pet stops staring
    // at the last scroll/tap point when you're not touching the screen.
    // v28 — scrolling (wheel or touch-drag) drives a vertical glance along
    // the scroll line instead of the old full-circle eye roll.
    var spriteCenter by remember { mutableStateOf<Offset?>(null) }
    val pointerPos = PetPointer.position
    val pressPos = PetPointer.press
    val lookAt = pressPos ?: pointerPos
    val lookStrength = remember { Animatable(1f) }
    LaunchedEffect(PetPointer.activityTick, PetPointer.press, pointerAware) {
        if (!pointerAware) return@LaunchedEffect
        lookStrength.snapTo(1f)
        // While the finger is held down (long-press / button hold) the look
        // stays; only schedule the fade-out once it's been released.
        if (PetPointer.press == null) {
            delay(2_000)
            lookStrength.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
    }
    val lookCells: Offset = when {
        pointerAware && spriteCenter != null && lookAt != Offset.Unspecified && lookStrength.value > 0f -> {
            val d = lookAt - spriteCenter!!
            val dist = d.getDistance().coerceAtLeast(1f)
            // Aim saturates within ~200dp — a farther cursor keeps pointing
            // the same direction without the eyes over-tracking. Scaled by
            // the v27v timeout so the aim eases back to neutral when idle.
            val prox = (dist / with(density) { 200.dp.toPx() }).coerceIn(0f, 1f) * lookStrength.value
            Offset(d.x / dist * 1.15f, d.y / dist * 0.75f) * prox
        }
        else -> Offset.Zero
    }

    val desc = contentDescription
    Box(
        modifier = modifier
            .size(spriteSize)
            .onGloballyPositioned {
                spriteCenter = it.positionInWindow() +
                    Offset(it.size.width / 2f, it.size.height / 2f)
            }
            .then(if (desc != null) Modifier.semantics { this.contentDescription = desc } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // One motion layer carries the bob/hop/wiggle/lean and the breathing
        // + squish scales so the pixel art moves as one clean silhouette.
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
                    rotationZ = wiggle + walkLean + tilt + idleTilt + angleTilt + spinAngle.value + dizzyWobble
                }
        ) {
            // Flip layer — mirrors the sprite horizontally so it faces the
            // way it walks without touching the motion transforms above.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = facing }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // v10 — the canvas adapts from the baby 16-grid through
                    // legacy custom sizes to the evolved 64-grid.
                    // Design cells (drawGridPx) use one cell = px; the
                    // procedural face art (drawPx) is authored in a 16-grid
                    // space and scaled to the canvas (opx), so the face
                    // keeps its proportions on any grid.
                    val grid = activeDesign.gridSize
                    val px = size.width / grid
                    // Procedural face/motion art keeps its legacy 16-space
                    // proportions; detailed evolved accessories use the
                    // design grid through drawGridPx instead.
                    val opx = size.width / 16f
                    fun drawGridPx(col: Int, row: Int, color: Color, alpha: Float = 1f) {
                        if (col !in 0 until grid || row !in 0 until grid) return
                        // Use exact, square cells. Rounded overlapping cells
                        // created hairline seams and color halos on dense
                        // 64×64 designs, especially while the sprite scaled
                        // or moved between device pixels.
                        val left = floor(col * px)
                        val top = floor(row * px)
                        val right = ceil((col + 1) * px)
                        val bottom = ceil((row + 1) * px)
                        drawRect(
                            color = color.copy(alpha = alpha),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top)
                        )
                    }
                    // Procedural overlay cell — the legacy 16-grid authoring space.
                    // NOTE: Int params — Kotlin has no implicit Int→Float
                    // conversion, and the face art passes integer coords.
                    // (Int * opx yields Float, so scaling works.)
                    fun drawPx(col: Int, row: Int, color: Color, alpha: Float = 1f) {
                        val left = floor(col * opx)
                        val top = floor(row * opx)
                        val right = ceil((col + 1) * opx)
                        val bottom = ceil((row + 1) * opx)
                        drawRect(
                            color = color.copy(alpha = alpha),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top)
                        )
                    }

                    /** Draws one authored detail layer with its saved body-relative transform. */
                    fun drawDetailLayer(layer: String) {
                        // Keep authored path accessories in the same on/off contract
                        // as the generated accessory art and the Accessories dialog.
                        if (layer == "accessories" && !activeDesign.isProceduralEnabled("accessories")) return
                        val rows = activeDesign.details[layer] ?: return
                        val transform = if (AppPreferences.petPartTransformsState) {
                            activeDesign.detailTransform(layer)
                        } else {
                            DetailTransform()
                        }
                        val offsetX = transform.offsetX * px
                        val offsetY = transform.offsetY * px
                        translate(left = offsetX, top = offsetY) {
                            scale(
                                scale = transform.scale,
                                pivot = Offset(size.width / 2f, size.height / 2f)
                            ) {
                                rows.forEachIndexed { row, line ->
                                    line.forEachIndexed { col, ch ->
                                        activeDesign.colorFor(ch)?.let { hex ->
                                            drawGridPx(col, row, petDesignColor(hex))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Body — static pattern with the accent scarf. v8.14 —
                    // asleep pets CURL UP into a cozy ball instead of
                    // standing with closed eyes. v8.34 — the active design's
                    // grids replace the default ones. v8.35 — every palette
                    // key renders (custom paint slots included).
                    val bodyRows = if (sleeping) (curledOverride ?: activeDesign.curledRows)
                    else (bodyOverride ?: activeDesign.bodyRows)
                    bodyRows.forEachIndexed { row, line ->
                        line.forEachIndexed { col, ch ->
                            val hex = activeDesign.colorFor(ch)
                            if (hex != null) drawGridPx(col, row, petDesignColor(hex))
                        }
                    }
                    // A turned-back view gets its own silhouette cues: no
                    // face, a centered spine stripe, and a scarf knot at the
                    // nape. It remains compatible with every saved body grid.
                    if (!sleeping && viewAngle == PetViewAngle.BACK) {
                        drawRoundRect(
                            color = bodyShade.copy(alpha = 0.72f),
                            topLeft = Offset(7.2f * opx, 5.5f * opx),
                            size = Size(1.6f * opx, 7.5f * opx),
                            cornerRadius = CornerRadius(0.7f * opx)
                        )
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(6f * opx, 10.5f * opx),
                            size = Size(4f * opx, 1.8f * opx),
                            cornerRadius = CornerRadius(0.6f * opx)
                        )
                    }

                    if (sleeping) {
                        // ── Curled sleep pose (v8.14) ──────────────────────
                        // Happy closed-eye arcs on the ball.
                        if (activeCustomGrid == null) {
                            drawPx(5, 4, ink); drawPx(6, 4, ink)
                            drawPx(9, 4, ink); drawPx(10, 4, ink)
                        }
                        // The rare startle flashes the eyes open for a beat.
                        if (startling && activeCustomGrid == null) {
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
                        if (activeDesign.isProceduralEnabled("antenna")) {
                            drawPx(7, 0, gold); drawPx(8, 0, gold)
                            drawPx(6, 1, cap); drawPx(7, 1, cap); drawPx(8, 1, cap); drawPx(9, 1, cap)
                            drawPx(6, 2, cap); drawPx(7, 2, cap); drawPx(8, 2, cap); drawPx(9, 2, cap)
                            drawPx(6, 3, capTrim); drawPx(7, 3, capTrim); drawPx(8, 3, capTrim); drawPx(9, 3, capTrim)
                        }
                        if (activeCustomGrid != null) {
                            activeCustomGrid.forEachIndexed { row, line ->
                                line.forEachIndexed { col, ch ->
                                    activeDesign.colorFor(ch)?.let { hex ->
                                        drawGridPx(col, row, petDesignColor(hex))
                                    }
                                }
                            }
                        }
                    } else {
                        // Soft belly patch — a lighter tummy inside the blob.
                        if (activeDesign.isProceduralEnabled("belly")) {
                            drawRoundRect(
                                color = bellyLight.copy(alpha = 0.85f),
                                topLeft = Offset(5 * opx, 9 * opx),
                                size = Size(6 * opx, 3 * opx),
                                cornerRadius = CornerRadius(3 * opx)
                            )
                        }

                        // Little tail on the sprite's right side — wags when
                        // walking, excited, or mid-play (faster while playing).
                        if (!dragged && activeDesign.isProceduralEnabled("tail")) {
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
                        if (flicking && activeDesign.isProceduralEnabled("accessories")) {
                            drawPx(2, 1, ink)
                            drawPx(2, 0, ink)
                        }

                        // Face overlays — the eyes drift with the glance and
                        // lift while watching the Spin deck; cheeks and mouth
                        // stay put. v8.13 — the eyes sit one row HIGHER
                        // (rows 6-8 instead of 7-9) so there is a clear gap
                        // between them and the mouth — never joined.
                        if (viewAngle != PetViewAngle.BACK && activeCustomGrid == null) translate(
                            // v27t — the pointer-aim offsets join the glance
                            // drift so the eyes track the cursor too.
                            left = (glanceShift + angleFaceShift + lookCells.x) * opx,
                            top = lookCells.y * opx + when (viewAngle) {
                                PetViewAngle.LOOKING_UP -> -2f * opx
                                PetViewAngle.LOOKING_DOWN -> opx
                                else -> if (watchingNow) -opx else 0f
                            }
                        ) {
                            if (eyeOverride != null) {
                                // v8.52 — per-frame eye layer: palette-aware
                                // pixels drawn exactly where authored (16-space),
                                // still drifting with the glance.
                                eyeOverride.forEachIndexed { row, line ->
                                    line.forEachIndexed { col, ch ->
                                        if (ch == '.') return@forEachIndexed
                                        val hex = activeDesign.colorFor(ch) ?: return@forEachIndexed
                                        drawPx(col, row, petDesignColor(hex))
                                    }
                                }
                            } else {
                                // v64 — procedural eye size preset + placement:
                                // scale each eye around its own center (left eye
                                // center col 4.5, right 10.5, row 7 in the same
                                // 16-space the eyes are authored in), then apply
                                // the user's offset. The designer's Eyes section
                                // writes these fields; the sprite reads them so
                                // the live pet and every preview stay in sync.
                                val scaleF = when (activeDesign.eyeScale) {
                                    0 -> 0.85f
                                    2 -> 1.2f
                                    else -> 1f
                                }
                                val offX = activeDesign.eyeOffsetX
                                val offY = activeDesign.eyeOffsetY
                                EYE_STYLE_PIXELS[eyes]?.forEach { (c, r, slot) ->
                                    val color = when (slot) {
                                        "white" -> white
                                        "star" -> starEye
                                        else -> ink
                                    }
                                    val centerC = if (c < 8) 4.5f else 10.5f
                                    val nc = ((centerC + (c - centerC) * scaleF) + offX).roundToInt()
                                    val nr = ((7f + (r - 7f) * scaleF) + offY).roundToInt()
                                    drawPx(nc, nr, color)
                                }
                            }
                        }

                        // Cheeks — only when the pet is happy/excited/proud/
                        // bouncy or mid-play/spin (v8.13: not a permanent
                        // feature, and the row-10 pair is gone).
                        if (viewAngle != PetViewAngle.BACK && activeCustomGrid == null && blushing) {
                            drawPx(2, 9, blush, 0.5f)
                            drawPx(3, 9, blush, 0.5f)
                            drawPx(12, 9, blush, 0.5f)
                            drawPx(13, 9, blush, 0.5f)
                        }

                        // Mouth. v8.10 — the smile was drawn upside down (a
                        // frown); flipped: corners UP (row 10), middle DOWN
                        // (row 11) = a proper happy smile.
                        if (viewAngle != PetViewAngle.BACK && activeCustomGrid == null) when (mouth) {
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

                        // Growth accessories (spec §10.4).
                        if (activeDesign.isProceduralEnabled("accessories")) when (stage) {
                            CurioPet.Stage.BABY -> {
                                // Baby: tiny leaf sprout on head.
                                drawPx(4, 2, leaf); drawPx(5, 1, leaf); drawPx(5, 2, leaf); drawPx(5, 3, leaf)
                            }
                            CurioPet.Stage.FIRST_EVO -> {
                                // Evo: element badge on chest.
                                val evoColor = when (CurioPet.currentEvoPath()) {
                                    CurioPet.EvoPath.FIRE -> Color(0xFFFF6B4A)
                                    CurioPet.EvoPath.WATER -> Color(0xFF4A9BFF)
                                    CurioPet.EvoPath.NATURE -> Color(0xFF6BBF59)
                                    null -> accent
                                }
                                drawPx(6, 9, evoColor); drawPx(7, 9, evoColor); drawPx(8, 9, evoColor); drawPx(9, 9, evoColor)
                                drawPx(7, 10, evoColor); drawPx(8, 10, evoColor)
                            }
                            CurioPet.Stage.FINAL_EVO -> {
                                // Final: gold halo + element aura.
                                drawPx(4, 0, gold); drawPx(8, 0, gold)
                                drawPx(6, 1, goldDeep)
                            }
                            else -> Unit
                        }
                        if (activeCustomGrid != null) {
                            // Hand-drawn mood/reaction faces are transparent overlays;
                            // the body, tail, accessories, and other motion art remain intact.
                            activeCustomGrid.forEachIndexed { row, line ->
                                line.forEachIndexed { col, ch ->
                                    activeDesign.colorFor(ch)?.let { hex ->
                                        drawGridPx(col, row, petDesignColor(hex))
                                    }
                                }
                            }
                        }
                    }

                    // Sleep Z's — v8.14 pronounced: three z's drifting up-right
                    // above the curled ball.
                    if (sleeping && activeDesign.isProceduralEnabled("effects")) {
                        val drift = ((breathePhase * 2f) % 1f).toInt()
                        drawPx(11, 3 - drift, ink, 0.85f); drawPx(12, 4 - drift, ink, 0.85f); drawPx(11, 4 - drift, ink, 0.85f)
                        drawPx(12, 5 - drift, ink, 0.65f); drawPx(13, 6 - drift, ink, 0.65f); drawPx(12, 6 - drift, ink, 0.65f)
                        drawPx(14, 7 - drift, ink, 0.4f); drawPx(15, 8 - drift, ink, 0.4f); drawPx(14, 8 - drift, ink, 0.4f)
                    }

                    // Excited sparkles around the pet (v8.35 — toggleable in
                    // the Face/Excitement editor via the design's face).
                    if (sparklesOn && activeDesign.isProceduralEnabled("effects")) {
                        val twinkle = sin(bobPhase * 8f * PI.toFloat()) * 0.5f + 0.5f
                        drawPx(1, 2, gold, twinkle * 0.9f)
                        drawPx(14, 3, gold, (1f - twinkle) * 0.9f)
                        drawPx(2, 13, gold, twinkle * 0.8f)
                        drawPx(13, 2, gold, (1f - twinkle) * 0.8f)
                    }

                    // v8.21 — while dizzy, little whoosh marks trail beside
                    // the pet so the spin feels like it's actually moving.
                    if (dizzy && activeDesign.isProceduralEnabled("effects")) {
                        val whoosh = sin(bobPhase * 10f * PI.toFloat())
                        val a = 0.45f + whoosh * 0.25f
                        drawPx(1, 6, ink, a); drawPx(0, 7, ink, a * 0.8f); drawPx(1, 8, ink, a)
                        drawPx(14, 6, ink, a); drawPx(15, 7, ink, a * 0.8f); drawPx(14, 8, ink, a)
                    }

                    // A white glint twinkles on the antenna star.
                    if (activeDesign.isProceduralEnabled("antenna") && sin(bobPhase * 2f * PI.toFloat()) > 0.78f) {
                        drawPx(7, 0, white, 0.9f)
                    }

                    // A tiny "?" hovers above the antenna while thinking.
                    if (thinkingNow && activeDesign.isProceduralEnabled("antenna")) {
                        drawPx(9, 0, ink)
                        drawPx(8, 1, ink); drawPx(9, 1, ink)
                        drawPx(9, 2, ink)
                    }

                    // User-authored details are the final visual layer so
                    // every aspect can be drawn over generated art without
                    // changing Curie's existing motion or z-order behavior.
                    drawDetailLayer("tail")
                    drawDetailLayer("accessories")
                    drawDetailLayer("antenna")
                    drawDetailLayer("effects")
                }
            }
        }
    }
}

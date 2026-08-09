package com.curio.app.ui.pet

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.data.CustomPetAction
import com.curio.app.data.PetActionTrigger
import com.curio.app.data.PetAnimation
import com.curio.app.data.PetDesign
import com.curio.app.data.PetFace
import com.curio.app.data.PetReactionEvents
import com.curio.app.data.ReactionAnim
import com.curio.app.data.animationById
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

private val FLOAT_SIZE = 72.dp
private val EDGE_MARGIN = 14.dp
private val AUTO_NAP_AFTER_MS = 8 * 60_000L
// v8.13 — hearts rise in their own box ABOVE the pet (never over its face).
// v8.21 — a smaller box so the hearts read as tiny, and they fade out fully.
private val HEARTS_W = 132.dp
private val HEARTS_H = 84.dp
// v8.20 — the little cloud the pet rides while it walks (under the sprite).
// v8.23 — it is a PIXEL cloud now, drawn in the pet's own style (soft
// rounded pixels), so the ride reads on-style instead of as a blurry blob.
// v8.26 — smaller and tucked right under the feet; the grid is 20 wide so
// the full 20-char pixel rows render (the old 16-col grid clipped the
// right edge and made the silhouette lopsided).
private val CLOUD_W = 80.dp
private val CLOUD_H = 32.dp
private const val CLOUD_GRID_W = 20
private const val CLOUD_GRID_H = 8
// v8.20 — how close a drop must be to the flower bed to count as "home".
private val DROP_FORGIVENESS = 12.dp
// v8.35 — the tiny pixel keyboard Curie types on while the user types.
private val TYPING_W = 150.dp
private val TYPING_H = 34.dp

/**
 * The floating Curio pet (v8.8) — a global overlay that lives on top of
 * every screen once the pet is awake ([CurioPet.awake], set by tapping it
 * in its flower bed). It:
 *
 *  - WANDERS on its own: every few seconds it picks a new spot inside the
 *    screen bounds and walks over, facing the way it's going (disabled when
 *    the system's animator scale is 0 — reduced motion).
 *  - CAN BE DRAGGED ANYWHERE: grab it and it stretches like it's being
 *    lifted; release and it settles where you put it (clamped to the edges).
 *  - REACTS TO TOUCH (v8.11): quick repeated taps escalate the reaction
 *    (soft boop -> playful play-bow -> happy celebration) with a matching
 *    line and hearts, then a playful dart to a nearby spot. v8.21 — being
 *    DRAGGED is what makes it dizzy now: swirl eyes + a wobbly sway while
 *    it's flung around, then a short groggy recovery with a line.
 *  - CELEBRATES: when its mood flips to EXCITED/PROUD (a new lane, a
 *    level-up, a claim), it hops with a short excited line.
 *  - NAPS: after a long idle it fades back into its flower bed
 *    ([CurioPet.settleToSleep]) — the bed shows it asleep until tapped.
 *
 * Gated by the Appearance toggles: the whole pet layer
 * ([AppPreferences.petEnabledState]) and the floating companion itself
 * ([AppPreferences.floatingPetEnabledState]). v8.21 — the pet STAYS visible
 * while a dialog is open (dimmed behind the scrim) and only hides while it
 * is sitting at home in its bed ([CurioPet.atHome]).
 *
 * v8.10 — the sprite wears ONE fixed color (the Curio brand coral), so the
 * overlay no longer takes an accent; the soft cream glow disc behind the
 * pet was removed. A LONG-PRESS fades the pet out and sends it home to sit
 * in its flower bed.
 *
 * Touch plumbing lives ONLY on the pet element, so the transparent overlay
 * never blocks taps or scrolls on the screen beneath.
 */
@Composable
fun CurioFloatingPet(
    routePrefix: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // v8.26b — the throw-glide coroutine scope. NOTE: in this Compose BOM
    // (2026.05) PointerInputScope no longer extends CoroutineScope, so the
    // drag callbacks cannot launch on `this` — this composition-scoped
    // scope is the safe host for the glide (cancelled when the pet leaves
    // composition).
    val glideScope = rememberCoroutineScope()
    if (!AppPreferences.petEnabledState ||
        !AppPreferences.floatingPetEnabledState ||
        !CurioPet.awake ||
        CurioPet.atHome
    ) return

    // Reduced motion: no autonomous wandering — the pet still follows touch.
    val animatorScale = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    val autoWander = animatorScale > 0f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val petPx = with(density) { FLOAT_SIZE.toPx() }
        val marginPx = with(density) { EDGE_MARGIN.toPx() }
        // Start just above the bottom bar / nav inset so it never covers the
        // action dock or the bottom nav out of the box.
        val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density) }
        val defaultPos = remember(maxW, maxH, petPx, marginPx, navBottom) {
            Offset(
                x = maxW - petPx - marginPx,
                y = (maxH - petPx - marginPx - navBottom - with(density) { 84.dp.toPx() })
                    .coerceAtLeast(marginPx)
            )
        }

        var pos by remember(maxW, maxH) { mutableStateOf(defaultPos) }
        var facing by remember { mutableStateOf(1f) }
        var moving by remember { mutableStateOf(false) }
        var dragged by remember { mutableStateOf(false) }
        // v8.26 — true while the post-throw glide is running, so the wander
        // loop holds off until the pet finishes sliding.
        var gliding by remember { mutableStateOf(false) }
        var thinking by remember { mutableStateOf(false) }
        var squishKey by remember { mutableIntStateOf(0) }
        var playKey by remember { mutableIntStateOf(0) }
        var spinKey by remember { mutableIntStateOf(0) }
        var celebrateKey by remember { mutableIntStateOf(0) }
        var heartsKey by remember { mutableIntStateOf(0) }
        var reaction by remember { mutableStateOf<String?>(null) }
        var reactionKey by remember { mutableIntStateOf(0) }
        // v8.26 — the speech bubble fades + rises in and out instead of
        // popping, so line changes feel smooth rather than abrupt.
        val bubbleAnim = remember { Animatable(0f) }
        var lastMood by remember { mutableStateOf<CurioPet.Mood?>(null) }
        var lastTouch by remember { mutableStateOf(System.currentTimeMillis()) }
        var leavingHome by remember { mutableStateOf(false) }
        // v8.21 — dragging flings the pet dizzy: swirl eyes + a wobbly sway
        // while it's held, then a short recovery after release.
        var dizzy by remember { mutableStateOf(false) }
        var recovering by remember { mutableStateOf(false) }
        var dragStartAt by remember { mutableStateOf(0L) }
        // v8.11 — touch escalation: rapid repeated taps (within 1.6s) push
        // the reaction tier up (boop -> play-bow -> celebration; v8.21 —
        // the dizzy zoomies tier moved to dragging). A tap also queues
        // a playful dart that the wander loop dashes to promptly.
        var tapStreak by remember { mutableIntStateOf(0) }
        var lastTapAt by remember { mutableStateOf(0L) }
        var playDartTarget by remember { mutableStateOf<Offset?>(null) }
        // v8.16 — landmark pokes keep a cooldown so the pet interacts often
        // but never spams the same thing every beat. On the Spin screen the
        // wander beat cycles every ~300ms (the watching gate exits the wait
        // loop early), so without this the pet would boop the Shuffle button
        // almost constantly while the deck waits.
        var lastPokeAt by remember { mutableStateOf(0L) }
        val appear = remember { Animatable(0f) }
        // v8.9 — on the Spin screen the pet stops to watch the deck; event
        // reactions start from the current count so stale events never fire.
        val watching = routePrefix?.startsWith("spin") == true
        // v8.13 — the pet knows when the user is writing on the capture
        // screen and wears its quiet FOCUSED mood there.
        val captureScreen = routePrefix?.startsWith("capture") == true
        val screenHint = if (captureScreen) "capture" else null
        var seenEvents by remember { mutableIntStateOf(CurioPet.eventCount) }
        // v8.35 — the saved custom design's reaction rules drive what Curie
        // does for each event (reactive — re-read when a design is saved).
        val savedText = AppPreferences.petDesignState
        val activeDesign = remember(savedText) {
            savedText?.let { PetDesign.DEFAULT.toParsedOr(it, PetDesign.DEFAULT) }
        } ?: PetDesign.DEFAULT
        val accentColor = designColor(activeDesign.colorOf('s'))
        // v8.35 — the reaction rule's face while a reaction plays (cleared a
        // beat later); hide-and-peek crouch; typing-along state.
        var reactionFace by remember { mutableStateOf<PetFace?>(null) }
        var reactionFaceKey by remember { mutableIntStateOf(0) }
        var peeking by remember { mutableStateOf(false) }
        var lastPeekAt by remember { mutableStateOf(0L) }
        var typingReaction by remember { mutableStateOf(false) }
        var lastTypingAt by remember { mutableStateOf(0L) }
        var lastTypingScreen by remember { mutableStateOf<String?>(null) }
        // v8.53 — Phase 7: a user-defined custom action playing right now.
        // The animation is stepped once by a LaunchedEffect; its frame drives
        // the sprite's per-frame pixel layers + transform while it plays.
        var customActionAnim by remember { mutableStateOf<PetAnimation?>(null) }
        var customActionFrame by remember { mutableIntStateOf(0) }
        var customActionKey by remember { mutableIntStateOf(0) }

        /**
         * v8.35 — fires a configured reaction: the animation + face from the
         * design's reaction rule for [event], plus an optional [line].
         */
        fun fireReaction(event: String, line: String?) {
            val rule = activeDesign.reactionFor(event)
            if (!rule.enabled) return
            when (rule.anim) {
                ReactionAnim.HOP -> celebrateKey++
                ReactionAnim.SPIN -> spinKey++
                ReactionAnim.SQUISH -> squishKey++
                ReactionAnim.BOUNCE -> playKey++
                ReactionAnim.NONE -> Unit
            }
            reactionFace = rule.face
            reactionFaceKey++
            lastTouch = System.currentTimeMillis()
            if (line != null) {
                // Custom lines are deliberately opt-in. When enabled, an
                // event with saved lines speaks one of them; an event with
                // no saved lines keeps Curie's built-in dialogue.
                reaction = if (AppPreferences.customReactionLinesState) {
                    rule.lines.randomOrNull() ?: line
                } else {
                    line
                }
                reactionKey++
            }
        }

        /**
         * v8.53 — plays one user-defined custom action: resolves its
         * animation (built-in or drawn in the designer), starts the frame
         * stepper, wears the animation's mood face, and speaks a random
         * saved line when the action has any.
         */
        fun playCustomAction(action: CustomPetAction) {
            val anim = activeDesign.animations[action.animationId]
                ?: animationById(action.animationId)
                ?: return
            if (anim.frames.isEmpty()) return
            customActionAnim = anim
            customActionFrame = 0
            customActionKey++
            lastTouch = System.currentTimeMillis()
            val line = action.dialogueLines.randomOrNull()
            if (line != null) {
                reaction = line
                reactionKey++
            }
        }

        /**
         * v8.53 — fires a random enabled custom action whose trigger kind
         * matches [kind] (optionally filtered by [param], used by the
         * time-of-day and idle triggers).
         */
        fun fireCustomActions(kind: String, param: Int? = null) {
            val candidates = activeDesign.customActions.filter {
                it.enabled && it.trigger.kind == kind &&
                    (param == null || it.trigger.param == param)
            }
            if (candidates.isEmpty()) return
            playCustomAction(candidates.random())
        }

        // v8.53 — steps the custom action's animation through its frames
        // once, then stops (one-shot, exactly like a reaction move).
        LaunchedEffect(customActionKey, customActionAnim) {
            val anim = customActionAnim ?: return@LaunchedEffect
            val frames = anim.frames
            if (frames.isEmpty()) {
                customActionAnim = null
                return@LaunchedEffect
            }
            for (i in frames.indices) {
                customActionFrame = i
                delay(frames[i].durationMs.toLong())
            }
            customActionAnim = null
        }

        // Entrance hop.
        LaunchedEffect(Unit) {
            appear.snapTo(0f)
            appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f))
        }
        // v8.53 — app-open custom actions fire once when the pet appears.
        val appOpenFired = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!appOpenFired.value) {
                appOpenFired.value = true
                fireCustomActions(PetActionTrigger.APP_OPEN)
            }
        }
        // Keep the pet in bounds after rotation / resize.
        LaunchedEffect(maxW, maxH) {
            pos = Offset(
                pos.x.coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx)),
                pos.y.coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
            )
        }

        // ── Autonomy: wander, think, and PLAY (v8.11) ───────────────────
        // Keyed on awake too: the loop dies when the pet naps and restarts
        // fresh when it wakes again. On the Spin screen the pet prefers to
        // WATCH the deck, so it stays put there. `watching` is a plain val
        // (not state), so it must be an effect key — otherwise the loop
        // would keep a stale value after navigating between screens.
        // v8.16 — keyed on routePrefix too: the pet re-reads the current
        // screen's landmarks (PetLandmarks.forScreen) fresh inside the loop,
        // so navigating swaps which things it can poke.
        LaunchedEffect(maxW, maxH, autoWander, CurioPet.awake, watching, routePrefix) {
            if (!autoWander) return@LaunchedEffect
            // A shared walker for gentle wanders and fast playful darts.
            // [stepMs] small = fast dash; [steps] = path length.
            suspend fun walkTo(target: Offset, stepMs: Long = 24, steps: Int = 56) {
                facing = if (target.x >= pos.x) 1f else -1f
                moving = true
                val start = pos
                for (i in 1..steps) {
                    // v8.26 — a throw-glide owns the position: hold the walk
                    // until the slide settles.
                    if (dragged || gliding || !CurioPet.awake) break
                    val t = i.toFloat() / steps
                    pos = Offset(
                        start.x + (target.x - start.x) * t,
                        start.y + (target.y - start.y) * t
                    )
                    delay(stepMs)
                }
                moving = false
            }
            while (CurioPet.awake) {
                // Wait for the next wander beat, but answer a pending tap
                // dart within ~200ms instead of the full 3-7s pause.
                val waitMs = Random.nextLong(2800, 7000)
                var waited = 0L
                while (waited < waitMs && playDartTarget == null &&
                    !dragged && !watching && !gliding && !typingReaction && CurioPet.awake
                ) {
                    delay(200)
                    waited += 200
                }
                if (!CurioPet.awake) break
                if (playDartTarget != null && !dragged) {
                    val target = playDartTarget!!
                    playDartTarget = null
                    // Playful dash to where the tap happened — quick and keen.
                    CurioPet.notePlay(context)
                    walkTo(target, stepMs = 14, steps = 40)
                    continue
                }
                if (dragged) {
                    // Held in a drag: pause a beat so the loop never
                    // busy-spins while grabbed.
                    delay(300)
                    continue
                }
                // v8.16 — landmark interactions: instead of a random point,
                // the pet sometimes walks TO an interesting thing on this
                // screen (the spin button, the profile avatar, a heading)
                // and POKES it — the thing springs back a beat. Movement
                // adapts to what it's approaching: eager quick steps + a
                // boop for FUN gadgets, a slow curious tiptoe + a read-tilt
                // for CURIOUS text. Runs even while "watching" the deck.
                val landmarks = PetLandmarks.forScreen(routePrefix)
                // v8.16 — while the deck is actively reeling, the pet stays
                // glued to watch it land; landmark pokes only happen when
                // the deck is idle on the spin screen. The 4s cooldown keeps
                // pokes occasional even where the beat loop cycles fast.
                if (!CurioPet.spinning && landmarks.isNotEmpty() &&
                    System.currentTimeMillis() - lastPokeAt > 4_000L &&
                    Random.nextFloat() < 0.45f
                ) {
                    val target = landmarks.random()
                    val c = target.bounds.center
                    // Stand BESIDE the thing, never on top of it.
                    val tx = (c.x + (if (Random.nextFloat() < 0.5f) -1 else 1) * (petPx * 0.95f))
                        .coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx))
                    val ty = (c.y + (if (Random.nextFloat() < 0.5f) -1 else 1) * (petPx * 0.95f))
                        .coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
                    when (target.kind) {
                        PetLandmarks.Kind.FUN -> {
                            // Eager approach — quick happy steps, then a
                            // boop with hearts.
                            if (Random.nextFloat() < 0.5f) playKey++
                            walkTo(Offset(tx, ty), stepMs = 15, steps = 44)
                            PetLandmarks.poke(target.id)
                            squishKey++
                            heartsKey++
                            reaction = CurioPet.landmarkLine(funThing = true)
                            reactionKey++
                            lastTouch = System.currentTimeMillis()
                        }
                        PetLandmarks.Kind.CURIOUS -> {
                            // Curious tiptoe — slow steps, a read-tilt, then
                            // a gentle poke.
                            thinking = true
                            walkTo(Offset(tx, ty), stepMs = 36, steps = 56)
                            thinking = false
                            delay(420)
                            PetLandmarks.poke(target.id)
                            reaction = CurioPet.landmarkLine(funThing = false)
                            reactionKey++
                            lastTouch = System.currentTimeMillis()
                        }
                        PetLandmarks.Kind.PLAY -> {
                            // v8.17 — a SPECIAL spot (the pet's flower bed):
                            // an eager dash over, a poke (the spot springs a
                            // beat), then a little happy jig — a squish
                            // bounce, a play-bow and a twirl.
                            walkTo(Offset(tx, ty), stepMs = 15, steps = 44)
                            PetLandmarks.poke(target.id)
                            squishKey++
                            delay(180)
                            playKey++
                            delay(320)
                            // The hop fires with the twirl so the moment
                            // reads as a real dance, not a generic spin.
                            celebrateKey++
                            spinKey++
                            heartsKey++
                            reaction = CurioPet.jigLine()
                            reactionKey++
                            lastTouch = System.currentTimeMillis()
                        }
                    }
                    lastPokeAt = System.currentTimeMillis()
                    continue
                }
                // v8.35 — hide-and-peek: every so often Curie crouches beside
                // a button or gadget and peeks out — play without words.
                // Not while glued to the Spin deck or mid-spin.
                if (!watching && !CurioPet.spinning && landmarks.isNotEmpty() &&
                    System.currentTimeMillis() - lastPeekAt > 22_000L &&
                    Random.nextFloat() < 0.4f
                ) {
                    lastPeekAt = System.currentTimeMillis()
                    val target = landmarks.random()
                    val c = target.bounds.center
                    val side = if (Random.nextFloat() < 0.5f) -1 else 1
                    val tx = (c.x + side * petPx * 1.25f)
                        .coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx))
                    val ty = (c.y + side * petPx * 0.55f)
                        .coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
                    walkTo(Offset(tx, ty), stepMs = 24, steps = 46)
                    peeking = true
                    squishKey++
                    delay(720)
                    peeking = false
                    squishKey++
                    reaction = "Peekaboo!"
                    reactionKey++
                    lastTouch = System.currentTimeMillis()
                    continue
                }
                // v8.35 — sometimes Curie hides at the very bottom edge, only
                // its head peeking up over the lip (never mid-watch/spin).
                if (!watching && !CurioPet.spinning &&
                    System.currentTimeMillis() - lastPeekAt > 22_000L &&
                    Random.nextFloat() < 0.12f
                ) {
                    lastPeekAt = System.currentTimeMillis()
                    val edgeY = (maxH - petPx * 0.30f).coerceAtLeast(marginPx)
                    val tx = (maxW / 2f).coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx))
                    walkTo(Offset(tx, edgeY), stepMs = 24, steps = 40)
                    peeking = true
                    delay(900)
                    peeking = false
                    squishKey++
                    reaction = "Peekaboo!"
                    reactionKey++
                    lastTouch = System.currentTimeMillis()
                    continue
                }
                if (watching) {
                    // Glued to the Spin deck between pokes: stay and watch.
                    delay(300)
                    continue
                }
                // v8.21 — a bottom drawer (filter / category) is open: the
                // pet hurries over to the screen's bottom edge and hops up
                // and down, trying to peek over the drawer's lip.
                if (PetLandmarks.isSheetOpen(routePrefix) &&
                    System.currentTimeMillis() - lastPokeAt > 4_000L
                ) {
                    val edgeX = (maxW / 2f).coerceIn(
                        marginPx,
                        (maxW - petPx - marginPx).coerceAtLeast(marginPx)
                    )
                    val edgeY = (maxH - petPx - marginPx).coerceAtLeast(marginPx)
                    walkTo(Offset(edgeX, edgeY), stepMs = 15, steps = 40)
                    // Peek-hop: bob up and down at the edge a few times,
                    // like it's trying to see over the lip.
                    val baseY = pos.y
                    repeat(3) {
                        pos = pos.copy(y = (baseY - 26f).coerceAtLeast(marginPx))
                        delay(130)
                        pos = pos.copy(y = baseY)
                        delay(150)
                    }
                    squishKey++
                    heartsKey++
                    reaction = CurioPet.drawerLine()
                    reactionKey++
                    lastPokeAt = System.currentTimeMillis()
                    lastTouch = System.currentTimeMillis()
                    continue
                }
                // v8.11 — the pet sometimes starts a game on its own: a play
                // bow + a "catch me!" line, then it zooms off. v8.12 — how
                // often it does this comes from its GROWING PERSONALITY
                // (bouncy pets play a lot, sparky ones are shy).
                if (Random.nextFloat() < CurioPet.playfulBias(context)) {
                    CurioPet.notePlay(context)
                    fireReaction(PetReactionEvents.PLAY, CurioPet.playInitiation())
                    val tx = marginPx + Random.nextFloat() * (maxW - petPx - 2 * marginPx).coerceAtLeast(0f)
                    val ty = marginPx + Random.nextFloat() * (maxH - petPx - 2 * marginPx).coerceAtLeast(0f)
                    walkTo(Offset(tx, ty), stepMs = 16, steps = 44)
                    continue
                }
                // Normal wander — a downward bias keeps it grounded instead
                // of floating over the top bars. v8.35 — pick a spot that
                // does NOT cover a landmark (button/text), so wandering stays
                // out of the way; fall back to the last candidate.
                val avoid = PetLandmarks.forScreen(routePrefix)
                var tx = marginPx
                var ty = marginPx
                val tyBand = (maxH - petPx - 2 * marginPx).coerceAtLeast(0f)
                for (attempt in 0 until 6) {
                    val candX = marginPx + Random.nextFloat() * (maxW - petPx - 2 * marginPx).coerceAtLeast(0f)
                    val candY = if (Random.nextFloat() < 0.25f)
                        marginPx + Random.nextFloat() * tyBand * 0.35f
                    else
                        marginPx + tyBand * (0.35f + Random.nextFloat() * 0.65f)
                    tx = candX
                    ty = candY
                    val spot = Rect(Offset(candX, candY), Size(petPx, petPx))
                        .inflate(with(density) { 6.dp.toPx() })
                    if (avoid.none { it.bounds.overlaps(spot) }) break
                }
                // v8.9 — sometimes the pet 'thinks' (tilts + "?") before
                // walking; v8.11 also sometimes looks around after arriving.
                if (Random.nextFloat() < 0.45f) {
                    thinking = true
                    delay(620)
                    thinking = false
                }
                walkTo(Offset(tx, ty), stepMs = Random.nextLong(18, 34))
                thinking = false
                if (Random.nextFloat() < 0.2f) {
                    thinking = true
                    delay(450)
                    thinking = false
                }
                delay(Random.nextLong(1200, 3200))
            }
        }

        // ── Event reactions (v8.9) — real actions bump CurioPet events; the
        //    pet hops, cheers and (on saves) pops hearts. v8.35 — the
        //    animation + face come from the design's reaction rules.
        LaunchedEffect(CurioPet.eventCount) {
            val latest = CurioPet.lastEvent
            if (CurioPet.eventCount > seenEvents && latest != null) {
                seenEvents = CurioPet.eventCount
                val event = when (latest) {
                    CurioPet.Event.SPIN_LANDED -> PetReactionEvents.SPIN_LANDED
                    CurioPet.Event.REVEAL_TAPPED,
                    CurioPet.Event.REVEAL_AUTO -> PetReactionEvents.REVEAL
                    CurioPet.Event.EXPLORE -> PetReactionEvents.EXPLORE
                    CurioPet.Event.SAVE -> PetReactionEvents.SAVE
                }
                fireReaction(event, CurioPet.eventLine(latest))
                if (latest == CurioPet.Event.SAVE) heartsKey++
                // v8.53 — Phase 7: user-defined actions for app events fire
                // alongside the built-in reaction.
                when (latest) {
                    CurioPet.Event.REVEAL_TAPPED,
                    CurioPet.Event.REVEAL_AUTO -> fireCustomActions(PetActionTrigger.REVEAL)
                    CurioPet.Event.SAVE -> fireCustomActions(PetActionTrigger.SAVE)
                    else -> Unit
                }
            }
        }

        // ── Mood reactions: hop + excited line on EXCITED/PROUD ─────────
        // v8.35 — the LEVEL_UP reaction rule drives the animation + face.
        LaunchedEffect(Unit) {
            while (true) {
                delay(1200)
                val m = CurioPet.mood(context, CurioQuests.categoriesState, screenHint)
                if (lastMood != m) {
                    lastMood = m
                    if (m == CurioPet.Mood.EXCITED || m == CurioPet.Mood.PROUD) {
                        // App activity counts as interaction — the pet won't
                        // nap away mid-celebration.
                        fireReaction(
                            PetReactionEvents.LEVEL_UP,
                            CurioPet.lineFor(context, m, CurioQuests.categoriesState)
                        )
                        // v8.53 — a user-defined level-up action joins in.
                        fireCustomActions(PetActionTrigger.LEVEL_UP)
                    }
                }
            }
        }

        // ── Spin cheer (v8.13) — while the deck is reeling, the pet cheers
        //    it on with a line + a little bounce (once per spin).
        LaunchedEffect(CurioPet.spinning) {
            if (CurioPet.spinning && autoWander) {
                celebrateKey++
                reaction = CurioPet.spinCheer()
                reactionKey++
                lastTouch = System.currentTimeMillis()
            }
        }

        // ── Auto-nap: after a long idle, the pet goes home to bed ───────
        LaunchedEffect(Unit) {
            while (true) {
                delay(30_000)
                if (CurioPet.awake && !dragged &&
                    System.currentTimeMillis() - lastTouch > AUTO_NAP_AFTER_MS
                ) {
                    CurioPet.settleToSleep()
                }
            }
        }

        // ── Time-of-day custom actions (v8.53): a `time` action fires once
        //    when the clock reaches its hour. Guarded per hour so it never
        //    spams every check.
        LaunchedEffect(activeDesign) {
            var lastFiredHour = -1
            while (true) {
                delay(45_000)
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val timeActions = activeDesign.customActions.filter {
                    it.enabled && it.trigger.kind == PetActionTrigger.TIME && it.trigger.param == hour
                }
                if (timeActions.isNotEmpty() && lastFiredHour != hour) {
                    lastFiredHour = hour
                    playCustomAction(timeActions.random())
                }
            }
        }

        // ── Idle custom actions (v8.53): a gentle nudge when the pet has
        //    been untouched for the action's seconds (checked, then rested
        //    for a minute so it never loops non-stop).
        LaunchedEffect(activeDesign) {
            var lastFiredIdleAt = 0L
            while (true) {
                delay(15_000)
                val idleActions = activeDesign.customActions.filter {
                    it.enabled && it.trigger.kind == PetActionTrigger.IDLE
                }
                if (idleActions.isEmpty()) continue
                val now = System.currentTimeMillis()
                val minIdleMs = idleActions.minOf { it.trigger.param.coerceAtLeast(1) } * 1000L
                if (now - lastTouch > minIdleMs && now - lastFiredIdleAt > 60_000L) {
                    lastFiredIdleAt = now
                    val shortest = idleActions.firstOrNull {
                        it.trigger.param == (minIdleMs / 1000L).toInt()
                    }
                    playCustomAction(shortest ?: idleActions.random())
                }
            }
        }

        // Bubble lifecycle — fade + rise IN, hold a readable beat, fade OUT,
        // then clear. v8.26 — animated both ways so a new line never pops or
        // vanishes abruptly; the ~2.3s hold keeps reactions in the 2-3s range
        // (dizzy, cheers, home drops) so they read at a glance.
        LaunchedEffect(reactionKey) {
            if (reaction != null) {
                bubbleAnim.snapTo(0f)
                bubbleAnim.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                delay(2300)
                bubbleAnim.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                reaction = null
            }
        }

        // v8.21 — the dizzy spell wears off a beat after the drag ends.
        // v8.26 — 2.5s so a flung pet visibly reels before it settles.
        LaunchedEffect(recovering) {
            if (recovering) {
                delay(2500)
                recovering = false
            }
        }

        // v8.35 — a reaction face (from the reaction editor) lingers a beat
        // so the animation reads, then clears back to the mood face.
        LaunchedEffect(reactionFaceKey) {
            if (reactionFace != null) {
                delay(1400)
                reactionFace = null
            }
        }

        // ── Typing reaction (v8.35; v8.37 compile fix) — when the on-screen
        //    keyboard opens, Curie hurries up above it and types along on a
        //    tiny keyboard. Once per screen visit (60s cooldown), so it never
        //    spams. WindowInsets.ime is @Composable (it reads the window's
        //    live IME insets), so it's read HERE in composition — calling it
        //    inside snapshotFlow/collect (non-composable lambdas) was a CI
        //    compile failure (@Composable invocations in a non-composable
        //    context). The recomposition on IME change re-keys the effect.
        val imeBottomPx = WindowInsets.ime.getBottom(density)
        LaunchedEffect(routePrefix, imeBottomPx, maxW, maxH) {
            if (imeBottomPx > 0 && autoWander && CurioPet.awake && !dragged && !CurioPet.atHome) {
                val now = System.currentTimeMillis()
                if (now - lastTypingAt > 60_000L || lastTypingScreen != routePrefix) {
                    lastTypingAt = now
                    lastTypingScreen = routePrefix
                    typingReaction = true
                    lastTouch = System.currentTimeMillis()
                    val targetY = (maxH - imeBottomPx - petPx - with(density) { 10.dp.toPx() })
                        .coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
                    pos = Offset(
                        (maxW / 2f).coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx)),
                        targetY
                    )
                    facing = 1f
                    squishKey++
                    reaction = "Tap tap tap! I can type too!"
                    reactionKey++
                }
            } else {
                typingReaction = false
            }
        }

        // Long-press: fade out, then hop back into the flower bed (the bed
        // shows the pet sitting there until tapped to come out again).
        LaunchedEffect(leavingHome) {
            if (leavingHome) {
                appear.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                CurioPet.goHome()
                leavingHome = false
            }
        }

        // v8.20 — the pet rides a cute cloud while it walks: drawn UNDER
        // the sprite (a sibling before it), fading in/out with movement.
        CloudRide(
            visible = moving,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (pos.x + petPx / 2f - with(density) { CLOUD_W.toPx() } / 2f).roundToInt(),
                        // v8.26 — tucked higher up under the feet so the
                        // smaller puff reads as a proper ride, not a mat.
                        (pos.y + petPx * 0.72f).roundToInt()
                    )
                }
                .size(CLOUD_W, CLOUD_H)
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .size(FLOAT_SIZE)
                .graphicsLayer {
                    alpha = appear.value
                    scaleX = 0.5f + 0.5f * appear.value
                    scaleY = 0.5f + 0.5f * appear.value
                }
                .pointerInput(maxW, maxH, petPx, marginPx, routePrefix) {
                    // v8.20 — self-heal a drag interrupted by navigation:
                    // keying this on routePrefix cancels the coroutine on a
                    // tab switch WITHOUT firing onDragCancel, which would
                    // leave `dragged` stuck true (wander pause + no auto-nap
                    // forever). The new coroutine starts immediately, so
                    // clearing the flags here resets any stale state.
                    dragged = false
                    dizzy = false
                    recovering = false
                    gliding = false
                    peeking = false
                    typingReaction = false
                    PetLandmarks.setHovered("bed", false)
                    // v8.26 — throw momentum: the drag tracks its own
                    // velocity; on release the pet keeps a little of the
                    // fling and slides on with friction before settling.
                    // The glide runs on the composition-level [glideScope]
                    // (the drag callbacks are plain lambdas with no scope
                    // receiver).
                    var lastDragPos: Offset? = null
                    var lastDragAt = 0L
                    var dragVelX = 0f
                    var dragVelY = 0f
                    var glideJob: Job? = null
                    detectDragGestures(
                        onDragStart = {
                            glideJob?.cancel()
                            gliding = false
                            // v8.26 — a fresh drag starts with clean velocity
                            // so a quick re-grab never inherits a throw's
                            // momentum.
                            lastDragPos = null
                            lastDragAt = 0L
                            dragVelX = 0f
                            dragVelY = 0f
                            dragged = true
                            // v8.21 — flinging it around makes it dizzy.
                            dizzy = true
                            peeking = false
                            typingReaction = false
                            dragStartAt = System.currentTimeMillis()
                            lastTouch = System.currentTimeMillis()
                            // v8.20 — a fresh drag starts clear of the bed.
                            PetLandmarks.setHovered("bed", false)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            pos = Offset(
                                (pos.x + amount.x).coerceIn(
                                    marginPx,
                                    (maxW - petPx - marginPx).coerceAtLeast(marginPx)
                                ),
                                (pos.y + amount.y).coerceIn(
                                    marginPx,
                                    (maxH - petPx - marginPx).coerceAtLeast(marginPx)
                                )
                            )
                            if (amount.x != 0f) facing = if (amount.x > 0f) 1f else -1f
                            // Rolling velocity estimate (px/s), blended so a
                            // single jittery frame can't spike the fling.
                            val now = System.currentTimeMillis()
                            val prev = lastDragPos
                            lastDragPos = Offset(pos.x, pos.y)
                            if (prev != null && now > lastDragAt) {
                                val dt = (now - lastDragAt) / 1000f
                                if (dt > 0f) {
                                    val vx = (pos.x - prev.x) / dt
                                    val vy = (pos.y - prev.y) / dt
                                    dragVelX = dragVelX * 0.55f + vx * 0.45f
                                    dragVelY = dragVelY * 0.55f + vy * 0.45f
                                }
                            }
                            lastDragAt = now
                            // v8.20 — glow the flower bed while the pet is
                            // dragged over it (the drop target reads before
                            // the drop). No-op on screens without a bed.
                            val bed = PetLandmarks.forScreen(routePrefix)
                                .firstOrNull { it.id == "bed" }
                            if (bed != null) {
                                val dropRect = bed.bounds.inflate(
                                    with(density) { DROP_FORGIVENESS.toPx() }
                                )
                                PetLandmarks.setHovered(
                                    "bed",
                                    Rect(Offset(pos.x, pos.y), Size(petPx, petPx))
                                        .overlaps(dropRect)
                                )
                            }
                        },
                        onDragEnd = {
                            dragged = false
                            dizzy = false
                            lastTouch = System.currentTimeMillis()
                            // v8.21 — the release leaves it dizzy for a beat
                            // (swirl eyes + wobble) with a groggy line — but
                            // only when the drag actually flung it around.
                            val flung = System.currentTimeMillis() - dragStartAt > 900L
                            // v8.20 — drop the pet onto its flower bed to
                            // send it home (hover glow off either way).
                            val bed = PetLandmarks.forScreen(routePrefix)
                                .firstOrNull { it.id == "bed" }
                            if (bed != null) {
                                val dropRect = bed.bounds.inflate(
                                    with(density) { DROP_FORGIVENESS.toPx() }
                                )
                                val dropped = Rect(Offset(pos.x, pos.y), Size(petPx, petPx))
                                    .overlaps(dropRect)
                                PetLandmarks.setHovered("bed", false)
                                if (dropped) {
                                    squishKey++
                                    heartsKey++
                                    reaction = "Home sweet home!"
                                    reactionKey++
                                    leavingHome = true
                                }
                            }
                            if (flung && !leavingHome) {
                                recovering = true
                                reaction = CurioPet.dizzyLine()
                                reactionKey++
                            }
                            // v8.26 — throw momentum: a real fling keeps a
                            // LITTLE of its speed on release (capped, and
                            // friction-worn) so the pet glides a short way
                            // in the thrown direction before settling.
                            val speed = hypot(dragVelX, dragVelY)
                            val flingMin = with(density) { 350.dp.toPx() }
                            val flingCap = with(density) { 620.dp.toPx() }
                            if (speed > flingMin) {
                                val v0 = speed.coerceAtMost(flingCap)
                                val dirX = dragVelX / speed
                                val dirY = dragVelY / speed
                                gliding = true
                                glideJob = glideScope.launch {
                                    try {
                                        var v = v0
                                        var px = pos.x
                                        var py = pos.y
                                        val decay = 8f
                                        val stop = with(density) { 26.dp.toPx() }
                                        var lastN = System.nanoTime()
                                        while (v > stop && !dragged && !leavingHome) {
                                            val nowN = System.nanoTime()
                                            val dt = ((nowN - lastN) / 1_000_000_000f)
                                                .coerceIn(0f, 0.05f)
                                            lastN = nowN
                                            v -= v * decay * dt
                                            px += dirX * v * dt
                                            py += dirY * v * dt
                                            pos = Offset(
                                                px.coerceIn(
                                                    marginPx,
                                                    (maxW - petPx - marginPx).coerceAtLeast(marginPx)
                                                ),
                                                py.coerceIn(
                                                    marginPx,
                                                    (maxH - petPx - marginPx).coerceAtLeast(marginPx)
                                                )
                                            )
                                            if (dirX != 0f) facing = if (dirX > 0f) 1f else -1f
                                            delay(16)
                                        }
                                    } finally {
                                        gliding = false
                                    }
                                }
                            }
                            dragVelX = 0f
                            dragVelY = 0f
                        },
                        onDragCancel = {
                            glideJob?.cancel()
                            gliding = false
                            dragged = false
                            dizzy = false
                            recovering = false
                            lastTouch = System.currentTimeMillis()
                            PetLandmarks.setHovered("bed", false)
                        }
                    )
                }
                // v8.20 — keyed on routePrefix too: the tap handler reads
                // `watching` (from routePrefix), so navigating must restart
                // it or a stale Spin-screen value lingers after a tab switch.
                .pointerInput(routePrefix) {
                    detectTapGestures(
                        onTap = {
                            lastTouch = System.currentTimeMillis()
                            // Every pet feeds the persona (v8.12).
                            CurioPet.noteTouch(context)
                            // Escalation: quick repeated taps push the pet
                            // from a boop to a play-bow to a happy
                            // celebration (v8.21 — no more tap-dizzy).
                            val now = System.currentTimeMillis()
                            tapStreak = if (now - lastTapAt < 1600L) tapStreak + 1 else 1
                            lastTapAt = now
                            val tier = tapStreak.coerceAtMost(3)
                            // v8.35 — the TOUCH reaction rule's face, and
                            // fewer words: only ~40% of taps show a line so
                            // the reaction is mostly motion.
                            val rule = activeDesign.reactionFor(PetReactionEvents.TOUCH)
                            if (rule.enabled) {
                                reactionFace = rule.face
                                reactionFaceKey++
                                if (Random.nextFloat() < 0.4f) {
                                    val builtInLine = CurioPet.touchReaction(tier)
                                    reaction = if (AppPreferences.customReactionLinesState) {
                                        rule.lines.randomOrNull() ?: builtInLine
                                    } else {
                                        builtInLine
                                    }
                                    reactionKey++
                                }
                                when (tier) {
                                    // v8.21 — tapping never spins it dizzy anymore
                                    // (that's for dragging): boop → play-bow → a
                                    // big happy celebration hop.
                                    1 -> squishKey++
                                    2 -> playKey++
                                    else -> {
                                        // v8.35 — the biggest taps add a
                                        // celebratory twirl.
                                        celebrateKey++
                                        spinKey++
                                    }
                                }
                                // v8.21 — hearts for the playful/celebrate taps
                                // only, so a plain boop stays clean.
                                if (tier >= 2) heartsKey++
                            }
                            // v8.53 — user-defined tap actions fire after
                            // the built-in touch reaction.
                            fireCustomActions(PetActionTrigger.TAP)
                            // The pet dashes to a nearby spot after the
                            // reaction — it wants to play (not in reduced
                            // motion, and not while watching the Spin deck;
                            // the wander loop is what moves it).
                            if (autoWander && !watching) {
                                val tx = (pos.x + Random.nextFloat() * 140f - 70f)
                                    .coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx))
                                val ty = (pos.y + Random.nextFloat() * 120f - 60f)
                                    .coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
                                playDartTarget = Offset(tx, ty)
                            }
                        },
                        onLongPress = {
                            lastTouch = System.currentTimeMillis()
                            // v8.53 — user-defined long-press actions fire
                            // before the pet heads home.
                            fireCustomActions(PetActionTrigger.LONG_PRESS)
                            tapStreak = 0 // a fresh start when it comes home
                            squishKey++
                            heartsKey++
                            reaction = "Home sweet home!"
                            reactionKey++
                            leavingHome = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // v8.53 — the custom action's current frame drives the sprite's
            // per-frame pixel layers + transform while it plays; when no
            // custom action is running the frame is null and everything
            // falls back to the normal look.
            val caFrame = customActionAnim?.frames?.getOrNull(customActionFrame)
            CurioPetSprite(
                stage = CurioPet.currentStage(),
                mood = CurioPet.mood(context, CurioQuests.categoriesState, screenHint),
                spriteSize = FLOAT_SIZE * 0.92f,
                celebrateKey = celebrateKey,
                squishKey = squishKey,
                playKey = playKey,
                spinKey = spinKey,
                moving = moving,
                dragged = dragged,
                facing = facing,
                thinking = thinking,
                watching = watching,
                spinning = CurioPet.spinning,
                // v8.21 — swirls + wobble while flung, and while recovering.
                dizzy = dizzy || recovering,
                // v8.35 — the reaction editor's face + the hide-and-peek pose.
                // v8.53 — a custom action's animation mood wins while playing.
                faceOverride = customActionAnim?.let { activeDesign.faceFor(it.mood) } ?: reactionFace,
                // v8.53 — per-frame pixel layers of the custom animation.
                bodyOverride = caFrame?.bodyRows,
                curledOverride = caFrame?.curledRows,
                eyeOverride = caFrame?.eyeGrid,
                peeking = peeking,
                contentDescription = "Curie, your companion pet. Drag it anywhere, tap to say hi",
                modifier = Modifier.graphicsLayer {
                    translationY = (caFrame?.offsetY ?: 0f).dp.toPx()
                    scaleX = caFrame?.scale ?: 1f
                    scaleY = caFrame?.scale ?: 1f
                    rotationZ = caFrame?.rotationDegrees ?: 0f
                }
            )
        }
        // v8.13 — hearts rise ABOVE the pet in their own offset sibling
        // (like the bubble), so they never cover the face. Bottom of the
        // hearts box hugs the pet's head; hearts float up and fade.
        HeartsOverlay(
            key = heartsKey,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (pos.x + petPx / 2f - with(density) { HEARTS_W.toPx() } / 2f).roundToInt(),
                        // Lifted a touch clear of the speech bubble and
                        // clamped so it never flies off-screen at the top.
                        (pos.y - with(density) { (HEARTS_H + 10.dp).toPx() })
                            .coerceAtLeast(marginPx)
                            .roundToInt()
                    )
                }
                .size(HEARTS_W, HEARTS_H)
        )

        // v8.35 — while the user's keyboard is open, Curie types along on a
        // tiny pixel keyboard (beside the bubble, above the pet).
        TypingKeyboard(
            visible = typingReaction,
            accent = accentColor,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (pos.x + petPx / 2f - with(density) { TYPING_W.toPx() } / 2f).roundToInt(),
                        (pos.y - with(density) { 34.dp.toPx() }).roundToInt()
                    )
                }
                .size(TYPING_W, TYPING_H)
        )

        // Tiny reaction bubble floating just above the pet. Drawn as a
        // separate offset sibling (never inside the pet's touch box) so it
        // can wrap freely and never eats the pet's taps. The offset box is
        // lifted by the pet's own height and the bubble bottom-aligns to it,
        // so its tail touches the pet's head (padding is never used to move
        // it — that would overlay the face).
        reaction?.let { text ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            pos.x.roundToInt().coerceIn(marginPx.toInt(), (maxW - with(density) { 160.dp.toPx() }).toInt()),
                            (pos.y - with(density) { FLOAT_SIZE.toPx() }).roundToInt()
                        )
                    }
                    .height(FLOAT_SIZE)
                    // v8.26 — fade + gentle rise so the bubble glides in and
                    // out instead of snapping (driven by [bubbleAnim]).
                    .graphicsLayer {
                        alpha = bubbleAnim.value
                        translationY = (1f - bubbleAnim.value) * 8.dp.toPx()
                    }
            ) {
                PetSpeechBubble(
                    text = text,
                    tailOnLeft = false,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

/**
 * v8.35 — the tiny pixel keyboard Curie "types" on while the user's own
 * keyboard is open: a row of keys with one pulsing in turn (a paw tapping
 * its way across).
 */
@Composable
private fun TypingKeyboard(visible: Boolean, accent: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.92f else 0f,
        animationSpec = tween(200),
        label = "typingAlpha"
    )
    val flash = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            flash.snapTo(0f)
            while (true) {
                flash.animateTo(1f, tween(110, easing = LinearEasing))
                flash.snapTo(0f)
            }
        }
    }
    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val keyW = size.width / 7f
        val active = (flash.value * 7f).toInt().coerceIn(0, 6)
        for (i in 0 until 7) {
            val x = i * keyW + with(density) { 1.dp.toPx() }
            val w = keyW - with(density) { 2.dp.toPx() }
            drawRoundRect(
                color = if (i == active) accent else Color(0xFFFFFFFF),
                topLeft = Offset(x, 0f),
                size = Size(w, size.height),
                cornerRadius = CornerRadius(with(density) { 5.dp.toPx() })
            )
        }
    }
}

/** v8.35 — parses a design hex into a Compose color (safe fallback). */
private fun designColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(Color(0xFFFF6F61))

/**
 * Pink hearts that float UP from just above the pet's head and fade
 * (v8.13). Drawn in a sibling box offset above the pet, so they never
 * cover the sprite's face. Starts near the bottom of this box (the pet's
 * head) and rises to the top, shrinking as it fades.
 */
@Composable
private fun HeartsOverlay(key: Int, modifier: Modifier = Modifier) {
    val rise = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key > 0) {
            rise.snapTo(0f)
            fade.snapTo(1f)
            rise.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
            // v8.21 — fade the whole burst OUT at the end so no heart ever
            // lingers over the pet's head (the old modulo math left one
            // fully-visible forever).
            fade.animateTo(0f, tween(280))
        }
    }
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        repeat(3) { i ->
            val t = (rise.value + i * 0.22f).coerceIn(0f, 1f)
            val alpha = (1f - t) * fade.value
            // v8.21 — smaller hearts.
            val s = size.minDimension * (0.065f + t * 0.03f)
            val x = centerX + (i - 1) * size.width * 0.2f
            val y = size.height * 0.95f - t * size.height * 0.95f
            drawHeart(x, y, s, Color(0xFFF7AFAF).copy(alpha = alpha * 0.95f))
        }
    }
}

/**
 * v8.20 — the puff of cloud the pet rides while it walks. Drawn in its own
 * offset sibling under the sprite (never inside its touch box). v8.23 —
 * REDESIGNED as a PIXEL cloud in the pet's own style: soft rounded pixels
 * (the same drawing language as the sprite) with a fluffy three-lobe
 * silhouette, cool top highlights and a shaded base — detailed, fluffy and
 * on-style instead of a blurry vector blob. v8.26 — smaller (80×32) and on
 * a 20-wide grid so every row of the pixel art renders. Bobbing gently,
 * fading in with movement so it only shows on the go.
 */
@Composable
private fun CloudRide(visible: Boolean, modifier: Modifier = Modifier) {
    val cloudAlpha by animateFloatAsState(
        targetValue = if (visible) 0.92f else 0f,
        animationSpec = tween(240),
        label = "petCloudAlpha"
    )
    // v8.20 — the bob only runs while the cloud is shown: an idle pet never
    // ticks this animation, so an invisible cloud doesn't burn frames. When
    // [visible] flips, the effect restarts and the loop picks up cleanly.
    val bob = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            bob.snapTo(0f)
            while (true) {
                bob.animateTo(1f, tween(640, easing = FastOutSlowInEasing))
                bob.animateTo(0f, tween(640, easing = FastOutSlowInEasing))
            }
        }
    }
    Canvas(
        modifier = modifier.graphicsLayer {
            this.alpha = cloudAlpha
            // A gentle ride: bob up/down and a soft swell.
            translationY = (bob.value * 3f - 1.5f).dp.toPx()
            scaleX = 1f + bob.value * 0.04f
            scaleY = 1f + bob.value * 0.04f
        }
    ) {
        // v8.23 — the pixel cloud: 'H' crisp highlights, 'W' fluffy body,
        // 'w' under-shade, 'd' deep base. Every cell is a slightly-rounded,
        // slightly-overlapping square — the sprite's own softened pixel look.
        val highlight = Color.White
        val body = Color(0xFFF6F8FE)
        val shade = Color(0xFFDCE3F2)
        val deep = Color(0xFFC9D2E8)
        val cloud = listOf(
            ".....HHHHHWW......",
            "...HWWWWWWWWWW....",
            "..WWWWWWWWWWWWWW..",
            ".WWWWWWWWWWWWWWWW.",
            ".WWWWWWWWWWWWWWWW.",
            "WWWWWWWWWWWWWWWWWW",
            "wWWWWWWWWWWWWWWWWw",
            ".dddddddddddddddd."
        )
        val px = size.width / CLOUD_GRID_W
        fun pxAt(col: Int, row: Int, color: Color, alpha: Float = 1f) {
            if (col !in 0 until CLOUD_GRID_W || row !in 0 until CLOUD_GRID_H) return
            drawRoundRect(
                color = color.copy(alpha = alpha),
                // v8.23 — center the 6% overlap so the cloud's outer edges
                // round symmetrically instead of clipping at the canvas.
                topLeft = Offset(col * px - px * 0.03f, row * px - px * 0.03f),
                size = Size(px * 1.06f, px * 1.06f),
                cornerRadius = CornerRadius(px * 0.18f)
            )
        }
        cloud.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch ->
                when (ch) {
                    'H' -> pxAt(col, row, highlight)
                    'W' -> pxAt(col, row, body)
                    'w' -> pxAt(col, row, shade)
                    'd' -> pxAt(col, row, deep)
                }
            }
        }
    }
}

private fun DrawScope.drawHeart(cx: Float, cy: Float, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy + s * 0.42f)
        cubicTo(cx - s, cy - s * 0.12f, cx - s * 0.55f, cy - s * 0.72f, cx, cy - s * 0.2f)
        cubicTo(cx + s * 0.55f, cy - s * 0.72f, cx + s, cy - s * 0.12f, cx, cy + s * 0.42f)
        close()
    }
    drawPath(path, color)
}

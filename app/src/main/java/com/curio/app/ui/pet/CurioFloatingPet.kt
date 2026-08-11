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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.data.CustomPetAction
import com.curio.app.data.PetActionTrigger
import com.curio.app.data.PetAnimation
import com.curio.app.data.PetDesign
import com.curio.app.data.PetFace
import com.curio.app.data.PetLifeDirector
import com.curio.app.data.PetLifeRoutine
import com.curio.app.data.PetReactionEvents
import com.curio.app.data.TourController
import com.curio.app.data.ReactionAnim
import com.curio.app.data.animationById
import com.curio.app.navigation.CurioRoutes
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
// v9.x — scaled to the pet's 72dp box: the cloud is ~¾ of the pet's width
// (was 80×32 — wider than the pet itself), so it reads as a ride under a
// SMALL pet instead of a mat that dwarfs it.
private val CLOUD_W = 56.dp
private val CLOUD_H = CLOUD_W * 0.4f
private const val CLOUD_GRID_W = 20
private const val CLOUD_GRID_H = 8
// v8.20 — how close a drop must be to the flower bed to count as "home".
private val DROP_FORGIVENESS = 12.dp
// v8.35 — the tiny pixel keyboard Curie types on while the user types.
private val TYPING_W = 150.dp
private val TYPING_H = 60.dp
// v9.x — Curie's typing keyboard renders 5× smaller (a tiny thing) so it
// never competes with the user's own keyboard. Tunable in one place.
private val TYPING_SCALE = 0.2f

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
 *    v11 — a tap / drag / app event SKIPS whatever bubble is showing and
 *    answers immediately instead of waiting behind (or cycling through)
 *    queued chatter; ambient lines still queue, but capped so they never
 *    pile up.
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
    val tourActive = TourController.active
    if ((!AppPreferences.petEnabledState || !AppPreferences.floatingPetEnabledState) && !tourActive) return
    if (!CurioPet.awake || CurioPet.atHome) return

    // Reduced motion: no autonomous wandering — the pet still follows touch.
    val animatorScale = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    val autoWander = animatorScale > 0f

    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlayOrigin = coordinates.positionInWindow()
            }
    ) {
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
        var reactionQueue by remember { mutableStateOf<List<String>>(emptyList()) }
        var reactionKey by remember { mutableIntStateOf(0) }
        /**
         * Keep one speech line on screen at a time. Reactions can arrive from
         * several independent effects in the same frame (for example an app
         * event plus a custom action); queue later lines instead of replacing
         * the visible line and restarting its animation. v11 — the backlog is
         * CAPPED so ambient chatter can never pile up into a long cycle of
         * stale lines: the pet repeats at most the latest one or two, then
         * falls quiet.
         */
        fun queueReaction(line: String) {
            if (reaction == null && reactionQueue.isEmpty()) {
                reaction = line
                reactionKey++
            } else {
                reactionQueue = (reactionQueue + line).takeLast(2)
            }
        }

        /**
         * v11 — speak a line RIGHT NOW: interrupts whatever bubble is showing
         * (skips it) and drops any queued backlog, so a direct interaction (a
         * tap, a drag, an app event) is answered immediately instead of
         * waiting behind — and cycling through — ambient chatter. A null line
         * simply dismisses the current bubble (the pet still reacts with its
         * motion) without speaking.
         */
        fun speakNow(line: String?) {
            reactionQueue = emptyList()
            reaction = line
            reactionKey++
        }
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
        // but never spams the same thing every beat. v9.x — the window grew
        // from 4s to 12s so button pokes read as occasional, not hovering.
        // On the Spin screen the wander beat cycles every ~300ms (the
        // watching gate exits the wait loop early), so without this the pet
        // would boop the Shuffle button almost constantly while the deck
        // waits.
        var lastPokeAt by remember { mutableStateOf(0L) }
        val appear = remember { Animatable(0f) }
        // v9.x — chameleon-game opacity: the pet fades to a faint outline
        // ("camouflage"), then pops back at a fresh spot. Multiplied into
        // the sprite's own alpha so the two fades compose cleanly.
        val chameleonAlpha = remember { Animatable(1f) }
        // v8.9 — on the Spin screen the pet stops to watch the deck; event
        // reactions start from the current count so stale events never fire.
        val watching = routePrefix?.startsWith("spin") == true
        // The Tour turns the ordinary wanderer into a deliberate guide: read
        // the currently registered landmark so Curie can walk beside the real
        // control instead of merely speaking from a random corner.
        val tourStep = TourController.currentStep
        var tourBubbleSize by remember { mutableStateOf(IntSize.Zero) }
        val tourLandmark = if (tourStep != null && routePrefix == tourStep.routePrefix) {
            PetLandmarks.forScreen(routePrefix).firstOrNull { it.id == tourStep.landmarkId }
        } else null
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
        // Pet Life routines are short authored scenes selected from the
        // current screen + personality. Keep the last few ids out of the
        // picker so autonomy feels varied instead of random-but-repetitive.
        var routineAnim by remember { mutableStateOf<PetAnimation?>(null) }
        var routineFrame by remember { mutableIntStateOf(0) }
        var routineKey by remember { mutableIntStateOf(0) }
        var routineView by remember { mutableStateOf(com.curio.app.data.PetViewAngle.FRONT) }
        var recentRoutineIds by remember { mutableStateOf<List<String>>(emptyList()) }

        /** Starts one contextual Pet Life routine and remembers its id. */
        fun playPetLifeRoutine(routine: PetLifeRoutine) {
            // A routine is a complete little scene: let it finish before a
            // second idle trigger can replace its frames or speech bubble.
            if (routineAnim != null || customActionAnim != null) return
            val anim = activeDesign.animations[routine.animationId]
                ?: animationById(routine.animationId)
                ?: return
            if (anim.frames.isEmpty()) return
            routineAnim = anim
            routineView = routine.view
            routineFrame = 0
            routineKey++
            recentRoutineIds = (listOf(routine.id) + recentRoutineIds).distinct().take(5)
            lastTouch = System.currentTimeMillis()
            routine.line?.let(::queueReaction)
        }

        /**
         * Steps a Pet Life routine once. Custom user actions still take
         * priority when both happen at the same time.
         */
        LaunchedEffect(routineKey, routineAnim) {
            val anim = routineAnim ?: return@LaunchedEffect
            for (i in anim.frames.indices) {
                routineFrame = i
                delay(anim.frames[i].durationMs.toLong())
            }
            routineAnim = null
            routineView = com.curio.app.data.PetViewAngle.FRONT
        }

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
                // v11 — a real event (spin landed, reveal, save, level-up)
                // speaks NOW: it skips whatever bubble is showing so the pet
                // reacts to what the user just did instead of finishing old
                // chatter first. A null line leaves the current bubble alone.
                speakNow(
                    if (AppPreferences.customReactionLinesState) {
                        rule.lines.randomOrNull() ?: line
                    } else {
                        line
                    }
                )
            }
        }

        /**
         * v8.53 — plays one user-defined custom action: resolves its
         * animation (built-in or drawn in the designer), starts the frame
         * stepper, wears the animation's mood face, and speaks a random
         * saved line when the action has any.
         */
        fun playCustomAction(action: CustomPetAction) {
            // Custom authored actions take priority over ambient Pet Life;
            // cancel the routine so the two scenes never overlap.
            if (routineAnim != null) {
                routineAnim = null
                routineView = com.curio.app.data.PetViewAngle.FRONT
            }
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
                queueReaction(line)
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

        // During the Tour, move once to the current real landmark and stay
        // there until the user advances. This keeps the guide legible and
        // prevents autonomous pokes from triggering unrelated UI behavior.
        LaunchedEffect(tourStep?.id, tourLandmark?.bounds, overlayOrigin, maxW, maxH) {
            val step = tourStep ?: return@LaunchedEffect
            val landmark = tourLandmark ?: return@LaunchedEffect
            val center = landmark.bounds.center - overlayOrigin
            val side = if (center.x > maxW / 2f) -1f else 1f
            val target = Offset(
                (center.x + side * petPx * 0.95f)
                    .coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx)),
                (center.y - petPx / 2f)
                    .coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
            )
            facing = if (target.x >= pos.x) 1f else -1f
            moving = true
            val start = pos
            repeat(34) { frame ->
                if (TourController.currentStep?.id != step.id || dragged) return@LaunchedEffect
                val t = (frame + 1) / 34f
                pos = Offset(
                    start.x + (target.x - start.x) * t,
                    start.y + (target.y - start.y) * t
                )
                delay(18)
            }
            moving = false
        }

        // v9.x — the pet EMERGES FROM ITS HOME on the Home screen: instead
        // of dropping into the screen corner, it SNAPS beside the house the
        // moment the bed landmark is measured (the appear hop is still
        // playing, so it reads as the pet popping out of its home — never a
        // corner flash). One-time per appearance (the flag resets when the
        // pet is sent home and comes back out, since the overlay leaves and
        // re-enters composition).
        val bedLandmark = PetLandmarks.forScreen(CurioRoutes.HOME)
            .firstOrNull { it.id == "bed" }
        val settledAtHome = remember { mutableStateOf(false) }
        LaunchedEffect(bedLandmark?.bounds, overlayOrigin, maxW, maxH) {
            if (settledAtHome.value) return@LaunchedEffect
            if (tourActive) return@LaunchedEffect
            if (routePrefix != CurioRoutes.HOME) return@LaunchedEffect
            val bed = bedLandmark ?: return@LaunchedEffect
            settledAtHome.value = true
            if (dragged) return@LaunchedEffect
            // Stand on the same floor line as the house, on the side away
            // from the screen edge, facing it (like it just walked out).
            val local = Rect(
                left = bed.bounds.left - overlayOrigin.x,
                top = bed.bounds.top - overlayOrigin.y,
                right = bed.bounds.right - overlayOrigin.x,
                bottom = bed.bounds.bottom - overlayOrigin.y
            )
            val side = if (local.center.x > maxW / 2f) -1f else 1f
            val gap = petPx * 0.18f
            val targetX = if (side > 0f) local.right + gap else local.left - petPx - gap
            pos = Offset(
                targetX.coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx)),
                (local.bottom - petPx).coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
            )
            facing = if (side > 0f) -1f else 1f
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
        LaunchedEffect(maxW, maxH, autoWander, CurioPet.awake, watching, routePrefix, tourStep?.id) {
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
                if (TourController.currentStep != null) {
                    delay(240)
                    continue
                }
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
                    // v12 — silent play: the tap already answered with its own
                    // (gated) line; firing the PLAY event here replaced it
                    // with a generic play line after every single tap.
                    CurioPet.notePlay(context, react = false)
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
                // the deck is idle on the spin screen. v9.x — the 12s
                // cooldown keeps pokes occasional (3× rarer than the old 4s)
                // even where the beat loop cycles fast.
                if (!CurioPet.spinning && landmarks.isNotEmpty() &&
                    System.currentTimeMillis() - lastPokeAt > 12_000L &&
                    Random.nextFloat() < 0.45f
                ) {
                    val target = landmarks.random()
                    playPetLifeRoutine(
                        PetLifeDirector.choose(
                            screen = routePrefix,
                            landmarkKind = target.kind.name,
                            persona = CurioPet.persona(context),
                            recentIds = recentRoutineIds.toSet()
                        )
                    )
                    // v9.x — walk right UP TO the thing instead of landing in
                    // a random offset box around it: convert the landmark's
                    // WINDOW bounds to overlay-local space, then stand on its
                    // nearest edge (same vertical center, a small gap) so the
                    // approach reads as walking to the button.
                    val local = Rect(
                        left = target.bounds.left - overlayOrigin.x,
                        top = target.bounds.top - overlayOrigin.y,
                        right = target.bounds.right - overlayOrigin.x,
                        bottom = target.bounds.bottom - overlayOrigin.y
                    )
                    val gap = petPx * 0.16f
                    val side = if (local.left - marginPx >=
                        maxW - petPx - marginPx - local.right
                    ) -1f else 1f
                    val tx = (if (side > 0f) local.right + gap else local.left - petPx - gap)
                        .coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx))
                    val ty = (local.center.y - petPx / 2f)
                        .coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
                    // Poke ONLY when the pet actually reached the button — an
                    // interrupted walk (drag / glide) must never interact
                    // from across the screen.
                    fun arrived(): Boolean {
                        val dx = pos.x - tx
                        val dy = pos.y - ty
                        return dx * dx + dy * dy <= (petPx * 0.4f) * (petPx * 0.4f)
                    }
                    when (target.kind) {
                        PetLandmarks.Kind.FUN -> {
                            // Eager approach — quick happy steps, then a
                            // boop with hearts.
                            if (Random.nextFloat() < 0.5f) playKey++
                            walkTo(Offset(tx, ty), stepMs = 15, steps = 44)
                            if (arrived()) {
                                PetLandmarks.poke(target.id)
                                squishKey++
                                heartsKey++
                            }
                            // The selected Pet Life routine owns the speech
                            // bubble; this avoids replacing its contextual line
                            // with the old generic landmark phrase.
                            lastTouch = System.currentTimeMillis()
                        }
                        PetLandmarks.Kind.CURIOUS -> {
                            // Curious tiptoe — slow steps, a read-tilt, then
                            // a gentle poke.
                            thinking = true
                            walkTo(Offset(tx, ty), stepMs = 36, steps = 56)
                            thinking = false
                            delay(420)
                            if (arrived()) PetLandmarks.poke(target.id)
                            lastTouch = System.currentTimeMillis()
                        }
                        PetLandmarks.Kind.PLAY -> {
                            // v8.17 — a SPECIAL spot (the pet's flower bed):
                            // an eager dash over, a poke (the spot springs a
                            // beat), then a little happy jig — a squish
                            // bounce, a play-bow and a twirl.
                            walkTo(Offset(tx, ty), stepMs = 15, steps = 44)
                            if (arrived()) {
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
                            }
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
                    playPetLifeRoutine(
                        PetLifeDirector.choose(
                            screen = routePrefix,
                            landmarkKind = target.kind.name,
                            persona = CurioPet.persona(context),
                            recentIds = recentRoutineIds.toSet()
                        )
                    )
                    val c = target.bounds.center
                    val side = if (Random.nextFloat() < 0.5f) -1 else 1
                    val tx = (c.x + side * petPx * 1.25f)
                        .coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx))
                    val ty = (c.y + side * petPx * 0.55f)
                        .coerceIn(marginPx, (maxH - petPx - marginPx).coerceAtLeast(marginPx))
                    walkTo(Offset(tx, ty), stepMs = 24, steps = 46)
                    peeking = true
                    squishKey++
                    // v9.x — hide-and-peek talks sometimes: a soft peek-a-boo
                    // line instead of always playing the crouch in silence.
                    if (Random.nextFloat() < 0.55f) queueReaction(CurioPet.peekLine())
                    delay(720)
                    peeking = false
                    squishKey++
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
                    playPetLifeRoutine(
                        PetLifeDirector.choose(
                            screen = routePrefix,
                            landmarkKind = null,
                            persona = CurioPet.persona(context),
                            recentIds = recentRoutineIds.toSet()
                        )
                    )
                    val edgeY = (maxH - petPx * 0.30f).coerceAtLeast(marginPx)
                    val tx = (maxW / 2f).coerceIn(marginPx, (maxW - petPx - marginPx).coerceAtLeast(marginPx))
                    walkTo(Offset(tx, edgeY), stepMs = 24, steps = 40)
                    peeking = true
                    if (Random.nextFloat() < 0.55f) queueReaction(CurioPet.peekLine())
                    delay(900)
                    peeking = false
                    squishKey++
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
                    playPetLifeRoutine(
                        PetLifeDirector.choose(
                            screen = routePrefix,
                            landmarkKind = null,
                            persona = CurioPet.persona(context),
                            recentIds = recentRoutineIds.toSet()
                        )
                    )
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
                    lastPokeAt = System.currentTimeMillis()
                    lastTouch = System.currentTimeMillis()
                    continue
                }
                // v9.x — CHAMELEON GAME: the pet occasionally fades into the
                // background (a faint outline), waits a beat, then pops back
                // somewhere new with a flourish. Never while glued to the
                // deck or mid-spin.
                if (!watching && !CurioPet.spinning && Random.nextFloat() < 0.05f) {
                    // v12 — the game speaks its own line; keep the generic
                    // PLAY reaction quiet so it can't clobber it.
                    CurioPet.notePlay(context, react = false)
                    queueReaction(CurioPet.chameleonLine())
                    squishKey++
                    chameleonAlpha.snapTo(1f)
                    chameleonAlpha.animateTo(0.12f, tween(420, easing = FastOutSlowInEasing))
                    delay(760)
                    // v9.x — if the user grabbed the pet while it was hidden,
                    // never teleport it mid-drag: pop back visible and let
                    // the drag win (the wander loop is NOT cancelled by a
                    // drag, so this guard is the only thing that stops a
                    // surprise jump under the finger).
                    if (dragged) {
                        chameleonAlpha.snapTo(1f)
                        continue
                    }
                    val nx = marginPx + Random.nextFloat() * (maxW - petPx - 2 * marginPx).coerceAtLeast(0f)
                    val ny = marginPx + Random.nextFloat() * (maxH - petPx - 2 * marginPx).coerceAtLeast(0f)
                    pos = Offset(nx, ny)
                    facing = if (Random.nextFloat() < 0.5f) -1f else 1f
                    chameleonAlpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
                    squishKey++
                    celebrateKey++
                    lastTouch = System.currentTimeMillis()
                    continue
                }
                // v9.x — SPARK-CATCH GAME: a quick eager dash to a fresh spot
                // to "grab" a falling spark, with a celebration on arrival.
                if (!watching && !CurioPet.spinning && Random.nextFloat() < 0.06f) {
                    // v12 — the game speaks its own line; keep the generic
                    // PLAY reaction quiet so it can't clobber it.
                    CurioPet.notePlay(context, react = false)
                    queueReaction(CurioPet.sparkLine())
                    val sx = marginPx + Random.nextFloat() * (maxW - petPx - 2 * marginPx).coerceAtLeast(0f)
                    val sy = marginPx + Random.nextFloat() * (maxH - petPx - 2 * marginPx).coerceAtLeast(0f)
                    walkTo(Offset(sx, sy), stepMs = 12, steps = 34)
                    squishKey++
                    celebrateKey++
                    heartsKey++
                    lastTouch = System.currentTimeMillis()
                    continue
                }
                // v8.11 — the pet sometimes starts a game on its own: a play
                // bow + a "catch me!" line, then it zooms off. v8.12 — how
                // often it does this comes from its GROWING PERSONALITY
                // (bouncy pets play a lot, sparky ones are shy).
                if (Random.nextFloat() < CurioPet.playfulBias(context)) {
                    // v12 — the routine speaks its own line; keep the generic
                    // PLAY reaction quiet so it can't clobber it.
                    CurioPet.notePlay(context, react = false)
                    playPetLifeRoutine(
                        PetLifeDirector.choose(
                            screen = routePrefix,
                            landmarkKind = PetLandmarks.Kind.PLAY.name,
                            persona = CurioPet.persona(context),
                            recentIds = recentRoutineIds.toSet()
                        )
                    )
                    // The Pet Life routine owns this moment's contextual
                    // speech; retain the legacy PLAY motion without replacing
                    // the routine bubble.
                    fireReaction(PetReactionEvents.PLAY, null)
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
                // v9.2 — the touch / play / level-up events map to their own
                // reaction rules (all present in PetReactionEvents).
                val event = when (latest) {
                    CurioPet.Event.SPIN_LANDED -> PetReactionEvents.SPIN_LANDED
                    CurioPet.Event.REVEAL_TAPPED,
                    CurioPet.Event.REVEAL_AUTO -> PetReactionEvents.REVEAL
                    CurioPet.Event.EXPLORE -> PetReactionEvents.EXPLORE
                    CurioPet.Event.SAVE -> PetReactionEvents.SAVE
                    CurioPet.Event.PLAY -> PetReactionEvents.PLAY
                    CurioPet.Event.LEVEL_UP -> PetReactionEvents.LEVEL_UP
                    // v12 — TOUCH is owned entirely by the tap handler (gated
                    // line + tiered motion + TAP actions). Re-firing it here
                    // made EVERY tap speak, overwriting the 40% gated boop
                    // line with the generic touch line.
                    else -> null
                }
                if (event != null) fireReaction(event, CurioPet.eventLine(latest))
                if (latest == CurioPet.Event.SAVE) heartsKey++
                // v8.53 — Phase 7: user-defined actions for app events fire
                // alongside the built-in reaction.
                when (latest) {
                    CurioPet.Event.REVEAL_TAPPED,
                    CurioPet.Event.REVEAL_AUTO -> fireCustomActions(PetActionTrigger.REVEAL)
                    CurioPet.Event.SAVE -> fireCustomActions(PetActionTrigger.SAVE)
                    // v9.2 — custom actions can ride the level-up moment.
                    // (Touch already fires TAP actions in its own handler, so
                    // it isn't re-fired here.)
                    CurioPet.Event.LEVEL_UP -> fireCustomActions(PetActionTrigger.LEVEL_UP)
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
                    // v12 — PROUD is fully owned by the LEVEL_UP event
                    // reaction (its line + custom actions fire once there);
                    // the mood loop re-firing it made the pet double-speak
                    // ~1.2s after a level-up. EXCITED (a new lane) has no
                    // event of its own, so the mood loop stays its only voice.
                    if (m == CurioPet.Mood.EXCITED) {
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
        // v9.x — gated on [watching] (actually ON the Spin screen): a stale
        // spinning flag (left mid-spin, effect cancelled) must never leak
        // spin cheers onto other pages.
        LaunchedEffect(CurioPet.spinning) {
            if (CurioPet.spinning && autoWander && watching) {
                celebrateKey++
                queueReaction(CurioPet.spinCheer())
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
        // v11 — a direct interaction (tap / drag / app event) calls speakNow,
        // which re-keys this effect: the current hold is cancelled and the new
        // line fades in at once (or the bubble dismisses), and the queue it
        // drained from is already cleared — no more cycling stale lines.
        LaunchedEffect(reactionKey) {
            if (reaction != null) {
                bubbleAnim.snapTo(0f)
                bubbleAnim.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                delay(2300)
                bubbleAnim.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                reaction = null
                val nextReaction = reactionQueue.firstOrNull()
                reactionQueue = if (nextReaction == null) {
                    emptyList()
                } else {
                    reactionQueue.drop(1)
                }
                if (nextReaction != null) {
                    reaction = nextReaction
                    reactionKey++
                }
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
                    queueReaction("Tap tap tap! I can type too!")
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
                    alpha = appear.value * chameleonAlpha.value
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
                            // v9.x — grabbing the pet mid-chameleon brings it
                            // straight back to full visibility (the drag
                            // callbacks aren't suspend, so this hops onto the
                            // composition glide scope, the same host the
                            // throw-glide uses).
                            glideScope.launch { chameleonAlpha.snapTo(1f) }
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
                                    speakNow("Home sweet home!")
                                    leavingHome = true
                                }
                            }
                            if (flung && !leavingHome) {
                                recovering = true
                                speakNow(CurioPet.dizzyLine())
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
                                // v11 — the tap answers IMMEDIATELY: it skips
                                // whatever bubble is showing and drops the
                                // queued chatter (a null line just dismisses
                                // the bubble — the pet's motion is the
                                // reaction).
                                val line = if (Random.nextFloat() < 0.4f) {
                                    val builtInLine = CurioPet.touchReaction(tier)
                                    if (AppPreferences.customReactionLinesState) {
                                        rule.lines.randomOrNull() ?: builtInLine
                                    } else {
                                        builtInLine
                                    }
                                } else null
                                speakNow(line)
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
                            speakNow("Home sweet home!")
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
            val lifeFrame = routineAnim?.frames?.getOrNull(routineFrame)
            val activeFrame = caFrame ?: lifeFrame
            val activeView = when {
                caFrame != null -> caFrame.view
                lifeFrame != null && lifeFrame.view != com.curio.app.data.PetViewAngle.FRONT -> lifeFrame.view
                else -> routineView
            }
            CurioPetSprite(
                stage = CurioPet.currentStage(),
                mood = CurioPet.mood(context, CurioQuests.categoriesState, screenHint),
                // v9.4 — the pet grows with its stage: hatchling sits
                // small (~62%), sage fills the box proudly (~112%).
                spriteSize = FLOAT_SIZE * 0.92f * CurioPet.currentStage().sizeScale,
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
                faceOverride = customActionAnim?.let { activeDesign.faceFor(it.mood) }
                    ?: routineAnim?.let { activeDesign.faceFor(it.mood) }
                    ?: reactionFace,
                // Per-frame pixel layers work for custom actions and Pet Life
                // routines; the custom action wins if both overlap.
                bodyOverride = activeFrame?.bodyRows,
                curledOverride = activeFrame?.curledRows,
                eyeOverride = activeFrame?.eyeGrid,
                viewAngle = activeView,
                peeking = peeking,
                contentDescription = "Curie, your companion pet. Drag it anywhere, tap to say hi",
                modifier = Modifier.graphicsLayer {
                    translationY = (activeFrame?.offsetY ?: 0f).dp.toPx()
                    scaleX = activeFrame?.scale ?: 1f
                    scaleY = activeFrame?.scale ?: 1f
                    rotationZ = activeFrame?.rotationDegrees ?: 0f
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
            scale = TYPING_SCALE,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (pos.x + petPx / 2f - with(density) { (TYPING_W * TYPING_SCALE).toPx() } / 2f).roundToInt(),
                        (pos.y - with(density) { (60.dp * TYPING_SCALE + 6.dp).toPx() }).roundToInt()
                    )
                }
                .size(TYPING_W * TYPING_SCALE, TYPING_H * TYPING_SCALE)
        )

        // Reaction bubbles belong to the pet. Tour guidance is different: it
        // belongs to the registered control, so it must follow that control's
        // window bounds rather than the pet's current position. Landmarks are
        // measured with positionInWindow(), the same coordinate space used by
        // this full-screen overlay.
        val visibleBubble = tourStep?.dialogue ?: reaction
        val tourActiveLandmark = tourStep?.let { tourLandmark }
        val tourBubbleWidthPx = if (tourActiveLandmark != null && tourBubbleSize.width > 0) {
            tourBubbleSize.width.toFloat()
        } else {
            with(density) { 260.dp.toPx() }
        }
        val tourBubbleHeightPx = if (tourActiveLandmark != null && tourBubbleSize.height > 0) {
            tourBubbleSize.height.toFloat()
        } else {
            with(density) { FLOAT_SIZE.toPx() }
        }
        val tourBubbleGapPx = with(density) { 8.dp.toPx() }
        val bubbleOffset = tourActiveLandmark?.bounds?.let { bounds ->
            // Landmark bounds are in window coordinates; convert them to this
            // overlay's local coordinate space before applying Modifier.offset.
            val localBounds = Rect(
                left = bounds.left - overlayOrigin.x,
                top = bounds.top - overlayOrigin.y,
                right = bounds.right - overlayOrigin.x,
                bottom = bounds.bottom - overlayOrigin.y
            )
            val x = (localBounds.center.x - tourBubbleWidthPx / 2f)
                .coerceIn(marginPx, (maxW - tourBubbleWidthPx - marginPx).coerceAtLeast(marginPx))
            val aboveY = localBounds.top - tourBubbleHeightPx - tourBubbleGapPx
            val belowY = localBounds.bottom + tourBubbleGapPx
            val y = if (aboveY >= marginPx) aboveY else belowY
                .coerceIn(marginPx, (maxH - tourBubbleHeightPx - marginPx).coerceAtLeast(marginPx))
            IntOffset(x.roundToInt(), y.roundToInt())
        } ?: IntOffset(
            pos.x.roundToInt().coerceIn(
                marginPx.toInt(),
                (maxW - with(density) { 160.dp.toPx() }).toInt()
            ),
            (pos.y - tourBubbleHeightPx).roundToInt()
        )
        LaunchedEffect(tourStep?.id) {
            tourBubbleSize = IntSize.Zero
        }
        visibleBubble?.let { text ->
            Box(
                modifier = Modifier
                    .offset { bubbleOffset }
                    .then(
                        if (tourActiveLandmark != null) {
                            Modifier.onSizeChanged { measuredSize ->
                                if (tourBubbleSize != measuredSize) {
                                    tourBubbleSize = measuredSize
                                }
                            }
                        } else {
                            Modifier.height(FLOAT_SIZE)
                        }
                    )
                    // v8.26 — fade + gentle rise so the bubble glides in and
                    // out instead of snapping (driven by [bubbleAnim]).
                    .graphicsLayer {
                        val bubbleProgress = if (tourStep != null) 1f else bubbleAnim.value
                        alpha = bubbleProgress
                        translationY = (1f - bubbleProgress) * 8.dp.toPx()
                    }
            ) {
                PetSpeechBubble(
                    text = text,
                    // The tour bubble is centered over the exact landmark;
                    // its left tail remains the least surprising orientation
                    // for controls near either side of the screen.
                    tailOnLeft = false,
                    // v9.x — the tour dialogue is NOT clipped to two lines
                    // (the old maxLines=2 cut longer steps like Settings) and
                    // may grow wider than a passive reaction bubble. Regular
                    // reactions keep their cozy two-line cap.
                    maxLines = if (tourStep != null) Int.MAX_VALUE else 2,
                    maxWidth = if (tourStep != null) 340.dp else 260.dp,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

/**
 * v8.36 — the premium mini keyboard Curie "types" on while the user's own
 * keyboard is open: a tiny 3-row keyboard with a screen strip (typed dots +
 * blinking caret) and a little hand that sweeps across the keys, lighting
 * them up as it taps. Calm, premium, readable — not the old flashing strip.
 */
@Composable
private fun TypingKeyboard(
    visible: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    // v9.x — uniform scale for the whole keyboard: the canvas size AND the
    // base dp unit below shrink together, so every key/row/hand scales
    // proportionally (all inner dimensions derive from [d] or w/h).
    scale: Float = 1f
) {
    val density = LocalDensity.current
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.95f else 0f,
        animationSpec = tween(200),
        label = "typingAlpha"
    )
    val sweep = remember { Animatable(0f) }
    val pressing = remember { mutableStateOf(true) }
    LaunchedEffect(visible) {
        if (visible) {
            sweep.snapTo(0f)
            pressing.value = true
            while (true) {
                // Sweep across the keys typing…
                sweep.animateTo(1f, tween(1700, easing = LinearEasing))
                // …then glide back to the home key (no keys pressed).
                pressing.value = false
                sweep.animateTo(0f, tween(650, easing = LinearEasing))
                pressing.value = true
            }
        }
    }
    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val w = size.width
        val h = size.height
        val d = with(density) { (1.dp * scale.coerceIn(0.1f, 1f)).toPx() }

        // Keyboard body with a soft drop shadow.
        drawRoundRect(
            color = Color(0x59000000),
            topLeft = Offset(0f, 2.5f * d),
            size = size,
            cornerRadius = CornerRadius(9f * d)
        )
        drawRoundRect(color = Color(0xE01C1F2C), cornerRadius = CornerRadius(9f * d))

        // Screen strip: typed dots + blinking caret.
        val inset = 4f * d
        val screenTop = 4f * d
        val screenH = 11f * d
        drawRoundRect(
            color = Color(0xFF0D0F16),
            topLeft = Offset(inset, screenTop),
            size = Size(w - inset * 2, screenH),
            cornerRadius = CornerRadius(3f * d)
        )
        val dots = if (pressing.value) ((sweep.value * 16f).toInt() % 4).coerceAtLeast(1) else 3
        for (i in 0 until dots) {
            drawCircle(
                color = accent.copy(alpha = 0.9f),
                radius = 1.1f * d,
                center = Offset(inset + 5f * d + i * 4.5f * d, screenTop + screenH / 2f)
            )
        }
        if ((sweep.value * 9f).toInt() % 2 == 0) {
            drawRoundRect(
                color = Color(0xFFA5D8FF),
                topLeft = Offset(inset + 5f * d + dots * 4.5f * d + 2f * d, screenTop + 1.5f * d),
                size = Size(1.2f * d, screenH - 3f * d),
                cornerRadius = CornerRadius(1f * d)
            )
        }

        // Key rows: 5 / 5 / 3 + wide space. Centers are collected in key order
        // so the hand can glide along a single continuous path.
        val keyTop = screenTop + screenH + 5f * d
        val gap = 2f * d
        val keyH = (h - keyTop - 4f * d - gap * 2) / 3f
        val keyW = (w - gap * 6f) / 5f
        val spaceW = w - gap * 2 - 3 * (keyW + gap)
        val centers = ArrayList<Offset>(13)
        for (row in 0 until 3) {
            val y = keyTop + row * (keyH + gap) + keyH / 2f
            val small = if (row < 2) 5 else 3
            for (col in 0 until small) {
                centers.add(Offset(gap + col * (keyW + gap) + keyW / 2f, y))
            }
            if (row == 2) {
                val left = gap + 3 * (keyW + gap)
                centers.add(Offset((left + w - gap) / 2f, y))
            }
        }

        // Hand position glides along the key path (continuous in both directions).
        val seg = sweep.value * 14f
        val idx = seg.toInt().coerceIn(0, 13)
        val frac = seg - idx
        val a = centers[idx.coerceAtMost(12)]
        val b = centers[(idx + 1).coerceAtMost(12)]
        val handX = a.x + (b.x - a.x) * frac
        val handY = a.y + (b.y - a.y) * frac
        val pressedIdx = if (pressing.value) idx.coerceAtMost(12) else -1

        for (i in centers.indices) {
            val c = centers[i]
            val wide = i == 12
            val kw = if (wide) spaceW else keyW
            val pressed = i == pressedIdx
            val keyRad = CornerRadius(3f * d)
            if (pressed) {
                // Soft glow under the tapped key.
                drawCircle(color = accent.copy(alpha = 0.5f), radius = kw * 0.55f, center = c)
            }
            val keyCol = if (pressed) accent else Color(0xFFF3EFE7)
            val keySize =
                if (pressed) Size(kw - 2f * d, keyH - 1.5f * d) else Size(kw, keyH)
            val keyOff =
                if (pressed) Offset(c.x - keySize.width / 2f, c.y - keySize.height / 2f + 1.5f * d)
                else Offset(c.x - keySize.width / 2f, c.y - keySize.height / 2f)
            if (!pressed) {
                // Shallow bottom shade for a keycap feel.
                drawRoundRect(
                    color = Color(0xFFB9B3A6),
                    topLeft = Offset(keyOff.x, keyOff.y + 1f * d),
                    size = keySize,
                    cornerRadius = keyRad
                )
            }
            drawRoundRect(color = keyCol, topLeft = keyOff, size = keySize, cornerRadius = keyRad)
            drawCircle(
                color = if (pressed) Color(0xFFFFFFFF) else Color(0xFFB9B3A6),
                radius = 0.8f * d,
                center = Offset(c.x, c.y)
            )
        }

        // The little hand resting on the active key: palm + three fingers + thumb.
        val hs = 6f * d
        val handCol = Color(0xFF262938)
        drawRoundRect(
            color = handCol,
            topLeft = Offset(handX - hs * 0.7f, handY - hs * 0.3f),
            size = Size(hs * 1.4f, hs * 0.85f),
            cornerRadius = CornerRadius(hs * 0.35f)
        )
        repeat(3) { i ->
            drawCircle(handCol, hs * 0.2f, Offset(handX + (i - 1) * hs * 0.5f, handY - hs * 0.5f))
        }
        drawCircle(handCol, hs * 0.16f, Offset(handX - hs * 0.8f, handY + hs * 0.1f))
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

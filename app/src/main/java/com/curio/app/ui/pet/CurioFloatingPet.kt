package com.curio.app.ui.pet

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private val FLOAT_SIZE = 72.dp
private val EDGE_MARGIN = 14.dp
private val AUTO_NAP_AFTER_MS = 8 * 60_000L
// v8.13 — hearts rise in their own box ABOVE the pet (never over its face).
private val HEARTS_W = 150.dp
private val HEARTS_H = 96.dp
// v8.20 — the little cloud the pet rides while it walks (under the sprite).
private val CLOUD_W = 96.dp
private val CLOUD_H = 42.dp
// v8.20 — how close a drop must be to the flower bed to count as "home".
private val DROP_FORGIVENESS = 12.dp

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
 *    (soft boop -> playful play-bow -> zoomies spin) with a matching line,
 *    hearts, and then a playful dart to a nearby spot — like a pet that
 *    wants to keep playing. Sometimes it even starts the game itself.
 *  - CELEBRATES: when its mood flips to EXCITED/PROUD (a new lane, a
 *    level-up, a claim), it hops with a short excited line.
 *  - NAPS: after a long idle it fades back into its flower bed
 *    ([CurioPet.settleToSleep]) — the bed shows it asleep until tapped.
 *
 * Gated by the Appearance toggles: the whole pet layer
 * ([AppPreferences.petEnabledState]) and the floating companion itself
 * ([AppPreferences.floatingPetEnabledState]). Hides while a pet dialog is
 * open ([CurioPet.dialogOpen]) so there is never a duplicate pet on screen,
 * and while the pet is sitting at home in its bed ([CurioPet.atHome]).
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
    if (!AppPreferences.petEnabledState ||
        !AppPreferences.floatingPetEnabledState ||
        !CurioPet.awake ||
        CurioPet.atHome ||
        CurioPet.dialogOpen
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
        var thinking by remember { mutableStateOf(false) }
        var squishKey by remember { mutableIntStateOf(0) }
        var playKey by remember { mutableIntStateOf(0) }
        var spinKey by remember { mutableIntStateOf(0) }
        var celebrateKey by remember { mutableIntStateOf(0) }
        var heartsKey by remember { mutableIntStateOf(0) }
        var reaction by remember { mutableStateOf<String?>(null) }
        var reactionKey by remember { mutableIntStateOf(0) }
        var lastMood by remember { mutableStateOf<CurioPet.Mood?>(null) }
        var lastTouch by remember { mutableStateOf(System.currentTimeMillis()) }
        var leavingHome by remember { mutableStateOf(false) }
        // v8.11 — touch escalation: rapid repeated taps (within 1.6s) push
        // the reaction tier up (boop -> play -> zoomies). A tap also queues
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

        // Entrance hop.
        LaunchedEffect(Unit) {
            appear.snapTo(0f)
            appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f))
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
                    if (dragged || !CurioPet.awake) break
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
                    !dragged && !watching && CurioPet.awake
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
                if (watching) {
                    // Glued to the Spin deck between pokes: stay and watch.
                    delay(300)
                    continue
                }
                // v8.11 — the pet sometimes starts a game on its own: a play
                // bow + a "catch me!" line, then it zooms off. v8.12 — how
                // often it does this comes from its GROWING PERSONALITY
                // (bouncy pets play a lot, sparky ones are shy).
                if (Random.nextFloat() < CurioPet.playfulBias(context)) {
                    playKey++
                    CurioPet.notePlay(context)
                    reaction = CurioPet.playInitiation()
                    reactionKey++
                    lastTouch = System.currentTimeMillis()
                    val tx = marginPx + Random.nextFloat() * (maxW - petPx - 2 * marginPx).coerceAtLeast(0f)
                    val ty = marginPx + Random.nextFloat() * (maxH - petPx - 2 * marginPx).coerceAtLeast(0f)
                    walkTo(Offset(tx, ty), stepMs = 16, steps = 44)
                    continue
                }
                // Normal wander — a downward bias keeps it grounded instead
                // of floating over the top bars.
                val tx = marginPx + Random.nextFloat() * (maxW - petPx - 2 * marginPx).coerceAtLeast(0f)
                val tyBand = (maxH - petPx - 2 * marginPx).coerceAtLeast(0f)
                val ty = if (Random.nextFloat() < 0.25f)
                    marginPx + Random.nextFloat() * tyBand * 0.35f
                else
                    marginPx + tyBand * (0.35f + Random.nextFloat() * 0.65f)
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
        //    pet hops, cheers and (on saves) pops hearts.
        LaunchedEffect(CurioPet.eventCount) {
            val latest = CurioPet.lastEvent
            if (CurioPet.eventCount > seenEvents && latest != null) {
                seenEvents = CurioPet.eventCount
                celebrateKey++
                lastTouch = System.currentTimeMillis()
                reaction = CurioPet.eventLine(latest)
                reactionKey++
                if (latest == CurioPet.Event.SAVE) heartsKey++
            }
        }

        // ── Mood reactions: hop + excited line on EXCITED/PROUD ─────────
        LaunchedEffect(Unit) {
            while (true) {
                delay(1200)
                val m = CurioPet.mood(context, CurioQuests.categoriesState, screenHint)
                if (lastMood != m) {
                    lastMood = m
                    if (m == CurioPet.Mood.EXCITED || m == CurioPet.Mood.PROUD) {
                        celebrateKey++
                        // App activity counts as interaction — the pet won't
                        // nap away mid-celebration.
                        lastTouch = System.currentTimeMillis()
                        reaction = CurioPet.lineFor(context, m, CurioQuests.categoriesState)
                        reactionKey++
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

        // Bubble auto-dismiss — the reaction shows for a beat, then the pet
        // settles back to its idle wander (v8.11: a touch shorter so it
        // feels snappy, not chatty).
        LaunchedEffect(reactionKey) {
            if (reaction != null) {
                delay(1500)
                reaction = null
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
                        (pos.y + petPx * 0.66f).roundToInt()
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
                    PetLandmarks.setHovered("bed", false)
                    detectDragGestures(
                        onDragStart = {
                            dragged = true
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
                            lastTouch = System.currentTimeMillis()
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
                        },
                        onDragCancel = {
                            dragged = false
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
                            // from a boop to a play-bow to zoomies (v8.11).
                            val now = System.currentTimeMillis()
                            tapStreak = if (now - lastTapAt < 1600L) tapStreak + 1 else 1
                            lastTapAt = now
                            val tier = tapStreak.coerceAtMost(3)
                            reaction = CurioPet.touchReaction(tier)
                            reactionKey++
                            heartsKey++
                            when (tier) {
                                1 -> squishKey++
                                2 -> playKey++
                                else -> spinKey++
                            }
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
                contentDescription = "Curio, your companion pet — drag it anywhere, tap to say hi"
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
 * Pink hearts that float UP from just above the pet's head and fade
 * (v8.13). Drawn in a sibling box offset above the pet, so they never
 * cover the sprite's face. Starts near the bottom of this box (the pet's
 * head) and rises to the top, shrinking as it fades.
 */
@Composable
private fun HeartsOverlay(key: Int, modifier: Modifier = Modifier) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key > 0) {
            rise.snapTo(0f)
            rise.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        }
    }
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        repeat(3) { i ->
            val t = ((rise.value * 3f + i * 0.33f) % 1f)
            val alpha = (1f - t).coerceIn(0f, 1f)
            val s = size.minDimension * (0.09f + t * 0.04f)
            val x = centerX + (i - 1) * size.width * 0.22f
            val y = size.height * 0.95f - t * size.height * 0.95f
            drawHeart(x, y, s, Color(0xFFF7AFAF).copy(alpha = alpha * 0.95f))
        }
    }
}

/**
 * v8.20 — the puff of cloud the pet rides while it walks. Drawn in its own
 * offset sibling under the sprite (never inside its touch box); three soft
 * overlapping puffs + a flat base, bobbing gently, fading in with movement
 * so it only shows on the go.
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
        val w = size.width
        val h = size.height
        val puff = Color.White
        val shadow = Color(0xFFDCD5C8)
        val cY = h * 0.55f
        // Three overlapping puffs + a flat base make the cloud read soft.
        drawCircle(puff, radius = w * 0.22f, center = Offset(w * 0.30f, cY))
        drawCircle(puff, radius = w * 0.27f, center = Offset(w * 0.48f, cY - h * 0.16f))
        drawCircle(puff, radius = w * 0.21f, center = Offset(w * 0.68f, cY))
        drawRoundRect(
            color = puff,
            topLeft = Offset(w * 0.12f, cY),
            size = Size(w * 0.76f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.12f)
        )
        // A whisper of shade under the puffs for definition.
        drawRoundRect(
            color = shadow.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.16f, h * 0.82f),
            size = Size(w * 0.68f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.06f)
        )
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

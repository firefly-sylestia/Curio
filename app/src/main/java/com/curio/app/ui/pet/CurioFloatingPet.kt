package com.curio.app.ui.pet

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.Offset
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
 *  - REACTS TO TOUCH: a tap squishes it, pops a tiny reaction bubble
 *    ("Boop!", "Hehe!") and refreshes its nap timer.
 *  - CELEBRATES: when its mood flips to EXCITED/PROUD (a new lane, a
 *    level-up, a claim), it hops with a short excited line.
 *  - NAPS: after a long idle it fades back into its flower bed
 *    ([CurioPet.settleToSleep]) — the bed shows it asleep until tapped.
 *
 * Gated by the Appearance toggles: the whole pet layer
 * ([AppPreferences.petEnabledState]) and the floating companion itself
 * ([AppPreferences.floatingPetEnabledState]).
 *
 * Touch plumbing lives ONLY on the pet element, so the transparent overlay
 * never blocks taps or scrolls on the screen beneath.
 */
@Composable
fun CurioFloatingPet(
    accent: Color,
    routePrefix: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    if (!AppPreferences.petEnabledState ||
        !AppPreferences.floatingPetEnabledState ||
        !CurioPet.awake
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
        var celebrateKey by remember { mutableIntStateOf(0) }
        var heartsKey by remember { mutableIntStateOf(0) }
        var reaction by remember { mutableStateOf<String?>(null) }
        var reactionKey by remember { mutableIntStateOf(0) }
        var lastMood by remember { mutableStateOf<CurioPet.Mood?>(null) }
        var lastTouch by remember { mutableStateOf(System.currentTimeMillis()) }
        val appear = remember { Animatable(0f) }
        // v8.9 — on the Spin screen the pet stops to watch the deck; event
        // reactions start from the current count so stale events never fire.
        val watching = routePrefix?.startsWith("spin") == true
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

        // ── Autonomy: wander to random spots ────────────────────────────
        // Keyed on awake too: the loop dies when the pet naps and restarts
        // fresh when it wakes again. On the Spin screen the pet prefers to
        // WATCH the deck, so it stays put there. `watching` is a plain val
        // (not state), so it must be an effect key — otherwise the loop
        // would keep a stale value after navigating between screens.
        LaunchedEffect(maxW, maxH, autoWander, CurioPet.awake, watching) {
            if (!autoWander) return@LaunchedEffect
            while (CurioPet.awake) {
                delay(Random.nextLong(2800, 7000))
                if (dragged || watching) continue
                val tx = marginPx + Random.nextFloat() * (maxW - petPx - 2 * marginPx).coerceAtLeast(0f)
                val ty = marginPx + Random.nextFloat() * (maxH - petPx - 2 * marginPx).coerceAtLeast(0f)
                facing = if (tx >= pos.x) 1f else -1f
                // v8.9 — sometimes the pet 'thinks' (tilts + "?") first.
                if (Random.nextFloat() < 0.45f) {
                    thinking = true
                    delay(620)
                    thinking = false
                }
                moving = true
                val start = pos
                val steps = 56
                val stepMs = Random.nextLong(18, 34)
                for (i in 1..steps) {
                    if (dragged || !CurioPet.awake) break
                    val t = i.toFloat() / steps
                    pos = Offset(
                        start.x + (tx - start.x) * t,
                        start.y + (ty - start.y) * t
                    )
                    delay(stepMs)
                }
                moving = false
                thinking = false
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
                val m = CurioPet.mood(context, CurioQuests.categoriesState)
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

        // Bubble auto-dismiss.
        LaunchedEffect(reactionKey) {
            if (reaction != null) {
                delay(2000)
                reaction = null
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .size(FLOAT_SIZE)
                .graphicsLayer {
                    alpha = appear.value
                    scaleX = 0.5f + 0.5f * appear.value
                    scaleY = 0.5f + 0.5f * appear.value
                }
                .pointerInput(maxW, maxH, petPx, marginPx) {
                    detectDragGestures(
                        onDragStart = {
                            dragged = true
                            lastTouch = System.currentTimeMillis()
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
                        },
                        onDragEnd = {
                            dragged = false
                            lastTouch = System.currentTimeMillis()
                        },
                        onDragCancel = {
                            dragged = false
                            lastTouch = System.currentTimeMillis()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        lastTouch = System.currentTimeMillis()
                        reaction = CurioPet.touchReaction()
                        reactionKey++
                        squishKey++
                        heartsKey++
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            // Soft cream glow so the fixed one-look sprite reads on any theme.
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFFFF3DC).copy(alpha = 0.30f),
                    radius = size.minDimension * 0.54f
                )
            }
            CurioPetSprite(
                stage = CurioPet.currentStage(),
                mood = CurioPet.mood(context, CurioQuests.categoriesState),
                accent = accent,
                spriteSize = FLOAT_SIZE * 0.92f,
                celebrateKey = celebrateKey,
                squishKey = squishKey,
                moving = moving,
                dragged = dragged,
                facing = facing,
                thinking = thinking,
                watching = watching,
                contentDescription = "Curio, your companion pet — drag it anywhere, tap to say hi"
            )
            // Little hearts rising on taps and saves.
            PetHearts(key = heartsKey, modifier = Modifier.fillMaxSize())
        }
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

/** Tiny pink hearts that rise and fade above the pet on taps and saves. */
@Composable
private fun PetHearts(key: Int, modifier: Modifier = Modifier) {
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
            val s = size.minDimension * (0.11f + t * 0.05f)
            val x = centerX + (i - 1) * size.width * 0.18f
            val y = size.height * 0.5f - t * size.height * 0.85f
            drawHeart(x, y, s, Color(0xFFF7AFAF).copy(alpha = alpha * 0.95f))
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

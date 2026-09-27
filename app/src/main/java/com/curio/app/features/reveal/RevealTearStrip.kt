package com.curio.app.features.reveal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * v494 — TEAR IT OFF THE PAGE (agenda § 5.2): the reveal's save becomes a strip
 * of paper you tear off.
 *
 * The member's ruling settled what this owns: **"Tear saves on the reveal; folding
 * closes a Cabinet card."** So this strip is a SECOND way to do the Completed
 * star's write — the same two calls (`setTopicSentiment` LIKE +
 * `ExploreSessionStore.setCompleted` true), made by pulling a perforated strip
 * down off the page. **The star pill is untouched**: a gesture may never be the
 * only route to something, and retiring the pill was never asked for.
 *
 * The physical read, in three parts:
 *
 *  - **The perforation is real.** A dashed line and a notched edge are drawn
 *    where the strip is attached; the ragged edge is revealed as the strip pulls
 *    down, so the tear is something you WATCH happen rather than a threshold
 *    that fires.
 *  - **The drag is the tear.** The strip follows the finger down, tilting as it
 *    goes, with a light tick each notch (~12% of the travel) — the same
 *    tick-per-step vocabulary as the deck's riffle and the dial's notches.
 *  - **The fall is momentum.** Past the threshold (45%) the strip tears free and
 *    drops away on its own — rotation growing as it falls, gone in under half a
 *    second — and the page keeps the ragged edge and a **Kept** line. Short of
 *    the threshold it springs back (`Springs.Snappy`), because a strip that
 *    half-tears and stays is broken paper.
 *
 * Already-completed topics render the torn state directly (the piece is simply
 * gone), and tapping the **Kept** line puts it back — the exact reverse of the
 * star's un-complete, so the two controls can never disagree about the fact.
 */
@Composable
fun RevealTearStrip(
    /** The category accent — the tear line and the Kept ink wear it. */
    accent: Color,
    /** Whether the topic is already completed (the star's own state). */
    completed: Boolean,
    /** The Completed star's exact write, run when the strip tears free. */
    onTear: () -> Unit,
    /** The star's exact un-write, run when the Kept line is tapped. */
    onPutBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    // The paper is the surface pushed one hair toward the accent, so the strip
    // reads as part of THIS topic's page rather than a generic component.
    val paper = lerp(scheme.surfaceContainerHigh, accent, 0.10f)
    val ink = scheme.onSurface
    val softInk = ink.copy(alpha = 0.55f)

    // Drag progress 0..1 — how far down the strip has been pulled. The fall is
    // separate (below) so a settle-back can never fire a tear.
    val pull = remember { Animatable(0f) }
    // The tear-free fall, 0..1: runs only after the threshold, carries the strip
    // off the page, and `onTear()` fires as it finishes so the parent's state
    // flip lands on the same beat as the piece leaving.
    val fall = remember { Animatable(0f) }
    var tearing by remember { mutableStateOf(false) }

    // The ragged edge: one jagged profile, seeded, computed ONCE from a single
    // generator. Random per frame would be a shimmer, not a tear — the
    // passport's stamp already set the rule that paper imperfection is
    // deterministic.
    val jagged = remember { run { val r = Random(20260927); List(24) { r.nextFloat() } } }

    // Notch ticks: one light tick per ~12% of travel, the riffle's vocabulary.
    var lastNotch by remember { mutableStateOf(0) }

    val density = LocalDensity.current
    val travelPx = with(density) { TearTravel.toPx() }

    // Already kept (or torn this visit): the stub is what remains — the ragged
    // edge, the Kept line, and the way back.
    if (completed) {
        Surface(
            onClick = onPutBack,
            shape = RoundedCornerShape(TearJawRadius),
            color = paper,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurioIcon(
                    CurioIcons.Check,
                    null,
                    tint = accent,
                    size = 19.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Torn off and kept — tap to put it back",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = softInk
                )
            }
        }
        return
    }

    // ── The strip, still attached ───────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TearBandHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        // The perforation: a dashed rule where the strip is attached, and the
        // ragged edge beneath it that shows itself as the pull grows. Drawn on
        // the page (behind the moving strip), so the tear appears where the
        // strip leaves rather than travelling with it.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(TearPerfHeight)
        ) {
            val w = this.size.width
            val midY = this.size.height * 0.5f
            // The dashed perforation — the promise of the tear, always visible.
            val dash = 7f
            var x = 0f
            while (x < w) {
                drawLine(
                    color = ink.copy(alpha = 0.22f),
                    start = Offset(x, midY),
                    end = Offset(minOf(x + dash, w), midY),
                    strokeWidth = 2f
                )
                x += dash * 2.1f
            }
            // The ragged edge, revealed by the pull. A jagged path across the
            // width — each notch its own seeded depth — stroked in the accent,
            // fading IN as the strip comes away (alpha tracks the pull).
            if (pull.value > 0.02f) {
                val steps = jagged.size
                val path = Path()
                jagged.forEachIndexed { i, f ->
                    val px = w * i / (steps - 1f)
                    val py = midY + (f - 0.5f) * 9f
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(
                    path = path,
                    color = accent,
                    style = Stroke(width = 2.4f),
                    alpha = (pull.value * 1.4f).coerceAtMost(1f)
                )
            }
        }

        // The strip itself: the one moving object. `translationY` follows the
        // pull, the tilt grows with it, and the fall carries it off with
        // rotation — momentum, on the tear, as the agenda asked.
        Surface(
            shape = RoundedCornerShape(TearJawRadius),
            color = paper,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(TearStripHeight)
                .graphicsLayer {
                    translationY = pull.value * travelPx + fall.value * (travelPx + 340f)
                    rotationZ = pull.value * 2.2f + fall.value * 9f
                    alpha = 1f - fall.value
                }
                .pointerInput(completed) {
                    detectVerticalDragGestures(
                        onDragStart = { if (!tearing) lastNotch = 0 },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            if (tearing) return@detectVerticalDragGestures
                            // Snap-to inside the gesture (not per-frame animateTo):
                            // the strip tracks the finger 1:1 like the deck's fan.
                            val next = (pull.value + amount / travelPx).coerceIn(0f, 1f)
                            pull.snapTo(next)
                            val notch = (next / 0.12f).roundToInt()
                            if (notch != lastNotch) {
                                lastNotch = notch
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            if (tearing) return@detectVerticalDragGestures
                            if (pull.value >= TearTearThreshold) {
                                tearing = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    // The piece falls on its own now — the tear is
                                    // DONE from the finger's point of view.
                                    fall.animateTo(1f, tween(durationMillis = 420))
                                    onTear()
                                    pull.snapTo(0f)
                                    fall.snapTo(0f)
                                    tearing = false
                                }
                            } else {
                                scope.launch {
                                    pull.animateTo(0f, CurioMotion.Springs.Snappy)
                                }
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(9.dp))
                Text(
                    text = "Tear off to keep",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "pull the strip down",
                    style = MaterialTheme.typography.labelSmall,
                    color = softInk,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Safety: if the parent flips `completed` under us mid-fall (another surface
    // completed the topic), stop the machinery cleanly rather than letting a
    // zombie coroutine write twice.
    LaunchedEffect(completed) {
        if (completed && tearing) {
            tearing = false
            fall.snapTo(0f)
            pull.snapTo(0f)
        }
    }
}

/** How far down the strip must travel for the pull to read as full. */
private val TearTravel = 96.dp

/** The pull fraction past which the strip tears free on release. */
private const val TearTearThreshold = 0.45f

/** Geometry of the band and its parts. */
private val TearBandHeight = 92.dp
private val TearStripHeight = 58.dp
private val TearPerfHeight = 26.dp
private val TearJawRadius = 16.dp

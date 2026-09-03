package com.curio.app.features.picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════
// v323 — RADIAL HOLD MENU (liquid edition)
// ═══════════════════════════════════════════════════════════════════════
// The category picker's tap-and-hold actions, rebuilt from the old single
// capsule into a fluid radial menu:
//  - NO dark scrim — the menu exists only while the finger is down.
//  - Circular actions well up OUT of the press point (gooey blob morph) and
//    settle into a ring AROUND it.
//  - Drag anywhere: the nearest disc highlights live; release over one to
//    pick it, release over nothing to cancel.
//
// The gesture lives on the tile ([radialHoldMenu]); the visuals live in
// [RadialHoldMenuOverlay], which the screen renders at the held anchor.
// Both share [radialRingPositions] / [radialPickIndex] so what the user
// sees and what the release resolves are the same geometry.

private const val RING_RADIUS_DP = 76f   // ring radius (dp)
private const val DISC_DP = 46f          // settled disc diameter (dp)
private const val HIT_SLOP_DP = 20f      // extra grab radius per disc (dp)

/** Ring centers around [anchor] (px) for [count] actions, starting straight up. */
fun radialRingPositions(anchor: Offset, count: Int, radiusPx: Float): List<Offset> {
    if (count <= 0) return emptyList()
    val step = 360.0 / count
    return List(count) { i ->
        val rad = Math.toRadians(-90.0 + step * i)
        Offset(
            anchor.x + (cos(rad) * radiusPx).toFloat(),
            anchor.y + (sin(rad) * radiusPx).toFloat()
        )
    }
}

/** The action index nearest [pos] within [hitRadiusPx], or null. */
fun radialPickIndex(anchor: Offset, count: Int, radiusPx: Float, hitRadiusPx: Float, pos: Offset): Int? {
    var best = -1
    var bestD = Float.MAX_VALUE
    radialRingPositions(anchor, count, radiusPx).forEachIndexed { i, p ->
        val d = (p - pos).getDistance()
        if (d < hitRadiusPx && d < bestD) {
            best = i
            bestD = d
        }
    }
    return best.takeIf { it >= 0 }
}

/**
 * One live hold-gesture session. A tile builds one (screen-level lambdas
 * drive the open overlay) and attaches it with [radialHoldMenu]:
 *  - [onOpen] fires with the press position (root px) after the long-press
 *    threshold — the ring's center.
 *  - [onMove] feeds the live finger position (drives the highlight).
 *  - [onEnd] hands the RELEASE position to the overlay, which resolves the
 *    picked action (or cancels).
 *  - [onTap] fires on a quick tap (no hold).
 */
class HoldSession(
    val onOpen: (Offset) -> Unit,
    val onMove: (Offset) -> Unit,
    val onEnd: (Offset) -> Unit,
    val onTap: () -> Unit
)

/**
 * Attaches the radial-hold gesture. Place BEFORE any clickable in the chain
 * so the hold owns the pointer once it opens. The long-press uses the
 * system's own timeout ([LocalViewConfiguration.longPressTimeoutMillis]).
 */
fun Modifier.radialHoldMenu(hold: HoldSession?): Modifier = composed {
    if (hold == null) return@composed this
    val viewConfig = LocalViewConfiguration.current
    this.pointerInput(hold) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pressPos = down.positionInRoot()
            var opened = false
            val holdJob = this@pointerInput.launch {
                delay(viewConfig.longPressTimeoutMillis)
                opened = true
                hold.onOpen(pressPos)
            }
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.pressed) {
                    if (opened) {
                        hold.onMove(change.positionInRoot())
                        // Own the pointer once the menu is open so the
                        // clickable underneath never sees the release.
                        change.consume()
                    }
                } else {
                    holdJob.cancel()
                    if (opened) hold.onEnd(change.positionInRoot()) else hold.onTap()
                    break
                }
            }
            holdJob.cancel()
        }
    }
}

/**
 * The radial menu visuals: a gooey liquid blob layer (blur + alpha-contrast
 * chain effect on API 31+, soft circles below) morphing out of [anchor],
 * with crisp glass discs on top. [cursor] highlights the nearest disc;
 * [endPos] (the release point) resolves the pick and fires its action.
 */
@Composable
internal fun RadialHoldMenuOverlay(
    anchor: Offset,
    actions: List<HoldAction>,
    cursor: Offset?,
    endPos: Offset?,
    onCancel: () -> Unit
) {
    if (actions.isEmpty()) return
    val density = LocalDensity.current
    val radiusPx = with(density) { RING_RADIUS_DP.dp.toPx() }
    val discPx = with(density) { DISC_DP.dp.toPx() }
    val hitPx = with(density) { HIT_SLOP_DP.dp.toPx() }
    var scrimSize by remember { mutableStateOf(IntSize.Zero) }
    var overlayRoot by remember { mutableStateOf(Offset.Zero) }
    // Clamp the ring center so every disc stays inside the sheet.
    val center = remember(anchor, scrimSize, radiusPx) {
        if (scrimSize == IntSize.Zero) anchor
        else Offset(
            anchor.x.coerceIn(radiusPx, (scrimSize.width - radiusPx).coerceAtLeast(radiusPx)),
            anchor.y.coerceIn(radiusPx, (scrimSize.height - radiusPx).coerceAtLeast(radiusPx))
        )
    }
    val positions = remember(center, actions.size, radiusPx) {
        radialRingPositions(center, actions.size, radiusPx)
    }
    // Open morph: blobs grow out of the press point and settle into the ring.
    val morph = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        morph.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    // Live highlight: the disc nearest the finger (generous hit slop).
    val activeIndex = cursor?.let { radialPickIndex(center, actions.size, radiusPx, hitPx, it) }
    // Release resolution: pick the nearest disc, else cancel.
    var pickedIndex by remember { mutableStateOf<Int?>(null) }
    var ripples by remember { mutableStateOf(listOf<RippleSpec>()) }
    var rippleSeq by remember { mutableStateOf(0) }
    LaunchedEffect(endPos) {
        if (endPos != null) {
            val pick = radialPickIndex(center, actions.size, radiusPx, hitPx, endPos)
            if (pick != null && pick in actions.indices) {
                pickedIndex = pick
                rippleSeq += 1
                ripples = ripples + RippleSpec(positions[pick], rippleSeq)
                delay(240)
                actions[pick].onClick()
            } else {
                onCancel()
            }
        }
    }
    // Ripples expire on their own (see RippleBurst) — drop finished ones.
    var deadRipples by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val liveRipples = ripples.filterNot { it.id in deadRipples }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { scrimSize = it }
            .onGloballyPositioned { overlayRoot = it.positionInRoot() }
    ) {
        // ── Liquid (goo) layer — blobs with a water-like merge ───────
        val primary = MaterialTheme.colorScheme.primary
        val accentDark = MaterialTheme.colorScheme.primary
        val blobBrush = remember(primary) {
            Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    primary.copy(alpha = 0.55f),
                    accentDark.copy(alpha = 0.38f)
                ),
                center = Offset(0.32f, 0.28f),
                radius = 1f
            )
        }
        val gooChain = remember(primary) { buildGooRenderEffect() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayerCompat(gooChain)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // Center pool — fades once the ring settles.
                val cp = centerPx(center, overlayRoot)
                drawGooBlob(cp, discPx * 0.55f * (1f - morph.value * 0.55f), blobBrush, alpha = 1f - morph.value * 0.55f)
                // Ring blobs — grow out of the center to their ring slot.
                positions.forEachIndexed { i, p ->
                    val eased = easeOutBack(((morph.value - i * 0.07f).coerceIn(0f, 1f)))
                    val pos = Offset(
                        cp.x + (p.x - cp.x) * eased,
                        cp.y + (p.y - cp.y) * eased
                    )
                    val grow = discPx * (0.42f + 0.58f * eased)
                    val isActive = activeIndex == i
                    drawGooBlob(
                        pos,
                        grow * (if (isActive) 1.18f else 1f),
                        blobBrush,
                        alpha = 0.95f
                    )
                }
                // Finger blob — follows the drag, fatter over a disc.
                if (cursor != null) {
                    val c = centerPx(cursor, overlayRoot)
                    drawGooBlob(
                        c,
                        if (activeIndex != null) discPx * 0.42f else discPx * 0.32f,
                        blobBrush,
                        alpha = 0.9f
                    )
                }
            }
        }
        // ── Crisp discs — the actual action buttons (unfiltered) ─────
        actions.forEachIndexed { i, action ->
            val isActive = activeIndex == i
            val isPicked = pickedIndex == i
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (positions[i].x - overlayRoot.x).roundToInt(),
                            (positions[i].y - overlayRoot.y).roundToInt()
                        )
                    }
                    .size(discPx.dp)
                    .graphicsLayer {
                        val t = easeOutBack((morph.value - i * 0.07f).coerceIn(0f, 1f))
                        scaleX = t * (if (isActive) 1.16f else 1f)
                        scaleY = t * (if (isActive) 1.16f else 1f)
                        alpha = t
                    }
                    .border(
                        width = if (isActive || isPicked) 2.dp else 1.dp,
                        color = when {
                            isPicked -> MaterialTheme.colorScheme.tertiary
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        },
                        shape = CircleShape
                    )
                    .background(
                        when {
                            isPicked -> MaterialTheme.colorScheme.tertiaryContainer
                            isActive -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = action.glyph,
                    contentDescription = action.description,
                    size = 19.dp,
                    tint = when {
                        isPicked -> MaterialTheme.colorScheme.onTertiaryContainer
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> action.contentColor
                    }
                )
            }
        }
        // ── Ripples — an expanding ring at open + at pick ────────────
        liveRipples.forEach { r ->
            RippleBurst(
                origin = r.origin,
                overlayRoot = overlayRoot,
                picked = r.picked,
                onFinished = { deadRipples = deadRipples + r.id }
            )
        }
    }
}

private data class RippleSpec(val origin: Offset, val id: Int, val picked: Boolean = false)

/** One expanding + fading ring (open ripple = theme primary, pick = gold). */
@Composable
private fun RippleBurst(
    origin: Offset,
    overlayRoot: Offset,
    picked: Boolean,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(620))
        onFinished()
    }
    val color = if (picked) Color(0xFFFFB648) else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (origin.x - overlayRoot.x).roundToInt(),
                    (origin.y - overlayRoot.y).roundToInt()
                )
            }
            .size((12 * (1 + progress.value * 8)).dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = 0.85f * (1f - progress.value)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx())
            )
        }
    }
}

/** Draws one gooey blob — a soft radial-gradient circle (the goo filter on
 *  the layer merges overlapping blobs into a liquid whole). */
private fun DrawScope.drawGooBlob(center: Offset, radius: Float, brush: Brush, alpha: Float) {
    if (radius <= 0f) return
    drawCircle(brush = brush, radius = radius, center = center, alpha = alpha)
}

/** Root → overlay-local coordinate mapping (both are px). */
private fun centerPx(rootPos: Offset, overlayRoot: Offset): Offset =
    Offset(rootPos.x - overlayRoot.x, rootPos.y - overlayRoot.y)

/** Overshoot-free pop for the blobs/discs (matches the pill's springy look). */
private fun easeOutBack(t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
}

private fun Float.pow(e: Int): Float {
    var r = 1f
    repeat(e) { r *= this }
    return r
}

/**
 * The goo filter: blur + alpha-contrast chain (like an SVG goo filter), so
 * overlapping blobs visibly MERGE into a water-like whole. RenderEffect is
 * API 31+; older devices render the soft circles without the merge.
 */
private fun buildGooRenderEffect(): androidx.compose.ui.graphics.RenderEffect? {
    if (android.os.Build.VERSION.SDK_INT < 31) return null
    return try {
        androidx.compose.ui.graphics.RenderEffect.createChainEffect(
            androidx.compose.ui.graphics.RenderEffect.createBlurEffect(
                22f, 22f, androidx.compose.ui.graphics.ShaderTileMode.DECAL
            ),
            androidx.compose.ui.graphics.RenderEffect.createColorFilterEffect(
                androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, 0f,
                        0f, 0f, 0f, 26f, -11f
                    ))
                )
            )
        )
    } catch (_: Throwable) {
        null
    }
}

/** Applies [effect] (when non-null) to this layer; no-op otherwise. */
private fun Modifier.graphicsLayerCompat(effect: androidx.compose.ui.graphics.RenderEffect?): Modifier =
    if (effect != null) {
        this.graphicsLayer { renderEffect = effect }
    } else {
        this
    }
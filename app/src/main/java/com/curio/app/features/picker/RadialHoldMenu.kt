package com.curio.app.features.picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════
// v330 — HOLD ACTION MENU (compact edition)
// ═══════════════════════════════════════════════════════════════════════
// The category picker's tap-and-hold actions, rewritten from the v323
// gooey radial ring (which read as huge, collapsed the moment the finger
// lifted, and forced a drag-to-pick gesture) into a small, readable
// option menu:
//  - Hold a tile → a compact card of icon+label pills pops in ABOVE the
//    finger, springy scale + fade.
//  - It STAYS OPEN after the finger lifts (no more instant collapse):
//    tap an action to fire it, tap anywhere outside to dismiss, and it
//    auto-dismisses after ~6s of idling so it never blocks the sheet.
//  - NO drag-while-holding required — the old drag-to-pick ring is gone.
//
// The gesture lives on the tile ([radialHoldMenu]); the visuals live in
// [RadialHoldMenuOverlay], which the screen renders at the held anchor.

/** Compact menu sizing (dp) — a fraction of the old ring's footprint. */
private const val MENU_MAX_WIDTH_DP = 220f
private const val MENU_ITEM_HEIGHT_DP = 42f

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
 *    threshold — the menu anchors there.
 *  - [onMove] / [onEnd] are fed for call-site compatibility; the compact
 *    menu does not react to the finger while held (no drag-to-pick).
 *  - [onTap] fires on a quick tap (no hold).
 */
class HoldSession(
    val onOpen: (Offset) -> Unit,
    val onMove: (Offset) -> Unit,
    val onEnd: (Offset) -> Unit,
    val onTap: () -> Unit
)

/**
 * Attaches the hold gesture. Place BEFORE any clickable in the chain so the
 * hold owns the pointer once it opens. The long-press uses the system's own
 * timeout ([LocalViewConfiguration.longPressTimeoutMillis]).
 *
 * v325 — this Compose generation (BOM 2026.05) removed
 * `PointerInputChange.positionInRoot()` and made the gesture scope
 * `@RestrictsSuspension` (no `launch`/`delay` inside it), so: the long-press
 * timer runs on a [rememberCoroutineScope] coroutine (never inside the
 * restricted scope), and root coordinates are computed as
 * `change.position` (node-local) + the node's own
 * `LayoutCoordinates.positionInRoot()` captured via [onGloballyPositioned]
 * and read fresh through [rememberUpdatedState] (the never-restarting
 * gesture must not close over a stale root).
 */
fun Modifier.radialHoldMenu(hold: HoldSession?): Modifier = composed {
    if (hold == null) return@composed this
    val viewConfig = LocalViewConfiguration.current
    // A REAL CoroutineScope for the hold timer — the awaitEachGesture scope
    // is restricted and cannot run delay()/launch().
    val scope = rememberCoroutineScope()
    var nodeRoot by remember { mutableStateOf(Offset.Zero) }
    // NOT `by` — rememberUpdatedState returns a State and we need the State
    // itself so the gesture reads `.value` fresh on every pointer event
    // (a `by` delegate unwraps it and would close over the stale offset).
    val currentRoot = rememberUpdatedState(nodeRoot)
    this.onGloballyPositioned { nodeRoot = it.positionInRoot() }
        .pointerInput(hold) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pressPos = down.position + currentRoot.value
                var opened = false
                val holdJob = scope.launch {
                    delay(viewConfig.longPressTimeoutMillis)
                    opened = true
                    hold.onOpen(pressPos)
                }
                // v337 — SCROLL-CANCEL: while the menu hasn't opened yet, any
                // pointer travel beyond touch slop means this is a SCROLL (or
                // a drag past the tile), not a hold — the timer is cancelled
                // and the gesture gives up, so scrolling through the grids no
                // longer pops an option menu at the release point. Before
                // this, only a lift aborted the hold timer: a finger that
                // lingered even briefly on a tile mid-scroll opened the menu
                // the instant the timeout passed.
                val slop = viewConfig.touchSlop
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.pressed) {
                        if (!opened) {
                            val moved = (change.position + currentRoot.value - pressPos).getDistance()
                            if (moved > slop) {
                                holdJob.cancel()
                                break
                            }
                        } else {
                            hold.onMove(change.position + currentRoot.value)
                            // Own the pointer once the menu is open so the
                            // clickable underneath never sees the release.
                            change.consume()
                        }
                    } else {
                        holdJob.cancel()
                        if (opened) hold.onEnd(change.position + currentRoot.value) else hold.onTap()
                        break
                    }
                }
                holdJob.cancel()
            }
        }
}

/**
 * The hold-action menu visuals: a compact card of icon+label pill buttons,
 * springy pop-in ABOVE the held spot. It stays open after the finger lifts
 * (no instant collapse): tap an action to fire it, tap the surrounding
 * scrim to dismiss, and it auto-dismisses after ~6s idle. [cursor] and
 * [endPos] are accepted for call-site compatibility (the old drag-to-pick
 * ring consumed them; the compact menu ignores both).
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
    var scrimSize by remember { mutableStateOf(IntSize.Zero) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    var overlayRoot by remember { mutableStateOf(Offset.Zero) }

    // ── Entry animation: springy scale + fade (starts invisible so the
    // first frame's top-left placement never flashes). ───────────────
    val popScale = remember { Animatable(0.72f) }
    val popAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        popScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
        popAlpha.animateTo(1f, tween(150))
    }
    // ── Exit: fade out, then tell the caller to clear the menu. ──────
    var closing by remember { mutableStateOf(false) }
    val dismissAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    fun dismiss() {
        if (closing) return
        closing = true
        scope.launch {
            dismissAlpha.animateTo(0f, tween(150))
            onCancel()
        }
    }
    // ── Auto-dismiss AFTER the finger lifts (~6s idle). While the finger
    // is still down the menu stays put; once released the clock starts so
    // an un-picked menu never blocks the sheet forever. ───────────────
    LaunchedEffect(endPos) {
        if (endPos != null) {
            delay(6000)
            dismiss()
        }
    }

    // Position: centered on the held spot, floated ~16dp ABOVE the finger
    // (flip below if there's no room), clamped inside the sheet. The
    // anchor arrives in ROOT px — subtract the overlay's own root offset.
    val gapPx = with(density) { 16.dp.toPx() }
    val offset = remember(anchor, scrimSize, menuSize, overlayRoot) {
        if (scrimSize == IntSize.Zero || menuSize == IntSize.Zero) IntOffset.Zero
        else {
            val localX = anchor.x - overlayRoot.x
            val localY = anchor.y - overlayRoot.y
            val x = (localX - menuSize.width / 2f).roundToInt()
                .coerceIn(8, (scrimSize.width - menuSize.width - 8).coerceAtLeast(8))
            val above = localY - menuSize.height - gapPx
            val y = if (above >= 8) above.roundToInt()
            else (localY + gapPx).roundToInt()
                .coerceIn(8, (scrimSize.height - menuSize.height - 8).coerceAtLeast(8))
            IntOffset(x, y)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { scrimSize = it }
            .onGloballyPositioned { overlayRoot = it.positionInRoot() }
            .graphicsLayer { alpha = dismissAlpha.value }
    ) {
        // ── Scrim: tap anywhere outside the menu to dismiss. ──────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismiss() }
        )
        // ── The menu card — compact icon+label pill buttons. ──────────
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 10.dp,
            modifier = Modifier
                .offset { offset }
                .onSizeChanged { menuSize = it }
                .graphicsLayer {
                    val hidden = menuSize == IntSize.Zero
                    scaleX = if (hidden) 0f else popScale.value
                    scaleY = if (hidden) 0f else popScale.value
                    alpha = if (hidden) 0f else popAlpha.value
                }
                .widthIn(max = MENU_MAX_WIDTH_DP.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(6.dp)
            ) {
                actions.forEach { action ->
                    Surface(
                        onClick = action.onClick,
                        shape = RoundedCornerShape(13.dp),
                        color = action.background,
                        modifier = Modifier.height(MENU_ITEM_HEIGHT_DP.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            CurioIcon(
                                name = action.glyph,
                                contentDescription = null,
                                size = 18.dp,
                                tint = action.contentColor
                            )
                            Text(
                                text = action.description,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = action.contentColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
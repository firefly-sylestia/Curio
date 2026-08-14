package com.curio.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.curio.app.data.CaptureData
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.ui.components.NotePaperCard
import com.curio.app.ui.components.limitQuoteContent
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.PatrickHandFontFamily
import com.curio.app.ui.theme.notePaperInk
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

/**
 * Mutable zoom state for one mood-board canvas. Only a SINGLE tile image is
 * ever magnified ([zoomedUri]) — the board around it stays put. The overlay
 * glides that one image from its resting spot on the collage to the center
 * of the viewport along an ARC (v7.24), then keeps it there until the user
 * taps to close; pinch-to-zoom and one-finger pan refine the magnified
 * image while it's open, and a double-tap adds one more zoom step (v7.36).
 *
 * [scaleTarget] and the pan offsets in pixels are the animation TARGETS
 * (the image centered at that zoom); the animated values live inside
 * [MoodBoardZoomOverlay] so the glide opens and closes smoothly.
 *
 * [closing] is a latch set by [zoomOut]: the overlay only removes itself
 * once the close animation has settled.
 */
class MoodBoardZoomState {
    var zoomedUri by mutableStateOf<String?>(null)
    var scaleTarget by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var closing by mutableStateOf(false)
    // The zoom level [zoomIn] opens at for the CURRENT tile — fit-based so a
    // small tile opens large enough to read while a big tile doesn't blow
    // past the screen.
    var defaultScale by mutableFloatStateOf(2.4f)

    /**
     * Double-tap a tile: only that image zooms. The overlay glides it from
     * its resting spot to the viewport CENTER at the fit-based zoom level,
     * so the screen visibly swoops to the image without moving the board.
     * [tileW]/[tileH] are the tile's on-screen size and [viewW]/[viewH] the
     * viewport it glides within — the centering target is derived purely
     * from those (the resting spot lives in the overlay, which reports the
     * tile's position itself). [centerX]/[centerY] are accepted for call-site
     * compatibility but not used by the target math.
     */
    fun zoomIn(
        uri: String,
        centerX: Float = 0f,
        centerY: Float = 0f,
        tileW: Float = 0f,
        tileH: Float = 0f,
        viewW: Float = 0f,
        viewH: Float = 0f
    ) {
        closing = false
        if (zoomedUri != uri) {
            zoomedUri = uri
            scaleTarget = 1f
            offsetX = 0f
            offsetY = 0f
        }
        val safeTileW = tileW.takeIf { it.isFinite() && it > 0f }?.coerceAtMost(8192f) ?: 1f
        val safeTileH = tileH.takeIf { it.isFinite() && it > 0f }?.coerceAtMost(8192f) ?: 1f
        val safeViewW = viewW.takeIf { it.isFinite() && it > 0f }?.coerceAtMost(8192f) ?: 1f
        val safeViewH = viewH.takeIf { it.isFinite() && it > 0f }?.coerceAtMost(8192f) ?: 1f
        defaultScale = fitZoomScale(safeTileW, safeTileH, safeViewW, safeViewH)
        scaleTarget = defaultScale
        // Glide target: the overlay lays the image out at the viewport's
        // top-left and its graphicsLayer scales about the image's OWN center
        // (default transformOrigin), so the image's visual center = its
        // layout center + translation at ANY scale. Centering it on the
        // viewport therefore needs a SCALE-INDEPENDENT translation:
        //   offsetX = viewW/2 − w/2,  offsetY = viewH/2 − h/2
        // (the scaled image stays centered as it zooms). The tile's resting
        // spot is NOT part of this target — the overlay owns it via its own
        // tileX/tileY and returns there on close.
        offsetX = (safeViewW / 2f - safeTileW / 2f).coerceIn(-8192f, 8192f)
        offsetY = (safeViewH / 2f - safeTileH / 2f).coerceIn(-8192f, 8192f)
    }

    /**
     * Fit-based open zoom: ~90% of the viewport's short side, clamped 1.1–5x.
     * The 1.1f floor means a tile that already fills the board opens at
     * essentially its fit size (straight + a slight lift) instead of
     * exploding past the screen — while small tiles still zoom in a lot.
     */
    private fun fitZoomScale(tileW: Float, tileH: Float, viewW: Float, viewH: Float): Float {
        if (tileW <= 0f || tileH <= 0f || viewW <= 0f || viewH <= 0f) return 2.4f
        val fit = minOf(viewW / tileW, viewH / tileH)
        return (fit * 0.9f).coerceIn(1.1f, 5f)
    }

    /**
     * Zoom back out to 1x; the overlay clears itself once the close
     * animation settles.
     */
    fun zoomOut() {
        closing = true
        scaleTarget = 1f
        offsetX = 0f
        offsetY = 0f
    }
}

/** Remember a [MoodBoardZoomState] scoped to one board canvas. */
@Composable
fun rememberMoodBoardZoomState(): MoodBoardZoomState = remember { MoodBoardZoomState() }

// ── High-resolution image loading ─────────────────────────────────────
// Coil's default decode size tracks the composable's on-screen size
// (~160dp), so magnifying a tile to 2.4-4x upscaled a tiny bitmap and looked
// awful. Decode larger instead: board tiles get a ~3-4x headroom cap and the
// magnified overlays a higher cap, so zoomed-in detail stays pixel-sharp.

/**
 * v7.19 — zoom animation spec: opening uses a deliberate spring so the
 * magnify reads as a physical glide, but CLOSING uses a fast tween so the
 * minimize snaps shut instead of lagging/delaying behind a long spring
 * tail (the old 280-stiffness spring took ~half a second to settle and
 * the overlay only removed itself after that).
 */
private fun moodBoardZoomSpec(closing: Boolean): FiniteAnimationSpec<Float> =
    if (closing) tween(durationMillis = 170, easing = FastOutSlowInEasing)
    else spring(dampingRatio = 0.8f, stiffness = 320f)

/** Decode cap for board tiles (~3-4x their on-screen size). */
private const val MoodBoardTileDecodePx = 1024

/**
 * Decode cap for the magnified overlays (v8.2 — raised from 2048 to 4096 so
 * the zoomed image supports the full 8x pinch at ~1:1 instead of upscaling a
 * small bitmap). Matches the export's own 4096 cap, so a zoomed photo shows
 * exactly as much detail as the saved PNG.
 */
private const val MoodBoardZoomDecodePx = 4096

/**
 * High-resolution Coil painter for mood-board images. [zoomed] requests the
 * larger cap for the magnified overlays so the zoomed image stays crisp.
 */
@Composable
fun moodBoardPainter(uri: String, zoomed: Boolean = false): Painter {
    val context = LocalContext.current
    val cap = if (zoomed) MoodBoardZoomDecodePx else MoodBoardTileDecodePx
    return rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(uri)
            .size(cap, cap)
            .build()
    )
}

/**
 * Static mood-board tile collage — renders saved [CaptureData.TileLayout]s at
 * their pixel offsets with their rotations. Shared by the saved EntryDetail
 * board, the expanded full-screen board and the board-magnifier overlay.
 * When [onTileZoom] is provided each tile responds to double-tap.
 */
@Composable
fun MoodBoardTiles(
    tiles: List<CaptureData.TileLayout>,
    canvasWPx: Float,
    canvasHPx: Float,
    onTileZoom: ((String, Float, Float, Float, Float, Float, Float) -> Unit)? = null,
    zoomed: Boolean = false
) {
    val density = LocalDensity.current
    tiles.forEachIndexed { i, tile ->
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        tile.offsetXPx.roundToInt().coerceIn(0, canvasWPx.roundToInt()),
                        tile.offsetYPx.roundToInt().coerceIn(0, canvasHPx.roundToInt())
                    )
                }
                .zIndex(i.toFloat())
                .then(
                    if (onTileZoom != null) {
                        // Double-tap zooms the tile — same gesture as the
                        // editor. A single tap on the saved board intentionally
                        // does nothing, so the double-tap timeout doesn't
                        // delay any action (there is no single-tap action).
                        // v7.24 — the callback now reports the tile's
                        // VIEWPORT position (x, y in this Box's space), so
                        // the overlay can glide the image from its resting
                        // spot on the collage to the viewport center.
                        Modifier.pointerInput(tile.uri) {
                            detectTapGestures(
                                onDoubleTap = {
                                    onTileZoom(
                                        tile.uri,
                                        tile.offsetXPx,
                                        tile.offsetYPx,
                                        tile.widthPx,
                                        tile.heightPx,
                                        canvasWPx,
                                        canvasHPx
                                    )
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(
                        width = with(density) { tile.widthPx.toDp() },
                        height = with(density) { tile.heightPx.toDp() }
                    )
                    .rotate(tile.rotationDeg)
            ) {
            Image(
                // v8.2 — sharpness comes from the DECODE CAP (moodBoardPainter
                // decodes zoomed tiles at 4096px); Coil 2 has no request-level
                // filter quality, and foundation 1.4+ removed the painter
                // overload's filterQuality parameter.
                painter = moodBoardPainter(tile.uri, zoomed),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                        // v6.1 — no inner padding: tiles are sized to the
                        // photo's own aspect ratio, so the image fills the
                        // rounded box edge-to-edge (no white frame).
                        .clip(RoundedCornerShape(18.dp))
                )
            }

        }
    }
}

/**
 * In-place zoom overlay for ONE magnified tile (v7.24). Drop it as the LAST
 * child of the board's Box scope, sized to the viewport: the tapped image
 * GLIDES from its resting spot on the collage to the CENTER of the viewport
 * along an ARC (perpendicular bow, sin(π·t) — the same swoop the old
 * whole-board zoom used), scaling up as it travels. The board around it
 * stays completely still. Once landed, pinch-to-zoom (up to 8x) and
 * one-finger pan refine the image; a tap closes it (fast tween back to the
 * resting spot, no laggy spring tail).
 *
 * v7.36 — a SINGLE gesture detector owns the overlay: pinch/pan refine the
 * image and taps are classified on the same stream, so a pinch begun right
 * after a double-tap is never eaten (the old two-detector design desynced
 * once the image's tap detector consumed events).
 *
 * v7.38 — SIMPLE, IMMEDIATE TAP SEMANTICS: a clean tap closes the overlay
 * (gliding back to the tile) when the image is at its base zoom, or springs
 * the pinch/pan back to base and stays open when refined. Double-tap closes
 * too — the first tap already starts the close, so there is no timing
 * window and nothing waits: no 300ms pending action, no "double-tap zooms
 * in instead of closing" surprise, no spring racing a follow-up pinch.
 *
 * [tileX]/[tileY] are the tile's top-left in VIEWPORT pixels (board offset
 * already applied), [widthPx]/[heightPx] its size, [viewW]/[viewH] the
 * viewport the image glides within.
 */
@Composable
fun MoodBoardZoomOverlay(
    zoomState: MoodBoardZoomState,
    tileUri: String,
    tileX: Float,
    tileY: Float,
    widthPx: Float,
    heightPx: Float,
    viewW: Float,
    viewH: Float,
    modifier: Modifier = Modifier
) {
    if (zoomState.zoomedUri == null) return
    // A malformed legacy tile must never become an invalid Compose size or
    // an unbounded pan calculation. Valid boards stay within these generous
    // bounds; degenerate/absurd records get a small, drawable fallback.
    val safeWidthPx = widthPx.takeIf { it.isFinite() && it > 0f }
        ?.coerceAtMost(8192f) ?: 1f
    val safeHeightPx = heightPx.takeIf { it.isFinite() && it > 0f }
        ?.coerceAtMost(8192f) ?: 1f
    val safeViewW = viewW.takeIf { it.isFinite() && it > 0f } ?: 1f
    val safeViewH = viewH.takeIf { it.isFinite() && it > 0f } ?: 1f
    val safeTileX = tileX.takeIf { it.isFinite() }?.coerceIn(-8192f, 8192f) ?: 0f
    val safeTileY = tileY.takeIf { it.isFinite() }?.coerceIn(-8192f, 8192f) ?: 0f
    val safeOffsetX = zoomState.offsetX.takeIf { it.isFinite() }?.coerceIn(-8192f, 8192f) ?: 0f
    val safeOffsetY = zoomState.offsetY.takeIf { it.isFinite() }?.coerceIn(-8192f, 8192f) ?: 0f
    val density = LocalDensity.current
    // ── Arc glide clock — ONE shared clock drives scale + pan in phase so
    // the image swoops from its resting spot to the centered target, and
    // back on close. Plain float states (the arc writes them per-frame).
    // Keyed on the tile URI so switching the zoomed tile resets the glide
    // to the NEW tile's resting spot (a stale glideX/Y from the previous
    // tile would make the next open start from the wrong place).
    var glideScale by remember(tileUri) { mutableFloatStateOf(1f) }
    var glideX by remember(tileUri) { mutableFloatStateOf(safeTileX) }
    var glideY by remember(tileUri) { mutableFloatStateOf(safeTileY) }
    val glideProgress = remember { Animatable(0f) }

    // ── Pinch / pan refinement — applied ON TOP of the glide (neutral at
    // rest; per-gesture deltas are consumed so no tap fires after a drag).
    var pinchScale by remember { mutableFloatStateOf(1f) }
    var pinchX by remember { mutableFloatStateOf(0f) }
    var pinchY by remember { mutableFloatStateOf(0f) }
    // Double-tap on the magnified image resets a pinch/pan back to the
    // fit-based zoom — incremented each time, the counter drives a smooth
    // spring back to the neutral resting state.
    var resetTick by remember { mutableIntStateOf(0) }
    // Double-tap reset: animate the pinch/pan back to the fit zoom (scale
    // 1x, centered) with a soft spring, then neutralize — a later double-tap
    // while un-pinched adds a zoom step instead (see below).
    val resetScale = remember { Animatable(1f) }
    val resetX = remember { Animatable(0f) }
    val resetY = remember { Animatable(0f) }
    LaunchedEffect(resetTick) {
        if (resetTick == 0) return@LaunchedEffect
        resetScale.snapTo(pinchScale)
        resetX.snapTo(pinchX)
        resetY.snapTo(pinchY)
        // Run the three springs in parallel. Each lambda paints the plain
        // float states from its Animatable every frame (Animatable.value is
        // always current — no stale closures), so the graphicsLayer follows
        // the reset smoothly; the effect ends when the springs settle.
        kotlinx.coroutines.coroutineScope {
            launch {
                resetScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 260f)) {
                    pinchScale = this.value
                }
            }
            launch {
                resetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 260f)) {
                    pinchX = this.value
                }
            }
            launch {
                resetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 260f)) {
                    pinchY = this.value
                }
            }
        }
    }

    LaunchedEffect(
        zoomState.scaleTarget, zoomState.offsetX, zoomState.offsetY,
        zoomState.closing
    ) {
        // Resting spot: the tile's own position at scale 1. Target: the
        // image centered at the fit zoom ([offsetX]/[offsetY] are the
        // translation that centers it). On CLOSE, glide back to the TILE's
        // resting spot ([tileX]/[tileY]) — zoomOut() zeroes the offsets for
        // the latch, so the overlay must target the tile itself, not (0,0)
        // (the top-left corner). The arc bows perpendicular to the travel
        // direction, peaking mid-flight, straight on close.
        val fromScale = glideScale
        val fromX = glideX
        val fromY = glideY
        // The user's pinch/pan rides ON TOP of the glide, so on close the
        // whole transform must return to the resting spot: glide → the
        // tile, pinch → neutral. Capturing the CURRENT pinch lets the close
        // tween fold it back in smoothly instead of popping.
        val fromPinchScale = pinchScale
        val fromPinchX = pinchX
        val fromPinchY = pinchY
        val toScale = if (zoomState.closing) 1f else zoomState.scaleTarget
        val toX = if (zoomState.closing) safeTileX else safeOffsetX
        val toY = if (zoomState.closing) safeTileY else safeOffsetY
        val dx = toX - fromX
        val dy = toY - fromY
        val len = sqrt(dx * dx + dy * dy)
        val perpX = if (len > 0.5f) dy / len else 0f
        val perpY = if (len > 0.5f) -dx / len else 0f
        val arcPeak = if (zoomState.closing) 0f else (len * 0.16f).coerceIn(32f, 150f)
        glideProgress.snapTo(0f)
        glideProgress.animateTo(1f, moodBoardZoomSpec(zoomState.closing)) {
            val t = this.value
            // Clamp the BULGE progress: a spring overshoots past 1.0, and
            // sin(π·t) goes negative there — unclamped it would flip the
            // arc the other way at the very end. The lerp stays unclamped
            // so the spring's natural settle still lands softly.
            val arcT = t.coerceIn(0f, 1f)
            val bulge = arcPeak * sin(PI * arcT).toFloat()
            glideScale = lerp(fromScale, toScale, t)
            glideX = lerp(fromX, toX, t) + perpX * bulge
            glideY = lerp(fromY, toY, t) + perpY * bulge
            // Fold the pinch/pan back to neutral in the same clock so the
            // closed image sits EXACTLY on its tile (no residual offset).
            if (zoomState.closing) {
                pinchScale = lerp(fromPinchScale, 1f, t)
                pinchX = lerp(fromPinchX, 0f, t)
                pinchY = lerp(fromPinchY, 0f, t)
            }
        }
        // The glide (open or close) finished — reset the pinch so the next
        // open starts neutral.
        pinchScale = 1f
        pinchX = 0f
        pinchY = 0f
    }

    // Remove the overlay once the close animation settles back at 1x AND the
    // pinch is neutral again — otherwise the overlay could vanish while the
    // image is still visibly enlarged/offset.
    LaunchedEffect(glideScale, pinchScale) {
        if (zoomState.closing && zoomState.scaleTarget <= 1.01f &&
            glideScale <= 1.01f && pinchScale <= 1.01f
        ) {
            zoomState.zoomedUri = null
            zoomState.closing = false
        }
    }

    // v7.36 — ONE gesture detector owns the whole overlay: pinch/pan refine
    // the magnified image and taps are classified on the SAME event stream.
    // (The old two-detector design desynced after a double-tap — the image's
    // detectTapGestures consumed tap events while this box's awaitFirstDown
    // skipped consumed downs, so a pinch/drag begun right after a double-tap
    // could be eaten and never refine the image.) With a single detector
    // every down is seen regardless of consumption:
    //  - movement / multi-touch → pinch-to-zoom + one-finger pan (a small
    //    tolerance keeps micro-movements from being misread as taps),
    //  - clean taps → close when at the base zoom; spring back to base and
    //    stay open when pinched/panned. Double-tap closes too (instantly,
    //    no timing window).
    // Tapping anywhere on the overlay closes — no dark scrim, no page.
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1000f)
            .pointerInput(tileUri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var hasMovement = false
                    // Cumulative pan since the down — a SLOW one-finger drag
                    // moves only a fraction of a pixel per event, so a
                    // per-event threshold would never trip and the drag would
                    // read as a tap (and close the image on release). Every
                    // delta is accumulated; the tolerance gates only the
                    // tap-vs-movement classification, never the pan itself.
                    var totalPan = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) hasMovement = true
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        totalPan += panChange
                        // Tolerance instead of exact-float equality: a gentle
                        // pinch/drag can move sub-pixel per event, and exact
                        // equality let those micro-movements read as taps
                        // (the image then "didn't zoom" — a close fired
                        // while the user was pinching). Cumulative pan of a
                        // few px (or any real zoom change / second finger)
                        // marks the gesture as movement.
                        if (kotlin.math.abs(zoomChange - 1f) > 0.02f ||
                            totalPan.getDistance() > 4f
                        ) {
                            hasMovement = true
                        }
                        // Apply EVERY event's deltas to the state while the
                        // fingers are down so the image tracks them live —
                        // including the first sub-threshold pixels of a
                        // drag (no dead-zone: the very first movement
                        // already moves the image). A clean tap's sub-pixel
                        // jitter lands harmlessly — pan is clamped to zero
                        // at rest and the tap classifier below ignores
                        // drift under the movement threshold.
                        pinchScale = (pinchScale * zoomChange).coerceIn(1f, 8f)
                        // Clamp the pan to the viewport (same rule the old
                        // full-screen viewer used): the image can travel
                        // just far enough to bring its edges to the viewport
                        // edges at the current zoom, and ZERO at rest — a
                        // tiny tile can never be dragged fully off-screen,
                        // and pinching back out recenters it automatically.
                        val s = glideScale * pinchScale
                        val maxPanX = ((safeWidthPx * s - safeViewW) / 2f).coerceAtLeast(0f)
                        val maxPanY = ((safeHeightPx * s - safeViewH) / 2f).coerceAtLeast(0f)
                        pinchX = (pinchX + panChange.x).coerceIn(-maxPanX, maxPanX)
                        pinchY = (pinchY + panChange.y).coerceIn(-maxPanY, maxPanY)
                        if (hasMovement) event.changes.forEach { it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                    if (!hasMovement) {
                        // A clean tap (no movement). When the image is
                        // pinched/panned, spring back to the base zoom and
                        // STAY open; otherwise close, gliding back to the
                        // tile. Double-tap closes too — the first tap already
                        // starts the close, so there is no timing window to
                        // wait out and no delayed action (the old 300ms
                        // pending-tap felt laggy, and its "double-tap zooms
                        // in" spring raced the user's next pinch).
                        if (zoomState.closing) {
                            // Second tap of a double-tap-close: the close
                            // glide is already folding the pinch back — do
                            // nothing (never start a reset spring mid-close).
                        } else {
                            // Threshold-consistent classification: pan is
                            // clamped to 0 at base zoom, so a "refined"
                            // state is purely the zoom level — and this
                            // gesture's drift under the movement threshold
                            // (totalPan ≤ 4px) still reads as a clean tap.
                            // (A tiny tap jitter now lands in pinchX/pinchY,
                            // so raw non-zero pan no longer means refined.)
                            val refined = pinchScale > 1.01f ||
                                totalPan.getDistance() > 4f
                            if (refined) resetTick++ else zoomState.zoomOut()
                        }
                    }
                }
            }
        // NO contentAlignment here: the image must lay out at the viewport's
        // top-left so the graphicsLayer translation IS its top-left position.
        // Centering the child (the old contentAlignment = Center) added a
        // (viewW − w)/2 shift on TOP of the translation, double-offsetting the
        // zoomed image — it opened shifted/outside the screen. The dismiss
        // button below uses explicit .align(), which is unaffected.
    ) {
        // The zoomed image — glides from its board spot to center, then
        // follows the user's pinch/pan. The whole transform lives in the
        // graphicsLayer (no layout offset): translationX/Y directly ARE the
        // image's top-left in viewport pixels, so at scale 1 with the
        // resting (tileX, tileY) it sits exactly on its tile. Taps are
        // classified by the ROOT detector above (single tap closes / springs
        // back when refined; double-tap closes instantly) — the image itself
        // carries NO gesture handler, so a pinch begun right after a
        // double-tap is never eaten by a competing detector.
        Box(
            modifier = Modifier
                .graphicsLayer {
                    // The image scales around its own center, so a
                    // translation of (tileX, tileY) at scale 1 puts it
                    // exactly on its resting tile.
                    scaleX = glideScale * pinchScale
                    scaleY = glideScale * pinchScale
                    translationX = glideX + pinchX
                    translationY = glideY + pinchY
                }
        ) {
            val imageModifier = Modifier
                .size(                     width = with(density) { safeWidthPx.toDp() },
                     height = with(density) { safeHeightPx.toDp() }
                )
                .clip(RoundedCornerShape(14.dp))
            // Frameless, like the editor tiles. The board-size painter renders
            // instantly (already cached from the collage) while the hi-res
            // decode for the magnifier streams in — no blank flash mid-glide.
            // v8.2 — both layers decode with a 4096px cap (moodBoardPainter),
            // so the zoomed image reads sharp even at the full 8x pinch;
            // Coil 2 has no request-level filter quality (foundation 1.4+
            // removed the painter overload's parameter), so upscale smoothing
            // during the glide is handled by the high-res layer arriving.
            Image(
                painter = moodBoardPainter(tileUri),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = imageModifier
            )
            Image(
                painter = moodBoardPainter(tileUri, zoomed = true),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = imageModifier
            )
        }

        // ── Top-end control — dismiss ──────────────────────────────────
        Surface(
            onClick = { zoomState.zoomOut() },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CurioIcon(
                    name = CurioIcons.Close,
                    contentDescription = "Close zoom",
                    tint = MaterialTheme.colorScheme.onSurface,
                    size = 18.dp
                )
            }
        }
    }
}

/** One deterministic floating-card slot on a [boardW]×[boardH] board. */
private data class MoodQuoteSlot(val x: Float, val y: Float, val w: Float, val h: Float)

private fun moodBoardQuoteSlot(index: Int, boardW: Float, boardH: Float): MoodQuoteSlot {
    val cols = 2
    val col = index % cols
    val row = index / cols
    val slotW = boardW * 0.5f
    val cardW = (slotW * 0.82f).coerceIn(120f, 240f)
    val cardH = cardW * 0.62f
    val x = col * slotW + (slotW - cardW) / 2f
    val y = boardH * 0.56f + row * (cardH * 1.02f) + (if (col == 1) cardH * 0.45f else 0f)
    return MoodQuoteSlot(x, y, cardW, cardH)
}

/**
 * v7.20 — the shared floating quote-card layer for mood boards.
 *
 * Renders the entry's paper quote boxes floating ON the collage at their
 * saved positions (editor board-pixel space, mapped into the current board
 * by [boardScale] — and shifted by [offsetX]/[offsetY] when the board is
 * centered inside a larger viewport, e.g. the inline card's center crop or
 * the expanded dialog's contain-fit). Cards never dragged (-1,-1) fall back
 * to the deterministic slot for their index, so legacy entries and freshly
 * added cards land exactly where the editor's "Quote" chip shows them.
 *
 * The EDITOR passes [onMoveCard] to make the cards draggable (committed to
 * the entry's [CaptureData.QuotePos] list); saved views pass null so the
 * cards are read-only there.
 */
@Composable
fun MoodBoardFloatingCards(
    quotes: List<String>,
    styles: List<NotePaperStyle>,
    colors: List<NotePaperColor>,
    tilts: List<Float>,
    positions: List<CaptureData.QuotePos>,
    canvasWPx: Float,
    canvasHPx: Float,
    boardScale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    onEditCard: ((Int) -> Unit)? = null,
    onMoveCard: ((index: Int, x: Float, y: Float) -> Unit)? = null,
    // v42 — commits a resized card's new width (RAW board px — the caller
    // divides the render-space width by the board scale, mirroring onMove).
    onResizeCard: ((index: Int, w: Float) -> Unit)? = null,
    // v7.22 — parallel per-card flag: false = the card renders BELOW the
    // board, so it must NOT float on the collage. Legacy entries lack the
    // list → null → every card floats (the v7.19 look).
    onBoard: List<Boolean>? = null,
    // v7.37 — stable torn-paper seed base: saved views pass a per-entry
    // seed so the on-board paper slips NEVER re-roll their tears between
    // opens (a per-index salt keeps the cards distinct on the board). Null
    // keeps the editor's remember-random behavior.
    seed: Int? = null
) {
    // Guard the pre-measure first frame (canvas = 0) and degenerate scales
    // so cards never stack at the top-left or divide by zero.
    if (canvasWPx <= 0f || canvasHPx <= 0f) return
    val scale = if (boardScale > 0f) boardScale else 1f
    quotes.forEachIndexed { i, quote ->
        // Skip below-board cards — they render under the board in their own
        // section, not on the collage. Missing flags (legacy) → on-board.
        if (onBoard != null && onBoard.getOrElse(i) { true } == false) return@forEachIndexed
        val slot = moodBoardQuoteSlot(i, canvasWPx, canvasHPx)
        val saved = positions.getOrElse(i) { CaptureData.QuotePos(-1f, -1f) }
        val placed = if (saved.x >= 0f && saved.y >= 0f) saved
            else CaptureData.QuotePos(slot.x, slot.y)
        // v42 — a resized card (saved.w >= 0) keeps its custom WIDTH; cards
        // never resized use the deterministic slot width as before.
        val cardW = if (saved.w >= 0f) saved.w else slot.w
        MoodBoardFloatingCard(
            text = quote,
            style = styles.getOrElse(i) { NotePaperStyle.RULED },
            color = colors.getOrElse(i) { NotePaperColor.CREAM },
            rotation = tilts.getOrElse(i) { (i * 4.2f % 8f) - 4f },
            x = placed.x * scale + offsetX,
            y = placed.y * scale + offsetY,
            w = cardW * scale,
            h = slot.h * scale,
            boardW = canvasWPx * scale,
            boardH = canvasHPx * scale,
            seed = seed?.let { it + i * 0x1F31 },
            onEdit = onEditCard?.let { edit -> { edit(i) } },
            onMove = onMoveCard?.let { move ->
                { rx, ry -> move(i, (rx - offsetX) / scale, (ry - offsetY) / scale) }
            },
            onResize = onResizeCard?.let { resize ->
                { rw -> resize(i, rw / scale) }
            }
        )
    }
}

/**
 * One floating paper quote card — tilt + paper look, draggable in the
 * editor, read-only (tap → no-op) in saved views. Drag preview lives INSIDE
 * the card (same scoping trick as the editor tiles) so per-frame drags
 * recompose only this card. Released positions are reported in RENDER
 * space, clamped to the board bounds.
 */
@Composable
private fun MoodBoardFloatingCard(
    text: String,
    style: NotePaperStyle,
    color: NotePaperColor,
    rotation: Float,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    boardW: Float,
    boardH: Float,
    // v7.37 — stable torn-paper seed, forwarded to the paper slip (see
    // [MoodBoardFloatingCards.seed]).
    seed: Int? = null,
    onEdit: (() -> Unit)? = null,
    onMove: ((Float, Float) -> Unit)? = null,
    // v42 — commit a resized card's new width (render-space px; the caller
    // divides by the board scale to store raw board px).
    onResize: ((Float) -> Unit)? = null
) {
    val density = LocalDensity.current
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    // v42 — the live resize preview (render-space px delta on the width).
    var resizeDelta by remember { mutableStateOf(0f) }
    var resizing by remember { mutableStateOf(false) }
    // The editing preview is capped at two visual lines, so a card's
    // measured height can only exceed the slot by one extra line. Keeping
    // drag bounds tied to its measured height (rather than the slot height)
    // still lets a slightly taller card stay fully on the board.
    var measuredHeightPx by remember { mutableIntStateOf(0) }
    // pointerInput never restarts, so the gesture coroutine must read the
    // LATEST geometry/callbacks — never the first composition's.
    val currentX by rememberUpdatedState(x)
    val currentY by rememberUpdatedState(y)
    val currentW by rememberUpdatedState(w)
    val currentCardH = measuredHeightPx.takeIf { it > 0 }?.toFloat() ?: h
    val currentH by rememberUpdatedState(currentCardH)
    val currentBoardW by rememberUpdatedState(boardW)
    val currentBoardH by rememberUpdatedState(boardH)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnResize by rememberUpdatedState(onResize)

    val renderX = (x + dragDelta.x).coerceIn(0f, (boardW - w).coerceAtLeast(0f))
    val renderY = (y + dragDelta.y).coerceIn(0f, (boardH - currentCardH).coerceAtLeast(0f))
    // v42 — width clamps: never smaller than half the slot (taps still hit
    // it), never larger than the board itself. Captured via
    // rememberUpdatedState so the never-restarting grip gesture reads the
    // LATEST clamps (a dragged/resized card moves them).
    val minW = (w * 0.5f).coerceAtLeast(60f)
    val maxW = (boardW - x).coerceAtLeast(minW)
    val currentMinW by rememberUpdatedState(minW)
    val currentMaxW by rememberUpdatedState(maxW)
    val renderW = (w + resizeDelta).coerceIn(currentMinW, currentMaxW)

    Box(
        modifier = Modifier
            .offset { IntOffset(renderX.roundToInt(), renderY.roundToInt()) }
            .zIndex(if (dragging || resizing) 55f else 50f)
            .width(with(density) { renderW.toDp() })
            .then(
                if (currentOnEdit != null) {
                    // Editor cards keep the board slot's minimum height, but
                    // the preview itself is capped at two lines (see below) —
                    // so a long quote can never stretch the slip to the
                    // board's full length. The explicit max makes that bound
                    // structural: even if the preview's maxLines ever grew,
                    // the slip stays at most 1.5× the slot height (two lines
                    // at any sane font scale). Saved/read-only cards retain
                    // the stable board slot height below.
                    Modifier.heightIn(
                        min = with(density) { h.toDp() },
                        max = with(density) { h.toDp() * 1.5f }
                    )
                } else {
                    Modifier.height(with(density) { h.toDp() })
                }
            )
            .rotate(rotation)
            .onSizeChanged { measuredHeightPx = it.height }
            // v57 — pinch-to-expand (EDITOR ONLY, when a resize handler
            // exists): two fingers on the card grow/shrink its width live
            // and commit it on release through the same path as the resize
            // grip. Placed BEFORE the drag handler so it wins on 2-finger
            // gestures — it only consumes once a second finger lands, so
            // single-finger drags pass through untouched.
            .then(
                if (currentOnResize != null) Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var pinching = false
                        var baseZoom = 1f
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            if (pressed >= 2) {
                                if (!pinching) {
                                    pinching = true
                                    resizing = true
                                    baseZoom = event.calculateZoom()
                                } else {
                                    // Live preview: the card's width tracks
                                    // the pinch (render px), clamped like the
                                    // resize grip.
                                    val z = event.calculateZoom()
                                    val target = (currentW * (z / baseZoom))
                                        .coerceIn(currentMinW, currentMaxW)
                                    resizeDelta = target - currentW
                                }
                                event.changes.forEach { it.consume() }
                            } else if (pinching) {
                                // Fingers lifted — commit like the grip.
                                resizing = false
                                val commitW = (currentW + resizeDelta)
                                    .coerceIn(currentMinW, currentMaxW)
                                currentOnResize?.invoke(commitW)
                                resizeDelta = 0f
                                pinching = false
                            }
                            if (event.changes.all { !it.pressed }) break
                        }
                    }
                } else Modifier
            )
            .then(
                if (currentOnMove != null) Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            // Compute the commit from LIVE state inside the
                            // coroutine: pointerInput(Unit) never restarts, so
                            // composition-captured renderX/renderY would be the
                            // FIRST frame's (pre-drag) values and every drop
                            // would snap the card back. dragDelta is snapshot-
                            // state-backed, so reading it here is current.
                            val commitX = (currentX + dragDelta.x)
                                .coerceIn(0f, (currentBoardW - currentW).coerceAtLeast(0f))
                            val commitY = (currentY + dragDelta.y)
                                .coerceIn(0f, (currentBoardH - currentH).coerceAtLeast(0f))
                            currentOnMove?.invoke(commitX, commitY)
                            // CRITICAL: the commit stores the position as the
                            // card's new x/y, so the visual delta must be
                            // cleared — otherwise the card renders at
                            // stored+delta (a double-offset SNAP) on the next
                            // frame and keeps accumulating on later drags.
                            dragDelta = Offset.Zero
                        },
                        onDragCancel = {
                            // No commit happens on cancel — clear the delta so
                            // the card settles back exactly on its stored
                            // position instead of floating mid-offset.
                            dragging = false
                            dragDelta = Offset.Zero
                        },
                        onDrag = { change, amount ->
                            // v57 — while a TWO-FINGER pinch is in flight
                            // (resizing = true), the pinch handler owns the
                            // gesture: skip the drag delta so the card only
                            // widens instead of also sliding with the first
                            // finger (consumption is advisory in pointer
                            // input, so the drag handler still sees these
                            // events). resizing is snapshot-backed, so this
                            // never-restarting coroutine reads its CURRENT
                            // value.
                            if (resizing) return@detectDragGestures
                            change.consume()
                            // Clamp the ACCUMULATED delta so the card sticks
                            // at the edges (clamping only the visual position
                            // would let the delta run away and snap back).
                            val nx = (currentX + dragDelta.x + amount.x)
                                .coerceIn(0f, (currentBoardW - currentW).coerceAtLeast(0f))
                            val ny = (currentY + dragDelta.y + amount.y)
                                .coerceIn(0f, (currentBoardH - currentH).coerceAtLeast(0f))
                            dragDelta = Offset(nx - currentX, ny - currentY)
                        }
                    )
                } else Modifier
            )
            // Press-detection (drag) comes BEFORE click-consumption so a drag
            // never also triggers the edit tap. Saved views pass onEdit = null
            // → no clickable at all, so taps fall through to the board's own
            // handlers (e.g. the expanded dialog's tap-to-dismiss).
            .then(
                if (currentOnEdit != null) Modifier.clickable(onClick = { currentOnEdit?.invoke() })
                else Modifier
            )
    ) {
        NotePaperCard(
            style = style,
            seed = seed,
            paperColor = color,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Quote cards show up to TWO lines on the board — editor and
            // saved views alike. The old editor branch used Int.MAX_VALUE, so
            // a long quote stretched the paper slip to the full board height;
            // the full text stays editable in the edit sheet. The preview
            // never mutates the source text — it only truncates the rendered
            // copy (RichTextEditor enforces the real 280-char / five-line
            // input limit when editing).
            val previewText = limitQuoteContent(text).first
            Text(
                text = if (previewText.isBlank()) "Quote…" else "“$previewText”",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PatrickHandFontFamily),
                color = notePaperInk(color),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        // v42 — resize grip, EDITOR ONLY: a small bottom-end handle that
        // drags the card's width. Commits the width in render px via
        // [currentOnResize] on release; the parent divides by the board
        // scale to store raw board px. The grip's own pointerInput sits
        // AFTER the card's drag/click so a touch on the grip never moves
        // the card (the grip consumes the gesture).
        if (currentOnResize != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.30f))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { resizing = true },
                            onDragEnd = {
                                resizing = false
                                // Commit the LIVE width (render px).
                                val commitW = (currentW + resizeDelta)
                                    .coerceIn(currentMinW, currentMaxW)
                                currentOnResize?.invoke(commitW)
                                resizeDelta = 0f
                            },
                            onDragCancel = {
                                resizing = false
                                resizeDelta = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                // Only the width changes (a horizontal drag).
                                val nw = (currentW + resizeDelta + amount.x)
                                    .coerceIn(currentMinW, currentMaxW)
                                resizeDelta = nw - currentW
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.PhotoSizeSelectLarge,
                    contentDescription = "Resize quote",
                    tint = Color.White,
                    size = 12.dp
                )
            }
        }
    }
}
package com.curio.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Curio side scroll indicator — a thin overlay knob on the right edge of a
 * scrollable list.
 *
 * Design contract:
 *  - Overlay only: lives in the screen's Box, so it NEVER moves on-screen
 *    content (no layout shift).
 *  - Thin when idle (3dp, faint), grows to a 9dp handle when the user
 *    touches it, so it reads quietly until it's needed.
 *  - Dragging the KNOB SPEED-SCROLLS (v26c): the knob's travel maps 1:1 onto
 *    the whole list (drag the knob halfway → the list lands halfway), so a
 *    long list naturally scrolls fast per knob px, and a sustained drag
 *    ramps the rate further on top. Pointer events accumulate into one
 *    per-frame drain loop — a smooth scroll per frame instead of a coroutine
 *    per event. Only the knob itself responds to touch — the empty strip
 *    above/below does nothing, and there is no tap-to-position jump.
 *  - Optional A–Z fast-scroller (v26, when [alphabet] is non-null): TAPPING
 *    the knob toggles a letter rail on the strip's outer edge (v26c — a
 *    dedicated tap check, because drag gestures never report a pure tap);
 *    the active letter highlights as the list scrolls, and tapping a letter
 *    fires [onAlphabetSelect] so the caller can jump to that section.
 *  - The knob color follows the theme's surface ink.
 *  - Hidden entirely when the content fits in the viewport (or when the
 *    state is null).
 *
 * Built on foundation's new [ScrollIndicatorState] (exposed by ScrollState /
 * LazyListState / LazyGridState / PagerState via `.scrollIndicatorState` —
 * NULLABLE in this foundation version). The old VerticalScrollbar API was
 * removed in this foundation version, so the knob is drawn here directly
 * from scrollOffset/contentSize/viewportSize.
 *
 * @param state the scroll state's indicator state (e.g. `listState.scrollIndicatorState`)
 *   — nullable: a null state (no scrollable backing yet) renders nothing.
 * @param onScrollBy called with a pixel delta while the knob is dragged —
 *   wire it to the same state (`{ listState.dispatchRawDelta(it) }`).
 * @param alphabet optional A–Z letters; when non-null, tapping the knob
 *   toggles the fast-scroller rail.
 * @param activeAlphabetIndex index of the letter that matches the row at
 *   the top of the list — highlighted while the rail is open.
 * @param onAlphabetSelect fired when the user taps a letter — the caller
 *   scrolls to the first matching row.
 * @param modifier sizes/aligns the indicator — callers should align it to the
 *   right edge of the list area (`Modifier.align(Alignment.CenterEnd).fillMaxHeight()`)
 *   with padding that clears heroes/pinned bars.
 */
@Composable
fun CurioVerticalScrollIndicator(
    state: ScrollIndicatorState?,
    onScrollBy: suspend (Float) -> Unit = {},
    alphabet: List<String>? = null,
    activeAlphabetIndex: Int? = null,
    onAlphabetSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // The hit strip's measured height in px (from onSizeChanged).
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    // Grows the knob while the user is touching it.
    var touched by remember { mutableStateOf(false) }
    // v26 — A–Z rail open (toggled by tapping the knob when [alphabet] is set).
    var showAlphabet by remember { mutableStateOf(false) }
    // v26c — per-frame scroll accumulator: the gesture handler only adds to
    // [pendingDelta]; a single drain loop below applies it once per frame,
    // so rapid pointer events coalesce into one smooth scroll per frame
    // instead of one coroutine per event (the old lag/jitter).
    var pendingDelta by remember { mutableFloatStateOf(0f) }
    // v26c — the drain loop: every frame, apply whatever the knob has
    // accumulated (capped relative to the list's scrollable range so a flick
    // can't burst), then reset. Frame-paced, so it sleeps between frames.
    val currentOnScrollBy by rememberUpdatedState(onScrollBy)
    LaunchedEffect(state) {
        while (true) {
            withFrameNanos { }
            val s = state ?: continue
            val scrollable = (s.contentSize - s.viewportSize).toFloat()
            if (scrollable <= 0f) continue
            val cap = maxOf(MinFrameDeltaPx, scrollable * FrameDeltaFraction)
            val delta = pendingDelta.coerceIn(-cap, cap)
            if (delta != 0f) {
                pendingDelta = 0f
                currentOnScrollBy(delta)
            }
        }
    }

    val knobWidth by animateDpAsState(if (touched) 9.dp else 3.dp, tween(140), label = "curioIndicatorWidth")
    val knobAlpha by animateFloatAsState(if (touched) 0.80f else 0.30f, tween(140), label = "curioIndicatorAlpha")

    // Knob geometry in px: height, offset from the strip's top, and the
    // draggable travel (strip minus knob). Recomputes as the list scrolls.
    // Keyed on the state so a null → non-null transition re-creates the block.
    val geometry: State<ScrollKnob> = remember(state) {
        derivedStateOf {
            val s = state ?: return@derivedStateOf ScrollKnob(0f, 0f, 0f)
            val content = s.contentSize.toFloat()
            val viewport = s.viewportSize.toFloat()
            val bar = barHeightPx
            if (content <= viewport || bar <= 0f) {
                ScrollKnob(0f, 0f, 0f)
            } else {
                val minThumb = 32f * density.density
                val thumb = maxOf(minThumb, bar * viewport / content)
                val travel = bar - thumb
                val scrollable = content - viewport
                val fraction = (s.scrollOffset.toFloat() / scrollable).coerceIn(0f, 1f)
                ScrollKnob(thumb, fraction * travel, travel)
            }
        }
    }

    val knobColor = MaterialTheme.colorScheme.onSurface.copy(alpha = knobAlpha)

    // The knob's touch-target geometry — its exact vertical extent plus a
    // small pad, so grabbing the knob is easy but the empty strip above and
    // below it never responds (no accidental jumps).
    val g = geometry.value
    val hitHeightPx = if (g.thumbPx > 0f) g.thumbPx + KnobTouchPadPx else 0f
    val hitOffsetPx = if (g.thumbPx > 0f) g.offsetPx - KnobTouchPadPx / 2f else 0f
    // v26 — the strip widens to make room for the letter rail on its outer
    // edge; the knob strip stays a fixed 28dp so the knob never resizes.
    val railOpen = alphabet != null && showAlphabet && g.thumbPx > 0f
    val stripWidth by animateDpAsState(
        if (railOpen) 54.dp else 28.dp,
        tween(140),
        label = "curioStripWidth"
    )

    Box(
        modifier = modifier
            // Fixed-width hit strip — the caller supplies the height (and any
            // hero-clearance padding); 28dp gives the knob a grabbable target
            // without stealing much screen real estate (54dp when the A–Z
            // rail is open).
            .width(stripWidth)
            .onSizeChanged { barHeightPx = it.height.toFloat() }
    ) {
        // ── Knob + touch target — pinned to the strip's LEFT 28dp (so the
        //    rail can open on the outer edge without moving the knob strip),
        //    the knob hugging the strip's right edge as before. ──
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(28.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            // ── The knob — aligned to the strip's top-right, then nudged
            //    down by the computed offset so it tracks the list position
            //    exactly. ──
            Box(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .width(knobWidth)
                    .height(with(density) { g.thumbPx.toDp() })
                    .offset(y = with(density) { g.offsetPx.toDp() })
                    .clip(RoundedCornerShape(50))
                    .background(knobColor)
            )

            // ── The knob's touch target — invisible, sized/placed to overlap
            //    the knob, and the ONLY touchable part of the strip. Dragging
            //    SPEED-SCROLLS (v26: cumulative travel ramps the rate); a tap
            //    (no real movement) toggles the A–Z rail when one is offered.
            //    The knob grows while a drag is active and shrinks on release. ──
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(with(density) { hitHeightPx.toDp() })
                    .offset(y = with(density) { hitOffsetPx.toDp() })
                    .pointerInput(state, hitHeightPx > 0f, alphabet != null) {
                        // No knob (content fits / no state yet) → nothing to grab.
                        if (hitHeightPx <= 0f) return@pointerInput
                        // v26c — manual tap-vs-drag: `detectVerticalDragGestures`
                        // never reports a pure tap (its onDragEnd only fires
                        // after touch slop), which is why the A–Z rail could
                        // never open. Track the travel ourselves — a pointer
                        // that goes up before slop is a TAP.
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var gestureCumulative = 0f
                            // Add one pointer delta to the per-frame
                            // accumulator, scaled by the knob's 1:1 travel map
                            // (long list → faster per knob px) and the ramp.
                            fun accumulate(amount: Float) {
                                if (amount == 0f) return
                                val s = state
                                val scrollable = s?.let {
                                    (it.contentSize - it.viewportSize).toFloat()
                                } ?: 0f
                                val travel = geometry.value.maxOffsetPx
                                if (scrollable <= 0f || travel <= 0f) return
                                gestureCumulative += amount
                                val ratio = (scrollable / travel).coerceAtLeast(1f)
                                val ramp = 1f + (abs(gestureCumulative) / SpeedRampPx)
                                    .coerceAtMost(SpeedRampBoost)
                                pendingDelta += amount * ratio * ramp
                            }
                            val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                                change.consume()
                                accumulate(over)
                            }
                            if (drag != null) {
                                touched = true
                                drag(down.id) { change ->
                                    change.consume()
                                    accumulate(change.position.y - change.previousPosition.y)
                                }
                                touched = false
                            } else if (alphabet != null) {
                                // Pure tap on the knob → toggle the A–Z rail.
                                showAlphabet = !showAlphabet
                            }
                        }
                    }
            )
        }

        // ── A–Z fast-scroller rail — the outer 26dp, spread edge-to-edge,
        //    with the active letter highlighted. Tap a letter to jump. ──
        if (railOpen && alphabet != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(26.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                alphabet.forEachIndexed { index, letter ->
                    val active = activeAlphabetIndex == index
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = if (active) 12.sp else 10.sp
                        ),
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                else Color.Transparent
                            )
                            .clickable { onAlphabetSelect(letter) }
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/** Cumulative drag (px) that reaches the FULL speed ramp — after this much
 *  travel the rate multiplies by 1 + [SpeedRampBoost]. */
private const val SpeedRampPx = 160f

/** How many extra base-ratios the ramp adds at full travel (max speed = 1+boost). */
private const val SpeedRampBoost = 2f

/** Per-frame scroll cap floor (px) — the smallest jump a frame may make. */
private const val MinFrameDeltaPx = 400f

/** The per-frame cap also scales with list length: up to this fraction of
 *  the scrollable range per frame keeps long lists fast without bursts. */
private const val FrameDeltaFraction = 0.08f

/** Extra vertical padding around the knob's touch target (px). */
private const val KnobTouchPadPx = 14f

/** Knob geometry in px — height, offset from the strip top, and the knob's
 *  draggable travel (currently reserved: the drag uses a fixed slow ratio, so
 *  travel is not read by the gesture code anymore). */
private data class ScrollKnob(
    val thumbPx: Float,
    val offsetPx: Float,
    val maxOffsetPx: Float,
)

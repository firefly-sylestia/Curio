package com.curio.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Curio side scroll indicator — a thin overlay knob on the right edge of a
 * scrollable list.
 *
 * Design contract:
 *  - Overlay only: lives in the screen's Box, so it NEVER moves on-screen
 *    content (no layout shift).
 *  - Thin when idle (3dp, faint), grows to a 9dp handle when the user
 *    touches it, so it reads quietly until it's needed.
 *  - Dragging the knob scrolls the list (maps drag distance to scroll
 *    distance); the knob color follows the theme's surface ink.
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
 * @param modifier sizes/aligns the indicator — callers should align it to the
 *   right edge of the list area (`Modifier.align(Alignment.CenterEnd).fillMaxHeight()`)
 *   with padding that clears heroes/pinned bars.
 */
@Composable
fun CurioVerticalScrollIndicator(
    state: ScrollIndicatorState?,
    onScrollBy: suspend (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // The hit strip's measured height in px (from onSizeChanged).
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    // Grows the knob while the user is touching it.
    var touched by remember { mutableStateOf(false) }

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

    Box(
        modifier = modifier
            // Fixed-width hit strip — the caller supplies the height (and any
            // hero-clearance padding); 28dp gives a grabbable target without
            // stealing much screen real estate.
            .width(28.dp)
            .onSizeChanged { barHeightPx = it.height.toFloat() }
            .pointerInput(state) {
                // Drag-to-scroll via the stable drag-gesture detector: it
                // hands us the pixel delta directly, so we never touch
                // per-event position math (which changed shape across
                // compose versions). The knob grows while a drag is active
                // and shrinks back on release.
                detectVerticalDragGestures(
                    onDragStart = { touched = true },
                    onDragEnd = { touched = false },
                    onDragCancel = { touched = false },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val s = state
                        if (s != null && dragAmount != 0f) {
                            val g = geometry.value
                            val scrollable = (s.contentSize - s.viewportSize).toFloat()
                            if (g.maxOffsetPx > 1f && scrollable > 0f) {
                                // Same ratio as the thumb: drag distance over
                                // knob travel times the total scrollable range.
                                val delta = dragAmount / g.maxOffsetPx * scrollable
                                scope.launch { onScrollBy(delta) }
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopEnd
    ) {
        // The knob — aligned to the strip's top-right, then nudged down by the
        // computed offset so it tracks the list position exactly.
        Box(
            modifier = Modifier
                .padding(end = 2.dp)
                .width(knobWidth)
                .height(with(density) { geometry.value.thumbPx.toDp() })
                .offset(y = with(density) { geometry.value.offsetPx.toDp() })
                .clip(RoundedCornerShape(50))
                .background(knobColor)
        )
    }
}

/** Knob geometry in px — height, offset from the strip top, draggable travel. */
private data class ScrollKnob(
    val thumbPx: Float,
    val offsetPx: Float,
    val maxOffsetPx: Float,
)

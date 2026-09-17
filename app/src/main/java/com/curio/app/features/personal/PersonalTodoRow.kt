package com.curio.app.features.personal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * v389 — A TO-DO ROW'S OWN GESTURES: pick it up, carry it, or swipe it away.
 *
 * A list is not a page of prose — it is a running ORDER the member keeps
 * changing (the thing you finally do today belongs at the top; the thing you
 * will never do should leave). So a to-do row is the one block in the whole
 * writing family that answers to two gestures of its own:
 *
 *  · LONG PRESS, then drag: the row lifts and the list makes room as the finger
 *    crosses each slot, and the drop COMMITS the new order (the list's order IS
 *    the stored document, so nothing has to be re-ranked later).
 *  · SWIPE SIDEWAYS: the row follows the finger and, past a third of the width,
 *    leaves the list — held by the editor so [PersonalUndoPill] can put it back
 *    exactly where it stood.
 *
 * Both follow the app's own gesture rule (see the DM thread's swipe-to-answer):
 * the HORIZONTAL detector is horizontal-only, so a vertical drag still falls
 * through to the page's scroll, and the long-press detector never sees a plain
 * drag at all.
 *
 * PERFORMANCE: the finger's pixel travel is read ONLY inside `graphicsLayer`, so
 * following a finger never recomposes a row — only crossing a slot boundary
 * does, and that is a plain Int.
 */
internal class PersonalRowDragState {

    /** The row being carried, or null when nothing is picked up. */
    var draggedId by mutableStateOf<String?>(null)
        private set

    /** The slot the carried row STARTED in — it stays there while the finger
     *  travels, so the rows it passes can slide out of the way. */
    var fromIndex by mutableIntStateOf(-1)
        private set

    /** How many slots the finger has carried it. A whole slot is rare, so this
     *  (and not the pixel travel) is what the list recomposes on. */
    var steps by mutableIntStateOf(0)
        private set

    /**
     * The live pixel travel. It IS backed by Compose state, but it is read in
     * exactly one place — the carried row's `graphicsLayer` — and a deferred
     * read there reaches only that layer: the row follows the finger at frame
     * rate without recomposing a single text field behind it.
     */
    private val travel = mutableFloatStateOf(0f)
    val travelY: Float get() = travel.floatValue

    /** One slot's height in pixels — the row's own height plus the list's gap. */
    var stride: Float = 0f
        private set

    val isDragging: Boolean get() = draggedId != null

    fun begin(id: String, index: Int, slotStride: Float) {
        draggedId = id
        fromIndex = index
        steps = 0
        travel.floatValue = 0f
        stride = slotStride.coerceAtLeast(1f)
    }

    fun dragBy(amountY: Float, lastIndex: Int) {
        travel.floatValue += amountY
        steps = (travel.floatValue / stride).roundToInt().let { raw ->
            (fromIndex + raw).coerceIn(0, lastIndex) - fromIndex
        }
    }

    fun targetIndex(lastIndex: Int): Int = (fromIndex + steps).coerceIn(0, lastIndex)

    /**
     * How far the row at [index] slides while another row is carried past it:
     * exactly one slot, in the direction the carried row is travelling. Zero for
     * every row outside the travelled range and for the carried row itself.
     */
    fun shiftFor(index: Int, lastIndex: Int): Float {
        if (!isDragging || index == fromIndex) return 0f
        val target = targetIndex(lastIndex)
        return when {
            fromIndex < target && index in (fromIndex + 1)..target -> -stride
            fromIndex > target && index in target until fromIndex -> stride
            else -> 0f
        }
    }

    fun reset() {
        draggedId = null
        fromIndex = -1
        steps = 0
        travel.floatValue = 0f
        stride = 0f
    }
}

/** How far a row must travel sideways before it leaves the list. */
private const val SWIPE_AWAY_FRACTION = 0.34f

/**
 * ONE TO-DO ROW, wrapped in the gestures above. The row itself is untouched —
 * this is a shell around whichever block the page drew, so the writing (and its
 * `key(id)` identity) is exactly what it is on every other page.
 */
@Composable
internal fun PersonalTodoRow(
    id: String,
    index: Int,
    state: PersonalEditorState,
    drag: PersonalRowDragState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val gapPx = with(LocalDensity.current) { 6.dp.toPx() }
    val scope = rememberCoroutineScope()

    var rowHeight by remember(id) { mutableFloatStateOf(0f) }
    var rowWidth by remember(id) { mutableFloatStateOf(0f) }

    // The swipe, in two pieces: while the finger is down the raw travel is what
    // the layer draws (no recomposition); on release the small Animatable takes
    // over for one spring-back, so a swipe that did not commit glides home
    // instead of snapping.
    val swipeRaw = remember(id) { mutableFloatStateOf(0f) }
    val settle = remember(id) { Animatable(0f) }
    var settling by remember(id) { mutableStateOf(false) }

    val isDragged = drag.draggedId == id
    val lastIndex = state.blockIds.lastIndex
    val shift by animateFloatAsState(
        targetValue = if (isDragged) 0f else drag.shiftFor(index, lastIndex),
        animationSpec = if (!drag.isDragging || isDragged) snap()
        else spring(dampingRatio = 0.82f, stiffness = 700f),
        label = "todoRowShift"
    )

    // The lifted look: the carried row reads as a card above the list (shadow
    // BEFORE the fill, per the project's shadow-order rule, and an OPAQUE fill,
    // because a translucent one lets the shadow bleed through the row).
    val lifted = if (isDragged) {
        Modifier
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurioDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHighest
                else Color(0xFFF7F1E6)
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged {
                if (it.height > 0) rowHeight = it.height.toFloat()
                if (it.width > 0) rowWidth = it.width.toFloat()
            }
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                translationY = shift + (if (drag.draggedId == id) drag.travelY else 0f)
                translationX = if (settling) settle.value else swipeRaw.floatValue
                // A row on its way out fades as it goes, so the swipe says
                // "leaving" before it leaves.
                val width = rowWidth.coerceAtLeast(1f)
                alpha = if (settling) 1f
                else (1f - abs(swipeRaw.floatValue) / width * 0.45f).coerceIn(0.45f, 1f)
                if (drag.draggedId == id) {
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            }
            .then(lifted)
            .then(
                if (!enabled) Modifier
                else Modifier.pointerInput(id, enabled) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            if (rowHeight > 0f) drag.begin(id, index, rowHeight + gapPx)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            drag.dragBy(amount.y, state.blockIds.lastIndex)
                        },
                        onDragEnd = {
                            val from = drag.fromIndex
                            val to = drag.targetIndex(state.blockIds.lastIndex)
                            if (from in 0..state.blockIds.lastIndex && from != to) {
                                state.moveBlock(from, to)
                            }
                            drag.reset()
                        },
                        onDragCancel = { drag.reset() }
                    )
                }
            )
            .then(
                if (!enabled) Modifier
                else Modifier.pointerInput(id, enabled) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeRaw.floatValue = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            swipeRaw.floatValue += amount
                        },
                        onDragEnd = {
                            val gone = rowWidth > 0f &&
                                abs(swipeRaw.floatValue) > rowWidth * SWIPE_AWAY_FRACTION
                            val from = swipeRaw.floatValue
                            if (gone) {
                                swipeRaw.floatValue = 0f
                                state.removeRow(id)
                            } else if (from != 0f) {
                                swipeRaw.floatValue = 0f
                                settling = true
                                scope.launch {
                                    settle.snapTo(from)
                                    settle.animateTo(0f, tween(190))
                                    settling = false
                                }
                            }
                        },
                        onDragCancel = { swipeRaw.floatValue = 0f }
                    )
                }
            )
    ) {
        content()
    }
}

/**
 * v389 — THE UNDO PILL: "row removed · Undo", floating over the page for a few
 * seconds.
 *
 * A swipe is the one destructive gesture in the writing family, so it gets the
 * one thing that makes a destructive gesture safe: a way back that costs one
 * tap, shows exactly what it will restore, and goes away by itself so it can
 * never become clutter (the page's own auto-dismiss timer lives in
 * [PersonalWritingPage]).
 */
@Composable
internal fun PersonalUndoPill(
    label: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = personalAccentInk(),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surface
            )
            Surface(
                onClick = onUndo,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CurioIcon(
                        CurioIcons.Undo,
                        null,
                        tint = MaterialTheme.colorScheme.surface,
                        size = 15.dp
                    )
                    Text(
                        "Undo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                modifier = Modifier.size(28.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.Close,
                        "Dismiss",
                        tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        size = 15.dp
                    )
                }
            }
        }
    }
}

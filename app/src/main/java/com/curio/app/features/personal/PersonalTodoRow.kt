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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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

    /**
     * v389 — EVERY ROW'S OWN HEIGHT, by row id.
     *
     * A to-do row WRAPS: a two-line task is twice the height of a one-line one,
     * so a list's slots are not equal and ONE uniform `stride` for the whole list
     * is what made a reorder of uneven rows land a place early or late — the
     * finger had travelled past a tall row without the list noticing, or the list
     * stepped before the finger had really cleared a short one (user report: "the
     * todo rearrange works but also sometimes buggy"). Written from each row's own
     * layout pass, read only by the gesture.
     */
    private val rowHeights = mutableMapOf<String, Float>()

    fun measure(id: String, height: Float) {
        if (height > 0f) rowHeights[id] = height
    }

    /** [id]'s measured height, or the carried row's own slot before it is known. */
    private fun heightOf(id: String): Float = rowHeights[id] ?: stride

    val isDragging: Boolean get() = draggedId != null

    fun begin(id: String, index: Int, slotStride: Float) {
        draggedId = id
        fromIndex = index
        steps = 0
        travel.floatValue = 0f
        stride = slotStride.coerceAtLeast(1f)
    }

    /**
     * The finger moved [amountY] with [ids] as the list in its current order.
     *
     * Each step is charged the height of the row being PASSED — never less than
     * the carried row's own slot — so crossing a wrapped, two-line task costs
     * two lines' travel and the list steps exactly when the eye says it should.
     * With equal rows this is the plain "half a slot per step" the list always
     * had; with uneven ones it is the whole reason it now lands where the finger
     * put it.
     */
    fun dragBy(amountY: Float, ids: List<String>, lastIndex: Int) {
        travel.floatValue += amountY
        val travel = travel.floatValue
        val down = travel > 0f
        var index = fromIndex
        var spent = 0f
        while ((down && index < lastIndex) || (!down && travel < 0f && index > 0)) {
            val neighbour = ids.getOrElse(if (down) index + 1 else index - 1) { "" }
            val cost = heightOf(neighbour).coerceAtLeast(stride)
            val remaining = if (down) travel - spent else -travel - spent
            if (remaining < cost / 2f) break
            spent += cost
            index += if (down) 1 else -1
        }
        steps = index - fromIndex
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
 * v389 — IS A BLOCK BEING CARRIED?
 *
 * A carried VOICE NOTE is a waveform strip whose own gesture seeks (see
 * [PersonalVoiceBar]), and two detectors on one finger is the bug the to-do rows
 * already had once. The strip consults this and stands down while the block is
 * being carried — provided by [PersonalMovableBlock] for its own content, so no
 * page has to thread the drag state into a voice note.
 */
internal val LocalPersonalBlockCarried = staticCompositionLocalOf { false }

/**
 * v389 — PICK A BLOCK UP AND CARRY IT (the to-do row's gesture, without the
 * swipe).
 *
 * The to-do list can be re-ordered; everything else on a page was fixed where it
 * was written — which is wrong for a VOICE NOTE, because a note is a thing about
the thought it sits under, and the place it arrived is just where the recording
 * happened (user request: "add drag to move the voice note too, in journal page,
 * also add in book review chapter review too"). So the same pick-up, the same
 * measured slots (see [PersonalRowDragState.dragBy]) and the same commit — the
 * page's order IS the stored document — wrapped around whichever block the page
 * drew, exactly like [PersonalTodoRow] wraps a row.
 *
 * The caller hands in the SAME `drag` the page hoisted, so a page that is both a
 * list and a page with a voice in it still has one gesture at a time.
 */
@Composable
internal fun PersonalMovableBlock(
    id: String,
    index: Int,
    state: PersonalEditorState,
    drag: PersonalRowDragState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val gapPx = with(LocalDensity.current) { 6.dp.toPx() }
    var blockHeight by remember(id) { mutableFloatStateOf(0f) }

    val isDragged = drag.draggedId == id
    val lastIndex = state.blockIds.lastIndex
    // The rows it passes slide out of the way; the commit at the drop is what
    // makes that shift permanent, so it SNAPS when the gesture ends.
    val shift by animateFloatAsState(
        targetValue = if (isDragged) 0f else drag.shiftFor(index, lastIndex),
        animationSpec = if (!drag.isDragging || isDragged) snap()
        else spring(dampingRatio = 0.82f, stiffness = 700f),
        label = "movableBlockShift"
    )

    // The lifted look, the project's own rule: shadow BEFORE the fill, and an
    // OPAQUE fill (a translucent one lets the shadow bleed through).
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
                if (it.height > 0) {
                    blockHeight = it.height.toFloat()
                    drag.measure(id, it.height.toFloat())
                }
            }
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                translationY = shift + (if (drag.draggedId == id) drag.travelY else 0f)
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
                            if (blockHeight > 0f) drag.begin(id, index, blockHeight + gapPx)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            drag.dragBy(amount.y, state.blockIds, state.blockIds.lastIndex)
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
    ) {
        // What is inside knows it is being carried, so a gesture of its own
        // (the waveform's seek) can stand down for the ride.
        CompositionLocalProvider(LocalPersonalBlockCarried provides isDragged) {
            content()
        }
    }
}

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
                // The list needs to know how TALL each of its rows is, not just
                // this one — a reorder is measured against the rows it passes
                // (see PersonalRowDragState.dragBy).
                drag.measure(id, it.height.toFloat())
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
                            drag.dragBy(amount.y, state.blockIds, state.blockIds.lastIndex)
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
                        // v389 — A ROW BEING CARRIED UP OR DOWN IS NOT BEING
                        // SWIPED. Both detectors sat on the same row, so one
                        // gesture could satisfy both: a reorder with a sideways
                        // wobble in it also dragged the row off the list, and a
                        // swipe could nudge the order on its way out. The
                        // long-press drag claims the gesture, and this one stands
                        // down for as long as it holds it (user report: "the
                        // todo rearrange works but also sometimes buggy").
                        onDragStart = { if (!drag.isDragging) swipeRaw.floatValue = 0f },
                        onHorizontalDrag = { change, amount ->
                            if (!drag.isDragging) {
                                change.consume()
                                swipeRaw.floatValue += amount
                            }
                        },
                        onDragEnd = {
                            if (drag.isDragging) {
                                swipeRaw.floatValue = 0f
                            } else {
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

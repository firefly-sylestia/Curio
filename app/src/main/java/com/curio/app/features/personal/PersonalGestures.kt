package com.curio.app.features.personal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalAlign
import com.curio.app.data.PersonalMarker
import kotlin.math.abs

/**
 * v413 — THE JOURNAL'S HIDDEN GESTURES.
 *
 * Ten writing tools with no button anywhere: each one is a GESTURE you make on
 * the page, and each is a switch of its own on the Dev page so the member can
 * meet them one at a time and read what it does before switching it on
 * (member: "lets add some gesture double tap etc function extra tools hidden
 * one for journal editing. 10 differnt action add them as experiment options in
 * journal with each explained so i would know, add the option in dev
 * experiment").
 *
 * WHY THERE IS NO BUTTON: the tool dock is already a row of eleven, and every
 * one of these is something a writer does WHILE the hand is on the page — you
 * do not leave the sentence you are writing to go and press something. A
 * gesture is the tool you cannot lose behind a scroll.
 *
 * HOW THEY ARE READ: [Modifier.journalGestures] is ONE recogniser for all of
 * them. It counts the fingers that land, follows their centre, and then decides
 * — a short still press is a TAP, a long travel is a SWIPE in the axis it
 * travelled, and two of the former inside the double-tap window is a
 * DOUBLE-TAP. Single-finger gestures are NOT taken here: a finger on a text
 * field belongs to the caret, to selection and to the writing column's own
 * scroll, so the two single-finger tools ride the page's BLANK space instead
 * (see [JournalGesture.BLANK_DOUBLE_TAP], wired through `combinedClickable` in
 * PersonalWritingPage). Everything here needs TWO fingers or more, which is
 * exactly the input a text field has no use for.
 */
internal enum class JournalGesture {
    TWO_FINGER_TAP,
    TWO_FINGER_DOUBLE_TAP,
    TWO_FINGER_SWIPE_UP,
    TWO_FINGER_SWIPE_DOWN,
    TWO_FINGER_SWIPE_LEFT,
    TWO_FINGER_SWIPE_RIGHT,
    THREE_FINGER_SWIPE_UP,
    THREE_FINGER_SWIPE_DOWN,
    /** Single-finger: the page's BLANK space only (see [JournalGestureTool.DATE_LINE]). */
    BLANK_DOUBLE_TAP,
    /** Single-finger: the page's BLANK space only (see [JournalGestureTool.CYCLE_FACE]). */
    BLANK_LONG_PRESS
}

/**
 * One switchable hidden tool: the gesture, the action it runs, and the sentence
 * that explains it on the Dev page. [bit] is the bit this tool owns in
 * [AppPreferences.journalGestureToolsState] — one int for all ten, so the Dev
 * page can grow a row without a new preference each time.
 */
internal enum class JournalGestureTool(
    val bit: Int,
    val label: String,
    /** What you DO — the gesture, said the way a hand would do it. */
    val how: String,
    /** What it DOES — the action, and when it is useful. */
    val what: String,
    val gesture: JournalGesture
) {
    RESTORE_ROW(
        bit = 1,
        label = "Bring back what you removed",
        how = "Two fingers, tap",
        what = "Puts back the row you swiped away — the same undo the floating pill offers, but without leaving the sentence you are writing. Does nothing when nothing was removed.",
        gesture = JournalGesture.TWO_FINGER_TAP
    ),
    CYCLE_ALIGN(
        bit = 1 shl 1,
        label = "Alignment without the menu",
        how = "Two fingers, double-tap",
        // Honest about the reach: the app's alignment is the PAGE's, not the
        // line's (all four dock buttons call the same `setAlign`), so this is
        // the dock's menu on a gesture — not a per-line nudge.
        what = "Moves the page through left, centre, right and justified, one double-tap at a time. Alignment in this app is the whole page's, so this is the dock's alignment menu without opening it.",
        gesture = JournalGesture.TWO_FINGER_DOUBLE_TAP
    ),
    CARRY_LINE_UP(
        bit = 1 shl 2,
        label = "Lift the line up",
        how = "Two fingers, swipe up",
        what = "Walks the line you are writing one place up the page. For the paragraph that belongs above the one before it.",
        gesture = JournalGesture.TWO_FINGER_SWIPE_UP
    ),
    CARRY_LINE_DOWN(
        bit = 1 shl 3,
        label = "Send the line down",
        how = "Two fingers, swipe down",
        what = "Walks the line you are writing one place down the page — the pair to swiping up. The caret follows the line.",
        gesture = JournalGesture.TWO_FINGER_SWIPE_DOWN
    ),
    CYCLE_LIST(
        bit = 1 shl 4,
        label = "List mark on the line",
        how = "Two fingers, swipe left",
        what = "Gives the line a bullet, then a checkbox, then a bullet again — the dock's list tool without visiting the dock.",
        gesture = JournalGesture.TWO_FINGER_SWIPE_LEFT
    ),
    PAGE_SELECT(
        bit = 1 shl 5,
        label = "Take the whole page",
        how = "Two fingers, swipe right",
        what = "Selects everything on the page (and a second swipe lets it go), so a tool then applies to the whole entry — all of it bold, all of it quoted.",
        gesture = JournalGesture.TWO_FINGER_SWIPE_RIGHT
    ),
    SAVE_NOW(
        bit = 1 shl 6,
        label = "Save on the spot",
        how = "Three fingers, swipe down",
        what = "Writes the page this second instead of waiting for the pause. For the moment you are about to close the app, or leave a page for hours.",
        gesture = JournalGesture.THREE_FINGER_SWIPE_DOWN
    ),
    CYCLE_MARKER(
        bit = 1 shl 7,
        label = "Cycle the line's mark",
        how = "Three fingers, swipe up",
        what = "Moves the caret's line onto the next mark — dot, ring, dash, star, spark, crystal, arrow, leaf, heart, bolt — so a list of thoughts is sorted by shape without the marker menu. A mark dresses a list row, so the line takes a bullet with it.",
        gesture = JournalGesture.THREE_FINGER_SWIPE_UP
    ),
    DATE_LINE(
        bit = 1 shl 8,
        label = "A dated line under the writing",
        how = "Double-tap the blank part of the page",
        what = "Drops today's date in as a heading under what you have written and leaves the caret in it — the way a diary page is divided, in one tap.",
        gesture = JournalGesture.BLANK_DOUBLE_TAP
    ),
    CYCLE_FACE(
        bit = 1 shl 9,
        label = "Change the line's face",
        how = "Press and hold the blank part of the page",
        what = "Moves the line the caret is in through the app's four writing faces — the page's own hand, sans, mono and display — so a quotation or a title can be set apart without the font menu.",
        gesture = JournalGesture.BLANK_LONG_PRESS
    );

    /** The switch's second line on the Dev page: the gesture, then the action. */
    val explanation: String get() = "$how — $what"

    companion object {
        /** The bit an untouched preference holds: no tool switched on. */
        const val NONE = 0

        /** Every bit a tool can own — what "switch everything off" clears. */
        val ALL_BITS: Int get() = entries.fold(NONE) { acc, tool -> acc or tool.bit }
    }
}

/**
 * The gestures switched on right now, read from the ONE bitmask preference, so
 * a page never has to ask ten questions (and a new tool never means a new
 * read). The preference is a Compose state, so switching a tool on the Dev page
 * is felt on the next page you open without a restart.
 */
internal fun enabledJournalGestures(): Set<JournalGesture> {
    val mask = AppPreferences.journalGestureToolsState
    if (mask == JournalGestureTool.NONE) return emptySet()
    return JournalGestureTool.entries
        .filter { mask and it.bit != 0 }
        .mapTo(mutableSetOf()) { it.gesture }
}

// ════════════════════════════════════════════════════════════════════════
// THE RECOGNISER
// ════════════════════════════════════════════════════════════════════════

/** How far the centre must travel before a press counts as a swipe. */
private const val GestureSwipeMinPx = 56f

/** How long a press may last and still count as a tap. */
private const val GestureTapMaxMs = 260L

/** The window two taps must fall inside to be a double-tap. */
private const val GestureDoubleTapMs = 320L

/** How far a "still" press may drift and still count as a tap. */
private const val GestureTapSlopPx = 26f

/**
 * v413 — READ THE JOURNAL'S MULTI-FINGER GESTURES.
 *
 * One pointer handler for every gesture in [enabled], written as a single
 * gesture loop rather than a stack of detectors: the finger count and the
 * journey of the centre are the ONLY facts needed, and a bespoke loop can say
 * "two fingers went up" without any of the detectors fighting each other over
 * the same events (the class of bug that made the reading-progress steppers
 * miss every second tap — see `TileStepButton`).
 *
 * The events are read in the [PointerEventPass.Initial] pass and consumed once
 * two or more fingers are down: from that moment the gesture is the page's, so
 * the text field underneath cannot also take the caret for a walk. A
 * single-finger press is left completely alone — it is never even looked at —
 * so scrolling, the caret, selection and typing behave exactly as before.
 */
internal fun Modifier.journalGestures(
    enabled: Set<JournalGesture>,
    onGesture: (JournalGesture) -> Unit
): Modifier {
    if (enabled.isEmpty()) return this
    return this.pointerInput(enabled) {
        // Remembered across gestures: the second tap of a double-tap has to
        // know where and when the first one was.
        var lastTapAt = 0L
        var lastTapPosition = Offset.Zero
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val start = first.position
            var centre = start
            var peakFingers = 1
            var travel = 0f
            var startedAt = first.uptimeMillis
            var endedAt = startedAt
            var ours = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val down = event.changes.filter { it.pressed }
                if (down.size > peakFingers) peakFingers = down.size
                if (down.isNotEmpty()) {
                    val next = Offset(
                        x = down.fold(0f) { acc, change -> acc + change.position.x } / down.size,
                        y = down.fold(0f) { acc, change -> acc + change.position.y } / down.size
                    )
                    travel += (next - centre).getDistance()
                    centre = next
                    // From the second finger on, this is ours: hold the events
                    // so the field underneath cannot act on the same press.
                    if (down.size >= 2) {
                        ours = true
                        event.changes.forEach { it.consume() }
                    }
                } else {
                    endedAt = event.changes.firstOrNull()?.uptimeMillis ?: endedAt
                    break
                }
            }
            if (!ours) return@awaitEachGesture
            // STILL is the whole journey of the centre (so a press that wandered
            // and came back is not a tap); FAR is where it ENDED (so a swipe is
            // the direction it left in). Neither is true for a slow, drifty
            // press — which is not a tool, and does nothing.
            val held = endedAt - startedAt
            val net = (centre - start).getDistance()
            val still = travel <= GestureTapSlopPx && held <= GestureTapMaxMs
            val far = net >= GestureSwipeMinPx
            if (still && peakFingers == 2 && JournalGesture.TWO_FINGER_DOUBLE_TAP in enabled &&
                endedAt - lastTapAt <= GestureDoubleTapMs &&
                (start - lastTapPosition).getDistance() <= GestureTapSlopPx
            ) {
                lastTapAt = 0L
                onGesture(JournalGesture.TWO_FINGER_DOUBLE_TAP)
                return@awaitEachGesture
            }
            if (still) {
                if (peakFingers == 2) {
                    lastTapAt = endedAt
                    lastTapPosition = start
                }
                val tap = if (peakFingers >= 3) null else JournalGesture.TWO_FINGER_TAP
                // A three-finger tap is nobody's tool yet: it is swallowed so a
                // stray third finger cannot fall back to the two-finger one.
                if (tap != null && tap in enabled) onGesture(tap)
                return@awaitEachGesture
            }
            val horizontal = abs(centre.x - start.x) >= abs(centre.y - start.y)
            val gesture = when {
                !far -> null
                peakFingers >= 3 && !horizontal && centre.y < start.y ->
                    JournalGesture.THREE_FINGER_SWIPE_UP
                peakFingers >= 3 && !horizontal && centre.y > start.y ->
                    JournalGesture.THREE_FINGER_SWIPE_DOWN
                peakFingers == 2 && !horizontal && centre.y < start.y ->
                    JournalGesture.TWO_FINGER_SWIPE_UP
                peakFingers == 2 && !horizontal && centre.y > start.y ->
                    JournalGesture.TWO_FINGER_SWIPE_DOWN
                peakFingers == 2 && horizontal && centre.x < start.x ->
                    JournalGesture.TWO_FINGER_SWIPE_LEFT
                peakFingers == 2 && horizontal && centre.x > start.x ->
                    JournalGesture.TWO_FINGER_SWIPE_RIGHT
                else -> null
            }
            if (gesture != null && gesture in enabled) onGesture(gesture)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// THE ACTIONS
// ════════════════════════════════════════════════════════════════════════

/**
 * Runs a recognised gesture against the page. Every action is written on the
 * editor's OWN public API — the same calls the dock makes — so a gesture can
 * never do something the buttons cannot, and the page's auto-save sees it as
 * ordinary writing.
 *
 * [onSaveNow] is the one action the editor cannot do for itself: saving is the
 * page's business (it owns the row and the debounce), so the save gesture is
 * handed back up to the caller.
 */
internal fun PersonalEditorState.runJournalGesture(
    gesture: JournalGesture,
    today: String,
    onSaveNow: () -> Unit = {}
) {
    when (gesture) {
        JournalGesture.TWO_FINGER_TAP -> restoreRemovedRow()
        JournalGesture.TWO_FINGER_DOUBLE_TAP -> cycleLineAlign()
        JournalGesture.TWO_FINGER_SWIPE_UP -> walkLine(-1)
        JournalGesture.TWO_FINGER_SWIPE_DOWN -> walkLine(1)
        JournalGesture.TWO_FINGER_SWIPE_LEFT -> cycleListStyle()
        JournalGesture.TWO_FINGER_SWIPE_RIGHT ->
            if (pageSelected) clearPageSelection() else selectPage()
        JournalGesture.THREE_FINGER_SWIPE_DOWN -> onSaveNow()
        JournalGesture.THREE_FINGER_SWIPE_UP -> cycleLineMarker()
        JournalGesture.BLANK_DOUBLE_TAP -> insertTitleLine(today)
        JournalGesture.BLANK_LONG_PRESS -> cycleLineFace()
    }
}

/** The line the caret is in, as a position on the page, or null. */
private fun PersonalEditorState.focusedIndex(): Int {
    val id = focusedId ?: return -1
    return blockIds.indexOf(id)
}

/** Left → centre → right → justified, one gesture at a time. */
private fun PersonalEditorState.cycleLineAlign() {
    val id = focusedId ?: return
    val next = when (align(id)) {
        PersonalAlign.START -> PersonalAlign.CENTER
        PersonalAlign.CENTER -> PersonalAlign.END
        PersonalAlign.END -> PersonalAlign.JUSTIFY
        PersonalAlign.JUSTIFY -> PersonalAlign.START
    }
    setAlign(next)
}

/** Carry the line one place up ([direction] -1) or down (+1) the page. */
private fun PersonalEditorState.walkLine(direction: Int) {
    val from = focusedIndex()
    if (from < 0) return
    val to = from + direction
    if (to !in blockIds.indices) return
    // The editor's list-side move: the target is counted against the ORIGINAL
    // list, and the caret's line is focused either way, so it stays the line
    // being carried.
    moveBlock(from, to)
}

/** The next mark for the caret's line (ten of them, then back to the dot). */
private fun PersonalEditorState.cycleLineMarker() {
    val marks = PersonalMarker.entries
    val now = marks.indexOf(markerOfFocused())
    applyMarker(marks[(now + 1) % marks.size])
}

/** The next writing face for the caret's line. */
private fun PersonalEditorState.cycleLineFace() {
    val now = PERSONAL_FONT_KEYS.indexOf(fontOfFocused())
    applyFont(PERSONAL_FONT_KEYS[(now + 1) % PERSONAL_FONT_KEYS.size])
}

/** Today, said the way a diary divides a page. */
internal fun journalTodayLine(now: Long = System.currentTimeMillis()): String {
    val format = java.text.SimpleDateFormat("EEEE d MMMM yyyy", java.util.Locale.getDefault())
    return format.format(java.util.Date(now))
}

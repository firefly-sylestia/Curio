package com.curio.app.navigation

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.curio.app.data.AppPreferences
import com.curio.app.ui.theme.CurioRevealHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * v3xx45 — SCREEN REVEAL (Settings ▸ Experiments, default OFF).
 *
 * Opening a screen plays the SAME motion as the light/dark flip: the frame the
 * user was looking at is frozen and peeled away in a feathered circular iris
 * centred on the tap, while the destination composes underneath. It runs a
 * touch FASTER than the 680ms theme wipe so navigation never feels slow.
 *
 * The freeze has to happen BEFORE the destination composes, and the app
 * navigates from ~120 call sites. Rather than wrap every one, the frame is
 * captured optimistically on pointer-DOWN (the tap happens ~50ms+ before its
 * click handler navigates) and stashed here; a destination-change listener then
 * plays the reveal from that stashed frame. If no fresh frame is armed — pin
 * navigation, back gestures, deep links, a failed capture — the normal page
 * transitions run untouched, so the experiment can never wedge navigation.
 */
object CurioRevealTaps {
    /** Last pointer-DOWN position in root pixels — the iris centre. */
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    val center: Offset get() = Offset(x, y)
}

/** Records every pointer-down position (and warms the next reveal frame) so a
 *  screen reveal can centre on the tap that opened it. One passive handler on
 *  the NavHost root — it never changes hit-testing (Initial pass only) and
 *  writes a couple of floats plus (when the experiment is on) one frame copy. */
fun Modifier.trackRevealTaps(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            CurioRevealTaps.x = change.position.x
            CurioRevealTaps.y = change.position.y
            // Only the DOWN transition — a drag would otherwise re-arm on
            // every move event.
            if (change.pressed && !change.previousPressed) CurioRevealNav.armReveal()
        }
    }
}

/** How long the screen-reveal iris takes — a touch faster than the light/dark
 *  wipe (680ms) so opening a screen stays snappy. */
private const val SCREEN_REVEAL_DURATION_MS = 440

/** Beat between freezing the old frame and letting the reveal animate, so the
 *  destination has really drawn underneath the frozen frame first. Shorter
 *  than the theme wipe's settle because nothing is being recoloured. */
private const val SCREEN_REVEAL_SETTLE_MS = 32

/** A warmed frame older than this is thrown away rather than revealed. */
private const val FRAME_FRESHNESS_MS = 900L

/** Re-use an already-warm frame for taps this close together instead of
 *  re-copying the screen on every rapid tap. */
private const val FRAME_REUSE_MS = 700L

/** Long-lived scope for the (sub-second) warm-up + reveal coroutines. */
private val revealScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/**
 * Owns the stashed pre-tap frame and turns it into a reveal on navigation.
 * All members run on the main thread — [armReveal] is called from the tap
 * handler and [onDestinationChanged] from the NavController's own callback.
 */
object CurioRevealNav {

    private var pendingFrame: Bitmap? = null
    private var pendingAt = 0L

    /**
     * Warm the next reveal frame from the tap that is about to navigate.
     * Cheap no-op while the experiment is off, while a reveal is already
     * playing, or when a recent enough frame is already stashed.
     */
    fun armReveal() {
        if (!AppPreferences.screenRevealEnabledState) {
            discard()
            return
        }
        val state = CurioRevealHost.transition ?: return
        if (state.isAnimating) return
        val existing = pendingFrame
        if (existing != null && !existing.isRecycled &&
            SystemClock.uptimeMillis() - pendingAt <= FRAME_REUSE_MS
        ) {
            return
        }
        revealScope.launch {
            val frame = runCatching { state.captureFrameNow() }.getOrNull() ?: return@launch
            if (frame.isRecycled) return@launch
            // A newer tap may have landed while this copy was in flight —
            // keep the freshest frame and drop the older one.
            discard()
            pendingFrame = frame
            pendingAt = SystemClock.uptimeMillis()
        }
    }

    /**
     * Called from the NavController's destination listener. [skipReveal] is
     * true for navigations whose motion is hand-tuned elsewhere — the
     * shared-element hand-offs (the Spin ticket → Reveal hero morph) and the
     * settings family (whose nav-rail pill morph IS the transition). The iris
     * would fight both, so the experiment steps aside and they keep their own
     * motion. Otherwise it plays the reveal from the stashed frame and reports
     * whether the reveal owns the frame — the NavHost then suppresses its own
     * enter/exit transition so the iris is the only motion on screen.
     */
    fun onDestinationChanged(skipReveal: Boolean) {
        if (skipReveal) {
            discard()
            CurioRevealHost.suppressDefaultTransition = false
            return
        }
        CurioRevealHost.suppressDefaultTransition = playReveal()
    }

    private fun playReveal(): Boolean {
        val frame = pendingFrame
        pendingFrame = null
        if (frame == null) return false
        val fresh = SystemClock.uptimeMillis() - pendingAt <= FRAME_FRESHNESS_MS
        val state = CurioRevealHost.transition
        if (!fresh || frame.isRecycled || !AppPreferences.screenRevealEnabledState || state == null) {
            frame.takeUnless { it.isRecycled }?.recycle()
            return false
        }
        state.revealDurationMs = SCREEN_REVEAL_DURATION_MS
        state.revealSettleDelayMs = SCREEN_REVEAL_SETTLE_MS
        val started = state.startTransitionWithFrame(frame, CurioRevealTaps.center)
        // A refused start (a sweep already in flight) leaves the frame with us.
        if (!started) frame.takeUnless { it.isRecycled }?.recycle()
        return started
    }

    /** Drop the stashed frame — it is never shown, so it is safe to recycle. */
    private fun discard() {
        val old = pendingFrame
        pendingFrame = null
        pendingAt = 0L
        old?.takeUnless { it.isRecycled }?.recycle()
    }
}

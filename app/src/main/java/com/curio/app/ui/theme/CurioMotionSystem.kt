package com.curio.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.curio.app.data.AppPreferences

/**
 * v455 — THE MOTION SYSTEM (experiment, default OFF).
 *
 * A second motion vocabulary for the app's SCREENS, ported from
 * **firefly-sylestia/Felicity** (`decorations/…/transitions/*`,
 * `…/itemanimators/*`, `music/…/utils/AnimationUtils.kt`). Felicity is
 * AGPL-3.0 and so is this app, so the port is licence-clean; what follows is
 * the *design* carried over, rewritten for Compose — see `app/MOTION_PLAN.md`
 * for the file-by-file provenance and what is deliberately not ported.
 *
 * **What Felicity actually does, in four sentences** (from its own sources):
 *
 * 1. `SeekableSharedAxisXTransition` — a screen enters drifting **25% of the
 *    scene's width** (`TRANSLATION_FRACTION = 0.25`) *while* fading in, and
 *    the outgoing screen drifts the other way while fading out. It is a
 *    **crossfade with drift, not a slide**: both screens are visible for the
 *    whole 500ms, which is why it reads as depth rather than as travel.
 * 2. `SeekableSharedAxisZTransition` — a screen scales and fades along Z
 *    (`SCALE_IN_FROM = 0.5`, `SCALE_OUT_TO = 1.5`): forward, the newcomer
 *    comes *forward from behind*; backward, it recedes in from *in front*.
 * 3. `SeekableSharedAxisFadeTransition` — a pure crossfade, for peers.
 * 4. **Seekable** — the same transition is driven by the back gesture's own
 *    progress: `getProgress()` uses **linear** interpolation while the
 *    animator is being seeked by a gesture and the decelerate curve when it
 *    runs free, so the content tracks the finger 1:1 and only *settles* when
 *    the finger lets go.
 *
 * **The one deliberate deviation.** Felicity's Z starts at 0.5 and ends at
 * 1.5. This app has been here before and rejected it: v166 records *"the
 * violent page opening"* that came from a 0.88 scale pop and softened it to
 * 0.94. So the Z *shape* is ported (scale + fade, mirrored on the way back)
 * and the *amounts* are tempered to [SCALE_IN]/[SCALE_OUT] — the vocabulary,
 * not the extremity. Same for the drift: [DRIFT] is Felicity's own 0.25, and
 * it is deliberately larger than this app's old 1/6 push, because the old
 * push was the thing the member called clanky.
 *
 * **Scope (what this file owns).** Screen motion only: the four navigation
 * transitions, the peer crossfade, and the item-entrance primitive
 * ([curioItemIn]). The floating furniture's clock ([CurioMotion]'s pill
 * arrive/leave) is NOT moved, on purpose — those numbers are tuned per
 * surface and approved, they are not "opening" motion, and swapping them
 * would make every pill in the app slower for no visible gain. Recorded in
 * `MOTION_PLAN.md` as the next step if it is ever wanted.
 */

object CurioMotionSystem {

    /** Felicity's `DEFAULT_DURATION`. One nav clocks nothing shorter. */
    const val NAV_MS: Int = 500

    /**
     * What a navigation actually runs for: [NAV_MS], or its Lite-mode twin.
     *
     * Lite mode shortens motion rather than removing it (that was its own
     * promise — *"disables useless animation without making it clanky"*), and a
     * screen open is not useless animation, so it still plays: it just plays
     * 320ms instead of 500. The vocabulary is identical, which is the point —
     * Lite mode is not a different design, it is the same design with less of
     * it. See `CurioLiteMode.kt`.
     */
    val NavMs: Int
        get() = if (isLiteMode) 320 else NAV_MS

    /** Felicity's `TRANSLATION_FRACTION`: a quarter of the width, no more. */
    const val DRIFT: Float = 0.25f

    /**
     * Z's amounts, tempered from Felicity's 0.5 / 1.5 (see the class note):
     * the newcomer grows from just-behind, the leaver recedes to just-in-front.
     */
    const val SCALE_IN: Float = 0.86f
    const val SCALE_OUT: Float = 1.14f

    /** Felicity's item animator: 300ms in, 500ms out, from 0.85 scale. */
    const val ADD_MS: Int = 300
    const val REMOVE_MS: Int = 500
    const val ADD_SCALE: Float = 0.85f

    /** `FlipItemAnimator`'s `changeDuration` — a content swap. */
    const val CHANGE_MS: Int = 400

    /**
     * `DecelerateInterpolator(3f)` — Android computes
     * `1 - (1 - t)^(2 * factor)`, so factor 3 is `1 - (1 - t)^6`: a hard,
     * fast opening move that spends most of its time arriving. That curve is
     * the feel of every free-running Felicity transition, and it is why a
     * 500ms nav does not read as slow.
     */
    val Settle: Easing = Easing { t -> 1f - (1f - t).let { it * it * it * it * it * it } }

    /**
     * The seeked curve (Felicity's `LinearInterpolator` branch): when a
     * gesture is scrubbing the transition, progress must be the finger's own
     * progress or the content fights the hand. Kept here so the day the
     * predictive-pop overload lands (navigation 2.10's
     * `predictivePopEnterTransition` / `predictivePopExitTransition`) there is
     * one obvious easing to hand it.
     */
    val Track: Easing = LinearEasing

    /** A quiet curve for the items that arrive inside a screen. */
    val Soft: Easing = CubicBezierEasing(0.22f, 0.9f, 0.24f, 1f)
}

/** Whether the motion system is in charge right now (reactive). */
val curioMotionSystemOn: Boolean
    get() = AppPreferences.motionSystemState

// ── Shared axis X: the drift ───────────────────────────────────────────
//
// Felicity's X, exactly: the newcomer drifts in from +25% (forward) or −25%
// (back) while fading in; the leaver drifts to the opposite side while
// fading out. Both are on screen for the whole 500ms.

/** Entering screen: drifts in from the side it is coming from, fading in. */
fun sharedAxisXEnter(forward: Boolean = true): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { full -> if (forward) (full * CurioMotionSystem.DRIFT).toInt()
        else -(full * CurioMotionSystem.DRIFT).toInt() },
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    ) + fadeIn(
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    )

/** Exiting screen: drifts the other way while fading out. */
fun sharedAxisXExit(forward: Boolean = true): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { full -> if (forward) -(full * CurioMotionSystem.DRIFT).toInt()
        else (full * CurioMotionSystem.DRIFT).toInt() },
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    ) + fadeOut(
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    )

// ── Shared axis Z: the open ────────────────────────────────────────────
//
// Forward: the newcomer grows from SCALE_IN and fades in; the leaver recedes
// to SCALE_OUT and fades out. Backward: mirrored — the screen underneath comes
// back in from SCALE_OUT, and the one on top shrinks away to SCALE_IN. (This
// is Felicity's Z with its amounts tempered — see the class note.)

/** The screen being opened: grows in from behind, fading in. */
fun sharedAxisZEnter(): EnterTransition =
    scaleIn(
        initialScale = CurioMotionSystem.SCALE_IN,
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    ) + fadeIn(
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    )

/** The screen being left behind a Z open: recedes, fading out. */
fun sharedAxisZExit(): ExitTransition =
    scaleOut(
        targetScale = CurioMotionSystem.SCALE_OUT,
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    ) + fadeOut(
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    )

/** Popping a Z open: the page underneath returns from in front, fading in. */
fun sharedAxisZPopEnter(): EnterTransition =
    scaleIn(
        initialScale = CurioMotionSystem.SCALE_OUT,
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    ) + fadeIn(
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    )

/** The top page of a Z pop: shrinks away behind, fading out. */
fun sharedAxisZPopExit(): ExitTransition =
    scaleOut(
        targetScale = CurioMotionSystem.SCALE_IN,
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    ) + fadeOut(
        animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle),
    )

// ── Shared axis F: the peer fade ───────────────────────────────────────
//
// Felicity's fade transition, for screens that are siblings of each other
// rather than a step deeper: the bottom-nav tabs, the settings family (whose
// nav rail's pill is a shared element and owns the motion), and the two
// routes whose own shared element is the whole animation (Topic Reveal, the
// Pet Designer). A drift on any of those would fight the element that is
// already moving.

/** A peer hand-off: pure crossfade, one clock. */
fun sharedAxisFadeEnter(): EnterTransition =
    fadeIn(animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle))

/** A peer hand-off, leaving. */
fun sharedAxisFadeExit(): ExitTransition =
    fadeOut(animationSpec = tween(CurioMotionSystem.NavMs, easing = CurioMotionSystem.Settle))

/**
 * Felicity's item ADD, as a modifier: `alpha 0 → 1` with `scale 0.85 → 1`
 * over 300ms (`FelicityDefaultAnimator.animateAdd`).
 *
 * Applied to a row, it animates when the row's [key] is first composed — so a
 * list that gains an item makes it arrive, and a list that is already there
 * is simply there (the key is what says "this one is new", and a key that is
 * the item itself means a *different* item is a new arrival). [order] staggers
 * a run of arrivals by a few ms each, which Felicity did not need (a
 * RecyclerView animates what changed, not a whole screen) and a Compose list
 * does: without it a first composition lands every row on the same frame and
 * the stagger is what makes it read as a list settling.
 *
 * Inert when the motion system is off, and reads its progress in the DRAW
 * phase, so an arriving row never recomposes.
 */
@Composable
fun Modifier.curioItemIn(
    key: Any?,
    order: Int = 0,
    staggerMs: Int = 45,
): Modifier {
    if (!curioMotionSystemOn) return this
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = CurioMotionSystem.ADD_MS,
                delayMillis = (order * staggerMs).coerceAtMost(240),
                easing = CurioMotionSystem.Soft,
            ),
        )
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        val scale = CurioMotionSystem.ADD_SCALE + (1f - CurioMotionSystem.ADD_SCALE) * p
        scaleX = scale
        scaleY = scale
    }
}

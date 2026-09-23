package com.curio.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Curio's motion design tokens — see Curio design contract section 0.5.
 *
 * Centralizes the spring specs + duration constants that the rest of the app
 * uses for animations, so every transition uses the same easing vocabulary.
 *
 * Spring presets (per M3 expressive motion spec + Curio morph extensions):
 *
 *  - [Springs.Snappy] — high stiffness, no overshoot. For small UI changes
 *    that should feel decisive (chip selection, drawer toggles, modal
 *    mounts). Use for anything where overshoot would feel jittery.
 *
 *  - [Springs.Bouncy] — lower stiffness, ~55% damping ratio. For reward
 *    moments + playful arrivals. Things should overshoot and settle like
 *    a gummy bounce. Use for the dial settling on the Spin screen,
 *    the entry card mounting, the Save success animation.
 *
 *  - [Springs.Deliberate] — moderate stiffness, slight overshoot. For
 *    bigger elements moving larger distances (screen transitions, sheet
 *    mounts). Slower than Snappy but more controlled than Bouncy.
 *
 *  - [Springs.Morph] — very low stiffness, high damping for organic
 *    shape/size morphing. Like a water droplet settling — slow, smooth,
 *    no bounce. Use for morphing transitions between screen states.
 *
 *  - [Springs.Elastic] — extreme overshoot, very bouncy. For dramatic
 *    entrances (hero cards, reward moments, the splash → home transition).
 *    Use sparingly — it's the "show-off" spring.
 *
 * Duration tokens in milliseconds:
 *
 *  - [Durations.Quick]        — 150ms (chip toggles, button presses)
 *  - [Durations.Standard]     — 300ms (default transitions)
 *  - [Durations.Deliberate]   — 500ms (larger movements)
 *  - [Durations.Morph]        — 700ms (shape morphing transitions)
 *  - [Durations.Reveal]       — 900ms (dramatic reveal moments)
 *  - [Durations.SpinMin]      — 2800ms (low end of The Spin rotation)
 *  - [Durations.SpinMax]      — 3600ms (high end of The Spin rotation)
 *  - [Durations.Confetti]     — 600ms (reward burst lifetime)
 *  - [Durations.ConfettiLong] — 1200ms (extended burst for save success)
 *  - [Durations.RevealHold]   — 400ms (pause after landing before nav to Reveal)
 *
 * v439 — AND THE PILL CLOCK IS THE SECOND HALF OF THIS FILE (see
 * [ENTER_MS]/[EXIT_MS] and [settle]). The springs above answer "how does a
 * thing that is already MOVING settle"; these answer "how does a thing that
 * appears COME and GO", which the floating furniture of the reading and
 * writing surfaces needed and did not have — every pill carried its own
 * numbers, which is what produced the member's report *"the animations are
 * still bad"*: not that any one of them was wrong, but that a pill arriving in
 * 180ms beside one arriving in 220ms has no shared language.
 */
object CurioMotion {

    object Springs {
        /** No overshoot, fast — chip toggles, button presses, drawer mounts. */
        val Snappy: SpringSpec<Float> = spring(
            dampingRatio = 1.0f,
            stiffness = 1800f
        )

        /** ~55% damping ratio, medium stiffness — gummy-bounce overshoot for rewards. */
        val Bouncy: SpringSpec<Float> = spring(
            dampingRatio = 0.55f,
            stiffness = 380f
        )

        /** ~85% damping ratio, slower stiffness — controlled overshoot for big transitions. */
        val Deliberate: SpringSpec<Float> = spring(
            dampingRatio = 0.85f,
            stiffness = 250f
        )

        /**
         * v166 — CRITICALLY damped calm spring: identical physics to the
         * nav-pill family (damping 1.0 = zero overshoot, stiffness 750 =
         * half of Medium) so screen entrances settle smoothly with NO
         * zoom-back. Used by the page/grid openings that used to run the
         * [Deliberate] spring (0.85 damping still overshoots ~1% and 250
         * stiffness dragged the settle past 700ms — the "violent page
         * opening" feel).
         */
        val Calm: SpringSpec<Float> = spring(
            dampingRatio = 1.0f,
            stiffness = 750f
        )

        /**
         * Organic morph spring — very low stiffness, high damping.
         * Like a water droplet settling; slow, smooth, no bounce.
         * Use for shape/size morphing, screen-to-screen transitions.
         *  ~200ms to 95% settled, ~700ms to full rest.
         */
        val Morph: SpringSpec<Float> = spring(
            dampingRatio = 0.92f,
            stiffness = 120f
        )

        /**
         * Extreme bouncy overshoot for dramatic entrances.
         * The splash → home transition, hero card appearances, reward
         * moments that deserve the "wow" treatment.
         * v7.94 — stiffness raised / damping raised so the entrance
         * settles noticeably faster (still bouncy, but no more 1s+
         * settle that read as a delayed animation).
         */
        val Elastic: SpringSpec<Float> = spring(
            dampingRatio = 0.45f,
            stiffness = 340f
        )

        /**
         * Gentle press-down — scale to 0.94 with a quick snap-back.
         * Used by interactive cards and buttons for tactile feedback.
         */
        val Press: SpringSpec<Float> = spring(
            dampingRatio = 0.65f,
            stiffness = 800f
        )

        /**
         * v465h — [Bouncy]'s physics, TYPED FOR A `Dp`.
         *
         * A width is a `Dp`, and a `SpringSpec<Float>` cannot animate one, so a
         * caller would otherwise restate 0.55/380 at the call site — which is the
         * exact habit the pill clock above exists to stop (*"no floating surface
         * invents its own milliseconds"*; the same is true of damping ratios).
         * The reader's speak bar is the first user: it morphs between its full
         * width and its own disc, and the overshoot is what makes the sweep read
         * as a bounce rather than a resize.
         */
        val BouncyDp: SpringSpec<Dp> = spring(
            dampingRatio = 0.55f,
            stiffness = 380f
        )
    }

    object Durations {
        const val Quick: Int = 150
        const val Standard: Int = 300
        const val Deliberate: Int = 500

        /**
         * v440 — HOW LONG A PAGE TAKES TO PUSH IN, AND TO COME BACK.
         *
         * The member, after the journal's own open: *"the animation open animation
         * of journal is clanky"* — and the cause was this clock, not the journal.
         * Every plain forward navigation (the journal editor, a chapter, a
         * profile, a book) fell into the nav host's generic branch, which glided
         * the new page in over **[Deliberate] = 500ms** while the outgoing page
         * drifted for the same half second behind it: half a second is a beat the
         * eye reads as the app thinking, and on a phone it is the difference
         * between a push and a slow cross-fade.
         *
         * The screen push has its own clock now, and it is the one every modern
         * platform uses for this: **a page arrives in a quarter of a second and is
         * gone a little faster than that on the way back**, with the travel
         * unchanged (1/6 of the width in, 1/8 out) so the language of the push is
         * the same — only its tempo changed. [Deliberate] stays for what it was
         * written for: a change worth watching, never a screen appearing.
         */
        const val Push: Int = 260

        /**
         * And the way back. A pop is a VERDICT — the member has already decided —
         * so it is quicker than the push it mirrors, the same asymmetry the
         * floating pills use ([ENTER_MS] vs [EXIT_MS]).
         */
        const val Pop: Int = 220

        /** Shape morphing transitions — smooth but snappy (v7.94: 700 → 450
         *  so screen-to-screen morphs stop feeling laggy). */
        const val Morph: Int = 450

        /** Dramatic reveal moments (splash → home, topic landing).
         *  v7.94: 900 → 650 — still dramatic, just no longer a pause. */
        const val Reveal: Int = 650

        /** The Spin rotation window — premium and unhurried: the wheel
         *  glides for a touch longer (2.8–3.6s) so the deceleration reads
         *  as a graceful reel slowing down rather than a fast whip, paired
         *  with a smooth sine deceleration curve. */
        const val SpinMin: Int = 2800
        const val SpinMax: Int = 3600

        /** Confetti / sparkle burst lifetime (per section 0.5: ~600ms total). */
        const val Confetti: Int = 600

        /** Extended confetti for big moments (save success). */
        const val ConfettiLong: Int = 1200

        /** Sparkle trail particle lifetime. */
        const val SparkleTrail: Int = 400

        /** Pause between Spin landing and auto-navigation to Topic Reveal. */
        const val RevealHold: Int = 400

        /** Shimmer sweep duration. */
        const val Shimmer: Int = 1500

        /** Breathing / ambient pulse cycle. */
        const val Breathe: Int = 3200

        /**
         * v465h — HOW LONG THE READING VOICE'S BAR STAYS OPEN.
         *
         * The voice's bar rests as its own disc while it reads (the member's
         * §60 choice: *"collapse to one round play/pause disc"*), and this is the
         * dwell before it closes: long enough to read the four controls and reach
         * one of them, short enough that a member who is listening rather than
         * steering gets their page back. It is a DWELL, not an animation clock, so
         * it is deliberately longer than anything in the pill clock above — and it
         * is a token rather than a literal for the same reason those are.
         */
        const val SpeakRest: Int = 4200
    }

    /** Particle count for the confetti burst (per section 0.5: 6 to 10 tiny shapes). */
    const val ConfettiParticleCount: Int = 8

    /** Extended particle count for big reward moments. */
    const val ConfettiParticleCountLarge: Int = 18

    /** Number of full rotations per Spin (per section 5: 3 to 5). */
    const val MinSpinTurns: Int = 3
    const val MaxSpinTurns: Int = 5

    // ── THE PILL CLOCK (v439) ───────────────────────────────────────────────
    //
    // What the reading and writing surfaces' floating furniture is built from:
    // the reader's head, foot, search and page scrubber, the journal's dock,
    // copy box, undo pill, mic and pinned line, and the sheets they open.
    //
    // THREE RULES, and they are rules rather than preferences:
    //
    //  1. **No floating surface invents its own milliseconds.** A duration
    //     written as a literal next to a pill is a bug — add a token instead.
    //  2. **Enter is slower than exit, always.** On the way in the member is
    //     deciding; on the way out the system is getting out of the way. That
    //     asymmetry is why an exit never reads as slow even when it is not
    //     short — and it is the reason a member can say a sheet closes
    //     "weirdly slow" while it is objectively fast: the travel was the
    //     problem, not the clock (a short sheet used to slide the full screen
    //     cap — see `ReaderSheetFrame`).
    //  3. **Travel is proportional to the thing that moves.** See [settle].

    /**
     * What arrives. 190ms on a slow-in/slow-out curve: long enough to read as
     * movement, short enough that a member tapping a tool never waits for it.
     *
     * v440 — 220 → 190, because the member's report after living with it was that
     * the app's furniture still read as soft: a tool that appears UNDER THE THUMB
     * (the journal's dock, its copy box, the reader's selection bar) is not a thing
     * the eye needs to watch arrive — it is a thing the member has already decided
     * to use, and every millisecond over ~200 is a beat they feel as the app
     * catching up. The curves and the one-clock rule are unchanged; only the tempo
     * is crisper.
     */
    const val ENTER_MS = 190L

    /**
     * What leaves. A faster verdict, on the curve that spends its travel early —
     * a thing on its way out should be gone by the time the eye looks for it.
     */
    const val EXIT_MS = 130L

    /**
     * A change worth watching: a panel growing under a row, a page's colour
     * settling, a chip becoming a bar. Only for something the member is looking
     * AT while it happens; never for a tool arriving.
     */
    const val EMPHASIS_MS = 320L

    /** The tick under a press, and the settle of a drag that let go — short
     *  enough to feel mechanical rather than animated. */
    const val TICK_MS = 90L

    /**
     * The pause a drag waits before it decides whether it was a throw or a
     * nudge. Long enough that a deliberate slow drag is never cut off, short
     * enough that a flick is answered at once.
     */
    const val SETTLE_DEBOUNCE_MS = 80L

    /** Arriving: no dead start, no hard stop. */
    val Enter: Easing = FastOutSlowInEasing

    /** Leaving: immediate, then out of the way. */
    val Exit: Easing = FastOutLinearInEasing

    /** A soft hand-off — used where something fades IN as a tool is put away. */
    val Soften: Easing = LinearOutSlowInEasing

    /** A panel growing in place, and something meant to be watched change. */
    val Emphasis: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * How far a floating pill drifts as it arrives, as a FRACTION of its own
     * height — the reader's head has settled this way since v437, and it is what
     * makes a pill look like it came from the edge it lives on without travelling
     * far enough to read as a slide.
     *
     * A fraction rather than a dp count on purpose: the app's pills are 42dp,
     * 46dp, 50dp and 58dp tall, and one fixed nudge would be a jump for the
     * tallest and invisible on the shortest.
     */
    const val SETTLE_FRACTION = 1f / 6f

    /** That drift as an `IntOffset`, for a `slideIn/OutVertically` lambda. */
    fun settle(heightPx: Int): Int = -(heightPx * SETTLE_FRACTION).toInt()

    /** The same drift as a [Dp], for a surface that measures in dp. */
    fun settle(height: Dp): Dp = height * SETTLE_FRACTION

    // ── AND THE SPECS THEMSELVES ────────────────────────────────────────────
    //
    // Rule 1 above is only enforceable if the token set can be SPENT in one
    // step: a surface that has to hand-build `fadeIn(tween(220, Enter)) +
    // slideInVertically(…)` will eventually hand-build it slightly differently.
    // These four factories are the whole vocabulary of the app's floating
    // furniture, and a pill's arrival is written as `pillArrive()`. Nothing here
    // holds state — each call builds a fresh spec — so they are safe to share.

    /** A bare fade in, on the enter clock (a scrim, a wash, a night dim). */
    fun arriveFade(): EnterTransition =
        fadeIn(tween(ENTER_MS.toInt(), easing = Soften))

    /** A bare fade out, on the exit clock. */
    fun leaveFade(): ExitTransition =
        fadeOut(tween(EXIT_MS.toInt(), easing = Exit))

    /**
     * ONE ARRIVAL: a floating pill comes in by fading on the enter clock while
     * drifting a sixth of its own height from the edge it lives on.
     *
     * [fromTop] is which edge that is — the reader's head and its search bar come
     * down from above, the foot, the scrubber, the journal's dock and every pill
     * that grows out of the page come up from below. `settle` is negative, so the
     * bottom edge negates it.
     */
    fun pillArrive(fromTop: Boolean = false): EnterTransition =
        arriveFade() + slideInVertically(
            tween(ENTER_MS.toInt(), easing = Enter)
        ) { height -> if (fromTop) settle(height) else -settle(height) }

    /** And it leaves the same way, on the exit clock. */
    fun pillLeave(fromTop: Boolean = false): ExitTransition =
        leaveFade() + slideOutVertically(
            tween(EXIT_MS.toInt(), easing = Enter)
        ) { height -> if (fromTop) settle(height) else -settle(height) }

    /**
     * The POP — the second arrival, for a round control that has no edge to come
     * from because it floats free over the page: the journal's mic, the marks
     * bubble, a reward. It grows from [POP_SCALE] rather than travelling, so a
     * button that appears under the member's thumb does not look like it slid in
     * from nowhere in particular.
     */
    fun popArrive(): EnterTransition =
        arriveFade() +
            scaleIn(
                tween(ENTER_MS.toInt(), easing = Enter),
                initialScale = POP_SCALE
            )

    /** And it leaves the same way. */
    fun popLeave(): ExitTransition =
        leaveFade() +
            scaleOut(
                tween(EXIT_MS.toInt(), easing = Enter),
                targetScale = POP_SCALE
            )

    /** How small a popping control starts and ends (a knob, not a dot). */
    const val POP_SCALE = 0.80f
}

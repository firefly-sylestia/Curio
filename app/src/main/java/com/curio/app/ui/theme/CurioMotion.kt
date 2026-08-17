package com.curio.app.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

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
    }

    object Durations {
        const val Quick: Int = 150
        const val Standard: Int = 300
        const val Deliberate: Int = 500

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
    }

    /** Particle count for the confetti burst (per section 0.5: 6 to 10 tiny shapes). */
    const val ConfettiParticleCount: Int = 8

    /** Extended particle count for big reward moments. */
    const val ConfettiParticleCountLarge: Int = 18

    /** Number of full rotations per Spin (per section 5: 3 to 5). */
    const val MinSpinTurns: Int = 3
    const val MaxSpinTurns: Int = 5
}
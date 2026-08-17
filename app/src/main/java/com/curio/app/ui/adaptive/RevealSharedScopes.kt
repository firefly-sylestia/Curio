package com.curio.app.ui.adaptive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Shared-element handoff between the Spin deck and the Topic Reveal page.
 *
 * The Spin front ticket and the Reveal hero are matched shared elements
 * (key [RevealSharedElementKey]), so opening a landed topic morphs the
 * reveal hero OUT of the ticket's position instead of sliding the page in.
 *
 * The scopes are created by [androidx.compose.animation.SharedTransitionLayout]
 * in CurioNavHost and provided per-destination here, because this Compose
 * version has no built-in CompositionLocal for the shared transition scope.
 * Both locals are null-guarded at the consumer sites (never null in the
 * NavHost subtree).
 */
const val RevealSharedElementKey = "reveal-hero"

/**
 * Bounds animation for the reveal morph — a quick, even FastOutSlowIn
 * tween (320ms) so the card expands into the hero smoothly without the
 * default spring's wobble or the earlier laggy feel.
 */
val RevealBoundsTransform = BoundsTransform { _, _ ->
    tween(320, easing = FastOutSlowInEasing)
}

/**
 * Bounds animation for the Cabinet→Detail morph — a near-critically damped
 * spring so the saved-entry card glides into the full-width detail hero
 * banner smoothly. The reveal's short 320ms tween read as a mechanical snap
 * on this much larger aspect change; the spring eases in AND out with no
 * overshoot wobble (dampingRatio 0.9). v8.36.
 */
val CabinetBoundsTransform = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.9f, stiffness = 260f)
}

/** The SharedTransitionScope instance wrapping the NavHost. */
val LocalRevealSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The destination's AnimatedContentScope (controls the element's visibility). */
val LocalRevealVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

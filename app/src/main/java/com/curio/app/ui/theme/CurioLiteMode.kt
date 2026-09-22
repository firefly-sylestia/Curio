package com.curio.app.ui.theme

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import com.curio.app.data.AppPreferences

/**
 * v454 — LITE MODE, the one door every "is this decorative motion allowed?"
 * question goes through.
 *
 * Lite mode is a *performance* profile, not a style: it exists so the app
 * stays smooth on weaker hardware. So the rule for gating is precise —
 *
 *   GATE the motion that only exists to be looked at:
 *     ambient clocks (shimmer, twinkle, breathe, idle wobble) and
 *     refraction/blur passes.
 *
 *   NEVER gate motion that carries meaning:
 *     a screen transition, a sheet opening, a press, a state change, a
 *     progress bar answering a real action, a page turn. Freezing those does
 *     not make the app cheaper to use — it makes it feel broken ("clanky"),
 *     which is exactly what Lite mode must not do.
 *
 * Layouts, colours and state are untouched in Lite mode, so nothing moves,
 * nothing disappears and no screen restyles itself — only the loops that
 * would otherwise hold the render thread's attention stop holding it. Every
 * switch it holds down is a preference that comes back exactly as it was.
 *
 * The two consumers today are the glass gate in
 * [com.curio.app.ui.components.isLiquidGlassRequested] (the single question
 * all ~33 glass sites already ask) and this file's [isAmbientMotionOn], read
 * by the shared animation helpers and the screens' own idle clocks.
 */

/** Whether Lite mode is on right now (reactive — reads the live preference). */
val isLiteMode: Boolean
    get() = AppPreferences.liteModeState

/**
 * Whether decorative, always-running motion may play. The inverse of
 * [isLiteMode], named positively so call sites read as the question they are
 * actually asking.
 */
val isAmbientMotionOn: Boolean
    get() = !AppPreferences.liteModeState

/**
 * The ambient clock: exactly `rememberInfiniteTransition`, but it hands back
 * **null** in Lite mode so the caller's decorative loop simply does not run.
 *
 * Callers compose the two states: `val t = rememberAmbientTransition("x")`,
 * then `t?.animateFloat(...)` with a resting value behind the `?:` — see the
 * call sites (splash, the drawer's star map, the Incursion mark, Spin's idle
 * die, the pet sprite's flourishes). Nothing else about the call site
 * changes, and the same shape also serves a screen that wants to park its
 * clock while it is not visible.
 *
 * Conditional composable calls are safe in Compose: flipping the branch
 * discards the other side's remembered state, which for an infinite
 * transition is exactly what should happen.
 */
@Composable
fun rememberAmbientTransition(label: String): InfiniteTransition? =
    if (isAmbientMotionOn) rememberInfiniteTransition(label = label) else null

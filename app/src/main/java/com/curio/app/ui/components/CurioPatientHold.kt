package com.curio.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration

/**
 * v407 — how long a hold-to-act gesture waits before it fires.
 *
 * The platform's own long press is ~500ms, which is short enough that a scroll
 * starting ON a row can trip it: the finger rests on the row for the timeout
 * while the list is still deciding it is a swipe, and the row's option pill
 * appears mid-scroll (the member's report on Recents). Two seconds is
 * deliberately unhurried — a real hold still lands well before a person gives
 * up, and a swipe never reaches it.
 */
const val CurioHoldMillis = 2_000L

/**
 * v407 — wraps a subtree so every `combinedClickable` long press inside it
 * waits [CurioHoldMillis] instead of the platform's short timeout.
 *
 * The platform's own cancellation is left in place, and it is half the fix: a
 * scroll that consumes the gesture cancels the pending press (Compose checks
 * the consumption at both the main and final passes), so a swipe can never arm
 * a row. This only makes the remaining window long enough that a resting
 * finger during a slow scroll cannot reach it either.
 *
 * Reacts to the platform configuration, so a device with its own longer hold
 * still gets max(platform, [CurioHoldMillis]).
 */
@Composable
fun CurioPatientHold(content: @Composable () -> Unit) {
    val base = LocalViewConfiguration.current
    val patient = remember(base) {
        object : ViewConfiguration by base {
            override val longPressTimeoutMillis: Long
                get() = maxOf(base.longPressTimeoutMillis, CurioHoldMillis)
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides patient, content = content)
}

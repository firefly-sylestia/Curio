package com.curio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.curio.app.ui.theme.CurioMotion

/**
 * v3xx46 — the shared PRESS FEEDBACK primitive.
 *
 * The app's tappable surfaces had drifted: the nav bar and a few hero cards
 * squished under the finger while every settings row, hub card and chip just
 * changed colour. [curioPressClickable] gives them one motion language — the
 * surface scales down a hair on touch-down and springs back on release (the
 * [CurioMotion.Springs.Press] family: a quick snap-back, no rubbery overshoot
 * on a small element), with one light tick of haptics as the finger lands.
 *
 * It is a drop-in for `Modifier.clickable(onClick = …)`: the ripple is
 * carried over from [LocalIndication] (so nothing loses its press wash) and
 * the scale is a draw-only transform, so neighbours never reflow while a
 * surface squishes.
 *
 * @param pressedScale how far to shrink while held. Keep it subtle on
 *        full-width rows and cards (0.97–0.985) — a big surface moving a full
 *        5% reads as a jump rather than a press.
 * @param hapticOnPress one light tick on touch-down. Off for surfaces that
 *        already fire their own haptic (the nav pills) so a tap never buzzes
 *        twice.
 */
@Composable
fun Modifier.curioPressClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    hapticOnPress: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier {
    val press = rememberCurioPressSource(
        pressedScale = pressedScale,
        hapticOnPress = hapticOnPress && enabled
    )
    return this
        .then(press.modifier)
        .clickable(
            interactionSource = press.interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
}

/**
 * The press half of [curioPressClickable] for surfaces that OWN their click
 * gesture — Material3's clickable `Surface`, a `combinedClickable` card, a
 * `ListItem` — which only let you hand in an `interactionSource`. Feed
 * [interactionSource] into that parameter and [modifier] into the surface's
 * modifier chain; the scale + the one-tick haptic then work exactly as they
 * do for [curioPressClickable].
 *
 * Both halves are required: the surface needs the SAME source the press
 * feedback observes, so never pass a different one.
 */
class CurioPressSource(
    val interactionSource: MutableInteractionSource,
    val modifier: Modifier
)

/**
 * Builds a [CurioPressSource] — see its docs. The returned interaction source
 * is remembered across recompositions, so passing it to a `Surface` keeps the
 * press state stable.
 *
 * @param pressedScale 1f disables the squish (e.g. a disabled row).
 */
@Composable
fun rememberCurioPressSource(
    pressedScale: Float = 0.97f,
    hapticOnPress: Boolean = true
): CurioPressSource {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = CurioMotion.Springs.Press,
        label = "curioPressScale"
    )
    val haptics = LocalHapticFeedback.current
    // Fire on the DOWN edge only — keyed on `pressed` so a release (or a
    // drag-off) never ticks, and a cancelled press can't leave a stray buzz.
    LaunchedEffect(pressed) {
        if (pressed && hapticOnPress) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    return CurioPressSource(interaction, Modifier.scale(scale))
}

/**
 * Wraps a control's change callback with the shared one-tick haptic — the
 * feedback for ON/OFF controls (switches, segmented rows) that have no press
 * state of their own to squish. Use it as the `onCheckedChange` /
 * `onSelected` argument:
 *
 * ```
 * val tick = rememberCurioControlTick()
 * Switch(onCheckedChange = { tick { onCheckedChange(it) } })
 * ```
 *
 * The tick fires only for a real user change (the returned lambda is never
 * invoked by initial composition), so a screen that opens with a switch
 * already ON stays silent.
 */
@Composable
fun rememberCurioControlTick(): (() -> Unit) -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) {
        { action ->
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            action()
        }
    }
}

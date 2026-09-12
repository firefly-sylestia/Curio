package com.curio.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * v3xx47 — the app-wide TOUCH feedback: a pressed LOOK instead of a ripple.
 *
 * Material's default ripple draws an expanding circle that reads as a foreign
 * blob on this app's cards, pills and rounded rows (user: "remove weird
 * selection highlights … the touch highlight, for all around the app … maybe
 * with animation or a pressed look"). This indication replaces it with a
 * pressed state: the element's OWN pixels tint toward [tint] over ~90ms on
 * touch-down and ease back over ~260ms on release.
 *
 * The tint is deliberately shape-agnostic — the node paints the content into
 * an offscreen layer and tints it with `SrcAtop`, so the wash lands ONLY on the
 * pixels the element already drew and therefore follows the element's own
 * rounded corners, pill or circle. No ripple geometry, nothing to size
 * against, and it can't bleed past a card's shape.
 *
 * Provided once at the theme root (`LocalIndication`), so every `clickable`,
 * `selectable`, Material surface and texture that reads the ambient indication
 * wears the same press language — the same treatment everywhere, including the
 * bottom sheets.
 */
class CurioPressIndication(private val tint: Color) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        CurioPressNode(interactionSource, tint)

    override fun hashCode(): Int = tint.hashCode()

    override fun equals(other: Any?): Boolean =
        other is CurioPressIndication && other.tint == tint
}

/** How strong the press wash gets at full press. Kept low: the wash is a
 *  state cue, not a highlight — it must never fight the element's own colors. */
private const val PRESS_WASH_ALPHA = 0.14f

/** Fast in (the finger has already landed), slower out (a release should feel
 *  like the surface easing back, not a flicker). */
private const val PRESS_IN_MS = 90
private const val PRESS_OUT_MS = 260

private class CurioPressNode(
    private val interactionSource: InteractionSource,
    private val tint: Color
) : Modifier.Node(), DrawModifierNode {

    /** 0 = at rest, 1 = fully pressed. */
    private val press = Animatable(0f)

    override fun onAttach() {
        // collectLatest cancels the in-flight animateTo when the opposite
        // interaction arrives, so a press/release pair can never leave the
        // wash stuck half-way (the node's scope is cancelled on detach).
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> press.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = PRESS_IN_MS,
                            easing = FastOutSlowInEasing
                        )
                    )
                    is PressInteraction.Release, is PressInteraction.Cancel -> press.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = PRESS_OUT_MS,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val p = press.value
        if (p <= 0.001f) {
            // At rest — draw straight through, no layer, no cost.
            drawContent()
            return
        }
        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
        drawContent()
        // SrcAtop keeps the element's own alpha but replaces its color where it
        // painted — the tint therefore clips itself to the element's shape.
        drawRect(
            color = tint,
            alpha = p * PRESS_WASH_ALPHA,
            blendMode = BlendMode.SrcAtop
        )
        drawContext.canvas.restore()
    }
}

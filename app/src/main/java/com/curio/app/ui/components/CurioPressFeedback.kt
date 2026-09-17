package com.curio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.curio.app.data.CurioAlivePreferences
import com.curio.app.ui.theme.CurioMotion

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
 * Curio's tap + hold primitive. The press animation shares the same visual
 * language as [curioPressClickable], while combinedClickable keeps the
 * long-press gesture available to scrollable surfaces.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.curioPressCombinedClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.975f,
    hapticOnPress: Boolean = true,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier {
    val press = rememberCurioPressSource(
        pressedScale = pressedScale,
        hapticOnPress = hapticOnPress && enabled
    )
    return this
        .then(press.modifier)
        .combinedClickable(
            interactionSource = press.interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onClick = onClick,
            onLongClick = onLongClick
        )
}

class CurioPressSource(
    val interactionSource: MutableInteractionSource,
    val modifier: Modifier
)

@Composable
fun rememberCurioPressSource(
    pressedScale: Float = 0.97f,
    hapticOnPress: Boolean = true
): CurioPressSource {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alive = CurioAlivePreferences.isEnabled(LocalContext.current)
    val targetScale = if (pressed) {
        if (alive) maxOf(pressedScale, 0.955f) else pressedScale
    } else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (alive) {
            androidx.compose.animation.core.spring(dampingRatio = 0.70f, stiffness = 760f)
        } else CurioMotion.Springs.Press,
        label = "curioPressScale"
    )
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(pressed) {
        if (pressed && hapticOnPress) {
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
        }
    }
    val animatedModifier = if (alive) {
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = if (pressed) -0.45f else 0f
            alpha = if (pressed) 0.975f else 1f
            transformOrigin = TransformOrigin.Center
        }
    } else {
        Modifier.scale(scale)
    }
    return CurioPressSource(interaction, animatedModifier)
}

@Composable
fun rememberCurioControlTick(): (() -> Unit) -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) {
        { action ->
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            action()
        }
    }
}

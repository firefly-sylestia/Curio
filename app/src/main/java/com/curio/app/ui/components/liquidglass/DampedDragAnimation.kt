/*
 * Adapted from vFlow (https://github.com/ChaoMixian/vFlow),
 * ui/main/glass/DampedDragAnimation.kt — GPL-2.0-or-later.
 * The draggable active-indicator physics: a critically-damped value spring
 * driven by raw drags, a lagging velocity spring (drives the squash/stretch
 * layer block), a press-progress spring and per-axis scale springs.
 */
package com.curio.app.ui.components.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val canDrag: (Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            }
        ) { change, dragAmount ->
            val isInside = canDrag(change.position)
            val wasInside = canDrag(change.previousPosition)
            if (isInside && wasInside) {
                onDrag(size, dragAmount)
            }
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val clamped = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(clamped, valueAnimationSpec) { updateVelocity() } }
        }
    }

    // v292h — INSTANT SNAP: used by the tab-switch LaunchedEffect so the
    // blob appears under the new tab immediately (no sideways glide).
    // Unlike animateToValue this does NOT press/release — it only
    // repositions the value and resets velocity.
    fun snapToValue(value: Float) {
        val clamped = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.snapTo(clamped) }
            launch { velocityAnimation.snapTo(0f) }
        }
    }

    // v292h — DIRECT DRAG: snaps the value instantly during a drag
    // gesture (no spring lag). The blob tracks the finger without
    // the animation delay that updateValue's spring introduces.
    fun setDragValue(value: Float) {
        val clamped = value.coerceIn(valueRange)
        animationScope.launch {
            valueAnimation.snapTo(clamped)
            updateVelocity()
        }
    }

    fun animateToValue(value: Float, spec: AnimationSpec<Float>? = null) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val clamped = value.coerceIn(valueRange)
                // v249 — callers can pass a softer spec: the default
                // critically-damped 1000-stiffness spring SNAPSHOTS between
                // tabs; programmatic switches deserve an iOS-style glide.
                launch { valueAnimation.animateTo(clamped, spec ?: valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(System.currentTimeMillis(), Offset(value, 0f))
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}

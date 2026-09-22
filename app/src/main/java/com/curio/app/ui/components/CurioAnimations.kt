package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioAlivePreferences
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.isAmbientMotionOn

@Composable
fun ScreenEntrance(content: @Composable () -> Unit) {
    val alive = CurioAlivePreferences.isEnabled(LocalContext.current)
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = if (alive) 360 else CurioMotion.Durations.Standard,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            animationSpec = if (alive) {
                spring<IntOffset>(dampingRatio = 0.86f, stiffness = 900f)
            } else {
                spring<IntOffset>(dampingRatio = 1f, stiffness = 750f)
            },
            initialOffsetY = { fullHeight -> if (alive) 12 else fullHeight / 8 }
        ),
        content = { content() }
    )
}

@Composable
fun CurioDialogEntrance(
    scale: Float = 0.96f,
    content: @Composable () -> Unit
) {
    val alive = CurioAlivePreferences.isEnabled(LocalContext.current)
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(animationSpec = tween(if (alive) 260 else CurioMotion.Durations.Standard)) +
                scaleIn(
                    initialScale = if (alive) minOf(scale, 0.94f) else scale,
                    animationSpec = if (alive) {
                        spring(dampingRatio = 0.78f, stiffness = 950f)
                    } else CurioMotion.Springs.Calm
                ),
        content = { content() }
    )
}

@Composable
fun MorphEntrance(
    bouncy: Boolean = true,
    content: @Composable () -> Unit
) {
    val alive = CurioAlivePreferences.isEnabled(LocalContext.current)
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = if (alive) 560 else CurioMotion.Durations.Reveal,
                easing = FastOutSlowInEasing
            )
        ) + scaleIn(
            initialScale = when {
                alive && bouncy -> 0.88f
                alive -> 0.93f
                bouncy -> 0.85f
                else -> 0.92f
            },
            animationSpec = when {
                alive && bouncy -> spring(dampingRatio = 0.66f, stiffness = 520f)
                alive -> spring(dampingRatio = 0.86f, stiffness = 850f)
                bouncy -> CurioMotion.Springs.Elastic
                else -> CurioMotion.Springs.Calm
            }
        ),
        content = { content() }
    )
}

@Composable
fun MorphingContainer(
    trigger: Any,
    modifier: Modifier = Modifier,
    animationSpec: androidx.compose.animation.core.SpringSpec<Float> = CurioMotion.Springs.Morph,
    content: @Composable () -> Unit
) {
    val alive = CurioAlivePreferences.isEnabled(LocalContext.current)
    val effectiveSpec = if (alive) {
        spring(dampingRatio = 0.8f, stiffness = 650f)
    } else animationSpec
    @Suppress("UnusedContentLambdaTargetStateParameter")
    androidx.compose.animation.AnimatedContent(
        targetState = trigger,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(if (alive) 360 else CurioMotion.Durations.Morph)) +
                    scaleIn(
                        initialScale = if (alive) 0.94f else 0.92f,
                        animationSpec = effectiveSpec
                    ) togetherWith
                    fadeOut(animationSpec = tween(if (alive) 180 else CurioMotion.Durations.Quick)) +
                    androidx.compose.animation.scaleOut(
                        targetScale = if (alive) 0.975f else 0.96f,
                        animationSpec = spring(
                            dampingRatio = if (alive) 0.88f else 0.95f,
                            stiffness = if (alive) 620f else 400f
                        )
                    )
        },
        label = "morph"
    ) { content() }
}

@Composable
fun rememberBreathingScale(
    active: Boolean = true,
    amplitude: Float = 0.03f
): Float {
    // v454 — an ambient clock: it exists to be looked at, so Lite mode parks
    // it at rest rather than paying for a frame it cannot be seen to earn.
    if (!active || !isAmbientMotionOn) return 1f
    val transition = rememberInfiniteTransition(label = "breathe")
    val scale by transition.animateFloat(
        initialValue = 1f - amplitude,
        targetValue = 1f + amplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CurioMotion.Durations.Breathe,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )
    return scale
}

@Composable
fun rememberShimmerBrush(
    shimmerColor: Color = Color.White.copy(alpha = 0.15f),
    baseColor: Color = Color.Transparent
): Brush {
    // v454 — Lite mode: no sweep at all (the surface keeps its base colour).
    if (!isAmbientMotionOn) return SolidColor(baseColor)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CurioMotion.Durations.Shimmer,
                easing = LinearEasing
            )
        ),
        label = "shimmerTranslate"
    )
    return Brush.horizontalGradient(
        colors = listOf(baseColor, shimmerColor, baseColor),
        startX = translateAnim * 1000f,
        endX = (translateAnim + 0.4f) * 1000f
    )
}

@Composable
fun rememberRotatingReveal(
    rotationPeriodMs: Int = 12000,
    pulseAmplitude: Pair<Float, Float> = 0.85f to 1.10f
): Pair<Float, Float> {
    // v454 — Lite mode: hold still at the loop's resting pose (no rotation,
    // unit scale) — the surface is unchanged, it simply is not spinning.
    if (!isAmbientMotionOn) return 0f to 1f
    val transition = rememberInfiniteTransition(label = "rotatingReveal")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationPeriodMs, easing = LinearEasing)
        ),
        label = "revealRot"
    )
    val pulse by transition.animateFloat(
        initialValue = pulseAmplitude.first,
        targetValue = pulseAmplitude.second,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "revealPulse"
    )
    return rotation to pulse
}

@Composable
fun rememberAnimatedScaleOnPress(
    pressed: Boolean,
    pressedScale: Float = 0.94f
): androidx.compose.runtime.State<Float> {
    val alive = CurioAlivePreferences.isEnabled(LocalContext.current)
    val target = if (pressed) {
        if (alive) minOf(pressedScale, 0.955f) else pressedScale
    } else 1f
    return animateFloatAsState(
        targetValue = target,
        animationSpec = if (alive) {
            spring(dampingRatio = 0.72f, stiffness = 1050f)
        } else CurioMotion.Springs.Press,
        label = "pressScale"
    )
}

@Composable
fun rememberPulseScale(active: Boolean): Float {
    if (!isAmbientMotionOn) return 1f
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    return if (active) scale else 1f
}

@Composable
fun LiveWaveform(
    modifier: Modifier = Modifier,
    color: Color,
    active: Boolean,
    barCount: Int = 36,
    level: Float = 0f
) {
    val levelState by rememberUpdatedState(level)
    val history = remember { FloatArray(barCount) { 0.08f } }
    var historyTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(active) {
        while (true) {
            val target = if (active) levelState else 0.08f
            if (active && barCount > 0) {
                for (i in 0 until barCount - 1) history[i] = history[i + 1]
                val front = history[barCount - 1]
                history[barCount - 1] =
                    (front + (target - front) * 0.65f).coerceIn(0.08f, 1f)
            } else {
                for (i in 0 until barCount) {
                    val current = history[i]
                    history[i] = (current + (0.08f - current) * 0.35f)
                        .coerceIn(0.06f, 0.2f)
                }
            }
            historyTick++
            kotlinx.coroutines.delay(70)
        }
    }

    val tick = historyTick
    Canvas(modifier = modifier) {
        val gap = 2f.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val amp = history[i]
            val h = size.height * amp
            drawRoundRect(
                color = color.copy(alpha = 0.9f),
                topLeft = Offset(i * (barWidth + gap), (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
        if (tick < 0) return@Canvas
    }
}

fun formatRecordingTime(seconds: Int): String {
    val mm = seconds / 60
    val ss = seconds % 60
    return "%d:%02d".format(mm, ss)
}

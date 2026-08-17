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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioMotion

// ═══════════════════════════════════════════════════════════════════════════
// Screen-level entrance animations
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Universal entrance wrapper — fades + slides any screen content up from
 * a small offset over a quick spring. Wraps the screen's main scrollable
 * content; the back-bar / top-bar should be rendered outside so the bar
 * stays anchored while the body slides in.
 *
 * Per Curio motion contract: "Everywhere else, keep transitions under 400ms
 * so the app never feels like it's making you wait to be delighted."
 */
@Composable
fun ScreenEntrance(content: @Composable () -> Unit) {
    // v7.94 — the entrance now starts on the FIRST composition frame: the
    // old `var visible by remember(false)` + LaunchedEffect flip left the
    // screen invisible for one frame before animating, which read as a
    // delayed blank flash on every navigation. MutableTransitionState with
    // targetState already true plays the enter transition immediately.
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    // v166 — the slide runs the CALM spring family (critically damped, the
    // same 750 stiffness as the nav pill) so pages lift in with zero
    // overshoot — the old 0.85 damping spring bounced slightly past the
    // target, one of the "violent page opening" feels.
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(animationSpec = tween(CurioMotion.Durations.Standard)) +
                slideInVertically(
                    animationSpec = CurioMotion.Springs.Calm,
                    initialOffsetY = { it / 8 }
                ),
        content = { content() }
    )
}

/**
 * v163 — smooth OPEN for raw [androidx.compose.ui.window.Dialog] windows.
 * M3 AlertDialogs and ModalBottomSheets already animate their entrance,
 * but bare `Dialog(...)` content pops in with NO animation (the full-screen
 * mood board, the floating quote editor). Fades the content in and scales
 * it up from [scale] (default 0.96) on a near-critical spring — a soft,
 * deliberate entrance instead of an instant cut-in. [scale] = 1f gives a
 * pure fade for full-screen canvases that shouldn't zoom.
 */
@Composable
fun CurioDialogEntrance(
    scale: Float = 0.96f,
    content: @Composable () -> Unit
) {
    // Same first-frame trick as ScreenEntrance: MutableTransitionState with
    // targetState already true plays the enter on the first composition
    // frame instead of leaving the dialog blank for one frame.
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    // v166 — critically damped (1.0) on the SAME 750 stiffness as the nav
    // pill family: zero overshoot, so the scale never pops past its target
    // (the old 0.9 damping spring could read as a violent bounce on open).
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(animationSpec = tween(CurioMotion.Durations.Standard)) +
                scaleIn(
                    initialScale = scale,
                    animationSpec = CurioMotion.Springs.Calm
                ),
        content = { content() }
    )
}

/**
 * Dramatic screen entrance — scale up from 0.85 + fade in, with an elastic
 * spring for that premium "morph into view" feel. Use for hero screens:
 * Topic Reveal, Spin landing, Splash → Home.
 *
 * [bouncy] = false swaps the elastic (underdamped, ~5% overshoot) spring for
 * a critically-damped one: dense GRID screens (the category pickers) use
 * this so the cards + their shadows never visibly overshoot — the overshoot
 * read as a brief "more elevated" shadow flash before settling.
 */
@Composable
fun MorphEntrance(
    bouncy: Boolean = true,
    content: @Composable () -> Unit
) {
    // v7.94 — same first-frame fix as ScreenEntrance: start the morph on
    // composition instead of one frame later.
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    // v166 — the non-bouncy path runs the new CALM spring (critically
    // damped, zero overshoot — the old Deliberate at 0.85 damping still
    // zoomed back ~1% and dragged past 700ms, reading violent) and starts
    // closer to full size (0.92 instead of 0.85, so the grid gently lifts
    // in instead of zooming 15%). The explicit bouncy path keeps its
    // dramatic Elastic spring + deeper 0.85 start.
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = CurioMotion.Durations.Reveal,
                easing = FastOutSlowInEasing
            )
        ) + scaleIn(
            initialScale = if (bouncy) 0.85f else 0.92f,
            animationSpec = if (bouncy) CurioMotion.Springs.Elastic
                else CurioMotion.Springs.Calm
        ),
        content = { content() }
    )
}

/**
 * Morphing container — smoothly crossfades + scales between two states.
 * When [trigger] changes, the old content scales down + fades out while
 * the new content scales up + fades in, creating a seamless morph effect.
 *
 * The [animationSpec] controls the spring feel — defaults to Morph spring
 * for an organic water-droplet feel.
 */
@Composable
fun MorphingContainer(
    trigger: Any,
    modifier: Modifier = Modifier,
    animationSpec: androidx.compose.animation.core.SpringSpec<Float> = CurioMotion.Springs.Morph,
    content: @Composable () -> Unit
) {
    @Suppress("UnusedContentLambdaTargetStateParameter")
    androidx.compose.animation.AnimatedContent(
        targetState = trigger,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(CurioMotion.Durations.Morph)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = animationSpec
                    ) togetherWith
                    fadeOut(animationSpec = tween(CurioMotion.Durations.Quick)) +
                    androidx.compose.animation.scaleOut(
                        targetScale = 0.96f,
                        animationSpec = spring(dampingRatio = 0.95f, stiffness = 400f)
                    )
        },
        label = "morph"
    ) { content() }
}

// ═══════════════════════════════════════════════════════════════════════════
// Ambient / breathing animations
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Breathing scale — a slow, gentle pulse that gives static elements a
 * "living" feel. Use for hero cards, decorative glyphs, and ambient
 * backgrounds that should feel alive rather than frozen.
 *
 * Returns a scale value between 0.97 and 1.03, cycling over ~3.2 seconds.
 *
 * @param active Whether to animate. When false, returns 1f.
 * @param amplitude Range of the pulse (default 0.03 for subtle, 0.06 for noticeable).
 */
@Composable
fun rememberBreathingScale(
    active: Boolean = true,
    amplitude: Float = 0.03f
): Float {
    if (!active) return 1f
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

/**
 * Shimmer brush — an animated linear gradient that sweeps left-to-right
 * across a surface, giving it a subtle \"light passing over\" effect.
 * Use on cards during loading, or as a premium ambient detail on hero
 * elements (subtle, low-alpha).
 *
 * Returns a [Brush] that animates continuously.
 *
 * @param shimmerColor The highlight color (typically white at low alpha).
 * @param baseColor The base surface color.
 */
@Composable
fun rememberShimmerBrush(
    shimmerColor: Color = Color.White.copy(alpha = 0.15f),
    baseColor: Color = Color.Transparent
): Brush {
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

/**
 * Rotating reveal — a decorative element that slowly rotates while gently
 * pulsing. Used by the Topic Reveal sparkle motif and other decorative
 * glyphs throughout the app.
 *
 * @param rotationPeriodMs Full rotation cycle in ms (default 12s).
 * @param pulseAmplitude Scale pulse range (default 0.85 to 1.10).
 */
@Composable
fun rememberRotatingReveal(
    rotationPeriodMs: Int = 12000,
    pulseAmplitude: Pair<Float, Float> = 0.85f to 1.10f
): Pair<Float, Float> {
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

// ═══════════════════════════════════════════════════════════════════════════
// Interactive micro-animations
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Animated scale on press — a simple interactive scale-down that springs
 * back. Wrap any clickable element for tactile feedback.
 *
 * Usage:
 * ```
 * val scale by rememberAnimatedScaleOnPress(pressed = isPressed)
 * Box(Modifier.scale(scale).clickable { isPressed = true })
 * ```
 *
 * @param pressed Whether the element is currently pressed.
 * @param pressedScale Target scale when pressed (default 0.94).
 */
@Composable
fun rememberAnimatedScaleOnPress(
    pressed: Boolean,
    pressedScale: Float = 0.94f
): androidx.compose.runtime.State<Float> {
    val target = if (pressed) pressedScale else 1f
    return animateFloatAsState(
        targetValue = target,
        animationSpec = CurioMotion.Springs.Press,
        label = "pressScale"
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// Pulsing + waveform animations (carried forward from v1)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Pulsing scale for any element that needs a \"live\" feel — used by the
 * Sound Bite mic ring while recording (Curio Sound Bite contract: \"Button morphs
 * into a pulsing ring while live\"). Returns 1f when inactive; when active,
 * pulses between 1.0 and ~1.18 over a 900ms cycle.
 */
@Composable
fun rememberPulseScale(active: Boolean): Float {
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

/**
 * Live waveform display — REAL microphone-driven visualizer drawn as N
 * rounded vertical bars. Used by Save/Capture Sound Bite format
 * (Curio Sound Bite contract) while recording, and as a quiet \"armed\" indicator
 * when not yet started.
 *
 * [level] is the live mic amplitude (0.0–1.0) polled from
 * [com.curio.app.features.capture.AudioRecorder.maxAmplitude]; each frame
 * the newest level is appended to a short history ring so the bars ripple
 * with a decaying tail, exactly like a real audio meter. When [active] is
 * false, a flat quiet bar row (no motion) suggests the controls are armed.
 */
@Composable
fun LiveWaveform(
    modifier: Modifier = Modifier,
    color: Color,
    active: Boolean,
    barCount: Int = 36,
    level: Float = 0f
) {
    val levelState by rememberUpdatedState(level)
    // Short history ring — the newest mic level slides in at the END (the
    // right edge of the bar row) and every bar shifts one slot toward the
    // start each frame, so the WHOLE wave ripples with a trailing tail,
    // exactly like a real audio meter. The old decay loop only ever moved
    // the last bar (the others multiplied toward the floor in a few frames
    // and sat frozen), which read as "just the last bar reacts".
    val history = remember { FloatArray(barCount) { 0.08f } }
    var historyTick by remember { mutableIntStateOf(0) }

    // Push a new level every frame while recording; when inactive, ease the
    // whole ring back to the quiet floor so the wave goes still within a
    // few frames of stop/pause (no long lingering tail).
    LaunchedEffect(active) {
        while (true) {
            val target = if (active) levelState else 0.08f
            if (active && barCount > 0) {
                // Ring-buffer shift: each bar inherits its right neighbour,
                // so the recent levels' shape is PRESERVED and visible as a
                // moving wave instead of decaying straight to the floor.
                for (i in 0 until barCount - 1) {
                    history[i] = history[i + 1]
                }
                // The newest level eases into the front bar (smoothed so a
                // single spike doesn't make the wave jump around).
                val front = history[barCount - 1]
                history[barCount - 1] =
                    (front + (target - front) * 0.65f).coerceIn(0.08f, 1f)
            } else {
                // Idle — settle every bar toward the quiet armed floor
                // quickly (the old fast-decay behavior), so pausing or
                // stopping visibly calms the meter right away.
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
        // Read the tick so the Canvas recomposes each new mic frame.
        if (tick < 0) return@Canvas
    }
}

/**
 * Formats an elapsed-seconds count as mm:ss for the Sound Bite timer
 * (Curio Sound Bite contract: \"running timer\").
 */
fun formatRecordingTime(seconds: Int): String {
    val mm = seconds / 60
    val ss = seconds % 60
    return "%d:%02d".format(mm, ss)
}

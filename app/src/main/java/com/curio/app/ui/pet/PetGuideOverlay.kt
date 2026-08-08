package com.curio.app.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioPet
import com.curio.app.data.QuestGuide
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlin.math.roundToInt

/**
 * The pet-GUIDED tour overlay (v8.15) — the First Journey walkthrough led by
 * the Curio pet itself. v8.22 redesign:
 *
 *  - **The window is the REAL button.** Each step names a landmark
 *    ([QuestGuide.Step.targetLandmark]) that the current screen registers
 *    with its true bounds ([PetLandmarks]); the pass-through window is drawn
 *    EXACTLY over that button, so the highlight is never a guess and the
 *    real button stays tappable through it. Steps without a landmark fall
 *    back to the old position-based zone.
 *  - **The dialog is the pet's speech bubble.** Instead of a detached pill,
 *    the title/message/dots/action live in a rounded bubble with a tail that
 *    points at the pet, sitting right beside it. Instructions wrap up to
 *    four lines — never cut at two.
 *  - A dim scrim covers everything else and blocks taps outside the window;
 *    the pet hops beside the window in its pointing pose, aiming a pulsing
 *    coral arrow into it.
 *
 * The pet + bubble never cover the window: they sit BELOW it when the target
 * sits high on the screen and ABOVE it when the target sits low.
 */
@Composable
fun PetGuideOverlay(
    title: String,
    message: String,
    stepIndex: Int,
    stepCount: Int,
    actionLabel: String,
    position: QuestGuide.Position,
    /** v8.22 — the current route prefix, so the step's landmark can be found. */
    screen: String? = null,
    /** v8.22 — the landmark id this step highlights (null = position fallback). */
    targetLandmark: String? = null,
    actionEnabled: Boolean = true,
    onClick: () -> Unit,
    onClose: (() -> Unit)? = null,
    skipLabel: String? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** The settings-family hero height — fallback TOP steps window below it. */
    heroTopOffset: Dp = 204.dp
) {
    val accent = CurioColors.CategoryCoral
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHpx = with(density) { maxHeight.toPx() }
        // v8.22 — the pass-through window over the step's REAL target: the
        // landmark's bounds (published via onGloballyPositioned, inflated a
        // touch so the whole control reads as highlighted). The landmark map
        // is snapshot state, so this recomposes the moment the screen
        // registers (or unregisters) it. No landmark → the position-based
        // fallback zone.
        val hole: Rect? = targetLandmark?.let { id ->
            PetLandmarks.forScreen(screen).firstOrNull { it.id == id }?.bounds
                ?.inflate(with(density) { 6.dp.toPx() })
        } ?: with(density) {
            when (position) {
            QuestGuide.Position.BOTTOM, QuestGuide.Position.LOWER -> {
                val holeW = (maxWidth * 0.82f).coerceAtMost(340.dp)
                val holeH = 170.dp
                val left = (maxWidth - holeW) / 2f
                val top = maxHeight - holeH - 16.dp
                Rect(
                    left = left.toPx(), top = top.toPx(),
                    right = (left + holeW).toPx(), bottom = (top + holeH).toPx()
                )
            }
            QuestGuide.Position.TOP -> {
                val holeW = (maxWidth * 0.86f).coerceAtMost(360.dp)
                val holeH = 140.dp
                val left = (maxWidth - holeW) / 2f
                val top = heroTopOffset + 8.dp
                Rect(
                    left = left.toPx(), top = top.toPx(),
                    right = (left + holeW).toPx(), bottom = (top + holeH).toPx()
                )
            }
            QuestGuide.Position.CENTER -> null
        }
        }

        // Soft pulsing ring around the window + the arrow's pulse — one
        // shared infinite transition so they breathe in sync.
        val pulse = rememberInfiniteTransition(label = "guidePulse")
        val ringPhase by pulse.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse
            ),
            label = "guideRing"
        )

        // ── Scrim with a pass-through hole ────────────────────────────────
        // Draws the dim over everything EXCEPT the window (EvenOdd path) and
        // a pulsing accent ring around it; consumes touches only OUTSIDE the
        // window, so taps inside reach the real button underneath.
        if (hole != null) {
            val ringInset = with(density) { 5.dp.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val path = Path().apply {
                            addRect(Rect(Offset.Zero, size))
                            addRoundRect(
                                RoundRect(
                                    left = hole.left, top = hole.top,
                                    right = hole.right, bottom = hole.bottom,
                                    radiusX = 22.dp.toPx(), radiusY = 22.dp.toPx()
                                )
                            )
                            fillType = PathFillType.EvenOdd
                        }
                        drawPath(path, Color.Black.copy(alpha = 0.44f))
                        drawRoundRect(
                            color = accent.copy(alpha = 0.42f + 0.38f * ringPhase),
                            topLeft = Offset(hole.left - ringInset, hole.top - ringInset),
                            size = Size(hole.width + ringInset * 2, hole.height + ringInset * 2),
                            cornerRadius = CornerRadius(24.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    .pointerInput(hole) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            // Only swallow taps outside the window — the
                            // target button inside stays fully interactive.
                            if (!hole.contains(down.position)) down.consume()
                        }
                    }
            )
        }

        // ── The pet + its speech bubble ───────────────────────────────────
        // The pet sits beside the window (never over it) and points at it;
        // the bubble is a real speech bubble with a tail aimed at the pet.
        val mood = if (actionEnabled) CurioPet.Mood.HAPPY else CurioPet.Mood.CURIOUS
        val pointing = hole != null
        // v8.22 — window high on the screen → pet + bubble below it; window
        // low → pet + bubble above it. Never covering the highlighted button.
        val holeCenterY = hole?.let { it.top + it.height / 2f }
        val holeInTopHalf = hole == null || (holeCenterY ?: 0f) <= maxHpx / 2f
        val gapPx = with(density) { 12.dp.toPx() }
        val anchorModifier = when {
            hole == null -> Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
            holeInTopHalf -> Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (hole!!.bottom + gapPx).roundToInt()) }
            else -> Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, -(maxHpx - hole!!.top + gapPx).roundToInt()) }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = anchorModifier
        ) {
            when {
                // CENTER (final step): no scrim, just the pet + bubble.
                hole == null -> {
                    GuideSpeechBubble(
                        title = title, message = message, stepIndex = stepIndex,
                        stepCount = stepCount, actionLabel = actionLabel,
                        actionEnabled = actionEnabled, tailDown = true,
                        onClick = onClick, onClose = onClose,
                        skipLabel = skipLabel, onSkip = onSkip
                    )
                    Spacer(Modifier.height(10.dp))
                    GuidePet(
                        mood = mood, pointing = false,
                        actionEnabled = actionEnabled, onClick = onClick, stepKey = stepIndex
                    )
                }
                // Window above: arrow up into it, pet below, bubble under pet.
                holeInTopHalf -> {
                    GuideArrow(name = CurioIcons.ArrowUpward, accent = accent, phase = ringPhase)
                    Spacer(Modifier.height(4.dp))
                    GuidePet(
                        mood = mood, pointing = true,
                        actionEnabled = actionEnabled, onClick = onClick, stepKey = stepIndex
                    )
                    Spacer(Modifier.height(8.dp))
                    GuideSpeechBubble(
                        title = title, message = message, stepIndex = stepIndex,
                        stepCount = stepCount, actionLabel = actionLabel,
                        actionEnabled = actionEnabled, tailDown = false,
                        onClick = onClick, onClose = onClose,
                        skipLabel = skipLabel, onSkip = onSkip
                    )
                }
                // Window below: bubble above the pet, pet pointing down into
                // the window, arrow between.
                else -> {
                    GuideSpeechBubble(
                        title = title, message = message, stepIndex = stepIndex,
                        stepCount = stepCount, actionLabel = actionLabel,
                        actionEnabled = actionEnabled, tailDown = true,
                        onClick = onClick, onClose = onClose,
                        skipLabel = skipLabel, onSkip = onSkip
                    )
                    Spacer(Modifier.height(8.dp))
                    GuidePet(
                        mood = mood, pointing = true,
                        actionEnabled = actionEnabled, onClick = onClick, stepKey = stepIndex
                    )
                    Spacer(Modifier.height(4.dp))
                    GuideArrow(name = CurioIcons.ArrowDownward, accent = accent, phase = ringPhase)
                }
            }
        }
    }
}

/**
 * The pet in its tour-guide pose — tappable to advance non-wait steps.
 * Hops in with a springy bounce on every step change so it reads as
 * "running over to the button" between steps.
 */
@Composable
private fun GuidePet(
    mood: CurioPet.Mood,
    pointing: Boolean,
    actionEnabled: Boolean,
    onClick: () -> Unit,
    stepKey: Int
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(stepKey) {
        appear.snapTo(0f)
        appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 340f))
    }
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                val t = appear.value
                scaleX = 0.55f + 0.45f * t
                scaleY = 0.55f + 0.45f * t
                alpha = t
                translationY = 10.dp.toPx() * (1f - t)
            }
            .clickable(enabled = actionEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CurioPetSprite(
            stage = CurioPet.currentStage(),
            mood = mood,
            spriteSize = 66.dp,
            pointing = pointing,
            watching = pointing,
            contentDescription = if (actionEnabled) "Curio the guide — tap to continue" else null
        )
    }
}

/**
 * v8.22 — the tour's speech bubble: the same visual language as the pet's
 * floating bubbles (rounded paper + a tail), carrying the title, a message
 * that wraps up to FOUR lines (the old pill cut at two), progress dots, the
 * action, an optional skip link and the close X. The tail points at the pet
 * ([tailDown] = bubble sits ABOVE the pet, so the tail hangs down to it).
 */
@Composable
private fun GuideSpeechBubble(
    title: String,
    message: String,
    stepIndex: Int,
    stepCount: Int,
    actionLabel: String,
    actionEnabled: Boolean,
    tailDown: Boolean,
    onClick: () -> Unit,
    onClose: (() -> Unit)?,
    skipLabel: String?,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val bubbleColor = MaterialTheme.colorScheme.surfaceContainerHigh
    // v8.24 — a happy confirmation bounce whenever the step advances. Wait
    // steps advance the moment the REAL action is done, so the bubble pops
    // and hops exactly on the "yay, done!" beat. The overshoot spring lands
    // past full size, then settles back.
    val pop = remember { Animatable(0f) }
    LaunchedEffect(stepIndex) {
        if (stepIndex > 0) {
            pop.snapTo(0f)
            pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 430.dp)
            .graphicsLayer {
                val t = pop.value
                alpha = t.coerceIn(0f, 1f)
                // Pops from small to full — the spring overshoots past 1,
                // so it lands with a little bounce.
                scaleX = 0.55f + 0.45f * t
                scaleY = 0.55f + 0.45f * t
                // Rises in from below, dipping back as it settles.
                translationY = (1f - t) * 12.dp.toPx()
            }
    ) {
        if (!tailDown) {
            BubbleTail(color = bubbleColor, pullUp = false)
        }
        Surface(
            onClick = { if (actionEnabled) onClick() },
            shape = RoundedCornerShape(20.dp),
            color = bubbleColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.widthIn(max = 430.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                // Title row + close.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CurioColors.CoralBlush),
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(CurioIcons.Flag, null, tint = Color.White, size = 17.dp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onClose != null) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onClose),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                name = CurioIcons.Close,
                                contentDescription = "Close guide",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // v8.22 — instructions wrap up to four lines instead of
                // being cut at two.
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GuideDots(stepIndex = stepIndex, stepCount = stepCount)
                    Spacer(Modifier.weight(1f))
                    if (skipLabel != null && onSkip != null) {
                        Text(
                            text = skipLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onSkip)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (actionEnabled) CurioColors.CoralBlush
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (actionEnabled) {
                        CurioIcon(
                            name = CurioIcons.ChevronRight,
                            contentDescription = actionLabel,
                            tint = CurioColors.CoralBlush,
                            size = 18.dp
                        )
                    }
                }
            }
        }
        if (tailDown) {
            BubbleTail(color = bubbleColor, pullUp = true)
        }
    }
}

/** The bubble's tail — a tiny rotated square wearing the bubble fill. */
@Composable
private fun BubbleTail(color: Color, pullUp: Boolean) {
    Box(
        modifier = Modifier
            .size(12.dp)
            // Slide half the diamond into the bubble edge so it reads as a
            // tail, not a detached gem.
            .offset(y = if (pullUp) (-6).dp else 6.dp)
            .rotate(45f)
            .background(color, RoundedCornerShape(2.dp))
    )
}

/** One dot per tour step — the current step filled, the rest hollow. */
@Composable
private fun GuideDots(stepIndex: Int, stepCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(stepCount.coerceAtLeast(1)) { i ->
            val isCurrent = i == stepIndex - 1
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) CurioColors.CoralBlush
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
            )
        }
    }
}

/** A small pulsing coral arrow aimed at the pass-through window. */
@Composable
private fun GuideArrow(name: String, accent: Color, phase: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer { this.alpha = 0.55f + 0.45f * phase }
            .clip(CircleShape)
            .background(accent),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(name = name, contentDescription = null, tint = Color.White, size = 15.dp)
    }
}

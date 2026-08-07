package com.curio.app.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioPet
import com.curio.app.data.QuestGuide
import com.curio.app.ui.components.QuestGuideToast
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * The pet-GUIDED tour overlay (v8.15) — replaces the plain pill for the
 * First Journey walkthrough. Instead of a floating toast, the Curio pet
 * itself walks to the action and POINTS at it:
 *
 *  - A dim SCRIM covers the whole screen and BLOCKS every other button; a
 *    pass-through window ("the hole") sits over the current step's target
 *    zone, so the real button there stays tappable and everything else is
 *    locked (spec §7.3 — the tour guides without a blocking modal, but it
 *    does keep the user on-task).
 *  - The PET (the real sprite, same stage/colors) hops beside the hole and
 *    wears its eager pointing pose ([CurioPetSprite] `pointing`), with a
 *    pulsing coral arrow aimed into the window and a soft pulsing ring
 *    around it — \"press THIS\".
 *  - Its speech card (reusing [QuestGuideToast] with no pointer arrow)
 *    carries the title, message, progress dots, action, skip and close.
 *    Tapping the card OR the pet advances on non-wait steps; wait steps
 *    advance only when the real action happens, and offer Skip.
 *
 * The pet + card never overlap the hole, so the target button is never
 * covered. [position] picks the window + pet placement: BOTTOM/LOWER target
 * the bottom action strip (Shuffle, the reveal dock, Save) with the pet
 * above pointing DOWN; TOP targets the band under the screen hero with the
 * pet below pointing UP; CENTER (final step) shows no scrim — just the pet
 * and its card, tap to Finish.
 */
@Composable
fun PetGuideOverlay(
    title: String,
    message: String,
    stepIndex: Int,
    stepCount: Int,
    actionLabel: String,
    position: QuestGuide.Position,
    actionEnabled: Boolean = true,
    onClick: () -> Unit,
    onClose: (() -> Unit)? = null,
    skipLabel: String? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** The settings-family hero height — TOP steps window below it. */
    heroTopOffset: Dp = 204.dp
) {
    val accent = CurioColors.CategoryCoral
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // The pass-through window over the step's target zone (null = the
        // centered final step — no blocking at all).
        val hole: Rect? = when (position) {
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
            val ringInset = 5.dp.toPx()
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

        // ── The pet + its speech card ────────────────────────────────────
        // The pet sits beside the window (never over it) and points at it;
        // the card is the bubble. Tapping either advances non-wait steps.
        val mood = if (actionEnabled) CurioPet.Mood.HAPPY else CurioPet.Mood.CURIOUS
        val pointing = hole != null
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when (position) {
                        QuestGuide.Position.BOTTOM, QuestGuide.Position.LOWER ->
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 212.dp)
                        QuestGuide.Position.TOP ->
                            Modifier.align(Alignment.TopCenter).padding(top = heroTopOffset + 164.dp)
                        QuestGuide.Position.CENTER ->
                            Modifier.align(Alignment.Center)
                    }
                )
        ) {
            when (position) {
                // TOP steps: the hole is ABOVE the pet — arrow up first, then
                // the pet, then the card below it.
                QuestGuide.Position.TOP -> {
                    GuideArrow(name = CurioIcons.ArrowUpward, accent = accent, phase = ringPhase)
                    Spacer(Modifier.height(4.dp))
                    GuidePet(
                        mood = mood, pointing = pointing,
                        actionEnabled = actionEnabled, onClick = onClick, stepKey = stepIndex
                    )
                    Spacer(Modifier.height(10.dp))
                    GuideCard(
                        title = title, message = message, stepIndex = stepIndex,
                        stepCount = stepCount, actionLabel = actionLabel,
                        actionEnabled = actionEnabled, onClick = onClick,
                        onClose = onClose, skipLabel = skipLabel, onSkip = onSkip
                    )
                }
                // BOTTOM/LOWER: the hole is BELOW the pet — card on top, then
                // the pet pointing down, then the arrow into the window.
                QuestGuide.Position.BOTTOM, QuestGuide.Position.LOWER -> {
                    GuideCard(
                        title = title, message = message, stepIndex = stepIndex,
                        stepCount = stepCount, actionLabel = actionLabel,
                        actionEnabled = actionEnabled, onClick = onClick,
                        onClose = onClose, skipLabel = skipLabel, onSkip = onSkip
                    )
                    Spacer(Modifier.height(10.dp))
                    GuidePet(
                        mood = mood, pointing = pointing,
                        actionEnabled = actionEnabled, onClick = onClick, stepKey = stepIndex
                    )
                    Spacer(Modifier.height(4.dp))
                    GuideArrow(name = CurioIcons.ArrowDownward, accent = accent, phase = ringPhase)
                }
                // CENTER (final step): no scrim, just the pet + card.
                QuestGuide.Position.CENTER -> {
                    GuideCard(
                        title = title, message = message, stepIndex = stepIndex,
                        stepCount = stepCount, actionLabel = actionLabel,
                        actionEnabled = actionEnabled, onClick = onClick,
                        onClose = onClose, skipLabel = skipLabel, onSkip = onSkip
                    )
                    Spacer(Modifier.height(10.dp))
                    GuidePet(
                        mood = mood, pointing = false,
                        actionEnabled = actionEnabled, onClick = onClick, stepKey = stepIndex
                    )
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

/** The speech card — reuses the guide toast (no pointer arrow; the pet points). */
@Composable
private fun GuideCard(
    title: String,
    message: String,
    stepIndex: Int,
    stepCount: Int,
    actionLabel: String,
    actionEnabled: Boolean,
    onClick: () -> Unit,
    onClose: (() -> Unit)?,
    skipLabel: String?,
    onSkip: (() -> Unit)?
) {
    QuestGuideToast(
        title = title,
        message = message,
        stepIndex = stepIndex,
        stepCount = stepCount,
        actionLabel = actionLabel,
        pointer = null,
        actionEnabled = actionEnabled,
        onClick = onClick,
        onClose = onClose,
        secondaryLabel = if (actionEnabled) null else skipLabel,
        onSecondary = if (actionEnabled) null else onSkip,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
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

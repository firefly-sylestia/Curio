package com.curio.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion

/**
 * The direction the quest guide's pointer arrow aims (v8.3) — the pill floats
 * near the screen's bottom or below the hero, and the arrow points at the
 * content it's describing. Null = no pointer (the centered final step).
 */
enum class GuidePointer { UP, DOWN }

/**
 * The quest guide's compact IN-APP OVERLAY (v8.3) — a small floating pill
 * that moves with the screen (see [GuidePointer] and the NavHost alignment),
 * not a system Toast, not a dialog. Carries a flag marker, a bold title, a
 * one-to-two line message, a PROGRESS-DOT indicator (one dot per step, the
 * current one filled), a Next / Finish action, and a pointer arrow aimed at
 * the content it describes. Tap the pill to advance the walkthrough; the
 * optional [onClose] X ends it.
 */
@Composable
fun QuestGuideToast(
    title: String,
    message: String,
    stepIndex: Int,
    stepCount: Int,
    actionLabel: String,
    pointer: GuidePointer? = GuidePointer.UP,
    /** v8.6 — false for real-action wait steps: the action is muted and taps
     *  on the pill do nothing (the step advances via the real action; the X
     *  still closes the tour). */
    actionEnabled: Boolean = true,
    onClick: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Gentle pop-in (fade + slight scale) so the pill doesn't slam in.
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, tween(CurioMotion.Durations.Quick, easing = FastOutSlowInEasing))
    }
    // The pointer arrow floats OUTSIDE the pill (above it when pointing up,
    // below it when pointing down) so it reads as an arrow aimed at content.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.graphicsLayer {
            val t = pop.value
            scaleX = 0.94f + 0.06f * t
            scaleY = 0.94f + 0.06f * t
            alpha = t
        }
    ) {
        if (pointer == GuidePointer.UP) GuidePointerArrow(CurioIcons.ArrowUpward)
        Surface(
            onClick = { if (actionEnabled) onClick() },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 10.dp,
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.widthIn(max = 430.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CurioColors.CoralBlush),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(CurioIcons.Flag, null, tint = Color.White, size = 20.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // v8.3 — progress dots under the message: one dot per
                    // step, the current one filled coral, the rest hollow.
                    Spacer(Modifier.height(6.dp))
                    GuideProgressDots(stepIndex = stepIndex, stepCount = stepCount)
                }
                Text(
                    actionLabel,
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
        }
        if (pointer == GuidePointer.DOWN) GuidePointerArrow(CurioIcons.ArrowDownward)
    }
}

/** One dot per tour step — the current step filled, the rest hollow. */
@Composable
private fun GuideProgressDots(stepIndex: Int, stepCount: Int) {
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

/** Small coral pointer arrow floating above/below the pill. */
@Composable
private fun GuidePointerArrow(name: String) {
    Box(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(CurioColors.CoralBlush),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(name = name, contentDescription = null, tint = Color.White, size = 15.dp)
    }
}

package com.curio.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v3xx50 — Curio's SKELETON system: quiet card-shaped placeholders that hold
 * the first frame while the archive is still being read, with a slow shimmer
 * sweep so the page reads as loading rather than broken.
 *
 * The point is the COUNT: the Cabinet knows how many saved entries it had on
 * its last visit (persisted), so it paints EXACTLY that many placeholders in
 * the SAME grid shape the real cards will land in — no flash of "Your
 * Cabinet is empty", no layout jump when the data arrives.
 */

/** The shimmer sweep: a soft highlight that travels left→right across the
 *  surface. The animated value is read in the DRAW phase (not during
 *  composition), so a sweeping skeleton never recomposes. */
@Composable
private fun Modifier.curioShimmerSweep(shape: RoundedCornerShape): Modifier {
    val transition = rememberInfiniteTransition(label = "curioShimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "curioShimmerProgress"
    )
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)
    return this
        .clip(shape)
        .drawBehind {
            val sweep = size.width * 0.9f
            val x = -sweep + (size.width + sweep * 2f) * progress.value
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    startX = x - sweep / 2f,
                    endX = x + sweep / 2f
                )
            )
        }
}

/** One entry-card placeholder — the same 20dp radius and ~96dp hero header
 *  as [CurioEntryCard], so the real cards land exactly where the skeleton
 *  was (no jump when the archive arrives). */
@Composable
fun CurioEntrySkeletonCard(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    val fill = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
    val bar = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(shape)
            .background(fill)
            .curioShimmerSweep(shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.30f))
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(bar)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(bar.copy(alpha = 0.7f))
            )
        }
    }
}

/**
 * The Cabinet's loading page: [count] entry placeholders laid out in the
 * SAME grid the real cards use (2 columns on phones, adaptive on wide
 * windows) with the grid's own paddings and gaps, so the page's shape never
 * changes when the data lands.
 *
 * [count] is the last known number of saved entries (persisted across
 * launches) — the user asked for the placeholder amount to match the real
 * archive. It is capped so a huge archive can't spend the first frame
 * composing hundreds of placeholders.
 */
@Composable
fun CabinetEntrySkeletonGrid(
    count: Int,
    topInset: Dp,
    wide: Boolean,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (wide) {
            ((maxWidth.value - 32f) / 188f).toInt().coerceAtLeast(2)
        } else 2
        val rows = (count + columns - 1) / columns
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = topInset),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(columns) { col ->
                        if (row * columns + col < count) {
                            CurioEntrySkeletonCard(modifier = Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

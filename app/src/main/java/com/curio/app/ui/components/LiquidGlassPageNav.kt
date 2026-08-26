package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcons

/**
 * v294 — REUSABLE PAGE NAV: circular prev/next + capsule page pill.
 *
 * Features:
 * - Circular 48dp floating pills for prev/next (drawer-menu style)
 * - Capsule pill for page number (primary color)
 * - Hold page pill to open bottom sheet page picker
 * - Scrollable page picker for large page counts (5-column grid)
 * - Smooth fade/scale animations on prev button visibility
 * - Liquid glass support via optional backdrop parameter
 *
 * Usage:
 * ```
 * LiquidGlassPageNav(
 *     currentPage = currentPage,
 *     totalPages = totalPages,
 *     onPageChange = { currentPage = it },
 *     visible = pageNavVisible,
 *     glassBackdrop = chipGlassBackdrop
 * )
 * ```
 */
@Composable
fun LiquidGlassPageNav(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    visible: Boolean = true,
    glassBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    var showPagePicker by remember { mutableStateOf(false) }

    val navAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250),
        label = "pageNavAlpha"
    )

    Row(
        modifier = modifier
            .graphicsLayer { alpha = navAlpha; translationY = (1f - navAlpha) * 20f },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button — circular pill
        AnimatedVisibility(
            visible = currentPage > 0,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.7f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.7f)
        ) {
            Surface(
                onClick = { onPageChange((currentPage - 1).coerceAtLeast(0)) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (glassBackdrop != null && isLiquidGlassPillsActive()) {
                            Modifier.liquidGlassCapsule(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                backdrop = glassBackdrop
                            )
                        } else Modifier
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CurioIcon(CurioIcons.ChevronLeft, null,
                        tint = MaterialTheme.colorScheme.onSurface, size = 22.dp)
                }
            }
        }

        // Page number capsule — hold to jump
        val pageInteraction = remember { MutableInteractionSource() }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp,
            modifier = Modifier
                .height(48.dp)
                .defaultMinSize(minWidth = 90.dp)
                .combinedClickable(
                    interactionSource = pageInteraction,
                    indication = null,
                    onClick = { /* tap — no-op, use arrows */ },
                    onLongClick = { showPagePicker = true }
                )
                .then(
                    if (glassBackdrop != null && isLiquidGlassPillsActive()) {
                        Modifier.liquidGlassCapsule(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            backdrop = glassBackdrop
                        )
                    } else Modifier
                )
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Next button — circular pill
        Surface(
            onClick = { onPageChange((currentPage + 1).coerceAtMost(totalPages - 1)) },
            shape = CircleShape,
            color = if (currentPage < totalPages - 1) MaterialTheme.colorScheme.surfaceContainerHigh
            else Color.Transparent,
            enabled = currentPage < totalPages - 1,
            shadowElevation = if (currentPage < totalPages - 1) 4.dp else 0.dp,
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (glassBackdrop != null && isLiquidGlassPillsActive()) {
                        Modifier.liquidGlassCapsule(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            backdrop = glassBackdrop
                        )
                    } else Modifier
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                CurioIcon(CurioIcons.ChevronRight, null,
                    tint = if (currentPage < totalPages - 1) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    size = 22.dp)
            }
        }
    }

    // Page picker bottom sheet — hold page pill to jump (scrollable)
    if (showPagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showPagePicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Jump to page", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text("${currentPage + 1} of $totalPages pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                val cols = 5
                LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.Fixed(cols),
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(totalPages) { pageNum ->
                        val isActive = pageNum == currentPage
                        Surface(
                            onClick = {
                                onPageChange(pageNum)
                                showPagePicker = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "${pageNum + 1}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

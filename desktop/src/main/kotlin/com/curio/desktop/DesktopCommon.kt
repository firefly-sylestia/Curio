package com.curio.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pill-shaped selectable chip — the shared building block of the desktop UI. */
@Composable
internal fun DesktopPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = when {
            !enabled -> colors.surfaceVariant
            active -> colors.primary
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(50),
        modifier = modifier.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> colors.onSurfaceVariant.copy(alpha = 0.6f)
                active -> colors.onPrimary
                else -> colors.onSurfaceVariant
            },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

/** Title + subtitle header used at the top of every screen. */
@Composable
internal fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
        Text(
            title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Horizontally scrollable row of all 36 lane chips. Tapping a lane selects
 * it, clears the landed topic and jumps to the Spin screen (Android Home
 * parity); on the Spin screen itself that jump is a no-op.
 */
@Composable
internal fun LaneChipsRow(modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(DesktopCatalog.categories) { cat ->
            DesktopPill(
                label = cat.displayName,
                active = shell.selectedSlug == cat.slug,
                onClick = {
                    shell.selectedSlug = cat.slug
                    shell.currentTopic = null
                    shell.screen = DesktopScreen.SPIN
                }
            )
        }
    }
}

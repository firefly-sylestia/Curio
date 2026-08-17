package com.curio.app.features.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioEntry
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/** v174d — the curiosity-stats time window. The drawer map's "This Week"
 *  selector and the stats page share [StatsRangeState], so picking a window
 *  anywhere filters the stats constellation's entry-based stats to it.
 *  [days] is null for the unfiltered All-Time view. */
enum class StatsRange(val label: String, val days: Long?) {
    WEEK("This Week", 7),
    MONTH("This Month", 30),
    ALL("All Time", null)
}

/** v174d — keep only entries captured inside [StatsRange]'s window. Shared
 *  by the stats page AND the drawer's curiosity map. */
fun List<CurioEntry>.filterForRange(range: StatsRange): List<CurioEntry> {
    val days = range.days ?: return this
    val cutoff = System.currentTimeMillis() - days * 24L * 3600 * 1000
    return filter { it.capturedAtMillis >= cutoff }
}

/** Process-wide holder (same pattern as CurioNavTint) — survives navigation
 *  so the drawer's selector and the stats screen stay in sync. */
object StatsRangeState {
    var selected by mutableStateOf(StatsRange.WEEK)
        private set

    fun select(range: StatsRange) {
        selected = range
    }
}

/** v174d — the shared "This Week ˅" selector pill: opens a small dropdown
 *  and writes the choice to [StatsRangeState]. Used on the drawer map AND
 *  the stats page's constellation card. */
@Composable
fun StatsRangeSelectorPill(modifier: Modifier = Modifier) {
    val range = StatsRangeState.selected
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { menuOpen = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(range.label, style = MaterialTheme.typography.labelSmall, color = muted)
                CurioIcon(CurioIcons.KeyboardArrowDown, null, tint = muted, size = 14.dp)
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            StatsRange.entries.forEach { r ->
                DropdownMenuItem(
                    text = {
                        Text(
                            r.label,
                            fontWeight = if (r == range) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        StatsRangeState.select(r)
                        menuOpen = false
                    }
                )
            }
        }
    }
}

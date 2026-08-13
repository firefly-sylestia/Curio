package com.curio.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Consecutive days (ending today or yesterday) with at least one save. */
private fun savedStreakDays(): Int {
    val days = DesktopEntryStore.entries
        .map { Instant.ofEpochMilli(it.savedAt).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toSortedSet()
    if (days.isEmpty()) return 0
    val today = LocalDate.now()
    val cursor = if (days.contains(today)) today else today.minusDays(1)
    if (!days.contains(cursor)) return 0
    var streak = 0
    var day = cursor
    while (days.contains(day)) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/** The desktop Home — rose hero with Streak · Cabinet · Topics, lane chips and a spin CTA. */
@Composable
internal fun DesktopHome() {
    val heroFill = if (shell.darkMode) RoseHeroDark else RoseHeroLight
    val heroInk = if (shell.darkMode) RoseHeroInkDark else RoseHeroInkLight
    val cabinetCount = DesktopEntryStore.entries.size
    val streak = remember(DesktopEntryStore.entries) { savedStreakDays() }
    // Total catalog size across all lanes — computed off the UI thread once.
    val topicsTotal by produceState(initialValue = 0) {
        value = withContext(Dispatchers.Default) {
            DesktopCatalog.categories
                .filter { it.slug != "wildcard" }
                .sumOf { DesktopCatalog.load(it.slug).size }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Rose hero banner (mirrors the Android Home quest hero) ──────
        Surface(
            color = heroFill,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 30.dp)) {
                Text(
                    "Curio",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = heroInk
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your curiosity, one spin at a time.",
                    fontSize = 14.sp,
                    color = heroInk.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeStatChip("$streak", "Streak", heroInk, Modifier.weight(1f))
                    HomeStatChip("$cabinetCount", "Cabinet", heroInk, Modifier.weight(1f))
                    HomeStatChip("$topicsTotal", "Topics", heroInk, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "Pick a lane",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
        Spacer(Modifier.height(12.dp))
        LaneChipsRow(Modifier.padding(horizontal = 28.dp))
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { shell.screen = DesktopScreen.SPIN },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(52.dp)
        ) {
            Text(
                "SPIN A LANE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        if (cabinetCount > 0) {
            Text(
                if (cabinetCount == 1) "1 discovery saved in your Cabinet — open it" else
                    "$cabinetCount discoveries saved in your Cabinet — open them",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { shell.screen = DesktopScreen.CABINET }
                    .padding(8.dp)
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HomeStatChip(value: String, label: String, ink: Color, modifier: Modifier = Modifier) {
    Surface(
        color = ink.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = ink
            )
            Text(
                label,
                fontSize = 11.sp,
                color = ink.copy(alpha = 0.8f)
            )
        }
    }
}

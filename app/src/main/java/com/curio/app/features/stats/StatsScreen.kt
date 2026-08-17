package com.curio.app.features.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.curio.app.R
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.StreakTracker
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.CurioBadgeMedal
import com.curio.app.ui.components.CurioConstellation
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.themedAccent

/** v174c — the Curiosity Stats page: the drawer's observatory world, full
 *  page. A celestial sky header, an INTERACTIVE constellation brain where
 *  every star is one of YOUR lanes (tap a star for that category's stats),
 *  the streak + level hero, lifetime totals, journey badges and a
 *  per-lane breakdown — everything from real app data. */
@Composable
fun StatsScreen(navController: NavController) {
    val context = LocalContext.current
    val streak = StreakTracker.getStreak(context)
    val bestStreak = CurioQuests.bestStreakState
    val xp = CurioQuests.xpState
    val level = CurioQuests.levelForXp(xp)
    val (levelProgress, nextThreshold) = CurioQuests.xpProgress(xp)
    val lifetime = CurioQuests.lifetimeState

    // v174d — the active time window (set from the drawer map's selector or
    // the pill on this card) filters the entry-based constellation stats.
    val range = StatsRangeState.selected
    var allEntries by remember { mutableStateOf<List<CurioEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        allEntries = runCatching { CurioRepositoryHolder.repo.getAll() }.getOrNull().orEmpty()
    }
    // Saved entries per lane INSIDE the window (+ recency for the glow).
    val filteredEntries = remember(allEntries, range) { allEntries.filterForRange(range) }
    val laneCounts = remember(filteredEntries) {
        filteredEntries.groupingBy { it.topic.categoryId }.eachCount()
    }
    val laneRecent = remember(filteredEntries) {
        filteredEntries.groupBy { it.topic.categoryId }
            .mapValues { (_, es) -> es.maxOf { it.capturedAtMillis } }
    }

    // Explored lanes = saved-entry lanes in the window ∪ (All Time only)
    // lanes the user has spun/quested — quest history has no timestamps.
    val knownNames = remember { CurioCategories.all.map { it.id.name }.toSet() }
    val explored = remember(laneCounts, range, CurioQuests.categoriesState) {
        val fromQuests = if (range == StatsRange.ALL) {
            CurioQuests.categoriesState
                .filter { it in knownNames }
                .mapNotNull { runCatching { CategoryId.valueOf(it) }.getOrNull() }
        } else emptyList()
        (laneCounts.keys + fromQuests).distinct().sortedBy { it.ordinal }
    }

    var selected by remember { mutableStateOf<CategoryId?>(null) }

    val (skyTop, skyBottom, skyInk) = statsSkyColors()
    Box(modifier = Modifier.fillMaxSize().background(heroPageBackground())) {
        CurioWatermarkBackdrop(
            activeCat = CurioCategories.byId(CategoryId.WILDCARD),
            modifier = Modifier.fillMaxSize(),
            alphaScale = 0.40f
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = StatsHeaderHeight + 14.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item("streakLevel") { StreakLevelCard(streak, bestStreak, level, levelProgress, nextThreshold, xp) }
            item("constellation") {
                StatsConstellationCard(
                    explored = explored,
                    laneCounts = laneCounts,
                    laneRecent = laneRecent,
                    selected = selected,
                    onSelect = { selected = it }
                )
            }
            item("lifetime") { LifetimeTotalsCard(lifetime) }
            item("badges") {
                JourneyCard(
                    level = level,
                    onOpenQuests = { navController.navigate(CurioRoutes.QUESTS) { launchSingleTop = true } }
                )
            }
            item("lanes") {
                LanesBreakdownCard(
                    explored = explored,
                    laneCounts = laneCounts,
                    onSelect = { selected = it }
                )
            }
            item { Spacer(Modifier.navigationBarsPadding().height(4.dp)) }
        }

        // ── Celestial header — a slice of the observatory sky ─────────────
        StatsSkyHeader(
            skyTop = skyTop,
            skyBottom = skyBottom,
            skyInk = skyInk,
            onBack = { navController.popBackStack() }
        )
    }
}

/** v174c — the stats page's celestial palette (mirrors the drawer's sky). */
@Composable
private fun statsSkyColors(): Triple<Color, Color, Color> {
    return if (isCurioDarkTheme()) {
        Triple(Color(0xFF12313A), Color(0xFF1D4750), Color(0xFFF4F1E7))
    } else {
        Triple(Color(0xFFC2E8DE), Color(0xFFE9F6F0), Color(0xFF2C5A53))
    }
}

private val StatsHeaderHeight = 148.dp

/** v178 — the fixed sky band: the SAME theme-picked sky artwork as the
 *  drawer hero (night sky in dark mode, day sky in light) behind the page
 *  title + back pill. The design (rounded tear, pill, ink) is unchanged —
 *  only the banner's art style changed. */
@Composable
private fun StatsSkyHeader(
    skyTop: Color,
    skyBottom: Color,
    skyInk: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // v178 — theme-picked drawer-hero sky SVG (dark → night, light → day).
    val heroSkyRes = if (isCurioDarkTheme()) R.raw.drawer_hero_sky_dark else R.raw.drawer_hero_sky_light
    val heroSkyModel = remember(context, heroSkyRes) {
        ImageRequest.Builder(context)
            .data(heroSkyRes)
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(true)
            .build()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StatsHeaderHeight)
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
    ) {
        // Theme gradient behind the art as the loading backdrop (same as the
        // drawer hero).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(skyTop, skyBottom)))
        )
        AsyncImage(
            model = heroSkyModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color(0xFFFFFDF4).copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(CurioIcons.ArrowBack, null, tint = skyInk, size = 22.dp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Your Curiosity",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = skyInk
                )
                Text(
                    "Stats, streaks & insights",
                    style = MaterialTheme.typography.bodyMedium,
                    color = skyInk.copy(alpha = 0.80f)
                )
            }
        }
    }
}

/** v174c — streak + level hero: current/best streak, and the level with its
 *  XP progress toward the next threshold. */
@Composable
private fun StreakLevelCard(
    streak: Int,
    bestStreak: Int,
    level: Int,
    levelProgress: Float,
    nextThreshold: Int,
    xp: Int
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    StatsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Streak pane
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                CurioIcon(CurioIcons.LocalFire, null, tint = Color(0xFFC96F4A), size = 30.dp)
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("$streak", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = ink)
                    Text(
                        "day streak · best $bestStreak",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            }
            // Level pane
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Level $level",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = ink
                    )
                    Text("$xp XP", style = MaterialTheme.typography.labelSmall, color = muted)
                }
                LinearProgressIndicator(
                    progress = { levelProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFD9A85C),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Text(
                    if (level >= CurioQuests.maxLevel) "Max level reached"
                    else "${nextThreshold - xp} XP to the next level",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted
                )
            }
        }
    }
}

/** v174c — the interactive constellation: every star is one of your lanes,
 *  sized by how much you've saved there (with a glow on recently active
 *  ones). Tap a star to see its stats. */
@Composable
private fun StatsConstellationCard(
    explored: List<CategoryId>,
    laneCounts: Map<CategoryId, Int>,
    laneRecent: Map<CategoryId, Long>,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val recentCutoff = remember { System.currentTimeMillis() - 7L * 24 * 3600 * 1000 }
    val totalSaves = laneCounts.values.sum()
    val selectedCat = selected?.let { CurioCategories.byId(it) }
    val selectedCount = selected?.let { laneCounts[it] ?: 0 } ?: 0
    val selectedRecent = selected?.let { laneRecent[it] ?: 0L } ?: 0L

    StatsCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Your Constellation",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = ink
                    )
                    Text(
                        "Every star is a lane you've explored — bigger means more saved ${StatsRangeState.selected.label.lowercase()}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
                // v174d — the live window selector (shared with the drawer map).
                StatsRangeSelectorPill()
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatsSummaryChip("$totalSaves", "saved", Color(0xFFB98A5E))
                StatsSummaryChip("${explored.size}", "lanes", Color(0xFF7FA0C8))
                StatsSummaryChip(CurioQuests.lifetimeState.spins.toString(), "spins", Color(0xFF9B7BB8))
            }
            CurioConstellation(
                explored = explored,
                laneCounts = laneCounts,
                laneRecent = laneRecent,
                recentCutoff = recentCutoff,
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth().height(230.dp)
            )
            AnimatedVisibility(
                visible = selectedCat != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedCat?.let { cat ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cat.themedAccent().copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            CurioIcon(cat.iconGlyph, null, tint = cat.themedAccent(), size = 20.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    cat.displayName,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ink
                                )
                                Text(
                                    if (selectedCount > 0) "$selectedCount saved${if (selectedRecent >= recentCutoff) " · active this week" else ""}"
                                    else "Explored, nothing saved yet",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = muted
                                )
                            }
                            // Dismiss chip — clears the selection.
                            Surface(
                                onClick = { onSelect(null) },
                                shape = CircleShape,
                                color = muted.copy(alpha = 0.10f),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    CurioIcon(CurioIcons.Close, null, tint = muted, size = 16.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** v174c — one tiny summary chip above the constellation. */
@Composable
private fun StatsSummaryChip(value: String, label: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** v174c — the interactive brain canvas: one glowing star per explored lane,
 *  arc-positioned into two hemisphere lobes (deterministic jitter per lane),
 *  sized by saved count, glowing when active this week. Tap a star to select
 *  it (tap empty space to clear). */
@Composable
/** v174c — the lifetime totals grid: spins, explores, saves, quotes, pins,
 *  likes, dislikes and daily quests, in compact paper panes. */
@Composable
private fun LifetimeTotalsCard(lifetime: CurioQuests.LifetimeCounters) {
    val items = listOf(
        LifetimeStat(CurioIcons.AutoAwesome, "Spins", lifetime.spins, Color(0xFF9B7BB8)),
        LifetimeStat("travel_explore", "Explores", lifetime.explores, CurioColors.CategorySky),
        LifetimeStat(CurioIcons.Bookmark, "Saved", lifetime.saves, Color(0xFFB98A5E)),
        LifetimeStat("format_quote", "Quotes", lifetime.quotes, Color(0xFF7FA0C8)),
        LifetimeStat("push_pin", "Pins", lifetime.pins, Color(0xFFC96F4A)),
        LifetimeStat(CurioIcons.ThumbUp, "Likes", lifetime.likes, Color(0xFFD9A85C)),
        LifetimeStat(CurioIcons.ThumbDown, "Dislikes", lifetime.dislikes, Color(0xFF8A8FA3)),
        LifetimeStat("task_alt", "Daily quests", lifetime.dailyCompleted, Color(0xFF7F9B6E))
    )
    StatsCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Lifetime totals",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Everything your curiosity has collected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        items.chunked(2).forEach { pairRow ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                pairRow.forEach { (icon, label, value, tint) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                        ) {
                            CurioIcon(icon, null, tint = tint, size = 18.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Text(
                                    "$value",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** v174c — one lifetime counter for the totals grid. */
private data class LifetimeStat(val icon: String, val label: String, val value: Int, val tint: Color)

/** v174c — journey stages: level chain progress + a row of earned medals
 *  (tap-through to Quests for the full shelf). */
@Composable
private fun JourneyCard(
    level: Int,
    onOpenQuests: () -> Unit
) {
    val allStages = remember { CurioQuests.allStages() }
    val unlocked = allStages.filter { CurioQuests.isStageDone(it) }
    val fraction = if (allStages.isEmpty()) 0f else unlocked.size.toFloat() / allStages.size
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    StatsCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Journey stages",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = ink
                    )
                    Text(
                        "${unlocked.size} of ${allStages.size} stages · ${CurioQuests.levelTitle(level)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
                Surface(
                    onClick = onOpenQuests,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "Quests",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        CurioIcon(CurioIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, size = 16.dp)
                    }
                }
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF9B7BB8),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            if (unlocked.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    unlocked.take(7).forEach { stage ->
                        CurioBadgeMedal(stage = stage, medalSize = 40.dp)
                    }
                    if (unlocked.size > 7) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(
                                "+${unlocked.size - 7}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = muted
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Earn your first medal on the Quests page — every stage you complete glows here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
        }
    }
}

/** v174c — per-lane breakdown: each explored lane as a row with its icon,
 *  name and saved count (tap selects it in the constellation above). */
@Composable
private fun LanesBreakdownCard(
    explored: List<CategoryId>,
    laneCounts: Map<CategoryId, Int>,
    onSelect: (CategoryId?) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    StatsCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Your lanes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = ink
            )
            Text(
                "Where your saved discoveries live",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }
        Spacer(Modifier.height(8.dp))
        if (explored.isEmpty()) {
            Text(
                "No lanes explored yet — spin a deck and save something to light this up.",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        } else {
            explored.forEach { id ->
                val cat = CurioCategories.byId(id)
                val accent = cat.themedAccent()
                val count = laneCounts[id] ?: 0
                Surface(
                    onClick = { onSelect(id) },
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = accent.copy(alpha = 0.14f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CurioIcon(cat.iconGlyph, null, tint = accent, size = 15.dp)
                            }
                        }
                        Text(
                            cat.displayName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = ink,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$count saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = muted
                        )
                    }
                }
            }
        }
    }
}

/** v174c — the shared pastel card shell for the stats page (opaque fill under
 *  the soft shadow — rule 11). */
@Composable
private fun StatsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isCurioDarkTheme())
            lerp(MaterialTheme.colorScheme.surface, Color(0xFF1B3A40), 0.55f)
        else
            lerp(MaterialTheme.colorScheme.surface, Color(0xFFCFE9E2), 0.45f),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

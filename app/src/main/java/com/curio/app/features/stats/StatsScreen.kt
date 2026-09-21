package com.curio.app.features.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import com.curio.app.data.AppPreferences
import com.curio.app.data.BrainDimension
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioPassport
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.LaneKnowledge
import com.curio.app.data.StreakTracker
import com.curio.app.data.brainProfile
import com.curio.app.data.laneKnowledge
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.PendingCabinetFilter
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.components.CurioBadgeMedal
import com.curio.app.ui.components.CurioGlassToolbar
import com.curio.app.ui.components.CurioLaneDetailStrip
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.LaneGridItem
import com.curio.app.ui.components.laneGridItems
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioAccentInk
import com.curio.app.ui.theme.curioCardShadow
import com.curio.app.ui.theme.curioTintOn
import com.curio.app.ui.theme.isCurioDarkTheme

/** v409 — the Curiosity Stats page, rebuilt around progress instead of prose.
 *
 *  What changed and why: the page carried six cards and a wall of explanatory
 *  sentences (a paragraph of science tip under each of six brain dimensions,
 *  a subtitle on every card, a second per-lane breakdown under a constellation
 *  that showed the same thing). The user's brief — "theres too much info. too
 *  much texts … lets redesign it beautifully" — is now: four cards, each one
 *  an instrument.
 *
 *   - PROGRESS   — streak, level + XP, journey stages and the medals, MERGED
 *                  from the old streak card and journey card (they were two
 *                  cards about the same number).
 *   - YOUR BRAIN — the six cognitive dimensions as meters, plus the single
 *                  tip that applies to the weakest one (it used to print six).
 *   - LANE STATS — the ranked bar list ([LaneStatsGraph]): knowledge per lane,
 *                  strongest first, cut to the top [LANE_BARS_SHOWN] so the
 *                  card's height never depends on how many lanes you've met.
 *   - LIFETIME   — the counters, with their captions trimmed to the number
 *                  itself.
 *
 *  The drawer keeps the painted constellation — the one place a map is
 *  actually a map. Here the same lanes are a chart.
 */
@Composable
fun StatsScreen(navController: NavController) {
    val context = LocalContext.current
    val streak = StreakTracker.getStreak(context)
    val bestStreak = CurioQuests.bestStreakState
    val xp = CurioQuests.xpState
    val level = CurioQuests.levelForXp(xp)
    val (levelProgress, nextThreshold) = CurioQuests.xpProgress(xp)
    val lifetime = CurioQuests.lifetimeState

    // The time window (StatsRangeSelectorPill) filters the knowledge scores
    // and the brain profile, so the whole page answers one question ("how am
    // I doing lately") instead of mixing windows together.
    val range = StatsRangeState.selected
    var allEntries by remember { mutableStateOf<List<CurioEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        allEntries = runCatching { CurioRepositoryHolder.repo.getAll() }.getOrNull().orEmpty()
    }
    val filteredEntries = remember(allEntries, range) { allEntries.filterForRange(range) }

    val progress = remember(context) { CurioPassport.allProgress(context) }
    val knowledge: Map<CategoryId, LaneKnowledge> =
        remember(progress, filteredEntries) { laneKnowledge(progress, filteredEntries) }
    // Derived in composition (not remembered) so hiding or reordering a lane
    // in Manage Categories re-lays the grid immediately.
    val lanes = laneGridItems(knowledge)
    val exploredCount = lanes.count { it.explored }
    val totalKnowledge = lanes.sumOf { it.knowledge }
    val dimensions = brainProfile(
        progress = progress,
        entries = filteredEntries,
        lifetime = lifetime,
        bestStreak = bestStreak,
        totalLanes = CurioCategories.visible.size
    )
    var selected by remember { mutableStateOf<CategoryId?>(null) }

    val (skyTop, skyBottom, skyInk) = statsSkyColors()
    Box(modifier = Modifier.fillMaxSize().background(heroPageBackground())) {
        CurioWatermarkBackdrop(
            activeCat = CurioCategories.byId(CategoryId.WILDCARD),
            modifier = Modifier.fillMaxSize(),
            alphaScale = 0.40f
        )
        // v3xx22 — the glass toolbar style swaps the celestial sky band for
        // the app-wide glass bar (title + subtitle, no back pill — the page
        // is a plain NavHost destination), reserving the bar's footprint so
        // the first card clears it.
        val statsGlass = AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (statsGlass) 150.dp else StatsHeaderHeight + 14.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item("progress") {
                ProgressCard(
                    streak = streak,
                    bestStreak = bestStreak,
                    level = level,
                    levelProgress = levelProgress,
                    nextThreshold = nextThreshold,
                    xp = xp,
                    onOpenQuests = {
                        navController.navigate(CurioRoutes.QUESTS) { launchSingleTop = true }
                    }
                )
            }
            item("brain") { BrainCard(dimensions) }
            item("map") {
                LaneMapCard(
                    lanes = lanes,
                    selected = selected,
                    onSelect = { selected = it },
                    exploredCount = exploredCount,
                    totalKnowledge = totalKnowledge,
                    spins = lifetime.spins,
                    onOpenLane = { id ->
                        // The Cabinet already knows how to open itself
                        // pre-filtered to one lane; that handoff is exactly
                        // what "where did I put the things from this lane"
                        // needs, so a lane tile is a real door, not a label.
                        PendingCabinetFilter.request(id)
                        navController.navigateToTab(CurioRoutes.CABINET)
                    }
                )
            }
            item("lifetime") { LifetimeTotalsCard(lifetime) }
            item { Spacer(Modifier.navigationBarsPadding().height(4.dp)) }
        }

        // ── Header — the glass toolbar (style on) or the celestial sky ────
        if (statsGlass) {
            CurioGlassToolbar(
                title = "Your Curiosity",
                subtitle = "Stats, streaks & insights"
            )
        } else {
            StatsSkyHeader(
                skyTop = skyTop,
                skyBottom = skyBottom,
                skyInk = skyInk
            )
        }
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
 *  title. The design (rounded tear, ink) is unchanged — only the banner's
 *  art style changed. v202 — the back pill is GONE: the page is a plain
 *  NavHost destination, so the system back gesture/button already pops it
 *  (user: "fix the back button in your curiosity page… just remove the
 *  back button"). */
@Composable
private fun StatsSkyHeader(
    skyTop: Color,
    skyBottom: Color,
    skyInk: Color
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
            modifier = Modifier
                .align(Alignment.BottomStart)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
        ) {
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

/**
 * v409 — PROGRESS: streak, level and the journey, in one card.
 *
 * The old streak/level card and the old "Journey stages" card were two
 * separate cards reporting the same number (your level) with two paragraphs
 * of course. Merged, the numbers sit together and the copy is down to the
 * few words a meter cannot say itself.
 */
@Composable
private fun ProgressCard(
    streak: Int,
    bestStreak: Int,
    level: Int,
    levelProgress: Float,
    nextThreshold: Int,
    xp: Int,
    onOpenQuests: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ember = Color(0xFFC96F4A)
    val gold = Color(0xFFD9A85C)
    val violet = Color(0xFF9B7BB8)
    val allStages = remember { CurioQuests.allStages() }
    // NOT remembered: [CurioQuests.isStageDone] is read live (a stage completed
    // on the Quests page must light its medal here without a reload).
    val unlocked = allStages.filter { CurioQuests.isStageDone(it) }
    val stageFraction = if (allStages.isEmpty()) 0f else unlocked.size.toFloat() / allStages.size

    StatsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                CurioIcon(CurioIcons.LocalFire, null, tint = ember, size = 28.dp)
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "$streak",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = ink
                    )
                    Text(
                        "day streak · best $bestStreak",
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            VerticalDivider(modifier = Modifier.height(40.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
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
                    color = gold,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    if (level >= CurioQuests.maxLevel) "Top level reached"
                    else "${nextThreshold - xp} XP to level ${level + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                LinearProgressIndicator(
                    progress = { stageFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = violet,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    "${unlocked.size} of ${allStages.size} stages",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted
                )
            }
            Spacer(Modifier.size(10.dp))
            StatsDoorChip(label = "Quests", onClick = onOpenQuests)
        }
        if (unlocked.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                unlocked.take(6).forEach { stage ->
                    CurioBadgeMedal(stage = stage, medalSize = 38.dp)
                }
                if (unlocked.size > 6) {
                    Text(
                        "+${unlocked.size - 6}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = muted,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * v409 — YOUR BRAIN: the six cognitive dimensions as meters.
 *
 * The old card printed a paragraph of science tip under each of the six. Six
 * paragraphs is not a stats page, it is an essay — so the card now shows the
 * meters and ONE tip, the one for the dimension that is actually behind.
 */
@Composable
private fun BrainCard(dimensions: List<BrainDimension>) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val dimensionColors = listOf(
        Color(0xFF7FA0C8), // knowledge — sky
        Color(0xFFB98A5E), // memory — keepsake
        Color(0xFFD9A85C), // expression — gold
        Color(0xFF9B7BB8), // focus — violet
        Color(0xFFC96F4A), // consistency — ember
        Color(0xFF7F9B6E)  // curiosity — moss
    )
    val weakest = dimensions.minByOrNull { it.score }
    StatsCard {
        Text(
            "Your brain",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = ink
        )
        Spacer(Modifier.height(12.dp))
        dimensions.forEachIndexed { i, d ->
            val tint = dimensionColors[i % dimensionColors.size]
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurioIcon(d.icon, null, tint = tint, size = 16.dp)
                    Spacer(Modifier.size(7.dp))
                    Text(
                        d.name,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        d.level,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint
                    )
                }
                LinearProgressIndicator(
                    progress = { d.score / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = tint,
                    trackColor = tint.copy(alpha = 0.14f)
                )
            }
            if (i != dimensions.lastIndex) Spacer(Modifier.height(11.dp))
        }
        if (weakest != null) {
            Spacer(Modifier.height(13.dp))
            Text(
                "${weakest.name}: ${weakest.tip}",
                style = MaterialTheme.typography.labelSmall,
                color = muted
            )
        }
    }
}

/**
 * v413 — LANE STATS: the ranked bar chart of where your knowledge actually is.
 *
 * The interactive lane grid it replaces was a *navigation* surface wearing a
 * statistics label: 24 tiles, a reveal strip and a door on every one of them,
 * which is more furniture than this card can carry and still be read at a
 * glance. A statistics page wants the shape of the data, so this is a plain
 * ranked bar list — knowledge per lane, longest bar first, one line each.
 *
 * Nothing became unreachable: a row is still a tap (it selects), and the one
 * lane you land on gets the readout strip and the Cabinet door underneath it,
 * so the card names a lane once instead of 24 times.
 */
@Composable
private fun LaneMapCard(
    lanes: List<LaneGridItem>,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit,
    exploredCount: Int,
    totalKnowledge: Int,
    spins: Int,
    onOpenLane: (CategoryId) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    StatsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Lane stats",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = ink,
                modifier = Modifier.weight(1f)
            )
            StatsRangeSelectorPill()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "$totalKnowledge knowledge · $exploredCount of ${lanes.size} lanes · $spins spins",
            style = MaterialTheme.typography.labelSmall,
            color = muted
        )
        Spacer(Modifier.height(12.dp))
        LaneStatsGraph(lanes = lanes, selected = selected, onSelect = onSelect)
        // The readout and the Cabinet door belong to the ONE lane you tapped,
        // not to every row — that is the whole difference from the grid.
        selectedLane(lanes, selected)?.let { item ->
            Spacer(Modifier.height(12.dp))
            CurioLaneDetailStrip(
                item = item,
                action = {
                    StatsDoorChip(
                        label = "Cabinet",
                        accent = item.accent,
                        onClick = { onOpenLane(item.id) }
                    )
                }
            )
        }
    }
}

/** How many lanes the compact graph prints before the rest stay in the Cabinet. */
private const val LANE_BARS_SHOWN = 7

/**
 * The ranked horizontal bar list: name, a bar scaled against the strongest
 * lane, and the count. One row per lane, strongest first — cutting the tail at
 * [LANE_BARS_SHOWN] so the card keeps a stable height no matter how many lanes
 * a reader has touched (a 36-row bar chart is a list, not a chart).
 */
@Composable
private fun LaneStatsGraph(
    lanes: List<LaneGridItem>,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ranked = remember(lanes) { lanes.sortedByDescending { it.knowledge } }
    val shown = ranked.take(LANE_BARS_SHOWN)
    // Bars are relative to the strongest lane, never to 100: the chart reads as
    // "where is it concentrated" the moment there is data at all.
    val strongest = shown.firstOrNull()?.knowledge?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        shown.forEach { item ->
            val on = item.id == selected
            val lane = if (item.knowledge > 0) {
                item.knowledge.toFloat() / strongest
            } else {
                0f
            }
            val grown by animateFloatAsState(
                targetValue = lane,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                label = "laneBar"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (on) {
                            lerp(MaterialTheme.colorScheme.surfaceContainerHigh, item.accent, 0.16f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onSelect(if (on) null else item.id) }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CurioIcon(name = item.icon, contentDescription = null, tint = item.accent, size = 17.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (item.knowledge > 0) "${item.knowledge}" else "—",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (item.explored) item.accent else muted
                        )
                    }
                    // One track, one fill — the only chart furniture on the card.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(lerp(MaterialTheme.colorScheme.surfaceContainerHigh, item.accent, 0.12f))
                    ) {
                        if (grown > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(grown.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(item.accent)
                            )
                        }
                    }
                }
            }
        }
        // The tail, counted rather than shown: the Cabinet holds them all.
        val rest = ranked.size - shown.size
        if (rest > 0) {
            Text(
                "+$rest more lanes in your Cabinet",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 2.dp)
            )
        }
    }
}

/** The lane behind an id, or null when nothing is picked. */
private fun selectedLane(lanes: List<LaneGridItem>, selected: CategoryId?): LaneGridItem? =
    selected?.let { id -> lanes.firstOrNull { it.id == id } }

/** The small "Quests ›" / "Cabinet ›" pill: one destination, one door. */
@Composable
private fun StatsDoorChip(
    label: String,
    onClick: () -> Unit,
    accent: Color? = null
) {
    // v426 — the accent as INK, not as fill: a named theme's `primary` is its
    // deep hero tone and vanished on its own dark page (see [curioAccentInk]).
    val tint = accent ?: curioAccentInk()
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    // v411 — the chip is SOLID (the accent mixed into the surface) and wears
    // NO card border.
    val fill = if (accent != null) curioTintOn(base, tint, 0.16f) else base
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = fill,
        // v411 — no hairline: a soft shadow lifts the chip instead.
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = if (accent != null) accent else MaterialTheme.colorScheme.onSurface
            )
            CurioIcon(
                CurioIcons.ChevronRight,
                null,
                tint = if (accent != null) accent.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 15.dp
            )
        }
    }
}

/**
 * v409 — the lifetime counters, compact: one pane per counter (icon, number,
 * one-word label) on the card's own surface. The old grid nested seven tinted
 * sub-cards inside the card and captioned the card twice.
 */
@Composable
private fun LifetimeTotalsCard(lifetime: CurioQuests.LifetimeCounters) {
    val items = listOf(
        LifetimeStat(CurioIcons.AutoAwesome, "Spins", lifetime.spins, Color(0xFF9B7BB8)),
        LifetimeStat("travel_explore", "Explores", lifetime.explores, CurioColors.CategorySky),
        LifetimeStat(CurioIcons.Bookmark, "Saved", lifetime.saves, Color(0xFFB98A5E)),
        LifetimeStat("format_quote", "Quotes", lifetime.quotes, Color(0xFF7FA0C8)),
        LifetimeStat("push_pin", "Pins", lifetime.pins, Color(0xFFC96F4A)),
        LifetimeStat(CurioIcons.Star, "Favorites", lifetime.likes, Color(0xFFD9A85C)),
        LifetimeStat("task_alt", "Daily quests", lifetime.dailyCompleted, Color(0xFF7F9B6E))
    )
    StatsCard {
        Text(
            "Lifetime",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        items.chunked(4).forEachIndexed { rowIndex, rowItems ->
            if (rowIndex > 0) Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CurioIcon(stat.icon, null, tint = stat.tint, size = 17.dp)
                        Text(
                            "${stat.value}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stat.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** v174c — one lifetime counter for the totals grid. */
private data class LifetimeStat(val icon: String, val label: String, val value: Int, val tint: Color)

/**
 * v409 — the shared stats card: a WHITE card on the cream page (the app-wide
 * card ladder — see the theme's CARD LADDER note), with the hairline edge that
 * separates it from the page. The old shell lerped the fill toward a seafoam
 * tint, which on the tinted page background was one of the cards that blended.
 */
@Composable
private fun StatsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        // v411 — no hairline; the soft shadow is the card's edge now.
        modifier = Modifier
            .fillMaxWidth()
            .curioCardShadow(RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

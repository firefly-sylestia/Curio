package com.curio.app.features.topichistory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.PinnedTopic
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioCategoryChip
import com.curio.app.ui.components.CurioEmptyState
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk

/**
 * Topic History — see Curio topic-history contract.
 *
 * Lists every topic the user has ever spun, grouped by day ("Today" /
 * "Yesterday" / "This week" / "Earlier" headers). Each row shows the topic
 * name + the category accent + a small format glyph + relative time.
 *
 * v8.4x redesign:
 *  - Header matches the shared push-screen style (ExtraBold title + a muted
 *    subtitle, like Recents/Profile).
 *  - A search field + category filter chips narrow the list live (the same
 *    search-field visual language as the Settings hub).
 *  - Day-group headers show their entry count, spacing is breathing-roomier,
 *    and an empty filter result gets its own "No matches — Clear filters"
 *    state.
 *
 * Backed by saved capture persistence so the history reflects real topics
 * the user has captured instead of placeholder samples.
 */
@Composable
fun TopicHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    // v6.7 — pinned-for-later topics from the Topic Reveal screen, listed
    // above the day-grouped capture history so the user can revisit them.
    val pinnedTopics = AppPreferences.pinnedTopicsState
    val entriesState = produceState<List<HistoryEntry>>(initialValue = emptyList()) {
        try {
            CurioRepositoryHolder.repo.observeAll().collect { savedEntries ->
                value = savedEntries.map { it.toHistoryEntry() }
            }
        } catch (_: Exception) {
            value = emptyList()
        }
    }
    val entries = entriesState.value

    // ── Search & filter (v8.4x) ─────────────────────────────────────────
    var query by rememberSaveable { mutableStateOf("") }
    var filterCategoryId by rememberSaveable { mutableStateOf<CategoryId?>(null) }
    val needle = query.trim()
    val hasFilter = needle.isNotEmpty() || filterCategoryId != null

    fun matches(categoryId: CategoryId, topicName: String): Boolean {
        val byCategory = filterCategoryId == null || filterCategoryId == categoryId
        val byQuery = needle.isEmpty() ||
            topicName.contains(needle, ignoreCase = true) ||
            // The search also matches the category's display name, so typing
            // "films" surfaces every film you've explored.
            CurioCategories.byId(categoryId).displayName.contains(needle, ignoreCase = true)
        return byCategory && byQuery
    }

    val filteredPinned = remember(pinnedTopics, needle, filterCategoryId) {
        pinnedTopics.filter { matches(it.categoryId, it.topicName) }
    }
    val filteredEntries = remember(entries, needle, filterCategoryId) {
        entries.filter { matches(it.categoryId, it.topicName) }
    }
    val grouped = remember(filteredEntries) { filteredEntries.groupBy { it.dayLabel } }
    // Chips only for categories that actually appear in the history.
    val availableCats = remember(entries, pinnedTopics) {
        (entries.map { it.categoryId } + pinnedTopics.map { it.categoryId })
            .distinct()
            .map { CurioCategories.byId(it) }
    }
    // v5.8 — saveable-backed: the history list keeps its scroll position
    // across rotation and nav-away/back.
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── Top bar — matches the shared push-screen header style ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CurioBackButton(onClick = { navController.popBackStack() })
            Column {
                Text(
                    text = "Topic history",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Every topic you've explored, grouped by day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (entries.isEmpty() && pinnedTopics.isEmpty()) {
            CurioEmptyState(
                glyph = CurioIcons.History,
                headline = "No shuffles yet",
                subtext = "Shuffle the deck and your picks will appear here, grouped by day.",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            return
        }

        // ── Search + category filter ────────────────────────────────────
        CurioSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search your history",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
        )
        if (availableCats.size > 1) {
            HistoryCategoryFilterRow(
                categories = availableCats,
                selected = filterCategoryId,
                onSelect = { filterCategoryId = it },
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
            )
        }

        if (filteredEntries.isEmpty() && filteredPinned.isEmpty()) {
            // Filters narrowed everything away — offer a one-tap reset.
            // (Same ScreenEntrance fade-up as the list, so the whole results
            // region shares one entrance language.)
            ScreenEntrance {
                val filterCatName = filterCategoryId?.let { CurioCategories.byId(it).displayName }
                CurioEmptyState(
                    glyph = CurioIcons.Search,
                    headline = "No matches",
                    subtext = when {
                        needle.isNotEmpty() && filterCatName != null ->
                            "Nothing in your $filterCatName history matches \"$needle\"."
                        needle.isNotEmpty() ->
                            "Nothing in your history matches \"$needle\"."
                        filterCatName != null ->
                            "No $filterCatName topics in your history yet."
                        else -> "Nothing in your history yet."
                    },
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    ctaLabel = "Clear filters",
                    onCtaClick = {
                        query = ""
                        filterCategoryId = null
                    }
                )
            }
        } else {
            ScreenEntrance {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = wideContentEdgePadding(),
                        vertical = 10.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ── Pinned for later (pin button on Topic Reveal) ────
                    if (filteredPinned.isNotEmpty()) {
                        item(key = "pinned_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurioIcon(
                                    CurioIcons.Bookmark, null,
                                    tint = CurioColors.CoralBlush,
                                    size = 16.dp
                                )
                                Text(
                                    text = "Pinned for later",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(filteredPinned, key = { "pin_${it.categoryId.name}_${it.topicName}" }) { pinned ->
                            PinnedRow(
                                pinned = pinned,
                                onClick = {
                                    navController.navigate(
                                        com.curio.app.navigation.CurioRoutes.revealFor(
                                            pinned.categoryId.routeSlug,
                                            pinned.topicName
                                        )
                                    ) { launchSingleTop = true }
                                },
                                onUnpin = {
                                    AppPreferences.unpinTopic(context, pinned.categoryId, pinned.topicName)
                                }
                            )
                        }
                        if (filteredEntries.isNotEmpty()) {
                            item(key = "pinned_divider") {
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }

                    grouped.forEach { (dayLabel: String, dayEntries: List<HistoryEntry>) ->
                        item(key = "header_$dayLabel") {
                            Text(
                                text = "$dayLabel · ${dayEntries.size}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(dayEntries, key = { historyEntry: HistoryEntry -> historyEntry.id }) { entry ->
                            HistoryRow(
                                entry = entry,
                                onClick = {
                                    navController.navigate(
                                        com.curio.app.navigation.CurioRoutes.revealFor(
                                            entry.categoryId.routeSlug,
                                            entry.topicName
                                        )
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                }
                // Side scroll indicator — thin overlay knob, grows on touch.
                CurioVerticalScrollIndicator(
                    state = listState.scrollIndicatorState,
                    onScrollBy = { listState.scrollBy(it) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )
                }
            }
        }
    }
}

/** Horizontally scrolling category filter — single-select; tapping the
 *  active chip clears the filter. Only categories present in the history
 *  are offered (no dead chips). */
@Composable
private fun HistoryCategoryFilterRow(
    categories: List<CurioCategory>,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            CurioCategoryChip(
                category = cat,
                selected = selected == cat.id,
                onClick = { onSelect(if (selected == cat.id) null else cat.id) }
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val cat = CurioCategories.byId(entry.categoryId)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Category accent dot ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(cat.tint, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = cat.iconGlyph,
                    contentDescription = null,
                    tint = cat.categoryInk(),
                    size = 22.dp
                )
            }

            Spacer(Modifier.size(12.dp))

            // ── Topic name + format glyph + category ──────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.topicName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = cat.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = cat.categoryInk()
                )
            }

            // ── Relative time + format glyph ──────────────────────────────
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = entry.relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CurioIcon(
                    name = entry.formatGlyph,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 16.dp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}


// ── Pinned-for-later row — bookmark glyph + category accent dot ───────────

@Composable
private fun PinnedRow(pinned: PinnedTopic, onClick: () -> Unit, onUnpin: () -> Unit) {
    val cat = CurioCategories.byId(pinned.categoryId)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Category accent dot with filled bookmark ────────────────
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(cat.tint, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.Bookmark,
                    contentDescription = null,
                    tint = cat.categoryInk(),
                    size = 20.dp
                )
            }

            Spacer(Modifier.size(12.dp))

            // ── Topic name + category ───────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pinned.topicName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = cat.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = cat.categoryInk()
                )
            }

            // ── Unpin affordance ────────────────────────────────────────
            Surface(
                onClick = onUnpin,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                CurioIcon(
                    CurioIcons.BookmarkBorder, "Unpin",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 16.dp,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}


// ── Mock data for the placeholder phase ────────────────────────────────────
private data class HistoryEntry(
    val id: String,
    val topicName: String,
    val categoryId: CategoryId,
    val relativeTime: String,
    val dayLabel: String,
    val formatGlyph: String
)

private fun CurioEntry.toHistoryEntry(): HistoryEntry = HistoryEntry(
    id = id,
    topicName = topic.name,
    categoryId = topic.categoryId,
    relativeTime = relativeTime(capturedAtMillis),
    dayLabel = dayLabel(capturedAtMillis),
    formatGlyph = format.toGlyph()
)

private fun CaptureFormat.toGlyph(): String = when (this) {
    CaptureFormat.SoundBite -> CurioIcons.Mic
    CaptureFormat.ReelNotes -> CurioIcons.Movie
    CaptureFormat.Marginalia -> CurioIcons.MenuBook
    CaptureFormat.GalleryWall -> CurioIcons.Image
    CaptureFormat.FieldNotes -> CurioIcons.Science
    CaptureFormat.OpenNotebook -> CurioIcons.Edit
}

private fun dayLabel(timestamp: Long): String {
    val elapsed = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val days = elapsed / (24L * 60L * 60L * 1000L)
    return when (days) {
        0L -> "Today"
        1L -> "Yesterday"
        in 2L..6L -> "This week"
        else -> "Earlier"
    }
}

private fun relativeTime(timestamp: Long): String {
    val elapsedMinutes = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / 60_000L)
    return when {
        elapsedMinutes < 1L -> "just now"
        elapsedMinutes < 60L -> "${elapsedMinutes} min ago"
        elapsedMinutes < 24L * 60L -> "${elapsedMinutes / 60L} hr ago"
        elapsedMinutes < 48L * 60L -> "yesterday"
        else -> "${elapsedMinutes / (24L * 60L)} days ago"
    }
}

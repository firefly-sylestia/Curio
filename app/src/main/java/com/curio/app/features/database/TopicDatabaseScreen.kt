package com.curio.app.features.database

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.TopicJsonLoader
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Browse Topics — the whole Curio database in one place.
 *
 * Opened from the Home drawer ("Browse Topics"). Renders every topic across
 * the ten real categories (wildcard is a merge, so it isn't a separate lane)
 * with a search bar, per-category filter chips, a small "explored" badge on
 * topics already marked done, and sort controls for A–Z / Z–A / newest /
 * oldest by year. Tapping any topic opens its full Topic Reveal page, exactly like
 * spinning it.
 *
 * Sorting reads each topic's year from its name ("Citizen Kane (1941)"),
 * its explore target ("Vespertine (2001) end-to-end"), its teaser, or a
 * decade tag ("1960s" → 1960) — whatever is available first. Topics with no
 * recoverable year sort last in year order.
 *
 * Sits in the settings family: torn rose hero on a muted watermark
 * backdrop, content scrolling under the ragged tear, ScreenEntrance
 * entrance animation.
 */
@Composable
fun TopicDatabaseScreen(navController: NavController) {
    // v7.97 — SAVEABLE state: the search query, the selected category filter
    // and the scroll position survive leaving the screen (Topic Reveal
    // round-trips, tab switches, rotation, process death) instead of
    // resetting to a fresh blank list every time you come back.
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCat by rememberSaveable { mutableStateOf<CategoryId?>(null) }
    // v8.54 — sort control: DEFAULT (category file order) / A–Z / Z–A /
    // YEAR_NEWEST / YEAR_OLDEST. Saved like the search + filter so it
    // survives reveal round-trips, tab switches, and rotation.
    var sortMode by rememberSaveable { mutableStateOf(DatabaseSortMode.DEFAULT) }
    // v7.98 — the scroll position is saved EXPLICITLY (index + offset), not
    // via LazyListState.Saver: the catalog loads asynchronously, so on return
    // the list first composes with zero rows and a restored LazyListState gets
    // clamped to 0 before the topics arrive — scrolling back to the top.
    // Saving the raw numbers and scrolling again once rows exist restores the
    // exact spot reliably.
    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    // Reactive done-set — reading the value registers the dependency so the
    // list refreshes when the user marks a topic done (e.g. after returning
    // from a Topic Reveal) or a session records an exploration.
    val doneTopics = ExploreSessionStore.doneTopicsState

    // Load + cache every category pool once; carry the topics alongside so
    // the filtered list stays a pure derivation. v7.94 — keyed on the
    // Manage Categories state so hidden/reordered lanes refresh on revisit.
    // Cache-first initialization matters here: returning to the browser should
    // render the already-parsed catalog immediately instead of flashing the
    // loading state for one composition frame.
    val visibleCategories = CurioCategories.visible
        .filter { it.id != CategoryId.WILDCARD }
    val cachedCatalog = remember(visibleCategories) {
        visibleCategories.mapNotNull { cat ->
            TopicJsonLoader.cached(cat.id)?.let { topics -> cat to topics }
        }
    }
    val catalogState by produceState<CatalogState>(
        initialValue = CatalogState(
            entries = cachedCatalog,
            loading = cachedCatalog.size < visibleCategories.size
        ),
        AppPreferences.hiddenCategoriesState,
        AppPreferences.categoryOrderState
    ) {
        value = withContext(Dispatchers.Default) {
            // Load only visible canonical categories. The old preloadAll call
            // parsed every lane, including the derived wildcard pool, before
            // the database screen could render anything.
            CatalogState(
                entries = visibleCategories.map { cat -> cat to TopicJsonLoader.load(cat.id) },
                loading = false
            )
        }
    }
    val catalog = catalogState.entries
    val catalogLoading = catalogState.loading
    val totalTopics = catalog.sumOf { it.second.size }

    // Build the expensive search/sort fields off the composition thread once
    // per catalog load. Sorting used to lowercase strings and create year
    // regexes for thousands of topics on the UI thread on every chip tap.
    val indexedTopics by produceState<List<IndexedTopic>>(emptyList(), catalog) {
        value = withContext(Dispatchers.Default) {
            catalog.flatMap { (cat, topics) ->
                topics.map { topic ->
                    IndexedTopic(
                        category = cat,
                        topic = topic,
                        nameKey = topic.name.lowercase(),
                        subtypeKey = topic.subtype.lowercase(),
                        bylineKey = topic.byline.lowercase(),
                        teaserKey = topic.teaser.lowercase(),
                        tagKeys = topic.tags.map(String::lowercase),
                        year = topicYear(topic)
                    )
                }
            }
        }
    }
    // v7.97 — the persisted filter can outlive its lane (a category hidden in
    // Manage Categories drops out of the catalog). Fall back to All instead
    // of leaving an invisible "no topics" state with no visible chip.
    val effectiveCat = remember(catalog, selectedCat) {
        if (selectedCat != null && catalog.none { it.first.id == selectedCat }) null else selectedCat
    }

    // Filtered rows — section headers while browsing All, topic rows always.
    // Keyed on the done-set snapshot (structural equality) so badges refresh.
    // v8.54 — with a non-default sort active the list flattens to one sorted
    // run (section headers would break a global A–Z / year order).
    val needle = query.trim().lowercase()
    val matches: (IndexedTopic) -> Boolean = { indexed ->
        needle.isEmpty() ||
            indexed.nameKey.contains(needle) ||
            indexed.subtypeKey.contains(needle) ||
            indexed.bylineKey.contains(needle) ||
            indexed.teaserKey.contains(needle) ||
            indexed.tagKeys.any { it.contains(needle) }
    }
    // Filtering and sorting happen on Dispatchers.Default. `remember` only
    // caches work; it still performs the entire sort on the UI thread.
    val rows by produceState<List<DatabaseRow>>(
        initialValue = emptyList(),
        catalog,
        indexedTopics,
        effectiveCat,
        needle,
        doneTopics,
        sortMode
    ) {
        value = withContext(Dispatchers.Default) {
            val indexById = indexedTopics.associateBy { it.topic.id }
            if (sortMode == DatabaseSortMode.DEFAULT) {
                buildList {
                    catalog.forEach { (cat, topics) ->
                        if (effectiveCat != null && effectiveCat != cat.id) return@forEach
                        val shown = topics.mapNotNull { indexById[it.id] }.filter(matches)
                        if (shown.isEmpty()) return@forEach
                        if (effectiveCat == null) {
                            add(DatabaseRow(key = "sec-${cat.id.name}", section = cat, sectionCount = shown.size))
                        }
                        shown.forEach { indexed ->
                            add(
                                DatabaseRow(
                                    key = indexed.topic.id,
                                    topic = indexed.topic,
                                    done = "${cat.id.name}::${indexed.topic.name}" in doneTopics
                                )
                            )
                        }
                    }
                }
            } else {
                val filtered = indexedTopics.filter { indexed ->
                    (effectiveCat == null || indexed.category.id == effectiveCat) && matches(indexed)
                }
                val sorted = when (sortMode) {
                    DatabaseSortMode.ALPHA_ASC ->
                        filtered.sortedWith(compareBy<IndexedTopic>({ it.nameKey }, { it.topic.id }))
                    DatabaseSortMode.ALPHA_DESC ->
                        filtered.sortedWith(
                            compareByDescending<IndexedTopic> { it.nameKey }
                                .thenByDescending { it.topic.id }
                        )
                    DatabaseSortMode.YEAR_NEWEST ->
                        filtered.sortedWith(
                            compareByDescending<IndexedTopic> { it.year ?: Int.MIN_VALUE }
                                .thenBy { it.nameKey }
                                .thenBy { it.topic.id }
                        )
                    DatabaseSortMode.YEAR_OLDEST ->
                        filtered.sortedWith(
                            compareBy<IndexedTopic> { it.year ?: Int.MAX_VALUE }
                                .thenBy { it.nameKey }
                                .thenBy { it.topic.id }
                        )
                    DatabaseSortMode.DEFAULT -> filtered
                }
                sorted.map { indexed ->
                    DatabaseRow(
                        key = indexed.topic.id,
                        topic = indexed.topic,
                        done = "${indexed.category.id.name}::${indexed.topic.name}" in doneTopics
                    )
                }
            }
        }
    }

    // ── Scroll restore + persist ─────────────────────────────────────
    // The catalog loads asynchronously (produceState), so the first frames
    // after returning compose an EMPTY list. Restoring a LazyListState there
    // clamps to 0 before the topics arrive. Instead: remember the exact
    // index+offset, scroll back once rows actually exist, and keep the
    // numbers fresh while the user scrolls (only when rows exist, so the
    // empty flash can't overwrite them).
    val hasRows = rows.isNotEmpty()
    LaunchedEffect(hasRows) {
        if (hasRows && (savedScrollIndex > 0 || savedScrollOffset > 0)) {
            listState.scrollToItem(savedScrollIndex, savedScrollOffset)
        }
    }
    LaunchedEffect(listState, hasRows) {
        if (!hasRows) return@LaunchedEffect
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            savedScrollIndex = index
            savedScrollOffset = offset
        }
    }
    // v8.54 — switching the sort reorders the whole list, so land back at
    // the top instead of keeping a random index into the new ordering.
    // (The category filter chips keep their pre-existing no-reset behavior.)
    LaunchedEffect(sortMode) {
        if (hasRows && listState.firstVisibleItemIndex > 0) {
            savedScrollIndex = 0
            savedScrollOffset = 0
            listState.scrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs (settings family).
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        // ── Scroll content — fills the screen, runs under the ragged tear.
        ScreenEntrance {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = SettingsHeroTotalHeight + 10.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Search + category filter chips ─────────────────────────
                item("controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // v8.xx — the search box is a pet landmark: the pet
                        // walks over and pokes it, and the tour's Browse-Topics
                        // stop points the guide right at it.
                        PetLandmark(
                            id = "search",
                            kind = PetLandmarks.Kind.FUN,
                            screen = "database"
                        ) { lm ->
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    if (totalTopics > 0) "Search $totalTopics topics…"
                                    else "Search topics…",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                CurioIcon(
                                    CurioIcons.Search, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 20.dp
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    Surface(
                                        onClick = { query = "" },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            CurioIcon(
                                                CurioIcons.Close, null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                size = 16.dp
                                            )
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = lm.fillMaxWidth()
                        )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item("all") {
                                DatabaseFilterChip(
                                    label = "All",
                                    count = totalTopics,
                                    selectedInk = MaterialTheme.colorScheme.onPrimaryContainer,
                                    accent = MaterialTheme.colorScheme.primary,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    selected = effectiveCat == null,
                                    onClick = { selectedCat = null }
                                )
                            }
                            items(catalog, key = { it.first.id.name }) { (cat, list) ->
                                DatabaseFilterChip(
                                    label = cat.displayName,
                                    count = list.size,
                                    selectedInk = cat.accent,
                                    accent = cat.accent,
                                    tint = cat.tint,
                                    selected = effectiveCat == cat.id,
                                    onClick = { selectedCat = cat.id }
                                )
                            }
                        }
                        // v8.54 — sort control: A–Z / Z–A / Newest / Oldest (by year).
                        // Direction is explicit so sorting never depends on a hidden toggle state.
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item("sort-label") {
                                Text(
                                    text = "Sort",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                            item("sort-alpha-asc") {
                                DatabaseSortChip(
                                    label = "A–Z",
                                    glyph = CurioIcons.ArrowUpward,
                                    selected = sortMode == DatabaseSortMode.ALPHA_ASC,
                                    onClick = { sortMode = DatabaseSortMode.ALPHA_ASC }
                                )
                            }
                            item("sort-alpha-desc") {
                                DatabaseSortChip(
                                    label = "Z–A",
                                    glyph = CurioIcons.ArrowDownward,
                                    selected = sortMode == DatabaseSortMode.ALPHA_DESC,
                                    onClick = { sortMode = DatabaseSortMode.ALPHA_DESC }
                                )
                            }
                            item("sort-newest") {
                                DatabaseSortChip(
                                    label = "Newest",
                                    glyph = CurioIcons.ArrowDownward,
                                    selected = sortMode == DatabaseSortMode.YEAR_NEWEST,
                                    onClick = {
                                        sortMode = if (sortMode == DatabaseSortMode.YEAR_NEWEST) DatabaseSortMode.DEFAULT
                                        else DatabaseSortMode.YEAR_NEWEST
                                    }
                                )
                            }
                            item("sort-oldest") {
                                DatabaseSortChip(
                                    label = "Oldest",
                                    glyph = CurioIcons.ArrowUpward,
                                    selected = sortMode == DatabaseSortMode.YEAR_OLDEST,
                                    onClick = {
                                        sortMode = if (sortMode == DatabaseSortMode.YEAR_OLDEST) DatabaseSortMode.DEFAULT
                                        else DatabaseSortMode.YEAR_OLDEST
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Loading / empty / list states ──────────────────────────
                // Catalog parsing and indexing are separate background steps.
                // Keep the loading state through both so the intermediate
                // empty `rows` value never flashes "No topics match".
                val browserLoading = catalogLoading ||
                    (catalog.isNotEmpty() && indexedTopics.isEmpty() && totalTopics > 0)
                if (browserLoading) {
                    item("loading") {
                        Text(
                            if (catalog.isEmpty()) "Loading topics…" else "Preparing topics…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (rows.isEmpty()) {
                    item("empty") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.SearchOff, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                size = 44.dp
                            )
                            Text(
                                "No topics match",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Try a different search or pick another category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(rows, key = { it.key }) { row ->
                        when {
                            row.section != null -> DatabaseSectionHeader(
                                cat = row.section,
                                count = row.sectionCount
                            )
                            row.topic != null -> DatabaseTopicRow(
                                cat = CurioCategories.byId(row.topic.categoryId),
                                topic = row.topic,
                                done = row.done,
                                onClick = {
                                    // Browse-Topics mode: the reveal opens
                                    // read-only (no explore, no recents
                                    // recording) and Back always returns here
                                    // with the scroll position restored.
                                    navController.navigate(
                                        CurioRoutes.revealForBrowse(
                                            row.topic.categoryId.routeSlug,
                                            row.topic.name
                                        )
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                }
            }
        }
        // ── Torn rose hero on top — rows disappear under the tear.
        SettingsHeroHeader(
            title = "Topic Database",
            subtitle = if (totalTopics > 0) "$totalTopics topics across ${catalog.size} lanes" else "Every topic, one place",
            onBack = { navController.popBackStack() }
        )
    }
}

/** Precomputed search/sort data for one topic. */
private data class IndexedTopic(
    val category: CurioCategory,
    val topic: CurioTopic,
    val nameKey: String,
    val subtypeKey: String,
    val bylineKey: String,
    val teaserKey: String,
    val tagKeys: List<String>,
    val year: Int?
)

/** One row in the database list — a category section header or a topic. */
private data class CatalogState(
    val entries: List<Pair<CurioCategory, List<CurioTopic>>>,
    val loading: Boolean
)

private data class DatabaseRow(
    val key: String,
    val section: CurioCategory? = null,
    val sectionCount: Int = 0,
    val topic: CurioTopic? = null,
    val done: Boolean = false
)

/** Small category filter chip with a live count. */
@Composable
private fun DatabaseFilterChip(
    label: String,
    count: Int,
    selectedInk: Color,
    accent: Color = MaterialTheme.colorScheme.primary,
    tint: Color = MaterialTheme.colorScheme.primaryContainer,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) tint else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = if (count > 0) "$label $count" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) selectedInk else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * How the Topic Database list is ordered. DEFAULT keeps the per-category
 * file order with section headers; the other modes flatten to one sorted
 * run (headers are hidden because a global sort breaks grouping).
 */
private enum class DatabaseSortMode {
    DEFAULT, ALPHA_ASC, ALPHA_DESC, YEAR_NEWEST, YEAR_OLDEST
}

/**
 * Best-effort publication/birth year for sorting. Topics have no dedicated
 * year field, so read it from the first available source: a `(Year)` in the
 * name ("Citizen Kane (1941)"), a `(Year)` in the explore target
 * ("Vespertine (2001) end-to-end"), the first 4-digit year in the teaser,
 * then the explore instruction (boosts people categories like Authors /
 * Painters where the teaser often omits dates), and finally a decade tag
 * ("1960s" → 1960). Returns null when nothing is recoverable (unknowns
 * sort last, alphabetically within that bucket).
 */
private val PARENTHESIZED_YEAR = Regex("\\((1[89]\\d{2}|20\\d{2})\\)")
private val BARE_YEAR = Regex("\\b(1[89]\\d{2}|20[0-2]\\d)\\b")
private val DECADE_YEAR = Regex("\\b(1[89]\\d|20[0-2]\\d)0s\\b")

private fun topicYear(topic: CurioTopic): Int? {
    PARENTHESIZED_YEAR.find(topic.name)?.let { return it.groupValues[1].toInt() }
    PARENTHESIZED_YEAR.find(topic.exploreAction.targetName)?.let { return it.groupValues[1].toInt() }
    BARE_YEAR.find(topic.teaser)?.let { return it.value.toInt() }
    BARE_YEAR.find(topic.exploreAction.instruction)?.let { return it.value.toInt() }
    topic.tags.forEach { tag ->
        DECADE_YEAR.find(tag)?.let { return it.groupValues[1].toInt() * 10 }
    }
    return null
}

/** Small toggle chip for the sort row — icon + label, mirrors [DatabaseFilterChip]. */
@Composable
private fun DatabaseSortChip(
    label: String,
    glyph: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
        ) {
            if (glyph != null) {
                CurioIcon(
                    glyph, null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 14.dp
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Category section header shown while browsing All. */
@Composable
private fun DatabaseSectionHeader(cat: CurioCategory, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(cat.accent, CircleShape)
        )
        Text(
            text = cat.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

/** One tappable topic row — category glyph chip, name, meta, teaser, badge. */
@Composable
private fun DatabaseTopicRow(
    cat: CurioCategory,
    topic: CurioTopic,
    done: Boolean,                                onClick: () -> Unit

) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 9.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cat.accent.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        cat.iconGlyph, null,
                        tint = cat.accent,
                        size = 22.dp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = topic.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (done) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CurioColors.Sage.copy(alpha = 0.16f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                CurioIcon(
                                    CurioIcons.Check, null,
                                    tint = CurioColors.Sage,
                                    size = 12.dp
                                )
                                Text(
                                    "explored",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CurioColors.Sage
                                )
                            }
                        }
                    }
                }
                val meta = listOfNotNull(
                    topic.byline.takeIf { it.isNotBlank() },
                    topic.subtype.takeIf { it.isNotBlank() }
                )
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = topic.teaser,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CurioIcon(
                CurioIcons.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                size = 20.dp
            )
        }
    }
}

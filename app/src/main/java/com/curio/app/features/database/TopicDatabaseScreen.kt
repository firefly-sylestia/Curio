package com.curio.app.features.database

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
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
import com.curio.app.data.publicationYear
import com.curio.app.data.TopicIndexEntry
import com.curio.app.data.TopicJsonLoader
import com.curio.app.features.settings.SettingsHeroActionPill
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.isLiquidGlassPillsActive
import com.curio.app.ui.components.LiquidGlassPageNav
import com.curio.app.ui.components.liquidGlassCapsule
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.pastelFillInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.heightIn

/**
 * Browse Topics — the whole Curio database in one place.
 *
 * Opened from the Home drawer ("Browse Topics"). Renders every topic across
 * the ten real categories PLUS a dedicated Wildcard lane (wildcard.json's
 * hand-curated curiosities, browsable on their own) with a typo-tolerant
 * search bar, a category panel (collapsed by default; its own tiny search
 * box + checkbox multi-select), an active-filters chips row, a small
 * "explored" badge on topics already marked done, and pagination. Tapping
 * any topic opens its full Topic Reveal page, exactly like spinning it.
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

/**
 * v199 — the Browse-Topics session: the selected category filter and the
 * category panel's open flag survive closing and REOPENING the screen
 * within the app session (a fresh backstack entry resets rememberSaveable,
 * so the v7.97 saveable state only survived in-place round-trips like the
 * Topic Reveal push). Process-scoped — an app restart clears it, matching
 * the user's "persistent until restart".
 */
object TopicBrowserSession {
    // v314 — multi-select categories round-trip as comma-joined CategoryId
    // names, so the selection survives closing and REOPENING the browser.
    var selectedSlugs: String by mutableStateOf("")
    var panelOpen by mutableStateOf(false)
    var savedPage by mutableIntStateOf(0)
    var savedScrollIndex by mutableIntStateOf(0)
    var savedScrollOffset by mutableIntStateOf(0)
}

@Composable
fun TopicDatabaseScreen(navController: NavController) {
    // v7.97 — SAVEABLE state: the selected category filter and the scroll
    // position survive leaving the screen (Topic Reveal round-trips, tab
    // switches, rotation, process death) instead of resetting to a fresh
    // blank list every time you come back. v26 — the search query moved into
    // [searchQuery] (the hero search field) but stays saveable here.
    // v199 — the category selection also seeds from + syncs to the
    // [TopicBrowserSession] so a fully closed-and-reopened browser restores
    // it (rememberSaveable dies with the backstack entry).
    // v314 — the filter is MULTI-SELECT now: a Set of CategoryIds, round-tripped
    // through a comma-joined saveable string (enum names) so it survives rotation,
    // screen teardown and (via [TopicBrowserSession]) reopen within the session.
    var selectedCatsKey by rememberSaveable {
        mutableStateOf(TopicBrowserSession.selectedSlugs)
    }
    val selectedCats: Set<CategoryId> = remember(selectedCatsKey) {
        selectedCatsKey.splitToSequence(',')
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { CategoryId.valueOf(it) }.getOrNull() }
            .toSet()
    }
    LaunchedEffect(selectedCats) {
        TopicBrowserSession.selectedSlugs = selectedCats.joinToString(",") { it.name }
    }
    // v314 — the one place that mutates the selection: recompute the persisted
    // string from the updated set, so [selectedCats] stays the single source.
    fun commitCats(update: (Set<CategoryId>) -> Set<CategoryId>) {
        selectedCatsKey = update(selectedCats).joinToString(",") { it.name }
    }
    // v105 — the sort control is removed; the browser keeps its default
    // per-lane A–Z order (see the rows builder).
    // v26 — hero search: the search pill morphs the hero into a search field
    // (the Cabinet's search-morph contract) so search/sort/filters all live
    // in the header instead of scrolling inside the list.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // v292i — BackHandler: close search before exiting the page.
    BackHandler(enabled = searchActive) {
        searchActive = false
        searchQuery = ""
    }
    // v314 — the Category pill toggles the category PANEL, collapsed by
    // default (only visible when explicitly opened; the old sticky chip bar
    // that auto-opened during search is gone, so searching shows NO category
    // chips — category options live in the panel only).
    var categoryPanelOpen by rememberSaveable {
        mutableStateOf(TopicBrowserSession.panelOpen)
    }
    LaunchedEffect(categoryPanelOpen) {
        TopicBrowserSession.panelOpen = categoryPanelOpen
    }
    // Tiny search box INSIDE the panel, filtering the category list itself.
    var catPanelQuery by rememberSaveable { mutableStateOf("") }
    // The category UI visible under the hero: the open panel, or the compact
    // active-filter chips row whenever at least one lane is selected.
    val filterUiVisible = categoryPanelOpen || selectedCats.isNotEmpty()
    // v36 — the Sort/Search pills live back INSIDE the hero (their top
    // row). v42 — the Category pill moved INSIDE the hero too (beside the
    // title), so content reserves only the filter UI when it is visible.
    val contentTop = DatabaseHeroTotalHeight +
        (if (categoryPanelOpen) DatabaseFilterPanelHeight
         else if (selectedCats.isNotEmpty()) DatabaseChipRowHeight
         else 0.dp) + 12.dp
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            searchFocus.requestFocus()
        }
    }
    // v7.98 — the scroll position is saved EXPLICITLY (index + offset), not
    // via LazyListState.Saver: the catalog loads asynchronously, so on return
    // the list first composes with zero rows and a restored LazyListState gets
    // clamped to 0 before the topics arrive — scrolling back to the top.
    // Saving the raw numbers and scrolling again once rows exist restores the
    // exact spot reliably.
    var savedScrollIndex by rememberSaveable { mutableIntStateOf(TopicBrowserSession.savedScrollIndex) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(TopicBrowserSession.savedScrollOffset) }
    val listState = rememberLazyListState()
    // v245 — LOCAL GLASS CAPTURE: the scrolling list records into its own
    // layer; the hero glass pills (a sibling overlay) sample it — the
    // crash-safe architecture.
    val chipGlassBackdrop = rememberLayerBackdrop()
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
    // The ten canonical lanes PLUS a dedicated Wildcard lane. wildcard.json
    // holds 500+ hand-curated curiosities (categoryId == WILDCARD) that live
    // in no other lane — the Spin deck's "wildcard" pool merges every lane,
    // so without its own lane here those topics would never be browsable.
    // Always present (the browser is an explicit browse-all surface) even if
    // Wildcard is hidden from the tab pickers in Manage Categories.
    // v27l — the filter pills + browse sections run alphabetically by
    // display name (Wildcard naturally sits near the end), so lanes are
    // easy to find instead of following the deck's default order.
    // v294 — "All" (WILDCARD) only shows when searching; default view is per-category.
    val visibleCategories = if (searchActive) {
        CurioCategories.visible + listOf(CurioCategories.byId(CategoryId.WILDCARD))
    } else {
        CurioCategories.visible
    }
        .distinctBy { it.id }
        .sortedBy { it.displayName.lowercase() }
    // The merged wildcard pool duplicates every canonical topic, so the
    // Wildcard lane shows ONLY the hand-curated wildcard.json originals —
    // the ten lanes keep their own topics and the sections never overlap.
    fun laneTopics(cat: CurioCategory, topics: List<CurioTopic>): List<CurioTopic> =
        if (cat.id == CategoryId.WILDCARD) topics.filter { it.categoryId == CategoryId.WILDCARD }
        else topics
    val cachedCatalog = remember(visibleCategories) {
        visibleCategories.mapNotNull { cat ->
            TopicJsonLoader.cached(cat.id)?.let { topics -> cat to laneTopics(cat, topics) }
        }
    }
    // v29 — the merged index path: the prebuilt topic_index.json (search
    // keys + year precomputed by scripts/build_topic_index.py) is parsed
    // when present; v174f — it no longer ships, so TopicJsonLoader builds
    // the same index at runtime from the per-category pools, prewarmed at
    // app start. Either way the browser renders from ONE merged list
    // INSTANTLY (no per-category parses, no runtime lowercase/year work)
    // and stays flat as the catalog grows past 20k. Falls back to the live
    // per-category load when the index is missing (or still warming on a
    // cold start).
    val indexEntries by produceState<List<TopicIndexEntry>?>(
        initialValue = TopicJsonLoader.cachedIndex(),
        AppPreferences.hiddenCategoriesState,
        AppPreferences.categoryOrderState
    ) {
        value = withContext(Dispatchers.Default) {
            runCatching { TopicJsonLoader.loadIndex() }.getOrNull()
        }
    }
    val useIndex = indexEntries != null
    // The fallback per-category load only runs when the index is absent.
    val catalogState by produceState<CatalogState>(
        initialValue = CatalogState(
            entries = cachedCatalog,
            loading = cachedCatalog.size < visibleCategories.size
        ),
        AppPreferences.hiddenCategoriesState,
        AppPreferences.categoryOrderState,
        useIndex
    ) {
        if (useIndex) {
            value = CatalogState(entries = emptyList(), loading = false)
        } else {
            value = withContext(Dispatchers.Default) {
                // Load the canonical lanes; the wildcard lane reuses those
                // caches (its pool merges every lane, then we keep only its
                // own curiosities) so the extra lane adds no duplicate parses.
                // v49 — a failed lane is SKIPPED, never fatal: an exception
                // here used to kill the produceState producer and freeze the
                // screen on "Loading topics…" forever. One bad file now
                // drops just its lane; the rest of the catalog still renders.
                CatalogState(
                    entries = visibleCategories.mapNotNull { cat ->
                        runCatching {
                            cat to laneTopics(cat, TopicJsonLoader.load(cat.id))
                        }.getOrNull()
                    },
                    loading = false
                )
            }
        }
    }
    val catalog: List<Pair<CurioCategory, List<CurioTopic>>> = if (useIndex) {
        remember(indexEntries, visibleCategories) {
            val byId = indexEntries.orEmpty().groupBy { it.topic.categoryId }
            visibleCategories.mapNotNull { cat ->
                val topics = byId[cat.id]
                    ?.map { it.topic }
                    ?.let { laneTopics(cat, it) }
                    .orEmpty()
                if (topics.isEmpty()) null else cat to topics
            }
        }
    } else {
        catalogState.entries
    }
    val catalogLoading = !useIndex && catalogState.loading
    val totalTopics = catalog.sumOf { it.second.size }

    // Build the expensive search/sort fields off the composition thread once
    // per catalog load (the index path uses the PRE-COMPUTED keys + year, so
    // that work disappears entirely). Sorting used to lowercase strings and
    // create year regexes for thousands of topics on the UI thread on every
    // chip tap.
    val indexedTopics by produceState<List<IndexedTopic>>(
        // Warm-cache seed: when the index is already prewarmed, the rows are
        // ready on the very first frame — no "Preparing topics…" flash.
        initialValue = TopicJsonLoader.cachedIndex().orEmpty().map { entry ->
            IndexedTopic(
                category = CurioCategories.byId(entry.topic.categoryId),
                topic = entry.topic,
                nameKey = entry.nameKey,
                subtypeKey = entry.subtypeKey,
                bylineKey = entry.bylineKey,
                teaserKey = entry.teaserKey,
                tagKeys = entry.tagKeys,
                year = entry.year
            )
        },
        catalog,
        useIndex
    ) {
        value = if (useIndex) {
            indexEntries.orEmpty().map { entry ->
                IndexedTopic(
                    category = CurioCategories.byId(entry.topic.categoryId),
                    topic = entry.topic,
                    nameKey = entry.nameKey,
                    subtypeKey = entry.subtypeKey,
                    bylineKey = entry.bylineKey,
                    teaserKey = entry.teaserKey,
                    tagKeys = entry.tagKeys,
                    year = entry.year
                )
            }
        } else {
            withContext(Dispatchers.Default) {
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
                }.distinctBy { it.topic.id }
            }
        }
    }
    // v7.97 — a persisted filter can outlive its lane (a category hidden in
    // Manage Categories drops out of the catalog): drop those from the
    // effective set instead of leaving an invisible "no topics" state.
    // v301 — category filtering works during search too (results narrow to
    // the selected lanes).
    val effectiveCats: Set<CategoryId> = remember(catalog, selectedCats) {
        selectedCats.filter { id -> catalog.any { it.first.id == id } }.toSet()
    }

    // Filtered rows — section headers while browsing All, topic rows always.
    // Keyed on the done-set snapshot (structural equality) so badges refresh.
    // v8.54 — with a non-default sort active the list flattens to one sorted
    // run (section headers would break a global A–Z / year order).
    val needle = searchQuery.trim().lowercase()
    // v314 — typo-tolerant matching: [matchLevel] returns 0 for a plain
    // substring (strong), 1 for a fuzzy (typo-tolerated) match and null for
    // no match. Search results also PRIORITIZE any lane whose name the query
    // mentions (e.g. "films", "science") — that lane's hits rank first.
    val priorityCats: Set<CategoryId> = remember(needle, visibleCategories) {
        if (needle.length < 3) emptySet()
        else visibleCategories.filter { cat ->
            val dn = cat.displayName.lowercase()
            dn.contains(needle) ||
                (needle.length >= 4 && needle.contains(dn)) ||
                fuzzyContains(dn, needle)
        }.map { it.id }.toSet()
    }
    // Title-first sort: exact title matches rank highest, then startsWith, then contains
    val titleComparator = compareBy<IndexedTopic> { t ->
        when {
            t.nameKey == needle -> 0 // exact match
            t.nameKey.startsWith(needle) -> 1 // starts with
            t.nameKey.contains(needle) -> 2 // contains in title
            else -> 3 // matched in other fields
        }
    }
    // v313 — per-category SEARCH-HIT counts, computed off the UI thread. Feeds
    // the category panel's per-lane counts (hits while searching, full totals
    // while browsing) and the "Also in" suggestion pills.
    val indexById = remember(indexedTopics) { indexedTopics.associateBy { it.topic.id } }
    val catHitCounts by produceState<Map<CategoryId, Int>>(
        initialValue = emptyMap(),
        catalog,
        indexById,
        needle
    ) {
        value = if (needle.isEmpty()) emptyMap()
        else withContext(Dispatchers.Default) {
            val out = mutableMapOf<CategoryId, Int>()
            catalog.forEach { (cat, topics) ->
                val n = topics.count { t ->
                    indexById[t.id]?.let { matchLevel(it, needle) != null } == true
                }
                if (n > 0) out[cat.id] = n
            }
            out
        }
    }
    // The dynamic chip data (full per-lane totals while browsing; per-lane
    // hit counts while searching) — feeds the category panel's counts.
    val chips: List<Pair<CurioCategory, Int>> = remember(catalog, catHitCounts, needle) {
        if (needle.isEmpty()) catalog.map { it.first to it.second.size }
        else catalog.mapNotNull { (cat, _) -> catHitCounts[cat.id]?.let { cat to it } }
    }
    // Filtering and sorting happen on Dispatchers.Default. `remember` only
    // caches work; it still performs the entire sort on the UI thread.
    val rows by produceState<List<DatabaseRow>>(
        initialValue = emptyList(),
        catalog,
        indexedTopics,
        effectiveCats,
        needle,
        doneTopics
    ) {
        value = withContext(Dispatchers.Default) {
            val indexById = indexedTopics.associateBy { it.topic.id }
            // v292h — when searching, collect ALL matches globally, sort by
            // name relevance, and render flat (no category sections). When
            // browsing, keep the per-lane A–Z order with section headers.
            if (needle.isNotEmpty()) {
                // SEARCH MODE: flat results sorted by relevance.
                // v313 — the selected categories FILTER search: results come
                // ONLY from the selected lanes (or every lane when none are
                // selected).
                // v314 — ranking: lanes mentioned by the query first, then
                // strong (substring) matches before fuzzy (typo) ones, then
                // the title-first comparator.
                val allHits = mutableListOf<RankedHit>()
                catalog.forEach { (cat, topics) ->
                    if (effectiveCats.isNotEmpty() && cat.id !in effectiveCats) return@forEach
                    topics.mapNotNull { indexById[it.id] }.forEach { indexed ->
                        val level = matchLevel(indexed, needle)
                        if (level != null) {
                            allHits += RankedHit(
                                indexed = indexed,
                                priority = indexed.category.id in priorityCats,
                                fuzzy = level == 1
                            )
                        }
                    }
                }
                allHits.sortedWith(
                    compareBy<RankedHit> { !it.priority }
                        .thenBy { it.fuzzy }
                        .thenComparator { a, b -> titleComparator.compare(a.indexed, b.indexed) }
                ).map { hit ->
                    val indexed = hit.indexed
                    DatabaseRow(
                        key = indexed.topic.id,
                        topic = indexed.topic,
                        done = "${indexed.category.id.name}::${indexed.topic.name}" in doneTopics
                    )
                }
            } else {
                // BROWSE MODE: per-lane with section headers.
                buildList {
                    catalog.forEach { (cat, topics) ->
                        if (effectiveCats.isNotEmpty() && cat.id !in effectiveCats) return@forEach
                        val shown = topics.mapNotNull { indexById[it.id] }
                            .filter { matchLevel(it, needle) != null }
                            .sortedWith(titleComparator)
                        if (shown.isEmpty()) return@forEach
                        // v314 — headers whenever browsing All or several lanes
                        // are selected; a single selected lane stays flat under
                        // its own top bar.
                        if (effectiveCats.size != 1) {
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
            }
        }
    }

    // ── v293 — PAGINATION (100 per page) ─────────────────────────────
    // The topic rows are paginated so only a manageable slice renders per
    // page. A floating nav bar at the bottom controls paging.
    // v301 — page lives in TopicBrowserSession (process-scoped singleton)
    // so it survives navigation reliably. LaunchedEffect syncs user changes.
    var currentPage by remember { mutableIntStateOf(TopicBrowserSession.savedPage) }
    LaunchedEffect(currentPage) { TopicBrowserSession.savedPage = currentPage }
    // Topic-only rows (skip section headers for counting purposes).
    val topicOnlyRows = remember(rows) { rows.filter { it.topic != null } }
    val totalPages = ((topicOnlyRows.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
    // v311 — only coerce AFTER rows load: on return the async produceState
    // starts with emptyList(), making totalPages=1 and clamping the saved
    // page to 0 before the catalog arrives. Guarding on rows.isNotEmpty()
    // preserves the saved page until the data is ready.
    // v292i — only coerce AFTER rows AND topicOnlyRows load: on return
    // the async produceState starts with emptyList(), making totalPages=1
    // and clamping the saved page to 0. Guarding on topicOnlyRows.isNotEmpty()
    // preserves the saved page until the data is ready.
    if (topicOnlyRows.isNotEmpty()) {
        currentPage = currentPage.coerceIn(0, totalPages - 1)
    }
    // Slice the full rows list to only show the current page's topics,
    // but keep section headers that belong to this page's range.
    // Guard: when topicOnlyRows is empty (async data hasn't loaded yet or
    // filter removed all topics), clamp start to 0 so subList never gets
    // fromIndex > toIndex — the persisted currentPage can exceed the new
    // row count after a category switch or data reload.
    val safeTopicCount = topicOnlyRows.size
    val topicStart = if (safeTopicCount == 0) 0 else (currentPage * PAGE_SIZE).coerceAtMost(safeTopicCount)
    val topicEnd = if (safeTopicCount == 0) 0 else (topicStart + PAGE_SIZE).coerceAtMost(safeTopicCount)
    val pageTopicKeys = remember(topicOnlyRows, topicStart, topicEnd) {
        if (topicStart < topicEnd) topicOnlyRows.subList(topicStart, topicEnd).map { it.key }.toSet()
        else emptySet()
    }
    val paginatedRows = remember(rows, pageTopicKeys) {
        // Keep all section headers + the topic rows for this page.
        rows.filter { it.section != null || it.key in pageTopicKeys }
    }
    // Preserve the session page on the first composition after navigation, then
    // reset only when the category filter actually changes.
    var categoryInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(effectiveCats) {
        if (categoryInitialized) currentPage = 0
        else categoryInitialized = true
    }
    // Page nav visibility: hide when scrolling, show when stopped.
    var pageNavVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) pageNavVisible = false
                else kotlinx.coroutines.delay(400); pageNavVisible = true
            }
    }


    // ── Scroll restore + persist ─────────────────────────────────────
    // The catalog loads asynchronously (produceState), so the first frames
    // after returning compose an EMPTY list. Restoring a LazyListState there
    // clamps to 0 before the topics arrive. Instead: remember the exact
    // index+offset, scroll back once rows actually exist, and keep the
    // numbers fresh while the user scrolls (only when rows exist, so the
    // empty flash can't overwrite them).
    // v49 — persist only when the first visible ROW changes, never on every
    // scroll frame: the old snapshotFlow collected the pixel offset too, so
    // a fast wheel scroll wrote to the saveable registry ~60x/second
    // (per-frame churn on a 16k-row list). Restore now lands at the row
    // top instead of mid-row — a sub-row precision loss no one can feel.
    val hasRows = rows.isNotEmpty()
    LaunchedEffect(hasRows) {
        if (hasRows && (savedScrollIndex > 0 || savedScrollOffset > 0)) {
            listState.scrollToItem(savedScrollIndex, savedScrollOffset)
        }
    }
    LaunchedEffect(listState, hasRows) {
        if (!hasRows) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                savedScrollIndex = index
                savedScrollOffset = 0
                TopicBrowserSession.savedScrollIndex = index
                TopicBrowserSession.savedScrollOffset = 0
            }
    }
    // v27r — switching the CATEGORY filter (or All) starts from the top,
    // never from a stale position into the new lane.
    LaunchedEffect(effectiveCats) {
        if (hasRows && listState.firstVisibleItemIndex > 0) {
            savedScrollIndex = 0
            savedScrollOffset = 0
            listState.scrollToItem(0)
        }
    }

    // ── A–Z fast-scroller (v26) — the scroll knob's letter rail: the active
    //    letter is derived from the topic row at the top of the list, and
    //    tapping a letter jumps to the first topic starting with it.
    val alphabetLetters = remember { ('A'..'Z').map { it.toString() } }
    val activeAlphabetIndex: Int? by remember(rows) {
        derivedStateOf {
            val last = (rows.size - 1).coerceAtLeast(0)
            val start = listState.firstVisibleItemIndex.coerceIn(0, last)
            for (i in start until rows.size) {
                val name = rows[i].topic?.name
                if (!name.isNullOrEmpty()) {
                    val idx = alphabetLetters.indexOf(name.first().uppercaseChar().toString())
                    if (idx >= 0) return@derivedStateOf idx
                }
            }
            null
        }
    }
    val alphabetScope = rememberCoroutineScope()
    val onAlphabetSelect: (String) -> Unit = { letter ->
        val target = rows.indexOfFirst {
            it.topic?.name?.startsWith(letter, ignoreCase = true) == true
        }
        if (target >= 0) {
            alphabetScope.launch { listState.scrollToItem(target.coerceIn(0, rows.lastIndex)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the lane wash.
            .background(heroPageBackground())
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
                // v245 — the glass hero pills sample this local capture to
                // refract the scrolling rows, so record whenever liquid glass
                // is on (not gated on the filter UI being visible).
                modifier = Modifier.fillMaxSize()
                    .then(if (isLiquidGlassPillsActive())
                        Modifier.layerBackdrop(chipGlassBackdrop)
                    else Modifier),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = contentTop,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                } else {
                    // v313 — BROWSE a selected lane: the list shows ONE
                    // category only (no in-list category names), topped by an
                    // arrow bar that returns to All.
                    if (needle.isEmpty() && effectiveCats.size == 1) {
                        item(key = "category-top-bar") {
                            val curCatId = effectiveCats.first()
                            val curCat = CurioCategories.byId(curCatId)
                            val curCount = catalog
                                .firstOrNull { it.first.id == curCatId }?.second?.size ?: 0
                            DatabaseCategoryTopBar(
                                cat = curCat,
                                count = curCount,
                                onBackToAll = { commitCats { emptySet() } }
                            )
                        }
                    }
                    // v313 — SEARCH suggestions: pills for other categories
                    // that also match the query, so results never hide behind
                    // the stale selected-lane filters. Tap = toggle that lane.
                    if (needle.isNotEmpty() && effectiveCats.isNotEmpty()) {
                        val others = catHitCounts
                            .filterKeys { it !in effectiveCats }
                            .mapNotNull { (id, n) ->
                                CurioCategories.all.firstOrNull { it.id == id }?.let { it to n }
                            }
                            .sortedByDescending { it.second }
                        if (others.isNotEmpty()) {
                            item(key = "search-suggestions") {
                                // v314 — multi-select: tapping a pill toggles that
                                // lane in the active set (adds it when absent).
                                SearchSuggestionRow(
                                    hits = others,
                                    onSelect = { id ->
                                        commitCats { current ->
                                            if (id in current) current - id else current + id
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (rows.isEmpty()) {
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
                        items(
                        paginatedRows,
                        key = { it.key },
                        // v49 — section headers and topic rows reuse their own
                        // LazyColumn slots instead of being treated as one
                        // interchangeable item type, so fast wheel scrolling
                        // over a 16k-row catalog doesn't churn slot types.
                        contentType = { row -> if (row.section != null) "section" else "topic" }
                    ) { row ->
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
        }
        // Side scroll indicator — speed-scrolling knob (v26) with the A–Z
        // fast-scroller rail (tap the knob to open it, tap a letter to jump).
        CurioVerticalScrollIndicator(
            state = listState.scrollIndicatorState,
            onScrollBy = { listState.dispatchRawDelta(it) },
            alphabet = alphabetLetters,
            activeAlphabetIndex = activeAlphabetIndex,
            onAlphabetSelect = onAlphabetSelect,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = contentTop, bottom = 16.dp)
        )

        // ── Floating back-to-top arrow (v26) — once the list is scrolled
        // down a ways (≈ a full screen of rows), a small arrow floats at the
        // top of the viewport (just below the pinned chip bar) and jumps
        // straight back to the top of the list instead of scrolling all the
        // way up again. Row-based estimate (~70dp each) so it only appears
        // when genuinely "too much down".
        val backToTopVisible by remember {
            derivedStateOf { listState.firstVisibleItemIndex >= BackToTopRowThreshold }
        }
        AnimatedVisibility(
            visible = backToTopVisible,
            enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.85f),
            exit = scaleOut(tween(180), targetScale = 0.85f) + fadeOut(tween(180)),
            modifier = Modifier
                // Floating just below the pinned chip bar, centered over the
                // list — clear of the scroll-indicator strip on the right.
                .align(Alignment.TopCenter)
                .padding(top = DatabaseHeroTotalHeight + 74.dp)
        ) {
            Surface(
                onClick = {
                    savedScrollIndex = 0
                    savedScrollOffset = 0
                    alphabetScope.launch { listState.scrollToItem(0) }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                // v27r — a compact arrow: 16dp glyph + slim padding (was
                // 20dp + 11dp, which read as a big button), flat 2dp shadow.
                shadowElevation = 2.dp
            ) {
                // v26c — Surface's content box has no contentAlignment, so the
                // glyph used to sit top-start inside the circle; center it.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(7.dp)
                ) {
                    CurioIcon(
                        CurioIcons.ArrowUpward,
                        "Back to top",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 16.dp
                    )
                }
            }
        }

        // ── Category filter UI (v314) — the Category pill inside the hero
        // toggles the category PANEL (slide + fade under the torn hero), or
        // the compact active-filter chips row whenever lanes are selected.
        // Only the active lanes ever render chips now — searching shows no
        // auto-opened every-lane bar, matching the user's request.
        AnimatedVisibility(
            visible = filterUiVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(380, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(320)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300, easing = LinearOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(220))
        ) {
            if (categoryPanelOpen) {
                // v314 — the collapsed-by-default category PANEL: its own tiny
                // search box (filters the category list) + checkbox multi-select.
                DatabaseCategoryPanel(
                    categories = visibleCategories,
                    counts = chips.associate { it.first.id to it.second },
                    query = catPanelQuery,
                    onQueryChange = { catPanelQuery = it },
                    selected = effectiveCats,
                    onToggle = { id ->
                        commitCats { current -> if (id in current) current - id else current + id }
                    },
                    onClearAll = {
                        commitCats { emptySet() }
                        catPanelQuery = ""
                    },
                    onDone = { categoryPanelOpen = false }
                )
            } else if (selectedCats.isNotEmpty()) {
                // v314 — the active-filter chips row: exactly the selected
                // lanes, each removable with one tap.
                ActiveFilterChips(
                    categories = visibleCategories.filter { it.id in effectiveCats },
                    onRemove = { id -> commitCats { current -> current - id } },
                    onClearAll = { commitCats { emptySet() } }
                )
            }
        }

        // ── v294 — FLOATING PAGE NAV: reusable liquid glass component ──
        if (totalPages > 1) {
            LiquidGlassPageNav(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = { currentPage = it },
                visible = pageNavVisible,
                glassBackdrop = if (isLiquidGlassPillsActive()) chipGlassBackdrop else null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            )
        }
        // Hold-to-jump state
        var showPagePicker by remember { mutableStateOf(false) }

// ── Torn rose hero on top — rows disappear under the tear. v36 —
        // the Sort dropdown + Search pill ride the hero's top row again
        // (they briefly sat in a below-hero row in v33; the user wanted
        // them back on the banner), the title stays at the TOP
        // (titleAtTop), and the search pill still morphs the hero into a
        // search field while active.
        SettingsHeroHeader(
            title = "Topic Database",
            subtitle = if (totalTopics > 0) "$totalTopics topics across ${catalog.size} lanes" else "Every topic, one place",
            onBack = { navController.popBackStack() },
            searchActive = searchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onCloseSearch = { searchActive = false; searchQuery = "" },
            searchFocus = searchFocus,
            searchPlaceholder = if (totalTopics > 0) "Search $totalTopics topics…" else "Search topics…",
            titleAtTop = true,
            glassBackdrop = if (isLiquidGlassPillsActive()) chipGlassBackdrop else null,
            // v42 — the Category pill lives INSIDE the hero beside the
            // title, directly under the Sort/Search pills.
            titleTrailing = { ink ->
                SettingsHeroActionPill(
                    onClick = {
                        if (categoryPanelOpen) catPanelQuery = ""
                        categoryPanelOpen = !categoryPanelOpen
                    },
                    glyph = CurioIcons.Tune,
                    label = if (selectedCats.isEmpty()) "Category · All"
                            else "Categories · ${selectedCats.size}",
                    ink = ink,
                    // v292h — liquid glass category pill
                    modifier = if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                        Modifier.liquidGlassCapsule(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            backdrop = chipGlassBackdrop
                        )
                    else Modifier,
                    // v314 — chevron flips with the panel: ▾ closed, ▴ open.
                    trailingGlyph = if (categoryPanelOpen)
                        CurioIcons.KeyboardArrowUp
                    else CurioIcons.KeyboardArrowDown,
                    trailingContentDescription = if (categoryPanelOpen) "Hide category options"
                        else "Show category options",
                    emphasized = categoryPanelOpen || selectedCats.isNotEmpty()
                )
            },
            // Passed as a NAMED argument (not trailing-lambda syntax): the
            // @Composable slot isn't the last parameter, and the trailing
            // form fails to bind under K2.
            trailing = { ink ->
                // v105 — the sort dropdown is gone; the hero row keeps the
                // Search pill only. The pet landmark rides the header with
                // the search box: the pet still walks over and pokes it, and
                // the tour's Browse-Topics stop points at it.
                PetLandmark(
                    id = "search",
                    kind = PetLandmarks.Kind.FUN,
                    screen = "database"
                ) { lm ->
                    SettingsHeroActionPill(
                        onClick = { searchActive = true },
                        glyph = CurioIcons.Search,
                        contentDescription = "Search topics",
                        ink = ink,
                        modifier = lm.then(
                            // v292h — liquid glass search pill matching the
                            // back button and page nav pills.
                            if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                                Modifier.liquidGlassCapsule(
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    backdrop = chipGlassBackdrop
                                )
                            else Modifier
                        ),
                        // v85 — emphasized hero fill (the hero action-pill
                        // language).
                        emphasized = true
                    )
                }
            }
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

/** One search hit with its ranking inputs — lane mention + strong/fuzzy. */
private data class RankedHit(
    val indexed: IndexedTopic,
    val priority: Boolean,
    val fuzzy: Boolean
)

/**
 * v314 — match quality for search: 0 = strong (plain substring on any
 * search field), 1 = fuzzy (typo-tolerated), null = no match. The strong
 * pass is the cheap `contains` check; fuzzy runs only when strong fails,
 * over name/byline/subtype with token-level edit distance.
 */
private fun matchLevel(t: IndexedTopic, needle: String): Int? {
    if (needle.isEmpty()) return 0
    if (t.nameKey.contains(needle) || t.subtypeKey.contains(needle) ||
        t.bylineKey.contains(needle) || t.teaserKey.contains(needle) ||
        t.tagKeys.any { it.contains(needle) }) return 0
    if (fuzzyTopicMatch(t, needle)) return 1
    return null
}

/** Typo-tolerant fallback — bounded to sane query lengths so the fuzzy pass
 *  over the catalog stays cheap (it runs off the UI thread in produceState). */
private fun fuzzyTopicMatch(t: IndexedTopic, needle: String): Boolean {
    if (needle.length < 3 || needle.length > 48) return false
    return fuzzyContains(t.nameKey, needle) ||
        fuzzyContains(t.bylineKey, needle) ||
        fuzzyContains(t.subtypeKey, needle)
}

/** All query tokens must fuzzy-match some word in the field ("hary ptter"
 *  → harry + potter: transposed/skipped letters still hit). */
private fun fuzzyContains(hay: String, needle: String): Boolean {
    val tokens = needle.split(' ').filter { it.isNotBlank() }
    val words = hay.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
    if (tokens.size > 1) return tokens.all { tok -> words.any { fuzzyWord(it, tok) } }
    return words.any { fuzzyWord(it, needle) }
}

private fun fuzzyWord(word: String, token: String): Boolean {
    if (word == token) return true
    if (word.startsWith(token) || token.startsWith(word)) return true
    // One typo for short tokens, two once the token has a few letters.
    val tol = when {
        token.length <= 2 -> 0
        token.length <= 5 -> 1
        else -> 2
    }
    return editDistance(word, token) <= tol
}

/** Levenshtein distance with an early bail at 3 (beyond our max tolerance). */
private fun editDistance(a: String, b: String): Int {
    if (kotlin.math.abs(a.length - b.length) > 2) return 3
    val dp = IntArray(b.length + 1) { it }
    for (i in a.indices) {
        var prev = dp[0]
        dp[0] = i + 1
        for (j in b.indices) {
            val tmp = dp[j + 1]
            dp[j + 1] = minOf(
                dp[j + 1] + 1,
                dp[j] + 1,
                prev + if (a[i] == b[j]) 0 else 1
            )
            prev = tmp
        }
    }
    return dp[b.length]
}

// ── Category filter UI ────────────────────────────────────────────────
// v314 — the old sticky every-lane chip bar is gone. The Category pill in
// the hero opens a collapsed-by-default PANEL (own tiny search box + checkbox
// multi-select); once lanes are selected, a compact ACTIVE-FILTER chips row
// sits under the hero with one-tap removal.
private val DatabaseHeroTotalHeight = SettingsHeroTotalHeight
/** Where the category UI (panel or active-filter chips) rests under the hero. */
private val DatabaseChipBarRestTop = DatabaseHeroTotalHeight + 4.dp
/** The active-filter chips row's layout height — content starts below it. */
private val DatabaseChipRowHeight = 52.dp
/** The category PANEL's layout height — search header + scrollable checkbox
 *  list (~7-8 lanes visible, then it scrolls). */
private val DatabaseFilterPanelHeight = 352.dp
/** Rows scrolled before the floating back-to-top arrow appears (≈ one
 *  full screen — each row is roughly 70dp tall). */
/** v292g — page size for Topic Database pagination. */
private const val PAGE_SIZE = 100

/** Rows scrolled before the floating back-to-top arrow appears (≈ one
 *  full screen — each row is roughly 70dp tall). */
private const val BackToTopRowThreshold = 10

/**
 * v314 — the ACTIVE-FILTER chips row: exactly the selected lanes (no "All"
 * pill, no every-lane bar while searching), each chip removable with one
 * tap (the trailing ✕), plus a Clear-all action when any lane is active.
 */
@Composable
private fun BoxScope.ActiveFilterChips(
    categories: List<CurioCategory>,
    onRemove: (CategoryId) -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .offset(y = DatabaseChipBarRestTop)
            .horizontalScroll(rememberScrollState())
    ) {
        categories.forEach { cat ->
            Surface(
                onClick = { onRemove(cat.id) },
                shape = RoundedCornerShape(50),
                color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
                shadowElevation = 1.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    CurioIcon(cat.iconGlyph, null, tint = cat.categoryInk(), size = 14.dp)
                    Text(
                        cat.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = cat.categoryInk()
                    )
                    CurioIcon(
                        CurioIcons.Close,
                        "Remove ${cat.displayName} filter",
                        tint = cat.categoryInk().copy(alpha = 0.7f),
                        size = 14.dp
                    )
                }
            }
        }
        if (categories.isNotEmpty()) {
            TextButton(onClick = onClearAll) {
                Text(
                    "Clear all",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * v314 — the category PANEL: opens from the hero Category pill (collapsed
 * by default), with its own tiny search box that filters the category list
 * itself, checkbox multi-select backed by a Set<CategoryId>, and Clear all /
 * Done (Done collapses back to the active-chips row).
 */
@Composable
private fun BoxScope.DatabaseCategoryPanel(
    categories: List<CurioCategory>,
    counts: Map<CategoryId, Int>,
    query: String,
    onQueryChange: (String) -> Unit,
    selected: Set<CategoryId>,
    onToggle: (CategoryId) -> Unit,
    onClearAll: () -> Unit,
    onDone: () -> Unit
) {
    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) categories
                else categories.filter { it.displayName.lowercase().contains(q) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .offset(y = DatabaseChipBarRestTop)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurioSearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholder = "Filter categories…",
                    modifier = Modifier.weight(1f)
                )
                if (selected.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            "Clear all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onDone) {
                    Text("Done", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (shown.isEmpty()) {
                Text(
                    "No categories match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 276.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    shown.forEach { cat ->
                        CategoryCheckboxRow(
                            cat = cat,
                            count = counts[cat.id] ?: 0,
                            checked = cat.id in selected,
                            onToggle = { onToggle(cat.id) }
                        )
                    }
                }
            }
        }
    }
}

/** One category row inside the panel — accent checkbox + name + count. */
@Composable
private fun CategoryCheckboxRow(
    cat: CurioCategory,
    count: Int,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        val accent = cat.themedAccent()
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = pastelFillInk(accent)
            ),
            modifier = Modifier.size(38.dp)
        )
        CurioIcon(cat.iconGlyph, null, tint = cat.categoryInk(), size = 16.dp)
        Text(
            cat.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Best-effort publication/birth year for sorting — v135: shared
 * [CurioTopic.publicationYear] (the reveal's decade tag chip uses the same
 * extraction, so the two surfaces can never drift). Unknowns sort last,
 * alphabetically within that bucket.
 */
private fun topicYear(topic: CurioTopic): Int? = topic.publicationYear()

/**
 * v313 — the single-category BROWSE bar, shown at the very top of the list
 * when a lane is selected (not searching): "← Films · 342 topics". The list
 * renders ONLY this lane's topics — no in-list category names — and tapping
 * the bar (arrow left) returns to All.
 */
@Composable
private fun DatabaseCategoryTopBar(
    cat: CurioCategory,
    count: Int,
    onBackToAll: () -> Unit
) {
    Surface(
        onClick = onBackToAll,
        shape = RoundedCornerShape(50),
        color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            CurioIcon(
                CurioIcons.ChevronLeft, "View all categories",
                tint = cat.categoryInk(), size = 18.dp
            )
            CurioIcon(
                cat.iconGlyph, null,
                tint = cat.categoryInk(), size = 16.dp
            )
            Text(
                text = cat.displayName.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count topics",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            CurioIcon(
                CurioIcons.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                size = 16.dp
            )
        }
    }
}

/**
 * v313 — "Also in: Films · 4" suggestion pills, shown ABOVE the search
 * results when lanes are selected and other lanes also match the query.
 * v314 — multi-select: tapping a pill TOGGLES that lane in the active set
 * (adds it when absent), so results from other categories are one tap away.
 */
@Composable
private fun SearchSuggestionRow(
    hits: List<Pair<CurioCategory, Int>>,
    onSelect: (CategoryId) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = "Also in",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        hits.forEach { (cat, count) ->
            Surface(
                onClick = { onSelect(cat.id) },
                shape = RoundedCornerShape(50),
                color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    CurioIcon(cat.iconGlyph, null, tint = cat.categoryInk(), size = 13.dp)
                    Text(
                        text = "${cat.displayName} · $count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
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
            // v27r — the section header sits higher (was 14dp top padding),
            // tucking the category name against the previous row.
            .padding(top = 6.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                // v135 — themed accent so the marker stays visible in dark
                // mode (raw deep accent disappears on the black page).
                .background(cat.themedAccent(), CircleShape)
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
            // v135 — the icon tile wears the MODERN theme-aware category
            // recipe (tinted card surface + readable category ink) instead
            // of the old raw-accent fill, which went dark-on-dark in dark
            // mode (deep accent on a 14% deep-accent tile on a black page).
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        cat.iconGlyph, null,
                        tint = cat.categoryInk(),
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
                            color = curioSageInk().copy(alpha = 0.16f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                CurioIcon(
                                    CurioIcons.Check, null,
                                    tint = curioSageInk(),
                                    size = 12.dp
                                )
                                Text(
                                    "explored",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = curioSageInk()
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

package com.curio.app.features.database

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
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
import com.curio.app.ui.components.isLiquidGlassPillsActive
import com.curio.app.ui.theme.isCurioDarkTheme
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

/**
 * Browse Topics — the whole Curio database in one place.
 *
 * Opened from the Home drawer ("Browse Topics"). Renders every topic across
 * the ten real categories PLUS a dedicated Wildcard lane (wildcard.json's
 * hand-curated curiosities, browsable on their own) with a search bar,
 * per-category filter chips, a small "explored" badge on
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

/**
 * v199 — the Browse-Topics session: the selected category filter and the
 * chip bar's expanded flag survive closing and REOPENING the screen within
 * the app session (a fresh backstack entry resets rememberSaveable, so the
 * v7.97 saveable state only survived in-place round-trips like the Topic
 * Reveal push). Process-scoped — an app restart clears it, matching the
 * user's "persistent until restart".
 */
object TopicBrowserSession {
    var selectedSlug by mutableStateOf<String?>(null)
    var chipBarOpen by mutableStateOf(false)
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
    var selectedCat by rememberSaveable {
        mutableStateOf(
            TopicBrowserSession.selectedSlug
                ?.let { CurioCategories.byRouteSlug(it)?.id }
        )
    }
    LaunchedEffect(selectedCat) {
        TopicBrowserSession.selectedSlug = selectedCat?.routeSlug
    }
    // v105 — the sort control is removed; the browser keeps its default
    // per-lane A–Z order (see the rows builder).
    // v26 — hero search: the search pill morphs the hero into a search field
    // (the Cabinet's search-morph contract) so search/sort/filters all live
    // in the header instead of scrolling inside the list.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // v30 — the Category pill (second row under the hero pills) toggles the
    // sticky category chips; they also show while searching. Hidden by
    // default (matches the Cabinet), so the Category pill is the way in.
    // v199 — the chip bar's expanded flag rides the session too, so the
    // browser reopens with the chips in the same state the user left them.
    var categoryFilterOpen by rememberSaveable {
        mutableStateOf(TopicBrowserSession.chipBarOpen)
    }
    LaunchedEffect(categoryFilterOpen) {
        TopicBrowserSession.chipBarOpen = categoryFilterOpen
    }
    // v30 — the chip-bar reservation only applies while the chips are
    // visible (the pill or search); collapsed, content starts right below
    // the (taller) hero.
    val chipsVisible = categoryFilterOpen || searchActive
    // v36 — the Sort/Search pills live back INSIDE the hero (their top
    // row). v42 — the Category pill moved INSIDE the hero too (beside the
    // title), so content reserves only the chip bar when the chips are
    // open — no separate pill row below the banner.
    val contentTop = DatabaseHeroTotalHeight +
        (if (chipsVisible) DatabaseChipBarHeight else 0.dp) + 12.dp
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
    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    // v245 — LOCAL GLASS CAPTURE for the floating category chip bar: the
    // scrolling list records into its own layer; the chips (a sibling
    // overlay) sample it — the crash-safe architecture.
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
    val visibleCategories = (CurioCategories.visible + listOf(CurioCategories.byId(CategoryId.WILDCARD)))
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
    val needle = searchQuery.trim().lowercase()
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
        doneTopics
    ) {
        value = withContext(Dispatchers.Default) {
            val indexById = indexedTopics.associateBy { it.topic.id }
            // v105 — the sort control is removed; the browser always keeps
            // its default per-lane A–Z order (stable sort: ties keep file
            // order) with the category section headers grouping the lanes.
            buildList {
                catalog.forEach { (cat, topics) ->
                    if (effectiveCat != null && effectiveCat != cat.id) return@forEach
                    val shown = topics.mapNotNull { indexById[it.id] }
                        .filter(matches)
                        .sortedBy { it.nameKey }
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
        }
    }

    // ── v292g — PAGINATION (100 per page) ─────────────────────────────
    // The topic rows are paginated so only a manageable slice renders per
    // page. A liquid-glass floating nav bar at the bottom controls paging.
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    // Topic-only rows (skip section headers for counting purposes).
    val topicOnlyRows = remember(rows) { rows.filter { it.topic != null } }
    val totalPages = ((topicOnlyRows.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
    currentPage = currentPage.coerceIn(0, totalPages - 1)
    // Slice the full rows list to only show the current page's topics,
    // but keep section headers that belong to this page's range.
    val topicStart = currentPage * PAGE_SIZE
    val topicEnd = (topicStart + PAGE_SIZE).coerceAtMost(topicOnlyRows.size)
    val pageTopicKeys = remember(topicOnlyRows, topicStart, topicEnd) {
        topicOnlyRows.subList(topicStart, topicEnd).map { it.key }.toSet()
    }
    val paginatedRows = remember(rows, pageTopicKeys) {
        // Keep all section headers + the topic rows for this page.
        rows.filter { it.section != null || it.key in pageTopicKeys }
    }
    // Reset to page 0 when filter/search changes.
    LaunchedEffect(effectiveCat, needle) {
        currentPage = 0
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
            }
    }
    // v27r — switching the CATEGORY filter (or All) starts from the top,
    // never from a stale position into the new lane.
    LaunchedEffect(effectiveCat) {
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
                // v291 — only capture when chip bar is visible.
                modifier = Modifier.fillMaxSize()
                    .then(if (chipsVisible && isLiquidGlassPillsActive())
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

        // ── Floating category filter bar (v26) — the Cabinet's sticky chip
        // bar language: rests just below the hero, then lifts, pops
        // (0.97 → 1.0) and frosts in as the list scrolls, pinning just
        // below the ragged tear while the topic rows pass underneath it.
        // v30 — hidden by default; the Category pill (now INSIDE the hero,
        // beside the title) or search reveal it, matching the Cabinet.
        // v52b — the category chip bar animates in/out instead of popping
        // instantly when the Category pill opens or search starts, matching
        // the Cabinet.
        // v69 — the bar is positioned with a large .offset(y = barTop), so
        // expandVertically's height+clip animation hid it until the clip
        // finished (a delayed pop with no visible motion). A vertical SLIDE
        // translates the whole bar instead — the chips emerge from under
        // the torn hero with a real slide + fade.
        // v105 — smoother: a longer, decelerating slide (LinearOutSlowIn)
        // with a matched fade so the chips settle gently instead of
        // snapping down.
        AnimatedVisibility(
            visible = chipsVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(380, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(320)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300, easing = LinearOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(220))
        ) {
            DatabaseStickyChipBar(
                glassBackdrop = if (isLiquidGlassPillsActive()) chipGlassBackdrop else null,
                listState = listState,
                catalog = catalog,
                totalTopics = totalTopics,
                selectedCat = effectiveCat,
                onSelectAll = { selectedCat = null },
                onSelectCategory = { selectedCat = it }
            )
        }

        // ── v293 — FLOATING PAGE NAV: individual liquid glass pills ────
        if (totalPages > 1) {
            val navAlpha by animateFloatAsState(
                targetValue = if (pageNavVisible) 1f else 0f,
                animationSpec = tween(250),
                label = "pageNavAlpha"
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .graphicsLayer { alpha = navAlpha; translationY = (1f - navAlpha) * 20f },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button — liquid glass pill
                AnimatedVisibility(
                    visible = currentPage > 0,
                    enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.7f),
                    exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.7f)
                ) {
                    Surface(
                        onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                        modifier = Modifier
                            .height(52.dp)
                            .defaultMinSize(minWidth = 52.dp)
                            .then(
                                if (isLiquidGlassPillsActive() && chipGlassBackdrop != null) {
                                    Modifier.liquidGlassCapsule(
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                        backdrop = chipGlassBackdrop
                                    )
                                } else Modifier
                            )
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(CurioIcons.ChevronLeft, null,
                                tint = MaterialTheme.colorScheme.onSurface, size = 22.dp)
                        }
                    }
                }
                // Page number pill — liquid glass with primary color
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .height(52.dp)
                        .defaultMinSize(minWidth = 80.dp)
                        .then(
                            if (isLiquidGlassPillsActive() && chipGlassBackdrop != null) {
                                Modifier.liquidGlassCapsule(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    backdrop = chipGlassBackdrop
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
                // Next button — liquid glass pill
                Surface(
                    onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages - 1) },
                    shape = RoundedCornerShape(26.dp),
                    color = if (currentPage < totalPages - 1) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                    else Color.Transparent,
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier
                        .height(52.dp)
                        .defaultMinSize(minWidth = 52.dp)
                        .then(
                            if (isLiquidGlassPillsActive() && chipGlassBackdrop != null) {
                                Modifier.liquidGlassCapsule(
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    backdrop = chipGlassBackdrop
                                )
                            } else Modifier
                        )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(CurioIcons.ChevronRight, null,
                            tint = if (currentPage < totalPages - 1) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            size = 22.dp)
                    }
                }
            }
        }

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
            // v42 — the Category pill lives INSIDE the hero beside the
            // title, directly under the Sort/Search pills.
            titleTrailing = { ink ->
                SettingsHeroActionPill(
                    onClick = { categoryFilterOpen = !categoryFilterOpen },
                    glyph = CurioIcons.Tune,
                    label = "Category · ${selectedCat?.let { CurioCategories.byId(it).displayName } ?: "All"}",
                    ink = ink,
                    // v292h — liquid glass category pill
                    modifier = if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                        Modifier.liquidGlassCapsule(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            backdrop = chipGlassBackdrop
                        )
                    else Modifier,
                    // v30 — chevron flips with the chips: ▾ closed, ▴ open.
                    trailingGlyph = if (categoryFilterOpen)
                        CurioIcons.KeyboardArrowUp
                    else CurioIcons.KeyboardArrowDown,
                    trailingContentDescription = if (categoryFilterOpen) "Hide category chips"
                        else "Show category chips",
                    emphasized = categoryFilterOpen
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

/** Small category filter chip with a live count. */
@Composable
private fun DatabaseFilterChip(
    label: String,
    count: Int,
    accent: Color = MaterialTheme.colorScheme.primary,
    selected: Boolean,
    onClick: () -> Unit,
    // v26 — the floating chip bar pops each pill on scroll: the label
    // blooms toward its accent as the pill pops (Cabinet's per-pill pop).
    popProgress: Float = 0f,
    // v245 — liquid glass: clear refracting capsule over the local page
    // capture; ONE theme ink for every label (no per-category colors).
    glass: Boolean = false,
    glassBackdrop: LayerBackdrop? = null
) {
    val themeInk = if (isCurioDarkTheme()) Color.White else Color.Black
    val labelColor = when {
        // v245 — one color only, straight from the theme.
        glass -> themeInk
        selected -> pastelFillInk(accent)
        else -> lerp(
            MaterialTheme.colorScheme.onSurfaceVariant,
            accent,
            popProgress * 0.55f
        )
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        // v27q — selection reads as a SOLID accent fill with pastel-aware
        // ink (the old primaryContainer/category-tint fills were translucent
        // and let the shadow bleed); elevation stays a flat 2dp.
        color = if (!glass && selected) accent else if (glass) Color.Transparent
                else MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = if (glass) 0.dp else 2.dp,
        modifier = if (glass && glassBackdrop != null)
            Modifier.liquidGlassCapsule(
                container = if (selected) accent
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                // v292g — Samsung frosted look: forceFrost overrides
                // the Clear-glass toggle so chips always frost.
                washAlpha = if (selected) 0.68f else 0.55f,
                backdrop = glassBackdrop,
                forceFrost = true
            ) else Modifier
    ) {
        Text(
            text = if (count > 0) "$label $count" else label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

// ── Floating category filter bar ─────────────────────────────────────────
// The Cabinet's sticky chip bar, adapted for the database's LazyListState:
// rests just below the hero + Category pill row, then lifts, pops and pins
// just below the ragged tear as the list scrolls, while the topic rows
// pass underneath. v31 — the Category pill lives in its own row BELOW the
// hero (not in a second hero row), so the hero keeps the shared settings
// height and the header text never moves down.
private val DatabaseHeroTotalHeight = SettingsHeroTotalHeight
/** Where the chip bar rests below the hero (v42 — the Category pill is
 *  inside the banner now, so the bar sits directly under it). */
private val DatabaseChipBarRestTop = DatabaseHeroTotalHeight + 4.dp
/** Where the chip bar pins when scrolled — just below the hero. */
private val DatabaseChipBarPinnedTop = DatabaseHeroTotalHeight + 2.dp
/** Scroll distance (dp) before the chip bar fully pins (Cabinet pill style). */
private val DatabaseChipStickyThreshold = 56.dp
/** The chip bar's layout height — scroll content starts below it. */
private val DatabaseChipBarHeight = 52.dp
/** Rows scrolled before the floating back-to-top arrow appears (≈ one
 *  full screen — each row is roughly 70dp tall). */
/** v292g — page size for Topic Database pagination. */
private const val PAGE_SIZE = 100

/** Rows scrolled before the floating back-to-top arrow appears (≈ one
 *  full screen — each row is roughly 70dp tall). */
private const val BackToTopRowThreshold = 10

/**
 * The floating category filter row — the Cabinet's sticky chip bar,
 * drawn ON TOP of the scroll content. As the list scrolls the row lifts a
 * few dp to pin just below the hero's ragged tear, and every pill pops on
 * its own — scale 0.90 → 1.0, staggered left→right, with the label
 * blooming toward its accent (v26).
 */
@Composable
private fun BoxScope.DatabaseStickyChipBar(
    // v245 — when Liquid glass is on, every chip renders as a clear
    // refracting capsule over this LOCAL page capture.
    glassBackdrop: LayerBackdrop? = null,
    listState: LazyListState,
    catalog: List<Pair<CurioCategory, List<CurioTopic>>>,
    totalTopics: Int,
    selectedCat: CategoryId?,
    onSelectAll: () -> Unit,
    onSelectCategory: (CategoryId) -> Unit
) {
    val thresholdPx = with(LocalDensity.current) { DatabaseChipStickyThreshold.toPx() }
    val barBottomPx = with(LocalDensity.current) { (DatabaseChipBarRestTop + DatabaseChipBarHeight).toPx() }
    val progress by remember {
        derivedStateOf {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (first == null) 0f
            else ((barBottomPx - first.offset) / thresholdPx).coerceIn(0f, 1f)
        }
    }
    val frostShift = FastOutSlowInEasing.transform(progress)
    val liftPx = with(LocalDensity.current) { (DatabaseChipBarRestTop - DatabaseChipBarPinnedTop).toPx() }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .offset(y = DatabaseChipBarRestTop)
            .graphicsLayer {
                translationY = -liftPx * frostShift
            }
    ) {
        item("all") {
            DatabaseChipPop(
                index = 0,
                frostShift = frostShift
            ) { popProgress ->
                DatabaseFilterChip(
                    label = "All",
                    count = totalTopics,
                    accent = MaterialTheme.colorScheme.primary,
                    selected = selectedCat == null,
                    onClick = onSelectAll,
                    popProgress = popProgress,
                    glass = glassBackdrop != null,
                    glassBackdrop = glassBackdrop
                )
            }
        }
        itemsIndexed(catalog, key = { _, pair -> pair.first.id.name }) { i, (cat, list) ->
            DatabaseChipPop(
                index = i + 1,
                frostShift = frostShift
            ) { popProgress ->
                DatabaseFilterChip(
                    label = cat.displayName,
                    count = list.size,
                    accent = cat.themedAccent(),
                    selected = selectedCat == cat.id,
                    onClick = { onSelectCategory(cat.id) },
                    popProgress = popProgress,
                    glass = glassBackdrop != null,
                    glassBackdrop = glassBackdrop
                )
            }
        }
    }
}

/** Per-pill pop — each chip rests at full size and pops subtly (1.0 → 1.05)
 *  as the bar lifts to pin (v29: the old 0.90 rest scale made the pills
 *  look like they were GROWING on entry — they now start full-size and
 *  only breathe a touch when the bar actually pins). */
@Composable
private fun DatabaseChipPop(
    index: Int,
    frostShift: Float,
    content: @Composable (popProgress: Float) -> Unit
) {
    val stagger = (index * 0.07f).coerceAtMost(0.85f)
    val pillProgress = ((frostShift - stagger) / (1f - stagger)).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(pillProgress)
    val pillScale = androidx.compose.ui.util.lerp(1f, 1.05f, eased)
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = pillScale
            scaleY = pillScale
        }
    ) {
        content(eased)
    }
}

/**
 * Best-effort publication/birth year for sorting — v135: shared
 * [CurioTopic.publicationYear] (the reveal's decade tag chip uses the same
 * extraction, so the two surfaces can never drift). Unknowns sort last,
 * alphabetically within that bucket.
 */
private fun topicYear(topic: CurioTopic): Int? = topic.publicationYear()

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

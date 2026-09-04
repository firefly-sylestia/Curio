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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.curio.app.features.settings.settingsHeroContentTopHeight
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
    // v-tablet — wide windows engage the master-detail layout (two-up
    // result grid + reveal pane); this single gate drives it all.
    val wide = windowWidthSizeClass().isWide
    val contentTop = settingsHeroContentTopHeight() +
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
    // v328 — the search needle is DEBOUNCED: the old code re-ran the
    // full-catalog fuzzy scan (every topic × edit distance, on
    // Dispatchers.Default) on EVERY keystroke, which is exactly what made
    // results feel slow and "loading" as you typed. The heavy scans below
    // key on [needle], which only updates ~200ms after the user pauses —
    // and the results are capped at [SEARCH_RESULT_CAP] topics, so a
    // broad query returns instantly instead of sorting thousands of hits.
    val typedNeedle = searchQuery.trim().lowercase()
    var needle by remember { mutableStateOf("") }
    LaunchedEffect(typedNeedle) {
        if (typedNeedle.isEmpty()) needle = ""  // exiting search clears instantly
        else {
            kotlinx.coroutines.delay(SEARCH_DEBOUNCE_MS)
            // Ignore stale runs: only apply if the query hasn't changed
            // again while we waited (keeps results honest while typing).
            if (typedNeedle == searchQuery.trim().lowercase()) needle = typedNeedle
        }
    }
    // v-tablet — the reveal pane's selected topic. Tapping a row on wide
    // previews it here instead of navigating; phones navigate as before.
    var paneTopic by remember { mutableStateOf<CurioTopic?>(null) }
    // A stale preview of a topic the new query/filter no longer shows is
    // more confusing than no preview — clear it whenever the needle or the
    // active lane set changes.
    LaunchedEffect(needle, effectiveCats) { paneTopic = null }
    val onTopicTap: (CurioTopic) -> Unit = { topic ->
        if (wide) paneTopic = topic
        else navController.navigate(
            CurioRoutes.revealForBrowse(topic.categoryId.routeSlug, topic.name)
        ) { launchSingleTop = true }
    }

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
                fuzzyContainsOnWords(dn, needle)
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
    // v333 — ONE catalog scan per settled query produces BOTH the result
    // rows and the per-lane stats (panel counts + "Also in" pills). The old
    // code ran two independent full-catalog scans per query (one for counts,
    // one for rows) and re-split every topic's fields per scan — a big slice
    // of the "results feel slow" cost. Results are split into two labelled
    // groups: EXACT (substring) matches first, then SIMILAR (typo-tolerant)
    // matches — both searches shown, exact on top.
    val topicsByCat = remember(indexedTopics) { indexedTopics.groupBy { it.category.id } }
    val searchPass by produceState<SearchPass>(
        initialValue = SearchPass(rows = emptyList()),
        catalog,
        topicsByCat,
        indexedTopics,
        effectiveCats,
        needle,
        doneTopics
    ) {
        value = withContext(Dispatchers.Default) {
            val indexById = indexedTopics.associateBy { it.topic.id }
            if (needle.isEmpty()) {
                // BROWSE MODE: per-lane with section headers (unchanged).
                val rows = buildList {
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
                SearchPass(rows = rows)
            } else {
                // SEARCH MODE — v313 filter: results come ONLY from the
                // selected lanes (or every lane when none are selected); the
                // per-lane stats still scan EVERY lane so the panel counts
                // and "Also in" pills stay accurate.
                val exact = ArrayList<RankedHit>()
                val similar = ArrayList<RankedHit>()
                val laneTotals = HashMap<CategoryId, Int>()
                val laneExactTitles = HashMap<CategoryId, Int>()
                catalog.forEach { (cat, topics) ->
                    val included = effectiveCats.isEmpty() || cat.id in effectiveCats
                    topics.mapNotNull { indexById[it.id] }.forEach { indexed ->
                        val level = matchLevel(indexed, needle)
                        if (level != null) {
                            val rank = titleRank(indexed, needle)
                            laneTotals[cat.id] = (laneTotals[cat.id] ?: 0) + 1
                            if (rank <= 2) {
                                laneExactTitles[cat.id] = (laneExactTitles[cat.id] ?: 0) + 1
                            }
                            if (included) {
                                val hit = RankedHit(
                                    indexed = indexed,
                                    priority = indexed.category.id in priorityCats,
                                    titleRank = rank
                                )
                                if (level == 0) exact += hit else similar += hit
                            }
                        }
                    }
                }
                // v333 — title-first inside BOTH groups: exact title →
                // startsWith → title contains → typo-tolerant title → matched
                // only in other fields, so whoever has the matching title
                // shows first.
                val groupOrder = compareBy<RankedHit> { it.titleRank }
                    .thenBy { !it.priority }
                    .thenBy { it.indexed.topic.name.lowercase() }
                exact.sortWith(groupOrder)
                similar.sortWith(groupOrder)
                val exactShown = exact.take(SEARCH_RESULT_CAP)
                val similarShown = similar.take((SEARCH_RESULT_CAP - exactShown.size).coerceAtLeast(0))
                val rows = buildList {
                    fun rowOf(hit: RankedHit): DatabaseRow {
                        val indexed = hit.indexed
                        return DatabaseRow(
                            key = indexed.topic.id,
                            topic = indexed.topic,
                            done = "${indexed.category.id.name}::${indexed.topic.name}" in doneTopics
                        )
                    }
                    if (exactShown.isNotEmpty()) {
                        add(
                            DatabaseRow(
                                key = "grp-exact",
                                groupHeader = SearchGroupHeader("Exact matches", exactShown.size)
                            )
                        )
                        exactShown.forEach { add(rowOf(it)) }
                    }
                    if (similarShown.isNotEmpty()) {
                        add(
                            DatabaseRow(
                                key = "grp-similar",
                                groupHeader = SearchGroupHeader("Similar matches", similarShown.size)
                            )
                        )
                        similarShown.forEach { add(rowOf(it)) }
                    }
                }
                SearchPass(
                    rows = rows,
                    laneHits = laneTotals,
                    laneExactTitles = laneExactTitles
                )
            }
        }
    }
    // The unified row list the rest of the screen consumes (pagination,
    // scroll restore and the alphabet rail all read `rows` unchanged).
    val rows = searchPass.rows
    // The dynamic chip data (full per-lane totals while browsing; per-lane
    // hit counts while searching) — feeds the category panel's counts.
    val chips: List<Pair<CurioCategory, Int>> = remember(catalog, searchPass, needle) {
        if (needle.isEmpty()) catalog.map { it.first to it.second.size }
        else catalog.mapNotNull { (cat, _) -> searchPass.laneHits[cat.id]?.let { cat to it } }
    }
    // v333 — the "Also in" pill lanes, TITLE-aware: lanes that own a topic
    // whose TITLE matches the query rank first (then by hit count), so the
    // category the user is searching for is never buried under high-count
    // fuzzy lanes. Hoisted to the composable scope (like `chips`) because
    // remember() isn't legal inside the LazyColumn content DSL.
    val otherLanes: List<Pair<CurioCategory, Int>> = remember(searchPass, needle, effectiveCats) {
        if (needle.isEmpty()) emptyList()
        else searchPass.laneHits.entries
            .asSequence()
            .filter { (id, n) -> n > 0 && (effectiveCats.isEmpty() || id !in effectiveCats) }
            .sortedWith(
                compareByDescending<Map.Entry<CategoryId, Int>> { e ->
                    searchPass.laneExactTitles[e.key] ?: 0
                }
                    .thenByDescending { it.value }
                    .thenBy { CurioCategories.byId(it.key).displayName }
            )
            .take(8)
            .mapNotNull { (id, n) ->
                CurioCategories.all.firstOrNull { it.id == id }?.let { it to n }
            }
            .toList()
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
        rows.filter { it.section != null || it.groupHeader != null || it.key in pageTopicKeys }
    }
    // v-tablet — the display list drives BOTH layouts: wide windows merge
    // consecutive topic rows into two-up pairs (the multi-column grid),
    // phones pass rows through unchanged (each topic keeps its own slot,
    // so the phone list is byte-identical to before).
    val displayRows = remember(paginatedRows, wide) {
        if (wide) buildWideRows(paginatedRows)
        else paginatedRows.map {
            DatabaseWideRow(
                key = it.key,
                section = it.section,
                sectionCount = it.sectionCount,
                groupHeader = it.groupHeader,
                first = it.topic,
                firstDone = it.done
            )
        }
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
    // v318b — flipping to another PAGE via the floating page nav also
    // auto-scrolls back to the top (each page's row set starts fresh).
    LaunchedEffect(currentPage) {
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
        // v-tablet — wide windows split the page into a MASTER column (the
        // scrolling results, two-up on wide) and a REVEAL pane beside it;
        // phones keep the full-width list exactly as before. The master
        // stays in PLAIN Box scope (no Row wrapper) so the overlays' align
        // + AnimatedVisibility calls resolve exactly like the phone layout;
        // on wide the master reserves the pane's lane via end padding.
        ScreenEntrance {
            Box(modifier = Modifier.fillMaxSize()) {
                // ── Master column — the scrolling result list ────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Wide: reserve the reveal pane's lane on the right
                        // (344dp pane + 12dp gap + 28dp page margin) so the
                        // overlays align to the LIST, not the whole window.
                        .padding(end = if (wide) 384.dp else 0.dp)
                ) {
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
                    start = if (wide) 16.dp else wideContentEdgePadding(),
                    end = if (wide) 12.dp else wideContentEdgePadding(),
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
                    // v313/v314 — SEARCH suggestions: pills for the lanes that
                    // also match the query. With lanes selected they list the
                    // OTHER lanes whose results hide behind the filter; from
                    // ALL (v316b) they show the lanes the flat results came
                    // from — so results never hide and you can always see /
                    // jump to which categories matched. Tap = toggle that lane
                    // into the active set.
                    // v333 — ordering is now TITLE-aware: lanes that own a
                    // topic whose TITLE matches the query rank first (then by
                    // hit count), so the category the user is searching for is
                    // never buried under high-count fuzzy lanes.
                    if (needle.isNotEmpty() && otherLanes.isNotEmpty()) {
                        item(key = "search-suggestions") {
                            // v318b — tapping an "Also in" pill SWITCHES the
                            // filter to that single lane (replacing the
                            // selection); no more silent add-to-set surprise.
                            SearchSuggestionRow(
                                hits = otherLanes,
                                onSelect = { id ->
                                    commitCats { setOf(id) }
                                }
                            )
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
                        displayRows,
                        key = { it.key },
                        // v49 — section headers and topic rows reuse their own
                        // LazyColumn slots instead of being treated as one
                        // interchangeable item type, so fast wheel scrolling
                        // over a 16k-row catalog doesn't churn slot types.
                        contentType = { row ->
                            if (row.section != null || row.groupHeader != null) "section" else "topic"
                        }
                    ) { row ->
                        when {
                            // v333 — the search group dividers (Exact matches /
                            // Similar matches) render like the section headers.
                            row.groupHeader != null -> SearchGroupHeaderRow(
                                label = row.groupHeader.label,
                                count = row.groupHeader.count
                            )
                            row.section != null -> DatabaseSectionHeader(
                                cat = row.section,
                                count = row.sectionCount
                            )
                            row.first != null -> DatabaseTopicRowSlot(
                                first = row.first,
                                firstDone = row.firstDone,
                                second = row.second,
                                secondDone = row.secondDone,
                                // Wide: preview in the reveal pane; phones
                                // navigate to the read-only reveal route.
                                onTopicTap = onTopicTap
                            )
                        }
                    }
                }
                }
            }
            // ── Master-scoped overlays — live over the LIST, clear of the
            //    reveal pane on wide ─────────────────────────────────────
            // Side scroll indicator — speed-scrolling knob (v26) with the
            // A–Z fast-scroller rail (tap it to open, tap a letter to jump).
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
                .padding(top = settingsHeroContentTopHeight() + 74.dp)
        ) {
            Surface(
                onClick = {
                    savedScrollIndex = 0
                    savedScrollOffset = 0
                    // v318b — smooth auto-scroll to the top (was an instant
                    // jump, which felt abrupt on a long list).
                    alphabetScope.launch { listState.animateScrollToItem(0) }
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
                } // ── master Box close ────────────────────────────────────

                // ── v-tablet — REVEAL pane: right lane beside the master
                // (master-detail); a quiet placeholder invites the first
                // tap. Phone: absent — rows navigate as before.
                if (wide) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 28.dp)
                    ) {
                        DatabaseRevealPaneSlot(
                            selected = paneTopic,
                            doneTopics = doneTopics,
                            contentTop = contentTop,
                            onClose = { paneTopic = null },
                            onOpen = onTopicTap
                        )
                    }
                }
            } // ── stage Box close (master + pane siblings) ─────────────────
        } // ── ScreenEntrance close ─────────────────────────────────────────
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
    val year: Int?,
    // v333 — pre-split word lists for the fuzzy (typo-tolerant) pass. The
    // old code re-split name/byline/subtype with a Regex on EVERY search
    // settle (thousands of topics × 3 fields × per keystroke), which was a
    // big slice of the "results feel slow" cost. Words are split ONCE per
    // topic at index build; the per-query fuzzy check is then pure word
    // comparisons over these retained lists.
    val nameWords: List<String> = splitSearchWords(nameKey),
    val bylineWords: List<String> = splitSearchWords(bylineKey),
    val subtypeWords: List<String> = splitSearchWords(subtypeKey)
)

/** v333 — a labelled search-result group header row ("Exact matches" /
 *  "Similar matches") inserted between result groups while searching. */
private data class SearchGroupHeader(
    val label: String,
    val count: Int
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
    val done: Boolean = false,
    // v333 — search group header rows (exact / similar). Non-null means the
    // row is a labelled divider between the two result groups.
    val groupHeader: SearchGroupHeader? = null
)

/** One search hit with its ranking inputs — title relevance + lane mention. */
private data class RankedHit(
    val indexed: IndexedTopic,
    val priority: Boolean,
    val titleRank: Int
)

/** v333 — one search pass result: the labelled row groups (already sorted &
 *  capped) plus per-lane stats that feed the filter-panel counts and the
 *  "Also in" pills — computed in the SAME catalog scan so the search runs
 *  once per settled query instead of twice. */
private data class SearchPass(
    val rows: List<DatabaseRow>,
    val laneHits: Map<CategoryId, Int> = emptyMap(),
    val laneExactTitles: Map<CategoryId, Int> = emptyMap()
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

/** v333 — splits a lowercase search key into words ONCE at index build (see
 *  [IndexedTopic]); the fuzzy pass compares tokens against these retained
 *  lists instead of re-splitting every field with a Regex on every settled
 *  query — re-splitting thousands of topics × 3 fields per keystroke was a
 *  big slice of the "results feel slow" cost. */
private val SearchWordSplit = Regex("[^a-z0-9]+")

/** Splits a lowercase key into words (called once per topic at index build). */
private fun splitSearchWords(key: String): List<String> =
    if (key.isBlank()) emptyList() else key.split(SearchWordSplit).filter { it.isNotBlank() }

/** Typo-tolerant fallback — bounded to sane query lengths so the fuzzy pass
 *  over the catalog stays cheap (it runs off the UI thread in produceState). */
private fun fuzzyTopicMatch(t: IndexedTopic, needle: String): Boolean {
    if (needle.length < 3 || needle.length > 48) return false
    return fuzzyContainsWords(t.nameWords, needle) ||
        fuzzyContainsWords(t.bylineWords, needle) ||
        fuzzyContainsWords(t.subtypeWords, needle)
}

/** All query tokens must fuzzy-match some word in the word list ("hary
 *  ptter" → harry + potter: transposed/skipped letters still hit). */
private fun fuzzyContainsWords(words: List<String>, needle: String): Boolean {
    if (words.isEmpty()) return false
    val tokens = needle.split(' ').filter { it.isNotBlank() }
    if (tokens.size > 1) return tokens.all { tok -> words.any { fuzzyWord(it, tok) } }
    return words.any { fuzzyWord(it, needle) }
}

/** v333 — title relevance: how directly the TOPIC NAME answers the query
 *  (0 exact → 3 typo-tolerant title → 4 matched only in other fields). The
 *  search sorts by this FIRST inside both result groups, so whoever has the
 *  matching title shows first. */
private fun titleRank(t: IndexedTopic, needle: String): Int = when {
    t.nameKey == needle -> 0
    t.nameKey.startsWith(needle) -> 1
    t.nameKey.contains(needle) -> 2
    fuzzyContainsWords(t.nameWords, needle) -> 3
    else -> 4
}

/** String convenience for callers without a prebuilt word list (lane-name
 *  priority matching) — splits on the fly; only called for ~36 lane names. */
private fun fuzzyContainsOnWords(hay: String, needle: String): Boolean =
    fuzzyContainsWords(splitSearchWords(hay), needle)

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
/** The active-filter chips row's layout height — content starts below it. */
private val DatabaseChipRowHeight = 52.dp
/** The category PANEL's layout height — search header + scrollable checkbox
 *  list (~7-8 lanes visible, then it scrolls). */
private val DatabaseFilterPanelHeight = 352.dp
/** Rows scrolled before the floating back-to-top arrow appears (≈ one
 *  full screen — each row is roughly 70dp tall). */
/** v292g — page size for Topic Database pagination. */
private const val PAGE_SIZE = 100

/** v328 — search results are capped at the best 50 (a broad query returns
 *  instantly; typing further narrows them). Browse mode still pages all
 *  topics at [PAGE_SIZE] per page. */
private const val SEARCH_RESULT_CAP = 50
/** v328 — pause before the full-catalog search scan fires (typing feeds the
 *  field live; the expensive scan waits for the query to settle). */
private const val SEARCH_DEBOUNCE_MS = 200L

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
            .offset(y = settingsHeroContentTopHeight() + 4.dp)
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
        // v328 — the panel surface now follows the theme (it used the bare
        // surfaceContainerHigh, which in the Curio LIGHT scheme is a warm tan
        // that read as a cream block even in dark-adjacent styles): the dialog
        // container is a dark lifted surface at night and a proper elevated
        // surface in light.
        color = com.curio.app.ui.theme.curioDialogContainerColor(),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .offset(y = settingsHeroContentTopHeight() + 4.dp)
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
                // v3xx14 — TWO-column checkbox grid (was one long list); the
                // fixed 276dp max-height keeps the panel the same footprint,
                // now roughly half the scroll depth.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 276.dp)
                ) {
                    gridItems(shown) { cat ->
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
    val accent = cat.themedAccent()
    // v328 — CHECKED rows get a visible category-tinted FILL (the row used
    // to stay transparent, so only the tiny checkbox showed selection). The
    // wash is theme-aware: a soft accent tint over the panel surface in
    // light, a deeper accent lift over the dark panel in dark mode, so the
    // row reads as selected in BOTH themes.
    val panelBase = com.curio.app.ui.theme.curioDialogContainerColor()
    val rowFill = if (checked) lerp(panelBase, accent, 0.22f) else panelBase
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowFill)
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
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
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (checked) FontWeight.ExtraBold else FontWeight.Normal
            ),
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
 * results when lanes are selected (or searching from All) and other lanes
 * also match the query.
 * v318b — tapping a pill SWITCHES the filter to that single lane (replacing
 * the selection entirely), so one tap jumps to the other category instead of
 * silently stacking another filter.
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

/**
 * v333 — one search-result group divider ("EXACT MATCHES · 12" / "SIMILAR
 * MATCHES · 3") above the relevant rows: shares the section-header cadence
 * but carries a neutral primary label instead of a category dot, so the
 * two-tier search reads as Exact matches first, typo-tolerant Similar
 * matches below.
 */
@Composable
private fun SearchGroupHeaderRow(label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            // v27r — the section header sits higher (was 14dp top padding),
            // tucking the label against the previous row.
            .padding(top = 6.dp, bottom = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
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

// ═══════════════════════════════════════════════════════════════════════
// v-tablet — MASTER-DETAIL: the two-up result grid + the reveal pane
// ═══════════════════════════════════════════════════════════════════════

/** v-tablet — one slot in the result list: a section/group header (full
 *  width) or a topic that fills one grid cell ([second] == null) or two
 *  cells side by side. Phones build this 1:1 from the legacy rows, so the
 *  compact list is unchanged. */
private data class DatabaseWideRow(
    val key: String,
    val section: CurioCategory? = null,
    val sectionCount: Int = 0,
    val groupHeader: SearchGroupHeader? = null,
    val first: CurioTopic? = null,
    val firstDone: Boolean = false,
    val second: CurioTopic? = null,
    val secondDone: Boolean = false
)

/** v-tablet — merges consecutive topic rows into two-up pairs (the
 *  multi-column grid on wide windows); section/group headers keep their own
 *  full-width slots so the category rhythm and the search group dividers
 *  survive. */
private fun buildWideRows(rows: List<DatabaseRow>): List<DatabaseWideRow> {
    val out = ArrayList<DatabaseWideRow>(rows.size)
    var pending: DatabaseRow? = null
    for (r in rows) {
        if (r.topic == null) {
            pending?.let { p ->
                out += DatabaseWideRow(key = "t-${p.key}", first = p.topic, firstDone = p.done)
                pending = null
            }
            out += DatabaseWideRow(
                key = r.key,
                section = r.section,
                sectionCount = r.sectionCount,
                groupHeader = r.groupHeader
            )
        } else if (pending == null) {
            pending = r
        } else {
            val p = pending
            pending = null
            out += DatabaseWideRow(
                key = "p-${p.key}|${r.key}",
                first = p.topic, firstDone = p.done,
                second = r.topic, secondDone = r.done
            )
        }
    }
    pending?.let { p ->
        out += DatabaseWideRow(key = "t-${p.key}", first = p.topic, firstDone = p.done)
    }
    return out
}

/** v-tablet — renders one result slot: a full-width row on phones, a
 *  two-up pair on wide. Tapping always routes through [onTopicTap]. */
@Composable
private fun DatabaseTopicRowSlot(
    first: CurioTopic,
    firstDone: Boolean,
    second: CurioTopic?,
    secondDone: Boolean,
    onTopicTap: (CurioTopic) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            DatabaseTopicRow(
                cat = CurioCategories.byId(first.categoryId),
                topic = first,
                done = firstDone,
                onClick = { onTopicTap(first) }
            )
        }
        if (second != null) {
            Box(modifier = Modifier.weight(1f)) {
                DatabaseTopicRow(
                    cat = CurioCategories.byId(second.categoryId),
                    topic = second,
                    done = secondDone,
                    onClick = { onTopicTap(second) }
                )
            }
        }
    }
}

/** v-tablet — the reveal pane host: a quiet placeholder invites the first
 *  tap; a selected topic renders the editorial preview panel. */
@Composable
private fun DatabaseRevealPaneSlot(
    selected: CurioTopic?,
    doneTopics: Set<String>,
    contentTop: Dp,
    onClose: () -> Unit,
    onOpen: (CurioTopic) -> Unit
) {
    val t = selected
    if (t == null) {
        Box(
            modifier = Modifier
                .width(344.dp)
                .fillMaxHeight()
                .padding(top = contentTop, bottom = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    CurioIcon(
                        CurioIcons.Search, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        size = 40.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Select a topic",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Preview it here, then open the full reveal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        DatabaseRevealPane(
            cat = CurioCategories.byId(t.categoryId),
            topic = t,
            done = "${t.categoryId.name}::${t.name}" in doneTopics,
            contentTop = contentTop,
            onClose = onClose,
            onOpen = { onOpen(t) }
        )
    }
}

/** v-tablet — the editorial reveal pane: category identity, title, byline,
 *  teaser, synopsis and tags in a quiet panel, with an Open CTA that routes
 *  to the real (read-only) reveal. */
@Composable
private fun DatabaseRevealPane(
    cat: CurioCategory,
    topic: CurioTopic,
    done: Boolean,
    contentTop: Dp,
    onClose: () -> Unit,
    onOpen: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .width(344.dp)
            .fillMaxHeight()
            .padding(top = contentTop, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Pane header — category chip + close ──────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        CurioIcon(
                            cat.iconGlyph, null,
                            tint = cat.categoryInk(),
                            size = 14.dp
                        )
                        Text(
                            cat.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = cat.categoryInk()
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CurioIcon(
                            CurioIcons.Close, "Close preview",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 18.dp
                        )
                    }
                }
            }
            // ── Scrollable body ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    topic.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val meta = listOfNotNull(
                    topic.byline.takeIf { it.isNotBlank() },
                    topic.subtype.takeIf { it.isNotBlank() }
                )
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        meta.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (done) {
                    Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    topic.teaser,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                topic.synopsis?.takeIf { it.isNotBlank() }?.let { syn ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        syn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (topic.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        topic.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            // ── CTA footer ───────────────────────────────────────────────
            Surface(
                onClick = onOpen,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    CurioIcon(
                        CurioIcons.OpenInNew, null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 16.dp
                    )
                    Text(
                        "Open topic",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

package com.curio.app.features.cabinet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureData
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCollection
import com.curio.app.data.CurioCollectionMember
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicCatalog
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.matchesSavedName
import com.curio.app.data.matchesSavedNameStrict
import com.curio.app.data.shortName
import com.curio.app.features.reveal.AlbumArtFetch
import com.curio.app.features.reveal.SeriesPosterFetch
import com.curio.app.features.settings.BookCoverFetch
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioEmptyState
import com.curio.app.ui.components.CurioEntryCard
import com.curio.app.ui.components.CurioHoldPill
import com.curio.app.ui.components.CurioTwoStepDeleteDialog
import com.curio.app.ui.components.isLiquidGlassPillsActive
import com.curio.app.ui.components.liquidGlassCapsule
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.themedAccent
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.util.UUID

/**
 * v3xx — CABINET v2 (collections): the experimental Cabinet behind
 * Settings → Experiments → "Cabinet v2 collections".
 *
 * One page, three levels reached from the torn Cabinet hero:
 *  - HOME — the collections surface (5.1): an "Everything" card (3×2 cover
 *    collage + count) plus one card per user collection (same collage of
 *    its members' covers, name, count), a "+ New collection" tile, and a
 *    "From a moodboard…" path in the create sheet when a GalleryWall
 *    capture exists.
 *  - EVERYTHING — the classic v2 content: saved captures (format filter
 *    chips + long-press multi-select batch delete) + liked books/series/
 *    albums rows.
 *  - COLLECTION DETAIL — one folder: its members (pinned topics as rows,
 *    saved entries as cards) with long-press Move up/down/Remove, an Add
 *    pill (multi-select saved entries), rename + delete via the kebab.
 *
 * The hero is the SAME torn Cabinet banner as the classic view (reused
 * [CabinetHeroHeader]), so the two views share chrome; search filters the
 * active level; the liquid-glass capture feeds the hero pills.
 */
@Composable
fun CabinetV2Content(navController: NavController) {
    val context = LocalContext.current
    val wide = windowWidthSizeClass().isWide
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()

    // ── Glass plumbing: the grid records into a LOCAL backdrop the hero
    // pills blur (sibling overlay outside the captured subtree).
    val glassOn = isLiquidGlassPillsActive()
    val glassBackdrop = if (glassOn) rememberLayerBackdrop() else null

    // ── Data: saved entries (repo flow) + liked books/series/albums +
    // user collections.
    val entries by androidx.compose.runtime.produceState<List<CurioEntry>>(initialValue = emptyList()) {
        try {
            CurioRepositoryHolder.repo.observeAll().collect { value = it }
        } catch (_: Exception) {
            value = emptyList()
        }
    }
    val entriesById = remember(entries) { entries.associateBy { it.id } }
    val books = remember(AppPreferences.bookFavoritesState, AppPreferences.bookCoverUrlsState) {
        AppPreferences.getBookFavorites(context)
            .map { name -> V2Liked(name, V2Kind.BOOK, findLikedTopic(V2Kind.BOOK, name)) }
            .sortedBy { it.name.lowercase() }
    }
    val albums = remember(AppPreferences.albumFavTracksState) {
        AppPreferences.albumFavTracksState.keys
            .filter { it.isNotBlank() }
            .map { name -> V2Liked(name, V2Kind.ALBUM, findLikedTopic(V2Kind.ALBUM, name)) }
            .sortedBy { it.name.lowercase() }
    }
    val series = remember(AppPreferences.seriesFavoritesState) {
        AppPreferences.getSeriesFavorites(context)
            .map { name -> V2Liked(name, V2Kind.SERIES, findLikedTopic(V2Kind.SERIES, name)) }
            .sortedBy { it.name.lowercase() }
    }
    val collections = AppPreferences.collectionsState

    // ── Seed the four empty starter shelves (Currently Reading / Want to
    // Read / Completed / Personal) once — they become ordinary, editable
    // collections (ids prefixed `shelf:`); the virtual shelves (Favorites /
    // Saved entries / Notes) are computed below and never persisted.
    LaunchedEffect(Unit) { AppPreferences.seedCabinetShelves(context) }

    // ── Shelf plumbing.
    val seededById = remember(collections) {
        collections.filter { it.id in seededShelfIds }.associateBy { it.id }
    }
    val userCollections = remember(collections) {
        collections.filterNot { it.id in seededShelfIds }
    }
    val allLikes = remember(books, albums, series) { books + albums + series }
    val noteEntries = remember(entries) { entries.filter { it.format in noteFormats } }

    // v3xx — the COVER CACHE warmer: liked books / albums / series resolve
    // their cover art ALWAYS (not gated on the Settings fetch toggles — the
    // cache is the point: URL persisted + image bytes on disk), so the
    // Cabinet grid fills with real covers on first open and loads them
    // INSTANTLY from the local file on every later visit. Throttled per
    // recomposition to a small batch so a big shelf never bursts the pipe.
    val unwarmedLikes = remember(allLikes) {
        allLikes.filter {
            CabinetCoverCache.persistedUrl(
                context,
                CabinetCoverCache.CoverKind.valueOf(it.kind.name),
                it.name
            ) == null
        }
    }
    LaunchedEffect(unwarmedLikes.size) {
        if (unwarmedLikes.isEmpty()) return@LaunchedEffect
        var warmed = 0
        for (item in unwarmedLikes) {
            if (warmed >= 30) break
            CabinetCoverCache.ensureLocalCover(
                context,
                CabinetCoverCache.CoverKind.valueOf(item.kind.name),
                item.name,
                item.topic?.byline,
                item.topic?.imageUrl
            )
            warmed++
        }
    }
    val shelfCounts = remember(allLikes.size, entries.size, noteEntries.size, seededById) {
        mapOf(
            V2ShelfId.FAVORITES to allLikes.size,
            V2ShelfId.CURRENTLY_READING to (seededById["shelf:currently-reading"]?.members?.size ?: 0),
            V2ShelfId.WANT_TO_READ to (seededById["shelf:want-to-read"]?.members?.size ?: 0),
            V2ShelfId.SAVED to entries.size,
            V2ShelfId.COMPLETED to (seededById["shelf:completed"]?.members?.size ?: 0),
            V2ShelfId.NOTES to noteEntries.size,
            V2ShelfId.PERSONAL to (seededById["shelf:personal"]?.members?.size ?: 0)
        )
    }

    // ── Level: "" = collections home · "everything" = the library page
    // · "shelf:*" = a virtual shelf detail · anything else = a collection
    // id (its detail).
    var openLevel by rememberSaveable { mutableStateOf("") }
    val openCollection = collections.firstOrNull { it.id == openLevel }

    // ── Search — filters the active level (collection names on home,
    // members in a detail, everything on the Everything page).
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) searchFocus.requestFocus()
    }
    fun matchesQ(text: String): Boolean {
        val q = searchQuery.trim()
        return q.isEmpty() || text.contains(q, ignoreCase = true)
    }

    // ── Everything-level state: type filter (the JSX filter rail) + sort.
    // (v3xx — the old grid/list view toggle is gone: Everything is always
    // the 3-column media grid, so there is no second view mode to store.)
    var typeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sortAtoZ by rememberSaveable { mutableStateOf(false) }

    // ── Everything-level filters: search + type + sort.
    val filteredEntries = remember(entries, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) entries
        else entries.filter {
            matchesQ(it.topic.name) || it.title?.let { t -> matchesQ(t) } == true ||
                it.tags.any { tag -> matchesQ(tag) }
        }
    }
    val shownEntries = remember(filteredEntries, typeFilter, sortAtoZ) {
        val base = filteredEntries.filter { entryInType(it, typeFilter) }
        if (sortAtoZ) base.sortedBy { it.topic.name.lowercase() }
        else base.sortedByDescending { it.capturedAtMillis }
    }
    fun filteredLiked(all: List<V2Liked>): List<V2Liked> =
        if (searchQuery.isBlank()) all
        else all.filter {
            matchesQ(it.name) || (it.topic?.byline?.let { b -> matchesQ(b) } == true)
        }
    // v3xx — liked media order by the LIKED timestamp (Recent) or name
    // (A–Z): the Everything "All" view reads as a proper media library, not
    // an alphabetical list glued to the sort toggle.
    val likedAtMap = AppPreferences.likedAtState
    fun likedAtFor(item: V2Liked): Long =
        likedAtMap["${item.kind.name.lowercase()}|${item.name}"] ?: 0L
    val shownBooks = remember(books, searchQuery, typeFilter, sortAtoZ, likedAtMap) {
        val base = filteredLiked(books).filter { likedShownForType(typeFilter, V2Kind.BOOK) }
        if (sortAtoZ) base.sortedBy { it.name.lowercase() }
        else base.sortedByDescending { likedAtFor(it) }
    }
    val shownAlbums = remember(albums, searchQuery, typeFilter, sortAtoZ, likedAtMap) {
        val base = filteredLiked(albums).filter { likedShownForType(typeFilter, V2Kind.ALBUM) }
        if (sortAtoZ) base.sortedBy { it.name.lowercase() }
        else base.sortedByDescending { likedAtFor(it) }
    }
    val shownSeries = remember(series, searchQuery, typeFilter, sortAtoZ, likedAtMap) {
        val base = filteredLiked(series).filter { likedShownForType(typeFilter, V2Kind.SERIES) }
        if (sortAtoZ) base.sortedBy { it.name.lowercase() }
        else base.sortedByDescending { likedAtFor(it) }
    }

    // ── v3xx — the Recent rail mixes recent saves with recent LIKES. The
    // liked-at stamps live in AppPreferences (kind|name → epoch ms); entries
    // carry capturedAtMillis. One merged, newest-first feed renders both.
    val recentFeed = remember(entries, books, albums, series, likedAtMap) {
        val cells = mutableListOf<V2RecentCell>()
        entries.forEach { cells.add(V2RecentCell.Entry(it)) }
        (books + albums + series).forEach { item ->
            val ts = likedAtFor(item)
            if (ts > 0L) cells.add(V2RecentCell.Liked(item, ts))
        }
        cells.sortedByDescending { it.ts }.take(10)
    }

    // ── v3xx — the rail only lists types that actually hold content.
    val railAvailable = remember(books, albums, series, noteEntries, entries) {
        buildSet {
            if (books.isNotEmpty()) add("books")
            if (albums.isNotEmpty()) add("albums")
            if (series.isNotEmpty()) add("series")
            if (noteEntries.isNotEmpty()) add("notes")
            if (entries.any { it.format == CaptureFormat.GalleryWall }) add("moodboard")
            if (entries.any { it.format == CaptureFormat.ReelNotes }) add("review")
        }
    }

    // ── Home-level filter: shelf + collection names.
    val visibleShelves = remember(builtInShelves, shelfCounts, searchQuery) {
        builtInShelves.map { it to shelfCounts[it.id]!! }.filter { matchesQ(it.first.title) }
    }
    val shownUserCollections = remember(userCollections, searchQuery) {
        if (searchQuery.isBlank()) userCollections
        else userCollections.filter { matchesQ(it.name) }
    }

    val searching = searchActive && searchQuery.isNotBlank()

    // ── Multi-select batch (Everything entries only).
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedEntryIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    // v3xx — BACK handling: system back now walks OUT of the open pages
    // instead of popping the whole Cabinet to Home. Order: cancel the
    // selection → close search → close the open collection / Everything /
    // virtual shelf → then (openLevel == "") let the system back leave the
    // Cabinet as usual. (Declared after the selection state it reads.)
    BackHandler(enabled = selectionMode || searchActive || openLevel.isNotEmpty()) {
        when {
            selectionMode -> {
                selectionMode = false
                selectedEntryIds = emptySet()
            }
            searchActive -> {
                searchActive = false
                searchQuery = ""
            }
            else -> {
                openLevel = ""
                searchActive = false
                searchQuery = ""
                typeFilter = null
            }
        }
    }
    val visibleIds = remember(openLevel, shownEntries, entries, noteEntries) {
        when (openLevel) {
            SHELF_LEVEL_SAVED -> entries.map { it.id }.toSet()
            SHELF_LEVEL_NOTES -> noteEntries.map { it.id }.toSet()
            "everything" -> shownEntries.map { it.id }.toSet()
            "" -> entries.map { it.id }.toSet() // home Saved-entries section
            else -> emptySet()
        }
    }
    LaunchedEffect(selectedEntryIds, visibleIds) {
        val kept = selectedEntryIds.intersect(visibleIds)
        if (kept != selectedEntryIds) selectedEntryIds = kept
        if (selectedEntryIds.isEmpty() && selectionMode) selectionMode = false
    }
    val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedEntryIds }

    // ── RAW content — decides between the genuinely-empty Cabinet
    // (suggestions) and a filtered-to-nothing page. The seeded starter
    // shelves don't count: a fresh Cabinet is still "empty" until the user
    // saves or likes something.
    val rawContent = entries.isNotEmpty() || books.isNotEmpty() ||
        albums.isNotEmpty() || series.isNotEmpty() || userCollections.isNotEmpty()

    // ── Empty-Cabinet suggestions (existing v2 behavior).
    var suggestions by remember { mutableStateOf<List<CurioTopic>>(emptyList()) }
    var suggestionSeed by remember { mutableIntStateOf(0) }
    LaunchedEffect(suggestionSeed) {
        val picked = mutableListOf<CurioTopic>()
        val seen = mutableSetOf<String>()
        var guard = 0
        while (picked.size < 3 && guard < 40) {
            guard++
            val t = runCatching { TopicCatalog.randomFor(CategoryId.WILDCARD) }.getOrNull() ?: break
            if (t.name !in seen) {
                seen.add(t.name)
                picked.add(t)
            }
        }
        suggestions = picked
    }

    // ── Collection edit state.
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    var renameTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var addTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var pillTarget by remember { mutableStateOf<PillTarget?>(null) }
    // v3xx — the liked-tile ⋮ target: opens the cover-source sheet where the
    // user can switch a book/album/series to its OTHER art provider.
    var coverSourceItem by remember { mutableStateOf<V2Liked?>(null) }

    // ── Hero chrome — the SAME torn banner as the classic view.
    val heroTitle = when {
        selectionMode -> "${selectedEntryIds.size} selected"
        openCollection != null -> openCollection.name
        openLevel == SHELF_LEVEL_FAVORITES -> "Favorites"
        openLevel == SHELF_LEVEL_SAVED -> "Saved entries"
        openLevel == SHELF_LEVEL_NOTES -> "Notes"
        openLevel == "everything" -> "Everything"
        else -> "The Cabinet"
    }
    val heroSubtitle = when {
        selectionMode -> "Long-press cards to select"
        openCollection != null -> "${openCollection.members.size} item${if (openCollection.members.size == 1) "" else "s"} · tap a member to open it"
        openLevel == SHELF_LEVEL_FAVORITES -> "${allLikes.size} liked books, series & albums"
        openLevel == SHELF_LEVEL_SAVED -> "${entries.size} saved captures"
        openLevel == SHELF_LEVEL_NOTES -> "${noteEntries.size} notes & voice captures"
        openLevel == "everything" -> "Books · albums · saved captures"
        else -> "Collections · Everything · your keepsakes"
    }
    val compactBannerHeight = if (wide) CabinetHeroBannerHeightCompact else CabinetHeroBannerHeight
    val heroTotal = compactBannerHeight + CabinetHeroSheetExtent
    // The hero's search field lives INSIDE the banner (fixed height), so the
    // grid just clears the torn hero + a breathing gap.
    val contentTop = if (wide) 0.dp else heroTotal + 12.dp

    val pageAccent = MaterialTheme.colorScheme.primary

    val heroTrailing: @Composable (Color, Color) -> Unit = { ink, fill ->
        V2HeroTrailing(
            openLevel = openLevel,
            selectionMode = selectionMode,
            allVisibleSelected = allVisibleSelected,
            selectedCount = selectedEntryIds.size,
            ink = ink, fill = fill,
            glassBackdrop = glassBackdrop,
            onBack = { openLevel = ""; searchActive = false; searchQuery = "" },
            onToggleSearch = { searchActive = !searchActive },
            onRecycleBin = { navController.navigate(CurioRoutes.RECYCLE_BIN) { launchSingleTop = true } },
            onSelectAll = {
                selectedEntryIds = if (allVisibleSelected) selectedEntryIds - visibleIds
                else selectedEntryIds + visibleIds
            },
            onDelete = { if (selectedEntryIds.isNotEmpty()) showBulkDeleteConfirm = true },
            onCancelSelection = { selectionMode = false; selectedEntryIds = emptySet() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── The scrolling grid — runs UNDER the hero.
        LazyVerticalGrid(
            state = gridState,
            // v3xx — Everything is a 3-column media grid (the old 2-col + a
            // separate list toggle are gone: list was replaced by the 3 grid);
            // every other level keeps its 2-col card grid.
            columns = if (wide) GridCells.Adaptive(minSize = 176.dp)
            else if (openLevel == "everything") GridCells.Fixed(3)
            else GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentTop,
                bottom = 24.dp + 84.dp +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .then(if (glassOn && glassBackdrop != null)
                    Modifier.layerBackdrop(glassBackdrop) else Modifier)
        ) {
            if (wide) {
                item(key = "hero", span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
                    CabinetHeroHeader(
                        title = heroTitle,
                        subtitle = heroSubtitle,
                        activeCat = null,
                        legacyMode = false,
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onCloseSearch = { searchActive = false; searchQuery = "" },
                        searchFocus = searchFocus,
                        trailing = heroTrailing,
                        glassBackdrop = glassBackdrop
                    )
                }
            }

            when {
                openCollection != null -> v2DetailItems(
                    collection = openCollection,
                    entriesById = entriesById,
                    searching = searching,
                    searchQuery = searchQuery,
                    onOpenMember = { m ->
                        val t = memberLiked(m, entriesById)?.topic
                        val cat = t?.categoryId ?: m.categoryName
                            ?.let { n -> runCatching { CategoryId.valueOf(n) }.getOrNull() }
                        if (cat != null) {
                            navController.navigate(
                                CurioRoutes.revealFor(cat.routeSlug, t?.name ?: m.refName)
                            ) { launchSingleTop = true }
                        }
                    },
                    onOpenEntry = { id ->
                        navController.navigate(CurioRoutes.entryDetail(id)) { launchSingleTop = true }
                    },
                    onMemberLongPress = { index -> pillTarget = PillTarget.Member(openCollection.id, index) },
                    onAdd = { addTarget = openCollection.id },
                    onKebab = { pillTarget = PillTarget.Collection(openCollection.id) }
                )
                openLevel == SHELF_LEVEL_FAVORITES -> v2VirtualShelfItems(
                    title = "Favorites",
                    likes = allLikes,
                    entries = emptyList(),
                    searchQuery = searchQuery,
                    selectedEntryIds = selectedEntryIds,
                    selectionMode = selectionMode,
                    onOpenLiked = { item -> item.open(navController) },
                    onEntryLongClick = { id ->
                        selectionMode = true
                        selectedEntryIds = selectedEntryIds + id
                    },
                    onEntryClick = { id ->
                        if (selectionMode) {
                            selectedEntryIds = if (id in selectedEntryIds) selectedEntryIds - id
                            else selectedEntryIds + id
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            navController.navigate(CurioRoutes.entryDetail(id)) { launchSingleTop = true }
                        }
                    }
                )
                openLevel == SHELF_LEVEL_SAVED -> v2VirtualShelfItems(
                    title = "Saved entries",
                    likes = emptyList(),
                    entries = entries,
                    searchQuery = searchQuery,
                    selectedEntryIds = selectedEntryIds,
                    selectionMode = selectionMode,
                    onOpenLiked = { item -> item.open(navController) },
                    onEntryLongClick = { id ->
                        selectionMode = true
                        selectedEntryIds = selectedEntryIds + id
                    },
                    onEntryClick = { id ->
                        if (selectionMode) {
                            selectedEntryIds = if (id in selectedEntryIds) selectedEntryIds - id
                            else selectedEntryIds + id
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            navController.navigate(CurioRoutes.entryDetail(id)) { launchSingleTop = true }
                        }
                    }
                )
                openLevel == SHELF_LEVEL_NOTES -> v2VirtualShelfItems(
                    title = "Notes",
                    likes = emptyList(),
                    entries = noteEntries,
                    searchQuery = searchQuery,
                    selectedEntryIds = selectedEntryIds,
                    selectionMode = selectionMode,
                    onOpenLiked = { item -> item.open(navController) },
                    onEntryLongClick = { id ->
                        selectionMode = true
                        selectedEntryIds = selectedEntryIds + id
                    },
                    onEntryClick = { id ->
                        if (selectionMode) {
                            selectedEntryIds = if (id in selectedEntryIds) selectedEntryIds - id
                            else selectedEntryIds + id
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            navController.navigate(CurioRoutes.entryDetail(id)) { launchSingleTop = true }
                        }
                    }
                )
                openLevel == "everything" -> v2EverythingItems(
                    shownEntries = shownEntries,
                    shownBooks = shownBooks,
                    shownAlbums = shownAlbums,
                    shownSeries = shownSeries,
                    recentFeed = recentFeed,
                    railAvailable = railAvailable,
                    typeFilter = typeFilter,
                    onTypeFilter = { typeFilter = it },
                    sortAtoZ = sortAtoZ,
                    onSort = { sortAtoZ = it },
                    selectionMode = selectionMode,
                    selectedEntryIds = selectedEntryIds,
                    onEntryLongClick = { id ->
                        selectionMode = true
                        selectedEntryIds = selectedEntryIds + id
                    },
                    onEntryClick = { id ->
                        if (selectionMode) {
                            selectedEntryIds = if (id in selectedEntryIds) selectedEntryIds - id
                            else selectedEntryIds + id
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            navController.navigate(CurioRoutes.entryDetail(id)) { launchSingleTop = true }
                        }
                    },
                    onOpenLiked = { item -> item.open(navController) },
                    onCoverSource = { coverSourceItem = it },
                    onAddNew = { navController.navigateToTab(CurioRoutes.SPIN) },
                    onClearFilters = { searchQuery = ""; searchActive = false; typeFilter = null },
                    pageAccent = pageAccent
                )
                else -> v2HomeItems(
                    everythingLikes = allLikes,
                    everythingCount = entries.size + allLikes.size,
                    savedEntries = entries.sortedByDescending { it.capturedAtMillis },
                    visibleShelves = visibleShelves,
                    userCollections = shownUserCollections,
                    searching = searching,
                    selectionMode = selectionMode,
                    selectedEntryIds = selectedEntryIds,
                    onOpenEverything = { openLevel = "everything"; searchActive = false; searchQuery = "" },
                    onOpenShelf = { id ->
                        openLevel = when (id) {
                            V2ShelfId.FAVORITES -> SHELF_LEVEL_FAVORITES
                            V2ShelfId.SAVED -> SHELF_LEVEL_SAVED
                            V2ShelfId.NOTES -> SHELF_LEVEL_NOTES
                            else -> {
                                val seeded = builtInShelves.firstOrNull { it.id == id }?.seededCollectionId
                                seeded ?: ""
                            }
                        }
                        if (openLevel.isNotEmpty()) {
                            searchActive = false
                            searchQuery = ""
                        }
                    },
                    onOpenCollection = { id -> openLevel = id; searchActive = false; searchQuery = "" },
                    onCollectionLongPress = { id -> pillTarget = PillTarget.Collection(id) },
                    onNewCollection = { showCreateSheet = true },
                    suggestions = suggestions,
                    onShuffle = { suggestionSeed++ },
                    onOpenSuggestion = { t ->
                        navController.navigate(
                            CurioRoutes.revealFor(t.categoryId.routeSlug, t.name)
                        ) { launchSingleTop = true }
                    },
                    showSuggestions = !rawContent,
                    onEntryLongClick = { id ->
                        selectionMode = true
                        selectedEntryIds = selectedEntryIds + id
                    },
                    onEntryClick = { id ->
                        if (selectionMode) {
                            selectedEntryIds = if (id in selectedEntryIds) selectedEntryIds - id
                            else selectedEntryIds + id
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            navController.navigate(CurioRoutes.entryDetail(id)) { launchSingleTop = true }
                        }
                    },
                    onClearSearch = { searchQuery = ""; searchActive = false }
                )
            }
        }

        // ── The torn hero — pinned overlay on phones (wide renders it as
        // the grid's first item above).
        if (!wide) {
            CabinetHeroHeader(
                title = heroTitle,
                subtitle = heroSubtitle,
                activeCat = null,
                legacyMode = false,
                searchActive = searchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = { searchActive = false; searchQuery = "" },
                searchFocus = searchFocus,
                trailing = heroTrailing,
                glassBackdrop = glassBackdrop
            )
        }
    }

    // ── Long-press option pills (collection card / member / kebab).
    when (val target = pillTarget) {
        is PillTarget.Collection -> {
            val c = collections.firstOrNull { it.id == target.id }
            if (c != null) {
                CurioHoldPill(
                    title = c.name,
                    actions = listOf(
                        "Rename" to { renameTarget = c.id; pillTarget = null },
                        "Delete collection" to { deleteTarget = c.id; pillTarget = null }
                    ),
                    destructiveIndexes = setOf(1),
                    onDismiss = { pillTarget = null }
                )
            }
        }
        is PillTarget.Member -> {
            val c = collections.firstOrNull { it.id == target.collectionId }
            if (c != null && target.index in c.members.indices) {
                CurioHoldPill(
                    title = c.members[target.index].refName,
                    actions = listOf(
                        "Move up" to {
                            moveMember(context, c, target.index, -1)
                            pillTarget = null
                        },
                        "Move down" to {
                            moveMember(context, c, target.index, 1)
                            pillTarget = null
                        },
                        "Remove from collection" to {
                            removeMember(context, c, target.index)
                            pillTarget = null
                        }
                    ),
                    destructiveIndexes = setOf(2),
                    onDismiss = { pillTarget = null }
                )
            }
        }
        null -> Unit
    }

    // ── Collection create / rename sheet (with "From a moodboard…").
    if (showCreateSheet || renameTarget != null) {
        val target = renameTarget?.let { id -> collections.firstOrNull { it.id == id } }
        V2CollectionNameSheet(
            initialName = target?.name ?: "",
            moodboards = if (target == null)
                entries.filter { it.format == CaptureFormat.GalleryWall } else emptyList(),
            showMoodboards = target == null,
            title = if (target != null) "Rename collection" else "New collection",
            confirmLabel = if (target != null) "Rename" else "Create",
            onConfirm = { name ->
                if (target != null) {
                    AppPreferences.addOrReplaceCollection(context, target.copy(name = name))
                } else {
                    val id = UUID.randomUUID().toString()
                    AppPreferences.addOrReplaceCollection(
                        context,
                        CurioCollection(id = id, name = name, createdAtMillis = System.currentTimeMillis(), members = emptyList())
                    )
                    openLevel = id
                }
                renameTarget = null
                showCreateSheet = false
            },
            onMoodboard = { entry ->
                val id = UUID.randomUUID().toString()
                AppPreferences.addOrReplaceCollection(
                    context,
                    CurioCollection(
                        id = id,
                        name = entry.title?.ifBlank { null } ?: entry.topic.name,
                        createdAtMillis = System.currentTimeMillis(),
                        members = listOf(CurioCollectionMember(
                            kind = CurioCollectionMember.MemberKind.ENTRY,
                            categoryName = null,
                            refName = entry.id
                        ))
                    )
                )
                showCreateSheet = false
                openLevel = id
            },
            onDismiss = {
                showCreateSheet = false
                renameTarget = null
            }
        )
    }

    // ── Add-entries sheet (multi-select saved entries into a collection).
    if (addTarget != null) {
        val c = collections.firstOrNull { it.id == addTarget }
        if (c != null) {
            V2AddEntriesSheet(
                collection = c,
                entries = entries,
                onAdd = { ids ->
                    val existing = c.members.filter { it.kind == CurioCollectionMember.MemberKind.ENTRY }
                        .map { it.refName }.toSet()
                    val fresh = ids.filterNot { it in existing }
                    if (fresh.isNotEmpty()) {
                        AppPreferences.addOrReplaceCollection(
                            context,
                            c.copy(members = c.members + fresh.map { id ->
                                CurioCollectionMember(
                                    kind = CurioCollectionMember.MemberKind.ENTRY,
                                    categoryName = null,
                                    refName = id
                                )
                            })
                        )
                    }
                    addTarget = null
                },
                onDismiss = { addTarget = null }
            )
        } else {
            LaunchedEffect(addTarget) { addTarget = null }
        }
    }

    // ── Delete-collection confirm.
    if (deleteTarget != null) {
        val c = collections.firstOrNull { it.id == deleteTarget }
        if (c != null) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete \"${c.name}\"?") },
                text = { Text("The collection and its ${c.members.size} member${if (c.members.size == 1) "" else "s"} are removed. Your saved entries and topics stay untouched.") },
                confirmButton = {
                    TextButton(onClick = {
                        AppPreferences.deleteCollection(context, c.id)
                        if (openLevel == c.id) openLevel = ""
                        deleteTarget = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                }
            )
        } else {
            LaunchedEffect(deleteTarget) { deleteTarget = null }
        }
    }

    // ── Liked-item cover source switch (the tile ⋮): pick the OTHER art
    // provider for this book/album/series — the winner is persisted to the
    // shared sheet-art store and the cache is redownloaded, so the Cabinet
    // (and the reveal) show it instantly and forever after.
    if (coverSourceItem != null) {
        V2CoverSourceSheet(
            item = coverSourceItem!!,
            onDismiss = { coverSourceItem = null }
        )
    }

    // ── Everything batch delete (two-step confirm like the classic view).
    if (showBulkDeleteConfirm) {
        CurioTwoStepDeleteDialog(
            visible = showBulkDeleteConfirm,
            title = if (selectedEntryIds.size == 1) "this capture"
            else "${selectedEntryIds.size} selected captures",
            body = "These ${selectedEntryIds.size} captures move to the Recycle bin.",
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirmed = {
                showBulkDeleteConfirm = false
                val ids = selectedEntryIds.toList()
                scope.launch {
                    runCatching { CurioRepositoryHolder.repo.softDeleteByIds(ids) }
                    selectedEntryIds = emptySet()
                    selectionMode = false
                }
            }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────
// Grid emitters (LazyGridScope extensions — one per level)
// ────────────────────────────────────────────────────────────────────────

/** Collections HOME — the JSX folders layout: Everything card → Saved
 *  entries → the shelf grid (built-ins + user collections) → New tile. */
private fun LazyGridScope.v2HomeItems(
    everythingLikes: List<V2Liked>,
    everythingCount: Int,
    savedEntries: List<CurioEntry>,
    visibleShelves: List<Pair<V2Shelf, Int>>,
    userCollections: List<CurioCollection>,
    searching: Boolean,
    selectionMode: Boolean,
    selectedEntryIds: Set<String>,
    onOpenEverything: () -> Unit,
    onOpenShelf: (V2ShelfId) -> Unit,
    onOpenCollection: (String) -> Unit,
    onCollectionLongPress: (String) -> Unit,
    onNewCollection: () -> Unit,
    suggestions: List<CurioTopic>,
    onShuffle: () -> Unit,
    onOpenSuggestion: (CurioTopic) -> Unit,
    showSuggestions: Boolean,
    onEntryLongClick: (String) -> Unit,
    onEntryClick: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    if (searching && visibleShelves.isEmpty() && userCollections.isEmpty()) {
        item(key = "no-match", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
            CurioEmptyState(
                glyph = CurioIcons.SearchOff,
                headline = "No collections match",
                subtext = "Try a different name, or open Everything to search your captures.",
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                ctaLabel = "Clear search",
                onCtaClick = onClearSearch
            )
        }
        return
    }

    if (!searching) {
        // A genuinely empty Cabinet: three suggested discoveries first, then
        // the design still shows the shelves + Everything card below.
        if (showSuggestions) {
            item(key = "suggestions", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                V2EmptySuggestions(
                    suggestions = suggestions,
                    onShuffle = onShuffle,
                    onOpen = onOpenSuggestion
                )
            }
        }
        item(key = "everything-card", span = { GridItemSpan(maxLineSpan) }, contentType = "collection") {
            V2EverythingCard(
                likes = everythingLikes,
                count = everythingCount,
                onOpen = onOpenEverything
            )
        }
        if (savedEntries.isNotEmpty()) {
            item(key = "h-saved", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                V2PageSectionHeader(
                    title = "Saved entries",
                    subtitle = "Your latest additions",
                    trailing = "${savedEntries.size}"
                )
            }
            v2EntryItems(
                entries = savedEntries,
                fullSpan = false,
                selectionMode = selectionMode,
                selectedEntryIds = selectedEntryIds,
                onEntryLongClick = onEntryLongClick,
                onEntryClick = onEntryClick
            )
        }
    }

    if (visibleShelves.isNotEmpty() || userCollections.isNotEmpty()) {
        item(key = "h-collections", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
            V2PageSectionHeader(
                title = "Collections",
                subtitle = "Your personal shelves",
                trailing = if (searching) null else "${visibleShelves.size + userCollections.size}"
            )
        }
        visibleShelves.forEach { (shelf, count) ->
            val seededId = shelf.seededCollectionId
            item(key = "s|${shelf.id}", contentType = "collection") {
                V2ShelfCard(
                    title = shelf.title,
                    icon = shelf.icon,
                    tone = shelf.tone,
                    art = shelf.art,
                    count = count,
                    onClick = { onOpenShelf(shelf.id) },
                    onLongPress = if (seededId != null) ({ onCollectionLongPress(seededId) }) else null,
                    onMoreClick = if (seededId != null) ({ onCollectionLongPress(seededId) }) else null
                )
            }
        }
        userCollections.forEachIndexed { i, c ->
            item(key = "c|${c.id}", contentType = "collection") {
                V2ShelfCard(
                    title = c.name,
                    icon = CurioIcons.AutoAwesome,
                    tone = userShelfTones[i % userShelfTones.size],
                    art = userShelfArts[i % userShelfArts.size],
                    count = c.members.size,
                    onClick = { onOpenCollection(c.id) },
                    onLongPress = { onCollectionLongPress(c.id) },
                    onMoreClick = { onCollectionLongPress(c.id) }
                )
            }
        }
    }
    if (!searching) {
        item(key = "new-collection", span = { GridItemSpan(maxLineSpan) }, contentType = "action") {
            V2NewCollectionTile(onClick = onNewCollection)
        }
    }
}

/** COLLECTION DETAIL — header + members (topic rows / entry cards). */
private fun LazyGridScope.v2DetailItems(
    collection: CurioCollection,
    entriesById: Map<String, CurioEntry>,
    searching: Boolean,
    searchQuery: String,
    onOpenMember: (CurioCollectionMember) -> Unit,
    onOpenEntry: (String) -> Unit,
    onMemberLongPress: (Int) -> Unit,
    onAdd: () -> Unit,
    onKebab: () -> Unit
) {
    item(key = "d-head", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        V2DetailHeader(
            name = collection.name,
            count = collection.members.size,
            onAdd = onAdd,
            onKebab = onKebab
        )
    }
    if (collection.members.isEmpty()) {
        item(key = "d-empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                CurioEmptyState(
                    glyph = CurioIcons.Inventory2,
                    headline = "This collection is empty",
                    subtext = "Pin discoveries from their pages (hold the top bar → File to…) or add saved captures here.",
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                    ctaLabel = "Add captures",
                    onCtaClick = onAdd
                )
            }
        }
        return
    }
    var emitted = 0
    collection.members.forEachIndexed { index, m ->
        val shown = if (searching) {
            when (m.kind) {
                CurioCollectionMember.MemberKind.TOPIC -> searchQuery.isBlank() || m.refName.contains(searchQuery.trim(), ignoreCase = true)
                CurioCollectionMember.MemberKind.ENTRY -> {
                    val e = entriesById[m.refName]
                    searchQuery.isBlank() || e != null && e.topic.name.contains(searchQuery.trim(), ignoreCase = true)
                }
            }
        } else true
        if (!shown) return@forEachIndexed
        emitted++
        when (m.kind) {
            CurioCollectionMember.MemberKind.TOPIC -> item(
                key = "d-t|${collection.id}|$index",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "member"
            ) {
                val liked = memberLiked(m, entriesById)
                V2LikedRow(
                    item = liked ?: V2Liked(m.refName, topicKindForMember(m), null),
                    onClick = { onOpenMember(m) },
                    onLongClick = { onMemberLongPress(index) }
                )
            }
            CurioCollectionMember.MemberKind.ENTRY -> {
                val entry = entriesById[m.refName]
                if (entry != null) {
                    item(key = "d-e|${collection.id}|${m.refName}", contentType = "member") {
                        CurioEntryCard(
                            entry = entry,
                            modifier = Modifier,
                            onLongClick = { onMemberLongPress(index) },
                            onClick = { onOpenEntry(entry.id) }
                        )
                    }
                }
            }
        }
    }
    if (emitted == 0) {
        item(key = "d-no-match", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
            CurioEmptyState(
                glyph = CurioIcons.SearchOff,
                headline = "No members match",
                subtext = "Try a different search.",
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
            )
        }
    }
}

/** EVERYTHING — the JSX library page. v3xx — every kind renders in its OWN
 *  view (no more one-look-alike grid): the toolbar (Filter / Sort) + a type
 *  rail that only lists types with real content, a Recent rail that mixes
 *  recently saved captures WITH recently liked books/albums/series, then a
 *  3-column grid grouped by kind — Books wear portrait jackets, Albums
 *  square covers, Series posters, saved notes keep the entry card, and
 *  REVIEWS (ReelNotes) get their own OUTLINED review card. The old list /
 *  grid view toggle is gone (the grid replaced the list). */
private fun LazyGridScope.v2EverythingItems(
    shownEntries: List<CurioEntry>,
    shownBooks: List<V2Liked>,
    shownAlbums: List<V2Liked>,
    shownSeries: List<V2Liked>,
    recentFeed: List<V2RecentCell>,
    railAvailable: Set<String>,
    typeFilter: String?,
    onTypeFilter: (String?) -> Unit,
    sortAtoZ: Boolean,
    onSort: (Boolean) -> Unit,
    selectionMode: Boolean,
    selectedEntryIds: Set<String>,
    onEntryLongClick: (String) -> Unit,
    onEntryClick: (String) -> Unit,
    onOpenLiked: (V2Liked) -> Unit,
    onCoverSource: (V2Liked) -> Unit,
    onAddNew: () -> Unit,
    onClearFilters: () -> Unit,
    pageAccent: Color
) {
    val reviewEntries = shownEntries.filter { it.format == CaptureFormat.ReelNotes }
    val moodEntries = shownEntries.filter { it.format == CaptureFormat.GalleryWall }
    val noteEntriesShown = shownEntries.filterNot {
        it.format == CaptureFormat.ReelNotes || it.format == CaptureFormat.GalleryWall
    }
    val totalShown =
        shownEntries.size + shownBooks.size + shownAlbums.size + shownSeries.size
    val anyContent = totalShown > 0 || recentFeed.isNotEmpty()
    if (!anyContent) {
        item(key = "e-empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
            CurioEmptyState(
                glyph = CurioIcons.Inventory2,
                headline = "Nothing saved yet",
                subtext = "Save a capture or like a book, series or album and it shows up here.",
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
            )
        }
        item(key = "add-new", span = { GridItemSpan(maxLineSpan) }, contentType = "action") {
            V2AddSomethingButton(onClick = onAddNew)
        }
        return
    }
    if (totalShown == 0 && recentFeed.isNotEmpty()) {
        // Filtered to nothing but the Cabinet still holds items.
        item(key = "e-filtered", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
            CurioEmptyState(
                glyph = CurioIcons.SearchOff,
                headline = "Nothing matches these filters",
                subtext = "Try clearing the search or the type filter.",
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                ctaLabel = "Clear filters",
                onCtaClick = onClearFilters
            )
        }
        return
    }

    item(key = "toolbar", span = { GridItemSpan(maxLineSpan) }, contentType = "toolbar") {
        V2EverythingToolbar(
            typeFilter = typeFilter,
            onTypeFilter = onTypeFilter,
            sortAtoZ = sortAtoZ,
            onSort = onSort
        )
    }
    item(key = "filter-rail", span = { GridItemSpan(maxLineSpan) }, contentType = "chips") {
        V2FilterRail(
            current = typeFilter,
            onSelect = onTypeFilter,
            accent = pageAccent,
            available = railAvailable
        )
    }

    // ── Recent — recently saved captures + recently LIKED books / albums /
    // series (the new likedAt timestamps), newest first, in one rail.
    if (recentFeed.isNotEmpty() && typeFilter == null && !selectionMode) {
        item(key = "h-recent", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
            V2PageSectionHeader(title = "Recent", subtitle = "Latest saves & likes")
        }
        item(key = "recent-rail", span = { GridItemSpan(maxLineSpan) }, contentType = "rail") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                recentFeed.forEach { cell ->
                    when (cell) {
                        is V2RecentCell.Entry -> item(key = cell.key) {
                            CurioEntryCard(
                                entry = cell.entry,
                                onClick = { onEntryClick(cell.entry.id) },
                                modifier = Modifier.width(150.dp)
                            )
                        }
                        is V2RecentCell.Liked -> item(key = cell.key) {
                            V2RecentMediaCell(
                                item = cell.item,
                                onClick = { onOpenLiked(cell.item) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── v3xx — grouped, per-kind sections (each kind keeps its own tile
    // language + cover shape). Hidden rails/types never show a section; a
    // single active filter shows only its kind with no headers.
    fun sectionHeader(title: String, subtitle: String, n: Int) {
        if (typeFilter != null) return
        item(
            key = "s|$title",
            span = { GridItemSpan(maxLineSpan) },
            contentType = "header"
        ) {
            V2PageSectionHeader(title = title, subtitle = subtitle, trailing = "$n")
        }
    }

    fun mediaGrid(likes: List<V2Liked>, title: String, subtitle: String) {
        if (likes.isEmpty()) return
        sectionHeader(title, subtitle, likes.size)
        items(likes, key = { "l|${it.kind.name}|${it.name}" }) { item ->
            V2MediaTileCard(
                item = item,
                onClick = { onOpenLiked(item) },
                onMore = { onCoverSource(item) }
            )
        }
    }

    fun entryGrid(list: List<CurioEntry>, title: String, subtitle: String, review: Boolean) {
        if (list.isEmpty()) return
        sectionHeader(title, subtitle, list.size)
        items(list, key = { "x|$title|${it.id}" }) { e ->
            if (review) {
                V2ReviewTileCard(
                    entry = e,
                    onClick = { onEntryClick(e.id) },
                    onLongClick = { onEntryLongClick(e.id) },
                    selected = e.id in selectedEntryIds
                )
            } else {
                CurioEntryCard(
                    entry = e,
                    selected = e.id in selectedEntryIds,
                    onLongClick = { onEntryLongClick(e.id) },
                    onClick = { onEntryClick(e.id) },
                    modifier = Modifier
                )
            }
        }
    }

    if (typeFilter == null || typeFilter == "books") {
        mediaGrid(shownBooks, "Books", "Your library")
    }
    if (typeFilter == null || typeFilter == "albums") {
        mediaGrid(shownAlbums, "Albums", "On your turntable")
    }
    if (typeFilter == null || typeFilter == "series") {
        mediaGrid(shownSeries, "Series", "On your watchlist")
    }
    if (typeFilter == null || typeFilter == "notes") {
        entryGrid(noteEntriesShown, "Notes", "Captures & journal", review = false)
    }
    if (typeFilter == null || typeFilter == "moodboard") {
        entryGrid(moodEntries, "Moodboards", "Visual collections", review = false)
    }
    if (typeFilter == null || typeFilter == "review") {
        entryGrid(reviewEntries, "Reviews", "Your takes on the things you keep", review = true)
    }

    item(key = "add-new", span = { GridItemSpan(maxLineSpan) }, contentType = "action") {
        V2AddSomethingButton(onClick = onAddNew)
    }
}

// ────────────────────────────────────────────────────────────────────────
// Hero pills
// ────────────────────────────────────────────────────────────────────────

/** The hero's action pills — level-aware (back / search / recycle bin, or
 *  the selection controls in Everything). */
@Composable
private fun V2HeroTrailing(
    openLevel: String,
    selectionMode: Boolean,
    allVisibleSelected: Boolean,
    selectedCount: Int,
    ink: Color,
    fill: Color,
    glassBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop?,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onRecycleBin: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onCancelSelection: () -> Unit
) {
    val glassMod = if (isLiquidGlassPillsActive() && glassBackdrop != null)
        Modifier.liquidGlassCapsule(
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            backdrop = glassBackdrop
        )
    else Modifier
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            CabinetHeroActionPill(
                onClick = onSelectAll,
                label = if (allVisibleSelected) "Clear" else "Select all",
                emphasized = true,
                ink = ink, backdrop = fill,
                modifier = glassMod
            )
            CabinetHeroActionPill(
                onClick = onDelete,
                label = "Delete ($selectedCount)",
                destructive = true,
                ink = ink, backdrop = fill,
                modifier = glassMod
            )
            CabinetHeroActionPill(
                onClick = onCancelSelection,
                glyph = CurioIcons.Close,
                contentDescription = "Cancel selection",
                ink = ink, backdrop = fill,
                modifier = glassMod
            )
        } else {
            if (openLevel.isNotEmpty()) {
                CabinetHeroActionPill(
                    onClick = onBack,
                    glyph = CurioIcons.ArrowBack,
                    contentDescription = "Back to collections",
                    ink = ink, backdrop = fill,
                    modifier = glassMod
                )
            }
            CabinetHeroActionPill(
                onClick = onToggleSearch,
                glyph = CurioIcons.Search,
                contentDescription = "Search",
                ink = ink, backdrop = fill,
                modifier = glassMod
            )
            CabinetHeroActionPill(
                onClick = onRecycleBin,
                glyph = CurioIcons.Delete,
                contentDescription = "Recycle bin",
                ink = ink, backdrop = fill,
                modifier = glassMod
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Collection cards
// ────────────────────────────────────────────────────────────────────────

/** The "+ New collection" tile at the foot of the collections home. */
@Composable
private fun V2NewCollectionTile(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)
        ) {
            CurioIcon(
                name = CurioIcons.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                size = 19.dp
            )
            Text(
                text = "New collection",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// JSX-style home + library components (the Cabinet folders look)
// ────────────────────────────────────────────────────────────────────────

/** The "type" filter rail options — All + the app's real item kinds
 *  (liked books/albums/series + capture formats) mapped onto the JSX's
 *  visual rail. */
private val TYPE_FILTERS = listOf(
    Triple<String?, String, String>(null, "All", CurioIcons.Inventory2),
    Triple<String?, String, String>("books", "Books", CurioIcons.MenuBook),
    Triple<String?, String, String>("albums", "Albums", CurioIcons.Album),
    Triple<String?, String, String>("series", "Series", CurioIcons.Movies),
    Triple<String?, String, String>("notes", "Notes", CurioIcons.Note),
    Triple<String?, String, String>("moodboard", "Moodboards", CurioIcons.Apps),
    Triple<String?, String, String>("review", "Reviews", CurioIcons.FormatQuote)
)

/** Capture formats that read as a personal "note" (voice + journal + field
 *  + wildcard) — the Notes shelf + the Notes rail chip. */
private val noteFormats = setOf(
    CaptureFormat.SoundBite,
    CaptureFormat.Marginalia,
    CaptureFormat.FieldNotes,
    CaptureFormat.OpenNotebook
)

/** Tone + art palette for USER collections (built-ins carry their own). */
private val userShelfTones = listOf(
    V2ShelfTone(light = 0xFFE6D7B6, dark = 0xFF4E4534),  // warm sand
    V2ShelfTone(light = 0xFFD6CAE9, dark = 0xFF4A3E63),  // soft lavender
    V2ShelfTone(light = 0xFFC9DDD2, dark = 0xFF3A4F43),  // sage
    V2ShelfTone(light = 0xFFEAC9CF, dark = 0xFF613F4B),  // blush pink
    V2ShelfTone(light = 0xFFC9DDE9, dark = 0xFF3A5160)   // powder blue
)
private val userShelfArts = listOf(
    V2ShelfArtType.STAR,
    V2ShelfArtType.NOTES,
    V2ShelfArtType.MOUNTAIN,
    V2ShelfArtType.PHOTOS,
    V2ShelfArtType.BOOKS
)

/** Does an entry belong to the given rail type? */
private fun entryInType(e: CurioEntry, t: String?): Boolean = when (t) {
    null -> true
    "notes" -> e.format in noteFormats
    "moodboard" -> e.format == CaptureFormat.GalleryWall
    "review" -> e.format == CaptureFormat.ReelNotes
    else -> false
}

/** Does a liked item (by kind) belong to the given rail type? */
private fun likedShownForType(t: String?, kind: V2Kind): Boolean = when (t) {
    null -> true
    "books" -> kind == V2Kind.BOOK
    "albums" -> kind == V2Kind.ALBUM
    "series" -> kind == V2Kind.SERIES
    else -> false
}

/** A section heading — display title + quiet subtitle + trailing count. */
@Composable
private fun V2PageSectionHeader(
    title: String,
    subtitle: String?,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** The JSX "Everything" card — frosted icon tile + title + circular arrow,
 *  a media rail of REAL saved jacket art (books/albums/series) + a "+" slot,
 *  and the running item count. Tap opens the Everything library. */
@Composable
private fun V2EverythingCard(
    likes: List<V2Liked>,
    count: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    // v3xx — THEME-AWARE (the old hardcoded cream wash looked wrong in dark
    // and in the pastel themes): the card wears the theme's surface tokens;
    // the header icon tile + covers add their own category tints.
    val dark = isCurioDarkTheme()
    val fill = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tileFill = MaterialTheme.colorScheme.surfaceContainerHighest
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(26.dp),
        color = fill,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 23.dp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Everything",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 24.sp
                        ),
                        color = ink,
                        maxLines = 1
                    )
                    Text(
                        text = "All your books, notes, media and more",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.ChevronRight,
                        contentDescription = "Open everything",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 19.dp
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            // v3xx — a horizontally SCROLLABLE cover rail (no fixed 5 slots,
            // no "+" tile): every saved book/album/series cover is reachable
            // by swiping, thumbnails stay real and instant from the cache.
            if (likes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tileFill.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Covers of your liked books, albums and series appear here",
                        style = MaterialTheme.typography.labelMedium,
                        color = muted
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(likes, key = { "${it.kind.name}|${it.name}" }) { item ->
                        val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
                        val accent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 88.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.lerp(
                                                accent, Color.White,
                                                if (dark) 0.14f else 0.46f
                                            ),
                                            androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.40f)
                                        )
                                    )
                                )
                        ) {
                            V2JacketArt(
                                item = item,
                                accent = accent,
                                modifier = Modifier.fillMaxSize().padding(3.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$count item${if (count == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = ink
                )
            }
        }
    }
}

/** The Everything library toolbar — Filter + Sort pills (with menus). The
 *  old grid/list view toggle is GONE (v3xx): Everything is always the
 *  3-column media grid — the toggle only offered a second way to show the
 *  same list-shaped tiles that the grid replaced. */
@Composable
private fun V2EverythingToolbar(
    typeFilter: String?,
    onTypeFilter: (String?) -> Unit,
    sortAtoZ: Boolean,
    onSort: (Boolean) -> Unit
) {
    var filterOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            V2ToolbarPill(
                glyph = CurioIcons.Tune,
                label = "Filter",
                emphasized = typeFilter != null,
                onClick = { filterOpen = true }
            )
            DropdownMenu(
                expanded = filterOpen,
                onDismissRequest = { filterOpen = false }
            ) {
                TYPE_FILTERS.forEach { (key, label, _) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (typeFilter == key) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        },
                        onClick = { onTypeFilter(key); filterOpen = false }
                    )
                }
            }
        }
        Box {
            V2ToolbarPill(
                glyph = if (sortAtoZ) CurioIcons.ArrowUpward else CurioIcons.ArrowDownward,
                label = "Sort",
                onClick = { sortOpen = true }
            )
            DropdownMenu(
                expanded = sortOpen,
                onDismissRequest = { sortOpen = false }
            ) {
                listOf(false to "Recent", true to "A–Z").forEach { (az, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (sortAtoZ == az) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        },
                        onClick = { onSort(az); sortOpen = false }
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = TYPE_FILTERS.firstOrNull { it.first == typeFilter }?.second ?: "All",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** The JSX filter rail — horizontal type chips (All · Books · Albums …).
 *  v3xx — `available` limits the rail to types that actually hold content:
 *  a kind with nothing saved never shows an empty chip ("only show what's
 *  in there, not something that doesn't exist"). All is always present. */
@Composable
private fun V2FilterRail(
    current: String?,
    onSelect: (String?) -> Unit,
    accent: Color,
    available: Set<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TYPE_FILTERS.forEach { (key, label, icon) ->
            if (key != null && key !in available) return@forEach
            val selected = current == key
            Surface(
                onClick = { onSelect(if (selected) null else key) },
                shape = RoundedCornerShape(50),
                color = if (selected) accent
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    CurioIcon(
                        name = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 15.dp
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** The JSX "Add something new" action — dashed hairline + rose circle. */
@Composable
private fun V2AddSomethingButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 24.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add something new",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Save a book, note, image or anything…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CurioIcon(
                name = CurioIcons.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                size = 22.dp
            )
        }
    }
}

/** Compact "when" label for the review tile footer (the shared
 *  formatTimeAgo lives in CurioTopicCard; this file needs its own). */
private fun v2TimeAgo(daysAgo: Int): String = when {
    daysAgo <= 0 -> "today"
    daysAgo == 1 -> "yesterday"
    daysAgo < 7 -> "$daysAgo days ago"
    daysAgo < 30 -> "${daysAgo / 7}w ago"
    else -> "${daysAgo / 30}mo ago"
}

/** v3xx — one cell of the Everything Recent rail: either a saved capture
 *  or a recently LIKED book/album/series (carrying its liked-at stamp). */
private sealed interface V2RecentCell {
    val key: String
    val ts: Long

    data class Entry(val entry: CurioEntry) : V2RecentCell {
        override val key get() = "re|${entry.id}"
        override val ts get() = entry.capturedAtMillis
    }

    data class Liked(val item: V2Liked, val likedAt: Long) : V2RecentCell {
        override val key get() = "rl|${item.kind.name}|${item.name}"
        override val ts get() = likedAt
    }
}

/** A compact liked-media cell for the Recent rail — uniform plate so the
 *  rail reads level, contain-fit jacket art inside (real cached covers). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2RecentMediaCell(item: V2Liked, onClick: () -> Unit) {
    val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
    val accent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 86.dp, height = 122.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.lerp(accent, Color.White, if (isCurioDarkTheme()) 0.14f else 0.5f),
                            androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.40f)
                        )
                    )
                )
                .combinedClickable(onClick = onClick)
        ) {
            V2JacketArt(
                item = item,
                accent = accent,
                modifier = Modifier.fillMaxSize().padding(3.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = when (item.kind) {
                V2Kind.BOOK -> "Book"
                V2Kind.ALBUM -> "Album"
                V2Kind.SERIES -> "Series"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** v3xx — a LIKED media tile in the Everything 3-column grid. Every kind
 *  renders in its OWN shape so nothing shares a look-alike cover box:
 *  BOOKS wear a portrait jacket (spine + sheen), ALBUMS a square cover
 *  with a vinyl disc peeking behind, SERIES a poster plate. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2MediaTileCard(
    item: V2Liked,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** v3xx — the JSX's ⋮ on every item: opens the cover-source sheet so a
     *  liked item can switch providers ("if you didn't like that one"). */
    onMore: (() -> Unit)? = null
) {
    val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
    val accent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
    val dark = isCurioDarkTheme()
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cat?.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
            ?: MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column {
            val artRatio = when (item.kind) {
                V2Kind.BOOK -> 0.667f   // portrait jacket
                V2Kind.ALBUM -> 1f      // square sleeve
                V2Kind.SERIES -> 0.72f  // poster
            }
            val plate = Brush.verticalGradient(
                listOf(
                    androidx.compose.ui.graphics.lerp(accent, Color.White, if (dark) 0.16f else 0.5f),
                    androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.42f)
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(artRatio)
                    .padding(8.dp)
            ) {
                // Albums get a vinyl disc behind the sleeve — a hint of the
                // media behind the cover so albums never read as generic
                // squares.
                if (item.kind == V2Kind.ALBUM) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    )
                }
                V2JacketArt(item = item, accent = accent, modifier = Modifier.fillMaxSize())
                if (onMore != null) {
                    Surface(
                        onClick = onMore,
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.30f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(27.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CurioIcon(
                                name = CurioIcons.MoreVert,
                                contentDescription = "Cover source",
                                tint = Color.White,
                                size = 16.dp
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(start = 11.dp, end = 11.dp, bottom = 11.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val byline = item.topic?.byline
                if (!byline.isNullOrBlank()) {
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.85f))
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = when (item.kind) {
                            V2Kind.BOOK -> "Book"
                            V2Kind.ALBUM -> "Album"
                            V2Kind.SERIES -> "Series"
                        } + (cat?.let { " · ${it.displayName}" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = cat?.categoryInk() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** v3xx — a REVIEW (ReelNotes) tile with its OWN OUTLINED box: the saved
 *  reviews of books/albums/series no longer masquerade as generic capture
 *  cards — a hairline outline, the quote mark, the star rating and a
 *  text preview make a review read as a review at a glance. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2ReviewTileCard(
    entry: CurioEntry,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false
) {
    val cat = CurioCategories.byId(entry.topic.categoryId)
    val accent = cat.themedAccent()
    val data = entry.captureData as? CaptureData.ReelNotes
    Surface(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = if (selected) 1f else 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.FormatQuote,
                        contentDescription = null,
                        tint = accent,
                        size = 16.dp
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text = entry.topic.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    CurioIcon(
                        name = CurioIcons.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        size = 18.dp
                    )
                }
            }
            val rating = data?.rating ?: 0
            if (rating > 0) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    repeat(5) { i ->
                        Text(
                            text = if (i < rating) "\u2605" else "\u2606",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (i < rating) Color(0xFFE8A33D)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
            val reviewText = data?.reviewText?.ifBlank { null }
            if (reviewText != null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = reviewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Review · ${v2TimeAgo(entry.capturedAtDaysAgo)}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** A liked item as a grid tile — jacket art plate + name + kind label. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2LikedTileCard(
    item: V2Liked,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
    val accent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cat?.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
            ?: MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(8.dp)
            ) {
                V2JacketArt(item = item, accent = accent, modifier = Modifier.fillMaxSize())
            }
            Column(modifier = Modifier.padding(start = 11.dp, end = 11.dp, bottom = 11.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when (item.kind) {
                        V2Kind.BOOK -> "Book"
                        V2Kind.ALBUM -> "Album"
                        V2Kind.SERIES -> "Series"
                    } + (cat?.let { " · ${it.displayName}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Emits the saved-capture cards in grid (2-col) or list (full-width) mode. */
private fun LazyGridScope.v2EntryItems(
    entries: List<CurioEntry>,
    fullSpan: Boolean,
    selectionMode: Boolean,
    selectedEntryIds: Set<String>,
    onEntryLongClick: (String) -> Unit,
    onEntryClick: (String) -> Unit
) {
    if (fullSpan) {
        entries.forEach { e ->
            item(key = "e|${e.id}", span = { GridItemSpan(maxLineSpan) }, contentType = "entry") {
                CurioEntryCard(
                    entry = e,
                    modifier = Modifier,
                    selected = e.id in selectedEntryIds,
                    onLongClick = { onEntryLongClick(e.id) },
                    onClick = { onEntryClick(e.id) }
                )
            }
        }
    } else {
        items(entries, key = { "e|${it.id}" }) { e ->
            CurioEntryCard(
                entry = e,
                modifier = Modifier,
                selected = e.id in selectedEntryIds,
                onLongClick = { onEntryLongClick(e.id) },
                onClick = { onEntryClick(e.id) }
            )
        }
    }
}

/** Emits liked items — jacket tiles in grid mode, full-width rows in list. */
private fun LazyGridScope.v2LikedItems(
    likes: List<V2Liked>,
    fullSpan: Boolean,
    onOpenLiked: (V2Liked) -> Unit
) {
    if (fullSpan) {
        likes.forEach { item ->
            item(key = "l|${item.kind}|${item.name}", span = { GridItemSpan(maxLineSpan) }, contentType = "liked") {
                V2LikedRow(item = item, onClick = { onOpenLiked(item) })
            }
        }
    } else {
        items(likes, key = { "l|${it.kind}|${it.name}" }) { item ->
            V2LikedTileCard(item = item, onClick = { onOpenLiked(item) })
        }
    }
}

/** A VIRTUAL shelf detail (Favorites / Saved entries / Notes) — header +
 *  liked rows + saved cards, search-filtered like the other levels. */
private fun LazyGridScope.v2VirtualShelfItems(
    title: String,
    likes: List<V2Liked>,
    entries: List<CurioEntry>,
    searchQuery: String,
    selectedEntryIds: Set<String>,
    selectionMode: Boolean,
    onOpenLiked: (V2Liked) -> Unit,
    onEntryLongClick: (String) -> Unit,
    onEntryClick: (String) -> Unit
) {
    val q = searchQuery.trim()
    fun shown(text: String): Boolean = q.isEmpty() || text.contains(q, ignoreCase = true)
    val shownLikes = likes.filter { shown(it.name) || it.topic?.byline?.let { b -> shown(b) } == true }
    val shownEntries = entries.filter {
        shown(it.topic.name) || it.title?.let { t -> shown(t) } == true || it.tags.any { tag -> shown(tag) }
    }
    val total = shownLikes.size + shownEntries.size

    item(key = "v-head", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        V2PageSectionHeader(
            title = title,
            subtitle = null,
            trailing = "$total item${if (total == 1) "" else "s"}"
        )
    }
    if (total == 0) {
        item(key = "v-empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                CurioEmptyState(
                    glyph = CurioIcons.Inventory2,
                    headline = when {
                        likes.isEmpty() && entries.isEmpty() -> "Nothing here yet"
                        else -> "Nothing matches"
                    },
                    subtext = when {
                        likes.isEmpty() && entries.isEmpty() && title == "Favorites" ->
                            "Like a book, series or album from its page and it lands here."
                        likes.isEmpty() && entries.isEmpty() && title == "Notes" ->
                            "Save a voice note, journal or field note and it lands here."
                        else -> "Try a different search."
                    },
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                )
            }
        }
        return
    }
    shownLikes.forEach { item ->
        item(
            key = "v-l|${item.kind}|${item.name}",
            span = { GridItemSpan(maxLineSpan) },
            contentType = "member"
        ) {
            V2LikedRow(item = item, onClick = { onOpenLiked(item) })
        }
    }
    v2EntryItems(
        entries = shownEntries,
        fullSpan = false,
        selectionMode = selectionMode,
        selectedEntryIds = selectedEntryIds,
        onEntryLongClick = onEntryLongClick,
        onEntryClick = onEntryClick
    )
}

/** The collection-detail header strip: name + count + Add pill + kebab. */
@Composable
private fun V2DetailHeader(
    name: String,
    count: Int,
    onAdd: () -> Unit,
    onKebab: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count item${if (count == 1) "" else "s"} · long-press a member for more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        V2ToolbarPill(
            glyph = CurioIcons.Add,
            contentDescription = "Add saved captures",
            emphasized = true,
            onClick = onAdd
        )
        Spacer(Modifier.width(8.dp))
        V2ToolbarPill(
            glyph = CurioIcons.MoreVert,
            contentDescription = "Rename or delete collection",
            onClick = onKebab
        )
    }
}

// ────────────────────────────────────────────────────────────────────────
// Sheets
// ────────────────────────────────────────────────────────────────────────

/** New-collection / rename sheet — name field + Create/Rename, plus the
 *  "From a moodboard…" list when creating. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V2CollectionNameSheet(
    initialName: String,
    moodboards: List<CurioEntry>,
    showMoodboards: Boolean,
    title: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onMoodboard: (CurioEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                onClick = { onConfirm(name.trim().ifBlank { "Collection" }) },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            if (showMoodboards && moodboards.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "From a moodboard…",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                moodboards.forEach { mb ->
                    Surface(
                        onClick = { onMoodboard(mb) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    size = 16.dp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mb.title?.ifBlank { null } ?: mb.topic.name,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Moodboard · ${mb.topic.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            CurioIcon(
                                name = CurioIcons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                size = 16.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** v3xx — the liked-item COVER SOURCE sheet: books / albums / series each
 *  have two art providers; if the current cover isn't right, tapping the
 *  other one re-resolves, persists, and re-downloads the bytes ("if you
 *  didn't like that one, show the other"). Images stay in the cover cache. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V2CoverSourceSheet(
    item: V2Liked,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kind = CabinetCoverCache.CoverKind.valueOf(item.kind.name)
    val labels = when (kind) {
        CabinetCoverCache.CoverKind.BOOK -> listOf("iTunes Search", "Open Library")
        CabinetCoverCache.CoverKind.ALBUM -> listOf("iTunes", "MusicBrainz")
        CabinetCoverCache.CoverKind.SERIES -> listOf("TVMaze", "iTunes")
    }
    val title = when (item.kind) {
        V2Kind.BOOK -> "Book cover"
        V2Kind.ALBUM -> "Album artwork"
        V2Kind.SERIES -> "Series poster"
    }
    var busy by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Cover source",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            labels.forEachIndexed { index, label ->
                Surface(
                    onClick = {
                        if (busy) return@Surface
                        busy = true
                        scope.launch {
                            val url = CabinetCoverCache.resolveWithProvider(
                                context,
                                kind,
                                item.name,
                                item.topic?.byline,
                                item.topic?.imageUrl,
                                index
                            )
                            if (!url.isNullOrBlank()) {
                                when (kind) {
                                    CabinetCoverCache.CoverKind.BOOK ->
                                        AppPreferences.setBookCoverUrl(context, item.name, url)
                                    else -> AppPreferences.setSheetArtUrl(
                                        context,
                                        "${kind.stateKey}|${item.name}",
                                        url
                                    )
                                }
                                CabinetCoverCache.ensureLocalCover(
                                    context,
                                    kind,
                                    item.name,
                                    item.topic?.byline,
                                    item.topic?.imageUrl,
                                    redownload = true
                                )
                            }
                            busy = false
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                name = when (index) {
                                    0 -> CurioIcons.PhotoLibrary
                                    1 -> CurioIcons.Shuffle
                                    else -> CurioIcons.AutoAwesome
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 16.dp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = if (index == 0) "First choice — usually matches best"
                                else "Alternate source — try this one if the cover is off",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        if (busy) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Add saved captures into a collection — multi-select sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V2AddEntriesSheet(
    collection: CurioCollection,
    entries: List<CurioEntry>,
    onAdd: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var picked by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    val existing = remember(collection) {
        collection.members.filter { it.kind == CurioCollectionMember.MemberKind.ENTRY }
            .map { it.refName }.toSet()
    }
    val available = remember(entries, existing) { entries.filterNot { it.id in existing } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Add to ${collection.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (available.isEmpty()) {
                Text(
                    text = "All your saved captures are already in this collection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(available, key = { it.id }) { e ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = e.topic.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = e.title?.ifBlank { null } ?: e.format.shortName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Checkbox(
                                checked = e.id in picked,
                                onCheckedChange = { on ->
                                    picked = if (on) picked + e.id else picked - e.id
                                }
                            )
                        }
                    }
                }
                Surface(
                    onClick = { onAdd(picked.toList()) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (picked.isEmpty()) "Add" else "Add ${picked.size}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Member / liked helpers
// ────────────────────────────────────────────────────────────────────────

private sealed interface PillTarget {
    data class Collection(val id: String) : PillTarget
    data class Member(val collectionId: String, val index: Int) : PillTarget
}

private fun moveMember(context: android.content.Context, c: CurioCollection, index: Int, delta: Int) {
    val to = index + delta
    if (to !in c.members.indices) return
    val members = c.members.toMutableList()
    val m = members.removeAt(index)
    members.add(to, m)
    AppPreferences.addOrReplaceCollection(context, c.copy(members = members))
}

private fun removeMember(context: android.content.Context, c: CurioCollection, index: Int) {
    val members = c.members.toMutableList()
    if (index !in members.indices) return
    members.removeAt(index)
    AppPreferences.addOrReplaceCollection(context, c.copy(members = members))
}

/** Resolve a collection member to a [V2Liked] (for collage art + rows). */
private fun memberLiked(
    m: CurioCollectionMember,
    entriesById: Map<String, CurioEntry>
): V2Liked? = when (m.kind) {
    CurioCollectionMember.MemberKind.TOPIC -> {
        val cat = m.categoryName?.let { n -> runCatching { CategoryId.valueOf(n) }.getOrNull() }
        val t = if (cat != null) {
            TopicJsonLoader.cached(cat)?.firstOrNull {
                it.matchesSavedNameStrict(m.refName) || it.matchesSavedName(m.refName)
            } ?: TopicCatalog.findByName(m.refName)
        } else {
            TopicCatalog.findByName(m.refName)
        }
        t?.let { V2Liked(it.name, topicKind(it), it) }
    }
    CurioCollectionMember.MemberKind.ENTRY -> {
        val e = entriesById[m.refName]
        e?.let { V2Liked(it.topic.name, topicKind(it.topic), it.topic) }
    }
}

/** One liked item — books / series / albums all live by NAME (the identity
 *  the reveal heart toggles store). The topic is resolved tolerantly; null
 *  = the name has not resolved yet (cold start before the lane pools warm)
 *  or no longer resolves (legacy / renamed) — the row still renders with a
 *  plain plate and STILL opens via the kind's canonical lane. */
private data class V2Liked(
    val name: String,
    val kind: V2Kind,
    val topic: CurioTopic?
) {
    fun open(navController: NavController) {
        // The reveal resolves by category (Room-backed), so the target
        // ALWAYS opens its real page: use the resolved topic's lane when
        // known, else the canonical lane for the kind (books → books,
        // albums → albums, series → series — the lane the heart lives on).
        val t = topic
        val slug = t?.categoryId?.routeSlug ?: when (kind) {
            V2Kind.BOOK -> CategoryId.BOOKS.routeSlug
            V2Kind.ALBUM -> CategoryId.ALBUMS.routeSlug
            V2Kind.SERIES -> CategoryId.SERIES.routeSlug
        }
        navController.navigate(
            CurioRoutes.revealFor(slug, t?.name ?: name)
        ) { launchSingleTop = true }
    }
}

private enum class V2Kind { BOOK, ALBUM, SERIES }

/** Kind-aware topic resolution for liked rows. Books/series/albums are
 *  hearted on their CANONICAL lane (BOOKS / SERIES / ALBUMS — that's where
 *  the heart lives in the reveal sheets), so that lane is searched FIRST: a
 *  global [TopicCatalog.findByName] walks lanes in enum order and would hand
 *  "Animal Farm" (the book) to "Animal Farm (1954)" (the animated film)
 *  because ANIMATED_MOVIES precedes BOOKS. The global search stays as the
 *  fallback for legacy / renamed names. */
private fun findLikedTopic(kind: V2Kind, name: String): CurioTopic? {
    val lane = when (kind) {
        V2Kind.BOOK -> CategoryId.BOOKS
        V2Kind.ALBUM -> CategoryId.ALBUMS
        V2Kind.SERIES -> CategoryId.SERIES
    }
    TopicJsonLoader.cached(lane)?.firstOrNull {
        it.matchesSavedNameStrict(name) || it.matchesSavedName(name)
    }?.let { return it }
    return TopicCatalog.findByName(name)
}

/** Best-fitting jacket shape for a suggested topic's lane. */
private fun topicKind(t: CurioTopic): V2Kind = when (t.categoryId) {
    CategoryId.BOOKS, CategoryId.AUTHORS -> V2Kind.BOOK
    CategoryId.SERIES, CategoryId.ANIME, CategoryId.MANGA, CategoryId.MANHWA,
    CategoryId.FILMS, CategoryId.DIRECTORS, CategoryId.ANIMATED_MOVIES -> V2Kind.SERIES
    else -> V2Kind.ALBUM
}

/** A fallback kind for an unresolvable TOPIC member (the detail row still
 *  renders its plate + name). */
private fun topicKindForMember(m: CurioCollectionMember): V2Kind = when (m.kind) {
    CurioCollectionMember.MemberKind.TOPIC -> V2Kind.BOOK
    CurioCollectionMember.MemberKind.ENTRY -> V2Kind.BOOK
}

/** ANALYSIS.md 4.4.4 — an empty Cabinet starts with three suggested
 *  discoveries (re-rolled by the Shuffle pill) instead of a blank page. */
@Composable
private fun V2EmptySuggestions(
    suggestions: List<CurioTopic>,
    onShuffle: () -> Unit,
    onOpen: (CurioTopic) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Your Cabinet is empty",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        if (suggestions.isEmpty()) {
            // Pools still loading — keep the frame steady.
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                )
                Spacer(Modifier.height(10.dp))
            }
        } else {
            suggestions.forEach { t ->
                V2LikedRow(
                    item = V2Liked(t.name, topicKind(t), t),
                    onClick = { onOpen(t) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Surface(
            onClick = onShuffle,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(38.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Shuffle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 17.dp
                )
                Text(
                    text = "Shuffle suggestions",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** A compact toolbar pill (add / kebab / selection actions). */
@Composable
private fun V2ToolbarPill(
    label: String? = null,
    glyph: String? = null,
    contentDescription: String? = null,
    emphasized: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val ink = if (destructive) MaterialTheme.colorScheme.error
    else if (emphasized) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val fill = if (destructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
    else if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = fill,
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = if (label != null) 13.dp else 9.dp)
        ) {
            if (glyph != null) {
                CurioIcon(
                    name = glyph,
                    contentDescription = contentDescription,
                    tint = ink,
                    size = 17.dp
                )
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = ink,
                    maxLines = 1
                )
            }
        }
    }
}

/** The full-width liked row: contain-fit jacket art + name / byline +
 *  category + chevron. Tap opens the reveal page (where the heart lives);
 *  optional long-press opens a "more" pill (used in collection details). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2LikedRow(
    item: V2Liked,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
    val accent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        color = cat?.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
            ?: MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            V2JacketArt(
                item = item,
                accent = accent,
                modifier = Modifier
                    .width(54.dp)
                    .aspectRatio(if (item.kind == V2Kind.ALBUM) 1f else 0.667f)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val byline = item.topic?.byline
                if (!byline.isNullOrBlank()) {
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.85f))
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = when (item.kind) {
                            V2Kind.BOOK -> "Book"
                            V2Kind.ALBUM -> "Album"
                            V2Kind.SERIES -> "Series"
                        } + (cat?.let { " · ${it.displayName}" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        // The category INK (not the accent): the row wears a
                        // category-tinted surface, so the raw accent text
                        // blended into the same hue. categoryInk resolves the
                        // readable deep/light twin for the active theme.
                        color = cat?.categoryInk() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CurioIcon(
                name = CurioIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                size = 20.dp
            )
        }
    }
}

/** Contain-fit jacket art for a liked item: book = half-book jacket with a
 *  spine + sheen, album = square, series = poster — the artwork is always
 *  FIT (never stretched) inside its plate, exactly like the reveal pages.
 *  Cover sources mirror the reveal: the stored/hub URL first (books), the
 *  authored imageUrl, then the keyless resolver when the matching fetch
 *  consent toggle is on. */
@Composable
private fun V2JacketArt(item: V2Liked, accent: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val topic = item.topic
    // v3xx — the COVER CACHE's local file first: when the bytes are already
    // on disk (the warmer or a sheet visit saved them) the cover loads
    // INSTANTLY — no URL, no Coil miss, no flash of the gradient plate.
    // Keyed on the cache version so a tile that composed BEFORE its cover
    // downloaded re-checks the moment the warmer saves the file.
    var cached by remember(item.name, item.kind, CabinetCoverCache.version.intValue) {
        mutableStateOf(
            CabinetCoverCache.localCoverFile(
                context,
                CabinetCoverCache.CoverKind.valueOf(item.kind.name),
                item.name
            )
        )
    }
    val stored = if (item.kind == V2Kind.BOOK) {
        AppPreferences.bookCoverUrlsState[item.name]?.takeIf { it.isNotBlank() }
    } else null
    // Albums / series: the artwork the reveal sheets ALREADY resolved and
    // persisted (sheetArtUrlsState keyed "album|<name>" / "series|<name>") —
    // real covers appear instantly after a sheet visit, no re-fetch.
    val sheetArt = if (item.kind != V2Kind.BOOK) {
        AppPreferences.sheetArtUrlsState["${item.kind.name.lowercase()}|${item.name}"]
            ?.takeIf { it.isNotBlank() }
    } else null
    val authored = topic?.imageUrl?.takeIf { it.isNotBlank() }
    val candidates = remember(item.name, topic, stored, sheetArt, authored) {
        val list = mutableListOf<String>()
        if (!stored.isNullOrBlank()) list.add(stored)
        if (!sheetArt.isNullOrBlank() && sheetArt != stored) list.add(sheetArt)
        if (!authored.isNullOrBlank() && authored != stored && authored != sheetArt) list.add(authored)
        if (item.kind == V2Kind.BOOK) {
            list.add(BookCoverFetch.coverUrlFor(item.name, ""))
        }
        list.distinct()
    }
    var coverIndex by remember(item.name, candidates) { mutableIntStateOf(0) }
    var resolved by remember(item.name) { mutableStateOf<String?>(null) }
    var liveDone by remember(item.name) { mutableStateOf(false) }
    // v3xx — the per-tile live resolve is ALWAYS armed (no Settings consent
    // gate): the cover cache is always-on by design, so an item liked mid-
    // session resolves here even before the warmer's next pass. Albums /
    // series cascade BOTH providers (iTunes then MusicBrainz / TVMaze then
    // iTunes) — if provider 0 misses, provider 1 fills the gap instead of
    // leaving a bare gradient plate. The winner is persisted to the same
    // sheetArt store the reveal reads, so every surface agrees instantly.
    LaunchedEffect(item.name, item.kind, coverIndex, liveDone) {
        if (!liveDone && item.kind != V2Kind.BOOK && coverIndex >= candidates.size) {
            var found: String? = null
            when (item.kind) {
                V2Kind.ALBUM -> {
                    for (p in 0 until AlbumArtFetch.PROVIDER_COUNT) {
                        val u = AlbumArtFetch.resolveArtworkUrl(item.name, topic?.byline, p)
                        if (!u.isNullOrBlank()) { found = u; break }
                    }
                }
                V2Kind.SERIES -> {
                    for (p in 0 until SeriesPosterFetch.PROVIDER_COUNT) {
                        val u = SeriesPosterFetch.resolvePosterUrl(item.name, p)
                        if (!u.isNullOrBlank()) { found = u; break }
                    }
                }
                else -> Unit
            }
            resolved = found
            if (!found.isNullOrBlank()) {
                CabinetCoverCache.ensureLocalCover(
                    context,
                    CabinetCoverCache.CoverKind.valueOf(item.kind.name),
                    item.name,
                    topic?.byline,
                    authored
                )
                AppPreferences.setSheetArtUrl(
                    context,
                    "${item.kind.name.lowercase()}|${item.name}",
                    found
                )
            }
            liveDone = true
        }
    }
    val local = cached?.toURI()?.toString()
    val url = resolved ?: candidates.getOrNull(coverIndex)
    val corner = if (item.kind == V2Kind.ALBUM) 10.dp else 8.dp
    val plate = Brush.verticalGradient(
        listOf(
            androidx.compose.ui.graphics.lerp(accent, Color.White, if (isCurioDarkTheme()) 0.16f else 0.5f),
            androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.42f)
        )
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(plate)
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(corner))) {
            CurioIcon(
                name = when (item.kind) {
                    V2Kind.BOOK -> CurioIcons.MenuBook
                    V2Kind.ALBUM -> CurioIcons.Album
                    V2Kind.SERIES -> CurioIcons.Movies
                },
                contentDescription = null,
                tint = Color.White.copy(alpha = if (url == null && local == null) 0.7f else 0f),
                size = 20.dp
            )
            // v3xx — the local cache file wins (instant, offline); network
            // URLs only when the bytes aren't on disk yet.
            if (local != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(cached)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover art for ${item.name}",
                    contentScale = ContentScale.Fit,
                    onError = {
                        // A stale local file — drop it and fall back to the
                        // URL cascade.
                        runCatching { cached?.delete() }
                        cached = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover art for ${item.name}",
                    contentScale = ContentScale.Fit,
                    onError = {
                        if (coverIndex < candidates.size) coverIndex++
                        else { resolved = null; liveDone = true }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (item.kind == V2Kind.BOOK) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(Color.Black.copy(alpha = 0.20f))
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.02f),
                                Color.White.copy(alpha = 0f)
                            )
                        )
                    )
                )
            }
        }
    }
}
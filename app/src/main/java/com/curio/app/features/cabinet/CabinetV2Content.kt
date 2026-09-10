package com.curio.app.features.cabinet

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
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
import com.curio.app.ui.components.formatGlyph
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

    // ── Glass plumbing: the grid records into a LOCAL backdrop the hero
    // pills blur (sibling overlay outside the captured subtree).
    val glassOn = isLiquidGlassPillsActive()
    val glassBackdrop = if (glassOn) rememberLayerBackdrop() else null

    // ── Data: saved entries (repo flow) + liked books/series/albums +
    // user collections.
    // v3xx37 — the LIGHT flow: the grid only needs light columns (topic,
    // format, timestamps, tags) — the full flow re-read + re-allocated every
    // payload JSON blob on every DB emission, which is what made the Cabinet
    // lag with a large saved-entries archive. Detail pages still use the
    // full [observeById]; ReelNotes reviews keep their payload via the
    // light query's ReelNotes-only formatDataJson column.
    val entries by androidx.compose.runtime.produceState<List<CurioEntry>>(initialValue = emptyList()) {
        try {
            CurioRepositoryHolder.repo.observeLight().collect { value = it }
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

    // ── Seed the four empty starter shelves (Curiying now / Want to
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
    // v3xx34 — the add-sheet's LIGHTWEIGHT projection (id + display text +
    // format glyph), computed once. The picker never carries full
    // [CurioEntry] objects — their capture payloads are what made the old
    // add sheet lag on large archives.
    val addOptions = remember(entries) {
        entries.map { e ->
            AddOption(
                id = e.id,
                name = e.topic.name,
                subtitle = e.title?.ifBlank { null } ?: e.format.shortName,
                glyph = formatGlyph(e.format)
            )
        }
    }

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
    // v3xx37 — the warmer runs WHOLE on IO: it pre-warms every liked
    // cover's dominant color (so composition-time [dominantCoverColor]
    // always hits the cache instead of decoding on the main thread), then
    // downloads up to 30 missing covers WITHOUT per-download version bumps
    // and bumps the version ONCE after the batch — the old per-save bump
    // recomposed every version-keyed tile per download.
    LaunchedEffect(allLikes.size, unwarmedLikes.size) {
        var newCovers = 0
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            allLikes.forEach { item ->
                CabinetCoverCache.warmDominantColor(
                    context,
                    CabinetCoverCache.CoverKind.valueOf(item.kind.name),
                    item.name
                )
            }
            for (item in unwarmedLikes) {
                if (newCovers >= 30) break
                if (CabinetCoverCache.localCoverFile(
                        context,
                        CabinetCoverCache.CoverKind.valueOf(item.kind.name),
                        item.name
                    ) != null
                ) continue
                val saved = CabinetCoverCache.ensureLocalCover(
                    context,
                    CabinetCoverCache.CoverKind.valueOf(item.kind.name),
                    item.name,
                    item.topic?.byline,
                    item.topic?.imageUrl,
                    bumpVersion = false
                )
                if (saved != null) {
                    CabinetCoverCache.warmDominantColor(
                        context,
                        CabinetCoverCache.CoverKind.valueOf(item.kind.name),
                        item.name
                    )
                    newCovers++
                }
            }
        }
        if (newCovers > 0) CabinetCoverCache.version.intValue++
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

    // ── Everything-level filters: search + type.
    val filteredEntries = remember(entries, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) entries
        else entries.filter {
            matchesQ(it.topic.name) || it.title?.let { t -> matchesQ(t) } == true ||
                it.tags.any { tag -> matchesQ(tag) }
        }
    }
    val shownEntries = remember(filteredEntries, typeFilter) {
        filteredEntries.filter { entryInType(it, typeFilter) }
            .sortedByDescending { it.capturedAtMillis }
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
    val shownBooks = remember(books, searchQuery, typeFilter, likedAtMap) {
        filteredLiked(books).filter { likedShownForType(typeFilter, V2Kind.BOOK) }
            .sortedByDescending { likedAtFor(it) }
    }
    val shownAlbums = remember(albums, searchQuery, typeFilter, likedAtMap) {
        filteredLiked(albums).filter { likedShownForType(typeFilter, V2Kind.ALBUM) }
            .sortedByDescending { likedAtFor(it) }
    }
    val shownSeries = remember(series, searchQuery, typeFilter, likedAtMap) {
        filteredLiked(series).filter { likedShownForType(typeFilter, V2Kind.SERIES) }
            .sortedByDescending { likedAtFor(it) }
    }

    // ── v3xx — the Everything rail only lists LIKED MEDIA (saved captures
    // are no longer part of the gallery), so only the kinds that actually
    // hold content show a chip.
    val railAvailable = remember(books, albums, series) {
        buildSet {
            if (books.isNotEmpty()) add("books")
            if (albums.isNotEmpty()) add("albums")
            if (series.isNotEmpty()) add("series")
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
    var addTarget by rememberSaveable { mutableStateOf<AddTarget?>(null) }
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
        openCollection != null -> "${openCollection.members.size} item${if (openCollection.members.size == 1) "" else "s"}"
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
        // v3xx — each level owns a FRESH grid state: the single shared
        // remembered scroll made opening a collection land MID-list and made
        // page switches visibly jump (the glitch). key(openLevel) recreates
        // the grid per level, so every level opens from the TOP.
        // The wide-window hero — the grid's first item on tablets/landscape
        // (shared by the regular grid and the Everything masonry below).
        val wideHero: @Composable () -> Unit = {
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

        key(openLevel) {
        // ── EVERYTHING / FAVORITES — the JSX masonry gallery: a dense
        // STAGGERED grid of covers-only posters (books tall jackets, albums
        // squares, series posters — no cards, no titles, no boxes, just the
        // art with a whisper of rounded corners) with filter chips that
        // smoothly reflow the gallery — every cover animates to its new spot
        // when the category changes (the CurioEverythingGallery concept).
        // Favorites stores ONLY liked media, so it wears the same gallery.
        if (openLevel == "everything" || openLevel == SHELF_LEVEL_FAVORITES) {
            LazyVerticalStaggeredGrid(
                state = rememberLazyStaggeredGridState(),
                columns = StaggeredGridCells.Fixed(if (wide) 8 else 4),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentTop,
                    bottom = 24.dp + 84.dp +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (glassOn && glassBackdrop != null)
                        Modifier.layerBackdrop(glassBackdrop) else Modifier)
            ) {
                if (wide) {
                    item(key = "hero", span = StaggeredGridItemSpan.FullLine, contentType = "hero") {
                        wideHero()
                    }
                }
                v2EverythingMasonryItems(
                    shownBooks = shownBooks,
                    shownAlbums = shownAlbums,
                    shownSeries = shownSeries,
                    railAvailable = railAvailable,
                    typeFilter = typeFilter,
                    onTypeFilter = { typeFilter = it },
                    likedAtFor = { likedAtFor(it) },
                    onOpenLiked = { item -> item.open(navController) },
                    onCoverSource = { coverSourceItem = it },
                    // Everything's add-new dives into Spin; Favorites opens
                    // the add-to-favorites sheet (same as its old Add pill).
                    onAddNew = if (openLevel == SHELF_LEVEL_FAVORITES) {
                        { addTarget = AddTarget.Favorites }
                    } else {
                        { navController.navigateToTab(CurioRoutes.SPIN) }
                    },
                    pageAccent = pageAccent
                )
            }
        } else {
        LazyVerticalGrid(
            state = rememberLazyGridState(),
            columns = if (wide) GridCells.Adaptive(minSize = 176.dp) else GridCells.Fixed(2),
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
                    wideHero()
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
                    onAdd = { addTarget = AddTarget.Collection(openCollection) },
                    onRename = { renameTarget = openCollection.id },
                    onDelete = { deleteTarget = openCollection.id }
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
                    },
                    onLikedMore = { coverSourceItem = it }
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
                    // v3xx — the collection cards' ⋮ now drives RENAME / DELETE
                    // straight from an anchored dropdown (no center overlay).
                    onRenameCollection = { id -> renameTarget = id },
                    onDeleteCollection = { id -> deleteTarget = id },
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

    // ── Long-press option pills (collection member only — the collection
    // card ⋮ and the detail ⋮ are anchored dropdowns now).
    when (val target = pillTarget) {
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
            // v3xx — the custom card style travels through the sheet: create
            // starts on Auto (-1 / null), rename keeps the collection's own.
            initialTone = target?.tone ?: -1,
            initialArt = target?.art ?: -1,
            initialIcon = target?.icon,
            moodboards = if (target == null)
                entries.filter { it.format == CaptureFormat.GalleryWall } else emptyList(),
            showMoodboards = target == null,
            title = if (target != null) "Rename collection" else "New collection",
            confirmLabel = if (target != null) "Rename" else "Create",
            onConfirm = { name, tone, art, icon ->
                if (target != null) {
                    AppPreferences.addOrReplaceCollection(context, target.copy(name = name))
                } else {
                    val id = UUID.randomUUID().toString()
                    AppPreferences.addOrReplaceCollection(
                        context,
                        CurioCollection(
                            id = id, name = name, createdAtMillis = System.currentTimeMillis(),
                            members = emptyList(),
                            tone = tone, art = art, icon = icon
                        )
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

    // ── Add-to-shelf sheet (search topics / favorites / saved captures).
    // v3xx34 — one redesigned picker for BOTH targets: a real collection
    // (multi-pick saved captures + one-tap topic adds from favorites/search)
    // and the Favorites virtual shelf (favorites mode: search topics to
    // save them).
    addTarget?.let { target ->
        val c = (target as? AddTarget.Collection)?.collection
        if (target is AddTarget.Collection && (c == null || collections.none { it.id == c.id })) {
            LaunchedEffect(addTarget) { addTarget = null }
        } else {
            V2AddToShelfSheet(
                target = target,
                likes = allLikes,
                addOptions = addOptions,
                onAddEntries = { ids ->
                    if (c == null) return@V2AddToShelfSheet
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
                onToggleTopic = { liked ->
                    if (c != null) {
                        val member = likedToMember(liked)
                        val has = c.members.any {
                            it.kind == member.kind && it.categoryName == member.categoryName &&
                                it.refName == member.refName
                        }
                        val members = if (has) c.members.filterNot {
                            it.kind == member.kind && it.categoryName == member.categoryName &&
                                it.refName == member.refName
                        } else c.members + member
                        AppPreferences.addOrReplaceCollection(context, c.copy(members = members))
                    } else {
                        toggleLikedFavorite(liked, context)
                    }
                },
                onDismiss = { addTarget = null }
            )
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
    onRenameCollection: (String) -> Unit,
    onDeleteCollection: (String) -> Unit,
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
                    // v3xx — the card's OWN anchored ⋮ (Rename for the seeded
                    // starter shelves; the seeded ones stay non-deletable).
                    onRename = if (seededId != null) ({ onRenameCollection(seededId) }) else null
                )
            }
        }
        userCollections.forEachIndexed { i, c ->
            item(key = "c|${c.id}", contentType = "collection") {
                V2ShelfCard(
                    title = c.name,
                    // v3xx — a user collection renders its CUSTOM card style
                    // (tone/art/icon chosen in the New-collection sheet) when
                    // set; otherwise it cycles the palette like before.
                    icon = c.icon ?: CurioIcons.AutoAwesome,
                    tone = c.tone.takeIf { it >= 0 && it < userShelfTones.size }
                        ?.let { userShelfTones[it] } ?: userShelfTones[i % userShelfTones.size],
                    art = c.art.takeIf { it >= 0 && it < userShelfArts.size }
                        ?.let { userShelfArts[it] } ?: userShelfArts[i % userShelfArts.size],
                    count = c.members.size,
                    onClick = { onOpenCollection(c.id) },
                    onRename = { onRenameCollection(c.id) },
                    onDelete = { onDeleteCollection(c.id) }
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
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    item(key = "d-head", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        V2DetailHeader(
            name = collection.name,
            count = collection.members.size,
            onAdd = onAdd,
            onRename = onRename,
            onDelete = onDelete
        )
    }
    if (collection.members.isEmpty()) {
        // v3xx — the JSX "Nothing here yet" scene (stacked books + leaf
        // sprig + note card + twinkles) drawn in the app's flask doodle
        // language — see V2CollectionEmptyState in CabinetShelves.kt.
        item(key = "d-empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
            V2CollectionEmptyState(onAdd = onAdd)
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

/** EVERYTHING — the JSX poster gallery (the CurioEverythingGallery
 *  concept, now COVERS ONLY): no saved captures, no Recent rail, no
 *  per-kind section headers, no card chrome — just the liked media's
 *  cover art (books tall, albums square, series posters) with a whisper
 *  of rounding, sized by kind. The most recently liked item is featured a
 *  little larger and the staggered grid packs everything else around it.
 *  The filter CHIPS ride the top full-line (the Filter/Sort pills are
 *  gone — the JSX has no filter dropdown), and every cover animates to
 *  its new spot when the category changes. */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyStaggeredGridScope.v2EverythingMasonryItems(
    shownBooks: List<V2Liked>,
    shownAlbums: List<V2Liked>,
    shownSeries: List<V2Liked>,
    railAvailable: Set<String>,
    typeFilter: String?,
    onTypeFilter: (String?) -> Unit,
    likedAtFor: (V2Liked) -> Long,
    onOpenLiked: (V2Liked) -> Unit,
    onCoverSource: (V2Liked) -> Unit,
    onAddNew: () -> Unit,
    pageAccent: Color
) {
    // v3xx — JUMBLED recency wall: all liked media merge into ONE
    // recency-ordered stream (no category grouping — books, albums and
    // series interleave exactly as they were liked). Staggered-grid spans
    // only offer FullLine / SingleLane, so every cover takes one lane and
    // the size variation comes from HEIGHT — the grid's native language.
    // Sizes step down by recency tier: the most recent runs 2x (featured,
    // neighbours pack around it), then 1.5x / 1x / 0.75x / 0.5x down the
    // wall — a magazine-like mix. Smooth reflow: every cover animates to
    // its new spot when the filter changes (animateItem is a member
    // extension of the item scope, so it's applied per-item inside each
    // item {} block).
    val allMedia = (shownBooks + shownAlbums + shownSeries)
        .sortedByDescending { likedAtFor(it) }
    val totalShown = allMedia.size

    // Size tier for a recency rank — a multiplier on the cover's base
    // aspect (smaller aspect = taller card): rank 0 (most recent) 2x, then
    // ~1.5x / 1x / 0.75x / 0.5x as the wall ages.
    fun tierMultiplier(rank: Int): Float = when {
        rank == 0 -> 0.5f     // 2x — the most recent, featured
        rank <= 2 -> 0.67f    // ~1.5x
        rank <= 7 -> 1f       // 1x
        rank <= 15 -> 1.33f   // ~0.75x
        else -> 2f            // ~0.5x
    }

    if (totalShown == 0) {
        item(key = "e-empty", span = StaggeredGridItemSpan.FullLine, contentType = "empty") {
            CurioEmptyState(
                glyph = CurioIcons.Inventory2,
                headline = "Nothing saved yet",
                subtext = "Like a book, series or album and its cover shows up here.",
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
            )
        }
        item(key = "add-new", span = StaggeredGridItemSpan.FullLine, contentType = "action") {
            V2AddSomethingButton(onClick = onAddNew)
        }
        return
    }

    item(key = "filter-rail", span = StaggeredGridItemSpan.FullLine, contentType = "chips") {
        V2FilterRail(
            current = typeFilter,
            onSelect = onTypeFilter,
            accent = pageAccent,
            available = railAvailable
        )
    }

    // One jumbled, recency-ranked stream — the tier comes from the rank.
    allMedia.forEachIndexed { rank, liked ->
        item(
            key = "l|${liked.kind.name}|${liked.name}",
            contentType = "media"
        ) {
            val fallbackAccent = liked.topic?.categoryId?.let { CurioCategories.byId(it) }
                ?.themedAccent() ?: MaterialTheme.colorScheme.primary
            val baseAspect = when (liked.kind) {
                V2Kind.BOOK -> 0.667f   // portrait jacket
                V2Kind.ALBUM -> 1f      // square sleeve
                V2Kind.SERIES -> 0.72f  // poster
            }
            // Tiered size: the recency rank drives how tall the card runs
            // (2x most recent → 0.5x oldest), neighbours pack around it.
            val aspect = (baseAspect * tierMultiplier(rank)).coerceIn(0.3f, 2.6f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        Modifier.animateItem(
                            fadeInSpec = tween(220),
                            fadeOutSpec = tween(150),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    )
                    .combinedClickable(
                        onClick = { onOpenLiked(liked) },
                        onLongClick = { onCoverSource(liked) }
                    )
            ) {
                V2JacketArt(item = liked, accent = fallbackAccent, modifier = Modifier.fillMaxSize())
            }
        }
    }

    item(key = "add-new", span = StaggeredGridItemSpan.FullLine, contentType = "action") {
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

/** Tone + art palette for USER collections (built-ins carry their own).
 *  v3xx — expanded for the New-collection style picker so there's plenty
 *  of variety; `tone`/`art` on a CurioCollection are INDICES into these
 *  two lists (-1 = auto/cycle). */
private val userShelfTones = listOf(
    V2ShelfTone(light = 0xFFE6D7B6, dark = 0xFF4E4534),  // warm sand
    V2ShelfTone(light = 0xFFD6CAE9, dark = 0xFF4A3E63),  // soft lavender
    V2ShelfTone(light = 0xFFC9DDD2, dark = 0xFF3A4F43),  // sage
    V2ShelfTone(light = 0xFFEAC9CF, dark = 0xFF613F4B),  // blush pink
    V2ShelfTone(light = 0xFFC9DDE9, dark = 0xFF3A5160),  // powder blue
    V2ShelfTone(light = 0xFFC9E4D8, dark = 0xFF3A5449),  // mint
    V2ShelfTone(light = 0xFFF2D8C3, dark = 0xFF5E4634),  // peach
    V2ShelfTone(light = 0xFFC9D4E0, dark = 0xFF3C4A5C),  // slate
    V2ShelfTone(light = 0xFFF1E3B8, dark = 0xFF54492E)   // butter
)
private val userShelfArts = listOf(
    V2ShelfArtType.STAR,
    V2ShelfArtType.NOTES,
    V2ShelfArtType.MOUNTAIN,
    V2ShelfArtType.PHOTOS,
    V2ShelfArtType.BOOKS,
    V2ShelfArtType.CONSTELLATION,
    V2ShelfArtType.READING,
    V2ShelfArtType.PEAK,
    V2ShelfArtType.WINDOW,
    V2ShelfArtType.MINIMAL_SUN,
    V2ShelfArtType.MINIMAL_RINGS,
    V2ShelfArtType.MINIMAL_WAVE,
    V2ShelfArtType.MINIMAL_DOTS
)

/** Icon glyphs offered in the New-collection sheet's style picker. */
private val collectionIconOptions = listOf(
    CurioIcons.AutoAwesome,
    CurioIcons.Star,
    CurioIcons.Bookmark,
    CurioIcons.MenuBook,
    CurioIcons.MusicNote,
    CurioIcons.Flag,
    CurioIcons.Lightbulb,
    CurioIcons.Brush,
    CurioIcons.Palette,
    CurioIcons.EmojiEvents,
    CurioIcons.Note,
    CurioIcons.Person
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
    val context = LocalContext.current
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
                        // v3xx — the preview wears each cover's OWN color too
                        // (dominant color extracted from the cached art). The
                        // category fallback is computed OUTSIDE the remember
                        // (themedAccent is @Composable).
                        val fallbackAccent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
                        val accent = remember(item.name, item.kind, CabinetCoverCache.version.intValue) {
                            CabinetCoverCache.dominantCoverColor(
                                context,
                                CabinetCoverCache.CoverKind.valueOf(item.kind.name),
                                item.name,
                                fallbackAccent
                            )
                        }
                        // v3xx35 — NO category-tint plate behind the covers: the
                        // art renders edge-to-edge (V2JacketArt shows its own
                        // accent placeholder only while a cover is loading).
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 88.dp)
                        ) {
                            V2JacketArt(
                                item = item,
                                accent = accent,
                                modifier = Modifier.fillMaxSize()
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

/** v3xx — a LIKED media tile in the Everything masonry. Every kind
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
    val context = LocalContext.current
    val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
    val fallbackAccent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
    // v3xx — the tile wears ITS OWN color: the dominant color extracted from
    // its cover art (fallback = category accent while the cover is still
    // downloading). Re-keys when the cover cache warms a file.
    val accent = remember(item.name, item.kind, CabinetCoverCache.version.intValue) {
        CabinetCoverCache.dominantCoverColor(
            context,
            CabinetCoverCache.CoverKind.valueOf(item.kind.name),
            item.name,
            fallbackAccent
        )
    }
    val dark = isCurioDarkTheme()
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = androidx.compose.ui.graphics.lerp(
            cat?.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ?: MaterialTheme.colorScheme.surfaceContainerHigh,
            accent,
            if (dark) 0.10f else 0.16f
        )
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

/** Which cover kind a REVIEW's topic refers to — decided by which art the
 *  Cabinet already persisted for that name (books key by name, albums /
 *  series by `album|<name>` / `series|<name>`). Null = no cover yet → the
 *  review falls back to the category accent. */
private fun reviewCoverKind(name: String): CabinetCoverCache.CoverKind? {
    if (!AppPreferences.bookCoverUrlsState[name].isNullOrBlank()) return CabinetCoverCache.CoverKind.BOOK
    if (!AppPreferences.sheetArtUrlsState["album|$name"].isNullOrBlank()) return CabinetCoverCache.CoverKind.ALBUM
    if (!AppPreferences.sheetArtUrlsState["series|$name"].isNullOrBlank()) return CabinetCoverCache.CoverKind.SERIES
    return null
}

/** v3xx — a REVIEW (ReelNotes) tile with the REVIEWED media's OWN color: the
 *  saved reviews of books/albums/series no longer masquerade as generic
 *  capture cards — the dominant color extracted from the reviewed cover
 *  tints the card (the thick outline is gone), with the quote mark, star
 *  rating and a text preview making a review read as a review at a glance. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2ReviewTileCard(
    entry: CurioEntry,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cat = CurioCategories.byId(entry.topic.categoryId)
    val fallbackAccent = cat.themedAccent()
    val kind = reviewCoverKind(entry.topic.name)
    // v3xx — the review wears the reviewed media's OWN extracted cover
    // color (fallback = category accent while the cover is still
    // downloading). Re-keys when the cover cache warms a file.
    val accent = if (kind != null) {
        remember(entry.topic.name, kind, CabinetCoverCache.version.intValue) {
            CabinetCoverCache.dominantCoverColor(context, kind, entry.topic.name, fallbackAccent)
        }
    } else fallbackAccent
    val data = entry.captureData as? CaptureData.ReelNotes
    Surface(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        // v3xx — no more outline: the extracted cover color tints the card
        // so it keeps its identity without the boxy border.
        color = androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            accent,
            if (isCurioDarkTheme()) 0.08f else 0.14f
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
    val context = LocalContext.current
    val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
    val fallbackAccent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
    // v3xx — the tile wears ITS OWN color: the dominant color extracted from
    // its cover art (fallback = category accent while the cover is still
    // downloading). Re-keys when the cover cache warms a file.
    val accent = remember(item.name, item.kind, CabinetCoverCache.version.intValue) {
        CabinetCoverCache.dominantCoverColor(
            context,
            CabinetCoverCache.CoverKind.valueOf(item.kind.name),
            item.name,
            fallbackAccent
        )
    }
    // v3xx35 — NO outline + NO background plate behind the art: the tile is
    // the page surface and the cover art sits edge-to-edge on it (the old
    // tinted Surface + 8dp frame read as an outline with a wash behind the
    // album/series tiles).
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
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
    onEntryClick: (String) -> Unit,
    // v3xx33 — the liked-row ⋮ (shelf toggles + cover source sheet).
    onLikedMore: (V2Liked) -> Unit = {},
    // v3xx34 — an Add pill in the header (Favorites opens the add sheet in
    // favorites mode).
    onAdd: (() -> Unit)? = null
) {
    val q = searchQuery.trim()
    fun shown(text: String): Boolean = q.isEmpty() || text.contains(q, ignoreCase = true)
    val shownLikes = likes.filter { shown(it.name) || it.topic?.byline?.let { b -> shown(b) } == true }
    val shownEntries = entries.filter {
        shown(it.topic.name) || it.title?.let { t -> shown(t) } == true || it.tags.any { tag -> shown(tag) }
    }
    val total = shownLikes.size + shownEntries.size

    item(key = "v-head", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        if (onAdd != null) {
            // v3xx34 — Favorites wears the collection-detail header language:
            // title + count + an emphasized Add pill.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$total item${if (total == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                V2ToolbarPill(
                    glyph = CurioIcons.Add,
                    contentDescription = "Add to favorites",
                    emphasized = true,
                    onClick = onAdd
                )
            }
        } else {
            V2PageSectionHeader(
                title = title,
                subtitle = null,
                trailing = "$total item${if (total == 1) "" else "s"}"
            )
        }
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
            V2LikedRow(item = item, onClick = { onOpenLiked(item) }, onMore = { onLikedMore(item) })
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
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var moreOpen by remember { mutableStateOf(false) }
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
                text = "$count item${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        // v3xx — the Add action is a LARGER labeled pill ("+ Add") — the
        // style that will expand app-wide; the ⋮ sits in a slightly bigger
        // circular button next to it (still an ANCHORED dropdown below).
        Surface(
            onClick = onAdd,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
            modifier = Modifier.height(46.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = "Add saved captures",
                    tint = MaterialTheme.colorScheme.primary,
                    size = 22.dp
                )
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        // v3xx — the collection ⋮ is an ANCHORED dropdown (Rename / Add
        // captures / Delete) right under the dots — no center-screen overlay.
        Box {
            Surface(
                onClick = { moreOpen = true },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CurioIcon(
                        name = CurioIcons.MoreVert,
                        contentDescription = "Rename, add or delete collection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp
                    )
                }
            }
            DropdownMenu(
                expanded = moreOpen,
                onDismissRequest = { moreOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        CurioIcon(name = CurioIcons.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, size = 17.dp)
                    },
                    onClick = { moreOpen = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("Add captures", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        CurioIcon(name = CurioIcons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, size = 17.dp)
                    },
                    onClick = { moreOpen = false; onAdd() }
                )
                DropdownMenuItem(
                    text = { Text("Delete collection", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        CurioIcon(name = CurioIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, size = 17.dp)
                    },
                    onClick = { moreOpen = false; onDelete() }
                )
            }
        }
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
    initialTone: Int,
    initialArt: Int,
    initialIcon: String?,
    moodboards: List<CurioEntry>,
    showMoodboards: Boolean,
    title: String,
    confirmLabel: String,
    onConfirm: (name: String, tone: Int, art: Int, icon: String?) -> Unit,
    onMoodboard: (CurioEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    // v3xx — custom card style (tone / art / icon): -1 / "" = Auto (the
    // card cycles the palette); tap a selected option again to reset it.
    var tone by rememberSaveable(initialTone) { mutableStateOf(initialTone) }
    var art by rememberSaveable(initialArt) { mutableStateOf(initialArt) }
    var icon by rememberSaveable(initialIcon ?: "") { mutableStateOf(initialIcon ?: "") }
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
                .verticalScroll(rememberScrollState())
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

            // ── Card style — tone swatches, art previews, icon chips. All
            // optional (left on Auto the card cycles the palette so every
            // collection still looks hand-picked); tap again to reset.
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Card style",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Pick a tone, art and icon — tap one again to go back to Auto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            // Tone swatches.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "style-t-auto") {
                    StyleAutoChip(selected = tone < 0) { tone = -1 }
                }
                itemsIndexed(userShelfTones) { i, t ->
                    val selected = tone == i
                    val dark = isCurioDarkTheme()
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(if (dark) Color(t.dark) else Color(t.light))
                            .then(
                                if (selected)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(21.dp))
                                else Modifier
                            )
                            .clickable { tone = if (selected) -1 else i },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            CurioIcon(
                                name = CurioIcons.Check,
                                contentDescription = null,
                                tint = if (dark) Color(0xFFEAF3EC) else Color.White,
                                size = 18.dp
                            )
                        }
                    }
                }
            }

            // Art previews (the drawn scene on its tone fill, exactly like
            // the card wears it).
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "style-a-auto") {
                    StyleAutoChip(selected = art < 0) { art = -1 }
                }
                itemsIndexed(userShelfArts) { i, a ->
                    val selected = art == i
                    val dark = isCurioDarkTheme()
                    val t = userShelfTones[i % userShelfTones.size]
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (dark) Color(t.dark) else Color(t.light))
                            .then(
                                if (selected)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable { art = if (selected) -1 else i }
                    ) {
                        V2ShelfArt(
                            art = a,
                            dark = dark,
                            modifier = Modifier.fillMaxSize().alpha(0.6f)
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    size = 12.dp
                                )
                            }
                        }
                    }
                }
            }

            // Icon chips.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "style-i-auto") {
                    StyleAutoChip(selected = icon.isBlank()) { icon = "" }
                }
                itemsIndexed(collectionIconOptions) { _, glyph ->
                    val selected = icon == glyph
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .then(
                                if (selected)
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable { icon = if (selected) "" else glyph },
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(
                            name = glyph,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 21.dp
                        )
                    }
                }
            }

            Surface(
                onClick = {
                    onConfirm(name.trim().ifBlank { "Collection" }, tone, art, icon.ifBlank { null })
                },
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

/** The leading "Auto" chip in each style-picker row — the collection card
 *  cycles the tone/art/icon palette on its own when Auto is selected. */
@Composable
private fun StyleAutoChip(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .then(
                if (selected)
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Auto",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** v3xx33 — the "Curiying now" / "Want to read" shelf toggles for a liked
 *  item (shown in the liked-item sheet — the Cabinet half of the toggle,
 *  the reveal bottom sheets are the other half). */
@Composable
private fun V2ShelfToggleChips(
    context: Context,
    topicName: String,
    categoryId: CategoryId,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val collections = AppPreferences.collectionsState
    fun inShelf(id: String): Boolean = collections.firstOrNull { it.id == id }
        ?.members?.any {
            it.kind == CurioCollectionMember.MemberKind.TOPIC &&
                it.categoryName == categoryId.name && it.refName == topicName
        } == true
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    @Composable fun chip(id: String, label: String, icon: String) {
        val active = inShelf(id)
        Surface(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                AppPreferences.toggleShelfTopic(context, id, categoryId, topicName)
            },
            shape = RoundedCornerShape(50),
            color = if (active) accent.copy(alpha = 0.16f) else surface.copy(alpha = 0.7f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                CurioIcon(
                    name = icon,
                    contentDescription = null,
                    tint = if (active) accent else onSurface,
                    size = 15.dp
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (active) accent else onSurface
                )
            }
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        chip("shelf:currently-reading", "Curiying now", CurioIcons.PlayCircle)
        chip("shelf:want-to-read", "Want to read", CurioIcons.Bookmark)
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
            Spacer(Modifier.height(10.dp))
            // v3xx33 — the Cabinet shelf toggles (Curiying now / Want to
            // read), live from the liked-item sheet.
            V2ShelfToggleChips(
                context = context,
                topicName = item.name,
                categoryId = item.topic?.categoryId ?: item.kind.categoryId()
            )
            Spacer(Modifier.height(10.dp))
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

// ── Add-to-shelf model (v3xx34) ────────────────────────────────────────

/** What the add sheet targets: a real collection (topic + capture members)
 *  or the Favorites virtual shelf (favorites mode — topics only). */
private sealed interface AddTarget {
    data class Collection(val collection: CurioCollection) : AddTarget
    data object Favorites : AddTarget
}

/** The add-sheet's lightweight saved-capture row — id + display text +
 *  format glyph. Projected ONCE in the parent so the picker never carries
 *  full [CurioEntry] objects (their capture payloads are what made the old
 *  sheet lag on big archives). */
private data class AddOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val glyph: String
)

/** A liked item as a collection TOPIC member. */
private fun likedToMember(liked: V2Liked): CurioCollectionMember =
    CurioCollectionMember(
        kind = CurioCollectionMember.MemberKind.TOPIC,
        categoryName = liked.kind.categoryId().name,
        refName = liked.name
    )

/** Whether a liked item sits in the favorites sets right now. */
private fun isLikedFavorite(liked: V2Liked, context: Context): Boolean = when (liked.kind) {
    V2Kind.BOOK -> AppPreferences.bookFavoritesState.contains(liked.name)
    V2Kind.SERIES -> AppPreferences.seriesFavoritesState.contains(liked.name)
    V2Kind.ALBUM -> AppPreferences.albumFavTracksState.containsKey(liked.name)
}

/** Save / un-save a liked item (favorites-mode add). Albums save their
 *  first authored track — the reveal hearts per-track, so this keeps the
 *  album inside the favorites sets the same way. */
private fun toggleLikedFavorite(liked: V2Liked, context: Context) {
    when (liked.kind) {
        V2Kind.BOOK -> AppPreferences.toggleBookFavorite(context, liked.name)
        V2Kind.SERIES -> AppPreferences.toggleSeriesFavorite(context, liked.name)
        V2Kind.ALBUM -> {
            val track = liked.topic?.tracks?.firstOrNull()?.title ?: liked.name
            AppPreferences.toggleAlbumFavoriteTrack(context, liked.name, track)
        }
    }
}

/** With no query the saved-capture list renders at most this many rows —
 *  search finds the rest. Keeps opening the picker instant on big archives. */
private const val ADD_OPTION_CAP = 100

/** v3xx34 — the redesigned add sheet: a search field over topics
 *  (favorites + saved captures + one catalog fallback), a favorites
 *  quick-pick list, and the saved-captures multi-pick. [target] decides
 *  the mode: [AddTarget.Collection] adds topic/entry members;
 *  [AddTarget.Favorites] saves / un-saves topics. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V2AddToShelfSheet(
    target: AddTarget,
    likes: List<V2Liked>,
    addOptions: List<AddOption>,
    onAddEntries: (List<String>) -> Unit,
    onToggleTopic: (V2Liked) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var query by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    val isFavorites = target is AddTarget.Favorites
    val collection = (target as? AddTarget.Collection)?.collection

    val existingTopicKeys = remember(collection) {
        collection?.members?.filter { it.kind == CurioCollectionMember.MemberKind.TOPIC }
            ?.map { "${it.categoryName}|${it.refName}" }?.toSet() ?: emptySet()
    }
    val existingEntryIds = remember(collection) {
        collection?.members?.filter { it.kind == CurioCollectionMember.MemberKind.ENTRY }
            ?.map { it.refName }?.toSet() ?: emptySet()
    }

    val q = query.trim()
    fun shown(text: String): Boolean = q.isEmpty() || text.contains(q, ignoreCase = true)

    // Favorite quick-pick pool — liked books/series/albums, search-filtered.
    val shownLikes = remember(q, likes) {
        likes.filter { shown(it.name) || it.topic?.byline?.let { b -> shown(b) } == true }
    }
    // Saved-capture pool — the lightweight projection; already-added hidden.
    val availableOptions = remember(q, addOptions, existingEntryIds) {
        addOptions.filterNot { it.id in existingEntryIds }
            .filter { shown(it.name) || shown(it.subtitle) }
    }
    // Catalog fallback — one best topic match when the pools come up empty
    // (cached lanes only, never a full catalog load — stays O(1) per
    // keystroke).
    val catalogHit = remember(q, shownLikes, availableOptions) {
        if (q.length >= 2 && shownLikes.isEmpty() && availableOptions.isEmpty()) {
            TopicCatalog.findByName(q)?.let { V2Liked(it.name, topicKind(it), it) }
        } else null
    }
    // Lag guard: cap the rendered capture list when idle; search finds more.
    val cappedOptions = if (q.isEmpty() && availableOptions.size > ADD_OPTION_CAP)
        availableOptions.take(ADD_OPTION_CAP) else availableOptions

    fun inTarget(liked: V2Liked): Boolean =
        if (isFavorites) isLikedFavorite(liked, context)
        else "${liked.kind.categoryId().name}|${liked.name}" in existingTopicKeys

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
        ) {
            item(key = "title") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFavorites) "Add to Favorites"
                            else "Add to ${collection?.name ?: ""}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFavorites) "Search topics to save them here"
                            else "Search topics, or pick from favorites & saved captures",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isFavorites && picked.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${picked.size} picked",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item(key = "search") {
                AddSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = if (isFavorites) "Search topics to add…" else "Search topics or captures…"
                )
            }

            if (shownLikes.isNotEmpty()) {
                item(key = "likes-label") {
                    AddSectionLabel(if (isFavorites) "Your favorites — tap to remove" else "From favorites")
                }
                items(shownLikes.size, key = { "lk-$it" }) { index ->
                    val liked = shownLikes[index]
                    AddTopicPickRow(
                        liked = liked,
                        added = inTarget(liked),
                        favoritesMode = isFavorites,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onToggleTopic(liked)
                        }
                    )
                }
            }

            if (!isFavorites && availableOptions.isNotEmpty()) {
                item(key = "entries-label") {
                    AddSectionLabel("Saved captures")
                }
                items(cappedOptions.size, key = { "en-${cappedOptions[it].id}" }) { index ->
                    val opt = cappedOptions[index]
                    AddEntryPickRow(
                        option = opt,
                        checked = opt.id in picked,
                        onCheckedChange = { on ->
                            picked = if (on) picked + opt.id else picked - opt.id
                        }
                    )
                }
                if (cappedOptions.size < availableOptions.size) {
                    item(key = "entries-more") {
                        Text(
                            text = "…and ${availableOptions.size - cappedOptions.size} more — search to find them",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 53.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            }

            if (catalogHit != null) {
                item(key = "catalog-label") {
                    AddSectionLabel("Search results")
                }
                item(key = "catalog-hit") {
                    AddTopicPickRow(
                        liked = catalogHit,
                        added = inTarget(catalogHit),
                        favoritesMode = isFavorites,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onToggleTopic(catalogHit)
                        }
                    )
                }
            }

            if (shownLikes.isEmpty() && availableOptions.isEmpty() && catalogHit == null) {
                item(key = "empty") {
                    Text(
                        text = "Nothing matches — try a different search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }

            if (!isFavorites && picked.isNotEmpty()) {
                item(key = "add") {
                    Surface(
                        onClick = { onAddEntries(picked.toList()) },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(top = 6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (picked.size == 1) "Add 1 capture" else "Add ${picked.size} captures",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The add sheet's frosted search field (the hub's frosted-tile language). */
@Composable
private fun AddSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFF2E8DC))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        CurioIcon(
            name = CurioIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 18.dp
        )
        Spacer(Modifier.width(9.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") }
            ) {
                CurioIcon(
                    name = CurioIcons.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 16.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/** A small settings-style section label inside the add sheet. */
@Composable
private fun AddSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 2.dp)
    )
}

/** One liked-item pick row: kind tile + name/byline + the target state
 *  (check = already in the collection; bookmark = already in favorites). */
@Composable
private fun AddTopicPickRow(
    liked: V2Liked,
    added: Boolean,
    favoritesMode: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(
                name = when (liked.kind) {
                    V2Kind.BOOK -> CurioIcons.Books
                    V2Kind.ALBUM -> CurioIcons.Album
                    V2Kind.SERIES -> CurioIcons.Movie
                },
                contentDescription = null,
                tint = accent,
                size = 20.dp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = liked.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = liked.topic?.byline?.ifBlank { null } ?: when (liked.kind) {
                    V2Kind.BOOK -> "Book"
                    V2Kind.ALBUM -> "Album"
                    V2Kind.SERIES -> "Series"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // The state glyph: collection mode = a check when already added;
        // favorites mode = a filled bookmark when already saved.
        CurioIcon(
            name = if (favoritesMode) {
                if (added) CurioIcons.Bookmark else CurioIcons.BookmarkBorder
            } else {
                if (added) CurioIcons.Check else CurioIcons.Add
            },
            contentDescription = null,
            tint = if (added) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            size = 20.dp
        )
    }
}

/** One saved-capture pick row: format tile + name/subtitle + checkbox. */
@Composable
private fun AddEntryPickRow(
    option: AddOption,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(
                name = option.glyph,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                size = 20.dp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ────────────────────────────────────────────────────────────────────────
// Member / liked helpers
// ────────────────────────────────────────────────────────────────────────

private sealed interface PillTarget {
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

/** The canonical lane for a liked kind (the lane where the reveal hearts
 *  live) — maps liked items to collection TOPIC members and to the
 *  favorites sets. */
private fun V2Kind.categoryId(): CategoryId = when (this) {
    V2Kind.BOOK -> CategoryId.BOOKS
    V2Kind.ALBUM -> CategoryId.ALBUMS
    V2Kind.SERIES -> CategoryId.SERIES
}

/** Kind-aware topic resolution for liked rows. Books/series/albums are
 *  hearted on their CANONICAL lane (BOOKS / SERIES / ALBUMS — that's where
 *  the heart lives in the reveal sheets), so that lane is searched FIRST: a
 *  global [TopicCatalog.findByName] walks lanes in enum order and would hand
 *  "Animal Farm" (the book) to "Animal Farm (1954)" (the animated film)
 *  because ANIMATED_MOVIES precedes BOOKS. The global search stays as the
 *  fallback for legacy / renamed names — but it is CANONICAL-LANE
 *  GUARDED: a liked book must NEVER resolve to a series, film, album or
 *  author that merely shares its name (the "wrong entry opens" bug — the
 *  saved book opening the wrong lane's reveal). Only a global hit that
 *  lives on the SAME canonical lane is accepted; anything else falls
 *  through to the canonical-lane open, which resolves by name within the
 *  right lane at reveal time. */
private fun findLikedTopic(kind: V2Kind, name: String): CurioTopic? {
    val lane = when (kind) {
        V2Kind.BOOK -> CategoryId.BOOKS
        V2Kind.ALBUM -> CategoryId.ALBUMS
        V2Kind.SERIES -> CategoryId.SERIES
    }
    TopicJsonLoader.cached(lane)?.firstOrNull {
        it.matchesSavedNameStrict(name) || it.matchesSavedName(name)
    }?.let { return it }
    return TopicCatalog.findByName(name)?.takeIf { it.categoryId == lane }
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
    onLongClick: (() -> Unit)? = null,
    // v3xx33 — the row's ⋮ opens the liked-item sheet (shelf toggles +
    // cover source), so the shelf toggles are reachable from Cabinet rows.
    onMore: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
    val fallbackAccent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
    // v3xx — the row wears ITS OWN color: the dominant color extracted from
    // its cover art (fallback = category accent while the cover is still
    // downloading). Re-keys when the cover cache warms a file.
    val accent = remember(item.name, item.kind, CabinetCoverCache.version.intValue) {
        CabinetCoverCache.dominantCoverColor(
            context,
            CabinetCoverCache.CoverKind.valueOf(item.kind.name),
            item.name,
            fallbackAccent
        )
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        color = androidx.compose.ui.graphics.lerp(
            cat?.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ?: MaterialTheme.colorScheme.surfaceContainerHigh,
            accent,
            if (isCurioDarkTheme()) 0.10f else 0.16f
        )
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
            if (onMore != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable(onClick = onMore),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.MoreVert,
                        contentDescription = "Curiying now / Want to read / cover source",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        size = 19.dp
                    )
                }
            } else {
                CurioIcon(
                    name = CurioIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    size = 20.dp
                )
            }
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
    // v3xx35 — the accent plate is only the LOADING placeholder now: once a
    // cover is on screen (local cache or network) the art renders straight
    // on the page — no colored wash peeking around the fitted image (the
    // "background behind the album" look).
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .then(
                if (url == null && local == null) Modifier.background(plate)
                else Modifier
            )
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
            // v3xx — PURE cover art: no fake spine strip, no gradient wash
            // (the "overlay or background" on book covers). The art renders
            // edge-to-edge with a whisper of rounded corners and nothing
            // else on top.
        }
    }
}
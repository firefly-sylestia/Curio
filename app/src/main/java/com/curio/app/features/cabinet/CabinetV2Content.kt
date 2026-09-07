package com.curio.app.features.cabinet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicCatalog
import com.curio.app.data.shortName
import com.curio.app.features.reveal.AlbumArtFetch
import com.curio.app.features.reveal.SeriesPosterFetch
import com.curio.app.features.settings.BookCoverFetch
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioEmptyState
import com.curio.app.ui.components.CurioEntryCard
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.CurioTwoStepDeleteDialog
import com.curio.app.ui.components.fauxGlassCapsule
import com.curio.app.ui.components.isLiquidGlassPillsActive
import com.curio.app.ui.components.isLiquidGlassRequested
import com.curio.app.ui.components.liquidGlassCapsule
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.themedAccent
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * v3xx — CABINET v2: the experimental collections view behind
 * Settings → Experiments → "Cabinet v2 collections".
 *
 * One page, four grouped sections:
 *  - Saved — the classic capture grid (CurioEntryCard), with the same
 *    long-press multi-select + batch move-to-recycle-bin flow.
 *  - Liked books / Liked series / Liked albums — rows with CONTAIN-FIT
 *    jacket art (books = half-book jacket with a spine + sheen, albums =
 *    square, series = poster — never stretched), resolving the same cover
 *    sources the reveal pages use (authored URL → stored/hub cover →
 *    keyless resolver when the matching fetch toggle is on).
 *
 * The toolbar is a translucent glass bar (real refracted glass when the
 * Liquid glass experiment is on, a faux-glass coat below Android 12 /
 * when the toggle is off, plain translucent otherwise) that sits OVER the
 * scrolling grid so content slides underneath it; search filters every
 * section, and each section header collapses in place.
 */
@Composable
fun CabinetV2Content(navController: NavController) {
    val context = LocalContext.current
    val wide = windowWidthSizeClass().isWide
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    // ── Glass plumbing: the grid records into a LOCAL backdrop the toolbar
    // blurs (toolbar is a sibling overlay OUTSIDE the captured subtree, the
    // in-screen-glass architecture — never the NavHost capture).
    val glassOn = isLiquidGlassPillsActive()
    val glassBackdrop = if (glassOn) rememberLayerBackdrop() else null

    // ── Data: saved entries (repo flow) + liked books/series/albums.
    val entries by androidx.compose.runtime.produceState<List<CurioEntry>>(initialValue = emptyList()) {
        try {
            CurioRepositoryHolder.repo.observeAll().collect { value = it }
        } catch (_: Exception) {
            value = emptyList()
        }
    }
    val books = remember(AppPreferences.bookFavoritesState, AppPreferences.bookCoverUrlsState) {
        AppPreferences.getBookFavorites(context)
            .map { name -> V2Liked(name, V2Kind.BOOK, TopicCatalog.findByName(name)) }
            .sortedBy { it.name.lowercase() }
    }
    val albums = remember(AppPreferences.albumFavTracksState) {
        AppPreferences.albumFavTracksState.keys
            .filter { it.isNotBlank() }
            .map { name -> V2Liked(name, V2Kind.ALBUM, TopicCatalog.findByName(name)) }
            .sortedBy { it.name.lowercase() }
    }
    val series = remember(AppPreferences.seriesFavoritesState) {
        AppPreferences.getSeriesFavorites(context)
            .map { name -> V2Liked(name, V2Kind.SERIES, TopicCatalog.findByName(name)) }
            .sortedBy { it.name.lowercase() }
    }

    // ── Search — filters every section by name / byline / entry text.
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
    val filteredEntries = remember(entries, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) entries
        else entries.filter {
            matchesQ(it.topic.name) || it.title?.let { t -> matchesQ(t) } == true ||
                it.tags.any { tag -> matchesQ(tag) }
        }
    }
    // ── Capture-format filter (ANALYSIS.md 4.4.5): narrow the saved
    // captures by format with tiny chips; the chip row only appears when
    // several formats actually exist among the entries. Tapping the active
    // chip clears the filter.
    var formatFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val availableFormats = remember(entries) {
        entries.map { it.format }.distinct().sortedBy { it.shortName }
    }
    val shownEntries = remember(filteredEntries, formatFilter) {
        if (formatFilter == null) filteredEntries
        else filteredEntries.filter { it.format.name == formatFilter }
    }
    fun filteredLiked(all: List<V2Liked>) = if (searchQuery.isBlank()) all
    else all.filter {
        matchesQ(it.name) || (it.topic?.byline?.let { b -> matchesQ(b) } == true)
    }
    val shownBooks = filteredLiked(books)
    val shownAlbums = filteredLiked(albums)
    val shownSeries = filteredLiked(series)

    // ── Collapsible sections (rememberSaveable comma-joined names).
    var collapsedNames by rememberSaveable { mutableStateOf("") }
    val collapsedSet: Set<String> = remember(collapsedNames) {
        collapsedNames.splitToSequence(',').filter { it.isNotBlank() }.toSet()
    }
    fun toggleCollapsed(key: String) {
        collapsedNames = if (key in collapsedSet) {
            (collapsedSet - key).joinToString(",")
        } else {
            (collapsedSet + key).joinToString(",")
        }
    }
    // While searching, every section force-expands so matches are visible.
    val searching = searchActive && searchQuery.isNotBlank()
    fun expanded(key: String) = !searching && key !in collapsedSet

    // ── Multi-select batch (saved entries only — mirrors the classic grid).
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedEntryIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    // Stable identity for the visible set (a fresh Set each recomposition
    // would restart the prune effect below every frame).
    val visibleIds = remember(shownEntries) { shownEntries.map { it.id }.toSet() }
    LaunchedEffect(selectedEntryIds, visibleIds) {
        val kept = selectedEntryIds.intersect(visibleIds)
        if (kept != selectedEntryIds) selectedEntryIds = kept
        if (selectedEntryIds.isEmpty() && selectionMode) selectionMode = false
    }
    val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedEntryIds }

    // RAW content (before search / format filtering) — decides between the
    // genuinely-empty Cabinet (suggestions) and a filtered-to-nothing page.
    val rawContent = entries.isNotEmpty() || books.isNotEmpty() ||
        albums.isNotEmpty() || series.isNotEmpty()
    val nothingVisible = shownEntries.isEmpty() && shownBooks.isEmpty() &&
        shownAlbums.isEmpty() && shownSeries.isEmpty()

    // ── Empty-Cabinet suggestions (ANALYSIS.md 4.4.4): when nothing is
    // saved or liked yet, surface three random discoveries from the loaded
    // pools with a Shuffle pill — a real starting point instead of a blank
    // page. The seed bump re-rolls them.
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

    // ── Shared accents for the toolbar + section headers.
    val pageAccent = MaterialTheme.colorScheme.primary
    val toolbarInk = MaterialTheme.colorScheme.onSurface

    val toolbarMod = when {
        glassOn && glassBackdrop != null -> Modifier.liquidGlassCapsule(
            container = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
            backdrop = glassBackdrop,
            shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
            washAlpha = 0.55f
        )
        isLiquidGlassRequested() -> Modifier.fauxGlassCapsule(
            MaterialTheme.colorScheme.surfaceContainer, corner = 22.dp
        )
        else -> Modifier.background(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── The scrolling grid — runs UNDER the glass toolbar.
        LazyVerticalGrid(
            state = gridState,
            columns = if (wide) GridCells.Adaptive(minSize = 176.dp) else GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (searchActive) 200.dp else 124.dp,
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
            if (!rawContent) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    if (searching) {
                        CurioEmptyState(
                            glyph = CurioIcons.SearchOff,
                            headline = "No matches",
                            subtext = "Nothing in the Cabinet matches \"${searchQuery.trim()}\".",
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                            ctaLabel = "Clear search",
                            onCtaClick = { searchQuery = ""; searchActive = false }
                        )
                    } else {
                        V2EmptySuggestions(
                            suggestions = suggestions,
                            onShuffle = { suggestionSeed++ },
                            onOpen = { t ->
                                navController.navigate(
                                    CurioRoutes.revealFor(t.categoryId.routeSlug, t.name)
                                ) { launchSingleTop = true }
                            }
                        )
                    }
                }
            } else if (nothingVisible) {
                // Content exists but every section was filtered / searched
                // away — a quiet no-match state with one tap to clear.
                item(key = "empty-filtered", span = { GridItemSpan(maxLineSpan) }) {
                    CurioEmptyState(
                        glyph = CurioIcons.SearchOff,
                        headline = if (searching) "No matches" else "Nothing matches these filters",
                        subtext = "Try clearing the search or the format filter.",
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                        ctaLabel = "Clear filters",
                        onCtaClick = {
                            searchQuery = ""; searchActive = false; formatFilter = null
                        }
                    )
                }
            }

            if (shownEntries.isNotEmpty()) {
                // ── Capture-format filter chips (ANALYSIS.md 4.4.5): shown
                // when the entries span several formats; tapping the active
                // chip clears the filter. Clean, no hint text.
                if (availableFormats.size > 1 || formatFilter != null) {
                    item(key = "format-chips", span = { GridItemSpan(maxLineSpan) }, contentType = "chips") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableFormats.forEach { fmt ->
                                val selected = formatFilter == fmt.name
                                Surface(
                                    onClick = { formatFilter = if (selected) null else fmt.name },
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) pageAccent
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = fmt.shortName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "h-saved", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    V2SectionHeader(
                        title = "Saved",
                        count = shownEntries.size,
                        glyph = CurioIcons.Inventory2,
                        expanded = expanded("saved"),
                        accent = pageAccent,
                        onToggle = { toggleCollapsed("saved") }
                    )
                }
                if (expanded("saved")) {
                    items(shownEntries, key = { "e|${it.id}" }) { entry ->
                        CurioEntryCard(
                            entry = entry,
                            modifier = Modifier,
                            selected = entry.id in selectedEntryIds,
                            onLongClick = {
                                selectionMode = true
                                selectedEntryIds = selectedEntryIds + entry.id
                            },
                            onClick = {
                                if (selectionMode) {
                                    selectedEntryIds = if (entry.id in selectedEntryIds) {
                                        selectedEntryIds - entry.id
                                    } else {
                                        selectedEntryIds + entry.id
                                    }
                                } else {
                                    navController.navigate(
                                        CurioRoutes.entryDetail(entry.id)
                                    ) { launchSingleTop = true }
                                }
                            }
                        )
                    }
                }
            }

            if (shownBooks.isNotEmpty()) {
                item(key = "h-books", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    V2SectionHeader(
                        title = "Liked books",
                        count = shownBooks.size,
                        glyph = CurioIcons.MenuBook,
                        expanded = expanded("books"),
                        accent = pageAccent,
                        onToggle = { toggleCollapsed("books") }
                    )
                }
                if (expanded("books")) {
                    items(shownBooks, key = { "b|${it.name}" }) { item ->
                        V2LikedRow(item = item, onClick = { item.open(navController) })
                    }
                }
            }

            if (shownSeries.isNotEmpty()) {
                item(key = "h-series", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    V2SectionHeader(
                        title = "Liked series",
                        count = shownSeries.size,
                        glyph = CurioIcons.Movies,
                        expanded = expanded("series"),
                        accent = pageAccent,
                        onToggle = { toggleCollapsed("series") }
                    )
                }
                if (expanded("series")) {
                    items(shownSeries, key = { "s|${it.name}" }) { item ->
                        V2LikedRow(item = item, onClick = { item.open(navController) })
                    }
                }
            }

            if (shownAlbums.isNotEmpty()) {
                item(key = "h-albums", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    V2SectionHeader(
                        title = "Liked albums",
                        count = shownAlbums.size,
                        glyph = CurioIcons.Album,
                        expanded = expanded("albums"),
                        accent = pageAccent,
                        onToggle = { toggleCollapsed("albums") }
                    )
                }
                if (expanded("albums")) {
                    items(shownAlbums, key = { "a|${it.name}" }) { item ->
                        V2LikedRow(item = item, onClick = { item.open(navController) })
                    }
                }
            }
        }

        // ── The glass toolbar — overlay, over the grid.
        Column(
            modifier = toolbarMod.fillMaxWidth().statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectionMode) "${selectedEntryIds.size} selected"
                        else "The Cabinet",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = toolbarInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selectionMode) "Long-press cards to select"
                        else "Saved captures · liked books, series & albums",
                        style = MaterialTheme.typography.bodySmall,
                        color = toolbarInk.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectionMode) {
                        V2ToolbarPill(
                            label = if (allVisibleSelected) "Clear" else "Select all",
                            emphasized = true,
                            onClick = {
                                selectedEntryIds = if (allVisibleSelected) {
                                    selectedEntryIds - visibleIds
                                } else {
                                    selectedEntryIds + visibleIds
                                }
                            }
                        )
                        V2ToolbarPill(
                            label = "Delete (${selectedEntryIds.size})",
                            destructive = true,
                            onClick = {
                                if (selectedEntryIds.isNotEmpty()) showBulkDeleteConfirm = true
                            }
                        )
                        V2ToolbarPill(
                            glyph = CurioIcons.Close,
                            contentDescription = "Cancel selection",
                            onClick = { selectionMode = false; selectedEntryIds = emptySet() }
                        )
                    } else {
                        V2ToolbarPill(
                            glyph = CurioIcons.Search,
                            contentDescription = "Search the Cabinet",
                            emphasized = true,
                            onClick = { searchActive = !searchActive }
                        )
                        V2ToolbarPill(
                            glyph = CurioIcons.Delete,
                            contentDescription = "Recycle bin",
                            onClick = { navController.navigate(CurioRoutes.RECYCLE_BIN) { launchSingleTop = true } }
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = searchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                ) {
                    CurioSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search captures, books, series, albums…",
                        modifier = Modifier.focusRequester(searchFocus),
                        onCancel = { searchActive = false; searchQuery = "" }
                    )
                }
            }
            if (!searchActive) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }

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

/** One liked item — books / series / albums all live by NAME (the identity
 *  the reveal heart toggles store). The topic is resolved tolerantly; null
 *  = the name no longer resolves (legacy / renamed) — the row still renders
 *  with a plain plate and no navigation. */
private data class V2Liked(
    val name: String,
    val kind: V2Kind,
    val topic: CurioTopic?
) {
    fun open(navController: NavController) {
        val t = topic ?: return
        navController.navigate(
            CurioRoutes.revealFor(t.categoryId.routeSlug, t.name)
        ) { launchSingleTop = true }
    }
}

private enum class V2Kind { BOOK, ALBUM, SERIES }

/** Best-fitting jacket shape for a suggested topic's lane. */
private fun topicKind(t: CurioTopic): V2Kind = when (t.categoryId) {
    CategoryId.BOOKS, CategoryId.AUTHORS -> V2Kind.BOOK
    CategoryId.SERIES, CategoryId.ANIME, CategoryId.MANGA, CategoryId.MANHWA,
    CategoryId.FILMS, CategoryId.DIRECTORS, CategoryId.ANIMATED_MOVIES -> V2Kind.SERIES
    else -> V2Kind.ALBUM
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
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().height(72.dp)
                ) {}
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

/** One section header: glyph chip + title + count + collapse chevron. */
@Composable
private fun V2SectionHeader(
    title: String,
    count: Int,
    glyph: String,
    expanded: Boolean,
    accent: Color,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = glyph,
                    contentDescription = null,
                    tint = accent,
                    size = 15.dp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            CurioIcon(
                name = if (expanded) CurioIcons.KeyboardArrowUp else CurioIcons.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse section" else "Expand section",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        }
    }
}

/** A compact toolbar pill (search / recycle bin / selection actions). */
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
 *  category + chevron. Tap opens the reveal page (where the heart lives). */
@Composable
private fun V2LikedRow(item: V2Liked, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cat = item.topic?.categoryId?.let { CurioCategories.byId(it) }
    val accent = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = cat?.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
            ?: MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
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
                        color = accent.copy(alpha = 0.9f),
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
    val consent = when (item.kind) {
        V2Kind.BOOK -> AppPreferences.bookFetchEnabledState
        V2Kind.ALBUM -> AppPreferences.albumFetchEnabledState
        V2Kind.SERIES -> AppPreferences.seriesFetchEnabledState
    }
    // Static candidates — the verified sheet art first (albums/series),
    // then the authored imageUrl; books also carry the keyless Open Library
    // title guess (same fallback the reveal poster uses); albums/series
    // resolve keylessly below when their fetch toggle is on.
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
    // Live keyless fallback for albums / series (fires once, only after
    // every static candidate has errored, and only with the user's consent).
    var resolved by remember(item.name) { mutableStateOf<String?>(null) }
    var liveDone by remember(item.name) { mutableStateOf(false) }
    // Keys include coverIndex/liveDone so the resolver re-checks when a
    // candidate errors and exhausts the list; the liveDone guard makes the
    // whole chain run at most once (no retry loop on a failing resolved URL).
    LaunchedEffect(item.name, item.kind, consent, coverIndex, liveDone) {
        if (consent && !liveDone && item.kind != V2Kind.BOOK && coverIndex >= candidates.size) {
            resolved = when (item.kind) {
                V2Kind.ALBUM -> AlbumArtFetch.resolveArtworkUrl(item.name, topic?.byline)
                V2Kind.SERIES -> SeriesPosterFetch.resolvePosterUrl(item.name)
                else -> null
            }
            liveDone = true
        }
    }
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
        // Inner clip: artwork, spine and sheen must respect the rounded
        // corners (children of a clipped Box are NOT clipped themselves).
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(corner))) {
            CurioIcon(
                name = when (item.kind) {
                    V2Kind.BOOK -> CurioIcons.MenuBook
                    V2Kind.ALBUM -> CurioIcons.Album
                    V2Kind.SERIES -> CurioIcons.Movies
                },
                contentDescription = null,
                tint = Color.White.copy(alpha = if (url == null) 0.7f else 0f),
                size = 20.dp
            )
            if (url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover art for ${item.name}",
                    contentScale = ContentScale.Fit,
                    onError = {
                        // Skip a failed candidate; once every static
                        // candidate AND the live keyless resolution have
                        // failed, settle on the plain plate (no retry loop).
                        if (coverIndex < candidates.size) coverIndex++
                        else { resolved = null; liveDone = true }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (item.kind == V2Kind.BOOK) {
                // Half-book jacket: a darker spine strip down the left edge
                // and a soft diagonal sheen over the artwork.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(Color.Black.copy(alpha = 0.20f))
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        // No explicit start/end: the default diagonal maps
                        // the gradient across the box's corners.
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

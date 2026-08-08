package com.curio.app.features.cabinet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.TopicCatalog
import com.curio.app.data.AudioStorageManager
import com.curio.app.data.ImageStorageManager
import com.curio.app.features.settings.settingsReadableInk
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.adaptive.LocalRevealSharedScope
import com.curio.app.ui.adaptive.LocalRevealVisibilityScope
import com.curio.app.ui.adaptive.RevealBoundsTransform
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioEmptyState
import com.curio.app.ui.components.CurioNavTint
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.CurioEntryCard
import com.curio.app.ui.components.MorphEntrance
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryBorder
import com.curio.app.ui.theme.categoryChipSurface
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent

/**
 * The Cabinet — see Curio design contract. Library of saved captures.
 *
 * Upgraded with:
 *  - Entry cards render at once (no per-item stagger)
 *  - MorphEntrance for empty state content
 */
/**
 * Process-local identity for Cabinet filter state. The object remains stable
 * through recomposition, rotation and in-session tab restoration, but a fresh
 * app process receives a new identity so rememberSaveable intentionally
 * discards the previous filter and opens on "All".
 */
private object CabinetSessionToken

/**
 * Saves the active Cabinet filter chip by enum name; "All" (null) stays
 * null through an empty-string sentinel, surviving rotation and navigation
 * within the current app process.
 */
private val CategoryIdSaver = Saver<CategoryId?, String>(
    save = { it?.name ?: "" },
    restore = { name ->
        name.takeIf { it.isNotEmpty() }
            ?.let { n -> CategoryId.values().firstOrNull { it.name == n } }
    }
)

@Composable
fun CabinetScreen(navController: NavController) {
    // Wide windows (tablet / landscape) spread the grid into more columns.
    val wide = windowWidthSizeClass().isWide
    // Compact hero on tablets/landscape — 140dp instead of 180dp.
    val compactBannerHeight = if (wide) 140.dp else CabinetHeroBannerHeight
    val contentTop = compactBannerHeight + CabinetHeroSheetExtent + CabinetChipBarHeight + 12.dp
    var selectedFilter by rememberSaveable(CabinetSessionToken, stateSaver = CategoryIdSaver) {
        mutableStateOf<CategoryId?>(null)
    }
    var showLegacyOnly by rememberSaveable(CabinetSessionToken) { mutableStateOf(false) }
    // Saveable-backed scroll state — the grid keeps its position on rotation.
    val gridState = rememberLazyGridState()

    // Search + sort — the search button expands into a real filter bar
    // (matches by topic name or custom title, case-insensitive), and the
    // sort button toggles newest-first / oldest-first by capture time.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortNewestFirst by rememberSaveable { mutableStateOf(true) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedEntryIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    val deleteScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            searchFocus.requestFocus()
        }
    }

    // v7.107 — promo/demo-content mode fills the Cabinet with the sample
    // entries (real topics, all six capture formats) so screenshots look
    // rich; they stay fully tappable (EntryDetail resolves `sample-*` ids
    // via TopicCatalog.sampleEntries). Real data returns the instant the
    // mode is toggled off (keyed on the reactive promo state).
    val promoOn = AppPreferences.promoModeState
    val entries by produceState<List<CurioEntry>>(initialValue = emptyList(), promoOn) {
        if (promoOn) {
            value = runCatching { TopicCatalog.sampleEntries() }.getOrDefault(emptyList())
        } else {
            try {
                CurioRepositoryHolder.repo.observeAll().collect { value = it }
            } catch (_: Exception) {
                value = emptyList()
            }
        }
    }

    val visibleEntries = remember(entries, selectedFilter, showLegacyOnly, searchQuery, sortNewestFirst) {
        val q = searchQuery.trim()
        var result = if (selectedFilter == null) entries
            else entries.filter { it.topic.categoryId == selectedFilter }
        // Legacy captures live in their own Cabinet section. The normal
        // Cabinet never mixes restored FieldMind records with native Curio
        // captures; selecting Legacy is the explicit opt-in view.
        result = if (showLegacyOnly) result.filter { it.isLegacy }
                 else result.filterNot { it.isLegacy }
        if (q.isNotEmpty()) {
            result = result.filter {
                it.topic.name.contains(q, ignoreCase = true) ||
                    it.title?.contains(q, ignoreCase = true) == true ||
                    // v7.17 — custom tags are searchable too.
                    it.tags.any { tag -> tag.contains(q, ignoreCase = true) }
            }
        }
        if (sortNewestFirst) result.sortedByDescending { it.capturedAtMillis }
        else result.sortedBy { it.capturedAtMillis }
    }

    val categorySelectionIds = visibleEntries.map { it.id }.toSet()
    LaunchedEffect(selectedFilter, showLegacyOnly, searchQuery) {
        selectedEntryIds = selectedEntryIds.intersect(categorySelectionIds)
        if (selectedEntryIds.isEmpty()) selectionMode = false
    }
    val allVisibleSelected = categorySelectionIds.isNotEmpty() &&
        categorySelectionIds.all { it in selectedEntryIds }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete selected captures?", fontWeight = FontWeight.Bold) },
            text = { Text("This permanently deletes ${selectedEntryIds.size} selected capture(s), including their attached media.") },
            confirmButton = {
                TextButton(onClick = {
                    showBulkDeleteConfirm = false
                    val ids = selectedEntryIds.toList()
                    deleteScope.launch {
                        val selectedEntries = entries.filter { it.id in ids }
                        val deleted = runCatching {
                            CurioRepositoryHolder.repo.deleteByIds(ids)
                        }.isSuccess
                        if (deleted) {
                            selectedEntries.forEach { entry ->
                                entry.captureData.audioFilePaths().forEach { path ->
                                    AudioStorageManager.deleteAudio(context, path)
                                }
                                ImageStorageManager.deleteImagesForEntry(context, entry.id)
                            }
                            selectedEntryIds = emptySet()
                            selectionMode = false
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // The Cabinet wears the active filter's category wash — the same tinted
    // background as the filters page — ONLY while a category filter is
    // active. The "All" page stays on the plain theme background (like Home),
    // and the search button keeps its neutral look in every state.
    val filterCat = selectedFilter?.let { CurioCategories.byId(it) }
    // Publish the active filter's wash so the Scaffold-level bottom bar can
    // blend with the tinted Cabinet page (mirrors Spin's CurioNavTint
    // handoff — the bar lives outside the NavHost and can't read this
    // screen's state directly). Null on "All" so the bar stays plain.
    val cabinetWash = filterCat?.categoryBackgroundWash()
    LaunchedEffect(cabinetWash) {
        CurioNavTint.publishCabinetWash(cabinetWash)
    }
    // Hygiene: clear the handoff when the Cabinet leaves composition so a
    // stale wash never lingers for another tab.
    DisposableEffect(Unit) {
        onDispose { CurioNavTint.publishCabinetWash(null) }
    }

    // The hero banner runs up BEHIND the status bar (it applies its own
    // status-bar inset), so the root Box carries no status-bar padding.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(filterCat?.categoryBackgroundWash() ?: MaterialTheme.colorScheme.background)
    ) {
        // Muted category-glyph watermark behind the grid — the same
        // backdrop language as Home / Spin / the saved-entry page, so the
        // Cabinet reads as part of the app's paper-and-glyph world. The
        // backdrop is deliberately STATIC (always the wildcard scatter): if
        // the emphasis followed the active filter, the highlighted glyph
        // would jump to a different position on every page switch — the
        // "shifting watermark". Fixed, so switching All / categories /
        // Legacy never moves a glyph; the active category is already
        // carried by the page wash, the chip row and the card tints.
        // v7.77 — the flat grid sits directly on this backdrop, so the
        // glyphs stay a faint whisper and the cards always read first.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                modifier = Modifier.fillMaxSize(),
                alphaScale = 0.45f
            )
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // ── Grid or empty state — the scroll content fills the screen and
        // runs UNDER the torn hero banner and the sticky chip bar (both are
        // drawn on top in this root Box), so cards disappear under the
        // ragged tear and the pinned chips as they scroll — the settings
        // overlay pattern.
        if (visibleEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentTop)
            ) {
            MorphEntrance {
                if (searchActive && searchQuery.isNotBlank()) {
                    // Live search came up empty — tell the user what didn't
                    // match (and that the keyboard is still up, ready to edit).
                    CurioEmptyState(
                        glyph = CurioIcons.SearchOff,
                        headline = "No captures match",
                        subtext = "Nothing in the Cabinet matches \"${searchQuery.trim()}\". Try a different name.",
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                        ctaLabel = "Clear search",
                        onCtaClick = {
                            searchQuery = ""
                            searchActive = false
                        }
                    )
                } else if (showLegacyOnly) {
                    CurioEmptyState(
                        glyph = CurioIcons.History,
                        headline = "No legacy captures yet",
                        subtext = "Restore a FieldMind archive from Settings to keep old observations separate from Curio.",
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                        ctaLabel = "Open settings",
                        onCtaClick = { navController.navigate(CurioRoutes.SETTINGS) { launchSingleTop = true } }
                    )
                } else if (selectedFilter == null && !showLegacyOnly) {
                    CurioEmptyState(
                        glyph = CurioIcons.Inventory2,
                        headline = "Your Cabinet is empty",
                        subtext = "Every capture you save will live here. Shuffle to find your first one.",
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                        ctaLabel = "Discover something",
                        onCtaClick = {
                            // Tab switch (not a plain push): Cabinet is itself
                            // a tab, so pushing spin on top of it would leave a
                            // hybrid back stack — back would walk into Cabinet
                            // and tab switches would pile up duplicates. Anchor
                            // to HOME like every other Spin launch in the app.
                            navController.navigateToTab(CurioRoutes.SPIN)
                        }
                    )
                } else {
                    val filterId = selectedFilter ?: CategoryId.WILDCARD
                    val cat = CurioCategories.byId(filterId)
                    CurioEmptyState(
                        glyph = CurioIcons.SearchOff,
                        headline = "No ${cat.displayName} captures yet",
                        subtext = "Shuffle for ${cat.displayName} to find your first one.",
                        tint = cat.categoryInk().copy(alpha = 0.4f),
                        ctaLabel = "Shuffle for ${cat.displayName}",
                        onCtaClick = {
                            // Same tab-switch contract as the "All" empty state
                            // (and Home's quest cards): anchor to HOME so the
                            // Shuffle tab replaces Cabinet instead of stacking
                            // a spin/… entry on top of the Cabinet tab entry.
                            navController.navigateToTab(
                                CurioRoutes.spinWithCategory(cat.id.routeSlug)
                            )
                        }
                    )
                }
            }
            }
        } else {
            // v8.18 — the Cabinet grid is a CURIOUS landmark: the pet
            // sometimes tiptoes over and peeks at your saved keepsakes
            // (the whole shelf springs a beat — bounds only, no layout
            // change, and the sticky chips/hero stay put above it).
            PetLandmark(
                id = "grid",
                kind = PetLandmarks.Kind.CURIOUS,
                screen = "cabinet"
            ) { m ->
                LazyVerticalGrid(
                    state = gridState,
                    // Phones keep the 2-column grid; wide windows gain columns
                    // automatically (3 across on the ~720dp content column).
                    columns = if (wide) GridCells.Adaptive(minSize = 176.dp) else GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = contentTop,
                        bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = m.fillMaxSize()
                ) {
                    items(visibleEntries, key = { it.id }) { entry ->
                        // ── Cabinet→Detail morph: match this card to the
                        //    entry detail hero via shared element. The modifier
                        //    only attaches when NOT in selection mode (otherwise
                        //    a multi-select card would become a morph source).
                        val sharedScope = LocalRevealSharedScope.current
                        val visScope = LocalRevealVisibilityScope.current
                        val cardMorphMod = if (!selectionMode && sharedScope != null && visScope != null) {
                            val state = sharedScope.rememberSharedContentState("cabinet-${entry.id}")
                            sharedScope.run {
                                Modifier.sharedElement(state, visScope, boundsTransform = RevealBoundsTransform)
                            }
                        } else Modifier

                        CurioEntryCard(
                            entry = entry,
                            modifier = cardMorphMod,
                            selected = entry.id in selectedEntryIds,
                            onLongClick = {
                                // v7.107 — promo/demo mode disables multi-select:
                                // bulk delete would no-op on the sample entries.
                                if (!promoOn) {
                                    selectionMode = true
                                    selectedEntryIds = selectedEntryIds + entry.id
                                }
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
        }
        }

        // ── Sticky filter chip bar — drawn ON TOP of the scroll content.
        // As the grid scrolls the bar lifts, pops (0.97 → 1.0) and frosts in
        // (Profile's pill mechanism), pinning just below the ragged tear
        // while the entry cards pass underneath it.
        CabinetStickyChipBar(
            gridState = gridState,
            entries = entries,
            selectedFilter = selectedFilter,
            showLegacyOnly = showLegacyOnly,
            onSelectAll = { selectedFilter = null; showLegacyOnly = false },
            onSelectCategory = { selectedFilter = it; showLegacyOnly = false },
            onToggleLegacy = { selectedFilter = null; showLegacyOnly = !showLegacyOnly }
        )

        // ── Torn rose hero banner — drawn ON TOP of the scroll content; the
        // search field expands INSIDE the banner when search is active. The
        // title + subtitle sit pinned just above the tear and the
        // search/sort/select pills ride the banner's top row as ink-glass
        // pills (replaced by a Cancel pill while searching).
        val cabinetTitle = when {
            selectionMode -> "${selectedEntryIds.size} selected"
            showLegacyOnly -> "Legacy Cabinet"
            else -> "The Cabinet"
        }
        val cabinetSubtitle = when {
            selectionMode -> "Long-press cards to select · ${if (showLegacyOnly) "legacy" else "current filter"}"
            showLegacyOnly -> "Restored FieldMind records"
            else -> selectedFilter?.let { "Showing ${CurioCategories.byId(it).displayName}" } ?: "Your saved captures"
        }
        CabinetHeroHeader(
            title = cabinetTitle,
            subtitle = cabinetSubtitle,
            activeCat = filterCat,
            legacyMode = showLegacyOnly,
            backVisible = selectedFilter != null || showLegacyOnly,
            onBack = { selectedFilter = null; showLegacyOnly = false },
            searchActive = searchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onCloseSearch = { searchActive = false; searchQuery = "" },
            searchFocus = searchFocus,
            compact = wide,
            // Passed as a NAMED argument (not trailing-lambda syntax): the
            // @Composable slot isn't the last parameter, and the trailing
            // form fails to bind it under K2 ("no value passed for
            // 'trailing'" / "too many arguments").
            trailing = { ink ->
                if (selectionMode) {
                    CabinetHeroActionPill(
                        onClick = {
                            selectedEntryIds = if (allVisibleSelected) {
                                selectedEntryIds - categorySelectionIds
                            } else {
                                selectedEntryIds + categorySelectionIds
                            }
                        },
                        label = if (allVisibleSelected) "Clear" else "Select all",
                        ink = ink,
                        emphasized = true
                    )
                    CabinetHeroActionPill(
                        onClick = {
                            if (selectedEntryIds.isNotEmpty()) showBulkDeleteConfirm = true
                        },
                        label = "Delete (${selectedEntryIds.size})",
                        ink = ink,
                        emphasized = true,
                        destructive = true
                    )
                    CabinetHeroActionPill(
                        onClick = { selectionMode = false; selectedEntryIds = emptySet() },
                        glyph = CurioIcons.Close,
                        contentDescription = "Cancel selection",
                        ink = ink
                    )
                } else {
                    CabinetHeroActionPill(
                        onClick = {
                            selectionMode = true
                            selectedEntryIds = emptySet()
                        },
                        label = "Select",
                        ink = ink
                    )
                    CabinetHeroActionPill(
                        onClick = { sortNewestFirst = !sortNewestFirst },
                        glyph = if (sortNewestFirst) CurioIcons.ArrowDownward else CurioIcons.ArrowUpward,
                        contentDescription = if (sortNewestFirst) "Newest first. Tap for oldest" else "Oldest first. Tap for newest",
                        ink = ink,
                        emphasized = sortNewestFirst
                    )
                    CabinetHeroActionPill(
                        onClick = { searchActive = true },
                        glyph = CurioIcons.Search,
                        contentDescription = "Search captures",
                        ink = ink
                    )
                }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════════════════════════
// Torn rose hero banner — the Profile/Settings hero-card language, with
// the Cabinet's own fixed tear seed. Title + subtitle pinned just above
// the tear; the top row carries the back pill (when a filter/legacy view
// is active) and the search/sort/select action pills as ink-glass pills.
// ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════

/** The hero banner's solid body height — compact, like the settings hero. */
private val CabinetHeroBannerHeight = 180.dp
/** Extra layout space reserved for the under-sheet below the torn banner. */
private val CabinetHeroSheetExtent = 24.dp
/** Total header footprint — the torn banner plus its under-sheet extent. */
private val CabinetHeroTotalHeight = CabinetHeroBannerHeight + CabinetHeroSheetExtent
/** Fixed tear seed — the Cabinet tears in its own bold pattern, never re-rolls. */
private const val CABINET_TEAR_SEED = 0xCAB1E

// ── Sticky filter chip bar ──────────────────────────────────────────────
// The chip row is a scroll-reactive overlay (like Profile's pinned pills):
// it rests below the hero, then lifts, pops (0.97 → 1.0) and frosts in as
// the grid scrolls, pinning just below the ragged tear while the entry
// cards pass underneath it.
/** Where the chip bar rests below the hero (its unpinned spot). */
private val CabinetChipBarRestTop = CabinetHeroTotalHeight + 4.dp
/** Where the chip bar pins when scrolled — just below the ragged tear. */
private val CabinetChipBarPinnedTop = CabinetHeroTotalHeight + 2.dp
/** Scroll distance (dp) before the chip bar fully pins (Profile pill style). */
private val CabinetChipStickyThreshold = 56.dp
/** The chip bar's layout height — scroll content starts below it. */
private val CabinetChipBarHeight = 52.dp
/** Top content padding — hero + chip bar + breathing room. */
private val CabinetContentTop = CabinetHeroTotalHeight + CabinetChipBarHeight + 12.dp

/** One mirrored hero watermark pair (the settings/profile collage). */
private data class CabinetHeroPair(
    val biasX: Float,
    val biasY: Float,
    val size: Dp,
    val rotation: Float,
    val alpha: Float
)

/**
 * The Cabinet's torn hero banner — the shared Profile/Settings construction:
 * a solid banner with the same bold SoftTorn tear and a theme-matched
 * under-sheet, the mirrored watermark collage, the back pill (when a
 * filter/legacy view is active) and the caller-provided action pills riding
 * the top row, and the title + subtitle pinned just above the tear. Runs up
 * behind the status bar; the under-sheet is always the shared white paper
 * layer so it stays legible in every theme and filter.
 *
 * v7.96 — the banner MATCHES the active category: a filtered view wears the
 * category's own accent (and its family's watermark scatter), the Legacy
 * view wears the tertiary accent, and All stays on the shared rose. The
 * fill and ink MORPH smoothly when the filter changes.
 */
@Composable
private fun CabinetHeroHeader(
    title: String,
    subtitle: String,
    activeCat: CurioCategory?,
    legacyMode: Boolean,
    backVisible: Boolean,
    onBack: () -> Unit,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    searchFocus: FocusRequester,
    trailing: @Composable (ink: Color) -> Unit,
    // Narrow the torn banner on landscape/tablet so it doesn't cover
    // most of the already-short vertical space.
    compact: Boolean = false
) {
    val bannerHeight = if (compact) 140.dp else CabinetHeroBannerHeight
    val totalHeight = bannerHeight + CabinetHeroSheetExtent
    val heroTornShape = remember(CABINET_TEAR_SEED) { SoftTornBottomShape(CABINET_TEAR_SEED, bold = true) }
    val sheetShape = remember(CABINET_TEAR_SEED) {
        SoftTornSheetShape(CABINET_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
    }
    // v7.96 — category-matched hero: the fill/ink resolve from the active
    // filter (category accent + onAccent ink, tertiary for Legacy, rose for
    // All) and ANIMATE between states so the banner visibly morphs into the
    // category's color instead of snapping.
    val targetFill = when {
        legacyMode -> MaterialTheme.colorScheme.tertiary
        activeCat != null -> activeCat.themedAccent()
        else -> settingsRoseAccent()
    }
    val targetInk = when {
        legacyMode -> MaterialTheme.colorScheme.onTertiary
        activeCat != null -> activeCat.onAccent()
        else -> settingsReadableInk(targetFill)
    }
    val fill by animateColorAsState(targetFill, tween(CurioMotion.Durations.Morph), label = "cabinetHeroFill")
    val ink by animateColorAsState(targetInk, tween(CurioMotion.Durations.Morph), label = "cabinetHeroInk")
    val heroSymbols = CurioIcons.heroWatermarkSymbols(activeCat?.family ?: CategoryFamily.WILDCARD)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        // ── Under-sheet — a shared white paper layer, so the tear remains
        // visible instead of turning into a dark/black strip in dark mode.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .offset(y = bannerHeight - 18.dp)
                .clip(sheetShape)
                .background(CurioColors.CreamWhite)
        )
        // ── Torn-edge shadow — hairline dark rim under the seam.
        Box(
            modifier = Modifier
                .fillMaxWidth()                .height(bannerHeight)
                .offset(y = 1.dp)
                .clip(heroTornShape)
                .background(Color.Black.copy(alpha = 0.20f))
            )
            // ── Solid rose banner, torn bottom edge — shares the exact rose
        // family as Profile/Settings (settingsRoseAccent).
        Surface(
            shape = heroTornShape,
            color = fill,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mirrored watermark collage — the ACTIVE category's family
                // symbols pop around the banner edges (the settings/profile
                // collage), so a Movies view scatters film glyphs, etc.
                val symbols = heroSymbols
                val pairs = listOf(
                    CabinetHeroPair(biasX = 0.93f, biasY = -0.85f, size = 44.dp, rotation = 12f, alpha = 0.11f),
                    CabinetHeroPair(biasX = 0.55f, biasY = -0.64f, size = 48.dp, rotation = 8f, alpha = 0.13f),
                    CabinetHeroPair(biasX = 0.94f, biasY = -0.12f, size = 56.dp, rotation = 14f, alpha = 0.14f),
                    CabinetHeroPair(biasX = 0.56f, biasY = 0.54f, size = 50.dp, rotation = 10f, alpha = 0.13f),
                    CabinetHeroPair(biasX = 0.94f, biasY = 0.80f, size = 44.dp, rotation = 6f, alpha = 0.11f)
                )
                pairs.forEachIndexed { i, pair ->
                    CabinetHeroSymbol(symbols[i * 2], BiasAlignment(-pair.biasX, pair.biasY), pair.size, -pair.rotation, pair.alpha, ink)
                    CabinetHeroSymbol(symbols[i * 2 + 1], BiasAlignment(pair.biasX, pair.biasY), pair.size, pair.rotation, pair.alpha, ink)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)
                ) {
                    // ── Top row — back pill (when needed) + action pills ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (backVisible) {
                            CurioBackButton(
                                onClick = onBack,
                                containerColor = ink.copy(alpha = 0.18f),
                                contentColor = ink,
                                disableRipple = true
                            )
                        } else {
                            // Balance the row when there's no back pill.
                            Spacer(Modifier.size(42.dp))
                        }
                        if (searchActive) {
                            // Search is open — the top row holds just the
                            // Cancel pill (the action pills are hidden).
                            CabinetHeroActionPill(
                                onClick = onCloseSearch,
                                label = "Cancel",
                                glyph = CurioIcons.Close,
                                contentDescription = "Close search",
                                ink = ink
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                trailing(ink)
                            }
                        }
                    }
                    // Flex spacer — pins the title block just above the tear.
                    Spacer(Modifier.weight(1f))
                    // ── Search field or title, animated expand/collapse ──
                    //    The search bar scales in from the pill's position
                    //    when opened, and the title fades back in when closed.
                    AnimatedContent(
                        targetState = searchActive,
                        transitionSpec = {
                            if (targetState) {
                                // Search opening: scale in + fade in
                                (scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.92f)
                                    + fadeIn(tween(280, easing = FastOutSlowInEasing)))
                                    .togetherWith(fadeOut(tween(200)))
                            } else {
                                // Search closing: title fades back in
                                (fadeIn(tween(280, easing = FastOutSlowInEasing)))
                                    .togetherWith(
                                        scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.92f)
                                            + fadeOut(tween(200))
                                    )
                            }
                        },
                        label = "searchExpand"
                    ) { active ->
                        if (active) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text("Search captures…") },
                                leadingIcon = {
                                    CurioIcon(CurioIcons.Search, null, tint = ink, size = 20.dp)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            CurioIcon(
                                                CurioIcons.Close,
                                                "Clear search",
                                                tint = ink.copy(alpha = 0.85f),
                                                size = 20.dp
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {}),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ink.copy(alpha = 0.16f),
                                    unfocusedContainerColor = ink.copy(alpha = 0.16f),
                                    focusedBorderColor = ink.copy(alpha = 0.55f),
                                    unfocusedBorderColor = ink.copy(alpha = 0.30f),
                                    cursorColor = ink,
                                    focusedTextColor = ink,
                                    unfocusedTextColor = ink,
                                    focusedPlaceholderColor = ink.copy(alpha = 0.72f),
                                    unfocusedPlaceholderColor = ink.copy(alpha = 0.72f),
                                    focusedLeadingIconColor = ink,
                                    unfocusedLeadingIconColor = ink,
                                    focusedTrailingIconColor = ink.copy(alpha = 0.85f),
                                    unfocusedTrailingIconColor = ink.copy(alpha = 0.85f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocus)
                            )
                        } else {
                            Column {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ink,
                                    maxLines = 1
                                )
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ink.copy(alpha = 0.82f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The Cabinet's filter chip row, drawn ON TOP of the scroll content.
 *
 * Scroll-reactive, like Profile's pinned pills: as the grid scrolls, the
 * row lifts a few dp to pin just below the hero's ragged tear, and every
 * pill pops on its own — scale 0.90 → 1.0, staggered left→right, with a
 * COLOR MORPH: each pill's surface blooms toward its category accent and
 * it lifts with a soft shadow as it pops (v7.96) — no card background
 * behind the chips. The entry cards scroll underneath the row.
 */
@Composable
private fun BoxScope.CabinetStickyChipBar(
    gridState: LazyGridState,
    entries: List<CurioEntry>,
    selectedFilter: CategoryId?,
    showLegacyOnly: Boolean,
    onSelectAll: () -> Unit,
    onSelectCategory: (CategoryId) -> Unit,
    onToggleLegacy: () -> Unit
) {
    // Scroll-reactive lift — the chips pop + frost as the first card row
    // approaches them and pin once it reaches the bar, so the lift is tied
    // to the cards actually arriving (not raw scroll offset, which would
    // include the grid's large top content padding). Progress reads the
    // first visible card row's top edge inside the viewport: it starts at
    // the content top (~274dp) and falls as the user scrolls.
    val thresholdPx = with(LocalDensity.current) { CabinetChipStickyThreshold.toPx() }
    val barBottomPx = with(LocalDensity.current) { (CabinetChipBarRestTop + CabinetChipBarHeight).toPx() }
    val progress by remember {
        derivedStateOf {
            val first = gridState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (first == null) 0f
            else ((barBottomPx - first.offset.y) / thresholdPx).coerceIn(0f, 1f)
        }
    }
    val frostShift = FastOutSlowInEasing.transform(progress)
    val liftPx = with(LocalDensity.current) { (CabinetChipBarRestTop - CabinetChipBarPinnedTop).toPx() }

    // No frosted card behind the chips — each pill pops on its own (v7.89:
    // per-pill pop-up animation as the bar lifts to pin just below the tear).
    val hasLegacyEntries = entries.any { it.isLegacy }
    // Vertical padding keeps the row at the tuned bar height (the old
    // frosted container supplied it) so the pin/progress constants hold.
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .offset(y = CabinetChipBarRestTop)
            .graphicsLayer {
                translationY = -liftPx * frostShift
            }
    ) {
        item("all") {
            CabinetChipPop(
                index = 0,
                frostShift = frostShift,
                restSurface = MaterialTheme.colorScheme.surfaceVariant,
                popSurface = MaterialTheme.colorScheme.primaryContainer
            ) { popProgress, surface, elevation ->
                FilterChipLite(
                    label = "All",
                    accent = MaterialTheme.colorScheme.primary,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    ink = MaterialTheme.colorScheme.onPrimaryContainer,
                    // Opaque unselected pill — the chip reads as a solid
                    // surface over the backdrop, not a see-through wash.
                    chipSurface = surface,
                    chipBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    popProgress = popProgress,
                    elevation = elevation,
                    selected = selectedFilter == null && !showLegacyOnly,
                    onClick = onSelectAll
                )
            }
        }
        itemsIndexed(CurioCategories.visible) { i, cat ->
            val restSurface = cat.categoryChipSurface(MaterialTheme.colorScheme.surfaceVariant)
            CabinetChipPop(
                index = i + 1,
                frostShift = frostShift,
                restSurface = restSurface,
                // The color bloom — the neutral pill morphs toward its
                // accent as it pops (a 30% pull keeps the capsule tasteful
                // at full pop).
                popSurface = lerp(restSurface, cat.themedAccent(), 0.30f)
            ) { popProgress, surface, elevation ->
                FilterChipLite(
                    label = cat.displayName,
                    accent = cat.categoryInk(),
                    tint = cat.tint,
                    // The button (label text) never adapts to the category —
                    // it stays on the neutral theme ink in every state, so
                    // only the background carries the tint.
                    ink = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Opaque category pill — full-strength chip surface so
                    // the tinted pill reads solid on the backdrop.
                    chipSurface = surface,
                    chipBorder = cat.categoryBorder(
                        fallback = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ),
                    popProgress = popProgress,
                    elevation = elevation,
                    selected = selectedFilter == cat.id && !showLegacyOnly,
                    onClick = { onSelectCategory(cat.id) }
                )
            }
        }
        // Legacy sits LAST, after every native category — and only when
        // there's something to show (or the legacy view is currently
        // open, so the active chip stays visible/deselectable).
        if (hasLegacyEntries || showLegacyOnly) {
            item("legacy") {
                CabinetChipPop(
                    index = CurioCategories.visible.size + 1,
                    frostShift = frostShift,
                    restSurface = MaterialTheme.colorScheme.surfaceVariant,
                    popSurface = lerp(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.tertiary,
                        0.30f
                    )
                ) { popProgress, surface, elevation ->
                    FilterChipLite(
                        label = "Legacy",
                        accent = MaterialTheme.colorScheme.tertiary,
                        tint = MaterialTheme.colorScheme.tertiaryContainer,
                        ink = MaterialTheme.colorScheme.onTertiaryContainer,
                        chipSurface = surface,
                        chipBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        popProgress = popProgress,
                        elevation = elevation,
                        selected = showLegacyOnly,
                        onClick = onToggleLegacy
                    )
                }
            }
        }
    }
}

/** Per-pill pop — each chip scales 0.90 → 1.0 AND morphs its surface from
 *  [restSurface] toward [popSurface] as the bar lifts, staggered per pill so
 *  the row ripples left→right with its own color bloom instead of scaling
 *  as one block. No frosted card behind the row (v7.89). */
@Composable
private fun CabinetChipPop(
    index: Int,
    frostShift: Float,
    restSurface: Color,
    popSurface: Color,
    content: @Composable (popProgress: Float, surface: Color, elevation: Dp) -> Unit
) {
    // Each pill starts its pop a beat after its left neighbor, so the row
    // reads as per-pill motion while the whole bar pins. Normalized so the
    // last pill still reaches full pop at full scroll.
    val stagger = (index * 0.07f).coerceAtMost(0.85f)
    val pillProgress = ((frostShift - stagger) / (1f - stagger)).coerceIn(0f, 1f)
    // v7.x — ease the per-pill pop on the SAME curve as the bar's lift
    // (FastOutSlowIn): the old linear progress made each pill's scale/color
    // track the scroll 1:1 while the bar eased, which read as a slightly
    // mechanical, janky pop. Easing it settles every pill in sync with the
    // bar's glide.
    val eased = FastOutSlowInEasing.transform(pillProgress)
    val pillScale = androidx.compose.ui.util.lerp(0.90f, 1f, eased)
    // v7.96 — COLOR MORPH: as each pill pops, its neutral surface blooms
    // toward its accent [popSurface] and it lifts with a soft shadow — every
    // chip ripples with its own color as the bar pins, instead of scaling
    // alone.
    val morphedSurface = lerp(restSurface, popSurface, eased)
    val popElevation = 6.dp * eased
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = pillScale
            scaleY = pillScale
        }
    ) {
        content(eased, morphedSurface, popElevation)
    }
}

/** One mirrored watermark glyph on the Cabinet hero (settings/profile style). */
@Composable
private fun BoxScope.CabinetHeroSymbol(
    glyph: String,
    alignment: Alignment,
    size: Dp,
    rotation: Float,
    alpha: Float,
    tint: Color
) {
    CurioIcon(
        name = glyph,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        size = size,
        modifier = Modifier
            .align(alignment)
            .padding(10.dp)
            .graphicsLayer { rotationZ = rotation }
    )
}

/** One ink-glass action pill on the Cabinet hero — the banner's readable
 *  ink at a soft alpha (the Profile edit-pill language), so the Select /
 *  Sort / Search / selection buttons read on the rose in every theme.
 *  [emphasized] deepens the fill for the active/primary state;
 *  [destructive] deepens it further for the delete action. */
@Composable
private fun CabinetHeroActionPill(
    onClick: () -> Unit,
    ink: Color,
    label: String? = null,
    glyph: String? = null,
    contentDescription: String? = null,
    emphasized: Boolean = false,
    destructive: Boolean = false
) {
    val fill = when {
        destructive -> ink.copy(alpha = 0.55f)
        emphasized -> ink.copy(alpha = 0.42f)
        else -> ink.copy(alpha = 0.18f)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = fill,
        border = BorderStroke(1.dp, ink.copy(alpha = 0.28f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (glyph != null) {
                CurioIcon(
                    name = glyph,
                    contentDescription = contentDescription,
                    tint = ink,
                    size = 18.dp
                )
            }
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ink
                )
            }
        }
    }
}

@Composable
private fun FilterChipLite(
    label: String,
    accent: Color,
    tint: Color,
    ink: Color,
    chipSurface: Color = MaterialTheme.colorScheme.surfaceVariant,
    chipBorder: BorderStroke? = null,
    // v7.96 — premium pop: the capsule wears a soft vertical sheen (top
    // light / slightly deeper base) instead of a flat fill; as [popProgress]
    // goes 0→1 the unselected label blooms toward [accent] and the pill
    // lifts with [elevation]'s shadow — the per-pill color pop on top of
    // the scale pop. Selected chips keep their accent-container gradient.
    popProgress: Float = 0f,
    elevation: Dp = 0.dp,
    selected: Boolean,
    onClick: () -> Unit
) {
    val labelColor = if (selected) {
        ink
    } else {
        lerp(
            MaterialTheme.colorScheme.onSurfaceVariant,
            accent,
            popProgress * 0.55f
        )
    }
    val fillBrush = if (selected) {
        // Accent-container gradient — deeper at the base like the category
        // card fills, so the active pill reads premium rather than flat.
        Brush.verticalGradient(listOf(tint, lerp(tint, Color.Black, 0.10f)))
    } else {
        // Neutral capsule with a whisper of top light (the rigid-card sheen).
        Brush.verticalGradient(
            listOf(lerp(chipSurface, Color.White, 0.06f), chipSurface)
        )
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        shadowElevation = if (selected) elevation.coerceAtLeast(3.dp) else elevation
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(fillBrush)
                .then(
                    if (selected || chipBorder == null) {
                        Modifier
                    } else {
                        Modifier.border(chipBorder, RoundedCornerShape(50))
                    }
                )
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = labelColor,
                maxLines = 1
            )
        }
    }
}

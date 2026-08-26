package com.curio.app.features.cabinet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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

import com.curio.app.features.settings.settingsReadableInk
import com.curio.app.features.settings.heroLaneCategory
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.PendingCabinetFilter
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioEmptyState
import com.curio.app.ui.components.CurioNavTint
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.curioSearchFill
import com.curio.app.ui.components.CurioTwoStepDeleteDialog
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.isLiquidGlassPillsActive
import com.curio.app.ui.components.liquidGlassCapsule
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.curioGlassGlow
import com.curio.app.ui.components.CurioEntryCard
import com.curio.app.ui.components.MorphEntrance
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.components.PaperTitleLines
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryChipSurface
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.curioPillTintLift
import com.curio.app.ui.theme.headerAccent
import com.curio.app.ui.theme.heroHeaderInk
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
    // Compact hero on tablets/landscape — 192dp instead of 232dp.
    val compactBannerHeight = if (wide) CabinetHeroBannerHeightCompact else CabinetHeroBannerHeight
    var selectedFilter by rememberSaveable(CabinetSessionToken, stateSaver = CategoryIdSaver) {
        mutableStateOf<CategoryId?>(null)
    }
    var showLegacyOnly by rememberSaveable(CabinetSessionToken) { mutableStateOf(false) }
    // Saveable-backed scroll state — the grid keeps its position on rotation.
    val gridState = rememberLazyGridState()
    // v245 — LOCAL GLASS CAPTURE for the floating category chip bar (the
    // scrolling grid records; the chips are a sibling overlay).
    val chipGlassBackdrop = rememberLayerBackdrop()

    // Search + sort — the search button expands into a real filter bar
    // (matches by topic name or custom title, case-insensitive), and the
    // sort button toggles newest-first / oldest-first by capture time.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // v39 — opening the Cabinet from a lane tile (Profile → "Your lanes")
    // lands pre-filtered to that lane: the pending handoff is consumed once
    // on first composition (keyed on the monotonic bump so re-opens fire).
    // Placed AFTER searchActive/searchQuery so the effect can clear them.
    LaunchedEffect(PendingCabinetFilter.trigger) {
        PendingCabinetFilter.take()?.let { name ->
            runCatching { CategoryId.valueOf(name) }.getOrNull()?.let { catId ->
                selectedFilter = catId
                showLegacyOnly = false
                searchActive = false
                searchQuery = ""
            }
        }
    }
    // v30 — the category pill (second row under the hero pills) toggles the
    // sticky category chips; they also show while searching (same chips).
    var categoryFilterOpen by rememberSaveable { mutableStateOf(false) }
    // v30 — the chip-bar reservation only applies while the chips are
    // visible (search or the category pill); collapsed, content starts
    // right below the hero.
    val chipsVisible = searchActive || categoryFilterOpen
    // v31 — the hero keeps its original height. v42 — the Category pill
    // moved INSIDE the hero (beside the title), so content reserves only
    // the chip bar when the chips are open — no separate pill row below.
    val heroTotal = compactBannerHeight + CabinetHeroSheetExtent
    val contentTop = heroTotal +
        (if (chipsVisible) CabinetChipBarHeight else 0.dp) + 12.dp
    // v105 — the sort control is removed; the Cabinet keeps its default
    // ordering (newest captures first, see [visibleEntries]).
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedEntryIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    val deleteScope = rememberCoroutineScope()
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

    val visibleEntries = remember(entries, selectedFilter, showLegacyOnly, searchQuery) {
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
        // v105 — the sort control is removed; the Cabinet keeps its default
        // ordering: newest captures first.
        result.sortedByDescending { it.capturedAtMillis }
    }

    val categorySelectionIds = visibleEntries.map { it.id }.toSet()
    LaunchedEffect(selectedFilter, showLegacyOnly, searchQuery) {
        selectedEntryIds = selectedEntryIds.intersect(categorySelectionIds)
        if (selectedEntryIds.isEmpty()) selectionMode = false
    }
    val allVisibleSelected = categorySelectionIds.isNotEmpty() &&
        categorySelectionIds.all { it in selectedEntryIds }

    if (showBulkDeleteConfirm) {
        // v26 — double confirmation + recycle bin: nothing is permanently
        // deleted here (media is kept so restores work), so the old
        // "permanently deletes including media" wording is gone.
        CurioTwoStepDeleteDialog(
            visible = showBulkDeleteConfirm,
            title = if (selectedEntryIds.size == 1) "this capture"
            else "${selectedEntryIds.size} selected captures",
            body = "These ${selectedEntryIds.size} captures move to the Recycle bin.",
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirmed = {
                showBulkDeleteConfirm = false
                val ids = selectedEntryIds.toList()
                deleteScope.launch {
                    runCatching { CurioRepositoryHolder.repo.softDeleteByIds(ids) }
                    selectedEntryIds = emptySet()
                    selectionMode = false
                }
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
    // v223 — the wash now mirrors the page's ACTUAL background: the active
    // filter's wash when a category is selected, else the shared-hero
    // family's lane wash (Adaptive Hero) — the same fallback the page's own
    // .background() uses below. Publishing null on "All" left the floating
    // nav capsule painting the PLAIN theme background behind the
    // lane-washed page — a visible strip the entries scrolled behind (Home
    // never had it because it publishes its real homeBg).
    val cabinetWash = filterCat?.categoryBackgroundWash()
        ?: heroLaneCategory()?.categoryBackgroundWash()
    // v149 — the active filter's accent (resolved in composition —
    // themedAccent is @Composable and can't run inside the effect).
    val cabinetAccent = filterCat?.themedAccent()
    LaunchedEffect(cabinetWash, cabinetAccent) {
        CurioNavTint.publishCabinetWash(cabinetWash)
        // v149 — publish the active filter's accent so the floating nav
        // bar's ACTIVE pill wears it on Cabinet (null on "All" → secondary).
        CurioNavTint.publishCabinetAccent(cabinetAccent)
    }
    // The wash handoff is deliberately KEPT when the Cabinet leaves
    // composition (v8.36, mirroring Spin's publishSpinWash): while a
    // Cabinet→Detail morph runs, the NavHost's reserved bottom strip falls
    // back to this published wash for its first frame (the detail page's own
    // wash spacer registers a frame later). A stale wash is harmless — only
    // the Cabinet route reads it, and Cabinet republishes on every visit.

    // The hero banner runs up BEHIND the status bar (it applies its own
    // status-bar inset), so the root Box carries no status-bar padding.
    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — an active filter washes the page; the "All" page falls
            // back to the shared hero family's background (the Spin lane's
            // wash when "Hero follows Spin lane" is on, else plain).
            .background(filterCat?.categoryBackgroundWash()
                ?: (heroLaneCategory()?.categoryBackgroundWash() ?: MaterialTheme.colorScheme.background))
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
            modifier = Modifier
                .fillMaxSize()
            // v227 — NO reserved bottom band anymore: the grid now scrolls
            // FULL-BLEED to the screen edge and entries pass UNDER the
            // floating pill (the Home reference behavior). The old reserved
            // clearance clipped every card at a hard horizontal line at the
            // capsule's top — a visible "strip" the pill sat on. Clearance
            // moved into the grid's own contentPadding below, so only the
            // LAST row is lifted clear while scrolling runs under the pill.
        ) {
        // ── Grid or empty state — the scroll content fills the screen and
        // runs UNDER the torn hero banner and the sticky chip bar (both are
        // drawn on top in this root Box), so cards disappear under the
        // ragged tear and the pinned chips as they scroll — the settings
        // overlay pattern.
        if (visibleEntries.isEmpty()) {
            // v119 — the empty state registers the same "grid" landmark the
            // filled grid does, so the pet-led tour's Cabinet stop keeps its
            // anchor even with nothing saved yet (otherwise the guide bubble
            // floats over the tour dock's Next button).
            PetLandmark(
                id = "grid",
                kind = PetLandmarks.Kind.CURIOUS,
                screen = "cabinet"
            ) { m ->
            Box(
                modifier = m
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
                        // v227 — gesture-bar inset + the floating pill's
                        // 84dp slot + breathing room, so the LAST entry can
                        // scroll fully clear of the bar while every other
                        // card passes underneath it.
                        bottom = 24.dp + 84.dp +
                            WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    // v245 — the grid records into the chip bar's LOCAL glass
                    // capture while keeping its entrance-animated modifier.
                    // v291 — only capture when chips are visible: the backdrop
                    // re-records the full grid every frame, which is expensive
                    // when the chip bar is closed.
                    modifier = m
                        .fillMaxSize()
                        .then(if (chipsVisible && isLiquidGlassPillsActive())
                            Modifier.layerBackdrop(chipGlassBackdrop)
                        else Modifier)
                ) {
                    items(visibleEntries, key = { it.id }) { entry ->
                        // v8.38 — the Cabinet→Detail morph is gone: the detail
                        // page pops up from center instead of expanding out of
                        // the card, so the card carries no shared element.
                        CurioEntryCard(
                            entry = entry,
                            modifier = Modifier,
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

        // Side scroll indicator — thin overlay knob, grows on touch.
        CurioVerticalScrollIndicator(
            state = gridState.scrollIndicatorState,
            onScrollBy = { gridState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = contentTop + 8.dp, bottom = 16.dp)
        )

        // ── Sticky filter chip bar — drawn ON TOP of the scroll content.
        // As the grid scrolls the bar lifts, pops (0.97 → 1.0) and frosts in
        // (Profile's pill mechanism), pinning just below the ragged tear
        // while the entry cards pass underneath it. v30 — shown while
        // searching OR when the Category pill is open; v42 — the Category
        // pill moved INSIDE the hero, so the bar sits directly under the
        // banner again.
        // v52b — the category/search chip bar animates in/out instead of
        // popping instantly when the Category pill opens or search starts.
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
            CabinetStickyChipBar(
                glassBackdrop = if (isLiquidGlassPillsActive()) chipGlassBackdrop else null,
                gridState = gridState,
                barTop = heroTotal,
                entries = entries,
                selectedFilter = selectedFilter,
                showLegacyOnly = showLegacyOnly,
                onSelectAll = { selectedFilter = null; showLegacyOnly = false },
                onSelectCategory = { selectedFilter = it; showLegacyOnly = false },
                onToggleLegacy = { selectedFilter = null; showLegacyOnly = !showLegacyOnly }
            )
        }

        // ── Torn rose hero banner — drawn ON TOP of the scroll content; the
        // search field expands INSIDE the banner when search is active.
        // v36 — the action pills live back INSIDE the hero's top row (they
        // briefly rode a below-hero row in v33; the user wanted them back on
        // the banner): Sort + Search normally, Select-all/Clear + Delete +
        // Cancel while selecting. The back pill stays gone and the title
        // sits at the TOP of the banner.
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
            searchActive = searchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onCloseSearch = { searchActive = false; searchQuery = "" },
            searchFocus = searchFocus,
            compact = wide,
            // v42 — the Category pill lives INSIDE the hero beside the
            // title, directly under the sort/search pills; hidden while
            // selecting (the selection pills own the top row then).
            titleTrailing = if (selectionMode) null else { ink, backdrop ->
                val categoryLabel = when {
                    showLegacyOnly -> "Category · Legacy"
                    else -> "Category · ${selectedFilter?.let { CurioCategories.byId(it).displayName } ?: "All"}"
                }
                CabinetHeroActionPill(
                    onClick = { categoryFilterOpen = !categoryFilterOpen },
                    glyph = CurioIcons.Tune,
                    label = categoryLabel,
                    ink = ink,
                    backdrop = backdrop,
                    // v30 — chevron flips with the chips: ▾ closed, ▴ open.
                    trailingGlyph = if (categoryFilterOpen) CurioIcons.KeyboardArrowUp
                        else CurioIcons.KeyboardArrowDown,
                    trailingContentDescription = if (categoryFilterOpen) "Hide category chips"
                        else "Show category chips",
                    emphasized = categoryFilterOpen
                )
            },
            // Passed as a NAMED argument (not trailing-lambda syntax): the
            // @Composable slot isn't the last parameter, and the trailing
            // form fails to bind it under K2.
            trailing = { ink, backdrop ->
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
                        backdrop = backdrop,
                        emphasized = true
                    )
                    CabinetHeroActionPill(
                        onClick = {
                            if (selectedEntryIds.isNotEmpty()) showBulkDeleteConfirm = true
                        },
                        label = "Delete (${selectedEntryIds.size})",
                        ink = ink,
                        backdrop = backdrop,
                        emphasized = true,
                        destructive = true
                    )
                    CabinetHeroActionPill(
                        onClick = { selectionMode = false; selectedEntryIds = emptySet() },
                        glyph = CurioIcons.Close,
                        contentDescription = "Cancel selection",
                        ink = ink,
                        backdrop = backdrop
                    )
                } else {
                    // v105 — the sort dropdown is gone; the hero row keeps
                    // the Search pill only.
                    CabinetHeroActionPill(
                        onClick = { searchActive = true },
                        glyph = CurioIcons.Search,
                        contentDescription = "Search captures",
                        ink = ink,
                        backdrop = backdrop,
                        // v85 — emphasized hero fill (the hero action-pill
                        // language).
                        emphasized = true
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

/** The hero banner's solid body height — compact, like the settings hero.
 *  v31 — back to the original 180dp: the Category pill no longer lives
 *  inside the hero (it rides its own row below), so the banner no longer
 *  needs the v30 +52dp growth and the header text stays put. */
private val CabinetHeroBannerHeight = 180.dp
/** Banner height on wide windows (tablet/landscape). */
private val CabinetHeroBannerHeightCompact = 140.dp
/** Extra layout space reserved for the under-sheet below the torn banner. */
private val CabinetHeroSheetExtent = 24.dp
/** Fixed tear seed — the Cabinet tears in its own bold pattern, never re-rolls. */
private const val CABINET_TEAR_SEED = 0xCAB1E

// ── Sticky filter chip bar ──────────────────────────────────────────────
// The chip row is a scroll-reactive overlay (like Profile's pinned pills):
// it rests below the hero, then lifts, pops (0.97 → 1.0) and frosts in as
// the grid scrolls, pinning just below the ragged tear while the entry
// cards pass underneath it.
/** Scroll distance (dp) before the chip bar fully pins (Profile pill style). */
private val CabinetChipStickyThreshold = 56.dp
/** The chip bar's layout height — scroll content starts below it. */
private val CabinetChipBarHeight = 52.dp

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
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    searchFocus: FocusRequester,
    // v27n — the slot also receives the banner fill so hero pills can
    // resolve an OPAQUE glass fill (lerp of the ink into the banner).
    // v36 — the action pills (sort/search or selection pills) ride the
    // banner's top row again.
    trailing: @Composable (ink: Color, backdrop: Color) -> Unit,
    // v42 — the Category pill rides INSIDE the banner, beside the title,
    // directly under the sort/search pills (the below-hero row is gone).
    titleTrailing: (@Composable (ink: Color, backdrop: Color) -> Unit)? = null,
    // Narrow the torn banner on landscape/tablet so it doesn't cover
    // most of the already-short vertical space.
    compact: Boolean = false
) {
    val bannerHeight = if (compact) CabinetHeroBannerHeightCompact else CabinetHeroBannerHeight
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
        // v27j — header fill depth: a slightly darker painter accent by
        // default (toggle in Experiments → Paper & headers).
        activeCat != null -> activeCat.headerAccent()
        else -> settingsRoseAccent()
    }
    val targetInk = when {
        legacyMode -> MaterialTheme.colorScheme.onTertiary
        // v28 — dark mode: white/creamish hero text (the tinted light twin
        // would read as pastel title over the deep banner); light keeps the
        // pastel-aware on-accent ink exactly as before.
        activeCat != null -> activeCat.heroHeaderInk()
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
        // v81 — dark: a subtle lighter lip off the dark hero so the seam
        // still reads (never a bright white sliver on the black page).
        // v108 — OFF by default (Settings → Experiments → Paper & headers);
        // the toggle restores this extra paper layer.
        if (AppPreferences.heroTearSheetState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .offset(y = bannerHeight - 18.dp)
                .clip(sheetShape)
                .background(
                    if (isCurioDarkTheme()) lerp(fill, Color.White, 0.10f)
                    else CurioColors.CreamWhite
                )
        )
        }
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
                    // ── v36 — action pills row: Sort + Search normally,
                    //    Select-all/Clear + Delete + Cancel while selecting,
                    //    and a single Cancel pill while searching. The back
                    //    pill stays gone; the pills ride the banner's top
                    //    right as theme-aware ink-glass (matching the
                    //    restored hero-pill style).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (searchActive) {
                            CabinetHeroActionPill(
                                onClick = onCloseSearch,
                                label = "Cancel",
                                glyph = CurioIcons.Close,
                                contentDescription = "Close search",
                                ink = ink,
                                backdrop = fill
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                trailing(ink, fill)
                            }
                        }
                    }
                    // v36 — title block at the TOP of the banner, right
                    // under the pills (no flex spacer down to the tear).
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
                            // v90 — unified One UI search bar: banner ink +
                            // frosted category glass through the shared
                            // CurioSearchField (same 46dp height + hairline
                            // as every other search bar in the app).
                            // v100 — search-text audit: the banner ink (white
                            // in light) sat at ~3:1 on the whitened hero
                            // glass; the icon/text now use the THEME text
                            // color so they read crisp on the frosted glass.
                            CurioSearchField(
                                query = searchQuery,
                                onQueryChange = onSearchQueryChange,
                                placeholder = "Search captures…",
                                ink = MaterialTheme.colorScheme.onSurface,
                                // v108 — dark: the filter chips' near-black
                                // raised glass instead of the mid-tone lift.
                                fill = curioSearchFill(fill),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocus)
                            )
                        } else {
                            // v42 — title + subtitle on the left, the
                            // Category pill beside them (right-aligned), so
                            // the filter control lives INSIDE the hero under
                            // the sort/search pills.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ink,
                                        maxLines = 1
                                    )
                                    // v27 — experimental paper-title underline (two
                                    // short lines under the title text; OFF by default).
                                    if (AppPreferences.paperHeaderCutsState) {
                                        PaperTitleLines(
                                            ink = ink,
                                            title = title,
                                            fontSize = MaterialTheme.typography.headlineSmall.fontSize
                                        )
                                    }
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ink.copy(alpha = 0.82f),
                                        maxLines = 1
                                    )
                                }
                                if (titleTrailing != null) {
                                    titleTrailing(ink, fill)
                                }
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
    // v245 — when Liquid glass is on, chips render as clear refracting
    // capsules over this LOCAL page capture.
    glassBackdrop: LayerBackdrop? = null,
    gridState: LazyGridState,
    // v31 — the top of the hero + Category pill row (the chip bar now sits
    // BELOW the pill row, so its rest/pin offsets derive from this).
    barTop: Dp,
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
    val barBottomPx = with(LocalDensity.current) { (barTop + 4.dp + CabinetChipBarHeight).toPx() }
    val progress by remember {
        derivedStateOf {
            val first = gridState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (first == null) 0f
            else ((barBottomPx - first.offset.y) / thresholdPx).coerceIn(0f, 1f)
        }
    }
    val frostShift = FastOutSlowInEasing.transform(progress)
    // The bar pins 2dp above its rest spot (the rest/pin gap).
    val liftPx = with(LocalDensity.current) { 2.dp.toPx() }

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
            .offset(y = barTop + 4.dp)
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
                    ink = MaterialTheme.colorScheme.onPrimary,
                    // Opaque unselected pill — the chip reads as a solid
                    // surface over the backdrop, not a see-through wash.
                    chipSurface = surface,
                    popProgress = popProgress,
                    selected = selectedFilter == null && !showLegacyOnly,
                    onClick = onSelectAll,
                    glass = glassBackdrop != null,
                    glassBackdrop = glassBackdrop
                )
            }
        }
        itemsIndexed(CurioCategories.visible, key = { _, cat -> cat.id.name }) { i, cat ->
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
                    accent = cat.themedAccent(),
                    // v27q — the selected label flips to the on-accent ink so
                    // it stays readable on the solid accent fill.
                    ink = cat.onAccent(),
                    // Opaque category pill — full-strength chip surface so
                    // the tinted pill reads solid on the backdrop.
                    chipSurface = surface,
                    popProgress = popProgress,
                    selected = selectedFilter == cat.id && !showLegacyOnly,
                    onClick = { onSelectCategory(cat.id) },
                    glass = glassBackdrop != null,
                    glassBackdrop = glassBackdrop
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
                        ink = MaterialTheme.colorScheme.onTertiary,
                        chipSurface = surface,
                        popProgress = popProgress,
                        selected = showLegacyOnly,
                        onClick = onToggleLegacy,
                        glass = glassBackdrop != null,
                        glassBackdrop = glassBackdrop
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
    // v29 — pills REST at full size (1.0) and only pop subtly (1.05) while
    // the bar actually pins: the old 0.90 rest scale made every pill look
    // like it was GROWING when the Cabinet opened.
    val eased = FastOutSlowInEasing.transform(pillProgress)
    val pillScale = androidx.compose.ui.util.lerp(1f, 1.05f, eased)
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
    // v27n — the banner fill behind the pill (the opaque-fill conversion
    // needs it to resolve the same perceived tint on the banner).
    backdrop: Color,
    label: String? = null,
    glyph: String? = null,
    contentDescription: String? = null,
    // v30 — optional trailing glyph (the Category pill's up/down chevron).
    trailingGlyph: String? = null,
    trailingContentDescription: String? = null,
    emphasized: Boolean = false,
    destructive: Boolean = false
) {
    // v27 — deepen the ink-glass: the old 18% fill vanished on the rose
    // banner (especially in light mode), so hero actions like Select /
    // Sort / Search read as invisible.
    // v27n — the fill is now OPAQUE (ink lerped into the banner at the old
    // glass alpha): a translucent fill let the elevation shadow bleed
    // through as a blurry broken background; the opaque lerp resolves to
    // the exact same perceived tint on the banner.
    // v29 — the fills are now a LIGHT frosted glass (the banner lifted
    // toward white): the v27r ink-lean fills (lerp toward the ink at
    // 0.30/0.35/0.55) read too dark in light + pastel themes. Lifting
    // toward white keeps the same visible-pill look with full-ink glyphs
    // that pop in every mode — creamy in light/pastel, brighter glass on
    // the deep dark banner. Destructive stays the darkest pill (a whisper
    // of black) so delete reads as the danger action.
    // v42 — the glass lifts toward the COLOR-TINTED page background
    // ([curioPillTintLift] — a whisper of the brand rose instead of plain
    // cream) so the Cabinet's hero pills stop reading as flat cream blocks;
    // AMOLED gets a soft grey glass instead of pitch black. Dark keeps the
    // white lift so the pill stays a brighter glass on the deep banner.
    // v108 — dark mode swaps to the FILTER CHIPS' dark raised glass
    // (near-black tinted surface) so the hero pills read as part of the
    // same chip family at night; destructive stays its black-lean pill.
    val fill = if (isCurioDarkTheme()) {
        lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
    } else when {
        destructive -> lerp(backdrop, Color.Black, 0.14f)
        emphasized -> lerp(backdrop, curioPillTintLift(), 0.24f)
        else -> lerp(backdrop, curioPillTintLift(), 0.38f)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = fill,
        shadowElevation = 3.dp,
        // v28 — dark mode elevation visibility (glow + hairline).
        // v85 — same One UI glass glow as the sort dropdown, so the search /
        // select / cancel pills render as the sort pill's identical sibling
        // in dark (before, the sort pill glowed and these stayed flat).
        modifier = Modifier
            .curioDarkGlow(3.dp, RoundedCornerShape(50))
            .curioGlassGlow(RoundedCornerShape(50), ink)
    ) {
        Row(
            // v29 — bigger hit areas (was 11/8dp + 20dp glyph) so the hero
            // controls read as substantial buttons, not tiny chips.
            // v30 — uniform 42dp height so label-only pills match glyph
            // pills and the sort dropdown (which reads the same 42dp).
            // v79 — middle-size unification with the sort pill: height
            // 42 → 46dp and glyph 22 → 20dp so the icon-only Search pill
            // reads the same size as the sort dropdown beside it.
            modifier = Modifier
                .heightIn(min = 46.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (glyph != null) {
                CurioIcon(
                    name = glyph,
                    contentDescription = contentDescription,
                    tint = ink,
                    size = 20.dp
                )
            }
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ink
                )
            }
            if (trailingGlyph != null) {
                CurioIcon(
                    name = trailingGlyph,
                    contentDescription = trailingContentDescription,
                    tint = ink.copy(alpha = 0.85f),
                    size = 18.dp
                )
            }
        }
    }
}

@Composable
private fun FilterChipLite(
    label: String,
    accent: Color,
    ink: Color,
    chipSurface: Color = MaterialTheme.colorScheme.surfaceVariant,
    // v27n — the capsule is defined by its fill + elevation shadow, no
    // outline ring. v7.96 — premium pop: the capsule wears a soft vertical
    // sheen (top light / slightly deeper base) instead of a flat fill; as
    // [popProgress] goes 0→1 the unselected label blooms toward [accent]
    // — the per-pill color pop on top of the scale pop. v27q — selected
    // pills wear a SOLID accent gradient (the old accent-container tints
    // were translucent and let the shadow bleed) and elevation stays a flat
    // 2dp.
    popProgress: Float = 0f,
    selected: Boolean,
    onClick: () -> Unit,
    // v245 — liquid glass: clear refracting capsule over the local page
    // capture; ONE theme ink for every label (no per-category colors).
    glass: Boolean = false,
    glassBackdrop: LayerBackdrop? = null
) {
    val themeInk = if (isCurioDarkTheme()) Color.White else Color.Black
    val labelColor = when {
        glass -> themeInk
        selected -> ink
        else -> lerp(
            MaterialTheme.colorScheme.onSurfaceVariant,
            accent,
            popProgress * 0.55f
        )
    }
    val fillBrush = if (glass) {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    } else if (selected) {
        // v27q — SOLID accent gradient — deeper at the base like the
        // category card fills, so the active pill reads premium.
        Brush.verticalGradient(listOf(accent, lerp(accent, Color.Black, 0.10f)))
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
        // v27q — flat 2dp in both states (no selected raise).
        shadowElevation = if (glass) 0.dp else 2.dp,
        // v28 — dark mode elevation visibility (glow + hairline).
        modifier = Modifier
            .then(
                if (glass && glassBackdrop != null)
                    Modifier.liquidGlassCapsule(
                        container = if (selected) accent
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                        // v292g — Samsung frosted look: forceFrost overrides
                        // the Clear-glass toggle so chips always frost.
                        washAlpha = if (selected) 0.68f else 0.55f,
                        backdrop = glassBackdrop,
                        forceFrost = true
                    ) else Modifier
            )
            .curioDarkGlow(if (glass) 0.dp else 2.dp, RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .then(if (!glass) Modifier.background(fillBrush) else Modifier)
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

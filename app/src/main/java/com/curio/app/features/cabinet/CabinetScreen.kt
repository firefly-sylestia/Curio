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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.curioPillTintLift
import com.curio.app.ui.theme.headerAccent
import com.curio.app.ui.theme.heroHeaderInk
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.pastelFillInk
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

@Composable
fun CabinetScreen(navController: NavController) {
    // Satisfying haptics: a light tick when an entry is opened / actioned.
    val haptics = LocalHapticFeedback.current
    // Wide windows (tablet / landscape) spread the grid into more columns.
    val wide = windowWidthSizeClass().isWide
    // Compact hero on tablets/landscape — 192dp instead of 232dp.
    val compactBannerHeight = if (wide) CabinetHeroBannerHeightCompact else CabinetHeroBannerHeight
    // v316 — MULTI-SELECT category filters (a Set of CategoryIds), persisted
    // as a comma-joined enum-name string so rotation/tab restores keep the
    // selection. Mirrors the Topic Database's v314 category-panel state.
    var selectedFilterNames by rememberSaveable(CabinetSessionToken) {
        mutableStateOf("")
    }
    val selectedFilters: Set<CategoryId> = remember(selectedFilterNames) {
        selectedFilterNames.splitToSequence(',')
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { CategoryId.valueOf(it) }.getOrNull() }
            .toSet()
    }
    var showLegacyOnly by rememberSaveable(CabinetSessionToken) { mutableStateOf(false) }
    // v316 — the one place that mutates the category selection.
    fun commitFilters(update: (Set<CategoryId>) -> Set<CategoryId>) {
        selectedFilterNames = update(selectedFilters).joinToString(",") { it.name }
    }
    // Saveable-backed scroll state — the grid keeps its position on rotation.
    val gridState = rememberLazyGridState()
    // v245 — LOCAL GLASS CAPTURE for the floating filter UI (the scrolling
    // grid records; the panel/chips are sibling overlays).
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
                commitFilters { setOf(catId) }
                showLegacyOnly = false
                searchActive = false
                searchQuery = ""
            }
        }
    }
    // v316 — the Category pill toggles a collapsed-by-default category PANEL
    // (its own tiny search box + checkbox multi-select), replacing the old
    // sticky every-lane chip bar that also auto-showed while searching.
    var categoryPanelOpen by rememberSaveable { mutableStateOf(false) }
    // Tiny search box INSIDE the panel, filtering the category list itself.
    var catPanelQuery by rememberSaveable { mutableStateOf("") }
    // The category UI visible under the hero: the open panel, or the compact
    // active-filter chips row whenever at least one lane (or Legacy) is
    // active. Searching alone shows NO category chips.
    val filterUiVisible = categoryPanelOpen || selectedFilters.isNotEmpty() || showLegacyOnly
    // v31 — the hero keeps its original height. v42 — the Category pill
    // moved INSIDE the hero (beside the title), so content reserves only
    // the filter UI when it is visible.
    val heroTotal = compactBannerHeight + CabinetHeroSheetExtent
    val contentTop = heroTotal +
        (if (categoryPanelOpen) CabinetFilterPanelHeight
         else if (filterUiVisible) CabinetChipBarHeight
         else 0.dp) + 12.dp
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

    val visibleEntries = remember(entries, selectedFilters, showLegacyOnly, searchQuery) {
        val q = searchQuery.trim()
        // v316 — filtering: an entry passes when it matches the text query
        // AND (no categories selected OR its category is in the selected set)
        // AND the Legacy toggle agrees (the Topic Database's v314 contract).
        var result = if (selectedFilters.isEmpty()) entries
            else entries.filter { it.topic.categoryId in selectedFilters }
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
    LaunchedEffect(selectedFilters, showLegacyOnly, searchQuery) {
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
    // background as the filters page — ONLY while a SINGLE category filter is
    // active (multi-select falls back to the neutral shared wash, and "All"
    // stays on the plain theme background like Home).
    val filterCat = selectedFilters.singleOrNull()?.let { CurioCategories.byId(it) }
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
                } else if (selectedFilters.isEmpty() && !showLegacyOnly) {
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
                    // Filters are active but nothing matches: a single lane
                    // gets its own "no X captures yet" + spin CTA; multiple
                    // lanes / combos get a generic clear-filters state.
                    val singleCat = selectedFilters.singleOrNull()
                        ?.let { CurioCategories.byId(it) }
                    if (singleCat != null) {
                        CurioEmptyState(
                            glyph = CurioIcons.SearchOff,
                            headline = "No ${singleCat.displayName} captures yet",
                            subtext = "Shuffle for ${singleCat.displayName} to find your first one.",
                            tint = singleCat.categoryInk().copy(alpha = 0.4f),
                            ctaLabel = "Shuffle for ${singleCat.displayName}",
                            onCtaClick = {
                                // Same tab-switch contract as the "All" empty
                                // state: anchor to HOME so the Shuffle tab
                                // replaces Cabinet instead of stacking.
                                navController.navigateToTab(
                                    CurioRoutes.spinWithCategory(singleCat.id.routeSlug)
                                )
                            }
                        )
                    } else {
                        CurioEmptyState(
                            glyph = CurioIcons.SearchOff,
                            headline = "No captures match these filters",
                            subtext = "Clear some category filters to see your saves again.",
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                            ctaLabel = "Clear filters",
                            onCtaClick = {
                                commitFilters { emptySet() }
                                showLegacyOnly = false
                            }
                        )
                    }
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
                    // v291 — only capture while the filter UI is visible: the
                    // backdrop re-records the full grid every frame, which is
                    // expensive when the panel/chips are hidden.
                    modifier = m
                        .fillMaxSize()
                        .then(if (filterUiVisible && isLiquidGlassPillsActive())
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
                                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
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
            // Per-lane counts for the panel + whether legacy entries exist.
            val catCounts = remember(entries) {
                entries.filterNot { it.isLegacy }
                    .groupingBy { it.topic.categoryId }
                    .eachCount()
            }
            val hasLegacyEntries = entries.any { it.isLegacy }
            if (categoryPanelOpen) {
                // v316 — the collapsed-by-default category PANEL: its own tiny
                // search box (filters the category list) + checkbox multi-select.
                CabinetCategoryPanel(
                    categories = CurioCategories.visible,
                    counts = catCounts,
                    hasLegacy = hasLegacyEntries || showLegacyOnly,
                    query = catPanelQuery,
                    onQueryChange = { catPanelQuery = it },
                    selected = selectedFilters,
                    legacySelected = showLegacyOnly,
                    onToggleCategory = { id ->
                        commitFilters { current -> if (id in current) current - id else current + id }
                    },
                    onToggleLegacy = { showLegacyOnly = !showLegacyOnly },
                    onClearAll = {
                        commitFilters { emptySet() }
                        showLegacyOnly = false
                        catPanelQuery = ""
                    },
                    onDone = { categoryPanelOpen = false },
                    barTop = heroTotal
                )
            } else if (filterUiVisible) {
                // v316 — the active-filter chips row: exactly the selected
                // lanes (+ Legacy when on), each removable with one tap.
                CabinetActiveFilterChips(
                    categories = CurioCategories.visible.filter { it.id in selectedFilters },
                    legacySelected = showLegacyOnly,
                    onRemoveCategory = { id -> commitFilters { current -> current - id } },
                    onRemoveLegacy = { showLegacyOnly = false },
                    onClearAll = {
                        commitFilters { emptySet() }
                        showLegacyOnly = false
                    },
                    barTop = heroTotal
                )
            }
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
            selectedFilters.size == 1 -> "Showing ${CurioCategories.byId(selectedFilters.first()).displayName}"
            selectedFilters.size > 1 -> "Showing ${selectedFilters.size} categories"
            else -> "Your saved captures"
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
                // v316 — the pill opens the category PANEL; the label reflects
                // the current multi-select ("All" / count / Legacy).
                val categoryLabel = when {
                    showLegacyOnly -> "Category · Legacy"
                    selectedFilters.isEmpty() -> "Category · All"
                    else -> "Categories · ${selectedFilters.size}"
                }
                CabinetHeroActionPill(
                    onClick = {
                        if (categoryPanelOpen) catPanelQuery = ""
                        categoryPanelOpen = !categoryPanelOpen
                    },
                    glyph = CurioIcons.Tune,
                    label = categoryLabel,
                    ink = ink,
                    backdrop = backdrop,
                    modifier = if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                        Modifier.liquidGlassCapsule(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            backdrop = chipGlassBackdrop
                        )
                    else Modifier,
                    // v316 — chevron flips with the panel: ▾ closed, ▴ open.
                    trailingGlyph = if (categoryPanelOpen) CurioIcons.KeyboardArrowUp
                        else CurioIcons.KeyboardArrowDown,
                    trailingContentDescription = if (categoryPanelOpen) "Hide category options"
                        else "Show category options",
                    emphasized = categoryPanelOpen || selectedFilters.isNotEmpty() || showLegacyOnly
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
                        modifier = if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                            Modifier.liquidGlassCapsule(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                backdrop = chipGlassBackdrop
                            )
                        else Modifier,
                        emphasized = true
                    )
                    CabinetHeroActionPill(
                        onClick = {
                            if (selectedEntryIds.isNotEmpty()) showBulkDeleteConfirm = true
                        },
                        label = "Delete (${selectedEntryIds.size})",
                        ink = ink,
                        backdrop = backdrop,
                        modifier = if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                            Modifier.liquidGlassCapsule(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                backdrop = chipGlassBackdrop
                            )
                        else Modifier,
                        emphasized = true,
                        destructive = true
                    )
                    CabinetHeroActionPill(
                        onClick = { selectionMode = false; selectedEntryIds = emptySet() },
                        glyph = CurioIcons.Close,
                        contentDescription = "Cancel selection",
                        ink = ink,
                        backdrop = backdrop,
                        modifier = if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                            Modifier.liquidGlassCapsule(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                backdrop = chipGlassBackdrop
                            )
                        else Modifier
                    )
                } else {
                    // v105 — the sort dropdown is gone; the hero row keeps
                    // the Search pill + Recycle Bin pill.
                    CabinetHeroActionPill(
                        onClick = { searchActive = true },
                        glyph = CurioIcons.Search,
                        contentDescription = "Search captures",
                        ink = ink,
                        backdrop = backdrop,
                        modifier = if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                            Modifier.liquidGlassCapsule(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                backdrop = chipGlassBackdrop
                            )
                        else Modifier,
                        // v85 — emphasized hero fill (the hero action-pill
                        // language).
                        emphasized = true
                    )
                    CabinetHeroActionPill(
                        onClick = { navController.navigate(CurioRoutes.RECYCLE_BIN) },
                        glyph = CurioIcons.Delete,
                        contentDescription = "Recycle bin",
                        ink = ink,
                        backdrop = backdrop,
                        modifier = if (isLiquidGlassPillsActive() && chipGlassBackdrop != null)
                            Modifier.liquidGlassCapsule(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                backdrop = chipGlassBackdrop
                            )
                        else Modifier
                    )
                }
            },
            glassBackdrop = if (isLiquidGlassPillsActive()) chipGlassBackdrop else null
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

// ── Category filter UI (v316) ─────────────────────────────────────────
// The old sticky every-lane chip bar is gone (mirroring the Topic Database
// v314): the Category pill opens a collapsed-by-default PANEL with its own
// tiny search box + checkbox multi-select, and the ACTIVE lanes render as a
// compact removable chips row.
/** The active-filter chips row's layout height — content starts below it. */
private val CabinetChipBarHeight = 52.dp
/** The category PANEL's layout height — search header + scrollable checkbox
 *  list (~7-8 lanes visible, then it scrolls). */
private val CabinetFilterPanelHeight = 352.dp

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
    compact: Boolean = false,
    // v292h — optional glass backdrop for the sticky-bar cancel pill.
    glassBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null
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
                                backdrop = fill,
                                modifier = if (isLiquidGlassPillsActive() && glassBackdrop != null)
                                    Modifier.liquidGlassCapsule(
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                        backdrop = glassBackdrop
                                    )
                                else Modifier
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
 * v316 — the Cabinet's ACTIVE-FILTER chips row (mirrors the Topic Database's
 * v314 row): exactly the selected lanes (+ Legacy when on), each removable
 * with ONE tap via its trailing ✕, plus a Clear-all action when anything is
 * active. No "All" pill and no every-lane bar — searching alone shows no
 * chips (the old bar that auto-showed while searching is gone).
 */
@Composable
private fun BoxScope.CabinetActiveFilterChips(
    categories: List<CurioCategory>,
    legacySelected: Boolean,
    onRemoveCategory: (CategoryId) -> Unit,
    onRemoveLegacy: () -> Unit,
    onClearAll: () -> Unit,
    barTop: Dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .offset(y = barTop + 4.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        categories.forEach { cat ->
            Surface(
                onClick = { onRemoveCategory(cat.id) },
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
                        // v3xx14 — identical typography to the Topic Database
                        // chip row (labelLarge, no bold): the Cabinet's chips
                        // previously read heavier than the browser's.
                        cat.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = cat.categoryInk(),
                        maxLines = 1
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
        if (legacySelected) {
            val legacyAccent = MaterialTheme.colorScheme.tertiary
            Surface(
                onClick = onRemoveLegacy,
                shape = RoundedCornerShape(50),
                color = legacyAccent,
                shadowElevation = 1.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Legacy",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiary,
                        maxLines = 1
                    )
                    CurioIcon(
                        CurioIcons.Close,
                        "Remove Legacy filter",
                        tint = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.75f),
                        size = 14.dp
                    )
                }
            }
        }
        if (categories.isNotEmpty() || legacySelected) {
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
 * v316 — the Cabinet's category PANEL (mirrors the Topic Database's v314
 * panel): opens from the hero Category pill, collapsed by default. Its own
 * tiny search box filters the category list itself; lanes toggle via accent
 * checkboxes into the multi-select Set<CategoryId>; Legacy (restored
 * FieldMind records) toggles separately with a tertiary checkbox; Clear all
 * + Done (Done collapses back to the active-filter chips row).
 */
@Composable
private fun BoxScope.CabinetCategoryPanel(
    categories: List<CurioCategory>,
    counts: Map<CategoryId, Int>,
    hasLegacy: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    selected: Set<CategoryId>,
    legacySelected: Boolean,
    onToggleCategory: (CategoryId) -> Unit,
    onToggleLegacy: () -> Unit,
    onClearAll: () -> Unit,
    onDone: () -> Unit,
    barTop: Dp
) {
    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) categories
                else categories.filter { it.displayName.lowercase().contains(q) }
    // Legacy is not a category, so the panel's search box doesn't hide it:
    // the row stays reachable (to toggle off) whenever legacy entries exist.
    val legacyRowVisible = hasLegacy

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
            .padding(top = 6.dp)
            .offset(y = barTop + 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
                if (selected.isNotEmpty() || legacySelected) {
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
            if (shown.isEmpty() && !legacyRowVisible) {
                Text(
                    "No categories match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
            } else {
                // v3xx14 — TWO-column checkbox grid (was one long list); the
                // fixed 260dp max-height keeps the panel the same footprint,
                // now roughly half the scroll depth. Legacy stays a
                // full-width row at the end.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    items(shown) { cat ->
                        CabinetCategoryCheckboxRow(
                            cat = cat,
                            count = counts[cat.id] ?: 0,
                            checked = cat.id in selected,
                            onToggle = { onToggleCategory(cat.id) }
                        )
                    }
                    if (legacyRowVisible) {
                        item(key = "legacy", span = { GridItemSpan(maxLineSpan) }) {
                            CabinetLegacyCheckboxRow(
                                checked = legacySelected,
                                onToggle = onToggleLegacy
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One category row inside the Cabinet panel — accent checkbox + name +
 *  count (the Topic Database panel row's language). */
@Composable
private fun CabinetCategoryCheckboxRow(
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

/** The Legacy (restored FieldMind) row — tertiary checkbox, no lane count. */
@Composable
private fun CabinetLegacyCheckboxRow(
    checked: Boolean,
    onToggle: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    // v328 — same checked-row fill as the category rows (theme-aware).
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
        Text(
            "Legacy",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            "Restored FieldMind records",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
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
    destructive: Boolean = false,
    modifier: Modifier = Modifier
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
        modifier = modifier
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


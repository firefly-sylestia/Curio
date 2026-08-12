package com.curio.app.features.picker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioCategoryCard
import com.curio.app.ui.components.MorphEntrance
import com.curio.app.ui.components.categoryEdgeShine
import com.curio.app.ui.components.curioButtonColors
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryBackgroundWash
import kotlinx.coroutines.launch

/**
 * Full-screen Category Picker.
 *
 * v27i — two swipeable pages behind the same deck grid:
 *  - **Original** — the 21 original lanes (tap a card to open it on the Spin
 *    page immediately; tap-and-hold enters multi-select to Mix a deck).
 *  - **New** — the 15 content-expansion lanes. Lanes whose topic data has
 *    shipped (`CurioCategory.isReady`) behave like original cards; the rest
 *    render as disabled **Coming soon** tiles.
 *
 * Quick mixes: a small row of preset chips (Brainy / Stories / Screens /
 * Sounds / Everything) replaces the old subtitle. Tapping a preset enters
 * multi-select with exactly those lanes ticked, so the user can SEE and
 * CHANGE the mix before hitting Mix — the same row on both pages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerScreen(navController: NavController) {
    val context = LocalContext.current
    // v7.94 — read the REACTIVE visible list directly (no remember): hidden
    // lanes drop out and reordered lanes follow Manage Categories instantly.
    // v27i — `visible` already excludes new lanes that haven't shipped, so the
    // original page (and every other surface) never shows empty dead tiles.
    val categories = CurioCategories.visible
    // v27i — the NEW-lanes page reads `all` directly so it can show the
    // not-yet-shipped lanes as Coming soon tiles (they're filtered out of
    // `visible` until their content ships).
    val newLanes = CurioCategories.all.filter {
        it.id in CategoryId.newLanes && it.id !in AppPreferences.hiddenCategoriesState
    }
    val originalLanes = categories.filter { it.id !in CategoryId.newLanes }
    // v26 — reopen with the persisted deck: a mixed selection comes back in
    // multi-select with every lane ticked, so the user can SEE and CHANGE
    // the mix instead of it collapsing to the single first category. Hidden
    // lanes are filtered out so they never show as pre-selected.
    val persistedVisible = remember {
        AppPreferences.getLastSpinCategories(context)
            .mapNotNull { id -> categories.firstOrNull { it.id == id } }
    }
    val originalGridState = rememberLazyGridState()
    val newGridState = rememberLazyGridState()
    // Wide windows (tablet / landscape) spread the deck grid and cap the
    // sheet's content width so the picker stays readable on large screens.
    val wide = windowWidthSizeClass().isWide
    // ── Category tint wash — this picker hands off straight to the Shuffle
    //    tab, so it wears the last-used deck's color story (same wash as the
    //    Spin page / Save / Cabinet) instead of a plain theme background.
    val washCat = remember {
        val id = AppPreferences.getLastSpinCategories(context).firstOrNull()
            ?: AppPreferences.getLastSpinCategory(context)
        CurioCategories.byId(id)
    }
    // Null = not in multi-select mode (tap-to-open). Once set, cards toggle.
    var selectedSlugs by rememberSaveable {
        mutableStateOf(persistedVisible.map { it.id.routeSlug })
    }
    var multiSelectMode by rememberSaveable { mutableStateOf(persistedVisible.size > 1) }

    val toggleSlug = { slug: String ->
        selectedSlugs = if (slug in selectedSlugs) selectedSlugs - slug else selectedSlugs + slug
    }

    // ── v27i — the two pages (Original / New) + a scope for tab jumps ──
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // Same full-screen + swipe-down-dismiss pattern as the filter page — a
    // ModalBottomSheet expanded to full height with a drag handle.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // v8.21 — tell the pet a drawer is up so it comes over to peek.
    LaunchedEffect(Unit) { PetLandmarks.noteSheet("picker", true) }
    DisposableEffect(Unit) {
        onDispose { PetLandmarks.noteSheet("picker", false) }
    }

    ModalBottomSheet(
        onDismissRequest = { navController.popBackStack() },
        sheetState = sheetState,
        // Theme-aware category wash — deep accent over cream in light,
        // pastel twin glow over midnight in dark.
        containerColor = washCat.categoryBackgroundWash(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        // The sheet spans the whole window; on wide windows the content is
        // centered in the same max-width column as every other page.
        Box(
            modifier = Modifier.fillMaxWidth(),
            // contentAlignment takes a full Alignment, not Alignment.Horizontal
            // (CenterHorizontally) — Center also matches the vertical no-op
            // since the box wraps the sheet content's height.
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurioBackButton(onClick = { navController.popBackStack() })
            Text(
                text = "What are we exploring?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── v27i — quick-mix preset chips (replaces the old subtitle; the
        //    same row sits above BOTH pages). Tapping one enters multi-select
        //    with exactly those lanes ticked so the mix can be adjusted
        //    before Mix — it never silently launches a deck you can't see.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            deckPresets.forEach { preset ->
                val active = multiSelectMode &&
                    preset.lanes(categories).all { it.id.routeSlug in selectedSlugs }
                PickerPresetChip(
                    label = preset.label,
                    glyph = preset.glyph,
                    selected = active,
                    onClick = {
                        val lanes = preset.lanes(categories)
                        if (lanes.isNotEmpty()) {
                            multiSelectMode = true
                            selectedSlugs = lanes.map { it.id.routeSlug }
                        }
                    }
                )
            }
        }

        // ── v27i — page tabs: Original vs the new lanes ─────────────
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PickerPageTab(
                label = "Original",
                count = originalLanes.size,
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
            )
            PickerPageTab(
                label = "New",
                count = newLanes.size,
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
            )
            Spacer(Modifier.weight(1f))
            // v27i — the tap/hold hint moved here so the preset chips could
            // take the subtitle slot; multi-select is still a long-press.
            Text(
                text = if (multiSelectMode) {
                    "Tap to toggle decks"
                } else {
                    "Tap opens · hold to mix"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
        }

        // v7.4 — the grid sits inside a WEIGHTED Box that is a DIRECT child
        // of the sheet Column. Weight inside the old MorphEntrance wrapper
        // was ignored, so the grid rendered at full height and pushed the
        // Mix row off-screen on smaller phones. The Box keeps the entrance
        // animation AND bounds the grid, so the action row stays pinned.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            MorphEntrance {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> LazyVerticalGrid(
                            state = originalGridState,
                            columns = if (wide) GridCells.Adaptive(minSize = 160.dp) else GridCells.Fixed(2),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                        items(originalLanes) { cat ->
                            val slug = cat.id.routeSlug
                            CurioCategoryCard(
                                category = cat,
                                isSelected = multiSelectMode && slug in selectedSlugs,
                                onClick = {
                                    if (multiSelectMode) {
                                        toggleSlug(slug)
                                    } else {
                                        // Default: tap opens this category on the
                                        // persistent Shuffle tab (the plain "spin"
                                        // route), not a separate spin/{slug} page.
                                        // The selection is persisted so it survives
                                        // back navigation, tab switches and relaunch.
                                        AppPreferences.setLastSpinCategories(context, listOf(cat.id))
                                        navController.navigateToTab(CurioRoutes.SPIN)
                                    }
                                },
                                onLongClick = {
                                    // Enter multi-select mode and select this card.
                                    multiSelectMode = true
                                    if (slug !in selectedSlugs) toggleSlug(slug)
                                }
                            )
                        }
                        }
                        else -> LazyVerticalGrid(
                            state = newGridState,
                            columns = if (wide) GridCells.Adaptive(minSize = 160.dp) else GridCells.Fixed(2),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                        items(newLanes) { cat ->
                            val slug = cat.id.routeSlug
                            val comingSoon = !cat.isReady
                            CurioCategoryCard(
                                category = cat,
                                comingSoon = comingSoon,
                                isSelected = multiSelectMode && slug in selectedSlugs,
                                onClick = {
                                    if (!comingSoon) {
                                        if (multiSelectMode) {
                                            toggleSlug(slug)
                                        } else {
                                            AppPreferences.setLastSpinCategories(context, listOf(cat.id))
                                            navController.navigateToTab(CurioRoutes.SPIN)
                                        }
                                    }
                                },
                                onLongClick = if (comingSoon) null else {
                                    {
                                        multiSelectMode = true
                                        if (slug !in selectedSlugs) toggleSlug(slug)
                                    }
                                }
                            )
                        }
                        }
                    }
                }
            }
        }

        if (multiSelectMode) {
            // ── Mix row — only visible in multi-select mode ────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val mixShape = RoundedCornerShape(24.dp)
                Button(
                    onClick = {
                        if (selectedSlugs.isEmpty()) return@Button
                        // Resolve the chosen slugs and persist the FULL set
                        // (single or mixed) so the Shuffle tab reopens the
                        // same deck after back navigation, tab switches and
                        // app restarts. navigateToTab drops the picker and
                        // lands on the real Shuffle tab — not a separate
                        // spin/{slug} instance.
                        val ids = selectedSlugs.mapNotNull { CurioCategories.byRouteSlug(it)?.id }
                        if (ids.isEmpty()) return@Button
                        AppPreferences.setLastSpinCategories(context, ids)
                        navController.navigateToTab(CurioRoutes.SPIN)
                    },
                    enabled = selectedSlugs.isNotEmpty(),
                    shape = mixShape,
                    colors = curioButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        // v12 — AMOLED: curioButtonColors forces the plate to
                        // pitch black, so the scheme's onPrimary (a deep
                        // maroon) would vanish on it — the content flips to
                        // white on the black glass.
                        contentColor = if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED)
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .categoryEdgeShine(mixShape)
                ) {
                    CurioIcon(CurioIcons.Check, null, size = 18.dp)
                    Text(
                        text = if (selectedSlugs.isEmpty()) "Mix" else "Mix · ${selectedSlugs.size}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                TextButton(
                    onClick = {
                        // Exit multi-select mode; selection is discarded.
                        multiSelectMode = false
                        selectedSlugs = emptyList()
                    }
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
    }
    }
}

/**
 * DeckPreset, deckPresets, and PickerPresetChip live in DeckPresets.kt so the
 * full-screen picker and the Spin page's inline sheet share the same mixes.
 */

/** Small page-tab pill for the Original / New pager. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerPageTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .categoryEdgeShine(shape, accent = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

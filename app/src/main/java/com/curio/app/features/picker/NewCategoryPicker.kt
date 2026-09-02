package com.curio.app.features.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.NamedMix
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.curioPillLift
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent
import com.curio.app.data.CurioPassport
import kotlin.random.Random
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

/**
 * The NEW category picker — "Category Mix Studio".
 *
 * A premium-minimal, user-friendly replacement for the glass-pill picker:
 * a quick sheet (pinned lanes · named mixes · continue exploring) plus a
 * full Browse page ([NewCategoryPickerBrowseScreen]) with an in-page bottom nav.
 *
 * Style contract (user direction v3xx2 + v27 refinement):
 *  - **Idle tiles use neutral cream-lift roles** (the classic picker's raised
 *    cream pill recipe in light mode). SELECTION wears the **classic
 *    category-tint style** — a solid [themedAccent] fill with [onAccent] ink
 *    (icon, label, check) — per user direction. Tap-and-hold a category
 *    shows an option pill (Pin/Unpin · Spin · or Remove for Continue-
 *    exploring lanes) instead of pinning directly. Same in the Pinned row.
 *  - The sheet has **no close (cross) button** — swipe down or back to close.
 *  - A **HorizontalPager** swaps between page 1 — the Curio / Knowledge /
 *    Mix mode picker (PRESET chips removed, per user direction) — and page 2,
 *    the new picker. The user can set which opens first
 *    (`AppPreferences.pickerDefaultPageState`). Both pages share the same
 *    bottom action row (Surprise me · Create mix · Browse).
 *  - Page 1 starts CLEAN every open (tap opens a lane; HOLD is the only way
 *    into multi-select — no persisted-deck pre-tick, so holding selects one).
 *  - **Your mixes**: a 2-column grid, max 6 visible + an Expand button; below
 *    it a "Continue exploring" section: recently-spun categories first, then
 *    curated "fun to explore" lanes up to 10 (hold → Remove, plus an Add
 *    sheet). Both update LIVE via reactive prefs state.
 *
 * Everything here is gated by `AppPreferences.classicPickerEnabledState`:
 * OFF (default) = this picker; ON restores the old CategoryPickerContent.
 */

/** Seeds the starter named mixes (from the old quick presets) once. */
fun seedStarterMixes(context: android.content.Context) {
    if (AppPreferences.pickerMixesSeededState) return
    AppPreferences.setPickerMixesSeeded(context, true)
    if (AppPreferences.savedMixesState.isNotEmpty()) return
    val now = System.currentTimeMillis()
    val starter = deckPresets.filter { !it.clearAll }.mapNotNull { preset ->
        val lanes = preset.lanes(CurioCategories.visible)
        if (lanes.isEmpty()) null
        else NamedMix(
            name = preset.label,
            laneIds = lanes.map { it.id },
            createdAtMillis = now -
                (deckPresets.size - deckPresets.indexOf(preset)).toLong()
        )
    }
    if (starter.isNotEmpty()) AppPreferences.saveSavedMixes(context, starter)
}

/**
 * "Surprise me" — a random mini-mix of ~4-6 ready visible lanes. Around a
 * quarter of the time it opts for the full Wildcard (everything) surprise.
 */
fun surpriseMiniMix(categories: List<CurioCategory>): List<CurioCategory> {
    val ready = categories.filter { it.isReady }
    if (ready.isEmpty()) return emptyList()
    if (ready.any { it.id == CategoryId.WILDCARD } && Random.nextFloat() < 0.25f) {
        return listOf(CurioCategories.byId(CategoryId.WILDCARD))
    }
    val count = (4..6).random().coerceAtMost(ready.size)
    return ready.shuffled().take(count)
}

/**
 * The quick "Category Mix Studio" sheet — renders INSIDE the host's
 * ModalBottomSheet (mirrors [CategoryPickerContent]'s contract so the Spin
 * page and the PICKER route share one surface).
 *
 * Layout: title → pager (classic / new) → pinned bottom action row.
 * The new page holds: Pinned · Your mixes (grid) · Continue exploring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCategoryPickerSheet(
    washCat: CurioCategory,
    categories: List<CurioCategory>,
    onCategorySelected: (CurioCategory) -> Unit,
    onCategoriesMixed: (List<CurioCategory>) -> Unit,
    onBrowse: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    seedStarterMixes(context)

    val deckIds = remember { AppPreferences.getLastSpinCategories(context) }
    val mixes = AppPreferences.savedMixesState

    var showEditor by remember { mutableStateOf(false) }
    var editMix by remember { mutableStateOf<NamedMix?>(null) }
    var deleteMix by remember { mutableStateOf<NamedMix?>(null) }
    var mixesExpanded by remember { mutableStateOf(false) }
    var pinnedList by remember {
        mutableStateOf(
            AppPreferences.getPinnedCategories(context)
                .mapNotNull { id -> CurioCategories.all.firstOrNull { it.id == id } }
        )
    }
    // Tap-and-hold target category → option pill overlay.
    var optionTarget by remember { mutableStateOf<CurioCategory?>(null) }
    // Tap-and-hold on a Continue-exploring lane → remove pill overlay.
    var removeTarget by remember { mutableStateOf<CurioCategory?>(null) }
    // Add-suggestion picker dialog.
    var showAddSuggestion by remember { mutableStateOf(false) }
    // Tap-and-hold on a saved mix → Edit/Delete option pill overlay.
    var mixOptionTarget by remember { mutableStateOf<NamedMix?>(null) }

    // v3xx14 — multi-select on page 0 flips the shared bottom row's primary
    // capsule from "Surprise me" to "Mix · N": the classic page reports its
    // pending selection count + an apply closure; the capsule applies it.
    var page0MixCount by remember { mutableIntStateOf(0) }
    var page0MixApply by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Pager: page 0 = classic picker, page 1 = new picker. Default from prefs.
    val initialPage = AppPreferences.pickerDefaultPageState.coerceIn(0, 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    // Persist the user's chosen default page once they settle on one.
    // (v3xx13 — this "last page becomes the default" behavior is the
    // intended feature and stays as is; the scroll positions below are the
    // new persistence.)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != initialPage) {
            AppPreferences.setPickerDefaultPage(context, pagerState.currentPage)
        }
    }

    // Per-page scroll persistence (v3xx13/v3xx14): each pager page's scroll
    // state is hoisted here so flipping between pages keeps the position, and
    // the index/offset are saved to prefs (debounced) so closing the sheet —
    // or even restarting the app — returns you to where you were.
    // Page 0 (ClassicPickerPage) keeps its OWN per-mode-tab grid states +
    // persistence internally (v3xx14 — Curio/Knowledge/Mix each scroll
    // separately); page 1 is a LazyColumn (LazyListState) saved here.
    val newScroll = rememberLazyListState()
    LaunchedEffect(Unit) {
        // runCatching: the saved index may exceed the item count if the
        // hidden lane set changed since the position was stored.
        val p1 = AppPreferences.getPickerPage1Scroll(context)
        runCatching { newScroll.scrollToItem(p1.index, p1.offset) }
    }
    LaunchedEffect(newScroll) {
        snapshotFlow {
            newScroll.firstVisibleItemIndex to newScroll.firstVisibleItemScrollOffset
        }
            .drop(1)
            .debounce(300)
            .collect { (index, offset) ->
                AppPreferences.setPickerPage1Scroll(
                    context,
                    AppPreferences.PickerScrollPos(index, offset)
                )
            }
    }

    val pinnedToggle: (CategoryId) -> Unit = { id ->
        pinnedList = AppPreferences.togglePinnedCategory(context, id)
            .mapNotNull { pid -> CurioCategories.all.firstOrNull { it.id == pid } }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // ── Header: big friendly title (NO close button) ───────────
            Text(
                text = "Pick your mix",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (pagerState.currentPage == 0) "Curio · Knowledge · Mix — swipe for picks"
                       else "Pins, mixes & a surprise",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            // ── Pager: classic (0) / new (1) ───────────────────────────
            // The pager MUST fill the remaining sheet height (weight fill =
            // true): with fill = false the child is measured with an infinite
            // max height, which the pages' LazyColumn / LazyVerticalGrid pass
            // through and crash on ("Vertically scrollable component was
            // measured with an infinity maximum height constraints").
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = 12.dp
            ) { page ->
                if (page == 0) {
                    // Page 1 — Curio / Knowledge / Mix mode picker.
                    // Self-contained so its LazyVerticalGrid fills the pager
                    // page cleanly (no nested weight conflict).
                    ClassicPickerPage(
                        washCat = washCat,
                        onCategorySelected = onCategorySelected,
                        onCategoriesMixed = onCategoriesMixed,
                        onMixStatus = { count, apply ->
                            page0MixCount = count
                            page0MixApply = apply
                        }
                    )
                } else {
                    NewPickerPage(
                        categories = categories,
                        deckIds = deckIds,
                        mixes = mixes,
                        mixesExpanded = mixesExpanded,
                        onMixesExpanded = { mixesExpanded = it },
                        scrollState = newScroll,
                        pinnedList = pinnedList,
                        onPinnedToggle = pinnedToggle,
                        onSpinLane = onCategorySelected,
                        onApplyMix = { mix ->
                            val cats = mix.laneIds.mapNotNull { id ->
                                categories.firstOrNull { it.id == id }
                            }
                            if (cats.isNotEmpty()) {
                                // v318b — a named mix stamps the Spin pill.
                                AppPreferences.setLastMixName(context, mix.name)
                                onCategoriesMixed(cats)
                            }
                        },
                        onMixOption = { mixOptionTarget = it },
                        onNewMix = { editMix = null; showEditor = true },
                        onOptionTarget = { optionTarget = it },
                        onRemoveTarget = { removeTarget = it },
                        onAddSuggestion = { showAddSuggestion = true }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Shared bottom action row (both pages) ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // v3xx14 — while page 0 is building a mix the primary capsule
                // becomes "Mix · N" (and applies the pending selection);
                // otherwise it stays "Surprise me".
                val mixing = page0MixCount > 0
                NewPrimaryCapsule(
                    label = if (mixing) "Mix · $page0MixCount" else "Surprise me",
                    glyph = if (mixing) CurioIcons.Check else CurioIcons.Shuffle,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (mixing) {
                            page0MixApply?.invoke()
                        } else {
                            // v318b — a surprise/unnamed deck clears the name.
                            AppPreferences.setLastMixName(context, null)
                            val mix = surpriseMiniMix(categories)
                            if (mix.isNotEmpty()) onCategoriesMixed(mix)
                        }
                    }
                )
                NewPickerCircle(
                    glyph = CurioIcons.Add,
                    contentDescription = "Create a mix",
                    onClick = { editMix = null; showEditor = true }
                )
                NewPickerCircle(
                    glyph = CurioIcons.GridView,
                    contentDescription = "Browse all categories",
                    onClick = onBrowse
                )
            }
        }

        // ── Overlays — INSIDE the picker Box so the .fillMaxSize() scrim
        // covers the whole sheet. As Column siblings the option pill only
        // filled the leftover space BELOW the picker content and could
        // render invisible (e.g. hold on a Pinned pill showed nothing). ────
        optionTarget?.let { target ->
            CategoryOptionPill(
                category = target,
                onDismiss = { optionTarget = null },
                onPinToggle = {
                    pinnedToggle(target.id)
                    optionTarget = null
                },
                onSpin = {
                    optionTarget = null
                    if (target.isReady) onCategorySelected(target)
                }
            )
        }
        removeTarget?.let { target ->
            CategoryOptionPill(
                category = target,
                onDismiss = { removeTarget = null },
                onRemove = {
                    // Removing a curated lane writes the user's suggestion
                    // list live, so the section updates without closing the
                    // picker.
                    AppPreferences.removePickerSuggestion(context, target.id)
                    removeTarget = null
                }
            )
        }
        // Tap-and-hold on a saved mix → Edit/Delete option pill overlay.
        mixOptionTarget?.let { target ->
            MixOptionPill(
                name = target.name,
                onDismiss = { mixOptionTarget = null },
                onEdit = {
                    mixOptionTarget = null
                    editMix = target
                    showEditor = true
                },
                onDelete = {
                    mixOptionTarget = null
                    deleteMix = target
                }
            )
        }
    }

    // ── Mix editor + delete confirm + add-suggestion ─────────────────
    if (showEditor) {
        MixEditorSheet(
            washCat = washCat,
            categories = categories,
            editMix = editMix,
            onDismiss = { showEditor = false },
            onSave = { mix ->
                AppPreferences.addOrReplaceMix(context, mix)
                showEditor = false
                val cats = mix.laneIds.mapNotNull { id -> categories.firstOrNull { it.id == id } }
                if (cats.isNotEmpty()) {
                    // v318b — a saved/edited mix stamps the Spin pill.
                    AppPreferences.setLastMixName(context, mix.name)
                    onCategoriesMixed(cats)
                }
            }
        )
    }
    if (deleteMix != null) {
        AlertDialog(
            containerColor = com.curio.app.ui.theme.curioDialogContainerColor(),
            shape = com.curio.app.ui.theme.CurioDialogShape,
            onDismissRequest = { deleteMix = null },
            title = { Text("Delete \"${deleteMix?.name}\"?") },
            text = { Text("The mix is removed from your saved list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteMix?.let { AppPreferences.deleteMix(context, it.createdAtMillis) }
                        deleteMix = null
                    },
                    colors = com.curio.app.ui.theme.curioDialogActionButtonColors()
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteMix = null },
                    colors = com.curio.app.ui.theme.curioDialogActionButtonColors()
                ) { Text("Keep") }
            }
        )
    }
    // Add-suggestion picker.
    if (showAddSuggestion) {
        AddSuggestionSheet(
            categories = categories,
            onDismiss = { showAddSuggestion = false }
        )
    }
}

/** The NEW picker page (page 1): Pinned · Your mixes · Continue exploring. */
@Composable
private fun NewPickerPage(
    categories: List<CurioCategory>,
    deckIds: List<CategoryId>,
    mixes: List<NamedMix>,
    mixesExpanded: Boolean,
    onMixesExpanded: (Boolean) -> Unit,
    scrollState: LazyListState,
    pinnedList: List<CurioCategory>,
    onPinnedToggle: (CategoryId) -> Unit,
    onSpinLane: (CurioCategory) -> Unit,
    onApplyMix: (NamedMix) -> Unit,
    onMixOption: (NamedMix) -> Unit,
    onNewMix: () -> Unit,
    onOptionTarget: (CurioCategory) -> Unit,
    onRemoveTarget: (CurioCategory) -> Unit,
    onAddSuggestion: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Pinned (taller/wider pills) ──────────────────────────────
        item(key = "pinned-label") {
            NewSectionLabel("Pinned")
        }
        if (pinnedList.isEmpty()) {
            item(key = "pinned-empty") {
                Text(
                    "Hold a category to pin your favourites here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )
            }
        } else {
            item(key = "pinned-row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pinnedList.forEach { cat ->
                        NewPinnedPill(
                            category = cat,
                            onClick = { onSpinLane(cat) },
                            onLongClick = { onOptionTarget(cat) }
                        )
                    }
                }
            }
        }

        // ── Your mixes (2-col grid, max 6 + expand) ───────────────────
        item(key = "mixes-label") {
            // v3xx13 — the header "New" pill is gone (the bottom action
            // row's "+" already creates mixes); the label row stays clean.
            NewSectionLabel(
                if (mixes.isEmpty()) "Your mixes" else "Your mixes · ${mixes.size}"
            )
        }
        if (mixes.isEmpty()) {
            // Empty state is a CTA (previously bare text — with zero mixes
            // there was no visible way to create one on this page).
            item(key = "mixes-empty") {
                NewSecondaryOutline(
                    label = "Build your first mix",
                    glyph = CurioIcons.Add,
                    onClick = onNewMix,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        } else {
            // v27 — six mixes visible, then "Show all"
            // (was five — the grid reads fuller at six).
            val visible = if (mixesExpanded) mixes else mixes.take(6)
            val showExpand = !mixesExpanded && mixes.size > 6
            item(key = "mixes-grid") {
                // A manual 2-col grid via chunked rows (keeps expand simple).
                val rows = visible.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowMixes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMixes.forEach { mix ->
                                NewMixCard(
                                    mix = mix,
                                    categories = categories,
                                    active = mix.laneIds.toSet() == deckIds.toSet(),
                                    onApply = { onApplyMix(mix) },
                                    onLongClick = { onMixOption(mix) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill the last cell of an odd row so the grid stays balanced.
                            if (rowMixes.size == 1) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (showExpand) {
                        NewSecondaryOutline(
                            label = "Show all ${mixes.size}",
                            glyph = CurioIcons.KeyboardArrowDown,
                            onClick = { onMixesExpanded(true) }
                        )
                    } else if (mixesExpanded && mixes.size > 6) {
                        NewSecondaryOutline(
                            label = "Show less",
                            glyph = CurioIcons.KeyboardArrowUp,
                            onClick = { onMixesExpanded(false) }
                        )
                    }
                }
            }
        }

        // ── Continue exploring (recently spun + curated suggestions) ──
        item(key = "explore-label") {
            NewSectionLabel("Continue exploring")
        }
        item(key = "explore-grid") {
            ContinueExploringSection(
                categories = categories,
                deckIds = deckIds,
                onSpinLane = onSpinLane,
                onRemoveTarget = onRemoveTarget,
                onAdd = onAddSuggestion
            )
        }
    }
}

/**
 * "Continue exploring": the user's most-spun categories (from CurioPassport)
 * first, then curated "fun to explore" lanes up to 10 total. The user can
 * remove a lane (tap-and-hold → Remove pill) and add from the full list.
 * Reads the suggestions LIVE ([AppPreferences.pickerSuggestionsState]) so
 * Add/Remove in the picker's sheets updates this grid instantly, without
 * closing and reopening the picker.
 */
@Composable
private fun ContinueExploringSection(
    categories: List<CurioCategory>,
    deckIds: List<CategoryId>,
    onSpinLane: (CurioCategory) -> Unit,
    onRemoveTarget: (CurioCategory) -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    val deckSet = remember(deckIds) { deckIds.toSet() }
    val visibleIds = remember { categories.map { it.id }.toSet() }
    // Most-spun (CurioPassport spin counts), desc, ready + visible only.
    val mostUsed = remember {
        CurioPassport.allProgress(context).entries
            .filter { it.key in visibleIds && it.value.spins > 0 }
            .sortedByDescending { it.value.spins }
            .map { it.key }
    }
    // REACTIVE read (no remember): toggling a suggestion in the Add sheet or
    // removing one here recomposes this list immediately.
    val curated = AppPreferences.pickerSuggestionsState
        .ifEmpty { AppPreferences.defaultSuggestions }
    val combined = remember(mostUsed, curated) {
        val seen = mutableSetOf<CategoryId>()
        val out = mutableListOf<CategoryId>()
        mostUsed.forEach { if (seen.add(it)) out.add(it) }
        curated.forEach { if (seen.add(it) && it in visibleIds) out.add(it) }
        out.take(10)
    }
    val wide = windowWidthSizeClass().isWide
    // Manual chunked rows — NOT a nested LazyVerticalGrid. This section
    // renders inside a LazyColumn item, and lazy items are measured with an
    // INFINITE max height, so a nested lazy grid crashed the picker
    // ("Vertically scrollable component was measured with an infinity
    // maximum height constraints"). With ≤10 lanes + the Add tile it's
    // small enough to lay out as plain rows, like the mixes grid.
    val cols = if (wide) 4 else 3
    val tiles: List<CategoryId?> = mutableListOf<CategoryId?>().apply {
        addAll(combined)
        add(null)  // null = the "+ Add" tile
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(cols).forEach { rowIds ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowIds.forEach { id ->
                    if (id == null) {
                        AddSuggestionTile(onClick = onAdd, modifier = Modifier.weight(1f))
                    } else {
                        val cat = categories.firstOrNull { it.id == id }
                        if (cat != null) {
                            NewPickerTile(
                                category = cat,
                                // v3xx14 — lanes already in the current deck
                                // wear the category's ACTIVE accent in
                                // Continue exploring too.
                                selected = cat.id in deckSet,
                                onClick = { onSpinLane(cat) },
                                onLongClick = { onRemoveTarget(cat) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                repeat(cols - rowIds.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Page 1 — the Curio / Knowledge / Mix mode picker. Replaces the old preset
 * chips (Science / Entertainment / …) + flat grid: three mode tabs now lead
 * the page, with the same grouped tap-to-open decks as the classic picker
 * and a Mix mode holding the multi-select grid + Mix/Cancel row.
 *
 * v27 — every open starts CLEAN: tap opens a lane (single), tap-and-hold is
 * the ONLY way into multi-select, so holding selects EXACTLY ONE lane. The
 * old version seeded the selection from the persisted deck, so starting a
 * mix lit up the previous deck's lanes too ("it auto-selects 2").
 * Self-contained so its LazyVerticalGrid fills the pager page cleanly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicPickerPage(
    washCat: CurioCategory,
    onCategorySelected: (CurioCategory) -> Unit,
    onCategoriesMixed: (List<CurioCategory>) -> Unit,
    // v3xx14 — reports the pending multi-select (count + an apply closure)
    // so the SHARED bottom row can swap "Surprise me" for "Mix · N".
    onMixStatus: (Int, (() -> Unit)?) -> Unit
) {
    val context = LocalContext.current
    val allUnhidden = remember {
        CurioCategories.all.filter { it.id !in AppPreferences.hiddenCategoriesState }
    }
    val sortedCategories = remember(allUnhidden) {
        val wildcard = allUnhidden.filter { it.id == CategoryId.WILDCARD }
        val rest = allUnhidden.filter { it.id != CategoryId.WILDCARD }
            .sortedBy { it.displayName.lowercase() }
        wildcard + rest
    }
    // v196 — always open in tap-to-open (single) mode with an EMPTY
    // selection, even when the current deck is a mix. Tap OPENS a category
    // (replacing the deck); long-press is the ONLY way into multi-select.
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedSlugs by remember { mutableStateOf(emptySet<String>()) }
    // v3xx14 — the mode TAB (Curio / Knowledge / Mix) persists across sheet
    // closes and app restarts (seeded from prefs; saved on every switch).
    var modeName by rememberSaveable {
        mutableStateOf(
            AppPreferences.pickerPage0ModeState.takeIf { it in PickerMode.entries.map { m -> m.name } }
                ?: PickerMode.MIX.name
        )
    }
    val mode = runCatching { PickerMode.valueOf(modeName) }.getOrDefault(PickerMode.MIX)
    LaunchedEffect(mode) {
        AppPreferences.setPickerPage0Mode(context, mode.name)
    }
    // v3xx14 — ONE grid state per mode tab; each is persisted (debounced)
    // under its own pref key so every tab returns to where it was.
    val curioGrid = rememberLazyGridState()
    val knowledgeGrid = rememberLazyGridState()
    val mixGrid = rememberLazyGridState()
    val gridStates = mapOf(
        PickerMode.CURIO to curioGrid,
        PickerMode.KNOWLEDGE to knowledgeGrid,
        PickerMode.MIX to mixGrid
    )
    LaunchedEffect(Unit) {
        // runCatching: a saved index may exceed the item count if the hidden
        // lane set changed since the position was stored.
        PickerMode.entries.forEach { m ->
            val pos = AppPreferences.getPickerPage0TabScroll(context, m.name)
            runCatching { gridStates.getValue(m).scrollToItem(pos.index, pos.offset) }
        }
    }
    PickerMode.entries.forEach { m ->
        val gridState = gridStates.getValue(m)
        LaunchedEffect(gridState) {
            snapshotFlow {
                gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
            }
                .drop(1)
                .debounce(300)
                .collect { (index, offset) ->
                    AppPreferences.setPickerPage0TabScroll(
                        context, m.name, AppPreferences.PickerScrollPos(index, offset)
                    )
                }
        }
    }
    val toggleSlug = { slug: String ->
        selectedSlugs = if (slug in selectedSlugs) selectedSlugs - slug else selectedSlugs + slug
    }
    // Keep the shared bottom row in sync: while multi-selecting on Mix the
    // capsule reports the count + an apply closure; otherwise it clears.
    LaunchedEffect(multiSelectMode, selectedSlugs, mode) {
        if (mode == PickerMode.MIX && multiSelectMode && selectedSlugs.isNotEmpty()) {
            val cats = selectedSlugs.mapNotNull { CurioCategories.byRouteSlug(it) }
            onMixStatus(cats.size) {
                if (cats.isNotEmpty()) {
                    // v318b — an UNNAMED multi-lane selection clears the name.
                    AppPreferences.setLastMixName(context, null)
                    onCategoriesMixed(cats)
                }
            }
        } else {
            onMixStatus(0, null)
        }
    }
    val wide = windowWidthSizeClass().isWide

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Mode tabs: Curio / Knowledge / Mix (classic tint style) ────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PickerMode.entries.forEach { m ->
                ClassicModeTab(
                    label = m.label,
                    glyph = m.glyph,
                    selected = mode == m,
                    accent = washCat.themedAccent(),
                    accentInk = washCat.onAccent(),
                    onClick = {
                        modeName = m.name
                        // Leaving Mix drops a transient multi-select.
                        if (m != PickerMode.MIX) {
                            multiSelectMode = false
                            selectedSlugs = emptySet()
                        }
                    }
                )
            }
        }

        // Hint row — mode aware.
        Text(
            text = when {
                mode == PickerMode.MIX && multiSelectMode -> "Tap to toggle lanes"
                mode == PickerMode.MIX -> "Tap opens · hold to start a mix"
                mode == PickerMode.CURIO -> "A relaxed, culture-first deck — tap any lane to explore"
                else -> "Dig into knowledge — tap any lane to explore"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )

        // Grid — fills the remaining height.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (mode) {
                // ── Mix — full deck, multi-select via hold ────────────
                PickerMode.MIX -> LazyVerticalGrid(
                    state = gridStates.getValue(PickerMode.MIX),
                    columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItems(sortedCategories) { cat ->
                        val slug = cat.id.routeSlug
                        NewPickerTile(
                            category = cat,
                            comingSoon = !cat.isReady,
                            selected = multiSelectMode && slug in selectedSlugs,
                            // v3xx14 — while multi-selecting the Wildcard tile
                            // relabels itself "Mix" (it no longer surprises; it
                            // toggles the lane into the pending mix).
                            labelOverride = if (multiSelectMode && cat.id == CategoryId.WILDCARD) "Mix" else null,
                            onClick = {
                                if (!cat.isReady) return@NewPickerTile
                                if (multiSelectMode) toggleSlug(slug)
                                else onCategorySelected(cat)
                            },
                            onLongClick = if (cat.isReady) {
                                { if (!multiSelectMode) multiSelectMode = true; toggleSlug(slug) }
                            } else null
                        )
                    }
                }
                // ── Curio / Knowledge — grouped, tap-to-open ──────────
                else -> {
                    val groups = if (mode == PickerMode.CURIO) curioModeGroups else knowledgeModeGroups
                    LazyVerticalGrid(
                        state = gridStates.getValue(mode),
                        columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groups.forEach { group ->
                            val lanes = group.lanes.mapNotNull { id ->
                                // Union of all + visible so hidden lanes drop
                                // out but coming-soon (not-yet-ready) still
                                // show their disabled tile.
                                CurioCategories.all.firstOrNull { it.id == id }
                                    ?.takeIf { it.id !in AppPreferences.hiddenCategoriesState }
                            }.ifEmpty { return@forEach }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    group.label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                )
                            }
                            gridItems(lanes) { cat ->
                                NewPickerTile(
                                    category = cat,
                                    comingSoon = !cat.isReady,
                                    selected = false,
                                    onClick = {
                                        if (!cat.isReady) return@NewPickerTile
                                        onCategorySelected(cat)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (multiSelectMode && mode == PickerMode.MIX) {
            // ── Mix row — only visible in multi-select mode ────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NewPrimaryCapsule(
                    label = if (selectedSlugs.isEmpty()) "Pick lanes" else "Mix · ${selectedSlugs.size}",
                    glyph = CurioIcons.Check,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (selectedSlugs.isEmpty()) return@NewPrimaryCapsule
                        // v318b — an unnamed multi-lane selection clears any
                        // previously applied mix's name from the Spin pill.
                        AppPreferences.setLastMixName(context, null)
                        val cats = selectedSlugs.mapNotNull { CurioCategories.byRouteSlug(it) }
                        if (cats.isNotEmpty()) onCategoriesMixed(cats)
                    }
                )
                // v3xx14 — Cancel as a FLOATING pill (raised capsule) instead
                // of a flat text button, so it reads as the mix row's escape
                // hatch beside the primary Mix capsule.
                Surface(
                    onClick = {
                        multiSelectMode = false
                        selectedSlugs = emptySet()
                    },
                    shape = RoundedCornerShape(50),
                    color = newPickerIdleFill(),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurioIcon(
                            name = CurioIcons.Close,
                            contentDescription = null,
                            size = 16.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * The idle (unselected) fill for the new picker's tiles, pills and panels.
 * LIGHT mode lifts toward the soft-cream page background (the classic
 * picker's cream-pill recipe) so the picker reads creamy, not dark tan;
 * DARK mode keeps the raised surface tone unchanged.
 */
@Composable
internal fun newPickerIdleFill(base: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color =
    lerp(
        base,
        if (isCurioDarkTheme()) base else curioPillLift(),
        0.82f
    )

/** Mode tab pill (Curio / Knowledge / Mix) — classic tint style selected. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicModeTab(
    label: String,
    glyph: String,
    selected: Boolean,
    accent: Color,
    accentInk: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) accent else newPickerIdleFill(),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                size = 15.dp,
                tint = if (selected) accentInk else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) accentInk else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Small uppercase-ish section label with an optional inline hint. */
@Composable
private fun NewSectionLabel(label: String, hint: String? = null, withRow: Boolean = true) {
    val content: @Composable () -> Unit = {
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
    if (withRow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    } else {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * One named mix CARD (grid cell) — a refined "mix stamp":
 *  - a LEAD-LANE cover plate tinted in the mix's first lane's color, so
 *    every mix has a color identity (no more identical grey plates);
 *  - the ExtraBold name + one-line teaser, with an "Active" label (in the
 *    lead lane's accent) on the mix currently applied to the Spin deck;
 *  - the lane-composition dots previewing the mix.
 * Tapping the card spins the mix; tap-and-hold opens the Edit/Delete
 * option pill (v3xx13 — no visible action buttons, no borders on the
 * card; the "Active" label alone marks the playing mix). Uniform 122dp
 * cells so the 2-col grid reads alike; the footer wears each lane's
 * category icon in its own accent chip (v3xx14 — no more washed-out
 * dots / "+N" text).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMixCard(
    mix: NamedMix,
    categories: List<CurioCategory>,
    active: Boolean = false,
    onApply: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val lanes = mix.laneIds.mapNotNull { id -> categories.firstOrNull { it.id == id } }
    val lead = lanes.firstOrNull()
    val leadAccent = lead?.themedAccent()
    Surface(
        shape = shape,
        color = newPickerIdleFill(),
        shadowElevation = 0.dp,
        modifier = modifier
            .height(122.dp)
            .combinedClickable(onClick = onApply, onLongClick = onLongClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Header: tinted lane cover + name (+ Active label) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            // v3xx14 — the lead-lane cover was washed out at
                            // 16% alpha; blend the accent in properly so the
                            // mix keeps a real color identity.
                            .background(
                                if (leadAccent != null) lerp(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    leadAccent,
                                    0.42f
                                )
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(
                            name = lead?.iconGlyph ?: CurioIcons.Tune,
                            contentDescription = null,
                            size = 22.dp,
                            tint = leadAccent ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mix.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (active && leadAccent != null) {
                                Text(
                                    "Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = leadAccent,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = mixTeaser(mix.laneIds, categories),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // ── Footer: lane-composition ICONS (v3xx14 — each saved
                // lane shows its real category icon in an accent chip, up to
                // 5 + "+N" for huge mixes; no Edit/Delete buttons here —
                // those live behind tap-and-hold) ───────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shown = if (lanes.size > 6) lanes.take(5) else lanes
                    shown.forEachIndexed { i, cat ->
                        val ink = cat.categoryInk()
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(ink.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                name = cat.iconGlyph,
                                contentDescription = null,
                                size = 12.dp,
                                tint = ink
                            )
                        }
                        if (i < shown.size - 1) Spacer(Modifier.width(5.dp))
                    }
                    if (lanes.size > 6) {
                        Text(
                            text = "+${lanes.size - 5}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A premium-minimal lane tile: flat cream-lift fill, NEUTRAL icon
 * (onSurfaceVariant) when idle. SELECTION wears the classic picker's
 * category-tint style — a SOLID category accent fill with its on-accent
 * ink (icon, label) — so picking a lane reads in the category's own
 * color story instead of a flat neutral. No check tick on the tile (user
 * call: the tint fill alone carries selection; the tick read as a pale
 * white dot in dark mode). The tile NEVER draws a border in EITHER theme
 * (v3xx13 — user call: no borders at all); pinned lanes read via the pin
 * badge only. Tap-and-hold surfaces the option pill / remove pill
 * (caller's onLongClick).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPickerTile(
    category: CurioCategory,
    selected: Boolean = false,
    pinned: Boolean = false,
    comingSoon: Boolean = false,
    labelOverride: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val isWildcard = category.id == CategoryId.WILDCARD
    // Selection = classic category-tint style: solid accent fill + on-accent
    // content. Idle = the cream-lift neutral (light mode stays creamy).
    val catAccent = category.themedAccent()
    val catInk = category.onAccent()
    val fill = if (selected) catAccent else newPickerIdleFill()
    val iconTint = if (selected) catInk else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (selected) catInk else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = shape,
        color = fill,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .then(
                if (comingSoon) Modifier
                else Modifier.combinedClickable(
                    enabled = true,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Pin badge — top-end (neutral).
            if (pinned && !selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.PushPin,
                        contentDescription = "Pinned",
                        size = 12.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (comingSoon) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Soon",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) catInk.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = category.iconGlyph,
                        contentDescription = null,
                        size = 22.dp,
                        tint = if (comingSoon) iconTint.copy(alpha = 0.45f) else iconTint
                    )
                }
                Text(
                    text = labelOverride
                        ?: when {
                            comingSoon -> "Coming soon"
                            isWildcard -> "Surprise mix"
                            else -> category.displayName
                        },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                    ),
                    color = when {
                        selected -> labelColor
                        comingSoon -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else -> labelColor
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A taller/wider pinned pill — neutral, with a leading glyph + name. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewPinnedPill(
    category: CurioCategory,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = newPickerIdleFill(),
        shadowElevation = 0.dp,
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Taller pill: 12dp vertical padding (was 9dp) + larger glyph.
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CurioIcon(
                name = category.iconGlyph,
                contentDescription = null,
                size = 17.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (category.id == CategoryId.WILDCARD) "Surprise mix" else category.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}/**
 * v318b — tap-and-hold actions are NO LONGER a dialog: a small floating
 * capsule MORPHS in (spring pop + fade) at the center, holding only
 * circular ICON buttons (no text). Tapping the scrim dismisses it.
 * internal — shared with the Browse screen's tap-and-hold overlays.
 */
@Composable
internal fun HoldActionsPill(
    actions: List<HoldAction>,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // Beautiful morph-in: springy pop + fade, one frame after the
        // overlay appears.
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        alpha.animateTo(1f, tween(140))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f))
            .combinedClickable(onClick = onDismiss)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = alpha.value
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions.forEach { action ->
                    Surface(
                        onClick = action.onClick,
                        shape = CircleShape,
                        color = action.background,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CurioIcon(
                                name = action.glyph,
                                contentDescription = action.description,
                                size = 19.dp,
                                tint = action.contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One circular icon action inside [HoldActionsPill]. */
internal class HoldAction(
    val glyph: String,
    val description: String,
    val background: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)

/**
 * The tap-and-hold option pill for a category — the OLD dialog-style
 * centered panel is gone; the same actions now live in the morphing
 * [HoldActionsPill] as circular icon buttons. Replaces the old
 * direct-pin-on-long-press behavior.
 */
@Composable
private fun CategoryOptionPill(
    category: CurioCategory,
    onDismiss: () -> Unit,
    onPinToggle: (() -> Unit)? = null,
    onSpin: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isPinned = remember { category.id in AppPreferences.getPinnedCategories(context) }
    val actions = buildList {
        if (onPinToggle != null) {
            add(
                HoldAction(
                    CurioIcons.PushPin,
                    if (isPinned) "Unpin" else "Pin",
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinToggle
                )
            )
        }
        if (onSpin != null && category.isReady) {
            add(
                HoldAction(
                    CurioIcons.PlayArrow,
                    "Spin ${category.displayName}",
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary,
                    onSpin
                )
            )
        }
        if (onRemove != null) {
            add(
                HoldAction(
                    CurioIcons.Delete,
                    "Remove from Continue exploring",
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                    onRemove
                )
            )
        }
    }
    if (actions.isEmpty()) return
    HoldActionsPill(actions = actions, onDismiss = onDismiss)
}

/**
 * v318b — tap-and-hold on a saved mix opens the same morphing pill: Edit +
 * Delete as circular icon buttons (no text, no dialog).
 */
@Composable
private fun MixOptionPill(
    name: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    HoldActionsPill(
        actions = listOf(
            HoldAction(
                CurioIcons.Edit,
                "Edit $name",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                onEdit
            ),
            HoldAction(
                CurioIcons.Delete,
                "Delete $name",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                onDelete
            )
        ),
        onDismiss = onDismiss
    )
}

/** An "+ Add" tile for the continue-exploring grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSuggestionTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = newPickerIdleFill(MaterialTheme.colorScheme.surfaceContainerLow),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .combinedClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.Add,
                        contentDescription = "Add category",
                        size = 22.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Add",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A bottom sheet to pick categories to add to suggestions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSuggestionSheet(
    categories: List<CurioCategory>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // LIVE reactive read (no remembered snapshot): toggling below writes
    // the reactive state, so the grid ticks update instantly — the old
    // remember froze the list until the sheet was closed and reopened.
    val userList = AppPreferences.pickerSuggestionsState
    val current = if (userList.isNotEmpty()) userList else AppPreferences.defaultSuggestions
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Text(
            "Add to Continue exploring",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        val wide = windowWidthSizeClass().isWide
        LazyVerticalGrid(
            columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            gridItems(categories.filter { it.isReady }) { cat ->
                val added = cat.id in current
                NewPickerTile(
                    category = cat,
                    selected = added,
                    onClick = {
                        // Toggle against the EFFECTIVE list and persist it
                        // explicitly: unchecking a default suggestion must
                        // actually remove it (the old add/remove write was a
                        // no-op while the user list was still empty — the
                        // defaults kept showing).
                        val eff = AppPreferences.pickerSuggestionsState
                            .ifEmpty { AppPreferences.defaultSuggestions }
                        AppPreferences.setPickerSuggestions(
                            context,
                            if (cat.id in eff) eff - cat.id else eff + cat.id
                        )
                    }
                )
            }
        }
    }
}

/** The primary "Surprise me" capsule — NEUTRAL primary fill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewPrimaryCapsule(
    label: String,
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 0.dp,
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                size = 20.dp,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** A secondary outline capsule (Show all / Show less). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewSecondaryOutline(
    label: String,
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = newPickerIdleFill(),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/** A round icon capsule — create, browse. NEUTRAL fill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewPickerCircle(
    glyph: String,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = newPickerIdleFill(),
        shadowElevation = 0.dp,
        modifier = modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = glyph,
                contentDescription = contentDescription,
                size = 22.dp,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * The mix editor bottom sheet — name field + multi-select category grid.
 * [editMix] != null opens it in edit mode (same stable id is preserved).
 * [onSave] is the caller's job (persist + apply).
 *
 * v27 — tiles show the classic category-tint selected state (solid accent
 * fill + on-accent ink) and the selection is IMMUTABLE state, so every tap
 * recomposes the grid and the Save label instantly (the old in-place
 * MutableSet toggle never recomposed until the sheet reopened).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixEditorSheet(
    washCat: CurioCategory,
    categories: List<CurioCategory>,
    editMix: NamedMix?,
    onDismiss: () -> Unit,
    onSave: (NamedMix) -> Unit
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape
    ) {
        var name by remember { mutableStateOf(editMix?.name ?: "") }
        // IMMUTABLE Set — every toggle writes a NEW set so the grid and the
        // Save label recompose instantly. The old MutableSet-toggle mutated
        // the remembered set in place and wrote the SAME instance back:
        // structural equality saw no change, so the selected-tile fill (and
        // a batch of stray selections that appeared later) only updated
        // after the editor was closed and reopened.
        var selected by remember {
            mutableStateOf<Set<CategoryId>>(
                editMix?.laneIds?.toSet() ?: emptySet()
            )
        }
        val wide = windowWidthSizeClass().isWide

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = if (editMix != null) "Edit mix" else "Create a mix",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                label = { Text("Mix name") },
                placeholder = { Text("e.g. Cosy night") }
            )

            // Multi-select grid — clear selection indication via NewPickerTile.
            LazyVerticalGrid(
                columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // weight fill = true: fill=false would measure the grid with
                // an infinite max height and crash (same issue as the sheet
                // pager). The grid fills the remaining sheet height.
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                gridItems(categories) { cat ->
                    val slug = cat.id
                    NewPickerTile(
                        category = cat,
                        selected = slug in selected,
                        onClick = {
                            selected = if (slug in selected) selected - slug else selected + slug
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = com.curio.app.ui.theme.curioDialogActionButtonColors()
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                NewPrimaryCapsule(
                    label = if (selected.isEmpty()) "Pick categories" else "Save · ${selected.size}",
                    glyph = CurioIcons.Check,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (selected.isEmpty()) return@NewPrimaryCapsule
                        val finalName = name.trim().ifBlank { "My mix" }
                        onSave(
                            NamedMix(
                                name = finalName,
                                laneIds = categories.filter { it.id in selected }.map { it.id },
                                createdAtMillis = editMix?.createdAtMillis
                                    ?: System.currentTimeMillis()
                            )
                        )
                    }
                )
            }
        }
    }
}

/** "Films · Books · Anime · +4" teaser for a mix row. */
internal fun mixTeaser(laneIds: List<CategoryId>, categories: List<CurioCategory>): String {
    val names = laneIds.mapNotNull { id ->
        categories.firstOrNull { it.id == id }?.displayName
    }
    return when {
        names.isEmpty() -> "No categories"
        names.size <= 3 -> names.joinToString(" · ")
        else -> names.take(3).joinToString(" · ") + " · +${laneIds.size - 3}"
    }
}

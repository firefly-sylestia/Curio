package com.curio.app.features.picker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.curio.app.data.CurioPassport
import kotlin.random.Random

/**
 * The NEW category picker — "Category Mix Studio".
 *
 * A premium-minimal, user-friendly replacement for the glass-pill picker:
 * a quick sheet (pinned lanes · named mixes · continue exploring) plus a
 * full Browse page ([NewCategoryPickerBrowseScreen]) with an in-page bottom nav.
 *
 * Style contract (user direction v3xx2):
 *  - **NO category accent colors** on tiles — fills, borders and icons use
 *    neutral theme roles (surfaceContainerHigh / onSurface / onSurfaceVariant)
 *    in BOTH dark and light mode. Selection reads via a neutral solid fill
 *    (secondaryContainer) + primary check, never a category accent.
 *  - **Tap-and-hold** a category shows an option pill (Pin/Unpin · Spin)
 *    instead of pinning directly. Same in the Pinned row and the Pins tab.
 *  - The sheet has **no close (cross) button** — swipe down or back to close.
 *  - A **HorizontalPager** swaps between the classic picker (page 0, default)
 *    and the new picker (page 1). The user can set which opens first
 *    (`AppPreferences.pickerDefaultPageState`). Both pages share the same
 *    bottom action row (Surprise me · Create mix · Browse).
 *  - **Your mixes**: a 2-column grid, max 5 visible + an Expand button; below
 *    it a "Continue exploring" section: recently-spun categories first, then
 *    curated "fun to explore" lanes up to 10 (user can add/remove).
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
    // Add-suggestion picker dialog.
    var showAddSuggestion by remember { mutableStateOf(false) }

    // Pager: page 0 = classic picker, page 1 = new picker. Default from prefs.
    val initialPage = AppPreferences.pickerDefaultPageState.coerceIn(0, 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    // Persist the user's chosen default page once they settle on one.
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != initialPage) {
            AppPreferences.setPickerDefaultPage(context, pagerState.currentPage)
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
                text = if (pagerState.currentPage == 0) "Classic deck · swipe for mixes"
                       else "Pins, mixes & a surprise",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            // ── Pager: classic (0) / new (1) ───────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                pageSpacing = 12.dp
            ) { page ->
                if (page == 0) {
                    // Classic-style multi-select grid. Self-contained so its
                    // LazyVerticalGrid fills the pager page cleanly (no nested
                    // weight conflict). Preset chips + Mix/Cancel row mirror
                    // the old classic picker behavior with the new neutral tiles.
                    ClassicPickerPage(
                        categories = categories,
                        onCategorySelected = onCategorySelected,
                        onCategoriesMixed = onCategoriesMixed
                    )
                } else {
                    NewPickerPage(
                        categories = categories,
                        deckIds = deckIds,
                        mixes = mixes,
                        mixesExpanded = mixesExpanded,
                        onMixesExpanded = { mixesExpanded = it },
                        pinnedList = pinnedList,
                        onPinnedToggle = pinnedToggle,
                        onSpinLane = onCategorySelected,
                        onApplyMix = { mix ->
                            val cats = mix.laneIds.mapNotNull { id ->
                                categories.firstOrNull { it.id == id }
                            }
                            if (cats.isNotEmpty()) onCategoriesMixed(cats)
                        },
                        onEditMix = { editMix = it; showEditor = true },
                        onDeleteMix = { deleteMix = it },
                        onNewMix = { editMix = null; showEditor = true },
                        onOptionTarget = { optionTarget = it },
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
                NewPrimaryCapsule(
                    label = "Surprise me",
                    glyph = CurioIcons.Shuffle,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val mix = surpriseMiniMix(categories)
                        if (mix.isNotEmpty()) onCategoriesMixed(mix)
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
    }

    // ── Mix editor + delete confirm + option pill + add-suggestion ────
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
                if (cats.isNotEmpty()) onCategoriesMixed(cats)
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
    // Tap-and-hold option pill.
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
    pinnedList: List<CurioCategory>,
    onPinnedToggle: (CategoryId) -> Unit,
    onSpinLane: (CurioCategory) -> Unit,
    onApplyMix: (NamedMix) -> Unit,
    onEditMix: (NamedMix) -> Unit,
    onDeleteMix: (NamedMix) -> Unit,
    onNewMix: () -> Unit,
    onOptionTarget: (CurioCategory) -> Unit,
    onAddSuggestion: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Pinned (taller/wider pills) ──────────────────────────────
        item(key = "pinned-label") {
            NewSectionLabel("Pinned", hint = if (pinnedList.isNotEmpty()) "hold for options" else null)
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

        // ── Your mixes (2-col grid, max 5 + expand) ───────────────────
        item(key = "mixes-label") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NewSectionLabel("Your mixes", withRow = false)
                if (mixes.isNotEmpty()) {
                    Surface(
                        onClick = { onNewMix() },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CurioIcon(CurioIcons.Add, null, size = 13.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "New",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        if (mixes.isEmpty()) {
            item(key = "mixes-empty") {
                Text(
                    "No mixes yet — tap New to build one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        } else {
            val visible = if (mixesExpanded) mixes else mixes.take(5)
            val showExpand = !mixesExpanded && mixes.size > 5
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
                                    onMore = { onEditMix(mix) },
                                    onDelete = { onDeleteMix(mix) },
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
                    } else if (mixesExpanded && mixes.size > 5) {
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
            NewSectionLabel("Continue exploring", hint = "hold to remove")
        }
        item(key = "explore-grid") {
            ContinueExploringSection(
                categories = categories,
                onSpinLane = onSpinLane,
                onOptionTarget = onOptionTarget,
                onAdd = onAddSuggestion
            )
        }
    }
}

/**
 * "Continue exploring": the user's most-spun categories (from CurioPassport)
 * first, then curated "fun to explore" lanes up to 10 total. The user can
 * remove a lane (tap-and-hold) and add from the full list.
 */
@Composable
private fun ContinueExploringSection(
    categories: List<CurioCategory>,
    onSpinLane: (CurioCategory) -> Unit,
    onOptionTarget: (CurioCategory) -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    val visibleIds = remember { categories.map { it.id }.toSet() }
    // Most-spun (CurioPassport spin counts), desc, ready + visible only.
    val mostUsed = remember {
        CurioPassport.allProgress(context).entries
            .filter { it.key in visibleIds && it.value.spins > 0 }
            .sortedByDescending { it.value.spins }
            .map { it.key }
    }
    val curated = remember {
        val user = AppPreferences.pickerSuggestionsState
        if (user.isNotEmpty()) user else AppPreferences.defaultSuggestions
    }
    val combined = remember(mostUsed, curated) {
        val seen = mutableSetOf<CategoryId>()
        val out = mutableListOf<CategoryId>()
        mostUsed.forEach { if (seen.add(it)) out.add(it) }
        curated.forEach { if (seen.add(it) && it in visibleIds) out.add(it) }
        out.take(10)
    }
    val wide = windowWidthSizeClass().isWide
    LazyVerticalGrid(
        columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        gridItems(combined) { id ->
            val cat = categories.firstOrNull { it.id == id } ?: return@gridItems
            NewPickerTile(
                category = cat,
                onClick = { onSpinLane(cat) },
                onLongClick = { onOptionTarget(cat) }
            )
        }
        // An "+ Add" tile.
        item(key = "add-suggestion") {
            AddSuggestionTile(onClick = onAdd)
        }
    }
}

/**
 * The classic-style picker page (pager page 0): preset chips + a neutral
 * multi-select grid + Mix/Cancel row. Tap opens a lane (single), hold enters
 * multi-select to build a mix. Self-contained so its grid fills the pager page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicPickerPage(
    categories: List<CurioCategory>,
    onCategorySelected: (CurioCategory) -> Unit,
    onCategoriesMixed: (List<CurioCategory>) -> Unit
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
    // Seed multi-select + selection from the persisted deck (a saved mix
    // reopens with every lane pre-ticked).
    val persistedVisible = remember {
        AppPreferences.getLastSpinCategories(context)
            .mapNotNull { id -> categories.firstOrNull { it.id == id } }
    }
    var multiSelectMode by remember { mutableStateOf(persistedVisible.size > 1) }
    var selectedSlugs by remember {
        mutableStateOf(persistedVisible.map { it.id.routeSlug }.toSet())
    }
    val toggleSlug = { slug: String ->
        selectedSlugs = if (slug in selectedSlugs) selectedSlugs - slug else selectedSlugs + slug
    }
    val wide = windowWidthSizeClass().isWide

    Column(modifier = Modifier.fillMaxSize()) {
        // Preset chips row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            deckPresets.forEach { preset ->
                val active = if (preset.clearAll) {
                    multiSelectMode && selectedSlugs.isEmpty()
                } else {
                    multiSelectMode &&
                        preset.lanes(categories).all { it.id.routeSlug in selectedSlugs }
                }
                ClassicPresetPill(
                    label = preset.label,
                    glyph = preset.glyph,
                    selected = active,
                    onClick = {
                        if (preset.clearAll) {
                            multiSelectMode = true
                            selectedSlugs = emptySet()
                        } else {
                            val lanes = preset.lanes(categories)
                            if (lanes.isNotEmpty()) {
                                val laneSlugs = lanes.map { it.id.routeSlug }.toSet()
                                multiSelectMode = true
                                selectedSlugs = if (active) selectedSlugs - laneSlugs else laneSlugs
                            }
                        }
                    }
                )
            }
        }

        // Grid — fills the remaining height.
        LazyVerticalGrid(
            columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
            contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            gridItems(sortedCategories) { cat ->
                val slug = cat.id.routeSlug
                NewPickerTile(
                    category = cat,
                    comingSoon = !cat.isReady,
                    selected = multiSelectMode && slug in selectedSlugs,
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

        if (multiSelectMode) {
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
                        val cats = selectedSlugs.mapNotNull { CurioCategories.byRouteSlug(it) }
                        if (cats.isNotEmpty()) onCategoriesMixed(cats)
                    }
                )
                TextButton(onClick = {
                    multiSelectMode = false
                    selectedSlugs = emptySet()
                }) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

/** A neutral preset pill for the classic page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicPresetPill(
    label: String,
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurface
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
 * One named mix CARD (grid cell): name + lane teaser, a Spin pill, and a
 * 3-dot menu (Edit / Delete). [active] marks the mix currently applied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMixCard(
    mix: NamedMix,
    categories: List<CurioCategory>,
    active: Boolean,
    onApply: () -> Unit,
    onMore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp,
        modifier = modifier
            .combinedClickable(
                onClick = onApply,
                onLongClick = { menuOpen = true }
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = mix.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = { menuOpen = true },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(
                        modifier = Modifier.size(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(
                            name = CurioIcons.MoreVert,
                            contentDescription = "Mix options",
                            size = 16.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DropdownMenuSurface(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    onEdit = { menuOpen = false; onMore() },
                    onDelete = { menuOpen = false; onDelete() }
                )
            }
            Text(
                text = mixTeaser(mix.laneIds, categories),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onApply,
                    shape = RoundedCornerShape(50),
                    color = if (active) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        if (active) "Spinning" else "Spin",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/** A tiny dropdown-ish surface for Edit/Delete (avoids M3 DropdownMenu quirks). */
@Composable
private fun DropdownMenuSurface(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AnimatedVisibility(visible = expanded, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 4.dp,
            modifier = Modifier.padding(top = 36.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Surface(
                    onClick = { onDismiss(); onEdit() },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Edit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Surface(
                    onClick = { onDismiss(); onDelete() },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * A premium-minimal NEUTRAL lane tile: flat soft fill, NEUTRAL icon
 * (onSurfaceVariant), no category accent anywhere. Selection (when used
 * by the mix editor) reads via a solid secondaryContainer fill + primary
 * check. Tap-and-hold surfaces the option pill (caller's onLongClick).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPickerTile(
    category: CurioCategory,
    selected: Boolean = false,
    pinned: Boolean = false,
    comingSoon: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    val isWildcard = category.id == CategoryId.WILDCARD
    // NEUTRAL fills + icons — no category accent.
    val fill = if (selected) MaterialTheme.colorScheme.secondaryContainer
               else MaterialTheme.colorScheme.surfaceContainerHigh
    val iconTint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                   else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                     else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = shape,
        color = fill,
        shadowElevation = 0.dp,
        modifier = Modifier
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                // NEUTRAL selection ring (primary), never a category accent.
                .border(
                    width = if (selected) 2.dp else if (pinned) 1.5.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = shape
                )
        ) {
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
                        .background(if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
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
                    text = when {
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
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 6.dp, end = 6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.Check,
                        contentDescription = "Selected",
                        size = 12.dp,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
}

/**
 * The tap-and-hold option pill — a centered overlay with Pin/Unpin + Spin.
 * Replaces the old direct-pin-on-long-press behavior.
 */
@Composable
private fun CategoryOptionPill(
    category: CurioCategory,
    onDismiss: () -> Unit,
    onPinToggle: () -> Unit,
    onSpin: () -> Unit
) {
    val context = LocalContext.current
    val isPinned = remember { category.id in AppPreferences.getPinnedCategories(context) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
            .combinedClickable(onClick = onDismiss)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 40.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (category.id == CategoryId.WILDCARD) "Surprise mix" else category.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onPinToggle,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.PushPin,
                                contentDescription = null,
                                size = 16.dp,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                if (isPinned) "Unpin" else "Pin",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (category.isReady) {
                        Surface(
                            onClick = onSpin,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Casino,
                                    contentDescription = null,
                                    size = 16.dp,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    "Spin",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** An "+ Add" tile for the continue-exploring grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSuggestionTile(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .combinedClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = shape
                ),
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
    val current = remember { AppPreferences.getPickerSuggestions(context).ifEmpty { AppPreferences.defaultSuggestions } }
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
                        if (added) AppPreferences.removePickerSuggestion(context, cat.id)
                        else AppPreferences.addPickerSuggestion(context, cat.id)
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
 * v3xx2 — tiles now show a CLEAR neutral selected state (solid
 * secondaryContainer + primary check) so tapping a category visibly toggles.
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
        var selected by remember {
            mutableStateOf<MutableSet<CategoryId>>(
                editMix?.laneIds?.toMutableSet() ?: mutableSetOf<CategoryId>()
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
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                gridItems(categories) { cat ->
                    val slug = cat.id
                    NewPickerTile(
                        category = cat,
                        selected = slug in selected,
                        onClick = {
                            selected = selected.apply {
                                if (!add(slug)) remove(slug)
                            }
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

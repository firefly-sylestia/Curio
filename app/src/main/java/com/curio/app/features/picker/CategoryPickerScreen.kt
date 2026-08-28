package com.curio.app.features.picker

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.TopicJsonLoader
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioCategoryCard
import com.curio.app.ui.components.MorphEntrance

import com.curio.app.ui.components.curioButtonColors
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.curioGlassEdge
import com.curio.app.ui.components.curioInnerGlow
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.curioPillLift
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.themedButtonFill
import com.curio.app.ui.theme.themedButtonInk
import kotlinx.coroutines.launch

/**
 * v27l — display rank for the expanded (new) lanes on the picker's New page.
 * Groups related fields together: life sciences, chemistry, earth & space,
 * math, technology, then human sciences. Unknown ids fall back to a high
 * rank so they sort after every ranked lane (then alpha by display name).
 */
/**
 * v44 — process-scoped draft of the category picker. The selection, the
 * multi-select mode, the Original/New page and BOTH grids' scroll offsets
 * are mirrored here live, so leaving the picker (back / swipe-down) and
 * reopening restores exactly where you were — selection, preset mix and
 * scroll position all survive navigation. The draft lives for the process
 * ("kept saved until the restart"): an app restart clears it naturally, and
 * committing a mix (or tapping a lane open) clears it so the next open
 * shows the persisted deck fresh.
 */
object CategoryPickerDraft {
    var selected: List<String>? = null
    var multiSelect: Boolean = false

    fun clear() {
        selected = null
        multiSelect = false
    }
}

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
    // v301 — Single flat grid, no pager. Wildcard first, then alphabetical.
    val sortedCategories = remember(categories) {
        val wildcard = categories.filter { it.id == CategoryId.WILDCARD }
        val rest = categories.filter { it.id != CategoryId.WILDCARD }
            .sortedBy { it.displayName.lowercase() }
        wildcard + rest
    }
    // v26 — reopen with the persisted deck: a mixed selection comes back in
    // multi-select with every lane ticked, so the user can SEE and CHANGE
    // the mix instead of it collapsing to the single first category. Hidden
    // lanes are filtered out so they never show as pre-selected.
    val persistedVisible = remember {
        AppPreferences.getLastSpinCategories(context)
            .mapNotNull { id -> categories.firstOrNull { it.id == id } }
    }
    // v301 — single grid state (no pager).
    val gridState = rememberLazyGridState()
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
    // v44 — seed from the process draft when one is staged (selection + mode
    // restored), else from the persisted deck as before.
    var selectedSlugs by rememberSaveable {
        mutableStateOf(
            draft.selected ?: persistedVisible.map { it.id.routeSlug }
        )
    }
    var multiSelectMode by rememberSaveable {
        mutableStateOf(if (draft.selected != null) draft.multiSelect else persistedVisible.size > 1)
    }

    val toggleSlug = { slug: String ->
        selectedSlugs = if (slug in selectedSlugs) selectedSlugs - slug else selectedSlugs + slug
    }

    // v27k — total topics across the ticked lanes (the Mix button shows the
    // pool size, not the lane count).
    var selectedTopicCount by remember { mutableStateOf(0) }
    LaunchedEffect(selectedSlugs) {
        val ids = selectedSlugs.mapNotNull { CurioCategories.byRouteSlug(it)?.id }
        selectedTopicCount = if (CategoryId.WILDCARD in ids) {
            TopicJsonLoader.countCanonicalTopics()
        } else {
            ids.sumOf { TopicJsonLoader.countFor(it) }
        }
    }

    val scope = rememberCoroutineScope()

    // v44 — keep the draft live: every selection/mode/page/scroll change is
    // mirrored into [CategoryPickerDraft] so leaving and reopening the
    // picker restores exactly where you were (selection, preset mix, page
    // and both grids' scroll offsets).
    LaunchedEffect(selectedSlugs, multiSelectMode) {
        draft.selected = selectedSlugs
        draft.multiSelect = multiSelectMode
    }


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
                // v28 — the preset row hugs the Original/New tabs.
                // v90 — unhugged a touch (4/1 → 8/2) so the fuller preset
                // chips aren't squished against the tabs.
                .padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            deckPresets.forEach { preset ->
                val active = if (preset.clearAll) {
                    multiSelectMode && selectedSlugs.isEmpty()
                } else {
                    multiSelectMode &&
                        preset.lanes(categories).all { it.id.routeSlug in selectedSlugs }
                }
                PickerPresetChip(
                    label = preset.label,
                    glyph = preset.glyph,
                    selected = active,
                    // v83 — dynamic category accent + ink (icon rides it).
                    accent = washCat.themedAccent(),
                    accentInk = washCat.onAccent(),
                    onClick = {
                        if (preset.clearAll) {
                            multiSelectMode = true
                            selectedSlugs = emptyList()
                        } else {
                            val lanes = preset.lanes(categories)
                            if (lanes.isNotEmpty()) {
                                val laneSlugs = lanes.map { it.id.routeSlug }
                                multiSelectMode = true
                                // v27t — presets toggle: tapping the active
                                // preset again UNDOES it (deselects its
                                // lanes); a different preset replaces the
                                // whole selection (remove everything, add it).
                                selectedSlugs =
                                    if (active) selectedSlugs - laneSlugs else laneSlugs
                            }
                        }
                    }
                )
            }
        }

        // v301 — single flat grid, no tabs. Hint row:
        Text(
            text = if (multiSelectMode) "Tap to toggle decks" else "Tap opens · hold to mix",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )

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
            // v83 — no-overshoot entrance: the elastic spring's ~5%
            // overshoot read as a brief "more elevated" card shadow flash
            // before settling on the category page.
            MorphEntrance(bouncy = false) {
                // v301 — Single flat grid, 3 columns, Wildcard first.
                LazyVerticalGrid(
                    state = gridState,
                    columns = if (wide) GridCells.Adaptive(minSize = 140.dp) else GridCells.Fixed(3),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedCategories) { cat ->
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
                                        onCategorySelected(cat)
                                    }
                                }
                            },
                            onLongClick = if (comingSoon) null else {
                                {
                                    if (!multiSelectMode) {
                                        multiSelectMode = true
                                    }
                                    toggleSlug(slug)
                                }
                            }
                        )
                    }
                }
            }        }

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
                        // The mix is committed — drop the draft so the next
                        // picker open shows the persisted deck fresh.
                        CategoryPickerDraft.clear()
                        navController.navigateToTab(CurioRoutes.SPIN)
                    },
                    enabled = selectedSlugs.isNotEmpty(),
                    shape = mixShape,
                    colors = curioButtonColors(
                        // v83 — the Mix CTA wears the wash category's
                        // theme-aware fill + ink (deep in dark, never the
                        // pale rose with near-black content).
                        containerColor = washCat.themedButtonFill(),
                        contentColor = washCat.themedButtonInk()
                    ),
                    modifier = Modifier
                        .weight(1f)
                        // v28 — dark mode: soft glow + top-lit shine, no
                        // border rings on the black AMOLED plate.
                        // v114 — shape-matched glass edge (the old
                        // categoryEdgeShine band peeked past the capsule).
                        .curioDarkGlow(2.dp, mixShape)
                        .curioGlassEdge(mixShape)
                        .curioInnerGlow(mixShape, washCat.themedAccent(), strength = 0.12f)
                ) {
                    CurioIcon(CurioIcons.Check, null, size = 18.dp)
                    Text(
                        text = if (selectedSlugs.isEmpty()) "Mix" else "Mix · $selectedTopicCount",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                TextButton(
                    onClick = {
                        // Exit multi-select mode; selection is discarded (and
                        // the draft cleared so the next open starts fresh).
                        multiSelectMode = false
                        selectedSlugs = emptyList()
                        CategoryPickerDraft.clear()
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

/**
 * Small page-tab pill for the Original / New pager.
 *
 * v83 — theme-aware + DYNAMIC colors: callers drive the selected fill and
 * content inks from their context — the category banner's ink/fill when the
 * tabs ride a tear hero, the category accent when they sit on the wash —
 * instead of the fixed rose primary (which glared pale in dark mode).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerPageTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    accentInk: Color = MaterialTheme.colorScheme.onPrimary,
    idleInk: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    idleFill: Color? = null
) {
    val shape = RoundedCornerShape(50)
    // v86 — DARK-aware default idle fill: the old default lifted toward
    // curioPillLift() (WHITE in dark) → near-white idle tabs with light-
    // grey text on the black sheet (washed out). Dark now stays a dark
    // raised glass; light keeps the cream lift exactly as before.
    val resolvedIdleFill = idleFill ?: lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        if (isCurioDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else curioPillLift(),
        0.82f
    )
    Surface(
        onClick = onClick,
        shape = shape,
        // v27q — selection reads as a SOLID accent fill with its content ink.
        // v33 — the UNselected tab is a proper raised pill that stands off
        // the category wash: it lifts clearly toward the page background
        // (cream in light, a lighter glass in dark) instead of the flat
        // surfaceContainerHigh that blended into the tinted picker.
        // v38 — the light lift rises to 0.82 (neutral cream) so the tabs
        // separate from the pale pastel wash.
        // v98 — elevation flattened 3 → 2dp (the v27q selectable-chip
        // standard) so the halo no longer reads as a shadow above the pill.
        color = if (selected) accent else resolvedIdleFill,
        shadowElevation = 2.dp,
        modifier = Modifier
            // v28 — soft glow + top-lit shine.
            // v114 — the dark-mode edge must match the filter-chip pill
            // treatment: `categoryEdgeShine` painted a full-width band that
            // peeked past the capsule's rounded ends — `curioGlassEdge` +
            // `curioInnerGlow` hug the pill shape (same family as the Spin
            // filter chips / reveal explore pills).
            .curioDarkGlow(2.dp, shape)
            .curioGlassEdge(shape)
            .curioInnerGlow(shape, accent, strength = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = if (selected) accentInk else idleInk
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accentInk.copy(alpha = 0.8f)
                        else idleInk.copy(alpha = 0.7f)
            )
        }
    }
}

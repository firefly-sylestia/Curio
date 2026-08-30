package com.curio.app.features.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * v1 — the redesigned picker's mode tabs. The old single flat deck is
 * split into three ways to choose:
 *
 *  - **Curio** — a casual, culture-first deck (music, screen, stories, art,
 *    comics, food) that needs zero background knowledge; grouped and
 *    playful. Tap a lane to open it immediately.
 *  - **Knowledge** — the discovery/learning lanes (sciences, history, maths,
 *    technology, the human mind), grouped by theme; tap to open.
 *  - **Mix** — everything, with multi-select + the Mix button (the classic
 *    picker, Wildcard included).
 *
 * The lane sets are DECLARATIVE lists of [CategoryId] (resolved to visible
 * categories at render), so future additions — like branch/daily discovery
 * paths — are just more groups or a new mode added to the enum without
 * touching the grid renderer.
 */
// Glyphs must exist in the bundled font subset (CurioIcon resolves them
// by name); AutoAwesome (sparkle) / Science (flask) / Check are all in use.
enum class PickerMode(val label: String, val glyph: String) {
    CURIO("Curio", CurioIcons.AutoAwesome),
    KNOWLEDGE("Knowledge", CurioIcons.Science),
    MIX("Mix", CurioIcons.Check)
}

/** One labelled row-group of lanes inside a Curio/Knowledge mode. */
private data class PickerGroup(val label: String, val lanes: List<CategoryId>)

/**
 * The Curio/Knowledge mode lanes, grouped by theme. Mix uses everything +
 * Wildcard (declared separately in [mixModeLaneIds]). Kept as the single
 * source of truth so the picker grid only iterates these groups.
 */
private val curioModeGroups = listOf(
    PickerGroup("Music", listOf(CategoryId.ARTISTS, CategoryId.ALBUMS, CategoryId.SONGS)),
    PickerGroup("On screen", listOf(CategoryId.DIRECTORS, CategoryId.FILMS, CategoryId.ANIMATED_MOVIES, CategoryId.SERIES)),
    PickerGroup("Stories", listOf(CategoryId.AUTHORS, CategoryId.BOOKS, CategoryId.MYTHOLOGY, CategoryId.QUOTES)),
    PickerGroup("Art & artists", listOf(CategoryId.PAINTERS, CategoryId.ARTWORKS)),
    PickerGroup("Comics & anime", listOf(CategoryId.ANIME, CategoryId.MANGA, CategoryId.MANHWA)),
    PickerGroup("Play & taste", listOf(CategoryId.GAMES, CategoryId.SPORTS, CategoryId.FOOD, CategoryId.INTERNET))
)

private val knowledgeModeGroups = listOf(
    PickerGroup("Life sciences", listOf(CategoryId.BIOLOGY, CategoryId.ANIMALS, CategoryId.PLANTS, CategoryId.MEDICINE)),
    PickerGroup("Physical sciences", listOf(CategoryId.CHEMISTRY, CategoryId.GEOLOGY, CategoryId.ASTRONOMY, CategoryId.SCIENTISTS)),
    PickerGroup("How things work", listOf(CategoryId.TECHNOLOGIES, CategoryId.ENGINEERING, CategoryId.DISCOVERIES)),
    PickerGroup("The human mind", listOf(CategoryId.PSYCHOLOGY, CategoryId.MATHEMATICS, CategoryId.ECONOMICS, CategoryId.LANGUAGE)),
    PickerGroup("Our world", listOf(CategoryId.HISTORY, CategoryId.OCEANS))
)

/** The Mix mode shows every lane plus Wildcard (the surprise-mix lane). */
private val mixModeLaneIds: List<CategoryId> =
    CategoryId.values().toList()

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
    val categories = CurioCategories.visible
    val washCat = remember {
        val id = AppPreferences.getLastSpinCategories(context).firstOrNull()
            ?: AppPreferences.getLastSpinCategory(context)
        CurioCategories.byId(id)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { PetLandmarks.noteSheet("picker", true) }
    DisposableEffect(Unit) {
        onDispose { PetLandmarks.noteSheet("picker", false) }
    }

    ModalBottomSheet(
        onDismissRequest = { navController.popBackStack() },
        sheetState = sheetState,
        containerColor = washCat.categoryBackgroundWash(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        CategoryPickerContent(
            washCat = washCat,
            categories = categories,
            onDismiss = { navController.popBackStack() },
            onCategorySelected = { cat ->
                AppPreferences.setLastSpinCategories(context, listOf(cat.id))
                navController.navigateToTab(CurioRoutes.SPIN)
            },
            onCategoriesMixed = { cats ->
                if (cats.isEmpty()) {
                    val single = AppPreferences.getLastSpinCategory(context)
                    AppPreferences.setLastSpinCategories(context, listOf(single))
                } else {
                    AppPreferences.setLastSpinCategories(context, cats.map { it.id })
                }
                navController.navigateToTab(CurioRoutes.SPIN)
            }
        )
    }
}

/**
 * The redesigned picker's inner content — the Curio/Knowledge/Mix mode tabs,
 * preset chips, icon-tile grid and Mix/Cancel row. Shared by the full-screen
 * [CategoryPickerScreen] (wrapped in its own ModalBottomSheet) and the Spin
 * page's inline sheet (shown above the shuffle deck).
 *
 * @param washCat the category whose tint wash the picker wears.
 * @param categories the visible category list (for preset resolution).
 * @param onCategorySelected fired on tap-to-open (Curio/Knowledge mode, or
 *   Mix single-tap). The caller persists the selection and dismisses.
 * @param onCategoriesMixed fired on Mix (empty = cancelled mix → revert to
 *   the last single category). The caller persists and dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerContent(
    washCat: CurioCategory,
    categories: List<CurioCategory>,
    onCategorySelected: (CurioCategory) -> Unit,
    onCategoriesMixed: (List<CurioCategory>) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    // v301 — Single flat grid. Uses ALL categories (not just visible) so
    // Coming Soon tiles show for unready lanes. Hidden lanes filtered out.
    val allUnhidden = remember {
        CurioCategories.all.filter { it.id !in AppPreferences.hiddenCategoriesState }
    }
    val sortedCategories = remember(allUnhidden) {
        val wildcard = allUnhidden.filter { it.id == CategoryId.WILDCARD }
        val rest = allUnhidden.filter { it.id != CategoryId.WILDCARD }
            .sortedBy { it.displayName.lowercase() }
        wildcard + rest
    }
    val draft = CategoryPickerDraft
    val persistedVisible = remember {
        AppPreferences.getLastSpinCategories(context)
            .mapNotNull { id -> categories.firstOrNull { it.id == id } }
    }
    val gridState = rememberLazyGridState()
    val wide = windowWidthSizeClass().isWide
    var selectedSlugs by rememberSaveable {
        mutableStateOf(
            draft.selected ?: persistedVisible.map { it.id.routeSlug }
        )
    }
    var multiSelectMode by rememberSaveable {
        mutableStateOf(if (draft.selected != null) draft.multiSelect else persistedVisible.size > 1)
    }
    var modeName by rememberSaveable { mutableStateOf(PickerMode.MIX.name) }
    val mode = runCatching { PickerMode.valueOf(modeName) }.getOrDefault(PickerMode.MIX)

    val toggleSlug = { slug: String ->
        selectedSlugs = if (slug in selectedSlugs) selectedSlugs - slug else selectedSlugs + slug
    }

    var selectedTopicCount by remember { mutableStateOf(0) }
    LaunchedEffect(selectedSlugs) {
        val ids = selectedSlugs.mapNotNull { CurioCategories.byRouteSlug(it)?.id }
        selectedTopicCount = if (CategoryId.WILDCARD in ids) {
            TopicJsonLoader.countCanonicalTopics()
        } else {
            ids.sumOf { TopicJsonLoader.countFor(it) }
        }
    }

    LaunchedEffect(selectedSlugs, multiSelectMode) {
        draft.selected = selectedSlugs
        draft.multiSelect = multiSelectMode
    }

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
            CurioBackButton(onClick = onDismiss)
            Text(
                text = "What are we exploring?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── v1 — Mode tabs: Curio / Knowledge / Mix. The redesigned picker
        //    splits the old single flat deck into two curated tap-to-open
        //    modes plus the classic Mix (multi-select + presets). The mode
        //    drives which lanes / groups show below.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PickerMode.entries.forEach { m ->
                PickerPresetChip(
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
                            selectedSlugs = emptyList()
                            CategoryPickerDraft.clear()
                        }
                    }
                )
            }
        }

        // ── Quick-mix preset chips — only in Mix mode (the original row) ──
        if (mode == PickerMode.MIX) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                // v28 — the preset row hugs the tabs.
                // v90 — unhugged a touch (4/1 → 8/2) so the fuller preset
                // chips aren't squished against the tabs.
                .padding(top = 6.dp, bottom = 2.dp),
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
            }        }
        }

        // Hint row — mode aware.
        Text(
            text = when {
                mode == PickerMode.MIX && multiSelectMode -> "Tap to toggle decks"
                mode == PickerMode.MIX -> "Tap opens · hold to mix"
                mode == PickerMode.CURIO -> "A relaxed, culture-first deck — tap any lane to explore"
                else -> "Dig into knowledge — tap any lane to explore"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 2,
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
                when (mode) {
                    // ── Mix — classic full deck, everything + Wildcard ──
                    PickerMode.MIX -> LazyVerticalGrid(
                        state = gridState,
                        columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedCategories) { cat ->
                            val slug = cat.id.routeSlug
                            PickerIconTile(
                                category = cat,
                                comingSoon = !cat.isReady,
                                selected = multiSelectMode && slug in selectedSlugs,
                                onClick = {
                                    if (!cat.isReady) return@PickerIconTile
                                    if (multiSelectMode) toggleSlug(slug)
                                    else onCategorySelected(cat)
                                },
                                onLongClick = if (cat.isReady) {
                                    {
                                        if (!multiSelectMode) multiSelectMode = true
                                        toggleSlug(slug)
                                    }
                                } else null
                            )
                        }
                    }
                    // ── Curio / Knowledge — grouped, tap-to-open ───────
                    else -> {
                        val groups = if (mode == PickerMode.CURIO) curioModeGroups else knowledgeModeGroups
                        LazyVerticalGrid(
                            state = gridState,
                            columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
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
                                items(lanes) { cat ->
                                    PickerIconTile(
                                        category = cat,
                                        comingSoon = !cat.isReady,
                                        selected = false,
                                        onClick = {
                                            if (!cat.isReady) return@PickerIconTile
                                            onCategorySelected(cat)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }        }

        if (multiSelectMode && mode == PickerMode.MIX) {
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
                        val cats = selectedSlugs.mapNotNull { CurioCategories.byRouteSlug(it) }
                        if (cats.isEmpty()) return@Button
                        CategoryPickerDraft.clear()
                        onCategoriesMixed(cats)
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

/**
 * v1 — the redesigned picker's compact lane tile: a small rounded square
 * with the lane's glyph over its name, on a THEME-NEUTRAL surface (no
 * per-category color wash / gradient) so the grid reads quiet and the icon
 * leads. Used by every mode (Curio / Knowledge / Mix). Multi-select (Mix)
 * shows the selected check state; tap opens the lane.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerIconTile(
    category: CurioCategory,
    comingSoon: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val idleFill = lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        if (isCurioDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else curioPillLift(),
        0.82f
    )
    val isWildcard = category.id == CategoryId.WILDCARD
    Surface(
        shape = shape,
        color = when {
            selected -> MaterialTheme.colorScheme.secondary
            else -> idleFill
        },
        shadowElevation = if (comingSoon) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            // v28 — soft glow + top-lit shine (only tappable tiles).
            .then(if (comingSoon) Modifier else Modifier.curioDarkGlow(2.dp, shape))
            .then(if (comingSoon) Modifier else Modifier.curioGlassEdge(shape))
            // Tap opens; long-press enters multi-select (Mix mode). The M3
            // Surface in this version has no onLongClick overload, so the
            // press handling rides a combinedClickable (ripple + disabled
            // state for coming-soon lanes).
            .then(
                if (comingSoon) Modifier
                else Modifier.combinedClickable(
                    enabled = !comingSoon,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                selected -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.18f)
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = category.iconGlyph,
                        contentDescription = null,
                        size = 21.dp,
                        tint = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selected) {
                        CurioIcon(
                            name = CurioIcons.Check,
                            contentDescription = "Selected",
                            size = 12.dp,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
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
                        selected -> MaterialTheme.colorScheme.onSecondary
                        comingSoon -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

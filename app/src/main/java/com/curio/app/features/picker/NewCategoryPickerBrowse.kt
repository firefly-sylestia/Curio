package com.curio.app.features.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.curio.app.data.NamedMix
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.curioGlassEdge
import com.curio.app.ui.components.isLiquidGlassPillsActive
import com.curio.app.ui.components.liquidGlassCapsule
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.themedButtonFill
import com.curio.app.ui.theme.themedButtonInk
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * The full "Browse" page of the new category picker — route CurioRoutes.PICKER.
 *
 * User direction: "sheet + full browse page where you can customise the
 * sheet, and use bottom nav style". So this page is a full screen with an
 * in-page BOTTOM NAV with three tabs:
 *   - Browse — every category grid (tap to spin, long-press to pin)
 *   - Mixes — your saved named mixes (apply / edit / delete / new)
 *   - Pins — the pinned quick-access lanes (unpin / tap to spin)
 *
 * Changes made here customize the quick sheet (same prefs).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBrowseScreen(navController: NavController) {
    val context = LocalContext.current
    seedStarterMixes(context)
    // Local glass capture: records ONLY the tab content area (the grid/
    // list). The bottom-nav capsules below are SIBLINGS of that subtree, so
    // they never sample their own pixels (v228 self-capture rule).
    val contentGlassBackdrop = rememberLayerBackdrop()

    val washCat = remember {
        val id = AppPreferences.getLastSpinCategories(context).firstOrNull()
            ?: AppPreferences.getLastSpinCategory(context)
        CurioCategories.byId(id)
    }
    val deckIds = remember { AppPreferences.getLastSpinCategories(context).toSet() }

    LaunchedEffect(Unit) { PetLandmarks.noteSheet("picker", true) }
    DisposableEffect(Unit) {
        onDispose { PetLandmarks.noteSheet("picker", false) }
    }

    var tabName by rememberSaveable { mutableStateOf(BrowseTab.BROWSE.name) }
    val tab = runCatching { BrowseTab.valueOf(tabName) }.getOrDefault(BrowseTab.BROWSE)

    // Mix editor + delete confirm live at page scope so every tab can trigger them.
    var showEditor by remember { mutableStateOf(false) }
    var editMix by remember { mutableStateOf<NamedMix?>(null) }
    var deleteMix by remember { mutableStateOf<NamedMix?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(washCat.categoryBackgroundWash())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .align(Alignment.TopCenter)
        ) {
            // ── Top bar: back + title (nav-bar style) ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NewPickerCircle(
                    glyph = CurioIcons.ChevronLeft,
                    contentDescription = "Back",
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(44.dp)
                )
                Column {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = tab.tagline,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Tab content ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .layerBackdrop(contentGlassBackdrop)
            ) {
                when (tab) {
                    BrowseTab.BROWSE -> BrowseTabContent(
                        context = context,
                        onSpinLane = { cat ->
                            AppPreferences.setLastSpinCategories(context, listOf(cat.id))
                            navController.navigateToTab(CurioRoutes.SPIN)
                        }
                    )
                    BrowseTab.MIXES -> MixesTabContent(
                        deckIds = deckIds,
                        washCat = washCat,
                        onApplyMix = { mix ->
                            val cats = mix.laneIds.mapNotNull { id ->
                                CurioCategories.visible.firstOrNull { it.id == id }
                            }
                            if (cats.isNotEmpty()) {
                                AppPreferences.setLastSpinCategories(context, cats.map { it.id })
                                navController.navigateToTab(CurioRoutes.SPIN)
                            }
                        },
                        onEditMix = { editMix = it; showEditor = true },
                        onDeleteMix = { deleteMix = it },
                        onNewMix = { editMix = null; showEditor = true }
                    )
                    BrowseTab.PINS -> PinsTabContent(
                        context = context,
                        onSpinLane = { cat ->
                            AppPreferences.setLastSpinCategories(context, listOf(cat.id))
                            navController.navigateToTab(CurioRoutes.SPIN)
                        }
                    )
                }
            }

        }

        // ── In-page bottom nav (overlay, so the captured content above
        //    extends underneath it and the glass capsules refract real
        //    pixels — the v241 sibling-capture pattern).
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrowseTab.entries.forEach { t ->
                NewPickerTabCapsule(
                    label = t.label,
                    glyph = t.glyph,
                    selected = tab == t,
                    accent = washCat.themedAccent(),
                    backdrop = contentGlassBackdrop,
                    modifier = Modifier.weight(1f),
                    onClick = { tabName = t.name }
                )
            }
        }
    }

    // ── Mix editor + delete confirm ──────────────────────────────────
    if (showEditor) {
        MixEditorSheet(
            washCat = washCat,
            categories = CurioCategories.visible,
            editMix = editMix,
            onDismiss = { showEditor = false },
            onSave = { mix ->
                AppPreferences.addOrReplaceMix(context, mix)
                showEditor = false
                AppPreferences.setLastSpinCategories(context, mix.laneIds)
                navController.navigateToTab(CurioRoutes.SPIN)
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
}

/** The three in-page bottom-nav tabs. */
internal enum class BrowseTab(val label: String, val glyph: String, val tagline: String) {
    BROWSE("Browse", CurioIcons.GridView, "Tap to spin · long-press to pin"),
    MIXES("Mixes", CurioIcons.Tune, "Your saved lane mixes"),
    PINS("Pins", CurioIcons.PushPin, "Faster starts from pinned lanes")
}

/** Browse — every category in a grid. */
@Composable
private fun BrowseTabContent(
    context: android.content.Context,
    onSpinLane: (CurioCategory) -> Unit
) {
    val allUnhidden = remember {
        CurioCategories.all.filter { it.id !in AppPreferences.hiddenCategoriesState }
    }
    val sortedCategories = remember(allUnhidden) {
        val wildcard = allUnhidden.filter { it.id == CategoryId.WILDCARD }
        val rest = allUnhidden.filter { it.id != CategoryId.WILDCARD }
            .sortedBy { it.displayName.lowercase() }
        wildcard + rest
    }
    var pinnedSet by remember {
        mutableStateOf(AppPreferences.getPinnedCategories(context).toSet())
    }
    val wide = windowWidthSizeClass().isWide

    LazyVerticalGrid(
        columns = if (wide) GridCells.Adaptive(minSize = 110.dp) else GridCells.Fixed(3),
        // bottom clearance for the overlaid bottom nav.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(sortedCategories) { cat ->
            NewPickerTile(
                category = cat,
                pinned = cat.id in pinnedSet,
                comingSoon = !cat.isReady,
                onClick = {
                    if (cat.isReady) onSpinLane(cat)
                },
                onLongClick = if (cat.isReady) {
                    {
                        pinnedSet = AppPreferences.togglePinnedCategory(context, cat.id).toSet()
                    }
                } else null
            )
        }
    }
}

/** Mixes — saved named mixes with apply / edit / delete. */
@Composable
private fun MixesTabContent(
    deckIds: Set<CategoryId>,
    washCat: CurioCategory,
    onApplyMix: (NamedMix) -> Unit,
    onEditMix: (NamedMix) -> Unit,
    onDeleteMix: (NamedMix) -> Unit,
    onNewMix: () -> Unit
) {
    val mixes = AppPreferences.savedMixesState
    val categories = CurioCategories.visible
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // bottom clearance for the overlaid bottom nav.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "new-mix") {
            NewPrimaryCapsule(
                label = "Create a mix",
                glyph = CurioIcons.Add,
                accent = washCat.themedButtonFill(),
                accentInk = washCat.themedButtonInk(),
                modifier = Modifier.padding(bottom = 10.dp),
                onClick = onNewMix
            )
        }
        if (mixes.isEmpty()) {
            item(key = "empty") {
                Text(
                    "No mixes yet — tap Create a mix to build your first one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(mixes, key = { it.createdAtMillis }) { mix ->
                NewMixRow(
                    mix = mix,
                    categories = categories,
                    active = mix.laneIds.toSet() == deckIds,
                    onApply = { onApplyMix(mix) },
                    onLongClick = { onDeleteMix(mix) },
                    onMore = { onEditMix(mix) }
                )
            }
        }
    }
}

/** Pins — the pinned quick-access lanes. */
@Composable
private fun PinsTabContent(
    context: android.content.Context,
    onSpinLane: (CurioCategory) -> Unit
) {
    var pinnedCats by remember {
        mutableStateOf(
            AppPreferences.getPinnedCategories(context)
                .mapNotNull { id -> CurioCategories.all.firstOrNull { it.id == id } }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // bottom clearance for the overlaid bottom nav.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp)
    ) {
        if (pinnedCats.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Long-press a category in Browse to pin it here — the pinned row also sits at the top of the quick sheet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(pinnedCats, key = { it.id }) { cat ->
                val shape = RoundedCornerShape(18.dp)
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .curioGlassEdge(shape)
                        .combinedClickable(
                            onClick = { onSpinLane(cat) },
                            onLongClick = {
                                pinnedCats = AppPreferences.togglePinnedCategory(context, cat.id)
                                    .mapNotNull { id -> CurioCategories.all.firstOrNull { it.id == id } }
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(lerp(MaterialTheme.colorScheme.surfaceContainerHigh, cat.themedAccent(), 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                name = cat.iconGlyph,
                                contentDescription = null,
                                size = 21.dp,
                                tint = cat.themedAccent()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (cat.id == CategoryId.WILDCARD) "Surprise mix" else cat.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Long-press to unpin · tap to spin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            onClick = {
                                pinnedCats = AppPreferences.togglePinnedCategory(context, cat.id)
                                    .mapNotNull { id -> CurioCategories.all.firstOrNull { it.id == id } }
                            },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                "Unpin",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One capsule of the in-page bottom nav. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPickerTabCapsule(
    label: String,
    glyph: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null
) {
    val shape = RoundedCornerShape(50)
    val glassOn = backdrop != null && isLiquidGlassPillsActive()
    val fill = if (selected) lerp(MaterialTheme.colorScheme.surfaceContainerHigh, accent, 0.22f)
    else MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (glassOn) Color.Transparent else fill,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(46.dp)
            .then(
                if (glassOn) Modifier.liquidGlassCapsule(
                    fill,
                    backdrop = backdrop,
                    shape = shape,
                    compact = true
                )
                else Modifier
            )
            .curioGlassEdge(shape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                size = 18.dp,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
                ),
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
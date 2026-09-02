package com.curio.app.features.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * The full "Browse" page of the new category picker — route CurioRoutes.PICKER.
 *
 * User direction (v3xx2): a full screen with an in-page BOTTOM NAV with three
 * tabs (Browse · Mixes · Pins). The in-page nav capsules are SOLID-filled
 * (surfaceContainerHigh / secondaryContainer) with a hairline glass edge —
 * never fully transparent, even in liquid-glass mode. Category tiles use
 * NEUTRAL theme roles (no category accent for fill/border/icon). Tap-and-hold
 * a category surfaces an option pill (Pin/Unpin · Spin) instead of direct pin.
 *
 * Back from this page re-opens the Spin category picker sheet (via
 * [com.curio.app.ui.components.SpinPickerRequest]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBrowseScreen(navController: NavController) {
    val context = LocalContext.current
    seedStarterMixes(context)
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
    // Tap-and-hold option pill target.
    var optionTarget by remember { mutableStateOf<CurioCategory?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .align(Alignment.TopCenter)
        ) {
            // ── Top bar: back + title ──────────────────────────────────
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
                    onClick = {
                        // Back re-opens the Spin picker sheet.
                        navController.popBackStack()
                        com.curio.app.ui.components.SpinPickerRequest.pending = true
                    },
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
                        },
                        onOptionTarget = { optionTarget = it }
                    )
                    BrowseTab.MIXES -> MixesTabContent(
                        deckIds = deckIds,
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
                        },
                        onOptionTarget = { optionTarget = it }
                    )
                }
            }
        }

        // ── In-page bottom nav (SOLID-fill capsules, keep the glass edge) ──
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
                    backdrop = contentGlassBackdrop,
                    modifier = Modifier.weight(1f),
                    onClick = { tabName = t.name }
                )
            }
        }
    }

    // ── Mix editor + delete confirm + option pill ──────────────────────
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
    optionTarget?.let { target ->
        val isPinned = target.id in AppPreferences.getPinnedCategories(context)
        BrowseOptionPill(
            category = target,
            isPinned = isPinned,
            onDismiss = { optionTarget = null },
            onPinToggle = {
                AppPreferences.togglePinnedCategory(context, target.id)
                optionTarget = null
            },
            onSpin = {
                optionTarget = null
                if (target.isReady) {
                    AppPreferences.setLastSpinCategories(context, listOf(target.id))
                    navController.navigateToTab(CurioRoutes.SPIN)
                }
            }
        )
    }
}

/** The three in-page bottom-nav tabs. */
internal enum class BrowseTab(val label: String, val glyph: String, val tagline: String) {
    BROWSE("Browse", CurioIcons.GridView, "Tap to spin · hold for options"),
    MIXES("Mixes", CurioIcons.Tune, "Your saved lane mixes"),
    PINS("Pins", CurioIcons.PushPin, "Faster starts from pinned lanes")
}

/** Browse — every category in a NEUTRAL grid. Hold → option pill. */
@Composable
private fun BrowseTabContent(
    context: android.content.Context,
    onSpinLane: (CurioCategory) -> Unit,
    onOptionTarget: (CurioCategory) -> Unit
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
                onClick = { if (cat.isReady) onSpinLane(cat) },
                onLongClick = if (cat.isReady) {
                    { onOptionTarget(cat) }
                } else null
            )
        }
    }
}

/** Mixes — saved named mixes with apply / edit / delete. */
@Composable
private fun MixesTabContent(
    deckIds: Set<CategoryId>,
    onApplyMix: (NamedMix) -> Unit,
    onEditMix: (NamedMix) -> Unit,
    onDeleteMix: (NamedMix) -> Unit,
    onNewMix: () -> Unit
) {
    val mixes = AppPreferences.savedMixesState
    val categories = CurioCategories.visible
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "new-mix") {
            NewPrimaryCapsule(
                label = "Create a mix",
                glyph = CurioIcons.Add,
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
                BrowseMixRow(
                    mix = mix,
                    categories = categories,
                    active = mix.laneIds.toSet() == deckIds,
                    onApply = { onApplyMix(mix) },
                    onEdit = { onEditMix(mix) },
                    onDelete = { onDeleteMix(mix) }
                )
            }
        }
    }
}

/** Pins — the pinned quick-access lanes. Hold → option pill. */
@Composable
private fun PinsTabContent(
    context: android.content.Context,
    onSpinLane: (CurioCategory) -> Unit,
    onOptionTarget: (CurioCategory) -> Unit
) {
    var pinnedCats by remember {
        mutableStateOf(
            AppPreferences.getPinnedCategories(context)
                .mapNotNull { id -> CurioCategories.all.firstOrNull { it.id == id } }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp)
    ) {
        if (pinnedCats.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Hold a category in Browse to pin it here — the pinned row also sits at the top of the quick sheet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(pinnedCats, key = { it.id }) { cat ->
                val shape = RoundedCornerShape(18.dp)
                // NEUTRAL pin row — no category accent.
                Surface(
                    shape = shape,
                    color = newPickerIdleFill(),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .combinedClickable(
                            onClick = { onSpinLane(cat) },
                            onLongClick = { onOptionTarget(cat) }
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
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                name = cat.iconGlyph,
                                contentDescription = null,
                                size = 21.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (cat.id == CategoryId.WILDCARD) "Surprise mix" else cat.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Hold for options · tap to spin",
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

/**
 * One named mix row for the Browse Mixes tab: name + lane teaser, a 3-dot
 * menu offering Edit / Delete, and a Spin pill. [active] marks the mix
 * currently applied as the deck.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseMixRow(
    mix: NamedMix,
    categories: List<CurioCategory>,
    active: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = newPickerIdleFill(),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .combinedClickable(onClick = onApply, onLongClick = { menuOpen = true })
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // NEUTRAL leading glyph plate — no category accent.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                val first = mix.laneIds.firstOrNull()
                val cat = first?.let { id -> categories.firstOrNull { c -> c.id == id } }
                CurioIcon(
                    name = cat?.iconGlyph ?: CurioIcons.Casino,
                    contentDescription = null,
                    size = 20.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mix.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mixTeaser(mix.laneIds, categories),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (active) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "Spinning",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                Surface(
                    onClick = onApply,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        "Spin",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            // 3-dot → Edit / Delete menu.
            Box {
                Surface(
                    onClick = { menuOpen = true },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(
                            name = CurioIcons.MoreVert,
                            contentDescription = "Mix options",
                            size = 18.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Edit", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

/**
 * The tap-and-hold option pill on the Browse page — a centered overlay with
 * Pin/Unpin + Spin. Solid surface (no liquid-glass transparency).
 */
@Composable
private fun BrowseOptionPill(
    category: CurioCategory,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onPinToggle: () -> Unit,
    onSpin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
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

/**
 * One SOLID-fill capsule of the in-page bottom nav. v3xx2 — the capsule is
 * always SOLID (surfaceContainerHigh idle / secondaryContainer selected)
 * with the hairline glass edge preserved; never fully transparent, even in
 * liquid-glass mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPickerTabCapsule(
    label: String,
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null
) {
    val shape = RoundedCornerShape(50)
    // SOLID fill in both states — no transparency. Idle uses the picker's
    // cream-lift neutral (light mode reads creamy, not dark tan).
    val fill = if (selected) MaterialTheme.colorScheme.secondaryContainer
               else newPickerIdleFill()
    Surface(
        onClick = onClick,
        shape = shape,
        color = fill,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(46.dp)
            .then(if (isLiquidGlassPillsActive()) Modifier.curioGlassEdge(shape) else Modifier)
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
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
                ),
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

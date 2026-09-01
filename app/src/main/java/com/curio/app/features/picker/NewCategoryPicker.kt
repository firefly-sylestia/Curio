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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.NamedMix
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.curioGlassEdge
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.themedButtonFill
import com.curio.app.ui.theme.themedButtonInk
import kotlin.random.Random

/**
 * The NEW category picker — "Category Mix Studio".
 *
 * A premium-minimal, user-friendly replacement for the glass-pill picker:
 * a quick sheet (pinned lanes · named mixes · Surprise me) plus a full
 * Browse page ([NewCategoryPickerBrowseScreen]) with an in-page bottom nav.
 *
 * Style contract (user direction):
 *  - NO raised glow pills, NO solid saturated selected tiles, NO per-tile
 *    category gradients. Flat rounded tiles with soft surface fills, a
 *    hairline liquid-glass edge (curioGlassEdge), tiny accent hints and
 *    big friendly type.
 *  - Real liquid-glass refraction (`liquidGlassCapsule`) is reserved for
 *    the Browse page's bottom-nav capsules, whose LOCAL backdrop records
 *    only the content area (sibling sampling — the v228 self-capture rule
 *    in app/AGENTS.md). Everything here stays solid + hairline edge so no
 *    pill ever samples its own subtree.
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
 * Layout: header → scrollable body (Pinned · Your mixes · Now spinning) →
 * pinned bottom action row (Surprise me · Create mix · Browse categories).
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

    val deckIds = remember {
        AppPreferences.getLastSpinCategories(context)
    }
    val deckCats = remember(deckIds) {
        deckIds.mapNotNull { id -> categories.firstOrNull { it.id == id } }
    }
    val mixes = AppPreferences.savedMixesState

    var showEditor by remember { mutableStateOf(false) }
    var editMix by remember { mutableStateOf<NamedMix?>(null) }
    var deleteMix by remember { mutableStateOf<NamedMix?>(null) }
    var pinnedList by remember {
        mutableStateOf(
            AppPreferences.getPinnedCategories(context)
                .mapNotNull { id -> CurioCategories.all.firstOrNull { it.id == id } }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // ── Header: big friendly title + glass close chip ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pick your mix",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pins, saved mixes & a surprise",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                NewPickerCircle(
                    glyph = CurioIcons.Close,
                    contentDescription = "Close",
                    onClick = onDismiss
                )
            }
            Spacer(Modifier.height(10.dp))

            // ── Scrollable body ───────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                // ── Pinned ──
                val pinned = pinnedList
                item(key = "pinned-label") {
                    NewSectionLabel("Pinned", hint = if (pinned.isNotEmpty()) "long-press to unpin" else null)
                }
                if (pinned.isEmpty()) {
                    item(key = "pinned-empty") {
                        Text(
                            "Long-press a category anywhere to pin your favourites here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            modifier = Modifier.padding(vertical = 2.dp, bottom = 6.dp)
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
                            pinned.forEach { cat ->
                                NewPickerChip(
                                    category = cat,
                                    trailingGlyph = CurioIcons.PushPin,
                                    onClick = { onCategorySelected(cat) },
                                    onLongClick = {
                                        pinnedList = AppPreferences.togglePinnedCategory(context, cat.id)
                                            .mapNotNull { id -> CurioCategories.all.firstOrNull { it.id == id } }
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Your mixes ──
                item(key = "mixes-label") { NewSectionLabel("Your mixes") }
                if (mixes.isEmpty()) {
                    item(key = "mixes-empty") {
                        Text(
                            "No mixes yet — tap + to build one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                } else {
                    items(mixes, key = { it.createdAtMillis }) { mix ->
                        NewMixRow(
                            mix = mix,
                            categories = categories,
                            active = mix.laneIds.toSet() == deckIds.toSet(),
                            onApply = {
                                val cats = mix.laneIds.mapNotNull { id ->
                                    categories.firstOrNull { it.id == id }
                                }
                                if (cats.isNotEmpty()) onCategoriesMixed(cats)
                            },
                            onLongClick = { deleteMix = mix }
                        )
                    }
                }

                // ── Now spinning ──
                item(key = "deck-label") { NewSectionLabel("Now spinning") }
                item(key = "deck-row") {
                    if (deckCats.isEmpty()) {
                        Text(
                            "Nothing picked yet — Surprise me!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            deckCats.take(5).forEach { cat ->
                                NewPickerChip(category = cat, onClick = null)
                            }
                            if (deckCats.size > 5) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        "+${deckCats.size - 5}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Pinned bottom action row ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Surprise me — the primary action.
                NewPrimaryCapsule(
                    label = "Surprise me",
                    glyph = CurioIcons.Shuffle,
                    accent = washCat.themedAccent(),
                    accentInk = washCat.onAccent(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val mix = surpriseMiniMix(categories)
                        if (mix.isNotEmpty()) onCategoriesMixed(mix)
                    }
                )
                // Create mix — + opens the editor.
                NewPickerCircle(
                    glyph = CurioIcons.Add,
                    contentDescription = "Create a mix",
                    onClick = {
                        editMix = null
                        showEditor = true
                    }
                )
                // Browse — opens the full page with bottom nav.
                NewPickerCircle(
                    glyph = CurioIcons.GridView,
                    contentDescription = "Browse all categories",
                    onClick = onBrowse
                )
            }
        }
    }

    // ── Mix editor + delete confirm ──────────────────────────────────
    if (showEditor) {
        MixEditorSheet(
            washCat = washCat,
            categories = categories,
            editMix = editMix,
            onDismiss = { showEditor = false },
            onSave = { mix ->
                AppPreferences.addOrReplaceMix(context, mix)
                showEditor = false
                // Saving also applies the mix, so the deck re-deals right away.
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
}

/** Small uppercase-ish section label with an optional inline hint. */
@Composable
private fun NewSectionLabel(label: String, hint: String? = null) {
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
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * One named mix row: name + lane teaser on the left, a Spin pill on the
 * right. Long-press (or trailing More) opens Delete. [active] marks the mix
 * currently applied as the deck.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMixRow(
    mix: NamedMix,
    categories: List<CurioCategory>,
    active: Boolean,
    onApply: () -> Unit,
    onLongClick: () -> Unit,
    onMore: (() -> Unit)? = null
) {
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
                onClick = onApply,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val first = mix.laneIds.firstOrNull()
            val cat = first?.let { id -> categories.firstOrNull { c -> c.id == id } }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(lerp(MaterialTheme.colorScheme.surfaceContainerHigh, cat?.themedAccent() ?: MaterialTheme.colorScheme.primary, 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = cat?.iconGlyph ?: CurioIcons.Casino,
                    contentDescription = null,
                    size = 20.dp,
                    tint = cat?.themedAccent() ?: MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = cat?.themedAccent() ?: MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "Spinning",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = cat?.onAccent() ?: MaterialTheme.colorScheme.onPrimary,
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
            if (onMore != null) {
                Surface(
                    onClick = onMore,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(
                            name = CurioIcons.MoreVert,
                            contentDescription = "Edit mix",
                            size = 18.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

/**
 * A premium-minimal lane tile: flat soft fill, hairline liquid-glass edge,
 * accent-tinted glyph plate, tiny accent pin badge. NO shadows, NO glow
 * pills, NO saturated fills — selection reads via a thin accent ring and
 * the glyph/check in accent.
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
    val catAccent = category.themedAccent()
    val isWildcard = category.id == CategoryId.WILDCARD
    val fill = when {
        selected -> lerp(MaterialTheme.colorScheme.surfaceContainerHigh, catAccent, 0.16f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Surface(
        shape = shape,
        color = fill,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .curioGlassEdge(shape)
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
            // Thin accent ring for selection, lighter ring for pin.
            androidx.compose.foundation.border(
                width = if (selected) 2.dp else if (pinned) 1.5.dp else 0.dp,
                color = if (selected) catAccent.copy(alpha = 0.9f)
                        else catAccent.copy(alpha = 0.45f),
                shape = shape
            )
            // Pin badge — top-end.
            if (pinned && !selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(lerp(fill, catAccent, 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.PushPin,
                        contentDescription = "Pinned",
                        size = 12.dp,
                        tint = catAccent
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
                        .background(lerp(fill, catAccent, if (selected) 0.20f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = category.iconGlyph,
                        contentDescription = null,
                        size = 22.dp,
                        tint = if (comingSoon) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        else catAccent
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
                        selected -> catAccent
                        comingSoon -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurface
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
                        .clip(RoundedCornerShape(50))
                        .background(catAccent),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.Check,
                        contentDescription = "Selected",
                        size = 12.dp,
                        tint = com.curio.app.ui.theme.onAccent(category)
                    )
                }
            }
        }
    }
}

/** A small pill chip — pinned lanes and the current deck summary. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewPickerChip(
    category: CurioCategory,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null,
    trailingGlyph: String? = null
) {
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp,
        modifier = Modifier
            .curioGlassEdge(shape)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = category.iconGlyph,
                contentDescription = null,
                size = 15.dp,
                tint = category.themedAccent()
            )
            Text(
                text = if (category.id == CategoryId.WILDCARD) "Surprise mix" else category.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (trailingGlyph != null) {
                CurioIcon(
                    name = trailingGlyph,
                    contentDescription = "Pinned",
                    size = 13.dp,
                    tint = category.themedAccent().copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** The primary "Surprise me" capsule — full-width, category accent fill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewPrimaryCapsule(
    label: String,
    glyph: String,
    accent: Color,
    accentInk: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = accent,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(52.dp)
            .curioGlassEdge(shape)
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
                tint = accentInk
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = accentInk,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** A round icon capsule — close, create, browse. */
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
        modifier = modifier
            .size(52.dp)
            .curioGlassEdge(shape)
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
        containerColor = washCat.categoryBackgroundWash(),
        shape = shape
    ) {
        var name by remember { mutableStateOf(editMix?.name ?: "") }
        var selected by remember {
            mutableStateOf(
                editMix?.laneIds?.toMutableSet() ?: mutableSetOf()
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

            // Multi-select grid of ready lanes.
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
                    accent = washCat.themedButtonFill(),
                    accentInk = washCat.themedButtonInk(),
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

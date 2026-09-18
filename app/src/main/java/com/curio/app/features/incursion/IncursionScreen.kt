package com.curio.app.features.incursion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.IncursionCatalog
import com.curio.app.data.IncursionEntry
import com.curio.app.data.IncursionGroup
import com.curio.app.data.IncursionStore
import com.curio.app.data.IncursionStudio
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioDropdownItem
import com.curio.app.ui.components.CurioDropdownMenu
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * INCURSION — the viewing order, on its own page.
 *
 * This is the app's only screen with no door: it is not in the drawer, not in
 * Settings, and it has no tab. It appears when the member types
 * [IncursionStore.PHRASE] into any search field in the app (see
 * `CurioSearchField`, which every search surface wears, and
 * `IncursionUnlockReveal` at the root, which announces it once).
 *
 * What it IS is deliberate: every Marvel, Sony and X-Men title in the order
 * they are meant to be watched, the essentials from upstream marked as such, and
 * a status on every row — not watched, watching, plan to watch, watched, on hold,
 * dropped. Two things exist here that no other Curio list has:
 *
 *  - ITS OWN NAV. The four destinations (Marvel · Sony · X-Men · Essentials)
 *    live in a bar at the foot of THIS page, not in Curio's tabs, because a
 *    viewing order is a different mode of looking — Curio's Cabinet is what you
 *    saved, this is what exists.
 *  - BULK BY PHASE. A phase header carries its own action: mark every title in
 *    Phase 3 watched in one gesture, or set the whole phase to any of the six
 *    states. Thirty rows is what a phase IS, and marking thirty rows one at a
 *    time is how a tracker goes unused. The same action exists per studio from
 *    the header's own menu.
 *
 * Nothing here writes to Room, the Cabinet, the streak or topic progress. A
 * viewing order is its own shelf, and marking Iron Man watched here says nothing
 * about a Curio topic about Iron Man.
 */
@Composable
fun IncursionScreen(navController: NavController) {
    val context = LocalContext.current
    val studios = remember(context) { IncursionCatalog.load(context) }

    // Its own nav state — deliberately saveable so a rotation does not throw the
    // member back to Marvel from the middle of the X-Men list.
    var destination by rememberSaveable { mutableStateOf(IncursionDestination.MARVEL.id) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(IncursionFilter.ALL.id) }
    var grid by rememberSaveable { mutableStateOf(false) }
    var detail by remember { mutableStateOf<IncursionEntry?>(null) }
    var pendingClear by remember { mutableStateOf<PendingBulkClear?>(null) }

    val activeFilter = IncursionFilter.fromId(filter)
    val essentials = remember(studios) { IncursionCatalog.essentials(studios) }
    val studioDestinations = studios.map { IncursionDestination.of(it) }
    val destinationId = destination.takeIf { id -> studioDestinations.any { it.id == id } }
        ?: IncursionDestination.ESSENTIALS.id
    val onEssentials = destinationId == IncursionDestination.ESSENTIALS.id
    val activeStudio = studios.firstOrNull { it.id == destinationId }

    // Everything below reads ONE shape — (studio, entries) pairs — so the
    // Essentials tab (which spans three studios) and a studio tab (which is one)
    // share a single renderer instead of being two lists that drift apart.
    //
    // `statusState` is read HERE, in composition, and handed to the filter as a
    // plain value: a `remember` whose body reads Compose state directly would
    // cache the first answer for the life of the screen and a status would never
    // appear to change.
    val statuses = IncursionStore.statusState
    val sections: List<Pair<IncursionStudio, List<IncursionEntry>>> = remember(
        studios, essentials, destinationId, query, filter, statuses
    ) {
        val needle = query.trim()
        fun keep(entry: IncursionEntry): Boolean {
            if (needle.isNotEmpty() && !entry.title.contains(needle, ignoreCase = true)) return false
            val status = statuses[entry.storageKey]
                ?.let { IncursionStore.Status.entries.getOrNull(it) }
                ?: IncursionStore.Status.UNWATCHED
            return activeFilter.matches(entry, status)
        }
        if (onEssentials) {
            val byStudio = essentials.filter { (_, entry) -> keep(entry) }
                .groupBy({ it.first }, { it.second })
            studios.mapNotNull { studio -> byStudio[studio]?.let { studio to it } }
        } else {
            val studio = activeStudio ?: return@remember emptyList()
            listOf(studio to studio.entries.filter(::keep))
        }
    }

    val allKeys = remember(sections) { sections.flatMap { (_, entries) -> entries.map { it.storageKey } } }
    val watched = remember(allKeys, statuses) {
        IncursionStore.count(allKeys) { it == IncursionStore.Status.WATCHED }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IncursionHeader(
            title = if (onEssentials) "Essentials" else activeStudio?.name.orEmpty(),
            blurb = if (onEssentials) {
                "The rows a first-time viewer should not skip, from all three lines."
            } else {
                activeStudio?.blurb.orEmpty()
            },
            groupLabel = activeStudio?.groupLabel?.lowercase() ?: "phase",
            watched = watched,
            total = allKeys.size,
            grid = grid,
            onGridChange = { grid = it },
            onBulk = { status ->
                IncursionStore.setGroupStatus(context, allKeys, status)
            },
            onClearAll = { pendingClear = PendingBulkClear(allKeys, "everything on this page") },
            onBack = { navController.popBackStack() }
        )

        CurioSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search these titles",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        IncursionFilterRow(
            selected = activeFilter,
            onSelect = { filter = it.id },
            modifier = Modifier.padding(top = 10.dp)
        )

        // The results take everything between the filters and the nav bar. It has
        // to be weighted: without it the list would claim the whole column and
        // push the page's own nav off the bottom.
        Box(Modifier.weight(1f)) {
            if (sections.isEmpty() || sections.all { it.second.isEmpty() }) {
                IncursionEmpty(query = query.trim(), filtered = activeFilter != IncursionFilter.ALL)
            } else if (grid) {
                IncursionGrid(
                    sections = sections,
                    onOpen = { detail = it },
                    onBulk = { keys, status -> IncursionStore.setGroupStatus(context, keys, status) },
                    onClear = { keys, name -> pendingClear = PendingBulkClear(keys, name) }
                )
            } else {
                IncursionList(
                    sections = sections,
                    onOpen = { detail = it },
                    onBulk = { keys, status -> IncursionStore.setGroupStatus(context, keys, status) },
                    onClear = { keys, name -> pendingClear = PendingBulkClear(keys, name) }
                )
            }
        }

        IncursionNavBar(
            destinations = studioDestinations + IncursionDestination.ESSENTIALS,
            active = destinationId,
            onSelect = { destination = it.id }
        )
    }

    detail?.let { entry ->
        IncursionDetailSheet(
            entry = entry,
            onDismiss = { detail = null },
            onStatus = { status -> IncursionStore.setStatus(context, entry.storageKey, status) }
        )
    }

    pendingClear?.let { victim ->
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            title = { Text("Clear ${victim.name}?") },
            text = {
                Text(
                    "Every status in ${victim.name} goes back to not watched. " +
                        "The titles themselves stay — only what you marked comes off."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    IncursionStore.setGroupStatus(context, victim.keys, IncursionStore.Status.UNWATCHED)
                    pendingClear = null
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = null }) { Text("Keep") }
            }
        )
    }
}

private data class PendingBulkClear(val keys: List<String>, val name: String)

// ── The four destinations ────────────────────────────────────────────────────

private enum class IncursionDestination(val id: String, val label: String, val glyph: String) {
    MARVEL("marvel", "Marvel", CurioIcons.Public),
    SONY("sony", "Sony", CurioIcons.Movie),
    XMEN("xmen", "X-Men", CurioIcons.AutoAwesome),
    ESSENTIALS("essentials", "Essentials", CurioIcons.Star);

    companion object {
        fun of(studio: IncursionStudio): IncursionDestination = when (studio.id) {
            "sony" -> SONY
            "xmen" -> XMEN
            else -> MARVEL
        }
    }
}

private enum class IncursionFilter(val id: String, val label: String) {
    ALL("all", "All"),
    ESSENTIALS("essential", "Essentials"),
    UNWATCHED("unwatched", "Not watched"),
    PROGRESS("progress", "In progress"),
    WATCHED("watched", "Watched");

    fun matches(entry: IncursionEntry, status: IncursionStore.Status): Boolean = when (this) {
        ALL -> true
        ESSENTIALS -> entry.essential
        UNWATCHED -> status == IncursionStore.Status.UNWATCHED
        PROGRESS -> status == IncursionStore.Status.WATCHING ||
            status == IncursionStore.Status.PLANNED ||
            status == IncursionStore.Status.ON_HOLD
        // A row the member finished and then dropped still counts as finished
        // for this filter: "watched" is a fact about the past, and the status is
        // where they left it afterwards.
        WATCHED -> status == IncursionStore.Status.WATCHED ||
            status == IncursionStore.Status.DROPPED
    }

    companion object {
        fun fromId(id: String): IncursionFilter = entries.firstOrNull { it.id == id } ?: ALL
    }
}

// ── Status ink ──────────────────────────────────────────────────────────────

/**
 * Each state's own colour, so a list of a hundred rows reads at a glance without
 * a legend. These are held here rather than derived from the theme's accent
 * because they are SIX different facts, not six shades of one — a single accent
 * could only have said "decided" and "not decided".
 */
private fun statusInk(status: IncursionStore.Status): Color = when (status) {
    IncursionStore.Status.WATCHED -> Color(0xFF3E7C58)
    IncursionStore.Status.WATCHING -> Color(0xFFC2543F)
    IncursionStore.Status.PLANNED -> Color(0xFF4A6E9C)
    IncursionStore.Status.ON_HOLD -> Color(0xFFB4842E)
    IncursionStore.Status.DROPPED -> Color(0xFF7A6E68)
    IncursionStore.Status.UNWATCHED -> Color(0xFF8A857E)
}

// ── The header ──────────────────────────────────────────────────────────────

@Composable
private fun IncursionHeader(
    title: String,
    blurb: String,
    groupLabel: String,
    watched: Int,
    total: Int,
    grid: Boolean,
    onGridChange: (Boolean) -> Unit,
    onBulk: (IncursionStore.Status) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val accent = settingsRoseAccent()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CurioBackButton(onClick = onBack)
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                // The wordmark: the page's own name, spaced like a codename
                // rather than a screen title. It is the one place in the app
                // that draws this word.
                Text(
                    "INCURSION",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                var menu by remember { mutableStateOf(false) }
                IconPill(
                    glyph = CurioIcons.Tune,
                    description = "Everything in this list",
                    onClick = { menu = true }
                )
                CurioDropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    accent = accent
                ) {
                    IncursionStore.Status.entries.forEach { status ->
                        CurioDropdownItem(
                            text = { Text("Mark all ${status.label.lowercase()}") },
                            accent = accent,
                            onClick = {
                                menu = false
                                onBulk(status)
                            }
                        )
                    }
                    CurioDropdownItem(
                        text = { Text("Clear every status") },
                        accent = accent,
                        danger = true,
                        onClick = {
                            menu = false
                            onClearAll()
                        }
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            IconPill(
                glyph = if (grid) CurioIcons.Menu else CurioIcons.GridView,
                description = if (grid) "List view" else "Grid view",
                onClick = { onGridChange(!grid) }
            )
        }

        Spacer(Modifier.height(6.dp))
        if (blurb.isNotBlank()) {
            Text(
                blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        IncursionProgress(watched = watched, total = total, accent = accent, groupLabel = groupLabel)
        Spacer(Modifier.height(10.dp))
    }
}

/** The one bar that says how far in the member is, in the page's own ink. */
@Composable
private fun IncursionProgress(watched: Int, total: Int, accent: Color, groupLabel: String) {
    val fraction = if (total == 0) 0f else watched.toFloat() / total
    val fill by animateFloatAsState(fraction, tween(420), label = "incursion-progress")
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$watched of $total watched",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                "by $groupLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }
    }
}

@Composable
private fun IconPill(glyph: String, description: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = glyph,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 19.dp
            )
        }
    }
}

// ── Filters ─────────────────────────────────────────────────────────────────

@Composable
private fun IncursionFilterRow(
    selected: IncursionFilter,
    onSelect: (IncursionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        IncursionFilter.entries.forEach { chip ->
            val on = chip == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (on) settingsRoseAccent()
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { if (!on) onSelect(chip) }
            ) {
                Text(
                    chip.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// ── The list ────────────────────────────────────────────────────────────────

@Composable
private fun IncursionList(
    sections: List<Pair<IncursionStudio, List<IncursionEntry>>>,
    onOpen: (IncursionEntry) -> Unit,
    onBulk: (List<String>, IncursionStore.Status) -> Unit,
    onClear: (List<String>, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((studio, entries) in sections) {
            val byGroup = entries.groupBy { it.group }
            for (group in studio.groups) {
                val rows = byGroup[group.id] ?: continue
                item(key = "${studio.id}-group-${group.id}") {
                    GroupHeader(
                        group = group,
                        studio = studio,
                        entries = rows,
                        onBulk = onBulk,
                        onClear = onClear
                    )
                }
                items(rows, key = { it.storageKey }) { entry ->
                    EntryRow(entry = entry, onOpen = { onOpen(entry) })
                }
            }
            // A title whose group id is not in the studio's own table (upstream
            // can add one before its heading ships) still has to be reachable,
            // so anything unmatched is drawn under a plain heading rather than
            // silently dropped.
            val orphans = entries.filter { entry -> studio.groups.none { it.id == entry.group } }
            if (orphans.isNotEmpty()) {
                item(key = "${studio.id}-group-orphan") {
                    Text(
                        "Unsorted",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }
                items(orphans, key = { it.storageKey }) { entry ->
                    EntryRow(entry = entry, onOpen = { onOpen(entry) })
                }
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun GroupHeader(
    group: IncursionGroup,
    studio: IncursionStudio,
    entries: List<IncursionEntry>,
    onBulk: (List<String>, IncursionStore.Status) -> Unit,
    onClear: (List<String>, String) -> Unit
) {
    val accent = settingsRoseAccent()
    val keys = entries.map { it.storageKey }
    val statuses = IncursionStore.statusState
    val watched = IncursionStore.count(keys) { it == IncursionStore.Status.WATCHED }
    val decided = IncursionStore.count(keys) { it.decided }
    // THE ONE STATE THE WHOLE PHASE WEARS, when every row in it agrees. That chip
    // then reads as taken, which is how a glance says "this phase is done"
    // without counting rows; while the rows disagree (the usual case) nothing is
    // pressed and the tally above is what speaks.
    val unanimous = IncursionStore.Status.entries.firstOrNull { candidate ->
        keys.isNotEmpty() && keys.all { key ->
            val status = statuses[key]?.let { IncursionStore.Status.entries.getOrNull(it) }
            (status ?: IncursionStore.Status.UNWATCHED) == candidate
        }
    }
    // The block's own kind and name: "PHASE / Phase 1" for Marvel, "ERA 1 /
    // Original Trilogy" for a line that numbers its eras as well as naming them.
    val eyebrow = (group.label ?: studio.groupLabel).uppercase()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                lerp(
                    MaterialTheme.colorScheme.surface,
                    accent,
                    if (isCurioDarkTheme()) 0.16f else 0.07f
                )
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.3.sp,
                color = accent
            )
            Spacer(Modifier.weight(1f))
            // The phase's own tally, right where the phase's own name is.
            Text(
                "$watched/${keys.size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
        }
        if (group.name.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // The block's own words: its tagline where upstream wrote one, and the
        // summary it carries otherwise. Two lines is the whole of it — this is a
        // heading, not a synopsis.
        val words = group.tagline?.takeIf { it.isNotBlank() }
            ?: group.summary?.takeIf { it.isNotBlank() }
        words?.let { line ->
            Spacer(Modifier.height(3.dp))
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(9.dp))
        // How far into the phase the member is, as one bar in the phase's own
        // accent — the header's bar, at the size of a card.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        if (keys.isEmpty()) 0f else (watched.toFloat() / keys.size).coerceIn(0f, 1f)
                    )
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }
        Spacer(Modifier.height(10.dp))
        // ── PHASE BULK SELECT ──────────────────────────────────────────────
        // The six states as themselves, on the phase they apply to: one tap
        // takes the whole block to that state. This was a "Mark all" menu
        // behind a tap, which meant the page's central gesture was invisible
        // until you went looking for it. The row scrolls rather than wraps, so
        // a phase header is three lines tall whatever the phone's width.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            IncursionStore.Status.entries.forEach { status ->
                PhaseStatusChip(
                    status = status,
                    pressed = status == unanimous,
                    onClick = { onBulk(keys, status) }
                )
            }
            if (decided > 0) {
                PhaseClearChip(
                    label = "Clear ${studio.groupLabel.lowercase()}",
                    onClick = { onClear(keys, group.name) }
                )
            }
        }
    }
}

/** One state of the phase's bulk select — the whole block, in one tap. */
@Composable
private fun PhaseStatusChip(
    status: IncursionStore.Status,
    pressed: Boolean,
    onClick: () -> Unit
) {
    val ink = statusInk(status)
    Surface(
        shape = RoundedCornerShape(50),
        color = if (pressed) {
            ink.copy(alpha = if (isCurioDarkTheme()) 0.26f else 0.15f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (pressed) ink.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            StatusDot(status = status, size = 8.dp)
            Spacer(Modifier.width(5.dp))
            Text(
                status.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (pressed) FontWeight.Bold else FontWeight.Medium,
                color = if (pressed) ink else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Putting a phase back to "not watched" — offered only once something is set. */
@Composable
private fun PhaseClearChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            CurioIcon(
                name = CurioIcons.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                size = 12.dp
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EntryRow(entry: IncursionEntry, onOpen: () -> Unit) {
    val status = IncursionStore.status(entry.storageKey)
    val ink = statusInk(status)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // The ORDER is the row's identity here — this list's whole promise is
            // a sequence, so the number leads.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (status == IncursionStore.Status.WATCHED) ink.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
            ) {
                Text(
                    entry.orderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (status == IncursionStore.Status.WATCHED) ink
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.essential) {
                        Spacer(Modifier.width(5.dp))
                        CurioIcon(
                            name = CurioIcons.Star,
                            contentDescription = "Essential",
                            tint = settingsRoseAccent(),
                            size = 13.dp
                        )
                    }
                }
                Text(
                    entryMeta(entry),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(status = status)
        }
    }
}

/** The meta line under a title: year · kind · rating · how much of it there is. */
private fun entryMeta(entry: IncursionEntry): String = buildList {
    entry.year?.let { add(it.toString()) }
    add(entry.typeLabel)
    entry.ageRating?.takeIf { it.isNotBlank() }?.let { add(it) }
    if (entry.type.lowercase() == "series") {
        val season = entry.season
        val episodes = entry.episodes
        // A stated range beats a count: upstream names which episodes a row is
        // ("S1 Eps 8–12" is a block of the season, not a season), and for the
        // rows that carry no count at all it is the only thing there is to say.
        val range = entry.episodeRange
        val body = when {
            season != null && range != null -> "S$season · ep $range"
            season != null && episodes != null -> "S$season · $episodes ep"
            range != null -> "ep $range"
            season != null -> "S$season"
            episodes != null -> "$episodes ep"
            else -> null
        }
        body?.let { add(it) }
    }
    entry.runtime?.takeIf { it > 0 }?.let { add("${it}m") }
    if (entry.upcoming) add(entry.releaseLabel ?: entry.releaseStatus.orEmpty())
}.joinToString(" · ")

@Composable
private fun StatusChip(status: IncursionStore.Status) {
    if (status == IncursionStore.Status.UNWATCHED) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        return
    }
    val ink = statusInk(status)
    Surface(
        shape = RoundedCornerShape(50),
        color = ink.copy(alpha = if (isCurioDarkTheme()) 0.26f else 0.14f)
    ) {
        Text(
            status.let {
                when (it) {
                    IncursionStore.Status.WATCHING -> "Watching"
                    IncursionStore.Status.PLANNED -> "Planned"
                    IncursionStore.Status.WATCHED -> "Watched"
                    IncursionStore.Status.ON_HOLD -> "On hold"
                    IncursionStore.Status.DROPPED -> "Dropped"
                    IncursionStore.Status.UNWATCHED -> ""
                }
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatusDot(status: IncursionStore.Status, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(statusInk(status))
    )
}

// ── The grid ────────────────────────────────────────────────────────────────

/**
 * The same rows, as covers-in-a-case.
 *
 * A grid drops the meta line for the two things a glance needs — the order and
 * the title — and keeps the status on a strip down the tile's edge, because a
 * colour bar is legible at tile size where a chip's words are not.
 *
 * The GROUP HEADERS stay, as full-width rows between the tiles: the point of
 * this page is watching a whole phase at once, and a grid that dissolved its
 * phases into one undifferentiated wall would have quietly removed the very
 * gesture the page exists for. Same header, same "Mark all" — two layouts, one
 * behaviour.
 */
@Composable
private fun IncursionGrid(
    sections: List<Pair<IncursionStudio, List<IncursionEntry>>>,
    onOpen: (IncursionEntry) -> Unit,
    onBulk: (List<String>, IncursionStore.Status) -> Unit,
    onClear: (List<String>, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for ((studio, entries) in sections) {
            val byGroup = entries.groupBy { it.group }
            for (group in studio.groups) {
                val rows = byGroup[group.id] ?: continue
                item(
                    key = "${studio.id}-grid-group-${group.id}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    GroupHeader(
                        group = group,
                        studio = studio,
                        entries = rows,
                        onBulk = onBulk,
                        onClear = onClear
                    )
                }
                items(rows, key = { it.storageKey }) { entry ->
                    EntryTile(entry = entry, onOpen = { onOpen(entry) })
                }
            }
            val orphans = entries.filter { entry -> studio.groups.none { it.id == entry.group } }
            if (orphans.isNotEmpty()) {
                item(key = "${studio.id}-grid-orphan", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Unsorted",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }
                items(orphans, key = { it.storageKey }) { entry ->
                    EntryTile(entry = entry, onOpen = { onOpen(entry) })
                }
            }
        }
    }
}

@Composable
private fun EntryTile(entry: IncursionEntry, onOpen: () -> Unit) {
    val status = IncursionStore.status(entry.storageKey)
    val ink = statusInk(status)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
    ) {
        Column {
            // The status reads as a bar across the tile's top edge: a chip's
            // words are illegible at this size, a colour is not.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(ink)
            )
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${entry.orderLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = settingsRoseAccent()
                    )
                    Spacer(Modifier.weight(1f))
                    if (entry.essential) {
                        CurioIcon(
                            name = CurioIcons.Star,
                            contentDescription = "Essential",
                            tint = settingsRoseAccent(),
                            size = 13.dp
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(
                        entry.year?.toString(),
                        entry.typeLabel,
                        status.takeIf { it != IncursionStore.Status.UNWATCHED }?.label
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status == IncursionStore.Status.UNWATCHED) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        ink
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── One title ───────────────────────────────────────────────────────────────

@Composable
private fun IncursionDetailSheet(
    entry: IncursionEntry,
    onDismiss: () -> Unit,
    onStatus: (IncursionStore.Status) -> Unit
) {
    val accent = settingsRoseAccent()
    val status = IncursionStore.status(entry.storageKey)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    entryMeta(entry),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                entry.desc?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                }
                entry.prereq?.takeIf { it.isNotBlank() && it != "—" }?.let { prereq ->
                    Text(
                        "WATCH FIRST",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        prereq,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    "WHERE YOU ARE",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                // Six rows, the current one ticked — the same picker the phase
                // header's menu offers, so a title and a whole phase are marked
                // the same way.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IncursionStore.Status.entries.forEach { option ->
                        val on = option == status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onStatus(option) }
                                .background(
                                    if (on) statusInk(option).copy(alpha = 0.14f) else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 9.dp)
                        ) {
                            StatusDot(status = option, size = 11.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (on) statusInk(option) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (on) {
                                CurioIcon(
                                    name = CurioIcons.Check,
                                    contentDescription = null,
                                    tint = statusInk(option),
                                    size = 16.dp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// ── The page's own nav bar ───────────────────────────────────────────────────

@Composable
private fun IncursionNavBar(
    destinations: List<IncursionDestination>,
    active: String,
    onSelect: (IncursionDestination) -> Unit
) {
    val accent = settingsRoseAccent()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            destinations.forEach { item ->
                val on = item.id == active
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (on) lerp(MaterialTheme.colorScheme.surface, accent, 0.16f)
                    else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { if (!on) onSelect(item) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CurioIcon(
                            name = item.glyph,
                            contentDescription = null,
                            tint = if (on) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            color = if (on) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ── Nothing found ───────────────────────────────────────────────────────────

@Composable
private fun IncursionEmpty(query: String, filtered: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CurioIcon(
            name = CurioIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            size = 30.dp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (query.isNotEmpty()) "Nothing matches \"$query\"" else "Nothing here yet",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (filtered) "Try another filter, or clear the search."
            else "This line has no titles in the order yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

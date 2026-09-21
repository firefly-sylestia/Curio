package com.curio.app.features.incursion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
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
import com.curio.app.ui.theme.curioCardShadow
import com.curio.app.ui.theme.curioFillInk
import com.curio.app.ui.theme.curioTintOn
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.delay

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
 * dropped. Three things exist here that no other Curio list has:
 *
 *  - ITS OWN NAV. The four destinations (Marvel · Sony · X-Men · Personal) live
 *    in a bar at the foot of THIS page, not in Curio's tabs, because a viewing
 *    order is a different mode of looking — Curio's Cabinet is what you saved,
 *    this is what exists.
 *  - BULK BY PHASE. A phase header carries its own action: mark every title in
 *    Phase 3 watched in one gesture, or set the whole phase to any of the six
 *    states. Thirty rows is what a phase IS, and marking thirty rows one at a
 *    time is how a tracker goes unused. The same action exists per studio from
 *    the header's own menu.
 *  - ART ON EVERY ROW (v427). Each row carries its own poster — the data has
 *    always known each title's TMDB id, so the app's own poster chain asks for
 *    the exact title instead of guessing by name (see `IncursionPosters`), under
 *    the same artwork switch every other fetched cover in the app obeys.
 *
 * v427 — THE PERSONAL TAB, AND WHAT ELSE CHANGED. The member: *"do the incursion
 * full ui redesign with better ui matching the app … instead of essential tab add
 * personal tab where [the] status shows … use proper icons in that ui"*, and,
 * asked what that tab should hold, *"all 3"* — the desk, the tally and the
 * figures. So:
 *
 *  · **Essentials is a FILTER and a STAR now, not a destination.** The rows it
 *    names are still marked on every row they belong to (the star, and the
 *    "Essentials" chip in the filter row); the destination it used to hold is
 *    the Personal tab the member asked for, and the catalog's own
 *    `IncursionCatalog.essentials` still answers who they are (the desk counts
 *    them).
 *  · **THE PERSONAL TAB IS THE STATUS DESK** (`IncursionPersonalDesk`): how far
 *    in the whole order is, one bar per line, what each line wants next, the
 *    six-state tally and the figures those statuses add up to. It reads; it
 *    never writes, and the only thing it can do is take you to a title.
 *  · **ONE SEARCH, EVERY LINE.** A query is answered across all three lines at
 *    once and each group says which line it came from — a search for a name
 *    should not depend on which tab happened to be open.
 *  · **NEXT UP.** The first row the page still wants from the member, in the
 *    head, one tap into its sheet.
 *  · **A TITLE'S SHEET IS THE APP'S SHEET**, with the poster as its head, the
 *    six states as one compact row, the synopsis and the "watch first" line,
 *    and a line of the member's own saved with the title.
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
    val studioDestinations = studios.map { IncursionDestination.of(it) }
    val destinationId = destination.takeIf { id -> studioDestinations.any { it.id == id } }
        ?: IncursionDestination.MARVEL.id
    val onPersonal = destinationId == IncursionDestination.PERSONAL.id
    val activeStudio = studios.firstOrNull { it.id == destinationId }

    // `statusState` is read HERE, in composition, and handed to the filter as a
    // plain value: a `remember` whose body reads Compose state directly would
    // cache the first answer for the life of the screen and a status would never
    // appear to change.
    val statuses = IncursionStore.statusState
    // ONE SEARCH, EVERY LINE (v427). The search used to be scoped to whichever
    // destination was open, so looking for a title meant knowing which of three
    // lists it was in first. A query now answers across all three at once, and
    // each group of results says which line it came from (see the studio band in
    // `IncursionList`). The tabs keep their meaning for BROWSING; searching is
    // the one gesture that is about the title, not the shelf.
    val needle = query.trim()
    val searching = needle.isNotEmpty()
    val sections: List<Pair<IncursionStudio, List<IncursionEntry>>> = remember(
        studios, destinationId, needle, filter, statuses
    ) {
        fun keep(entry: IncursionEntry): Boolean {
            if (needle.isNotEmpty() && !entry.title.contains(needle, ignoreCase = true)) return false
            val status = statuses[entry.storageKey]
                ?.let { IncursionStore.Status.entries.getOrNull(it) }
                ?: IncursionStore.Status.UNWATCHED
            return activeFilter.matches(entry, status)
        }
        if (searching) {
            studios.mapNotNull { studio ->
                studio.entries.filter(::keep).takeIf { it.isNotEmpty() }?.let { studio to it }
            }
        } else {
            val studio = activeStudio ?: return@remember emptyList()
            listOf(studio to studio.entries.filter(::keep))
        }
    }

    val allKeys = remember(sections) { sections.flatMap { (_, entries) -> entries.map { it.storageKey } } }
    val watched = remember(allKeys, statuses) {
        IncursionStore.count(allKeys) { it == IncursionStore.Status.WATCHED }
    }
    // NEXT UP (v427) — the first row this page still wants from the member: not
    // watched, and not one they deliberately set aside, in the order the list is
    // in. The same rule the Personal tab's desk uses, so the two can never point
    // at different titles.
    val nextUp = remember(sections, statuses) {
        sections.asSequence()
            .flatMap { (_, entries) -> entries.asSequence() }
            .firstOrNull { entry ->
                val status = statuses[entry.storageKey]
                    ?.let { IncursionStore.Status.entries.getOrNull(it) }
                    ?: IncursionStore.Status.UNWATCHED
                status == IncursionStore.Status.UNWATCHED ||
                    status == IncursionStore.Status.PLANNED
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IncursionHeader(
            title = when {
                searching -> "Every line"
                onPersonal -> "Your status"
                else -> activeStudio?.name.orEmpty()
            },
            blurb = when {
                searching -> "Matches across Marvel, Sony and X-Men — every group says " +
                    "which line it came from."
                onPersonal -> "Where you are in the whole order: a bar per line, the six " +
                    "states, and the figures your statuses add up to."
                else -> activeStudio?.blurb.orEmpty()
            },
            groupLabel = activeStudio?.groupLabel?.lowercase() ?: "phase",
            watched = watched,
            total = allKeys.size,
            // The list's own tools belong to a list: the desk is not one, and a
            // "mark everything on this page" over a stats page would be a
            // destructive control with nothing to mark.
            showListTools = !onPersonal || searching,
            nextUp = nextUp,
            onOpenNext = { detail = it },
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
            // The scope is the whole page now, so the field says so — a search
            // that quietly answered from three lists while its placeholder named
            // one would be the worse surprise.
            placeholder = "Search all three lines",
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
            val hasRows = sections.any { it.second.isNotEmpty() }
            when {
                hasRows && grid -> IncursionGrid(
                    sections = sections,
                    onOpen = { detail = it },
                    onBulk = { keys, status -> IncursionStore.setGroupStatus(context, keys, status) },
                    onClear = { keys, name -> pendingClear = PendingBulkClear(keys, name) }
                )

                hasRows -> IncursionList(
                    sections = sections,
                    // A search is answered from three lists at once, so its
                    // groups are labelled by line (see the studio band).
                    showLineBand = searching,
                    onOpen = { detail = it },
                    onBulk = { keys, status -> IncursionStore.setGroupStatus(context, keys, status) },
                    onClear = { keys, name -> pendingClear = PendingBulkClear(keys, name) }
                )

                searching || activeFilter != IncursionFilter.ALL -> IncursionEmpty(
                    query = needle,
                    filtered = true
                )

                // THE PERSONAL TAB: the desk, from the catalog the page already
                // holds and the statuses it already reads.
                onPersonal -> IncursionPersonalDesk(
                    studios = studios,
                    statuses = statuses,
                    onOpen = { detail = it }
                )

                else -> IncursionEmpty(query = "", filtered = false)
            }
        }

        IncursionNavBar(
            destinations = studioDestinations + IncursionDestination.PERSONAL,
            active = destinationId,
            onSelect = { destination = it.id }
        )
    }

    detail?.let { entry ->
        IncursionDetailSheet(
            entry = entry,
            onDismiss = { detail = null },
            onStatus = { status -> IncursionStore.setStatus(context, entry.storageKey, status) },
            onNote = { text -> IncursionStore.setNote(context, entry.storageKey, text) }
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

/**
 * THE PAGE'S OWN NAV — four destinations, each with a REAL icon of its own.
 *
 * v427 — EVERY GLYPH HERE IS A DRAWN ICON FROM THE APP'S SET, and no two of them
 * share a shape: the reel for Marvel, the round play for Sony, the spark for
 * X-Men and the person for the member. The old bar used a globe for Marvel and
 * the same film glyph for both film lines, so a glance could not tell which tab
 * was which — "use proper icons in that ui". [PERSONAL] took the place
 * [ESSENTIALS] held (the member: *"instead of essential tab add personal tab"*);
 * the essentials idea is still a filter chip and the star on a row, which is
 * where it belongs now that it is not a place.
 */
internal enum class IncursionDestination(val id: String, val label: String, val glyph: String) {
    MARVEL("marvel", "Marvel", CurioIcons.Movies),
    SONY("sony", "Sony", CurioIcons.PlayCircle),
    XMEN("xmen", "X-Men", CurioIcons.AutoAwesome),
    PERSONAL("personal", "Personal", CurioIcons.Person);

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
// v427 — `internal` rather than file-private: the Personal tab's desk draws the
// same six tones in its tally, and two mappings of "what colour is this state"
// is exactly how a page starts disagreeing with itself.

/**
 * Each state's own colour, so a list of a hundred rows reads at a glance without
 * a legend. These are held here rather than derived from the theme's accent
 * because they are SIX different facts, not six shades of one — a single accent
 * could only have said "decided" and "not decided".
 *
 * v425 — THEY ARE THE THEME'S OWN TONES NOW, not six fixed hexes. The old set
 * was chosen against a flat light page, so on a named theme's night (and on the
 * dark scheme's own plates) #3E7C58, #C2543F and the rest were the loudest ink
 * on the page — half of "it doesn't match the app style at all" is a palette
 * saying so. Each state keeps its own HUE (six facts stay six), but every one
 * is read through the app's own tone discipline — the shape the named themes'
 * own `accentFor` uses: a deeper, calmer ink by day, a lifted but muted one at
 * night. [curioFillInk] is still what reads on a chip FILLED with one of these.
 */
@Composable
internal fun incursionStatusInk(status: IncursionStore.Status): Color {
    val dark = isCurioDarkTheme()
    val hue = when (status) {
        IncursionStore.Status.WATCHED -> 152f
        IncursionStore.Status.WATCHING -> 18f
        IncursionStore.Status.PLANNED -> 214f
        IncursionStore.Status.ON_HOLD -> 40f
        IncursionStore.Status.DROPPED -> 22f
        IncursionStore.Status.UNWATCHED -> 30f
    }
    // A dropped row and an untouched one are both MUTED facts — they carry
    // almost no hue, so they read as "nothing to see" beside the four that do.
    val chroma = when (status) {
        IncursionStore.Status.DROPPED -> if (dark) 0.14f else 0.16f
        IncursionStore.Status.UNWATCHED -> 0.10f
        else -> if (dark) 0.30f else 0.44f
    }
    val lightness = when {
        status == IncursionStore.Status.UNWATCHED -> if (dark) 0.62f else 0.52f
        dark -> 0.70f
        else -> 0.36f
    }
    return fromHsl(hue, chroma, lightness)
}

// ── The header ──────────────────────────────────────────────────────────────

@Composable
private fun IncursionHeader(
    title: String,
    blurb: String,
    groupLabel: String,
    watched: Int,
    total: Int,
    /** False on the Personal tab: a desk has no rows to mark or to lay out. */
    showListTools: Boolean,
    /** The first row this page still wants, if it wants one. */
    nextUp: IncursionEntry?,
    onOpenNext: (IncursionEntry) -> Unit,
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
            if (showListTools) Box {
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
            if (showListTools) {
                Spacer(Modifier.width(6.dp))
                IconPill(
                    glyph = if (grid) CurioIcons.Menu else CurioIcons.GridView,
                    description = if (grid) "List view" else "Grid view",
                    onClick = { onGridChange(!grid) }
                )
            }
        }

        // v425 — THE PAGE'S HEAD IS A PLATE. The blurb and the one progress bar
        // used to sit straight on the page, which is the one thing this app
        // never does with a page's own summary: it is a card, on the app's own
        // ladder ([curioTintOn] over the card step, the soft shadow, no drawn
        // edge), and the name above it stays the page's title.
        Spacer(Modifier.height(10.dp))
        val headShape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .curioCardShadow(headShape)
                .clip(headShape)
                .background(
                    curioTintOn(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        accent,
                        if (isCurioDarkTheme()) 0.10f else 0.06f
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (blurb.isNotBlank()) {
                Text(
                    blurb,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            }
            IncursionProgress(
                watched = watched,
                total = total,
                accent = accent,
                groupLabel = groupLabel
            )
            // ── NEXT UP (v427) ────────────────────────────────────────────────
            // The one row the page still wants from the member, in the head,
            // where they already are: a progress bar says how far in they are and
            // then leaves them to find the next row themselves. It is a tap, not
            // a control — it opens the title's own sheet, exactly as the row it
            // names would.
            nextUp?.let { entry ->
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onOpenNext(entry) }
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                ) {
                    CurioIcon(
                        name = CurioIcons.PlayCircle,
                        contentDescription = null,
                        tint = accent,
                        size = 16.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Next up",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "#${entry.orderLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
        // v425 — opaque, off the ladder: a control is a real plate, and a
        // translucent disc over whatever is behind it is how a page starts to
        // look like it was assembled from parts.
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
            val chipInk = settingsRoseAccent()
            Surface(
                shape = RoundedCornerShape(50),
                color = if (on) chipInk else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { if (!on) onSelect(chip) }
            ) {
                Text(
                    chip.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    // The ink asks the FILL it sits on instead of wearing white
                    // on whatever the accent happens to be — a named theme's
                    // night accent is a light tone, and white on it vanished.
                    color = if (on) curioFillInk(chipInk)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
    /** True when these groups came from MORE THAN ONE line — a search. */
    showLineBand: Boolean,
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
            if (showLineBand && sections.size > 1) {
                item(key = "${studio.id}-band") {
                    StudioBand(studio = studio, hits = entries.size)
                }
            }
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
    // v425 — the phase header is a CARD in the app's own language: the card
    // step tinted with the page's accent, the soft shadow for its lift,
    // and no drawn edge anywhere (the old wash over `surface` read as a
    // painted band rather than as a plate).
    val headerShape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .curioCardShadow(headerShape)
            .clip(headerShape)
            .background(
                curioTintOn(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    accent,
                    if (isCurioDarkTheme()) 0.10f else 0.06f
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
    val ink = incursionStatusInk(status)
    // v425 — a chip is a FILL, not an outline: the app gave up drawn edges
    // ("no more outlines anywhere — a card is its fill, its radius and a soft
    // shadow"), and these two were the last boxed rectangles on the page.
    Surface(
        shape = RoundedCornerShape(50),
        color = if (pressed) {
            curioTintOn(
                MaterialTheme.colorScheme.surfaceContainerLow,
                ink,
                if (isCurioDarkTheme()) 0.20f else 0.14f
            )
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
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
    // The same rule as its neighbours: a fill, never a boxed line.
    Surface(
        shape = RoundedCornerShape(50),
        color = curioTintOn(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.error,
            if (isCurioDarkTheme()) 0.20f else 0.12f
        ),
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
    val ink = incursionStatusInk(status)
    val rowShape = RoundedCornerShape(18.dp)
    Surface(
        shape = rowShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .curioCardShadow(rowShape)
            .clip(rowShape)
            .clickable(onClick = onOpen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            // ── THE POSTER, AND THE ORDER THAT LEADS IT (v427) ────────────────
            //
            // The member, of this list: "with movie cover also in list i want
            // proper moview cover fetching". The plate is the row's own art — the
            // exact title's, by TMDB id — and while it is arriving (or while
            // fetching is off) it is the app's own drawn plate carrying the order
            // the row leads with, because this list's whole promise is a sequence.
            IncursionPosterPlate(
                entry = entry,
                modifier = Modifier.width(40.dp).height(56.dp),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The order stays ON the row rather than inside the plate: a
                    // poster is the art, and the place in the sequence is a fact
                    // that must survive the art arriving.
                    Text(
                        "#${entry.orderLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (status == IncursionStore.Status.WATCHED) ink
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(7.dp))
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
    val ink = incursionStatusInk(status)
    Surface(
        shape = RoundedCornerShape(50),
        color = ink.copy(alpha = if (isCurioDarkTheme()) 0.26f else 0.14f)
    ) {
        Text(
            // v427 — the state's own name for a chip lives on the state
            // (`Status.shortLabel`), not in a `when` here: this was the third
            // mapping of the same six words.
            status.shortLabel,
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
            .background(incursionStatusInk(status))
    )
}

// ── The line a search hit came from ────────────────────────────────────────

/**
 * v427 — WHICH LINE IS THIS?
 *
 * A search answers from all three lists at once, so its results are grouped by
 * the line they belong to and each group says so at its head — the line's own
 * icon, its name, and how many rows it holds. It is drawn BETWEEN the results,
 * not as a heading over the whole page: the member is looking for a title, and the
 * band is the one thing that tells them where in the order they just landed.
 */
@Composable
private fun StudioBand(studio: IncursionStudio, hits: Int) {
    val accent = settingsRoseAccent()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp)
    ) {
        CurioIcon(
            name = IncursionDestination.of(studio).glyph,
            contentDescription = null,
            tint = accent,
            size = 15.dp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            studio.name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.3.sp,
            color = accent
        )
        Spacer(Modifier.weight(1f))
        Text(
            if (hits == 1) "1 title" else "$hits titles",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
    val showLineBand = sections.size > 1
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for ((studio, entries) in sections) {
            if (showLineBand) {
                item(key = "${studio.id}-grid-band", span = { GridItemSpan(maxLineSpan) }) {
                    StudioBand(studio = studio, hits = entries.size)
                }
            }
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
    val ink = incursionStatusInk(status)
    val tileShape = RoundedCornerShape(20.dp)
    Surface(
        shape = tileShape,
        // v425 — the status is a whisper in the tile's own fill plus a dot
        // beside the order, instead of a colour bar pinned across its top
        // edge: a 3dp stripe is a rule drawn on a card, and this app's cards
        // have not worn one since the outline pass.
        color = if (status == IncursionStore.Status.UNWATCHED) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            curioTintOn(
                MaterialTheme.colorScheme.surfaceContainerLow,
                ink,
                if (isCurioDarkTheme()) 0.10f else 0.06f
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .curioCardShadow(tileShape)
            .clip(tileShape)
            .clickable(onClick = onOpen)
    ) {
        // ── THE POSTER IS THE TILE NOW (v427) ────────────────────────────────
        //
        // A grid of titles with no artwork is a wall of text; the member asked
        // for covers "in list", and a two-up grid is exactly where a cover earns
        // its place. The art takes the tile's whole width at a poster's own
        // height, and everything the tile said before (the order, the essentials
        // mark, the state, the meta line) keeps its place underneath it.
        Column {
            IncursionPosterPlate(
                entry = entry,
                modifier = Modifier.fillMaxWidth().height(168.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
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
                    if (status != IncursionStore.Status.UNWATCHED) {
                        StatusDot(status = status, size = 8.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    if (entry.essential) {
                        CurioIcon(
                            name = CurioIcons.Star,
                            contentDescription = "Essential",
                            tint = settingsRoseAccent(),
                            size = 13.dp
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(
                        entry.year?.toString(),
                        entry.typeLabel,
                        status.takeIf { it != IncursionStore.Status.UNWATCHED }?.shortLabel
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
    onStatus: (IncursionStore.Status) -> Unit,
    /** v427 — the member's own line, saved with the title. */
    onNote: (String) -> Unit
) {
    val accent = settingsRoseAccent()
    val status = IncursionStore.status(entry.storageKey)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pillShape = RoundedCornerShape(50)
    // The note is edited locally and written after a pause, not on every
    // keystroke: a prefs write per character is how a text field starts to
    // stutter, and the store is the app's own small JSON map.
    var draft by remember(entry.storageKey) { mutableStateOf(IncursionStore.note(entry.storageKey)) }
    LaunchedEffect(draft) {
        if (draft != IncursionStore.note(entry.storageKey)) {
            delay(500)
            onNote(draft)
        }
    }

    // v425 — A TITLE OPENS AS A SHEET, NOT A BOX. This was an `AlertDialog`:
    // the app has ONE detail language — the sheet (a topic reveal, a book's own
    // page, the feedback form) — and a boxed dialog put a second, foreign
    // surface in front of a page that already had one, on the only screen where
    // a title's synopsis is read out. The sheet gives it room instead of 420dp.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── THE HEAD IS THE POSTER (v427) ───────────────────────────────
            //
            // The app's one detail language is a sheet with the thing itself at
            // the top — a topic reveal opens on the topic, a book on its cover —
            // and a title's own sheet opened on a small disc with a number in
            // it. The poster is the title's own art now (see [IncursionPosters]):
            // the order and the line it belongs to over it as the page's own
            // eyebrow, the title and its meta beside it, the state and the
            // essentials mark under them.
            Row(verticalAlignment = Alignment.Top) {
                IncursionPosterPlate(
                    entry = entry,
                    modifier = Modifier.width(94.dp).height(140.dp),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "INCURSION · #${entry.orderLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.3.sp,
                        color = accent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        entryMeta(entry),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusChip(status = status)
                        if (entry.essential) {
                            Surface(
                                shape = pillShape,
                                color = curioTintOn(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    accent,
                                    if (isCurioDarkTheme()) 0.18f else 0.12f
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    CurioIcon(
                                        name = CurioIcons.Star,
                                        contentDescription = null,
                                        tint = accent,
                                        size = 12.dp
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "Essential",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            entry.desc?.takeIf { it.isNotBlank() }?.let { desc ->
                Column {
                    Text(
                        "ABOUT",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            entry.prereq?.takeIf { it.isNotBlank() && it != "—" }?.let { prereq ->
                Column {
                    Text(
                        "WATCH FIRST",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        prereq,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "WHERE YOU ARE",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            // Six CHIPS, the current one filled — v427 compacted this from six
            // full-width rows to one scrolling line, because six stacked rows
            // pushed the synopsis and the note off the sheet's first screen for a
            // control that is a single tap either way. They are the very chips the
            // phase header offers (same tones, same shapes), so a title and a
            // whole phase are still marked in one language.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                IncursionStore.Status.entries.forEach { option ->
                    val on = option == status
                    val ink = incursionStatusInk(option)
                    val chipShape = RoundedCornerShape(50)
                    Surface(
                        shape = chipShape,
                        color = if (on) {
                            curioTintOn(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                ink,
                                if (isCurioDarkTheme()) 0.24f else 0.16f
                            )
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier
                            .clip(chipShape)
                            .clickable { onStatus(option) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                        ) {
                            StatusDot(status = option, size = 9.dp)
                            Spacer(Modifier.width(7.dp))
                            Text(
                                option.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                color = if (on) ink else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (on) {
                                Spacer(Modifier.width(5.dp))
                                CurioIcon(
                                    name = CurioIcons.Check,
                                    contentDescription = null,
                                    tint = ink,
                                    size = 13.dp
                                )
                            }
                        }
                    }
                }
            }

            // ── A LINE OF YOUR OWN (v427) ────────────────────────────────────
            //
            // The member asked for it plainly ("your own note on a title"), and
            // it belongs HERE rather than in a menu: the reason a row is marked
            // the way it is is the one thing about it only they can write, and
            // the sheet is where they decide. One line, saved with the title (see
            // IncursionStore.setNote) and written after a pause, not per keystroke.
            Column {
                Text(
                    "YOUR NOTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                IncursionNoteField(
                    value = draft,
                    onValueChange = { draft = it.take(IncursionStore.NOTE_LIMIT) },
                    accent = accent
                )
            }

            Surface(
                onClick = onDismiss,
                shape = pillShape,
                color = accent,
                contentColor = curioFillInk(accent),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp)
                )
            }
        }
    }
}

// ── The note field ──────────────────────────────────────────────────────────

/**
 * ONE LINE OF THE MEMBER'S OWN, inside a title's sheet.
 *
 * A `BasicTextField` in a rounded plate — the shape the app's other small inputs
 * wear (see the shared bubble's session note) — with the placeholder as a `Text`
 * drawn UNDER the field rather than as a Material label, so the empty state costs
 * no height and the field never jumps when the first character arrives.
 */
@Composable
private fun IncursionNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(accent),
        maxLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) { inner ->
        Box {
            if (value.isEmpty()) {
                Text(
                    "Why you left it where you did…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            inner()
        }
    }
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

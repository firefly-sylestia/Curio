package com.curio.app.features.incursion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.curio.app.data.SeriesEpisode
import com.curio.app.features.reveal.SeriesEpisodeFetcher
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
    // v429 — the list's own order, and which way round it runs (see
    // [IncursionSort]). Saveable like the tab and the filter, so a rotation does
    // not drop the member back into the viewing order mid-browse.
    var sortId by rememberSaveable { mutableStateOf(IncursionSort.ORDER.id) }
    var sortFlipped by rememberSaveable { mutableStateOf(false) }
    var detail by remember { mutableStateOf<IncursionEntry?>(null) }
    var pendingClear by remember { mutableStateOf<PendingBulkClear?>(null) }

    val activeFilter = IncursionFilter.fromId(filter)
    val activeSort = IncursionSort.fromId(sortId)
    val studioDestinations = studios.map { IncursionDestination.of(it) }
    // v428 — AND THE TAB THAT IS NOT A STUDIO IS STILL A TAB.
    //
    // This validity check listed the STUDIO ids only, so the Personal tab's own
    // id failed it and every tap on Personal was quietly answered with Marvel:
    // `onPersonal` could never be true, the desk never composed, and the member's
    // report was exactly that ("the profile page doesnt even open"). The check is
    // against ALL the destinations — the tabs the nav bar actually draws — which
    // is the list a saved tab id has to be legal against (a `rememberSaveable`
    // restoration lands here too, so a rotation on the desk used to bounce to
    // Marvel as well).
    val destinations = studioDestinations + IncursionDestination.PERSONAL
    val destinationId = destination.takeIf { id -> destinations.any { it.id == id } }
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
        studios, destinationId, needle, filter, statuses, sortId, sortFlipped
    ) {
        fun keep(entry: IncursionEntry): Boolean {
            if (needle.isNotEmpty() && !entry.title.contains(needle, ignoreCase = true)) return false
            val status = statuses[entry.storageKey]
                ?.let { IncursionStore.Status.entries.getOrNull(it) }
                ?: IncursionStore.Status.UNWATCHED
            return activeFilter.matches(entry, status)
        }
        val gathered = if (searching) {
            studios.mapNotNull { studio ->
                studio.entries.filter(::keep).takeIf { it.isNotEmpty() }?.let { studio to it }
            }
        } else {
            val studio = activeStudio ?: return@remember emptyList()
            listOf(studio to studio.entries.filter(::keep))
        }
        // v429 — THE ORDER IS APPLIED WHERE THE ROWS ARE GATHERED, so the list,
        // the grid, the head's own progress and "Next up" all see the same rows
        // in the same order: a sorted page that still counted its progress over
        // a differently-ordered list is how two numbers about one shelf start
        // disagreeing.
        if (activeSort == IncursionSort.ORDER) gathered
        else gathered.map { (studio, rows) -> studio to activeSort.order(rows, sortFlipped) }
    }

    /**
     * True when the rows are in an order OTHER than the viewing one, which is
     * what tells the list and the grid to drop their PHASE headers: a phase is a
     * block of the viewing order, so a title/year/runtime order drawn under
     * phase headings would be sorted rows filed under headings that no longer
     * mean anything.
     */
    val sortedFlat = activeSort != IncursionSort.ORDER

    // v428 — THE WATCH BUTTON'S ONE DECISION, in one place: a row that is
    // WATCHED goes back to not watched, and anything else becomes watched. Two
    // taps therefore mean "done" and "undo", which is what the member asked for
    // ("mark it Watched, tap again to undo") — and everything finer than that
    // (Watching, Planned, On hold, Dropped) is still the sheet's own six chips.
    val watchToggle: (IncursionEntry) -> Unit = { entry ->
        IncursionStore.setStatus(
            context,
            entry.storageKey,
            if (IncursionStore.status(entry.storageKey) == IncursionStore.Status.WATCHED) {
                IncursionStore.Status.UNWATCHED
            } else {
                IncursionStore.Status.WATCHED
            }
        )
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
            sort = activeSort,
            sortFlipped = sortFlipped,
            onSort = { picked -> sortId = picked.id },
            onSortFlip = { sortFlipped = !sortFlipped },
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

        // v428 — THE FILTERS BELONG TO A LIST. On the Personal tab they were
        // still offered, and a filter chosen there decided whether the desk drew
        // at all (see the branch below) — which is how a stats page could answer
        // with "nothing here". A desk has no rows to filter, so the row is not
        // drawn there; a SEARCH still is, because a search is about a title.
        if (!onPersonal || searching) {
            IncursionFilterRow(
                selected = activeFilter,
                onSelect = { filter = it.id },
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // The results take everything between the filters and the nav bar. It has
        // to be weighted: without it the list would claim the whole column and
        // push the page's own nav off the bottom.
        Box(Modifier.weight(1f)) {
            val hasRows = sections.any { it.second.isNotEmpty() }
            when {
                // v428 — THE DESK IS NOT A LIST, so it is asked about FIRST.
                //
                // It used to be reached only after "are there rows?", "is there a
                // search?" and "is a filter on?" had all said no — so the Personal
                // tab showed the FILTERED-EMPTY page whenever a filter was set (a
                // stats desk has no rows to filter) and its own desk only in the
                // one state where nothing else applied. Searching still wins: a
                // query is about a title, not about the shelf it happens to be
                // under, and a search made from Personal is answered across all
                // three lines like any other.
                onPersonal && !searching -> IncursionPersonalDesk(
                    studios = studios,
                    statuses = statuses,
                    onOpen = { detail = it }
                )

                hasRows && grid -> IncursionGrid(
                    sections = sections,
                    // v429 — a sorted view is one flat run of rows; phases belong
                    // to the viewing order (see `sortedFlat`).
                    flat = sortedFlat,
                    onOpen = { detail = it },
                    onWatch = watchToggle,
                    onBulk = { keys, status -> IncursionStore.setGroupStatus(context, keys, status) },
                    onClear = { keys, name -> pendingClear = PendingBulkClear(keys, name) }
                )

                hasRows -> IncursionList(
                    sections = sections,
                    // A search is answered from three lists at once, so its
                    // groups are labelled by line (see the studio band).
                    showLineBand = searching,
                    flat = sortedFlat,
                    onOpen = { detail = it },
                    onWatch = watchToggle,
                    onBulk = { keys, status -> IncursionStore.setGroupStatus(context, keys, status) },
                    onClear = { keys, name -> pendingClear = PendingBulkClear(keys, name) }
                )

                else -> IncursionEmpty(
                    query = needle,
                    filtered = searching || activeFilter != IncursionFilter.ALL
                )
            }
        }

        IncursionNavBar(
            destinations = destinations,
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
    /**
     * v429 — THE LIST'S ORDER, and the two ways the member changes it: pick
     * another order, or turn this one around. It lives in the head rather than
     * beside the filter chips because it describes the ROWS rather than the
     * shelf they are on — and because the chips row is already the page's full
     * width at four filters on a phone.
     */
    sort: IncursionSort,
    sortFlipped: Boolean,
    onSort: (IncursionSort) -> Unit,
    onSortFlip: () -> Unit,
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
            // v429 — THE ORDER CONTROL. A list can be read four ways, and the
            // one that matters most — the viewing order — is the one it starts
            // in, so the control is a quiet pill that names the order it is in
            // ("Order: Title · A to Z") rather than a chip row competing with
            // the filters below it.
            //
            // The glyph is `drag_handle`, and that is a MEASURED choice, not a
            // preference: the bundled Material Symbols subset has no `sort`,
            // `swap_vert`, `sort_by_alpha`, `low_priority` or `filter_list` in
            // its name table (checked with the byte probe the icon contract
            // prescribes — see [safeGlyphName]'s note), so a `sort` pill would
            // have drawn the word "sort" on a phone that lacks the ligature.
            if (showListTools) {
                Spacer(Modifier.width(6.dp))
                Box {
                    var menu by remember { mutableStateOf(false) }
                    val flip = sort.flipLabel(sortFlipped)
                    IconPill(
                        glyph = CurioIcons.DragHandle,
                        description = if (flip == null) "Order: ${sort.label}"
                        else "Order: ${sort.label} · $flip",
                        onClick = { menu = true }
                    )
                    CurioDropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false },
                        accent = accent
                    ) {
                        IncursionSort.entries.forEach { option ->
                            val on = option == sort
                            CurioDropdownItem(
                                text = {
                                    // The ACTIVE order says which way round it is
                                    // running, so the member never has to open
                                    // the flip row to find out.
                                    val way = if (on) option.flipLabel(sortFlipped) else null
                                    Text(if (way == null) option.label else "${option.label} · $way")
                                },
                                accent = accent,
                                selected = on,
                                trailingIcon = {
                                    if (on) CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                                },
                                onClick = {
                                    menu = false
                                    onSort(option)
                                }
                            )
                        }
                        // Only offered where it means something: the viewing
                        // order has no other way round (see flipLabel).
                        if (flip != null) {
                            CurioDropdownItem(
                                text = { Text("Switch to ${sort.flipLabel(!sortFlipped)}") },
                                accent = accent,
                                onClick = {
                                    menu = false
                                    onSortFlip()
                                }
                            )
                        }
                    }
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

// ── Sorting ─────────────────────────────────────────────────────────────────

/**
 * v429 — HOW THE ROWS ARE ORDERED (the member: *"for incursion ui, make it more
 * better, add sorting etc."*).
 *
 * [ORDER] is the page's own: the viewing order upstream curated, phase by phase —
 * the one thing an Incursion list exists to be. Every other order is a way of
 * LOOKING FOR a title rather than following the story, and that is why they drop
 * the phase headers: a list that says "Title, A to Z" and still arrives in
 * phases would be an order the page only pretended to have (see `flat` in
 * [IncursionList]).
 *
 * [flipped] reverses whatever the order's own sense is, and each order has a
 * DEFAULT sense a member actually means: titles A–Z, the newest year first, the
 * longest runtime first. A row with no year (an announced title) sorts to the
 * end of a year order rather than pretending to be year 0 of the list.
 */
internal enum class IncursionSort(val id: String, val label: String) {
    ORDER("order", "Viewing order"),
    TITLE("title", "Title"),
    YEAR("year", "Year"),
    RUNTIME("runtime", "Runtime");

    /**
     * The rows in this order, or the same list when this IS [ORDER].
     *
     * The two fact orders PARTITION first, so a row that has no year yet (an
     * announced title) or no stated runtime always sinks to the END rather than
     * pretending to be year 0 or a zero-minute film at the top of the list — that
     * is the one thing a reversed list would get wrong (see [flipLabel]).
     */
    fun order(rows: List<IncursionEntry>, flipped: Boolean): List<IncursionEntry> = when (this) {
        ORDER -> rows
        TITLE -> {
            val alphabetical = rows.sortedBy { it.title.trim().lowercase() }
            if (flipped) alphabetical.reversed() else alphabetical
        }
        YEAR -> rows.factOrder(flipped) { it.year }
        RUNTIME -> rows.factOrder(flipped) { it.runtime }
    }

    /**
     * A numeric order with the rows that have no number yet pushed to the back:
     * ascending when [flipped], and the order's own default sense otherwise (the
     * newest year, the longest runtime).
     */
    private fun List<IncursionEntry>.factOrder(
        flipped: Boolean,
        value: (IncursionEntry) -> Int?
    ): List<IncursionEntry> {
        val (known, unknown) = partition { value(it) != null }
        val ascending = known.sortedBy { value(it) ?: 0 }
        return (if (flipped) ascending else ascending.reversed()) + unknown
    }

    /**
     * What FLIPPING this order would mean, in the member's own words — or null
     * for [ORDER], which has no other way round to offer: a curatorial viewing
     * order read backwards is not an order anybody asked for, so the flip row is
     * not drawn for it.
     */
    fun flipLabel(flipped: Boolean): String? = when (this) {
        ORDER -> null
        TITLE -> if (flipped) "Z to A" else "A to Z"
        YEAR -> if (flipped) "Oldest first" else "Newest first"
        RUNTIME -> if (flipped) "Shortest first" else "Longest first"
    }

    companion object {
        fun fromId(id: String): IncursionSort = entries.firstOrNull { it.id == id } ?: ORDER
    }
}

// ── The list ────────────────────────────────────────────────────────────────

@Composable
private fun IncursionList(
    sections: List<Pair<IncursionStudio, List<IncursionEntry>>>,
    /** True when these groups came from MORE THAN ONE line — a search. */
    showLineBand: Boolean,
    /**
     * v429 — TRUE WHEN THE ROWS ARE IN AN ORDER OTHER THAN THE VIEWING ONE, so
     * the PHASE HEADERS come off (and with them the per-phase bulk menu — the
     * page's own Tune menu still marks the whole list).
     *
     * The headers are not decoration: "Phase 2" is a claim about where a row
     * sits in the viewing order, so A–Z rows filed under Phase 1/2/3 would be a
     * list sorted by title and captioned as though it were not. Dropping them is
     * the honest rendering of a title order; the line bands stay, because a
     * search's bands say which LINE a row came from, which stays true in any
     * order.
     */
    flat: Boolean,
    onOpen: (IncursionEntry) -> Unit,
    /** v428 — the row's own watch button (see [WatchButton]). */
    onWatch: (IncursionEntry) -> Unit,
    onBulk: (List<String>, IncursionStore.Status) -> Unit,
    onClear: (List<String>, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (flat) {
            for ((studio, entries) in sections) {
                if (showLineBand && sections.size > 1) {
                    item(key = "${studio.id}-flat-band") {
                        StudioBand(studio = studio, hits = entries.size)
                    }
                }
                items(entries, key = { it.storageKey }) { entry ->
                    EntryRow(
                        entry = entry,
                        onOpen = { onOpen(entry) },
                        onWatch = { onWatch(entry) }
                    )
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
            return@LazyColumn
        }
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
                    EntryRow(
                        entry = entry,
                        onOpen = { onOpen(entry) },
                        onWatch = { onWatch(entry) }
                    )
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
                    EntryRow(
                        entry = entry,
                        onOpen = { onOpen(entry) },
                        onWatch = { onWatch(entry) }
                    )
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
private fun EntryRow(
    entry: IncursionEntry,
    onOpen: () -> Unit,
    onWatch: () -> Unit = {}
) {
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
            // v428 — THE WATCH BUTTON, on every row.
            //
            // The member: *"add watch icon button in rows and grid"*, and, asked
            // what one tap should do, *"mark it Watched, tap again to undo"*. So
            // it is the ONLY quick action a row carries: one tap finishes a title
            // from the list, the second takes it back — the same two-tap truth the
            // six-state chips keep for everything finer than watched or not. It
            // sits before the state chip rather than replacing it, because the
            // chip says where the row IS and this says what to do with it.
            WatchButton(
                watched = status == IncursionStore.Status.WATCHED,
                accent = settingsRoseAccent(),
                title = entry.title,
                onClick = onWatch
            )
            Spacer(Modifier.width(6.dp))
            StatusChip(status = status)
        }
    }
}

/**
 * ONE TAP FINISHES A TITLE — or takes it back.
 *
 * A round button the size of the row's own chip, wearing the page's accent when
 * the row is watched and the plate's quiet fill when it is not, so a list can be
 * read down its right-hand edge for "what have I still got". Its mark changes
 * with the state, which is what makes the second tap read as UNDO rather than as
 * the same action twice.
 *
 * ── v429 — THE MARK IS AN EYE, AND IT IS DRAWN, NOT TYPED ────────────────
 *
 * Two reports built this. First the mark went missing — `CurioIcons.Visibility`,
 * whose ligature is NOT in the bundled Material Symbols subset (`visibility_off`
 * is there, `visibility` is not; measured against
 * `material_symbols_outlined.ttf`), so the button drew with nothing in it
 * ("the icon is invalid and not showing"). It was drawn by hand next, as a PLAY
 * TRIANGLE — which the member rejected on sight ("the watch button in incursion
 * ui is not right"): a play mark is an instruction to PLAY something, and this
 * button's whole job is to say the title has been SEEN. An eye is the mark that
 * means watching, so it is an eye:
 *
 *  · **unwatched** — a hairline eye: two shallow arcs meeting at the corners,
 *    with a small pupil between them;
 *  · **watched** — the same eye FILLED in the page's accent, its pupil punched
 *    out in the button's own colour (the pupil is a hole in the almond, not a
 *    second shape on top of it), so the state reads at a glance down a list.
 *
 * Drawn rather than typed for the reason the first report taught: a glyph the
 * font may or may not carry is a butt on that can vanish on a font update, and
 * this app already draws its own marks wherever the subset is thin (B / I / U / S,
 * the four alignments).
 */
@Composable
private fun WatchButton(
    watched: Boolean,
    accent: Color,
    title: String,
    onClick: () -> Unit
) {
    val shape = CircleShape
    // The button's own fill, kept in a value because the pupil is punched out in
    // exactly this colour (see below).
    val plate = MaterialTheme.colorScheme.surfaceContainerHigh
    val fill = if (watched) {
        curioTintOn(
            plate,
            accent,
            if (isCurioDarkTheme()) 0.30f else 0.18f
        )
    } else {
        plate
    }
    // Read OUT here: the mark is drawn in a DrawScope lambda, and a theme read
    // belongs to the composition that owns the button.
    val markInk = if (watched) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = shape,
        color = fill,
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .clickable(
                onClickLabel = if (watched) "Unwatch $title" else "Mark $title watched",
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val stroke = 1.7f.dp.toPx()
                val w = size.width
                val h = size.height
                val left = w * 0.06f
                val right = w * 0.94f
                val middle = h * 0.50f
                if (watched) {
                    val almond = Path().apply {
                        moveTo(left, middle)
                        quadraticBezierTo(w * 0.50f, h * 0.10f, right, middle)
                        quadraticBezierTo(w * 0.50f, h * 0.90f, left, middle)
                        close()
                    }
                    drawPath(almond, markInk)
                    // The pupil: the button's own fill, so it reads as a hole in
                    // the filled eye rather than as a dot resting on it.
                    drawCircle(fill, radius = w * 0.13f, center = Offset(w * 0.50f, middle))
                } else {
                    val lid = Path().apply {
                        moveTo(left, middle)
                        quadraticBezierTo(w * 0.50f, h * 0.12f, right, middle)
                    }
                    val floor = Path().apply {
                        moveTo(left, middle)
                        quadraticBezierTo(w * 0.50f, h * 0.88f, right, middle)
                    }
                    drawPath(lid, markInk, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    drawPath(floor, markInk, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    drawCircle(markInk, radius = w * 0.115f, center = Offset(w * 0.50f, middle))
                }
            }
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
    /** v429 — a sorted view is one flat run of tiles (see [IncursionList]). */
    flat: Boolean,
    onOpen: (IncursionEntry) -> Unit,
    /** v428 — the tile's own watch button (see [WatchButton]). */
    onWatch: (IncursionEntry) -> Unit,
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
        if (flat) {
            for ((studio, entries) in sections) {
                if (showLineBand) {
                    item(key = "${studio.id}-grid-flat-band", span = { GridItemSpan(maxLineSpan) }) {
                        StudioBand(studio = studio, hits = entries.size)
                    }
                }
                items(entries, key = { it.storageKey }) { entry ->
                    EntryTile(
                        entry = entry,
                        onOpen = { onOpen(entry) },
                        onWatch = { onWatch(entry) }
                    )
                }
            }
            return@LazyVerticalGrid
        }
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
                    EntryTile(
                        entry = entry,
                        onOpen = { onOpen(entry) },
                        onWatch = { onWatch(entry) }
                    )
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
                    EntryTile(
                        entry = entry,
                        onOpen = { onOpen(entry) },
                        onWatch = { onWatch(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryTile(
    entry: IncursionEntry,
    onOpen: () -> Unit,
    onWatch: () -> Unit = {}
) {
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
                Spacer(Modifier.height(9.dp))
                // v428 — THE SAME WATCH BUTTON THE ROWS CARRY, at the tile's own
                // foot: a grid is where a member picks what to start, so the one
                // quick action belongs on it too, and it is the same control and
                // the same two taps as the list's.
                WatchButton(
                    watched = status == IncursionStore.Status.WATCHED,
                    accent = settingsRoseAccent(),
                    title = entry.title,
                    onClick = onWatch
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

    // ── v428 — THE EPISODE GUIDE, THE SAME ONE THE SERIES SHEET SHOWS ──────
    //
    // The member: *"for series in incursion use what the series category bottom
    // sheet uses with episode guide"*. The series lane's sheet leads with the
    // poster and the synopsis and then hands over to the show's own episodes; an
    // Incursion row is a SEASON of a show ("Loki S2"), so without them the sheet
    // can only say the season exists. This is the reveal's own fetcher — TMDB when
    // a key is set (it is the only source that states outright whether a title is
    // a show), TVMaze keyless behind it — through the same `stripNaming` the
    // poster doors use, so "Loki S2" is looked up as "Loki".
    //
    // A preview already in memory is used as the seed (nothing is re-asked), and
    // the fetch runs once per title, on open.
    val series = entry.type.lowercase() == "series"
    var guide by remember(entry.storageKey) { mutableStateOf(SeriesEpisodeFetcher.cached(entry.title)) }
    var guideOpen by remember(entry.storageKey) { mutableStateOf(false) }
    LaunchedEffect(entry.storageKey) {
        // v429 — THE SEASON IS PASSED IN. A row is "Loki S2": the guide it wants
        // is that season's, and the hint lets a keyed door read ONE season
        // instead of walking four (the walk was most of the wait the member saw
        // as a guide that never loaded). The keyless door ignores it and answers
        // with the whole show, which is exactly what the series lane shows.
        if (series && guide == null) {
            guide = SeriesEpisodeFetcher.fetchForAny(entry.title, season = entry.season)
        }
    }
    // ── v429 — WHAT THE DOORS ADD TO THE ROW ────────────────────────────────
    //
    // The member: *"for incursion ui the movie posters description doesnt
    // fetch, use all the avalabel api"*. Upstream's own `desc` is present on some
    // rows and absent on many, so the sheet reads its record either way: the
    // chain in [IncursionSources] answers with a description where the row has
    // none and with the facts it gathered on the way (the rating, the runtime,
    // the genres), each door budgeted and the chain memoised per row.
    var record by remember(entry.storageKey) { mutableStateOf<IncursionSources.Record?>(null) }
    LaunchedEffect(entry.storageKey) {
        record = IncursionSources.record(entry)
    }
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

            // v429 — THE ROW'S OWN WORDS FIRST, THE FETCHED ONES BEHIND THEM.
            //
            // `entry.desc` is upstream's own synopsis and it is the app's voice
            // for this title, so it always wins; the chain's answer fills in
            // where the catalogue said nothing. Under the text sits what the
            // doors stated from the API that the row itself does not carry —
            // the community rating, the runtime, the genres — as the app's own
            // quiet pills, and only the facts that are actually there.
            val about = entry.desc?.takeIf { it.isNotBlank() } ?: record?.description
            val facts = record
            if (about != null || facts != null) {
                Column {
                    if (about != null) {
                        Text(
                            "ABOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            about,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val factsPills = buildList {
                        if (facts != null && facts.rating > 0f) {
                            add("★ " + String.format(java.util.Locale.US, "%.1f", facts.rating))
                        }
                        if (facts != null && facts.runtime > 0 && entry.runtime == null) {
                            add("${facts.runtime} min")
                        }
                        if (facts != null && facts.rated.isNotBlank() && entry.ageRating.isNullOrBlank()) {
                            add(facts.rated)
                        }
                        if (facts != null && facts.genres.isNotEmpty()) {
                            add(facts.genres.take(3).joinToString(" · "))
                        }
                    }
                    if (factsPills.isNotEmpty()) {
                        Spacer(Modifier.height(if (about != null) 9.dp else 0.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            factsPills.forEach { pill ->
                                Surface(
                                    shape = pillShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        pill,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
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

            // ── THE SHOW'S OWN EPISODES (v428) ─────────────────────────────
            if (series) {
                val list = guide
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "EPISODE GUIDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        if (list != null && list.isNotEmpty()) {
                            val seasons = list.map { it.season }.distinct().size
                            Text(
                                buildString {
                                    append("${list.size} episode")
                                    if (list.size != 1) append("s")
                                    if (seasons > 1) append(" · $seasons seasons")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    when {
                        list == null -> Text(
                            "Reading the guide…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        list.isEmpty() -> Text(
                            "No episode guide for this one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> {
                            // The first half-season is shown at a glance; the rest
                            // is one tap away, because a long runner's guide is
                            // hundreds of rows and the sheet's job is the title.
                            val shown = if (guideOpen) list else list.take(EPISODE_PREVIEW)
                            // A season heading only when the guide spans seasons:
                            // on a single-season show it would be the same word over
                            // every row.
                            val multiSeason = list.map { it.season }.distinct().size > 1
                            shown.forEachIndexed { index, episode ->
                                val newSeason = index == 0 || shown[index - 1].season != episode.season
                                if (multiSeason && newSeason) {
                                    Text(
                                        "SEASON ${episode.season}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            top = if (index == 0) 0.dp else 9.dp,
                                            bottom = 3.dp
                                        )
                                    )
                                }
                                EpisodeRow(episode = episode, accent = accent)
                            }
                            if (list.size > EPISODE_PREVIEW) {
                                Spacer(Modifier.height(7.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { guideOpen = !guideOpen }
                                ) {
                                    Text(
                                        if (guideOpen) "Show less"
                                        else "Show all ${list.size} episodes",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
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

// ── The episode guide ───────────────────────────────────────────────────────

/** How many episodes a series' sheet shows before the rest is asked for. */
private const val EPISODE_PREVIEW = 12

/**
 * ONE EPISODE — its own number, its name and the day it aired.
 *
 * The reveal's series sheet draws this as a card per episode; inside a title's
 * sheet (which already leads with a poster, a synopsis and the member's own six
 * states) one quiet line per episode is what fits, and it is the same information
 * in the same order: `S2E3`, the episode's name, the airdate.
 */
@Composable
private fun EpisodeRow(episode: SeriesEpisode, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            "S${episode.season}E${episode.number}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
        Spacer(Modifier.width(9.dp))
        Text(
            episode.title.ifBlank { "Episode ${episode.number}" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        episode.airdate.takeIf { it.isNotBlank() }?.let { air ->
            Spacer(Modifier.width(8.dp))
            Text(
                air,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

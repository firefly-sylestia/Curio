package com.curio.app.features.personal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// v455 phase 4 — the rows' entrance (see the list below).
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalMood
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.wordCount
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.rememberCurioPressSource
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.curioItemIn
import com.curio.app.ui.theme.rememberCurioArrivals
import com.curio.app.ui.theme.curioCardShadow
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v387 — THE JOURNAL COLLECTION.
 *
 * Every page the member has written, newest day first, in the journal's own
 * small view: the day, how it felt, the title they gave it and the first line
 * of the writing. It is deliberately NOT the saved-entry list — no topic, no
 * format badge, no capture card.
 */
@Composable
fun JournalListScreen(navController: NavController) {
    // ── v389e — DAYS ONLY ─────────────────────────────────────────────
    //
    // The personal store keeps journals, notes on topics and to-do lists in one
    // table, and this list used to show all three — a to-do list sitting among
    // the days it is not, and a note about a topic in a list about a day's
    // feelings. The list is the JOURNAL's: the pages that are neither live in the
    // Notes collection in the Cabinet (user request: "the journal in personal
    // keep sthe journals only … also todo goes inside notes too, no more in
    // journa").
    val pages by produceState(initialValue = emptyList<PersonalNoteEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeJournals().collect { value = it }
        }
    }
    val journals = remember(pages) {
        pages.filter { !it.isTodo && !it.hasTopic }
    }
    var pendingDelete by remember { mutableStateOf<PersonalNoteEntity?>(null) }
    val scope = rememberCoroutineScope()

    // ── v440 — FINDING A PAGE, AND THE ORDER THEY SIT IN ────────────────
    //
    // The member asked for both in one breath: *"add search for journals and also
    // sorting by date by tapping the date in journals date"*. A collection that is
    // one page per day grows into hundreds of days, and until now the only way to
    // a page was to scroll to its month.
    //
    // The search reads what a ROW already shows (its title, its opening line, the
    // topic it is about, its mood, and its own date line) — it never decodes a
    // document, so a keystroke cannot cost a JSON parse per page (see the row's own
    // note on `doc`).
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    //
    // ── v440 — AND WHAT KIND OF PAGE, NOT JUST WHAT IT SAYS (the member's own
    // pick from the settings list: *"Filter the list by mood, colour or length"*).
    //
    // Two of the three are COLUMNS the row already carries (`mood`, `accentArgb`),
    // so they cost nothing at all to filter by. Length is the one that has to be
    // counted, and a word count only exists inside the page's own document — so it
    // is counted ONCE, off the main thread, and only while a length is actually
    // being filtered for (`lengths` below). Counting every page on every keystroke
    // is exactly the kind of thing that makes a long list stutter (see [answers]).
    //
    // ── v440 — AND TODAY'S OWN COUNT, WHEN A GOAL EXISTS ─────────────
    //
    // The other half of the member's *"Word count goal with a daily reminder"*: a
    // goal the member can only see in Settings is a goal they have to remember to
    // go and check, so the collection shows today's progress where the day's pages
    // are. **Only the day's own pages are counted** (usually one), and only while a
    // goal is set — a word count means decoding a document, so counting the whole
    // collection the way the length filter does would be paying for an answer to a
    // question nobody asked.
    val context = LocalContext.current
    val journalGoal = remember { AppPreferences.getJournalGoal(context) }
    val todayWords by produceState(0, journals, journalGoal) {
        value = if (journalGoal <= 0) 0
        else {
            val today = startOfToday()
            withContext(Dispatchers.Default) {
                journals.filter { it.dateMillis == today }
                    .sumOf { page -> page.doc.wordCount() }
            }
        }
    }
    // ── v440 — WHERE THIS LIST'S OWN DOOR LANDS ─────────────────────
    //
    // The member's own pick (*"'First page of the day' preference (today's page vs
    // the last one you opened)"*), resolved in one place so the floating "+" and
    // the empty state's own action can never disagree about it.
    //
    // ── v440b — AND TODAY'S OWN PAGE IS ALREADY IN HAND ─────────────
    //
    // The list HAS every journal (it is the query it is drawn from), so the page
    // for today is a lookup in memory rather than a second trip to the database.
    // That matters: the first cut of this door asked the database before navigating
    // and the journal took a beat to open (member: *"journal opening is clanky
    // too"*) — a door has to answer on the tap.
    val todayEntryId = remember(journals) {
        val today = startOfToday()
        journals.firstOrNull { page -> page.dateMillis == today }?.id
    }
    val journalDoor = rememberJournalDoor(navController, todayEntryId)
    var moodFilter by remember { mutableStateOf<PersonalMood?>(null) }
    // ── v453 — THE COLOUR FILTER IS GONE ────────────────────────────────
    //
    // The member: *"fix the journals page filtering, and remove sorting by
    // color"*. Filtering the collection by the colour a page was given was a
    // row of hue chips that answered a question nobody asks while looking for a
    // day they wrote ("which cafe was the ochre one?") and cost a third of the
    // panel — three scrolling rows of chips over a list. HOW THE DAY FELT and HOW
    // MUCH WAS WRITTEN are the two things a member actually remembers about a
    // page, and they keep their rows. The colour is still ON every row's spine and
    // in each page's own tools; it is simply no longer a way to hide pages.
    var lengthFilter by remember { mutableStateOf(JournalLength.ANY) }
    var filtersOpen by remember { mutableStateOf(false) }
    //
    // NEWEST FIRST is the default, which is what the list has always been: the
    // latest day at the top. The head's date pill orders it (see [PersonalHeaderDate]).
    var newestFirst by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    val needle = query.trim().lowercase()
    val filtersOn = moodFilter != null || lengthFilter != JournalLength.ANY
    val lengths by produceState<Map<String, Int>>(emptyMap(), journals, lengthFilter) {
        value = if (lengthFilter == JournalLength.ANY) emptyMap()
        else withContext(Dispatchers.Default) {
            journals.associate { page -> page.id to page.doc.wordCount() }
        }
    }
    val matched = remember(journals, needle, moodFilter, lengthFilter, lengths) {
        journals.filter { page ->
            (needle.isEmpty() || page.answers(needle)) &&
                (moodFilter == null || page.moodEnum == moodFilter) &&
                (lengthFilter == JournalLength.ANY ||
                    lengthFilter.holds(lengths[page.id] ?: 0))
        }
    }
    // The order is decided HERE rather than in the query so the door can reverse
    // it without a second trip to the database. `writtenAtMillis` is the
    // tiebreaker — NOT `updatedAtMillis`: a page edited late does not climb its
    // own day (member: "in journals view dont update the time if its edited again
    // late").
    val ordered = remember(matched, newestFirst) {
        val by = compareBy<PersonalNoteEntity> { it.dateMillis }
            .thenBy { it.writtenAtMillis() }
        matched.sortedWith(if (newestFirst) by.reversed() else by)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
        PersonalHeader(
            title = "Journals",
            subtitle = when {
                // A FIND says what it found, out of what there is: a count of
                // the whole collection while a search or a filter is on is a
                // number the member cannot use.
                needle.isNotEmpty() || filtersOn ->
                    "${matched.size} of ${journals.size} pages"
                journals.size == 0 -> "A page for a day"
                journals.size == 1 -> "1 page"
                else -> "${journals.size} pages"
            },
            onBack = { navController.popBackStack() },
            // v389 — no "Write today" pill: the floating + already writes today's
            // page, and a second door in the head only crowded the title. The
            // head wears TODAY instead, which is the one date a journal is
            // about (user request).
            //
            // v440 — AND THE DATE IS THE ORDER'S DOOR (member: "sorting by date by
            // tapping the date in journals date"): a tap says which end of the
            // collection to read from, and the pill's own arrow says which end it
            // is showing.
            action = {
                PersonalHeaderDate(
                    onToggleSort = { newestFirst = !newestFirst },
                    newestFirst = newestFirst
                )
            }
        )

        // ── v440 — THE SEARCH, AS A PILL THAT OPENS INTO A FIELD ────────
        //
        // Closed it is one quiet pill under the head, so a collection nobody is
        // searching still reads as a list of days; opened it grows into the field
        // and the pill IS the field (the same shape the reader's own search wears —
        // a door that becomes the thing it opened).
        JournalSearchPill(
            open = searchOpen,
            query = query,
            onOpen = { searchOpen = true },
            onQuery = { query = it },
            onClose = {
                searchOpen = false
                query = ""
                focusManager.clearFocus()
            },
            onToggleFilters = { filtersOpen = !filtersOpen },
            filtersOpen = filtersOpen,
            filtersActive = filtersOn
        )

        // ── v440 — TODAY, AGAINST THE GOAL ──────────────────────────────
        //
        // One line, under the find row, and only when a goal is set: the count,
        // the goal, and a rail that fills — the same rail the reader's progress
        // card uses, so "how much of this have I done" reads the same way in both
        // halves of the app. It turns the accent the moment the goal is met, which
        // is the whole reward a writing goal gives.
        if (journalGoal > 0) {
            val met = todayWords >= journalGoal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    if (met) "Today: $todayWords words \u00b7 goal met"
                    else "Today: $todayWords of $journalGoal words",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (met) personalAccentInk()
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                (todayWords.toFloat() / journalGoal.toFloat()).coerceIn(0f, 1f)
                            )
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(personalAccent())
                    )
                }
            }
        }

        // ── v440 — THE FILTER PANEL ─────────────────────────────────────
        //
        // Three rows of capsules, one question each: how it felt, what colour the
        // day wears, and how much was written. Each row opens with its own "any"
        // chip, so the way out of a filter is always in the same place as the way
        // in, and every row scrolls sideways rather than wrapping — a panel over a
        // list must not push the list down the screen to say itself.
        AnimatedVisibility(
            visible = filtersOpen,
            enter = CurioMotion.pillArrive(fromTop = true),
            exit = CurioMotion.pillLeave(fromTop = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // v453 — TWO ROWS, AND EACH ONE SAYS WHAT IT FILTERS. Three
                // unlabelled rows of chips left the member to work out which row
                // was mood and which was length by tapping them; a quiet label
                // in front of each row answers that before the first tap.
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JournalFilterLabel("Felt")
                    JournalFilterChip(
                        label = "Any",
                        live = moodFilter == null,
                        onClick = { moodFilter = null }
                    )
                    PersonalMood.entries.forEach { mood ->
                        JournalFilterChip(
                            label = mood.label,
                            live = moodFilter == mood,
                            onClick = { moodFilter = mood },
                            glyph = personalMoodGlyph(mood)
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JournalFilterLabel("Length")
                    JournalLength.entries.forEach { length ->
                        JournalFilterChip(
                            label = length.label,
                            live = lengthFilter == length,
                            onClick = { lengthFilter = length }
                        )
                    }
                }
            }
        }

        if (journals.isEmpty()) {
            PersonalEmptyCard(
                glyph = CurioIcons.Note,
                title = "Nothing written yet",
                body = "A journal page is a day you decided to keep: how it felt, " +
                    "what happened, what you want to remember.",
                actionLabel = "Write today's page",                        onAction = journalDoor
            )
        } else if (ordered.isEmpty()) {
            // ── v440 — A SEARCH THAT FOUND NOTHING ──────────────────────
            //
            // The one empty state that is NOT a bare dash: a filtered list has a
            // CAUSE the member needs told (see [com.curio.app.ui.components.CurioEmptyLine]),
            // because an empty pane with no words in it reads as a broken search.
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Nothing matches",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    // v440 — a find can come up empty because of a WORD or because
                    // of a FILTER, and the sentence has to name the one the member
                    // can actually change (a filter they forgot is the likelier of
                    // the two).
                    when {
                        needle.isNotEmpty() ->
                            "No page in your journals has \u201C${query.trim()}\u201D in it."
                        else -> "No page in your journals answers every filter at once."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            // Grouped by MONTH: a journal is a run of days, and a month
            // heading turns a flat list of entries into a book's chapters. The
            // grouping follows the ORDER above, so reversing the list reverses the
            // months with it (a `groupBy` keeps insertion order).
            val months = ordered.groupBy { journal ->
                journal.dateMillis.toLocalDate().withDayOfMonth(1)
            }
            // v455 phase 4 — the days arrive, and only once each: the list's
            // own arrivals record is what stops a row re-animating when it is
            // scrolled back to (see CurioArrivals).
            val journalArrivals = rememberCurioArrivals()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                months.forEach { (month, pages) ->
                    item(key = "month-$month") { MonthHead(month = month) }
                    itemsIndexed(items = pages, key = { _, j -> j.id }) { index, journal ->
                        JournalRow(
                            journal = journal,
                            modifier = Modifier.curioItemIn(
                                key = journal.id,
                                order = index,
                                arrivals = journalArrivals
                            ),
                            // v389 — a row opens ITS OWN page: a journal day the
                            // editor, a to-do list the checklist page, a note on a
                            // topic the topic page. See personalRouteFor.
                            onClick = {
                                navController.navigate(personalRouteFor(journal)) {
                                    launchSingleTop = true
                                }
                            },
                            onLongPress = { pendingDelete = journal }
                        )
                    }
                }
                item("tail") { Spacer(Modifier.height(60.dp)) }
            }
        }
        }
        PersonalCreateLauncher(
            visible = true,
            onClick = journalDoor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 18.dp)
        )
    }

    pendingDelete?.let { journal ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this page?") },
            text = {
                Text(
                    "The ${journal.dateMillis.prettyDate()} entry and everything written " +
                        "in it leaves your journals. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = journal.id
                    pendingDelete = null
                    scope.launch { withContext(Dispatchers.IO) { runCatching { PersonalRepositoryHolder.repo.deleteNote(id) } } }
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Keep") }
            }
        )
    }
}

/** A month heading — the serif month with the year behind it, and a hairline
 *  running to the edge so the list reads as one bound volume. */
@Composable
private fun MonthHead(month: java.time.LocalDate) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            month.month.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = personalIconTint(personalAccent())
        )
        Text(
            month.year.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ink.copy(alpha = 0.4f)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(ink.copy(alpha = 0.08f))
        )
    }
}

/** One journal in the collection: the day, the mood, the title, the opening
 *  line and how much is on the page. Tap opens it, a long press offers the
 *  removal — the same habits as every other Curio list. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JournalRow(
    journal: PersonalNoteEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    // v455 phase 4 — the row's arrival (the motion system only; inert
    // otherwise). See CurioMotionSystem.curioItemIn.
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val mood = journal.moodEnum
    // Decoding a body is only for the row that is actually measuring words:
    // `doc` re-parses the stored JSON on every access, so it is read ONCE per
    // version of the entry rather than on every recomposition of the list.
    val words = remember(journal.id, journal.updatedAtMillis) { journal.doc.wordsLabel() }
    val press = rememberCurioPressSource(pressedScale = 0.985f)
    // v428 — a page the member gave a colour to wears it HERE, on the spine: the
    // list stays one notebook, but a coloured day is findable down the margin
    // without opening it (see [journalDoorAccent]). Resolved in the COMPOSABLE
    // scope, never inside the draw lambda below: a `drawBehind` block is a DRAW
    // pass where no @Composable may be called, so the colour is read once per row
    // here and captured there.
    val spine = journalDoorAccent(journal.accentArgb)
    Surface(
        shape = RoundedCornerShape(20.dp),
        // v411 — the journal's OWN paper (a warm parchment tinted with the
        // member's accent), lifted by the soft shadow: a page in the
        // collection sits ON the page instead of being outlined into it.
        color = journalPaper(),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .curioCardShadow(RoundedCornerShape(20.dp))
            .then(press.modifier)
            .combinedClickable(
                interactionSource = press.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                // The margin rule: every page in the collection wears the same
                // accent spine, so the list reads as one notebook.
                .drawBehind {
                    val barWidth = 3.dp.toPx()
                    drawRoundRect(
                        color = spine,
                        size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                    )
                }
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(min = 54.dp)
            ) {
                // v389e — the day is the page's own ink, not the accent's lighter
                // shade: it is the one figure a row exists to say (user report:
                // "for journal number date its too accent color and very light
                // colored so fix it by making it dark").
                Text(
                    journal.dateMillis.toLocalDate().dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
                )
                Text(
                    journal.dateMillis.toLocalDate()
                        .month.name.lowercase().take(3)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f)
                )
                if (mood != null) {
                    Spacer(Modifier.height(4.dp))
                    CurioIcon(
                        personalMoodGlyph(mood),
                        mood.label,
                        tint = ink.copy(alpha = 0.6f),
                        size = 15.dp
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    journal.title.ifBlank { "Untitled day" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (journal.title.isBlank()) ink.copy(alpha = 0.5f) else ink,
                    // Two lines at most: a long title turned a row into a wall
                    // of words and pushed the preview and the count off it
                    // (user request).
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // v389 — a LIST is previewed as a list: its own rows with their
                // ticks, rather than its text run into a paragraph where a
                // finished row and an unfinished one read alike (user request:
                // "its preview as a separate todo preview not inside the
                // journal"). Everything else previews as prose exactly as before.
                if (journal.isTodo) {
                    Spacer(Modifier.height(6.dp))
                    ChecklistPreview(doc = journal.doc, ink = ink)
                } else if (journal.preview.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        journal.preview,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LoraFontFamily),
                        color = ink.copy(alpha = 0.68f),
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(6.dp))
                // ── v440 — AND THE MOMENT IT WAS WRITTEN ────────────────
                //
                // The member: *"for journal ad time note too its only note date"*.
                // It rides the metadata line the row already had, beside the word
                // count, rather than adding a line to the date column: the day is
                // what the column is for, and the moment belongs with the facts
                // about the page.
                //
                // **It is [writtenAtMillis] — the time the page was WRITTEN.** It
                // does not move when the page is edited later, which is the second
                // half of the same request.
                Text(
                    "$words \u00b7 ${journal.writtenAtMillis().prettyTime()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.45f)
                )
            }
        }
    }
}

/**
 * v440 — WHEN A PAGE WAS WRITTEN, not when it was last touched.
 *
 * The member: *"in journals view dont update the time if its edited again late"*.
 * [PersonalNoteEntity.createdAtMillis] is stamped once by `saveNote` and preserved
 * on every later write, so it IS the moment the page came into being — while
 * `updatedAtMillis` moves every time a word changes, which is exactly what must not
 * show in a list (a page written at nine in the morning and corrected at midnight
 * would claim midnight).
 *
 * The fallback is for rows written before v389 stamped a creation time: those have
 * only `updatedAtMillis`, and showing nothing at all would be worse than showing the
 * only stamp they have.
 */
private fun PersonalNoteEntity.writtenAtMillis(): Long =
    if (createdAtMillis > 0L) createdAtMillis else updatedAtMillis

/**
 * v440 — WHETHER A PAGE ANSWERS A SEARCH.
 *
 * It reads what a ROW already shows and nothing else: the title, the stored opening
 * line, the topic the page is about, the mood, and the page's own date line (so
 * "wednesday", "16" or "sept" find a day). **No document is decoded** — a search
 * that parsed every page's JSON per keystroke is a search that stutters on a
 * collection of a few hundred days (see [JournalRow]'s own note on `doc`).
 */
private fun PersonalNoteEntity.answers(needle: String): Boolean =
    title.lowercase().contains(needle) ||
        preview.lowercase().contains(needle) ||
        topicName.lowercase().contains(needle) ||
        (moodEnum?.label.orEmpty()).lowercase().contains(needle) ||
        dateMillis.prettyDate().lowercase().contains(needle)

/**
 * v440 — THE SEARCH, AS A PILL THAT BECOMES A FIELD.
 *
 * Closed, it is one quiet pill under the head: a collection nobody is searching
 * still reads as a list of days rather than as a toolbar. Tapped, it IS the field —
 * the same trick the reader's own search plays on its head (a door that becomes the
 * thing it opened, on the one motion clock), so the app has one search shape rather
 * than two.
 *
 * It takes focus as it opens, because a member who tapped it has already said they
 * are going to type.
 */
@Composable
private fun JournalSearchPill(
    open: Boolean,
    query: String,
    onOpen: () -> Unit,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    /** v440 — the FILTER door, at the end of the same row (see [JournalFindRow]). */
    onToggleFilters: (() -> Unit)? = null,
    /** Whether the filter panel is down — the door wears it. */
    filtersOpen: Boolean = false,
    /** Whether anything is actually filtering, so the door can say so. */
    filtersActive: Boolean = false
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val focus = remember { FocusRequester() }
    LaunchedEffect(open) { if (open) runCatching { focus.requestFocus() } }
    // ── v440 — ONE ROW, TWO DOORS ───────────────────────────────────
    //
    // The filter door sits at the END of the search's own row rather than on a
    // row of its own: a second strip of chrome over a list of days is exactly the
    // "two stacked rows" the member rejected in the writing dock (§7.4), and the
    // two doors ask the same kind of question ("what am I looking at").
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = open,
            transitionSpec = { CurioMotion.pillArrive() togetherWith CurioMotion.pillLeave() },
            label = "journal-search"
        ) { searching ->
            if (searching) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        placeholder = { Text("Search your journals") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focus)
                    )
                    Surface(
                        onClick = onClose,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CurioIcon(
                                CurioIcons.Close,
                                "Close the search",
                                tint = ink.copy(alpha = 0.7f),
                                size = 18.dp
                            )
                        }
                    }
                }
            } else {
                Surface(
                    onClick = onOpen,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        CurioIcon(
                            CurioIcons.Search,
                            null,
                            tint = personalAccentInk(),
                            size = 15.dp
                        )
                        Text(
                            "Search your journals",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ink.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        if (onToggleFilters != null) {
            // A round door, in the ⋯-tile language the app settles on: the glyph
            // carries it, and it wears the accent when the list is actually
            // filtered — so a member who filtered a week ago and forgot can see
            // that the list they are scrolling is not the whole collection.
            Surface(
                onClick = onToggleFilters,
                shape = CircleShape,
                color = if (filtersActive || filtersOpen) personalAccent().copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.Tune,
                        if (filtersOpen) "Hide the filters" else "Filter your journals",
                        tint = if (filtersActive || filtersOpen) personalAccentInk()
                        else ink.copy(alpha = 0.7f),
                        size = 19.dp
                    )
                }
            }
        }
    }
}

/**
 * v440 — WHERE A JOURNAL DOOR LANDS (see `AppPreferences.isJournalOpenToday`).
 *
 * Two honest positions for the same door. **Today's page** (the default, and what
 * the door has always done) opens the day already written if there is one — so a
 * member who taps "+" to add a line to this morning's page gets THIS MORNING'S
 * PAGE rather than a second page for the same day — and writes a new one when the
 * day is still blank. **The last page you opened** returns to the piece you left
 * half-written.
 *
 * **TODAY'S PAGE IS HANDED IN, NOT FETCHED** ([todayEntryId]) — the journalling list
 * already holds every page it draws, so the lookup is a scan of a list in memory and
 * the door navigates on the tap. **A door that awaits a query is a door that feels
 * broken**: the first cut of this asked the database first and the journal took a beat
 * to open (member: *"journal opening is clanky too"*). The context is hoisted above the
 * lambda because the door is an onClick and a lambda is not a composable scope (the
 * v439 rule).
 */
@Composable
internal fun rememberJournalDoor(
    navController: NavController,
    /** Today's journal page, when one exists — a scan of the list, never a query. */
    todayEntryId: String? = null
): () -> Unit {
    val context = LocalContext.current
    return remember(navController, todayEntryId) {
        {
            if (AppPreferences.isJournalOpenToday(context)) {
                openJournal(navController, todayEntryId ?: CurioRoutes.PERSONAL_NEW)
            } else {
                openJournal(
                    navController,
                    AppPreferences.getLastJournalId(context).ifBlank { CurioRoutes.PERSONAL_NEW }
                )
            }
        }
    }
}

/** One way in, so both halves of the door push the same route. */
private fun openJournal(navController: NavController, entryId: String) {
    navController.navigate(CurioRoutes.journalEditor(entryId)) { launchSingleTop = true }
}

/**
 * v453 — WHAT A FILTER ROW FILTERS.
 *
 * Two quiet words in front of two chip rows. The rows used to be three unlabelled
 * scrolls of capsules, so which one held the moods and which held the lengths had to
 * be discovered by tapping; a label costs 4dp of the row and answers it before the
 * member touches anything. It is not a heading — it is the row's own name, at the
 * ink and the weight of the chips' resting state, so the eye reads the chips first.
 */
@Composable
private fun JournalFilterLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        maxLines = 1,
        modifier = Modifier.padding(start = 2.dp, end = 2.dp)
    )
}

/**
 * v440 — ONE CHOICE IN THE FIND ROW, in the app's own capsule language.
 *
 * The same chip the reader's gestures box uses ([ZoneChip]'s shape): transparent
 * until it is live, an accent wash when it is, and the label weight says which —
 * so a member can read the state of a filter without a legend explaining it.
 */
@Composable
private fun JournalFilterChip(
    label: String,
    live: Boolean,
    onClick: () -> Unit,
    glyph: String? = null
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (live) personalAccent().copy(alpha = 0.18f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (glyph != null) {
                CurioIcon(
                    glyph,
                    null,
                    tint = if (live) personalAccentInk() else ink.copy(alpha = 0.6f),
                    size = 14.dp
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (live) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (live) personalAccentInk() else ink.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}


/**
 * The lengths a journal is filed under (see the filter panel in [JournalListScreen]).
 *
 * Three buckets rather than a slider: the question a member is asking of a
 * collection of days is "where are the long ones" / "where did I only manage a
 * line", and a range would be a second thing to set before the question is
 * answered. The thresholds are a page's worth of writing rather than a word count
 * pulled from anywhere: under a paragraph, a page, and more than a page.
 */
private enum class JournalLength(val label: String) {
    ANY("Any length"),
    SHORT("A few lines"),
    PAGE("About a page"),
    LONG("Longer");

    fun holds(words: Int): Boolean = when (this) {
        ANY -> true
        SHORT -> words < 120
        PAGE -> words in 120..399
        LONG -> words >= 400
    }
}

/**
 * The personal family's header — shared by journals, the shelf and a book,
 * so the whole system wears one identity that is nobody else's: the page
 * background, a serif title, one pill action.
 */
@Composable
internal fun PersonalHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    action: @Composable (() -> Unit)? = null,
    /**
     * v389 — THE ROLLING TITLE (a book's page).
     *
     * A page that repeats its own title in the head AND in the body shows the
     * same words twice before anything is scrolled (user report: "in book also
     * in header it shows the title and then below too"). So the head's title is
     * a ROLL-UP: it is invisible while the page's own title is on screen and
     * comes in as that one scrolls under the head — which is what a reader
     * means by "where am I" once the page's own title is gone. The line is
     * always LAID OUT (only its ink moves), so nothing below it can jump.
     */
    titleRevealed: Boolean = true,
    /**
     * v389 — WHAT THE HEAD SAYS WHILE THE PAGE'S OWN TITLE IS STILL ON SCREEN.
     *
     * Empty (the default) is right for a page with no context to name. A book's
     * page names its SHELF: until the book's own title and author have scrolled
     * under the head there is nothing about the book for the head to add, and
     * "Your shelf" is where the member actually is (user request: "instead of
     * initial blank say your shelf").
     */
    idleTitle: String = ""
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(42.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(CurioIcons.ArrowBack, "Back", tint = ink, size = 20.dp)
            }
        }
        // ONE, not two: the head says one thing at a time, and it ROLLS — the
        // idle line and the rolled one are the same two rows, so the swap is
        // the words changing and the head never changes size. The AUTHOR comes
        // in with the title (user request: "when i scroll away the title appears
        // in header make the same for title author too not just title"): before
        // the roll the page is still saying both of them itself, in the middle
        // of the screen, at a size the head cannot match.
        AnimatedContent(
            targetState = titleRevealed,
            transitionSpec = {
                // v439 — the head's title rolling up into it is the same arrival
                // as every other floating pill (see [CurioMotion]).
                CurioMotion.pillArrive(fromTop = true) togetherWith
                    CurioMotion.pillLeave(fromTop = true)
            },
            label = "personal-header-roll",
            modifier = Modifier.weight(1f)
        ) { rolled ->
            if (!rolled && idleTitle.isNotBlank()) {
                // ── v421 — THE IDLE LINE IS A PAGE HEADING, NOT A PLACEHOLDER ──
                //
                // The idle line used to be the ROLLED line's exact slot: the
                // same `headlineSmall`, with the subtitle's own line left in
                // place but transparent to hold the head's height. Stacked that
                // way the words sat ABOVE the head's centre — level with the top
                // of the back pill rather than with the pill itself — and, being
                // the same size as the title they stand in for, they read as a
                // small grey ghost of it (member: "the your shelf text isnt
                // properly bigger and not properly aligned").
                //
                // So the idle state is its own layout: ONE line, one step up the
                // scale (`headlineMedium`), centred inside a box whose height is
                // EXACTLY the two-line slot the rolled state occupies. The head
                // therefore does not change size when the roll happens, and the
                // idle words line up with the back pill.
                val stackHeight = with(LocalDensity.current) {
                    // headlineSmall's 32sp line + labelMedium's 16sp line: the
                    // rolled state's own two rows, resolved in dp so a larger
                    // system font scale grows the box with the words instead of
                    // clipping them.
                    (32.sp).toDp() + (16.sp).toDp()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stackHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        idleTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ink.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        if (rolled) title else idleTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (rolled) ink else ink.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        // The line HOLDS ITS HEIGHT while it is out of the way, so
                        // the head is one size whatever it is saying.
                        color = if (rolled) ink.copy(alpha = 0.5f) else Color.Transparent,
                        maxLines = 1
                    )
                }
            }
        }
        action?.invoke()
    }
}

/**
 * v389 — the personal family's head wears TODAY where a pill action used to be.
 * A journal collection and a book shelf are both "what I am doing now", and the
 * date says that without a button: the doors to a new page are the floating `+`
 * (journals, shelf) and the empty state's own action (user request — the
 * "Write today" / "Add a book" pills left both heads).
 */
@Composable
internal fun PersonalHeaderDate(
    dateMillis: Long = System.currentTimeMillis(),
    /**
     * v440 — WHEN SET, THE DATE IS A DOOR THAT ORDERS THE LIST.
     *
     * The member: *"sorting by date by tapping the date in journals date"*. The
     * shelf leaves it null and keeps the plain pill it always had; the journals
     * list hands it a toggle, and the pill grows an arrow that says which end of
     * the collection is at the top.
     */
    onToggleSort: (() -> Unit)? = null,
    /** Which end is at the top right now (see [onToggleSort]). */
    newestFirst: Boolean = true
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Surface(
        onClick = { onToggleSort?.invoke() },
        // A plain label on the shelf (an `enabled = false` Surface takes no
        // presses and draws no ripple), a real door on the journals list.
        enabled = onToggleSort != null,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                CurioIcons.CalendarToday,
                null,
                tint = personalAccentInk(),
                size = 15.dp
            )
            Text(
                dateMillis.prettyDate(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // ── v440 — AND WHERE THE LIST BEGINS ────────────────────────
            //
            // An arrow at the pill's far edge: DOWN for newest-first (the list
            // descends from today) and UP for oldest-first, with the tooltip saying
            // it in words. Only on a pill that is a door — the shelf's label grows
            // nothing.
            if (onToggleSort != null) {
                CurioIcon(
                    if (newestFirst) CurioIcons.ArrowDownward else CurioIcons.ArrowUpward,
                    if (newestFirst) "Newest first" else "Oldest first",
                    tint = personalAccentInk(),
                    size = 15.dp
                )
            }
        }
    }
}

@Composable
internal fun PersonalHeaderAction(
    glyph: String,
    label: String,
    onClick: () -> Unit
) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = accent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(glyph, null, tint = personalOnAccent(), size = 17.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = personalOnAccent()
            )
        }
    }
}

@Composable
internal fun PersonalEmptyCard(
    glyph: String,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val ink = journalInk()
    Surface(
        shape = RoundedCornerShape(24.dp),
        // v411 — the journal's paper and the soft elevation, same as a page.
        color = journalPaper(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .curioCardShadow(RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                // v412 — opaque: the accent is mixed into the journal paper
                // rather than laid over it translucently.
                color = lerp(journalPaper(), personalAccent(), 0.24f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(glyph, null, tint = personalIconTint(personalAccent()), size = 24.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LoraFontFamily, fontSize = 14.sp),
                color = ink.copy(alpha = 0.66f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            PersonalHeaderAction(glyph = CurioIcons.Add, label = actionLabel, onClick = onAction)
        }
    }
}

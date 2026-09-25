package com.curio.app.features.personal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newPersonalBookId
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.PendingJournalDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily

/**
 * v387 — THE PERSONAL FAMILY ON HOME.
 *
 * Two pieces live here, both belonging to Home rather than to the journals:
 *
 *  1. [PersonalCreateLauncher] — the floating "+" that opens the writing
 *     sheet. It is a fixed-size accent disc that slips away while the page is
 *     being scrolled DOWN and comes back the moment the finger goes up (or
 *     the page reaches the top), so a long read is never covered by a button
 *     nobody asked for mid-scroll.
 *  2. [PersonalChipsRow] — the journals and books as small, fixed-shape
 *     chips in a horizontal row, each one opening its own page directly.
 *     They wear the app's own accent tokens (never the capture paper's
 *     creams), so the personal writing reads as part of Home's furniture
 *     rather than as a saved capture.
 */

/** The fixed chip geometry — the chips never grow, so the row is stable
 *  whatever the writing contains. */
private val CHIP_WIDTH = 96.dp
private val CHIP_HEIGHT = 118.dp

/**
 * v389 — WHERE A ROW'S CONTENT MAY BEGIN: past the pinned door and the air
 * around it. The door holds the left edge of its row (see [PinnedDoorRow]), so
 * the chips start HERE and slide back under the door as the row is dragged.
 */
private val DOOR_GUTTER = 16.dp + CHIP_WIDTH + 10.dp

/**
 * The floating create button. [visible] is owned by the caller (the page's
 * scroll direction), the tap is the sheet's.
 */
@Composable
fun PersonalCreateLauncher(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = personalAccent()
    val progress = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            if (visible) 1f else 0f,
            animationSpec = tween(if (visible) 200 else 160)
        )
    }
    // v389 — the old AnimatedVisibility painted the shadow on a separate render
    // node from the scale transform, so the shadow's box popped out as a stale
    // rectangle during the scale-out instead of shrinking with the circle. This
    // drives both scale AND shadowElevation from the same progress float inside
    // one graphicsLayer, so the shadow is always the circle's own shadow at
    // every moment of the animation.
    if (progress.value > 0.01f) {
        val p = progress.value
        Surface(
            onClick = onClick,
            enabled = p > 0.9f,
            shape = CircleShape,
            color = accent,
            shadowElevation = (10 * p).dp,
            modifier = modifier
                .size(56.dp)
                .graphicsLayer {
                    val s = 0.82f + 0.18f * p
                    scaleX = s
                    scaleY = s
                    alpha = p
                }
        ) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                CurioIcon(
                    CurioIcons.Add,
                    "Write something",
                    tint = personalOnAccent(),
                    size = 26.dp
                )
            }
        }
    }
}

/**
 * What the "+" opens: the four things a member can start writing here — a
 * journal page for a day, a book they are reading with somewhere to put the
 * chapters, a note about one topic, and a to-do list — plus v449's dictionary
 * door, which is not writing but is one of the things a member opens this sheet
 * to do. Each door opens the page's OWN screen (see [personalRouteFor]); none of
 * them is a journal day wearing route params.
 *
 * v474 — the doors are a 2-column GRID of tiles ([CreateEntryTile]), with the
 * dictionary taking the sheet's whole width beneath them, and no tile carries a
 * second line of explanation (see the note inside for why a lazy grid must never
 * be used here).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEntrySheet(
    onDismiss: () -> Unit,
    onJournal: () -> Unit,
    onBook: () -> Unit,
    onTopicNote: () -> Unit,
    onTodoList: () -> Unit,
    /** v449 — the dictionary's own page (see `ReaderDictionaryPage`). */
    onDictionary: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // v474 — read ONCE for the whole sheet (it was called again inside every
    // door's arguments, on every recomposition of the sheet).
    val accent = personalAccent()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Start writing",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            // ── v474 — THE DOORS ARE A GRID ────────────────────────────────
            //
            // The member: *"we can make the buttom sheet option of books etc like
            // in a grid maybe"*. Five full-width rows made the sheet something to
            // READ before it was something to choose, and every row carried a
            // second line explaining a title that needed no explaining ("A
            // book" / "Pick a book, review it chapter by chapter"). Two columns
            // of tiles put the whole sheet in one glance; the labels are
            // unchanged, and the one line of guidance the sheet still needs is
            // its own title.
            //
            // ⚠️ PLAIN `Row`s WITH `weight(1f)`, NEVER A `LazyVerticalGrid`: a
            // bottom sheet measures its content against an INFINITE height, and a
            // lazy grid needs a bounded one — it would either crash or force a
            // fixed height onto the sheet. Five fixed doors do not need laziness.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreateEntryTile(
                    glyph = CurioIcons.Note,
                    title = "A journal page",
                    accent = accent,
                    onClick = onJournal,
                    modifier = Modifier.weight(1f)
                )
                CreateEntryTile(
                    glyph = CurioIcons.MenuBook,
                    title = "A book",
                    accent = accent,
                    onClick = onBook,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreateEntryTile(
                    glyph = CurioIcons.TravelExplore,
                    title = "A note on a topic",
                    accent = accent,
                    onClick = onTopicNote,
                    modifier = Modifier.weight(1f)
                )
                // v389 — the door wears the CHECKBOX the to-do page itself draws
                // (the same mark the journal's to-do tool wears), instead of the
                // `task_alt` icon, which reads as "task added", not "a list".
                CreateEntryTile(
                    glyph = null,
                    title = "A to-do list",
                    accent = accent,
                    onClick = onTodoList,
                    modifier = Modifier.weight(1f),
                    drawn = { TodoGlyph(active = false, iconSize = 20.dp) }
                )
            }
            // ── v449 — AND THE DICTIONARY, WHICH IS NOT WRITING ────────
            //
            // The member asked for this page to be reachable from Home's "+" as well
            // as the reader's ⋯ menu, and it belongs here even though nothing is
            // written on it: the sheet is the member's own "what shall I do now" door,
            // and looking a word up is one of the things they do.
            //
            // v474 — and it takes the sheet's WHOLE WIDTH rather than a cell, which
            // says the same thing the comment above does: this one is not a writing
            // page, and a wide last tile is also what keeps an odd fifth door from
            // leaving a hole in a two-column grid.
            CreateEntryTile(
                glyph = CurioIcons.Search,
                title = "The dictionary",
                accent = accent,
                onClick = onDictionary,
                wide = true
            )
        }
    }
}

/**
 * ONE DOOR IN THE "Start writing" GRID (v474).
 *
 * The glyph on its accent plate and the label — a tile says two things, which is
 * all a choice in a sheet needs to say. [wide] is the horizontal form: the
 * dictionary is not a writing page, so it takes the sheet's whole width instead
 * of a cell (and that is also what keeps the odd fifth door from leaving a hole
 * in the grid).
 *
 * The tile is built to sit in a `Row` with `weight(1f)`; it never sets its own
 * width, so both shapes fill whatever the caller gives them.
 */
@Composable
private fun CreateEntryTile(
    glyph: String?,
    title: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** A drawn door mark, for the doors whose icon the bundled font subset
     *  cannot say — the to-do list wears the page's OWN checklist box. */
    drawn: (@Composable () -> Unit)? = null,
    wide: Boolean = false
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        if (wide) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                CreateEntryTileGlyph(glyph, accent, drawn)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                CurioIcon(CurioIcons.ChevronRight, null, tint = ink.copy(alpha = 0.4f), size = 18.dp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                CreateEntryTileGlyph(glyph, accent, drawn)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ink,
                    // Two lines, so "A note on a topic" wraps rather than
                    // ellipsising into "A note on a…" in a half-width cell.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** The 42dp accent plate a door's mark sits on — shared by both tile shapes. */
@Composable
private fun CreateEntryTileGlyph(
    glyph: String?,
    accent: Color,
    drawn: (@Composable () -> Unit)?
) {
    // The glyph tone is NOT the raw accent: in light mode the accent is too
    // pale to read on its own wash, so the icon takes the deeper hero ink.
    val glyphTint = personalIconTint(accent)
    Surface(
        shape = RoundedCornerShape(14.dp),
        // v412 — opaque: the accent is mixed into the card fill instead
        // of tinting it translucently.
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.24f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            if (drawn != null) {
                CompositionLocalProvider(LocalContentColor provides glyphTint) { drawn() }
            } else {
                CurioIcon(glyph.orEmpty(), null, tint = glyphTint, size = 20.dp)
            }
        }
    }
}

/**
 * v457 — WHAT THE WRITING PAGES PAINT ON THEIR FIRST FRAME.
 *
 * The member: *"many ui elements have loading unloading behaviors
 * unnecessarily"* — and, asked which, *"the books and journals in home
 * screen"*. The rows read the personal store through `produceState(initialValue
 * = emptyList())`, so every return to Home composed an EMPTY row and filled it a
 * frame or two later when Room's flow delivered: the chips (and the journals
 * list, and the shelf) visibly unloaded and loaded again, on every visit, for
 * data that had not changed.
 *
 * A process-scoped snapshot is the initial value now — the same shape the topic
 * browser already uses for its rows (`BrowseRowsCache`) — so the last known
 * journals and books are on screen in the first frame and the flow only ever
 * CORRECTS them. It is one snapshot for all three surfaces because they read the
 * same two flows (Home's two rows, the journals list, the shelf).
 *
 * `@Volatile` because the flows write on their own thread and the next
 * composition reads on the main thread, with no lock and no hand-off between
 * them.
 */
internal object PersonalShelfSnapshot {
    @Volatile var journals: List<PersonalNoteEntity> = emptyList()
    @Volatile var books: List<PersonalBookEntity> = emptyList()
}

/**
 * The Home row: what the member is writing, as chips — the newest journals under
 * the Pages door, the books under My shelf — a fixed-height row that never
 * reflows as the library grows.
 *
 * v413 — NO `onWrite` PARAMETER ANY MORE. It existed for the empty state's one
 * chip ("No pages yet / Start your first one", which opened the writing sheet),
 * and that chip is a row of days now, each opening the journal on its own date.
 * The writing sheet keeps its real door: Home's floating "+"
 * (`PersonalCreateLauncher` → `writeSheetOpen`), which is where a member actually
 * starts a page. The parameter was deleted with its only caller rather than left
 * behind as a dead one.
 */
@Composable
fun PersonalChipsRow(
    navController: NavController,
    /**
     * v389d — THE PAGE'S OWN BACKDROP, for the doors' plate.
     *
     * The two doors ride on an opaque fill so the chips can slide under them —
     * and that fill was the theme's plain `background`, which on Home is NOT
     * what the page is painted with (Home wears a lane wash, or its own rose
     * tint). The doors therefore sat on a pale plate of their own, a visible
     * seam in every theme that is not plain white (user report: "in home screen
     * the stikky pages and your my shelf. they have a white background which
     * creates weird theme issues with background"). The caller hands over what
     * it actually painted with.
     */
    backdrop: Color = MaterialTheme.colorScheme.background,
    modifier: Modifier = Modifier
) {
    // v457 — SEEDED FROM THE LAST LOOK (see [PersonalShelfSnapshot]): the row is
    // already full when Home comes back, and an empty snapshot (a genuinely empty
    // library) still reads as empty on the very first frame.
    val journals by produceState(initialValue = PersonalShelfSnapshot.journals) {
        runCatching {
            PersonalRepositoryHolder.repo.observeJournals().collect {
                PersonalShelfSnapshot.journals = it
                value = it
            }
        }
    }
    val books by produceState(initialValue = PersonalShelfSnapshot.books) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBooks().collect {
                PersonalShelfSnapshot.books = it
                value = it
            }
        }
    }
    val ink = MaterialTheme.colorScheme.onBackground

    // ── v413 — WHAT A DOOR OFFERS WHEN ITS ROW IS EMPTY ─────────────────
    //
    // The shelf's three suggestions come from Curio's OWN book catalog (offline,
    // curated, and every one of them carries a real chapter list and page
    // count) and are read only while the shelf is actually empty — a member with
    // books never pays for the parse. One flag guards the write: a second tap
    // while the first is still landing would shelve the same book twice, and the
    // empty state lasts exactly as long as that write.
    val scope = rememberCoroutineScope()
    var shelving by remember { mutableStateOf(false) }
    // v461 — the pin (see the shelf row below): read once here, because the badge
    // and the sort must never disagree about which book is pinned.
    val pinnedBookId = AppPreferences.pinnedBookIdState
    val shelfBooks = remember(books, pinnedBookId) {
        if (pinnedBookId.isBlank()) {
            books
        } else {
            books.sortedByDescending { it.id == pinnedBookId }
        }
    }
    val suggestedBooks by produceState(
        initialValue = emptyList<BookCatalog.Hit>(),
        books.isEmpty()
    ) {
        if (books.isEmpty()) {
            value = withContext(Dispatchers.IO) {
                runCatching { BookCatalog.suggestions(3) }.getOrDefault(emptyList())
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // v461 — EACH ROW WEARS ITS OWN SWITCH (Settings → Home). The condition
        // is the whole call, so a hidden row costs one boolean and leaves nothing
        // behind: no plate, no gap, no empty state — the row below simply moves up
        // (the Column's `spacedBy(10.dp)` is the only spacing between them).
        if (AppPreferences.homePagesRowState) PinnedDoorRow(
            backdrop = backdrop,
            door = {
                DoorChip(
                    glyph = CurioIcons.Note,
                    label = "Pages",
                    caption = "All journals",
                    onClick = { navController.navigate(CurioRoutes.JOURNALS) { launchSingleTop = true } }
                )
            }
        ) {
            // ── v406 — A DOOR'S ROW WITH NOTHING IN IT SAYS SO ──
            //
            // A first-time member's Home had two doors and two rows of nothing:
            // a LazyRow draws no chip until a journal or a book exists, so the
            // strip collapsed to the door alone and read as part of the page
            // that had failed to load (member's report: "in home screen the
            // doors and its list so when user opens the page for the first time
            // those place feels empty"). The door now offers the first step
            // itself, at a chip's own size, so the row keeps its shape.
            //
            // ── v413 — AND THE FIRST STEP IS A DAY, NOT A BUTTON ──
            //
            // It used to be one chip that read "No pages yet / Start your
            // first one" and opened the writing sheet — a button in a row of
            // chips (member: "for the empty states of the my shelf door and
            // pages door instead of new button show like today, yesterday or
            // day before date and say write down something about them"). The
            // row now offers the LAST THREE DAYS as chips — Today, Yesterday
            // and the day before — each opening the journal ON ITS OWN DAY (see
            // `PendingJournalDay`), with the line that asks for the writing in
            // front of them. A day is a subject; "no pages yet" is only an
            // absence.
            if (journals.isEmpty()) {
                item("empty-journals-lead") {
                    EmptyDoorLead(
                        title = "Nothing here yet",
                        caption = "Write down something about one of these days."
                    )
                }
                items(items = emptyDayChips(), key = { it.millis }) { day ->
                    EmptyDoorChip(
                        glyph = CurioIcons.CalendarToday,
                        label = day.label,
                        caption = day.caption,
                        onClick = {
                            // The day rides out of band (a journal route carries
                            // an entry id and nothing else), and the page
                            // consumes it as it seeds its own date.
                            PendingJournalDay.request(day.millis)
                            navController.navigate(
                                CurioRoutes.journalEditor(CurioRoutes.PERSONAL_NEW)
                            ) { launchSingleTop = true }
                        }
                    )
                }
            }
            // v389d — MORE THAN THREE (user question: "why only 3 books and 3
            // journal shows. add more keeping scroll too"). The row has always
            // been a LazyRow — it scrolls — so the cap was the only reason the
            // rest of the library could not be reached from here.
            items(items = journals.take(CHIP_ROW_LIMIT), key = { it.id }) { journal ->
                JournalChip(journal = journal, onClick = {
                    // v389 — the chip opens the page's OWN screen (a to-do list is
                    // not a journal day; see personalRouteFor).
                    navController.navigate(personalRouteFor(journal)) { launchSingleTop = true }
                })
            }
        }
        if (AppPreferences.homeShelfRowState) PinnedDoorRow(
            backdrop = backdrop,
            door = {
                DoorChip(
                    glyph = CurioIcons.MenuBook,
                    label = "My shelf",
                    caption = "Books",
                    onClick = { navController.navigate(CurioRoutes.BOOKS) { launchSingleTop = true } }
                )
            }
        ) {
            // The shelf's own first step, for the same reason as the row above.
            //
            // ── v413 — AND IT IS A BOOK, NOT A BUTTON ──
            //
            // "No books yet / Open the shelf" was a door inside a door — it led
            // to the same shelf this row already is (member: "for books show 3
            // book suggestions"). The row now offers three real books out of
            // Curio's own catalog, drawn with the same cover chip a shelved book
            // wears, and one tap shelves it and opens it. The catalog is why
            // this is possible offline: every one of its ~800 books arrives
            // with its real chapter list, its page count and its own cover.
            if (books.isEmpty()) {
                item("empty-books-lead") {
                    EmptyDoorLead(
                        title = "Nothing here yet",
                        caption = "Pick one to start with."
                    )
                }
                items(items = suggestedBooks, key = { it.topicId }) { hit ->
                    BookChip(
                        book = PersonalBookEntity(
                            id = "",
                            title = hit.title,
                            author = hit.author,
                            coverUrl = hit.coverUrl
                        ),
                        onClick = pick@{
                            // One tap, one book: the shelf's own row (and this
                            // row's empty state) disappears the moment the write
                            // lands, because both read the same flow. The label
                            // is explicit (and named differently from the flag it
                            // guards) so the early return can never be read as
                            // returning from something else.
                            if (shelving) return@pick
                            shelving = true
                            scope.launch {
                                val id = runCatching { shelveSuggestion(hit) }.getOrNull()
                                shelving = false
                                if (id != null) {
                                    navController.navigate(CurioRoutes.bookDetail(id)) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
            // ── v461 — THE PINNED BOOK LEADS THE ROW ─────────────────────────────
            //
            // The member: *"how about when added a pdf in book, user can pin it in
            // home screen shelf door and it shows with the small in icon"*. A pin
            // that only ADDED a badge would still leave the book behind whatever
            // six books came first, which is the one thing a pin is for — so the
            // pinned id is sorted to the front here (on the whole list, before
            // `take`), and the chip that wears the pin is the chip that is first.
            //
            // `remember`ed on the list AND the id, so a pin change re-sorts
            // without rebuilding the chips: `sortedByDescending` on `false < true`
            // is the entire sort.
            items(items = shelfBooks.take(CHIP_ROW_LIMIT), key = { it.id }) { book ->
                BookChip(
                    book = book,
                    pinned = book.id == pinnedBookId,
                    // ── v462 — A CHIP OPENS THE SAME THING ITS CARD DOES ──────
                    //
                    // The shelf's card now opens straight to the reader ONLY for the
                    // pinned book (see `BookShelfScreen`), and this row is a shortcut
                    // INTO that shelf — so it follows the same rule: the pinned book
                    // (which already leads this row) goes to the reader, every other
                    // chip goes to the book's page. Same test, same destinations.
                    onClick = {
                        val opensToReader = book.id == pinnedBookId &&
                            BookFiles.documentOf(book.documentPath, book.coverUrl).isNotBlank()
                        val target = if (opensToReader) {
                            CurioRoutes.reader(book.id)
                        } else {
                            CurioRoutes.bookDetail(book.id)
                        }
                        navController.navigate(target) { launchSingleTop = true }
                    }
                )
            }
        }
    }
}

/**
 * v389 — A ROW WHOSE DOOR CANNOT SCROLL AWAY.
 *
 * The door ("Pages" / "My shelf") holds the row's left edge and the content
 * scrolls BESIDE it, sliding UNDER it — so the way into the full list is one tap
 * away however far the row has been dragged (user request: "make the pages and
 * my shelf sticky … the content of them stays scrollable and it goes under
 * that").
 *
 * The door rides in an opaquely filled overlay because what passes beneath it
 * has to be HIDDEN: the fill is the page's own background, not a translucent
 * scrim, which would ghost the sliding chips through the door and its shadow.
 */
@Composable
private fun PinnedDoorRow(
    backdrop: Color,
    door: @Composable () -> Unit,
    content: LazyListScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            // The strip the door occupies is RESERVED, so the first chip starts
            // clear of it and only slides under once the row is dragged.
            contentPadding = PaddingValues(start = DOOR_GUTTER, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                // The plate: the page's own backdrop, solid under the door and
                // FADED at its trailing edge — an opaque rectangle laid over a
                // tinted page reads as a seam, and a bare fade would ghost the
                // chips through the door's own text.
                .drawBehind {
                    val tail = 18.dp.toPx().coerceAtMost(size.width)
                    val solid = (size.width - tail).coerceAtLeast(0.001f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to backdrop,
                                (solid / size.width).coerceIn(0f, 1f) to backdrop,
                                1f to backdrop.copy(alpha = 0f)
                            )
                        )
                    )
                }
                .padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            door()
        }
    }
}

/**
 * v389d — HOW MANY CHIPS A HOME ROW OFFERS. The row scrolls, so this is only
 * about not building a chip for a library of hundreds on first frame.
 */
private const val CHIP_ROW_LIMIT = 12

/**
 * v406 — THE FIRST STEP, WHERE A DOOR'S ROW HAS NOTHING YET.
 *
 * A chip's own width and height, so an empty row is a row and not a strip that
 * collapsed to its door. It reads as an invitation rather than as an error: the
 * glyph in the accent's own wash, the label a plain fact, the caption the thing
 * to do about it (see [PersonalChipsRow]).
 */
@Composable
private fun EmptyDoorChip(
    glyph: String,
    label: String,
    caption: String,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                // v412 — opaque (see the disc above).
                color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    CurioIcon(glyph, null, tint = accent, size = 18.dp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.55f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * v413 — THE LINE THAT ASKS FOR THE WRITING.
 *
 * An empty door's row used to open with a chip that said what was MISSING
 * ("No pages yet / Start your first one"); it now opens with the member's own
 * last three days as chips (see [emptyDayChips]) and this line in front of them,
 * because a row of three dates with nothing said about them reads as a list of
 * dates rather than as an invitation (member: "show like today, yesterday or day
 * before date and say write down something about them").
 *
 * It is sized and centred like the chips beside it — a lead-in, not a heading —
 * and it carries no button of its own: the chips under it are the actions.
 */
@Composable
private fun EmptyDoorLead(title: String, caption: String) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .width(LEAD_WIDTH)
            .height(CHIP_HEIGHT),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = ink
        )
        Spacer(Modifier.height(3.dp))
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.55f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The lead-in's measure — wider than a chip, so its sentence sets in 2–3 short
 *  lines beside the dates rather than in a column of single words. */
private val LEAD_WIDTH = 158.dp

/** One day an empty Pages row offers: the moment it stands for, its name and
 *  the date itself. */
private data class EmptyDoorDay(
    val millis: Long,
    val label: String,
    val caption: String
)

/**
 * v413 — THE LAST THREE DAYS, as the first thing an empty Pages row offers.
 *
 * Today, yesterday and the day before — the three days anyone can still say
 * something about, and the three a diary is actually written backwards from.
 * The third is named by its WEEKDAY rather than as "day before yesterday": at a
 * chip's width the two words that say when it was are the weekday and the date
 * under it, and "Day before yesterday" would either wrap or shrink to fit.
 *
 * Each chip carries its own local midnight, which is what the journal page
 * stores for a day (`startOfToday`/`shiftDay` — a journal day is a calendar day).
 */
private fun emptyDayChips(now: Long = startOfToday()): List<EmptyDoorDay> {
    val locale = java.util.Locale.getDefault()
    val date = java.text.SimpleDateFormat("d MMMM", locale)
    val weekday = java.text.SimpleDateFormat("EEEE", locale)
    return listOf(0L, 1L, 2L).map { daysAgo ->
        val millis = shiftDay(now, -daysAgo)
        EmptyDoorDay(
            millis = millis,
            label = when (daysAgo) {
                0L -> "Today"
                1L -> "Yesterday"
                else -> weekday.format(java.util.Date(millis))
            },
            caption = date.format(java.util.Date(millis))
        )
    }
}

/**
 * v413 — SHELVE A SUGGESTED BOOK, AND OPEN IT.
 *
 * The write is the same row the shelf's own add flow makes for a catalog book —
 * title, author, cover, the catalog's page count and its REAL chapter list, and
 * the `catalogId` that lets the book's page read those chapters, its synopsis and
 * its ranges back out of Curio's own JSON offline. Nothing here is a guess:
 * every field comes from the catalog entry the member just tapped.
 *
 * `createdAtMillis`/`updatedAtMillis` are stamped here so the new book sorts
 * into the shelf as the newest thing in it (the store keeps a zero it is given,
 * but a book added now belongs at the head of a "what you are reading" row).
 */
private suspend fun shelveSuggestion(hit: BookCatalog.Hit): String {
    val id = newPersonalBookId()
    val now = System.currentTimeMillis()
    PersonalRepositoryHolder.repo.saveBook(
        PersonalBookEntity(
            id = id,
            title = hit.title,
            author = hit.author,
            coverUrl = hit.coverUrl,
            totalChapters = hit.chapterCount,
            pageCount = hit.pageCount,
            catalogId = hit.topicId,
            createdAtMillis = now,
            updatedAtMillis = now
        )
    )
    return id
}

@Composable
private fun NewChip(onClick: () -> Unit) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        // v412 — opaque: the chip is the accent mixed into the card fill, never
        // a translucent wash over the page.
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.12f),
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = accent, modifier = Modifier.size(34.dp)) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.Add,
                        null,
                        tint = personalOnAccent(),
                        size = 19.dp
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "New",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = accent
            )
        }
    }
}

/** A journal as a chip: the day, how it felt, and what it is called. */
@Composable
private fun JournalChip(
    journal: PersonalNoteEntity,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val mood = journal.moodEnum
    // v428 — A CHIP IS A PAGE'S DOOR, so a page the member gave a colour to
    // wears it here: a wash of it under the chip and the spine down its left
    // edge, so a coloured day is findable from Home without opening anything
    // (see [journalDoorAccent]). An uncoloured page is the chip it always was.
    val own = journalDoorAccent(journal.accentArgb)
    val worn = if (journal.hasOwnAccent) own else null
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = worn?.let { lerp(MaterialTheme.colorScheme.surfaceContainerLow, it, 0.14f) }
            ?: MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (worn != null) {
                        val barWidth = 3.dp.toPx()
                        drawRoundRect(
                            color = worn,
                            size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                        )
                    }
                }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // v389e — THE DAY IS INK, NOT ACCENT.
                //
                // The numeral wore the accent, which on a pale card is the
                // lightest thing on it — the day, the one figure a journal chip
                // exists to say, was the hardest word on the tile to read (user
                // report: "for journal number date its too accent color and very
                // light colored so fix it by making it dark"). It is the page's
                // own ink now, with the month beside it held back — so the day
                // leads and the month follows.
                Text(
                    journal.dateMillis.toLocalDate().dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    journal.dateMillis.toLocalDate()
                        .month.name.lowercase().take(3)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f)
                )
                Spacer(Modifier.weight(1f))
                if (mood != null) {
                    CurioIcon(
                        personalMoodGlyph(mood),
                        mood.label,
                        tint = ink.copy(alpha = 0.5f),
                        size = 14.dp
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                journal.title.ifBlank { journal.preview.ifBlank { "Untitled day" } },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * v389 — A BOOK AS A CHIP: THE WHOLE COVER, WITH ITS NAME ON A FOOTER.
 *
 * The cover used to be a 58dp band with the title printed UNDER it, which read
 * as two unrelated things (a sliver of artwork and a line of text) — and the
 * band cropped most covers to a strip. The chip is now the COVER, full height,
 * with a small footer bar laid across its bottom edge holding the name in ONE
 * line: the strip is a fixed height, so a long title is ellipsised instead of
 * pushing the cover around (user request).
 */
@Composable
private fun BookChip(
    book: PersonalBookEntity,
    /** v461 — whether this book is the one pinned to Home's shelf door. */
    pinned: Boolean = false,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val footerInk = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Box(Modifier.fillMaxSize()) {
            BookCover(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                corner = 0.dp,
                modifier = Modifier.fillMaxSize()
            )
            // ── v461 — THE PIN'S OWN MARK ────────────────────────────────────
            //
            // A small disc in the cover's own corner: the app's accent under the
            // theme's on-accent ink, on a `push_pin` glyph. It is deliberately
            // SMALL and in the corner a cover's own art never needs — the point of
            // a pin mark is that the shelf door says "this one is the one you are
            // reading" at a glance, not that it decorates the chip.
            if (pinned) {
                Surface(
                    shape = CircleShape,
                    // The personal accent, not the theme's `primary`: the shelf
                    // card wears this exact disc (see `BookShelfScreen`), and the
                    // two marks have to be the same mark — a member who pins from
                    // the shelf should recognise the chip at a glance.
                    color = personalAccent(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.PushPin,
                            "Pinned to Home",
                            tint = personalOnAccent(),
                            size = 13.dp
                        )
                    }
                }
            }
            // The footer: one line, its own height, OPAQUE so the cover cannot
            // bleed through the title it is naming.
            Surface(
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                color = footerInk,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // v389 — the footer's type is SMALL (user request: "make
                    // the title font more smaller"): it sits on a 24dp strip
                    // under the artwork, so it names the book rather than
                    // competing with its cover.
                    Text(
                        book.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            letterSpacing = 0.1.sp
                        ),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * A door chip: the row's quiet "there is more" (and where an empty library
 * still finds its list).
 *
 * v389 — it has DEPTH now: a soft shadow plus a hairline edge, the way the
 * Cabinet's shelves read, so the two doors (Pages / My shelf) sit ABOVE the
 * chips around them instead of flat beside them (user request).
 *
 * AND IT WEARS THE ACCENT (user request: "give them the theme color accent to
 * the card of it"): an opaque wash of the member's own accent over the surface,
 * with an accent hairline and the accent's deep shade as the ink, so the door is
 * unmistakably the way in rather than a third chip. The wash is OPAQUE on
 * purpose — the row slides under this card, so a translucent fill would show the
 * chips passing beneath it and let the shadow bleed through.
 */
@Composable
private fun DoorChip(
    glyph: String,
    label: String,
    caption: String,
    onClick: () -> Unit
) {
    val accent = personalAccent()
    // ── THE DOOR IS THE ACCENT (v389e) ──────────────────────────────────
    //
    // It used to be a mostly-surface card with an accent hairline around it and
    // the words in the accent's darker ink — a tinted outline on a pale fill
    // (user request: "for the door in home screen pages and my shelf dont give it
    // accent border but make the whole card accent color and proper text color as
    // well so its readable too"). So the whole face is the accent, with no
    // border, and the words wear the app's own ink for text ON an accent fill —
    // which is what makes a label read on a filled card instead of glowing on
    // it. The fill is opaque on purpose: a translucent one lets the shadow bleed
    // through (see the shadow rule).
    val onAccent = personalOnAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = accent,
        shadowElevation = 6.dp,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurioIcon(glyph, null, tint = onAccent, size = 22.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = onAccent
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = onAccent.copy(alpha = 0.78f)
            )
        }
    }
}

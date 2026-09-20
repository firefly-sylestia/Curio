package com.curio.app.features.personal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.ReaderMarkEntity
import com.curio.app.data.openSearchUrl
import com.curio.app.features.cabinet.CabinetCoverCache
import com.curio.app.features.community.SocialPullQuote
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioCardShadow
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v387 — ONE BOOK ON THE PERSONAL SHELF.
 *
 * The page does four things, all in place: shows the cover and where the
 * member is in it, lets them move that progress, lists the chapters, and
 * opens a chapter's writing IN THE PAGE (never a separate editor screen) —
 * "opening them shows the book chapter review they wrote or let them write if
 * there's nothing, a new creating and saved and view flow". Everything
 * auto-saves: the review, the blurb and the progress.
 */
@Composable
fun BookDetailScreen(navController: NavController, bookId: String) {
    // v389c — THE FIRST FRAME IS THE LAST VISIT'S ANSWER.
    //
    // Both of these are Room flows, and a flow's first value lands a frame or
    // two after the page composes — so reopening a book began from nothing: the
    // head without its title, then the title; the chapter rows as numbered
    // placeholders, then their real names and page ranges. BookPageMemory keeps
    // what the last visit ended on and hands it over as the initial value only;
    // the flows below still decide, and they win on their first emission (user
    // report: "the chapter view well sometimes it reload like it fetches the
    // chapter notes etc then when i close and open again for a berif moment i
    // see pages").
    val book by produceState<com.curio.app.data.PersonalBookEntity?>(
        initialValue = BookPageMemory.row(bookId)
    ) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBook(bookId).collect {
                BookPageMemory.rememberRow(it)
                value = it
            }
        }
    }
    val notes by produceState(initialValue = BookPageMemory.notes(bookId)) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBookNotes(bookId).collect {
                BookPageMemory.rememberNotes(bookId, it)
                value = it
            }
        }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // THE MARGINS: what the member marked while READING this book in Curio's
    // reader. A highlight carries the passage's own WORDS, so the page can hold
    // it as a quote without opening the file again.
    val margins by produceState(initialValue = emptyList<ReaderMarkEntity>(), bookId) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBookMarks(bookId).collect { value = it }
        }
    }
    // ── v408 — THE READER'S OWN ANSWER TO "WHERE WAS I" ────────────────
    // The reader keeps ONE position row per book + file (rewritten on every
    // read, never appended), keyed by the FILE PATH as its sourceKey — the
    // same resolution the Read pill makes below. The progress card observes
    // that row so the card shows the page the file itself says the member is
    // on — not just the chapter number typed by hand.
    val attachedDocument = book?.let { BookFiles.documentOf(it.documentPath, it.coverUrl) }.orEmpty()
    val isPdfDocument = remember(attachedDocument) {
        attachedDocument.lowercase().endsWith(".pdf")
    }
    val lastPosition by produceState(
        initialValue = ReaderMarkEntity(id = "", bookId = bookId, sourceKey = attachedDocument),
        bookId, attachedDocument
    ) {
        if (attachedDocument.isBlank()) return@produceState
        runCatching {
            // The reactive position row (v408): it excludes itself from every
            // mark query, so the card has its own flow — see the DAO.
            PersonalRepositoryHolder.repo.observeReaderPosition(bookId, attachedDocument)
                .collect { value = it ?: ReaderMarkEntity(id = "", bookId = bookId, sourceKey = attachedDocument) }
        }
    }
    // The file's own length, read off the document ONCE per book (a PDF's page
    // count is a fact of the file, not of the session).
    val filePageCount by produceState(initialValue = 0, attachedDocument) {
        if (attachedDocument.isBlank() || !isPdfDocument) return@produceState
        withContext(Dispatchers.IO) {
            value = runCatching { pdfPageCount(context, attachedDocument) }.getOrDefault(0)
        }
    }
    // The page the member last read, in the file's own terms (0 when the book
    // is not a PDF — no pretending a reflowable text has pages).
    val lastReadPage = lastPageOf(lastPosition, isPdfDocument)
    // v409 — A HAND-SET PAGE IS THE BOOK ROW'S OWN MARK (see
    // [PersonalBookEntity.currentPage]), and THE MOST RECENT ANSWER WINS: the
    // reader stamps its position row on every turn, this page stamps the book
    // row on a hand move, and the card shows whichever of the two was written
    // last. That is what lets a hand move stick in EITHER direction — a plain
    // "furthest wins" rule would snap a move backwards straight back to the
    // reader's page and the stepper would keep looking broken — while reading
    // on simply takes the card back over.
    val handPage = book?.currentPage ?: 0
    val progressPage = when {
        handPage <= 0 -> lastReadPage
        lastReadPage <= 0 -> handPage
        (book?.updatedAtMillis ?: 0L) >= lastPosition.updatedAtMillis -> handPage
        else -> lastReadPage
    }
    // THE APP'S OWN CATALOG. A book added from Curio's own lane carries its
    // topic id, so its chapter rows can wear the book's REAL chapter names, page
    // ranges and summaries instead of "Chapter 7" — the catalog is the reason
    // the shelf can be more than a list of titles.
    val catalogId by produceState(initialValue = "", book) { value = book?.catalogId.orEmpty() }
    // THE CHAPTERS, from whichever door the book came in by: Curio's own lane
    // when it has the book, else the table of contents read from Open Library
    // (see BookEnrichment) — so a hand-added book has real chapter names too.
    val chapters = rememberBookChapters(book)
    // The catalog's own blurb for this book — shown when it has one.
    val catalogSynopsis by produceState(initialValue = "", catalogId) {
        value = BookCatalog.synopsis(catalogId)
    }

    // ── Auto-fetch ─────────────────────────────────────────────────────
    // What Curio knows about this book is filled in FOR the member rather
    // than asked of them: the app's own catalog first (real chapters, pages
    // and synopsis), then Open Library for a page count the catalog does not
    // have. One pass per visit, plus one more when they tap "Look it up".
    var lookupTick by remember(bookId) { mutableIntStateOf(0) }
    var enrichedTick by remember(bookId) { mutableIntStateOf(-1) }
    var lookingUp by remember(bookId) { mutableStateOf(false) }
    // What the last TAPPED pass found, said plainly under the pills. The pill
    // used to be silent: the pass ran, learned nothing, and the button looked
    // broken. Now it reports — found what, or why it found nothing.
    var lookupNote by remember(bookId) { mutableStateOf<String?>(null) }
    // Auto-fetch: skip if the book already has chapters, pages, AND synopsis
    // (everything is filled in — nothing to learn).
    val alreadyComplete = book?.let {
        it.totalChapters > 0 && it.pageCount > 0 &&
        (it.synopsis.isNotBlank() || catalogSynopsis.isNotBlank())
    } ?: false
    LaunchedEffect(book, lookupTick) {
        val current = book ?: return@LaunchedEffect
        if (enrichedTick == lookupTick) return@LaunchedEffect
        // v389 — an already-complete book skips the initial pass, and so does a
        // book whose lookup already COMPLETED (the marker is durable, so a
        // restart or a re-open no longer repeats the same network request).
        // Only the INITIAL pass consults it: a manual tap is an explicit retry.
        // The marker set is disk-backed, so it is read off the main thread.
        val skipInitial = lookupTick == 0 && (
            alreadyComplete ||
                withContext(Dispatchers.IO) {
                    AppPreferences.getBookLookupDone(context).contains(bookId)
                }
            )
        if (skipInitial) return@LaunchedEffect
        val manual = lookupTick > 0
        enrichedTick = lookupTick
        lookingUp = true
        if (manual) lookupNote = null
        val report = withContext(Dispatchers.IO) {
            runCatching { BookEnrichment.enrich(current) }.getOrNull()
        }
        if (report != null && report.book != current) {
            withContext(Dispatchers.IO) {
                runCatching { PersonalRepositoryHolder.repo.saveBook(report.book) }
            }
        }
        // v389 — persist a pass that COMPLETED, and only that. The marker is
        // durable and the initial pass consults it, so recording a pass that
        // threw (offline, a timeout, a bad response) made ONE bad attempt
        // permanent: that book was never looked up again on any later open.
        // A completed pass that found nothing stays done — asking again would
        // learn nothing — and the Look-it-up pill is always a deliberate retry.
        if (report != null) {
            withContext(Dispatchers.IO) { AppPreferences.markBookLookupDone(context, bookId) }
        }
        if (manual) {
            lookupNote = when {
                report == null -> "Could not reach the catalogue just now."
                report.learned.isNotEmpty() ->
                    "Found " + report.learned.joinToString(", ") + "."
                report.needsConsent ->
                    "Book lookups are off in Settings — turn them on to search Open Library."
                else -> "Nothing more found for this book."
            }
        }
        lookingUp = false
    }

    // ── AND ITS COVER, IF IT CAME IN WITHOUT ONE (v410) ───────────────
    // The page's cover is the book row's own URL, and a book shelved from a
    // topic reveal before v410 (or typed in, or imported from a file) has
    // none — so the page drew a generated plate for a book whose artwork one
    // search returns. It is resolved here through the same verified-bytes
    // machinery the Cabinet and the shelf use (see [BookCoverWarmup]) and
    // written onto the row, so the cover is there on this visit and every one
    // after it. Fetching is gated on the Settings cover consent inside.
    LaunchedEffect(book?.id, book?.coverUrl) {
        val current = book ?: return@LaunchedEffect
        if (current.coverUrl.isNotBlank()) return@LaunchedEffect
        if (BookCoverWarmup.ensureCover(context, current) != null) {
            // The bytes are on disk now, so the plate re-checks the local file
            // rather than downloading the same art a second time through Coil.
            CabinetCoverCache.version.intValue++
        }
    }

    // The download-help sheet (PDF / EPUB).
    var downloadSheet by remember(bookId) { mutableStateOf(false) }

    // The Read pill's own menu — held down, not tapped (see BookReadPill).
    var fileMenu by remember(bookId) { mutableStateOf(false) }

    // ── The book's own FILE (v389) ─────────────────────────────────────
    // A PDF or EPUB the member wires to THIS book. The picked file is COPIED
    // into the app's own storage ([BookFiles]) and opened in Curio's reader,
    // so it works offline and long after the picker's permission would have
    // expired — and attaching one never touches the book's name, blurb,
    // chapters or progress. The pill below is the only door to it.
    var attachFailed by remember(bookId) { mutableStateOf(false) }
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                BookFiles.import(context, bookId, uri, book?.documentPath.orEmpty())
            }
            if (path.isNullOrBlank()) {
                attachFailed = true
            } else {
                withContext(Dispatchers.IO) {
                    runCatching { PersonalRepositoryHolder.repo.setDocument(bookId, path) }
                    // ── v389e — THE FILE TAKES THE BOOK OVER ────────────
                    //
                    // The member's own copy is the book now: its own contents
                    // and its own page count are read straight out of it and
                    // written onto the row, so the chapter rows are the ones the
                    // file prints and the progress stepper counts the pages the
                    // file has — not the ones a lookup guessed for some other
                    // edition (user request: "when i add a books own file it
                    // takes over the fetched file, and takes info from the book
                    // if there is one. also the page number from the file").
                    // A file with no contents of its own answers nothing and
                    // leaves what was fetched where it was.
                    runCatching {
                        val fromFile = documentChapters(context, path)
                        val pages = if (path.lowercase().endsWith(".pdf")) {
                            pdfPageCount(context, path)
                        } else {
                            0
                        }
                        PersonalRepositoryHolder.repo.adoptDocumentFacts(bookId, fromFile, pages)
                    }
                }
                navController.navigate(CurioRoutes.reader(bookId)) { launchSingleTop = true }
            }
        }
    }

    // The book's own note ("why I picked it up") — same auto-save discipline.
    var blurb by remember(bookId) { mutableStateOf("") }
    var blurbSeeded by remember(bookId) { mutableStateOf(false) }
    LaunchedEffect(book) {
        val current = book
        if (current != null && !blurbSeeded) {
            blurb = current.blurb
            blurbSeeded = true
        }
    }

    LaunchedEffect(blurb, bookId) {
        if (!blurbSeeded) return@LaunchedEffect
        delay(700)
        withContext(Dispatchers.IO) {
            runCatching { PersonalRepositoryHolder.repo.setBlurb(bookId, blurb) }
        }
    }

    var pendingReviewDelete by remember { mutableStateOf<PersonalNoteEntity?>(null) }

    // ── READ FIRST (v389) ──────────────────────────────────────────────
    // The page used to open as the book VIEW every time — description, look-up
    // pills, progress steppers — even for a member who only wanted to read back
    // what they had written. Now the head carries the same eye/pen switch the
    // journal pages use: a book with writing opens READING, a book with nothing
    // written opens with the pen down, and the switch is the member's from then
    // on (the seed runs ONCE, when the notes first arrive, so it can never
    // override a toggle they just made).
    var editing by remember(bookId) { mutableStateOf(true) }
    // v389 — the seed keeps FOLLOWING the shelf until the member touches the
    // switch, which is what makes it right on a slow first read: `notes` is a
    // FLOW, so it emits an empty list first and the real rows a moment later,
    // and deciding on that first emission read "nothing written" before
    // anything had been read — a book WITH writing still opened as the book
    // view (user report: "why i picked for book in eye it doesnt show that",
    // and "similar to journal opening in view do the same for saved books
    // too"). It also asks for actual WRITING rather than for a row: a review
    // that was opened and left empty is nothing to read back, and a book with
    // nothing written opens with the pen down exactly like a journal page.
    var modeTouched by remember(bookId) { mutableStateOf(false) }
    LaunchedEffect(notes, book) {
        if (modeTouched) return@LaunchedEffect
        // Wait for the book row itself: "has writing" is a question for the
        // store, and it is only worth asking once the store has answered.
        if (book == null) return@LaunchedEffect
        editing = notes.none { !it.doc.isEmpty }
    }

    // ── The page's own scroll, so its head can ROLL UP ──────────────────
    // The title is on the page twice: big, with the cover, and here in the
    // head. The head's copy is a roll-up — it stays out of the way until the
    // page's own title has gone under it (see [PersonalHeader.titleRevealed]),
    // which is exactly where "which book am I in" stops being answered by the
    // page itself.
    val pageScroll = rememberLazyListState()
    val titleScrollThreshold = with(LocalDensity.current) {
        remember { 26.dp.roundToPx() }
    }
    val titleRolled by remember(pageScroll) {
        derivedStateOf {
            pageScroll.firstVisibleItemIndex > 0 ||
                pageScroll.firstVisibleItemScrollOffset > titleScrollThreshold
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        val current = book
        PersonalHeader(
            title = current?.title.orEmpty(),
            subtitle = current?.author.orEmpty(),
            titleRevealed = titleRolled,
            // The head's idle line names the SHELF the book sits on: while the
            // page is still showing the book's own title and author there is
            // nothing for the head to add (user request: "instead of initial
            // blank say your shelf").
            idleTitle = "Your shelf",
            onBack = { navController.popBackStack() },
            action = {
                PersonalModeSwitch(
                    editing = editing,
                    // The member's own toggle wins from here on — the seed above
                    // never overrides a side they have chosen.
                    onToggleMode = { modeTouched = true; editing = it }
                )
            }
        )

        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Opening the book…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            return@Column
        }

        // The learned list is a length too: a book whose ToC arrived after the
        // member added it should read as complete, not as "set the length".
        val total = if (current.totalChapters > 0) current.totalChapters else chapters.size
        val writtenChapters = notes.mapNotNull { it.chapterIndex }.toSet()
        val highestWritten = writtenChapters.maxOrNull() ?: 0
        val chapterCount = maxOf(total, chapters.size, highestWritten, 1)

        // The list keeps the column's weight (the reader is a separate screen),
        // and the Read pill floats OVER it so it is reachable wherever the        // member has scrolled to.
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = pageScroll,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item("identity") {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BookCover(
                            title = current.title,
                            author = current.author,
                            coverUrl = current.coverUrl,
                            corner = 12.dp,
                            modifier = Modifier
                                .width(104.dp)
                                .height(156.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                current.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (current.author.isNotBlank()) {
                                Text(
                                    current.author,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LoraFontFamily),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            // What the app knows about the book, at a glance.
                            val facts = listOfNotNull(
                                current.pageCount.takeIf { it > 0 }?.let { "$it pages" },
                                current.totalChapters.takeIf { it > 0 }?.let { "$it chapters" },
                                current.catalogId.takeIf { it.isNotBlank() }
                                    ?.let { "From Curio's catalog" }
                            ).joinToString(" \u00b7 ")
                            if (facts.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    facts,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                )
                            }
                            // Two doors, both always open: ask the app's sources
                            // again (the first pass runs by itself), or go looking
                            // for a copy to download.
                            Spacer(Modifier.height(8.dp))
                            // ── BOTH DOORS STAY (v389e) ──────────────────
                            //
                            // These two used to fold away with the pen, on the
                            // reasoning that a reader does not need them. The
                            // member's own answer is the other way: finding the
                            // book and finding a copy of it are things a reader
                            // does, and a door that vanishes when the eye is
                            // taken is a door they have to flip back for (user
                            // request: "keep the look it up and download help
                            // button in eye view too for books"). So they stay
                            // put, in both modes, with no fold to sit through.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LookUpPill(lookingUp = lookingUp) { lookupTick += 1 }
                                DownloadPill(enabled = true) { downloadSheet = true }
                            }
                            if (lookingUp || lookupNote != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    lookupNote ?: "Looking it up\u2026",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                )
                            }
                        AnimatedVisibility(
                            visible = editing,
                            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(120)) + shrinkVertically(tween(170))
                        ) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                BlurbField(
                                    value = blurb,
                                    onValueChange = { blurb = it },
                                    enabled = blurbSeeded
                                )
                            }
                        }
                        // WHY I PICKED IT UP, READ BACK (v389). The note was only
                        // ever drawn while the pen was down, so the one thing a
                        // member writes BEFORE reading was the one thing the eye
                        // could not see (user report: "in book page the why i
                        // picked it up isnt visible in read view so fix it
                        // please"). A member who never wrote it sees nothing —
                        // an empty card would be a box of nothing.
                        AnimatedVisibility(
                            visible = !editing && blurb.isNotBlank(),
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(120))
                        ) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                BlurbReadBack(value = blurb)
                            }
                        }
                    }
                }
            }

            // v389 — THE WHOLE-BOOK REVIEW. One page for the book itself
            // (CurioRoutes.BOOK_REVIEW), with chapter markers the member can
            // drop in as they go — so writing about a book is not a
            // chapter-by-chapter errand any more.
            item("book-review") {
                val whole = notes.firstOrNull { it.chapterIndex == null }
                BookReviewDoor(
                    preview = whole?.preview.orEmpty(),
                    words = whole?.let { it.doc.wordsLabel() }.orEmpty(),
                    hasWriting = whole != null,
                    ink = MaterialTheme.colorScheme.onBackground,
                    onClick = {
                        navController.navigate(CurioRoutes.bookReview(bookId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

                // v389 — FROM THE MARGINS. The passage is quoted from the mark
                // itself (a highlight stores the words it was made on), so the
                // page can show what the member kept without re-reading the
                // file — and tapping one opens the reader AT that mark, which is
                // what makes this a door rather than a museum. Made read-side
                // only: the margins are something to read back, not a pile of
                // cards to scroll past on the way to the writing.
                if (margins.isNotEmpty()) {
                    item("margins") {
                        // Read-side only, and it folds like the writing tools
                        // do: the margins are what a reader comes back for, so
                        // they arrive with the eye rather than blink into place.
                        // Hoisted into `PersonalFloatingLayer` on purpose: a bare
                        // `AnimatedVisibility` inside a `LazyColumn` item resolves
                        // to the ColumnScope overload and is then rejected (see
                        // that function's own note).
                        PersonalFloatingLayer(
                            visible = !editing,
                            enter = fadeIn(tween(200)) + expandVertically(tween(240)),
                            exit = fadeOut(tween(120)) + shrinkVertically(tween(170))
                        ) {
                        MarginsCard(
                            marks = margins,
                            onOpen = { mark ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            // The reader restores where it was
                                            // left, so "go to this mark" is
                                            // written as a position first.
                                            PersonalRepositoryHolder.repo.saveReaderPosition(
                                                bookId = bookId,
                                                sourceKey = mark.sourceKey,
                                                index = mark.positionIndex,
                                                fraction = mark.positionFraction
                                            )
                                        }
                                    }
                                    navController.navigate(CurioRoutes.reader(bookId)) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        }
                    }
                }

                item("progress") {
                    ProgressCard(
                        total = total,
                        current = current.currentChapter,
                        finished = current.isFinished,
                        // v408 — THE FILE'S OWN MEASURES.
                        pageCount = filePageCount,
                        lastPage = progressPage,
                        chapterNames = chapters.map { it.title },
                        chapterPages = chapters.map { it.pageStart },
                        // v409 — A HAND MOVE OF THE PAGE LANDS ON THE BOOK ROW.
                        // It used to be written as a whole-row save that set the
                        // page COUNT and the chapter but never the page itself,
                        // so the number snapped straight back to the reader's
                        // position and the stepper looked broken (member: "i am
                        // not able to change the pages update from there").
                        // Now: the book's own page mark (one column), plus the
                        // chapter the page falls in when the file's outline
                        // knows its ranges — so the two steppers agree.
                        onPage = if (filePageCount > 0) { page ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching {
                                        PersonalRepositoryHolder.repo.setPage(bookId, page)
                                        val movedTo = chapterForPage(page, chapters)
                                        if (movedTo != null && movedTo != book?.currentChapter) {
                                            PersonalRepositoryHolder.repo.setProgress(bookId, movedTo)
                                        }
                                    }
                                }
                            }
                        } else null,
                        // v411 — THE MOVE GOES BOTH WAYS NOW.
                        // A page move already carried the chapter with it (see
                        // the page's own write above), but a CHAPTER move left
                        // the page where it was — so the two steppers disagreed
                        // in one direction (member: "just like how when
                        // changing pages the chapter also updates, do the same
                        // for pages too"). A chapter move now lands the page on
                        // the page that chapter opens at, when the file's own
                        // outline knows it, and leaves the page alone when it
                        // does not (a fabricated page is worse than none).
                        onChapter = { chapter ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching {
                                        PersonalRepositoryHolder.repo.setProgress(bookId, chapter)
                                        chapterStartPage(chapter, chapters)?.let { start ->
                                            PersonalRepositoryHolder.repo.setPage(bookId, start)
                                        }
                                    }
                                }
                            }
                        },
                        // v413 — THE LENGTH GOES THROUGH ITS OWN COLUMN NOW.
                        // It used to be a whole-row `saveBook(current.copy(…))`
                        // built from this composition's snapshot of the book,
                        // which is fine for one deliberate tap and wrong for a
                        // hold: the rail now repeats while it is held, so every
                        // tick rewrote every column from a snapshot that was
                        // already behind — and two ticks landing out of order
                        // could put the old length back.
                        onTotal = { chapters ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching {
                                        PersonalRepositoryHolder.repo
                                            .setTotalChapters(bookId, chapters)
                                    }
                                }
                            }
                        },
                        onFinished = { done ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching { PersonalRepositoryHolder.repo.setFinished(bookId, done) }
                                }
                            }
                        }
                    )
                }

                // ABOUT THIS BOOK. A book from Curio's own lane reads the catalog's
                // synopsis; one the catalog does not have reads the description
                // Open Library keeps for the work (fetched by BookEnrichment, so it
                // is here offline too). The card says which of the two it is.
                val about = catalogSynopsis.ifBlank { current.synopsis }
                if (about.isNotBlank()) {
                item("synopsis") {
                    SynopsisCard(
                        synopsis = about,
                        source = if (catalogSynopsis.isNotBlank()) "Curio catalog" else "Open Library",
                        // v389 — while the member is WRITING, the description is
                        // folded to a line: it is a thing to read, not a thing
                        // to scroll past on the way to their own words. Tap it
                        // and it opens. ("keep the about this book but as
                        // Collapsed when writing")
                        collapsed = editing
                    )
                }
                }

                item("chapters-title") {
                    Text(
                        if (total > 0) "Chapters" else "Where you write",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                items(
                    count = chapterCount,
                    key = { index -> "chapter-${index + 1}" },
                    // v389 — the eye is a READING eye: in read mode a chapter
                    // row carries no delete button, so the page cannot be
                    // changed by accident while it is being read.
                    ) { index ->
                    val chapter = index + 1
                    val review = notes.firstOrNull { it.chapterIndex == chapter }
                    ChapterCard(
                        chapter = chapter,
                        // v409 — where this chapter sits in the reading run, so
                        // the list reads as progress instead of as forty
                        // identical rows (a finished book closes all of them).
                        state = when {
                            current.isFinished || chapter < current.currentChapter ->
                                ChapterReadState.DONE
                            chapter == current.currentChapter -> ChapterReadState.HERE
                            else -> ChapterReadState.AHEAD
                        },
                        // The catalog's own words for this chapter, when the book came
                        // from Curio's lane: a name, and the pages it spans.
                        catalogTitle = chapters.getOrNull(chapter - 1)?.title.orEmpty(),
                        catalogPages = chapters.getOrNull(chapter - 1)
                            ?.takeIf { it.pageStart > 0 && it.pageEnd > 0 }
                            ?.let { "pp. ${it.pageStart}\u2013${it.pageEnd}" }
                            .orEmpty(),
                        review = review,
                        // The chapter's own page (its summary and its review, and
                        // the writing) — never a canvas grown inside this list.
                        onOpen = {
                            navController.navigate(CurioRoutes.chapter(bookId, chapter)) {
                                launchSingleTop = true
                            }
                        },
                        onDelete = { pendingReviewDelete = review }
                    )
                }

                item("shelf-tail") { Spacer(Modifier.height(96.dp)) }
            }

            // HOLD THE READ PILL: the FILE itself.
            //
            // Tapping it reads (or asks for a file the first time); holding it
            // says which DOCUMENT Curio opens and how to swap it — the member
            // who downloaded the wrong edition, or who has the PDF now and the
            // EPUB later, should not have to hunt for that door (user request:
            // "when i tap and hold the read button it should show a drop down to
            // change the pdf the file attach").
            val attachedFile = BookFiles.documentOf(current.documentPath, current.coverUrl)
            if (fileMenu) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 74.dp)
                ) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { fileMenu = false }
                    ) {
                        if (attachedFile.isBlank()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Curio has no file for this book yet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                },
                                enabled = false,
                                onClick = {}
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Choose a PDF", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                fileMenu = false
                                documentPicker.launch(arrayOf("application/pdf"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Choose an EPUB", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                fileMenu = false
                                documentPicker.launch(
                                    arrayOf("application/epub+zip", "text/plain")
                                )
                            }
                        )
                    }
                }
            }

            // THE READ PILL. With a file wired to the book it opens the reader;
            // without one it asks for the file first. It used to be a plain
            // "Read" text button tucked beside "Download help", which only
            // appeared when a `content://` handle happened to be sitting in the
            // cover column.
            BookReadPill(
                hasFile = attachedFile.isNotBlank(),
                onClick = {
                    if (attachedFile.isNotBlank()) {
                        navController.navigate(CurioRoutes.reader(bookId)) { launchSingleTop = true }
                    } else {
                        documentPicker.launch(
                            arrayOf("application/pdf", "application/epub+zip", "text/plain")
                        )
                    }
                },
                onLongPress = { fileMenu = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 20.dp)
            )
        }
    }

    if (attachFailed) {
        AlertDialog(
            onDismissRequest = { attachFailed = false },
            title = { Text("Could not open that file") },
            text = {
                Text("Curio could not copy the file into its own storage. Try picking it again.")
            },
            confirmButton = {
                TextButton(onClick = { attachFailed = false }) { Text("OK") }
            }
        )
    }

    // Download help: PDF or EPUB, chosen here and searched out in the
    // browser. The extension is the app's own — the member picks a format.
    if (downloadSheet) {
        DownloadHelpSheet(
            title = book?.title.orEmpty(),
            author = book?.author.orEmpty(),
            onDismiss = { downloadSheet = false }
        )
    }

    pendingReviewDelete?.let { review ->
        AlertDialog(
            onDismissRequest = { pendingReviewDelete = null },
            title = { Text("Remove chapter ${review.chapterIndex}?") },
            text = { Text("The review you wrote for this chapter leaves your book.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = review.id
                    pendingReviewDelete = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { PersonalRepositoryHolder.repo.deleteNote(id) }
                        }
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingReviewDelete = null }) { Text("Keep") } }
        )
    }
}

/**
 * THE BOOK'S MARGINS — what the member marked while reading it.
 *
 * A quote is the family's own pull quote ([SocialPullQuote]): the same rule down
 * the side and the same coffee ink the journal's quote panel and the community's
 * text posts wear, so a quote is one thing across the app. The member's own note
 * rides as the credit — their words tied to the passage they were about — and a
 * highlight with no note reads as what it is.
 */
@Composable
private fun MarginsCard(
    marks: List<ReaderMarkEntity>,
    onOpen: (ReaderMarkEntity) -> Unit
) {
    val accent = personalAccent()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurioIcon(CurioIcons.Bookmark, null, tint = accent, size = 17.dp)
                Text(
                    "From the margins",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${marks.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
            marks.take(MARGINS_SHOWN).forEach { mark ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { onOpen(mark) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SocialPullQuote(
                        words = mark.text.ifBlank { "Chapter ${mark.chapter}" },
                        credit = mark.note.ifBlank { mark.markKind.label }
                    )
                }
            }
            if (marks.size > MARGINS_SHOWN) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${marks.size - MARGINS_SHOWN} more in the reader",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

/** How many margins the book page quotes: enough to read back, never a wall. */
private const val MARGINS_SHOWN = 4

/**
 * v408 — WHICH CHAPTER A PAGE OF THE FILE LANDS IN.
 *
 * The file's own table of contents (an outline with page ranges, or an EPUB
 * whose chapter rows carry pageStart/pageEnd) is the only authority consulted:
 * the first chapter whose range contains the page. 0-range chapters are
 * skipped — an outline that names chapters but not pages cannot answer. Null
 * when no range claims the page, so the caller keeps what it had instead of
 * guessing.
 */
private fun chapterForPage(page: Int, chapters: List<com.curio.app.data.PersonalChapter>): Int? =
    chapters.firstOrNull { ch ->
        ch.pageStart > 0 && ch.pageEnd >= ch.pageStart && page in ch.pageStart..ch.pageEnd
    }?.number

/**
 * v411 — THE PAGE A CHAPTER OPENS AT (1-based), or null when the file's own
 * outline never said where it starts.
 *
 * The reverse of [chapterForPage], and the other half of the two steppers
 * agreeing: a page move names the chapter the page falls in, and a chapter
 * move names the page the chapter begins at. A chapter with no `pageStart`
 * (an EPUB from the catalog carries names, not ranges) answers null and the
 * page is left untouched rather than invented.
 */
private fun chapterStartPage(
    chapter: Int,
    chapters: List<com.curio.app.data.PersonalChapter>
): Int? = chapters.firstOrNull { it.number == chapter }
    ?.takeIf { it.pageStart > 0 }
    ?.pageStart

/**
 * v408 — THE PAGE THE MEMBER LAST READ, in file terms.
 *
 * For a PDF the reader's position row IS a page ([ReaderMarkEntity.positionIndex]),
 * so the answer is direct. For an EPUB (and a plain text) the position is a
 * block/section index, which is not a page of anything — those books show
 * chapter progress without a page bar rather than a bar pretending to count
 * pages it never had.
 */
private fun lastPageOf(mark: com.curio.app.data.ReaderMarkEntity?, isPdf: Boolean): Int =
    if (mark != null && isPdf) mark.positionIndex + 1 else 0

/**
 * v408 — THE PROGRESS CARD, REBUILT FROM THE FILE.
 *
 * What the old card knew was "chapter N of M" — a pair of steppers over a
 * number the member typed, even when the book's own file was sitting right
 * there knowing exactly which page they were on and which chapter that page
 * opens (member: "add proper progress from the pdf or epub tracking, and
 * also proper chapter updates if the pdf had it, with proper progress, and
 * use can change the progress like before").
 *
 * So the card now reads the READER'S OWN MEMORY ([ReaderMarkKind.POSITION],
 * one row per book + file, rewritten on every read): a PDF's position is a
 * page and an EPUB's is the chapter block the reader restored — both arrive
 * here as WHERE I AM, not as a second thing to keep in sync.
 *
 * ── The two clocks, and who wins ───────────────────────────────────
 * The member can still move progress BY HAND ("i'm on chapter 4", "page
 * 120"), exactly like before. The rule that keeps the two honest:
 *
 *  · A hand move writes the BOOK ROW (currentChapter / the page hint) and
 *    leaves the reader's POSITION row alone.
 *  · The READER writes only the POSITION row.
 *  · The card SHOWS whichever is further along — never backwards. Opening
 *    the reader after a hand move goes to the reader's own last position
 *    (its restore logic is untouched), so the member's real reading place
 *    can never be yanked by an edit here; a hand edit ahead of the reader
 *    simply shows as ahead until reading catches up.
 *
 * Page/Chapter layout: ONE gauge carries both measures (the fill is the page
 * fraction, the chapter openings are notches inside its track), and the two
 * tiles under it are the only places the chapter and the page are NAMED —
 * v412 removed the headline and the count row that said the same pair twice
 * more (member: "there are a lot of duplicates … remove the top 2 … make it
 * one beautiful progress view").
 */
@Composable
private fun ProgressCard(
    total: Int,
    current: Int,
    finished: Boolean,
    /** v408 — the file's own measures, when the book has one. */
    pageCount: Int,
    lastPage: Int,
    chapterNames: List<String>,
    /** v411 — the 1-based page each chapter opens at, for the gauge's notches
     *  (empty when the file's outline has names but no ranges). */
    chapterPages: List<Int> = emptyList(),
    onChapter: (Int) -> Unit,
    onTotal: (Int) -> Unit,
    onFinished: (Boolean) -> Unit,
    /** v408 — a hand move of the PAGE. Null when the book has no file to
     *  count pages against (the page row then stays hidden). */
    onPage: ((Int) -> Unit)? = null
) {
        val ink = MaterialTheme.colorScheme.onSurface
        val accent = personalAccent()
        // ── v413 — THE STEPPER'S OWN COUNT, UNTIL THE BOOK CATCHES UP ───────
        // The tiles printed the PERSISTED value and nothing else, so a step
        // only appeared after a database write AND a flow round trip — a hold
        // that ticked twenty times still displayed one step, which reads as a
        // dead button (member: "the tap and hold works i hear the haptics but
        // the count only goes 1 and no more"). These three overlays are the
        // number the FINGER has reached: a step moves its overlay the instant
        // it is pressed, the write follows behind it, and the moment the
        // book's own value changes the overlay steps aside for it. The card is
        // therefore either showing the truth or the truth in flight, never a
        // stale read.
        var chapterMove by remember { mutableStateOf<Int?>(null) }
        var pageMove by remember { mutableStateOf<Int?>(null) }
        var totalMove by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(current) { chapterMove = null }
        LaunchedEffect(lastPage) { pageMove = null }
        LaunchedEffect(total) { totalMove = null }
        val shownChapter = chapterMove ?: current
        val shownPage = pageMove ?: lastPage
        val shownTotal = totalMove ?: total
        // A step reads the OVERLAY, never the persisted value — that is what
        // lets a hold pile onto itself instead of re-proposing the same "+1".
        val stepChapter: (Int) -> Unit = { delta ->
            val next = (shownChapter + delta)
                .coerceAtLeast(0)
                .coerceAtMost(if (shownTotal > 0) shownTotal else 999)
            if (next != shownChapter) {
                chapterMove = next
                onChapter(next)
            }
        }
        val stepPage: (Int) -> Unit = { delta ->
            val next = (shownPage + delta).coerceIn(0, pageCount)
            if (next != shownPage) {
                pageMove = next
                onPage?.invoke(next)
            }
        }
        val stepTotal: (Int) -> Unit = { delta ->
            val next = (shownTotal + delta).coerceIn(0, 999)
            if (next != shownTotal) {
                totalMove = next
                onTotal(next)
            }
        }
        // ── THE ONE MEASURE THE CARD IS ABOUT ──────────────────────────────
        // The honest fraction of the book: its own PAGES when there is a file
        // to count (a PDF), else the chapter run. Finished is full, always.
        // Read off the overlays, so the gauge moves WITH the finger rather than
        // a round trip behind it.
        val fraction = when {
            finished -> 1f
            pageCount > 0 && shownPage > 0 ->
                (shownPage.toFloat() / pageCount.toFloat()).coerceIn(0f, 1f)
            shownTotal > 0 -> (shownChapter.toFloat() / shownTotal.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
        Column(Modifier.padding(16.dp)) {
            // ── THE HEAD: ONE LABEL, ONE ACTION (v412) ────────────────────
            // The card opened with the chapter name over "Page 87 of 312" —
            // the SAME place the two tiles below state, in the SAME units, so
            // the member read their position twice before reaching the
            // controls and the card felt like three progress views stacked
            // (member: "there are a lot of duplicates … remove the top 2").
            // The headline is gone; only the label and its one action stay.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "READING PROGRESS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.1.sp
                    ),
                    color = personalAccentInk().copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                if (finished) {
                    Surface(
                        onClick = { onFinished(false) },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            "Reading again",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ink.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                        )
                    }
                } else {
                    Surface(
                        onClick = { onFinished(true) },
                        shape = RoundedCornerShape(50),
                        // v412 — opaque: the accent is mixed into the card the
                        // pill sits on, so the page never shows through it.
                        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.16f)
                    ) {
                        Text(
                            "Mark finished",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = personalAccentInk(),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // ── THE GAUGE — ONE BAR, AND IT CARRIES BOTH FACTS (v411) ──
            // The card used to draw TWO progress bars — a page gauge and, under
            // it, a whole row of chapter ticks — which said "how far" twice, in
            // two units, and had to be read twice (member: "in book view there
            // are 2 progress bars now which is again bad can u please fix its
            // design"). There is ONE bar now: the gauge carries the page
            // fraction, and the chapter openings are drawn INSIDE its track as
            // notches, so the run you are in and how much of the file is left
            // read in a single look.
            if (pageCount > 0 || total > 0) {
                ReadingGauge(
                    fraction = fraction,
                    chapterStarts = chapterPages,
                    pageCount = pageCount,
                    // The notch is the CARD's own fill, so a chapter line reads
                    // as a cut in the bar rather than another colour on it.
                    notch = MaterialTheme.colorScheme.surfaceContainerLow,
                    accent = accent,
                    ink = ink
                )
            }
            if (finished) {
                // The one place the finished state is said — the gauge is
                // full and the action reads "Reading again" above it.
                Spacer(Modifier.height(10.dp))
                Text(
                    "Every chapter closed",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f)
                )
            } else {
                // ── THE CONTROLS — say where you are, or how long the book is. ──
                // A hand move writes the BOOK ROW (currentChapter / currentPage)
                // and leaves the reader's own memory alone, so the reader still
                // opens where it was left and a move here can never yank the
                // place they are really reading at (see this card's note on the
                // two clocks). Hidden while finished: a finished book has read
                // all of it, and "Reading again" is what un-closes the
                // chapters.
                //
                // v412 — and the row that repeated "Chapter 5 of 40 / 312
                // pages" UNDER the bar is gone too: the tiles already carry
                // both counts, so it was the same fact a third time (member:
                // "remove the top 2 … keep the last one with the +- button").
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(Modifier.height(12.dp))
                // ── TWO TILES, THEN ONE QUIET SETTING (v411) ──
                // WHERE YOU ARE is two things you change while reading, so they
                // get the space and the emphasis (a tile each, the chapter tile
                // naming the CHAPTER and not just its number), while HOW LONG
                // THE BOOK IS is a setting you touch once and sits below them
                // in the quiet register (v412: a footer rail, not a third row
                // that looked like another place you are).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ProgressTile(
                        label = "I'm on chapter",
                        value = chapterNames.getOrNull(shownChapter - 1)
                            ?.takeIf { it.isNotBlank() }
                            ?: if (shownChapter > 0) "Chapter $shownChapter" else "Not started",
                        detail = if (shownTotal > 0) {
                            "${shownChapter.coerceAtLeast(0)} of $shownTotal"
                        } else {
                            ""
                        },
                        ink = ink,
                        modifier = Modifier.weight(1f),
                        onDecrease = { stepChapter(-1) },
                        onIncrease = { stepChapter(1) }
                    )
                    if (onPage != null && pageCount > 0) {
                        ProgressTile(
                            label = "Page",
                            value = if (shownPage > 0) "$shownPage" else "—",
                            detail = "of $pageCount",
                            ink = ink,
                            modifier = Modifier.weight(1f),
                            onDecrease = { stepPage(-1) },
                            onIncrease = { stepPage(1) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                BookLengthRow(total = shownTotal, onStep = stepTotal, ink = ink)
            }
        }
    }
}

/**
 * v412 — HOW LONG THE BOOK IS, AS A QUIET FOOTER RAIL.
 *
 * The row used to read "This book has" against a chevron stepper with the number
 * floating between the arrows — a THIRD control row on a card that had just
 * drawn two, and one that borrowed the same shape as "I'm on chapter" (member:
 * "change its view and style and design"). It is a footer now: an uppercase rail
 * label on the left, and one compact pill on the right holding the count
 * between its two ends, so the setting is unmistakably not a place you are.
 */
@Composable
private fun BookLengthRow(
    total: Int,
    onStep: (Int) -> Unit,
    ink: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "BOOK LENGTH",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp
            ),
            color = ink.copy(alpha = 0.55f),
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                TileStepButton(CurioIcons.Remove, "One fewer chapter", ink) {
                    onStep(-1)
                }
                Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (total > 0) "$total" else "—",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ink
                    )
                }
                TileStepButton(CurioIcons.Add, "One more chapter", ink) {
                    onStep(1)
                }
            }
        }
    }
}

/**
 * v411 — THE ONE GAUGE.
 *
 * A single bar for the whole book: the fill is how far in the member is (the
 * file's own pages when there are pages to count, else the chapter run), and
 * the chapter openings are drawn INSIDE the track as notches — so the structure
 * of the book and the position in it share one bar instead of being told on two
 * ([MAX_GAUGE_NOTCHES] keeps a 300-chapter file from drawing 300 hairlines).
 *
 * [chapterStarts] are the 1-based pages the chapters open at (empty for a file
 * whose outline carries names but no ranges — an EPUB from the catalogue), and
 * the notches are skipped entirely when there is no page to place them on.
 */
@Composable
private fun ReadingGauge(
    fraction: Float,
    chapterStarts: List<Int>,
    pageCount: Int,
    notch: Color,
    accent: Color,
    ink: Color
) {
    // ── THE FILL TRAVELS, AND THE BAR BREATHES (v412) ─────────────────────
    //
    // A held stepper writes a new fraction many times a second, and a bar that
    // SNAPS to each one reads as broken rather than as progress — so the fill
    // and the figure both resolve through their own animation and chase the
    // number instead of jumping to it (member: "make the progress gauge animate
    // smoothly as the held stepper moves the count"). On top of that a slow
    // sheen drifts along the FILLED part while nothing is moving, so a bar at
    // rest is alive rather than a dead rectangle ("and also a subtle idle
    // animation, gradient style maybe").
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 380f),
        label = "gauge-fill"
    )
    val sheen by rememberInfiniteTransition(label = "gauge-idle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gauge-sheen"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(11.dp)
                .clip(RoundedCornerShape(50))
                .background(ink.copy(alpha = 0.09f))
        ) {
            val filled = size.width * animated
            // A GRADIENT, not a flat block: the fill runs from the accent into
            // a lighter READING of the same accent, which is what makes a thin
            // bar read as a lit metre instead of a painted strip. Both stops
            // are opaque lerps (no alpha on a fill — the Pantone rule), so
            // nothing shows through the bar.
            val lift = lerp(accent, Color.White, 0.30f)
            if (filled > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accent, lift),
                        startX = 0f,
                        endX = size.width
                    ),
                    size = Size(filled, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
                // The idle sheen, clipped to the FILL so it can only ever read
                // as light travelling along the progress — never as a band
                // crossing the empty track.
                clipRect(right = filled) {
                    val band = size.width * 0.22f
                    val centre = -band + (size.width + band * 2f) * sheen
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(accent, lift, accent),
                            startX = centre - band,
                            endX = centre + band
                        ),
                        topLeft = Offset(centre - band, 0f),
                        size = Size(band * 2f, size.height)
                    )
                }
            }
            if (pageCount > 1 && chapterStarts.size in 2..MAX_GAUGE_NOTCHES) {
                val stroke = 1.5.dp.toPx()
                chapterStarts.forEach { start ->
                    val at = (start - 1).toFloat() / (pageCount - 1).toFloat() * size.width
                    if (at > stroke && at < size.width - stroke) {
                        drawLine(
                            color = notch,
                            start = Offset(at, 0f),
                            end = Offset(at, size.height),
                            strokeWidth = stroke
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(11.dp))
        // The figure follows the bar rather than jumping with it, so a held
        // stepper counts up smoothly instead of flickering.
        Text(
            "${(animated * 100f).toInt()}%",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = personalAccentInk()
        )
    }
}

/**
 * v411 — ONE OF THE TWO CHOICES: a name, the value it holds, and the two ways to
 * move it.
 *
 * The value is the PROMINENT line — a chapter is NAMED, not numbered, because
 * the name is what a reader says when asked where they are — and the count sits
 * under it. The stepper is two round buttons rather than a chevron rail: the
 * value is already said above them, and printing it a second time between the
 * arrows is exactly the duplication this card was rebuilt to remove.
 */
@Composable
private fun ProgressTile(
    label: String,
    value: String,
    detail: String,
    ink: Color,
    modifier: Modifier = Modifier,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(
        modifier = modifier
            .curioCardShadow(RoundedCornerShape(16.dp), 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp
            ),
            color = personalAccentInk().copy(alpha = 0.8f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (detail.isNotBlank()) {
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.55f)
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TileStepButton(CurioIcons.Remove, "One back", ink, onDecrease)
            TileStepButton(CurioIcons.Add, "One on", ink, onIncrease)
        }
    }
}

/**
 * v412 — ONE ROUND END OF A STEPPER, AND IT KEEPS GOING WHILE IT IS HELD.
 *
 * A tap moves the value one step; pressing and holding past
 * [TileStepHoldDelayMs] starts a repeat that ACCELERATES — every tick comes a
 * little sooner than the last, from [TileStepRepeatStartMs] down to
 * [TileStepRepeatMinMs] — so a hundred chapters or three hundred pages are one
 * press of a finger away instead of a hundred taps (member: "make the plus
 * holdable and when held it goes fast the page count"). Every step, the tap
 * and each tick alike, plays a light haptic tick, so the count can be felt
 * moving without watching the number.
 *
 * The press gesture owns the whole thing rather than sitting beside a
 * `Surface(onClick)`: one detector means one code path for "was this a tap or a
 * hold", so a hold can never ALSO fire the tap that ended it (the same
 * construction as the reader page-bar arrow). The repeat runs in the
 * composition's own scope, so it dies with the card.
 */
@Composable
private fun TileStepButton(glyph: String, label: String, ink: Color, onClick: () -> Unit) {
    val repeater = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    // ── v413 — EVERY STEP READS THE CURRENT NUMBER (the real bug behind "the
    // +- only accepts one tap") ────────────────────────────────────────────
    // `pointerInput(Unit)` runs its block exactly ONCE, so the `onClick` it
    // closed over was the one handed in on the FIRST composition — and every
    // step after that computed from the chapter/page/total the card was
    // showing back then. Tapping "+" moved 0 → 1 and then re-proposed "start
    // + 1" forever, so the count stuck at one no matter how many times it was
    // pressed, and only jumped ahead once the card happened to be rebuilt from
    // scratch (member: "the +- only accepts one tap ... it goes forward but
    // after so long or when i scroll a little and tap again it goes ahead" —
    // scrolling the LazyColumn into a new composition is exactly that
    // rebuild). rememberUpdatedState hands the gesture the LATEST callback
    // without restarting it; restarting pointerInput on the callback instead
    // would drop the press the finger is still holding.
    val step = rememberUpdatedState(onClick)
    // Plain flag (not Compose state): nothing in composition reads it, it only
    // tells the trailing tap whether the hold already did the work.
    val held = remember { booleanArrayOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(110),
        label = "tileStepPress"
    )
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .size(34.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        held[0] = false
                        val job = repeater.launch {
                            delay(TileStepHoldDelayMs)
                            held[0] = true
                            var interval = TileStepRepeatStartMs
                            while (true) {
                                step.value()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                delay(interval)
                                interval = (interval - TileStepRepeatAccelMs)
                                    .coerceAtLeast(TileStepRepeatMinMs)
                            }
                        }
                        try {
                            // Suspends until the finger lifts (or the gesture is
                            // cancelled) — which is what stops the metronome.
                            tryAwaitRelease()
                        } finally {
                            job.cancel()
                            pressed = false
                        }
                    },
                    onTap = {
                        if (!held[0]) {
                            step.value()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )
            }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(glyph, label, tint = ink.copy(alpha = 0.8f), size = 17.dp)
        }
    }
}

/**
 * v389 — WHY I PICKED IT UP, as the eye reads it.
 *
 * The same words the field holds, drawn as a note on the page rather than as a
 * box: no placeholder, no caret, no container that says "type here". It is
 * something to read back, which is what the reading side is for.
 */
@Composable
private fun BlurbReadBack(value: String) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 15.dp)
                    .clip(RoundedCornerShape(50))
                    .background(personalAccent())
            )
            Column {
                Text(
                    "Why I picked it up",
                    style = MaterialTheme.typography.labelSmall,
                    color = personalAccentInk()
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    value,
                    style = TextStyle(
                        fontFamily = LoraFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = ink
                    )
                )
            }
        }
    }
}

@Composable
private fun BlurbField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            maxLines = 3,
            textStyle = TextStyle(
                fontFamily = LoraFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = ink
            ),
            cursorBrush = SolidColor(personalAccent()),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "Why I picked it up…",
                            style = TextStyle(
                                fontFamily = LoraFontFamily,
                                fontSize = 13.sp,
                                color = ink.copy(alpha = 0.36f)
                            )
                        )
                    }
                    inner()
                }
            }
        )
    }
}

/**
 * v409 — WHERE A CHAPTER SITS IN THE READING RUN.
 *
 * A chapter row is the same row whether the book is at chapter two or done
 * with, so without a state the member has to count rows to find their place.
 * These three say it in the row itself (the number's weight, the side rule and
 * the subtitle), which is also what makes "Mark finished" visibly close every
 * chapter at once — and "Reading again" visibly open them back up.
 */
private enum class ChapterReadState(val label: String) {
    DONE("read"),
    HERE("reading now"),
    AHEAD("to read")
}

/**
 * One chapter: the review you wrote (read), or the invitation to write when
 * there is nothing yet. Tapping the row opens the writing IN PLACE.
 */
@Composable
private fun ChapterCard(
    chapter: Int,
    /** v409 — where this chapter sits in the reading run. */
    state: ChapterReadState,
    /** The catalog's name for this chapter (blank for a book added by hand). */
    catalogTitle: String = "",
    /** The pages this chapter spans, e.g. "pp. 121–154". */
    catalogPages: String = "",
    review: PersonalNoteEntity?,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    val done = state == ChapterReadState.DONE
    val here = state == ChapterReadState.HERE
    // `doc` re-parses the stored body on every access — read it once per
    // version of the review, never per recomposition.
    val words = remember(review?.id, review?.updatedAtMillis) { review?.doc?.wordsLabel().orEmpty() }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Column(
            modifier = Modifier
                // A written chapter wears the accent rule down its side — the
                // same language the page's quotes use, so "there is writing
                // here" is visible before a single word is read.
                .drawBehind {
                    if (review == null && !here) return@drawBehind
                    val barWidth = 3.dp.toPx()
                    drawRoundRect(
                        color = accent,
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(barWidth / 2f)
                    )
                }
                .padding(horizontal = 15.dp, vertical = 13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        here -> accent
                        done -> accent.copy(alpha = 0.22f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            chapter.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                here -> personalAccentInk()
                                done -> personalAccentInk()
                                else -> ink.copy(alpha = 0.62f)
                            }
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        // The book's OWN chapter name leads when the catalog has
                        // one; the number stays for the ones it does not.
                        // The edition's own title with our number said once —
                        // see chapterNameOnly (v389).
                        text = chapterNameOnly(chapter - 1, catalogTitle),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(
                            catalogPages.takeIf { it.isNotBlank() },
                            state.label,
                            when {
                                review == null -> null
                                review.preview.isBlank() -> "open to keep writing"
                                else -> words
                            }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (here) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (here) personalAccentInk() else ink.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (review != null) {
                    Surface(
                        onClick = onDelete,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CurioIcon(
                                CurioIcons.Delete,
                                "Remove chapter $chapter review",
                                tint = ink.copy(alpha = 0.6f),
                                size = 15.dp
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Surface(
                    onClick = onOpen,
                    shape = RoundedCornerShape(50),
                    color = if (review == null) accent.copy(alpha = 0.24f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        if (review == null) "Write" else "Open",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (review == null) personalIconTint(accent) else ink.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }

            // The review reads HERE, in place: the member's own words, before
            // they decide to open the chapter's page.
            if (review != null && review.preview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    review.preview,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LoraFontFamily),
                    color = ink.copy(alpha = 0.72f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** The other door on the book page: find a copy to download. */
@Composable
private fun DownloadPill(enabled: Boolean, onClick: () -> Unit) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                CurioIcons.Download,
                null,
                tint = personalIconTint(accent),
                size = 14.dp
            )
            Text(
                "Download help",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = personalIconTint(accent)
            )
        }
    }
}

/**
 * DOWNLOAD HELP — the format chooser.
 *
 * Two formats, and that is the whole sheet: the app searches the web for the
 * book as a PDF or as an EPUB (the file-type the member chose is added to the
 * search for them, which is the "hidden extension" — nobody has to know the
 * syntax), and opens the results in their browser. It deliberately does not
 * host or download anything itself: it hands the member to the pages that do.
 */
@Composable
private fun DownloadHelpSheet(
    title: String,
    author: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accent = personalAccent()
    val ink = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Find a copy",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            // Search uses ordinary title/author text; free-public-domain search
            // engines often ignore or reject filetype qualifiers.
            DownloadFormatRow(
                tile = "PDF",
                label = "Search for a PDF",
                accent = accent
            ) { openDownloadSearch(context, title, author, "pdf"); onDismiss() }
            DownloadFormatRow(
                tile = "EPUB",
                label = "Search for an EPUB",
                accent = accent
            ) { openDownloadSearch(context, title, author, "epub"); onDismiss() }
            DownloadFormatRow(
                tile = "FREE",
                label = "Search free public-domain copies",
                accent = accent
            ) { openDownloadSearch(context, title, author, "gutenberg"); onDismiss() }
        }
    }
}

@Composable
private fun DownloadFormatRow(
    tile: String,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // The extension itself, in a coloured tile — no icon glyph risk,
            // and it reads as a format badge the member can recognise at once.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tile,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = personalIconTint(accent)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink,
                modifier = Modifier.weight(1f)
            )
            CurioIcon(
                CurioIcons.OpenInNew,
                null,
                tint = ink.copy(alpha = 0.45f),
                size = 16.dp
            )
        }
    }
}

/**
 * The search itself: the book's title and author in quotes, then the file
 * type the member chose — the syntax engines answer "where is this book as a
 * PDF/EPUB" with, and it costs them nothing to read it.
 */
private fun openDownloadSearch(
    context: android.content.Context,
    title: String,
    author: String,
    extension: String
) {
    val name = buildString {
        append(title.trim())
        if (author.isNotBlank()) {
            append(' ')
            append(author.trim())
        }
    }
    // v389g — THE PDF DOOR ASKS THE ENGINE FOR PDFs; THE OTHER TWO ASK IN WORDS.
    //
    // "… free pdf download" is words on a page, and an engine reads them as
    // words: the results were articles ABOUT the book, pages with no file on
    // them, and the odd scraper. `filetype:pdf` is an OPERATOR — it narrows the
    // answer to documents the engine has actually indexed as PDFs, which is the
    // whole question the member is asking (user request: "for the download help
    // only for the pdf use filetype:pdf not the text search only").
    //
    // EPUB and the public-domain door KEEP the words ("text search of free epub
    // download like that is for epub and the bottom last also"). A
    // `filetype:epub` there would pin the answer to one extension, while what
    // actually helps is a page that LISTS a free edition — the words are the
    // right tool for that, and the bottom door names the library outright.
    val query = when (extension) {
        "pdf" -> "$name filetype:pdf"
        "gutenberg" -> "$name free public domain epub download"
        else -> "$name free $extension download"
    }
    val url = "https://www.google.com/search?q=" +
        java.net.URLEncoder.encode(query, "UTF-8")
    openSearchUrl(context, url)
}

/** "Look it up": asks again for the book's catalog record / page count. */
@Composable
private fun LookUpPill(lookingUp: Boolean, onClick: () -> Unit) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                CurioIcons.Search,
                null,
                tint = personalIconTint(accent),
                size = 14.dp
            )
            Text(
                if (lookingUp) "Looking it up…" else "Look it up",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = personalIconTint(accent)
            )
        }
    }
}

/**
 * The catalog's own blurb for the book, when Curio has one.
 *
 * v389 — it can arrive COLLAPSED: while the member is writing, the description
 * is one line with the source beside it, and a tap opens the whole thing. The
 * words themselves are untouched either way, so a collapsed card is folded, not
 * hidden.
 */
@Composable
private fun SynopsisCard(synopsis: String, source: String, collapsed: Boolean = false) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    // The tap opens it; the mode only decides where it STARTS.
    var open by remember(collapsed) { mutableStateOf(!collapsed) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable { if (collapsed) open = !open }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                )
                Text(
                    "ABOUT THIS BOOK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = personalIconTint(accent)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    source,
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.4f)
                )
                if (collapsed) {
                    CurioIcon(
                        if (open) CurioIcons.KeyboardArrowUp else CurioIcons.KeyboardArrowDown,
                        if (open) "Fold the description" else "Open the description",
                        tint = ink.copy(alpha = 0.45f),
                        size = 18.dp
                    )
                }
            }
            if (open) {
                Spacer(Modifier.height(8.dp))
                Text(
                    synopsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = LoraFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 24.sp
                    ),
                    color = ink.copy(alpha = 0.82f)
                )
            }
        }
    }
}

/**
 * v389 — THE BOOK'S OWN REVIEW, AS A DOOR.
 *
 * One page for the whole book (CurioRoutes.BOOK_REVIEW). It shows what the
 * member already wrote — the first words and how much of it there is — or, when
 * the page is blank, what the page is FOR, and it opens either way: writing
 * about a book should not be an expedition through its chapters.
 */
@Composable
private fun BookReviewDoor(
    preview: String,
    words: String,
    hasWriting: Boolean,
    ink: Color,
    onClick: () -> Unit
) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.22f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.MenuBook,
                        null,
                        tint = personalAccentInk(),
                        size = 19.dp
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Book review",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
                )
                Text(
                    text = if (hasWriting) {
                        listOfNotNull(
                            words.takeIf { it.isNotBlank() },
                            preview.takeIf { it.isNotBlank() }
                        ).joinToString(" \u00b7 ").ifBlank { "Open to keep writing" }
                    } else {
                        "The whole book at once"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CurioIcon(
                CurioIcons.ChevronRight,
                null,
                tint = ink.copy(alpha = 0.4f),
                size = 18.dp
            )
        }
    }
}

/**
 * v389 — THE BOOK PAGE'S FLOATING READ PILL.
 *
 * One door, two states: a book with its own file says "Read" and opens it, and
 * a book without one says "Read a file" and asks for it. It is a SOLID accent
 * pill with on-accent ink because it is the page's one primary action — the
 * look-up and download pills beside it are tinted, and a third tinted pill
 * would make none of them read as the thing to do.
 *
 * HOLD IT for the file itself: the tap reads, the hold asks WHICH document
 * Curio opens (see the page's own file menu). It is the same door — the
 * difference is how long the finger stays, which is the one thing a second
 * pill in this corner could not say without crowding the page.
 */
@Composable
private fun BookReadPill(
    hasFile: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = personalAccentInk(),
        shadowElevation = 8.dp,
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurioIcon(
                CurioIcons.MenuBook,
                null,
                tint = MaterialTheme.colorScheme.surface,
                size = 18.dp
            )
            Text(
                if (hasFile) "Read" else "Read a file",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

/** Past this many chapters the tick rail stops being readable. */
private const val MAX_TICKS = 48

/**
 * v411 — how many chapter notches the gauge will draw before it gives up.
 *
 * A bar is about 200dp wide; past ~80 openings the hairlines stop reading as
 * chapters and start reading as a texture, so a very long file simply gets the
 * clean fill (its chapter count is still stated under the bar).
 */
private const val MAX_GAUGE_NOTCHES = 80

// ── v412 — A HELD STEPPER RUNS, IT DOES NOT WALK ────────────────────────────
/** How long a tile stepper must be held before it starts repeating. */
private const val TileStepHoldDelayMs = 340L
/** The first repeat's interval — one step every [TileStepRepeatStartMs]. */
private const val TileStepRepeatStartMs = 160L
/** The fastest the repeat gets; it never comes sooner than this. */
private const val TileStepRepeatMinMs = 50L
/** Shaved off the interval after every tick, so the count visibly accelerates. */
private const val TileStepRepeatAccelMs = 16L

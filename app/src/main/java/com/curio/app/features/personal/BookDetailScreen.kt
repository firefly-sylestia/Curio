package com.curio.app.features.personal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
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
import com.curio.app.features.community.SocialPullQuote
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
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
    val book by produceState<com.curio.app.data.PersonalBookEntity?>(initialValue = null) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBook(bookId).collect { value = it }
        }
    }
    val notes by produceState(initialValue = emptyList<PersonalNoteEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBookNotes(bookId).collect { value = it }
        }
    }
    // THE MARGINS: what the member marked while READING this book in Curio's
    // reader. A highlight carries the passage's own WORDS, so the page can hold
    // it as a quote without opening the file again.
    val margins by produceState(initialValue = emptyList<ReaderMarkEntity>(), bookId) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBookMarks(bookId).collect { value = it }
        }
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

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            title = current?.title ?: " ",
            subtitle = current?.author.orEmpty().ifBlank { "Your book" },
            titleRevealed = titleRolled,
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
                            // The app's own doors belong to the WRITING side: a
                            // reader does not need "look it up" or a download
                            // search sitting over the words they came back for.
                            // They FOLD with the switch instead of appearing the
                            // instant the pen is pressed, so the eye/pen flip is
                            // one move rather than a page that snaps (user
                            // report: the family's switch "looks clanky").
                            AnimatedVisibility(
                                visible = editing,
                                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                                exit = fadeOut(tween(120)) + shrinkVertically(tween(170))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LookUpPill(lookingUp = lookingUp) { lookupTick += 1 }
                                    DownloadPill(enabled = true) { downloadSheet = true }
                                }
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
                        onChapter = { chapter ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching { PersonalRepositoryHolder.repo.setProgress(bookId, chapter) }
                                }
                            }
                        },
                        onTotal = { chapters ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching {
                                        PersonalRepositoryHolder.repo.saveBook(
                                            current.copy(totalChapters = chapters)
                                        )
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

@Composable
private fun ProgressCard(
    total: Int,
    current: Int,
    finished: Boolean,
    onChapter: (Int) -> Unit,
    onTotal: (Int) -> Unit,
    onFinished: (Boolean) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            finished -> "Finished"
                            total <= 0 -> "Not started"
                            else -> "Chapter $current"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ink
                    )
                    Text(
                        if (total > 0) "$total chapters" else "Set how long the book is",
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.copy(alpha = 0.55f)
                    )
                }
                if (finished) {
                    Surface(
                        onClick = { onFinished(false) },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            "Reading again",
                            style = MaterialTheme.typography.labelMedium,
                            color = ink.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Surface(
                        onClick = { onFinished(true) },
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            "Mark finished",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = personalAccentInk(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // One tick per chapter, so the number is a PLACE and not a figure:
            // you can see the run you are in and how much is left at a glance.
            if (total in 1..MAX_TICKS) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(total) { index ->
                        val done = finished || index < current
                        val here = !finished && index == current - 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (here) 7.dp else 3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when {
                                        done -> accent
                                        here -> personalAccentInk()
                                        else -> ink.copy(alpha = 0.12f)
                                    }
                                )
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "I'm on",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                ChapterStepper(
                    count = current,
                    onChange = { onChapter(it.coerceAtMost(if (total > 0) total else 999)) },
                    accent = accent,
                    ink = ink
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "The book has",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                ChapterStepper(count = total, onChange = onTotal, accent = accent, ink = ink)
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
 * One chapter: the review you wrote (read), or the invitation to write when
 * there is nothing yet. Tapping the row opens the writing IN PLACE.
 */
@Composable
private fun ChapterCard(
    chapter: Int,
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
                    if (review == null) return@drawBehind
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
                    color = if (review != null) accent.copy(alpha = 0.24f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            chapter.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (review != null) personalAccentInk() else ink.copy(alpha = 0.62f)
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        // The book's OWN chapter name leads when the catalog has
                        // one; the number stays for the ones it does not.
                        text = catalogTitle.takeIf { it.isNotBlank() } ?: "Chapter $chapter",
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
                            when {
                                review == null -> "Nothing written yet"
                                review.preview.isBlank() -> "Open to keep writing"
                                else -> words
                            }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.copy(alpha = 0.5f),
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
    val query = buildString {
        append(title.trim())
        if (author.isNotBlank()) {
            append(' ')
            append(author.trim())
        }
        if (extension == "gutenberg") append(" free public domain epub download")
        else append(" free ").append(extension).append(" download")
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

package com.curio.app.features.personal

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.openSearchUrl
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
        // Skip auto-fetch when the book is already complete and this is
        // the initial pass (lookupTick == 0). Manual taps still run.
        if (lookupTick == 0 && alreadyComplete) return@LaunchedEffect
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
            onBack = { navController.popBackStack() }
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

        LazyColumn(
            // weight, not fillMaxSize: the tool dock is the column's LAST child
            // and has to stay visible below the list (and above the keyboard).
            modifier = Modifier.fillMaxWidth().weight(1f),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LookUpPill(lookingUp = lookingUp) { lookupTick += 1 }
                            DownloadPill(enabled = !lookingUp) { downloadSheet = true }
                        }
                        if (lookingUp || lookupNote != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                lookupNote ?: "Looking it up\u2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        BlurbField(
                            value = blurb,
                            onValueChange = { blurb = it },
                            enabled = blurbSeeded
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
                        source = if (catalogSynopsis.isNotBlank()) "Curio catalog" else "Open Library"
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
                key = { index -> "chapter-${index + 1}" }
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
                            color = ink
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
            // The extension is the app's own "hidden" search token — the member
            // picks a format and the app writes the filetype syntax for them.
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
        append('"')
        append(title.trim())
        append('"')
        if (author.isNotBlank()) {
            append(" \"")
            append(author.trim())
            append('"')
        }
        append(" filetype:")
        append(extension)
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

/** The catalog's own blurb for the book, when Curio has one. */
@Composable
private fun SynopsisCard(synopsis: String, source: String) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
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
            }
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

/** Past this many chapters the tick rail stops being readable. */
private const val MAX_TICKS = 48

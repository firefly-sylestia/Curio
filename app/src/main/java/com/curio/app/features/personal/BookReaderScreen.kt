package com.curio.app.features.personal

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.ReaderMarkEntity
import com.curio.app.data.ReaderMarkKind
import com.curio.app.data.newReaderMarkId
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.curio.app.ui.theme.WritingFontFamily
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import java.util.zip.ZipFile

/**
 * v389 — THE BOOK'S READER, as a place to READ.
 *
 * The first pass was a pager with a Prev/Next pair under it: one page at a time,
 * a header that never left, and nothing remembered between visits. A reader is
 * the opposite of all three — the words fill the screen, the chrome gets out of
 * the way by itself, and coming back lands exactly where the member stopped.
 *
 * WHAT IT DOES NOW
 *
 *  · EPUB / plain text read CONTINUOUSLY: the whole file is one scrolling
 *    column of paragraphs (headings kept as headings), so there is no page
 *    boundary to fight and no "next" to press.
 *  · The HEADER AND THE FOOTER HIDE THEMSELVES after a few seconds and come back
 *    on a tap — the same auto-hide whatever the format, so the reader never
 *    sees two different ideas of "reading mode".
 *  · WHERE THEY STOPPED is remembered per book AND per file ([ReaderMarkEntity],
 *    kind `POSITION`): the scroll settles, the position is written, and the next
 *    open starts there. That row IS the auto bookmark, so "auto marking the
 *    perfect read" costs no extra state.
 *  · MARKS: long-press a paragraph to highlight it (four inks), write a note on
 *    it, or drop a bookmark. A highlight's WORDS are stored on the mark, so the
 *    marks sheet can list them, jump back to them, and hand one on as a quote
 *    without re-parsing the file.
 *  · The INK of the page itself is adjustable (paper / sepia / night / white),
 *    because a book is read in bed as often as in daylight.
 *  · A PDF is read a PAGE at a time (a pager), which is what a PDF page is —
 *    rendered ONE page per frame instead of every page at open, which is what
 *    made a long PDF open slowly and hold a phone's memory hostage.
 */
@Composable
fun BookReaderScreen(navController: NavController, bookId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val book by produceState<com.curio.app.data.PersonalBookEntity?>(null, bookId) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBook(bookId).collect { value = it }
        }
    }
    // The document the book carries: its own file, or the legacy `content://`
    // handle an import parked in `coverUrl` before it had a column of its own.
    val document = book?.let { BookFiles.documentOf(it.documentPath, it.coverUrl) }.orEmpty()

    var content by remember(bookId, document) { mutableStateOf<ReaderContent?>(null) }
    var error by remember(bookId, document) { mutableStateOf<String?>(null) }

    LaunchedEffect(document) {
        if (document.isBlank()) {
            error = "This book has no file yet \u2014 add its PDF or EPUB from the book's page."
            return@LaunchedEffect
        }
        error = null
        content = null
        val result = withContext(Dispatchers.IO) {
            runCatching { readBook(context, document) }
        }
        result.onSuccess { content = it }
            .onFailure { error = "This file could not be opened in Curio." }
    }

    // ── The marks, and where the member stopped ────────────────────────
    val marks by produceState(initialValue = emptyList<ReaderMarkEntity>(), bookId, document) {
        if (document.isBlank()) return@produceState
        runCatching {
            PersonalRepositoryHolder.repo.observeReaderMarks(bookId, document).collect { value = it }
        }
    }
    var openedAt by remember(bookId, document) { mutableStateOf<ReaderMarkEntity?>(null) }

    // ── The reader's own look and its chrome ───────────────────────────
    var chrome by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<ReaderSheet?>(null) }
    var marking by remember { mutableStateOf<ReaderParagraph?>(null) }
    var noteFor by remember { mutableStateOf<ReaderParagraph?>(null) }
    var searching by remember { mutableStateOf<ReaderSearch?>(null) }

    // The chrome leaves on its own — that is what "auto hide" means — and it
    // stays while a sheet is up, because a sheet is a deliberate act.
    LaunchedEffect(chrome, sheet) {
        if (!chrome || sheet != null) return@LaunchedEffect
        delay(4200)
        chrome = false
    }

    /**
     * v389 — THE PAGE IS THE SWITCH.
     *
     * The chrome used to be reachable only by tapping the space AROUND the
     * words: every paragraph carried a long-press detector with no tap of its
     * own, and Compose consumes the press before it can bubble — so tapping the
     * page itself did nothing at all, and the tools, once auto-hidden, could not
     * be got back (user report: "the tools they have they disapear and they never
     * appear, so make it when i tap the page … it should appear and when i tap
     * again or scroll the tools gets hidden"). Both reading surfaces now answer a
     * tap themselves, and a scroll puts the chrome away.
     */
    fun tapPage() {
        chrome = !chrome
    }

    fun hideChrome() {
        if (chrome) chrome = false
    }

    val palette = readerPalette(ReaderLook.inkKey)

    // The two things a jump has to reach: the text list and the page pager.
    // Hoisted HERE, to the screen, because the marks and chapter sheets move
    // them — a state that a sheet has to reach is the screen's state, not the
    // composable's that happens to draw it. `pageCount` is a lambda over
    // `content`, so the pager is told its length the moment the file is read.
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState {
        (content as? ReaderContent.Pages)?.pageCount ?: 0
    }
    /**
     * Jump to a MARK's own place — a block index in a reflowable book, a page in
     * a PDF. It lands on the passage ITSELF: a highlight belongs to the words it
     * was made on, never to a heading that happens to share its number.
     */
    suspend fun jumpToMark(index: Int) {
        when (val loaded = content) {
            is ReaderContent.Text -> listState.scrollToItem(
                index.coerceIn(0, (loaded.blocks.size - 1).coerceAtLeast(0))
            )
            is ReaderContent.Pages -> pagerState.scrollToPage(
                index.coerceIn(0, (loaded.pageCount - 1).coerceAtLeast(0))
            )
            null -> Unit
        }
    }

    /**
     * Jump to a CHAPTER, which the chapter sheet names by its SECTION number
     * rather than by a block index — so this is where that heading is found, and
     * the jump lands on the heading that OPENS the chapter.
     */
    suspend fun jumpToChapter(section: Int) {
        when (val loaded = content) {
            is ReaderContent.Text -> {
                val heading = loaded.blocks.indexOfFirst { it.isHeading && it.section == section }
                listState.scrollToItem(
                    (if (heading >= 0) heading else section - 1)
                        .coerceIn(0, (loaded.blocks.size - 1).coerceAtLeast(0))
                )
            }
            is ReaderContent.Pages -> pagerState.scrollToPage(
                section.coerceIn(0, (loaded.pageCount - 1).coerceAtLeast(0))
            )
            null -> Unit
        }
    }

    // WHERE THEY ARE, said as a fact about the book — the chapter they are in,
    // how many marks they have left, the page of the file. It reads the LIVE
    // position (the list's first block, the pager's current page), so it follows
    // the reading instead of naming the place the book happened to open at.
    val positionLabel = when (val loaded = content) {
        is ReaderContent.Pages -> "Page ${pagerState.currentPage + 1} of ${loaded.pageCount}"

        is ReaderContent.Text -> {
            val at = loaded.blocks.getOrNull(listState.firstVisibleItemIndex)
            val place = at?.sectionTitle.orEmpty().ifBlank {
                at?.let { "Section ${it.section}" }.orEmpty()
            }
            val marked = marks.count { !it.isPosition }
            buildString {
                append(place)
                if (marked > 0) {
                    if (isNotEmpty()) append(" \u00b7 ")
                    append("$marked marked")
                }
            }
        }

        null -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
            // A tap ANYWHERE brings the chrome back (and takes it away again):
            // this sits UNDER the words, so it never eats a long press meant for
            // a paragraph.
            .pointerInput(Unit) {
                detectTapGestures(onTap = { chrome = !chrome })
            }
    ) {
        when {
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    error.orEmpty(),
                    color = palette.ink.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 32.dp),
                    style = TextStyle(fontFamily = LoraFontFamily, fontSize = 16.sp, lineHeight = 24.sp)
                )
            }

            content == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }

            else -> when (val loaded = content) {
                is ReaderContent.Text -> TextReader(
                    content = loaded,
                    listState = listState,
                    marks = marks,
                    palette = palette,
                    bookId = bookId,
                    document = document,
                    onOpenedAt = { openedAt = it },
                    onLongPress = { marking = it },
                    chromeVisible = chrome,
                    onTap = { tapPage() },
                    onScrolled = { hideChrome() },
                    query = searching?.query.orEmpty(),
                    hitIndex = searching?.current ?: -1,
                    hitLength = searching?.query?.length ?: 0
                )

                is ReaderContent.Pages -> PageReader(
                    pageCount = loaded.pageCount,
                    pagerState = pagerState,
                    document = document,
                    marks = marks,
                    palette = palette,
                    bookId = bookId,
                    onOpenedAt = { openedAt = it },
                    onLongPress = { index ->
                        marking = ReaderParagraph(
                            positionIndex = index,
                            section = index + 1,
                            text = "Page ${index + 1}",
                            isHeading = false,
                            isPage = true
                        )
                    },
                    onTap = { tapPage() },
                    onScrolled = { hideChrome() }
                )

                // `content` is a delegated property, so the null check above
                // cannot smart-cast it — this branch is what makes the `when`
                // exhaustive (the file was still being read a moment ago).
                null -> Unit
            }
        }

        ReaderChrome(
            visible = chrome,
            title = book?.title.orEmpty().ifBlank { "Reader" },
            palette = palette,
            positionLabel = positionLabel,
            onClose = { navController.popBackStack() },
            onSearch = {
                searching = ReaderSearch().also { started ->
                    started.isPaged = content is ReaderContent.Pages
                }
                sheet = ReaderSheet.SEARCH
            },
            onInk = { sheet = ReaderSheet.INK },
            onMarks = { sheet = ReaderSheet.MARKS },
            onChapters = { sheet = ReaderSheet.CHAPTERS }
        )
    }

    // -- THE SWEEP -------------------------------------------------------
    // Runs while a search is open, in the background, and hands the frame back
    // between pages (a PDF's words cost a parse to read, so a reader that
    // stuttered while it searched would have stopped being a reader). The hits
    // appear as they are found and the sheet says how far the sweep has got.
    val sweep = searching
    LaunchedEffect(sweep?.token) {
        val run = sweep ?: return@LaunchedEffect
        val asked = run.query.trim()
        run.hits = emptyList()
        run.current = -1
        run.scanned = 0
        run.total = 0
        if (asked.isBlank()) return@LaunchedEffect
        when (val loaded = content) {
            is ReaderContent.Text -> {
                val found = ArrayList<ReaderSearchHit>()
                loaded.blocks.forEachIndexed { index, block ->
                    if (block.text.contains(asked, ignoreCase = true)) {
                        found.add(ReaderSearchHit(index, block.text.take(120)))
                    }
                }
                run.hits = found
                run.total = loaded.blocks.size
                run.scanned = loaded.blocks.size
            }

            is ReaderContent.Pages -> {
                run.total = loaded.pageCount
                val found = ArrayList<ReaderSearchHit>()
                for (page in 0 until loaded.pageCount) {
                    val words = withContext(Dispatchers.IO) {
                        runCatching { extractPdfPageText(context, document, page) }.getOrNull()
                    }
                    val at = words?.text?.indexOf(asked, ignoreCase = true) ?: -1
                    if (at >= 0) {
                        found.add(
                            ReaderSearchHit(page, words?.text.orEmpty().aroundSnippet(at, asked.length))
                        )
                    }
                    run.hits = found.toList()
                    run.scanned = page + 1
                    kotlinx.coroutines.yield()
                }
            }

            null -> Unit
        }
    }

    when (sheet) {
        ReaderSheet.INK -> ReaderInkSheet(
            palette = palette,
            onPick = { key -> ReaderLook.inkKey = key },
            onDismiss = { sheet = null }
        )

        ReaderSheet.MARKS -> ReaderMarksSheet(
            marks = marks,
            position = openedAt ?: marks.firstOrNull { it.isPosition },
            palette = palette,
            onJump = { mark ->
                sheet = null
                scope.launch { jumpToMark(mark.positionIndex) }
            },
            onDelete = { mark ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { PersonalRepositoryHolder.repo.deleteReaderMark(mark.id) }
                    }
                }
            },
            onDismiss = { sheet = null }
        )

        ReaderSheet.CHAPTERS -> ReaderChaptersSheet(
            content = content,
            palette = palette,
            onPick = { index ->
                sheet = null
                scope.launch { jumpToChapter(index) }
            },
            onDismiss = { sheet = null }
        )

        ReaderSheet.SEARCH -> ReaderSearchSheet(
            search = searching ?: ReaderSearch(),
            palette = palette,
            onQuery = { asked -> searching?.query = asked },
            onSubmit = { searching?.token = (searching?.token ?: 0) + 1 },
            onPick = { index ->
                searching?.current = index
                sheet = null
                scope.launch {
                    when (content) {
                        is ReaderContent.Text -> listState.scrollToItem(
                            index.coerceIn(0, ((content as ReaderContent.Text).blocks.size - 1).coerceAtLeast(0))
                        )
                        is ReaderContent.Pages -> pagerState.scrollToPage(
                            index.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
                        )
                        null -> Unit
                    }
                }
            },
            onDismiss = {
                sheet = null
                searching = null
            }
        )

        null -> Unit
    }

    marking?.let { paragraph ->
        ReaderMarkSheet(
            paragraph = paragraph,
            marks = marks,
            palette = palette,
            onHighlight = { color ->
                scope.launch {
                    saveReaderMark(
                        bookId = bookId,
                        document = document,
                        paragraph = paragraph,
                        kind = ReaderMarkKind.HIGHLIGHT,
                        text = paragraph.text,
                        colorKey = color.key
                    )
                }
                marking = null
            },
            onHighlightChapter = if (!paragraph.isHeading) null else {
                {
                    scope.launch {
                        val loaded = content
                        if (loaded is ReaderContent.Text) {
                            loaded.blocks.forEachIndexed { index, block ->
                                if (block.section != paragraph.section) return@forEachIndexed
                                saveReaderMark(
                                    bookId = bookId,
                                    document = document,
                                    paragraph = ReaderParagraph(
                                        index,
                                        block.section,
                                        block.text,
                                        block.isHeading
                                    ),
                                    kind = ReaderMarkKind.HIGHLIGHT,
                                    text = block.text,
                                    colorKey = ReaderHighlighter.AMBER.key
                                )
                            }
                        }
                    }
                    marking = null
                }
            },
            onNote = {
                noteFor = paragraph
                marking = null
            },
            onBookmark = {
                scope.launch {
                    saveReaderMark(
                        bookId = bookId,
                        document = document,
                        paragraph = paragraph,
                        kind = ReaderMarkKind.BOOKMARK,
                        text = paragraph.text.take(90)
                    )
                }
                marking = null
            },
            onRemove = { existing ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { PersonalRepositoryHolder.repo.deleteReaderMark(existing.id) }
                    }
                }
                marking = null
            },
            onDismiss = { marking = null }
        )
    }

    noteFor?.let { paragraph ->
        ReaderNoteDialog(
            palette = palette,
            initial = marks.firstOrNull { it.isNote && it.positionIndex == paragraph.positionIndex }
                ?.note.orEmpty(),
            onSave = { words ->
                scope.launch {
                    saveReaderMark(
                        bookId = bookId,
                        document = document,
                        paragraph = paragraph,
                        kind = ReaderMarkKind.NOTE,
                        text = paragraph.text,
                        note = words
                    )
                }
                noteFor = null
            },
            onDismiss = { noteFor = null }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// The two reading surfaces
// ────────────────────────────────────────────────────────────────────────────

/**
 * A CONTINUOUS READER: headings, then their paragraphs, all in ONE scrolling
 * list — no page breaks to fight, which is what "proper continuous scroll" means
 * for a reflowable book.
 *
 * The position is written when the scroll SETTLES (`collectLatest` + a delay:
 * each new scroll cancels the pending write), so a fast flick does not write a
 * hundred rows, and what is stored is where the finger actually stopped.
 */
@Composable
private fun TextReader(
    content: ReaderContent.Text,
    listState: LazyListState,
    marks: List<ReaderMarkEntity>,
    palette: ReaderPalette,
    bookId: String,
    document: String,
    onOpenedAt: (ReaderMarkEntity?) -> Unit,
    onLongPress: (ReaderParagraph) -> Unit,
    chromeVisible: Boolean,
    onTap: () -> Unit,
    onScrolled: () -> Unit,
    query: String,
    hitIndex: Int,
    hitLength: Int
) {
    val state = listState

    // A scroll means the member is moving through the book, and the chrome has
    // no business over the words while they do it (see [tapPage]).
    LaunchedEffect(state, onScrolled) {
        snapshotFlow { state.isScrollInProgress }
            .collect { scrolling -> if (scrolling) onScrolled() }
    }

    // PINCH makes the TYPE bigger, not the pixels: a reflowed book that is
    // magnified like a photograph is a worse book, and every reader on earth
    // re-lays the page out instead (v389).
    val zoomModifier = Modifier.pinchToZoom { zoom, _ ->
        ReaderLook.textScale = (ReaderLook.textScale * zoom).coerceIn(0.8f, 2.6f)
    }

    // Read the stored position once per open, and go there.
    var restored by remember(bookId, document) { mutableStateOf(false) }
    LaunchedEffect(bookId, document, content.blocks.size) {
        if (restored) return@LaunchedEffect
        val stored = withContext(Dispatchers.IO) {
            runCatching { PersonalRepositoryHolder.repo.readerPosition(bookId, document) }.getOrNull()
        }
        onOpenedAt(stored)
        if (stored != null && stored.positionIndex > 0) {
            state.scrollToItem(stored.positionIndex.coerceIn(0, (content.blocks.size - 1).coerceAtLeast(0)))
        }
        restored = true
    }

    LaunchedEffect(bookId, document, content.blocks.size, restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collectLatest { (index, _) ->
                delay(900)
                withContext(Dispatchers.IO) {
                    runCatching {
                        // The FRACTION is how far through the book this block is,
                        // which is what "where was I" means to a reader — the
                        // index alone says nothing about the shape of the book.
                        PersonalRepositoryHolder.repo.saveReaderPosition(
                            bookId = bookId,
                            sourceKey = document,
                            index = index,
                            fraction = index.toFloat() / (content.blocks.size - 1).coerceAtLeast(1)
                        )
                    }
                }
            }
    }

    LazyColumn(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .then(zoomModifier)
            // THE PAGE ANSWERS A TAP. The blocks below answer their own (a long
            // press marks a passage, a tap puts the chrome back), and this one
            // catches the presses that land in the gaps between them.
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        contentPadding = PaddingValues(
            start = 22.dp,
            end = 22.dp,
            top = if (chromeVisible) 76.dp else 34.dp,
            bottom = if (chromeVisible) 96.dp else 56.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            count = content.blocks.size,
            key = { index -> "block-$index" }
        ) { index ->
            val block = content.blocks[index]
            val highlight = marks.firstOrNull { it.isHighlight && it.positionIndex == index }
            val note = marks.firstOrNull { it.isNote && it.positionIndex == index }
            val bookmark = marks.firstOrNull { it.markKind == ReaderMarkKind.BOOKMARK && it.positionIndex == index }
            ReaderParagraphBlock(
                block = block,
                palette = palette,
                highlightColor = highlight?.let { readerHighlighter(it.colorKey).ink } ?: Color.Transparent,
                note = note?.note.orEmpty(),
                bookmarked = bookmark != null,
                onLongPress = { onLongPress(ReaderParagraph(index, block.section, block.text, block.isHeading)) },
                onTap = onTap,
                query = query,
                // The find the member is standing on wears the wash; the rest of
                // the matches in the same block still read as matches, which is
                // what a search's highlighting is for.
                hitHere = hitIndex == index && query.isNotBlank(),
                hitLength = hitLength
            )
        }
        item("the-end") {
            Text(
                "\u2014 end of the book \u2014",
                style = TextStyle(fontFamily = LoraFontFamily, fontSize = 13.sp),
                color = palette.ink.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
            )
        }
    }
}

/**
 * A PAGE READER for a PDF: one page at a time, which is what a PDF page IS.
 *
 * Each page is rendered when it becomes visible (`produceState` keyed on the
 * index), so a 400-page file costs one bitmap rather than 400 — the first pass
 * rendered EVERY page at open, which is why a big PDF took seconds to appear and
 * held a phone's memory while it did.
 */
@Composable
private fun PageReader(
    pageCount: Int,
    pagerState: PagerState,
    document: String,
    marks: List<ReaderMarkEntity>,
    palette: ReaderPalette,
    bookId: String,
    onOpenedAt: (ReaderMarkEntity?) -> Unit,
    onLongPress: (Int) -> Unit,
    onTap: () -> Unit,
    onScrolled: () -> Unit
) {
    val context = LocalContext.current

    // WHERE THEY STOPPED, read once per open — and the write below waits for it,
    // or the restore would race its own save and page 1 would overwrite the page
    // they were on.
    var restored by remember(bookId, document) { mutableStateOf(false) }
    LaunchedEffect(bookId, document, pageCount) {
        if (restored) return@LaunchedEffect
        val found = withContext(Dispatchers.IO) {
            runCatching { PersonalRepositoryHolder.repo.readerPosition(bookId, document) }.getOrNull()
        }
        onOpenedAt(found)
        if (found != null && found.positionIndex > 0) {
            pagerState.scrollToPage(found.positionIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))
        }
        restored = true
    }

    LaunchedEffect(bookId, document, pageCount, restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage }
            .collectLatest { index ->
                // Settled, not passed through: a swipe across ten pages writes
                // the page the finger stopped on, once.
                delay(700)
                withContext(Dispatchers.IO) {
                    runCatching {
                        PersonalRepositoryHolder.repo.saveReaderPosition(
                            bookId = bookId,
                            sourceKey = document,
                            index = index,
                            fraction = index.toFloat() / (pageCount - 1).coerceAtLeast(1)
                        )
                    }
                }
            }
    }

    // A page turned is the member moving through the book: the chrome goes.
    LaunchedEffect(pagerState, onScrolled) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) onScrolled() }
    }

    // PINCH ZOOMS THE PAGE ITSELF. A PDF page is not reflowable — zooming it has
    // to mean magnifying it, which is also what makes the small print of a
    // scanned document legible on a phone at all (v389).
    val zoomModifier = Modifier.pinchToZoom { zoom, pan ->
        val next = (ReaderLook.pdfZoom * zoom).coerceIn(1f, 4f)
        ReaderLook.pdfZoom = next
        if (next <= 1.02f) {
            ReaderLook.pdfPanX = 0f
            ReaderLook.pdfPanY = 0f
        } else {
            ReaderLook.pdfPanX += pan.x
            ReaderLook.pdfPanY += pan.y
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().then(zoomModifier),
        // A zoomed page is being INSPECTED, not turned: while the member is in
        // close, the horizontal drag belongs to the pan and the pager takes it
        // back the moment they are out again.
        userScrollEnabled = ReaderLook.pdfZoom <= 1.02f,
        pageSpacing = 8.dp
    ) { page ->
        val bitmap by produceState<Bitmap?>(null, document, page) {
            value = withContext(Dispatchers.IO) {
                runCatching { renderPdfPage(context, document, page) }.getOrNull()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(page) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress(page) }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val drawn = bitmap
            if (drawn != null) {
                // THE PAGE FILLS ITS SCREEN (user request: "make the page full
                // fit") and wears the reader's own ink — it used to be stretched
                // across the width and stayed white whatever the member chose
                // (user request: "for the pdf reader make the pdf background
                // change too").
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = ReaderLook.pdfZoom
                            scaleY = ReaderLook.pdfZoom
                            translationX = ReaderLook.pdfPanX
                            translationY = ReaderLook.pdfPanY
                        }
                ) {
                    Image(
                        bitmap = drawn.asImageBitmap(),
                        contentDescription = "Page ${page + 1}",
                        contentScale = ContentScale.Fit,
                        colorFilter = readerPdfFilter(palette.inkKey),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                CircularProgressIndicator(color = palette.accent)
            }
            val marksHere = marks.count { it.positionIndex == page && !it.isPosition }
            if (marksHere > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.surface,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        "$marksHere marked",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ink.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// One paragraph, and the marks it can wear
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReaderParagraphBlock(
    block: ReaderBlock,
    palette: ReaderPalette,
    highlightColor: Color,
    note: String,
    bookmarked: Boolean,
    onLongPress: () -> Unit,
    onTap: () -> Unit,
    query: String,
    hitHere: Boolean,
    hitLength: Int
) {
    val marked = highlightColor != Color.Transparent
    // The BOOK decides how loud a heading is (v389): its own <h1>, <h2> and <h3>
    // are three sizes rather than one, and every size follows the member's own
    // type size (ReaderLook.textScale).
    val scale = ReaderLook.textScale
    val level = when {
        block.headingLevel in 1..3 -> block.headingLevel
        block.isHeading -> 1
        else -> 0
    }
    val body = when (level) {
        1 -> TextStyle(
            fontFamily = FrauncesFontFamily,
            fontSize = (23f * scale).sp,
            lineHeight = (31f * scale).sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.ink
        )
        2 -> TextStyle(
            fontFamily = FrauncesFontFamily,
            fontSize = (20f * scale).sp,
            lineHeight = (27f * scale).sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.ink
        )
        3 -> TextStyle(
            fontFamily = LoraFontFamily,
            fontSize = (18f * scale).sp,
            lineHeight = (26f * scale).sp,
            fontWeight = FontWeight.Bold,
            color = palette.ink
        )
        else -> TextStyle(
            fontFamily = LoraFontFamily,
            fontSize = (17f * scale).sp,
            lineHeight = (29f * scale).sp,
            color = palette.ink
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(block.text) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .then(
                if (marked) {
                    Modifier
                        // A highlight is drawn BEHIND the words AND down their
                        // side: the wash says "this passage", the rule says
                        // where it starts — the same shape the journal's own
                        // quote panel wears, one system rather than two.
                        .drawBehind {
                            val bar = 3.dp.toPx()
                            drawRoundRect(
                                color = highlightColor.copy(alpha = 0.30f),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                            drawRoundRect(
                                color = highlightColor,
                                topLeft = Offset(0f, 0f),
                                size = Size(bar, size.height),
                                cornerRadius = CornerRadius(bar / 2f)
                            )
                        }
                        .padding(start = 11.dp, top = 4.dp, bottom = 4.dp)
                } else {
                    Modifier.padding(vertical = if (block.isHeading) 12.dp else 6.dp)
                }
            )
    ) {
        // A PICTURE FROM INSIDE THE BOOK, drawn in the place it actually stood.
        if (block.imagePath != null) {
            val picture by produceState<ImageBitmap?>(null, block.imagePath) {
                value = withContext(Dispatchers.IO) {
                    bookImageBitmap(block.imagePath.orEmpty())
                }
            }
            val drawn = picture
            if (drawn != null) {
                Image(
                    bitmap = drawn,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }
        // WHAT A FIND LOOKS LIKE: every occurrence in the block wears the wash,
        // not only the one the search jumped to — a search that lit a single
        // find would leave the member hunting for the rest.
        val shown = remember(block.text, query, hitHere) {
            val needle = query.trim()
            if (!hitHere || needle.isEmpty() || !block.text.contains(needle, ignoreCase = true)) {
                AnnotatedString(block.text)
            } else {
                buildAnnotatedString {
                    var from = 0
                    while (from <= block.text.length) {
                        val at = block.text.indexOf(needle, from, ignoreCase = true)
                        if (at < 0) {
                            append(block.text.substring(from))
                            break
                        }
                        append(block.text.substring(from, at))
                        withStyle(
                            SpanStyle(
                                background = palette.accent.copy(alpha = 0.32f),
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(block.text.substring(at, at + needle.length))
                        }
                        from = at + needle.length
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = shown,
                style = body,
                modifier = Modifier.weight(1f)
            )
            if (bookmarked) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp, top = 6.dp)
                        .size(7.dp)
                        .background(palette.accent, CircleShape)
                )
            }
        }
        if (note.isNotBlank()) {
            // The member's own words sit UNDER the passage they are about, in
            // their own hand — never mixed into the author's.
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, end = 6.dp)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Text(
                        "Your note",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        note,
                        style = TextStyle(
                            fontFamily = WritingFontFamily,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = palette.ink.copy(alpha = 0.9f)
                        )
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Chrome and sheets
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReaderChrome(
    visible: Boolean,
    title: String,
    palette: ReaderPalette,
    positionLabel: String,
    onClose: () -> Unit,
    onSearch: () -> Unit,
    onInk: () -> Unit,
    onMarks: () -> Unit,
    onChapters: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // The HEAD carries the way out and what is being read — nothing else. A
        // reader in the middle of a chapter does not need a row of controls over
        // the words; they are one tap away on the FOOT, where a thumb already is.
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.paper.copy(alpha = 0.94f))
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReaderChromeButton(CurioIcons.ChevronLeft, "Close the reader", palette) { onClose() }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.ink,
                        maxLines = 1
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.paper.copy(alpha = 0.94f))
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // WHERE THEY ARE, as a fact about the book rather than a
                // sentence of advice: a reader does not need to be told to hold
                // a passage, they need to know which chapter they are in.
                if (positionLabel.isNotBlank()) {
                    Text(
                        positionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ink.copy(alpha = 0.55f),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                ReaderChromeButton(CurioIcons.Search, "Search this book", palette) { onSearch() }
                ReaderChromeButton(CurioIcons.FormatText, "The page's ink", palette) { onInk() }
                ReaderChromeButton(CurioIcons.Bookmark, "Bookmarks and highlights", palette) { onMarks() }
                ReaderChromeButton(CurioIcons.MenuBook, "Chapters", palette) { onChapters() }
            }
        }
    }
}

@Composable
private fun ReaderChromeButton(
    glyph: String,
    label: String,
    palette: ReaderPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(38.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(glyph, label, tint = palette.ink.copy(alpha = 0.75f), size = 19.dp)
        }
    }
}

/** A sheet of the reader's own — a plain surface, because the reader is not a
 *  place for the app's chrome. */
@Composable
private fun ReaderSheetFrame(
    title: String,
    palette: ReaderPalette,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            color = palette.paper,
            modifier = Modifier
                .fillMaxWidth()
                // The sheet's own surface swallows taps, so tapping INSIDE it
                // does not dismiss it (which is what the scrim above is for).
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    title,
                    style = TextStyle(
                        fontFamily = FrauncesFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.ink
                    )
                )
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun ReaderSearchSheet(
    search: ReaderSearch,
    palette: ReaderPalette,
    onQuery: (String) -> Unit,
    onSubmit: () -> Unit,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ReaderSheetFrame("Search this book", palette, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = palette.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurioIcon(
                        CurioIcons.Search,
                        null,
                        tint = palette.ink.copy(alpha = 0.5f),
                        size = 17.dp
                    )
                    BasicTextField(
                        value = search.query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = LoraFontFamily,
                            fontSize = 16.sp,
                            color = palette.ink
                        ),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSubmit() }
                        ),
                        decorationBox = { inner ->
                            Box {
                                if (search.query.isEmpty()) {
                                    Text(
                                        "Words to find",
                                        style = TextStyle(
                                            fontFamily = LoraFontFamily,
                                            fontSize = 16.sp,
                                            color = palette.ink.copy(alpha = 0.35f)
                                        )
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                    )
                    if (search.query.isNotEmpty()) {
                        Surface(
                            onClick = onSubmit,
                            shape = RoundedCornerShape(50),
                            color = palette.accent
                        ) {
                            Text(
                                "Find",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // HOW FAR THE SWEEP HAS GOT. A PDF is read page by page (its words
            // live in a text layer that costs a parse apiece), so the count is
            // the honest thing to show rather than a spinner with no meaning.
            if (search.query.isNotBlank()) {
                Text(
                    when {
                        search.total == 0 -> "Reading the book\u2026"
                        search.done && search.hits.isEmpty() -> "Nothing found."
                        search.done -> "${search.hits.size} found"
                        else -> "${search.hits.size} found \u00b7 read ${search.scanned} of ${search.total}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.ink.copy(alpha = 0.55f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                search.hits.forEachIndexed { index, hit ->
                    val active = index == search.current
                    Surface(
                        onClick = { onPick(hit.index) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) palette.accent.copy(alpha = 0.22f) else palette.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                hit.snippet,
                                style = TextStyle(
                                    fontFamily = LoraFontFamily,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = palette.ink
                                ),
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (search.isPaged) "p ${hit.index + 1}" else "\u00a7 ${hit.index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderInkSheet(
    palette: ReaderPalette,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ReaderSheetFrame("The page", palette, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReaderSkin.entries.forEach { option ->
                    val active = option.key == ReaderLook.inkKey
                    val tone = readerPalette(option.key)
                    Surface(
                        onClick = { onPick(option.key) },
                        shape = RoundedCornerShape(12.dp),
                        color = tone.paper,
                        border = androidx.compose.foundation.BorderStroke(
                            if (active) 2.dp else 1.dp,
                            if (active) tone.accent else tone.ink.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = tone.ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderMarksSheet(
    marks: List<ReaderMarkEntity>,
    position: ReaderMarkEntity?,
    palette: ReaderPalette,
    onJump: (ReaderMarkEntity) -> Unit,
    onDelete: (ReaderMarkEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ReaderSheetFrame("Bookmarks & highlights", palette, onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (position != null && position.positionIndex > 0) {
                val through = (position.positionFraction * 100f).roundToInt()
                ReaderMarkRow(
                    label = "Last read \u00b7 auto",
                    body = if (through > 0) "$through% through" else "Section ${position.positionIndex + 1}",
                    palette = palette,
                    onJump = { onJump(position) },
                    onDelete = null
                )
            }
            if (marks.isEmpty()) {
                Text(
                    "Nothing marked yet \u2014 hold a passage while you read.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.55f)
                )
            }
            marks.forEach { mark ->
                ReaderMarkRow(
                    label = if (mark.isNote) "Note" else mark.markKind.label,
                    body = mark.text.ifBlank { "Section ${mark.positionIndex + 1}" },
                    palette = palette,
                    note = mark.note,
                    onJump = { onJump(mark) },
                    onDelete = { onDelete(mark) }
                )
            }
        }
    }
}

@Composable
private fun ReaderMarkRow(
    label: String,
    body: String,
    palette: ReaderPalette,
    onJump: () -> Unit,
    onDelete: (() -> Unit)?,
    note: String = ""
) {
    Surface(
        onClick = onJump,
        shape = RoundedCornerShape(12.dp),
        color = palette.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink,
                    maxLines = 2
                )
                if (note.isNotBlank()) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }
            if (onDelete != null) {
                Surface(
                    onClick = onDelete,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.Close,
                            "Remove this mark",
                            tint = palette.ink.copy(alpha = 0.45f),
                            size = 15.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderChaptersSheet(
    content: ReaderContent?,
    palette: ReaderPalette,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ReaderSheetFrame("Chapters", palette, onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (content) {
                is ReaderContent.Text -> {
                    val headings = content.blocks.filter { it.isHeading }
                    if (headings.isEmpty()) {
                        Text(
                            "This file has no chapter headings of its own.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink.copy(alpha = 0.6f)
                        )
                    }
                    headings.forEach { heading ->
                        Surface(
                            onClick = { onPick(heading.section) },
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                heading.text,
                                style = TextStyle(
                                    fontFamily = WritingFontFamily,
                                    fontSize = 15.sp,
                                    color = palette.ink
                                ),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                is ReaderContent.Pages -> {
                    // A PDF's own pages: the jumps a reader actually wants are
                    // the coarsest ones, so this is a page grid rather than a
                    // list of 400 rows.
                    val chunks = (0 until content.pageCount).chunked(4)
                    chunks.take(60).forEachIndexed { row, pages ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pages.forEach { page ->
                                Surface(
                                    onClick = { onPick(page) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.surface,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Page ${page + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.ink,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    )
                                }
                            }
                            if (pages.size < 4) {
                                Spacer(Modifier.weight((4 - pages.size).toFloat()))
                            }
                        }
                        if (row == 59) {
                            Text(
                                "More in the file itself",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.ink.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                null -> Text(
                    "Still opening the file\u2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ReaderMarkSheet(
    paragraph: ReaderParagraph,
    marks: List<ReaderMarkEntity>,
    palette: ReaderPalette,
    onHighlight: (ReaderHighlighter) -> Unit,
    /**
     * v389 — THE WHOLE CHAPTER IN ONE GO. Highlighting ran a paragraph at a
     * time, which is the wrong unit for the thing a member most often wants to
     * keep: the chapter. Offered on a HEADING (the block that names a chapter),
     * so the gesture is "mark this chapter" and nothing else has to say so.
     */
    onHighlightChapter: (() -> Unit)?,
    onNote: () -> Unit,
    onBookmark: () -> Unit,
    onRemove: (ReaderMarkEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val existing = marks.filter { it.positionIndex == paragraph.positionIndex && !it.isPosition }
    ReaderSheetFrame("Mark this passage", palette, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                paragraph.text.take(180),
                style = TextStyle(
                    fontFamily = LoraFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = palette.ink.copy(alpha = 0.75f)
                ),
                maxLines = 4
            )
            if (!paragraph.isPage) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (existing.any { it.isHighlight }) "Change the highlight" else "Highlight",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ink.copy(alpha = 0.55f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReaderHighlighter.entries.forEach { color ->
                            val active = existing.any { it.isHighlight && it.colorKey == color.key }
                            Surface(
                                onClick = { onHighlight(color) },
                                shape = CircleShape,
                                color = color.ink,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (active) 2.dp else 0.dp,
                                    if (active) palette.ink else Color.Transparent
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                if (active) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CurioIcon(CurioIcons.Check, null, tint = palette.ink, size = 16.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (onHighlightChapter != null) {
                ReaderSheetAction(
                    glyph = CurioIcons.FormatText,
                    label = "Highlight this whole chapter",
                    palette = palette,
                    onClick = onHighlightChapter
                )
            }
            ReaderSheetAction(
                glyph = CurioIcons.Note,
                label = if (existing.any { it.isNote }) "Edit your note" else "Write a note on this",
                palette = palette,
                onClick = onNote
            )
            ReaderSheetAction(
                glyph = CurioIcons.Bookmark,
                label = "Bookmark this place",
                palette = palette,
                onClick = onBookmark
            )
            existing.forEach { mark ->
                ReaderSheetAction(
                    glyph = CurioIcons.Close,
                    label = "Remove the ${mark.markKind.label.lowercase()}",
                    palette = palette,
                    onClick = { onRemove(mark) }
                )
            }
        }
    }
}

@Composable
private fun ReaderSheetAction(
    glyph: String,
    label: String,
    palette: ReaderPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = palette.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CurioIcon(glyph, null, tint = palette.accent, size = 18.dp)
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink
            )
        }
    }
}

/** The member's own words on a passage. Their hand, their page. */
@Composable
private fun ReaderNoteDialog(
    palette: ReaderPalette,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.paper,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .padding(horizontal = 20.dp)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "Your note",
                    style = TextStyle(
                        fontFamily = FrauncesFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.ink
                    )
                )
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        fontFamily = WritingFontFamily,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = palette.ink
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(palette.surface, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { onSave(text.trim()) },
                        shape = RoundedCornerShape(50),
                        color = palette.accent
                    ) {
                        Text(
                            "Keep it",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = palette.surface
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.ink,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// The reader's own palette
// ────────────────────────────────────────────────────────────────────────────

/** The four inks a page can be read on. */
private enum class ReaderSkin(val key: String, val label: String) {
    PAPER("paper", "Paper"),
    SEPIA("sepia", "Sepia"),
    NIGHT("night", "Night"),
    WHITE("white", "White");

    companion object {
        val inks: List<ReaderSkin> get() = entries
    }
}

/**
 * THE READER'S OWN LOOK, held for the PROCESS rather than per book: a member
 * chooses an ink for READING, not for one novel, so the choice follows them from
 * book to book (and every reader on screen follows it at once). Compose state, so
 * choosing one repaints immediately; a plain object rather than a stored
 * preference because it costs nothing to choose again.
 */
private object ReaderLook {
    var inkKey by mutableStateOf(ReaderSkin.PAPER.key)

    /**
     * v389 — HOW BIG THE WORDS ARE, for the PROCESS like the ink: a member who
     * needs larger type needs it in every book, and asking them again per novel
     * would be an errand. A PDF page is a picture, so its zoom is a real
     * scale-and-pan ([readerZoom]); a reflowable book's is this — its own type
     * size, which re-lays the page out instead of magnifying a photograph of it.
     */
    var textScale by mutableStateOf(1f)

    /** The PDF's own scale and pan, reset per page. */
    var pdfZoom by mutableStateOf(1f)
    var pdfPanX by mutableStateOf(0f)
    var pdfPanY by mutableStateOf(0f)
}

/** What the reader draws with, for one ink. */
private data class ReaderPalette(
    val paper: Color,
    val ink: Color,
    val accent: Color,
    val surface: Color,
    /**
     * v389 — WHICH INK THIS IS, kept as its own key so a PDF PAGE can wear it.
     *
     * The words of a reflowable book are drawn in [ink] on [paper] and follow
     * the skin by themselves; a PDF page is a BITMAP, so the only way it can
     * follow the same choice is a colour filter — which needs to know WHICH
     * choice it is, not just what colour the surrounding page happens to be
     * (user request: "for the pdf reader make the pdf background change too").
     */
    val inkKey: String
)

/**
 * THE PAGE'S INK, APPLIED TO A PDF PAGE.
 *
 * A rendered PDF page arrives as white paper with black type whatever the
 * member has chosen, which is why changing the reader's ink used to change
 * everything EXCEPT the one thing they were looking at. A colour matrix is the
 * honest way to fix that on a bitmap: white and black are scaled on the way
 * through, so the page keeps its own contrast and only its temperature changes
 * (and `night` inverts it, which is what reading a PDF in the dark actually
 * requires). `null` means "leave the page exactly as it was drawn", which is
 * what the White skin asks for.
 */
private fun readerPdfFilter(key: String): ColorFilter? = when (key) {
    ReaderSkin.WHITE.key -> null
    ReaderSkin.SEPIA.key -> pdfTint(0.95f, 0.87f, 0.74f, 0f, 0f, 0f)
    ReaderSkin.NIGHT.key -> pdfTint(-0.86f, -0.86f, -0.86f, 236f, 231f, 214f)
    else -> pdfTint(0.97f, 0.94f, 0.88f, 0f, 0f, 0f)
}

/** A scale-and-offset matrix: what turns white paper into the reader's ink. */
private fun pdfTint(
    r: Float,
    g: Float,
    b: Float,
    rOffset: Float,
    gOffset: Float,
    bOffset: Float
): ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            r, 0f, 0f, 0f, rOffset,
            0f, g, 0f, 0f, gOffset,
            0f, 0f, b, 0f, bOffset,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

/**
 * v389 — TWO FINGERS ZOOM, ONE FINGER IS THE MEMBER'S.
 *
 * A plain `transformable` swallows the drag that was meant to scroll the page or
 * turn it, which on a reader is the gesture that matters most. This waits until
 * a SECOND finger is down before it claims anything, so pinch is free and the
 * single-finger scroll and swipe are untouched.
 */
private fun Modifier.pinchToZoom(onZoom: (zoom: Float, pan: Offset) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } >= 2) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    if (zoom != 1f || pan != Offset.Zero) {
                        onZoom(zoom, pan)
                        event.changes.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }

// @Composable because the default ink asks [isCurioDarkTheme] what the app is
// wearing — one reader, two themes.
@Composable
private fun readerPalette(key: String): ReaderPalette = when (key) {
    "sepia" -> ReaderPalette(
        paper = Color(0xFFF3E7D3),
        ink = Color(0xFF4A3A28),
        accent = Color(0xFF9A6A43),
        surface = Color(0x1A4A3A28),
        inkKey = "sepia"
    )
    "night" -> ReaderPalette(
        paper = Color(0xFF12100E),
        ink = Color(0xFFD8CFC2),
        accent = Color(0xFFC09263),
        surface = Color(0x1FD8CFC2),
        inkKey = "night"
    )
    "white" -> ReaderPalette(
        paper = Color(0xFFFFFFFF),
        ink = Color(0xFF1B1B1B),
        accent = Color(0xFF8A5A33),
        surface = Color(0x14000000),
        inkKey = "white"
    )
    else -> if (isCurioDarkTheme()) {
        ReaderPalette(
            paper = Color(0xFF1A1714),
            ink = Color(0xFFE2D9CC),
            accent = Color(0xFFC09263),
            surface = Color(0x1FE2D9CC),
            inkKey = "paper"
        )
    } else {
        ReaderPalette(
            paper = Color(0xFFFBF6EC),
            ink = Color(0xFF2E2620),
            accent = Color(0xFF8A5A33),
            surface = Color(0x14000000),
            inkKey = "paper"
        )
    }
}

/** The four highlighters. Deeper than a theme accent on purpose — a highlight
 *  is a MARK on the page, not the app's colour showing through. */
private enum class ReaderHighlighter(val key: String, val ink: Color) {
    AMBER("amber", Color(0xFFE0A33C)),
    ROSE("rose", Color(0xFFD98A8A)),
    SAGE("sage", Color(0xFF8FB08A)),
    SKY("sky", Color(0xFF7FA8C9));

    companion object {
        fun of(key: String?): ReaderHighlighter =
            entries.firstOrNull { it.key == key } ?: AMBER
    }
}

private fun readerHighlighter(key: String?): ReaderHighlighter = ReaderHighlighter.of(key)

// ────────────────────────────────────────────────────────────────────────────
// The content model and the parsers
// ────────────────────────────────────────────────────────────────────────────

/** What a reader is holding: reflowable text, or pages. */
private sealed interface ReaderContent {
    data class Text(val blocks: List<ReaderBlock>) : ReaderContent

    data class Pages(val pageCount: Int) : ReaderContent
}

/** ONE paragraph (or heading, or picture) of a reflowable book. */
private data class ReaderBlock(
    val text: String,
    /** 1-based chapter/section this paragraph belongs to. */
    val section: Int,
    val sectionTitle: String,
    val isHeading: Boolean,
    /**
     * v389 — WHICH HEADING THIS IS: 1, 2 or 3 for an `<h1>`-`<h3>` of the
     * book's own markup, 0 for a paragraph. The file said how loud the line is
     * meant to be, so the reader no longer flattens every heading into one size
     * (user request: "EPUB images/styled headings").
     */
    val headingLevel: Int = 0,
    /**
     * v389 — A PICTURE FROM INSIDE THE BOOK, copied out of the archive to a
     * cache file when the book was opened and drawn here in the flow, between
     * the paragraphs it actually sat between.
     */
    val imagePath: String? = null
)

/** What a long press was aimed at. */
private data class ReaderParagraph(
    val positionIndex: Int,
    val section: Int,
    val text: String,
    val isHeading: Boolean,
    /**
     * True when this is a PDF PAGE rather than a passage of reflowable text.
     * A page can be bookmarked and noted, but it cannot be HIGHLIGHTED: the
     * page is a picture, and there is no passage to mark until the reader can
     * draw on it (see the marks sheet — it hides the highlight inks for a page).
     */
    val isPage: Boolean = false
)

private enum class ReaderSheet { INK, MARKS, CHAPTERS, SEARCH }

/**
 * v389 — A SEARCH RUNNING OVER THE BOOK.
 *
 * Held as an object rather than a handful of states because a search is ONE
 * thing with a beginning and a middle: what was asked, how far the sweep has
 * got, what it has found, and which find the member is standing on. A PDF has to
 * be SWEPT page by page (its words live in a text layer that costs a parse per
 * page), so the sweep is honest about itself — [scanned] of [total] — and the
 * hits appear as they are found instead of after a silent wait.
 */
private class ReaderSearch {
    /** What is in the sheet's field right now. */
    var query by mutableStateOf("")
    /** Bumped when the member commits the field — what actually starts a sweep. */
    var token by mutableIntStateOf(0)
    var scanned by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var hits by mutableStateOf<List<ReaderSearchHit>>(emptyList())
    var current by mutableIntStateOf(-1)
    /** True when the book is a PDF, so a find is named by its PAGE. */
    var isPaged by mutableStateOf(false)

    val done: Boolean get() = total > 0 && scanned >= total
}

/** A little of what surrounds a find — enough to recognise it by. */
private fun String.aroundSnippet(at: Int, length: Int): String {
    val from = (at - 40).coerceAtLeast(0)
    val to = (at + length + 40).coerceAtMost(length.coerceAtLeast(this.length))
    val head = if (from > 0) "…" else ""
    val tail = if (to < this.length) "…" else ""
    return head + substring(from, to.coerceAtLeast(from)).trim() + tail
}

private data class ReaderSearchHit(
    /** The block index of a reflowable book, or the page of a PDF. */
    val index: Int,
    val snippet: String
)



private suspend fun saveReaderMark(
    bookId: String,
    document: String,
    paragraph: ReaderParagraph,
    kind: ReaderMarkKind,
    text: String,
    colorKey: String = "",
    note: String = ""
) {
    withContext(Dispatchers.IO) {
        val existing = runCatching {
            PersonalRepositoryHolder.repo
                .readerMarks(bookId, document)
                .firstOrNull { it.positionIndex == paragraph.positionIndex && it.markKind == kind }
        }.getOrNull()
        runCatching {
            PersonalRepositoryHolder.repo.saveReaderMark(
                (existing ?: ReaderMarkEntity(
                    id = newReaderMarkId(),
                    bookId = bookId,
                    sourceKey = document,
                    positionIndex = paragraph.positionIndex,
                    kind = kind.key
                )).copy(
                    text = text,
                    note = note,
                    colorKey = colorKey,
                    chapter = paragraph.section
                )
            )
        }
    }
}

/** A plain filesystem path (the app's own copy), or null for a `content://`. */
private fun localFile(value: String): File? {
    if (value.startsWith("content://")) return null
    return File(value.removePrefix("file://"))
}

private fun readBook(context: android.content.Context, value: String): ReaderContent {
    val file = localFile(value)
    val name = (file?.name ?: value).lowercase()
    val type = if (file == null) {
        context.contentResolver.getType(android.net.Uri.parse(value)).orEmpty()
    } else {
        ""
    }
    return when {
        type == "application/pdf" || name.endsWith(".pdf") -> {
            // Only the COUNT is read up front now: the pages render as they are
            // reached (see PageReader), so opening a 400-page book costs one
            // descriptor, not four hundred bitmaps.
            ReaderContent.Pages(pageCount = pdfPageCount(context, value, file))
        }
        type.startsWith("text/") || name.endsWith(".txt") ->
            ReaderContent.Text(readPlainText(context, value, file))
        else -> ReaderContent.Text(readEpubText(context, value, file))
    }
}

private fun pdfPageCount(context: android.content.Context, value: String, file: File?): Int {
    val descriptor = if (file != null) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    } else {
        context.contentResolver.openFileDescriptor(android.net.Uri.parse(value), "r")
            ?: error("No PDF")
    }
    val renderer = PdfRenderer(descriptor)
    return try {
        renderer.pageCount
    } finally {
        renderer.close()
        descriptor.close()
    }
}

/** Renders ONE page of a PDF at screen resolution. */
private fun renderPdfPage(
    context: android.content.Context,
    value: String,
    index: Int
): Bitmap {
    val file = localFile(value)
    val descriptor = if (file != null) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    } else {
        context.contentResolver.openFileDescriptor(android.net.Uri.parse(value), "r")
            ?: error("No PDF")
    }
    return try {
        val renderer = PdfRenderer(descriptor)
        val page = renderer.openPage(index)
        // A page drawn at 1.5x reads cleanly on a phone without holding a
        // poster-sized bitmap per page.
        val scale = 1.5f
        val bitmap = Bitmap.createBitmap(
            (page.width * scale).toInt().coerceAtLeast(1),
            (page.height * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        bitmap
    } finally {
        descriptor.close()
    }
}

/**
 * A PLAIN TEXT FILE as paragraphs: a blank line ends one, which is the only
 * structural rule plain text actually has.
 */
private fun readPlainText(
    context: android.content.Context,
    value: String,
    file: File?
): List<ReaderBlock> {
    val text = if (file != null) {
        file.readText()
    } else {
        context.contentResolver.openInputStream(android.net.Uri.parse(value))
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
    return text.split(Regex("\\n{2,}"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapIndexed { index, paragraph ->
            ReaderBlock(paragraph, 1, "", isHeading = index == 0 && paragraph.length < 80)
        }
}

/**
 * AN EPUB as a CONTINUOUS column.
 *
 * The XHTML files are read in the archive's own order and each one becomes a
 * SECTION: its heading (an `<h1>`/`<h2>` if it has one, else the file's own
 * name) is a block of its own, and its paragraphs follow. That is what makes
 * continuous scroll possible — a chapter is a run of paragraphs, not a page —
 * and it is also what the chapter sheet lists.
 */
private fun readEpubText(
    context: android.content.Context,
    value: String,
    file: File?
): List<ReaderBlock> {
    val temp = if (file == null) {
        File.createTempFile("curio-reader", ".epub", context.cacheDir).also { target ->
            context.contentResolver.openInputStream(android.net.Uri.parse(value)).use { input ->
                target.outputStream().use { output -> input?.copyTo(output) }
            }
        }
    } else {
        null
    }
    val source = file ?: temp ?: error("No file")
    return try {
        val blocks = ArrayList<ReaderBlock>()
        ZipFile(source).use { zip ->
            val pages = zip.entries().asSequence()
                .filter { entry ->
                    !entry.isDirectory &&
                        (entry.name.endsWith(".xhtml", true) || entry.name.endsWith(".html", true) ||
                            entry.name.endsWith(".htm", true))
                }
                .toList()
            pages.forEachIndexed { sectionIndex, entry ->
                val raw = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                val section = sectionIndex + 1
                // The FIRST heading still names the section (the chapter sheet's
                // entry). It is no longer added as a block of its own: every
                // heading is a block now, at its own level, so adding one here
                // too would print it twice.
                val heading = Regex(
                    "<h[1-2][^>]*>(.*?)</h[1-2]>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                ).find(raw)?.groupValues?.getOrNull(1)
                    ?.let { stripMarkup(it) }
                    ?.takeIf { it.isNotBlank() }
                    ?: entry.name.substringAfterLast('/').substringBeforeLast('.')
                        .replace('-', ' ')
                        .replace('_', ' ')
                        .trim()
                        .takeIf { it.isNotBlank() && it.length < 60 }
                        .orEmpty()
                val found = epubBlocks(context, zip, entry.name, raw, section, heading)
                if (found.none { it.isHeading } && heading.isNotBlank()) {
                    // A file with no heading of its own gets the one it is named
                    // by, so the chapter sheet can still list it.
                    blocks.add(ReaderBlock(heading, section, heading, isHeading = true, headingLevel = 1))
                }
                blocks.addAll(found)
            }
        }
        blocks
    } finally {
        temp?.delete()
    }
}

/**
 * v389 — ONE XHTML DOCUMENT, IN THE ORDER IT READS.
 *
 * Headings and pictures are turned into MARKERS *before* the markup is taken
 * off, so the document's own order survives in one pass: a heading keeps its
 * level (`<h1>` is not `<h3>`), a picture lands exactly between the paragraphs
 * it sat between, and neither has to be found by a second walk that could not
 * have known where it belonged.
 */
private fun epubBlocks(
    context: android.content.Context,
    zip: ZipFile,
    entryName: String,
    raw: String,
    section: Int,
    sectionTitle: String
): List<ReaderBlock> {
    val marked = raw
        .replace(Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<img[^>]*?src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)) { match ->
            "\n\n\u0000I:${match.groupValues.getOrNull(1).orEmpty()}\u0000\n\n"
        }
        .replace(
            Regex("<h([1-6])[^>]*>(.*?)</h\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        ) { match ->
            val level = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 2
            "\n\n\u0000H$level:${match.groupValues.getOrNull(2).orEmpty()}\u0000\n\n"
        }
        .replace(Regex("</(p|div|li|blockquote|h[1-6])>", RegexOption.IGNORE_CASE), "\n\n")
    val out = ArrayList<ReaderBlock>()
    stripMarkup(marked)
        .split(Regex("\\n{2,}"))
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotBlank() }
        .forEach { chunk ->
            when {
                chunk.startsWith("\u0000H") -> {
                    val body = chunk.removePrefix("\u0000").removeSuffix("\u0000")
                    val level = body.substringAfter('H').substringBefore(':').toIntOrNull() ?: 2
                    val text = body.substringAfter(':').trim()
                    if (text.isNotBlank()) {
                        out.add(
                            ReaderBlock(
                                text = text,
                                section = section,
                                sectionTitle = sectionTitle,
                                isHeading = true,
                                headingLevel = level.coerceIn(1, 3)
                            )
                        )
                    }
                }

                chunk.startsWith("\u0000I:") -> {
                    val src = chunk.removePrefix("\u0000I:").removeSuffix("\u0000").trim()
                    val path = readEpubImage(context, zip, entryName, src)
                    if (path != null) {
                        out.add(
                            ReaderBlock(
                                text = "",
                                section = section,
                                sectionTitle = sectionTitle,
                                isHeading = false,
                                imagePath = path
                            )
                        )
                    }
                }

                else -> out.add(
                    ReaderBlock(
                        text = chunk,
                        section = section,
                        sectionTitle = sectionTitle,
                        isHeading = false
                    )
                )
            }
        }
    return out
}

/**
 * A PICTURE FROM INSIDE THE BOOK, copied out of the archive once.
 *
 * A zip entry is a stream, and a page being scrolled cannot hold one open — so
 * the bytes are written to the app's own cache the first time the book is
 * opened and the block remembers the PATH. What cannot be decoded is dropped
 * outright: half a page of broken pictures is worse than a page with none.
 */
private fun readEpubImage(
    context: android.content.Context,
    zip: ZipFile,
    entryName: String,
    src: String
): String? {
    if (src.isBlank() || src.startsWith("data:")) return null
    val decoded = android.net.Uri.decode(src).substringBefore('#').substringBefore('?').trim()
    if (decoded.startsWith("http://") || decoded.startsWith("https://")) return null
    val base = entryName.substringBeforeLast('/', "")
    val resolved = normaliseZipPath(
        if (decoded.startsWith("/")) decoded.drop(1) else "$base/$decoded"
    )
    val entry = zip.getEntry(resolved)
        ?: zip.getEntry(decoded.dropWhile { it == '/' })
        ?: return null
    if (entry.isDirectory) return null
    val target = File(imageDir(context), resolved.replace('/', '_').takeLast(72))
    if (target.exists() && target.length() > 0L) return target.absolutePath
    return runCatching {
        zip.getInputStream(entry).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(target.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            target.delete()
            null
        } else {
            target.absolutePath
        }
    }.getOrNull()
}

/** `a/../b/c.png` -> `b/c.png`, so a relative `src` lands on a real entry. */
private fun normaliseZipPath(path: String): String {
    val parts = ArrayList<String>()
    path.split('/').forEach { part ->
        when (part) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
            else -> parts.add(part)
        }
    }
    return parts.joinToString("/")
}

private fun imageDir(context: android.content.Context): File =
    File(context.cacheDir, "book-images").apply { mkdirs() }

/**
 * A book's picture at reading size. Decoded with a sample size rather than at
 * full resolution: an illustrated page can carry a 3000px scan, and a phone
 * has no business holding four of them to draw 600 of those pixels.
 */
private fun bookImageBitmap(path: String): ImageBitmap? = runCatching {
    val probe = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, probe)
    if (probe.outWidth <= 0) return@runCatching null
    var sample = 1
    while (probe.outWidth / sample > 1400) sample *= 2
    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
    android.graphics.BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}.getOrNull()

private fun stripMarkup(html: String): String = html
    .replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .trim()

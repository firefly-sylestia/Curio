package com.curio.app.features.personal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
/** The Activity a View is drawn in, however wrapped its context is. */
private fun Context.findActivity(): Activity? {
    var candidate: Context? = this
    while (candidate is ContextWrapper) {
        if (candidate is Activity) return candidate
        candidate = candidate.baseContext
    }
    return null
}

@Composable
fun BookReaderScreen(navController: NavController, bookId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // THE READER TAKES THE WHOLE SCREEN (v389). The system's own status bar sat
    // over every page of every book, competing with the words for the top of the
    // screen (user request: "hide the system status bar" in the reader). It is
    // hidden for as long as the reader is on screen and comes back on the way
    // out; and because it is hidden, the chrome's own `statusBarsPadding`
    // collapses with it, which is exactly right — nothing is drawn under
    // anything, so there is nothing to pad for.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.statusBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }
    }
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

    // ── HOW THIS BOOK FLOWS ──────────────────────────────────────────────
    // The PAGED text flow lays the book out itself, so the page count is the
    // pager's to report; the bar that shows it is built further down, beside
    // the PDF pager it also reads (see `pageBar` — it cannot be built here,
    // because at this point in the body the pager it names does not exist yet).
    var textPageCount by remember { mutableIntStateOf(0) }
    val textPager = rememberPagerState { textPageCount }
    val flowLabel = when (content) {
        is ReaderContent.Pages -> ReaderLook.pageFlow
        else -> ReaderLook.textFlow
    }

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

    // ── THE PAGE BAR ─────────────────────────────────────────────────────
    // The floating bar that turns a page, and the only place the reader says
    // WHICH page it is in terms the member can act on. It belongs to the CHROME,
    // which hides itself: a page bar that lived inside the pager could not go
    // away with the tools, and a reader whose chrome is gone should have nothing
    // over the words at all (user request: "a floating page chnaging bar in the
    // tool bar which again hides with the tool barm").
    //
    // Two pagers, one bar: the PDF's own, and the text pager the PAGED flow
    // lays a reflowable book out into. A book that prints its own page numbers
    // is the exception — Curio does not number it a second time.
    val pageBar: ReaderPageBar? = when (val loaded = content) {
        is ReaderContent.Pages -> if (ReaderLook.pageFlow == ReaderFlow.PAGED) {
            ReaderPageBar(
                label = "Page ${pagerState.currentPage + 1} of ${loaded.pageCount}",
                onPrev = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage - 1).coerceAtLeast(0)
                        )
                    }
                },
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(loaded.pageCount - 1)
                        )
                    }
                }
            )
        } else {
            null
        }

        is ReaderContent.Text -> if (
            ReaderLook.textFlow == ReaderFlow.PAGED && !loaded.ownPages && textPageCount > 0
        ) {
            ReaderPageBar(
                label = "Page ${textPager.currentPage + 1} of $textPageCount",
                onPrev = {
                    scope.launch {
                        textPager.animateScrollToPage((textPager.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onNext = {
                    scope.launch {
                        textPager.animateScrollToPage(
                            (textPager.currentPage + 1).coerceAtMost(textPageCount - 1)
                        )
                    }
                }
            )
        } else {
            null
        }

        null -> null
    }

    // WHERE THEY ARE, said as a fact about the book — the chapter they are in,
    // how many marks they have left, the page of the file. It reads the LIVE
    // position (the list's first block, the pager's current page), so it follows
    // the reading instead of naming the place the book happened to open at.
    val positionLabel = when (val loaded = content) {
        // The PAGE BAR is where a PDF says its page, and in the paged flow the
        // bar is right there in the same chrome — so the foot says the thing the
        // bar cannot: how much of the book is marked. (The two used to say "Page
        // 7 of 300" at once, which reads as the same number told twice.) In the
        // SCROLL flow there is no bar, and the foot still does not state a page:
        // the pager is not the thing being scrolled, so any number here would be
        // a stale one — a wrong page is worse than none.
        is ReaderContent.Pages -> {
            val marked = marks.count { !it.isPosition }
            if (marked > 0) "$marked marked" else ""
        }

        is ReaderContent.Text -> {
            val at = loaded.blocks.getOrNull(listState.firstVisibleItemIndex)
            val place = at?.sectionTitle.orEmpty().ifBlank {
                // A book that prints its own page numbers says where the member
                // is by itself, so Curio does not number it a second time — no
                // "Section 12" against the book's own "12" (see
                // carriesOwnPageMarkers).
                if (loaded.ownPages) "" else at?.let { "Section ${it.section}" }.orEmpty()
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

    // ── THE BOOK'S OWN CONTENTS (v389) ─────────────────────────────────
    //
    // The chapters sheet lists what the FILE says, not what the reader could
    // guess for itself: an EPUB's nav document or NCX, a PDF's own outline, and
    // the reader's own headings only when the file has neither. A PDF's outline
    // costs a parse, so it is read when the sheet is OPENED and not a moment
    // before — a book must never open slower for a list nobody has asked for
    // (user request: "instead of the chapters tab where in chapters i see the
    // pages it should show the chapters and all detected from the epub or pdf,
    // im sure boo pdf hav that table of contnt etc").
    var pdfChapters by remember(document) { mutableStateOf<List<ReaderOutlineEntry>?>(null) }
    LaunchedEffect(sheet, document) {
        if (sheet != ReaderSheet.CHAPTERS) return@LaunchedEffect
        if (content !is ReaderContent.Pages) return@LaunchedEffect
        if (pdfChapters != null || document.isBlank()) return@LaunchedEffect
        pdfChapters = withContext(Dispatchers.IO) {
            runCatching { pdfOutline(context, document) }.getOrNull()
        }
    }
    val chapters: List<ReaderOutlineEntry> = when (val loaded = content) {
        is ReaderContent.Text -> if (loaded.outline.isNotEmpty()) {
            loaded.outline.map { entry ->
                val section = loaded.sectionSources.indexOf(entry.target)
                entry.copy(
                    block = if (section >= 0) {
                        loaded.blocks.indexOfFirst { it.section == section + 1 }
                    } else {
                        -1
                    }
                )
            }
        } else {
            loaded.blocks.withIndex()
                .filter { it.value.isHeading }
                .map { (index, block) ->
                    ReaderOutlineEntry(
                        title = block.text,
                        block = index,
                        depth = block.headingLevel.coerceIn(1, 3)
                    )
                }
        }

        is ReaderContent.Pages -> pdfChapters.orEmpty()
        null -> emptyList()
    }

    // A JUMP ASKED FOR FROM OUTSIDE the reading surface — the chapters sheet, a
    // search find. It is handed to whichever surface is showing (the scroll list
    // or the pager) because only that one knows where the block landed, and
    // cleared by it so the same jump never fires twice.
    var pendingBlock by remember { mutableStateOf<Int?>(null) }
    fun jumpToBlock(index: Int) {
        pendingBlock = index
        chrome = false
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
                    hitLength = searching?.query?.length ?: 0,
            pagerState = textPager,
            onPageCount = { count -> textPageCount = count },
            ownPages = loaded.ownPages,
            pendingBlock = pendingBlock,
            onPendingConsumed = { pendingBlock = null }
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
                    onScrolled = { hideChrome() },
                    flow = ReaderLook.pageFlow
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
            pageBar = pageBar,
            flowLabel = flowLabel.label,
            onToggleFlow = {
                when (content) {
                    is ReaderContent.Pages -> ReaderLook.pageFlow = ReaderLook.pageFlow.flipped()
                    is ReaderContent.Text -> ReaderLook.textFlow = ReaderLook.textFlow.flipped()
                    null -> Unit
                }
            },
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
            chapters = chapters,
            palette = palette,
            onPickBlock = { block ->
                sheet = null
                if (block >= 0) jumpToBlock(block)
            },
            onPickPage = { page ->
                sheet = null
                chrome = false
                scope.launch {
                    pagerState.scrollToPage(
                        (page - 1).coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
                    )
                }
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
                when (content) {
                    // A block index for a reflowed book: the surface showing it
                    // decides which page that block is on.
                    is ReaderContent.Text -> jumpToBlock(index)
                    is ReaderContent.Pages -> scope.launch {
                        pagerState.scrollToPage(
                            index.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
                        )
                    }

                    null -> Unit
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
    hitLength: Int,
    /** Hoisted to the SCREEN, because the page bar lives in the chrome. */
    pagerState: PagerState,
    onPageCount: (Int) -> Unit,
    ownPages: Boolean,
    /** A block to land on, asked for from outside (a chapter, a search find). */
    pendingBlock: Int?,
    onPendingConsumed: () -> Unit
) {
    val state = listState

    // THE JUMP, on the scrolling side: the pager answers it itself (it is the
    // only one that knows which page a block landed on).
    LaunchedEffect(pendingBlock) {
        val target = pendingBlock ?: return@LaunchedEffect
        if (ReaderLook.textFlow != ReaderFlow.SCROLL) return@LaunchedEffect
        state.scrollToItem(target.coerceIn(0, (content.blocks.size - 1).coerceAtLeast(0)))
        onPendingConsumed()
    }

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
    var restoredBlock by remember(bookId, document) { mutableIntStateOf(0) }
    LaunchedEffect(bookId, document, content.blocks.size) {
        if (restored) return@LaunchedEffect
        val stored = withContext(Dispatchers.IO) {
            runCatching { PersonalRepositoryHolder.repo.readerPosition(bookId, document) }.getOrNull()
        }
        onOpenedAt(stored)
        if (stored != null && stored.positionIndex > 0) {
            restoredBlock = stored.positionIndex
                .coerceIn(0, (content.blocks.size - 1).coerceAtLeast(0))
            // Only the SCROLL list is moved here. In PAGED flow the pager cannot
            // be told about a BLOCK until it knows which page that block landed
            // on, which is its own business (see TextPagedReader).
            if (ReaderLook.textFlow == ReaderFlow.SCROLL) {
                state.scrollToItem(restoredBlock)
            }
        }
        restored = true
    }

    LaunchedEffect(bookId, document, content.blocks.size, restored, ReaderLook.textFlow) {
        if (!restored) return@LaunchedEffect
        // In PAGED flow the pager owns where the member is (see TextPagedReader),
        // and this would write block 0 over their place.
        if (ReaderLook.textFlow == ReaderFlow.PAGED) return@LaunchedEffect
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

    // ── PAGES, for a book whose own pages do not exist ───────────────────
    // A reflowed book has no pages of its own, so "Pages" means the reader makes
    // them: the words are measured at the member's own type size and broken into
    // screenfuls, which is what every ebook reader does and what makes a page
    // turn mean the same thing in a novel as in a PDF (user request: "same ofor
    // epub page like epub option too").
    if (ReaderLook.textFlow == ReaderFlow.PAGED) {
        TextPagedReader(
            content = content,
            pagerState = pagerState,
            onPageCount = onPageCount,
            restoredBlock = restoredBlock,
            marks = marks,
            palette = palette,
            bookId = bookId,
            document = document,
            onLongPress = onLongPress,
            onTap = onTap,
            query = query,
            hitIndex = hitIndex,
            pendingBlock = pendingBlock,
            onPendingConsumed = onPendingConsumed
        )
        return
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
 * A PDF AS ONE LONG COLUMN OF PAGES (v389).
 *
 * The other half of the flow choice, and the one a reference book or a scanned
 * document wants: the pages run on under each other instead of being turned
 * (user request: "add like continuos veritical pdf style too"). Each page is
 * still rendered ON DEMAND — a `LazyColumn` asks for the pages near the screen
 * and nothing else — so a 400-page file costs the same handful of bitmaps it
 * costs in the pager. Its scroll carries its own auto-bookmark, so switching
 * between the two flows never loses the member's place.
 */
@Composable
private fun PdfScrollReader(
    pageCount: Int,
    document: String,
    marks: List<ReaderMarkEntity>,
    palette: ReaderPalette,
    bookId: String,
    onOpenedAt: (ReaderMarkEntity?) -> Unit,
    onTap: () -> Unit,
    onScrolled: () -> Unit,
    onLongPress: (Int) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var restored by remember(bookId, document) { mutableStateOf(false) }
    LaunchedEffect(bookId, document, pageCount) {
        if (restored) return@LaunchedEffect
        val found = withContext(Dispatchers.IO) {
            runCatching { PersonalRepositoryHolder.repo.readerPosition(bookId, document) }.getOrNull()
        }
        onOpenedAt(found)
        if (found != null && found.positionIndex > 0) {
            listState.scrollToItem(found.positionIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))
        }
        restored = true
    }

    LaunchedEffect(listState, onScrolled) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) onScrolled() }
    }

    LaunchedEffect(bookId, document, pageCount, restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { index ->
                delay(800)
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

    // ── ONE PAGE PER SCREEN, IN A COLUMN (v389c) ──────────────────────
    //
    // The column used to size every page by its WIDTH (`ContentScale.FillWidth`),
    // so on a modern phone a page was taller than the screen and the next one's
    // top was showing under it — which is exactly what a member reported seeing
    // ("for pdf it was showing double pages view": two page images stacked). A
    // scrolling reader should still read ONE page at a time, so each page now
    // takes the whole viewport and fits INSIDE it: the column scrolls, the page
    // is a page, and the only place two pages meet is the 10dp gap between them.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pageHeight = maxHeight
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        items(count = pageCount, key = { page -> "pdf-page-$page" }) { page ->
            val bitmap by produceState<Bitmap?>(null, document, page) {
                value = withContext(Dispatchers.IO) {
                    runCatching { renderPdfPage(context, document, page) }.getOrNull()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pageHeight)
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
                    Image(
                        bitmap = drawn.asImageBitmap(),
                        contentDescription = "Page ${page + 1}",
                        // FIT, not FillWidth: the page is fitted into the one
                        // screen it is given, so no second page can be showing
                        // under it.
                        contentScale = ContentScale.Fit,
                        colorFilter = readerPdfFilter(palette.inkKey),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = palette.accent)
                    }
                }
                val marksHere = marks.count { it.positionIndex == page && !it.isPosition }
                if (marksHere > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.surface,
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
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
    }
}

/**
 * v389 — PAGES FOR A BOOK THAT HAS NONE OF ITS OWN.
 *
 * The words are MEASURED at the member's own type size and broken into
 * screenfuls, so a page turn in a novel means what a page turn in a PDF means
 * and a pinch re-lays the book out instead of magnifying a photograph of it.
 * Nothing is cached: a page is a RANGE of blocks, and the ranges are worked out
 * from the room the screen actually has — which is also what makes the mode
 * survive a rotation or a keyboard.
 */
@Composable
private fun TextPagedReader(
    content: ReaderContent.Text,
    pagerState: PagerState,
    onPageCount: (Int) -> Unit,
    restoredBlock: Int,
    marks: List<ReaderMarkEntity>,
    palette: ReaderPalette,
    bookId: String,
    document: String,
    onLongPress: (ReaderParagraph) -> Unit,
    onTap: () -> Unit,
    query: String,
    hitIndex: Int,
    pendingBlock: Int?,
    onPendingConsumed: () -> Unit
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var room by remember { mutableStateOf(IntSize.Zero) }
    val pages = remember(content.blocks, room, ReaderLook.textScale) {
        paginateBlocks(content.blocks, room, ReaderLook.textScale, measurer, density)
    }

    LaunchedEffect(pages.size) { onPageCount(pages.size) }

    // Where the member was, once the pages exist to be counted: the page whose
    // range covers the block they stopped on.
    var placed by remember(content.blocks.size) { mutableStateOf(false) }
    LaunchedEffect(pages.size, restoredBlock) {
        if (placed || pages.isEmpty()) return@LaunchedEffect
        val target = pages.indexOfFirst { restoredBlock in it }
        if (target > 0) pagerState.scrollToPage(target)
        placed = true
    }

    // A block asked for from outside: the page whose range covers it.
    LaunchedEffect(pendingBlock, pages.size) {
        val target = pendingBlock ?: return@LaunchedEffect
        val page = pages.indexOfFirst { target in it }
        if (page >= 0) pagerState.scrollToPage(page)
        onPendingConsumed()
    }

    // …and the same auto-bookmark the scroll carries: the page settles, the
    // block it opens on is written, and the next visit lands there.
    LaunchedEffect(bookId, document, pages.size, placed) {
        if (!placed || pages.isEmpty()) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                delay(700)
                val index = pages.getOrNull(page)?.first ?: return@collectLatest
                withContext(Dispatchers.IO) {
                    runCatching {
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

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { room = it }
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        pageSpacing = 14.dp
    ) { page ->
        val range = pages.getOrNull(page) ?: return@HorizontalPager
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            range.forEach { index ->
                val block = content.blocks.getOrNull(index) ?: return@forEach
                val highlight = marks.firstOrNull { it.isHighlight && it.positionIndex == index }
                val note = marks.firstOrNull { it.isNote && it.positionIndex == index }
                val bookmark = marks.firstOrNull {
                    it.markKind == ReaderMarkKind.BOOKMARK && it.positionIndex == index
                }
                ReaderParagraphBlock(
                    block = block,
                    palette = palette,
                    highlightColor = highlight?.let { readerHighlighter(it.colorKey).ink }
                        ?: Color.Transparent,
                    note = note?.note.orEmpty(),
                    bookmarked = bookmark != null,
                    onLongPress = {
                        onLongPress(ReaderParagraph(index, block.section, block.text, block.isHeading))
                    },
                    onTap = onTap,
                    query = query,
                    hitHere = hitIndex == index && query.isNotBlank(),
                    hitLength = query.length
                )
            }
        }
    }
}

/**
 * THE BREAK, worked out from the room the screen has.
 *
 * A heading, a paragraph and a picture all take a HEIGHT; the pages are the
 * ranges that fit the height available at the member's own type size. An
 * unmeasured screen (the first frame, before anything has been laid out) is one
 * page, which is the honest answer when there is no room to divide yet.
 */
private fun paginateBlocks(
    blocks: List<ReaderBlock>,
    room: IntSize,
    scale: Float,
    measurer: TextMeasurer,
    density: Density
): List<IntRange> {
    if (blocks.isEmpty()) return emptyList()
    val side = with(density) { PAGE_SIDE_PADDING.roundToPx() }
    val vertical = with(density) { PAGE_VERTICAL_PADDING.roundToPx() }
    val width = room.width - side
    val height = room.height - vertical
    if (width <= 0 || height <= 0) return listOf(blocks.indices)

    val pages = ArrayList<IntRange>()
    var start = 0
    var used = 0
    blocks.forEachIndexed { index, block ->
        val gap = with(density) { 2.dp.roundToPx() }
        val needed = if (block.imagePath != null) {
            // A picture's height is not known until it is decoded, so a page's
            // worth is estimated from the width — the same guess the page's own
            // layout makes, and the reason an illustrated page may end early.
            (width * 0.62f).toInt() + gap
        } else {
            val style = pagedTextStyle(block, scale)
            runCatching {
                measurer.measure(
                    text = AnnotatedString(block.text),
                    style = style,
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = width)
                ).size.height + gap
            }.getOrDefault(with(density) { 26.dp.roundToPx() })
        }
        if (used > 0 && used + needed > height) {
            pages.add(start until index)
            start = index
            used = 0
        }
        used += needed
        if (used >= height && index < blocks.size - 1) {
            pages.add(start..index)
            start = index + 1
            used = 0
        }
    }
    if (start < blocks.size) pages.add(start until blocks.size)
    return pages.filter { !it.isEmpty() }.ifEmpty { listOf(blocks.indices) }
}

/** The type a block is drawn in, at the member's own size — the same numbers
 *  ReaderParagraphBlock uses, so a measured page is the page they will read. */
private fun pagedTextStyle(block: ReaderBlock, scale: Float): TextStyle {
    val level = when {
        block.headingLevel in 1..3 -> block.headingLevel
        block.isHeading -> 1
        else -> 0
    }
    return when (level) {
        1 -> TextStyle(
            fontFamily = FrauncesFontFamily,
            fontSize = (23f * scale).sp,
            lineHeight = (31f * scale).sp,
            fontWeight = FontWeight.SemiBold
        )
        2 -> TextStyle(
            fontFamily = FrauncesFontFamily,
            fontSize = (20f * scale).sp,
            lineHeight = (27f * scale).sp,
            fontWeight = FontWeight.SemiBold
        )
        3 -> TextStyle(
            fontFamily = LoraFontFamily,
            fontSize = (18f * scale).sp,
            lineHeight = (26f * scale).sp,
            fontWeight = FontWeight.Bold
        )
        else -> TextStyle(
            fontFamily = LoraFontFamily,
            fontSize = (17f * scale).sp,
            lineHeight = (29f * scale).sp
        )
    }
}

private val PAGE_SIDE_PADDING = 44.dp
private val PAGE_VERTICAL_PADDING = 60.dp

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
    onScrolled: () -> Unit,
    flow: ReaderFlow
) {
    val context = LocalContext.current

    // ── ONE LONG COLUMN OF PAGES (v389) ─────────────────────────────────
    // The other half of the flow choice, and the one a reference book or a
    // scanned document wants: the pages run on under each other instead of
    // being turned (user request: "add like continuos veritical pdf style
    // too"). Its own scroll carries its own bookmark, exactly like the paged
    // one, so switching between them never loses the member's place.
    if (flow == ReaderFlow.SCROLL) {
        PdfScrollReader(
            pageCount = pageCount,
            document = document,
            marks = marks,
            palette = palette,
            bookId = bookId,
            onOpenedAt = onOpenedAt,
            onTap = onTap,
            onScrolled = onScrolled,
            onLongPress = onLongPress
        )
        return
    }

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
    onChapters: () -> Unit,
    pageBar: ReaderPageBar?,
    flowLabel: String,
    onToggleFlow: () -> Unit
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

        // THE PAGE BAR, riding just above the foot and going away with it.
        AnimatedVisibility(
            visible = visible && pageBar != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (pageBar != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 54.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.paper.copy(alpha = 0.96f),
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                start = 3.dp,
                                end = 9.dp,
                                top = 2.dp,
                                bottom = 2.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            ReaderChromeButton(
                                CurioIcons.ChevronLeft,
                                "The page before",
                                palette
                            ) { pageBar.onPrev() }
                            Text(
                                pageBar.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = palette.ink.copy(alpha = 0.8f)
                            )
                            ReaderChromeButton(
                                CurioIcons.ChevronRight,
                                "The next page",
                                palette
                            ) { pageBar.onNext() }
                        }
                    }
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
                // HOW THE BOOK FLOWS — the one control that changes the whole
                // page, so it sits where the thumb already is and names the
                // OTHER way of reading rather than the one it is in.
                Surface(
                    onClick = onToggleFlow,
                    shape = RoundedCornerShape(50),
                    color = palette.surface
                ) {
                    Text(
                        if (flowLabel == ReaderFlow.PAGED.label) "Scrolling" else "Pages",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ink.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
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
    chapters: List<ReaderOutlineEntry>,
    palette: ReaderPalette,
    onPickBlock: (Int) -> Unit,
    onPickPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ReaderSheetFrame("Contents", palette, onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                // THE FILE'S OWN CONTENTS (v389). An EPUB's nav/NCX and a PDF's
                // outline both carry real chapter names, so this is a list of
                // chapters rather than a page grid — the naming is the book's,
                // the indent says how deep it sits, and a part's own chapters
                // read as being under it.
                chapters.isNotEmpty() -> chapters.forEach { entry ->
                    val openable = entry.block >= 0 || entry.isPage
                    Surface(
                        onClick = {
                            if (entry.isPage) onPickPage(entry.page) else onPickBlock(entry.block)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                // The indent is the HIERARCHY, so it has to be an
                                // indent: 18dp a level, with the row's own type
                                // and weight falling as it goes deeper. The old
                                // 14dp and 1sp step made a part and its chapters
                                // look like one flat list.
                                start = (14 + (entry.depth - 1) * 18).dp,
                                end = 12.dp,
                                top = if (entry.depth <= 1) 12.dp else 9.dp,
                                bottom = if (entry.depth <= 1) 12.dp else 9.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            // A deeper row wears a small mark in the accent, so
                            // the level is legible even where the indent is
                            // slight (a wrapped title starts at the same edge
                            // whatever it is, which is what an indent alone
                            // cannot say).
                            if (entry.depth > 1) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(if (entry.depth == 2) 16.dp else 11.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(palette.accent.copy(alpha = if (entry.depth == 2) 0.75f else 0.4f))
                                )
                            }
                            Text(
                                entry.title,
                                style = TextStyle(
                                    fontFamily = WritingFontFamily,
                                    fontSize = when (entry.depth) {
                                        1 -> 16.sp
                                        2 -> 14.5.sp
                                        else -> 13.5.sp
                                    },
                                    fontWeight = if (entry.depth <= 1) FontWeight.SemiBold
                                    else FontWeight.Normal,
                                    color = palette.ink
                                ),
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                when {
                                    entry.isPage -> "p ${entry.page}"
                                    openable -> ""
                                    else -> "not in this file"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.ink.copy(alpha = 0.45f)
                            )
                        }
                    }
                }

                content is ReaderContent.Pages -> {
                    Text(
                        "This PDF carries no contents of its own.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink.copy(alpha = 0.6f)
                    )
                    // The fallback: a page grid, because on a file with no
                    // contents the only jumps there are ARE the pages.
                    val chunks = (0 until content.pageCount).chunked(4)
                    chunks.take(60).forEachIndexed { row, pages ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pages.forEach { page ->
                                Surface(
                                    onClick = { onPickPage(page + 1) },
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

                content is ReaderContent.Text -> Text(
                    "This file has no chapter headings of its own.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.6f)
                )

                else -> Text(
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

    /**
     * v389 — HOW THE BOOK FLOWS.
     *
     * One column you scroll, or pages you turn — a choice every reader on earth
     * offers, and the two are good at different things: a scroll is how a novel
     * is read in bed, and pages are how a PDF and a reference book are looked at
     * (user request: "add horizontal readies too, also add like continuos
     * veritical pdf style too and same ofor epub page like epub option too").
     *
     * Kept PER KIND, because the two kinds start from opposite ends: a reflowed
     * book opens as a scroll (it has no pages of its own) and a PDF opens as
     * pages (it has nothing but). Changing one never changes the other.
     */
    var textFlow by mutableStateOf(ReaderFlow.SCROLL)
    var pageFlow by mutableStateOf(ReaderFlow.PAGED)
}

/** The two ways a book can be laid out on screen. */
private enum class ReaderFlow(val label: String) {
    SCROLL("Scrolling"),
    PAGED("Pages");

    fun flipped(): ReaderFlow = if (this == SCROLL) PAGED else SCROLL
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
    data class Text(
        val blocks: List<ReaderBlock>,
        /**
         * THE BOOK'S OWN CONTENTS (v389) — EPUB nav/NCX, or a PDF outline. What
         * the chapters sheet lists, with the reader's own headings as the
         * fallback for a file that has none (see [ReaderOutlineEntry]).
         */
        val outline: List<ReaderOutlineEntry> = emptyList(),
        /**
         * Section 1..n as the FILE each one came from, so an outline entry can
         * be turned into the block it opens.
         */
        val sectionSources: List<String> = emptyList(),
        /**
         * True when the book numbers its own pages in the text — the reader
         * then stops counting them itself (see [carriesOwnPageMarkers]).
         */
        val ownPages: Boolean = false
    ) : ReaderContent

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

/**
 * v389 — THE PAGE BAR.
 *
 * A paged book needs one thing neither the head nor the foot can give it: which
 * page of how many, and the two arrows that move one. It lives in the CHROME, so
 * the same tap that puts the tools away takes it away too and a reader with the
 * chrome gone has nothing over the words at all (user request: "a floating page
 * chnaging bar in the tool bar which again hides with the tool barm").
 */
private class ReaderPageBar(
    val label: String,
    val onPrev: () -> Unit,
    val onNext: () -> Unit
)

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
        type.startsWith("text/") || name.endsWith(".txt") -> {
            val blocks = readPlainText(context, value, file)
            ReaderContent.Text(
                blocks = blocks,
                // A plain text file has no contents of its own to read, and its
                // own line numbering is plain text's business — but it very
                // often still prints the print edition's page numbers, which is
                // the same duplication the EPUB check looks for.
                ownPages = carriesOwnPageMarkers(blocks.map { it.text })
            )
        }

        else -> {
            val read = readEpubText(context, value, file)
            ReaderContent.Text(
                blocks = read.blocks,
                outline = read.outline,
                sectionSources = read.sources,
                ownPages = carriesOwnPageMarkers(read.blocks.map { it.text })
            )
        }
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
        // ── THE PAGE IS DRAWN FOR THE SCREEN IT IS DRAWN ON (v389c) ───────
        // A fixed 1.5x was the wrong number twice over: on a 400dpi phone the
        // page was rendered SMALLER than the box it was then stretched into
        // (which is what "the pdf quality" is — a bitmap scaled up is a blurry
        // bitmap), and on a cheap screen it rendered bigger than anything that
        // could ever be shown. The honest scale is the one that makes the page
        // exactly as many pixels wide as the screen the reader is holding, so
        // there is no resampling in either direction at the fit size. The
        // height is capped because a very tall page at screen width would hold
        // a poster per page in memory, and beyond the cap the extra pixels are
        // not being displayed anyway.
        val screenWidth = context.resources.displayMetrics.widthPixels.coerceAtLeast(320)
        val scale = (screenWidth.toFloat() / page.width.coerceAtLeast(1))
            .coerceIn(1f, MAX_PDF_RENDER_SCALE)
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(
            (page.width * scale).toInt().coerceAtLeast(1),
            height,
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
 * How far a page may be scaled up. 3x is already more pixels than any phone
 * shows of a page that has to fit on it; past that the bitmap is memory the
 * reader never gets to look at.
 */
private const val MAX_PDF_RENDER_SCALE = 3f

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
/** An EPUB, read: its blocks, the file each section came from, and its own
 *  contents — the three things that let an outline entry name a block. */
private class EpubRead(
    val blocks: List<ReaderBlock>,
    val sources: List<String>,
    val outline: List<ReaderOutlineEntry>
)

private fun readEpubText(
    context: android.content.Context,
    value: String,
    file: File?
): EpubRead {
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
        val sources = ArrayList<String>()
        var outline: List<ReaderOutlineEntry> = emptyList()
        ZipFile(source).use { zip ->
            // The book's OWN contents, read while the archive is open anyway.
            outline = epubOutline(zip)
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
                sources.add(entry.name)
                val found = epubBlocks(context, zip, entry.name, raw, section, heading)
                if (found.none { it.isHeading } && heading.isNotBlank()) {
                    // A file with no heading of its own gets the one it is named
                    // by, so the chapter sheet can still list it.
                    blocks.add(ReaderBlock(heading, section, heading, isHeading = true, headingLevel = 1))
                }
                blocks.addAll(found)
            }
        }
        EpubRead(blocks = blocks, sources = sources, outline = outline)
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

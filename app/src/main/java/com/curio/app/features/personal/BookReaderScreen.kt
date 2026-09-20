package com.curio.app.features.personal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale
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
    // v389c — WHAT IS SELECTED RIGHT NOW. One selection for the whole reader (a
    // PDF page and a reflowable paragraph are two ways of choosing the same
    // thing: words), so the bar that acts on it is drawn once, and either
    // surface simply reports what the finger swept.
    var selection by remember { mutableStateOf<ReaderSelection?>(null) }

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
        // A tap means "get out of the way" — and if words were selected, the
        // selection goes before the chrome does, because the selection's own bar
        // is standing where the foot of the reader is.
        if (selection != null) {
            selection = null
            return
        }
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
    // ── A PAGE ASKED FOR, INSTEAD OF A PAGE CHANGED (v399) ──────────────
    //
    // A PDF has TWO reading surfaces — a pager of pages and a column of them —
    // and they hold two different scroll states. Every jump used to be performed
    // on the PAGER's state directly, so in the scrolling flow (which is what a
    // PDF opens in when the member has never switched) "Continue reading", every
    // mark in the places sheet and every contents row moved an off-screen pager
    // and the member saw nothing happen at all. The number is now ASKED FOR and
    // whichever surface is showing takes it and clears it, exactly like a text
    // jump already did (see [pendingBlock]).
    var pendingPage by remember { mutableStateOf<Int?>(null) }

    // ── AND A BLOCK ASKED FOR, HOISTED WITH IT (v406) ──────────────────
    //
    // [jumpToMark] hands a place to whichever surface is showing — a block for
    // reflowable text, a page for a PDF — and a local function cannot reach a
    // local that is declared LATER in the same body, so this state lives up
    // here beside the page ask rather than down by the chapters sheet.
    var pendingBlock by remember { mutableStateOf<Int?>(null) }

    /**
     * v405 — A TURN THE READER ASKED FOR IS NOT THE MEMBER MOVING.
     *
     * Every reading surface puts the chrome away when it starts scrolling,
     * because a scroll means the member is moving through the book. A page
     * asked for by the reader ITSELF scrolls exactly the same way — so pressing
     * the page bar's arrow put the bar away, and the member was HOLDING the
     * chrome when the tools left from under their finger (member's report: "when
     * i switch page though the that page switch pill why the tools hide fix that
     * behaviour it should hide only when i touch the page"). This flag says "that
     * scroll is ours", and it is cleared when the scroll settles.
     */
    var askedByReader by remember { mutableStateOf(false) }

    /**
     * A jump asked for from OUTSIDE the reading surface — a chapter, a mark,
     * "Continue reading". The sheet closes and the member wants the page and not
     * the tools, so the chrome goes.
     */
    fun jumpToPage(page: Int) {
        pendingPage = page
        askedByReader = true
        hideChrome()
    }

    /**
     * THE PAGE BAR'S OWN TURN — the same ask WITHOUT putting the chrome away:
     * the member is using the bar, so the bar stays (see [askedByReader]).
     */
    fun turnPageFromBar(page: Int) {
        pendingPage = page
        askedByReader = true
    }

    // THE FLAG CANNOT STICK. A turn asked for settles in a moment, and it is
    // normally cleared by the scroll's own "finished" edge — but an instant jump
    // (the column's own `scrollToItem`) may never report one, and a flag left
    // standing would stop the chrome from ever hiding on a member's own scroll
    // again. So it also expires on its own.
    LaunchedEffect(askedByReader) {
        if (askedByReader) {
            delay(1200)
            askedByReader = false
        }
    }

    // ── THE CHROME LEAVES ON ITS OWN — BUT NOT OUT FROM UNDER A HAND (v406) ──
    //
    // The countdown is reset by a turn asked for at the bar, and it does not run
    // at all while one is in flight: the member was HOLDING the tools when the
    // tools left (their report: "when i switch page though the that page switch
    // pill why the tools hide … it should hide only when i touch the page"). A
    // turn asked for is not the member moving, so the countdown waits for it to
    // settle and then starts again from full.
    LaunchedEffect(chrome, sheet, askedByReader) {
        if (!chrome || sheet != null) return@LaunchedEffect
        if (askedByReader) return@LaunchedEffect
        delay(4200)
        chrome = false
    }

    /**
     * Jump to a MARK's own place — a block index in a reflowable book, a page in
     * a PDF. It lands on the passage ITSELF: a highlight belongs to the words it
     * was made on, never to a heading that happens to share its number.
     */
    suspend fun jumpToMark(index: Int) {
        when (val loaded = content) {
            // v406 — A TEXT JUMP IS ASKED FOR TOO. A reflowable book has TWO
            // reading surfaces and only the scroll list is hoisted, so a jump
            // done here directly moved an off-screen list whenever the PAGED
            // flow was showing. The block is handed over exactly as a chapter
            // jump hands it (see [pendingBlock]) and whichever surface is
            // showing takes it and clears it.
            is ReaderContent.Text -> {
                pendingBlock = index.coerceIn(0, (loaded.blocks.size - 1).coerceAtLeast(0))
            }
            // v399 — A PAGE JUMP IS ASKED FOR, NOT PERFORMED. This used to call
            // `pagerState.scrollToPage` directly, which is the PAGED reader's
            // pager — so in the scrolling flow (the default for a PDF nobody has
            // switched) "Continue reading" and every mark in the places sheet
            // moved an off-screen pager and the member saw nothing happen. The
            // number is handed to whichever surface is showing, exactly like a
            // text jump is (see [pendingPage]).
            is ReaderContent.Pages -> pendingPage = index.coerceAtLeast(0)
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
    // v399 — THE PAGE THE SCROLLING COLUMN IS SHOWING.
    //
    // The column is one long strip and has no "page" of its own, so it reports
    // the one under the reader's eye; the bookmarks, the contents and the page
    // bar then have a number to work with.
    var shownPage by remember(bookId, document) { mutableStateOf(0) }

    // ── WHERE THEY ARE, LIVE (v406) ────────────────────────────────────
    //
    // The progress card reads this instead of the stored auto-bookmark, so it
    // follows the reading rather than the last write. Every surface already had
    // a live source — the column's `firstVisibleItemIndex`, the pager's
    // `currentPage`, and the two the PDF reports through [onPageShown] — except
    // the PAGED text flow, whose pages are its own invention and whose block is
    // only known inside it, so that one reports `liveTextBlock`.
    var liveTextBlock by remember(bookId, document) { mutableIntStateOf(-1) }

    // ── v422 — ONE STEP, FOR A TAP (see [ReaderLook.tapZones]) ────────
    //
    // A tap inside a zone asks for the next thing rather than for the chrome: a
    // page where the book has pages, a screenful where the book scrolls. It is
    // the same step the page bar's own arrows take, so the two can never disagree
    // about what "on" means.
    fun stepPage(step: Int) {
        askedByReader = true
        when (val loaded = content) {
            is ReaderContent.Pages -> if (ReaderLook.pageFlow == ReaderFlow.PAGED) {
                val last = (loaded.pageCount - 1).coerceAtLeast(0)
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage + step).coerceIn(0, last))
                }
            } else {
                val last = (loaded.pageCount - 1).coerceAtLeast(0)
                turnPageFromBar((shownPage + step).coerceIn(0, last))
            }

            is ReaderContent.Text -> if (ReaderLook.textFlow == ReaderFlow.PAGED) {
                val last = (textPageCount - 1).coerceAtLeast(0)
                scope.launch {
                    textPager.animateScrollToPage((textPager.currentPage + step).coerceIn(0, last))
                }
            } else {
                // A SCREENFUL, measured on the list itself and a little short of
                // one, so the line the member was reading is still above the new
                // first line rather than gone.
                val screenful = listState.layoutInfo.viewportSize.height * 0.86f
                scope.launch { listState.animateScrollBy(step * screenful) }
            }

            null -> Unit
        }
    }

    // ── v424 — THE OTHER HALF OF A ZONE: A SCREENFUL ─────────────────
    //
    // A zone can ask for the READING to move on rather than for the page to turn,
    // which is the same screenful the head and the foot have always moved — a
    // little short of one, so the line the member was reading is still above the
    // new first line rather than gone (see [ReaderZoneAction.scrolls]).
    fun scrollPage(step: Int) {
        askedByReader = true
        val screenful = listState.layoutInfo.viewportSize.height * 0.86f
        when (val loaded = content) {
            is ReaderContent.Pages -> if (ReaderLook.pageFlow == ReaderFlow.PAGED) {
                val last = (loaded.pageCount - 1).coerceAtLeast(0)
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage + step).coerceIn(0, last))
                }
            } else {
                scope.launch { listState.animateScrollBy(step * screenful) }
            }

            is ReaderContent.Text -> if (ReaderLook.textFlow == ReaderFlow.PAGED) {
                val last = (textPageCount - 1).coerceAtLeast(0)
                scope.launch {
                    textPager.animateScrollToPage((textPager.currentPage + step).coerceIn(0, last))
                }
            } else {
                scope.launch { listState.animateScrollBy(step * screenful) }
            }

            null -> Unit
        }
    }

    // ── AND THE TAP ITSELF: a zone, or the chrome ────────────────────
    //
    // The zone is measured against the SURFACE the member sees, so a tap on the
    // side of the SCREEN answers even where the page under it is magnified and
    // its own frame no longer spans what is on screen (v424 — the surfaces
    // translate a tap inside a page before it gets here; see [readerZoneActionAt]).
    val onSurfaceTap: (Offset, IntSize) -> Unit = { at, size ->
        val action =
            if (ReaderLook.tapZones) readerZoneActionAt(at, size) else ReaderZoneAction.OFF
        when {
            action == ReaderZoneAction.OFF -> tapPage()
            action.scrolls -> scrollPage(action.step)
            else -> stepPage(action.step)
        }
    }

    val livePlace: ReaderLivePlace? = when (val loaded = content) {
        is ReaderContent.Text -> {
            val total = loaded.blocks.size
            val index = if (ReaderLook.textFlow == ReaderFlow.PAGED) {
                liveTextBlock
            } else {
                listState.firstVisibleItemIndex
            }
            if (total > 0 && index >= 0) {
                ReaderLivePlace(index.coerceIn(0, total - 1), total)
            } else {
                null
            }
        }

        is ReaderContent.Pages -> {
            val total = loaded.pageCount
            val index = if (ReaderLook.pageFlow == ReaderFlow.PAGED) {
                pagerState.currentPage
            } else {
                shownPage
            }
            if (total > 0) ReaderLivePlace(index.coerceIn(0, total - 1), total) else null
        }

        null -> null
    }

    // ── THE BOOK'S PAGE MARK FOLLOWS THE READING (v411) ─────────────────
    //
    // The member: "the progress overriden based on the user pdf progress if
    // they open again and read some it will be updated to that".
    //
    // The book row carries its own page mark ([PersonalBookEntity.currentPage],
    // the one the book page's stepper writes) and the reader carries its own
    // position row; the progress card picks between them by "the most recent
    // write wins". That rule quietly broke the moment anything ELSE touched the
    // row — resolving a cover, saving a blurb, attaching the file — because
    // those bump the ROW's stamp without moving the page, so a stale hand-set
    // page outlived the page the member was really reading.
    //
    // So the reader now writes the page it is actually showing back onto the
    // row: the two answers can no longer disagree, the shelf and the book page
    // agree with the file, and reading simply takes the card back over.
    //
    // One column, one write, and DEBOUNCED — flicking through thirty pages is
    // one write on the page they settled on, never thirty on the way past.
    // The +1 is the file's own 1-based numbering (the card's `lastPage`), and
    // only a book of real PAGES has one: a reflowable book's position is a
    // block index, which is not a page of anything (see [lastPageOf]).
    val livePageMark = (content as? ReaderContent.Pages)
        ?.let { livePlace?.index?.plus(1) }
    LaunchedEffect(bookId, document, livePageMark) {
        if (livePageMark == null || document.isBlank()) return@LaunchedEffect
        delay(ReaderPageMarkDebounceMs)
        runCatching { PersonalRepositoryHolder.repo.setPage(bookId, livePageMark) }
    }

    // ── THE PAGE FOLLOWS THE PHONE, UNLESS IT IS ASKED NOT TO (v406/v418) ──
    //
    // AUTO is "turn with the phone": the window rotates and the page turns with
    // it, which IS the horizontal mode a wide page wants and needs no layout of
    // its own (member's choice: "just allow landscape"). The member can also
    // hold the page UPRIGHT or WIDE while a book is open ([ReaderLook
    // .orientation]), for reading in bed. It is applied on the ACTIVITY, because
    // the window is the activity's own — and v418 applies it to BOTH kinds of
    // book, since a reflowable book re-lays itself out at any width and can hold
    // one just as well as a PDF can.
    val readerActivity = remember(context) { context.findActivity() }
    LaunchedEffect(content, ReaderLook.orientation) {
        val act = readerActivity ?: return@LaunchedEffect
        runCatching { act.requestedOrientation = ReaderLook.orientation.requested() }
    }
    DisposableEffect(readerActivity) {
        onDispose {
            runCatching {
                readerActivity?.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // THE BOOK'S OWN PRINTED PAGES (v389c) — resolved the same way a chapter is,
    // because to the reader they are the same thing: a name for a place.
    val printedPages: List<ReaderOutlineEntry> = when (val loaded = content) {
        is ReaderContent.Text -> loaded.pages.map { entry ->
            entry.copy(block = blockForEntry(loaded, entry))
        }

        else -> emptyList()
    }

    // ── THE BOOK'S OWN PAGE NUMBER (v422) ─────────────────────────────
    //
    // A reflowed book has no pages of its own, so the reader used to count its
    // own screenfuls at the member — "Page 12 of 340", a number that changed
    // with the type size and named nothing the book itself said (user report:
    // "our page number is making the epub feels bad"). The book's own page list
    // is the honest answer, and it is already resolved just above: the page the
    // member is in is the last printed page at or before where they are. A book
    // that prints no page numbers says nothing here rather than something false,
    // and the page bar keeps its two arrows either way — the number was the
    // thing that lied, not the way to turn the page.
    //
    // Declared HERE, above the bar that reads it, because a local function in
    // Kotlin cannot reach a local declared later in the same body (the same
    // reason [jumpToBlock] sits below its state).
    fun printedPageAt(block: Int): String =
        printedPages.lastOrNull { it.block in 0..block }?.title.orEmpty()

    val pageBar: ReaderPageBar? = when (val loaded = content) {
        is ReaderContent.Pages -> if (ReaderLook.pageFlow == ReaderFlow.PAGED) {
            ReaderPageBar(
                label = "Page ${pagerState.currentPage + 1} of ${loaded.pageCount}",
                onPrev = {
                    askedByReader = true
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage - 1).coerceAtLeast(0)
                        )
                    }
                },
                onNext = {
                    askedByReader = true
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(loaded.pageCount - 1)
                        )
                    }
                }
            )
        } else {
            // The scrolling flow used to have NO bar at all, so the one surface
            // that shows a single page at a time was the one that could not turn
            // one: the bar names the page the column is showing and asks the
            // column to go to the next (v399).
            val at = shownPage.coerceIn(0, (loaded.pageCount - 1).coerceAtLeast(0))
            ReaderPageBar(
                label = "Page ${at + 1} of ${loaded.pageCount}",
                onPrev = { turnPageFromBar((at - 1).coerceAtLeast(0)) },
                onNext = { turnPageFromBar((at + 1).coerceAtMost(loaded.pageCount - 1)) }
            )
        }

        is ReaderContent.Text -> if (
            ReaderLook.textFlow == ReaderFlow.PAGED && textPageCount > 0
        ) {
            // v422 — WHAT THE BOOK SAYS, NOT WHAT THE READER COUNTED. See
            // [printedPageAt]: the label is the book's own printed page, and it
            // is blank for a book that carries none.
            ReaderPageBar(
                label = printedPageAt(liveTextBlock),
                onPrev = {
                    askedByReader = true
                    scope.launch {
                        textPager.animateScrollToPage((textPager.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onNext = {
                    askedByReader = true
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
        if (sheet != ReaderSheet.PLACES) return@LaunchedEffect
        if (content !is ReaderContent.Pages) return@LaunchedEffect
        if (pdfChapters != null || document.isBlank()) return@LaunchedEffect
        pdfChapters = withContext(Dispatchers.IO) {
            runCatching { pdfOutline(context, document) }.getOrNull()
        }
    }
    val chapters: List<ReaderOutlineEntry> = when (val loaded = content) {
        is ReaderContent.Text -> if (loaded.outline.isNotEmpty()) {
            loaded.outline.map { entry -> entry.copy(block = blockForEntry(loaded, entry)) }
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
    // cleared by it so the same jump never fires twice. The state itself is
    // hoisted with the page ask far above (see `pendingBlock` there):
    // [jumpToMark] reads it, and a local function cannot reach a local declared
    // later in the same body.
    fun jumpToBlock(index: Int) {
        pendingBlock = index
        chrome = false
    }

    // ── v418 — THE FLOW SWITCH SETTLES IN (member: "the flow animation is bad
    // of it"). Scrolling ↔ Pages used to swap in one frame, which read as a
    // flinch. The reading surface now fades and lifts a hair each time the flow
    // (or the format, on first load) changes — a single instance, so the two
    // pagers are never composed at once.
    val flowKey = if (content is ReaderContent.Pages) {
        ReaderLook.pageFlow.name
    } else {
        ReaderLook.textFlow.name
    }
    val flowFade = remember { Animatable(1f) }
    LaunchedEffect(flowKey) {
        flowFade.snapTo(0f)
        flowFade.animateTo(1f, tween(durationMillis = 230, easing = FastOutSlowInEasing))
    }
    // ── v424 — WHERE THE READER'S OWN SURFACE SITS, AND HOW BIG IT IS ──
    //
    // A tap zone belongs to the surface the member sees, and the scrolling PDF's
    // frames sit on a document that can be wider than the screen — so a tap inside
    // one of them reports the FRAME's own coordinates. Both facts are captured
    // here, once, and handed to the surface below (see [PdfScrollReader]).
    var surfaceOrigin by remember { mutableStateOf(Offset.Zero) }
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
            .onGloballyPositioned { coords ->
                surfaceOrigin = coords.positionInRoot()
                surfaceSize = coords.size
            }
            .graphicsLayer {
                alpha = flowFade.value
                translationY = (1f - flowFade.value) * 16.dp.toPx()
            }
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
                    color = palette.ink.copy(alpha = 0.75f),
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
                    onBlockShown = { liveTextBlock = it },
                    onLongPress = { marking = it },
                    chromeVisible = chrome,
                    onTap = onSurfaceTap,
                    onScrolled = { scrolling ->
                        // A scroll the READER asked for keeps the chrome (v405).
                        if (scrolling) {
                            if (!askedByReader) hideChrome()
                        } else {
                            askedByReader = false
                        }
                    },
                    selection = selection,
                    onSelect = { swept ->
                        // Selecting words is a deliberate act on the page: the
                        // chrome steps out of the way of the selection's bar.
                        selection = swept
                        chrome = false
                    },
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
                    pendingPage = pendingPage,
                    onPendingPageConsumed = { pendingPage = null },
                    onPageShown = { shownPage = it },
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
                    onTap = onSurfaceTap,
                    onScrolled = { scrolling ->
                        // A scroll the READER asked for keeps the chrome (v405).
                        if (scrolling) {
                            if (!askedByReader) hideChrome()
                        } else {
                            askedByReader = false
                        }
                    },
                    flow = ReaderLook.pageFlow,
                    // The sweep on a PDF page is reported the same way the
                    // reflowable one is, because it is the same act: the words
                    // go to the ONE bar the reader owns, and the chrome steps
                    // out of its way either way.
                    selection = selection,
                    onSelect = { swept ->
                        selection = swept
                        chrome = false
                    }
                )

                // `content` is a delegated property, so the null check above
                // cannot smart-cast it — this branch is what makes the `when`
                // exhaustive (the file was still being read a moment ago).
                null -> Unit
            }
        }

        ReaderChrome(
            visible = chrome && !ReaderLook.zonesEditing,
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
            tapZones = ReaderLook.tapZones,
            onToggleTapZones = { ReaderLook.tapZones = !ReaderLook.tapZones },
            onEditTapZones = { ReaderLook.zonesEditing = true },
            onClose = { navController.popBackStack() },
            onSearch = {
                searching = ReaderSearch().also { started ->
                    started.isPaged = content is ReaderContent.Pages
                }
                sheet = ReaderSheet.SEARCH
            },
            onInk = { sheet = ReaderSheet.INK },
            onPlaces = { sheet = ReaderSheet.PLACES }
        )

        // ── v424 — AND THE ZONES' OWN EDITOR, ON THE PAGE ───────────────
        //
        // Drawn over everything (the chrome stands down while it is up, see the
        // call above) because the lines it places have to be seen against the
        // page they govern — and it takes every tap, so placing a line can never
        // turn a page by accident.
        AnimatedVisibility(
            visible = ReaderLook.zonesEditing,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140))
        ) {
            ReaderTapZoneEditor(
                palette = palette,
                onDone = { ReaderLook.zonesEditing = false }
            )
        }

        // ── THE SELECTION'S BAR (v389c) ─────────────────────────────────
        //
        // Words are selected on the page and marked from here, without leaving
        // the reading: a tap on an ink is the highlight, and a note or a
        // bookmark is one more tap. It stands where the foot of the reader is
        // and the chrome is already out of the way (see [tapPage]).
        val swept = selection
        if (swept != null && swept.text.isNotBlank() && sheet == null) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(160)) + slideInVertically { it / 3 },
                exit = fadeOut(tween(140)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                ReaderSelectionBar(
                    selection = swept,
                    palette = palette,
                    onHighlight = { ink ->
                        selection = null
                        scope.launch {
                            saveReaderMark(
                                bookId = bookId,
                                document = document,
                                paragraph = swept.asParagraph(),
                                kind = ReaderMarkKind.HIGHLIGHT,
                                text = swept.text,
                                colorKey = ink.key
                            )
                        }
                    },
                    onNote = {
                        noteFor = swept.asParagraph()
                        selection = null
                    },
                    onBookmark = {
                        selection = null
                        scope.launch {
                            saveReaderMark(
                                bookId = bookId,
                                document = document,
                                paragraph = swept.asParagraph(),
                                kind = ReaderMarkKind.BOOKMARK,
                                text = swept.text.take(90)
                            )
                        }
                    },
                    onMore = {
                        // The whole-place sheet is still here, one tap away:
                        // selecting words ADDS a way to mark a book up, it does
                        // not take the old one away.
                        marking = swept.asParagraph()
                        selection = null
                    },
                    onClear = { selection = null }
                )
            }
        }
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
            // v406 — the type size is a text book's business: a PDF page is a
            // picture of a page, and its own size is the pinch's.
            showType = content is ReaderContent.Text,
            onPick = { key -> ReaderLook.inkKey = key },
            onDismiss = { sheet = null }
        )

        // v394 — ONE SHEET for where you are, what you kept and where you can
        // go. v395 — and now on ONE PAGE behind ONE door: the progress card,
        // the marks and the book's own contents are the same scroll, because a
        // reader's three questions have an order and a chip that hides two of
        // them is a question of its own.
        //
        // The LIVE auto-bookmark is the one to show, not the row read at open:
        // the reader writes its position as they read, so the marks flow
        // already knows where they are (the row read at open is only the
        // fallback for a book whose position has never been written).
        ReaderSheet.PLACES -> ReaderPlacesSheet(
            marks = marks,
            position = marks.firstOrNull { it.isPosition } ?: openedAt,
            live = livePlace,
            content = content,
            chapters = chapters,
            pages = printedPages,
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
            onPickBlock = { block ->
                sheet = null
                if (block >= 0) jumpToBlock(block)
            },
            onPickPage = { page ->
                sheet = null
                jumpToPage((page - 1).coerceAtLeast(0))
            },
            onBookmarkHere = { paragraph ->
                scope.launch {
                    saveReaderMark(
                        bookId = bookId,
                        document = document,
                        paragraph = paragraph,
                        kind = ReaderMarkKind.BOOKMARK,
                        text = paragraph.text.take(90)
                    )
                }
            },
            onContinueAt = { index ->
                sheet = null
                scope.launch { jumpToMark(index) }
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
                    is ReaderContent.Pages -> jumpToPage(index)

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
    /** v406 — the block being shown, reported live so the progress is live. */
    onBlockShown: (Int) -> Unit,
    onLongPress: (ReaderParagraph) -> Unit,
    chromeVisible: Boolean,
    onTap: (Offset, IntSize) -> Unit,
    onScrolled: (Boolean) -> Unit,
    /** v389c — the live selection, when it belongs to this book's text. */
    selection: ReaderSelection?,
    onSelect: (ReaderSelection) -> Unit,
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
            .collect { scrolling -> onScrolled(scrolling) }
    }

    // PINCH makes the TYPE bigger, not the pixels: a reflowed book that is
    // magnified like a photograph is a worse book, and every reader on earth
    // re-lays the page out instead (v389).
    val zoomModifier = Modifier.pinchToZoom { zoom, _, _ ->
        ReaderLook.textScale = (ReaderLook.textScale * zoom).coerceIn(0.8f, 2.6f)
        // The type IS the zoom here, so there is no pan for the page to take —
        // one finger keeps scrolling the book, which is the whole point of a
        // reflowed page (see [pinchToZoom]).
        Offset.Zero
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

    // ── AND IT IS REPORTED AS THEY MOVE, NOT AS THEY SETTLE (v406) ──
    // The write below waits for the scroll to stop; the SHEET does not have to.
    // The block on screen is reported the moment it changes, so the progress
    // card names the passage the member is actually in.
    LaunchedEffect(bookId, document, content.blocks.size, restored) {
        if (!restored) return@LaunchedEffect
        // In PAGED flow the pager owns the place and reports it itself (v406).
        if (ReaderLook.textFlow == ReaderFlow.PAGED) return@LaunchedEffect
        snapshotFlow { state.firstVisibleItemIndex }.collect { index -> onBlockShown(index) }
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
            onBlockShown = onBlockShown,
            restoredBlock = restoredBlock,
            marks = marks,
            palette = palette,
            bookId = bookId,
            document = document,
            onLongPress = onLongPress,
            onTap = onTap,
            selection = selection,
            onSelect = onSelect,
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
            .pointerInput(Unit) { detectTapGestures(onTap = { at -> onTap(at, size) }) },
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
            val bookmark = marks.firstOrNull {
                it.markKind == ReaderMarkKind.BOOKMARK && it.positionIndex == index
            }
            ReaderParagraphBlock(
                block = block,
                palette = palette,
                highlights = highlightsFor(marks, index),
                note = note?.note.orEmpty(),
                bookmarked = bookmark != null,
                onLongPress = { onLongPress(ReaderParagraph(index, block.section, block.text, block.isHeading)) },
                selection = selection?.takeIf { !it.isPage && it.index == index }
                    ?.let { range -> range.from..range.to },
                onSelect = { range, text ->
                    onSelect(
                        ReaderSelection(
                            index = index,
                            from = range.first,
                            to = range.last,
                            text = text,
                            isPage = false,
                            section = block.section
                        )
                    )
                },
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
    onTap: (Offset, IntSize) -> Unit,
    /** v424 — the size of the screen the zones are measured on. */
    viewport: IntSize,
    /** v424 — where that screen starts, so a frame's own tap can be translated. */
    surfaceOrigin: Offset,
    onScrolled: (Boolean) -> Unit,
    onLongPress: (Int) -> Unit,
    /** v389c — the live sweep, when it belongs to this page of the file. */
    selection: ReaderSelection?,
    onSelect: (ReaderSelection) -> Unit,
    /** A page asked for from outside — a mark, a chapter, the page bar (v399). */
    pendingPage: Int?,
    onPendingPageConsumed: () -> Unit,
    /** Which page the column is showing, so the bar and the marks can name it. */
    onPageShown: (Int) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    // v422 — THE SIDEWAYS HALF OF A MAGNIFIED DOCUMENT. A page wider than the
    // screen has to be reachable (see the frame maths below), so the column
    // rides in a horizontal scroll of its own; a vertical drag still belongs to
    // the column, because a vertical scroller ignores a sideways one.
    val across = rememberScrollState()

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
            .collect { scrolling -> onScrolled(scrolling) }
    }

    LaunchedEffect(bookId, document, pageCount, restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { index ->
                // The bar and the marks read this the moment it changes; the
                // WRITE waits for the reader to settle, so a flick through ten
                // pages saves the page the finger stopped on, once.
                onPageShown(index)
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

    // A PAGE ASKED FOR FROM OUTSIDE: a mark in the places sheet, a chapter, the
    // page bar's own arrows. The column takes the number itself and clears it,
    // so the same jump never fires twice.
    LaunchedEffect(pendingPage, pageCount) {
        val at = pendingPage ?: return@LaunchedEffect
        val target = at.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        listState.scrollToItem(target)
        onPageShown(target)
        onPendingPageConsumed()
    }

    // ── WHICH PAGES ARE ON SCREEN (v422) ──────────────────────────────
    //
    // A page's words are read when the page can be read — and in this flow that
    // is every sheet the column is showing, not just the one it calls "first".
    // Asking only for `firstVisibleItemIndex` left the sheet below it without a
    // text layer: a hold on its words found nothing to sweep and fell back to
    // the old "mark this page" press, which is why selecting in the scrolling
    // flow felt broken. The column is tall enough to show a page and a half, so
    // the page under the finger was frequently the second one.
    //
    // The window is published as a SET, and written only when the set of visible
    // pages actually changes — never once per scroll frame, so dragging over the
    // column does not recompose the pages it is passing.
    var visiblePages by remember(document) { mutableStateOf(emptySet<Int>()) }
    LaunchedEffect(listState, pageCount) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet() }
            .distinctUntilChanged()
            .collect { visiblePages = it }
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
        // The width a page has at rest — the viewport minus the column's own
        // side air. A magnified page is this multiplied by the zoom.
        val pageWidth = (maxWidth - 28.dp).coerceAtLeast(1.dp)
        // ── ONE PAGE, ONE FRAME, AND THE PAGE YOU PINCHED IS THE ONE THAT GROWS (v399) ──
        //
        // Three passes have gone into this column, and each was taught by the last:
        //
        //   v389c  every page was sized by its WIDTH, so a phone showed two page
        //          images stacked ("for pdf it was showing double pages view").
        //          Each page now fits INSIDE the viewport: one page at a time.
        //   v395   the zoom grew each page's own BOX, so a magnified page could
        //          never overlap its neighbours — but EVERY page in the column
        //          grew with it, which pushed the page above out from under the
        //          member's fingers ("when i pinch zoom in the middle the top part
        //          of the previous page zooms in").
        //   v399   the box NEVER changes size. Only the page you pinched is
        //          magnified, inside its own frame (`clipToBounds`), and the zoom
        //          is anchored at the fingers ([readerZoomedPan]). Nothing around
        //          it moves, so there is nothing to compensate for.
        //
        // A magnified page used to be a WINDOW onto one page: drag to move
        // around it, and when it runs out of room in the direction you are
        // dragging the drag is handed back and the column scrolls on — the rule
        // the member asked for ("it should page change only when it reaches the
        // page end and then on another swipe it does").
        //
        // ── v422 — AND NOW THE WHOLE FILE IS WHAT GROWS ─────────────────
        //
        // One sheet swollen among its neighbours read as a mistake rather than
        // a zoom (user report: "only one page zooms in in that place feels
        // wrong"), so a pinch in the scrolling flow is the DOCUMENT's: every
        // sheet is laid out at the magnified size, the column scrolls a bigger
        // book, and the page under the fingers stays where it was instead of
        // sliding out from under them. Nothing here grows a box per gesture, so
        // there is still nothing to compensate for (see [readerZoomDocument]).
        //
        // A zoom left over from the paged flow belongs to a screen, and this
        // reader's frames are its pages, so it is cleared as the column opens.
        LaunchedEffect(bookId, document) {
            ReaderLook.pdfZoom = 1f
            ReaderLook.pdfPanX = 0f
            ReaderLook.pdfPanY = 0f
            ReaderLook.pdfZoomPage = -1
        }

        // ── AND THE DOCUMENT IS WHAT THE ZOOM BELONGS TO (v422) ────────
        //
        // The factor every sheet is laid out at, read in the composition so the
        // column re-lays out when a pinch changes it — and read from the ONE
        // owner the scroll flow uses, the document itself (`-1`, the sentinel
        // the paged flow leaves free because its frame is the screen).
        val docZoom = if (ReaderLook.pdfZoomPage == -1) ReaderLook.pdfZoom else 1f
        LazyColumn(
            state = listState,
            modifier = Modifier
                // A magnified page is wider than the screen, so the column is as
                // wide as its own sheet and the reader pans it sideways. The
                // width is STATED rather than left to the content: a lazy list
                // has to be handed a bounded width to lay out against.
                .horizontalScroll(across)
                .fillMaxHeight()
                .width(pageWidth * docZoom + 28.dp)
                .clipToBounds()
                // v424 — the two taps that land on the column ITSELF (the air
                // between two sheets) are said in the screen's own space too: the
                // column rides a horizontal scroll, so its x is the screen's x
                // plus whatever has been panned away.
                .pointerInput(viewport) {
                    detectTapGestures(onTap = { at ->
                        onTap(Offset(at.x - across.value, at.y), viewport)
                    })
                },
            // ── AND THE SHEETS ARE SEPARATED (v403) ──────────────────────
            //
            // The pages were stacked FLUSH (`spacedBy(0.dp)`), each one clipped
            // at its own edge, so a column of scans read as one continuous strip
            // with four slivers of rounding down it — and with a page magnified
            // inside its frame the whole thing looked like a printout with the
            // odd page swollen (user report: "its kind of weird looking when the
            // above pages are not separated of their own pages"). A sheet of
            // paper is its own object: it sits on the reader's own paper, it
            // wears a hairline edge, and there is air between it and the next
            // one — which is also what makes a magnified page read as a page
            // held closer rather than as the column stretching.
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        items(count = pageCount, key = { page -> "pdf-page-$page" }) { page ->
            val bitmap by produceState<Bitmap?>(null, document, page) {
                value = withContext(Dispatchers.IO) {
                    runCatching { renderPdfPage(context, document, page) }.getOrNull()
                }
            }
            // The scrolling reader has no "current page" of its own — the ones
            // on screen are whichever the column is showing (`visiblePages`,
            // v422) — so the words are read for every page the member can put a
            // finger on, and for every page already wearing a mark.
            var words by remember(document, page) { mutableStateOf<PdfPageText?>(null) }
            var container by remember { mutableStateOf(IntSize.Zero) }
            // v424 — WHERE THIS SHEET SITS in the window, so a tap it answers can
            // be said in the screen's own coordinates (see the tap handler below).
            var where by remember { mutableStateOf(Offset.Zero) }
            val wanted = page in visiblePages ||
                highlightsFor(marks, page).isNotEmpty()
            LaunchedEffect(document, page, wanted) {
                if (!wanted || words != null) return@LaunchedEffect
                words = withContext(Dispatchers.IO) {
                    runCatching { extractPdfPageText(context, document, page) }.getOrNull()
                }
            }
            // ── THE FRAME, AND WHAT IS MAGNIFIED INSIDE IT (v399) ──────
            //
            // The page is laid out at its OWN size and never changes it: a zoom
            // is drawn inside this frame, so the column's layout is the same at
            // 1× and at 4×, and the pages above and below stay exactly where the
            // finger left them. (`requiredWidth` because these ARE the page's
            // measurements — the column's constraints describe the page at rest.)
            val aspect = bitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: 0f
            // WHOSE ZOOM THIS IS. Read in the composition — it changes only when
            // a pinch starts or ends on a page, so this costs one recomposition
            // per pinch — and used by the DRAW lambda below, where the scale and
            // pan are read live, so a pinch redraws a page instead of
            // recomposing the column. The GESTURE asks the same question live
            // instead, because a handler outlives the composition that armed it.
            val mine = ReaderLook.pdfZoomPage == page
            Box(
                modifier = Modifier
                    // v422 — THE SHEET IS LAID OUT AT THE MAGNIFIED SIZE, and
                    // that is the whole of the document zoom: the column measures
                    // a bigger book, scrolls it, and every word layer measures
                    // the frame it is given — so a sweep over a magnified page
                    // lands on the word under the finger with no extra maths.
                    .requiredWidth(pageWidth * docZoom)
                    .then(
                        if (aspect > 0f) Modifier.requiredHeight(pageWidth / aspect * docZoom)
                        else Modifier.height(pageHeight * docZoom)
                    )
                    // THE PAGE'S OWN EDGE IS THE END OF ITS ZOOM: clipped at its
                    // frame, a magnified page can never reach a neighbour — which
                    // is what the v395 growth was for, without the growth.
                    .clip(RoundedCornerShape(6.dp))
                    // v403 — the frame is PAPER, so the letterbox around a page
                    // that is not the frame's own shape is the sheet's margin
                    // and not a hole through the reader's background, and the
                    // sheet wears its own edge (the hairline below) so a column
                    // of scans reads as pages and not as one continuous strip.
                    .background(palette.paper)
                    .border(
                        width = 1.dp,
                        color = palette.ink.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .onSizeChanged {
                        container = it
                    }
                    .pinchToZoom(
                        // The page's shape, once the render lands: the gesture is
                        // re-armed with it, so the zoom is measured on a page that
                        // has actually been drawn.
                        key = aspect,
                        zoomed = { ReaderLook.pdfZoomPage == -1 && ReaderLook.pdfZoom > 1.02f }
                    ) { zoom, drag, focus ->
                        // v422 — one zoom for the whole file: see
                        // [readerZoomDocument]. v424 — and the fingers' own place
                        // is what the file grows ABOUT.
                        readerZoomDocument(zoom, drag, focus, listState, across)
                    }
                    // Keyed on the page's SHAPE as well as its number: the tap
                    // handler outlives the composition that armed it, and a
                    // double tap has to measure the page it actually sees (v399).
                    //
                    // v424 — AND THE TAP IS TRANSLATED. A sheet's own frame is
                    // the document's, not the screen's, so a tap on the SIDE of
                    // the screen would be read as a point in the middle of a
                    // magnified page and miss its zone. The frame's own place in
                    // the window ([where]) is taken off, which is what makes the
                    // side of the screen answer wherever the page happens to sit.
                    .pointerInput(page, aspect, viewport) {
                        detectTapGestures(
                            onTap = { at -> onTap(at + where - surfaceOrigin, viewport) },
                            onDoubleTap = { at ->
                                readerDoubleTapDocument(at, listState, across)
                            },
                            onLongPress = { if (words == null) onLongPress(page) }
                        )
                    },
                    .onGloballyPositioned { coords -> where = coords.positionInRoot() },
                contentAlignment = Alignment.Center
            ) {
                val drawn = bitmap
                if (drawn != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // THE MAGNIFIED PAGE LIVES INSIDE ITS FRAME: scaled
                            // about its centre and moved by the pan, both read
                            // here (in the draw phase) so a pinch does not
                            // recompose the page it is magnifying.
                            .graphicsLayer {
                                val z = if (mine) ReaderLook.pdfZoom else 1f
                                scaleX = z
                                scaleY = z
                                // The pan is drawn whenever this page owns the
                                // zoom, and it is zero at 1× — the old
                                // `z > 1.02f` gate dropped a still-2%-scaled
                                // page's pan to nothing and shifted it (v406).
                                translationX = if (mine) ReaderLook.pdfPanX else 0f
                                translationY = if (mine) ReaderLook.pdfPanY else 0f
                            }
                    ) {
                        Image(
                            bitmap = drawn.asImageBitmap(),
                            contentDescription = "Page ${page + 1}",
                            contentScale = ContentScale.Fit,
                            colorFilter = readerPdfFilter(palette.inkKey),
                            modifier = Modifier.fillMaxSize()
                        )
                        words?.let { read ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PdfPageTextLayer(
                                    text = read,
                                    page = page,
                                    bitmapSize = IntSize(drawn.width, drawn.height),
                                    container = container,
                                    palette = palette,
                                    highlights = highlightsFor(marks, page),
                                    selection = selection,
                                    onSelect = onSelect,
                                    onPagePress = { onLongPress(page) }
                                )
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(color = palette.accent)
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
                            color = palette.ink.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                // v399 — AND THE PAGE SAYS ITS OWN NUMBER.
                //
                // A PDF page is a picture of a page, and whatever number is
                // printed on it belongs to the scan: it can be missing, or a
                // roman numeral, or set at a size nobody can read on a phone.
                // The reader's own number sits in the corner, on the paper, out
                // of the way of the words — so "which page am I on" has an
                // answer in the scrolling flow too, where there is no page bar.
                // It is drawn OUTSIDE the zoom, so a magnified page cannot carry
                // its own number off the screen.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.paper.copy(alpha = 0.88f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                ) {
                    Text(
                        "${page + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ink.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
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
    /** v406 — the block this page opens on, reported live. */
    onBlockShown: (Int) -> Unit,
    restoredBlock: Int,
    marks: List<ReaderMarkEntity>,
    palette: ReaderPalette,
    bookId: String,
    document: String,
    onLongPress: (ReaderParagraph) -> Unit,
    onTap: (Offset, IntSize) -> Unit,
    /** v389c — the live selection, when it belongs to this book's text. */
    selection: ReaderSelection?,
    onSelect: (ReaderSelection) -> Unit,
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
                val index = pages.getOrNull(page)?.first ?: return@collectLatest
                // LIVE FIRST (v406): the sheet names the place they are on now;
                // the write below still waits for the turn to settle.
                onBlockShown(index)
                delay(700)
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

    // v394 — PAGES PINCH TOO (user report: "when im in pages i cant pinch to
    // zoom"): the same re-lay-the-type zoom the scroll carries, so a book read
    // as pages answers the two fingers like the same book read as a scroll.
    val pagerZoom = Modifier.pinchToZoom { zoom, _, _ ->
        val next = (ReaderLook.textScale * zoom).coerceIn(0.8f, 2.6f)
        if (next != ReaderLook.textScale) ReaderLook.textScale = next
        Offset.Zero
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .then(pagerZoom)
            .onSizeChanged { room = it }
            .pointerInput(Unit) { detectTapGestures(onTap = { at -> onTap(at, size) }) },
        // v394 — PAGES SIT FLUSH. A gutter between self-made pages read as one
        // book cut into cards (user report: "the pages are not continuosn
        // connected"); with no gap a turn is a slide of the paper itself.
        pageSpacing = 0.dp,
        // v418 — THE NEXT PAGE IS ALREADY LAID OUT. Composing only the visible
        // page meant a turn painted its neighbour from scratch mid-slide, which
        // is the hitch a page turn used to show (member: "smoother page turns").
        beyondViewportPageCount = 1
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
                    highlights = highlightsFor(marks, index),
                    note = note?.note.orEmpty(),
                    bookmarked = bookmark != null,
                    onLongPress = {
                        onLongPress(ReaderParagraph(index, block.section, block.text, block.isHeading))
                    },
                    selection = selection?.takeIf { !it.isPage && it.index == index }
                        ?.let { range -> range.from..range.to },
                    onSelect = { range, text ->
                        onSelect(
                            ReaderSelection(
                                index = index,
                                from = range.first,
                                to = range.last,
                                text = text,
                                isPage = false,
                                section = block.section
                            )
                        )
                    },
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
 * v411 — how long the reader waits before it writes the page it is showing back
 * onto the book's own row (see the page-mark effect in the reader body). Long
 * enough that a flick through a dozen pages is a single write, short enough that
 * leaving the reader right after a page turn has already recorded it.
 */
private const val ReaderPageMarkDebounceMs = 450L

/**
 * v411 — HOLD AN ARROW TO KEEP TURNING.
 *
 * How long the page bar's arrow waits before a press becomes a repeat, and the
 * cadence it repeats at. The wait is just past the long-press threshold so a
 * deliberate single tap never turns two pages; the cadence is fast enough to
 * cross a chapter, slow enough to stop on the page you meant (the pager is
 * asked for ONE more page each tick, and each ask retargets its own animation).
 */
private const val PageTurnHoldDelayMs = 320L
private const val PageTurnHoldRepeatMs = 150L

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
    /** A page asked for from outside — a mark, a chapter, a search find (v399). */
    pendingPage: Int?,
    onPendingPageConsumed: () -> Unit,
    /** Which page the pager settled on, so the bar and the marks can name it. */
    onPageShown: (Int) -> Unit,
    document: String,
    marks: List<ReaderMarkEntity>,
    palette: ReaderPalette,
    bookId: String,
    onOpenedAt: (ReaderMarkEntity?) -> Unit,
    onLongPress: (Int) -> Unit,
    onTap: (Offset, IntSize) -> Unit,
    onScrolled: (Boolean) -> Unit,
    flow: ReaderFlow,
    /** v389c — the live sweep, when it belongs to this page of the file. */
    selection: ReaderSelection?,
    onSelect: (ReaderSelection) -> Unit
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
            // v424 — the surface a tap zone is measured against, and where it
            // starts in the window, so a tap inside a magnified sheet can be
            // said in the screen's own coordinates.
            viewport = surfaceSize,
            surfaceOrigin = surfaceOrigin,
            onScrolled = onScrolled,
            onLongPress = onLongPress,
            selection = selection,
            onSelect = onSelect,
            pendingPage = pendingPage,
            onPendingPageConsumed = onPendingPageConsumed,
            onPageShown = onPageShown
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
                // The chrome names this page at once; the WRITE waits for the
                // swipe to settle, so ten pages passed in one flick save the
                // one the finger stopped on, once.
                onPageShown(index)
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

    // A PAGE ASKED FOR FROM OUTSIDE — a mark in the places sheet, a chapter, a
    // search find, the bar's own arrows. The pager is showing, so it takes the
    // number and clears it (v399).
    LaunchedEffect(pendingPage, pageCount) {
        val at = pendingPage ?: return@LaunchedEffect
        pagerState.scrollToPage(at.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))
        onPendingPageConsumed()
    }

    // A page turned is the member moving through the book: the chrome goes.
    LaunchedEffect(pagerState, onScrolled) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collect { scrolling -> onScrolled(scrolling) }
    }

    // PINCH ZOOMS THE PAGE ITSELF. A PDF page is not reflowable — zooming it has
    // to mean magnifying it, which is also what makes the small print of a
    // scanned document legible on a phone at all (v389).
    //
    // v394 — AND A ZOOMED PAGE STAYS READABLE: one finger pans it (the modifier
    // claims the drag while [zoomed] answers true), and the pager STAYS ON so a
    // swipe at the page's edge still turns it (user report: "i have to minimise
    // it to be able to switch pages").
    //
    // v395 — AND THE PAN STOPS AT THE PAGE'S EDGE (user report: "when im zoomed
    // in and im moving around, it changes page by mistake").
    //
    // The pan used to be added up without a bound, so a magnified page could be
    // dragged clean off the screen (a blank where the page was) and — because
    // the drag was claimed for as long as the finger moved — the pager could
    // never get it back either. The travel is now measured against the page's
    // OWN drawn size at this zoom, which is what tells the modifier when the
    // page has run out of room and the drag belongs to the pager again.
    //
    // The page is drawn INSIDE its box (`ContentScale.Fit`), so the thing being
    // magnified is the letterboxed page and not the box around it — hence the
    // box is kept here (a full-screen pager gives every page the same one) and
    // each page carries the shape of the page drawn in IT, read where that
    // page's own render lands.
    val viewport = remember { mutableStateOf(IntSize.Zero) }
    HorizontalPager(
        state = pagerState,
        // ── v403 — THE PAGE OF THE PAGER OWNS ITS OWN GESTURE ──
        //
        // The pinch and the pan used to be a modifier on the PAGER (`Modifier
        // .fillMaxSize().then(zoomModifier)`), which put the handler OUTSIDE the
        // pager's own scroll detector. In the main pass a deeper node handles an
        // event first, so `scrollable` had already taken the drag — and, with a
        // second finger landing a moment later, had already started the page
        // turn — before the zoom could claim it: the member's "the pages slip
        // when its on side by side pages", a pinch that turns the page it is
        // magnifying. On the PAGE, the page is the child: it claims the drag
        // first (see [pinchToZoom]) and consumes it, and the pager's own slop
        // wait is cancelled by that consumption instead of racing it.
        modifier = Modifier.fillMaxSize(),
        // v418 — keep the neighbouring page ready so a turn never paints from
        // scratch mid-slide (see the reflowable pager above).
        beyondViewportPageCount = 1,
        // v394 — the PDF's pages sit flush too: a scan read as pages is one
        // document being slid across, not a stack of cards with gaps between.
        pageSpacing = 0.dp
    ) { page ->
        // ── v419 — A TURN HAS MOTION ─────────────────────────────────────
        //
        // The PDF's pages sat FLUSH and full-bleed, so a swipe was two stills
        // swapping with nothing travelling between them (member: "add page-turn
        // motion polish to the PDF pager (a subtle slide/curl)"). Each page now
        // reads its own distance from the settle point and slides, tilts and
        // eases back as the turn runs — the outgoing page trails a little behind
        // and shrinks, the incoming one rises to meet the finger. It is a DRAW
        // transform only: layout, the pinch and the marks are untouched. The
        // offset is read INSIDE the layer lambda (see below), never in
        // composition, so a swipe invalidates the layer instead of recomposing
        // every page on every frame.
        val bitmap by produceState<Bitmap?>(null, document, page) {
            value = withContext(Dispatchers.IO) {
                runCatching { renderPdfPage(context, document, page) }.getOrNull()
            }
        }
        // THE SHAPE OF THIS PAGE (v403), read from its own render: it is what
        // the pan is measured against, and it used to be reported only by the
        // page being looked at (a single shared value re-written on every turn),
        // which left a neighbour's pinch measuring against the page just left
        // behind. A page's own shape is its own.
        var myAspect by remember(page) { mutableStateOf(0f) }
        LaunchedEffect(page, bitmap) {
            val drawn = bitmap
            if (drawn != null && drawn.height > 0) {
                myAspect = drawn.width.toFloat() / drawn.height.toFloat()
            }
        }
        // ── THE PAGE'S OWN WORDS (v389c) ───────────────────────────────
        //
        // Read for the page being looked at, and for any page already wearing a
        // mark (a highlight has to be drawn on its words even while the page it
        // belongs to is only a swipe away). Null means this page has NO text
        // layer — a scan — and the long press below then says exactly what it
        // always said: mark this page.
        var words by remember(document, page) { mutableStateOf<PdfPageText?>(null) }
        var container by remember { mutableStateOf(IntSize.Zero) }
        val wanted = page == pagerState.currentPage || highlightsFor(marks, page).isNotEmpty()
        LaunchedEffect(document, page, wanted) {
            if (!wanted || words != null) return@LaunchedEffect
            words = withContext(Dispatchers.IO) {
                runCatching { extractPdfPageText(context, document, page) }.getOrNull()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                // ── v419 — THE PAGE-TURN LAYER ───────────────────────────
                // Kept deliberately SMALL so a turn reads as paper being carried
                // across, not a spinning card: a tenth of a width of slide, a
                // 9° tilt, a 4.5% shrink and a light fade at the far edge, hung
                // on the OUTER edge of the moving page (`transformOrigin`) and
                // softened by a long camera so the tilt has no sharp curl.
                .graphicsLayer {
                    val off = (
                        (pagerState.currentPage - page) +
                            pagerState.currentPageOffsetFraction
                        ).coerceIn(-1f, 1f)
                    val away = abs(off)
                    translationX = -off * size.width * 0.10f
                    scaleX = 1f - away * 0.045f
                    scaleY = 1f - away * 0.045f
                    rotationY = off * 9f
                    alpha = 1f - away * 0.22f
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (off >= 0f) 0f else 1f,
                        pivotFractionY = 0.5f
                    )
                    cameraDistance = 24f * density
                }
                .onSizeChanged {
                    container = it
                    // The box being magnified is exactly what the pager gives
                    // each page — read here, where the page is laid out.
                    viewport.value = it
                }
                // ── THE PAGE'S OWN GESTURE (v403) ────────────────────────
                //
                // Armed AFTER the box is measured and keyed on the page's SHAPE
                // as well as its number: the handler outlives the composition
                // that armed it, so a pinch has to measure the page it actually
                // sees rather than the page as it was before its render landed.
                // It comes BEFORE the tap handler below, so the drag is claimed
                // ahead of the pager's own scroll (see the pager's comment).
                .pinchToZoom(
                    key = page to myAspect,
                    zoomed = { ReaderLook.pdfZoomPage == page && ReaderLook.pdfZoom > 1.02f }
                ) { zoom, drag, focus ->
                    // One rule for both surfaces: see [readerZoomThisPage].
                    readerZoomThisPage(page, viewport.value, myAspect, zoom, drag, focus)
                }
                .pointerInput(page, myAspect) {
                    detectTapGestures(
                        onTap = { at -> onTap(at, size) },
                        // v399 — A DOUBLE TAP IS THE PINCH, AT ONE POINT: in to
                        // read a line closely, out to see the page whole again.
                        // It is anchored where it was tapped, like the pinch is,
                        // and it uses the same rule — so the page grows about
                        // the word the finger asked about.
                        onDoubleTap = { at ->
                            readerDoubleTapZoom(page, viewport.value, myAspect, at)
                        },
                        // Held words are the sweep's: the layer below answers
                        // the press, and this one only stands in where the page
                        // has nothing to select (see PdfPageTextLayer).
                        onLongPress = { if (words == null) onLongPress(page) }
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
                // ── AND ONLY THE PAGE THAT OWNS THE ZOOM WEARS IT (v403) ──
                //
                // The pager's zoom used to be one number applied to EVERY page
                // (a leftover from when the frame was the screen rather than a
                // page), so turning the page carried the magnification onto a
                // page nobody had pinched. A zoom belongs to the page it was
                // asked for; its neighbours draw at 1×.
                //
                // WHOSE ZOOM THIS IS is read in the COMPOSITION and used in the
                // draw lambda, which is the same split the column uses: the
                // owner changes only when a pinch starts or ends on a page (one
                // recomposition), while the scale and the pan are read live, so
                // a pinch redraws a page instead of recomposing the pager.
                val mine = ReaderLook.pdfZoomPage == page
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val z = if (mine) ReaderLook.pdfZoom else 1f
                            scaleX = z
                            scaleY = z
                            // Zero at 1× by construction; see the column's own
                            // layer (v406).
                            translationX = if (mine) ReaderLook.pdfPanX else 0f
                            translationY = if (mine) ReaderLook.pdfPanY else 0f
                        }
                ) {
                    Image(
                        bitmap = drawn.asImageBitmap(),
                        contentDescription = "Page ${page + 1}",
                        contentScale = ContentScale.Fit,
                        colorFilter = readerPdfFilter(palette.inkKey),
                        modifier = Modifier.fillMaxSize()
                    )
                    // THE WORDS SIT EXACTLY OVER THE PAGE THEY BELONG TO, and
                    // INSIDE the zoom layer — so a magnified page carries its
                    // marks and its sweep at the magnification the member chose.
                    words?.let { read ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PdfPageTextLayer(
                                text = read,
                                page = page,
                                bitmapSize = IntSize(drawn.width, drawn.height),
                                container = container,
                                palette = palette,
                                highlights = highlightsFor(marks, page),
                                selection = selection,
                                onSelect = onSelect,
                                onPagePress = { onLongPress(page) }
                            )
                        }
                    }
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
                        color = palette.ink.copy(alpha = 0.75f),
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
    highlights: List<ReaderPassage>,
    note: String,
    bookmarked: Boolean,
    onLongPress: () -> Unit,
    /** The live selection, as character offsets into THIS block's text. */
    selection: IntRange?,
    /** A sweep of the words: the range and the words themselves. */
    onSelect: (IntRange, String) -> Unit,
    query: String,
    hitHere: Boolean,
    hitLength: Int
) {
    // A PAGE-ANCHOR BLOCK (v389c) IS A TARGET, NOT A LINE.
    //
    // The book's own page-break markers arrive as blocks of their own so the
    // page-list has something to land on — and they draw nothing at all, which
    // is the whole point: a marker is a bookmark in the text, not a thing in the
    // text. (Without this the row would still take its own padding, and a page
    // break would look like a gap in the paragraph.)
    if (block.text.isBlank() && block.imagePath == null) return
    val marked = highlights.isNotEmpty()
    val firstInk = highlights.firstOrNull()?.ink ?: Color.Transparent
    // The words are laid out by Compose, so Compose is what can say which
    // CHARACTER a finger landed on — the one thing needed to select text inside
    // a paragraph rather than marking the whole of it.
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // Where the sweep began, so a drag extends from the word it started on
    // instead of chasing the finger's own character.
    var anchorWord by remember(block.text) { mutableStateOf<IntRange?>(null) }
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
            .then(
                if (marked) {
                    Modifier
                        // A highlight is drawn DOWN THE SIDE of the paragraph it
                        // lives in: the rule says "a passage here", and the wash
                        // on the words themselves is drawn by the text spans
                        // below (which is what lets it cover ONE RUN of a
                        // paragraph rather than the whole of it).
                        .drawBehind {
                            val bar = 3.dp.toPx()
                            drawRoundRect(
                                color = firstInk,
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
        // ── WHAT THE PARAGRAPH LOOKS LIKE (v389c) ──────────────────────────
        //
        // Three things can wash the words and they are not exclusive: every
        // stored HIGHLIGHT (in its own ink, over exactly the run it was made on),
        // the LIVE SELECTION (in the page's accent, while the member is choosing),
        // and every SEARCH FIND (the wash and a heavier weight). So the runs are
        // worked out from the spans themselves — each stretch of characters wears
        // every span that covers it — instead of one `if` picking a winner.
        val shown = remember(block.text, block.emphasis, query, hitHere, highlights, selection) {
            val needle = query.trim()
            val spans = ArrayList<ReaderTextSpan>()
            // v398 — the BOOK's OWN emphasis goes in first: it is the paragraph's
            // own type, and every wash added below (a highlight, the live
            // selection, a find) paints over it rather than under it.
            block.emphasis.forEach { run ->
                spans.add(
                    ReaderTextSpan(
                        run.start.coerceIn(0, block.text.length),
                        run.end.coerceIn(0, block.text.length),
                        SpanStyle(
                            fontWeight = if (run.bold) FontWeight.SemiBold else null,
                            fontStyle = if (run.italic) FontStyle.Italic else null
                        )
                    )
                )
            }
            highlights.forEach { passage ->
                val at = if (passage.text.isBlank()) -1 else block.text.indexOf(passage.text)
                if (at >= 0) {
                    spans.add(
                        ReaderTextSpan(
                            at,
                            at + passage.text.length,
                            SpanStyle(background = passage.ink.copy(alpha = 0.30f))
                        )
                    )
                } else if (passage.text.isBlank()) {
                    // A mark whose words could not be found (the file changed
                    // under it) still has to say WHERE it was: the whole block.
                    spans.add(
                        ReaderTextSpan(0, block.text.length, SpanStyle(background = passage.ink.copy(alpha = 0.18f)))
                    )
                }
            }
            if (hitHere && needle.isNotEmpty()) {
                var from = 0
                while (from <= block.text.length) {
                    val at = block.text.indexOf(needle, from, ignoreCase = true)
                    if (at < 0) break
                    spans.add(
                        ReaderTextSpan(
                            at,
                            at + needle.length,
                            SpanStyle(
                                background = palette.accent.copy(alpha = 0.32f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    )
                    from = at + needle.length
                }
            }
            selection?.let { range ->
                val from = range.first.coerceIn(0, block.text.length)
                val to = (range.last + 1).coerceIn(from, block.text.length)
                if (to > from) {
                    spans.add(
                        ReaderTextSpan(
                            from,
                            to,
                            SpanStyle(background = palette.accent.copy(alpha = 0.38f))
                        )
                    )
                }
            }
            annotatedWithSpans(block.text, spans)
        }
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = shown,
                style = body,
                onTextLayout = { result -> layout = result },
                modifier = Modifier
                    .weight(1f)
                    // A LONG PRESS SELECTS THE WORD UNDER THE FINGER, and the
                    // drag extends it — the standard reading gesture, and the
                    // only way to mark a passage rather than a whole paragraph.
                    // A plain TAP is deliberately not handled here: it falls
                    // through to the reader's own detector, which is what has
                    // always brought the chrome back.
                    .pointerInput(block.text) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { start ->
                                val result = layout ?: return@detectDragGesturesAfterLongPress
                                // v389d — an EMPTY block (an image with no caption)
                                // has no offset to ask for and no word to bound, and
                                // asking anyway threw out of the gesture.
                                if (block.text.isEmpty()) {
                                    return@detectDragGesturesAfterLongPress
                                }
                                val last = block.text.length - 1
                                val at = result.getOffsetForPosition(start).coerceIn(0, last)
                                val word = result.getWordBoundary(at)
                                val range = word.start.coerceIn(0, last)..
                                    (word.end - 1).coerceIn(0, last)
                                anchorWord = range
                                onSelect(range, block.text.substring(range.first, range.last + 1))
                            },
                            onDrag = { change, _ ->
                                val result = layout ?: return@detectDragGesturesAfterLongPress
                                val anchor = anchorWord ?: return@detectDragGesturesAfterLongPress
                                if (block.text.isEmpty()) {
                                    return@detectDragGesturesAfterLongPress
                                }
                                val last = block.text.length - 1
                                val at = result.getOffsetForPosition(change.position)
                                    .coerceIn(0, last)
                                val word = result.getWordBoundary(at)
                                val end = (word.end - 1).coerceIn(0, last)
                                val range = if (word.start < anchor.first) {
                                    word.start.coerceIn(0, last)..anchor.last.coerceAtMost(last)
                                } else {
                                    anchor.first.coerceAtMost(last)..maxOf(end, anchor.last)
                                }
                                onSelect(range, block.text.substring(range.first, range.last + 1))
                            },
                            onDragEnd = { }
                        )
                    }
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
    onPlaces: () -> Unit,
    pageBar: ReaderPageBar?,
    flowLabel: String,
    onToggleFlow: () -> Unit,
    /** v422/v424 — the page's own tap zones, and the switch that governs them. */
    tapZones: Boolean,
    onToggleTapZones: () -> Unit,
    /** v424 — a HOLD on that switch opens the editor of where the zones sit. */
    onEditTapZones: () -> Unit
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
                            // v411 — HOLD TO KEEP GOING (member: "add holding
                            // the arrow for faster page forward"). A tap still
                            // turns exactly one page; holding the arrow turns
                            // them on a cadence until the finger lifts.
                            ReaderHoldButton(
                                glyph = CurioIcons.ChevronLeft,
                                label = "The page before",
                                palette = palette,
                                step = pageBar.onPrev
                            )
                            // v422 — a bar with no page to name is the two
                            // arrows and nothing else.
                            if (pageBar.label.isNotBlank()) {
                                Text(
                                    pageBar.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = palette.ink.copy(alpha = 0.8f)
                                )
                            }
                            ReaderHoldButton(
                                glyph = CurioIcons.ChevronRight,
                                label = "The next page",
                                palette = palette,
                                step = pageBar.onNext
                            )
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
                // page, so it sits where the thumb already is. v394 — and it is
                // an ICON now: the button names the OTHER way of reading (a
                // book while you scroll, a stack of layers while you turn
                // pages) instead of a word at the foot of the page (user
                // request: "instead of scrolling and pages text show it as
                // icon").
                Surface(
                    onClick = onToggleFlow,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CurioIcon(
                            if (flowLabel == ReaderFlow.PAGED.label) CurioIcons.Layers
                            else CurioIcons.MenuBook,
                            if (flowLabel == ReaderFlow.PAGED.label) "Read as one scroll"
                            else "Read as pages",
                            tint = palette.ink.copy(alpha = 0.75f),
                            size = 19.dp
                        )
                    }
                }
                // ── v422 — AND THE PAGE'S OWN TAP ZONES ──────────────
                //
                // On, a tap near an edge of the screen acts and everything else
                // brings the chrome back; off, every tap is the one that brings
                // it back. It wears the accent while it is on, because a switch
                // whose state cannot be read from the page it governs is a
                // switch nobody flips (see [readerZoneActionAt]).
                //
                // v424 — AND A HOLD OPENS WHERE THEY SIT.
                //
                // An edge is a preference — how deep it reaches, and what its
                // tap asks for — and a preference belongs on the surface it
                // changes, not three screens away in Settings: holding this
                // switch puts the zones' own lines on the page to be dragged
                // (see [ReaderTapZoneEditor]).
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleTapZones, onLongClick = onEditTapZones),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        CurioIcons.Crop,
                        if (tapZones) "Tap zones on — hold to place them"
                        else "Tap zones off — hold to place them",
                        tint = if (tapZones) palette.accent else palette.ink.copy(alpha = 0.75f),
                        size = 19.dp
                    )
                }
                // WHERE THEY ARE, as a fact about the book rather than a
                // sentence of advice: a reader does not need to be told to hold
                // a passage, they need to know which chapter they are in.
                if (positionLabel.isNotBlank()) {
                    Text(
                        positionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ink.copy(alpha = 0.75f),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                ReaderChromeButton(CurioIcons.Search, "Search this book", palette) { onSearch() }
                // v394 — THE INK IS A COLOUR, SO IT WEARS THE PALETTE. The old
                // text_fields glyph read as "Tt" — a type-size tool — and the
                // member tapped it expecting larger words (user report: "for the
                // book backgroud color it shows tt as icon which is wrong").
                ReaderChromeButton(CurioIcons.Palette, "The page's ink", palette) { onInk() }
                // v395 — ONE DOOR FOR THE BOOK'S PLACES (user request: "they are
                // not merged in one page and one button"). Where you stopped,
                // what you kept and where you can go were three answers behind
                // two glyphs — a bookmark and a menu book — so a reader looking
                // for "where was I" had to guess which one to open. One glyph,
                // one sheet, one scroll (see [ReaderPlacesSheet]).
                ReaderChromeButton(
                    CurioIcons.Bookmark,
                    "Bookmarks, progress and contents",
                    palette
                ) { onPlaces() }
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

/**
 * v411 — A PAGE-BAR ARROW THAT KEEPS GOING WHILE IT IS HELD.
 *
 * A plain tap turns one page (exactly like [ReaderChromeButton]); pressing and
 * holding past [PageTurnHoldDelayMs] starts a repeat on
 * [PageTurnHoldRepeatMs] until the finger lifts, which is the difference
 * between flipping to the next page and getting through a chapter.
 *
 * The press gesture owns the whole thing rather than sitting beside a
 * `Surface(onClick)`: one detector means one code path for "was this a tap or
 * a hold", so a hold can never ALSO fire the tap that ended it. The repeat
 * runs in the composition's own scope, so it dies with the bar.
 */
@Composable
private fun ReaderHoldButton(
    glyph: String,
    label: String,
    palette: ReaderPalette,
    step: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repeater = rememberCoroutineScope()
    // Plain flag (not Compose state): nothing in composition reads it, it only
    // tells the trailing tap whether the hold already did the work.
    val held = remember { booleanArrayOf(false) }
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
            .size(38.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        held[0] = false
                        val job = repeater.launch {
                            delay(PageTurnHoldDelayMs)
                            held[0] = true
                            while (true) {
                                step()
                                delay(PageTurnHoldRepeatMs)
                            }
                        }
                        // Suspends until the finger lifts (or the gesture is
                        // cancelled) — which is what stops the metronome.
                        tryAwaitRelease()
                        job.cancel()
                    },
                    onTap = { if (!held[0]) step() }
                )
            }
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
                        tint = palette.ink.copy(alpha = 0.75f),
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
                    color = palette.ink.copy(alpha = 0.75f)
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
    /** v406 — whether this book has a type size to set (a reflowable one does). */
    showType: Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ReaderSheetFrame("The page", palette, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

            // ── THE TYPE SIZE, AS A SLIDER (v406) ─────────────────────
            //
            // The pinch already sets the words' size, but a pinch is a guess:
            // the member cannot see the number, and a big change takes several
            // of them (member's request: "add horizontal mode in pdf and epub
            // reader with text size slider for epub"). This is the same value
            // the pinch writes ([ReaderLook.textScale]) said out loud, with a
            // thumb that can be dragged to it exactly. A book read as pages
            // re-lays itself out from the same number, so one slider serves
            // both flows.
            if (showType) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "TYPE SIZE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = palette.accent,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${(ReaderLook.textScale * 100f).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = palette.ink.copy(alpha = 0.75f)
                        )
                    }
                    Slider(
                        value = ReaderLook.textScale,
                        onValueChange = { next ->
                            ReaderLook.textScale = next.coerceIn(0.8f, 2.6f)
                        },
                        valueRange = 0.8f..2.6f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.ink.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            // ── HOW THE PAGE STANDS (v406 → v418) ───────────────────
            //
            // A three-way choice now, offered for EVERY book: Auto follows the
            // phone, Upright and Wide hold the page while the book is open (see
            // [ReaderLook.orientation]). v406 offered a lone switch and only to
            // a PDF, which is why the member found no auto-rotation in either
            // format.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "AUTO-ROTATE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = palette.accent
                )
                Text(
                    ReaderLook.orientation.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.75f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderOrientation.entries.forEach { option ->
                        val active = option == ReaderLook.orientation
                        Surface(
                            onClick = { ReaderLook.orientation = option },
                            shape = RoundedCornerShape(50),
                            color = if (active) palette.accent else palette.ink.copy(alpha = 0.08f)
                        ) {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (active) palette.paper else palette.ink.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderPlacesSheet(
    marks: List<ReaderMarkEntity>,
    position: ReaderMarkEntity?,
    /** v406 — where the member is RIGHT NOW, so the card reads live. */
    live: ReaderLivePlace?,
    content: ReaderContent?,
    chapters: List<ReaderOutlineEntry>,
    pages: List<ReaderOutlineEntry>,
    palette: ReaderPalette,
    onJump: (ReaderMarkEntity) -> Unit,
    onDelete: (ReaderMarkEntity) -> Unit,
    onPickBlock: (Int) -> Unit,
    onPickPage: (Int) -> Unit,
    onBookmarkHere: (ReaderParagraph) -> Unit,
    /** v406 — carry on from the LIVE place, which the stored mark can trail. */
    onContinueAt: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // ── ONE PAGE, ONE SCROLL, ONE ANSWER (v395) ──────────────────────
    //
    // This sheet used to be two HALVES behind two chips — "Bookmarks &
    // progress" and "Contents" — so half of it was always hidden, the two
    // chrome doors opened the same surface on a different tab, and a reader had
    // to guess which door led to "where was I" (user report: "they are not
    // merged in one page and one button"). A reader's three questions have an
    // order — where am I, what did I keep, what is left — so the sheet ANSWERS
    // them in that order and scrolls: the progress card, then the marks, then
    // the book's own contents. Nothing is behind a chip, and every chapter row
    // carries its own bookmark (see [ReaderContentsSection]).
    // IN THE BOOK'S OWN ORDER: a reader's marks are an index to the book, so
    // they read the way the book reads — from where it starts to where it ends —
    // and not in whatever order the table happens to hand them over (v399).
    val kept = marks.filter { !it.isPosition }.sortedBy { it.positionIndex }
    // ── LIVE WHERE POSSIBLE (v406) ──
    // The card follows the reading; the stored auto-bookmark is only the
    // fallback for a book no surface has reported a place in yet. `atIndex` is
    // the one place both the figure and the "mark my place" button agree on.
    val atIndex = live?.index ?: position?.positionIndex
    val atTotal = live?.total
    val through = live?.fraction?.coerceIn(0f, 1f)
        ?: position?.positionFraction?.coerceIn(0f, 1f)
        ?: 0f
    val placeTitle = readerPlaceTitle(content, chapters, position)
    val countLabel = when (val loaded = content) {
        is ReaderContent.Pages -> {
            val total = atTotal ?: loaded.pageCount
            atIndex?.let { "Page ${(it + 1).coerceIn(1, total)} of $total" }.orEmpty()
        }

        is ReaderContent.Text -> {
            val total = atTotal ?: loaded.blocks.size
            atIndex?.let { "Section ${(it + 1).coerceIn(1, total)} of $total" }.orEmpty()
        }

        null -> ""
    }
    // One bookmark per place, so "mark my place" can also take it back.
    val placeMark = atIndex?.let { index ->
        kept.firstOrNull {
            it.positionIndex == index && it.markKind == ReaderMarkKind.BOOKMARK
        }
    }
    ReaderSheetFrame("Places in this book", palette, onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 470.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReaderProgressCard(
                through = through,
                placeTitle = placeTitle,
                countLabel = countLabel,
                hasPlace = atIndex != null,
                marked = placeMark != null,
                palette = palette,
                onContinue = {
                    val index = live?.index
                    if (index != null) onContinueAt(index) else position?.let(onJump)
                },
                onMarkPlace = {
                    if (placeMark != null) {
                        onDelete(placeMark)
                    } else {
                        onBookmarkHere(
                            ReaderParagraph(
                                positionIndex = atIndex ?: 0,
                                section = 0,
                                text = placeTitle.ifBlank { countLabel }.ifBlank { "My place" },
                                isHeading = true,
                                isPage = content is ReaderContent.Pages
                            )
                        )
                    }
                }
            )

            ReaderPlacesHeader(
                label = "YOUR MARKS",
                trailing = if (kept.isEmpty()) "" else "${kept.size}",
                palette = palette
            )
            ReaderMarksSection(
                marks = kept,
                content = content,
                chapters = chapters,
                palette = palette,
                onJump = onJump,
                onDelete = onDelete
            )

            // The auto-bookmark is not one of the marks — it is the progress
            // card above — so the contents only appear when the file has an
            // order to show. A PDF with no outline still gets its pages, and
            // that IS its contents.
            ReaderPlacesHeader(
                label = "CONTENTS",
                trailing = if (chapters.isEmpty()) "" else "${chapters.size}",
                palette = palette
            )
            ReaderContentsSection(
                content = content,
                chapters = chapters,
                pages = pages,
                marks = kept,
                palette = palette,
                atIndex = atIndex,
                onPickBlock = onPickBlock,
                onPickPage = onPickPage,
                onBookmarkHere = onBookmarkHere,
                onUnbookmark = onDelete
            )
        }
    }
}

/**
 * WHERE THE READER IS, in the book's own words: the chapter heading the auto
 * bookmark sits under for a reflowed book, the outline entry it has passed for
 * a PDF. Blank when the file says nothing about where the member is — the
 * sheet then shows the page or section figure and no name it would have to
 * invent.
 */
private fun readerPlaceTitle(
    content: ReaderContent?,
    chapters: List<ReaderOutlineEntry>,
    position: ReaderMarkEntity?
): String {
    val at = position?.positionIndex ?: return ""
    return when (content) {
        is ReaderContent.Pages -> chapters
            .lastOrNull { it.isPage && it.page <= at + 1 }
            ?.title
            .orEmpty()

        is ReaderContent.Text -> content.blocks.getOrNull(at)?.sectionTitle.orEmpty()
        null -> ""
    }
}

/** The bookmark a contents row already carries, if it has one. A chapter and a
 *  page both mark a PLACE (an index), so the index is what identifies it. */
private fun readerContentsBookmark(
    marks: List<ReaderMarkEntity>,
    entry: ReaderOutlineEntry
): ReaderMarkEntity? {
    val at = if (entry.isPage) entry.page - 1 else entry.block
    if (at < 0) return null
    return marks.firstOrNull {
        !it.isPosition && it.markKind == ReaderMarkKind.BOOKMARK && it.positionIndex == at
    }
}

/** The passage a contents row marks: a chapter's own opening, or a PDF's page. */
private fun readerParagraphFor(entry: ReaderOutlineEntry): ReaderParagraph {
    val at = if (entry.isPage) (entry.page - 1).coerceAtLeast(0) else entry.block
    return ReaderParagraph(
        positionIndex = at,
        section = if (entry.isPage) at + 1 else 0,
        text = entry.title,
        isHeading = true,
        isPage = entry.isPage
    )
}

/** A section's name, on a hairline that runs to the sheet's edge — the one
 *  piece of furniture that lets three lists share a page without chips. */
@Composable
private fun ReaderPlacesHeader(label: String, trailing: String, palette: ReaderPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = palette.accent
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(palette.ink.copy(alpha = 0.10f))
        )
        if (trailing.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                trailing,
                style = MaterialTheme.typography.labelSmall,
                color = palette.ink.copy(alpha = 0.45f)
            )
        }
    }
}

/**
 * HOW FAR THROUGH, as the top of the sheet rather than a row in a list.
 *
 * The figure, the rail it is filled along, the place it belongs to and the two
 * things a reader does with it — carry on, or keep the place. It replaced a
 * "Last read \u00b7 auto" list row that said the same number in a smaller voice and
 * could only be tapped, never acted on.
 */
@Composable
private fun ReaderProgressCard(
    through: Float,
    placeTitle: String,
    countLabel: String,
    hasPlace: Boolean,
    marked: Boolean,
    palette: ReaderPalette,
    onContinue: () -> Unit,
    onMarkPlace: () -> Unit
) {
    val figure = (through.coerceIn(0f, 1f) * 100f).roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "YOUR PLACE IN THIS BOOK",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = palette.accent,
                modifier = Modifier.weight(1f)
            )
            if (countLabel.isNotBlank()) {
                Text(
                    countLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.ink.copy(alpha = 0.45f)
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$figure",
                style = TextStyle(
                    fontFamily = FrauncesFontFamily,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.ink
                )
            )
            Text(
                "%",
                style = TextStyle(
                    fontFamily = FrauncesFontFamily,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent
                ),
                modifier = Modifier.padding(start = 1.dp, bottom = 5.dp)
            )
            Text(
                "through",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.75f),
                modifier = Modifier.padding(start = 7.dp, bottom = 6.dp)
            )
        }
        // The rail. A bar that only fills is a number drawn twice; the bead at
        // its head is where the finger would put it if the page were a slider,
        // which is what makes the figure read as PROGRESS rather than as a
        // statistic.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(50))
                .background(palette.ink.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(through.coerceIn(0.04f, 1f))
                    .height(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(palette.accent.copy(alpha = 0.55f), palette.accent)
                        )
                    )
            )
        }
        if (placeTitle.isNotBlank()) {
            Text(
                placeTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink.copy(alpha = 0.8f),
                maxLines = 2
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasPlace) {
                ReaderPlaceAction(
                    label = "Continue reading",
                    container = palette.accent,
                    ink = palette.paper,
                    onClick = onContinue
                )
            }
            ReaderPlaceAction(
                label = if (marked) "Bookmarked" else "Mark my place",
                container = if (marked) palette.accent else palette.ink.copy(alpha = 0.10f),
                ink = if (marked) palette.paper else palette.ink.copy(alpha = 0.8f),
                onClick = onMarkPlace
            )
        }
    }
}

@Composable
private fun ReaderPlaceAction(
    label: String,
    container: Color,
    ink: Color,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = container) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ink,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/** The marks: every bookmark, highlight and note the book carries. */
@Composable
private fun ReaderMarksSection(
    marks: List<ReaderMarkEntity>,
    content: ReaderContent?,
    chapters: List<ReaderOutlineEntry>,
    palette: ReaderPalette,
    onJump: (ReaderMarkEntity) -> Unit,
    onDelete: (ReaderMarkEntity) -> Unit
) {
    if (marks.isEmpty()) {
        Text(
            "Nothing marked yet \u2014 hold a passage while you read.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.75f)
        )
    }
    val paged = content is ReaderContent.Pages
    marks.forEach { mark ->
        ReaderMarkRow(
            mark = mark,
            place = readerMarkPlace(content, mark),
            where = readerMarkChapter(content, chapters, mark),
            made = readerMarkWhen(mark.createdAtMillis),
            // A PDF bookmark's own text IS its page ("Page 42"), which the line
            // above already says — quoting it back would be the sheet talking to
            // itself. A highlight or a note always has words of its own.
            snippet = if (paged && mark.markKind == ReaderMarkKind.BOOKMARK) "" else mark.text,
            palette = palette,
            onJump = { onJump(mark) },
            onDelete = { onDelete(mark) }
        )
    }
}

/**
 * WHERE A MARK IS, in the book's own terms.
 *
 * v399 — IT SAYS PAGE, NOT SECTION. The row used to fall back to
 * "Section ${'$'}{index + 1}" — the reader's own block numbering, which is a
 * fact about how the FILE was split and not about the book (user report:
 * "theyre not marked as page numbers are they?"). A PDF's places are PAGES, a
 * reflowed book's are chapters when the file numbers them and sections when it
 * does not.
 */
private fun readerMarkPlace(content: ReaderContent?, mark: ReaderMarkEntity): String {
    val at = mark.positionIndex.coerceAtLeast(0)
    return when (content) {
        is ReaderContent.Pages -> "Page ${(at + 1).coerceAtMost(content.pageCount)}"
        is ReaderContent.Text -> if (mark.chapter > 0) "Chapter ${mark.chapter}"
        else "Section ${(at + 1).coerceAtMost(content.blocks.size.coerceAtLeast(1))}"

        null -> if (mark.chapter > 0) "Chapter ${mark.chapter}" else "Section ${at + 1}"
    }
}

/** The chapter or outline entry a mark sits under — the book's own words for
 *  the place, blank when the file says nothing about it. */
private fun readerMarkChapter(
    content: ReaderContent?,
    chapters: List<ReaderOutlineEntry>,
    mark: ReaderMarkEntity
): String = when (content) {
    is ReaderContent.Pages -> chapters
        .lastOrNull { it.isPage && it.page <= mark.positionIndex + 1 }
        ?.title
        .orEmpty()

    is ReaderContent.Text -> content.blocks.getOrNull(mark.positionIndex)?.sectionTitle.orEmpty()
    null -> ""
}

/** WHEN a mark was made, in the reader's own shorthand: today, yesterday, or a
 *  date small enough to sit in the corner of the row. */
private fun readerMarkWhen(millis: Long): String {
    if (millis <= 0L) return ""
    return runCatching {
        val day = millis.toLocalDate()
        val today = System.currentTimeMillis().toLocalDate()
        when (day) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> day.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
        }
    }.getOrDefault("")
}

/**
 * The book's own chapters and printed pages, each row a way to GO there and
 * each wearing its own bookmark.
 *
 * v395 — THE PILL IS A TOGGLE, AND IT SAYS WHICH WAY IT IS (user report: "it
 * also doesnt have bookmark option for contents"). The contents list could
 * only ever ADD a bookmark: the pill was always the same quiet grey, so a
 * chapter that was already bookmarked looked exactly like one that was not and
 * tapping it again simply re-marked the same place — which is why the
 * contents and the bookmarks read as two different lists about two different
 * books. A bookmarked row's pill is filled in the reader's accent and takes it
 * back; a PDF's page rows carry one too, because a scanned book has no chapter
 * names to mark instead.
 */
@Composable
private fun ReaderContentsSection(
    content: ReaderContent?,
    chapters: List<ReaderOutlineEntry>,
    pages: List<ReaderOutlineEntry>,
    marks: List<ReaderMarkEntity>,
    palette: ReaderPalette,
    /** v418 — where the reader is RIGHT NOW, so the row they are in is lit. */
    atIndex: Int?,
    onPickBlock: (Int) -> Unit,
    onPickPage: (Int) -> Unit,
    onBookmarkHere: (ReaderParagraph) -> Unit,
    onUnbookmark: (ReaderMarkEntity) -> Unit
) {
    var showingPages by remember { mutableStateOf(false) }
    val entries = if (showingPages) pages else chapters
    // ── v418 — THE CHAPTER YOU ARE IN IS LIT ───────────────────────────
    // The contents used to read as a flat list however far in the member was;
    // the row they are reading in now wears a tint of the accent (member:
    // "better chapter & outline handling"). The entry is the LAST one at or
    // before the live place, which is the chapter that place sits inside.
    val currentEntry = atIndex?.let { key ->
        entries.lastOrNull { entry ->
            val at = if (entry.isPage) entry.page - 1 else entry.block
            at in 0..key
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (pages.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(false to "Chapters", true to "Printed pages").forEach { (asPages, label) ->
                    val on = asPages == showingPages
                    Surface(
                        onClick = { showingPages = asPages },
                        shape = RoundedCornerShape(50),
                        color = if (on) palette.accent else palette.ink.copy(alpha = 0.08f)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (on) palette.paper else palette.ink.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
        when {
            entries.isNotEmpty() -> entries.forEach { entry ->
                val openable = entry.block >= 0 || entry.isPage
                val bookmark = readerContentsBookmark(marks, entry)
                val isCurrent = entry === currentEntry
                Surface(
                    onClick = {
                        if (entry.isPage) onPickPage(entry.page) else onPickBlock(entry.block)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCurrent) palette.accent.copy(alpha = 0.14f) else palette.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = (14 + (entry.depth - 1) * 18).dp,
                            end = 12.dp,
                            top = if (entry.depth <= 1) 12.dp else 9.dp,
                            bottom = if (entry.depth <= 1) 12.dp else 9.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
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
                                color = if (isCurrent) palette.accent else palette.ink
                            ),
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                        // v394 — A BOOKMARK PER ROW. The pill lands the mark
                        // on the chapter's own opening, so the marks list later
                        // reads the chapter's name — without opening the reader,
                        // finding the heading and holding it.
                        if (openable) {
                            Surface(
                                onClick = {
                                    if (bookmark != null) onUnbookmark(bookmark)
                                    else onBookmarkHere(readerParagraphFor(entry))
                                },
                                shape = RoundedCornerShape(50),
                                color = if (bookmark != null) palette.accent
                                else palette.ink.copy(alpha = 0.07f)
                            ) {
                                Box(Modifier.padding(4.dp)) {
                                    CurioIcon(
                                        CurioIcons.Bookmark,
                                        if (bookmark != null) "Remove this bookmark"
                                        else "Bookmark this chapter",
                                        tint = if (bookmark != null) palette.paper
                                        else palette.ink.copy(alpha = 0.75f),
                                        size = 14.dp
                                    )
                                }
                            }
                        }
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
                    color = palette.ink.copy(alpha = 0.75f)
                )
                val chunks = (0 until content.pageCount).chunked(4)
                chunks.take(60).forEachIndexed { row, pageRow ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pageRow.forEach { page ->
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
                        if (pageRow.size < 4) {
                            Spacer(Modifier.weight((4 - pageRow.size).toFloat()))
                        }
                    }
                    if (row == 59) {
                        Text(
                            "More in the file itself",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.ink.copy(alpha = 0.75f)
                        )
                    }
                }
            }
            content is ReaderContent.Text -> Text(
                "This file has no chapter headings of its own.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.75f)
            )
            else -> Text(
                "Still opening the file\u2026",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.75f)
            )
        }
    }
}

/**
 * ONE MARK, AS A READER WOULD WRITE IT DOWN.
 *
 * v399 — IT SAYS WHAT IT IS, WHERE IT IS, AND WHAT IT SAID (user report: "the
 * your marks are kind of really bad view can u chnage its look").
 *
 * Every mark used to be the same grey card: a kind in the accent, a line of
 * words, an X. The three kinds are three different acts — a place kept, a
 * passage kept, a thought of your own — so each now wears its own glyph on a wash
 * of its own colour (a highlight in the ink it was actually made with), the row
 * is headed by the PAGE it belongs to with the chapter beside it, the passage is
 * set as a quotation in the book's own serif, and the note reads as an aside. The
 * date sits in the corner, and only the remove button carries a container.
 */
@Composable
private fun ReaderMarkRow(
    mark: ReaderMarkEntity,
    place: String,
    where: String,
    /** When it was made — `made` and not `when`, which is a keyword. */
    made: String,
    snippet: String,
    palette: ReaderPalette,
    onJump: () -> Unit,
    onDelete: (() -> Unit)?
) {
    // Lint's own rule: a composable must not read `Locale.getDefault()` — it is
    // not observable, so a member who changes their language would keep seeing
    // the old one's casing here until the page was rebuilt. The configuration IS
    // observable, and its first locale is the one the rest of the app formats
    // with (this is what the lint check asks for instead).
    val markLocale = LocalConfiguration.current.locales[0]
    val kind = mark.markKind
    val tone = if (kind == ReaderMarkKind.HIGHLIGHT) {
        readerHighlighter(mark.colorKey).ink
    } else {
        palette.accent
    }
    val glyph = when (kind) {
        ReaderMarkKind.HIGHLIGHT -> CurioIcons.FormatHighlight
        ReaderMarkKind.NOTE -> CurioIcons.Note
        else -> CurioIcons.Bookmark
    }
    Surface(
        onClick = onJump,
        shape = RoundedCornerShape(14.dp),
        color = palette.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(tone.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(glyph, kind.label, tint = tone, size = 16.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        place.uppercase(markLocale),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = tone
                    )
                    if (where.isNotBlank()) {
                        Text(
                            where,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.ink.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (made.isNotBlank()) {
                    Text(
                        made,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ink.copy(alpha = 0.4f)
                    )
                }
                if (onDelete != null) {
                    Surface(
                        onClick = onDelete,
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CurioIcon(
                                CurioIcons.Close,
                                "Remove this mark",
                                tint = palette.ink.copy(alpha = 0.4f),
                                size = 14.dp
                            )
                        }
                    }
                }
            }
            if (snippet.isNotBlank()) {
                // A passage, set as one: the book's own serif, indented under the
                // kind it was marked with, in the ink the reader chose to read
                // it in — a maximum of three lines, because a mark is a reminder
                // and not the page itself.
                Text(
                    "\u201C${snippet.trim()}\u201D",
                    style = TextStyle(
                        fontFamily = LoraFontFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        fontStyle = FontStyle.Italic
                    ),
                    color = palette.ink.copy(alpha = 0.92f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
            if (mark.note.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .padding(start = 40.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(palette.accent.copy(alpha = 0.10f))
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CurioIcon(
                        CurioIcons.Notes,
                        null,
                        tint = palette.accent.copy(alpha = 0.8f),
                        size = 13.dp
                    )
                    Text(
                        mark.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink.copy(alpha = 0.78f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                        color = palette.ink.copy(alpha = 0.75f)
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
/**
 * v389c — A HIGHLIGHT, AS THE PAGE DRAWS IT: the exact words, and the ink.
 *
 * The reader used to know only "this paragraph is marked", which is why a mark
 * held the whole block's words and a passage could not be chosen. Now the words
 * ARE the mark: a paragraph (or a PDF page) finds what it stored and washes that
 * run, so two passages in one paragraph are two different marks.
 */
private data class ReaderPassage(val text: String, val ink: Color)

/**
 * EVERY HIGHLIGHT A BLOCK WEARS (v389c).
 *
 * All of them, not the first: the words ARE the mark now, so a paragraph can
 * hold two passages the member thought were worth keeping, and a renderer that
 * took only the first would quietly drop the second.
 */
private fun highlightsFor(marks: List<ReaderMarkEntity>, index: Int): List<ReaderPassage> =
    marks.filter { it.isHighlight && it.positionIndex == index }
        .map { ReaderPassage(it.text, readerHighlighter(it.colorKey).ink) }

/** One stretch of a paragraph and the style it wears — see [annotatedWithSpans]. */
private data class ReaderTextSpan(val start: Int, val end: Int, val style: SpanStyle)

/**
 * THE SAME WORDS, WITH EVERY SPAN THAT COVERS THEM (v389c).
 *
 * A paragraph can be wearing three different washes at once — a stored highlight
 * on one run, the live selection over part of it, every search find lit — and
 * they overlap. So the string is cut at every span boundary and each piece wears
 * every style that covers it (nested, innermost last), which is the only way to
 * draw a selection INSIDE a highlight without one silently winning.
 */
private fun annotatedWithSpans(text: String, spans: List<ReaderTextSpan>): AnnotatedString {
    if (spans.isEmpty()) return AnnotatedString(text)
    val live = spans
        .map {
            ReaderTextSpan(
                it.start.coerceIn(0, text.length),
                it.end.coerceIn(0, text.length),
                it.style
            )
        }
        .filter { it.end > it.start }
    if (live.isEmpty()) return AnnotatedString(text)
    val points = sortedSetOf(0, text.length)
    live.forEach {
        points.add(it.start)
        points.add(it.end)
    }
    val cuts = points.toList()
    return buildAnnotatedString {
        for (i in 0 until cuts.size - 1) {
            val from = cuts[i]
            val to = cuts[i + 1]
            if (to <= from) continue
            val covering = live.filter { it.start <= from && it.end >= to }.map { it.style }
            if (covering.isEmpty()) {
                append(text.substring(from, to))
            } else {
                fun emit(depth: Int) {
                    if (depth >= covering.size) {
                        append(text.substring(from, to))
                        return
                    }
                    withStyle(covering[depth]) { emit(depth + 1) }
                }
                emit(0)
            }
        }
    }
}

/**
 * v389c — WHAT THE MEMBER HAS SWEPT UP, right now.
 *
 * One selection lives in the reader screen (not in a paragraph and not in a
 * page), because the bar that acts on it is drawn once, over everything: a
 * highlight is made for the same reason whether it was chosen on a PDF page or
 * in a reflowable paragraph, and the two surfaces should not each own half of
 * that decision.
 *
 * [index] is the format's own unit — a BLOCK index for a reflowable book (the
 * reader's paragraphs are flat list items) or a PAGE for a PDF — matching
 * [ReaderMarkEntity.positionIndex], which is how a highlight made here is found
 * again on the next visit.
 */
private data class ReaderSelection(
    val index: Int,
    /** Character offsets into the block's own text (or the page's own words). */
    val from: Int,
    val to: Int,
    val text: String,
    /** True when [index] is a PDF page rather than a paragraph. */
    val isPage: Boolean,
    /** The 1-based section a paragraph belongs to (0 for a page). */
    val section: Int = 0
) {
    /**
     * The same selection in the shape the mark layer already speaks (see
     * [saveReaderMark]) — so a highlight made by sweeping words is stored by
     * exactly the same path as one made from the hold-a-passage sheet, and
     * there is one writer for a book's marks rather than two.
     */
    fun asParagraph(): ReaderParagraph = ReaderParagraph(
        positionIndex = index,
        section = if (isPage) index + 1 else section,
        text = text,
        isHeading = false,
        isPage = isPage
    )
}

/**
 * THE WORDS ON A PDF PAGE (v389c).
 *
 * A PDF page is a BITMAP, which is why it could be bookmarked and noted but
 * never selected: there is nothing in a picture to put a caret in. The words do
 * exist, though — just not in the renderer — and `BookPdfText` already extracts
 * them, one page at a time, with every glyph's position in the page's own points.
 *
 * This layer is the other half: it sits exactly over the drawn page (the bitmap
 * is fitted into its box, so the drawn rect is that letterbox), draws every
 * stored passage back onto the words it was swept from, and turns a long press →
 * drag into a glyph range. The points-to-pixels ratio cancels out of every
 * mapping, so zooming or re-rendering the page at another resolution never moves
 * a highlight off its words.
 *
 * The extraction costs a parse, so the words are read by the CALLER — the page
 * being looked at, and any page already carrying a mark — and handed in ready. A
 * press therefore never waits for a parse to begin: either the words are there to
 * sweep, or the page has no text layer at all (a scan), in which case the press
 * is HANDED BACK and means exactly what it always meant — mark this page.
 */
@Composable
private fun PdfPageTextLayer(
    text: PdfPageText?,
    page: Int,
    bitmapSize: IntSize,
    container: IntSize,
    palette: ReaderPalette,
    highlights: List<ReaderPassage>,
    selection: ReaderSelection?,
    onSelect: (ReaderSelection) -> Unit,
    onPagePress: () -> Unit
) {
    if (bitmapSize.width <= 0 || bitmapSize.height <= 0 ||
        container.width <= 0 || container.height <= 0
    ) {
        return
    }
    val fitted = remember(bitmapSize, container) { fittedSize(bitmapSize, container) }
    if (fitted.width <= 0 || fitted.height <= 0) return
    val density = LocalDensity.current
    val drawnWidthPx = fitted.width.toFloat()

    // The gesture lives in a detector built once for this page, so everything it
    // reaches for is read through STATE rather than captured: the words, the two
    // answers and the width the page is drawn at are all live, and a sweep can
    // never end up selecting against a page that has since been re-read.
    val liveText = rememberUpdatedState(text)
    val liveSelect = rememberUpdatedState(onSelect)
    val livePagePress = rememberUpdatedState(onPagePress)
    val liveWidth = rememberUpdatedState(drawnWidthPx)
    var anchorGlyph by remember { mutableStateOf<IntRange?>(null) }

    /**
     * A SWEEP, from the word the press landed on to the word under the finger.
     *
     * Whole words at both ends — half a word is not a passage — and everything
     * between them, so a drag down the page keeps the words it passed over.
     */
    fun report(anchor: IntRange, at: Int) {
        val words = liveText.value ?: return
        val word = words.wordAround(at)
        if (word.isEmpty()) return
        val range = if (word.first < anchor.first) {
            word.first..anchor.last
        } else {
            anchor.first..maxOf(word.last, anchor.last)
        }
        val phrase = words.textBetween(range.first, range.last)
        if (phrase.isBlank()) return
        liveSelect.value(
            ReaderSelection(
                index = page,
                from = range.first,
                to = range.last,
                text = phrase,
                isPage = true
            )
        )
    }

    Canvas(
        modifier = Modifier
            .size(
                with(density) { fitted.width.toDp() },
                with(density) { fitted.height.toDp() }
            )
            .pointerInput(page) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        val words = liveText.value ?: return@detectDragGesturesAfterLongPress
                        // v389d — THE MARGIN, NOT THE WORDS. A press only keeps
                        // the old "hold to mark the whole page" meaning when it
                        // is a clear line and a half away from any type (or the
                        // page has no text layer at all). Everywhere else it is
                        // a sweep, settled on the LINE the finger landed on —
                        // which is what stops a hold on the words from washing
                        // the entire page (the reported selection bug).
                        val offWords = words.lineDistance(start.x, start.y, liveWidth.value) > 1.5f
                        val at = words.glyphAt(start.x, start.y, liveWidth.value)
                        val word = if (at < 0) IntRange.EMPTY else words.wordAround(at)
                        anchorGlyph = word
                        if (word.isEmpty() || offWords) {
                            // The finger is on the page's own margin, or on a
                            // page of pictures: nothing here to sweep, so the
                            // press keeps the meaning it has always had.
                            livePagePress.value()
                            return@detectDragGesturesAfterLongPress
                        }
                        report(word, at)
                    },
                    onDrag = { change, _ ->
                        val anchor = anchorGlyph ?: return@detectDragGesturesAfterLongPress
                        if (anchor.isEmpty()) return@detectDragGesturesAfterLongPress
                        val words = liveText.value ?: return@detectDragGesturesAfterLongPress
                        // A finger that leaves the page (the top or foot margin,
                        // or past the edge while sweeping) keeps the word it was
                        // last over rather than jumping to a glyph on the far
                        // side of the page — the other half of the same bug.
                        if (words.lineDistance(
                                change.position.x,
                                change.position.y,
                                liveWidth.value
                            ) > 1.5f
                        ) {
                            return@detectDragGesturesAfterLongPress
                        }
                        val at = words.glyphAt(change.position.x, change.position.y, liveWidth.value)
                        if (at < 0) return@detectDragGesturesAfterLongPress
                        report(anchor, at)
                    },
                    onDragEnd = { }
                )
            }
    ) {
        val words = text ?: return@Canvas
        val scale = if (words.pageWidthPt > 0f) size.width / words.pageWidthPt else 1f
        highlights.forEach { passage ->
            if (passage.text.isBlank()) return@forEach
            val at = words.text.indexOf(passage.text)
            if (at < 0) return@forEach
            drawPdfPassage(
                text = words,
                range = words.glyphRange(at, passage.text.length),
                color = passage.ink.copy(alpha = 0.34f),
                scale = scale
            )
        }
        selection?.takeIf { it.isPage && it.index == page }?.let { live ->
            drawPdfPassage(
                text = words,
                range = live.from..live.to,
                color = palette.accent.copy(alpha = 0.40f),
                scale = scale
            )
        }
    }
}

/** The drawn page inside its box: the same letterbox `ContentScale.Fit` makes. */
private fun fittedSize(bitmap: IntSize, container: IntSize): IntSize {
    if (bitmap.width <= 0 || bitmap.height <= 0) return IntSize.Zero
    val scale = minOf(
        container.width.toFloat() / bitmap.width,
        container.height.toFloat() / bitmap.height
    )
    return IntSize(
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1)
    )
}

/**
 * A PASSAGE, DRAWN ON THE WORDS IT WAS STOLEN FROM.
 *
 * One rectangle per GLYPH would leave a comb of seams between the letters; so
 * runs of glyphs that share a line and touch each other are joined into one
 * rectangle, which is what makes the wash look like a highlighter rather than a
 * row of boxes.
 */
private fun DrawScope.drawPdfPassage(
    text: PdfPageText,
    range: IntRange,
    color: Color,
    scale: Float
) {
    if (range.isEmpty() || text.glyphs.isEmpty()) return
    var runLeft = -1f
    var runRight = 0f
    var runTop = 0f
    var runBottom = 0f
    var runY = 0f
    var runHeight = 0f

    fun flush() {
        if (runLeft < 0f) return
        drawRoundRect(
            color = color,
            topLeft = Offset(runLeft, runTop),
            size = Size((runRight - runLeft).coerceAtLeast(1f), (runBottom - runTop).coerceAtLeast(1f)),
            cornerRadius = CornerRadius(2f * scale)
        )
        runLeft = -1f
    }

    val last = minOf(range.last, text.glyphs.size - 1)
    for (index in range.first..last) {
        if (index < 0) continue
        val glyph = text.glyphs[index]
        val left = glyph.x * scale
        val top = glyph.y * scale
        val right = (glyph.x + glyph.width) * scale
        val bottom = (glyph.y + glyph.height) * scale
        val sameLine = runLeft >= 0f && kotlin.math.abs(glyph.y - runY) <= runHeight * 0.6f
        val touching = sameLine && left - runRight <= runHeight * scale * 0.9f
        when {
            runLeft < 0f -> {
                runLeft = left
                runRight = right
                runTop = top
                runBottom = bottom
                runY = glyph.y
                runHeight = glyph.height
            }

            touching -> {
                runRight = right
                runTop = minOf(runTop, top)
                runBottom = maxOf(runBottom, bottom)
            }

            else -> {
                flush()
                runLeft = left
                runRight = right
                runTop = top
                runBottom = bottom
                runY = glyph.y
                runHeight = glyph.height
            }
        }
    }
    flush()
}

/**
 * THE SELECTION'S OWN BAR.
 *
 * One strip, drawn over the foot of the reader while words are selected: the
 * four inks (a tap marks the passage in that ink), a note, a bookmark, and a
 * door to everything else the old hold-a-passage sheet offered — so selecting
 * text ADDS a way to mark a book up without taking the old one away.
 *
 * It is deliberately not a Material toolbar: the reader has its own paper and
 * its own inks, and a quote marked on a page should look like the same system
 * the journal's own quote panels belong to.
 */
@Composable
private fun ReaderSelectionBar(
    selection: ReaderSelection,
    palette: ReaderPalette,
    onHighlight: (ReaderHighlighter) -> Unit,
    onNote: () -> Unit,
    onBookmark: () -> Unit,
    onMore: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            // The words themselves, so the member knows what they are about to
            // mark before they mark it (a selection handle off the edge of a
            // screen can leave the passage itself out of sight).
            Text(
                "\u201C${selection.text.take(140)}\u201D",
                style = TextStyle(
                    fontFamily = LoraFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = palette.ink.copy(alpha = 0.8f)
                ),
                maxLines = 2,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ReaderHighlighter.entries.forEach { ink ->
                    Surface(
                        onClick = { onHighlight(ink) },
                        shape = CircleShape,
                        color = ink.ink.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ink.ink),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(ink.ink)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(2.dp))
                SelectionBarAction(CurioIcons.Note, "Write a note on this passage", palette, onNote)
                SelectionBarAction(CurioIcons.Bookmark, "Bookmark this passage", palette, onBookmark)
                SelectionBarAction(CurioIcons.MoreHoriz, "More about this passage", palette, onMore)
                Spacer(Modifier.weight(1f))
                SelectionBarAction(CurioIcons.Close, "Clear the selection", palette, onClear)
            }
        }
    }
}

@Composable
private fun SelectionBarAction(
    glyph: String,
    description: String,
    palette: ReaderPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = palette.ink.copy(alpha = 0.12f),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = glyph,
                contentDescription = description,
                tint = palette.ink.copy(alpha = 0.8f),
                size = 17.dp
            )
        }
    }
}

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
     * v399 — WHICH PAGE THE SCROLLING READER'S ZOOM BELONGS TO.
     *
     * The column magnifies THE PAGE YOU PINCHED, not the whole file. Growing
     * every page at once moved the page above out from under the member's
     * fingers, because a column's neighbours are in the flow (user report: "in
     * vertical pages … when i pinch zoom in the middle the top part of the
     * previous page zooms in"). A magnified page is now a framed window onto
     * one page — nothing else in the column changes size, so nothing shifts.
     *
     * -1 means EVERY page, which is what the paged reader wants: the page it
     * shows is the only page there is.
     */
    var pdfZoomPage by mutableStateOf(-1)

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

    /**
     * v418 — HOW THE PAGE STANDS. A real three-way choice now, and it applies to
     * BOTH kinds of book.
     *
     * v406 offered a single "keep the page upright" switch, and only for a PDF,
     * so a reflowable book had no orientation control at all and the switch read
     * as broken (member: "the auto rotation wasnt working in pdf or epub with the
     * option"). The reader now takes the same tri-state every reader app does:
     *
     *  · [AUTO] follows the phone (the default, and what "auto rotation" means),
     *  · [PORTRAIT] holds the page upright while this book is open (reading in
     *    bed with the phone lying on its side),
     *  · [LANDSCAPE] holds it wide.
     *
     * It is applied on the ACTIVITY, so it works whatever the format is.
     */
    var orientation by mutableStateOf(ReaderOrientation.AUTO)

    /**
     * v422 — THE PAGE'S OWN TAP ZONES.
     *
     * A tap near an edge acts, and a tap anywhere else is the one the reader has
     * always had, the one that brings the chrome back. The reader that turns
     * pages wants the zones and the reader that turns pages by accident wants
     * them gone, which is why this is a switch in the reader's own foot rather
     * than a rule (see [readerZoneActionAt]).
     */
    var tapZones by mutableStateOf(true)

    /**
     * v424 — AND THEY ARE THE MEMBER'S TO PLACE.
     *
     * The SIDES are the whole height of the surface and the head and the foot
     * are the two bands between them, because a tap on the side of the SCREEN is
     * what a reader reaches for — not a corner the eye has to aim at (member:
     * "the side click to change page isnt working … not the page but side of
     * screen"). Each edge says what its OWN tap does ([ReaderZoneAction]) and
     * how deep it reaches ([zoneLeftDepth] and friends, as a share of the
     * surface), and the places are edited ON the page by holding the switch
     * above (see [ReaderTapZoneEditor]). The defaults are the reading the member
     * described: both sides turn the page, the top scrolls back, the foot goes on.
     */
    var zoneLeft by mutableStateOf(ReaderZoneAction.BACK)
    var zoneRight by mutableStateOf(ReaderZoneAction.FORWARD)
    var zoneTop by mutableStateOf(ReaderZoneAction.SCROLL_BACK)
    var zoneBottom by mutableStateOf(ReaderZoneAction.FORWARD)

    /** How deep each edge reaches: a share of the WIDTH for a side, of the HEIGHT for a band. */
    var zoneLeftDepth by mutableStateOf(ReaderZoneEdge.DEFAULT_SIDE)
    var zoneRightDepth by mutableStateOf(ReaderZoneEdge.DEFAULT_SIDE)
    var zoneTopDepth by mutableStateOf(ReaderZoneEdge.DEFAULT_BAND)
    var zoneBottomDepth by mutableStateOf(ReaderZoneEdge.DEFAULT_BAND)

    /** Whether the zone editor is up (a hold on the switch opens it — v424). */
    var zonesEditing by mutableStateOf(false)
}

/** v418 — the reader's orientation choice (see [ReaderLook.orientation]). */
private enum class ReaderOrientation(val label: String, val detail: String) {
    AUTO("Auto", "The page turns with your phone."),
    PORTRAIT("Upright", "The page stands up while this book is open."),
    LANDSCAPE("Wide", "The page stays wide while this book is open.");

    fun requested(): Int = when (this) {
        AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

/** The two ways a book can be laid out on screen. */
private enum class ReaderFlow(val label: String) {
    SCROLL("Scrolling"),
    PAGED("Pages");

    fun flipped(): ReaderFlow = if (this == SCROLL) PAGED else SCROLL
}

/**
 * v406 — WHERE THE MEMBER IS RIGHT NOW, not where they last stopped.
 *
 * The progress card used to read the STORED auto-bookmark, and that row is only
 * written once a scroll settles (up to 900ms later) — so the sheet could name a
 * place the member had already left (member's report: "the reading progres isnt
 * live"). A [ReaderLivePlace] is what a surface reports as it moves: an [index]
 * into the same space the stored mark uses (a block for reflowable text, a page
 * for a PDF) and the [total] of that space, which is all the fraction and the
 * "Page 7 of 300" line need.
 */
private data class ReaderLivePlace(val index: Int, val total: Int) {
    /** How far through the book this place is, 0..1. */
    val fraction: Float get() = index.toFloat() / (total - 1).coerceAtLeast(1)
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
 *
 * v394 — AND ONE FINGER MOVES A ZOOMED PAGE (user report: "when i pinch to zoom
 * then i cant just drag to move with one finger"). While the content is in
 * close, [zoomed] answers true and a single-finger drag is claimed as a PAN —
 * the drag the pager would otherwise take for a page turn.
 *
 * v395 — AND THE PORTION OF THE PAN THE PAGE COULD NOT USE GOES BACK (user
 * report: "when im zoomed in and im moving around, it changes page by mistake").
 *
 * The old rule consumed a one-finger move only from the gesture's SECOND event
 * (`previous != null`), so the first movement of every pan was handed to the
 * pager — which read it as the slop that starts a page turn, exactly the
 * mis-turn reported. [onZoom] now ANSWERS with the pan it actually took, and
 * the rule is the page's room instead of a count of events:
 *
 *  - a drag the page could use is consumed, from the first movement on, so the
 *    pager never sees the slop at all;
 *  - a drag at the page's OWN EDGE (no room left that way) is left unconsumed,
 *    so the pager picks it up and the turn still works while zoomed — the
 *    v394 behaviour, but decided by arithmetic instead of by accident;
 *  - and nothing is consumed until the finger has travelled past a share of
 *    the touch slop, so a TAP on a zoomed page still reaches the page (the
 *    chrome is raised by a tap, and a tap must not be eaten by its own jitter).
 */
/**
 * v424 — WHAT A TAP ON AN EDGE OF THE PAGE ASKS FOR.
 *
 * Two of these are the reader's oldest gestures (a page turn, and a screenful of
 * the reading) and one of them is nothing at all, which is what makes an edge the
 * member does not want a zone simply stop being one.
 *
 * TURN and SCROLL are deliberately separate: where a book has pages a scroll is
 * not possible and a turn is the only step there is, so the two meet there — but
 * a scrolling flow has real pages to turn as well as a column to move, and which
 * one an edge asks for is the member's choice, not the flow's.
 */
private enum class ReaderZoneAction(val label: String, val hint: String) {
    BACK("Back", "The page before"),
    FORWARD("On", "The next page"),
    SCROLL_BACK("Scroll back", "A screenful the other way"),
    SCROLL_FORWARD("Scroll on", "A screenful further on"),
    OFF("Nothing", "The tap stays with the page");

    /** -1 the way back, +1 the way on, 0 for a tap that belongs to the page. */
    val step: Int
        get() = when (this) {
            BACK, SCROLL_BACK -> -1
            FORWARD, SCROLL_FORWARD -> 1
            OFF -> 0
        }

    /** Whether this edge moves the READING by a screenful instead of turning. */
    val scrolls: Boolean get() = this == SCROLL_BACK || this == SCROLL_FORWARD
}

/**
 * v424 — THE FOUR EDGES OF THE SURFACE, and where each one's own answers live.
 *
 * An edge owns an [ReaderZoneAction] and a DEPTH: the sides are measured across
 * the width and stand the whole height, the head and the foot are measured down
 * the height and sit between the sides (see [readerZoneActionAt]).
 */
private enum class ReaderZoneEdge(val label: String) {
    LEFT("Left"),
    RIGHT("Right"),
    TOP("Top"),
    BOTTOM("Bottom");

    fun action(): ReaderZoneAction = when (this) {
        LEFT -> ReaderLook.zoneLeft
        RIGHT -> ReaderLook.zoneRight
        TOP -> ReaderLook.zoneTop
        BOTTOM -> ReaderLook.zoneBottom
    }

    fun setAction(action: ReaderZoneAction) {
        when (this) {
            LEFT -> ReaderLook.zoneLeft = action
            RIGHT -> ReaderLook.zoneRight = action
            TOP -> ReaderLook.zoneTop = action
            BOTTOM -> ReaderLook.zoneBottom = action
        }
    }

    fun depth(): Float = when (this) {
        LEFT -> ReaderLook.zoneLeftDepth
        RIGHT -> ReaderLook.zoneRightDepth
        TOP -> ReaderLook.zoneTopDepth
        BOTTOM -> ReaderLook.zoneBottomDepth
    }

    fun setDepth(value: Float) {
        val safe = value.coerceIn(DEPTH_MIN, DEPTH_MAX)
        when (this) {
            LEFT -> ReaderLook.zoneLeftDepth = safe
            RIGHT -> ReaderLook.zoneRightDepth = safe
            TOP -> ReaderLook.zoneTopDepth = safe
            BOTTOM -> ReaderLook.zoneBottomDepth = safe
        }
    }

    /** A side reaches across; a band reaches down. */
    val across: Boolean get() = this == LEFT || this == RIGHT

    /** Measured from the left or the top, rather than from the right or the foot. */
    val fromStart: Boolean get() = this == LEFT || this == TOP

    companion object {
        /** A side's share of the width, and a band's share of the height. */
        const val DEFAULT_SIDE = 0.16f
        const val DEFAULT_BAND = 0.14f

        /** How shallow and how deep a zone may be made, as a share of the surface. */
        const val DEPTH_MIN = 0.06f
        const val DEPTH_MAX = 0.42f

        /** What an edge does before the member has said otherwise. */
        fun defaultAction(edge: ReaderZoneEdge): ReaderZoneAction = when (edge) {
            LEFT -> ReaderZoneAction.BACK
            RIGHT -> ReaderZoneAction.FORWARD
            TOP -> ReaderZoneAction.SCROLL_BACK
            BOTTOM -> ReaderZoneAction.FORWARD
        }

        /** How deep an edge is before the member has said otherwise. */
        fun defaultDepth(edge: ReaderZoneEdge): Float =
            if (edge.across) DEFAULT_SIDE else DEFAULT_BAND
    }
}

/**
 * v422/v424 — WHERE A TAP LANDED, when the reader's tap zones are on.
 *
 * The SIDES come first and stand the whole height of the surface, so a tap on the
 * side of the screen always answers; the head and the foot are the two bands left
 * between them. Every answer and every depth is the member's own (see
 * [ReaderLook]); the point and the size are both in the SURFACE's coordinates,
 * which is why a magnified sheet translates the taps it answers (v424 — see
 * [PdfScrollReader]).
 */
private fun readerZoneActionAt(at: Offset, size: IntSize): ReaderZoneAction {
    if (size.width <= 0 || size.height <= 0) return ReaderZoneAction.OFF
    val left = size.width * ReaderLook.zoneLeftDepth
    val right = size.width * (1f - ReaderLook.zoneRightDepth)
    val top = size.height * ReaderLook.zoneTopDepth
    val bottom = size.height * (1f - ReaderLook.zoneBottomDepth)
    return when {
        at.x <= left -> ReaderLook.zoneLeft
        at.x >= right -> ReaderLook.zoneRight
        at.y <= top -> ReaderLook.zoneTop
        at.y >= bottom -> ReaderLook.zoneBottom
        else -> ReaderZoneAction.OFF
    }
}

/**
 * v424 — THE TAP ZONES, AS A PLACE THE MEMBER CAN PUT THEM.
 *
 * The switch in the reader's foot turns the zones on and off; a HOLD on it opens
 * this, which is the same page with the zones drawn on it: every edge is a wash
 * with its own line, every line has a handle to drag (that is the edge's depth),
 * the chosen edge wears the stronger wash, and the panel at the foot says what
 * its tap asks for.
 *
 * An edge is placed by looking at the page it governs — which is why nothing
 * here is a row in another screen (member: "let user adjust the positon or area
 * by tap an holding the button").
 */
@Composable
private fun ReaderTapZoneEditor(palette: ReaderPalette, onDone: () -> Unit) {
    val accent = palette.accent
    val ink = palette.ink
    var chosen by remember { mutableStateOf(ReaderZoneEdge.LEFT) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // The page underneath must not answer while the zones are being
            // placed: one tap here is one edit, never a page turn.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val boxSize = IntSize(constraints.maxWidth, constraints.maxHeight)

        // ── THE ZONES, DRAWN WHERE THEY ARE ─────────────────────────
        Canvas(Modifier.fillMaxSize()) {
            if (widthPx <= 0f || heightPx <= 0f) return@Canvas
            val left = widthPx * ReaderLook.zoneLeftDepth
            val right = widthPx * (1f - ReaderLook.zoneRightDepth)
            val top = heightPx * ReaderLook.zoneTopDepth
            val bottom = heightPx * (1f - ReaderLook.zoneBottomDepth)
            val hair = 1.dp.toPx()
            zoneBand(Offset.Zero, Offset(left, heightPx), ReaderZoneEdge.LEFT == chosen, accent)
            zoneBand(Offset(right, 0f), Offset(widthPx, heightPx), ReaderZoneEdge.RIGHT == chosen, accent)
            zoneBand(Offset(left, 0f), Offset(right, top), ReaderZoneEdge.TOP == chosen, accent)
            zoneBand(Offset(left, bottom), Offset(right, heightPx), ReaderZoneEdge.BOTTOM == chosen, accent)
            zoneRule(Offset(left, 0f), Offset(left, heightPx), ReaderZoneEdge.LEFT == chosen, accent, hair)
            zoneRule(Offset(right, 0f), Offset(right, heightPx), ReaderZoneEdge.RIGHT == chosen, accent, hair)
            zoneRule(Offset(left, top), Offset(right, top), ReaderZoneEdge.TOP == chosen, accent, hair)
            zoneRule(Offset(left, bottom), Offset(right, bottom), ReaderZoneEdge.BOTTOM == chosen, accent, hair)
        }

        // ── AND A HANDLE PER EDGE, WHICH IS WHERE ITS DEPTH IS SET ──
        ReaderZoneEdge.entries.forEach { edge ->
            ReaderZoneHandle(
                edge = edge,
                box = boxSize,
                selected = edge == chosen,
                accent = accent,
                onSelect = { chosen = edge }
            )
        }

        // ── THE PANEL: WHAT THE CHOSEN EDGE ASKS FOR, AND HOW DEEP ──
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = palette.paper.copy(alpha = 0.98f),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tap zones",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ink
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = {
                            ReaderZoneEdge.entries.forEach { edge ->
                                edge.setAction(ReaderZoneEdge.defaultAction(edge))
                                edge.setDepth(ReaderZoneEdge.defaultDepth(edge))
                            }
                        },
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "Reset",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onDone,
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            "Done",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }
                Text(
                    if (ReaderLook.tapZones) "Drop the lines where your thumb reaches."
                    else "The zones are off — turn them on with the switch in the reader's foot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.7f)
                )
                // WHICH EDGE, then WHAT IT DOES.
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReaderZoneEdge.entries.forEach { edge ->
                        ZoneChip(
                            label = edge.label,
                            live = edge == chosen,
                            accent = accent,
                            ink = ink,
                            onClick = { chosen = edge }
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReaderZoneAction.entries.forEach { action ->
                        ZoneChip(
                            label = action.label,
                            live = action == chosen.action(),
                            accent = accent,
                            ink = ink,
                            onClick = { chosen.setAction(action) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Depth",
                        style = MaterialTheme.typography.labelMedium,
                        color = ink.copy(alpha = 0.75f)
                    )
                    Slider(
                        value = chosen.depth(),
                        onValueChange = { chosen.setDepth(it) },
                        valueRange = ReaderZoneEdge.DEPTH_MIN..ReaderZoneEdge.DEPTH_MAX,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Text(
                        "${(chosen.depth() * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = ink.copy(alpha = 0.75f)
                    )
                }
                Text(
                    chosen.action().hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * v424 — ONE ZONE'S WASH, and the line that stands for its edge.
 *
 * File-level [DrawScope] extensions rather than local functions inside the
 * editor's `Canvas` lambda, for the one reason that matters here: the receiver
 * of a lambda is not reliably in scope inside a local declaration, so drawing
 * helpers live beside the drawing they belong to.
 */
private fun DrawScope.zoneBand(from: Offset, to: Offset, lit: Boolean, accent: Color) {
    drawRect(
        color = accent.copy(alpha = if (lit) 0.22f else 0.10f),
        topLeft = from,
        size = Size(
            (to.x - from.x).coerceAtLeast(0f),
            (to.y - from.y).coerceAtLeast(0f)
        )
    )
}

private fun DrawScope.zoneRule(from: Offset, to: Offset, lit: Boolean, accent: Color, hair: Float) {
    drawLine(
        color = accent.copy(alpha = if (lit) 0.85f else 0.45f),
        start = from,
        end = to,
        strokeWidth = hair
    )
}

/**
 * v424 — ONE EDGE'S HANDLE: the line that edge stands on, and a grip to drag it.
 *
 * The depth is read and written through the edge itself rather than captured, so
 * a drag can never compute against a value the composition has already replaced.
 */
@Composable
private fun ReaderZoneHandle(
    edge: ReaderZoneEdge,
    box: IntSize,
    selected: Boolean,
    accent: Color,
    onSelect: () -> Unit
) {
    if (box.width <= 0 || box.height <= 0) return
    val density = LocalDensity.current
    val across = edge.across
    val longPx = with(density) { 44.dp.toPx() }
    val thickPx = with(density) { 22.dp.toPx() }
    val handleW = if (across) thickPx else longPx
    val handleH = if (across) longPx else thickPx
    val span = if (across) box.width.toFloat() else box.height.toFloat()
    val x = if (across) {
        (if (edge.fromStart) edge.depth() else 1f - edge.depth()) * box.width
    } else {
        box.width / 2f
    }
    val y = if (across) {
        box.height / 2f
    } else {
        (if (edge.fromStart) edge.depth() else 1f - edge.depth()) * box.height
    }
    Box(
        modifier = Modifier
            .offset { IntOffset((x - handleW / 2f).roundToInt(), (y - handleH / 2f).roundToInt()) }
            .size(
                width = with(density) { handleW.toDp() },
                height = with(density) { handleH.toDp() }
            )
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else accent.copy(alpha = 0.40f))
            .clickable(onClick = onSelect)
            .pointerInput(edge, box) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val travel = if (across) drag.x else drag.y
                    val sign = if (edge.fromStart) 1f else -1f
                    if (span > 0f) edge.setDepth(edge.depth() + sign * travel / span)
                }
            }
    )
}

/** One choice in the zone editor's panel, in the app's own chip language. */
@Composable
private fun ZoneChip(
    label: String,
    live: Boolean,
    accent: Color,
    ink: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (live) accent.copy(alpha = 0.18f) else Color.Transparent
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (live) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (live) accent else ink.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

private fun Modifier.pinchToZoom(
    /**
     * What the gesture is bound to. A page whose SHAPE arrives after it was
     * first composed has to re-arm the handler, or the pinch would keep
     * measuring against the page as it was — nothing, when the render had not
     * landed yet (v399).
     */
    key: Any? = Unit,
    zoomed: () -> Boolean = { false },
    /**
     * @param zoom  the scale this event asks for.
     * @param pan   the drag this event carries, in the view's own pixels.
     * @param focus WHERE THE FINGERS ARE — the gesture's centroid at the
     *              position it held when this event's delta was measured. It is
     *              the point a magnified page has to KEEP under them, which is
     *              why the zoom is anchored here and not at the view's centre
     *              (see [readerZoomedPan]).
     * @return the part of [pan] the zoomed view actually took. [Offset.Zero]
     *         means "no room this way" — the drag is not consumed, and the
     *         caller's own scrolling or paging gets it.
     */
    onZoom: (zoom: Float, pan: Offset, focus: Offset) -> Offset
): Modifier = pointerInput(key) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var last: Offset? = null
        // Whether THIS gesture has moved the page, and whether the page turned
        // the drag down at its edge. Both are per-gesture on purpose (v406).
        var ownsTheDrag = false
        var declined = false
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            if (pressed.size >= 2) {
                // Two fingers are always the zoom's, from the first event: a
                // pinch that begins on a page is never a page turn.
                ownsTheDrag = true
                declined = false
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                // The centroid the fingers HELD during this delta (not where
                // they are now): the page grows about the point they grabbed.
                val focus = event.calculateCentroid(useCurrent = false)
                onZoom(zoom, pan, focus)
                // ── CLAIMED FROM THE FIRST EVENT (v406) ──
                //
                // The changes used to be consumed only once the pinch had
                // something to report (`zoom != 1f || pan != Offset.Zero`), so
                // the very first frame of every zoom was left to the scroll
                // underneath and the page moved before the pinch did — the
                // small shift the member saw when they zoomed or unzoomed a
                // page. Two fingers are the zoom's, so the zoom takes them at
                // once and the surface underneath never starts.
                event.changes.forEach { it.consume() }
                last = null
            } else if (pressed.size == 1 && zoomed()) {
                val position = pressed.first().position
                val previous = last
                last = position
                // ── v403 — A MAGNIFIED PAGE OWNS THE DRAG FROM THE FIRST MOVE ──
                //
                // While a page is magnified there is nothing to decide: the
                // drag belongs to the page, and it is consumed here — in the
                // page's own frame, which the scrolling column and the pager
                // both sit OUTSIDE of — so their slop wait is cancelled by the
                // consumption and neither of them ever starts.
                //
                // ── v406 — AND IT KEEPS IT TO THE END OF THE GESTURE ──
                //
                // The drag used to be handed back the moment the page reached
                // its edge, MID-GESTURE. The pager underneath had its own slop
                // cancelled by the consumption already, so when it finally took
                // over it did so with the finger's whole accumulated travel and
                // jumped to the next page in a flash (member's report: "i see
                // the glitched preview"). A gesture now belongs to the page
                // from its first move until the finger lifts; a page that is
                // ALREADY at its edge when the gesture starts takes nothing at
                // all, so the surface underneath owns the entire gesture — and
                // the page turns on ANOTHER swipe, exactly as the member asked
                // ("it should page change only when it reaches the page end and
                // then on another swipe it does").
                val delta = if (previous != null) position - previous else Offset.Zero
                if (!declined) {
                    val taken = if (delta == Offset.Zero) Offset.Zero
                    else onZoom(1f, delta, position)
                    if (taken != Offset.Zero) {
                        ownsTheDrag = true
                    } else if (delta != Offset.Zero) {
                        declined = true
                    }
                }
                if (ownsTheDrag) pressed.forEach { it.consume() }
            } else {
                ownsTheDrag = false
                declined = false
                last = null
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * THE PAGE INSIDE ITS FRAME, from the page's own [aspect].
 *
 * A page is drawn with `ContentScale.Fit`, so the thing a zoom magnifies and a
 * pan has room in is the LETTERBOXED page and not the box around it — the two
 * are the same shape only when the page and the screen happen to agree.
 */
private fun readerDrawnPage(box: IntSize, aspect: Float): Size {
    val width = box.width.toFloat()
    val height = box.height.toFloat()
    if (aspect <= 0f || width <= 0f || height <= 0f) return Size(width, height)
    return if (aspect >= width / height) Size(width, width / aspect)
    else Size(height * aspect, height)
}

/**
 * THE PINCH, THE PAN AND THE HAND-BACK FOR ONE PAGE, in that page's own frame
 * (v403).
 *
 * Both reading surfaces use it: the column's frames are its page items, and the
 * pager's frame is one page of the screen. It does four things, and the order
 * matters:
 *
 *  1. **A zoom belongs to a page.** If the page being pinched is not the page
 *     that owns the current zoom, this pinch starts from NOTHING rather than
 *     compounding a magnification the member left on a page they have since
 *     turned away from (which is also why a one-finger pan on a page that owns
 *     no zoom falls out at step 3 and is handed to the scroll underneath).
 *  2. **The zoom is anchored under the fingers** ([readerZoomedPan]), so the
 *     place the member grabbed stays under them as the page grows.
 *  3. **At rest it clears itself** — zooming back out to nothing gives the page
 *     back to the reader, with no pan left over.
 *  4. **It answers with the part of the drag it TOOK**: a drag the page has no
 *     room for is left unconsumed, which is what hands a page turn (or a scroll)
 *     back to the surface underneath — the member's own rule ("it should page
 *     change only when it reaches the page end and then on another swipe it
 *     does").
 */
/**
 * v422 — THE WHOLE DOCUMENT'S ZOOM, for the reader that scrolls a column.
 *
 * The paged flow magnifies THE PAGE, which is the only page there is; the
 * scrolling flow magnifies THE FILE, because one sheet swollen among its
 * neighbours reads as a mistake rather than a zoom (user report: "only one page
 * zooms in in that place feels wrong"). Nothing is anchored here and nothing
 * needs to be: the magnification IS the layout, so the lazy column keeps the
 * sheet the member was on exactly where they left it and simply grows it.
 *
 * The pan a two-finger drag carries is handed to the document's own two scrolls
 * — one down the column, one across the sheet — and a single finger is left
 * entirely to them, which is what [Offset.Zero] means: nothing taken, so the
 * column owns the gesture and the page still turns on a swipe.
 */
private fun readerZoomDocument(
    zoom: Float,
    drag: Offset,
    focus: Offset,
    down: ScrollableState,
    across: ScrollableState
): Offset {
    // ONE FINGER IS THE DOCUMENT'S SCROLL, not the zoom's: the pinch only
    // reports a factor of its own for two.
    if (zoom == 1f) return Offset.Zero
    val owns = ReaderLook.pdfZoomPage == -1
    val was = if (owns) ReaderLook.pdfZoom else 1f
    val next = (was * zoom).coerceIn(1f, 4f)
    // ── v424 — THE SHEET GROWS ABOUT THE FINGERS, NOT ABOUT ITS CORNER ──
    //
    // The magnification IS the layout (see above), so a sheet grows from its own
    // top-left and the place the member pinched slides away from them as it
    // grows — which is exactly the report this fixes (member: "its inaccurate
    // zoom, it zooming from the corner, not zooming where i am zooming").
    //
    // The correction needs no measurement, only one fact: the sheet's size is
    // PROPORTIONAL to the zoom, so the point under the fingers sits at
    // `focus * z` from the sheet's own start. Holding it still therefore means
    // scrolling the document by `focus * (z' - z)` in each direction — and since
    // a pinch arrives as many small steps, the anchor holds for the whole
    // gesture rather than sliding a little on every frame.
    //
    // `focus` is in the SHEET's coordinates (the gesture is armed on the sheet),
    // and the two scrolls are the document's own, so no viewport is involved.
    val ratio = if (was > 0f) next / was else 1f
    if (ratio != 1f) {
        down.dispatchRawDelta(focus.y * (ratio - 1f))
        across.dispatchRawDelta(focus.x * (ratio - 1f))
    }
    // AT REST, AND ONLY AT REST — the same rule the page's own zoom learned in
    // v406: the pan is thrown away only when there is no magnification left to
    // pan, so an unzoom cannot snap a still-magnified page anywhere.
    ReaderLook.pdfZoom = next
    ReaderLook.pdfZoomPage = -1
    ReaderLook.pdfPanX = 0f
    ReaderLook.pdfPanY = 0f
    if (drag != Offset.Zero) {
        down.dispatchRawDelta(-drag.y)
        across.dispatchRawDelta(-drag.x)
    }
    return drag
}

/**
 * v422 — THE DOCUMENT'S ZOOM AT ONE POINT: a double tap in the scrolling flow
 * takes the file in to read a line closely, and gives it back whole — the one
 * way to magnify that needs no second finger (see [readerZoomDocument]).
 *
 * v424 — AND IT LANDS WHERE IT WAS TAPPED, by the same arithmetic the pinch
 * uses: a double tap that magnified from the sheet's corner was the same
 * complaint in its other form.
 */
private fun readerDoubleTapDocument(
    at: Offset,
    down: ScrollableState,
    across: ScrollableState
) {
    val owns = ReaderLook.pdfZoomPage == -1
    val out = owns && ReaderLook.pdfZoom > 1.02f
    val was = if (owns) ReaderLook.pdfZoom else 1f
    val next = if (out) 1f else 2.2f
    val ratio = if (was > 0f) next / was else 1f
    if (ratio != 1f) {
        down.dispatchRawDelta(at.y * (ratio - 1f))
        across.dispatchRawDelta(at.x * (ratio - 1f))
    }
    ReaderLook.pdfZoom = next
    ReaderLook.pdfZoomPage = -1
    ReaderLook.pdfPanX = 0f
    ReaderLook.pdfPanY = 0f
}

private fun readerZoomThisPage(
    page: Int,
    box: IntSize,
    aspect: Float,
    zoom: Float,
    drag: Offset,
    focus: Offset
): Offset {
    val owns = ReaderLook.pdfZoomPage == page
    val was = if (owns) ReaderLook.pdfZoom else 1f
    val wasPan = if (owns) Offset(ReaderLook.pdfPanX, ReaderLook.pdfPanY) else Offset.Zero
    val next = (was * zoom).coerceIn(1f, 4f)
    // ── AT REST, AND ONLY AT REST (v406) ──
    //
    // This used to give the page up at 1.02, which threw the pan away while the
    // page was still 2% magnified — so an unzoom snapped the page back to its
    // centre from wherever it had been panned to (member's report: "when i zoom
    // or unzoom the page shifts"). At exactly 1× the pan is already zero, by
    // construction: a page at 1× has no room, so [readerZoomedPan] clamps every
    // translation to nothing. The page is therefore left at precisely 1× before
    // it is handed back, and there is nothing left to jump.
    if (next <= 1.001f) {
        ReaderLook.pdfZoom = 1f
        ReaderLook.pdfPanX = 0f
        ReaderLook.pdfPanY = 0f
        ReaderLook.pdfZoomPage = -1
        return Offset.Zero
    }
    val moved = readerZoomedPan(
        box = box,
        drawn = readerDrawnPage(box, aspect),
        from = was,
        to = next,
        // The fingers' own position WITHOUT the drag: the drag is folded in by
        // [readerZoomedPan] itself (v404). It used to be added here instead,
        // which cancelled the drag out at a constant zoom — the one-finger pan
        // came back as "no room" and was handed to the surface underneath, so a
        // zoomed page could not be moved at all and the column scrolled (or the
        // pager turned the page) under the finger.
        focus = focus,
        pan = wasPan,
        drag = drag
    )
    ReaderLook.pdfZoom = next
    ReaderLook.pdfZoomPage = page
    ReaderLook.pdfPanX = moved.x
    ReaderLook.pdfPanY = moved.y
    // What the page TOOK, in the drag's own direction: a page out of room
    // sideways must not claim a sideways drag, or a page turn or a scroll
    // would need a perfectly straight swipe to win one back.
    val sideways = (if (drag.x < 0f) -drag.x else drag.x) >=
        (if (drag.y < 0f) -drag.y else drag.y)
    val took = if (sideways) moved.x != wasPan.x else moved.y != wasPan.y
    return if (took) Offset(moved.x - wasPan.x, moved.y - wasPan.y) else Offset.Zero
}

/**
 * DOUBLE TAP: the same zoom as a pinch, asked for with one finger.
 *
 * In to read a line closely, out to see the whole page again — the gesture every
 * PDF reader has, and the one that shows what the pinch's anchoring is for: the
 * page grows about the point that was TAPPED, so the word the finger asked about
 * is still under it afterwards (v399).
 */
private fun readerDoubleTapZoom(page: Int, box: IntSize, aspect: Float, at: Offset) {
    if (ReaderLook.pdfZoomPage == page && ReaderLook.pdfZoom > 1.02f) {
        ReaderLook.pdfZoom = 1f
        ReaderLook.pdfPanX = 0f
        ReaderLook.pdfPanY = 0f
        ReaderLook.pdfZoomPage = -1
        return
    }
    val next = 2.2f
    val pan = readerZoomedPan(
        box = box,
        drawn = readerDrawnPage(box, aspect),
        from = 1f,
        to = next,
        focus = at,
        pan = Offset.Zero
    )
    ReaderLook.pdfZoom = next
    ReaderLook.pdfZoomPage = page
    ReaderLook.pdfPanX = pan.x
    ReaderLook.pdfPanY = pan.y
}

/**
 * THE PAN A MAGNIFIED PAGE NEEDS, from the fingers' [focus], the [drag] they
 * just made, the zoom they had ([from]) and the zoom they are asking for ([to]).
 *
 * The page point under the fingers before the gesture is
 * `centre + (p - centre) * from + pan`; after it, the SAME page point has to sit
 * under the fingers where they have moved to — so solving for the new
 * translation gives
 *
 *     next = pan * ratio + drag + (focus - centre) * (1 - ratio)
 *
 * with `ratio = to / from`. Both halves matter:
 *
 *  - `drag` is the one-finger pan, and it survives at a constant zoom, where
 *    the ratio is exactly 1 and the formula reduces to `pan + drag` — the
 *    reason a zoomed page can be moved at all. It was folded in by the CALLER
 *    as `focus + drag` before (v403), which cancelled it out at `ratio == 1`:
 *    every pan answered "no room", was left unconsumed, and the surface
 *    underneath took it (user report: "i cant even move around when zoomed in
 *    in vertical scrolling" / "the pages slips when its on side by side
 *    pages").
 *  - `(focus - centre) * (1 - ratio)` is the anchoring: the point the member
 *    grabbed grows about itself instead of about the frame's centre, so a
 *    pinch on the middle of a page does not drag the page's head out from under
 *    the finger (the v399 report, "the top part of the previous page zooms in").
 *
 * The travel is then clamped to what the page actually HAS: [drawn] is the page
 * inside [box], so half of the growth is how far either edge can travel before
 * it reaches its frame — and a page with no room says so by what it returns
 * ([pinchToZoom] leaves the drag to the scrolling or paging underneath, which
 * is how a page turn at the end of a magnified page still arrives on the next
 * swipe).
 */
private fun readerZoomedPan(
    box: IntSize,
    drawn: Size,
    from: Float,
    to: Float,
    focus: Offset,
    pan: Offset,
    drag: Offset = Offset.Zero
): Offset {
    // Nothing measured yet: this page has no room to give, so the drag belongs
    // to whatever is underneath.
    if (box.width <= 0 || box.height <= 0) return pan
    val centre = Offset(box.width / 2f, box.height / 2f)
    val ratio = if (from <= 0.001f) 1f else to / from
    // `Offset.Unspecified` (what a centroid is when there is no pointer to take
    // one of) means "anchor at the frame's centre", which is a pinch with no
    // fingers on it. Compared as a VALUE: Offset's own equality is bit-wise, so
    // its NaN-packed sentinel compares equal to itself.
    val at = if (focus != Offset.Unspecified) focus else centre
    val nextX = pan.x * ratio + drag.x + (at.x - centre.x) * (1f - ratio)
    val nextY = pan.y * ratio + drag.y + (at.y - centre.y) * (1f - ratio)
    val roomX = ((drawn.width * to - box.width) / 2f).coerceAtLeast(0f)
    val roomY = ((drawn.height * to - box.height) / 2f).coerceAtLeast(0f)
    return Offset(nextX.coerceIn(-roomX, roomX), nextY.coerceIn(-roomY, roomY))
}

// @Composable because the default ink asks [isCurioDarkTheme] what the app is
// wearing — one reader, two themes.
@Composable
private fun readerPalette(key: String): ReaderPalette = when (key) {
    "sepia" -> ReaderPalette(
        paper = Color(0xFFF3E7D3),
        ink = Color(0xFF4A3A28),
        accent = Color(0xFF9A6A43),
        surface = Color(0xFFF3E7D3),
        inkKey = "sepia"
    )
    "night" -> ReaderPalette(
        paper = Color(0xFF12100E),
        ink = Color(0xFFD8CFC2),
        accent = Color(0xFFC09263),
        surface = Color(0xFF252018),
        inkKey = "night"
    )
    "white" -> ReaderPalette(
        paper = Color(0xFFFFFFFF),
        ink = Color(0xFF1B1B1B),
        accent = Color(0xFF8A5A33),
        surface = Color(0xFFF5F5F5),
        inkKey = "white"
    )
    else -> if (isCurioDarkTheme()) {
        ReaderPalette(
            paper = Color(0xFF1A1714),
            ink = Color(0xFFE2D9CC),
            accent = Color(0xFFC09263),
            surface = Color(0xFF252018),
            inkKey = "paper"
        )
    } else {
        ReaderPalette(
            paper = Color(0xFFFBF6EC),
            ink = Color(0xFF2E2620),
            accent = Color(0xFF8A5A33),
            surface = Color(0xFFF5F0E8),
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
         * v389c — THE BOOK'S OWN PRINTED PAGES (see `epubPageList`).
         *
         * A reflowed book has no pages until it is rendered, but a book that was
         * TYPESET does — and an EPUB 3 carries that mapping as a page-list. Held
         * apart from [outline] rather than mixed into it, because "where does
         * chapter four begin" and "where is printed page 42" are two different
         * questions and the contents sheet offers them as two answers.
         */
        val pages: List<ReaderOutlineEntry> = emptyList(),
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
    val imagePath: String? = null,
    /**
     * v389c — THE BOOK'S OWN PAGE-BREAK MARKER, when one stood here.
     *
     * A typeset EPUB marks the place its printed page 42 begins with
     * `<span epub:type="pagebreak" id="page42">`. The parser records that id on
     * a block of its OWN — a zero-height block that is nothing but a target — so
     * the page-list's `#page42` has something to land on. The block draws
     * nothing: it is a bookmark in the text, not a thing in the text.
     */
    val anchor: String = "",
    /**
     * v398 — WHAT THE BOOK ITSELF SET IN BOLD OR ITALIC, as ranges into [text].
     *
     * The markup pass used to throw every tag away, so a phrase the edition
     * emphasised read as plain prose (user request: "epub emphasis now"). A
     * paragraph's own `<b>`/`<strong>` and `<i>`/`<em>` now survive as marks
     * during the parse and arrive here as runs, which the reader draws through
     * the same span layer its highlights and selections already use.
     */
    val emphasis: List<ReaderEmphasis> = emptyList()
)

/** One stretch of a paragraph the BOOK emphasised (see [ReaderBlock.emphasis]). */
private data class ReaderEmphasis(val start: Int, val end: Int, val bold: Boolean, val italic: Boolean)

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

private enum class ReaderSheet { INK, PLACES, SEARCH }

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
                .firstOrNull {
                    it.positionIndex == paragraph.positionIndex && it.markKind == kind &&
                        // v389c — A HIGHLIGHT BELONGS TO ITS PASSAGE. One per
                        // place was right while a mark was "this paragraph is
                        // marked"; now the words ARE the mark, so two runs of one
                        // paragraph are two marks and neither overwrites the
                        // other. Notes and bookmarks stay one per place (asking
                        // twice about the same page means editing, not adding).
                        (kind != ReaderMarkKind.HIGHLIGHT || it.text == text)
                }
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
                pages = read.pages,
                sectionSources = read.sources,
                // A book that carries its own page-list IS a book that says what
                // page the member is on, so Curio stops counting sections for it
                // — the same rule that makes a book printing page numbers in its
                // text not get a second set from the reader.
                ownPages = read.pages.isNotEmpty() ||
                    carriesOwnPageMarkers(read.blocks.map { it.text })
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
 * WHICH BLOCK AN OUTLINE ENTRY OPENS (v389c).
 *
 * An entry names a FILE and, when the book was kind enough to say so, an ANCHOR
 * inside it. Resolving by file alone was enough while every chapter began a file
 * of its own, but it is wrong twice over: a book that keeps several chapters in
 * one XHTML sent every one of them to the top of that file, and an EPUB's own
 * printed page numbers are nothing BUT anchors, so every page landed in the same
 * place. The anchor is tried first (it is the book's own, precise answer) and the
 * section's first block is the fallback for an entry that carries none — or one
 * whose marker this reader could not record, which must still open somewhere
 * near where it meant.
 */
private fun blockForEntry(content: ReaderContent.Text, entry: ReaderOutlineEntry): Int {
    val section = content.sectionSources.indexOf(entry.target)
    if (section < 0) return -1
    val inSection = content.blocks.withIndex().filter { it.value.section == section + 1 }
    if (inSection.isEmpty()) return -1
    if (entry.anchor.isNotBlank()) {
        inSection.firstOrNull { it.value.anchor == entry.anchor }?.let { return it.index }
    }
    return inSection.first().index
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
/** An EPUB, read: its blocks, the file each section came from, and its own
 *  contents — the three things that let an outline entry name a block. */
private class EpubRead(
    val blocks: List<ReaderBlock>,
    val sources: List<String>,
    val outline: List<ReaderOutlineEntry>,
    /** The book's own printed page numbers, when it carries a page-list. */
    val pages: List<ReaderOutlineEntry> = emptyList()
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
        var printedPages: List<ReaderOutlineEntry> = emptyList()
        ZipFile(source).use { zip ->
            // The book's OWN contents and its own printed pages, read while the
            // archive is open anyway (two navigation lists, one pass).
            outline = epubOutline(zip)
            printedPages = epubPageList(zip)
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
        EpubRead(
            blocks = blocks,
            sources = sources,
            outline = outline,
            pages = printedPages
        )
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
        // v398 — THE BOOK'S OWN EMPHASIS IS MARKED BEFORE ANYTHING ELSE IS
        // STRIPPED. `<b>`/`<strong>` and `<i>`/`<em>` become characters no book
        // text can hold, so they cross the tag-stripping pass, the whitespace
        // collapse and the paragraph split, and [splitEmphasis] reads them back
        // into ranges once the paragraph's own words are settled. Done FIRST so
        // that a bold word inside a heading is marked before the heading's own
        // regex captures its text.
        .replace(Regex("<(b|strong)(\\s[^>]*)?>", RegexOption.IGNORE_CASE), "\u0001")
        .replace(Regex("</(b|strong)\\s*>", RegexOption.IGNORE_CASE), "\u0002")
        .replace(Regex("<(i|em)(\\s[^>]*)?>", RegexOption.IGNORE_CASE), "\u0003")
        .replace(Regex("</(i|em)\\s*>", RegexOption.IGNORE_CASE), "\u0004")
        .replace(Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        // A PAGE BREAK, turned into a marker of its own BEFORE the markup comes
        // off: `<span epub:type="pagebreak" id="page42">` is where the printed
        // edition's page 42 begins, and the page-list's own link points at that
        // id. It becomes a zero-height block carrying the anchor, which is what
        // makes a printed page number a real place to jump to (v389c).
        .replace(
            Regex(
                "<span[^>]*?(?:epub:type\\s*=\\s*[\"']pagebreak[\"']|role\\s*=\\s*[\"']doc-pagebreak[\"'])[^>]*>",
                RegexOption.IGNORE_CASE
            )
        ) { match ->
            val id = Regex("id\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                .find(match.value)?.groupValues?.getOrNull(1).orEmpty()
            if (id.isBlank()) "" else "\n\n\u0000P:$id\u0000\n\n"
        }
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
                    val (text, emphasis) = splitEmphasis(body.substringAfter(':').trim())
                    if (text.isNotBlank()) {
                        out.add(
                            ReaderBlock(
                                text = text,
                                section = section,
                                sectionTitle = sectionTitle,
                                isHeading = true,
                                headingLevel = level.coerceIn(1, 3),
                                emphasis = emphasis
                            )
                        )
                    }
                }

                chunk.startsWith("\u0000P:") -> {
                    // A printed page begins here. The block is a TARGET, not a
                    // line: it says nothing and draws nothing, and the read
                    // views skip it entirely (see ReaderParagraphBlock).
                    val id = chunk.removePrefix("\u0000P:").removeSuffix("\u0000").trim()
                    if (id.isNotBlank()) {
                        out.add(
                            ReaderBlock(
                                text = "",
                                section = section,
                                sectionTitle = sectionTitle,
                                isHeading = false,
                                anchor = id
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

                else -> {
                    val (text, emphasis) = splitEmphasis(chunk)
                    if (text.isNotBlank()) {
                        out.add(
                            ReaderBlock(
                                text = text,
                                section = section,
                                sectionTitle = sectionTitle,
                                isHeading = false,
                                emphasis = emphasis
                            )
                        )
                    }
                }
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

/**
 * v398 — A PARAGRAPH'S WORDS, WITH THE BOOK'S OWN EMPHASIS READ BACK OUT OF THEM.
 *
 * [epubBlocks] marks `<b>`/`<strong>` and `<i>`/`<em>` with four characters no
 * book text can hold, because the alternative — measuring ranges in the markup
 * and hoping the whitespace collapse did not move a single offset — is
 * arithmetic that only has to be wrong once to put a bold run on the wrong
 * words. Here the marks come out and the plain text is left exactly as it
 * looked, so every range is by construction in the coordinates the reader
 * draws with.
 *
 * Nesting is COUNTED rather than assumed: `<b>a <i>b</i> c</b>` is one bold run
 * holding an italic one, and an unbalanced tag (a book with a stray `</b>`)
 * cannot drive a count negative.
 */
private fun splitEmphasis(chunk: String): Pair<String, List<ReaderEmphasis>> {
    if (!chunk.any { it == EMPH_BOLD_ON || it == EMPH_BOLD_OFF || it == EMPH_ITALIC_ON || it == EMPH_ITALIC_OFF }) {
        return chunk to emptyList()
    }
    val text = StringBuilder(chunk.length)
    val flags = ArrayList<Int>(chunk.length)
    var bold = 0
    var italic = 0
    chunk.forEach { character ->
        when (character) {
            EMPH_BOLD_ON -> bold++
            EMPH_BOLD_OFF -> bold = (bold - 1).coerceAtLeast(0)
            EMPH_ITALIC_ON -> italic++
            EMPH_ITALIC_OFF -> italic = (italic - 1).coerceAtLeast(0)
            else -> {
                text.append(character)
                flags.add((if (bold > 0) 1 else 0) or (if (italic > 0) 2 else 0))
            }
        }
    }
    val runs = ArrayList<ReaderEmphasis>()
    var i = 0
    while (i < flags.size) {
        val flag = flags[i]
        var j = i + 1
        while (j < flags.size && flags[j] == flag) j++
        if (flag != 0) runs.add(ReaderEmphasis(i, j, flag and 1 != 0, flag and 2 != 0))
        i = j
    }
    return text.toString() to runs
}

/** The four marks [epubBlocks] writes and [splitEmphasis] reads. */
private const val EMPH_BOLD_ON = '\u0001'
private const val EMPH_BOLD_OFF = '\u0002'
private const val EMPH_ITALIC_ON = '\u0003'
private const val EMPH_ITALIC_OFF = '\u0004'

private fun stripMarkup(html: String): String = html
    .replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .trim()

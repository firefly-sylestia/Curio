package com.curio.app.features.personal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.NeuralSpeaker
import com.curio.app.data.NeuralVoicePacks
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.ReaderMarkEntity
import com.curio.app.data.ReaderMarkKind
import com.curio.app.data.newReaderMarkId
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.ambientGlassOn
import com.curio.app.ui.components.curioAmbientGlass
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.ui.components.rememberCurioGlassScreen
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.infrastructure.ReadAloudService
import com.curio.app.infrastructure.ReadAloudSession
import com.curio.app.ui.theme.CurioMotion
// v455 phase 3 — the reader's sheets take the motion system's clock and curve
// when the experiment is on (see the note in [ReaderSheetFrame]).
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.curio.app.ui.theme.WritingFontFamily
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    // out. Because it is hidden, `statusBarsPadding()` collapses with it — so
    // the chrome does NOT ask it for room and states its own floor instead
    // (v435, `ReaderChromeTopFloor`): the display cut-out plus a real margin,
    // which is what keeps the head pill off the edge of the glass.
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
    /**
     * v437 — WHETHER THE PAGE SLIDER IS UP (see [ReaderScrubPill]).
     *
     * It is NOT a [ReaderSheet] any more: a sheet is a modal panel with a scrim
     * that covers the page, and the whole of the member's ask was that scrubbing
     * through a book should not cover the book ("small floating without the bottom
     * sheet"). It is a floating control of the reading surface, so it has its own
     * flag — and `sheet` staying null while it is up is what lets the selection bar
     * and the chrome behave exactly as they do without it.
     */
    var scrubOpen by remember { mutableStateOf(false) }
    //
    // v431 — AND THE READING SETTINGS ARE A PAGE OF THEIR OWN. The member asked
    // for it ("settings gets its own screen"), and it belongs to the reader rather
    // than to the app: it opens OVER the book, in the reader's own paper, and
    // back returns to the page that was being read (see [ReaderSettingsScreen]).
    var readerSettingsOpen by remember { mutableStateOf(false) }
    // The back handler itself lives further down, beside the state it answers for
    // (v448 moved it: a selection and a snapshot are things back must put down, and
    // both are declared below this line) — see "BACK CLOSES WHAT IS OPEN".
    var marking by remember { mutableStateOf<ReaderParagraph?>(null) }
    var noteFor by remember { mutableStateOf<ReaderParagraph?>(null) }
    var searching by remember { mutableStateOf<ReaderSearch?>(null) }
    // v389c — WHAT IS SELECTED RIGHT NOW. One selection for the whole reader (a
    // PDF page and a reflowable paragraph are two ways of choosing the same
    // thing: words), so the bar that acts on it is drawn once, and either
    // surface simply reports what the finger swept.
    var selection by remember { mutableStateOf<ReaderSelection?>(null) }
    // ── v448 — WHAT IS BEING SNAPSHOTTED ─────────────────────────────────
    //
    // The member: *"for share, when sharing from 3 dot open a crop selection … and
    // it hides the dock and header too"*. `snapshotArmed` is the moment between the
    // ⋯ door and the copy (the chrome takes a beat to leave, and it must be gone
    // before the picture is taken), and `snapshot` is the crop frame itself — the
    // capture, once it exists (see [ReaderSnapshot]).
    var snapshotArmed by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(snapshotArmed) {
        if (!snapshotArmed) return@LaunchedEffect
        // The pills' own leave, plus a breath: a capture taken mid-animation would
        // keep a half-faded pill in the member's crop.
        delay(CurioMotion.EXIT_MS.toLong() + 140)
        val shot = captureReaderScreen(view)
        snapshotArmed = false
        if (shot != null) {
            snapshot = shot
        } else {
            // No picture (a view that refused to hand one over): the chrome comes
            // back rather than the member being left on a bare page.
            chrome = true
        }
    }
    // ── v438/v448 — BACK CLOSES WHAT IS OPEN, NOT THE BOOK ───────────────
    //
    // The member: *"backing from settings exits the reader"*, and their own
    // follow-up now: *"add back handling when i select highlight something, coz
    // when backing it exits the reader"*. Reading settings is drawn OVER the
    // reading from inside this screen (see [ReaderSettingsScreen]) — it is not a
    // route — so one handler answers for everything this screen opens over itself,
    // innermost thing first, and it is enabled only while something IS open: with
    // nothing up, back leaves the book exactly as before. A crop frame and a
    // selection are the two newest things it puts down; either of them used to
    // hand the back to the navigation stack and leave the book with them still up.
    BackHandler(
        enabled = readerSettingsOpen || ReaderLook.zonesEditing || sheet != null || scrubOpen ||
            selection != null || snapshot != null || snapshotArmed
    ) {
        when {
            snapshot != null -> {
                snapshot?.recycle()
                snapshot = null
                chrome = true
            }
            snapshotArmed -> {
                snapshotArmed = false
                chrome = true
            }
            readerSettingsOpen -> readerSettingsOpen = false
            ReaderLook.zonesEditing -> ReaderLook.zonesEditing = false
            sheet != null -> sheet = null
            scrubOpen -> scrubOpen = false
            selection != null -> selection = null
        }
    }
    // v434 — WHAT THE DICTIONARY OPENS ON when it was asked for from the mark
    // dock rather than from a sweep (see [ReaderMarkSheet]). v442 — it carries
    // the PASSAGE, not a word picked out of it: the sheet reads the passage for
    // the words worth offering and for the sentence to quote (see
    // [ReaderDictionary.wordsIn] and [readerContextFor]).
    var dictionarySeed by remember { mutableStateOf("") }
    // v444 — WHICH DOOR OPENED THE DICTIONARY. The ⋯ menu's own door is a
    // SEARCH (a taller sheet, its field the point of it), while a sweep or a
    // held passage opens the same sheet to answer for the words in hand (see
    // [ReaderDictionarySheet]'s `searchMode`).
    var dictionarySearch by remember { mutableStateOf(false) }

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
        // v437 — and the page slider goes first of all (the member: "hides when
        // tap on page"). It floats over the very page a tap lands on, so a tap
        // there is a tap on it rather than a request for the tools.
        if (scrubOpen) {
            scrubOpen = false
            return
        }
        chrome = !chrome
    }

    fun hideChrome() {
        if (chrome) chrome = false
    }

    val palette = readerPalette(ReaderLook.inkKey)

    // ── v434 — THE LOOK IS REMEMBERED, AND THE SCREEN IS TOLD TO STAY ───
    //
    // Two small effects, and both belong to the READER rather than to the page:
    // the preferences are the member's own (loaded once on the way in, written
    // once per settling change — see [ReaderLookStore]), and "keep the screen
    // awake" is a flag on the view the reader is drawn in, so it can never
    // outlive the reading.
    val lookContext = LocalContext.current.applicationContext
    LaunchedEffect(Unit) { ReaderLookStore.load(lookContext) }
    LaunchedEffect(Unit) {
        snapshotFlow { ReaderLook.rememberKey() }
            .distinctUntilChanged()
            .debounce(400)
            .collect { ReaderLookStore.save(lookContext) }
    }
    val readerView = LocalView.current
    DisposableEffect(readerView, ReaderLook.keepScreenOn) {
        readerView.keepScreenOn = ReaderLook.keepScreenOn
        onDispose { readerView.keepScreenOn = false }
    }
    // ── v439 — AND IT LEAVES NOTHING BEHIND (see [ReaderLook.lowPower]) ────
    //
    // The pictures an EPUB copies out of its archive live in `cacheDir/book-images`
    // and stayed there until Android felt like reclaiming them. That is exactly
    // the "app cache is huge" a member notices in their own settings, and while
    // low power reading is on the reader clears it as it closes. Off screen and on
    // a background thread, because deleting a folder is not something a page turn
    // should wait for — and only ever the reader's OWN folder, so nothing else the
    // app has cached is touched (the book re-copies what it needs next time it is
    // opened, which is the whole reason this folder is a cache rather than data).
    DisposableEffect(lookContext, ReaderLook.lowPower) {
        onDispose {
            if (!ReaderLook.lowPower) return@onDispose
            CoroutineScope(Dispatchers.IO).launch {
                imageDir(lookContext).listFiles()?.forEach { runCatching { it.delete() } }
            }
        }
    }

    // ── HOW THIS BOOK FLOWS ──────────────────────────────────────────────
    // The PAGED text flow lays the book out itself, so the page count is the
    // pager's to report; the count the foot pill wears is built further down,
    // beside the PDF pager it also reads (see `scrubber` — it cannot be built
    // here, because at this point in the body the pager it names does not exist
    // yet, and v431 it also needs the live place every surface reports).
    var textPageCount by remember { mutableIntStateOf(0) }
    val textPager = rememberPagerState { textPageCount }
    // v431 — the keyboard goes down with the search bar (see `onCloseSearch`).
    val focusManager = LocalFocusManager.current

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

    // ── v432 — AND THE CHROME NEVER LEAVES ON ITS OWN ANY MORE ─────────────
    //
    // v406 put a 4.2s countdown on the chrome: the tools went away by themselves
    // while the member was reading (their report now: "its appear and disapper of
    // the tools"). A countdown is the wrong shape for this — the reader can never
    // tell "the member has stopped using me" from "the member is reading the page
    // I am standing over", and the tools vanishing mid-sentence is what that
    // guess looks like from the other side. So the chrome leaves when it is TOLD
    // to, which is the rule the member asked for in the first place: a tap on the
    // page puts it away ([tapPage]), a scroll of the member's own does the same
    // ([onScrolled] — a turn or a jump the reader asked for keeps it, see
    // [askedByReader]), a selection steps it aside, and a jump from a mark or a
    // chapter closes it. Nothing else moves it.

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

    /**
     * v440 — HOW MANY PAGES OF THE CHAPTER ARE STILL AHEAD.
     *
     * Reported by the paged text flow, which is the only surface that knows where
     * a chapter's pages end (see [TextPagedReader]). A BOOK WITH ITS OWN PAGES never
     * reports at all — the places sheet works the PDF's own answer out from the
     * outline, which carries a page per chapter.
     */
    var sectionPagesLeft by remember(bookId, document) { mutableStateOf<Int?>(null) }

    // ── v441 — THE STEP THAT IS ALREADY ON ITS WAY ────────────────────
    //
    // Every arrow and zone used to work its target out from a place that is only
    // true once the turn has FINISHED — `pagerState.currentPage`, `shownPage`,
    // `listState.firstVisibleItemIndex`, `textPager.currentPage`. Rapid taps
    // therefore all computed the SAME next page, each asked for the turn the one
    // before it had already started, and the slider answered one page for four
    // taps and then went dead while it settled (the member: "next and previous
    // button doesnt work on rapid click only goes 1 and stops working").
    //
    // So the ask is remembered for as long as it is still an ask. [stepLedger]
    // holds the settled place the last step was taken FROM and the place it was
    // sent TO; the next tap steps from the destination whenever the reader is
    // still standing on either end of that hop — settled (the turn finished) or
    // not (the turn is in flight). The moment the reader is anywhere else — a
    // scrub, a chapter, a mark, their own scroll — neither end matches and the
    // ledger is ignored, so a stale hop can never send the arrows somewhere the
    // member is not. A plain IntArray: nothing in composition reads it, so it
    // must not invalidate anything.
    //
    // It is declared HERE, above both readers of it ([stepPage] below and the
    // page slider's own arrows much further down), because a local function in
    // Kotlin cannot reach a local declared later in the same body.
    val stepLedger = remember(bookId, document) { intArrayOf(-1, -1) }

    fun stepFrom(from: Int, step: Int, last: Int): Int {
        val asked = stepLedger[1]
        val base = if (asked in 0..last && (from == stepLedger[0] || from == asked)) {
            asked
        } else {
            from
        }
        val target = (base + step).coerceIn(0, last)
        stepLedger[0] = from
        stepLedger[1] = target
        return target
    }

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
                val from = pagerState.currentPage.coerceIn(0, last)
                scope.launch { pagerState.animateScrollToPage(stepFrom(from, step, last)) }
            } else {
                val last = (loaded.pageCount - 1).coerceAtLeast(0)
                val from = shownPage.coerceIn(0, last)
                turnPageFromBar(stepFrom(from, step, last))
            }

            is ReaderContent.Text -> if (ReaderLook.textFlow == ReaderFlow.PAGED) {
                val last = (textPageCount - 1).coerceAtLeast(0)
                val from = textPager.currentPage.coerceIn(0, last)
                scope.launch { textPager.animateScrollToPage(stepFrom(from, step, last)) }
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
        // ── v432 — A PINCH IS NOT A TAP ─────────────────────────────────────
        //
        // A zoom ends with both fingers lifting in one event, which every tap
        // detector in the reader reads as a TAP on the page it happened over —
        // so the tools came and went with each zoom, and a lift near the side of
        // the screen turned the page instead of putting the chrome back (member:
        // "its appear and disapper of the tools"). Every tap the reader answers
        // comes through here, the zones included, so this is where it is said
        // once (see [ReaderTouch]).
        val action =
            if (ReaderLook.tapZones) readerZoneActionAt(at, size) else ReaderZoneAction.OFF
        when {
            ReaderTouch.multi -> Unit
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
        // ── v448 — AND IT IS WRITTEN THE MOMENT IT IS ASKED FOR ──────────
        //
        // The rotation this line causes can rebuild the whole screen, and a rebuild
        // reads the stored look ([ReaderLookStore.load]). The look's own save is
        // debounced 400ms by design (a slider should not write a file per pixel), so
        // an orientation change wrote itself NOW rather than racing the window it
        // just turned — together with the once-per-process load above, that is what
        // makes "Wide" stay wide.
        runCatching { ReaderLookStore.save(lookContext) }
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

    // ── v431 — WHERE THE READER IS, AND THE THREE WAYS TO MOVE ────────
    //
    // ONE description, built here, read by the foot pill's middle button and by
    // the sheet that button opens (see [ReaderScrubber]). This is the only place
    // in the reader that knows what kind of place this book counts in: PAGES for
    // a PDF whichever flow it is in, the book's own printed pages for a reflowed
    // book read as pages, and the book's SECTIONS for one read as a scroll — which
    // is what lets the scrubber drag through a novel that has no pages of its own
    // yet instead of opening on nothing.
    val scrubber: ReaderScrubber? = when (val loaded = content) {
        is ReaderContent.Pages -> if (ReaderLook.pageFlow == ReaderFlow.PAGED) {
            val at = pagerState.currentPage.coerceIn(0, (loaded.pageCount - 1).coerceAtLeast(0))
            ReaderScrubber(
                short = "${at + 1} / ${loaded.pageCount}",
                label = "Page ${at + 1} of ${loaded.pageCount}",
                at = at + 1,
                total = loaded.pageCount,
                onPrev = {
                    askedByReader = true
                    scope.launch {
                        pagerState.animateScrollToPage(
                            stepFrom(at, -1, loaded.pageCount - 1)
                        )
                    }
                },
                onNext = {
                    askedByReader = true
                    scope.launch {
                        pagerState.animateScrollToPage(
                            stepFrom(at, 1, loaded.pageCount - 1)
                        )
                    }
                },
                onScrub = { page ->
                    turnPageFromBar((page - 1).coerceIn(0, (loaded.pageCount - 1).coerceAtLeast(0)))
                }
            )
        } else {
            // The scrolling column has no "current page" of its own, so the place
            // it names is the sheet under the reader's eye ([shownPage], v399).
            val at = shownPage.coerceIn(0, (loaded.pageCount - 1).coerceAtLeast(0))
            ReaderScrubber(
                short = "${at + 1} / ${loaded.pageCount}",
                label = "Page ${at + 1} of ${loaded.pageCount}",
                at = at + 1,
                total = loaded.pageCount,
                onPrev = { turnPageFromBar(stepFrom(at, -1, loaded.pageCount - 1)) },
                onNext = { turnPageFromBar(stepFrom(at, 1, loaded.pageCount - 1)) },
                onScrub = { page ->
                    turnPageFromBar((page - 1).coerceIn(0, (loaded.pageCount - 1).coerceAtLeast(0)))
                }
            )
        }

        is ReaderContent.Text -> {
            val blocks = loaded.blocks.size
            if (ReaderLook.textFlow == ReaderFlow.PAGED && textPageCount > 0) {
                val at = textPager.currentPage.coerceIn(0, (textPageCount - 1).coerceAtLeast(0))
                // v422 — WHAT THE BOOK SAYS, NOT WHAT THE READER COUNTED. See
                // [printedPageAt]: the book's own printed page names the place
                // wherever one covers it, and it is blank for a book that carries
                // none — a number the reader invented and the file contradicts is
                // worse than no number at all.
                val named = printedPageAt(liveTextBlock)
                ReaderScrubber(
                    short = "${at + 1} / $textPageCount",
                    label = named.ifBlank { "Page ${at + 1} of $textPageCount" },
                    at = at + 1,
                    total = textPageCount,
                    onPrev = {
                        askedByReader = true
                        scope.launch {
                            textPager.animateScrollToPage(stepFrom(at, -1, textPageCount - 1))
                        }
                    },
                    onNext = {
                        askedByReader = true
                        scope.launch {
                            textPager.animateScrollToPage(stepFrom(at, 1, textPageCount - 1))
                        }
                    },
                    onScrub = { page ->
                        askedByReader = true
                        scope.launch {
                            textPager.scrollToPage((page - 1).coerceIn(0, textPageCount - 1))
                        }
                    }
                )
            } else if (blocks > 0) {
                // A REFLOWED BOOK COUNTS SECTIONS (see the note above).
                val at = listState.firstVisibleItemIndex.coerceIn(0, blocks - 1)
                // Inline rather than [jumpToBlock], which is declared further down
                // this same body: a local declared later cannot be reached here.
                val put = { place: Int ->
                    pendingBlock = place.coerceIn(0, blocks - 1)
                    chrome = false
                }
                ReaderScrubber(
                    short = "${at + 1} / $blocks",
                    label = "Section ${at + 1} of $blocks",
                    at = at + 1,
                    total = blocks,
                    onPrev = { put(stepFrom(at, -1, blocks - 1)) },
                    onNext = { put(stepFrom(at, 1, blocks - 1)) },
                    onScrub = { place -> put(place - 1) }
                )
            } else {
                null
            }
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
        if (sheet != ReaderSheet.CONTENTS) return@LaunchedEffect
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

    // ── v440 — READING IT ALOUD (see [ReaderSpeaker]) ──────────────────
    //
    // The member, from the settings list: *"Read-aloud: a speed and voice picker"*
    // and, asked what it reads, *"The visible page, then follow on."*
    //
    // So the driver below does exactly that and nothing cleverer: it starts at what
    // the member is LOOKING AT (`liveTextBlock` for a reflowable book, the page on
    // screen for a PDF), feeds the voice a chunk at a time, and — because a finished
    // chunk is the only signal that says the reader is still listening — hands the
    // reader on to the next one from the moment the previous finishes, to the end of
    // the book.
    //
    // Three rules it keeps, and each of them is a bug that does not happen:
    //  · **ONE CURSOR, TWO MEANINGS.** A reflowable book's index is a BLOCK and a
    //    PDF's is a PAGE; the cursor is the same state because the flow already says
    //    which one it is (see [content]).
    //  · **THE CURSOR IS MOVED BY THE CALLBACK, NEVER BY THE EFFECT.** The effect is
    //    keyed on the cursor, so moving it there would queue the next chunk twice —
    //    once as it launched and once when the key changed.
    //  · **A PAGE WITH NOTHING TO SAY IS STEPPED OVER, NOT STUCK ON.** A PDF of
    //    plates has pages with no words on them; the driver moves to the next one
    //    instead of going quiet (and stops at the book's own end).
    // ── v463 — THE VOICE READS SENTENCES NOW (see [ReaderSentence]) ────
    //
    // v440 handed the engine four paragraphs at a time, which is the right unit for a
    // speech engine and the wrong one for a reader: the screen could place the voice no
    // more precisely than "somewhere in this block", so there was nothing to draw a
    // mark on and nothing to skip to. The driver steps one SENTENCE per utterance, and
    // that same sentence index is what draws the read-along wash and what the skip
    // controls move — one cursor, three readers of it.
    //
    // The session has TWO states rather than one, and that is what makes pausing mean
    // something: [voiceOn] is "a voice belongs to this book", [voicePaused] is "it is
    // holding its breath". A pause keeps the sentence marked (the member can see where
    // they stopped) and takes the dimming away (the page is theirs to read again).
    val hasText = content is ReaderContent.Text
    // Built ONCE per file, never per frame: the keys are the file that was opened, so a
    // re-layout, a theme change or a scroll cannot walk the whole book again.
    val sentences = remember(bookId, document, hasText) {
        speechSentences((content as? ReaderContent.Text)?.blocks.orEmpty())
    }
    var voiceOn by remember(bookId, document) { mutableStateOf(false) }
    var voicePaused by remember(bookId, document) { mutableStateOf(false) }
    var speakCursor by remember(bookId, document) { mutableIntStateOf(-1) }
    // ── v465h — A NUDGE, SO A VOICE CHANGE IS HEARD AT ONCE ──────────────
    //
    // The engine and the voice are read PER SENTENCE (see [sayAloud]), which is
    // what lets a change land mid-book — but it also means a change made while
    // listening would take effect on the NEXT sentence, up to half a minute of the
    // voice the member just replaced. Bumping this restarts the driver on the SAME
    // cursor with the new voice, so picking a voice is answered by the voice. It is
    // in the driver's keys for exactly that reason and for no other.
    var speakEpoch by remember(bookId, document) { mutableIntStateOf(0) }
    // v465h — the reader's own voice picker, opened from the voice bar (see
    // [ReaderVoiceSheet]): a voice is the one reading choice a member makes WHILE
    // listening, and it used to be four screens away.
    var voiceSheet by remember { mutableStateOf(false) }
    // WHAT THE PAGE IS TOLD TO LIGHT, AND TO STAND BACK FROM.
    val spoken = sentences.getOrNull(speakCursor)
    val spokenRun = if (voiceOn && spoken != null) {
        ReaderSpoken(spoken.block, spoken.from, spoken.to)
    } else {
        null
    }
    val readingAloud = voiceOn && !voicePaused
    // ── v465i — AND TAKING THE READING BACK ─────────────────────────────
    //
    // The other half of the handover (see the dispose below): a reading of THIS
    // book that is going on without its page is picked up here — at the sentence
    // the voice has reached — so reopening a book that is being read aloud
    // continues it in place, with the wash back on the right line, instead of
    // starting over or leaving a hidden second driver speaking.
    LaunchedEffect(bookId, document) {
        val taken = ReadAloudContinuation.takeOver(bookId)
        if (taken != null) {
            speakCursor = taken.index
            voiceOn = true
            voicePaused = !taken.playing
            ReaderSpeaker.prepare(context, ReaderEngine.packageFor(ReaderLook.speakEngine))
        }
    }
    // The engine is a service binding, so it is brought up when the member ASKS for
    // the voice and released the moment the reader closes — never merely because a
    // book was opened (see [ReaderSpeaker.prepare]).
    DisposableEffect(bookId, document) {
        onDispose {
            // ── v465i — LEAVING THE PAGE IS NOT STOPPING THE VOICE ─────────
            //
            // The driver lives in this composition, so popping the reader used to
            // end the reading with it — the member's own report was "exiting
            // cancels it", and the background-listening switch could not help,
            // because there was nothing left for the service to keep alive. A live
            // session is HANDED OVER instead: [ReadAloudContinuation] reads the
            // remaining sentences, owns the notification's four buttons, and gives
            // the reading back — at the sentence the voice has reached — when this
            // book's page is opened again.
            val handingOver = voiceOn && AppPreferences.readAloudBackgroundEnabledState
            if (handingOver) {
                val textAt: suspend (Int) -> String? = when (content) {
                    is ReaderContent.Text -> { index -> sentences.getOrNull(index)?.text }
                    is ReaderContent.Pages -> { index ->
                        runCatching { extractPdfPageText(context, document, index) }
                            .getOrNull()?.text
                    }
                    null -> { _ -> null }
                }
                val pieces = when (val shape = content) {
                    is ReaderContent.Text -> sentences.size
                    is ReaderContent.Pages -> shape.pageCount
                    null -> 0
                }
                ReadAloudContinuation.handOff(
                    context = context,
                    bookId = bookId,
                    title = book?.title.orEmpty(),
                    count = pieces,
                    from = speakCursor,
                    playing = !voicePaused,
                    textAt = textAt
                )
                return@onDispose
            }
            // Nothing to carry on with (or background listening is off): the
            // reading goes with the page, exactly as it always did. A handover of
            // ANOTHER book ends here too — one page owns the voice.
            ReadAloudContinuation.end()
            ReaderSpeaker.release()
            // v465c — a downloaded pack's model is the other thing that must not
            // outlive the reader: it holds onnxruntime's memory and, for Kokoro,
            // hundreds of megabytes of it. Released on exactly the same event as
            // the system engine, so the three voices have one lifetime rule.
            NeuralSpeaker.release()
            // v465f — and the Edge voice's socket and player, for the same reason:
            // a WebSocket outliving the reader is a connection for a screen that
            // is gone.
            EdgeVoice.stop()
            // v465h — and the session's keep-alive, with the session itself: the
            // service exists to keep a reading alive, and there is no reading once
            // the reader is gone. `clear()` also drops the four notification
            // lambdas, because a control wired to a screen that no longer exists is
            // worse than a dead button — it would look alive.
            ReadAloudSession.clear()
            ReadAloudService.stop(context)
        }
    }
    val startSpeaking = {
        val at = when (content) {
            is ReaderContent.Pages -> livePlace?.index ?: shownPage
            is ReaderContent.Text -> sentences.indexOfFirst { it.block >= liveTextBlock }
            null -> 0
        }
        speakCursor = at.coerceAtLeast(0)
        ReaderSpeaker.prepare(context, ReaderEngine.packageFor(ReaderLook.speakEngine))
        voiceOn = true
        voicePaused = false
        // v465i — a fresh start is a fresh attempt at the Edge experiment: the
        // refusal remembered during the last session must not outlive it.
        ReadAloudSession.edgeUnavailable = false
        // ── ONE VOICE, AND IT IS THIS PAGE'S ────────────────────────────
        //
        // A reading handed over from ANOTHER book (see [ReadAloudContinuation])
        // keeps going while this page is open — that is the point of the handover.
        // But the moment this page starts a voice of its own, the other one has to
        // go: two drivers speaking over each other is the one thing the whole
        // single-cursor design exists to prevent.
        if (!voiceOn) ReadAloudContinuation.end()
        Unit
    }

    /**
     * v463 — A SKIP, IN SENTENCES (or, for a PDF, in pages — a page is that book's own
     * unit and it has no sentences to step through).
     *
     * A skip with no session OPEN is still a reasonable thing to ask for — "read from
     * here" — so it opens one. With a session on it moves the cursor and lets the driver
     * speak the new sentence at once; the engine's own `QUEUE_FLUSH` is the other half of
     * that, so the sentence being abandoned cannot finish first (see [ReaderSpeaker.say]).
     */
    fun stepSentence(step: Int) {
        when (val loaded = content) {
            is ReaderContent.Text -> {
                val list = sentences
                if (list.isEmpty()) return
                val at = if (voiceOn && speakCursor in list.indices) {
                    speakCursor
                } else {
                    list.indexOfFirst { it.block >= liveTextBlock }.coerceAtLeast(0)
                }
                speakCursor = (at + step).coerceIn(0, list.size - 1)
            }

            is ReaderContent.Pages -> {
                val last = (loaded.pageCount - 1).coerceAtLeast(0)
                speakCursor = (speakCursor.coerceIn(0, last) + step).coerceIn(0, last)
            }

            null -> return
        }
        if (!voiceOn) ReadAloudContinuation.end()
        ReaderSpeaker.prepare(context, ReaderEngine.packageFor(ReaderLook.speakEngine))
        voiceOn = true
        voicePaused = false
    }

    /**
     * v463 — A SKIP, IN CHAPTERS.
     *
     * A chapter is a change of [ReaderSentence.section], so the target is "the first
     * sentence whose section is the next one along" — which lands on the chapter's own
     * heading rather than on a paragraph, and is why this needs no lookup table of its
     * own. A PDF has no chapters in its text, so there a chapter step is a jump to an end.
     */
    fun stepChapter(step: Int) {
        when (val loaded = content) {
            is ReaderContent.Text -> {
                val list = sentences
                if (list.isEmpty()) return
                val here = list.getOrNull(speakCursor.coerceIn(0, list.size - 1))
                    ?: list.first()
                val sections = list.map { it.section }.distinct()
                val want = (sections.indexOf(here.section) + step)
                    .coerceIn(0, sections.size - 1)
                val index = list.indexOfFirst { it.section == sections[want] }
                if (index < 0) return
                speakCursor = index
                jumpToBlock(list[index].block)
            }

            is ReaderContent.Pages -> {
                val last = (loaded.pageCount - 1).coerceAtLeast(0)
                speakCursor = if (step < 0) 0 else last
                jumpToPage(speakCursor)
            }

            null -> return
        }
        if (!voiceOn) ReadAloudContinuation.end()
        ReaderSpeaker.prepare(context, ReaderEngine.packageFor(ReaderLook.speakEngine))
        voiceOn = true
        voicePaused = false
    }

    // ── v465h — ONE VOICE, TWO DOORS ────────────────────────────────────
    //
    // The bar on the page and the four buttons in the notification are the SAME
    // control, so both call these two functions rather than the notification
    // growing its own copy of the `when` below. That is the whole reason the
    // session carries lambdas instead of intents (see [ReadAloudSession]): a
    // member pausing from the shade and a member pausing on the page must not be
    // two code paths that can disagree about what "paused" means.
    fun toggleVoice() {
        when {
            // v463 — the two states are one button: a running voice holds its
            // breath, a held one carries on, and a book with no voice yet starts
            // one at the page the member is looking at.
            voicePaused -> voicePaused = false
            voiceOn -> {
                voicePaused = true
                ReaderSpeaker.stop()
                // v465c — and the neural voice, for the same reason: a pause that
                // only silenced one of the two engines would be a pause that did
                // not pause.
                NeuralSpeaker.stop()
                // v465f — and the Edge voice: a socket plus a MediaPlayer.
                EdgeVoice.stop()
            }
            else -> startSpeaking()
        }
    }

    /**
     * Ends the session, from the page or from the shade's Stop.
     *
     * Every engine is silenced for the same reason pause silences all three, and
     * `voiceOn = false` is what stands the keep-alive service down (see the
     * session effect below) — so a stop is a stop wherever it came from.
     */
    fun stopVoice() {
        voiceOn = false
        voicePaused = false
        ReaderSpeaker.stop()
        NeuralSpeaker.stop()
        EdgeVoice.stop()
    }

    // ── v465h — THE SESSION THAT OUTLIVES THE SCREEN ────────────────────
    //
    // Read aloud is driven HERE, and it stays here (see [ReadAloudSession] for
    // why). What leaves is the promise that the phone will not stop it: a
    // backgrounded app's threads are freezable, and a voice that goes quiet
    // mid-sentence with no error anywhere IS that freeze. These two effects are the
    // whole of the fix — the controls and the description are written into the
    // session on every recomposition, and the keep-alive service is started,
    // re-rendered or stood down when the voice's STATE changes.
    //
    // Re-registered every recomposition on purpose: these close over the PAGE the
    // member is looking at (a skip with no session open starts one from the block
    // on screen), so a lambda captured once when the session began would keep
    // starting from wherever they were then.
    SideEffect {
        ReadAloudSession.onToggle = { toggleVoice() }
        ReadAloudSession.onPrev = { stepSentence(-1) }
        ReadAloudSession.onNext = { stepSentence(1) }
        ReadAloudSession.onStop = { stopVoice() }
    }
    // A STATE CHANGE, NOT A SENTENCE: what the notification says is the book and
    // whether it is speaking, both of which change a handful of times in a session
    // — one call per sentence would be hundreds of service starts to redraw a line
    // that never moved.
    LaunchedEffect(voiceOn, voicePaused, book?.title) {
        ReadAloudSession.active = voiceOn
        ReadAloudSession.playing = voiceOn && !voicePaused
        ReadAloudSession.title = book?.title.orEmpty()
        if (voiceOn) ReadAloudService.sync(context) else ReadAloudService.stop(context)
    }

    LaunchedEffect(voiceOn, voicePaused, speakCursor, speakEpoch) {
        if (!voiceOn || voicePaused) return@LaunchedEffect
        when (val loaded = content) {
            is ReaderContent.Text -> {
                val list = sentences
                if (list.isEmpty()) {
                    voiceOn = false
                    return@LaunchedEffect
                }
                val from = speakCursor.coerceIn(0, list.size - 1)
                val sentence = list[from]
                // The page follows the voice — but only when it has to MOVE. A
                // sentence in the block already on screen must not scroll the page
                // out from under the member's eye.
                if (sentence.block != liveTextBlock) jumpToBlock(sentence.block)
                // ── v465i — THE DRIVER WAITS, AND IT DOES NOT WAIT FOR EVER ──
                //
                // This used to be fire-and-forget: the sentence was handed to the
                // engine and a callback moved the cursor. Two things were wrong with
                // that. An engine that never reported back left the reading frozen on
                // one mark with no error anywhere (the member's *"the 2nd one wasnt
                // playing"*), and a callback that landed while the last words were
                // still sounding had the NEXT sentence flush exactly the words before
                // the sentence's own comma or full stop (*"i was skipping comma and
                // full stop words"*). So the effect body now AWAITS the sentence, and
                // a sentence nobody reports on within [ALOUD_STALL_MS] ends the
                // session where it stands instead of hanging on it.
                val done = CompletableDeferred<Unit>()
                sayAloud(context, sentence.text, ReaderLook.speakSpeed) { done.complete(Unit) }
                if (withTimeoutOrNull(ALOUD_STALL_MS) { done.await() } == null) {
                    stopVoice()
                    return@LaunchedEffect
                }
                // The grace, so the engine's own tail is not cut by the next flush.
                delay(ALOUD_TAIL_GRACE_MS)
                // THE END OF THE BOOK IS THE ONLY THING THAT STOPS IT. There is
                // no next sentence to move to, and an engine cannot be asked to
                // speak nothing — so the session closes and the last mark stays,
                // which is where a reader would leave the page anyway.
                val next = from + 1
                if (next >= list.size) {
                    voiceOn = false
                    voicePaused = false
                } else {
                    speakCursor = next
                }
            }

            is ReaderContent.Pages -> {
                val page = speakCursor.coerceIn(0, (loaded.pageCount - 1).coerceAtLeast(0))
                // A page's words cost a parse of the file's own text layer, so it is
                // read off the main thread — the same door the search and the marks
                // read through.
                val said = withContext(Dispatchers.IO) {
                    runCatching { extractPdfPageText(context, document, page) }
                        .getOrNull()?.text.orEmpty()
                }.trim()
                val after = page + 1
                if (said.isBlank()) {
                    // A PLATE HAS NOTHING TO SAY (v440's rule), and the book's own end
                    // is the end (the v463 fix: the old code came back round and read
                    // the last page of a PDF for ever).
                    if (after < loaded.pageCount) {
                        speakCursor = after
                    } else {
                        voiceOn = false
                        voicePaused = false
                    }
                } else {
                    jumpToPage(page)
                    // v465i — the same wait, the same timeout and the same grace a
                    // sentence gets: a page's last words are words too, and a page
                    // that never reports back must not freeze the reading either.
                    val donePage = CompletableDeferred<Unit>()
                    sayAloud(context, said, ReaderLook.speakSpeed) { donePage.complete(Unit) }
                    if (withTimeoutOrNull(ALOUD_STALL_MS) { donePage.await() } == null) {
                        stopVoice()
                        return@LaunchedEffect
                    }
                    delay(ALOUD_TAIL_GRACE_MS)
                    if (after >= loaded.pageCount) {
                        voiceOn = false
                        voicePaused = false
                    } else {
                        speakCursor = after
                    }
                }
            }

            null -> voiceOn = false
        }
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

    // ── v431 — WHERE THE FIND BEING STOOD ON IS ─────────────────────
    //
    // The bar names it ("3/17") and the pages wash it harder than the rest, so
    // the two have to agree on which hit is current. One number, read by both.
    val currentHitIndex = searching?.let { run ->
        run.hits.getOrNull(run.current)?.index ?: -1
    } ?: -1
    val searchQuery = searching?.query.orEmpty()
    // ── v424 — WHERE THE READER'S OWN SURFACE SITS, AND HOW BIG IT IS ──
    //
    // A tap zone belongs to the surface the member sees, and the scrolling PDF's
    // frames sit on a document that can be wider than the screen — so a tap inside
    // one of them reports the FRAME's own coordinates. Both facts are captured
    // here, once, and handed to the surface below (see [PdfScrollReader]).
    var surfaceOrigin by remember { mutableStateOf(Offset.Zero) }
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    // ── v451 — THE READER'S GLASS (the app-wide ambient, see
    // [rememberCurioGlassScreen]) ─────────────────────────────────────
    //
    // The member: *"liquid glass to more buttons and things app wide, many doesn't
    // have it"* — and the reader was the biggest "doesn't": its head, foot, search,
    // motion lock, speak, pinned-count and scrubber pills were all SOLID `palette
    // .surface` with a 10–12dp lift, while every other floating surface in the app
    // could refract.
    //
    // The page is marked as the capture ([readerGlass.capture] below) and the pills
    // are its SIBLINGS, which is the one rule real refraction has (a pill may never
    // sample a layer containing itself — the v228 cyclic render node). Nothing else
    // in the reader changes: with glass off, or on Android below 12, `capture` and
    // the pills' [curioAmbientGlass] are no-ops and every pill keeps the fill it has
    // today.
    val readerGlass = rememberCurioGlassScreen()
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
                detectTapGestures(
                    onTap = {
                        // v437 — the page slider answers a tap before the chrome
                        // does (see [tapPage], and the member's "hides when tap on
                        // page").
                        if (scrubOpen) scrubOpen = false
                        else if (!ReaderTouch.multi) chrome = !chrome
                    }
                )
            }
    ) {
        // The captured layer: everything the pills float over, and ONLY that —
        // the chrome is composed after it, outside this box.
        //
        // The PAPER is repeated inside the capture on purpose. `layerBackdrop`
        // records this subtree alone, so without a fill the capture would be
        // transparent wherever the page draws nothing (a margin, the end of a
        // chapter) and a pill there would refract nothing at all. Painting the
        // reader's own paper in here makes the layer opaque, so a pill over the
        // words and a pill over the margin are the same glass (this is the same
        // construction Home's capture uses, and the outer box's own fill is
        // underneath it, so nothing changes visually).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.paper)
                .then(readerGlass.capture)
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
                    onSectionPagesLeft = { left -> sectionPagesLeft = left },
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
            onPendingConsumed = { pendingBlock = null },
            spoken = spokenRun,
            readingAloud = readingAloud
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
                    },                    onTap = onSurfaceTap,
                    // v424 — the screen the tap zones are measured on, and
                    // where it starts in the window: the scrolling PDF's frames
                    // sit on a document wider than the screen, so a tap inside
                    // one of them is reported in the FRAME's coordinates and has
                    // to be said in the screen's (see [PdfScrollReader]).
                    viewport = surfaceSize,
                    surfaceOrigin = surfaceOrigin,
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
                    },
                    // v431 — the floating search's words, washed on the pages
                    // (see [PdfPageTextLayer]).
                    query = searchQuery,
                    currentHit = currentHitIndex
                )

                // `content` is a delegated property, so the null check above
                // cannot smart-cast it — this branch is what makes the `when`
                // exhaustive (the file was still being read a moment ago).
                null -> Unit
            }
        }
        }

        /**
         * v431 — ONE FIND, ONE STEP.
         *
         * The two arrows of the floating search ask for the next or the previous
         * hole the words were found in, and the surface that owns the place goes
         * there — a page for a PDF, a block for a reflowable book — through the
         * SAME asks every other jump uses (see [pendingPage] and [pendingBlock]),
         * so a find can never move a surface that is not showing.
         */
        fun searchStep(step: Int) {
            val run = searching ?: return
            val hits = run.hits
            if (hits.isEmpty()) {
                // Nothing found YET: the arrow is "ask again", which is what the
                // member means before the sweep has finished.
                run.token += 1
                return
            }
            val size = hits.size
            val next = (((run.current + step) % size) + size) % size
            run.current = next
            val at = hits[next].index
            when (content) {
                is ReaderContent.Text -> jumpToBlock(at)
                is ReaderContent.Pages -> jumpToPage(at)
                null -> Unit
            }
        }

        // The pills' own door to the capture above — see [CurioGlassScreen.Provide].
        readerGlass.Provide {
        ReaderChrome(
            visible = chrome && !ReaderLook.zonesEditing,
            title = book?.title.orEmpty().ifBlank { "Reader" },
            palette = palette,
            pageLabel = scrubber?.short.orEmpty(),
            footHidden = scrubOpen,
            // v439 — the motion lock is a PDF's control (see [ReaderChrome]).
            pdf = content is ReaderContent.Pages,
            // v440 — the voice's own pill, and the reader's own driver behind it:
            // pausing stops the engine as well as the driver, so a resumed tap starts
            // from where the member IS rather than finishing a stale sentence (the
            // driver's `QUEUE_FLUSH` is the other half of that).
            speaking = voiceOn,
            paused = voicePaused,
            // v465h — the bar's own button and the shade's button are the same
            // call now (see [toggleVoice]), so the page and the notification cannot
            // drift into two ideas of what "paused" means.
            onToggleSpeak = { toggleVoice() },
            // v465h — and the voice is changeable from the page: a voice is the one
            // reading choice a member makes WHILE listening, and it used to be four
            // screens away from the thing they were listening to.
            onVoice = { voiceSheet = true },
            onSpeakPrevSentence = { stepSentence(-1) },
            onSpeakNextSentence = { stepSentence(1) },
            onSpeakPrevChapter = { stepChapter(-1) },
            onSpeakNextChapter = { stepChapter(1) },
            search = searching,
            onClose = { navController.popBackStack() },
            onSearch = {
                searching = ReaderSearch()
                // The bar IS the chrome's head, so the head has to be up.
                chrome = true
            },
            onCloseSearch = {
                focusManager.clearFocus()
                searching = null
            },
            onSearchQuery = { asked -> searching?.query = asked },
            onSearchStep = { step -> searchStep(step) },
            onAppearance = { sheet = ReaderSheet.APPEARANCE },
            onContents = { sheet = ReaderSheet.CONTENTS },
            // v437 — the page slider is a toggle on the page, not a sheet:
            // a second tap on the count puts it away (see [ReaderScrubPill]).
            onPages = { scrubOpen = !scrubOpen },
            onPinPages = { ReaderLook.pinnedPage = !ReaderLook.pinnedPage },
            onBookmarks = { sheet = ReaderSheet.BOOKMARKS },
            onMenu = { sheet = ReaderSheet.MENU }
        )
        }

        // ── v431 — THE PINNED COUNT, OUTSIDE THE CHROME ─────────────────
        //
        // Outside on purpose: a pin that went away with the tools would be a pin
        // worth nothing (see [ReaderPinnedPage]). It sits under the head pill's
        // row so it never fights it for the corner.
        val pinned = scrubber
        if (ReaderLook.pinnedPage && pinned != null && pinned.short.isNotBlank()) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                // Sits outside [ReaderChrome], so it gets the screen's glass of its
                // own (see [rememberCurioGlassScreen]).
                readerGlass.Provide {
                ReaderPinnedPage(
                    pageLabel = pinned.short,
                    palette = palette,
                    onUnpin = { ReaderLook.pinnedPage = false }
                )
                }
            }
        }

        // ── v438 — AND THE NIGHT DIM, OVER THE PAPER **AND** THE TOOLS ──────
        //
        // v434 drew this wash UNDER the chrome, on the reasoning that a tool you
        // cannot see is a tool you cannot find. The member's answer is that the
        // opposite is true at night: *"the night dim should also work on the
        // buttons etc"* — a dimmed page under undimmed white pills is the one
        // arrangement that makes the tools the brightest thing in a dark room,
        // which is exactly what a night dim is for.
        //
        // So it is drawn OVER the page AND the whole chrome — head pill, search
        // bar, foot pill and the pinned count alike — and UNDER the things a
        // member is actively working in: the selection's own bar, the mark dock,
        // the zones editor and every sheet. Those are drawn after it in this Box
        // (or outside it), which is what keeps them bright; it takes no pointer
        // input, so a tap still reaches the page it is dimming.
        // ── v440 — AND THE DIM'S OWN WHEN (see [ReaderLook.dimAuto]) ──
        //
        // v442 — AND THE WINDOW IS THE MEMBER'S NOW. v440 hung "at sunset" on the
        // phone's own dark theme, which follows the sky without this app ever
        // asking for a location — but the member asked for the other half of that
        // ("add at sunset customisation to be able to set the tiem"), and chose
        // **from / until**. So the dim comes on inside the window they set and
        // stays off outside it, and the window is remembered with the rest of the
        // look (see [ReaderLook.dimFromMinute]).
        //
        // The clock is TICKED rather than read once: a member reading at 19:59 with
        // the dim due at 20:00 would otherwise keep the page bright until something
        // else recomposed the reader (which, with the chrome gone, can be minutes).
        // A half-minute tick is plenty for a dim and costs one Int comparison.
        var readerClock by remember { mutableIntStateOf(readerMinuteOfDay()) }
        LaunchedEffect(ReaderLook.dimAuto) {
            if (!ReaderLook.dimAuto) return@LaunchedEffect
            while (true) {
                readerClock = readerMinuteOfDay()
                delay(30_000)
            }
        }
        val dimDue = !ReaderLook.dimAuto || ReaderLook.dimWindowContains(readerClock)
        val nightDim = if (dimDue) ReaderLook.dim else 0f
        if (nightDim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = nightDim))
            )
        }

        // ── v424 — AND THE ZONES' OWN EDITOR, ON THE PAGE ───────────────
        //
        // Drawn over everything (the chrome stands down while it is up, see the
        // call above) because the lines it places have to be seen against the
        // page they govern — and it takes every tap, so placing a line can never
        // turn a page by accident.
        AnimatedVisibility(
            visible = ReaderLook.zonesEditing,
            // v439 — a full-area overlay: a bare fade on the shared clock.
            enter = CurioMotion.arriveFade(),
            exit = CurioMotion.leaveFade()
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
        // ── v448 — THE MEANING, WHICH IS WHAT ONE WORD IS FOR ─────────────
        //
        // The member: *"separate the meaning dictionary icon from the dock and
        // place it as a floating pill above that dock when word is selected"*. A
        // single word swept on a page is the one time a meaning is the obvious
        // next thing, so it stands over the dock as its own pill and NAMES the
        // word it will answer for (see the pill below).
        val meaningWord = swept?.let { ReaderDictionary.headword(it.text) }.orEmpty()
        val singleWord = swept != null && meaningWord.length > 1 &&
            !swept.text.trim().contains(' ')
        if (swept != null && swept.text.isNotBlank() && sheet == null) {
            AnimatedVisibility(
                visible = true,
                // v439 — the selection bar is a floating pill off the page's
                // bottom edge: one arrival, like the foot it replaces.
                enter = CurioMotion.pillArrive(),
                exit = CurioMotion.pillLeave(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                // The passage's OWN ink, if it already carries a highlight: the
                // dock draws that swatch as taken, and a second press on it removes
                // the mark (v443). Read here rather than passed as a boolean because
                // the bar needs to know WHICH colour it is, not just that one exists.
                val wornMark = marks.firstOrNull {
                    it.isHighlight && it.positionIndex == swept.index
                }
                ReaderSelectionBar(
                    selection = swept,
                    palette = palette,
                    appliedInk = wornMark?.let { readerHighlighter(it.colorKey) },
                    onHighlight = { ink ->
                        selection = null
                        scope.launch {
                            // ── v443 — THE COLOUR IN HAND IS A SWITCH ────────
                            //
                            // Pressing the ink the passage already wears TAKES
                            // THE HIGHLIGHT BACK — the member's own answer for
                            // how a mark is deselected (*"when tappin git again
                            // the color it should deselect"*). Any other ink is a
                            // new mark, written by the same path it always was.
                            if (wornMark != null && wornMark.colorKey == ink.key) {
                                runCatching {
                                    PersonalRepositoryHolder.repo.deleteReaderMark(wornMark.id)
                                }
                            } else {
                                saveReaderMark(
                                    bookId = bookId,
                                    document = document,
                                    paragraph = swept.asParagraph(),
                                    kind = ReaderMarkKind.HIGHLIGHT,
                                    text = swept.text,
                                    colorKey = ink.key,
                                    // v457 — the swept run itself, so the wash
                                    // is drawn back where it was made.
                                    from = swept.from,
                                    to = swept.to
                                )
                            }
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
                    // ── v465 — THE PASSAGE'S OWN DICTIONARY DOOR ────────────
                    //
                    // Handed to the bar only when the selection is NOT one word,
                    // because one word has the pill above it (see [singleWord]):
                    // two doors to the same sheet, one of them anonymous, would
                    // make the row read as a mistake. The passage goes to the
                    // sheet as it is — the sheet's own candidate chips
                    // ([ReaderDictionary.wordsIn]) pick the word out of it, which
                    // is the same path the ⋯ menu's Dictionary tile takes for a
                    // whole paragraph — and the selection is deliberately LEFT UP
                    // behind the sheet, so closing it puts the member back on the
                    // words they asked about (the pill above does the same).
                    onDictionary = if (singleWord) {
                        null
                    } else {
                        {
                            dictionarySeed = swept.text
                            dictionarySearch = false
                            sheet = ReaderSheet.DICTIONARY
                        }
                    },
                    onMore = {
                        // The whole-place sheet is still here, one tap away:
                        // selecting words ADDS a way to mark a book up, it does
                        // not take the old one away.
                        marking = swept.asParagraph()
                        selection = null
                    }
                )
            }
        }

        // ── v448 — AND THE MEANING PILL, ABOVE THE DOCK ────────────────────
        //
        // Its own arrival and its own lift, standing where the reader's other
        // floating pills stand, and gone the moment the selection is: a word's
        // meaning is a door, not a state the dock has to carry.
        AnimatedVisibility(
            visible = singleWord && sheet == null,
            enter = CurioMotion.pillArrive(),
            exit = CurioMotion.pillLeave(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                // Clear of the dock's own 84dp (see [ReaderSelectionBar]): the two
                // are one stack, the pill over the tools.
                .padding(bottom = 92.dp)
        ) {
            val pillBody = lerp(palette.surface, palette.ink, 0.06f)
            val pillEdge = lerp(palette.surface, palette.ink, 0.16f)
            Surface(
                onClick = {
                    // The dictionary reads the selection itself (see the sheet
                    // call), so the pill does not hand the word over — and the
                    // selection stays up behind the sheet, so closing it puts the
                    // member back on the words they asked about.
                    dictionarySearch = false
                    sheet = ReaderSheet.DICTIONARY
                },
                shape = RoundedCornerShape(50),
                color = pillBody,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, pillEdge)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurioIcon(
                        CurioIcons.MenuBook,
                        "Look this word up",
                        tint = palette.accent,
                        size = 17.dp
                    )
                    Text(
                        meaningWord,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.ink
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        // ── v448 — AND THE CROP FRAME, ON THE PICTURE IT JUST TOOK ─────────
        //
        // The member: *"open a crop selection to be able to only select the
        // cropped image of that part of the book keeping the color as it is for the
        // selected … from there user can share it"*. It is the LAST thing in this
        // Box, so nothing the reader draws can sit over it, and it takes every
        // touch: the page under it cannot scroll or turn while the frame is up.
        val shot = snapshot
        if (shot != null) {
            ReaderSnapshotCrop(
                bitmap = shot,
                palette = palette,
                onDone = {
                    snapshot?.recycle()
                    snapshot = null
                    // The chrome the snapshot put away comes back with the frame's
                    // own exit, so the reader is exactly as the member left it.
                    chrome = true
                }
            )
        }

        // ── v437 — AND THE PAGE SLIDER, FLOATING OVER THE PAGE ─────────────
        //
        // Above the foot pill's own row (58dp of pill, 14dp of air, and the
        // navigation bar under it — see [ReaderBottomPill]), so the two never
        // fight for the same strip of the screen, and it comes up with the same
        // settle-in the reader's other floating controls use.
        val run = scrubber
        AnimatedVisibility(
            visible = scrubOpen && run != null,
            enter = fadeIn(tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Soften)) +
                slideInVertically(
                    tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Enter)
                ) { height -> -CurioMotion.settle(height) },
            exit = fadeOut(tween(CurioMotion.EXIT_MS.toInt(), easing = CurioMotion.Exit)) +
                slideOutVertically(
                    tween(CurioMotion.EXIT_MS.toInt(), easing = CurioMotion.Enter)
                ) { height -> -CurioMotion.settle(height) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = 84.dp)
        ) {
            if (run != null) {
                // The scrubber is outside [ReaderChrome] too — its own glass door.
                readerGlass.Provide {
                ReaderScrubPill(
                    scrubber = run,
                    palette = palette,
                    onDismiss = { scrubOpen = false }
                )
                }
            }
        }
    }

    // -- AND THE SWEEP STARTS WHEN THE TYPING STOPS (v431) ----------------
    //
    // The floating bar is typed into, so the sweep is asked for on a pause
    // rather than on every letter: a PDF's words cost a parse PER PAGE, and a
    // sweep restarted per keystroke would re-walk the file while the member was
    // still spelling the word. The ask is a token bump, which is exactly what
    // the sweep already listens for (Enter and the arrows bump it too).
    val askedQuery = searching?.query.orEmpty()
    LaunchedEffect(askedQuery, searching) {
        val run = searching ?: return@LaunchedEffect
        if (askedQuery.isBlank()) return@LaunchedEffect
        delay(320)
        run.token += 1
    }

    // -- THE SWEEP -------------------------------------------------------
    // Runs while a search is open, in the background, and hands the frame back
    // between pages (a PDF's words cost a parse to read, so a reader that
    // stuttered while it searched would have stopped being a reader). The hits
    // appear as they are found and the bar says how far the sweep has got.
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
                // v431 — THE FIRST FIND IS WHERE THE MEMBER IS TAKEN. A search
                // that found seventeen things and moved nothing would make the
                // arrows the only way in; the first hit is stepped onto the moment
                // it exists (see [searchStep]).
                if (run.current < 0 && found.isNotEmpty()) {
                    run.current = 0
                    pendingBlock = found[0].index
                }
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
                    // ── AND THE FIRST PAGE FOUND IS TURNED TO AT ONCE ──────
                    //
                    // A PDF's sweep is a parse per page, so it can take a while
                    // on a long file: waiting for the end of it before showing
                    // the member anything would make the search feel broken. The
                    // first hit steps onto its page as soon as it is found, and
                    // the arrows move on from there.
                    if (run.current < 0 && found.isNotEmpty()) {
                        run.current = 0
                        pendingPage = page
                        askedByReader = true
                    }
                    kotlinx.coroutines.yield()
                }
            }

            null -> Unit
        }
    }

    // ── v431 — THE SHEETS: ONE DOOR PER QUESTION ───────────────────────
    //
    // The old reader had ONE places sheet (progress + marks + contents) behind
    // one glyph, which was right while there was one button to hang it on. The
    // member split it back out ("lets separate the bookmarks again and it will be
    // 3rd option with bookmarks"), so each question has its own door and every
    // door opens a half-height sheet in the reader's own paper (see
    // [ReaderSheetFrame]). The shared callbacks live HERE, once, so the four mark
    // sheets cannot disagree about what a jump or a delete means.
    val keptMarks = marks.filter { !it.isPosition }
    val liveOrStored = marks.firstOrNull { it.isPosition } ?: openedAt
    val jumpToMarkNow: (ReaderMarkEntity) -> Unit = { mark ->
        sheet = null
        scope.launch { jumpToMark(mark.positionIndex) }
    }
    val deleteMarkNow: (ReaderMarkEntity) -> Unit = { mark ->
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { PersonalRepositoryHolder.repo.deleteReaderMark(mark.id) }
            }
        }
    }
    val pickBlockNow: (Int) -> Unit = { block ->
        sheet = null
        if (block >= 0) jumpToBlock(block)
    }
    val pickPageNow: (Int) -> Unit = { page ->
        sheet = null
        jumpToPage((page - 1).coerceAtLeast(0))
    }
    val bookmarkHereNow: (ReaderParagraph) -> Unit = { paragraph ->
        scope.launch {
            saveReaderMark(
                bookId = bookId,
                document = document,
                paragraph = paragraph,
                kind = ReaderMarkKind.BOOKMARK,
                text = paragraph.text.take(90)
            )
        }
    }
    val continueAtNow: (Int) -> Unit = { index ->
        sheet = null
        scope.launch { jumpToMark(index) }
    }

    // ── v465h — THE VOICE PICKER, FROM THE VOICE'S OWN BAR ─────────────
    //
    // Its own dialog rather than a `ReaderSheet`: the sheets are about the BOOK
    // (its places, its marks, its look), and this is about the voice reading it.
    // `onChanged` re-speaks the sentence being read in the new voice — see the
    // driver's `speakEpoch` — because a member who changes the voice mid-sentence
    // and hears nothing different for another twenty seconds concludes it did not
    // work.
    if (voiceSheet) {
        ReaderVoiceSheet(
            palette = palette,
            onDismiss = { voiceSheet = false },
            onChanged = {
                // v465i — a voice chosen by hand deserves a fresh attempt, the Edge
                // experiment included: a refusal earlier in the session is forgotten
                // the moment the member picks a voice themselves.
                ReadAloudSession.edgeUnavailable = false
                if (voiceOn && !voicePaused) {
                    // Stop first: the old voice's sentence is already in flight, and
                    // for the system engine that is a queue that would finish before
                    // the new sentence began.
                    ReaderSpeaker.stop()
                    NeuralSpeaker.stop()
                    EdgeVoice.stop()
                    speakEpoch++
                }
            }
        )
    }

    when (sheet) {
        ReaderSheet.APPEARANCE -> ReaderAppearanceSheet(
            palette = palette,
            // v406 — the type size and the face are a text book's business: a PDF
            // page is a picture of a page, and its own size is the pinch's.
            showType = content is ReaderContent.Text,
            // v434 — and that is exactly why a PDF gets a ZOOM row in its place.
            showZoom = content is ReaderContent.Pages,
            paged = when (content) {
                is ReaderContent.Pages -> ReaderLook.pageFlow == ReaderFlow.PAGED
                is ReaderContent.Text -> ReaderLook.textFlow == ReaderFlow.PAGED
                null -> false
            },
            onTogglePaged = {
                when (content) {
                    is ReaderContent.Pages ->
                        ReaderLook.pageFlow = ReaderLook.pageFlow.flipped()
                    is ReaderContent.Text ->
                        ReaderLook.textFlow = ReaderLook.textFlow.flipped()
                    null -> Unit
                }
            },
            onDismiss = { sheet = null }
        )

        // THE BOOK'S OWN ORDER — the member's 3-line button.
        ReaderSheet.CONTENTS -> ReaderPlacesSheet(
            mode = ReaderPlacesMode.CONTENTS,
            marks = marks,
            position = liveOrStored,
            live = livePlace,
            content = content,
            chapters = chapters,
            pages = printedPages,
            // v440 — what the paged text flow says is left of the chapter.
            sectionPagesLeft = sectionPagesLeft,
            palette = palette,
            onJump = jumpToMarkNow,
            onDelete = deleteMarkNow,
            onPickBlock = pickBlockNow,
            onPickPage = pickPageNow,
            onBookmarkHere = bookmarkHereNow,
            onContinueAt = continueAtNow,
            onDismiss = { sheet = null }
        )

        // THE PLACES THE MEMBER KEPT — its own door again (v431).
        ReaderSheet.BOOKMARKS -> ReaderPlacesSheet(
            mode = ReaderPlacesMode.BOOKMARKS,
            marks = marks,
            position = liveOrStored,
            live = livePlace,
            content = content,
            chapters = chapters,
            pages = printedPages,
            // v440 — what the paged text flow says is left of the chapter.
            sectionPagesLeft = sectionPagesLeft,
            palette = palette,
            onJump = jumpToMarkNow,
            onDelete = deleteMarkNow,
            onPickBlock = pickBlockNow,
            onPickPage = pickPageNow,
            onBookmarkHere = bookmarkHereNow,
            onContinueAt = continueAtNow,
            onDismiss = { sheet = null }
        )

        ReaderSheet.NOTES -> ReaderPlacesSheet(
            mode = ReaderPlacesMode.NOTES,
            marks = marks,
            position = liveOrStored,
            live = livePlace,
            content = content,
            chapters = chapters,
            pages = printedPages,
            // v440 — what the paged text flow says is left of the chapter.
            sectionPagesLeft = sectionPagesLeft,
            palette = palette,
            onJump = jumpToMarkNow,
            onDelete = deleteMarkNow,
            onPickBlock = pickBlockNow,
            onPickPage = pickPageNow,
            onBookmarkHere = bookmarkHereNow,
            onContinueAt = continueAtNow,
            onDismiss = { sheet = null }
        )

        ReaderSheet.HIGHLIGHTS -> ReaderPlacesSheet(
            mode = ReaderPlacesMode.HIGHLIGHTS,
            marks = marks,
            position = liveOrStored,
            live = livePlace,
            content = content,
            chapters = chapters,
            pages = printedPages,
            // v440 — what the paged text flow says is left of the chapter.
            sectionPagesLeft = sectionPagesLeft,
            palette = palette,
            onJump = jumpToMarkNow,
            onDelete = deleteMarkNow,
            onPickBlock = pickBlockNow,
            onPickPage = pickPageNow,
            onBookmarkHere = bookmarkHereNow,
            onContinueAt = continueAtNow,
            onDismiss = { sheet = null }
        )

        // ── THE ⋯ MENU ────────────────────────────────────────────────
        ReaderSheet.MENU -> ReaderMenuSheet(
            palette = palette,
            notes = keptMarks.count { it.isNote },
            highlights = keptMarks.count { it.markKind == ReaderMarkKind.HIGHLIGHT },
            gesturesOn = ReaderLook.tapZones,
            onGestures = {
                sheet = null
                ReaderLook.zonesEditing = true
            },
            onNotes = { sheet = ReaderSheet.NOTES },
            onHighlights = { sheet = ReaderSheet.HIGHLIGHTS },
            onDictionary = {
                // ── v449 — THE ⋯ MENU'S DICTIONARY IS THE PAGE ────────────
                //
                // The member, on this door: *"when the dictionary is opened from the
                // 3 dot one [it should be] more longer and let user search any word"*
                // — and then asked for its own page outright. The sheet keeps the
                // door it was built for (a SELECTION: it opens on the words that
                // were swept, and stays over the page they came from), while the ⋯
                // menu's door leaves the book for the page that is there to search
                // in.
                sheet = null
                navController.navigate(CurioRoutes.READER_DICTIONARY) {
                    launchSingleTop = true
                }
            },
            onSnapshot = {
                // ── v448 — THE SNAPSHOT, WITH THE CHROME OUT OF THE WAY ───
                //
                // The member: *"open a crop selection … it hides the dock and
                // header too"*. The pills leave FIRST — the capture waits for them
                // (see the armed effect) — the selection goes with them, and the
                // crop frame arrives on the picture a moment later.
                sheet = null
                selection = null
                chrome = false
                snapshotArmed = true
            },
            onSettings = {
                sheet = null
                readerSettingsOpen = true
            },
            onDismiss = { sheet = null }
        )

        // ── THE DICTIONARY ───────────────────────────────────────────
        ReaderSheet.DICTIONARY -> ReaderDictionarySheet(
            palette = palette,
            searchMode = dictionarySearch,
            // A selection that IS one word arrives ready to look up; a PASSAGE
            // arrives with the field EMPTY, because its own words are the chips
            // under it — seeding one of them would be the reader guessing which
            // word the member meant (see [ReaderDictionary.wordsIn]).
            initial = dictionarySeed.trim()
                .ifBlank { selection?.text?.trim().orEmpty() }
                .takeIf { it.isNotBlank() && !it.contains(' ') }
                .orEmpty(),
            // The passage a lookup came from — the mark dock's seed, or the words
            // just swept on the page — for the suggestions and the context line.
            passage = dictionarySeed.ifBlank { selection?.text.orEmpty() },
            onDismiss = {
                sheet = null
                dictionarySeed = ""
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
            // ── v434 — AND THE TWO THE SELECTION BAR ALWAYS HAD ─────────
            //
            // The member: "the dock that appears after i tap and hold well it
            // doesnt have the tools we had before for selections". A passage
            // chosen by holding a page is the same passage a sweep chooses, so
            // its dock offers the same doors: the dictionary and sharing the
            // words themselves. v442 — the dictionary is handed the WHOLE
            // passage: picking its first word out here was the reader guessing
            // which word the member meant (see [ReaderDictionary.wordsIn]).
            onDictionary = {
                dictionarySeed = paragraph.text
                dictionarySearch = false
                marking = null
                sheet = ReaderSheet.DICTIONARY
            },
            onShare = {
                marking = null
                shareReaderPlace(
                    context,
                    book?.title.orEmpty(),
                    positionLabel,
                    paragraph.text
                )
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

    // ── v431 — THE READING SETTINGS, OVER THE BOOK ──────────────────
    //
    // A full-screen page in the READER's own paper rather than the app's
    // settings family — the member's own instruction was "dont use settings style
    // use the reader style ui for it" — and drawn OVER the reading, so the book
    // stays the thing being set up and back puts the page exactly where it was.
    AnimatedVisibility(
        visible = readerSettingsOpen,
        // v439 — a full-screen page over the book, so it RISES rather than
        // settling: a sixth of the screen would read as a bounce for something
        // this tall, and a fade alone would ignore where it came from.
        enter = fadeIn(tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Soften)) +
            slideInVertically(tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Enter)) { it / 8 },
        exit = CurioMotion.leaveFade()
    ) {
        ReaderSettingsScreen(
            palette = palette,
            showType = content is ReaderContent.Text,
            showZoom = content is ReaderContent.Pages,
            paged = when (content) {
                is ReaderContent.Pages -> ReaderLook.pageFlow == ReaderFlow.PAGED
                is ReaderContent.Text -> ReaderLook.textFlow == ReaderFlow.PAGED
                null -> false
            },
            onTogglePaged = {
                when (content) {
                    is ReaderContent.Pages ->
                        ReaderLook.pageFlow = ReaderLook.pageFlow.flipped()
                    is ReaderContent.Text ->
                        ReaderLook.textFlow = ReaderLook.textFlow.flipped()
                    null -> Unit
                }
            },
            canPlaceZones = true,
            // The zones' lines belong on the PAGE, so the page has to be the
            // thing on screen: the settings step out of the way and the editor
            // comes up over the words it governs (see [ReaderTapZoneEditor]).
            onGestures = {
                readerSettingsOpen = false
                ReaderLook.zonesEditing = true
            },
            onBack = { readerSettingsOpen = false }
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
    /** v440 — what the paged flow says is left of the chapter (see [TextPagedReader]). */
    onSectionPagesLeft: (Int?) -> Unit,
    ownPages: Boolean,
    /** A block to land on, asked for from outside (a chapter, a search find). */
    pendingBlock: Int?,
    onPendingConsumed: () -> Unit,
    /**
     * v463 — THE RUN OF WORDS A VOICE IS ON, and whether the page should stand back
     * from everything that is not it (see the reader's own driver). Handed in rather
     * than read here because the voice belongs to the SCREEN: this surface draws the
     * mark, it does not own the cursor.
     */
    spoken: ReaderSpoken? = null,
    readingAloud: Boolean = false
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
            onSectionPagesLeft = onSectionPagesLeft,
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
            onPendingConsumed = onPendingConsumed,
            spoken = spoken,
            readingAloud = readingAloud
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
            .pointerInput(Unit) { detectTapGestures(onTap = { at -> onTap(at, size) }) }
            // ── v442 — AND A SIDE TAP IS ANSWERED BEFORE IT ──────────────
            //
            // Placed AFTER the detector above on purpose: the innermost handler
            // is the one that sees the finger lift first, so a zone tap gets its
            // answer at once and the double-tap wait above can never hold it back
            // or eat the next tap (see [readerZoneTaps]).
            .readerZoneTaps { point, area -> onTap(point, area) },
        // v434 — the side air is the MEMBER's now (see [ReaderLook.pageMargin]).
        contentPadding = PaddingValues(
            start = ReaderLook.pageMargin.dp,
            end = ReaderLook.pageMargin.dp,
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
                hitLength = hitLength,
                // v463 — the read-along mark belongs to the block it is a run OF.
                spokenRange = spoken?.takeIf { it.block == index }?.let { it.from..it.to },
                readingAloud = readingAloud
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
    onPageShown: (Int) -> Unit,
    /** v431 — the floating search's words, washed on every page they were found in. */
    query: String,
    /** v431 — the page the find being stood on is in, or -1. */
    currentHit: Int
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    // v422 — THE SIDEWAYS HALF OF A MAGNIFIED DOCUMENT. A page wider than the
    // screen has to be reachable (see the frame maths below), so the column
    // rides in a horizontal scroll of its own; a vertical drag still belongs to
    // the column, because a vertical scroller ignores a sideways one.
    val across = rememberScrollState()

    // ── v432 — A DOUBLE TAP'S CORRECTION, MADE AFTER THE RELAYOUT ───────────
    //
    // A double tap is one BIG step of the same zoom a pinch asks for in many
    // small ones, and that difference decides where its scroll correction can be
    // made: a pixel offset into a sheet only means a place at the zoom it was
    // measured at, so the correction waits here until the column has been laid
    // out at the new size (see [ReaderZoomAsk]).
    var zoomAsk by remember { mutableStateOf<ReaderZoomAsk?>(null) }
    LaunchedEffect(zoomAsk) {
        val ask = zoomAsk ?: return@LaunchedEffect
        // The sheet has to report its NEW height before the numbers mean
        // anything — a frame, or two, is all a relayout ever takes.
        var sheet = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == ask.index }
        var waited = 0
        while (waited < 3 && (sheet == null || sheet.size == ask.wasSize)) {
            withFrameNanos { }
            sheet = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == ask.index }
            waited += 1
        }
        // A sheet that never reports (it is several screens from where the list
        // settled) is still the sheet to open, so the new height is taken from
        // the scale the tap asked for — the file is proportional by design.
        val size = sheet?.size ?: (ask.wasSize * ask.ratio).roundToInt()
        // The tapped share of the sheet, put back under the same finger. A
        // negative ask (the taper near a sheet's head) lands on its top edge,
        // which is as close as a sheet can be put to the finger.
        val wanted = (ask.fraction * size - ask.viewportY).roundToInt()
        listState.scrollToItem(ask.index, wanted.coerceAtLeast(0))
        if (zoomAsk === ask) zoomAsk = null
    }

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
        // v430 — THE COLUMN'S SIDE AIR IN PIXELS, because the zoom's own gesture
        // now lives on the column and its anchor is said in the two spaces that
        // matter: the SURFACE's y (what the list's layout measures against) and
        // the SHEET's x (what the sideways pan is anchored on). A finger the
        // column reports at x is a finger `sidePad` px further into the sheet
        // than the sheet's own ruler says (see [readerZoomDocument]).
        val sidePad = with(LocalDensity.current) { 14.dp.toPx() }
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
                    detectTapGestures(
                        onTap = { at ->
                            onTap(Offset(at.x - across.value, at.y), viewport)
                        },
                        // ── v430 — AND THE FILE'S OTHER ZOOM DOOR, HERE TOO ────
                        //
                        // A double tap magnifies the whole file (v422), and its
                        // gesture now lives on this column rather than on each
                        // sheet (see the pinch below), so a double tap in the air
                        // BETWEEN two sheets is the file's as well. The point is
                        // said in the surface's own space — the column rides a
                        // horizontal scroll, so its x is the screen's x plus
                        // whatever has been panned away (the same correction the
                        // tap above makes).
                        onDoubleTap = { at ->
                            readerDoubleTapDocument(
                                // The tap's own correction (`- across.value`) is
                                // for the SCREEN's x; the zoom wants the sheet's,
                                // so the pan is deliberately not taken off here.
                                at = Offset(at.x - sidePad, at.y),
                                down = listState,
                                across = across,
                                onAnchor = { ask -> zoomAsk = ask }
                            )
                        }
                    )
                }
                // ── v442 — AND THE SIDE TAPS ARE THEIRS, AT ONCE ────────────
                //
                // The column's own taps are said in the SCREEN's space (it rides
                // a horizontal scroll), so the zone is measured there too — see
                // [readerZoneTaps].
                .readerZoneTaps(
                    key = viewport,
                    at = { point -> Offset(point.x - across.value, point.y) },
                    screen = { viewport }
                ) { point, area -> onTap(point, area) }
                // ── v430 — THE WHOLE SCREEN IS THE GESTURE ────────────────────
                //
                // The member: *"why the zoom is based on pages it should be for
                // the hole screen i mean the pin to zoom gesture is working when
                // its inside one page, and the gtlich is it weirdly scrolls"*.
                //
                // The pinch used to be armed on each SHEET, which is wrong twice
                // over. It did nothing in the air between two sheets (a 16dp gap,
                // or the margin above the first one), and — worse — its anchor
                // had to GUESS how much sheet sat above the fingers, from the
                // sheet's own shape: a number read in the COMPOSITION that armed
                // the gesture, which `pointerInput(key)` never refreshes. So the
                // whole pinch ran with the FIRST frame's value while the file kept
                // growing, and the correction fell further behind with every
                // event: the page slid under the fingers, the column scrolled
                // itself, and the file appeared to grow about a point far above
                // the hand.
                //
                // One handler on the COLUMN fixes both. Every point of the screen
                // is the document's, and the anchor is read from the list's own
                // LAYOUT at the moment of each event ([documentOffsetAt]) — no
                // assumption that pages share a shape, no stale capture, and
                // nothing page-shaped left in the arithmetic.
                .pinchToZoom(
                    key = document,
                    // v434 — AND A SWEEP OUTRANKS THE PAN (see [ReaderTouch.selecting]).
                    zoomed = {
                        ReaderLook.pdfZoomPage == -1 && ReaderLook.pdfZoom > 1.02f &&
                            !ReaderTouch.selecting
                    }
                ) { zoom, drag, focus ->
                    readerZoomDocument(
                        zoom = zoom,
                        drag = drag,
                        // x back into the sheet's own terms, y already the
                        // surface's (the column does not scroll vertically at
                        // the node level — its content does).
                        focus = Offset(focus.x - sidePad, focus.y),
                        down = listState,
                        across = across
                    )
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
            val bitmap by produceState<Bitmap?>(null, document, page, ReaderLook.lowPower) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        renderPdfPage(context, document, page, ReaderLook.lowPower)
                    }.getOrNull()
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
            // ── v430 — NOTHING PAGE-SHAPED IS LEFT IN THE ZOOM ─────────────
            //
            // v428b measured this sheet's magnified height and multiplied it by
            // the page number to say how much sheet sat above the fingers. It was
            // right in theory and wrong in practice: the value was read in the
            // composition that ARMED the gesture, and `pointerInput(key)` does not
            // re-arm when the value changes, so the whole pinch ran with the
            // number it started with while the file kept growing — the drift the
            // member reports ("it weirdly scrolls"). The anchor is now read from
            // the column's own LAYOUT, per event, in [documentOffsetAt].
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
                    //
                    // v430 — AND THE DOUBLE TAP STILL LIVES HERE, because a tap
                    // that lands ON a sheet is this handler's (the child sees the
                    // event first and consumes it, so the column's own detector
                    // never hears it). Its point is translated into the surface's
                    // space and handed to the same layout-read anchor the column's
                    // pinch uses.
                    .pointerInput(page, aspect, viewport) {
                        detectTapGestures(
                            onTap = { at -> onTap(at + where - surfaceOrigin, viewport) },
                            onDoubleTap = { at ->
                                readerDoubleTapDocument(
                                    // A sheet's own x is already the sheet's
                                    // ruler; only its y has to be said in the
                                    // surface's (which is what the list's layout
                                    // offsets are measured against).
                                    at = Offset(at.x, (at + where - surfaceOrigin).y),
                                    onAnchor = { ask -> zoomAsk = ask },
                                    down = listState,
                                    across = across
                                )
                            },
                            onLongPress = { if (words == null) onLongPress(page) }
                        )
                    }
                    // ── v442 — A SIDE TAP ON A SHEET, AT ONCE ──────────────
                    //
                    // The sheet's own frame is the DOCUMENT's ruler, so its tap is
                    // translated into the screen's space exactly as the tap above
                    // is — and the key is the page, never the translation, so a
                    // tap is never cancelled by the layout moving under it (see
                    // [readerZoneTaps]).
                    .readerZoneTaps(
                        key = page to viewport,
                        at = { point -> point + where - surfaceOrigin },
                        screen = { viewport }
                    ) { point, area -> onTap(point, area) }
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
                                    query = query,
                                    queryCurrent = currentHit == page,
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
    /**
     * v440 — HOW MANY PAGES OF THE CHAPTER THE MEMBER IS IN ARE STILL AHEAD.
     *
     * The member asked for the count in the progress card (*"'Pages left in this
     * chapter' in the progress card"*). It is reported from HERE rather than
     * computed by the card because the page ranges are this reader's own
     * invention: [paginateBlocks] breaks the book into ranges of BLOCKS, and
     * nothing outside this function can say which page a chapter reaches. Every
     * block carries its [ReaderBlock.section], so the chapter's own last page is
     * the last range holding a block of the section the current page opens in.
     *
     * `null` means "not known" — a page the reader cannot place — and the card
     * then simply says nothing rather than printing a zero it cannot stand behind.
     */
    onSectionPagesLeft: (Int?) -> Unit,
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
    onPendingConsumed: () -> Unit,
    /** v463 — the read-along mark and the page's own standing back (see [TextReader]). */
    spoken: ReaderSpoken? = null,
    readingAloud: Boolean = false
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var room by remember { mutableStateOf(IntSize.Zero) }
    // v434 — the break follows EVERY look setting that can change a measurement:
    // the type size, the leading, the margins and the paragraph gap.
    val pages = remember(
        content.blocks,
        room,
        ReaderLook.textScale,
        ReaderLook.lineSpacing,
        ReaderLook.pageMargin,
        ReaderLook.paraSpacing
    ) {
        paginateBlocks(content.blocks, room, ReaderLook.textScale, measurer, density)
    }

    LaunchedEffect(pages.size) { onPageCount(pages.size) }

    // ── v440 — AND WHAT IS LEFT OF THE CHAPTER (see [onSectionPagesLeft]) ──
    //
    // Pages AFTER this one, which is what "pages left" means to a reader: on the
    // chapter's final page the count is zero, and the card says so in words.
    LaunchedEffect(pages.size, pagerState.currentPage, content.blocks.size) {
        val section = pages.getOrNull(pagerState.currentPage)
            ?.let { range -> content.blocks.getOrNull(range.first)?.section }
        val chapterEnd = section?.let { here -> content.blocks.indexOfLast { it.section == here } }
        val lastPage = chapterEnd?.takeIf { it >= 0 }?.let { block ->
            pages.indexOfLast { block in it }
        }
        onSectionPagesLeft(
            if (lastPage == null || lastPage < 0) null
            else (lastPage - pagerState.currentPage).coerceAtLeast(0)
        )
    }

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
    //
    // `pages` is remembered on `ReaderLook.textScale` (see [paginateBlocks]
    // below), so every single pinch event re-measured EVERY block in the book —
    // which is the whole of the member's "lagging on pinch to zoom in epub".
    //
    // v426 — SO IT MAGNIFIES LIVE, SETTLING AT THE END (the member's own choice:
    // "live magnify, settle after"). The type size is what a reflowable book's
    // pages are MADE of, so re-making them per event is the stutter itself —
    // nothing is re-made while the fingers are down: the page is magnified in the
    // DRAW phase (`graphicsLayer`, which costs nothing), and the type size is
    // written ONCE when the fingers lift ([pinchToZoom]'s own `onEnd`). The
    // magnify is handed back at that same moment, so the page the member is left
    // with is the real page at its new size rather than a scaled copy of the old.
    var magnify by remember { mutableStateOf(1f) }
    val pagerZoom = Modifier.pinchToZoom(
        onEnd = {
            if (magnify != 1f) {
                ReaderLook.textScale = (ReaderLook.textScale * magnify).coerceIn(0.8f, 2.6f)
                magnify = 1f
            }
        }
    ) { zoom, _, _ ->
        magnify = (magnify * zoom).coerceIn(0.6f, 3.2f)
        Offset.Zero
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            // v426 — the live magnify of a pinch, drawn about the page's centre
            // (the settle puts the type where it belongs, so the anchor only has
            // to feel right while the fingers are down). The LAYOUT size is
            // untouched, which is what `room` below paginates against — a page
            // is measured at its real size, never at the magnified one.
            .graphicsLayer {
                scaleX = magnify
                scaleY = magnify
            }
            .then(pagerZoom)
            .onSizeChanged { room = it }
            .pointerInput(Unit) { detectTapGestures(onTap = { at -> onTap(at, size) }) }
            // v442 — the side tap answers at once (see [readerZoneTaps]).
            .readerZoneTaps { point, area -> onTap(point, area) },
        // v394 — PAGES SIT FLUSH. A gutter between self-made pages read as one
        // book cut into cards (user report: "the pages are not continuosn
        // connected"); with no gap a turn is a slide of the paper itself.
        pageSpacing = 0.dp,
        // v418 — THE NEXT PAGE IS ALREADY LAID OUT. Composing only the visible
        // page meant a turn painted its neighbour from scratch mid-slide, which
        // is the hitch a page turn used to show (member: "smoother page turns").
        //
        // v439 — UNLESS LOW POWER READING IS ON, in which case the neighbour is
        // exactly the cost that setting exists to remove: a second full render, a
        // second text extraction and a second set of marks for a page the member
        // may never turn to (see [ReaderLook.lowPower]).
        beyondViewportPageCount = if (ReaderLook.lowPower) 0 else 1
    ) { page ->
        val range = pages.getOrNull(page) ?: return@HorizontalPager
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ReaderLook.pageMargin.dp, vertical = 24.dp)
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
                    hitLength = query.length,
                    // v463 — the read-along mark belongs to the block it is a run OF.
                    spokenRange = spoken?.takeIf { it.block == index }?.let { it.from..it.to },
                    readingAloud = readingAloud
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
    // v434 — THE MEMBER'S OWN MARGINS. The break has to be measured against the
    // column the member will actually read, or a wider page would still break
    // where the old margins did (see [ReaderLook.pageMargin]).
    val side = with(density) { (ReaderLook.pageMargin * 2f).dp.roundToPx() }
    val vertical = with(density) { PAGE_VERTICAL_PADDING.roundToPx() }
    val width = room.width - side
    val height = room.height - vertical
    if (width <= 0 || height <= 0) return listOf(blocks.indices)

    val pages = ArrayList<IntRange>()
    var start = 0
    var used = 0
    blocks.forEachIndexed { index, block ->
        val gap = with(density) { (2.dp * ReaderLook.paraSpacing).roundToPx() }
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
        // ── v426 — A PAGE IS NEVER JUST A HEADING ──
        //
        // A heading is big type with margins of its own, so it can take most of a
        // page — and then the paragraph it NAMES is what tips the pair over, which
        // put the break exactly where it must not be: the heading alone on one
        // sheet (an empty page with the app's own title on it, in the member's
        // words: "the apps own title shows in a empty page") and the prose
        // starting again on the next. A heading belongs WITH what it names, so
        // while a page holds nothing but the heading the break waits, and the
        // paragraph joins it even if the page runs a line or two past its own
        // height — a page scrolls, and a turn is still a page.
        val aloneIsAHeading = start == index - 1 && blocks.getOrNull(start)?.isHeading == true
        if (used > 0 && used + needed > height && !aloneIsAHeading) {
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
    // v431 — ONE FAMILY FOR THE WHOLE PAGE (see the note in [ReaderParagraphBlock]).
    val family = readerTypeFamily(ReaderLook.typeFace)
    // v434 — AND THE MEMBER'S OWN LEADING, because a page is measured at the
    // height it will really be drawn at (see [ReaderLook.lineSpacing]).
    val leading = ReaderLook.lineSpacing
    return when (level) {
        1 -> TextStyle(
            fontFamily = family,
            fontSize = (23f * scale).sp,
            lineHeight = (31f * scale * leading).sp,
            fontWeight = FontWeight.SemiBold
        )
        2 -> TextStyle(
            fontFamily = family,
            fontSize = (20f * scale).sp,
            lineHeight = (27f * scale * leading).sp,
            fontWeight = FontWeight.SemiBold
        )
        3 -> TextStyle(
            fontFamily = family,
            fontSize = (18f * scale).sp,
            lineHeight = (26f * scale * leading).sp,
            fontWeight = FontWeight.Bold
        )
        else -> TextStyle(
            fontFamily = family,
            fontSize = (17f * scale).sp,
            lineHeight = (29f * scale * leading).sp
        )
    }
}

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
    /** v424 — the size of the screen the zones are measured on. */
    viewport: IntSize,
    /** v424 — where that screen starts, so a frame's own tap can be translated. */
    surfaceOrigin: Offset,
    onScrolled: (Boolean) -> Unit,
    flow: ReaderFlow,
    /** v389c — the live sweep, when it belongs to this page of the file. */
    selection: ReaderSelection?,
    onSelect: (ReaderSelection) -> Unit,
    /** v431 — the floating search: the words to wash, and the find being stood on. */
    query: String,
    currentHit: Int
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
            viewport = viewport,
            surfaceOrigin = surfaceOrigin,
            onScrolled = onScrolled,
            onLongPress = onLongPress,
            selection = selection,
            onSelect = onSelect,
            pendingPage = pendingPage,
            onPendingPageConsumed = onPendingPageConsumed,
            onPageShown = onPageShown,
            query = query,
            currentHit = currentHit
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
        // scratch mid-slide (see the reflowable pager above); v439 — stood down
        // while low power reading is on, for the same reason.
        beyondViewportPageCount = if (ReaderLook.lowPower) 0 else 1,
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
        val bitmap by produceState<Bitmap?>(null, document, page, ReaderLook.lowPower) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    renderPdfPage(context, document, page, ReaderLook.lowPower)
                }.getOrNull()
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
                    zoomed = {
                        ReaderLook.pdfZoomPage == page && ReaderLook.pdfZoom > 1.02f &&
                            !ReaderTouch.selecting
                    }
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
                }
                // v442 — the side tap answers at once, ahead of the double tap
                // this detector is holding its breath for (see [readerZoneTaps]).
                .readerZoneTaps(key = page to viewport.value) { point, area ->
                    onTap(point, area)
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
                                query = query,
                                queryCurrent = currentHit == page,
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
    hitLength: Int,
    /**
     * v463 — THE RUN A VOICE IS READING, as offsets into THIS block (null: not here).
     *
     * The mark is a wash on the words rather than a bar down the side, so it cannot be
     * confused with a highlight the member made (see the span pass below).
     */
    spokenRange: IntRange? = null,
    /** v463 — whether a voice is running, so the rest of the page stands back. */
    readingAloud: Boolean = false
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
    // v431 — THE TYPE IS THE MEMBER'S, THE SIZE AND THE WEIGHT ARE THE BOOK'S:
    // the chosen family is applied to every level, so choosing Fraunces or the
    // writing hand sets the WHOLE page and a heading stays a heading because its
    // size and weight say so.
    val family = readerTypeFamily(ReaderLook.typeFace)
    // v434 — THE MEMBER'S OWN LEADING AND ALIGNMENT (see [ReaderLook.lineSpacing]
    // and [ReaderLook.justify]). A heading keeps ragged lines whatever the body
    // does: justified display type is never what a book does.
    val leading = ReaderLook.lineSpacing
    val align = if (ReaderLook.justify && level == 0) TextAlign.Justify else TextAlign.Start
    val body = when (level) {
        1 -> TextStyle(
            fontFamily = family,
            fontSize = (23f * scale).sp,
            lineHeight = (31f * scale * leading).sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.ink
        )
        2 -> TextStyle(
            fontFamily = family,
            fontSize = (20f * scale).sp,
            lineHeight = (27f * scale * leading).sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.ink
        )
        3 -> TextStyle(
            fontFamily = family,
            fontSize = (18f * scale).sp,
            lineHeight = (26f * scale * leading).sp,
            fontWeight = FontWeight.Bold,
            color = palette.ink
        )
        else -> TextStyle(
            fontFamily = family,
            fontSize = (17f * scale).sp,
            lineHeight = (29f * scale * leading).sp,
            color = palette.ink
        )
    }.copy(textAlign = align)
    // ── v463 — AND THE REST OF THE PAGE STANDS BACK ────────────────────
    //
    // The wash is only half of "which sentence is being read": the other half is the
    // page agreeing not to compete with it. The block the voice is IN keeps its full
    // contrast (the wash alone says which sentence), and every other block steps back —
    // and only while the voice is actually RUNNING, so a paused session marks the place
    // without dimming the page the member has gone back to reading by eye.
    val standBack = readingAloud && spokenRange == null
    Column(
        modifier = Modifier
            .then(if (standBack) Modifier.alpha(READ_ALOUD_DIM) else Modifier)
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
                    // v434 — the member's own paragraph gap (see [ReaderLook.paraSpacing]).
                    Modifier.padding(
                        vertical = (if (block.isHeading) 12.dp else 6.dp) *
                            ReaderLook.paraSpacing
                    )
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
        val shown = remember(
            block.text,
            block.emphasis,
            query,
            hitHere,
            highlights,
            selection,
            spokenRange
        ) {
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
                // ── v457 — THE MARK'S OWN OFFSETS FIRST ─────────────────────
                //
                // The member: *"when highlighted the highlight goes to a
                // totally different line or text, but in highlight it shows
                // correctly the one i highlighted, but the view of highlight is
                // at a wrong text or line"*. A mark stores where its words are
                // now, so the wash is drawn AT the run that was swept. The
                // stored offsets are only trusted when the words they point at
                // are still the mark's own words (a re-imported file can move
                // them), and a mark from before v457 has none — both fall back
                // to the old search, which is the only answer either can give.
                val stored = if (passage.from >= 0 && passage.to >= passage.from &&
                    passage.to < block.text.length &&
                    block.text.substring(passage.from, passage.to + 1) == passage.text
                ) {
                    passage.from
                } else {
                    -1
                }
                val at = if (stored >= 0) {
                    stored
                } else if (passage.text.isBlank()) {
                    -1
                } else {
                    block.text.indexOf(passage.text)
                }
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
            // ── v463 — THE SENTENCE THE VOICE IS ON ──────────────────
            //
            // A WASH, not the bar the member's own highlights wear: a mark THEY made
            // draws a rule down the side of the paragraph it lives in, and a read-along
            // that borrowed that furniture would be mistaken for one of their own. It
            // is added BEFORE the find wash and the live selection below, so both paint
            // over it rather than under it — a search hit must never be hidden by the
            // voice, and a sweep the member is making is the more important of the two.
            spokenRange?.let { range ->
                val from = range.first.coerceIn(0, block.text.length)
                val to = (range.last + 1).coerceIn(from, block.text.length)
                if (to > from) {
                    spans.add(
                        ReaderTextSpan(
                            from,
                            to,
                            SpanStyle(background = palette.accent.copy(alpha = 0.18f))
                        )
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
    /** v431 — the compact count the middle button wears; blank with no pages. */
    pageLabel: String,
    /**
     * v438 — WHETHER THE PAGE SLIDER HAS THE FOOT TO ITSELF.
     *
     * The member: *"the page slider is bad too it should hide the dock hen the
     * slider shows"*. The scrubber is a pill of its own floating above the foot
     * pill, so with both up the page's own bottom carried two rows of controls
     * at once — and the count button that OPENED the slider was still sitting
     * there underneath it. The foot stands down while the slider is up, which is
     * the same bargain the journal's dock makes for its copy box.
     */
    footHidden: Boolean = false,
    /**
     * v439 — WHETHER THIS BOOK IS A PDF, for the motion lock pill.
     *
     * The lock is a PDF's business: a reflowable book's size is its TYPE (a real
     * re-lay-out, in the appearance sheet) and it has no pan to freeze, so the
     * pill would be a control over nothing (see [ReaderLook.motionLock]).
     */
    pdf: Boolean = false,
    /**
     * v440 — WHETHER A VOICE IS READING THIS BOOK (see [ReaderSpeaker]).
     *
     * Passed in rather than read here because the voice belongs to the SCREEN: this
     * component draws the pill, the reader owns the cursor and the engine.
     */
    speaking: Boolean = false,
    /**
     * v463 — WHETHER THE VOICE IS HOLDING ITS BREATH rather than gone.
     *
     * A paused session is still a session: the bar stays up (so there is always a way
     * to carry on) and the sentence keeps its mark. Only the word on the bar and the
     * fill of its disc change, which is why this is a second flag rather than a third
     * meaning bolted onto [speaking].
     */
    paused: Boolean = false,
    /** The speak bar's own door; null leaves the bar out entirely. */
    onToggleSpeak: (() -> Unit)? = null,
    /** v465h — the speak bar's voice door; null draws the bar without it. */
    onVoice: (() -> Unit)? = null,
    /** v463 — the four ways to move the voice (see [ReaderSpeakBar]); null hides them. */
    onSpeakPrevSentence: (() -> Unit)? = null,
    onSpeakNextSentence: (() -> Unit)? = null,
    onSpeakPrevChapter: (() -> Unit)? = null,
    onSpeakNextChapter: (() -> Unit)? = null,
    /** v431 — while the search bar is up the head hands its row over to it. */
    search: ReaderSearch?,
    onClose: () -> Unit,
    onSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onSearchStep: (Int) -> Unit,
    onAppearance: () -> Unit,
    onContents: () -> Unit,
    onPages: () -> Unit,
    onPinPages: () -> Unit,
    onBookmarks: () -> Unit,
    onMenu: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // ── v431 — THE HEAD IS A FLOATING PILL ─────────────────────────
        //
        // The head used to be an opaque band right across the top, which is a
        // wall over a page the member is reading. It is a PILL now, floating
        // clear of the paper: the way out, the book's own name, and the one door
        // a reader reaches without thinking (the member's request — "the upper
        // back button with the pdf namr of book make it floating pill style that
        // floating at the top", plus "put the search icon at the top right
        // corner").
        // ── v432 — AND IT COMES IN FROM ITS OWN EDGE ───────────────────
        //
        // The pills only faded before, a 1%-per-frame wash that read as a flinch
        // rather than a tool arriving (member: "its appear and disapper of the
        // tools"). Each one now SETTLES IN from the edge it lives on — the head
        // from above, the foot from below — so the reader can see which way the
        // chrome went, and the two never look like they blinked.
        // v435 — AND IT STEPS ASIDE, rather than blinking out, while the search
        // takes the row: the head shrinks toward the edge it came from so the
        // bar reads as growing OUT of it (see the search block below).
        // v437 — AND IT FADES, RATHER THAN FLINCHING.
        //
        // The member: *"for the floating title in pdf reader use smooth fade
        // animation"*. The head used to travel half its own height as it came and
        // went, which on a pill that hangs 18dp off the glass is a jump with a
        // fade attached — and two panels travelling at once is what the eye reads
        // as a swap. It SETTLES now: a six-of-its-height drift over a longer fade,
        // built on the slow-in/slow-out pair so the movement has no corner in it,
        // and on the way out it still slides toward the RIGHT edge the search pill
        // lives in, so opening the search reads as the head merging into the door
        // rather than as the head leaving (see the search block below).
        AnimatedVisibility(
            visible = visible && search == null,
            // ── v442 — ONE ARRIVAL, AND ONE EXIT PER REASON ────────────────
            //
            // The member: *"the upper header animation is clanky"*. The head had
            // three transitions on the way out AT ALL TIMES — a fade, a drift and a
            // sideways shrink toward the right edge — so simply HIDING the chrome
            // (a tap on the page, the commonest thing in the reader) collapsed the
            // name capsule into the corner WHILE it was leaving upward: two motions
            // for one intent, and the one gesture where the head should just go
            // away is where the movement looked busiest.
            //
            // The shrink belongs to exactly ONE moment: the search opening, where
            // it is what makes the bar read as growing out of the corner the search
            // icon lives in (v435's merge). So the head is asked which exit this is,
            // and the merging one runs on the ENTER clock — the same clock the bar
            // is arriving on — so the two are one movement rather than two panels
            // changing places (v437's one-clock rule, applied to the hand-off).
            enter = CurioMotion.pillArrive(fromTop = true),
            exit = if (search != null) {
                fadeOut(tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Exit)) +
                    slideOutVertically(
                        tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Enter)
                    ) { height -> CurioMotion.settle(height) } +
                    shrinkHorizontally(
                        tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Exit),
                        shrinkTowards = Alignment.End,
                        clip = false
                    )
            } else {
                CurioMotion.pillLeave(fromTop = true)
            },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopPill(title = title, palette = palette, onClose = onClose, onSearch = onSearch)
        }

        // ── v431 — AND THE SEARCH TAKES THAT ROW WHILE IT IS UP ────────
        //
        // One search door in the whole reader, and it is this one: the member
        // asked for the icon in the TOP RIGHT corner and for the ⋯ menu to stop
        // offering a second one ("add it in the 3dot menu remove the search as
        // search already got the optin in top right").
        //
        // v435 — AND IT GROWS OUT OF THE CIRCLE IT CAME FROM.
        //
        // The bar used to slide down from above like a second piece of chrome
        // arriving. It now EXPANDS from the right edge — the corner the search
        // pill lives in — while the name capsule shrinks away from the left, so
        // the one action is the search opening rather than two panels swapping
        // (member: "when opened it merges smoothly with the header for search").
        // v437 — AND THE TWO RUN ON ONE CLOCK.
        //
        // The head left in 200ms while the bar arrived in 300ms, so for a tenth
        // of a second the row had neither and the morph read as two panels
        // changing places. Both are 220ms on the same easing now, which is what
        // makes it ONE movement: the name collapsing into the right corner and the
        // bar growing out of it are the same 220ms (member: "for search pil use
        // merge and smooth morphe").
        AnimatedVisibility(
            visible = search != null,
            enter = fadeIn(tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Soften)) +
                expandHorizontally(
                    tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Enter),
                    expandFrom = Alignment.End,
                    clip = false
                ),
            // v442 — and it gives the row back on the same clock the head
            // returns on, so closing the search is one hand-off and not a beat of
            // empty row (see the head's note above).
            exit = fadeOut(tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Exit)) +
                shrinkHorizontally(
                    tween(CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Enter),
                    shrinkTowards = Alignment.End,
                    clip = false
                ),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val run = search
            if (run != null) {
                ReaderSearchBar(
                    run = run,
                    palette = palette,
                    onQuery = onSearchQuery,
                    onStep = onSearchStep,
                    onClose = onCloseSearch
                )
            }
        }

        // v431 — THE PAGE BAR IS THE SCRUBBER NOW. The middle button of the
        // foot pill opens it (a tap) and a HOLD on that button pins the counter,
        // so the reader's page control lives on the pill the member asked for
        // and never floats over the words on its own (see [ReaderScrubPill]
        // and [ReaderPinnedPage]).

        // ── v431 — AND THE FOOT IS ONE FLOATING PILL ───────────────────
        //
        // The foot used to be an opaque band across the page carrying six
        // controls and a word for a state. It is the pill the member asked for
        // now — "a proper pill shape not thin … proper pill with 5 buttons" — and
        // it floats clear of the paper with a real lift, five glyphs rather than
        // five sentences:
        //
        //   · Appearance — the type, its face, the paper and the two switches;
        //   · Contents   — the book's own chapters, each with its own bookmark;
        //   · Pages      — the count (tap for the scrubber, hold to pin it);
        //   · Bookmarks  — the places the member kept;
        //   · ⋯          — notes, highlights, the dictionary, share and settings.
        AnimatedVisibility(
            visible = visible && !footHidden,
            // v439 — ONE ARRIVAL (see [CurioMotion]): a fade on the enter clock
            // plus this pill's own six-of-height drift from the edge it lives on.
            // It used to travel HALF ITS HEIGHT, which is the one place in the
            // reader whose motion read as a slide rather than a settle.
            //
            // v442 — written as the tokens themselves rather than as their
            // numbers, so the head and the foot can never drift apart (rule 1 of
            // the pill clock, see [CurioMotion.pillArrive]).
            enter = CurioMotion.pillArrive(),
            exit = CurioMotion.pillLeave(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderBottomPill(
                palette = palette,
                pageLabel = pageLabel,
                onAppearance = onAppearance,
                onContents = onContents,
                onPages = onPages,
                onPinPages = onPinPages,
                onBookmarks = onBookmarks,
                onMenu = onMenu
            )
        }

        // ── v439 — THE MOTION LOCK, ABOVE THE FOOT'S RIGHT CORNER ──────────
        //
        // The member: *"what about just remove the zoom slider and instead add the
        // motuin lock as a pill above the dock at the right corner"* — so it sits
        // where a thumb reaches for it, clear of the foot pill's own row (58dp of
        // pill, 14dp of air, plus the navigation bar under it — see
        // [ReaderBottomPill]), and it belongs to the CHROME: it comes and goes with
        // the head and the foot, because a member reading with the tools away is
        // reading, not adjusting.
        //
        // Hidden while the page slider is up (the same `footHidden` the foot obeys)
        // because the slider's own pill now owns that strip of the page.
        AnimatedVisibility(
            visible = visible && pdf && !footHidden,
            enter = CurioMotion.popArrive(),
            exit = CurioMotion.popLeave(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 78.dp)
        ) {
            ReaderMotionLockPill(palette)
        }

        // ── v440 — AND THE VOICE, ABOVE THE FOOT'S LEFT CORNER ────────────
        //
        // The motion lock's own mirror: same 40dp pill, same floor, the other
        // corner, so the two never fight for a thumb. **It stays up while the chrome
        // is away** ([speaking] counts, not only [visible]) — the chrome hides itself
        // as the voice follows the book on, and a pause control that vanished with it
        // would leave the member with no way to stop the reading except closing the
        // book. It still obeys `footHidden`: while the page slider is up, that strip
        // of the page belongs to the slider.
        if (onToggleSpeak != null) {
            AnimatedVisibility(
                visible = (visible || speaking) && !footHidden,
                enter = CurioMotion.popArrive(),
                exit = CurioMotion.popLeave(),
                // v465h — the bar is the page's own width now, and centred: it
                // rests as a single disc in the middle of the foot (see
                // [ReaderSpeakBar]), and a disc that slid to one corner as it closed
                // would be a move nobody asked for.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    // Two calls, not one: `padding` has a SIDE overload
                    // (start/end/top/bottom) and a HORIZONTAL/VERTICAL one, and
                    // there is no overload taking `horizontal` and `bottom`
                    // together — the mixed form is a compile error, not a
                    // silently-ignored parameter.
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 78.dp)
            ) {
                ReaderSpeakBar(
                    palette = palette,
                    speaking = speaking,
                    paused = paused,
                    onToggle = onToggleSpeak,
                    onVoice = onVoice,
                    onPrevSentence = onSpeakPrevSentence,
                    onNextSentence = onSpeakNextSentence,
                    onPrevChapter = onSpeakPrevChapter,
                    onNextChapter = onSpeakNextChapter
                )
            }
        }
    }
}

/**
 * v463 — THE VOICE'S OWN BAR: where the voice is, and the four ways to move it.
 *
 * v440's pill had one glyph and one job (play, or pause) and it said what it was with its
 * SHAPE, which is right for a control that only ever does one thing. A member listening to
 * a book wants three more things a thumb can find without reading a label: back a
 * sentence, on a sentence, and an escape from a chapter they do not want. So the pill
 * became a bar — the chapter steps at the two ends (the bigger jump, so they wear the
 * up/down pair), the sentence steps either side of the mark, and the mark itself is the
 * one FILLED thing on it.
 *
 * It wears the accent while it is reading, so "is it still going" is answerable at a glance
 * from across the room, which is the only way it is ever asked. The state word stays
 * ("Listen" / "Reading" / "Paused") because a first-time member should not have to decode
 * a media bar to find out that the app can read to them at all.
 */
@Composable
private fun ReaderSpeakBar(
    palette: ReaderPalette,
    speaking: Boolean,
    paused: Boolean,
    onToggle: () -> Unit,
    onVoice: (() -> Unit)?,
    onPrevSentence: (() -> Unit)?,
    onNextSentence: (() -> Unit)?,
    onPrevChapter: (() -> Unit)?,
    onNextChapter: (() -> Unit)?
) {
    // v451 — the voice's bar refracts the page as well, tinted by its state (see
    // [ReaderMotionLockPill]).
    val glass = ambientGlassOn()
    val running = speaking && !paused
    val body = if (running) lerp(palette.surface, palette.accent, 0.30f) else palette.surface

    // ── v465h — IT RESTS AS ITS OWN DISC ─────────────────────────────────
    //
    // The member: *"it shouldnt always show the buttons etc"*. They are right, and
    // the reason is the surface rather than the controls: a full bar parked across
    // the foot of a page is a slab over the words for the whole of a chapter, which
    // is the opposite of what a reading page wants. So while it READS the bar closes
    // down to the one control still worth having in reach, and opens back up when
    // the member touches it. **While it is PAUSED it stays open**, because a paused
    // member is steering rather than listening — and the dwell only ever runs while
    // `running`, so a pause can never close the bar under a thumb that is using it.
    var expanded by remember { mutableStateOf(true) }
    LaunchedEffect(running, expanded) {
        if (!running || !expanded) return@LaunchedEffect
        delay(CurioMotion.Durations.SpeakRest.toLong())
        expanded = false
    }

    // THE MORPH IS THE WIDTH AND NOTHING ELSE. Both states are the same Surface at
    // the same place with the same height, and the disc sits in a slot of its own in
    // the middle of both — so the disc does not move a single pixel as the bar
    // breathes, and there is nothing for the eye to follow except the two edges
    // sweeping in. The overshoot is [CurioMotion.Springs.BouncyDp]: a width that
    // springs is what makes this read as the bar BOUNCING shut rather than as a
    // resize.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(SPEAK_BAR_HEIGHT)
    ) {
        val openWidth = maxWidth
        val width by animateDpAsState(
            targetValue = if (expanded) openWidth else SPEAK_BAR_HEIGHT,
            animationSpec = CurioMotion.Springs.BouncyDp,
            label = "readerSpeakBarWidth"
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = if (glass) Color.Transparent else body,
            shadowElevation = if (glass) 0.dp else 8.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .width(width)
                .curioAmbientGlass(body, shape = RoundedCornerShape(50))
        ) {
            // The clip is explicit: the word and the voice door are pinned to the
            // two ENDS of the bar, so during the sweep they must be cut off by the
            // surface's own edge rather than drawn over the page.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50))
            ) {
                // ── THE TRANSPORT, CENTRED, AND SYMMETRIC ABOUT THE DISC ──
                // The two step groups arrive as a pair, so the disc they flank stays
                // at the bar's centre all the way through — a morph that moved the one
                // control with weight would be a morph that moved the thing the member
                // is aiming at.
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(visible = expanded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            onPrevChapter?.let { step ->
                                ReaderSpeakStep(
                                    palette,
                                    CurioIcons.KeyboardArrowUp,
                                    "Previous chapter",
                                    step
                                )
                            }
                            onPrevSentence?.let { step ->
                                ReaderSpeakStep(
                                    palette,
                                    CurioIcons.ChevronLeft,
                                    "Previous sentence",
                                    step
                                )
                            }
                        }
                    }
                    // THE ONE CONTROL WITH WEIGHT: a filled disc, so a thumb finds it
                    // without reading the bar — and its own slot, so it is in exactly
                    // the same place whether the bar is open or closed.
                    Box(
                        modifier = Modifier.size(SPEAK_BAR_HEIGHT),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (running) palette.accent
                                    else palette.accent.copy(alpha = 0.16f)
                                )
                                .curioPressClickable(
                                    pressedScale = 0.90f,
                                    onClickLabel = when {
                                        !expanded -> "Show the reading controls"
                                        running -> "Pause the voice"
                                        speaking -> "Carry on reading aloud"
                                        else -> "Read this page aloud"
                                    },
                                    // ── WHAT A TAP ON THE CLOSED BAR DOES ──
                                    // It does what the glyph says AND opens the bar,
                                    // because a member who pauses is the member who is
                                    // about to steer: skip a sentence, change the voice,
                                    // leave a mark. The two are one tap rather than a
                                    // tap to open and a second to pause, and the closed
                                    // bar is never a control that lies about what it
                                    // will do.
                                    onClick = {
                                        if (!expanded) {
                                            expanded = true
                                            onToggle()
                                        } else {
                                            onToggle()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                if (running) CurioIcons.Pause else CurioIcons.PlayArrow,
                                null,
                                tint = if (running) palette.ink else palette.accent,
                                size = 17.dp
                            )
                        }
                    }
                    AnimatedVisibility(visible = expanded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            onNextSentence?.let { step ->
                                ReaderSpeakStep(
                                    palette,
                                    CurioIcons.ChevronRight,
                                    "Next sentence",
                                    step
                                )
                            }
                            onNextChapter?.let { step ->
                                ReaderSpeakStep(
                                    palette,
                                    CurioIcons.KeyboardArrowDown,
                                    "Next chapter",
                                    step
                                )
                            }
                        }
                    }
                }

                // ── WHERE IT IS, AT THE LEADING END ──────────────────────
                // The state word stays ("Listen" / "Reading" / "Paused") because a
                // first-time member should not have to decode a media bar to find out
                // that the app can read to them at all.
                AnimatedVisibility(
                    visible = expanded,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(
                        when {
                            running -> "Reading"
                            speaking -> "Paused"
                            else -> "Listen"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = palette.ink.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 18.dp, end = 6.dp)
                    )
                }

                // ── AND WHOSE VOICE, AT THE TRAILING END (v465h) ──────────
                // The door the member asked for: *"changing voice in that screen"*. A
                // voice is the one reading choice a member makes WHILE listening, and
                // it used to be four screens away from the thing they were listening
                // to. It is a glyph and one word rather than the voice's own name,
                // deliberately — naming it here would mean either binding a speech
                // engine or holding a pack's label, and a bar that shows a stale name
                // over the voice actually speaking is worse than one that says where
                // the choice lives (the picker itself names what is live).
                onVoice?.let { open ->
                    AnimatedVisibility(
                        visible = expanded,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .curioPressClickable(
                                    pressedScale = 0.92f,
                                    onClickLabel = "Change the reading voice",
                                    onClick = open
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurioIcon(CurioIcons.Tune, null, tint = palette.accent, size = 16.dp)
                                Text(
                                    "Voice",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = palette.ink.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The voice bar's own height — and its CLOSED WIDTH, which is the same number.
 *
 * The bar rests as a circle (see [ReaderSpeakBar]), so the disc's slot and the
 * surface's collapsed width have to be one measurement: derive them separately and
 * the closed bar becomes an ellipse.
 */
private val SPEAK_BAR_HEIGHT = 40.dp

/**
 * v463 — ONE STEP OF THE VOICE'S BAR, and the only one that is a plain glyph.
 *
 * 30dp of touch for a 17dp mark: the bar carries five controls in the room one pill used
 * to take, so each target is smaller than the 40dp a lone control gets — which is exactly
 * why the four steps are the ones a thumb may legitimately miss and the filled disc is
 * not. Each carries its own spoken label, so the bar is never a row of unlabelled arrows
 * to a screen reader.
 */
@Composable
private fun ReaderSpeakStep(
    palette: ReaderPalette,
    glyph: String,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .curioPressClickable(
                pressedScale = 0.90f,
                onClickLabel = label,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(glyph, null, tint = palette.ink.copy(alpha = 0.72f), size = 17.dp)
    }
}

/**
 * v435 — THE READER'S OWN TOP FLOOR.
 *
 * The reader HIDES the system status bar (see the note in the screen), so
 * `statusBarsPadding()` collapses to zero under it and the head pill used to
 * settle 8dp from the very edge of the glass — which on a phone with a camera
 * cut-out put it under the lens (member: "the header of the search and back and
 * title floating pill its too much close to the status bar").
 *
 * So the head does not ask the (hidden) status bar for room; it states its own:
 * whatever the display cut-out claims, plus this floor. A cut-out device gets
 * the cut-out's height, everything else gets a real margin, and neither case
 * depends on insets that are deliberately switched off.
 *
 * `@Composable`, and that is not decoration: `WindowInsets.Companion.displayCutout`
 * is a **@Composable getter** in this Compose version (it reads a composition-local
 * insets object rather than a plain value), so a plain `Modifier` extension that
 * touched it is a compile error — "functions which invoke @Composable functions
 * must be marked with the @Composable annotation". Every call site is inside a
 * `@Composable` body, which is why the annotation belongs here and not on the
 * insets read alone.
 */
private val ReaderChromeTopFloor = 18.dp

/**
 * v438 — `internal`, because reading settings is drawn INSIDE the reader and had
 * the same bug: `statusBarsPadding()` collapses to zero under a reader that hides
 * the status bar, so its own head settled on the glass (member: *"in settings the
 * header is again over the status bar"*). One floor, both heads.
 */
@Composable
internal fun Modifier.readerChromeTopInset(): Modifier = this
    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
    .padding(top = ReaderChromeTopFloor)

/**
 * v431 — THE HEAD, AS A FLOATING PILL.
 *
 * Way out, book's name, and the search door — the two controls a reader wants
 * while reading and the one piece of chrome that must never be a wall across the
 * page (see the note in [ReaderChrome]). The name is set in the member's own
 * reading type, so the chrome belongs to the page it floats over rather than to
 * the app's settings family.
 *
 * ── v435 — TWO PILLS, NOT ONE ──────────────────────────────────────────
 *
 * The search used to be the last glyph INSIDE the name pill, which made one
 * wide capsule carry two unrelated jobs. It is its own ROUND pill now, sitting
 * beside the name capsule with a gap between them (member: "separate the search
 * and the back and title pill, search icon is just a circle pill"). Same 50dp
 * height, same lift, same surface — two objects of one language, so the name
 * keeps the full width of the row and the search is a door you can hit without
 * aiming.
 */
@Composable
private fun ReaderTopPill(
    title: String,
    palette: ReaderPalette,
    onClose: () -> Unit,
    onSearch: () -> Unit
) {
    // ── v451 — THE HEAD REFRACTS THE PAGE ─────────────────────────────
    //
    // All three pills of the head wear the screen's ambient glass (see
    // [rememberCurioGlassScreen]): the fill goes TRANSPARENT and the lift goes
    // away so the glass is what draws the surface — its own refraction carries the
    // edge, and two shadows under one capsule was what made a glass pill read as a
    // smudge. With glass off nothing here changes.
    val glass = ambientGlassOn()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .readerChromeTopInset()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // ── v439 — WAY OUT IS ITS OWN PILL ────────────────────────────────
        //
        // The member: *"for pill use pill for th eback button its own pill"*.
        // The way out was the first glyph INSIDE the name capsule, so the one
        // control a member reaches for without looking shared its fill with a
        // label — and the two had to be the same width no matter which was
        // needed. It is a 50dp circle of its own now, the same object as the
        // search door at the other end, so the head reads as three pills of one
        // family: out, the book's name, and search.
        Surface(
            onClick = onClose,
            shape = CircleShape,
            color = if (glass) Color.Transparent else palette.surface,
            shadowElevation = if (glass) 0.dp else 10.dp,
            modifier = Modifier
                .size(50.dp)
                .curioAmbientGlass(palette.surface, shape = CircleShape)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(
                    CurioIcons.ArrowBack,
                    "Close the reader",
                    tint = palette.ink.copy(alpha = 0.85f),
                    size = 21.dp
                )
            }
        }
        // The book's name keeps the middle: it is the one thing in this row that
        // is a LABEL rather than a door, so it takes the room the two circles
        // leave and ellipsises inside it.
        Surface(
            shape = RoundedCornerShape(50),
            color = if (glass) Color.Transparent else palette.surface,
            shadowElevation = if (glass) 0.dp else 10.dp,
            modifier = Modifier
                .weight(1f)
                .curioAmbientGlass(palette.surface, shape = RoundedCornerShape(50))
        ) {
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = TextStyle(
                        fontFamily = readerTypeFamily(ReaderLook.typeFace),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.ink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 2.dp)
                )
            }
        }

        // The search, as its own circle: a door beside the name rather than a
        // second idea inside it.
        Surface(
            onClick = onSearch,
            shape = CircleShape,
            color = if (glass) Color.Transparent else palette.surface,
            shadowElevation = if (glass) 0.dp else 10.dp,
            modifier = Modifier
                .size(50.dp)
                .curioAmbientGlass(palette.surface, shape = CircleShape)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(
                    CurioIcons.Search,
                    "Search this book",
                    tint = palette.ink.copy(alpha = 0.8f),
                    size = 21.dp
                )
            }
        }
    }
}

/**
 * v431 — THE FOOT, AS ONE PILL WITH FIVE BUTTONS.
 *
 * Every control a reader needs on a page, in the order a thumb reaches them,
 * with the COUNT in the middle where the eye already is. `Surface` with a lift,
 * not a row on a band: the member asked for a pill that floats ("same for the
 * buttom tools make it floating, a proper pill shape not thin").
 */
@Composable
private fun ReaderBottomPill(
    palette: ReaderPalette,
    pageLabel: String,
    onAppearance: () -> Unit,
    onContents: () -> Unit,
    onPages: () -> Unit,
    onPinPages: () -> Unit,
    onBookmarks: () -> Unit,
    onMenu: () -> Unit
) {
    // ── v451 — THE FOOT REFRACTS THE PAGE (see [rememberCurioGlassScreen])
    val glass = ambientGlassOn()
    Surface(
        shape = RoundedCornerShape(50),
        color = if (glass) Color.Transparent else palette.surface,
        shadowElevation = if (glass) 0.dp else 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
            .curioAmbientGlass(palette.surface, shape = RoundedCornerShape(50), blurMultiplier = 1.2f)
    ) {
        Row(
            modifier = Modifier
                .height(58.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderPillButton(CurioIcons.FormatText, "Appearance", palette, onAppearance)
            ReaderPillButton(CurioIcons.Menu, "Contents", palette, onContents)
            ReaderPagesButton(
                pageLabel = pageLabel,
                palette = palette,
                onTap = onPages,
                onHold = onPinPages
            )
            ReaderPillButton(CurioIcons.Bookmark, "Bookmarks", palette, onBookmarks)
            ReaderPillButton(CurioIcons.MoreVert, "More", palette, onMenu)
        }
    }
}

/** One glyph of the foot pill. No label on screen — the glyph says it. */
@Composable
private fun RowScope.ReaderPillButton(
    glyph: String,
    label: String,
    palette: ReaderPalette,
    onClick: () -> Unit
) {
    // v439 — and the foot's glyphs press like the rest of the chrome: the foot
    // is where a member's thumb lands without looking, so it is the one row that
    // must answer a press while the page is still moving under it (see
    // [curioPressClickable]).
    Box(
        modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(RoundedCornerShape(50))
            .curioPressClickable(
                pressedScale = 0.88f,
                onClickLabel = label,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(glyph, label, tint = palette.ink.copy(alpha = 0.8f), size = 21.dp)
    }
}

/**
 * v439 — THE LOCKED PAGE, AS ONE PILL.
 *
 * The member asked for the motion lock to replace the Zoom slider, and for it to
 * be "a pill above the dock at the right corner". It is a glyph on a pill rather
 * than a word, like every other control in this reader, and it says its state
 * the way the journal's dock says a lit tool: **a locked page wears the reader's
 * own accent as a fill**, so the answer to "is my page held?" is readable from
 * across the room, and an unlocked one is the plain pill every other control is.
 *
 * The fill is an OPAQUE blend rather than a translucent wash — a shadowed pill
 * with a see-through fill lets the page's words bleed into it (the root rail's
 * own rule), and this pill floats directly over words.
 *
 * The label is spoken, not printed: the pill is the only thing on the page at
 * that corner, and a sentence there would be the one piece of prose on a screen
 * the member is reading.
 */
@Composable
private fun ReaderMotionLockPill(palette: ReaderPalette) {
    val locked = ReaderLook.motionLock
    // v451 — and it refracts too, TINTED BY ITS OWN STATE: the container handed to
    // the glass is the same accent blend the solid pill wears when locked, so a
    // held page still reads as held through the glass.
    val glass = ambientGlassOn()
    val body = if (locked) lerp(palette.surface, palette.accent, 0.30f) else palette.surface
    Surface(
        shape = RoundedCornerShape(50),
        color = if (glass) Color.Transparent else body,
        shadowElevation = if (glass) 0.dp else 8.dp,
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .curioAmbientGlass(body, shape = RoundedCornerShape(50))
            .curioPressClickable(
                pressedScale = 0.92f,
                onClickLabel = if (locked) "Let the page move again" else "Hold the page still"
            ) { ReaderLook.motionLock = !locked }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CurioIcon(
                if (locked) CurioIcons.Lock else CurioIcons.DragHandle,
                if (locked) "The page is held still" else "The page can be moved",
                tint = palette.ink.copy(alpha = 0.85f),
                size = 18.dp
            )
        }
    }
}

/**
 * THE MIDDLE BUTTON: THE COUNT, AND THE TWO THINGS IT CAN DO.
 *
 * A TAP opens the scrubber — one long drag from the first page to the last, with
 * the bar's own hold-to-turn arrows either side of it. A HOLD pins the count as
 * a small counter in the corner, where it stays while the chrome is away, for a
 * reader who keeps looking back at which page they are on (the member's request:
 * "when tapping the pages it opens the page scrubber, and holding the page pins
 * the page count as a small ounter in the corner").
 */
@Composable
private fun RowScope.ReaderPagesButton(
    pageLabel: String,
    palette: ReaderPalette,
    onTap: () -> Unit,
    onHold: () -> Unit
) {
    val pinned = ReaderLook.pinnedPage
    Box(
        modifier = Modifier
            .weight(1.6f)
            .height(42.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.accent.copy(alpha = if (pinned) 0.26f else 0.15f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onHold() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CurioIcon(CurioIcons.MenuBook, null, tint = palette.accent, size = 17.dp)
            if (pageLabel.isNotBlank()) {
                Text(
                    pageLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = palette.ink,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * v431 — THE FLOATING SEARCH, AT THE TOP, WITH THE FIND IT IS STANDING ON.
 *
 * The member asked for the icon in the top right corner and for a proper bar
 * under it: the field, how many finds there are and which one you are on, the two
 * arrows that step through them, and the cross. The finds themselves are drawn ON
 * the pages for a PDF (see [PdfPageTextLayer]) and washed on the paragraphs for a
 * reflowable book, so "next" moves the page under the highlight rather than
 * naming it in a list.
 */
@Composable
private fun ReaderSearchBar(
    run: ReaderSearch,
    palette: ReaderPalette,
    onQuery: (String) -> Unit,
    onStep: (Int) -> Unit,
    onClose: () -> Unit
) {
    // The keyboard comes up with the bar: a search that needs a second tap before
    // it can be typed into is not a search. One shot, on the way in.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val progress = if (run.hits.isNotEmpty()) {
        "${run.current + 1}/${run.hits.size}"
    } else if (!run.done && run.total > 0) {
        "\u2026"
    } else {
        ""
    }
    // v451 — the search bar is the fourth pill of the reader's own family, so it
    // refracts the page like the head, the foot and the lock.
    val glass = ambientGlassOn()
    Surface(
        shape = RoundedCornerShape(50),
        color = if (glass) Color.Transparent else palette.surface,
        shadowElevation = if (glass) 0.dp else 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            // The same floor the head stands on, so the bar lands exactly where
            // the name capsule was instead of jumping a few dp up.
            .readerChromeTopInset()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .curioAmbientGlass(palette.surface, shape = RoundedCornerShape(50), blurMultiplier = 1.2f)
    ) {
        Row(
            modifier = Modifier
                .height(50.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurioIcon(
                CurioIcons.Search,
                null,
                tint = palette.ink.copy(alpha = 0.5f),
                size = 18.dp,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
            )
            BasicTextField(
                value = run.query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                    fontSize = 15.sp,
                    color = palette.ink
                ),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onStep(1) }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (run.query.isEmpty()) {
                            Text(
                                "Find in this book",
                                style = TextStyle(fontSize = 15.sp),
                                color = palette.ink.copy(alpha = 0.35f)
                            )
                        }
                        inner()
                    }
                }
            )
            if (progress.isNotBlank()) {
                Text(
                    progress,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = palette.ink.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            ReaderChromeButton(CurioIcons.KeyboardArrowUp, "The find before", palette) {
                onStep(-1)
            }
            ReaderChromeButton(CurioIcons.KeyboardArrowDown, "The find after", palette) {
                onStep(1)
            }
            ReaderChromeButton(CurioIcons.Close, "Close the search", palette) { onClose() }
        }
    }
}

/**
 * v431 — THE PAGE COUNT, PINNED IN THE CORNER (see [ReaderPagesButton]).
 *
 * It is drawn OUTSIDE the chrome, so it survives the chrome leaving: that is the
 * whole point of pinning it. A tap takes the pin back out.
 */
@Composable
private fun ReaderPinnedPage(
    pageLabel: String,
    palette: ReaderPalette,
    onUnpin: () -> Unit
) {
    // v451 — the pin refracts the page like every other pill of the chrome; the
    // glass is what keeps it reading as a pill once the chrome it left behind is
    // gone.
    val glass = ambientGlassOn()
    Surface(
        onClick = onUnpin,
        shape = RoundedCornerShape(50),
        color = if (glass) Color.Transparent else palette.surface,
        shadowElevation = if (glass) 0.dp else 8.dp,
        modifier = Modifier
            .readerChromeTopInset()
            // Clear of the head row, which owns the first ~76dp below the floor.
            .padding(top = 70.dp, end = 14.dp)
            .curioAmbientGlass(palette.surface, shape = RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(CurioIcons.Bookmark, null, tint = palette.accent, size = 13.dp)
            Text(
                pageLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = palette.ink,
                maxLines = 1
            )
        }
    }
}

/**
 * v431 — THE SCRUBBER: ONE DRAG ACROSS THE WHOLE BOOK.  v437 — AND IT FLOATS.
 *
 * The old page bar could only step, one page per tap or a held arrow — which is
 * fine for the next page and hopeless for page 180 of 300. A scrubber is the
 * other half of the same job, and the two arrows are still here (they are the
 * hold-to-turn [ReaderHoldButton] the bar always had), so nothing the reader
 * could do before is gone.
 *
 * ── v437 — SMALL, FLOATING, AND NOT A SHEET ──────────────────────────────
 *
 * The member: *"the page scrobble slider it needs to be similair to the dock
 * small floaating without the buttom sheet so its easier todo the page scrbbing
 * faster. a buttom pill floating at the buttom with the slider and hides when tap
 * on page, also a way to close it"*.
 *
 * It was a half-height MODAL sheet: a scrim over the book, a panel across the
 * bottom, a slider inside it — three gestures to choose a page, on the one control
 * whose entire point is speed, and the page being scrubbed to was covered by the
 * panel while the member scrubbed to it. It is a PILL now, floating over the
 * page's own foot where the thumb already is (above the foot pill, see the
 * caller), with the count INSIDE it as the slider is dragged and a cross to put it
 * away. A tap anywhere on the page closes it as well (see [tapPage]) — the member's
 * own rule, and the same tap that already means "get out of the way".
 *
 * The two arrows are the hold-to-turn ones from the old sheet, one on each side of
 * the slider, so the pill is the whole of the page bar: drag for a long jump, hold
 * to walk a page at a time. Nothing was dropped in the move.
 */
@Composable
private fun ReaderScrubPill(
    scrubber: ReaderScrubber,
    palette: ReaderPalette,
    onDismiss: () -> Unit
) {
    // The thumb is LOCAL until the drag ends, so the reading surface is asked for
    // a page once per gesture instead of once per pixel of travel (see
    // `onValueChangeFinished`).
    var dragged by remember(scrubber.at) { mutableIntStateOf(scrubber.at) }
    val last = scrubber.total.coerceAtLeast(1)
    // ── v441 — A PILL, NOT A SHADOW ─────────────────────────────────────────
    //
    // The member: *"page slider ui is bad with tha weird shadow"*. It was the
    // palette's `surface` (F5F0E8) floating over its `paper` (FBF6EC) — two
    // colours about two per cent apart — so the only part of the capsule the eye
    // could actually see was its shadow, spread over pale paper on every side: a
    // floating smudge with a slider in it rather than a pill standing on the
    // page. Three changes, all in the language the rest of the reader's pills
    // already speak:
    //
    //  · the fill is a real, OPAQUE blend of the surface toward the ink, edged
    //    with a hairline, so the capsule has a body of its own wherever the
    //    shadow falls (and, being opaque, the shadow cannot bleed through it —
    //    see the shadow rules in app/AGENTS.md);
    //  · the lift is the PINNED PAGE's 8dp, not the largest number in the reader;
    //  · `animateContentSize` is gone. The pill is full width and its height
    //    never changes, so it animated nothing — it only asked for a layout pass
    //    on every frame of every arrival.
    val body = lerp(palette.surface, palette.ink, 0.06f)
    val edge = lerp(palette.surface, palette.ink, 0.16f)
    // ── v451 — THE SCRUBBER REFRACTS THE PAGE ───────────────────────────
    //
    // It is the pill the member scrubs to a page ON, so it is the one that most
    // wants the page visible through it. Its own hairline edge is kept (it is what
    // says "capsule" over pale paper — the v441 note) and the lift goes to the glass.
    val glass = ambientGlassOn()
    Surface(
        // 28dp, the dock's own radius: it is a capsule at this height and it
        // never arcs a slider thumb out of its track.
        shape = RoundedCornerShape(28.dp),
        color = if (glass) Color.Transparent else body,
        shadowElevation = if (glass) 0.dp else 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, edge),
        modifier = Modifier
            .fillMaxWidth()
            .curioAmbientGlass(body, shape = RoundedCornerShape(28.dp), blurMultiplier = 1.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReaderHoldButton(
                glyph = CurioIcons.ChevronLeft,
                label = "The page before",
                palette = palette,
                step = scrubber.onPrev
            )
            // A book with one page has nothing to scrub THROUGH, and a slider
            // whose range is a single value is a divide by zero wearing a thumb
            // (a one-page PDF is the honest case). The arrows still work.
            if (last > 1) {
                Slider(
                    value = dragged.coerceIn(1, last).toFloat(),
                    onValueChange = { next -> dragged = next.roundToInt() },
                    onValueChangeFinished = { scrubber.onScrub(dragged.coerceIn(1, last)) },
                    valueRange = 1f..last.toFloat(),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = lerp(body, palette.ink, 0.18f)
                    )
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            ReaderHoldButton(
                glyph = CurioIcons.ChevronRight,
                label = "The next page",
                palette = palette,
                step = scrubber.onNext
            )
            // The count follows the THUMB, not the settled page: a scrubber that
            // named the old page while the finger was elsewhere would be the one
            // thing on it that is not the answer to the question asked.
            //
            // And it is a FIXED slot, right-aligned (v441). It used to be
            // whatever width its own digits needed, in a row beside a slider that
            // had the weight — so every time the number gained a digit the track
            // next to it got narrower, the thumb moved with it, and the page
            // under the member's own finger changed for no reason they could see.
            Text(
                "$dragged / $last",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = palette.ink.copy(alpha = 0.75f),
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.width(72.dp)
            )
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(34.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.Close,
                        "Close the page slider",
                        tint = palette.ink.copy(alpha = 0.75f),
                        size = 18.dp
                    )
                }
            }
        }
    }
}

/**
 * v434 — THE ⋯ MENU: SIX CAPSULE DOORS IN A GRID, GLYPH FIRST.
 *
 * The member redrew this page in their own words: "use proper pill shape grid
 * with just capsulepills in a 6 grid with huge icon and a small text below
 * instead of share a passage just share, reading settings to settings, tap zones
 * to gestures". So the six rows are six TILES — a big glyph and the shortest name
 * that still says what it does — in two rows of three, which is also what makes
 * a door reachable with a thumb instead of a careful aim.
 *
 * The names are the member's: Share (not "Share a passage"), Gestures (not "Tap
 * zones") and Settings (not "Reading settings"). A door that leads somewhere with
 * a count wears it as a small mark on the corner of its glyph, and Gestures wears
 * a lit mark while the zones are on, so the grid still answers "are they on?"
 * without a word of state.
 */
@Composable
private fun ReaderMenuSheet(
    palette: ReaderPalette,
    notes: Int,
    highlights: Int,
    /** v434 — whether the page's zones answer a tap (the Gestures tile's mark). */
    gesturesOn: Boolean,
    onGestures: () -> Unit,
    onNotes: () -> Unit,
    onHighlights: () -> Unit,
    onDictionary: () -> Unit,
    /** v448 — the crop-and-share door (see [ReaderSnapshot]). */
    onSnapshot: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    // ── v442 — SIZED TO ITS OWN TILES, AND CLEAR OF THE BOTTOM ───────
    //
    // The member: *"the 3 dot for reader its too empty spae and not proper
    // spaced, fix its weird look"*, and, asked which way to go: "scrink it but
    // dont make it too lose to th buttom". The 45% floor every sheet wears (see
    // [ReaderSheetFrame]) is right for a LIST of marks, which grows — but a grid
    // of six fixed tiles is six tiles tall whatever else happens, so the panel
    // was two thirds empty paper with a row of pills at the top of it.
    //
    // So this sheet's floor is the height its own grid needs (30% of the screen on
    // a phone — still a real panel, still standing clear of the foot of the glass,
    // which is the other half of what the member asked for) and its rows are given
    // air: a wider gap between the tiles, a wider one between the rows, and a
    // breath under the last row so nothing sits jammed against the panel's edge.
    //
    // ── v448 — A REAL RHYTHM: REAL GAPS, REAL GUTTERS ─────────────────
    //
    // The member, again: *"still the 3 dot menu isnt good the padding etc and
    // spacing feels off"*. The rows are given a real gap (18dp) and the tiles a
    // real gutter (12dp), so the grid reads as one object instead of a row of
    // pills jammed into the top of an empty panel. The panel's floor grows with
    // its own content (0.38).
    //
    // ── v452 — SIX DOORS, TWO FULL ROWS, AND NO SHARE ─────────────────
    //
    // The member: *"from the 3 dot menu remove the share button"*. Share is the
    // SELECTION's door and it stays there (the wide capsule toolbar still offers
    // it, and Snapshot beside it crops what you are looking at) — what is gone is
    // a Share tile that answered for a passage whether or not there was one. That
    // leaves six doors, and SIX IS TWO ROWS OF THREE: Settings moves up into the
    // second row rather than sitting alone in a row of one, so every row is full
    // and every tile is the same width (a two-tile row beside three-tile rows
    // would have made the survivors half again as wide — see [ReaderTileRow]).
    ReaderSheetFrame("More in this book", palette, onDismiss, minHeightFraction = 0.38f) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ReaderTileRow(
                tiles = listOf(
                    ReaderTile(CurioIcons.Note, "Notes", notes, false, onNotes),
                    ReaderTile(CurioIcons.FormatHighlight, "Highlights", highlights, false, onHighlights),
                    ReaderTile(CurioIcons.MenuBook, "Dictionary", 0, false, onDictionary)
                ),
                palette = palette
            )
            ReaderTileRow(
                tiles = listOf(
                    ReaderTile(CurioIcons.Screenshot, "Snapshot", 0, false, onSnapshot),
                    ReaderTile(CurioIcons.Crop, "Gestures", 0, gesturesOn, onGestures),
                    ReaderTile(CurioIcons.Settings, "Settings", 0, false, onSettings)
                ),
                palette = palette
            )
            Spacer(Modifier.height(2.dp))
        }
    }
}

/** v434 — one door of the ⋯ grid: its glyph, its name, and its mark. */
private class ReaderTile(
    val glyph: String,
    val label: String,
    val count: Int,
    /** A lit mark for a state (the zones) rather than a count. */
    val lit: Boolean,
    val onClick: () -> Unit
)

/**
 * One row of the ⋯ grid, three capsules wide.
 *
 * [pad] puts an invisible tile-width of air on EACH side, so a row that holds
 * fewer tiles than the others keeps the same tile width and reads as centred
 * (v448 — the grid is three rows of three, three and one).
 */
@Composable
private fun ReaderTileRow(tiles: List<ReaderTile>, palette: ReaderPalette, pad: Int = 0) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        // v442/v448 — the tiles keep a real gutter between them: at 8dp two 46dp
        // pills nearly touched once their labels were the widest thing in the row.
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(pad) { Spacer(Modifier.weight(1f)) }
        tiles.forEach { tile ->
            ReaderMenuTile(tile = tile, palette = palette, modifier = Modifier.weight(1f))
        }
        repeat(pad) { Spacer(Modifier.weight(1f)) }
    }
}

/**
 * A TILE: a capsule with a big glyph over a short name.
 *
 * Big glyph, small name, no other furniture — the member's "less text … more icon
 * based button style". The count (or the state dot) sits in the glyph's own
 * corner rather than as a trailing word, which is what keeps the tile quiet.
 */
@Composable
private fun ReaderMenuTile(
    tile: ReaderTile,
    palette: ReaderPalette,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = tile.onClick,
            shape = RoundedCornerShape(50),
            color = palette.ink.copy(alpha = 0.06f),
            // ── v438 — A PILL WITH THE GLYPH IN IT, AND THE NAME UNDER IT ──
            //
            // The member: *"the 3 dot in pdf buttom sheet still looks bad with
            // huge buttons, and keep the button inside the pil keep the text out
            // of it"*. v437 shrank a 94dp tile to 68 and left the NAME inside the
            // capsule under the glyph, so the pill was tall and the name sat in
            // the fill like a second control. The capsule holds the GLYPH and
            // nothing else — a real 46dp pill, the height the reader's own
            // controls are — and the name is a label UNDERNEATH it, outside the
            // fill, which is where a label belongs.
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box {
                CurioIcon(tile.glyph, null, tint = palette.accent, size = 22.dp)
                when {
                    tile.count > 0 -> Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 7.dp, y = (-4).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(palette.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${tile.count}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = palette.paper,
                            maxLines = 1
                        )
                    }

                    tile.lit -> Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-2).dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette.accent)
                    )
                }
            }
            }
        }
        Text(
            tile.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = palette.ink.copy(alpha = 0.78f),
            maxLines = 1
        )
    }
}

@Composable
internal fun ReaderChromeButton(
    glyph: String,
    label: String,
    palette: ReaderPalette,
    onClick: () -> Unit
) {
    // ── v439 — THE READER PRESSES BACK ────────────────────────────────
    //
    // The member's own pick: *"Press feedback everywhere"*. A reader's chrome
    // was a row of controls that answered a tap with nothing but the thing they
    // did — on a surface that hides its own chrome, that is a tap a member makes
    // twice because the first one looked like it missed. This is the reader's
    // ONE control (the head, the search bar and the foot are all built from it),
    // so the feedback lands everywhere at once, and it is the app's own press
    // helper rather than a second one written here (see [curioPressClickable]):
    // the same dip, the same haptic every other Curio control gives.
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .curioPressClickable(
                pressedScale = 0.86f,
                onClickLabel = label,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(glyph, label, tint = palette.ink.copy(alpha = 0.75f), size = 19.dp)
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
    // ── v441 — THE ARROW MUST ASK THE PAGE IT IS ON ────────────────────
    //
    // The handler is built once (key `Unit`) so a press-and-hold is never
    // cancelled by a recomposition — which also means the `step` it was built
    // with is the one it would keep calling forever: a closure that still
    // believed the reader was on the page the arrow was FIRST built at. The
    // standard remedy, and one this file already uses elsewhere: read the ask
    // through state that composition keeps current, not through the closure.
    val liveStep = rememberUpdatedState(step)
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
                                liveStep.value()
                                delay(PageTurnHoldRepeatMs)
                            }
                        }
                        // Suspends until the finger lifts (or the gesture is
                        // cancelled) — which is what stops the metronome.
                        tryAwaitRelease()
                        job.cancel()
                    },
                    onTap = { if (!held[0]) liveStep.value() }
                )
            }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(glyph, label, tint = palette.ink.copy(alpha = 0.75f), size = 19.dp)
        }
    }
}

/**
 * v434 — A READER'S SHEET: AS TALL AS IT NEEDS, SCROLLABLE, AND DRAGGABLE SHUT.
 *
 * The member asked for a real bottom sheet ("make the ui smooth stbale buttom
 * sheet which smoothly collapse or anything the height stays half of the
 * screen"), and then for the two things it was missing ("the buttom sheet isnt
 * scrollable and its not able to close with swipe so fix these"). It is built
 * here rather than borrowed from Material 3 on purpose: the reader runs with the
 * system bars hidden, and a dialog-backed sheet brings its own window back over
 * the page. So this is the same idea, hand-made —
 *
 *  · the height WRAPS its content and stops at 60% of the screen, so a short
 *    sheet is not half-empty paper and a tall one scrolls its own words;
 *  · ONE scroll lives here, in the body — callers pass plain content and never
 *    wrap it in their own (a scroll inside a scroll is a scroll nobody can use);
 *  · a downward drag ANYWHERE moves the sheet: the body takes it while it can
 *    still scroll up and the leftover moves the sheet ([pull]), so the swipe the
 *    member reached for works from the content and not just from the handle;
 *  · it rises on a tween and the scrim fades with it.
 *
 * `appear` and `drag` are read in a deferred `offset`/`graphicsLayer` lambda, so
 * a drag or an opening animation is a LAYOUT pass and never a recomposition of
 * everything inside the sheet.
 */
@Composable
internal fun ReaderSheetFrame(
    title: String,
    palette: ReaderPalette,
    onDismiss: () -> Unit,
    /**
     * v437 — HOW MUCH OF THE SCREEN THE SHEET KEEPS EVEN WHEN IT HAS LITTLE TO
     * SAY, as a fraction of the screen's height (0 = pure wrap).
     *
     * The member: *"the highloght and notes dropd won is so small"*. Both are
     * LISTS of marks, so both shrank to whatever two rows they happened to hold
     * and a reader with nothing kept yet met a strip of paper with one line in it —
     * a panel that size reads as a mistake rather than as an empty list. The two
     * places sheets keep a real panel's floor now ([ReaderPlacesSheet] passes it),
     * while a sheet that genuinely has one line to say (a dictionary entry, the
     * mark sheet) still wraps and stays small.
     *
     * ── v440 — AND "STAYS SMALL" WAS WRONG, SO EVERY SHEET HAS A FLOOR NOW ──
     *
     * The member, after living with it: *"the 3 dot buttom sheet is so small same
     * for the discounary buttom sheet"*. A wrap-sized panel under a page the member
     * was reading does not read as "exactly as big as it needs" — it reads as a
     * strip that appeared at the foot of the screen, and both sheets they named are
     * short by nature (six tiles; one word and its senses). The floor is the whole
     * answer: **every reader sheet keeps 45% of the screen**, which is what the
     * member asked for in the first place ("the height stays half of the screen"),
     * and a sheet with more to say still grows to the 60% cap and scrolls inside it.
     * Pass a different fraction for a specific sheet; pass 0 only if a sheet is ever
     * meant to be a strip.
     *
     * ── v442 — AND THE ONE EXCEPTION IS A FIXED GRID ────────────────
     *
     * The member: *"the 3 dot for reader its too empty spae and not proper spaced,
     * fix its weird look"*. A floor is right for a LIST — a list grows, and a floor
     * is what stops two kept marks reading as a broken panel — but the ⋯ menu is six
     * fixed tiles and is the same height whatever happens, so 45% of the screen was
     * two thirds empty paper. [ReaderMenuSheet] passes **0.30f**: still a real panel
     * standing clear of the foot of the glass, never a strip.
     */
    minHeightFraction: Float = 0.45f,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val appear = remember { Animatable(0f) }
    // ── v439 — AND THE SHEET LEAVES BY ITS OWN HEIGHT ────────────────────
    //
    // The member: *"still the pdf reader buttom sheet close is weirdly slow
    // looking"*. It was travelling `(1 - appear) * capPx` where `capPx` is 60% of
    // THE SCREEN — so a five-row sheet, which is 200dp tall, slid more than twice
    // its own height to leave, and the slow-looking part was all that empty
    // travel below it. It is measured now and leaves by exactly its own height.
    //
    // (The cap stays as the fallback for the one frame before measurement, when
    // there is no height to travel by yet.)
    var measuredHeight by remember { mutableIntStateOf(0) }
    // Note what is NOT here: the sheet's STRUCTURE. It still travels by exactly
    // its own measured height (v439's fix) and the drag still drives `drag`
    // directly with no clock at all — a finger on the sheet must never be
    // interpolated.
    LaunchedEffect(Unit) {
        appear.animateTo(
            1f,
            tween(durationMillis = CurioMotion.ENTER_MS.toInt(), easing = CurioMotion.Enter)
        )
    }
    var drag by remember { mutableFloatStateOf(0f) }
    // ── v442 — A SHEET SHUTS THE WAY A SHEET SHUTS EVERYWHERE ────────────
    //
    // The member, again: *"still the buttom sheet closing is bad"*. Two things
    // were wrong with it, and neither was the clock:
    //
    //  · **A flick did not count.** The only door out was DISTANCE — 108dp of
    //    deliberate dragging — so the gesture everyone actually makes on a sheet,
    //    a quick downward flick, dragged a few millimetres and sprang back. The
    //    finger's own travel per millisecond is measured now — in the head's drag
    //    and in the body's nested scroll — and a throw past `dismissFling` below
    //    shuts the sheet without the pull having to reach the distance.
    //  · **The distance was a long way to pull** for a panel only a few rows tall,
    //    so it is 96dp now — and a pull that does not reach it springs straight
    //    back on the exit clock rather than sitting half-way down.
    //
    // A re-entrancy guard rides with them, so the flick and the settle that
    // follows it cannot both dismiss the same sheet.
    val closing = remember { booleanArrayOf(false) }
    // The drag that means "shut", in the sheet's own pixels.
    val dismissPull = remember(density) { with(density) { 96.dp.toPx() } }
    // The speed that means "shut" on its own — a flick, not a drag.
    val dismissFling = remember(density) { with(density) { 620.dp.toPx() } }
    // The pull's own speed and the moment it was last measured. Plain arrays, not
    // Compose state: nothing draws these, and a state write on every frame of a
    // drag would recompose the whole sheet for a number the eye never sees.
    val flickPeak = remember { floatArrayOf(0f) }
    val flickAt = remember { longArrayOf(0L) }
    val body = rememberScrollState()
    /**
     * Shut, and QUICKLY.
     *
     * 200ms in, 120ms out: leaving is the system getting out of the way, and the
     * member's report was that the reader's sheets were slow to go. The fade is
     * shorter than the travel it hides on purpose — the eye needs the sheet GONE, not
     * a performance of it going.
     */
    fun close() {
        if (closing[0]) return
        closing[0] = true
        scope.launch {
            appear.animateTo(
                0f,
                tween(durationMillis = CurioMotion.EXIT_MS.toInt(), easing = CurioMotion.Exit)
            )
            onDismiss()
        }
    }
    /** Put the sheet back where it was — unless the pull was far enough to shut it. */
    fun settle() {
        // FAR ENOUGH, OR FAST ENOUGH (v442). The member's gesture is a quick
        // downward throw, and a throw that travelled a few millimetres still meant
        // "shut this" — so the peak speed the finger reached counts as a dismissal
        // on its own, exactly as it does on the handle (see the note on
        // [dismissFling]).
        if (drag > dismissPull || (drag > 0f && flickPeak[0] > dismissFling)) {
            close()
            return
        }
        if (drag <= 0f) {
            // Nothing was pulled: whatever speed this gesture reached belonged to a
            // drag that came back, and must not be spent on the next one.
            flickPeak[0] = 0f
            flickAt[0] = 0L
            return
        }
        scope.launch {
            val anim = Animatable(drag)
            anim.animateTo(
                0f,
                tween(durationMillis = CurioMotion.EXIT_MS.toInt(), easing = CurioMotion.Enter)
            ) {
                drag = value
            }
        }
    }
    // ── v434 — THE BODY SCROLLS FIRST, AND THE SHEET GOES NEXT ─────────
    //
    // The sheet used to be shut by dragging the HANDLE strip only, which nobody
    // reaches for — the member's report was exact ("its not able to close with
    // swipe"). A downward drag anywhere belongs to the sheet now, and it is the
    // body's until the reading has no more to give: the scrollable content takes
    // the drag while it can still scroll up, and only the leftover moves the
    // sheet. That split is the one thing a `NestedScrollConnection` is for, and
    // it is built here — rather than borrowed from Material 3 — so the sheet
    // keeps the reader's own paper, ink and type.
    val pull = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f) {
                    // v442 — the speed of the pull, read from the finger's own clock
                    // (the nested scroll reports travel and no velocity). The PEAK
                    // is what counts: a throw is fastest in its last frame.
                    val now = SystemClock.uptimeMillis()
                    val at = flickAt[0]
                    if (at != 0L) {
                        val since = (now - at).coerceAtLeast(1L)
                        flickPeak[0] = maxOf(flickPeak[0], available.y / since * 1000f)
                    }
                    flickAt[0] = now
                    drag = (drag + available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                if (available.y < 0f && drag > 0f) {
                    // The finger is on its way back UP with the sheet part-way
                    // down: it closes the sheet before it scrolls anything.
                    val take = available.y.coerceAtLeast(-drag)
                    drag += take
                    return Offset(0f, take)
                }
                return Offset.Zero
            }

            // NOTE, v442: there is deliberately NO `onPostFling` here. The speed
            // read above is what dismisses a flick on the body; the velocity
            // handler is a second, engine-versioned way to learn the same thing,
            // and the reader does not need two (see the note on [dismissFling]).
        }
    }
    // A body drag has no "end" of its own to hang the settle on, and the nested
    // scroll's own stop callback is not in the API this reader builds against —
    // so the leftover drag settles once the finger has stopped moving for a
    // moment, which is also what makes a slow, deliberate drag feel anchored.
    // v437 — AND THE SETTLE IS QUICK: 150ms of waiting was a tenth of a second the
    // member spent watching a sheet sit half-way down before it decided.
    LaunchedEffect(Unit) {
        snapshotFlow { drag }.debounce(CurioMotion.SETTLE_DEBOUNCE_MS).collect { settle() }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // ── v444 — AND THE SHEET RIDES ABOVE THE KEYBOARD ───────────
            //
            // The member: *"fix the search box hiding below the keyboard for
            // dictionary"*. Every sheet in the reader sits on the bottom of the
            // screen, so a sheet with a FIELD in it had its field under the keys
            // the moment they came up. The inset is taken HERE rather than on the
            // sheet's own surface: the sheet is bottom-aligned inside this box, so
            // shrinking the box lifts the whole panel clear of the keyboard — and
            // the wrap/cap floors below are measured against what is actually
            // left, so a tall sheet never measures itself into the keys.
            .imePadding()
    ) {
        // ── v434 — AS TALL AS IT NEEDS, UP TO A CAP ─────────────────────
        //
        // A fixed half-screen left a short sheet half empty and gave a long one
        // a body that had nowhere to scroll. The sheet now WRAPS its content and
        // stops at 60% of the screen; anything past that scrolls inside it, and
        // the swipe above shuts it from the body. (Member's choice: "wrap
        // content, cap at ~60%.")
        val cap = maxHeight * 0.6f
        val floor = maxHeight * minHeightFraction
        val capPx = with(density) { cap.toPx() }
        // What the sheet actually travels: its own height once it has one (see
        // the note on [measuredHeight]), the cap until then.
        val travel = if (measuredHeight > 0) measuredHeight.toFloat() else capPx
        // THE SCRIM: a wash, not a wall — the page stays legible under it.
        // ── v440 — DRAWN, NOT LAID OUT IN A LAYER ─────────────────────────
        //
        // It used to fade with `graphicsLayer { alpha = appear.value }`, which
        // asks the renderer for an OFFSCREEN LAYER the size of the screen and
        // re-blends it every frame of the sheet's arrival and departure — on a
        // full-screen scrim, that is the most expensive way to change a wash's
        // opacity there is, and it is at the exact moment the member is watching
        // for smoothness (their report: the app "feels clunky and not smooth").
        // The alpha is now drawn in the DRAW phase, so the fade costs a rect and
        // never a recomposition or a layer.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(color = Color.Black, alpha = 0.42f * appear.value)
                }
                .pointerInput(Unit) { detectTapGestures { close() } }
        )
        Surface(
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = palette.paper,
            shadowElevation = 16.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = floor, max = cap)
                .onSizeChanged { measuredHeight = it.height }
                // ── v457 — THE SHEET MOVES IN A LAYER, NOT IN LAYOUT ────────
                //
                // This was an `offset { }`, which is read in the LAYOUT phase:
                // every frame of a drag (and every frame of the arrival and the
                // departure) re-laid-out the whole panel — its title, its senses,
                // its rows — and re-rendered its 16dp shadow with it. The member's
                // report was that the read's sheets still felt clunky, and this is
                // the cost inside them. `graphicsLayer { translationY }` moves the
                // sheet's own layer instead: the same pixels, no relayout, and the
                // shadow travels with the layer it belongs to.
                .graphicsLayer {
                    translationY = drag + (1f - appear.value) * travel
                }
                // The body's over-scroll and the sheet's own drag are one
                // gesture (see [pull]).
                .nestedScroll(pull)
                // ── v444 — THE FINGER LEAVING IS THE END OF THE DRAG ─────────
                //
                // The member: *"the swipe down to close is buggy it stays as an
                // overlay for some time"*. The body's drag has no "end" of its
                // own — a nested scroll reports travel and stops — so the only
                // thing that ever settled it was the debounce timer below, which
                // meant the sheet SAT half-way down for the whole wait after the
                // finger was already gone. This watches the raw pointer stream
                // and settles the instant no finger is left down, so a swipe that
                // was a dismissal is one (on the same flick the head already
                // reads) and one that was not springs straight back.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(
                                androidx.compose.ui.input.pointer.PointerEventPass.Final
                            )
                            if (drag > 0f && event.changes.none { it.pressed }) settle()
                        }
                    }
                }
                // THE SHEET SWALLOWS A TAP, so tapping INSIDE it never dismisses
                // it: the scrim below is a sibling, and a tap nothing in the sheet
                // claims would reach it and shut the sheet the member is using.
                // A child (a button, a row) still gets the event FIRST and keeps
                // it, so this only eats the taps on the sheet's own blank paper.
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // THE HANDLE AND THE TITLE ARE STILL A DRAG TARGET of their own
                // — the body has the nested scroll now, and the head has this.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            // ── v442 — AND THE HANDLE KNOWS HOW FAST IT IS MOVING ──
                            //
                            // `detectVerticalDragGestures` reports travel and no
                            // velocity, so the speed is read from the finger's own
                            // clock: the last frame's travel over the time it took
                            // (see [dismissFling]). A finger that STOPS before it
                            // lifts reports ~0 and simply springs back, which is
                            // what a deliberate half-drag is.
                            var lastAt = 0L
                            var speed = 0f
                            detectVerticalDragGestures(
                                onDragStart = {
                                    lastAt = 0L
                                    speed = 0f
                                },
                                onDragEnd = {
                                    if (drag > dismissPull || speed > dismissFling) close()
                                    else drag = 0f
                                },
                                onDragCancel = { drag = 0f },
                                onVerticalDrag = { change, travel ->
                                    val now = change.uptimeMillis
                                    if (lastAt != 0L) {
                                        val since = (now - lastAt).coerceAtLeast(1L)
                                        speed = travel / since * 1000f
                                    }
                                    lastAt = now
                                    drag = (drag + travel).coerceAtLeast(0f)
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(palette.ink.copy(alpha = 0.22f))
                        )
                    }
                    Text(
                        title,
                        style = TextStyle(
                            fontFamily = readerTypeFamily(ReaderLook.typeFace),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.ink
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                // ── THE BODY SCROLLS (v434) ─────────────────────────────
                //
                // One scroll lives here and nowhere else, so a short sheet is
                // short and a tall one scrolls its own words instead of clipping
                // them — and so the nested scroll above has exactly one child to
                // hear. Callers pass plain content; they no longer wrap it.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp)
                        .verticalScroll(body)
                ) {
                    content()
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/**
 * v434 — APPEARANCE: THE WHOLE LOOK, IN THE READER'S OWN CAPSULE LANGUAGE.
 *
 * The member's own list, in their own order ("one with appearncae with A- A+ witha
 * slider to adjust the text size, below the font option only 3 in a row then
 * below 5 differnt backgroud color the paper, sepia, night white and 2 more, and
 * then belo 2 toggle with one auto rotate and another ith the horizontal
 * option"). Five swatches is what fits a phone at a comfortable tap size; the
 * sixth tile unfolds the tuned papers instead of crowding them (see
 * [ReaderSkin.extra]).
 *
 * ── WHAT v434 CHANGED, AND WHY ─────────────────────────────────────────
 *
 *  · A PDF HAS A SIZE CONTROL NOW. The type row is a reflowable book's, and a
 *    PDF's size is its own zoom — so a PDF gets a ZOOM slider in the same slot
 *    (member: "in apperance its missing, the text size or zoom slider").
 *  · THE SWITCHES ARE SEGMENTS. "Auto-rotate" was a switch that could only say
 *    auto-or-not (and so could never reach "wide"), and "Horizontal pages" was
 *    a switch for a question with two named answers. Both are now animated
 *    segmented pills — the member's own direction ("instead of toggle use proper
 *    2 opton style with animation").
 *  · AND THE REST OF THE LOOK IS HERE: the leading, the margins, the paragraph
 *    gap, the alignment, keeping the screen awake and the night dim. They are
 *    the member's own list of what a reader expects (see [ReaderLook]).
 *
 * The type controls are a REFLOWABLE book's business: a PDF page is a picture of
 * a page. They are absent for a PDF rather than present and inert, and the layout
 * rows (leading, margins, paragraphs, alignment) go with them.
 */
@Composable
private fun ReaderAppearanceSheet(
    palette: ReaderPalette,
    /** Whether a type size (and the layout controls) mean anything here. */
    showType: Boolean,
    /**
     * v434 — whether this is a PDF, for the page control it needs instead.
     *
     * v439 — that control is the MOTION LOCK now, not a Zoom slider: a slider was
     * how a member set the page's size, which the pinch already does better, and
     * what they actually wanted afterwards was for the page to stay (see
     * [ReaderLook.motionLock]).
     */
    showZoom: Boolean,
    /** v431 — whether this book is being read as PAGES, for the mode segment. */
    paged: Boolean,
    onTogglePaged: () -> Unit,
    onDismiss: () -> Unit
) {
    var moreInks by remember { mutableStateOf(false) }
    // v442 — WHICH END OF THE DIM'S WINDOW IS BEING SET (see [ReaderClockRow]).
    var clockPick by remember { mutableStateOf("") }
    ReaderSheetFrame("Appearance", palette, onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── THE ONE SIZE CONTROL THIS PAGE HAS (v434) ───────────────
            //
            // A reflowable book's size is its TYPE, and a PDF's is its ZOOM —
            // and only one of the two exists for any given book. It used to be
            // absent for a PDF entirely, which is the member's "in apperance its
            // missing, the text size or zoom slider".
            if (showType) {
                ReaderSliderRow(
                    label = "Text size",
                    value = ReaderLook.textScale,
                    range = 0.8f..2.6f,
                    step = 0.08f,
                    valueLabel = "${(ReaderLook.textScale * 100f).roundToInt()}%",
                    palette = palette,
                    onValue = { next -> ReaderLook.textScale = next.coerceIn(0.8f, 2.6f) },
                    leadingGlyph = CurioIcons.TextDecrease,
                    leadingLabel = "Smaller type",
                    trailingGlyph = CurioIcons.TextIncrease,
                    trailingLabel = "Larger type"
                )
            }

            // ── v439 — THE ZOOM SLIDER IS GONE; THE LOCK REPLACES IT ──────
            //
            // The member: *"in pdf only remove that zoom slider and add the motion
            // lock pill which restrits that drag to move and pinch to zoom it locks
            // in the state the user left the zoom position"*. A slider was the
            // wrong control for a PDF page in the first place: the page is a
            // picture, the pinch is how a reader sizes it, and what they actually
            // want afterwards is for it to STAY — which is the lock, and it is
            // here as well as on the page's own pill so a member who locked a page
            // by accident can undo it from the place they look for settings
            // (see [ReaderLook.motionLock] for what it freezes and where it is
            // enforced).
            if (showZoom) {
                ReaderSheetLabel("Moving the page", palette)
                ReaderSegmentRow(
                    segments = listOf(
                        ReaderSegment("Held still", CurioIcons.Lock),
                        ReaderSegment("Free", CurioIcons.DragHandle)
                    ),
                    selectedIndex = if (ReaderLook.motionLock) 0 else 1,
                    palette = palette,
                    onSelect = { at -> ReaderLook.motionLock = at == 0 }
                )
            }

            // ── THE TYPE ────────────────────────────────────────────────
            if (showType) {
                ReaderSheetLabel("Typeface", palette)
                ReaderSegmentRow(
                    segments = ReaderTypeFace.entries.map { ReaderSegment(it.label) },
                    selectedIndex = ReaderTypeFace.entries.indexOf(
                        ReaderTypeFace.of(ReaderLook.typeFace)
                    ),
                    palette = palette,
                    onSelect = { at ->
                        ReaderTypeFace.entries.getOrNull(at)?.let { ReaderLook.typeFace = it.key }
                    }
                )
            }

            // ── THE PAPER ───────────────────────────────────────────────
            ReaderSheetLabel("Page", palette)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ReaderSkin.primary.forEach { skin ->
                    ReaderInkSwatch(skin, palette, Modifier.weight(1f))
                }
                ReaderMoreInkTile(palette, open = moreInks, modifier = Modifier.weight(0.8f)) {
                    moreInks = !moreInks
                }
            }
            if (moreInks) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ReaderSkin.extra.forEach { skin ->
                        ReaderInkSwatch(skin, palette, Modifier.weight(1f))
                    }
                }
            }

            // ── HOW THE BOOK IS LAID OUT (v434) ─────────────────────────
            //
            // The two `Switch`es are gone. A switch can say "this or not"; the
            // page wants "which of these", which is a segment — and the member
            // asked for it in both places ("keep it as an animated 2-option
            // segment in both places"). A PDF keeps the pair because a picture
            // of a page can be scrolled or turned; a reflowable book says it
            // too, because that is where the member expects it.
            ReaderSheetLabel("Reading mode", palette)
            ReaderSegmentRow(
                segments = ReaderFlow.entries.map { ReaderSegment(it.label, it.modeGlyph()) },
                selectedIndex = if (paged) 1 else 0,
                palette = palette,
                onSelect = { if ((it == 1) != paged) onTogglePaged() }
            )

            ReaderSheetLabel("How the page stands", palette)
            ReaderSegmentRow(
                segments = ReaderOrientation.entries.map {
                    ReaderSegment(it.label, it.orientationGlyph())
                },
                selectedIndex = ReaderOrientation.entries.indexOf(ReaderLook.orientation),
                palette = palette,
                onSelect = { at ->
                    ReaderOrientation.entries.getOrNull(at)?.let { ReaderLook.orientation = it }
                }
            )

            // ── THE WORDS' OWN LAYOUT, for a reflowable book only ───────
            if (showType) {
                ReaderSliderRow(
                    label = "Line spacing",
                    value = ReaderLook.lineSpacing,
                    range = 0.85f..1.6f,
                    step = 0.05f,
                    valueLabel = "${(ReaderLook.lineSpacing * 100f).roundToInt()}%",
                    palette = palette,
                    onValue = { next -> ReaderLook.lineSpacing = next.coerceIn(0.85f, 1.6f) },
                    leadingGlyph = CurioIcons.Remove,
                    leadingLabel = "Tighter lines",
                    trailingGlyph = CurioIcons.Add,
                    trailingLabel = "Looser lines"
                )
                ReaderSliderRow(
                    label = "Page margins",
                    value = ReaderLook.pageMargin,
                    range = 10f..40f,
                    step = 2f,
                    valueLabel = "${ReaderLook.pageMargin.roundToInt()} dp",
                    palette = palette,
                    onValue = { next -> ReaderLook.pageMargin = next.coerceIn(10f, 40f) },
                    leadingGlyph = CurioIcons.Remove,
                    leadingLabel = "Narrower margins",
                    trailingGlyph = CurioIcons.Add,
                    trailingLabel = "Wider margins"
                )
                ReaderSliderRow(
                    label = "Paragraph spacing",
                    value = ReaderLook.paraSpacing,
                    range = 0.6f..2f,
                    step = 0.1f,
                    valueLabel = "${(ReaderLook.paraSpacing * 100f).roundToInt()}%",
                    palette = palette,
                    onValue = { next -> ReaderLook.paraSpacing = next.coerceIn(0.6f, 2f) },
                    leadingGlyph = CurioIcons.Remove,
                    leadingLabel = "Closer paragraphs",
                    trailingGlyph = CurioIcons.Add,
                    trailingLabel = "Further paragraphs"
                )
                ReaderSheetLabel("Lines", palette)
                ReaderAlignRow(palette)
            }

            // ── v465 — THE DICTIONARY'S SOURCE ROW IS NOT IN THE QUICK SHEET ──
            //
            // The member: *"just its option from the reader appearance bottom
            // sheet"*. A source switch (Wiktionary or the free dictionary) is an
            // either/or a member decides ONCE, and the appearance sheet is the
            // surface for the few things a reader changes WHILE reading — paper,
            // type, lines, the night's dim. It was the sheet's only row that
            // governs a different screen, so it read as a promise the sheet does
            // not keep. The choice still exists, in the one place it belongs: the
            // reader's own Reading settings page ([ReaderSettingsScreen]), which
            // every lookup reads ([ReaderLook.dictionary]).

            // ── AND THE NIGHT'S TWO (v434) ────────────────────────────
            ReaderSheetLabel("Screen", palette)
            ReaderSegmentRow(
                segments = listOf(
                    ReaderSegment("Awake", CurioIcons.Lightbulb),
                    ReaderSegment("Let it sleep", CurioIcons.Bedtime)
                ),
                selectedIndex = if (ReaderLook.keepScreenOn) 0 else 1,
                palette = palette,
                onSelect = { at -> ReaderLook.keepScreenOn = at == 0 }
            )
            // v440 — WHEN the dim comes on (see [ReaderLook.dimAuto]): always, or
            // inside a window of the member's own.
            //
            // v442 — AND THE WINDOW IS THEIRS TO SET. "At sunset" used to be the
            // phone's dark theme; it is now the two times below it, which is what
            // the member asked for ("add at sunset customisation to be able to set
            // the tiem"). The rows appear only while that mode is on, so the
            // surface does not carry a control that governs nothing.
            ReaderSegmentRow(
                segments = listOf(
                    ReaderSegment("Dim always", CurioIcons.DarkMode),
                    ReaderSegment("At sunset", CurioIcons.Nightlight)
                ),
                selectedIndex = if (ReaderLook.dimAuto) 1 else 0,
                palette = palette,
                onSelect = { at -> ReaderLook.dimAuto = at == 1 }
            )
            if (ReaderLook.dimAuto) {
                ReaderClockRow(
                    label = "Dim from",
                    minuteOfDay = ReaderLook.dimFromMinute,
                    palette = palette
                ) { clockPick = "from" }
                ReaderClockRow(
                    label = "Dim until",
                    minuteOfDay = ReaderLook.dimUntilMinute,
                    palette = palette
                ) { clockPick = "until" }
            }
            ReaderSliderRow(
                label = "Night dim",
                value = ReaderLook.dim,
                range = 0f..0.6f,
                step = 0.05f,
                valueLabel = if (ReaderLook.dim <= 0f) "Off"
                else "${(ReaderLook.dim / 0.6f * 100f).roundToInt()}%",
                palette = palette,
                onValue = { next -> ReaderLook.dim = next.coerceIn(0f, 0.6f) },
                leadingGlyph = CurioIcons.DarkMode,
                leadingLabel = "Less dim",
                trailingGlyph = CurioIcons.Nightlight,
                trailingLabel = "More dim"
            )
        }
    }

    // ── v442 — AND THE CLOCK ITSELF (see [ReaderClockRow]) ──────────────
    if (clockPick.isNotBlank()) {
        val settingFrom = clockPick == "from"
        ReaderClockDialog(
            palette = palette,
            minuteOfDay = if (settingFrom) ReaderLook.dimFromMinute else ReaderLook.dimUntilMinute,
            onDismiss = { clockPick = "" },
            onPick = { minute ->
                if (settingFrom) ReaderLook.dimFromMinute = minute
                else ReaderLook.dimUntilMinute = minute
            }
        )
    }
}

/** v434 — the glyph a flow wears in the reading-mode segment. */
internal fun ReaderFlow.modeGlyph(): String = when (this) {
    ReaderFlow.SCROLL -> CurioIcons.Subject
    ReaderFlow.PAGED -> CurioIcons.AutoStories
}

/** v434 — the glyph an orientation wears in its segment. */
internal fun ReaderOrientation.orientationGlyph(): String = when (this) {
    ReaderOrientation.AUTO -> CurioIcons.AspectRatio
    ReaderOrientation.PORTRAIT -> CurioIcons.Crop
    ReaderOrientation.LANDSCAPE -> CurioIcons.Fullscreen
}

/** A section's name inside a reader sheet. The only furniture a sheet needs. */
@Composable
internal fun ReaderSheetLabel(text: String, palette: ReaderPalette) {
    Text(
        text.uppercase(Locale.US),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = palette.accent
    )
}

/** One A−/A+ disc either side of the type slider. */
@Composable
internal fun ReaderStepperButton(
    glyph: String,
    label: String,
    palette: ReaderPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = palette.ink.copy(alpha = 0.08f),
        modifier = Modifier.size(38.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(glyph, label, tint = palette.ink.copy(alpha = 0.8f), size = 19.dp)
        }
    }
}

/** One paper in the row: the skin's own sheet, with its own ink on it. */
@Composable
internal fun ReaderInkSwatch(
    skin: ReaderSkin,
    palette: ReaderPalette,
    modifier: Modifier = Modifier
) {
    val tone = readerPalette(skin.key)
    val live = ReaderLook.inkKey == skin.key
    Surface(
        onClick = { ReaderLook.inkKey = skin.key },
        shape = RoundedCornerShape(14.dp),
        color = tone.paper,
        border = androidx.compose.foundation.BorderStroke(
            if (live) 2.dp else 1.dp,
            if (live) tone.accent else palette.ink.copy(alpha = 0.18f)
        ),
        modifier = modifier.height(52.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Aa",
                style = TextStyle(
                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = tone.ink
            )
        }
    }
}

/** The `+` that unfolds the tuned papers (see [ReaderSkin.extra]). */
@Composable
internal fun ReaderMoreInkTile(
    palette: ReaderPalette,
    open: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = palette.ink.copy(alpha = if (open) 0.13f else 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.ink.copy(alpha = 0.18f)),
        modifier = modifier.height(52.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(
                if (open) CurioIcons.Remove else CurioIcons.Add,
                if (open) "Hide the other papers" else "More papers",
                tint = palette.ink.copy(alpha = 0.7f),
                size = 18.dp
            )
        }
    }
}

/** v434 — one choice of a [ReaderSegmentRow]. */
internal data class ReaderSegment(val label: String, val glyph: String? = null)

/**
 * v434 — AN ANIMATED SEGMENTED PILL: THE READER'S REPLACEMENT FOR A SWITCH.
 *
 * A `Switch` says one thing (this or not). The reader's choices are almost never
 * that shape — a book is scrolled or turned, a page stands auto, upright or wide,
 * the screen is kept awake or allowed to sleep — so every one of them is a SEGMENT
 * the member picks from, with the selected pill SLIDING between its options rather
 * than blinking (the member's own design direction: "instead of toggle use proper
 * 2 opton style with animation", and "similar design system to samsung, less text
 * and toggle but more icon based button style").
 *
 * The thumb is moved in the LAYOUT phase (the `offset` lambda), so a slide never
 * recomposes the labels; only their ink animates, which is why the two run in
 * step on a cheap frame.
 */
@Composable
internal fun ReaderSegmentRow(
    segments: List<ReaderSegment>,
    selectedIndex: Int,
    palette: ReaderPalette,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return
    val index = selectedIndex.coerceIn(0, segments.size - 1)
    val slide = animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "readerSegmentSlide"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = palette.ink.copy(alpha = 0.06f),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.height(46.dp)) {
            val each = maxWidth / segments.size
            Box(
                modifier = Modifier
                    .offset { IntOffset((slide.value * each.toPx()).roundToInt(), 0) }
                    .width(each)
                    .fillMaxHeight()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.accent)
            )
            Row(modifier = Modifier.fillMaxSize()) {
                segments.forEachIndexed { at, segment ->
                    val live = at == index
                    val ink = animateColorAsState(
                        targetValue = if (live) palette.paper else palette.ink.copy(alpha = 0.72f),
                        animationSpec = tween(durationMillis = 200),
                        label = "readerSegmentInk"
                    )
                    Surface(
                        onClick = { onSelect(at) },
                        shape = RoundedCornerShape(50),
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            segment.glyph?.let { glyph ->
                                CurioIcon(glyph, null, tint = ink.value, size = 15.dp)
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                segment.label,
                                style = TextStyle(
                                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                                    fontSize = 13.sp,
                                    fontWeight = if (live) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = ink.value,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * v434 — ONE LOOK SETTING: ITS NAME, HOW BIG IT IS, AND THE TWO DISCS THAT STEP IT.
 *
 * The A−/slider/A+ row the member asked for, generalised so every size-like
 * setting in the reader (the type, a PDF's zoom, the leading, the margins, the
 * paragraph gap and the night dim) is the SAME control in the same place. One
 * shape, learned once.
 */
@Composable
internal fun ReaderSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    valueLabel: String,
    palette: ReaderPalette,
    onValue: (Float) -> Unit,
    leadingGlyph: String,
    leadingLabel: String,
    trailingGlyph: String,
    trailingLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = TextStyle(
                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.ink
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.accent
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReaderStepperButton(leadingGlyph, leadingLabel, palette) {
                onValue((value - step).coerceIn(range.start, range.endInclusive))
            }
            Slider(
                value = value.coerceIn(range.start, range.endInclusive),
                onValueChange = { next -> onValue(next.coerceIn(range.start, range.endInclusive)) },
                valueRange = range,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = palette.accent,
                    activeTrackColor = palette.accent,
                    inactiveTrackColor = palette.ink.copy(alpha = 0.15f)
                )
            )
            ReaderStepperButton(trailingGlyph, trailingLabel, palette) {
                onValue((value + step).coerceIn(range.start, range.endInclusive))
            }
        }
    }
}

/**
 * v434 — THE ALIGNMENT GLYPH, DRAWN (three rules, ragged or flush).
 *
 * The bundled font carries none of the `format_align_*` ligatures, so a named
 * icon here would render as a blank pill — the reason the four alignments
 * elsewhere in Curio are drawn too.
 */
/**
 * v434 — THE TWO LINE-ALIGNMENT DOORS, in the reader's own capsule language.
 *
 * A pair rather than a [ReaderSegmentRow] because both glyphs are DRAWN (see
 * [ReaderAlignGlyph]) and a segment only carries a named icon.
 */
@Composable
internal fun ReaderAlignRow(palette: ReaderPalette, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReaderAlign.entries.forEach { option ->
            val live = ReaderLook.justify == option.justified
            Surface(
                onClick = { ReaderLook.justify = option.justified },
                shape = RoundedCornerShape(50),
                color = if (live) palette.accent else palette.ink.copy(alpha = 0.07f),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ReaderAlignGlyph(
                        justified = option.justified,
                        tint = if (live) palette.paper else palette.ink.copy(alpha = 0.78f)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        option.label,
                        style = TextStyle(
                            fontFamily = readerTypeFamily(ReaderLook.typeFace),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (live) palette.paper else palette.ink.copy(alpha = 0.78f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReaderAlignGlyph(justified: Boolean, tint: Color, iconSize: Dp = 18.dp) {
    Canvas(modifier = Modifier.size(iconSize)) {
        val stroke = this.size.height * 0.11f
        val widths = if (justified) listOf(1f, 1f, 1f) else listOf(1f, 0.62f, 0.86f)
        val gap = this.size.height / (widths.size * 2f - 1f)
        widths.forEachIndexed { at, share ->
            val y = gap / 2f + at * gap * 2f
            drawLine(
                color = tint,
                start = Offset(0f, y),
                end = Offset(this.size.width * share, y),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * WHAT THE DICTIONARY IS SHOWING — THREE ANSWERS, WHERE THERE USED TO BE ONE NULL
 * (v443).
 *
 * A single `List?` was carrying two different meanings at once (`null` = "not asked
 * yet" AND "the dictionary could not be reached"), and the first frame of every
 * sheet is "not asked yet" — so opening the dictionary flashed *"The dictionary
 * could not be reached."* for a moment before its first real answer, and a blank
 * field (a passage with no word worth suggesting) sat on that message for good.
 * The member's report: *"the dictionary wasnt working"*. Each answer has its own
 * name now, and the sheet draws each one differently.
 */
private sealed interface ReaderLookup {
    /** Nothing asked yet: an empty field, or the moment before the first call. */
    object Idle : ReaderLookup

    /** The source answered. An EMPTY list is a real answer — no such headword. */
    data class Answer(val senses: List<ReaderDictionarySense>) : ReaderLookup

    /** The source could not be reached. Never confused with "no such word". */
    object Unreachable : ReaderLookup
}

/**
 * v431 — THE DICTIONARY: THE WORD, AND WHAT IT MEANS, WITHOUT LEAVING THE PAGE.
 *
 * The member asked for the lookup on the spot ("In app wikitionary"), and for
 * the selection bar to offer NOTHING ELSE when the sweep landed on one word ("when
 * user hight one wor only show the dictionarcy icon"), because looking a word up
 * is the one thing a reader wants from a one-word selection. The lookup itself
 * lives in [ReaderDictionary]; this is only its door.
 */
/**
 * v444 — WHICH DOOR ANSWERS, as the sheet's badge row.
 *
 * The member: *"for dictionary use both the providers and show it as a badge
 * option to switch between"*, then *"offline first once downloaded"*. The two
 * online doors used to be an either/or SETTING (one of them, chosen in reading
 * settings), which is why a word the chosen one does not carry read as a dead
 * end. They are doors of one sheet now, side by side with the downloaded
 * dictionary, and the sheet shows the answer of the door the member is on — so
 * switching is one tap rather than a trip to settings, and the offline file is
 * the door that is already answered before any network is asked.
 */
private enum class DictionaryDoor(
    val label: String,
    /**
     * v452 — THE VOLUMES THIS DOOR ANSWERS FROM, best first.
     *
     * The member: *"merge the two modern and full 1913 in offline as offline
     * shows nothing, only modern and full 1913 does"*. Three offline BADGES meant
     * a member had to guess which of the app's own dictionaries to stand on, and
     * the one called "Offline" — the one a person actually reaches for — was the
     * abridged Webster's alone, so choosing it could answer nothing at all while
     * the two beside it answered. **The badge is the INTENT now** ("answer me
     * without a connection") and the volumes are what it draws on: one Offline
     * door over every volume the phone has, modern senses first (WordNet), then
     * the complete 1913.
     *
     * v457 — and the abridged 1913 conversion is gone: it was the same
     * public-domain text as the full edition in a lighter conversion, so the two
     * rows read as one dictionary twice and the smaller one was a strict subset
     * (see [ReaderOfflineDictionary]).
     */
    val volumes: List<ReaderOfflineDictionary.Volume> = emptyList(),
    /** The online door this badge stands for, or null for a file. */
    val online: ReaderDictionarySource? = null
) {
    OFFLINE(
        "Offline",
        volumes = listOf(
            ReaderOfflineDictionary.Volume.MODERN,
            ReaderOfflineDictionary.Volume.FULL
        )
    ),
    WIKTIONARY("Wiktionary", online = ReaderDictionarySource.WIKTIONARY),
    FREE("Free", online = ReaderDictionarySource.FREE);

    /**
     * The volume whose row the sheet shows first — the door's own best one. The
     * other volumes of the door keep their chips (see the sheet's volume block).
     */
    val volume: ReaderOfflineDictionary.Volume? get() = volumes.firstOrNull()
}

@Composable
private fun ReaderDictionarySheet(
    palette: ReaderPalette,
    /** The word to look up, when the selection was one word (blank otherwise). */
    initial: String,
    /** The passage a lookup came from: its words are offered, its line quoted. */
    passage: String,
    /**
     * v444 — OPENED TO SEARCH, rather than to answer for the page.
     *
     * The member: *"when the dictionary is opened from the 3 dot one it [should
     * be] more longer and let user search any word"*. The ⋯ menu's door is for
     * looking something up, so that sheet stands taller (`minHeightFraction`
     * below) and its field is the point of it — a passage-driven sheet, opened
     * from a sweep, keeps the shorter panel it had.
     */
    searchMode: Boolean = false,
    onDismiss: () -> Unit
) {
    // ── v442 — A PASSAGE SUGGESTS ITS OWN WORDS ─────────────────────────
    //
    // The member: *"improve the discoonary that it suggest work explanation from
    // the selected para"*, and, asked how: "chips + context line". A selection
    // that is a whole passage has no single word in it to look up, so the sheet
    // reads the passage for the words worth asking about
    // ([ReaderDictionary.wordsIn]) and offers them as chips — and the word being
    // answered right now also brings the SENTENCE it stood in, so the meaning
    // arrives beside the line that raised the question.
    val candidates = remember(passage) { ReaderDictionary.wordsIn(passage) }
    var word by remember(initial, passage) {
        mutableStateOf(initial.ifBlank { candidates.firstOrNull()?.word.orEmpty() })
    }
    var lookup by remember { mutableStateOf<ReaderLookup>(ReaderLookup.Idle) }
    var looking by remember { mutableStateOf(false) }
    var asked by remember { mutableStateOf("") }
    // The spellings the dictionary offered when the word itself had no entry.
    var guesses by remember { mutableStateOf<List<String>>(emptyList()) }
    // ── v444 — THE DOORS, AND THE ONE THAT LIVES ON THE PHONE ──────────
    val context = LocalContext.current
    val doorScope = rememberCoroutineScope()
    // ── v446 — WHICH VOLUMES ARE ALREADY ON THE PHONE ───────────────────
    //
    // Each read once when the sheet opens and re-read after a download or a
    // removal, so a badge can never claim a volume the phone does not have.
    // ── v457 — AND A RETIRED VOLUME'S FILE GOES ─────────────────────────
    //
    // The abridged 1913 door is removed; a phone that had downloaded it keeps the
    // 9MB forever unless something deletes it, and nothing can search it any more
    // (see [ReaderOfflineDictionary.purgeRetired]). It runs off the main thread
    // and nothing below waits on it — the doors read the volumes that exist.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { ReaderOfflineDictionary.purgeRetired(context) }
    }
    var ready by remember {
        mutableStateOf(
            ReaderOfflineDictionary.Volume.entries.associateWith {
                ReaderOfflineDictionary.isReady(context, it)
            }
        )
    }
    // The volume being fetched right now, and how far it has come. Only one at a
    // time: the rows are the member's own, and two 11MB downloads racing on a phone
    // connection is not a choice a sheet should make for them.
    var downloading by remember { mutableStateOf<ReaderOfflineDictionary.Volume?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    // The door the sheet OPENS on: the local file once it is there, else whichever
    // online door reading settings already named (so nobody's habit changes).
    var door by remember {
        mutableStateOf(
            // v452 — offline first once ANY volume is on the phone (the member's own
            // rule, now that Offline is the merged door rather than Webster's alone).
            if (ReaderOfflineDictionary.Volume.entries.any { ready[it] == true }) {
                DictionaryDoor.OFFLINE
            } else when (ReaderLook.dictionary) {
                ReaderDictionarySource.FREE -> DictionaryDoor.FREE
                ReaderDictionarySource.WIKTIONARY -> DictionaryDoor.WIKTIONARY
            }
        )
    }
    /**
     * The lookup, against the door the member is on.
     *
     * The offline door answers from the file (and says `null` when there is no
     * file — the sheet offers the download rather than pretending the word does
     * not exist); the online doors answer through [ReaderDictionary], which
     * memoises them, so flipping back and forth between badges is free.
     */
    suspend fun ask(term: String): List<ReaderDictionarySense>? {
        if (door.volumes.isNotEmpty()) {
            // ── v452 — THE MERGED OFFLINE ANSWER ────────────────────────
            //
            // Ask every volume the door has, best first, and take the first one
            // that carries the word — so "Offline" answers from WordNet's modern
            // senses, or from the complete 1913, or from the abridged one, without
            // the member choosing. The two answers the sheet must still tell apart
            // are kept apart: a volume that is not on the phone says `null`
            // (define's own contract) and is simply passed over, `anyReady` records
            // whether ANY of them was there at all, and the door answers
            // `emptyList()` (no such word) only when a dictionary really was asked;
            // no volume at all still returns `null`, which is what makes the sheet
            // offer the downloads instead of claiming the word does not exist.
            var anyReady = false
            for (volume in door.volumes) {
                val senses = ReaderOfflineDictionary.define(context, volume, term)
                    ?: continue
                anyReady = true
                if (senses.isNotEmpty()) return senses
            }
            return if (anyReady) emptyList() else null
        }
        val online = door.online ?: ReaderDictionarySource.WIKTIONARY
        return ReaderDictionary.define(term, online)
    }
    LaunchedEffect(word, door) {
        // The word AS WRITTEN is not always the word to ask for: a sweep carries
        // its punctuation and its possessive (see [ReaderDictionary.headword]).
        val term = ReaderDictionary.headword(word)
        if (term.isBlank()) {
            lookup = ReaderLookup.Idle
            asked = ""
            guesses = emptyList()
            return@LaunchedEffect
        }
        looking = true
        // The typing pause, not every letter: a definition is a network call.
        delay(320)
        val found = withContext(Dispatchers.IO) {
            runCatching { ask(term) }.getOrNull()
        }
        if (found == null) {
            // Unreachable is NOT "no such word" — the two read differently (see
            // [ReaderDictionary.define]).
            lookup = ReaderLookup.Unreachable
            asked = term
            guesses = emptyList()
        } else if (found.isNotEmpty()) {
            lookup = ReaderLookup.Answer(found)
            asked = term
            guesses = emptyList()
        } else {
            // ── A MISS IS USUALLY A SPELLING (v442) ─────────────────────
            //
            // The member: *"or a mis type"*. The nearest page names are asked
            // for, and the first one that HAS a definition simply answers —
            // labelled, so the member can see which word answered for which.
            val near = withContext(Dispatchers.IO) {
                runCatching { ReaderDictionary.suggest(term) }.getOrNull().orEmpty()
            }
            // ── v443 — THE SOURCE IS ONLY ASKED WHILE IT IS ANSWERING ────
            //
            // Four neighbours, four timeouts, twelve seconds of spinner, and a
            // sheet that then said the dictionary was unreachable anyway. The
            // first `null` — the source is not answering AT ALL — ends the run,
            // which is both the faster and the truer answer.
            var answeredBy = ""
            var answered: List<ReaderDictionarySense>? = null
            var silent = false
            for (candidate in near) {
                val answer = withContext(Dispatchers.IO) {
                    runCatching { ReaderDictionary.define(candidate) }.getOrNull()
                }
                if (answer == null) {
                    silent = true
                    break
                }
                if (answer.isNotEmpty()) {
                    answeredBy = candidate
                    answered = answer
                    break
                }
            }
            val hit = answered
            lookup = when {
                hit != null -> ReaderLookup.Answer(hit)
                silent -> ReaderLookup.Unreachable
                else -> ReaderLookup.Answer(emptyList())
            }
            asked = if (hit != null) answeredBy else term
            guesses = near
        }
        looking = false
    }
    ReaderSheetFrame(
        title = "Dictionary",
        palette = palette,
        onDismiss = onDismiss,
        // v444 — a sheet you SEARCH in stands taller than one that answers for the
        // words you just swept (the member: *"the bottom sheet can be scrollable
        // and a little up"*, and *"when opened from the 3 dot … more longer"*).
        minHeightFraction = if (searchMode) 0.62f else 0.55f
    ) {
        // v434 — wrap-sized, because the sheet's own body scrolls now (see
        // [ReaderSheetFrame]); a `weight(1f)` here would have measured to
        // nothing inside that scroll.
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── v444 — THE DOORS, AS A BADGE ROW ────────────────────────
            //
            // One badge per dictionary, side by side, the chosen one lit. A door
            // with a dictionary behind it reads plainly; the offline one is
            // dimmed until its file is downloaded (and says so under the row).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DictionaryDoor.entries.forEach { option ->
                    val chosen = option == door
                    // v452 — an offline badge is dim only when the phone has NONE of
                    // its volumes: any one of them is a working dictionary, and the
                    // door searches them all.
                    val live = if (option.volumes.isEmpty()) {
                        true
                    } else {
                        option.volumes.any { ready[it] == true }
                    }
                    Surface(
                        onClick = { door = option },
                        shape = RoundedCornerShape(50),
                        color = if (chosen) {
                            palette.accent.copy(alpha = 0.18f)
                        } else {
                            palette.ink.copy(alpha = 0.06f)
                        },
                        contentColor = palette.ink
                    ) {
                        Text(
                            option.label,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = palette.ink.copy(
                                alpha = when {
                                    chosen -> 1f
                                    live -> 0.75f
                                    else -> 0.45f
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            // ── THE VOLUME THE DOOR IN FRONT OF YOU NEEDS ──────────────
            //
            // Nothing is fetched until the member asks for it, and once it is there
            // the row is where it can be let go again. The source and its licence are
            // stated where the tap is, because a file this app will keep for good is
            // worth naming (see [ReaderOfflineDictionary]).
            //
            // v446 — AND IT IS THE ROW OF THE DOOR YOU ARE STANDING ON. There are
            // three offline volumes now and one row: standing on a badge is what
            // chooses a dictionary, so the row under the badges offers exactly that
            // one's download — or, once it is there, its removal.
            val wanted = door.volume
            if (wanted != null && ready[wanted] != true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurioIcon(
                        CurioIcons.Download,
                        null,
                        tint = palette.ink.copy(alpha = 0.6f),
                        size = 16.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            wanted.source,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.ink
                            )
                        )
                        Text(
                            if (downloading == wanted) {
                                "Downloading\u2026 ${(downloadProgress * 100f).toInt()}%"
                            } else {
                                wanted.blurb + " \u00b7 " + wanted.size
                            },
                            style = TextStyle(fontSize = 11.sp, color = palette.ink.copy(alpha = 0.55f))
                        )
                    }
                    if (downloading == null) {
                        Surface(
                            onClick = {
                                downloading = wanted
                                downloadProgress = 0f
                                doorScope.launch {
                                    val saved = ReaderOfflineDictionary.download(
                                        context,
                                        wanted
                                    ) { ratio -> downloadProgress = ratio }
                                    downloading = null
                                    // A finished download is the door the member
                                    // wanted; the badge lights and the sheet keeps
                                    // standing on it.
                                    ready = ready + (wanted to saved)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            color = palette.accent,
                            contentColor = Color.White
                        ) {
                            Text(
                                "Download",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                // ── v446 — AND IT SAYS HOW FAR ALONG IT IS ─────────────
                //
                // The member's own follow-up: *"show the offline dictionary's
                // download progress as a real bar"*. A percentage that only
                // ticks every few hundred kilobytes reads as a stuck number on a
                // phone connection, so the row carries a bar too — drawn from two
                // boxes (the track, then the fill sized to the fraction) rather
                // than a progress-indicator API, so the reader's own paper and
                // accent decide its look on every Material version this app builds
                // against.
                if (downloading == wanted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.ink.copy(alpha = 0.10f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(downloadProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(palette.accent)
                        )
                    }
                }
            } else if (wanted != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        wanted.source + " \u00b7 " + wanted.blurb + " \u00b7 stored",
                        style = TextStyle(fontSize = 11.sp, color = palette.ink.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        onClick = {
                            ReaderOfflineDictionary.remove(context, wanted)
                            val now = ready + (wanted to false)
                            ready = now
                            // v452 — ONE volume leaving does not empty the door: it
                            // steps to an online one only when the phone has none of
                            // the door's volumes left, so removing WordNet while the
                            // 1913s are still there keeps the member exactly where
                            // they are standing (the door searches what remains).
                            door = if (door.volumes.any { now[it] == true }) {
                                DictionaryDoor.OFFLINE
                            } else {
                                DictionaryDoor.WIKTIONARY
                            }
                        },
                        shape = RoundedCornerShape(50),
                        color = palette.ink.copy(alpha = 0.06f),
                        contentColor = palette.ink
                    ) {
                        Text(
                            "Remove",
                            style = TextStyle(fontSize = 11.sp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            // ── AND THE VOLUMES YOU ARE NOT STANDING ON ────────────────
            //
            // The badges are doors, but a member who landed on Wiktionary has no
            // reason to guess that tapping "Modern" is how WordNet is fetched. So
            // every offline volume that is not on the phone yet is offered right
            // here as a small chip — one tap, its own progress on the chip — and a
            // volume already downloaded is not offered at all (there is nothing
            // left to do to it from this row).
            val missing = ReaderOfflineDictionary.Volume.entries.filter { ready[it] != true }
            if (missing.any { it != wanted }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    missing.forEach { volume ->
                        if (volume == wanted) return@forEach
                        Surface(
                            onClick = {
                                downloading = volume
                                downloadProgress = 0f
                                doorScope.launch {
                                    val saved = ReaderOfflineDictionary.download(
                                        context,
                                        volume
                                    ) { ratio -> downloadProgress = ratio }
                                    downloading = null
                                    ready = ready + (volume to saved)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            color = palette.ink.copy(alpha = 0.06f),
                            contentColor = palette.ink
                        ) {
                            Text(
                                // The chip names the SOURCE, not the badge (v452): the
                                // labels were badges before, and "Download Offline"
                                // beside a badge called Offline says nothing about
                                // which dictionary is about to be fetched.
                                if (downloading == volume) {
                                    volume.source + " \u2026 " +
                                        (downloadProgress * 100f).toInt() + "%"
                                } else {
                                    "Download " + volume.source + " \u00b7 " + volume.size
                                },
                                style = TextStyle(fontSize = 11.sp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = palette.ink.copy(alpha = 0.06f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
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
                        value = word,
                        onValueChange = { next -> word = next },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = readerTypeFamily(ReaderLook.typeFace),
                            fontSize = 16.sp,
                            color = palette.ink
                        ),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        decorationBox = { inner ->
                            Box {
                                if (word.isEmpty()) {
                                    Text(
                                        "A word",
                                        style = TextStyle(fontSize = 16.sp),
                                        color = palette.ink.copy(alpha = 0.35f)
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    )
                }
            }
            // ── v442 — THE WORDS THE PASSAGE OFFERS ────────────────────
            if (candidates.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ReaderSheetLabel("From the passage", palette)
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    candidates.forEach { candidate ->
                        ReaderDictionaryChip(
                            label = candidate.word,
                            live = candidate.word.equals(asked, ignoreCase = true),
                            palette = palette
                        ) { word = candidate.word }
                    }
                }
            }

            // ── AND THE WORD IT THINKS YOU MEANT ───────────────────────
            //
            // Shown whenever a lookup had to be answered by a neighbour: the
            // typo is the member's to see, and the row is the door to the other
            // spellings rather than a dead end.
            if (guesses.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ReaderSheetLabel("Did you mean", palette)
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    guesses.forEach { guess ->
                        ReaderDictionaryChip(
                            label = guess,
                            live = guess.equals(asked, ignoreCase = true),
                            palette = palette
                        ) { word = guess }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            val state = lookup
            when {
                looking -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = palette.accent, modifier = Modifier.size(22.dp))
                }

                // A NULL IS NOT AN EMPTY LIST: unreachable and "no such word"
                // are two different answers (see [ReaderDictionary.define]), and an
                // empty FIELD is a third that used to draw the first one (v443, see
                // [ReaderLookup]).
                state is ReaderLookup.Unreachable -> Text(
                    "The dictionary could not be reached.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink.copy(alpha = 0.7f)
                )

                state is ReaderLookup.Answer && state.senses.isEmpty() -> Text(
                    "Nothing for \u201C$asked\u201D.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink.copy(alpha = 0.7f)
                )

                // An empty field, or the moment before the first call: nothing
                // under the input. The field and its own "A word" placeholder are
                // the whole instruction, and a shorter sheet beats a sentence that
                // says nothing (see [ReaderLookup.Idle]).
                state is ReaderLookup.Idle -> Unit

                else -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // The compiler cannot carry the smart cast into a branch that
                    // is not a shape it can see, so the senses are named once here.
                    val list = (state as ReaderLookup.Answer).senses
                    // ── v442 — AND THE LINE IT CAME FROM ───────────────
                    //
                    // The member's own pick for a passage lookup: a meaning read
                    // beside the sentence the word stood in is the difference
                    // between a dictionary and an answer.
                    val context = remember(passage, asked) { readerContextFor(passage, asked) }
                    if (context.isNotBlank()) {
                        Text(
                            context,
                            style = TextStyle(
                                fontFamily = readerTypeFamily(ReaderLook.typeFace),
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                fontStyle = FontStyle.Italic,
                                color = palette.ink.copy(alpha = 0.65f)
                            )
                        )
                    }
                    list.forEach { sense ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReaderSheetLabel(sense.partOfSpeech.ifBlank { "Entry" }, palette)
                            sense.definitions.forEach { line ->
                                Text(
                                    line,
                                    style = TextStyle(
                                        fontFamily = readerTypeFamily(ReaderLook.typeFace),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        color = palette.ink
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * v442 — ONE OF THE PASSAGE'S WORDS, AS A CHIP.
 *
 * The word being answered right now wears an opaque accent blend rather than a
 * wash, so "which word are these meanings for" survives a pale paper (the same
 * rule the reader's other pills follow).
 */
@Composable
private fun ReaderDictionaryChip(
    label: String,
    live: Boolean,
    palette: ReaderPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (live) lerp(palette.surface, palette.accent, 0.30f)
        else palette.ink.copy(alpha = 0.07f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (live) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (live) palette.ink else palette.ink.copy(alpha = 0.8f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

/** One sentence of [passage], split the way a reader reads them. Compiled once. */
/**
 * ── v465c — WHICH VOICE THE READER ACTUALLY USES ────────────────────────────
 *
 * [ReaderLook.speakEngine] has always held an Android speech-engine *package*
 * name, and `""` has always meant "the phone's own". A downloaded voice pack is
 * neither — it is Curio's own runtime, not a service — so it is named with a
 * sentinel that a package name can never be: every real package has at least one
 * dot and starts with a letter, and `"curio:"` is not a legal Android package
 * prefix. That keeps ONE stored field as the whole choice, so every existing
 * install's setting keeps meaning exactly what it meant.
 */
internal object ReaderEngine {
    /** The phone's own engine — the v440 default, and the empty package. */
    const val PHONE = ""

    /**
     * The prefix both of Curio's own engines carry — see [isSentinel].
     *
     * ⚠️ THE TWO VALUES BELOW ARE STORED SETTINGS, so they are built from this
     * prefix rather than written out again: changing either string renames every
     * member's saved choice, and a member whose stored engine no longer matches
     * any row silently reads with the phone's voice instead.
     */
    const val PREFIX = "curio:"

    /** A downloaded sherpa-onnx voice pack, chosen by its PACK ID in `speakVoice`. */
    const val NEURAL = PREFIX + "neural"

    /** The hidden Dev-page Edge TTS experiment. */
    const val EDGE = PREFIX + "edge"

    /**
     * Whether [engine] is one of Curio's own rather than an Android package.
     *
     * Called at the moment of speaking: a sentinel must never reach
     * `TextToSpeech(context, listener, package)` as if it were one, and the two
     * cases that fall through to the system engine (an Edge engine with the
     * experiment switched off, a pack that cannot be honoured) are exactly the
     * ones where the member's stored value IS a sentinel.
     */
    fun isSentinel(engine: String): Boolean = engine.startsWith(PREFIX)

    /**
     * The engine PACKAGE to hand the platform for [engine].
     *
     * ⚠️ A SENTINEL IS NOT A PACKAGE NAME, and this is the one place that is
     * settled: `TextToSpeech(context, listener, "curio:neural")` asks the platform for
     * an engine that cannot exist, which binds nothing and logs an init failure — so
     * every `ReaderSpeaker.prepare` in the reader goes through here. Both sentinels
     * map to [PHONE], which is exactly what they fall back to when their own voice
     * cannot be honoured (a pack that was deleted, the Edge experiment switched off),
     * and any real package is passed through untouched.
     */
    fun packageFor(engine: String): String = if (isSentinel(engine)) PHONE else engine
}

/**
 * Says [text] in whatever voice the member chose, and calls [onDone] when it is
 * finished — the one place the reader has to ask for speech.
 *
 * **The fallback is the point.** A member can select a voice pack and then delete
 * it (or clear Curio's storage, or move to a phone where the 305 MB Kokoro pack
 * was never downloaded), and the stored setting survives all three. So a neural
 * choice that cannot be honoured falls back to the phone's own voice rather than
 * to silence: a reading session that quietly says nothing is far worse than one
 * read in the wrong voice, and it looks like a hang.
 *
 * The pack is loaded HERE, on the first sentence, rather than when the member
 * taps play — loading a 305 MB Kokoro model takes seconds, and spending them
 * before the button responds would read as a dead control. [NeuralSpeaker.prepare]
 * is idempotent, so only the first sentence of a session pays for it.
 */
internal suspend fun sayAloud(
    context: Context,
    text: String,
    speed: Float,
    onDone: () -> Unit
) {
    // ⚠️ THE FLAG IS CHECKED AT THE MOMENT OF SPEAKING, NOT WHEN THE VOICE WAS
    // CHOSEN. The experiment can be switched OFF in Settings while the reader is
    // already pointed at this voice, and a stored choice outliving its feature is
    // the normal case rather than an odd one (see
    // [AppPreferences.setEdgeVoiceEnabled] for why nothing clears it). So the flag
    // is read HERE, which is what makes switching the experiment off take effect at
    // once: an Edge engine with the flag off simply falls through to the tail below
    // and reads in the phone's own voice, rather than opening a socket to an
    // endpoint the member just turned away from.
    if (ReaderLook.speakEngine == ReaderEngine.EDGE && AppPreferences.edgeVoiceEnabledState &&
        !ReadAloudSession.edgeUnavailable
    ) {
        EdgeVoice.say(context, text, speed, ReaderLook.speakVoice, onDone) {
            // ── v465i — A REFUSED SOCKET IS NOT A READ SENTENCE ─────────
            //
            // The experiment is undocumented (see [EdgeVoice]): a rotated GEC
            // version, a 403 or a dead network is its normal weather. It used to
            // report "done" anyway, so the page raced through the book in silence.
            // Now the first refusal ends Edge for this reading and the SAME
            // sentence is read in the phone's own voice, so the member hears a
            // voice rather than a page of moving highlight.
            ReadAloudSession.edgeUnavailable = true
            ReaderSpeaker.prepare(context, ReaderEngine.PHONE)
            // An empty voice name because the fallback is the PHONE's own voice —
            // a stored name here is an endpoint id (`en-US-AriaNeural`), which no
            // system engine has ever heard of.
            ReaderSpeaker.say(text, speed, "", onDone)
        }
        return
    }
    if (ReaderLook.speakEngine == ReaderEngine.NEURAL) {
        val pack = NeuralVoicePacks.byId(ReaderLook.speakVoice)
        if (pack != null) {
            // ⚠️ READY FOR **THIS** PACK — `NeuralSpeaker.isReady` alone is the
            // bug it looks like the fix for (v465j). It is true whenever ANY pack
            // is loaded, so a member who listened to Piper and then chose the
            // Kokoro pack they had just downloaded kept being read to by Piper:
            // the engine was already up, `prepare` was skipped, and the pack they
            // picked was never opened. See [NeuralSpeaker.isReadyFor].
            val ready = NeuralSpeaker.isReadyFor(pack.id) || withContext(Dispatchers.IO) {
                NeuralSpeaker.prepare(context, pack)
            }
            if (ready) {
                // THE MEMBER'S NARRATOR, CLAMPED TO THE PACK ACTUALLY LOADED.
                // The stored sid can outlive the table it came from (see
                // [ReaderLook.speakSpeaker]), so it is bounded by the model's own
                // count here rather than trusted: out of range lands on the last
                // real voice instead of a generation that returns nothing.
                val last = (NeuralSpeaker.speakerCount() - 1).coerceAtLeast(0)
                NeuralSpeaker.say(text, speed, ReaderLook.speakSpeaker.coerceIn(0, last), onDone)
                return
            }
        }
        // A PACK THAT CANNOT BE HONOURED FALLS THROUGH to the tail below — deleted,
        // never downloaded, or the model failed to load — which reads in the
        // phone's own voice rather than in silence (the rule this function's own
        // note states).
    }
    // ── v465h — AND EVERYTHING ELSE IS THE MEMBER'S OWN VOICE ────────────
    //
    // This tail used to bind `ReaderEngine.PHONE` and speak with an EMPTY voice
    // name, which quietly undid v464: choosing a speech engine the member installed
    // (a neural engine, an F-Droid TTS) prepared that engine on the play tap, and
    // then the next sentence's line here released it and bound the phone's own
    // instead — so the engine they had just chosen read in exactly the voice they
    // had just replaced, in both the read-aloud and every skip.
    //
    // ⚠️ AND A CURIO SENTINEL IS NOT A PACKAGE NAME. `ReaderEngine.NEURAL` and
    // `ReaderEngine.EDGE` are `"curio:"`-prefixed values that no Android package can
    // be, and handing one to `TextToSpeech(context, listener, package)` binds
    // nothing — so the two sentinels are mapped to the empty package ("the phone's
    // own", which is exactly what they fall back to by design) and to no voice name
    // at all, since their stored voice is a pack id or an endpoint voice, not a
    // system voice. An empty `speakEngine` IS the phone's own, so a member who never
    // opened the picker hears exactly what they always heard.
    val curioEngine = ReaderEngine.isSentinel(ReaderLook.speakEngine)
    ReaderSpeaker.prepare(context, ReaderEngine.packageFor(ReaderLook.speakEngine))
    ReaderSpeaker.say(text, speed, if (curioEngine) "" else ReaderLook.speakVoice, onDone)
}

private val ReaderSentenceSplit = Regex("(?<=[.!?])\\s+")

/** What a word looks like inside a sentence, for the context line's own search. */
private val ReaderHeadwordShape = Regex("[A-Za-z][A-Za-z'\\-]{3,}")

/**
 * v442 — THE SENTENCE A LOOKED-UP WORD STOOD IN.
 *
 * Blank when the word is not in the passage at all (a typed word, a suggestion
 * from another spelling): a context line that quoted the wrong sentence would be
 * worse than no context line, which is the one thing a quoted line must never do.
 */
private fun readerContextFor(passage: String, term: String): String {
    if (passage.isBlank() || term.isBlank()) return ""
    val wanted = ReaderDictionary.headword(term).lowercase()
    if (wanted.isEmpty()) return ""
    for (sentence in ReaderSentenceSplit.split(passage)) {
        val holds = ReaderHeadwordShape.findAll(sentence).any { found ->
            ReaderDictionary.headword(found.value).lowercase() == wanted
        }
        if (holds) return sentence.replace(Regex("\\s+"), " ").trim()
    }
    return ""
}

/**
 * v431 — SHARING A PLACE (or the words selected on it).
 *
 * The reader had no share door before, and the member put one in the ⋯ menu. What
 * it shares is what a reader can actually vouch for: the passage if words are
 * selected, the book's name, and where in it they are — never a link to a file on
 * this phone.
 */
private fun shareReaderPlace(
    context: Context,
    book: String,
    place: String,
    passage: String
) {
    val body = buildString {
        if (passage.isNotBlank()) {
            append("\u201C").append(passage.trim()).append("\u201D\n\n")
        }
        append(book.ifBlank { "A book" })
        if (place.isNotBlank()) append(" \u00b7 ").append(place)
    }
    runCatching {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, book.ifBlank { "A book" })
            putExtra(android.content.Intent.EXTRA_TEXT, body)
        }
        context.startActivity(
            android.content.Intent.createChooser(send, "Share a passage")
        )
    }
}

@Composable
private fun ReaderPlacesSheet(
    /**
     * v431 — WHICH QUESTION THIS OPENING ANSWERS.
     *
     * v395 merged the reader's three questions into one scroll and v431 splits
     * them back out, because the member asked for the doors again: "keep the
     * bookmark button per chapter with the progress lets separate the bookmarks
     * again and it will be 3rd option with bookmarks". The shape that satisfies
     * both is this — ONE composable, ONE layout, and a mode that says which of
     * its parts an opening is for, so the four sheets can never drift apart and
     * the contents rows still see every mark (which is how a chapter knows it is
     * already bookmarked).
     */
    mode: ReaderPlacesMode,
    marks: List<ReaderMarkEntity>,
    position: ReaderMarkEntity?,
    /** v406 — where the member is RIGHT NOW, so the card reads live. */
    live: ReaderLivePlace?,
    content: ReaderContent?,
    chapters: List<ReaderOutlineEntry>,
    pages: List<ReaderOutlineEntry>,
    /** v440 — what the paged text flow says is left of the chapter (may be null). */
    sectionPagesLeft: Int? = null,
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
    // ── ONE LAYOUT, FOUR DOORS (v431) ────────────────────────────────
    //
    // A reader's questions have an order — where am I, what did I keep, what is
    // left — and the sheet answers them IN THAT ORDER: the progress card, then
    // the marks, then the book's own contents. Every chapter row carries its own
    // bookmark and its own progress (see [ReaderContentsSection]), which is the
    // half of the member's request that keeps the contents door worth opening.
    // IN THE BOOK'S OWN ORDER: a reader's marks are an index to the book, so
    // they read the way the book reads — from where it starts to where it ends —
    // and not in whatever order the table happens to hand them over (v399).
    val kept = marks.filter { !it.isPosition }.sortedBy { it.positionIndex }
    val shown = when (mode) {
        ReaderPlacesMode.BOOKMARKS -> kept.filter { it.markKind == ReaderMarkKind.BOOKMARK }
        ReaderPlacesMode.NOTES -> kept.filter { it.isNote }
        ReaderPlacesMode.HIGHLIGHTS -> kept.filter { it.markKind == ReaderMarkKind.HIGHLIGHT }
        else -> kept
    }
    val title = when (mode) {
        ReaderPlacesMode.CONTENTS -> "Contents"
        ReaderPlacesMode.BOOKMARKS -> "Bookmarks"
        ReaderPlacesMode.NOTES -> "Notes"
        ReaderPlacesMode.HIGHLIGHTS -> "Highlights"
        ReaderPlacesMode.ALL -> "Places in this book"
    }
    val marksLabel = when (mode) {
        ReaderPlacesMode.BOOKMARKS -> "BOOKMARKS"
        ReaderPlacesMode.NOTES -> "NOTES"
        ReaderPlacesMode.HIGHLIGHTS -> "HIGHLIGHTS"
        else -> "YOUR MARKS"
    }
    // Where the "where am I" card belongs: with the book's order and with the
    // places kept, never in front of a list of notes or highlights.
    val withProgress = mode == ReaderPlacesMode.ALL || mode == ReaderPlacesMode.CONTENTS ||
        mode == ReaderPlacesMode.BOOKMARKS
    val withMarks = mode != ReaderPlacesMode.CONTENTS
    val withContents = mode == ReaderPlacesMode.ALL || mode == ReaderPlacesMode.CONTENTS
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
    // ── v440 — AND WHAT IS LEFT OF THIS CHAPTER ─────────────────────
    //
    // Two books, two ways of knowing. A book that HAS pages answers it from the
    // outline, which carries "PDF: 1-based page the entry opens" for every chapter
    // — so the chapter's pages run from its own first page to the page the next
    // chapter opens on (or the book's last page). A reflowed book has no such map
    // here at all: its pages are the paged reader's own slicing, so IT reports the
    // count up ([sectionPagesLeft]) and this simply passes it on. A flow with no
    // pages — a novel read as a scroll — leaves it null and the card says nothing,
    // which is the only honest answer when there is no page to be on.
    val pagesLeftInChapter: Int? = when (val loaded = content) {
        is ReaderContent.Pages -> {
            val page = live?.index ?: position?.positionIndex
            val starts = chapters
                .mapNotNull { entry -> entry.page.takeIf { it > 0 }?.minus(1) }
                .distinct()
                .sorted()
            val opens = starts.lastOrNull { start -> page != null && start <= page }
            val next = starts.firstOrNull { start -> page != null && start > page }
            val end = next ?: loaded.pageCount
            if (page == null || opens == null) null else (end - page - 1).coerceAtLeast(0)
        }

        is ReaderContent.Text -> sectionPagesLeft

        null -> null
    }
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
    // v437 — A PANEL'S FLOOR (see [ReaderSheetFrame.minHeightFraction]): a list of
    // marks keeps 45% of the screen whether it holds twenty rows or none, because
    // the member's own read of the small one was that it looked broken rather than
    // empty.
    ReaderSheetFrame(title, palette, onDismiss, minHeightFraction = 0.45f) {
        // v434 — no scroll of its own: the SHEET's body is the one scroll now,
        // so this sheet cannot end up with a scroll inside a scroll (see
        // [ReaderSheetFrame]).
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (withProgress) {
                ReaderProgressCard(
                    through = through,
                    placeTitle = placeTitle,
                    countLabel = countLabel,
                    pagesLeft = pagesLeftInChapter,
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
            }

            if (withMarks) {
                ReaderPlacesHeader(
                    label = marksLabel,
                    trailing = if (shown.isEmpty()) "" else "${shown.size}",
                    palette = palette
                )
                ReaderMarksSection(
                    marks = shown,
                    content = content,
                    chapters = chapters,
                    palette = palette,
                    onJump = onJump,
                    onDelete = onDelete
                )
            }

            // The auto-bookmark is not one of the marks — it is the progress
            // card above — so the contents only appear when the file has an
            // order to show. A PDF with no outline still gets its pages, and
            // that IS its contents.
            if (withContents) {
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
    /**
     * v440 — HOW MANY PAGES OF THIS CHAPTER ARE STILL AHEAD, when that is known.
     *
     * The member asked for the line (*"'Pages left in this chapter' in the
     * progress card"*). It sits with the figure rather than in the header row,
     * because it is the same question the percentage answers — how much reading is
     * left — asked in the unit the member actually turns. Omitted entirely when no
     * surface could work it out: a made-up zero is worse than a missing line.
     */
    pagesLeft: Int? = null,
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
        // ── v440 — AND WHAT IS LEFT OF THE CHAPTER (see [pagesLeft]) ──
        if (pagesLeft != null) {
            Text(
                when (pagesLeft) {
                    0 -> "This is the chapter's last page"
                    1 -> "1 page left in this chapter"
                    else -> "$pagesLeft pages left in this chapter"
                },
                style = MaterialTheme.typography.bodySmall,
                color = palette.accent
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
        // ── v438 — AN EMPTY LIST IS AN EM DASH ──────────────────────────
        //
        // It said "Nothing marked yet — hold a passage while you read.", which is
        // a sentence every opening had to read again to learn the same thing. The
        // member: *"highliths and notes empty stat eis bad dot use erm dash"*. The
        // dash is the app's own way of saying "nothing here" everywhere else, and
        // the sheet's own title has already said which list is empty.
        Text(
            "\u2014",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.ink.copy(alpha = 0.45f)
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
    /** v434 — the selection bar's own dictionary door, on the same passage. */
    onDictionary: () -> Unit,
    /** v434 — and its share door, handing on the words themselves. */
    onShare: () -> Unit,
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
            // v434 — THE PASSAGE IS A PASSAGE, whichever way it was chosen: the
            // same two doors the swept selection has (see the reader body).
            ReaderSheetAction(
                glyph = CurioIcons.MenuBook,
                label = "Look a word up",
                palette = palette,
                onClick = onDictionary
            )
            ReaderSheetAction(
                glyph = CurioIcons.Share,
                label = "Share this passage",
                palette = palette,
                onClick = onShare
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
 *
 * ── v457 — [from]/[to] ARE THE MARK'S OWN PLACE, NOT A SEARCH RESULT ──────
 *
 * They are the character offsets the passage was swept from (in the block's
 * own text, or in a PDF page's glyph stream), or `-1` on a mark made before
 * v457 stored them. With them the wash lands where the member put it; without
 * them the renderer can only search for the words, and a phrase that occurs
 * twice in one block lands on the wrong one (see [ReaderMarkEntity]).
 */
private data class ReaderPassage(
    val text: String,
    val ink: Color,
    val from: Int = -1,
    val to: Int = -1
)

/**
 * EVERY HIGHLIGHT A BLOCK WEARS (v389c).
 *
 * All of them, not the first: the words ARE the mark now, so a paragraph can
 * hold two passages the member thought were worth keeping, and a renderer that
 * took only the first would quietly drop the second.
 */
private fun highlightsFor(marks: List<ReaderMarkEntity>, index: Int): List<ReaderPassage> =
    marks.filter { it.isHighlight && it.positionIndex == index }
        .map {
            ReaderPassage(
                text = it.text,
                ink = readerHighlighter(it.colorKey).ink,
                from = it.startIndex,
                to = it.endIndex
            )
        }

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
    /** v431 — the floating search's own words, washed where they were found. */
    query: String,
    /** v431 — whether THIS page holds the find the reader is standing on. */
    queryCurrent: Boolean,
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
                        // v434 — THE SWEEP OWNS THIS GESTURE (see [ReaderTouch]).
                        // Said before anything else, so a hold on the margin (the
                        // dock) is not panned out from under the finger either.
                        ReaderTouch.selecting = true
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
                    // The page may have this gesture back the moment it ends.
                    onDragEnd = { ReaderTouch.selecting = false },
                    onDragCancel = { ReaderTouch.selecting = false }
                )
            }
    ) {
        val words = text ?: return@Canvas
        val scale = if (words.pageWidthPt > 0f) size.width / words.pageWidthPt else 1f
        // ── v431 — THE FIND, WASHED ONTO THE WORDS IT WAS FOUND IN ─────────
        //
        // A search that only jumped the file and named a page in a list would
        // leave the member hunting the words with their eyes. Every occurrence on
        // every visible page is washed, and the one the member is STANDING ON is
        // washed harder, so the bar's arrows move a mark the eye can follow (the
        // member asked for the search to highlight the results ON the pdf).
        // Drawn before the highlights, so a passage the member marked themselves
        // still reads as theirs.
        if (query.isNotBlank()) {
            val wash = if (queryCurrent) {
                palette.accent.copy(alpha = 0.48f)
            } else {
                palette.accent.copy(alpha = 0.24f)
            }
            var from = 0
            var guard = 0
            // A bound on the walk: a one-letter query in a dense page can hit
            // hundreds of times, and a wash per hit is a wash nobody can read.
            while (guard < 240) {
                val at = words.text.indexOf(query, from, ignoreCase = true)
                if (at < 0) break
                drawPdfPassage(
                    text = words,
                    range = words.glyphRange(at, query.length),
                    color = wash,
                    scale = scale
                )
                from = at + query.length.coerceAtLeast(1)
                guard += 1
            }
        }
        highlights.forEach { passage ->
            if (passage.text.isBlank()) return@forEach
            // ── v457 — THE SWEPT GLYPHS, NOT A SEARCH (see [ReaderPassage]) ──
            //
            // A page's mark carries the glyph range it was swept from, so the
            // wash goes back onto those glyphs exactly. The word search is the
            // fallback for a mark made before the offsets were stored.
            val range = if (passage.from >= 0 && passage.to >= passage.from) {
                passage.from..passage.to
            } else {
                val at = words.text.indexOf(passage.text)
                if (at < 0) return@forEach
                words.glyphRange(at, passage.text.length)
            }
            drawPdfPassage(
                text = words,
                range = range,
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
    /**
     * THE INK THIS PASSAGE ALREADY WEARS, or null when it is unmarked.
     *
     * v443 — the dock's colours are the mark's own SWITCH now: the one in hand is
     * drawn as taken, and pressing it again TAKES THE HIGHLIGHT BACK (see the
     * caller). Without this the bar could only ever add a highlight, so the only
     * ways out of one were the sheet or the marks list.
     */
    appliedInk: ReaderHighlighter? = null,
    onHighlight: (ReaderHighlighter) -> Unit,
    onNote: () -> Unit,
    onBookmark: () -> Unit,
    /**
     * v465 — THE DICTIONARY DOOR, WHICH ONLY A PASSAGE NEEDS.
     *
     * Null when the selection is a single word: that one already has the pill
     * ABOVE this dock, which NAMES the word it will answer for — offering the
     * same door twice, once anonymously, would make the row the poorer of the
     * two. A passage has no such pill (see the caller), so the door belongs
     * here, beside the note and the bookmark, where the rest of the row's tools
     * live.
     */
    onDictionary: (() -> Unit)? = null,
    onMore: () -> Unit
) {
    // ── v438 — ONE WORD GETS THE WHOLE BAR BACK ──────────────────────
    //
    // v431 cut this bar down to the dictionary alone for a single word (the
    // member's rule then: *"when user hight one wor only show the dictionarcy
    // icon"*). Their answer now is the opposite: *"the hihglight screen dock only
    // have disconary option and nothing else … compare the dock in this commit see
    // it had many hihglith options etc, please fix the highlighht pill selecter in
    // pdf"*. One word is the commonest thing a reader highlights — an unfamiliar
    // word, a name, a term — so hiding the five pens behind it made the most
    // frequent action the one with the fewest tools. The bar is ONE row for every
    // selection now — the five pens, the note, the bookmark, the dictionary, the
    // ⋯ door and the cross — and the `singleWord` test that used to branch it is
    // gone with the branch (the dictionary door was already the row's own, so a
    // single word simply gets it beside the pens rather than instead of them).
    //
    // ── AND THE BAR IS A WIDE FLOATING CAPSULE ───────────────────────
    //
    // It used to be a two-row panel with the passage quoted in it — a card where
    // the member asked for the toolbar every phone has ("use similiar capsule
    // style wide floating ui for selected text too, current one is too small so
    // similiar to what samsung uses"). One row, a full-radius capsule, one lift,
    // icons only.
    //
    // ── v442 — AND IT WEARS THE DOCK'S OWN BODY ───────────────────────
    //
    // The member: *"the highlight dock is bad fix it too. weird shado and doesnt
    // match the dock"*. It was the palette's `surface` floating over the `paper` —
    // two colours about two per cent apart — so the only part of the capsule the
    // eye could see was a shadow spread over pale paper on every side: the exact
    // smudge the page slider wore before v441, still sitting over a page the member
    // is reading. It now speaks the same language the reader's other floating
    // pills do: an OPAQUE body (a blend of the surface toward the ink, so it is a
    // body wherever the shadow falls, and — being opaque — the shadow cannot bleed
    // through it), a hairline edge, and the same 8dp lift and 28dp radius the dock
    // uses. `animateContentSize` is gone with it: the bar is full width and its
    // height never changes, so it animated nothing and only asked for a layout pass
    // on every arrival.
    //
    // ── v448 — AND IT IS A REAL TOOLBAR, AT A REAL SIZE ────────────────
    //
    // The member: *"the highlight selected dock is too small and doesnt follow the
    // size parameter"*, and, asked what it should follow: **a fixed, bigger dock**.
    // The discs were 30dp and the doors 32dp — Material's own minimum touch target
    // is 48dp, so every one of them was a control the member had to aim at, on a
    // page they are holding one-handed. They are 42dp discs and 44dp doors now, the
    // padding grows with them, and the gutter between them is a real one (9dp) so
    // the row reads as tools rather than as ticks. The dictionary door went with it
    // — it is its own pill ABOVE this dock when one word is picked (see the caller),
    // which is the only time a meaning is what the member means.
    //
    // ── v465 — AND IT CAME BACK, FOR THE SELECTION THE PILL CANNOT SERVE ──
    //
    // That reasoning holds for ONE word and fails for a passage: the pill is drawn
    // only while `singleWord` is true, so a multi-word sweep had no dictionary door
    // anywhere on the page. The member's report is exactly that — *"when i select a
    // paragraph the dictionary option doesnt show"*. So a passage gets the door in
    // this row ([onDictionary], non-null only then) and the single word keeps the
    // pill: the two selections each have one dictionary door, and neither has two.
    val body = lerp(palette.surface, palette.ink, 0.06f)
    val edge = lerp(palette.surface, palette.ink, 0.16f)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = body,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, edge),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
                ReaderHighlighter.entries.forEach { ink ->
                    val worn = appliedInk == ink
                    Surface(
                        onClick = { onHighlight(ink) },
                        shape = CircleShape,
                        // ── v443 — THE COLOUR IN HAND IS DRAWN AS TAKEN, AND
                        // IT IS THE WAY BACK OUT. It was a 35% wash of itself
                        // with a hairline whether the passage wore it or not, so
                        // a marked passage's own colour looked exactly like the
                        // three it did not wear, and a second press re-wrote the
                        // same mark. Opaque with a CHECK while it is the
                        // passage's, and pressing it REMOVES the highlight (the
                        // caller answers that) — which is the same pairing the
                        // marking sheet's own colours wear (see [ReaderMarkSheet]),
                        // and it is what makes the × this bar used to carry
                        // unnecessary: the member: *"for the hihgligh selecter
                        // remove the frst x and when tappin git again the color it
                        // should deselect"*.
                        color = ink.ink.copy(alpha = if (worn) 1f else 0.35f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (worn) 2.dp else 1.dp,
                            if (worn) palette.ink else ink.ink
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (worn) {
                                CurioIcon(
                                    CurioIcons.Check,
                                    "Remove this highlight",
                                    // The ink that reads on the fill the member is
                                    // looking at (see `journalInkOn`) — a glyph in
                                    // the colour of its own disc is a blank disc.
                                    tint = journalInkOn(ink.ink),
                                    size = 20.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(ink.ink)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                SelectionBarAction(CurioIcons.Note, "Write a note on this passage", palette, onNote)
                SelectionBarAction(
                    CurioIcons.Bookmark,
                    "Bookmark this passage",
                    palette,
                    onBookmark
                )
                // ── v465 — AND A PASSAGE CAN BE LOOKED UP ──────────────────
                //
                // Drawn only when the caller hands it over (see [onDictionary]):
                // a single word's door is the pill above, a passage's is here. The
                // sheet it opens is handed the WHOLE passage, so its own candidate
                // chips can offer the word the member actually meant rather than
                // this bar guessing at the first one.
                if (onDictionary != null) {
                    SelectionBarAction(
                        CurioIcons.MenuBook,
                        "Look up a word in this passage",
                        palette,
                        onDictionary
                    )
                }
                SelectionBarAction(
                    CurioIcons.MoreHoriz,
                    "More about this passage",
                    palette,
                    onMore
                )
                // ── v443 — AND NO CROSS AT THE END ──────────────────────────
                //
                // A × here meant "nothing to do with this" — but a selection is
                // not a state the member has to clear: the ⋯ door is the way to
                // everything else, and a tap on the page puts the dock away (see
                // `tapPage`). What the member asked for is the mark's own switch:
                // the colour in hand, pressed again, takes the highlight back —
                // and with that in the row, the cross was a second, weaker way to
                // say "clear", so it is gone.
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
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = glyph,
                contentDescription = description,
                tint = palette.ink.copy(alpha = 0.8f),
                size = 21.dp
            )
        }
    }
}

internal enum class ReaderSkin(val key: String, val label: String) {
    PAPER("paper", "Paper"),
    SEPIA("sepia", "Sepia"),
    NIGHT("night", "Night"),
    WHITE("white", "White"),
    GRAY("gray", "Gray"),
    MINT("mint", "Mint"),
    ROSE("rose", "Rose"),
    AMBER("amber", "Amber"),
    SLATE("slate", "Slate");

    companion object {
        val inks: List<ReaderSkin> get() = entries

        /**
         * v431 — THE FIVE THAT FIT IN A ROW, and the rest behind the `+`.
         *
         * Five swatches is what a phone can show at a comfortable tap size, and
         * the fifth one is the member's own addition ("5 differnt backgroud color
         * the paper, sepia, night white and 2 more" → Gray, plus the `+` tile that
         * unfolds the tuned papers a reader reaches for at night or in the sun:
         * mint, rose, amber and slate).
         */
        val primary: List<ReaderSkin> get() = listOf(PAPER, SEPIA, NIGHT, WHITE, GRAY)
        val extra: List<ReaderSkin> get() = listOf(MINT, ROSE, AMBER, SLATE)
    }
}

/**
 * v431 — THE READER'S THREE TYPEFACES.
 *
 * The member asked for three and no more. They are the app's own three text
 * families, so a reader's page is set in the same type the rest of Curio writes
 * in: Lora (the editorial serif a novel expects), Fraunces (the display serif
 * Curio uses for headlines) and the app's writing hand for a page read aloud.
 * The choice follows the member through the process, like the ink, and it is the
 * FAMILY ONLY — every heading still keeps its level, its size and its weight.
 */
internal enum class ReaderTypeFace(val key: String, val label: String) {
    LORA("lora", "Lora"),
    FRAUNCES("fraunces", "Fraunces"),
    WRITING("writing", "Sans");

    companion object {
        fun of(key: String?): ReaderTypeFace =
            entries.firstOrNull { it.key == key } ?: LORA
    }
}

/** The font family a reader's page is set in, for one typeface key. */
internal fun readerTypeFamily(key: String?): FontFamily = when (ReaderTypeFace.of(key)) {
    ReaderTypeFace.LORA -> LoraFontFamily
    ReaderTypeFace.FRAUNCES -> FrauncesFontFamily
    ReaderTypeFace.WRITING -> WritingFontFamily
}

/**
 * THE READER'S OWN LOOK, held for the PROCESS rather than per book: a member
 * chooses an ink for READING, not for one novel, so the choice follows them from
 * book to book (and every reader on screen follows it at once). Compose state, so
 * choosing one repaints immediately; a plain object rather than a stored
 * preference because it costs nothing to choose again.
 */
internal object ReaderLook {
    var inkKey by mutableStateOf(ReaderSkin.PAPER.key)

    /**
     * v431 — THE READER'S TYPEFACE (see [ReaderTypeFace]). It follows the member
     * through the process exactly like the ink, and it is READ BY THE PAGE, never
     * written per book: a reader picks the type they read in, not the type for
     * one novel.
     */
    var typeFace by mutableStateOf(ReaderTypeFace.LORA.key)

    /**
     * v431 — THE PAGE COUNT, PINNED IN THE CORNER.
     *
     * A HOLD on the foot pill's Pages button pins a small counter in the corner,
     * where it stays while the chrome is away and while the member reads — the
     * answer to "which page is this" for a reader who keeps looking back at it.
     * A tap on the counter takes the pin out again (see [ReaderPinnedPage]).
     */
    var pinnedPage by mutableStateOf(false)

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

    /**
     * v434 — AND THE REST OF THE PAGE'S LOOK, the settings a reader adjusts by
     * eye rather than by word: the leading, the side air, the gap between
     * paragraphs, whether the lines are justified, whether the screen is kept
     * awake, and how far the paper is dimmed for reading in the dark.
     *
     * They are the same kind of choice as the ink and the typeface — a reading
     * preference, not a preference about one book — so they follow the member
     * from book to book and are REMEMBERED across restarts (see
     * [ReaderLookStore]).
     */
    var lineSpacing by mutableStateOf(1f)
    var pageMargin by mutableStateOf(22f)
    var paraSpacing by mutableStateOf(1f)
    var justify by mutableStateOf(false)
    var keepScreenOn by mutableStateOf(false)
    var dim by mutableStateOf(0f)

    /**
     * v440 — WHEN THE NIGHT DIM COMES ON: always, or inside a window.
     *
     * The member asked for a night dim that works on a schedule rather than by
     * hand (*"Night dim on a schedule (auto at sunset, not just manual)"*), and
     * then, after living with it:
     *
     *     "add at sunset customisation to be able to set the tiem"
     *
     * v440 answered that with the phone's own dark theme (which is what a phone set
     * to automatic switches at sunset, so no location permission is ever needed).
     * v442 answers it the way they asked for it: **a window the member sets** —
     * [dimFromMinute] to [dimUntilMinute] — because a member reading at 19:00 in
     * winter is not reading at 19:00 in summer, and the page they are on is the page
     * that is too bright, not the system chrome.
     *
     * The DEFAULT window is the evening one (20:00 → 06:00), which is where the
     * dim's own switch used to leave it, and the default MODE stays "Dim always":
     * every member who had a dim before this keeps exactly the dim they had, at
     * exactly the level they set, at every hour.
     */
    var dimAuto by mutableStateOf(false)

    /** When the dim's window OPENS — minutes since midnight (see [dimAuto]). */
    var dimFromMinute by mutableStateOf(20 * 60)

    /** And when it closes again. May be smaller than [dimFromMinute]: a window
     *  that runs over midnight is the normal evening case, and
     *  [dimWindowContains] reads it as one. */
    var dimUntilMinute by mutableStateOf(6 * 60)

    /**
     * v442 — IS THE CLOCK INSIDE THE DIM'S WINDOW?
     *
     * A window that RUNS OVER MIDNIGHT is the normal case (on at 20:00, off at
     * 06:00), so the test is not `from until until` alone: inside is anything from
     * the opening minute onward OR anything before the closing minute. Equal ends
     * mean the member set no window at all, which reads as "all day" rather than as
     * a zero-length window that would silently switch the dim off.
     */
    fun dimWindowContains(minuteOfDay: Int): Boolean {
        val from = dimFromMinute.coerceIn(0, READER_MINUTES_IN_DAY - 1)
        val until = dimUntilMinute.coerceIn(0, READER_MINUTES_IN_DAY - 1)
        if (from == until) return true
        return if (from < until) minuteOfDay in from until until
        else minuteOfDay >= from || minuteOfDay < until
    }

    /**
     * v440 — WHICH DICTIONARY ANSWERS A LOOKUP (see [ReaderDictionarySource]).
     *
     * The member, asked what "bundled vs online" should mean once they heard there
     * is no bundled dictionary in the app: **two online sources to choose between**.
     * It is a READING preference and lives here with the rest of them, so it follows
     * the member from book to book and is remembered across restarts
     * (see [ReaderLookStore]).
     */
    var dictionary by mutableStateOf(ReaderDictionarySource.WIKTIONARY)

    /**
     * v440 — HOW FAST IT READS, AND IN WHOSE VOICE (see [ReaderSpeaker]).
     *
     * The member's own pick (*"Read-aloud: a speed and voice picker"*). Both are
     * READING preferences and are remembered with the rest of the look: a member who
     * needs a slower voice on a book needs it on the next book too. The VOICE is held
     * by its engine-assigned NAME ("en-us-x-sfg#female_1-local") rather than an index,
     * because a phone that adds or reorders its voices would otherwise silently change
     * which one the member reads in.
     *
     * The SPEAKING state itself is NOT here: whether a voice is mid-sentence is a fact
     * about this visit to this screen, not a preference — a book that reopened with the
     * voice already talking would be an ambush (see the reader's own driver).
     */
    var speakSpeed by mutableStateOf(1f)
    var speakVoice by mutableStateOf("")

    /**
     * v464 — WHICH TEXT-TO-SPEECH ENGINE READS (see [ReaderSpeaker.engines]).
     *
     * The member: *"wire the read-aloud voice engine to any installed system TTS engine,
     * so I can point Curio at a better voice I install myself"*. Android lets any app
     * provide speech by answering `android.intent.action.TTS_SERVICE`, and the platform's
     * own engine is usually the plainest one on the phone — so the reader can be pointed
     * at whichever engine the member prefers, including one they installed for exactly
     * that reason. That is the whole feature: no model to download into Curio, no bundled
     * runtime, and a better voice is a better ENGINE rather than a bigger APK.
     *
     * Empty means THE PHONE'S OWN, which is what the platform would have used anyway — so
     * a member who never opens the row hears exactly the voice they always heard.
     */
    var speakEngine by mutableStateOf("")

    /**
     * v465e — WHICH NARRATOR INSIDE THE DOWNLOADED PACK, as a speaker id.
     *
     * A `sid` is an INDEX into the pack's own speaker table, not a name — "3"
     * means `af_sarah` for Kokoro and nothing at all for Piper, which has one
     * voice and ignores it (see [NeuralVoicePacks.Pack.speakers]). It is stored
     * as an Int and clamped at the reading edge rather than validated on write,
     * because the table a sid belongs to can change under a member's feet: they
     * pick narrator 8, then delete the 11-voice pack and download one with four.
     * A stale 8 must land on a real voice, not on silence or a crash.
     */
    var speakSpeaker by mutableStateOf(0)

    /**
     * v439 — LOW POWER READING, AND IT IS ON FROM THE START.
     *
     * The member: *"in pdf reader, a high charge save turns on which makes the
     * app cache and background usage very low in reder so the phone doesnt
     * heat"*. Reading is the one screen a member can sit on for an hour, and it
     * was spending more than the words cost:
     *
     *  · Every PDF page was rendered as a full-screen ARGB_8888 bitmap with an
     *    alpha channel nothing composites against, up to THREE TIMES the screen's
     *    own width — pixels that are never displayed, held while the page is on
     *    screen. On ([lowPower]) they are RGB_565 and the upscale stops at 1.5×.
     *  · The pager kept the neighbouring page composed (`beyondViewportPageCount
     *    = 1`) so a turn never painted from scratch — a second full-size bitmap,
     *    a second text extraction and a second set of highlight overlays, for a
     *    page the member may never look at. On, the pager composes the page it
     *    shows and nothing else.
     *  · The pictures an EPUB copies out of its archive are written to
     *    `cacheDir/book-images` and stayed there until Android felt like
     *    reclaiming them. On, the reader prunes that folder as it closes.
     *
     * It is a REAL toggle rather than a hidden flag, in the reader's own Screen
     * section: a member on a tablet with a charger attached may well prefer the
     * sharper page, and "it got blurrier and I could not turn it off" is not a
     * trade a reading app gets to make for someone (see [ReaderLookStore]).
     */
    var lowPower by mutableStateOf(true)

    /**
     * v439 — THE MOTION LOCK, AND THE ZOOM SLIDER IT REPLACED.
     *
     * The member: *"in pdf only remove that zoom slider and add the motion lock
     * pill which restrits that drag to move and pinch to zoom it locks in the
     * state the user left the zoom position"*, then, asked what the lock freezes,
     * *"Freeze pan and pinch, and remember it"*.
     *
     * So: a member magnifies a page once, to the size they read it at, and then
     * says so — from that moment the page cannot be zoomed or dragged by accident
     * while they read or sweep a highlight, and the position they left it in is
     * the position it comes back to. It is remembered (this field is in
     * [rememberKey]) because a member who reads a PDF at 2× reads every PDF at 2×.
     *
     * **ENFORCED IN THE GESTURE PATH, NEVER IN THE DRAWING.** The lock is read by
     * [pinchToZoom] — the one handler EVERY gesture in the reader passes through —
     * where drags and pinches are swallowed, and by [readerDoubleTapZoom].
     * Swallowing is deliberate rather than "not applying": a drag it merely
     * ignored would still be claimed by the scrolling column or the pager
     * underneath, so the page would move anyway. It must NOT swallow a gesture's
     * first down, though — a tap has to keep turning the page and putting the
     * chrome back (see the guard's own note).
     */
    var motionLock by mutableStateOf(false)

    /**
     * v434 — EVERYTHING THE MEMBER CHOSE, as one string, for the store.
     *
     * Read inside a `snapshotFlow`, so every field below is tracked and the write
     * happens once per settling change rather than once per slider pixel (see the
     * reader body).
     */
    fun rememberKey(): String = listOf(
        inkKey,
        typeFace,
        textScale.toString(),
        textFlow.name,
        pageFlow.name,
        orientation.name,
        tapZones.toString(),
        lineSpacing.toString(),
        pageMargin.toString(),
        paraSpacing.toString(),
        justify.toString(),
        keepScreenOn.toString(),
        dim.toString(),
        // v440 — the dim's own WHEN, in the key like every other look field (the
        // v434 rule): a field left out of here saves every other setting and
        // silently forgets this one.
        dimAuto.toString(),
        // v442 — and the window it comes on in: a field left out of here saves
        // every other setting and silently forgets this one (the v434 rule).
        dimFromMinute.toString(),
        dimUntilMinute.toString(),
        // v440 — and which dictionary the lookups go to (the v434 rule).
        dictionary.key,
        speakSpeed.toString(),
        speakVoice,
        // v464 — and which engine reads (the v434 rule: a field left out of here saves
        // every other setting and silently forgets this one).
        speakEngine,
        // v465e — and which narrator inside a downloaded pack (the v434 rule again).
        speakSpeaker.toString(),
        lowPower.toString(),
        // v439 — the motion lock MUST be in here, or it saves all of the other
        // fields and silently forgets this one (the v434 rule).
        motionLock.toString()
    ).joinToString("|")
}

/**
 * v463 — HOW FAR THE PAGE STANDS BACK while a voice is reading one of its sentences.
 *
 * The member asked for the sentence being read to be the one thing that reads, with the
 * rest of the page going quiet behind it — but not so quiet that a member following along
 * by eye cannot make the next paragraph out. It is applied ONLY while the voice is
 * actually running: a paused voice keeps its sentence marked and hands the page its
 * contrast back, because a page dimmed indefinitely is a page that is hard to read.
 */
private const val READ_ALOUD_DIM = 0.38f

/**
 * v434 — how the lines sit in their column (see [ReaderLook.justify]).
 *
 * The glyph is DRAWN rather than named ([CurioIcon]): the bundled Material
 * Symbols subset carries none of the `format_align_*` ligatures — the four
 * alignments elsewhere in Curio are drawn for exactly that reason — so a named
 * icon here would render as a blank pill.
 */
internal enum class ReaderAlign(val label: String, val justified: Boolean) {
    LEFT("Ragged", false),
    JUSTIFIED("Justified", true);

    companion object {
        fun of(justify: Boolean): ReaderAlign = if (justify) JUSTIFIED else LEFT
    }
}

/**
 * v434 — THE READER'S LOOK, REMEMBERED.
 *
 * The reader's preferences used to live for the process only: every restart put
 * the page back to paper and 1.0× type, and a member who reads in sepia at 1.4×
 * had to say so again every morning (member's own ask: "Remember all of these
 * across restarts"). One prefs file — the same `curio_prefs` the rest of the app
 * uses — and one value per field, so a single unreadable row can never take the
 * others down with it.
 */
internal object ReaderLookStore {
    private const val PREFS = "curio_prefs"
    private const val MARK = "reader_look_v434"

    private const val INK = "reader_ink"
    private const val FACE = "reader_face"
    private const val SCALE = "reader_text_scale"
    private const val FLOW_TEXT = "reader_text_flow"
    private const val FLOW_PAGE = "reader_page_flow"
    private const val ORIENT = "reader_orientation"
    private const val ZONES = "reader_tap_zones"
    private const val LEADING = "reader_line_spacing"
    private const val MARGIN = "reader_page_margin"
    private const val PARA = "reader_para_spacing"
    private const val JUSTIFY = "reader_justify"
    private const val KEEP_ON = "reader_keep_screen_on"
    private const val DIM = "reader_dim"
    private const val LOW_POWER = "reader_low_power"
    private const val DIM_AUTO = "reader_dim_auto"
    // v442 — the dim's own window, in minutes since midnight (see
    // [ReaderLook.dimWindowContains]). A minute rather than an hour, because the
    // member sets a TIME and a clock picker offers minutes.
    private const val DIM_FROM = "reader_dim_from"
    private const val DIM_UNTIL = "reader_dim_until"
    private const val DICTIONARY = "reader_dictionary"
    private const val SPEAK_SPEED = "reader_speak_speed"
    private const val SPEAK_VOICE = "reader_speak_voice"
    private const val SPEAK_ENGINE = "reader_speak_engine"
    private const val SPEAK_SPEAKER = "reader_speak_speaker"
    private const val MOTION_LOCK = "reader_motion_lock"

    /**
     * Read once, on the way into a reader. Does nothing at all on a fresh install:
     * the object's own defaults ARE the answer then, and the mark is only written
     * on the first save.
     */
    /**
     * v448 — AND IT LOADS ONCE PER PROCESS, NOT ONCE PER SCREEN.
     *
     * The member: *"the wide option isnt working, it works and it rotates and then
     * it rotates back to upright"*. The cause was this function running again after
     * the rotation it had just caused: the reader is rebuilt by the config change,
     * the rebuild read the store, and the 400ms-debounced save had not landed yet —
     * so the store still said what the member had just replaced, their choice was
     * overwritten, and the window turned straight back. The look IN MEMORY is the
     * truth for as long as the process lives (every screen writes the same object),
     * so a later entry into a reader has nothing to learn.
     */
    @Volatile
    private var loadedThisProcess = false

    fun load(context: Context) {
        if (loadedThisProcess) return
        val prefs = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }.getOrNull() ?: return
        if (!prefs.contains(MARK)) return
        loadedThisProcess = true
        runCatching {
            ReaderLook.inkKey = prefs.getString(INK, ReaderLook.inkKey) ?: ReaderLook.inkKey
            ReaderLook.typeFace = prefs.getString(FACE, ReaderLook.typeFace) ?: ReaderLook.typeFace
            ReaderLook.textScale = prefs.getFloat(SCALE, ReaderLook.textScale).coerceIn(0.8f, 2.6f)
            ReaderLook.textFlow = flowOf(prefs.getString(FLOW_TEXT, null), ReaderLook.textFlow)
            ReaderLook.pageFlow = flowOf(prefs.getString(FLOW_PAGE, null), ReaderLook.pageFlow)
            ReaderLook.orientation = orientationOf(
                prefs.getString(ORIENT, null),
                ReaderLook.orientation
            )
            ReaderLook.tapZones = prefs.getBoolean(ZONES, ReaderLook.tapZones)
            ReaderLook.lineSpacing = prefs.getFloat(LEADING, ReaderLook.lineSpacing)
                .coerceIn(0.85f, 1.6f)
            ReaderLook.pageMargin = prefs.getFloat(MARGIN, ReaderLook.pageMargin)
                .coerceIn(10f, 40f)
            ReaderLook.paraSpacing = prefs.getFloat(PARA, ReaderLook.paraSpacing)
                .coerceIn(0.6f, 2f)
            ReaderLook.justify = prefs.getBoolean(JUSTIFY, ReaderLook.justify)
            ReaderLook.keepScreenOn = prefs.getBoolean(KEEP_ON, ReaderLook.keepScreenOn)
            ReaderLook.dim = prefs.getFloat(DIM, ReaderLook.dim).coerceIn(0f, 0.6f)
            ReaderLook.dimAuto = prefs.getBoolean(DIM_AUTO, ReaderLook.dimAuto)
            ReaderLook.dimFromMinute =
                prefs.getInt(DIM_FROM, ReaderLook.dimFromMinute).coerceIn(0, 24 * 60 - 1)
            ReaderLook.dimUntilMinute =
                prefs.getInt(DIM_UNTIL, ReaderLook.dimUntilMinute).coerceIn(0, 24 * 60 - 1)
            ReaderLook.dictionary = ReaderDictionarySource.fromKey(
                prefs.getString(DICTIONARY, ReaderLook.dictionary.key)
            )
            ReaderLook.speakSpeed = prefs.getFloat(SPEAK_SPEED, ReaderLook.speakSpeed)
                .coerceIn(0.5f, 2.5f)
            ReaderLook.speakVoice = prefs.getString(SPEAK_VOICE, ReaderLook.speakVoice).orEmpty()
            ReaderLook.speakEngine =
                prefs.getString(SPEAK_ENGINE, ReaderLook.speakEngine).orEmpty()
            ReaderLook.speakSpeaker = prefs.getInt(SPEAK_SPEAKER, ReaderLook.speakSpeaker)
            ReaderLook.lowPower = prefs.getBoolean(LOW_POWER, ReaderLook.lowPower)
            ReaderLook.motionLock = prefs.getBoolean(MOTION_LOCK, ReaderLook.motionLock)
        }
    }

    /** The whole look, written as one commit so a half-saved page never exists. */
    fun save(context: Context) {
        val prefs = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }.getOrNull() ?: return
        runCatching {
            prefs.edit()
                .putString(INK, ReaderLook.inkKey)
                .putString(FACE, ReaderLook.typeFace)
                .putFloat(SCALE, ReaderLook.textScale)
                .putString(FLOW_TEXT, ReaderLook.textFlow.name)
                .putString(FLOW_PAGE, ReaderLook.pageFlow.name)
                .putString(ORIENT, ReaderLook.orientation.name)
                .putBoolean(ZONES, ReaderLook.tapZones)
                .putFloat(LEADING, ReaderLook.lineSpacing)
                .putFloat(MARGIN, ReaderLook.pageMargin)
                .putFloat(PARA, ReaderLook.paraSpacing)
                .putBoolean(JUSTIFY, ReaderLook.justify)
                .putBoolean(KEEP_ON, ReaderLook.keepScreenOn)
                .putFloat(DIM, ReaderLook.dim)
                .putBoolean(DIM_AUTO, ReaderLook.dimAuto)
                .putInt(DIM_FROM, ReaderLook.dimFromMinute)
                .putInt(DIM_UNTIL, ReaderLook.dimUntilMinute)
                .putString(DICTIONARY, ReaderLook.dictionary.key)
                .putFloat(SPEAK_SPEED, ReaderLook.speakSpeed)
                .putString(SPEAK_VOICE, ReaderLook.speakVoice)
                .putString(SPEAK_ENGINE, ReaderLook.speakEngine)
                .putInt(SPEAK_SPEAKER, ReaderLook.speakSpeaker)
                .putBoolean(LOW_POWER, ReaderLook.lowPower)
                .putBoolean(MOTION_LOCK, ReaderLook.motionLock)
                .putBoolean(MARK, true)
                .apply()
        }
    }

    private fun flowOf(key: String?, fallback: ReaderFlow): ReaderFlow =
        ReaderFlow.entries.firstOrNull { it.name == key } ?: fallback

    private fun orientationOf(key: String?, fallback: ReaderOrientation): ReaderOrientation =
        ReaderOrientation.entries.firstOrNull { it.name == key } ?: fallback
}

/** v418 — the reader's orientation choice (see [ReaderLook.orientation]). */
internal enum class ReaderOrientation(val label: String, val detail: String) {
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
internal enum class ReaderFlow(val label: String) {
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
internal data class ReaderPalette(
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
    // ── v431 — THE TUNED PAPERS, AS A REAL PAPER MAP ────────────────
    //
    // `out = a · in + ink`, with `a = (paper - ink) / 255`, which is the honest
    // way to move a rendered page onto a different sheet: WHITE paper lands on
    // the skin's paper colour and BLACK type lands on the skin's ink, so the page
    // keeps its own contrast instead of being washed by a flat scale.
    ReaderSkin.GRAY.key -> pdfTint(0.960f, 0.960f, 0.960f, 10f, 10f, 10f)
    ReaderSkin.MINT.key -> pdfTint(0.816f, 0.827f, 0.824f, 26f, 32f, 26f)
    ReaderSkin.ROSE.key -> pdfTint(0.804f, 0.796f, 0.792f, 42f, 32f, 34f)
    ReaderSkin.AMBER.key -> pdfTint(0.769f, 0.769f, 0.745f, 54f, 42f, 24f)
    ReaderSkin.SLATE.key -> pdfTint(-0.674f, -0.706f, -0.722f, 198f, 210f, 220f)
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
internal enum class ReaderZoneAction(val label: String, val hint: String) {
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
internal enum class ReaderZoneEdge(val label: String) {
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
    // ── v434 — THE OVERLAY GETS OUT OF THE WAY (member's own instruction) ──
    //
    // "let user hide the overlay so they can see what they are doing and only
    // overlay the slider when they adjust and hide the overlay when they use the
    // slider so they can see what they are chnaging". So: an eye on the panel
    // takes the washes off entirely, and the depth slider takes them off for as
    // long as it is being dragged. The GRIPS stay either way, because they are
    // the handles the member is placing, not the thing covering the page.
    var showOverlay by remember { mutableStateOf(true) }
    var showDepth by remember { mutableStateOf(false) }
    var draggingDepth by remember { mutableStateOf(false) }
    // ── v440 — AND THE BOX ITSELF STANDS DOWN (the rest of the member's ask) ──
    //
    // "the gesture box hide that when adjusting area and a way to hide that box
    // not the backgroud thing, and a way to make it appear again to edit".
    //
    // So the gestures box answers to two things now, where the eye only ever
    // reached the WASHES: a finger placing an edge takes it off the page for as long
    // as the finger is down ([adjustingZone] — the member is aiming at real words
    // through the exact part of the screen the box occupies), and a door on it puts
    // it away for good ([panelUp]). Either way it comes back — the drag ends, or the
    // small "Gestures" pill in the corner is tapped — so nothing here can be
    // hidden from its owner.
    var panelUp by remember { mutableStateOf(true) }
    var adjustingZone by remember { mutableStateOf(false) }
    val washesUp = showOverlay && !draggingDepth
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // ── A TAP ON THE PAGE PICKS ITS ZONE (v434) ────────────────
            //
            // The page underneath must not answer while the gestures are being
            // placed — one tap here is one edit and never a page turn — and the
            // edit it IS is the obvious one: whichever edge the finger landed in
            // becomes the edge being edited, and a tap in the middle picks the
            // nearest edge to it (member: "selecting one tap zone should switch
            // its area").
            .pointerInput(Unit) {
                detectTapGestures { at -> chosen = zoneEdgeAt(at, size) }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val boxSize = IntSize(constraints.maxWidth, constraints.maxHeight)

        // ── THE ZONES, DRAWN WHERE THEY ARE ─────────────────────────
        Canvas(Modifier.fillMaxSize()) {
            if (widthPx <= 0f || heightPx <= 0f || !washesUp) return@Canvas
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
                onSelect = { chosen = edge },
                onAdjust = { adjustingZone = it }
            )
        }

        // ── THE PANEL: WHAT THE CHOSEN EDGE ASKS FOR, AND HOW DEEP ──
        //
        // v440 — and it is a FLOATING pill that can leave, on the one motion clock
        // every other pill in the app uses: away while an edge is under a finger,
        // and away for as long as the member wants the page to themselves.
        AnimatedVisibility(
            visible = panelUp && !adjustingZone,
            enter = CurioMotion.pillArrive(),
            exit = CurioMotion.pillLeave(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(28.dp),
                color = palette.paper.copy(alpha = 0.98f),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Gestures",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ink
                        )
                        Spacer(Modifier.weight(1f))
                        // THE EYE: the page, without the washes (see [washesUp]).
                        Surface(
                            onClick = { showOverlay = !showOverlay },
                            shape = CircleShape,
                            color = if (showOverlay) accent.copy(alpha = 0.14f)
                            else ink.copy(alpha = 0.07f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ReaderEyeGlyph(
                                    open = showOverlay,
                                    tint = if (showOverlay) accent else ink.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
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
                        // ── v440 — AND OUT OF THE WAY, ON PURPOSE ───────────
                        //
                        // The eye takes the washes off the page; THIS takes the box off
                        // it, which is the other half of the same wish. It is a small
                        // round door like every other control in the row, and the pill
                        // it leaves behind in the corner brings it straight back.
                        Surface(
                            onClick = { panelUp = false },
                            shape = CircleShape,
                            color = ink.copy(alpha = 0.07f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CurioIcon(
                                    CurioIcons.ArrowDownward,
                                    "Hide the gestures box",
                                    tint = ink.copy(alpha = 0.7f),
                                    size = 18.dp
                                )
                            }
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
                        when {
                            !ReaderLook.tapZones ->
                                "The gestures are off — turn them on in Reading settings."
                            showOverlay -> "Tap a zone on the page to pick it, then say what it does."
                            else -> "The washes are off — the page is clear to read."
                        },
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
                    // ── THE DEPTH, ON DEMAND (v434) ────────────────────────
                    //
                    // It used to stand here always, taking a row of a panel that
                    // floats over the very lines it governs. It is a capsule now: a
                    // tap opens it, and while it is dragged the washes come off the
                    // page so the member can see the edge move against real words.
                    Surface(
                        onClick = { showDepth = !showDepth },
                        shape = RoundedCornerShape(50),
                        color = if (showDepth) accent.copy(alpha = 0.14f) else ink.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CurioIcon(CurioIcons.Tune, null, tint = accent, size = 17.dp)
                            Text(
                                "Depth",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = ink.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(chosen.depth() * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = accent
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = showDepth,
                        enter = CurioMotion.pillArrive(fromTop = true),
                        exit = CurioMotion.pillLeave(fromTop = true)
                    ) {
                        Slider(
                            value = chosen.depth(),
                            onValueChange = {
                                // The page is what the member is aiming at, so the
                                // washes step out of the way for the drag itself.
                                draggingDepth = true
                                chosen.setDepth(it)
                            },
                            onValueChangeFinished = { draggingDepth = false },
                            valueRange = ReaderZoneEdge.DEPTH_MIN..ReaderZoneEdge.DEPTH_MAX,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = ink.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
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
        // ── v440 — WHAT THE BOX LEFT BEHIND ────────────────────────
        //
        // A hidden box with no way back is a trap the member cannot get out of, so
        // the moment the panel stands down this takes its place: one small pill in
        // the corner that says what it is and opens it again. The page is clear, and
        // the way to edit it is still one tap away.
        AnimatedVisibility(
            visible = !panelUp,
            enter = CurioMotion.popArrive(),
            exit = CurioMotion.popLeave(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            Surface(
                onClick = { panelUp = true },
                shape = RoundedCornerShape(50),
                color = palette.paper.copy(alpha = 0.98f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurioIcon(CurioIcons.Tune, null, tint = accent, size = 17.dp)
                    Text(
                        "Gestures",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ink.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * v434 — WHICH EDGE A TAP PICKED: its zone if the tap was inside one, otherwise
 * the edge the finger was nearest (see [ReaderTapZoneEditor]).
 *
 * The same arithmetic the tap itself uses ([readerZoneActionAt]), so picking an
 * edge and triggering it can never disagree about where the edge is.
 */
private fun zoneEdgeAt(at: Offset, size: IntSize): ReaderZoneEdge {
    if (size.width <= 0 || size.height <= 0) return ReaderZoneEdge.LEFT
    val left = size.width * ReaderLook.zoneLeftDepth
    val right = size.width * (1f - ReaderLook.zoneRightDepth)
    val top = size.height * ReaderLook.zoneTopDepth
    val bottom = size.height * (1f - ReaderLook.zoneBottomDepth)
    return when {
        at.x <= left -> ReaderZoneEdge.LEFT
        at.x >= right -> ReaderZoneEdge.RIGHT
        at.y <= top -> ReaderZoneEdge.TOP
        at.y >= bottom -> ReaderZoneEdge.BOTTOM
        else -> listOf(
            ReaderZoneEdge.LEFT to at.x,
            ReaderZoneEdge.RIGHT to (size.width - at.x),
            ReaderZoneEdge.TOP to at.y,
            ReaderZoneEdge.BOTTOM to (size.height - at.y)
        ).minBy { it.second }.first
    }
}

/**
 * v434 — AN EYE, DRAWN (the bundled font has `visibility_off` but no `visibility`).
 *
 * Open: the lash and the pupil. Closed: the lash and a strike through it — the
 * same two states every eye toggle in the world has.
 */
@Composable
internal fun ReaderEyeGlyph(open: Boolean, tint: Color, iconSize: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(iconSize)) {
        val stroke = this.size.height * 0.09f
        drawOval(
            color = tint,
            topLeft = Offset(0f, this.size.height * 0.24f),
            size = Size(this.size.width, this.size.height * 0.52f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        if (open) {
            drawCircle(
                color = tint,
                radius = this.size.height * 0.14f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
        } else {
            drawLine(
                color = tint,
                start = Offset(this.size.width * 0.16f, this.size.height * 0.84f),
                end = Offset(this.size.width * 0.84f, this.size.height * 0.16f),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
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
    onSelect: () -> Unit,
    /**
     * v440 — WHETHER THIS HANDLE IS BEING PLACED RIGHT NOW.
     *
     * True from the finger going down to it coming up. The editor uses it to take
     * its own panel off the page for the drag (member: *"the gesture box hide that
     * when adjusting area"*) — the member is aiming an edge at real lines of text, and
     * a white slab across the foot of the screen is the one thing in the way of that.
     */
    onAdjust: (Boolean) -> Unit = {}
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
                detectDragGestures(
                    onDragStart = { onAdjust(true) },
                    onDragEnd = { onAdjust(false) },
                    onDragCancel = { onAdjust(false) }
                ) { change, drag ->
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

/**
 * v432 — WHETHER THE GESTURE IN FLIGHT HAS HAD A SECOND FINGER ON IT.
 *
 * A pinch ends with the fingers lifting within a frame of each other, and Android
 * delivers that as ONE event with every change up — which is precisely what
 * `detectTapGestures` reads as a TAP. So every zoom ended by tapping the page it
 * was made on: the reader's tools came and went with the gesture, and a lift near
 * the side of the screen turned the page instead of putting the chrome back
 * (member: "its appear and disapper of the tools").
 *
 * The pinch says so here, on the second finger down, and every tap the reader
 * answers asks first. It is cleared on the NEXT gesture's first finger — in
 * [pinchToZoom]'s `awaitFirstDown(requireUnconsumed = false)`, which is the one
 * place that hears EVERY gesture in the reader, whatever surface it starts on.
 */
internal object ReaderTouch {
    var multi by mutableStateOf(false)

    /**
     * v434 — A SWEEP IS IN FLIGHT.
     *
     * A long press on a magnified PDF page opens a word sweep, and the page's
     * own one-finger PAN claims every drag from its first move (see
     * [pinchToZoom]) — which cancelled the sweep before it could report a
     * single word (member: "when im zoomed in and i try to tap and hold to
     * select it doesnt work"). The sweep says so here the moment its long press
     * fires, and the pan stands down for the rest of the gesture.
     */
    var selecting by mutableStateOf(false)
}

/**
 * v442 — THE SIDE TAP, ANSWERED AT ONCE.
 *
 * The member: *"the side tap gesture its slow doesnt work faster receives one tap
 * and doesnt work anymore and doesnt work sometimes fix it"*. Three symptoms, one
 * cause, and it was never the zone arithmetic — it was WHERE the tap was heard.
 *
 * A tap on a reading surface is answered by the surface's own [detectTapGestures],
 * which ALSO owns the double tap (the pinch-at-one-point — see
 * [readerDoubleTapZoom] and [readerDoubleTapDocument]). A detector waiting to see
 * whether a second tap follows cannot answer the first one until the double-tap
 * window has passed, so a tap in a side zone was late by construction (the
 * "slow"), and a second tap inside that window was read as the FIRST HALF of a
 * double tap and zoomed the page instead of turning it (the "receives one tap and
 * doesnt work anymore"). Where a child of the surface claimed the gesture first,
 * nobody heard it at all (the "doesnt work sometimes").
 *
 * A zone tap is not ambiguous: the member asked for the SIDE OF THE SCREEN to act,
 * and it acts the moment the finger lifts. This handler is placed INNERMOST in its
 * chain, so it processes the up before the surface's own tap detector does, and it
 * CONSUMES the up it answered — which is what cancels the double-tap detector's
 * wait, so one tap is one page turn and never a zoom. Everything else about the
 * gesture is left alone: only a tap whose point falls in a zone is claimed, and
 * only when nothing else in the reader already owns the gesture.
 *
 * The gesture must still BE a tap, which is three guards and all of them matter:
 *  · a down a real control (a chrome button, a chip) already claimed is skipped by
 *    `awaitFirstDown()`'s own `requireUnconsumed`, so the foot pill's buttons never
 *    also fire the bottom zone;
 *  · a press held past the long-press threshold is the SWEEP's, not a tap's, so it
 *    is dropped on the timeout rather than on the lift;
 *  · and a gesture something else consumed mid-flight (a scroll, a page turn of the
 *    pager's own, a pinch) never reaches the lift at all, because
 *    `waitForUpOrCancellation` answers null for a consumed change.
 *
 * @param key re-arms the handler when the thing the tap is measured against
 *  changes (a page's number, the screen's size). It is deliberately NOT the
 *  translation function: a handler keyed on a value that moves while the finger is
 *  down would cancel a tap in flight, which is its own "doesnt work sometimes".
 * @param at where a tap in this handler's own space is **on the screen**. A
 *  magnified sheet's frame is the document's ruler, not the screen's, and the
 *  column rides a horizontal scroll (see the call sites in [PdfScrollReader]).
 * @param screen the size the zones are measured against, when it is not this
 *  handler's own size (a column wider than the screen). Blank falls back to the
 *  handler's own measured size, which is right for every full-screen surface.
 */
private fun Modifier.readerZoneTaps(
    key: Any? = Unit,
    at: (Offset) -> Offset = { it },
    screen: () -> IntSize = { IntSize.Zero },
    onTap: (Offset, IntSize) -> Unit
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown()
        if (!ReaderLook.tapZones) return@awaitEachGesture
        // A press that outlives the long press is the sweep's (see the note above).
        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            waitForUpOrCancellation()
        } ?: return@awaitEachGesture
        if (ReaderTouch.multi || ReaderTouch.selecting) return@awaitEachGesture
        val area = screen().let { if (it.width > 0 && it.height > 0) it else size }
        val point = at(down.position)
        if (readerZoneActionAt(point, area) == ReaderZoneAction.OFF) return@awaitEachGesture
        onTap(point, area)
        // Consumed, so the surface's own tap detector drops the tap it was holding
        // back for a possible double tap: one tap, one answer.
        up.consume()
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
     * v426 — THE GESTURE'S OWN END, for a surface that only wants to COMMIT its
     * result once. A reflowable book's pages re-lay the whole text when the type
     * size changes, so its pinch magnifies the page LIVE (a cheap draw-phase
     * scale) and settles to a real type size when the fingers lift — which needs
     * to know when that is, and this is the one honest place to say it (see the
     * end of the gesture below). Null for every caller that has nothing to
     * settle.
     */
    onEnd: (() -> Unit)? = null,
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
        // v432 — a gesture begins with one finger; it only becomes a pinch when
        // a second one lands (see [ReaderTouch]).
        ReaderTouch.multi = false
        var last: Offset? = null
        // Whether THIS gesture has moved the page, and whether the page turned
        // the drag down at its edge. Both are per-gesture on purpose (v406).
        var ownsTheDrag = false
        var declined = false
        // ── v439 — HOW FAR THIS GESTURE HAS TRAVELLED, AND WHETHER THE PAGE
        // HAS CLAIMED IT YET (see the wear-in below). Both per-gesture.
        var travelled = 0f
        var claimedPan = false
        // v442 — the motion lock's own travel, for the same wear-in (see the
        // lock's branch below). Also per-gesture.
        var held = 0f
        // v448 — and that travel split by axis, because the lock has to tell a
        // SIDEWAYS claim (a page turn, a zone sweep) from a vertical move (see the
        // thaw below). Also per-gesture.
        var heldX = 0f
        var heldY = 0f
        // ── v457 — THE LOCK'S ONE ANSWER FOR THIS GESTURE ────────────────────
        //
        // `null` until the finger crosses the touch slop (nothing is consumed
        // before that, so a tap and a hold are never touched), then `true` when
        // this gesture belongs to the page's own vertical move and `false` when it
        // is the lock's. It is deliberately a plain local: it is decided and read
        // inside the gesture loop and must never cause a recomposition.
        var lockVerdict: Boolean? = null
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            // ── v439 — THE MOTION LOCK, AT THE ONE DOOR EVERY GESTURE USES ──
            //
            // The member: *"the motion lock pill which restrits that drag to move
            // and pinch to zoom"* (see [ReaderLook.motionLock]). This is the right
            // seam for it because EVERY gesture in the reader passes through this
            // handler, whatever surface it starts on — one guard, and no surface
            // can be forgotten.
            //
            // It consumes MOVEMENT and never the first down: a tap is a down and
            // an up with nothing in between, and `detectTapGestures` needs that
            // down unconsumed — consuming it would make the page untappable and
            // take the page turns and the chrome with it. A real drag or a second
            // finger is swallowed whole (consuming is what stops the scrolling
            // column or the pager underneath from taking it instead, which is how
            // a locked page stays exactly where the member left it).
            // ── v448 — HOW FAR THIS EVENT WENT, IN EACH AXIS ───────────────
            //
            // Read for EVERY event (not only while the lock is on), because the
            // lock's own thaw below needs to know which way the gesture is going
            // before it can decide whether it is the lock's to take.
            var stepX = 0f
            var stepY = 0f
            event.changes.forEach { change ->
                val step = change.position - change.previousPosition
                stepX += if (step.x < 0f) -step.x else step.x
                stepY += if (step.y < 0f) -step.y else step.y
            }
            held += stepX + stepY
            heldX += stepX
            heldY += stepY
            // ── v448 — AND A WIDE OR MAGNIFIED PAGE KEEPS ITS VERTICAL MOVE ─
            //
            // The member: *"when in wide or zoomed in with reading mode in pages,
            // allow the vertical move around scrolling or move around gesture in
            // motion lock on too"*. The lock freezes the ZOOM and the sideways drag
            // (the page turn, the zone sweep); it was never meant to nail a page
            // that is wider than the screen (Wide) or taller than its own frame (a
            // magnified page in the paged reader) and leave the member unable to
            // see the rest of it. So a gesture whose whole travel is VERTICAL falls
            // through to the page's own handling below — where a magnified page pans
            // and a page at rest hands the drag to the surface underneath — and only
            // the sideways claim and the pinch stay locked.
            // ── v457 — AND THE LOCK DECIDES ONCE, AT THE SLOP, AND HOLDS IT ──
            //
            // The member: *"fix the weird scrolling when zoom locked so the scroll
            // isnt like scrolling but it lets me drag to side too, weird
            // behavior"*. Both halves of that were one bug: the thaw was asked
            // afresh on EVERY event from the gesture's accumulated travel, so the
            // lock could change its mind in the middle of a drag.
            //
            //  · **"The scroll isnt like scrolling."** A drag whose first few
            //    pixels went a hair sideways was swallowed whole — and because
            //    consuming is what cancels the scrolling column's own slop wait,
            //    the page then could not scroll at all, even after the finger
            //    went straight down and the travel turned vertical. The gesture
            //    was simply dead.
            //  · **"It lets me drag to side too."** The mirror image: a drag that
            //    began vertically thawed the lock, and from then on the sideways
            //    move it went on to make was the pager's — so a page turn slipped
            //    through the lock that exists to freeze it.
            //
            // The axis is settled ONCE now, on the event the finger crosses the
            // touch slop, and the rest of the gesture obeys it to the lift: a
            // vertical drag belongs to the page's own scrolling from that event
            // onward, and anything else is the lock's. Same rules as v448 — only
            // a page with somewhere to move vertically can thaw at all, and a
            // pinch or a sweep in flight is never the page's.
            val verticalCanMove =
                ReaderLook.orientation == ReaderOrientation.LANDSCAPE ||
                    ReaderLook.pdfZoom > 1.02f
            if (ReaderLook.motionLock && lockVerdict == null &&
                pressed.size < 2 && !ReaderTouch.selecting &&
                held >= viewConfiguration.touchSlop
            ) {
                // Dominantly vertical travel = the page's scroll; dominantly
                // sideways = the lock's (a page turn is exactly what it freezes).
                lockVerdict = verticalCanMove && heldY > heldX
            }
            // A second finger is never the page's to take: two fingers are the
            // pinch, and the lock swallows them whole whatever the verdict said.
            if (pressed.size >= 2) {
                // A pinch's second finger must not read as a tap when it lifts
                // (see [ReaderTouch.multi]).
                ReaderTouch.multi = true
            }
            val lockYields = pressed.size < 2 &&
                (ReaderTouch.selecting || lockVerdict == true)
            if (ReaderLook.motionLock && !lockYields) {
                // ── v442 — AND A LOCKED PAGE STILL HEARS A TAP ──────────────
                //
                // The lock used to swallow every event in which any finger had
                // moved at all — and a finger that taps or holds still is never
                // perfectly still, so the first pixel of jitter consumed the move
                // and every tap detector threw the gesture away with it: on a
                // locked page the tools could not be brought back, a long press
                // could not open the mark dock and a sweep could not report a
                // word (member: *"the zoom lock is bad it also locks the touches
                // fix it"*).
                //
                // The page WEARS IN here exactly like the magnified page below:
                // nothing is consumed until the finger has actually travelled
                // the touch slop, so a tap and a hold are left alone and only a
                // real drag (or a second finger) is swallowed whole. Consuming
                // is still what keeps a locked page still — a drag the handler
                // merely ignored would be taken by the column or the pager under
                // it and the page would move anyway.
                //
                // A sweep in flight is the selection's, not the lock's: the
                // member asked for the pan to be frozen, not for words to stop
                // being selectable (see [ReaderTouch.selecting]) — it never
                // reaches here, and this line is what keeps that true whatever a
                // later event's pressed-count says.
                if (pressed.size >= 2 || held >= viewConfiguration.touchSlop) {
                    event.changes.forEach { it.consume() }
                }
                last = null
                continue
            }
            if (pressed.size >= 2) {
                // Two fingers are always the zoom's, from the first event: a
                // pinch that begins on a page is never a page turn.
                ReaderTouch.multi = true
                ownsTheDrag = true
                declined = false
                // v439 — two fingers are already a claim: there is no tap to
                // protect from a second finger (see the wear-in below).
                claimedPan = true
                travelled = 0f
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
                // ── v439 — AND A TAP'S OWN WOBBLE IS NOT A DRAG ──────────────
                //
                // The member, twice: *"when im zoomed in and i try to tap and
                // hold to select it doesnt work"* and *"when zoomed in the tools
                // doesnt appear when i tap once"*. Both were THIS line's fault,
                // and neither was a broken detector: a finger that taps or holds
                // still is not perfectly still, and the page panned (and
                // CONSUMED) those few pixels from its first event. Consuming a
                // single move is enough to cancel `detectTapGestures` — it drops a
                // tap the moment a change it is tracking is consumed, and it does
                // the same to a pending long press — so on a magnified page a tap
                // could not bring the tools back and a hold could never reach the
                // sweep. Worse, the sweep's own flag (v434's `ReaderTouch.selecting`,
                // which makes the pan stand down) is only set ONCE THE LONG PRESS
                // FIRES — and it never fired.
                //
                // So the page now WEARS IN like every other scrollable surface:
                // it takes nothing until the finger has actually travelled the
                // touch slop. A tap and a hold are left alone (their wobble is
                // under the slop), and a real drag crosses the slop on the same
                // event the pager's own slop wait would have used — where the
                // child wins, because a descendant's handler sees the event first
                // and its consumption is what cancels the parent's wait. That is
                // v403's guarantee (the page keeps the drag rather than handing it
                // to the pager mid-slide) with the tap left intact, and it is the
                // threshold AppKit and Android both use for the same reason.
                travelled += delta.getDistance()
                if (travelled >= viewConfiguration.touchSlop) claimedPan = true
                if (!declined && claimedPan) {
                    val taken = if (delta == Offset.Zero) Offset.Zero
                    else onZoom(1f, delta, position)
                    if (taken != Offset.Zero) {
                        ownsTheDrag = true
                    } else if (delta != Offset.Zero) {
                        declined = true
                    }
                }
                if (ownsTheDrag && claimedPan) pressed.forEach { it.consume() }
            } else {
                ownsTheDrag = false
                declined = false
                travelled = 0f
                claimedPan = false
                held = 0f
                heldX = 0f
                heldY = 0f
                last = null
            }
        } while (event.changes.any { it.pressed })
        // The fingers are all up: whoever asked to settle does it now, once.
        onEnd?.invoke()
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
 * zooms in in that place feels wrong"). The magnification IS the layout, so the
 * lazy column keeps the sheet the member was on where they left it and simply
 * grows it.
 *
 * The pan a two-finger drag carries is handed to the document's own two scrolls
 * — one down the column, one across the sheet — and a single finger is left
 * entirely to them, which is what [Offset.Zero] means: nothing taken, so the
 * column owns the gesture and the page still turns on a swipe.
 *
 * ── v430/v431 — AND THE ANCHOR IS READ FROM THE COLUMN'S OWN LAYOUT ──────
 *
 * The rule is the same one every zoom needs — the file grows about the point the
 * fingers grabbed, not about the top of anything — and the amount owed is the
 * finger's distance below the first visible sheet's top edge:
 *
 *   the fingers sit at  focus.y = anchor.offset + <that distance>
 *   so holding that point still needs  scroll' - scroll = distance·(ratio - 1)
 *
 * which is what [documentOffsetAt] answers. The padding and the 16dp gaps between
 * sheets do not scale with the zoom and therefore CANCEL in that difference, and
 * a SHEET'S INDEX must not appear in it at all: the list already preserves the
 * scroll position across the relayout, so an index-proportional term lands on top
 * of a move that has happened — the whole of the "it scrolls ten pages" report
 * (see the note on [documentOffsetAt]). The anchor is the first visible item, so
 * a pinch in the AIR between two sheets — the place the old per-sheet handler
 * answered nothing at all — is anchored exactly like one on the words.
 *
 * The horizontal axis needs none of this: there is one sheet across, so its left
 * edge never moves and the fingers' own x is the whole of the anchor (`focus.x`
 * is said in the column's space, which rides the sideways pan, so [across]'s own
 * value comes off it first).
 *
 * ── v431 — AND THE ANCHOR IS MEASURED FROM THE SHEET, NOT FROM THE FILE ──
 *
 * `documentOffsetAt` used to answer `index · sheetHeight + into` — how far the
 * finger is from the TOP OF THE FILE — and that number was the bug the member
 * reported ("it scroll so fast when i try zoom that like 10 pages it scrolls
 * by"). A lazy column PRESERVES `(firstVisibleItemIndex, scrollOffset)` across a
 * relayout, and every sheet grows by `ratio`, so the relayout ALREADY carries the
 * viewport forward by `index · sheetHeight · (ratio - 1)` in document space. The
 * compensation was therefore applied ON TOP of a move that had already happened:
 * it doubled the jump, and it was proportional to how deep into the book the
 * member was — which is exactly why ten pages went by.
 *
 * What the difference actually needs is the finger's distance BELOW THE ONE
 * LANDMARK THE RELAYOUT KEEPS STILL — the top edge of the FIRST VISIBLE item,
 * because the scroll position is anchored on it (`firstVisibleItemIndex` plus an
 * offset in pixels into that item, which is a number the relayout keeps). In
 * document terms:
 *
 *   before:  finger =  i0 · s      + o0 + y        (s = sheet height, o0 = how
 *                                                   much of i0 is off the top)
 *   after:   finger =  i0 · s · r  + (o0 + y) · r
 *   and the viewport top after the relayout is already  i0 · s · r + o0
 *   so the shift that is still owed is  (r - 1) · (o0 + y)
 *
 * The column's own padding and the 16dp gaps between sheets do not scale with
 * the zoom, so they cancel in that difference — which is why the SHEET'S INDEX
 * must not appear in it, and why the measure is taken in the anchor's frame and
 * not in the frame of whichever sheet happens to be under the hand.
 *
 * [viewportY] is a point in the viewport, which is the space an item's own
 * `offset` is said in (where the item starts in the viewport). A pinch in the AIR
 * — the 16dp gap between two sheets, the margin above the first — is measured
 * from the same landmark, so it is anchored exactly like one on the words.
 */
private fun documentOffsetAt(state: LazyListState, viewportY: Float): Float {
    val visible = state.layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return viewportY
    // The anchor: the lowest visible index, which is the item the scroll position
    // is held on (the list reports its visible items in index order, and this
    // asks for the lowest rather than trusting that order).
    val anchor = visible.minByOrNull { it.index } ?: return viewportY
    return (viewportY - anchor.offset).coerceAtLeast(0f)
}

private fun readerZoomDocument(
    zoom: Float,
    drag: Offset,
    focus: Offset,
    down: LazyListState,
    across: ScrollableState
): Offset {
    // ONE FINGER IS THE DOCUMENT'S SCROLL, not the zoom's: the pinch only
    // reports a factor of its own for two, and the column's own scrolling is left
    // entirely to the column (this returns Offset.Zero, so nothing is consumed).
    if (zoom == 1f) return Offset.Zero
    val owns = ReaderLook.pdfZoomPage == -1
    val was = if (owns) ReaderLook.pdfZoom else 1f
    val next = (was * zoom).coerceIn(1f, 4f)
    // ── THE FILE GROWS ABOUT THE FINGERS, NOT ABOUT ITS TOP ──
    //
    // Every sheet's laid-out size is PROPORTIONAL to the zoom, so a point `D`
    // below the top of the file is `D·z` from it once the file is magnified —
    // and since a pinch arrives as many small steps, the anchor holds for the
    // whole gesture rather than sliding a little on every frame.
    val ratio = if (was > 0f) next / was else 1f
    if (ratio != 1f) {
        down.dispatchRawDelta(documentOffsetAt(down, focus.y) * (ratio - 1f))
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
 *
 * v430 — with the same layout-read anchor the pinch carries ([documentOffsetAt]),
 * so a double tap on page 30 opens the page the member tapped instead of
 * somewhere near the top of the file, and one that lands between two sheets
 * opens on the sheet it is nearest.
 */
private fun readerDoubleTapDocument(
    at: Offset,
    down: LazyListState,
    across: ScrollableState,
    /** Where the file has to be put back — handed over, not done here (v432). */
    onAnchor: (ReaderZoomAsk) -> Unit
) {
    // A PINCH IS NOT A TAP (see [ReaderTouch]).
    if (ReaderTouch.multi) return
    val owns = ReaderLook.pdfZoomPage == -1
    val out = owns && ReaderLook.pdfZoom > 1.02f
    val was = if (owns) ReaderLook.pdfZoom else 1f
    val next = if (out) 1f else 2.2f
    val ratio = if (was > 0f) next / was else 1f
    // The SIDEWAYS half is safe to do here: a scroll state is a plain number and
    // the content's own width settles itself around it. The file's own scroll is
    // NOT (see [ReaderZoomAsk]), so it is measured now — against the layout as
    // it still stands — and applied by the caller once the new one exists.
    if (ratio != 1f) across.dispatchRawDelta(at.x * (ratio - 1f))
    zoomAskOf(down, at.y, ratio)?.let(onAnchor)
    ReaderLook.pdfZoom = next
    ReaderLook.pdfZoomPage = -1
    ReaderLook.pdfPanX = 0f
    ReaderLook.pdfPanY = 0f
}

/**
 * v432 — A DOUBLE TAP'S OWN CORRECTION, MADE ONCE THE FILE HAS SETTLED.
 *
 * The vertical half of a double tap's anchoring used to be a raw scroll delta,
 * computed from the layout as it stood a moment before the zoom was written — and
 * that is right for a PINCH, which arrives as many small steps, and wrong for a
 * single big one, which is exactly what a double tap is. The scroll position is
 * held as a pixel offset into the sheet it sits on, and that number is only a
 * pixel offset at the zoom it was measured at: at 2.2x a sheet is 2.2 screens
 * tall, so the offset can be LONGER than the whole sheet becomes at 1x. The list
 * then has to roll it back into the sheet above, and the member ends up a page
 * (or three) away from the word they tapped (their report: "the double tap zoom
 * and double tap again to unzoom is kinda buggy").
 *
 * So what crosses the zoom is the tapped point's SHARE of its own sheet, and the
 * scroll is corrected only once the column has been laid out at the new zoom —
 * the one moment those numbers mean what the correction needs them to. The
 * remaining error is nil: the sheet under the tap is put back with the same
 * point of itself under the same finger, at any zoom, in either direction.
 */
private class ReaderZoomAsk(
    /** The sheet the tap landed on. */
    val index: Int,
    /** How far into that sheet the finger was, as a SHARE of it (0..1). */
    val fraction: Float,
    /** The sheet's laid-out height BEFORE the zoom, so the correction can tell
     *  the relayout has landed instead of measuring the layout it came from. */
    val wasSize: Int,
    /** Where the tap was, in the viewport's own y. */
    val viewportY: Float,
    /** The scale change asked for, for the fallback if the sheet never reports. */
    val ratio: Float
)

/**
 * The sheet a tap at [viewportY] landed on, and how far into it it went.
 *
 * A tap in the AIR between two sheets (the 16dp gap, the margin above the first)
 * belongs to whichever sheet it is nearest — the same reading the tap zones and
 * the pinch anchor already use, so a double tap in the gap does something
 * sensible rather than nothing.
 */
private fun zoomAskOf(state: LazyListState, viewportY: Float, ratio: Float): ReaderZoomAsk? {
    val visible = state.layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return null
    val under = visible.firstOrNull { viewportY >= it.offset && viewportY < it.offset + it.size }
        ?: visible.minByOrNull { abs(it.offset + it.size / 2f - viewportY) }
        ?: return null
    val fraction = ((viewportY - under.offset) / under.size.toFloat()).coerceIn(0f, 1f)
    return ReaderZoomAsk(under.index, fraction, under.size, viewportY, ratio)
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
    // A PINCH IS NOT A TAP (see [ReaderTouch]).
    if (ReaderTouch.multi) return
    // v439 — and a LOCKED page does not zoom at all: a double tap that resized a
    // page the member has just told the reader to hold still would be the reader
    // arguing with them (see [ReaderLook.motionLock]). The tap is not swallowed —
    // it is simply not a zoom, so everything else a double tap might mean on this
    // surface is untouched.
    if (ReaderLook.motionLock) return
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

// ── v442 — THE READER'S OWN CLOCK ───────────────────────────────────────────
//
// One minute count, one label and one picker, shared by both surfaces the night
// dim is set on (the appearance sheet and reading settings), so the two can never
// disagree about what "at sunset" means. The dim itself lives in the reader's own
// body, ticked half a minute at a time (see the night-dim block there).

/** How many minutes a day holds. The dim's window is stored in these units. */
internal const val READER_MINUTES_IN_DAY = 24 * 60

/** The reader's clock, in minutes since midnight, from the phone's own time. */
internal fun readerMinuteOfDay(): Int =
    // `java.time` and not `Calendar`: minSdk 26 ships it, and it cannot be misread.
    java.time.LocalTime.now().let { now -> now.hour * 60 + now.minute }

/** "20:00" — a time as the reader wears one. */
internal fun readerClockLabel(minuteOfDay: Int): String {
    val safe = minuteOfDay.coerceIn(0, READER_MINUTES_IN_DAY - 1)
    return "%02d:%02d".format(Locale.US, safe / 60, safe % 60)
}

/**
 * v442 — ONE END OF THE DIM'S WINDOW, AS A ROW.
 *
 * The reader's own furniture: a capsule that names the end and shows the time, and
 * whose tap opens the clock. Used by the appearance sheet and by reading settings,
 * which is why it is `internal` and lives here rather than in either of them.
 */
@Composable
internal fun ReaderClockRow(
    label: String,
    minuteOfDay: Int,
    palette: ReaderPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = palette.ink.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CurioIcon(CurioIcons.Schedule, null, tint = palette.accent, size = 17.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = palette.ink.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
            Text(
                readerClockLabel(minuteOfDay),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = palette.accent
            )
        }
    }
}

/**
 * v442 — AND THE CLOCK ITSELF, ON THE READER'S OWN PAPER.
 *
 * A real Material dial rather than a pair of steppers, because the member asked
 * for a TIME and a dial is how a time is set — wrapped in the reader's paper and
 * type so it belongs to the page it dims (see [ReaderClockRow]).
 */
@Composable
internal fun ReaderClockDialog(
    palette: ReaderPalette,
    minuteOfDay: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit
) {
    val picker = rememberTimePickerState(
        initialHour = (minuteOfDay / 60).coerceIn(0, 23),
        initialMinute = (minuteOfDay % 60).coerceIn(0, 59),
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.paper,
        title = {
            Text(
                "Set the time",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = readerTypeFamily(ReaderLook.typeFace)
                ),
                color = palette.ink
            )
        },
        text = { TimePicker(state = picker) },
        confirmButton = {
            TextButton(
                onClick = {
                    onPick(picker.hour * 60 + picker.minute)
                    onDismiss()
                }
            ) {
                Text("Set", color = palette.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.ink.copy(alpha = 0.7f))
            }
        }
    )
}

// @Composable because the default ink asks [isCurioDarkTheme] what the app is
// wearing — one reader, two themes.
@Composable
internal fun readerPalette(key: String): ReaderPalette = when (key) {
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
    // ── v431 — THE TUNED PAPERS (the `+` row) ─────────────────────
    "gray" -> ReaderPalette(
        paper = Color(0xFFECECEC),
        ink = Color(0xFF2B2B2B),
        accent = Color(0xFF6B6B6B),
        surface = Color(0xFFF4F4F4),
        inkKey = "gray"
    )
    "mint" -> ReaderPalette(
        paper = Color(0xFFEAF3EC),
        ink = Color(0xFF1A201A),
        accent = Color(0xFF3F6B4C),
        surface = Color(0xFFF1F8F3),
        inkKey = "mint"
    )
    "rose" -> ReaderPalette(
        paper = Color(0xFFF7EBEC),
        ink = Color(0xFF2A2022),
        accent = Color(0xFF8E5560),
        surface = Color(0xFFFBF3F4),
        inkKey = "rose"
    )
    "amber" -> ReaderPalette(
        paper = Color(0xFFFAEED6),
        ink = Color(0xFF362A18),
        accent = Color(0xFF8A6234),
        surface = Color(0xFFFDF5E6),
        inkKey = "amber"
    )
    // A DARK page at the end of the extras row, so the `+` row has a night for
    // the readers who want the ink to actually invert ("Slate").
    "slate" -> ReaderPalette(
        paper = Color(0xFF1A1E24),
        ink = Color(0xFFC6D2DC),
        accent = Color(0xFF7FA3C4),
        surface = Color(0xFF232931),
        inkKey = "slate"
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

/**
 * v463 — ONE SENTENCE OF THE BOOK, AND WHERE IT IS.
 *
 * The voice used to be handed four paragraphs at a time, which is a fine unit for an
 * ENGINE and a useless one for a READER: all the screen could say about where the voice
 * had got to was "somewhere in this block", so there was nothing to highlight and
 * nothing to skip to. A sentence is the unit a read-along mark needs (a run of the
 * block's own text) and the unit a skip needs (the next thing worth hearing), so the
 * reader is driven in sentences now and a block is merely the row a sentence is drawn in.
 */
private data class ReaderSentence(
    /** The index of the block this sentence is a run of. */
    val block: Int,
    /** The sentence's own bounds INSIDE that block's text. */
    val from: Int,
    val to: Int,
    /** 1-based chapter, so a chapter skip is a change of this number. */
    val section: Int,
    val text: String
)

/**
 * v463 — WHICH RUN OF WHICH BLOCK A VOICE IS ON (see [ReaderSentence]).
 *
 * Handed to the paragraph renderer as offsets rather than as the sentence itself: a
 * block knows its own text and nothing about the book, so the only thing it may be told
 * is "this range of YOUR words".
 */
private data class ReaderSpoken(val block: Int, val from: Int, val to: Int)

/**
 * v463 — THE BOOK, CUT INTO SENTENCES.
 *
 * Built once per file and kept: a book is opened once and read for hours, so this is one
 * walk over the text rather than a scan per frame — and the point of doing it up front is
 * that the driver, the highlight and the skip controls all read the SAME list, so the
 * sentence being spoken and the sentence being lit cannot disagree.
 *
 * A heading is a sentence of its own whatever its punctuation (it is a line the book set
 * apart, and reading it into the paragraph under it would be wrong), and a block that is
 * blank is skipped entirely — a page-anchor block exists only to be a scroll target and
 * has no words to say.
 */
private fun speechSentences(blocks: List<ReaderBlock>): List<ReaderSentence> {
    val out = ArrayList<ReaderSentence>(blocks.size * 4)
    blocks.forEachIndexed { index, block ->
        if (block.text.isBlank()) return@forEachIndexed
        val ranges = if (block.isHeading) {
            listOf(block.text.indices)
        } else {
            ReaderSentenceScanner.split(block.text)
        }
        ranges.forEach { range ->
            if (range.isEmpty()) return@forEach
            val raw = block.text.substring(range.first, range.last + 1)
            val lead = raw.indexOfFirst { !it.isWhitespace() }
            if (lead < 0) return@forEach
            val trail = raw.indexOfLast { !it.isWhitespace() }
            out.add(
                ReaderSentence(
                    block = index,
                    from = range.first + lead,
                    to = range.first + trail,
                    section = block.section,
                    text = raw.substring(lead, trail + 1)
                )
            )
        }
    }
    return out
}

/**
 * v463 — WHERE A SENTENCE ENDS.
 *
 * A full stop is not a sentence end by itself, and the ways it lies are exactly what this
 * has to survive: an ABBREVIATION ("Mr. Knightley"), an INITIAL ("J. R. R."), and a
 * DECIMAL or a dotted acronym. A sentence also never continues in lower case, which is
 * the one check that gets the overwhelming majority of prose right.
 *
 * It is deliberately a heuristic and deliberately a FORGIVING one: a mis-split costs the
 * member one odd breath, whereas an over-eager splitter would cut every "No. 5" in half
 * — and a voice that keeps stopping mid-clause is worse than one that occasionally reads
 * a sentence long.
 *
 * The abbreviation list is therefore kept SHORT and to words that are not English words
 * in their own right. "no", "sat", "mar", "sun" and "rev" were all in an earlier
 * draft and each one silently merged two real sentences ("The answer is no. She left"
 * read as one line), which is precisely the failure this is meant to avoid.
 *
 * **IT IS CALLED A SCANNER AND NOT A SPLITTER BECAUSE THE NAME WAS TAKEN.** This file
 * already had a `private val ReaderSentenceSplit = Regex(...)` (the dictionary's lookup
 * context line), and a second top-level declaration of that name is a compile error —
 * which it duly was. Before adding a top-level name here, `grep` the NAME, not the shape
 * of the declaration you are about to write (`grep -rw Name`): searching for
 * "object ReaderSentenceSplit" finds nothing while the name is very much in use.
 */
private object ReaderSentenceScanner {

    /** Words whose full stop belongs to the word. None is also an English word. */
    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "vs", "etc",
        "eg", "ie", "fig", "vol", "ch", "pp", "inc", "ltd", "corp", "dept",
        "univ", "capt", "lt", "sgt", "sept", "approx", "eds"
    )

    /** A close after the stop belongs to the sentence the stop closes. */
    private const val CLOSERS = "\"'\u201D\u2019)]}"

    fun split(text: String): List<IntRange> {
        if (text.isBlank()) return emptyList()
        val out = ArrayList<IntRange>(4)
        val n = text.length
        var start = 0
        var i = 0
        while (i < n) {
            if (!isTerminator(text[i])) {
                i += 1
                continue
            }
            var run = i + 1
            while (run < n && isTerminator(text[run])) run += 1
            var end = run
            while (end < n && text[end] in CLOSERS) end += 1
            if (!endsSentence(text, i, end)) {
                i = run
                continue
            }
            out.add(start..(end - 1))
            start = end
            i = end
        }
        if (start < n) out.add(start..(n - 1))
        return out
    }

    private fun isTerminator(c: Char): Boolean =
        c == '.' || c == '!' || c == '?' || c == '\u2026'

    private fun endsSentence(text: String, stop: Int, end: Int): Boolean {
        val n = text.length
        var p = end
        while (p < n && text[p].isWhitespace()) p += 1
        // The paragraph simply ran out: that is an end whatever it stopped on.
        if (p >= n) return true
        // A sentence never continues in lower case.
        if (text[p].isLowerCase()) return false
        if (text[stop] == '.') {
            // "3.14" — a full stop between two digits is a decimal point.
            if (stop > 0 && text[stop - 1].isDigit() &&
                stop + 1 < n && text[stop + 1].isDigit()
            ) {
                return false
            }
            val word = wordBefore(text, stop)
            // A single letter is an initial ("J. R. R. Tolkien").
            if (word.length == 1) return false
            if (word.lowercase() in ABBREVIATIONS) return false
        }
        return true
    }

    /** The word a stop is attached to, read backwards from it. */
    private fun wordBefore(text: String, stop: Int): String {
        var at = stop - 1
        if (at >= 0 && isTerminator(text[at])) at -= 1
        val end = at
        while (at >= 0 && (text[at].isLetter() || text[at] == '\'' || text[at] == '\u2019')) {
            at -= 1
        }
        if (end <= at) return ""
        return text.substring(at + 1, end + 1)
    }
}

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

/**
 * v431 — THE READER'S SHEETS, ONE PER DOOR ON THE FOOT PILL.
 *
 * `INK`/`PLACES`/`SEARCH` is gone with the old chrome: the ink is half of the
 * Appearance sheet (the pill's first button), the places split back into the
 * three lists the member asked for, the ⋯ menu is a door of its own, the page
 * scrubber is the middle button, and the Dictionary is the in-app Wiktionary
 * lookup a selected word opens.
 */
private enum class ReaderSheet {
    APPEARANCE,
    CONTENTS,
    BOOKMARKS,
    NOTES,
    HIGHLIGHTS,
    MENU,
    DICTIONARY
}

/**
 * v431 — WHICH OF THE BOOK'S PLACES A SHEET IS SHOWING (see [ReaderPlacesSheet]).
 *
 * `ALL` is the old merged sheet — kept because it is the one shape that proves
 * the four doors are views of ONE layout, and because a future surface (the
 * book's own page) can still ask for everything at once.
 */
private enum class ReaderPlacesMode { ALL, CONTENTS, BOOKMARKS, NOTES, HIGHLIGHTS }

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
 * v431 — WHERE THE READER IS, AND THE THREE WAYS TO MOVE.
 *
 * The v389 page bar named a page and offered two arrows; the member's own
 * request made it a SCRUBBER as well ("the pages in the middle of the doc pill
 * when tapping the pages it opens the page scrubber"). One description now
 * carries both: what the foot pill wears ([short]), what the sheet calls itself
 * ([label]), where the reader is ([at] of [total]), and the three ways to move —
 * back, on, and straight to a place ([onScrub]). It is a class rather than four
 * parameters because it is built in ONE place (the reader body, which is the only
 * code that knows whether this book has pages, sections or neither) and read in
 * two (the pill and the sheet).
 */
private class ReaderScrubber(
    /** The compact count the foot pill wears, e.g. "12 / 300"; blank for no pages. */
    val short: String,
    /** The long name of the place, for the scrubber sheet's own header. */
    val label: String,
    val at: Int,
    val total: Int,
    val onPrev: () -> Unit,
    val onNext: () -> Unit,
    val onScrub: (Int) -> Unit
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
    note: String = "",
    /**
     * v457 — WHERE THE WORDS ARE (see [ReaderMarkEntity]). The offsets the
     * passage was swept from, or -1 when the caller has none (a whole
     * paragraph, a chapter, a note's own place).
     */
    from: Int = -1,
    to: Int = -1
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
                        //
                        // v457 — and TWO RUNS OF THE SAME WORDS ARE TWO MARKS.
                        // The same phrase twice in a block used to collapse onto
                        // the first run; with the offsets stored, the run is part
                        // of a highlight's identity. A row from before the
                        // columns (startIndex -1) still matches on its words, so
                        // the member's first re-highlight upgrades it in place
                        // instead of stacking a second wash on the same words.
                        (kind != ReaderMarkKind.HIGHLIGHT ||
                            (it.text == text && (from < 0 || it.startIndex < 0 ||
                                it.startIndex == from)))
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
                    chapter = paragraph.section,
                    startIndex = from,
                    endIndex = to
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

/**
 * Renders ONE page of a PDF at screen resolution.
 *
 * [lowPower] is the member's own choice (see [ReaderLook.lowPower]) and it is
 * about the two things that actually cost a phone heat while reading: the number
 * of PIXELS a page is drawn into, and whether those pixels carry an alpha
 * channel that nothing on this surface ever composites against.
 *
 *  · **full detail** — ARGB_8888, up to [MAX_PDF_RENDER_SCALE] times the
 *    screen's width.
 *  · **low power** — RGB_565 (half the bytes per pixel to write, hold and
 *    upload, which is the whole of the difference on a page of black type on
 *    white) and no upscale past [LOW_POWER_PDF_RENDER_SCALE]. Past that the page
 *    is being drawn into pixels the screen cannot show.
 *
 * The renderer itself does the same work either way: a page is still
 * `RENDER_MODE_FOR_DISPLAY`, and the text and the marks are untouched.
 */
private fun renderPdfPage(
    context: android.content.Context,
    value: String,
    index: Int,
    lowPower: Boolean = false
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
            .coerceIn(1f, if (lowPower) LOW_POWER_PDF_RENDER_SCALE else MAX_PDF_RENDER_SCALE)
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(
            (page.width * scale).toInt().coerceAtLeast(1),
            height,
            // ── ARGB_8888, ALWAYS, AND THIS IS NOT A STYLE CHOICE (v440) ───
            //
            // v439 tried to save the bitmap's alpha channel in low power by
            // rendering into RGB_565. **`PdfRenderer.Page.render` accepts nothing
            // but ARGB_8888** — any other config throws — so "Cooler" stopped
            // rendering pages at all (member: *"pdf isnt loading now in cooler"*).
            // The saving that IS real is the SCALE below (the upscale cap) and
            // `beyondViewportPageCount`; the config must stay what the framework
            // demands. Do not make this conditional again.
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
 * v439 — how far a page may be scaled up while low power reading is on (see
 * [ReaderLook.lowPower]). The screen's own width is 1×; half again is headroom
 * for the moment a page is magnified a little, and past it the extra pixels are
 * drawn and uploaded for nobody.
 */
private const val LOW_POWER_PDF_RENDER_SCALE = 1.5f

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

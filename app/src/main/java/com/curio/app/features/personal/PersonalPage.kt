package com.curio.app.features.personal

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.curio.app.data.AppPreferences
import com.curio.app.data.PAGE_KIND_JOURNAL
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newNoteId
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v389 — THE WRITING PAGE CORE: one editor, three pages.
 *
 * A journal day, a note on a topic and a to-do list are three different pages
 * with three different heads — a date and a mood, a topic card, a progress line
 * — but they are the SAME page underneath: a block canvas, a tool dock riding
 * the keyboard above it, and an auto-save that never lets an app switch lose
 * the words. This holds that shared half, so a page is its head plus a body
 * (and the 200 lines of load / debounce / flush / photo plumbing exist once,
 * which is what makes the three pages behave identically by construction).
 *
 * What the core owns: the entry id, the document, the editor state, the
 * debounced save, the ON_STOP flush, the photo picker, the read/edit crossfade
 * and the dock.
 *
 * What the page owns (via [header], [aboveCanvas] and [readView]): its own top
 * bar, the fields above the canvas, and how the saved page READS.
 *
 * [meta] is the page's half of the stored row. It is read as a lambda so the
 * core can key its debounce on it (a changed title, mood or topic saves on the
 * same clock as a typed word) without owning any of those fields.
 */
internal data class PersonalPageMeta(
    val title: String = "",
    val mood: String = "",
    val dateMillis: Long = 0L,
    val kind: String = PAGE_KIND_JOURNAL,
    val topicId: String = "",
    val topicName: String = "",
    val categoryId: String = "",
    /**
     * v428 — the page's OWN colour ([PersonalNoteEntity.accentArgb]), or
     * [JOURNAL_ACCENT_THEME]. It rides the meta because that IS this page's
     * half of the stored row: the writer below rebuilds the whole entity from
     * it, so a colour left out here would be a colour erased on the next
     * keystroke — and because the debounce reads the meta as a value, choosing
     * a colour saves on the same clock a typed word does.
     */
    val accentArgb: Int = JOURNAL_ACCENT_THEME,
    /**
     * v429 — WHETHER THE PAGE ITSELF WEARS THAT COLOUR ([PersonalNoteEntity
     * .pagePainted]). It rides the meta for the same reason the colour does: the
     * writer rebuilds the whole entity from the meta, so a flag left out here
     * would be a flag ERASED on the next keystroke, and the debounce reads the
     * meta as a value — flipping the switch saves on the same clock a word does.
     */
    val pagePainted: Boolean = false
)

/**
 * Renders the page. NOTHING here is a "save" button: the entry writes itself to
 * its own store (debounced while typing, again the moment the app leaves the
 * foreground, and once more on the way out), which is what "don't lose it mid
 * app switch" requires — an app switch never gets the chance to ask.
 *
 * [header] gets the mode and its setter: the read/write switch belongs to each
 * page's own top bar (the journal's eye/pen, the note's pen), but the MODE
 * itself is the core's — it decides whether the dock is out and whether the
 * canvas takes typing.
 */
@Composable
internal fun PersonalWritingPage(
    entryIdArg: String,
    photos: PersonalPhotoOverlayState,
    meta: () -> PersonalPageMeta,
    header: @Composable (
        editing: Boolean,
        saving: Boolean,
        onEditing: (Boolean) -> Unit,
        /** The GUARDED way out — the page's own back button must use this, so
         *  a live recording can be asked about (see PersonalVoice). */
        onBack: () -> Unit
    ) -> Unit,
    readView: @Composable (doc: PersonalDoc) -> Unit,
    /** The screen's way off this page, wrapped by the core's leave guard. */
    onExit: () -> Unit = {},
    /** How to come BACK here (the keep-recording pill's tap target). */
    voiceRoute: (String) -> String = { CurioRoutes.journalEditor(it) },
    onLoaded: (PersonalNoteEntity) -> Unit = {},
    /**
     * WHERE THE JOURNAL'S OWN COLOUR COMES FROM, and where it goes.
     *
     * A page's colour is only offered when the page WANTS it: [onJournalAccent]
     * is null for the note-on-a-topic page, the chapter review and the book
     * review, so the door never appears on a surface that has nowhere to put
     * the answer. The journal page passes its own setter, and the colour itself
     * arrives through the meta (see [PersonalPageMeta.accentArgb]), which is
     * what makes the write the page's normal debounced save.
     */
    journalAccent: Int = JOURNAL_ACCENT_THEME,
    onJournalAccent: ((Int) -> Unit)? = null,
    /** The document as it changes — the to-do page counts its own rows from it
     *  (see TodoScreen), and nothing else has to reach into the editor. */
    onDoc: (PersonalDoc) -> Unit = {},
    /**
     * A CHECKLIST page (the to-do list): it opens with the pen already down
     * (a list is used, not read), its first empty line armed as a checkbox row,
     * and Enter at the end of a row starting the next row — see
     * [PersonalEditorState.keepsChecklistRows].
     */
    checklistFirst: Boolean = false,
    showJournalTools: Boolean = true,
    aboveCanvas: @Composable () -> Unit = {},
    /**
     * v389 — A HEAD THAT DOES NOT SCROLL.
     *
     * [aboveCanvas] rides INSIDE the writing column, so it travels up out of
     * view as soon as the page is scrolled or the keyboard lifts the caret —
     * which is wrong for anything that is the page's own subject rather than a
     * field above it (user report, about the topic note: "the choose a topic
     * area gets hidden as it's not on top the page. it's little scrolled down so
     * fix it"). This slot sits UNDER the page's top bar and outside the scroll,
     * so the subject of the page stays on the page.
     *
     * v389e — IT IS TOLD THE MODE. A pinned head is a CARD, and a card of the
     * page's subject standing over the READING is the subject said twice: the
     * note page's read view already opens with the topic's own name and its
     * lane (user request: "remove the card of topic for note in eye view"). So
     * the slot receives `editing` and a page that only wants its head while
     * writing can say so (see TopicNoteScreen).
     */
    pinnedHead: @Composable (editing: Boolean) -> Unit = {},
    /**
     * v389 — HOW FAR THE PAGE HAS BEEN SCROLLED, for a head that ROLLS UP.
     *
     * The writing column's own scroll lives here, so a page whose top bar wants
     * to say something only once the writing has moved under it (the journal's
     * title — see JournalEditorScreen) has to be told. It is handed the OFFSET
     * rather than the state on purpose: the bar's business is how far down the
     * page is, not how to move it.
     */
    onScroll: ((Int) -> Unit)? = null
) {
    val isNew = entryIdArg == CurioRoutes.PERSONAL_NEW
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // THE PAGE'S OWN COLOUR, resolved once for this page and handed to the
    // surfaces that need it (see [JournalPagePaint]). Only a page that HAS a
    // colour of its own resolves to one — which is exactly the journal, and why
    // the note-on-a-topic, chapter-review and book-review pages keep the theme's
    // parchment. v443 — the paper is no longer one of the things it paints: the
    // colour reaches the page's own controls ([journalPaperRaised]) and its doors,
    // and the words are never read against a colour off a member's wheel.
    val pagePaint = remember(journalAccent) {
        JournalPagePaint(argb = journalAccent)
    }

    // SAVED, not merely remembered: a brand-new page's id is minted on the
    // first composition, and navigating away (the topic page, a settings trip)
    // DISPOSES that composition — so coming back used to mint a SECOND id, save
    // a second row and lose everything the first one had. The id is a fact about
    // the page, so it is saved with the page's own state (user report, about a
    // topic note: "when i tap the look the topic from the header and i press
    // back the previous topic gets saved and it asks me again to choose a new").
    var entryId by rememberSaveable(entryIdArg) {
        mutableStateOf(if (isNew) newNoteId() else entryIdArg)
    }
    var doc by remember { mutableStateOf(PersonalDoc(emptyList())) }
    // READ FIRST, write on request: a saved page OPENS as the page it is and
    // the pen switches the tools on. A brand new page has nothing to read, so
    // it opens with the pen already down — and so does a checklist.
    var editing by remember(entryId) { mutableStateOf(isNew || checklistFirst) }
    var loaded by remember { mutableStateOf(isNew) }
    /**
     * v389d — DID THIS PAGE HAVE ANYTHING ON IT when it was opened? A page that
     * DID can be saved EMPTY (the member deleted their words — see [shouldWrite]);
     * one that never did is left alone rather than written as a blank entry.
     */
    var storedEmpty by remember(entryId) { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var createdAt by remember { mutableLongStateOf(0L) }

    // The editor state is rebuilt ONCE per entry (never per keystroke — that
    // would drop the caret). A checklist page owns its rows' style: Enter at
    // the end of a row makes the next one a row too.
    val editor = remember(entryId) {
        PersonalEditorState(doc).also { it.keepsChecklistRows = checklistFirst }
    }

    // ── Voice notes (v389) ─────────────────────────────────────────────
    // The mic is a FLOATING button of the page's own, and while a note is being
    // made the dock steps aside and the recording capsule takes its place: a
    // recording is not a moment for bold. The session itself lives in
    // [PersonalVoiceRecording] so leaving the page never decides for the member.
    val liveVoice = PersonalVoiceRecording.session?.takeIf { it.noteId == entryId }
    var recordFailed by remember { mutableStateOf(false) }
    var leavePrompt by remember { mutableStateOf(false) }
    // The microphone was refused (or refused for good) — the page SAYS so and
    // offers Android's own app page, which is the only way back from "Don't
    // allow". A dead-looking button was the reported bug.
    var micDenied by remember { mutableStateOf(false) }

    fun startVoice() {
        val started = PersonalVoiceRecording.start(context, entryId, voiceRoute(entryId))
        if (started == null) recordFailed = true
    }

    val askToRecord = rememberRecordPermission(
        onGranted = { startVoice() },
        onDenied = { micDenied = true }
    )

    // Only the note whose page is OPEN hides the pill at the app's root.
    DisposableEffect(entryId) {
        PersonalVoiceRecording.setOnScreen(entryId)
        onDispose { PersonalVoiceRecording.setOnScreen(null) }
    }

    /** The one way out of the page: a live recording gets asked about first. */
    fun leave() {
        val live = PersonalVoiceRecording.session
        if (live != null && live.noteId == entryId) leavePrompt = true else onExit()
    }

    BackHandler(enabled = liveVoice != null) { leave() }
    SideEffect { editor.onDocChanged = { updated -> doc = updated } }

    // ── Load the page ──────────────────────────────────────────────────
    LaunchedEffect(entryId) {
        val existing = withContext(Dispatchers.IO) { PersonalRepositoryHolder.repo.note(entryId) }
        if (existing != null) {
            val decoded = existing.doc
            doc = decoded
            editor.replace(decoded)
            createdAt = existing.createdAtMillis
            storedEmpty = decoded.isEmpty && existing.title.isBlank()
            onLoaded(existing)
            // v389 — WHICH SIDE AN EXISTING PAGE OPENS ON (user decision):
            // opening a journal from Home, from the list or from the Cabinet
            // lands on the EYE — a saved page is a page to READ, and the pen is
            // one tap away — while a page with nothing on it opens with the pen
            // down, because there is nothing to read yet and a caret waiting is
            // the whole point of the door that led here. `editing` already
            // starts false for an existing entry (see its declaration), so this
            // only lifts the EMPTY pages back into writing.
            if (decoded.isEmpty && !checklistFirst) editing = true
        }
        loaded = true
    }

    LaunchedEffect(loaded, editing, checklistFirst) {
        if (!loaded || !editing) return@LaunchedEffect
        if (checklistFirst) {
            editor.armCheckboxOnEmptyLine()
            editor.requestFocusOnEmptyLine()
        }
    }

    /**
     * v390 — THE EYE PUTS THE KEYBOARD AWAY.
     *
     * The whole page is laid out under the keyboard's inset, so a keyboard left
     * standing over the READ side both covers the page being read and holds the
     * box short, which is half of the shift the member reported while switching
     * ("the journal etc shifts when switching between edit and view due to the
     * keyboard" — the other half was the keyboard being raised by the switch
     * itself, which no longer happens: see the journal's own `aboveCanvas`). The
     * reading side is a page, not a field, so nothing there wants an inset.
     */
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(editing) {
        if (!editing) keyboard?.hide()
    }

    // The page's own view of the document (the to-do page's progress line).
    LaunchedEffect(doc) { onDoc(doc) }

    // ── The Undo pill (v389) ───────────────────────────────────────────
    // A swiped-away to-do row is held by the editor, and the pill that puts it
    // back goes away by itself — long enough to notice and reach, short enough
    // that it can never become part of the page.
    val removedRow = editor.lastRemovedRow
    LaunchedEffect(removedRow) {
        if (removedRow != null) {
            delay(5000)
            editor.clearRemovedRow()
        }
    }

    // ── Auto-save ──────────────────────────────────────────────────────
    // Everything that can change is held in `rememberUpdatedState` so the
    // writers below (a debounce inside composition, and an app-switch flush
    // outside it) always read the latest values instead of the ones they were
    // created with.
    val liveId = rememberUpdatedState(entryId)
    val liveDoc = rememberUpdatedState(doc)
    val liveCreatedAt = rememberUpdatedState(createdAt)
    val liveMeta = rememberUpdatedState(meta)

    /** The ONE writer: every save path below hands the same row to the store. */
    suspend fun writePage(body: PersonalDoc, page: PersonalPageMeta): PersonalNoteEntity =
        withContext(Dispatchers.IO + NonCancellable) {
            PersonalRepositoryHolder.repo.saveNote(
                PersonalNoteEntity(
                    id = liveId.value,
                    bookId = null,
                    chapterIndex = null,
                    title = page.title.trim(),
                    bodyJson = com.curio.app.data.PersonalDocCodec.encode(body),
                    preview = "",
                    dateMillis = page.dateMillis,
                    mood = page.mood,
                    createdAtMillis = liveCreatedAt.value,
                    updatedAtMillis = 0L,
                    kind = page.kind,
                    topicId = page.topicId,
                    topicName = page.topicName,
                    categoryId = page.categoryId,
                    accentArgb = page.accentArgb,
                    pagePainted = page.pagePainted
                )
            )
        }

    /**
     * v389d — AN EMPTIED PAGE IS AN EDIT, NOT AN ABSENCE.
     *
     * Every write path here refused an empty body, which is right for a page
     * nobody has written on yet — and wrong for one the member has just cleared:
     * deleting all the words and leaving skipped the save entirely, so the next
     * visit restored exactly what they had deleted (user report: "in book review
     * or any page i can't leave the page blank after saving once, and each time
     * i delete all text it keeps coming back when i exit"). So a write is
     * allowed when there is something to write OR when the page HAD something
     * before — [storedEmpty] is that fact, and it follows every save.
     */
    fun shouldWrite(body: PersonalDoc): Boolean =
        !body.isEmpty || liveMeta.value().title.isNotBlank() || !storedEmpty

    fun saveNow() {
        val body = liveDoc.value
        val page = liveMeta.value()
        if (!shouldWrite(body)) return
        saving = true
        scope.launch {
            val saved = writePage(body, page)
            entryId = saved.id
            createdAt = saved.createdAtMillis
            storedEmpty = body.isEmpty && page.title.isBlank()
            saving = false
        }
    }

    // Debounced: 700ms after the writer stops moving. `pageNow` is READ during
    // composition, so a changed title / mood / topic restarts the same clock a
    // typed word does.
    val pageNow = meta()
    LaunchedEffect(loaded, doc, pageNow) {
        if (!loaded) return@LaunchedEffect
        if (!shouldWrite(doc)) return@LaunchedEffect
        delay(700)
        saveNow()
    }

    // An app switch must not be able to lose the page.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) saveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // …and neither must LEAVING it. The debounce above lives inside the
    // composition, so a back gesture cancels it mid-wait: a page that was typed
    // into a moment before the member left — or a voice note they chose to KEEP
    // on the way out — used to lose exactly that last change (user question:
    // "is it persistent too like it saves when i back by accident?"). This
    // flush runs on its OWN scope, because by the time onDispose runs the
    // composition's scope is already cancelled and a `scope.launch` there would
    // never write anything.
    DisposableEffect(entryId) {
        val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        onDispose {
            val body = liveDoc.value
            val page = liveMeta.value()
            if (!shouldWrite(body)) return@onDispose
            flushScope.launch { writePage(body, page) }
        }
    }

    // ── Photos ─────────────────────────────────────────────────────────
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            // Persisted permission: a page's photo has to be readable in a later
            // session, not just until this screen closes.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            editor.insertPhoto(uri.toString())
        }
    }

    // The writing column's scroll — the page's head reads it (see onScroll).
    val pageScroll = rememberScrollState()
    LaunchedEffect(onScroll, pageScroll) {
        val report = onScroll ?: return@LaunchedEffect
        snapshotFlow { pageScroll.value }.collect { report(it) }
    }

    // ── v391 — THE DOCK IS A TYPING INSTRUMENT, NOT FURNITURE ─────────────
    //
    // Two things decide whether the tools are up (user request: "hide the tool
    // bar when keyboard is closed and i scroll down, smoothly hide it … only
    // show the journal tool bar when the keyboard is open and the cursor is in
    // focus in the text field"):
    //
    //   1. the KEYBOARD. The IME inset is read in composition the way the rest
    //      of the app reads it, so with no keyboard up there is nothing to
    //      format — which is also exactly the moment a page is being READ
    //      rather than written. Focus alone could not say this: dismissing the
    //      keyboard leaves the caret in the line.
    //   2. the SCROLL DIRECTION. A downward scroll means the member is moving
    //      through the page (looking for a place to type, or reading what they
    //      wrote) — the tools step out of the way; the smallest upward nudge
    //      brings them back, and so does coming back to write (see the reset
    //      below). The threshold is what keeps a stray pixel of drift from
    //      flapping the dock in and out.
    val densityNow = LocalDensity.current
    var dockScrolledAway by remember { mutableStateOf(false) }
    LaunchedEffect(pageScroll) {
        val threshold = with(densityNow) { 18.dp.toPx() }
        var last = pageScroll.value
        snapshotFlow { pageScroll.value }.collect { now ->
            val travel = now - last
            last = now
            if (travel > threshold) dockScrolledAway = true
            else if (travel < -threshold) dockScrolledAway = false
        }
    }

    // ── v389e — THE PAGE FOLLOWS A CARRIED BLOCK ─────────────────────────
    //
    // A block can be carried anywhere on the page, and a page is taller than its
    // window: a note dragged past the last line that fits on screen used to stop
    // dead at the fold, because nothing told the page that the finger was sitting
    // at the foot of the screen holding it (user report: "the journal page doesnt
    // auto scroll when i go to the bottom of the page while holding the vooce note
    // box"). While a drag is live, the finger's own place decides: inside the top
    // or bottom margin of the writing area, the page scrolls that way — a step per
    // frame, in the same direction as the finger — and the carried block keeps up
    // with the page (see PersonalRowDragState.advanceBy), so it stays under the
    // thumb and keeps stepping through the rows it passes.
    //
    // The loop is driven by snapshotFlow so merely STARTING a drag never
    // subscribes the page's own composition to the gesture, and the viewport is
    // held in a plain array: a scrolling page lays out every frame, and writing
    // Compose state there would recompose the whole page on a flick.
    val writingViewport = remember { floatArrayOf(0f, 0f) } // [top, height]
    LaunchedEffect(editor) {
        val margin = with(densityNow) { 96.dp.toPx() }
        val step = with(densityNow) { 9.dp.toPx() }
        snapshotFlow { editor.rowDrag.draggedId }.collectLatest { carried ->
            if (carried == null) return@collectLatest
            while (true) {
                // One step per frame, so the scroll reads as travel rather than
                // as a jump, and the finger's own reports stay in charge.
                withFrameNanos { }
                val finger = editor.rowDrag.pointerRootY
                if (finger <= 0f) continue
                val ceiling = writingViewport[0] + margin
                val floor = writingViewport[0] + writingViewport[1] - margin
                val towards = when {
                    finger > floor -> step
                    finger < ceiling -> -step
                    else -> 0f
                }
                if (towards == 0f) continue
                if (towards > 0f && !pageScroll.canScrollForward) continue
                if (towards < 0f && !pageScroll.canScrollBackward) continue
                val before = pageScroll.value
                pageScroll.scrollBy(towards)
                // ScrollState measures in whole pixels while the drag is in
                // floats, so the travel is taken as a float — as an Int the
                // comparison and the hand-off below would not even compile.
                val moved = (pageScroll.value - before).toFloat()
                if (moved != 0f) {
                    editor.rowDrag.advanceBy(moved, editor.blockIds, editor.blockIds.lastIndex)
                }
            }
        }
    }
    // Writing again is what asks for the tools back.
    LaunchedEffect(editor.focusedId) {
        dockScrolledAway = false
    }

    // ── THE PAGE'S OWN SECTIONS, HELD AT THE TOP (v389) ─────────────────
    //
    // The book review's pinned chapter, on EVERY writing page: a TITLE line is a
    // section of the page, and once one has scrolled away the page has stopped
    // saying which part of itself is being read (user request: "similiar to the
    // book review chapter pinned floating view i want something similiar in
    // journal too"). Both sides report where their title lines sit — the reading
    // side is the CALLER's view, so the reporter travels to it through
    // [LocalPersonalTitleReport] — the writing column's own scroll turns that
    // into "gone by", and the bar holds the top edge and goes back when tapped.
    val sectionLines = remember { mutableStateMapOf<String, PersonalSectionLine>() }
    // v402 — THE PIN IS JUDGED IN THE WINDOW.
    //
    // Every earlier version of this tried to translate a heading's place into the
    // scroller's own coordinates, and got it wrong every time: the first compared
    // a parent-relative number against the scroll, the next added the canvas'
    // offset twice, and the reading side was judged against the WRITING page's
    // scroll while the reader was looking at the reading page's. The observable
    // result was exactly what the member reported — a heading pinned before it had
    // gone, a heading pinned when it was still on screen, the wrong heading, the
    // bar missing altogether, and a tap that went to the top of the page.
    //
    // Window coordinates have no such problem: a title reports where it IS on the
    // screen, the writing area knows where ITS top edge is on the same screen, and
    // "gone by" is one comparison — no offsets to add, no side to translate. The
    // page's own scroll is still what makes the answer change from frame to frame
    // (it is what [liveBottom] is asked with), which is why it is read here even
    // though it is not part of the comparison.
    // The scroll a reading side writes into (see PersonalPinScrollHolder). Held on
    // the page, not read from the local: the page is the one that PROVIDES it.
    val pinHolder = remember { PersonalPinScrollHolder() }
    // Where the writing area's own top edge is on the screen — the line a heading
    // has to have gone above to be pinned.
    var areaTop by remember { mutableFloatStateOf(0f) }
    val pinnedSection by remember {
        derivedStateOf {
            val side = editing
            val scrollNow = if (side) {
                pageScroll.value.toFloat()
            } else {
                pinHolder.scroll?.value?.toFloat() ?: 0f
            }
            sectionLines.values
                // Only this side's headings, and only whole lines that have gone
                // above the writing area's own top edge (one dp of grace, so a
                // heading whose last pixel is exactly on the line counts as gone).
                .filter {
                    it.label.isNotBlank() && it.writing == side &&
                        it.liveBottom(scrollNow) <= areaTop + 1f
                }
                // The deepest one gone by is the one being read under it.
                .maxByOrNull { it.liveBottom(scrollNow) }
        }
    }
    val reportSectionLine:
        (String, String, Float, Float, Boolean, Float) -> Unit =
        { id, label, top, bottom, writing, scroll ->
            // ONE ENTRY PER SIDE: both sides report the same block ids, and each
            // side's numbers belong to its own box (see PersonalSectionLine).
            val key = if (writing) "w:$id" else "r:$id"
            if (label.isBlank()) {
                // "Not a place anymore" — a line that lost its title flag, or one
                // that left the page. Cleared rather than left behind, which is
                // what let the bar name a heading that was no longer there.
                sectionLines.remove(key)
            } else {
                sectionLines[key] = PersonalSectionLine(
                    label = label,
                    top = top,
                    bottom = bottom,
                    writing = writing,
                    // The writing side's scroll IS the page's, so it is filled in
                    // here; a read view's own scroll arrives with its report,
                    // because nobody else can see it.
                    scroll = if (writing) pageScroll.value.toFloat() else scroll
                )
            }
        }
    // A line the member deleted stops being a place to pin, whichever side of the
    // switch last reported it.
    LaunchedEffect(doc) {
        val alive = doc.blocks.map { it.id }.toSet()
        sectionLines.keys.retainAll { key -> key.substringAfter(':') in alive }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // The page's own header sits UNDER the status bar without this.
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
    ) {
        header(editing, saving, { mode -> editing = mode }, { leave() })

        // The page's subject, held still above the writing (the page decides
        // whether the reading side wants it too).
        pinnedHead(editing)

        CompositionLocalProvider(
            LocalPersonalTitleReport provides reportSectionLine,
            // The box a read view drops its own scroll into (see
            // PersonalPinScrollHolder) — provided here so a switch of side cannot
            // lose it.
            LocalPersonalPinScrollHolder provides pinHolder
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // ── v443 — THE PAPER IS THE THEME'S, AND ALWAYS ──────────────
                //
                // This box has been both ways: v429 grew "Paint the page too" so
                // the member could colour the paper itself, v439 withdrew it, v440b
                // restored it at their request, and their answer now is the other
                // one — *"in journal the paint the page remove that option"* →
                // **never paint the page**. It is the plain background again, the
                // surface the words stand on, and the member's colour stays on the
                // page's doors and its controls (see [JournalPagePaint]) where it
                // decorates rather than has to be read against.
                .background(MaterialTheme.colorScheme.background)
                // The writing area's own top edge, in the window: the line a
                // heading has to have gone above to be pinned (see
                // `pinnedSection`).
                .onGloballyPositioned { areaTop = it.boundsInWindow().top }
        ) {
            // v389 — ONE PAGE TURNING, not two pages sliding past each other.
            //
            // The first cut slid the writing in from the right and the reading
            // from the left, tied to the side of the switch that was pressed —
            // which is a NAVIGATION idea worn by a mode (user report: "switching
            // between eye and pen isnt smooth ... specially when the keyboard
            // opens the transition moved up"). The keyboard is the thing that
            // actually moves: pressing the pen focuses the page and the inset
            // rises under it. So the two sides now travel the SAME way — a few
            // dp upward, into the keyboard — and cross-fade: the swap agrees
            // with the inset instead of fighting it, and it is over quickly
            // enough (200ms) that the keyboard's own rise is the motion the eye
            // follows. Nothing is measured differently either: both sides fill
            // the same box, so no clip and no jump while the inset animates.
            // v389d — AND THE TURN IS A FADE, NOT A SLIDE.
            //
            // The travel was the last thing that could still be seen moving in
            // the wrong direction: `imePadding()` resizes this box WHILE the
            // keyboard rises, so the vertical offset being animated (a fraction
            // of the box's own height) was computed from a height that was
            // changing under it — the page visibly ducked and jumped at exactly
            // the moment the member was switching (user report: "the keyboard
            // open really makes the eye view to pen buggy. like the animation
            // shifts fir the page"). A cross-fade cannot be moved by a resize,
            // so the keyboard is now the only thing that travels, and the two
            // sides simply trade places.
            AnimatedContent(
                targetState = editing,
                transitionSpec = {
                    CurioMotion.arriveFade() togetherWith CurioMotion.leaveFade()
                },
                label = "personal-page-mode",
                modifier = Modifier.fillMaxSize()
            ) { writing ->
                if (!writing) {
                    // v389 — a checklist row is tickable WHILE READING: the view
                    // draws the box, this page owns the document, so the write
                    // is handed down rather than reached for (the tick lands in
                    // the same store the editor writes to, and the page's own
                    // debounce saves it).
                    //
                    // …and the whole reading side ANSWERS A DOUBLE TAP with the
                    // pen (user request). A reader who wants to add a line should
                    // not have to travel to the switch's 34dp: the page they are
                    // reading is the door. The detector sits UNDER the view, so a
                    // child that consumes its own tap (a checklist box, a photo)
                    // keeps it — only the page's own blank space answers.
                    // v389g — ONE door, TWO detectors.
                    //
                    // The detector under the page answers a double tap on BLANK
                    // space, and the read view's own text detector answers it on
                    // WORDS (it has to own its taps — a URL in a page opens the
                    // browser — which is exactly why taps on text never reached
                    // this one). Both are handed the same action through
                    // [LocalPersonalTapToEdit] (user request: "double tap on the
                    // whole page too not just blank area, but also over the text
                    // works too").
                    //
                    // `remember`ed on the editor so the local's value is stable:
                    // a fresh lambda every recomposition would invalidate every
                    // reader of the local, which is the whole read subtree.
                    // The explicit `() -> Unit` type is load-bearing: without it
                    // `remember` infers the lambda's own return type, which then
                    // has to match the local's `(() -> Unit)?` and would not.
                    val tapToEdit: () -> Unit = remember(editor) {
                        {
                            editing = true
                            editor.focusLastLine()
                        }
                    }
                    CompositionLocalProvider(
                        LocalPersonalCheckToggle provides { index ->
                            val block = doc.blocks.getOrNull(index)
                            if (block != null) editor.setChecked(block.id, !block.checked)
                        },
                        LocalPersonalTapToEdit provides tapToEdit,
                        // v429 — the READING side's paper (see [pagePaint]).
                        LocalJournalPagePaint provides pagePaint
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(onDoubleTap = { tapToEdit() })
                                }
                        ) { readView(doc) }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(pageScroll)
                            // Where the writing area sits in the window, for the
                            // drag's auto-scroll (see writingViewport).
                            .onGloballyPositioned { coordinates ->
                                writingViewport[0] = coordinates.positionInRoot().y
                                writingViewport[1] = coordinates.size.height.toFloat()
                            }
                            // v389 — the blank part of a page is writing space too: a
                            // tap anywhere in the gaps (under the last line, between
                            // the title and the words) hands the caret to the last
                            // line instead of needing the "Write…" placeholder to be
                            // hit exactly (user report).
                            .clickable { editor.focusLastLine() }
                            .padding(horizontal = 20.dp)
                            .widthIn(max = 680.dp)
                    ) {
                        Spacer(Modifier.height(6.dp))
                        // v402 — THE HEAD IS LAID OUT, NOT MEASURED.
                        //
                        // This wrapper existed only to measure where the canvas
                        // began inside the scroll, and that number was what the pin
                        // maths was handed — an offset that grew three versions of
                        // patches and never became right (see `pinnedSection`).
                        // Headings now report where they are ON THE SCREEN, so
                        // there is nothing to add and nothing to keep in step: a
                        // Column lays the page's head out, and that is all it does
                        // (a Box STACKED the mood pill on the title — "the mood
                        // select and the journal title … are overlapping").
                        Column { aboveCanvas() }
                        // v429 — the WRITING side's paper: the canvas paints the
                        // page the member types on, so it is told the same paint
                        // the reading side was ("the journal page color also
                        // needs to chnage with the color chnage").
                        CompositionLocalProvider(
                            LocalJournalPagePaint provides pagePaint
                        ) {
                            PersonalCanvas(
                                state = editor,
                                modifier = Modifier.fillMaxWidth(),
                                onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) },
                                onTitlePosition = reportSectionLine
                            )
                        }
                        Spacer(Modifier.height(140.dp))
                    }
                }
            }

            // The pill floats INSIDE the writing area, so a swipe away and the
            // undo of it never move a line of the page.
            PersonalFloatingLayer(
                visible = editing && removedRow != null,
                // v439 — ONE ARRIVAL (see [CurioMotion.pillArrive]).
                enter = CurioMotion.pillArrive(),
                exit = CurioMotion.pillLeave(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            ) {
                PersonalUndoPill(
                    label = "Row removed",
                    onUndo = { editor.restoreRemovedRow() },
                    onDismiss = { editor.clearRemovedRow() }
                )
            }

            // THE PAGE'S OWN MIC (v389). It floats over the WRITING, clear of
            // the dock — the dock is a row of buttons a thumb sweeps across, and
            // a mic sitting in that row is one stray drag from a recording. It
            // belongs to THIS box and not to the dock's, for a reason that was a
            // bug: a button hanging outside its parent's bounds is never hit-
            // tested, so the version that rode the dock's top edge looked dead
            // (user report: "tapping it doesnt do anything"). Here it is inside
            // the writing area, above the toolbar, and the whole disc is tappable.
            // ── v433 — AND THE MIC STEPS ASIDE FOR THE COPY BOX ──────────
            //
            // The member: "hide the voice note option when copy tools are on".
            // The box floats over this same corner of the page, so a mic under
            // it would be a button half-covered by the thing the member is
            // using — and a recording started from under a copy box is not a
            // recording anyone asked for.
            PersonalFloatingLayer(
                visible = editing && liveVoice == null && !editor.pageEditBarOpen,
                // v439 — the free-floating control: the POP, not a slide.
                enter = CurioMotion.popArrive(),
                exit = CurioMotion.popLeave(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 16.dp)
            ) {
                PersonalVoiceButton(onClick = askToRecord)
            }

            // THE PAGE'S OWN PINNED SECTION — the same bar the book review
            // wears for its chapters, and a door rather than a label: tapping it
            // goes back to the heading it names.
            if (AppPreferences.pinnedTitleViewState) PersonalPinnedLine(
                label = pinnedSection?.label.orEmpty(),
                caption = "Heading",
                accent = personalAccent(),
                onClick = {
                    // A DOOR: the bar IS the heading it names, so tapping it puts
                    // that heading at the top edge of the writing area. The
                    // distance is asked for in the window and applied to whichever
                    // scroll is on screen, so the same tap works from the reading
                    // side as from the writing one — and it can never land at the
                    // top of the page instead of at the heading.
                    val section = pinnedSection ?: return@PersonalPinnedLine
                    val scroll = (if (editing) pageScroll else pinHolder.scroll)
                        ?: return@PersonalPinnedLine
                    val delta = section.liveTop(scroll.value.toFloat()) - areaTop
                    scope.launch {
                        scroll.animateScrollTo(
                            (scroll.value + delta).toInt().coerceIn(0, scroll.maxValue)
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 10.dp)
            )
        }
        }

        // The dock rides with the page while it is being WRITTEN and steps
        // out of the way only while it is being READ. A live recording REPLACES
        // it: the voice capsule is what the page's bottom is for while it runs.
        //
        // v394 — the dock NO LONGER WAITS FOR THE KEYBOARD (user request: "keep
        // the tool visible in journal page when i close the keyboard dont hide
        // it"): writing mode is the dock's home — the keyboard going down (to
        // drag a photo, to read back a paragraph, to rest) never pulls the
        // tools with it. It still yields to a recording and to a scroll down.
        //
        // ── v433 — AND THE PAGE'S COPY BOX RIDES ABOVE IT ────────────────
        //
        // The member: "also the copy floating layout … add a cross button to
        // close the option box". The box used to TAKE the dock's own row, which
        // made the page's cut / copy / paste read as five more tools and left
        // the member no way out of them but a word. It is a floating box of its
        // own now — over the writing, above the dock, of the journal's own paper
        // with a real lift — so the dock's tools stay where they were and the
        // cross on the box is what puts it away (see [PersonalPageEditBar]).
        AnimatedVisibility(
            visible = editing && editor.pageEditBarOpen,
            // v439 — the copy box is a pill like any other (it grows UP out of the
            // dock's own edge, so it arrives with the same settle).
            enter = CurioMotion.pillArrive(),
            exit = CurioMotion.pillLeave(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                PersonalPageEditBar(
                    state = editor,
                    accent = personalAccentInk(),
                    ink = journalInk()
                )
            }
        }

        // ── v437 — AND THE DOCK STEPS ASIDE FOR THE COPY BOX ─────────────
        //
        // The member: *"hide the dock just show the copy doc when thats on"*.
        // v433 kept the two stacked — the box floating above a dock that still
        // held its nine writing tools — which left the page's bottom with two
        // toolbars and eighteen controls in it while the member was doing the
        // one thing: picking a reach and cutting it. The copy box IS the page's
        // bottom while it is open, so the dock goes away with the same slide it
        // leaves by when the writing is scrolled past (see [dockScrolledAway]).
        AnimatedVisibility(
            visible = editing && liveVoice == null && !dockScrolledAway &&
                !editor.pageEditBarOpen,
            enter = CurioMotion.pillArrive(),
            exit = CurioMotion.pillLeave(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // v429 — AND THE DOCK, because the dock is where the page's
                // EXPORT is resolved: a file a page leaves as must wear the paper
                // the page is wearing, painted or not (see [PersonalExport]).
                CompositionLocalProvider(
                    LocalJournalPagePaint provides pagePaint
                ) {
                    PersonalToolDock(
                        state = editor,
                        onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                        showJournalTools = showJournalTools,
                        journalAccent = journalAccent,
                        onJournalAccent = onJournalAccent,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = editing && liveVoice != null,
            enter = CurioMotion.pillArrive(),
            exit = CurioMotion.pillLeave(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (liveVoice != null) {
                    PersonalVoiceRecorderCapsule(
                        session = liveVoice,
                        onKeep = {
                            scope.launch {
                                val voice = PersonalVoiceRecording.keep(context, entryId)
                                if (voice != null) editor.insertVoice(voice)
                            }
                        },
                        onDiscard = { PersonalVoiceRecording.discard() }
                    )
                }
            }
        }
    }

    if (leavePrompt) {
        PersonalVoiceLeaveDialog(
            elapsed = liveVoice?.elapsed ?: "",
            onKeepRecording = { leavePrompt = false },
            onKeepNote = {
                leavePrompt = false
                scope.launch {
                    val voice = PersonalVoiceRecording.keep(context, entryId)
                    if (voice != null) editor.insertVoice(voice)
                    onExit()
                }
            },
            onDiscard = {
                leavePrompt = false
                PersonalVoiceRecording.discard()
                onExit()
            }
        )
    }

    if (recordFailed) {
        AlertDialog(
            onDismissRequest = { recordFailed = false },
            title = { Text("Could not start recording") },
            text = {
                Text("The microphone is busy or unavailable. Try again in a moment.")
            },
            confirmButton = {
                TextButton(onClick = { recordFailed = false }) { Text("OK") }
            }
        )
    }

    // Android's own page for THIS app, when the device has one.
    val micSettings = remember(context) { appPermissionSettingsIntent(context) }
    if (micDenied) {
        AlertDialog(
            onDismissRequest = { micDenied = false },
            title = { Text("Curio needs the microphone") },
            text = {
                Text(
                    "Recording a voice note on this page needs microphone access. " +
                        "If Android will not ask again, its own page for Curio is " +
                        "where it is turned back on."
                )
            },
            confirmButton = {
                if (micSettings != null) {
                    TextButton(onClick = {
                        micDenied = false
                        runCatching { context.startActivity(micSettings) }
                    }) { Text("Open settings") }
                } else {
                    TextButton(onClick = { micDenied = false }) { Text("OK") }
                }
            },
            dismissButton = if (micSettings != null) {
                { TextButton(onClick = { micDenied = false }) { Text("Not now") } }
            } else {
                null
            }
        )
    }
}

/**
 * v389 — WHERE A SAVED PAGE LIVES.
 *
 * Three kinds of personal page, three screens, and ONE place that knows which
 * is which: the journal list, the Home chips, the Cabinet's Personal shelf and
 * the note page's own head all route through this, so a checklist can never
 * open in the journal editor (which would greet it with a date bar and a mood
 * pill) and a note about a topic always opens on its topic page.
 */
internal fun personalRouteFor(note: PersonalNoteEntity): String = when {
    note.isTodo -> CurioRoutes.todo(note.id)
    note.hasTopic -> CurioRoutes.topicNote(note.id)
    else -> CurioRoutes.journalEditor(note.id)
}

/**
 * v389 — A FLOATING LAYER: `AnimatedVisibility` inside a Box that lives inside
 * a Column.
 *
 * Compose ships a `ColumnScope.AnimatedVisibility` overload, and calling the
 * plain one from a Box nested in a Column makes the compiler pick the SCOPED
 * overload and then reject it ("cannot be called in this context with an
 * implicit receiver"). Hoisting the call into this function takes it out of the
 * Column's implicit scope once, which is all the disambiguation it needs. The
 * caller still computes its own `Modifier.align(...)`, because that belongs to
 * the Box it floats in.
 */
@Composable
internal fun PersonalFloatingLayer(
    visible: Boolean,
    enter: EnterTransition,
    exit: ExitTransition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit
    ) {
        content()
    }
}

/**
 * v389 — THE FAMILY'S READ/WRITE SWITCH.
 *
 * The eye hides the tools and stops the page being typed into; the pen hands the
 * writing back. Whichever is lit is the mode the page is IN. It lives here
 * because a journal day and a note on a topic both need it, and the two must not
 * disagree about which icon means which mode.
 */
/**
 * v402 — WHERE ONE OF A PAGE'S TITLE LINES SITS, AND WHAT IT SAYS.
 *
 * The numbers are WINDOW coordinates ([androidx.compose.ui.layout.boundsInWindow],
 * taken by the reporting line) and [scroll] is how far the reporting side had
 * scrolled when they were taken. The pair is what makes a report usable as the
 * page keeps moving: a report is taken when a line is LAID OUT, which happens
 * rarely, while the page slides under the member on every frame — so the host
 * asks [liveTop] / [liveBottom] with the scroll it can see right now instead of
 * trusting a stale number.
 *
 * [writing] says which side of the eye / pen switch reported it. Both sides hold
 * the same block ids and different scrolls in different boxes, so one entry per
 * side is kept and only the side being SHOWN is ever read (see
 * `LocalPersonalTitleReport`).
 */
internal data class PersonalSectionLine(
    val label: String,
    val top: Float,
    val bottom: Float,
    val writing: Boolean = true,
    val scroll: Float = 0f
) {
    /** Where the line's top edge is NOW, in the window. */
    fun liveTop(scrollNow: Float): Float = top - (scrollNow - scroll)

    /** Where the line's BOTTOM edge is NOW — the edge the pin is judged on, so
     *  a heading is pinned only once its whole line has gone by. */
    fun liveBottom(scrollNow: Float): Float = bottom - (scrollNow - scroll)
}

/**
 * v389 — THE PINNED LINE.
 *
 * A page whose own headings have scrolled away has stopped saying where the
 * member is; this is the bar that says it, holding the top edge of the writing
 * area and naming the heading the words under it belong to. SHARED on purpose —
 * the book review's chapter markers, the chapter review's, and the journal's own
 * section titles all ride it (user request: "similiar to the book review chapter
 * pinned floating view i want something similiar in journal too") — and it is a
 * DOOR rather than a label: tapping it goes back to the heading it names.
 *
 * It is deliberately a size up from a chip and sits close under the page's
 * header — near enough to read as belonging to it, clear enough not to touch it
 * (user request: "make the pinned view bigger and attached to the header a
 * little, not like attached but closer"). Nothing here knows about scrolling:
 * the caller decides the label, and a blank one is simply the bar being away.
 *
 * v402 — IT WEARS THE PAGE'S OWN ACCENT, AND IT SAYS WHAT IT IS.
 *
 * The bar was a plain grey pill carrying one line of text: it named a place but
 * never said what KIND of place it was naming (a heading of a journal reads the
 * same as a chapter of a book), and its only colour was a small grey rail. It now
 * reads as a small card of the page it belongs to — a filled accent medallion (the
 * same rail-glyph idea, drawn as the app's own bookmark), the kind of place as a
 * micro-label over the name ([caption]: "Heading" on a journal, "Chapter" on a
 * book review), and the way back as an accent chevron. The name still swaps
 * through a small vertical fade, so a change of heading reads as a change rather
 * than a replacement (user request: "make the pinned view ui better and
 * matching").
 */
@Composable
internal fun PersonalPinnedLine(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** What kind of place a heading is on this page — "Heading" for a page's own
     *  titles, "Chapter" for a book review's markers. */
    caption: String = "Heading"
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    // The medallion's fill is the accent BLENDED INTO the pill's own surface
    // rather than the accent at a low alpha: a translucent fill on a surface that
    // floats over the page lets the words underneath bleed through it (the root
    // rail's own rule — see AGENTS rule 11).
    val badge = lerp(surface, accent, 0.22f)
    val badgeInk = lerp(MaterialTheme.colorScheme.onSurface, accent, 0.55f)
    AnimatedVisibility(
        visible = label.isNotBlank(),
        // v439 — the pinned line hangs from the TOP edge of the writing area.
        enter = CurioMotion.pillArrive(fromTop = true),
        exit = CurioMotion.pillLeave(fromTop = true),
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            color = surface,
            // A hairline of the page's own accent: the bar belongs to the page it
            // is standing on, and a shadowed grey pill could belong to any of them.
            border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // THE MEDALLION — the place's own mark, at the app's floating-
                // control size (30dp, the same as the page's own mic), so the bar
                // reads as one of the page's objects rather than a notice.
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(badge, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 15.dp)
                            .background(badgeInk, RoundedCornerShape(50))
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        caption.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = accent,
                        maxLines = 1
                    )
                    AnimatedContent(
                        targetState = label,
                        transitionSpec = {
                            // v439 — the heading's own name changing, inside the
                            // pill it is written on (it comes from BELOW, the way
                            // the writing it belongs to moves).
                            CurioMotion.pillArrive() togetherWith CurioMotion.pillLeave()
                        },
                        label = "personal-pinned-line"
                    ) { shown ->
                        Text(
                            shown,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                CurioIcon(
                    CurioIcons.KeyboardArrowUp,
                    "Back to this heading",
                    tint = accent,
                    size = 17.dp
                )
            }
        }
    }
}

@Composable
internal fun PersonalModeSwitch(
    editing: Boolean,
    onToggleMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = personalAccent()
    val calm = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    // v389 — THE LIT HALF TRAVELS (user report: the switch "looks clanky",
    // especially when the keyboard is coming up at the same time). A fill that
    // appears on one button and vanishes from the other is two states being
    // swapped; a fill that SLIDES is one control being moved — and the icons
    // tint through the same motion, so the whole switch is a single gesture
    // rather than two recolours and a jump.
    val slide = animateFloatAsState(
        targetValue = if (editing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 900f),
        label = "personal-mode-slide"
    )
    val onInk = personalOnAccent()
    // ── v433 — IT IS THE PAGE'S CAPSULE NOW (see [JournalCapsule]) ─────────
    //
    // The member: "make the today and eye pen pill more capsule like … use one
    // unified capsule style, so they look good". The switch was 40dp tall with
    // 34dp halves, beside a 36dp date pill: two controls of two heights, both of
    // them thin next to the page's own type. It is the shared capsule now, and
    // its two halves are the capsule minus its padding, so the travelling fill
    // is exactly one half of the thing it travels in.
    val half = JournalCapsule.Height - 6.dp
    val gap = 2.dp
    // The window is one half + the gap: one button each way.
    val travel = (slide.value * (half + gap).value).dp
    Surface(
        shape = JournalCapsule.Shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(3.dp)) {
            // v394 — THE TRAVELLING HALF IS A SOLID FILL (user request: "make
            // some button solid filled in journal: the eye and pen switch"). The
            // airy 26% wash read as a selection, not a position; the accent's
            // full colour under the lit icon reads as the switch it is. The
            // lit icon wears the ink that sits ON the accent fill.
            Box(
                modifier = Modifier
                    .offset(x = travel)
                    .size(half)
                    .background(color = accent, shape = JournalCapsule.Shape)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                PersonalModeButton(
                    label = "Reading",
                    size = half,
                    onClick = { onToggleMode(false) }
                ) {
                    EyeGlyph(tint = lerp(onInk, calm, slide.value))
                }
                PersonalModeButton(
                    label = "Writing",
                    size = half,
                    onClick = { onToggleMode(true) }
                ) {
                    CurioIcon(
                        CurioIcons.Edit,
                        null,
                        tint = lerp(calm, onInk, slide.value),
                        size = 18.dp
                    )
                }
            }
        }
    }
}

/** One half of the switch — the FILL under it is the switch's own (it travels
 *  between the two), so a button paints nothing of its own. */
@Composable
private fun PersonalModeButton(
    label: String,
    size: Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = JournalCapsule.Shape,
        color = Color.Transparent,
        modifier = Modifier.size(size)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** The eye itself — drawn here, because the icon set only bundles the struck
 *  version and the reading mode is not a "hidden" state. Its ink is handed in:
 *  the switch's slide decides how lit it is. */
@Composable
private fun EyeGlyph(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(19.dp)) {
        val stroke = 1.6f.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.06f, h * 0.5f)
            cubicTo(w * 0.3f, h * 0.16f, w * 0.7f, h * 0.16f, w * 0.94f, h * 0.5f)
            cubicTo(w * 0.7f, h * 0.84f, w * 0.3f, h * 0.84f, w * 0.06f, h * 0.5f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = stroke))
        drawCircle(color = tint, radius = h * 0.13f, center = Offset(w * 0.5f, h * 0.5f))
    }
}

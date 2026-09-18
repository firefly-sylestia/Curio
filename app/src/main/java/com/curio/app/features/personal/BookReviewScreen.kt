package com.curio.app.features.personal

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.WritingFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v389 — A BOOK'S OWN REVIEW: ONE PAGE FOR THE WHOLE BOOK.
 *
 * Writing about a book used to mean writing about each chapter in turn — open
 * chapter 7, write, come back, open chapter 8. A member who wanted to say
 * something about the BOOK had nowhere to say it, and a chapter-by-chapter
 * pass is exactly the work a whole-book page should remove.
 *
 * So a book gets its own page, the member's own note with no chapter index, and
 * it is a page in the journal's shape:
 *
 *  · READ FIRST. The review is the page, and each chapter's OWN review folds in
 *    under the marker that names it — so one page reads the whole book back.
 *  · WRITE ON TAP, with the dock rising on the keyboard, plus ONE floating door
 *    the ordinary dock cannot offer: **Add chapter**. It opens the BOOK'S OWN
 *    chapter list — number and name, the same list the read view folds reviews
 *    under — and drops the picked one in as a title-style marker line at the
 *    caret ([insertTitleLine]); a book with no chapter list still gets a blank
 *    marker to name itself. A chapter marker is only a heading in their own
 *    review.
 *  · NOTHING IS A SAVE BUTTON. It persists while typing, when the app leaves the
 *    foreground, and once more on the way out (the flush the chapter page was
 *    missing), so a back gesture cannot lose the last words.
 */
@Composable
fun BookReviewScreen(
    navController: NavController,
    bookId: String,
    photos: PersonalPhotoOverlayState = rememberPersonalPhotoOverlayState()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val book by produceState<PersonalBookEntity?>(initialValue = null) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBook(bookId).collect { value = it }
        }
    }
    val notes by produceState(initialValue = emptyList<PersonalNoteEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBookNotes(bookId).collect { value = it }
        }
    }
    // The book's own note (no chapter) is ITS review; the rest are the chapters'
    // — the same rows the chapter pages and the topic's Book Notes sheet write.
    val review = notes.firstOrNull { it.chapterIndex == null }
    val chapterReviews = notes.filter { it.chapterIndex != null }
    val chapterNames = rememberBookChapters(book)

    var notesLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(notes) { notesLoaded = true }

    // ── The page's own mic (v389) ──────────────────────────────────────
    // The book review is its own layout, so it carries the family's mic by hand
    // (see PersonalVoiceMic). `voiceId` is this PAGE's tag — stable for as long
    // as the review is open, and what the keep-recording pill comes back to.
    val voiceId = rememberSaveable(bookId) { "book-review-" + bookId }
    val liveVoice = PersonalVoiceRecording.session?.takeIf { it.noteId == voiceId }
    var leavePrompt by remember { mutableStateOf(false) }

    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(PersonalDoc(emptyList())) }
    var seeded by remember { mutableStateOf(false) }
    val editor = remember { PersonalEditorState(PersonalDoc(emptyList())) }
    SideEffect {
        editor.onDocChanged = { updated -> draft = updated }
    }

    // ── WHICH CHAPTER THE WORDS BELONG TO (v389) ────────────────────────
    //
    // The review's chapter markers are TITLE lines in the page's own document
    // (see [PersonalEditorState.insertTitleLine]), so the page can say where it
    // is by itself — without the chapter list, and without the member naming
    // anything twice. Each marker reports its own place in the scrolling
    // content (both sides of the page do this: the canvas while writing, the
    // doc view while reading), and the scroll offset turns that into "above the
    // top". A marker is the CURRENT chapter once its own line has gone up out
    // of sight; while it is still on screen the writing under it belongs to the
    // chapter before it, which is why the pinned line changes as the next
    // marker arrives rather than when it is written (user request: "add like a
    // top pinned chapter switching … when im on the start point of that chapter
    // it switches to the previous chapter view").
    val readScroll = rememberScrollState()
    val writeScroll = rememberScrollState()
    val chapterLines = remember { mutableStateMapOf<String, PinnedChapterLine>() }
    // WHICH MARKER IS BEHIND US — and where to go back to it.
    //
    // A marker is behind us only once its WHOLE line has gone up: the bar names
    // the chapter being READ, and the heading of the chapter being read is not
    // that. v389 fixes the glitch the member reported — the bar used to appear
    // the instant a heading's top edge touched the top edge of the page, which
    // on the page's own FIRST heading (whose top is 0 before a single line has
    // scrolled) meant it was up and naming chapter one the moment the review
    // opened (user report: "in reading view it shows that title at the same
    // position even though the title isnt scrolled away yet"). The bottom edge
    // is what answers the question, and the id rides along so the bar can be
    // TAPPED to go back to the heading it names.
    val pinnedMarker by remember {
        derivedStateOf {
            val scroll = (if (editing) writeScroll.value else readScroll.value).toFloat()
            chapterLines.entries
                .filter { it.value.label.isNotBlank() && it.value.bottom - scroll <= 0f }
                .maxByOrNull { it.value.bottom }
        }
    }
    val pinnedChapter = pinnedMarker?.value?.label.orEmpty()
    // A line the member deleted (or renamed away) stops being a place to pin.
    LaunchedEffect(draft, review) {
        val alive = (draft.blocks + (review?.doc?.blocks ?: emptyList())).map { it.id }.toSet()
        chapterLines.keys.retainAll(alive)
    }
    // v389d — THE MARKER IS MEASURED IN THE SCROLL'S OWN SPACE.
    //
    // A chapter marker reports where it sits inside the WRITING COLUMN, and the
    // column is a child of the scrolling page — so the spacer above it was
    // missing from every number. The pin therefore lit up before its heading had
    // actually gone (user report: "it's very buggy in books … shows the floating
    // pin but sometimes it doesn't"), and tapping it scrolled to the wrong place.
    // The canvas' own place in the scroll is measured and added, so the judge and
    // the jump agree.
    var canvasTop by remember { mutableFloatStateOf(0f) }
    val reportChapterLine: (String, String, Float, Float) -> Unit = { id, label, top, bottom ->
        chapterLines[id] = PinnedChapterLine(label, top + canvasTop, bottom + canvasTop)
    }

    // Entering the canvas seeds it from the stored review exactly once (re-
    // seeding mid-typing is what drops the caret).
    LaunchedEffect(editing, review, notesLoaded) {
        if (!editing || seeded || !notesLoaded) return@LaunchedEffect
        val document = review?.doc ?: PersonalDoc(emptyList())
        draft = document
        editor.replace(document)
        seeded = true
    }

    val liveDraft = rememberUpdatedState(draft)

    /**
     * v389d — AN EMPTIED REVIEW IS AN EDIT.
     *
     * Refusing to write an empty body is right for a book nobody has written
     * about, and wrong for one the member has just cleared: deleting the whole
     * review and leaving skipped the save, so the next visit put back exactly
     * what was deleted (user report: "in book review or any page i can't leave
     * the page blank after saving once, and each time i delete all text it keeps
     * coming back when i exit"). A write is allowed when there are words OR when
     * the stored review HAD words — deleting them is the edit.
     */
    fun shouldWrite(): Boolean =
        !liveDraft.value.isEmpty || review?.doc?.isEmpty == false

    fun saveNow() {
        val body = liveDraft.value
        if (!shouldWrite()) return
        scope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                runCatching { PersonalRepositoryHolder.repo.saveBookReview(bookId, body) }
            }
        }
    }

    /** A recording outlives a page's back gesture only with the member's say-so. */
    fun leave() {
        if (liveVoice != null) leavePrompt = true
    }

    // Debounced while typing…
    LaunchedEffect(editing, draft) {
        if (!editing || !shouldWrite()) return@LaunchedEffect
        delay(700)
        withContext(Dispatchers.IO) {
            runCatching { PersonalRepositoryHolder.repo.saveBookReview(bookId, draft) }
        }
    }

    // …flushed when the app leaves the foreground…
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, editing) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && editing) saveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // …and ONCE MORE on the way out: the debounce above lives inside the
    // composition, so a back gesture cancels it mid-wait. This runs on its own
    // scope, because by then the composition's scope is already cancelled.
    DisposableEffect(bookId) {
        val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        onDispose {
            val body = liveDraft.value
            if (shouldWrite()) {
                flushScope.launch {
                    runCatching { PersonalRepositoryHolder.repo.saveBookReview(bookId, body) }
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            editor.insertPhoto(uri.toString())
        }
    }

    // Which chapter the floating "Add chapter" door is offering (see below).
    var chapterMenu by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = personalAccent()

    /** The chapter a marker names — by number ("Chapter 7", "7") or by its own
     *  name — so a folded review lands under the marker that belongs to it. */
    fun chapterForMarker(marker: String): PersonalNoteEntity? {
        val text = marker.trim()
        val number = Regex("(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
        number?.let { n -> chapterReviews.firstOrNull { it.chapterIndex == n }?.let { return it } }
        val index = chapterNames.indexOfFirst { it.title.trim().equals(text, ignoreCase = true) }
        if (index < 0) return null
        return chapterReviews.firstOrNull { it.chapterIndex == index + 1 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = {
                    // A live recording is asked about FIRST (see PersonalVoice).
                    val live = liveVoice != null
                    if (live) {
                        leave()
                    } else if (editing) {
                        saveNow()
                        editing = false
                    } else {
                        navController.popBackStack()
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(CurioIcons.ArrowBack, "Back", tint = ink, size = 20.dp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Book review",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(
                        book?.title?.takeIf { it.isNotBlank() },
                        book?.author?.takeIf { it.isNotBlank() }
                    ).joinToString(" \u00b7 "),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // THE FAMILY'S EYE/PEN, like every other page in the personal
            // family (user request: "in book review it doesnt have the eye and
            // pen style switch fix that"). Leaving the pen SAVES first: the
            // text pill this replaces was the page's only "Done", so the switch
            // wears that duty too, and the page never leaves writing without
            // writing what was written.
            PersonalModeSwitch(
                editing = editing,
                onToggleMode = { writing ->
                    if (!writing) saveNow()
                    editing = writing
                }
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Crossfade(
                targetState = editing,
                animationSpec = tween(220),
                label = "book-review-read-write",
                modifier = Modifier.fillMaxSize()
            ) { writing ->
                if (writing) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(writeScroll)
                            // The blank part of a review is writing space too: a
                            // tap in the gaps (under the last line, above the
                            // dock) hands the caret to the last line and the
                            // keyboard follows it — the page used to sit there
                            // dead, with the caret nowhere (user report: "in book
                            // review or chapter review i tap the blank space to
                            // write but the cursor doesnt start and my keyboard
                            // too"). The journal's own writing column has done
                            // this since v389; the book's two pages had not.
                            .clickable { editor.focusLastLine() }
                            .padding(horizontal = 20.dp)
                            .widthIn(max = 680.dp)
                    ) {
                        // v389d — THE CANVAS' PLACE IN THE SCROLL.
                        // v389h — AND THE NUMBER IS STORED. The book review had the
                        // journal page's exact defect: the head's height was measured
                        // into a local nothing else could see and `canvasTop` stayed
                        // 0, so the pin judged every marker against a coordinate
                        // that stopped at the canvas' own top (wrong chapter,
                        // early arrival, a tap that jumped to the top).
                        var aboveContentHeight by remember { mutableFloatStateOf(0f) }
                        val density = LocalDensity.current
                        Box(
                            Modifier.onSizeChanged {
                                aboveContentHeight = it.height.toFloat() + with(density) { 4.dp.toPx() }
                                canvasTop = aboveContentHeight
                            }
                        ) { Spacer(Modifier.height(0.dp)) }
                        PersonalCanvas(
                            state = editor,
                            modifier = Modifier.fillMaxWidth(),
                            accent = accent,
                            onTitlePosition = reportChapterLine,
                            onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
                        )
                        Spacer(Modifier.height(160.dp))
                    }
                } else {
                    // A DOUBLE TAP ON THE READING SIDE hands the pen back (user
                    // request: "when im on eye view and i double tap switch to
                    // edit pen mode, for all").
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        editing = true
                                        editor.focusLastLine()
                                    }
                                )
                            }
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(readScroll)
                            .padding(horizontal = 20.dp)
                            .widthIn(max = 680.dp)
                    ) {
                        val document = review?.doc
                        if (document == null || document.isEmpty) {
                            BookReviewEmpty(accent = accent, ink = ink) { editing = true }
                        } else {
                            PersonalDocView(
                                doc = document,
                                ink = ink,
                                accent = accent,
                                onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) },
                                onTitlePosition = reportChapterLine,
                                // A chapter marker carries its chapter's own
                                // review: the whole book reads back on one page.
                                afterTitle = { marker ->
                                    val folded = chapterForMarker(marker)
                                    if (folded != null) {
                                        FoldedChapterReview(
                                            chapter = folded.chapterIndex ?: 0,
                                            name = chapterNames
                                                .getOrNull((folded.chapterIndex ?: 1) - 1)
                                                ?.title
                                                .orEmpty(),
                                            doc = folded.doc,
                                            ink = ink,
                                            accent = accent,
                                            onOpenPhoto = { uri, bounds ->
                                                photos.open(uri, bounds)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(140.dp))
                    }
                    }
                }
            }

            // THE PINNED CHAPTER.
            //
            // It holds the top edge of the page and names the chapter the words
            // under it belong to — the one thing a long review cannot say for
            // itself once its markers have gone by. It exists only after a
            // marker has been scrolled past (there is nothing to pin before
            // that), and the label swaps through a small vertical fade so a
            // change of chapter reads as a change, not a replacement.
            //
            // Through `PersonalFloatingLayer`: this is a Box inside a Column, so
            // a bare `AnimatedVisibility` resolves to the ColumnScope overload
            // and is then rejected (see that function's own note).
            if (AppPreferences.pinnedTitleViewState) PersonalPinnedLine(
                label = pinnedChapter,
                accent = accent,
                onClick = {
                    // A DOOR, not a label: the bar IS the chapter it names, so
                    // tapping it goes back to that heading (user request:
                    // "tapping that floating pinned should take me to that title
                    // position").
                    val marker = pinnedMarker ?: return@PersonalPinnedLine
                    scope.launch {
                        val target = marker.value.top.toInt().coerceAtLeast(0)
                        if (editing) writeScroll.animateScrollTo(target)
                        else readScroll.animateScrollTo(target)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 10.dp)
            )

            // THE PAGE'S OWN MIC. The Add-chapter door has the RIGHT corner
            // (below), so the mic takes the LEFT: the two float side by side
            // instead of on top of each other (user request: "in book review
            // keep it mind theres add chapter floating button too so properly
            // adjust it").
            PersonalFloatingLayer(
                visible = editing && liveVoice == null,
                enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.80f),
                exit = fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.80f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 12.dp)
            ) {
                PersonalVoiceMic(
                    entryId = voiceId,
                    route = CurioRoutes.bookReview(bookId)
                )
            }

            // THE FLOATING CHAPTER DOOR. It rides over the writing area, above
            // the dock, and it is the one thing the ordinary tool dock cannot
            // give: a marker in the member's own review.
            PersonalFloatingLayer(
                visible = editing,
                enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 2 },
                exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 10.dp)
            ) {
                Box {
                    Surface(
                        onClick = { chapterMenu = true },
                        shape = RoundedCornerShape(50),
                        color = accent,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.FoldedCorner,
                                null,
                                tint = personalOnAccent(),
                                size = 16.dp
                            )
                            Text(
                                "Add chapter",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = personalOnAccent()
                            )
                        }
                    }
                    // WHICH chapter — the book's own list, the same one the read
                    // view folds reviews under, so a marker is picked rather
                    // than typed. The label carries the NUMBER as well as the
                    // name ("Chapter 7 · The Fall") because that is what the
                    // fold matches on, and a book with no chapter list at all
                    // still gets a blank marker to name itself.
                    DropdownMenu(
                        expanded = chapterMenu,
                        onDismissRequest = { chapterMenu = false }
                    ) {
                        chapterNames.forEachIndexed { index, chapter ->
                            // The book's own title, with the number said once —
                            // see chapterMarkerLabel.
                            val label = chapterMarkerLabel(index, chapter.title)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = WritingFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    chapterMenu = false
                                    editor.insertTitleLine(label)
                                }
                            )
                        }
                        if (chapterNames.size > 1) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "A chapter of its own",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = WritingFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    chapterMenu = false
                                    editor.insertTitleLine()
                                }
                            )
                        }
                        if (chapterNames.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Chapter marker",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = WritingFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    chapterMenu = false
                                    editor.insertTitleLine()
                                }
                            )
                        }
                    }
                }
            }
        }

        // The dock steps aside for a live recording — a voice note is not a
        // moment for bold — and the capsule takes the page's bottom.
        AnimatedVisibility(
            visible = editing && liveVoice == null,
            enter = slideInVertically(tween(220)) { height -> height / 2 } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(160)) { height -> height / 2 } + fadeOut(tween(120)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                PersonalToolDock(
                    state = editor,
                    onPickPhoto = { photoPicker.launch(arrayOf("image/*")) }
                )
            }
        }

        AnimatedVisibility(
            visible = editing && liveVoice != null,
            enter = slideInVertically(tween(220)) { height -> height / 2 } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(160)) { height -> height / 2 } + fadeOut(tween(120)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val live = liveVoice
                if (live != null) {
                    PersonalVoiceRecorderCapsule(
                        session = live,
                        onKeep = {
                            scope.launch {
                                val voice = PersonalVoiceRecording.keep(context, voiceId)
                                if (voice != null) editor.insertVoice(voice)
                            }
                        },
                        onDiscard = { PersonalVoiceRecording.discard() }
                    )
                }
            }
        }
    }

    // The system's back gesture is guarded the same way the page's own is.
    BackHandler(enabled = liveVoice != null) { leave() }

    if (leavePrompt) {
        PersonalVoiceLeaveDialog(
            elapsed = liveVoice?.elapsed ?: "",
            onKeepRecording = { leavePrompt = false },
            onKeepNote = {
                leavePrompt = false
                scope.launch {
                    val voice = PersonalVoiceRecording.keep(context, voiceId)
                    if (voice != null) editor.insertVoice(voice)
                    navController.popBackStack()
                }
            },
            onDiscard = {
                leavePrompt = false
                PersonalVoiceRecording.discard()
                navController.popBackStack()
            }
        )
    }
}

/**
 * ONE CHAPTER MARKER LINE, and where it sits in the page's scrolling content
 * (see [BookReviewScreen]'s pinned chapter).
 */
private data class PinnedChapterLine(val label: String, val top: Float, val bottom: Float)

/**
 * v389 — A CHAPTER'S NAME, WITH THE NUMBER SAID ONCE.
 *
 * A book's own contents very often already say which chapter it is —
 * "Chapter 7", "7. The Fall", "Ch. VII" — so prefixing our own number produced
 * "Chapter 7 · Chapter 7" and, on a chapter the edition never named,
 * "Chapter 1 Chapter 1" (user report: "it automaticaly says hapter 1 and
 * sometimes the chapter name dont have names so it says chapter 1 athen it comes
 * chapter 1 chater 1 so fix that, add auto detetct something when chapter name
 * contains chapter 1 or ch etc then it doesnt say it again").
 *
 * So the name is CLEANED first: a leading chapter word with or without a
 * number, or a leading bare number, is taken off — but ONLY when it says the
 * number we were about to write. A heading that says a different chapter is the
 * book's own words and is left exactly as the book wrote it.
 */
internal fun chapterMarkerLabel(index: Int, name: String): String {
    val cleaned = chapterNameOnly(index, name)
    return if (cleaned.startsWith("Chapter ")) cleaned else "Chapter ${index + 1} \u00b7 $cleaned"
}

/** Just the name a chapter goes by: the book's own title with our number taken
 *  back out of it, or "Chapter N" when the edition never named it. */
internal fun chapterNameOnly(index: Int, name: String): String {
    val cleaned = stripChapterPrefix(name.trim(), index + 1)
    return cleaned.ifBlank { "Chapter ${index + 1}" }
}

private val CHAPTER_WORD = Regex(
    "^\\s*(?:chapter|chap|ch|book|part|section|\u00a7)\\.?\\s*([0-9]+|[IVXLCDMivxlcdm]+)?\\s*[\\-\u2013\u2014.:\u00b7)]?\\s*",
    RegexOption.IGNORE_CASE
)

private val CHAPTER_BARE_NUMBER = Regex("^\\s*([0-9]+)\\s*[\\-\u2013\u2014.:\u00b7)]\\s*")

private fun stripChapterPrefix(name: String, number: Int): String {
    if (name.isEmpty()) return ""
    CHAPTER_WORD.find(name)?.let { match ->
        val said = match.groupValues.getOrNull(1).orEmpty()
        val same = said.isBlank() || said.toIntOrNull() == number || romanValue(said) == number
        // Only OUR number is a duplicate. "Chapter 9" standing over chapter 7 is
        // the book's own heading and stays whole.
        if (same) return name.substring(match.range.last + 1).trim()
        return name
    }
    CHAPTER_BARE_NUMBER.find(name)?.let { match ->
        if (match.groupValues[1].toIntOrNull() == number) {
            return name.substring(match.range.last + 1).trim()
        }
    }
    return name
}

/** "VII" -> 7, "" for anything that is not a numeral. */
private fun romanValue(text: String): Int {
    if (text.isEmpty()) return -1
    var total = 0
    var previous = 0
    text.uppercase().reversed().forEach { ch ->
        val value = when (ch) {
            'I' -> 1
            'V' -> 5
            'X' -> 10
            'L' -> 50
            'C' -> 100
            'D' -> 500
            'M' -> 1000
            else -> return -1
        }
        if (value < previous) total -= value else total += value
        previous = value
    }
    return total
}

/**
 * A chapter's own review, folded under the marker that names it. It is drawn as
 * a quieter card, because it is somebody else's page: the member's own words
 * are the review around it, and this is what they wrote when they finished
 * that chapter.
 */
@Composable
private fun FoldedChapterReview(
    chapter: Int,
    name: String,
    doc: PersonalDoc,
    ink: Color,
    accent: Color,
    onOpenPhoto: (String, androidx.compose.ui.geometry.Rect?) -> Unit
) {
    if (doc.isEmpty) return
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
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
                    chapterNameOnly(chapter - 1, name).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp
                    ),
                    color = personalIconTint(accent)
                )
            }
            Spacer(Modifier.height(8.dp))
            PersonalDocView(
                doc = doc,
                ink = ink,
                accent = accent,
                onOpenPhoto = onOpenPhoto
            )
        }
    }
}

/** The blank page, with ONE way forward. */
@Composable
private fun BookReviewEmpty(
    accent: Color,
    ink: Color,
    onWrite: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = accent.copy(alpha = 0.12f),
            modifier = Modifier.size(54.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(
                    CurioIcons.MenuBook,
                    null,
                    tint = personalIconTint(accent),
                    size = 24.dp
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Nothing written about this book yet",
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "The whole book at once \u2014 and if you want them, drop a chapter marker in as you go.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = WritingFontFamily,
                fontSize = 14.sp
            ),
            color = ink.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            onClick = onWrite,
            shape = RoundedCornerShape(50),
            color = accent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                CurioIcon(CurioIcons.Note, null, tint = personalOnAccent(), size = 17.dp)
                Text(
                    "Write the review",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = personalOnAccent()
                )
            }
        }
    }
}

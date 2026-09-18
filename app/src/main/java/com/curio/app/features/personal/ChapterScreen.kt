package com.curio.app.features.personal

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.WritingFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ONE CHAPTER OF A BOOK — its own page, like a journal day.
 *
 * Writing a chapter review used to happen INSIDE the shelf's chapter list: the
 * row expanded and a whole writing canvas grew in the middle of the list, with
 * the tool dock pinned under a scrolling column of other chapters. This page
 * is the journal's own shape instead — the chapter's name, what the app knows
 * about it, and the review you wrote — and the writing happens HERE, not in
 * the list:
 *
 *  · READ BY DEFAULT: the review is the page, previews and all. No mood row
 *    (a book has no mood) and no format dock — nothing on screen that is only
 *    useful while typing.
 *  · WRITE ON TAP: the same page becomes the canvas (the dock slides up from
 *    the keyboard, the read chrome steps out), and tapping Done brings the
 *    reading view back. Nothing is a separate screen, and nothing auto-saves
 *    as a "save" button: it persists while you type, when the app leaves the
 *    foreground, and on the way out.
 *
 * The chapter notes written on the topic page's book sheet are the SAME rows
 * (see ChapterNoteBridge), so a note taken while reading the topic appears
 * here as this chapter's review.
 */
@Composable
fun ChapterScreen(
    navController: NavController,
    bookId: String,
    chapter: Int,
    // The chapter's own photo viewer (see PersonalPhotoOverlay) — handed in by
    // the route that hosts this page.
    photos: PersonalPhotoOverlayState = rememberPersonalPhotoOverlayState()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    val review = notes.firstOrNull { it.chapterIndex == chapter }
    var notesLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(notes) { notesLoaded = true }

    // The chapter's own record: its real name, the pages it spans and the
    // one-line summary — Curio's catalog when the book came from the lane,
    // else the table of contents the shelf read for it (BookEnrichment).
    val chapterMeta = rememberBookChapters(book).getOrNull(chapter - 1)

    var editing by remember(chapter) { mutableStateOf(false) }
    var draft by remember(chapter) { mutableStateOf(PersonalDoc(emptyList())) }
    var seeded by remember(chapter) { mutableStateOf(false) }
    val editor = remember(chapter) { PersonalEditorState(PersonalDoc(emptyList())) }
    SideEffect {
        editor.onDocChanged = { updated -> draft = updated }
    }

    // Entering the canvas seeds it from the stored review exactly once per
    // entry (re-seeding mid-typing is what drops the caret).
    LaunchedEffect(editing, review, notesLoaded) {
        if (!editing || seeded || !notesLoaded) return@LaunchedEffect
        val document = review?.doc ?: PersonalDoc(emptyList())
        draft = document
        editor.replace(document)
        seeded = true
    }

    val liveDraft = rememberUpdatedState(draft)

    /**
     * v389d — AN EMPTIED CHAPTER NOTE IS AN EDIT (see the book review's own):
     * deleting every word and leaving must clear what is stored, not be skipped
     * because there is now nothing to write.
     */
    fun shouldWrite(): Boolean =
        !liveDraft.value.isEmpty || review?.doc?.isEmpty == false

    fun saveNow() {
        val body = liveDraft.value
        if (!shouldWrite()) return
        val title = chapterMeta?.title.orEmpty()
        scope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                runCatching {
                    PersonalRepositoryHolder.repo.saveChapterNote(bookId, chapter, body, title)
                }
            }
        }
    }

    // Debounced while typing…
    LaunchedEffect(editing, draft) {
        if (!editing || !shouldWrite()) return@LaunchedEffect
        delay(700)
        withContext(Dispatchers.IO) {
            runCatching {
                PersonalRepositoryHolder.repo.saveChapterNote(
                    bookId, chapter, draft, chapterMeta?.title.orEmpty()
                )
            }
        }
    }

    // …and flushed when the app leaves the foreground: an app switch must
    // never be able to lose the page.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, editing) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && editing) saveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

    val ink = MaterialTheme.colorScheme.onBackground
    val accent = personalAccent()
    val chapterName = chapterMeta?.title.orEmpty()
    val words = remember(review?.id, review?.updatedAtMillis) { review?.doc?.wordsLabel().orEmpty() }

    // ── The page's own mic (v389) ──────────────────────────────────────
    // A chapter review is its own layout, so it carries the family's mic by hand
    // (see PersonalVoiceMic) — the same floating button, permission door and
    // guards the journal has.
    val voiceId = rememberSaveable(bookId, chapter) { "chapter-" + bookId + "-" + chapter }
    val liveVoice = PersonalVoiceRecording.session?.takeIf { it.noteId == voiceId }
    var leavePrompt by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // ── Header: back · which chapter · write/done ────────────────────
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
                    if (liveVoice != null) {
                        leavePrompt = true
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
                    text = if (chapterName.isNotBlank()) chapterName else "Chapter $chapter",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        book?.title?.takeIf { it.isNotBlank() },
                        "Chapter $chapter",
                        chapterMeta
                            ?.takeIf { it.pageStart > 0 && it.pageEnd > 0 }
                            ?.let { "pp. ${it.pageStart}\u2013${it.pageEnd}" }
                    ).joinToString(" \u00b7 "),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // THE FAMILY'S EYE/PEN (user request). The text pill it replaces
            // was this page's only "Done", so leaving the pen still SAVES
            // first — a chapter review must never leave writing unwritten.
            PersonalModeSwitch(
                editing = editing,
                onToggleMode = { writing ->
                    if (!writing) saveNow()
                    editing = writing
                }
            )
        }

        // Where this chapter sits in the book: a hairline rail with one mark,
        // so the page always knows how much of the book surrounds it.
        ChapterRail(
            chapter = chapter,
            total = if ((book?.totalChapters ?: 0) > 0) book?.totalChapters ?: 0
            else rememberBookChapters(book).size,
            accent = accent,
            ink = ink
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Crossfade(
            targetState = editing,
            animationSpec = tween(220),
            label = "chapter-read-write",
            modifier = Modifier.fillMaxSize()
        ) { writing ->             if (writing) {
                 Column(
                     modifier = Modifier
                         .fillMaxSize()
                         .verticalScroll(rememberScrollState())
                         // v389 — the blank part of the review is writing space:
                         // a tap in the gaps hands the caret to the LAST line and
                         // the keyboard follows (user report: "i tap the blank
                         // space to write but the cursor doesnt start and my
                         // keyboard too"). The journal's writing column has done
                         // this since v389; this page had not.
                         .clickable { editor.focusLastLine() }
                         .padding(horizontal = 20.dp)
                         .widthIn(max = 680.dp)
                 ) {
                     Spacer(Modifier.height(4.dp))
                     PersonalCanvas(
                        state = editor,
                        modifier = Modifier.fillMaxWidth(),
                        accent = accent,
                        onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
                    )
                    Spacer(Modifier.height(140.dp))
                }
            } else {
                // A DOUBLE TAP ON THE READING SIDE hands the pen back (user
                // request: "when im on eye view and i double tap switch to edit
                // pen mode, for all"). The detector sits UNDER the view, so a
                // child that consumes its own tap keeps it.
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
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .widthIn(max = 680.dp)
                ) {
                    if (!chapterMeta?.summary.isNullOrBlank()) {
                        ChapterSummaryCard(
                            summary = chapterMeta?.summary.orEmpty(),
                            ink = ink,
                            accent = accent
                        )
                        Spacer(Modifier.height(18.dp))
                    }
                    val document = review?.doc
                    if (document == null || document.isEmpty) {
                        NothingWrittenYet(accent = accent, ink = ink) {
                            editing = true
                        }
                    } else {
                        PersonalDocView(
                            doc = document,
                            accent = accent,
                            onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
                        )
                        if (words.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                words,
                                style = MaterialTheme.typography.labelSmall,
                                color = ink.copy(alpha = 0.45f)
                            )
                        }
                    }
                    Spacer(Modifier.height(120.dp))
                }
                }
            }
        }

            // THE PAGE'S OWN MIC, floating over the writing (no Add-chapter
            // door lives on this page, so it takes the corner the journal's own
            // mic takes).
            PersonalFloatingLayer(
                visible = editing && liveVoice == null,
                enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.80f),
                exit = fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.80f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 16.dp)
            ) {
                PersonalVoiceMic(
                    entryId = voiceId,
                    route = CurioRoutes.chapter(bookId, chapter)
                )
            }
        }

        // The dock is a WRITING tool, so it only exists while writing — it
        // rises with the keyboard instead of standing over the reading view.
        // A live recording REPLACES it (see PersonalVoice).
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
    BackHandler(enabled = liveVoice != null) { leavePrompt = true }

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

/** What the app knows about this chapter — the catalog's own words. */
@Composable
private fun ChapterSummaryCard(
    summary: String,
    ink: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color
) {
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
                    "ABOUT THIS CHAPTER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = personalIconTint(accent)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                summary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = WritingFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 25.sp
                ),
                color = ink.copy(alpha = 0.85f)
            )
        }
    }
}

/** The book's spine, with this chapter marked on it. */
@Composable
private fun ChapterRail(
    chapter: Int,
    total: Int,
    accent: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color
) {
    if (total <= 0) return
    val fraction = (chapter.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(50))
            .background(ink.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(2.dp)
                .clip(RoundedCornerShape(50))
                .background(accent)
        )
    }
}

/** The blank page, with ONE way forward. */
@Composable
private fun NothingWrittenYet(
    accent: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
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
                    CurioIcons.Note,
                    null,
                    tint = personalIconTint(accent),
                    size = 24.dp
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Nothing written for this chapter yet",
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "What stayed with you, what you want to remember when you read it again.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = WritingFontFamily,
                fontSize = 14.sp
            ),
            color = ink.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
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
                    "Write a review",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = personalOnAccent()
                )
            }
        }
    }
}

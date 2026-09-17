package com.curio.app.features.personal

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
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
 *    the ordinary dock cannot offer: **Add chapter**. It drops a title-style
 *    marker line at the caret, named as the member types it ([insertTitleLine]),
 *    which is all a chapter marker is — a heading in their own review.
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

    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(PersonalDoc(emptyList())) }
    var seeded by remember { mutableStateOf(false) }
    val editor = remember { PersonalEditorState(PersonalDoc(emptyList())) }
    SideEffect {
        editor.onDocChanged = { updated -> draft = updated }
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

    fun saveNow() {
        val body = liveDraft.value
        if (body.isEmpty) return
        scope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                runCatching { PersonalRepositoryHolder.repo.saveBookReview(bookId, body) }
            }
        }
    }

    // Debounced while typing…
    LaunchedEffect(editing, draft) {
        if (!editing || draft.isEmpty) return@LaunchedEffect
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
            if (!body.isEmpty) {
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
                    if (editing) {
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
            Surface(
                onClick = {
                    if (editing) {
                        saveNow()
                        editing = false
                    } else {
                        editing = true
                    }
                },
                shape = RoundedCornerShape(50),
                color = if (editing) accent.copy(alpha = 0.16f) else accent
            ) {
                Text(
                    text = if (editing) "Done" else if (review == null) "Write" else "Edit",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (editing) personalIconTint(accent) else personalOnAccent(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
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
                            .verticalScroll(rememberScrollState())
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
                        Spacer(Modifier.height(160.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
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

            // THE FLOATING CHAPTER DOOR. It rides over the writing area, above
            // the dock, and it is the one thing the ordinary tool dock cannot
            // give: a marker in the member's own review.
            AnimatedVisibility(
                visible = editing,
                enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 2 },
                exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 10.dp)
            ) {
                Surface(
                    onClick = { editor.insertTitleLine() },
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
            }
        }

        AnimatedVisibility(
            visible = editing,
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
    }
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
                    (name.ifBlank { "Chapter $chapter" }).uppercase(),
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

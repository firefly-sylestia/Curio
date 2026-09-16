package com.curio.app.features.personal

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newNoteId
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.LightboxTarget
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
    var notesLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(notes) { notesLoaded = true }

    val scope = rememberCoroutineScope()

    // Which chapter is open for writing (one at a time — the page keeps a
    // single tool dock, so only one writer can be live).
    var expanded by remember { mutableStateOf<Int?>(null) }
    var draft by remember { mutableStateOf(PersonalDoc(emptyList())) }
    val activeState = remember(expanded) {
        expanded?.let { PersonalEditorState(PersonalDoc(emptyList())) }
    }
    SideEffect {
        activeState?.onDocChanged = { updated -> draft = updated }
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

    // Load the chapter's existing review into the editor when it opens.
    var loadedChapter by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(expanded, notesLoaded, notes) {
        val chapter = expanded ?: return@LaunchedEffect
        if (!notesLoaded) return@LaunchedEffect
        if (loadedChapter == chapter) return@LaunchedEffect
        val existing = notes.firstOrNull { it.chapterIndex == chapter }
        val document = existing?.doc ?: PersonalDoc(emptyList())
        draft = document
        activeState?.replace(document)
        loadedChapter = chapter
    }

    // Auto-save the open chapter (debounced), and persist the book's note.
    val liveDraft = rememberUpdatedState(draft)
    val liveExpanded = rememberUpdatedState(expanded)
    val liveNotes = rememberUpdatedState(notes)
    LaunchedEffect(expanded, draft) {
        val chapter = expanded ?: return@LaunchedEffect
        if (liveDraft.value.isEmpty) return@LaunchedEffect
        delay(700)
        withContext(Dispatchers.IO) {
            runCatching { saveChapterReview(bookId, chapter, liveDraft.value, liveNotes.value) }
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
            .imePadding()
    ) {
        val current = book
        PersonalHeader(
            title = current?.title ?: " ",
            subtitle = current?.author.orEmpty().ifBlank { "Your book" },
            onBack = {
                // Leaving the page must never be able to drop what is open.
                val chapter = liveExpanded.value
                if (chapter != null && !liveDraft.value.isEmpty) {
                    val body = liveDraft.value
                    val known = liveNotes.value
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { saveChapterReview(bookId, chapter, body, known) }
                        }
                    }
                }
                navController.popBackStack()
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

        val total = current.totalChapters
        val writtenChapters = notes.mapNotNull { it.chapterIndex }.toSet()
        val highestWritten = writtenChapters.maxOrNull() ?: 0
        val chapterCount = maxOf(total, highestWritten, 1)

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
                    bookId = bookId,
                    chapter = chapter,
                    review = review,
                    isOpen = expanded == chapter,
                    openState = if (expanded == chapter) activeState else null,
                    onToggle = {
                        expanded = if (expanded == chapter) null else chapter
                        loadedChapter = null
                    },
                    onDelete = { pendingReviewDelete = review },
                    onOpenPhoto = { uri ->
                        LightboxTarget.uri = uri
                        navController.navigate(CurioRoutes.LIGHTBOX) { launchSingleTop = true }
                    }
                )
            }

            item("shelf-tail") { Spacer(Modifier.height(96.dp)) }
        }

        // The writing dock rides above the keyboard while a chapter is open.
        if (expanded != null && activeState != null) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                PhotoAndTools(state = activeState)
            }
        }
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
 * The chapter's tool dock, with the photo door: a chapter review lives in the
 * same canvas as a journal page, so a diagram, a page of the book or a
 * passage photographed at the desk can sit inside the writing.
 */
@Composable
private fun PhotoAndTools(state: PersonalEditorState) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            state.insertPhoto(uri.toString())
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PersonalToolDock(
            state = state,
            onPickPhoto = { picker.launch(arrayOf("image/*")) }
        )
    }
}

/** Saves (or refreshes) the review of one chapter. */
private suspend fun saveChapterReview(
    bookId: String,
    chapter: Int,
    document: PersonalDoc,
    existing: List<PersonalNoteEntity>
) {
    val match = existing.firstOrNull { it.chapterIndex == chapter }
    PersonalRepositoryHolder.repo.saveNote(
        PersonalNoteEntity(
            id = match?.id ?: newNoteId(),
            bookId = bookId,
            chapterIndex = chapter,
            title = "Chapter $chapter",
            bodyJson = com.curio.app.data.PersonalDocCodec.encode(document),
            preview = "",
            dateMillis = match?.dateMillis ?: startOfToday(),
            mood = "",
            chapterTitle = "",
            createdAtMillis = match?.createdAtMillis ?: 0L,
            updatedAtMillis = 0L
        )
    )
    // Writing about a chapter is moving through the book: progress follows the
    // writing, never the other way round.
    val book = PersonalRepositoryHolder.repo.book(bookId) ?: return
    if (chapter > book.currentChapter) {
        PersonalRepositoryHolder.repo.setProgress(bookId, chapter)
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
    val accent = MaterialTheme.colorScheme.primary
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
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            "Mark finished",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (total <= 0) 0f else (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = accent.copy(alpha = 0.16f)
            )
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
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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
    bookId: String,
    chapter: Int,
    review: PersonalNoteEntity?,
    isOpen: Boolean,
    openState: PersonalEditorState?,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onOpenPhoto: (String) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    // `doc` re-parses the stored body on every access — read it once per
    // version of the review, never per recomposition.
    val words = remember(review?.id, review?.updatedAtMillis) { review?.doc?.wordsLabel().orEmpty() }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isOpen) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (review != null) accent.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            chapter.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (review != null) accent else ink.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Chapter $chapter",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ink
                    )
                    Text(
                        when {
                            review == null -> "Nothing written yet"
                            review.preview.isBlank() -> "Open to keep writing"
                            else -> words
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.copy(alpha = 0.5f)
                    )
                }
                if (review != null && !isOpen) {
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
                    onClick = onToggle,
                    shape = RoundedCornerShape(50),
                    color = if (isOpen) accent else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        if (isOpen) "Done" else if (review == null) "Write" else "Open",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isOpen) MaterialTheme.colorScheme.onPrimary else ink.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }

            if (!isOpen && review != null && review.preview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    review.preview,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LoraFontFamily),
                    color = ink.copy(alpha = 0.72f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isOpen && openState != null) {
                Spacer(Modifier.height(12.dp))
                PersonalCanvas(
                    state = openState,
                    modifier = Modifier.fillMaxWidth(),
                    onOpenPhoto = onOpenPhoto
                )
            }
        }
    }
}

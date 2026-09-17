package com.curio.app.features.personal

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.curio.app.data.PAGE_KIND_JOURNAL
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newNoteId
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
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
    val categoryId: String = ""
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
    header: @Composable (editing: Boolean, saving: Boolean, onEditing: (Boolean) -> Unit) -> Unit,
    readView: @Composable (doc: PersonalDoc) -> Unit,
    onLoaded: (PersonalNoteEntity) -> Unit = {},
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
    aboveCanvas: @Composable () -> Unit = {}
) {
    val isNew = entryIdArg == CurioRoutes.PERSONAL_NEW
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var entryId by remember { mutableStateOf(if (isNew) newNoteId() else entryIdArg) }
    var doc by remember { mutableStateOf(PersonalDoc(emptyList())) }
    // READ FIRST, write on request: a saved page OPENS as the page it is and
    // the pen switches the tools on. A brand new page has nothing to read, so
    // it opens with the pen already down — and so does a checklist.
    var editing by remember(entryId) { mutableStateOf(isNew || checklistFirst) }
    var loaded by remember { mutableStateOf(isNew) }
    var saving by remember { mutableStateOf(false) }
    var createdAt by remember { mutableLongStateOf(0L) }

    // The editor state is rebuilt ONCE per entry (never per keystroke — that
    // would drop the caret). A checklist page owns its rows' style: Enter at
    // the end of a row makes the next one a row too.
    val editor = remember(entryId) {
        PersonalEditorState(doc).also { it.keepsChecklistRows = checklistFirst }
    }
    SideEffect {
        editor.onDocChanged = { updated -> doc = updated }
    }

    // ── Load the page ──────────────────────────────────────────────────
    LaunchedEffect(entryId) {
        val existing = withContext(Dispatchers.IO) { PersonalRepositoryHolder.repo.note(entryId) }
        if (existing != null) {
            val decoded = existing.doc
            doc = decoded
            editor.replace(decoded)
            createdAt = existing.createdAtMillis
            onLoaded(existing)
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

    // The page's own view of the document (the to-do page's progress line).
    LaunchedEffect(doc) { onDoc(doc) }

    // ── Auto-save ──────────────────────────────────────────────────────
    // Everything that can change is held in `rememberUpdatedState` so the
    // writers below (a debounce inside composition, and an app-switch flush
    // outside it) always read the latest values instead of the ones they were
    // created with.
    val liveId = rememberUpdatedState(entryId)
    val liveDoc = rememberUpdatedState(doc)
    val liveCreatedAt = rememberUpdatedState(createdAt)
    val liveMeta = rememberUpdatedState(meta)

    fun saveNow() {
        val body = liveDoc.value
        val page = liveMeta.value()
        if (body.isEmpty && page.title.isBlank()) return
        saving = true
        scope.launch {
            val saved = withContext(Dispatchers.IO + NonCancellable) {
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
                        categoryId = page.categoryId
                    )
                )
            }
            entryId = saved.id
            createdAt = saved.createdAtMillis
            saving = false
        }
    }

    // Debounced: 700ms after the writer stops moving. `pageNow` is READ during
    // composition, so a changed title / mood / topic restarts the same clock a
    // typed word does.
    val pageNow = meta()
    LaunchedEffect(loaded, doc, pageNow) {
        if (!loaded) return@LaunchedEffect
        if (doc.isEmpty && pageNow.title.isBlank()) return@LaunchedEffect
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // The page's own header sits UNDER the status bar without this.
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
    ) {
        header(editing, saving, { mode -> editing = mode })

        Crossfade(
            targetState = editing,
            animationSpec = tween(220),
            label = "personal-page-mode",
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { writing ->
            if (!writing) {
                readView(doc)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .widthIn(max = 680.dp)
                ) {
                    Spacer(Modifier.height(6.dp))
                    aboveCanvas()
                    PersonalCanvas(
                        state = editor,
                        modifier = Modifier.fillMaxWidth(),
                        onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
                    )
                    Spacer(Modifier.height(140.dp))
                }
            }
        }

        // The dock rides the keyboard while the page is being WRITTEN and steps
        // out of the way while it is being read.
        AnimatedVisibility(
            visible = editing,
            enter = slideInVertically(tween(220)) { height -> height / 2 } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(160)) { height -> height / 2 } + fadeOut(tween(120)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                PersonalToolDock(
                    state = editor,
                    onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                    showJournalTools = showJournalTools,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
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
 * v389 — THE FAMILY'S READ/WRITE SWITCH.
 *
 * The eye hides the tools and stops the page being typed into; the pen hands the
 * writing back. Whichever is lit is the mode the page is IN. It lives here
 * because a journal day and a note on a topic both need it, and the two must not
 * disagree about which icon means which mode.
 */
@Composable
internal fun PersonalModeSwitch(
    editing: Boolean,
    onToggleMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PersonalModeButton(
                label = "Reading",
                active = !editing,
                onClick = { onToggleMode(false) }
            ) {
                EyeGlyph(active = !editing)
            }
            PersonalModeButton(
                label = "Writing",
                active = editing,
                onClick = { onToggleMode(true) }
            ) {
                CurioIcon(
                    CurioIcons.Edit,
                    null,
                    tint = if (editing) personalAccentInk()
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    size = 17.dp
                )
            }
        }
    }
}

/** One half of the switch. */
@Composable
private fun PersonalModeButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (active) accent.copy(alpha = 0.26f) else Color.Transparent,
        modifier = Modifier.size(34.dp)
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
 *  version and the reading mode is not a "hidden" state. */
@Composable
private fun EyeGlyph(active: Boolean) {
    val ink = if (active) personalAccentInk()
    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
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
        drawPath(path, color = ink, style = Stroke(width = stroke))
        drawCircle(color = ink, radius = h * 0.13f, center = Offset(w * 0.5f, h * 0.5f))
    }
}

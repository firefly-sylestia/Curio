package com.curio.app.features.personal

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.curio.app.data.PAGE_KIND_JOURNAL
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalMood
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newNoteId
import com.curio.app.data.wordCount
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * v387 — ONE JOURNAL DAY.
 *
 * The page is its own thing, deliberately unlike a saved capture: no topic,
 * no hero, no paper page. A date you can change, how the day felt, a title,
 * and the writing — with the tool dock riding above the keyboard so the
 * tools are under the thumb while the words stay in view.
 *
 * NOTHING here is a "save" button. The entry writes itself to its own store
 * (debounced while typing, again the moment the app leaves the foreground, and
 * once more on the way out), which is what "don't lose it mid app switch"
 * requires: an app switch never gets the chance to ask.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    navController: NavController,
    entryIdArg: String,
    // The page's own photo viewer. The route that hosts this screen draws it,
    // so a tapped picture grows out of the page instead of leaving it.
    photos: PersonalPhotoOverlayState = rememberPersonalPhotoOverlayState(),
    // Topic-note and to-do support: when the "+" sheet creates a note on a
    // topic or a to-do list, these arrive as route query params and ride along
    // into the saved entity.
    initialTopicId: String = "",
    initialTopicName: String = "",
    initialCategoryId: String = "",
    initialKind: String = PAGE_KIND_JOURNAL
) {
    val isNew = entryIdArg == CurioRoutes.PERSONAL_NEW
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val topicId = remember(initialTopicId) { mutableStateOf(initialTopicId) }
    val topicName = remember(initialTopicName) { mutableStateOf(initialTopicName) }
    val categoryId = remember(initialCategoryId) { mutableStateOf(initialCategoryId) }
    val pageKind = remember(initialKind) { mutableStateOf(initialKind) }

    var entryId by remember { mutableStateOf(if (isNew) newNoteId() else entryIdArg) }
    var doc by remember { mutableStateOf(PersonalDoc(emptyList())) }
    var title by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<PersonalMood?>(null) }
    // READ FIRST, write on request: a saved page OPENS as the page it is (the
    // date, the title, the writing) and the pen switches the tools on. A brand
    // new page has nothing to read, so it opens with the pen already down.
    var editing by remember(entryId) { mutableStateOf(isNew) }
    var dateMillis by remember { mutableLongStateOf(startOfToday()) }
    var createdAt by remember { mutableLongStateOf(0L) }
    var loaded by remember { mutableStateOf(isNew) }
    var saving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pickerForDate by remember { mutableLongStateOf(dateMillis) }

    // The editor state is rebuilt ONCE per entry (never per keystroke — that
    // would drop the caret, which is exactly the bug RichTextEditor's own
    // comment warns about).
    val editor = remember(entryId) { PersonalEditorState(doc) }
    // The canvas reports every change here; the debounce below persists them.
    SideEffect {
        editor.onDocChanged = { updated -> doc = updated }
    }

    // ── Load the day ───────────────────────────────────────────────────
    LaunchedEffect(entryId) {
        val existing = withContext(Dispatchers.IO) { PersonalRepositoryHolder.repo.note(entryId) }
        if (existing != null) {
            title = existing.title
            mood = existing.moodEnum
            createdAt = existing.createdAtMillis
            dateMillis = if (existing.dateMillis > 0L) existing.dateMillis else startOfToday()
            val decoded = existing.doc
            doc = decoded
            editor.replace(decoded)
        }
        loaded = true
    }

    // ── Auto-save ──────────────────────────────────────────────────────
    // Everything that can change is held in `rememberUpdatedState` so the
    // writers below (a debounce inside composition, and an app-switch flush
    // outside it) always read the latest values instead of the ones they were
    // created with.
    val liveId = rememberUpdatedState(entryId)
    val liveTitle = rememberUpdatedState(title)
    val liveMood = rememberUpdatedState(mood)
    val liveDate = rememberUpdatedState(dateMillis)
    val liveDoc = rememberUpdatedState(doc)
    val liveCreatedAt = rememberUpdatedState(createdAt)

    fun saveNow() {
        val body = liveDoc.value
        val titleNow = liveTitle.value
        if (body.isEmpty && titleNow.isBlank()) return
        saving = true
        scope.launch {
            val saved = withContext(Dispatchers.IO + NonCancellable) {
                PersonalRepositoryHolder.repo.saveNote(
                    PersonalNoteEntity(
                        id = liveId.value,
                        bookId = null,
                        chapterIndex = null,
                        title = titleNow.trim(),
                        bodyJson = com.curio.app.data.PersonalDocCodec.encode(body),
                        preview = "",
                        dateMillis = liveDate.value,
                        mood = liveMood.value?.key.orEmpty(),
                        createdAtMillis = liveCreatedAt.value,
                        updatedAtMillis = 0L,
                        kind = pageKind.value,
                        topicId = topicId.value,
                        topicName = topicName.value,
                        categoryId = categoryId.value
                    )
                )
            }
            entryId = saved.id
            createdAt = saved.createdAtMillis
            saving = false
        }
    }

    // Debounced: 700ms after the writer stops moving.
    LaunchedEffect(loaded, doc, title, mood, dateMillis) {
        if (!loaded) return@LaunchedEffect
        if (doc.isEmpty && title.isBlank()) return@LaunchedEffect
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
            // Persisted permission: a journal photo has to be readable in a
            // later session, not just until this screen closes.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // The page's own header sits UNDER the status bar without this:
            // every new personal page in the engine padded the bars and this
            // one only padded the bottom.
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
    ) {
        JournalTopBar(
            dateMillis = dateMillis,
            saving = saving,
            editing = editing,
            onToggleMode = { mode -> editing = mode },
            onBack = {
                saveNow()
                navController.popBackStack()
            },
            onShiftDate = { days ->
                dateMillis = shiftDay(dateMillis, days)
            },
            onPickDate = {
                pickerForDate = dateMillis
                showDatePicker = true
            }
        )

        Crossfade(
            targetState = editing,
            animationSpec = tween(220),
            label = "journal-mode",
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { writing ->
            if (!writing) {
                JournalReadView(
                    dateMillis = dateMillis,
                    title = title,
                    mood = mood,
                    doc = doc,
                    ink = ink,
                    accent = accent,
                    onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
                )
            } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .widthIn(max = 680.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            MoodSelector(selected = mood, onSelect = { mood = it }, ink = ink)
            Spacer(Modifier.height(18.dp))
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = false,
                maxLines = 3,
                textStyle = TextStyle(
                    fontFamily = FrauncesFontFamily,
                    fontSize = 27.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "Title this day",
                                style = TextStyle(
                                    fontFamily = FrauncesFontFamily,
                                    fontSize = 27.sp,
                                    lineHeight = 34.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ink.copy(alpha = 0.32f)
                                )
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(14.dp))
            PersonalCanvas(
                state = editor,
                modifier = Modifier.fillMaxWidth(),
                onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
            )
            Spacer(Modifier.height(140.dp))
        }
            }
        }

        // The dock rides the keyboard while the page is being WRITTEN and
        // steps out of the way while it is being read.
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
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = pickerForDate)
DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp,
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = personalAccent(),
                        contentColor = personalOnAccent()
                    )
                ) { Text("Move the entry") }
            },
            dismissButton = {
                androidx.compose.material3.Button(
                    onClick = { showDatePicker = false },
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = personalAccent().copy(alpha = 0.16f),
                        contentColor = personalAccentInk()
                    )
                ) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = personalAccentInk(),
                    selectedDayContentColor = personalOnAccent(),
                    todayDateBorderColor = personalAccentInk(),
                    todayContentColor = personalAccentInk()
                )
            )
        }
    }
}

@Composable
private fun JournalTopBar(
    dateMillis: Long,
    saving: Boolean,
    editing: Boolean,
    onToggleMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    onShiftDate: (Long) -> Unit,
    onPickDate: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = personalAccent()
    val today = dateMillis.toLocalDate() == LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(42.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(CurioIcons.ArrowBack, "Back", tint = ink, size = 20.dp)
            }
        }

        // The day sits just after the back button (not floating in the middle
        // of the bar) — it is the page's title, so it belongs to its head.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (editing) Surface(
                onClick = { onShiftDate(-1L) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(CurioIcons.ChevronLeft, "Previous day", tint = personalAccentInk(), size = 18.dp)
                }
            }
            Surface(
                onClick = if (editing) onPickDate else ({}),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CurioIcon(CurioIcons.CalendarToday, null, tint = personalAccentInk(), size = 15.dp)
                    Text(
                        if (today) "Today" else dateMillis.prettyDate(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
color = personalAccentInk()
                )
            }
        }
        if (editing) Surface(
                onClick = { onShiftDate(1L) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(CurioIcons.ChevronRight, "Next day", tint = personalAccentInk(), size = 18.dp)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // EYE or PEN: the eye hides the tools and stops the page being typed
        // into, the pen hands the writing back. Whichever is lit is the mode
        // the page is IN.
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ModeButton(label = "Reading", active = !editing, onClick = { onToggleMode(false) }) { EyeGlyph(active = !editing) }
                ModeButton(label = "Writing", active = editing, onClick = { onToggleMode(true) }) {
                    CurioIcon(
                        CurioIcons.Edit,
                        null,
                        tint = if (editing) personalAccentInk() else ink.copy(alpha = 0.55f),
                        size = 17.dp
                    )
                }
            }
        }

        // The save state, as a dot: alive while the page is being written to
        // its store, calm once it is safe. No sentence, nothing to read.
        val transition = rememberInfiniteTransition(label = "journal-saving")
        val pulse by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
            label = "journal-saving-pulse"
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(
                        color = if (saving) accent.copy(alpha = pulse) else ink.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (saving) "Saving" else "Saved",
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * HOW THE DAY FELT, as ONE button.
 *
 * Six glyphs standing on the page was a wall of icons over the writing the
 * member came here to do (user request: "make the mood just one button and it
 * expands on tap"). The pill names the mood — or offers one — and the six
 * chips unfold UNDER it when it is tapped, then fold away again once a mood
 * is picked.
 */
@Composable
private fun MoodSelector(
    selected: PersonalMood?,
    onSelect: (PersonalMood?) -> Unit,
    ink: Color
) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Surface(
            onClick = { open = !open },
            shape = RoundedCornerShape(50),
            color = if (selected != null) personalAccent().copy(alpha = 0.24f)
            else MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (selected != null) {
                    CurioIcon(
                        personalMoodGlyph(selected),
                        null,
                        tint = personalAccentInk(),
                        size = 17.dp
                    )
                }
                Text(
                    selected?.label ?: "How did the day feel?",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (selected != null) personalAccentInk() else ink.copy(alpha = 0.6f)
                )
                CurioIcon(
                    if (open) CurioIcons.KeyboardArrowUp else CurioIcons.KeyboardArrowDown,
                    null,
                    tint = if (selected != null) personalAccentInk() else ink.copy(alpha = 0.5f),
                    size = 18.dp
                )
            }
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(180)) + fadeIn(tween(140)),
            exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PersonalMood.entries.forEach { mood ->
                    val on = mood == selected
                    Surface(
                        onClick = {
                            onSelect(if (on) null else mood)
                            open = false
                        },
                        shape = RoundedCornerShape(50),
                        color = if (on) personalAccent() else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.height(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                personalMoodGlyph(mood),
                                mood.label,
                                tint = if (on) personalOnAccent() else personalAccentInk(),
                                size = 18.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One half of the eye/pen switch. */
@Composable
private fun ModeButton(label: String, active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
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
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.06f, h * 0.5f)
            cubicTo(w * 0.3f, h * 0.16f, w * 0.7f, h * 0.16f, w * 0.94f, h * 0.5f)
            cubicTo(w * 0.7f, h * 0.84f, w * 0.3f, h * 0.84f, w * 0.06f, h * 0.5f)
            close()
        }
        drawPath(path, color = ink, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
        drawCircle(color = ink, radius = h * 0.13f, center = Offset(w * 0.5f, h * 0.5f))
    }
}

/**
 * THE PAGE AS IT READS: the day, how it felt, the title and the writing — and
 * nothing that can be typed into. Opening a saved journal used to drop the
 * member straight into the editor (tools up, caret waiting) when what they
 * tapped was a page to READ.
 */
@Composable
private fun JournalReadView(
    dateMillis: Long,
    title: String,
    mood: PersonalMood?,
    doc: PersonalDoc,
    ink: Color,
    accent: Color,
    onOpenPhoto: (String, Rect?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .widthIn(max = 680.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            dateMillis.prettyDate(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            ),
            color = personalAccentInk()
        )
        if (mood != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurioIcon(personalMoodGlyph(mood), null, tint = ink.copy(alpha = 0.55f), size = 15.dp)
                Text(
                    mood.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            title.ifBlank { "Untitled day" },
            style = TextStyle(
                fontFamily = FrauncesFontFamily,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (title.isBlank()) ink.copy(alpha = 0.42f) else ink
            )
        )
        Spacer(Modifier.height(18.dp))
        if (doc.isEmpty) {
            Text(
                "Nothing written for this day yet — tap the pen to start.",
                style = TextStyle(
                    fontFamily = FrauncesFontFamily,
                    fontSize = 15.sp,
                    color = ink.copy(alpha = 0.5f)
                )
            )
        } else {
            PersonalDocView(doc = doc, accent = accent, onOpenPhoto = onOpenPhoto)
        }
        Spacer(Modifier.height(120.dp))
    }
}

/** The one place a mood becomes a glyph. */
internal fun personalMoodGlyph(mood: PersonalMood): String = when (mood) {
    PersonalMood.CALM -> CurioIcons.MoodCalm
    PersonalMood.HAPPY -> CurioIcons.MoodHappy
    PersonalMood.CURIOUS -> CurioIcons.MoodCurious
    PersonalMood.INSPIRED -> CurioIcons.MoodInspired
    PersonalMood.TIRED -> CurioIcons.MoodTired
    PersonalMood.HEAVY -> CurioIcons.MoodOverwhelmed
}

// ── Date helpers (local midnight based: a journal day is a calendar day) ──

internal fun startOfToday(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

internal fun shiftDay(millis: Long, days: Long): Long =
    millis.toLocalDate().plusDays(days).atStartOfDay(ZoneId.systemDefault())
        .toInstant().toEpochMilli()

internal fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/** "Wed, 16 September" — the journal's own date line. */
internal fun Long.prettyDate(): String =
    toLocalDate().format(DateTimeFormatter.ofPattern("EEE, d MMMM", Locale.getDefault()))

/** The word count shown on a list row / the shelf (never a hint, always a
 *  fact about what is on the page). */
internal fun PersonalDoc.wordsLabel(): String {
    val words = wordCount()
    return when {
        words == 0 -> "Empty page"
        words == 1 -> "1 word"
        else -> "$words words"
    }
}

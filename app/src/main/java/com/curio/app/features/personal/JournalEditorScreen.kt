package com.curio.app.features.personal

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalMood
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newNoteId
import com.curio.app.data.wordCount
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.LightboxTarget
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
fun JournalEditorScreen(navController: NavController, entryIdArg: String) {
    val isNew = entryIdArg == CurioRoutes.PERSONAL_NEW
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var entryId by remember { mutableStateOf(if (isNew) newNoteId() else entryIdArg) }
    var doc by remember { mutableStateOf(PersonalDoc(emptyList())) }
    var title by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<PersonalMood?>(null) }
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
                        updatedAtMillis = 0L
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // weight, not fillMaxSize: the tool dock below is the column's
                // last child, and the writing scrolls in the space left over
                // (so the dock always rides the keyboard).
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .widthIn(max = 680.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            MoodRow(selected = mood, onSelect = { mood = it }, accent = accent, ink = ink)
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
                onOpenPhoto = { uri ->
                    LightboxTarget.uri = uri
                    navController.navigate(CurioRoutes.LIGHTBOX) { launchSingleTop = true }
                }
            )
            Spacer(Modifier.height(120.dp))
        }

        // The dock sits directly above the keyboard (the column's imePadding
        // lifts it), so a tool is always one tap away while writing.
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = pickerForDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("Move the entry") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun JournalTopBar(
    dateMillis: Long,
    saving: Boolean,
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
        horizontalArrangement = Arrangement.SpaceBetween
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                onClick = { onShiftDate(-1L) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(CurioIcons.ChevronLeft, "Previous day", tint = ink.copy(alpha = 0.7f), size = 18.dp)
                }
            }
            Surface(
                onClick = onPickDate,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CurioIcon(CurioIcons.CalendarToday, null, tint = accent, size = 15.dp)
                    Text(
                        if (today) "Today" else dateMillis.prettyDate(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = ink
                    )
                }
            }
            Surface(
                onClick = { onShiftDate(1L) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(CurioIcons.ChevronRight, "Next day", tint = ink.copy(alpha = 0.7f), size = 18.dp)
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

@Composable
private fun MoodRow(
    selected: PersonalMood?,
    onSelect: (PersonalMood?) -> Unit,
    accent: Color,
    ink: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PersonalMood.entries.forEach { mood ->
            val on = mood == selected
            Surface(
                onClick = { onSelect(if (on) null else mood) },
                shape = RoundedCornerShape(50),
                color = if (on) accent else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.width(42.dp)
            ) {
                Box(
                    modifier = Modifier.height(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        personalMoodGlyph(mood),
                        mood.label,
                        tint = if (on) personalOnAccent() else ink.copy(alpha = 0.62f),
                        size = 18.dp
                    )
                }
            }
        }
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

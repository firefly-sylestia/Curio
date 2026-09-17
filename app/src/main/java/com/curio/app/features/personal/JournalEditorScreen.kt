package com.curio.app.features.personal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.PAGE_KIND_JOURNAL
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalMood
import com.curio.app.data.wordCount
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import androidx.navigation.NavController
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
 * v389 — THE PAGE IS ITS HEAD. Everything this screen draws is the journal's
 * OWN half (the date bar, the mood pill, the title, how the saved day reads);
 * the document, the dock, the auto-save and the app-switch flush live in
 * [PersonalWritingPage], which the note-on-a-topic page and the to-do page use
 * too. A page is therefore its head plus a body, and the three pages can no
 * longer drift apart in how they save — the thing a writing app must never get
 * wrong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    navController: NavController,
    entryIdArg: String,
    // The page's own photo viewer. The route that hosts this screen draws it,
    // so a tapped picture grows out of the page instead of leaving it.
    photos: PersonalPhotoOverlayState = rememberPersonalPhotoOverlayState()
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = personalAccent()

    // The page's own half of the row: the day it belongs to, how it felt, its
    // title. The document and every write path belong to the core.
    var title by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<PersonalMood?>(null) }
    var dateMillis by remember { mutableLongStateOf(startOfToday()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pickerForDate by remember { mutableLongStateOf(startOfToday()) }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController =
        androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    PersonalWritingPage(
        entryIdArg = entryIdArg,
        photos = photos,
        // The page's own way out (the core guards it: a live voice recording is
        // asked about before a back gesture can drop it — see PersonalVoice).
        onExit = { navController.popBackStack() },
        meta = {
            PersonalPageMeta(
                title = title,
                mood = mood?.key.orEmpty(),
                dateMillis = dateMillis,
                kind = PAGE_KIND_JOURNAL
            )
        },
        // A saved day arrives with its own head: the date it belongs to, how it
        // felt, its title.
        onLoaded = { existing ->
            title = existing.title
            mood = existing.moodEnum
            if (existing.dateMillis > 0L) dateMillis = existing.dateMillis
        },
        header = { editing, saving, onEditing, _ ->
            // A journal day's bar has no back button of its own (the date pill
            // takes that corner), so the guarded exit is the system's.
            JournalTopBar(
                dateMillis = dateMillis,
                saving = saving,
                editing = editing,
                onToggleMode = onEditing,
                onShiftDate = { days -> dateMillis = shiftDay(dateMillis, days) },
                onPickDate = {
                    pickerForDate = dateMillis
                    showDatePicker = true
                }
            )
            if (showDatePicker) {
                val pickerState =
                    rememberDatePickerState(initialSelectedDateMillis = pickerForDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    shape = RoundedCornerShape(12.dp),
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
                                containerColor = personalAccent().copy(alpha = 0.24f),
                                contentColor = personalAccentInk()
                            )
                        ) { Text("Cancel") }
                    }
                ) {
                    DatePicker(
                        state = pickerState,
                        // v389 — the selected day is the ACCENT's airy tone, not
                        // its deep ink: the deep shade made the picked day the
                        // darkest thing on the calendar and swallowed the
                        // numeral (user report: "the calendar selected date
                        // highlight is too dark"). The pairing is the same one
                        // every accent FILL in the app uses — the airy fill,
                        // the readable ink on it.
                        colors = DatePickerDefaults.colors(
                            selectedDayContainerColor = personalAccent(),
                            selectedDayContentColor = personalOnAccent(),
                            selectedYearContainerColor = personalAccent(),
                            selectedYearContentColor = personalOnAccent(),
                            todayDateBorderColor = personalAccent(),
                            todayContentColor = personalAccentInk()
                        )
                    )
                }
            }
        },
        readView = { doc ->
            JournalReadView(
                title = title,
                mood = mood,
                doc = doc,
                ink = ink,
                accent = accent,
                onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
            )
        },
        aboveCanvas = {
            // The caret lands in the title the moment the page opens for
            // writing, so the first thing to do is type (this block is only
            // composed in the writing mode, which is what makes that true).
            LaunchedEffect(Unit) {
                titleFocusRequester.requestFocus()
                keyboardController?.show()
            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
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
        }
    )
}

@Composable
private fun JournalTopBar(
    dateMillis: Long,
    saving: Boolean,
    editing: Boolean,
    onToggleMode: (Boolean) -> Unit,
    onShiftDate: (Long) -> Unit,
    onPickDate: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = personalAccent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
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
                    // v389 — the day MOVES when it changes (user report: "date
                    // switching isnt smooth"): a later day rises in and an
                    // earlier day drops in, so the arrow the thumb pressed and
                    // the direction the date travels agree. Vertical on purpose
                    // — the two dates are the same height, so the pill never has
                    // to resize while they swap.
                    AnimatedContent(
                        targetState = dateMillis,
                        transitionSpec = {
                            val forward = targetState > initialState
                            val enter = if (forward) 1 else -1
                            (
                                fadeIn(tween(200)) +
                                    slideInVertically(tween(240)) { height -> enter * height / 2 }
                                ) togetherWith (
                                fadeOut(tween(140)) +
                                    slideOutVertically(tween(180)) { height -> -enter * height / 2 }
                                )
                        },
                        label = "journal-date"
                    ) { millis ->
                        Text(
                            if (millis.toLocalDate() == LocalDate.now()) "Today" else millis.prettyDate(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = personalAccentInk()
                        )
                    }
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
        // the page is IN. Shared with the note page, so the two pages cannot
        // disagree about which icon means which mode (v389).
        PersonalModeSwitch(editing = editing, onToggleMode = onToggleMode)

        // The save state, as a dot: alive while the page is being written to
        // its store, calm once it is safe. No sentence, nothing to read.
        val transition = rememberInfiniteTransition(label = "journal-saving")
        val pulse by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
            label = "journal-saving-pulse"
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (saving) accent.copy(alpha = pulse) else ink.copy(alpha = 0.22f),
                    shape = CircleShape
                )
                .semantics { contentDescription = if (saving) "Saving" else "Saved" }
        )
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

// v389 — the eye/pen switch (ModeButton + EyeGlyph) moved to PersonalPage.kt
// as PersonalModeSwitch, so the journal page and the note page share ONE switch.

/**
 * THE PAGE AS IT READS: the day, how it felt, the title and the writing — and
 * nothing that can be typed into. Opening a saved journal used to drop the
 * member straight into the editor (tools up, caret waiting) when what they
 * tapped was a page to READ.
 */
@Composable
private fun JournalReadView(
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
        // v389 — the page used to open with the DAY, the mood, and then the day
        // again under them; the bar above already carries the date, so the head
        // of the page starts at what the member chose (user report).
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

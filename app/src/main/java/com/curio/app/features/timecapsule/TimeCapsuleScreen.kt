package com.curio.app.features.timecapsule

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.AudioStorageManager
import com.curio.app.data.CurioTimeCapsules
import com.curio.app.data.PersonalAudioBars
import com.curio.app.features.capture.AudioRecorder
import com.curio.app.features.personal.PersonalPagePhoto
import com.curio.app.features.personal.PersonalVoiceBar
import com.curio.app.features.personal.PersonalVoiceRecorderCapsule
import com.curio.app.features.personal.PersonalVoiceSession
import com.curio.app.features.personal.appPermissionSettingsIntent
import com.curio.app.features.personal.rememberRecordPermission
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.WaveformExtractor
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.PixelFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

/**
 * v496 — THE TIME CAPSULE'S WRITING PAGE, REBUILT (agenda §5.5).
 *
 * ⚠️ THIS REPLACES v493's brass-dial machine, which the member judged out whole:
 * *"the time capsule what the hell is that screen, so bad no way to customise the
 * date by day time hour minstes etc, with proper deck style digital clocl look
 * with physcial style, and that whole screen itself is really bad fully delete it
 * the old time capsule and make a new beautiful dynamic animated colorful material
 * app style screen with button etc"*.
 *
 * So the page is a CLOCK and a deck of cards rather than a machine:
 *
 *  - **A flip clock.** Each part of the promised moment — day, month, year, hour,
 *    minute — is a physical card on an opaque plate with a crease across its own
 *    middle, and a card TURNS OVER on its axis when its value changes: edge-on at
 *    the halfway point, back face-on with the new glyph. That is the "deck style
 *    digital clock look with physical style", and a tick fires on every turn.
 *  - **Every knob is a Material button.** Circular step buttons for a day, an
 *    hour, a quarter of an hour; Material chips for the usual distances; and
 *    Android's OWN Material date and time pickers behind two doors — because the
 *    member's complaint was precisely that they could not set the day, the time,
 *    the hour or the minute themselves.
 *  - **A colourful, drifting hero** — an animated gradient in the app's own
 *    accent family rather than a brass plate — and a filled Material button to
 *    seal, rather than a wax disc to press.
 *
 * The clock is REAL: [target] is a millisecond instant the member moves, the
 * promise line reads it live (one tick a second), and the store is asked to seal
 * FOR THAT INSTANT ([CurioTimeCapsules.sealAt]) — no five-span ladder and no
 * thirty-day months. The steppers clamp to the store's own floor, so the page can
 * never promise a moment the store would refuse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCapsuleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val floor = remember { CurioTimeCapsules.MIN_LEAD_MS }
    // v501 — SEVEN DAYS OUT BY DEFAULT, the member's own number (it was thirty,
    // which is a month of waiting for a note that was usually meant for next
    // week). A default is a STARTING POINT: the clock below is the member's.
    var target by rememberSaveable { mutableStateOf(System.currentTimeMillis() + 7L * DAY_MS) }
    var message by rememberSaveable { mutableStateOf("") }
    var dateOpen by remember { mutableStateOf(false) }
    var timeOpen by remember { mutableStateOf(false) }
    var sealedCapsule by remember { mutableStateOf<CurioTimeCapsules.Capsule?>(null) }
    // ⚠️ v501 — A REFUSED SEAL HAS TO SAY SO. The store refuses a blank letter and
    // a moment inside its floor, and this page used to drop that answer on the
    // floor: the button looked dead (the member's "the seal it option wasnt
    // working"). The refusal is written here and drawn under the button.
    var sealNote by remember { mutableStateOf("") }
    var nowTick by remember { mutableIntStateOf(0) }
    // ── v506 — THE DRAFT DOOR ────────────────────────────────────────────────
    // The member: *"and also it doesnt have draft dalog too"*. Leaving this page
    // used to throw a half-written letter away in silence, so: the page opens on the
    // draft if there is one, and it asks before it lets a letter go (see the dialog
    // at the tail of this composable).
    var draftReturned by remember { mutableStateOf(false) }
    var askDraft by remember { mutableStateOf(false) }

    // ── v502 — WHAT THE LETTER CARRIES BESIDES WORDS ─────────────────────────
    //
    // The member: *"and your letter place is bad too and it doesnt even have any
    // image or voive note option"*. A capsule is a letter for a later day, and a
    // letter can hold a picture and the writer's own voice — both are sealed WITH
    // it (see [CurioTimeCapsules.sealAt]) and both come back out on the day the
    // words do.
    var photo by remember { mutableStateOf("") }
    var voicePath by remember { mutableStateOf("") }
    var voiceSeconds by remember { mutableIntStateOf(0) }
    var voiceBars by remember { mutableStateOf("") }
    // ── v507 — AND THIS IS READ BELOW ITS OWN DECLARATIONS, NOT ABOVE THEM ────
    //
    // A letter on the page that the store has not taken yet: the page's way back
    // asks about it (see the dialog at the tail of this composable). Blank once the
    // letter is sealed, so the sealed screen's own back is a plain back.
    //
    // ⚠️ It used to sit at the top of this block and read `photo` and `voicePath`:
    // a Kotlin LOCAL is visible only from its own declaration onward, so both were
    // unresolved references and the whole page did not COMPILE — which is exactly
    // what the CI run on the v506 push reported (two errors, all four jobs). Every
    // local it reads (`message`, `photo`, `voicePath`, `sealedCapsule`) is declared
    // above this line now.
    val hasUnsealedWork = sealedCapsule == null &&
        (message.isNotBlank() || photo.isNotBlank() || voicePath.isNotBlank())
    // The live recording is owned HERE rather than through the journal's app-wide
    // recording singleton: a capsule is not a journal note, and taking that state
    // over would put a "still recording" pill on every other screen offering to
    // return to a note that does not exist.
    var recorder by remember { mutableStateOf<PersonalVoiceSession?>(null) }
    var recordIssue by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // The picture door. The grant taken here is PERSISTABLE on purpose: this URI
    // is read again on the day the capsule lands, long after the picker's own
    // grant would have expired (the journal's prints use the same pattern).
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val uri = uris.firstOrNull() ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        photo = uri.toString()
        sealNote = ""
        recordIssue = ""
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val askMic = rememberRecordPermission(
        onGranted = {
            val fresh = AudioRecorder(context.applicationContext)
            if (runCatching { fresh.start() }.isSuccess) {
                recorder = PersonalVoiceSession(
                    noteId = "capsule",
                    returnRoute = CurioRoutes.TIME_CAPSULE,
                    recorder = fresh
                )
                recordIssue = ""
                sealNote = ""
            } else {
                runCatching { fresh.release() }
                recordIssue = "The microphone is busy or unavailable. Try again in a moment."
            }
        },
        onDenied = {
            recordIssue =
                "Curio needs the microphone for a voice note. Android's own settings page for Curio is where that can be allowed."
        }
    )

    /**
     * KEEP THE RECORDING: the cache file the recorder wrote is copied into the
     * app's own audio store and its waveform extracted ONCE — the same two things
     * the journal does with a voice note (see `PersonalVoiceRecording.keep`), so a
     * capsule's recording is a first-class recording rather than a cache file with
     * a path in it. A copy also goes into the device's own music library under
     * `Your Journal Voices`, so an uninstall cannot take the member's voice away.
     */
    fun keepRecording(live: PersonalVoiceSession) {
        recorder = null
        scope.launch {
            val cache = runCatching { live.stop() }.getOrNull()
            if (cache == null) {
                recordIssue = "That recording could not be saved."
                return@launch
            }
            val secondsNow = live.seconds
            val saved = withContext(Dispatchers.IO) {
                val bars = WaveformExtractor.extract(cache, PersonalAudioBars.BAR_COUNT)
                    ?: FloatArray(PersonalAudioBars.BAR_COUNT) { 0.08f }
                val madeAt = System.currentTimeMillis()
                val persisted = runCatching {
                    AudioStorageManager.persistAudio(
                        context.applicationContext,
                        cache,
                        "capsule_$madeAt"
                    )
                }.getOrNull()
                val published = persisted?.let { result ->
                    runCatching {
                        AudioStorageManager.publishVoice(
                            context.applicationContext,
                            result.persistentPath,
                            madeAt
                        )
                    }.getOrNull()
                }
                if (persisted != null) runCatching { File(cache).delete() }
                if (published != null) {
                    runCatching {
                        AudioStorageManager.deleteAudio(
                            context.applicationContext,
                            persisted.persistentPath
                        )
                    }
                }
                Triple(
                    published ?: persisted?.persistentPath ?: cache,
                    secondsNow,
                    PersonalAudioBars.encode(bars)
                )
            }
            voicePath = saved.first
            voiceSeconds = saved.second
            voiceBars = saved.third
            sealNote = ""
            recordIssue = ""
        }
    }

    // The recording's own clock and meter, ticked while it runs.
    LaunchedEffect(recorder) {
        val live = recorder ?: return@LaunchedEffect
        while (true) {
            live.tick()
            delay(80)
        }
    }

    // The head's own count, and the app's one place that recomputes what is
    // waiting: the page can be the first thing opened after a return.
    LaunchedEffect(Unit) { CurioTimeCapsules.refresh(context) }
    // v506 — AND THE PAGE OPENS ON THE DRAFT, if there is one. Read ONCE, as the
    // page comes up: the draft is not Compose state (no other screen shows one), so
    // there is nothing to observe — and a draft is only ever written by this page.
    LaunchedEffect(Unit) {
        val draft = CurioTimeCapsules.draft(context) ?: return@LaunchedEffect
        message = draft.message
        photo = draft.photo
        voicePath = draft.voice
        voiceSeconds = draft.voiceSeconds
        voiceBars = draft.voiceBars
        // The clock only comes back if it is still legal — a draft kept for a week
        // can carry a moment that has since passed, and the page's own floor is what
        // the seal is checked against.
        if (draft.openAt > System.currentTimeMillis() + floor) target = draft.openAt
        draftReturned = true
    }
    // A live clock, so "lands in 3 hours" stays honest while the member sits on
    // the page. One tick a second is all the promise line needs.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            nowTick++
        }
    }

    fun shift(delta: Long) {
        val minAt = System.currentTimeMillis() + floor
        val next = target + delta
        target = if (next < minAt) minAt else next
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun setFromNow(months: Int) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, months)
        target = maxOf(cal.timeInMillis, System.currentTimeMillis() + floor)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val cal = remember(target, nowTick) { Calendar.getInstance().apply { timeInMillis = target } }
    val promise = remember(target) { promiseLabel(target) }
    val until = remember(target, nowTick) { CurioTimeCapsules.untilText(target) }
    val hours24 = remember { android.text.format.DateFormat.is24HourFormat(context) }

    // ⚠️ v504 — THE PICKERS WEAR THE PAGE'S OWN FACE TOO. They are Android's own
    // `DatePicker` / `TimePicker` (the member asked to set the day on a calendar
    // they already know), but they used to open in Material's default type over a
    // default container while the page behind them was a split-flap — the member's
    // *"the dialog screen doesnt have similiar stle too fix all"*. Each dialog is
    // wrapped in the page's own nested theme (the same `pixelTypography()` the page
    // body uses) and takes the app's own dialog container, so the picker reads as
    // part of the capsule rather than as a different app's window.

    if (dateOpen) {
        // Android's own Material calendar: the member asked to set the DAY, so
        // the day is set on the picker they already know.
        val picker = rememberDatePickerState(initialSelectedDateMillis = target)
        CapsuleDialogTheme {
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = picker.selectedDateMillis
                    if (picked != null) {
                        // ⚠️ v501 — THE PICKED DAY IS READ IN UTC AND WRITTEN IN
                        // THE MEMBER'S OWN ZONE (a real accuracy bug).
                        //
                        // `selectedDateMillis` is UTC MIDNIGHT of the day that was
                        // tapped. Reading that instant straight into a local
                        // calendar puts it on the evening of the day BEFORE
                        // wherever the member is west of Greenwich, so a capsule
                        // set for "the 12th" was promised the 11th, and the landing
                        // line was a day out — the member's "also lands in isnt
                        // accurate sometimes". The three date fields are taken from
                        // a UTC calendar and set on the local one; the hour and
                        // minute the member already chose are kept, so picking a
                        // day still never moves their time.
                        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = picked
                        }
                        val keep = Calendar.getInstance().apply { timeInMillis = target }
                        keep.set(Calendar.YEAR, utc.get(Calendar.YEAR))
                        keep.set(Calendar.MONTH, utc.get(Calendar.MONTH))
                        keep.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
                        keep.set(Calendar.SECOND, 0)
                        keep.set(Calendar.MILLISECOND, 0)
                        val minAt = System.currentTimeMillis() + floor
                        target = maxOf(keep.timeInMillis, minAt)
                    }
                    sealNote = ""
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    dateOpen = false
                }) { Text("Use this day") }
            },
            dismissButton = {
                TextButton(onClick = { dateOpen = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = picker, showModeToggle = true)
        }
        }
    }

    if (timeOpen) {
        // …and the same for the hour and minute.
        val picker = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = hours24
        )
        CapsuleDialogTheme {
        AlertDialog(
            onDismissRequest = { timeOpen = false },
            containerColor = curioDialogContainerColor(),
            confirmButton = {
                TextButton(onClick = {
                    val keep = Calendar.getInstance().apply { timeInMillis = target }
                    keep.set(Calendar.HOUR_OF_DAY, picker.hour)
                    keep.set(Calendar.MINUTE, picker.minute)
                    keep.set(Calendar.SECOND, 0)
                    keep.set(Calendar.MILLISECOND, 0)
                    val minAt = System.currentTimeMillis() + floor
                    target = maxOf(keep.timeInMillis, minAt)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sealNote = ""
                    timeOpen = false
                }) { Text("Use this time", color = scheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { timeOpen = false }) {
                    Text("Cancel", color = scheme.onSurfaceVariant)
                }
            },
            title = { Text("What time?", color = scheme.onSurface) },
            text = { TimePicker(state = picker) }
        )
        }
    }

    // v502 — THE WHOLE PAGE WEARS THE PIXEL FACE (the member: "i want digital
    // that clocl tyle font for the whole page"). VT323 ships one weight, so Compose
    // synthesises the bold requests, which is exactly the heavy phosphor look a
    // split-flap page wants. v504 — and the two pickers above wear it as well (see
    // `dialogTheme`), because they are part of this page rather than a second app.
    MaterialTheme(colorScheme = scheme, typography = pixelTypography()) {
        Box(modifier = Modifier.fillMaxSize().background(scheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 680.dp)
            ) {
                Spacer(Modifier.height(10.dp))
                // v501 — THE HEADER WEARS THE PAGE'S OWN LANGUAGE: one soft rectangle
                // holding the way back, the name of the page and the count of letters
                // already waiting, rather than a floating arrow beside a bare title.
                // v506 — and the way back ASKS first when a letter is on the page (see
                // [CapsuleDraftDialog]), which is what makes this arrow safe to press.
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        CapsuleCircleButton(
                            glyph = CurioIcons.ArrowBack,
                            label = "Back",
                            accent = accent,
                            onClick = { if (hasUnsealedWork) askDraft = true else onBack() }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Time capsule",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = scheme.onSurface
                            )
                            Text(
                                if (CurioTimeCapsules.sealedCountState > 0) {
                                    "${CurioTimeCapsules.sealedCountState} letters waiting"
                                } else {
                                    "A letter for your own future self"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))

                // v506 — a draft that came back says so, in one quiet line: a page
                // that had silently filled itself with yesterday's words would read
                // as a bug rather than as a kindness.
                if (draftReturned) {
                    Text(
                        "Your draft came back. It is still yours to change.",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                }

                CapsuleHero(accent = accent, until = until, promise = promise)
                Spacer(Modifier.height(20.dp))

                // ── THE CLOCK ─────────────────────────────────────────────────
                CapsuleClock(
                    cal = cal,
                    hours24 = hours24,
                    accent = accent,
                    onEditDay = { dateOpen = true },
                    onEditTime = { timeOpen = true },
                    onStepDays = { shift(it.toLong() * DAY_MS) },
                    onStepMinutes = { shift(it.toLong() * MINUTE_MS) }
                )
                Spacer(Modifier.height(16.dp))

                // ── AND THE USUAL DISTANCES, AS ONE TAP ───────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("A month" to 1, "3 months" to 3, "6 months" to 6, "A year" to 12)
                        .forEach { (label, months) ->
                            CapsulePresetChip(
                                label = label,
                                accent = accent,
                                modifier = Modifier.weight(1f)
                            ) { setFromNow(months) }
                        }
                }
                Spacer(Modifier.height(22.dp))

                // ── THE LETTER ────────────────────────────────────────────────
                Text(
                    "Your letter",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = scheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = {
                        Text(
                            "Write what you want to read on that day…",
                            // v504 — a placeholder is still writing: the page's own
                            // muted ink at full strength, not a washed copy of it.
                            color = scheme.onSurfaceVariant
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // ── AND WHAT THE LETTER CARRIES ───────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CapsuleAttachmentDoor(
                        glyph = CurioIcons.Image,
                        label = if (photo.isBlank()) "Add a picture" else "Change the picture",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) {
                        // A capsule is one letter, so one picture: the member can
                        // change it, and does not have to manage a gallery here.
                        photoPicker.launch(arrayOf("image/*"))
                    }
                    CapsuleAttachmentDoor(
                        glyph = CurioIcons.Mic,
                        label = if (voicePath.isBlank()) "Add your voice" else "Record again",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (recorder == null) askMic()
                        else recordIssue = "A recording is already running."
                    }
                }

                if (photo.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        PersonalPagePhoto(
                            uri = photo,
                            height = 190.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                        )
                        CapsuleAttachmentRemove(
                            label = "Remove the picture",
                            accent = accent,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) { photo = "" }
                    }
                }

                if (recorder != null) {
                    Spacer(Modifier.height(12.dp))
                    PersonalVoiceRecorderCapsule(
                        session = recorder!!,
                        onKeep = { recorder?.let { keepRecording(it) } },
                        onDiscard = {
                            val live = recorder
                            recorder = null
                            runCatching { live?.discard() }
                        }
                    )
                } else if (voicePath.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    PersonalVoiceBar(
                        path = voicePath,
                        seconds = voiceSeconds,
                        bars = voiceBars,
                        ink = scheme.onSurface,
                        accent = accent,
                        onRemove = {
                            voicePath = ""
                            voiceSeconds = 0
                            voiceBars = ""
                        }
                    )
                }

                if (recordIssue.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        recordIssue,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // A permission refused for good is only given back on Android's
                    // own page for this app (see [appPermissionSettingsIntent]).
                    val micSettings = remember(context) { appPermissionSettingsIntent(context) }
                    if (micSettings != null && recorder == null) {
                        Spacer(Modifier.height(8.dp))
                        CapsuleAttachmentDoor(
                            glyph = CurioIcons.Settings,
                            label = "Open Android's app settings",
                            accent = accent,
                            modifier = Modifier.fillMaxWidth()
                        ) { runCatching { context.startActivity(micSettings) } }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── v502 — AND SEALING IT IS A GESTURE, NOT A TAP ──────
                //
                // The member: *"and the optio sea it it lands in 1 2month that
                // option is bad make it like a way to user have to swipe to save"*.
                // A button says "press me"; a letter that will be sealed away for
                // months is not a button's job. The slug is DRAGGED along the
                // track to its end and the seal lands there; released early it
                // springs home and nothing is sealed, so a slip of the thumb
                // cannot send a letter into next year.
                val carriesSomething = message.isNotBlank() ||
                    photo.isNotBlank() || voicePath.isNotBlank()
                CapsuleSealSwipe(
                    enabled = carriesSomething,
                    accent = accent,
                    onSeal = {
                        val sealed = CurioTimeCapsules.sealAt(
                            context = context,
                            message = message,
                            openAt = target,
                            label = promise,
                            photo = photo,
                            voice = voicePath,
                            voiceSeconds = voiceSeconds,
                            voiceBars = voiceBars
                        )
                        if (sealed != null) {
                            // ⚠️ v506 — AND THE DRAFT GOES WITH IT. A sealed letter
                            // must never come back as a draft of itself, so the draft
                            // is cleared in the SAME branch that sealed it (the "Keep
                            // as draft" door writes, this one removes — one of them
                            // always wins, and neither can be forgotten).
                            CurioTimeCapsules.clearDraft(context)
                            draftReturned = false
                            sealNote = ""
                            sealedCapsule = sealed
                            true
                        } else {
                            // The store said no, so the page says why — and the track
                            // springs home so the member can fix the clock and slide
                            // again (a control that silently does nothing is the bug
                            // the member reported; a control that STAYS at the end of
                            // its travel having done nothing was the second one).
                            sealNote = if (!carriesSomething) {
                                "A capsule needs something to carry. A line, a picture, or your voice."
                            } else {
                                "That moment has already passed. Set the clock a little further on."
                            }
                            false
                        }
                    }
                )
                // WHY THE TRACK IS LOCKED, and why a seal was refused: one line,
                // drawn only when it has something to say.
                val note = sealNote.ifBlank {
                    if (!carriesSomething) {
                        "Add a line, a picture or a voice note, and the track unlocks."
                    } else ""
                }
                if (note.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── v504 — AND YOUR LETTERS, ON THE PAGE THEY BELONG TO ────────
                // Home's sealed-letters row pill is deleted (the member: *"remove
                // it and place it somewhere else"*, and their answer to where was
                // this page), and *"a way to see previous capsules"* is the same
                // surface: every letter ever sealed, waiting or already opened,
                // each one tapping open in place. The list is re-read on the
                // page's own second (it is cheap, and a letter written to yourself
                // yesterday should be here the moment you look).
                val archive = remember(nowTick, sealedCapsule) {
                    CurioTimeCapsules.all(context)
                }
                if (archive.isNotEmpty()) {
                    Spacer(Modifier.height(26.dp))
                    CapsuleArchive(
                        capsules = archive,
                        accent = accent,
                        now = System.currentTimeMillis(),
                        onOpened = { CurioTimeCapsules.open(context, it) }
                    )
                }
                Spacer(Modifier.height(28.dp))
            }

            // ── THE SEALED STATE ──────────────────────────────────────────────
            val done = sealedCapsule
            if (done != null) {
                // The page has nothing left to say once the letter is sealed: this is
                // the one thing on screen, and it says when it comes back.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.background)
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // v506 — the disc wears the pixel face's own mark at a size
                        // worth looking at: this is the one screen in the app that
                        // says a thing is DONE, and it was the plainest.
                        Surface(
                            shape = CircleShape,
                            color = accent,
                            modifier = Modifier.size(104.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CurioIcon(
                                    CurioIcons.Lock,
                                    null,
                                    tint = scheme.onPrimary,
                                    size = 40.dp
                                )
                            }
                        }
                        Text(
                            "Sealed",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = scheme.onBackground
                        )
                        Text(
                            // v497 — the promise label is the member's own date AND
                            // time now, so it is named as a day here rather than as
                            // an interval: "in 3 days — on 13 October 2026 at 09:30".
                            "Back to you ${CurioTimeCapsules.untilText(done.openAt)}, on ${done.spanLabel}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        // ── v506 — TWO DOORS, BECAUSE THERE ARE TWO THINGS TO DO ──
                        // "Done" used to be the only way off this screen. A capsule
                        // page is somewhere a member writes one letter and often
                        // writes another, so the second door clears the letter (and
                        // its attachments) and leaves the clock where it was: the
                        // next letter is a fresh page, not a copy of the last one.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = onBack,
                                shape = RoundedCornerShape(12.dp),
                                color = accent,
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "Done",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = scheme.onPrimary
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    message = ""
                                    photo = ""
                                    voicePath = ""
                                    voiceSeconds = 0
                                    voiceBars = ""
                                    sealNote = ""
                                    recordIssue = ""
                                    draftReturned = false
                                    sealedCapsule = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = capsuleWash(accent),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "Write another",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        // v507 — the door is a solid accent fill now, so
                                        // its word is [capsuleOn] rather than the page's
                                        // ink (which was dark-on-dark on a dark accent).
                                        color = capsuleOn(accent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── v506 — THE DRAFT DOOR ITSELF ────────────────────────────────────────
    //
    // The system back and the header's arrow both ask here when a letter is on the
    // page (the member: *"and also it doesnt have draft dalog too"*). Three answers,
    // and only one of them can lose the letter: "Keep as draft" writes it through
    // `CurioTimeCapsules.saveDraft`, "Throw it away" clears it, and "Keep writing"
    // closes the dialog and leaves the page exactly as it was. The dialog wears the
    // page's own face like the two pickers (see [CapsuleDialogTheme]).
    BackHandler(enabled = hasUnsealedWork) { askDraft = true }
    if (askDraft) {
        CapsuleDialogTheme {
            AlertDialog(
                onDismissRequest = { askDraft = false },
                containerColor = curioDialogContainerColor(),
                title = { Text("Keep this letter?", color = scheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "It is written but not sealed. A draft stays on this " +
                                "device and comes back the next time you open this page.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                CurioTimeCapsules.clearDraft(context)
                                askDraft = false
                                onBack()
                            }
                        ) {
                            Text("Throw it away", color = scheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            CurioTimeCapsules.saveDraft(
                                context,
                                CurioTimeCapsules.Draft(
                                    message = message,
                                    photo = photo,
                                    voice = voicePath,
                                    voiceSeconds = voiceSeconds,
                                    voiceBars = voiceBars,
                                    openAt = target
                                )
                            )
                            askDraft = false
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        }
                    ) { Text("Keep as draft", color = scheme.primary) }
                },
                dismissButton = {
                    TextButton(onClick = { askDraft = false }) {
                        Text("Keep writing", color = scheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

private const val DAY_MS = 24L * 60L * 60L * 1000L
private const val HOUR_MS = 60L * 60L * 1000L
private const val MINUTE_MS = 60L * 1000L

/**
 * v504 — THE PAGE'S OWN THEME, WRAPPED AROUND ITS TWO PICKERS.
 *
 * The writing page applies its pixel face with a nested `MaterialTheme` (see
 * [pixelTypography]), and the date and time dialogs sit ABOVE that in the
 * composition, so they used to open in Material's default type over the page that
 * had a split-flap on it — the member's *"the dialog screen doesnt have similiar
 * stle too fix all"*. A named composable rather than a lambda-typed value: this is a
 * theme wrapper, and it reads that way at both call sites.
 */
@Composable
private fun CapsuleDialogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = pixelTypography()
    ) { content() }
}

/**
 * WHERE THE LETTER IS GOING, AS A CARD OF LIGHT.
 *
 * An animated four-stop gradient in the app's own accent family — the page's one
 * colourful surface, and the only thing on it that moves on its own. It carries
 * the promise, because that is the fact the page exists to state.
 */
@Composable
private fun CapsuleHero(accent: Color, until: String, promise: String) {
    val scheme = MaterialTheme.colorScheme
    val drift = rememberInfiniteTransition(label = "capsuleHero")
    val spin by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "capsuleSpin"
    )
    // ⚠️ v501 — THE LIGHT GOES ROUND, SO IT NEVER SNAPS HOME (the member: "the
    // gradient animation isnt infinite like a dynamic abstract as it resets and
    // doesnt feel continuis").
    //
    // The hero used to slide its gradient sideways from shift 0 to shift 1 and
    // then JUMP back to 0 — every eleven seconds the light visibly started again.
    // The gradient's two ends now travel a full circle around the card's centre
    // instead: at the end of the loop they are exactly where they started, so the
    // last frame and the first frame are the same picture and there is nothing to
    // reset. The colours are also pulled toward each other (a softer, fluffier
    // spread) so the motion reads as one surface breathing rather than four
    // stripes swapping places.
    val angle = spin * 2f * Math.PI.toFloat()
    val dx = cos(angle) * 900f
    val dy = sin(angle) * 900f
    val far = lerp(accent, scheme.tertiary, 0.42f)
    val near = lerp(accent, scheme.secondary, 0.60f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent, far, near, accent),
                    start = Offset(-dx, -dy),
                    end = Offset(dx, dy)
                )
            )
    ) {
        // ── v507 — THE WORDS SIT ON A SOLID PLAQUE, NOT A TRANSLUCENT SCRIM ────
        //
        // The member: *"use solid filled colors no transparent colors anywhere"*.
        // The hero used to darken its own bottom with a black-to-transparent
        // gradient — a scrim, which is a colour with alpha in it, and one that made
        // the card's lower third look fogged rather than lit.
        //
        // The promise now sits on its own DEEP ACCENT PLAQUE instead: a solid slab
        // mixed 55% toward black from the page's own accent, so white on it is
        // readable whatever the palette's primary happens to be, and the moving
        // gradient stays exactly as it is everywhere the plaque is not.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // The disc is a SOLID white mark with the accent's own glyph in it —
            // the hero's badge, not a 22% white ghost of one.
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(CurioIcons.Lock, null, tint = accent, size = 22.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = lerp(accent, Color.Black, 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // v501 — THE PROMISE LEADS AND THE DISTANCE FOLLOWS: the date
                    // and time the letter is written for is the fact, and "in 7
                    // days" is how far away it is. It used to read the other way
                    // round ("Lands in 2 months" over the date), which is the
                    // member's "that option is bad make it like ... use better
                    // english".
                    Text(
                        promise,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                    Text(
                        "Back to you $until",
                        style = MaterialTheme.typography.labelLarge,
                        color = lerp(Color.White, lerp(accent, Color.Black, 0.55f), 0.18f)
                    )
                }
            }
        }
    }
}

/**
 * THE CLOCK ITSELF: the promised day on one deck of cards, the promised time on
 * the next row, and one circular button either side of each pair.
 *
 * The steppers are the deck's own manners — a step is a tick — and they CLAMP to
 * the store's floor, so a member holding "+15 min" against a moment five minutes
 * away stops at the floor instead of promising something the store would refuse.
 */
@Composable
private fun CapsuleClock(
    cal: Calendar,
    hours24: Boolean,
    accent: Color,
    onEditDay: () -> Unit,
    onEditTime: () -> Unit,
    onStepDays: (Int) -> Unit,
    onStepMinutes: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    // ── v507 — THE CLOCK HAS ITS OWN PALETTE, AND IT NO LONGER INVERTS ───────────
    //
    // The member: *"still the time capsule button colors etc is bad and sepeiclaly the
    // clock number colors"*. The old pair was a single inversion of the page in both
    // themes — `plate = lerp(surface, onSurface, 0.86)` — which happens to read as a
    // black deck with white digits on a LIGHT page and as a WHITE deck with black
    // digits on a DARK one. A flip clock is not whichever way the page happens to
    // lean; it is a dark plate with bright digits, always, which is what makes it read
    // as a machine on paper and as a machine in the dark.
    //
    // So both themes draw a DEEP plate (`0.86` of the way to the page's ink on a light
    // page, and the page's own surface lifted a touch on a dark one, because a page
    // that is already black needs a plate it can still be seen against) with BRIGHT
    // digits, and both carry a whisper of the page's accent: the plate a 6% tint, the
    // digits 18% — enough that the clock belongs to the page it sits on, nowhere near
    // enough to cost the contrast a number being read across a room needs.
    val dark = scheme.surface.luminance() <= 0.5f
    // The plate's own colour, before the accent is mixed in: the LIGHT page's ink on
    // a light page, and the dark page's own surface lifted just enough to be seen
    // against it on a dark one.
    val plateBase = if (dark) {
        lerp(scheme.surface, scheme.onSurface, 0.14f)
    } else {
        lerp(scheme.surface, scheme.onSurface, 0.86f)
    }
    val plate = lerp(plateBase, accent, 0.06f)
    // The digits: bright in both themes, toward the accent by a fifth.
    val digitBase = if (dark) {
        lerp(scheme.onSurface, scheme.surface, 0.04f)
    } else {
        lerp(scheme.onSurface, scheme.surface, 0.96f)
    }
    val ink = lerp(digitBase, accent, 0.18f)
    // ⚠️ v501 — THE CLOCK IS ITS OWN EDITOR (the member's "remove the plus and
    // arro sign, tapping it should let me edit it and with gesture edit with
    // scrolling", and "the pick a date pick a time feels duplicat).
    //
    // It used to be a deck of cards flanked by six circular step buttons with a
    // pair of "15 min earlier / 15 min later" bars below them and TWO separate
    // "Pick a date" / "Pick a time" doors underneath all of that. Three ways to
    // move one clock, none of them the thing a member's thumb goes to first. The
    // steppers and the doors are gone; the deck IS the control:
    //
    //   • tap the day row    → Android's own calendar
    //   • tap the time row   → Android's own time picker
    //   • drag either row up or down → that row's unit turns, one notch per
    //     thumb's width of travel, with a tick each notch
    //
    // The rows are centred with fixed card widths, so the two decks line up with
    // each other instead of being pushed off centre by a button either side.
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // v504 — THE CARDS SHARE THE ROW BY WEIGHT, NOT BY A FIXED WIDTH.
            //
            // Three fixed widths (84 + 132 + 112dp plus the gaps) came to more than
            // the page's own content width on a 360dp phone, so the deck ran past
            // its panel — which is what "some isnt properly aligned" was about, and
            // why the cards could not simply be made bigger. Weights give every card
            // a share of whatever the screen actually has, so the deck is as wide as
            // its panel on every size, and the cards are bigger for free.
            ClockRow(
                label = "The day it comes back",
                accent = accent,
                onTap = onEditDay,
                onStep = onStepDays
            ) {
                FlipCard(
                    value = flipDay(cal),
                    caption = "DAY",
                    plate = plate,
                    ink = ink,
                    modifier = Modifier.weight(1f)
                )
                FlipCard(
                    value = flipMonth(cal),
                    caption = "MONTH",
                    plate = plate,
                    ink = ink,
                    modifier = Modifier.weight(1.45f)
                )
                FlipCard(
                    value = flipYear(cal),
                    caption = "YEAR",
                    plate = plate,
                    ink = ink,
                    modifier = Modifier.weight(1.25f)
                )
            }

            ClockRow(
                label = "The hour it opens",
                accent = accent,
                onTap = onEditTime,
                onStep = onStepMinutes
            ) {
                FlipCard(
                    value = flipHour(cal, hours24),
                    caption = "HOUR",
                    plate = plate,
                    ink = ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = scheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                FlipCard(
                    value = flipMinute(cal),
                    caption = "MIN",
                    plate = plate,
                    ink = ink,
                    modifier = Modifier.weight(1f)
                )
                if (!hours24) {
                    FlipCard(
                        value = flipMarker(cal),
                        caption = "",
                        plate = plate,
                        ink = ink,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }
    }
}

/**
 * ONE DECK OF THE CLOCK, AND THE GESTURES THAT TURN IT.
 *
 * A tap opens the picker that owns this row's unit; a SIDEWAYS drag turns the deck
 * under the thumb, one notch each [NOTCH] of travel, with a tick at every notch and
 * a tick when the thumb lands.
 *
 * ⚠️ v504 — THE DRAG IS HORIZONTAL, AND THAT IS THE FIX FOR A REAL FAULT: this page
 * is a `verticalScroll` column, so a VERTICAL drag on the clock was fighting the
 * page's own scroll for the same gesture — and the scroll, being the ancestor, won
 * most of it: the member's *"the time adjust gesture is bad too"*. A sideways drag
 * on a deck is not a scroll the page wants, so there is nothing to lose it to. Left
 * takes the unit on, right takes it back — the deck travels the way the thumb pushes
 * it, which is the same rule the vertical version used, rotated.
 *
 * The press-detection pointer input sits BEFORE the click in the chain (see the
 * project's modifier-order rule): the drag consumes its own movement, so a real
 * drag never fires the tap, and a tap with no travel never turns a card.
 */
@Composable
private fun ClockRow(
    label: String,
    accent: Color,
    onTap: () -> Unit,
    onStep: (Int) -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val notch = with(density) { NOTCH.toPx() }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            // v504 — the row is a bigger target: a deck you drag has to be tall
            // enough to put a thumb on without covering the card you are watching.
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = scheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var travel = 0f
                        // ⚠️ v507 — ANY DIRECTION, AND A SHORTER NOTCH (the member,
                        // twice: *"the time adjust gesture is bad too"*, then *"the
                        // gesture to chnage isnt working"*). Two causes, both here:
                        //
                        //  · **the detector was HORIZONTAL-ONLY** — a thumb that set
                        //    off even slightly uphill was not recognised at all and
                        //    the page scrolled instead, which is exactly what "isnt
                        //    working" looks like from the outside. A plain drag
                        //    detector is what a deck wants: any direction counts, and
                        //    the row still lets a MOSTLY VERTICAL drag through to the
                        //    page (it only consumes what it turned).
                        //  · **one notch cost 34dp** of travel, more than a short
                        //    flick covers, so a small drag did nothing and felt
                        //    broken. It is 22dp now (see [NOTCH]).
                        //
                        // A drag is read on whichever axis is doing the work: left or
                        // up takes the unit on, right or down takes it back — the deck
                        // travels the way the thumb pushes it, in either direction.
                        detectDragGestures(
                            onDragStart = {
                                travel = 0f
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragEnd = { travel = 0f },
                            onDragCancel = { travel = 0f }
                        ) { change, drag ->
                            travel += if (kotlin.math.abs(drag.x) >= kotlin.math.abs(drag.y)) {
                                drag.x
                            } else {
                                -drag.y
                            }
                            val notches = (travel / notch).toInt()
                            if (notches != 0) {
                                travel -= notches * notch
                                onStep(notches)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                // The movement is spent, so nothing downstream (the
                                // page's own scroll) acts on it as well.
                                change.consume()
                            }
                        }
                    }
                    .clickable(
                        onClickLabel = if (label.startsWith("The day")) "Set the day"
                        else "Set the hour and minute",
                        onClick = onTap
                    ),
                content = content
            )
        }
    }
}

/**
 * ── v507 — THE ONE FILL EVERY DOOR ON THIS PAGE WEARS, AND IT IS SOLID ───────
 *
 * A function, not a plain colour, because it is read inside a composable and the
 * number lives in one place. The doors used to be `lerp(surface, accent, 0.18)` at
 * five different strengths — the member's *"still the time capsule button colors
 * etc is bad"*, and then, plainly, *"more colorful buttom and use solid filled
 * colors no transparent colors anywhere"*: an accent mixed 14–24% into the page's
 * own surface is not a colour at all, it is the page with a rumour of a tint, so
 * every door on the page read as a grey shape rather than as a button.
 *
 * So every secondary door now wears THE ACCENT ITSELF — fully opaque and fully
 * saturated — with its own content in [capsuleOn]. The page's single primary action
 * is told apart by SHAPE and WEIGHT rather than by being the only coloured thing on
 * the page: the seal is a full-width travelling track, the doors are circles and
 * chips.
 */
@Composable
private fun capsuleWash(accent: Color): Color = accent

/**
 * What sits ON a [capsuleWash] fill: solid ink or solid white, whichever the fill
 * can carry, chosen from the fill's own brightness.
 *
 * The member's rule on this page is that nothing is translucent (*"use solid filled
 * colors no transparent colors anywhere"*), so a content colour is a REAL colour and
 * never `Color.White.copy(alpha = …)` over the fill. Reading the fill's luminance is
 * also what keeps the rule safe for a pale accent — a light primary gets dark ink on
 * it rather than white on white.
 */
private fun capsuleOn(fill: Color): Color =
    if (fill.luminance() > 0.60f) Color(0xFF15120F) else Color.White

/**
 * How far the thumb travels to turn one notch of the clock's deck.
 *
 * v504 — 34dp, when the deck's drag became sideways. v507 — 22dp, because 34dp is
 * more travel than a short flick covers: a member adjusting a clock taps and nudges,
 * they do not swipe across the page, so a notch they cannot reach in one gesture
 * reads as a dead control (the member's *"the gesture to chnage isnt working"*). One
 * notch is about a thumb's own width of travel, which is what a dial should be.
 */
private val NOTCH = 22.dp

/**
 * ONE CARD OF THE DECK, AND ONLY HALF OF IT TURNS.
 *
 * ⚠️ v501 — A REAL SPLIT-FLAP (the member's "the whole number flips instead of it
 * flipping only half here its visble as break"). The old card rotated its WHOLE
 * face about its own middle, so a digit going edge-on pulled the plate's top and
 * bottom edges in with it and the card visibly broke in half on the way through.
 * A split-flap never does that: the top half HOLDS, the bottom leaf drops away
 * around the crease, the glyph changes behind the crease where nothing is on
 * screen, and then the new top leaf falls into place over it. Two leaves, one
 * each side of the crease, and the card is never less than a card.
 */
@Composable
private fun FlipCard(
    value: String,
    caption: String,
    plate: Color,
    ink: Color,
    modifier: Modifier = Modifier
) {
    var shown by remember { mutableStateOf(value) }
    // The bottom leaf falls first, then the top leaf lands: one after the other,
    // never together, which is what makes a flip read as two halves of one motion.
    val lower = remember { Animatable(0f) }
    val upper = remember { Animatable(0f) }
    var leafFace by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value == shown) return@LaunchedEffect
        lower.snapTo(0f)
        lower.animateTo(1f, tween(150, easing = FastOutLinearInEasing))
        // Behind the crease, edge-on, the card changes its mind.
        shown = value
        upper.snapTo(1f)
        leafFace = value
        upper.animateTo(0f, tween(170, easing = FastOutSlowInEasing))
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CARD_HEIGHT)
        ) {
            // The bottom half already wears the NEW glyph: the falling leaf is
            // what was covering it.
            CardHalf(
                top = false,
                plate = plate,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { CardGlyph(value, ink) }
            // The top half holds the glyph it had until the leaf lands.
            CardHalf(
                top = true,
                plate = plate,
                modifier = Modifier.align(Alignment.TopCenter)
            ) { CardGlyph(shown, ink) }
            // The leaf that drops away — the OLD bottom half.
            CardHalf(
                top = false,
                plate = plate,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        rotationX = -lower.value * 90f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        cameraDistance = 16f * density
                    }
            ) { CardGlyph(shown, ink) }
            // The leaf that lands — the NEW top half.
            CardHalf(
                top = true,
                plate = plate,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        rotationX = upper.value * 90f
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        cameraDistance = 16f * density
                    }
            ) { CardGlyph(leafFace, ink) }
            // The crease itself, over every layer: one hairline that all four
            // faces are cut by, and the line the whole illusion hangs on. It is a
            // SOLID mix of the plate and the digit's ink (v507 — no alpha on this
            // page), so on a near-black plate it is a whisper of the digit colour.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(lerp(ink, plate, 0.76f))
            )
        }
        if (caption.isNotBlank()) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * v504 — THE CARDS ARE THE PAGE'S OWN SIZE NOW (80dp tall, was 62).
 *
 * A split-flap is the one thing on this page you are meant to read from across
 * the room; at 62dp with `headlineSmall` digits it read as a row of small tiles,
 * which is the member's *"tey are not big"* (see also their "the time adjust
 * gesture is bad too", which is the vertical-drag fault noted on [ClockRow]). The
 * deck grew, the digits went up a step ([CardGlyph]) and the row that holds them
 * got taller in the same pass, so the whole clock is a control rather than a
 * detail.
 */
private val CARD_HEIGHT = 80.dp
private val CARD_HALF = 40.dp

/**
 * The opaque plate of one half of a card, clipped to its own side of the crease.
 *
 * The caller has to hand in the alignment (it is the one inside the card's
 * `BoxScope`): a `Modifier.align` here would not resolve, because this function
 * is not in the box it is being drawn into.
 *
 * ⚠️ v507 — AND IT PAINTS THE PLATE, WHICH FOR A WHILE NOBODY DID. [FlipCard] has
 * always taken a `plate` colour — and never drew it, so the digits stood on the page's
 * own `surfaceContainer` with nothing behind them. On a LIGHT page that is the
 * member's *"the clock number colors is bad"*: the palette below had already been
 * set for a dark plate with bright digits (see [CapsuleClock]), so pale digits were
 * being painted directly onto a near-white row — a clock you cannot read. Each half
 * paints the plate as a FULL card shape and its own clip cuts it: the two outer
 * corners stay rounded, the crease edge stays square, and a turning leaf carries its
 * own half of the plate with it, which is the whole illusion.
 */
@Composable
private fun CardHalf(
    top: Boolean,
    plate: Color,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CARD_HALF)
            .clipToBounds()
            .then(modifier)
    ) {
        // The glyph is drawn once at the card's FULL height and pushed so that its
        // centre sits on the crease, then the half's own clip cuts it: that is
        // what makes two halves of one digit rather than two small digits.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CARD_HEIGHT)
                .offset(y = if (top) 0.dp else -CARD_HALF)
                .background(plate, RoundedCornerShape(CARD_RADIUS))
        ) {
            glyph()
        }
    }
}

/** The card's own corner radius, shared by the plate the halves paint. */
private val CARD_RADIUS = 12.dp

/** A glyph cut to a half — the digits are the same everywhere the deck is used. */
@Composable
private fun CardGlyph(value: String, ink: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = ink,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * EVERY STYLE ON THIS PAGE, IN THE PIXEL FACE — AND AT SIZES THE PIXEL FACE
 * ACTUALLY WANTS.
 *
 * One nested theme rather than a `fontFamily` argument on forty `Text` calls:
 * the page's whole voice changes in one place and a style added later inherits it.
 *
 * ⚠️ v504 — IT USED TO CHANGE THE FAMILY AND NOTHING ELSE, which is the member's
 * *"the text font size etc isnt good"*: VT323 is a bitmap-derived face drawn on a
 * coarse grid, so at Material's sizes it comes out visibly SMALLER and much lighter
 * than the same point size in a text face — a `bodyMedium` (14sp) paragraph read as
 * a caption and a `labelSmall` (11sp) caption read as a speck. The whole scale is
 * set here for THIS face: each style is a couple of points larger than Material's
 * with its line height opened to match (a pixel face needs the extra leading or the
 * ascenders of one line sit on the descenders of the last), and the small styles
 * carry a little tracking because a bitmap face's glyphs touch at small sizes.
 *
 * Nothing here changes a LAYOUT: only sizes, line heights and tracking, so every
 * padding and every panel on the page keeps the rhythm it had.
 */
@Composable
private fun pixelTypography(): Typography {
    val base = MaterialTheme.typography
    fun pixel(
        style: TextStyle,
        size: Float,
        line: Float,
        tracking: Float = 0.0f
    ): TextStyle = style.copy(
        fontFamily = PixelFontFamily,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp
    )
    return base.copy(
        displayLarge = pixel(base.displayLarge, 40f, 46f),
        displayMedium = pixel(base.displayMedium, 34f, 40f),
        displaySmall = pixel(base.displaySmall, 29f, 35f),
        headlineLarge = pixel(base.headlineLarge, 27f, 33f),
        headlineMedium = pixel(base.headlineMedium, 24f, 29f),
        headlineSmall = pixel(base.headlineSmall, 21f, 26f),
        titleLarge = pixel(base.titleLarge, 19f, 25f, 0.2f),
        titleMedium = pixel(base.titleMedium, 17.5f, 23f, 0.2f),
        titleSmall = pixel(base.titleSmall, 16f, 21f, 0.3f),
        bodyLarge = pixel(base.bodyLarge, 16.5f, 23f, 0.2f),
        bodyMedium = pixel(base.bodyMedium, 15f, 21f, 0.2f),
        bodySmall = pixel(base.bodySmall, 13.5f, 19f, 0.3f),
        labelLarge = pixel(base.labelLarge, 14.5f, 19f, 0.6f),
        labelMedium = pixel(base.labelMedium, 13f, 17f, 0.8f),
        labelSmall = pixel(base.labelSmall, 12f, 16f, 1.0f)
    )
}

@Composable
private fun CapsuleCircleButton(
    glyph: String,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        // v507 — THE ACCENT ITSELF (see [capsuleWash]): a solid coloured disc with
        // its glyph in [capsuleOn], rather than a faint tint with a coloured glyph
        // on it. A coloured circle is a button; a pale circle is a hole.
        color = capsuleWash(accent),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CurioIcon(glyph, label, tint = capsuleOn(accent), size = 20.dp)
        }
    }
}

/**
 * SLIDE TO SEAL — the travelling confirm that sends a letter on its way.
 *
 * A track with a slug at its leading edge: the slug is DRAGGED to the far end and
 * the seal lands there, with a tick as it comes unstuck and the app's own confirm
 * thunk on the arrival. Dragged less than [SEAL_REACH] of the way, it springs home
 * and NOTHING is sealed — which is the whole point of a travelling confirm over a
 * button (a slip of the thumb cannot send a letter into next year by accident).
 *
 * The travel is a plain `mutableFloatStateOf` and the settling is
 * `animateFloatAsState`: no `snapTo`/`animateTo` is ever called from the gesture
 * callback (a gesture lambda is not a suspend context — the rule the tear-strip
 * went red on once), so a released slug is the animation doing its own job.
 *
 * ⚠️ v506 — AND A REFUSED SEAL PUTS THE SLUG BACK (a real bug the member hit:
 * *"the slide to seal is bugged when it fails with wrong time it stucks in that end
 * position with no way to save again"*). [onSeal] answers whether the store took the
 * letter, and the slug springs home when it did not: it used to be latched at the
 * far end (`sealed = true`, `travelled = travel`) BEFORE the store was asked, so a
 * refusal left a full track with a lock at the end of it that could no longer be
 * dragged — the page's own note explaining the refusal was drawn under a control that
 * had already given up. The reset happens in the drag callback as two plain float
 * writes (no suspend call — see the note above).
 */
@Composable
private fun CapsuleSealSwipe(
    enabled: Boolean,
    accent: Color,
    /** True when the letter was actually sealed; a refusal springs the slug home. */
    onSeal: () -> Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var trackWidth by remember { mutableFloatStateOf(0f) }
    var travelled by remember { mutableFloatStateOf(0f) }
    var sealed by remember { mutableStateOf(false) }
    val knob = 46.dp
    val knobPx = with(density) { knob.toPx() }
    val travel = (trackWidth - knobPx - 12f).coerceAtLeast(1f)
    val settled by animateFloatAsState(
        targetValue = travelled,
        animationSpec = CurioMotion.Springs.Snappy,
        label = "capsuleSealSwipe"
    )
    val progress = (settled / travel).coerceIn(0f, 1f)
    // ── v507 — A SOLID TRACK, STARTING AT THE ACCENT AND DEEPENING INTO IT ──────
    //
    // The member: *"more colorful buttom and use solid filled colors no transparent
    // colors anywhere"*. The track used to be `lerp(surface, accent, 0.16…0.60)`,
    // which began as the page's own surface with a rumour of accent on it — the
    // faintest control on a page that had four other strengths of the same mix. It
    // starts at a REAL mix of the accent now (still opaque, still coloured) and
    // travels the rest of the way into the accent itself as the seal approaches, so
    // the track's own colour is the progress bar. The knob is the accent's own
    // opposite ([capsuleOn]) — a solid white slug on a rose track, a solid ink one
    // on a pale accent — so it can never disappear into the fill it lands on.
    val restFill = lerp(accent, scheme.surfaceContainerHighest, 0.45f)
    val trackFill = lerp(restFill, accent, progress)
    val onTrack = capsuleOn(accent)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = trackFill,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .onSizeChanged { trackWidth = it.width.toFloat() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                // The words step aside as the slug travels, so the thing reads as
                // a track rather than as a label with a handle on it.
                text = if (enabled) "Slide to seal" else "Add a line, a picture or your voice",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                // v504 — solid ink that steps aside as the slug travels (it used to
                // fade from the muted variant, which is already the quietest text on
                // the page, so the track's words were nearly invisible at rest).
                // v507 — and it steps aside by MIXING INTO THE TRACK'S OWN COLOUR,
                // not by fading to transparent: the same look, no alpha anywhere.
                color = lerp(onTrack, trackFill, progress * 0.92f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = knob + 10.dp, end = knob + 10.dp)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .offset(x = with(density) { settled.toDp() })
                    .size(knob)
                    .background(
                        color = if (enabled) onTrack else scheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(11.dp)
                    )
                    .pointerInput(enabled, travel) {
                        if (!enabled) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (travelled / travel < SEAL_REACH) travelled = 0f
                            },
                            onDragCancel = {
                                if (travelled / travel < SEAL_REACH) travelled = 0f
                            }
                        ) { _, dx ->
                            if (sealed) return@detectHorizontalDragGestures
                            val next = (travelled + dx).coerceIn(0f, travel)
                            if (next > 2f && travelled <= 2f) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            travelled = next
                            if (travelled / travel >= SEAL_REACH) {
                                sealed = true
                                travelled = travel
                                // The store decides. It takes the letter (the page
                                // shows its own sealed state and the slug has done
                                // its job) or it refuses for a real reason — a
                                // moment that has passed, an empty letter — and
                                // then the track has to be usable again, which is
                                // exactly what the member could not do before v506.
                                if (onSeal()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sealed = false
                                    travelled = 0f
                                }
                            }
                        }
                    }
            ) {
                CurioIcon(
                    CurioIcons.Lock,
                    "Slide to seal",
                    // The slug's glyph is the TRACK'S colour: the two are always the
                    // same fill and its opposite, whichever way round they are.
                    tint = if (enabled) accent else scheme.onSurfaceVariant,
                    size = 20.dp
                )
            }
        }
    }
}

/** How far the slug has to travel before the seal lands (and below which a
 *  release springs back). Four fifths: far enough that a slip cannot do it,
 *  short enough that a deliberate slide never has to touch the very edge. */
private const val SEAL_REACH = 0.8f

@Composable
private fun CapsulePresetChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // v501 — A SOFT RECTANGLE: the presets were accent-coloured pills on a page
    // that now uses rounded RECTANGLES and a washed accent, so they are the same
    // soft fill as everything else, squared off to match.
    //
    // ⚠️ v504 — AND THE LABEL IS SOLID INK. It was `lerp(onSurface, accent, 0.55f)`
    // — a colour half way between the page's ink and the accent, on a fill that is
    // ALREADY a wash of that accent, which is the member's *"all the text elemet
    // colors are bad and blending"*: two accent-tinged layers a shade apart, so the
    // word did not read as a word. The fill carries the accent; the label carries
    // the page's own ink, at full strength.
    //
    // v507 — AND EVERY DOOR ON THIS PAGE WEARS THE SAME SOLID ACCENT FILL
    // ([capsuleWash]) WITH ITS LABEL IN [capsuleOn]. The member's second report —
    // *"still the time capsule button colors etc is bad"* — was a page with five
    // different accent strengths on it (0.10, 0.16, 0.18, 0.20, 0.24), and then
    // their own correction: *"more colorful buttom and use solid filled colors no
    // transparent colors anywhere"*. One solid accent for every door on the page,
    // and the label is a real colour on it — never a translucent white.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = capsuleWash(accent),
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = capsuleOn(accent),
                maxLines = 1
            )
        }
    }
}

/** "12 March 2027 at 09:30" — the promise, in the member's own locale. */
private fun promiseLabel(at: Long): String =
    SimpleDateFormat("d MMMM yyyy 'at' HH:mm", Locale.getDefault()).format(Date(at))

private fun flipDay(cal: Calendar): String =
    String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))

private fun flipMonth(cal: Calendar): String =
    SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time).uppercase(Locale.getDefault())

private fun flipYear(cal: Calendar): String =
    String.format(Locale.US, "%d", cal.get(Calendar.YEAR))

private fun flipHour(cal: Calendar, hours24: Boolean): String {
    val hour = if (hours24) cal.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR)
    return String.format(Locale.US, "%02d", if (!hours24 && hour == 0) 12 else hour)
}

private fun flipMinute(cal: Calendar): String =
    String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

private fun flipMarker(cal: Calendar): String =
    if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

/** "12 March 2027" — the day a letter lands, in the member's own locale. */
private fun dayLabel(at: Long): String =
    SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(at))

/**
 * "three weeks ago" — how long a letter has been sealed, for the arrival's own
 * line ("YOU, A MONTH AGO"). Rounded DOWN to the unit the gap is actually in, the
 * same rule [com.curio.app.data.CurioRecall.agoText] follows: an arrival that
 * overstates how long ago it was is an arrival that cannot be trusted with the
 * rest of the sentence.
 */
private fun agoLabel(at: Long, now: Long = System.currentTimeMillis()): String {
    val days = ((now - at).coerceAtLeast(0L) / DAY_MS).toInt()
    return when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        days < 7 -> "$days days ago"
        days < 14 -> "a week ago"
        days < 30 -> "${days / 7} weeks ago"
        days < 60 -> "a month ago"
        else -> "${days / 30} months ago"
    }
}

/** Whether a colour reads light or dark — the one test the brass mix needs. */
private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

/**
 * v493 — THE ARRIVAL: the day a capsule is due, it TAKES THE WHOLE SCREEN.
 *
 * The member's answer, verbatim in intent: *"it covers the whole screen on the
 * day it returns"*. So this is not a card and not a notification — it is a sealed
 * envelope over everything, and the letter inside only exists once the seal is
 * broken. That is the feature: the words are unreadable until the day, and the
 * screen is what proves it.
 *
 * It waits rather than expiring (the same rule as the Return), and `waiting` names
 * how many MORE letters are behind this one, so a member who was away for a
 * season knows the stack has a bottom.
 */
/**
 * v493 — THE ARRIVAL: the day a capsule is due, it TAKES THE WHOLE SCREEN.
 *
 * The member's answer, verbatim in intent: *"it covers the whole screen on the
 * day it returns"*. So this is not a card and not a notification — it is a sealed
 * envelope over everything, and the letter inside only exists once the seal is
 * broken. That is the feature: the words are unreadable until the day, and the
 * screen is what proves it.
 *
 * ⚠️ v502 — AND IT IS OPENED BY HOLDING, NOT BY TAPPING (the member: *"when the
 * time capsule lands use similiar style which shows when app in opened with way
 * unique way to open it it should feel like a nostagia or something"*).
 *
 * A tap is a door; a hold is an act. The seal now has to be HELD DOWN: a ring
 * fills around the disc while the thumb stays on it, one slow second of pressure,
 * and only a completed hold breaks it. Let go and the ring runs back down and
 * nothing happens — so a letter written months ago cannot be torn open by a
 * pocket. It is the same language as the swipe that sealed it: a deliberate,
 * felt gesture rather than a button.
 *
 * The page around it wears the capsule's own pixel face ([pixelTypography]) and
 * its soft rectangles, so the writing page, the arrival and the letter all read
 * as one object. The letter's own words stay in the reading face: a letter is
 * something to READ, and the pixel face is the machine's voice, not the friend's.
 *
 * It waits rather than expiring (the same rule as the Return), and `waiting` names
 * how many MORE letters are behind this one, so a member who was away for a
 * season knows the stack has a bottom.
 */
@Composable
fun TimeCapsuleArrival(
    capsule: CurioTimeCapsules.Capsule,
    /** How many letters are waiting in total — this one included. */
    waiting: Int,
    onOpened: (CurioTimeCapsules.Capsule) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val brass = lerp(scheme.tertiary, Color(0xFFB08D4F), if (scheme.surface.luminance() > 0.5f) 0.45f else 0.55f)
    val ink = scheme.onSurface
    val paper = lerp(scheme.surface, Color.White, if (scheme.surface.luminance() > 0.5f) 0.5f else 0.06f)
    // ⚠️ v504 — THE ARRIVAL STANDS ON THE SCRIM, SO ITS OWN INK IS LIGHT.
    //
    // This screen is drawn over `scheme.scrim` — which is BLACK in both themes —
    // but every word on it that is not on the letter's paper was drawn in
    // `scheme.onSurface`. In LIGHT mode that is near-black ink on a black scrim:
    // "Sealed 4 August 2026", "Hold the seal to open it" and "Let go and nothing
    // happens" were, in practice, invisible — the member's *"its view is bad really
    // bad too textx etc isnt looking good color etc"*. Anything on the SCRIM is white
    // now (`onScrim`), and anything on the letter's PAPER keeps the page's own ink.
    //
    // ⚠️ v507 — AND A QUIET LINE IS A MIX, NOT AN ALPHA (the member: *"use solid
    // filled colors no transparent colors anywhere"*). Every softened line on this
    // screen used to be `color.copy(alpha = …)`, which is the same picture as mixing
    // it into what is behind it — but a mix is a real colour the theme can reason
    // about, and an alpha is a hole. `onScrimQuiet` is the scrim's own top of the
    // gradient, `scrim` the bottom, and the letter's two quiet lines mix into the
    // paper they sit on.
    val onScrim = Color.White
    val scrimTop = lerp(scheme.scrim, brass, 0.20f)

    // `broken` is the whole state machine: false = sealed, true = the letter is out.
    var broken by remember(capsule.id) { mutableStateOf(false) }
    // The hold: `holding` is the thumb's own state, and `fill` is how far round
    // the ring has gone. The effect restarts on release, which is what makes the
    // ring run back down and NOTHING happen.
    var holding by remember { mutableStateOf(false) }
    val fill = remember { Animatable(0f) }
    // `reveal` is the letter arriving once the words are the member's.
    val reveal = remember { Animatable(0f) }
    // Driven off the BREAK rather than off the gesture, so the letter's entrance
    // is the same beat as the thunk instead of a frame ahead of it.
    LaunchedEffect(broken) {
        if (broken) reveal.animateTo(1f, CurioMotion.Springs.Bouncy)
    }
    LaunchedEffect(holding) {
        if (holding) {
            fill.snapTo(0f)
            // An incomplete hold is CANCELLED here (this effect restarts when the
            // thumb lifts), which is exactly why the completion is verified rather
            // than assumed.
            fill.animateTo(1f, tween(HOLD_TO_OPEN_MS, easing = LinearEasing))
            if (fill.value >= 1f) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                broken = true
                onOpened(capsule)
            }
        } else {
            fill.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        }
    }

    MaterialTheme(colorScheme = scheme, typography = pixelTypography()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(scrimTop, scheme.scrim))
                )
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            if (!broken) {
                // ── SEALED: the envelope and its wax ────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (waiting > 1) "A LETTER FROM YOU · $waiting WAITING"
                        else "A LETTER FROM YOU",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.6.sp
                        ),
                        color = brass
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Sealed ${dayLabel(capsule.sealedAt)} · for ${capsule.spanLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = lerp(onScrim, scrimTop, 0.22f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    // The envelope: a slab of the member's own paper with a wax
                    // disc on it, and the disc is what has to be held.
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = paper,
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "sealed",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                // On the letter's own paper, so it keeps the page's
                                // ink — a watermark, deep enough to read as a word.
                                color = lerp(ink, paper, 0.72f)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(104.dp)
                                    .pointerInput(capsule.id) {
                                        detectTapGestures(
                                            onPress = {
                                                holding = true
                                                haptics.performHapticFeedback(
                                                    HapticFeedbackType.TextHandleMove
                                                )
                                                tryAwaitRelease()
                                                holding = false
                                            }
                                        )
                                    }
                            ) {
                                // THE RING: how much of the hold is done, drawn
                                // as an arc that closes on the disc.
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = lerp(brass, paper, 0.70f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 5f)
                                    )
                                    if (fill.value > 0f) {
                                        drawArc(
                                            color = brass,
                                            startAngle = -90f,
                                            sweepAngle = 360f * fill.value,
                                            useCenter = false,
                                            style = Stroke(width = 5f)
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = brass,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .graphicsLayer {
                                            val s = 1f - fill.value * 0.08f
                                            scaleX = s
                                            scaleY = s
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CurioIcon(
                                            CurioIcons.Lock,
                                            "Hold to break the seal",
                                            tint = Color.White,
                                            size = 30.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = if (holding) "Keep holding" else "Hold the seal to open it",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = onScrim
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Let go and nothing happens",
                        style = MaterialTheme.typography.labelMedium,
                        color = lerp(onScrim, scheme.scrim, 0.32f)
                    )
                }
            } else {
                // ── BROKEN: the letter itself ───────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer {
                            alpha = reveal.value
                            val s = 0.96f + 0.04f * reveal.value
                            scaleX = s
                            scaleY = s
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        // `spanLabel` is a DATE and TIME since the rewrite ("12
                        // March 2027 at 09:30"), so "YOU, … AGO" reads off the seal
                        // date instead of the promise.
                        text = "YOU, ${agoLabel(capsule.sealedAt).uppercase()}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.6.sp
                        ),
                        color = brass
                    )
                    Spacer(Modifier.height(16.dp))
                    // ── AND WHAT THE LETTER CARRIED ────────────────────────
                    // The picture comes back FIRST, above the words, because that
                    // is the order it was meant in: the photograph the member chose
                    // for this day is what they will look at, and the words under it
                    // are what they wrote about it.
                    if (capsule.photo.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = paper,
                            shadowElevation = 12.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PersonalPagePhoto(
                                uri = capsule.photo,
                                height = 220.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = paper,
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp)
                        ) {
                            Text(
                                // The letter's words stay in the reading face: a
                                // letter is something to read, and the pixel face
                                // is the machine's voice, not the friend's.
                                text = capsule.message,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Default,
                                    lineHeight = 26.sp
                                ),
                                color = ink
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = "written ${dayLabel(capsule.sealedAt)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = lerp(ink, paper, 0.38f)
                            )
                        }
                    }
                    // ── AND THE VOICE, READ BACK ───────────────────────────
                    // The same wave the member recorded, played with the same
                    // control the journal gives a note (see PersonalVoiceBar): a
                    // letter read months later should sound like the person who
                    // wrote it.
                    if (capsule.voice.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = paper,
                            shadowElevation = 12.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(10.dp)) {
                                PersonalVoiceBar(
                                    path = capsule.voice,
                                    seconds = capsule.voiceSeconds,
                                    bars = capsule.voiceBars,
                                    ink = ink,
                                    accent = brass
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        onClick = { onDismiss() },
                        shape = RoundedCornerShape(14.dp),
                        color = brass,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Put it away",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A DOOR FOR WHAT THE LETTER CARRIES: a soft rectangle in the page's own shape
 * language, with a glyph and a word. The two doors are one size whether or not
 * they hold something — the LABEL is what changes ("Add a picture" becoming
 * "Change the picture"), so the row never jumps when the member uses it.
 */
@Composable
private fun CapsuleAttachmentDoor(
    glyph: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        // v507 — the same SOLID accent fill as every other door on the page, with
        // its glyph and word in [capsuleOn]: the coloured glyph these two doors used
        // to carry sat on the accent itself (see [capsuleWash]), which is invisible.
        color = capsuleWash(accent),
        modifier = modifier.height(46.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            CurioIcon(glyph, null, tint = capsuleOn(accent), size = 18.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = capsuleOn(accent),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The way to take an attachment back off the letter, on the attachment itself. */
@Composable
private fun CapsuleAttachmentRemove(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        // v507 — a SOLID accent disc with the close mark in [capsuleOn], rather than
        // an 18% tint of the accent with near-black ink on it (see [capsuleWash]).
        color = capsuleWash(accent),
        shadowElevation = 4.dp,
        modifier = modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CurioIcon(CurioIcons.Close, label, tint = capsuleOn(accent), size = 18.dp)
        }
    }
}

/** How long the seal has to be held before it breaks. One slow second: long
 *  enough that it cannot be a tap by accident, short enough that it is not a
 *  chore. */
private const val HOLD_TO_OPEN_MS = 1000

/**
 * ── v504 — YOUR LETTERS: EVERY CAPSULE, ON THE PAGE IT BELONGS TO ────────────
 *
 * The member's two asks in one place: *\"the time capusle sealed shows in home
 * screen in a row pill whih is bad so remove it and place it soemwhere else\"* and
 * *\"a way to see previous capsules etc\"*. Home's row pill is deleted (see
 * `HomeCapsules.kt`), and the count, the next landing AND the letters already
 * written live here — the page the letters are about, which is where the home
 * pill was asked to move to.
 *
 * What a row says depends on what the letter IS:
 *  * **waiting** — still sealed, its day ahead: when it comes back, and the moment
 *    it was written for;
 *  * **ready** — sealed, its day has come: the arrival has not been opened yet, and
 *    opening it here is the SAME write the arrival makes (see [CurioTimeCapsules.open]),
 *    so a letter read on this page is never read twice;
 *  * **read** — already opened: the opened letter, tapped open again in place.
 *
 * One row expands at a time and expands IN PLACE rather than on a screen of its
 * own: this is a list of your own letters, not a reader. `all()` is the store's own
 * door for it (it has existed since v496 and had no caller until now).
 */
@Composable
private fun CapsuleArchive(
    capsules: List<CurioTimeCapsules.Capsule>,
    accent: Color,
    now: Long,
    onOpened: (CurioTimeCapsules.Capsule) -> Unit,
    modifier: Modifier = Modifier
) {
    if (capsules.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    // v507 — what sits on a READY row's own fill. The ready row is the one row on
    // this page asking for something, and "solid filled colors no transparent colors
    // anywhere" (the member) means the row is the accent itself — so every word on it
    // is [capsuleOn], and the quiet step under the title is a MIX toward the accent
    // rather than an alpha (see [capsuleOn]).
    val onAccent = capsuleOn(accent)
    val onAccentQuiet = lerp(onAccent, accent, 0.24f)
    val haptics = LocalHapticFeedback.current
    var expandedId by remember { mutableStateOf("") }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Your letters",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = scheme.onSurface
        )
        capsules.forEach { capsule ->
            val ready = capsule.sealed && capsule.openAt <= now
            val waiting = capsule.sealed && !ready
            val expanded = expandedId == capsule.id
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    expandedId = if (expanded) "" else capsule.id
                    // Opening a READY letter retires it — the same single write the
                    // full-screen arrival makes, so the two can never disagree.
                    if (ready && !expanded) onOpened(capsule)
                },
                shape = RoundedCornerShape(14.dp),
                // A letter whose day has come is the one row on this page that is
                // asking for something, so it is the one wearing the accent — the
                // accent ITSELF, solid, rather than a 20% tint of it.
                color = if (ready) accent else scheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurioIcon(
                            CurioIcons.Lock,
                            null,
                            tint = when {
                                ready -> onAccent
                                waiting -> scheme.onSurfaceVariant
                                else -> accent
                            },
                            size = 17.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when {
                                    ready -> "Ready to read"
                                    waiting -> "Back to you ${
                                        CurioTimeCapsules.untilText(capsule.openAt, now)
                                    }"
                                    else -> "Opened ${capsule.spanLabel}"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (ready) onAccent else scheme.onSurface
                            )
                            // The moment it was written for, always: it is the one
                            // fact that makes a letter a letter.
                            Text(
                                if (waiting) "For ${capsule.spanLabel}" else capsule.spanLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (ready) onAccentQuiet else scheme.onSurfaceVariant
                            )
                        }
                        CurioIcon(
                            if (expanded) CurioIcons.Close else CurioIcons.ChevronRight,
                            null,
                            tint = if (ready) onAccentQuiet else scheme.onSurfaceVariant,
                            size = 18.dp
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(10.dp))
                        if (capsule.message.isNotBlank()) {
                            Text(
                                capsule.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ready) onAccent else scheme.onSurface
                            )
                        }
                        if (capsule.photo.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            PersonalPagePhoto(
                                uri = capsule.photo,
                                height = 170.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        if (capsule.voice.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            PersonalVoiceBar(
                                path = capsule.voice,
                                seconds = capsule.voiceSeconds,
                                bars = capsule.voiceBars,
                                ink = if (ready) onAccent else scheme.onSurface,
                                // On a ready row the row's own fill IS the accent, so the
                                // bar's highlight is the light contrast colour instead —
                                // otherwise its wave and its controls would be drawn in
                                // the colour they are sitting on.
                                accent = if (ready) onAccentQuiet else accent,
                                onRemove = null
                            )
                        }
                    }
                }
            }
        }
    }
}

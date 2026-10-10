package com.curio.app.features.timecapsule

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioTimeCapsules
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.FrauncesFontFamily
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import java.util.Calendar

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
    var target by rememberSaveable { mutableStateOf(System.currentTimeMillis() + 30L * DAY_MS) }
    var message by rememberSaveable { mutableStateOf("") }
    var dateOpen by remember { mutableStateOf(false) }
    var timeOpen by remember { mutableStateOf(false) }
    var sealedCapsule by remember { mutableStateOf<CurioTimeCapsules.Capsule?>(null) }
    var nowTick by remember { mutableIntStateOf(0) }

    // The head's own count, and the app's one place that recomputes what is
    // waiting: the page can be the first thing opened after a return.
    LaunchedEffect(Unit) { CurioTimeCapsules.refresh(context) }
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

    if (dateOpen) {
        // Android's own Material calendar: the member asked to set the DAY, so
        // the day is set on the picker they already know.
        val picker = rememberDatePickerState(initialSelectedDateMillis = target)
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = picker.selectedDateMillis
                    if (picked != null) {
                        // The picker answers a DAY (midnight UTC); the page keeps
                        // the hour and minute the member already set, so picking a
                        // date never silently moves the time they chose.
                        val keep = Calendar.getInstance().apply { timeInMillis = target }
                        val day = Calendar.getInstance().apply {
                            timeInMillis = picked
                            set(Calendar.HOUR_OF_DAY, keep.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, keep.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val minAt = System.currentTimeMillis() + floor
                        target = maxOf(day.timeInMillis, minAt)
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    dateOpen = false
                }) { Text("Set the day") }
            },
            dismissButton = {
                TextButton(onClick = { dateOpen = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = picker, showModeToggle = true)
        }
    }

    if (timeOpen) {
        // …and the same for the hour and minute.
        val picker = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = hours24
        )
        AlertDialog(
            onDismissRequest = { timeOpen = false },
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
                    timeOpen = false
                }) { Text("Set the time") }
            },
            dismissButton = {
                TextButton(onClick = { timeOpen = false }) { Text("Cancel") }
            },
            title = { Text("At what time?") },
            text = { TimePicker(state = picker) }
        )
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CapsuleCircleButton(
                    glyph = CurioIcons.ArrowBack,
                    label = "Back",
                    accent = accent,
                    onClick = onBack
                )
                Text(
                    "Time capsule",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = scheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (CurioTimeCapsules.sealedCountState > 0) {
                    Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.14f)) {
                        Text(
                            "${CurioTimeCapsules.sealedCountState} sealed",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            CapsuleHero(accent = accent, until = until, promise = promise)
            Spacer(Modifier.height(20.dp))

            // ── THE CLOCK ─────────────────────────────────────────────────
            CapsuleClock(
                cal = cal,
                hours24 = hours24,
                accent = accent,
                onPreviousDay = { shift(-DAY_MS) },
                onNextDay = { shift(DAY_MS) },
                onPreviousHour = { shift(-HOUR_MS) },
                onNextHour = { shift(HOUR_MS) },
                onPreviousQuarter = { shift(-QUARTER_MS) },
                onNextQuarter = { shift(QUARTER_MS) }
            )
            Spacer(Modifier.height(16.dp))

            // ── THE DOORS TO ANDROID'S OWN PICKERS ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CapsuleDoor(
                    glyph = CurioIcons.Schedule,
                    label = "Pick a date",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                ) { dateOpen = true }
                CapsuleDoor(
                    glyph = CurioIcons.AutoAwesome,
                    label = "Pick a time",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                ) { timeOpen = true }
            }
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
                        color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                minLines = 6,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val sealed = CurioTimeCapsules.sealAt(
                        context = context,
                        message = message,
                        openAt = target,
                        label = promise
                    )
                    if (sealed != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        sealedCapsule = sealed
                    }
                },
                enabled = message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                CurioIcon(CurioIcons.Lock, null, tint = scheme.onPrimary, size = 20.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Seal it — it lands $until",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
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
                    Surface(
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier.size(88.dp)
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
                        "It comes back ${CurioTimeCapsules.untilText(done.openAt)} — on ${done.spanLabel}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

private const val DAY_MS = 24L * 60L * 60L * 1000L
private const val HOUR_MS = 60L * 60L * 1000L
private const val QUARTER_MS = 15L * 60L * 1000L

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
    val shift by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "capsuleDrift"
    )
    val far = lerp(accent, scheme.tertiary, 0.55f)
    val near = lerp(accent, scheme.secondary, 0.85f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent, far, near, accent),
                    start = Offset(-600f + shift * 1200f, -200f),
                    end = Offset(600f + shift * 1200f, 700f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(CurioIcons.Lock, null, tint = Color.White, size = 22.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Lands $until",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                promise,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.86f)
            )
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
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousHour: () -> Unit,
    onNextHour: () -> Unit,
    onPreviousQuarter: () -> Unit,
    onNextQuarter: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val plate = lerp(scheme.surface, scheme.onSurface, 0.86f)
    val ink = lerp(scheme.onSurface, scheme.surface, 0.96f)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── the day ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CapsuleStepButton(
                    glyph = CurioIcons.ChevronLeft,
                    label = "A day earlier",
                    accent = accent
                ) { onPreviousDay() }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                        modifier = Modifier.weight(1.4f)
                    )
                    FlipCard(
                        value = flipYear(cal),
                        caption = "YEAR",
                        plate = plate,
                        ink = ink,
                        modifier = Modifier.weight(1.6f)
                    )
                }
                CapsuleStepButton(
                    glyph = CurioIcons.Add,
                    label = "A day later",
                    accent = accent
                ) { onNextDay() }
            }

            // ── the time ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CapsuleStepButton(
                    glyph = CurioIcons.Remove,
                    label = "An hour earlier",
                    accent = accent
                ) { onPreviousHour() }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                        color = scheme.onSurfaceVariant
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
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                CapsuleStepButton(
                    glyph = CurioIcons.Add,
                    label = "An hour later",
                    accent = accent
                ) { onNextHour() }
            }

            // ── a quarter of an hour, the finest step a thumb needs ───────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CapsuleWideStep(
                    label = "15 min earlier",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                ) { onPreviousQuarter() }
                CapsuleWideStep(
                    label = "15 min later",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                ) { onNextQuarter() }
            }
        }
    }
}

/**
 * ONE CARD OF THE DECK, AND IT TURNS.
 *
 * The plate is opaque with a crease across its middle, and the card rotates about
 * that crease when its value changes: edge-on at the halfway point, then back
 * face-on with the new value — the one flip a split-flap makes. `cameraDistance`
 * is what keeps a rotating card from looking like a squashed rectangle.
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
    var away by remember { mutableStateOf(true) }
    val turn = remember { Animatable(0f) }
    LaunchedEffect(value) {
        if (value == shown) return@LaunchedEffect
        away = true
        turn.animateTo(1f, tween(130, easing = FastOutLinearInEasing))
        shown = value
        away = false
        turn.animateTo(0f, tween(190, easing = FastOutSlowInEasing))
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .graphicsLayer {
                    rotationX = if (away) -(turn.value * 90f) else turn.value * 90f
                    cameraDistance = 16f * density
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = plate,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        shown,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = ink,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    // The crease: what makes a plate a FLAP rather than a tile.
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ink.copy(alpha = 0.22f))
                    )
                }
            }
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

@Composable
private fun CapsuleCircleButton(
    glyph: String,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CurioIcon(glyph, label, tint = accent, size = 20.dp)
        }
    }
}

@Composable
private fun CapsuleStepButton(
    glyph: String,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = lerp(scheme.surfaceContainerHigh, accent, 0.16f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CurioIcon(glyph, label, tint = accent, size = 18.dp)
        }
    }
}

@Composable
private fun CapsuleWideStep(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = lerp(scheme.surfaceContainerHigh, accent, 0.10f),
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CapsuleDoor(
    glyph: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceContainerHigh,
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            CurioIcon(glyph, null, tint = accent, size = 18.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = scheme.onSurface
            )
        }
    }
}

@Composable
private fun CapsulePresetChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.15f),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = accent,
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
@Composable
fun TimeCapsuleArrival(
    capsule: CurioTimeCapsules.Capsule,
    /** How many letters are waiting in total — this one included. */
    waiting: Int,
    onOpened: (CurioTimeCapsules.Capsule) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val brass = lerp(scheme.surface, Color(0xFFB08D4F), if (scheme.surface.luminance() > 0.5f) 0.55f else 0.72f)
    val ink = scheme.onSurface
    val paper = lerp(scheme.surface, Color.White, if (scheme.surface.luminance() > 0.5f) 0.5f else 0.06f)

    // `broken` is the whole state machine: false = sealed, true = the letter is out.
    var broken by remember(capsule.id) { mutableStateOf(false) }
    // Two clocks on purpose: `press` is the thumb coming down on the wax, and
    // `reveal` is the letter itself arriving once the words are the member's.
    val press = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    // Driven off the BREAK rather than off the click, so the letter's entrance is
    // the same beat as the thunk instead of a frame ahead of it.
    LaunchedEffect(broken) {
        if (broken) reveal.animateTo(1f, CurioMotion.Springs.Bouncy)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(lerp(scheme.scrim, brass, 0.18f), scheme.scrim)
                )
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
                    color = ink.copy(alpha = 0.65f)
                )
                Spacer(Modifier.height(30.dp))
                // The envelope: a slab of the member's own paper with a wax disc
                // on it, and the disc is the button.
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = paper,
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "sealed",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ink.copy(alpha = 0.25f)
                        )
                        Surface(
                            onClick = {
                                if (press.value <= 0f) {
                                    // The same two halves as every other landing
                                    // in the app: the press is felt on the way
                                    // down, and the THUNK lands on the break —
                                    // which is the moment the words become the
                                    // member's again.
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        press.animateTo(1f, CurioMotion.Springs.Snappy)
                                        broken = true
                                        onOpened(capsule)
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = brass,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(84.dp)
                                .graphicsLayer {
                                    // Coming down: the seal leans while it is
                                    // pressed, so the break has a before.
                                    val s = 1f - press.value * 0.1f
                                    scaleX = s
                                    scaleY = s
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CurioIcon(
                                    CurioIcons.Lock,
                                    "Break the seal",
                                    tint = Color.White,
                                    size = 30.dp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Tap the seal to open it",
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = 0.6f)
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
                    // v497 — `spanLabel` is a DATE and TIME since the rewrite
                    // ("12 March 2027 at 09:30"), so "YOU, … AGO" reads off the
                    // seal date instead of the promise.
                    text = "YOU, ${agoLabel(capsule.sealedAt).uppercase()}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    ),
                    color = brass
                )
                Spacer(Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
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
                            text = capsule.message,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 26.sp
                            ),
                            color = ink
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "written ${dayLabel(capsule.sealedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ink.copy(alpha = 0.45f)
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                Surface(
                    onClick = { onDismiss() },
                    shape = RoundedCornerShape(50),
                    color = brass,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Put it away",
                        style = MaterialTheme.typography.titleSmall.copy(
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

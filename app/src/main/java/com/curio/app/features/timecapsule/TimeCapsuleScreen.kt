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

/**
 * v493 — THE TIME MACHINE'S WRITING PAGE (agenda § 5.5).
 *
 * The member's shape for it, in their own words: *"a new time capsule option with
 * its page … a really beautiful time machine style ui to write the message"*.
 *
 * So the page is a MACHINE, not a form:
 *
 *  - A **brass dial** you DRAG, with the five spans notched around its top half
 *    and a slow ring of teeth turning behind them. It is the one interaction the
 *    page is built around, and it is the thing that makes "how far ahead" a
 *    physical choice rather than a dropdown — a thumb turns the dial, and the
 *    notch it lands on is felt (one tick per notch, the same vocabulary the
 *    deck's riffle and the passport's stamp use).
 *  - The **message** on the app's own paper, because the app is paper and a
 *    letter should be written on it even when it is a letter to the future.
 *  - A **wax seal** that is PRESSED — it compresses under the thumb and lands
 *    with the app's heaviest confirm, exactly like the passport's stamp, because
 *    sealing a letter is the same act of something physical arriving and this
 *    app already has one language for that.
 *
 * The dial's arithmetic is real (`dialIndexAt`): the pointer's angle decides the
 * notch, not a hit target per notch, so a drag anywhere near the dial turns it.
 */
@Composable
fun TimeCapsuleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var message by remember { mutableStateOf("") }
    var spanIndex by remember { mutableIntStateOf(CurioTimeCapsules.Span.THREE_MONTHS.ordinal) }
    val spans = CurioTimeCapsules.Span.entries.toList()
    val span = spans[spanIndex.coerceIn(0, spans.size - 1)]

    // The seal's own two halves, exactly like the passport's stamp: a squash
    // under the thumb, then the arrival. `landed` carries the capsule that was
    // written, so the confirmation can name the day it will come back.
    var sealing by remember { mutableStateOf(false) }
    var landed by remember { mutableStateOf<CurioTimeCapsules.Capsule?>(null) }
    val sealPress = remember { Animatable(0f) }

    val scheme = MaterialTheme.colorScheme
    // A warm brass on the app's own surface: the machine is the surface's own
    // colour pushed toward gold, so it reads as this app's machine rather than as
    // a stock bronze panel — and it works in both themes because it is a mix of
    // what is already there rather than a fixed hex.
    val brass = lerp(scheme.surface, Color(0xFFB08D4F), if (scheme.surface.luminance() > 0.5f) 0.55f else 0.72f)
    val ink = scheme.onSurface
    val paper = lerp(scheme.surface, Color.White, if (scheme.surface.luminance() > 0.5f) 0.5f else 0.06f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(scheme.background, brass, 0.10f),
                        scheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // ── The way back, its own disc, like every floating header ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.ArrowBack,
                            "Back",
                            tint = ink,
                            size = 22.dp
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "A letter forward",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ink
                    )
                    Text(
                        // One line, and it is not decoration: it is the whole
                        // contract of the feature in the fewest words that make
                        // it honest.
                        text = "You will not be able to read this until the day you set.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ink.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── THE DIAL ────────────────────────────────────────────────
            TimeDial(
                spans = spans.map { it.label },
                index = spanIndex,
                onIndex = { next ->
                    if (next != spanIndex) {
                        spanIndex = next
                        // One tick per notch: the thumb hears the machine click.
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                brass = brass,
                ink = ink
            )

            Spacer(Modifier.height(6.dp))
            val landsAt = span.openAt(System.currentTimeMillis())
            Text(
                text = "lands ${CurioTimeCapsules.untilText(landsAt)} · ${dayLabel(landsAt)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = brass,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(22.dp))

            // ── THE MESSAGE, ON PAPER ───────────────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = paper,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (message.isEmpty()) {
                        Text(
                            text = "What do you want to remember?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ink.copy(alpha = 0.32f)
                        )
                    }
                    BasicTextField(
                        value = message,
                        onValueChange = { message = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                        cursorBrush = SolidColor(brass),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── THE WAX SEAL ────────────────────────────────────────────
            val ready = message.isNotBlank() && landed == null
            Surface(
                onClick = {
                    if (ready) sealing = true
                },
                enabled = ready,
                shape = RoundedCornerShape(50),
                color = when {
                    landed != null -> brass
                    ready -> brass
                    // A disabled seal is the page's own surface: the button is
                    // not there yet, because there is nothing to seal.
                    else -> scheme.surfaceContainerHigh
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val squashed = 1f - sealPress.value * 0.06f
                        scaleX = squashed
                        scaleY = squashed
                    }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurioIcon(
                        if (landed != null) CurioIcons.Check else CurioIcons.Lock,
                        null,
                        tint = if (ready) Color.White else ink.copy(alpha = 0.35f),
                        size = 19.dp
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = when {
                            landed != null -> "Sealed · opens ${dayLabel(landed?.openAt ?: 0L)}"
                            ready -> "Seal it for ${span.label}"
                            else -> "Write something first"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (ready) Color.White else ink.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
        }

        // The press-and-land, the same two halves as the passport's stamp: the
        // squash is felt while the thumb is down, and the THUNK is on the land.
        //
        // `sealing` deliberately stays true for the whole press. Flipping it back
        // inside this effect would recompose the `if` that guards it away and
        // cancel the coroutine before its own close ran — the page is leaving
        // anyway, so there is nothing to flip back to.
        if (sealing) {
            LaunchedEffect(sealing) {
                sealPress.animateTo(1f, CurioMotion.Springs.Snappy)
                val capsule = CurioTimeCapsules.seal(context, message, span)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                landed = capsule
                sealPress.snapTo(0f)
                // The confirmation reads for a beat, then the machine closes
                // itself — a sealed letter has nothing else to say.
                kotlinx.coroutines.delay(1400)
                onBack()
            }
        }
    }
}

/**
 * The dial: five notches across its top half, a slow ring of teeth behind them,
 * and a pointer at the chosen one.
 *
 * **The angle decides, not a hit target.** [dialIndexAt] maps wherever the finger
 * is to a notch, so a drag anywhere near the dial turns it — which is what makes
 * it feel like a dial rather than five buttons arranged in an arc.
 */
@Composable
private fun TimeDial(
    spans: List<String>,
    index: Int,
    onIndex: (Int) -> Unit,
    brass: Color,
    ink: Color
) {
    if (spans.size < 2) return
    var dialSize by remember { mutableStateOf(Size.Zero) }
    // The teeth turn on their own so the machine reads as running. One infinite
    // rotation, and it is the only thing on this page that moves by itself.
    val drift = rememberInfiniteTransition(label = "capsuleDial")
    val spin by drift.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing)
        ),
        label = "capsuleDialSpin"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(186.dp)
                .onSizeChanged { dialSize = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(spans.size) {
                    // `size` here is the CANVAS's own size (a PointerInputScope
                    // member), so the centre is known without threading it in.
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    detectDragGestures(
                        onDragStart = { at -> onIndex(dialIndexAt(at, centre, spans.size)) },
                        onDrag = { change, _ ->
                            change.consume()
                            onIndex(dialIndexAt(change.position, centre, spans.size))
                        }
                    )
                }
        ) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // The teeth: 48 short radial lines turning slowly behind the dial.
            drawContext.canvas.save()
            drawContext.canvas.rotate(spin, centre)
            repeat(48) { i ->
                val a = Math.toRadians((i * (360f / 48f)).toDouble())
                val from = Offset(
                    centre.x + cos(a).toFloat() * (radius - 7f),
                    centre.y + sin(a).toFloat() * (radius - 7f)
                )
                val to = Offset(
                    centre.x + cos(a).toFloat() * (radius - 1f),
                    centre.y + sin(a).toFloat() * (radius - 1f)
                )
                drawLine(
                    color = brass.copy(alpha = 0.30f),
                    start = from,
                    end = to,
                    strokeWidth = 2f
                )
            }
            drawContext.canvas.restore()

            // The face, then the travelled arc up to the chosen notch.
            drawCircle(
                color = brass.copy(alpha = 0.16f),
                radius = radius - 12f,
                center = centre
            )
            drawCircle(
                color = brass.copy(alpha = 0.55f),
                radius = radius - 12f,
                center = centre,
                style = Stroke(width = 2f)
            )
            val startDeg = DIAL_START_DEG
            val endDeg = DIAL_START_DEG + (index.toFloat() / (spans.size - 1)) * DIAL_SWEEP_DEG
            drawArc(
                color = brass,
                startAngle = startDeg,
                sweepAngle = endDeg - startDeg,
                useCenter = false,
                topLeft = Offset(centre.x - (radius - 22f), centre.y - (radius - 22f)),
                size = Size((radius - 22f) * 2f, (radius - 22f) * 2f),
                style = Stroke(width = 5f)
            )

            // The notches, and the pointer on the chosen one.
            spans.indices.forEach { i ->
                val deg = DIAL_START_DEG + (i.toFloat() / (spans.size - 1)) * DIAL_SWEEP_DEG
                val a = Math.toRadians(deg.toDouble())
                val selected = i == index
                val r = radius - 12f
                val at = Offset(
                    centre.x + cos(a).toFloat() * r,
                    centre.y + sin(a).toFloat() * r
                )
                drawCircle(
                    color = if (selected) brass else brass.copy(alpha = 0.45f),
                    radius = if (selected) 7f else 3.5f,
                    center = at
                )
                if (selected) {
                    drawCircle(
                        color = brass.copy(alpha = 0.35f),
                        radius = 13f,
                        center = at
                    )
                }
            }
        }

        // The chosen span, named in the middle of its own dial.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = spans[index.coerceIn(0, spans.size - 1)],
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink
            )
            Text(
                text = "HOW FAR AHEAD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                ),
                color = ink.copy(alpha = 0.45f)
            )
        }
    }
}

/** The dial's arc: from due WEST, sweeping clockwise over the top, to due EAST. */
private const val DIAL_START_DEG = 180f
private const val DIAL_SWEEP_DEG = 180f

/**
 * Which notch the finger is on, from its ANGLE alone.
 *
 * The dial's arc is the top half of the circle, so a point below the centre is
 * outside it and clamps to the nearer END rather than to the nearest notch —
 * dragging off the bottom of a dial must not teleport the pointer to the middle
 * of its travel. `atan2`'s 0° is east and its angles run clockwise on screen
 * (y is down), so 180°…360° is exactly the top half as drawn.
 */
private fun dialIndexAt(point: Offset, centre: Offset, notches: Int): Int {
    val deg = Math.toDegrees(
        kotlin.math.atan2((point.y - centre.y).toDouble(), (point.x - centre.x).toDouble())
    ).toFloat().let { if (it < 0f) it + 360f else it }
    if (deg < DIAL_START_DEG) {
        // Below the dial's arc: the east end is at >270°, so anything in the
        // lower-RIGHT quadrant belongs to the last notch and anything in the
        // lower-LEFT to the first.
        return if (deg < 90f) notches - 1 else 0
    }
    val t = ((deg - DIAL_START_DEG) / DIAL_SWEEP_DEG).coerceIn(0f, 1f)
    return (t * (notches - 1)).roundToInt().coerceIn(0, notches - 1)
}

/** "12 March 2027" — the day a letter lands, in the member's own locale. */
private fun dayLabel(at: Long): String =
    SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(at))

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
                    text = "YOU, ${capsule.spanLabel.uppercase()} AGO",
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

package com.curio.app.features.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioNamedTheme
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v428 — THE JOURNAL'S OWN COLOUR.
 *
 * The member: *"make the journal theme[-]not[-]aware and it have its own theme
 * if user wants to change ... and by default they follow the theme only the
 * changed colour stays as it looks"*. So a page is coloured by the app's theme
 * UNLESS the member gives it one of its own — and when they do, that exact
 * colour is what the page's doors wear, on whatever theme they are on later.
 *
 * The stored value is [PersonalNoteEntity.accentArgb], where **0 means follow
 * the theme**: nothing here invents a default, because the honest default for
 * every journal ever written is the theme it was already wearing.
 *
 * WHAT WEARS IT (the member's own answer): Home's journal chips, the journal
 * list's spine, and the palette button in the page's own tools — i.e. every
 * place a page is a DOOR rather than a page.
 */

/** Follow the app's theme — the value every page starts life with. */
internal const val JOURNAL_ACCENT_THEME = 0

/**
 * The colour a page's door wears: its own when it has one, and otherwise the
 * app's accent INK — the measured, readable accent (see [personalAccentInk]),
 * which is the tone a spine or a chip wants rather than a fill.
 */
@Composable
internal fun journalDoorAccent(argb: Int): Color =
    if (argb == JOURNAL_ACCENT_THEME) personalAccentInk() else Color(argb)

/**
 * THE APP'S OWN HUES — the theme picker's own five, plus the rose Curio wears
 * when no named theme is on, each read through the theme's own measurements
 * ([CurioNamedTheme.heroFor]) so a swatch here IS the colour the app would
 * paint with, not a second guess at it.
 */
@Composable
internal fun journalHueChoices(): List<Pair<String, Color>> {
    val dark = isCurioDarkTheme()
    return buildList {
        add("Rose" to settingsRoseAccent())
        CurioNamedTheme.entries.forEach { theme ->
            add(theme.label to theme.heroFor(dark))
        }
    }
}

/**
 * The picker. It applies AS THE MEMBER MOVES — the door in the dock wears the
 * colour while the wheel is being dragged, which is the only way to know what a
 * colour looks like before committing to it — and the page's own debounced
 * writer is what persists it (see `PersonalPageMeta.accentArgb`), so a drag is
 * one write, not one per pixel.
 *
 * v443 — AND THE PAGE IS NOT ONE OF THE THINGS IT COLOURS. v429 offered a "Paint
 * the page too" switch here; the member has since asked for it to go (*"in
 * journal the paint the page remove that option"*) and, asked what removing it
 * should do, chose **never paint the page**. The sheet therefore sets the DOORS'
 * colour and nothing else — Home's chips, the list's spine, the palette door in
 * the dock — so the colour can never become something the member has to read
 * their own writing against.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalAccentSheet(
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hues = journalHueChoices()
    // The wheel keeps its own HS, seeded from the colour in hand (the theme's
    // accent when the page follows it, so the wheel opens ON the colour the
    // page is wearing rather than on a red that means nothing yet).
    val accent = personalAccent()
    val seed = remember(current) {
        val base = if (current == JOURNAL_ACCENT_THEME) accent else Color(current)
        FloatArray(3).also {
            android.graphics.Color.colorToHSV(base.toArgb(), it)
        }
    }
    var hue by remember(seed) { mutableFloatStateOf(seed[0]) }
    var sat by remember(seed) { mutableFloatStateOf(seed[1]) }
    var value by remember(seed) { mutableFloatStateOf(seed[2]) }
    // The brightness the wheel is drawn at, so the wheel's own disc shows the
    // colour being chosen rather than only its most lit version.
    val picked = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "This journal's colour",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            // ── FOLLOW THE THEME, THEN THE APP'S OWN HUES ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccentSwatch(
                    label = "Theme",
                    color = accent,
                    selected = current == JOURNAL_ACCENT_THEME,
                    modifier = Modifier.weight(1f)
                ) { onPick(JOURNAL_ACCENT_THEME) }
                hues.forEach { (label, color) ->
                    val argb = color.toArgb()
                    AccentSwatch(
                        label = label,
                        color = color,
                        selected = current == argb,
                        modifier = Modifier.weight(1f)
                    ) { onPick(argb) }
                }
            }
            // ── ANY COLOUR AT ALL ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ColorWheel(
                    hue = hue,
                    sat = sat,
                    value = value,
                    onPick = { nextHue, nextSat ->
                        hue = nextHue
                        sat = nextSat
                        onPick(
                            android.graphics.Color.HSVToColor(floatArrayOf(nextHue, nextSat, value))
                        )
                    }
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // The colour in hand, as the member has made it: the wheel's
                    // own reading, right beside the control that makes it.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(picked)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(14.dp)
                            )
                    )
                    Slider(
                        value = value,
                        onValueChange = { next ->
                            value = next
                            onPick(
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, next))
                            )
                        }
                    )
                }
            }
        }
    }
}

/** One of the app's hues (or the theme itself) as a tappable round swatch. */
@Composable
private fun AccentSwatch(
    label: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .clickable(onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                // v443 — THE TICK IS MEASURED AGAINST THE SWATCH IT SITS ON.
                // It asked [settingsReadableInk], which answers from the THEME
                // and never looks at the colour handed to it — so the tick on a
                // member's dark swatch was the theme's dark ink on dark, which is
                // the "weird unreadable text" this sheet was reported for. A
                // swatch IS the arbitrary fill, so it asks the measurement.
                CurioIcon(
                    CurioIcons.Check,
                    null,
                    tint = journalInkOn(color),
                    size = 18.dp
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * THE WHEEL: hue around the rim, saturation towards the middle, and the value
 * the disc is drawn at — so what the finger is on is the colour it will give.
 *
 * A sweep gradient supplies the rim's hues and a white-to-clear radial gradient
 * the saturation falloff; the value is a black wash over both. Touching it
 * anywhere picks (single pointer, no slop requirement), which is the behaviour a
 * colour wheel has everywhere else.
 */
@Composable
private fun ColorWheel(
    hue: Float,
    sat: Float,
    value: Float,
    onPick: (hue: Float, sat: Float) -> Unit
) {
    val hues = remember {
        List(37) { step ->
            val at = step / 36f
            Color(android.graphics.Color.HSVToColor(floatArrayOf(at * 360f, 1f, 1f)))
        }
    }
    Canvas(
        modifier = Modifier
            .size(132.dp)
            // A Canvas carries no label of its own (see the app's own note on
            // this in [CaptureFormatComponents]), so it is given one.
            .semantics { contentDescription = "Colour wheel" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onPick(angleOf(down.position, size.width, size.height), radiusOf(down.position, size.width, size.height))
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        onPick(
                            angleOf(change.position, size.width, size.height),
                            radiusOf(change.position, size.width, size.height)
                        )
                        change.consume()
                    }
                }
            }
    ) {
        val radius = size.minDimension / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.sweepGradient(hues, center = centre),
            radius = radius,
            center = centre
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = centre,
                radius = radius
            ),
            radius = radius,
            center = centre
        )
        if (value < 1f) {
            drawCircle(
                color = Color.Black.copy(alpha = 1f - value),
                radius = radius,
                center = centre
            )
        }
        // Where the finger is: the picked colour sitting on the wheel, with the
        // ink that reads on it as its ring.
        val at = Offset(
            centre.x + (radius * sat) * kotlin.math.cos(Math.toRadians(hue.toDouble())).toFloat(),
            centre.y + (radius * sat) * kotlin.math.sin(Math.toRadians(hue.toDouble())).toFloat()
        )
        drawCircle(
            color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))),
            radius = 11.dp.toPx(),
            center = at
        )
        drawCircle(
            color = Color.White,
            radius = 11.dp.toPx(),
            center = at,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/** The wheel's hue for a point: 0° at 3 o'clock, sweeping the way the gradient
 *  does. */
private fun angleOf(position: Offset, width: Int, height: Int): Float {
    val dx = position.x - width / 2f
    val dy = position.y - height / 2f
    val degrees = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    return (degrees + 360f) % 360f
}

/** The wheel's saturation for a point: the centre is 0, the rim 1. */
private fun radiusOf(position: Offset, width: Int, height: Int): Float {
    val dx = position.x - width / 2f
    val dy = position.y - height / 2f
    val radius = kotlin.math.min(width, height) / 2f
    if (radius <= 0f) return 0f
    return (kotlin.math.sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
}

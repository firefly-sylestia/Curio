package com.curio.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.ExploreSession
import com.curio.app.data.formatElapsed
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * The explore-timer bubble's visual content — kept SHORT on purpose: the
 * category glyph, the topic name and the live elapsed time on the compact
 * pill; the expanded panel adds icon-only controls, a shared session note,
 * a screenshot button, and a Finish action. No verb/target lines or
 * descriptions — those live in the done-prompt, not on a floating pill.
 *
 * Two states, swapped INSTANTLY (v27 — the old morph animation between the
 * pill and the panel was laggy on many devices and is gone):
 *  - **Minimized** (default): a compact capsule pill — category glyph chip,
 *    the topic name (long names slow-scroll/marquee), and a chronometer
 *    readout. Tapping it expands.
 *  - **Expanded**: a rounded card panel. Header (glyph + topic + elapsed +
 *    minimize chevron), then a compact ICON-ONLY control row — Pause /
 *    Resume, Hide, and Screenshot (with a count badge when shots exist) —
 *    then the shared session note field, then the Finish button that ends
 *    the session and opens the write-it-down page.
 *
 * Dragging lives HERE (Compose), not on the window: a system-overlay
 * ComposeView's composed child consumes every View-level touch, so a View
 * drag listener never fires. The pill reports raw drag deltas via [onDragBy]
 * and release via [onDragEnd] — the owning service moves the window. The
 * detector is slop-gated, so taps on the pill/buttons still land while real
 * drags reposition the bubble.
 *
 * Used by [com.curio.app.infrastructure.ExploreSessionService] inside a
 * system overlay window (`TYPE_APPLICATION_OVERLAY`), so it renders over
 * other apps — including the browser — while an explore session runs.
 * Pure presentation apart from the drag deltas, the transient minimize
 * state (which resets to small whenever the window is rebuilt) and the note
 * field's local draft (pushed live to the session store).
 *
 * Theme-aware: surfaces, ink and borders come from [MaterialTheme] (so the
 * bubble follows light / dark / AMOLED / Material styles) and the accent is
 * the session category's theme-resolved color. The live elapsed value ticks
 * once per second while NOT paused (pause freezes it via the session).
 */
@Composable
fun ExploreBubbleContent(
    session: ExploreSession,
    onTogglePause: () -> Unit,
    onHide: () -> Unit,
    // v27 — the note field writes the session's SHARED note live.
    onNoteChange: (String) -> Unit,
    // v27 — screenshot button: capture a frame (or launch the consent flow).
    onScreenshot: () -> Unit,
    // v27 — Finish button: end the session and open the write-it-down page.
    onFinish: () -> Unit,
    // v27 — focus changes on the note field; the service makes the overlay
    // window focusable (so the keyboard can type) and restores it on blur.
    onNoteFocusChange: (Boolean) -> Unit,
    onDragBy: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    // Called with the new pixel size right after an expand/collapse swap —
    // the service re-centers/clamps the window so the instant size change
    // reads as growth around the middle instead of a jump from the corner.
    // Timer ticks never forward (see [resizeBurst]).
    onSizeChanged: (wPx: Int, hPx: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val category = CurioCategories.byId(session.categoryId)
    val accent = category.themedAccent()
    val ink = category.categoryInk()
    // Pastel accents are intentionally airy for fills, but using that same
    // pale color for the pill outline makes the overlay disappear against
    // light surfaces. Use the category's resolved ink as the stronger edge
    // and keep the original accent treatment outside pastel mode.
    val pillBorderColor = if (AppPreferences.pastelColorsState) ink else accent

    // Live elapsed — recomputed every second while NOT paused. When paused
    // the value freezes (session.elapsedMillis handles the freeze itself).
    var now by remember(session.paused, session.startMillis) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(session.paused, session.startMillis) {
        if (session.paused) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsed = session.elapsedMillis(now)

    // Minimized by default — a compact chip + timer pill. Expanded shows the
    // full controls. Transient UI state: whenever the window is rebuilt
    // (hide → re-show, service restart) the bubble comes back small.
    var minimized by remember { mutableStateOf(true) }

    // v27 — the shared note's draft. Kept as LOCAL state so the field never
    // fights the store's recomposition (the session object changes on every
    // keystroke, which would otherwise reset the cursor); each change is
    // pushed live via [onNoteChange]. Re-seeded when the window is rebuilt
    // for a different session.
    var noteDraft by remember(session.topicName, session.startMillis) {
        mutableStateOf(session.note)
    }

    // v27 — instant swap, no morph. The service still re-centers the window
    // once after the size changes: forward size callbacks for a short burst
    // after each expand/collapse toggle (NOT on the per-second timer tick,
    // which would nudge the window every second).
    var resizeBurst by remember { mutableStateOf(false) }
    LaunchedEffect(minimized) {
        resizeBurst = true
        delay(RESIZE_BURST_MS)
        resizeBurst = false
    }

    // Drag — slop-gated Compose detector: taps on the pill/buttons still
    // land, real drags report deltas for the service to move the window.
    // Placed OUTER to the clickable so drags win over taps (a clickable that
    // consumes the up cancels the tap, and a consumed down/move cancels it).
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(onDragEnd = { onDragEnd() }) { change, dragAmount ->
            change.consume()
            onDragBy(dragAmount.x, dragAmount.y)
        }
    }

    Surface(
        shape = RoundedCornerShape(if (minimized) PILL_CORNER_RADIUS else PANEL_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            pillBorderColor.copy(alpha = if (AppPreferences.pastelColorsState) 0.78f else 0.50f)
        ),
        // No elevation shadow: the old 8dp shadow rendered BEYOND the
        // overlay window's bounds and the window clipped it into a hard,
        // boxy edge around the pill. The crisp accent border carries the
        // definition now.
        shadowElevation = 0.dp,
        modifier = modifier
            .then(dragModifier)
            .onSizeChanged { size ->
                // Only during the post-toggle burst — timer ticks change the
                // pill width by a pixel and must not move the window.
                if (resizeBurst) onSizeChanged(size.width, size.height)
            }
            // The minimized pill is tappable anywhere to expand; the expanded
            // bubble's buttons handle their own input. Applied conditionally
            // so the expanded bubble carries no dead clickable semantics.
            .then(if (minimized) Modifier.clickable { minimized = false } else Modifier)
    ) {
        if (minimized) {
            MinimizedPill(
                session = session,
                category = category,
                accent = accent,
                ink = ink,
                elapsed = elapsed,
                onExpand = { minimized = false }
            )
        } else {
            ExpandedPanel(
                session = session,
                category = category,
                accent = accent,
                ink = ink,
                elapsed = elapsed,
                noteDraft = noteDraft,
                onTogglePause = onTogglePause,
                onHide = onHide,
                onNoteChange = { note ->
                    noteDraft = note
                    onNoteChange(note)
                },
                onNoteFocusChange = onNoteFocusChange,
                onScreenshot = onScreenshot,
                onFinish = onFinish,
                onMinimize = { minimized = true }
            )
        }
    }
}

/** The compact capsule pill — glyph chip + scrolling topic + compact timer. */
@Composable
private fun MinimizedPill(
    session: ExploreSession,
    category: CurioCategory,
    accent: Color,
    ink: Color,
    elapsed: Long,
    onExpand: () -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CategoryGlyphChip(category = category, accent = accent, ink = ink)

        // Topic + live timer — the topic caps at [MINIMIZED_TOPIC_WIDTH] and
        // slow-scrolls (marquee) when it's longer, so the pill stays small.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = MINIMIZED_TOPIC_WIDTH)
        ) {
            MarqueeTopicText(
                text = session.topicName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (AppPreferences.pastelColorsState) ink
                        else MaterialTheme.colorScheme.onSurface,
                maxWidth = MINIMIZED_TOPIC_WIDTH,
                paused = session.paused
            )
            Text(
                text = if (session.paused) "Paused · ${compactElapsed(elapsed)}"
                       else compactElapsed(elapsed),
                style = MaterialTheme.typography.labelSmall,
                color = if (session.paused) {
                    if (AppPreferences.pastelColorsState) ink else accent
                } else if (AppPreferences.pastelColorsState) {
                    ink.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // ── Expand to the full controls ────────────────────────────
        BubbleIconButton(
            icon = CurioIcons.KeyboardArrowUp,
            contentDescription = "Expand timer",
            tint = if (AppPreferences.pastelColorsState) ink else accent,
            onClick = onExpand
        )
    }
}

/**
 * The expanded card panel — header (glyph chip + topic + elapsed + Minimize
 * chevron), an ICON-ONLY control row (Pause/Resume, Hide, Screenshot), the
 * shared session note field, and the Finish button. Deliberately a rounded
 * rectangle, not a capsule, so it never reads as a circle.
 */
@Composable
private fun ExpandedPanel(
    session: ExploreSession,
    category: CurioCategory,
    accent: Color,
    ink: Color,
    elapsed: Long,
    noteDraft: String,
    onTogglePause: () -> Unit,
    onHide: () -> Unit,
    onNoteChange: (String) -> Unit,
    onNoteFocusChange: (Boolean) -> Unit,
    onScreenshot: () -> Unit,
    onFinish: () -> Unit,
    onMinimize: () -> Unit
) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header: glyph chip + topic + elapsed + minimize ────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CategoryGlyphChip(category = category, accent = accent, ink = ink)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = EXPANDED_TOPIC_WIDTH)
            ) {
                MarqueeTopicText(
                    text = session.topicName,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (AppPreferences.pastelColorsState) ink
                            else MaterialTheme.colorScheme.onSurface,
                    maxWidth = EXPANDED_TOPIC_WIDTH,
                    paused = session.paused
                )
                Text(
                    text = if (session.paused) "Paused · ${formatElapsed(elapsed)}"
                           else formatElapsed(elapsed),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (session.paused) accent
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BubbleIconButton(
                icon = CurioIcons.KeyboardArrowDown,
                contentDescription = "Minimize timer",
                tint = if (AppPreferences.pastelColorsState) ink
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onMinimize
            )
        }

        // ── Icon-only controls: Pause/Resume · Hide · Screenshot ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BubbleIconButton(
                icon = if (session.paused) CurioIcons.PlayArrow else CurioIcons.Pause,
                contentDescription = if (session.paused) "Resume timer" else "Pause timer",
                tint = if (AppPreferences.pastelColorsState) ink else accent,
                onClick = onTogglePause
            )
            BubbleIconButton(
                icon = CurioIcons.Close,
                contentDescription = "Hide timer",
                tint = if (AppPreferences.pastelColorsState) ink
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onHide
            )
            // Screenshot — shows a count badge once shots exist.
            Box {
                BubbleIconButton(
                    icon = CurioIcons.Screenshot,
                    contentDescription = "Capture a screenshot for this session",
                    tint = if (AppPreferences.pastelColorsState) ink else accent,
                    onClick = onScreenshot
                )
                if (session.screenshotPaths.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = session.screenshotPaths.size.coerceAtMost(9).toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.75f
                                ),
                                color = category.onAccent()
                            )
                        }
                    }
                }
            }
        }

        // ── Shared session note (v27) — one note per session, attached
        // to every entry saved from it. Local draft state (see
        // [ExploreBubbleContent.noteDraft]); each keystroke is pushed live
        // via [onNoteChange].
        NoteField(
            note = noteDraft,
            onChange = onNoteChange,
            onFocusChange = onNoteFocusChange,
            accent = accent,
            ink = ink
        )

        // ── Finish — end the session and open the write-it-down page.
        Surface(
            onClick = onFinish,
            shape = RoundedCornerShape(50),
            color = accent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CurioIcon(
                    name = CurioIcons.Check,
                    contentDescription = null,
                    tint = category.onAccent(),
                    size = 18.dp
                )
                Text(
                    text = "Finish & write it down",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = category.onAccent(),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

/**
 * The shared-note editor inside the expanded bubble. A compact rounded
 * BasicTextField that grows to 2 lines; focused state is reported to the
 * owning service so the overlay window can become focusable (the keyboard
 * only types into a focusable window).
 */
@Composable
private fun NoteField(
    note: String,
    onChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    accent: Color,
    ink: Color
) {
    val pastel = AppPreferences.pastelColorsState
    BasicTextField(
        value = note,
        onValueChange = { onChange(it.take(240)) },
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = if (pastel) ink else MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(accent),
        maxLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (pastel) accent.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .onFocusChanged { onFocusChange(it.isFocused) }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) { inner ->
        if (note.isEmpty()) {
            Text(
                text = "Session note…",
                style = MaterialTheme.typography.bodySmall,
                color = if (pastel) ink.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        inner()
    }
}

/** The small category-glyph circle shown at the pill's start. */
@Composable
private fun CategoryGlyphChip(
    category: CurioCategory,
    accent: Color,
    ink: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(
            name = category.iconGlyph,
            contentDescription = null,
            tint = ink,
            size = 18.dp
        )
    }
}

/**
 * Single-line topic text that slow-scrolls (marquee) when it's longer than
 * [maxWidth]: it holds at the start, glides left to reveal the full name,
 * holds, then glides back — so the complete topic is always readable inside
 * a small pill. Fits text simply sits still (no scrolling); while [paused]
 * the topic freezes at the start, matching the frozen timer.
 *
 * The visible box is `min(textWidth, maxWidth)` wide and clips; the text
 * inside is drawn at its full measured width and translated by the scroll
 * offset, so the overflowing tail actually appears instead of being
 * ellipsized away.
 */
@Composable
private fun MarqueeTopicText(
    text: String,
    style: TextStyle,
    color: Color,
    maxWidth: Dp,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textLayout = remember(text, style, density) {
        textMeasurer.measure(
            text = text,
            style = style,
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxLines = 1
        )
    }
    val textWidthPx = textLayout.size.width
    val capPx = with(density) { maxWidth.toPx() }.roundToInt()
    // Text glyphs can have a small negative left side-bearing. The old
    // exact-width Text node clipped that bearing during the first marquee
    // frame, making the opening letters look cut off. Keep a tiny breathing
    // inset inside the mask and let the outer Box do the only clipping.
    val textInsetPx = with(density) { 2.dp.toPx() }.roundToInt()
    val boxWidthPx = minOf(textWidthPx + textInsetPx * 2, capPx)
    val visibleTextWidthPx = (boxWidthPx - textInsetPx * 2).coerceAtLeast(0)

    val scrollX = remember { Animatable(0f) }
    val scrollDistance = (textWidthPx - visibleTextWidthPx).coerceAtLeast(0)
    LaunchedEffect(scrollDistance, text, paused) {
        scrollX.snapTo(0f)
        if (paused || scrollDistance <= 0) return@LaunchedEffect
        // Cap the one-way glide (~12s) so an absurdly long topic never
        // crawls; the speed constant already makes the normal case slow.
        val travelMs = (scrollDistance / MARQUEE_PX_PER_MS).toInt().coerceIn(1, 12_000)
        while (true) {
            delay(MARQUEE_START_HOLD_MS)
            scrollX.animateTo(scrollDistance.toFloat(), tween(travelMs, easing = LinearEasing))
            delay(MARQUEE_END_HOLD_MS)
            scrollX.animateTo(0f, tween(travelMs, easing = LinearEasing))
            delay(MARQUEE_END_HOLD_MS)
        }
    }

    Box(
        modifier = modifier
            .requiredWidth(with(density) { boxWidthPx.toDp() })
            .clipToBounds()
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            // The outer Box is the marquee mask. Keeping overflow visible on
            // this inner node prevents its exact measured width from cutting
            // the first glyph before the mask gets a chance to draw it.
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .requiredWidth(with(density) { (textWidthPx + textInsetPx * 2).toDp() })
                .graphicsLayer {
                    translationX = textInsetPx.toFloat() - scrollX.value
                }
        )
    }
}

/** Small circular icon button used by the bubble's controls. */
@Composable
private fun BubbleIconButton(
    icon: String,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            1.dp,
            tint.copy(alpha = if (AppPreferences.pastelColorsState) 0.58f else 0.35f)
        ),
        shadowElevation = 0.dp
    ) {
        CurioIcon(
            name = icon,
            contentDescription = contentDescription,
            tint = tint,
            size = 20.dp,
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * Compact chronometer-style reading for the minimized pill ("12:34",
 * "1:02:34") — tighter than the friendly [formatElapsed] ("12m 5s") so the
 * small bubble stays small.
 */
private fun compactElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
    } else {
        "%02d:%02d".format(Locale.US, minutes, seconds)
    }
}

// ── Tuning constants ────────────────────────────────────────────────────
// Corner radii: a near-capsule pill when minimized (24dp ≈ the pill's
// half-height, so the ends stay fully rounded) and a refined card when
// expanded.
private val PILL_CORNER_RADIUS = 24.dp
private val PANEL_CORNER_RADIUS = 18.dp

// Topic area width caps: tight in the minimized pill, roomier in the
// expanded panel. Longer topics slow-scroll within these bounds.
private val MINIMIZED_TOPIC_WIDTH = 110.dp
private val EXPANDED_TOPIC_WIDTH = 160.dp

// Marquee tuning — a slow ticker (~42 px/s) that holds briefly at each end
// before gliding back, so the full topic name reveals itself at a readable
// pace without feeling restless.
private const val MARQUEE_PX_PER_MS = 0.042f
private const val MARQUEE_START_HOLD_MS = 900L
private const val MARQUEE_END_HOLD_MS = 1_100L

// v27 — how long size callbacks forward after an expand/collapse toggle
// (long enough for the instant swap + one window relayout to land, short
// enough that a per-second timer tick can never slip into it).
private const val RESIZE_BURST_MS = 120L

package com.curio.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.unit.sp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.ExploreSession
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
 * and a Finish action. No verb/target lines or
 * descriptions — those live in the done-prompt, not on a floating pill.
 *
 * Two states, morphed smoothly (v253 restored the animation the v27 instant
 * swap removed — one shared spring drives the container size while the
 * contents crossfade/scale):
 *  - **Minimized** (default): JUST the category glyph in a small circle —
 *    no topic, no timer — so the resting bubble takes the least possible
 *    space over what the user is watching. Left idle at a snapped edge it
 *    docks into the screen edge assistive-touch-style (a peek sliver);
 *    touching it slides it back out.
 *  - **Expanded**: a rounded card panel. Header (glyph + topic + elapsed +
 *    minimize chevron), then a compact ICON-ONLY control row — Pause /
 *    Resume, Hide, Cancel —
 *    then the shared session note field, then the Finish action that ends
 *    the session and opens the write-it-down page. Left untouched (and not
 *    typing) it auto-collapses back to the pill so it never keeps covering
 *    the thing being watched.
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
    // v253 — which horizontal edge the window last snapped to (-1 left,
    // 1 right, 0 unknown), owned by the service; drives the edge dock.
    edgeSnap: MutableState<Int>,
    onTogglePause: () -> Unit,
    onHide: () -> Unit,
    // v27 — the note field writes the session's SHARED note live.
    onNoteChange: (String) -> Unit,
    // v27 — Finish button: end the session and open the write-it-down page.
    onFinish: () -> Unit,
    // v226 — Cancel button: end the session WITHOUT the write-it-down page
    // (the session is stashed for Home's cancelled-explore recovery row).
    onCancel: () -> Unit,
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

    // Minimized by default — a compact icon pill. Expanded shows the full
    // controls. Transient UI state: whenever the window is rebuilt
    // (hide → re-show, service restart) the bubble comes back small.
    var minimized by remember { mutableStateOf(true) }

    // v253 — whether the note field currently owns the keyboard. While it
    // does, the panel must never auto-collapse under the user's hands.
    var noteFocused by remember { mutableStateOf(false) }

    // v253 — interaction clock for BOTH idle behaviors (panel auto-collapse
    // and edge docking). The observe-only pointerInput below pokes it on
    // every touch, so any contact resets both timers.
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // v262 — EDGE DOCK REMOVED (user request): the pill stays exactly where
    // it is dropped — no snapping to edges, no assistive-touch parking.

    // v253 — an untouched expanded panel folds back to the pill so it never
    // keeps covering what the user is watching. Typing (noteFocused) or any
    // touch keeps it open.
    LaunchedEffect(minimized, noteFocused) {
        if (minimized || noteFocused) return@LaunchedEffect
        while (true) {
            delay(1_000)
            if (System.currentTimeMillis() - lastInteraction >= AUTO_COLLAPSE_MS) {
                minimized = true
                break
            }
        }
    }

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

    // v263 — the expanded panel does NOT carry a whole-surface drag detector:
    // dragging from any pixel (including over the note field and buttons)
    // fought the controls and read as glitchy. The pill drags anywhere; the
    // panel drags ONLY from its header row (same gesture, shared callbacks).
    // The key restarts the detector when minimized flips so a stale scope
    // never eats touches after an expand/collapse.
    val panelHeaderDragModifier = Modifier.pointerInput(minimized) {
        if (!minimized) {
            detectDragGestures(onDragEnd = { onDragEnd() }) { change, dragAmount ->
                change.consume()
                onDragBy(dragAmount.x, dragAmount.y)
            }
        }
    }

    // v253 — observe-only pointer pass: pokes the interaction clock on every
    // touch (resetting auto-collapse AND the dock timer), tracks raw press
    // state for the pill's touch squish, and undocks a docked pill the
    // instant it's contacted.
    var pillPressed by remember { mutableStateOf(false) }
    val interactionModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val anyPressed = event.changes.any { it.pressed }
                if (anyPressed) {
                    lastInteraction = System.currentTimeMillis()
                    if (!pillPressed) pillPressed = true
                } else if (pillPressed) {
                    pillPressed = false
                }
            }
        }
    }

    // v27 → v253 — smooth morph instead of the instant swap: one shared
    // spring drives the container size (SizeTransform), while the outgoing
    // state fades/scales down and the incoming one fades/scales up — the
    // pill visibly grows into the panel around its center.
    val cornerRadius by animateDpAsState(
        targetValue = if (minimized) PILL_CORNER_RADIUS else PANEL_CORNER_RADIUS,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "bubbleCorner"
    )

    // v263 — tactile press feedback on the pill: a gentle 0.94 squish while
    // touched (driven by the raw press state from [interactionModifier]), so
    // touching the bubble feels alive before any action fires.
    val pressedScale by animateFloatAsState(
        targetValue = if (pillPressed && minimized) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "bubblePress"
    )
    // v264 — the expanded panel's hairline rim color (hoisted: the draw
    // lambda below isn't a composable context).
    val panelRim = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        // v262 — NO glass here (user request): an overlay window can't
        // sample what's behind it anyway, so the bubble wears its plain,
        // opaque elevated surface.
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        // Overlay windows clip elevation shadows into a hard, boxy edge
        // around the pill, so the bubble stays flat — its definition
        // comes from the container step + accent glow, not a shadow.
        shadowElevation = 0.dp,
        modifier = modifier
            .then(interactionModifier)
            // v263 — whole-surface drag only while MINIMIZED (the pill drags
            // anywhere); expanded drags live on the panel's header row via
            // [panelHeaderDragModifier], so buttons + the note field are
            // never fighting a move gesture.
            .then(if (minimized) dragModifier else Modifier)
            .onSizeChanged { size ->
                // Only during the post-toggle burst — timer ticks change the
                // pill width by a pixel and must not move the window.
                if (resizeBurst) onSizeChanged(size.width, size.height)
            }
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            // v264 — EXPANDED PANEL DEPTH: a domed sheen (light catching the
            // top, shade pooling at the bottom) plus a hairline rim, so the
            // card reads as raised glass-like depth instead of a flat sheet.
            .drawWithContent {
                drawContent()
                val r = CornerRadius(cornerRadius.toPx())
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f)
                        )
                    ),
                    cornerRadius = r
                )
                drawRoundRect(
                    color = panelRim,
                    cornerRadius = r,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
            // The minimized pill is tappable anywhere to expand; the expanded
            // bubble's buttons handle their own input. Applied conditionally
            // so the expanded bubble carries no dead clickable semantics.
            .then(if (minimized) Modifier.clickable { minimized = false } else Modifier)
    ) {
        // v257 — GLITCH FIX: the old AnimatedContent morph animated the
        // container's SIZE every frame while the owning service re-centered
        // the WRAP_CONTENT overlay window on every size callback — two
        // animation loops fighting over the window geometry, which read as a
        // jittery expand. Now the content crossfades while the corner radius
        // springs; the window itself resizes in ONE step (the panel has a
        // FIXED width and the pill is fixed too), so the service sees one or
        // two size callbacks instead of forty.
// v260 — fade + gentle scale beats a bare crossfade: the incoming state
        // grows in from 92% like iOS, and since both states have FIXED sizes
        // there is no geometry fight with the window resize.
        AnimatedContent(
            targetState = minimized,
            transitionSpec = {
                // v263 — one shared spring drives BOTH directions (no mixed
                // tween/spring timing): the incoming state pops in with a
                // slight overshoot while the outgoing settles back. The
                // window still resizes in ONE deterministic step, so there is
                // no geometry fight — this spring only shapes the content,
                // matching the corner-radius spring's feel.
                (fadeIn(tween(120)) +
                    scaleIn(
                        initialScale = 0.9f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f)
                    )) togetherWith
                    (fadeOut(tween(130)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(130)))
            },
            label = "bubbleExpand"
        ) { m ->
            if (m) {
        MinimizedPill(
            category = category,
            accent = accent,
            ink = ink,
            paused = session.paused,
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
                headerDrag = panelHeaderDragModifier,
                onTogglePause = onTogglePause,
                onHide = onHide,
                onNoteChange = { note ->
                    noteDraft = note
                    onNoteChange(note)
                },
                onNoteFocusChange = { focused ->
                    noteFocused = focused
                    onNoteFocusChange(focused)
                },
                onFinish = onFinish,
                onCancel = onCancel,
                onMinimize = { minimized = true }
                )
            }
        }
    }
}

/**
 * The collapsed pill — v253: JUST the category glyph in a small circle.
 * No topic, no timer (tap to see them in the panel), so the resting bubble
 * takes the least possible space over whatever the user is watching. While
 * paused the glyph swaps to the pause mark and the accent ring drops to a
 * neutral outline, so the frozen state reads at a glance.
 */
@Composable
private fun MinimizedPill(
    category: CurioCategory,
    accent: Color,
    ink: Color,
    paused: Boolean,
    onExpand: () -> Unit
) {
    val baseFill = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(46.dp)
            // v264 — 3D DEPTH + BROAD GLOW RING. Overlay windows clip real
            // elevation into a hard boxy edge, so the depth is painted:
            // a soft drop shadow (offset down), the category glow as a WIDE
            // halo disc behind the pill, and a domed radial fill (light
            // top-start → base) with a bottom inner shade — the pill reads
            // as a raised, lit bubble instead of a flat circle.
            .drawBehind {
                val r = size.minDimension / 2f
                drawCircle(
                    Color.Black.copy(alpha = 0.30f),
                    radius = r + 1.5f.dp.toPx(),
                    center = center + Offset(0f, 2.5f.dp.toPx())
                )
                drawCircle(accent.copy(alpha = 0.22f), radius = r + 5f.dp.toPx())
                drawCircle(accent.copy(alpha = 0.12f), radius = r + 9f.dp.toPx())
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        lerp(baseFill, Color.White, 0.22f),
                        baseFill,
                        lerp(baseFill, Color.Black, 0.16f)
                    ),
                    center = Offset(size.width * 0.34f, size.height * 0.28f),
                    radius = size.minDimension * 0.95f
                )
            )
            .border(
                width = 3.dp,
                brush = if (paused) Brush.sweepGradient(
                    listOf(
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) else Brush.sweepGradient(
                    listOf(accent, accent.copy(alpha = 0.45f), Color.White.copy(alpha = 0.65f), accent)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(
            name = if (paused) CurioIcons.Pause else category.iconGlyph,
            contentDescription = if (paused) "Paused — tap for timer"
                                else "Explore timer",
            tint = ink,
            size = 22.dp
        )
    }
}

/**
 * The expanded card panel — v257 REDESIGN with a FIXED width (the window
 * resizes in one deterministic step when expanding, which is what kills the
 * old morph glitch):
 *
 *   1. Header — glyph chip, marquee topic, minimize chevron.
 *   2. Big chronometer readout, centered, with a Paused tag when frozen.
 *   3. Three equal-weight labeled controls: Pause/Resume · Hide · Cancel.
 *   4. The shared session note field.
 *   5. The full-width Finish action.
 */
@Composable
private fun ExpandedPanel(
    session: ExploreSession,
    category: CurioCategory,
    accent: Color,
    ink: Color,
    elapsed: Long,
    noteDraft: String,
    // v263 — the panel's drag gesture lives on its HEADER row only, so the
    // note field and control buttons never fight the move gesture.
    headerDrag: Modifier = Modifier,
    onTogglePause: () -> Unit,
    onHide: () -> Unit,
    onNoteChange: (String) -> Unit,
    onNoteFocusChange: (Boolean) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onMinimize: () -> Unit
) {
    val pastel = AppPreferences.pastelColorsState
    Column(
        modifier = Modifier
            .width(PANEL_WIDTH)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header: glyph chip + topic + minimize ──────────────────
        // This row is also the panel's drag handle (see [headerDrag]).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = headerDrag
        ) {
            CategoryGlyphChip(category = category, accent = accent, ink = ink)
            MarqueeTopicText(
                text = session.topicName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (pastel) ink else MaterialTheme.colorScheme.onSurface,
                maxWidth = PANEL_TOPIC_WIDTH,
                paused = session.paused,
                modifier = Modifier.weight(1f, fill = false)
            )
            BubbleIconButton(
                icon = CurioIcons.KeyboardArrowDown,
                contentDescription = "Minimize timer",
                tint = if (pastel) ink else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onMinimize
            )
        }

        // ── Big chronometer readout — the panel's hero line ────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = compactElapsed(elapsed),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (pastel) ink else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (session.paused) "PAUSED" else "exploring ${session.topicName}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (session.paused) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = if (session.paused) accent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── Labeled controls: Pause/Resume · Hide · Cancel ─────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelControlButton(
                icon = if (session.paused) CurioIcons.PlayArrow else CurioIcons.Pause,
                label = if (session.paused) "Resume" else "Pause",
                tint = if (pastel) ink else accent,
                onClick = onTogglePause,
                modifier = Modifier.weight(1f)
            )
            PanelControlButton(
                icon = CurioIcons.Close,
                label = "Hide",
                tint = if (pastel) ink else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onHide,
                modifier = Modifier.weight(1f)
            )
            // v226 — Cancel ends the session outright (no write-it-down
            // page); Home keeps it recoverable via the cancelled-explore row.
            PanelControlButton(
                icon = CurioIcons.Delete,
                label = "Cancel",
                tint = MaterialTheme.colorScheme.error,
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
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
        onValueChange = { onChange(it.take(600)) },
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = if (pastel) ink else MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(accent),
        maxLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            // v257 — the panel now has a FIXED width, so fillMaxWidth
            // resolves to that width and the field can never collapse to a
            // sliver (the old min-width pin is gone).
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (pastel) accent.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .onFocusChanged { onFocusChange(it.isFocused) }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) { inner ->
        Box {
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
 * v257 — one equal-width control in the panel's labeled row: a soft tonal
 * pill with the glyph over its small caption, so Pause / Hide / Cancel read
 * by name instead of by guessing icons.
 */
@Composable
private fun PanelControlButton(
    icon: String,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CurioIcon(name = icon, contentDescription = null, tint = tint, size = 18.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = tint
            )
        }
    }
}

/**
 * Compact chronometer-style reading ("12:34", "1:02:34") — the big panel
 * readout and the docked pill both use it; tighter than the friendly
 * [com.curio.app.data.formatElapsed] ("12m 5s").
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
// Corner radii: a circle-ish collapsed pill (23dp ≈ the 46dp pill's radius
// minus padding) and a refined card when expanded.
private val PILL_CORNER_RADIUS = 23.dp
private val PANEL_CORNER_RADIUS = 18.dp

// v257 — the expanded panel has a FIXED width: expanding resizes the
// overlay window in ONE deterministic step (no per-frame morph), which is
// what kills the old expand glitch. PANEL_TOPIC_WIDTH caps the header's
// marquee topic inside it.
private val PANEL_WIDTH = 236.dp
private val PANEL_TOPIC_WIDTH = 118.dp

// Marquee tuning — a slow ticker (~42 px/s) that holds briefly at each end
// before gliding back, so the full topic name reveals itself at a readable
// pace without feeling restless.
private const val MARQUEE_PX_PER_MS = 0.042f
private const val MARQUEE_START_HOLD_MS = 900L
private const val MARQUEE_END_HOLD_MS = 1_100L

// v253 — how long size callbacks forward after an expand/collapse toggle:
// long enough for the full morph spring (~400ms) plus a window relayout,
// short enough that a per-second timer tick can never slip into it.
private const val RESIZE_BURST_MS = 600L

// v253 — idle windows for the two auto behaviors. An untouched panel folds
// back to the pill after this long (so it stops covering what you're
// watching); a collapsed pill docks into its snapped edge after this long.
private const val AUTO_COLLAPSE_MS = 12_000L

// v253 — how much of the docked pill stays visible at the screen edge.

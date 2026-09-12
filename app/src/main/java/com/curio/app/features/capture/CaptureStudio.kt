package com.curio.app.features.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CurioCategory
import com.curio.app.data.JournalMood
import com.curio.app.data.formatSessionShort
import com.curio.app.data.shortName
import com.curio.app.features.capture.formats.MoodChipsRow
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.formatGlyph
import com.curio.app.ui.components.rememberCurioPressSource
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.glyph
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent
import kotlinx.coroutines.launch

/** The live-recording indicator's dot — a plain alarm red, readable on every
 *  fill (the category accents can be warm enough to blur into it). */
private val StudioRecordRed = Color(0xFFE5484D)

/**
 * v3xx52 — CAPTURE STUDIO (Settings ▸ Experiments → Capture → *Take studio*).
 *
 * A designed workspace for the Save-your-take flow. The classic page stacks a
 * topic strip, a format-chip row, a take-tab row, the scrolling body and a
 * save tray — four chrome bands competing with the take. The studio instead
 * splits the page into a **hero** (who + how it felt), a **canvas** (the take
 * itself, and nothing else) and a **tray** (takes + tools + save), so the
 * paper notes own the middle of the screen.
 *
 * What is here and not in the classic page:
 *  - A tinted hero card with the lane medallion, the topic, the session
 *    duration and an inline mood row that expands under the card.
 *  - A take RAIL that lives on the bottom tray (thumb reach) instead of a row
 *    pinned under the topic — each pill springs when it becomes active.
 *  - Format + mood + tags moved into one **tools bottom sheet**, so the
 *    canvas is never crowded by pickers. The paper notes themselves are
 *    byte-identical: the sheet and the canvas both delegate to the same
 *    [FormatBodyForCategory] / [TagEditorRow] the classic page uses.
 *  - A live recording pulse (driven by the take's existing `busy` flag) on
 *    the hero, the active take pill and the save dock.
 *  - Spring-driven micro-motion throughout: the canvas flips on a take
 *    switch, the save dock pops when the take becomes savable, the format
 *    cards animate their selection.
 *
 * The experiment is OFF by default and the classic page is untouched when it
 * is — [SaveCaptureScreen] simply composes one or the other.
 */
@Composable
internal fun CaptureStudio(
    modifier: Modifier,
    cat: CurioCategory,
    topicName: String?,
    editMode: Boolean,
    sessionMillis: Long,
    tintWash: Boolean,
    sections: SnapshotStateList<CaptureSectionState>,
    activeIndex: Int,
    activeMood: JournalMood?,
    recording: Boolean,
    boardSeed: Int?,
    waitingForEntry: Boolean,
    canSave: Boolean,
    saveInProgress: Boolean,
    saveError: String?,
    hasNote: Boolean,
    noteExpanded: Boolean,
    note: String,
    tags: List<String>,
    tagInput: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSelectTake: (Int) -> Unit,
    onAddTake: () -> Unit,
    onRequestRemoveTake: (Int) -> Unit,
    onPickFormat: (CaptureFormat) -> Unit,
    onPickMood: (JournalMood?) -> Unit,
    onToggleNote: () -> Unit,
    onNoteChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onImageTap: (String) -> Unit
) {
    var toolsOpen by remember { mutableStateOf(false) }
    val activeSection = sections.getOrNull(activeIndex)
    val activeFormat = activeSection?.format ?: CaptureFormat.SoundBite

    Column(modifier = modifier) {
        StudioTopBar(
            editMode = editMode,
            onBack = onBack,
            onOpenTools = { toolsOpen = true }
        )
        StudioHero(
            cat = cat,
            topicName = topicName,
            sessionMillis = sessionMillis,
            mood = activeMood,
            recording = recording,
            tintWash = tintWash,
            onPickMood = onPickMood
        )
        StudioCanvas(
            cat = cat,
            sections = sections,
            activeIndex = activeIndex,
            boardSeed = boardSeed,
            waitingForEntry = waitingForEntry,
            tags = tags,
            tagInput = tagInput,
            hasNote = hasNote,
            noteExpanded = noteExpanded,
            note = note,
            onToggleNote = onToggleNote,
            onNoteChange = onNoteChange,
            onTagInputChange = onTagInputChange,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
            onImageTap = onImageTap,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        StudioTray(
            cat = cat,
            sections = sections,
            activeIndex = activeIndex,
            activeFormat = activeFormat,
            recording = recording,
            tintWash = tintWash,
            editMode = editMode,
            canSave = canSave,
            saveInProgress = saveInProgress,
            saveError = saveError,
            onSelectTake = onSelectTake,
            onAddTake = onAddTake,
            onRequestRemoveTake = onRequestRemoveTake,
            onOpenTools = { toolsOpen = true },
            onSave = onSave
        )
    }

    if (toolsOpen) {
        CaptureToolsSheet(
            cat = cat,
            sections = sections,
            activeIndex = activeIndex,
            tags = tags,
            tagInput = tagInput,
            onPickFormat = onPickFormat,
            onPickMood = onPickMood,
            onTagInputChange = onTagInputChange,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
            onDismiss = { toolsOpen = false }
        )
    }
}

/**
 * The studio's slim top bar: back, the page title, and one way into the tools
 * sheet. Deliberately quieter than the classic band — the hero below carries
 * the topic, so the title is just the flow's name.
 */
@Composable
private fun StudioTopBar(
    editMode: Boolean,
    onBack: () -> Unit,
    onOpenTools: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CurioBackButton(onClick = onBack)
        Text(
            text = if (editMode) "Edit entry" else "Save your take",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp)
        )
        Surface(
            onClick = onOpenTools,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.curioDarkGlow(2.dp, CircleShape)
        ) {
            CurioIcon(
                name = CurioIcons.Tune,
                contentDescription = "Take tools",
                tint = MaterialTheme.colorScheme.onSurface,
                size = 20.dp,
                modifier = Modifier.padding(9.dp)
            )
        }
    }
}

/**
 * The topic hero — the studio's identity block. Wears the lane medallion, the
 * topic, the session duration and the take's mood. Tapping the mood chip
 * expands the shared mood row INSIDE the card (no second chrome band), and a
 * live recording takes the chip's place with a pulsing dot.
 */
@Composable
private fun StudioHero(
    cat: CurioCategory,
    topicName: String?,
    sessionMillis: Long,
    mood: JournalMood?,
    recording: Boolean,
    tintWash: Boolean,
    onPickMood: (JournalMood?) -> Unit
) {
    val accent = cat.themedAccent()
    val ink = if (tintWash) cat.categoryInk() else MaterialTheme.colorScheme.onSurface
    val quietInk = if (tintWash) cat.categoryInk() else MaterialTheme.colorScheme.onSurfaceVariant
    val heroColor = if (tintWash) {
        cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    var moodOpen by remember { mutableStateOf(false) }

    // One-shot card entrance: a short rise + fade as the studio mounts. Reads
    // as the workspace arriving rather than a hard cut from the previous page.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(CurioMotion.Durations.Standard, easing = FastOutSlowInEasing))
    }

    Surface(
        color = heroColor,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .curioDarkGlow(4.dp, RoundedCornerShape(24.dp))
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 20f
            }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = lerp(heroColor, accent, if (tintWash) 0.24f else 0.18f)
                ) {
                    CurioIcon(
                        name = cat.iconGlyph,
                        contentDescription = null,
                        tint = ink,
                        size = 26.dp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = topicName ?: "Loading…",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = cat.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = quietInk.copy(alpha = 0.85f)
                        )
                        if (sessionMillis > 0L) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = quietInk.copy(alpha = 0.5f)
                            )
                            CurioIcon(
                                name = CurioIcons.Timer,
                                contentDescription = null,
                                tint = quietInk.copy(alpha = 0.7f),
                                size = 12.dp
                            )
                            Text(
                                text = formatSessionShort(sessionMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = quietInk.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                if (recording) {
                    StudioRecordingPill()
                } else {
                    Surface(
                        onClick = { moodOpen = !moodOpen },
                        shape = RoundedCornerShape(50),
                        color = lerp(heroColor, accent, if (moodOpen) 0.20f else 0.10f),
                        border = BorderStroke(
                            1.dp,
                            if (moodOpen) accent else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CurioIcon(
                                name = mood?.glyph ?: CurioIcons.MoodHappy,
                                contentDescription = "Mood",
                                tint = ink,
                                size = 16.dp
                            )
                            Text(
                                text = mood?.label ?: "Mood",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ink
                            )
                            if (mood == null) {
                                CurioIcon(
                                    name = CurioIcons.Add,
                                    contentDescription = null,
                                    tint = ink,
                                    size = 13.dp
                                )
                            }
                        }
                    }
                }
            }
            // The shared mood row, expanded from the chip — picking a mood
            // (or tapping "None") collapses it straight away.
            AnimatedVisibility(
                visible = moodOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MoodChipsRow(
                    mood = mood,
                    accent = accent,
                    onMoodChange = { picked ->
                        onPickMood(picked)
                        moodOpen = false
                    },
                    header = null
                )
            }
        }
    }
}

/** The live recording badge — a pulsing dot with a label, shown wherever a
 *  take is capturing audio. The take's own `busy` flag drives it, so it stays
 *  truthful without the studio knowing anything about the recorder. */
@Composable
private fun StudioRecordingPill() {
    val pulse = rememberInfiniteTransition(label = "studioRecording")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "studioRecordingDot"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = StudioRecordRed,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = dotAlpha }
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = "Recording",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

/**
 * The take canvas — the paper note and nothing else, plus the take's tags
 * beneath it. Switching takes plays a fast flip (a scale/alpha dip driven by
 * one [Animatable], so the previous editor is never composed alongside the
 * new one — heavy boards stay cheap).
 */
@Composable
private fun StudioCanvas(
    cat: CurioCategory,
    sections: SnapshotStateList<CaptureSectionState>,
    activeIndex: Int,
    boardSeed: Int?,
    waitingForEntry: Boolean,
    tags: List<String>,
    tagInput: String,
    hasNote: Boolean,
    noteExpanded: Boolean,
    note: String,
    onToggleNote: () -> Unit,
    onNoteChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onImageTap: (String) -> Unit,
    modifier: Modifier
) {
    val flip = remember { Animatable(1f) }
    LaunchedEffect(activeIndex) {
        flip.snapTo(0.955f)
        flip.animateTo(1f, CurioMotion.Springs.Calm)
    }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 22.dp)
                    .graphicsLayer {
                        scaleX = flip.value
                        scaleY = flip.value
                        alpha = ((flip.value - 0.955f) / 0.045f).coerceIn(0f, 1f)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (waitingForEntry) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = cat.themedAccent())
                    }
                } else {
                    FormatBodyForCategory(
                        category = cat,
                        sections = sections,
                        activeIndex = activeIndex,
                        boardSeed = boardSeed,
                        onImageTap = onImageTap
                    )
                }
            }
            // This take's labels ride under the note so they read as part of
            // the take (not as page chrome).
            TagEditorRow(
                tags = tags,
                tagInput = tagInput,
                onTagInputChange = onTagInputChange,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                accent = cat.themedAccent(),
                tint = cat.tint,
                ink = cat.categoryInk(),
                onAccentContent = cat.onAccent(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
        // The shared session note floats over the canvas (bottom-start of the
        // canvas box) so it stays reachable while the take scrolls.
        if (hasNote) {
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                SessionNoteFloatingPill(
                    cat = cat,
                    note = note,
                    expanded = noteExpanded,
                    onToggle = onToggleNote,
                    onNoteChange = onNoteChange
                )
            }
        }
    }
}

/**
 * The bottom tray: the take rail, then the tools + save dock. Lives on the
 * bottom edge where the thumb already is, which is what frees the top of the
 * page for the hero and the canvas.
 */
@Composable
private fun StudioTray(
    cat: CurioCategory,
    sections: SnapshotStateList<CaptureSectionState>,
    activeIndex: Int,
    activeFormat: CaptureFormat,
    recording: Boolean,
    tintWash: Boolean,
    editMode: Boolean,
    canSave: Boolean,
    saveInProgress: Boolean,
    saveError: String?,
    onSelectTake: (Int) -> Unit,
    onAddTake: () -> Unit,
    onRequestRemoveTake: (Int) -> Unit,
    onOpenTools: () -> Unit,
    onSave: () -> Unit
) {
    val accent = cat.themedAccent()
    val ink = if (tintWash) cat.categoryInk() else MaterialTheme.colorScheme.onSurface
    val accentContent = if (tintWash) cat.categoryInk() else cat.onAccent()
    val trayColor = if (tintWash) {
        cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        color = trayColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudioTakeRail(
                cat = cat,
                sections = sections,
                activeIndex = activeIndex,
                recording = recording,
                tintWash = tintWash,
                onSelect = onSelectTake,
                onRequestRemove = onRequestRemoveTake,
                onAddTake = onAddTake
            )
            saveError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary: the current format, and the door to the tools
                // sheet (format / mood / tags live there).
                Surface(
                    onClick = onOpenTools,
                    shape = RoundedCornerShape(20.dp),
                    color = lerp(trayColor, accent, 0.16f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        CurioIcon(
                            name = formatGlyph(activeFormat),
                            contentDescription = null,
                            tint = ink,
                            size = 17.dp
                        )
                        Text(
                            text = activeFormat.shortName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = ink,
                            maxLines = 1
                        )
                    }
                }
                StudioSaveButton(
                    editMode = editMode,
                    canSave = canSave,
                    saveInProgress = saveInProgress,
                    containerColor = if (tintWash) cat.tint else accent,
                    contentColor = accentContent,
                    onSave = onSave,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * The save dock. It pops once when the take becomes savable (a small spring on
 * the way in), then settles; while recording or while a save is in flight it
 * reads as busy instead. Mirrors the classic button's enabled rule exactly.
 */
@Composable
private fun StudioSaveButton(
    editMode: Boolean,
    canSave: Boolean,
    saveInProgress: Boolean,
    containerColor: Color,
    contentColor: Color,
    onSave: () -> Unit,
    modifier: Modifier
) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(canSave) {
        if (canSave) {
            pop.snapTo(0.93f)
            pop.animateTo(1f, CurioMotion.Springs.Bouncy)
        } else {
            pop.snapTo(1f)
        }
    }
    val settle by animateFloatAsState(
        targetValue = if (saveInProgress) 0.97f else 1f,
        animationSpec = CurioMotion.Springs.Snappy,
        label = "studioSaveSettle"
    )
    val enabled = canSave && !saveInProgress

    Button(
        onClick = onSave,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier
            .graphicsLayer {
                val s = pop.value * settle
                scaleX = s
                scaleY = s
            }
    ) {
        if (saveInProgress) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Saving…",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        } else {
            // Text-only CTA — the same language as the Manage / Apply pills
            // (the leading tick was retired app-wide).
            Text(
                text = if (editMode) "Save changes" else "Save entry",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

/**
 * The take rail — one pill per take plus "New take". The active pill wears the
 * accent and springs a hair larger; a recording take's pill carries a pulsing
 * dot so the rail says WHERE the audio is being captured when the canvas is
 * scrolled away from the mic.
 */
@Composable
private fun StudioTakeRail(
    cat: CurioCategory,
    sections: SnapshotStateList<CaptureSectionState>,
    activeIndex: Int,
    recording: Boolean,
    tintWash: Boolean,
    onSelect: (Int) -> Unit,
    onRequestRemove: (Int) -> Unit,
    onAddTake: () -> Unit
) {
    val accent = cat.themedAccent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sections.forEachIndexed { i, section ->
            val active = i == activeIndex
            // ONE press source per pill — a shared source would squish the
            // whole rail whenever any single pill was touched.
            val pressed = rememberCurioPressSource(pressedScale = 0.94f)
            val selection by animateFloatAsState(
                targetValue = if (active) 1f else 0.96f,
                animationSpec = CurioMotion.Springs.Press,
                label = "studioTakeSelection"
            )
            val fill by animateColorAsState(
                targetValue = if (active) accent
                else if (tintWash) cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHighest)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = tween(CurioMotion.Durations.Quick),
                label = "studioTakeFill"
            )
            val contentColor = if (active) cat.onAccent()
            else MaterialTheme.colorScheme.onSurface
            Surface(
                onClick = { onSelect(i) },
                shape = RoundedCornerShape(50),
                color = fill,
                interactionSource = pressed.interactionSource,
                modifier = Modifier
                    .then(pressed.modifier)
                    .graphicsLayer {
                        scaleX = selection
                        scaleY = selection
                    }
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = if (sections.size > 1) 4.dp else 12.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (active && recording) {
                        StudioRailPulse(color = contentColor)
                    } else {
                        CurioIcon(
                            name = formatGlyph(section.format),
                            contentDescription = null,
                            tint = if (active) contentColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 15.dp
                        )
                    }
                    Text(
                        text = "${i + 1} · ${section.format.shortName}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = contentColor,
                        maxLines = 1
                    )
                    if (sections.size > 1) {
                        Surface(
                            onClick = { onRequestRemove(i) },
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            CurioIcon(
                                name = CurioIcons.Close,
                                contentDescription = "Remove take ${i + 1}",
                                tint = if (active) contentColor
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
        Surface(
            onClick = onAddTake,
            shape = RoundedCornerShape(50),
            color = lerp(
                if (tintWash) cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHighest)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                accent,
                0.14f
            ),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = null,
                    tint = accent,
                    size = 16.dp
                )
                Text(
                    text = "New take",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** A pulsing status dot for the rail's active-take pill while it records. */
@Composable
private fun StudioRailPulse(color: Color) {
    val pulse = rememberInfiniteTransition(label = "studioRailPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "studioRailPulseScale"
    )
    Box(
        modifier = Modifier
            .size(14.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(color)
    )
}

/** One line per format, describing what the note actually captures. */
private fun formatBlurb(format: CaptureFormat): String = when (format) {
    CaptureFormat.SoundBite -> "A voice take you can trim, title and note"
    CaptureFormat.ReelNotes -> "A rating, your review and a photo collage"
    CaptureFormat.Marginalia -> "Written thoughts, quote cards and a voice note"
    CaptureFormat.GalleryWall -> "A wall of images with captions and quotes"
    CaptureFormat.FieldNotes -> "Observed, surprised, and what's next"
    CaptureFormat.OpenNotebook -> "Pick the note yourself"
}

/**
 * The tools sheet — ONE place for everything that shapes the take: the format,
 * the mood and the labels. The classic page spends three chrome bands on these;
 * here they live behind a single pill, so the canvas stays paper.
 *
 * Sheet rules per the app contract: drag handle only (no close cross), swipe
 * or back to dismiss, and every choice closes the sheet before it acts — a
 * format switch that needs confirmation must not animate in behind a leaving
 * sheet.
 */
@Composable
private fun CaptureToolsSheet(
    cat: CurioCategory,
    sections: SnapshotStateList<CaptureSectionState>,
    activeIndex: Int,
    tags: List<String>,
    tagInput: String,
    onPickFormat: (CaptureFormat) -> Unit,
    onPickMood: (JournalMood?) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val accent = cat.themedAccent()
    val active = sections.getOrNull(activeIndex)
    val activeFormat = active?.format ?: CaptureFormat.SoundBite

    // Close, then act — the sheet is fully gone before a confirmation dialog
    // (or a format swap) can appear on top of it.
    val closeThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioDialogContainerColor(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Take tools",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Take ${activeIndex + 1} of ${sections.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StudioSheetHeading("Format")
            // A two-up grid — chunked rows keep every card exactly half width
            // (a FlowRow would let an odd tail card stretch).
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CAPTURE_FORMATS.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { fmt ->
                            StudioFormatCard(
                                format = fmt,
                                selected = fmt == activeFormat,
                                cat = cat,
                                onClick = { closeThen { onPickFormat(fmt) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            StudioSheetHeading("Mood")
            MoodChipsRow(
                mood = active?.mood,
                accent = accent,
                onMoodChange = { picked -> closeThen { onPickMood(picked) } },
                header = null
            )

            StudioSheetHeading("Tags")
            TagEditorRow(
                tags = tags,
                tagInput = tagInput,
                onTagInputChange = onTagInputChange,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                accent = accent,
                tint = cat.tint,
                ink = cat.categoryInk(),
                onAccentContent = cat.onAccent()
            )
            // Tags are entered in the sheet; a chip tap removes — no confirm
            // (the field is right there to re-add).
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun StudioSheetHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
    )
}

/**
 * One pickable format in the tools sheet: an accent-tinted glyph plate, the
 * name and what the note captures. Selection animates its fill, border and a
 * small scale bump so picking a format feels like a choice, not a radio.
 */
@Composable
private fun StudioFormatCard(
    format: CaptureFormat,
    selected: Boolean,
    cat: CurioCategory,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val accent = cat.themedAccent()
    val pressed = rememberCurioPressSource(pressedScale = 0.96f)
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = CurioMotion.Springs.Press,
        label = "studioFormatScale"
    )
    val fill by animateColorAsState(
        targetValue = if (selected) {
            lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(CurioMotion.Durations.Quick),
        label = "studioFormatFill"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = fill,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) accent else MaterialTheme.colorScheme.outlineVariant
        ),
        interactionSource = pressed.interactionSource,
        modifier = modifier
            .then(pressed.modifier)
            .graphicsLayer {
                scaleX = selectedScale
                scaleY = selectedScale
            }
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = lerp(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        accent,
                        if (selected) 0.30f else 0.14f
                    )
                ) {
                    CurioIcon(
                        name = formatGlyph(format),
                        contentDescription = null,
                        tint = if (selected) cat.categoryInk() else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (selected) {
                    CurioIcon(
                        name = CurioIcons.Check,
                        contentDescription = null,
                        tint = accent,
                        size = 18.dp
                    )
                }
            }
            Text(
                text = format.shortName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = formatBlurb(format),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

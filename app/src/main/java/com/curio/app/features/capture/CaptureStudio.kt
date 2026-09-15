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

private val StudioRecordRed = Color(0xFFE5484D)

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
            modifier = Modifier.weight(1f).padding(start = 6.dp)
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
                        Text(cat.displayName, style = MaterialTheme.typography.labelSmall, color = quietInk.copy(alpha = 0.85f))
                        if (sessionMillis > 0L) {
                            Text("·", style = MaterialTheme.typography.labelSmall, color = quietInk.copy(alpha = 0.5f))
                            CurioIcon(CurioIcons.Timer, null, quietInk.copy(alpha = 0.7f), 12.dp)
                            Text(formatSessionShort(sessionMillis), style = MaterialTheme.typography.labelSmall, color = quietInk.copy(alpha = 0.8f))
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
                        border = BorderStroke(1.dp, if (moodOpen) accent else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CurioIcon(mood?.glyph ?: CurioIcons.MoodHappy, "Mood", ink, 16.dp)
                            Text(mood?.label ?: "Mood", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = ink)
                            if (mood == null) CurioIcon(CurioIcons.Add, null, ink, 13.dp)
                        }
                    }
                }
            }
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
    Surface(shape = RoundedCornerShape(50), color = StudioRecordRed, shadowElevation = 3.dp) {
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
            Text("Recording", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

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
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cat.themedAccent())
                    }
                } else {
                    FormatBodyForCategory(category = cat, sections = sections, activeIndex = activeIndex, boardSeed = boardSeed, onImageTap = onImageTap)
                }
            }
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
    onAddTake: () -> Unit,
    onRequestRemoveTake: (Int) -> Unit,
    onOpenTools: () -> Unit,
    onSave: () -> Unit
) {
    val accent = cat.themedAccent()
    val ink = if (tintWash) cat.categoryInk() else MaterialTheme.colorScheme.onSurface
    val accentContent = if (tintWash) cat.categoryInk() else cat.onAccent()
    val trayColor = if (tintWash) cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow) else MaterialTheme.colorScheme.surfaceContainerLow

    Surface(
        color = trayColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StudioTakeRail(
                cat = cat,
                sections = sections,
                activeIndex = activeIndex,
                tintWash = tintWash,
                onAddTake = onAddTake,
                onRequestRemoveTake = onRequestRemoveTake
            )
            saveError?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        CurioIcon(formatGlyph(activeFormat), null, ink, 17.dp)
                        Text(activeFormat.shortName, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = ink, maxLines = 1)
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

/** The original app take-row contract, moved into the studio tray. */
@Composable
private fun StudioTakeRail(
    cat: CurioCategory,
    sections: SnapshotStateList<CaptureSectionState>,
    activeIndex: Int,
    tintWash: Boolean,
    onAddTake: () -> Unit,
    onRequestRemoveTake: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sections.forEachIndexed { index, section ->
            Surface(
                onClick = { /* The active take is still controlled by SaveCaptureScreen. */ },
                shape = RoundedCornerShape(50),
                color = if (index == activeIndex) cat.themedAccent() else cat.categorySurface(MaterialTheme.colorScheme.surfaceVariant),
                shadowElevation = 2.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = if (sections.size > 1) 4.dp else 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurioIcon(
                        name = formatGlyph(section.format),
                        contentDescription = null,
                        tint = if (index == activeIndex) cat.onAccent() else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 14.dp
                    )
                    Text(
                        text = "${index + 1} · ${section.format.shortName}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (index == activeIndex) cat.onAccent() else MaterialTheme.colorScheme.onSurface
                    )
                    if (sections.size > 1) {
                        Surface(
                            onClick = { onRequestRemoveTake(index) },
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            CurioIcon(
                                name = CurioIcons.Close,
                                contentDescription = "Remove take",
                                tint = if (index == activeIndex) cat.onAccent() else MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = if (tintWash) cat.tint else cat.themedAccent(),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = "Add take",
                    tint = if (tintWash) cat.categoryInk() else cat.onAccent(),
                    size = 16.dp
                )
                Text(
                    text = "Add take",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (tintWash) cat.categoryInk() else cat.onAccent()
                )
            }
        }
    }
}

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
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Take tools", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text("Take ${activeIndex + 1} of ${sections.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StudioSheetHeading("Format")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CAPTURE_FORMATS.chunked(2).forEach { pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        }
    }
}

@Composable
private fun StudioSheetHeading(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
}

private fun formatBlurb(format: CaptureFormat): String = when (format) {
    CaptureFormat.SoundBite -> "A voice take you can trim, title and note"
    CaptureFormat.ReelNotes -> "A rating, your review and a photo collage"
    CaptureFormat.Marginalia -> "Written thoughts, quote cards and a voice note"
    CaptureFormat.GalleryWall -> "A wall of images with captions and quotes"
    CaptureFormat.FieldNotes -> "Observed, surprised, and what's next"
    CaptureFormat.OpenNotebook -> "Pick the note yourself"
}

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
        targetValue = if (selected) lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(CurioMotion.Durations.Quick),
        label = "studioFormatFill"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = fill,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant),
        interactionSource = pressed.interactionSource,
        modifier = modifier.then(pressed.modifier).graphicsLayer { scaleX = selectedScale; scaleY = selectedScale }
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, if (selected) 0.30f else 0.14f)
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
                if (selected) CurioIcon(CurioIcons.Check, null, accent, 18.dp)
            }
            Text(format.shortName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(formatBlurb(format), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

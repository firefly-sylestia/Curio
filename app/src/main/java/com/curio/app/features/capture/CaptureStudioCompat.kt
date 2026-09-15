package com.curio.app.features.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CurioCategory
import com.curio.app.data.JournalMood

/**
 * Compatibility overload for callers that still provide the selected format
 * when creating a take. The Studio itself now owns the simple Add take action,
 * while this adapter preserves the previous SaveCaptureScreen contract.
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
    onAddTake: (CaptureFormat) -> Unit,
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
    val defaultFormat = if (cat.defaultFormat == CaptureFormat.OpenNotebook) {
        CaptureFormat.SoundBite
    } else {
        cat.defaultFormat
    }

    CaptureStudio(
        modifier = modifier,
        cat = cat,
        topicName = topicName,
        editMode = editMode,
        sessionMillis = sessionMillis,
        tintWash = tintWash,
        sections = sections,
        activeIndex = activeIndex,
        activeMood = activeMood,
        recording = recording,
        boardSeed = boardSeed,
        waitingForEntry = waitingForEntry,
        canSave = canSave,
        saveInProgress = saveInProgress,
        saveError = saveError,
        hasNote = hasNote,
        noteExpanded = noteExpanded,
        note = note,
        tags = tags,
        tagInput = tagInput,
        onBack = onBack,
        onSave = onSave,
        // Keep this lambda's static type on the Studio signature. Without
        // the cast, Kotlin sees both CaptureStudio overloads as candidates
        // at this call site and reports OVERLOAD_RESOLUTION_AMBIGUITY.
        onAddTake = ({ onAddTake(defaultFormat) } as () -> Unit),
        onRequestRemoveTake = onRequestRemoveTake,
        onPickFormat = onPickFormat,
        onPickMood = onPickMood,
        onToggleNote = onToggleNote,
        onNoteChange = onNoteChange,
        onTagInputChange = onTagInputChange,
        onAddTag = onAddTag,
        onRemoveTag = onRemoveTag,
        onImageTap = onImageTap
    )
}

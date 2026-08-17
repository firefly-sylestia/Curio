package com.curio.app.features.capture.formats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureData
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.data.TextSpan
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.curio.app.ui.components.RichTextEditor
import com.curio.app.ui.components.RichTextToolbarMode
import com.curio.app.ui.theme.paperInk

/**
 * Field Notes format body — CURIO_SPEC §8.5 (Science & Nature).
 *
 * Three pre-labeled sections give gentle structure (mirrors a real
 * field journal) without forcing all three to be filled:
 *   - "What I observed"
 *   - "What surprised me"
 *   - "What I want to learn next"
 *
 * Each section is independently collapsible/expandable via
 * [CollapsibleSectionHeader] (CURIO_SPEC §12.2 disclosure pattern). When
 * collapsed with content, a grey preview snippet trails the header so
 * nothing feels hidden-and-forgotten.
 *
 * Optional single photo attach at the bottom (placeholder thumbnail for
 * now; Phase 4 wires the picker).
 *
 * [onCanSaveChange] fires true when at least ONE of the three sections
 * has content.
 */
@Composable
fun FieldNotesFormat(
    accent: Color,
    tint: Color,
    onCanSaveChange: (Boolean) -> Unit,
    onDataChanged: (CaptureData?) -> Unit = {},
    initialData: CaptureData.FieldNotes? = null
) {
    val context = LocalContext.current
    // v158 — the dictation mic shows only while the Voice-to-text settings
    // toggle is on (same experiment gate as the sound bite note box).
    val voiceToTextEnabled = AppPreferences.voiceToTextEnabledState
    // Edit mode: restore the three sections + photos so re-saving preserves
    // the original capture instead of silently wiping it.
    var observed by remember(initialData) { mutableStateOf(initialData?.observed ?: "") }
    var surprised by remember(initialData) { mutableStateOf(initialData?.surprised ?: "") }
    var learnNext by remember(initialData) { mutableStateOf(initialData?.learnNext ?: "") }
    // Rich-text formatting per section — legacy entries lack them (Gson →
    // null), guard with orEmpty().
    var observedSpans by remember(initialData) { mutableStateOf(initialData?.observedSpans.orEmpty()) }
    var surprisedSpans by remember(initialData) { mutableStateOf(initialData?.surprisedSpans.orEmpty()) }
    var learnNextSpans by remember(initialData) { mutableStateOf(initialData?.learnNextSpans.orEmpty()) }
    // Note-paper style per section — each of the three field-journal boxes
    // wears its own choice. Legacy entries lack the per-field fields (Gson
    // → null), fall back to the take-level paperStyle → RULED.
    var observedStyle by remember(initialData) {
        mutableStateOf(initialData?.observedStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED)
    }
    var surprisedStyle by remember(initialData) {
        mutableStateOf(initialData?.surprisedStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED)
    }
    var learnNextStyle by remember(initialData) {
        mutableStateOf(initialData?.learnNextStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED)
    }
    // Note-paper color per section — legacy entries lack the per-field
    // fields (Gson → null), fall back to CREAM.
    var observedColor by remember(initialData) {
        mutableStateOf(initialData?.observedColor ?: NotePaperColor.CREAM)
    }
    var surprisedColor by remember(initialData) {
        mutableStateOf(initialData?.surprisedColor ?: NotePaperColor.CREAM)
    }
    var learnNextColor by remember(initialData) {
        mutableStateOf(initialData?.learnNextColor ?: NotePaperColor.CREAM)
    }
    // Mood — the shared "How did it make you feel?" row. Optional; legacy
    // entries have none (Gson → null).
    var mood by remember(initialData) { mutableStateOf(initialData?.mood) }
    var imageUris by remember(initialData) { mutableStateOf(initialData?.imageUris.orEmpty()) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        imageUris = (imageUris + uris.map { it.toString() }).take(MaxAttachedImages)
    }

    var observedExpanded by remember { mutableStateOf(true) }
    var surprisedExpanded by remember { mutableStateOf(true) }
    var learnNextExpanded by remember { mutableStateOf(true) }

    val canSave = observed.isNotBlank() ||
                  surprised.isNotBlank() ||
                  learnNext.isNotBlank() ||
                  imageUris.isNotEmpty()
    LaunchedEffect(
        canSave, observed, observedSpans, surprised, surprisedSpans,
        learnNext, learnNextSpans, imageUris, observedStyle, surprisedStyle, learnNextStyle,
        observedColor, surprisedColor, learnNextColor, mood
    ) {
        onCanSaveChange(canSave)
        onDataChanged(
            if (canSave) CaptureData.FieldNotes(
                observed = observed,
                surprised = surprised,
                learnNext = learnNext,
                imageUris = imageUris,
                observedSpans = observedSpans,
                surprisedSpans = surprisedSpans,
                learnNextSpans = learnNextSpans,
                observedStyle = observedStyle,
                surprisedStyle = surprisedStyle,
                learnNextStyle = learnNextStyle,
                observedColor = observedColor,
                surprisedColor = surprisedColor,
                learnNextColor = learnNextColor,
                // Legacy fallback — mirror the first section's style.
                paperStyle = observedStyle,
                mood = mood
            )
            else null
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Section 1: What I observed ──────────────────────────────────────
        CollapsibleSectionHeader(
            label = "What I observed",
            accent = accent,
            expanded = observedExpanded,
            onToggle = { observedExpanded = !observedExpanded },
            preview = previewSnippet(observed, !observedExpanded)
        )
        AnimatedVisibility(
            visible = observedExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            // Each section wears the note-paper slip like the journal pages;
            // the toolbar renders OUTSIDE the paper (paper mode) so the
            // ruled lines line up under the text while typing.
            RichTextEditor(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = observed,
                spans = observedSpans,
                onRichTextChange = { newText, newSpans ->
                    observed = newText
                    observedSpans = newSpans
                },
                placeholder = "What did you see, hear, notice?",
                toolbarMode = RichTextToolbarMode.TOGGLE,
                minHeight = 100.dp,
                ink = paperInk(),
                accent = MaterialTheme.colorScheme.tertiary,
                paper = true,
                paperStyle = observedStyle,
                onPaperStyleChange = { observedStyle = it },
                paperColor = observedColor,
                onPaperColorChange = { observedColor = it },
                paperContentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                // v158 — dictation rides the section's own tool dock.
                trailingAction = {
                    DictationMic(
                        enabled = true,
                        visible = voiceToTextEnabled,
                        accent = MaterialTheme.colorScheme.tertiary,
                        onInsert = { text ->
                            observed = if (observed.isBlank()) text else "$observed\n$text"
                        }
                    )
                }
            )
        }

        // ── Section 2: What surprised me ───────────────────────────────────
        CollapsibleSectionHeader(
            label = "What surprised me",
            accent = accent,
            expanded = surprisedExpanded,
            onToggle = { surprisedExpanded = !surprisedExpanded },
            preview = previewSnippet(surprised, !surprisedExpanded)
        )
        AnimatedVisibility(
            visible = surprisedExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            RichTextEditor(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = surprised,
                spans = surprisedSpans,
                onRichTextChange = { newText, newSpans ->
                    surprised = newText
                    surprisedSpans = newSpans
                },
                placeholder = "What was unexpected or delightful?",
                toolbarMode = RichTextToolbarMode.TOGGLE,
                minHeight = 100.dp,
                ink = paperInk(),
                accent = MaterialTheme.colorScheme.tertiary,
                paper = true,
                paperStyle = surprisedStyle,
                onPaperStyleChange = { surprisedStyle = it },
                paperColor = surprisedColor,
                onPaperColorChange = { surprisedColor = it },
                paperContentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                // v158 — dictation rides the section's own tool dock.
                trailingAction = {
                    DictationMic(
                        enabled = true,
                        visible = voiceToTextEnabled,
                        accent = MaterialTheme.colorScheme.tertiary,
                        onInsert = { text ->
                            surprised = if (surprised.isBlank()) text else "$surprised\n$text"
                        }
                    )
                }
            )
        }

        // ── Section 3: What I want to learn next ───────────────────────────
        CollapsibleSectionHeader(
            label = "What I want to learn next",
            accent = accent,
            expanded = learnNextExpanded,
            onToggle = { learnNextExpanded = !learnNextExpanded },
            preview = previewSnippet(learnNext, !learnNextExpanded)
        )
        AnimatedVisibility(
            visible = learnNextExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            RichTextEditor(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = learnNext,
                spans = learnNextSpans,
                onRichTextChange = { newText, newSpans ->
                    learnNext = newText
                    learnNextSpans = newSpans
                },
                placeholder = "Where does this lead?",
                toolbarMode = RichTextToolbarMode.TOGGLE,
                minHeight = 100.dp,
                ink = paperInk(),
                accent = MaterialTheme.colorScheme.tertiary,
                paper = true,
                paperStyle = learnNextStyle,
                onPaperStyleChange = { learnNextStyle = it },
                paperColor = learnNextColor,
                onPaperColorChange = { learnNextColor = it },
                paperContentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                // v158 — dictation rides the section's own tool dock.
                trailingAction = {
                    DictationMic(
                        enabled = true,
                        visible = voiceToTextEnabled,
                        accent = MaterialTheme.colorScheme.tertiary,
                        onInsert = { text ->
                            learnNext = if (learnNext.isBlank()) text else "$learnNext\n$text"
                        }
                    )
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Spacer(Modifier.height(8.dp))

        // ── Optional photo attach (CURIO_SPEC §8.5) ────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Attach photos (optional, up to $MaxAttachedImages)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Many attachments wrap into a tidy grid of thumbs instead of
            // overflowing one long row.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                imageUris.forEachIndexed { index, uri ->
                    ImageThumb(
                        index = index + 1,
                        accent = accent,
                        tint = tint,
                        imageUri = uri,
                        onClick = { },
                        onRemove = { imageUris = imageUris.filterIndexed { i, _ -> i != index } }
                    )
                }
                if (imageUris.size < MaxAttachedImages) {
                    AddImageButton(
                        accent = accent,
                        tint = tint,
                        label = "Add",
                        onClick = { imagePicker.launch(arrayOf("image/*")) }
                    )
                }
            }
        }
    }
}

/**
 * Returns a short preview snippet ("first 40 chars + ellipsis") for a
 * collapsible section header when the section is collapsed and has
 * content. Null otherwise — header renders without the preview row.
 */
private fun previewSnippet(text: String, collapsed: Boolean): String? =
    if (collapsed && text.isNotBlank()) text.take(40) + "\u2026" else null
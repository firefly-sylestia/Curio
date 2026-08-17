package com.curio.app.features.capture.formats

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureData
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.features.capture.AudioRecorder
import com.curio.app.ui.components.RichTextEditor
import com.curio.app.ui.components.RichTextToolbarMode
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.paperInk
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.features.capture.formats.tintedTileInk
import kotlinx.coroutines.delay

/**
 * Marginalia format body — CURIO_SPEC §8.3 (Books / Authors).
 *
 * The quotes entry wears a NOTE-PAPER look instead of the category tint:
 * journal + quote cards sit on warm cream paper (warm off-black toned paper
 * in dark mode) with faint ruled lines, and each field carries a compact
 * rich-text toolbar (B / I / highlighter) over the current selection.
 *
 * Layout:
 *   - "My thoughts" — paper journal page with an always-visible toolbar
 *   - "Favorite quotes" — compact paper quote cards, each with its own
 *     toolbar, slightly rotated (±1.5°) for a hand-placed feel. Tap
 *     "+ Add quote" below to create a new blank card; "Remove" deletes it.
 *
 * [onCanSaveChange] fires true when journal OR at least one quote card
 * has content.
 */
@Composable
fun MarginaliaFormat(
    accent: Color,
    tint: Color,
    onCanSaveChange: (Boolean) -> Unit,
    onDataChanged: (CaptureData?) -> Unit = {},
    initialData: CaptureData.Marginalia? = null
) {
    // v158 — the dictation mic shows only while the Voice-to-text settings
    // toggle is on (same experiment gate as the sound bite note box).
    val voiceToTextEnabled = AppPreferences.voiceToTextEnabledState
    // Edit mode: restore journal text + spans + quote cards so re-saving
    // preserves the original capture instead of silently wiping it.
    // Legacy entries lack the span fields (Gson → null), guard with orEmpty().
    var journalText by remember(initialData) { mutableStateOf(initialData?.journalText ?: "") }
    var journalSpans by remember(initialData) { mutableStateOf(initialData?.journalSpans.orEmpty()) }
    // Note-paper style per text box — the journal page and EACH quote card
    // wear their own choice. Legacy entries lack the per-field fields (Gson
    // → null), fall back to the take-level paperStyle → RULED.
    var journalStyle by remember(initialData) {
        mutableStateOf(initialData?.journalStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED)
    }
    // Note-paper color per text box — the journal page and EACH quote card
    // wear their own color. Legacy entries lack the per-field fields (Gson
    // → null/empty), fall back to CREAM.
    var journalColor by remember(initialData) {
        mutableStateOf(initialData?.journalColor ?: NotePaperColor.CREAM)
    }
    // Mood — the "How did it make you feel?" row. Optional; legacy entries
    // have none (Gson → null).
    var mood by remember(initialData) { mutableStateOf(initialData?.mood) }
    // Attached gallery images (up to [MaxAttachedImages], same as Field
    // Notes) — legacy entries omit them (Gson → null, guard with orEmpty()).
    var imageUris by remember(initialData) { mutableStateOf(initialData?.imageUris.orEmpty()) }
    val context = LocalContext.current
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

    // ── Voice-note attachment — the shared AudioRecorder (MediaRecorder)
    //    pipeline trimmed to record → stop → keep/remove.
    val recorder = remember(context) { AudioRecorder(context) }
    var audioState by remember(initialData) {
        mutableStateOf(
            if (initialData?.audioFilePath != null) AudioRecorder.State.STOPPED
            else AudioRecorder.State.IDLE
        )
    }
    var audioSeconds by remember(initialData) {
        mutableIntStateOf(initialData?.audioDurationSeconds ?: 0)
    }
    var audioFilePath by remember(initialData) { mutableStateOf(initialData?.audioFilePath) }
    var audioFileSize by remember(initialData) {
        mutableStateOf(initialData?.audioFileSizeBytes ?: 0L)
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                recorder.start()
                audioState = recorder.state
                audioSeconds = 0
                permissionDenied = false
            } catch (_: Exception) {
                audioState = AudioRecorder.State.IDLE
            }
        } else {
            permissionDenied = true
        }
    }

    // ── Tick the recording timer every second while RECORDING ────────────
    LaunchedEffect(audioState) {
        if (audioState == AudioRecorder.State.RECORDING) {
            while (audioState == AudioRecorder.State.RECORDING) {
                audioSeconds = recorder.elapsedSeconds
                delay(1000)
            }
        }
    }
    // Quote cards — the SHARED hand-placed paper notecard section (same
    // component Reel Notes / Sound Bite / Mood Board use). Owns the parallel
    // lists (text / spans / tilt / style / color) and never re-rolls a
    // card's tilt after it's created.
    val quoteCards = rememberQuoteCardsState(
        initialQuotes = initialData?.quotes.orEmpty(),
        initialSpans = initialData?.quoteSpans.orEmpty(),
        initialTilts = initialData?.quoteTilts.orEmpty(),
        initialStyles = initialData?.quoteStyles.orEmpty(),
        initialColors = initialData?.quoteColors.orEmpty(),
        defaultStyle = initialData?.paperStyle ?: NotePaperStyle.RULED,
        defaultColor = NotePaperColor.CREAM
    )

    val canSave = journalText.isNotBlank() || quoteCards.hasContent ||
        audioFilePath != null || imageUris.isNotEmpty()
    // Key on the content too: journal text typed or quotes added AFTER the
    // first character must re-emit, or saving would persist stale data
    // (later text/quotes/attachments silently dropped from the saved entry).
    LaunchedEffect(
        canSave, journalText, journalSpans, quoteCards.quotes.toList(),
        quoteCards.spans.toList(), quoteCards.tilts.toList(), journalStyle,
        quoteCards.styles.toList(), journalColor, quoteCards.colors.toList(),
        mood, imageUris, audioFilePath, audioState, audioSeconds, audioFileSize
    ) {
        onCanSaveChange(canSave)
        onDataChanged(
            if (canSave) CaptureData.Marginalia(
                journalText = journalText,
                quotes = quoteCards.quotes.toList(),
                journalSpans = journalSpans,
                quoteSpans = quoteCards.spans.toList(),
                quoteTilts = quoteCards.tilts.toList(),
                journalStyle = journalStyle,
                quoteStyles = quoteCards.styles.toList(),
                journalColor = journalColor,
                quoteColors = quoteCards.colors.toList(),
                // Legacy fallback — mirror the journal's style.
                paperStyle = journalStyle,
                mood = mood,
                imageUris = imageUris,
                audioFilePath = audioFilePath,
                audioDurationSeconds = audioSeconds,
                audioFileSizeBytes = audioFileSize,
                audioEncodingFormat = "AAC"
            )
            else null
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Journal page ──────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "My thoughts",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // The toolbar renders OUTSIDE the paper slip (paper mode) so the
            // ruled lines line up under the text while typing.
            RichTextEditor(
                modifier = Modifier.fillMaxWidth(),
                text = journalText,
                spans = journalSpans,
                onRichTextChange = { newText, newSpans ->
                    journalText = newText
                    journalSpans = newSpans
                },
                placeholder = "What did this book make you think about?",
                toolbarMode = RichTextToolbarMode.MAIN,
                minHeight = 140.dp,
                ink = paperInk(),
                accent = MaterialTheme.colorScheme.tertiary,
                paper = true,
                paperStyle = journalStyle,
                onPaperStyleChange = { journalStyle = it },
                paperColor = journalColor,
                onPaperColorChange = { journalColor = it },
                paperContentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                // v158 — dictation rides the journal's own tool dock.
                trailingAction = {
                    DictationMic(
                        enabled = true,
                        visible = voiceToTextEnabled,
                        accent = MaterialTheme.colorScheme.tertiary,
                        onInsert = { text ->
                            journalText = if (journalText.isBlank()) text else "$journalText\n$text"
                        }
                    )
                }
            )
        }

        // ── Quote cards — the SHARED hand-placed paper notecard section ──
        // New cards inherit the journal's current paper style + color.
        QuoteCardsSection(
            state = quoteCards,
            newCardStyle = { journalStyle },
            newCardColor = { journalColor }
        )

        // ── Attachments — always on (v30: the "Entry date & mood" option
        // was removed — date, mood and attachments are permanent).
        run {
            // Attach gallery images (up to [MaxAttachedImages]) — same row
            // as Reel Notes.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Attach images (optional, up to $MaxAttachedImages)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Many attachments wrap into a tidy grid of thumbs instead
                // of overflowing one long row.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    imageUris.forEachIndexed { i, uri ->
                        ImageThumb(
                            index = i + 1,
                            accent = accent,
                            tint = tint,
                            imageUri = uri,
                            onClick = { /* Phase 4: lightbox */ },
                            onRemove = {
                                imageUris = imageUris.filterIndexed { idx, _ -> idx != i }
                            }
                        )
                    }
                    if (imageUris.size < MaxAttachedImages) {
                        AddImageButton(
                            accent = accent,
                            tint = tint,
                            onClick = { imagePicker.launch(arrayOf("image/*")) }
                        )
                    }
                }
            }

            // Voice-note attachment — record → stop → keep/remove.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Voice note (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                JournalVoiceNoteRow(
                    state = audioState,
                    seconds = audioSeconds,
                    fileSizeBytes = audioFileSize,
                    permissionDenied = permissionDenied,
                    accent = accent,
                    tint = tint,
                    onRecord = {
                        if (hasPermission) {
                            try {
                                recorder.start()
                                audioState = recorder.state
                                audioSeconds = 0
                            } catch (_: Exception) {
                                audioState = AudioRecorder.State.IDLE
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStop = {
                        try {
                            val path = recorder.stop()
                            audioFilePath = path
                            audioFileSize = path.let { java.io.File(it).length() }
                        } catch (_: Exception) {
                            audioFilePath = null
                        }
                        audioState = recorder.state
                        audioSeconds = recorder.elapsedSeconds
                    },
                    onDiscard = {
                        recorder.discard()
                        audioState = recorder.state
                        audioSeconds = 0
                    },
                    onRemove = {
                        audioFilePath = null
                        audioSeconds = 0
                        audioFileSize = 0L
                        audioState = AudioRecorder.State.IDLE
                    }
                )
            }
        }
    }
}

/**
 * Compact voice-note attachment row for the journal — record / stop /
 * discard while capturing, then a kept capsule with a remove button.
 * Uses the shared [AudioRecorder] (MediaRecorder) pipeline.
 */
@Composable
private fun JournalVoiceNoteRow(
    state: AudioRecorder.State,
    seconds: Int,
    fileSizeBytes: Long,
    permissionDenied: Boolean,
    accent: Color,
    tint: Color,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    onRemove: () -> Unit
) {
    when (state) {
        AudioRecorder.State.IDLE -> Surface(
            onClick = onRecord,
            shape = RoundedCornerShape(12.dp),
            // v29 — opaque fill (no transparency): the translucent tint
            // looked broken after the border removal.
            color = categoryTintFill(accent),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurioIcon(
                    CurioIcons.Mic, null,
                    tint = if (permissionDenied) MaterialTheme.colorScheme.error else tintedTileInk(accent),
                    size = 18.dp
                )
                Text(
                    text = if (permissionDenied) "Microphone permission needed"
                           else "Record a voice note",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (permissionDenied) MaterialTheme.colorScheme.error else tintedTileInk(accent)
                )
            }
        }

        AudioRecorder.State.RECORDING,
        AudioRecorder.State.PAUSED -> Surface(
            shape = RoundedCornerShape(12.dp),
            // v29 — opaque fill (no transparency).
            color = categoryTintFill(accent),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurioIcon(CurioIcons.Mic, null, tint = tintedTileInk(accent), size = 18.dp)
                Text(
                    text = "Recording · ${seconds}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onStop,
                    shape = RoundedCornerShape(50),
                    color = accent
                ) {
                    CurioIcon(
                        CurioIcons.Stop, "Stop recording",
                        tint = pastelFillInk(accent), size = 18.dp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Surface(
                    onClick = onDiscard,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    CurioIcon(
                        CurioIcons.Close, "Discard recording",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }

        AudioRecorder.State.STOPPED -> Surface(
            shape = RoundedCornerShape(12.dp),
            // v29 — opaque fill (no transparency).
            color = categoryTintFill(accent),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurioIcon(CurioIcons.PlayArrow, null, tint = tintedTileInk(accent), size = 18.dp)
                Text(
                    text = buildString {
                        append("Voice note · ${seconds}s")
                        if (fileSizeBytes > 0) append(" · ${formatBytes(fileSizeBytes)}")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onRemove,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    CurioIcon(
                        CurioIcons.Delete, "Remove voice note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

/** Compact file-size label for the journal's voice-note capsule. */
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "${bytes / 1_000_000} MB"
    bytes >= 1_000 -> "${bytes / 1_000} KB"
    else -> "$bytes B"
}

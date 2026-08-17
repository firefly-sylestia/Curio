package com.curio.app.features.capture.formats

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioStorageManager
import com.curio.app.data.CaptureData
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.data.TextSpan
import com.curio.app.features.capture.AudioRecorder
import com.curio.app.ui.components.AudioTrimmer
import com.curio.app.ui.components.RichTextEditor
import com.curio.app.ui.components.RichTextToolbarMode
import com.curio.app.ui.theme.paperInk
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.components.LiveWaveform
import com.curio.app.ui.components.TrimWaveform
import com.curio.app.ui.components.WaveformExtractor
import com.curio.app.ui.components.formatRecordingTime
import com.curio.app.ui.components.rememberPulseScale
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// v125 — dictation lives behind ONE floating mic on the large note box: no
// more per-field mic buttons. The mic appears only while the note is focused;
// tapping it opens a dictation dialog with a live preview; the session only
// ends when the user taps Stop (the OS recognizer gets generous silence
// windows so a mid-thought pause doesn't cut it off), and Insert commits the
// transcript into the note — which stays fully editable afterwards.

/**
 * Sound Bite format body — CURIO_SPEC §8.1 (Music / Artists).
 *
 * 4-state machine: IDLE / RECORDING / PAUSED / STOPPED.
 * - IDLE: big mic button to start, requests RECORD_AUDIO permission if needed
 * - RECORDING / PAUSED: pulsing ring + live waveform + mm:ss timer +
 *   Pause/Stop/Discard controls — driven by real [AudioRecorder] (MediaRecorder)
 * - STOPPED: shows trim waveform with draggable handles, plus title field
 *   and Record-over button. User can trim the audio before saving.
 *
 * Uses real [AudioRecorder] (MediaRecorder) for actual voice capture,
 * and [AudioTrimmer] to trim the recorded file after recording stops.
 * The audio file path is stored in [CaptureData.SoundBite.audioFilePath].
 *
 * Runtime permission (RECORD_AUDIO) is requested on first tap of the mic button.
 */
@Composable
fun SoundBiteFormat(
    accent: Color,
    tint: Color,
    onCanSaveChange: (Boolean) -> Unit,
    onDataChanged: (CaptureData?) -> Unit = {},
    onBusyChange: (Boolean) -> Unit = {},
    initialData: CaptureData.SoundBite? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceToTextEnabled = AppPreferences.voiceToTextEnabledState
    val recorder = remember(context) { AudioRecorder(context) }
    // Edit mode: restore a saved recording as STOPPED with its file + title so
    // re-saving preserves the original capture (no silent wipe). Keyed on
    // initialData so a newly-loaded edit preloads once.
    var recordingState by remember(initialData) {
        mutableStateOf(if (initialData != null) AudioRecorder.State.STOPPED else AudioRecorder.State.IDLE)
    }
    var recordingSeconds by remember(initialData) { mutableIntStateOf(initialData?.durationSeconds ?: 0) }
    var title by remember(initialData) { mutableStateOf(initialData?.title ?: "") }
    var note by remember(initialData) { mutableStateOf(initialData?.note ?: "") }
    // Rich-text formatting for the note — legacy entries lack it (Gson →
    // null), guard with orEmpty().
    var noteSpans by remember(initialData) { mutableStateOf(initialData?.noteSpans.orEmpty()) }
    // Note-paper style per text box — the title slip and the note wear
    // their OWN choice. Legacy entries lack the per-field fields (Gson →
    // null), fall back to the take-level paperStyle → RULED.
    var titleStyle by remember(initialData) {
        mutableStateOf(initialData?.titleStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED)
    }
    var noteStyle by remember(initialData) {
        mutableStateOf(initialData?.noteStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED)
    }
    // Note-paper color per text box — legacy entries lack the per-field
    // fields (Gson → null), fall back to CREAM.
    var titleColor by remember(initialData) {
        mutableStateOf(initialData?.titleColor ?: NotePaperColor.CREAM)
    }
    var noteColor by remember(initialData) {
        mutableStateOf(initialData?.noteColor ?: NotePaperColor.CREAM)
    }
    // Mood — the shared "How did it make you feel?" row. Optional; legacy
    // entries have none (Gson → null).
    var mood by remember(initialData) { mutableStateOf(initialData?.mood) }
    // Quote cards — the SHARED hand-placed paper notecard section (same
    // component Marginalia / Reel Notes / Mood Board use). Owns the parallel
    // lists (text / spans / tilt / style / color); new cards inherit the
    // note box's current paper style + color.
    val quoteCards = rememberQuoteCardsState(
        initialQuotes = initialData?.quotes.orEmpty(),
        initialSpans = initialData?.quoteSpans.orEmpty(),
        initialTilts = initialData?.quoteTilts.orEmpty(),
        initialStyles = initialData?.quoteStyles.orEmpty(),
        initialColors = initialData?.quoteColors.orEmpty(),
        defaultStyle = initialData?.noteStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED,
        defaultColor = initialData?.noteColor ?: NotePaperColor.CREAM
    )
    var savedFilePath by remember(initialData) { mutableStateOf(initialData?.audioFilePath) }
    var permissionDenied by remember { mutableStateOf(false) }
    // Edit-mode restore: a restored recording must NOT auto-open the trimmer —
    // only freshly-recorded audio should. Cleared the moment the user (re)records.
    var restoredRecording by remember { mutableStateOf(initialData != null) }

    // ── Trim state ───────────────────────────────────────────────────────
    var showTrimmer by remember { mutableStateOf(false) }
    var startTrim by remember { mutableFloatStateOf(0f) }
    var endTrim by remember { mutableFloatStateOf(1f) }
    var trimInProgress by remember { mutableStateOf(false) }

    // ── Voice-to-text (v158) ───────────────────────────────────────────
    // Dictation rides the SHARED [DictationMic] (see DictationMic.kt), which
    // owns the recognizer + permission flow per field. We only track whether
    // a session is LIVE so the format-switch confirmation knows a dictation
    // is in progress (same RECORD_AUDIO permission as recording).
    var dictating by remember { mutableStateOf(false) }

    // Runtime permission launcher — powers RECORDING. Dictation has its own
    // permission flow inside [DictationMic].
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            try {
                recorder.start()
                restoredRecording = false
                recordingState = recorder.state
                recordingSeconds = 0
            } catch (_: Exception) {
                recordingState = AudioRecorder.State.IDLE
            }
        } else {
            permissionDenied = true
        }
    }

    // ── Tick the recording timer every second while RECORDING ────────────
    LaunchedEffect(recordingState) {
        if (recordingState == AudioRecorder.State.RECORDING) {
            while (recordingState == AudioRecorder.State.RECORDING) {
                delay(1000)
                recordingSeconds = recorder.elapsedSeconds
            }
        }
    }

    // ── Real-time mic level (0..1) while recording — drives the live
    //    visualizer with ACTUAL input (AudioRecorder.maxAmplitude), not a
    //    fake sine, so the bars dance with the voice. Decays to 0 when idle.
    var micLevel by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(recordingState) {
        if (recordingState == AudioRecorder.State.RECORDING ||
            recordingState == AudioRecorder.State.PAUSED
        ) {
            while (recordingState == AudioRecorder.State.RECORDING) {
                micLevel = (recorder.maxAmplitude / 32767f).coerceIn(0f, 1f)
                delay(70)
            }
            micLevel = 0f
        }
    }

    // ── Extract waveform when entering STOPPED with a valid file ─────────
    val waveformSamples by produceState<FloatArray>(
        initialValue = FloatArray(120),
        key1 = savedFilePath
    ) {
        if (savedFilePath != null) {
            value = withContext(Dispatchers.Default) {
                WaveformExtractor.extract(savedFilePath!!, barCount = 120)
            } ?: FloatArray(120) { kotlin.random.Random.nextFloat() * 0.6f + 0.2f }
        }
    }

    // ── Show trimmer when first entering STOPPED (fresh recordings only) ─
    LaunchedEffect(recordingState, savedFilePath) {
        if (recordingState == AudioRecorder.State.STOPPED &&
            savedFilePath != null && !restoredRecording
        ) {
            showTrimmer = true
            startTrim = 0f
            endTrim = 1f
        }
    }

    // ── Clean up recorder on dispose ─────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            recorder.release()
            // Tell the parent the take is no longer busy (live recording or
            // a live dictation lost with this editor) so format-switch
            // confirmation can't be skipped.
            onBusyChange(false)
        }
    }

    // ── Report busy state (recording OR dictation in progress) so the
    // universal picker can confirm before switching format on a live take ──
    LaunchedEffect(recordingState, dictating) {
        onBusyChange(
            recordingState == AudioRecorder.State.RECORDING ||
                recordingState == AudioRecorder.State.PAUSED ||
                dictating
        )
    }

    // ── Report can-save + capture data ───────────────────────────────────
    // A completed recording is the primary content, but a take that only
    // holds typed content (title / note / quotes, no recording) is still a
    // saveable draft — the old recording-only rule silently dropped those
    // on back/switch.
    // A very short but valid recording can report 0 elapsed seconds because
    // the UI timer is second-granular. Use the actual file as the save gate so
    // those voice notes are not silently omitted from the saved entry.
    val hasRecording = recordingState == AudioRecorder.State.STOPPED &&
                       savedFilePath?.let { path ->
                           File(path).isFile && File(path).length() > 0L
                       } == true &&
                       !trimInProgress
    val hasTypedContent = title.isNotBlank() || note.isNotBlank() || quoteCards.hasContent
    val canSave = hasRecording || hasTypedContent
    LaunchedEffect(
        canSave, savedFilePath, title, note, noteSpans, titleStyle, noteStyle,
        titleColor, noteColor, mood, quoteCards.quotes.toList(), quoteCards.spans.toList(),
        quoteCards.tilts.toList(), quoteCards.styles.toList(), quoteCards.colors.toList()
    ) {
        onCanSaveChange(canSave)
        onDataChanged(
            if (canSave) CaptureData.SoundBite(
                durationSeconds = recordingSeconds,
                title = title,
                note = note,
                noteSpans = noteSpans,
                audioFilePath = savedFilePath,
                titleStyle = titleStyle,
                noteStyle = noteStyle,
                titleColor = titleColor,
                noteColor = noteColor,
                quotes = quoteCards.quotes.toList(),
                quoteSpans = quoteCards.spans.toList(),
                quoteTilts = quoteCards.tilts.toList(),
                quoteStyles = quoteCards.styles.toList(),
                quoteColors = quoteCards.colors.toList(),
                // Legacy fallback — mirror the primary field's style.
                paperStyle = noteStyle,
                mood = mood
            )
            else null
        )
    }

    // ── Check initial permission state ───────────────────────────────────
    // v7.25 — computed LIVE per dictation request (not remember-once): the
    // remember-cached value stayed false forever after the user granted
    // permission via the launcher, so every later mic tap re-launched the
    // permission dialog instead of dictating.
    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Format body (state-dependent) ────────────────────────────────
        when (recordingState) {
            AudioRecorder.State.IDLE -> {
                IdleControls(
                    accent = accent,
                    hasPermission = hasMicrophonePermission(),
                    permissionDenied = permissionDenied,
                    onRecord = {
                        if (hasMicrophonePermission()) {
                            try {
                                recorder.start()
                                restoredRecording = false
                                recordingState = recorder.state
                                recordingSeconds = 0
                            } catch (_: Exception) {
                                recordingState = AudioRecorder.State.IDLE
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

            }
            AudioRecorder.State.RECORDING,
            AudioRecorder.State.PAUSED -> LiveControls(
                accent = accent,
                tint = tint,
                isPaused = recordingState == AudioRecorder.State.PAUSED,
                seconds = recordingSeconds,
                level = micLevel,
                onPauseResume = {
                    if (recordingState == AudioRecorder.State.RECORDING) {
                        recorder.pause()
                    } else {
                        recorder.resume()
                    }
                    recordingState = recorder.state
                },
                onStop = {
                    try {
                        savedFilePath = recorder.stop()
                    } catch (_: Exception) {
                        savedFilePath = null
                    }
                    recordingState = recorder.state
                    recordingSeconds = recorder.elapsedSeconds
                },
                onDiscard = {
                    recorder.discard()
                    recordingState = recorder.state
                    recordingSeconds = 0
                    savedFilePath = null
                }
            )
            AudioRecorder.State.STOPPED -> {
                // ── Trim mode (before trimming or after) ─────────────────
                if (showTrimmer && savedFilePath != null) {
                    TrimSection(
                        accent = accent,
                        tint = tint,
                        seconds = recordingSeconds,
                        waveformSamples = waveformSamples,
                        startTrim = startTrim,
                        endTrim = endTrim,
                        trimInProgress = trimInProgress,
                        onStartTrimChange = { startTrim = it },
                        onEndTrimChange = { endTrim = it },
                        onApplyTrim = {
                            trimInProgress = true
                            scope.launch {
                                try {
                                    val trimmedPath = withContext(Dispatchers.Default) {
                                        AudioTrimmer.trim(
                                            outputDir = context.cacheDir,
                                            inputPath = savedFilePath!!,
                                            startMs = (startTrim * recordingSeconds * 1000).toLong(),
                                            endMs = (endTrim * recordingSeconds * 1000).toLong()
                                        )
                                    }
                                    if (trimmedPath != null) {
                                        // Update the file path and duration
                                        savedFilePath = trimmedPath
                                        recordingSeconds = ((endTrim - startTrim) * recordingSeconds).toInt()
                                    }
                                } catch (_: Exception) { /* keep original file */ }
                                trimInProgress = false
                                showTrimmer = false
                            }
                        },
                        onKeepFull = {
                            showTrimmer = false
                        }
                    )
                } else {
                    StoppedControls(
                        accent = accent,
                        tint = tint,
                        seconds = recordingSeconds,
                        onReRecord = {
                            recorder.release()
                            restoredRecording = false
                            recordingState = recorder.state
                            recordingSeconds = 0
                            savedFilePath = null
                            showTrimmer = false
                            startTrim = 0f
                            endTrim = 1f
                        },
                        // v27h — delete the recorded attachment for good:
                        // remove the audio file and reset to the idle state
                        // so the take can't save a ghost recording.
                        onDelete = {
                            AudioStorageManager.deleteAudio(context, savedFilePath)
                            recorder.release()
                            restoredRecording = false
                            recordingState = recorder.state
                            recordingSeconds = 0
                            savedFilePath = null
                            showTrimmer = false
                            startTrim = 0f
                            endTrim = 1f
                        }
                    )
                }
            }
        }

        // ── Permission denied hint ───────────────────────────────────────
        if (permissionDenied) {
            Text(
                text = "Microphone access is needed to record. Grant permission in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Optional title field — show always, disabled during trim ─────
        // v125 — the quick title is a PLAIN input now (no paper/tear slip,
        // no per-field style/color toggles); its paper styling was removed
        // per the user and the detail page shows it as a clean pill below
        // the quick fact instead.
        PaperLineField(
            value = title,
            onValueChange = { title = it },
            label = "Add a quick title (optional)",
            enabled = recordingState != AudioRecorder.State.RECORDING,
            imeAction = ImeAction.Next,
            paper = false
        )

        // Rich-text note — formatting behind a small toggle. The toolbar
        // renders OUTSIDE the paper slip (paper mode) so the ruled lines
        // line up under the text while typing; the field itself sits
        // directly on the paper (no inner box / double margin).
        RichTextEditor(
            modifier = Modifier.fillMaxWidth(),
            text = note,
            spans = noteSpans,
            onRichTextChange = { newText, newSpans ->
                note = newText
                noteSpans = newSpans
            },
            placeholder = "What did this recording capture?",
            toolbarMode = RichTextToolbarMode.TOGGLE,
            minHeight = 96.dp,
            enabled = recordingState != AudioRecorder.State.RECORDING,
            ink = paperInk(),
            accent = MaterialTheme.colorScheme.tertiary,
            paper = true,
            paperStyle = noteStyle,
            onPaperStyleChange = { noteStyle = it },
            paperColor = noteColor,
            onPaperColorChange = { noteColor = it },
            paperContentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            // v158 — the dictation mic rides the note box's own tool dock
            // (above the field), so it never hides below the page behind the
            // keyboard. The shared [DictationMic] owns the recognizer,
            // permission flow and the live-preview dialog; Insert appends
            // the transcript to the note (which stays fully editable).
            trailingAction = {
                DictationMic(
                    enabled = recordingState != AudioRecorder.State.RECORDING,
                    visible = voiceToTextEnabled,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onInsert = { text ->
                        note = if (note.isBlank()) text else "$note\n$text"
                    },
                    onListeningChange = { dictating = it }
                )
            }
        )

        // ── Quote cards — the SHARED hand-placed paper notecard section ──
        // Frozen while actively recording (the cards need the keyboard).
        QuoteCardsSection(
            state = quoteCards,
            enabled = recordingState != AudioRecorder.State.RECORDING,
            newCardStyle = { noteStyle },
            newCardColor = { noteColor }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Trim section — shown when recording is stopped and trim UI is active
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TrimSection(
    accent: Color,
    tint: Color,
    seconds: Int,
    waveformSamples: FloatArray,
    startTrim: Float,
    endTrim: Float,
    trimInProgress: Boolean,
    onStartTrimChange: (Float) -> Unit,
    onEndTrimChange: (Float) -> Unit,
    onApplyTrim: () -> Unit,
    onKeepFull: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Saved indicator
        Text(
            text = "✓ Recording saved (${formatRecordingTime(seconds)})",
            style = MaterialTheme.typography.titleSmall,
            color = accent
        )

        // Waveform with trim handles
        TrimWaveform(
            samples = waveformSamples,
            startTrim = startTrim,
            endTrim = endTrim,
            accent = accent,
            tint = tint,
            totalSeconds = seconds,
            onTrimChange = { start, end ->
                onStartTrimChange(start)
                onEndTrimChange(end)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Keep Full — dismiss trim UI
            Surface(
                onClick = onKeepFull,
                // v114 — 24dp rounded to match the app's button language
                // (the old 16dp corners read stock M3 next to the pill
                // chips).
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Keep full",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Apply Trim — enabled when range is narrower than full
            val hasTrim = startTrim > 0.01f || endTrim < 0.99f
            Button(
                onClick = onApplyTrim,
                enabled = hasTrim && !trimInProgress,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = pastelFillInk(accent),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (trimInProgress) {
                    CircularProgressIndicator(
                        color = pastelFillInk(accent),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = if (trimInProgress) "Trimming…" else "Apply trim",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Private sub-composables
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun IdleControls(
    accent: Color,
    hasPermission: Boolean,
    permissionDenied: Boolean,
    onRecord: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Surface(
            onClick = onRecord,
            shape = CircleShape,
            color = accent,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CurioIcon(
                    name = CurioIcons.Mic,
                    contentDescription = "Start recording",
                    tint = pastelFillInk(accent),
                    size = 48.dp
                )
            }
        }
        Text(
            text = if (!hasPermission && !permissionDenied) "Tap to grant mic access & record"
                   else "Tap to record your take",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LiveControls(
    accent: Color,
    tint: Color,
    isPaused: Boolean,
    seconds: Int,
    level: Float,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit
) {
    val pulseScale = rememberPulseScale(active = !isPaused)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Pulsing outer ring + solid mic center
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = tint,
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
            ) {}
            Surface(
                shape = CircleShape,
                color = accent,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CurioIcon(
                        name = if (isPaused) CurioIcons.MicNone else CurioIcons.Mic,
                        contentDescription = null,
                        tint = pastelFillInk(accent),
                        size = 48.dp
                    )
                }
            }
        }

        // Live waveform — real mic amplitude (level 0..1) drives the bars.
        LiveWaveform(
            color = accent,
            active = !isPaused,
            level = level,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )

        // Running mm:ss timer
        Text(
            text = formatRecordingTime(seconds),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Pause / Stop / Discard controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                icon = if (isPaused) CurioIcons.PlayArrow else CurioIcons.Pause,
                label = if (isPaused) "Resume" else "Pause",
                tint = accent,
                onClick = onPauseResume
            )
            ControlButton(
                icon = CurioIcons.Stop,
                label = "Stop",
                tint = accent,
                onClick = onStop
            )
            ControlButton(
                icon = CurioIcons.Replay,
                label = "Discard",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onDiscard
            )
        }
    }
}

@Composable
private fun StoppedControls(
    accent: Color,
    tint: Color,
    seconds: Int,
    onReRecord: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = tint,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CurioIcon(
                    name = CurioIcons.Mic,
                    contentDescription = null,
                    tint = accent,
                    size = 48.dp
                )
            }
        }
        Text(
            text = "Recording saved (${formatRecordingTime(seconds)})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onReRecord,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = "Record over", color = accent)
            }
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = "Delete recording", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: String,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = tint.copy(alpha = 0.15f)
        ) {
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = icon,
                    contentDescription = label,
                    tint = tint,
                    size = 24.dp
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package com.curio.app.features.capture.formats

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale
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

    // ── Voice-to-text (v125) ───────────────────────────────────────────
    // One floating mic on the LARGE note box (not the title): it appears only
    // while the note is focused and opens a dictation dialog with a live
    // preview. The same RECORD_AUDIO permission powers recording and
    // dictation; `dictationRequested` disambiguates a grant: the launcher
    // opens the dialog when the user asked to dictate, the recorder when
    // they asked for a voice note.
    var noteFocused by remember { mutableStateOf(false) }
    var dictationOpen by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    // v131 — the transcript ACCUMULATES across pauses: `dictatedText` holds
    // every committed utterance (each break becomes a full stop), while
    // `partialTranscript` is only the LIVE words of the current utterance.
    // Before this, every new partial replaced the whole preview, so a pause
    // wiped everything you had already said.
    var dictatedText by remember { mutableStateOf("") }
    var partialTranscript by remember { mutableStateOf("") }
    // A pause (onEndOfSpeech) seen since the last commit — a fresh partial
    // after it belongs to a NEW utterance, so the old partial must be
    // committed first (for engines that never fire onResults per utterance).
    var speechEnded by remember { mutableStateOf(false) }
    var transcribeError by remember { mutableStateOf<String?>(null) }
    var dictationRequested by remember { mutableStateOf(false) }
    val speechRecognizer = remember(context, voiceToTextEnabled) {
        if (voiceToTextEnabled && SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun startDictation() {
        if (!voiceToTextEnabled) return
        val recognizer = speechRecognizer ?: run {
            transcribeError = "Speech recognition isn't available on this device."
            return
        }
        listening = true
        transcribeError = null
        dictatedText = ""
        partialTranscript = ""
        speechEnded = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // v125 — generous silence windows so a natural pause mid-thought
            // doesn't end the dictation early; the session only ends when
            // the user taps Stop.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500)
        }
        // v7.25 — a recognizer reused across sessions (second dictation) can
        // throw IllegalStateException / silently never call back on many
        // devices unless the previous session is cancelled first.
        runCatching { recognizer.cancel() }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // A pause mid-session — the current partial is a finished
                // utterance. If the engine never fires onResults for it (it
                // keeps the session open and streams a fresh partial for the
                // next utterance), the next partial commits this one first
                // (see onPartialResults), so the break still becomes a full
                // stop and the words are never lost.
                speechEnded = true
            }
            override fun onError(error: Int) {
                listening = false
                transcribeError = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech heard. Try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone access is needed to transcribe."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Speech service unreachable. Check your connection."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition isn't available on this device."
                    else -> "Couldn't transcribe. Try again."
                }
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                // v7.25 — some engines finish with an EMPTY RESULTS list but
                // deliver the text as the final partial — keep it so the
                // dialog can still offer it for Insert.
                val final = matches?.firstOrNull { it.isNotBlank() }
                    ?: partialTranscript.takeIf { it.isNotBlank() }.orEmpty()
                // v131 — commit, never replace: the finished utterance is
                // APPENDED, so a pause + re-speak no longer wipes the
                // earlier text (and the break becomes a full stop).
                commitUtterance(final)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull().orEmpty()
                // v131 — a blank partial during a pause must NOT clear the
                // live preview; keep the last words on screen.
                if (partial.isBlank()) return
                // A partial right after a pause: if the previous utterance was
                // never finalized (no onResults), decide whether this is a
                // NEW utterance (commit the old one first — the break becomes
                // a full stop and the words are kept) or a same-utterance
                // REFINEMENT (the engine polished the wording — just update
                // the live partial and stay armed).
                if (speechEnded && partialTranscript.isNotBlank()) {
                    if (partial == partialTranscript) return
                    if (partial.startsWith(partialTranscript) || partialTranscript.startsWith(partial)) {
                        partialTranscript = partial
                        return
                    }
                    commitUtterance(partialTranscript)
                }
                speechEnded = false
                partialTranscript = partial
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    fun stopListening() {
        runCatching { speechRecognizer?.cancel() }
        listening = false
    }

    /**
     * v131 — commits a finished utterance into [dictatedText]. Each break
     * becomes a full stop: utterances join with a period (always on, per
     * user), and the next sentence starts capitalized.
     */
    fun commitUtterance(text: String) {
        val t = text.trim()
        if (t.isBlank()) {
            speechEnded = false
            return
        }
        val sentence = if (dictatedText.isNotBlank()) t.replaceFirstChar { it.uppercase() } else t
        if (dictatedText.isBlank()) {
            dictatedText = sentence
        } else {
            val prevEndsPunct = dictatedText.lastOrNull()?.let { it == '.' || it == '?' || it == '!' } == true
            dictatedText = if (!prevEndsPunct) "$dictatedText. $sentence" else "$dictatedText $sentence"
        }
        partialTranscript = ""
        speechEnded = false
    }

    /** The dialog preview — committed utterances plus the live partial. */
    fun dictationPreview(): String = buildString {
        append(dictatedText)
        if (partialTranscript.isNotBlank()) {
            if (dictatedText.isNotBlank()) append(' ')
            append(partialTranscript)
        }
    }.trim()

    /** Inserts the transcript at the end of the note; the box stays editable. */
    fun insertDictation() {
        val text = dictationPreview()
        if (text.isNotBlank()) {
            note = if (note.isBlank()) text else "$note\n$text"
        }
        stopListening()
        dictationOpen = false
        dictatedText = ""
        partialTranscript = ""
        speechEnded = false
        transcribeError = null
    }

    // Runtime permission launcher — shared by recording AND dictation.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            if (dictationRequested) {
                // Consume a pending dictation grant even if the user turned
                // the experiment off while Android's permission dialog was
                // open. Never fall through to ordinary recording in that
                // case; recording was not what this permission request meant.
                dictationRequested = false
                if (voiceToTextEnabled) {
                    dictationOpen = true
                    startDictation()
                }
            } else {
                try {
                    recorder.start()
                    restoredRecording = false
                    recordingState = recorder.state
                    recordingSeconds = 0
                } catch (_: Exception) {
                    recordingState = AudioRecorder.State.IDLE
                }
            }
        } else {
            permissionDenied = true
            // Denied for dictation — drop the pending flag so a later grant
            // from the RECORD path can't accidentally open the dialog.
            dictationRequested = false
        }
    }

    // Turning the experiment off while a dictation or permission flow is
    // active must release the recognizer and clear the pending request.
    LaunchedEffect(voiceToTextEnabled) {
        if (!voiceToTextEnabled) {
            speechRecognizer?.cancel()
            dictationRequested = false
            dictationOpen = false
            listening = false
            dictatedText = ""
            partialTranscript = ""
            speechEnded = false
            transcribeError = null
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

    // ── Clean up recorder + recognizer on dispose ────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            recorder.release()
            // Tell the parent the take is no longer busy (live recording or
            // a live dictation lost with this editor) so format-switch
            // confirmation can't be skipped.
            onBusyChange(false)
        }
    }
    DisposableEffect(speechRecognizer) {
        onDispose { speechRecognizer?.destroy() }
    }

    // ── Report busy state (recording OR dictation in progress) so the
    // universal picker can confirm before switching format on a live take ──
    LaunchedEffect(recordingState, listening) {
        onBusyChange(
            recordingState == AudioRecorder.State.RECORDING ||
                recordingState == AudioRecorder.State.PAUSED ||
                listening
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

    // v126 — moved AFTER the launcher + permission check it uses: local
    // functions/vals can't be forward-referenced in Kotlin (this compiled in
    // the editor but failed CI compileDebugKotlin).
    fun onFloatMicTap() {
        if (!voiceToTextEnabled || dictationOpen) return
        if (hasMicrophonePermission()) {
            dictationOpen = true
            startDictation()
        } else {
            dictationRequested = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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
            // v131 — the dictation mic rides the note box's own tool dock
            // (above the field), so it never hides below the page behind the
            // keyboard. It appears only while the note is focused (not the
            // title) and is hidden while the dialog is up.
            trailingAction = {
                if (voiceToTextEnabled && noteFocused && !dictationOpen) {
                    Surface(
                        onClick = { onFloatMicTap() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CurioIcon(
                                name = CurioIcons.Mic,
                                contentDescription = "Dictate note",
                                tint = MaterialTheme.colorScheme.onTertiary,
                                size = 18.dp
                            )
                        }
                    }
                }
            },
            onFocusChanged = { noteFocused = it }
        )

        // ── Dictation dialog (v125) — floating-mic flow: live preview at
        // the bottom, Stop to end listening, Insert to drop the transcript
        // into the note (the box stays editable).
        if (dictationOpen) {
            DictationDialog(
                accent = MaterialTheme.colorScheme.tertiary,
                listening = listening,
                partial = dictationPreview(),
                error = transcribeError,
                onStop = {
                    stopListening()
                    transcribeError = null
                },
                onInsert = { insertDictation() },
                onDismiss = {
                    stopListening()
                    dictationOpen = false
                    dictatedText = ""
                    partialTranscript = ""
                    speechEnded = false
                    transcribeError = null
                }
            )
        }

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

/**
 * v125 — the floating-mic dictation dialog. Opens when the user taps the
 * floating mic on the large note box; the live partial transcript streams
 * into the preview pinned at the BOTTOM of the dialog. The session only
 * ends when the user taps Stop (the recognizer gets generous silence
 * windows so a mid-thought pause doesn't cut it off); Insert commits the
 * transcript into the note, which stays fully editable afterwards.
 */
@Composable
private fun DictationDialog(
    accent: Color,
    listening: Boolean,
    partial: String,
    error: String?,
    onStop: () -> Unit,
    onInsert: () -> Unit,
    onDismiss: () -> Unit
) {
    val pulseScale = rememberPulseScale(active = listening)
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        onDismissRequest = onDismiss,
        title = { Text("Dictate note", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // ── Status row — pulsing mic + what's happening ─────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(48.dp)
                                .scale(pulseScale)
                        ) {}
                        Surface(
                            shape = CircleShape,
                            color = accent,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CurioIcon(
                                    name = if (listening) CurioIcons.Mic else CurioIcons.MicNone,
                                    contentDescription = null,
                                    tint = pastelFillInk(accent),
                                    size = 20.dp
                                )
                            }
                        }
                    }
                    Text(
                        text = when {
                            error != null -> error
                            listening -> "Listening… speak now"
                            partial.isBlank() -> "Nothing heard yet"
                            else -> "Ready — review the preview below"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (error != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
                // ── Live preview pinned to the BOTTOM of the dialog ─────
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp, max = 160.dp)
                ) {
                    Text(
                        text = partial.ifBlank { "Your words will appear here while you speak…" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = if (partial.isBlank()) androidx.compose.ui.text.font.FontStyle.Italic
                                       else androidx.compose.ui.text.font.FontStyle.Normal
                        ),
                        color = if (partial.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (listening) {
                TextButton(onClick = onStop) {
                    Text("Stop", fontWeight = FontWeight.Bold, color = accent)
                }
            } else {
                TextButton(onClick = onInsert, enabled = partial.isNotBlank()) {
                    Text("Insert", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

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

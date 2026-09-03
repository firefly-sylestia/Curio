package com.curio.app.features.capture.formats

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.ui.components.rememberPulseScale
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.pastelFillInk
import java.util.Locale

// v125 — dictation lived behind ONE floating mic on the sound bite's note
// box. v158 — the SAME flow is now reusable: [DictationMic] rides ANY note
// or quote text box in Save your take (the field decides when it shows via
// [visible], e.g. while focused) and owns the full session — recognizer,
// permission flow, live-preview dialog. The mic appears only while the field
// is focused; tapping it opens a dictation dialog with a live preview; the
// session only ends when the user taps Stop (the OS recognizer gets
// generous silence windows so a mid-thought pause doesn't cut it off), and
// Insert commits the transcript via [onInsert] — the box stays editable.

/**
 * v158 — the reusable dictation mic for note / quote text boxes: a small
 * accent mic button rendered beside the field (only when [visible] and
 * [enabled]) that opens the shared dictation dialog with a live preview.
 * Owns its own recognizer session (created lazily on first tap, destroyed
 * on dispose) and its own RECORD_AUDIO permission flow, so every field can
 * offer dictation without sharing state. [onInsert] receives the final
 * transcript (the caller appends it to its own text).
 */
@Composable
fun DictationMic(
    enabled: Boolean,
    visible: Boolean,
    accent: Color,
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** v158 — reports whether a dictation session is LIVE (recognizer
     *  listening) so callers can gate format switching / busy states on it. */
    onListeningChange: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    // v131 — the transcript ACCUMULATES across pauses: `dictatedText` holds
    // every committed utterance (each break becomes a full stop), while
    // `partialTranscript` is only the LIVE words of the current utterance.
    var dictatedText by remember { mutableStateOf("") }
    var partialTranscript by remember { mutableStateOf("") }
    // A pause (onEndOfSpeech) seen since the last commit — a fresh partial
    // after it belongs to a NEW utterance, so the old partial must be
    // committed first (for engines that never fire onResults per utterance).
    var speechEnded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // True while Android's permission dialog is up after a mic tap.
    var pendingPermission by remember { mutableStateOf(false) }
    // Lazy recognizer — only created once the mic is actually tapped, so
    // fields the user never focuses never bind the speech service.
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    DisposableEffect(Unit) {
        onDispose { runCatching { recognizer?.destroy() } }
    }

    /**
     * v131 — commits a finished utterance into [dictatedText]. Each break
     * becomes a full stop: utterances join with a period (always on, per
     * user), and the next sentence starts capitalized. Declared BEFORE
     * startListening because the RecognitionListener inside it calls this
     * (Kotlin local functions can't be forward-referenced — v131 CI fix).
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

    fun startListening() {
        if (!enabled) return
        val rec = recognizer ?: run {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
            } else null
        }
        if (rec == null) {
            error = "Speech recognition isn't available on this device."
            return
        }
        listening = true
        onListeningChange?.invoke(true)
        error = null
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
        runCatching { rec.cancel() }
        rec.setRecognitionListener(object : RecognitionListener {
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
            override fun onError(errorCode: Int) {
                listening = false
                onListeningChange?.invoke(false)
                error = when (errorCode) {
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
                onListeningChange?.invoke(false)
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
        rec.startListening(intent)
    }

    fun stopListening() {
        runCatching { recognizer?.cancel() }
        listening = false
        onListeningChange?.invoke(false)
    }

    /** The dialog preview — committed utterances plus the live partial. */
    fun preview(): String = buildString {
        append(dictatedText)
        if (partialTranscript.isNotBlank()) {
            if (dictatedText.isNotBlank()) append(' ')
            append(partialTranscript)
        }
    }.trim()

    fun dismissDialog() {
        stopListening()
        open = false
        dictatedText = ""
        partialTranscript = ""
        speechEnded = false
        error = null
    }

    // RECORD_AUDIO permission — requested on first mic tap, then reused.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingPermission) {
                // Consume the pending mic tap even if the feature was
                // turned off while Android's permission dialog was open.
                pendingPermission = false
                if (enabled) {
                    open = true
                    startListening()
                }
            }
        } else {
            pendingPermission = false
            error = "Microphone access is needed to transcribe."
        }
    }

    // Turning the feature off mid-dictation releases the recognizer and
    // clears the pending flow.
    LaunchedEffect(enabled) {
        if (!enabled) {
            runCatching { recognizer?.cancel() }
            pendingPermission = false
            open = false
            listening = false
            onListeningChange?.invoke(false)
            dictatedText = ""
            partialTranscript = ""
            speechEnded = false
            error = null
        }
    }

    if (visible && enabled && !open) {
        Surface(
            onClick = {
                pendingPermission = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            shape = CircleShape,
            color = accent,
            shadowElevation = 2.dp,
            modifier = modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CurioIcon(
                    name = CurioIcons.Mic,
                    contentDescription = "Dictate",
                    tint = pastelFillInk(accent),
                    size = 18.dp
                )
            }
        }
    }

    if (open) {
        DictationDialog(
            accent = accent,
            listening = listening,
            partial = preview(),
            error = error,
            onStop = {
                stopListening()
                error = null
            },
            onInsert = {
                val text = preview()
                if (text.isNotBlank()) onInsert(text)
                dismissDialog()
            },
            onDismiss = { dismissDialog() }
        )
    }
}

/**
 * v125 — the shared dictation dialog: live partial transcript streams into
 * the preview pinned at the BOTTOM of the dialog. The session only ends
 * when the user taps Stop (the recognizer gets generous silence windows so
 * a mid-thought pause doesn't cut it off); Insert commits the transcript
 * via [onInsert], which keeps the source box fully editable afterwards.
 * v158 — moved from SoundBiteFormat into the shared [DictationMic] flow.
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
        title = { Text("Dictate", fontWeight = FontWeight.SemiBold) },
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
                            else -> "Ready · review the preview below"
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
                            fontStyle = if (partial.isBlank()) FontStyle.Italic
                                       else FontStyle.Normal
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

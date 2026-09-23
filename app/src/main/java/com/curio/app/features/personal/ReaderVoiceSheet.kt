package com.curio.app.features.personal

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.NeuralVoicePacks

/**
 * ── v465h — THE VOICE, FROM THE PAGE THE MEMBER IS ON ─────────────────────
 *
 * The member: *"changing voice in that screen"* — meaning the reader, not the
 * Reading settings page. And they are right that it belongs here: a voice is the
 * one reading choice a member makes WHILE LISTENING, and until now the only way
 * to make it was to leave the book, open Settings, find Reading, and come back —
 * which is four screens away from the thing you were trying to change.
 *
 * **IT IS THE SETTINGS PAGE'S OWN LISTS, NOT A SECOND SET.** The engines come
 * from [readerEngines], the packs from [downloadedPacks] and the rows are the
 * same [VoiceChoice] the settings pickers use, so the two surfaces cannot drift
 * into offering different voices — and the sentinels (`ReaderEngine.NEURAL`,
 * `ReaderEngine.EDGE`), the pack ids and the endpoint's voice names all keep
 * meaning exactly what they mean there.
 *
 * **ALL THREE CHOICES IN ONE DOOR.** Engine, voice and narrator were three
 * separate rows in Settings because each is a long list of its own; from inside a
 * book, three doors in a row is three dialogs to dismiss. So the three are
 * sections of one sheet, and which of them are drawn depends on what the engine
 * has: the phone's own engine lists its voices and nothing else, a downloaded
 * pack lists one voice (itself) and its narrators, and the Edge experiment lists
 * the endpoint's own names. A section with nothing under it is not drawn at all —
 * an empty heading is a promise of a choice that is not there.
 */
@Composable
internal fun ReaderVoiceSheet(
    palette: ReaderPalette,
    onDismiss: () -> Unit,
    /**
     * Called after any choice, so the reader can start the sentence again in the
     * new voice. A member who changes the voice mid-sentence and hears nothing
     * different for another twenty seconds will conclude it did not work.
     */
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    var engines by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var voices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // The engines are a PackageManager query, so they are asked for once per open
    // (the same rule the Reading settings page follows).
    LaunchedEffect(Unit) {
        engines = readerEngines(context)
        voices = voicesFor(context)
    }

    // THE NARRATORS ARE READ FROM THE CATALOG, never from the loaded model:
    // sherpa-onnx exposes a speaker COUNT and never a name, so naming them from
    // the engine is impossible — and reading them from the catalog also avoids
    // loading a 305 MB pack just to draw a list (the v465c rule).
    val pack = if (ReaderLook.speakEngine == ReaderEngine.NEURAL) {
        NeuralVoicePacks.byId(ReaderLook.speakVoice)
    } else {
        null
    }
    val narrators = pack?.speakers.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.paper,
        title = {
            Text(
                "Which voice reads",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = readerTypeFamily(ReaderLook.typeFace)
                ),
                color = palette.ink
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ReaderVoiceHeading("Engine", palette)
                engines.forEach { (name, label) ->
                    VoiceChoice(label = label, live = ReaderLook.speakEngine == name, palette = palette) {
                        if (ReaderLook.speakEngine != name) {
                            ReaderLook.speakEngine = name
                            // A VOICE NAME BELONGS TO ITS ENGINE: leaving the old
                            // one would point the new engine at a voice it has
                            // never heard of — harmless to the API, and a lie in
                            // the row the member is looking at.
                            ReaderLook.speakVoice = ""
                            if (name !in listOf(ReaderEngine.NEURAL, ReaderEngine.EDGE)) {
                                // THE PLATFORM'S VOICES COME FROM THE ENGINE, and
                                // binding one is asynchronous — so the list is
                                // asked for from the binding callback, never on
                                // the line after the request (the v464 bug: the
                                // row said "no voices are installed" until it was
                                // opened a second time). The two Curio engines are
                                // lists we already hold and need no binding.
                                ReaderSpeaker.prepare(context, name, onReady = {
                                    voices = ReaderSpeaker.voices()
                                })
                            }
                        }
                        onChanged()
                    }
                }

                if (voices.isNotEmpty()) {
                    ReaderVoiceHeading("Voice", palette)
                    voices.forEach { (name, label) ->
                        VoiceChoice(label = label, live = ReaderLook.speakVoice == name, palette = palette) {
                            if (ReaderLook.speakVoice != name) ReaderLook.speakVoice = name
                            onChanged()
                        }
                    }
                }

                // A pack with ONE voice gets no narrator row at all — Piper reads
                // with the voice it has, and a list of one is not a choice.
                if (narrators.size > 1) {
                    ReaderVoiceHeading("Narrator", palette)
                    narrators.forEachIndexed { index, name ->
                        VoiceChoice(
                            label = name,
                            live = ReaderLook.speakSpeaker.coerceIn(0, narrators.size - 1) == index,
                            palette = palette
                        ) {
                            ReaderLook.speakSpeaker = index
                            onChanged()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = palette.accent)
            }
        }
    )
}

/**
 * The voices [ReaderLook.speakEngine] actually has, in the three cases there are.
 *
 * The engine's own list arrives through a callback rather than a return value
 * because binding a speech engine is ASYNCHRONOUS — reading `voices()` on the
 * line after asking for the engine is always too early, which is the v464 bug
 * that made the picker say "no voices are installed" until it was opened a second
 * time. A voice chosen from this list takes effect on the next sentence, so a
 * late answer is harmless here in a way it never was in that row.
 */
private fun voicesFor(context: Context): List<Pair<String, String>> =
    when (ReaderLook.speakEngine) {
        // v465c — a downloaded pack IS this engine's voice, so the same list
        // holds it: one list per engine, whichever kind of engine it is.
        ReaderEngine.NEURAL -> downloadedPacks(context)
        // v465f — and the Edge experiment's names, which are its own voice ids.
        ReaderEngine.EDGE -> EdgeVoice.VOICES
        // v464 — the phone's own engine (the empty package) and every engine the
        // member has installed, asked of the platform.
        else -> ReaderSpeaker.voices()
    }

/** A section's own name, quieter than the rows under it. */
@Composable
private fun ReaderVoiceHeading(label: String, palette: ReaderPalette) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = palette.ink.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
    )
}

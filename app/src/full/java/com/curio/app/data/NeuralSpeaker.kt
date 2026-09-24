package com.curio.app.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.util.concurrent.atomic.AtomicInteger

/**
 * ── v465c — THE NEURAL VOICE, PLAYING WHAT IT MAKES (FULL EDITION ONLY) ─────
 *
 * The other half of the pack feature: [NeuralVoicePacks] fetches a voice,
 * this says it. It sits BESIDE `ReaderSpeaker` rather than replacing it — the
 * reader still reads with the phone's own engine unless the member has
 * downloaded a pack and chosen it, and everything the root AGENTS says about
 * asking before replacing a code path applies to a member's reading voice more
 * than to most things.
 *
 * **One sentence at a time, deliberately.** The reader's driver already feeds one
 * sentence per call (that is what makes the read-along sentence highlight
 * trustworthy), so this synthesises that sentence whole, plays it, and reports
 * back — rather than streaming with `generateWithCallback`. Two reasons, both
 * about failure modes a member would feel: a sentence of narration is a couple
 * of seconds of audio (well under a megabyte of floats), so buffering it costs
 * nothing; and the streaming callback's return value is the library's own
 * stop signal, which is a contract worth not guessing at. Buffering keeps the
 * stop path in this file, where it can be read.
 *
 * **NOTHING HERE RUNS ON THE MAIN THREAD.** Synthesising a sentence is seconds of
 * onnxruntime work on a phone, and playback is a blocking `AudioTrack.write`.
 * Both happen on one worker thread per utterance; [say] returns immediately.
 *
 * **It can always fail quietly.** A pack that is half on disk, a phone that
 * cannot allocate the track, a model the runtime refuses: every one of them ends
 * in `onDone` rather than an exception, because a book that cannot be read aloud
 * is an inconvenience and a reader that dies mid-chapter is a bug.
 */
internal object NeuralSpeaker {

    /** The engine for the pack that is loaded, or null when none is. */
    @Volatile private var tts: OfflineTts? = null

    /** Which pack [tts] belongs to, so [prepare] can be idempotent. */
    @Volatile private var loaded: String? = null

    /** The utterance in flight, and the flag that stops it. */
    @Volatile private var track: AudioTrack? = null

    /** Bumped per utterance: a callback from an older one is not the new one. */
    private val utterance = AtomicInteger(0)

    @Volatile private var cancelled = false

    val isReady: Boolean get() = tts != null

    /**
     * ── v465j — READY FOR *THIS* PACK, WHICH IS NOT THE SAME QUESTION ─────
     *
     * [isReady] answers "is an engine loaded", and the reader asked exactly that
     * before speaking: `isReady || prepare(pack)`. That reads correctly and is
     * wrong, because the engine already loaded may belong to a DIFFERENT pack.
     *
     * **It is why Kokoro "did not work".** Piper is the pack offered first, so a
     * member downloads Piper, listens, then downloads Kokoro and chooses it. The
     * very next sentence finds `isReady == true` — Piper's engine, still in
     * memory — skips `prepare` entirely, and reads on in Piper's voice for ever.
     * Nothing fails, nothing logs, and the 305 MB pack they chose is never
     * loaded. The same trap catches a pack switched back again.
     *
     * So readiness is asked per pack, which is the only form of it that means
     * anything here.
     */
    fun isReadyFor(id: String?): Boolean = tts != null && loaded == id

    /** How many voices the loaded pack offers (Piper 1, Kokoro eleven). */
    fun speakerCount(): Int = runCatching { tts?.numSpeakers() ?: 1 }.getOrDefault(1)

    /**
     * Loads [pack] and builds the engine for it, once.
     *
     * Returns false when the pack is not usable, and that is the normal answer
     * for a phone that has downloaded nothing — the reader falls back to the
     * system voice rather than showing a dead control.
     *
     * The config is assembled from files that are DISCOVERED (see
     * [NeuralVoicePacks.findModel]) because the two packs do not agree on their
     * model's file name. `assetManager` is null on purpose: that makes the
     * runtime read from the filesystem, which is where a downloaded pack lives —
     * the AssetManager door is for models bundled into the APK, and nothing here
     * is.
     */
    fun prepare(context: Context, pack: NeuralVoicePacks.Pack): Boolean {
        val id = pack.id
        if (tts != null && loaded == id) return true
        release()
        val app = context.applicationContext
        val model = NeuralVoicePacks.findModel(app, pack) ?: return false
        val tokens = NeuralVoicePacks.tokensFile(app, pack).takeIf { it.isFile } ?: return false
        val dataDir = NeuralVoicePacks.espeakDataDir(app, pack).takeIf { it.isDirectory } ?: return false
        val attempt = runCatching {
            val voices = NeuralVoicePacks.voicesFile(app, pack)
            val lexicon = NeuralVoicePacks.lexicons(app, pack)
            val modelConfig = when {
                // Kokoro carries `voices.bin` and reads its speakers from it.
                pack.kind == NeuralVoicePacks.Kind.KOKORO && voices.isFile ->
                    OfflineTtsModelConfig(
                        kokoro = OfflineTtsKokoroModelConfig(
                            model = model.absolutePath,
                            voices = voices.absolutePath,
                            tokens = tokens.absolutePath,
                            dataDir = dataDir.absolutePath,
                            lexicon = lexicon,
                            // ⚠️ ISO 639-3, NOT THE "en" IT READS LIKE. This value
                            // is the espeak-ng voice the Kokoro frontend phonemizes
                            // with, and sherpa-onnx's own Android engine is explicit
                            // about it: its `kokoro-en-v0_19` entry passes `eng`
                            // (`scripts/apk/generate-tts-apk-script.py` converts the
                            // ISO 639-1 code it is written with through `Lang.pt3`,
                            // and the generated `TtsEngine.kt` carries the 639-3
                            // form). Left empty the model's own `voice` metadata is
                            // used instead — also "en-us" — but a value that is
                            // neither is a language espeak-ng cannot resolve.
                            lang = "eng",
                        ),
                        // Kokoro is the heavier model and the one whose RTF is
                        // closest to the line, so it is the one that wants the
                        // threads: Piper's 0.357 already has room to spare.
                        // 4 is not a guess — it is what sherpa-onnx's OWN Kotlin
                        // helper uses for a voices-carrying (Kokoro/Kitten) model
                        // (`getOfflineTtsConfig`: 4 when `voices` is set, else 2).
                        numThreads = threadsFor(4),
                        provider = "cpu",
                    )
                // Piper (and any future VITS pack): the model and espeak alone.
                else ->
                    OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = model.absolutePath,
                            tokens = tokens.absolutePath,
                            dataDir = dataDir.absolutePath,
                            lexicon = lexicon,
                        ),
                        numThreads = threadsFor(2),
                        provider = "cpu",
                    )
            }
            OfflineTts(
                config = OfflineTtsConfig(
                    model = modelConfig,
                    // ── v468 — A SENTENCE HAS TO END, AUDIBLY ─────────────
                    // sherpa's own default trims every silence the model draws to a
                    // fifth (0.2) — right for a short UI phrase, wrong for a book:
                    // the pause at a full stop becomes a click, so consecutive
                    // sentences run together as one breathless line (the member,
                    // about Piper · Lessac: *"not taking a break"*). 0.6 keeps a real
                    // break at the punctuation without stretching the gaps between
                    // words. THE KNOB, if it ever reads too slow: this constant.
                    silenceScale = SENTENCE_SILENCE_SCALE
                )
            )
        }
        // ⚠️ A PACK THAT WILL NOT LOAD IS NEVER SILENT ABOUT IT (v468). The member's
        // *"kokoro doesnt work at all"* reached us as nothing at all — no error, no
        // state, just the phone's own voice reading the book, which is
        // indistinguishable from a feature that ignored them. The reason is logged
        // now (it is the line a bug report would carry), so a pack the runtime
        // refuses says WHY wherever anyone looks.
        val built = attempt
            .onFailure { Log.e(TAG, "The voice pack '${pack.id}' could not be loaded", it) }
            .getOrNull() ?: return false
        tts = built
        loaded = id
        return true
    }

    /**
     * Never asks for more threads than the phone has, and never fewer than two.
     *
     * onnxruntime will happily accept a thread count larger than the device's
     * cores and then spend its time context-switching — which on a phone is heat
     * for no speed.
     */
    private fun threadsFor(wanted: Int): Int =
        wanted.coerceAtMost(Runtime.getRuntime().availableProcessors().coerceAtLeast(2))

    /**
     * Says [text] in the loaded pack's voice ([speakerId]), calling [onDone] on
     * the calling thread once the audio has finished or failed.
     */
    fun say(
        text: String,
        speed: Float,
        speakerId: Int,
        onDone: () -> Unit,
        /**
         * ── v468 — THE PACK SAID NOTHING, AND THAT IS NOT A SENTENCE ─────────
         *
         * This path used to report [onDone] whatever happened: a model that threw, a
         * frontend that could not phonemize a word, an empty sample array. The reader
         * counts `onDone` as *that sentence has been read*, so a 305 MB pack that
         * answers nothing was a pack that read the whole book in silence — the
         * member's *"kokoro doesnt work at all, like totally it doesnt work"* seen
         * from the page, where the voice they chose simply never makes a sound and
         * nothing anywhere says so.
         *
         * A failure now SAYS it is a failure, and the reader answers it exactly as it
         * answers a refused Edge socket: this pack is dropped for the rest of the
         * session and the SAME sentence is read in the phone's own voice, so the
         * member always hears a reading and always learns which voice is speaking.
         */
        onFail: () -> Unit
    ) {
        val engine = tts ?: run { onFail(); return }
        if (text.isBlank()) { onDone(); return }
        cancelled = false
        val mine = utterance.incrementAndGet()
        Thread {
            val audio = runCatching {
                // sherpa's own speed knob, pinned to the same window the system
                // voice is held to in ReaderSpeaker so the two paths feel alike.
                engine.generate(text, speakerId, speed.coerceIn(0.5f, 2.5f))
            }
                .onFailure { Log.e(TAG, "A voice pack could not say a sentence", it) }
                .getOrNull()
            // ── ⚠️ AN EMPTY SOUND IS A FAILURE, NOT A FINISHED SENTENCE ────
            //
            // Both shapes matter and both are reported: `null` (the model threw, or
            // the frontend could not turn the text into tokens) and a zero-length
            // result (the runtime accepted the sentence and produced nothing). The
            // one thing that must NOT happen is the reader being told the sentence is
            // spoken.
            if (audio == null || audio.samples.isEmpty() || audio.sampleRate <= 0) {
                Log.e(TAG, "A voice pack returned no audio for a sentence (${audio?.samples?.size ?: -1} samples)")
                // A pause or a skip is not this pack's fault: it is nobody's, and the
                // newer utterance owns the cursor.
                if (!cancelled && utterance.get() == mine) onFail() else onDone()
                return@Thread
            }
            // A newer utterance (or a pause) arrived while this one was being
            // made: drop it rather than playing over the member's place.
            if (cancelled || utterance.get() != mine) {
                onDone()
                return@Thread
            }
            play(audio.samples, audio.sampleRate, mine)
            onDone()
        }.start()
    }

    /**
     * Plays mono float samples through an [AudioTrack].
     *
     * `ENCODING_PCM_FLOAT` and `MODE_STREAM`, written in blocks rather than in
     * one call, purely so a pause can take effect part-way through a sentence:
     * a single `write` of a whole sentence would block until the buffer drained
     * and the member's Stop would appear to do nothing for two seconds.
     */
    private fun play(samples: FloatArray, sampleRate: Int, mine: Int) {
        if (sampleRate <= 0 || samples.isEmpty()) return
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                // One second of audio, so playback starts as soon as the first
                // block lands instead of waiting for the whole sentence.
                .setBufferSizeInBytes(sampleRate * 4)
                .build()
        }.getOrNull() ?: return
        this.track = track
        runCatching {
            track.play()
            var offset = 0
            while (offset < samples.size) {
                if (cancelled || utterance.get() != mine) break
                val size = minOf(BLOCK, samples.size - offset)
                val written = track.write(samples, offset, size, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
            }
            // ── ⚠️ THE AUDIO IN THE BUFFER HAS TO BE HEARD, NOT DISCARDED (v468) ──
            //
            // `AudioTrack.write` returns as soon as a block is COPIED into the
            // buffer, not when it has been played — so at the end of a sentence there
            // is still up to a buffer (~1 s at 24 kHz; see the buffer size below) of
            // UNPLAYED audio, and that buffer is the END of the sentence: the words
            // before its full stop and the packet of silence after them. `stop()` in
            // MODE_STREAM throws that whole buffer away.
            //
            // The member, twice, and about Piper · Lessac specifically: *"i was
            // skipping comma and full stop words like the words which were before
            // those 2 were getting skipped"*, then *"the lessac is skipping the words
            // which are before full stop and not taking a break"*. **One bug, both
            // halves of it**: the missing words were this buffer, and the missing
            // break was the trailing silence sitting in the same buffer.
            //
            // So the tail is DRAINED first. `playbackHeadPosition` counts the frames
            // the device has actually played, and the loop waits for the last of them.
            // It polls in short sleeps rather than awaiting a marker callback so that a
            // pause or a skip still interrupts it on the next poll — a drain that could
            // not be interrupted would make Stop feel broken instead of late.
            if (!cancelled && utterance.get() == mine) {
                // The clip's own duration, plus half a second of slack for the
                // device's own latency. Bounded by the audio itself, so a long
                // sentence waits longer than a short one and neither can hang.
                val deadline = SystemClock.elapsedRealtime() +
                    samples.size.toLong() * 1000L / sampleRate + DRAIN_SLACK_MS
                while (
                    !cancelled && utterance.get() == mine &&
                    track.playbackHeadPosition < samples.size &&
                    SystemClock.elapsedRealtime() < deadline
                ) {
                    Thread.sleep(DRAIN_POLL_MS)
                }
            }
            if (!cancelled && utterance.get() == mine) track.stop()
        }
        runCatching { track.release() }
        if (this.track === track) this.track = null
    }

    /** Stops whatever is being said, without unloading the model. */
    fun stop() {
        cancelled = true
        utterance.incrementAndGet()
        val track = this.track
        this.track = null
        runCatching {
            track?.pause()
            track?.flush()
            track?.release()
        }
    }

    /** The way out: stop, free the engine, forget the pack. */
    fun release() {
        stop()
        runCatching { tts?.free() }
        tts = null
        loaded = null
    }

    /** A block big enough to keep the audio thread fed, small enough to interrupt. */
    private const val BLOCK = 4096

    /**
     * How much of every silence the model draws survives, for a sentence's sake.
     *
     * sherpa's default is 0.2 (a fifth). See the note where it is set: a book needs
     * a break at its full stops, and the default turns that break into a click.
     */
    private const val SENTENCE_SILENCE_SCALE = 0.6f

    /** Slack added to a clip's own length before the drain gives up on it. */
    private const val DRAIN_SLACK_MS = 500L

    /**
     * How often the drain re-checks the playback head, in milliseconds.
     *
     * Small enough that a pause mid-drain lands within a frame or two of the tap
     * (~15 ms is under one frame at 60 Hz), large enough that the poll is not a spin.
     */
    private const val DRAIN_POLL_MS = 15L

    /** The tag every refusal from this object is logged under. */
    private const val TAG = "NeuralSpeaker"
}

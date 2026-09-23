package com.curio.app.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
        val built = runCatching {
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
                            lang = "en",
                        ),
                        // Kokoro is the heavier model and the one whose RTF is
                        // closest to the line, so it is the one that wants the
                        // threads: Piper's 0.357 already has room to spare.
                        numThreads = threadsFor(6),
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
                        numThreads = threadsFor(4),
                        provider = "cpu",
                    )
            }
            OfflineTts(config = OfflineTtsConfig(model = modelConfig))
        }.getOrNull() ?: return false
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
    fun say(text: String, speed: Float, speakerId: Int, onDone: () -> Unit) {
        val engine = tts ?: run { onDone(); return }
        if (text.isBlank()) { onDone(); return }
        cancelled = false
        val mine = utterance.incrementAndGet()
        Thread {
            val audio = runCatching {
                // sherpa's own speed knob, pinned to the same window the system
                // voice is held to in ReaderSpeaker so the two paths feel alike.
                engine.generate(text, speakerId, speed.coerceIn(0.5f, 2.5f))
            }.getOrNull()
            // A newer utterance (or a pause) arrived while this one was being
            // made: drop it rather than playing over the member's place.
            if (audio == null || cancelled || utterance.get() != mine) {
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
            // Drain rather than cut: a sentence that is stopped the instant after
            // its last block is written should still finish being heard, or the
            // reader's own pause sounds like a glitch.
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
}

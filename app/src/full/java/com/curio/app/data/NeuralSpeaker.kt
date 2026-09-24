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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

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

    /**
     * ── v469 — WHICH ENGINE A QUEUED JOB BELONGS TO ────────────────────────
     *
     * Synthesis runs on a QUEUE now (see [synth]), so a job can be waiting when
     * the member leaves the reader — and [release] frees the model. A job captures
     * this number when it is queued and gives up if the engine has been replaced
     * since, and the free is queued on the SAME LANE so it can never land in the
     * middle of a synthesis that is already running.
     */
    private val engineGen = AtomicInteger(0)

    /**
     * ── v469 — THE SENTENCE MADE AHEAD OF THE ONE PLAYING ─────────────────
     *
     * The member: *"they stop way too long on full stops like maybe for 2 sec or
     * something fix it and make it natural"*. On a phone a neural pack cannot keep
     * up with its own voice — Piper medium is 0.357 RTF on a Raspberry Pi 4 with
     * four threads and this device runs it on two — so when the next sentence was
     * only asked for AFTER the last one had finished playing, every full stop paid
     * the whole synthesis. That wait IS the two seconds.
     *
     * So the sentence after this one is synthesised WHILE this one is in the
     * speaker ([prefetch], called by `sayAloud` with the text it already holds) and
     * waits here. One slot is enough: a reading walks forward one sentence at a
     * time, and the speaker is the only thing that can be listening.
     */
    @Volatile private var ahead: Ahead? = null

    /** The text a prefetch job is making right now, so one promise is paid once. */
    @Volatile private var making: String? = null

    /**
     * ── v469 — THE READING'S OWN GENERATION FOR WORK MADE AHEAD ───────────
     *
     * Bumped by [stop] — a pause, a skip, a voice change, the end of a reading:
     * the sentence a queued prefetch was making is not the one wanted any more, and
     * on a phone synthesising it anyway is seconds of the member's battery spent on
     * words nobody will hear. (A skip that leaves four stale sentences queued would
     * otherwise stall the sentence they DID ask for behind all four.)
     *
     * ⚠️ WHY THIS IS SAFE HERE AND WAS NOT FOR [EdgeVoice]: that voice's `say` calls
     * its own `stop` before EVERY sentence, so a generation bumped by a stop there
     * would throw away the head start on every single line. This object's `say`
     * never stops anything, so a stop only ever means what it says.
     */
    @Volatile private var prefetchGen = 0

    /**
     * ── v469 — ONE LANE FOR MAKING AUDIO, ONE FOR PLAYING IT ───────────────
     *
     * Two `generate` calls at once on a phone's two cores do not overlap, they
     * halve each other — so synthesis is serial and FIFO, and **the sentence being
     * spoken is queued BEFORE the sentence after it** (that is what `sayAloud`
     * does, and it is the one ordering rule this pipeline has).
     *
     * Playback is a lane of its own because `play` blocks for the whole clip while
     * it drains the audio the device has not played yet: on one lane, every
     * prefetch would have to wait for the sentence in front of it to finish
     * sounding — which is exactly the wait this pipeline exists to remove.
     */
    private val synth = Executors.newSingleThreadExecutor { job ->
        // Daemon: these two lanes outlive a reading, and a parked thread that
        // could hold a process open is not worth the microseconds it saves.
        Thread(job, "curio-voice-synth").apply { isDaemon = true }
    }
    private val playback = Executors.newSingleThreadExecutor { job ->
        Thread(job, "curio-voice-play").apply { isDaemon = true }
    }

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
                            // ── v469 — ⚠️ EMPTY, AND THAT IS THE KOKORO FIX ────
                            //
                            // This field was `"eng"`, chosen (v465j) on the
                            // reasoning that the runtime wants an ISO 639-3 code.
                            // It does not, and the mistake is worth recording
                            // because it produced a pack that LOADED PERFECTLY and
                            // said nothing at all.
                            //
                            // `kokoro-en-v0_19` is a v0.19 model, and sherpa-onnx
                            // gives every such model a `PiperPhonemizeLexicon`
                            // frontend — whose `ConvertTextToTokenIds(text, voice)`
                            // hands this value to **espeak-ng as the VOICE NAME**:
                            //
                            //   config.voice = voice; // e.g., voice is en-us
                            //   piper::phonemize_eSpeak(text, config, phonemes);
                            //
                            // and that call THROWS when espeak-ng cannot resolve
                            // the name (the library's own comment: "throws if
                            // espeak-ng does not recognize config.voice, e.g., when
                            // a user passes an unsupported --kokoro-lang").
                            // `OfflineTtsKokoroImpl::Generate` catches nothing — it
                            // clears the phonemes, gets no token ids, and returns an
                            // EMPTY result. The engine was up, `numSpeakers()`
                            // answered 11, the health test said "Loaded · 11 voices"
                            // and the model generated 0 samples (the member: *"it
                            // says loaded 11 voices it doesnt show or work when
                            // choosen"*).
                            //
                            // **`"eng"` is not a voice espeak-ng has.** Its voices
                            // are named `en`, `en-us`, `en-gb`; `"eng"` is the kind
                            // of plausible-looking code that no test in this
                            // environment could ever have caught. Left EMPTY, the
                            // runtime uses the model's own `voice` metadata —
                            // "en-us" for this pack, which is the very example the
                            // source comments give — so the voice comes from the
                            // model rather than from a guess. See
                            // `OfflineTtsKokoroImpl::Generate`:
                            // `lang = config_.model.kokoro.lang.empty() ?
                            // meta_data.voice : config_.model.kokoro.lang`.
                            lang = "",
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
        val gen = engineGen.get()
        // ── v469 — THE SENTENCE THE PREFETCH ALREADY MADE ───────────────────
        //
        // [prefetch] synthesises the sentence AFTER this one while this one plays,
        // and this is where that promise is cashed: a clip made for exactly these
        // words, in this voice, at this speed is played at once — with no synthesis
        // at all between the two sentences. `Ahead.matches` is deliberately exact,
        // because a cache that guesses is a cache that plays the wrong sentence in
        // the right voice.
        val ready = ahead?.takeIf { it.matches(text, speed, speakerId) }
        if (ready != null) ahead = null
        // ⚠️ QUEUED, NOT THREADED. See [synth]: one synthesis at a time keeps the
        // sentence the member is waiting to hear from being slowed down by the
        // sentence after it, and it is what makes the prefetch below a head start
        // rather than a competitor.
        synth.execute {
            // The engine was replaced (or freed) while this job sat in the queue.
            if (engineGen.get() != gen) { onDone(); return@execute }
            // A newer sentence (or a pause) landed first: nobody is waiting on this
            // one any more, and making its audio would be seconds of wasted work.
            if (utterance.get() != mine) { onDone(); return@execute }
            val clip = ready ?: synthesise(engine, text, speed, speakerId)
            // ── ⚠️ AN EMPTY SOUND IS A FAILURE, NOT A FINISHED SENTENCE ────
            //
            // Both shapes matter and both are reported: `null` (the model threw, or
            // the frontend could not turn the text into tokens) and a zero-length
            // result (the runtime accepted the sentence and produced nothing). The
            // one thing that must NOT happen is the reader being told the sentence is
            // spoken.
            if (clip == null) {
                // A pause or a skip is not this pack's fault: it is nobody's, and the
                // newer utterance owns the cursor.
                if (!cancelled && utterance.get() == mine) onFail() else onDone()
                return@execute
            }
            // A newer utterance (or a pause) arrived while this one was being
            // made: drop it rather than playing over the member's place.
            if (cancelled || utterance.get() != mine) {
                onDone()
                return@execute
            }
            // The speaker is its own lane, so this blocks only the PLAYBACK queue
            // while the synthesis lane is already free for the next sentence.
            playback.execute {
                play(clip.samples, clip.sampleRate, mine)
                onDone()
            }
        }
    }

    /**
     * ── v469 — MAKE THE SENTENCE AFTER THIS ONE, WHILE THIS ONE PLAYS ─────
     *
     * Called by `sayAloud` with the text the reading is about to want (the page
     * already holds it — one index into its own sentence list), and does nothing at
     * all when there is nothing ahead to make.
     *
     * ⚠️ **THE ORDER MATTERS: THIS IS CALLED AFTER [say], NEVER BEFORE IT.** Both
     * jobs go on one FIFO lane, and the sentence being spoken has to be made first —
     * a prefetch queued in front of it would delay the very words the member asked
     * for by the whole length of the one they have not asked for yet.
     *
     * A failure is silent on purpose: this is an optimisation, so a pack that cannot
     * make the next sentence makes it in [say] instead, where the reader's own
     * failure path (the fallback to the phone's voice) lives. A cache that could fail
     * a sentence would be a cache deciding the reading.
     */
    fun prefetch(text: String, speed: Float, speakerId: Int) {
        val engine = tts ?: return
        if (text.isBlank()) return
        val held = ahead
        if (held != null && held.matches(text, speed, speakerId)) return
        if (making == text) return
        making = text
        val gen = engineGen.get()
        val reading = prefetchGen
        synth.execute {
            if (engineGen.get() == gen && prefetchGen == reading) {
                val clip = synthesise(engine, text, speed, speakerId)
                if (clip != null && engineGen.get() == gen && prefetchGen == reading) {
                    ahead = clip
                }
            }
            if (making == text) making = null
        }
    }

    /**
     * One sentence, made whole: synthesis, then the trim that makes its ending
     * sound like a reader rather than a machine (see [trimmed]).
     *
     * Returns null when the pack gave nothing back — the model threw, the frontend
     * could not phonemize, or the result was silent from end to end — and logs it,
     * because that log line is what a bug report about a pack carries.
     */
    private fun synthesise(engine: OfflineTts, text: String, speed: Float, speakerId: Int): Ahead? {
        val audio = runCatching {
            // sherpa's own speed knob, pinned to the same window the system voice is
            // held to in ReaderSpeaker so the two paths feel alike.
            engine.generate(text, speakerId, speed.coerceIn(0.5f, 2.5f))
        }
            .onFailure { Log.e(TAG, "A voice pack could not say a sentence", it) }
            .getOrNull()
        if (audio == null || audio.samples.isEmpty() || audio.sampleRate <= 0) {
            Log.e(
                TAG,
                "A voice pack returned no audio for a sentence " +
                    "(${audio?.samples?.size ?: -1} samples)"
            )
            return null
        }
        val samples = trimmed(audio.samples, audio.sampleRate)
        if (samples.isEmpty()) {
            Log.e(TAG, "A voice pack returned a sentence of pure silence")
            return null
        }
        return Ahead(text, speed, speakerId, samples, audio.sampleRate)
    }

    /**
     * ── v469 — THE PAUSE AT A FULL STOP IS THE READER'S, NOT THE MODEL'S ────
     *
     * A neural model draws its own silence around every sentence, and sherpa's own
     * `silenceScale` only scales it — this object keeps 0.6 (see
     * [SENTENCE_SILENCE_SCALE]) — so a clip can end with most of a second of
     * nothing, and the reader
     * then holds its own grace on top of it. Together that is the *"they stop way
     * too long on full stops like maybe for 2 sec"* the member hears, even with the
     * synthesis out of the way.
     *
     * So the clip is trimmed to what a reader actually does: a SHORT breath at the
     * end ([TAIL_BREATH_MS]) and no dead air at the front. Silence INSIDE the
     * sentence is untouched — a comma's pause is the model's and belongs to the
     * words — because only the run at each END is removed.
     *
     * A clip that is silent from end to end trims to nothing, and an empty clip is
     * treated exactly as a pack that said nothing at all (see [synthesise]) — which
     * is the shape a broken pack takes, and it must never be counted as a read
     * sentence.
     */
    private fun trimmed(samples: FloatArray, sampleRate: Int): FloatArray {
        if (samples.isEmpty() || sampleRate <= 0) return samples
        val perMs = sampleRate / 1000f
        fun quiet(at: Int) = abs(samples[at]) < SILENCE_LEVEL
        var first = 0
        while (first < samples.size && quiet(first)) first++
        if (first >= samples.size) return FloatArray(0)
        // A whisker of the original lead is kept so the first syllable is not
        // clipped into a click.
        first = maxOf(0, first - (LEAD_PAD_MS * perMs).toInt())
        var last = samples.size - 1
        while (last > first && quiet(last)) last--
        val end = minOf(samples.size, last + 1 + (TAIL_BREATH_MS * perMs).toInt())
        return if (first == 0 && end == samples.size) samples else samples.copyOfRange(first, end)
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
        // ── v469 — AND THE WORK MADE AHEAD GOES WITH IT ──────────────────
        //
        // A pause, a skip, a voice change or the end of a reading: whatever was
        // being made for the sentence after this one is not wanted any more, and a
        // queued job that synthesises it anyway is the member's battery spent on
        // words nobody will hear. The clip ALREADY made is kept — a pause resumed
        // on the same sentence should not have to make it again.
        prefetchGen += 1
        val track = this.track
        this.track = null
        runCatching {
            track?.pause()
            track?.flush()
            track?.release()
        }
    }

    /**
     * The way out: stop, free the engine, forget the pack.
     *
     * ── v469 — THE FREE IS QUEUED ON THE SYNTHESIS LANE ──────────────────
     *
     * `OfflineTts.free()` releases the onnxruntime session, and running it while a
     * sentence is being made in that very session is a crash rather than a leak. The
     * synthesis lane is FIFO, so queueing the free behind the jobs that were queued
     * before it means it can only ever run when nothing is using the engine — and
     * jobs queued AFTER it carry the new [engineGen] and give up on their own. (The
     * reader calls this from a `DisposableEffect`, i.e. on the main thread, so the
     * free must not be waited on here either.)
     */
    fun release() {
        stop()
        val engine = tts
        tts = null
        loaded = null
        ahead = null
        making = null
        engineGen.incrementAndGet()
        if (engine != null) {
            runCatching { synth.execute { runCatching { engine.free() } } }
        }
    }

    /**
     * ── v469 — ONE SENTENCE'S AUDIO, MADE AND NOT YET PLAYED ───────────────
     *
     * The cue is `text | speed | speaker`, and [matches] is exact on all three:
     * a narrator picked mid-book, a speed nudged, a sentence skipped all miss this
     * deliberately — the one failure a cache like this can have is playing the
     * wrong sentence in the right voice, and an exact match is the whole guard.
     */
    private class Ahead(
        val text: String,
        val speed: Float,
        val speakerId: Int,
        val samples: FloatArray,
        val sampleRate: Int,
    ) {
        fun matches(otherText: String, otherSpeed: Float, otherSpeaker: Int): Boolean =
            text == otherText && speed == otherSpeed && speakerId == otherSpeaker
    }

    /** A block big enough to keep the audio thread fed, small enough to interrupt. */
    private const val BLOCK = 4096

    /**
     * Where a sample stops counting as silence (v469).
     *
     * About \u221242 dBFS: a real recording's noise floor is far below it, and a
     * value any lower would fail to find the end of a clip that fades rather than
     * stops.
     */
    private const val SILENCE_LEVEL = 0.008f

    /**
     * How much of the model's silence is left after a sentence, in milliseconds
     * (v469). A full stop in a well-read book is a breath, not a gap: this is that
     * breath, and it is the whole of the pause a neural pack gets (see
     * `aloudTailGraceMs`, which adds nothing on top of it for this voice).
     */
    private const val TAIL_BREATH_MS = 260f

    /** The sliver of the clip's own lead kept so a first syllable is not clipped. */
    private const val LEAD_PAD_MS = 20f

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

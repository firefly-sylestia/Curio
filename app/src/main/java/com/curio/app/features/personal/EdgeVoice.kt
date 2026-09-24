package com.curio.app.features.personal

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * ── v465f — THE EDGE VOICE: A HIDDEN, DEV-ONLY EXPERIMENT ─────────────────
 *
 * The member's §58 choice, and the one voice in the reader that is deliberately
 * NOT a shipping path: *"Edge TTS as a hidden dev-only experiment"*.
 *
 * **WHAT IT IS.** Microsoft Edge's own read-aloud endpoint — the WebSocket the
 * browser opens when you press "Read aloud" — driven directly. It needs **no
 * key, no account and no signup**, and its neural voices are excellent, which is
 * exactly why it was asked for.
 *
 * **WHY IT IS HIDDEN AND OFF BY DEFAULT.** It is **undocumented and
 * unsanctioned**: it is not a published API, Microsoft has never offered it as
 * one, and it can be rate-limited, changed or switched off without notice —
 * there is nothing to be notified about, because there is no contract. Shipping
 * it as a supported path would mean an app whose reading voice can stop working
 * one morning for reasons no changelog could explain. Behind a switch that is
 * off until a member goes looking, it is an experiment with an honest lifespan.
 *
 * **WHAT IT IS NOT.** It is not a fallback and never a default. The phone's own
 * engine reads by default; a downloaded pack is offline and permanent; this is
 * the third option, reachable only from Experiments, and the reader falls back
 * to the phone's voice the moment it fails (see `sayAloud`).
 *
 * Three things about the protocol are load-bearing and are commented at the
 * point they are used: the **signed token** in the query string (Microsoft added
 * it, and without it every connection is refused), the **binary frame layout**
 * (two length bytes, then a text header, then the audio), and **turn.end** as
 * the only reliable completion signal.
 *
 * ── v465j — WHY IT WAS NOT WORKING AT ALL, AND WHAT THE UPGRADE NOW NEEDS ──
 *
 * The member: *"edge tts wasnt working or something it was just going fast the
 * highlight with no sound"*. That was the **upgrade** being refused, every time,
 * and the shape of the refusal is worth recording because the read-aloud path
 * around it was (correctly) blamed first:
 *
 *  - **The 403 arrives on the WebSocket upgrade, not on a message.** OkHttp
 *    answers it with `onFailure(response)`, `fetch` returns null, and the reader
 *    falls back (see `sayAloud`). There is no audio frame to be missing, and
 *    nothing to retry inside the socket — so a fix at the message layer could
 *    never have touched it.
 *  - **THE IDENTITY HEADERS ARE NOT DECORATION.** The endpoint now judges the
 *    client by them: the reference implementation (`rany2/edge-tts`, whose
 *    `constants.py` is the source of every constant here) sends an **Edge
 *    `User-Agent`**, the browser extension's **`Origin`**, no-cache headers and a
 *    **`muid` cookie**. OkHttp's own `okhttp/4.x` user agent and empty Origin
 *    identify an Android app, which is exactly what the service refuses.
 *  - **AND THE VERSION ROTS ON A SCHEDULE.** `Sec-MS-GEC-Version` has to name a
 *    build the endpoint still recognises; the value this file carried was
 *    **Edge 131** (`1-131.0.2903.86`) — twelve majors behind the 143 that the
 *    reference client sends now.
 *
 * ⚠️ **WHAT TO DO WHEN IT STOPS AGAIN.** It will: this is undocumented and
 * unsanctioned by construction (see above). Bump [GEC_VERSION] and
 * [EDGE_USER_AGENT] to `CHROMIUM_FULL_VERSION` and `BASE_HEADERS['User-Agent']`
 * in edge-tts's `constants.py`. Nothing else in this file should need to change.
 */
internal object EdgeVoice {

    /**
     * The token every client sends, including Edge itself. It is not a secret
     * and not a credential — it identifies the client as Edge's read-aloud
     * front-end. Public by construction: it ships inside the browser's own JS.
     */
    private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"

    private const val ENDPOINT =
        "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"

    /**
     * The Edge build this pretends to be, sent as `Sec-MS-GEC-Version`.
     *
     * ⚠️ THIS IS THE MOST LIKELY THING TO ROT. The endpoint checks that the
     * version is one it recognises; an old one can be refused outright (a 403 on
     * the upgrade). It is a single constant precisely so that drift is a one-line
     * fix, and it is in the experiment's row description so a member who turns it
     * on knows what they are relying on.
     */
    private const val GEC_VERSION = "1-143.0.3650.75"

    /**
     * The Edge build [GEC_VERSION] belongs to, sent as the `User-Agent`.
     *
     * Verbatim from edge-tts's `constants.py` (`BASE_HEADERS`), built from the
     * same `CHROMIUM_FULL_VERSION` as [GEC_VERSION] so the two can never drift
     * apart — a UA claiming 143 beside a signature claiming 131 is a mismatch on
     * its own.
     */
    private const val EDGE_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"

    /**
     * The read-aloud extension's own origin, which is what the service expects a
     * browser to announce itself with. Public by construction — it ships inside
     * Edge's own package.
     */
    private const val EXTENSION_ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold"

    /**
     * The smallest reply that counts as a sentence, in bytes.
     *
     * 48 kbit/s mono MP3 spends the head of the file on headers, so anything
     * under this is a truncated stream rather than speech — and the phone's own
     * voice reads that sentence far better than a click does.
     */
    private const val MIN_CLIP_BYTES = 512L

    /**
     * Where a sample stops counting as silence when a clip's tail is cut (v471).
     *
     * About −42 dBFS, the same floor the downloaded packs use, plus the RELATIVE one
     * beside it — see [audibleEnd].
     */
    private const val TAIL_SILENCE_LEVEL = 0.008f

    /** The share of a clip's own peak that still counts as its tail's silence (v471). */
    private const val TAIL_LEVEL_SHARE = 0.03f

    /** Mono MP3 at 24 kHz — the format the endpoint sends by default. */
    private const val OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3"

    /**
     * The voices offered, and the reason `speakVoice` can hold a name like this
     * at all: for the Edge engine the stored voice IS the endpoint's voice name,
     * which is the same shape the Android engine's voice names have — so the
     * Voice row needs no third branch.
     */
    val VOICES: List<Pair<String, String>> = listOf(
        "en-US-AriaNeural" to "Aria \u00b7 American female",
        "en-US-JennyNeural" to "Jenny \u00b7 American female",
        "en-US-GuyNeural" to "Guy \u00b7 American male",
        "en-GB-SoniaNeural" to "Sonia \u00b7 British female",
        "en-GB-RyanNeural" to "Ryan \u00b7 British male",
    )

    /** What the reader says when the member never opened the Voice row. */
    const val DEFAULT_VOICE = "en-US-AriaNeural"

    private val main = Handler(Looper.getMainLooper())

    private var socket: WebSocket? = null
    private var player: MediaPlayer? = null

    /**
     * ── v468 — THE SENTENCE THAT IS ALREADY FETCHED, AND ITS CUE ─────────
     *
     * The member: *"the edge tts stops way too long at full stops maybe sentence by
     * sentence or is it playing online and that show much time it takes to load, fix the
     * loading and pre load the next paragraph so its not slow like rn"*. It is playing
     * online, and the pause is the whole round trip: a fresh WebSocket handshake and a
     * fresh synthesis per sentence, started only once the previous one has finished.
     *
     * So the next sentence is fetched WHILE this one is being read ([prefetch], driven by
     * `sayAloud`'s `nextText`) and its bytes wait here. Three fields, and the CUE is what
     * makes them safe: a clip belongs to `voice|speed|text`, so a change of voice, of
     * speed or of sentence simply misses rather than playing the wrong sentence in the
     * right voice — which is the one way a cache like this can lie to a member.
     *
     * ── v468 — ONE CLIP BECAME A PARAGRAPH ([PREFETCH_SLOTS]) ─────────────
     *
     * A single slot fixes the pause at every full stop but does nothing for the one
     * place a reading still stutters: the FIRST sentence, which has nothing fetched
     * before it because nothing has played yet. So the cache is a MAP keyed by the same
     * cue — [warm] fills it with the opening paragraph when a reading starts, [say]
     * drains it as the reading walks forward, and a sentence that is not in it is simply
     * fetched as before. A map is what makes several clips safe at once: each cue names
     * its own slot file, so two saved sentences can never be confused for each other.
     */
    @Volatile
    private var readyClips: Map<String, File> = emptyMap()

    /** The cues being fetched right now, so one promise is never paid for twice. */
    @Volatile
    private var fetchingKeys: Set<String> = emptySet()

    /**
     * Bumped by [warm] — the one place a reading's position is known to move.
     *
     * A prefetch outlives the sentence that asked for it, so its result can arrive after
     * the member has paused, skipped or moved on — and publishing it then would leave a
     * stale clip keyed to a sentence the reading has left behind. Each prefetch carries
     * the generation it started in and publishes only if that generation is still the
     * current one, which is a cheaper and more honest answer than cancelling workers that
     * are already mid-socket. Written from the main thread only ([say]/[warm]), read from
     * worker threads.
     */
    @Volatile
    private var prefetchGen = 0

    /** The cue a clip belongs to: who is reading, how fast, and which words. */
    private fun cue(text: String, voice: String, speed: Float): String = "$voice|$speed|$text"

    /**
     * How far this phone's clock is from the endpoint's, in milliseconds.
     *
     * **THE ONE PART OF THE TOKEN THAT IS NOT OURS TO GET RIGHT.** [gec] signs
     * the device's own time, and the endpoint validates it against ITS five-minute
     * window — so a phone a few minutes out signs every handshake with a value
     * that looks forged, and no header can fix that. edge-tts corrects in exactly
     * this way: read the server's `Date` out of the refused upgrade, keep the
     * difference, and ask again. Zero until a refusal has taught us otherwise.
     */
    @Volatile
    private var clockSkewMs: Long = 0L

    /** Below this a difference is ordinary network jitter, not a wrong clock. */
    private const val SKEW_FLOOR_MS = 30_000L

    /**
     * ── v468 — HOW MANY SENTENCES ONE HEAD START COVERS ───────────────────
     *
     * Three, because the member asked to *"pre load the next paragraph"* and a
     * paragraph in this reader is one to three sentences: three slots are always
     * enough to have the paragraph after the current one in hand, and small enough
     * that a phone never holds more than a few hundred kilobytes of audio nobody
     * has asked for yet. [warm] fills them; [say] drains them one at a time.
     */
    private const val PREFETCH_SLOTS = 3

    /** The slot the sentence being SPOKEN is written to — never an index in [PREFETCH_SLOTS]. */
    private const val PLAYING_SLOT = -1

    /** Bumped per utterance so a late completion cannot resume a newer one. */
    @Volatile private var utterance = 0

    /**
     * Where a clip is written. ONE PLAYING SLOT PLUS [PREFETCH_SLOTS] AHEAD (v468).
     *
     * The sentence coming out of the speaker owns the playing slot, and each prefetched
     * sentence owns a numbered slot of its own — so a prefetch can never write the file
     * `MediaPlayer` is reading, and two saved sentences can never land on each other.
     */
    private fun clipFile(context: Context, slot: Int = PLAYING_SLOT, gen: Int = 0): File = File(
        context.cacheDir,
        // ⚠️ THE GENERATION IS IN THE NAME, and that is what makes a stale prefetch
        // harmless rather than merely unpublishable: a fetch started in an older
        // generation writes its OWN files and cannot corrupt the slots a newer
        // [warm] is filling, even while both are mid-socket.
        if (slot == PLAYING_SLOT) "edge-voice.mp3" else "edge-voice-next-$gen-$slot.mp3"
    )

    /**
     * Says [text] in [voiceName], calling [onDone] on the MAIN thread — after the
     * audio finishes, or immediately if anything at all goes wrong.
     *
     * Never throws: a voice that cannot be reached must leave the reader exactly
     * where it was, which is what the immediate [onDone] guarantees.
     */
    fun say(
        context: Context,
        text: String,
        speed: Float,
        voiceName: String,
        onDone: () -> Unit,
        /**
         * v465i — THE VOICE COULD NOT BE REACHED, AND SAYING SO IS THE POINT.
         *
         * This used to be `onDone()`, and that one line was the reported bug: the
         * reader treats a finished utterance as "that sentence has been read", so
         * a refused socket, a 403 from a rotated GEC version or a dead network
         * advanced the cursor anyway — the read-along wash raced through the page
         * in silence (member: *"edge tts wasnt working or something it was just
         * going fast the highlight with no sound"*). A failure now says it is a
         * failure, and the reader answers it by reading the sentence in the
         * phone's own voice instead (see `sayAloud`).
         */
        onFail: () -> Unit
    ) {
        if (text.isBlank()) { onDone(); return }
        stop()
        val mine = utterance
        val app = context.applicationContext
        val voice = voiceName.ifBlank { DEFAULT_VOICE }
        Thread {
            val clip = clipFor(app, text, speed, voice)
            // A NEWER SENTENCE, OR A PAUSE, ARRIVED WHILE THIS ONE WAS IN FLIGHT:
            // nobody is waiting on this one any more — the run that superseded it
            // owns the cursor — so it reports NOTHING. Reporting "done" here was
            // the other half of the same bug: a stale clip's completion moved the
            // reading on from a sentence the member had already left.
            if (utterance != mine) return@Thread
            if (clip == null) {
                main.post { if (utterance == mine) onFail() }
                return@Thread
            }
            main.post { play(clip, mine, onDone, onFail) }
        }.start()
    }

    /**
     * ── v468 — FETCH THE SENTENCE AFTER THIS ONE, WHILE THIS ONE PLAYS ───────
     *
     * Called by `sayAloud` with the text the reader is about to want next, and does
     * nothing at all unless there IS a next sentence to fetch. Runs on its own thread and
     * publishes into [readyClips] only if its generation is still current, so two
     * promises can never race each other into the wrong audio.
     *
     * **A failure is remembered as an empty slot, not as an error.** The prefetched clip
     * is an optimisation: if the fetch is refused (the endpoint is having one of its
     * days, or the phone just went through a tunnel) then [say] simply makes the request
     * itself, gets its own verdict, and reports the failure through the ordinary path —
     * which is where the reader's fallback to the phone's voice lives. A prefetch that
     * could fail the reading would be a cache deciding a sentence's fate.
     *
     * ⚠️ It shares this object's single [socket] field, so pausing or skipping cancels a
     * prefetch that happens to be in flight. That is a wasted head start on one sentence
     * and nothing more: the reading itself never depends on it.
     */
    fun prefetch(context: Context, text: String, speed: Float, voiceName: String, slot: Int = 0) {
        if (text.isBlank()) return
        if (slot < 0 || slot >= PREFETCH_SLOTS) return
        val app = context.applicationContext
        val voice = voiceName.ifBlank { DEFAULT_VOICE }
        val key = cue(text, voice, speed)
        if (readyClips[key]?.isFile == true) return
        if (fetchingKeys.contains(key)) return
        // ⚠️ CLAIMED ON THE CALLING THREAD, BEFORE ITS WORKER EXISTS. A [warm] and
        // a `sayAloud`'s own `nextText` often want the SAME sentence at the same
        // moment, and if both passed this gate they would both write it — this is
        // the line that makes the second one a no-op instead.
        fetchingKeys = fetchingKeys + key
        val gen = prefetchGen
        Thread {
            if (prefetchGen != gen) {
                fetchingKeys = fetchingKeys - key
                return@Thread
            }
            val clip = runCatching { fetch(app, text, speed, voice, slot, gen) }.getOrNull()
            // A GENERATION THAT IS NO LONGER CURRENT IS DROPPED (see [prefetchGen]):
            // the reading has moved, so this sentence is not the one wanted now.
            if (prefetchGen == gen && clip != null && clip.isFile) {
                readyClips = readyClips + (key to clip)
            }
            fetchingKeys = fetchingKeys - key
        }.start()
    }

    /**
     * ── v468 — WARM THE FIRST PARAGRAPH ──────────────────────────────────
     *
     * The member, after the first head start landed: *"and warm first paragraph"*.
     * [prefetch] alone can never do that, because it always knows only the sentence
     * after the one playing — so the FIRST sentence of a reading still paid a full
     * round trip, and the first words of a book were the one place the reading still
     * stuttered.
     *
     * So the driver hands over the next few sentences at once when the reading starts
     * (and again after a jump), and this fills a slot for each of them. A sentence
     * already in hand is skipped by [prefetch] itself, so calling this twice over
     * overlapping text costs nothing.
     *
     * ⚠️ The generation moves here, which is what frees the new warm's slots: any
     * fetch still carrying the old one writes its own files and is dropped on arrival.
     */
    fun warm(context: Context, texts: List<String>, speed: Float, voiceName: String) {
        prefetchGen += 1
        texts.take(PREFETCH_SLOTS).forEachIndexed { index, next ->
            prefetch(context, next, speed, voiceName, slot = index)
        }
    }

    /**
     * The clip for one utterance: the prefetched one when it is EXACTLY this sentence's,
     * otherwise a fresh fetch. Runs on the caller's worker thread — the copy below is
     * file I/O and must never be on the main one.
     */
    private fun clipFor(app: Context, text: String, speed: Float, voice: String): File? {
        val ready = takeReady(text, voice, speed)
        if (ready != null) {
            // ⚠️ COPIED INTO THE PLAYING SLOT, NEVER PLAYED IN PLACE. The prefetch slot
            // is written again by the NEXT prefetch, and that write must not land in the
            // file `MediaPlayer` is reading. A tenth of a megabyte, off the main thread.
            val copied = runCatching {
                val live = clipFile(app)
                // ⚠️ THE COPY KEEPS THE READY CLIP'S OWN EXTENSION (v471). The
                // playing slot is NAMED `.mp3`, but the clip that is ready may be
                // the TRIMMED `.wav` (see [trimToWav]) — copying a WAV's bytes into
                // a file whose name says it is an MP3 is the kind of lie that works
                // until the day a player trusts the extension.
                val target = File(live.parentFile, live.nameWithoutExtension + "." + ready.extension)
                ready.copyTo(target, overwrite = true)
                target
            }.getOrNull()
            if (copied != null) return copied
        }
        return runCatching { fetch(app, text, speed, voice) }.getOrNull()
    }

    /** Takes the prefetched clip when it belongs to this cue, and only then. */
    private fun takeReady(text: String, voice: String, speed: Float): File? {
        val key = cue(text, voice, speed)
        val file = readyClips[key] ?: return null
        // Removed on TAKE, so one saved clip is never played twice: the reading
        // walks forward, and the sentence behind it is not coming back.
        readyClips = readyClips - key
        return file.takeIf { it.isFile }
    }

    /** Stops whatever is being said. Safe to call when nothing is. */
    fun stop() {
        // ⚠️ NO [prefetchGen] BUMP HERE, DELIBERATELY. `say` stops the previous
        // utterance before EVERY sentence, so a bump in this method would
        // invalidate the prefetch `sayAloud` had just started for the next line —
        // the head start would be thrown away on every single sentence, which is
        // precisely the pause this cache exists to remove.
        utterance += 1
        runCatching { socket?.cancel() }
        socket = null
        val playing = player
        player = null
        runCatching {
            playing?.stop()
            playing?.release()
        }
    }

    // ── THE WIRE ──────────────────────────────────────────────────────────

    /**
     * Opens the socket, asks for [text], and returns the finished MP3 — or null
     * if the turn never completed. Runs on a background thread.
     */
    private fun fetch(
        context: Context,
        text: String,
        speed: Float,
        voice: String,
        /**
         * Which slot to write: [PLAYING_SLOT] for the sentence being spoken, or a
         * prefetch slot 0..[PREFETCH_SLOTS]-1 — never the one being played.
         */
        slot: Int = PLAYING_SLOT,
        /** The generation that asked for this fetch — see [clipFile] and [prefetchGen]. */
        gen: Int = 0
    ): File? {
        val clip = clipFile(context, slot, gen)
        if (clip.exists()) clip.delete()
        var turn = fetchOnce(text, speed, voice)
        // ── ONE RETRY, AND ONLY EVER BECAUSE OF THE CLOCK (v465j) ─────────
        // A refusal that came with the server's own date is the one failure here
        // that is this phone's fault and ours to repair; see [clockSkewMs]. The
        // retry is bounded at one, and only when the difference is real — a
        // second refusal is the service saying no, and asking a third time is how
        // an experiment gets rate-limited into never working again.
        val refusedAt = turn.refusedAt
        if (turn.audio == null && refusedAt != null) {
            val skew = refusedAt - System.currentTimeMillis()
            if (abs(skew) > SKEW_FLOOR_MS) {
                clockSkewMs = skew
                turn = fetchOnce(text, speed, voice)
            }
        }
        val audio = turn.audio ?: return null
        clip.writeBytes(audio)
        // A clip has to be more than a few hundred bytes of MP3 header to be
        // worth a MediaPlayer: anything smaller is a truncated turn, and playing
        // it is a click rather than a sentence.
        if (clip.length() <= MIN_CLIP_BYTES) return null
        // ── v471 — ITS FULL STOP IS CUT TO THE MEMBER'S OWN BREAK ─────────────
        //
        // The member: *"the full stop break … is still very long like very long,
        // not natural at all. for edge tts and kokoro, not the lessac"*. For this
        // voice the pause was **the endpoint's own trailing silence, played in
        // full**: the clip was written to disk exactly as it arrived and handed to
        // `MediaPlayer`, which plays a file to its end — so no amount of care in
        // the reader could shorten it, and the reader's own half-grace was added on
        // top of it. The tail is now cut the way a downloaded pack's is, to the
        // same breath (`aloudBreakMs()` — one setting, both voices).
        //
        // **THIS HAPPENS HERE, ON THE FETCH, AND NOT AT PLAYBACK TIME.** `fetch`
        // already runs off the main thread and already exists to pay for the
        // sentence the reader has not reached yet, so the decode is hidden under
        // the previous sentence's audio — exactly like the synthesis it is standing
        // next to.
        //
        // ⚠️ **A FAILURE HERE IS SILENT AND LEAVES TODAY'S BEHAVIOUR STANDING.**
        // This is a decode of a format we did not write, on a device we cannot
        // test: if anything at all goes wrong the raw MP3 is returned and the
        // reading sounds precisely as it did before v471. No fallback needed, and
        // nothing to break.
        val wav = File(clip.parentFile, clip.nameWithoutExtension + ".wav")
        // Never let a PREVIOUS sentence's trimmed audio outlive the MP3 it came
        // from: the slot names are reused, so a stale WAV would be played in the
        // place of the sentence the member is waiting for.
        wav.delete()
        val trimmed = runCatching { trimToWav(clip, wav) }.getOrNull()
        return trimmed ?: clip
    }

    // ── v471 — CUTTING A CLIP'S OWN SILENCE OFF ──────────────────────────────

    /**
     * Trims [source]'s trailing silence to the member's full stop break and writes the
     * result as a mono 16-bit WAV at [target]. Returns null when anything goes wrong.
     *
     * Runs on a worker thread ([fetch]'s), never the main one: this decodes an MP3 of
     * a few seconds with `MediaCodec`.
     */
    private fun trimToWav(source: File, target: File): File? {
        val pcm = decodePcm(source) ?: return null
        if (pcm.samples.isEmpty() || pcm.sampleRate <= 0) return null
        val end = audibleEnd(pcm.samples, pcm.sampleRate)
        if (end <= 0) return null
        writeWav(target, pcm.samples, end, pcm.sampleRate)
        return target.takeIf { it.isFile && it.length() > MIN_CLIP_BYTES }
    }

    /** Decoded audio: mono samples in −1..1, plus the rate the device played it at. */
    private class Pcm(val samples: FloatArray, val sampleRate: Int)

    /**
     * Decodes [file] to monophonic floats with `MediaExtractor` + `MediaCodec` — the
     * same pair (and the same little-endian trap) `WaveformExtractor` documents: a fresh
     * `ByteBuffer` is BIG_ENDIAN, while a decoder emits little-endian PCM, so reading
     * the shorts without fixing the order byte-swaps every sample.
     */
    private fun decodePcm(file: File): Pcm? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
        } catch (_: Exception) {
            extractor.release()
            return null
        }
        val track = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run { extractor.release(); return null }
        val format = extractor.getTrackFormat(track)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: run { extractor.release(); return null }
        var sampleRate = runCatching { format.getInteger(MediaFormat.KEY_SAMPLE_RATE) }
            .getOrDefault(24_000).coerceAtLeast(8_000)
        var channels = runCatching { format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }
            .getOrDefault(1).coerceIn(1, 2)
        extractor.selectTrack(track)
        val codec = try {
            MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
        } catch (_: Exception) {
            extractor.release()
            return null
        }

        val raw = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var done = false
        try {
            while (!done) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val input = codec.getInputBuffer(inIndex)
                    if (input == null) break
                    val size = extractor.readSampleData(input, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
                when (val outIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // The decoder's own view is the authority once it speaks: an
                        // MP3 header can disagree with what comes out of it.
                        val out = codec.outputFormat
                        sampleRate = runCatching { out.getInteger(MediaFormat.KEY_SAMPLE_RATE) }
                            .getOrDefault(sampleRate).coerceAtLeast(8_000)
                        channels = runCatching { out.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }
                            .getOrDefault(channels).coerceIn(1, 2)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> { /* keep waiting */ }
                    else -> if (outIndex >= 0) {
                        val output = codec.getOutputBuffer(outIndex)
                        if (output != null && info.size > 0) {
                            val chunk = ByteArray(info.size)
                            val dup = output.duplicate()
                            dup.position(info.offset)
                            dup.limit(info.offset + info.size)
                            dup.get(chunk)
                            raw.write(chunk)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
                    }
                }
            }
        } catch (_: Exception) {
            // Falls through to the null below — a decode that cannot finish leaves
            // the reading exactly as it was (see [fetch]).
        }
        runCatching { codec.stop() }
        codec.release()
        extractor.release()

        val bytes = raw.toByteArray()
        val frames = bytes.size / 2 / channels
        if (frames <= 0) return null
        val mono = FloatArray(frames)
        var at = 0
        for (frame in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) {
                val lo = bytes[at].toInt() and 0xFF
                val hi = bytes[at + 1].toInt()
                sum += ((hi shl 8) or lo).toShort() / 32768f
                at += 2
            }
            mono[frame] = sum / channels
        }
        return Pcm(mono, sampleRate)
    }

    /**
     * Where the audio should end: the last audible sample plus the member's full stop
     * break, in samples. Zero means the clip says nothing at all.
     *
     * **THE FLOOR IS RELATIVE TO THE CLIP ITSELF** — the same fix a downloaded pack
     * needed (see `NeuralSpeaker.trimmed`), and for the same reason: an absolute
     * threshold cannot hear a quiet tail, and a tail is quiet compared with the speech
     * in front of it rather than with full scale.
     */
    private fun audibleEnd(samples: FloatArray, sampleRate: Int): Int {
        var peak = 0f
        for (s in samples) {
            val level = abs(s)
            if (level > peak) peak = level
        }
        if (peak <= TAIL_SILENCE_LEVEL) return 0
        val floor = maxOf(TAIL_SILENCE_LEVEL, peak * TAIL_LEVEL_SHARE)
        var last = samples.size - 1
        while (last >= 0 && abs(samples[last]) < floor) last--
        if (last < 0) return 0
        val breath = (aloudBreakMs() * (sampleRate / 1000f)).toInt()
        return minOf(samples.size, last + 1 + breath)
    }

    /** Writes [samples]·[0, end) as a mono 16-bit PCM WAV — the format `MediaPlayer` plays. */
    private fun writeWav(file: File, samples: FloatArray, end: Int, sampleRate: Int) {
        val dataBytes = end * 2
        FileOutputStream(file).use { out ->
            out.write(wavHeader(dataBytes, sampleRate))
            val block = ByteArray(8192)
            var filled = 0
            for (i in 0 until end) {
                val value = (samples[i] * 32767f).toInt().coerceIn(-32768, 32767)
                block[filled++] = (value and 0xFF).toByte()
                block[filled++] = ((value shr 8) and 0xFF).toByte()
                if (filled == block.size) {
                    out.write(block, 0, filled)
                    filled = 0
                }
            }
            if (filled > 0) out.write(block, 0, filled)
        }
    }

    /** The 44-byte canonical WAV header for mono 16-bit PCM at [sampleRate]. */
    private fun wavHeader(dataBytes: Int, sampleRate: Int): ByteArray {
        val header = ByteArray(44)
        fun putAscii(at: Int, text: String) {
            text.forEachIndexed { i, c -> header[at + i] = c.code.toByte() }
        }
        fun putInt(at: Int, value: Int) {
            header[at] = (value and 0xFF).toByte()
            header[at + 1] = ((value shr 8) and 0xFF).toByte()
            header[at + 2] = ((value shr 16) and 0xFF).toByte()
            header[at + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShort(at: Int, value: Int) {
            header[at] = (value and 0xFF).toByte()
            header[at + 1] = ((value shr 8) and 0xFF).toByte()
        }
        putAscii(0, "RIFF")
        putInt(4, 36 + dataBytes)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putInt(16, 16)
        putShort(20, 1)                       // PCM
        putShort(22, 1)                       // mono
        putInt(24, sampleRate)
        putInt(28, sampleRate * 2)            // byte rate
        putShort(32, 2)                       // block align
        putShort(34, 16)                      // bits per sample
        putAscii(36, "data")
        putInt(40, dataBytes)
        return header
    }

    /** One turn's audio, and — when the handshake was refused — the server's clock. */
    private class Turn(val audio: ByteArray?, val refusedAt: Long?)

    /**
     * One handshake and one whole turn. Runs on a background thread; never throws.
     */
    private fun fetchOnce(text: String, speed: Float, voice: String): Turn {
        val audio = java.io.ByteArrayOutputStream()
        val finished = java.util.concurrent.CountDownLatch(1)
        var ok = false
        var refusedAt: Long? = null
        // A random per-connection id, exactly as the reference client sends one.
        val muid = UUID.randomUUID().toString().replace("-", "").uppercase()

        val client = OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            // A long read window: a sentence's audio arrives in chunks and the
            // gap between two of them is not a failure.
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(configMessage())
                webSocket.send(ssmlMessage(text, voice, speed))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // The ONLY completion signal that can be relied on. The audio
                // arrives as binary frames; the text frames are bookkeeping —
                // `Path:turn.start`, `Path:response`, `Path:audio.metadata` — and
                // none of them means "done". Waiting for `turn.end` is what stops
                // a clip being played half-finished.
                if (text.contains("Path:turn.end")) {
                    ok = audio.size() > 0
                    finished.countDown()
                    runCatching { webSocket.close(1000, null) }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // ⚠️ THE BINARY FRAME LAYOUT: a 2-byte BIG-ENDIAN header length,
                // then that many bytes of ASCII header, then the audio payload.
                // Reading the audio from offset 0 instead is the classic way to
                // get a file that is mostly HTTP-ish text and plays as noise.
                val raw = bytes.toByteArray()
                if (raw.size < 2) return
                val headerLength = ((raw[0].toInt() and 0xFF) shl 8) or (raw[1].toInt() and 0xFF)
                val start = 2 + headerLength
                if (start >= raw.size) return
                audio.write(raw, start, raw.size - start)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // ⚠️ A 403 IS THE HANDSHAKE'S OWN VERDICT, and its `Date` header is
                // how the caller learns WHICH verdict: a signed time it disagrees
                // with (see [clockSkewMs]) or a client it will not talk to. Only
                // the first is worth a second attempt, so only the first is kept.
                refusedAt = if (response?.code == 403) httpDate(response.header("Date")) else null
                finished.countDown()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                finished.countDown()
            }
        }

        // ── ⚠️ THE HEADERS THE UPGRADE IS JUDGED BY (v465j) ───────────────
        // Without these the endpoint answers the upgrade with a 403 and the voice
        // never gets as far as a message — which is precisely how "it was just
        // going fast with no sound" looked from the page.
        //
        // Two deliberate omissions: `Accept-Encoding` (the reference client offers
        // br/zstd, and OkHttp cannot decode either, so advertising them would be a
        // lie that breaks the frames rather than the handshake) and
        // `Sec-WebSocket-Version` (OkHttp's own WebSocket layer sets it, and a
        // second copy in the header list is not an upgrade request OkHttp will
        // send). Per-message deflate is negotiated by OkHttp itself.
        val request = Request.Builder()
            .url(url())
            .header("User-Agent", EDGE_USER_AGENT)
            .header("Origin", EXTENSION_ORIGIN)
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cookie", "muid=$muid;")
            .build()
        socket = client.newWebSocket(request, listener)
        // The whole turn, bounded: a socket that opens and then says nothing must
        // not hold a reading session hostage.
        finished.await(30, java.util.concurrent.TimeUnit.SECONDS)
        socket = null
        client.dispatcher.executorService.shutdown()
        return Turn(if (ok) audio.toByteArray() else null, refusedAt)
    }

    /**
     * An RFC 1123 `Date` header — the one shape the endpoint sends one in — as
     * epoch milliseconds, or null when it is missing or unparseable.
     *
     * Deliberately tolerant: a header this cannot read must leave the retry
     * unmade rather than throw inside a socket callback.
     */
    private fun httpDate(raw: String?): Long? = runCatching {
        if (raw.isNullOrBlank()) return@runCatching null
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("GMT") }
            .parse(raw)
            ?.time
    }.getOrNull()

    /**
     * The endpoint URL, with the two tokens the handshake is checked against.
     *
     * The four parameters and their order are the reference client's own
     * (`{WSS_URL}&ConnectionId=…&Sec-MS-GEC=…&Sec-MS-GEC-Version=…`) — the `_=`
     * cache-buster an older client carried is gone, because a parameter nothing
     * reads is one more thing to be wrong about.
     */
    private fun url(): String =
        "$ENDPOINT?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
            "&ConnectionId=${UUID.randomUUID().toString().replace("-", "")}" +
            "&Sec-MS-GEC=${gec()}&Sec-MS-GEC-Version=$GEC_VERSION"

    /**
     * The `Sec-MS-GEC` signature.
     *
     * **THE ALGORITHM, because guessing at it produces 403s that look like a
     * network fault:** take the current time in Windows FILETIME ticks (100 ns
     * units since 1601 — hence the 11,644,473,600-second epoch shift, times
     * 10,000,000), **floor it to a five-minute boundary** (3,000,000,000 ticks),
     * append the trusted client token as literal text (NOT hashed together —
     * string concatenation, then one SHA-256 over the whole thing), and send the
     * digest as UPPERCASE hex.
     *
     * The five-minute floor is the part that matters: the server validates the
     * signature against ITS current window, and a value computed from an
     * unrounded clock still works only while the two agree — rounding to the
     * window the server itself uses is what keeps a slightly-off device clock
     * from being refused.
     */
    private fun gec(): String {
        // The device's own clock, corrected by whatever a refused upgrade taught
        // us about it (see [clockSkewMs]) — the signature has to sit inside the
        // endpoint's window, not merely inside this phone's idea of the time.
        val secondsSince1601 =
            (System.currentTimeMillis() + clockSkewMs) / 1000L + 11_644_473_600L
        val ticks = secondsSince1601 * 10_000_000L
        val windowed = ticks - (ticks % 3_000_000_000L)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$windowed$TRUSTED_CLIENT_TOKEN".toByteArray(Charsets.US_ASCII))
        return digest.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }

    /** The `speech.config` frame: the format the audio is asked for in. */
    private fun configMessage(): String =
        "X-Timestamp:${stamp()}\r\n" +
            "Content-Type:application/json; charset=utf-8\r\n" +
            "Path:speech.config\r\n\r\n" +
            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":" +
            "{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"}," +
            "\"outputFormat\":\"$OUTPUT_FORMAT\"}}}}"

    /** The `ssml` frame: the voice, the speed, and the words. */
    private fun ssmlMessage(text: String, voice: String, speed: Float): String {
        val requestId = UUID.randomUUID().toString().replace("-", "")
        // `prosody rate` is a PERCENTAGE relative to normal speech, which is a
        // different scale from the Android engine's multiplier — hence the
        // conversion, and the clamp that keeps a nonsense rate off the wire.
        val percent = ((speed.coerceIn(0.5f, 2.5f) - 1f) * 100f).toInt()
        val rate = if (percent >= 0) "+$percent%" else "$percent%"
        val body = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' " +
            "xml:lang='en-US'><voice name='$voice'>" +
            "<prosody rate='$rate' pitch='+0Hz'>${escapeXml(text)}</prosody>" +
            "</voice></speak>"
        return "X-RequestId:$requestId\r\n" +
            "Content-Type:application/ssml+xml\r\n" +
            "X-Timestamp:${stamp()}\r\n" +
            "Path:ssml\r\n\r\n" +
            body
    }

    /**
     * SSML is XML, and a book title is not.
     *
     * The sentence comes from a file the member owns, so an `&` or a `<` in it
     * would end the document early and the endpoint would answer with a parse
     * error — a silent voice for a sentence that looks perfectly ordinary. The
     * five entities below are the whole of what XML requires.
     */
    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun stamp(): String =
        SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

    // ── PLAYBACK ──────────────────────────────────────────────────────────

    /**
     * Plays the finished clip. Runs on the MAIN thread, because `MediaPlayer`
     * delivers its callbacks through the Looper of the thread that created it —
     * created on a worker thread, its completion listener would never fire and
     * the reader's driver would stall on that sentence for ever.
     */
    private fun play(clip: File, mine: Int, onDone: () -> Unit, onFail: () -> Unit) {
        // Superseded between the fetch and the play: same rule as above — nothing
        // to report, because nobody is waiting.
        if (utterance != mine) return
        val built = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(clip.absolutePath)
                // ⚠️ EVERY LISTENER FIRST, `prepareAsync()` LAST. Setting a
                // listener after asking for preparation is a race the reader pays
                // for: if preparation lands before the listener is attached, the
                // callback never fires and the driver waits on that sentence for
                // ever. Reporting is the reason each one checks `utterance` — a
                // stopped or superseded clip must not advance the reading.
                setOnPreparedListener {
                    if (utterance == mine) runCatching { it.start() } else runCatching { it.release() }
                }
                setOnCompletionListener {
                    if (utterance == mine) onDone()
                }
                setOnErrorListener { _, _, _ ->
                    // A clip that failed to PLAY is a failure like any other: the
                    // reader falls back to the phone's voice for this sentence
                    // rather than counting a silent one as read (v465i).
                    if (utterance == mine) onFail()
                    true
                }
                prepareAsync()
            }
        }.getOrNull()
        if (built == null) { onDone(); return }
        player = built
    }
}

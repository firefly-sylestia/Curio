package com.curio.app.features.personal

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
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
    private const val GEC_VERSION = "1-131.0.2903.86"

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

    /** Bumped per utterance so a late completion cannot resume a newer one. */
    @Volatile private var utterance = 0

    /** Where the clip is written; one file, overwritten per sentence. */
    private fun clipFile(context: Context): File = File(context.cacheDir, "edge-voice.mp3")

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
            val clip = runCatching { fetch(app, text, speed, voice) }.getOrNull()
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

    /** Stops whatever is being said. Safe to call when nothing is. */
    fun stop() {
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
    private fun fetch(context: Context, text: String, speed: Float, voice: String): File? {
        val clip = clipFile(context)
        if (clip.exists()) clip.delete()
        val audio = java.io.ByteArrayOutputStream()
        val finished = java.util.concurrent.CountDownLatch(1)
        var ok = false

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
                finished.countDown()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                finished.countDown()
            }
        }

        val request = Request.Builder().url(url()).build()
        socket = client.newWebSocket(request, listener)
        // The whole turn, bounded: a socket that opens and then says nothing must
        // not hold a reading session hostage.
        finished.await(30, java.util.concurrent.TimeUnit.SECONDS)
        socket = null
        client.dispatcher.executorService.shutdown()
        if (!ok) return null
        clip.writeBytes(audio.toByteArray())
        return clip.takeIf { it.length() > 512L }
    }

    /** The endpoint URL, with the two tokens the handshake is checked against. */
    private fun url(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
        return "$ENDPOINT?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
            "&Sec-MS-GEC=${gec()}&Sec-MS-GEC-Version=$GEC_VERSION" +
            "&ConnectionId=${UUID.randomUUID().toString().replace("-", "")}" +
            "&_=$stamp"
    }

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
        val secondsSince1601 = System.currentTimeMillis() / 1000L + 11_644_473_600L
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

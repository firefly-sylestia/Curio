package com.curio.app.data

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * ── v465c — THE NEURAL READ-ALOUD VOICE PACKS (FULL EDITION ONLY) ──────────
 *
 * The member's §58 request: *"customisation play pause skip and a custom voice
 * download option except system voice, research whats a better natural reading
 * voice download available"*, and, at the choice, **Piper medium as the default
 * pack and Kokoro as the optional "best quality" one**.
 *
 * **Why these two, from the research rather than from taste.** A book reader has
 * one non-negotiable: sustained narration must OUTRUN playback. sherpa-onnx's own
 * RTF benchmark (Raspberry Pi 4, 4 threads — a fair proxy for a mid-range phone)
 * puts Piper medium at **0.357** and Kokoro-82M at **2.77–3.19**. RTF above 1
 * means synthesis is slower than speech, which on a phone is stutter, heat and a
 * flat battery — so Piper is the pack that is offered first and Kokoro is offered
 * second, with its size and its speed both stated in the row rather than buried.
 *
 * **Why the runtime is vendored rather than resolved.** sherpa-onnx publishes an
 * official Android AAR (**48 MB**, all four ABIs) from its own GitHub release and
 * has **no Maven Central coordinate** — the Maven hits are third-party
 * repackages — so the binary is committed at `app/libs/sherpa-onnx-1.13.8.aar`
 * and wired as `fullImplementation` in app/build.gradle.kts. It is Apache-2.0 and
 * commercially clean, and so are both packs' models; Piper's *training* codebase
 * moved to GPL-3.0 but is not embedded, and sherpa-onnx's Piper runtime is its
 * own Apache-2.0 code, so no GPL reaches Curio.
 *
 * **This file is the SEAM's download half, and the core edition has a twin.** The
 * core edition ships `app/src/core/.../NeuralVoices.kt` with an identical API and
 * an empty [NeuralVoicePacks.CATALOG] — the same shape `OfflineTranscriber` uses
 * next door. Nothing under `main` may import a sherpa or commons-compress type.
 *
 * **Android cannot decompress BZIP2.** Every pack is published as `.tar.bz2` and
 * neither `java.util.zip` nor anything in the framework reads bzip2, which is the
 * single reason `commons-compress` is a dependency at all: it supplies
 * `BZip2CompressorInputStream` and `TarArchiveInputStream`.
 */
object NeuralVoicePacks {

    /**
     * What a pack is FOR, which is the only thing that decides its config: Piper
     * is a VITS model and Kokoro reads its speaker list out of `voices.bin`.
     */
    enum class Kind { PIPER, KOKORO }

    /** The order the rows are offered in, and what each one is honestly worth. */
    enum class Tier(val label: String) {
        /** Fast enough to narrate a whole book on a mid-range phone. */
        RECOMMENDED("Recommended"),

        /** The better voice, and slower than it speaks on older hardware. */
        BEST("Best quality"),
    }

    /**
     * One downloadable voice pack.
     *
     * [sizeBytes] is the published length of the archive, read from the release
     * rather than estimated — the row prints it, and a download row that
     * understates a 300 MB file is a row that lies to someone on mobile data.
     */
    data class Pack(
        val id: String,
        val displayName: String,
        val voiceLabel: String,
        val sizeLabel: String,
        val sizeBytes: Long,
        val url: String,
        val kind: Kind,
        val tier: Tier,
        /** The single directory the archive wraps its files in. */
        val archiveRoot: String,
        /**
         * The narrators inside the pack, in SPEAKER-ID ORDER — the index into
         * this list IS the `sid` handed to the model. Empty for a single-voice
         * pack (Piper), which is also what hides the narrator row.
         *
         * **These are copied from the model's own published map, never
         * inferred.** Kokoro's ids are NOT alphabetical-by-luck and NOT guessable
         * from a voice list: `0` is the bare `af` and the ids run
         * af ×5 → am ×2 → bf ×2 → bm ×2, so a plausible-looking slice of some
         * other ordering would put a British male's name on an American female's
         * voice. Source: sherpa-onnx's own page for this exact model
         * (kokoro-en-v0_19: 11 speakers, 24 kHz, "speaker ID to speaker name").
         */
        val speakers: List<String>,
    )

    /**
     * The two packs, both English, both Apache-2.0-clean, both verified against
     * the release's own asset list (sizes) and the model pages (layout).
     */
    val CATALOG: List<Pack> = listOf(
        Pack(
            id = "piper-lessac-medium",
            displayName = "Piper \u00b7 Lessac",
            voiceLabel = "One narrator, American English \u00b7 reads a whole book without strain",
            sizeLabel = "64 MB",
            sizeBytes = 67_230_653L,
            url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/" +
                "vits-piper-en_US-lessac-medium.tar.bz2",
            kind = Kind.PIPER,
            tier = Tier.RECOMMENDED,
            archiveRoot = "vits-piper-en_US-lessac-medium",
            // One voice, so no narrator row: Piper's model has a single speaker
            // and `numSpeakers()` answers 1 for it.
            speakers = emptyList(),
        ),
        Pack(
            id = "kokoro-en",
            displayName = "Kokoro \u00b7 82M",
            voiceLabel = "Eleven speakers \u00b7 the more natural voice, and slower to make",
            sizeLabel = "305 MB",
            sizeBytes = 319_625_534L,
            url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/" +
                "kokoro-en-v0_19.tar.bz2",
            kind = Kind.KOKORO,
            tier = Tier.BEST,
            archiveRoot = "kokoro-en-v0_19",
            // sid order, verbatim from the model's published map — do not sort,
            // do not "tidy" the bare `af` into something prettier, and do not
            // reorder: the index is the id the model is asked for.
            speakers = listOf(
                "af \u00b7 American female",
                "af_bella \u00b7 American female",
                "af_nicole \u00b7 American female",
                "af_sarah \u00b7 American female",
                "af_sky \u00b7 American female",
                "am_adam \u00b7 American male",
                "am_michael \u00b7 American male",
                "bf_emma \u00b7 British female",
                "bf_isabella \u00b7 British female",
                "bm_george \u00b7 British male",
                "bm_lewis \u00b7 British male",
            ),
        ),
    )

    fun byId(id: String?): Pack? = CATALOG.firstOrNull { it.id == id }

    /** Where every pack lives: `filesDir/voice-packs/<id>/`. */
    fun packsDir(context: Context): File = File(context.filesDir, "voice-packs")

    /** The directory a downloaded pack's files are extracted into. */
    fun packDir(context: Context, id: String): File = File(packsDir(context), id)

    /**
     * The directory the MODEL actually sits in once extracted.
     *
     * The archive wraps everything in [Pack.archiveRoot], so this is normally
     * `packDir/<archiveRoot>` — but the root is DISCOVERED rather than assumed,
     * because a single stray top-level directory in a future model would
     * otherwise leave the pack downloaded and unusable.
     */
    fun modelDir(context: Context, pack: Pack): File {
        val root = packDir(context, pack.id)
        val wrapped = File(root, pack.archiveRoot)
        if (wrapped.isDirectory) return wrapped
        val only = root.listFiles()?.filter { it.isDirectory }
        return if (only?.size == 1) only.first() else root
    }

    /**
     * IS THE PACK USABLE, not merely downloaded.
     *
     * A half-extracted pack is the failure this exists to catch: the marker file
     * is written **last**, so a download that was killed mid-extraction reads as
     * not-there and the row offers Download again instead of a voice that
     * silently says nothing.
     */
    fun isDownloaded(context: Context, id: String?): Boolean {
        val pack = byId(id) ?: return false
        return File(packDir(context, pack.id), DONE_MARKER).isFile &&
            findModel(context, pack) != null
    }

    /** What the pack occupies on the phone, for the row's own honesty. */
    fun sizeOnDisk(context: Context, id: String?): Long {
        val pack = byId(id) ?: return 0L
        val dir = packDir(context, pack.id)
        if (!dir.isDirectory) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** Removes a pack whole. Safe to call on one that was never downloaded. */
    fun delete(context: Context, id: String) {
        val dir = packDir(context, id)
        if (dir.isDirectory) dir.deleteRecursively()
    }

    /** Drops the directory of any pack that is no longer in [CATALOG]. */
    fun pruneRemoved(context: Context) {
        val known = CATALOG.map { it.id }.toSet()
        packsDir(context).listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name !in known) dir.deleteRecursively()
        }
    }

    /**
     * THE MODEL FILE, FOUND RATHER THAN NAMED.
     *
     * The two packs do not agree on what their model is called — Piper's is
     * `en_US-lessac-medium.onnx` and Kokoro's is `model.onnx` — and a third pack
     * would bring a third name. So the model is the **largest `.onnx` in the
     * directory**, which is true of every TTS pack here by construction: the
     * acoustic model is the biggest thing in it, and nothing else in these
     * archives ends in `.onnx`.
     */
    fun findModel(context: Context, pack: Pack): File? =
        modelDir(context, pack)
            .listFiles()
            ?.filter { it.isFile && it.name.endsWith(".onnx") }
            ?.maxByOrNull { it.length() }

    /**
     * The lexicon files, comma-joined, or `""`.
     *
     * Kokoro carries three (`lexicon-us-en.txt`, `lexicon-gb-en.txt`,
     * `lexicon-zh.txt`) and hands them to sherpa as one comma-separated string;
     * Piper carries none, and an absent lexicon is not an error. Globbed, so the
     * next pack's differently-named lexicons are picked up for free.
     */
    fun lexicons(context: Context, pack: Pack): String =
        modelDir(context, pack)
            .listFiles()
            ?.filter { it.isFile && it.name.startsWith("lexicon") && it.name.endsWith(".txt") }
            ?.sortedBy { it.name }
            ?.joinToString(",") { it.absolutePath }
            .orEmpty()

    /** The `espeak-ng-data` directory these models phonemize with. */
    fun espeakDataDir(context: Context, pack: Pack): File = File(modelDir(context, pack), "espeak-ng-data")

    /** `voices.bin` — Kokoro's speaker table. */
    fun voicesFile(context: Context, pack: Pack): File = File(modelDir(context, pack), "voices.bin")

    /** `tokens.txt`, which every pack carries under the same name. */
    fun tokensFile(context: Context, pack: Pack): File = File(modelDir(context, pack), "tokens.txt")

    /** A size a member can read, for a download row. */
    fun formatSize(bytes: Long): String {
        // Locale pinned: a `%.1f` that renders as "305,0 MB" in half of Europe
        // is the kind of thing a download row should not surprise anyone with.
        val locale = java.util.Locale.US
        return when {
            bytes <= 0L -> "0 MB"
            bytes >= 1_073_741_824L -> String.format(locale, "%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> String.format(locale, "%.0f MB", bytes / 1_048_576.0)
            else -> String.format(locale, "%.0f KB", bytes / 1024.0)
        }
    }

    /**
     * Written into a pack's directory AFTER its files are all in place, so its
     * presence is the proof that extraction finished (see [isDownloaded]).
     */
    internal const val DONE_MARKER = ".curio-pack-complete"

    /** The archive downloaded to this name while it is on its way in. */
    internal const val PARTIAL_SUFFIX = ".part"
}

/**
 * The download and extraction of a pack, as observable state.
 *
 * Deliberately NOT resumable and deliberately cancellable: a paused HTTP range
 * request against a GitHub release redirect is a second failure mode for no
 * gain, and a member who stops a 305 MB download wants it stopped.
 */
object NeuralVoiceDownloads {

    enum class Status { Idle, Downloading, Extracting, Failed }

    data class State(
        val status: Status = Status.Idle,
        /** 0f..1f while [Status.Downloading]; 1f once extracting. */
        val progress: Float = 0f,
        val error: String? = null,
        /**
         * v465i — HOW MUCH HAS ARRIVED, AND HOW MUCH THERE IS.
         *
         * The row used to have one number to show, a percentage, and a
         * percentage cannot answer the two questions a member actually asks
         * about a 305 MB pack: how much of my data has gone, and when will this
         * finish. Bytes answer both — the rate below turns the remainder into a
         * time (member: *"its not accurate in reader settings how would i know
         * when its gonna finish"*).
         */
        val bytesRead: Long = 0L,
        val totalBytes: Long = 0L,
        /** A smoothed arrival rate in bytes/second, 0 until two samples land. */
        val bytesPerSecond: Long = 0L,
    )

    private val _states = MutableStateFlow<Map<String, State>>(emptyMap())
    val states: StateFlow<Map<String, State>> = _states

    /** The cancellable handle per pack, so a Stop is immediate. */
    private val calls = HashMap<String, okhttp3.Call>()

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // A pack is up to 305 MB, so the read timeout has to survive a slow
            // connection between two chunks rather than time out the transfer.
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun set(id: String, state: State) {
        _states.value = _states.value + (id to state)
    }

    /** Starts (or restarts) a pack's download. No-op when it is already going. */
    fun start(context: Context, pack: NeuralVoicePacks.Pack) {
        if (_states.value[pack.id]?.status == Status.Downloading) return
        set(pack.id, State(Status.Downloading, 0f))
        Thread {
            runCatching { fetchAndExtract(context.applicationContext, pack) }
                .onSuccess {
                    set(pack.id, State(Status.Idle, 1f))
                }
                .onFailure { error ->
                    set(pack.id, State(Status.Failed, 0f, error.message ?: "The download did not finish."))
                    // A failed run must not leave a half-extracted pack behind
                    // that the NEXT run would try to extract into.
                    NeuralVoicePacks.delete(context.applicationContext, pack.id)
                }
        }.start()
    }

    /** Stops this pack's download and clears what it wrote. */
    fun cancel(context: Context, id: String) {
        calls.remove(id)?.cancel()
        NeuralVoicePacks.delete(context.applicationContext, id)
        set(id, State(Status.Idle, 0f))
    }

    private fun fetchAndExtract(context: Context, pack: NeuralVoicePacks.Pack) {
        val dir = NeuralVoicePacks.packDir(context, pack.id)
        if (dir.exists()) dir.deleteRecursively()
        if (!dir.mkdirs()) error("Could not create the voice pack folder.")
        val archive = File(
            context.cacheDir,
            "${pack.id}${NeuralVoicePacks.PARTIAL_SUFFIX}",
        )
        try {
            val call = http.newCall(Request.Builder().url(pack.url).build())
            calls[pack.id] = call
            call.execute().use { response ->
                if (!response.isSuccessful) error("The download server answered ${response.code}.")
                val body = response.body
                val total = body.contentLength().takeIf { it > 0 } ?: pack.sizeBytes
                var read = 0L
                // v465i — THE MEASUREMENT, AND THE THROTTLE.
                //
                // Every 64 KB chunk used to publish a new [State], which is a new
                // immutable Map for a settings page to diff — thousands of them on
                // a 64 MB pack, for a bar nobody can read that fast. The numbers
                // below are what the row needs instead: the WINDOW (how many bytes
                // arrived, and how long they took) feeds a smoothed rate, and a
                // report is published at most twice a second.
                var windowAt = SystemClock.elapsedRealtime()
                var windowBytes = 0L
                var rate = 0L
                var reportedAt = windowAt
                body.byteStream().use { input ->
                    FileOutputStream(archive).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                            read += n
                            val now = SystemClock.elapsedRealtime()
                            windowBytes += n.toLong()
                            val span = now - windowAt
                            if (span >= 400L) {
                                val instant = windowBytes * 1000L / span
                                // An exponential average, because a raw per-chunk
                                // rate jumps enough to make a time-left estimate
                                // unreadable: it is the trend that answers "when".
                                rate = if (rate <= 0L) instant else (rate * 3L + instant) / 4L
                                windowAt = now
                                windowBytes = 0L
                            }
                            if (now - reportedAt >= 500L || read >= total) {
                                reportedAt = now
                                set(
                                    pack.id,
                                    State(
                                        Status.Downloading,
                                        (read.toDouble() / total.toDouble()).toFloat()
                                            .coerceIn(0f, 0.99f),
                                        null,
                                        read,
                                        total,
                                        rate,
                                    ),
                                )
                            }
                        }
                        output.flush()
                    }
                }
            }
            calls.remove(pack.id)
            // v465i — the network half is DONE, and the row says so: everything
            // from here is unpacking, which needs no data and no connection.
            set(pack.id, State(Status.Extracting, 1f, null, read, total))
            extract(archive, dir)
            File(dir, NeuralVoicePacks.DONE_MARKER).writeText(pack.id)
        } finally {
            calls.remove(pack.id)
            archive.delete()
        }
    }

    /**
     * Unpacks `.tar.bz2` into [dir].
     *
     * Runs on its own thread (called from [fetchAndExtract]) and never from the
     * main one: a 305 MB Kokoro archive is ~330 MB of bzip2 work before anything
     * can be said, and decompressing that on the UI thread is an ANR.
     */
    private fun extract(archive: File, dir: File) {
        BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive))).use { bz ->
            TarArchiveInputStream(bz).use { tar ->
                val root = dir.canonicalFile
                while (true) {
                    val entry = tar.nextEntry ?: break
                    // ⚠️ ZIP-SLIP. An archive entry may name `../../` and write
                    // anywhere the app can reach; nothing here trusts the names
                    // in the file, only their resolved location inside [root].
                    val target = File(dir, entry.name).canonicalFile
                    if (!target.path.startsWith(root.path + File.separator)) continue
                    if (entry.isDirectory) {
                        target.mkdirs()
                        continue
                    }
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out -> tar.copyTo(out, 64 * 1024) }
                }
            }
        }
    }
}

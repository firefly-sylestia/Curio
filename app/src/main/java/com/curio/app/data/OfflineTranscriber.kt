package com.curio.app.data

import android.content.Context
import android.media.MediaCodec
import android.os.StatFs
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

/**
 * Offline voice-to-text for PRE-RECORDED sound bites (v125) — Vosk on-device
 * ASR, no network at transcription time.
 *
 * The engine is Vosk (not whisper.cpp — see AGENTS.md v125): whisper.cpp has
 * no published Android binding, so Vosk's published AAR (`vosk-android`,
 * bundles .so for every ABI) delivers the same offline model + transcribe
 * flow with a fraction of the integration risk.
 *
 * Three moving parts:
 *  - [VoskModels] — the downloadable model catalog (id / name / size / URL),
 *    in-app download + unzip into `filesDir/vosk-models/<id>/`, delete.
 *  - [OfflineTranscriber.transcribe] — decodes the recorded m4a (AAC) to
 *    16 kHz mono PCM via MediaExtractor + MediaCodec (resampling + downmix
 *    handled here), then feeds Vosk chunk-by-chunk on a background thread.
 *
 * Model download uses the same plain [HttpURLConnection] pattern as
 * [UpdateChecker.downloadApk] — no new HTTP dependency.
 */
object VoskModels {

    /** One downloadable offline model. */
    data class Info(
        val id: String,
        val displayName: String,
        val langLabel: String,
        val sizeLabel: String,
        val sizeBytes: Long,
        val url: String
    )

    /** The catalog offered in Settings → Recording → Offline model. */
    val CATALOG: List<Info> = listOf(
        Info(
            id = "vosk-model-small-en-us-0.15",
            displayName = "Small · English (US)",
            langLabel = "English — best accuracy on clear US speech",
            sizeLabel = "~40 MB",
            sizeBytes = 40_600_000L,
            url = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        ),
        Info(
            id = "vosk-model-small-0.22",
            displayName = "Small · Multilingual",
            langLabel = "Many languages, lighter accuracy",
            sizeLabel = "~45 MB",
            sizeBytes = 45_800_000L,
            url = "https://alphacephei.com/vosk/models/vosk-model-small-0.22.zip"
        ),
        Info(
            id = "vosk-model-small-en-in-0.4",
            displayName = "Small · English (India)",
            langLabel = "English tuned for Indian accents",
            sizeLabel = "~40 MB",
            sizeBytes = 40_400_000L,
            url = "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
        ),
        // v131 — the bigger tiers: Large (~128 MB, phone-friendly, notably
        // more accurate than the smalls) and the Full server-grade models
        // (~1-2.3 GB — most accurate, but heavy downloads that need real
        // storage + memory). Sizes from the alphacephei.com model page.
        Info(
            id = "vosk-model-en-us-0.22-lgraph",
            displayName = "Large · English (US)",
            langLabel = "Higher accuracy with a dynamic graph",
            sizeLabel = "~128 MB",
            sizeBytes = 128_000_000L,
            url = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-lgraph.zip"
        ),
        Info(
            id = "vosk-model-en-us-0.22",
            displayName = "Full · English (US)",
            langLabel = "Most accurate US English — large download, needs real storage & memory",
            sizeLabel = "~1.8 GB",
            sizeBytes = 1_800_000_000L,
            url = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22.zip"
        ),
        Info(
            id = "vosk-model-en-us-0.42-gigaspeech",
            displayName = "Full · English (US) — Gigaspeech",
            langLabel = "Newest accurate model — best on podcasts & clear speech",
            sizeLabel = "~2.3 GB",
            sizeBytes = 2_300_000_000L,
            url = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.42-gigaspeech.zip"
        ),
        Info(
            id = "vosk-model-en-in-0.5",
            displayName = "Full · English (India)",
            langLabel = "Higher accuracy for Indian accents — large download",
            sizeLabel = "~1 GB",
            sizeBytes = 1_000_000_000L,
            url = "https://alphacephei.com/vosk/models/vosk-model-en-in-0.5.zip"
        )
    )

    fun byId(id: String?): Info? = CATALOG.firstOrNull { it.id == id }

    /** Root dir holding every downloaded model. */
    fun modelsDir(context: Context): File =
        File(context.filesDir, "vosk-models").apply { mkdirs() }

    /**
     * The extracted model directory, or null when [id] isn't fully present.
     * A model counts as downloaded only when its `am/` (acoustic model)
     * folder exists — a half-extracted download never passes this check.
     */
    fun modelDir(context: Context, id: String?): File? {
        if (id.isNullOrBlank()) return null
        val dir = File(modelsDir(context), id)
        return if (dir.isDirectory && File(dir, "am").isDirectory) dir else null
    }

    fun isDownloaded(context: Context, id: String?): Boolean =
        modelDir(context, id) != null

    /** Real on-disk size of a downloaded model (0 when not installed). */
    fun modelSizeBytes(context: Context, id: String?): Long {
        val dir = modelDir(context, id) ?: return 0L
        return dir.walkBottomUp().sumOf { it.length() }
    }

    /** Free bytes in the app's file storage (where models install). */
    fun availableStorageBytes(context: Context): Long =
        runCatching { StatFs(context.filesDir.absolutePath).availableBytes }.getOrDefault(0L)

    /**
     * B / KB / MB / GB — the catalog's ~size labels cap at MB, but the
     * Full tiers run 1–2.3 GB, so picker rows and confirmations need GB.
     */
    fun formatModelSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
        else -> "%.2f GB".format(bytes.toDouble() / (1024L * 1024 * 1024))
    }

    fun deleteModel(context: Context, id: String) {
        // v137 — guard: only ever delete THIS model's subdir, never the
        // shared models root (a mis-resolved path would wipe every model).
        val dir = File(modelsDir(context), id)
        if (dir.parentFile?.name == "vosk-models" && dir.name.isNotBlank()) {
            dir.deleteRecursively()
        }
        // Drop the cached (possibly partial) zip so a re-download starts
        // clean instead of resuming from a stale file.
        File(context.cacheDir, "$id.zip").delete()
        AppPreferences.bumpOfflineModelVersion()
    }
}

/**
 * v137 — app-scoped model download manager.
 *
 * Downloads used to run inside the picker dialog's `rememberCoroutineScope`,
 * so swiping the sheet away (or the dialog leaving composition) CANCELLED
 * the transfer mid-download. State now lives here on an application-lifetime
 * scope, so a download keeps running after the picker closes; rows observe
 * [states] and can pause / resume / cancel per model, and several models
 * download at once (each has its own job). Pausing keeps the partial zip in
 * cache; resuming continues with an HTTP Range request where the server
 * supports it (and restarts cleanly where it doesn't).
 */
object VoskModelDownloads {

    enum class Status { Idle, Downloading, Paused, Failed }

    data class State(
        val status: Status = Status.Idle,
        val progress: Float = 0f,
        val error: String? = null
    )

    /** Per-model download state, keyed by model id. */
    val states: StateFlow<Map<String, State>> = _states
    private val _states = MutableStateFlow<Map<String, State>>(emptyMap())

    // App-lifetime scope — surviving the picker dialog is the whole point.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val connections = ConcurrentHashMap<String, HttpURLConnection>()
    // Pause gates: while a model is Paused its download loop awaits this
    // gate; resume() completes it so the loop continues where it stopped.
    private val pauseGates = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /** Aborts a transfer cleanly on pause — the partial zip is kept. */
    private class PauseRequested : Exception()

    /** Starts (or retries) a download. No-op while one is already active. */
    fun start(context: Context, info: Info) {
        val existing = jobs[info.id]
        if (existing != null && existing.isActive) return
        val job = scope.launch {
            update(info.id) { it.copy(status = Status.Downloading, progress = 0f, error = null) }
            val ok = downloadWithPause(context, info)
            if (ok) {
                update(info.id) { State(Status.Idle) }
                AppPreferences.setOfflineModelId(context, info.id)
                AppPreferences.bumpOfflineModelVersion()
            } else {
                update(info.id) {
                    it.copy(status = Status.Failed, error = "Download failed. Check your connection and try again.")
                }
            }
        }
        job.invokeOnCompletion {
            // Only clear the entry if it still refers to THIS job — a fresh
            // start() after a cancel must not have its entry removed by the
            // dying job's cleanup.
            if (jobs[info.id] === job) jobs.remove(info.id)
            pauseGates.remove(info.id)?.complete(Unit)
        }
        jobs[info.id] = job
    }

    fun pause(id: String) {
        if (_states.value[id]?.status != Status.Downloading) return
        update(id) { it.copy(status = Status.Paused) }
    }

    fun resume(id: String) {
        if (_states.value[id]?.status != Status.Paused) return
        update(id) { it.copy(status = Status.Downloading) }
        pauseGates.remove(id)?.complete(Unit)
    }

    /** Stops a download and drops the partial zip (a deliberate cancel). */
    fun cancel(context: Context, id: String) {
        update(id) { State(Status.Idle) }
        jobs.remove(id)?.cancel()
        connections.remove(id)?.disconnect()
        pauseGates.remove(id)?.complete(Unit)
        File(context.cacheDir, "$id.zip").delete()
    }

    private fun update(id: String, transform: (State) -> State) {
        _states.update { it + (id to transform(it[id] ?: State())) }
    }

    /**
     * The pausable transfer: downloads [info]'s zip into cache (resuming
     * from a partial file via Range where supported), then extracts it into
     * `filesDir/vosk-models/<id>/` (the zip's single root folder stripped).
     * Returns true only on a complete, verified extract.
     */
    private suspend fun downloadWithPause(context: Context, info: Info): Boolean =
        withContext(Dispatchers.IO) {
            val zipFile = File(context.cacheDir, "${info.id}.zip")
            try {
                var received = zipFile.length()
                var fullSize = 0L
                var done = false
                while (!done) {
                    // ── Wait while paused ───────────────────────────────
                    while (_states.value[info.id]?.status == Status.Paused) {
                        pauseGates.computeIfAbsent(info.id) { CompletableDeferred() }.await()
                    }
                    // ── (Re)open — a resumed transfer asks for the rest ─
                    val conn = URL(info.url).openConnection() as HttpURLConnection
                    try {
                        conn.connectTimeout = 15_000
                        conn.readTimeout = 20_000
                        conn.instanceFollowRedirects = true
                        if (received > 0) conn.setRequestProperty("Range", "bytes=$received-")
                        when (conn.responseCode) {
                            HttpURLConnection.HTTP_OK -> {
                                if (received > 0) {
                                    // Server ignored Range — start over.
                                    zipFile.delete()
                                    received = 0
                                }
                                fullSize = conn.contentLengthLong
                            }
                            HttpURLConnection.HTTP_PARTIAL -> {
                                if (fullSize <= 0) fullSize = received + conn.contentLengthLong
                            }
                            else -> return@withContext false
                        }
                        connections[info.id] = conn
                        zipFile.outputStream(received > 0).use { out ->
                            conn.inputStream.use { input ->
                                val buf = ByteArray(64 * 1024)
                                while (true) {
                                    if (_states.value[info.id]?.status == Status.Paused) throw PauseRequested()
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    out.write(buf, 0, n)
                                    received += n
                                    if (fullSize > 0) {
                                        update(info.id) {
                                            it.copy(progress = (received.toFloat() / fullSize).coerceIn(0f, 1f))
                                        }
                                    }
                                }
                            }
                        }
                        done = true
                    } catch (_: PauseRequested) {
                        // Partial kept — the loop waits on the gate above.
                    } finally {
                        connections.remove(info.id)
                        conn.disconnect()
                    }
                }
                // ── Extract ─────────────────────────────────────────────
                val dest = File(modelsDir(context), info.id)
                dest.deleteRecursively()
                dest.mkdirs()
                var extracted = 0
                ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            // Vosk zips wrap everything in one root folder
                            // named after the model — strip it so the model
                            // sits directly under our <id>/ dir.
                            val rel = entry.name.substringAfter('/', missingDelimiterValue = "")
                            if (rel.isNotBlank()) {
                                val target = File(dest, rel)
                                target.parentFile?.mkdirs()
                                target.outputStream().use { out -> zip.copyTo(out) }
                                extracted++
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                zipFile.delete()
                if (extracted == 0 || !File(dest, "am").isDirectory) {
                    dest.deleteRecursively()
                    return@withContext false
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                zipFile.delete()
                false
            }
        }
}

/** Decoded PCM ready for the recognizer. */
private class DecodedAudio(
    val samples: ShortArray,
    val sampleRate: Int,
    val channels: Int
)

/**
 * Transcribes the recorded sound bite at [audioPath] (AAC m4a) with the
 * offline model [modelId]. Runs entirely on a background dispatcher.
 *
 * Returns the trimmed transcript, or null when the model/audio is missing
 * or nothing intelligible was recognized. [onProgress] reports 0..1 as the
 * PCM is fed through the recognizer.
 */
object OfflineTranscriber {

    /** Vosk wants 16 kHz mono PCM regardless of the recording's own rate. */
    private const val TARGET_RATE = 16_000

    /**
     * Chunk fed per acceptWaveForm call — 5 seconds of 16 kHz mono samples
     * keeps the JNI hop granular enough for progress without hammering it
     * every few milliseconds.
     */
    private const val CHUNK_SAMPLES = TARGET_RATE * 5

    suspend fun transcribe(
        context: Context,
        audioPath: String?,
        modelId: String?,
        onProgress: (Float) -> Unit = {}
    ): String? {
        if (audioPath.isNullOrBlank()) return null
        val modelDir = VoskModels.modelDir(context, modelId) ?: return null
        return withContext(Dispatchers.Default) {
            val decoded = decodeToPcm(audioPath) ?: return@withContext null
            val mono = toMono16k(decoded.samples, decoded.sampleRate, decoded.channels)
            if (mono.isEmpty()) return@withContext null
            val model = Model(modelDir.absolutePath)
            val recognizer = Recognizer(model, TARGET_RATE.toFloat())
            try {
                val sb = StringBuilder()
                var offset = 0
                // Use the short[] overload with the SAMPLE count — vosk's
                // C API takes samples, and the byte[] overload's len is
                // ambiguous (a known vosk-android footgun). This matches
                // the library's own live-mic SpeechService usage.
                while (offset < mono.size) {
                    val end = minOf(offset + CHUNK_SAMPLES, mono.size)
                    val endOfSpeech = recognizer.acceptWaveForm(mono.copyOfRange(offset, end), end - offset)
                    if (endOfSpeech) {
                        appendText(sb, recognizer.result)
                    }
                    onProgress(offset.toFloat() / mono.size)
                    offset = end
                }
                // vosk-android's Java binding names it getFinalResult()
                // (not finalResult()) — verified against the published
                // 0.3.47 sources.
                appendText(sb, recognizer.getFinalResult())
                sb.toString().trim().ifBlank { null }
            } finally {
                runCatching { recognizer.close() }
                runCatching { model.close() }
            }
        }
    }

    private fun appendText(sb: StringBuilder, resultJson: String) {
        val text = runCatching { JSONObject(resultJson).optString("text") }.getOrNull() ?: return
        if (text.isNotBlank()) sb.append(text.trim()).append(' ')
    }

    /**
     * Decodes any audio container MediaExtractor understands (our recordings
     * are AAC-in-MPEG4) into raw 16-bit little-endian PCM at the source
     * sample rate/channel count, using MediaCodec's decoder (no ffmpeg).
     */
    private fun decodeToPcm(audioPath: String): DecodedAudio? {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(audioPath)
            var trackIndex = -1
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    trackIndex = i
                    trackFormat = f
                    break
                }
            }
            if (trackIndex < 0 || trackFormat == null) return null
            extractor.selectTrack(trackIndex)
            decoder = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME)!!)
            decoder.configure(trackFormat, null, null, 0)
            decoder.start()
            val bufferInfo = MediaCodec.BufferInfo()
            val pcm = ByteArrayOutputStream()
            var sampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else TARGET_RATE
            var channels = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx) ?: continue
                        val size = extractor.readSampleData(inBuf, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outIdx >= 0 -> {
                        if (bufferInfo.size > 0) {
                            val outBuf = decoder.getOutputBuffer(outIdx) ?: continue
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val chunk = ByteArray(bufferInfo.size)
                            outBuf.get(chunk)
                            pcm.write(chunk)
                        }
                        decoder.releaseOutputBuffer(outIdx, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = decoder.outputFormat
                        if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                    outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No output yet — loop and keep feeding input.
                    }
                }
            }
            val raw = pcm.toByteArray()
            if (raw.size < 2 || raw.size % 2 != 0) return null
            val samples = ShortArray(raw.size / 2)
            ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
            return DecodedAudio(samples, sampleRate, channels)
        } catch (_: Exception) {
            return null
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor.release() }
        }
    }

    /** Downmixes to mono and resamples to 16 kHz (linear interpolation). */
    private fun toMono16k(samples: ShortArray, srcRate: Int, srcChannels: Int): ShortArray {
        val mono = if (srcChannels > 1) {
            ShortArray(samples.size / srcChannels).also { out ->
                for (i in out.indices) {
                    var acc = 0
                    for (c in 0 until srcChannels) acc += samples[i * srcChannels + c]
                    out[i] = (acc / srcChannels).toShort()
                }
            }
        } else samples
        if (srcRate == TARGET_RATE) return mono
        val ratio = srcRate.toDouble() / TARGET_RATE
        val outLen = (mono.size / ratio).toInt()
        val out = ShortArray(outLen)
        for (i in 0 until outLen) {
            val pos = i * ratio
            val i0 = pos.toInt()
            val frac = (pos - i0).toFloat()
            val s0 = mono[i0].toInt()
            // v126 — getOrElse's lambda must return Short (ShortArray), so
            // the default closes over the raw Short, not the converted Int.
            val s1 = mono.getOrElse(i0 + 1) { mono[i0] }.toInt()
            // v126 — Float.toShort() is deprecated: round through Int.
            out[i] = (s0 + ((s1 - s0) * frac)).roundToInt().toShort()
        }
        return out
    }
}

package com.curio.app.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    fun deleteModel(context: Context, id: String) {
        modelDir(context, id)?.deleteRecursively()
        AppPreferences.bumpOfflineModelVersion()
    }

    /**
     * Downloads [info]'s zip to cache and extracts it into
     * `filesDir/vosk-models/<id>/` (the zip's single root folder is
     * stripped). [onProgress] reports 0..1 across the DOWNLOAD phase only
     * (unzipping is quick relative to a 40MB transfer). Returns true only
     * on a complete, verified extract; false on any failure.
     */
    suspend fun download(context: Context, info: Info, onProgress: (Float) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            val zipFile = File(context.cacheDir, "${info.id}.zip")
            try {
                // ── Download ────────────────────────────────────────────
                val conn = URL(info.url).openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 15_000
                    conn.readTimeout = 20_000
                    conn.instanceFollowRedirects = true
                    if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext false
                    val total = conn.contentLengthLong
                    zipFile.outputStream().use { out ->
                        conn.inputStream.use { input ->
                            val buf = ByteArray(64 * 1024)
                            var received = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                received += n
                                onProgress(if (total > 0) (received.toFloat() / total).coerceIn(0f, 1f) else 0f)
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
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
            } finally {
                zipFile.delete()
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

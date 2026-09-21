package com.curio.app.ui.components

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Extracts PCM amplitude samples from an audio file for waveform rendering.
 *
 * Uses [MediaExtractor] + [MediaCodec] to decode AAC/M4A files to PCM,
 * then downsamples to `barCount` normalized amplitude values (0.0–1.0).
 *
 * Usage:
 *   val samples = WaveformExtractor.extract(filePath, barCount = 120)
 *   // samples is a FloatArray of normalized amplitudes
 *
 * ── v439 — WHAT "NORMALIZED" MEANS, AND WHY IT CHANGED ──────────────────────
 *
 * It used to mean "divided by 32767": each bar was the loudest sample of its
 * window over the full 16-bit range. **That is not a depiction of the sound.**
 * A phone microphone recording normal speech peaks somewhere around 0.10–0.35 of
 * full scale, so every bar of every voice note landed in the bottom fifth of the
 * range — and the drawing then squared it (see `voiceReach`), which put a normal
 * sentence at about 4% of the band. The member's report is exactly that picture:
 * *"make the wave and bar of the sound in vn more accurate depiction"*. Speech
 * was drawn as a nearly flat line with a few ticks in it.
 *
 * Two things are true of the sound and were missing from the number:
 *
 *  1. **Dynamics are RELATIVE.** A waveform's job is to show how loud each moment
 *     is compared with the rest of THIS recording, not compared with digital full
 *     scale. So each bar is now normalized against the recording's OWN loudest
 *     moment: the peak of the note is 1.0 and everything else is proportional.
 *     (Nothing is stored differently — the bar is still one hex byte and the
 *     note's document does not change, so no old note is invalidated.)
 *  2. **One sample is not a moment.** The old bar was a bare PEAK, which is why a
 *     fast passage drew as a fuzzy band of near-maximum columns. Each bar is now
 *     the geometric mean of its window's peak and RMS — `sqrt(peak · rms)`. RMS
 *     alone would hide a transient (a hard consonant is a spike, not sustained
 *     energy); peak alone is the fuzzy band. The mean of the two is the envelope
 *     a waveform display is supposed to be, and it is what makes the bars rise
 *     and fall with the voice rather than flicker.
 *
 * **A genuinely silent recording is NOT amplified.** If the loudest moment of the
 * file is below [SILENCE_PEAK] the bars are left as they are: honest silence draws
 * a flat line, and normalizing it would turn a recording of a quiet room into a
 * false picture of speech.
 */
object WaveformExtractor {

    /** Number of amplitude bars in the rendered waveform. */
    private const val DEFAULT_BAR_COUNT = 120

    /**
     * Below this peak (as a share of 16-bit full scale) the file is treated as
     * silence and is NOT normalized up — see the note above.
     */
    private const val SILENCE_PEAK = 0.01f

    /**
     * Extract amplitude samples from the audio file at [filePath].
     *
     * @param filePath Absolute path to the audio file.
     * @param barCount Target number of waveform bars.
     * @return FloatArray of size [barCount] with values 0.0–1.0 relative to the
     *         recording's own loudest moment, or null if extraction fails.
     */
    fun extract(filePath: String, barCount: Int = DEFAULT_BAR_COUNT): FloatArray? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val pcm = decodeToPcm(filePath) ?: return null
            downsample(pcm, barCount)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decode the audio file to raw PCM 16-bit samples using MediaExtractor + MediaCodec.
     *
     * ── v439 — AND THE SAMPLES ARE COLLECTED INTO ONE PRIMITIVE ARRAY ────────
     *
     * They used to be appended to a `MutableList<Short>`, which BOXES every one of
     * them: a three-minute 44.1kHz stereo note is sixteen million samples, so that
     * list was sixteen million heap objects and a GC storm at exactly the moment
     * the member had finished speaking and pressed stop. [PcmSink] grows a
     * `ShortArray` instead, which is the same data at a sixtieth of the memory.
     */
    private fun decodeToPcm(filePath: String): ShortArray? {
        val extractor = MediaExtractor().apply {
            try { setDataSource(filePath) } catch (_: Exception) { release(); return null }
        }

        val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run { extractor.release(); return null }

        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: run { extractor.release(); return null }

        extractor.selectTrack(trackIndex)

        val codec = try {
            MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
        } catch (_: Exception) {
            extractor.release()
            return null
        }

        val sink = PcmSink()
        val bufferInfo = MediaCodec.BufferInfo()
        var done = false

        while (!done) {
            val inputIndex = codec.dequeueInputBuffer(10_000)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex) ?: break
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> { /* loop */ }
                outputIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        sink.add(outputBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        done = true
                    }
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        return if (sink.size == 0) null else sink.trimmed()
    }

    /**
     * A growable primitive buffer for decoded PCM.
     *
     * Little-endian on purpose: MediaCodec decoders emit PCM as **little-endian**
     * shorts (per the platform's canonical PCM layout), but a fresh ByteBuffer
     * defaults to BIG_ENDIAN — reading shorts without flipping the order
     * byte-swaps every sample, turning a clean waveform into noise (the "broken
     * visualizer" symptom).
     */
    private class PcmSink {
        private var data = ShortArray(1 shl 16)
        var size = 0
            private set

        fun add(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            val count = info.size / 2  // 2 bytes per 16-bit sample
            if (count <= 0) return
            ensure(size + count)
            val dup = buffer.duplicate()
            dup.order(ByteOrder.LITTLE_ENDIAN)
            dup.position(info.offset)
            dup.limit(info.offset + info.size)
            for (i in 0 until count) {
                data[size + i] = dup.short
            }
            size += count
        }

        private fun ensure(needed: Int) {
            if (needed <= data.size) return
            var next = data.size
            while (next < needed) next = next shl 1
            data = data.copyOf(next)
        }

        fun trimmed(): ShortArray = data.copyOf(size)
    }

    /**
     * Downsample raw PCM samples to [barCount] bars, as the recording's own
     * envelope (see the class note for why it is peak+RMS and why it is relative).
     */
    private fun downsample(samples: ShortArray, barCount: Int): FloatArray {
        if (samples.isEmpty() || barCount <= 0) return FloatArray(barCount)
        val result = FloatArray(barCount)
        val window = (samples.size / barCount).coerceAtLeast(1)

        var loudest = 0f
        for (bar in 0 until barCount) {
            val start = bar * window
            if (start >= samples.size) {
                // A file shorter than the bar count: the tail says nothing rather
                // than repeating the last bar it did have.
                result[bar] = 0f
                continue
            }
            val end = (start + window).coerceAtMost(samples.size)
            var peak = 0
            var energy = 0.0
            for (i in start until end) {
                val value = samples[i].toInt()
                val magnitude = abs(value)
                if (magnitude > peak) peak = magnitude
                energy += value.toDouble() * value
            }
            val count = (end - start).coerceAtLeast(1)
            val peakNorm = peak / 32767f
            val rmsNorm = sqrt(energy / count).toFloat() / 32767f
            // sqrt(peak · rms): an envelope that keeps a consonant visible without
            // letting a whole passage read as one flat maximum.
            val env = sqrt((peakNorm * rmsNorm).coerceAtLeast(0f))
            result[bar] = env
            if (env > loudest) loudest = env
        }

        // RELATIVE TO THIS RECORDING, unless it is silent (see the class note).
        if (loudest <= SILENCE_PEAK) return result
        val scale = 1f / loudest
        for (bar in result.indices) {
            result[bar] = (result[bar] * scale).coerceIn(0f, 1f)
        }
        return result
    }
}

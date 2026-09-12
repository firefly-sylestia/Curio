package com.curio.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Audio recording quality level — controls sampling rate and bitrate.
 *
 * Stored in SharedPreferences. Default: HIGH (v3xx51 — voice notes record at
 * the best quality out of the box; the picker still offers Low/Medium).
 */
enum class AudioQuality(
    val label: String,
    val samplingRate: Int,
    val bitRate: Int,
    val description: String
) {
    LOW(
        label = "Low",
        samplingRate = 44100,
        bitRate = 64000,
        description = "Smaller files, clear voice"
    ),
    MEDIUM(
        label = "Medium",
        samplingRate = 44100,
        bitRate = 128000,
        description = "Balanced quality & size"
    ),
    HIGH(
        label = "High",
        samplingRate = 48000,
        bitRate = 256000,
        description = "Best quality, larger files"
    )
}

/**
 * Persists and retrieves the user's [AudioQuality] preference.
 *
 * Usage:
 *   val quality = AudioQualitySettings.get(context)
 *   AudioQualitySettings.set(context, AudioQuality.HIGH)
 */
object AudioQualitySettings {

    private const val PREFS_NAME = "curio_audio_quality"
    private const val KEY_QUALITY = "quality_level"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the stored quality level (default: HIGH). */
    fun get(context: Context): AudioQuality {
        val ordinal = prefs(context).getInt(KEY_QUALITY, AudioQuality.HIGH.ordinal)
        return AudioQuality.entries.getOrElse(ordinal) { AudioQuality.HIGH }
    }

    /** Persist a new quality level. */
    fun set(context: Context, quality: AudioQuality) {
        prefs(context).edit().putInt(KEY_QUALITY, quality.ordinal).apply()
    }
}

package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONObject

/**
 * THE MEMBER'S OWN NAMES FOR THE CAPTURE FORMATS (v514).
 *
 * A format is a shape, not a label: a member who writes field notes about
 * wildlife may call that row "Observations", and one who records voice takes
 * may call that row "Voice memos". The member asked for the name to be theirs,
 * and for the change to hold EVERYWHERE — so the override lives in one
 * process-wide map that [CaptureFormat.shortName] reads, which means every
 * surface that already prints a format name (the studio, the take rows, the
 * cabinet, the entry page, the recycle bin, a saved entry's preview) picks the
 * rename up without knowing this exists.
 *
 * Storage is its own small prefs file, like the time capsules' — renaming a
 * format is not app state that belongs in the settings blob, and a corrupt
 * entry must never take the settings with it.
 */
object FormatLabels {

    private const val PREFS = "curio_format_labels"

    /** Blank clears the override, so a format always has a name to fall back to. */
    private const val MAX_LENGTH = 24

    private val overrides = mutableStateMapOf<CaptureFormat, String>()

    private var loaded = false

    /** The member's name for [format], or null when they have not renamed it. */
    fun labelOf(format: CaptureFormat): String? =
        overrides[format]?.takeIf { it.isNotBlank() }

    /**
     * Reads the saved names once per process.
     *
     * Called from `MainActivity`'s start-up; a second call is a no-op, so any
     * screen may ask again without paying for it.
     */
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val raw = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null)
        }.getOrNull() ?: return
        runCatching {
            val json = JSONObject(raw)
            for (format in CaptureFormat.entries) {
                val name = json.optString(format.name, "").trim()
                if (name.isNotBlank()) overrides[format] = name.take(MAX_LENGTH)
            }
        }
    }

    /** Renames [format] everywhere. A blank name puts the app's own back. */
    fun rename(context: Context, format: CaptureFormat, label: String) {
        val clean = label.trim().take(MAX_LENGTH)
        if (clean.isBlank()) overrides.remove(format) else overrides[format] = clean
        persist(context)
    }

    /** The app's own name, ignoring any rename — for a "reset" row. */
    fun defaultLabel(format: CaptureFormat): String = when (format) {
        CaptureFormat.SoundBite -> "Voice"
        CaptureFormat.ReelNotes -> "Review"
        CaptureFormat.Marginalia -> "Journal"
        CaptureFormat.GalleryWall -> "Moodboard"
        CaptureFormat.FieldNotes -> "Field notes"
        CaptureFormat.OpenNotebook -> "Wildcard"
    }

    private fun persist(context: Context) {
        val json = JSONObject()
        for (format in CaptureFormat.entries) {
            labelOf(format)?.let { json.put(format.name, it) }
        }
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, json.toString())
                .apply()
        }
    }

    private const val KEY = "names"
}

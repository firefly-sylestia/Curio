package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * v29 — per-topic progress (pages read / episodes watched), keyed by topic
 * id and persisted as JSON in SharedPreferences.
 *
 * Progress is tracked on the TOPIC (not the saved entry): the reveal card,
 * the Cabinet cards and the detail hero all read the same map, so marking
 * progress on a reveal updates the same topic everywhere, and progress
 * survives even before anything is saved to the Cabinet.
 *
 * The [progressState] map is reactive Compose state (topicId → completed
 * count), seeded once from prefs by [seed] and kept in sync on every write.
 */
object TopicProgressStore {

    private const val KEY_PROGRESS = "topic_progress_v1"

    var progressState by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences("curio_prefs", Context.MODE_PRIVATE)

    /** Loads the persisted map (called once from MainActivity onCreate). */
    fun seed(context: Context) {
        progressState = readAll(context)
    }

    fun get(topicId: String): Int = progressState[topicId] ?: 0

    /**
     * Sets completed progress for [topicId], clamped to [max]. Stores only
     * non-zero values so the map stays empty until the user engages.
     */
    fun set(context: Context, topicId: String, value: Int, max: Int) {
        val clamped = value.coerceIn(0, max.coerceAtLeast(0))
        val next = if (clamped <= 0) progressState - topicId
            else progressState + (topicId to clamped)
        progressState = next
        writeAll(context, next)
    }

    /** Clears progress for one topic (used by the editor's reset). */
    fun clear(context: Context, topicId: String) {
        if (topicId !in progressState) return
        progressState = progressState - topicId
        writeAll(context, progressState)
    }

    // ── Persistence ─────────────────────────────────────────────────────

    private fun readAll(context: Context): Map<String, Int> {
        val raw = prefs(context).getString(KEY_PROGRESS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val out = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                out[key] = obj.optInt(key, 0)
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeAll(context: Context, map: Map<String, Int>) {
        val obj = JSONObject()
        map.forEach { (id, v) -> obj.put(id, v) }
        // v29 — commit() (not apply()): an async apply() write can be LOST if
        // the process is killed right after saving, which showed up as
        // progress silently vanishing. The payload is tiny, so the sync
        // write is negligible; durability now matches the UI.
        prefs(context).edit().putString(KEY_PROGRESS, obj.toString()).commit()
    }
}

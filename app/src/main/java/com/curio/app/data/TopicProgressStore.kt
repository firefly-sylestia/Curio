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
 *
 * v126 — [targetOverrides] lets the user FIX a wrong data total from the
 * progress editor dialog: many anime entries carried the SUM of all seasons
 * (a season-1 entry showed the merged total) and book page counts vary by
 * edition, so the dialog's number is now tappable to correct the target for
 * that topic. The override wins over the topic's baked-in
 * `progressTarget` everywhere the pill / card fraction is shown.
 */
object TopicProgressStore {

    private const val KEY_PROGRESS = "topic_progress_v1"
    private const val KEY_TARGETS = "topic_target_overrides_v1"

    var progressState by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** topicId → user-corrected target (pages/episodes). Empty = all baked-in. */
    var targetOverrides by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences("curio_prefs", Context.MODE_PRIVATE)

    /** Loads the persisted maps (called once from MainActivity onCreate). */
    fun seed(context: Context) {
        progressState = readAll(context, KEY_PROGRESS)
        targetOverrides = readAll(context, KEY_TARGETS)
    }

    fun get(topicId: String): Int = progressState[topicId] ?: 0

    /**
     * The effective target for [topicId]: the user's override when present,
     * otherwise [defaultTarget] (the topic's baked-in page/episode count).
     */
    fun getTarget(topicId: String, defaultTarget: Int): Int =
        targetOverrides[topicId] ?: defaultTarget

    /**
     * Sets completed progress for [topicId], clamped to [max]. Stores only
     * non-zero values so the map stays empty until the user engages.
     */
    fun set(context: Context, topicId: String, value: Int, max: Int) {
        val clamped = value.coerceIn(0, max.coerceAtLeast(0))
        val next = if (clamped <= 0) progressState - topicId
            else progressState + (topicId to clamped)
        progressState = next
        writeAll(context, KEY_PROGRESS, next)
    }

    /**
     * v126 — persists the user's corrected target for [topicId] (the number
     * tapped in the progress editor dialog). Used to fix wrong baked-in
     * totals (merged anime seasons, edition-dependent book page counts).
     */
    fun setTarget(context: Context, topicId: String, target: Int) {
        val next = targetOverrides + (topicId to target.coerceAtLeast(1))
        targetOverrides = next
        writeAll(context, KEY_TARGETS, next)
    }

    /** Clears the target override so the topic's baked-in count returns. */
    fun clearTarget(context: Context, topicId: String) {
        if (topicId !in targetOverrides) return
        targetOverrides = targetOverrides - topicId
        writeAll(context, KEY_TARGETS, targetOverrides)
    }

    /** Clears progress for one topic (used by the editor's reset). */
    fun clear(context: Context, topicId: String) {
        if (topicId !in progressState) return
        progressState = progressState - topicId
        writeAll(context, KEY_PROGRESS, progressState)
    }

    // ── Persistence ─────────────────────────────────────────────────────

    private fun readAll(context: Context, key: String): Map<String, Int> {
        val raw = prefs(context).getString(key, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val out = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.optInt(k, 0)
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeAll(context: Context, key: String, map: Map<String, Int>) {
        val obj = JSONObject()
        map.forEach { (id, v) -> obj.put(id, v) }
        // v29 — commit() (not apply()): an async apply() write can be LOST if
        // the process is killed right after saving, which showed up as
        // progress silently vanishing. The payload is tiny, so the sync
        // write is negligible; durability now matches the UI.
        prefs(context).edit().putString(key, obj.toString()).commit()
    }
}

package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * INCURSION — where each title stands, and whether the page exists at all.
 *
 * Two things live here, both of them small:
 *
 *  1. THE UNLOCK. Incursion is not a tab and is not in any menu; it is a page
 *     that appears for a member who types [PHRASE] into any search field in the
 *     app. That check is one function ([offer]) so the phrase is accepted in
 *     exactly one place — see `CurioSearchField`, which every search surface in
 *     the app wears, which is what makes "anywhere" true rather than a claim.
 *  2. THE STATUSES. Six states per title, keyed by the entry's studio-qualified
 *     [IncursionEntry.storageKey] so the same number in two studios stays two
 *     different rows. Stored as one JSON map in the app's prefs, reactive via
 *     [statusState] the way every other store here is.
 *
 * The statuses deliberately do NOT touch the Cabinet, the streak or the topic
 * progress: a viewing order is its own shelf. Nothing here writes to Room, and
 * nothing here can mark a Curio topic watched.
 */
object IncursionStore {

    const val PREFS_NAME = "curio_prefs"
    private const val KEY_UNLOCKED = "incursion_unlocked_v1"
    private const val KEY_STATUS = "incursion_status_v1"

    /** v427 — one line of the member's own, per title. */
    private const val KEY_NOTES = "incursion_notes_v1"

    /**
     * The phrase, lowercase and collapsed, as it is matched.
     *
     * It is matched against the same normalisation of whatever is in the field,
     * so "I Love You 3000", "i love you 3000" and a stray double space all
     * count — a member typing an easter egg should not have to be exact about
     * whitespace.
     */
    const val PHRASE = "i love you 3000"

    /**
     * Every state a title can be in, in the order the picker shows them.
     *
     * v427 — [label] is the state as it is OFFERED ("Plan to watch"), and
     * [shortLabel] is the same state as it is TALLIED ("Planned"). The two live
     * here rather than in a `when` at each call site because a page that offers a
     * state and a page that counts it must never disagree about what it is called,
     * and the chip that shows a row's own state had grown a third mapping of its
     * own.
     */
    enum class Status(val label: String, val shortLabel: String) {
        /** The default: nothing said about it yet. */
        UNWATCHED("Not watched", "Not watched"),
        WATCHING("Watching", "Watching"),
        PLANNED("Plan to watch", "Planned"),
        WATCHED("Watched", "Watched"),
        ON_HOLD("On hold", "On hold"),
        DROPPED("Dropped", "Dropped");

        /** A title that has been decided about, one way or another. */
        val decided: Boolean get() = this != UNWATCHED
    }

    /** `storageKey → status ordinal`. Reactive; seeded once by [seed]. */
    var statusState by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /**
     * v427 — `storageKey → a line of the member's own`. Reactive, seeded by
     * [seed], written through like the statuses.
     *
     * It is ONE line on purpose: a viewing order is a list, and the thing worth
     * writing on a row is the reason it is marked the way it is ("stopped here,
     * the pacing drops") — a notebook per title belongs in the journal, which is
     * where the app already keeps writing.
     */
    var noteState by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** Whether Incursion has been unlocked on this device. Reactive. */
    var unlocked by mutableStateOf(false)
        private set

    /**
     * Set for the instant the phrase is accepted, and cleared by the reveal that
     * shows it — so the page can announce itself once, wherever in the app the
     * member happened to type it.
     */
    var justUnlocked by mutableStateOf(false)
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun seed(context: Context) {
        val prefs = prefs(context)
        unlocked = prefs.getBoolean(KEY_UNLOCKED, false)
        statusState = readAll(prefs.getString(KEY_STATUS, null))
        noteState = readNotes(prefs.getString(KEY_NOTES, null))
    }

    // ── The phrase ───────────────────────────────────────────────────────────

    /** The one way the phrase is ever compared: lowercased, whitespace collapsed. */
    private fun normalise(text: String): String =
        text.lowercase().trim().replace(Regex("\\s+"), " ")

    /**
     * Offers a search field's text to the lock.
     *
     * Called on every keystroke by the shared search field, so it has to be
     * cheap: one lowercase + one collapse, and a comparison that fails on the
     * first character for almost every string. Returns true only the FIRST time
     * the phrase arrives on this device — a member who types it twice gets no
     * second celebration, and a member who never does pays nothing.
     */
    fun offer(context: Context, query: String): Boolean {
        if (unlocked) return false
        if (query.length !in PHRASE.length..(PHRASE.length + 8)) return false
        if (normalise(query) != PHRASE) return false
        prefs(context).edit().putBoolean(KEY_UNLOCKED, true).apply()
        unlocked = true
        justUnlocked = true
        return true
    }

    /** The reveal has been shown; the next unlock (on another device) may announce. */
    fun consumeUnlockReveal() {
        justUnlocked = false
    }

    // ── The statuses ─────────────────────────────────────────────────────────

    fun status(key: String): Status =
        statusState[key]?.let { Status.entries.getOrNull(it) } ?: Status.UNWATCHED

    fun setStatus(context: Context, key: String, status: Status) {
        applyStatuses(context, listOf(key), status)
    }

    /**
     * BULK — the whole of a group at once.
     *
     * This is what a phase header's own action calls: marking Phase One watched
     * is one gesture for the member and one prefs write here, not thirty.
     * [Status.UNWATCHED] removes the rows instead of storing zeroes, so a bulk
     * clear leaves the map as small as it was before the phase was touched.
     */
    fun setGroupStatus(context: Context, keys: List<String>, status: Status) {
        if (keys.isEmpty()) return
        applyStatuses(context, keys, status)
    }

    private fun applyStatuses(context: Context, keys: List<String>, status: Status) {
        val next = statusState.toMutableMap()
        if (status == Status.UNWATCHED) keys.forEach { next.remove(it) }
        else keys.forEach { next[it] = status.ordinal }
        statusState = next
        write(context, next)
    }

    /** How many of [keys] are marked [match]. Synchronous — the lists are tiny. */
    fun count(keys: List<String>, match: (Status) -> Boolean): Int =
        keys.count { match(status(it)) }

    // ── The notes ────────────────────────────────────────────────────────────

    /** The line the member left on a title, or "" when they left none. */
    fun note(key: String): String = noteState[key].orEmpty()

    /**
     * Writes a title's own line. A blank or whitespace-only note REMOVES the
     * entry instead of storing an empty string, so the map stays as small as the
     * notes that actually exist — the same rule the statuses follow for
     * [Status.UNWATCHED].
     */
    fun setNote(context: Context, key: String, text: String) {
        val clean = text.trim().take(NOTE_LIMIT)
        val next = noteState.toMutableMap()
        if (clean.isEmpty()) next.remove(key) else next[key] = clean
        noteState = next
        writeNotes(context, next)
    }

    /** How long one title's own line may be — a line, not a page. */
    const val NOTE_LIMIT = 160

    private fun readNotes(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val value = json.optString(key).trim()
                    if (value.isNotEmpty()) put(key, value)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun writeNotes(context: Context, map: Map<String, String>) {
        val json = JSONObject()
        map.forEach { (key, value) -> runCatching { json.put(key, value) } }
        prefs(context).edit().putString(KEY_NOTES, json.toString()).apply()
    }

    private fun readAll(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val value = json.optInt(key, 0)
                    if (value != 0) put(key, value)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun write(context: Context, map: Map<String, Int>) {
        val json = JSONObject()
        map.forEach { (key, value) -> runCatching { json.put(key, value) } }
        prefs(context).edit().putString(KEY_STATUS, json.toString()).apply()
    }
}

package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * An in-progress explore session — the user tapped "Explore now" on a topic
 * reveal, the app opened a browser search for it, and a timer notification
 * is recording how long they spend. Persisted so the session survives the
 * app being backgrounded or killed: on return/startup the app asks whether
 * they're done and lets them write about it.
 */
data class ExploreSession(
    val categoryId: CategoryId,
    val topicName: String,
    val subtype: String,
    val verb: String,
    val targetName: String,
    val durationMinutes: Int,
    val instruction: String,
    val searchUrl: String,
    val startMillis: Long,
    // Pause state — freezes the visible timer only (the reminder still
    // fires at the original start + duration). [pausedAtMillis] is the
    // instant the current pause began; [accumulatedPausedMillis] banks
    // paused time from completed pauses so elapsed = now - start - total
    // paused. Legacy sessions decode both to the defaults below.
    val paused: Boolean = false,
    val pausedAtMillis: Long? = null,
    val accumulatedPausedMillis: Long = 0L,
    // The user hid the floating explore bubble for this session — persisted
    // so it doesn't pop back on every recomposition or navigation. (Field
    // name kept as `pillHidden` for JSON/legacy compatibility.)
    val pillHidden: Boolean = false,
    // v27 — the session's SHARED note (universal — one per session, shown
    // on every entry saved from it via the editing page's floating note
    // button) and the screenshots captured during the session (the bubble's
    // screenshot button + auto-attached device shots). Persisted with the
    // session so the bubble and the write-it-down page share them; the
    // finish flow hands them off to the write session, and the save page
    // attaches them to the saved entry.
    val note: String = "",
    val screenshotPaths: List<String> = emptyList()
) {
    /**
     * Active explore time — frozen while paused. [now] defaults to the
     * wall clock; pass a captured "now" when ticking in a coroutine.
     */
    fun elapsedMillis(now: Long = System.currentTimeMillis()): Long {
        val effectiveNow = pausedAtMillis ?: now
        return (effectiveNow - startMillis - accumulatedPausedMillis).coerceAtLeast(0L)
    }
}

/**
 * A category-flavored reflection question for an active/ended explore
 * session — "Finished listening? What track or lyric landed hardest?" etc.
 * Shown in the live timer notification and the wrap-up reminder so the
 * user leaves with something to write down. Categories map to the verb
 * their topics use (albums → listening, films → watching, books →
 * reading…); wildcard falls back to the topic's own verb.
 */
fun ExploreSession.reflectionQuestion(): String = when (categoryId) {
    CategoryId.ARTISTS, CategoryId.ALBUMS, CategoryId.SONGS ->
        "Finished listening? What track or lyric landed hardest?"
    CategoryId.DIRECTORS, CategoryId.FILMS, CategoryId.SERIES, CategoryId.ANIME ->
        "Finished watching? What scene or shot stayed with you?"
    CategoryId.AUTHORS, CategoryId.BOOKS, CategoryId.MANGA, CategoryId.MANHWA,
    CategoryId.MYTHOLOGY ->
        "Finished reading? What idea do you want to keep?"
    CategoryId.PAINTERS, CategoryId.ARTWORKS ->
        "Finished looking? What detail caught your eye first?"
    CategoryId.SCIENTISTS, CategoryId.DISCOVERIES ->
        "Finished exploring? What fact surprised you most?"
    CategoryId.GAMES ->
        "Finished watching? What moment or decision stuck with you?"
    CategoryId.SPORTS ->
        "Finished watching? What play or moment do you want to remember?"
    CategoryId.FOOD ->
        "Done reading? What food story surprised you most?"
    CategoryId.INTERNET ->
        "Done watching? What detail made the rabbit hole worth it?"
    CategoryId.WILDCARD -> when (verb.lowercase()) {
        "listen" -> "Finished listening? What caught your ear?"
        "watch" -> "Finished watching? What caught your eye?"
        "read" -> "Finished reading? What will you remember?"
        else -> "Done exploring? What's one thing you'd keep?"
    }
}

/**
 * A topic the user engaged with (tapped Explore) — recently-explored list.
 *
 * [wasUnexplored] marks a topic that previously sat in the recently-
 * unexplored list and the user came back to (resumed) — Home shows a small
 * "Resumed" tag on those rows.
 */
data class ExploredTopic(
    val categoryId: CategoryId,
    val topicName: String,
    val exploredAtMillis: Long,
    val wasUnexplored: Boolean = false
)

/** A topic the user left WITHOUT exploring — recently-unexplored list. */
data class UnexploredTopic(
    val categoryId: CategoryId,
    val topicName: String,
    val seenAtMillis: Long
)

/**
 * Persists the explore-session flow state:
 *  - the single [ExploreSession] (one at a time; starting a new one replaces
 *    any previous),
 *  - the [ExploredTopic] list ("recently explored" on Home — recorded the
 *    moment the user taps Explore on a topic reveal, even before anything is
 *    saved to the Cabinet),
 *  - the [UnexploredTopic] list ("recently unexplored" on Home — topics the
 *    user backed out of without exploring, so they can resume later).
 *
 * All JSON-persisted (topic names carry arbitrary characters, so raw string
 * prefs would break on delimiters). Reactive states are seeded by
 * [seed] from MainActivity and stay in sync as the lists change.
 */
object ExploreSessionStore {

    private const val KEY_ACTIVE_SESSION = "explore_active_session"
    private const val KEY_EXPLORED = "explore_recently_explored"
    private const val KEY_UNEXPLORED = "explore_recently_unexplored"
    private const val KEY_QUEUED_SESSIONS = "explore_queued_sessions"
    // v7.80 — topics the user has EXPLORED or marked "Already watched /
    // listened / read / explored". Persistent and unbounded (unlike the
    // capped recents list) because the shuffle deck must never deal one of
    // these again while alternatives remain.
    private const val KEY_DONE = "explore_done_topics"
    // v27 — the pending "write it down" package (elapsed + shared note +
    // screenshots) that survives the session being cleared, so a process
    // death between finishing and saving never loses the note or shots.
    private const val KEY_PENDING_WRITE = "explore_pending_write"

    // Cap the Home lists so they never grow unbounded.
    private const val MAX_LIST = 12
    // Queued explores are heavier than list rows (full sessions) — keep the
    // pile small so Home never turns into a backlog.
    private const val MAX_QUEUED = 3

    var activeSessionState by mutableStateOf<ExploreSession?>(null)
        private set
    var queuedSessionsState by mutableStateOf<List<ExploreSession>>(emptyList())
        private set
    var recentlyExploredState by mutableStateOf<List<ExploredTopic>>(emptyList())
        private set
    var recentlyUnexploredState by mutableStateOf<List<UnexploredTopic>>(emptyList())
        private set
    // v7.80 — done-topic keys ("CATEGORY::topicName"), read by the Spin
    // deck so explored topics never reappear in the shuffle.
    var doneTopicsState by mutableStateOf<Set<String>>(emptySet())
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences("curio_prefs", Context.MODE_PRIVATE)

    /** Load all persisted state (called once from MainActivity onCreate). */
    fun seed(context: Context) {
        activeSessionState = readSession(context)
        queuedSessionsState = readQueued(context)
        recentlyExploredState = readExplored(context)
        recentlyUnexploredState = readUnexplored(context)
        doneTopicsState = readDone(context)
        readPendingWrite(context)
    }

    // ── Active session ─────────────────────────────────────────────────

    fun getActiveSession(context: Context): ExploreSession? = readSession(context)

    /** Starts (or replaces) the active explore session. */
    fun startSession(context: Context, session: ExploreSession) {
        prefs(context).edit()
            .putString(KEY_ACTIVE_SESSION, session.toJson().toString())
            .apply()
        activeSessionState = session
    }

    /** Clears the active session (explore finished / written about). */
    fun clearSession(context: Context) {
        prefs(context).edit().remove(KEY_ACTIVE_SESSION).apply()
        activeSessionState = null
    }

    // v17/v27 — write-session handoff. The "write about it" flows (the done
    // dialog, Home's session card, and the bubble's Finish) CLEAR the active
    // session before navigating to the capture screen, so the save page can't
    // read the elapsed time live. The ending flow stashes the pause-aware
    // elapsed time PLUS the session's shared note + screenshots here (keyed
    // by topic); the save page peeks them once, only when they match the
    // topic being saved, and clears them once the save succeeds. v27 — the
    // package is PERSISTED so a process death between finishing and saving
    // never loses the note or the captured screenshots; the device-screenshot
    // watcher and the editing page keep appending to it until the save lands.
    private var pendingWriteCategory by mutableStateOf<CategoryId?>(null)
    private var pendingWriteTopic by mutableStateOf<String?>(null)
    private var pendingWriteMillis by mutableStateOf(0L)
    private var pendingWriteNote by mutableStateOf("")
    private var pendingWriteScreenshots by mutableStateOf<List<String>>(emptyList())

    private fun savePendingWrite(context: Context) {
        val cat = pendingWriteCategory ?: return
        prefs(context).edit().putString(
            KEY_PENDING_WRITE,
            JSONObject()
                .put("categoryId", cat.name)
                .put("topicName", pendingWriteTopic ?: "")
                .put("elapsedMillis", pendingWriteMillis)
                .put("note", pendingWriteNote)
                .put(
                    "screenshotPaths",
                    JSONArray().apply { pendingWriteScreenshots.forEach { put(it) } }
                )
                .toString()
        ).apply()
    }

    private fun readPendingWrite(context: Context) {
        val raw = prefs(context).getString(KEY_PENDING_WRITE, null) ?: return
        val obj = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return
        }
        val id = obj.optString("categoryId")
        val cat = CategoryId.values().firstOrNull { it.name == id } ?: return
        pendingWriteCategory = cat
        pendingWriteTopic = obj.optString("topicName")
        pendingWriteMillis = obj.optLong("elapsedMillis").coerceAtLeast(0L)
        pendingWriteNote = obj.optString("note")
        pendingWriteScreenshots = obj.optJSONArray("screenshotPaths")?.let { arr ->
            List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
        } ?: emptyList()
    }

    /**
     * Hands the session's write package (elapsed time + shared note +
     * screenshots) to the upcoming capture page for this topic, called by
     * the session-ending flows just before they clear the session.
     */
    fun handoffWriteSession(
        context: Context,
        categoryId: CategoryId,
        topicName: String,
        elapsedMillis: Long,
        note: String = "",
        screenshots: List<String> = emptyList()
    ) {
        pendingWriteCategory = categoryId
        pendingWriteTopic = topicName
        pendingWriteMillis = elapsedMillis.coerceAtLeast(0L)
        pendingWriteNote = note
        pendingWriteScreenshots = screenshots
        savePendingWrite(context)
    }

    /**
     * Reads the handed-off session duration for [categoryId]/[topicName]
     * WITHOUT consuming it (0 when no handoff matches) — the capture page
     * peeks it at save time and only clears it once the save succeeds, so a
     * failed save + retry keeps the duration.
     */
    fun peekWriteSessionMillis(categoryId: CategoryId, topicName: String): Long =
        if (pendingWriteCategory == categoryId && pendingWriteTopic == topicName)
            pendingWriteMillis else 0L

    /**
     * v27 — the handed-off package's target, when one exists. Lets the
     * device-screenshot watcher and other background paths append to the
     * pending write (which survives the session being cleared) without
     * knowing the topic in advance.
     */
    fun pendingWriteTarget(): Pair<CategoryId, String>? {
        val cat = pendingWriteCategory ?: return null
        val topic = pendingWriteTopic ?: return null
        return cat to topic
    }

    /** v27 — true when a pending write handoff exists for this exact topic. */
    fun hasPendingWriteFor(categoryId: CategoryId, topicName: String): Boolean =
        pendingWriteCategory == categoryId && pendingWriteTopic == topicName

    /** The handed-off shared note ("" when none matches). */
    fun peekWriteSessionNote(categoryId: CategoryId, topicName: String): String =
        if (pendingWriteCategory == categoryId && pendingWriteTopic == topicName)
            pendingWriteNote else ""

    /** The handed-off session screenshots (empty when none match). */
    fun peekWriteSessionScreenshots(categoryId: CategoryId, topicName: String): List<String> =
        if (pendingWriteCategory == categoryId && pendingWriteTopic == topicName)
            pendingWriteScreenshots else emptyList()

    /**
     * v27 — appends a screenshot to the pending write package (used by the
     * device-screenshot watcher and the editing page's add-from-gallery).
     * No-op when no handoff matches [categoryId]/[topicName].
     */
    fun appendPendingScreenshot(
        context: Context,
        categoryId: CategoryId,
        topicName: String,
        path: String
    ) {
        if (pendingWriteCategory != categoryId || pendingWriteTopic != topicName) return
        if (path.isBlank() || path in pendingWriteScreenshots) return
        pendingWriteScreenshots = pendingWriteScreenshots + path
        savePendingWrite(context)
    }

    /** v27 — removes one screenshot from the pending write package. */
    fun removePendingScreenshot(context: Context, categoryId: CategoryId, topicName: String, path: String) {
        if (pendingWriteCategory != categoryId || pendingWriteTopic != topicName) return
        pendingWriteScreenshots = pendingWriteScreenshots.filterNot { it == path }
        savePendingWrite(context)
    }

    /** v27 — sets the shared note on the pending write package. */
    fun setPendingNote(context: Context, categoryId: CategoryId, topicName: String, note: String) {
        if (pendingWriteCategory != categoryId || pendingWriteTopic != topicName) return
        pendingWriteNote = note
        savePendingWrite(context)
    }

    /** Drops the pending handoff for [categoryId]/[topicName] after a save. */
    fun clearWriteSessionHandoff(context: Context, categoryId: CategoryId, topicName: String) {
        if (pendingWriteCategory == categoryId && pendingWriteTopic == topicName) {
            pendingWriteCategory = null
            pendingWriteTopic = null
            pendingWriteMillis = 0L
            pendingWriteNote = ""
            pendingWriteScreenshots = emptyList()
            prefs(context).edit().remove(KEY_PENDING_WRITE).apply()
        }
    }

    // ── Active-session note + screenshots (v27) — the bubble edits these
    // directly on the live session; the finish flow hands them off (see
    // [handoffWriteSession]) and the save page attaches them to the entry.

    /** v27 — the bubble's note field writes the shared note onto the active session. */
    fun setSessionNote(context: Context, note: String) {
        val current = activeSessionState ?: return
        if (current.note == note) return
        startSession(context, current.copy(note = note))
    }

    /** v27 — a captured screenshot joins the active session's screenshot list. */
    fun addSessionScreenshot(context: Context, path: String) {
        val current = activeSessionState ?: return
        if (path.isBlank() || path in current.screenshotPaths) return
        startSession(context, current.copy(screenshotPaths = current.screenshotPaths + path))
    }

    /** v27 — removes one screenshot from the active session. */
    fun removeSessionScreenshot(context: Context, path: String) {
        val current = activeSessionState ?: return
        if (path !in current.screenshotPaths) return
        startSession(context, current.copy(screenshotPaths = current.screenshotPaths.filterNot { it == path }))
    }

    /** Pauses the timer — freezes elapsed display; reminder is unaffected. */
    fun pauseSession(context: Context) {
        val current = activeSessionState ?: return
        if (current.paused) return
        val updated = current.copy(
            paused = true,
            pausedAtMillis = System.currentTimeMillis()
        )
        startSession(context, updated)
    }

    /** Resumes a paused timer, banking the paused span into the session. */
    fun resumeSession(context: Context) {
        val current = activeSessionState ?: return
        val pausedAt = current.pausedAtMillis ?: return
        val now = System.currentTimeMillis()
        val updated = current.copy(
            paused = false,
            pausedAtMillis = null,
            accumulatedPausedMillis = current.accumulatedPausedMillis + (now - pausedAt).coerceAtLeast(0L)
        )
        startSession(context, updated)
    }

    /** Hides (or re-shows) the floating explore bubble for the session. */
    fun setPillHidden(context: Context, hidden: Boolean) {
        val current = activeSessionState ?: return
        if (current.pillHidden == hidden) return
        startSession(context, current.copy(pillHidden = hidden))
    }

    // ── Queued sessions (set aside, resumable) ─────────────────────────
    // When a new explore starts while another is running, the running one is
    // paused (time banked) and queued here instead of silently discarded.
    // Home lists them so the user can swap back anytime.

    /**
     * Pauses the active session (time banked), queues it for later, and
     * vacates the active slot — the caller starts the replacement session
     * immediately. (Without the clear, the session would live BOTH active
     * and queued until the caller's startSession overwrote it.)
     */
    fun queueActiveSession(context: Context) {
        val current = activeSessionState ?: return
        val paused = current.copy(
            paused = true,
            pausedAtMillis = current.pausedAtMillis ?: System.currentTimeMillis()
        )
        saveQueued(context, (listOf(paused) + readQueued(context)).take(MAX_QUEUED))
        clearSession(context)
    }

    /** Removes one queued session (discarded — its banked time is lost). */
    fun removeQueued(context: Context, index: Int) {
        val list = readQueued(context).toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        saveQueued(context, list)
    }

    /** Drops every queued session (feature teardown). */
    fun clearQueued(context: Context) {
        saveQueued(context, emptyList())
    }

    /**
     * Swaps the queued session at [index] into the active slot: the currently
     * running session (if any) is pause-banked into the queue, and the
     * resumed session continues ticking from where it stopped.
     */
    fun resumeQueuedSession(context: Context, index: Int) {
        val queued = readQueued(context).getOrNull(index) ?: return
        val list = readQueued(context).toMutableList()
        list.removeAt(index)
        activeSessionState?.let { current ->
            list.add(
                0,
                current.copy(
                    paused = true,
                    pausedAtMillis = current.pausedAtMillis ?: System.currentTimeMillis()
                )
            )
        }
        saveQueued(context, list.take(MAX_QUEUED))
        startSession(context, queued)
        // Bank the paused span so the timer continues from where it stopped.
        resumeSession(context)
    }

    private fun readQueued(context: Context): List<ExploreSession> {
        val raw = prefs(context).getString(KEY_QUEUED_SESSIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> parseExploreSession(arr.getString(i)) }.filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveQueued(context: Context, sessions: List<ExploreSession>) {
        val arr = JSONArray()
        sessions.forEach { arr.put(it.toJson().toString()) }
        prefs(context).edit().putString(KEY_QUEUED_SESSIONS, arr.toString()).apply()
        queuedSessionsState = sessions
    }

    private fun readSession(context: Context): ExploreSession? {
        val raw = prefs(context).getString(KEY_ACTIVE_SESSION, null) ?: return null
        return parseExploreSession(raw)
    }

    // ── Recently explored ──────────────────────────────────────────────

    /**
     * Records a topic as recently-explored (newest first, deduped by
     * category+topic). Called the moment the user taps "Start exploring" on
     * the reveal screen — independent of any Cabinet save.
     */
    fun recordExplored(context: Context, categoryId: CategoryId, topicName: String) {
        if (topicName.isBlank()) return
        // Was the topic sitting in the recently-unexplored list? If so the
        // user has come BACK to it — tag the explored entry so Home can
        // show a "Resumed" badge instead of a plain explored row.
        val wasUnexplored = readUnexplored(context).any {
            it.categoryId == categoryId && it.topicName == topicName
        }
        val updated = listOf(
            ExploredTopic(
                categoryId = categoryId,
                topicName = topicName,
                exploredAtMillis = System.currentTimeMillis(),
                wasUnexplored = wasUnexplored
            )
        ) + readExplored(context).filterNot {
            it.categoryId == categoryId && it.topicName == topicName
        }
        saveExplored(context, updated.take(MAX_LIST))
        // A topic is no longer unexplored the moment it's explored — drop it
        // from the resume list so the merged Home "Recents" never shows the
        // same topic twice (once "Resumed", once "Unexplored").
        if (wasUnexplored) removeUnexplored(context, categoryId, topicName)
        // v7.80 — exploring a topic marks it DONE: the shuffle deck never
        // deals it again while alternatives remain.
        addDone(context, categoryId, topicName)
        // Feed the quests system — explores drive journey + daily + badges.
        CurioQuests.onExplore(context, categoryId)
    }

    fun removeExplored(context: Context, categoryId: CategoryId, topicName: String) {
        saveExplored(
            context,
            readExplored(context).filterNot {
                it.categoryId == categoryId && it.topicName == topicName
            }
        )
        // Rolling back an explore record also rolls back its done mark, so
        // the topic returns to the shuffle (the conflict-dismissal path).
        removeDone(context, categoryId, topicName)
    }

    // ── Done topics (v7.80) ────────────────────────────────────────────
    // Explored topics (and topics the user marks "Already watched / listened
    // / read / explored") are permanently done: they never appear in the
    // shuffle deck again while unvisited alternatives remain. Unlike the
    // capped recents list this set is unbounded, so an explored topic can't
    // quietly roll back into the deck.

    /**
     * Marks a topic as done — recorded as explored (shows in Home recents,
     * feeds the quests system) AND excluded from the shuffle deck. Called by
     * the reveal screen's "Already …" button.
     */
    fun markDone(context: Context, categoryId: CategoryId, topicName: String) {
        if (topicName.isBlank()) return
        recordExplored(context, categoryId, topicName)
    }

    /** True if the topic is marked done (explored or "already seen"). */
    fun isDone(categoryId: CategoryId, topicName: String): Boolean =
        doneKey(categoryId, topicName) in doneTopicsState

    /**
     * Un-marks a done topic ("not watched after all") — the exact inverse of
     * [markDone]: the done mark is dropped AND the explored recents entry is
     * rolled back, so the topic can appear in the shuffle deck again. Called
     * by the reveal screen's unwatch action.
     */
    fun unmarkDone(context: Context, categoryId: CategoryId, topicName: String) {
        if (topicName.isBlank()) return
        removeExplored(context, categoryId, topicName)
    }

    private fun addDone(context: Context, categoryId: CategoryId, topicName: String) {
        if (topicName.isBlank()) return
        val key = doneKey(categoryId, topicName)
        if (key in doneTopicsState) return
        saveDone(context, doneTopicsState + key)
    }

    private fun removeDone(context: Context, categoryId: CategoryId, topicName: String) {
        val key = doneKey(categoryId, topicName)
        if (key !in doneTopicsState) return
        saveDone(context, doneTopicsState - key)
    }

    private fun doneKey(categoryId: CategoryId, topicName: String): String =
        "${categoryId.name}::$topicName"

    private fun readDone(context: Context): Set<String> {
        val raw = prefs(context).getString(KEY_DONE, null) ?: return emptySet()
        return try {
            val arr = JSONArray(raw)
            val out = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("categoryId")
                val cat = CategoryId.values().firstOrNull { it.name == id } ?: continue
                val name = obj.optString("topicName")
                if (name.isNotBlank()) out.add(doneKey(cat, name))
            }
            out
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun saveDone(context: Context, keys: Set<String>) {
        val arr = JSONArray()
        keys.forEach { key ->
            val sep = key.indexOf("::")
            if (sep <= 0) return@forEach
            val id = key.substring(0, sep)
            arr.put(JSONObject().put("categoryId", id).put("topicName", key.substring(sep + 2)))
        }
        prefs(context).edit().putString(KEY_DONE, arr.toString()).apply()
        doneTopicsState = keys
    }

    private fun readExplored(context: Context): List<ExploredTopic> {
        val raw = prefs(context).getString(KEY_EXPLORED, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val id = obj.optString("categoryId")
                val cat = CategoryId.values().firstOrNull { it.name == id } ?: return@List null
                ExploredTopic(
                    categoryId = cat,
                    topicName = obj.optString("topicName"),
                    exploredAtMillis = obj.optLong("exploredAtMillis"),
                    wasUnexplored = obj.optBoolean("wasUnexplored")
                )
            }.filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveExplored(context: Context, topics: List<ExploredTopic>) {
        val arr = JSONArray()
        topics.forEach {
            arr.put(
                JSONObject()
                    .put("categoryId", it.categoryId.name)
                    .put("topicName", it.topicName)
                    .put("exploredAtMillis", it.exploredAtMillis)
                    .put("wasUnexplored", it.wasUnexplored)
            )
        }
        prefs(context).edit().putString(KEY_EXPLORED, arr.toString()).apply()
        recentlyExploredState = topics
    }

    // ── Recently unexplored ────────────────────────────────────────────

    /**
     * Records a topic the user backed out of WITHOUT exploring (newest
     * first, deduped), so Home can offer "Resume exploring".
     */
    fun recordUnexplored(context: Context, categoryId: CategoryId, topicName: String) {
        if (topicName.isBlank()) return
        val updated = listOf(
            UnexploredTopic(categoryId, topicName, System.currentTimeMillis())
        ) + readUnexplored(context).filterNot {
            it.categoryId == categoryId && it.topicName == topicName
        }
        saveUnexplored(context, updated.take(MAX_LIST))
    }

    /** Clears a topic from the unexplored list once it's actually explored. */
    fun removeUnexplored(context: Context, categoryId: CategoryId, topicName: String) {
        saveUnexplored(
            context,
            readUnexplored(context).filterNot {
                it.categoryId == categoryId && it.topicName == topicName
            }
        )
    }

    private fun readUnexplored(context: Context): List<UnexploredTopic> {
        val raw = prefs(context).getString(KEY_UNEXPLORED, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val id = obj.optString("categoryId")
                val cat = CategoryId.values().firstOrNull { it.name == id } ?: return@List null
                UnexploredTopic(
                    categoryId = cat,
                    topicName = obj.optString("topicName"),
                    seenAtMillis = obj.optLong("seenAtMillis")
                )
            }.filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveUnexplored(context: Context, topics: List<UnexploredTopic>) {
        val arr = JSONArray()
        topics.forEach {
            arr.put(
                JSONObject()
                    .put("categoryId", it.categoryId.name)
                    .put("topicName", it.topicName)
                    .put("seenAtMillis", it.seenAtMillis)
            )
        }
        prefs(context).edit().putString(KEY_UNEXPLORED, arr.toString()).apply()
        recentlyUnexploredState = topics
    }
}

// ── Serialization helpers (shared with the explore-session service) ──────

/** Hand the session across component/process boundaries via intent extras. */
fun ExploreSession.toJsonString(): String = toJson().toString()

/** Inverse of [ExploreSession.toJsonString] — null on malformed input. */
fun parseExploreSession(raw: String): ExploreSession? {
    return runCatching {
        val obj = JSONObject(raw)
        val id = obj.optString("categoryId")
        val cat = CategoryId.values().firstOrNull { it.name == id } ?: return null
        ExploreSession(
            categoryId = cat,
            topicName = obj.optString("topicName"),
            subtype = obj.optString("subtype"),
            verb = obj.optString("verb"),
            targetName = obj.optString("targetName"),
            durationMinutes = obj.optInt("durationMinutes"),
            instruction = obj.optString("instruction"),
            searchUrl = obj.optString("searchUrl"),
            startMillis = obj.optLong("startMillis"),
            paused = obj.optBoolean("paused"),
            pausedAtMillis = if (obj.has("pausedAtMillis") && !obj.isNull("pausedAtMillis"))
                obj.optLong("pausedAtMillis") else null,
            accumulatedPausedMillis = obj.optLong("accumulatedPausedMillis"),
            pillHidden = obj.optBoolean("pillHidden"),
            note = obj.optString("note"),
            screenshotPaths = obj.optJSONArray("screenshotPaths")?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()
        )
    }.getOrNull()
}

private fun ExploreSession.toJson(): JSONObject = JSONObject()
    .put("categoryId", categoryId.name)
    .put("topicName", topicName)
    .put("subtype", subtype)
    .put("verb", verb)
    .put("targetName", targetName)
    .put("durationMinutes", durationMinutes)
    .put("instruction", instruction)
    .put("searchUrl", searchUrl)
    .put("startMillis", startMillis)
    .put("paused", paused)
    .put("pausedAtMillis", pausedAtMillis ?: JSONObject.NULL)
    .put("accumulatedPausedMillis", accumulatedPausedMillis)
    .put("pillHidden", pillHidden)
    .put("note", note)
    .put("screenshotPaths", JSONArray().apply { screenshotPaths.forEach { put(it) } })


/**
 * Formats elapsed explore time as a friendly reading — "34s", "12m 5s",
 * "1h 24m" — not a countdown, just how long the user has been at it.
 * Shared by the done-exploring dialog and the Home session card.
 */
fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

/**
 * Compact session duration for cards and hero labels — "45s", "12m",
 * "1h 24m". Seconds are rounded up so a sub-second session never reads
 * as "0s"; shorter than [formatElapsed]'s seconds-level detail so it fits
 * a small card row or the frosted date segment's tiny line.
 */
fun formatSessionShort(millis: Long): String {
    val totalSeconds = ((millis + 999L) / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

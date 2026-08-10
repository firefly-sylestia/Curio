package com.curio.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
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
    val pillHidden: Boolean = false
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
        "Finished playing? What moment or decision stuck with you?"
    CategoryId.SPORTS ->
        "Finished watching? What play or moment do you want to remember?"
    CategoryId.FOOD ->
        "Done cooking? What flavor surprised you most?"
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
            pillHidden = obj.optBoolean("pillHidden")
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

/**
 * Opens the topic's explore search page WITHOUT recording quest progress —
 * no quest chains, no dailies, no pet events, no recents, no done-mark and
 * no timer (v8.12). Used by the silent "Explore" buttons on the Topic
 * Database and the browse-mode reveal: browsing around must never inflate
 * the user's progress, so it is a pure out-of-app search.
 *
 * v8.13 — it feeds the CATEGORY PASSPORT's engagement counter
 * ([CurioPassport.noteExplore]) and awards the tiny exploration XP
 * ([CurioQuests.awardXpOnly]) — the user asked for XP on these too — but
 * NOTHING else: the pet and the discovery quests stop suggesting a lane the
 * user already tried, while chains, dailies, recents and the done-mark stay
 * untouched.
 */
fun openSilentExplore(context: Context, topic: CurioTopic) {
    CurioPassport.noteExplore(context, topic.categoryId)
    // A silent browse still counts as exploration XP (same as a real
    // explore) without any of the quest tracking.
    CurioQuests.awardXpOnly(context, 5)
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildExploreSearchUrl(topic))))
    }
}

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

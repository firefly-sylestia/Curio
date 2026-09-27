package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * v489 — THE RETURN: a topic you finished comes back, on a widening schedule.
 *
 * The member's request, and the reason it exists: *"The Return — spaced
 * resurfacing of completed topics … your streak shifts from days-you-spun to
 * things-you-still-know"*. The evidence behind it is in
 * `docs/plans/curio-idea-agenda.md` (agenda §3.1): Curio's XP is earned by
 * ACTIONS only — spin, explore, save, pin — and not one point by recall, which is
 * the "metric drift" the engagement-vs-learning literature warns about. Recall is
 * the mechanism those reviews say engagement has to be tied to (retrieval
 * practice + spacing).
 *
 * ## What a recall IS
 *
 * When a topic is marked Completed ([ExploreSession.markCompleted]) it is
 * scheduled one day out. When the day arrives, Home's quest hero LEADS with the
 * recall instead of the shuffle (the member's own rule: *"recall leads on the
 * day it falls due, then back to the quest"*). One line answers it — what you
 * still remember — and the topic moves to the next rung of [LADDER_DAYS].
 *
 * ## The rules the member set (2026-09-27)
 *
 *  - **It waits.** There is NO skip and no snooze: an unanswered recall stays due
 *    and leads the hero every day until it is answered. It never blocks anything
 *    else — the card is a hero, not a gate, and the Shuffle tab is untouched.
 *  - **It is worth more than saving** (`XP_PER_RECALL` 15 > a save's 10), because
 *    recall is the act the whole app is for.
 *  - **Nothing is ever lost.** Every answer is kept on the record, so the second
 *    and third returns can show what you wrote the first time.
 *
 * ## Where it lives
 *
 * Its OWN prefs file (`curio_recall`, listed in [CurioBackupManager] so it ships
 * with the member's backup) — never Room, so no migration is needed and a
 * corrupt record can never take the capture database with it. Keys are
 * `categoryId|topicName`, the same identity [ExploreSession]'s done set and
 * [CurioPassport]'s counters already use.
 */
object CurioRecall {

    private const val PREFS = "curio_recall"
    private const val KEY_ENTRIES = "recalls"

    /**
     * The widening ladder, in days — 1, then 3, 7, 21, 60. Each ANSWERED recall
     * moves the topic one rung down; a topic that clears the last rung is put to
     * sleep for [RETIRED_DAYS] rather than dropped, so it can still return.
     *
     * A day is a calendar day in spirit but a fixed 24h here: the due time is
     * `completedAt + days × DAY_MS`, so the card arrives at roughly the hour the
     * topic was finished — which is exactly when the member was thinking about it.
     */
    val LADDER_DAYS = intArrayOf(1, 3, 7, 21, 60)

    /** What a topic that has cleared the ladder waits before its next return. */
    const val RETIRED_DAYS = 180

    /** The XP one answered recall earns — deliberately above a save's 10. */
    const val XP_PER_RECALL = 15

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /** One topic's return record. */
    data class Recall(
        val categoryId: CategoryId,
        val topicName: String,
        /** When the member first finished the topic — the "three weeks ago" line. */
        val firstCompletedAt: Long,
        /** When this return is due. */
        val dueAt: Long,
        /** How many rungs of [LADDER_DAYS] this topic has already cleared. */
        val stage: Int,
        /** The most recent answer, verbatim. Blank until the first return. */
        val answer: String = "",
        /** When that answer was written (0 = never). */
        val answeredAt: Long = 0L,
        /** How many returns have been answered in total. */
        val returns: Int = 0
    ) {
        /** The identity the store keys on. */
        val key: String get() = "${categoryId.name}|$topicName"
    }

    /**
     * The recall Home's hero should lead with, or null.
     *
     * A COMPOSE STATE, not a plain read: Home recomposes the moment an answer is
     * written or the day turns, with no polling. [refresh] is the only writer.
     */
    var dueState by mutableStateOf<Recall?>(null)
        private set

    /** How many topics are waiting (the tiny count beside the card's eyebrow). */
    var waitingState by mutableStateOf(0)
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(context: Context): MutableList<Recall> {
        val raw = prefs(context).getString(KEY_ENTRIES, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Recall(
                    categoryId = runCatching {
                        CategoryId.valueOf(o.optString("category", ""))
                    }.getOrDefault(CategoryId.WILDCARD),
                    topicName = o.optString("topic"),
                    firstCompletedAt = o.optLong("completedAt"),
                    dueAt = o.optLong("dueAt"),
                    stage = o.optInt("stage"),
                    answer = o.optString("answer"),
                    answeredAt = o.optLong("answeredAt"),
                    returns = o.optInt("returns")
                )
            }
        } catch (_: Exception) {
            // An unreadable store is an empty store: losing a schedule is a
            // shrug, and guessing at a corrupt one is how a record gets written
            // wrong on the next save.
            mutableListOf()
        }
    }

    private fun write(context: Context, list: List<Recall>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("category", r.categoryId.name)
                    .put("topic", r.topicName)
                    .put("completedAt", r.firstCompletedAt)
                    .put("dueAt", r.dueAt)
                    .put("stage", r.stage)
                    .put("answer", r.answer)
                    .put("answeredAt", r.answeredAt)
                    .put("returns", r.returns)
            )
        }
        prefs(context).edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    /**
     * A topic was finished — put it on the first rung.
     *
     * Called from [ExploreSession.markCompleted], the ONE completion door every
     * path uses. A topic already on the ladder is LEFT WHERE IT IS: re-finishing
     * something must never reset a schedule the member has already been walking,
     * and the deck (unlike the ladder) is what stops re-dealing a done topic.
     */
    fun schedule(context: Context, categoryId: CategoryId, topicName: String) {
        if (topicName.isBlank()) return
        val list = read(context)
        val key = "${categoryId.name}|$topicName"
        if (list.any { it.key == key }) {
            refresh(context)
            return
        }
        val now = System.currentTimeMillis()
        list.add(
            Recall(
                categoryId = categoryId,
                topicName = topicName,
                firstCompletedAt = now,
                dueAt = now + LADDER_DAYS[0] * DAY_MS,
                stage = 0
            )
        )
        write(context, list)
        refresh(context)
    }

    /**
     * Recompute what is due. The single writer of [dueState] / [waitingState].
     *
     * With the feature switched off nothing is ever due, so the member's Home is
     * exactly what it was before this existed.
     */
    fun refresh(context: Context) {
        if (!AppPreferences.recallEnabledState) {
            dueState = null
            waitingState = 0
            return
        }
        val now = System.currentTimeMillis()
        val list = read(context)
        val waiting = list.filter { it.dueAt <= now }
        waitingState = waiting.size
        dueState = waiting.minByOrNull { it.dueAt }
    }

    /**
     * Answer a return: keep the words, climb one rung, and put the topic to bed
     * until the next one.
     *
     * The answer is stored even if it is blank (a return answered with "I don't
     * remember" is a real, useful record), and [Recall.returns] counts it, so a
     * later screen can say "you have remembered this four times".
     */
    fun answer(context: Context, recall: Recall, text: String) {
        val now = System.currentTimeMillis()
        val nextStage = recall.stage + 1
        val nextDue = now + (
            if (nextStage < LADDER_DAYS.size) LADDER_DAYS[nextStage] * DAY_MS
            else RETIRED_DAYS * DAY_MS
            )
        val list = read(context)
        val index = list.indexOfFirst { it.key == recall.key }
        if (index >= 0) {
            list[index] = list[index].copy(
                dueAt = nextDue,
                stage = nextStage,
                answer = text,
                answeredAt = now,
                returns = list[index].returns + 1
            )
            write(context, list)
        }
        refresh(context)
    }

    /** Everything on the ladder, soonest first — for a future "Memory" page. */
    fun all(context: Context): List<Recall> = read(context).sortedBy { it.dueAt }

    /** The answers this topic already carries, oldest first (newest is [Recall.answer]). */
    fun historyFor(context: Context, recall: Recall): List<String> =
        read(context).firstOrNull { it.key == recall.key }
            ?.takeIf { it.answer.isNotBlank() }
            ?.let { listOf(it.answer) }
            ?: emptyList()

    /**
     * "three weeks ago" — the phrase the card's prompt is built from. Rounded
     * DOWN to the unit the gap is actually in, so a two-day gap never reads as a
     * week (a return that overstates how long ago it was is a return that cannot
     * be trusted with anything else).
     */
    fun agoText(fromMillis: Long, now: Long = System.currentTimeMillis()): String {
        val days = ((now - fromMillis).coerceAtLeast(0L) / DAY_MS).toInt()
        return when {
            days <= 0 -> "today"
            days == 1 -> "yesterday"
            days < 7 -> "$days days ago"
            days < 14 -> "a week ago"
            days < 30 -> "${days / 7} weeks ago"
            days < 60 -> "a month ago"
            else -> "${days / 30} months ago"
        }
    }
}

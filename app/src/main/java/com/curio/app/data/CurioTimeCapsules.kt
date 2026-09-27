package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * v493 — TIME CAPSULES: a letter you write now and Curio hands back later.
 *
 * The idea is agenda § 5.5, and the member's own shape for it: *"a new time
 * capsule option with its page … and it covers the whole screen on the day it
 * returns"*, with *"a really beautiful time machine style ui to write the
 * message"*. So there is exactly one door, its own writing page, and a
 * full-screen arrival on the day — no card in a list, no notification to ignore.
 *
 * ## What is actually new here
 *
 * The app can already hold a note. What it cannot do is hold one **you cannot
 * read** — and that is the whole feature. A capsule is sealed (the message is
 * the payload, the seal date is a promise) and the only way back in is the day
 * arriving. Nothing in this file can surface a capsule early except [due], and
 * [due] asks the clock, not the caller.
 *
 * ## Its own prefs file, like the other two
 *
 * `curio_time_capsules`, listed in [CurioBackupManager] so a capsule ships with
 * the member's backup — a letter to yourself is the last thing that should be
 * lost to a reinstall. No Room migration, and a corrupt record can never take
 * the capture database with it.
 *
 * ## It never expires and it is never due twice
 *
 * A capsule past its day waits, exactly as a recall does: [due] returns the
 * OLDEST unopened one whose day has come, so a member who is away for a month
 * comes back to their letters in the order they were written, and [open] is the
 * only thing that retires one.
 */
object CurioTimeCapsules {

    private const val PREFS = "curio_time_capsules"
    private const val KEY_ITEMS = "capsules"

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /**
     * The spans the machine's dial can be set to, in the dial's own order.
     *
     * A fixed ladder rather than a date picker on purpose: the choice a time
     * capsule asks is *"how far ahead"*, not *"which Tuesday"*, and five named
     * spans are a thing a thumb can turn. [months] is what the arithmetic uses;
     * [label] is what the dial says.
     */
    enum class Span(val label: String, val months: Int) {
        ONE_MONTH("1 month", 1),
        THREE_MONTHS("3 months", 3),
        SIX_MONTHS("6 months", 6),
        ONE_YEAR("1 year", 12),
        FIVE_YEARS("5 years", 60);

        /** The day this span lands on, from [from]. */
        fun openAt(from: Long): Long = from + months * 30L * DAY_MS
    }

    /** One sealed letter. */
    data class Capsule(
        /** Stable identity — the day it was sealed to the millisecond. */
        val id: String,
        val message: String,
        val sealedAt: Long,
        val openAt: Long,
        /** Which rung of [Span] it was sealed on, for the arrival's own line. */
        val spanLabel: String,
        /** When it was opened (0 = still sealed). */
        val openedAt: Long = 0L
    ) {
        val sealed: Boolean get() = openedAt == 0L
    }

    /**
     * The capsule waiting to be opened, or null.
     *
     * Compose state, not a plain read: Home recomposes the instant one is sealed
     * or opened, with no polling. [refresh] is the only writer.
     */
    var dueState by mutableStateOf<Capsule?>(null)
        private set

    /** How many are waiting in total — the arrival's quiet count. */
    var waitingState by mutableStateOf(0)
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(context: Context): MutableList<Capsule> {
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Capsule(
                    id = o.optString("id"),
                    message = o.optString("message"),
                    sealedAt = o.optLong("sealedAt"),
                    openAt = o.optLong("openAt"),
                    spanLabel = o.optString("span"),
                    openedAt = o.optLong("openedAt")
                )
            }
        } catch (_: Exception) {
            // An unreadable store is an empty store: a corrupt record is a
            // shrug, and guessing at one is how it gets written wrong next.
            mutableListOf()
        }
    }

    private fun write(context: Context, list: List<Capsule>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("message", c.message)
                    .put("sealedAt", c.sealedAt)
                    .put("openAt", c.openAt)
                    .put("span", c.spanLabel)
                    .put("openedAt", c.openedAt)
            )
        }
        prefs(context).edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    /** Everything ever sealed, newest first — for a future shelf. */
    fun all(context: Context): List<Capsule> =
        read(context).sortedByDescending { it.sealedAt }

    /**
     * Seal a message for [span] from now. Returns the capsule it wrote, so the
     * caller can show the member the day it will land without re-reading.
     *
     * A blank message seals nothing and returns null: an empty letter is not a
     * letter, and a capsule with no words in it would arrive as a disappointment
     * three months later with nothing anywhere to explain it.
     */
    fun seal(context: Context, message: String, span: Span): Capsule? {
        val text = message.trim()
        if (text.isBlank()) return null
        val now = System.currentTimeMillis()
        val capsule = Capsule(
            id = "cap-$now",
            message = text,
            sealedAt = now,
            openAt = span.openAt(now),
            spanLabel = span.label
        )
        val list = read(context)
        list.add(capsule)
        write(context, list)
        refresh(context)
        return capsule
    }

    /**
     * Recompute what is due. The single writer of [dueState] / [waitingState].
     *
     * With the feature switched off nothing is ever due, so Home is exactly what
     * it was before this existed — and NOTHING is lost: the letters stay sealed
     * in `curio_time_capsules` and arrive the moment it is switched back on.
     */
    fun refresh(context: Context) {
        if (!AppPreferences.timeCapsuleEnabledState) {
            dueState = null
            waitingState = 0
            return
        }
        val now = System.currentTimeMillis()
        val waiting = read(context).filter { it.sealed && it.openAt <= now }
        waitingState = waiting.size
        // The OLDEST first: a member who was away gets their letters in the order
        // they were written, which is the order they meant them in.
        dueState = waiting.minByOrNull { it.openAt }
    }

    /** The seal is broken. The words are the member's for good from here. */
    fun open(context: Context, capsule: Capsule) {
        val list = read(context)
        val index = list.indexOfFirst { it.id == capsule.id }
        if (index >= 0) {
            list[index] = list[index].copy(openedAt = System.currentTimeMillis())
            write(context, list)
        }
        refresh(context)
    }

    /** How long until [capsule] lands, phrased — for the writing page's promise. */
    fun untilText(openAt: Long, now: Long = System.currentTimeMillis()): String {
        val days = ((openAt - now).coerceAtLeast(0L) / DAY_MS).toInt()
        return when {
            days <= 1 -> "tomorrow"
            days < 60 -> "in $days days"
            days < 365 -> "in ${days / 30} months"
            else -> "in ${days / 365} years"
        }
    }
}

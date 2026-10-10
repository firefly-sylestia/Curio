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
    private const val KEY_DRAFT = "draft"

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /**
     * ⚠️ v496 — THE FIVE-SPAN DIAL IS GONE, AND THE MOMENT IS EXACT.
     *
     * The first version asked *"how far ahead"* and answered it with a brass dial
     * of five named spans (`Span`, 1 / 3 / 6 / 12 / 60 months). The member has
     * since judged the whole writing page out and named what it should have been
     * instead — *"no way to customise the date by day time hour minutes etc"* — so
     * a capsule is now sealed for **an exact instant** ([sealAt]), chosen on the
     * new page's own clock, and the label that rides with it is the member's own
     * date and time in words. Do not bring the ladder back: thirty-day months were
     * a convenience of the dial and are nobody's idea of a promised day.
     */
    /**
     * ⚠️ v501 — THE FLOOR IS SECONDS, NOT MINUTES (a real bug the member hit).
     *
     * It was FIVE MINUTES, on the reasoning that a capsule a minute out is a
     * mis-set clock. That reasoning was wrong twice over. A letter written for
     * *"in one minute"* is a perfectly good capsule — that is exactly what the
     * member was trying to seal when they reported that the seal *"wasnt working
     * when the time duration as like 1 min or 2 min"* — and a ROLLING five-minute
     * floor is worse than a fixed one: the page clamped the clock up to `now + 5
     * min` when the moment was chosen, and every second that passed while the
     * member wrote their letter put that same instant back INSIDE the floor, so
     * the seal was refused by a clock that still read a legal, future time and
     * said nothing about why.
     *
     * Five SECONDS keeps the one real meaning of a floor: a capsule is sealed for
     * a moment strictly ahead of the seal, never for an instant already gone.
     */
    const val MIN_LEAD_MS = 5L * 1000L

    /** One sealed letter. */
    data class Capsule(
        /** Stable identity — the day it was sealed to the millisecond. */
        val id: String,
        val message: String,
        val sealedAt: Long,
        val openAt: Long,
        /**
         * The promise in the MEMBER'S OWN WORDS — since v497 the date and time
         * the page's own clock was set to ("12 March 2027 at 09:30"), which is
         * what the arrival reads back. It is NOT an interval any more: the
         * five-span dial that used to write "a month" here is deleted.
         */
        val spanLabel: String,
        /** When it was opened (0 = still sealed). */
        val openedAt: Long = 0L,
        /**
         * v502 — A PICTURE SEALED WITH THE LETTER, as a persisted content URI
         * ("" = none). The member: *"and your letter place is bad too and it
         * doesnt even have any image or voive note option"*.
         *
         * The URI is the one the picker handed back with a PERSISTABLE grant
         * taken on it (the same pattern the journal's prints use), so it still
         * resolves months later — a capsule is read after the app has been
         * restarted a hundred times, and a temporary grant would have expired.
         */
        val photo: String = "",
        /**
         * v502 — THE MEMBER'S OWN VOICE, sealed with the letter: a path in the
         * app's own audio store (see AudioStorageManager), with the facts the
         * wave is drawn from ([voiceBars], [voiceSeconds]) kept beside it so the
         * note renders and plays without decoding anything.
         */
        val voice: String = "",
        val voiceSeconds: Int = 0,
        val voiceBars: String = ""
    ) {
        val sealed: Boolean get() = openedAt == 0L
    }

    /**
     * ── v506 — THE LETTER BEING WRITTEN, KEPT WHEN THE PAGE IS LEFT ────────────
     *
     * The member: *"and also it doesnt have draft dalog too"*. A capsule page is a
     * page you write a letter on, and backing out of it — or being interrupted by
     * the phone — used to throw the words away with nothing said. So a half-written
     * letter is a DRAFT: the writing page asks before it lets one go (see
     * `CapsuleDraftDialog`), and "Keep as draft" writes it here.
     *
     * Not Compose state, unlike the capsule lists: no other screen shows a draft, and
     * the page reads it once as it opens, so a `refresh()` read would be a wasted
     * read on every Home visit.
     */
    data class Draft(
        val message: String = "",
        val photo: String = "",
        val voice: String = "",
        val voiceSeconds: Int = 0,
        val voiceBars: String = "",
        /** The clock the member had set. 0 = the page's own default. */
        val openAt: Long = 0L
    ) {
        val isEmpty: Boolean
            get() = message.isBlank() && photo.isBlank() && voice.isBlank()
    }

    /** The draft on this device, or null. Read once, as the writing page opens. */
    fun draft(context: Context): Draft? {
        val raw = prefs(context).getString(KEY_DRAFT, null) ?: return null
        return try {
            val o = JSONObject(raw)
            Draft(
                message = o.optString("message"),
                photo = o.optString("photo"),
                voice = o.optString("voice"),
                voiceSeconds = o.optInt("voiceSeconds"),
                voiceBars = o.optString("voiceBars"),
                openAt = o.optLong("openAt")
            ).takeUnless { it.isEmpty }
        } catch (_: Exception) {
            null
        }
    }

    /** Keep a half-written letter. An empty draft CLEARS it, so the page has one door. */
    fun saveDraft(context: Context, draft: Draft) {
        if (draft.isEmpty) {
            clearDraft(context)
            return
        }
        val json = JSONObject()
            .put("message", draft.message)
            .put("photo", draft.photo)
            .put("voice", draft.voice)
            .put("voiceSeconds", draft.voiceSeconds)
            .put("voiceBars", draft.voiceBars)
            .put("openAt", draft.openAt)
            .toString()
        prefs(context).edit().putString(KEY_DRAFT, json).apply()
    }

    /**
     * The draft is gone — the letter was sealed, or the member threw it away. Called
     * by the page after [sealAt] succeeds, so a sealed letter can never come back as a
     * draft of itself.
     */
    fun clearDraft(context: Context) {
        prefs(context).edit().remove(KEY_DRAFT).apply()
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

    /**
     * v496 — THE ONE STILL SEALED NEAREST, and how many are sealed at all.
     *
     * The arrival only ever cares about what is DUE, so a member sealing a letter
     * a year out used to have no evidence anywhere that it existed until the year
     * was up. Home's capsule (see `TimeCapsuleCapsule`) says how many letters are
     * waiting and when the next one lands, which needs these two — both Compose
     * state, written by [refresh] like everything else here.
     */
    var nextAtState by mutableStateOf(0L)
        private set

    var sealedCountState by mutableStateOf(0)
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
                    openedAt = o.optLong("openedAt"),
                    // v502 — a capsule sealed before the letter could carry a
                    // picture or a voice simply has none: the fields are OPTIONAL
                    // in the record, so an old letter still reads.
                    photo = o.optString("photo"),
                    voice = o.optString("voice"),
                    voiceSeconds = o.optInt("voiceSeconds"),
                    voiceBars = o.optString("voiceBars")
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
                    .put("photo", c.photo)
                    .put("voice", c.voice)
                    .put("voiceSeconds", c.voiceSeconds)
                    .put("voiceBars", c.voiceBars)
            )
        }
        prefs(context).edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    /** Everything ever sealed, newest first — for a future shelf. */
    fun all(context: Context): List<Capsule> =
        read(context).sortedByDescending { it.sealedAt }

    /**
     * v496 — SEAL A MESSAGE FOR AN EXACT MOMENT ([openAt]).
     *
     * Returns the capsule it wrote, so the caller can show the member the day it
     * will land without re-reading. [label] is the promise in the member's own
     * words (the new page passes its clock's date and time), and it is what the
     * arrival reads back.
     *
     * Nothing is sealed for null: a blank message is not a letter (a capsule with
     * no words in it would arrive as a disappointment with nothing anywhere to
     * explain it), and a moment inside [MIN_LEAD_MS] is refused rather than nudged
     * — silently moving the day the member chose would be worse than saying no, so
     * the PAGE is what keeps its clock legal (see `TimeCapsuleScreen`).
     */
    fun sealAt(
        context: Context,
        message: String,
        openAt: Long,
        label: String,
        /**
         * v502 — WHAT THE LETTER CARRIES BESIDES WORDS: a picture (a persisted
         * content URI) and a voice note (a path in the app's own audio store).
         * Both are optional, and both are sealed WITH the letter — the store
         * holds them, so nothing on the writing page has to survive the screen.
         */
        photo: String = "",
        voice: String = "",
        voiceSeconds: Int = 0,
        voiceBars: String = ""
    ): Capsule? {
        val text = message.trim()
        // A letter needs SOMETHING in it. Words are the ordinary something, but a
        // picture the member chose and a recording of their own voice are a letter
        // too — refusing those would be the store second-guessing the page.
        if (text.isBlank() && photo.isBlank() && voice.isBlank()) return null
        val now = System.currentTimeMillis()
        if (openAt <= now + MIN_LEAD_MS) return null
        val capsule = Capsule(
            id = "cap-$now",
            message = text,
            sealedAt = now,
            openAt = openAt,
            spanLabel = label,
            photo = photo,
            voice = voice,
            voiceSeconds = voiceSeconds,
            voiceBars = voiceBars
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
            nextAtState = 0L
            sealedCountState = 0
            return
        }
        val now = System.currentTimeMillis()
        val sealed = read(context).filter { it.sealed }
        val waiting = sealed.filter { it.openAt <= now }
        waitingState = waiting.size
        // The OLDEST first: a member who was away gets their letters in the order
        // they were written, which is the order they meant them in.
        dueState = waiting.minByOrNull { it.openAt }
        // v496 — and the ones still ahead of the clock, for Home's capsule.
        sealedCountState = sealed.size
        nextAtState = sealed.filter { it.openAt > now }.minOfOrNull { it.openAt } ?: 0L
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

    /**
     * How long until a capsule lands, phrased — for the writing page's promise.
     *
     * v496 — IT COUNTED IN DAYS ONLY, which was right for a dial that could not be
     * set finer than a month and wrong for a clock that can be set to the minute
     * ("in 0 days" was what a letter sealed for this evening used to say). It now
     * walks minutes → hours → days → months → years.
     */
    fun untilText(openAt: Long, now: Long = System.currentTimeMillis()): String {
        val left = openAt - now
        if (left <= 0L) return "now"
        val minutes = left / 60_000L
        val hours = left / 3_600_000L
        val days = left / DAY_MS
        return when {
            minutes < 1 -> "in under a minute"
            minutes < 60 -> "in $minutes ${if (minutes == 1L) "minute" else "minutes"}"
            hours < 24 -> "in $hours ${if (hours == 1L) "hour" else "hours"}"
            days <= 1 -> "tomorrow"
            days < 60 -> "in $days days"
            days < 365 -> "in ${days / 30} months"
            else -> {
                val years = days / 365
                "in $years ${if (years == 1L) "year" else "years"}"
            }
        }
    }
}

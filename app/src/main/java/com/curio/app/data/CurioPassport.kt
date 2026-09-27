package com.curio.app.data

import android.content.Context
import org.json.JSONObject

/**
 * The Category Passport (spec §6) — tracks the user's relationship with
 * every visible category so Quests can stamp explored lanes and generate
 * discovery quests that push into categories the user has barely touched.
 *
 * Per category it counts spins, topic reveals (peeked), explores started and
 * captures saved, plus the last-explored timestamp. Stamps (spec §6.1):
 *  - [Stamp.UNSEEN]   — never opened a topic here
 *  - [Stamp.PEEKED]   — revealed/opened a topic but didn't explore
 *  - [Stamp.EXPLORED] — started an explore
 *  - [Stamp.MASTERED] — saved a capture (completed the lane's loop)
 *
 * Hooks are fire-and-forget, called next to the existing [CurioQuests] event
 * hooks (SpinScreen, TopicRevealScreen, ExploreSessionStore, SaveCaptureScreen).
 * Persisted in its own SharedPreferences file (`curio_passport`, listed in
 * [CurioBackupManager] so it ships with the user's backup).
 */
object CurioPassport {

    private const val PREFS_NAME = "curio_passport"
    private const val KEY_SPINS = "spins"
    private const val KEY_REVEALS = "reveals"
    private const val KEY_EXPLORES = "explores"
    private const val KEY_SAVES = "saves"
    private const val KEY_LAST = "last_explored"
    // v491 — THE PRESSED STAMPS. A lane's MASTERED stamp is EARNED by real
    // activity (see [CategoryProgress.stamp]) and then PRESSED by the member:
    // a rubber stamp head comes down, the imprint lands with a thunk, and it
    // stays. This set records which lanes have had that press, so the ceremony
    // happens once per lane and the imprint is never taken back.
    private const val KEY_PRESSED = "pressed_lanes"
    private const val KEY_PRESS_SEEDED = "pressed_seeded"

    enum class Stamp { UNSEEN, PEEKED, EXPLORED, MASTERED }

    /** Per-category counters — the [Stamp] is derived from them. */
    data class CategoryProgress(
        val spins: Int = 0,
        val reveals: Int = 0,
        val explores: Int = 0,
        val saves: Int = 0,
        val lastAt: Long = 0L
    ) {
        val stamp: Stamp
            get() = when {
                saves > 0 -> Stamp.MASTERED
                explores > 0 -> Stamp.EXPLORED
                reveals > 0 -> Stamp.PEEKED
                // v8.13 — a spin counts as a peek. Without this the WILDCARD
                // lane (whose reveals/explores/saves always resolve to a real
                // category, so it ONLY ever accumulates spins) would show
                // "New · spin!" on the passport forever.
                spins > 0 -> Stamp.PEEKED
                else -> Stamp.UNSEEN
            }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readMap(context: Context, key: String): Map<String, Int> {
        val raw = prefs(context).getString(key, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap { obj.keys().forEach { k -> put(k, obj.optInt(k)) } }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun readLast(context: Context): Map<String, Long> {
        val raw = prefs(context).getString(KEY_LAST, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap { obj.keys().forEach { k -> put(k, obj.optLong(k)) } }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun bump(context: Context, key: String, category: CategoryId) {
        val current = readMap(context, key)
        val updated = current + (category.name to ((current[category.name] ?: 0) + 1))
        prefs(context).edit().putString(key, JSONObject(updated).toString()).apply()
    }

    private fun touchLast(context: Context, category: CategoryId) {
        val current = readLast(context)
        val updated = current + (category.name to System.currentTimeMillis())
        prefs(context).edit().putString(KEY_LAST, JSONObject(updated).toString()).apply()
    }

    // ── Hooks (fire-and-forget, called from the real action sites) ──────

    /** A spin landed on a topic in [category] (SpinScreen, at the settle). */
    fun noteSpin(context: Context, category: CategoryId) {
        bump(context, KEY_SPINS, category)
    }

    /** A topic reveal opened in [category] (TopicRevealScreen). */
    fun noteReveal(context: Context, category: CategoryId) {
        bump(context, KEY_REVEALS, category)
        touchLast(context, category)
    }

    /** An explore started in [category] (CurioQuests.onExplore). */
    fun noteExplore(context: Context, category: CategoryId) {
        bump(context, KEY_EXPLORES, category)
        touchLast(context, category)
    }

    /** A capture was saved for [category] (SaveCaptureScreen, new saves only). */
    fun noteSave(context: Context, category: CategoryId) {
        bump(context, KEY_SAVES, category)
        touchLast(context, category)
    }

    // ── Reads ───────────────────────────────────────────────────────────

    /** One category's counters. */
    fun progress(context: Context, category: CategoryId): CategoryProgress {
        val name = category.name
        return CategoryProgress(
            spins = readMap(context, KEY_SPINS)[name] ?: 0,
            reveals = readMap(context, KEY_REVEALS)[name] ?: 0,
            explores = readMap(context, KEY_EXPLORES)[name] ?: 0,
            saves = readMap(context, KEY_SAVES)[name] ?: 0,
            lastAt = readLast(context)[name] ?: 0L
        )
    }

    /** All visible categories' counters (keyed by category id). */
    fun allProgress(context: Context): Map<CategoryId, CategoryProgress> =
        CurioCategories.visible.associate { it.id to progress(context, it.id) }

    /** Number of lanes the user has at least explored (drives discovery). */
    fun exploredLaneCount(context: Context): Int =
        allProgress(context).count { it.value.explores > 0 }

    // ── The press (v491) ────────────────────────────────────────────────

    /** Every lane id whose mastered stamp has been pressed. */
    fun pressedStamps(context: Context): Set<String> {
        ensurePressSeed(context)
        return readPressed(context)
    }

    /**
     * Records the press. Once per lane, for ever — an imprint is not a state
     * that can be un-taken, and pressing again is not offered anywhere.
     */
    fun markStampPressed(context: Context, category: CategoryId) {
        ensurePressSeed(context)
        val pressed = readPressed(context)
        if (category.name in pressed) return
        prefs(context).edit()
            .putString(KEY_PRESSED, JSONObject(pressed.associateWith { true } + (category.name to true)).toString())
            .apply()
    }

    private fun readPressed(context: Context): Set<String> {
        val raw = prefs(context).getString(KEY_PRESSED, null) ?: return emptySet()
        return try {
            val obj = JSONObject(raw)
            buildSet { obj.keys().forEach { k -> if (obj.optBoolean(k)) add(k) } }
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * THE ONE-TIME GRANDFATHER — and the reason this feature does not break an
     * existing passport.
     *
     * A member who upgrades already has stamps: they appeared on their own, as
     * they always had. The moment the press exists, "mastered and not pressed"
     * would be true for every one of those lanes at once and the whole page
     * would read as un-inked — losing a record the member earned. So the very
     * first read marks every ALREADY-mastered lane as pressed: the stamps they
     * had stay exactly as they were, and only a lane mastered FROM HERE ON
     * waits for its press. It runs once (`KEY_PRESS_SEEDED`) and never again,
     * so a lane mastered later can never be silently swept in.
     */
    private fun ensurePressSeed(context: Context) {
        if (prefs(context).getBoolean(KEY_PRESS_SEEDED, false)) return
        val already = CurioCategories.visible
            .filter { progress(context, it.id).stamp == Stamp.MASTERED }
            .associate { it.id.name to true }
        val merged = readPressed(context).associateWith { true } + already
        prefs(context).edit()
            .putString(KEY_PRESSED, JSONObject(merged).toString())
            .putBoolean(KEY_PRESS_SEEDED, true)
            .apply()
    }

    /** The stamp for the least-engaged visible category (null when all seen). */
    fun leastEngaged(context: Context): CurioCategory? {
        val byId = allProgress(context)
        return CurioCategories.visible.minByOrNull { cat ->
            val p = byId[cat.id] ?: CategoryProgress()
            // Order matters: unseen (0) < peeked (1) < explored (2) < mastered (3).
            when (p.stamp) {
                Stamp.UNSEEN -> 0
                Stamp.PEEKED -> 1
                Stamp.EXPLORED -> 2
                Stamp.MASTERED -> 3
            }
        }?.takeIf { (byId[it.id] ?: CategoryProgress()).stamp != Stamp.MASTERED }
    }
}

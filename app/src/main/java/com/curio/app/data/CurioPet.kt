package com.curio.app.data

import android.content.Context

/**
 * The Curio pet — a tiny pixelated "spark-spirit" companion (spec §10).
 *
 * This object is the pet's BRAIN only: growth stages, moods, and the
 * rule-based dialogue engine ("local AI" — templates driven by app state
 * and the user's own activity stats; no server, no data leaves the device).
 * The pixel sprite, animations and the per-screen presence live in
 * `ui/pet/CurioPetSprite.kt` and the screens that host it.
 *
 * Growth (spec §10.4) is derived entirely from the existing [CurioQuests]
 * state — XP/level, explored lanes and saved captures — so the pet grows
 * from the same XP the quests already award. Moods (spec §10.5) are derived
 * from recent-activity timestamps written by the small hooks in
 * [CurioQuests] ([noteXpEarned] / [noteLevelUp] / [noteLaneExplored]).
 *
 * The whole pet layer is gated by `AppPreferences.petEnabledState`
 * (default ON — see the Appearance settings toggle).
 */
object CurioPet {

    private const val PREFS_NAME = "curio_pet"
    private const val KEY_LAST_XP_AT = "last_xp_at"
    private const val KEY_LAST_LEVEL_AT = "last_level_at"
    private const val KEY_LAST_NEW_LANE_AT = "last_new_lane_at"
    private const val KEY_LAST_BUBBLE_SCREEN = "last_bubble_screen"
    private const val KEY_LAST_BUBBLE_AT = "last_bubble_at"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Growth stages (spec §10.4) ─────────────────────────────────────
    enum class Stage(val displayName: String, val unlockHint: String) {
        HATCHLING("Hatchling Spark", "Level 3 to grow"),
        SPROUT("Curious Sprout", "Explore 3 lanes to grow"),
        TRAIL_BUDDY("Trail Buddy", "Save 10 discoveries to grow"),
        ARCHIVE_PAL("Archive Pal", "Explore every lane to grow"),
        LANE_GUARDIAN("Lane Guardian", "Reach Level 25 to grow"),
        SAGE("Curio Sage", "Fully grown")
    }

    /**
     * The highest growth stage the user's stats satisfy. Checks the top
     * stages first so a late-stage user never "shrinks" back down.
     */
    fun stageFor(xp: Int, saves: Int, lanes: Set<String>, allLaneCount: Int): Stage {
        val level = CurioQuests.levelForXp(xp)
        return when {
            level >= 25 -> Stage.SAGE
            allLaneCount > 0 && lanes.size >= allLaneCount -> Stage.LANE_GUARDIAN
            saves >= 10 -> Stage.ARCHIVE_PAL
            lanes.size >= 3 -> Stage.TRAIL_BUDDY
            level >= 3 -> Stage.SPROUT
            else -> Stage.HATCHLING
        }
    }

    /** Current stage from live [CurioQuests] state. */
    fun currentStage(): Stage =
        stageFor(
            xp = CurioQuests.xpState,
            saves = CurioQuests.lifetimeState.saves,
            lanes = CurioQuests.categoriesState,
            allLaneCount = CurioCategories.visible.size
        )

    /** The next stage up (null when fully grown). */
    fun nextStage(current: Stage): Stage? {
        val order = Stage.entries
        val index = order.indexOf(current)
        return order.getOrNull(index + 1)
    }

    /** A short human line about what's missing to reach the next stage. */
    fun nextStageHint(stage: Stage): String {
        val next = nextStage(stage) ?: return "Fully grown — every lane is yours."
        return when (next) {
            Stage.SPROUT -> "Reach Level 3 to grow."
            Stage.TRAIL_BUDDY -> "Explore ${(3 - CurioQuests.categoriesState.size).coerceAtLeast(0)} more lane(s) to grow."
            Stage.ARCHIVE_PAL -> "Save ${(10 - CurioQuests.lifetimeState.saves).coerceAtLeast(0)} more discovery(ies) to grow."
            Stage.LANE_GUARDIAN -> "Explore every lane to grow."
            Stage.SAGE -> "Reach Level 25 to grow."
            else -> next.unlockHint
        }
    }

    // ── Moods (spec §10.5) — derived from recent activity, never shaming ──
    enum class Mood { PROUD, EXCITED, HAPPY, CURIOUS, SLEEPY }

    fun mood(context: Context, lanes: Set<String>): Mood {
        val now = System.currentTimeMillis()
        return when {
            now - lastLevelUpAt(context) < 90_000L -> Mood.PROUD
            now - lastNewLaneAt(context) < 60_000L -> Mood.EXCITED
            now - lastXpAt(context) < 120_000L -> Mood.HAPPY
            leastExploredLane(lanes) != null -> Mood.CURIOUS
            now - lastXpAt(context) > 12 * 3_600_000L -> Mood.SLEEPY
            else -> Mood.HAPPY
        }
    }

    /**
     * The first visible category the user has never explored — the
     * passport/discovery gap the pet nudges toward (null = every lane seen).
     */
    fun leastExploredLane(explored: Set<String>): CurioCategory? =
        CurioCategories.visible.firstOrNull { it.id.name !in explored }

    // ── Rule-based dialogue ("local AI", spec §10.6/10.7) ──────────────
    // One sentence for passive bubbles; no nagging; never interrupts input.

    /** A passive bubble line for the current [mood]. */
    fun lineFor(context: Context, mood: Mood, lanes: Set<String>): String = when (mood) {
        Mood.PROUD -> "Level ${CurioQuests.levelForXp(CurioQuests.xpState)} — I grew a little!"
        Mood.EXCITED -> leastExploredLane(lanes)?.let { "A fresh lane to wander!" }
            ?: "The deck surprised us both!"
        Mood.HAPPY -> "Nice one — XP banked. Keep going?"
        Mood.CURIOUS -> leastExploredLane(lanes)?.let {
            "We haven't tried ${it.displayName} yet — want a new stamp?"
        } ?: "Spin something new today?"
        Mood.SLEEPY -> "I'll keep your seat warm. Come spin when you're ready."
    }

    /**
     * One bubble per screen visit: returns a line the first time [screen]
     * is composed, then stays quiet for a cooldown (or until tapped).
     */
    fun bubbleFor(context: Context, screen: String, lanes: Set<String>): String? {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        if (p.getString(KEY_LAST_BUBBLE_SCREEN, null) == screen &&
            now - p.getLong(KEY_LAST_BUBBLE_AT, 0L) < 90_000L
        ) {
            return null
        }
        p.edit().putString(KEY_LAST_BUBBLE_SCREEN, screen)
            .putLong(KEY_LAST_BUBBLE_AT, now).apply()
        return lineFor(context, mood(context, lanes), lanes)
    }

    /** What tapping the pet reveals: mood, next quest, growth status. */
    data class TapInfo(
        val mood: Mood,
        val stage: Stage,
        val nextStageLabel: String,
        val nextQuestTitle: String?
    )

    fun tapInfo(context: Context, lanes: Set<String>): TapInfo {
        val stage = currentStage()
        return TapInfo(
            mood = mood(context, lanes),
            stage = stage,
            nextStageLabel = nextStageHint(stage),
            nextQuestTitle = CurioQuests.currentQuest()?.title
        )
    }

    // ── Activity hooks (called from CurioQuests) ───────────────────────
    fun noteXpEarned(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_XP_AT, System.currentTimeMillis()).apply()
    }

    fun noteLevelUp(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_LEVEL_AT, System.currentTimeMillis()).apply()
    }

    fun noteLaneExplored(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_NEW_LANE_AT, System.currentTimeMillis()).apply()
    }

    fun lastXpAt(context: Context): Long = prefs(context).getLong(KEY_LAST_XP_AT, 0L)
    fun lastLevelUpAt(context: Context): Long = prefs(context).getLong(KEY_LAST_LEVEL_AT, 0L)
    fun lastNewLaneAt(context: Context): Long = prefs(context).getLong(KEY_LAST_NEW_LANE_AT, 0L)
}

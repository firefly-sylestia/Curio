package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    // ── Wakefulness (v8.8) — the pet naps in its flower bed when the app
    //    opens and stays asleep until tapped. In-memory per process, so a
    //    fresh launch always finds it snoozing at home (spec §10.3).
    var awake by mutableStateOf(false)
        private set

    /**
     * v8.10 — the pet is SITTING in its flower bed (awake but home): the
     * floating overlay hides and the bed shows it up instead of asleep.
     * The user sends it home with a long-press; tapping the bed brings it
     * back out.
     */
    var atHome by mutableStateOf(false)
        private set

    /**
     * v8.10 — a pet dialog is on screen (the hero check-in). The floating
     * overlay hides so there is never a duplicate pet on screen.
     */
    var dialogOpen by mutableStateOf(false)

    /** Wake the pet (tap on the bed). The floating companion appears. */
    fun wake() {
        awake = true
        atHome = false
    }

    /** Send the pet back to sit in its flower bed (long-press the floater). */
    fun goHome() {
        atHome = true
    }

    /** Bring the pet back out of the bed (tap the bed while it sits there). */
    fun comeOut() {
        atHome = false
    }

    // ── One-shot events (v8.9) — screens bump these on real actions and the
    //    floating pet watches [eventCount] to react (hop + line).
    enum class Event { SPIN_LANDED, REVEAL_OPEN, EXPLORE, SAVE }

    var eventCount by mutableIntStateOf(0)
        private set
    var lastEvent by mutableStateOf<Event?>(null)
        private set

    /** Called by the screens where the action really happens. */
    fun reactTo(event: Event) {
        lastEvent = event
        eventCount++
    }

    /** A short, cute line for the pet's reaction to [event]. */
    fun eventLine(event: Event): String = when (event) {
        Event.SPIN_LANDED -> listOf(
            "It landed!", "Ooh — the deck chose well!", "A new topic, a new tale!"
        ).random()
        Event.REVEAL_OPEN -> listOf(
            "Open it, open it!", "A mystery awaits!", "Peek inside!"
        ).random()
        Event.EXPLORE -> listOf(
            "Go explore!", "Adventure time!", "I'll wait right here — go see!"
        ).random()
        Event.SAVE -> listOf(
            "Keepsake saved!", "Mine now… I mean, ours!", "Tucked away safely!"
        ).random()
    }

    /** Send the pet back to bed after a long idle — the bed shows it asleep. */
    fun settleToSleep() {
        awake = false
        atHome = false
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

    // ── Passive dialogue (v8.8 — a little variety per mood) ────────────
    // `__LANE__` is swapped for the least-explored lane's name at show time.
    private val excitedLines = listOf(
        "Ooh! A fresh lane to wander!",
        "The deck surprised us both!",
        "Wheee — new ground!"
    )
    private val happyLines = listOf(
        "Nice one — XP banked. Keep going?",
        "That was fun. More?",
        "Curiosity looks good on you."
    )
    private val curiousLines = listOf(
        "We haven't tried __LANE__ yet — want a new stamp?",
        "I wonder what __LANE__ hides…",
        "Pssst — __LANE__ is calling."
    )
    private val sleepyLines = listOf(
        "I'll keep your seat warm. Come spin when you're ready.",
        "Yawn… the deck can wait a moment.",
        "Soft blanket, warm lamp… I'm ready when you are."
    )

    /** A passive bubble line for the current [mood]. */
    fun lineFor(context: Context, mood: Mood, lanes: Set<String>): String = when (mood) {
        // Inlined so the level reads live, not baked at first access.
        Mood.PROUD -> listOf(
            "Level ${CurioQuests.levelForXp(CurioQuests.xpState)} — I grew a little!",
            "Shiny! We leveled up together.",
            "Do you feel that? That's growth!"
        ).random()
        Mood.EXCITED -> excitedLines.random()
        Mood.HAPPY -> happyLines.random()
        Mood.CURIOUS -> {
            val lane = leastExploredLane(lanes)
            if (lane != null) {
                curiousLines.map { it.replace("__LANE__", lane.displayName) }.random()
            } else {
                "Spin something new today?"
            }
        }
        Mood.SLEEPY -> sleepyLines.random()
    }

    /** A short burst when the user touches/pets the floating pet. */
    fun touchReaction(): String = listOf(
        "Hehe!", "Boop!", "Wheee!", "Ooh!", "Again, again!", "That tickles!"
    ).random()

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

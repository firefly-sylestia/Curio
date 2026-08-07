package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Curio's quests & levels system (v8.0).
 *
 * A light gamification layer that runs ALWAYS-ON (no settings toggle):
 *
 *  - **XP & levels** — every curious act earns XP (spin +2, explore +5,
 *    save +10, pin/quote +3, like +2…). XP is cumulative; the level curve
 *    climbs from "First Spark" through 50 ranks to "Curio Sovereign".
 *  - **Quest chains** — every quest lives in a progressive CHAIN: related
 *    goals are grouped (The Deck = spin 1 → 5 → 25 → 100; Keepsakes =
 *    save 1 → 5 → 25 → 100 → every format…). A chain's stages complete in
 *    order — when one is done the NEXT one becomes the current quest, so
 *    the UI always points at exactly one thing to do. Chains replace the
 *    old flat journey list, daily pool display, and achievement shelf:
 *    every stage is a badge, every badge lives in a chain.
 *  - **The Tour** — a guided walkthrough chain (Settings → Profile →
 *    pin → quote → daily → badge) with routes to jump straight to each
 *    screen; the in-app guide overlay drives this chain, and tapping the
 *    very first quest launches the full auto-navigating quest tour
 *    (see [QuestGuide]).
 *  - **Daily quests** — three quests picked per day from a rotating pool
 *    (seeded by the calendar day, stable all day, resets at midnight).
 *
 * Persistence mirrors the other data objects: one SharedPreferences file
 * (`curio_quests`, listed in [CurioBackupManager] so it ships with the
 * user's backup) with JSON values, plus reactive Compose state seeded by
 * [seed] from MainActivity. All event hooks are fire-and-forget and
 * cheap — they may be called from any thread. Legacy journey/achievement
 * awards migrate into their chain-stage ids on first seed.
 */
object CurioQuests {

    private const val PREFS_NAME = "curio_quests"

    private const val KEY_XP = "xp"
    private const val KEY_LIFETIME = "lifetime"
    private const val KEY_FORMATS = "formats"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_STAGES_AWARDED = "stages_awarded"
    private const val KEY_DAILY_DATE = "daily_date"
    private const val KEY_DAILY_PROGRESS = "daily_progress"
    private const val KEY_DAILY_AWARDED = "daily_awarded"
    private const val KEY_BEST_STREAK = "best_streak"
    // Legacy keys — read once during migration, never written again.
    private const val KEY_LEGACY_JOURNEY_AWARDED = "journey_awarded"
    private const val KEY_LEGACY_ACHIEVEMENTS = "achievements"

    // ── Reactive state (seeded by [seed], kept in sync by every hook) ──
    var xpState by mutableIntStateOf(0)
        private set
    var lifetimeState by mutableStateOf(LifetimeCounters())
        private set
    var formatsState by mutableStateOf<Set<String>>(emptySet())
        private set
    var categoriesState by mutableStateOf<Set<String>>(emptySet())
        private set
    var awardedStagesState by mutableStateOf<Set<String>>(emptySet())
        private set
    var dailyDateState by mutableIntStateOf(-1)
        private set
    var dailyProgressState by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var dailyAwardedState by mutableStateOf<Set<String>>(emptySet())
        private set
    var bestStreakState by mutableIntStateOf(0)
        private set

    /** One-time cumulative counters that drive chain progress. */
    data class LifetimeCounters(
        val spins: Int = 0,
        val explores: Int = 0,
        val saves: Int = 0,
        val quotes: Int = 0,
        val pins: Int = 0,
        val likes: Int = 0,
        val dislikes: Int = 0,
        val profileVisits: Int = 0,
        val settingsVisits: Int = 0,
        val dailyCompleted: Int = 0
    )

    // ── Level curve — cumulative XP needed to REACH each level (50) ────
    // The first 12 thresholds keep the v7.40 pacing (a new user sees Level 2
    // within a couple of actions); beyond that the per-level cost rises
    // GENTLY (+8/level) until a 240 XP ceiling, so the deep ranks stay a
    // long-term goal without turning into a grind — the v7.106 step kept
    // climbing to a 360 XP cap (~12.1k total), which still made the high
    // ranks feel frozen. Total 50 levels, top at ~8.6k XP.
    private val XP_THRESHOLDS: List<Int> = buildList {
        addAll(listOf(0, 15, 40, 80, 135, 205, 290, 390, 505, 635, 780, 940))
        var xp = 940
        var step = 90
        while (size < 50) {
            step = (step + 8).coerceAtMost(240)
            xp += step
            add(xp)
        }
    }

    private val LEVEL_TITLES = listOf(
        "First Spark", "Curious Newcomer", "Tuned Ear", "Pattern Spotter", "Comparator",
        "Synthesizer", "Curator", "Master Curator", "Lore Keeper", "Lane Walker",
        "Archive Scholar", "Grand Curator",
        // 13–20
        "Wandering Scholar", "Lantern Bearer", "Tome Walker", "Index Dreamer",
        "Shelf Maven", "Margin Dweller", "Footnote Follower", "Chapter Scout",
        // 21–28
        "Card Cataloguer", "Stack Surfer", "Dust Jacket Dancer", "Bookplate Baron",
        "Archive Acolyte", "Footnote Philosopher", "Marginalia Monk", "Deck Diviner",
        // 29–36
        "Lane Luminator", "Curiosity Conductor", "Wonder Warden", "Spark Shepherd",
        "Knowledge Keeper", "Scribe of Secrets", "Vault Walker", "Atlas Aficionado",
        // 37–44
        "Chronology Keeper", "Labyrinth Listener", "Riddle Reader", "Insight Oracle",
        "Truth Tender", "Wisdom Weaver", "Grand Gatherer", "Lore Lord",
        // 45–50
        "Myth Keeper", "Archive Sovereign", "Eternal Student", "Curio Champion",
        "Curio Legend", "Curio Sovereign"
    )

    fun levelForXp(xp: Int): Int {
        var level = 1
        XP_THRESHOLDS.forEachIndexed { index, threshold -> if (xp >= threshold) level = index + 1 }
        return level.coerceIn(1, XP_THRESHOLDS.size)
    }

    /** Fraction toward the next level (1f at max) + the next threshold's XP. */
    fun xpProgress(xp: Int): Pair<Float, Int> {
        val level = levelForXp(xp)
        val lastIndex = XP_THRESHOLDS.lastIndex
        if (level >= XP_THRESHOLDS.size) return 1f to XP_THRESHOLDS[lastIndex]
        val from = XP_THRESHOLDS[level - 1]
        val to = XP_THRESHOLDS[level]
        return ((xp - from).toFloat() / (to - from).coerceAtLeast(1)) to to
    }

    fun levelTitle(level: Int): String =
        LEVEL_TITLES.getOrElse(level - 1) { LEVEL_TITLES.last() }

    /** The highest achievable level — UI uses this for the max-level state. */
    val maxLevel: Int get() = XP_THRESHOLDS.size

    // ── Quest chains — progressive stages; complete one, the next shows ──
    enum class QuestKind {
        SPIN, EXPLORE, SAVE, SETTINGS, PROFILE, PIN, QUOTE, DAILY, ACHIEVEMENT,
        STREAK, LIKE, FORMATS, LANES, XP
    }

    data class QuestStage(
        val id: String,
        val title: String,
        val description: String,
        val hint: String,
        val xpReward: Int,
        val kind: QuestKind,
        val target: Int,
        /** Optional route to jump straight to the quest's screen. */
        val navRoute: String? = null
    )

    data class QuestChain(
        val id: String,
        val glyph: String,
        val title: String,
        val subtitle: String,
        val stages: List<QuestStage>
    )

    val Chains: List<QuestChain> = listOf(
        // ── The Deck — spin chain (leads the guide: a new user's first
        //    quest is "First Spin", which jumps straight to the Spin tab) ──
        QuestChain(
            id = "deck", glyph = "casino", title = "The Deck", subtitle = "Spin your way up the ranks",
            stages = listOf(
                QuestStage("deck-1", "First Spin", "Spin the deck once", "Tap the Shuffle button on the Spin tab.", 10, QuestKind.SPIN, 1, navRoute = "spin"),
                QuestStage("deck-3", "Warming Up", "Spin 3 times", "Three shuffles — the deck is getting to know you.", 5, QuestKind.SPIN, 3),
                QuestStage("deck-5", "Deck Regular", "Spin 5 times", "Keep shuffling — the deck resets each spin.", 15, QuestKind.SPIN, 5),
                QuestStage("deck-10", "Deck Habit", "Spin 10 times", "Double digits — a proper habit forming.", 10, QuestKind.SPIN, 10),
                QuestStage("deck-25", "Deck Master", "Spin 25 times", "A quarter of a century of spins.", 25, QuestKind.SPIN, 25),
                QuestStage("deck-50", "Deck Virtuoso", "Spin 50 times", "Halfway to legend.", 20, QuestKind.SPIN, 50),
                QuestStage("deck-100", "Deck Legend", "Spin 100 times", "The deck knows your name now.", 40, QuestKind.SPIN, 100)
            )
        ),
        // ── Discovery — explore chain ──────────────────────────────────
        QuestChain(
            id = "discovery", glyph = "explore", title = "Discovery", subtitle = "Go find things in the world",
            stages = listOf(
                QuestStage("disc-1", "First Discovery", "Explore your first topic", "Open a topic and tap Explore.", 10, QuestKind.EXPLORE, 1),
                QuestStage("disc-3", "Three Steps Out", "Explore 3 topics", "Three deep dives — the itch is real.", 5, QuestKind.EXPLORE, 3),
                QuestStage("disc-5", "Globe Trotter", "Explore 5 topics", "Five deep dives under your belt.", 15, QuestKind.EXPLORE, 5),
                QuestStage("disc-10", "Trail Maker", "Explore 10 topics", "A trail of your own making.", 10, QuestKind.EXPLORE, 10),
                QuestStage("disc-25", "Pathfinder", "Explore 25 topics", "A quarter century of exploration.", 25, QuestKind.EXPLORE, 25),
                QuestStage("disc-lane3", "Lane Hopper", "Explore in 3 lanes", "Sample three different categories.", 10, QuestKind.LANES, 3),
                QuestStage("disc-lanes", "All Lanes", "Explore in every lane", "Try every category at least once.", 30, QuestKind.LANES, CurioCategories.visible.size)
            )
        ),
        // ── Keepsakes — save chain ─────────────────────────────────────
        QuestChain(
            id = "keepsakes", glyph = "inventory_2", title = "Keepsakes", subtitle = "Fill your Cabinet",
            stages = listOf(
                QuestStage("keep-1", "First Keepsake", "Save your first capture", "Write down what you found.", 10, QuestKind.SAVE, 1),
                QuestStage("keep-3", "Souvenir Seeker", "Save 3 captures", "Three keepsakes — the shelf starts to fill.", 5, QuestKind.SAVE, 3),
                QuestStage("keep-5", "Notebook Keeper", "Save 5 captures", "Five keepsakes in the Cabinet.", 15, QuestKind.SAVE, 5),
                QuestStage("keep-10", "Memory Keeper", "Save 10 captures", "A neat row of remembered moments.", 10, QuestKind.SAVE, 10),
                QuestStage("keep-25", "Archivist", "Save 25 captures", "A growing archive of curiosity.", 25, QuestKind.SAVE, 25),
                QuestStage("keep-50", "Archive Curator", "Save 50 captures", "Your Cabinet has a mind of its own now.", 20, QuestKind.SAVE, 50),
                QuestStage("keep-100", "Librarian of Lanes", "Save 100 captures", "A hundred moments, preserved.", 35, QuestKind.SAVE, 100),
                QuestStage("keep-formats", "Every Format", "Save one capture in every format", "Notes, sound bites, galleries — the full kit.", 30, QuestKind.FORMATS, CaptureFormat.entries.size)
            )
        ),
        // ── The Tour — the guided walkthrough (drives the auto-guide) ──
        QuestChain(
            id = "tour", glyph = "flag", title = "The Tour", subtitle = "A guided walk through Curio",
            stages = listOf(
                QuestStage(
                    id = "tour-settings", title = "Look around Settings",
                    description = "Appearance, reminders, recording, backup — make Curio yours.",
                    hint = "Open Settings and browse the sections.",
                    xpReward = 10, kind = QuestKind.SETTINGS, target = 1, navRoute = "settings"
                ),
                QuestStage(
                    id = "tour-profile", title = "Visit your profile",
                    description = "See your stats, level, and lanes.",
                    hint = "Open Profile from Home's avatar pill.",
                    xpReward = 10, kind = QuestKind.PROFILE, target = 1, navRoute = "profile"
                ),
                QuestStage(
                    id = "tour-pin", title = "Pin a topic for later",
                    description = "Bookmark a topic you want to come back to.",
                    hint = "On any topic reveal, tap the pin button.",
                    xpReward = 10, kind = QuestKind.PIN, target = 1
                ),
                QuestStage(
                    id = "tour-quote", title = "Bookmark a quote",
                    description = "Save a line from a capture to your Saved shelf.",
                    hint = "Open a saved capture and tap the bookmark on a quote.",
                    xpReward = 10, kind = QuestKind.QUOTE, target = 1
                ),
                QuestStage(
                    id = "tour-daily", title = "Complete a daily quest",
                    description = "Finish one of today's three quests.",
                    hint = "Check Today's quests below and knock one out.",
                    xpReward = 15, kind = QuestKind.DAILY, target = 1
                ),
                QuestStage(
                    id = "tour-achievement", title = "Unlock a badge",
                    description = "Earn any badge from the quest chains.",
                    hint = "Every stage you finish unlocks its badge.",
                    xpReward = 25, kind = QuestKind.ACHIEVEMENT, target = 1
                )
            )
        ),
        // ── The Shelf — quote chain ────────────────────────────────────
        QuestChain(
            id = "shelf", glyph = "format_quote", title = "The Shelf", subtitle = "Save the lines you love",
            stages = listOf(
                QuestStage("quote-1", "Quote Collector", "Bookmark your first quote", "Tap the bookmark on any quote.", 10, QuestKind.QUOTE, 1),
                QuestStage("quote-3", "Quote Keeper", "Bookmark 3 quotes", "Three lines worth keeping close.", 5, QuestKind.QUOTE, 3),
                QuestStage("quote-5", "Quote Hoarder", "Bookmark 5 quotes", "Five lines worth keeping.", 15, QuestKind.QUOTE, 5)
            )
        ),
        // ── Pin Board — pin chain ──────────────────────────────────────
        QuestChain(
            id = "pinboard", glyph = "bookmark", title = "Pin Board", subtitle = "Keep topics at hand",
            stages = listOf(
                QuestStage("pin-1", "Pin Cushion", "Pin your first topic", "Tap the pin on any topic reveal.", 10, QuestKind.PIN, 1),
                QuestStage("pin-3", "Pin Collector", "Pin 3 topics", "Three pins on the board.", 5, QuestKind.PIN, 3),
                QuestStage("pin-5", "Pin Board", "Pin 5 topics", "Five topics pinned for later.", 15, QuestKind.PIN, 5)
            )
        ),
        // ── The Flame — streak chain ───────────────────────────────────
        QuestChain(
            id = "flame", glyph = "local_fire_department", title = "The Flame", subtitle = "Keep the streak alive",
            stages = listOf(
                QuestStage("flame-1", "First Warmth", "Keep a 1-day streak", "Come back tomorrow and the flame stays lit.", 5, QuestKind.STREAK, 1),
                QuestStage("flame-3", "Spark Streak", "Keep a 3-day streak", "Come back tomorrow, and the day after.", 15, QuestKind.STREAK, 3),
                QuestStage("flame-7", "Week of Wonder", "Keep a 7-day streak", "A full week of daily curiosity.", 25, QuestKind.STREAK, 7),
                QuestStage("flame-14", "Fortnight Flame", "Keep a 14-day streak", "Two weeks of steady wonder.", 20, QuestKind.STREAK, 14),
                QuestStage("flame-30", "Month of Mystery", "Keep a 30-day streak", "Thirty days of wonder.", 40, QuestKind.STREAK, 30)
            )
        ),
        // ── Taste — like chain ─────────────────────────────────────────
        QuestChain(
            id = "taste", glyph = "thumb_up", title = "Taste", subtitle = "Trust your instincts",
            stages = listOf(
                QuestStage("like-1", "First Nod", "Like your first topic", "Tap the heart on a topic you love.", 10, QuestKind.LIKE, 1),
                QuestStage("like-3", "Taste Maker", "Like 3 topics", "Three topics you'd defend.", 5, QuestKind.LIKE, 3),
                QuestStage("like-10", "Curator's Taste", "Like 10 topics", "Ten topics you'd defend in public.", 20, QuestKind.LIKE, 10)
            )
        ),
        // ── The Ladder — rank chain (level milestones) ─────────────────
        QuestChain(
            id = "rank", glyph = "workspace_premium", title = "The Ladder", subtitle = "Climb the 50 ranks",
            stages = listOf(
                QuestStage("rank-5", "Five Rungs Up", "Reach Level 5", "Earn XP from any curious act.", 15, QuestKind.XP, XP_THRESHOLDS[4]),
                QuestStage("rank-10", "Lore Keeper", "Reach Level 10", "Keep exploring, saving, and spinning.", 25, QuestKind.XP, XP_THRESHOLDS[9]),
                QuestStage("rank-20", "Archive Scholar", "Reach Level 20", "Halfway up the low ranks.", 40, QuestKind.XP, XP_THRESHOLDS[19]),
                QuestStage("rank-30", "Wonder Warden", "Reach Level 30", "The middle ranks bow to you.", 60, QuestKind.XP, XP_THRESHOLDS[29]),
                QuestStage("rank-40", "Insight Oracle", "Reach Level 40", "Few reach the high shelves.", 80, QuestKind.XP, XP_THRESHOLDS[39]),
                QuestStage("rank-50", "Curio Sovereign", "Reach Level 50", "The whole shelf is yours.", 120, QuestKind.XP, XP_THRESHOLDS[49])
            )
        )
    )

    // ── Legacy id migration — old journey quests + achievement badges ──
    // map onto their chain-stage successors so existing users keep their
    // awards (and their XP is not paid twice).
    private val LEGACY_STAGE_MAP = mapOf(
        "take-the-wheel" to "deck-1", "first-look" to "disc-1", "first-keepsake" to "keep-1",
        "settle-in" to "tour-settings", "raise-the-flag" to "tour-profile", "pin-it" to "pin-1",
        "collect-a-thought" to "quote-1", "daily-driver" to "tour-daily", "five-keepsakes" to "keep-5",
        "badge-of-honor" to "tour-achievement",
        "spin-1" to "deck-1", "spin-25" to "deck-5", "spin-100" to "deck-25",
        "explore-1" to "disc-1", "explore-25" to "disc-5", "lanes-all" to "disc-lanes",
        "save-1" to "keep-1", "save-25" to "keep-5", "save-100" to "keep-25", "formats-all" to "keep-formats",
        "quote-5" to "quote-5", "pin-5" to "pin-5",
        "streak-3" to "flame-3", "streak-7" to "flame-7", "streak-30" to "flame-30",
        "like-10" to "like-10", "journey-done" to "tour-achievement", "xp-505" to "rank-10"
    )

    // ── Daily quests — three per day, one of each ROLE (spec §5.1) ──────
    //    Warm-up (easy one-action), discovery (a new lane via the passport),
    //    creation (save/reflect). DISCOVERY completes when the passport's
    //    least-engaged lane is explored (spec §6.2).
    enum class DailyKind { SPIN, EXPLORE, SAVE, QUOTE, PIN, PROFILE, LIKE, DISCOVERY }

    data class DailyQuest(
        val id: String,
        val title: String,
        val xpReward: Int,
        val kind: DailyKind,
        val target: Int
    )

    private val DailyPool: List<DailyQuest> = listOf(
        DailyQuest("d-spin-1", "Spin the deck once", 10, DailyKind.SPIN, 1),
        DailyQuest("d-spin-3", "Spin the deck 3 times", 15, DailyKind.SPIN, 3),
        DailyQuest("d-explore-1", "Explore a topic", 15, DailyKind.EXPLORE, 1),
        DailyQuest("d-save-1", "Save a capture", 15, DailyKind.SAVE, 1),
        DailyQuest("d-quote-1", "Bookmark a quote", 10, DailyKind.QUOTE, 1),
        DailyQuest("d-pin-1", "Pin a topic for later", 10, DailyKind.PIN, 1),
        DailyQuest("d-profile-1", "Visit your profile", 10, DailyKind.PROFILE, 1),
        DailyQuest("d-like-1", "Like a topic", 10, DailyKind.LIKE, 1),
        // Discovery role — "New Lane": explore the passport's least-engaged
        // lane (its stamp becomes EXPLORED). The UI titles it with the lane
        // name and routes its CTA to that lane's Spin deck (spec §6.3).
        DailyQuest("d-lane-1", "Try a new lane", 20, DailyKind.DISCOVERY, 1)
    )

    /**
     * The three quests live for [epochDay] — stable all day, new at midnight.
     * Role diversity (spec §5.1): one warm-up + the discovery quest + one
     * creation quest. When [context] is given and every lane is already
     * mastered there is nothing left to discover — the discovery slot is
     * replaced by a second creation quest instead.
     */
    fun dailyQuestsFor(epochDay: Long, context: Context? = null): List<DailyQuest> {
        val warmups = DailyPool.filter {
            it.kind == DailyKind.SPIN || it.kind == DailyKind.EXPLORE || it.kind == DailyKind.PROFILE
        }
        val creations = DailyPool.filter {
            it.kind == DailyKind.SAVE || it.kind == DailyKind.QUOTE ||
                it.kind == DailyKind.PIN || it.kind == DailyKind.LIKE
        }
        val discovery = DailyPool.firstOrNull { it.kind == DailyKind.DISCOVERY }
        fun pick(list: List<DailyQuest>): DailyQuest {
            val base = (epochDay % list.size).toInt()
            return list[if (base < 0) base + list.size else base]
        }
        val hasDiscoveryTarget = context == null || CurioPassport.leastEngaged(context) != null
        return if (discovery != null && hasDiscoveryTarget) {
            listOf(pick(warmups), discovery, pick(creations))
        } else {
            val c0 = pick(creations)
            val c1 = creations[(creations.indexOf(c0) + 1) % creations.size]
            listOf(pick(warmups), c0, c1)
        }
    }

    // ── Seed / persistence ──────────────────────────────────────────────

    /** Load all persisted state (called once from MainActivity onCreate). */
    fun seed(context: Context) {
        val prefs = prefs(context)
        xpState = prefs.getInt(KEY_XP, 0)
        lifetimeState = readLifetime(prefs.getString(KEY_LIFETIME, null))
        formatsState = readStringSet(prefs.getString(KEY_FORMATS, null))
        categoriesState = readStringSet(prefs.getString(KEY_CATEGORIES, null))
        // Chain-stage awards: new key, with legacy journey/achievement sets
        // migrated into their successor stage ids.
        val awarded = readStringSet(prefs.getString(KEY_STAGES_AWARDED, null)).toMutableSet()
        listOf(KEY_LEGACY_JOURNEY_AWARDED, KEY_LEGACY_ACHIEVEMENTS).forEach { legacyKey ->
            readStringSet(prefs.getString(legacyKey, null)).forEach { old ->
                LEGACY_STAGE_MAP[old]?.let(awarded::add)
            }
        }
        awardedStagesState = awarded
        dailyDateState = prefs.getInt(KEY_DAILY_DATE, -1)
        dailyProgressState = readIntMap(prefs.getString(KEY_DAILY_PROGRESS, null))
        dailyAwardedState = readStringSet(prefs.getString(KEY_DAILY_AWARDED, null))
        bestStreakState = prefs.getInt(KEY_BEST_STREAK, 0)
        ensureDaily(context)
        // Re-award any stage the counters already satisfy (post-migration
        // catch-up, also heals a killed write).
        checkAll(context)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readLifetime(raw: String?): LifetimeCounters {
        if (raw == null) return LifetimeCounters()
        return try {
            val o = JSONObject(raw)
            LifetimeCounters(
                spins = o.optInt("spins"),
                explores = o.optInt("explores"),
                saves = o.optInt("saves"),
                quotes = o.optInt("quotes"),
                pins = o.optInt("pins"),
                likes = o.optInt("likes"),
                dislikes = o.optInt("dislikes"),
                profileVisits = o.optInt("profileVisits"),
                settingsVisits = o.optInt("settingsVisits"),
                dailyCompleted = o.optInt("dailyCompleted")
            )
        } catch (_: Exception) {
            LifetimeCounters()
        }
    }

    private fun readStringSet(raw: String?): Set<String> {
        if (raw == null) return emptySet()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun readIntMap(raw: String?): Map<String, Int> {
        if (raw == null) return emptyMap()
        return try {
            val o = JSONObject(raw)
            buildMap { o.keys().forEach { key -> put(key, o.optInt(key)) } }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun write(context: Context) {
        val counters = lifetimeState
        val lifetime = JSONObject()
            .put("spins", counters.spins)
            .put("explores", counters.explores)
            .put("saves", counters.saves)
            .put("quotes", counters.quotes)
            .put("pins", counters.pins)
            .put("likes", counters.likes)
            .put("dislikes", counters.dislikes)
            .put("profileVisits", counters.profileVisits)
            .put("settingsVisits", counters.settingsVisits)
            .put("dailyCompleted", counters.dailyCompleted)
        val dailyProgress = JSONObject()
        dailyProgressState.forEach { (k, v) -> dailyProgress.put(k, v) }
        prefs(context).edit()
            .putInt(KEY_XP, xpState)
            .putString(KEY_LIFETIME, lifetime.toString())
            .putString(KEY_FORMATS, JSONArray(formatsState.toList()).toString())
            .putString(KEY_CATEGORIES, JSONArray(categoriesState.toList()).toString())
            .putString(KEY_STAGES_AWARDED, JSONArray(awardedStagesState.toList()).toString())
            .putInt(KEY_DAILY_DATE, dailyDateState)
            .putString(KEY_DAILY_PROGRESS, dailyProgress.toString())
            .putString(KEY_DAILY_AWARDED, JSONArray(dailyAwardedState.toList()).toString())
            .putInt(KEY_BEST_STREAK, bestStreakState)
            .apply()
    }

    // ── Daily rollover — a new calendar day resets quest progress ──────
    private fun ensureDaily(context: Context) {
        val today = todayEpochDay().toInt()
        if (dailyDateState == today) return
        dailyDateState = today
        dailyProgressState = emptyMap()
        dailyAwardedState = emptySet()
        write(context)
    }

    /** Today's epoch day (midnight-normalized) — the daily reset key. */
    fun todayEpochDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 86_400_000L
    }

    // ── XP ───────────────────────────────────────────────────────────────
    private fun addXp(context: Context, amount: Int) {
        val levelBefore = levelForXp(xpState)
        xpState += amount
        val levelAfter = levelForXp(xpState)
        write(context)
        checkAll(context)
        // Feed the Curio pet's mood timestamps (spec §10.5): a level-up is
        // the proud moment; any positive XP is a happy one. (The pet only
        // reacts to REAL XP — the 0-XP refresh calls stay quiet.)
        if (amount > 0) {
            if (levelAfter > levelBefore) CurioPet.noteLevelUp(context)
            else CurioPet.noteXpEarned(context)
        }
    }

    // ── Event hooks — the app calls these where real actions happen ────

    /**
     * A spin landed in [categoryId] (SpinScreen). [categoryId] is the lane
     * the user actually spun — WILDCARD when they used the surprise deck.
     */
    fun onSpin(context: Context, categoryId: CategoryId) {
        ensureDaily(context)
        lifetimeState = lifetimeState.copy(spins = lifetimeState.spins + 1)
        bumpDaily(context, DailyKind.SPIN)
        // v8.10 — spinning the discovery daily's target lane completes it at
        // the spin itself (its "Go" chip routes into that lane's deck). This
        // is the ONLY path a Wildcard-targeted discovery can ever complete:
        // Wildcard is a merge, so every wildcard spin lands on a real
        // category and no reveal/explore reports WILDCARD back — there is no
        // proper wildcard topic page to open. Real lanes keep the explore
        // path in [onExplore] too, so opening the topic still counts.
        val discoveryTarget = CurioPassport.leastEngaged(context)
        if (discoveryTarget != null && categoryId == discoveryTarget.id) {
            bumpDaily(context, DailyKind.DISCOVERY)
        }
        write(context)
        addXp(context, 2)
        // The tour's Spin step advances the moment the user actually spins.
        QuestGuide.onWait(QuestGuide.Wait.SPIN)
    }

    /** The user started exploring a topic (ExploreSessionStore.recordExplored). */
    fun onExplore(context: Context, categoryId: CategoryId) {
        ensureDaily(context)
        val isNewLane = categoryId.name !in categoriesState
        lifetimeState = lifetimeState.copy(explores = lifetimeState.explores + 1)
        categoriesState = categoriesState + categoryId.name
        bumpDaily(context, DailyKind.EXPLORE)
        // v8.6 — the "New Lane" discovery daily (spec §6.2) completes when the
        // passport's least-engaged lane is explored.
        val discoveryTarget = CurioPassport.leastEngaged(context)
        if (discoveryTarget != null && categoryId == discoveryTarget.id) {
            bumpDaily(context, DailyKind.DISCOVERY)
        }
        write(context)
        addXp(context, 5)
        // The pet gets excited the first time a lane is explored (spec §10.5),
        // and always hops when the user starts exploring (spec §10.6 event hook).
        if (isNewLane) CurioPet.noteLaneExplored(context)
        CurioPet.reactTo(CurioPet.Event.EXPLORE)
        // Feed the category passport — an explore advances the lane's stamp
        // toward EXPLORED and refreshes its last-explored date (spec §6.1).
        CurioPassport.noteExplore(context, categoryId)
        // The tour's Explore step advances when a topic is explored.
        QuestGuide.onWait(QuestGuide.Wait.EXPLORE)
    }

    /** A capture was saved (SaveCaptureScreen). [format] feeds the Every-Format stage. */
    fun onSave(context: Context, format: CaptureFormat) {
        ensureDaily(context)
        lifetimeState = lifetimeState.copy(saves = lifetimeState.saves + 1)
        formatsState = formatsState + format.name
        bumpDaily(context, DailyKind.SAVE)
        write(context)
        addXp(context, 10)
        // The tour's Save step advances when a capture is saved.
        QuestGuide.onWait(QuestGuide.Wait.SAVE)
    }

    /** A quote was bookmarked (AppPreferences.saveQuote). */
    fun onQuoteSaved(context: Context) {
        ensureDaily(context)
        lifetimeState = lifetimeState.copy(quotes = lifetimeState.quotes + 1)
        bumpDaily(context, DailyKind.QUOTE)
        write(context)
        addXp(context, 3)
    }

    /** A topic was pinned (AppPreferences.pinTopic). */
    fun onTopicPinned(context: Context) {
        ensureDaily(context)
        lifetimeState = lifetimeState.copy(pins = lifetimeState.pins + 1)
        bumpDaily(context, DailyKind.PIN)
        write(context)
        addXp(context, 3)
    }

    /** A topic was liked (AppPreferences.setTopicSentiment). */
    fun onTopicLiked(context: Context) {
        ensureDaily(context)
        lifetimeState = lifetimeState.copy(likes = lifetimeState.likes + 1)
        bumpDaily(context, DailyKind.LIKE)
        write(context)
        addXp(context, 2)
    }

    /** A topic was disliked (AppPreferences.setTopicSentiment). */
    fun onTopicDisliked(context: Context) {
        ensureDaily(context)
        lifetimeState = lifetimeState.copy(dislikes = lifetimeState.dislikes + 1)
        write(context)
        addXp(context, 1)
    }

    /** Profile opened (ProfileScreen) — counts for the tour + daily quests. */
    fun onProfileVisited(context: Context) {
        ensureDaily(context)
        lifetimeState = lifetimeState.copy(profileVisits = lifetimeState.profileVisits + 1)
        bumpDaily(context, DailyKind.PROFILE)
        write(context)
        // 0 XP — the call is just a refresh so the tour chain checks run.
        addXp(context, 0)
        // The tour's Profile step advances when the profile is opened.
        QuestGuide.onWait(QuestGuide.Wait.PROFILE)
    }

    /** Settings opened (SettingsHubScreen) — counts for the tour quest. */
    fun onSettingsVisited(context: Context) {
        ensureDaily(context)
        lifetimeState = lifetimeState.copy(settingsVisits = lifetimeState.settingsVisits + 1)
        write(context)
        // 0 XP — the call is just a refresh so the tour chain checks run.
        addXp(context, 0)
        // The tour's Settings step advances when Settings is opened.
        QuestGuide.onWait(QuestGuide.Wait.SETTINGS)
    }

    /** Streak advanced (StreakTracker.recordActivity) — feeds streak stages. */
    fun onStreakRecorded(context: Context, streak: Int) {
        if (streak > bestStreakState) {
            bestStreakState = streak
            write(context)
        }
        checkAll(context)
    }

    // ── Daily quest progress (XP is CLAIMED on the Quests page — v8.3) ──
    private fun bumpDaily(context: Context, kind: DailyKind) {
        val today = todayEpochDay().toInt()
        if (dailyDateState != today) {
            dailyDateState = today
            dailyProgressState = emptyMap()
            dailyAwardedState = emptySet()
        }
        val key = kind.name
        val current = dailyProgressState[key] ?: 0
        dailyProgressState = dailyProgressState + (key to (current + 1))
        // v8.3 — progress is tracked but XP is NOT granted here: each daily
        // quest's reward is claimed explicitly on the Quests page
        // ([claimDaily]), so the +XP is a deliberate tap, not a silent grant.
        // (The event hooks persist via write(context) right after this.)
    }

    /**
     * Claim a completed daily quest's XP (Quests page "Claim" button, v8.3).
     * No-op when the quest isn't complete, was already claimed today, or
     * doesn't belong to today's set.
     */
    fun claimDaily(context: Context, questId: String) {
        ensureDaily(context)
        val quest = dailyQuestsFor(todayEpochDay(), context).firstOrNull { it.id == questId } ?: return
        if (quest.id in dailyAwardedState) return
        if ((dailyProgressState[quest.kind.name] ?: 0) < quest.target) return
        dailyAwardedState = dailyAwardedState + quest.id
        lifetimeState = lifetimeState.copy(dailyCompleted = lifetimeState.dailyCompleted + 1)
        write(context)
        addXp(context, quest.xpReward)
    }

    // ── Chain checks — award each stage's XP once when its target hits ──
    private fun checkAll(context: Context) {
        var changed = true
        var awarded = false
        while (changed) {
            changed = false
            allStages().forEach { stage ->
                if (stage.id !in awardedStagesState && stageProgress(stage) >= stage.target) {
                    awardedStagesState = awardedStagesState + stage.id
                    xpState += stage.xpReward
                    changed = true
                    awarded = true
                }
            }
        }
        // Only persist when something changed — hooks call this on every tap.
        if (awarded) write(context)
    }

    /** Every stage across every chain, in display order. */
    fun allStages(): List<QuestStage> = Chains.flatMap { it.stages }

    /** Live progress for one stage, derived from the lifetime counters. */
    fun stageProgress(stage: QuestStage): Int = when (stage.kind) {
        QuestKind.SPIN -> lifetimeState.spins
        QuestKind.EXPLORE -> lifetimeState.explores
        QuestKind.SAVE -> lifetimeState.saves
        QuestKind.SETTINGS -> lifetimeState.settingsVisits
        QuestKind.PROFILE -> lifetimeState.profileVisits
        QuestKind.PIN -> lifetimeState.pins
        QuestKind.QUOTE -> lifetimeState.quotes
        QuestKind.DAILY -> lifetimeState.dailyCompleted
        QuestKind.ACHIEVEMENT -> awardedStagesState.size
        QuestKind.STREAK -> bestStreakState
        QuestKind.LIKE -> lifetimeState.likes
        QuestKind.FORMATS -> formatsState.size
        QuestKind.LANES -> categoriesState.size
        QuestKind.XP -> xpState
    }

    /** True once the stage's badge has been earned. */
    fun isStageDone(stage: QuestStage): Boolean = stage.id in awardedStagesState

    /** Completed stage count for a chain (drives its progress bar). */
    fun chainProgress(chain: QuestChain): Int =
        chain.stages.count { it.id in awardedStagesState }

    /** The next quest the guide + UI should point at — null when all done. */
    fun currentQuest(): QuestStage? =
        allStages().firstOrNull { stageProgress(it) < it.target }
}

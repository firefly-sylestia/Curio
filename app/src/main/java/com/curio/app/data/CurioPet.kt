package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

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
    private const val KEY_LAST_PLAY_AT = "last_play_at"
    private const val KEY_LAST_EVOLVE_AT = "last_evolve_at"
    private const val KEY_LAST_SEEN_AT = "pet_last_seen_at"
    private const val KEY_LAST_QUEST_AT = "last_quest_at"
    private const val KEY_LAST_STREAK_AT = "last_streak_at"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Growth stages (spec §10.4) ─────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════
    // v9.5 — Evolution system: three tiers (Baby → 1st Evo → Final Evo)
    // with three element paths (Fire / Water / Nature). The baby form is
    // universal; at level 15 the user chooses a path; at level 25 the pet
    // reaches its final form. Each evolution plays an animation.
    // ═══════════════════════════════════════════════════════════════════

    /** Evolution path — the elemental affinity chosen at level 15. */
    enum class EvoPath(val displayName: String, val element: String) {
        FIRE("Blaze", "Fire"),
        WATER("Tide", "Water"),
        NATURE("Bloom", "Nature")
    }

    /** The pet's current growth tier. */
    enum class Stage(val displayName: String, val sizeScale: Float) {
        BABY("Baby Spark", 0.55f),
        FIRST_EVO("Evolved", 0.88f),
        FINAL_EVO("Fully Grown", 1.08f);

        /** Human-readable label for this tier, including the element path. */
        fun label(path: EvoPath?): String = when (this) {
            BABY -> "Baby Spark"
            FIRST_EVO -> "${path?.displayName ?: "Evolved"} (Level 15)"
            FINAL_EVO -> "${path?.displayName ?: "Grown"} (Level 25)"
        }
    }

    /** Computes the evolution tier + path from level and the saved choice. */
    fun evolutionStage(level: Int, path: EvoPath?): Pair<Stage, EvoPath?> {
        return when {
            level >= 25 && path != null -> Stage.FINAL_EVO to path
            level >= 15 && path != null -> Stage.FIRST_EVO to path
            else -> Stage.BABY to null
        }
    }

    /** True when the pet is ready to evolve (level >= 15, no path yet). */
    fun canEvolve(level: Int, path: EvoPath?): Boolean =
        level >= 15 && path == null

    /** True when the final evolution is unlocked (level >= 25). */
    fun canFinalEvolve(level: Int, path: EvoPath?): Boolean =
        level >= 25 && path != null

    /**
     * The highest growth stage the user's stats satisfy. Kept for
     * backward compat with the old Stage-based APIs — maps directly to
     * the new evolution system.
     */
    fun stageFor(xp: Int, saves: Int, lanes: Set<String>, allLaneCount: Int): Stage {
        val level = CurioQuests.levelForXp(xp)
        val path = AppPreferences.evoPath()
        return when {
            level >= 25 && path != null -> Stage.FINAL_EVO
            level >= 15 && path != null -> Stage.FIRST_EVO
            else -> Stage.BABY
        }
    }

    /** Current stage from live [CurioQuests] state + saved evolution path. */
    fun currentStage(): Stage {
        val level = CurioQuests.levelForXp(CurioQuests.xpState)
        val path = AppPreferences.evoPath()
        return when {
            level >= 25 && path != null -> Stage.FINAL_EVO
            level >= 15 && path != null -> Stage.FIRST_EVO
            else -> Stage.BABY
        }
    }

    /** Current evolution path (null before first evolution). */
    fun currentEvoPath(): EvoPath? = AppPreferences.evoPath()

    /** The next stage up (null when fully grown). */
    fun nextStage(current: Stage): Stage? {
        val order = Stage.entries
        val index = order.indexOf(current)
        return order.getOrNull(index + 1)
    }

    /** A short human line about what's missing to reach the next stage. */
    fun nextStageHint(stage: Stage): String {
        val next = nextStage(stage) ?: return "Fully grown. Every lane is yours."
        return when (next) {
            Stage.FIRST_EVO -> "Reach Level 15 to evolve."
            Stage.FINAL_EVO -> "Reach Level 25 for the final evolution."
            Stage.BABY -> "Start your journey."

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
    // v8.30 — REVEAL_OPEN split by cause: REVEAL_TAPPED = the USER opened
    // the card with a tap (reacts to the touch); REVEAL_AUTO = the deck
    // auto-opened it after a spin.
    // v9.2 — the pet now reacts to touches, play sessions and level-ups too.
    enum class Event { SPIN_LANDED, REVEAL_TAPPED, REVEAL_AUTO, EXPLORE, SAVE, TOUCH, PLAY, LEVEL_UP, EVOLVE, QUEST_COMPLETE, STREAK_MILESTONE }

    var eventCount by mutableIntStateOf(0)
        private set
    var lastEvent by mutableStateOf<Event?>(null)
        private set

    /**
     * v8.30 — marks the NEXT reveal open as the spin's AUTO-open, so the
     * reveal screen can pick the right pet line. Set right before the
     * auto-navigation and consumed (and cleared) when the reveal composes.
     */
    var pendingRevealAuto by mutableStateOf(false)
        private set

    /** v8.30 — the spin auto-open is about to navigate. */
    fun markRevealAuto() {
        pendingRevealAuto = true
    }

    /** v8.30 — the reveal consumed the pending auto-open marker (clears it). */
    fun consumeRevealAuto(): Boolean {
        val was = pendingRevealAuto
        pendingRevealAuto = false
        return was
    }

    // v9.x — a quest-complete nudge waiting for Home to show it: set by
    // [noteQuestComplete], consumed once by the Home flower bed so the pet
    // visibly celebrates when the player comes back to Home after a claim.
    var pendingQuestNudge by mutableStateOf(false)
        private set

    fun consumeQuestNudge(): Boolean {
        val was = pendingQuestNudge
        pendingQuestNudge = false
        return was
    }

    /** Called by the screens where the action really happens. */
    fun reactTo(event: Event) {
        lastEvent = event
        eventCount++
    }

    // ── Anti-repeat bag (v9.x) — v16 persisted across sessions ────────
    // The pet never says the same line twice in a row: [pickLine] skips
    // anything spoken recently and only falls back to the full pool once
    // every option has been used. v16 — the bag survives process restarts
    // (seeded from prefs at launch and written through), so lines can't
    // loop across days either.
    private val saidLines = ArrayDeque<String>()
    private const val SAID_LINES_CAP = 40
    private const val KEY_SAID_LINES = "pet_said_lines"

    @Volatile
    private var appContext: Context? = null

    /** Attach the app context once (MainActivity) so dialogue can persist. */
    fun attach(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    /** Seeds the anti-repeat bag from the last session (called at launch). */
    fun seedDialogueMemory(context: Context) {
        val raw = prefs(context).getString(KEY_SAID_LINES, null) ?: return
        saidLines.clear()
        raw.split('|').filter { it.isNotBlank() }.takeLast(SAID_LINES_CAP)
            .forEach { saidLines.addLast(it) }
    }

    /** Picks a line from [options], avoiding anything said recently. */
    fun pickLine(options: List<String>): String {
        if (options.isEmpty()) return "…"
        val fresh = options.filterNot { it in saidLines }
        val chosen = (if (fresh.isNotEmpty()) fresh else options).random()
        saidLines.addLast(chosen)
        while (saidLines.size > SAID_LINES_CAP) saidLines.removeFirst()
        // v16 — write-through so the bag survives restarts (best effort).
        appContext?.let { ctx ->
            runCatching {
                prefs(ctx).edit()
                    .putString(KEY_SAID_LINES, saidLines.joinToString("|"))
                    .apply()
            }
        }
        return chosen
    }

    // ── Chatter + game-frequency tuning (v16) ─────────────────────────
    /** True when a line should be spoken for a base probability (settings-aware). */
    fun shouldSpeak(base: Float): Boolean {
        val mult = when (AppPreferences.petChatterState) {
            "talkative" -> 1.35f
            "quiet" -> 0.35f
            else -> 1f
        }
        return kotlin.random.Random.nextFloat() < (base * mult).coerceIn(0f, 1f)
    }

    /** Multiplier for how often the pet starts games on its own. */
    fun gameFrequencyMultiplier(): Float = when (AppPreferences.petGameFrequencyState) {
        "relaxed" -> 0.55f
        "eager" -> 1.5f
        else -> 1f
    }

    // ── Screen context (v16) — what the pet can reference in lines ────
    @Volatile
    var lastLaneName: String? = null
        private set
    @Volatile
    var lastSavedLaneName: String? = null
        private set
    @Volatile
    var lastSavedTopicName: String? = null
        private set

    /** The Spin screen reports the active deck's lane name. */
    fun noteLaneFocus(lane: String?) {
        lastLaneName = lane
    }

    /** A capture was saved in [lane] about [topic] (persisted weekly too). */
    fun noteSavedLane(context: Context, lane: String?, topic: String?) {
        lastSavedLaneName = lane
        lastSavedTopicName = topic
        // v16 — persist the weekly lane counts so factLine can say "you
        // saved 3 songs keepsakes this week!"
        lane?.let { AppPreferences.noteWeeklySave(context, it) }
    }

    // ── Playful-annoyed (v9.x) ────────────────────────────────────────
    // If the SAME action repeats several times within a few minutes (the
    // user keeps opening things back-to-back), the pet gets adorably sassy
    // for one line instead of cheerfully repeating its standard reaction,
    // then the burst resets.
    private val eventBursts = mutableMapOf<Event, MutableList<Long>>()
    private const val EVENT_BURST_WINDOW_MS = 4 * 60_000L
    private const val EVENT_BURST_COUNT = 3

    private fun isEventBurst(event: Event): Boolean {
        val now = System.currentTimeMillis()
        val times = eventBursts.getOrPut(event) { mutableListOf() }
        times.add(now)
        times.removeAll { now - it > EVENT_BURST_WINDOW_MS }
        if (times.size >= EVENT_BURST_COUNT) {
            eventBursts[event] = mutableListOf()
            return true
        }
        return false
    }

    private val sassyLines = listOf(
        "Again?! Hehe, okay!",
        "You really like this one!",
        "Again! I knew you'd do it.",
        "Ooh, we're doing this again?",
        "You came back for another one!",
        "Hehe, I saw that coming.",
        "One more? I won't complain.",
        "Again again? I can handle that!",
        "You really don't want to let this one go, huh?",
        "Okay! I'm invested now.",
        "More?! My little heart is ready.",
        "You keep picking it! I approve.",
        "I wasn't going to say anything... but again?",
        "Hehe, you're making this a habit.",
        "Alright, one more. For science."
    )

    /** A short, cute line for the pet's reaction to [event]. */
    fun eventLine(event: Event): String {
        // v9.x — a burst of the same action earns one adorably sassy line.
        // v14 — the BABY's comeback is tiny and repetitive, not witty; the
        // fully grown pet answers with dry patience.
        if (isEventBurst(event)) {
            return pickLine(
                when (currentStage()) {
                    Stage.BABY -> babySassyLines
                    Stage.FINAL_EVO -> matureSassyLines
                    Stage.FIRST_EVO -> sassyLines
                }
            )
        }
        // v14 — the BABY voice is telegraphic and the fully grown voice is
        // its own mature register: both speak from their own per-event pools.
        when (currentStage()) {
            Stage.BABY -> return babyEventLine(event)
            Stage.FINAL_EVO -> return matureEventLine(event)
            Stage.FIRST_EVO -> Unit
        }
        return when (event) {
            Event.SPIN_LANDED -> {
                // v16 — the pet references the actual deck when it can.
                val lane = lastLaneName
                if (lane != null && kotlin.random.Random.nextFloat() < 0.5f) {
                    pickLine(listOf(
                        "Ooh, $lane! I like this one.",
                        "$lane! Ooh, can we peek?",
                        "We landed on $lane! Yay!",
                        "It's $lane again! I remember this one.",
                        "$lane! My little brain is already curious.",
                        "Ooh, $lane found us!",
                        "Look! $lane!",
                        "We got $lane! Good spin!"
                    ))
                } else {
                    pickLine(listOf(
                        "Ooh! It landed!",
                        "We got one!",
                        "What did we find?",
                        "Ooh, this looks interesting!",
                        "Yay! A new little mystery.",
                        "Hehe, it stopped!",
                        "We found something!",
                        "Where did we land?",
                        "Ooh ooh! Show me!",
                        "Aha! This one!",
                        "Can we look now?",
                        "I wonder what this is.",
                        "The spin picked one! I like it already.",
                        "Weee! What a spin!",
                        "That was a good one!"
                    ))
                }
            }
            // v8.30 — the USER's tap gets a touch reaction, never "it opened
            // itself".
            Event.REVEAL_TAPPED -> pickLine(listOf(
                "You picked it! Ooh!",
                "Ooh, good choice!",
                "You wanted this one, didn't you?",
                "Hehe, you picked it!",
                "I like your choice.",
                "That one looked interesting to me too.",
                "Good pick! Let's see what it's hiding.",
                "You found a good one!",
                "Ooh! This could be fun.",
                "I was hoping you'd tap that one.",
                "Your curiosity chose well.",
                "Okay, okay, let's look!",
                "You have good curiosity.",
                "That one definitely deserved a peek.",
                "Ooh, I'm excited for this one."
            ))
            // v8.30 — only the spin's true AUTO-open says the surprise lines.
            Event.REVEAL_AUTO -> pickLine(listOf(
                "Ooh! It opened!",
                "It opened by itself! Sneaky.",
                "Hehe, surprise!",
                "Oh! Look what appeared.",
                "We didn't even have to ask.",
                "It picked for us!",
                "A surprise topic!",
                "Well, hello there, little mystery.",
                "Ooh, I wasn't ready!",
                "It chose one for us. Bold.",
                "Look! A new thing!",
                "Peek-a-boo, topic!",
                "Hehe, that was unexpected.",
                "Ohhh, what's this?",
                "I guess we're looking at this one now!"
            ))
            Event.EXPLORE -> pickLine(listOf(
                "Go explore! I'll be right here.",
                "Have fun! Bring me something interesting.",
                "Go see what you can find!",
                "Ooh, adventure time!",
                "Take me with you! ...Oh. Right. I can't.",
                "Go on! I'll wait here.",
                "Find something that makes you go “wow!”",
                "Go look around. I'll keep watch.",
                "Your adventure starts now!",
                "Bring back a good story, okay?",
                "Explore lots! I like hearing what you find.",
                "Off you go!",
                "Go find a little wonder.",
                "Have fun out there!",
                "I'll be cheering from here!"
            ))
            // v8.29 — "Mine now… I mean, ours!" only after the bond is FRIEND+.
            Event.SAVE -> {
                val savedLane = lastSavedLaneName
                val base = if (isWarm()) listOf(
                    "Yay! We get to keep it!",
                    "It's ours now!",
                    "Another little memory for us.",
                    "I love keeping things with you.",
                    "Tucked away safe and sound!",
                    "Our collection is growing!",
                    "Hehe, another one for our shelf.",
                    "I'll remember this one with you.",
                    "One more little treasure!"
                ) else listOf(
                    "Saved! Yay!",
                    "We get to keep it!",
                    "Tucked away!",
                    "Ooh, another keepsake!",
                    "Safe on the shelf.",
                    "Kept for later!",
                    "A little something to remember.",
                    "Into the collection it goes!",
                    "We saved it!",
                    "Hehe, keeper!"
                )
                // v16 — reference the lane the capture was saved in.
                if (savedLane != null && kotlin.random.Random.nextFloat() < 0.5f) {
                    pickLine(listOf(
                        "$savedLane got another little treasure!",
                        "We saved one for the $savedLane shelf!",
                        "Ooh, another $savedLane keepsake!",
                        "$savedLane is growing! Yay!",
                        "Into the $savedLane pile it goes.",
                        "Our $savedLane shelf is getting cozy."
                    ))
                } else {
                    pickLine(base)
                }
            }
            // v9.2 — the pet answers the touch / play / level-up moments too.
            Event.TOUCH -> pickLine(listOf(
                "Boop!",
                "Hehe! Again?",
                "That tickles!",
                "You found my spot!",
                "Mmm, pats!",
                "Soft! I like that.",
                "Boop boop!",
                "Hehe, hi!",
                "Ooh! What was that?",
                "Tiny boop!",
                "More pats, please.",
                "You came to see me!",
                "That was a good boop.",
                "Hehe... you're silly.",
                "Boop me again. I dare you."
            ))
            Event.PLAY -> pickLine(listOf(
                "Wheee! That was fun!",
                "Again! Please?",
                "One more round!",
                "Hehe, you can't catch me!",
                "That was so much fun!",
                "I'm not tired yet!",
                "Can we play again?",
                "I could do that all day.",
                "My paws are still bouncing!",
                "You got me! ...Maybe.",
                "Best game ever. For now.",
                "Hehe! I want another turn.",
                "That was silly. I loved it.",
                "Zoomies!",
                "Okay, okay... one more!"
            ))
            Event.LEVEL_UP -> pickLine(listOf(
                "Ooh! I grew!",
                "Did you see that?!",
                "I'm bigger! Hehe!",
                "A shiny new level!",
                "Yay! Look at me!",
                "I feel all sparkly!",
                "We grew together!",
                "Something feels different. In a good way!",
                "I'm getting better at this!",
                "Ooh, new power!",
                "Hehe, I'm leveling up!",
                "Look at my new little glow!",
                "Another step!",
                "I'm growing!",
                "I feel extra curious today."
            ))
            // v13 — the evolution ceremony: a bigger moment than a level-up.
            Event.EVOLVE -> evolutionCeremonyLine()
            // v13 — a claimed daily/weekly quest is a reward moment.
            Event.QUEST_COMPLETE -> pickLine(questCompleteLines)
            // v13 — a new best streak (day-specific at the flame milestones).
            Event.STREAK_MILESTONE -> streakMilestoneLine(CurioQuests.bestStreakState)
        }
    }

    /**
     * v13 — the streak-milestone line: the flame days (1 / 3 / 7 / 14 / 30)
     * get their own bigger celebrations; other new-best days get a warm
     * "still glowing" line.
     */
    fun streakMilestoneLine(streak: Int): String = when (streak) {
        1 -> pickLine(listOf(
            "Day one! We started!", "Our little streak is alive!"
        ))
        3 -> pickLine(listOf(
            "Three days! Look at us go!", "Day three! You're keeping the spark going!"
        ))
        7 -> pickLine(listOf(
            "A whole week! I'm so happy!", "Seven days! We made a tiny tradition!"
        ))
        14 -> pickLine(listOf(
            "Two weeks! That's a lot of curiosity.", "Fourteen days! We're really doing this!"
        ))
        30 -> pickLine(listOf(
            "Thirty days! Wow... that's our whole little month.",
            "A whole month of discoveries! I'm proud of us."
        ))
        else -> pickLine(listOf(
            "Day $streak! We're still glowing!",
            "$streak days! Look how far we've come.",
            "Day $streak! One more little spark.",
            "$streak days in a row! Yay us!",
            "Day $streak! Keep that little flame going."
        ))
    }

    /**
     * v13 — the evolution ceremony: a line the pet says the moment it
     * crosses into a new growth tier, flavored by its element path.
     */
    fun evolutionCeremonyLine(): String {
        val stage = currentStage()
        val path = currentEvoPath()
        return when (stage) {
            Stage.FIRST_EVO -> when (path) {
                EvoPath.FIRE -> pickLine(listOf(
                    "Ooh! I'm all warm now!",
                    "I got a bigger spark! Hehe!",
                    "Look! I can glow more!",
                    "Something fiery happened to me!",
                    "I'm warm, I'm bright, I'm ready!"
                ))
                EvoPath.WATER -> pickLine(listOf(
                    "Ooh! I feel all splashy!",
                    "I got a little wave in me!",
                    "I'm all cool and glowy now!",
                    "Hehe, I feel like I'm floating.",
                    "Look! My spark got a ripple!"
                ))
                EvoPath.NATURE -> pickLine(listOf(
                    "Ooh! I feel all leafy!",
                    "Something green grew in me!",
                    "Look! I'm blooming!",
                    "Hehe, I feel like spring.",
                    "I think I grew a little wild."
                ))
                null -> pickLine(listOf(
                    "Wait... I grew?!",
                    "Look at me! I'm bigger!",
                    "Ooh! New me!",
                    "Hehe, I feel different.",
                    "I got a little upgrade!"
                ))
            }
            Stage.FINAL_EVO -> pickLine(listOf(
                "I made it all the way!",
                "Wow... look at me now.",
                "I'm fully grown! But I'm still me.",
                "Hehe. I really did grow up.",
                "I feel like all our little moments came with me.",
                "Look how far we've come.",
                "I'm all grown up... that feels strange.",
                "I think I finally know what kind of little creature I am."
            ))
            Stage.BABY -> pickLine(listOf(
                "Big me!", "Grow!", "New me!", "Wow!", "Bigger!"
            ))
        }
    }

    /** Send the pet back to bed after a long idle — the bed shows it asleep. */
    fun settleToSleep() {
        awake = false
        atHome = false
    }

    // ── Personality (v8.12) — a persona that BUILDS from the user's actual
    //    interaction history and PERSISTS across sessions: boops make it
    //    cuddly, play sessions make it bouncy, exploring makes it curious.
    //    Slightly biases how often it starts games on its own.
    enum class Persona(val displayName: String, val tagline: String) {
        CUDDLY("Cuddly", "loves boops and scritches"),
        BOUNCY("Bouncy", "always up for a game"),
        EXPLORER("Explorer", "can't pass up a new lane"),
        SPARKY("Sparky", "still finding its spark")
    }

    private const val KEY_PET_BOOPS = "pet_boops"
    private const val KEY_PET_PLAYS = "pet_plays"

    /** The user petted the floating pet (persisted — feeds the persona). */
    fun noteTouch(context: Context) {
        val p = prefs(context)
        p.edit().putInt(KEY_PET_BOOPS, p.getInt(KEY_PET_BOOPS, 0) + 1).apply()
        CurioPetBrain.observeTouch(context)
        // v9.2 — boops trigger the pet's TOUCH reaction (hop + line).
        reactTo(Event.TOUCH)
    }

    /** The pet played (a dart / self-started game — persisted). */
    fun notePlay(context: Context, react: Boolean = true) {
        val p = prefs(context)
        p.edit()
            .putInt(KEY_PET_PLAYS, p.getInt(KEY_PET_PLAYS, 0) + 1)
            .putLong(KEY_LAST_PLAY_AT, System.currentTimeMillis())
            .apply()
        CurioPetBrain.observePlay(context)
        // v16 — every real play also feeds the PLAY daily quest.
        CurioQuests.notePetPlay(context)
        // v9.2 — a play session starts the pet's PLAY reaction.
        // v12 — some plays (the post-tap dart, the autonomous games, the
        // Pet Life routine) bring their own line; `react = false` still
        // counts the play and feeds the persona, but skips the event so the
        // generic play reaction can't clobber that line.
        if (react) reactTo(Event.PLAY)
    }

    private fun touchCount(context: Context): Int = prefs(context).getInt(KEY_PET_BOOPS, 0)
    private fun playCount(context: Context): Int = prefs(context).getInt(KEY_PET_PLAYS, 0)

    /**
     * The pet's growing personality — the dominant of cuddles / play /
     * exploration, learned from real history (not a random pick).
     */
    fun persona(context: Context): Persona {
        val boops = touchCount(context)
        val plays = playCount(context)
        val explores = CurioQuests.lifetimeState.explores
        return when {
            boops >= 3 && boops >= plays && boops >= explores -> Persona.CUDDLY
            plays >= 3 && plays >= boops -> Persona.BOUNCY
            explores >= 3 -> Persona.EXPLORER
            else -> Persona.SPARKY
        }
    }

    /**
     * How often the pet starts a game on its own — bouncy pets play more,
     * and the clock sets the energy level (v8.14): sprightly in the
     * morning, winding down after dark.
     */
    fun playfulBias(context: Context): Float {
        val base = when (persona(context)) {
            Persona.BOUNCY -> 0.22f
            Persona.EXPLORER -> 0.14f
            Persona.CUDDLY -> 0.10f
            Persona.SPARKY -> 0.08f
        }
        val energy = when (timeOfDay()) {
            TimeOfDay.MORNING -> 1.5f
            TimeOfDay.AFTERNOON -> 1.15f
            TimeOfDay.EVENING -> 0.9f
            TimeOfDay.NIGHT -> 0.45f
        }
        return (base * energy).coerceIn(0f, 0.3f)
    }

    // ── Time of day (v8.14) — 4 phases from the device clock ──────────
    enum class TimeOfDay(val displayName: String) {
        MORNING("Morning"), AFTERNOON("Afternoon"), EVENING("Evening"), NIGHT("Night")
    }

    /** The current time-of-day phase from the device clock (v8.14). */
    fun timeOfDay(): TimeOfDay {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            in 18..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    /**
     * v8.14 — at app launch the pet wakes on its own in the MORNING (and
     * greets), while at night it stays tucked in bed. Afternoon/evening
     * launches keep the old asleep-until-tapped behavior. Called from
     * MainActivity after the state seeds.
     */
    fun wakeForMorning() {
        if (timeOfDay() == TimeOfDay.MORNING && !awake) {
            awake = true
            atHome = false
        }
    }

    /** A short morning greeting (v8.14) — shown when the pet appears in the morning. */
    fun morningGreeting(): String =
        // v14 — the BABY greets the morning in two words; the fully grown
        // pet greets it with calm.
        when (currentStage()) {
            Stage.BABY -> pickLine(babyMorningLines)
            Stage.FINAL_EVO -> pickLine(matureMorningLines)
            Stage.FIRST_EVO -> pickLine(listOf(
                "Good morning!",
                "Morning! You're here!",
                "Good morning, sleepyhead.",
                "Hehe, morning!",
                "A new day! Ready?",
                "Morning! I have questions already.",
                "Rise and shine! Gently, though.",
                "Good morning! Let's find something interesting.",
                "Morning! Come explore with me.",
                "Hi! It's a brand-new day.",
                "The day is awake. So am I!",
                "Morning! What shall we discover?",
                "Good morning! I missed seeing you.",
                "Fresh day, fresh curiosity!",
                "Hehe, hello morning."
            ))
        }

    /**
     * v13 — return-after-absence welcome: the first time the pet appears
     * after ≥1 day away it says a welcome-home line instead of jumping
     * straight into mood chatter. Consumed once per absence (the timestamp
     * is refreshed when called); a brand-new pet (never seen before) stays
     * quiet. Longer absences pick warmer pools.
     */
    fun welcomeBackLine(context: Context): String? {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        val lastSeen = p.getLong(KEY_LAST_SEEN_AT, 0L)
        if (lastSeen <= 0L) {
            p.edit().putLong(KEY_LAST_SEEN_AT, now).apply()
            return null
        }
        val awayMs = now - lastSeen
        p.edit().putLong(KEY_LAST_SEEN_AT, now).apply()
        val dayMs = 86_400_000L
        // v14 — the BABY's welcome is telegraphic too; the fully grown pet
        // welcomes with quiet warmth.
        return when {
            awayMs < dayMs -> null
            awayMs >= 7 * dayMs -> pickLine(when (currentStage()) {
                Stage.BABY -> babyWelcomeBackWeekLines
                Stage.FINAL_EVO -> matureWelcomeBackWeekLines
                Stage.FIRST_EVO -> welcomeBackWeekLines
            })
            awayMs >= 3 * dayMs -> pickLine(when (currentStage()) {
                Stage.BABY -> babyWelcomeBackDaysLines
                Stage.FINAL_EVO -> matureWelcomeBackDaysLines
                Stage.FIRST_EVO -> welcomeBackDaysLines
            })
            else -> pickLine(when (currentStage()) {
                Stage.BABY -> babyWelcomeBackDayLines
                Stage.FINAL_EVO -> matureWelcomeBackDayLines
                Stage.FIRST_EVO -> welcomeBackDayLines
            })
        }
    }

    // ── Bond (v8.29) — how familiar the pet is allowed to be ───────────
    // The pet starts polite and neutral and only talks like a close friend
    // once the player has actually grown with it (levels from XP). Warm,
    // intimate lines ("Best friends!", "I saved your spot") stay gated
    // behind the FRIEND tier, so a brand-new pet never acts familiar.
    enum class Bond(val displayName: String) {
        STRANGER("Stranger"), ACQUAINTANCE("Acquaintance"),
        FRIEND("Friend"), CLOSE("Close friend")
    }

    /**
     * The current bond tier — level sets the ceiling, but the FRIEND/CLOSE
     * tiers also need real saved history, so warmth genuinely GROWS over
     * time instead of arriving on day one with fast early leveling.
     */
    fun bond(): Bond {
        val level = CurioQuests.levelForXp(CurioQuests.xpState)
        val saves = CurioQuests.lifetimeState.saves
        return when {
            level >= 12 && saves >= 5 -> Bond.CLOSE
            level >= 6 && saves >= 2 -> Bond.FRIEND
            level >= 3 -> Bond.ACQUAINTANCE
            else -> Bond.STRANGER
        }
    }

    /** True once the pet can talk like a familiar friend (v8.29). */
    private fun isWarm(): Boolean = bond().ordinal >= Bond.FRIEND.ordinal

    // ── Moods (spec §10.5) — derived from recent activity, never shaming ──
    // v8.13 — two more status moods: FOCUSED (the user is writing/saving on
    // the capture screen) and BOUNCY (a play session just ended — the pet is
    // still full of beans). All moods are state-derived, so the pet reads
    // the user's behavior instead of wearing a fixed face.
    // v9.2 — three new emotions: SHY (a blushy first-contact), GRUMPY (a
    // long daytime lull before it nods off) and PLAYFUL (the post-play high
    // fading gently out of BOUNCY).
    enum class Mood { PROUD, EXCITED, HAPPY, CURIOUS, SLEEPY, FOCUSED, BOUNCY, SHY, GRUMPY, PLAYFUL }

    fun mood(context: Context, lanes: Set<String>, screen: String? = null): Mood {
        val now = System.currentTimeMillis()
        return when {
            now - lastLevelUpAt(context) < 90_000L -> Mood.PROUD
            now - lastNewLaneAt(context) < 60_000L -> Mood.EXCITED
            // The user is mid-capture — the pet stays quiet and attentive.
            screen == "capture" -> Mood.FOCUSED
            // A play session just ended (a dart, a self-started game).
            now - lastPlayAt(context) < 5 * 60_000L -> Mood.BOUNCY
            // v9.2 — the play high lingers a little softer before it fades.
            now - lastPlayAt(context) < 30 * 60_000L -> Mood.PLAYFUL
            now - lastXpAt(context) < 120_000L -> Mood.HAPPY
            // v9.2 — a brand-new bond gets one shy blush moment the first
            // time the user engages, then grows out of it.
            bond() == Bond.STRANGER && now - lastXpAt(context) < 5 * 60_000L -> Mood.SHY
            leastExploredLane(context, lanes) != null -> Mood.CURIOUS
            // v9.2 — a long daytime lull reads grumpy before it reads sleepy.
            // v12 — 6h was effectively unreachable (the pet auto-naps after
            // 8 min idle, so it only ever sulked right after being woken from
            // a long absence). 45 min makes it a real mood during a quiet or
            // petting-heavy stretch without tripping over the daytime nap.
            timeOfDay() != TimeOfDay.NIGHT && now - lastXpAt(context) > 45 * 60_000L -> Mood.GRUMPY
            // v8.14 — natural bed time: after dark the pet gets drowsy once
            // the day's excitement cools (30 min without XP) and dozes off.
            timeOfDay() == TimeOfDay.NIGHT && now - lastXpAt(context) > 30 * 60_000L -> Mood.SLEEPY
            now - lastXpAt(context) > 12 * 3_600_000L -> Mood.SLEEPY
            else -> Mood.HAPPY
        }
    }

    /**
     * The first visible category the user has never really TRIED — the
     * passport/discovery gap the pet nudges toward (null = every lane seen).
     *
     * v8.13 — smarter: a lane counts as tried when the quests' explored set
     * knows it OR the passport has ANY engagement (a reveal counts — opening
     * a topic is "trying" it, even when the explore was a silent browse that
     * never touched quest progress). This stops the pet from nagging
     * "We haven't tried X" for lanes the user already peeked or explored
     * through the non-tracking Explore buttons, and stops the discovery
     * daily from pointing at lanes the user has actually been in.
     */
    fun leastExploredLane(context: Context, explored: Set<String>): CurioCategory? =
        CurioCategories.visible.firstOrNull { cat ->
            cat.id.name !in explored &&
                CurioPassport.progress(context, cat.id).stamp == CurioPassport.Stamp.UNSEEN
        }

    // ── Rule-based dialogue ("local AI", spec §10.6/10.7) ──────────────
    // One sentence for passive bubbles; no nagging; never interrupts input.

    // ── Passive dialogue (v8.8 — a little variety per mood) ────────────
    // `__LANE__` is swapped for the least-explored lane's name at show time.
    private val excitedLines = listOf(
        "Ooh! What's over here?",
        "A new place! Can we look?",
        "I've never seen this before!",
        "New things! My favorite!",
        "Ooh ooh! I want to know!",
        "Something new found us!",
        "My curiosity is doing little jumps.",
        "I want to peek!",
        "Come on! Let's see!",
        "This feels like the good kind of unknown.",
        "Ooh! A new little corner of the world.",
        "Can we explore this one together?",
        "I have questions already!",
        "New lane! New adventure!",
        "I wonder what's hiding in here."
    )
    // v8.14 — the HAPPY mood wears the hour's voice: morning energy, cozy
    // evening, hushed night.
    private val morningLines = listOf(
        "Good morning! I saved some curiosity for you.",
        "Morning! Can we find something lovely?",
        "New day! New things to notice!",
        "You're here! Let's start gently.",
        "The morning feels extra curious today.",
        "I woke up ready to explore!",
        "Hehe, good morning!",
        "Fresh day, fresh little questions.",
        "Morning! What shall we discover?",
        "I think today might have a good surprise."
    )
    private val afternoonLines = listOf(
        "Afternoon! Want a tiny discovery?",
        "Ooh, a little break!",
        "The day still has room for one more wonder.",
        "Come peek at something with me.",
        "Afternoon curiosity time!",
        "Hehe, you're here. Let's look around.",
        "A little discovery would be nice right now.",
        "Want to add a spark to the afternoon?",
        "The day isn't done yet!",
        "Let's find something interesting."
    )
    private val eveningLines = listOf(
        "Evening! Cozy discovery time.",
        "The day is slowing down. Want one more peek?",
        "Ooh, warm lights and little mysteries.",
        "This feels like a nice time to wonder.",
        "Evening! Come sit with me.",
        "One cozy discovery before the day ends?",
        "Hehe, I like evenings with you.",
        "The shelf feels extra cozy tonight.",
        "Let's find something gentle to end the day.",
        "The world is quieting down. We can still be curious."
    )
    private val nightLines = listOf(
        "Shhh... night time. Want a quiet little discovery?",
        "The stars are out. I want to look too.",
        "It's late... but curiosity is still awake.",
        "One tiny peek, then sleepy time?",
        "The night makes everything feel mysterious.",
        "Ooh, moonlight!",
        "We can wonder quietly tonight.",
        "The world is sleepy. I'm only a little sleepy.",
        "One more little question?",
        "I'll keep you company for a bit."
    )
    // v8.29 — the warmer twins only speak once the bond is FRIEND or closer.
    private val warmMorningLines = listOf(
        "Good morning! I'm happy you're here.",
        "Morning! I was waiting for you.",
        "You came back! Good morning!",
        "Morning, friend. Let's find something together.",
        "Hehe, I saved a little curiosity for us.",
        "Good morning! I missed our little adventures.",
        "You're here. Now my morning feels right."
    )
    private val warmAfternoonLines = listOf(
        "You're here! Yay!",
        "Afternoon, friend. Come sit with me.",
        "I was hoping you'd come back.",
        "Hehe, let's find something together.",
        "A little discovery with you sounds perfect.",
        "There you are! Want to peek?"
    )
    private val warmEveningLines = listOf(
        "Evening, friend. Come be cozy with me.",
        "I'm glad you're here tonight.",
        "Hehe, my favorite part of evening is this.",
        "You're here! Let's make a little memory.",
        "Cozy hour! Stay with me a little.",
        "Evening feels nicer with you around."
    )
    private val warmNightLines = listOf(
        "You're still awake? Hehe, me too.",
        "Good night, friend... not quite yet.",
        "Stay a little? We can be quiet.",
        "I'm glad I get to keep you company.",
        "One tiny discovery before sleep?",
        "Night feels softer when you're here."
    )
    private val curiousLines = listOf(
        "We haven't peeked at __LANE__ yet.",
        "Ooh... what do you think is hiding in __LANE__?",
        "__LANE__ is still waiting for us.",
        "Psst... __LANE__ looks interesting.",
        "Can we try __LANE__ next?",
        "I've been looking at __LANE__...",
        "One little mystery left: __LANE__.",
        "What if we peek at __LANE__?",
        "__LANE__ is calling me. Very quietly.",
        "I wonder what we'd find in __LANE__.",
        "Should we give __LANE__ a chance?",
        "I keep thinking about __LANE__.",
        "__LANE__ is the one we haven't met yet.",
        "Come on... let's meet __LANE__."
    )
    private val sleepyLines = listOf(
        "Yawn... I'm getting sleepy.",
        "The deck can wait. Probably.",
        "I might curl up soon.",
        "My eyes are getting heavy.",
        "Cozy time...",
        "Zzz... I mean, I'm listening!",
        "One more yawn and I'm a pillow.",
        "I'll be right here when you come back.",
        "Maybe we should rest our little brains.",
        "The night feels soft.",
        "I'm sleepy, but I'm still here.",
        "Good night... when you're ready."
    )
    // v8.13 — the new moods' lines: focused keeps out of the way while the
    // user writes; bouncy rides the post-play high.
    private val focusedLines = listOf(
        "Shhh... I'm watching over your thoughts.",
        "Take your time. I'll be quiet.",
        "Ooh, you're thinking hard.",
        "Keep going. I won't interrupt.",
        "I'll guard this little thought for you.",
        "Your words are taking shape.",
        "Hehe, serious thinking face!",
        "No distractions. Tiny paws: quiet.",
        "This one feels important. Take your time.",
        "I'll stay right here while you write.",
        "Keep going. I'm listening.",
        "A thought worth keeping deserves a little time."
    )
    private val bouncyLines = listOf(
        "Hehe! I'm still bouncing!",
        "My paws forgot how to be still.",
        "That was fun! I want another one.",
        "I still have zoomies!",
        "Wheee... okay, I'm calming down.",
        "My little feet are doing their own thing.",
        "That game left me all wiggly.",
        "I'm still smiling!",
        "Round two later?",
        "I think I need to bounce one more time.",
        "Play energy: still very much alive.",
        "Hehe, I can't sit still."
    )
    // v9.2 — lines for the three new emotions.
    private val shyLines = listOf(
        "H-hi...",
        "Oh! You noticed me.",
        "Hehe... hi.",
        "I was just hiding over here.",
        "Don't stare! ...Okay, you can.",
        "*peeks* Hi.",
        "I'm still getting brave.",
        "Um... can I stay here?",
        "You seem nice.",
        "I think I like you.",
        "*tiny wave* Hi.",
        "I'm a little shy today.",
        "Don't worry, I'll come out eventually.",
        "Hehe... you caught me."
    )
    private val grumpyLines = listOf(
        "Hmph. It's been quiet.",
        "I'm not grumpy. I'm... thinking loudly.",
        "The deck is being very boring today.",
        "I think we need a little adventure.",
        "My spark needs something to do.",
        "I've been waiting. Just saying.",
        "Someone should spin something.",
        "I am absolutely not pouting.",
        "Okay, maybe I am pouting a little.",
        "This is a very serious lack of excitement.",
        "My tiny patience is running low.",
        "I vote for a little discovery."
    )
    private val playfulLines = listOf(
        "Hehe! I still want to play.",
        "One more game?",
        "Boop me. I dare you.",
        "My zoomies aren't finished.",
        "I could play again. Just saying.",
        "That game made me silly.",
        "Catch me!",
        "I have an idea. It's probably a game.",
        "Play? Play?",
        "I'm trying to be calm. It's not working.",
        "Hehe... your turn!",
        "I still have one tiny game left in me."
    )
    // v13 — return-after-absence welcome pools (see [welcomeBackLine]).
    private val welcomeBackDayLines = listOf(
        "You're back!",
        "I missed you!",
        "There you are!",
        "Hehe, welcome back!",
        "I was waiting for you.",
        "Yay! You're here again.",
        "I kept your spot warm.",
        "I wondered when you'd come back.",
        "Back already? I like that.",
        "Oh! It's you!",
        "The shelf felt quieter without you.",
        "I'm glad you're here."
    )
    private val welcomeBackDaysLines = listOf(
        "You were gone! I missed you!",
        "You're back! Yay!",
        "I was starting to wonder.",
        "Hehe, finally!",
        "I kept everything safe for you.",
        "Look who's back!",
        "I saved some curiosity for you.",
        "The shelf is happy you're here.",
        "I had lots of little thoughts while you were gone.",
        "Come on, tell me what you missed.",
        "You're back! I have so much to show you.",
        "Home again!"
    )
    private val welcomeBackWeekLines = listOf(
        "A whole week?! I missed you!",
        "You're really back!",
        "Hehe, I kept waiting!",
        "Seven days! That's a long time for a tiny pet.",
        "I saved all my excitement for you.",
        "You're here! I was starting to narrate to myself.",
        "Come here! I have missed you.",
        "A whole week away... and now you're back!",
        "Yay! The shelf feels right again.",
        "I kept your little spot just the same."
    )
    // v13 — lines for claiming a daily/weekly quest (see [noteQuestComplete]).
    private val questCompleteLines = listOf(
        "We did it!",
        "Quest complete! Yay!",
        "You finished it!",
        "Hehe, that quest didn't stand a chance.",
        "We got it done!",
        "Another little win!",
        "Done! I'm proud of us.",
        "Look at that! Finished!",
        "One more thing checked off!",
        "We did the thing!",
        "Quest conquered!",
        "Yay! That felt good.",
        "Another win for us.",
        "Finished and sparkly!",
        "High five! Tiny paw five!"
    )

    // ═══════════════════════════════════════════════════════════════════
    // v14 — BABY voice (telegraphic, ~18-24 month equivalent)
    // Research-informed (child telegraphic speech + pet-directed speech):
    // 1-3 word utterances, content words only (no articles/auxiliaries),
    // concrete nouns & verbs, exclamation-led, heavy on onomatopoeia. The
    // baby says FEW things; the evolved forms speak the full rich library
    // above. Routed by currentStage() in every line source below.
    // ═══════════════════════════════════════════════════════════════════
    private val babySassyLines = listOf(
        "Again?!", "More!", "Again again!", "You!", "Ooh again!",
        "Same!", "More more!", "Again!"
    )
    private val babyProudLines = listOf(
        "Big!", "Grow!", "Up up!", "More me!", "New me!", "Glow!", "Proud!"
    )
    private val babyExcitedLines = listOf(
        "New!", "Ooh!", "Look look!", "Wow!", "What?!", "Fresh!", "Want!", "New new!",
        // v118 — BABY VOICE EXPANSION: excited/creature-like + surprise.
        "OOOH!", "Ooh ooh ooh!", "Curi OOOH!", "Look look look!", "New new new!",
        "New thing!", "New thing! New thing!", "Curi see!", "What what?", "That! That!",
        "There! There!", "Ooh, there!", "Shiny!", "So shiny!", "Pretty!",
        "Curi likes shiny!", "Curi wants peek!", "Peek peek peek!", "Curi peek now?",
        "Can Curi see?", "Want! Want!", "Curi want!", "Go go go!", "Fast fast!",
        "Wheee!", "Wiii!", "Pwee!", "Zoom zoom!", "Curi zoom!", "Curi go zoom!",
        "Pounce!", "Pounce pounce!", "Curi ready!", "Ready ready!", "Yay! New!",
        "Ooh! New!", "Wow wow!", "Woooow!", "Curi wow!",
        "Huh?", "Huhhh?", "Eh?!", "What?!", "Ooh?!", "Curi confused.", "Brain... hmm.",
        "Curi doesn't know.", "Don't know!", "Curi think...", "Think think...",
        "Hmm hmm...", "Wait wait!", "Wait!", "What happened?", "Where go?", "It moved!",
        "Curi saw that!", "Did you see?", "You saw?", "Again?", "What was that?!",
        "Ooh, weird!", "Weird weird!", "Strange!", "Need look!", "Need investigate!",
        "Curi investigate!", "Mystery!"
    )
    private val babyHappyLines = listOf(
        "Happy!", "Yay!", "Hi!", "Warm!", "Good!", "Glow!", "You here!", "Yay you!",
        // v118 — BABY VOICE EXPANSION: happy/affectionate + friendship.
        "Curi happy-happy!", "Yay yay Curi!", "Cozy Curi!", "Curi cuddle!",
        "Curi snuggle!", "Curi hug!", "Tiny hug!", "Big hug!", "Curi likes you!",
        "Curi likes you lots!", "You here! Yay!", "You came! Yay yay!",
        "Curi missed you!", "Miss miss!", "You back!", "Back back!", "Home home!",
        "Curi here!", "Curi stay!", "Hehe! Curi happy!", "Prrr... happy.",
        "Mrrp! Happy!", "Squee! You!", "Curi loves boops!", "Boop makes happy!",
        "Pats! More pats!",
        "You!", "You here!", "Curi happy!", "Curi missed you!", "Missed missed!",
        "Curi waited!", "You came back!", "Yay you!", "Curi likes you!", "Curi likes us!",
        "Us us!", "We go!", "We play!", "We look!", "We find!", "We keep!", "We did it!",
        "Our shelf!", "Our spark!", "Our little thing!", "Curi stay with you.",
        "You stay with Curi?", "Together!", "Together together!", "Curi friend!",
        "Friend friend!", "Best friend!", "Curi and you!", "You and Curi!", "Us!"
    )
    private val babyCuriousLines = listOf(
        "What?", "Ooh?", "New?", "Look!", "That!", "What this?", "Peek?", "Ooh what?",
        // v118 — BABY VOICE EXPANSION: curious little questions.
        "What this?", "What that?", "Who this?", "Who that?", "Why?", "Why why?", "How?",
        "How that?", "Where?", "Where go?", "Where it go?", "Curi look?", "Curi peek?",
        "Can Curi see?", "Can Curi touch?", "Can Curi try?", "What's inside?",
        "What hiding?", "Something there?", "Something new?", "Is it shiny?", "Is it fun?",
        "Is it tiny?", "Is it big?", "Is it ours?", "We keep?", "We look?", "We go?",
        "Again?", "More?", "More more?", "Another?", "Another one?", "Curi wonder...",
        "Curi wonder why.", "Curi wonder what.", "Curi need know!", "Curi must know!",
        "Curi curious!"
    )
    private val babySleepyLines = listOf(
        "Sleepy...", "Zzz...", "Night night...", "Yawn...", "Soft...", "Nap...", "Bed bed...",
        // v118 — BABY VOICE EXPANSION: sleepy.
        "Curi sleepy...", "Big yawn!", "Tiny yawn!", "Zzz... Curi...", "Cozy...",
        "Warm...", "Curi curl up...", "Curl curl...", "Curi nap?", "Nap time?",
        "Sleep now?", "Curi tired...", "Tiny tired...", "Paws tired...", "Brain tired...",
        "Curiosity sleepy...", "One more?", "One more peek...", "Then sleep.",
        "Curi stay...", "Curi here tomorrow.", "Good night...", "Night night, you...",
        "Prrr... sleepy.", "Mrrr... sleepy."
    )
    private val babyFocusedLines = listOf(
        "Shh...", "Quiet...", "Write!", "Think...", "Words!", "Paws still!", "Shh shh!"
    )
    private val babyBouncyLines = listOf(
        "Bounce!", "Zoom!", "More!", "Wiggle!", "Wheee!", "Jump!", "Up up!"
    )
    private val babyShyLines = listOf(
        "...Hi.", "Peek!", "Shy...", "Hid!", "Tiny hi...", "*peek*", "Hello...",
        // v118 — BABY VOICE EXPANSION: shy.
        "H-hi!", "Curi shy...", "Shy shy...", "Peek...", "Curi hiding.", "Hide hide.",
        "Don't look!", "...Okay look.", "You saw Curi!", "Hehe...", "Eeeh...",
        "Curi blush!", "Tiny wave!", "*tiny wave*", "Curi here...", "Curi maybe brave.",
        "Brave? Maybe.", "Curi try hi.", "Hi hi...", "Curi likes you... a little.",
        "Curi likes you lots.", "Don't tell!", "Secret!", "Curi secret!", "Hehe... shy."
    )
    private val babyGrumpyLines = listOf(
        "Hmph!", "Grumpy!", "Meh...", "No!", "Bored!", "Hmph hmph!", "Grr!",
        // v118 — BABY VOICE EXPANSION: grumpy.
        "Hmph hmph!", "Curi grumpy.", "Nooo!", "Curi no!", "Nope!", "Nuh-uh!",
        "Not fair!", "Hmph. Boring.", "Curi bored.", "So bored!", "Too quiet!",
        "No fun!", "Curi needs fun!", "Curi needs spin!", "Spin now?", "Please spin.",
        "Curi waiting.", "Curi waited long!", "Pfft!", "Curi pout.", "Tiny pout.",
        "Curi not pouting!", "Maybe pouting.", "...Fine.", "Okay fine!", "Curi forgive.",
        "Hehe... maybe."
    )
    private val babyPlayfulLines = listOf(
        "Play!", "Again!", "Boop!", "Chase!", "Zoom!", "Fun!", "More!",
        // v118 — BABY VOICE EXPANSION: play.
        "Play play!", "Curi play!", "Play with Curi!", "Again again!", "More!",
        "More more more!", "Chase Curi!", "Catch Curi!", "Curi fast!", "Too fast!",
        "Hehe, catch!", "Can't catch!", "Curi zoom!", "Zoom zoom zoom!", "Wheee!",
        "Bounce!", "Bounce bounce!", "Jump!", "Hop hop!", "Pounce!", "Pounce pounce!",
        "Tag!", "You're it!", "My turn!", "Your turn!", "Curi turn!", "Play more?",
        "Please?", "Pleeease?", "Curi wants play!", "Tiny game!", "Big game!",
        "Game time!", "Go go!", "Ready!", "Ready ready!", "Hehe! Go!"
    )
    // ── v118 — BABY VOICE EXPANSION (Curie-isms) ──────────────────────
    // The extra BABY-only lines from the canonical dialog doc, grouped by
    // theme and wired into the baby event lines + moods so every added
    // line is live in the app.
    private val babySaveLines = listOf(
        "Save!", "Save save!", "Keep!", "Keep keep!", "Ours!", "Ours ours!",
        "Curi keep!", "Curi guard!", "Safe!", "Safe safe!", "Shelf!", "Shelf shelf!",
        "Curi keep safe!", "Another treasure!", "Little treasure!", "Tiny treasure!",
        "Curi likes treasure!", "We keep this!", "This one stays!", "Don't lose!",
        "Curi remember!", "Little memory!", "Memory memory!", "Yay, saved!",
        "Saved saved!", "Keeper!", "Good keeper!", "Curi found keeper!"
    )
    private val babyTouchLines = listOf(
        "Boop!", "Boop boop!", "Boop boop boop!", "Curi boop!", "Boop Curi!",
        "More boop!", "More more!", "Again boop!", "Boop again!", "Tiny boop!",
        "Big boop!", "Soft boop!", "Nose boop!", "Pat pat!", "Pat pat pat!",
        "Curi likes pats!", "Mmm... pats.", "Hehe! Tickles!", "Tickly!",
        "Tickle tickle!", "Ooh! Tickles!", "That's Curi ear!", "My ear!", "Soft!",
        "So soft!", "Curi squeak!", "Squee!", "Mrrp!", "Purr-purr!", "Curi wiggle!",
        "Wiggle wiggle!", "Tiny squish!", "More cuddles?"
    )
    private val babyLevelUpLines = listOf(
        "Big Curi!", "Curi grow!", "Grow grow!", "Curi bigger!", "Look! Bigger!",
        "New Curi!", "Shiny Curi!", "Curi glow!", "Glow glow!", "Level up!",
        "Up up!", "Curi up!", "Yay! Level!", "Big level!", "Curi did it!",
        "We did it!", "Curi strong!", "Tiny strong!", "More spark!",
        "More more spark!", "Curi sparkle!", "Sparkle sparkle!", "Look at Curi!",
        "Curi proud!", "Proud Curi!", "Hehe! Big!"
    )
    private val babyEvolveLines = listOf(
        "Ooh! What happened?!", "Curi changed!", "Curi grow!", "Big Curi!",
        "New Curi!", "Look look!", "Curi glow!", "So shiny!", "Curi feels different!",
        "Curi got new spark!", "Spark bigger!", "Curi evolved!", "Evolved Curi!",
        "Wow!", "Woooow!", "Curi big now!", "Still Curi!", "Curi still me!",
        "New me! New me!", "Curi likes new me!", "Hehe! Look!"
    )
    private val babyExploreLines = listOf(
        "Go!", "Go go!", "Curi go!", "Explore!", "Explore explore!", "See!", "Look!",
        "Curi look!", "Find!", "Find find!", "New!", "New new!", "Adventure!",
        "Tiny adventure!", "Big adventure!", "Curi ready!", "Take Curi!",
        "Curi coming!", "Wait! Curi coming!", "Let's go!", "Come come!",
        "This way!", "That way!", "Which way?", "Ooh, there!", "Curi found!",
        "Something!", "Something there!", "What's hiding?", "Curi investigate!",
        "Curi look around!", "Curi curious!", "Curi very curious!"
    )
    private val babyDiscoveryLines = listOf(
        "Ooh! Topic!", "Curi found topic!", "New topic!", "Topic! Topic!",
        "Curi see new!", "What's this one?", "Ooh, this one!", "This one! This one!",
        "Good one!", "Curi likes this one!", "You picked! Yay!", "Curi approve!",
        "Good pick!", "Very good pick!", "Curi was hoping!", "Hehe, you picked it!",
        "We look now?", "Open open!", "Show Curi!", "Curi wants see!",
        "Curi ready peek!", "Peek now!", "Ooh... interesting!", "Hm! Interesting!",
        "Curi curious now!", "Brain awake!", "Curi brain go!", "Questions! Questions!",
        "So many questions!", "Curi needs answers!", "Let's see!", "Let's look!",
        "Curi look close!", "Closer! Closer!", "Ooh wow!", "Tiny wow!", "Big wow!"
    )
    private val babyMishapLines = listOf(
        "Aww...", "Aww, no!", "Curi missed!", "Missed!", "Oops!", "Oopsie!",
        "Curi oops!", "Uh-oh!", "Uh-oh uh-oh!", "Not got!", "Almost!", "So close!",
        "Curi almost!", "Again?", "Try again!", "Curi try again!", "Next time!",
        "Next next!", "It got away!", "Bye-bye spark...", "Curi sad...", "Tiny sad.",
        "Aww... come back!", "Curi wanted that!", "Hehe... oops.", "Little mistake.",
        "Curi okay!", "Curi try!"
    )
    // Curie sounds + tiny phrases + rare silly lines — the pure
    // "Curie-ism" voice, mixed into every BABY pick so the tiny-creature
    // energy shows through all of the baby's lines.
    private val babyCurieLines = listOf(
        "Curi!", "Curie!", "Curi-curi!", "Curiii!", "Curieee!",
        "Curi-cuu!", "Curi-pip!", "Curi-pip-pip!", "Curi-pi!",
        "Pruu!", "Prru!", "Mrru!", "Mrrp!", "Mip!", "Mipi!",
        "Pui!", "Pwee!", "Pupu!", "Pippi!", "Pipip!",
        "Bibi!", "Bibu!", "Bubu!", "Buu!", "Mimi!", "Mimu!",
        "Kiri!", "Kiri-kiri!", "Kiki!", "Kiki-kuri!",
        "Nyaa!", "Nyu!", "Nyuu!", "Mew?", "Mrr?",
        "Chuu!", "Chupi!", "Chup-chup!",
        "Poka!", "Poki!", "Poko!", "Poku!",
        "Tii!", "Titi!", "Tutu!", "Tuu!",
        "Wawa!", "Wiii!", "Wuu!", "Wee-wee!",
        "Hm?", "Hmmu?", "Huh?", "Eh?", "Eeeh?",
        "Ooh!", "Ooo!", "Ooooh!", "Ohi!",
        "Aha!", "Awu!", "Awwu!",
        "Hehe!", "Hihi!", "Ehehe!",
        "Pfft!", "Pff!", "Hmph!", "Hmp!",
        "Blep!", "Blip!", "Blup!", "Bloop!", "Boop!",
        "Squeak!", "Squee!", "Peep!", "Pip!",
        "Ziiip!", "Zoom!", "Vwoom!", "Whee!",
        "Prrr...", "Mrrr...", "Purr-purr!", "Mrrp!",
        "Curi! Look!", "Curi! Ooh!", "Curi wants!", "Curi go!", "Curi see!",
        "Curi peek!", "Curi found!", "Curi happy!", "Curi curious!", "Curi sleepy...",
        "Curi likes!", "Curi loves!", "Curi yes!", "Curi nooo!", "Curi wait!",
        "Curi here!", "Curi there!", "Curi got it!", "Curi did it!", "Curi win!",
        "Curi big!", "Curi grow!", "Curi glow!", "Curi shiny!", "Curi tiny!",
        "Curi fast!", "Curi zoom!", "Curi boop!", "Curi peek-peek!",
        "Curi curious-curious!", "Curi happy-happy!", "Curi go-go!", "Curi more-more!",
        "Curi again-again!", "Curi want more!", "Curi see more!", "Curi found you!",
        "Curi found it!", "Curi found something!", "Curi knows! Maybe.", "Curi thinks...",
        "Curi has idea!", "Curi big idea!", "Curi very curious!", "Curi super curious!",
        "Curi sparkle!", "Curi sparkle sparkle!", "Curi wiggle!", "Curi bounce!",
        "Curi pounce!", "Curi ready!",
        "Curi has no thoughts. Only sparkle.", "Curi brain go boop.",
        "Curi forgot what Curi was thinking.", "Curi was busy being tiny.",
        "Curi saw a dust.", "Dust suspicious.", "Curi has important mission.",
        "Mission: boop.", "Mission: discover!", "Curi found nothing. Found you instead!",
        "Curi approves!", "Curi does a wiggle.", "*wiggle wiggle*",
        "Curi has become pancake.", "Curi is tiny.", "Maximum tiny!", "Tiny mode!",
        "Curi zoom protocol!", "Sparkle mode!", "Boop mode!", "Sneaky mode!",
        "Sleepy mode...", "Curi loading...", "Curi thinking...", "Curi ready!",
        "Curi not ready!", "Curi ready now!", "Hehe. Curi.", "Curi says hi.",
        "Curi says bye.", "Curi says boop.", "Boop is important.", "Curi has decided.",
        "Decision: yes!", "Decision: more!", "Decision: again!", "Decision: boop!"
    )
    private val babySpinCheerLines = listOf(
        "Go go!", "Spin!", "Round!", "Wheee!", "Faster!", "Ooh!", "Go go!", "Spin spin!"
    )
    private val babyPeekLines = listOf(
        "Boo!", "Peek!", "Hid!", "Here!", "Peek boo!", "Found me!", "Surprise!", "Boo boo!"
    )
    private val babyChameleonLines = listOf(
        "Gone!", "Poof!", "Bye!", "Here!", "Sneaky!", "Hide!", "Where?", "Poof back!"
    )
    private val babySparkLines = listOf(
        "Spark!", "Mine!", "Shiny!", "Catch!", "Got!", "Zoom!", "Pounce!", "Spark!"
    )
    private val babyMorningLines = listOf(
        "Morn!", "Hi hi!", "Up!", "Sun!", "Morning!",
        "Hello!", "New day!", "Yay!"
    )
    private val babyWelcomeBackDayLines = listOf(
        "You back!", "Missed you!", "Yay!", "Home!", "You here!", "Back back!"
    )
    private val babyWelcomeBackDaysLines = listOf(
        "Long gone!", "Missed you!", "You back!", "Home home!", "Yay you!", "Back!"
    )
    private val babyWelcomeBackWeekLines = listOf(
        "Week!", "Missed!", "You back!", "Big hug!", "Long gone!", "Home home!"
    )

    /** v16 — the baby's vocabulary grows with its level. */
    private fun babyGrownWords(): List<String> {
        val lvl = CurioQuests.levelForXp(CurioQuests.xpState)
        return when {
            lvl >= 6 -> listOf("Look look! Big word: SPARKLE!", "I know more words now!", "Bigger me, more words!")
            lvl >= 4 -> listOf("Two words!", "I talk MORE now!", "New word: DECK!")
            else -> emptyList()
        }
    }

    /** v14 — a BABY mood bubble: 1-3 words, concrete, exclamation-led. */
    private fun babyMoodLine(mood: Mood): String = when (mood) {
        // v118 — babyCurieLines rides every mood so the Curie-isms surface.
        Mood.PROUD -> pickLine(babyProudLines + babyGrownWords() + babyLevelUpLines + babyCurieLines)
        Mood.EXCITED -> pickLine(babyExcitedLines + babyGrownWords() + babyCurieLines)
        Mood.HAPPY -> pickLine(babyHappyLines + babyGrownWords() + babyCurieLines)
        Mood.CURIOUS -> pickLine(babyCuriousLines + babyCurieLines)
        Mood.FOCUSED -> pickLine(babyFocusedLines + babyCurieLines)
        Mood.BOUNCY -> pickLine(babyBouncyLines + babyCurieLines)
        Mood.SHY -> pickLine(babyShyLines + babyCurieLines)
        Mood.GRUMPY -> pickLine(babyGrumpyLines + babyCurieLines)
        Mood.PLAYFUL -> pickLine(babyPlayfulLines + babyGrownWords() + babyCurieLines)
        Mood.SLEEPY -> pickLine(babySleepyLines + babyCurieLines)
    }

    /** v14 — a BABY reaction line for [event]. */
    private fun babyEventLine(event: Event): String = when (event) {
        Event.SPIN_LANDED -> pickLine(listOf(
            "Ooh!", "Landed!", "We got it!", "Spin spin!", "Yay!", "New!",
            "Ooh, this!", "Got one!", "Look!", "Again?"
        ) + babyCurieLines + babyDiscoveryLines)
        Event.REVEAL_TAPPED -> pickLine(listOf(
            "You pick!", "Ooh!", "Good one!", "Look!", "Yay, this!", "Picked!",
            "Mine too!", "Go look!"
        ) + babyCurieLines + babyDiscoveryLines)
        Event.REVEAL_AUTO -> pickLine(listOf(
            "Oh!", "Surprise!", "Ooh!", "Ta-da!", "It opened!", "Look look!",
            "New!", "What?!"
        ) + babyCurieLines + babyDiscoveryLines)
        Event.EXPLORE -> pickLine(listOf(
            "Go go!", "Explore!", "Bye!", "See!", "New things!", "Go!",
            "Adventure!", "Find!"
        ) + babyCurieLines + babyExploreLines)
        Event.SAVE -> pickLine(listOf(
            "Keep!", "Save!", "Ours!", "Yay!", "Mine!", "Safe!", "Save save!",
            "Got it!"
        ) + babyCurieLines + babySaveLines)
        Event.TOUCH -> pickLine(babyTouchLines + babyCurieLines)
        Event.PLAY -> pickLine(listOf(
            "Wheee!", "Again!", "Fun!", "Zoom!", "More!", "Play!", "Yay!",
            "Again again!"
        ) + babyCurieLines)
        Event.LEVEL_UP -> pickLine(listOf(
            "Big!", "Grow!", "Up!", "Yay!", "More!", "Big me!", "Glow!",
            "Grow grow!"
        ) + babyCurieLines + babyLevelUpLines)
        Event.EVOLVE -> pickLine(babyEvolveLines + babyCurieLines)
        Event.QUEST_COMPLETE -> pickLine(listOf(
            "Done!", "Yay!", "Win!", "We did!", "Finished!", "Good!",
            "Yay yay!", "All done!"
        ) + babyCurieLines)
        Event.STREAK_MILESTONE -> babyStreakLine(CurioQuests.bestStreakState)
    }

    /** v14 — a BABY streak line: the flame in tiny words. */
    private fun babyStreakLine(streak: Int): String = when (streak) {
        1 -> pickLine(listOf("Start!", "Day one!", "Fire!") + babyCurieLines)
        3 -> pickLine(listOf("Three!", "Three days!", "More!") + babyCurieLines)
        7 -> pickLine(listOf("Week!", "Big streak!", "Seven!") + babyCurieLines)
        14 -> pickLine(listOf("Two weeks!", "Many days!") + babyCurieLines)
        30 -> pickLine(listOf("Month!", "Big big!", "Thirty!") + babyCurieLines)
        else -> pickLine(listOf("Day $streak!", "More days!", "Still glow!") + babyCurieLines)
    }

    /** v14 — a BABY tap answer: boop → bounce → tiny celebration. */
    private fun babyTouchLine(tier: Int): String = when {
        tier >= 3 -> pickLine(listOf("Wheee!", "Yay yay!", "More!", "Boop boop!", "Again!") + babyCurieLines)
        tier >= 2 -> pickLine(listOf("Hehe!", "Zoom!", "Again!", "Bounce!") + babyCurieLines)
        else -> pickLine(listOf("Boop!", "Soft!", "Hehe!", "Again?") + babyCurieLines)
    }

    // ═══════════════════════════════════════════════════════════════════
    // v14.1 — MATURE voice (fully grown, Level 25 / FINAL_EVO)
    // A third register, distinct from the first evolution: calm, wise and
    // reflective where the evolved form is witty and eager. Longer,
    // compound sentences, philosophical and mentor-like observations, dry
    // gentle humor, sensory grounding in time and knowledge. Still warm -
    // this is a voice that has settled into itself, not a cold one.
    // Routed by currentStage() alongside the BABY and FIRST_EVO branches.
    // ═══════════════════════════════════════════════════════════════════
    private val matureSassyLines = listOf(
        "Again? Some questions deserve another look.",
        "You returned to it. I understand.",
        "Repetition can be its own kind of curiosity.",
        "I have seen this one before. I am still pleased you came back."
    )
    private val matureProudLines = listOf(
        "Level ${CurioQuests.levelForXp(CurioQuests.xpState)}. We have come a long way.",
        "Growth is easy to miss until you look back.",
        "Another level. Another little piece of the journey.",
        "I remember when we were just starting."
    )
    private val matureExcitedLines = listOf(
        "A new lane. I still get that little flutter.",
        "The unknown never really stops being exciting.",
        "Something new has arrived. Let us give it our attention.",
        "I wonder what we will find here."
    )
    private val matureHappyLines = listOf(
        "It is a good day to be curious.",
        "Some moments are enough simply because they are shared.",
        "Quiet happiness is still happiness.",
        "I am glad we found this little moment.",
        "A gentle day. A gentle spark.",
        "The world feels a little kinder when we notice it."
    )
    private val matureCuriousLines = listOf(
        "There is still a question waiting somewhere.",
        "Even familiar shelves can hide unfamiliar things.",
        "Curiosity always leaves one little door unopened.",
        "Let us look somewhere we have not looked before."
    )
    private val matureFocusedLines = listOf(
        "Take your time. Important thoughts rarely need rushing.",
        "I will be quiet until the words are ready.",
        "Give the thought room. It may surprise you.",
        "Some ideas need silence before they can speak."
    )
    private val matureBouncyLines = listOf(
        "A little playfulness does no harm.",
        "I still have a bit of bounce left.",
        "Even grown-up sparks need to play.",
        "That was good for me. I feel lighter."
    )
    private val matureShyLines = listOf(
        "You caught me being shy.",
        "Some greetings take a little courage.",
        "I am still learning how to be brave around new friends.",
        "I think I am glad you found me."
    )
    private val matureGrumpyLines = listOf(
        "Even little sparks have quiet moods.",
        "Perhaps the deck and I both need a change of scenery.",
        "I have been patient. Mostly.",
        "A little curiosity might improve my mood."
    )
    private val maturePlayfulLines = listOf(
        "I still have a playful thought or two.",
        "A little game? I would not object.",
        "Come on. Let us be silly for a moment.",
        "I suppose I am not finished having fun."
    )
    private val matureSleepyLines = listOf(
        "Rest now. Curiosity will still be here tomorrow.",
        "Even little sparks need to sleep.",
        "The questions can wait until morning.",
        "Good night. We have another day to discover."
    )
    private val matureSpinCheerLines = listOf(
        "Let it turn. Something will find us.",
        "The wheel is thinking. Let us be patient.",
        "Round and round. I wonder where we will land.",
        "Whatever arrives, we can give it a little attention."
    )
    private val maturePeekLines = listOf(
        "You found me. Well done.",
        "A little hiding makes a hello more fun.",
        "I was there all along.",
        "You looked in exactly the right place."
    )
    private val matureChameleonLines = listOf(
        "I have become very good at being unnoticed.",
        "Sometimes hiding is part of the game.",
        "Quiet enough, and even a spark can disappear.",
        "I will be here when you find me."
    )
    private val matureSparkLines = listOf(
        "A little spark. Let us see if I can catch it.",
        "Some tiny things are worth chasing.",
        "Quick now. It will not wait.",
        "A falling spark always looks like an invitation."
    )
    private val matureMorningLines = listOf(
        "Good morning. A new day gives us another chance to notice something.",
        "Morning. I wonder what today will bring us.",
        "A fresh day. Let us not rush past it.",
        "Good morning. I saved a little wonder for you."
    )
    private val matureWelcomeBackDayLines = listOf(
        "Welcome back. I am glad to see you again.",
        "The shelf felt a little quieter without you.",
        "There you are. It is nice to have you back.",
        "A day away, and here we are again."
    )
    private val matureWelcomeBackDaysLines = listOf(
        "Welcome home. I kept your little corner safe.",
        "You were away for a while. I am glad the quiet is over.",
        "The shelf waited patiently. I tried to.",
        "You are back. Let us begin again, gently."
    )
    private val matureWelcomeBackWeekLines = listOf(
        "A whole week. Welcome back.",
        "The shelf kept your place while you were away.",
        "You have returned. Some things are simply better with company.",
        "A week is a long pause. I am glad we are here again."
    )

    /** v14.1 — a MATURE mood bubble: calm, reflective, mentor-like. */
    private fun matureMoodLine(mood: Mood): String = when (mood) {
        Mood.PROUD -> pickLine(matureProudLines)
        Mood.EXCITED -> pickLine(matureExcitedLines)
        Mood.HAPPY -> pickLine(matureHappyLines)
        Mood.CURIOUS -> pickLine(matureCuriousLines)
        Mood.FOCUSED -> pickLine(matureFocusedLines)
        Mood.BOUNCY -> pickLine(matureBouncyLines)
        Mood.SHY -> pickLine(matureShyLines)
        Mood.GRUMPY -> pickLine(matureGrumpyLines)
        Mood.PLAYFUL -> pickLine(maturePlayfulLines)
        Mood.SLEEPY -> pickLine(matureSleepyLines)
    }

    /** v14.1 — a MATURE reaction line for [event]. */
    private fun matureEventLine(event: Event): String = when (event) {
        Event.SPIN_LANDED -> pickLine(listOf(
            "There we are. Let us see what found us.",
            "A new landing. A new little question.",
            "The wheel has chosen. Shall we look closer?",
            "Wherever we land, there is usually something worth noticing."
        ))
        Event.REVEAL_TAPPED -> pickLine(listOf(
            "A thoughtful choice. I think you will enjoy this one.",
            "You followed your curiosity. Good.",
            "That one caught your attention for a reason.",
            "A good question often begins with a simple tap."
        ))
        Event.REVEAL_AUTO -> pickLine(listOf(
            "It opened on its own. A pleasant surprise.",
            "Sometimes curiosity arrives before we ask for it.",
            "Well, then. This is what found us today.",
            "An unexpected little door. Let us peek inside."
        ))
        Event.EXPLORE -> pickLine(listOf(
            "Go on. There is always something worth noticing.",
            "Explore gently. Wonder has no need to hurry.",
            "Bring back whatever catches your eye.",
            "The world is full of small discoveries. Go find one."
        ))
        Event.SAVE -> pickLine(listOf(
            "Kept. Some discoveries deserve a place to return to.",
            "Safe on the shelf. We can find it again whenever we wish.",
            "Another memory tucked away.",
            "It is nice to have somewhere for the things that matter."
        ))
        Event.TOUCH -> pickLine(listOf(
            "A gentle boop. I approve.", "Hm. That was nice.",
            "You found the right spot.", "I do not mind being fussed over."
        ))
        Event.PLAY -> pickLine(listOf(
            "That was lovely. I still have a little playfulness left.",
            "One more round? I suppose I can spare one.",
            "Play makes even an old spark feel young.",
            "Very well. One more. Then perhaps another."
        ))
        Event.LEVEL_UP -> pickLine(listOf(
            "Another level. I can feel how far we have come.",
            "A little more growth, a little more to discover.",
            "Up we go. I am glad you are here for it.",
            "Every small step changes us."
        ))
        Event.EVOLVE -> pickLine(listOf(
            "So this is who I have become. I like her.",
            "I grew, but I never left the little spark behind.",
            "We changed together. I think that is the nicest part.",
            "Every little stage brought me here."
        ))
        Event.QUEST_COMPLETE -> pickLine(listOf(
            "Done. Another promise kept.",
            "A small task finished is still progress.",
            "Well done. One more thing carried across the line.",
            "Completed together. I am glad we did it."
        ))
        Event.STREAK_MILESTONE -> matureStreakLine(CurioQuests.bestStreakState)
    }

    /** v14.1 — a MATURE streak line: the flame, spoken with quiet pride. */
    private fun matureStreakLine(streak: Int): String = when (streak) {
        1 -> pickLine(listOf("Every streak begins with one day.", "Day one. A small beginning."))
        3 -> pickLine(listOf("Three days. A rhythm is forming.", "Day three. Keep going."))
        7 -> pickLine(listOf("A week of showing up. That matters.", "Seven days. A little habit is becoming a tradition."))
        14 -> pickLine(listOf("Two weeks. The spark has settled into a rhythm.", "Fourteen days. Steady is beautiful."))
        30 -> pickLine(listOf("A month of curiosity. That is something worth keeping.", "Thirty days. You made the spark part of your days."))
        else -> pickLine(listOf(
            "Day $streak. One little day at a time.", "$streak days. The rhythm continues."
        ))
    }

    /** v14.1 — a MATURE tap answer: calm acknowledgment, then delight. */
    private fun matureTouchLine(tier: Int): String = when {
        tier >= 3 -> pickLine(listOf(
            "Hehe. I am thoroughly spoiled.", "That was lovely.",
            "I could become accustomed to this."
        ))
        tier >= 2 -> pickLine(listOf(
            "A playful mood, I see.", "That was nice.",
            "You are persistent. I approve."
        ))
        else -> pickLine(listOf(
            "A gentle boop. Thank you.", "Hm. Nice.", "Yes. That spot."
        ))
    }

    /**
     * v14.1 — a MATURE line for a Pet Life routine: the fully grown pet
     * replaces the youthful routine lines (PetLife.kt) with its own calm
     * register. Unknown ids return null, so the routine plays as pure
     * motion rather than speaking out of voice.
     */
    fun matureRoutineLine(routineId: String): String? = when (routineId) {
        "look-around" -> pickLine(listOf("Hmm... what should we notice today?"))
        "little-wave" -> pickLine(listOf("Hi from over here."))
        "stretch" -> pickLine(listOf("A little stretch. Then we explore."))
        "turn-and-peek" -> pickLine(listOf("Did I see something? Nope. Just me."))
        "tiny-stumble" -> pickLine(listOf("Oops. I meant to do that."))
        "look-up" -> pickLine(listOf("I wonder what is up there."))
        "backstage" -> pickLine(listOf("One tiny look in the mirror. Hehe."))
        "victory-pose" -> pickLine(listOf("We did it! Tiny victory pose."))
        "home-stretch" -> pickLine(listOf("Home. Cozy."))
        "room-tour" -> pickLine(listOf("Come on. I'll show you around."))
        "window-watch" -> pickLine(listOf("The view is nice today."))
        "cozy-turn" -> pickLine(listOf("Just checking my cozy little corner."))
        "home-dance" -> pickLine(listOf("A tiny dance for no reason."))
        "deck-anticipation" -> pickLine(listOf("Ooh... it's thinking!"))
        "deck-side-peek" -> pickLine(listOf("I wonder what it's going to choose."))
        "deck-stretch" -> pickLine(listOf("Ready? Let's spin."))
        "deck-victory" -> pickLine(listOf("Ooh! Good landing!"))
        "quest-read" -> pickLine(listOf("Hmm... let me see what we have to do."))
        "quest-wave" -> pickLine(listOf("We can do this!"))
        "quest-proud" -> pickLine(listOf("Look at us go."))
        "quest-hide" -> pickLine(listOf("Hehe... mysterious pet mode."))
        "topic-peek" -> pickLine(listOf("Can we peek?"))
        "topic-wow" -> pickLine(listOf("Ooh... pretty interesting."))
        "topic-spin" -> pickLine(listOf("That was a good one!"))
        "topic-inspect" -> pickLine(listOf("Hmm. Let's look closer."))
        "writing-focus" -> pickLine(listOf("I'll keep watch while you think."))
        "writing-wave" -> pickLine(listOf("Take your time. I'm here."))
        "writing-stretch" -> pickLine(listOf("Tiny stretch, then back to your words."))
        "writing-shy" -> pickLine(listOf("This thought looks important. I'll be quiet."))
        "shelf-hunt" -> pickLine(listOf("Which little memory should we visit?"))
        "shelf-wave" -> pickLine(listOf("Our shelf is looking lovely."))
        "shelf-backstage" -> pickLine(listOf("Hehe, I'm checking the back row."))
        "shelf-proud" -> pickLine(listOf("Look at all the things we've found."))
        "mirror-check" -> pickLine(listOf("Do I look cute? ...Yes."))
        "profile-wave" -> pickLine(listOf("Hi, little profile."))
        "profile-proud" -> pickLine(listOf("Look how much you've grown."))
        "profile-look-up" -> pickLine(listOf("Another level is waiting. Let's go."))
        else -> null
    }

    /** A passive bubble line for the current [mood]. */
    fun lineFor(context: Context, mood: Mood, lanes: Set<String>): String {
        // v14 — the BABY voice: limited telegraphic pools, no time-of-day
        // or bond variants (a baby says the same few things all day). The
        // fully grown pet speaks its calm mature register everywhere.
        when (currentStage()) {
            Stage.BABY -> return babyMoodLine(mood)
            Stage.FINAL_EVO -> return matureMoodLine(mood)
            Stage.FIRST_EVO -> Unit
        }
        // Inlined so the level reads live, not baked at first access.
        return when (mood) {
            // v118 — the level reads LIVE (not baked at first access), so
            // the number is right every time the pool is used.
            Mood.PROUD -> pickLine(listOf(
                "Look! Level ${CurioQuests.levelForXp(CurioQuests.xpState)}!",
                "I grew again! Did you see?",
                "Hehe, I'm getting bigger.",
                "Level ${CurioQuests.levelForXp(CurioQuests.xpState)}! That's ours.",
                "I feel extra sparkly today.",
                "Another level! I'm proud of us.",
                "${CurioQuests.levelForXp(CurioQuests.xpState)} already? Wow!",
                "I can feel myself growing.",
                "Look at my little glow!",
                "We did that together."
            ))
            Mood.EXCITED -> pickLine(excitedLines)
            Mood.HAPPY -> when (timeOfDay()) {
                // v8.29 — strangers hear the polite lines; friends+ hear the
                // warmer twins.
                TimeOfDay.MORNING -> pickLine(if (isWarm()) warmMorningLines else morningLines)
                TimeOfDay.AFTERNOON -> pickLine(if (isWarm()) warmAfternoonLines else afternoonLines)
                TimeOfDay.EVENING -> pickLine(if (isWarm()) warmEveningLines else eveningLines)
                TimeOfDay.NIGHT -> pickLine(if (isWarm()) warmNightLines else nightLines)
            }
            Mood.CURIOUS -> {
                val lane = leastExploredLane(context, lanes)
                if (lane != null) {
                    pickLine(curiousLines.map { it.replace("__LANE__", lane.displayName) })
                } else {
                    "Want to find something new today?"
                }
            }
            Mood.FOCUSED -> pickLine(focusedLines)
            Mood.BOUNCY -> pickLine(bouncyLines)
            Mood.SHY -> pickLine(shyLines)
            Mood.GRUMPY -> pickLine(grumpyLines)
            Mood.PLAYFUL -> pickLine(playfulLines)
            Mood.SLEEPY -> pickLine(sleepyLines)
        }
    }

    /** A short cheer while the Spin deck is reeling (v8.13). */
    fun spinCheer(): String =
        // v14 — the BABY cheers in short bursts; the fully grown pet cheers
        // with steady calm.
        when (currentStage()) {
            Stage.BABY -> pickLine(babySpinCheerLines)
            Stage.FINAL_EVO -> pickLine(matureSpinCheerLines)
            Stage.FIRST_EVO -> {
                // v16 — the cheer sometimes calls the lane by name.
                val lane = lastLaneName
                if (lane != null && kotlin.random.Random.nextFloat() < 0.3f) {
                    pickLine(listOf(
                        "Come on, $lane! Show us something good!",
                        "Ooh, $lane! Pick a good one!",
                        "$lane! I'm watching!",
                        "Go, $lane! Let's see what we get!",
                        "Hehe, $lane is spinning!"
                    ))
                } else {
                    pickLine(listOf(
                        "Go go go!",
                        "Spinny spin!",
                        "Ooh! Where will it land?",
                        "Come on, little deck!",
                        "Round and round!",
                        "I can't look! ...I'm looking.",
                        "Ooh ooh ooh!",
                        "Pick a good one!",
                        "Faster! Hehe!",
                        "What's it going to be?",
                        "Almost!",
                        "I think it's choosing!",
                        "Come on, come on!",
                        "One good topic, please!",
                        "Wheee! Spin!"
                    ))
                }
            }
        }

    /**
     * A short burst when the user touches/pets the floating pet (v8.11).
     * [tier] grows with rapid repeated taps (computed by the overlay from
     * the tap streak): 1 = a soft boop, 2 = playful, 3+ = a happy
     * celebration. v8.21 — the DIZZY tier moved to DRAGGING (the pet gets
     * flung around), so tapping never spins it anymore — rapid taps just
     * escalate from boop to play-bow to a big happy bounce.
     */
    fun touchReaction(tier: Int): String {
        // v14 — the BABY voice answers taps with tiny exclamations; the
        // fully grown pet answers with calm acknowledgment.
        when (currentStage()) {
            Stage.BABY -> return babyTouchLine(tier)
            Stage.FINAL_EVO -> return matureTouchLine(tier)
            Stage.FIRST_EVO -> Unit
        }
        return when {
            // v8.29 — intimacy scales with the bond: "Best friends!" and "You're
            // my favorite!" only at CLOSE; FRIEND gets loving-but-restrained;
            // strangers just celebrate happily.
            tier >= 3 -> when {
                bond() == Bond.CLOSE -> pickLine(listOf(
                    "Yay! More boops!",
                    "Hehe, I love this!",
                    "Best boop buddy!",
                    "You're my favorite!",
                    "Squee!",
                    "More, more, more!",
                    "Party paws!",
                    "Hehe! You know exactly where to tap.",
                    "I could get used to this.",
                    "Boop attack!",
                    "You're very good at this.",
                    "Tiny hugs!",
                    "Again! Please!",
                    "I like being your little pet.",
                    "Okay, okay, one more!"
                ))
                isWarm() -> pickLine(listOf(
                    "Yay!",
                    "I love boops!",
                    "Hehe!",
                    "More, please!",
                    "Squee!",
                    "Boop buddy!",
                    "This is fun!",
                    "You found my spot again!",
                    "Hehe, you're good at that.",
                    "One more?",
                    "I like this.",
                    "Tiny pats!",
                    "Again again!"
                ))
                else -> pickLine(listOf(
                    "Ooh!", "Boop!", "Hehe!", "That tickles!", "Again?",
                    "Wheee!", "Squee!", "That's fun!", "More?", "Tiny boop!"
                ))
            }
            tier >= 2 -> pickLine(listOf(
                "Hehehe!", "Again!", "More more!", "That tickles!", "Boop boop!",
                "Hehe, fun!", "Catch me!", "Wiggle wiggle!", "Zoom!", "Poke poke!",
                "You found me!", "Again again!", "I like this!"
            ))
            else -> pickLine(listOf(
                "Boop!", "Hehe!", "Ooh!", "That tickles!", "Hi hi!",
                "Boop boop!", "Again?", "Poke!", "Soft pats!", "Mrow!",
                "Blep!", "Squeak!", "That's my ear!", "Hehehe!", "Tiny boop!"
            ))
        }
    }

    /**
     * The pet sometimes starts a game on its own (v8.11) — it play-bows,
     * calls out, then zooms off. Kept short: one sentence max.
     */
    fun playInitiation(): String = pickLine(listOf(
        "Wanna play with me?",
        "Catch me!",
        "Boop! You're it!",
        "Hehe, chase me!",
        "Play with me!",
        "Tag! Your turn!",
        "I have zoomies!",
        "Come on! Just one game.",
        "I bet you can't catch me.",
        "I saw something! We should chase it.",
        "Pounce mode: ready!",
        "Game time!",
        "I made a game plan. It's mostly running.",
        "Chase me! Chase me!",
        "I need someone to play with.",
        "Come on, tiny adventure?"
    ))

    /**
     * v8.16 — the pet's line when it pokes something on the current screen:
     * [funThing] = a button/gadget (boop! hearts!), otherwise a curious
     * read of some text. One sentence max, matching the passive-bubble rule.
     */
    fun landmarkLine(funThing: Boolean): String =
        (if (funThing) pickLine(listOf(
            "Boop!",
            "Ooh, shiny!",
            "Hehe, what does this do?",
            "I found a button!",
            "Can I press it again?",
            "Bloop!",
            "Squeak!",
            "Spinny!",
            "Ooh! I like this.",
            "Boop boop boop!",
            "It booped back!",
            "Hehe, I touched it.",
            "This button is suspicious.",
            "I approve of this gadget."
        )) else pickLine(listOf(
            "What's this?",
            "Ooh, words!",
            "Can I read too?",
            "*peeks*",
            "Hmm... interesting.",
            "Read read read!",
            "So many letters!",
            "Let me see!",
            "I'm looking!",
            "This says something important. Probably.",
            "Ooh, I found a word!",
            "My little brain is busy.",
            "Paws can't turn pages. Tragic.",
            "Hehe, I like learning things."
        )))

    /**
     * v8.17 — the pet's line when it does a little jig at a special spot
     * (a PLAY landmark). One sentence max, matching the passive-bubble rule.
     */
    fun jigLine(): String = pickLine(listOf(
        "Tippy tap tap!",
        "Happy feet!",
        "Wiggle wiggle!",
        "Dance with me!",
        "Hehe, look at me go!",
        "Tiny dance!",
        "Party paws!",
        "Shake shake!",
        "Da-da-daaa!",
        "I have a groove!",
        "Twinkle toes!",
        "Wiggle time!",
        "Hehe! Again!",
        "I call this the sparkly shuffle."
    ))

    /**
     * v8.21 — the pet got flung around (dragged) and is dizzy: swirl eyes,
     * a wobble, and a groggy line while it recovers.
     */
    fun dizzyLine(): String = pickLine(listOf(
        "Whoa... everything is spinning!",
        "Wheee! ...Wait, stop!",
        "My head is doing circles.",
        "Who put the room on a spin cycle?",
        "I need a tiny sit-down.",
        "Whoa whoa whoa!",
        "The floor moved! I think.",
        "I'm dizzy!",
        "Hehe... maybe don't do that again.",
        "My paws forgot which way is down.",
        "Everything has become a carousel.",
        "Give me a second... okay, maybe three.",
        "My brain is still spinning.",
        "I would like the floor to behave now."
    ))

    /**
     * v8.21 — a bottom drawer (filter / category sheet) just opened and the
     * pet hurried over to peek at it from the bottom edge.
     */
    fun drawerLine(): String = pickLine(listOf(
        "Ooh! What's in here?",
        "So many choices!",
        "Can I pick one?",
        "Ooh, look at all these!",
        "Which one should we choose?",
        "Hehe, choices!",
        "Can I peek?",
        "I want to see!",
        "So many little options!",
        "Ooh, a secret menu!",
        "Let's choose together.",
        "My paws are ready for picking.",
        "Which one looks fun?",
        "Hmm... too many good choices!"
    ))

    // ── Fun games (v9.x) ────────────────────────────────────────────────
    // Lines for the little autonomous games the floating pet plays: peek-a-
    // boo around buttons, vanishing like a chameleon, and dashing after
    // falling sparks. All through [pickLine] so games never repeat lines.

    /** The pet ducks behind a button, then pops out (hide-and-peek). */
    fun peekLine(): String =
        // v14 — the BABY plays peek in one or two words; the fully grown
        // pet plays it with practiced calm.
        when (currentStage()) {
            Stage.BABY -> pickLine(babyPeekLines)
            Stage.FINAL_EVO -> pickLine(maturePeekLines)
            Stage.FIRST_EVO -> pickLine(listOf(
                "Peek-a-boo!",
                "I see you!",
                "Hehe, found me!",
                "Boo! ...Cute boo.",
                "Peek! Peek!",
                "You can't see me! ...Oh. You can.",
                "Surprise!",
                "Hehe, I'm over here!",
                "Sneak sneak... hi!",
                "I was hiding!",
                "Did you miss me?",
                "Boop from my hiding spot!",
                "You found me already?!",
                "Hehe! I wasn't ready!"
            ))
        }

    /** The pet fades into the background like a chameleon, then pops back. */
    fun chameleonLine(): String =
        // v14 — the BABY fades with a tiny word; the fully grown pet fades
        // with practiced stillness.
        when (currentStage()) {
            Stage.BABY -> pickLine(babyChameleonLines)
            Stage.FINAL_EVO -> pickLine(matureChameleonLines)
            Stage.FIRST_EVO -> pickLine(listOf(
                "Shhh... I'm hiding!",
                "Can you see me?",
                "Hehe, camouflage!",
                "I'm part of the background now.",
                "Poof! Gone!",
                "Can you find me?",
                "I'm being sneaky.",
                "You almost saw me!",
                "Now you see me... now you don't.",
                "Hiding is fun.",
                "Hehe, I'm invisible-ish.",
                "Did I disappear?",
                "Sneak mode!",
                "I'm blending in!"
            ))
        }

    /** The pet darts after a falling spark and catches it. */
    fun sparkLine(): String =
        // v14 — the BABY chases sparks with one small shout; the fully
        // grown pet gives chase with quiet purpose.
        when (currentStage()) {
            Stage.BABY -> pickLine(babySparkLines)
            Stage.FINAL_EVO -> pickLine(matureSparkLines)
            Stage.FIRST_EVO -> pickLine(listOf(
                "A spark! Get it!",
                "Ooh, shiny!",
                "Catch catch catch!",
                "Mine! ...Maybe.",
                "Zoom!",
                "It's falling! Go go go!",
                "Hehe, I'm chasing it!",
                "One tiny spark!",
                "I almost had it!",
                "Got it! ...Wait, did I?",
                "Faster!",
                "Spark chase!",
                "It can't escape me!",
                "Hehe, come back, little spark!",
                "Left a trail of sparkle dust!",
                "Sky snacks!",
                "I'm the fastest spark catcher in town!"
            ))
        }

    // ── Interactive game lines (v16) — play WITH the user ─────────────
    // The spark-catch and chameleon games now involve the user: the spark
    // can be tapped, the hidden pet can be found. Each moment gets its own
    // small pool, routed by growth stage like everything else.

    /** The pet dares the user to find it (chameleon hide). */
    fun findMePromptLine(): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Find!", "Here!", "Peek! Find!") + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "Find me when you are ready.", "I will wait somewhere new."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "Find me!",
            "Peek! Come find me!",
            "I'm hiding! Can you see me?",
            "Hehe, catch me if you can!",
            "Where am I?",
            "I'm somewhere sneaky!",
            "Come find your little pet!",
            "I picked a very good hiding spot.",
            "Behind the screen edge! No, wait...",
            "I am so sneaky right now."
        ))
    }

    /** The user found the hidden pet. */
    fun foundMeLine(): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Found!", "You! Here!", "Boo! You!") + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "You found me. Well done.", "Found already. You are getting good at this."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "You found me!",
            "Boo! You got me!",
            "Hehe, caught!",
            "You found me already?!",
            "Okay, okay, you win!",
            "Sneaky! You saw me!",
            "Found! Nice one.",
            "Hehe, I was trying to be sneaky.",
            "A whole corner and you still found me!",
            "Rematch! I'll be sneakier."
        ))
    }

    /** The user caught the falling spark. */
    fun caughtItLine(): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Got!", "Spark! We got!", "Yay!") + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "Caught together. Well done.", "The spark chose us."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "Got it! We did it!",
            "Yay! We caught it!",
            "Teamwork!",
            "You helped!",
            "Hehe, we got the spark!",
            "It didn't stand a chance against us!",
            "We caught it together!",
            "Spark caught! High five!",
            "Together we're unstoppable!",
            "It never saw us coming!"
        ))
    }

    /** The spark got away (timeout). */
    fun gotAwayLine(): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Bye spark...", "Got? No.", "Next!") + babyMishapLines + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "It got away. Another will come.", "Some sparks are simply quick."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "Aww... it got away.",
            "So close!",
            "Hehe, next time!",
            "It was too quick!",
            "We almost had it!",
            "Sneaky little spark.",
            "We'll get the next one.",
            "Rematch?",
            "It drifted off on purpose, I'm sure.",
            "That one was extra wiggly!"
        ))
    }

    /** The user caught the pet mid-peek. */
    fun peekWinLine(): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Boo! You!", "Hid! Found!", "Hehe!") + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "You caught me mid-peek. Fair play.", "Well timed. You found me."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "Boo! You caught me!",
            "Hehe, you got me!",
            "Peek-a-boo! You win!",
            "I wasn't ready!",
            "Sneak interrupted!",
            "You found me mid-peek!",
            "Hehe! Nice timing.",
            "Okay, you caught me.",
            "Busted! I was peeking.",
            "My peek betrayed me!"
        ))
    }

    /** The user missed the peeking pet (hide-and-seek timeout). */
    fun missedMeLine(): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Here!...Missed!", "Peek! You missed!", "Boo...no. Hehe!") + babyMishapLines + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "You looked away at just the wrong moment.", "Nearly. Try again when you are ready."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "Hehe! You missed me!",
            "Too slow!",
            "I was right there!",
            "Peek! ...Oops, missed!",
            "Better luck next time!",
            "I even waved!",
            "Almost!",
            "Hehe, you blinked!",
            "I was camouflaged! Well, sort of.",
            "The edge hid me best!"
        ))
    }

    /** The pet invites the user to pop rising bubbles (POP! round). */
    fun popPromptLine(): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Pop! Pop!", "Bubbles! Pop them!", "Up! Pop!") + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "Bubbles are rising. Mind the prickly ones.",
            "Shiny bubbles are rising; I could use a spare tap or two."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "Bubbles! Pop them!",
            "Pop pop pop!",
            "Ooh, bubble round!",
            "The shiny ones pop nicely!",
            "Catch them before they float away!",
            "I love a good bubble pop!",
            "Pop the shiny ones, but mind the prickly!",
            "Bubbles everywhere!",
            "They tickle when they pop near me!"
        ))
    }

    /** A BIG bubble round — the pet is delighted by the score. */
    fun popNiceLine(count: Int): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Pop! Yay!", "So many pop!", "Hehe! Big round!") + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "A fine round of popping. Well played.",
            "You are a natural bubble popper."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "Wheee! $count bubbles!",
            "Pop-tastic! We got $count!",
            "Look at all those pops! $count!",
            "Best bubble day ever!",
            "High five for $count pops!",
            "Not a single bubble escaped us!",
            "The bubbles never stood a chance!"
        ))
    }

    /** A prickly bubble caught the pet (score penalty). */
    fun popPrickleLine(): String = when (currentStage()) {
        Stage.BABY -> pickLine(listOf("Ow! Spike!", "Hiss! Ouch!") + babyMishapLines + babyCurieLines)
        Stage.FINAL_EVO -> pickLine(listOf(
            "Prickly. I should have read the label.",
            "Ow. That one was not friendly."
        ))
        Stage.FIRST_EVO -> pickLine(listOf(
            "Ow! Prickly!",
            "That one bit back!",
            "Yikes! A spike bubble!",
            "No fair, it had spikes!",
            "Ouch! My nose!",
            "Spiky! I won't pop that one again."
        ))
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
        // v8.43 — every spoken screen visit feeds the learning brain (the
        // time-of-day histogram + trait decay), then the brain's
        // personalized voice gets first say; the classic rule-based library
        // answers until the brain has enough signal (spec §10.6 fallback).
        CurioPetBrain.observeActivity(context, timeOfDay())
        val currentMood = mood(context, lanes, screen)
        // v16 — a memory-flavored line sometimes replaces the mood bubble so
        // the pet references real facts (weekly saves, the streak, seasons,
        // the hatch-day anniversary). Evolved voice only — the baby speaks
        // telegraphic pools and the mature voice keeps its calm register.
        if (currentStage() == Stage.FIRST_EVO && kotlin.random.Random.nextFloat() < 0.3f) {
            factLine(context)?.let { return it }
        }
        // v14 — the BABY voice is telegraphic and the fully grown voice is
        // its own mature register: neither composes the brain's stats
        // sentences, so both always speak from their own pools.
        return when (currentStage()) {
            Stage.BABY, Stage.FINAL_EVO -> lineFor(context, currentMood, lanes)
            Stage.FIRST_EVO -> CurioPetBrain.say(context, currentMood, lanes)
                ?: lineFor(context, currentMood, lanes)
        }
    }

    // ── Memory + rare moments (v16) — the pet references real facts ────
    private val weekdayLines = listOf(
        "Busy day? Let's sneak in a little curiosity.",
        "Even busy days can have one tiny wonder."
    )
    private val weekendLines = listOf(
        "Weekend! More time for little adventures.",
        "Slow day, curious brain. Perfect."
    )
    private val seasonSpringLines = listOf(
        "Spring! Everything feels new again.",
        "Ooh, everything is waking up!"
    )
    private val seasonSummerLines = listOf(
        "Summer glow! Even the long days feel curious.",
        "Warm days make me want to explore."
    )
    private val seasonAutumnLines = listOf(
        "Ooh, autumn! Everything looks cozy.",
        "The leaves are changing. I want to look at everything."
    )
    private val seasonWinterLines = listOf(
        "Winter cozy! Come sit with me.",
        "Cold outside, cozy little shelf inside."
    )
    private val hatchDayLines = listOf(
        "It's my hatch day! Can we celebrate?",
        "Another year with you! Yay!",
        "It's my hatch day! I feel extra sparkly.",
        "A whole year of little discoveries together.",
        "It's my birthday-ish! Hehe!"
    )

    private fun season(): Int =
        java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) / 3 + 1 // 1..4

    /**
     * v16 — a memory-flavored line from real facts: weekly saves, an active
     * streak, the season, weekday/weekend, the last saved topic, or the
     * pet's hatch-day anniversary. Returns null when there's nothing to say.
     */
    fun factLine(context: Context): String? {
        // The hatch day only comes around once a year — make it count.
        if (kotlin.random.Random.nextFloat() < 0.5f) {
            val birthday = AppPreferences.petBirthdayEpochDay(context)
            val today = java.util.Calendar.getInstance().timeInMillis / 86_400_000L
            if (today == birthday) return pickLine(hatchDayLines)
        }
        if (CurioQuests.bestStreakState >= 3 && kotlin.random.Random.nextFloat() < 0.25f) {
            val streak = CurioQuests.bestStreakState
            return pickLine(listOf(
                "Day $streak! We're still going!",
                "$streak days! I love our little rhythm.",
                "Streak $streak! Look at us!",
                "$streak days of curiosity. That's a lot of little sparks.",
                "Day $streak! Keep the glow going."
            ))
        }
        val weekly = AppPreferences.weeklySaveSummary(context)
        if (weekly.isNotEmpty()) {
            val (lane, count) = weekly.first()
            if (count >= 2) {
                return pickLine(listOf(
                    "We saved $count $lane keepsakes this week!",
                    "$count little $lane treasures! Wow.",
                    "Our $lane shelf got $count new friends.",
                    "$count for $lane this week. I like that.",
                    "$lane is having a very good week."
                ))
            }
        }
        if (kotlin.random.Random.nextFloat() < 0.3f) {
            val seasonPool = when (season()) {
                1 -> seasonSpringLines
                2 -> seasonSummerLines
                3 -> seasonAutumnLines
                else -> seasonWinterLines
            }
            return pickLine(seasonPool)
        }
        if (kotlin.random.Random.nextFloat() < 0.3f) {
            val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            val isWeekend = day == java.util.Calendar.SATURDAY || day == java.util.Calendar.SUNDAY
            return pickLine(if (isWeekend) weekendLines else weekdayLines)
        }
        val topic = lastSavedTopicName
        if (topic != null && kotlin.random.Random.nextFloat() < 0.4f) {
            return pickLine(listOf(
                "\"$topic\" is still one of my favorites.",
                "I keep thinking about \"$topic\".",
                "Last keeper: $topic. Good choice.",
                "\"$topic\" was a good one, wasn't it?",
                "I remember \"$topic\". I'd peek at that again."
            ))
        }
        return null
    }

    /** What tapping the pet reveals: mood, personality, growth status. */
    data class TapInfo(
        val mood: Mood,
        val stage: Stage,
        val persona: Persona,
        val nextStageLabel: String,
        val nextQuestTitle: String?,
        // v14 — the catchphrases the learning brain coined from the user's
        // habits (empty = the pet hasn't found its own words yet).
        val coinedPhrases: List<String>
    )

    fun tapInfo(context: Context, lanes: Set<String>): TapInfo {
        val stage = currentStage()
        return TapInfo(
            mood = mood(context, lanes),
            stage = stage,
            persona = persona(context),
            nextStageLabel = nextStageHint(stage),
            nextQuestTitle = CurioQuests.currentQuest()?.title,
            coinedPhrases = CurioPetBrain.coinedSayings(context)
        )
    }

    // ── Wakefulness / spin watching (v8.13) ───────────────────────────
    // [spinning] flips true the moment the Spin deck starts reeling and
    // false when it settles — the floating pet cheers it on while it turns.
    var spinning by mutableStateOf(false)
        private set

    /** The Spin screen sets this around its shuffle animation. */
    fun noteSpinning(value: Boolean) {
        spinning = value
    }

    // ── Activity hooks (called from CurioQuests) ───────────────────────
    // v8.43 — each real action also feeds the learning brain, so the pet's
    // personality genuinely grows from what the user does.
    fun noteXpEarned(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_XP_AT, System.currentTimeMillis()).apply()
        CurioPetBrain.observeXp(context)
    }

    fun noteLevelUp(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_LEVEL_AT, System.currentTimeMillis()).apply()
        CurioPetBrain.observeLevelUp(context)
        // v9.2 — leveling up fires the pet's LEVEL_UP reaction.
        reactTo(Event.LEVEL_UP)
    }

    /**
     * v13 — the pet evolved: persists the moment and fires the EVOLVE event
     * so the floating pet performs the ceremony (a celebratory reaction + a
     * path-flavored line resolved from [currentStage] when it speaks). Called
     * from CurioQuests when a level crosses a growth tier and from the Pet
     * Designer when a path is chosen.
     */
    fun noteEvolved(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_EVOLVE_AT, System.currentTimeMillis()).apply()
        CurioPetBrain.observeLevelUp(context)
        reactTo(Event.EVOLVE)
    }

    /**
     * v13 — a daily/weekly quest was claimed (CurioQuests.claimDaily /
     * claimWeekly): the pet celebrates the reward moment.
     */
    fun noteQuestComplete(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_QUEST_AT, System.currentTimeMillis()).apply()
        reactTo(Event.QUEST_COMPLETE)
        // v9.x — Home shows a one-shot celebration nudge.
        pendingQuestNudge = true
    }

    /**
     * v13 — a new best streak was recorded (CurioQuests.onStreakRecorded):
     * the pet celebrates the milestone, with day-specific lines at the flame
     * days (1 / 3 / 7 / 14 / 30). The day count is read from
     * [CurioQuests.bestStreakState] when the line speaks.
     */
    fun noteStreakMilestone(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_STREAK_AT, System.currentTimeMillis()).apply()
        reactTo(Event.STREAK_MILESTONE)
    }

    fun noteLaneExplored(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_NEW_LANE_AT, System.currentTimeMillis()).apply()
        CurioPetBrain.observeExplore(context)
    }

    fun lastXpAt(context: Context): Long = prefs(context).getLong(KEY_LAST_XP_AT, 0L)
    fun lastLevelUpAt(context: Context): Long = prefs(context).getLong(KEY_LAST_LEVEL_AT, 0L)
    fun lastNewLaneAt(context: Context): Long = prefs(context).getLong(KEY_LAST_NEW_LANE_AT, 0L)
    fun lastPlayAt(context: Context): Long = prefs(context).getLong(KEY_LAST_PLAY_AT, 0L)
}

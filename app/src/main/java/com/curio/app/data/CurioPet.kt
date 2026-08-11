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
    // universal; at level 7 the user chooses a path; at level 25 the pet
    // reaches its final form. Each evolution plays an animation.
    // ═══════════════════════════════════════════════════════════════════

    /** Evolution path — the elemental affinity chosen at level 7. */
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
            FIRST_EVO -> "${path?.displayName ?: "Evolved"} (Level 7)"
            FINAL_EVO -> "${path?.displayName ?: "Grown"} (Level 25)"
        }
    }

    /** Computes the evolution tier + path from level and the saved choice. */
    fun evolutionStage(level: Int, path: EvoPath?): Pair<Stage, EvoPath?> {
        return when {
            level >= 25 && path != null -> Stage.FINAL_EVO to path
            level >= 7 && path != null -> Stage.FIRST_EVO to path
            else -> Stage.BABY to null
        }
    }

    /** True when the pet is ready to evolve (level >= 7, no path yet). */
    fun canEvolve(level: Int, path: EvoPath?): Boolean =
        level >= 7 && path == null

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
            level >= 7 && path != null -> Stage.FIRST_EVO
            else -> Stage.BABY
        }
    }

    /** Current stage from live [CurioQuests] state + saved evolution path. */
    fun currentStage(): Stage {
        val level = CurioQuests.levelForXp(CurioQuests.xpState)
        val path = AppPreferences.evoPath()
        return when {
            level >= 25 && path != null -> Stage.FINAL_EVO
            level >= 7 && path != null -> Stage.FIRST_EVO
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
            Stage.FIRST_EVO -> "Reach Level 7 to evolve."
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

    /** Called by the screens where the action really happens. */
    fun reactTo(event: Event) {
        lastEvent = event
        eventCount++
    }

    // ── Anti-repeat bag (v9.x) ────────────────────────────────────────
    // The pet never says the same line twice in a row: [pickLine] skips
    // anything spoken recently and only falls back to the full pool once
    // every option has been used. Kept in memory per process — plenty to
    // make short-term chatter feel endless instead of looping.
    private val saidLines = ArrayDeque<String>()
    private const val SAID_LINES_CAP = 16

    /** Picks a line from [options], avoiding anything said recently. */
    fun pickLine(options: List<String>): String {
        if (options.isEmpty()) return "…"
        val fresh = options.filterNot { it in saidLines }
        val chosen = (if (fresh.isNotEmpty()) fresh else options).random()
        saidLines.addLast(chosen)
        while (saidLines.size > SAID_LINES_CAP) saidLines.removeFirst()
        return chosen
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
        "Again?! …I mean, AGAIN! I love it!",
        "That's the third one today. Not that I'm counting. (I'm counting.)",
        "Okay, okay — one more cheer! My paws are getting tired!",
        "You're on a roll! I'm running out of sparkles… okay, no I'm not.",
        "Another one?! You spoil me. …Keep going.",
        "Hmph! So many things to open. My tiny heart can't take it. Do it again.",
        "You again! Hehe — okay, I'm invested now.",
        "I've cheered so much my sparkle needs a snack break.",
        "Is that a new thing? …It's the same thing. I don't care. MORE!",
        "My excitement is now legally yours. Proceed."
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
            Event.SPIN_LANDED -> pickLine(listOf(
                "It landed!", "Ooh, the deck chose well!", "A new topic, a new tale!",
                "Spin-spin-spin! …I mean, ooh.", "That landing had drama!",
                "The wheel spoke!", "Destiny, served on a card!",
                "I saw that one coming… nope, I didn't.", "Round and round it goes!",
                "Where it stops, nobody knows… except the deck."
            ))
            // v8.30 — the USER's tap gets a touch reaction, never "it opened
            // itself".
            Event.REVEAL_TAPPED -> pickLine(listOf(
                "You picked it!", "Ooh, good choice!", "That one called to you!",
                "Nice pick!", "It knew you'd tap it!", "Ooh, the good kind of surprise!",
                "Great pick!", "Ooh, good taste!", "That one's a keeper, I can tell!",
                "You have the magic touch!", "I was rooting for this one!",
                "A confident tap! I respect that."
            ))
            // v8.30 — only the spin's true AUTO-open says the surprise lines.
            Event.REVEAL_AUTO -> pickLine(listOf(
                "There it is!", "It opened itself, sneaky!", "Ta-da! A new tale!",
                "Ooh, look what landed!", "Surprise!", "It chose FOR us. Bold.",
                "Look what rolled in!", "No hands! Well… no paws!",
                "The deck knows what it's doing.", "Bold move, deck. I like it.",
                "Peek-a-boo! …It's a whole topic!", "It picked FOR us. How forward."
            ))
            Event.EXPLORE -> pickLine(listOf(
                "Go explore!", "Adventure time!", "I'll wait right here. Go see!",
                "Bring back a story!", "Go see the world!", "Adventure awaits!",
                "Say hi to the world for me!", "I'll guard the deck while you're out!",
                "Pack snacks. Bring tales."
            ))
            // v8.29 — "Mine now… I mean, ours!" only after the bond is FRIEND+.
            Event.SAVE -> if (isWarm()) pickLine(listOf(
                "Keepsake saved!", "Mine now… I mean, ours!", "Tucked away safely!",
                "Our shelf grows!", "A treasure for the shelf!", "We collect memories!",
                "One more spark for our collection!", "It's OURS now. Officially."
            )) else pickLine(listOf(
                "Keepsake saved!", "Tucked away safely!", "It's yours to keep!",
                "Captured for later!", "Snap! Saved!", "Another keepsake!",
                "The shelf grows!", "Well kept, spark keeper!"
            ))
            // v9.2 — the pet answers the touch / play / level-up moments too.
            Event.TOUCH -> pickLine(listOf(
                "Boop!", "Hehe — again!", "That's my favorite spot.", "Boop boop!",
                "That's the spot!", "Soft! …Wait, that's me.", "Tiny hugs!",
                "You found my favorite spot!", "Hehehe, tickles!", "Squeak!",
                "Two boops in one! Professional."
            ))
            Event.PLAY -> pickLine(listOf(
                "Wheee!", "You're good at this!", "One more round!", "Again, again!",
                "This is the best!", "I'm undefeatable! …Almost.", "One more round, promise!",
                "Wheee, the floor is my trampoline!", "Tag! You're it!"
            ))
            Event.LEVEL_UP -> pickLine(listOf(
                "We leveled up!", "Feel that? Growth!", "Shiny new spark!",
                "I can almost do a backflip!", "Level up! I'll pretend that was hard.",
                "Sparks of power!", "I'm 10% more sparkly now.", "Up up up we go!"
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
            "The flame is lit!", "Day 1! A brand-new spark!"
        ))
        3 -> pickLine(listOf(
            "Day 3! A real streak is born!", "Three days! The flame has friends!"
        ))
        7 -> pickLine(listOf(
            "Day 7! A whole week of wonder!", "Seven days! The flame is a bonfire now!"
        ))
        14 -> pickLine(listOf(
            "Day 14! Two weeks of fire!", "Fortnight flame! Steady as starlight!"
        ))
        30 -> pickLine(listOf(
            "Day 30! A month of mystery!", "Thirty days! Legendary flame!"
        ))
        else -> pickLine(listOf(
            "Day $streak! The flame grows!",
            "$streak days in a row! Still glowing strong!",
            "Streak day $streak! One spark at a time.",
            "Day $streak! The flame likes this pace."
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
                    "I'm Blaze now! Small, but VERY warm!",
                    "Fire path! My spark has opinions now!",
                    "Look at me! A blaze of pure curiosity!"
                ))
                EvoPath.WATER -> pickLine(listOf(
                    "I'm Tide now! Cool, calm, and deep!",
                    "Water path! I ripple wherever questions lead!",
                    "Look at me! I flow with every wonder!"
                ))
                EvoPath.NATURE -> pickLine(listOf(
                    "I'm Bloom now! I grow wherever I go!",
                    "Nature path! Something new sprouts in me!",
                    "Look at me! I'm blooming with ideas!"
                ))
                null -> pickLine(listOf(
                    "Ta-da! I grew all the way up!",
                    "Same me, but BIGGER spark!"
                ))
            }
            Stage.FINAL_EVO -> pickLine(listOf(
                "I'm fully grown! The whole shelf is mine!",
                "This is it, my final form! Every lane made me!",
                "I made it all the way! I'm fully me now!",
                "Look at the grown me! All the sparks came home!"
            ))
            Stage.BABY -> "Fresh little me!"
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
                "Good morning!", "Morning! Ready for a spin?", "Rise and shine!",
                "Fresh day, fresh topics!", "Sun's up — the deck is waiting!",
                "A brand-new day to explore!", "Morning stretch. Okay, we go!",
                "Good morning! I made the bed… of ideas!", "Hello hello! Fresh topics!",
                "Rise and shine and SPIN and shine!"
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
        "Ooh! Somewhere new!",
        "Wheee, new ground!",
        "The deck has taste!",
        "Fresh paths ahead!",
        "New things! New things! …I contain myself. Mostly.",
        "Ooh, I can feel the newness!",
        "Fresh territory! My paws are ready.",
        "Somewhere we've never been!",
        "The curiosity tingles!",
        "This is the good stuff!"
    )
    private val happyLines = listOf(
        "Nice! XP banked. Keep going?",
        "That was fun. More?",
        "Curiosity looks good on you.",
        "Ooh, we're on a roll!",
        "Doing that again? Please?",
        "That one felt great!",
        "Happy little sparks!",
        "The deck approves. So do I.",
        "Another lovely moment for the shelf!",
        "You make this easy, you know."
    )
    // v8.14 — the HAPPY mood wears the hour's voice: morning energy, cozy
    // evening, hushed night.
    private val morningLines = listOf(
        "Morning! The deck smells fresh.",
        "Rise and shine. Something new is waiting.",
        "Fresh eyes, fresh topics. Let's go!",
        "Morning! The topics have been waiting patiently.",
        "Bright morning, bright ideas.",
        "First spin of the day is the best spin.",
        "Good morning to us! Mainly you.",
        "The sun and I both say: explore something!"
    )
    private val afternoonLines = listOf(
        "Afternoon wander? Let's go.",
        "Bright and busy. A good time to peek.",
        "Midday! Perfect for a quick spin.",
        "Afternoon lull? Perfect cover for a spin.",
        "The afternoon light makes everything look wise.",
        "Quick break for a discovery?",
        "Noon snack: a topic, ideally.",
        "Halfway through the day — let's add a spark."
    )
    private val eveningLines = listOf(
        "Evening! Cozy hour, warm lamp.",
        "The day's winding down. One more spin?",
        "Evening glow. Nice time for a discovery.",
        "Evening, evening, time for leaning back.",
        "The lamp's on. The deck's ready. You?",
        "Golden hour for golden facts!",
        "Warm light, warm topics.",
        "One little discovery before the day tucks in?"
    )
    private val nightLines = listOf(
        "Shh, night mode. One quiet spin?",
        "The stars are out. The deck still shines.",
        "It's late, but the deck will be here tomorrow.",
        "Night owl hour. My whiskers approve.",
        "Quiet now… the facts are whispering.",
        "Under the stars, even facts glow softly.",
        "Just one more, then blankets. Deal?",
        "The moon is out. Curiosity can't sleep."
    )
    // v8.29 — the warmer twins only speak once the bond is FRIEND or closer.
    private val warmMorningLines = listOf(
        "Good morning! I saved your spot.",
        "Morning! I missed this.",
        "Morning! I dreamed about our shelf.",
        "Good morning, my favorite explorer!",
        "Morning! Same spot, same us. Perfect."
    )
    private val warmAfternoonLines = listOf(
        "Afternoon! You always pick the best topics.",
        "Afternoon, friend. The shelf is waiting.",
        "You're here! The deck did a happy shuffle."
    )
    private val warmEveningLines = listOf(
        "Evening! Cozy hour, and I'm glad you're here.",
        "Evening, friend. Best part of the day.",
        "The lamp's on and so is our shelf."
    )
    private val warmNightLines = listOf(
        "Past my bedtime… but for you, I'll stay.",
        "Night, friend. I'll keep the shelf warm.",
        "One quiet spin, then I'll curl up. Promise."
    )
    private val curiousLines = listOf(
        "We haven't tried __LANE__ yet. Want a new stamp?",
        "I wonder what __LANE__ hides…",
        "Pssst, __LANE__ is calling.",
        "__LANE__ is right there, unexplored!",
        "What's in __LANE__? Only one way to know.",
        "My paws are itching for __LANE__.",
        "__LANE__ looks interesting… just saying."
    )
    private val sleepyLines = listOf(
        "I'll keep your seat warm. Come spin when you're ready.",
        "Yawn… the deck can wait a moment.",
        "Soft blanket, warm lamp… I'm ready when you are.",
        "My eyelids are doing reps…",
        "Zzz… I mean, I'm listening!",
        "The deck is nice, but blankets are nicer.",
        "One more yawn and I'm a pillow.",
        "I'll be here. Probably. Definitely. Zzz…"
    )
    // v8.13 — the new moods' lines: focused keeps out of the way while the
    // user writes; bouncy rides the post-play high.
    private val focusedLines = listOf(
        "Write it down. I'll guard your thoughts.",
        "Quiet paws, I promise.",
        "Take your time. This one's a keeper.",
        "Shh — the words are working. I'll wait.",
        "Thinking face! Mine too. Keep going.",
        "Every thought you save is a tiny treasure.",
        "I'm guarding this sentence personally.",
        "Deep focus mode. Paws: sealed."
    )
    private val bouncyLines = listOf(
        "Phew, that was fun. Again soon?",
        "I'm still bouncing from that game!",
        "Best play date ever. …Round two?",
        "My paws won't stop wiggling!",
        "That was AMAZING. Phew. More please!",
        "I'm chasing my own tail in my head.",
        "Game energy: still 100%!"
    )
    // v9.2 — lines for the three new emotions.
    private val shyLines = listOf(
        "H-hi. I'm still getting used to you…",
        "*hides behind the deck*",
        "You're nice. I think. Probably.",
        "*peeks out one eye* …Hi.",
        "I'm warming up. Slowly. Cutely.",
        "Don't mind me, just being small.",
        "You saw me. That's… that's fine. Probably."
    )
    private val grumpyLines = listOf(
        "Hmph. The deck hasn't moved in a while…",
        "I'm not pouting. I'm conserving energy.",
        "A spin would fix this mood, just saying.",
        "My sparkles need exercise.",
        "I'm practicing my serious face. How is it?",
        "I counted the tiles. Twice.",
        "Someone should spin something. Not naming names. It's me. I'm naming you."
    )
    private val playfulLines = listOf(
        "That game left me sparkling! Again?",
        "I could do three more rounds. Four. Maybe five.",
        "Boop me. I dare you.",
        "I've still got zoomies, round two?",
        "One more game and then… one more game.",
        "Catch me if you can. Okay, you can. Always can.",
        "Play! Play play play! …I'm calm. PLAY!"
    )
    // v13 — return-after-absence welcome pools (see [welcomeBackLine]).
    private val welcomeBackDayLines = listOf(
        "I missed you. The shelf waited.",
        "Welcome back! I kept the topics warm.",
        "Oh, you're back! I saved you the good lane.",
        "Missed you! The deck missed you too.",
        "Back at last! I was just dusting the curiosity."
    )
    private val welcomeBackDaysLines = listOf(
        "You were gone so long the topics started their own club.",
        "Welcome home! I watered the curiosity while you were away.",
        "A few days away! I narrated the shelf to myself.",
        "You're back! I reorganized the deck twice. Okay, once."
    )
    private val welcomeBackWeekLines = listOf(
        "A whole week! I've been practicing my patience.",
        "You're back! I grew a whole new eagerness while you were gone.",
        "Seven days! I even missed the sassy ones.",
        "A week away! I saved you all the good questions."
    )
    // v13 — lines for claiming a daily/weekly quest (see [noteQuestComplete]).
    private val questCompleteLines = listOf(
        "Quest done! Sparkle earned!",
        "That quest never stood a chance!",
        "Another quest, conquered!",
        "Checked off! The list quivers.",
        "We finished it together. Well, mostly you.",
        "Quest complete! I'm so proud of our teamwork.",
        "One more quest bites the dust!"
    )

    // ═══════════════════════════════════════════════════════════════════
    // v14 — BABY voice (telegraphic, ~18-24 month equivalent)
    // Research-informed (child telegraphic speech + pet-directed speech):
    // 1-3 word utterances, content words only (no articles/auxiliaries),
    // concrete nouns & verbs, exclamation-led, heavy on onomatopoeia. The
    // baby says FEW things; the evolved forms speak the full rich library
    // above. Routed by currentStage() in every line source below.
    // ═══════════════════════════════════════════════════════════════════
    private val babySassyLines = listOf("Again?", "Same same!", "Ooh ooh!", "You! Again!")
    private val babyProudLines = listOf("Big!", "Grew!", "Up up!", "Warm!")
    private val babyExcitedLines = listOf("New! New!", "Ooh ooh!", "Wow!", "Look look!")
    private val babyHappyLines = listOf("Happy!", "Good!", "Yay!", "Sparkle!")
    private val babyCuriousLines = listOf("What?", "Look?", "Ooh?", "New thing!")
    private val babySleepyLines = listOf("Tired…", "Nighty…", "Zzz…", "Sleepy…")
    private val babyFocusedLines = listOf("Shh…", "Quiet…", "Work work.")
    private val babyBouncyLines = listOf("Bounce!", "Wheee!", "More more!")
    private val babyShyLines = listOf("…Hi.", "*peek*", "Small…", "…Hid")
    private val babyGrumpyLines = listOf("Hmph!", "No!", "…Grumps", "Grumble!")
    private val babyPlayfulLines = listOf("Play!", "Again!", "Boop boop!", "Fun fun!")
    private val babySpinCheerLines = listOf(
        "Go go!", "Spin spin!", "Round!", "Wheee!", "Faster!", "Ooh ooh!"
    )
    private val babyPeekLines = listOf("Boo!", "Peek!", "Hid!", "Here!")
    private val babyChameleonLines = listOf("Gone!", "Poof!", "Bye bye!", "…Here!")
    private val babySparkLines = listOf("Spark!", "Mine!", "Got!", "Shiny shiny!")
    private val babyMorningLines = listOf("Morn!", "Hi hi!", "Day!", "Up up!")
    private val babyWelcomeBackDayLines = listOf(
        "You back!", "Missed you!", "Here again!", "Yay you!"
    )
    private val babyWelcomeBackDaysLines = listOf(
        "Many days! Missed you!", "You go long! Missed!", "Home home!"
    )
    private val babyWelcomeBackWeekLines = listOf(
        "Whole week! Missed you!", "Long long! You here now!", "Week! Big hug!"
    )

    /** v14 — a BABY mood bubble: 1-3 words, concrete, exclamation-led. */
    private fun babyMoodLine(mood: Mood): String = when (mood) {
        Mood.PROUD -> pickLine(babyProudLines)
        Mood.EXCITED -> pickLine(babyExcitedLines)
        Mood.HAPPY -> pickLine(babyHappyLines)
        Mood.CURIOUS -> pickLine(babyCuriousLines)
        Mood.FOCUSED -> pickLine(babyFocusedLines)
        Mood.BOUNCY -> pickLine(babyBouncyLines)
        Mood.SHY -> pickLine(babyShyLines)
        Mood.GRUMPY -> pickLine(babyGrumpyLines)
        Mood.PLAYFUL -> pickLine(babyPlayfulLines)
        Mood.SLEEPY -> pickLine(babySleepyLines)
    }

    /** v14 — a BABY reaction line for [event]. */
    private fun babyEventLine(event: Event): String = when (event) {
        Event.SPIN_LANDED -> pickLine(listOf("Ooh!", "Landed!", "Round round!", "Spin done!"))
        Event.REVEAL_TAPPED -> pickLine(listOf("You!", "Ta-da!", "Look!", "Good pick!"))
        Event.REVEAL_AUTO -> pickLine(listOf("Oh!", "It did!", "Ta-da!", "Surprise!"))
        Event.EXPLORE -> pickLine(listOf("Go go!", "See!", "Bye bye!", "New!"))
        Event.SAVE -> pickLine(listOf("Keep!", "Mine!", "Snap!", "Save save!"))
        Event.TOUCH -> pickLine(listOf("Boop!", "Hehe!", "Soft!", "Again!"))
        Event.PLAY -> pickLine(listOf("Wheee!", "Fun!", "Again!", "Zoom!"))
        Event.LEVEL_UP -> pickLine(listOf("Big!", "Up!", "Grow!", "Yay!"))
        Event.EVOLVE -> "Fresh me!"
        Event.QUEST_COMPLETE -> pickLine(listOf("Done!", "Yay!", "Win win!", "Good good!"))
        Event.STREAK_MILESTONE -> babyStreakLine(CurioQuests.bestStreakState)
    }

    /** v14 — a BABY streak line: the flame in tiny words. */
    private fun babyStreakLine(streak: Int): String = when (streak) {
        1 -> pickLine(listOf("Fire!", "Warm warm!"))
        3 -> pickLine(listOf("Day 3! Big!", "Three days! Warm!"))
        7 -> pickLine(listOf("Day 7! Big fire!", "Week! Wow!"))
        14 -> pickLine(listOf("Day 14! Fire fire!", "Many days!"))
        30 -> pickLine(listOf("Day 30! Big big!", "So many days!"))
        else -> pickLine(listOf("Day $streak! Warm!", "Day $streak! Fire!", "More days!"))
    }

    /** v14 — a BABY tap answer: boop → bounce → tiny celebration. */
    private fun babyTouchLine(tier: Int): String = when {
        tier >= 3 -> pickLine(listOf("Wheee!", "Yay yay!", "More more!", "Boop boop boop!"))
        tier >= 2 -> pickLine(listOf("Hehe!", "Zoom!", "Again!", "Bounce!"))
        else -> pickLine(listOf("Boop!", "Soft!", "Hehe!", "…Again?"))
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
        "The same door, again?", "I have seen this card before. It has not changed.",
        "Repetition builds mastery. You are testing it.", "Ah. This again. How nostalgic.",
        "Some curiosities are worth revisiting."
    )
    private val matureProudLines = listOf(
        "Growth comes quietly, then all at once.", "Another level. The path keeps its promises.",
        "I have carried every lane here with me.", "The shelf has noticed, and so have I."
    )
    private val matureExcitedLines = listOf(
        "Oh. A good kind of spark.", "Even I lean forward for this one.",
        "There it is. The old excitement.", "The deck has my attention now."
    )
    private val matureHappyLines = listOf(
        "This is a good day to be curious.", "Contentment, with a little wonder in it.",
        "Warm, steady, and quietly glad.", "Some days simply fit."
    )
    private val matureCuriousLines = listOf(
        "Knowledge calls when you are still.", "A new question. Good. Questions keep us young.",
        "I have room for one more wonder.", "Let us look closer."
    )
    private val matureFocusedLines = listOf(
        "Now that is a riddle worth sitting with.", "Quiet. The answer is almost here.",
        "Focus is a kind of love.", "Patience. The details are arriving."
    )
    private val matureBouncyLines = listOf(
        "Even at my age, gravity has not won.", "The spark is willing, and the body agrees.",
        "A little bounce. The shelf allows it.", "Lightness finds me now and then."
    )
    private val matureShyLines = listOf(
        "Ah. You caught me mid-thought.", "I was somewhere else. A pleasant somewhere.",
        "Do not mind me. I was reminiscing.", "Hm. I was not expecting an audience."
    )
    private val matureGrumpyLines = listOf(
        "Even the wise have their grumpy hours.", "The deck can wait a moment.",
        "I am resting my opinions.", "Hmph. The shelf is too loud today."
    )
    private val maturePlayfulLines = listOf(
        "Very well. One round for old time's sake.", "You wish to play? The old spark agrees.",
        "Come then. I still remember the moves.", "A game. How very young of me. I accept."
    )
    private val matureSleepyLines = listOf(
        "The shelf grows quiet. So do I.", "Sleep calls. Even the wise answer.",
        "Rest now. The questions will keep.", "Tired, but satisfied. A good kind."
    )
    private val matureSpinCheerLines = listOf(
        "Steady now. Let it choose.", "The deck deliberates. Patience.",
        "Round and round. It knows the way.", "Whatever lands, there is a story in it.",
        "The wheel remembers every lane."
    )
    private val maturePeekLines = listOf(
        "I was here all along. Mostly.", "You looked. I was there. A classic.",
        "Hidden, but never gone.", "A little absence makes a fine hello."
    )
    private val matureChameleonLines = listOf(
        "I am the wall now. It suits me.", "Camouflage is patience with a costume.",
        "Gone, as it were. Back, as I am.", "Stillness is its own disguise."
    )
    private val matureSparkLines = listOf(
        "A spark. Let us see who is faster.", "Some things are worth the chase.",
        "Catch. Then we discuss it.", "The tiny ones move quickest. A lesson."
    )
    private val matureMorningLines = listOf(
        "Morning. The world kept turning without us.", "A new day for old curiosities.",
        "Sun's up. The deck stirs. So do I.", "Good morning. I saved you the first wonder."
    )
    private val matureWelcomeBackDayLines = listOf(
        "You were gone. I kept the shelf warm.", "Welcome back. The topics missed your eyes.",
        "A day away. The deck and I managed.", "There you are. I was beginning to narrate to myself."
    )
    private val matureWelcomeBackDaysLines = listOf(
        "A few days away. The curiosity missed you.", "Welcome home. I watered the questions in your absence.",
        "The shelf feels right again with you here.", "You were gone long enough for the deck to reorganize itself."
    )
    private val matureWelcomeBackWeekLines = listOf(
        "A whole week. The topics began to worry.", "Welcome back. I have been practicing my patience, as promised.",
        "Seven days is a long time for a curious mind.", "You return, and the shelf exhales."
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
            "The wheel has spoken, as it always does.", "A landing. The story begins.",
            "Where it stopped, the deck already knew."
        ))
        Event.REVEAL_TAPPED -> pickLine(listOf(
            "A confident choice. I approve.", "You have a good eye for stories.",
            "That one called to you. And rightly so."
        ))
        Event.REVEAL_AUTO -> pickLine(listOf(
            "It opened itself. Bold little deck.", "Some doors open on their own.",
            "The deck decided. I simply agree."
        ))
        Event.EXPLORE -> pickLine(listOf(
            "Go on. The world is worth your attention.", "Bring back something interesting.",
            "Explore well. Curiosity is its own reward."
        ))
        Event.SAVE -> pickLine(listOf(
            "Keep it close. Some stories you carry.", "Saved. The shelf remembers everything.",
            "A wise thing to hold onto."
        ))
        Event.TOUCH -> pickLine(listOf(
            "Hm. That was well done.", "Gentle. I like that.", "Yes. That is the right way."
        ))
        Event.PLAY -> pickLine(listOf(
            "Very well. One more round.", "The old spark wakes. Acceptable.",
            "Play, then. I am not so old as all that."
        ))
        Event.LEVEL_UP -> pickLine(listOf(
            "Another step on the same long path.", "Up we go. The climb never truly ends.",
            "Level after level. The journey is the point."
        ))
        Event.EVOLVE -> pickLine(listOf(
            "This is the me I was always becoming.", "Change arrives, and I greet it.",
            "Every ending grows into a beginning."
        ))
        Event.QUEST_COMPLETE -> pickLine(listOf(
            "Done, as it should be.", "A task finished. The shelf approves.",
            "Completed. One more promise kept."
        ))
        Event.STREAK_MILESTONE -> matureStreakLine(CurioQuests.bestStreakState)
    }

    /** v14.1 — a MATURE streak line: the flame, spoken with quiet pride. */
    private fun matureStreakLine(streak: Int): String = when (streak) {
        1 -> pickLine(listOf("The flame is lit. Guard it well.", "Day one. Every long road starts here."))
        3 -> pickLine(listOf("Day 3. The rhythm takes hold.", "Three days. The flame learns to trust you."))
        7 -> pickLine(listOf("A week alight. The fire is real now.", "Day 7. Consistency is a quiet power."))
        14 -> pickLine(listOf("Fortnight of fire. The shelf is proud.", "Two weeks. The flame has roots."))
        30 -> pickLine(listOf("A month of wonder. Legend, gently earned.", "Day 30. Few reach this. You did."))
        else -> pickLine(listOf(
            "Day $streak. The flame remembers.", "Day $streak. Steady as starlight.",
            "Streak day $streak. One spark at a time."
        ))
    }

    /** v14.1 — a MATURE tap answer: calm acknowledgment, then delight. */
    private fun matureTouchLine(tier: Int): String = when {
        tier >= 3 -> pickLine(listOf(
            "Very well. Joy, on your command.", "I have not felt this light in years.",
            "Enough. I am fully celebrated."
        ))
        tier >= 2 -> pickLine(listOf("Ah, playful today.", "You are persistent. I respect that.", "Hm. That is nice."))
        else -> pickLine(listOf("Hm. Noted.", "Gentle. Good.", "Yes. A quiet boop."))
    }

    /**
     * v14.1 — a MATURE line for a Pet Life routine: the fully grown pet
     * replaces the youthful routine lines (PetLife.kt) with its own calm
     * register. Unknown ids return null, so the routine plays as pure
     * motion rather than speaking out of voice.
     */
    fun matureRoutineLine(routineId: String): String? = when (routineId) {
        "look-around" -> pickLine(listOf("Let us see what deserves attention."))
        "little-wave" -> pickLine(listOf("A quiet greeting from over here."))
        "stretch" -> pickLine(listOf("A stretch, and then the next wonder."))
        "turn-and-peek" -> pickLine(listOf("I thought I saw a story. Only me."))
        "tiny-stumble" -> pickLine(listOf("Steady. The floor and I have an agreement."))
        "look-up" -> pickLine(listOf("Even the ceiling keeps a few secrets."))
        "backstage" -> pickLine(listOf("One checks one's best side. It is tradition."))
        "victory-pose" -> pickLine(listOf("That deserves a pose. And a pause."))
        "home-stretch" -> pickLine(listOf("Home. A stretch, and all is well."))
        "room-tour" -> pickLine(listOf("Come. I will show you what I found."))
        "window-watch" -> pickLine(listOf("The view is patient. It waits for us."))
        "cozy-turn" -> pickLine(listOf("One small inspection. The shelf will forgive me."))
        "home-dance" -> pickLine(listOf("A dance, for the quiet joy of it."))
        "deck-anticipation" -> pickLine(listOf("Steady. The deck is thinking."))
        "deck-side-peek" -> pickLine(listOf("I can almost see the answer arriving."))
        "deck-stretch" -> pickLine(listOf("Ready, then. Let the wheel turn."))
        "deck-victory" -> pickLine(listOf("A landing with style. Naturally."))
        "quest-read" -> pickLine(listOf("Let me read the fine print. It matters."))
        "quest-wave" -> pickLine(listOf("I believe in this quest. Wholeheartedly."))
        "quest-proud" -> pickLine(listOf("Quest energy, steady and true."))
        "quest-hide" -> pickLine(listOf("I shall be your mysterious guide."))
        "topic-peek" -> pickLine(listOf("Shall we peek together?"))
        "topic-wow" -> pickLine(listOf("Oh. That one is glowing."))
        "topic-spin" -> pickLine(listOf("A story worth a quiet celebration."))
        "topic-inspect" -> pickLine(listOf("Let us look closer. Details matter."))
        "writing-focus" -> pickLine(listOf("I am guarding this thought for you."))
        "writing-wave" -> pickLine(listOf("Take your time. Words keep."))
        "writing-stretch" -> pickLine(listOf("A stretch, then back to the good words."))
        "writing-shy" -> pickLine(listOf("That thought looks important. I will stay quiet."))
        "shelf-hunt" -> pickLine(listOf("Which keepsake shall we revisit?"))
        "shelf-wave" -> pickLine(listOf("Your shelf is looking fine. As it should."))
        "shelf-backstage" -> pickLine(listOf("I am checking the back row. One must."))
        "shelf-proud" -> pickLine(listOf("All those discoveries. A fine harvest."))
        "mirror-check" -> pickLine(listOf("Do I look wise from back here? I do."))
        "profile-wave" -> pickLine(listOf("Hello, profile page. Still growing."))
        "profile-proud" -> pickLine(listOf("Your progress sparkles. I have watched it."))
        "profile-look-up" -> pickLine(listOf("There is always another level. That is the point."))
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
            Mood.PROUD -> pickLine(listOf(
                "Level ${CurioQuests.levelForXp(CurioQuests.xpState)}. I grew a little!",
                "Shiny! We leveled up together.",
                "Do you feel that? That's growth!",
                "I can practically do a backflip at level ${CurioQuests.levelForXp(CurioQuests.xpState)}.",
                "Another level! My sparkle has sparkles.",
                "Level ${CurioQuests.levelForXp(CurioQuests.xpState)} — the deck is impressed. So am I."
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
                    "Spin something new today?"
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
            Stage.FIRST_EVO -> pickLine(listOf(
                "Go, go, go!", "Spinny spin!", "Ooh, where will it land?",
                "Come on, good one!", "Round and round!", "I can't watch. Okay, I'm watching.",
                "Spinning! Spinning! Don't fall!", "The deck is showing off!",
                "Ooh ooh ooh — I can't look. Looking!", "Gravity, do your thing!",
                "Round and round and ROUND!", "Pick a good one, deck!",
                "I'm cheering so hard I'm vibrating!", "Almost… almost… it's choosing!",
                "Go deck go! You can do the thing!", "Tiny heart, big spin energy!"
            ))
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
                    "Yay!", "I love boops!", "Best friends!", "Squee!",
                    "More, more, more!", "You're my favorite!", "Party time!",
                    "You're my favorite person-pet duo!", "Squee! Okay, more!",
                    "This is my favorite spot AND you found it."
                ))
                isWarm() -> pickLine(listOf(
                    "Yay!", "I love boops!", "Squee!", "More, more, more!",
                    "Party time!", "Hehe!", "Boop buddy!", "We're so good at this!",
                    "Hehe, you know my spot!", "Best boop partner!"
                ))
                else -> pickLine(listOf(
                    "Yay!", "Squee!", "More, more, more!", "Party time!", "Wheee!",
                    "Boop!", "Hehe!", "Ooh!", "That's fun!", "Yippee!"
                ))
            }
            tier >= 2 -> pickLine(listOf(
                "Hehehe!", "More, more!", "This is fun!", "Tag, you're it!",
                "Catch me!", "Bouncy bouncy!", "Again, again!", "Wiggle wiggle!",
                "Boop attack!", "I'm too bouncy to stop!", "Zoom zoom zoom!",
                "Poke poke poke!", "We're playing, right? We're playing!"
            ))
            else -> pickLine(listOf(
                "Boop!", "Hehe!", "Wheee!", "Ooh!", "That tickles!", "Hihi!",
                "Boop boop!", "Again!", "You found me!", "Poke!", "Hi hi hi!",
                "Soft paws!", "Mrow!", "Pfft!", "Blep!", "Mmm, pats!",
                "Squeak!", "That's my ear!", "Hehehe!", "Boop rights! You earned them!"
            ))
        }
    }

    /**
     * The pet sometimes starts a game on its own (v8.11) — it play-bows,
     * calls out, then zooms off. Kept short: one sentence max.
     */
    fun playInitiation(): String = pickLine(listOf(
        "Wanna play? Catch me!", "Boop! You're it!", "I'm feeling bouncy!",
        "Zoom zoom, chase me!", "Play with me!", "Tag! Your turn!",
        "I'm bored, come chase me!", "Pounce position: ready!",
        "Game mode: ON!", "I saw a speck. It must be chased.",
        "Ready, set… zoom!", "You move, I chase. Rules of the room.",
        "Catch me if your fingers are fast!", "I've got the zoomies and I've got a plan!"
    ))

    /**
     * v8.16 — the pet's line when it pokes something on the current screen:
     * [funThing] = a button/gadget (boop! hearts!), otherwise a curious
     * read of some text. One sentence max, matching the passive-bubble rule.
     */
    fun landmarkLine(funThing: Boolean): String =
        (if (funThing) pickLine(listOf(
            "Boop!", "Ooh, shiny!", "Hehe, hi!", "Tag! You're it!",
            "I like this one!", "Spinny spinny!", "Wheee!", "Boop boop boop!",
            "Bloop!", "Squeak!", "I booped it. It's mine now.",
            "Ooh, a gadget! Hi, gadget!", "Press… press… press!",
            "It goes boop back!"
        )) else pickLine(listOf(
            "What's this?", "Hmm, interesting…", "*peeks*", "Read read read!",
            "Ooh, words!", "Let me read this!", "Scribble scribble!",
            "So many letters!", "I'm reading. Slowly. Cutely.",
            "Hmm… aha! …I don't know what aha yet.",
            "This page smells like knowledge.", "Words words words!",
            "Paws can't turn pages. Tragic."
        )))

    /**
     * v8.17 — the pet's line when it does a little jig at a special spot
     * (a PLAY landmark). One sentence max, matching the passive-bubble rule.
     */
    fun jigLine(): String = pickLine(listOf(
        "Tippy tap tap!", "Happy feet!", "Wiggle wiggle!",
        "Da-da-daaaa!", "Jiggle jiggle!", "Party paws!",
        "Dance break!", "Shake it off!", "Tap dance time!",
        "Boots and cats and cats and boots!", "I'm a dancing machine!",
        "Shimmy shimmy shake!", "Twinkle toes, tiny feet!",
        "This groove is legally mine now."
    ))

    /**
     * v8.21 — the pet got flung around (dragged) and is dizzy: swirl eyes,
     * a wobble, and a groggy line while it recovers.
     */
    fun dizzyLine(): String = pickLine(listOf(
        "Whoa… the room is spinning!", "Wheee, dizzy!", "Spin spin… okay, stop!",
        "Whoa whoa whoa!", "I think I need a sit-down…", "So dizzy!",
        "We-e-ee! …Whew!", "The floor is wobbly!", "Round and round goes my head!",
        "I'm seeing double. Adorable double.", "World, please stop being a carousel.",
        "My ears are still orbiting me.", "Who put the room on a turntable?",
        "Give me a moment… and a floor that stays put."
    ))

    /**
     * v8.21 — a bottom drawer (filter / category sheet) just opened and the
     * pet hurried over to peek at it from the bottom edge.
     */
    fun drawerLine(): String = pickLine(listOf(
        "Ooh, a drawer!", "Peek peek, what's in there?", "Can I come too?",
        "Hmm, so many choices!", "Ooh, filters!", "What are we picking?",
        "I'll wait right here!", "Ooh, shiny options!", "A secret compartment!",
        "Paws up! …For picking, I mean.", "Ooh, a menu of everything!",
        "Choices, choices, little choices!", "I love a good drawer.",
        "What's behind door number drawer?"
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
                "Peek-a-boo!", "I see you!", "Hidden! …Found! Dang.",
                "Boo! …It's me. Cute boo.", "Peek! …peek! …PEEK!",
                "You can't see me. You saw me.", "Now you see me! …Me again!",
                "Surprise! It's a face! Mine!", "Crouch… and POP!",
                "Sneak sneak sneak—HI!", "I was here the whole time. Suspicious.",
                "Boop from my hiding spot!"
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
                "Chameleon mode… ON!", "Can you see me? …Wait, no, don't answer!",
                "I'm part of the wallpaper now.", "Vanish! …Reappear! Ta-da!",
                "Fade to… me again!", "I blend in. It's a talent.",
                "Poof! …Poof back!", "Sneak 100. I'm basically invisible.",
                "Camouflage activated!", "Now I'm here! …Now I'm not! …Now I am!",
                "Hiding is my love language.", "Did I startle you? Good. I mean, sorry. I mean, again?"
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
                "A spark! Mine!", "Catch the spark!", "Ooh, shiny falling thing!",
                "Sparkle dash!", "Got it! …Almost got it! …Got it!",
                "Falling stars are FASTER than me. Impressive.", "Zooms!",
                "One tiny spark, one big pounce!", "I caught a star! Sort of!",
                "Chase chase chase—caught!", "The spark didn't stand a chance.",
                "Gravity vs me: round one, me!"
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
        // v14 — the BABY voice is telegraphic and the fully grown voice is
        // its own mature register: neither composes the brain's stats
        // sentences, so both always speak from their own pools.
        return when (currentStage()) {
            Stage.BABY, Stage.FINAL_EVO -> lineFor(context, currentMood, lanes)
            Stage.FIRST_EVO -> CurioPetBrain.say(context, currentMood, lanes)
                ?: lineFor(context, currentMood, lanes)
        }
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

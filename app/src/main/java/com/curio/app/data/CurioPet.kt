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
        val next = nextStage(stage) ?: return "Fully grown. Every lane is yours."
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
     * v8.25 — UI-suppression flag: while a modal dialog shows its OWN pet
     * sprite (the onboarding tour-ask), the floating pet is hidden so the
     * pet never appears twice (once in the dialog, once wandering behind
     * it). Set/cleared by the screen that owns the dialog via
     * [setFloatingSuppressed].
     */
    var floatingSuppressed by mutableStateOf(false)
        private set

    /** v8.25 — hide ([value] = true) or restore the floating pet. */
    fun setFloatingSuppressed(value: Boolean) {
        floatingSuppressed = value
    }

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
            "It landed!", "Ooh, the deck chose well!", "A new topic, a new tale!"
        ).random()
        // v8.16 — the line adapts to the auto-open preference: when the
        // reveal opens BY ITSELF (auto-open ON) the pet cheers the surprise
        // instead of nagging "Open it, open it!" at an already-open page.
        // With auto-open OFF the reveal only opens on the user's tap, so
        // the eager "Open it!" cheer stays.
        Event.REVEAL_OPEN -> if (AppPreferences.autoOpenRevealState)
            listOf(
                "There it is!", "It opened itself, sneaky!", "Ta-da! A new tale!",
                "Ooh, look what landed!", "Surprise!"
            ).random()
        else
            listOf(
                "Open it, open it!", "Come see what's inside!", "I'm dying of curiosity!"
            ).random()
        Event.EXPLORE -> listOf(
            "Go explore!", "Adventure time!", "I'll wait right here. Go see!"
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
    }

    /** The pet played (a dart / self-started game — persisted). */
    fun notePlay(context: Context) {
        val p = prefs(context)
        p.edit()
            .putInt(KEY_PET_PLAYS, p.getInt(KEY_PET_PLAYS, 0) + 1)
            .putLong(KEY_LAST_PLAY_AT, System.currentTimeMillis())
            .apply()
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
    fun morningGreeting(): String = listOf(
        "Good morning!", "Morning! Ready for a spin?", "Rise and shine!",
        "Fresh day, fresh topics!"
    ).random()

    // ── Moods (spec §10.5) — derived from recent activity, never shaming ──
    // v8.13 — two more status moods: FOCUSED (the user is writing/saving on
    // the capture screen) and BOUNCY (a play session just ended — the pet is
    // still full of beans). All moods are state-derived, so the pet reads
    // the user's behavior instead of wearing a fixed face.
    enum class Mood { PROUD, EXCITED, HAPPY, CURIOUS, SLEEPY, FOCUSED, BOUNCY }

    fun mood(context: Context, lanes: Set<String>, screen: String? = null): Mood {
        val now = System.currentTimeMillis()
        return when {
            now - lastLevelUpAt(context) < 90_000L -> Mood.PROUD
            now - lastNewLaneAt(context) < 60_000L -> Mood.EXCITED
            // The user is mid-capture — the pet stays quiet and attentive.
            screen == "capture" -> Mood.FOCUSED
            // A play session just ended (a dart, a self-started game).
            now - lastPlayAt(context) < 5 * 60_000L -> Mood.BOUNCY
            now - lastXpAt(context) < 120_000L -> Mood.HAPPY
            leastExploredLane(context, lanes) != null -> Mood.CURIOUS
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
        "Fresh paths ahead!"
    )
    private val happyLines = listOf(
        "Nice! XP banked. Keep going?",
        "That was fun. More?",
        "Curiosity looks good on you.",
        "Ooh, we're on a roll!",
        "Doing that again? Please?"
    )
    // v8.14 — the HAPPY mood wears the hour's voice: morning energy, cozy
    // evening, hushed night.
    private val morningLines = listOf(
        "Morning! The deck smells fresh.",
        "Rise and shine. Something new is waiting.",
        "Fresh eyes, fresh topics. Let's go!",
        "Good morning! I saved your spot."
    )
    private val afternoonLines = listOf(
        "Afternoon wander? Let's go.",
        "Bright and busy. A good time to peek.",
        "Midday! Perfect for a quick spin."
    )
    private val eveningLines = listOf(
        "Evening! Cozy hour, warm lamp.",
        "The day's winding down. One more spin?",
        "Evening glow. Nice time for a discovery."
    )
    private val nightLines = listOf(
        "Past my bedtime… but for you, I'll stay.",
        "Shh, night mode. One quiet spin?",
        "The stars are out. The deck still shines."
    )
    private val curiousLines = listOf(
        "We haven't tried __LANE__ yet. Want a new stamp?",
        "I wonder what __LANE__ hides…",
        "Pssst, __LANE__ is calling."
    )
    private val sleepyLines = listOf(
        "I'll keep your seat warm. Come spin when you're ready.",
        "Yawn… the deck can wait a moment.",
        "Soft blanket, warm lamp… I'm ready when you are."
    )
    // v8.13 — the new moods' lines: focused keeps out of the way while the
    // user writes; bouncy rides the post-play high.
    private val focusedLines = listOf(
        "Write it down. I'll guard your thoughts.",
        "Quiet paws, I promise.",
        "Take your time. This one's a keeper."
    )
    private val bouncyLines = listOf(
        "Phew, that was fun. Again soon?",
        "I'm still bouncing from that game!",
        "Best play date ever. …Round two?"
    )

    /** A passive bubble line for the current [mood]. */
    fun lineFor(context: Context, mood: Mood, lanes: Set<String>): String = when (mood) {
        // Inlined so the level reads live, not baked at first access.
        Mood.PROUD -> listOf(
            "Level ${CurioQuests.levelForXp(CurioQuests.xpState)}. I grew a little!",
            "Shiny! We leveled up together.",
            "Do you feel that? That's growth!"
        ).random()
        Mood.EXCITED -> excitedLines.random()
        Mood.HAPPY -> when (timeOfDay()) {
            TimeOfDay.MORNING -> morningLines.random()
            TimeOfDay.AFTERNOON -> afternoonLines.random()
            TimeOfDay.EVENING -> eveningLines.random()
            TimeOfDay.NIGHT -> nightLines.random()
        }
        Mood.CURIOUS -> {
            val lane = leastExploredLane(context, lanes)
            if (lane != null) {
                curiousLines.map { it.replace("__LANE__", lane.displayName) }.random()
            } else {
                "Spin something new today?"
            }
        }
        Mood.FOCUSED -> focusedLines.random()
        Mood.BOUNCY -> bouncyLines.random()
        Mood.SLEEPY -> sleepyLines.random()
    }

    /** A short cheer while the Spin deck is reeling (v8.13). */
    fun spinCheer(): String = listOf(
        "Go, go, go!", "Spinny spin!", "Ooh, where will it land?",
        "Come on, good one!", "Round and round!", "I can't watch. Okay, I'm watching."
    ).random()

    /**
     * A short burst when the user touches/pets the floating pet (v8.11).
     * [tier] grows with rapid repeated taps (computed by the overlay from
     * the tap streak): 1 = a soft boop, 2 = playful, 3+ = a happy
     * celebration. v8.21 — the DIZZY tier moved to DRAGGING (the pet gets
     * flung around), so tapping never spins it anymore — rapid taps just
     * escalate from boop to play-bow to a big happy bounce.
     */
    fun touchReaction(tier: Int): String = when {
        tier >= 3 -> listOf(
            "Yay!", "I love boops!", "Best friends!", "Squee!",
            "More, more, more!", "You're my favorite!", "Party time!"
        ).random()
        tier >= 2 -> listOf(
            "Hehehe!", "More, more!", "This is fun!", "Tag, you're it!",
            "Catch me!", "Bouncy bouncy!", "Again, again!"
        ).random()
        else -> listOf(
            "Boop!", "Hehe!", "Wheee!", "Ooh!", "That tickles!", "Hihi!",
            "Boop boop!", "Again!", "You found me!", "Poke!", "Hi hi hi!",
            "Soft paws!", "Mrow!", "Pfft!"
        ).random()
    }

    /**
     * The pet sometimes starts a game on its own (v8.11) — it play-bows,
     * calls out, then zooms off. Kept short: one sentence max.
     */
    fun playInitiation(): String = listOf(
        "Wanna play? Catch me!", "Boop! You're it!", "I'm feeling bouncy!",
        "Zoom zoom, chase me!", "Play with me!", "Tag! Your turn!",
        "I'm bored, come chase me!"
    ).random()

    /**
     * v8.16 — the pet's line when it pokes something on the current screen:
     * [funThing] = a button/gadget (boop! hearts!), otherwise a curious
     * read of some text. One sentence max, matching the passive-bubble rule.
     */
    fun landmarkLine(funThing: Boolean): String =
        (if (funThing) listOf(
            "Boop!", "Ooh, shiny!", "Hehe, hi!", "Tag! You're it!",
            "I like this one!", "Spinny spinny!", "Wheee!", "Boop boop boop!"
        ) else listOf(
            "What's this?", "Hmm, interesting…", "*peeks*", "Read read read!",
            "Ooh, words!", "Let me read this!", "Scribble scribble!"
        )).random()

    /**
     * v8.17 — the pet's line when it does a little jig at a special spot
     * (a PLAY landmark). One sentence max, matching the passive-bubble rule.
     */
    fun jigLine(): String = listOf(
        "Tippy tap tap!", "Happy feet!", "Wiggle wiggle!",
        "Da-da-daaaa!", "Jiggle jiggle!", "Party paws!",
        "Dance break!", "Shake it off!", "Tap dance time!"
    ).random()

    /**
     * v8.21 — the pet got flung around (dragged) and is dizzy: swirl eyes,
     * a wobble, and a groggy line while it recovers.
     */
    fun dizzyLine(): String = listOf(
        "Whoa… the room is spinning!", "Wheee, dizzy!", "Spin spin… okay, stop!",
        "Whoa whoa whoa!", "I think I need a sit-down…", "So dizzy!",
        "We-e-ee! …Whew!", "The floor is wobbly!"
    ).random()

    /**
     * v8.21 — a bottom drawer (filter / category sheet) just opened and the
     * pet hurried over to peek at it from the bottom edge.
     */
    fun drawerLine(): String = listOf(
        "Ooh, a drawer!", "Peek peek, what's in there?", "Can I come too?",
        "Hmm, so many choices!", "Ooh, filters!", "What are we picking?",
        "I'll wait right here!", "Ooh, shiny options!"
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
        return lineFor(context, mood(context, lanes, screen), lanes)
    }

    /** What tapping the pet reveals: mood, personality, growth status. */
    data class TapInfo(
        val mood: Mood,
        val stage: Stage,
        val persona: Persona,
        val nextStageLabel: String,
        val nextQuestTitle: String?
    )

    fun tapInfo(context: Context, lanes: Set<String>): TapInfo {
        val stage = currentStage()
        return TapInfo(
            mood = mood(context, lanes),
            stage = stage,
            persona = persona(context),
            nextStageLabel = nextStageHint(stage),
            nextQuestTitle = CurioQuests.currentQuest()?.title
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
    fun lastPlayAt(context: Context): Long = prefs(context).getLong(KEY_LAST_PLAY_AT, 0L)
}

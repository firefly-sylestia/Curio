package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Curio's quest guided tour (v8.6) — the **First Journey** (spec §7): a
 * tap-along walkthrough that navigates the REAL screens and WAITS for the
 * REAL actions — spin → open the landed topic → start exploring → save a
 * capture → see the Cabinet — so the user finishes the tour having done the
 * whole core loop, not just looked at it.
 *
 * Offered ONCE from the Quests page when the user taps the first quest
 * ("First Spin") and accepts the one-time prompt (v8.2) — never auto-shown
 * from other screens.
 *
 * Presentation is an IN-APP OVERLAY (not a system Toast, not a dialog): a
 * compact floating pill that MOVES WITH THE SCREEN — bottom for the tab
 * screens, below the hero on the settings-family screens, centered on the
 * final step — with a pointer arrow toward the content it describes, a
 * progress-dot indicator and an action label. Steps that WAIT for a real
 * action ([Wait]) disable the action ("Do this to continue") and advance
 * automatically the moment the action happens — [CurioQuests.onSpin],
 * [CurioQuests.onExplore], [CurioQuests.onSave], the reveal screen's open
 * ([Wait.REVEAL]) and Profile/Settings visits report in via [onWait]. The
 * X always closes the tour ("Skip tour", spec §7.3).
 *
 * v8.6 — the tour SURVIVES PROCESS DEATH (spec §7.3): the active flag + step
 * index are persisted whenever they change ([persist], called from the
 * NavHost's tour runner) and restored on launch ([seed], called from
 * MainActivity), so a killed app resumes exactly where it left off.
 *
 * Routes use the same raw names as [QuestStage.navRoute] ("home", "spin",
 * "cabinet", "profile", "quests", "settings"); the NavHost maps them onto
 * real navigation (tabs via navigateToTab, the rest pushed). Steps marked
 * [Step.hold] never auto-navigate the user away mid-flow — they wait for
 * the action on whatever screen it happens (e.g. the reveal's auto-open).
 */
object QuestGuide {

    /** The real-world event a step can wait for before auto-advancing. */
    enum class Wait { SPIN, REVEAL, EXPLORE, SAVE, PROFILE, SETTINGS }

    /** Where the overlay floats on the current screen (v8.3). */
    enum class Position {
        /** Bottom of the screen, pointer up at the content above. */
        BOTTOM,
        /** Just below the settings-family hero, pointer down. */
        TOP,
        /** Mid-screen with no pointer — the final step. */
        CENTER,
        /**
         * v8.12 — bottom but LIFTED above the bottom action row (the
         * capture screen's Save bar): the pill must never cover the button
         * it is pointing at.
         */
        LOWER
    }

    data class Step(
        /** Raw route name — empty means "no navigation": the final step. */
        val route: String,
        val title: String,
        val message: String,
        val waitFor: Wait? = null,
        val position: Position = Position.BOTTOM,
        /**
         * v8.22 — the landmark id this step HIGHLIGHTS (the pet-guide draws
         * its pass-through window over the REAL button's bounds, not a
         * guessed zone). The screen registers it via [PetLandmarks]; null =
         * fall back to the position-based window.
         */
        val targetLandmark: String? = null,
        /**
         * v8.6 — true for action-wait steps: the runner only guides the user
         * back to [route] when they're PARKED on a bottom-nav tab, and never
         * yanks them away mid-flow (e.g. off the reveal while it auto-open
         * after a spin). The step advances via [onWait] when the action
         * happens, wherever that is.
         */
        val hold: Boolean = false,
        /**
         * v8.12 — optional "skip this step" label on wait steps ("Explore
         * later" / "Skip"): the user can move on WITHOUT doing the action
         * (spec §7.3 — the tour guides, it never blocks). Null = no skip.
         */
        val skipLabel: String? = null
    )

    // ── Reactive state (mirrors CurioQuests' pattern) ──
    var active by mutableStateOf(false)
        private set
    var steps by mutableStateOf<List<Step>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set

    /** The step the tour is currently showing, or null when idle. */
    val current: Step? get() = steps.getOrNull(index)

    /** True on the final step — the overlay's button reads "Finish". */
    val isLast: Boolean get() = active && index >= steps.lastIndex

    /** The very first quest a new user sees — tapping it launches the tour. */
    val firstQuestId: String? get() = CurioQuests.allStages().firstOrNull()?.id

    /** Starts the full walkthrough from the first step. */
    fun start() {
        steps = buildTourSteps()
        index = 0
        active = true
    }

    /** Advances one step; the final step's Finish ends the tour. */
    fun next() {
        if (!active) return
        if (index >= steps.lastIndex) stop() else index += 1
    }

    /** Ends the tour (Finish button or the overlay's close X). */
    fun stop() {
        active = false
        steps = emptyList()
        index = 0
    }

    /** Event-driven advance — reported by the [CurioQuests] hooks. */
    fun onWait(wait: Wait) {
        if (!active) return
        val step = current ?: return
        if (step.waitFor == wait) next()
    }

    // ── Persistence (v8.6 — spec §7.3: the tour survives process death) ──
    private const val PREFS_NAME = "curio_guide"
    private const val KEY_ACTIVE = "active"
    private const val KEY_INDEX = "index"

    /** Restores an in-flight tour (called once from MainActivity onCreate). */
    fun seed(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!p.getBoolean(KEY_ACTIVE, false)) return
        steps = buildTourSteps()
        index = p.getInt(KEY_INDEX, 0).coerceIn(0, steps.lastIndex.coerceAtLeast(0))
        active = true
    }

    /** Persists the exact active state + step (called from the NavHost runner). */
    fun persist(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACTIVE, active)
            .putInt(KEY_INDEX, index)
            .apply()
    }

    // v8.3 — one-line messages (the pill shows at most two lines) and a
    // per-step position so the pill never floats over the thing it explains.
    // v8.6 — the First Journey core loop (spec §7.2): Home → Quests → Spin →
    // open the landed topic → Start exploring → Capture/Save → Cabinet →
    // Reward & pet growth. The action-wait steps (SPIN/REVEAL/EXPLORE/SAVE)
    // advance the moment the user really does the thing.
    private fun buildTourSteps(): List<Step> = listOf(
        Step(
            "home", "Welcome to Curio",
            "This is home — your daily quests, the deck, and everything you've saved all live here."
        ),
        Step(
            "quests", "Daily Quest Hub",
            "Check the daily quests whenever you like — they're the fastest way to grow.",
            position = Position.TOP,
            targetLandmark = "daily"
        ),
        Step(
            "spin", "Pick a lane & spin",
            // v8.16 — copy adapts to the auto-open preference: with it ON
            // (the default since v8.21) the reveal opens by itself; OFF the
            // deck lands quietly and the user taps the card to open it.
            if (AppPreferences.autoOpenRevealState)
                "Give Shuffle a tap — the deck picks something fresh and opens it for you."
            else
                "Give Shuffle a tap — the deck picks something fresh for you.",
            waitFor = Wait.SPIN,
            skipLabel = "Skip",
            targetLandmark = "spin"
        ),
        Step(
            "spin", "Open the landed topic",
            if (AppPreferences.autoOpenRevealState)
                "Nice one! It's already open — read the teaser, then start exploring."
            else
                "Nice one! Tap the card to open the teaser, then start exploring.",
            waitFor = Wait.REVEAL,
            hold = true,
            position = Position.TOP,
            skipLabel = "Skip",
            targetLandmark = "deck"
        ),
        Step(
            "", "Start exploring",
            "Ready? Tap Start exploring on the topic — or save it for later.",
            waitFor = Wait.EXPLORE,
            hold = true,
            // v8.15 — the Start exploring button lives in the reveal's
            // bottom action dock; v8.22 — its REAL bounds are the highlight.
            position = Position.BOTTOM,
            skipLabel = "Explore later",
            targetLandmark = "start-exploring"
        ),
        Step(
            "", "Capture what you found",
            "Back from exploring? Jot it down — saving a note makes the discovery yours.",
            waitFor = Wait.SAVE,
            hold = true,
            // v8.12 — the pill floats ABOVE the Save bar so it never covers
            // the button it's pointing at.
            position = Position.LOWER,
            skipLabel = "Skip",
            targetLandmark = "save"
        ),
        Step(
            "cabinet", "The Cabinet",
            "This is the Cabinet — every keepsake you save lands here.",
            position = Position.TOP,
            targetLandmark = "grid"
        ),
        Step(
            "quests", "Reward & pet growth",
            "Every curious act earns XP and feeds your Curio pet — watch it grow!",
            position = Position.TOP
        ),
        Step(
            "", "You're all set",
            "That's the loop — spin, explore, save. You're all set!",
            position = Position.CENTER
        )
    )
}

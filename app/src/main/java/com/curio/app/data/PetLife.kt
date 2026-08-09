package com.curio.app.data

import kotlin.random.Random

/**
 * Authored viewpoints for the pet. A frame can carry one of these labels so
 * the renderer can show a deliberate angle instead of treating every moment
 * as a front-facing bounce.
 */
enum class PetViewAngle {
    FRONT,
    THREE_QUARTER,
    SIDE,
    BACK,
    LOOKING_UP,
    LOOKING_DOWN,
    CURLED
}

/** A short, context-aware behavior chosen by the Pet Life director. */
data class PetLifeRoutine(
    val id: String,
    val animationId: String,
    val view: PetViewAngle = PetViewAngle.FRONT,
    val line: String? = null
)

/**
 * Chooses small routines instead of repeatedly selecting the same generic
 * landmark reaction. The recent-id input is kept outside this object so the
 * composable owner can scope memory to one visible companion instance.
 */
object PetLifeDirector {
    private val fallback = listOf(
        PetLifeRoutine("look-around", "glance", PetViewAngle.THREE_QUARTER, "Hmm… what should we inspect?"),
        PetLifeRoutine("little-wave", "wave", PetViewAngle.SIDE, "Hi from over here!"),
        PetLifeRoutine("stretch", "stretch", PetViewAngle.LOOKING_DOWN, "A good stretch makes room for ideas."),
        PetLifeRoutine("turn-and-peek", "sidepeek", PetViewAngle.SIDE, "Psst… I see something!"),
        PetLifeRoutine("tiny-stumble", "stumble", PetViewAngle.THREE_QUARTER, "Whoops! Nailed it."),
        PetLifeRoutine("look-up", "look_up", PetViewAngle.LOOKING_UP, "The ceiling has thoughts too."),
        PetLifeRoutine("backstage", "backturn", PetViewAngle.BACK, "Just checking my best side."),
        PetLifeRoutine("victory-pose", "victory", PetViewAngle.THREE_QUARTER, "That deserves a pose!")
    )

    private val home = listOf(
        PetLifeRoutine("home-stretch", "stretch", PetViewAngle.LOOKING_DOWN, "Home stretch! Literally."),
        PetLifeRoutine("room-tour", "backturn", PetViewAngle.BACK, "Come see what I found."),
        PetLifeRoutine("window-watch", "look_up", PetViewAngle.LOOKING_UP, "The view is lovely today."),
        PetLifeRoutine("cozy-turn", "sidepeek", PetViewAngle.SIDE, "One tiny house inspection."),
        PetLifeRoutine("home-dance", "victory", PetViewAngle.THREE_QUARTER, "Dance break in the house!")
    )

    private val spin = listOf(
        PetLifeRoutine("deck-anticipation", "look_up", PetViewAngle.LOOKING_UP, "Come on… pick a good one."),
        PetLifeRoutine("deck-side-peek", "sidepeek", PetViewAngle.SIDE, "I can almost see the answer."),
        PetLifeRoutine("deck-stretch", "stretch", PetViewAngle.LOOKING_DOWN, "Ready, set, spin!"),
        PetLifeRoutine("deck-victory", "victory", PetViewAngle.THREE_QUARTER, "That landing had style!")
    )

    private val quests = listOf(
        PetLifeRoutine("quest-read", "inspect", PetViewAngle.LOOKING_DOWN, "Let me read the fine print."),
        PetLifeRoutine("quest-wave", "wave", PetViewAngle.SIDE, "I believe in this quest!"),
        PetLifeRoutine("quest-proud", "victory", PetViewAngle.THREE_QUARTER, "Quest energy activated!"),
        PetLifeRoutine("quest-hide", "backturn", PetViewAngle.BACK, "I’ll be your mysterious quest guide.")
    )

    private val reveal = listOf(
        PetLifeRoutine("topic-peek", "sidepeek", PetViewAngle.SIDE, "Should we peek together?"),
        PetLifeRoutine("topic-wow", "look_up", PetViewAngle.LOOKING_UP, "Ooh. That one is glowing."),
        PetLifeRoutine("topic-spin", "victory", PetViewAngle.THREE_QUARTER, "A story worth celebrating!"),
        PetLifeRoutine("topic-inspect", "inspect", PetViewAngle.LOOKING_DOWN, "Let’s look closer.")
    )

    private val capture = listOf(
        PetLifeRoutine("writing-focus", "inspect", PetViewAngle.LOOKING_DOWN, "I’m guarding this thought."),
        PetLifeRoutine("writing-wave", "wave", PetViewAngle.SIDE, "Take your time. I’ll wait."),
        PetLifeRoutine("writing-stretch", "stretch", PetViewAngle.LOOKING_DOWN, "A stretch, then back to the good words."),
        PetLifeRoutine("writing-shy", "sidepeek", PetViewAngle.SIDE, "That thought looks important…")
    )

    private val cabinet = listOf(
        PetLifeRoutine("shelf-hunt", "inspect", PetViewAngle.LOOKING_DOWN, "Which keepsake should we revisit?"),
        PetLifeRoutine("shelf-wave", "wave", PetViewAngle.SIDE, "Your shelf is looking good!"),
        PetLifeRoutine("shelf-backstage", "backturn", PetViewAngle.BACK, "I’m checking the back row."),
        PetLifeRoutine("shelf-proud", "victory", PetViewAngle.THREE_QUARTER, "Look at all those discoveries!")
    )

    private val profile = listOf(
        PetLifeRoutine("mirror-check", "backturn", PetViewAngle.BACK, "Do I look wise from back here?"),
        PetLifeRoutine("profile-wave", "wave", PetViewAngle.SIDE, "Hello, profile page!"),
        PetLifeRoutine("profile-proud", "victory", PetViewAngle.THREE_QUARTER, "Your progress is sparkling."),
        PetLifeRoutine("profile-look-up", "look_up", PetViewAngle.LOOKING_UP, "There’s always another level.")
    )

    /** Selects a routine while avoiding the most recently used routine ids. */
    fun choose(
        screen: String?,
        landmarkKind: String?,
        persona: CurioPet.Persona,
        recentIds: Set<String>,
        random: Random = Random.Default
    ): PetLifeRoutine {
        val route = screen?.substringBefore('/').orEmpty()
        val source = when {
            landmarkKind == "PLAY" -> home + spin
            route == "home" -> home
            route == "spin" -> spin
            route == "quests" -> quests
            route == "reveal" -> reveal
            route == "capture" -> capture
            route == "cabinet" -> cabinet
            route == "profile" -> profile
            else -> fallback
        }
        val eligible = source.filter { it.id !in recentIds }
        val pool = if (eligible.isNotEmpty()) eligible else source
        val personaPool = when (persona) {
            CurioPet.Persona.BOUNCY -> pool.filter { it.animationId == "victory" || it.animationId == "stumble" || it.animationId == "wave" }
            CurioPet.Persona.EXPLORER -> pool.filter { it.view != PetViewAngle.FRONT && it.animationId != "victory" }
            CurioPet.Persona.CUDDLY -> pool.filter { it.animationId == "stretch" || it.animationId == "sidepeek" || it.animationId == "wave" }
            CurioPet.Persona.SPARKY -> pool.filter { it.animationId == "look_up" || it.animationId == "sidepeek" || it.animationId == "inspect" }
        }
        return (personaPool.ifEmpty { pool }).random(random)
    }
}

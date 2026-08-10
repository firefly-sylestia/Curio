package com.curio.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.curio.app.navigation.CurioRoutes

/**
 * Transient controller for Curie's pet-led product tour. The tour never
 * persists progress or performs the action behind a demonstrated control.
 */
object TourController {
    data class Step(
        val id: String,
        val route: String,
        val routePrefix: String,
        val landmarkId: String,
        val dialogue: String,
        val nextHint: String
    )

    val steps: List<Step> = listOf(
        Step(
            id = "home-quest",
            route = CurioRoutes.HOME,
            routePrefix = CurioRoutes.HOME,
            landmarkId = "quest",
            dialogue = "Let’s take a tiny tour. Tap the shuffle button and I’ll show you around.",
            nextHint = "A safe demo — nothing starts yet."
        ),
        Step(
            id = "spin-button",
            route = CurioRoutes.SPIN,
            routePrefix = CurioRoutes.SPIN,
            landmarkId = "spin",
            dialogue = "Here is the deck. Tap Spin and I’ll take you to the next stop without spinning it.",
            nextHint = "The action is only being demonstrated."
        ),
        Step(
            id = "express-yourself",
            route = CurioRoutes.revealForBrowse("artists", "David Bowie"),
            routePrefix = CurioRoutes.REVEAL.substringBefore("/"),
            landmarkId = "express-yourself",
            dialogue = "When something sparks a thought, you can tap Express yourself to write it down. This tour won’t open it — tap Next when you’re ready.",
            nextHint = "The note stays closed on the tour."
        ),
        Step(
            id = "cabinet",
            route = CurioRoutes.CABINET,
            routePrefix = CurioRoutes.CABINET,
            landmarkId = "grid",
            dialogue = "This is the Cabinet — every keepsake you save lands here.",
            nextHint = "Everything you keep collects here."
        ),
        Step(
            id = "topic-browser",
            route = CurioRoutes.DATABASE,
            routePrefix = CurioRoutes.DATABASE,
            landmarkId = "search",
            dialogue = "Browse Topics is the whole catalog — every artist, film, book, and discovery, ready to explore.",
            nextHint = "Search and explore any lane."
        ),
        Step(
            id = "profile",
            route = CurioRoutes.PROFILE,
            routePrefix = CurioRoutes.PROFILE,
            landmarkId = "avatar",
            dialogue = "Profile is where your journey lives — XP, badges, and your streak.",
            nextHint = "Your progress lives here."
        ),
        Step(
            id = "quests",
            route = CurioRoutes.QUESTS,
            routePrefix = CurioRoutes.QUESTS,
            landmarkId = "daily",
            dialogue = "Quests give you a tiny daily goal — the fastest way to grow.",
            nextHint = "A little curiosity every day."
        ),
        Step(
            id = "settings",
            route = CurioRoutes.SETTINGS,
            routePrefix = CurioRoutes.SETTINGS,
            landmarkId = "appearance",
            dialogue = "And this is Settings — where you make Curio yours: theme, permissions, your pet. That’s everything — you’re all set to explore!",
            nextHint = "That’s the whole tour."
        ),
    )

    var active by mutableStateOf(false)
        private set
    var stepIndex by mutableIntStateOf(0)
        private set
    var offerPending by mutableStateOf(false)
        private set

    val currentStep: Step? get() = steps.getOrNull(stepIndex).takeIf { active }

    /** True on the final stop — the tour controls label the button "Done". */
    val isLastStep: Boolean get() = active && stepIndex >= steps.lastIndex

    fun offer() { offerPending = true }
    fun declineOffer() { offerPending = false }

    fun start() {
        offerPending = false
        stepIndex = 0
        active = true
        CurioPet.wake()
        CurioPet.comeOut()
    }

    fun skip() {
        active = false
        offerPending = false
        stepIndex = 0
    }

    fun advance() {
        if (!active) return
        if (stepIndex >= steps.lastIndex) {
            active = false
            stepIndex = 0
        } else {
            stepIndex++
        }
    }

    fun routeForCurrentStep(): String? = currentStep?.route

    /** Returns true only for the currently demonstrated landmark. */
    fun consumeTap(landmarkId: String): Boolean {
        val step = currentStep ?: return false
        if (step.landmarkId != landmarkId) return false
        advance()
        return true
    }
}

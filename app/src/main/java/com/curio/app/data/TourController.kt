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
            dialogue = "Let\u2019s take a tiny tour! I\u2019ll walk you through everything.",
            nextHint = "Tap the Shuffle button when you\u2019re ready."
        ),
        Step(
            id = "spin-button",
            route = CurioRoutes.SPIN,
            routePrefix = CurioRoutes.SPIN,
            landmarkId = "spin",
            dialogue = "Here\u2019s the deck \u2014 every spin lands a fresh topic. Tap it and we\u2019ll keep going!",
            nextHint = "Every spin deals a fresh topic."
        ),
        Step(
            id = "express-yourself",
            route = CurioRoutes.revealForBrowse("artists", "David Bowie"),
            routePrefix = CurioRoutes.REVEAL.substringBefore("/"),
            landmarkId = "express-yourself",
            dialogue = "When something sparks a thought, tap Express yourself to write it down \u2014 your keepsakes collect in the Cabinet.",
            nextHint = "Your notes land in the Cabinet."
        ),
        Step(
            id = "cabinet",
            route = CurioRoutes.CABINET,
            routePrefix = CurioRoutes.CABINET,
            landmarkId = "grid",
            dialogue = "This is the Cabinet \u2014 every keepsake you save lands here.",
            nextHint = "Everything you keep collects here."
        ),
        Step(
            id = "topic-browser",
            route = CurioRoutes.DATABASE,
            routePrefix = CurioRoutes.DATABASE,
            landmarkId = "search",
            dialogue = "Browse Topics is the whole catalog \u2014 every artist, film, book, and discovery, ready to explore.",
            nextHint = "Search and explore any lane."
        ),
        Step(
            id = "profile",
            route = CurioRoutes.PROFILE,
            routePrefix = CurioRoutes.PROFILE,
            landmarkId = "avatar",
            dialogue = "Profile is where your journey lives \u2014 XP, badges, and your streak.",
            nextHint = "Your progress lives here."
        ),
        Step(
            id = "quests",
            route = CurioRoutes.QUESTS,
            routePrefix = CurioRoutes.QUESTS,
            landmarkId = "daily",
            dialogue = "Quests give you a tiny daily goal \u2014 the fastest way to grow.",
            nextHint = "A little curiosity every day."
        ),
        Step(
            id = "settings",
            route = CurioRoutes.SETTINGS,
            routePrefix = CurioRoutes.SETTINGS,
            landmarkId = "appearance",
            dialogue = "And this is Settings \u2014 where you make Curio yours: theme, permissions, your pet. That\u2019s everything \u2014 you\u2019re all set!",
            nextHint = "That\u2019s the whole tour."
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

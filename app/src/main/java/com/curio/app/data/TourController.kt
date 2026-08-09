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
            dialogue = "When something sparks a thought, tap Express yourself to write it down.",
            nextHint = "This tour will not open or save a note."
        ),
        Step(
            id = "google-youtube",
            route = CurioRoutes.revealForBrowse("artists", "David Bowie"),
            routePrefix = CurioRoutes.REVEAL.substringBefore("/"),
            landmarkId = "start-exploring",
            dialogue = "For a real deep dive later, Explore now gives you Google or YouTube.",
            nextHint = "Tap to see the idea — this tour will not open either one."
        )
    )

    var active by mutableStateOf(false)
        private set
    var stepIndex by mutableIntStateOf(0)
        private set
    var offerPending by mutableStateOf(false)
        private set

    val currentStep: Step? get() = steps.getOrNull(stepIndex).takeIf { active }

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

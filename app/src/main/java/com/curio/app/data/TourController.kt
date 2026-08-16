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
        // v118 — dialogue + hints ported from the canonical dialog doc §10.
        Step(
            id = "home-quest",
            route = CurioRoutes.HOME,
            routePrefix = CurioRoutes.HOME,
            landmarkId = "quest",
            dialogue = "Hi! I'm your little curiosity buddy. Want to take a tiny tour together?",
            nextHint = "Tap Shuffle when you're ready."
        ),
        Step(
            id = "spin-button",
            route = CurioRoutes.SPIN,
            routePrefix = CurioRoutes.SPIN,
            landmarkId = "spin",
            dialogue = "This is where we find something new. Give it a spin and I'll peek with you!",
            nextHint = "Spin to discover something new."
        ),
        Step(
            id = "express-yourself",
            route = CurioRoutes.revealForBrowse("artists", "David Bowie"),
            routePrefix = CurioRoutes.REVEAL.substringBefore("/"),
            landmarkId = "express-yourself",
            dialogue = "Ooh, did that spark a thought? Tap Express yourself and tell me about it. I'll keep it safe.",
            nextHint = "Save your thoughts with a keepsake."
        ),
        Step(
            id = "cabinet",
            route = CurioRoutes.CABINET,
            routePrefix = CurioRoutes.CABINET,
            landmarkId = "grid",
            dialogue = "This is our Cabinet! Everything you choose to keep comes home here.",
            nextHint = "Your keepsakes live here."
        ),
        Step(
            id = "topic-browser",
            route = CurioRoutes.DATABASE,
            routePrefix = CurioRoutes.DATABASE,
            landmarkId = "search",
            dialogue = "Want something specific? Browse Topics lets you look through everything and find exactly what you're curious about.",
            nextHint = "Search or browse any topic."
        ),
        Step(
            id = "profile",
            route = CurioRoutes.PROFILE,
            routePrefix = CurioRoutes.PROFILE,
            landmarkId = "avatar",
            dialogue = "This is your little journey. You can see your progress, badges, streak, and how much you've discovered.",
            nextHint = "Your progress lives here."
        ),
        Step(
            id = "quests",
            route = CurioRoutes.QUESTS,
            routePrefix = CurioRoutes.QUESTS,
            landmarkId = "daily",
            dialogue = "These are tiny things you can do each day. Finish them with me and we'll keep your curiosity moving!",
            nextHint = "A little curiosity every day."
        ),
        Step(
            id = "settings",
            route = CurioRoutes.SETTINGS,
            routePrefix = CurioRoutes.SETTINGS,
            landmarkId = "appearance",
            dialogue = "And here you can make things feel like you. You can choose your theme, manage permissions, and... hehe, you can design me!",
            nextHint = "Make Curio yours."
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

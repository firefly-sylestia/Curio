package com.curio.app.data

import java.util.Calendar
import kotlin.math.min

/**
 * Android-side encoder for the Python `FEATURE_NAMES` contract.
 *
 * Curio currently has no physical room simulator in the app, so unavailable
 * world/interaction channels remain zero rather than receiving fake meaning.
 * Real app signals are mapped where they exist: current screen/activity,
 * time-of-day, owner presence (the user is in the app), recent XP/lane events,
 * and persistent quest counters. This conservative boundary lets the model
 * run without pretending the app knows hunger, object positions, or drag
 * physics that it does not yet collect.
 */
object NeuralPetObservation {
    private const val SIZE = 128

    @Suppress("UNUSED_PARAMETER")
    fun encode(context: android.content.Context, screen: String): FloatArray {
        val values = FloatArray(SIZE)
        fun put(index: Int, value: Float) {
            values[index] = value.coerceIn(-1f, 1f)
        }
        fun oneHot(index: Int, active: Boolean) {
            if (active) put(index, 1f)
        }

        // Internal state: unavailable physical needs remain zero. These are
        // real Curio signals, not fabricated hunger/energy values.
        val now = System.currentTimeMillis()
        val lastXp = CurioPet.lastXpAt(context)
        val lastInteractionAge = if (lastXp == 0L) 1f
        else min(1f, (now - lastXp).coerceAtLeast(0L) / 3_600_000f)
        put(21, lastInteractionAge) // time_since_interaction
        oneHot(22, currentHour() in 6..11)
        oneHot(23, currentHour() in 12..17)
        oneHot(24, currentHour() in 18..21)
        oneHot(25, currentHour() !in 6..21)
        put(26, 1f) // owner_present: the user is actively in the app
        put(27, 0f) // owner_distance: no spatial owner model in Android yet
        // Screen identity is deliberately not written into activity
        // channels: a screen is not the same thing as the pet physically
        // eating, sleeping, playing, exploring, or socializing.

        // Interaction channels: current app events are not passed as a live
        // gesture here yet. Zero means "no event observed in this sample".
        // The existing CurioPet hooks remain the source for the fallback brain.

        // Environment channels: the Android app has no room/object simulator
        // yet, so food/water/bed/toy positions and availability stay zero.

        // Persistent-memory channels are the only history slots populated
        // by this first Android adapter. Previous physical action/reward
        // channels remain zero until the app records those exact events.
        val lifetime = CurioQuests.lifetimeState
        put(120, min(1f, lifetime.spins / 100f))
        put(121, min(1f, lifetime.explores / 100f))
        put(122, min(1f, lifetime.saves / 100f))
        put(123, min(1f, lifetime.quotes / 100f))
        put(124, min(1f, lifetime.pins / 100f))
        put(125, min(1f, lifetime.likes / 100f))
        put(126, min(1f, CurioQuests.xpState / 10_000f))
        put(127, min(1f, CurioQuests.bestStreakState / 30f))

        return values
    }

    private fun currentHour(): Int =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}

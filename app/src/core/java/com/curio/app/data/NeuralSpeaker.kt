package com.curio.app.data

import android.content.Context

/**
 * ── v465c — THE CORE EDITION'S NEURAL VOICE, WHICH IS DELIBERATELY NOTHING ──
 *
 * The twin of `app/src/full/java/com/curio/app/data/NeuralSpeaker.kt`: same
 * package, same name, same signatures, no behaviour. The full one imports
 * `com.k2fsa.sherpa.onnx.*`, which exists only in the full edition (the vendored
 * AAR is `fullImplementation`), so this file is what lets `main` name the object
 * at all.
 *
 * **[isReady] is false and stays false**, and that is the whole safety story: the
 * reader asks [isReady] before it routes a sentence here, so the core edition
 * falls through to the phone's own voice exactly as it did before this feature
 * existed — no flag check needed at the call site, no dead control, and no path
 * that can reach a class that is not in the APK.
 *
 * **[say] still calls the callback**, because a speaker that silently swallows a
 * sentence would stop the reader's driver dead if anything ever did route to it.
 *
 * Adding a member to one file means adding it to the other in the same change,
 * or the core edition stops compiling.
 */
internal object NeuralSpeaker {

    val isReady: Boolean get() = false

    /**
     * v465j — the full edition's per-pack form of [isReady] (see its own note on
     * why "an engine is loaded" and "THIS pack's engine is loaded" are different
     * questions). There is never an engine here, so it is false for every id.
     */
    fun isReadyFor(id: String?): Boolean = false

    fun speakerCount(): Int = 1

    /**
     * v469 — the full edition's synthesis-ahead (see its own note on why the
     * sentence after this one is made while this one plays). Nothing here ever
     * speaks, so there is never a sentence to make ahead of anything.
     */
    fun prefetch(text: String, speed: Float, speakerId: Int) = Unit

    fun prepare(context: Context, pack: NeuralVoicePacks.Pack): Boolean = false

    /**
     * v468 — [onFail] exists so this twin's signature stays the full edition's
     * (see its own note on why a pack that answers nothing is a failure and not a
     * finished sentence). Nothing here can fail, so nothing here ever calls it.
     */
    fun say(
        text: String,
        speed: Float,
        speakerId: Int,
        onDone: () -> Unit,
        onFail: () -> Unit
    ) = onDone()

    fun stop() = Unit

    fun release() = Unit
}

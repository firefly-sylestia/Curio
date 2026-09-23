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

    fun speakerCount(): Int = 1

    fun prepare(context: Context, pack: NeuralVoicePacks.Pack): Boolean = false

    fun say(text: String, speed: Float, speakerId: Int, onDone: () -> Unit) = onDone()

    fun stop() = Unit

    fun release() = Unit
}

package com.curio.app.features.personal

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

/**
 * v440 — THE READER, READING ALOUD.
 *
 * The member, from the settings list: *"Read-aloud: a speed and voice picker"*,
 * and asked what it should read: **"The visible page, then follow on."** Android's
 * own text-to-speech engine is the door for that — no dependency, no key, and the
 * voices the phone already has (including any the member installed), so nothing
 * here downloads a model or asks for a new kind of permission.
 *
 * The rules this file exists to keep:
 *
 *  · **NEVER BLOCK, NEVER CRASH ON A PHONE WITH NO SPEECH ENGINE.** The engine is
 *    created lazily, is allowed to fail ([isReady]), and every entry point returns
 *    quietly when it is not there — a book that cannot be read aloud is an
 *    inconvenience, and a reader that dies on open is a bug.
 *  · **ONE UTTERANCE AT A TIME, AND THE NEXT ONE IS THE CALLER'S DECISION.** This
 *    wrapper says a piece of text and reports when it is done; what comes next
 *    (the next blocks of the chapter, the next page of the PDF) belongs to the
 *    reader, which is the only thing that knows where the member is.
 *  · **NOTHING IS SAID BEFORE THE ENGINE IS READY.** A request that arrives while
 *    the engine is still initializing is REMEMBERED and said the moment it answers
 *    — the alternative is a play button that appears to do nothing on the first tap.
 *  · **IT IS RELEASED, ALWAYS** ([release]). An engine outliving the reader is a
 *    service holding audio focus for a screen that is gone.
 */
internal object ReaderSpeaker {

    private var engine: TextToSpeech? = null
    private var ready = false

    /** A request that arrived before the engine answered (see the rule above). */
    private var waiting: (() -> Unit)? = null

    /** The main thread, because the engine's callbacks arrive on a binder thread. */
    private val main = Handler(Looper.getMainLooper())

    /** Bumped per utterance so an old callback can never be taken for the new one. */
    private var utterance = 0

    /** Whether the phone can speak at all. False until the engine answers. */
    val isReady: Boolean get() = ready

    /**
     * Brings the engine up, once per process.
     *
     * Called when the reader opens, never from a screen that merely MENTIONS
     * read-aloud: a text-to-speech engine is a service binding, and holding one for
     * a book nobody is listening to is exactly the kind of background cost the
     * power pass (§7.6) was about.
     */
    fun prepare(context: Context) {
        if (engine != null) return
        val app = context.applicationContext
        engine = runCatching {
            TextToSpeech(app) { status ->
                ready = status == TextToSpeech.SUCCESS
                val pending = waiting
                waiting = null
                if (ready && pending != null) main.post { pending() }
            }
        }.getOrNull()
    }

    /**
     * Says [text], and calls [onDone] on the MAIN thread when it has finished.
     *
     * `QUEUE_FLUSH`, always: a member who taps play again wants the new position
     * read, not the old one finished first (see the reader's own driver).
     */
    fun say(text: String, speed: Float, voiceName: String, onDone: () -> Unit) {
        val tts = engine ?: return
        if (!ready) {
            // One pending request, replaced by a newer one — the latest tap wins.
            waiting = { say(text, speed, voiceName, onDone) }
            return
        }
        if (text.isBlank()) return
        runCatching {
            tts.setSpeechRate(speed.coerceIn(0.5f, 2.5f))
            if (voiceName.isNotBlank()) {
                tts.voices?.firstOrNull { voice -> voice.name == voiceName }
                    ?.let { voice -> tts.voice = voice }
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit

                override fun onDone(id: String?) {
                    main.post(onDone)
                }

                @Deprecated("Superseded by onError(id, errorCode), which the engine calls.")
                override fun onError(id: String?) {
                    main.post(onDone)
                }

                override fun onError(id: String?, errorCode: Int) {
                    main.post(onDone)
                }
            })
            utterance += 1
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "curio-read-$utterance")
        }
    }

    /** Stops the voice without tearing the engine down (the pause state). */
    fun stop() {
        waiting = null
        runCatching { engine?.stop() }
    }

    /** The engine's voices, for the picker — name to a label a member can read. */
    fun voices(): List<Pair<String, String>> {
        val all = runCatching { engine?.voices }.getOrNull() ?: return emptyList()
        return all
            .map { voice ->
                val locale = voice.locale
                val name = locale.displayName.ifBlank { locale.language }
                // Engine voice ids look like `en-us-x-sfg#female_1-local` — the part
                // after the hash is the one that tells two voices OF THE SAME language
                // apart, which is the only thing a picker needs to say.
                val flavour = voice.name.substringAfter('#', "").substringBefore('-')
                voice.name to if (flavour.isNotBlank()) "$name \u00b7 $flavour" else name
            }
            .distinctBy { it.second }
            .sortedBy { it.second }
    }

    /** The way out: stop, release, and forget — the reader calls this as it closes. */
    fun release() {
        waiting = null
        ready = false
        runCatching {
            engine?.stop()
            engine?.shutdown()
        }
        engine = null
    }
}

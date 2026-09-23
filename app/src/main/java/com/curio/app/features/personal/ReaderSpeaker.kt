package com.curio.app.features.personal

import android.content.Context
import android.content.Intent
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

    /**
     * v464 — THE ENGINE THIS VOICE IS BOUND TO, as its package name ("" = the phone's
     * own default).
     *
     * Kept beside [engine] because a CHANGE of engine is a change of ENGINE: the old one
     * has to be stopped and shut down before the new one is built, or two speech services
     * hold the audio focus at once. Comparing this against the asked-for package is also
     * what makes [prepare] idempotent — it is called on every play, every skip and every
     * settings row, and must not rebuild the engine each time.
     */
    private var bound = ""

    /**
     * v464 — THE CALLERS WANTING TO KNOW WHEN THE ENGINE ANSWERS.
     *
     * Binding an engine is asynchronous, so a settings row that read [voices] the line
     * after [prepare] was ALWAYS reading an engine that had not answered yet — which is
     * why the voice list showed "No voices are installed on this phone yet" the first
     * time it was opened, and only filled on the second. These run on the main thread
     * once the engine is ready, and are dropped when it is not (an engine that failed has
     * no voices to offer, and a row waiting for one would spin for ever).
     */
    private val waitingForVoices = ArrayList<() -> Unit>()

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
    fun prepare(context: Context, enginePackage: String = "", onReady: (() -> Unit)? = null) {
        // THE VOICE LIST ASKED TO BE TOLD, and it is answered either way: from here when
        // the engine is already up, or from the binding's own callback when it is not.
        if (onReady != null) {
            if (engine != null && bound == enginePackage && ready) {
                main.post(onReady)
                return
            }
            waitingForVoices.add(onReady)
        }
        if (engine != null && bound == enginePackage) return
        val app = context.applicationContext
        // A DIFFERENT ENGINE MEANS THE OLD ONE GOES FIRST (see [bound]).
        if (engine != null) release()
        bound = enginePackage
        engine = runCatching {
            val listener = TextToSpeech.OnInitListener { status ->
                ready = status == TextToSpeech.SUCCESS
                val pending = waiting
                waiting = null
                val waiters = waitingForVoices.toList()
                waitingForVoices.clear()
                if (ready) {
                    main.post {
                        pending?.invoke()
                        waiters.forEach { it() }
                    }
                }
            }
            // THE PACKAGE IS THE WHOLE OF THE FEATURE (see [engines]): the platform's
            // own constructor takes it, so pointing the reader at a better engine is
            // one argument rather than a second speech stack.
            if (enginePackage.isBlank()) {
                TextToSpeech(app, listener)
            } else {
                TextToSpeech(app, listener, enginePackage)
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

    /**
     * v464 — THE ENGINES THE PHONE HAS, for the picker: package name to a label.
     *
     * Asked of the PLATFORM rather than the engine's own list. Android lets any app
     * provide speech by answering `android.intent.action.TTS_SERVICE`, which is what the
     * platform's `TextToSpeech` constructor reads when it is given a package — so the
     * set of engines that can be pointed at is exactly the set of services answering that
     * intent. Querying the intent asks the same question AOSP's own TtsEngines asks, and
     * it needs no engine bound and no process-wide state: a member who only ever opens
     * the picker must not have to boot a speech engine to be told what speech engines
     * exist, which is the cost every rule in this file is written against.
     *
     * The FIRST entry is always the phone's default, spelled as the empty package, because
     * "" is what the constructor means by "whatever the phone reads with" — so a member
     * who never opens this row reads in the voice they always had.
     */
    fun engines(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val found = runCatching {
            pm.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
        }.getOrNull().orEmpty()
        val installed = found
            .mapNotNull { it.serviceInfo?.packageName }
            .distinct()
            .map { name ->
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(name, 0)).toString()
                }.getOrNull().orEmpty().ifBlank { name }
                name to label
            }
            .sortedBy { it.second }
        return listOf("" to "The phone's own") + installed
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
        waitingForVoices.clear()
        ready = false
        bound = ""
        runCatching {
            engine?.stop()
            engine?.shutdown()
        }
        engine = null
    }
}

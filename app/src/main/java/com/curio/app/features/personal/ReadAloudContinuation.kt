package com.curio.app.features.personal

import android.content.Context
import com.curio.app.data.NeuralSpeaker
import com.curio.app.infrastructure.ReadAloudService
import com.curio.app.infrastructure.ReadAloudSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * v465i — ONE NUMBER, BOTH DRIVERS: THE GRACE THE END OF A SENTENCE IS GIVEN.
 *
 * An engine's `onDone` can land while its last words are still sounding, and the
 * next utterance's `QUEUE_FLUSH` then truncates them. Every sentence ends at a
 * comma or a full stop, so the words that went missing were always the ones just
 * BEFORE the punctuation (member: *"i was skipping comma and full stop words like
 * the words which were before those 2 were getting skipped"*). A short pause
 * before the cursor moves lets the sentence finish in its own time.
 *
 * The same constant serves the page's driver and [ReadAloudContinuation]: one
 * reading, one grace, wherever it is being driven from.
 */
internal const val ALOUD_TAIL_GRACE_MS = 180L

/**
 * ── v469 — THE SENTENCE'S OWN ENDING, NOT ONE GRACE FOR EVERY VOICE ────────
 *
 * The member: *"the behavior of piper and edge read aloud is bad as they stop way
 * too long on full stops like maybe for 2 sec or something fix it and make it
 * natural"*. Half of that wait was the reader's own: [ALOUD_TAIL_GRACE_MS] was
 * added for the PHONE's engine, whose `onDone` can land while its last words are
 * still sounding (the next utterance's `QUEUE_FLUSH` is what used to truncate
 * them), and every other voice was paying it as well — on top of the silence the
 * model had already drawn at the end of its own clip.
 *
 * So the grace follows the voice:
 *
 *  · **A downloaded pack adds nothing.** Its clip was TRIMMED to a reader's own
 *    breath at the full stop before it was ever played (see
 *    `NeuralSpeaker.trimmed`), and its playback lane drains the audio the device
 *    has not played yet before it reports — so its ending is already exact, and a
 *    grace on top of it is a pause nobody wrote.
 *  · **Edge keeps half.** `MediaPlayer` reports the end of the FILE, which can
 *    arrive with a bufferful of the tail still to sound — so a guard is real —
 *    but the endpoint's own clip already ends with a short silence, which is the
 *    rest of the pause.
 *  · **The phone's own engine keeps all of it**, because truncating the words
 *    before a comma is the bug this constant was written for.
 */
internal fun aloudTailGraceMs(): Long = when (ReaderLook.speakEngine) {
    ReaderEngine.NEURAL -> 0L
    ReaderEngine.EDGE -> ALOUD_TAIL_GRACE_MS / 2
    // The phone's own engine is the one this constant was written for, and it is
    // also the one the slider can honestly change: `QUEUE_FLUSH` truncates the last
    // words unless the reader waits, so the wait is a real number, not a silence
    // baked into a file (v471).
    else -> (ALOUD_TAIL_GRACE_MS * (0.6f + aloudBreak() * 1.2f)).toLong()
}

/**
 * ── v471 — HOW LONG A FULL STOP IS HELD, AND WHY ONE NUMBER COVERS THREE VOICES ──
 *
 * The member: *"the full stop break … is still very long like very long, not natural
 * at all. for edge tts and kokoro, not the lessac. also for piper lessac its a little
 * fast in full stop incrase it by just a little. and also add full stop break
 * customisation"*.
 *
 * **Three voices, three mechanisms, one breath.** A downloaded pack's pause is
 * silence INSIDE its own clip (trimmed before it is played), the online voice's is
 * silence at the end of the file it downloads (trimmed the same way now), and the
 * phone's own engine's is the wait before the reader flushes an utterance — there is
 * no file to trim there at all. So the setting is a POSITION (see
 * `ReaderLook.speakStop`) and this is the one place a position becomes milliseconds,
 * which is what keeps the row honest whatever voice is reading.
 *
 * **THE DEFAULT IS THE TUNING, AND IT IS THE TUNING THE MEMBER ASKED FOR:**
 * `0.5` gives a pack a **~360 ms** breath — a little longer than v469's 260 ms, which
 * is the *"a little fast … increase it by just a little"* on Piper · Lessac — and,
 * because that is now the tail EVERY pack gets, Kokoro's much longer tail comes down
 * with it (*"very long … for edge tts and kokoro"*). The phone's own grace is left
 * exactly where it was at the default and only follows the slider from there.
 */
internal fun aloudBreak(): Float = ReaderLook.speakStop.coerceIn(0f, 1f)

/**
 * The trailing silence a CLIP is trimmed to, in milliseconds — the pack's own tail
 * and the online voice's alike (see `NeuralSpeaker.trimmed` and `EdgeVoice`).
 *
 * 80 ms at the clipped end, 640 ms at the long one, **360 ms at the default**.
 */
internal fun aloudBreakMs(): Float = 80f + aloudBreak() * 560f

/**
 * v465i — HOW LONG ONE SENTENCE MAY GO UNREPORTED BEFORE THE READING GIVES UP.
 *
 * An engine can accept a sentence and say nothing about it — no `onDone`, no
 * error — and both drivers wait on exactly that callback. Without a bound, the
 * page kept a mark lit and the notification kept promising a reading that had
 * stopped (the member's *"the 2nd one wasnt playing"*). Two minutes is far longer
 * than any sentence on this phone can take to read, and far shorter than forever.
 */
internal const val ALOUD_STALL_MS = 120_000L

/**
 * ── v465i — THE READING THAT OUTLIVES ITS PAGE ─────────────────────────────
 *
 * The member: *"exiting cancels it"*. They are describing the shape of the old
 * design, and they are right about it. Read aloud was driven from the reader's own
 * composition (a `LaunchedEffect` in `BookReaderScreen`, one sentence at a time),
 * so the moment the reader was popped off the back stack the driver was disposed
 * with it and the voice stopped mid-book — even though the member had asked, in
 * Settings, for reading to keep going when Curio is not on screen.
 *
 * [ReadAloudSession] and [ReadAloudService] already kept the PROCESS alive; what
 * was missing was something for the service to keep alive. That is this object:
 * when the page is left with a session live, the reader hands over the book, the
 * sentence it had reached and a way to ask for the text of any other one, and this
 * simple loop carries the reading on. The notification's three shade controls are
 * re-registered to point here (they were the reader's lambdas, and those died with
 * its composition), so pause, skip and stop still work from the shade.
 *
 * **ONE DRIVER AT A TIME, AND THE PAGE ALWAYS WINS.** The reader takes the reading
 * back — at the sentence the voice has reached — the moment it is opened again for
 * the same book ([takeOver]), and a reader that is closed with its own voice off
 * ends this loop ([end]), so two loops can never speak over each other. That is the
 * whole of the contract; nothing here decides what is read beyond "the next one".
 *
 * **IT READS PLAIN TEXT AND NOTHING ELSE.** Without the page there is no highlight
 * to keep in step and no scroll to follow, so all it needs is the words — asked for
 * one index at a time through [handOff]'s own provider, which is what lets a PDF
 * keep being read too (its page text is parsed on demand, exactly as the reader
 * would have done it). The provider closes over the document; the sentences of a
 * text book are held by the reader and kept alive by this loop for as long as the
 * reading lasts.
 *
 * **A STALL ENDS THE SESSION RATHER THAN RACING IT.** If an engine refuses a
 * sentence and never reports back, the loop gives up after two minutes and ends the
 * session: a notification that promises to be reading while nothing is playing is
 * worse than a reading that stops where it stood.
 */
internal object ReadAloudContinuation {

    /** What a page that is opening again needs to carry on: where, and whether. */
    data class Taken(val index: Int, val playing: Boolean)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    /** True while a handover is live — so a page can ask whether it owns the voice. */
    private var live = false
    private var book = ""
    private var index = 0
    private var count = 0
    private var speaking = false
    private var app: Context? = null
    private var textAt: (suspend (Int) -> String?)? = null

    /**
     * Takes the reading over from the book page that is being disposed.
     *
     * [from] is the sentence the page had reached; [playing] is false when the
     * member had paused, in which case the session is kept (and the notification
     * keeps saying "Paused") but nothing is read until they press carry on.
     */
    fun handOff(
        context: Context,
        bookId: String,
        title: String,
        count: Int,
        from: Int,
        playing: Boolean,
        textAt: suspend (Int) -> String?
    ) {
        stopLoop()
        val app = context.applicationContext
        this.app = app
        book = bookId
        this.count = count.coerceAtLeast(0)
        this.textAt = textAt
        index = from.coerceIn(0, (this.count - 1).coerceAtLeast(0))
        speaking = playing && this.count > 0
        live = true
        ReadAloudSession.active = true
        ReadAloudSession.playing = speaking
        ReadAloudSession.title = title
        ReadAloudSession.onToggle = { toggle() }
        ReadAloudSession.onPrev = { step(-1) }
        ReadAloudSession.onNext = { step(1) }
        ReadAloudSession.onStop = { end() }
        ReadAloudService.sync(app)
        if (speaking) startLoop()
    }

    /**
     * Gives the reading back to the page that is opening for [bookId], or null when
     * this object is not reading that book (nothing to take back).
     *
     * The loop is stopped and the provider dropped, but the SESSION is left standing:
     * the page's own driver picks it up in the very next frame, and clearing here
     * would blink the notification out between two screens.
     */
    fun takeOver(bookId: String): Taken? {
        if (!live || book != bookId) return null
        val taken = Taken(index, speaking)
        stopLoop()
        textAt = null
        return taken
    }

    /** Ends the reading — the shade's Stop, the end of the book, or a new page. */
    fun end() {
        val ctx = app
        live = false
        speaking = false
        stopLoop()
        textAt = null
        book = ""
        index = 0
        count = 0
        this.app = null
        ReaderSpeaker.stop()
        NeuralSpeaker.stop()
        EdgeVoice.stop()
        ReaderSpeaker.release()
        NeuralSpeaker.release()
        ReadAloudSession.clear()
        if (ctx != null) ReadAloudService.stop(ctx)
    }

    // ── THE CONTROLS, WHICH ARE THE NOTIFICATION'S OWN BUTTONS ───────────

    private fun toggle() {
        if (!live) return
        if (speaking) {
            speaking = false
            ReaderSpeaker.stop()
            NeuralSpeaker.stop()
            EdgeVoice.stop()
        } else {
            speaking = true
            startLoop()
        }
        ReadAloudSession.playing = speaking
        app?.let { ReadAloudService.sync(it) }
    }

    private fun step(step: Int) {
        if (!live || count <= 0) return
        index = (index + step).coerceIn(0, count - 1)
        // The sentence being abandoned must not finish first — the same rule the
        // reader's own skip follows.
        ReaderSpeaker.stop()
        NeuralSpeaker.stop()
        EdgeVoice.stop()
        if (speaking) startLoop()
    }

    // ── THE LOOP ─────────────────────────────────────────────────────────

    private fun stopLoop() {
        val running = job
        job = null
        running?.cancel()
    }

    private fun startLoop() {
        stopLoop()
        val ctx = app ?: return
        job = scope.launch {
            while (live && speaking && index in 0 until count) {
                val provider = textAt
                val text = provider?.invoke(index)?.trim().orEmpty()
                if (text.isBlank()) {
                    // A page of plates has nothing to say: stepped over, never
                    // stuck on (the reader's own rule for a PDF).
                    if (index + 1 >= count) {
                        end()
                        return@launch
                    }
                    index += 1
                    continue
                }
                val done = CompletableDeferred<Unit>()
                // ── v468 — THE SAME HEAD START THE PAGE GIVES THE ONLINE VOICE ──
                //
                // The next sentence is fetched while this one plays (see `sayAloud`'s
                // `nextText`), which is the whole difference between a reading that
                // pauses at every full stop and one that does not.
                //
                // ── v469 — AND IT IS THE SAME HEAD START A PACK NEEDS ───────
                //
                // A neural pack does not fetch, it SYNTHESISES: a phone makes a
                // sentence slower than it speaks it, so a pack that is only asked for
                // the next sentence once this one has finished reads every full stop
                // as a two-second gap (the member's own number). The pack is handed
                // the same next text and makes it under this sentence's audio (see
                // `NeuralSpeaker.prefetch`).
                //
                // ⚠️ **THE PROVIDER IS ASKED OFF THE MAIN THREAD.** This loop runs on
                // the main dispatcher, and for a PDF the provider is a text
                // extraction of the file — the reader's own driver pays that on the IO
                // dispatcher, so an extra ask here does too.
                val next = if (ReaderLook.speakEngine == ReaderEngine.EDGE ||
                    ReaderLook.speakEngine == ReaderEngine.NEURAL
                ) {
                    withContext(Dispatchers.IO) {
                        provider?.invoke(index + 1)?.trim().orEmpty().ifBlank { null }
                    }
                } else {
                    null
                }
                sayAloud(ctx, text, ReaderLook.speakSpeed, nextText = next) { done.complete(Unit) }
                if (withTimeoutOrNull(ALOUD_STALL_MS) { done.await() } == null) {
                    end()
                    return@launch
                }
                delay(aloudTailGraceMs())
                if (!live || !speaking) return@launch
                if (index + 1 >= count) {
                    end()
                    return@launch
                }
                index += 1
            }
        }
    }

}

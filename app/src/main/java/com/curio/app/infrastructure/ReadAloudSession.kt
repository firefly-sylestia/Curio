package com.curio.app.infrastructure

/**
 * ── v465h — THE READING VOICE'S OWN SESSION, AND WHY IT IS ONLY A BRIDGE ──
 *
 * Read aloud has always been driven from the READER — a `LaunchedEffect` in
 * `BookReaderScreen` walks the sentences and calls the engine one at a time (see
 * `sayAloud`). That is the right owner: the sentence list comes from the document
 * the reader has open, and the cursor is what draws the read-along wash.
 *
 * The problem is that a composition cannot keep a process alive. Once Curio goes
 * to the background the phone is free to FREEZE the app's threads — the driver
 * stalls mid-sentence and the voice simply stops, with no error anywhere — and it
 * may also kill the process outright. So the driver stays exactly where it is and
 * this object carries the little the SERVICE needs to keep it running:
 *
 *  - **what the notification says** ([active], [playing], [title]) — written by
 *    the reader, read by the service when it renders, and
 *  - **the member's four controls** ([onToggle], [onPrev], [onNext], [onStop]) —
 *    the same lambdas the reader's own bar uses.
 *
 * **v465i — AND WHEN THE PAGE IS GONE, THE DRIVER IS NOT LEFT WITH NOTHING.**
 * The paragraph above describes surviving the app going to the BACKGROUND, which
 * the foreground service does. It does not survive the book page being POPPED off
 * the back stack, because that disposes the very composition the driver lives in —
 * which was the member's own report (*"exiting cancels it"*). So a live session is
 * now handed over on the way out:
 * `com.curio.app.features.personal.ReadAloudContinuation` carries the book, the
 * sentence it had reached and a way to ask for the text of the next one, and
 * re-registers the four controls below to point at itself. The page takes the
 * reading back, at the sentence the voice has reached, when it is opened again.
 *
 * ⚠️ ONLY [active] DECIDES WHETHER THE SERVICE LIVES. The controls are written on
 * every recomposition and [active] in the one effect that also wakes the service,
 * so the two are never out of step; a service that asked whether its buttons had
 * arrived yet would be a service that could stand itself down in the frame before
 * they did — and a notification that failed to appear is silent, so it would look
 * like the feature simply not working.
 *
 * **IT IS NOT A SECOND DRIVER, AND IT MUST NEVER BECOME ONE.** Nothing here
 * decides what sentence is next or which voice reads it; the reader owns all of
 * that — and where the page no longer exists, [ReadAloudSession.clear]'s v465i
 * neighbour (`ReadAloudContinuation`) takes that ownership over, one driver at a
 * time, never two. A service that could advance the reading on its own would be a
 * second source of truth for the cursor, and the read-along wash would start
 * disagreeing with the voice — the one thing the sentence-at-a-time design exists
 * to prevent.
 *
 * **THE CONTROLS ARE LAMBDAS, NOT INTENTS, BECAUSE THE SERVICE IS IN THIS
 * PROCESS.** The notification's actions arrive as intents on the service (a
 * `PendingIntent` cannot call a lambda), and the service then invokes one of
 * these. Same process, so the reader's own state is reachable and the member's
 * tap on the shade button takes effect on the very sentence they are hearing.
 * [active] is what tells the service there is anything to act on: when the reader
 * closes the book, the battery and the notification both end there.
 */
internal object ReadAloudSession {

    /**
     * Whether a voice belongs to a book right now — the reader's `voiceOn`.
     *
     * `@Volatile` because the two sides are on different threads: the reader
     * writes from the main thread, the service reads from its own.
     */
    @Volatile
    var active: Boolean = false

    /** Whether it is actually speaking — the reader's `voiceOn && !voicePaused`. */
    @Volatile
    var playing: Boolean = false

    /** The book's name, for the notification's own line. */
    @Volatile
    var title: String = ""

    /**
     * v465i — WHETHER THE EDGE EXPERIMENT HAS GIVEN UP FOR THIS READING.
     *
     * The endpoint is undocumented and unsanctioned (see [com.curio.app.features
     * .personal.EdgeVoice]): a rotated GEC version, a 403 or a dead network is a
     * normal Tuesday for it, not an exceptional case. When the first sentence
     * fails, the member's own report was a page of silent fast-forwarding — so
     * the failure is remembered HERE, for the rest of the session, and every
     * later sentence reads in the phone's own voice instead of opening a socket
     * that has already refused us once. Cleared when a session ends, and when the
     * member picks the voice again (a fresh choice deserves a fresh attempt).
     */
    @Volatile
    var edgeUnavailable: Boolean = false

    /**
     * ── v468 — THE VOICE PACK THAT ANSWERED NOTHING, BY PACK ID ────────────
     *
     * The same remedy as [edgeUnavailable], for the downloaded packs. A pack is a
     * 300 MB model an offline runtime either loads or does not, and until v468 a pack
     * that answered nothing was indistinguishable from a sentence being read: the
     * reader counted it as spoken and moved on in silence (the member: *"kokoro doesnt
     * work at all, like totally it doesnt work"*).
     *
     * So a pack that fails says so ONCE, is remembered for the rest of this reading,
     * and every later sentence is read in the phone's own voice instead of paying a
     * model load to hear nothing again. **The ID is the value, not a flag**, because a
     * member may have both packs: `kokoro-en` failing must not disable
     * `piper-lessac-medium`, which is the pack the app recommends first and the one
     * known to work. Choosing a different pack clears it by itself (`sayAloud` compares
     * this against the id it was asked for), and so does stopping the reading.
     */
    @Volatile
    var packUnavailable: String? = null

    /**
     * The member's controls, registered by the reader on every recomposition.
     *
     * **RE-REGISTERED RATHER THAN CAPTURED ONCE, ON PURPOSE.** The reader's toggle
     * is not a constant: it closes over the composition's own state, and the
     * start-of-reading path reads the page the member is LOOKING AT. A lambda
     * captured at the moment the session began would keep starting from wherever
     * they were then.
     */
    @Volatile
    var onToggle: (() -> Unit)? = null

    @Volatile
    var onPrev: (() -> Unit)? = null

    @Volatile
    var onNext: (() -> Unit)? = null

    /**
     * Ends the session — the reader's `voiceOn = false` plus the engine teardown,
     * so a "Stop" from the shade and a stop from the page are the same event.
     */
    @Volatile
    var onStop: (() -> Unit)? = null

    fun toggle() {
        onToggle?.invoke()
    }

    fun prev() {
        onPrev?.invoke()
    }

    fun next() {
        onNext?.invoke()
    }

    fun stop() {
        onStop?.invoke()
    }

    /**
     * The reader is gone. The lambdas go with it — a control that outlived its
     * composition would be a notification button wired to a screen that no longer
     * exists, which is worse than a dead button: it would look alive.
     */
    fun clear() {
        active = false
        playing = false
        title = ""
        edgeUnavailable = false
        packUnavailable = null
        onToggle = null
        onPrev = null
        onNext = null
        onStop = null
    }
}

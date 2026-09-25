package com.curio.app.infrastructure

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.curio.app.R
import com.curio.app.data.AppPreferences

/**
 * ── v465h — THE NOTIFICATION THAT KEEPS THE READING VOICE ALIVE ───────────
 *
 * Read aloud is driven by the reader's own composition (see
 * [ReadAloudSession] for why it stays there), and a cached app is FREEZABLE: the
 * moment Curio leaves the foreground the phone may stop the app's threads, the
 * sentence in flight never reports done, and the voice goes quiet mid-book with
 * nothing in the app to explain it. A foreground service is the one thing that
 * says "this process is doing work the member asked for", so the reading survives
 * the member switching apps or turning the screen off.
 *
 * **IT OWNS NO READING.** Every decision — which sentence, which voice, which
 * page — belongs to the reader. This service answers three questions and nothing
 * else: *is a session live* ([ReadAloudSession.active]), *is it speaking*, and
 * *what is the book called*. Its three shade buttons call straight back into the
 * reader's own lambdas, so a tap in the shade and a tap on the page are one code
 * path.
 *
 * **THE FLAVOUR IS `specialUse`, AND THAT IS DELIBERATE.** Android 14's
 * `mediaPlayback` type would want `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and a real
 * `MediaSession` — a second, competing owner of the transport, and a permission
 * this manifest does not carry. `specialUse` is already declared for the explore
 * timer and the pet overlay, so this adds a subtype and no permission at all.
 * The cost, recorded honestly: the shade controls are notification buttons rather
 * than media-session controls, so there is no system media widget and no
 * "previous app" resume button on the lock screen.
 *
 * **THE `POST_NOTIFICATIONS` PERMISSION IS NEVER ASSUMED.** A denied notification
 * permission must leave the reading exactly as it was rather than take the app
 * down, so the promotion is wrapped and a failure is logged (the same rule
 * [ExploreSessionService.start] follows).
 */
class ReadAloudService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ── THE THREE SHADE BUTTONS ───────────────────────────────────────
        // Each one is delivered here as an intent (a `PendingIntent` cannot call
        // a lambda) and is delegated straight back to the reader. Nothing is
        // decided here, which is what keeps one cursor in the app (see
        // [ReadAloudSession]'s note).
        when (intent?.action) {
            ACTION_TOGGLE -> ReadAloudSession.toggle()
            ACTION_NEXT -> ReadAloudSession.next()
            ACTION_STOP -> {
                // ── v473 — A STOP TAKES THE KEEP-ALIVE DOWN HERE, NOT IN THE READER ──
                //
                // The member: *"the curio is now staying in active apps in
                // background even though nothing is being played or active
                // notifications"*. The reading is driven by the READER, and the
                // reader's own teardown (which used to be the only thing that stood
                // this service down) runs from a `LaunchedEffect` — i.e. from a
                // RECOMPOSITION, which a backgrounded app may not be doing at all.
                // So a Stop tapped in the shade while Curio was off screen could set
                // the page's state and leave the foreground service — and the
                // process it pins — running for a reading that had ended: swiping the
                // notification away then left the app listed as active with nothing
                // playing and nothing in the shade.
                //
                // The member's own stop still runs first ([ReadAloudSession.stop]),
                // so the page and this service agree about the state either way; and
                // then the notification and the foreground state go with it, from
                // here, where the work is already done.
                ReadAloudSession.stop()
                ReadAloudSession.clear()
                return standDown()
            }
        }
        return render()
    }

    /**
     * ── v473 — A NOTIFICATION MUST NOT OUTLIVE ITS SERVICE ────────────────
     *
     * A notification detached with `STOP_FOREGROUND_DETACH` — which is how a
     * paused reading keeps its controls without holding the foreground state
     * ([hold]) — is *by design* left posted when the service it belongs to is
     * destroyed, because the whole point of detaching is that the notification is
     * the member's, not the service's. That is right for a read-aloud service that
     * is still alive behind it and wrong for one that is gone: a *Paused · <book>*
     * notification whose Carry on button belongs to a dead process is exactly the
     * notification that outlived the app the member reported in v470. Every way out
     * of this service — the reader's own `stopService`, a stop from the shade, the
     * OS reclaiming it — passes through here, so this is the one place that has to
     * say it.
     */
    override fun onDestroy() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    /**
     * ── v470 — CLOSING THE APP CLOSES THE READING ──────────────────────────
     *
     * The member: *"the reader aloud notification stays even after closing the app
     * and there is no stop option which should stop and clear the notification"*.
     * A foreground service is not killed when its task is swept out of Recents, so
     * the notification outlived the app with nothing of the member's left driving
     * it. Swiping Curio away IS the member saying they are done, so the reading
     * ends here: the session's own stop runs first — the reader's, or
     * [com.curio.app.features.personal.ReadAloudContinuation]'s when the book page
     * is already gone, one teardown for both — and then the foreground state and
     * the notification go with it.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        ReadAloudSession.stop()
        ReadAloudSession.clear()
        // The same three calls a stop from the shade makes (v473): foreground state,
        // notification, self. One teardown, whichever door the member used.
        standDown()
        super.onTaskRemoved(rootIntent)
    }

    /**
     * Re-reads the session and stops quietly when there is nothing left to keep
     * alive — the shape [ExploreSessionService.render] uses, and for the same
     * reason: the service is a consequence of the session, never its owner.
     *
     * **THE SETTING IS CHECKED HERE AND NOT ONLY WHEN THE SESSION STARTS.** The
     * member can switch background listening off while a book is being read, and
     * the notification is the promise that Curio is still reading — an off switch
     * that left it up would be a switch that changed nothing visible.
     */
    private fun render(): Int {
        if (!ReadAloudSession.active) return standDown()
        if (!AppPreferences.isReadAloudBackgroundEnabled(this)) return standDown()
        return hold()
    }

    /**
     * ── v473 — THE FOREGROUND STATE IS OWED TO WHAT IS PLAYING ────────────
     *
     * A foreground service is a claim that *this process is doing something the
     * member asked for*, and only one of the two live states here is that. WHILE
     * THE VOICE IS SPEAKING the claim is true and the service is what stops the
     * phone freezing the reader mid-sentence (the whole reason this service
     * exists). WHILE IT IS PAUSED nothing is being read, nothing can be lost by
     * freezing the process, and the claim is false — but the app went on making it,
     * so a paused reading sat in the phone's own "active apps" list for as long as
     * the member left it there (their *"nothing is being played or active
     * notifications"*).
     *
     * So the paused state keeps the NOTIFICATION and gives back the FOREGROUND
     * STATE:
     *
     *  · **Detach, never remove.** `STOP_FOREGROUND_DETACH` is what keeps Carry on,
     *    Next sentence and Stop on the lock screen for a paused reading — the
     *    controls the member asked for in v465h — while the app stops being listed
     *    as active. [onDestroy] is what stops such a notification outliving the
     *    service (it is never removed by the system once detached).
     *  · **Promote first, unconditionally.** A service started through
     *    `startForegroundService` owes the system this call within five seconds of
     *    every start, and the promotion is also what POSTS the notification — so it
     *    is made first and the detach follows. Resuming re-promotes through the same
     *    path, which is allowed exactly where a resume can come from: a visible app
     *    or a tap on this notification (the system allowlists an app the member has
     *    just interacted with).
     */
    private fun hold(): Int {
        val notification = buildNotification()
        val speaking = ReadAloudSession.playing
        // A start failure (a denied notification permission, a revoking system)
        // must not take the reading down with it.
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            if (!speaking) stopForeground(STOP_FOREGROUND_DETACH)
            START_NOT_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Could not keep the read-aloud session alive", e)
            standDown()
        }
    }

    /**
     * Nothing here has a reason to exist — take the notification and the foreground
     * state down together and stop. The explicit `cancel` matters: a notification
     * that was DETACHED (a paused reading) is not removed by the system when the
     * service goes, so `stopSelf()` alone would leave it in the shade (see
     * [onDestroy], which makes the same call for every other way out).
     */
    private fun standDown(): Int {
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        stopSelf()
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val playing = ReadAloudSession.playing
        val title = ReadAloudSession.title.ifBlank { "your book" }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (playing) "Reading $title" else "Paused · $title")
            .setContentText("Curio is reading this aloud")
            .setContentIntent(openAppIntent())
            // ── v473 — ONGOING WHILE IT SPEAKS, SWIPEABLE WHILE IT DOES NOT ───
            //
            // Ongoing is right for a reading that is playing: the notification is
            // the member's control surface AND the promise that the process behind it
            // is alive, so a stray swipe must not be able to leave an invisible
            // foreground service. It is wrong for a PAUSED reading, which holds no
            // foreground state at all ([hold]) — a non-dismissible notification with
            // nothing playing is the one kind the member cannot clear, and if the
            // process is reclaimed while it sits there, a tap on it is what cleans it
            // up ("Carry on" arrives at a service with no session and stands down).
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // VISIBILITY_PUBLIC is what puts the buttons on the lock screen: the
            // controls are this notification's own actions, so a member whose lock
            // screen is set to hide notification content still gets them.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // ── THREE ACTIONS, NOT FOUR, AND STOP IS ONE OF THEM ───────────
            // A standard notification renders at most three action buttons and
            // SILENTLY DROPS the rest. There were four here — "Previous
            // sentence" first, "Stop reading" last — so the last one was never
            // drawn anywhere, which is exactly why the member could find no
            // Stop in the shade. "Previous sentence" keeps its home on the
            // reader's own bar; the shade carries the three that matter.
            .addAction(0, if (playing) "Pause" else "Carry on", controlIntent(ACTION_TOGGLE, 1))
            .addAction(0, "Next sentence", controlIntent(ACTION_NEXT, 2))
            .addAction(0, "Stop reading", controlIntent(ACTION_STOP, 3))
            .build()
    }

    /**
     * A control button's intent, aimed at THIS service.
     *
     * `getService` rather than a broadcast receiver: the service is already
     * running, so the intent is delivered to its own `onStartCommand` — which is
     * the only shape Android 12+ allows from a notification anyway (starting a
     * service from the background is refused). And where the service has given up
     * the foreground state (a paused reading, see [hold]) the tap IS the member
     * interacting with this notification, which is what allowlists the start it
     * asks the service to make in return. The request code is per action because a
     * `PendingIntent` is cached by its identity: four actions sharing one code
     * would all resolve to whichever was built first.
     */
    private fun controlIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, ReadAloudService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName)
                ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_IMMUTABLE
        )

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // LOW and silent: this notification's job is to exist where the member
        // looks for a control, never to make a sound or hold a wake lock of its
        // own. The reading is already audible; a notification that bleeped over it
        // would be the app interrupting the thing it was asked to do.
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Reading aloud",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps read aloud going when Curio is not on screen." }
        )
    }

    companion object {
        private const val TAG = "ReadAloudService"
        const val CHANNEL_ID = "read_aloud"
        const val NOTIFICATION_ID = 4212
        const val ACTION_TOGGLE = "com.curio.app.action.READ_ALOUD_TOGGLE"
        const val ACTION_NEXT = "com.curio.app.action.READ_ALOUD_NEXT"
        const val ACTION_STOP = "com.curio.app.action.READ_ALOUD_STOP"

        /**
         * Keeps a reading session alive, or re-renders its notification.
         *
         * Called on a state CHANGE and never per sentence: what the notification
         * says is the book and whether it is speaking, both of which change a
         * handful of times a session, so one call per sentence would be hundreds
         * of service starts to redraw a line that never moved.
         *
         * Never throws: a synchronous FGS start can be refused (background start
         * restrictions, a revoked permission), and the reading itself is driven
         * by the reader and must not be taken down by its own notification.
         */
        fun sync(context: Context) {
            if (!ReadAloudSession.active) return
            if (AppPreferences.isReadAloudBackgroundEnabled(context)) {
                try {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, ReadAloudService::class.java)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start the read-aloud service", e)
                }
            } else {
                // The member just switched it off. Ask the running service to
                // re-render, which now finds the setting off and stands down —
                // stopping rather than never having started, so the notification
                // that was promising to keep reading goes with it.
                context.stopService(Intent(context, ReadAloudService::class.java))
            }
        }

        /** Ends the session's keep-alive. The reading is the reader's to stop. */
        fun stop(context: Context) {
            context.stopService(Intent(context, ReadAloudService::class.java))
        }
    }
}

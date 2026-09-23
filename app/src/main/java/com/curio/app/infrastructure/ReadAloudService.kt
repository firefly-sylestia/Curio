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
 * *what is the book called*. Its four buttons call straight back into the
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
        // ── THE FOUR SHADE BUTTONS ────────────────────────────────────────
        // Each one is delivered here as an intent (a `PendingIntent` cannot call
        // a lambda) and is delegated straight back to the reader. Nothing is
        // decided here, which is what keeps one cursor in the app (see
        // [ReadAloudSession]'s note).
        when (intent?.action) {
            ACTION_TOGGLE -> ReadAloudSession.toggle()
            ACTION_PREV -> ReadAloudSession.prev()
            ACTION_NEXT -> ReadAloudSession.next()
            ACTION_STOP -> ReadAloudSession.stop()
        }
        return render()
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
        if (!ReadAloudSession.active) return stopQuietly()
        if (!AppPreferences.isReadAloudBackgroundEnabled(this)) return stopQuietly()
        return promote()
    }

    private fun promote(): Int {
        val notification = buildNotification()
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
            START_NOT_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Could not keep the read-aloud session alive", e)
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun stopQuietly(): Int {
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
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // VISIBILITY_PUBLIC is what puts the buttons on the lock screen: the
            // controls are this notification's own actions, so a member whose lock
            // screen is set to hide notification content still gets them.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Previous sentence", controlIntent(ACTION_PREV, 1))
            .addAction(0, if (playing) "Pause" else "Carry on", controlIntent(ACTION_TOGGLE, 2))
            .addAction(0, "Next sentence", controlIntent(ACTION_NEXT, 3))
            .addAction(0, "Stop reading", controlIntent(ACTION_STOP, 4))
            .build()
    }

    /**
     * A control button's intent, aimed at THIS service.
     *
     * `getService` rather than a broadcast receiver: the service is already
     * running in the foreground, so the intent is delivered to its own
     * `onStartCommand` with no new start — which is the only shape Android 12+
     * allows from a notification anyway (starting a service from the background
     * is refused). The request code is per action because a `PendingIntent` is
     * cached by its identity: four actions sharing one code would all resolve to
     * whichever was built first.
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
        const val ACTION_PREV = "com.curio.app.action.READ_ALOUD_PREV"
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

package com.curio.app.infrastructure

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.curio.app.MainActivity
import com.curio.app.R
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSession
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.formatElapsed
import com.curio.app.data.reflectionQuestion
import com.curio.app.data.toJsonString
import com.curio.app.ui.components.ExploreBubbleContent
import com.curio.app.ui.theme.CurioShapes
import com.curio.app.ui.theme.CurioTypography
import com.curio.app.ui.theme.curioColorScheme
import com.curio.app.ui.theme.isCurioDarkThemeForContext
import com.curio.app.ui.theme.pastelAccent

/**
 * Foreground service behind an active explore session.
 *
 * Two jobs, both controlled by the session + Settings:
 *
 * 1. **Live explore notification** (when "Live explore notification" is ON):
 *    a persistent, audible notification with a live elapsed-time chronometer,
 *    a progress bar against the recommended duration, the topic name, and
 *    Pause/Resume + "Done exploring" actions, tinted with the topic's
 *    category accent. Kept SHORT by design — just the topic and the elapsed
 *    time, no description lines. An elapsed clock, NOT a countdown.
 *
 * 2. **Floating explore bubble** (when "Floating explore bubble" is ON and
 *    the "Display over other apps" permission is granted): a Messenger-style
 *    bubble rendered in a `TYPE_APPLICATION_OVERLAY` window that floats over
 *    OTHER apps — including the browser — while the session runs. It shows
 *    the same topic + live timer with Pause/Resume and Hide (ending a
 *    session lives in the app's session card, not on the floating pill),
 *    starts minimized (compact chip + timer), and can be dragged anywhere
 *    (snaps to the nearest horizontal edge on release). Long topic names
 *    slow-scroll across the pill so the full name is always readable.
 *
 * The service runs while EITHER is active. When only the bubble is on, the
 * mandatory FGS notification downgrades to a quiet, non-chronometer
 * "bubble active" note (Android requires every foreground service to have a
 * notification). Every start re-arms the end-of-session reminder alarm so a
 * killed process still nudges the user.
 */
class ExploreSessionService : Service() {

    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    // ── Overlay bubble window ─────────────────────────────────────────
    private var bubbleView: View? = null
    private var bubbleComposeView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    // Last bubble size seen by the expand/collapse position compensation
    // (the ComposeView's own width/height can lag the window relayout by a
    // frame, so the deltas are computed against these instead).
    private var bubbleLastW = 0
    private var bubbleLastH = 0
    // Prevent repeated overlay attempts after a synchronous WindowManager or
    // owner setup failure (a rejected window retried on every render tick
    // would become a restart loop). The foreground notification remains
    // available; the latch is cleared again on the next explicit start so a
    // fresh session always retries the overlay (see onStartCommand).
    private var bubbleUnavailable = false
    // Self-heal budget: ONE transient overlay attach/composition failure
    // (a window-install race, an Android 16 attach hiccup, an OEM rejection)
    // retries the bubble shortly after instead of latching it off for the
    // whole session — but the budget is hard-capped so a persistent
    // device-level rejection can never turn into a restart loop. Reset on
    // every explicit start (see onStartCommand).
    private var bubbleRetryCount = 0
    // v253 — which horizontal edge the window last snapped to: -1 left,
    // 1 right, 0 unknown. Read inside the bubble's composition to drive the
    // assistive-touch-style edge dock (the pill slides mostly off-screen,
    // leaving a peek sliver, after a few idle seconds at the edge).
    private val bubbleEdgeSnap = mutableStateOf(0)

    // ── Periodic live-notification refresh ─────────────────────────────
    // The shade chronometer ticks live, but the progress bar and expanded
    // text only update when the notification is re-posted. A gentle
    // every-minute tick re-renders while a live notification is showing, so
    // the progress bar creeps forward and the text never goes stale.
    private val mainHandler = Handler(Looper.getMainLooper())
    private val notificationTick = object : Runnable {
        override fun run() {
            // Re-render from the persisted session — refreshes the progress
            // bar + content text; render() re-schedules this tick while a
            // live notification is wanted and stops the service otherwise.
            render()
        }
    }

    // ── Overlay-window owner ─────────────────────────────────────────
    // A TYPE_APPLICATION_OVERLAY window has no Activity behind it, so the
    // bubble's ComposeView inherits no ViewTree owners. Without them,
    // attaching the view throws "ViewTreeSavedStateRegistryOwner not found".
    // The owner is created only while a bubble is actually needed, and its
    // construction is guarded so an OEM/Lifecycle failure falls back to the
    // notification instead of taking down the process in a restart loop.
    private var overlayOwner: OverlayOwner? = null

    /**
     * Service-owned owners for the overlay bubble's ComposeView. A plain
     * (static) nested class — no outer-service reference — keeps the overlay
     * independent from the Activity while still providing every owner that
     * ComposeView requires when it is attached directly to WindowManager.
     */
    private class OverlayOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {
        private val store = ViewModelStore()
        private val registry = LifecycleRegistry.createUnsafe(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        init {
            // ComposeView resolves the saved-state owner during attachment,
            // before its content is composed. Attach the controller first,
            // then move the lifecycle through the normal owner states.
            //
            // NOTE (savedstate 1.4.0): performAttach() now registers the
            // Recreator lifecycle observer immediately, and that observer
            // calls consumeRestoredStateForKey("androidx.savedstate.Restarter")
            // on ON_CREATE. consumeRestoredStateForKey is guarded by
            // check(isRestored), which is only satisfied by performRestore().
            // Skipping it (as the old savedstate allowed) throws
            // "You can 'consumeRestoredStateForKey' only after the
            // corresponding component has moved to the 'CREATED' state",
            // which showBubble() caught and silently demoted the floating
            // bubble to notification-only. This mirrors ComponentActivity's
            // contract: performAttach() at INITIALIZED, then performRestore()
            // before the lifecycle is allowed to reach ON_CREATE/STARTED.
            savedStateController.performAttach()
            savedStateController.performRestore(null)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry get() = savedStateController.savedStateRegistry

        fun destroy() {
            if (registry.currentState != Lifecycle.State.DESTROYED) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
            store.clear()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // Seed the reactive session store from persisted prefs. A system
        // re-arm (boot receiver, START_STICKY after process death) can start
        // this service in a process where MainActivity never ran, leaving
        // activeSessionState null — the bubble's composition falls back to
        // the persisted session (see setContent), but seeding here keeps the
        // whole reactive flow (pause/resume/hide recomposition) working in
        // fresh processes too. Idempotent — re-reading the same prefs after
        // MainActivity's seed is a no-op.
        ExploreSessionStore.seed(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // Toggled pause from the notification or the bubble — flip the
            // store; the render below re-draws in the new state (and the
            // bubble/notification recompose from the reactive session).
            ACTION_TOGGLE_PAUSE -> {
                val current = ExploreSessionStore.getActiveSession(this) ?: return stopQuietly()
                if (current.paused) ExploreSessionStore.resumeSession(this)
                else ExploreSessionStore.pauseSession(this)
            }
        }
        // A fresh explicit start — a new explore session, or a re-arm after
        // settings/permission changes — gets a clean overlay slate. The
        // bubble-failure latch exists to stop restart loops (never retry a
        // rejected window on every render tick), NOT to permanently disable
        // the bubble: a transient WindowManager / Android 16 attach failure
        // in an earlier session must not silence the floating bubble for
        // every later session in this process. Every explicit start retries
        // the overlay once.
        if (intent?.hasExtra(EXTRA_SESSION) == true) {
            bubbleUnavailable = false
            bubbleRetryCount = 0
        }
        return render()
    }

    /**
     * Re-evaluates what the service should show from the persisted session
     * and the Settings toggles, and stops itself when nothing is wanted.
     */
    private fun render(): Int {
        val session = ExploreSessionStore.getActiveSession(this) ?: return stopQuietly()
        val liveNotif = AppPreferences.isLiveNotificationsEnabled(this)
        // v7.35 — [AppPreferences.overlayActuallyUsable] instead of raw
        // canDrawOverlays: on Android 15+ a first-time grant (or one right
        // after clearing app data) can sit in the system's PENDING state,
        // where canDrawOverlays() returns true but the overlay is silently
        // never shown. Treating that as not granted keeps the notification
        // path honest and lets the permission prompts re-ask.
        // The bubble is visually hidden when the user taps Hide (pillHidden)
        // or when the overlay permission is missing/disabled. But hiding the
        // bubble must NOT kill the notification — the two are independent.
        // The bubble is *usable* when the overlay permission is available;
        // whether it's actually shown depends on pillHidden.
        val bubbleUsable = AppPreferences.isOverlayBubbleEnabled(this) &&
            AppPreferences.overlayActuallyUsable(this)
        val bubbleWanted = bubbleUsable && !session.pillHidden
        // Only stop the service when BOTH the live notification AND the
        // bubble's overlay are genuinely unavailable — hiding the bubble
        // (pillHidden) should leave the notification running.
        if (!liveNotif && !bubbleUsable) return stopQuietly()

        promote(if (liveNotif) liveNotification(session) else bubbleOnlyNotification(session))
        if (bubbleWanted) showBubble() else removeBubble()
        // Keep a single periodic refresh chain (never stack ticks): the
        // chronometer in the shade ticks on its own, this keeps the progress
        // bar + content text honest while a live notification is showing.
        mainHandler.removeCallbacks(notificationTick)
        if (liveNotif) mainHandler.postDelayed(notificationTick, NOTIFICATION_REFRESH_MS)
        return START_STICKY
    }

    private fun stopQuietly(): Int {
        removeBubble()
        stopSelf()
        return START_NOT_STICKY
    }

    /**
     * v27 — the bubble's Finish button. Ends the session the same way as the
     * notification's "Done exploring" (shared teardown in
     * [com.curio.app.infrastructure.ExploreReminderReceiver]): hands the
     * session's elapsed time + shared note + screenshots off to the write
     * package, clears the session, cancels the reminder, stops this service,
     * and navigates to the write-it-down page. Reuses the receiver's
     * ACTION_STOP so every end path stays in lock-step.
     */
    private fun finishToWritePage() {
        runCatching {
            sendBroadcast(
                Intent(this, ExploreReminderReceiver::class.java)
                    .setAction(ExploreReminderReceiver.ACTION_STOP)
            )
        }
    }

    /** Puts the service into the foreground with [notification]. */
    private fun promote(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ── Notifications ─────────────────────────────────────────────────

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            4201,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun stopSessionIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            4202,
            Intent(this, ExploreReminderReceiver::class.java)
                .setAction(ExploreReminderReceiver.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * "Cancel" — ends the session QUIETLY: same teardown as Done-exploring
     * (clear session, cancel reminder, stop the service) but NO navigation
     * to the write-it-down page. The notification simply disappears with
     * the service stop.
     */
    private fun cancelSessionIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            4203,
            Intent(this, ExploreReminderReceiver::class.java)
                .setAction(ExploreReminderReceiver.ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun togglePauseIntent(): PendingIntent =
        PendingIntent.getService(
            this,
            4204,
            Intent(this, ExploreSessionService::class.java)
                .setAction(ACTION_TOGGLE_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Full live-timer notification — used when live notifications are ON. A
     * default-importance channel so it's actually seen/heard, with a live
     * chronometer, a progress bar against the recommended duration, and a
     * SHORT readout (just the elapsed time — no description lines).
     */
    private fun liveNotification(session: ExploreSession): Notification {
        val category = CurioCategories.byId(session.categoryId)
        val accent = notificationAccent(category)
        val elapsed = session.elapsedMillis()
        val paused = session.paused
        val totalMins = session.durationMinutes.coerceAtLeast(1)
        // Progress bar: elapsed minutes vs. the recommended duration (capped
        // so a long explore simply fills the bar).
        val progressMins = (elapsed / 60_000L).toInt().coerceAtMost(totalMins)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentTitle(
                if (paused) "Paused: ${session.topicName}"
                else "Exploring ${session.topicName}"
            )
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(totalMins, progressMins, false)

        // v226 — the stale inner readout is gone while running: the
        // shade's own live chronometer IS the timer now (the old text only
        // refreshed on the service's periodic re-render, so it visibly
        // lagged the ticking chronometer next to it). Paused keeps a frozen
        // text readout — the chronometer is dropped there, so text is the
        // only time shown and it never drifts.
        val body = if (paused) "Paused · ${formatElapsed(elapsed)}" else ""
        if (paused) {
            // Frozen readout — the chronometer would keep counting, so drop
            // it and print the banked elapsed time as text instead. The
            // progress bar freezes where the timer paused. Short on purpose:
            // topic + elapsed only, no verb/target or description lines.
            builder
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setContentText(body)
                .addAction(0, "Resume", togglePauseIntent())
        } else {
            // Live chronometer anchored at start + banked pauses, so it shows
            // active elapsed time even after pause/resume cycles — the system
            // ticks it in the shade without any app wakeups. The progress bar
            // re-renders on pause/resume (the chronometer ticks in between).
            builder
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(session.startMillis + session.accumulatedPausedMillis)
                .addAction(0, "Pause", togglePauseIntent())
        }
        builder
            .addAction(0, "Done exploring", stopSessionIntent())
            // Plain cancel — end the session without jumping to the
            // write-it-down page (Done exploring opens it).
            .addAction(0, "Cancel", cancelSessionIntent())

        if (!paused && android.os.Build.VERSION.SDK_INT >= 36) {
            // v227/v229 — RUNNING on Android 16+: post a genuine LIVE UPDATE.
            // The framework's ProgressStyle is the rich ongoing-notification
            // surface (the same one LiveBridge promotes other apps' progress
            // notifications into): the shade renders a styled segment bar,
            // and the system may promote the notification to a status-bar
            // chip / lock-screen live activity. v229 fix — the style alone
            // was never enough: promotion must be REQUESTED via
            // `setRequestPromotedOngoing(true)` (exactly what LiveBridge
            // does before posting), the segments need
            // `setStyledByProgress(true)` so the fill tracks progress, and
            // `setShortCriticalText` carries the readout onto the chip.
            builder.setRequestPromotedOngoing(true)
            builder.setShortCriticalText("${progressMins}m")
            val liveUpdateStyle = NotificationCompat.ProgressStyle()
                .setProgress(progressMins)
                .setStyledByProgress(true)
                .addProgressSegment(
                    NotificationCompat.ProgressStyle.Segment(totalMins).setColor(accent)
                )
                // IconCompat (NOT the framework Icon) — the compat style
                // bridges it to the framework tracker itself on API 36+.
                .setProgressTrackerIcon(
                    IconCompat.createWithResource(this, R.drawable.ic_notification)
                )
            builder.setStyle(liveUpdateStyle)
        } else {
            // Category-flavored reflection question — shown under the timer
            // when the notification is expanded ("Finished listening? What
            // track or lyric landed hardest?" etc.), so the wrap-up nudge
            // leaves the user with something to write down.
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        if (paused) "$body\n${session.reflectionQuestion()}"
                        else session.reflectionQuestion()
                    )
            )
        }
        return builder.build()
    }

    /**
     * Minimal quiet notification — used when live notifications are OFF but
     * the floating bubble is on (Android mandates a notification for every
     * foreground service). No chronometer; the bubble carries the live timer.
     */
    private fun bubbleOnlyNotification(session: ExploreSession): Notification {
        val accent = notificationAccent(CurioCategories.byId(session.categoryId))
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setContentTitle("Explore bubble · ${session.topicName}")
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Done exploring", stopSessionIntent())
            .addAction(0, "Cancel", cancelSessionIntent())
            .build()
    }

    /**
     * The category accent a notification wears — resolved the same way the
     * UI resolves it. v7.5 — pastel mode softens the accent to its pastel
     * twin (muted deep pastel in dark, airy pastel in light) so the shade
     * matches the rest of the app; the raw deep accent is used otherwise.
     */
    private fun notificationAccent(category: CurioCategory): Int {
        val raw = category.accent
        return if (AppPreferences.isPastelColorsEnabled(this))
            pastelAccent(raw, isCurioDarkThemeForContext(this)).toArgb()
        else raw.toArgb()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Explore session timer",
                // DEFAULT (not LOW) so the live timer is actually seen and
                // heard when an explore starts — the user asked for a live
                // notification, and a LOW channel collapses into the silent
                // section and gets missed. onlyAlertOnce prevents nagging.
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows how long you've been exploring a topic and when to wrap up."
            }
        )
    }

    // ── Floating bubble (system overlay window) ───────────────────────

    /** Adds the bubble window if it isn't already showing. */
    private fun showBubble() {
        if (bubbleView != null || bubbleUnavailable) return

        // Window params are created FIRST so the compose content below can
        // capture them for drag updates (the content composes on attach, by
        // which point this local is fully initialized).
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // v27h — FLAG_SECURE REMOVED: it blanked the overlay in the
            // user's own device screenshots (and made the session screenshot
            // feature feel blocked). The bubble is now capturable — the user
            // WANTS the session visible in the shots they take and the ones
            // the session attaches.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val owner = runCatching {
            overlayOwner ?: OverlayOwner().also { overlayOwner = it }
        }.getOrElse { error ->
            bubbleUnavailable = true
            Log.e(TAG, "Unable to create overlay Compose owners; using notification only", error)
            return
        }

        // WindowManager makes an overlay root its own ViewRootImpl. Keep the
        // ComposeView one level below a plain host and put all ViewTree owners
        // on that host (and the child) before attachment. This is important on
        // Android 16: a ComposeView used as the direct overlay root can still
        // resolve an empty tree during ViewRootImpl attachment even when its
        // own tags were set.
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            bubbleComposeView = this
            // Defer composition until the host is attached to the overlay
            // window. ComposeView otherwise resolves its ViewTree owners while
            // WindowManager is still installing the ViewRootImpl on Android 16
            // — and a plain post() can run BEFORE that attach and skip the
            // composition forever (the "attached but empty" bubble, then a
            // rebuild loop). doOnAttach runs exactly when the window is up
            // (immediately when already attached), so the content always
            // composes on a fully-attached host.
            doOnAttach {
                if (bubbleUnavailable) return@doOnAttach
                runCatching {
                    setContent {
                // Reads the reactive session — pause/resume/hide from the
                // notification or the bubble recompose this content live.
                // Falls back to the persisted session when the reactive
                // store hasn't been seeded: a system re-arm (START_STICKY
                // after process death, boot receiver) can start this
                // service in a process where MainActivity never ran, leaving
                // activeSessionState null — without the fallback the overlay
                // window would compose nothing (an invisible empty bubble).
                val session = ExploreSessionStore.activeSessionState
                    ?: ExploreSessionStore.getActiveSession(this@ExploreSessionService)
                if (session != null) {
                    MaterialTheme(
                        colorScheme = curioColorScheme(),
                        typography = CurioTypography,
                        shapes = CurioShapes
                    ) {
                        ExploreBubbleContent(
                            session = session,
                            edgeSnap = bubbleEdgeSnap,
                            onTogglePause = {
                                val current = ExploreSessionStore.getActiveSession(this@ExploreSessionService)
                                    ?: return@ExploreBubbleContent
                                if (current.paused) {
                                    ExploreSessionStore.resumeSession(this@ExploreSessionService)
                                } else {
                                    ExploreSessionStore.pauseSession(this@ExploreSessionService)
                                }
                                render()
                            },
                            onHide = {
                                ExploreSessionStore.setPillHidden(this@ExploreSessionService, true)
                                render()
                            },
                            // v27 — the note field writes the session's SHARED
                            // note live (one note per session; the finish flow
                            // hands it off and every entry saved from the
                            // session carries it).
                            onNoteChange = { note ->
                                ExploreSessionStore.setSessionNote(this@ExploreSessionService, note)
                            },
                            // v27 — the Finish button ends the session exactly
                            // like the notification's "Done exploring": hand
                            // off note + screenshots, clear, and hand the
                            // user to the write-it-down page.
                            onFinish = {
                                finishToWritePage()
                            },
                            // v226 — Cancel: same teardown as the
                            // notification's Cancel action, but the session
                            // is stashed first so Home's cancelled-explore
                            // row can revive it. No write-it-down
                            // navigation.
                            onCancel = {
                                val current = ExploreSessionStore.getActiveSession(this@ExploreSessionService)
                                    ?: return@ExploreBubbleContent
                                ExploreSessionStore.stashCancelledSession(this@ExploreSessionService, current)
                                ExploreSessionStore.clearSession(this@ExploreSessionService)
                                ExploreReminderScheduler.cancel(this@ExploreSessionService)
                                stopQuietly()
                            },
                            // v27 — typing in the note field needs a FOCUSABLE
                            // window (an overlay with FLAG_NOT_FOCUSABLE can't
                            // receive the keyboard). Flip the window flags for
                            // the duration of the note edit and restore them on
                            // blur. No FLAG_SECURE (v27h): the note is part of
                            // the session the user is allowed to screenshot.
                            onNoteFocusChange = { focused ->
                                val view = bubbleView ?: return@ExploreBubbleContent
                                val p = bubbleParams ?: return@ExploreBubbleContent
                                val newFlags = if (focused) {
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                } else {
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                }
                                if (p.flags != newFlags) {
                                    p.flags = newFlags
                                    runCatching { windowManager.updateViewLayout(view, p) }
                                }
                            },
                            // Drag lives in Compose (the composed child of an
                            // overlay ComposeView consumes every View-level
                            // touch, so a View listener never fires). Each
                            // delta moves the window; release settles where
                            // dropped. Deltas are CLAMPED live against the
                            // display bounds so a fast fling can never drag
                            // the pill half off-screen mid-gesture (the old
                            // release-only clamp read as a rubber-band snap).
                            onDragBy = { dx, dy ->
                                val view = bubbleView ?: return@ExploreBubbleContent
                                val bounds = windowBounds()
                                val marginPx = (12 * resources.displayMetrics.density).toInt()
                                params.x = (params.x + dx).toInt()
                                    .coerceIn(marginPx, (bounds.width() - view.width - marginPx).coerceAtLeast(marginPx))
                                params.y = (params.y + dy).toInt()
                                    .coerceIn(marginPx, (bounds.height() - view.height - marginPx).coerceAtLeast(marginPx))
                                runCatching { windowManager.updateViewLayout(view, params) }
                            },
                            onDragEnd = { snapBubble() },
                            // Center-anchored growth: as the bubble animates
                            // between the pill and the panel, keep its visual
                            // center pinned (grow around the middle) instead
                            // of anchored to the window's top-left, and clamp
                            // so the larger panel never leaves the screen.
                            // Compose only forwards this while an expand /
                            // collapse transition runs, so the per-second
                            // timer tick (a 1-2px width change) can't drift
                            // the bubble.
                            onSizeChanged = { w, h ->
                                val view = bubbleView ?: return@ExploreBubbleContent
                                val p = bubbleParams ?: return@ExploreBubbleContent
                                if (bubbleLastW == 0 || bubbleLastH == 0) {
                                    bubbleLastW = view.width
                                    bubbleLastH = view.height
                                }
                                val deltaX = (w - bubbleLastW) / 2
                                val deltaY = (h - bubbleLastH) / 2
                                if (deltaX == 0 && deltaY == 0) return@ExploreBubbleContent
                                bubbleLastW = w
                                bubbleLastH = h
                                val bounds = windowBounds()
                                val marginPx = (12 * resources.displayMetrics.density).toInt()
                                val minX = marginPx
                                val maxX = (bounds.width() - w - marginPx).coerceAtLeast(minX)
                                val minY = marginPx
                                val maxY = (bounds.height() - h - marginPx).coerceAtLeast(minY)
                                val newX = (p.x - deltaX).coerceIn(minX, maxX)
                                val newY = (p.y - deltaY).coerceIn(minY, maxY)
                                if (newX == p.x && newY == p.y) return@ExploreBubbleContent
                                p.x = newX
                                p.y = newY
                                view.post {
                                    runCatching { windowManager.updateViewLayout(view, p) }
                                }
                            }
                        )
                    }
                }
                    }
                }.onFailure { error ->
                    handleOverlayFailure(error)
                }
            }
        }
        val view = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val addResult = runCatching { windowManager.addView(view, params) }
        if (addResult.isFailure) {
            // Overlay unavailable (permission revoked between the check and
            // the add, or an OEM rejection) — skip the bubble; the
            // notification-only path keeps running. Never retry this failed
            // window on every service tick, because that can turn an OEM
            // rejection into a restart loop. The latch is cleared again on
            // the next explicit start (see onStartCommand).
            bubbleUnavailable = true
            Log.e(TAG, "Explore overlay window add failed; using notification only", addResult.exceptionOrNull())
            composeView.disposeComposition()
            bubbleComposeView = null
            overlayOwner?.destroy()
            overlayOwner = null
            // One bounded retry — a transient attach failure (a window-install
            // race, a system animation, an Android 16 attach hiccup) must not
            // kill the bubble for the rest of the session.
            scheduleBubbleRetry()
            return
        }
        bubbleParams = params
        // Publish the host before the posted composition can run, so a
        // composition failure can remove the actual attached overlay root.
        bubbleView = view
        // Initial placement: bottom-center, clear of the nav-bar area.
        view.doOnLayout {
            runCatching {
                val bounds = windowBounds()
                val density = resources.displayMetrics.density
                val margin = (12 * density).toInt()
                params.x = ((bounds.width() - view.width) / 2).coerceAtLeast(margin)
                params.y = (bounds.height() - view.height - (96 * density).toInt()).coerceAtLeast(margin)
                windowManager.updateViewLayout(view, params)
                // Seed the size-compensation tracker with the pill's size so
                // the first expand transition measures against it (not 0).
                bubbleLastW = view.width
                bubbleLastH = view.height
            }
        }
        // Self-heal check: the window must actually contain the composed
        // pill. A window that attached but never composed (the posted
        // composition raced attachment and bailed, or rendered nothing) is
        // zero-size and INVISIBLE — verify shortly after attach and rebuild
        // once instead of leaving the user with no bubble.
        verifyBubbleVisibleOnce()
    }

    /**
     * Disables the bubble for this service instance if composition or owner
     * resolution fails after the overlay has been attached. The failure can
     * happen from the posted Compose setup rather than inside addView(), so
     * the normal runCatching around WindowManager.addView cannot protect it.
     * Notification mode remains available instead of allowing a restart loop.
     */
    private fun handleOverlayFailure(error: Throwable) {
        if (bubbleUnavailable) return
        bubbleUnavailable = true
        Log.e(TAG, "Explore overlay composition failed; using notification only", error)
        // Whole cleanup wrapped: this handler runs from the posted
        // composition's onFailure — a throw in the teardown itself (e.g. a
        // removeView race) would escape runCatching and crash the main
        // looper. Wrapped, it also always reaches the bounded retry below.
        runCatching {
            runCatching { bubbleView?.let { windowManager.removeView(it) } }
            bubbleView = null
            bubbleParams = null
            bubbleLastW = 0
            bubbleLastH = 0
            bubbleComposeView?.disposeComposition()
            bubbleComposeView = null
            overlayOwner?.destroy()
            overlayOwner = null
        }
        // Same bounded retry as the addView path — a composition that threw
        // during attach shouldn't latch the bubble off for the session.
        scheduleBubbleRetry()
    }

    /**
     * Schedules ONE bounded retry of the overlay after a transient
     * attach/composition failure. [bubbleUnavailable] latches the failed
     * attempt (so the per-tick render never loops); this clears it once,
     * shortly after, and re-attempts — capped by [bubbleRetryCount] so a
     * persistent device-level rejection can't restart-loop.
     */
    private fun scheduleBubbleRetry() {
        if (bubbleRetryCount >= MAX_BUBBLE_RETRIES) return
        bubbleRetryCount++
        mainHandler.postDelayed({
            // Only retry when a bubble is still wanted — a later render()
            // may have torn it down or the session may have ended.
            if (bubbleUnavailable) {
                bubbleUnavailable = false
                showBubble()
            }
        }, BUBBLE_RETRY_DELAY_MS)
    }

    /**
     * Verifies the attached overlay window actually has content. If it is
     * still zero-size shortly after attach (the posted composition raced
     * attachment and bailed, or rendered nothing), tear it down and rebuild
     * once — a silently empty overlay is exactly the "no bubble" report.
     */
    private fun verifyBubbleVisibleOnce() {
        if (bubbleRetryCount >= MAX_BUBBLE_RETRIES) return
        mainHandler.postDelayed({
            val view = bubbleView ?: return@postDelayed
            if (view.width > 0 && view.height > 0) return@postDelayed
            bubbleRetryCount++
            Log.w(TAG, "Explore bubble attached but empty — rebuilding once")
            removeBubble()
            showBubble()
        }, BUBBLE_VERIFY_DELAY_MS)
    }

    /** Removes the bubble window (no-op when it isn't showing). */
    private fun removeBubble() {
        bubbleView?.let { v ->
            runCatching { windowManager.removeView(v) }
        }
        bubbleView = null
        bubbleComposeView?.disposeComposition()
        bubbleComposeView = null
        bubbleParams = null
        // Clear the size tracker too — the next showBubble's doOnLayout
        // re-seeds it; leaving stale sizes would mislead the guard.
        bubbleLastW = 0
        bubbleLastH = 0
        // Release the overlay owner with the window — the next showBubble
        // builds a FRESH owner instead of reusing a stale RESUMED one across
        // bubble lifecycles. ComposeView resolves its ViewTree owners during
        // attachment, so a fresh owner per lifecycle is the reliable path
        // (the Android 16 overlay-attach hardening depends on it).
        overlayOwner?.destroy()
        overlayOwner = null
    }

    /** v262 — NO edge snapping (user request): on release the bubble just
     *  settles WHERE IT WAS DROPPED, only clamped so it stays on-screen. */
    private fun snapBubble() {
        val view = bubbleView ?: return
        val params = bubbleParams ?: return
        val bounds = windowBounds()
        val marginPx = (12 * resources.displayMetrics.density).toInt()
        params.x = params.x.coerceIn(marginPx, (bounds.width() - view.width - marginPx).coerceAtLeast(marginPx))
        params.y = params.y.coerceIn(marginPx, (bounds.height() - view.height - marginPx).coerceAtLeast(marginPx))
        runCatching { windowManager.updateViewLayout(view, params) }
        bubbleEdgeSnap.value = 0
    }

    private fun windowBounds(): Rect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds
        } else {
            val dm = resources.displayMetrics
            Rect(0, 0, dm.widthPixels, dm.heightPixels)
        }

    override fun onDestroy() {
        mainHandler.removeCallbacks(notificationTick)
        removeBubble()
        overlayOwner?.destroy()
        overlayOwner = null
        bubbleUnavailable = false
        bubbleRetryCount = 0
        super.onDestroy()
    }

    companion object {
        const val TAG = "ExploreSessionService"
        const val EXTRA_SESSION = "explore_session_json"
        const val ACTION_TOGGLE_PAUSE = "com.curio.app.action.TOGGLE_EXPLORE_PAUSE"
        const val ACTION_SYNC = "com.curio.app.action.SYNC_EXPLORE_SESSION"
        const val CHANNEL_ID = "explore_session_timer"
        const val NOTIFICATION_ID = 4211
        // How often the live notification re-renders (progress bar + text).
        // 15s keeps the elapsed text above the progress bar visibly ticking
        // (the shade chronometer ticks on its own; the re-render refreshes
        // the text + progress against it).
        const val NOTIFICATION_REFRESH_MS = 15_000L
        // Self-heal tuning: how long to wait before retrying a transient
        // overlay failure, and how long to wait before verifying the attached
        // bubble window actually has content.
        const val BUBBLE_RETRY_DELAY_MS = 1_500L
        // Long enough that a briefly-busy main thread (browser launch + Home
        // navigation right after start) can't false-positive the check.
        const val BUBBLE_VERIFY_DELAY_MS = 3_000L
        // Hard cap on in-session overlay self-heal attempts (restart-loop
        // guard for persistent device rejections). v7.35 — raised from 2 so
        // a single attach/composition hiccup plus its verify can't exhaust
        // the budget and latch the bubble off for the whole session.
        const val MAX_BUBBLE_RETRIES = 3

        /** Starts the explore foreground service for [session]. */
        fun start(context: Context, session: ExploreSession) {
            // Crash-loop safe mode: never re-arm the service while the app is
            // recovering on the crash screen — an automatic start here is what
            // kept re-triggering the crash before the user could see the logs.
            if (CurioCrashReporter.isSafeMode(context)) return
            // A SYNCHRONOUS start failure (background FGS start on Android
            // 12+, a revoked permission, a dead context…) must never take the
            // app down — log it and continue. (The async path — the service
            // constructor throwing after a successful start — is covered by
            // the OverlayOwner init-order fix + crash-loop guard, not by this
            // catch.) The session is already persisted, the end-of-session
            // reminder still fires, and the done-prompt still shows on
            // return, so nothing is lost.
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ExploreSessionService::class.java)
                        .putExtra(EXTRA_SESSION, session.toJsonString())
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start explore service", e)
            }
        }

        /**
         * Asks the service to re-render from the persisted session — used
         * after Settings toggles change or the bubble is restored, so the
         * notification + bubble match the current state. No-op effect when
         * nothing wants the service (the render stops it quietly).
         */
        fun sync(context: Context) {
            if (CurioCrashReporter.isSafeMode(context)) return
            val session = ExploreSessionStore.getActiveSession(context) ?: return
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ExploreSessionService::class.java)
                        .setAction(ACTION_SYNC)
                        .putExtra(EXTRA_SESSION, session.toJsonString())
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync explore service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ExploreSessionService::class.java))
        }
    }
}

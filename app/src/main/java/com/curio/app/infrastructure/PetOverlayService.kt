package com.curio.app.infrastructure

import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
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
import com.curio.app.R
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.ui.pet.CurioPetSprite
import com.curio.app.ui.theme.CurioShapes
import com.curio.app.ui.theme.CurioTypography
import com.curio.app.ui.theme.curioColorScheme

/**
 * v256 — THE PET OUTSIDE THE APP. A tiny system-overlay window
 * (`TYPE_APPLICATION_OVERLAY`) that keeps the pet sprite floating over
 * other apps while Curio runs in the background — opt-in from the Pet
 * Designer's Settings page ("Display over other apps" permission required).
 *
 * Deliberately MINIMAL compared to the in-app companion: no quests,
 * reactions or speech bubbles (those are wired to app screens and would be
 * meaningless over other apps). The sprite is [CurioPetSprite] reading the
 * same saved design + growth stage; tap it for a happy hop, drag it
 * anywhere on screen, long-press to send it home.
 *
 * The overlay ComposeView plumbing mirrors [ExploreSessionService]'s proven
 * construction: a plain FrameLayout host carries the ViewTree owners (a
 * direct overlay-root ComposeView can resolve an empty tree during attach
 * on Android 16), composition happens on doOnAttach, and a bounded retry
 * covers transient attach failures without ever restart-looping.
 */
class PetOverlayService : Service() {

    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    // v261 — shared with the overlay composition so the WANDER loop can flip
    // the pet's facing and the menu can render inside the same window.
    private val facingState = androidx.compose.runtime.mutableStateOf(1f)
    private val draggingState = androidx.compose.runtime.mutableStateOf(false)
    private val wanderState = androidx.compose.runtime.mutableStateOf(true)
    private val menuVisibleState = androidx.compose.runtime.mutableStateOf(false)

    /** Persisted position — the pet STAYS where the user left it. */
    private val overlayPrefs by lazy {
        getSharedPreferences("curio_pet_overlay", MODE_PRIVATE)
    }

    private var wanderScheduled = false

    private var petView: View? = null
    private var petParams: WindowManager.LayoutParams? = null
    private var overlayOwner: OverlayOwner? = null
    private var composeUnavailable = false
    private var retryCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // Re-check after process death / re-arm: the window may need rebuilding.
        if (petView == null && !composeUnavailable) showOverlay()
        return START_STICKY
    }

    // ── Overlay window ────────────────────────────────────────────────

    private fun showOverlay() {
        if (petView != null || composeUnavailable) return
        if (!Settings.canDrawOverlays(this)) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val owner = runCatching {
            overlayOwner ?: OverlayOwner().also { overlayOwner = it }
        }.getOrElse { error ->
            latchUnavailable("owner setup failed", error)
            return
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            doOnAttach {
                if (composeUnavailable) return@doOnAttach
                runCatching { setContent { PetOverlayContent() } }
                    .onFailure { error -> latchUnavailable("composition failed", error) }
            }
        }

        val view = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
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
            Log.e(TAG, "Pet overlay add failed", addResult.exceptionOrNull())
            composeView.disposeComposition()
            overlayOwner?.destroy()
            overlayOwner = null
            scheduleRetry()
            return
        }
        petParams = params
        petView = view
        // v261 — restore the SAVED position; fall back to bottom-end only on
        // first launch. The pet now stays wherever the user put it.
        view.doOnLayout {
            runCatching {
                val dm = resources.displayMetrics
                val margin = (16 * dm.density).toInt()
                val savedX = overlayPrefs.getInt(KEY_X, Int.MIN_VALUE)
                val savedY = overlayPrefs.getInt(KEY_Y, Int.MIN_VALUE)
                params.x = if (savedX != Int.MIN_VALUE) {
                    savedX.coerceIn(margin, (dm.widthPixels - view.width - margin).coerceAtLeast(margin))
                } else {
                    dm.widthPixels - view.width - margin
                }
                params.y = if (savedY != Int.MIN_VALUE) {
                    savedY.coerceIn(margin, (dm.heightPixels - view.height - margin).coerceAtLeast(margin))
                } else {
                    (dm.heightPixels - view.height - margin * 6).coerceAtLeast(margin)
                }
                windowManager.updateViewLayout(view, params)
                scheduleWander()
            }
        }
    }

    private fun savePosition() {
        val p = petParams ?: return
        overlayPrefs.edit().putInt(KEY_X, p.x).putInt(KEY_Y, p.y).apply()
    }

    // ── Wander loop ───────────────────────────────────────────────────
    // v261 — the pet ambles around ON ITS OWN while idle: every few seconds
    // it strolls a short hop toward a nearby random point (clamped to the
    // screen), flipping its facing with the direction. Dragging pauses it;
    // long-press → menu can toggle wandering off.

    fun toggleWander() {
        wanderState.value = !wanderState.value
        if (wanderState.value) scheduleWander()
    }

    private fun scheduleWander() {
        if (wanderScheduled || !wanderState.value) return
        wanderScheduled = true
        mainHandler.postDelayed({
            wanderScheduled = false
            wanderStep()
        }, (2_200L..4_400L).random())
    }

    private fun wanderStep() {
        val view = petView ?: return
        val params = petParams ?: return
        if (draggingState.value) { scheduleWander(); return }
        val dm = resources.displayMetrics
        val density = dm.density
        val margin = (12 * density).toInt()
        val maxX = (dm.widthPixels - view.width - margin).coerceAtLeast(margin)
        val maxY = (dm.heightPixels * 2 / 3).coerceAtLeast(margin) // keep off the nav area
        val dx = ((50..170).random() * listOf(-1, 1).random()).toInt()
        val dy = ((-110..110).random()).toInt()
        val targetX = (params.x + dx).coerceIn(margin, maxX)
        val targetY = (params.y + dy).coerceIn(margin, maxOf(maxY, params.y))
        if (dx != 0) facingState.value = if (dx > 0f) 1f else -1f

        // ~14-frame eased glide (~450ms).
        val startX = params.x; val startY = params.y
        var frame = 0; val frames = 14
        val stepper = object : Runnable {
            override fun run() {
                val v = petView ?: return
                val pp = petParams ?: return
                frame++
                val t = frame.toFloat() / frames
                val ease = 1f - (1f - t) * (1f - t) // easeOutQuad
                pp.x = (startX + (targetX - startX) * ease).toInt()
                pp.y = (startY + (targetY - startY) * ease).toInt()
                runCatching { windowManager.updateViewLayout(v, pp) }
                if (frame < frames) mainHandler.postDelayed(this, 33) else savePosition()
            }
        }
        mainHandler.postDelayed(stepper, 33)
        scheduleWander()
    }

    /**
     * The overlay content: the saved pet design at its current growth stage,
     * draggable via raw deltas (the service moves the window), tap = hop.
     */
    @Composable
    private fun PetOverlayContent() {
        MaterialTheme(
            colorScheme = curioColorScheme(),
            typography = CurioTypography,
            shapes = CurioShapes
        ) {
            var squishKey by remember { mutableIntStateOf(0) }
            var celebrateKey by remember { mutableIntStateOf(0) }
            // v261 — facing/dragging live on the SERVICE so the wander loop
            // can drive them too; the menu renders inside this window.
            var facing = facingState
            var dragged = draggingState
            val menuVisible = menuVisibleState
            val stage = remember { CurioPet.currentStage() }
            val mood = remember { CurioPet.mood(this@PetOverlayService, emptySet()) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (menuVisible.value) {
                    // v261 — LONG-PRESS MENU (was: a silent send-home with no
                    // dialog). Two pill actions inside the overlay window:
                    // Send home + Wandering toggle.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        OverlayMenuPill("Send home", MaterialTheme.colorScheme.error) {
                            menuVisible.value = false
                            AppPreferences.setPetOutsideAppEnabled(
                                this@PetOverlayService, false
                            )
                            stopSelf()
                        }
                        OverlayMenuPill(
                            if (wanderState.value) "Wander ✓" else "Wander ✗",
                            MaterialTheme.colorScheme.primary
                        ) {
                            toggleWander()
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Box(
                    modifier = Modifier
                        .size(PET_WINDOW_DP)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    dragged.value = true
                                    menuVisible.value = false
                                },
                                onDragEnd = {
                                    dragged.value = false
                                    // v262 — stays exactly where dropped; only
                                    // clamp back on-screen.
                                    settleInBounds()
                                    savePosition()
                                    scheduleWander()
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                if (dragAmount.x != 0f) {
                                    facing.value = if (dragAmount.x > 0f) 1f else -1f
                                }
                                val p = petParams ?: return@detectDragGestures
                                p.x += dragAmount.x.toInt()
                                p.y += dragAmount.y.toInt()
                                petView?.let { v ->
                                    runCatching { windowManager.updateViewLayout(v, p) }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (menuVisible.value) {
                                        menuVisible.value = false
                                    } else {
                                        squishKey++
                                        celebrateKey++
                                    }
                                },
                                onLongPress = {
                                    // v261 — opens the in-window menu instead of
                                    // instantly sending the pet home.
                                    menuVisible.value = !menuVisible.value
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // v261 — the sprite renders directly (the old invisible
                    // circle carried a shadow that drew a stray gray blob).
                    CurioPetSprite(
                        stage = stage,
                        mood = mood,
                        spriteSize = 84.dp,
                        celebrateKey = celebrateKey,
                        squishKey = squishKey,
                        moving = false,
                        dragged = dragged.value,
                        facing = facing.value
                    )
                }
            }
        }
    }

    /** Small text pill used inside the overlay's long-press menu. */
    @Composable
    private fun OverlayMenuPill(label: String, tint: Color, onClick: () -> Unit) {
        Text(
            label,
            color = tint,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }

    /** v262 — clamps the pet back on-screen WITHOUT moving it to an edge:
     *  it stays wherever the user dropped it. */
    private fun settleInBounds() {
        val view = petView ?: return
        val params = petParams ?: return
        val dm = resources.displayMetrics
        val margin = (8 * dm.density).toInt()
        params.x = params.x.coerceIn(margin, (dm.widthPixels - view.width - margin).coerceAtLeast(margin))
        params.y = params.y.coerceIn(margin, (dm.heightPixels - view.height - margin).coerceAtLeast(margin))
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    // ── Lifecycle plumbing ────────────────────────────────────────────

    private fun latchUnavailable(reason: String, error: Throwable) {
        if (composeUnavailable) return
        composeUnavailable = true
        Log.e(TAG, "Pet overlay unavailable ($reason); stopping", error)
        stopSelf()
    }

    private fun scheduleRetry() {
        if (retryCount >= MAX_RETRIES) return
        retryCount++
        mainHandler.postDelayed({
            if (petView == null && !composeUnavailable) showOverlay()
        }, RETRY_DELAY_MS)
    }

    override fun onDestroy() {
        runCatching { petView?.let { windowManager.removeView(it) } }
        petView = null
        petParams = null
        overlayOwner?.destroy()
        overlayOwner = null
        super.onDestroy()
    }

    // ── Notification ──────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Pet companion",
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = "Keeps your pet floating outside the app." }
        )
        val openApp = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName)
                ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Your pet is tagging along outside the app")
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    /**
     * Service-owned owners for the overlay ComposeView — the same plain
     * nested-class construction [ExploreSessionService] uses (no outer
     * reference; every owner a detached ComposeView needs).
     */
    private class OverlayOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {
        private val store = ViewModelStore()
        private val registry = LifecycleRegistry.createUnsafe(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        init {
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

    companion object {
        private const val TAG = "PetOverlayService"
        private const val CHANNEL_ID = "pet_overlay"
        private const val NOTIFICATION_ID = 3001
        private const val MAX_RETRIES = 1
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val RETRY_DELAY_MS = 2_000L
        private const val ACTION_STOP = "com.curio.app.pet_overlay.STOP"
        private val PET_WINDOW_DP = 96.dp

        /** Whether the overlay service is wanted by the preference + permission. */
        fun shouldRun(context: Context): Boolean =
            AppPreferences.isPetOutsideAppEnabled(context) &&
                Settings.canDrawOverlays(context)

        fun sync(context: Context) {
            if (shouldRun(context)) start(context) else stop(context)
        }

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            ContextCompat.startForegroundService(
                context,
                Intent(context, PetOverlayService::class.java)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, PetOverlayService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}

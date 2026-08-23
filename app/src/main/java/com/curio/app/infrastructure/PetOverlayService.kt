package com.curio.app.infrastructure

import android.app.Notification
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
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
        // Initial placement: bottom-end, above the gesture area.
        view.doOnLayout {
            runCatching {
                val dm = resources.displayMetrics
                val margin = (16 * dm.density).toInt()
                params.x = dm.widthPixels - view.width - margin
                params.y = (dm.heightPixels - view.height - margin * 6).coerceAtLeast(margin)
                windowManager.updateViewLayout(view, params)
            }
        }
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
            var facing by remember { mutableStateOf(1f) }
            var dragged by remember { mutableStateOf(false) }
            val stage = remember { CurioPet.currentStage() }
            val mood = remember { CurioPet.mood(this@PetOverlayService, emptySet()) }

            Box(
                modifier = Modifier
                    .size(PET_WINDOW_DP)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragged = true },
                            onDragEnd = {
                                dragged = false
                                snapToNearestEdge()
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            facing = if (dragAmount.x != 0f) {
                                if (dragAmount.x > 0f) 1f else -1f
                            } else facing
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
                                squishKey++
                                celebrateKey++
                            },
                            onLongPress = {
                                // Long-press sends the pet home (stops the service).
                                AppPreferences.setPetOutsideAppEnabled(
                                    this@PetOverlayService, false
                                )
                                stopSelf()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    CurioPetSprite(
                        stage = stage,
                        mood = mood,
                        spriteSize = 64.dp,
                        celebrateKey = celebrateKey,
                        squishKey = squishKey,
                        moving = false,
                        dragged = dragged,
                        facing = facing
                    )
                }
            }
        }
    }

    /** Snaps the pet window to the nearest horizontal edge after a drag. */
    private fun snapToNearestEdge() {
        val view = petView ?: return
        val params = petParams ?: return
        val dm = resources.displayMetrics
        val margin = (8 * dm.density).toInt()
        val snapLeft = params.x + view.width / 2 <= dm.widthPixels / 2
        params.x = if (snapLeft) margin else dm.widthPixels - view.width - margin
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

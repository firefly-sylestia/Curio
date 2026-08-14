package com.curio.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureRepository
import com.curio.app.data.CurioDatabase
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.RecycleBinExpiry
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.TopicProgressStore
import com.curio.app.data.UpdateChecker
import kotlinx.coroutines.launch
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.navigation.CurioNavHost
import com.curio.app.navigation.PendingEntryOpen
import com.curio.app.navigation.PendingSpinOpen
import com.curio.app.ui.theme.CurioTheme

/**
 * Curio's single Activity — see Curio design contract.
 *
 * Hosts the entire app via [CurioNavHost] inside [CurioTheme]. Edge-to-edge
 * is enabled so the splash background and bottom-nav surface extend behind
 * the system bars (M3 expressive + the spec's "warm cream" feel).
 *
 * Installs [TopicJsonLoader] before any Compose code runs so the loader
 * has access to the AssetManager. Topic JSONs are read lazily on first
 * access; screens load only the category data they need.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // A "Done exploring" notification action may carry the topic to open,
        // and the daily-reminder tap may carry a spin-deck request
        // (cold-start path) — stash both for the NavHost to navigate to once
        // the splash settles on HOME. Gated on a FRESH process (no saved
        // instance state): recreation (rotation / process-death restore)
        // re-delivers the same intent and would otherwise re-trigger the
        // navigation over a state the user already has on screen.
        if (savedInstanceState == null) {
            PendingEntryOpen.capture(intent)
            PendingSpinOpen.capture(intent)
        }

        // Wire the asset manager into the topic loader before any Compose
        // code runs. Must happen here (not in SplashScreen's LaunchedEffect)
        // because CurioNavHost routes are resolved synchronously on first
        // composition, before the splash coroutine has a chance to run.
        TopicJsonLoader.install(this)

        // Initialize crash reporter before anything else
        CurioCrashReporter.init(this)

        // Initialize Room database and repository singleton
        val db = CurioDatabase.getInstance(this)
        CurioRepositoryHolder.init(db.captureDao())
        // v27 — auto-delete recycle-bin captures that passed their retention
        // window (runs again whenever the recycle bin opens).
        lifecycleScope.launch { RecycleBinExpiry.purgeExpired(this@MainActivity) }

        AppPreferences.initThemeMode(this)
        // Load the persisted explore-session flow state (active session +
        // recently explored/unexplored lists) before any screen reads it.
        ExploreSessionStore.seed(this)
        // v29 — load per-topic reading/watching progress before any screen
        // (reveal / Cabinet / detail) reads it.
        TopicProgressStore.seed(this)
        // v29 — prewarm the topic catalog in the background so the Topic
        // Database opens with ZERO loading: the prebuilt index (search keys
        // + years precomputed at build time) is parsed once here, and the
        // per-category pools land in the loader cache while the user does
        // anything else. Both are cached, so screens read them instantly.
        lifecycleScope.launch {
            runCatching { TopicJsonLoader.loadIndex() }
            runCatching { TopicJsonLoader.preloadAll() }
        }
        // v53 — update notifier on app start: a toast whenever a check finds
        // a newer release, and a notification ONCE per version (never
        // re-notified for the same tag). Offline/failures are silent.
        lifecycleScope.launch {
            runCatching { UpdateChecker.notifyIfUpdateAvailable(this@MainActivity) }
        }
        // v27 — watch for device screenshots while a session (or a handed-off
        // write package) is live, so the user's own shots auto-join the
        // session. Permission-gated internally; the bubble's own capture
        // button works without it.
        com.curio.app.infrastructure.DeviceScreenshotWatcher.start(this)
        // Load the persisted quests/levels state (XP, journey, daily quests,
        // achievements) before any screen reads it.
        CurioQuests.seed(this)
        // v8.14 — the pet wakes on its own in the morning (and stays tucked
        // in at night); afternoon/evening launches keep asleep-until-tapped.
        CurioPet.wakeForMorning()
        if (AppPreferences.isReminderEnabled(this)) {
            com.curio.app.data.DailyReminderScheduler.schedule(
                this,
                AppPreferences.getReminderHour(this)
            )
        }
        setContent {
            CurioTheme {
                CurioNavHost()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Topic catalogs are immutable and reloadable. Release the process
        // cache when Android reports real running-low pressure instead of
        // retaining every parsed topic object through the next screen.
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            TopicJsonLoader.clearCache()
        }
    }

    override fun onResume() {
        super.onResume()
        // v29 — re-seed progress on every resume: a killed-in-background
        // process could otherwise show stale progress until a full restart
        // (the vanish-then-reappear-after-restart symptom). The read is a
        // tiny prefs load and the in-memory state is always newer-or-equal.
        TopicProgressStore.seed(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Warm-start path: the activity was already running, so pending
        // notification targets arrive here instead of onCreate.
        PendingEntryOpen.capture(intent)
        PendingSpinOpen.capture(intent)
    }
}
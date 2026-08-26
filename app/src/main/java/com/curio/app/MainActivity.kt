package com.curio.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureRepository
import com.curio.app.data.CurioBackupManager
import com.curio.app.data.CurioDatabase
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.RecycleBinExpiry
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.VoskModels
import com.curio.app.data.TopicProgressStore
import com.curio.app.data.UpdateChecker
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.infrastructure.ExploreSessionService
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

    private companion object {
        // Auto-backup fallback cadence — overridden per-launch by the
        // user's chosen frequency (Settings → Backup & restore).
        const val AUTO_BACKUP_INTERVAL_MILLIS = 24L * 60 * 60 * 1000
    }

    /**
     * v227c — run the auto backup when it's DUE: enabled + destination
     * saved + the user-chosen frequency elapsed. Called from onCreate AND
     * onResume so a long-lived process still backs up on schedule (the old
     * onCreate-only hook could go days without firing while Android kept
     * the process warm). The due-date gate makes repeat calls no-ops.
     */
    private fun runAutoBackupIfDue() {
        if (!AppPreferences.isAutoBackupEnabled(this)) return
        val autoUri = AppPreferences.getAutoBackupUri(this)
        if (autoUri.isBlank()) return
        val interval = AppPreferences.getAutoBackupFrequencyDays(this)
            .coerceAtLeast(1) * AUTO_BACKUP_INTERVAL_MILLIS
        val lastAuto = AppPreferences.getAutoBackupLastAtMillis(this)
        if (lastAuto != 0L && System.currentTimeMillis() - lastAuto < interval) return
        lifecycleScope.launch {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    CurioBackupManager.export(this@MainActivity, Uri.parse(autoUri))
                    AppPreferences.setAutoBackupLastAtMillis(this@MainActivity, System.currentTimeMillis())
                }
            }
        }
    }

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

        // v294 — Initialize Room topic repository (populates from JSON on first launch).
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.curio.app.data.TopicRepository.init(this@MainActivity)
        }

        // Initialize crash reporter before anything else
        CurioCrashReporter.init(this)

        // Initialize Room database and repository singleton
        val db = CurioDatabase.getInstance(this)
        CurioRepositoryHolder.init(db.captureDao())
        // v27 — auto-delete recycle-bin captures that passed their retention
        // window (runs again whenever the recycle bin opens).
        lifecycleScope.launch { RecycleBinExpiry.purgeExpired(this@MainActivity) }

        // v158 — the Full server-grade Vosk models were removed from the
        // catalog (they lagged and crashed phones): prune any already-
        // installed one and clear a stale selection so transcription can
        // never load a removed model again.
        VoskModels.pruneRemovedModels(this)

        AppPreferences.initThemeMode(this)
        // Load the persisted explore-session flow state (active session +
        // recently explored/unexplored lists) before any screen reads it.
        ExploreSessionStore.seed(this)
        // v29 — load per-topic reading/watching progress before any screen
        // (reveal / Cabinet / detail) reads it.
        TopicProgressStore.seed(this)
        // v29 — prewarm the topic catalog in the background so the Topic
        // Database opens with ZERO loading: the merged index (search keys +
        // years; v174f builds it at runtime from the per-category pools —
        // the 23MB prebuilt asset no longer ships) is built once here, and
        // the per-category pools land in the loader cache while the user
        // does anything else. Both are cached, so screens read them instantly.
        // v55 — NonCancellable: a rotation (activity destroy) mid-warmup
        // used to cancel loadIndex and restart the whole parse; the warm-up
        // now runs to completion regardless (parses are bounded by the
        // loader's gate, so it can't hog the CPU).
        lifecycleScope.launch {
            withContext(kotlinx.coroutines.NonCancellable) {
                runCatching { TopicJsonLoader.loadIndex() }
                runCatching { TopicJsonLoader.preloadAll() }
            }
        }
        // v53 — update notifier on app start: a toast whenever a check finds
        // a newer release, and a notification ONCE per version (never
        // re-notified for the same tag). Offline/failures are silent.
        // OPT-IN: Curio is offline-first, so the background check (which
        // costs data every launch) only runs when the Updates page toggle is
        // ON. The manual check on the Updates page always works.
        if (AppPreferences.isUpdateCheckerEnabled(this)) {
            lifecycleScope.launch {
                runCatching { UpdateChecker.notifyIfUpdateAvailable(this@MainActivity) }
            }
        }
        // Auto backup: when enabled with a saved destination, write a
        // backup there automatically — throttled to the user's chosen
        // frequency so a frequent launch never spams the drive. Runs in the
        // background; failures are silent (the manual "Back up now" path
        // remains authoritative).
        runAutoBackupIfDue()
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
        // v55 — the shed is TIERED inside the loader (pools at RUNNING_LOW,
        // the heavy 16k-entry index only at RUNNING_CRITICAL+) so a trim
        // never triggers a full re-parse storm that lags + heats the device.
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            TopicJsonLoader.shedForMemory(level)
        }
    }

    override fun onResume() {
        super.onResume()
        // v227c — warm starts re-check the auto-backup schedule too (a
        // process kept alive by Android would otherwise skip its due date).
        runAutoBackupIfDue()
        // v29 — re-seed progress on every resume: a killed-in-background
        // process could otherwise show stale progress until a full restart
        // (the vanish-then-reappear-after-restart symptom). The read is a
        // tiny prefs load and the in-memory state is always newer-or-equal.
        TopicProgressStore.seed(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // v226 — closing the app auto-pauses the running explore session so
        // the timer stops counting while Curio is closed (the paused state
        // is what makes it resumable from Home / the return prompt instead
        // of silently burning minutes in a drawer). Configuration changes
        // (rotation, fold, dark-mode flip) are excluded — isFinishing is
        // false on a recents-swipe destroy too, so this also covers the
        // swipe-the-app-away path.
        if (!isChangingConfigurations) {
            val session = ExploreSessionStore.getActiveSession(this)
            if (session != null && !session.paused) {
                ExploreSessionStore.pauseSession(this)
                // Re-arm the service so the shade flips to its Paused
                // readout instead of ticking a chronometer that the frozen
                // session no longer agrees with.
                ExploreSessionStore.getActiveSession(this)?.let { paused ->
                    if (AppPreferences.exploreServiceShouldRun(this)) {
                        ExploreSessionService.start(this, paused)
                    }
                }
            }
        }
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
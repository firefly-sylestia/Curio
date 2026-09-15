package com.curio.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.curio.app.data.AppPreferences
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
import com.curio.app.features.community.SocialNotificationWatcher
import com.curio.app.features.create.BookCreateScreen
import com.curio.app.features.create.CreateEntryLauncher
import com.curio.app.features.create.PersonalJournalScreen
import com.curio.app.navigation.CurioNavHost
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.PendingCommunityOpen
import com.curio.app.navigation.PendingDirectMessageOpen
import com.curio.app.navigation.PendingEntryOpen
import com.curio.app.navigation.PendingSpinOpen
import com.curio.app.ui.theme.CurioTheme
import com.curio.app.ui.theme.CurioThemeTransitionHost

class MainActivity : ComponentActivity() {
    private companion object { const val AUTO_BACKUP_INTERVAL_MILLIS = 24L * 60 * 60 * 1000 }

    private fun runAutoBackupIfDue() {
        if (!AppPreferences.isAutoBackupEnabled(this)) return
        val autoUri = AppPreferences.getAutoBackupUri(this)
        if (autoUri.isBlank()) return
        val interval = AppPreferences.getAutoBackupFrequencyDays(this).coerceAtLeast(1) * AUTO_BACKUP_INTERVAL_MILLIS
        val lastAuto = AppPreferences.getAutoBackupLastAtMillis(this)
        if (lastAuto != 0L && System.currentTimeMillis() - lastAuto < interval) return
        lifecycleScope.launch { withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { CurioBackupManager.export(this@MainActivity, Uri.parse(autoUri)); AppPreferences.setAutoBackupLastAtMillis(this@MainActivity, System.currentTimeMillis()) } } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(); super.onCreate(savedInstanceState)
        if (savedInstanceState == null) { PendingEntryOpen.capture(intent); PendingSpinOpen.capture(intent); PendingDirectMessageOpen.capture(intent); PendingCommunityOpen.capture(intent) }
        TopicJsonLoader.install(this)
        runCatching {
            coil.Coil.setImageLoader(coil.ImageLoader.Builder(this).crossfade(true).memoryCache { coil.memory.MemoryCache.Builder(this).maxSizePercent(.22).build() }.diskCache { coil.disk.DiskCache.Builder().directory(java.io.File(cacheDir, "curio_image_cache")).maxSizePercent(.03).build() }.components { add(coil.decode.SvgDecoder.Factory()) }.build())
        }
        if (!com.curio.app.data.TopicRepository.isInitialized()) lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) { com.curio.app.data.TopicRepository.init(this@MainActivity) }
        CurioCrashReporter.init(this)
        val db = CurioDatabase.getInstance(this)
        CurioRepositoryHolder.init(db.captureDao(), db.cachedTopicDao())
        lifecycleScope.launch { RecycleBinExpiry.purgeExpired(this@MainActivity) }
        VoskModels.pruneRemovedModels(this)
        AppPreferences.initThemeMode(this); ExploreSessionStore.seed(this); TopicProgressStore.seed(this)
        lifecycleScope.launch { withContext(kotlinx.coroutines.Dispatchers.IO) { com.curio.app.data.TopicRepository.init(this@MainActivity) }; withContext(kotlinx.coroutines.NonCancellable) { runCatching { TopicJsonLoader.loadIndex() }; runCatching { TopicJsonLoader.preloadAll() } } }
        if (AppPreferences.isUpdateCheckerEnabled(this)) lifecycleScope.launch { runCatching { UpdateChecker.notifyIfUpdateAvailable(this@MainActivity) } }
        runAutoBackupIfDue(); CurioQuests.seed(this); CurioPet.wakeForMorning()
        if (AppPreferences.isReminderEnabled(this)) com.curio.app.data.DailyReminderScheduler.schedule(this, AppPreferences.getReminderHour(this), AppPreferences.getReminderMinute(this))
        setContent {
            CurioTheme {
                CurioThemeTransitionHost {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val onHome = currentRoute == CurioRoutes.HOME
                    var showCreateButton by remember(currentRoute) { mutableStateOf(true) }
                    var creationWorkspace by remember { mutableStateOf<String?>(null) }
                    val createScrollConnection = remember(onHome) { object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (!onHome || creationWorkspace != null) return Offset.Zero
                            when { available.y < -2f -> showCreateButton = false; available.y > 2f -> showCreateButton = true }
                            return Offset.Zero
                        }
                    } }
                    Box(Modifier.fillMaxSize().nestedScroll(createScrollConnection)) {
                        CurioNavHost(navController = navController)
                        AnimatedVisibility(visible = onHome && showCreateButton && creationWorkspace == null, enter = slideInVertically(tween(220, easing = FastOutSlowInEasing), initialOffsetY = { it }), exit = slideOutVertically(tween(180, easing = FastOutSlowInEasing), targetOffsetY = { it }), modifier = Modifier.align(Alignment.BottomCenter).fillMaxSize()) {
                            CreateEntryLauncher(onJournal = { creationWorkspace = "journal" }, onBook = { creationWorkspace = "book" }, onQuickNote = { navController.navigate(CurioRoutes.PICKER) { launchSingleTop = true } })
                        }
                        AnimatedVisibility(visible = creationWorkspace != null, enter = fadeIn(tween(220, easing = FastOutSlowInEasing)), exit = fadeOut(tween(180, easing = FastOutSlowInEasing)), modifier = Modifier.fillMaxSize()) {
                            when (creationWorkspace) {
                                "journal" -> PersonalJournalScreen(onClose = { creationWorkspace = null }, onSaved = { creationWorkspace = null })
                                "book" -> BookCreateScreen(onClose = { creationWorkspace = null }, onCreated = { creationWorkspace = null })
                            }
                        }
                    }
                }
                SocialNotificationWatcher()
            }
        }
    }

    override fun onTrimMemory(level: Int) { super.onTrimMemory(level); if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) TopicJsonLoader.shedForMemory(level) }
    override fun onResume() { super.onResume(); runAutoBackupIfDue(); TopicProgressStore.seed(this) }
    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            val session = ExploreSessionStore.getActiveSession(this)
            if (session != null && !session.paused) { ExploreSessionStore.pauseSession(this); ExploreSessionStore.getActiveSession(this)?.let { paused -> if (AppPreferences.exploreServiceShouldRun(this)) ExploreSessionService.start(this, paused) } }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); PendingEntryOpen.capture(intent); PendingSpinOpen.capture(intent); PendingDirectMessageOpen.capture(intent); PendingCommunityOpen.capture(intent) }
}

package com.curio.app.features.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioQuality
import com.curio.app.data.AudioQualitySettings
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.SearchEngine
import com.curio.app.features.onboarding.CurioOnboardingState
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsInfoRow
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioUpdateCheckRow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.formatHour
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/** Settings destination selected from the compact hub. */
enum class SettingsPage(val title: String, val subtitle: String) {
    APPEARANCE("Appearance", "Theme, tint, and color mood"),
    NOTIFICATIONS("Notifications", "Reminders and explore controls"),
    RECORDING("Recording", "Voice-note quality and dictation"),
    DATA("Backup & restore", "Keep your captures safe"),
    ABOUT("About Curio", "Help and app details")
}

@Composable
fun SettingsSectionScreen(navController: NavController, page: SettingsPage) {
    // ── Deep-search highlight (v8.0) — the hub's search hands over the exact
    // row key when a result points inside this sub-section; scroll to it and
    // pulse it once on entry.
    val highlightKey = remember { SettingsHighlightTarget.takeIf { it.page == page }?.rowKey }
    LaunchedEffect(Unit) {
        SettingsHighlightTarget.page = null
        SettingsHighlightTarget.rowKey = null
    }
    val listState = rememberLazyListState()
    LaunchedEffect(highlightKey) {
        if (highlightKey != null) listState.scrollToItem(1)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs behind the content
        // (wildcard sparkle leads; settings is category-neutral).
        // v7.76 — the flat rows below the hero sit directly on this
        // backdrop, so the glyphs drop to a faint whisper and the text,
        // headers and chips always read first.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        // The hero banner runs up BEHIND the status bar (the header applies
        // its own status-bar inset for the back pill) — Profile/Home style.
        // The hero is drawn LAST (on top of the scroll content): the rows
        // scroll UP and disappear behind the ragged tear instead of clipping
        // at a straight line.
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight + 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CurioSectionLabel(page.title) }
            item {
                when (page) {
                    SettingsPage.APPEARANCE -> AppearanceSection(highlightKey)
                    SettingsPage.NOTIFICATIONS -> NotificationsSection(highlightKey)
                    SettingsPage.RECORDING -> RecordingSection(highlightKey)
                    SettingsPage.DATA -> DataSection(navController, highlightKey)
                    SettingsPage.ABOUT -> AboutSection(navController, highlightKey)
                }
            }
        }
        // Side scroll indicator — thin overlay knob, grows on touch.
        CurioVerticalScrollIndicator(
            state = listState.scrollIndicatorState,
            onScrollBy = { listState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = SettingsHeroTotalHeight + 8.dp, bottom = 16.dp)
        )
        // Drawn on top of the scroll content — rows slide under the ragged
        // tear as they scroll up.
        SettingsHeroHeader(title = page.title, subtitle = page.subtitle, onBack = { navController.popBackStack() })
    }
}

@Composable
private fun AppearanceSection(highlightKey: String? = null) {
    val context = LocalContext.current
    val themeStyles = listOf(AppPreferences.THEME_STYLE_DEFAULT, AppPreferences.THEME_STYLE_AMOLED, AppPreferences.THEME_STYLE_MATERIAL)
    val themes = listOf(AppPreferences.THEME_LIGHT, AppPreferences.THEME_DARK, AppPreferences.THEME_SYSTEM)
    val themeStyle = AppPreferences.themeStyleState
    val themeMode = AppPreferences.themeModeState
    val styleIndex = themeStyles.indexOf(themeStyle).coerceAtLeast(0)
    val themeIndex = themes.indexOf(themeMode).coerceAtLeast(0)
    Column(modifier = Modifier.fillMaxWidth()) {
        CurioCardHeader(CurioIcons.AutoAwesome, "Visual language", "Small choices shape every page")
        SettingsRowPulse(highlightKey == "appearance-style") {
            // The Material style stays greyed out until it ships — the option
            // is visible so users know it's coming, but can't be selected.
            CompactSegmentedRow(
                "Theme style",
                listOf("Curio", "AMOLED", "Material"),
                styleIndex,
                disabledIndices = setOf(2),
                disabledHint = "Material theme · coming soon"
            ) { index ->
                AppPreferences.setThemeStyle(context, themeStyles[index])
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "appearance-theme") {
            CompactSegmentedRow("Theme", listOf("Light", "Dark", "System"), themeIndex, enabled = themeStyle != AppPreferences.THEME_STYLE_AMOLED) { index ->
                AppPreferences.setThemeMode(context, themes[index])
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "appearance-tint") {
            CompactSwitchRow("Category tint", "Colorful page backgrounds", AppPreferences.tintWashEffective(), themeStyle == AppPreferences.THEME_STYLE_DEFAULT) {
                AppPreferences.setTintWashEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "appearance-pastel") {
            CompactSwitchRow("Pastel colors", "Soft category accents and page tints", AppPreferences.pastelColorsState) {
                AppPreferences.setPastelColorsEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "appearance-entry") {
            CompactSwitchRow("Entry date & mood", "Date, mood, and attachments on saved entries", AppPreferences.entryMetaEnabledState) {
                AppPreferences.setEntryMetaEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        // v8.5 — the Curio pet companion (spec §10): pixel pet + rule-based
        // dialogue + passport/discovery on Quests and Home. Default ON.
        SettingsRowPulse(highlightKey == "appearance-pet") {
            CompactSwitchRow("Curie", "Pixel companion that grows with your XP", AppPreferences.petEnabledState) {
                AppPreferences.setPetEnabled(context, it)
            }
        }
        // v8.8 — the floating pet companion: wanders on every screen, can be
        // dragged anywhere, and naps back into its flower bed. Default ON;
        // turning it off keeps the pet at home in the bed.
        SettingsRowPulse(highlightKey == "appearance-floating-pet") {
            CompactSwitchRow(
                "Floating pet",
                "Wanders, follows your finger, naps in its flower bed",
                AppPreferences.floatingPetEnabledState
            ) {
                AppPreferences.setFloatingPetEnabled(context, it)
            }
        }
        // v8.43 — the pet's learning brain (CurioPetBrain): observes real
        // activity, builds a personality, and develops its own catchphrases
        // over time. Default ON; off = classic rule-based lines only.
        SettingsRowPulse(highlightKey == "appearance-pet-brain") {
            CompactSwitchRow(
                "Pet brain",
                "The pet learns your habits and grows its own personality",
                AppPreferences.petBrainEnabledState
            ) {
                AppPreferences.setPetBrainEnabled(context, it)
            }
        }
        // v16 — how chatty the pet is. Cozy is the default; Talkative opens
        // the bubble more often, Quiet says less.
        SettingsRowPulse(highlightKey == "appearance-pet-chatter") {
            CompactSegmentedRow(
                "Pet chatter",
                listOf("Quiet", "Cozy", "Talkative"),
                when (AppPreferences.petChatterState) {
                    "quiet" -> 0
                    "talkative" -> 2
                    else -> 1
                }
            ) { index ->
                AppPreferences.setPetChatter(
                    context,
                    when (index) {
                        0 -> "quiet"
                        2 -> "talkative"
                        else -> "cozy"
                    }
                )
            }
        }
        // v16 — how often the pet starts its games on its own: Relaxed,
        // Normal (default), or Eager.
        SettingsRowPulse(highlightKey == "appearance-pet-games") {
            CompactSegmentedRow(
                "Pet games",
                listOf("Relaxed", "Normal", "Eager"),
                when (AppPreferences.petGameFrequencyState) {
                    "relaxed" -> 0
                    "eager" -> 2
                    else -> 1
                }
            ) { index ->
                AppPreferences.setPetGameFrequency(
                    context,
                    when (index) {
                        0 -> "relaxed"
                        2 -> "eager"
                        else -> "normal"
                    }
                )
            }
        }
        CurioSettingsDivider()
        // v8.16 — whether a landed topic's reveal opens itself as soon as
        // the deck settles. Default OFF: the deck just lands and the front
        // card stays tappable (no reveal page, no open-it prompt).
        SettingsRowPulse(highlightKey == "appearance-auto-open") {
            CompactSwitchRow(
                "Auto-open landed topic",
                "Open the topic reveal as soon as the deck lands",
                AppPreferences.autoOpenRevealState
            ) {
                AppPreferences.setAutoOpenReveal(context, it)
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "appearance-reaction-lines") {
            CompactSwitchRow(
                "Custom reaction lines",
                "Let Curie speak your saved lines for each event",
                AppPreferences.customReactionLinesState
            ) {
                AppPreferences.setCustomReactionLinesEnabled(context, it)
            }
        }
    }
}

@Composable
private fun NotificationsSection(highlightKey: String? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reminderHour by remember { mutableStateOf(AppPreferences.getReminderHour(context)) }
    var overlayEnabled by remember { mutableStateOf(AppPreferences.overlayBubbleEnabledState) }
    // v8.1 — live "Display over other apps" grant state + a flag that the
    // system special-access page was opened (so ON_RESUME knows whether a
    // grant — or a decline — just happened).
    var overlayUsable by remember { mutableStateOf(AppPreferences.overlayActuallyUsable(context)) }
    var overlaySettingsOpened by remember { mutableStateOf(false) }
    var liveNotificationsEnabled by remember { mutableStateOf(AppPreferences.liveNotificationsEnabledState) }
    var exploreSessionsEnabled by remember { mutableStateOf(AppPreferences.exploreSessionsEnabledState) }
    // v19 — the explore search-engine picker (which engine the "Explore in
    // browser" button opens).
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    val permissionMissing = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reminderHour = AppPreferences.getReminderHour(context)
                overlayEnabled = AppPreferences.isOverlayBubbleEnabled(context)
                overlayUsable = AppPreferences.overlayActuallyUsable(context)
                liveNotificationsEnabled = AppPreferences.isLiveNotificationsEnabled(context)
                exploreSessionsEnabled = AppPreferences.isExploreSessionsEnabled(context)
                // v8.1 — returning from the system overlay-settings page: a
                // grant re-enables the bubble and clears the declined flag;
                // coming back without granting records the "no" so automatic
                // prompts stop (Settings toggles still work anytime).
                if (overlaySettingsOpened) {
                    overlaySettingsOpened = false
                    overlayUsable = AppPreferences.overlayActuallyUsable(context)
                    if (overlayUsable) {
                        AppPreferences.setOverlayAskDeclined(context, false)
                        AppPreferences.setOverlayBubbleEnabled(context, true)
                        overlayEnabled = true
                    } else {
                        AppPreferences.setOverlayAskDeclined(context, true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var pendingEnable by remember { mutableStateOf<(() -> Unit)?>(null) }
    // The result callback can fire while the system page is STILL open (the
    // permission not yet granted), so the grant/decline decision lives in
    // the ON_RESUME observer below, guarded by [overlaySettingsOpened].
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* no-op — ON_RESUME is the source of truth */ }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingEnable?.invoke()
        pendingEnable = null
    }
    fun enableNotifications(action: () -> Unit) {
        if (!permissionMissing) action() else {
            pendingEnable = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        CurioCardHeader(CurioIcons.Notifications, "Notifications", "Quiet nudges, when you want them")
        SettingsRowPulse(highlightKey == "notif-reminder") {
            CompactSwitchRow("Daily shuffle reminder", if (AppPreferences.reminderEnabledState) "Every day at ${formatHour(AppPreferences.getReminderHour(context))}" else "Off", AppPreferences.reminderEnabledState) { enabled ->
                if (enabled) enableNotifications { AppPreferences.setReminderEnabled(context, true) } else AppPreferences.setReminderEnabled(context, false)
            }
        }
        if (AppPreferences.reminderEnabledState) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                items(listOf(9, 12, 15, 18, 21)) { hour ->
                    val selected = hour == reminderHour
                    // AMOLED: the selected chip was the coral primary; it
                    // swaps to pitch-black glass (white text + hairline rim) to
                    // match the switches and the app's AMOLED control language.
                    val isAmoled = AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED
                    Surface(
                        onClick = {
                            reminderHour = hour
                            AppPreferences.setReminderHour(context, hour)
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        color = when {
                            selected && isAmoled -> Color.Black
                            selected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = when {
                            selected && isAmoled -> MaterialTheme.colorScheme.onSurface
                            selected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        border = if (selected && isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)) else null,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            formatHour(hour),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "notif-sessions") {
            CompactSwitchRow("Explore sessions", "Timer, reminder, and done prompt", exploreSessionsEnabled) {
                exploreSessionsEnabled = it
                AppPreferences.setExploreSessionsEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        // v19 — which search engine the "Explore in browser" button opens.
        // A row that opens the engine picker; the subtitle shows the choice.
        SettingsRowPulse(highlightKey == "notif-search-engine") {
            CurioSettingsRow(
                CurioIcons.Search,
                "Search engine",
                "Explore in browser opens ${SearchEngine.fromId(AppPreferences.searchEngineState).displayName}"
            ) {
                showSearchEngineDialog = true
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "notif-live") {
            CompactSwitchRow("Live explore notification", "Ongoing timer with pause and stop", liveNotificationsEnabled) { enabled ->
                if (enabled) enableNotifications {
                    liveNotificationsEnabled = true
                    AppPreferences.setLiveNotificationsEnabled(context, true)
                } else {
                    liveNotificationsEnabled = false
                    AppPreferences.setLiveNotificationsEnabled(context, false)
                }
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "notif-bubble") {
            CompactSwitchRow("Floating explore bubble", "Timer bubble over other apps", overlayEnabled) { enabled ->
                if (enabled && !AppPreferences.overlayActuallyUsable(context)) {
                    // Explicit intent — stop suppressing the prompt and
                    // remember the settings trip so the return decides.
                    AppPreferences.setOverlayAskDeclined(context, false)
                    val launched = runCatching {
                        overlaySettingsOpened = true
                        overlaySettingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                    if (launched.isFailure) overlaySettingsOpened = false
                } else {
                    overlayEnabled = enabled
                    AppPreferences.setOverlayBubbleEnabled(context, enabled)
                }
            }
        }
        CurioSettingsDivider()
        // v8.1 — the permission toggle itself: shows the live grant state
        // and opens the system special-access page (on or off — the grant
        // can't be flipped from the app). The switch re-reads reality on
        // return, so granting here re-enables the bubble and future prompts.
        SettingsRowPulse(highlightKey == "notif-overlay") {
            CompactSwitchRow(
                "Display over other apps",
                if (overlayUsable) "Granted. The bubble can float over other apps"
                else "System permission for the floating bubble",
                overlayUsable
            ) { enabled ->
                // An explicit toggle is a fresh intent: it always opens the
                // system page (grant OR revoke) and clears the declined flag.
                AppPreferences.setOverlayAskDeclined(context, false)
                val launched = runCatching {
                    overlaySettingsOpened = true
                    overlaySettingsLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
                if (launched.isFailure) overlaySettingsOpened = false
            }
        }
    }
    if (showSearchEngineDialog) {
        SearchEngineDialog(
            current = SearchEngine.fromId(AppPreferences.searchEngineState),
            onDismiss = { showSearchEngineDialog = false },
            onSelected = { engine ->
                AppPreferences.setSearchEngine(context, engine)
                showSearchEngineDialog = false
            }
        )
    }
}

@Composable
private fun RecordingSection(highlightKey: String? = null) {
    val context = LocalContext.current
    var quality by remember { mutableStateOf(AudioQualitySettings.get(context)) }
    var showQualityDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        CurioCardHeader(CurioIcons.Mic, "Recording", "Voice notes that sound like you")
        SettingsRowPulse(highlightKey == "recording-quality") {
            CurioSettingsRow(CurioIcons.Mic, "Audio quality", quality.label) {
                showQualityDialog = true
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "recording-voice") {
            CompactSwitchRow("Voice-to-text", "Dictation buttons on voice-note fields", AppPreferences.voiceToTextEnabledState) {
                AppPreferences.setVoiceToTextEnabled(context, it)
            }
        }
    }
    if (showQualityDialog) {
        AudioQualityDialog(
            currentQuality = quality,
            onDismiss = { showQualityDialog = false },
            onSelected = {
                quality = it
                AudioQualitySettings.set(context, it)
                showQualityDialog = false
            }
        )
    }
}

@Composable
private fun DataSection(navController: NavController, highlightKey: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CurioCardHeader(CurioIcons.Backup, "Backup & restore", "Your captures stay yours")
        SettingsRowPulse(highlightKey == "data-tools") {
            CurioSettingsRow(CurioIcons.Backup, "Open backup tools", "Export, restore, or import FieldMind data") {
                navController.navigate(CurioRoutes.SETTINGS_DATA) { launchSingleTop = true }
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "data-workspace") {
            CurioSettingsInfoRow(CurioIcons.History, "Backup workspace", "Full backup tools remain in the data workspace")
        }
    }
}

@Composable
private fun AboutSection(navController: NavController, highlightKey: String? = null) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        CurioCardHeader(CurioIcons.Info, "About Curio", "Help and app details")
        SettingsRowPulse(highlightKey == "about-intro") {
            CurioSettingsRow(CurioIcons.Replay, "Replay intro", "See the welcome screens again") {
                CurioOnboardingState.reset(context)
                navController.navigate(CurioRoutes.ONBOARDING) { launchSingleTop = true }
            }
        }
        CurioSettingsDivider()
        // Version straight from the build — VERSION_NAME is the release tag
        // this APK was built from (e.g. "1.0.0"), VERSION_CODE is the
        // per-build number, so the readout is always accurate.
        SettingsRowPulse(highlightKey == "about-version") {
            CurioSettingsInfoRow(
                CurioIcons.Info,
                "Version",
                "${com.curio.app.BuildConfig.VERSION_NAME} · build ${com.curio.app.BuildConfig.VERSION_CODE}"
            )
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "about-update") {
            CurioUpdateCheckRow()
        }
    }
}

@Composable
private fun CompactSegmentedRow(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    enabled: Boolean = true,
    disabledIndices: Set<Int> = emptySet(),
    disabledHint: String? = null,
    onSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    enabled = enabled && index !in disabledIndices,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size)
                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
            }
        }
        if (disabledHint != null && disabledIndices.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            ) {
                CurioIcon(CurioIcons.Schedule, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                Text(
                    disabledHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactSwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    // AMOLED: the scheme primary is the coral brand color, so the ON
    // track lit pink. Pitch-black glass instead (black track, white knob,
    // hairline white rim) — the app's AMOLED control language.
    val isAmoled = AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = if (isAmoled) {
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                    checkedTrackColor = Color.Black,
                    checkedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            } else {
                SwitchDefaults.colors()
            }
        )
    }
}

/**
 * Deep-search highlight pulse — wraps the row the hub's search jumped to.
 * On entry the matched row flashes a soft primary wash that fades out over
 * ~1.4s, so the user sees exactly which setting the search found.
 */
@Composable
private fun SettingsRowPulse(
    active: Boolean,
    content: @Composable () -> Unit
) {
    val pulseAlpha = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            pulseAlpha.snapTo(0.55f)
            pulseAlpha.animateTo(0f, tween(1400, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha.value * 0.35f)
            )
            .padding(horizontal = 2.dp)
    ) {
        content()
    }
}

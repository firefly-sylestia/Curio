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
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsInfoRow
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.formatHour
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/** Settings destination selected from the compact hub. */
enum class SettingsPage(val title: String, val subtitle: String) {
    APPEARANCE("Appearance", "Theme, tint, and color mood"),
    // v26 — Preferences: the behavioral settings that aren't about how the
    // app LOOKS — search engine, explore sessions and the floating bubble,
    // the pet's chatter/games personality, and (v27) every notification
    // control: the daily shuffle reminder + hour chips and the explore
    // dialog's bubble opt-in row (the Notifications section is gone).
    PREFERENCES("Preferences", "Search, explore, and pet behavior"),
    RECORDING("Recording", "Voice-note quality and dictation"),
    DATA("Backup & restore", "Keep your captures safe")
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
                    SettingsPage.PREFERENCES -> PreferencesSection(highlightKey)
                    SettingsPage.RECORDING -> RecordingSection(highlightKey)
                    SettingsPage.DATA -> DataSection(navController, highlightKey)
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
        // v27l — optional sky-azure hero variant for the shared torn banner
        // (Home / Profile / Settings / Cabinet). Default OFF — the rose
        // stays.
        SettingsRowPulse(highlightKey == "appearance-hero") {
            CompactSwitchRow("Sky azure hero", "The shared hero banner wears airy azure instead of rose", AppPreferences.heroBlueState) {
                AppPreferences.setHeroBlueEnabled(context, it)
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
        // v26 — pet chatter + pet games moved to Preferences (the pet's
        // behavior personality is a preference, not a look).
        // v23 — auto-open landed topic is always on now (its toggle was
        // removed) and custom reaction lines are permanently off (their
        // editor is no longer reachable, so the toggle was removed too).
    }
}

/**
 * Preferences — the behavioral settings that aren't about how the app LOOKS
 * (Appearance) or when it REMINDS you (Notifications): which search engine
 * Explore opens, how explore sessions behave (timer / live notification /
 * floating bubble), and the pet's personality (chatter + games). v26 — the
 * rows moved here from Notifications and Appearance so Preferences is the
 * home for "how Curio behaves" choices.
 */
@Composable
private fun PreferencesSection(highlightKey: String? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var overlayEnabled by remember { mutableStateOf(AppPreferences.overlayBubbleEnabledState) }
    // v8.1 — live "Display over other apps" grant state + a flag that the
    // system special-access page was opened (so ON_RESUME knows whether a
    // grant — or a decline — just happened).
    var overlayUsable by remember { mutableStateOf(AppPreferences.overlayActuallyUsable(context)) }
    var overlaySettingsOpened by remember { mutableStateOf(false) }
    var liveNotificationsEnabled by remember { mutableStateOf(AppPreferences.liveNotificationsEnabledState) }
    var exploreSessionsEnabled by remember { mutableStateOf(AppPreferences.exploreSessionsEnabledState) }
    // v27 — the daily shuffle reminder + its hour chips moved in from the
    // removed Notifications section.
    var reminderHour by remember { mutableStateOf(AppPreferences.getReminderHour(context)) }
    var showBubbleOptInDialogEnabled by remember { mutableStateOf(AppPreferences.showBubbleOptInDialogState) }
    // v19 — the explore search-engine picker (which engine the "Explore in
    // browser" button opens).
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    // v26 — shared notification-permission gate (live-notification row).
    val enableNotifications = rememberNotificationPermissionGate()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayEnabled = AppPreferences.isOverlayBubbleEnabled(context)
                overlayUsable = AppPreferences.overlayActuallyUsable(context)
                liveNotificationsEnabled = AppPreferences.isLiveNotificationsEnabled(context)
                exploreSessionsEnabled = AppPreferences.isExploreSessionsEnabled(context)
                reminderHour = AppPreferences.getReminderHour(context)
                showBubbleOptInDialogEnabled = AppPreferences.isShowBubbleOptInDialog(context)
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
    // The result callback can fire while the system page is STILL open (the
    // permission not yet granted), so the grant/decline decision lives in
    // the ON_RESUME observer below, guarded by [overlaySettingsOpened].
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* no-op — ON_RESUME is the source of truth */ }
    Column(modifier = Modifier.fillMaxWidth()) {
        // v19 — which search engine the "Explore in browser" button opens.
        // A row that opens the engine picker; the subtitle shows the choice.
        SettingsRowPulse(highlightKey == "pref-search-engine") {
            CurioSettingsRow(
                CurioIcons.Search,
                "Search engine",
                "Explore in browser opens ${SearchEngine.fromId(AppPreferences.searchEngineState).displayName}"
            ) {
                showSearchEngineDialog = true
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "pref-sessions") {
            CompactSwitchRow("Explore sessions", "Timer, reminder, and done prompt", exploreSessionsEnabled) {
                exploreSessionsEnabled = it
                AppPreferences.setExploreSessionsEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "pref-live") {
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
        SettingsRowPulse(highlightKey == "pref-bubble") {
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
        SettingsRowPulse(highlightKey == "pref-overlay") {
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
        CurioSettingsDivider()
        // v16 — how chatty the pet is. Cozy is the default; Talkative opens
        // the bubble more often, Quiet says less. Moved from Appearance (v26).
        SettingsRowPulse(highlightKey == "pref-pet-chatter") {
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
        CurioSettingsDivider()
        // v16 — how often the pet starts its games on its own: Relaxed,
        // Normal (default), or Eager. Moved from Appearance (v26).
        SettingsRowPulse(highlightKey == "pref-pet-games") {
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
        // v27 — the daily shuffle reminder + its hour chips moved in from the
        // removed Notifications section: Preferences is now the one home for
        // notification controls.
        SettingsRowPulse(highlightKey == "pref-reminder") {
            CompactSwitchRow("Daily shuffle reminder", if (AppPreferences.reminderEnabledState) "Every day at ${formatHour(AppPreferences.getReminderHour(context))}" else "Off", AppPreferences.reminderEnabledState) { enabled ->
                if (enabled) enableNotifications { AppPreferences.setReminderEnabled(context, true) } else AppPreferences.setReminderEnabled(context, false)
            }
        }
        if (AppPreferences.reminderEnabledState) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                items(listOf(9, 12, 15, 18, 21)) { hour ->
                    val selected = hour == reminderHour
                    // AMOLED: the selected chip swaps to pitch-black glass
                    // (white text + hairline rim) to match the switches and
                    // the app's AMOLED control language.
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
                        // v27q — flat 2dp: selection reads through the solid
                        // primary/black fill, not a raise.
                        shadowElevation = 2.dp,
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
        // v23 — the explore dialog's bubble opt-in row is hidden by default;
        // this re-shows it there as a single-line choice (no subtext).
        SettingsRowPulse(highlightKey == "pref-bubble-dialog") {
            CompactSwitchRow(
                "Explore bubble option in Explore dialog",
                "Show the bubble choice as one line when you start an explore",
                showBubbleOptInDialogEnabled
            ) {
                showBubbleOptInDialogEnabled = it
                AppPreferences.setShowBubbleOptInDialog(context, it)
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
 * The shared POST_NOTIFICATIONS permission gate (v26 — extracted so the
 * Notifications and Preferences sections don't duplicate it).
 *
 * Returns an `enable(action)` function: if the notification permission is
 * already granted (or the OS doesn't require it, pre-Android 13) the action
 * runs immediately; otherwise the system permission dialog is requested
 * first and the action runs only on grant (a declined request drops it —
 * the switch stays off, the user can retry anytime).
 */
@Composable
private fun rememberNotificationPermissionGate(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val permissionMissing = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    var pendingEnable by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingEnable?.invoke()
        pendingEnable = null
    }
    // Deliberately NOT remembered: a fresh lambda each recomposition reads the
    // current [permissionMissing], so a grant on return is seen immediately
    // (remembering would capture the pre-grant value).
    return { action ->
        if (!permissionMissing) action() else {
            pendingEnable = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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

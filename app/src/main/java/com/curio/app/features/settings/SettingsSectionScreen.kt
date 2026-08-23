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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import com.curio.app.ui.components.liquidGlassCapsule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioQuality
import com.curio.app.data.AudioQualitySettings
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.MusicService
import com.curio.app.data.SearchEngine
import com.curio.app.data.VoskModelDownloads
import com.curio.app.data.VoskModels
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsInfoRow
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.CurioCardHeader
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
    RECORDING("Recording", "Voice-note quality, dictation and offline transcription"),
    DATA("Backup & restore", "Keep your captures safe")
}

/**
 * v27t — the rows of one settings page, standalone. Rendered by
 * [SettingsSectionScreen] behind its hero, and reused by the wide two-pane
 * hub ([SettingsHubScreen]) so the tablet Settings screen shows the nav list
 * on the left and the selected page's options on the right.
 */
@Composable
internal fun SettingsPageContent(
    page: SettingsPage,
    navController: NavController,
    highlightKey: String? = null
) {
    // v115 — every sub-page's options sit in the same paper card as the
    // hub rows, so the section screens read as proper settings options
    // instead of transparent rows floating on the backdrop.
    CurioSettingsCard(shadowElevation = 0.dp) {
        when (page) {
            SettingsPage.APPEARANCE -> AppearanceSection(highlightKey)
            SettingsPage.PREFERENCES -> PreferencesSection(highlightKey)
            SettingsPage.RECORDING -> RecordingSection(highlightKey)
            SettingsPage.DATA -> DataSection(navController, highlightKey)
        }
    }
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
    // v255 — the hero is now item 0 of the list; the highlight target is
    // the page-content item that follows the section label.
    LaunchedEffect(highlightKey) {
        if (highlightKey != null) listState.scrollToItem(2)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            // v31 — settings sub-pages wear the same soft page tint as the
            // Settings hub and Profile (a small rose-lean of the background
            // shade, in every theme; the spin-lane wash when Adaptive Hero
            // is on) instead of the plain cream background.
            .background(heroPageBackground(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
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
            // v255 — SCROLLING HERO (the Home/Profile construction): the
            // banner lives INSIDE the list and scrolls away with the page.
            contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // v257 — full-bleed banner (the list's edge padding must not
                // inset the torn hero from the screen sides).
                FullBleedHeroItem(edgePad = wideContentEdgePadding()) {
                    SettingsHeroHeader(title = page.title, subtitle = page.subtitle, onBack = { navController.popBackStack() })
                }
            }
            item { CurioSectionLabel(page.title) }
            item {
                SettingsPageContent(page, navController, highlightKey)
            }
        }
        // Side scroll indicator — thin overlay knob, grows on touch.
        CurioVerticalScrollIndicator(
            state = listState.scrollIndicatorState,
            onScrollBy = { listState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 16.dp)
        )
        // v257 — sticky back pill once the scrolling hero moves up.
        SettingsStickyBackPill(
            onBack = { navController.popBackStack() },
            visible = listState.isPastHero(),
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
private fun AppearanceSection(highlightKey: String? = null) {
    val context = LocalContext.current
    // v81 — the Theme picker (Light / Dark / System) is back: dark mode is
    // the reimagined pitch-black + glow design (no AMOLED/Material styles).
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsRowPulse(highlightKey == "appearance-theme") {
            CompactSegmentedRow(
                "Theme",
                listOf("Light", "Dark", "System"),
                when (AppPreferences.themeModeState) {
                    AppPreferences.THEME_DARK -> 1
                    AppPreferences.THEME_SYSTEM -> 2
                    else -> 0
                }
            ) { index ->
                AppPreferences.setThemeMode(
                    context,
                    when (index) {
                        1 -> AppPreferences.THEME_DARK
                        2 -> AppPreferences.THEME_SYSTEM
                        else -> AppPreferences.THEME_LIGHT
                    }
                )
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "appearance-tint") {
            CompactSwitchRow("Category tint", "Colorful page backgrounds", AppPreferences.tintWashEffective()) {
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
        // v185 — the proper M3 Material theme system (opt-in, default OFF —
        // the current app look is untouched). The v185 "Material guidelines"
        // + "Material chrome" options were removed (user verdict: not good).
        SettingsRowPulse(highlightKey == "appearance-material-theme") {
            CompactSwitchRow(
                "Material theme",
                "Proper Material 3 colors: one primary, neutral surfaces, muted category families",
                AppPreferences.materialThemeState
            ) {
                AppPreferences.setMaterialThemeEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        // v223 — one more Material option: the torn shared heroes
        // follow the Material theme (primaryContainer + its ink)
        // instead of the app-default rose/azure. Only meaningful while
        // Material theme is on — the row greys out otherwise.
        SettingsRowPulse(highlightKey == "appearance-material-hero-tears") {
            CompactSwitchRow(
                "Material hero tears",
                "Torn heroes wear the theme's container color, not rose",
                AppPreferences.materialHeroTearsState,
                enabled = AppPreferences.materialThemeState
            ) {
                AppPreferences.setMaterialHeroTearsEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        // v101 — the dark-mode pill glow is the subtle top-only version by
        // default; the toggle restores the fuller glow for comparison.
        SettingsRowPulse(highlightKey == "appearance-pill-glow") {
            CompactSwitchRow("Subtle pill glow", "Gentler, top-only glow on pills in dark mode", AppPreferences.pillGlowSubtleState) {
                AppPreferences.setPillGlowSubtleEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        // v242 — LIQUID GLASS moved here from Experiments and MERGED with the
        // former separate "In-screen glass" toggle (one switch now drives the
        // nav bar AND every in-screen pill — they all share the same crash-
        // safe local-capture architecture). Clear glass shows inline only
        // while liquid glass is on; the tuning sliders shape the recipe live.
        SettingsRowPulse(highlightKey == "appearance-liquid-glass") {
            CompactSwitchRow("Liquid glass", "Refracting glass on the nav bar and floating pills (real on Android 12+, simulated on older devices)", AppPreferences.liquidGlassPillsState) {
                AppPreferences.setLiquidGlassPillsEnabled(context, it)
            }
        }
        if (AppPreferences.liquidGlassPillsState) {
            CurioSettingsDivider()
            CompactSwitchRow("Clear glass", "Less frost, stronger refraction — glass reads clear like the glow under your finger", AppPreferences.glassClarityState) {
                AppPreferences.setGlassClarityEnabled(context, it)
            }
            CurioSettingsDivider()
            // v252 — the tuning sliders live in a DIALOG whose header is a
            // live glass preview: dragging updates every capsule instantly.
            // (The old Indicator-shadow slider is gone.)
            var showGlassTuning by remember { mutableStateOf(false) }
            CurioSettingsRow(
                CurioIcons.Tune,
                "Tune glass",
                "Reflection, refraction and blur — with a live preview"
            ) { showGlassTuning = true }
            if (showGlassTuning) {
                GlassTuningDialog(onDismiss = { showGlassTuning = false })
            }
        }
        CurioSettingsDivider()
        // v42 — the hero picker is a two-option control (Rose hero / Azure
        // hero), both fully selectable — azure is back and now the DEFAULT.
        // The whole control greys out while Adaptive Hero (below) is active,
        // since the lane then owns the hero color.
        SettingsRowPulse(highlightKey == "appearance-hero") {
            CompactSegmentedRow(
                "Hero",
                listOf("Rose hero", "Azure hero"),
                if (AppPreferences.heroBlueState) 1 else 0,
                enabled = !AppPreferences.heroFollowLaneState
            ) { index ->
                AppPreferences.setHeroBlueEnabled(context, index == 1)
            }
        }
        CurioSettingsDivider()
        // v30 — the shared hero AND its page background follow the category
        // last picked on Spin (the Cabinet's language) instead of the
        // rose/azure. Off by default — rose stays. v31 — renamed
        // "Adaptive Hero".
        SettingsRowPulse(highlightKey == "appearance-hero-lane") {
            CompactSwitchRow("Adaptive Hero", "Shared hero and page take the category you last picked on Spin", AppPreferences.heroFollowLaneState) {
                AppPreferences.setHeroFollowLaneEnabled(context, it)
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
    // v30 — the bubble and the overlay permission are ONE option now; this
    // flags the "Remove overlay permission" trip so the return only refreshes
    // the grant state instead of re-enabling the bubble.
    var overlayRevokeOpened by remember { mutableStateOf(false) }
    var liveNotificationsEnabled by remember { mutableStateOf(AppPreferences.liveNotificationsEnabledState) }
    var exploreSessionsEnabled by remember { mutableStateOf(AppPreferences.exploreSessionsEnabledState) }
    // v27 — the daily shuffle reminder + its hour chips moved in from the
    // removed Notifications section.
    var reminderHour by remember { mutableStateOf(AppPreferences.getReminderHour(context)) }
    var showBubbleOptInDialogEnabled by remember { mutableStateOf(AppPreferences.showBubbleOptInDialogState) }
    // v19 — the explore search-engine picker (which engine the "Explore in
    // browser" button opens).
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    // v27s — the explore music-service picker (which service the "Watch in"
    // button opens for albums, artists & songs).
    var showMusicServiceDialog by remember { mutableStateOf(false) }
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
                    val revoking = overlayRevokeOpened
                    overlayRevokeOpened = false
                    overlayUsable = AppPreferences.overlayActuallyUsable(context)
                    if (revoking) {
                        // v30 — the "Remove overlay permission" row opened the
                        // system page: the bubble stays OFF; only the live
                        // grant state refreshes in the subtitle.
                    } else if (overlayUsable) {
                        // Grant: re-enable the bubble + clear the declined flag.
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
        // v27s — which music service the "Watch in" explore button opens for
        // albums, artists and songs (next to the search-engine picker).
        SettingsRowPulse(highlightKey == "pref-music-service") {
            CurioSettingsRow(
                CurioIcons.MusicNote,
                "Music service",
                "Watch in opens ${MusicService.fromId(AppPreferences.musicServiceState).displayName} for albums, artists & songs"
            ) {
                showMusicServiceDialog = true
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
        // v30 — the floating bubble and the overlay permission are ONE
        // option (the bubble IS the overlay). Enabling without the permission
        // opens the system page to ask for it; the subtitle shows the live
        // grant state. When the bubble is OFF and the permission is still
        // granted, an inline row below offers to remove it.
        SettingsRowPulse(highlightKey == "pref-bubble") {
            CompactSwitchRow(
                "Floating explore bubble",
                if (overlayUsable) "Timer bubble over other apps · overlay permission granted"
                else "Timer bubble over other apps — needs the overlay permission",
                overlayEnabled
            ) { enabled ->
                if (enabled && !AppPreferences.overlayActuallyUsable(context)) {
                    // Enabling without the permission: ask for it — stop
                    // suppressing the prompt, open the system page, and let
                    // the ON_RESUME observer decide grant vs decline.
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
        if (!overlayEnabled && overlayUsable) {
            SettingsRowPulse(highlightKey == "pref-bubble-revoke") {
                CurioSettingsRow(
                    CurioIcons.Layers,
                    "Remove overlay permission",
                    "Open system settings to revoke the floating bubble's permission"
                ) {
                    // A fresh trip with no intent to enable: only refresh the
                    // grant state on return — never re-enable the bubble.
                    AppPreferences.setOverlayAskDeclined(context, false)
                    overlayRevokeOpened = true
                    overlaySettingsOpened = true
                    val launched = runCatching {
                        overlaySettingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                    if (launched.isFailure) {
                        overlaySettingsOpened = false
                        overlayRevokeOpened = false
                    }
                }
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
                    // v78 — light only (the AMOLED pitch-black chip is gone
                    // with dark mode).
                    Surface(
                        onClick = {
                            reminderHour = hour
                            AppPreferences.setReminderHour(context, hour)
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
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
    if (showMusicServiceDialog) {
        MusicServiceDialog(
            current = MusicService.fromId(AppPreferences.musicServiceState),
            onDismiss = { showMusicServiceDialog = false },
            onSelected = { service ->
                AppPreferences.setMusicService(context, service)
                showMusicServiceDialog = false
            }
        )
    }
}

@Composable
private fun RecordingSection(highlightKey: String? = null) {
    val context = LocalContext.current
    var quality by remember { mutableStateOf(AudioQualitySettings.get(context)) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    // v125 — the selected offline model's subtitle ("Small · English (US) ·
    // ~40 MB", or a prompt to download one). Reads the reactive state + the
    // model-version bump so it updates right after a download/delete/select
    // inside the dialog.
    AppPreferences.offlineModelVersionState
    val offlineModelId = AppPreferences.offlineModelIdState
    val offlineModel = VoskModels.byId(offlineModelId)
    // v137 — the download manager outlives the picker sheet, so the row's
    // subtitle reports a background transfer still running (with %).
    val downloadStates by VoskModelDownloads.states.collectAsState()
    val activeDownload = downloadStates.entries.firstOrNull { (_, s) ->
        s.status == VoskModelDownloads.Status.Downloading || s.status == VoskModelDownloads.Status.Paused
    }
    val offlineModelSubtitle = when {
        activeDownload != null -> {
            val m = VoskModels.byId(activeDownload.key)
            val pct = (activeDownload.value.progress * 100).toInt()
            if (m != null) "Downloading ${m.displayName} · $pct%"
            else "Downloading a model · $pct%"
        }
        offlineModel != null && VoskModels.isDownloaded(context, offlineModelId) ->
            "${offlineModel.displayName} · ${offlineModel.sizeLabel}"
        else -> "Offline model for pre-recorded voice-to-text"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsRowPulse(highlightKey == "recording-quality") {
            CurioSettingsRow(CurioIcons.Mic, "Audio quality", quality.label) {
                showQualityDialog = true
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "recording-voice") {
            CompactSwitchRow("Voice-to-text", "Live dictation while typing, and transcription of recordings", AppPreferences.voiceToTextEnabledState) {
                AppPreferences.setVoiceToTextEnabled(context, it)
            }
        }
        CurioSettingsDivider()
        SettingsRowPulse(highlightKey == "recording-offline-model") {
            CurioSettingsRow(CurioIcons.Download, "Offline model", offlineModelSubtitle) {
                showModelDialog = true
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
    if (showModelDialog) {
        OfflineModelDialog(
            currentModelId = AppPreferences.offlineModelIdState,
            onDismiss = { showModelDialog = false }
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
    // v78 — light only (the AMOLED switch color override is gone with dark
    // mode): the scheme's default switch colors.
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors()
        )
    }
}

/** v242 — compact settings slider: label + live value, used by the Liquid
 *  glass tuning rows in Appearance. `value` is 0f..2f (1f = default). */
@Composable
private fun CompactSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.coerceIn(0f, 2f),
            onValueChange = onValueChange,
            valueRange = 0f..2f,
            steps = 7 // 25% increments — 0, 25, 50, 75, 100, 125, 150, 175, 200
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

/**
 * v252 — LIQUID GLASS TUNING DIALOG. The three recipe sliders
 * (Reflection / Refraction / Blur) with a LIVE PREVIEW capsule above them:
 * the preview draws over a colorful collage and re-renders on every slider
 * tick using the exact same preference values the real capsules read, so
 * what you see is what the nav pill will do.
 */
@Composable
fun GlassTuningDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        CurioSettingsCard(shadowElevation = 0.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CurioCardHeader(CurioIcons.Info, "Tune liquid glass", "Drag a slider — the capsule previews it live")
                // v258 — REAL PREVIEW: an actual [liquidGlassCapsule] pill
                // you can DRAG over a colorful collage. Every slider writes
                // the same preference state the real capsules read, so the
                // pill under your finger IS how the nav pills will render.
                var previewOffset by remember { mutableStateOf(Offset.Zero) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF7E57C2),
                                    Color(0xFFEF9A9A),
                                    Color(0xFF80DEEA),
                                    Color(0xFFFFD54F)
                                )
                            )
                        )
                ) {
                    // Collage content behind the glass — real text to bend.
                    Text(
                        text = "Aa Bb Cc\n123 456",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = "Curio",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(previewOffset.x.roundToPx(), previewOffset.y.roundToPx()) }
                            .size(width = 132.dp, height = 48.dp)
                            .liquidGlassCapsule(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(Unit) {
                                detectDragGestures { change, amount ->
                                    change.consume()
                                    previewOffset += amount
                                }
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                "Preview",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                CompactSliderRow("Reflection", "Light sheen strength", AppPreferences.glassReflectionScaleState) {
                    AppPreferences.setGlassReflectionScale(context, it)
                }
                CompactSliderRow("Refraction", "Edge bending strength", AppPreferences.glassRefractionScaleState) {
                    AppPreferences.setGlassRefractionScale(context, it)
                }
                CompactSliderRow("Blur", "Frostiness", AppPreferences.glassBlurScaleState) {
                    AppPreferences.setGlassBlurScale(context, it)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}

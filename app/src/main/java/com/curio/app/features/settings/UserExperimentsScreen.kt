package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioAlivePreferences
import com.curio.app.data.CurioCategories
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private const val KEY_EXPERIMENTS_SEEN = "user_experiments_dialog_seen"

/**
 * v294 — USER EXPERIMENTS PAGE: accessible from Settings hub.
 * Shows unstable/experimental features for users to try.
 * First-time visitors see a warning dialog.
 */
@Composable
fun UserExperimentsScreen(navController: NavController) {
    val context = LocalContext.current
    CurioAlivePreferences.seed(context)
    val prefs = context.getSharedPreferences("curio_prefs", 0)
    var dialogSeen by remember { mutableStateOf(prefs.getBoolean(KEY_EXPERIMENTS_SEEN, false)) }

    if (!dialogSeen) {
        AlertDialog(
            containerColor = com.curio.app.ui.theme.curioDialogContainerColor(),
            shape = com.curio.app.ui.theme.CurioDialogShape,
            onDismissRequest = { dialogSeen = true; prefs.edit().putBoolean(KEY_EXPERIMENTS_SEEN, true).apply() },
            title = { Text("Experimental features") },
            text = {
                Text(
                    "These features are unstable and may change, break, or be removed. " +
                    "Enable them at your own risk. Some features may cause lag or visual glitches on certain devices."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dialogSeen = true
                    prefs.edit().putBoolean(KEY_EXPERIMENTS_SEEN, true).apply()
                }, colors = com.curio.app.ui.theme.curioDialogActionButtonColors()) {
                    Text("I understand")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(heroPageBackground(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
    ) {
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        val listState = rememberLazyListState()
        val glassBackdrop = rememberLayerBackdrop()
        val wide = windowWidthSizeClass().isWide
        LazyColumn(
            state = listState,
            modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
            contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = if (wide) 0.dp else SettingsHeroTotalHeight, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (wide) {
                item(key = "hero", contentType = "hero") {
                    SettingsHeroHeader(
                        title = "Experiments",
                        subtitle = "Try features before they ship",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            item(key = "settings-nav", contentType = "settings-nav") {
                SettingsNavRail(
                    active = "experiments",
                    onSelect = { navigateToSettingsSection(navController, it) },
                    navController = navController
                )
            }

            item { SettingsSectionHeading("Motion") }
            item {
                SettingsOptionCard {
                    ExperimentSwitchRow(
                        "Curio Alive",
                        "Upgrades press feedback, transitions, card arrivals and interaction motion across the app. Turn it off anytime to return to the classic motion.",
                        CurioAlivePreferences.enabledState
                    ) { wanted ->
                        CurioAlivePreferences.setEnabled(context, wanted)
                    }
                }
            }

            item { SettingsSectionHeading("Liquid glass") }
            item {
                SettingsOptionCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Liquid glass", "Refracting glass on the nav bar and floating pills. Not supported by all devices, and can be very laggy on lower-end phones", AppPreferences.liquidGlassPillsState) {
                        AppPreferences.setLiquidGlassPillsEnabled(context, it)
                    }
                    if (AppPreferences.liquidGlassPillsState) {
                        CurioSettingsDivider()
                        // v482 — the whitish SMUDGED frost (default ON): heavy
                        // blur, near-opaque white wash, no refraction. Turning
                        // it off returns the clear refracting glass.
                        ExperimentSwitchRow("Frosted glass (smudged)", "Heavily blurred, near-opaque whitish glass instead of a clear pane — glass reads as smudged white frost, not transparent. Also frosts bottom sheets and dialogs", AppPreferences.glassFrostedState) {
                            AppPreferences.setGlassFrostedEnabled(context, it)
                        }
                        CurioSettingsDivider()
                        ExperimentSwitchRow("Force glass", "Bypass device capability checks and always enable glass", AppPreferences.forceGlassEnabled) {
                            AppPreferences.setForceGlassEnabled(context, it)
                        }
                        if (!AppPreferences.glassFrostedState) {
                        CurioSettingsDivider()
                        ExperimentSwitchRow("Clear glass", "Less frost, stronger refraction. Glass reads clear, like the glow under your finger", AppPreferences.glassClarityState) {
                            AppPreferences.setGlassClarityEnabled(context, it)
                        }
                        }
                        CurioSettingsDivider()
                        var showGlassTuning by remember { mutableStateOf(false) }
                        SettingsOptionRow(
                            CurioIcons.Tune,
                            "Tune glass",
                            "Reflection, refraction and blur, with a live preview"
                        ) { showGlassTuning = true }
                        if (showGlassTuning) {
                            GlassTuningDialog(onDismiss = { showGlassTuning = false })
                        }
                    }
                }
                }
            }

            item { SettingsSectionHeading("Reading") }
            item {
                SettingsOptionCard {
                    ExperimentSwitchRow(
                        "Pinned title view",
                        "Keep the current Book Review and Journal title visible while scrolling. Experimental.",
                        AppPreferences.pinnedTitleViewState
                    ) { AppPreferences.setPinnedTitleViewEnabled(context, it) }
                }
            }

            item { SettingsSectionHeading("Headers") }
            item {
                SettingsOptionCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow(
                        "Glass toolbar header",
                        "Swap the torn paper banners for a content-height liquid-glass bar (more blur, own tint) across Settings, Cabinet, Home and Profile",
                        AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS
                    ) {
                        AppPreferences.setHeaderStyle(
                            context,
                            if (it) AppPreferences.HeaderStyle.GLASS else AppPreferences.HeaderStyle.TORN
                        )
                    }
                    // v468 — the floating pill header; see ExperimentsScreen's twin
                    // row for why there are now three header shapes and why the
                    // landscape half is always-on while this switch is not.
                    ExperimentSwitchRow(
                        "Floating pill header",
                        "A small detached pill (back chevron + title) floating over the page instead of a full-height header. It is always used in landscape and on short windows; this switch turns it on in portrait too.",
                        AppPreferences.floatingPillHeadersState
                    ) { wanted -> AppPreferences.setFloatingPillHeadersEnabled(context, wanted) }
                }
                }
            }

            item { SettingsSectionHeading("Cover fetching") }
            item {
                SettingsOptionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ExperimentSwitchRow(
                            "Cover fetching",
                            "Download book covers + ratings, album artwork, series posters and the " +
                                "art on an Incursion row (keyless providers, TMDB when a key is set)",
                            AppPreferences.coverFetchEnabledState
                        ) {
                            AppPreferences.setCoverFetchEnabled(context, it)
                        }
                    }
                }
            }

            item { SettingsSectionHeading("Capture") }
            item {
                SettingsOptionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ExperimentSwitchRow(
                            "Take studio",
                            "A redesigned Save-your-take workspace: tinted topic hero, a take rail on the bottom tray, format + mood + tags in one tools sheet, and a live recording pulse",
                            AppPreferences.captureStudioState
                        ) {
                            AppPreferences.setCaptureStudioEnabled(context, it)
                        }
                    }
                }
            }

            item { SettingsSectionHeading("Social") }
            item {
                SettingsOptionCard {
                    ExperimentSwitchRow(
                        "Social text editing",
                        "Show edit controls for your own direct messages and comments. Experimental.",
                        AppPreferences.socialTextEditingState
                    ) { AppPreferences.setSocialTextEditingEnabled(context, it) }
                }
            }

            item { SettingsSectionHeading("Content tools") }
            item {
                SettingsOptionCard {
                    SettingsOptionRow(
                        CurioIcons.MenuBook,
                        "Book covers & ratings",
                        if (!AppPreferences.bookFetchEnabledState) "OFF · open the hub to turn fetching on"
                        else if (AppPreferences.bookCoverFailedState.isNotEmpty())
                            "Open the hub · ${AppPreferences.bookCoverFailedState.size} failed covers to retry"
                        else "Open the hub · fetch & retry covers, get ratings",
                        onClick = {
                            navController.navigate(com.curio.app.navigation.CurioRoutes.SETTINGS_BOOK_COVER) {
                                launchSingleTop = true
                            }
                        }
                    )
                    SettingsOptionDivider()
                    SettingsOptionRow(
                        CurioIcons.MenuBook,
                        "Book browser",
                        "Every book line by line — covers, ratings and years, scrollable",
                        onClick = {
                            navController.navigate(com.curio.app.navigation.CurioRoutes.SETTINGS_BOOK_BROWSER) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            // ── v465f — THE EDGE VOICE EXPERIMENT ──────────────────────────
            // Its own section rather than a row beside the pet switch: it is a
            // READING voice, and the caveat it has to carry is the reason it is
            // an experiment at all. The hint is not decoration — a member turning
            // this on is relying on an endpoint nobody promised them.
            item { SettingsSectionHeading("Reading voice \u00b7 experimental") }
            item {
                SettingsOptionCard {
                    ExperimentSwitchRow(
                        "Edge voice",
                        "Read aloud through the Edge browser's own endpoint. No key and no account, and the voices are good \u2014 but it is undocumented, so it can stop working at any time. Your phone's voice takes over when it does.",
                        AppPreferences.edgeVoiceEnabledState
                    ) { AppPreferences.setEdgeVoiceEnabled(context, it) }                }
            }

            item { SettingsSectionHeading("Pet & explore") }
            item {
                SettingsOptionCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Voice-to-text", "Live dictation while typing, and transcription of recordings", AppPreferences.voiceToTextEnabledState) {
                        AppPreferences.setVoiceToTextEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Pet outside the app", "Let your pet float over other apps. Long-press to bring it home.", AppPreferences.petOutsideAppState) { wanted ->
                        if (wanted && !android.provider.Settings.canDrawOverlays(context)) {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:" + context.packageName)
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        } else {
                            AppPreferences.setPetOutsideAppEnabled(context, wanted)
                            com.curio.app.infrastructure.PetOverlayService.sync(context)
                        }
                    }
                    CurioSettingsDivider()
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pet chatter", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("How chatty Curie is", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("Quiet", "Cozy", "Talkative").forEachIndexed { index, label ->
                                val selected = when (AppPreferences.petChatterState) { "quiet" -> 0; "talkative" -> 2; else -> 1 } == index
                                Surface(
                                    onClick = { AppPreferences.setPetChatter(context, when (index) { 0 -> "quiet"; 2 -> "talkative"; else -> "cozy" }) },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                    CurioSettingsDivider()
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pet games", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("How often Curie starts games", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("Relaxed", "Normal", "Eager").forEachIndexed { index, label ->
                                val selected = when (AppPreferences.petGameFrequencyState) { "relaxed" -> 0; "eager" -> 2; else -> 1 } == index
                                Surface(
                                    onClick = { AppPreferences.setPetGameFrequency(context, when (index) { 0 -> "relaxed"; 2 -> "eager"; else -> "normal" }) },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                }
                }
            }

            item {
                SettingsOptionCard {
                    SettingsOptionInfoRow(CurioIcons.Info, "About experiments", "These features are experimental and may change or be removed")
                }
            }
        }
        if (!wide) {
            SettingsHeroHeader(
                title = "Experiments",
                subtitle = "Try features before they ship",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }
}

@Composable
private fun ExperimentSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsOptionSwitchRow(
        icon = null, title, subtitle, checked, enabled,
        modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
        onCheckedChange = onCheckedChange
    )
}

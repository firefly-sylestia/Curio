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
import androidx.compose.material3.Switch
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
import com.curio.app.data.CurioCategories
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsInfoRow
import com.curio.app.ui.components.CurioSettingsRow
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
    val prefs = context.getSharedPreferences("curio_prefs", 0)
    var dialogSeen by remember { mutableStateOf(prefs.getBoolean(KEY_EXPERIMENTS_SEEN, false)) }

    // First-time warning dialog
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
        LazyColumn(
            state = listState,
            modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
            contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Liquid glass section
            item { CurioSectionLabel("Liquid glass") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Liquid glass", "Refracting glass on the nav bar and floating pills — not supported by all devices and can be very laggy on lower-end phones", AppPreferences.liquidGlassPillsState) {
                        AppPreferences.setLiquidGlassPillsEnabled(context, it)
                    }
                    if (AppPreferences.liquidGlassPillsState) {
                        CurioSettingsDivider()
                        ExperimentSwitchRow("Force glass", "Bypass device capability checks — always enable glass", AppPreferences.forceGlassEnabled) {
                            AppPreferences.setForceGlassEnabled(context, it)
                        }
                        CurioSettingsDivider()
                        ExperimentSwitchRow("Clear glass", "Less frost, stronger refraction — glass reads clear like the glow under your finger", AppPreferences.glassClarityState) {
                            AppPreferences.setGlassClarityEnabled(context, it)
                        }
                        CurioSettingsDivider()
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
                }
                }
            }

            // Appearance experiments
            item { CurioSectionLabel("Appearance experiments") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Subtle pill glow", "Gentler, top-only glow on pills in dark mode", AppPreferences.pillGlowSubtleState) {
                        AppPreferences.setPillGlowSubtleEnabled(context, it)
                    }
                }
                }
            }

            // Category picker
            item { CurioSectionLabel("Category picker") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Classic category picker", "The new picker is the default — turn this on to swap back to the old glass-pill picker view", AppPreferences.classicPickerEnabledState) {
                        AppPreferences.setClassicPickerEnabled(context, it)
                    }
                }
                }
            }

            // Content tools — non-toggle experiments
            item { CurioSectionLabel("Content tools") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                    // v320 — the book-cover fetch is now a HUB of its own
                    // (provider picker, retry-failed, keyless ratings).
                    CurioSettingsRow(
                        CurioIcons.MenuBook,
                        "Book covers & ratings",
                        // v320b — opt-out by default: surface the OFF state so
                        // the row explains why nothing is downloading.
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
                }
            }

            // Pet & explore
            item { CurioSectionLabel("Pet & explore") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Voice-to-text", "Live dictation while typing, and transcription of recordings", AppPreferences.voiceToTextEnabledState) {
                        AppPreferences.setVoiceToTextEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Live explore notification", "Ongoing timer with pause and stop", AppPreferences.liveNotificationsEnabledState) {
                        AppPreferences.setLiveNotificationsEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    // Pet outside app
                    ExperimentSwitchRow("Pet outside the app", "Let your pet float over other apps — long-press to bring it home", AppPreferences.petOutsideAppState) { wanted ->
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
                    // Pet chatter
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
                    // Pet games
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
                CurioSettingsCard(shadowElevation = 0.dp) {
                    CurioSettingsInfoRow(CurioIcons.Info, "About experiments", "These features are experimental and may change or be removed")
                }
            }
        }
        SettingsHeroHeader(
            title = "Experiments",
            subtitle = "Try features before they ship",
            onBack = { navController.popBackStack() },
            glassBackdrop = glassBackdrop
        )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsInfoRow
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Experimental controls live here instead of inside Appearance. Each switch
 * keeps its existing preference and remains independently reversible.
 */
@Composable
fun ExperimentsScreen(navController: NavController) {
    val context = LocalContext.current
    // v27v — the ring-style picker (Coil spring / Split ring / Oblique coil).
    var showRingStylePicker by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the lane wash.
            // v31 — settings-family pages wear the soft page tint (a small
            // rose-lean of the background shade; the spin-lane wash when
            // Adaptive Hero is on) instead of the plain cream background.
            .background(heroPageBackground(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
    ) {
        // ── Watermark backdrop — muted category glyphs behind the content
        // (wildcard sparkle leads; experiments are category-neutral).
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
        // RESTORED (user request) — STICKY HERO: the banner is pinned on top
        // of the page and rows scroll UP BEHIND the ragged tear. The list
        // records into a local capture so the hero's back pill can wear REAL
        // liquid glass (rows bending through it) — the pill itself lives
        // OUTSIDE this capture, so no self-sample cycle.
        val listState = rememberLazyListState()
        val glassBackdrop = rememberLayerBackdrop()
        LazyColumn(
            state = listState,
            modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
            contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // v223 — the "Spin visuals" section is GONE: all five
            // experiments (Main card shadow, Nav-style buttons, Top-lit deck
            // cards, Tinted deck edges, Roomier deck titles) CONCLUDED with
            // the new look ON — their toggles were removed and the reads in
            // SpinScreen are hardcoded true. The v25 Deck & controls card was
            // already gone (3D shuffle button always on, Pastel crown depth
            // passed), and v24 removed the Layout & input section.
            // v27 — paper & header experiments, all OFF by default.
            item { CurioSectionLabel("Paper & headers") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Title cut lines", "Two short lines under header titles", AppPreferences.paperHeaderCutsState) {
                        AppPreferences.setPaperHeaderCutsEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Stamped pin holes", "See-through punch holes on the paper stat card (needs the card on)", AppPreferences.paperHeaderHolesState) {
                        AppPreferences.setPaperHeaderHolesEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Hole rings", "3D steel rings through the pin holes (needs Stamped pin holes on)", AppPreferences.paperHoleRingsState) {
                        AppPreferences.setPaperHoleRingsEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    // v27v — pick between the three 3D ring looks; only
                    // enabled while Hole rings is on.
                    val ringStyleEnabled = AppPreferences.paperHoleRingsState
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (ringStyleEnabled) 1f else 0.45f)
                    ) {
                        CurioSettingsRow(
                            CurioIcons.Tune,
                            "Ring style",
                            when (AppPreferences.paperHoleRingStyleState) {
                                "split" -> "Split ring"
                                "oblique" -> "Oblique coil"
                                else -> "Coil spring"
                            }
                        ) {
                            if (ringStyleEnabled) showRingStylePicker = true
                        }
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Paper stat card", "Soft rose paper card on the stat panes and the Profile quests block (on by default)", AppPreferences.paperStatCardsState) {
                        AppPreferences.setPaperStatCardsEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Torn paper edges", "Torn edges on the stat card — extended tear on top", AppPreferences.paperStatTearState) {
                        AppPreferences.setPaperStatTearEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Deeper header color", "Torn-hero headers wear a slightly darker category accent (on by default)", AppPreferences.headerDeepState) {
                        AppPreferences.setHeaderDeepEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    // v108 — the layered white paper lip below the hero's own
                    // bottom tear is OFF by default; the toggle restores it
                    // for comparison.
                    ExperimentSwitchRow("Torn hero under-sheet", "The white paper lip below torn heroes — off: the hero tears straight into the page", AppPreferences.heroTearSheetState) {
                        AppPreferences.setHeroTearSheetEnabled(context, it)
                    }
                }
                }
            }
            item { CurioSectionLabel("Promo") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CurioSettingsRow(
                        CurioIcons.Star,
                        "Promo mode",
                        "Demo content for store screenshots"
                    ) {
                        navController.navigate(CurioRoutes.PROMO) { launchSingleTop = true }
                    }
                }
                }
            }
            item { CurioSectionLabel("Constellation") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("3D star zoom", "Tap a constellation star for a perspective tilt + glow", AppPreferences.starZoom3dState) {
                        AppPreferences.setStarZoom3dEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Drawer constellation", "The star map at the top of the navigation drawer — off shows a small stat strip", AppPreferences.drawerConstellationState) {
                        AppPreferences.setDrawerConstellationEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow(
                        "Classic active indicator",
                        "The nav bar's blob renders as fully transparent refracting glass instead of the solid white/black pill (needs Liquid glass)",
                        AppPreferences.glassClassicIndicatorState
                    ) {
                        AppPreferences.setGlassClassicIndicatorEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow(
                        "Real blur (older devices)",
                        "Below Android 12: an app-side blur engine draws the REAL content behind the nav bar and Topic Reveal pills as frosted glass instead of a static veil (needs Liquid glass pills)",
                        AppPreferences.legacyGlassBlurState
                    ) {
                        AppPreferences.setLegacyGlassBlurEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    // v280 — custom blur engine: replaces Samsung One UI / system
                    // blur paths with Curio's own AGSL gaussian (API 33+) or
                    // CPU StackBlur (older) for the glass widget and live wallpaper.
                    ExperimentSwitchRow(
                        "Custom blur engine",
                        "Replace Samsung/system blur with Curio's own blur in glass widgets and live wallpaper (every launcher gets real blur)",
                        AppPreferences.customBlurEngineState
                    ) {
                        AppPreferences.setCustomBlurEngineEnabled(context, it)
                    }
                }
                }
            }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                    // v264 — the glass widget lab: drag REAL refracting widget
                    // shapes over your actual wallpaper (test bed for a future
                    // home-screen widget design).
                    com.curio.app.ui.components.CurioSettingsRow(
                        CurioIcons.AutoAwesome,
                        "Glass widget lab",
                        "Drag real liquid-glass widget shapes over your wallpaper"
                    ) {
                        navController.navigate(com.curio.app.navigation.CurioRoutes.GLASS_WIDGET_LAB) { launchSingleTop = true }
                    }
                    // v281 - in-app editor: works even on launchers without
                    // the long-press Edit flow for reconfigurable widgets.
                    com.curio.app.ui.components.CurioSettingsRow(
                        CurioIcons.Settings,
                        "Edit home screen widgets",
                        "Change mode, pane and corners of placed widgets in-app"
                    ) {
                        navController.navigate(com.curio.app.navigation.CurioRoutes.GLASS_WIDGET_EDITOR) { launchSingleTop = true }
                    }
                }
            }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                    CurioSettingsInfoRow(CurioIcons.Info, "About experiments", "These controls are temporary and may change")
                }
            }
        }
        // Drawn on TOP of the scroll content — rows slide under the ragged
        // tear as they scroll up. Its back pill refracts the captured rows.
        SettingsHeroHeader(
            title = "Experiments",
            subtitle = "Try ideas before they ship",
            onBack = { navController.popBackStack() },
            glassBackdrop = glassBackdrop
        )
    }

    // ── Ring style picker — the three 3D ring looks, single-select ──
    if (showRingStylePicker) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { showRingStylePicker = false },
            title = { Text("Ring style") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("coil", "Coil spring", "Wire coil through the hole — spiral-notebook look"),
                        Triple("split", "Split ring", "Closed metal torus — keyring / binder-ring look"),
                        Triple("oblique", "Oblique coil", "Short coil segments springing out of the hole")
                    ).forEach { (value, label, desc) ->
                        Surface(
                            onClick = { AppPreferences.setPaperHoleRingStyle(context, value) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = if (AppPreferences.paperHoleRingStyleState == value)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (AppPreferences.paperHoleRingStyleState == value)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (AppPreferences.paperHoleRingStyleState == value)
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        "The stat cards on Home and Profile preview the change immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showRingStylePicker = false },
                    colors = curioDialogActionButtonColors()
                ) {
                    Text("Done")
                }
            }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

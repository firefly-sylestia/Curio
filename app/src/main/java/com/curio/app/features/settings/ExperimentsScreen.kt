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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.curio.app.ui.theme.CurioIcons

/**
 * Experimental controls live here instead of inside Appearance. Each switch
 * keeps its existing preference and remains independently reversible.
 */
@Composable
fun ExperimentsScreen(navController: NavController) {
    val context = LocalContext.current
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight + 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CurioSectionLabel("Spin visuals") }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Main card shadow", "Ambient depth below the hero card", AppPreferences.heroShadowState) {
                        AppPreferences.setHeroShadowEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Nav-style buttons", "Categories, Filter and their vertical twins wear the floating nav-pill look", AppPreferences.navPillButtonsState) {
                        AppPreferences.setNavPillButtonsEnabled(context, it)
                    }
                }
                }
            }
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Top-lit deck cards", "Peek cards catch light at the top edge", AppPreferences.peekGradientState) {
                        AppPreferences.setPeekGradientEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Tinted deck edges", "Category-tinted hairline on peek cards", AppPreferences.peekHairlineState) {
                        AppPreferences.setPeekHairlineEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Roomier deck titles", "Two-line near-card titles", AppPreferences.peekTitlesState) {
                        AppPreferences.setPeekTitlesEnabled(context, it)
                    }
                }
                }
            }
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
                    ExperimentSwitchRow("Paper stat card", "Soft rose paper card on the stat panes and the Profile quests block (on by default)", AppPreferences.paperStatCardsState) {
                        AppPreferences.setPaperStatCardsEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Torn paper edges", "Torn edges on the stat card - extended tear on top", AppPreferences.paperStatTearState) {
                        AppPreferences.setPaperStatTearEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Deeper header color", "Torn-hero headers wear a slightly darker category accent (on by default)", AppPreferences.headerDeepState) {
                        AppPreferences.setHeaderDeepEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Torn hero under-sheet", "The white paper lip below torn heroes - off: the hero tears straight into the page", AppPreferences.heroTearSheetState) {
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
            item {
                CurioSettingsCard(shadowElevation = 0.dp) {
                    CurioSettingsInfoRow(CurioIcons.Info, "About experiments", "These controls are temporary and may change")
                }
            }
        }
        SettingsHeroHeader(
            title = "Experiments",
            subtitle = "Try ideas before they ship",
            onBack = { navController.popBackStack() }
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

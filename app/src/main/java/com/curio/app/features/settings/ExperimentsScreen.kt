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
            .background(MaterialTheme.colorScheme.background)
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
        // The hero banner runs up BEHIND the status bar (the header applies
        // its own status-bar inset for the back pill) — Profile/Home style.
        // The hero is drawn LAST (on top of the scroll content): the rows
        // scroll UP and disappear behind the ragged tear instead of clipping
        // at a straight line.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight + 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CurioSectionLabel("Spin visuals") }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // v25 — the Enhanced main gradient experiment PASSED
                    // (always ON), so its toggle was removed here.
                    // v24 — the dual-accent hero gradient experiment was
                    // rejected (ugly golden blend); always OFF, so its toggle
                    // was removed here.
                    ExperimentSwitchRow("Main card shadow", "Ambient depth below the hero card", AppPreferences.heroShadowState) {
                        AppPreferences.setHeroShadowEnabled(context, it)
                    }
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Top-lit deck cards", "Peek cards catch light at the top edge", AppPreferences.peekGradientState) {
                        AppPreferences.setPeekGradientEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Tinted deck edges", "Category-tinted hairline on peek cards", AppPreferences.peekHairlineState) {
                        AppPreferences.setPeekHairlineEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    // v24 — deck card shadows (weird look while the cards
                    // animate) and tail-fade peek motion (didn't pass) were
                    // both rejected; always OFF, so their toggles were removed.
                    ExperimentSwitchRow("Roomier deck titles", "Two-line near-card titles", AppPreferences.peekTitlesState) {
                        AppPreferences.setPeekTitlesEnabled(context, it)
                    }
                }
            }
            // v25 — the Deck & controls card is gone: the 3D shuffle button
            // (always on) and Pastel crown depth (PASSED, always on) both had
            // their toggles removed, leaving the card empty.
            // v24 — the Layout & input section was removed: Smart Spin layout
            // is gone for good (the deck always uses its natural size) and
            // Smart density's control moved out; Voice-to-text still lives in
            // Settings → Recording.
            // v27 — paper & header experiments, all OFF by default.
            item { CurioSectionLabel("Paper & headers") }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExperimentSwitchRow("Title cut lines", "Two short lines under header titles", AppPreferences.paperHeaderCutsState) {
                        AppPreferences.setPaperHeaderCutsEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Stamped pin holes", "See-through punch holes on the paper stat card (needs the card on)", AppPreferences.paperHeaderHolesState) {
                        AppPreferences.setPaperHeaderHolesEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Paper stat card", "Streak · Cabinet · Topics on a soft rose paper card", AppPreferences.paperStatCardsState) {
                        AppPreferences.setPaperStatCardsEnabled(context, it)
                    }
                    CurioSettingsDivider()
                    ExperimentSwitchRow("Torn paper edges", "Torn edges on the stat card — extended tear on top", AppPreferences.paperStatTearState) {
                        AppPreferences.setPaperStatTearEnabled(context, it)
                    }
                }
            }
            item { CurioSectionLabel("Promo") }
            item {
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
            item { CurioSettingsInfoRow(CurioIcons.Info, "About experiments", "These controls are temporary and may change") }
        }
        // Drawn on top of the scroll content — rows slide under the ragged
        // tear as they scroll up.
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

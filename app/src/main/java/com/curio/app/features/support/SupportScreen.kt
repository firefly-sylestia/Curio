package com.curio.app.features.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.BuildConfig
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.features.onboarding.CurioOnboardingState
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.delay

/**
 * Support & diagnostics — the dedicated page behind Profile's
 * "Support & diagnostics" row. Sits in the settings family (shared torn
 * rose hero on a watermark backdrop, rows scrolling under the tear) and
 * opens with a ScreenEntrance animation.
 *
 * Contents:
 *  - Updates: v112 — the update flow (check / release notes / download /
 *    install) moved to its OWN sub-page (Settings → Updates). This page
 *    keeps the version readout (five-tap diagnostic → Experiments) and a
 *    row that opens the Updates page.
 *  - Feedback: Report a bug, Crash logs, Test crash.
 *  - About Curio: Replay intro + the open-source GitHub repository (merged
 *    here from the old Settings → About page).
 */
@Composable
fun SupportScreen(navController: NavController) {
    val context = LocalContext.current
    val crashCount = remember { CurioCrashReporter.getCrashHistory(context).size }

    // Version row five-tap (v24) — opens the Experiments screen (kept open);
    // the counter resets itself after a short pause so stray taps never fire.
    var versionTaps by remember { mutableIntStateOf(0) }
    LaunchedEffect(versionTaps) {
        if (versionTaps in 1..4) {
            delay(2500)
            versionTaps = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the lane wash.
            // v31 — the Support page wears the soft page tint (a small
            // rose-lean of the background shade; the spin-lane wash when
            // Adaptive Hero is on) instead of the plain cream background.
            .background(heroPageBackground(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
    ) {
        // ── Watermark backdrop — muted category glyphs (settings family).
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        // ── Scroll content — fills the screen, runs under the ragged tear.
        ScreenEntrance {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = SettingsHeroTotalHeight + 10.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { CurioSectionLabel("Updates") }
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CurioCardHeader(CurioIcons.Download, "Updates", "Your build and what's new")
                        // Version — tappable: five taps TOGGLE promo mode
                        // (on → off, off → on); the promo page then shows
                        // the resulting state. Subtitle hints while counting
                        // and shows the live mode when on.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    versionTaps++
                                    if (versionTaps >= 5) {
                                        versionTaps = 0
                                        // v24 — the five-tap opens the
                                        // Experiments screen (and keeps it
                                        // open); it no longer toggles promo
                                        // mode (promo lives in Experiments).
                                        navController.navigate(CurioRoutes.EXPERIMENTS) { launchSingleTop = true }
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.Info, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 21.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Version", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = when {
                                        versionTaps in 1..4 ->
                                            "Tap ${5 - versionTaps} more to open Experiments"
                                        else ->
                                            "${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        CurioSettingsDivider()
                        // v112 — the update flow (check / release notes /
                        // download / install) lives on the dedicated Updates
                        // sub-page; this row is the link there.
                        CurioSettingsRow(
                            CurioIcons.Download,
                            "Open Updates",
                            "Your build, release notes & update checker"
                        ) {
                            navController.navigate(CurioRoutes.UPDATES) { launchSingleTop = true }
                        }
                    }
                }
                item { CurioSectionLabel("Feedback") }
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CurioCardHeader(CurioIcons.BugReport, "Reports & crash logs", "Tell us what broke")
                        CurioSettingsRow(
                            CurioIcons.BugReport,
                            "Report a bug",
                            "Opens a pre-filled GitHub issue"
                        ) {
                            navController.navigate(CurioRoutes.BUG_REPORT) { launchSingleTop = true }
                        }
                        if (crashCount > 0) {
                            CurioSettingsDivider()
                            CurioSettingsRow(
                                CurioIcons.History,
                                "Crash logs",
                                "$crashCount saved report${if (crashCount == 1) "" else "s"}"
                            ) {
                                navController.navigate(CurioRoutes.CRASH) { launchSingleTop = true }
                            }
                        }
                        CurioSettingsDivider()
                        CurioSettingsRow(
                            CurioIcons.ErrorOutline,
                            "Test crash",
                            "Diagnostic tool"
                        ) { CurioCrashReporter.testCrash() }
                    }
                }
                // ── About Curio — merged here from the old Settings → About
                //    page (v24): Replay intro + the project link. One page,
                //    reached from Settings and Profile alike.
                item { CurioSectionLabel("About Curio") }
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CurioCardHeader(CurioIcons.Info, "About Curio", "The app, the journey, and its source")
                        CurioSettingsRow(
                            CurioIcons.Replay,
                            "Replay intro",
                            "See the welcome screens again"
                        ) {
                            CurioOnboardingState.reset(context)
                            navController.navigate(CurioRoutes.ONBOARDING) { launchSingleTop = true }
                        }
                        CurioSettingsDivider()
                        CurioSettingsRow(
                            CurioIcons.Info,
                            "GitHub repository",
                            "Source, releases, and issues"
                        ) {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/firefly-sylestia/Curio")
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        // ── Torn rose hero on top — rows disappear under the tear as they
        // scroll (the settings overlay pattern).
        SettingsHeroHeader(
            title = "Support & diagnostics",
            subtitle = "Reports & help",
            onBack = { navController.popBackStack() }
        )
    }
}

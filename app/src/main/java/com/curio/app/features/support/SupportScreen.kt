package com.curio.app.features.support

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.BuildConfig
import com.curio.app.R
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.features.feedback.FeedbackFormCard
import com.curio.app.features.feedback.FeedbackFormState
import com.curio.app.features.onboarding.CurioOnboardingState
import com.curio.app.features.reveal.TmdbFetch
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsNavRail
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionDivider
import com.curio.app.features.settings.SettingsOptionRow
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.navigateToSettingsSection
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.delay
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.features.settings.SettingsHeroTotalHeight

/**
 * Support & diagnostics — the dedicated page behind Profile's
 * "Support & diagnostics" row. Sits in the settings family (shared torn
 * rose hero on a watermark backdrop, rows scrolling under the tear) and
 * opens with a ScreenEntrance animation.
 *
 * Contents:
 *  - Feedback: Report a bug, Crash logs, Test crash.
 *  - About Curio: Replay intro + the open-source GitHub repository (merged
 *    here from the old Settings → About page). v428 — this is also the app's
 *    CREDITS section, so it carries TMDB's required attribution row (its own
 *    approved logo + the verbatim notice, shown only when a build holds one of
 *    TMDB's two credentials; see [TmdbFetch]).
 *  - Updates (LAST): v112 — the update flow (check / release notes /
 *    download / install) moved to its OWN sub-page (Settings → Updates).
 *    This page keeps the version readout (five-tap diagnostic →
 *    Experiments) plus an "Updates" row that OPENS that sub-page (v118 —
 *    the user asked for a direct update link here; the v116 de-dupe stays
 *    intact because there is exactly ONE link, not a duplicate header).
 */
/**
 * v436 — BLOBATAR'S NOTICE, AS THE LICENCE WRITES IT.
 *
 * The bundled raw file is the authority (see the credits row); this only shows
 * it, and a failure to read it says so rather than opening an empty box.
 */
@Composable
private fun BlobatarLicenseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val notice = remember {
        runCatching {
            context.resources.openRawResource(R.raw.blobatar_license)
                .bufferedReader()
                .use { it.readText() }
        }.getOrDefault("The licence text could not be read on this device.")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Member faces by blobatar") },
        text = {
            Text(
                notice,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            CurioGlassWindowBlur()
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun SupportScreen(navController: NavController) {
    val context = LocalContext.current
    val crashCount = remember { CurioCrashReporter.getCrashHistory(context).size }
    // v436 — blobatar's notice, read from the bundled raw file only if the row is
    // ever opened (a licence text nobody taps should not be decoded on the way in).
    var blobatarNoticeShown by remember { mutableStateOf(false) }
    if (blobatarNoticeShown) {
        BlobatarLicenseDialog(onDismiss = { blobatarNoticeShown = false })
    }

    // v403 — the one published feedback form (if there is one). The card at
    // the top of the page is the ask; the row inside the Feedback card is the
    // way back to it later, which the member asked for by name.
    LaunchedEffect(Unit) { FeedbackFormState.refresh(context) }
    val liveForm = FeedbackFormState.liveForm
    val formReady = liveForm != null && FeedbackFormState.visible

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
            val listState = rememberLazyListState()
            val glassBackdrop = rememberLayerBackdrop()
            // v-tablet — the torn hero is NOT sticky on wide windows
            // (landscape tablet): it leads the list as its first item and
            // scrolls away with it; the pinned glass overlay stays
            // phone-only.
            val wide = windowWidthSizeClass().isWide
            LazyColumn(
                state = listState,
                modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
                // v255 — SCROLLING HERO: the banner is the list's first item
                // and scrolls away with the page (the Home/Profile way).
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = if (wide) 0.dp else SettingsHeroTotalHeight,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (wide) {
                    item(key = "hero", contentType = "hero") {
                        SettingsHeroHeader(
                            title = "Support & diagnostics",
                            subtitle = "Reports & help",
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                // v3xx — the shared settings nav rail: switch sections
                // without going back to the hub (the open page sits in the
                // 2nd slot).
                item(key = "settings-nav", contentType = "settings-nav") {
                    SettingsNavRail(
                        active = "support",
                        onSelect = { navigateToSettingsSection(navController, it) },
                        navController = navController
                    )
                }
                // ── The form, first: the page's standout tile while one is
                //    live (filled in the app's own accent tone, so it stands
                //    apart from every pale settings card under it).
                if (formReady) {
                    val form = requireNotNull(liveForm)
                    item(key = "feedback-form", contentType = "feedback-form") {
                        FeedbackFormCard(
                            form = form,
                            onOpen = { FeedbackFormState.openSheet() },
                            onSkip = { FeedbackFormState.skip(context, form, never = false) },
                            onNever = { FeedbackFormState.skip(context, form, never = true) }
                        )
                    }
                }
                item { SettingsSectionHeading("Feedback") }
                item {
                    SettingsOptionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // The form has a door here for as long as it is live, so
                        // a member can take it later without hunting for it.
                        SettingsOptionRow(
                            CurioIcons.AutoAwesome,
                            "Feedback form",
                            when {
                                !OnlineAccount.signedIn ->
                                    "Turn Online mode on to answer"
                                liveForm == null -> "No form is open right now"
                                formReady -> "Answering is anonymous · take it when you like"
                                else -> "You have already dealt with this one · " +
                                    "thank you"
                            }
                        ) {
                            if (formReady) FeedbackFormState.openSheet()
                        }
                        SettingsOptionDivider()
                        SettingsOptionRow(
                            CurioIcons.BugReport,
                            "Report a bug",
                            "Opens a pre-filled GitHub issue"
                        ) {
                            navController.navigate(CurioRoutes.BUG_REPORT) { launchSingleTop = true }
                        }
                        if (crashCount > 0) {
                            SettingsOptionDivider()
                            SettingsOptionRow(
                                CurioIcons.History,
                                "Crash logs",
                                "$crashCount saved report${if (crashCount == 1) "" else "s"}"
                            ) {
                                navController.navigate(CurioRoutes.CRASH) { launchSingleTop = true }
                            }
                        }
                        SettingsOptionDivider()
                        SettingsOptionRow(
                            CurioIcons.ErrorOutline,
                            "Test crash",
                            "Diagnostic tool"
                        ) { CurioCrashReporter.testCrash() }
                    }
                    }
                }
                // ── About Curio — merged here from the old Settings → About
                //    page (v24): Replay intro + the project link. One page,
                //    reached from Settings and Profile alike. v3xx — the
                //    card's internal "About Curio" header was a duplicate of
                //    the section heading above it; the heading owns the
                //    title now and the rows wear the plain icon-tile rows.
                item { SettingsSectionHeading("About Curio") }
                item {
                    SettingsOptionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsOptionRow(
                            CurioIcons.Replay,
                            "Replay intro",
                            "See the welcome screens again"
                        ) {
                            CurioOnboardingState.reset(context)
                            navController.navigate(CurioRoutes.ONBOARDING) { launchSingleTop = true }
                        }
                        SettingsOptionDivider()
                        SettingsOptionRow(
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
                        SettingsOptionDivider()
                        // v286 — open-source credit: the liquid-glass
                        // pipeline runs on Kyant0's backdrop library
                        // (drawBackdrop + vibrancy + blur + lens recipe).
                        SettingsOptionRow(
                            CurioIcons.Info,
                            "Liquid glass by Kyant",
                            "github.com/Kyant0/AndroidLiquidGlass"
                        ) {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/Kyant0/AndroidLiquidGlass")
                                    )
                                )
                            }
                        }
                        SettingsOptionDivider()
                        // ── v436 — BLOBATAR'S MIT NOTICE ──────────────────
                        //
                        // The member's faces are a Kotlin port of blobatar's
                        // generation-2 core (see `features/community/Blobatar.kt`),
                        // and MIT requires the copyright and permission notice to
                        // be included with a substantial portion of the software
                        // — so this is a LICENCE OBLIGATION, not a courtesy, and
                        // the row opens the notice verbatim rather than only
                        // linking to somebody else's page.
                        SettingsOptionRow(
                            CurioIcons.Info,
                            "Member faces by blobatar",
                            "MIT licence \u00b7 \u00a9 2026 Alain"
                        ) {
                            blobatarNoticeShown = true
                        }
                        // v428 — TMDB'S ATTRIBUTION, IN THE APP'S CREDITS
                        // SECTION — the place TMDB's own terms name. Every
                        // application using its API must carry the sentence
                        // below verbatim and show its logo (unmodified), and
                        // the film sheets and the Incursion rows take TMDB's
                        // artwork and facts; so the row is here, in the same
                        // card as the app's other credits, wearing TMDB's own
                        // approved mark instead of a Curio glyph.
                        //
                        // SHOWN ONLY WHEN THIS BUILD CARRIES ONE OF TMDB'S TWO
                        // CREDENTIALS (see [TmdbFetch]): a build with neither
                        // never asks TMDB for anything, so there is nothing to
                        // attribute — the row is a statement of fact, not a
                        // permanently parked credit. The tap goes to
                        // themoviedb.org, which is the link TMDB's branding
                        // rules require it to be.
                        if (TmdbFetch.isConfigured) {
                            SettingsOptionDivider()
                            SettingsOptionRow(
                                icon = null,
                                logoRes = R.raw.tmdb_logo,
                                // The required notice, kept verbatim.
                                title = "Artwork and metadata by TMDB",
                                subtitle = "This product uses the TMDB API but is not " +
                                    "endorsed or certified by TMDB.",
                                // The one row whose sentence must never be cut:
                                // `plain` is what lets it run to three lines.
                                plain = true
                            ) {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.themoviedb.org")
                                        )
                                    )
                                }
                            }
                        }
                    }
                    }
                }
                // ── Updates — the LAST section: the update flow itself lives
                //    on the dedicated Updates sub-page (Settings → Updates),
                //    so this keeps only the version readout (five taps →
                //    Experiments) with no duplicate Updates entry (v116).
                item { SettingsSectionHeading("Updates") }
                item {
                    // v115 — the support sections sit in the shared settings
                    // card so the page reads as settings options, not
                    // transparent rows floating on the backdrop.
                    SettingsOptionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // v118 — the user asked for an update link right here:
                        // one row that opens the dedicated Updates sub-page.
                        SettingsOptionRow(
                            CurioIcons.Download,
                            "Updates",
                            "Check for updates, release notes & install"
                        ) {
                            navController.navigate(CurioRoutes.UPDATES) { launchSingleTop = true }
                        }
                        SettingsOptionDivider()
                        // Version — tappable: five taps open the Dev page.
                        SettingsOptionRow(
                            CurioIcons.Info,
                            "Version",
                            when {
                                versionTaps in 1..4 ->
                                    "Tap ${5 - versionTaps} more to open Dev page"
                                else ->
                                    "${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}"
                            }
                        ) {
                            versionTaps++
                            if (versionTaps >= 5) {
                                versionTaps = 0
                                // v24 — the five-tap opens the Dev page (and
                                // keeps it open); promo lives in Experiments.
                                navController.navigate(CurioRoutes.EXPERIMENTS) { launchSingleTop = true }
                            }
                        }
                    }
                    }
                }
            }

            // RESTORED (user request) — STICKY HERO drawn on TOP of the scroll
            // content: rows slide under the ragged tear as they scroll up, and
            // the back pill refracts them through REAL liquid glass.
            // v-tablet — pinned overlay is phone-only; wide windows scroll
            // the hero as the list's first item instead.
            if (!wide) {
                SettingsHeroHeader(
                    title = "Support & diagnostics",
                            subtitle = "Reports & help",
                    onBack = { navController.popBackStack() },
                    glassBackdrop = glassBackdrop
                )
            }
        }
    }
}

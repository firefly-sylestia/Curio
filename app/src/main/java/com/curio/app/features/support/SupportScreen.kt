package com.curio.app.features.support

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.curio.app.BuildConfig
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.UpdateChecker
import com.curio.app.data.UpdateInfo
import com.curio.app.features.onboarding.CurioOnboardingState
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
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
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Support & diagnostics — the dedicated page behind Profile's
 * "Support & diagnostics" row. Sits in the settings family (shared torn
 * rose hero on a watermark backdrop, rows scrolling under the tear) and
 * opens with a ScreenEntrance animation.
 *
 * Contents:
 *  - Updates: accurate version readout + a Check for updates row that runs
 *    on open, with an animated result card. v25 — GitHub-only: the check
 *    fetches the latest release and the in-app updater downloads its APK
 *    (with progress) then hands it to the system installer; the release
 *    notes are shown inline (expandable). The v24 Google Play in-app
 *    update was removed.
 *  - Feedback: Report a bug, Crash logs, Test crash.
 *  - About Curio: Replay intro + the open-source GitHub repository (merged
 *    here from the old Settings → About page).
 */
@Composable
fun SupportScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkState by remember { mutableStateOf(UpdateCheckUi.Idle) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var resultVisible by remember { mutableStateOf(false) }
    val crashCount = remember { CurioCrashReporter.getCrashHistory(context).size }
    // v25 — GitHub-only update flow: the Play in-app update was removed and
    // replaced with an in-app APK download from the release itself.
    var notesExpanded by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf(UpdateDownloadUi.Idle) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    // True until the response's Content-Length is known — the bar runs
    // indeterminate instead of pinning at a misleading 0%.
    var downloadIndeterminate by remember { mutableStateOf(true) }

    // Version row five-tap (v24) — opens the Experiments screen (kept open);
    // the counter resets itself after a short pause so stray taps never fire.
    var versionTaps by remember { mutableIntStateOf(0) }
    LaunchedEffect(versionTaps) {
        if (versionTaps in 1..4) {
            delay(2500)
            versionTaps = 0
        }
    }

    fun runCheck() {
        if (checkState == UpdateCheckUi.Checking) return
        checkState = UpdateCheckUi.Checking
        resultVisible = false
        notesExpanded = false
        downloadState = UpdateDownloadUi.Idle
        downloadIndeterminate = true
        scope.launch {
            // v25 — GitHub-only: fetch the latest release; the in-app updater
            // downloads its APK when one is attached to the release.
            val gh = UpdateChecker.fetchLatestRelease()
            when {
                gh != null && UpdateChecker.isNewer(gh.tagName, BuildConfig.VERSION_NAME) -> {
                    updateInfo = gh
                    checkState = UpdateCheckUi.GithubAvailable
                }
                gh != null -> {
                    updateInfo = gh
                    checkState = UpdateCheckUi.UpToDate
                }
                else -> checkState = UpdateCheckUi.Failed
            }
            resultVisible = true
        }
    }

    // v25 — GitHub in-app updater: downloads the release APK with progress,
    // then hands it to the system installer — the USER confirms the install
    // (never auto-installed). Android 8+ asks once to allow installs from
    // this source (REQUEST_INSTALL_PACKAGES).
    fun downloadAndInstall(info: UpdateInfo) {
        if (downloadState == UpdateDownloadUi.Downloading) return
        val apkUrl = info.apkUrl ?: return
        scope.launch {
            downloadState = UpdateDownloadUi.Downloading
            downloadProgress = 0f
            downloadIndeterminate = true
            val target = File(context.cacheDir, "downloads/curio-${info.tagName}.apk")
            target.parentFile?.mkdirs()
            val ok = UpdateChecker.downloadApk(apkUrl, target) { received, total ->
                if (total > 0) {
                    downloadIndeterminate = false
                    downloadProgress = received.toFloat() / total
                }
            }
            if (!ok) {
                downloadState = UpdateDownloadUi.Failed
                return@launch
            }
            downloadState = UpdateDownloadUi.Idle
            // Launch the package installer — the user confirms the install.
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    target
                )
                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(install)
            }.onFailure { downloadState = UpdateDownloadUi.InstallFailed }
        }
    }

    // Auto-check on open so the release-notes preview appears immediately.
    LaunchedEffect(Unit) { runCheck() }

    val checkSubtitle = when (checkState) {
        UpdateCheckUi.Idle -> "Tap to check for the latest release"
        UpdateCheckUi.Checking -> "Checking for updates…"
        UpdateCheckUi.UpToDate -> "You're up to date · v${BuildConfig.VERSION_NAME}"
        UpdateCheckUi.GithubAvailable -> "New version: ${updateInfo?.tagName ?: ""}"
        UpdateCheckUi.Failed -> "Couldn't check · tap to retry"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        CurioSettingsRow(
                            CurioIcons.Download,
                            "Check for updates",
                            checkSubtitle,
                            onClick = { runCheck() }
                        )
                        // Result card — release-notes preview, animated in.
                        AnimatedVisibility(
                            visible = resultVisible,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            UpdateResultCard(
                                state = checkState,
                                info = updateInfo,
                                notesExpanded = notesExpanded,
                                onToggleNotes = { notesExpanded = !notesExpanded },
                                downloadState = downloadState,
                                downloadProgress = downloadProgress,
                                downloadIndeterminate = downloadIndeterminate,
                                onDownloadUpdate = { updateInfo?.let { downloadAndInstall(it) } },
                                onRetryDownload = { updateInfo?.let { downloadAndInstall(it) } },
                                onOpenRelease = { url ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }
                            )
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
            subtitle = "Updates, reports & help",
            onBack = { navController.popBackStack() }
        )
    }
}

/** The update result card — up-to-date / update-available / failed, with a
 *  release-notes preview when the API returned them. v25 — a GitHub
 *  download-and-install flow (with progress) replaces the removed Play path. */
@Composable
private fun UpdateResultCard(
    state: UpdateCheckUi,
    info: UpdateInfo?,
    notesExpanded: Boolean,
    onToggleNotes: () -> Unit,
    downloadState: UpdateDownloadUi,
    downloadProgress: Float,
    downloadIndeterminate: Boolean,
    onDownloadUpdate: () -> Unit,
    onRetryDownload: () -> Unit,
    onOpenRelease: (String) -> Unit
) {
    if (state == UpdateCheckUi.Idle || state == UpdateCheckUi.Checking) return
    val (tint, title) = when (state) {
        UpdateCheckUi.UpToDate ->
            CurioColors.Sage to "You're on the latest version"
        UpdateCheckUi.GithubAvailable ->
            CurioColors.CoralBlush to "New version: ${info?.tagName ?: ""}"
        else ->
            MaterialTheme.colorScheme.error to "Couldn't check for updates"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            val notes = info?.releaseNotes?.takeIf { it.isNotBlank() }
            if (notes != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "What's new in ${info.tagName}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (notesExpanded) Int.MAX_VALUE else 5,
                    overflow = if (notesExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )
                // v24 — the full release notes inline, not a clipped preview.
                Text(
                    if (notesExpanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = CurioColors.CoralBlush
                    ),
                    modifier = Modifier
                        .clickable(onClick = onToggleNotes)
                        .padding(top = 6.dp)
                )
            }
            when (state) {
                UpdateCheckUi.GithubAvailable -> {
                    Spacer(Modifier.height(10.dp))
                    when (downloadState) {
                        UpdateDownloadUi.Idle -> {
                            // v25 — in-app updater: download the release APK
                            // and let the system installer finish it (the
                            // user confirms).
                            if (info?.apkUrl != null) {
                                Surface(
                                    onClick = onDownloadUpdate,
                                    shape = RoundedCornerShape(50),
                                    color = CurioColors.CoralBlush,
                                    contentColor = CurioColors.DeepPlum
                                ) {
                                    Text(
                                        "Update now",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                    )
                                }
                            }
                            // Short secondary link to the release page.
                            Text(
                                "Open release",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = CurioColors.CoralBlush
                                ),
                                modifier = Modifier
                                    .clickable { info?.htmlUrl?.let { onOpenRelease(it) } }
                                    .padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        UpdateDownloadUi.Downloading -> {
                            Column {
                                // Indeterminate until the response's
                                // Content-Length is known; determinate after.
                                if (downloadIndeterminate) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = CurioColors.CoralBlush,
                                        trackColor = CurioColors.CoralBlush.copy(alpha = 0.20f)
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = CurioColors.CoralBlush,
                                        trackColor = CurioColors.CoralBlush.copy(alpha = 0.20f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (downloadIndeterminate) "Downloading…"
                                    else "Downloading… ${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        UpdateDownloadUi.Failed -> {
                            Text(
                                "Download didn't finish — try again",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .clickable(onClick = onRetryDownload)
                                    .padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                        UpdateDownloadUi.InstallFailed -> {
                            Text(
                                "Couldn't open the installer — try again",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .clickable(onClick = onRetryDownload)
                                    .padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                    }
                }
                UpdateCheckUi.UpToDate -> {
                    if (info != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "View release notes",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = CurioColors.CoralBlush
                            ),
                            modifier = Modifier
                                .clickable { onOpenRelease(info.htmlUrl) }
                                .padding(top = 4.dp)
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}

private enum class UpdateCheckUi { Idle, Checking, UpToDate, GithubAvailable, Failed }

/** v25 — GitHub in-app updater states (download → system installer). */
private enum class UpdateDownloadUi { Idle, Downloading, Failed, InstallFailed }

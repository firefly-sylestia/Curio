package com.curio.app.features.updates

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.curio.app.BuildConfig
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.UpdateChecker
import com.curio.app.data.UpdateInfo
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsInfoRow
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Updates — the dedicated sub-page for the in-app updater (v112). Own UI
 * instead of the old card inside Support & diagnostics: a settings-family
 * torn-rose hero on a watermark backdrop, with the version readout, a check
 * for updates row, the release-notes / download / install result card, and
 * the opt-in UPDATE CHECKER toggle (Curio is offline-first, so the
 * background check — which costs data every launch — is off until enabled
 * here; the manual check always works).
 */
@Composable
fun UpdatesScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkState by remember { mutableStateOf(UpdateCheckUi.Idle) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var resultVisible by remember { mutableStateOf(false) }
    var notesExpanded by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf(UpdateDownloadUi.Idle) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadIndeterminate by remember { mutableStateOf(true) }
    // Opt-in toggle state — seeded from prefs, written through on flip.
    var checkerEnabled by remember { mutableStateOf(AppPreferences.isUpdateCheckerEnabled(context)) }

    fun runCheck() {
        if (checkState == UpdateCheckUi.Checking) return
        checkState = UpdateCheckUi.Checking
        resultVisible = false
        notesExpanded = false
        downloadState = UpdateDownloadUi.Idle
        downloadIndeterminate = true
        scope.launch {
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
            .background(heroPageBackground(lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
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
                    // v115 — the update rows sit in the shared settings card
                    // so the page reads as settings options, not transparent
                    // rows floating on the backdrop.
                    CurioSettingsCard(shadowElevation = 0.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Version readout.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    text = "${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}",
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
                        CurioSettingsDivider()
                        // Opt-in update checker toggle — off by default (Curio
                        // is offline-first; the background check costs data).
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.Notifications, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 21.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Update checker", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (checkerEnabled) "Checks for new versions on app open"
                                    else "Off — check manually to save data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = checkerEnabled,
                                onCheckedChange = { enabled ->
                                    checkerEnabled = enabled
                                    AppPreferences.setUpdateCheckerEnabled(context, enabled)
                                },
                                colors = SwitchDefaults.colors()
                            )
                        }
                    }
                    }
                }
                item { CurioSectionLabel("Need help?") }
                item {
                    CurioSettingsCard(shadowElevation = 0.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CurioSettingsRow(
                            CurioIcons.Info,
                            "Support & diagnostics",
                            "Reports, crash logs & app details"
                        ) {
                            navController.navigate(com.curio.app.navigation.CurioRoutes.SUPPORT) {
                                launchSingleTop = true
                            }
                        }
                        CurioSettingsDivider()
                        CurioSettingsInfoRow(
                            CurioIcons.Download,
                            "How updates work",
                            "Updates install the latest release from GitHub"
                        )
                    }
                    }
                }
            }
        }
        // ── Torn rose hero on top — rows disappear under the tear.
        SettingsHeroHeader(
            title = "Updates",
            subtitle = "Your build and what's new",
            onBack = { navController.popBackStack() }
        )
    }
}

/** The update result card — up-to-date / update-available / failed, with a
 *  release-notes preview when the API returned them (mirrors the old Support
 *  card; lives on the dedicated Updates page since v112). */
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
            curioSageInk() to "You're on the latest version"
        UpdateCheckUi.GithubAvailable ->
            curioRoseInk() to "New version: ${info?.tagName ?: ""}"
        else ->
            MaterialTheme.colorScheme.error to "Couldn't check for updates"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        // v27n — opaque tinted fill (was 10% alpha, which let the elevation
        // shadow bleed through).
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, tint, 0.10f),
        shadowElevation = 3.dp,
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
                Text(
                    if (notesExpanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = curioRoseInk()
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
                            if (info?.apkUrl != null) {
                                Surface(
                                    onClick = onDownloadUpdate,
                                    shape = RoundedCornerShape(50),
                                    color = if (isCurioDarkTheme()) CurioColors.HomeRosewoodDark else CurioColors.CoralBlush,
                                    contentColor = if (isCurioDarkTheme()) CurioColors.CoralBlush else CurioColors.DeepPlum
                                ) {
                                    Text(
                                        "Update now",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                    )
                                }
                            }
                            Text(
                                "Open release",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = curioRoseInk()
                                ),
                                modifier = Modifier
                                    .clickable { info?.htmlUrl?.let { onOpenRelease(it) } }
                                    .padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        UpdateDownloadUi.Downloading -> {
                            Column {
                                if (downloadIndeterminate) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = curioRoseInk(),
                                        trackColor = curioRoseInk().copy(alpha = 0.20f)
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = curioRoseInk(),
                                        trackColor = curioRoseInk().copy(alpha = 0.20f)
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
                                color = curioRoseInk()
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

/** Update download/install states (download → system installer). */
private enum class UpdateDownloadUi { Idle, Downloading, Failed, InstallFailed }

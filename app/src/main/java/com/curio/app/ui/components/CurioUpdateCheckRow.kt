package com.curio.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.curio.app.BuildConfig
import com.curio.app.data.UpdateChecker
import com.curio.app.data.UpdateInfo
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.launch

/**
 * Shared "Check for updates" row for the About page and Profile.
 *
 * Tapping the row runs a manual update check against the latest GitHub
 * release (the authoritative build tag). At rest it shows the installed
 * version straight from the build — `VERSION_NAME` (the tag the APK was
 * built from, e.g. "1.0.0") plus `VERSION_CODE` (the per-build number) —
 * so the displayed version is always accurate. When a newer release is
 * found the row turns into a link that opens the release page in the
 * browser; any failure (offline, no release yet) shows a neutral
 * "couldn't check · tap to retry" state.
 */
@Composable
fun CurioUpdateCheckRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(UpdateCheckState.Idle) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    CurioSettingsRow(
        icon = CurioIcons.Download,
        title = "Check for updates",
        subtitle = when (state) {
            UpdateCheckState.Idle ->
                "Version ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}"
            UpdateCheckState.Checking -> "Checking for updates…"
            UpdateCheckState.UpToDate -> "You're up to date · v${BuildConfig.VERSION_NAME}"
            UpdateCheckState.UpdateAvailable -> "Update available: ${updateInfo?.tagName ?: ""}"
            UpdateCheckState.Failed -> "Couldn't check · tap to retry"
        },
        onClick = {
            when (state) {
                // An update is waiting — the row now opens the release page.
                UpdateCheckState.UpdateAvailable -> {
                    updateInfo?.htmlUrl?.let { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                }
                // Ignore taps while a check is already running — prevents
                // rapid double-taps from stacking parallel network calls.
                else -> if (state != UpdateCheckState.Checking) {
                    state = UpdateCheckState.Checking
                    scope.launch {
                        val info = UpdateChecker.fetchLatestRelease()
                        if (info == null || info.tagName.isBlank()) {
                            state = UpdateCheckState.Failed
                        } else if (UpdateChecker.isNewer(info.tagName, BuildConfig.VERSION_NAME)) {
                            updateInfo = info
                            state = UpdateCheckState.UpdateAvailable
                        } else {
                            state = UpdateCheckState.UpToDate
                        }
                    }
                }
            }
        }
    )
}

private enum class UpdateCheckState { Idle, Checking, UpToDate, UpdateAvailable, Failed }

package com.curio.app.features.updates

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.theme.isCurioDarkTheme
import java.io.File
import kotlinx.coroutines.launch
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.features.settings.SettingsHeroTotalHeight

/**
 * Updates — the dedicated sub-page for the in-app updater (v112). Own UI
 * instead of the old card inside Support & diagnostics: a settings-family
 * torn-rose hero on a watermark backdrop, with a status header (version
 * chip + live check state), the check-for-updates row, the release notes
 * rendered from the SAVED check result (v115 — the last successful check is
 * cached in [AppPreferences], so the notes show instantly on open instead
 * of reloading the network result), the download/install flow, and the
 * opt-in UPDATE CHECKER toggle (Curio is offline-first, so the background
 * check — which costs data every launch — is off until enabled here; the
 * manual check always works).
 */
@Composable
fun UpdatesScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkState by remember { mutableStateOf(UpdateCheckUi.Idle) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var resultVisible by remember { mutableStateOf(false) }
    // v115 — a background refresh that failed keeps the SAVED result on
    // screen and only marks the check row with a retry hint.
    var refreshFailed by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf(UpdateDownloadUi.Idle) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadIndeterminate by remember { mutableStateOf(true) }
    // Opt-in toggle state — seeded from prefs, written through on flip.
    var checkerEnabled by remember { mutableStateOf(AppPreferences.isUpdateCheckerEnabled(context)) }

    // v115 — the SAVED check result (tag, notes, links) loads instantly so
    // the page never reloads the network result on open.
    val cachedInfo = remember { AppPreferences.getCachedUpdateInfo(context) }

    fun applyResult(gh: UpdateInfo) {
        updateInfo = gh
        checkState = if (UpdateChecker.isNewer(gh.tagName, BuildConfig.VERSION_NAME)) {
            UpdateCheckUi.GithubAvailable
        } else {
            UpdateCheckUi.UpToDate
        }
    }

    fun runCheck(keepResult: Boolean = false) {
        if (checkState == UpdateCheckUi.Checking) return
        checkState = UpdateCheckUi.Checking
        refreshFailed = false
        if (!keepResult) {
            resultVisible = false
            downloadState = UpdateDownloadUi.Idle
            downloadIndeterminate = true
        }
        scope.launch {
            val gh = UpdateChecker.fetchLatestRelease()
            when {
                gh != null -> {
                    applyResult(gh)
                    // Save the fetched release (tag, notes, links) so the
                    // page — and every future open — shows the notes
                    // without another network call.
                    AppPreferences.setCachedUpdateInfo(context, gh)
                }
                else -> {
                    // A delegated property won't smart-cast — capture it.
                    val saved = updateInfo
                    if (saved != null) {
                        // Keep the saved result; just offer a retry.
                        applyResult(saved)
                        refreshFailed = true
                    } else {
                        checkState = UpdateCheckUi.Failed
                    }
                }
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

    // Open: show the SAVED notes immediately (no reload), then refresh
    // silently in the background — the refresh only replaces the result
    // when it succeeds.
    LaunchedEffect(Unit) {
        if (cachedInfo != null) {
            applyResult(cachedInfo)
            resultVisible = true
        }
        runCheck(keepResult = cachedInfo != null)
    }

    val checkSubtitle = when {
        checkState == UpdateCheckUi.Checking -> "Checking for updates…"
        refreshFailed -> "Couldn't refresh · tap to retry"
        checkState == UpdateCheckUi.UpToDate -> "You're up to date · v${BuildConfig.VERSION_NAME}"
        checkState == UpdateCheckUi.GithubAvailable -> "New version: ${updateInfo?.tagName ?: ""}"
        checkState == UpdateCheckUi.Failed -> "Couldn't check · tap to retry"
        else -> "Tap to check for the latest release"
    }

    // Status header treatment — accent tint follows the check state.
    val statusTint = when {
        checkState == UpdateCheckUi.GithubAvailable -> curioRoseInk()
        checkState == UpdateCheckUi.UpToDate -> curioSageInk()
        checkState == UpdateCheckUi.Failed -> MaterialTheme.colorScheme.error
        else -> curioRoseInk()
    }
    val statusGlyph = when (checkState) {
        UpdateCheckUi.UpToDate -> CurioIcons.Check
        UpdateCheckUi.Failed -> CurioIcons.Warning
        else -> CurioIcons.Download
    }
    val statusHeadline = when {
        checkState == UpdateCheckUi.GithubAvailable ->
            "New version ${updateInfo?.tagName?.removePrefix("v") ?: ""} ready"
        checkState == UpdateCheckUi.UpToDate -> "Curio is up to date"
        checkState == UpdateCheckUi.Failed -> "Couldn't check for updates"
        checkState == UpdateCheckUi.Checking -> "Checking for updates…"
        else -> "Welcome to Curio ${BuildConfig.VERSION_NAME}"
    }
    val statusSubline = when {
        checkState == UpdateCheckUi.GithubAvailable -> "The latest build is one tap away"
        checkState == UpdateCheckUi.Failed -> "You may be offline · tap Check to retry"
        checkState == UpdateCheckUi.Checking -> "Looking for the newest release"
        else -> "v${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}"
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
            val listState = rememberLazyListState()
            val glassBackdrop = rememberLayerBackdrop()
            LazyColumn(
                state = listState,
                modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
                // v255 — SCROLLING HERO: the banner is the list's first item
                // and scrolls away with the page (the Home/Profile way).
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = SettingsHeroTotalHeight,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { CurioSectionLabel("Updates") }
                // ── Status card — current version, check state, the
                //    update action (when one is available) and the checker
                //    toggle. The status header gives the page a real
                //    presence instead of a bare list of rows.
                item {
                    CurioSettingsCard(shadowElevation = 0.dp) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Status header — accent status dot + headline +
                            // the version chip.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(statusTint.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CurioIcon(
                                        statusGlyph, null,
                                        tint = statusTint,
                                        size = 22.dp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        statusHeadline,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    Text(
                                        statusSubline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = statusTint.copy(alpha = 0.12f),
                                    contentColor = statusTint
                                ) {
                                    Text(
                                        "v${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                            // ── Update action — only when a newer version
                            //    was found: an accent-tinted banner with the
                            //    Update now CTA (or the download progress /
                            //    retry states) plus the release-page link.
                            if (checkState == UpdateCheckUi.GithubAvailable && updateInfo != null) {
                                CurioSettingsDivider()
                                val info = updateInfo
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = lerp(
                                        MaterialTheme.colorScheme.surfaceContainerLow,
                                        curioRoseInk(),
                                        0.09f
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Update now",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                        when (downloadState) {
                                            UpdateDownloadUi.Idle -> {
                                                if (info?.apkUrl != null) {
                                                    Surface(
                                                        onClick = { info?.let { downloadAndInstall(it) } },
                                                        shape = RoundedCornerShape(50),
                                                        color = if (isCurioDarkTheme()) CurioColors.HomeRosewoodDark
                                                        else CurioColors.CoralBlush,
                                                        contentColor = if (isCurioDarkTheme()) CurioColors.CoralBlush
                                                        else CurioColors.DeepPlum
                                                    ) {
                                                        Text(
                                                            "Download & install",
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                                        )
                                                    }
                                                }
                                                info?.let {
                                                    Text(
                                                        "Open release on GitHub",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = curioRoseInk()
                                                        ),
                                                        modifier = Modifier
                                                            .clickable {
                                                                runCatching {
                                                                    context.startActivity(
                                                                        Intent(Intent.ACTION_VIEW, Uri.parse(it.htmlUrl))
                                                                    )
                                                                }
                                                            }
                                                            .padding(top = 2.dp, bottom = 2.dp)
                                                    )
                                                }
                                            }
                                            UpdateDownloadUi.Downloading -> {
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
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    if (downloadIndeterminate) "Downloading…"
                                                    else "Downloading… ${(downloadProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            UpdateDownloadUi.Failed, UpdateDownloadUi.InstallFailed -> {
                                                Text(
                                                    if (downloadState == UpdateDownloadUi.Failed)
                                                        "Download didn't finish · tap to retry"
                                                    else
                                                        "Couldn't open the installer · tap to retry",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.error
                                                    ),
                                                    modifier = Modifier
                                                        .clickable { info?.let { downloadAndInstall(it) } }
                                                        .padding(top = 4.dp, bottom = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            CurioSettingsDivider()
                            // Opt-in update checker toggle — off by default
                            // (Curio is offline-first; the background check
                            // costs data).
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
                                        else "Off · check manually to save data",
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
                // ── What's new — the release notes from the SAVED check
                //    result, rendered markdown-style (headers, bullets,
                //    bold). Appears once a check has succeeded (cached or
                //    fresh) so the page never re-fetches to show it.
                val notes = updateInfo?.releaseNotes?.takeIf { it.isNotBlank() }
                if (resultVisible && notes != null) {
                    item { CurioSectionLabel("What's new") }
                    item {
                        CurioSettingsCard(shadowElevation = 0.dp) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "In ${updateInfo?.tagName ?: "this version"}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                                ReleaseNotesBlock(notes = notes, accent = statusTint)
                                if (checkState == UpdateCheckUi.UpToDate && updateInfo != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "View release on GitHub",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = curioRoseInk()
                                        ),
                                        modifier = Modifier
                                            .clickable {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(updateInfo?.htmlUrl)
                                                        )
                                                    )
                                                }
                                            }
                                            .padding(top = 2.dp)
                                    )
                                }
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

            // RESTORED (user request) — STICKY HERO drawn on TOP of the scroll
            // content: rows slide under the ragged tear as they scroll up, and
            // the back pill refracts them through REAL liquid glass.
            SettingsHeroHeader(
                title = "Updates",
                        subtitle = "Your build and what's new",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }
}

/** One parsed line of the GitHub release-notes body (markdown-lite — the
 *  project carries no markdown dependency, so the common release-note
 *  syntax is parsed by hand: # headers, - bullets, --- dividers, **bold**
 *  and [label](url) links).
 *
 *  NOTE: never write a slash-star pair inside a KDoc — Kotlin block
 *  comments NEST, so a stray hyphen slash-star in this comment once
 *  opened a nested comment that swallowed this block's closing
 *  star-slash (and everything after it, including the trailing enum
 *  declarations) and broke the whole-file compile. */
private sealed interface NoteBlock {
    data class Header(val text: String) : NoteBlock
    data class Bullet(val text: String) : NoteBlock
    data class Paragraph(val text: String) : NoteBlock
    data object Divider : NoteBlock
}

/** One inline-styled segment — text + whether it was wrapped in **bold**. */
private data class NoteSpan(val text: String, val bold: Boolean)

private fun parseReleaseNotes(body: String): List<NoteBlock> =
    body.lineSequence()
        .map { it.trimEnd() }
        .mapNotNull { line ->
            when {
                line.isBlank() -> null
                line == "---" || line == "***" || line == "___" -> NoteBlock.Divider
                line.startsWith("#### ") -> NoteBlock.Header(line.removePrefix("#### ").trim())
                line.startsWith("### ") -> NoteBlock.Header(line.removePrefix("### ").trim())
                line.startsWith("## ") -> NoteBlock.Header(line.removePrefix("## ").trim())
                line.startsWith("# ") -> NoteBlock.Header(line.removePrefix("# ").trim())
                line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ") ->
                    NoteBlock.Bullet(line.drop(2).trim())
                else -> NoteBlock.Paragraph(line.trim())
            }
        }
        .toList()

/** Splits one line into bold/normal spans; strips markdown link syntax
 *  (`[label](url)` → label) and inline backticks. */
private fun parseInline(raw: String): List<NoteSpan> {
    val spans = mutableListOf<NoteSpan>()
    var bold = false
    val current = StringBuilder()
    fun flush() {
        if (current.isNotEmpty()) {
            spans += NoteSpan(current.toString(), bold)
            current.setLength(0)
        }
    }
    var i = 0
    while (i < raw.length) {
        val c = raw[i]
        when {
            // [label](url) → keep the label only.
            c == '[' -> {
                val close = raw.indexOf(']', i)
                val open = if (close > i) raw.indexOf('(', close) else -1
                val end = if (open == close + 1) raw.indexOf(')', open) else -1
                if (end > 0) {
                    current.append(raw.substring(i + 1, close))
                    i = end + 1
                } else {
                    current.append(c)
                    i++
                }
            }
            // ** toggles bold.
            c == '*' && i + 1 < raw.length && raw[i + 1] == '*' -> {
                flush()
                bold = !bold
                i += 2
            }
            // `code` → plain text (monospace isn't worth a font switch).
            c == '`' -> {
                val end = raw.indexOf('`', i + 1)
                if (end > i) {
                    current.append(raw.substring(i + 1, end))
                    i = end + 1
                } else {
                    current.append(c)
                    i++
                }
            }
            else -> {
                current.append(c)
                i++
            }
        }
    }
    flush()
    return spans
}

/** Renders the parsed note text with its bold spans. */
@Composable
private fun NoteSpanText(spans: List<NoteSpan>, style: TextStyle, color: Color) {
    val annotated = remember(spans) {
        buildAnnotatedString {
            spans.forEach { span ->
                withStyle(if (span.bold) SpanStyle(fontWeight = FontWeight.Bold) else SpanStyle()) {
                    append(span.text)
                }
            }
        }
    }
    Text(annotated, style = style, color = color)
}

/** The release notes body, rendered markdown-style: headers as bold title
 *  lines, bullets with accent dots, `---` as hairline dividers. */
@Composable
private fun ReleaseNotesBlock(notes: String, accent: Color) {
    val blocks = remember(notes) { parseReleaseNotes(notes) }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        blocks.forEach { block ->
            when (block) {
                is NoteBlock.Header -> Text(
                    block.text,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                is NoteBlock.Bullet -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    NoteSpanText(
                        parseInline(block.text),
                        MaterialTheme.typography.bodySmall,
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is NoteBlock.Paragraph -> NoteSpanText(
                    parseInline(block.text),
                    MaterialTheme.typography.bodySmall,
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
                NoteBlock.Divider -> HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }    }
}

private enum class UpdateCheckUi { Idle, Checking, UpToDate, GithubAvailable, Failed }

/** Update download/install states (download → system installer). */
private enum class UpdateDownloadUi { Idle, Downloading, Failed, InstallFailed }

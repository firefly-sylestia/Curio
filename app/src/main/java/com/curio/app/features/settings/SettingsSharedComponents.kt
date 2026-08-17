package com.curio.app.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioQuality
import com.curio.app.data.MusicService
import com.curio.app.data.SearchEngine
import com.curio.app.data.VoskModelDownloads
import com.curio.app.data.VoskModels
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.brandRes
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor

/**
 * v27q — content ink that reads on the SOLID [curioDialogActionColor]
 * selected-row fill. v78 — light Curio only: white on the rose/primary
 * rows (the AMOLED black flip is gone with dark mode).
 */
@Composable
private fun dialogRowSelectedInk(): Color = Color.White

@Composable
fun AudioQualityDialog(
    currentQuality: AudioQuality,
    onDismiss: () -> Unit,
    onSelected: (AudioQuality) -> Unit
) {
    AlertDialog(
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        onDismissRequest = onDismiss,
        title = { Text("Recording quality", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Higher quality sounds clearer but uses more storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AudioQuality.entries.forEach { quality ->
                    val selected = quality == currentQuality
                    Surface(
                        onClick = { onSelected(quality) },
                        shape = RoundedCornerShape(16.dp),
                        // v27q — selection reads as a SOLID action fill with
                        // readable on-fill content; unselected rows wear an
                        // opaque surface so the flat 2dp shadow renders
                        // cleanly behind every row.
                        color = if (selected) curioDialogActionColor()
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) dialogRowSelectedInk()
                                       else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            // v28 — dark mode elevation visibility.
                            .curioDarkGlow(2.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (selected) dialogRowSelectedInk()
                                                   else curioDialogActionColor()
                                )
                            )
                            Column {
                                Text(quality.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    quality.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) dialogRowSelectedInk().copy(alpha = 0.8f)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) { Text("Close", fontWeight = FontWeight.Bold) }
        }
    )
}

/**
 * v19 — single-choice picker for the explore search engine (Settings →
 * Notifications → Search engine). Mirrors [AudioQualityDialog]'s styling so
 * the picker feels native to the settings section.
 */
@Composable
fun SearchEngineDialog(
    current: SearchEngine,
    onDismiss: () -> Unit,
    onSelected: (SearchEngine) -> Unit
) {
    AlertDialog(
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        onDismissRequest = onDismiss,
        title = { Text("Search engine", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Which search engine the \"Explore in browser\" button searches with.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SearchEngine.entries.forEach { engine ->
                    val selected = engine == current
                    Surface(
                        onClick = { onSelected(engine) },
                        shape = RoundedCornerShape(16.dp),
                        // v27q — see the audio-quality rows above: solid
                        // action fill, flat 2dp shadow behind every row.
                        color = if (selected) curioDialogActionColor()
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) dialogRowSelectedInk()
                                       else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            // v28 — dark mode elevation visibility.
                            .curioDarkGlow(2.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (selected) dialogRowSelectedInk()
                                                   else curioDialogActionColor()
                                )
                            )
                            Column {
                                Text(engine.displayName, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    engine.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) dialogRowSelectedInk().copy(alpha = 0.8f)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) { Text("Close", fontWeight = FontWeight.Bold) }
        }
    )
}

/**
 * v27s — single-choice picker for the explore music service (Settings →
 * Notifications → Music service). Mirrors [SearchEngineDialog]'s styling so
 * the picker feels native to the settings section.
 */
@Composable
fun MusicServiceDialog(
    current: MusicService,
    onDismiss: () -> Unit,
    onSelected: (MusicService) -> Unit
) {
    AlertDialog(
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        onDismissRequest = onDismiss,
        title = { Text("Music service", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    // v109 — the pill says "Listen in" for audio services
                    // (Apple Music / Spotify / YouTube Music) and "Watch in"
                    // for YouTube, so the subtitle stays neutral.
                    "Which streaming service opens albums, artists and songs from the explore dialog.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MusicService.entries.forEach { service ->
                    val selected = service == current
                    Surface(
                        onClick = { onSelected(service) },
                        shape = RoundedCornerShape(16.dp),
                        // v27q — see the audio-quality rows above: solid
                        // action fill, flat 2dp shadow behind every row.
                        color = if (selected) curioDialogActionColor()
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) dialogRowSelectedInk()
                                       else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            // v28 — dark mode elevation visibility.
                            .curioDarkGlow(2.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // v106 — the service's brand logo (keeps its own
                            // brand colors; never tinted).
                            Image(
                                painter = painterResource(service.brandRes),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            // v109 — no radio indicator: selection reads
                            // through the row's solid fill (v27q) alone.
                            Column {
                                Text(service.displayName, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    service.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) dialogRowSelectedInk().copy(alpha = 0.8f)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) { Text("Close", fontWeight = FontWeight.Bold) }
        }
    )
}

/**
 * v125 — the OFFLINE voice-to-text model picker (Settings → Recording →
 * Offline model). Each catalog model ([VoskModels.CATALOG]) shows its
 * quality + size; tapping a downloaded model selects it, Download fetches
 * + extracts it in-app with a live progress bar, and Delete removes a
 * downloaded model. This model powers the "Transcribe voice note" action
 * for pre-recorded sound bites on the entry detail page — fully on-device,
 * no network at transcription time.
 */
@Composable
fun OfflineModelDialog(
    currentModelId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // v137 — download state lives in the app-scoped VoskModelDownloads
    // manager, so a transfer keeps running after this sheet closes; the
    // rows below observe it for per-model progress / pause / resume.
    val downloadStates by VoskModelDownloads.states.collectAsState()
    // v125 — the offline model version is bumped by download/delete, so
    // re-reading the installed state below recomposes with fresh data.
    AppPreferences.offlineModelVersionState
    // v136 — the picker was an AlertDialog whose fixed max height squeezed
    // the rows and clipped the bottom of the list ("squished, can't see
    // below"); it's now a FULL-HEIGHT ModalBottomSheet with a scrolling
    // list so all seven models fit with breathing room.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioDialogContainerColor(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Offline model",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    CurioIcon(
                        CurioIcons.Close,
                        "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            Text(
                "The offline model turns pre-recorded voice notes into text on your device. Small models are fast and light; the Large and Full models are much more accurate but are heavy downloads that need real storage and memory. Downloads keep running if you close this screen — pause or cancel them anytime, and several can download at once. No internet needed while it runs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
            // The model list — scrolls within the full-height sheet (the
            // old dialog capped the height and clipped the bottom rows).
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(VoskModels.CATALOG, key = { it.id }) { model ->
                    val downloaded = VoskModels.isDownloaded(context, model.id)
                    val selected = downloaded && model.id == currentModelId
                    val dl = downloadStates[model.id]
                    val isDownloading = dl?.status == VoskModelDownloads.Status.Downloading
                    val isPaused = dl?.status == VoskModelDownloads.Status.Paused
                    val failed = dl?.status == VoskModelDownloads.Status.Failed
                    Surface(
                        onClick = { if (downloaded && !isDownloading) AppPreferences.setOfflineModelId(context, model.id) },
                        enabled = downloaded && !isDownloading,
                        shape = RoundedCornerShape(16.dp),
                        // v27q — selection reads as a SOLID action fill;
                        // unselected rows wear an opaque surface.
                        color = if (selected) curioDialogActionColor()
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) dialogRowSelectedInk()
                                       else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .curioDarkGlow(2.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (downloaded) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = if (selected) dialogRowSelectedInk()
                                                       else curioDialogActionColor()
                                    )
                                )
                            } else {
                                CurioIcon(
                                    name = CurioIcons.Download,
                                    contentDescription = null,
                                    tint = if (isDownloading) MaterialTheme.colorScheme.onSurfaceVariant
                                           else curioDialogActionColor(),
                                    size = 22.dp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.displayName, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    "${model.langLabel} · ${model.sizeLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) dialogRowSelectedInk().copy(alpha = 0.8f)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isDownloading || isPaused) {
                                    LinearProgressIndicator(
                                        progress = { dl?.progress ?: 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        color = if (selected) dialogRowSelectedInk()
                                                else curioDialogActionColor(),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                                if (failed) {
                                    Text(
                                        text = dl?.error.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            when {
                                isDownloading || isPaused -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${((dl?.progress ?: 0f) * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) dialogRowSelectedInk()
                                                   else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        // Pause ⇄ Resume — resume continues from the partial file.
                                        Surface(
                                            onClick = {
                                                if (isPaused) VoskModelDownloads.resume(model.id)
                                                else VoskModelDownloads.pause(model.id)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) dialogRowSelectedInk().copy(alpha = 0.18f)
                                                   else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            CurioIcon(
                                                name = if (isPaused) CurioIcons.PlayArrow else CurioIcons.Pause,
                                                contentDescription = if (isPaused) "Resume ${model.displayName}" else "Pause ${model.displayName}",
                                                tint = if (selected) dialogRowSelectedInk()
                                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                                size = 16.dp,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        // Cancel — the only thing that stops a download for good;
                                        // closing this sheet keeps it running in the background.
                                        Surface(
                                            onClick = { VoskModelDownloads.cancel(context, model.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) dialogRowSelectedInk().copy(alpha = 0.18f)
                                                   else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            CurioIcon(
                                                name = CurioIcons.Close,
                                                contentDescription = "Cancel download ${model.displayName}",
                                                tint = if (selected) dialogRowSelectedInk()
                                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                                size = 16.dp,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                }
                                downloaded -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (selected) "In use" else "Downloaded",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (selected) dialogRowSelectedInk()
                                                   else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            onClick = {
                                                VoskModels.deleteModel(context, model.id)
                                                if (AppPreferences.getOfflineModelId(context) == model.id) {
                                                    AppPreferences.setOfflineModelId(context, "")
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) dialogRowSelectedInk().copy(alpha = 0.18f)
                                                   else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            CurioIcon(
                                                name = CurioIcons.Delete,
                                                contentDescription = "Delete ${model.displayName}",
                                                tint = if (selected) dialogRowSelectedInk()
                                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                                size = 16.dp,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                }
                                failed -> {
                                    TextButton(
                                        onClick = { VoskModelDownloads.start(context, model) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            "Retry",
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) dialogRowSelectedInk() else curioDialogActionColor()
                                        )
                                    }
                                }
                                else -> {
                                    TextButton(
                                        onClick = { VoskModelDownloads.start(context, model) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            "Download",
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) dialogRowSelectedInk() else curioDialogActionColor()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

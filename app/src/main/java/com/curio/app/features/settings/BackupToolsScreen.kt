package com.curio.app.features.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioBackupManager
import com.curio.app.data.CurioCategories
import com.curio.app.data.FieldMindArchivePreview
import com.curio.app.data.FieldMindLegacyImport
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsInfoRow
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

/** Dedicated data workspace for Curio backups and additive FieldMind import. */
@Composable
fun BackupToolsScreen(navController: NavController) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val scope = rememberCoroutineScope()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var lastBackupAt by remember { mutableStateOf(CurioBackupManager.lastBackupAtMillis(context)) }
    var legacyPreview by remember { mutableStateOf<FieldMindArchivePreview?>(null) }
    var legacyPendingUri by remember { mutableStateOf<Uri?>(null) }
    var legacyBusy by remember { mutableStateOf(false) }
    var legacyStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(CurioBackupManager.MIME_TYPE)
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val result = CurioBackupManager.export(context, uri)
                    lastBackupAt = CurioBackupManager.lastBackupAtMillis(context)
                    backupStatus = true to (
                        "Backed up ${result.captureCount} capture(s), your settings and sound recordings.\n" +
                            "Keep the file somewhere safe. It brings everything back on a new device."
                        )
                } catch (e: Exception) {
                    backupStatus = false to "Backup failed: ${e.message ?: "unknown error"}"
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val result = CurioBackupManager.restore(context, uri)
                    com.curio.app.data.AppPreferences.initThemeMode(context)
                    backupStatus = true to
                        "Restored ${result.captureCount} capture(s), your settings and sound recordings."
                } catch (e: Exception) {
                    backupStatus = false to "Restore failed: ${e.message ?: "unknown error"}"
                }
            }
        }
    }

    val legacyPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !legacyBusy) {
            scope.launch {
                legacyBusy = true
                try {
                    legacyPreview = FieldMindLegacyImport.preview(context, uri)
                    legacyPendingUri = uri
                } catch (e: Exception) {
                    legacyStatus = false to (
                        e.message?.let { "Couldn't read that file: $it" }
                            ?: "That file doesn't look like a FieldMind archive."
                        )
                } finally {
                    legacyBusy = false
                }
            }
        }
    }

    if (legacyPreview != null && legacyPendingUri != null) {
        val preview = legacyPreview!!
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { legacyPreview = null; legacyPendingUri = null },
            title = { Text("Import FieldMind data?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "Found ${preview.observations} observation" +
                        (if (preview.observations == 1) "" else "s") +
                        ", ${preview.notes} note" +
                        (if (preview.notes == 1) "" else "s") +
                        " and ${preview.images} image" +
                        (if (preview.images == 1) "" else "s") +
                        ". ${preview.species} species " +
                        (if (preview.species == 1) "entry" else "entries") +
                        " go to the saved catalog.\n\n" +
                        "They'll be added to your Cabinet as legacy entries. Nothing currently in Curio is touched."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = legacyPendingUri
                    legacyPreview = null
                    legacyPendingUri = null
                    if (uri != null) {
                        scope.launch {
                            legacyBusy = true
                            try {
                                val result = FieldMindLegacyImport.restore(context, uri)
                                legacyStatus = true to buildString {
                                    append("Imported ${result.observations} observation")
                                    if (result.observations != 1) append("s")
                                    append(", ${result.notes} note")
                                    if (result.notes != 1) append("s")
                                    append(" and ${result.images} image")
                                    if (result.images != 1) append("s")
                                    append(". ${result.species} species saved to the catalog.")
                                    if (result.skipped > 0) {
                                        append(" Skipped ${result.skipped} already-imported record")
                                        if (result.skipped != 1) append("s")
                                        append(".")
                                    }
                                }
                            } catch (e: Exception) {
                                legacyStatus = false to "Import failed: ${e.message ?: "unknown error"}"
                            } finally {
                                legacyBusy = false
                            }
                        }
                    }
                },
                    colors = curioDialogActionButtonColors()
                ) { Text("Import", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { legacyPreview = null; legacyPendingUri = null },
                    colors = curioDialogActionButtonColors()
                ) { Text("Cancel") }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore backup?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("This replaces all of your current captures and settings with the contents of the backup file. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        restoreLauncher.launch(arrayOf(CurioBackupManager.MIME_TYPE))
                    },
                    colors = curioDialogActionButtonColors()
                ) { Text("Continue", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }, colors = curioDialogActionButtonColors()) { Text("Cancel") }
            }
        )
    }

    legacyStatus?.let { (success, message) ->
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { legacyStatus = null },
            title = { Text(if (success) "Done" else "Couldn't do that", fontWeight = FontWeight.ExtraBold) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { legacyStatus = null }, colors = curioDialogActionButtonColors()) { Text("OK", fontWeight = FontWeight.Bold) }
            }
        )
    }

    backupStatus?.let { (success, message) ->
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { backupStatus = null },
            title = { Text(if (success) "Done" else "Couldn't do that", fontWeight = FontWeight.ExtraBold) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { backupStatus = null }, colors = curioDialogActionButtonColors()) { Text("OK", fontWeight = FontWeight.Bold) }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs behind the content
        // (wildcard sparkle leads; the data workspace is category-neutral).
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
            item { CurioSectionLabel("Your data") }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CurioCardHeader(CurioIcons.Backup, "Backup & restore", "A complete, portable copy of Curio")
                    CurioSettingsRow(CurioIcons.Backup, "Back up now", "Save captures, settings + recordings") {
                        backupLauncher.launch(CurioBackupManager.suggestedFileName())
                    }
                    CurioSettingsDivider()
                    CurioSettingsRow(CurioIcons.Restore, "Restore from backup", "Replace current data from a file") {
                        showRestoreConfirm = true
                    }
                    CurioSettingsDivider()
                    val backupLabel = if (lastBackupAt > 0L) {
                        SimpleDateFormat("MMM d, yyyy · h:mm a", locale).format(Date(lastBackupAt))
                    } else "Never"
                    CurioSettingsInfoRow(CurioIcons.History, "Last backup", backupLabel)
                }
            }
            item { CurioSectionLabel("Legacy import") }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CurioCardHeader(CurioIcons.History, "FieldMind archive", "Add older observations to your Cabinet")
                    CurioSettingsRow(
                        CurioIcons.History,
                        "Restore from FieldMind backup",
                        if (legacyBusy) "Reading archive…" else "Import observations, notes + species"
                    ) {
                        legacyPickerLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "application/json",
                                "application/octet-stream",
                                "application/x-zip-compressed"
                            )
                        )
                    }
                    CurioSettingsDivider()
                    CurioSettingsInfoRow(CurioIcons.Info, "Additive import", "Existing Curio captures are never replaced")
                }
            }
        }
        // Drawn on top of the scroll content — rows slide under the ragged
        // tear as they scroll up.
        SettingsHeroHeader(
            title = "Backup & restore",
            subtitle = "Keep your captures safe",
            onBack = { navController.popBackStack() }
        )
    }
}

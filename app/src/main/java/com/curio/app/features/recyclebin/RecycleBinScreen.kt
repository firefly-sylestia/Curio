package com.curio.app.features.recyclebin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioStorageManager
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.shortName
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.ImageStorageManager
import com.curio.app.data.RecycleBinExpiry
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioEmptyState
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.themedAccent
import kotlinx.coroutines.launch
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.features.settings.SettingsHeroTotalHeight

/**
 * Recycle bin (v302) — full redesign matching concept UI.
 *
 * Features:
 * - Summary card with item count, total size, and auto-delete badge
 * - "RECENTLY DELETED" section header with Select all + Empty bin actions
 * - Item rows with format icon, topic name, deletion date, and format label
 * - Multi-select mode with checkboxes and a floating bottom action bar
 * - Redesigned empty bin confirmation dialog with trash icon and red CTA
 */
@Composable
fun RecycleBinScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val trashed by produceState<List<CurioEntry>>(initialValue = emptyList()) {
        try {
            CurioRepositoryHolder.repo.observeTrashed().collect { value = it }
        } catch (_: Exception) {
            value = emptyList()
        }
    }
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()

    // Selection state
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val multiSelectMode = selectedIds.isNotEmpty()

    // Dialogs
    var purgeTarget by remember { mutableStateOf<CurioEntry?>(null) }
    var showEmptyBinConfirm by remember { mutableStateOf(false) }
    var showExpiryDialog by remember { mutableStateOf(false) }
    val expiryDays = AppPreferences.recycleBinExpiryDaysState

    LaunchedEffect(Unit) { RecycleBinExpiry.purgeExpired(context) }

    fun purgeWithMedia(entry: CurioEntry) {
        scope.launch {
            entry.captureData.audioFilePaths().forEach { path ->
                AudioStorageManager.deleteAudio(context, path)
            }
            ImageStorageManager.deleteImagesForEntry(context, entry.id)
            runCatching { CurioRepositoryHolder.repo.purgeById(entry.id) }
        }
    }

    fun purgeSelected() {
        scope.launch {
            val toPurge = trashed.filter { it.id in selectedIds }
            toPurge.forEach { entry ->
                entry.captureData.audioFilePaths().forEach { path ->
                    AudioStorageManager.deleteAudio(context, path)
                }
                ImageStorageManager.deleteImagesForEntry(context, entry.id)
            }
            runCatching { CurioRepositoryHolder.repo.purgeTrashed() }
            selectedIds = emptySet()
        }
    }

    fun restoreSelected() {
        scope.launch {
            selectedIds.forEach { id ->
                runCatching { CurioRepositoryHolder.repo.restoreById(id) }
            }
            selectedIds = emptySet()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(heroPageBackground())
    ) {
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }

        ScreenEntrance {
            if (trashed.isEmpty()) {
                Column {
                    SettingsHeroHeader(
                        title = "Recycle bin",
                        subtitle = "Recently deleted captures",
                        onBack = { navController.popBackStack() }
                    )
                    CurioEmptyState(
                        glyph = CurioIcons.Restore,
                        headline = "Recycle bin is empty",
                        subtext = "Deleted captures wait here so you can bring them back — nothing is lost yet.",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .layerBackdrop(glassBackdrop)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = SettingsHeroTotalHeight,
                        bottom = if (multiSelectMode) 88.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Summary card ─────────────────────────────────
                    item("summary") {
                        BinSummaryCard(
                            count = trashed.size,
                            expiryDays = expiryDays,
                            onExpiryClick = { showExpiryDialog = true }
                        )
                    }

                    // ── Section header: RECENTLY DELETED + actions ───
                    item("section-header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENTLY DELETED",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            // Select all
                            Text(
                                text = if (multiSelectMode && selectedIds.size == trashed.size)
                                    "Deselect all" else "Select all",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    selectedIds = if (selectedIds.size == trashed.size)
                                        emptySet()
                                    else trashed.map { it.id }.toSet()
                                }
                            )
                            Spacer(Modifier.width(16.dp))
                            // Empty bin
                            Text(
                                text = "Empty bin",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable {
                                    showEmptyBinConfirm = true
                                }
                            )
                        }
                    }

                    // ── Item rows ────────────────────────────────────
                    items(trashed, key = { it.id }) { entry ->
                        TrashedEntryRow(
                            entry = entry,
                            selected = entry.id in selectedIds,
                            onToggleSelect = {
                                selectedIds = if (entry.id in selectedIds)
                                    selectedIds - entry.id
                                else selectedIds + entry.id
                            },
                            onRestore = {
                                scope.launch {
                                    runCatching {
                                        CurioRepositoryHolder.repo.restoreById(entry.id)
                                    }
                                }
                            },
                            onDeleteForever = { purgeTarget = entry }
                        )
                    }
                }
            }
        }

        // Scroll indicator
        if (trashed.isNotEmpty()) {
            CurioVerticalScrollIndicator(
                state = listState.scrollIndicatorState,
                onScrollBy = { listState.dispatchRawDelta(it) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 10.dp, bottom = 16.dp)
            )
        }

        // Sticky hero
        SettingsHeroHeader(
            title = "Recycle bin",
            subtitle = if (trashed.isEmpty()) "Recently deleted captures"
            else "${trashed.size} items in recycle bin",
            onBack = { navController.popBackStack() },
            glassBackdrop = glassBackdrop
        )

        // ── Multi-select bottom bar ──────────────────────────────────
        AnimatedVisibility(
            visible = multiSelectMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionBottomBar(
                selectedCount = selectedIds.size,
                onDeleteForever = { showEmptyBinConfirm = true },
                onRestore = { restoreSelected() }
            )
        }
    }

    // ── Delete forever (single entry) dialog ──────────────────────────
    purgeTarget?.let { entry ->
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { purgeTarget = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            size = 28.dp
                        )
                    }
                }
            },
            title = {
                Text(
                    "Delete forever?",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "\"${entry.topic.name}\" and its attached media will be permanently erased. This can't be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Surface(
                    onClick = {
                        purgeTarget = null
                        purgeWithMedia(entry)
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Delete forever",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { purgeTarget = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // ── Empty bin dialog ──────────────────────────────────────────────
    if (showEmptyBinConfirm) {
        val count = if (multiSelectMode) selectedIds.size else trashed.size
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { showEmptyBinConfirm = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            size = 28.dp
                        )
                    }
                }
            },
            title = {
                Text(
                    "Empty recycle bin?",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "All $count item(s) will be permanently deleted. You can't undo this action.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Surface(
                    onClick = {
                        showEmptyBinConfirm = false
                        if (multiSelectMode) {
                            purgeSelected()
                        } else {
                            scope.launch {
                                trashed.forEach { entry ->
                                    entry.captureData.audioFilePaths().forEach { path ->
                                        AudioStorageManager.deleteAudio(context, path)
                                    }
                                    ImageStorageManager.deleteImagesForEntry(context, entry.id)
                                }
                                runCatching { CurioRepositoryHolder.repo.purgeTrashed() }
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Empty bin · $count items",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEmptyBinConfirm = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // ── Auto-delete window picker ─────────────────────────────────────
    if (showExpiryDialog) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { showExpiryDialog = false },
            title = { Text("Auto-delete after", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Captures left in the recycle bin past this window are deleted forever (with their media).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        0 to "Keep forever",
                        7 to "7 days",
                        30 to "30 days",
                        90 to "90 days"
                    ).forEach { (days, label) ->
                        val selected = days == expiryDays
                        Surface(
                            onClick = {
                                AppPreferences.setRecycleBinExpiryDays(context, days)
                                showExpiryDialog = false
                                scope.launch { RecycleBinExpiry.purgeExpired(context) }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
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
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExpiryDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ── Summary card ──────────────────────────────────────────────────────

@Composable
private fun BinSummaryCard(
    count: Int,
    expiryDays: Int,
    onExpiryClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trash icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        size = 22.dp
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$count items in Recycle bin",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Auto-delete in ${expiryLabel(expiryDays)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Expiry badge
            Surface(
                onClick = onExpiryClick,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = expiryLabel(expiryDays),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ── Item row ──────────────────────────────────────────────────────────

@Composable
private fun TrashedEntryRow(
    entry: CurioEntry,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val category = CurioCategories.byId(entry.topic.categoryId)
    val accent = category.themedAccent()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Format icon
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        name = formatIcon(entry.format),
                        contentDescription = null,
                        tint = accent,
                        size = 20.dp
                    )
                }
            }
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.topic.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${entry.format.shortName} · deleted ${deletedDaysAgoLabel(entry.deletedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Checkbox
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onToggleSelect() }
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    border = if (!selected) BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.outline
                    ) else null,
                    modifier = Modifier.size(24.dp)
                ) {
                    if (selected) {
                        Box(contentAlignment = Alignment.Center) {
                            CurioIcon(
                                CurioIcons.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                size = 14.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Selection bottom bar ──────────────────────────────────────────────

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    onDeleteForever: () -> Unit,
    onRestore: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selected count
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // Delete forever
            Surface(
                onClick = onDeleteForever,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    CurioIcon(
                        CurioIcons.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        size = 18.dp
                    )
                    Text(
                        "Delete forever",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            // Restore
            Surface(
                onClick = onRestore,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    CurioIcon(
                        CurioIcons.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 18.dp
                    )
                    Text(
                        "Restore",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────

private fun formatIcon(format: com.curio.app.data.CaptureFormat): String = when (format) {
    com.curio.app.data.CaptureFormat.ReelNotes -> CurioIcons.FormatQuote
    com.curio.app.data.CaptureFormat.Marginalia -> CurioIcons.FormatQuote
    com.curio.app.data.CaptureFormat.SoundBite -> CurioIcons.Mic
    com.curio.app.data.CaptureFormat.GalleryWall -> CurioIcons.Image
    com.curio.app.data.CaptureFormat.FieldNotes -> CurioIcons.Edit
    com.curio.app.data.CaptureFormat.OpenNotebook -> CurioIcons.AutoAwesome
}

private fun expiryLabel(days: Int): String =
    if (days <= 0) "Keep forever" else "$days days"

private fun deletedDaysAgoLabel(deletedAt: Long?): String {
    if (deletedAt == null) return "recently"
    val days = ((System.currentTimeMillis() - deletedAt) / 86_400_000L).toInt()
    return when (days) {
        0 -> "today"
        1 -> "yesterday"
        else -> "${days}d ago"
    }
}

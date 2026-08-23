package com.curio.app.features.recyclebin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioStorageManager
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
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
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.themedAccent
import kotlinx.coroutines.launch

/**
 * Recycle bin (v26) — every soft-deleted capture lands here instead of being
 * erased. The user can restore entries back to the Cabinet or permanently
 * delete them (which is when attached media is finally removed). Opened from
 * Settings → Safety & support → Recycle bin.
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
    // Single-confirm dialogs for the permanent actions (already in the bin).
    var purgeTarget by remember { mutableStateOf<CurioEntry?>(null) }
    var showEmptyBinConfirm by remember { mutableStateOf(false) }
    // v27 — customizable auto-delete window; expired captures are purged on
    // open (below) and on app start (MainActivity).
    var showExpiryDialog by remember { mutableStateOf(false) }
    val expiryDays = AppPreferences.recycleBinExpiryDaysState
    LaunchedEffect(Unit) { RecycleBinExpiry.purgeExpired(context) }

    fun purgeWithMedia(entry: CurioEntry) {
        scope.launch {
            // Permanent: only now are the recording + images finally removed.
            entry.captureData.audioFilePaths().forEach { path ->
                AudioStorageManager.deleteAudio(context, path)
            }
            ImageStorageManager.deleteImagesForEntry(context, entry.id)
            runCatching { CurioRepositoryHolder.repo.purgeById(entry.id) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the lane wash.
            .background(heroPageBackground())
    ) {
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        ScreenEntrance {
            // v255 — SCROLLING HERO (the Home/Profile construction): the
            // banner leads the page — as the empty state's top block or the
            // list's first item — and scrolls away with it.
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = 10.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item("bin-hero") {
                        SettingsHeroHeader(
                            title = "Recycle bin",
                            subtitle = if (trashed.isEmpty()) "Recently deleted captures" else "${trashed.size} capture(s) awaiting you",
                            onBack = { navController.popBackStack() }
                        )
                    }
                    item("bin-controls") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${trashed.size} capture(s) waiting. Restore them here, or delete forever.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { showEmptyBinConfirm = true }
                            ) {
                                Text(
                                    "Empty bin",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    item("bin-expiry") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto-delete after",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showExpiryDialog = true }) {
                                Text(
                                    expiryLabel(expiryDays),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    items(trashed, key = { it.id }) { entry ->
                        TrashedEntryRow(
                            entry = entry,
                            onRestore = {
                                scope.launch {
                                    runCatching { CurioRepositoryHolder.repo.restoreById(entry.id) }
                                }
                            },
                            onDeleteForever = { purgeTarget = entry }
                        )
                    }
                }
            }
        }
        if (trashed.isEmpty()) {
            TextButton(
                onClick = { showExpiryDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Auto-delete after ${expiryLabel(expiryDays)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    }

    // ── Delete forever (single entry) ──────────────────────────────────
    purgeTarget?.let { entry ->
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { purgeTarget = null },
            title = { Text("Delete forever?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${entry.topic.name}\" and its attached media will be permanently erased. " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    purgeTarget = null
                    purgeWithMedia(entry)
                }) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { purgeTarget = null }, colors = curioDialogActionButtonColors()) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Empty bin — purges everything, then removes all attached media ──
    if (showEmptyBinConfirm) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { showEmptyBinConfirm = false },
            title = { Text("Empty the Recycle bin?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${trashed.size} capture(s) — and their attached media — will be permanently erased. " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyBinConfirm = false
                    scope.launch {
                        trashed.forEach { entry ->
                            entry.captureData.audioFilePaths().forEach { path ->
                                AudioStorageManager.deleteAudio(context, path)
                            }
                            ImageStorageManager.deleteImagesForEntry(context, entry.id)
                        }
                        runCatching { CurioRepositoryHolder.repo.purgeTrashed() }
                    }
                }) {
                    Text("Empty bin", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinConfirm = false }, colors = curioDialogActionButtonColors()) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Auto-delete window picker ─────────────────────────────────────────
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
                                // Apply the new window right away.
                                scope.launch { RecycleBinExpiry.purgeExpired(context) }
                            },
                            shape = RoundedCornerShape(16.dp),
                            // v27q — selection reads as a SOLID action fill;
                            // unselected rows wear an opaque surface so the
                            // flat 2dp shadow renders cleanly behind every
                            // row.
                            color = if (selected) curioDialogActionColor()
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) recycleRowSelectedInk()
                                           else MaterialTheme.colorScheme.onSurface,
                            shadowElevation = 2.dp,
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
                                        selectedColor = if (selected) recycleRowSelectedInk()
                                                       else curioDialogActionColor()
                                    )
                                )
                                Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExpiryDialog = false }, colors = curioDialogActionButtonColors()) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/** "Keep forever" / "7 days" label for the recycle-bin expiry window. */
/**
 * v27q — content ink that reads on the SOLID [curioDialogActionColor]
 * selected-row fill: white on the rose/primary rows.
 */
@Composable
private fun recycleRowSelectedInk(): Color = Color.White

private fun expiryLabel(days: Int): String =
    if (days <= 0) "Keep forever" else "$days days"

@Composable
private fun TrashedEntryRow(
    entry: CurioEntry,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val category = CurioCategories.byId(entry.topic.categoryId)
    val accent = category.themedAccent()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = category.categorySurface(),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        name = category.iconGlyph,
                        contentDescription = null,
                        tint = accent,
                        size = 22.dp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.topic.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${category.displayName} · deleted ${deletedDaysAgoLabel(entry.deletedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onRestore) {
                    Text("Restore", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDeleteForever) {
                    Text(
                        "Delete forever",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** \"today\" / \"yesterday\" / \"3d ago\" for the soft-delete timestamp. */
private fun deletedDaysAgoLabel(deletedAt: Long?): String {
    if (deletedAt == null) return "recently"
    val days = ((System.currentTimeMillis() - deletedAt) / 86_400_000L).toInt()
    return when (days) {
        0 -> "today"
        1 -> "yesterday"
        else -> "$days days ago"
    }
}

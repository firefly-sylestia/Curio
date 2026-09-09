package com.curio.app.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Text history — a GLOBAL, persistent history of the text you type or edit
 * in Curio's editors (share-card title / fact / captions first; more screens
 * hook in over time). One shared feed per device:
 *
 *  - Snapshots are captured when you PAUSE while typing, at every 10th word,
 *    and right before the editor leaves the screen (paste / delete / undo all
 *    land in one of those paths).
 *  - The feed lives OUTSIDE the card's own edit state, so "Reset layout",
 *    switching fields or topics never erases it — entries survive restarts.
 *  - Entries carry the field label + timestamp and can be PINNED, COPIED,
 *    RESTORED back into the active field, or DELETED. Blank text and exact
 *    repeats of the field's previous snapshot are skipped.
 */
data class TextHistoryEntry(
    val id: Long,
    val field: String,
    val text: String,
    val ts: Long,
    val pinned: Boolean = false
)

object TextHistoryStore {
    private const val PREFS = "curio_text_history"
    private const val KEY = "history"
    private const val MAX = 300

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun decode(json: String): List<TextHistoryEntry> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val text = o.optString("x", "")
                    if (text.isBlank()) continue
                    add(
                        TextHistoryEntry(
                            id = o.optLong("i", 0L),
                            field = o.optString("f", "Text"),
                            text = text,
                            ts = o.optLong("t", 0L),
                            pinned = o.optBoolean("p", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(entries: List<TextHistoryEntry>): String {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject()
                    .put("i", e.id).put("f", e.field)
                    .put("x", e.text).put("t", e.ts).put("p", e.pinned)
            )
        }
        return arr.toString()
    }

    private fun save(ctx: Context, entries: List<TextHistoryEntry>) {
        prefs(ctx).edit().putString(KEY, encode(entries)).apply()
    }

    /** Pinned entries first, then newest first, capped at [MAX]. */
    fun snapshot(ctx: Context): List<TextHistoryEntry> {
        val all = decode(prefs(ctx).getString(KEY, "") ?: "")
        val sorted = all.sortedWith(compareByDescending<TextHistoryEntry> { it.pinned }.thenByDescending { it.ts })
        return if (sorted.size > MAX) sorted.take(MAX) else sorted
    }

    fun record(ctx: Context, field: String, text: String) {
        val t = text.trim()
        if (t.isEmpty() || t.length > 20000) return
        val cur = snapshot(ctx)
        // Exact repeat of the field's LATEST snapshot — nothing changed, skip.
        val lastSameField = cur.firstOrNull { it.field == field }
        if (lastSameField != null && lastSameField.text.trim() == t) return
        // v3xx — dedupe against ANY older entry: when the same text comes
        // back (restore, retype, paste), don't stack a duplicate — MOVE the
        // existing entry to the top with a fresh timestamp so it reads as
        // the newest version instead of a repeat. Pin status rides along.
        val existing = cur.firstOrNull { it.field == field && it.text.trim() == t }
        if (existing != null) {
            val moved = existing.copy(ts = System.currentTimeMillis())
            save(ctx, listOf(moved) + cur.filterNot { it.id == existing.id })
            return
        }
        val fresh = TextHistoryEntry(id = System.currentTimeMillis(), field = field, text = t, ts = System.currentTimeMillis())
        save(ctx, listOf(fresh) + cur)
    }

    fun setPinned(ctx: Context, id: Long, pinned: Boolean) {
        save(ctx, snapshot(ctx).map { if (it.id == id) it.copy(pinned = pinned) else it })
    }

    fun delete(ctx: Context, id: Long) {
        save(ctx, snapshot(ctx).filterNot { it.id == id })
    }

    fun clearAll(ctx: Context) {
        prefs(ctx).edit().remove(KEY).apply()
    }
}

/** Word count of a snapshot (used for the every-10th-word capture). */
private fun wordCount(text: String): Int =
    text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

private fun bucketOf(text: String): Int = wordCount(text) / 10

/** "12:04" plus "d MMM" (plus year when the entry is from a past year). */
@Composable
private fun historyStamp(ts: Long): String {
    val time = remember(ts) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts)) }
    val day = remember(ts) { SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts)) }
    val year = remember(ts) { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(ts)) }
    val thisYear = remember(ts) {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(System.currentTimeMillis()))
    }
    return if (year == thisYear) "$day · $time" else "$day $year · $time"
}

@Composable
fun formatHistoryTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        else -> historyStamp(ts)
    }
}

/**
 * Capture helper — call once per tracked text state inside an editor. Snap a
 * history entry when the text stops changing for ~1.3s (pause), the moment a
 * new 10-word bucket is reached, and again when the editor leaves the
 * composition. [resetKey] changes whenever the edited subject or field
 * changes so the memory never carries across different cards/fields.
 */
@Composable
fun rememberTextHistoryCapture(
    ctx: Context,
    field: String,
    text: String,
    resetKey: Any?
) {
    val safeField = field.ifBlank { "Text" }
    var lastSnap by remember(resetKey, safeField) { mutableStateOf(text) }
    var lastBucket by remember(resetKey, safeField) { mutableStateOf(bucketOf(text)) }
    val latestText by rememberUpdatedState(text)
    val latestField by rememberUpdatedState(safeField)
    val latestCtx by rememberUpdatedState(ctx)

    LaunchedEffect(text, resetKey, safeField) {
        val t = text
        if (t.trim().isEmpty() || t.trim() == lastSnap.trim()) return@LaunchedEffect
        val bucket = bucketOf(t)
        if (bucket > lastBucket) {
            // Crossed a 10-word boundary mid-type → snapshot right away.
            TextHistoryStore.record(ctx, safeField, t)
            lastSnap = t
            lastBucket = bucket
        } else {
            // Pause capture — cancelled by the next keystroke.
            delay(1300)
            if (t.trim() != lastSnap.trim()) {
                TextHistoryStore.record(ctx, safeField, t)
                lastSnap = t
                lastBucket = bucket
            }
        }
    }

    // Final snapshot when the editor leaves composition (Save / Share /
    // dismiss) so the very last edit is never lost.
    DisposableEffect(Unit) {
        onDispose {
            TextHistoryStore.record(latestCtx, latestField, latestText)
        }
    }
}

/** Small circular history pill for the corner of an editor surface. */
@Composable
fun TextHistoryPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = CurioIcons.History,
                contentDescription = "Text history",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 20.dp
            )
        }
    }
}

/**
 * History browser — one shared bottom sheet for every host (the share
 * bottom sheet, the full-screen editor dialog and the Enlarge writing
 * sheet). v3xx — a proper ModalBottomSheet with a drag handle instead of
 * the old centered dialog; a List / Tree toggle switches between the plain
 * feed and a paragraph-branch tree. Lists every captured snapshot with a
 * preview, field label, date/time and Pin / Copy / Restore / Delete;
 * tapping a row opens the full text.
 *
 * [activeField] is the field currently being edited — Restore writes back
 * into it via [onRestore] (entries of that field are highlighted).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextHistoryBrowser(
    ctx: Context,
    activeField: String,
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var entries by remember { mutableStateOf(TextHistoryStore.snapshot(ctx)) }
    var previewId by remember { mutableStateOf<Long?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var armedClear by remember { mutableStateOf(false) }
    var treeMode by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1600)
            toast = null
        }
    }

    fun reload() {
        entries = TextHistoryStore.snapshot(ctx)
        armedClear = false
    }

    val fullEntry = entries.firstOrNull { it.id == previewId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
            // ── Header ──────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Text history",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (entries.isEmpty()) "No snapshots yet — edits you type or paste are kept here"
                        else "${entries.size} snapshot${if (entries.size == 1) "" else "s"} · restoring into “$activeField”",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entries.size > 1) {
                    // List / Tree segmented toggle.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(3.dp)
                    ) {
                        HistoryModeChip("List", !treeMode, { treeMode = false })
                        HistoryModeChip("Tree", treeMode, { treeMode = true })
                    }
                    Spacer(Modifier.size(6.dp))
                }
                if (entries.isNotEmpty()) {
                    Text(
                        if (armedClear) "Tap again" else "Clear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (armedClear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                if (armedClear) {
                                    TextHistoryStore.clearAll(ctx)
                                    reload()
                                } else {
                                    armedClear = true
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.size(2.dp))
                CurioIcon(
                    name = CurioIcons.Close,
                    contentDescription = "Close history",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 20.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(6.dp)
                )
            }
            if (toast != null) {
                Text(
                    toast!!,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            // ── Entry list ─────────────────────────
            if (entries.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Snapshots stay on this device — “Reset layout” or a new card never clears them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (treeMode) {
                val branches = buildHistoryBranches(entries)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(branches, key = { it.key }) { branch ->
                        HistoryBranchCard(
                            branch = branch,
                            activeField = activeField,
                            onPreview = { previewId = it },
                            onPin = { e -> TextHistoryStore.setPinned(ctx, e.id, !e.pinned); reload() },
                            onCopy = { e -> clipboard.setText(AnnotatedString(e.text)); toast = "Copied" },
                            onRestore = { e -> onRestore(e.text); toast = "Restored into $activeField" },
                            onDelete = { e -> TextHistoryStore.delete(ctx, e.id); reload() }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries, key = { it.id }) { e ->
                        val isActive = e.field == activeField
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { previewId = e.id }.padding(vertical = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        e.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${e.field} · ${formatHistoryTime(e.ts)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HistoryRowAction(e.pinned, true, CurioIcons.PushPin, "Unpin", "Pin to top") {
                                    TextHistoryStore.setPinned(ctx, e.id, !e.pinned); reload()
                                }
                                HistoryRowAction(false, true, CurioIcons.ContentCopy, "Copy text", "Copy text") {
                                    clipboard.setText(AnnotatedString(e.text))
                                    toast = "Copied"
                                }
                                HistoryRowAction(isActive, isActive, CurioIcons.Restore, "Restore into $activeField", "Restore into $activeField (from ${e.field})") {
                                    onRestore(e.text)
                                    toast = "Restored into $activeField"
                                }
                                HistoryRowAction(false, true, CurioIcons.Delete, "Delete snapshot", "Delete snapshot") {
                                    TextHistoryStore.delete(ctx, e.id)
                                    reload()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full-text preview (selectable copy for long snapshots).
    if (fullEntry != null) {
        Dialog(
            onDismissRequest = { previewId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp).fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                fullEntry.field,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                formatHistoryTime(fullEntry.ts),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CurioIcon(
                            name = CurioIcons.Close,
                            contentDescription = "Close preview",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp,
                            modifier = Modifier.clip(CircleShape).clickable { previewId = null }.padding(6.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 380.dp)
                    ) {
                        Text(
                            fullEntry.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(fullEntry.text))
                            toast = "Copied"
                            previewId = null
                        }) {
                            Text("Copy", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.size(8.dp))
                        TextButton(onClick = { previewId = null }) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** One tiny round action button in a history row (icon-only, tinted when
 *  [enabled]; [active] highlights it in the primary colour). v3xx — pin is
 *  always ENABLED so an unpinned entry can actually be pinned (the old
 *  `enabled = e.pinned` made the button dead until it was already pinned). */
@Composable
private fun HistoryRowAction(
    active: Boolean,
    enabled: Boolean,
    glyph: String,
    onDesc: String,
    offDesc: String,
    onClick: () -> Unit
) {
    CurioIcon(
        name = glyph,
        contentDescription = if (active) onDesc else offDesc,
        tint = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        size = 17.dp,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(7.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// v3xx — TREE VIEW (paragraph branches)
// ═══════════════════════════════════════════════════════════════════════
// Snapshots that SHARE paragraphs are grouped into BRANCHES: the shared
// opening paragraphs form the trunk (rendered once), and each version in
// the branch shows only the CHANGED paragraphs — removed lines struck
// through, added lines highlighted — like small branches off a tree.

private data class HistoryVersion(
    val entry: TextHistoryEntry,
    val removed: List<String>,
    val added: List<String>
)

private data class HistoryBranch(
    val key: Long,
    val trunk: List<String>,
    val versions: List<HistoryVersion>
)

private fun splitParagraphs(text: String): List<String> =
    text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

private fun commonPrefixLen(a: List<String>, b: List<String>): Int {
    var i = 0
    while (i < a.size && i < b.size && a[i] == b[i]) i++
    return i
}

/** Groups [entries] (newest-first display order) into chronological
 *  (oldest → newest) branches keyed on shared opening paragraphs. */
private fun buildHistoryBranches(entries: List<TextHistoryEntry>): List<HistoryBranch> {
    val chrono = entries.sortedBy { it.ts }
    if (chrono.isEmpty()) return emptyList()
    val out = mutableListOf<HistoryBranch>()
    var prev: TextHistoryEntry? = null
    var trunk: List<String> = emptyList()
    var versions = mutableListOf<HistoryVersion>()
    for (e in chrono) {
        val paras = splitParagraphs(e.text)
        val prevParas = prev?.let { splitParagraphs(it.text) }
        val shared = if (prevParas != null) commonPrefixLen(prevParas, paras) else 0
        if (prevParas != null && shared >= 1) {
            // Same branch — the version node shows ONLY the changes vs the
            // previous snapshot (removed lines struck, added lines fresh).
            if (trunk.isEmpty()) trunk = paras.take(shared)
            versions.add(HistoryVersion(e, prevParas.drop(shared), paras.drop(shared)))
        } else {
            if (versions.isNotEmpty()) {
                out.add(HistoryBranch(versions.first().entry.id, trunk, versions))
            }
            // New branch: the first paragraph is the trunk, the rest (or the
            // whole text when it is a single paragraph) is the first node.
            trunk = paras.take(1)
            versions = mutableListOf(
                HistoryVersion(e, emptyList(), paras.drop(1).ifEmpty { paras })
            )
        }
        prev = e
    }
    if (versions.isNotEmpty()) out.add(HistoryBranch(versions.first().entry.id, trunk, versions))
    return out
}

/** One pill of the List / Tree segmented toggle in the browser header. */
@Composable
private fun HistoryModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** One branch card in Tree mode: the shared trunk once, then each version
 *  as a small tree node with a leading connector and its changed lines. */
@Composable
private fun HistoryBranchCard(
    branch: HistoryBranch,
    activeField: String,
    onPreview: (Long) -> Unit,
    onPin: (TextHistoryEntry) -> Unit,
    onCopy: (TextHistoryEntry) -> Unit,
    onRestore: (TextHistoryEntry) -> Unit,
    onDelete: (TextHistoryEntry) -> Unit
) {
    val first = branch.versions.first().entry
    val last = branch.versions.last().entry
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // Branch header — versions + field + time range.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${branch.versions.size} version${if (branch.versions.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "${first.field} · ${formatHistoryTime(last.ts)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // The trunk — the shared opening paragraphs, once.
            if (branch.versions.size > 1) {
                Spacer(Modifier.height(4.dp))
                branch.trunk.forEach { para ->
                    Text(
                        para,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Each version node — the changes, tree-style.
            branch.versions.forEachIndexed { index, v ->
                val isActive = v.entry.field == activeField
                Row(
                    Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // The connector + node dot.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(16.dp)
                    ) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(if (index == branch.versions.lastIndex) 10.dp else 22.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        )
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 6.dp)) {
                        // Changed lines: removed struck-through, added fresh.
                        v.removed.forEach { para ->
                            Text(
                                para,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        v.added.forEach { para ->
                            Text(
                                para,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                formatHistoryTime(v.entry.ts),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onPreview(v.entry.id) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            HistoryRowAction(v.entry.pinned, true, CurioIcons.PushPin, "Unpin", "Pin to top") { onPin(v.entry) }
                            HistoryRowAction(false, true, CurioIcons.ContentCopy, "Copy text", "Copy text") { onCopy(v.entry) }
                            HistoryRowAction(isActive, isActive, CurioIcons.Restore, "Restore into $activeField", "Restore into $activeField (from ${v.entry.field})") { onRestore(v.entry) }
                            HistoryRowAction(false, true, CurioIcons.Delete, "Delete snapshot", "Delete snapshot") { onDelete(v.entry) }
                        }
                    }
                }
            }
        }
    }
}

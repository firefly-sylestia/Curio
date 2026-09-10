package com.curio.app.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.PlayfairDisplayFontFamily
import com.curio.app.ui.theme.isCurioDarkTheme
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

/** How a restored snapshot should land in a field that ALREADY has text:
 *  replace it, or add the snapshot above / below the current content. */
enum class TextHistoryRestoreMode { REPLACE, ADD_TOP, ADD_BOTTOM }

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
fun TextHistoryPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.size(size)
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
 * bottom sheet, the full-screen editor dialog, the Enlarge writing sheet
 * and the capture journal editors). v3xx — a proper ModalBottomSheet with
 * a drag handle instead of the old centered dialog; a List / Tree toggle
 * switches between the plain feed and a field+session tree. Lists every
 * captured snapshot with a preview, field label, date/time and Pin /
 * Copy / Restore / Delete; tapping a row opens the full text.
 *
 * [activeField] is the field currently being edited — Restore writes back
 * into it via [onRestore] (entries of that field are highlighted).
 * [currentText] is the field's LIVE text: when it already has content, a
 * restore asks Replace / Add above / Add below instead of silently
 * clobbering the draft; an empty field restores straight away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextHistoryBrowser(
    ctx: Context,
    activeField: String,
    currentText: String = "",
    onRestore: (text: String, mode: TextHistoryRestoreMode) -> Unit,
    onDismiss: () -> Unit
) {
    var entries by remember { mutableStateOf(TextHistoryStore.snapshot(ctx)) }
    var previewId by remember { mutableStateOf<Long?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var armedClear by remember { mutableStateOf(false) }
    // v3xx — TREE is the default view: the grouped, diff-first browser is
    // what the user wants to land in (List stays one tap away).
    var treeMode by remember { mutableStateOf(true) }
    // JSX concept features: search, filters, compare, collapsible groups.
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FILTER_ALL) }
    var compareA by remember { mutableStateOf<TextHistoryEntry?>(null) }
    var comparePair by remember { mutableStateOf<Pair<TextHistoryEntry, TextHistoryEntry>?>(null) }
    var collapsedFields by remember { mutableStateOf(setOf<String>()) }
    // A restore into a non-empty field first asks HOW the snapshot should
    // come back (replace / add above / add below) via the settings-style
    // chooser below.
    var pendingRestore by remember { mutableStateOf<TextHistoryEntry?>(null) }
    // Deleting a snapshot always asks first (v3xx — no silent deletes).
    var pendingDelete by remember { mutableStateOf<TextHistoryEntry?>(null) }
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Frosted surfaces (the Settings sub-page language) for rows + cards.
    val dark = isCurioDarkTheme()

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

    // One restore path for list rows and tree nodes: empty field restores
    // immediately; a field with text opens the mode chooser.
    fun restoreEntry(e: TextHistoryEntry) {
        if (currentText.isBlank()) {
            onRestore(e.text, TextHistoryRestoreMode.REPLACE)
            toast = "Restored into $activeField"
        } else {
            pendingRestore = e
        }
    }

    // JSX compare flow: first tap arms the banner, second tap opens the
    // word-level diff dialog (tapping the SAME snapshot cancels).
    fun comparePick(e: TextHistoryEntry) {
        val a = compareA
        if (a == null) {
            compareA = e
            toast = "Pick another snapshot to compare"
        } else if (a.id == e.id) {
            compareA = null
        } else {
            comparePair = a to e
            compareA = null
        }
    }

    val pending = pendingRestore
    if (pending != null) {
        RestoreModeDialog(
            field = activeField,
            onPick = { mode ->
                onRestore(pending.text, mode)
                toast = "Restored into $activeField"
                pendingRestore = null
            },
            onDismiss = { pendingRestore = null }
        )
    }

    // Delete confirmation — a snapshot never vanishes silently.
    val deleteTarget = pendingDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this snapshot?") },
            text = {
                Text(
                    "\u201C${previewLine(deleteTarget.text, 64)}\u201D\n\n" +
                        "This removes it from Text history for good — the field itself is untouched."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        TextHistoryStore.delete(ctx, deleteTarget.id)
                        pendingDelete = null
                        reload()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    val fullEntry = entries.firstOrNull { it.id == previewId }

    // ── Search + filter (the JSX filter-row + search box) — applied to
    //    BOTH views; the tree is rebuilt from the filtered set. ──
    val isFiltering = query.isNotBlank() || filter != FILTER_ALL
    // First (chronological) snapshot id per field — the Initial / Edits split.
    val initialIds = remember(entries) {
        entries.groupBy { it.field }.mapValues { (_, es) -> es.minByOrNull { it.ts }?.id }
    }
    // Latest snapshot id per field — the CURRENT pill (the JSX current-pill).
    val latestIds = remember(entries) {
        entries.groupBy { it.field }.mapValues { (_, es) -> es.maxByOrNull { it.ts }?.id }
    }
    val filteredEntries = remember(entries, query, filter) {
        val q = query.trim().lowercase()
        entries.filter { e ->
            val matchesQuery =
                q.isEmpty() || e.text.lowercase().contains(q) || e.field.lowercase().contains(q)
            val matchesFilter = when (filter) {
                FILTER_PINNED -> e.pinned
                FILTER_INITIAL -> initialIds[e.field] == e.id
                FILTER_EDITS -> initialIds[e.field] != e.id
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

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
                        when {
                            entries.isEmpty() -> "No snapshots yet — edits you type or paste are kept here"
                            isFiltering -> "${filteredEntries.size} of ${entries.size} snapshot${if (entries.size == 1) "" else "s"} · restoring into “$activeField”"
                            else -> "${entries.size} snapshot${if (entries.size == 1) "" else "s"} · restoring into “$activeField”"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entries.size > 1) {
                    // List / Tree segmented toggle — the frosted settings
                    // pill language (warm-rose active chip).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.68f))
                            .padding(3.dp)
                    ) {
                        HistoryModeChip("List", !treeMode, { treeMode = false })
                        HistoryModeChip("Tree", treeMode, { treeMode = true })
                    }
                    Spacer(Modifier.size(6.dp))
                }
                if (entries.isNotEmpty()) {
                    // Clear — frosted pill; error-tinted once armed.
                    Text(
                        if (armedClear) "Tap again" else "Clear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (armedClear) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (armedClear) MaterialTheme.colorScheme.error
                                else if (dark) Color.White.copy(alpha = 0.09f)
                                else Color.White.copy(alpha = 0.68f)
                            )
                            .clickable {
                                if (armedClear) {
                                    TextHistoryStore.clearAll(ctx)
                                    reload()
                                } else {
                                    armedClear = true
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
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
            // ── Search + filters (the JSX topbar search + filter-row) ──
            if (entries.isNotEmpty()) {
                HistorySearchBox(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HISTORY_FILTERS.forEach { f ->
                        HistoryModeChip(f, filter == f, { filter = f })
                    }
                }
            }
            // Compare banner — one snapshot armed, waiting for the second.
            val armed = compareA
            if (armed != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            if (dark) Color(0xFF815947).copy(alpha = 0.30f) else Color(0xFFF5DFE3)
                        )
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    CurioIcon(
                        name = CurioIcons.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 16.dp
                    )
                    Text(
                        "Comparing ${formatHistoryTime(armed.ts)} — tap another snapshot's compare",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { compareA = null }
                    )
                }
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
            } else if (filteredEntries.isEmpty()) {
                // Search / filter found nothing — a clear way back out.
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 34.dp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No snapshots match",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (query.isNotBlank()) "Try a different search or filter."
                        else "Try another filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Clear filters",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (dark) Color(0xFFFFF9F1) else Color(0xFF52383C),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (dark) Color(0xFF815947) else Color(0xFFF2E8DC))
                            .clickable { query = ""; filter = FILTER_ALL }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            } else if (treeMode) {
                val fields = buildHistoryTree(filteredEntries)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                items(fields, key = { it.field }) { fieldTree ->
                    val isCollapsed = fieldTree.field in collapsedFields
                    HistoryFieldCard(
                        tree = fieldTree,
                        collapsed = isCollapsed,
                        onToggle = {
                            collapsedFields = if (isCollapsed) collapsedFields - fieldTree.field
                            else collapsedFields + fieldTree.field
                        },
                        activeField = activeField,
                        onPin = { e -> TextHistoryStore.setPinned(ctx, e.id, !e.pinned); reload() },
                        onCopy = { e -> clipboard.setText(AnnotatedString(e.text)); toast = "Copied" },
                        onRestore = { restoreEntry(it) },
                        onDelete = { e -> pendingDelete = e },
                        onCompare = { comparePick(it) },
                        isCurrent = { e -> latestIds[e.field] == e.id }
                    )
                }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { e ->
                        val isActive = e.field == activeField
                        val prevSameField = entries
                            .filter { it.field == e.field && it.ts < e.ts }
                            .maxByOrNull { it.ts }
                        val delta = wordDeltaBadge(prevSameField, e)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            // v3xx — the active row used the theme's
                            // ButterYellow secondaryContainer, which read as a
                            // bad yellow highlight; the settings family's warm
                            // rose tint marks the active field instead.
                            color = if (isActive)
                                if (dark) Color(0xFF815947).copy(alpha = 0.30f)
                                else Color(0xFF815947).copy(alpha = 0.13f)
                            else if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.68f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ── Frosted icon tile — the field reads first. ──
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (dark) Color.White.copy(alpha = 0.09f) else Color(0xFFF2E8DC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CurioIcon(
                                        name = CurioIcons.Edit,
                                        contentDescription = null,
                                        tint = if (dark) Color(0xFFD7B8A9) else Color(0xFF755647),
                                        size = 18.dp
                                    )
                                }
                                Spacer(Modifier.size(11.dp))
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { previewId = e.id }.padding(vertical = 1.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Field (bold, top) + time — the snapshot
                                    // meta reads before the text.
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            e.field,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            formatHistoryTime(e.ts),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (delta != null) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                delta,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        if (latestIds[e.field] == e.id) {
                                            Spacer(Modifier.width(6.dp))
                                            HistoryCurrentPill()
                                        }
                                    }
                                    Text(
                                        e.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                HistoryActionsRow(
                                    pinned = e.pinned,
                                    active = isActive,
                                    activeField = activeField,
                                    onPin = { TextHistoryStore.setPinned(ctx, e.id, !e.pinned); reload() },
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(e.text))
                                        toast = "Copied"
                                    },
                                    onRestore = { restoreEntry(e) },
                                    onCompare = { comparePick(e) },
                                    onDelete = { pendingDelete = e }
                                )
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
                    // Stats + what changed vs the previous snapshot (the JSX
                    // detail-panel stats / Changes note).
                    val words = wordCount(fullEntry.text)
                    val chars = fullEntry.text.length
                    val prevOf = entries
                        .filter { it.field == fullEntry.field && it.ts < fullEntry.ts }
                        .maxByOrNull { it.ts }
                    val changedPair = if (prevOf != null) {
                        val d = diffTokens(prevOf.text, fullEntry.text)
                        d.first.count { it.changed } to d.second.count { it.changed }
                    } else 0 to 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$words words · $chars characters",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        if (prevOf != null) {
                            Text(
                                if (changedPair.second > 0 || changedPair.first > 0)
                                    "+${changedPair.second} · −${changedPair.first} words"
                                else "No word-level change",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (prevOf == null) "Original version — the first snapshot of this field."
                        else "Changed from the version saved ${formatHistoryTime(prevOf.ts)}.",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(fullEntry.text))
                            toast = "Copied"
                            previewId = null
                        }) {
                            Text("Copy", fontWeight = FontWeight.Bold)
                        }
                        if (entries.size > 1) {
                            Spacer(Modifier.size(8.dp))
                            TextButton(onClick = {
                                val e = fullEntry
                                previewId = null
                                comparePick(e)
                            }) {
                                Text("Compare", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        TextButton(onClick = {
                            val e = fullEntry
                            previewId = null
                            restoreEntry(e)
                        }) {
                            Text("Restore", fontWeight = FontWeight.Bold)
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

    // Word-level compare dialog — the JSX compare panel with removed/added
    // highlights.
    val pair = comparePair
    if (pair != null) {
        CompareVersionsDialog(
            a = pair.first,
            b = pair.second,
            onDismiss = { comparePair = null }
        )
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
// v3xx — TREE VIEW (field groups + edit sessions)
// ═══════════════════════════════════════════════════════════════════════
// The plain feed mixes every field into one list, which reads as noise.
// Tree mode groups snapshots by FIELD, then by EDIT SESSION (a burst of
// changes within ~20 minutes): each field becomes one card — Playfair
// heading + snapshot count — with its sessions as soft sub-groups and each
// version a node showing the time, a +/− line-change badge and the text
// itself. Same visual language as the Settings pages: frosted cards, warm
// icon tiles, hairline dividers.

private const val SESSION_GAP_MS = 20 * 60 * 1000L

// ── Filters (the JSX filter-row, adapted to the linear snapshot model) ──
private const val FILTER_ALL = "All"
private const val FILTER_EDITS = "Edits"
private const val FILTER_INITIAL = "Initial"
private const val FILTER_PINNED = "Pinned"
private val HISTORY_FILTERS = listOf(FILTER_ALL, FILTER_EDITS, FILTER_INITIAL, FILTER_PINNED)

// ── Word-level diff (the JSX compare panel) ────────────────────────────
/** One word of a compared snapshot; [changed] marks words that differ
 *  between the two versions (removed on the older side, added on the
 *  newer). */
private data class DiffToken(val word: String, val changed: Boolean)

/** Splits [a] and [b] into tokens and runs a classic LCS (longest common
 *  subsequence) over the words, so "changed" is the TRUE word-level edit:
 *  unchanged words are shared, everything else is marked on one side only. */
private fun diffTokens(a: String, b: String): Pair<List<DiffToken>, List<DiffToken>> {
    val ta = a.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val tb = b.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val n = ta.size
    val m = tb.size
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            dp[i][j] = if (ta[i] == tb[j]) dp[i + 1][j + 1] + 1
            else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
    }
    val aOut = mutableListOf<DiffToken>()
    val bOut = mutableListOf<DiffToken>()
    var i = 0
    var j = 0
    while (i < n && j < m) {
        if (ta[i] == tb[j]) {
            aOut.add(DiffToken(ta[i], false))
            bOut.add(DiffToken(tb[j], false))
            i++
            j++
        } else if (dp[i + 1][j] >= dp[i][j + 1]) {
            aOut.add(DiffToken(ta[i], true))
            i++
        } else {
            bOut.add(DiffToken(tb[j], true))
            j++
        }
    }
    while (i < n) { aOut.add(DiffToken(ta[i], true)); i++ }
    while (j < m) { bOut.add(DiffToken(tb[j], true)); j++ }
    return aOut to bOut
}

/** Word-delta badge of [curr] vs its [prev] snapshot — "+3 words",
 *  "−2 words" or "Original" for the first version (the JSX delta). */
private fun wordDeltaBadge(prev: TextHistoryEntry?, curr: TextHistoryEntry): String? {
    if (prev == null) return "Original"
    val a = prev.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val b = curr.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val d = b - a
    return when {
        d > 0 -> "+$d words"
        d < 0 -> "−${-d} words"
        else -> null
    }
}

/** The highlighted snapshot text — changed words wear a soft container
 *  background (error-tint on the removed side, warm-tint on the added),
 *  removed words also get a strike-through. Built from the most basic
 *  AnnotatedString API (constructor + Range — the Builder's withStyle
 *  helpers don't resolve in this compose version), so the whole version
 *  renders as a single flowing paragraph. */
private fun buildDiffString(tokens: List<DiffToken>, removed: Boolean, scheme: ColorScheme): AnnotatedString {
    val bg = if (removed) scheme.errorContainer else scheme.tertiaryContainer
    val fg = if (removed) scheme.onErrorContainer else scheme.onTertiaryContainer
    val style = SpanStyle(
        background = bg,
        color = fg,
        textDecoration = if (removed) TextDecoration.LineThrough else null
    )
    val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    val text = StringBuilder()
    tokens.forEach { tok ->
        if (text.isNotEmpty()) text.append(' ')
        val start = text.length
        text.append(tok.word)
        if (tok.changed) {
            spans.add(AnnotatedString.Range(style, start, text.length))
        }
    }
    return AnnotatedString(text.toString(), spanStyles = spans)
}

/** Tree geometry (relative to the frosted card's content): the trunk sits
 *  at [TreeTrunkInset] from the card's left edge, and every version node
 *  hangs [TreeNodeDotX] to its right on a short branch stub — the classic
 *  trunk → branch → leaf read instead of floating dots. */
private val TreeTrunkInset = 3.dp
private val TreeNodeDotX = 15.dp
private val TreeNodeDotY = 10.dp

/** One burst of edits on the same field (snapshots within ~20 minutes). */
private data class HistorySession(
    val startTs: Long,
    val versions: List<TextHistoryEntry>
)

/** One field's history: chronological sessions, busiest field first. */
private data class FieldTree(
    val field: String,
    val sessions: List<HistorySession>
)

private fun splitParagraphs(text: String): List<String> =
    text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

/** Groups [entries] (newest-first display order) by field, then splits
 *  each field's chronological snapshots into edit sessions (a gap of more
 *  than [SESSION_GAP_MS] starts a new session) — so similar texts and
 *  their modifications actually sit together. */
private fun buildHistoryTree(entries: List<TextHistoryEntry>): List<FieldTree> =
    entries.groupBy { it.field }
        .map { (field, es) ->
            val chrono = es.sortedBy { it.ts }
            val sessions = mutableListOf<HistorySession>()
            var current = mutableListOf<TextHistoryEntry>()
            var prevTs = Long.MIN_VALUE
            for (e in chrono) {
                if (current.isNotEmpty() && e.ts - prevTs > SESSION_GAP_MS) {
                    sessions.add(HistorySession(current.first().ts, current.toList()))
                    current = mutableListOf()
                }
                current.add(e)
                prevTs = e.ts
            }
            if (current.isNotEmpty()) sessions.add(HistorySession(current.first().ts, current.toList()))
            FieldTree(field, sessions)
        }
        .sortedWith(
            compareByDescending<FieldTree> { it.sessions.sumOf { s -> s.versions.size } }
                .thenBy { it.field.lowercase() }
        )

/** Added / removed paragraph counts of [curr] vs [prev] (the +/− badge). */
private fun diffSummary(prev: TextHistoryEntry?, curr: TextHistoryEntry): Pair<Int, Int> {
    val prevParas = prev?.let { splitParagraphs(it.text) }?.toSet().orEmpty()
    val currParas = splitParagraphs(curr.text)
    val added = currParas.count { it !in prevParas }
    val removed = prevParas.count { it !in currParas }
    return added to removed
}

/** One pill of the List / Tree segmented toggle in the browser header — the
 *  frosted settings chip: warm-rose fill when selected, transparent glass
 *  otherwise. */
@Composable
private fun HistoryModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = if (selected) Color(0xFFFFF9F1) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color(0xFF815947) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** One field's tree card — settings-style heading, then each edit session
 *  as a soft sub-group of version nodes. Tapping the heading collapses /
 *  expands the whole field (the JSX collapsible groups). */
@Composable
private fun HistoryFieldCard(
    tree: FieldTree,
    collapsed: Boolean,
    onToggle: () -> Unit,
    activeField: String,
    onPin: (TextHistoryEntry) -> Unit,
    onCopy: (TextHistoryEntry) -> Unit,
    onRestore: (TextHistoryEntry) -> Unit,
    onDelete: (TextHistoryEntry) -> Unit,
    onCompare: (TextHistoryEntry) -> Unit,
    isCurrent: (TextHistoryEntry) -> Boolean
) {
    val dark = isCurioDarkTheme()
    val versionCount = tree.sessions.sumOf { it.versions.size }
    Column(Modifier.fillMaxWidth()) {
        // ── Heading: glyph + Playfair label + count + rule + chevron ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                .clickable(onClick = onToggle)
        ) {
            Text(
                text = "\u2726",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = tree.field,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$versionCount ${if (versionCount == 1) "snapshot" else "snapshots"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(11.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
            Spacer(Modifier.width(4.dp))
            CurioIcon(
                name = if (collapsed) CurioIcons.KeyboardArrowDown else CurioIcons.KeyboardArrowUp,
                contentDescription = if (collapsed) "Expand group" else "Collapse group",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        }
        // ── Frosted card with the TREE TRUNK — ONE continuous vertical
        //    stem runs down the card; every session heading and version node
        //    hangs off it on a short branch stub. (The old per-row stems
        //    broke at each session divider, which killed the tree read.)
        AnimatedVisibility(visible = !collapsed) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                    else Color.White.copy(alpha = 0.68f)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box {
                // The trunk — spans the whole content height.
                val trunkColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                Canvas(Modifier.matchParentSize()) {
                    drawLine(
                        color = trunkColor,
                        start = Offset(TreeTrunkInset.toPx(), 0f),
                        end = Offset(TreeTrunkInset.toPx(), size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Column(Modifier.padding(start = TreeTrunkInset)) {
                    tree.sessions.forEachIndexed { sIdx, session ->
                        if (sIdx > 0) {
                            // Hairline between sessions — starts right of the
                            // trunk so the trunk reads as one unbroken line.
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 14.dp, top = 6.dp)
                            )
                        }
                        // Session sub-heading — a branch stub off the trunk,
                        // then the time range + edit count.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        ) {
                            SessionBranchStub()
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (session.versions.size == 1) formatHistoryTime(session.startTs)
                                else "${formatHistoryTime(session.startTs)} → ${formatHistoryTime(session.versions.last().ts)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (session.versions.size == 1) "1 edit" else "${session.versions.size} edits",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Version nodes — each hangs off the trunk.
                        session.versions.forEachIndexed { vIdx, e ->
                            HistoryVersionRow(
                                entry = e,
                                prev = session.versions.getOrNull(vIdx - 1),
                                isActive = e.field == activeField,
                                isCurrent = isCurrent(e),
                                activeField = activeField,
                                onPin = { onPin(e) },
                                onCopy = { onCopy(e) },
                                onRestore = { onRestore(e) },
                                onCompare = { onCompare(e) },
                                onDelete = { onDelete(e) }
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

/** One version node inside a session — a short branch stub runs from the
 *  card's trunk to the node dot (the trunk itself is drawn once by
 *  [HistoryFieldCard], so the tree stays continuous across sessions), the
 *  time + a +/− change badge, a COMPACT one-line preview that EXPANDS to a
 *  CLIPPED diff (previews of the added/removed lines, never the full
 *  text), then the actions. */
@Composable
private fun HistoryVersionRow(
    entry: TextHistoryEntry,
    prev: TextHistoryEntry?,
    isActive: Boolean,
    isCurrent: Boolean,
    activeField: String,
    onPin: () -> Unit,
    onCopy: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onCompare: () -> Unit
) {
    val (added, removed) = diffSummary(prev, entry)
    val badge = buildString {
        if (added > 0) append("+$added")
        if (added > 0 && removed > 0) append(" · ")
        if (removed > 0) append("−$removed")
    }
    var expanded by remember { mutableStateOf(false) }
    // The diff — what changed vs the previous version, line by line.
    val prevParas = prev?.let { splitParagraphs(it.text) }?.toSet().orEmpty()
    val currParas = splitParagraphs(entry.text)
    val addedLines = currParas.filter { it !in prevParas }
    val removedLines = prevParas.filter { it !in currParas }

    // Colors are read in the composable scope — drawBehind is not a
    // @Composable context, so MaterialTheme must not be read inside it.
    val stemColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    val dotColor = if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
        // ── Branch + node — the stub starts exactly at the trunk (x=0 of
        //    this column IS the trunk) and ends at the node dot.
        Box(
            modifier = Modifier
                .width(18.dp)
                .fillMaxHeight()
                .drawBehind {
                    drawLine(
                        color = stemColor,
                        start = Offset(0f, TreeNodeDotY.toPx()),
                        end = Offset(TreeNodeDotX.toPx(), TreeNodeDotY.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = TreeNodeDotX - 4.dp, y = TreeNodeDotY - 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            // Time + change badge.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatHistoryTime(entry.ts),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (badge.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary
                    )
                }
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    HistoryCurrentPill()
                }
            }
            // COMPACT preview — ONE line, tapping it expands the diff (the
            // tree never floods with giant texts).
            Text(
                entry.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(top = 3.dp, bottom = 3.dp, end = 4.dp)
            )
            // ── Expanded — the proper tree view: exactly what was added /
            //    removed, compact chips, scrolls when long. ──
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 2.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (addedLines.isEmpty() && removedLines.isEmpty()) {
                        Text(
                            "Initial version",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Clipped previews — a few short snippets of what changed,
                    // never the full text (v3xx — the expanded node used to
                    // dump whole paragraphs).
                    val shownAdded = addedLines.take(4)
                    shownAdded.forEach { line ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "+",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                previewLine(line),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (addedLines.size > shownAdded.size) {
                        DiffMoreChip("${addedLines.size - shownAdded.size} more added")
                    }
                    val shownRemoved = removedLines.take(4)
                    shownRemoved.forEach { line ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "−",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                previewLine(line),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (removedLines.size > shownRemoved.size) {
                        DiffMoreChip("${removedLines.size - shownRemoved.size} more removed")
                    }
                    // Restore — empty field restores straight away; a field
                    // with text opens the Add above / Add below / Replace
                    // chooser (the paste options depend on the text box).
                    Text(
                        "Restore this version",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isCurioDarkTheme()) Color(0xFFFFF9F1) else Color(0xFF52383C),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isCurioDarkTheme()) Color(0xFF815947) else Color(0xFFF2E8DC)
                            )
                            .clickable(onClick = onRestore)
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.field,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                HistoryActionsRow(
                    pinned = entry.pinned,
                    active = isActive,
                    activeField = activeField,
                    onPin = onPin,
                    onCopy = onCopy,
                    onRestore = onRestore,
                    onCompare = onCompare,
                    onDelete = onDelete
                )
            }
        }
    }
}

/** A compact one-line preview of a changed paragraph — long lines are
 *  clipped so the expanded diff never dumps full text. */
private fun previewLine(line: String, maxChars: Int = 56): String =
    if (line.length <= maxChars) line else line.take(maxChars - 1) + "…"

/** The "N more added/removed" footer of a clipped diff — a tiny quiet chip. */
@Composable
private fun DiffMoreChip(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, top = 1.dp)
    )
}

/** A tiny horizontal branch stub — connects a session heading to the trunk
 *  (its left edge sits exactly on the trunk's x, so the line reads as one
 *  continuous branch off the tree). */
@Composable
private fun SessionBranchStub() {
    val stubColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    Box(
        Modifier
            .width(14.dp)
            .height(14.dp)
            .drawBehind {
                drawLine(
                    color = stubColor,
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
    )
}

/** The round row actions (pin / copy / restore / compare / delete) — shared
 *  by the list rows and the tree version rows. Compare arms the JSX
 *  two-tap flow: pick this snapshot, then another, and the word-level diff
 *  dialog opens. */
@Composable
private fun HistoryActionsRow(
    pinned: Boolean,
    active: Boolean,
    activeField: String,
    onPin: () -> Unit,
    onCopy: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onCompare: (() -> Unit)? = null
) {
    HistoryRowAction(pinned, true, CurioIcons.PushPin, "Unpin", "Pin to top") { onPin() }
    HistoryRowAction(false, true, CurioIcons.ContentCopy, "Copy text", "Copy text") { onCopy() }
    HistoryRowAction(active, active, CurioIcons.Restore, "Restore into $activeField", "Restore into $activeField") { onRestore() }
    if (onCompare != null) {
        HistoryRowAction(false, true, CurioIcons.Layers, "Compare with another version", "Compare with another version") { onCompare() }
    }
    HistoryRowAction(false, true, CurioIcons.Delete, "Delete snapshot", "Delete snapshot") { onDelete() }
}

/** Settings-style restore chooser — shown when the active field already
 *  has text, so a restore can replace it or add the snapshot above/below
 *  instead of silently clobbering the current draft. */
@Composable
private fun RestoreModeDialog(
    field: String,
    onPick: (TextHistoryRestoreMode) -> Unit,
    onDismiss: () -> Unit
) {
    val dark = isCurioDarkTheme()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp).fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Restore into “$field”",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "The field already has text — where should the snapshot go?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                RestoreModeRow(
                    icon = CurioIcons.ArrowUpward,
                    title = "Add above",
                    subtitle = "Snapshot first, then what you already wrote",
                    onClick = { onPick(TextHistoryRestoreMode.ADD_TOP) }
                )
                RestoreModeRow(
                    icon = CurioIcons.ArrowDownward,
                    title = "Add below",
                    subtitle = "What you already wrote, then the snapshot",
                    onClick = { onPick(TextHistoryRestoreMode.ADD_BOTTOM) }
                )
                RestoreModeRow(
                    icon = CurioIcons.Restore,
                    title = "Replace",
                    subtitle = "Swap the current text for the snapshot",
                    onClick = { onPick(TextHistoryRestoreMode.REPLACE) }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/** One frosted option row of the restore chooser — the settings row
 *  language (warm icon tile + title + subtitle + chevron). */
@Composable
private fun RestoreModeRow(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (dark) Color.White.copy(alpha = 0.09f) else Color(0xFFF2E8DC)),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(
                name = icon,
                contentDescription = null,
                tint = if (dark) Color(0xFFD7B8A9) else Color(0xFF755647),
                size = 20.dp
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        CurioIcon(
            name = CurioIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            size = 18.dp
        )
    }
}

/** The frosted search pill under the browser header — the JSX topbar search
 *  box. Matches the rest of the sheet's glass language. */
@Composable
private fun HistorySearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.68f)
            )
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
    ) {
        CurioIcon(
            name = CurioIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 17.dp
        )
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search snapshots…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            CurioIcon(
                name = CurioIcons.Close,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onQueryChange("") }
                    .padding(5.dp)
            )
        }
    }
}

/** The tiny warm "CURRENT" pill — marks the newest snapshot of each field
 *  (the JSX current-pill). */
@Composable
private fun HistoryCurrentPill() {
    val dark = isCurioDarkTheme()
    Text(
        "CURRENT",
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp
        ),
        color = if (dark) Color(0xFFFFF9F1) else Color(0xFF52383C),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (dark) Color(0xFF815947) else Color(0xFFF2E8DC))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

/** The JSX compare panel — two snapshots side by side with the TRUE
 *  word-level diff: changed words highlighted (strike-through red on the
 *  older version, warm-tint on the newer) + added/removed word counts. */
@Composable
private fun CompareVersionsDialog(
    a: TextHistoryEntry,
    b: TextHistoryEntry,
    onDismiss: () -> Unit
) {
    val (older, newer) = if (a.ts <= b.ts) a to b else b to a
    val (aTokens, bTokens) = remember(older.id, newer.id) { diffTokens(older.text, newer.text) }
    val removedCount = aTokens.count { it.changed }
    val addedCount = bTokens.count { it.changed }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp).fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Comparing versions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Word-level changes between two snapshots",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CurioIcon(
                        name = CurioIcons.Close,
                        contentDescription = "Close comparison",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(6.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompareVersionCard(
                        label = "Earlier · ${formatHistoryTime(older.ts)}",
                        tokens = aTokens,
                        removed = true
                    )
                    CompareVersionCard(
                        label = "Later · ${formatHistoryTime(newer.ts)}",
                        tokens = bTokens,
                        removed = false
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "$removedCount word${if (removedCount == 1) "" else "s"} removed",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                    Text(
                        "$addedCount word${if (addedCount == 1) "" else "s"} added",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/** One side of the compare dialog — the highlighted version text. */
@Composable
private fun CompareVersionCard(
    label: String,
    tokens: List<DiffToken>,
    removed: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                buildDiffString(tokens, removed, MaterialTheme.colorScheme),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

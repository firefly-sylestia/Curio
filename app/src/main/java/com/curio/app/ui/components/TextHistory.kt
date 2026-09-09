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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxHeight
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
    var treeMode by remember { mutableStateOf(false) }
    // A restore into a non-empty field first asks HOW the snapshot should
    // come back (replace / add above / add below) via the settings-style
    // chooser below.
    var pendingRestore by remember { mutableStateOf<TextHistoryEntry?>(null) }
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
                val fields = buildHistoryTree(entries)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fields, key = { it.field }) { fieldTree ->
                        HistoryFieldCard(
                            tree = fieldTree,
                            activeField = activeField,
                            onPreview = { previewId = it },
                            onPin = { e -> TextHistoryStore.setPinned(ctx, e.id, !e.pinned); reload() },
                            onCopy = { e -> clipboard.setText(AnnotatedString(e.text)); toast = "Copied" },
                            onRestore = { restoreEntry(it) },
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
                            shape = RoundedCornerShape(16.dp),
                            color = if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            else if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.68f),
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
                                    onDelete = { TextHistoryStore.delete(ctx, e.id); reload() }
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

/** One field's tree card — settings-style heading, then each edit session
 *  as a soft sub-group of version nodes. */
@Composable
private fun HistoryFieldCard(
    tree: FieldTree,
    activeField: String,
    onPreview: (Long) -> Unit,
    onPin: (TextHistoryEntry) -> Unit,
    onCopy: (TextHistoryEntry) -> Unit,
    onRestore: (TextHistoryEntry) -> Unit,
    onDelete: (TextHistoryEntry) -> Unit
) {
    val dark = isCurioDarkTheme()
    val versionCount = tree.sessions.sumOf { it.versions.size }
    Column(Modifier.fillMaxWidth()) {
        // ── Heading: glyph + Playfair label + count + rule (settings) ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
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
        }
        // ── Frosted card ─────────────────────────────────────────────
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
            tree.sessions.forEachIndexed { sIdx, session ->
                if (sIdx > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 18.dp, top = 6.dp)
                    )
                }
                // Session sub-heading — time range + edit count.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                ) {
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
                // Version nodes.
                session.versions.forEachIndexed { vIdx, e ->
                    HistoryVersionRow(
                        entry = e,
                        isLast = vIdx == session.versions.lastIndex,
                        prev = session.versions.getOrNull(vIdx - 1),
                        isActive = e.field == activeField,
                        onPreview = { onPreview(e.id) },
                        onPin = { onPin(e) },
                        onCopy = { onCopy(e) },
                        onRestore = { onRestore(e) },
                        onDelete = { onDelete(e) }
                    )
                }
            }
        }
    }
}

/** One version node inside a session — connector + node dot, time and a
 *  +/− line-change badge, the full text (2 lines), then the row actions. */
@Composable
private fun HistoryVersionRow(
    entry: TextHistoryEntry,
    isLast: Boolean,
    prev: TextHistoryEntry?,
    isActive: Boolean,
    onPreview: () -> Unit,
    onPin: () -> Unit,
    onCopy: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val (added, removed) = diffSummary(prev, entry)
    val badge = buildString {
        if (added > 0) append("+$added")
        if (added > 0 && removed > 0) append(" · ")
        if (removed > 0) append("−$removed")
    }
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
        // Connector — node dot at the top, then a short stem down toward
        // the next node (no dangling line on the last row).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (isLast) 6.dp else 18.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
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
            }
            // The FULL text — the whole version reads at a glance (the old
            // change-only diff was the confusing part).
            Text(
                entry.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPreview)
                    .padding(top = 2.dp, bottom = 2.dp, end = 4.dp)
            )
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
                    onDelete = onDelete
                )
            }
        }
    }
}

/** The four round row actions (pin / copy / restore / delete) — shared by
 *  the list rows and the tree version rows. */
@Composable
private fun HistoryActionsRow(
    pinned: Boolean,
    active: Boolean,
    activeField: String,
    onPin: () -> Unit,
    onCopy: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    HistoryRowAction(pinned, true, CurioIcons.PushPin, "Unpin", "Pin to top") { onPin() }
    HistoryRowAction(false, true, CurioIcons.ContentCopy, "Copy text", "Copy text") { onCopy() }
    HistoryRowAction(active, active, CurioIcons.Restore, "Restore into $activeField", "Restore into $activeField") { onRestore() }
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

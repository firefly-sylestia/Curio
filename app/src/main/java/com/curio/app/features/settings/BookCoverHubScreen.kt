package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.TopicJsonLoader
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * v320 — the BOOK COVERS & RATINGS HUB: pick a cover provider (Open Library
 * or Google Books), bulk-fetch every cover into the shared disk cache (one
 * by one, with a live counter + Cancel), RETRY only the previously-failed
 * books, and — without any API key — fetch Google Books average ratings that
 * the reveal shows as star chips. Failed book names persist across restarts.
 */
@Composable
fun BookCoverHubScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var providerName by remember { mutableStateOf(AppPreferences.bookCoverProviderState) }
    val provider = runCatching { BookCoverFetch.BookCoverProvider.valueOf(providerName) }
        .getOrDefault(BookCoverFetch.BookCoverProvider.OPEN_LIBRARY)

    // Fetch engine state.
    var job by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var jobLabel by remember { mutableStateOf("") } // "Fetching covers…" / "Fetching ratings…"
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var failed by remember { mutableIntStateOf(0) }

    // TopicJsonLoader.load is suspend — load the count off the main thread.
    val bookCount by produceState(initialValue = 0) {
        value = runCatching { TopicJsonLoader.load(CategoryId.BOOKS) }.map { it.size }.getOrDefault(0)
    }
    // Reactive reads — prefs state updates as fetches complete.
    val failedList = AppPreferences.bookCoverFailedState
    val ratedCount = AppPreferences.bookRatingsState.size

    fun start(kind: String) {
        // v320b — fetching is OPT-OUT by default: nothing downloads until
        // the user flips the toggle below.
        if (busy || !AppPreferences.bookFetchEnabledState) return
        job = scope.launch {
            busy = true
            done = 0; total = 0; failed = 0
            try {
                when (kind) {
                    "covers" -> {
                        jobLabel = "Fetching covers…"
                        BookCoverFetch.fetchAll(context, provider) { d, t, f ->
                            done = d; total = t; failed = f
                        }
                    }
                    "failed" -> {
                        jobLabel = "Retrying failed covers…"
                        BookCoverFetch.fetchAll(context, provider, onlyFailed = true) { d, t, f ->
                            done = d; total = t; failed = f
                        }
                    }
                    "ratings" -> {
                        jobLabel = "Fetching ratings…"
                        BookCoverFetch.fetchRatings(context) { d, t ->
                            done = d; total = t
                        }
                    }
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Cancelled — whatever was cached stays.
            } finally {
                busy = false
                job = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        // ── Header ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Surface(
                onClick = { navController.popBackStack() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                CurioIcon(
                    CurioIcons.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 20.dp,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    BookCoverFetch.TITLE,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Choose a source · retry failures · fetch ratings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Opt-in / opt-out master switch (v320b) ─────────────────
            item(key = "master") {
                val fetchOn = AppPreferences.bookFetchEnabledState
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (fetchOn) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        CurioIcon(
                            if (fetchOn) CurioIcons.Download else CurioIcons.MenuBook, null,
                            tint = if (fetchOn) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Book cover fetching",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (fetchOn)
                                    "ON — covers and ratings can be downloaded"
                                else
                                    "OFF by default — nothing downloads until you turn this on",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = fetchOn,
                            onCheckedChange = { AppPreferences.setBookFetchEnabled(context, it) }
                        )
                    }
                }
            }

            // ── Provider picker ────────────────────────────────────────
            item(key = "provider") {
                Text(
                    "Cover source",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BookCoverFetch.BookCoverProvider.entries.forEach { p ->
                        val selected = p == provider
                        Surface(
                            onClick = {
                                providerName = p.name
                                AppPreferences.setBookCoverProvider(context, p.name)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        CurioIcon(
                                            CurioIcons.Check, null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            size = 12.dp
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        p.label,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        p.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Stats ──────────────────────────────────────────────────
            item(key = "stats") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        StatCell("$bookCount", "books", modifier = Modifier.weight(1f))
                        StatCell("${failedList.size}", "failed covers", modifier = Modifier.weight(1f))
                        StatCell("$ratedCount", "rated", modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Actions ────────────────────────────────────────────────
            item(key = "actions") {
                val fetchOn = AppPreferences.bookFetchEnabledState
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubButton(
                        label = "Fetch all covers",
                        glyph = CurioIcons.Download,
                        emphasize = true,
                        enabled = fetchOn && !busy,
                        onClick = { start("covers") }
                    )
                    val retryEnabled = fetchOn && !busy && failedList.isNotEmpty()
                    HubButton(
                        label = if (failedList.isEmpty()) "Retry failed covers" else "Retry failed (${failedList.size})",
                        glyph = CurioIcons.Refresh,
                        emphasize = false,
                        enabled = retryEnabled,
                        onClick = { start("failed") }
                    )
                    HubButton(
                        label = "Fetch ratings (keyless)",
                        glyph = CurioIcons.Star,
                        emphasize = false,
                        enabled = fetchOn && !busy,
                        onClick = { start("ratings") }
                    )
                    if (!fetchOn) {
                        Text(
                            "Fetching is off — flip the switch above to download covers and ratings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Progress / cancel ──────────────────────────────────────
            if (busy) {
                item(key = "progress") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    jobLabel + (if (total > 0) " $done / $total" else ""),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    onClick = { job?.cancel() },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        "Cancel",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                            if (total > 0) {
                                LinearProgressIndicator(
                                    progress = { done.toFloat() / total },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (failed > 0) {
                                Text(
                                    "$failed failed so far",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // ── Failed list ────────────────────────────────────────────
            if (failedList.isNotEmpty()) {
                item(key = "failed-header") {
                    Text(
                        "Failed covers — tap to retry one",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(failedList, key = { it }) { name ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.Close, null,
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                size = 14.dp
                            )
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (!busy && AppPreferences.bookFetchEnabledState) {
                                Surface(
                                    onClick = {
                                        job = scope.launch {
                                            busy = true
                                            // v320b — per-row retry also respects the opt-out.
                                            if (!AppPreferences.bookFetchEnabledState) {
                                                busy = false
                                                job = null
                                                return@launch
                                            }
                                            try {
                                                BookCoverFetch.fetchAll(context, provider, onlyFailed = true) { d, t, f ->
                                                    done = d; total = t; failed = f
                                                }
                                            } finally {
                                                busy = false
                                                job = null
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        "Retry",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Small stat cell for the hub's stats card — the caller supplies the
 *  RowScope weight so the three cells share the card width evenly. */
@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** One action button in the hub (solid primary / neutral outline). */
@Composable
private fun HubButton(
    label: String,
    glyph: String,
    emphasize: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (emphasize) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().height(46.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            CurioIcon(
                glyph, null,
                tint = if (emphasize) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp
            )
            Spacer(Modifier.size(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (emphasize) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
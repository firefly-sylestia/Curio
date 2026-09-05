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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.width
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
import com.curio.app.data.CurioTopic
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
    // v356 — iTunes is the default source. LibraryThing only applies when its
    // free key is configured (BuildConfig.LIBRARY_THING_API_KEY); without a
    // key a stored LIBRARY_THING pick silently falls back to iTunes.
    val provider = runCatching { BookCoverFetch.BookCoverProvider.valueOf(providerName) }
        .getOrDefault(BookCoverFetch.BookCoverProvider.ITUNES)
        .let { p ->
            if (p == BookCoverFetch.BookCoverProvider.LIBRARY_THING &&
                com.curio.app.BuildConfig.LIBRARY_THING_API_KEY.isBlank()
            ) BookCoverFetch.BookCoverProvider.ITUNES else p
        }

    // Fetch engine state.
    var job by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var jobLabel by remember { mutableStateOf("") } // "Fetching covers…" / "Fetching ratings…"
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var failed by remember { mutableIntStateOf(0) }

    // TopicJsonLoader.load is suspend — load the count off the main thread.
    val books by produceState(initialValue = emptyList<CurioTopic>()) {
        value = runCatching { TopicJsonLoader.load(CategoryId.BOOKS) }.getOrDefault(emptyList())
    }
    val bookCount = books.size
    // Reactive reads — prefs state updates as fetches complete.
    val failedList = AppPreferences.bookCoverFailedState
    val ratedCount = AppPreferences.bookRatingsState.size
    val ratingCounts = AppPreferences.bookRatingsCountState

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
                                    "ON · covers and ratings can be downloaded"
                                else
                                    "OFF by default · nothing downloads until you turn this on",
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
                    // v356 — LibraryThing needs its free key (BuildConfig);
                    // without one the row is hidden rather than failing every
                    // fetch. iTunes is listed first (the default source).
                    val providers = BookCoverFetch.BookCoverProvider.entries.filter { p ->
                        p != BookCoverFetch.BookCoverProvider.LIBRARY_THING ||
                            com.curio.app.BuildConfig.LIBRARY_THING_API_KEY.isNotBlank()
                    }
                    providers.forEach { p ->
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
                            "Fetching is off. Turn the switch above on to download covers and ratings.",
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
                        "Failed covers · tap to retry one",
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

            // ── All covers strip (v328) — every book's cover thumbnail
            // (renders from the Coil disk cache once fetched; tapping opens
            // the book's own reveal) with its cached star + count when rated.
            item(key = "covers-header") {
                Text(
                    "All covers · ${books.size} books",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "covers-strip") {
                if (books.isEmpty()) {
                    Text(
                        "Loading books…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(books) { _, book ->
                            CoverTile(
                                book = book,
                                rating = AppPreferences.bookRatingsState[book.name],
                                count = ratingCounts[book.name] ?: 0,
                                onClick = {
                                    navController.navigate(
                                        com.curio.app.navigation.CurioRoutes.revealForBrowse(
                                            CategoryId.BOOKS.routeSlug,
                                            book.name
                                        )
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One book cover tile in the hub's All-covers strip — the AsyncImage IS
 *  the poster (the ce892baa contract) inside a fixed 80×112 slot, with the
 *  cached star + ratings count under it when the book has been rated. */
@Composable
private fun CoverTile(
    book: CurioTopic,
    rating: Double?,
    count: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(92.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 2.dp,
            modifier = Modifier.size(width = 80.dp, height = 112.dp)
        ) {
            val cover = BookCoverFetch.coverCandidates(book.name, book.imageUrl).firstOrNull()
            if (cover != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover of ${book.name}",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            book.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        if (rating != null && rating > 0.0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                CurioIcon(
                    CurioIcons.Star, null,
                    tint = Color(0xFFF6B23B),
                    size = 11.dp
                )
                Text(
                    String.format("%.1f", rating) + if (count > 0) " · ${count}" else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
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
package com.curio.app.features.reveal

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategory
import com.curio.app.data.openSearchUrl
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.notesSheetContainerColor
import com.curio.app.ui.theme.themedAccent
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * v389d — WHAT AN AUTHOR WROTE (user request: "also add artwork button sheet too
 * and author button sheet with authors written books").
 *
 * One author's other books, read from Open Library — the same keyless source the
 * app already trusts for a book's chapters, its description and its neighbours
 * on the shelf. Two calls, both free and both cacheable forever:
 *
 *  1. `openlibrary.org/search/authors.json?q=<name>` — the author's OWN key
 *     (an `OL…A` id). A name is not an id: two authors share one, and the works
 *     endpoint needs the id.
 *  2. `openlibrary.org/authors/<key>/works.json?limit=50` — their works, each
 *     with a title, a first publication date and (often) a cover id, which is
 *     drawn exactly as the shelf draws a cover.
 *
 * Nothing is written to the database and nothing is looked up twice: a name's
 * answer lives in a small in-process map for the session, misses included, so
 * opening the sheet again is instant and a name Open Library does not know is
 * not asked after twice.
 */
internal data class AuthorWork(
    /** The work's own Open Library id, for its page. */
    val key: String,
    val title: String,
    /** The year it was first published, when Open Library says — "" otherwise. */
    val year: String,
    /** A cover URL, or "" when the work has none. */
    val coverUrl: String
)

object AuthorWorksFetch {

    /** A name → its works (an empty list is a real answer, and is kept). */
    private val cache = ConcurrentHashMap<String, List<AuthorWork>>()

    /** The author's works, best-effort; empty when the name is unknown. */
    internal suspend fun works(author: String): List<AuthorWork> = withContext(Dispatchers.IO) {
        val name = author.trim()
        if (name.isBlank()) return@withContext emptyList()
        cache[name]?.let { return@withContext it }
        val resolved = runCatching { lookup(name) }.getOrDefault(emptyList())
        cache[name] = resolved
        resolved
    }

    private fun lookup(name: String): List<AuthorWork> {
        val search = httpGet(
            "https://openlibrary.org/search/authors.json?q=${Uri.encode(name)}&limit=3"
        ) ?: return emptyList()
        val key = runCatching {
            val docs = JSONObject(search).optJSONArray("docs") ?: return emptyList()
            var best: String? = null
            var bestScore = -1
            for (i in 0 until docs.length()) {
                val doc = docs.optJSONObject(i) ?: continue
                val candidate = doc.optString("key")
                if (candidate.isBlank()) continue
                // The right author first: an exact name beats a partial one,
                // and a name that only CONTAINS the query is a last resort.
                val score = when {
                    doc.optString("name").equals(name, ignoreCase = true) -> 3
                    doc.optString("name").startsWith(name, ignoreCase = true) -> 2
                    doc.optString("name").contains(name, ignoreCase = true) -> 1
                    else -> 0
                }
                if (score > bestScore) {
                    bestScore = score
                    best = candidate
                }
            }
            best
        }.getOrNull() ?: return emptyList()
        if (key.isBlank()) return emptyList()

        val body = httpGet("https://openlibrary.org/authors/$key/works.json?limit=50")
            ?: return emptyList()
        return runCatching {
            val entries = JSONObject(body).optJSONArray("entries") ?: return emptyList()
            val seen = HashSet<String>()
            (0 until entries.length()).mapNotNull { index ->
                val entry = entries.optJSONObject(index) ?: return@mapNotNull null
                val title = entry.optString("title").trim()
                if (title.isBlank() || !seen.add(title.lowercase())) return@mapNotNull null
                val coverId = entry.optJSONArray("covers")
                    ?.let { covers -> if (covers.length() > 0) covers.optInt(0) else 0 }
                    ?: 0
                AuthorWork(
                    key = entry.optString("key"),
                    title = title,
                    year = entry.optString("first_publish_date").take(4),
                    coverUrl = if (coverId > 0) {
                        "https://covers.openlibrary.org/b/id/$coverId-M.jpg"
                    } else {
                        ""
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Minimal keyless GET — 8s timeout, best-effort, exactly as the art fetchers do. */
    private fun httpGet(urlString: String): String? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Curio/1.0")
            if (conn.responseCode != 200) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

/**
 * THE AUTHOR'S SHELF, as a sheet.
 *
 * The same anatomy as the book / album / series sheets — the top hairline, a
 * header that says what this is and whose, then the list — so a name tapped on a
 * book opens something that belongs to the same family as everything else the
 * reveal opens. Each work is a row: its cover where Open Library has one, its
 * title, the year, and a tap that opens its Open Library page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthorWorksSheet(
    cat: CurioCategory,
    author: String,
    onDismiss: () -> Unit
) {
    val accent = cat.themedAccent()
    // v389e — the sheet's words are the app's own ink, not the category's accent
    // ink over a wash of that same accent (see ArtworkSheet: a sheet is where the
    // member READS, and a pale accent on its own tint is the least readable pair
    // in the app). The accent keeps the hairline and the glyphs.
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val fetchConsent = AppPreferences.bookFetchEnabledState
    var works by remember(author) { mutableStateOf<List<AuthorWork>?>(null) }
    // v389d — THE PERSON'S OWN PICTURE, in the header (user request: "show the
    // author's portrait in the author sheet header, not just on the reveal
    // card"). The reveal card already resolved one and stored it under
    // "author|<name>", so this reads that first and only goes looking when the
    // card has not been seen in this install.
    var portrait by remember(author) {
        mutableStateOf(
            AppPreferences.sheetArtUrlsState["author|$author"]?.takeIf { it.isNotBlank() }
        )
    }
    LaunchedEffect(author, fetchConsent) {
        works = AuthorWorksFetch.works(author)
        // The reveal card only looks a face up when cover fetching is ON, so
        // the sheet honours the same switch: with it off, whatever that card
        // cached is what shows and no new call goes out for a picture.
        if (portrait == null && fetchConsent) {
            val found = ArtworkFetch.portraitOrCover(author)
            if (!found.isNullOrBlank()) {
                portrait = found
                AppPreferences.setSheetArtUrl(context, "author|$author", found)
            }
        }
    }
    val listed = works

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cat.notesSheetContainerColor(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .fillMaxHeight(0.92f)
                .padding(bottom = 20.dp)
        ) {
            NotesSheetTopHairline(accent)
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                val face = portrait?.takeIf { it.isNotBlank() }
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    // v389d — the silhouette wears the sibling sheet's flat
                    // 16% tint with NO shadow: a shadow behind a translucent
                    // fill bleeds through it (AGENTS rule 11). Only the
                    // portrait, which is opaque, sits on one.
                    modifier = Modifier
                        .size(44.dp)
                        .then(if (face != null) Modifier.shadow(3.dp, CircleShape) else Modifier)
                ) {
                    if (face != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(face)
                                .crossfade(true)
                                .build(),
                            contentDescription = author,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(CurioIcons.Person, null, tint = ink, size = 20.dp)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "WRITTEN WORKS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp
                        ),
                        color = ink
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        author,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        when {
                            listed == null -> "Looking them up\u2026"
                            listed.isEmpty() -> "Nothing found in Open Library"
                            listed.size == 1 -> "1 work"
                            else -> "${listed.size} works"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ink
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (listed == null) {
                // A quiet stand-in while the two lookups run — never a spinner
                // over an empty sheet.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) { index ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (index == 0) 58.dp else 44.dp)
                            ) {}
                        }
                    }
                }
            } else if (listed.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "This name is not in Open Library, so their other books cannot be " +
                            "listed here. A search still can.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )
                    Surface(
                        onClick = {
                            openSearchUrl(
                                context,
                                "https://openlibrary.org/search?q=" + Uri.encode(author)
                            )
                        },
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            CurioIcon(CurioIcons.Search, null, tint = ink, size = 15.dp)
                            Text(
                                "SEARCH OPEN LIBRARY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = ink
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 2.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = listed, key = { it.key.ifBlank { it.title } }) { work ->
                        Surface(
                            onClick = {
                                if (work.key.isNotBlank()) {
                                    openSearchUrl(
                                        context,
                                        "https://openlibrary.org${work.key}"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = cat.categorySurface(
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(58.dp)
                                        .shadow(2.dp, RoundedCornerShape(8.dp))
                                ) {
                                    if (work.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(work.coverUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CurioIcon(
                                                CurioIcons.MenuBook,
                                                null,
                                                tint = ink.copy(alpha = 0.5f),
                                                size = 16.dp
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        work.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (work.year.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            work.year,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item("author-end") { Spacer(Modifier.height(6.dp)) }
                }
            }
        }
    }
}

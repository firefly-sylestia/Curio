package com.curio.app.features.reveal

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
import com.curio.app.ui.theme.curioSheetContainerColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategory
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newPersonalBookId
import com.curio.app.data.openSearchUrl
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.notesSheetContainerColor
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    /**
     * The works, in-process only — no device cache is read or written.
     *
     * This is what an ART CARD wants: it only needs one cover URL out of the
     * list, and it already keeps its own copy of the URL it resolved (see
     * [AppPreferences.setSheetArtUrl]). Letting a card write the whole book list
     * into prefs would be storage for nothing, so the device cache belongs to
     * [works] below, which the author sheet — the surface that actually shows
     * the list — calls.
     */
    internal suspend fun works(author: String): List<AuthorWork> = withContext(Dispatchers.IO) {
        val name = author.trim()
        if (name.isBlank()) return@withContext emptyList()
        cache[name]?.let { return@withContext it }
        val resolved = runCatching { lookup(name) }.getOrDefault(emptyList())
        cache[name] = resolved
        resolved
    }

    /**
     * The author's works, best-effort; empty when the name is unknown.
     *
     * v389e — AND THE ANSWER IS KEPT ON THE DEVICE.
     *
     * The session map below only remembers a name while the app is running, so
     * the sheet went looking again the next day (user request: "authors written
     * work should save as cache"). The list is written down (see
     * [AppPreferences.setAuthorWorksJson]) and read back as the FIRST answer, so
     * the sheet opens with the books it showed last time and the lookup only ever
     * refreshes them.
     *
     * Only an answer WITH ROWS is stored. A lookup that failed is not a fact
     * about the author — a name Open Library is briefly unable to answer for must
     * not be remembered as "wrote nothing" — so a failure falls back to whatever
     * was cached and never overwrites it.
     */
    internal suspend fun works(context: android.content.Context, author: String): List<AuthorWork> =
        withContext(Dispatchers.IO) {
            val name = author.trim()
            if (name.isBlank()) return@withContext emptyList()
            cache[name]?.let { return@withContext it }
            val resolved = runCatching { lookup(name) }.getOrDefault(emptyList())
            if (resolved.isNotEmpty()) {
                cache[name] = resolved
                AppPreferences.setAuthorWorksJson(context, name, encode(resolved))
                return@withContext resolved
            }
            val stored = stored(context, name)
            if (stored.isNotEmpty()) cache[name] = stored
            stored
        }

    /** What the device already knows about [author], without any network. */
    internal fun stored(context: android.content.Context, author: String): List<AuthorWork> {
        val name = author.trim()
        if (name.isBlank()) return emptyList()
        val json = AppPreferences.getAuthorWorksJson(context, name) ?: return emptyList()
        return runCatching {
            val array = org.json.JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val row = array.optJSONObject(index) ?: continue
                    val title = row.optString("title")
                    if (title.isBlank()) continue
                    add(
                        AuthorWork(
                            key = row.optString("key"),
                            title = title,
                            year = row.optString("year"),
                            coverUrl = row.optString("cover")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /** The list as one small JSON array — the shape [stored] reads back. */
    private fun encode(works: List<AuthorWork>): String = runCatching {
        val array = org.json.JSONArray()
        works.forEach { work ->
            array.put(
                JSONObject()
                    .put("key", work.key)
                    .put("title", work.title)
                    .put("year", work.year)
                    .put("cover", work.coverUrl)
            )
        }
        array.toString()
    }.getOrDefault("")

    // ── ONE WORK, AS OPEN LIBRARY DESCRIBES IT (v389e) ────────────────────
    //
    // The author sheet's list gave a title and a year, and tapping a row left
    // the app for a browser page. The member asked for the work to "open in the
    // app with the fetched details", so the work's own record is read here —
    // keyless, free, the same source as everything else — and the sheet renders
    // it. A description in Open Library is either a string or an object with a
    // `value` (both shapes are in the wild), so both are read.

    /** One work's own facts, as the sheet shows them. */
    internal data class WorkDetail(
        val title: String,
        val year: String,
        val coverUrl: String,
        /** The long word about the work — "" when Open Library has none. */
        val about: String,
        /** Subject headings, kept short: the first few are the useful few. */
        val subjects: List<String>
    )

    /**
     * A work's own record, memoised for the session like [works]. A miss is
     * remembered too, so a work Open Library cannot describe is not asked about
     * twice while the app is running.
     */
    internal suspend fun detail(work: AuthorWork): WorkDetail? = withContext(Dispatchers.IO) {
        val key = work.key.trim()
        if (key.isBlank()) return@withContext null
        if (detailCache.containsKey(key)) return@withContext detailCache[key]
        val json = httpGet("https://openlibrary.org$key.json")
        if (json == null) {
            detailCache[key] = null
            return@withContext null
        }
        val parsed = runCatching {
            val row = JSONObject(json)
            val description = when (val raw = row.opt("description")) {
                is String -> raw
                is JSONObject -> raw.optString("value")
                else -> ""
            }.trim()
            val subjects = runCatching {
                val array = row.optJSONArray("subjects") ?: return@runCatching emptyList()
                buildList {
                    for (index in 0 until array.length()) {
                        if (size >= 8) break
                        val subject = array.optString(index).trim()
                        // "Accessible book" and friends are library plumbing, not
                        // subjects — they would be noise as chips.
                        if (subject.isBlank() || subject.lowercase().contains("accessible")) continue
                        add(subject)
                    }
                }
            }.getOrDefault(emptyList())
            val coverFromWork = runCatching {
                val covers = row.optJSONArray("covers") ?: return@runCatching ""
                for (index in 0 until covers.length()) {
                    val id = covers.optInt(index, 0)
                    if (id > 0) return@runCatching "https://covers.openlibrary.org/b/id/$id-L.jpg"
                }
                ""
            }.getOrDefault("")
            WorkDetail(
                title = work.title.ifBlank {
                    row.optString("title").trim()
                },
                year = work.year.ifBlank {
                    row.optString("first_publish_date").take(4)
                },
                coverUrl = work.coverUrl.ifBlank { coverFromWork },
                about = description,
                subjects = subjects
            )
        }.getOrNull()
        detailCache[key] = parsed
        parsed
    }

    /** Memo for [detail] — a work key → its facts (null = asked, none found). */
    private val detailCache = ConcurrentHashMap<String, WorkDetail?>()

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

    /**
     * Minimal keyless GET, best-effort — on the SHORT budget a door in a chain is
     * allowed (v429): an author's shelf is one door of several the art lanes ask
     * at once, so it may not be the reason a sheet waits.
     */
    private fun httpGet(urlString: String): String? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 4_000
            conn.readTimeout = 5_000
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
    onOpenWork: (AuthorWork) -> Unit = {},
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
    // v389e — THE DEVICE'S ANSWER OPENS THE SHEET, not a blank beat. The list
    // that was written down the last time this author was looked up is read here
    // (a prefs read inside `remember`, so once per author and not per frame) and
    // the lookup below only refreshes it. That is the whole point of caching it:
    // the sheet shows books on its first frame, offline included.
    var works by remember(author) {
        mutableStateOf(AuthorWorksFetch.stored(context, author).takeIf { it.isNotEmpty() })
    }
    // v389d — THE PERSON'S OWN PICTURE, in the header (user request: "show the
    // author's portrait in the author sheet header, not just on the reveal
    // card"). The reveal card already resolved one and stored it under
    // "author|<name>", so this reads that first and only goes looking when the
    // card has not been seen in this install.
    // v406 — the cached portrait keys the seed and the effect alike: keyed on
    // the author alone, a picture the reveal card cached after this sheet
    // composed was never noticed (see ArtworkSheet for the full note).
    val storedPortrait = AppPreferences.sheetArtUrlsState["author|$author"]?.takeIf { it.isNotBlank() }
    var portrait by remember(author, storedPortrait) {
        mutableStateOf(storedPortrait)
    }
    LaunchedEffect(author, fetchConsent, storedPortrait) {
        // `works()` itself falls back to the stored list when the lookup finds
        // nothing, so this assignment can never blank a list already on screen.
        works = AuthorWorksFetch.works(context, author)
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
        containerColor = curioSheetContainerColor(cat.notesSheetContainerColor()),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        CurioGlassWindowBlur()
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
                            // v389e — THE TAP STAYS IN THE APP. This used to hand
                            // the member to a browser; it now opens the work's own
                            // page over this sheet (the member asked for exactly
                            // that), and the browser is a quiet pill in there for
                            // when the full Open Library page is what they want.
                            onClick = { onOpenWork(work) },
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

/**
 * ONE WORK'S OWN PAGE, IN THE APP (v389e).
 *
 * Tapping a row in the author sheet used to hand the member to a browser. The
 * member asked for the opposite: "opening them should open them in the app with
 * the fetched details". So the work opens here — its cover, its year, what Open
 * Library says about it, and its subject headings — in the same sheet anatomy as
 * the rest of the reveal (top hairline, header, one scroll), which also means it
 * can be filed onto My shelf without leaving Curio.
 *
 * The shelf pill is deliberately the BOOK pill, not a collection toggle: a work
 * you liked is something you intend to read, so it lands on My shelf as a book
 * with its real author and cover, ready for chapters and notes (the same shape
 * the book reveal's own pill writes). Adding is additive, never destructive —
 * tapping twice does nothing rather than removing anything.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AuthorWorkSheet(
    cat: CurioCategory,
    author: String,
    work: AuthorWork,
    onDismiss: () -> Unit
) {
    val accent = cat.themedAccent()
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var detail by remember(work.key) { mutableStateOf<AuthorWorksFetch.WorkDetail?>(null) }
    var looked by remember(work.key) { mutableStateOf(false) }
    var onShelf by remember(work.key) { mutableStateOf(false) }
    var aboutExpanded by remember(work.key) { mutableStateOf(true) }

    LaunchedEffect(work.key) {
        detail = AuthorWorksFetch.detail(work)
        looked = true
    }
    // Whether this work is already on My shelf, so the pill opens in the right
    // state instead of offering to add something that is already there.
    LaunchedEffect(work.key) {
        onShelf = withContext(Dispatchers.IO) {
            runCatching {
                PersonalRepositoryHolder.repo.books().any {
                    it.title.trim().equals(work.title.trim(), ignoreCase = true)
                }
            }.getOrDefault(false)
        }
    }

    val cover = detail?.coverUrl?.takeIf { it.isNotBlank() }
        ?: work.coverUrl.takeIf { it.isNotBlank() }
    val year = detail?.year?.takeIf { it.isNotBlank() } ?: work.year
    val about = detail?.about?.takeIf { it.isNotBlank() }
    val subjects = detail?.subjects.orEmpty()

    fun shelve() {
        if (onShelf) return
        onShelf = true
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        val now = System.currentTimeMillis()
        val shelfCover = cover.orEmpty()
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    PersonalRepositoryHolder.repo.saveBook(
                        PersonalBookEntity(
                            id = newPersonalBookId(),
                            title = work.title,
                            author = author,
                            coverUrl = shelfCover,
                            createdAtMillis = now,
                            updatedAtMillis = now
                        )
                    )
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioSheetContainerColor(cat.notesSheetContainerColor()),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        CurioGlassWindowBlur()
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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier
                        .width(66.dp)
                        .height(96.dp)
                        // Shadow first, then the fill/clip — a shadow after the
                        // background paints its blur over the artwork
                        // (AGENTS rule 11).
                        .shadow(3.dp, RoundedCornerShape(10.dp))
                ) {
                    if (cover != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(cover)
                                .crossfade(true)
                                .build(),
                            contentDescription = work.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CurioIcon(CurioIcons.MenuBook, null, tint = ink.copy(alpha = 0.5f), size = 20.dp)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "WRITTEN WORK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp
                        ),
                        color = ink
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        work.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        buildString {
                            append(author)
                            if (year.isNotBlank()) append(" \u00b7 $year")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Surface(
                    onClick = { shelve() },
                    shape = RoundedCornerShape(50),
                    // Opaque when it is already shelved, so the pill reads as a
                    // state and not as a pressed button.
                    color = if (onShelf) accent else accent.copy(alpha = 0.14f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        CurioIcon(
                            if (onShelf) CurioIcons.Check else CurioIcons.Add,
                            null,
                            tint = if (onShelf) {
                                cat.onAccent()
                            } else {
                                ink
                            },
                            size = 15.dp
                        )
                        Text(
                            if (onShelf) "ON MY SHELF" else "ADD TO MY SHELF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = if (onShelf) cat.onAccent() else ink
                        )
                    }
                }
                if (work.key.isNotBlank()) {
                    Surface(
                        onClick = {
                            openSearchUrl(context, "https://openlibrary.org${work.key}")
                        },
                        shape = RoundedCornerShape(50),
                        color = surface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            CurioIcon(CurioIcons.OpenInNew, null, tint = ink, size = 14.dp)
                            Text(
                                "OPEN LIBRARY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = ink
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    !looked -> {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(72.dp)
                        ) {}
                    }
                    about == null && subjects.isEmpty() -> {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Open Library has no description for this work yet. Its page " +
                                "still has the editions, so the link above is the way in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    else -> {
                        if (about != null) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clickable { aboutExpanded = !aboutExpanded }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = accent.copy(alpha = 0.16f)
                                        ) {
                                            CurioIcon(
                                                CurioIcons.MenuBook,
                                                null,
                                                tint = ink,
                                                size = 15.dp,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                        Text(
                                            "ABOUT THIS WORK",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.2.sp
                                            ),
                                            color = ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            if (aboutExpanded) "Hide" else "Read",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = ink.copy(alpha = 0.9f)
                                        )
                                        CurioIcon(
                                            if (aboutExpanded) {
                                                CurioIcons.KeyboardArrowUp
                                            } else {
                                                CurioIcons.KeyboardArrowDown
                                            },
                                            if (aboutExpanded) "Collapse description" else "Expand description",
                                            tint = ink,
                                            size = 20.dp
                                        )
                                    }
                                    Text(
                                        about,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        color = onSurface,
                                        maxLines = if (aboutExpanded) Int.MAX_VALUE else 3,
                                        overflow = if (aboutExpanded) {
                                            TextOverflow.Clip
                                        } else {
                                            TextOverflow.Ellipsis
                                        },
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }
                        }
                        if (subjects.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "SUBJECTS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = ink
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    subjects.forEach { subject ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = surface
                                        ) {
                                            Text(
                                                subject,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 6.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

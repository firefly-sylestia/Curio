package com.curio.app.features.reveal

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.curio.app.data.CurioTopic
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
 * v389d — A WORK OF ART, AND WHO MADE IT (user request: "also add artwork button
 * sheet too … do the both", i.e. both free sources).
 *
 * Two keyless sources, used together because neither is complete on its own:
 *
 *  · THE METROPOLITAN MUSEUM OF ART'S OPEN ACCESS API
 *      `collectionapi.metmuseum.org/public/collection/v1` — search, then the
 *      object. Gives the title as the museum has it, the artist, the date, the
 *      medium, the department and a public-domain image. Exact, structured, and
 *      free with no key; its weakness is that it only knows what the Met holds.
 *  · WIKIPEDIA'S PAGE SUMMARY
 *      `en.wikipedia.org/api/rest_v1/page/summary/<title>` — the words that
 *      actually explain the work, plus a thumbnail and its page. Broad, prose,
 *      and free; its weakness is that it is prose and may describe a different
 *      painting with the same name.
 *
 * So the Met's record is trusted for the FACTS and Wikipedia's for the WORDS;
 * either may be missing, and the sheet draws whatever arrived. Nothing is
 * written to the database, and both answers are cached per title for the
 * session.
 */
internal data class ArtworkInfo(
    val title: String,
    val artist: String,
    val date: String,
    val medium: String,
    val museum: String,
    val imageUrl: String,
    val summary: String,
    /** Where the record lives, for the "read more" door. */
    val pageUrl: String
)

object ArtworkFetch {

    private const val MET = "https://collectionapi.metmuseum.org/public/collection/v1"

    /** title|artist → the record (an absent record is cached as null-free). */
    private val cache = ConcurrentHashMap<String, ArtworkInfo>()

    /** The work's own record, best-effort; null when neither source knows it. */
    internal suspend fun artwork(title: String, artist: String): ArtworkInfo? = withContext(Dispatchers.IO) {
        val key = "${title.trim()}|${artist.trim()}"
        if (title.isBlank()) return@withContext null
        cache[key]?.let { return@withContext it }
        val met = runCatching { metObject(title, artist) }.getOrNull()
        val wiki = runCatching { wikiSummary(title, artist) }.getOrNull()
        if (met == null && wiki == null) return@withContext null
        val merged = ArtworkInfo(
            title = met?.title?.takeIf { it.isNotBlank() } ?: title.trim(),
            artist = met?.artist.orEmpty(),
            date = met?.date.orEmpty(),
            medium = met?.medium.orEmpty(),
            museum = met?.museum.orEmpty(),
            imageUrl = met?.imageUrl?.takeIf { it.isNotBlank() } ?: wiki?.imageUrl.orEmpty(),
            summary = wiki?.summary.orEmpty(),
            pageUrl = met?.pageUrl?.takeIf { it.isNotBlank() } ?: wiki?.pageUrl.orEmpty()
        )
        cache[key] = merged
        merged
    }

    /**
     * THE MAKER'S OTHER WORKS, for an artist or a painter's own topic: the Met's
     * own `artistOrCulture` search, whose hits are already attributed, so no
     * false positives are dragged in by a name that is also a place or a word.
     * Capped — a sheet is not a catalogue, and each row costs one lookup. [limit]
     * is how many records to open, so the reveal card can ask for one and the
     * sheet can ask for eight.
     */
    internal suspend fun worksBy(maker: String, limit: Int = MAKER_LIMIT): List<AuthorWork> =
        withContext(Dispatchers.IO) {
        val name = maker.trim()
        if (name.isBlank()) return@withContext emptyList()
        // Only the FULL list is kept: a one-row answer belongs to its caller.
        if (limit >= MAKER_LIMIT) makerCache[name]?.let { return@withContext it }
        val resolved = runCatching {
            val ids = metIds(name, artistScoped = true).take(limit)
            ids.mapNotNull { id ->
                val objectJson = getJson("$MET/objects/$id") ?: return@mapNotNull null
                val row = runCatching { JSONObject(objectJson) }.getOrNull() ?: return@mapNotNull null
                val title = row.optString("title").trim()
                if (title.isBlank()) return@mapNotNull null
                AuthorWork(
                    key = row.optString("objectURL"),
                    title = title,
                    year = row.optString("objectDate").take(4),
                    coverUrl = row.optString("primaryImageSmall")
                        .ifBlank { row.optString("primaryImage") }
                )
            }
        }.getOrDefault(emptyList())
        if (limit >= MAKER_LIMIT) makerCache[name] = resolved
        resolved
    }

    private val makerCache = ConcurrentHashMap<String, List<AuthorWork>>()

    /**
     * A WORK THE MAKER MADE, for an artist's or a painter's own card: the first
     * of their attributed Met works that actually has a picture. One search and
     * a couple of lookups, taken from the same cached list the sheet reads, so
     * opening the maker's sheet afterwards costs nothing.
     */
    suspend fun makerArtwork(maker: String): String? {
        val name = maker.trim()
        if (name.isBlank()) return null
        return worksBy(name, limit = CARD_LOOKUPS)
            .firstOrNull { it.coverUrl.isNotBlank() }
            ?.coverUrl
    }

    /**
     * A PERSON'S OWN PICTURE, for an author's card: Wikipedia's lead image for
     * their name is the portrait a reader recognizes. When there is none — many
     * authors have no photograph — the cover of the first of their books that
     * Open Library has one for stands in, because an author with no face still
     * has a shelf.
     */
    suspend fun portraitOrCover(name: String): String? {
        val person = name.trim()
        if (person.isBlank()) return null
        val portrait = runCatching { wikiLeadImage(person) }.getOrNull()
        if (!portrait.isNullOrBlank()) return portrait
        return AuthorWorksFetch.works(person).firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
    }

    /** The page's own lead image for a NAME (a person, here) — or null. */
    private fun wikiLeadImage(name: String): String? {
        val slug = Uri.encode(name.replace(' ', '_'))
        val body = getJson("https://en.wikipedia.org/api/rest_v1/page/summary/$slug")
            ?: return null
        val row = runCatching { JSONObject(body) }.getOrNull() ?: return null
        // A disambiguation page is not a person, and a page with no image has
        // nothing to draw — both answer null rather than a wrong face.
        if (row.optString("type") != "standard") return null
        return row.optJSONObject("originalimage")?.optString("source")
            ?.takeIf { it.isNotBlank() }
            ?: row.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
    }

    /** The Met's search ids for a query, best-effort. */
    private fun metIds(query: String, artistScoped: Boolean): List<Int> {
        val url = buildString {
            append("$MET/search?hasImages=true&q=")
            append(Uri.encode(query))
            if (artistScoped) append("&artistOrCulture=true")
        }
        val body = getJson(url) ?: return emptyList()
        return runCatching {
            val ids = JSONObject(body).optJSONArray("objectIDs") ?: return emptyList()
            (0 until ids.length()).map { ids.optInt(it) }
        }.getOrDefault(emptyList())
    }

    /** The Met's object record for a title, matched by name — or null. */
    private fun metObject(title: String, artist: String): ArtworkInfo? {
        val ids = metIds(title, artistScoped = false).take(5)
        if (ids.isEmpty()) return null
        val wanted = normalise(title)
        for (id in ids) {
            val body = getJson("$MET/objects/$id") ?: continue
            val row = runCatching { JSONObject(body) }.getOrNull() ?: continue
            val name = row.optString("title").trim()
            if (name.isBlank()) continue
            // A search hit is not a match: the museum's title has to BE the
            // work's, or its first words have to be.
            val score = when {
                normalise(name) == wanted -> 3
                normalise(name).startsWith(wanted) || wanted.startsWith(normalise(name)) -> 2
                else -> 0
            }
            if (score == 0) continue
            return ArtworkInfo(
                title = name,
                artist = row.optString("artistDisplayName").trim(),
                date = row.optString("objectDate").trim(),
                medium = row.optString("medium").trim(),
                museum = row.optString("department").trim(),
                imageUrl = row.optString("primaryImage").ifBlank {
                    row.optString("primaryImageSmall")
                },
                summary = "",
                pageUrl = row.optString("objectURL")
            )
        }
        return null
    }

    /** Wikipedia's page summary for a work, or null when it has no page/words. */
    private fun wikiSummary(title: String, artist: String): ArtworkInfo? {
        val slug = Uri.encode(title.trim().replace(' ', '_'))
        val body = getJson("https://en.wikipedia.org/api/rest_v1/page/summary/$slug")
            ?: return null
        val row = runCatching { JSONObject(body) }.getOrNull() ?: return null
        // A disambiguation page or a same-name film says nothing about a
        // painting — Wikipedia itself tells us which pages are not articles.
        if (row.optString("type") != "standard") return null
        val words = row.optString("extract").trim()
        if (words.length < MIN_SUMMARY) return null
        // When the catalog names a maker, the page has to mention them: a
        // same-titled work by someone else is a different work.
        if (artist.isNotBlank()) {
            val surname = artist.trim().split(' ').lastOrNull().orEmpty()
            if (surname.length >= 4 && !words.contains(surname, ignoreCase = true)) return null
        }
        val image = row.optJSONObject("thumbnail")?.optString("source").orEmpty()
        val page = row.optJSONObject("content_urls")
            ?.optJSONObject("desktop")
            ?.optString("page")
            .orEmpty()
        return ArtworkInfo(
            title = row.optString("title").ifBlank { title },
            artist = artist.trim(),
            date = "",
            medium = "",
            museum = "",
            imageUrl = image,
            summary = words,
            pageUrl = page.ifBlank {
                "https://en.wikipedia.org/wiki/$slug"
            }
        )
    }

    /** Letters and digits only, lowercased — the same rule the book matcher uses. */
    private fun normalise(value: String): String =
        value.lowercase().filter { it.isLetterOrDigit() }

    /** Minimal keyless GET — 8s timeout, best-effort, with a plain User-Agent. */
    private fun getJson(urlString: String): String? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Curio/1.0 (https://curio.app)")
            if (conn.responseCode != 200) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /** A sheet is not a catalogue: eight rows is more than fits without scrolling. */
    private const val MAKER_LIMIT = 8

    /** A CARD needs one picture, so it opens this many records and no more. */
    private const val CARD_LOOKUPS = 3

    /** Below this many characters Wikipedia has said nothing worth quoting. */
    private const val MIN_SUMMARY = 80
}

/**
 * THE ART LANE'S SHEETS — one composable, two questions.
 *
 * [Mode.WORK] answers "what is this thing": the image, the maker, the date, the
 * medium, the museum and the words, with doors to its record. [Mode.MAKER]
 * answers "what else did they make": the Met's attributed works by that artist.
 * Both wear the same anatomy as every other sheet in the app (top hairline,
 * header, list) so the art lane stops being the one with nowhere to go.
 */
internal enum class ArtworkSheetMode { WORK, MAKER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArtworkSheet(
    cat: CurioCategory,
    topic: CurioTopic,
    mode: ArtworkSheetMode,
    onDismiss: () -> Unit
) {
    val accent = cat.themedAccent()
    val ink = cat.categoryInk()
    val surface = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    // The catalog's own words are the fallback copy for a work with no page.
    val catalogWords = topic.synopsis?.takeIf { it.isNotBlank() } ?: topic.teaser

    var info by remember(topic.name) { mutableStateOf<ArtworkInfo?>(null) }
    var makerWorks by remember(topic.name) { mutableStateOf<List<AuthorWork>?>(null) }
    LaunchedEffect(topic.name, mode) {
        when (mode) {
            ArtworkSheetMode.WORK -> info = ArtworkFetch.artwork(topic.name, topic.byline)
            ArtworkSheetMode.MAKER -> makerWorks = ArtworkFetch.worksBy(topic.name)
        }
    }

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

            if (mode == ArtworkSheetMode.WORK) {
                // A Wikipedia summary can run long and the image is tall, so the
                // work's own half scrolls rather than being cut off.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                WorkHeader(
                    topic = topic,
                    info = info,
                    ink = ink,
                    surface = surface,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant,
                    catalogWords = catalogWords
                )
                Spacer(Modifier.height(12.dp))
                // THE DOOR TO THE RECORD, when one answered at all — a
                // museum page or a Wikipedia article, whichever the facts and
                // the words came from.
                info?.pageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    Surface(
                        onClick = { openSearchUrl(context, url) },
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.14f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.Search,
                                null,
                                tint = ink,
                                size = 15.dp
                            )
                            Text(
                                "THE RECORD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = ink
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(16.dp))
                }
            } else {
                MakerHeader(
                    topic = topic,
                    count = makerWorks,
                    ink = ink,
                    onSurface = onSurface
                )
                Spacer(Modifier.height(12.dp))
                val listed = makerWorks.orEmpty()
                if (listed.isEmpty()) {
                    Text(
                        if (makerWorks == null) "Looking them up\u2026"
                        else "The Met holds no attributed works for this name.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
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
                                        openSearchUrl(context, work.key)
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
                                        shape = RoundedCornerShape(10.dp),
                                        color = cat.categorySurface(
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        modifier = Modifier
                                            .size(52.dp)
                                            .shadow(2.dp, RoundedCornerShape(10.dp))
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
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        } else {
                                            Box(
                                                Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CurioIcon(
                                                    CurioIcons.Palette,
                                                    null,
                                                    tint = ink.copy(alpha = 0.5f),
                                                    size = 18.dp
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
                        item("maker-end") { Spacer(Modifier.height(6.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkHeader(
    topic: CurioTopic,
    info: ArtworkInfo?,
    ink: Color,
    surface: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    catalogWords: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val image = info?.imageUrl.orEmpty()
        if (image.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${topic.name} artwork",
                    // FIT: a painting is a shape, and cropping it changes what
                    // it is.
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "THE WORK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                ),
                color = ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                info?.title?.takeIf { it.isNotBlank() } ?: topic.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val credit = listOf(info?.artist.orEmpty(), info?.date.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" \u00b7 ")
                .ifBlank { topic.byline }
            if (credit.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    credit,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ink.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val facts = listOf(info?.medium.orEmpty(), info?.museum.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" \u00b7 ")
            if (facts.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    facts,
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val words = info?.summary?.takeIf { it.isNotBlank() } ?: catalogWords
            if (words.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    words,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MakerHeader(
    topic: CurioTopic,
    count: List<AuthorWork>?,
    ink: Color,
    onSurface: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Surface(shape = CircleShape, color = ink.copy(alpha = 0.16f)) {
            CurioIcon(
                CurioIcons.Person,
                null,
                tint = ink,
                size = 20.dp,
                modifier = Modifier.padding(9.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "THEIR WORKS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                ),
                color = ink
            )
            Spacer(Modifier.height(3.dp))
            Text(
                topic.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                when {
                    count == null -> "Looking them up\u2026"
                    count.isEmpty() -> "Nothing attributed in the Met"
                    count.size == 1 -> "1 work in the Met"
                    else -> "${count.size} works in the Met"
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ink
            )
        }
    }
}

/**
 * WHICH ART LANE'S CARD this is — which also decides what its cover art IS and
 * where it is fetched from: an artwork shows ITSELF (the Met's public-domain
 * image, then Wikipedia's), an artist or a painter shows a work the Met
 * attributes to them, and an author is a person, so the only real cover they
 * have is their own portrait — Wikipedia's lead image for their name, and the
 * cover of the first book of theirs with one when even that is missing.
 */
internal enum class ArtworkLane { WORK, MAKER, AUTHOR }

/**
 * THE ART LANE'S DOOR on the reveal — the same card the book, album, series,
 * film, anime and song sections wear, so an artwork, an artist, a painter and an
 * author each open something instead of standing still.
 *
 * v389d — AND THE CARD WEARS THEIR PICTURE, the way the film, anime and song
 * cards already do (user request: "fetch real cover art for the art and author
 * section cards on the reveal"). The resolved URL is PERSISTED per topic and
 * lane, so the reveal never asks twice and the sheet that opens on a tap is
 * painted from the very same image.
 */
@Composable
internal fun ArtworkInfoSection(
    cat: CurioCategory,
    topic: CurioTopic,
    lane: ArtworkLane,
    label: String,
    glyph: String,
    hint: String,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fetchConsent = AppPreferences.bookFetchEnabledState
    val artKey = when (lane) {
        ArtworkLane.WORK -> "artwork|${topic.name}"
        ArtworkLane.MAKER -> "artist|${topic.name}"
        ArtworkLane.AUTHOR -> "author|${topic.name}"
    }
    var artUrl by remember(topic.imageUrl) {
        mutableStateOf(
            AppPreferences.sheetArtUrlsState[artKey]?.takeIf { it.isNotBlank() }
                ?: topic.imageUrl?.takeIf { it.isNotBlank() }
        )
    }
    LaunchedEffect(topic.imageUrl, fetchConsent) {
        val stored = AppPreferences.sheetArtUrlsState[artKey]?.takeIf { it.isNotBlank() }
        val resolved = stored ?: if (fetchConsent) {
            when (lane) {
                ArtworkLane.WORK -> ArtworkFetch.artwork(topic.name, topic.byline)?.imageUrl
                ArtworkLane.MAKER -> ArtworkFetch.makerArtwork(topic.name)
                ArtworkLane.AUTHOR -> ArtworkFetch.portraitOrCover(topic.name)
            }
        } else {
            null
        }
        artUrl = resolved ?: topic.imageUrl?.takeIf { it.isNotBlank() }
        if (resolved != null && stored == null) {
            AppPreferences.setSheetArtUrl(context, artKey, resolved)
        }
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surface),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSheet)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        CurioIcon(glyph, null, tint = cat.categoryInk(), size = 16.dp)
                    }
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = cat.categoryInk()
                )
                Spacer(Modifier.weight(1f))
                if (topic.byline.isNotBlank()) {
                    Text(
                        topic.byline,
                        style = MaterialTheme.typography.labelSmall,
                        color = cat.categoryInk().copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!artUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${topic.name} artwork",
                    // A painting is a shape and cropping it changes what it is,
                    // so an artwork FITS inside the card's band. A maker's work
                    // and a portrait are pictures of a person's work or face and
                    // fill it, exactly as the film and song cards do.
                    contentScale = if (lane == ArtworkLane.WORK) {
                        ContentScale.Fit
                    } else {
                        ContentScale.Crop
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            if (topic.teaser.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    topic.teaser,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cat.categoryInk().copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                ),
                color = cat.categoryInk()
            )
        }
    }
}

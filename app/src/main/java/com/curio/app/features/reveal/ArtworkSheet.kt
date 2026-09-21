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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

/**
 * v407 — A MAKER'S OWN RECORD (an artist or a painter), from Wikipedia: the
 * prose that describes them, their lead image, and their article. The Met is
 * asked first for their works (see [ArtworkFetch.worksBy]) because its answers
 * are attributed, but the Met only knows what the Met holds — this is what
 * fills an artist or painter page that the museum's catalogue has nothing to
 * say about (user: "artist info wasnt loading at all, and many painter info
 * wasnt loading too").
 */
internal data class MakerInfo(
    val name: String,
    val summary: String,
    val portraitUrl: String,
    val pageUrl: String
)

object ArtworkFetch {

    private const val MET = "https://collectionapi.metmuseum.org/public/collection/v1"

    /** title|artist → the record (an absent record is cached as null-free). */
    private val cache = ConcurrentHashMap<String, ArtworkInfo>()

    /**
     * v429 — A WORK NO DOOR HOLDS, REMEMBERED (keyed exactly as [cache]).
     *
     * Only a CONFIRMED miss lands here — every door reached its catalogue and
     * each answered "not here" — and that distinction is the whole fail-safe: a
     * door that FAILED (a timeout, a dead line) is not an answer, so it is not
     * remembered and the next open tries again (the app's own rule, learned the
     * expensive way on the shelf: *"a lookup that failed is no longer remembered
     * as done"*). Without this, a painting neither museum holds was re-asked of
     * three sources on every single open — the sheet re-ran the whole chain to
     * arrive at the same nothing.
     */
    private val misses: MutableSet<String> = java.util.Collections.newSetFromMap(ConcurrentHashMap())

    /** The work's own record, best-effort; null when neither source knows it. */
    internal suspend fun artwork(title: String, artist: String): ArtworkInfo? = withContext(Dispatchers.IO) {
        val key = "${title.trim()}|${artist.trim()}"
        if (title.isBlank()) return@withContext null
        cache[key]?.let { return@withContext it }
        if (key in misses) return@withContext null
        // v407 — THE TWO SOURCES ARE QUERIED AT THE SAME TIME. They are
        // independent, but they used to run back to back: the Met's search,
        // then up to five object records, THEN the Wikipedia article — so a
        // painting's sheet waited for the SUM of both round trips (user: "the
        // painting artworks loading was so slow"). In parallel the wait is the
        // slower of the two, and each source still fails alone.
        // v426b — AND CLEVELAND IS ASKED WITH THEM. It is the third door for a
        // WORK (the Met's catalogue is one museum's; Cleveland's is another's,
        // opened without a key), and it is the only source that can fill in a
        // painting the Met does not hold at all — which used to leave the sheet
        // reading an encyclopedia article about the painting instead of a
        // museum's own record of it. The three run together, so the wait is
        // still the slowest of them rather than their sum.
        val (metResult, clevelandResult, wikiResult) = coroutineScope {
            val metJob = async { runCatching { metObject(title, artist) } }
            val clevelandJob = async { runCatching { MuseumFetch.work(title, artist) } }
            val wikiJob = async { runCatching { wikiSummary(title, artist) } }
            Triple(metJob.await(), clevelandJob.await(), wikiJob.await())
        }
        val met = metResult.getOrNull()
        val cleveland = clevelandResult.getOrNull()
        val wiki = wikiResult.getOrNull()
        if (met == null && cleveland == null && wiki == null) {
            // A miss is only a miss when nobody FAILED: if any door threw, the
            // next open is allowed to ask again (see [misses]).
            val answered = metResult.isSuccess && clevelandResult.isSuccess && wikiResult.isSuccess
            if (answered) misses += key
            return@withContext null
        }
        val merged = ArtworkInfo(
            title = met?.title?.takeIf { it.isNotBlank() }
                ?: cleveland?.title?.takeIf { it.isNotBlank() }
                ?: title.trim(),
            // The Met answers attributed; Cleveland is the second museum's own
            // record; Wikipedia supplies the prose, not the facts.
            artist = met?.artist.orEmpty().ifBlank { cleveland?.maker.orEmpty() },
            date = met?.date.orEmpty().ifBlank { cleveland?.date.orEmpty() },
            medium = met?.medium.orEmpty().ifBlank { cleveland?.medium.orEmpty() },
            museum = met?.museum.orEmpty().ifBlank { cleveland?.museum.orEmpty() },
            imageUrl = met?.imageUrl?.takeIf { it.isNotBlank() }
                ?: cleveland?.imageUrl?.takeIf { it.isNotBlank() }
                ?: wiki?.imageUrl.orEmpty(),
            summary = wiki?.summary.orEmpty(),
            pageUrl = met?.pageUrl?.takeIf { it.isNotBlank() }
                ?: cleveland?.pageUrl?.takeIf { it.isNotBlank() }
                ?: wiki?.pageUrl.orEmpty()
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
            // v429 — the maker's records are read AT ONCE, in the order asked for
            // (see [metObject]): a list of eight was eight round trips in a row,
            // which is what made an artist's sheet sit on its own empty list.
            val bodies = coroutineScope {
                val jobs = ids.map { id -> async { getJson("$MET/objects/$id") } }
                jobs.map { job -> job.await() }
            }
            val fromMet = bodies.mapNotNull { objectJson ->
                if (objectJson == null) return@mapNotNull null
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
            // v426b — THE SECOND MUSEUM, and the works the Met simply does not
            // hold. Cleveland answers with its own attributed rows (asked here
            // because its own `artist` parameter does NOT filter — see
            // [MuseumFetch]), so an artist's or painter's page is no longer
            // empty just because the Met happens to hold nothing by them.
            // The Met's own hits come first; Cleveland fills the rest of the
            // list, and a work both museums hold stays one row.
            val fromCleveland = runCatching { MuseumFetch.worksBy(name, limit) }
                .getOrDefault(emptyList())
            (fromMet + fromCleveland)
                .distinctBy { it.title.trim().lowercase() }
                .take(limit)
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
            // v407 — THE PERSON'S OWN PICTURE WHEN THE MUSEUM HAS NOTHING.
            // The Met only knows what it holds, so an artist or a painter with
            // no attribution there used to leave the reveal card with no image
            // at all (user: "artist info wasnt loading at all, and many painter
            // info wasnt loading too"). Their Wikipedia lead image is the
            // fallback — the same rule the AUTHOR lane already lives by.
            ?: makerInfo(name)?.portraitUrl?.takeIf { it.isNotBlank() }
    }

    /**
     * v407 — WHO THE MAKER IS, in Wikipedia's words (with their lead image and
     * their article), for the ARTIST / PAINTER sheet and card. Memoized per
     * name; null when the page is a disambiguation or says nothing at all.
     */
    internal suspend fun makerInfo(maker: String): MakerInfo? = withContext(Dispatchers.IO) {
        val person = maker.trim()
        if (person.isBlank()) return@withContext null
        makerInfoCache[person]?.let { return@withContext it }
        // v426b — WIKIPEDIA FIRST, AND THE MUSEUM'S OWN WORDS WHEN THERE IS NO
        // ARTICLE. Cleveland writes a real paragraph about the makers it holds,
        // which is what gives an artist or painter page words when Wikipedia has
        // no page for them at all (the same gap this file's Met-first rule left
        // open for a maker the Met does not hold either).
        val record = runCatching { wikiPerson(person) }.getOrNull()
            ?: runCatching {
                MuseumFetch.makerBio(person)
                    ?.takeIf { it.length >= MIN_SUMMARY }
                    ?.let { bio ->
                        MakerInfo(
                            name = person,
                            summary = bio,
                            // The biography carries no picture; the card's own
                            // fallbacks (their works, then their books) stand.
                            portraitUrl = "",
                            pageUrl = ""
                        )
                    }
            }.getOrNull()
            ?: return@withContext null
        makerInfoCache[person] = record
        record
    }

    /** Wikipedia's page summary for a PERSON: prose + lead image + article. */
    private fun wikiPerson(name: String): MakerInfo? {
        val slug = Uri.encode(name.replace(' ', '_'))
        val body = getJson("https://en.wikipedia.org/api/rest_v1/page/summary/$slug")
            ?: return null
        val row = runCatching { JSONObject(body) }.getOrNull() ?: return null
        // A disambiguation page is not a person.
        if (row.optString("type") != "standard") return null
        val words = row.optString("extract").trim()
        val image = row.optJSONObject("originalimage")?.optString("source")
            ?.takeIf { it.isNotBlank() }
            ?: row.optJSONObject("thumbnail")?.optString("source").orEmpty()
        // Nothing to say and nothing to show is not a record.
        if (words.length < MIN_SUMMARY && image.isBlank()) return null
        val page = row.optJSONObject("content_urls")
            ?.optJSONObject("desktop")
            ?.optString("page")
            .orEmpty()
        return MakerInfo(
            name = row.optString("title").ifBlank { name },
            summary = words,
            portraitUrl = image,
            pageUrl = page.ifBlank { "https://en.wikipedia.org/wiki/$slug" }
        )
    }

    /** name → the maker's record (absent answer is not cached). */
    private val makerInfoCache = ConcurrentHashMap<String, MakerInfo>()

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
    private fun wikiLeadImage(name: String): String? =
        wikiPerson(name)?.portraitUrl?.takeIf { it.isNotBlank() }

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

    /**
     * The Met's object record for a title, matched by name — or null.
     *
     * v429 — THE THREE RECORDS ARE READ AT ONCE. They are independent reads of
     * the same catalogue, and they used to run one after another: three round
     * trips INSIDE a door that is itself one of three running in parallel, so a
     * slow line could make this single source cost three times its budget while
     * the other two doors had long finished. The order is unchanged — the list is
     * awaited in the order the search returned it, so the same record still wins.
     */
    private suspend fun metObject(title: String, artist: String): ArtworkInfo? {
        // v407 — three records, not five: each one is a round trip and the
        // title match almost always arrives on the first or second hit.
        val ids = metIds(title, artistScoped = false).take(3)
        if (ids.isEmpty()) return null
        val bodies = coroutineScope {
            val jobs = ids.map { id -> async { getJson("$MET/objects/$id") } }
            jobs.map { job -> job.await() }
        }
        val wanted = normalise(title)
        for (body in bodies) {
            if (body == null) continue
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
                // v407 — THE WEB-SIZE IMAGE FIRST. `primaryImage` is the
                // museum's full-resolution original — routinely several
                // thousand pixels and several megabytes — while the card that
                // draws it is 200–230dp tall, so the sheet was downloading a
                // poster to paint a stamp (the other half of "the painting
                // artworks loading was so slow").
                imageUrl = row.optString("primaryImageSmall").ifBlank {
                    row.optString("primaryImage")
                },
                summary = "",
                pageUrl = row.optString("objectURL")
            )
        }
        return null
    }

    /**
     * Wikipedia's page summary for a work, or null when it has no page/words.
     *
     * v429 — AND WHEN THE BARE TITLE IS NOT A PAGE, THE ARTICLE IS SEARCHED FOR.
     *
     * The read used to be a single direct slug (`…/summary/The_Starry_Night`),
     * which is right only when the work's name IS the article's name. A great
     * many paintings are filed with a disambiguator — "Nighthawks (painting)",
     * "Guernica (Picasso)", "The Kiss (Klimt)" — so the direct read answered
     * nothing for exactly the works whose names are also a word, a place or a
     * band, and the sheet fell back to whichever museum happened to be holding
     * one. The shared [WikipediaSummary] door is the search-then-best-hit path
     * (with `Kind.ART` preferring the bracket that says "painting"), and it is
     * asked only when the direct read actually came up empty.
     */
    private fun wikiSummary(title: String, artist: String): ArtworkInfo? {
        val direct = wikiSummaryOf(title, artist)
        if (direct != null) return direct
        val page = WikipediaSummary.page(
            title,
            kind = WikipediaSummary.Kind.ART
        ) ?: return null
        return if (normalise(page) == normalise(title)) null else wikiSummaryOf(page, artist)
    }

    /** One REST summary read, validated, as a work's own record. */
    private fun wikiSummaryOf(title: String, artist: String): ArtworkInfo? {
        if (title.isBlank()) return null
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

    /**
     * Minimal keyless GET, best-effort — on the SHORT budget a door in a chain is
     * allowed (v429): the Met, Cleveland and Wikipedia are asked at once for a
     * work, so no one of them may hold the sheet.
     */
    private fun getJson(urlString: String): String? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 4_000
            conn.readTimeout = 5_000
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
    // ── v389e — THE WORDS ARE INK, NOT THE ACCENT ────────────────────────
    //
    // The sheet drew every word in the category's own INK — an accent-derived
    // colour — over a container that is a pale tint of that SAME accent (see
    // `notesSheetContainerColor`). A pale accent on its own wash is the worst
    // contrast in the app, and this is the sheet a member opens to READ: the
    // words are the app's own ink now, and the accent stays where it belongs —
    // the hairline, the record pill, a glyph's tint (user report: "for artowkrs
    // and the authors artists singers etc the review in topic reveal is bad with
    // the text colro for the buttom sheet").
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    // The catalog's own words are the fallback copy for a work with no page.
    val catalogWords = topic.synopsis?.takeIf { it.isNotBlank() } ?: topic.teaser

    var info by remember(topic.name) { mutableStateOf<ArtworkInfo?>(null) }
    var makerWorks by remember(topic.name) { mutableStateOf<List<AuthorWork>?>(null) }
    // v407 — the maker's OWN record (prose + portrait) as well as the Met's
    // list of their works.
    var makerAbout by remember(topic.name) { mutableStateOf<MakerInfo?>(null) }
    LaunchedEffect(topic.name, mode) {
        when (mode) {
            ArtworkSheetMode.WORK -> info = ArtworkFetch.artwork(topic.name, topic.byline)
            ArtworkSheetMode.MAKER -> coroutineScope {
                // The two lookups are independent, so they run side by side:
                // the sheet waits for the slower one, not for their sum.
                val worksJob = async { ArtworkFetch.worksBy(topic.name) }
                val aboutJob = async { ArtworkFetch.makerInfo(topic.name) }
                makerWorks = worksJob.await()
                makerAbout = aboutJob.await()
            }
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
                    about = makerAbout,
                    ink = ink,
                    onSurface = onSurface
                )
                Spacer(Modifier.height(12.dp))
                val listed = makerWorks.orEmpty()
                val about = makerAbout
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 2.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── v407 — WHO THEY ARE FIRST, then what they made.
                    // The Met can only answer for the works it holds, so an
                    // artist or a painter the museum has nothing attributed
                    // to used to open an empty sheet that said so and stopped
                    // (user: "artist info wasnt loading at all, and many
                    // painter info wasnt loading too"). Their article's own
                    // words and their face are the answer now, and the works
                    // list follows when there is one.
                    about?.summary?.takeIf { it.isNotBlank() }?.let { words ->
                        item(key = "maker-about") {
                            Text(
                                words,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                                color = onSurfaceVariant
                            )
                        }
                    }
                    about?.pageUrl?.takeIf { it.isNotBlank() }?.let { recordUrl ->
                        item(key = "maker-record") {
                            Surface(
                                onClick = { openSearchUrl(context, recordUrl) },
                                shape = RoundedCornerShape(50),
                                color = accent.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                ) {
                                    CurioIcon(CurioIcons.Search, null, tint = ink, size = 15.dp)
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
                        }
                    }
                    if (listed.isEmpty()) {
                        item(key = "maker-none") {
                            Text(
                                if (makerWorks == null) "Looking them up\u2026"
                                else "The Met holds no attributed works for this name.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceVariant
                            )
                        }
                    } else {
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
    about: MakerInfo?,
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
        // v407 — their face when Wikipedia has one, the glyph when it does
        // not: a maker is a person, and a portrait says that better than an
        // icon does.
        Surface(shape = CircleShape, color = ink.copy(alpha = 0.16f)) {
            val portrait = about?.portraitUrl?.takeIf { it.isNotBlank() }
            if (portrait != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(portrait)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
            } else {
                CurioIcon(
                    CurioIcons.Person,
                    null,
                    tint = ink,
                    size = 20.dp,
                    modifier = Modifier.padding(9.dp)
                )
            }
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
    // v406 — THE CACHE IS A KEY, NOT A SNAPSHOT. Read once and used by both the
    // seed and the effect, so a URL another surface resolved after this card
    // composed is picked up instead of missed — keyed on the topic alone, this
    // card re-fetched art the app already had (member's report: "it loses its
    // fetched poster and it reloads after restart").
    val storedArt = AppPreferences.sheetArtUrlsState[artKey]?.takeIf { it.isNotBlank() }
    var artUrl by remember(topic.imageUrl, storedArt) {
        mutableStateOf(storedArt ?: topic.imageUrl?.takeIf { it.isNotBlank() })
    }
    LaunchedEffect(topic.imageUrl, fetchConsent, storedArt) {
        val stored = storedArt
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

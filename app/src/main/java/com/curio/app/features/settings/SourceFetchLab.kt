package com.curio.app.features.settings

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.BuildConfig
import com.curio.app.features.reveal.TmdbFetch
import com.curio.app.ui.components.CurioSettingsDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * v427 — THE SOURCE-FETCH LAB (Dev page).
 *
 * The member: *"add a api test fetching in dev settings"*, and, asked which
 * sources, *"Every content source"*. So this is a DEV PAGE SECTION (their choice)
 * that asks each of the app's content doors ONE question and shows what came
 * back: the status, how long it took, how many bytes, a one-line reading of the
 * payload, and the payload itself folded under it (their choice: *"Both, on one
 * screen"*).
 *
 * ── WHY ENDPOINTS, AND NOT THE APP'S OWN FUNCTIONS ────────────────────────────
 *
 * The app's fetchers RETURN PARSED TYPES — an album's cover URL, a list of
 * episodes — and throw away the payload they read it from, so a lab built on them
 * could say only "it worked" or "it did not", never WHAT the source actually
 * answered. This asks the source directly, with the SAME URL the app's own code
 * builds (every entry names the file it comes from, so a URL that drifts is
 * findable), so a bad key, a changed payload shape and a dead endpoint are three
 * different things on screen instead of one.
 *
 * ── THE THREE HONEST STATES ───────────────────────────────────────────────────
 *
 *  · **keyless** — the door is always open; the lab just asks it.
 *  · **keyed, no key in this build** — the row says so and does not fire (a 401
 *    would be a fact about the BUILD, not about the source; see `keyName`).
 *  · **keyed, key set** — the probe runs with the app's own `BuildConfig` value,
 *    so the lab tests the key this build would really use.
 *
 * A door may accept MORE THAN ONE credential (TMDB takes a v3 key or an API Read
 * Access Token — see [TmdbFetch]): the row is open when ANY of them is present,
 * and its message names them all, so the lab never reports a source as unusable
 * for a build the app itself authenticates with.
 *
 * Nothing here is persisted and nothing here runs on its own: a probe fires when
 * its own button (or Run all) is pressed, and its result lives in this screen's
 * own map until the page is left. It is a developer's bench, not a feature.
 */

// ── The probe ────────────────────────────────────────────────────────────────

/** What one probe came back with. [body] is empty for a [SourceFetchSource.binary] probe. */
internal class SourceFetchResult(
    val url: String,
    /** The HTTP code, or 0 when the request never completed. */
    val status: Int,
    val millis: Long,
    val bytes: Int,
    val contentType: String,
    val body: String,
    val summary: String
) {
    val ok: Boolean get() = status in 200..299
}

/**
 * One source, and the one question the lab asks it.
 *
 * [query] is FIXED and lives in the entry (the member's own choice: *"Fixed test
 * queries only"*), chosen so every one of these sources is known to answer it.
 */
internal class SourceFetchSource(
    val id: String,
    val label: String,
    val group: String,
    /** What this source answers, in one line, for the row's own subtitle. */
    val note: String,
    /** The file whose own code builds this URL — where drift would be found. */
    val where: String,
    /** The `BuildConfig` name this door needs, or null for a keyless source. */
    val keyName: String? = null,
    /**
     * What the "not set in this build" line should name, when one field is not
     * the whole story — TMDB takes either of its two credentials.
     */
    val keyLabel: String? = null,
    val probe: suspend () -> SourceFetchResult
)

/**
 * ONE REQUEST, THE WAY THE FETCHERS THEMSELVES MAKE IT.
 *
 * `HttpURLConnection`, a real User-Agent (MusicBrainz asks for one in so many
 * words, see `SongArtFetch`), timeouts, and the error stream read on a failure —
 * a 404's body is the only place that says WHY.
 */
private suspend fun probeRequest(
    url: String,
    method: String = "GET",
    body: String? = null,
    accept: String? = null,
    userAgent: String = "Curio/1.0 (source fetch lab)",
    headers: Map<String, String> = emptyMap(),
    readBody: Boolean = true
): SourceFetchResult = withContext(Dispatchers.IO) {
    val started = System.currentTimeMillis()
    fun mark(status: Int, bytes: Int, type: String, text: String, summary: String) =
        SourceFetchResult(url, status, System.currentTimeMillis() - started, bytes, type, text, summary)
    try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 12_000
        conn.readTimeout = 15_000
        conn.instanceFollowRedirects = true
        conn.requestMethod = method
        conn.setRequestProperty("User-Agent", userAgent)
        conn.setRequestProperty("Accept", accept ?: "application/json")
        headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { stream -> stream.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val status = conn.responseCode
        val type = conn.contentType.orEmpty().substringBefore(';')
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        // A BINARY SOURCE IS COUNTED, NOT READ: an image read as text is a page of
        // noise (and megabytes of it). The rest is kept whole — a payload a
        // developer cannot read is a payload they cannot check.
        val text = when {
            stream == null -> ""
            readBody -> stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            else -> {
                var total = 0
                val buffer = ByteArray(8 * 1024)
                stream.use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > 6 * 1024 * 1024) break
                    }
                }
                "\u0000$total"
            }
        }
        conn.disconnect()
        if (readBody) {
            mark(status, text.toByteArray(Charsets.UTF_8).size, type, text, describePayload(status, text))
        } else {
            val counted = text.removePrefix("\u0000").toIntOrNull() ?: 0
            mark(
                status = status,
                bytes = counted,
                type = type,
                text = "",
                summary = if (status in 200..299) {
                    "A ${type.ifBlank { "binary" }} answer, ${readableBytes(counted)}"
                } else {
                    "Refused — HTTP $status"
                }
            )
        }
    } catch (failure: Exception) {
        mark(0, 0, "", "", "No answer — ${failure.javaClass.simpleName}: ${failure.message.orEmpty()}")
    }
}

/** "1.2 MB" / "48 KB" / "912 B" — a size a developer reads at a glance. */
private fun readableBytes(bytes: Int): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}

/**
 * A ONE-LINE READING of a payload — what shape it is, how much of it there is, and
 * the first result's own fields. It is deliberately generic: a lab that hard-codes
 * "expect 12 results" is a lab that breaks the day a source answers 13, which is
 * precisely the day a developer needs it.
 */
internal fun describePayload(status: Int, body: String): String {
    val text = body.trim()
    if (status !in 200..299) {
        val hint = text.take(160).replace('\n', ' ').trim()
        return "Refused — HTTP $status${if (hint.isEmpty()) "" else " · $hint"}"
    }
    if (text.isEmpty()) return "Empty answer"
    return runCatching {
        when {
            text.startsWith("{") -> {
                val obj = JSONObject(text)
                obj.optJSONArray("errors")?.let { errors ->
                    val message = errors.optJSONObject(0)?.optString("message").orEmpty()
                    return@runCatching "ERRORS from the source — $message"
                }
                val counts = listOf("total", "totalResults", "count", "numFound", "nbHits", "totalHits", "result_count")
                    .mapNotNull { field -> obj.opt(field)?.takeIf { it != JSONObject.NULL }?.let { "$field $it" } }
                val array = (0 until obj.length())
                    .mapNotNull { index -> obj.names()?.optString(index)?.let { name -> name to obj.optJSONArray(name) } }
                    .firstOrNull { (_, value) -> value != null && value.length() > 0 }
                when {
                    array != null -> {
                        val (name, value) = array
                        val first = value!!.optJSONObject(0)
                        "$name × ${value.length()}${counts.firstOrNull()?.let { " · $it" }.orEmpty()}" +
                            "${first?.let { " · first: ${fieldsOf(it)}" }.orEmpty()}"
                    }
                    counts.isNotEmpty() -> "Answered · ${counts.joinToString(" · ")}"
                    else -> "Answered · ${fieldsOf(obj)}"
                }
            }
            text.startsWith("[") -> {
                val array = JSONArray(text)
                "Array × ${array.length()}" +
                    "${array.optJSONObject(0)?.let { " · first: ${fieldsOf(it)}" }.orEmpty()}"
            }
            text.startsWith("<") -> {
                val entries = Regex("<entry[ >]").findAll(text).count()
                val label = when {
                    text.contains("<feed") && entries > 0 -> "Atom feed · $entries entries"
                    text.contains("<html") -> "An HTML page, not an answer"
                    else -> "XML, ${text.length} chars"
                }
                label
            }
            else -> "Not JSON — ${text.length} chars"
        }
    }.getOrElse { failure ->
        "Unreadable payload — ${failure.javaClass.simpleName}"
    }
}

/** An object's own first few fields, named — what a row would be read out of. */
private fun fieldsOf(obj: JSONObject): String {
    val names = obj.names() ?: return ""
    return (0 until minOf(names.length(), 7))
        .mapNotNull { index -> names.optString(index).takeIf { it.isNotEmpty() } }
        .joinToString(", ")
}

// ── The doors ────────────────────────────────────────────────────────────────

/**
 * EVERY CONTENT SOURCE THE APP FETCHES FROM, and one fixed question each.
 *
 * The queries are the ones every one of these services is known to answer (a
 * famous painting, a famous album, a famous book, a famous series), so one tap is
 * one honest result rather than a search miss read as an outage.
 */
internal object SourceFetchCatalog {

    /**
     * v428 — TMDB's gate reads BOTH its credentials, because the app does.
     *
     * TMDB hands an account a v3 key and an API Read Access Token, and the app's
     * own [TmdbFetch] accepts either (the token as a Bearer header, the key as
     * `?api_key=`). A lab that only looked at the key would tell a member
     * carrying just the token that the source "needs TMDB_API_KEY" — while the
     * film sheets in the same build were happily resolving posters through it.
     * The value returned is whichever credential the app would actually send, so
     * a blank here means TMDB genuinely cannot answer.
     */
    private fun keyOf(name: String): String = when (name) {
        "TMDB_API_KEY" -> TmdbFetch.readToken.ifBlank { TmdbFetch.apiKey }
        "COMIC_VINE_API_KEY" -> BuildConfig.COMIC_VINE_API_KEY
        "GOOGLE_BOOKS_API_KEY" -> BuildConfig.GOOGLE_BOOKS_API_KEY
        "LIBRARY_THING_API_KEY" -> BuildConfig.LIBRARY_THING_API_KEY
        "SPOTIFY_CLIENT_ID" -> BuildConfig.SPOTIFY_CLIENT_ID
        "SPOTIFY_CLIENT_SECRET" -> BuildConfig.SPOTIFY_CLIENT_SECRET
        else -> ""
    }

    fun keyPresent(name: String?): Boolean =
        name == null || keyOf(name).isNotBlank()

    // NOT encoded here: the whole query goes through `Uri.encode` at the call
    // site, and encoding twice turns a space into `%2520` — a query MusicBrainz
    // answers with nothing, which a lab would report as the source being down.
    private fun quoted(value: String): String = "\"" + value + "\""

    private fun encoded(value: String): String = URLEncoder.encode(value, "UTF-8")

    val sources: List<SourceFetchSource> by lazy {
        listOf(
            // ── Artwork ─────────────────────────────────────────────────────
            SourceFetchSource(
                id = "met",
                label = "The Met",
                group = "Artwork",
                note = "The museum's own catalog: a work's record, its picture and its credit line",
                where = "features/reveal/ArtworkSheet.kt",
                probe = {
                    probeRequest(
                        "https://collectionapi.metmuseum.org/public/collection/v1/search" +
                            "?hasImages=true&q=" + Uri.encode("Starry Night")
                    )
                }
            ),
            SourceFetchSource(
                id = "cleveland",
                label = "Cleveland Museum of Art",
                group = "Artwork",
                note = "The second artwork door, and the maker's own biography",
                where = "features/reveal/MuseumFetch.kt",
                probe = {
                    probeRequest(
                        "https://openaccess-api.clevelandart.org/api/artworks/" +
                            "?q=" + Uri.encode("van gogh") + "&limit=3&has_image=1" +
                            "&fields=id,title,creation_date,technique,creators,images,url"
                    )
                }
            ),
            SourceFetchSource(
                id = "wikipedia",
                label = "Wikipedia",
                group = "Artwork",
                note = "A maker's and a work's own summary, and the article a page links to",
                where = "features/reveal/ArtworkSheet.kt",
                probe = {
                    probeRequest(
                        "https://en.wikipedia.org/api/rest_v1/page/summary/Vincent_van_Gogh",
                        accept = "application/json"
                    )
                }
            ),

            // ── Music ───────────────────────────────────────────────────────
            SourceFetchSource(
                id = "itunes",
                label = "iTunes / Apple Music",
                group = "Music",
                note = "Album and song artwork, and the one-line description",
                where = "features/reveal/AlbumArtFetch.kt · SongArtFetch.kt",
                probe = {
                    probeRequest(
                        "https://itunes.apple.com/search?term=" + Uri.encode("Random Access Memories") +
                            "&entity=album&limit=3"
                    )
                }
            ),
            SourceFetchSource(
                id = "musicbrainz",
                label = "MusicBrainz",
                group = "Music",
                note = "The release group the Cover Art Archive is then asked about",
                where = "features/reveal/AlbumArtFetch.kt",
                probe = {
                    probeRequest(
                        "https://musicbrainz.org/ws/2/release-group/?query=" +
                            Uri.encode("releasegroup:${quoted("Random Access Memories")} AND artist:${quoted("Daft Punk")}") +
                            "&fmt=json&limit=5",
                        userAgent = "CurioApp/1.0 (album art lookup)"
                    )
                }
            ),
            SourceFetchSource(
                id = "coverart",
                label = "Cover Art Archive",
                group = "Music",
                note = "The album cover itself — a second step, with MusicBrainz's own id",
                where = "features/reveal/AlbumArtFetch.kt",
                probe = {
                    val search = probeRequest(
                        "https://musicbrainz.org/ws/2/release-group/?query=" +
                            Uri.encode("releasegroup:${quoted("Random Access Memories")} AND artist:${quoted("Daft Punk")}") +
                            "&fmt=json&limit=5",
                        userAgent = "CurioApp/1.0 (album art lookup)"
                    )
                    val groupId = runCatching {
                        JSONObject(search.body).optJSONArray("release-groups")
                            ?.optJSONObject(0)?.optString("id")
                    }.getOrNull()
                    if (groupId.isNullOrBlank()) {
                        SourceFetchResult(
                            url = search.url,
                            status = search.status,
                            millis = search.millis,
                            bytes = search.bytes,
                            contentType = search.contentType,
                            body = search.body,
                            summary = "No release group to ask about — MusicBrainz answered " +
                                "${search.status}: ${search.summary}"
                        )
                    } else {
                        probeRequest(
                            "https://coverartarchive.org/release-group/$groupId/front-500",
                            accept = "image/*",
                            readBody = false
                        )
                    }
                }
            ),
            SourceFetchSource(
                id = "spotify",
                label = "Spotify",
                group = "Music",
                note = "The deep link to the album or track the topic names",
                where = "data/ExploreSearch.kt",
                keyName = "SPOTIFY_CLIENT_ID",
                probe = {
                    val token = spotifyToken()
                    if (token == null) {
                        SourceFetchResult(
                            url = "https://accounts.spotify.com/api/token",
                            status = 0,
                            millis = 0,
                            bytes = 0,
                            contentType = "",
                            body = "",
                            summary = "No token — the client id/secret pair was refused"
                        )
                    } else {
                        probeRequest(
                            "https://api.spotify.com/v1/search?q=" + encoded("Random Access Memories") +
                                "&type=album&limit=1",
                            headers = mapOf("Authorization" to "Bearer $token")
                        )
                    }
                }
            ),

            // ── Books ───────────────────────────────────────────────────────
            SourceFetchSource(
                id = "openlibrary",
                label = "Open Library",
                group = "Books",
                note = "A book's catalog record: editions, length, subjects",
                where = "features/personal/BookEnrichment.kt · features/settings/BookCoverFetch.kt",
                probe = {
                    probeRequest(
                        "https://openlibrary.org/search.json?title=" + Uri.encode("Dune") + "&limit=3"
                    )
                }
            ),
            SourceFetchSource(
                id = "openlibrary-covers",
                label = "Open Library covers",
                group = "Books",
                note = "The cover by title — the keyless fallback every book has",
                where = "features/settings/BookCoverFetch.kt",
                probe = {
                    probeRequest(
                        "https://covers.openlibrary.org/b/title/" + Uri.encode("Dune") + "-M.jpg",
                        accept = "image/*",
                        readBody = false
                    )
                }
            ),
            SourceFetchSource(
                id = "google-books",
                label = "Google Books",
                group = "Books",
                note = "A volume's own description and page count",
                where = "features/personal/BookEnrichment.kt",
                keyName = null,
                probe = {
                    val key = BuildConfig.GOOGLE_BOOKS_API_KEY
                    probeRequest(
                        "https://www.googleapis.com/books/v1/volumes?q=" +
                            Uri.encode("intitle:Dune") + "&maxResults=3" +
                            if (key.isNotBlank()) "&key=$key" else ""
                    )
                }
            ),
            SourceFetchSource(
                id = "librarything",
                label = "LibraryThing",
                group = "Books",
                note = "A cover by ISBN, for a book the shops do not draw",
                where = "features/settings/BookCoverFetch.kt",
                keyName = "LIBRARY_THING_API_KEY",
                probe = {
                    probeRequest(
                        "https://covers.librarything.com/devkey/${BuildConfig.LIBRARY_THING_API_KEY}" +
                            "/large/isbn/9780441013593",
                        accept = "image/*",
                        readBody = false
                    )
                }
            ),
            SourceFetchSource(
                id = "standard-ebooks",
                label = "Standard Ebooks",
                group = "Books",
                note = "A classic's own cover, blurb and edition, as an Atom feed",
                where = "features/personal/StandardEbooksFetch.kt",
                probe = {
                    probeRequest(
                        "https://standardebooks.org/feeds/opds/all?query=" +
                            Uri.encode("pride and prejudice") + "&per-page=5",
                        accept = "application/atom+xml"
                    )
                }
            ),
            SourceFetchSource(
                id = "comic-vine",
                label = "Comic Vine",
                group = "Books",
                note = "A comic volume's cover, publisher, year and issue count",
                where = "features/personal/ComicVineFetch.kt",
                keyName = "COMIC_VINE_API_KEY",
                probe = {
                    probeRequest(
                        "https://comicvine.gamespot.com/api/search/" +
                            "?api_key=${BuildConfig.COMIC_VINE_API_KEY}" +
                            "&format=json&resources=volume&limit=3" +
                            "&query=" + Uri.encode("Saga") +
                            "&field_list=id,name,deck,image,publisher,start_year,count_of_issues"
                    )
                }
            ),
            SourceFetchSource(
                id = "crossref",
                label = "Crossref",
                group = "Books",
                note = "A book's chapter list and page ranges, from the DOI registry",
                where = "features/personal/BookEnrichment.kt",
                probe = {
                    probeRequest(
                        "https://api.crossref.org/works?rows=3&filter=type:book" +
                            "&select=title,container-title" +
                            "&query.bibliographic=" + encoded("Frankenstein Mary Shelley")
                    )
                }
            ),

            // ── Screen ─────────────────────────────────────────────────────
            SourceFetchSource(
                id = "tvmaze",
                label = "TVmaze",
                group = "Screen",
                note = "A series' own episode guide, keyless",
                where = "features/reveal/SeriesEpisodeFetcher.kt · SeriesPosterFetch.kt",
                probe = {
                    probeRequest(
                        "https://api.tvmaze.com/singlesearch/shows?q=" + Uri.encode("Breaking Bad")
                    )
                }
            ),
            SourceFetchSource(
                id = "tmdb",
                label = "TMDB",
                group = "Screen",
                note = "A film's or a series' own poster, year, runtime and episode guide",
                where = "features/reveal/TmdbFetch.kt",
                keyName = "TMDB_API_KEY",
                keyLabel = "TMDB_API_KEY or TMDB_READ_TOKEN",
                probe = {
                    // The token travels as a header and the key as a query
                    // parameter, exactly as [TmdbFetch] sends them — see its own
                    // note on TMDB's two credentials. Never both, and never a
                    // bare `api_key=`.
                    val query = "https://api.themoviedb.org/3/search/movie?query=" + encoded("Inception")
                    val token = TmdbFetch.readToken
                    if (token.isNotBlank()) {
                        probeRequest(query, headers = mapOf("Authorization" to "Bearer $token"))
                    } else {
                        probeRequest(query + "&api_key=" + TmdbFetch.apiKey)
                    }
                }
            ),

            // ── Anime & manga ───────────────────────────────────────────────
            SourceFetchSource(
                id = "anilist",
                label = "AniList",
                group = "Anime & manga",
                note = "A manga's cover, length and creator (GraphQL, POST)",
                where = "features/personal/MangaFetch.kt",
                probe = {
                    val graph = "query (\$q: String) { Page(perPage: 3) { media(search: \$q, type: MANGA, " +
                        "sort: SEARCH_MATCH) { title { romaji english } coverImage { large } " +
                        "description(asHtml: false) volumes chapters " +
                        "staff(perPage: 1) { nodes { name { full } } } } } }"
                    val body = JSONObject()
                        .put("query", graph)
                        .put("variables", JSONObject().put("q", "Berserk"))
                        .toString()
                    probeRequest("https://graphql.anilist.co", method = "POST", body = body)
                }
            ),
            SourceFetchSource(
                id = "jikan",
                label = "Jikan (MyAnimeList)",
                group = "Anime & manga",
                note = "An anime's own records, and the manga fallback",
                where = "features/reveal/AnimePosterFetch.kt · AnimeEpisodeFetcher.kt · MangaFetch.kt",
                probe = {
                    probeRequest(
                        "https://api.jikan.moe/v4/anime?q=" + Uri.encode("Frieren") + "&limit=3"
                    )
                }
            ),
            SourceFetchSource(
                id = "kitsu",
                label = "Kitsu",
                group = "Anime & manga",
                note = "A manga's cover and length, the keyless second door",
                where = "features/personal/MangaFetch.kt",
                probe = {
                    probeRequest(
                        "https://kitsu.io/api/edge/manga?page[limit]=3&filter[text]=" + encoded("Berserk"),
                        accept = "application/vnd.api+json"
                    )
                }
            ),
            SourceFetchSource(
                id = "mangadex",
                label = "MangaDex",
                group = "Anime & manga",
                note = "A manga's cover art, author and status",
                where = "features/personal/MangaFetch.kt",
                probe = {
                    probeRequest(
                        "https://api.mangadex.org/manga?limit=3&order[relevance]=desc" +
                            "&includes[]=cover_art&includes[]=author&title=" + Uri.encode("Berserk")
                    )
                }
            )
        )
    }

    /** Spotify's client-credentials token — the same flow `ExploreSearch` runs. */
    private suspend fun spotifyToken(): String? = withContext(Dispatchers.IO) {
        val id = BuildConfig.SPOTIFY_CLIENT_ID
        val secret = BuildConfig.SPOTIFY_CLIENT_SECRET
        if (id.isBlank() || secret.isBlank()) return@withContext null
        runCatching {
            val conn = URL("https://accounts.spotify.com/api/token").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 12_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("User-Agent", "Curio/1.0 (source fetch lab)")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty(
                "Authorization",
                "Basic " + android.util.Base64.encodeToString(
                    "$id:$secret".toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )
            )
            conn.outputStream.use { stream ->
                stream.write("grant_type=client_credentials".toByteArray(Charsets.UTF_8))
            }
            val status = conn.responseCode
            val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (status !in 200..299) return@runCatching null
            JSONObject(text).optString("access_token").takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

// ── The section ──────────────────────────────────────────────────────────────

/**
 * THE DEV PAGE'S OWN SECTION — a heading, then the lab.
 *
 * State is this composition's own: which probes are running, and what each one
 * last answered. Leaving the page clears it, which is right for a bench (a stale
 * green from half an hour ago is worse than no answer at all).
 */
@Composable
internal fun SourceFetchLabSection() {
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateMapOf<String, SourceFetchResult>() }
    val running = remember { mutableStateMapOf<String, Boolean>() }
    var openBody by remember { mutableStateOf<String?>(null) }
    var sweeping by remember { mutableStateOf(false) }

    val sources = SourceFetchCatalog.sources

    fun run(one: SourceFetchSource) {
        if (running[one.id] == true) return
        running[one.id] = true
        scope.launch {
            val result = runCatching { one.probe() }.getOrElse { failure ->
                SourceFetchResult(
                    url = one.where,
                    status = 0,
                    millis = 0,
                    bytes = 0,
                    contentType = "",
                    body = "",
                    summary = "The probe itself failed — ${failure.javaClass.simpleName}"
                )
            }
            results[one.id] = result
            running[one.id] = false
        }
    }

    SettingsSectionHeading("Source fetch")
    SettingsOptionCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "One fixed question per content source, with the raw answer under it. " +
                    "Ask one, or ask them all — the results live on this page only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabButton(
                    label = if (sweeping) "Asking…" else "Run all",
                    enabled = !sweeping
                ) {
                    sweeping = true
                    scope.launch {
                        // SEQUENTIAL, AND PACED: MusicBrainz asks for one request a
                        // second, and a lab that gets its own key rate-limited is a
                        // lab that lied about the source.
                        sources.forEach { source ->
                            if (!SourceFetchCatalog.keyPresent(source.keyName)) return@forEach
                            running[source.id] = true
                            results[source.id] = runCatching { source.probe() }.getOrElse { failure ->
                                SourceFetchResult(
                                    source.where, 0, 0, 0, "", "",
                                    "The probe itself failed — ${failure.javaClass.simpleName}"
                                )
                            }
                            running[source.id] = false
                            delay(400)
                        }
                        sweeping = false
                    }
                }
                val answered = results.values.count { it.ok }
                Text(
                    if (results.isEmpty()) "Nothing asked yet"
                    else "$answered of ${results.size} answered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            var lastGroup = ""
            sources.forEach { source ->
                if (source.group != lastGroup) {
                    lastGroup = source.group
                    Text(
                        source.group.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                    )
                }
                val result = results[source.id]
                val busy = running[source.id] == true
                val hasKey = SourceFetchCatalog.keyPresent(source.keyName)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 1.6.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                LabDot(result)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                source.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                if (hasKey) source.note
                                else "Needs ${source.keyLabel ?: source.keyName} — not set in this build",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LabButton(
                            label = if (result == null) "Ask" else "Again",
                            enabled = !busy && hasKey
                        ) { run(source) }
                    }
                    if (result != null) {
                        Text(
                            text = buildString {
                                append(if (result.status == 0) "no answer" else "HTTP ${result.status}")
                                append(" · ${result.millis} ms")
                                append(" · ${readableBytes(result.bytes)}")
                                if (result.contentType.isNotBlank()) append(" · ${result.contentType}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (result.ok) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            result.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (result.body.isNotBlank()) {
                            val open = openBody == source.id
                            LabButton(label = if (open) "Hide the answer" else "Show the answer") {
                                openBody = if (open) null else source.id
                            }
                            AnimatedVisibility(visible = open) {
                                Text(
                                    text = result.body.take(20_000) +
                                        if (result.body.length > 20_000) "\n… truncated (" +
                                            readableBytes(result.bytes) + ")" else "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 260.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                    CurioSettingsDivider()
                }
            }
        }
    }
}

/** A three-state mark: an answer, a refusal, or nothing asked yet. */
@Composable
private fun LabDot(result: SourceFetchResult?) {
    val color = when {
        result == null -> MaterialTheme.colorScheme.outlineVariant
        result.ok -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

/**
 * One word of the lab — a filled chip in the page's own language, so a bench still
 * reads as the app it lives in rather than as a debug console.
 */
@Composable
private fun LabButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        // The fill FIRST and the finger on top of it: a press tint painted under
        // the background is a press nobody can see (the order is the whole rung).
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                    alpha = if (enabled) 1f else 0.5f
                ),
                RoundedCornerShape(50)
            )
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    )
}

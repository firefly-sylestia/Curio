package com.curio.app.features.reveal

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * v426b — THE CLEVELAND MUSEUM OF ART, THE MUSEUM WITH NO KEY AT ALL.
 *
 * The artworks and makers lanes had exactly two doors: the Met (attributed, but
 * it only knows what the Met holds) and Wikipedia (the prose). A painting the Met
 * does not have therefore fell through to an encyclopedia article about the
 * *painting* — which for an artist's own works list meant nothing at all, since
 * Wikipedia has no list of what hangs where.
 *
 * The Cleveland Museum of Art publishes its whole collection as open access with
 * **no key, no account, no registration** — text and pictures both, under CC0 for
 * the images it owns — which makes it the one addition that costs a member
 * nothing to configure. It answers with a work's own title, its date, its
 * technique, its museum page, its web-sized picture, and — the part no other
 * source here has — **a real biographical paragraph about the maker**, written by
 * the museum, which is what gives an artist or painter page words when Wikipedia
 * has no article for them.
 *
 * ── WHAT WAS VERIFIED BEFORE THIS FILE EXISTED ──────────────────────────
 *
 *  - `openaccess-api.clevelandart.org/api/artworks/?q=…&has_image=1&fields=…`
 *    answers `{ info, data: [ … ] }`, each row carrying `title`,
 *    `creation_date`, `technique`, `url`, `creators[]` (with `description` and
 *    `biography`) and `images.web.url`.
 *  - Its picture host serves real bytes: a 723×900 `_web.jpg` came back 200,
 *    `image/jpeg`, 232 KB — so a row never paints a broken plate.
 *  - **Its `artist=` filter does NOT filter** (asked for Claude Monet, answered
 *    with Copley, Bellows and Eakins), so every maker lookup here filters the
 *    results ITSELF, by the creator a row actually carries. A door that trusted
 *    that parameter would attribute one painter's works to another.
 *
 * ── AND THE ONE THAT IS NOT HERE ───────────────────────────────────────
 *
 * The Art Institute of Chicago's API is keyless and its catalogue is wonderful,
 * so it was tried first: its `iiif_url` images answer **403 from a client that
 * is not a browser** (verified against the same id the API itself returned,
 * 843px and 400px alike). A source whose pictures cannot be fetched would put a
 * broken plate on every work it won — worse than not asking it — so it is not
 * wired. See the museum note in `Prompt.md`.
 */
internal object MuseumFetch {

    private const val CLEVELAND = "https://openaccess-api.clevelandart.org/api/artworks/"

    /** How many rows a search is allowed to read (a sheet is not a catalogue). */
    private const val SEARCH_ROWS = 8

    private val cache = ConcurrentHashMap<String, List<ClevelandWork>>()
    private val bioCache = ConcurrentHashMap<String, String>()

    /** One work as Cleveland states it, in the few fields this app draws. */
    internal data class ClevelandWork(
        val title: String,
        val maker: String,
        val date: String,
        val medium: String,
        /** The museum's own picture at a size a phone should hold. */
        val imageUrl: String,
        /** The work's page at the museum. */
        val pageUrl: String
    )

    /**
     * The work named [title] (by [artist] when the caller has one), or null when
     * Cleveland does not hold it.
     *
     * The search is generous — it is a gallery's own full-text search — so the
     * row that answers is the one whose OWN title matches (normalised), with a
     * creator match breaking a tie. Asking for "Water Lilies" and answering with
     * a work called something else is how a sheet ends up describing the wrong
     * painting.
     */
    internal suspend fun work(title: String, artist: String): ClevelandWork? =
        withContext(Dispatchers.IO) {
            val wanted = title.trim()
            if (wanted.isBlank()) return@withContext null
            val query = listOf(wanted, artist.trim()).filter { it.isNotBlank() }.joinToString(" ")
            val rows = search(query, wanted)
            rows.firstOrNull { row ->
                normalise(row.title) == normalise(wanted) &&
                    (artist.isBlank() || makerMatches(row.maker, artist))
            } ?: rows.firstOrNull { normalise(row.title) == normalise(wanted) }
        }

    /**
     * THE MAKER'S OWN WORKS, as the museum holds them: rows whose CREATOR is this
     * person, filtered here because the API's `artist` parameter does not do it
     * (see this file's note). A row with no picture is skipped — a works list is
     * a wall of pictures.
     */
    internal suspend fun worksBy(maker: String, limit: Int): List<AuthorWork> =
        withContext(Dispatchers.IO) {
            val name = maker.trim()
            if (name.isBlank() || limit <= 0) return@withContext emptyList()
            search(name, firstTitle = null)
                .filter { row -> makerMatches(row.maker, name) && row.imageUrl.isNotBlank() }
                .distinctBy { normalise(it.title) }
                .take(limit)
                .map { row ->
                    AuthorWork(
                        key = row.pageUrl.ifBlank { row.title },
                        title = row.title,
                        year = row.date.filter { it.isDigit() }.take(4),
                        coverUrl = row.imageUrl
                    )
                }
        }

    /**
     * The maker's own biography as the MUSEUM wrote it — the prose that fills an
     * artist or painter page Wikipedia has no article for. Null when nothing
     * matches or the museum has no text for them.
     */
    internal suspend fun makerBio(maker: String): String? = withContext(Dispatchers.IO) {
        val name = maker.trim()
        if (name.isBlank()) return@withContext null
        bioCache[name]?.let { return@withContext it.ifEmpty { null } }
        val text = runCatching {
            val body = getJson(
                "$CLEVELAND?q=${Uri.encode(name)}&limit=$SEARCH_ROWS&has_image=1" +
                    "&fields=title,creators"
            ) ?: return@runCatching ""
            rows(body).firstOrNull { row -> makerMatches(row.maker, name) && row.bio.isNotBlank() }
                ?.bio
                .orEmpty()
        }.getOrDefault("")
        bioCache[name] = text
        text.takeIf { it.isNotBlank() }
    }

    // ── The door itself ────────────────────────────────────────────────────

    /**
     * One search, read into rows. [firstTitle] (when given) keeps only rows whose
     * own title relates to it, which is what a work lookup wants and a maker
     * lookup does not (a painter's list is whatever the museum holds, and their
     * name is in the query).
     */
    private fun search(query: String, firstTitle: String?): List<ClevelandWork> {
        val key = "$query|${firstTitle.orEmpty()}"
        cache[key]?.let { return it }
        val rows = runCatching {
            val body = getJson(
                "$CLEVELAND?q=${Uri.encode(query)}&limit=$SEARCH_ROWS&has_image=1" +
                    "&fields=id,title,creation_date,technique,creators,images,url"
            ) ?: return emptyList()
            rows(body)
                .filter { row -> firstTitle == null || titleRelates(row.title, firstTitle) }
        }.getOrDefault(emptyList())
        cache[key] = rows
        return rows
    }

    /** Cleveland's `data` array as this app's own rows (no picture = no row). */
    private fun rows(body: String): List<Row> = runCatching {
        val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { index ->
            val row = data.optJSONObject(index) ?: return@mapNotNull null
            val title = row.optString("title").trim()
            if (title.isBlank()) return@mapNotNull null
            Row(
                title = title,
                maker = firstCreator(row.optJSONArray("creators"))?.optString("description").orEmpty(),
                bio = firstCreator(row.optJSONArray("creators"))?.optString("biography").orEmpty(),
                date = row.optString("creation_date").trim(),
                medium = row.optString("technique").trim(),
                imageUrl = row.optJSONObject("images")
                    ?.optJSONObject("web")
                    ?.optString("url")
                    .orEmpty(),
                pageUrl = row.optString("url").trim()
            )
        }
    }.getOrDefault(emptyList())

    /** THE ARTIST'S CREATOR — the one whose role is `artist`, else the first. */
    private fun firstCreator(creators: JSONArray?): JSONObject? {
        if (creators == null || creators.length() == 0) return null
        val all = (0 until creators.length()).mapNotNull { creators.optJSONObject(it) }
        return all.firstOrNull { it.optString("role").equals("artist", ignoreCase = true) }
            ?: all.firstOrNull()
    }

    /**
     * Whether a row's creator line is this person. Cleveland writes a creator as
     * "Claude Monet (French, 1840–1926)", so the name is what comes BEFORE the
     * bracket — comparing the whole line would never match a bare name.
     */
    private fun makerMatches(creatorLine: String, maker: String): Boolean {
        val name = creatorLine.substringBefore('(').trim()
        if (name.isBlank()) return false
        val wanted = maker.trim()
        return name.equals(wanted, ignoreCase = true) ||
            name.contains(wanted, ignoreCase = true) ||
            wanted.contains(name, ignoreCase = true)
    }

    /** A title with case, punctuation and spacing taken off. */
    private fun normalise(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** True when a row's title really is the one being looked for (near enough). */
    private fun titleRelates(rowTitle: String, wanted: String): Boolean {
        val a = normalise(rowTitle)
        val b = normalise(wanted)
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true
        val longer = maxOf(a.length, b.length)
        val shorter = minOf(a.length, b.length)
        return shorter * 10 >= longer * 7 && (a.contains(b) || b.contains(a))
    }

    /** One Cleveland row, flattened (the raw creator objects are not kept). */
    private data class Row(
        val title: String,
        val maker: String,
        val bio: String,
        val date: String,
        val medium: String,
        val imageUrl: String,
        val pageUrl: String
    )

    /** Minimal keyless GET — 8s timeout, best-effort, like every other door. */
    private fun getJson(urlString: String): String? = runCatching {
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

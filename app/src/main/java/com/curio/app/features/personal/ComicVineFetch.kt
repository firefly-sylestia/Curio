package com.curio.app.features.personal

import com.curio.app.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * v426 — THE WESTERN COMICS DOOR (Comic Vine, keyed).
 *
 * A manga has four keyless catalogues to answer for it ([MangaFetch]'s cascade).
 * A WESTERN COMIC has none: AniList, MangaDex and the rest catalogue Japanese
 * and Korean series, so a volume of *Watchmen*, *Saga* or *The Sandman* comes
 * back empty from every one of them. Comic Vine is the database that actually
 * holds them — Marvel's and DC's whole lines, plus the independents — with each
 * volume's own cover, publisher, start year, issue count and credits.
 *
 * ── MARVEL'S OWN API IS NOT THE DOOR, AND THAT IS WHY THIS ONE IS ────────
 *
 * The member asked for "comic vine (needs a free key), marvel dc can be done
 * for the basic series one …". Marvel's developer API was **discontinued**
 * (announced in 2025; by 2026 its keys answer nothing at all), and DC has never
 * published a public one — so there are no publisher keys to add, and a Marvel
 * key here would have been a door onto a closed room. Comic Vine is both
 * publishers' door (`publisher` on every volume says which), and it is a single
 * key rather than a key plus a SECRET: Marvel's scheme needed a public key, a
 * private key AND an md5 of both, and a private key shipped inside an APK is not
 * private at all. One key it is.
 *
 * ── THE KEY IS OPTIONAL AND NOTHING IS ASKED WITHOUT IT ──────────────────
 *
 * [available] is false on a build with no key, and every entry point answers
 * null immediately in that case, so the keyless cascade carries on exactly as it
 * did before this file existed. See [com.curio.app.BuildConfig.COMIC_VINE_API_KEY].
 *
 * ── THE FREE KEY HAS RULES, AND THEY ARE RESPECTED ───────────────────────
 *
 * Comic Vine's free key is non-commercial, allows **1 request per second** and
 * **200 requests per resource per hour**, and warns that bursts are velocity
 * detected. So: this object is asked at most ONCE per search and ONCE per shelf
 * row, the request is throttled to a second apart ([pace]), and its answers are
 * cached by the shelves that hold them (a row's cover is stored on the row —
 * see [BookEnrichment]) rather than re-asked on every open.
 */
internal object ComicVineFetch {

    /** The API host. Both entry points live under it. */
    private const val HOST = "https://comicvine.gamespot.com/api"

    /**
     * What one volume is, in this app's own words. Comic Vine answers with far
     * more (character, creator, story-arc and issue credits); a shelf row needs
     * the six things a member can see, and asking for less keeps the payload —
     * and the free key's hourly allowance — small.
     */
    internal data class Volume(
        val title: String,
        val publisher: String,
        val year: Int,
        /** How many issues the volume runs to (0 = the source said nothing). */
        val issues: Int,
        val description: String,
        val coverUrl: String,
        /** The people Comic Vine credits on the volume, best-effort. */
        val credits: String
    )

    /**
     * Whether this build carries a key at all. False sends every caller down the
     * keyless road without a request being made, which is the contract the whole
     * feature rests on (an APK with no Comic Vine secret must behave exactly as
     * it did before).
     */
    internal val available: Boolean
        get() = runCatching { BuildConfig.COMIC_VINE_API_KEY.isNotBlank() }.getOrDefault(false)

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /** When the last request went out, for the 1/second rule. */
    private var lastAsked = 0L

    /**
     * Search Comic Vine's volumes for [query].
     *
     * Returns the volumes found, an EMPTY list when the source answered with
     * nothing, and NULL when it could not be asked at all (no key, a dead line,
     * an exhausted allowance) — the same three-way contract the keyless cascade
     * keeps, so a caller can tell "no such volume" from "I could not look".
     */
    internal fun search(query: String, limit: Int = 8): List<Volume>? {
        if (!available || query.isBlank()) return null
        val url = "$HOST/search/" +
            "?api_key=${BuildConfig.COMIC_VINE_API_KEY}" +
            "&format=json" +
            "&resources=volume" +
            "&limit=$limit" +
            "&query=${encode(query)}" +
            // Only what a shelf row wears. The default payload of one volume is
            // tens of kilobytes of credits this app never draws.
            "&field_list=id,name,deck,description,image,publisher,start_year," +
            "count_of_issues,person_credits"
        val payload = get(url) ?: return null
        if (payload.status() != 1) return null
        val results = payload.getAsJsonArray("results") ?: return emptyList()
        return results.mapNotNull { element -> element.asJsonObject.volume() }
    }

    /**
     * One FILM or SHOW, as Comic Vine files it.
     *
     * v429 — the second resource this object asks, added for the member's own
     * request (*"i added comicvine api too mybe use that for incursion"*). Comic
     * Vine's film catalogue is a real one — a comic adaptation's entry is often
     * longer and more precise than a general database's — so it is the last net
     * under an Incursion row's description and artwork, the door reached when
     * TMDB, OMDb and Wikipedia have all been empty.
     *
     * It is a SEARCH, not a detail read: a row knows a title, not Comic Vine's
     * own id, so the best name match wins — the same shape [search] uses for a
     * volume. Null means "could not be asked" (no key, dead line, exhausted
     * allowance) and an empty title means asked and nothing there.
     */
    internal fun movie(query: String): Movie? {
        val wanted = query.trim()
        if (!available || wanted.isBlank()) return null
        val url = "$HOST/search/" +
            "?api_key=${BuildConfig.COMIC_VINE_API_KEY}" +
            "&format=json" +
            "&resources=movie" +
            "&limit=5" +
            "&query=${encode(wanted)}" +
            "&field_list=id,name,deck,description,image,release_date"
        val payload = get(url) ?: return null
        if (payload.status() != 1) return null
        val results = payload.getAsJsonArray("results") ?: return null
        val rows = results.mapNotNull { element -> element.asJsonObject.movie() }
        return rows.firstOrNull { it.title.equals(wanted, ignoreCase = true) }
            ?: rows.firstOrNull()
    }

    /** A film or a show, in this app's own words. */
    internal data class Movie(
        val title: String,
        val year: Int,
        val description: String,
        val coverUrl: String
    )

    // ── The shared plumbing ────────────────────────────────────────────────

    /**
     * One GET, parsed — null for a dead line or an unparseable body.
     *
     * A non-2xx status is NOT treated as a failure on its own: this API answers
     * an invalid key with **HTTP 401 and a perfectly good JSON body** whose
     * `status_code` is 100, and a rate-limited call with 420. The body decides
     * (see [status]); reading the HTTP code instead would call every one of those
     * "unreachable" and hide the reason.
     */
    private fun get(url: String): JsonObject? = runCatching {
        pace()
        http.newCall(Request.Builder().url(url).header("User-Agent", "Curio/1.0").get().build())
            .execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) null else JsonParser.parseString(body) as? JsonObject
            }
    }.getOrNull()

    /**
     * The 1/second rule. Comic Vine asks for it outright ("all API requests be
     * throttled so that they come in no faster than once per second"), and a
     * burst is velocity-detected — so two calls that follow each other inside the
     * same second are spaced out. Callers reach this from a background
     * dispatcher, and a row asks at most once, so the wait is bounded by design
     * (a single second, never a queue).
     */
    private fun pace() {
        val since = System.currentTimeMillis() - lastAsked
        if (lastAsked != 0L && since in 0L until PACE_MS) {
            runCatching { Thread.sleep(PACE_MS - since) }
        }
        lastAsked = System.currentTimeMillis()
    }

    /** 1 = OK. Anything else (100 invalid key, 101 not found, 420 throttled) is a no. */
    private fun JsonObject.status(): Int =
        get("status_code")?.takeIf { !it.isJsonNull }?.asInt ?: 0

    /** One search result (or one volume) as this app's own [Volume]. */
    private fun JsonObject.volume(): Volume? {
        val name = string("name")?.trim().orEmpty()
        if (name.isBlank()) return null
        // `deck` is Comic Vine's own one-line summary and `description` is the-
        // long one; the deck reads better in a sheet, and the description is the
        // fallback (it is written in a wiki's voice and can run long, which is
        // why [plainText] cuts it).
        val description = plainText(string("deck")?.takeIf { it.isNotBlank() }
            ?: string("description"))
        return Volume(
            title = name,
            publisher = getAsJsonObject("publisher")?.string("name").orEmpty(),
            year = string("start_year")?.filter { it.isDigit() }?.take(4)?.toIntOrNull() ?: 0,
            issues = string("count_of_issues")?.toIntOrNull() ?: 0,
            description = description,
            coverUrl = coverUrl().orEmpty(),
            credits = credits()
        )
    }

    /** One film search result as this app's own [Movie]. */
    private fun JsonObject.movie(): Movie? {
        val name = string("name")?.trim().orEmpty()
        if (name.isBlank()) return null
        return Movie(
            title = name,
            year = string("release_date")?.take(4)?.filter { it.isDigit() }?.toIntOrNull() ?: 0,
            description = plainText(string("deck")?.takeIf { it.isNotBlank() }
                ?: string("description")),
            coverUrl = coverUrl().orEmpty()
        )
    }

    /**
     * A volume's cover. Comic Vine's `image` is an OBJECT of named sizes, and
     * the sizes are the API's own (`medium_url` around 400px wide,
     * `super_url` around 1000): medium is the right one for a shelf card — big
     * enough to read, small enough not to cost a member's data on a list.
     */
    private fun JsonObject.coverUrl(): String? {
        val image = getAsJsonObject("image") ?: return null
        val url = image.string("medium_url")?.takeIf { it.isNotBlank() }
            ?: image.string("small_url")?.takeIf { it.isNotBlank() }
            ?: image.string("original_url")
        return url?.replace("http://", "https://")
    }

    /** The first few credited people, as one line ("Alan Moore, Dave Gibbons"). */
    private fun JsonObject.credits(): String {
        val people = getAsJsonArray("person_credits") ?: return ""
        return people.mapNotNull { person ->
            person.asJsonObject.string("name")?.trim()?.takeIf { it.isNotBlank() }
        }.distinct().take(3).joinToString(", ")
    }

    /** A description with its wiki markup and its length taken off. */
    private fun plainText(value: String?): String {
        val text = value?.takeIf { it.isNotBlank() } ?: return ""
        return text.replace(Regex("<[^>]*>"), " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#039;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(420)
    }

    private fun encode(value: String): String = URLEncoder.encode(value.trim(), "UTF-8")

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString

    /** The 1/second floor, in milliseconds (a little over a second, on purpose). */
    private const val PACE_MS = 1100L
}

/**
 * The keyless plumbing a caller needs to hand a Comic Vine result into the
 * comics cascade: a [ComicVineFetch.Volume] as the [MangaFetch.Hit] a shelf row
 * already understands. Kept here (rather than in [MangaFetch]) so the two files
 * stay about their own sources — but note this app has no "series" concept for a
 * comic in [MangaFetch]'s shape, so a volume's own numbers are what carry over:
 * its issue count is its length, and its volumes count is 1 (a volume IS the
 * volume).
 */
internal fun ComicVineFetch.Volume.asHit(): MangaFetch.Hit = MangaFetch.Hit(
    title = title,
    author = credits,
    coverUrl = coverUrl,
    chapters = issues,
    volumes = if (issues > 0) 1 else 0,
    description = description,
    source = if (publisher.isBlank()) "Comic Vine" else "Comic Vine · $publisher"
)

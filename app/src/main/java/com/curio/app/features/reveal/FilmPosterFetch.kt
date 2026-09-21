package com.curio.app.features.reveal

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Keyless film poster resolver — mirrors [SeriesPosterFetch] for TV shows
 * and [AlbumArtFetch] for music.
 *
 * Films in the catalog carry an AUTHORED `imageUrl` field (currently empty
 * for most entries); this resolver fills the gap with keyless providers:
 *
 *  1. **Wikipedia's article image** ([wikipediaPoster]) — see the v428 note
 *     below: the film's own article leads with its POSTER, which is a real,
 *     tall, exact poster and needs no key at all.
 *  2. **iTunes Search API** — `itunes.apple.com/search?term=…&media=movie`.
 *     Free, no API key. Kept even though its film catalogue has stopped
 *     answering (v428): it costs nothing to try and would work again the day
 *     Apple restores the movie store.
 *  3. **TVMaze** — `api.tvmaze.com/singlesearch/shows?q=…`, for TV films and
 *     specials; and **TMDB** by name when a key is configured.
 *
 * Results are memoized per title in-process so reopening never re-queries;
 * Coil's disk cache holds the poster bytes.
 *
 * v428 — WHY WIKIPEDIA IS FIRST. The member: *"the posters are not loading for
 * films"*. The cause was not the app: **iTunes' movie search answers
 * `resultCount: 0` for every query** — `media=movie`, `entity=movie`, every
 * storefront, every title (measured against the live endpoint, while
 * `media=music` on the same endpoint still answers, so the door that closed is
 * the FILM one, not Apple's search itself). The keyless pair therefore had one
 * dead leg and TVMaze, which carries TV rather than cinema, behind it.
 *
 * A film's Wikipedia article leads with its poster, and the REST summary
 * endpoint serves that image (`thumbnail.source`), which is exactly what this
 * needs: no key, a real poster, and the article's own name resolution (`Iron
 * Man (2008 film)` redirects to `Iron Man (2008 film)` or `Iron Man (2008)` as
 * the wiki has it). The candidate chain below is what makes it reliable, and a
 * one-off search backs it up when none of the guesses exist.
 */
object FilmPosterFetch {

    /** iTunes artwork URLs arrive at 100px; 600px is plenty for a poster. */
    private const val ITUNES_SIZE = "600x600bb"

    /**
     * Wikimedia asks every client to say who it is. A named agent with a contact
     * URL is the difference between being served and being rate-limited.
     */
    private const val USER_AGENT = "Curio/1.0 (https://github.com/firefly-sylestia/Curio)"

    /** Tiny in-process memo: film title → poster URL ("" = miss). */
    private val cache = ConcurrentHashMap<String, String>()

    /** The number of resolvable poster providers (0 = Wikipedia, 1 = iTunes). */
    const val PROVIDER_COUNT = 2

    /**
     * Resolve a film's poster URL, best-effort. [filmName] is the topic name
     * verbatim (e.g. "Inception (2010)"); a year suffix becomes the
     * disambiguator the Wikipedia lookup wants and is stripped from the other
     * providers' queries. Returns null when nothing finds art.
     */
    suspend fun resolvePosterUrl(filmName: String, provider: Int = 0): String? =
        withContext(Dispatchers.IO) {
            val title = cleanTitle(filmName)
            val year = yearOf(filmName)
            val key = "$title|${year ?: 0}|p$provider"
            cache[key]?.let { return@withContext it.ifEmpty { null } }

            // v389f — the keyless sources first (the project's rule: a free
            // source answers first), with TMDB behind them only when nothing
            // else found art. No key = nothing changes.
            //
            // The TMDB call sits AFTER the `runCatching`, not inside it: that
            // lambda is not suspend, so a `posterUrl` inside it would not compile.
            val viaKeyless = runCatching {
                when (provider) {
                    1 -> itunesPoster(title) ?: tvmazePoster(title)
                    else -> wikipediaPoster(title, year)
                }
            }.getOrNull()
            val resolved = viaKeyless ?: if (provider == 0) TmdbFetch.posterUrl(title) else null

            cache[key] = resolved.orEmpty()
            resolved
        }

    /**
     * THE FILM'S OWN ARTICLE IMAGE (v428) — the keyless poster door that works.
     *
     * Wikipedia's REST summary answers with a page's lead image, and a film
     * article's lead image is its poster. The guesses are tried in the order a
     * wiki actually names them — `Title (Year film)`, `Title (Year)`,
     * `Title (film)`, `Title` — and each answer is resolved through RedIRECTs by
     * the endpoint itself, so `Inception (2010 film)` finds `Inception`.
     *
     * When every guess misses (a title the wiki files under something else, or a
     * film it does not hold), ONE search is made and its first two results are
     * tried the same way. That is the whole budget per title: at most five
     * small requests, once, and never again for that title.
     */
    private fun wikipediaPoster(title: String, year: Int?): String? {
        val guesses = ArrayList<String>(5)
        if (year != null) {
            guesses += "$title ($year film)"
            guesses += "$title ($year)"
        }
        guesses += "$title (film)"
        guesses += title
        for (guess in guesses) {
            wikiLeadImage(guess)?.let { return it }
        }
        val needle = if (year != null) "$title $year film" else "$title film"
        val json = httpGet(
            "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json" +
                "&srlimit=2&srsearch=${Uri.encode(needle)}"
        ) ?: return null
        val hits = runCatching {
            val rows = JSONObject(json).optJSONObject("query")?.optJSONArray("search")
                ?: return@runCatching emptyList<String>()
            (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.optString("title") }
        }.getOrDefault(emptyList())
        for (hit in hits) {
            if (hit.isBlank()) continue
            wikiLeadImage(hit)?.let { return it }
        }
        return null
    }

    /** A page's lead image, sized for a poster, or null when it has none. */
    private fun wikiLeadImage(page: String): String? {
        val json = httpGet(
            "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                Uri.encode(page.replace(' ', '_'))
        ) ?: return null
        val source = runCatching {
            val row = JSONObject(json)
            // The ORIGINAL image when the summary carries one, the served
            // thumbnail otherwise: the original is what a poster wants and it is
            // the only one the sizing below can widen.
            row.optJSONObject("originalimage")?.optString("source").orEmpty()
                .ifBlank { row.optJSONObject("thumbnail")?.optString("source").orEmpty() }
        }.getOrDefault("")
        return source
            .substringBefore('?')
            .takeIf { it.isNotBlank() }
            ?.let { wikiSized(it, WIKI_WIDTH) }
    }

    /**
     * A Wikimedia file URL at a chosen width.
     *
     * `upload.wikimedia.org/wikipedia/<site>/<a>/<ab>/<File>` becomes
     * `…/wikipedia/<site>/thumb/<a>/<ab>/<File>/<width>px-<File>` — the pattern
     * Wikimedia's own thumbor uses. A URL already on a `/thumb/` path, or one
     * that does not have that shape, is left exactly as it is (coarser, never
     * broken).
     */
    private fun wikiSized(url: String, width: Int): String {
        val marker = "/wikipedia/"
        val at = url.indexOf(marker)
        if (at < 0) return url
        val after = url.substring(at + marker.length)
        if (after.contains("/thumb/")) return url
        val parts = after.split('/')
        if (parts.size < 4) return url
        val site = parts[0]
        val file = parts.drop(3).joinToString("/")
        val prefix = url.substring(0, at + marker.length)
        return "$prefix$site/thumb/${parts[1]}/${parts[2]}/$file/${width}px-$file"
    }

    /** iTunes movie search → best-matching artwork, upscaled to 600px. */
    private fun itunesPoster(title: String): String? {
        val json = httpGet(
            "https://itunes.apple.com/search?term=${Uri.encode(title)}" +
                "&media=movie&entity=movie&limit=8"
        ) ?: return null
        return runCatching {
            val results = JSONObject(json).optJSONArray("results") ?: return null
            var best: String? = null
            var bestScore = 0
            for (i in 0 until results.length()) {
                val r = results.optJSONObject(i) ?: continue
                val art = r.optString("artworkUrl100")
                if (art.isBlank()) continue
                val score = matchScore(r.optString("trackName"), title)
                if (score > bestScore) {
                    bestScore = score
                    best = art
                }
                if (score >= 2) break
            }
            best
                ?.replace("100x100bb", ITUNES_SIZE)
                ?.replace("http://", "https://")
        }.getOrNull()
    }

    /** TVMaze single-show search → the show's poster (for TV movies/specials). */
    private fun tvmazePoster(title: String): String? {
        val json = httpGet(
            "https://api.tvmaze.com/singlesearch/shows?q=${Uri.encode(title)}"
        ) ?: return null
        return runCatching {
            val obj = JSONObject(json)
            val image = obj.optJSONObject("image") ?: return null
            image.optString("original")
                .takeIf { it.isNotBlank() }
                ?: image.optString("medium").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /** Rough relevance: 2 = exact title, 1 = containment / word overlap, 0 = miss. */
    private fun matchScore(name: String, wantTitle: String): Int {
        val n = name.trim()
        val w = wantTitle.trim()
        if (n.equals(w, ignoreCase = true)) return 2
        if (!w.isBlank() && (n.contains(w, ignoreCase = true) ||
                w.contains(n, ignoreCase = true) ||
                titleWordsOverlap(n, w))) return 1
        return 0
    }

    private fun titleWordsOverlap(a: String, b: String): Boolean {
        val wa = a.split(Regex("[^A-Za-z0-9]+")).filter { it.length >= 4 }.map { it.lowercase() }.toSet()
        val wb = b.split(Regex("[^A-Za-z0-9]+")).filter { it.length >= 4 }.map { it.lowercase() }.toSet()
        return wa.any { it in wb }
    }

    /** Minimal keyless GET — 8s timeout, best-effort. */
    private fun httpGet(urlString: String): String? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            val code = conn.responseCode
            if (code != 200) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /**
     * The title without its year or season suffix.
     *
     * The "(2008)" a topic carries is a DISAMBIGUATOR, not part of the name, and
     * the "S2" a tracker's row carries is which season it is, not part of the
     * show — both come off before any provider is asked, because "WandaVision S1"
     * is a string no catalogue has ever heard of ([stripNaming], shared with the
     * series door and the episode guide so all three look the same name up).
     */
    private fun cleanTitle(name: String): String = stripNaming(name)

    /** The year a title carries, when it carries one. */
    private fun yearOf(name: String): Int? =
        Regex("""\((\d{4})\)""").find(name)?.groupValues?.get(1)?.toIntOrNull()

    /** How wide a Wikipedia lead image is rendered — a poster's own width. */
    private const val WIKI_WIDTH = 500
}

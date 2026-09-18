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
 * for most entries); this resolver fills the gap with two keyless providers:
 *
 *  1. **iTunes Search API** — `itunes.apple.com/search?term=…&media=movie`.
 *     Free, no API key, returns artwork at 100px (upscaled to 600px).
 *     Primary because it answers with the exact film.
 *
 *  2. **TVMaze** — `api.tvmaze.com/singlesearch/shows?q=…` with
 *     `singlesearch/search?q=…&type=episode` fallback. TVMaze's single
 *     search covers some films as TV-movie entries; the search endpoint
 *     also indexes movies that were released as TV specials.
 *
 * Results are memoized per title in-process so reopening never re-queries;
 * Coil's disk cache holds the poster bytes.
 */
object FilmPosterFetch {

    /** iTunes artwork URLs arrive at 100px; 600px is plenty for a poster. */
    private const val ITUNES_SIZE = "600x600bb"

    /** Tiny in-process memo: film title → poster URL ("" = miss). */
    private val cache = ConcurrentHashMap<String, String>()

    /** The number of resolvable poster providers (0 = iTunes, 1 = TVMaze). */
    const val PROVIDER_COUNT = 2

    /**
     * Resolve a film's poster URL, best-effort. [filmName] is the topic name
     * verbatim (e.g. "Inception (2010)"); the year suffix is stripped for the
     * queries. Returns null when neither provider finds art.
     */
    suspend fun resolvePosterUrl(filmName: String, provider: Int = 0): String? =
        withContext(Dispatchers.IO) {
            val title = filmName
                .replace(Regex("""\s*\(\d{4}\)\s*$"""), "")
                .trim()
            val key = "$title|p$provider"
            cache[key]?.let { return@withContext it.ifEmpty { null } }

            // v389f — the keyless source first (the project's rule: a free source
            // answers first), with TMDB behind it only when the keyless pair found
            // nothing. iTunes indexes films well but serves a SQUARE artwork, and
            // the older or non-English films it does not carry at all are exactly
            // where a real poster comes from. No key = nothing changes.
            //
            // The TMDB call sits AFTER the `runCatching`, not inside it: that
            // lambda is not suspend, so a `posterUrl` inside it would not compile.
            val viaKeyless = runCatching {
                when (provider) {
                    1 -> tvmazePoster(title)
                    else -> itunesPoster(title)
                }
            }.getOrNull()
            val resolved = viaKeyless ?: TmdbFetch.posterUrl(title)

            cache[key] = resolved.orEmpty()
            resolved
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
            conn.setRequestProperty("User-Agent", "Curio/1.0")
            val code = conn.responseCode
            if (code != 200) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

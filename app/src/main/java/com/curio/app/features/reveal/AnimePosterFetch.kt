package com.curio.app.features.reveal

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Keyless anime poster resolver — mirrors [SeriesPosterFetch] for TV shows
 * and [AlbumArtFetch] for music.
 *
 * Anime in the catalog carry an AUTHORED `imageUrl` field (currently empty
 * for most entries); this resolver fills the gap with two keyless providers:
 *
 *  1. **Jikan** — `api.jikan.moe/v4/anime?q=…`. Free, no API key, returns
 *     MAL poster art (`images.jpg.large_image_url` / `images.jpg.image_url`).
 *     Primary because it is purpose-built for anime.
 *
 *  2. **iTunes Search API** — `itunes.apple.com/search?term=…&media=tvShow&entity=tvSeason`.
 *     The same keyless endpoint the album and series fetchers use; some
 *     anime are listed as TV shows on iTunes. Runs only when Jikan finds no
 *     image.
 *
 * Results are memoized per title in-process so reopening never re-queries;
 * Coil's disk cache holds the poster bytes.
 */
object AnimePosterFetch {

    /** iTunes artwork URLs arrive at 100px; 600px is plenty for a poster. */
    private const val ITUNES_SIZE = "600x600bb"

    /** Tiny in-process memo: anime title → poster URL ("" = miss). */
    private val cache = ConcurrentHashMap<String, String>()

    /** The number of resolvable poster providers (0 = Jikan, 1 = iTunes). */
    const val PROVIDER_COUNT = 2

    /**
     * Resolve an anime's poster URL, best-effort. [animeName] is the topic
     * name verbatim (e.g. "Attack on Titan (2013)"); the year suffix is
     * stripped for the queries. Returns null when neither provider finds art.
     */
    suspend fun resolvePosterUrl(animeName: String, provider: Int = 0): String? =
        withContext(Dispatchers.IO) {
            val title = animeName
                .replace(Regex("""\s*\(\d{4}\)\s*$"""), "")
                .trim()
            val key = "$title|p$provider"
            cache[key]?.let { return@withContext it.ifEmpty { null } }

            // v389f — the keyless pair first (Jikan is MyAnimeList's own data and
            // is the specialist here), with TMDB behind it when the key is set and
            // both keyless providers came up empty. The TMDB call sits AFTER the
            // `runCatching`, not inside it: that lambda is not suspend, so a
            // suspend call in there would not compile.
            val viaKeyless = runCatching {
                when (provider) {
                    1 -> itunesPoster(title)
                    else -> jikanPoster(title)
                }
            }.getOrNull()
            val resolved = viaKeyless ?: TmdbFetch.posterUrl(title)

            cache[key] = resolved.orEmpty()
            resolved
        }

    /** Jikan anime search → best-matching poster (large_image_url). */
    private fun jikanPoster(title: String): String? {
        val json = httpGet(
            "https://api.jikan.moe/v4/anime?q=${Uri.encode(title)}&limit=5"
        ) ?: return null
        return runCatching {
            val data = JSONObject(json).optJSONArray("data") ?: return null
            var best: String? = null
            var bestScore = 0
            for (i in 0 until data.length()) {
                val anime = data.optJSONObject(i) ?: continue
                val name = anime.optString("title", "")
                val images = anime.optJSONObject("images") ?: continue
                val jpg = images.optJSONObject("jpg") ?: continue
                val art = jpg.optString("large_image_url", "")
                    .ifBlank { jpg.optString("image_url", "") }
                if (art.isBlank()) continue
                val score = matchScore(name, title)
                if (score > bestScore) {
                    bestScore = score
                    best = art
                }
                if (score >= 2) break
            }
            best?.replace("http://", "https://")
        }.getOrNull()
    }

    /** iTunes TV-season search → best-matching artwork (fallback for anime on iTunes). */
    private fun itunesPoster(title: String): String? {
        val json = httpGet(
            "https://itunes.apple.com/search?term=${Uri.encode(title)}" +
                "&media=tvShow&entity=tvSeason&limit=8"
        ) ?: return null
        return runCatching {
            val results = JSONObject(json).optJSONArray("results") ?: return null
            var best: String? = null
            var bestScore = 0
            for (i in 0 until results.length()) {
                val r = results.optJSONObject(i) ?: continue
                val art = r.optString("artworkUrl100")
                if (art.isBlank()) continue
                val score = matchScore(r.optString("collectionName"), title)
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

    /**
     * Fetch an anime's synopsis from Jikan. Returns null when the title
     * doesn't match or the synopsis is too short to be useful.
     */
    suspend fun fetchSynopsis(animeName: String): String? = withContext(Dispatchers.IO) {
        val title = animeName
            .replace(Regex("""\s*\(\d{4}\)\s*$"""), "")
            .trim()
        val json = httpGet(
            "https://api.jikan.moe/v4/anime?q=${Uri.encode(title)}&limit=3"
        ) ?: return@withContext null
        runCatching {
            val data = JSONObject(json).optJSONArray("data") ?: return@runCatching null
            for (i in 0 until data.length()) {
                val anime = data.optJSONObject(i) ?: continue
                val name = anime.optString("title", "")
                val score = matchScore(name, title)
                if (score >= 1) {
                    val synopsis = anime.optString("synopsis", "")
                        .replace(Regex("\\[.*?]"), "") // Remove MAL spoiler tags
                        .trim()
                    return@runCatching synopsis.takeIf { it.length >= 40 }
                }
            }
            null
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

    /** Minimal keyless GET — 8s timeout, best-effort. Jikan has rate limits (3 req/s). */
    private fun httpGet(urlString: String): String? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Curio/1.0")
            val code = conn.responseCode
            if (code == 429) {
                // Jikan rate limit — back off briefly and retry once.
                Thread.sleep(1000)
                conn.disconnect()
                val retry = URL(urlString).openConnection() as HttpURLConnection
                retry.requestMethod = "GET"
                retry.connectTimeout = 8000
                retry.readTimeout = 8000
                retry.setRequestProperty("User-Agent", "Curio/1.0")
                if (retry.responseCode == 200) {
                    retry.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else null
            } else if (code == 200) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

package com.curio.app.features.reveal

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keyless TV-series poster resolver for the reveal's series section and the
 * episode-list sheet (mirrors [AlbumArtFetch] on the album side).
 *
 * Series in the catalog carry an AUTHORED `imageUrl` field (currently empty
 * for most shows); this resolver fills the gap with two keyless providers:
 *
 *  1. **TVMaze** — `api.tvmaze.com/singlesearch/shows?q=…`. Purpose-built
 *     for TV, returns real poster art (`image.original` / `image.medium`),
 *     no API key, no signup. Primary because it answers with the exact show.
 *  2. **iTunes Search API** — `itunes.apple.com/search?term=…&media=tvShow&
 *     entity=tvSeason`. The same proven keyless endpoint the album fetcher
 *     uses; artwork arrives at 100px and is upscaled by swapping the size
 *     token. Runs only when TVMaze finds no image.
 *
 * Results are memoized per show in-process so reopening never re-queries;
 * Coil's disk cache holds the poster bytes.
 */
object SeriesPosterFetch {

    /** iTunes artwork URLs arrive at 100px; 600px is plenty for a poster. */
    private const val ITUNES_SIZE = "600x600bb"

    /** Tiny in-process memo: show name (without year) → poster URL ("" = miss). */
    private val cache = ConcurrentHashMap<String, String>()

    /** The number of resolvable poster providers (0 = TVMaze, 1 = iTunes). */
    const val PROVIDER_COUNT = 2

    /**
     * Resolve a series' poster URL, best-effort (provider 0 = TVMaze,
     * 1 = iTunes — the default call cascades TVMaze → iTunes). [showName] is
     * the topic name verbatim (e.g. "Seinfeld (1989)"); the year suffix is
     * stripped for the queries. Returns null when neither provider finds art.
     */
    suspend fun resolvePosterUrl(showName: String, provider: Int = 0): String? =
        withContext(Dispatchers.IO) {
            // v428 — AND A ROW THAT NAMES ITS SEASON IS STILL THE SAME SHOW.
            //
            // A tracker's rows read "WandaVision S1", "Loki S2", "The Gifted
            // S1" — which is exactly what makes a list readable and exactly what
            // a catalogue has never heard of: TVMaze's single search answered
            // nothing for every one of them, so a series row in Curio's own order
            // fell back to its plate while its title sat there in plain sight
            // (member: "the posters are not loading ... in incursion movies or
            // series"). The season is HOW MUCH of the show, never WHICH show, so
            // it comes off before anything is asked (see [cleanShowName]).
            val title = cleanShowName(showName)
            val key = "$title|p$provider"
            cache[key]?.let { return@withContext it.ifEmpty { null } }

            val resolved = runCatching {
                when (provider) {
                    1 -> itunesPoster(title)
                    else -> tvmazePoster(title)
                }
            }.getOrNull()

            cache[key] = resolved.orEmpty()
            resolved
        }

    /** TVMaze single-show search → the show's poster (original, else medium). */
    private fun tvmazePoster(title: String): String? {
        val json = httpGet(
            "https://api.tvmaze.com/singlesearch/shows?q=${Uri.encode(tvmazeSpelling(title))}"
        ) ?: return null
        return runCatching {
            val obj = org.json.JSONObject(json)
            val image = obj.optJSONObject("image") ?: return null
            image.optString("original")
                .takeIf { it.isNotBlank() }
                ?: image.optString("medium").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /** iTunes TV-season search → best-matching artwork, upscaled to 600px. */
    private fun itunesPoster(title: String): String? {
        val json = httpGet(
            "https://itunes.apple.com/search?term=${Uri.encode(title)}" +
                "&media=tvShow&entity=tvSeason&limit=8"
        ) ?: return null
        return runCatching {
            val results = org.json.JSONObject(json).optJSONArray("results") ?: return null
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
                // An exact title hit is as good as it gets.
                if (score >= 2) break
            }
            best
                ?.replace("100x100bb", ITUNES_SIZE)
                ?.replace("http://", "https://")
        }.getOrNull()
    }

    /**
     * A series name as a catalogue writes it.
     *
     * A topic carries its year in brackets ("Seinfeld (1989)") and this app's own
     * Incursion rows carry their season ("WandaVision S1", "season 2"); neither
     * belongs in a query. Both come off — in ANY order, because a row can carry
     * both ("Loki S2 (2023)") — and the result is trimmed.
     */
    private fun cleanShowName(name: String): String = stripNaming(name)

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

    /** True when the two titles share a meaningful (≥4 char) word. */
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
}

/**
 * THE THREE FETCHERS' SHARED SPELLING (v428).
 *
 * A Curio title carries things that are about WHERE IT IS BEING SHOWN from the
 * thing being looked up: a topic's year in brackets ("Seinfeld (1989)") is a
 * disambiguator, and one of this app's own Incursion rows says which season it is
 * ("Loki S2", "Loki season 2"). No catalogue has ever heard of either, so every
 * query in this package goes through [stripNaming] first, and every provider that
 * files an ampersand as the word "and" gets that spelling too.
 *
 * It is one function on purpose: the film door, the series door and the episode
 * guide all look the same title up, and three copies of "which suffixes come off"
 * is how one of them ends up looking up a name nobody has.
 */
internal fun stripNaming(name: String): String {
    val pattern = Regex(
        """\s*(?:\(\d{4}\)|S\d+|season\s+\d+)\s*$""",
        RegexOption.IGNORE_CASE
    )
    // In a loop, because a title can carry BOTH ("Loki S2 (2023)") and one pass
    // only ever takes the last one off.
    var out = name.trim()
    while (true) {
        val next = out.replace(pattern, "").trim()
        if (next == out) return out
        out = next
    }
}

/** The spelling TVMaze files a title under — an ampersand is the word "and". */
internal fun tvmazeSpelling(title: String): String = title
    .replace("&amp;", "and")
    .replace("&", "and")
    .replace(Regex("""\s{2,}"""), " ")
    .trim()

/** Wikimedia asks every client to name itself; a named agent is served. */
private const val USER_AGENT = "Curio/1.0 (https://github.com/firefly-sylestia/Curio)"
package com.curio.app.features.reveal

import android.net.Uri
import com.curio.app.data.SeriesEpisode
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * An ANIME's episode list — TVMaze first, exactly like the series sheet, with
 * Jikan (MyAnimeList's public API, keyless and free) holding up what TVMaze
 * does not carry.
 *
 * v389f. The member's ask was that the anime sheet be "as series": a series
 * sheet is a poster, a progress rail and a real episode list you can tick off,
 * and the anime lane had only the poster and the synopsis.
 *
 * v398 — AND THE SAME SOURCE THE SERIES SHEET READS. The member's own answer
 * ("its look up is also so fast, can u make the anime use the same api as the
 * first same as series" — TVMaze, like series): the order is TVMaze, then
 * Jikan. What the order buys is [SeriesEpisodeFetcher]'s own per-episode data —
 * the air date, the runtime, the rating and the episode's STILL — none of which
 * Jikan states, and which is the whole reason a series sheet read richer than
 * an anime one. It is also one request instead of a paged sweep with a 429
 * back-off, which is the speed the member noticed.
 *
 * What Jikan states per episode: its number (`mal_id`), its title, its air date
 * and its community score. It states no synopsis and no still per episode —
 * those fields come back empty rather than invented, and the episode rows show
 * what there is. Rate limits are real (3 requests/second, 60/minute), so the
 * answer is memoised per title and a 429 is retried once before giving up,
 * exactly as [AnimePosterFetch] already does for its own calls.
 */
object AnimeEpisodeFetcher {

    /** Jikan pages its episode list; this is how many pages are read. */
    private const val MAX_PAGES = 4

    /** Per-page episode count Jikan allows (page size). */
    private const val PAGE_SIZE = 100

    /** In-process memo: title → episodes (an empty list is a real answer). */
    private val cache = ConcurrentHashMap<String, List<SeriesEpisode>>()

    /**
     * Every episode Jikan knows for [animeName], in order. An empty list means
     * "cannot be read" — the sheet then says so rather than spinning.
     */
    suspend fun fetchAll(animeName: String): List<SeriesEpisode> = withContext(Dispatchers.IO) {
        val title = clean(animeName)
        if (title.isBlank()) return@withContext emptyList()
        cache[title]?.let { return@withContext it }

        // v398 — THE SERIES SHEET'S OWN SOURCE FIRST (see this file's header):
        // one request, and the per-episode details Jikan has never had. An empty
        // answer is not cached as the lane's answer — the Jikan sweep below is
        // the fallback, and only ITS result is what this title is remembered by.
        val fromShow = SeriesEpisodeFetcher.fetchAll(title)
        if (fromShow.isNotEmpty()) {
            cache[title] = fromShow
            return@withContext fromShow
        }

        val id = lookupId(title)
        if (id <= 0) {
            cache[title] = emptyList()
            return@withContext emptyList()
        }

        val out = ArrayList<SeriesEpisode>()
        for (page in 1..MAX_PAGES) {
            val body = httpGet(
                "https://api.jikan.moe/v4/anime/$id/episodes?page=$page"
            ) ?: break
            val rows = readPage(body)
            if (rows.isEmpty()) break
            out += rows
            if (rows.size < PAGE_SIZE) break
        }
        cache[title] = out
        out
    }

    /**
     * v410 — THE LIST THIS TITLE ALREADY RESOLVED, without asking anything.
     *
     * The anime card on the reveal draws its episode chip row from the answer
     * this fetcher resolved, and the sheet that opens from that card shows the
     * same list — so the sheet seeds itself from here instead of opening on an
     * empty list and asking for what the card had already been handed (member
     * report: "sometimes the series or anime data is already shown in preview
     * but it loads again when the page opens"). Null means "not asked yet".
     */
    fun cached(animeName: String): List<SeriesEpisode>? = cache[clean(animeName)]

    /** Jikan's own anime id for a name, or 0 when nothing matches. */
    private fun lookupId(title: String): Int {
        val json = httpGet("https://api.jikan.moe/v4/anime?q=${Uri.encode(title)}&limit=5")
            ?: return 0
        return runCatching {
            // `return@runCatching`, never a bare `return`: a non-local return out
            // of this inline lambda makes the compiler emit its
            // `$$$$$NON_LOCAL_RETURN$$$$$` class, whose method name R8 refuses to
            // dex (see batch O). Same behaviour either way.
            val data = JSONObject(json).optJSONArray("data") ?: return@runCatching 0
            var best = 0
            var bestScore = 0
            for (index in 0 until data.length()) {
                val row = data.optJSONObject(index) ?: continue
                val id = row.optInt("mal_id", 0)
                if (id <= 0) continue
                val name = row.optJSONArray("titles")
                    ?.let { titles ->
                        var found = ""
                        for (t in 0 until titles.length()) {
                            val entry = titles.optJSONObject(t) ?: continue
                            if (entry.optString("type").equals("Default", ignoreCase = true)) {
                                found = entry.optString("title")
                                break
                            }
                        }
                        found
                    }
                    .orEmpty()
                    .ifBlank { row.optString("title") }
                    .trim()
                // The right show first: an exact name beats a partial one.
                val score = when {
                    name.equals(title, ignoreCase = true) -> 3
                    name.startsWith(title, ignoreCase = true) -> 2
                    name.contains(title, ignoreCase = true) -> 1
                    else -> 0
                }
                if (score > bestScore) {
                    bestScore = score
                    best = id
                }
            }
            best
        }.getOrDefault(0)
    }

    /** One page of Jikan episodes as [SeriesEpisode] rows. */
    private fun readPage(body: String): List<SeriesEpisode> = runCatching {
        val data = JSONObject(body).optJSONArray("data") ?: return@runCatching emptyList()
        (0 until data.length()).mapNotNull { index ->
            val row = data.optJSONObject(index) ?: return@mapNotNull null
            val number = row.optInt("mal_id", 0)
            if (number <= 0) return@mapNotNull null
            SeriesEpisode(
                // Jikan counts a title's episodes in one run — a later cour is a
                // separate anime entry there, which is exactly how the lane's
                // topics are named — so the whole list is one season.
                season = 1,
                number = number,
                title = row.optString("title").trim(),
                // Jikan states no per-episode synopsis. Empty is honest; an
                // invented one would be a lie on a page the member reads.
                summary = "",
                airdate = row.optString("aired").take(10),
                runtime = 0,
                rating = row.optDouble("score", 0.0).toFloat(),
                stillUrl = ""
            )
        }
    }.getOrDefault(emptyList())

    /** A topic name without its "(2013)" disambiguator. */
    private fun clean(animeName: String): String =
        animeName.replace(Regex("""\s*\(\d{4}\)\s*$"""), "").trim()

    /** Minimal keyless GET — 8s timeout, one retry on Jikan's 429. */
    private fun httpGet(urlString: String): String? = runCatching {
        var attempt = 0
        while (attempt < 2) {
            attempt += 1
            val conn = URL(urlString).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 8_000
                conn.readTimeout = 8_000
                conn.setRequestProperty("Accept", "application/json")
                when (conn.responseCode) {
                    200 -> return@runCatching conn.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    429 -> Thread.sleep(1200L)
                    else -> return@runCatching null
                }
            } finally {
                conn.disconnect()
            }
        }
        null
    }.getOrNull()
}

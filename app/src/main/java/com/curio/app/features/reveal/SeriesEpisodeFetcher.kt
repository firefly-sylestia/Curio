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
 * Enriches authored [SeriesEpisode] entries with metadata from TVMaze:
 * airdate, runtime, rating (0–10), and episode still image.
 *
 * The fetcher is opt-in (gated on [com.curio.app.data.AppPreferences.seriesFetchEnabledState])
 * and memoized per show name so reopening the sheet never re-queries.
 *
 * Flow:
 *  1. Look up the show by name → TVMaze show ID.
 *  2. Fetch the episode list for that ID.
 *  3. Match each authored episode by (season, number) and merge the
 *     fetched fields into the existing [SeriesEpisode] copies.
 */
object SeriesEpisodeFetcher {

    /** In-process memo: show name → list of TVMaze episode JSONs (raw). */
    private val episodeCache = ConcurrentHashMap<String, List<JSONObject>>()

    /**
     * Enrich [episodes] with TVMaze metadata. Returns the same list with
     * the new fields populated where a match was found. Episodes that don't
     * match a TVMaze entry keep their original values.
     */
    suspend fun enrich(
        showName: String,
        episodes: List<SeriesEpisode>
    ): List<SeriesEpisode> = withContext(Dispatchers.IO) {
        if (episodes.isEmpty()) return@withContext episodes

        val tvmazeEpisodes = fetchEpisodes(showName)
        if (tvmazeEpisodes.isEmpty()) return@withContext episodes

        // Index fetched episodes by (season, number) for O(1) lookup.
        val fetched = mutableMapOf<Pair<Int, Int>, JSONObject>()
        for (ep in tvmazeEpisodes) {
            val s = ep.optInt("season", 0)
            val n = ep.optInt("number", 0)
            if (s > 0 && n > 0) fetched[s to n] = ep
        }

        episodes.map { ep ->
            val match = fetched[ep.season to ep.number] ?: return@map ep
            ep.copy(
                airdate = ep.airdate.ifBlank { match.optString("airdate", "") },
                runtime = ep.runtime.takeIf { it > 0 } ?: match.optInt("runtime", 0),
                rating = ep.rating.takeIf { it > 0f }
                    ?: match.optJSONObject("rating")?.optDouble("average", 0.0)?.toFloat()
                    ?: 0f,
                stillUrl = ep.stillUrl.ifBlank {
                    match.optJSONObject("image")?.optString("original", "")
                        ?.takeIf { it.isNotBlank() }
                        ?: ""
                }
            )
        }
    }

    /** Fetch the episode list for [showName] from TVMaze, memoized. */
    private fun fetchEpisodes(showName: String): List<JSONObject> {
        val title = showName.replace(Regex("""\s*\(\d{4}\)\s*$"""), "").trim()
        episodeCache[title]?.let { return it }

        // v389d — PLAIN BRANCHES, NOT `?: run { … return }`. A non-local return
        // out of an inline lambda makes the compiler emit its
        // `$$$$$NON_LOCAL_RETURN$$$$$` class, and R8 cannot dex that class's
        // method name — which broke the release build while debug built fine.
        // Behaviour is identical; only the synthetic class is gone.
        val showId = lookupShowId(title)
        if (showId == null) {
            episodeCache[title] = emptyList()
            return emptyList()
        }

        val json = httpGet("https://api.tvmaze.com/shows/$showId/episodes")
        if (json == null) {
            episodeCache[title] = emptyList()
            return emptyList()
        }

        return try {
            val arr = org.json.JSONArray(json)
            val list = (0 until arr.length()).map { arr.getJSONObject(it) }
            episodeCache[title] = list
            list
        } catch (_: Exception) {
            episodeCache[title] = emptyList()
            emptyList()
        }
    }

    /**
     * Fetch ALL episodes for a show from TVMaze, converting them to
     * [SeriesEpisode] objects. Used when the topic has no authored episodes
     * but the member wants to see the episode guide.
     */
    suspend fun fetchAll(showName: String): List<SeriesEpisode> = withContext(Dispatchers.IO) {
        val title = showName.replace(Regex("""\s*\(\d{4}\)\s*$"""), "").trim()
        // Reuse the same memoized episode payload as enrichment. The previous
        // path bypassed this cache, so opening the series sheet fetched again.
        val cachedEpisodes = fetchEpisodes(title)
        if (cachedEpisodes.isEmpty()) return@withContext emptyList()
        try {
            cachedEpisodes.mapNotNull { ep ->
                run {
                    val season = ep.optInt("season", 0)
                    val number = ep.optInt("number", 0)
                    if (season <= 0 || number <= 0) return@run null
                    SeriesEpisode(
                        season = season,
                        number = number,
                        title = ep.optString("name", ""),
                        summary = ep.optString("summary", "").replace(Regex("<[^>]+>"), "").trim(),
                        airdate = ep.optString("airdate", ""),
                        runtime = ep.optInt("runtime", 0),
                        rating = ep.optJSONObject("rating")?.optDouble("average", 0.0)?.toFloat() ?: 0f,
                        stillUrl = ep.optJSONObject("image")?.optString("original", "") ?: ""
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Look up the TVMaze show ID by name. Returns null on miss. */
    private fun lookupShowId(title: String): Int? {
        val json = httpGet(
            "https://api.tvmaze.com/singlesearch/shows?q=${Uri.encode(title)}"
        ) ?: return null
        return try {
            JSONObject(json).optInt("id", 0).takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
    }

    /** Minimal synchronous HTTP GET — runs on IO. */
    private fun httpGet(urlStr: String): String? = try {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        conn.setRequestProperty("Accept", "application/json")
        if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() }
        else null
    } catch (_: Exception) {
        null
    }
}

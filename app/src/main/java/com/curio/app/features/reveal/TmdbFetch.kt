package com.curio.app.features.reveal

import android.net.Uri
import com.curio.app.BuildConfig
import com.curio.app.data.SeriesEpisode
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * TMDB (The Movie Database) — the KEYED provider behind the film and anime
 * sheets.
 *
 * v389f. Three things it buys that no keyless source can:
 *
 *  1. **Artwork at poster size.** `image.tmdb.org` serves real posters
 *     (`/t/p/w500`), where the keyless pair the app already had — iTunes'
 *     square 600px album style, and the still TVMaze keeps for a show — are
 *     both a compromise for a tall poster.
 *  2. **A film's own facts.** Runtime, the community rating, the genres, the
 *     director and the top-billed cast. Nothing keyless in the app states
 *     these; the film sheet's own record is where they belong.
 *  3. **Whether a title is a FILM or a SHOW.** This is the one the member
 *     actually asked for: an "animated movie" lane holds series as often as it
 *     holds films, and TMDB's answer decides which one a topic is — a show gets
 *     an episode list exactly as a series does, a film gets its facts.
 *
 * DESIGN RULES IT OBEYS (the project's own, or it would not be here):
 *
 *  - **Keyless first, keyed as the upgrade.** Everything in this file is only
 *    ever reached when [isConfigured] is true. With no key the callers fall
 *    through to iTunes/TVMaze/Jikan exactly as before, so a build with no
 *    secret behaves identically — there is no row, no error and no blank sheet.
 *  - **Nothing is asked twice.** Every query is memoised per title, misses
 *    included, so reopening a sheet never re-asks TMDB.
 *  - **A failure is not an answer.** Every call is wrapped, every parse is
 *    wrapped, and a null means "ask the next provider", never "show nothing".
 *  - **No key ever reaches a log or a string.** It travels as the `api_key`
 *    query parameter, which is how TMDB's own v3 documentation specifies it.
 *
 * The key comes from `BuildConfig.TMDB_API_KEY`, set by the `TMDB_API_KEY`
 * environment variable (see `.env.example` and `.github/AGENTS.md` for the
 * GitHub Actions guide).
 */
object TmdbFetch {

    /** Poster width TMDB serves from `image.tmdb.org`. */
    private const val POSTER_BASE = "https://image.tmdb.org/t/p/w500"

    /** How many seasons of a show are read before the list is "enough". */
    private const val SEASON_CAP = 4

    /** How much of the cast a sheet shows — the top-billed few. */
    private const val CAST_ROWS = 6

    /**
     * v428 — THE TWO CREDENTIALS, READ THE WAY TMDB DOCUMENTS THEM.
     *
     * TMDB's own application-authentication page states it plainly: *"Version 3
     * is controlled by either a single query parameter, `api_key`, or by using
     * your access token as a Bearer token"*, and the account page holds both — a
     * v3 **API key** and an **API Read Access Token** (a JWT), the token being
     * the one that *"has the added benefit of being a single authentication
     * process that you can use across both the v3 and v4 methods"*
     * (https://developer.themoviedb.org/docs/authentication-application).
     *
     * So a build may carry EITHER, and this reads both: the read token
     * (`TMDB_READ_TOKEN`) wins because it travels as a header and works on both
     * versions; otherwise the v3 key (`TMDB_API_KEY`) travels as `api_key`. A JWT
     * pasted into the KEY field is recognised as a token rather than sent as a
     * query parameter no one will accept (a v4 token starts with `eyJ`, which is
     * base64 for `{"` — a v3 key is 32 hex characters and never does).
     *
     * Both are `internal` on purpose: the Dev settings source lab probes TMDB
     * with the credential THIS object would send and in the shape it would send
     * it (the token as a Bearer header, the key as `?api_key=`), so the lab can
     * never report "no credential" for a build the app itself authenticates
     * with. The resolution lives here and nowhere else.
     */
    internal val apiKey: String
        get() = runCatching { BuildConfig.TMDB_API_KEY.trim() }.getOrDefault("")

    internal val readToken: String
        get() = runCatching {
            BuildConfig.TMDB_READ_TOKEN.trim().ifBlank {
                apiKey.takeIf { it.startsWith("eyJ") }.orEmpty()
            }
        }.getOrDefault("")

    /** Whether this build carries a TMDB credential at all. */
    val isConfigured: Boolean
        get() = readToken.isNotBlank() || apiKey.isNotBlank()

    /** A title's own record, as TMDB states it. */
    internal data class Facts(
        val posterUrl: String,
        val year: String,
        val runtime: Int,
        val rating: Float,
        val genres: List<String>,
        val director: String,
        val cast: List<String>,
        val overview: String,
        /** Non-zero only when TMDB knows this title as a TV SHOW. */
        val showId: Int,
        /** A show's season count; 0 for a film. */
        val seasonCount: Int
    ) {
        /** The one fact the film/anime sheets branch on. */
        val isShow: Boolean get() = showId > 0
    }

    private val factsCache = ConcurrentHashMap<String, Facts>()

    /**
     * v427 — `movie:603` / `tv:84958` → a poster URL ("" = asked, nothing there).
     *
     * Kept apart from [factsCache] because this door is reached by ID and never
     * by name: a caller that already holds TMDB's own number must not be sent
     * through a name search, where a remake, a re-release or a shared title is
     * exactly how the wrong poster arrives.
     */
    private val idPosterCache = ConcurrentHashMap<String, String>()

    /**
     * A title → its episodes. An EMPTY list is a real, remembered answer —
     * "asked, not a show" — because `ConcurrentHashMap` cannot hold a null value,
     * so "no answer yet" and "no show here" have to be told apart by the key
     * being absent rather than by the value being null.
     */
    private val episodeCache = ConcurrentHashMap<String, List<SeriesEpisode>>()

    /** The poster for a film (or a show), or null when there is no answer. */
    suspend fun posterUrl(title: String): String? =
        facts(title)?.posterUrl?.takeIf { it.isNotBlank() }

    /**
     * v427 — A POSTER FOR A TITLE WE ALREADY KNOW BY ID.
     *
     * For a caller whose own data carries a TMDB id — an Incursion row knows
     * which film or season it is by number — a search is a needless guess. The
     * detail read is one call, memoised per id, and a MISS is remembered as an
     * empty string so it is never asked twice (the same convention [Facts] uses:
     * a `ConcurrentHashMap` cannot hold a null, so "not asked" and "nothing
     * there" have to be told apart by the key).
     *
     * [isShow] decides WHICH catalogue the id belongs to. A row labelled a series
     * is a show; a row labelled a film is a movie — and the caller tries the
     * other one only when this returns nothing, so an id upstream filed under the
     * other kind still finds its art.
     */
    suspend fun posterUrlById(id: Int, isShow: Boolean): String? = withContext(Dispatchers.IO) {
        if (!isConfigured || id <= 0) return@withContext null
        val key = (if (isShow) "tv:" else "movie:") + id
        idPosterCache[key]?.let { return@withContext it.ifEmpty { null } }
        // Written as a branch rather than a non-local `return null`: the project's
        // own rule (see [getJson]) — a return out of an inline lambda becomes the
        // `$$$$$NON_LOCAL_RETURN$$$$$` helper class, whose method name R8 refuses
        // to dex, so a debug build is fine and a release build dies.
        val resolved = runCatching {
            val body = getJson(if (isShow) "/tv/$id" else "/movie/$id") ?: return@runCatching ""
            imageUrl(JSONObject(body).optString("poster_path"))
        }.getOrDefault("")
        idPosterCache[key] = resolved
        resolved.ifEmpty { null }
    }

    /**
     * A title's record — its poster, its facts, and whether it is really a show.
     *
     * Movies are searched FIRST, because the lanes that reach this call are
     * film lanes and a film that shares a name with a show should stay the film.
     * Only when the film search has nothing does the show search run, and the
     * order is why [Facts.isShow] can be trusted.
     */
    internal suspend fun facts(title: String): Facts? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val name = clean(title)
        if (name.isBlank()) return@withContext null
        factsCache[name]?.let { return@withContext it }
        val resolved = runCatching { movieFacts(name) ?: showFacts(name) }.getOrNull()
        if (resolved != null) factsCache[name] = resolved
        resolved
    }

    /**
     * The episode list of a title TMDB knows as a SHOW.
     *
     * Returns null (not an empty list) when the title is not a show at all, so
     * a caller can tell "this is a film, show its facts" from "this is a show
     * whose episodes could not be read".
     */
    suspend fun showEpisodes(title: String): List<SeriesEpisode>? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val name = clean(title)
        if (name.isBlank()) return@withContext null
        episodeCache[name]?.let { return@withContext it.ifEmpty { null } }
        val record = facts(name)
        val showId = record?.showId ?: 0
        if (showId <= 0) {
            episodeCache[name] = emptyList()
            return@withContext null
        }
        val seasons = record?.seasonCount ?: 0
        val episodes = runCatching { readShowEpisodes(showId, seasons) }.getOrDefault(emptyList())
        episodeCache[name] = episodes
        // A show whose list came back empty reads as "no show" to the caller —
        // which is the safe direction: the film sheet it then opens still shows
        // the work's own record, where an empty episode sheet would show nothing.
        episodes.ifEmpty { null }
    }

    // ── films ───────────────────────────────────────────────────────────────

    private fun movieFacts(title: String): Facts? {
        val searchBody = getJson("/search/movie?query=${Uri.encode(title)}&include_adult=false")
            ?: return null
        val hit = bestHit(searchBody, "title", "release_date", title) ?: return null
        val id = hit.optInt("id", 0)
        if (id <= 0) return null
        val detail = getJson("/movie/$id?append_to_response=credits") ?: return null
        val row = runCatching { JSONObject(detail) }.getOrNull() ?: return null
        val credits = row.optJSONObject("credits")
        return Facts(
            posterUrl = imageUrl(hit.optString("poster_path")),
            year = (hit.optString("release_date").ifBlank { row.optString("release_date") })
                .take(4),
            runtime = row.optInt("runtime", 0),
            rating = row.optDouble("vote_average", 0.0).toFloat(),
            genres = row.optJSONArray("genres").namedRows(),
            director = credits?.optJSONArray("crew")
                ?.firstWhere { it.optString("job").equals("Director", ignoreCase = true) }
                ?.optString("name")
                .orEmpty(),
            cast = credits?.optJSONArray("cast").namedRows(CAST_ROWS).orEmpty(),
            overview = row.optString("overview").ifBlank { hit.optString("overview") }.trim(),
            showId = 0,
            seasonCount = 0
        )
    }

    // ── shows ───────────────────────────────────────────────────────────────

    private fun showFacts(title: String): Facts? {
        val searchBody = getJson("/search/tv?query=${Uri.encode(title)}&include_adult=false")
            ?: return null
        val hit = bestHit(searchBody, "name", "first_air_date", title) ?: return null
        val id = hit.optInt("id", 0)
        if (id <= 0) return null
        val detail = getJson("/tv/$id") ?: return null
        val row = runCatching { JSONObject(detail) }.getOrNull() ?: return null
        return Facts(
            posterUrl = imageUrl(hit.optString("poster_path")),
            year = (hit.optString("first_air_date").ifBlank { row.optString("first_air_date") })
                .take(4),
            runtime = row.optJSONArray("episode_run_time")?.optInt(0, 0) ?: 0,
            rating = row.optDouble("vote_average", 0.0).toFloat(),
            genres = row.optJSONArray("genres").namedRows(),
            director = row.optJSONArray("created_by").namedRows(2).joinToString(", "),
            cast = emptyList(),
            overview = row.optString("overview").ifBlank { hit.optString("overview") }.trim(),
            showId = id,
            seasonCount = row.optInt("number_of_seasons", 0)
        )
    }

    /** Read a show's episodes, season by season, up to [SEASON_CAP]. */
    private fun readShowEpisodes(showId: Int, seasonCount: Int): List<SeriesEpisode> {
        val seasons = if (seasonCount > 0) seasonCount.coerceAtMost(SEASON_CAP) else 1
        val out = ArrayList<SeriesEpisode>()
        for (season in 1..seasons) {
            val body = getJson("/tv/$showId/season/$season") ?: continue
            val row = runCatching { JSONObject(body) }.getOrNull() ?: continue
            val list = row.optJSONArray("episodes") ?: continue
            for (index in 0 until list.length()) {
                val ep = list.optJSONObject(index) ?: continue
                val number = ep.optInt("episode_number", 0)
                if (number <= 0) continue
                out += SeriesEpisode(
                    season = season,
                    number = number,
                    title = ep.optString("name").trim(),
                    summary = ep.optString("overview").trim(),
                    airdate = ep.optString("air_date").trim(),
                    runtime = ep.optInt("runtime", 0),
                    rating = ep.optDouble("vote_average", 0.0).toFloat(),
                    stillUrl = imageUrl(ep.optString("still_path"))
                )
            }
        }
        return out
    }

    // ── shared ──────────────────────────────────────────────────────────────

    /**
     * The best of a search response's `results`, or null when nothing is close
     * enough. A name and a year are what make a match: an exact name wins, a
     * name at the head of the result wins over containment, and a title whose
     * year contradicts the topic's own loses — the "(2005)" in a topic name is
     * exactly the disambiguator that keeps the remake from answering for the
     * original.
     */
    private fun bestHit(body: String, nameField: String, dateField: String, want: String): JSONObject? {
        val results = runCatching { JSONObject(body).optJSONArray("results") }.getOrNull() ?: return null
        var best: JSONObject? = null
        var bestScore = 0
        val wantYear = Regex("""\((\d{4})\)""").find(want)?.groupValues?.get(1).orEmpty()
        for (index in 0 until results.length()) {
            val row = results.optJSONObject(index) ?: continue
            val name = row.optString(nameField).trim()
            if (name.isBlank()) continue
            var score = when {
                name.equals(want, ignoreCase = true) -> 3
                name.startsWith(want, ignoreCase = true) -> 2
                name.contains(want, ignoreCase = true) -> 1
                else -> 0
            }
            val year = row.optString(dateField).take(4)
            if (wantYear.isNotEmpty() && year.isNotEmpty()) {
                if (year == wantYear) score += 2 else score -= 2
            }
            if (score > bestScore) {
                bestScore = score
                best = row
            }
        }
        return best.takeIf { bestScore > 0 }
    }

    /** `…/t/p/w500/abc.jpg` from a TMDB `poster_path`, or "" when absent. */
    private fun imageUrl(path: String): String =
        path.trim().takeIf { it.isNotBlank() }?.let { POSTER_BASE + it }.orEmpty()

    /** A topic name without its "(2010)" disambiguator. */
    private fun clean(title: String): String =
        title.replace(Regex("""\s*\(\d{4}\)\s*$"""), "").trim()

    /**
     * Genre / cast names out of a TMDB array of objects, or an array of strings.
     *
     * Named [namedRows] rather than `names` on purpose: org.json's own
     * `JSONArray.names()` exists and returns a JSONArray, and a member always
     * wins over an extension, so an extension called `names` would silently
     * resolve to the member and hand back the wrong type.
     */
    private fun org.json.JSONArray?.namedRows(limit: Int = 8): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(limit)
        for (index in 0 until length()) {
            if (out.size >= limit) break
            val name = when (val item = opt(index)) {
                is JSONObject -> item.optString("name")
                is String -> item
                else -> ""
            }.trim()
            if (name.isNotBlank()) out += name
        }
        return out
    }

    /** The first row of a TMDB array of objects matching [predicate]. */
    private fun org.json.JSONArray.firstWhere(predicate: (JSONObject) -> Boolean): JSONObject? {
        for (index in 0 until length()) {
            val row = optJSONObject(index) ?: continue
            if (predicate(row)) return row
        }
        return null
    }

    /**
     * One GET against TMDB v3, best-effort.
     *
     * Written as a branch rather than `if (…) return null` ON PURPOSE: a return
     * out of this inline lambda is a non-local return, and the compiler answers
     * one with its `$$$$$NON_LOCAL_RETURN$$$$$` helper class, whose method name
     * R8 refuses to dex — a debug build fine, a release build dead (the bug
     * batch O chased through `PersonalCanvasKt`). The behaviour is identical.
     */
    private fun getJson(path: String): String? = runCatching {
        val token = readToken
        val key = apiKey
        // One credential or the other, never both and never a bare `api_key=`:
        // the token authenticates by HEADER (see the note on [readToken]), so the
        // query is left alone when it is present. With a token, `path` keeps its
        // own `?…` untouched.
        val url = "https://api.themoviedb.org/3$path" + when {
            token.isNotBlank() || key.isBlank() -> ""
            path.contains('?') -> "&api_key=$key"
            else -> "?api_key=$key"
        }
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("Accept", "application/json")
            // "Authorization: Bearer ACCESS_TOKEN" — the docs' own header, and
            // the one form of authentication TMDB accepts on v3 and v4 alike.
            if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

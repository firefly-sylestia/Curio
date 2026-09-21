package com.curio.app.features.reveal

import android.net.Uri
import com.curio.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * v428b — OMDb: THE SECOND KEYED DOOR FOR A FILM OR A SHOW.
 *
 * The member, after a round of doors that did not answer: *"also add omdb key for
 * fetching if any doesnt fetch"*. OMDb is the right shape for that job. It is one
 * request, it answers by NAME (no id needed), and it hands back the three things a
 * title's page wants that the keyless doors do not state — a **plot**, a **rating**
 * and a **poster** at a real size — plus the runtime, the genres and the IMDb id.
 *
 * WHERE IT SITS IN THE CHAIN, and why not higher:
 *
 *  - **Artwork** ([posterUrl]) is asked for AFTER TMDB by id and BEFORE the keyless
 *    pair, because OMDb's poster is a real poster (Amazon's large form) while
 *    iTunes' film catalogue answers nothing at all and TVMaze holds television
 *    stills. It is a FILM-and-SHOW door, so it also covers a series' art.
 *  - **A description** ([plot]) is one of the four answers the Incursion sheet's
 *    own chain asks for (see `features/incursion/IncursionSources.kt`), after
 *    TMDB's overview, which is longer and in the app's own voice.
 *
 * The rules this file keeps are the project's own:
 *
 *  - **Keyless first, keyed as the upgrade.** Nothing here runs without
 *    [isConfigured]; with no key the callers behave exactly as they did.
 *  - **Fail FAST.** OMDb is a fallback, so it must never be the reason a chain
 *    waits: 3.5s to connect and 5s to read, where the doors in front of it get the
 *    same short budget. (The member's own report of a TMDB probe taking 24s is why
 *    every one of these doors is budgeted now — a fallback that answers after the
 *    member has left the screen is not a fallback.)
 *  - **Nothing is asked twice** — answers AND misses are memoised per query.
 *  - **A failure is not an answer** — every call and every parse is wrapped, and a
 *    null means "ask the next door", never "show nothing".
 *
 * The key is `BuildConfig.OMDB_API_KEY` (see `.env.example` and
 * `.github/AGENTS.md`). OMDb's free tier is 1,000 requests a day, which is why
 * only the fields a page draws are read and only once per title.
 */
internal object OmdbFetch {

    /** A title's record, as OMDb states it. */
    internal data class Facts(
        val title: String,
        val year: String,
        val posterUrl: String,
        val plot: String,
        /** Minutes, 0 when OMDb said nothing usable. */
        val runtime: Int,
        val rating: Float,
        val genres: List<String>,
        val rated: String,
        val imdbId: String
    )

    private val cache = ConcurrentHashMap<String, Facts>()

    /** Whether this build carries an OMDb key at all. */
    internal val isConfigured: Boolean
        get() = runCatching { BuildConfig.OMDB_API_KEY.isNotBlank() }.getOrDefault(false)

    /**
     * A title's OMDb record, or null.
     *
     * [year] and [isSeries] are both OPTIONAL and both are disambiguators: the year
     * is what tells a remake from its original, and the kind stops a film's name
     * from answering with the series that borrowed it. Only the fields the page
     * draws are requested (`plot=short`), because a long plot is a third of the
     * free tier's daily allowance in bytes alone.
     */
    internal suspend fun facts(title: String, year: Int? = null, isSeries: Boolean = false): Facts? =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext null
            val name = clean(title)
            if (name.isBlank()) return@withContext null
            val key = "${name.lowercase()}|${year ?: 0}|${if (isSeries) "s" else "m"}"
            cache[key]?.let { return@withContext it }
            val body = getJson(name, year, isSeries)
            val row = body?.let { runCatching { JSONObject(it) }.getOrNull() }
            // OMDb says "no" with HTTP 200 and `Response: "False"`, so the
            // presence of the field is the answer, not the status code.
            val resolved = row
                ?.takeIf { it.optString("Response").equals("True", ignoreCase = true) }
                ?.let { it ->
                    Facts(
                        title = it.optString("Title").trim(),
                        year = it.optString("Year").trim().take(4),
                        posterUrl = it.optString("Poster").usableUrl(),
                        plot = it.optString("Plot").trim().takeIf { text ->
                            text.isNotBlank() && !text.equals("N/A", ignoreCase = true)
                        }.orEmpty(),
                        runtime = it.optString("Runtime").substringBefore(' ').trim().toIntOrNull() ?: 0,
                        rating = it.optString("imdbRating").toFloatOrNull() ?: 0f,
                        genres = it.optString("Genre").split(',').map { part -> part.trim() }
                            .filter { part -> part.isNotBlank() && !part.equals("N/A", true) },
                        rated = it.optString("Rated").trim().takeIf { text ->
                            text.isNotBlank() && !text.equals("N/A", ignoreCase = true)
                        }.orEmpty(),
                        imdbId = it.optString("imdbID").trim()
                    )
                }
            if (resolved != null) cache[key] = resolved
            resolved
        }

    /** The poster for a title, or null ("" and "N/A" are both "nothing there"). */
    internal suspend fun posterUrl(title: String, year: Int? = null, isSeries: Boolean = false): String? =
        facts(title, year, isSeries)?.posterUrl?.takeIf { it.isNotBlank() }

    /** The plot for a title, or null. */
    internal suspend fun plot(title: String, year: Int? = null, isSeries: Boolean = false): String? =
        facts(title, year, isSeries)?.plot?.takeIf { it.isNotBlank() }

    // ── shared ──────────────────────────────────────────────────────────────

    /** OMDb's own "nothing there" marker, which is a literal rather than a null. */
    private fun String.usableUrl(): String =
        trim().takeIf { it.startsWith("http", ignoreCase = true) }.orEmpty()

    /** A topic name without its \"(2010)\" disambiguator. */
    private fun clean(title: String): String =
        title.replace(Regex("""\s*\(\d{4}\)\s*$"""), "").trim()

    /**
     * One GET, best-effort, with the SHORT budget a fallback is allowed.
     *
     * Written as a branch rather than `if (…) return null`: a return out of this
     * inline lambda is a non-local return, and the compiler answers one with its
     * `$$$$$NON_LOCAL_RETURN$$$$$` helper class, whose method name R8 refuses to
     * dex (a debug build fine, a release build dead — the bug the project's own
     * notes call out).
     */
    private fun getJson(title: String, year: Int?, isSeries: Boolean): String? = runCatching {
        val url = buildString {
            append("https://www.omdbapi.com/?apikey=").append(BuildConfig.OMDB_API_KEY)
            append("&t=").append(Uri.encode(title))
            append("&plot=short&r=json")
            append("&type=").append(if (isSeries) "series" else "movie")
            if (year != null && year > 0) append("&y=").append(year)
        }
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 3_500
            conn.readTimeout = 5_000
            conn.setRequestProperty("Accept", "application/json")
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

package com.curio.app.features.incursion

import com.curio.app.data.AppPreferences
import com.curio.app.data.IncursionEntry
import com.curio.app.features.personal.ComicVineFetch
import com.curio.app.features.reveal.OmdbFetch
import com.curio.app.features.reveal.TmdbFetch
import com.curio.app.features.reveal.WikipediaSummary
import com.curio.app.features.reveal.stripNaming
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * v429 — WHAT AN INCURSION ROW KNOWS BEYOND ITSELF.
 *
 * A row arrives from upstream with an order, a title, a year, a kind and — for
 * some rows only — a line of synopsis. The member's report, after the posters
 * started arriving: *"for incursion ui the movie posters description doesnt
 * fetch, use all the avalabel api also i added comicvine api too mybe use that
 * for incursion with more fallbacks"*. This object is that: the row's own text,
 * facts and last-resort artwork, gathered from EVERY door the app holds a
 * credential (or none) for.
 *
 * ── THE DOORS, IN THE ORDER THEY ARE PREFERRED ─────────────────────────────
 *
 *  1. **TMDB** ([TmdbFetch]) — keyed. Its `overview` is the best-written synopsis
 *     of the set, and the same read states the runtime, the rating and the
 *     genres. Asked by name, film first (a title that is both stays the film).
 *  2. **OMDb** ([OmdbFetch]) — keyed, NEW, and asked by name: the door that
 *     answers a *description* where the keyless pair does not (TVMaze is a
 *     television database; iTunes' film catalogue answered nothing at all by
 *     2026). It also carries a real poster, the IMDb rating and the runtime.
 *  3. **Wikipedia** — keyless, and the widest net of the four: a film or a show
 *     almost always has an article, and its summary is prose a member can read.
 *     Pair (1)'s and (2)'s failures are usually a NAME the catalogue files
 *     differently, which is exactly what a search over an encyclopaedia fixes.
 *  4. **Comic Vine** ([ComicVineFetch]) — keyed, and the last net: it holds a
 *     film catalogue of its own, and when a film is a comic adaptation (a large
 *     share of what Incursion is) it is the door with the longest, most precise
 *     entry.
 *
 * ── WHY THE DOORS RACE, AND WHY THEY ARE BUDGETED ──────────────────────────
 *
 * Four doors in a row is four waits, and the member has now twice reported the
 * result of that arithmetic: *"tmdb no answer 24060 ms"* — three reads inside
 * one title, each allowed eight seconds to connect and eight to answer. So:
 *
 *  · **Staged, and raced within a stage.** Stage one is the keyed pair; stage two
 *    is the keyless pair, and it is only reached when stage one had nothing. Four
 *    sequential waits become two parallel ones, each bounded by its slowest door
 *    rather than by their sum.
 *  · **Every door gets [DOOR_BUDGET_MS].** A door that cannot answer inside it is
 *    treated as a door with nothing to say, and the next stage proceeds. A
 *    fallback that answers after the member has left the screen is not a fallback.
 *  · **Nothing is asked twice — misses included.** A row's record is remembered
 *    for the life of the process, and an empty record is remembered as empty, so
 *    reopening a sheet never re-runs the chain.
 *  · **One switch governs it.** Every candidate here is a network read, so the
 *    app's own consent (`AppPreferences.coverFetchEnabledState`, the Experiments
 *    page's "Cover fetching" — the canonical gate) is checked before anything is
 *    asked. With it off a row shows the plate and upstream's own words, exactly
 *    as it did before this file existed.
 */
internal object IncursionSources {

    /**
     * What a row's own chain found. Every field is optional because no door
     * states all of them, and a field that no door had is simply absent rather
     * than zeroed — the sheet draws what is there and nothing else.
     */
    internal data class Record(
        val description: String = "",
        val rating: Float = 0f,
        val runtime: Int = 0,
        val genres: List<String> = emptyList(),
        val rated: String = ""
    ) {
        val isEmpty: Boolean
            get() = description.isBlank() && rating <= 0f && runtime <= 0 && genres.isEmpty()
    }

    /** `storageKey → Record` (`isEmpty` = asked, nothing to be had). */
    private val cache = ConcurrentHashMap<String, Record>()

    /** Which rows have been asked at all — a miss is an answer, and is kept. */
    private val asked: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    /**
     * The row's record, from the doors, or null when every one of them was empty
     * (or fetching is off, or the row has not been asked yet and cannot be).
     */
    suspend fun record(entry: IncursionEntry): Record? = withContext(Dispatchers.IO) {
        if (!AppPreferences.coverFetchEnabledState) return@withContext null
        val key = entry.storageKey
        cache[key]?.let { remembered -> return@withContext remembered.takeIf { !it.isEmpty } }
        if (key in asked) return@withContext null
        val name = stripNaming(entry.title)
        if (name.isBlank()) return@withContext null
        val isSeries = entry.type.lowercase() == "series"

        // ── Stage one: the keyed pair, which also states the facts ──────────
        val keyed = race(
            { tmdb(name) },
            { omdb(name, entry.year, isSeries) }
        )
        // ── Stage two: the keyless net, reached only when stage one was empty ─
        val answer = keyed ?: race(
            { wikipedia(name, entry.year) },
            { comicVine(name) }
        )

        // The doors above are asked for a DESCRIPTION first; the facts come with
        // whichever one answered, and a door that had text but no facts still
        // leaves a usable record (an empty one is remembered as empty, which is
        // what stops a reopening sheet re-running the whole chain).
        val resolved = answer ?: Record()
        asked += key
        cache[key] = resolved
        resolved.takeIf { !it.isEmpty }
    }

    /**
     * LAST-RESORT ARTWORK for a row, from the three doors [IncursionPosters] does
     * not already ask: OMDb's poster (a real poster, at Amazon's large size),
     * Wikipedia's lead image, and Comic Vine's cover.
     *
     * Raced, for the same reason the text is: these are the doors BEHIND the
     * app's own keyless chain, so a row that reaches them is a row nothing else
     * could answer, and three more sequential waits is how a cover never arrives.
     */
    suspend fun artwork(entry: IncursionEntry): String? = withContext(Dispatchers.IO) {
        if (!AppPreferences.coverFetchEnabledState) return@withContext null
        val name = stripNaming(entry.title)
        if (name.isBlank()) return@withContext null
        val isSeries = entry.type.lowercase() == "series"
        race(
            { OmdbFetch.posterUrl(name, entry.year, isSeries) },
            { wikipediaImage(name, entry.year) },
            { ComicVineFetch.movie(name)?.coverUrl?.takeIf { it.isNotBlank() } }
        )
    }

    // ── the doors ───────────────────────────────────────────────────────────

    /** TMDB's read, as this app's own record. Null when TMDB has nothing (or no key). */
    private suspend fun tmdb(name: String): Record? {
        val facts = TmdbFetch.facts(name) ?: return null
        val found = Record(
            description = facts.overview.trim(),
            rating = facts.rating,
            runtime = facts.runtime,
            genres = facts.genres
        )
        return found.takeIf { !it.isEmpty }
    }

    /** OMDb's read. Empty fields stay empty; a title OMDb never heard of is null. */
    private suspend fun omdb(name: String, year: Int?, isSeries: Boolean): Record? {
        val facts = OmdbFetch.facts(name, year, isSeries) ?: return null
        val found = Record(
            description = facts.plot,
            rating = facts.rating,
            runtime = facts.runtime,
            genres = facts.genres,
            rated = facts.rated
        )
        return found.takeIf { !it.isEmpty }
    }

    /**
     * A Wikipedia article's summary, as a description.
     *
     * The lookup itself lives in [WikipediaSummary], shared with the shelf's
     * book enrichment — an article's prose is the same question on both
     * surfaces, and two copies of "search, then pick the best hit" is how one of
     * them ends up matching the wrong work. [WikipediaSummary.Kind.FILM] is what
     * makes the article about the FILM win rather than the one about the comic it
     * was adapted from.
     */
    private fun wikipedia(name: String, year: Int?): Record? {
        val extract = WikipediaSummary.extract(name, year, WikipediaSummary.Kind.FILM)
            ?: return null
        return Record(description = extract)
    }

    /** A Wikipedia article's lead image, for a row nothing else could dress. */
    private fun wikipediaImage(name: String, year: Int?): String? =
        WikipediaSummary.leadImage(name, year, WikipediaSummary.Kind.FILM)

    /** Comic Vine's film record, as a description (and the key for its artwork). */
    private fun comicVine(name: String): Record? {
        val movie = ComicVineFetch.movie(name) ?: return null
        val found = Record(description = movie.description)
        return found.takeIf { !it.isEmpty }
    }

    // ── shared ──────────────────────────────────────────────────────────────

    /**
     * THE FIRST NON-NULL ANSWER, in the order the doors are given — ALL OF THEM
     * ASKED AT ONCE.
     *
     * Order is preference, not sequence: the doors are started together, so the
     * wait is the slowest door's, never the sum, and the first one in the list
     * that HAS something wins even when a later door answered sooner. Every door
     * is bounded by [DOOR_BUDGET_MS] and every door is allowed to throw: a
     * provider's failure is never this chain's failure.
     */
    private suspend fun <T> race(vararg doors: suspend () -> T?): T? = coroutineScope {
        val jobs = doors.map { door ->
            async {
                runCatching { withTimeoutOrNull(DOOR_BUDGET_MS) { door() } }.getOrNull()
            }
        }
        jobs.map { it.await() }.firstOrNull { it != null }
    }

    /**
     * How long ONE door may take before it is treated as a door with nothing to
     * say. Six seconds, chosen against the member's own measurement of the old
     * chain (24s inside one title) and against every door's own read timeout:
     * long enough for a slow mobile answer, short enough that a dead door cannot
     * hold a sheet.
     */
    private const val DOOR_BUDGET_MS = 6_000L
}

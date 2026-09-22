package com.curio.app.features.reveal

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * v455 — THE KEYLESS DOOR FOR A FILM'S OWN FACTS.
 *
 * The member, twice: *"the movies doesnt load, without tmdb or odbmdb key, do
 * something about it please"* and, before that, *"movies doesnt even fetch ever
 * only series does"*. They are describing the same hole, and it is a structural
 * one rather than a bug in any single call site:
 *
 *  · A **series** has a keyless door that answers everything — TVMaze states a
 *    show's name, its art, its episode list, its runtime and its rating, no key
 *    asked for.
 *  · A **film** had nothing of the sort. Its keyless doors were ART ONLY
 *    ([FilmPosterFetch]'s Wikipedia lead image) and one article's prose
 *    ([WikipediaSummary]); every fact a film's sheet wants — the plot that is
 *    more than a teaser, the runtime, the rating, the genres, the director, the
 *    cast — came from TMDB or OMDb, both KEYED. So a build with no credential
 *    could show a film's poster and nothing about the film, while a series beside
 *    it was fully dressed ("only series does").
 *
 * This file closes it, with two keyless providers and no key anywhere:
 *
 *  1. **Wikipedia** ([WikipediaSummary]) decides WHICH WORK is meant — a search
 *     whose bracket rule keeps the novel or the comic from answering for the film
 *     — and then states it in prose (the article's own summary).
 *  2. **Wikidata** states the FACTS. A film's Wikidata item carries the runtime
 *     (`P2047`), the genres (`P136`), the director (`P57`), the cast (`P161`), the
 *     review score (`P444`), the publication date (`P577`), whether it is a film
 *     or a show (`P31`) and, when it has one, an image of its own (`P18`). It is
 *     run by the Wikimedia Foundation, needs no account and no key, and is the
 *     same provenance as the article beside it.
 *
 * WHY THIS IS NOT "ANOTHER FALLBACK THAT TAKES TWENTY SECONDS". Four reads make a
 * record here — the article (memoised by [WikipediaSummary], shared with the
 * poster door and the Incursion prose), the page's item id, the item itself, and
 * ONE batched read for the labels of every referenced item. The whole thing is
 * bounded by [FACTS_BUDGET_MS], every read is on the short Wikimedia budget, and
 * every answer — a miss included — is remembered for the life of the process, so
 * a title is paid for once and never again.
 *
 * The rules it keeps are the project's own:
 *  - **Keyless, so it sits IN FRONT of the keyed pair.** It is not an upgrade; it
 *    is the door that exists when nothing else does (`IncursionSources` asks it in
 *    its keyless stage, and the film sheet asks it directly).
 *  - **A failure is not an answer.** Every read and every parse is wrapped, and a
 *    null means "ask the next door", never "show nothing".
 *  - **Nothing is asked twice.** Answers AND misses are memoised per title+year.
 *  - **No non-local returns.** Every early exit is written as a branch, because a
 *    `return` out of one of the inline `runCatching` lambdas below becomes the
 *    compiler's `$$$$$NON_LOCAL_RETURN$$$$$` helper class, whose method name R8
 *    refuses to dex — fine in debug, dead in release (the project's known trap).
 */
internal object WikidataFilmFetch {

    /**
     * A film (or a show) as the keyless pair states it. Every field may be empty
     * — no single provider has all of them, and a field nobody had is ABSENT
     * rather than zeroed, so a caller draws what is there and nothing else.
     */
    internal data class Facts(
        val title: String = "",
        val year: String = "",
        val posterUrl: String = "",
        val plot: String = "",
        /** Minutes; 0 when neither provider stated one. */
        val runtime: Int = 0,
        /** A 0–10 score, the scale Wikidata's `P444` carries for films. */
        val rating: Float = 0f,
        val genres: List<String> = emptyList(),
        val director: String = "",
        val cast: List<String> = emptyList(),
        /** True when Wikidata classes this item as a television work. */
        val isShow: Boolean = false
    ) {
        val isEmpty: Boolean
            get() = posterUrl.isBlank() && plot.isBlank() && runtime <= 0 &&
                rating <= 0f && genres.isEmpty() && director.isBlank() && cast.isEmpty()

        /** The one-line fact row a card draws: `2010 · 148 min · ★ 8.8 · Sci-Fi`. */
        val factLine: String
            get() = buildList {
                if (year.isNotBlank()) add(year)
                if (runtime > 0) add("$runtime min")
                if (rating > 0f) add("\u2605 ${"%.1f".format(rating)}")
                addAll(genres.take(2))
            }.joinToString("  \u00b7  ")
    }

    /** `title|year → Facts` (a miss is held in [absent], never re-asked). */
    private val cache = ConcurrentHashMap<String, Facts>()

    /** The titles already asked with nothing to show — kept apart because a map cannot hold a null. */
    private val absent: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    /** Whether this door is available at all — it always is, because it is keyless. */
    val isConfigured: Boolean get() = true

    /** The work's keyless record, or null when the pair has nothing. */
    suspend fun facts(title: String, year: Int? = null): Facts? = withContext(Dispatchers.IO) {
        val name = stripNaming(title).ifBlank { title.trim() }
        if (name.isBlank()) return@withContext null
        val key = "$name|${year ?: 0}"
        cache[key]?.let { return@withContext it }
        if (key in absent) return@withContext null
        val resolved = runCatching { withTimeoutOrNull(FACTS_BUDGET_MS) { read(name, year) } }.getOrNull()
        if (resolved == null || resolved.isEmpty) {
            absent += key
            null
        } else {
            cache[key] = resolved
            resolved
        }
    }

    /** The work's poster, or null. */
    suspend fun posterUrl(title: String, year: Int? = null): String? =
        facts(title, year)?.posterUrl?.takeIf { it.isNotBlank() }

    /**
     * The work's long description, or null.
     *
     * Asked even when nothing else about the title could be read: the article's
     * prose is the one thing this door can state on its own, and a surface that
     * only wants a synopsis should not pay for the item read behind it.
     */
    suspend fun plot(title: String, year: Int? = null): String? {
        val name = stripNaming(title).ifBlank { title.trim() }
        if (name.isBlank()) return null
        return facts(name, year)?.plot?.takeIf { it.isNotBlank() }
            ?: WikipediaSummary.extract(name, year, WikipediaSummary.Kind.FILM)
    }

    /** Whether Wikidata classes a title as a show rather than a film. */
    suspend fun isShow(title: String, year: Int? = null): Boolean =
        facts(title, year)?.isShow ?: false

    // ── the two reads ───────────────────────────────────────────────────────

    /**
     * The work, from the article and the item.
     *
     * The article is asked for FIRST because it is the door that decides WHICH
     * work is meant — "Dune" is a novel, a film and a comic, and the bracket rule
     * in [WikipediaSummary] is what stops the wrong one answering — and because
     * its readability scores are the app's own answer to "did we find the right
     * thing". The item id comes from that page, so Wikidata is never searched by
     * name: a name search on an item store is exactly how a film's facts land on
     * the television series of the same name.
     */
    private fun read(name: String, year: Int?): Facts? {
        val page = WikipediaSummary.page(name, year, WikipediaSummary.Kind.FILM) ?: return null
        val itemId = itemIdOf(page) ?: return null
        val entity = entityOf(itemId) ?: return null
        return parse(entity, page, name, year)
    }

    /** The Wikidata item behind a Wikipedia article (`Q25169`), or null. */
    private fun itemIdOf(page: String): String? {
        val body = httpGet(
            "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json" +
                "&redirects=1&titles=${Uri.encode(page)}"
        ) ?: return null
        return runCatching {
            val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages")
                ?: return@runCatching ""
            var found = ""
            val keys = pages.keys()
            while (keys.hasNext() && found.isBlank()) {
                val row = pages.optJSONObject(keys.next()) ?: continue
                found = row.optJSONObject("pageprops")
                    ?.optString("wikibase_item")
                    .orEmpty()
                    .trim()
            }
            found
        }.getOrDefault("").takeIf { it.startsWith("Q") }
    }

    /** The item itself, from Wikidata's own fact dump for one entity. */
    private fun entityOf(itemId: String): JSONObject? {
        val body = httpGet("https://www.wikidata.org/wiki/Special:EntityData/$itemId.json")
            ?: return null
        return runCatching {
            JSONObject(body).optJSONObject("entities")?.optJSONObject(itemId)
        }.getOrNull()
    }

    // ── the parse ───────────────────────────────────────────────────────────

    private fun parse(entity: JSONObject, page: String, name: String, yearHint: Int?): Facts? {
        val claims = entity.optJSONObject("claims") ?: return null
        val itemIds = claims.itemIds("P136", 3) +
            claims.itemIds("P57", 2) +
            claims.itemIds("P161", 6)
        // ONE batched request for every referenced item's English label, rather
        // than one per name: a film's cast alone is six items, and six reads
        // inside a budgeted fallback is the arithmetic that made the old TMDB
        // chain take 24 seconds (the project's own measured report).
        val labels = labelMap(itemIds)
        val duration = claims.quantity("P2047")
        val year = claims.year("P577")
            .ifBlank { yearHint?.toString().orEmpty() }
        val image = claims.firstString("P18")
        val poster = WikipediaSummary.leadImage(name, yearHint, WikipediaSummary.Kind.FILM)
            ?: commonsImage(image)
        return Facts(
            title = entity.optJSONObject("labels")?.optJSONObject("en")?.optString("value")
                .orEmpty()
                .ifBlank { page },
            year = year,
            posterUrl = poster.orEmpty(),
            // The article's prose, which is the description a member can read;
            // the item's own "description" field is a four-word disambiguator.
            plot = WikipediaSummary.extract(name, yearHint, WikipediaSummary.Kind.FILM).orEmpty(),
            runtime = duration ?: 0,
            rating = claims.quantity("P444")?.toFloat() ?: 0f,
            genres = claims.itemIds("P136", 3).mapNotNull { labels[it] },
            director = claims.itemIds("P57", 2).mapNotNull { labels[it] }.joinToString(", "),
            cast = claims.itemIds("P161", 6).mapNotNull { labels[it] },
            isShow = claims.itemIds("P31", 12).any { it in SHOW_TYPES }
        )
    }

    /**
     * Every referenced item's English label in ONE read, or an empty map.
     *
     * A missing label is dropped rather than guessed: a QID is not a name, and
     * printing one would be worse than printing nothing.
     */
    private fun labelMap(itemIds: List<String>): Map<String, String> {
        val wanted = itemIds.distinct().take(16)
        if (wanted.isEmpty()) return emptyMap<String, String>()
        val body = httpGet(
            "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json" +
                "&props=labels&languages=en&ids=${wanted.joinToString("|")}"
        ) ?: return emptyMap()
        return runCatching {
            val entities = JSONObject(body).optJSONObject("entities")
                ?: return@runCatching emptyMap<String, String>()
            val out = LinkedHashMap<String, String>()
            for (id in wanted) {
                val label = entities.optJSONObject(id)
                    ?.optJSONObject("labels")
                    ?.optJSONObject("en")
                    ?.optString("value")
                    .orEmpty()
                    .trim()
                if (label.isNotBlank()) out[id] = label
            }
            out
        }.getOrDefault(emptyMap())
    }

    // ── claim readers ───────────────────────────────────────────────────────

    /**
     * The item ids a property states, best-effort, in statement order.
     *
     * Deprecated ranks are skipped where Wikidata marks them, because a
     * superseded value is exactly the kind of fact a member would notice as
     * wrong. (A rank is absent far more often than not; its absence is normal.)
     */
    private fun JSONObject.itemIds(property: String, limit: Int): List<String> {
        val statements = optJSONArray(property) ?: return emptyList()
        val out = ArrayList<String>(limit)
        for (index in 0 until statements.length()) {
            if (out.size >= limit) break
            val row = statements.optJSONObject(index) ?: continue
            if (row.optString("rank").equals("deprecated", ignoreCase = true)) continue
            val value = row.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.optJSONObject("value")
                ?: continue
            val id = value.optString("id").trim()
            if (id.startsWith("Q")) out += id
        }
        return out
    }

    /**
     * A quantity claim as an Int — the minute a runtime is stated in.
     *
     * Wikidata times are stated in SECONDS as often as in minutes (`P2047` unit
     * `Q11574` rather than `Q7727`), and a 8,880-second runtime printed as
     * "8880 min" is a fact that is worse than none — so the unit decides whether
     * the number is divided.
     */
    private fun JSONObject.quantity(property: String): Int? {
        val value = optJSONArray(property)?.let { statements ->
            (0 until statements.length())
                .mapNotNull { statements.optJSONObject(it) }
                .firstOrNull { !it.optString("rank").equals("deprecated", ignoreCase = true) }
                ?.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.optJSONObject("value")
        } ?: return null
        val amount = value.optString("amount").trim().removePrefix("+").toDoubleOrNull() ?: return null
        val unit = value.optString("unit")
        val minutes = if (unit.endsWith("Q11574")) amount / 60.0 else amount
        return minutes.toInt().takeIf { it > 0 }
    }

    /** A time claim's year (`+2010-07-16T00:00:00Z` → `2010`), or "". */
    private fun JSONObject.year(property: String): String {
        val statements = optJSONArray(property) ?: return ""
        for (index in 0 until statements.length()) {
            val row = statements.optJSONObject(index) ?: continue
            if (row.optString("rank").equals("deprecated", ignoreCase = true)) continue
            val time = row.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.optJSONObject("value")
                ?.optString("time")
                .orEmpty()
            val digits = Regex("""(-?\d{4})""").find(time)?.groupValues?.get(1).orEmpty()
            // A BCE date ("-0350-…") is not a year this app prints.
            if (digits.length == 4) return digits
        }
        return ""
    }

    /** A string claim — the Commons file name `P18` carries, or "". */
    private fun JSONObject.firstString(property: String): String {
        val statements = optJSONArray(property) ?: return ""
        for (index in 0 until statements.length()) {
            val row = statements.optJSONObject(index) ?: continue
            if (row.optString("rank").equals("deprecated", ignoreCase = true)) continue
            val text = row.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.optString("value")
                .orEmpty()
                .trim()
            if (text.isNotBlank()) return text
        }
        return ""
    }

    /**
     * A Commons file name as a served image URL.
     *
     * `Special:FilePath` is Wikimedia's own stable redirect to a file's bytes,
     * and its `width` parameter is the thumbnail service — the same shape the
     * poster door builds by hand, kept as a documented URL instead.
     */
    private fun commonsImage(file: String): String? {
        val name = file.trim().takeIf { it.isNotBlank() } ?: return null
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" +
            Uri.encode(name.replace(' ', '_')) + "?width=500"
    }

    // ── shared ──────────────────────────────────────────────────────────────

    /**
     * One GET, best-effort, on the SHORT Wikimedia budget this door shares with
     * every other fallback in the app.
     *
     * Written as a branch rather than `if (…) return null` on purpose — see the
     * note in the file header about R8 and the non-local-return helper.
     */
    private fun httpGet(url: String): String? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 3_500
            conn.readTimeout = 5_000
            conn.setRequestProperty("Accept", "application/json")
            // Wikimedia serves a named client; an anonymous one is throttled.
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /** Wikimedia asks every client to name itself. */
    private const val USER_AGENT = "Curio/1.0 (https://github.com/firefly-sylestia/Curio)"

    /**
     * How long one title's whole record may take, all four reads included.
     *
     * Nine seconds: the same ceiling the keyed pair is held to (see
     * `TmdbFetch.FACTS_BUDGET_MS`), chosen because a door the member is waiting on
     * must never be the reason a sheet sits empty — and because this one is
     * asked in FRONT of the keyed doors, so it must fail fast enough for them to
     * have their turn when it does.
     */
    private const val FACTS_BUDGET_MS = 9_000L

    /**
     * The `instance of` classes that mean "this is a show, not a film".
     *
     * The list is the television branch of Wikidata's own film/TV tree: series,
     * programmes, miniseries, web series, anime series and television films. A
     * title classes as both (a TV film that aired theatrically) stays a FILM
     * only when none of these is stated, which is the safe direction — a film's
     * facts are still a film's facts.
     */
    private val SHOW_TYPES = setOf(
        "Q5398426", // television series
        "Q15416", // television program
        "Q1259759", // miniseries
        "Q526877", // web series
        "Q63952888", // anime television series
        "Q506240", // television film
        "Q581714" // animated series
    )
}

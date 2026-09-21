package com.curio.app.features.reveal

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * v429 — WIKIPEDIA, AS ONE DOOR FOR EVERY SURFACE THAT NEEDS A DESCRIPTION.
 *
 * It arrived twice in the same batch: an Incursion row's synopsis (*"use all the
 * avalabel api"*) and a shelf book's about-text (*"if nothing returns use more
 * fallbacks"*). Both want the same two things from the same place — a page's own
 * summary prose, and its lead image — so it lives here, once, and every caller
 * goes through it. Two copies of "search, then read the REST summary, then pick
 * the best hit" is how one of them ends up matching the wrong article.
 *
 * WHY IT IS A GOOD FALLBACK, in one line: a FILM, a SHOW and a BOOK that anyone
 * has heard of has an article, while a catalogue answers a title only when it
 * files it under exactly the name the app is holding — so this is the door that
 * fixes a NAME, which is what most misses actually are.
 *
 * Two rules it keeps, the project's own:
 *
 *  - **A disambiguation page is not an answer.** Its summary is a list of other
 *    things; the type is checked and such a page is refused.
 *  - **Nothing is asked twice.** Both the page it settled on and the answer are
 *    remembered per query, misses included, so no surface re-runs a search.
 */
internal object WikipediaSummary {

    /** What kind of work is being looked for — it decides which article wins. */
    internal enum class Kind { ANY, FILM, BOOK, ART }

    private val pageCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val textCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val imageCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** The article's summary prose, or null when the door has nothing. */
    internal fun extract(title: String, year: Int? = null, kind: Kind = Kind.ANY): String? {
        val page = page(title, year, kind) ?: return null
        val key = "$page|$kind"
        textCache[key]?.let { return it.ifEmpty { null } }
        val text = summary(page)?.optString("extract")?.trim().orEmpty()
        textCache[key] = text
        return text.ifEmpty { null }
    }

    /** The article's lead image, or null. */
    internal fun leadImage(title: String, year: Int? = null, kind: Kind = Kind.ANY): String? {
        val page = page(title, year, kind) ?: return null
        val key = "$page|img"
        imageCache[key]?.let { return it.ifEmpty { null } }
        val row = summary(page)
        // ── v429 — THE THUMBNAIL FIRST, AND THAT IS A CHOICE ABOUT BYTES ────
        // Every caller of this door draws a ROW or a card (an Incursion plate of
        // 94×140dp, a shelf tile), never a full-screen view, so the REST
        // summary's ~320px thumbnail is already more pixels than the target and
        // the full-resolution original is several megabytes of a member's data
        // for detail nothing displays. The original is the fallback for a page
        // whose summary carries no thumbnail at all.
        val source = row?.optJSONObject("thumbnail")?.optString("source").orEmpty()
            .ifBlank { row?.optJSONObject("originalimage")?.optString("source").orEmpty() }
        val usable = source.takeIf { it.startsWith("http", ignoreCase = true) }.orEmpty()
        imageCache[key] = usable
        return usable.ifEmpty { null }
    }

    /**
     * THE ARTICLE FOR A TITLE: a search, then the best hit by name.
     *
     * A search rather than the "go straight to the title" shortcut, because a
     * work's article is almost never at the bare title — it is at
     * "Inception (film)", "Loki (TV series)", "Dune (novel)" — and the
     * disambiguator is a fact about Wikipedia's filing, not about the work. The
     * year separates two articles of the same name, and [kind] decides which
     * bracket is the RIGHT one: a film must not be answered with the novel it was
     * adapted from, and a book must not be answered with the film made of it.
     */
    internal fun page(title: String, year: Int? = null, kind: Kind = Kind.ANY): String? {
        val wanted = title.trim()
        if (wanted.isBlank()) return null
        val cacheKey = "$wanted|${year ?: 0}|$kind"
        pageCache[cacheKey]?.let { return it.ifEmpty { null } }
        val query = if (year != null && year > 0) "$wanted $year" else wanted
        val body = httpGet(
            "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json" +
                "&srlimit=8&srsearch=${Uri.encode(query)}"
        )
        val results = body?.let {
            runCatching { JSONObject(it).optJSONObject("query")?.optJSONArray("search") }.getOrNull()
        }
        val lower = wanted.lowercase()
        var best: String? = null
        var bestScore = 0
        if (results != null) {
            for (index in 0 until results.length()) {
                val row = results.optJSONObject(index) ?: continue
                val name = row.optString("title").trim()
                if (name.isBlank()) continue
                val plain = name.substringBefore(" (").trim().lowercase()
                var score = when {
                    plain == lower -> 4
                    plain.startsWith(lower) -> 3
                    lower.startsWith(plain) -> 2
                    else -> 0
                }
                val bracket = name.substringAfter(" (", "").lowercase()
                if (bracket.isNotBlank() && bracketMatches(bracket, kind)) score += 2
                if (year != null && year > 0 && name.contains(year.toString())) score += 2
                if (score > bestScore) {
                    bestScore = score
                    best = name
                }
            }
        }
        val resolved = best.takeIf { bestScore > 0 }.orEmpty()
        pageCache[cacheKey] = resolved
        return resolved.ifEmpty { null }
    }

    /** Whether a disambiguator is the right KIND of work. */
    private fun bracketMatches(bracket: String, kind: Kind): Boolean = when (kind) {
        Kind.ANY -> true
        Kind.FILM -> FILM_WORDS.any { bracket.contains(it) }
        Kind.BOOK -> BOOK_WORDS.any { bracket.contains(it) }
        Kind.ART -> ART_WORDS.any { bracket.contains(it) }
    }

    /** A page's REST summary, or null (a disambiguation page is refused). */
    private fun summary(page: String): JSONObject? {
        val body = httpGet(
            "https://en.wikipedia.org/api/rest_v1/page/summary/${Uri.encode(page.replace(' ', '_'))}"
        ) ?: return null
        val row = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (row.optString("type").equals("disambiguation", ignoreCase = true)) return null
        return row
    }

    /**
     * One GET, best-effort, on a fallback's own SHORT budget — this door is always
     * behind something else, so it may never be the reason a screen waits.
     *
     * Written as a branch rather than `if (…) return null`: a return out of this
     * inline lambda is a non-local return, whose compiler-generated
     * `$$$$$NON_LOCAL_RETURN$$$$$` helper class R8 refuses to dex (fine in debug,
     * dead in release — the project's own known trap).
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

    private val FILM_WORDS = listOf("film", "series", "television", "miniseries", "sitcom")
    private val BOOK_WORDS = listOf("novel", "book", "novella", "memoir", "biography", "comic")
    private val ART_WORDS = listOf("painting", "artwork", "sculpture", "portrait", "fresco", "mural")

    /** Wikimedia asks every client to name itself. */
    private const val USER_AGENT = "Curio/1.0 (https://github.com/firefly-sylestia/Curio)"
}

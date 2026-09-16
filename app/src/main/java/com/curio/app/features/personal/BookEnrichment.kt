package com.curio.app.features.personal

import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalChapter
import com.curio.app.data.PersonalChapterCodec
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * THE SHELF'S AUTO-FETCH — what Curio knows about a book, filled in for the
 * member instead of asked of them.
 *
 * A book added from Curio's own lane arrives complete (chapter list, page
 * count, synopsis). One added from Open Library — or typed in by hand — used
 * to arrive as a title with nothing under it: every chapter row said "Chapter
 * 7" and the member had to say how long the book was themselves. This object
 * closes those gaps:
 *
 *  1. THE APP'S OWN CATALOG FIRST. ~800 curated books with real chapter
 *     lists, page counts and synopses, offline and instant. An exact,
 *     punctuation-insensitive title match binds the shelf row to the catalog
 *     book (its `catalogId`), which is also what lets the topic page's book
 *     sheet and the shelf share one set of chapter notes.
 *  2. OPEN LIBRARY, for a book the catalog does not have: the edition's real
 *     TABLE OF CONTENTS (chapter names, and the page range each one spans when
 *     the edition gives it) and the median page count — but only while the
 *     member's book-fetch consent is on.
 *
 * Everything here is best-effort: a failure leaves the book exactly as it was,
 * and nothing is fetched at all without consent.
 */
internal object BookEnrichment {

    /**
     * What ONE pass over a book learned, so the book's page can say it out
     * loud. "Look it up" used to be a pill that visibly did nothing: the pass
     * ran, found nothing it could add, and reported nothing back. Now every
     * door it opened is named in [learned], and a pass that found nothing
     * while fetching is OFF says so ([needsConsent]) instead of sitting there.
     */
    internal data class EnrichReport(
        val book: PersonalBookEntity,
        /** "12 chapters", "416 pages", "the description" … in pass order. */
        val learned: List<String>,
        val needsConsent: Boolean
    ) {
        /** True when the row has to be written back. */
        val changed: Boolean get() = learned.isNotEmpty()
    }

    /**
     * Fills in what [book] is missing, trying EVERY source: the app's own
     * catalog first, then Open Library's table of contents, page count and
     * description. A catalog match no longer ends the pass — a book Curio
     * knows can still be missing its page count or an about-text.
     */
    suspend fun enrich(book: PersonalBookEntity): EnrichReport {
        val learned = mutableListOf<String>()
        var updated = book

        catalogMatch(updated)?.let { matched ->
            updated = matched
            learned += "the catalog's own record"
        }
        if (updated.chaptersJson.isBlank()) {
            openLibraryChapters(updated.title, updated.author)?.let { chapters ->
                updated = updated.copy(
                    chaptersJson = PersonalChapterCodec.encode(chapters),
                    totalChapters = if (updated.totalChapters <= 0) chapters.size
                    else updated.totalChapters
                )
                learned += "${chapters.size} chapters"
            }
        }
        if (updated.pageCount <= 0) {
            openLibraryPages(updated.title, updated.author)?.let { pages ->
                updated = updated.copy(pageCount = pages)
                learned += "$pages pages"
            }
        }
        // A catalog book's about-text is the catalog's own (read live by the
        // book's page), so only a book the catalog does not have asks Open
        // Library for a description.
        if (updated.catalogId.isBlank() && updated.synopsis.isBlank()) {
            openLibraryDescription(updated.title, updated.author)?.let { text ->
                updated = updated.copy(synopsis = text)
                learned += "the description"
            }
        }

        return EnrichReport(
            book = updated,
            learned = learned,
            needsConsent = learned.isEmpty() &&
                updated.catalogId.isBlank() &&
                !AppPreferences.bookFetchEnabledState
        )
    }

    /**
     * The work's DESCRIPTION — Open Library's own about-text, the one its
     * book pages show (the member asked which of the two the app uses: the
     * catalog's synopsis when Curio has the book, this otherwise). It lives on
     * the WORK, is sometimes an object (`{ "value": … }`) and is occasionally
     * missing entirely, in which case the first sentence stands in. Null when
     * consent is off, nothing matched by title, or the text is too short to be
     * a description at all.
     */
    suspend fun openLibraryDescription(title: String, author: String): String? {
        if (title.isBlank()) return null
        if (!AppPreferences.bookFetchEnabledState) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val wanted = normalise(title)
                val search = getJson(searchUrl(title, author, 5, "key,title"))
                    ?: return@runCatching null
                val match = search.asJsonObject.array("docs")
                    .mapNotNull { it as? JsonObject }
                    .firstOrNull {
                        normalise(it.str("title")) == wanted &&
                            it.str("key").startsWith("/works/")
                    }
                    ?: return@runCatching null
                val work = getJson("https://openlibrary.org${match.str("key")}.json")
                    ?.let { runCatching { it.asJsonObject }.getOrNull() }
                    ?: return@runCatching null
                val description = work.descriptionText().ifBlank { work.str("first_sentence") }
                description.trim().takeIf { it.length >= MIN_DESCRIPTION }
            }.getOrNull()
        }
    }

    /** `description`, which Open Library writes as a string OR an object. */
    private fun JsonObject.descriptionText(): String {
        val value = get("description") ?: return ""
        if (value.isJsonNull) return ""
        return runCatching {
            if (value.isJsonObject) {
                value.asJsonObject.get("value")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            } else {
                value.asString
            }
        }.getOrDefault("")
    }

    /**
     * Binds a hand-added book to the catalog's own record of it (its topic id,
     * chapters, pages, cover and author). Null when the catalog does not have
     * the book, or when the row is already bound.
     */
    suspend fun catalogMatch(book: PersonalBookEntity): PersonalBookEntity? {
        if (book.catalogId.isNotBlank()) return null
        val hit = BookCatalog.bestMatch(book.title, book.author) ?: return null
        return book.copy(
            catalogId = hit.topicId,
            totalChapters = if (book.totalChapters <= 0) hit.chapterCount else book.totalChapters,
            pageCount = if (book.pageCount <= 0) hit.pageCount else book.pageCount,
            coverUrl = book.coverUrl.ifBlank { hit.coverUrl },
            author = book.author.ifBlank { hit.author }
        )
    }

    /**
     * The book's chapter list, read out of Open Library's table of contents.
     *
     * Open Library keeps a ToC per EDITION, not per work, and most editions
     * have none — so this looks up the work by title (the edition's title must
     * match, or the result is a different book's contents) and then walks its
     * editions for the first real table: fewer than three rows is a
     * "Contents" line, not a chapter list. Null when nothing usable was found,
     * when consent is off, or when the catalogue could not be reached.
     */
    suspend fun openLibraryChapters(title: String, author: String): List<PersonalChapter>? {
        if (title.isBlank()) return null
        if (!AppPreferences.bookFetchEnabledState) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val wanted = normalise(title)
                val search = getJson(
                    searchUrl(title, author, 5, "key,title,number_of_pages_median")
                ) ?: return@runCatching null
                val docs = search.asJsonObject.array("docs")
                val match = docs.mapNotNull { it as? JsonObject }
                    .firstOrNull { normalise(it.str("title")) == wanted && it.str("key").isNotBlank() }
                    ?: return@runCatching null
                val editions = getJson(
                    "https://openlibrary.org${match.str("key")}/editions.json?limit=25"
                )?.let { runCatching { it.asJsonObject }.getOrNull() }?.array("entries").orEmpty()
                editions
                    .mapNotNull { entry -> (entry as? JsonObject)?.tableOfContents() }
                    .firstOrNull { it.size >= MIN_CHAPTERS }
                    ?.mapIndexed { index, chapter -> chapter.copy(number = index + 1) }
            }.getOrNull()
        }
    }

    /** One edition's table of contents as chapters (numbers assigned later). */
    private fun JsonObject.tableOfContents(): List<PersonalChapter>? {
        val rows = array("table_of_contents")
        if (rows.isEmpty()) return null
        val chapters = rows.mapNotNull { row ->
            val entry = row as? JsonObject ?: return@mapNotNull null
            val name = entry.str("title")
            if (name.isBlank()) return@mapNotNull null
            val (start, end) = pagesOf(entry.str("pagination"))
            PersonalChapter(number = 0, title = name, pageStart = start, pageEnd = end)
        }
        return chapters.takeIf { it.isNotEmpty() }
    }

    /** \"121-154\" / \"121\" / \"\" → the pages a chapter spans. */
    private fun pagesOf(pagination: String): Pair<Int, Int> {
        val numbers = Regex("\\d+").findAll(pagination).map { it.value.toInt() }.toList()
        return when {
            numbers.isEmpty() -> 0 to 0
            numbers.size == 1 -> numbers[0] to numbers[0]
            else -> numbers.min() to numbers.max()
        }
    }

    /**
     * Open Library's page count for a title. Null when the consent toggle is
     * off (no network is touched at all), when the catalogue could not be
     * reached, or when it does not know — the caller then simply leaves the
     * book alone.
     */
    private suspend fun openLibraryPages(title: String, author: String): Int? {
        if (title.isBlank()) return null
        if (!AppPreferences.bookFetchEnabledState) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val search = getJson(
                    searchUrl(title, author, 3, "title,number_of_pages_median")
                ) ?: return@runCatching null
                search.asJsonObject.array("docs")
                    .firstOrNull()
                    ?.asJsonObject
                    ?.get("number_of_pages_median")
                    ?.takeIf { !it.isJsonNull }
                    ?.asInt
                    ?.takeIf { it > 0 }
            }.getOrNull()
        }
    }

    /** One Open Library title search, encoded once for every caller. */
    private fun searchUrl(
        title: String,
        author: String,
        limit: Int,
        fields: String
    ): String = buildString {
        append("https://openlibrary.org/search.json?title=")
        append(java.net.URLEncoder.encode(title, "UTF-8"))
        if (author.isNotBlank()) {
            append("&author=")
            append(java.net.URLEncoder.encode(author, "UTF-8"))
        }
        append("&limit=$limit&fields=$fields")
    }

    private fun getJson(url: String): com.google.gson.JsonElement? = runCatching {
        val request = Request.Builder().url(url).get().build()
        val body = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use ""
            response.body?.string().orEmpty()
        }
        if (body.isBlank()) null else JsonParser.parseString(body)
    }.getOrNull()

    /** Letters and digits only, lowercased — the title match's own rule. */
    private fun normalise(value: String): String =
        value.lowercase().filter { it.isLetterOrDigit() }

    private fun JsonObject.str(key: String): String {
        val value = get(key) ?: return ""
        return if (value.isJsonNull) "" else runCatching { value.asString }.getOrDefault("")
    }

    /** A JSON array read as a list; an absent or odd value reads as empty. */
    private fun JsonObject.array(key: String): List<com.google.gson.JsonElement> =
        runCatching { getAsJsonArray(key)?.toList() }.getOrNull().orEmpty()

    /** Three rows is the floor for a real chapter list. */
    private const val MIN_CHAPTERS = 3

    /** Below this many characters it is a tagline, not a description. */
    private const val MIN_DESCRIPTION = 60

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}

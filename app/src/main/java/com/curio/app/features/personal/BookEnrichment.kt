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
     * Fills in what [book] is missing. Returns the updated row, or null when
     * there was nothing to learn (so the caller writes nothing).
     */
    suspend fun enrich(book: PersonalBookEntity): PersonalBookEntity? {
        catalogMatch(book)?.let { return it }
        var updated = book
        if (book.chaptersJson.isBlank()) {
            openLibraryChapters(book.title, book.author)?.let { chapters ->
                updated = updated.copy(
                    chaptersJson = PersonalChapterCodec.encode(chapters),
                    totalChapters = if (updated.totalChapters <= 0) chapters.size
                    else updated.totalChapters
                )
            }
        }
        if (updated.pageCount <= 0) {
            openLibraryPages(book.title, book.author)?.let { pages ->
                updated = updated.copy(pageCount = pages)
            }
        }
        return updated.takeIf { it != book }
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
                    buildString {
                        append("https://openlibrary.org/search.json?title=")
                        append(java.net.URLEncoder.encode(title, "UTF-8"))
                        if (author.isNotBlank()) {
                            append("&author=")
                            append(java.net.URLEncoder.encode(author, "UTF-8"))
                        }
                        append("&limit=5&fields=key,title,number_of_pages_median")
                    }
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
                    buildString {
                        append("https://openlibrary.org/search.json?title=")
                        append(java.net.URLEncoder.encode(title, "UTF-8"))
                        if (author.isNotBlank()) {
                            append("&author=")
                            append(java.net.URLEncoder.encode(author, "UTF-8"))
                        }
                        append("&limit=3&fields=title,number_of_pages_median")
                    }
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

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}

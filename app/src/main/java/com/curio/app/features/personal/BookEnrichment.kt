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
 *  2. OPEN LIBRARY, for a book the catalog does not have: a real TABLE OF
 *     CONTENTS (chapter names, and the page range each one spans when the
 *     edition gives it) and the median page count — but only while the
 *     member's book-fetch consent is on. Both places Open Library keeps one
 *     are read: the editions' own, richest table first, and the work's.
 *  3. GOOGLE BOOKS, then CROSSREF, when Open Library has no table at all.
 *     Google Books carries a volume's `tableOfContents` for a good many titles;
 *     Crossref registers every chapter of an academic or edited book as its own
 *     DOI with the pages it spans, which is the only one of the three that
 *     answers for a volume compiled from contributions. All three are keyless,
 *     and all three are held to the same floor: fewer than three titled rows is
 *     not a chapter list.
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
            // v389e — FREE FIRST, KEYS LAST.
            //
            // Open Library and Crossref both answer for nothing, and between
            // them they cover a table of contents someone typed AND an academic
            // book's chapters registered as DOIs. Google Books used to sit in the
            // middle of that list, and it is the one source that FAILS without a
            // key: the anonymous endpoint answers 429 ("Quota exceeded … Queries
            // per day", checked live from this repo), so every chapter lookup on a
            // build with no key spent a request on a source that could not answer
            // (user report: "the book chapters still doesnt fetch i am sure
            // googlebooks api doesnt work"). It is asked LAST, and only when a key
            // is actually configured (see googleBooksChapters).
            val chapters = openLibraryChapters(updated.title, updated.author)
                ?: crossrefChapters(updated.title, updated.author)
                ?: googleBooksChapters(updated.title, updated.author)
            chapters?.let { list ->
                updated = updated.copy(
                    chaptersJson = PersonalChapterCodec.encode(list),
                    totalChapters = if (updated.totalChapters <= 0) list.size
                    else updated.totalChapters
                )
                learned += "${list.size} chapters"
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
                    "https://openlibrary.org${match.str("key")}/editions.json?limit=50"
                )?.let { runCatching { it.asJsonObject }.getOrNull() }?.array("entries").orEmpty()
                // v389d — THE RICHEST TABLE WINS. This used to take the FIRST
                // edition whose table had three rows or more, and an edition's
                // table is often a bare "Contents" stub while a sibling edition
                // carries the book's real chapter list. Every candidate is kept
                // and the fullest one is used, with a table that has page ranges
                // preferred over one that has only names.
                val tables = editions
                    .mapNotNull { entry -> (entry as? JsonObject)?.tableOfContents() }
                    .filter { it.size >= MIN_CHAPTERS }
                val best = tables.maxByOrNull { table ->
                    table.count { it.pageStart > 0 } * 100 + table.size
                }
                // v389d — AND THE WORK ITSELF. Open Library keeps a table of
                // contents on the WORK as well as on its editions, and for a
                // good many records that is the only one anyone entered. Asked
                // for only when the editions had nothing, so the common case
                // costs no extra request.
                val fromWork = if (best == null) {
                    getJson("https://openlibrary.org${match.str("key")}.json")
                        ?.let { runCatching { it.asJsonObject }.getOrNull() }
                        ?.tableOfContents()
                        ?.takeIf { it.size >= MIN_CHAPTERS }
                } else {
                    null
                }
                (best ?: fromWork)
                    ?.mapIndexed { index, chapter -> chapter.copy(number = index + 1) }
            }.getOrNull()
        }
    }

    /**
     * v389d — THE THIRD SOURCE: CROSSREF, for the books that keep their chapters
     * in an academic record rather than in a table of contents page.
     *
     * Crossref registers every chapter of an edited or academic book as its own
     * DOI, with the pages it spans, which is a chapter list nobody had to type
     * into a table of contents. Two keyless calls, both in Crossref's free public
     * pool:
     *
     *  1. the BOOK, so the chapter query can be scoped to it — a bare
     *     `type:book-chapter` search would mix every other book's chapters in;
     *  2. its chapters, as `type:book-chapter,container-title:<the book>`,
     *     ordered by the page each one starts on.
     *
     * Null unless a book matches by title AND returns at least [MIN_CHAPTERS]
     * chapters that actually have titles — the same floor the other two sources
     * are held to.
     */
    private suspend fun crossrefChapters(
        title: String,
        author: String
    ): List<PersonalChapter>? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null
        runCatching {
            val wanted = normalise(title)
            val bookQuery = buildString {
                append("https://api.crossref.org/works?rows=3&filter=type:book")
                append("&select=title,container-title")
                append("&query.bibliographic=")
                append(java.net.URLEncoder.encode("$title $author".trim(), "UTF-8"))
            }
            val bookJson = getJson(bookQuery)?.let { runCatching { it.asJsonObject }.getOrNull() }
                ?: return@runCatching null
            val items = bookJson.getAsJsonObject("message")?.array("items").orEmpty()
            // The container the chapters are registered under — the book's own
            // title AS CROSSREF HAS IT, because that exact string is what the
            // filter has to match. Only a title that normalises to the one being
            // looked up may stand in for it, or the chapters of a different book
            // would be read as this one's.
            val container = items
                .mapNotNull { it as? JsonObject }
                .mapNotNull { doc ->
                    doc.array("title").firstOrNull()
                        ?.let { value -> runCatching { value.asString }.getOrNull() }
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }
                .firstOrNull { normalise(it) == wanted }
                ?: return@runCatching null

            val chaptersUrl = buildString {
                append("https://api.crossref.org/works?rows=100&select=title,page")
                append("&filter=type:book-chapter,container-title:")
                append(java.net.URLEncoder.encode(container, "UTF-8"))
            }
            val chaptersJson = getJson(chaptersUrl)
                ?.let { runCatching { it.asJsonObject }.getOrNull() }
                ?: return@runCatching null
            val rows = chaptersJson.getAsJsonObject("message")?.array("items").orEmpty()
            rows.mapNotNull { row ->
                val entry = row as? JsonObject ?: return@mapNotNull null
                val name = entry.array("title").firstOrNull()
                    ?.let { runCatching { it.asString }.getOrNull() }
                    ?.trim()
                    .orEmpty()
                if (name.isBlank()) return@mapNotNull null
                val (start, end) = pagesOf(entry.str("page"))
                PersonalChapter(number = 0, title = name, pageStart = start, pageEnd = end)
            }
                .takeIf { it.size >= MIN_CHAPTERS }
                // Registration order is not reading order: the page each
                // chapter starts on is.
                ?.sortedBy { if (it.pageStart > 0) it.pageStart else Int.MAX_VALUE }
                ?.mapIndexed { index, chapter -> chapter.copy(number = index + 1) }
        }.getOrNull()
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

    /**
     * v392 — GOOGLE BOOKS CHAPTER FALLBACK.
     *
     * When Open Library has no table of contents for a book, Google Books
     * sometimes does (its `volumeInfo.tableOfContents`).
     *
     * v389e — AND IT NEEDS THE KEY NOW, on purpose. The anonymous endpoint is a
     * shared daily quota, it is very often spent (a live check from this repo
     * answered `429 Quota exceeded … Queries per day` for the keyless URL), and a
     * request that is answered with an error is worse than no request: it costs
     * the member a wait and looks like a broken feature. So this asks only when
     * [com.curio.app.BuildConfig.GOOGLE_BOOKS_API_KEY] is set, and the answer no
     * longer depends on a quota nobody controls.
     */
    private suspend fun googleBooksChapters(
        title: String, author: String
    ): List<PersonalChapter>? {
        if (title.isBlank()) return null
        if (com.curio.app.BuildConfig.GOOGLE_BOOKS_API_KEY.isBlank()) return null
        if (!AppPreferences.bookFetchEnabledState) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val q = buildString {
                    append("intitle:")
                    append(java.net.URLEncoder.encode(title, "UTF-8"))
                    if (author.isNotBlank()) {
                        append("+inauthor:")
                        append(java.net.URLEncoder.encode(author, "UTF-8"))
                    }
                }
                val key = com.curio.app.BuildConfig.GOOGLE_BOOKS_API_KEY
                val url = "https://www.googleapis.com/books/v1/volumes?q=$q&maxResults=3" +
                    "&key=$key"
                val json = getJson(url) ?: return@runCatching null
                val items = json.asJsonObject.array("items")
                for (item in items) {
                    val vol = (item as? JsonObject)?.getAsJsonObject("volumeInfo")
                        ?: continue
                    val toc = vol.getAsJsonArray("tableOfContents")
                        ?: continue
                    val chapters = toc.mapNotNull { entry ->
                        val name = runCatching { entry.asString }.getOrDefault("")
                        if (name.isBlank()) null
                        else PersonalChapter(
                            number = 0,
                            title = name,
                            pageStart = 0,
                            pageEnd = 0
                        )
                    }
                    if (chapters.size >= MIN_CHAPTERS) {
                        return@runCatching chapters.mapIndexed { i, ch ->
                            ch.copy(number = i + 1)
                        }
                    }
                }
                null
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

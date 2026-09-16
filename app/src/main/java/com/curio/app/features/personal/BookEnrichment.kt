package com.curio.app.features.personal

import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalBookEntity
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
 * to arrive as a title with nothing under it, and every one of those gaps had
 * to be closed by the member. This object closes them:
 *
 *  1. THE APP'S OWN CATALOG FIRST. ~800 curated books with real chapter
 *     lists, page counts and synopses, offline and instant. An exact,
 *     punctuation-insensitive title match binds the shelf row to the catalog
 *     book (its `catalogId`), which is also what lets the topic page's book
 *     sheet and the shelf share one set of chapter notes.
 *  2. OPEN LIBRARY, only for a page count the catalog does not have, and only
 *     while the member's book-fetch consent is on.
 *
 * Everything here is best-effort: a failure leaves the book exactly as it was.
 */
internal object BookEnrichment {

    /**
     * Fills in what [book] is missing. Returns the updated row, or null when
     * there was nothing to learn (so the caller writes nothing).
     */
    suspend fun enrich(book: PersonalBookEntity): PersonalBookEntity? {
        catalogMatch(book)?.let { return it }
        if (book.pageCount > 0) return null
        val pages = openLibraryPages(book.title, book.author) ?: return null
        return book.copy(pageCount = pages)
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
                val url = buildString {
                    append("https://openlibrary.org/search.json?title=")
                    append(java.net.URLEncoder.encode(title, "UTF-8"))
                    if (author.isNotBlank()) {
                        append("&author=")
                        append(java.net.URLEncoder.encode(author, "UTF-8"))
                    }
                    append("&limit=3&fields=title,author_name,number_of_pages_median")
                }
                val request = Request.Builder().url(url).get().build()
                val body = http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use ""
                    response.body?.string().orEmpty()
                }
                if (body.isBlank()) return@runCatching null
                val docs = JsonParser.parseString(body).asJsonObject
                    .getAsJsonArray("docs") ?: return@runCatching null
                docs.firstOrNull()
                    ?.asJsonObject
                    ?.get("number_of_pages_median")
                    ?.takeIf { !it.isJsonNull }
                    ?.asInt
                    ?.takeIf { it > 0 }
            }.getOrNull()
        }
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}

package com.curio.app.features.settings

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.TopicJsonLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

private typealias BookTopic = com.curio.app.data.CurioTopic

/**
 * v320 — the book-cover hub engine. The reveal's poster still prefers the
 * topic's OWN `imageUrl` and falls back to Open Library's title cover; this
 * object powers the Settings hub: MULTIPLE providers (user-selectable), a
 * one-by-one bulk fetch that RECORDS WHICH BOOKS FAILED (persisted, so
 * "Retry failed" survives restarts), and — without any API key — a Google
 * Books RATINGS fetch (averageRating from the keyless JSON endpoint).
 */
object BookCoverFetch {

    val TITLE = "Book covers & ratings"

    /** Cover providers the hub offers. Open Library = the reveal's own
     *  title-cover fallback; Google Books = keyless volume search. */
    enum class BookCoverProvider(val label: String, val description: String) {
        OPEN_LIBRARY("Open Library", "Title covers · keyless"),
        GOOGLE_BOOKS("Google Books", "Keyless title+author search")
    }

    /** Resolves cover URL candidates for a book. v352 — the last RESOLVED
     *  URL from the hub (e.g. a Google Books thumbnail) sits between the
     *  authored imageUrl and the bare Open Library title fallback, so covers
     *  the hub actually found show up on the reveal + share card too. */
    fun coverCandidates(bookName: String, imageUrl: String): List<String> =
        listOfNotNull(
            imageUrl.takeIf { it.isNotBlank() },
            AppPreferences.bookCoverUrlsState[bookName]?.takeIf { it.isNotBlank() },
            "https://covers.openlibrary.org/b/title/${Uri.encode(bookName)}-M.jpg",
        ).distinct()

    /** Single-URL convenience for bulk fetch (uses first candidate). */
    fun coverUrlFor(bookName: String, imageUrl: String): String =
        coverCandidates(bookName, imageUrl).firstOrNull() ?: ""

    /**
     * Resolve ONE book's cover URL with the given provider (the topic's OWN
     * imageUrl always wins — it's an authored, curated cover). Google Books
     * needs a look-up; Open Library is the pure title fallback.
     */
    suspend fun resolveCoverUrl(
        context: Context,
        bookName: String,
        author: String?,
        imageUrl: String,
        provider: BookCoverProvider
    ): String? = withContext(Dispatchers.IO) {
        val resolved = imageUrl.takeIf { it.isNotBlank() }
            ?: when (provider) {
                BookCoverProvider.OPEN_LIBRARY ->
                    "https://covers.openlibrary.org/b/title/${Uri.encode(bookName)}-M.jpg"
                BookCoverProvider.GOOGLE_BOOKS -> googleThumbnail(bookName, author)
            }
        // v352 — remember what the hub actually resolved so the reveal poster
        // (and the share card) can reuse it instead of re-guessing.
        if (resolved != null) AppPreferences.setBookCoverUrl(context, bookName, resolved)
        resolved
    }

    /** Keyless Google Books volume search → the first match's cover thumbnail. */
    private fun googleThumbnail(title: String, author: String?): String? {
        val q = buildString {
            append("intitle:${Uri.encode(title)}")
            if (!author.isNullOrBlank()) append("+inauthor:${Uri.encode(author)}")
        }
        val json = httpGet("https://www.googleapis.com/books/v1/volumes?q=$q&maxResults=3")
            ?: return null
        return runCatching {
            val items = org.json.JSONObject(json).optJSONArray("items") ?: return null
            for (i in 0 until items.length()) {
                val vi = items.optJSONObject(i)?.optJSONObject("volumeInfo") ?: continue
                val img = vi.optJSONObject("imageLinks")?.optString("thumbnail") ?: continue
                return img.replace("http://", "https://")
            }
            null
        }.getOrNull()
    }

    /**
     * Fetch every unique book cover into the shared Coil disk cache, one by
     * one. [onProgress] fires per book with (done, total, failed). Books
     * whose cover could NOT be fetched are persisted to the prefs failed
     * list so the hub can retry just them later. When [onlyFailed] is true,
     * only the previously-failed books are retried and successes are dropped
     * from the failed list.
     */
    suspend fun fetchAll(
        context: Context,
        provider: BookCoverProvider,
        onlyFailed: Boolean = false,
        onProgress: (done: Int, total: Int, failed: Int) -> Unit
    ) {
        val books = withContext(Dispatchers.Default) {
            runCatching { TopicJsonLoader.load(CategoryId.BOOKS) }.getOrNull().orEmpty()
        }
        val failedBefore = AppPreferences.bookCoverFailedState.toMutableSet()
        var targets: List<BookTopic> = books
        if (onlyFailed) {
            targets = books.filter { it.name in failedBefore }
            if (targets.isEmpty()) {
                onProgress(0, 0, 0)
                AppPreferences.setBookCoverFailed(context, emptyList())
                return
            }
        }
        val loader = Coil.imageLoader(context)
        val total = targets.size
        var done = 0
        var failed = 0
        val stillFailed = mutableListOf<String>()
        onProgress(0, total, 0)
        for (book in targets) {
            val started = SystemClock.elapsedRealtime()
            val url = resolveCoverUrl(context, book.name, book.byline, book.imageUrl, provider)
            val ok = if (url == null) false else runCatching {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        // Bulk pass: skip the memory cache (300 decoded covers
                        // would bloat heap) — the disk cache is what we're
                        // filling; the reveal re-decodes from it on demand.
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .listener(
                            onSuccess = { _, _ -> cont.resume(true) },
                            onError = { _, _ -> cont.resume(false) }
                        )
                        .build()
                    val disposable = loader.enqueue(request)
                    cont.invokeOnCancellation { disposable.dispose() }
                }
            }.getOrDefault(false)
            if (ok) {
                done++
                failedBefore.remove(book.name)
            } else {
                failed++
                stillFailed.add(book.name)
            }
            onProgress(done, total, failed)
            val took = SystemClock.elapsedRealtime() - started
            if (took < 150L) delay(150L - took)
        }
        AppPreferences.setBookCoverFailed(
            context,
            if (onlyFailed) stillFailed else failedBefore.toList() + stillFailed
        )
    }

    /**
     * Keyless Google Books RATINGS fetch: for every book, query
     * "intitle:<name> inauthor:<byline>" and store the first hit's
     * averageRating. [onProgress] fires per book with (done, total).
     */
    suspend fun fetchRatings(
        context: Context,
        onProgress: (done: Int, total: Int) -> Unit
    ) {
        val books = withContext(Dispatchers.Default) {
            runCatching { TopicJsonLoader.load(CategoryId.BOOKS) }.getOrNull().orEmpty()
        }
        val total = books.size
        if (total == 0) { onProgress(0, 0); return }
        var done = 0
        onProgress(0, total)
        val cachedRatings = AppPreferences.getBookRatings(context)
        for (book in books) {
            // v334 — already-fetched books are SKIPPED (counted, not re-queried):
            // a ratings fetch resumes where the last one stopped instead of
            // restarting from the beginning and hammering the quota.
            if (cachedRatings.containsKey(book.name)) {
                done++
                onProgress(done, total)
                continue
            }
            // v328 — cache BOTH stars: the average rating AND its Google
            // Books ratingsCount, so the reveal can show "★ 4.2 · 1.2k".
            val rating = runCatching {
                val q = buildString {
                    append("intitle:${Uri.encode(book.name)}")
                    if (!book.byline.isNullOrBlank()) append("+inauthor:${Uri.encode(book.byline)}")
                }
                val json = httpGet("https://www.googleapis.com/books/v1/volumes?q=$q&maxResults=3")
                json?.let {
                    val items = org.json.JSONObject(it).optJSONArray("items") ?: return@runCatching null
                    for (i in 0 until items.length()) {
                        val vi = items.optJSONObject(i)?.optJSONObject("volumeInfo") ?: continue
                        // v352 — CONTINUE past hits without a rating instead of
                        // aborting: the first Google Books match is often a
                        // preview-only volume with no averageRating, while a
                        // later match has one.
                        if (vi.has("averageRating")) {
                            val r = vi.optDouble("averageRating", 0.0)
                            val c = vi.optInt("ratingsCount", 0)
                            if (r > 0.0) {
                                AppPreferences.setBookRatingWithCount(context, book.name, r, c)
                                return@runCatching r
                            }
                        }
                    }
                    null
                }
            }.getOrNull()
            done++
            onProgress(done, total)
            delay(120L)  // stay inside the keyless quota
        }
    }

    /** One book's Google Books star data (average + ratings count). */
    data class BookStars(val average: Double, val count: Int)

    /**
     * Keyless Google Books rating for ONE book (v327 — powers the reveal
     * page's on-demand star chip; the reveal fetches this when a book opens
     * and no cached rating exists yet). Null when the query finds nothing.
     * v328 — returns BOTH stars ([BookStars]) so callers can cache the
     * ratings count alongside the average.
     */
    suspend fun fetchRatingFor(bookName: String, author: String?): BookStars? =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = buildString {
                    append("intitle:${Uri.encode(bookName)}")
                    if (!author.isNullOrBlank()) append("+inauthor:${Uri.encode(author)}")
                }
                val json = httpGet("https://www.googleapis.com/books/v1/volumes?q=$q&maxResults=3")
                json?.let {
                    val items = org.json.JSONObject(it).optJSONArray("items") ?: return@runCatching null
                    for (i in 0 until items.length()) {
                        val vi = items.optJSONObject(i)?.optJSONObject("volumeInfo") ?: continue
                        // v352 — CONTINUE past hits without a rating instead of
                        // aborting (first Google Books match is often a
                        // preview-only volume with no averageRating).
                        if (vi.has("averageRating")) {
                            val r = vi.optDouble("averageRating", 0.0)
                            if (r > 0.0) {
                                return@runCatching BookStars(
                                    average = r,
                                    count = vi.optInt("ratingsCount", 0)
                                )
                            }
                        }
                    }
                    null
                }
            }.getOrNull()
        }

    /** Minimal keyless GET — 8s timeout, best-effort. */
    private fun httpGet(urlString: String): String? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Curio/1.0")
            val code = conn.responseCode
            if (code != 200) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}
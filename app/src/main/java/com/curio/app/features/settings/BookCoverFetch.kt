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

    /** Cover providers the hub offers, ordered BEST-FIRST (v356): iTunes is
     *  the default keyless ebook search; Google Books is the keyless volume
     *  search; Open Library is the pure title-cover fallback; LibraryThing
     *  needs a free key (LIBRARY_THING_API_KEY) and resolves covers via ISBN. */
    enum class BookCoverProvider(val label: String, val description: String) {
        ITUNES("iTunes", "Keyless ebook search"),
        GOOGLE_BOOKS("Google Books", "Keyless title+author search"),
        OPEN_LIBRARY("Open Library", "Title covers · keyless"),
        LIBRARY_THING("LibraryThing", "ISBN covers · free key")
    }

    /** Resolves cover URL candidates for a book. v352/v356 — the last
     *  RESOLVED URL from the hub (whatever provider found it — iTunes,
     *  Google Books or LibraryThing) sits between the authored imageUrl and
     *  the bare Open Library title fallback, so covers the hub actually
     *  found show up on the reveal + share card too. */
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
                BookCoverProvider.ITUNES -> itunesThumbnail(bookName, author)
                BookCoverProvider.GOOGLE_BOOKS -> googleThumbnail(bookName, author)
                BookCoverProvider.OPEN_LIBRARY ->
                    "https://covers.openlibrary.org/b/title/${Uri.encode(bookName)}-M.jpg"
                BookCoverProvider.LIBRARY_THING -> libraryThingCover(bookName, author)
            }
        // v352 — remember what the hub actually resolved so the reveal poster
        // (and the share card) can reuse it instead of re-guessing.
        if (resolved != null) AppPreferences.setBookCoverUrl(context, bookName, resolved)
        resolved
    }

    /** v354 — Google Books volumes endpoint. Stays fully KEYLESS unless the
     *  optional GOOGLE_BOOKS_API_KEY BuildConfig value is set (free tier,
     *  higher daily quota), in which case &key= is appended. */
    private fun googleBooksUrl(q: String): String {
        val key = com.curio.app.BuildConfig.GOOGLE_BOOKS_API_KEY
            .takeIf { it.isNotBlank() }
        return "https://www.googleapis.com/books/v1/volumes?q=$q&maxResults=3" +
            (key?.let { "&key=$it" } ?: "")
    }

    /** Keyless (or keyed, v354) Google Books volume search → the first
     *  match's cover thumbnail. */
    private fun googleThumbnail(title: String, author: String?): String? {
        val q = buildString {
            append("intitle:${Uri.encode(title)}")
            if (!author.isNullOrBlank()) append("+inauthor:${Uri.encode(author)}")
        }
        val json = httpGet(googleBooksUrl(q))
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
     * v356 — iTunes Search API, keyless ebook search (the same provider the
     * album/series posters already use). The 100px `artworkUrl100` token is
     * upscaled to 600px for the poster. Picks the best title+author match
     * (exact beats fuzzy), so a wrong-edition result doesn't win.
     */
    private fun itunesThumbnail(title: String, author: String?): String? {
        val term = buildString {
            append(Uri.encode(title.trim()))
            if (!author.isNullOrBlank()) append("+").append(Uri.encode(author.trim()))
        }
        val json = httpGet("https://itunes.apple.com/search?term=$term&entity=ebook&limit=10")
            ?: return null
        return runCatching {
            val results = org.json.JSONObject(json).optJSONArray("results") ?: return null
            var best: String? = null
            var bestScore = 0
            for (i in 0 until results.length()) {
                val r = results.optJSONObject(i) ?: continue
                val name = r.optString("trackName")
                val art = r.optString("artworkUrl100")
                if (art.isBlank()) continue
                val score = matchScore(name, title, r.optString("artistName"), author)
                if (score > bestScore) {
                    bestScore = score
                    best = art
                }
                if (score >= 3) break
            }
            best
                ?.replace("100x100bb", "600x600bb")
                ?.replace("http://", "https://")
        }.getOrNull()
    }

    /**
     * v356 — LibraryThing covers (the best quality for ISBN-resolvable
     * books), but ISBN-based and key-gated: covers.librarything.com/devkey/
     * {key}/large/isbn/{isbn}. Resolution is a keyless Google Books volume
     * search for the ISBN, then the cover URL. Returns null when no key is
     * configured or no ISBN can be found, so the caller falls through to the
     * next provider (and the hub hides the row entirely without a key).
     */
    private fun libraryThingCover(title: String, author: String?): String? {
        val key = com.curio.app.BuildConfig.LIBRARY_THING_API_KEY
            .takeIf { it.isNotBlank() } ?: return null
        val isbn = resolveIsbn(title, author) ?: return null
        return "https://covers.librarything.com/devkey/$key/large/isbn/$isbn"
    }

    /** First ISBN (ISBN-13 preferred) from a keyless Google Books search. */
    private fun resolveIsbn(title: String, author: String?): String? {
        val q = buildString {
            append("intitle:${Uri.encode(title)}")
            if (!author.isNullOrBlank()) append("+inauthor:${Uri.encode(author)}")
        }
        val json = httpGet(googleBooksUrl(q)) ?: return null
        return runCatching {
            val items = org.json.JSONObject(json).optJSONArray("items") ?: return null
            for (i in 0 until items.length()) {
                val vi = items.optJSONObject(i)?.optJSONObject("volumeInfo") ?: continue
                val ids = vi.optJSONArray("industryIdentifiers") ?: continue
                for (j in 0 until ids.length()) {
                    val id = ids.optJSONObject(j) ?: continue
                    val type = id.optString("type")
                    val value = id.optString("identifier").trim()
                    if (value.isNotEmpty() && (type == "ISBN_13" || type == "ISBN_10")) {
                        return value
                    }
                }
            }
            null
        }.getOrNull()
    }

    /**
     * Rough relevance score (same heuristic as the album resolver): 3 = exact
     * title AND author, 2 = exact title (or fuzzy title + exact author),
     * 1 = fuzzy containment / shared word, 0 = no match.
     */
    private fun matchScore(name: String, wantName: String, author: String, wantAuthor: String?): Int {
        val n = name.trim()
        val w = wantName.trim()
        val titleExact = n.equals(w, ignoreCase = true)
        val titleFuzzy = !w.isBlank() && (n.contains(w, ignoreCase = true) ||
            w.contains(n, ignoreCase = true) ||
            titleWordsOverlap(n, w))
        val authorExact = !wantAuthor.isNullOrBlank() &&
            author.trim().equals(wantAuthor.trim(), ignoreCase = true)
        return when {
            titleExact && authorExact -> 3
            titleExact -> 2
            titleFuzzy && authorExact -> 2
            titleFuzzy -> 1
            else -> 0
        }
    }

    /** True when the two titles share a meaningful (≥4 char) word. */
    private fun titleWordsOverlap(a: String, b: String): Boolean {
        val wa = a.split(Regex("[^A-Za-z0-9]+")).filter { it.length >= 4 }.map { it.lowercase() }.toSet()
        val wb = b.split(Regex("[^A-Za-z0-9]+")).filter { it.length >= 4 }.map { it.lowercase() }.toSet()
        return wa.any { it in wb }
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
                val json = httpGet(googleBooksUrl(q))
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
                val json = httpGet(googleBooksUrl(q))
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
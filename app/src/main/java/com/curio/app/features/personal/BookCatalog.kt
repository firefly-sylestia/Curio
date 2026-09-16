package com.curio.app.features.personal

import com.curio.app.data.BookChapter
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicJsonLoader

/**
 * THE APP'S OWN BOOK CATALOG — the `BOOKS` lane, read as a library.
 *
 * Curio already ships ~800 curated books: title, author, cover, page count, a
 * genre and — the part that matters here — a REAL chapter list, each chapter
 * carrying its own name, its page range and a one-line summary. Adding a book
 * from the shelf used to ignore all of it and go straight to Open Library, which
 * meant a member's chapter rows said "Chapter 7" while the app was holding
 * "Chapter 7 — The Breaking of the Truce, pp. 121–154" in its own assets.
 *
 * So the shelf searches THIS first (instant, offline, and genuinely part of
 * Curio), and Open Library is the fallback for anything the catalog does not
 * have. A book picked from the catalog keeps its `topicId`, which is how the
 * book's page can read the chapter list back.
 *
 * The list is parsed once per process and the search runs over a haystack built
 * at the same time, so a keystroke is a scan of ~800 short strings rather than a
 * re-walk of the JSON.
 */
internal object BookCatalog {

    /** One book from the app's catalog, shaped for the shelf's add flow. */
    data class Hit(
        val topicId: String,
        val title: String,
        val author: String,
        val coverUrl: String,
        val pageCount: Int,
        val genre: String,
        val chapters: List<BookChapter>,
        /** Title + author + genre + tags + teaser, lowercased once. */
        val haystack: String
    ) {
        val chapterCount: Int get() = chapters.size
    }

    @Volatile
    private var cache: List<Hit>? = null

    /**
     * The whole catalog, parsed on first use. Null when the assets could not be
     * read at all (a build shipped without its topic JSON) — the caller then
     * simply falls back to Open Library.
     */
    suspend fun library(): List<Hit>? {
        cache?.let { return it }
        val loaded = runCatching {
            TopicJsonLoader.load(CategoryId.BOOKS).map { it.toHit() }
        }.getOrNull() ?: return null
        cache = loaded
        return loaded
    }

    /**
     * Books matching [query], best first.
     *
     * The ranking is the one the topic picker uses, for the same reason: what a
     * person types is almost always the START of a title ("wuther", "the
     * ili"), so a title-prefix match beats a stray word match anywhere else in
     * the record. Shortest title first inside a rank, because a search for
     * "dune" should offer *Dune* before *Dune Messiah*.
     */
    suspend fun search(query: String, limit: Int = 6): List<Hit> {
        val all = library() ?: return emptyList()
        val needle = query.trim().lowercase().removePrefix("@")
        if (needle.isEmpty()) return emptyList()
        return all.asSequence()
            .mapNotNull { hit -> rank(hit, needle)?.let { it to hit } }
            .sortedWith(
                compareByDescending<Pair<Int, Hit>> { it.first }
                    .thenBy { it.second.title.length }
            )
            .take(limit)
            .map { it.second }
            .toList()
    }

    /** The chapter list of one catalog book, in order. */
    suspend fun chapters(topicId: String): List<BookChapter> =
        cache?.firstOrNull { it.topicId == topicId }?.chapters
            ?: library()?.firstOrNull { it.topicId == topicId }?.chapters.orEmpty()

    /** One catalog book by its topic id. */
    suspend fun book(topicId: String): Hit? =
        cache?.firstOrNull { it.topicId == topicId }
            ?: library()?.firstOrNull { it.topicId == topicId }

    // ── ranking ─────────────────────────────────────────────────────────────

    private fun rank(hit: Hit, needle: String): Int? {
        val title = hit.title.lowercase()
        val author = hit.author.lowercase()
        return when {
            title == needle -> 120
            title.startsWith(needle) -> 100
            title.split(' ', '-', ':', ',').any { it.startsWith(needle) } -> 80
            title.contains(needle) -> 55
            author.startsWith(needle) || author.contains(" $needle") -> 45
            author.contains(needle) -> 35
            hit.genre.lowercase().contains(needle) -> 20
            hit.haystack.contains(needle) -> 10
            else -> null
        }
    }

    private fun CurioTopic.toHit(): Hit {
        val tags = tags.joinToString(" ")
        return Hit(
            topicId = id,
            title = name.trim(),
            author = byline.trim(),
            coverUrl = imageUrl.trim(),
            pageCount = pageCount ?: 0,
            genre = subtype.trim(),
            chapters = chapters.orEmpty(),
            haystack = buildString {
                append(name).append(' ')
                append(byline).append(' ')
                append(subtype).append(' ')
                append(tags).append(' ')
                append(teaser)
            }.lowercase()
        )
    }
}

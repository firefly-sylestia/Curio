package com.curio.app.features.personal

import android.content.Context
import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.features.cabinet.CabinetCoverCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v410 — A SHELF BOOK'S REAL COVER, FETCHED ONCE AND KEPT.
 *
 * A book added from Curio's own lane can arrive with everything but its cover
 * (see the reveal's shelf bridge), and a book typed in or imported from a file
 * arrives with nothing at all. Either way the shelf, the book page, the
 * Cabinet's personal shelf and Home all draw the same code-generated plate —
 * a title on a tint — for a book whose real artwork ONE search would return
 * (member report: "the book covers are not loading in the book page now … all
 * of it my book shelf mainly it should fetch").
 *
 * So the resolution lives in one place, on the machinery the Cabinet already
 * uses for its liked covers ([CabinetCoverCache]): the catalogue's own curated
 * URL for a book Curio knows (or the row's own, v512 — see [ensureCover]),
 * otherwise the provider cascade (iTunes Search, then Open Library's title
 * cover), and a URL is only ever written onto the book's row once its BYTES
 * have actually arrived — a dead placeholder is never remembered as the
 * answer, which is what left covers blank for good.
 *
 * Nothing here reaches the network with cover fetching switched off in
 * Settings; the catalogue's own cover is local, so it is still adopted.
 */
internal object BookCoverWarmup {

    /**
     * Resolves [book]'s cover art and remembers it on the book's row.
     *
     * Returns the URL that delivered an image (or the catalogue's own URL when
     * fetching is off), or null when nothing could be resolved — in which case
     * the row is left exactly as it was and the generated cover keeps its
     * place.
     *
     * ── v512 — A BOOK THAT ALREADY HAS A URL IS WARMED TOO ──────────────────
     *
     * The member: *"do a double check on source as some books covers isnt showing
     * up instantly"*. The cause was this guard: an early `return` on
     * `book.coverUrl.isNotBlank()` meant a book that arrived WITH a cover URL (the
     * add sheet's iTunes hit, the reveal's shelf bridge) never had its BYTES put on
     * disk — only books that came in blank were warmed. Every plate for those books
     * therefore fell back to `coverUrl` and let Coil fetch it in the layout, so the
     * cover arrived a beat after the card, again on a cold cache, and never became
     * the instant local file that [CabinetCoverCache] exists to provide.
     *
     * So the row's own URL is now the warmer's `authoredUrl` just like the
     * catalogue's, the cascade still advances past it if it is dead, and the only
     * short-circuit left is the one that matters: a cover whose bytes are already on
     * disk returns without touching the network (and returns the URL, so a caller
     * can still adopt it).
     */
    suspend fun ensureCover(context: Context, book: PersonalBookEntity): String? {
        if (book.title.isBlank()) return null
        val kind = CabinetCoverCache.CoverKind.BOOK
        // The bytes are the answer: nothing to fetch, nothing to resolve.
        CabinetCoverCache.localCoverFile(context, kind, book.title)?.let {
            return CabinetCoverCache.persistedUrl(context, kind, book.title)
                ?: book.coverUrl.takeIf { url -> url.isNotBlank() }
        }
        val catalogCover = catalogCover(book.catalogId)
        if (!AppPreferences.coverFetchEnabledState) {
            // Fetching is off: adopt the catalogue's own cover when the app
            // already knows this book, and otherwise touch no network at all.
            catalogCover?.let { write(book.id, it) }
            return catalogCover
        }
        val file = CabinetCoverCache.ensureLocalCover(
            context = context,
            kind = kind,
            name = book.title,
            byline = book.author.takeIf { it.isNotBlank() },
            authoredUrl = book.coverUrl.takeIf { it.isNotBlank() } ?: catalogCover,
            // The CALLER bumps the cache version once for the whole batch (a
            // per-cover bump recomposes every version-keyed plate per save) —
            // the Cabinet's own warmer rule, v3xx37.
            bumpVersion = false,
        )
        val url = CabinetCoverCache.persistedUrl(context, kind, book.title)
        if (file == null || url.isNullOrBlank()) return null
        CabinetCoverCache.warmDominantColor(context, kind, book.title)
        write(book.id, url)
        return url
    }

    /** The catalogue's own cover for a book the app already knows. */
    private suspend fun catalogCover(catalogId: String): String? {
        if (catalogId.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching { BookCatalog.book(catalogId)?.coverUrl }.getOrNull()
        }?.takeIf { it.isNotBlank() }
    }

    /** Writes the resolved URL onto the book's row (a column-scoped update). */
    private suspend fun write(bookId: String, url: String) {
        withContext(Dispatchers.IO) {
            runCatching { PersonalRepositoryHolder.repo.setCoverUrl(bookId, url) }
        }
    }
}

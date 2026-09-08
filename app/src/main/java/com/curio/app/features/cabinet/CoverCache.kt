package com.curio.app.features.cabinet

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import com.curio.app.data.AppPreferences
import com.curio.app.features.reveal.AlbumArtFetch
import com.curio.app.features.reveal.SeriesPosterFetch
import com.curio.app.features.settings.BookCoverFetch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * v3xx — the CABINET COVER CACHE: a separate, always-on store for the cover
 * art of LIKED / SAVED books, albums and series.
 *
 *  - The resolved cover URL is persisted per topic (reusing the reveal's own
 *    `bookCoverUrlsState` / `sheetArtUrlsState` slots, so a cover picked here
 *    is the SAME cover the reveal sheets and share cards reuse).
 *  - The IMAGE BYTES are downloaded once and saved under
 *    `filesDir/cover_cache/<kind>-<name>.img`, so a liked item's cover opens
 *    INSTANTLY on every later visit — no network, no Coil miss, "the images
 *    stay cached".
 *  - Provider fallback: each kind resolves through its provider cascade
 *    (books: iTunes → Open Library title; albums: iTunes → MusicBrainz;
 *    series: TVMaze → iTunes). If the resolved URL can't be displayed the
 *    caller advances to the next candidate. [resolveWithProvider] lets the
 *    reveal sheets offer an explicit art-SOURCE switch ("if you didn't like
 *    that one, show the other").
 */
object CabinetCoverCache {

    enum class CoverKind(val stateKey: String) {
        BOOK("book"),
        ALBUM("album"),
        SERIES("series")
    }

    /** Bumped after every successful download so grid tiles that composed
     *  BEFORE their cover landed re-check the local file (reading this
     *  during composition subscribes the caller to the bump). */
    val version = mutableIntStateOf(0)

    private fun dir(context: Context): File =
        File(context.applicationContext.filesDir, "cover_cache").apply { mkdirs() }

    /** `<kind>-<sanitized-name>.img` — stable per topic, safe for the FS. */
    private fun fileName(kind: CoverKind, name: String): String =
        "${kind.stateKey}-" + name.filter { it.isLetterOrDigit() || it == ' ' || it == '-' }
            .trim().replace(' ', '_').take(80) + ".img"

    fun localCoverFile(context: Context, kind: CoverKind, name: String): File? {
        val f = File(dir(context), fileName(kind, name))
        return f.takeIf { it.exists() && it.length() > 0L }
    }

    /** The URL the app already verified + persisted for this topic, if any. */
    fun persistedUrl(context: Context, kind: CoverKind, name: String): String? = when (kind) {
        CoverKind.BOOK -> AppPreferences.bookCoverUrlsState[name]?.takeIf { it.isNotBlank() }
        else -> AppPreferences.sheetArtUrlsState["${kind.stateKey}|$name"]?.takeIf { it.isNotBlank() }
    }

    private fun persistUrl(context: Context, kind: CoverKind, name: String, url: String) {
        when (kind) {
            CoverKind.BOOK -> AppPreferences.setBookCoverUrl(context, name, url)
            else -> AppPreferences.setSheetArtUrl(context, "${kind.stateKey}|$name", url)
        }
    }

    /** Number of switchable art providers per kind (book sheets use 2 too —
     *  iTunes + Open Library — LibraryThing stays key-gated in the hub). */
    fun providerCount(kind: CoverKind): Int = when (kind) {
        CoverKind.BOOK -> 2
        CoverKind.ALBUM -> AlbumArtFetch.PROVIDER_COUNT
        CoverKind.SERIES -> SeriesPosterFetch.PROVIDER_COUNT
    }

    /**
     * Resolve a cover URL for [provider] (0-based; see the fetchers).
     * Books: 0 = iTunes, 1 = Open Library title; albums: 0 = iTunes,
     * 1 = MusicBrainz; series: 0 = TVMaze, 1 = iTunes. An authored topic
     * imageUrl always wins for books (provider-independent) — pass it in
     * [authoredUrl] so the caller can keep it as the first candidate.
     */
    suspend fun resolveWithProvider(
        context: Context,
        kind: CoverKind,
        name: String,
        byline: String?,
        authoredUrl: String?,
        provider: Int
    ): String? = when (kind) {
        CoverKind.BOOK -> BookCoverFetch.resolveCoverUrl(
            context,
            name,
            byline,
            authoredUrl.orEmpty(),
            if (provider == 1) BookCoverFetch.BookCoverProvider.OPEN_LIBRARY
            else BookCoverFetch.BookCoverProvider.ITUNES
        )
        CoverKind.ALBUM -> AlbumArtFetch.resolveArtworkUrl(name, byline, provider)
        CoverKind.SERIES -> SeriesPosterFetch.resolvePosterUrl(name, provider)
    }

    /** Resolve with the DEFAULT cascade (best provider first, then the next
     *  — "if one doesn't show, show the other") and persist the winner. */
    suspend fun resolveAndPersist(
        context: Context,
        kind: CoverKind,
        name: String,
        byline: String?,
        authoredUrl: String?
    ): String? {
        val existing = persistedUrl(context, kind, name)
        if (existing != null) return existing
        for (p in 0 until providerCount(kind)) {
            val url = resolveWithProvider(context, kind, name, byline, authoredUrl, p)
            if (!url.isNullOrBlank()) {
                persistUrl(context, kind, name, url)
                return url
            }
        }
        return null
    }

    /**
     * Ensure the cover IMAGE bytes are stored on disk for this topic.
     * Returns the local file (or null when nothing could be resolved). Safe
     * to call repeatedly — an existing file short-circuits. [redownload]
     * forces a fresh fetch (used when the user switches the art source).
     */
    suspend fun ensureLocalCover(
        context: Context,
        kind: CoverKind,
        name: String,
        byline: String?,
        authoredUrl: String?,
        redownload: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        if (!redownload) {
            localCoverFile(context, kind, name)?.let { return@withContext it }
        }
        val url = persistedUrl(context, kind, name)
            ?: resolveAndPersist(context, kind, name, byline, authoredUrl)
            ?: return@withContext null
        val bytes = downloadBytes(url) ?: return@withContext null
        val f = File(dir(context), fileName(kind, name))
        runCatching { f.writeBytes(bytes) }
        if (f.exists() && f.length() > 0L) version.intValue++
        f.takeIf { it.exists() && it.length() > 0L }
    }

    /** Download the image bytes (8s timeouts, best-effort). */
    private fun downloadBytes(urlString: String): ByteArray? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Curio/1.0 (cover cache)")
            conn.setRequestProperty("Accept", "image/*")
            if (conn.responseCode !in 200..399) return null
            conn.inputStream.use { it.readBytes().takeIf { b -> b.size > 512 } }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

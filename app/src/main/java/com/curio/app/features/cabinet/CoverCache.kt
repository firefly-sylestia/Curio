package com.curio.app.features.cabinet

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import com.curio.app.data.AppPreferences
import com.curio.app.features.reveal.AlbumArtFetch
import com.curio.app.features.reveal.SeriesPosterFetch
import com.curio.app.features.settings.BookCoverFetch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

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
     *  during composition subscribes the caller to the bump). The WARMER
     *  bumps ONCE per batch (see [ensureLocalCover]'s `bumpVersion`), so a
     *  big first-open shelf doesn't recompose the whole grid per download. */
    val version = mutableIntStateOf(0)

    /** Extracted-accent cache (`kind|name` → ARGB) — each cover's dominant
     *  color is computed ONCE and reused by every row/tile/review that asks.
     *  v3xx37 — CONCURRENT: the warmer pre-warms colors off the main thread
     *  while composition reads the cache, so a plain HashMap could corrupt
     *  under the race. */
    private val dominantColorCache = ConcurrentHashMap<String, Int>()

    /** The DOMINANT COLOR of a cover's cached bytes — downsampled decode +
     *  bucket quantization, computed once and cached forever. Falls back to
     *  [fallback] when the bytes aren't on disk yet (or decode fails), so a
     *  bare tile keeps the category accent until its cover lands. */
    fun dominantCoverColor(context: Context, kind: CoverKind, name: String, fallback: Color): Color {
        val key = "${kind.stateKey}|$name"
        dominantColorCache[key]?.let { return Color(it) }
        val file = localCoverFile(context, kind, name) ?: return fallback
        val argb = runCatching { extractDominantArgb(file) }.getOrNull()
        if (argb != null) dominantColorCache[key] = argb
        return argb?.let { Color(it) } ?: fallback
    }

    /** v3xx37 — pre-warm a cover's dominant color OFF the main thread (the
     *  warmer calls this right after saving the bytes). Composition-time
     *  [dominantCoverColor] calls then hit the cache instead of decoding a
     *  bitmap on the main thread — the decode still happens exactly once,
     *  just not on the UI thread. No-op when the bytes aren't on disk yet
     *  or the color is already cached. */
    fun warmDominantColor(context: Context, kind: CoverKind, name: String) {
        val key = "${kind.stateKey}|$name"
        if (dominantColorCache.containsKey(key)) return
        val file = localCoverFile(context, kind, name) ?: return
        val argb = runCatching { extractDominantArgb(file) }.getOrNull() ?: return
        dominantColorCache[key] = argb
    }

    /** Downsample the image to ~24px, bucket-quantize the RGB (4 bits per
     *  channel), and return the most frequent bucket as an ARGB int — count
     *  primary, saturation as the tiebreak so a colorful cover wins over a
     *  gray edge. */
    private fun extractDominantArgb(file: File): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalStateException("no bounds")
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 24 && bounds.outHeight / (sample * 2) >= 24) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: throw IllegalStateException("no bitmap")
        try {
            val counts = HashMap<Int, Int>()
            var total = 0L
            var rSum = 0L; var gSum = 0L; var bSum = 0L
            for (y in 0 until bmp.height step 2) {
                for (x in 0 until bmp.width step 2) {
                    val px = bmp.getPixel(x, y)
                    val r = (px shr 16) and 0xFF
                    val g = (px shr 8) and 0xFF
                    val b = px and 0xFF
                    rSum += r; gSum += g; bSum += b; total++
                    val q = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                    counts[q] = (counts[q] ?: 0) + 1
                }
            }
            if (total == 0L) throw IllegalStateException("empty")
            val best = counts.maxByOrNull { (q, n) ->
                val r = (q shr 8) shl 4
                val g = ((q shr 4) and 0xF) shl 4
                val b = (q and 0xF) shl 4
                n * 4 + (maxOf(r, g, b) - minOf(r, g, b))
            }?.key
            if (best != null) {
                return 0xFF000000.toInt() or
                    (((best shr 8) shl 4) shl 16) or
                    ((((best shr 4) and 0xF) shl 4) shl 8) or
                    ((best and 0xF) shl 4)
            }
            val avgR = (rSum / total).toInt()
            val avgG = (gSum / total).toInt()
            val avgB = (bSum / total).toInt()
            return 0xFF000000.toInt() or (avgR shl 16) or (avgG shl 8) or avgB
        } finally {
            bmp.recycle()
        }
    }

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
        redownload: Boolean = false,
        // v3xx37 — the WARMER batches its downloads and bumps the version
        // ONCE after the loop (a per-download bump recomposed every
        // version-keyed tile per save — 30 downloads = 30 grid-wide
        // recompositions + 30 main-thread dominant-color decodes on first
        // open). Single saves (the tile live-resolve) keep the bump.
        bumpVersion: Boolean = true
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
        if (bumpVersion && f.exists() && f.length() > 0L) version.intValue++
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

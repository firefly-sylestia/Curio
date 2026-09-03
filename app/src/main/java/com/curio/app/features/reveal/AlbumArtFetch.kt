package com.curio.app.features.reveal

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Keyless album-artwork resolver for the reveal's album sheet (mirrors the
 * book side, where [com.curio.app.features.settings.BookCoverFetch] powers
 * the poster).
 *
 * Albums in the catalog carry NO authored `imageUrl` (0/1000 today), so the
 * cover must be found from the album's name + artist. Strategy, cheapest and
 * most reliable first:
 *
 *  1. **iTunes Search API** — a single keyless GET
 *     (`itunes.apple.com/search?term=…&entity=album`). Returns the album's
 *     artwork URL, which is resizable by swapping the `100x100bb` size token
 *     for a larger one (`600x600bb`). No API key, no rate-limit drama.
 *  2. **MusicBrainz + Cover Art Archive** — keyless too, but a two-step
 *     lookup: search the release-group (album + artist), then ask the Cover
 *     Art Archive for that group's front cover. Slightly slower and needs a
 *     polite User-Agent, so it only runs when iTunes finds nothing.
 *
 * Results are memoized per album+artist in-process so re-opening the sheet
 * never re-queries the network; Coil's disk cache holds the artwork bytes.
 */
object AlbumArtFetch {

    /** iTunes artwork URLs arrive at 100px; 600px is plenty for a sheet
     *  poster without blowing up memory or bandwidth. */
    private const val ITUNES_SIZE = "600x600bb"

    /** Tiny in-process memo: albumName|artist → artwork URL ("" = known miss). */
    private val cache = ConcurrentHashMap<String, String>()

    /**
     * Resolve an album's artwork URL, best-effort. Returns null when neither
     * provider finds a match (callers show their fallback tile).
     *
     * @param albumName Album title (topic name), e.g. "Revolver".
     * @param artist    Album artist (topic byline), e.g. "The Beatles".
     */
    suspend fun resolveArtworkUrl(albumName: String, artist: String?): String? =
        withContext(Dispatchers.IO) {
            val key = "$albumName|${artist.orEmpty()}"
            cache[key]?.let { return@withContext it.ifEmpty { null } }

            val resolved = runCatching {
                itunesArtwork(albumName, artist) ?: musicBrainzArtwork(albumName, artist)
            }.getOrNull()

            cache[key] = resolved.orEmpty()
            resolved
        }

    /** iTunes Search API — album entity search, upscale the artwork token. */
    private fun itunesArtwork(albumName: String, artist: String?): String? {
        val term = buildString {
            append(Uri.encode(albumName.trim()))
            if (!artist.isNullOrBlank()) append("+").append(Uri.encode(artist.trim()))
        }
        val json = httpGet("https://itunes.apple.com/search?term=$term&entity=album&limit=8")
            ?: return null
        return runCatching {
            val results = org.json.JSONObject(json).optJSONArray("results") ?: return null
            var best: String? = null
            var bestScore = 0
            for (i in 0 until results.length()) {
                val r = results.optJSONObject(i) ?: continue
                val name = r.optString("collectionName")
                val art = r.optString("artworkUrl100")
                if (art.isBlank()) continue
                val score = matchScore(name, albumName, r.optString("artistName"), artist)
                if (score > bestScore) {
                    bestScore = score
                    best = art
                }
                // An exact title + artist hit is as good as it gets.
                if (score >= 3) break
            }
            best
                ?.replace("100x100bb", ITUNES_SIZE)
                ?.replace("http://", "https://")
        }.getOrNull()
    }

    /**
     * MusicBrainz release-group search → Cover Art Archive front cover.
     * MusicBrainz politely asks for a User-Agent; CAA redirects to the image.
     */
    private fun musicBrainzArtwork(albumName: String, artist: String?): String? {
        // MusicBrainz: one request per second out of politeness.
        Thread.sleep(1000)
        val query = buildString {
            append("releasegroup:\"${escapeLucene(albumName)}\"")
            if (!artist.isNullOrBlank()) append(" AND artist:\"${escapeLucene(artist)}\"")
        }
        val json = httpGet(
            "https://musicbrainz.org/ws/2/release-group/?query=" +
                Uri.encode(query) + "&fmt=json&limit=5",
            userAgent = "CurioApp/1.0 (album art lookup)"
        ) ?: return null
        val groupId = runCatching {
            val groups = org.json.JSONObject(json).optJSONArray("release-groups") ?: return null
            var best: String? = null
            var bestScore = 0
            for (i in 0 until groups.length()) {
                val g = groups.optJSONObject(i) ?: continue
                val id = g.optString("id")
                if (id.isBlank()) continue
                val score = matchScore(
                    g.optString("title"),
                    albumName,
                    g.optJSONArray("artist-credit")
                        ?.optJSONObject(0)?.optString("name").orEmpty(),
                    artist
                )
                if (score > bestScore) {
                    bestScore = score
                    best = id
                }
            }
            best
        }.getOrNull() ?: return null

        // Cover Art Archive: ask for the group's front cover (redirects to
        // the archive.org image). Returns non-2xx for art-less groups.
        val code = httpCode("https://coverartarchive.org/release-group/$groupId/front-500")
        if (code != null && code in 200..399) {
            return "https://coverartarchive.org/release-group/$groupId/front-500"
        }
        return null
    }

    /**
     * Rough relevance score: 3 = exact title AND artist match, 2 = exact
     * title (artist ignored/absent), 1 = fuzzy containment, 0 = no match.
     */
    private fun matchScore(name: String, wantName: String, artist: String, wantArtist: String?): Int {
        val n = name.trim()
        val w = wantName.trim()
        val titleExact = n.equals(w, ignoreCase = true)
        val titleFuzzy = !w.isBlank() && (n.contains(w, ignoreCase = true) ||
            w.contains(n, ignoreCase = true) ||
            titleWordsOverlap(n, w))
        val artistExact = !wantArtist.isNullOrBlank() &&
            artist.trim().equals(wantArtist.trim(), ignoreCase = true)
        return when {
            titleExact && artistExact -> 3
            titleExact -> 2
            titleFuzzy && artistExact -> 2
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

    /** Escape Lucene query specials for MusicBrainz' search syntax. */
    private fun escapeLucene(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    /** Minimal keyless GET — 8s timeout, best-effort. */
    private fun httpGet(urlString: String, userAgent: String = "Curio/1.0"): String? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", userAgent)
            val code = conn.responseCode
            if (code != 200) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /** HEAD-ish status probe (CAA answers with redirects; we accept any 2xx/3xx). */
    private fun httpCode(urlString: String): Int? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Curio/1.0")
            conn.setRequestProperty("Accept", "image/*")
            // CAA answers with a 307 redirect to archive.org when the art
            // exists (or 404 when the group has no cover). Do NOT follow the
            // redirect — any 2xx/3xx status confirms the cover exists, and
            // Coil fetches the real bytes later. Following it here would
            // download the full image just to probe.
            conn.instanceFollowRedirects = false
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

package com.curio.app.features.reveal

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Keyless song art resolver — mirrors [AlbumArtFetch] for albums.
 *
 * Songs in the catalog carry an AUTHORED `imageUrl` field (currently empty
 * for most entries); this resolver fills the gap with iTunes Search:
 *
 *  **iTunes Search API** — `itunes.apple.com/search?term=…&media=music&entity=song`.
 *     Free, no API key, returns artwork at 100px (upscaled to 600px).
 *     Primary because it is purpose-built for music and answers with the
 *     exact track.
 *
 * Results are memoized per title+artist in-process so reopening never
 * re-queries; Coil's disk cache holds the art bytes.
 */
object SongArtFetch {

    /** iTunes artwork URLs arrive at 100px; 600px is plenty for song art. */
    private const val ITUNES_SIZE = "600x600bb"

    /** Tiny in-process memo: "title|artist" → art URL ("" = miss). */
    private val cache = ConcurrentHashMap<String, String>()

    /**
     * Resolve a song's art URL, best-effort. [songName] is the topic name
     * verbatim; [artist] is the byline (may be blank). Returns null when
     * iTunes finds no art.
     */
    suspend fun resolveArtworkUrl(
        songName: String,
        artist: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val key = "${songName.trim()}|${artist.orEmpty().trim()}"
        cache[key]?.let { return@withContext it.ifEmpty { null } }

        val resolved = runCatching { itunesArtwork(songName, artist) }.getOrNull()
        cache[key] = resolved.orEmpty()
        resolved
    }

    /** iTunes song search → best-matching artwork, upscaled to 600px. */
    private fun itunesArtwork(songName: String, artist: String?): String? {
        val query = buildString {
            append(songName.trim())
            if (!artist.isNullOrBlank()) {
                append(" ")
                append(artist.trim())
            }
        }
        val json = httpGet(
            "https://itunes.apple.com/search?term=${Uri.encode(query)}" +
                "&media=music&entity=song&limit=8"
        ) ?: return null
        return runCatching {
            val results = JSONObject(json).optJSONArray("results") ?: return null
            var best: String? = null
            var bestScore = 0
            for (i in 0 until results.length()) {
                val r = results.optJSONObject(i) ?: continue
                val art = r.optString("artworkUrl100")
                if (art.isBlank()) continue
                val trackName = r.optString("trackName", "")
                val artistName = r.optString("artistName", "")
                // Score against both title and artist for best match.
                val titleScore = matchScore(trackName, songName)
                val artistScore = if (!artist.isNullOrBlank()) {
                    matchScore(artistName, artist)
                } else 0
                val score = titleScore + artistScore
                if (score > bestScore) {
                    bestScore = score
                    best = art
                }
                if (titleScore >= 2) break
            }
            best
                ?.replace("100x100bb", ITUNES_SIZE)
                ?.replace("http://", "https://")
        }.getOrNull()
    }

    /**
     * Fetch a song's description from iTunes (the track's "long description"
     * or "description" field). Returns null when the title doesn't match or
     * the description is too short to be useful.
     */
    suspend fun fetchDescription(songName: String, artist: String? = null): String? =
        withContext(Dispatchers.IO) {
            val query = buildString {
                append(songName.trim())
                if (!artist.isNullOrBlank()) {
                    append(" ")
                    append(artist.trim())
                }
            }
            val json = httpGet(
                "https://itunes.apple.com/search?term=${Uri.encode(query)}" +
                    "&media=music&entity=song&limit=5"
            ) ?: return@withContext null
            runCatching {
                val results = JSONObject(json).optJSONArray("results") ?: return@runCatching null
                for (i in 0 until results.length()) {
                    val r = results.optJSONObject(i) ?: continue
                    val trackName = r.optString("trackName", "")
                    val score = matchScore(trackName, songName)
                    if (score >= 1) {
                        val desc = r.optString("longDescription", "")
                            .ifBlank { r.optString("description", "") }
                            .trim()
                        return@runCatching desc.takeIf { it.length >= 40 }
                    }
                }
                null
            }.getOrNull()
        }

    /** Rough relevance: 2 = exact title, 1 = containment / word overlap, 0 = miss. */
    private fun matchScore(name: String, wantTitle: String): Int {
        val n = name.trim()
        val w = wantTitle.trim()
        if (n.equals(w, ignoreCase = true)) return 2
        if (!w.isBlank() && (n.contains(w, ignoreCase = true) ||
                w.contains(n, ignoreCase = true) ||
                titleWordsOverlap(n, w))) return 1
        return 0
    }

    private fun titleWordsOverlap(a: String, b: String): Boolean {
        val wa = a.split(Regex("[^A-Za-z0-9]+")).filter { it.length >= 4 }.map { it.lowercase() }.toSet()
        val wb = b.split(Regex("[^A-Za-z0-9]+")).filter { it.length >= 4 }.map { it.lowercase() }.toSet()
        return wa.any { it in wb }
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

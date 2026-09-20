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
 *  **MusicBrainz + Cover Art Archive** (v426b) — the same second door the
 *     ALBUM side already had, which songs did not: one keyless pair, asked
 *     only when iTunes found nothing. A song is searched as a RECORDING (a
 *     release-group's title is the album's, so the album door cannot answer
 *     for a track), and the cover comes from the release the recording sits on.
 *     MusicBrainz asks for 1 request/second and a real User-Agent, both kept
 *     to here.
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

        // v426b — ONE DOOR WAS NOT ENOUGH FOR A SONG. iTunes is a shop: a track
        // it does not carry (anything out of print, most non-Western releases,
        // live and bootleg recordings) had no cover at all, while the ALBUM
        // beside it had two sources. MusicBrainz is that second source, asked
        // only when the shop answered nothing.
        val resolved = runCatching {
            itunesArtwork(songName, artist) ?: musicBrainzArtwork(songName, artist)
        }.getOrNull()
        cache[key] = resolved.orEmpty()
        resolved
    }

    /**
     * MusicBrainz RECORDING search → the release it sits on → Cover Art Archive.
     *
     * The album door searches release-GROUPS, whose title is the album's — a
     * song's own name would never match one. A recording is the track, and its
     * `releases` carry the id the Cover Art Archive serves a front cover for.
     * Null on any miss (no match, no cover, an unreachable service).
     */
    private fun musicBrainzArtwork(songName: String, artist: String?): String? {
        pace()
        val query = buildString {
            append("recording:\"${escapeLucene(songName)}\"")
            if (!artist.isNullOrBlank()) append(" AND artist:\"${escapeLucene(artist)}\"")
        }
        val json = httpGet(
            "https://musicbrainz.org/ws/2/recording/?query=" +
                Uri.encode(query) + "&fmt=json&limit=5",
            userAgent = "CurioApp/1.0 (song art lookup)"
        ) ?: return null
        val releaseId = runCatching {
            val recordings = JSONObject(json).optJSONArray("recordings") ?: return null
            var best: String? = null
            var bestScore = 0
            for (i in 0 until recordings.length()) {
                val recording = recordings.optJSONObject(i) ?: continue
                val score = matchScore(recording.optString("title"), songName)
                if (score < 2 || score <= bestScore) continue
                val release = recording.optJSONArray("releases")?.optJSONObject(0)
                    ?: continue
                val id = release.optString("id")
                if (id.isBlank()) continue
                bestScore = score
                best = id
            }
            best
        }.getOrNull() ?: return null
        val url = "https://coverartarchive.org/release/$releaseId/front-500"
        val code = httpCode(url)
        return if (code != null && code in 200..399) url else null
    }

    /** When the last MusicBrainz request went out (its own 1/second rule). */
    private var lastAsked = 0L

    /** Space MusicBrainz calls a second apart, which is what it asks for. */
    private fun pace() {
        val since = System.currentTimeMillis() - lastAsked
        if (lastAsked != 0L && since in 0L until PACE_MS) {
            runCatching { Thread.sleep(PACE_MS - since) }
        }
        lastAsked = System.currentTimeMillis()
    }

    /** Escape Lucene query specials for MusicBrainz' search syntax. */
    private fun escapeLucene(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

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

    /**
     * A status probe for the Cover Art Archive, which answers a 307 REDIRECT to
     * its own image when the cover exists and 404 when it does not. The redirect
     * is deliberately not followed: its status is the answer, and following it
     * would download the whole picture just to ask.
     */
    private fun httpCode(urlString: String): Int? = runCatching {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Curio/1.0")
            conn.setRequestProperty("Accept", "image/*")
            conn.instanceFollowRedirects = false
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /** The 1/second floor MusicBrainz asks for, a little over a second. */
    private const val PACE_MS = 1100L
}

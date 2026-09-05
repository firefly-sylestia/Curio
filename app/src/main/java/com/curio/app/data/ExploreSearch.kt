package com.curio.app.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * v19 — the search engines Explore can open in the browser. The "Explore
 * in Google" button became "Explore in browser": it searches whichever
 * engine the user picked in Settings (Notifications → Search engine) so
 * people who don't want to use Google can choose their own. Google stays
 * the default, so behavior is unchanged until the user switches.
 */
enum class SearchEngine(val id: String, val displayName: String, val description: String) {
    GOOGLE("google", "Google", "The classic, everywhere"),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "Privacy-first, no tracking"),
    BING("bing", "Bing", "Microsoft's search"),
    BRAVE("brave", "Brave", "Independent & private"),
    ECOSIA("ecosia", "Ecosia", "Plants trees as you search"),
    STARTPAGE("startpage", "Startpage", "Google results, privately"),
    YAHOO("yahoo", "Yahoo", "The long-running portal");

    companion object {
        fun fromId(id: String?): SearchEngine =
            entries.firstOrNull { it.id == id } ?: GOOGLE
    }
}

/**
 * v27s — the music services the "Watch in" explore action can open for
 * Album / Artist / Song topics. Chosen in Settings next to the search
 * engine; YouTube is the default for new and unset preferences.
 */
enum class MusicService(val id: String, val displayName: String, val description: String) {
    YOUTUBE("youtube", "YouTube", "The main YouTube video platform"),
    YOUTUBE_MUSIC("youtube_music", "YouTube Music", "The music side of YouTube"),
    APPLE_MUSIC("apple_music", "Apple Music", "Apple's streaming catalog"),
    SPOTIFY("spotify", "Spotify", "The big green streaming app");

    companion object {
        fun fromId(id: String?): MusicService =
            entries.firstOrNull { it.id == id } ?: YOUTUBE_MUSIC
    }
}

fun buildExploreQuery(topic: CurioTopic): String {
    val parts = mutableListOf<String>()
    if (topic.subtype.equals("Album", ignoreCase = true)) {
        extractArtist(topic.teaser)?.let { parts += it }
    }
    parts += topic.name
    extractYear(topic)?.let { parts += it }
    parts += topic.subtype
    return parts.joinToString(" ").trim()
}

fun buildGoogleSearchUrl(topic: CurioTopic): String =
    "https://www.google.com/search?q=" + Uri.encode(buildExploreQuery(topic))

fun buildYouTubeSearchUrl(topic: CurioTopic): String =
    "https://www.youtube.com/results?search_query=" + Uri.encode(buildExploreQuery(topic))

/**
 * v27s — the search URL for [service] (the selected service by default,
 * read reactively from [AppPreferences.musicServiceState] so the Topic
 * Reveal dialog reopens the right service the moment the user changes it
 * in Settings). Used by the "Watch in" action on Album / Artist / Song
 * topics.
 */
fun buildMusicServiceSearchUrl(
    topic: CurioTopic,
    service: MusicService = MusicService.fromId(AppPreferences.musicServiceState)
): String {
    val q = Uri.encode(buildExploreQuery(topic))
    return when (service) {
        MusicService.YOUTUBE -> "https://www.youtube.com/results?search_query=$q"
        MusicService.YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=$q"
        // Apple Music's storefront is derived from the device locale instead of
        // hardcoding /us. When no country is available, use Apple's locale-free
        // search route so the URL does not force a US storefront.
        MusicService.APPLE_MUSIC -> {
            val storefront = Locale.getDefault().country
                .takeIf { it.length == 2 }
                ?.lowercase(Locale.ROOT)
            val path = storefront?.let { "/$it/search" } ?: "/search"
            // v52 — the native `music://` scheme, not https. The Android
            // Apple Music app renders music.apple.com/search (a web-only
            // page) in an in-app browser with an "Open in browser" banner
            // instead of searching natively. `music://` is Apple's registered
            // scheme — any music.apple.com path works with https swapped for
            // music — so the app's native router handles it and lands on the
            // search tab. [openSearchUrl] falls back to the https URL when
            // the app isn't installed (no handler for a custom scheme).
            "music://music.apple.com$path?term=$q"
        }
        // Spotify's web search path (/search/{query}) is the correct deep
        // link: it hands off into the installed app or opens the web player.
        MusicService.SPOTIFY -> "https://open.spotify.com/search/$q"
    }
}

/**
 * v52b — resolves the topic to a REAL Apple Music catalog item via the
 * public iTunes Search API and returns its native deep link
 * (`music://music.apple.com/...`). The Android Apple Music app only
 * handles ITEM pages natively — search links (`music.apple.com/search`)
 * render in an in-app browser with an "Open in browser" banner — so a
 * search URL can never land on the native result. Returns null when
 * nothing is found or the network fails; the caller falls back to
 * [buildMusicServiceSearchUrl]'s search link.
 *
 * v107 — two fixes that made SONG topics fail while artists and some
 * albums worked: (1) the old `music://music.apple.com/{cc}/song/{id}`
 * deep link is a DEAD route (music.apple.com/song/{id} → HTTP 404 — a
 * song's only canonical page is its ALBUM page with `?i=trackId`), so we
 * now use the API's own `trackViewUrl` / `collectionViewUrl` /
 * `artistLinkUrl` and swap the scheme to `music://`; (2) the search term
 * now leads with [CurioTopic.byline] (the artist — the old teaser regex
 * only ran for albums and often misfired) and strips the trailing
 * `(1984)` year — the API returns ZERO results for parenthesized years —
 * so the top hit is the right item.
 */
suspend fun resolveAppleMusicItemUrl(topic: CurioTopic): String? = withContext(Dispatchers.IO) {
    val entity = when {
        topic.subtype.equals("Album", ignoreCase = true) -> "album"
        topic.subtype.equals("Artist", ignoreCase = true) -> "musicArtist"
        else -> "song"
    }
    val storefront = Locale.getDefault().country
        .takeIf { it.length == 2 }
        ?.lowercase(Locale.ROOT)
        ?: "us"
    // v107 — focused catalog query: artist + title. byline IS the artist
    // for Album/Song topics; a trailing "(1984)" makes the API return
    // zero results, so strip it. The subtype word ("Song") adds no signal
    // here.
    val byline = topic.byline.takeIf { it.isNotBlank() }
        ?: if (topic.subtype.equals("Album", ignoreCase = true)) extractArtist(topic.teaser) else null
    val title = topic.name.replace(TRAILING_YEAR_IN_PARENS, "").trim()

    // v364 — ALBUM deep links resolve through the ARTIST'S REAL catalog:
    // the search API's ranking is unreliable for famous catalogs — the
    // actual "Nevermind" (Nirvana) and "The Dark Side of the Moon" (Pink
    // Floyd) never appear in the top 25 search hits at all, so a
    // results[0] deep link opened a tribute album, a same-title single by
    // another artist, or a totally different album ("The Wall") — which
    // reads as a wrong or blank Apple Music page. New path: resolve the
    // artist ID, pull their album catalog (/lookup), and match the title
    // with a strict score gate; fall back to a SCORED search (never blind
    // results[0]) and finally null, which sends the caller to the plain
    // search link.
    if (entity == "album") {
        return@withContext runCatching {
            val artistId = resolveArtistId(byline, storefront)
            val web = artistId?.let { bestAlbumFromCatalog(it, title, byline, storefront) }
                ?: bestAlbumFromSearch(title, byline, storefront)
            web?.let { toAppleMusicDeepLink(it) }
        }.getOrNull()
    }

    runCatching {
        val query = Uri.encode(listOfNotNull(byline, title).joinToString(" "))
        val conn = URL(
            "https://itunes.apple.com/search?term=$query&media=music&entity=$entity&country=$storefront&limit=1"
        ).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val first = JSONObject(raw).optJSONArray("results")?.optJSONObject(0)
                ?: return@runCatching null
            // v107 — canonical URL straight from the API: songs must open
            // via the ALBUM page + ?i=trackId (the /song/{id} route 404s),
            // and the URL's country matches the device storefront.
            val web = when (entity) {
                "song" -> first.optString("trackViewUrl")
                else -> first.optString("artistLinkUrl")
            }
            if (web.isBlank()) return@runCatching null
            toAppleMusicDeepLink(web)
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

/** v364 — `music://` deep link from an https Apple Music page URL. */
private fun toAppleMusicDeepLink(web: String): String =
    "music://" + web
        .removePrefix("https://")
        .removePrefix("http://")
        .replace(UO_TRACKING_PARAM, "")

/** v364 — keyless GET with the same 8s timeouts as the existing paths. */
private fun itunesHttpGet(url: String): String? = runCatching {
    val conn = URL(url).openConnection() as HttpURLConnection
    try {
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        conn.disconnect()
    }
}.getOrNull()

/** v364 — the artist's iTunes ID (exact name match on the musicArtist
 *  search; null when the name isn't found or the network fails). */
private fun resolveArtistId(byline: String?, storefront: String): Long? {
    if (byline.isNullOrBlank()) return null
    val json = itunesHttpGet(
        "https://itunes.apple.com/search?term=${Uri.encode(byline)}&media=music&entity=musicArtist&country=$storefront&limit=5"
    ) ?: return null
    val results = runCatching { JSONObject(json).optJSONArray("results") }.getOrNull() ?: return null
    for (i in 0 until results.length()) {
        val r = results.optJSONObject(i) ?: continue
        if (r.optString("artistName").equals(byline, ignoreCase = true)) {
            val id = r.optLong("artistId", 0L)
            if (id > 0L) return id
        }
    }
    null
}

/**
 * v364 — best album URL from the artist's OWN catalog (/lookup entity=album,
 * up to 200 albums). Score gate: >= 25 means an EXACT title (any artist) or
 * a title-containment fuzzy WITH the exact artist — anything weaker
 * (word-overlap fuzzy, a tribute/single by another artist) is rejected so
 * a wrong-album deep link never ships. Ties prefer real albums
 * (trackCount > 0 — a trackless preorder renders as a blank page).
 */
private fun bestAlbumFromCatalog(
    artistId: Long,
    title: String,
    byline: String?,
    storefront: String
): String? {
    val json = itunesHttpGet(
        "https://itunes.apple.com/lookup?id=$artistId&entity=album&country=$storefront&limit=200"
    ) ?: return null
    val results = runCatching { JSONObject(json).optJSONArray("results") }.getOrNull() ?: return null
    var bestUrl: String? = null
    var bestScore = 0
    var bestTracks = false
    for (i in 0 until results.length()) {
        val r = results.optJSONObject(i) ?: continue
        if (r.optString("wrapperType") != "collection") continue
        val score = appleAlbumScore(
            r.optString("collectionName"), title, r.optString("artistName"), byline
        )
        if (score < 25) continue
        val tracks = r.optInt("trackCount", 0) > 0
        if (score > bestScore || (score == bestScore && tracks && !bestTracks)) {
            bestScore = score
            bestTracks = tracks
            bestUrl = r.optString("collectionViewUrl").takeIf { it.isNotBlank() }
        }
        if (score >= 35) break
    }
    bestUrl
}

/** v364 — fallback scored search (10 results, same >= 25 gate as the
 *  catalog path): catches albums whose artist name differs from the topic's
 *  byline, so the artist lookup can't run. */
private fun bestAlbumFromSearch(
    title: String,
    byline: String?,
    storefront: String
): String? {
    val query = Uri.encode(listOfNotNull(byline, title).joinToString(" "))
    val json = itunesHttpGet(
        "https://itunes.apple.com/search?term=$query&media=music&entity=album&country=$storefront&limit=10"
    ) ?: return null
    val results = runCatching { JSONObject(json).optJSONArray("results") }.getOrNull() ?: return null
    var bestUrl: String? = null
    var bestScore = 0
    var bestTracks = false
    for (i in 0 until results.length()) {
        val r = results.optJSONObject(i) ?: continue
        val score = appleAlbumScore(
            r.optString("collectionName"), title, r.optString("artistName"), byline
        )
        if (score < 25) continue
        val tracks = r.optInt("trackCount", 0) > 0
        if (score > bestScore || (score == bestScore && tracks && !bestTracks)) {
            bestScore = score
            bestTracks = tracks
            bestUrl = r.optString("collectionViewUrl").takeIf { it.isNotBlank() }
        }
        if (score >= 35) break
    }
    bestUrl
}

/** v364 — iTunes album relevance: 35 = exact title + exact artist, 30 =
 *  exact title, 25 = containment-fuzzy title + exact artist, 20 =
 *  containment only, 15 = word-overlap + exact artist, 10 = overlap only,
 *  0 = no match. The >= 25 gate admits only results that are unmistakably
 *  the album. */
private fun appleAlbumScore(name: String, wantTitle: String, artist: String, wantArtist: String?): Int {
    val n = name.trim()
    val w = wantTitle.trim()
    val titleExact = n.equals(w, ignoreCase = true)
    val containment = !w.isBlank() &&
        (n.contains(w, ignoreCase = true) || w.contains(n, ignoreCase = true))
    val overlap = titleWordsOverlap(n, w)
    val artistExact = !wantArtist.isNullOrBlank() &&
        artist.trim().equals(wantArtist.trim(), ignoreCase = true)
    val titleScore = when {
        titleExact -> 30
        containment -> 20
        overlap -> 10
        else -> 0
    }
    return titleScore + if (artistExact) 5 else 0
}

/** True when the two titles share a meaningful (>= 4 char) word. */
private fun titleWordsOverlap(a: String, b: String): Boolean {
    val wa = a.split(Regex("[^A-Za-z0-9]+")).filter { it.length >= 4 }.map { it.lowercase() }.toSet()
    val wb = b.split(Regex("[^A-Za-z0-9]+")).filter { it.length >= 4 }.map { it.lowercase() }.toSet()
    return wa.any { it in wb }
}

/**
 * v358 — resolves a music topic to a REAL Spotify catalog item via the
 * client-credentials flow (app-level token, no user auth). Needs the
 * optional SPOTIFY_CLIENT_ID + SPOTIFY_CLIENT_SECRET BuildConfig values
 * (see .env.example); without them — or when the lookup misses — it returns
 * null and the caller falls back to the search link. Returns the native
 * `https://open.spotify.com/{album|track|artist}/{id}` deep link, which
 * hands off into the installed Spotify app (or the web player).
 */
suspend fun resolveSpotifyItemUrl(topic: CurioTopic): String? = withContext(Dispatchers.IO) {
    val clientId = com.curio.app.BuildConfig.SPOTIFY_CLIENT_ID.takeIf { it.isNotBlank() }
        ?: return@withContext null
    val clientSecret = com.curio.app.BuildConfig.SPOTIFY_CLIENT_SECRET.takeIf { it.isNotBlank() }
        ?: return@withContext null
    val token = runCatching { spotifyAppToken(clientId, clientSecret) }.getOrNull()
        ?: return@withContext null
    val type = when {
        topic.subtype.equals("Album", ignoreCase = true) -> "album"
        topic.subtype.equals("Artist", ignoreCase = true) -> "artist"
        else -> "track"
    }
    val byline = topic.byline.takeIf { it.isNotBlank() }
        ?: if (topic.subtype.equals("Album", ignoreCase = true)) extractArtist(topic.teaser) else null
    val title = topic.name.replace(TRAILING_YEAR_IN_PARENS, "").trim()
    // v364 — field-scoped quoted query: album:"..." artist:"..." / track:"..."
    // / artist:"..." — the plain "artist title" query lets weak matches in
    // (a same-title single by another artist can outrank the real album).
    val artistPart = byline?.takeIf { it.isNotBlank() }?.let { " artist:\"$it\"" } ?: ""
    val query = Uri.encode(
        if (type == "artist") "artist:\"$title\""
        else "$type:\"$title\"$artistPart"
    )
    runCatching {
        val conn = URL("https://api.spotify.com/v1/search?q=$query&type=$type&limit=5")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Authorization", "Bearer $token")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val items = JSONObject(raw).optJSONObject("${type}s")?.optJSONArray("items")
                ?: return@runCatching null
            var bestId: String? = null
            var bestScore = 0
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                val score = spotifyMatchScore(
                    item.optString("name"),
                    title,
                    item.optJSONArray("artists")?.optJSONObject(0)?.optString("name").orEmpty(),
                    byline
                )
                // v364 — reject weak fuzzy-only matches (score 1): a wrong
                // item is worse than falling back to the search link.
                if (score < 2) continue
                if (score > bestScore) {
                    bestScore = score
                    bestId = id
                }
                if (score >= 3) break
            }
            bestId?.let { "https://open.spotify.com/$type/$it" }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

/** Client-credentials token (app-level, no user auth): POST Basic auth with
 *  `id:secret` → `access_token` (valid ~1h; fetched per lookup). */
private fun spotifyAppToken(clientId: String, clientSecret: String): String? = runCatching {
    val conn = URL("https://accounts.spotify.com/api/token").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 8000
    conn.readTimeout = 8000
    conn.doOutput = true
    conn.setRequestProperty(
        "Authorization",
        "Basic " + android.util.Base64.encodeToString(
            "$clientId:$clientSecret".toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
    )
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    try {
        conn.outputStream.use { it.write("grant_type=client_credentials".toByteArray(Charsets.UTF_8)) }
        if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
        val raw = conn.inputStream.bufferedReader().use { it.readText() }
        JSONObject(raw).optString("access_token").takeIf { it.isNotBlank() }
    } finally {
        conn.disconnect()
    }
}.getOrNull()

/** Rough relevance: 3 = exact title + artist, 2 = exact title (or fuzzy
 *  title + exact artist), 1 = fuzzy containment, 0 = no match. */
private fun spotifyMatchScore(name: String, wantName: String, artist: String, wantArtist: String?): Int {
    val n = name.trim()
    val w = wantName.trim()
    val titleExact = n.equals(w, ignoreCase = true)
    val titleFuzzy = !w.isBlank() && (n.contains(w, ignoreCase = true) || w.contains(n, ignoreCase = true))
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

/**
 * v52 — launches [url] with a graceful fallback for Apple Music's custom
 * scheme. `music://` has no handler when the Apple Music app isn't
 * installed (a bare custom-scheme launch would throw
 * ActivityNotFoundException), so the https equivalent is opened instead —
 * preserving the old browser behavior.
 *
 * v110 — `music.youtube.com` links are package-PINNED to the YouTube Music
 * app (`com.google.android.apps.youtube.music`): the app's App Links
 * verification for that domain is unreliable — many devices hand the URL to
 * Chrome instead of the app, so the "Listen in" pill never opens inside
 * YouTube Music. Package-scoped delivery bypasses verification, so the
 * search lands IN the app; when the app isn't installed (no handler for
 * the pinned package) the plain https link opens in the browser instead.
 */
fun openSearchUrl(context: Context, url: String) {
    // v110 — the YouTube Music deep link: pin the intent to the app's
    // package so the OS delivers it there even when App Links verification
    // for music.youtube.com failed (the flaky-browser case). Falls back to
    // the bare https intent (browser) when the app isn't installed.
    if (url.startsWith("https://music.youtube.com/")) {
        val pinned = runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .setPackage(YOUTUBE_MUSIC_ANDROID_PACKAGE)
            )
        }
        if (pinned.isFailure) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        val httpsFallback = url.removePrefix("music://")
        if (httpsFallback != url) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://$httpsFallback"))
                )
            }
        }
    }
}

/** v110 — the YouTube Music Android app's package (Play Store id). */
private const val YOUTUBE_MUSIC_ANDROID_PACKAGE = "com.google.android.apps.youtube.music"

/**
 * v27s — the music lanes: Album / Artist / Song topics route the reveal
 * dialog's second action ("Watch in") to the user's chosen music service
 * instead of plain YouTube.
 */
fun CurioTopic.isMusicTopic(): Boolean =
    subtype.equals("Album", ignoreCase = true) ||
        subtype.equals("Artist", ignoreCase = true) ||
        subtype.equals("Song", ignoreCase = true)

/**
 * v19 — the search URL for [engine] (the selected engine by default, read
 * reactively from [AppPreferences.searchEngineState] so the Explore dialog
 * reopens the right engine the moment the user changes it in Settings).
 */
fun buildEngineSearchUrl(
    topic: CurioTopic,
    engine: SearchEngine = SearchEngine.fromId(AppPreferences.searchEngineState)
): String {
    val q = Uri.encode(buildExploreQuery(topic))
    return when (engine) {
        SearchEngine.GOOGLE -> buildGoogleSearchUrl(topic)
        SearchEngine.DUCKDUCKGO -> "https://duckduckgo.com/?q=$q"
        SearchEngine.BING -> "https://www.bing.com/search?q=$q"
        SearchEngine.BRAVE -> "https://search.brave.com/search?q=$q"
        SearchEngine.ECOSIA -> "https://www.ecosia.org/search?q=$q"
        SearchEngine.STARTPAGE -> "https://www.startpage.com/sp/search?query=$q"
        SearchEngine.YAHOO -> "https://search.yahoo.com/search?p=$q"
    }
}

/**
 * The default explore destination: the user's chosen search engine (v19 —
 * the old "music defaults to YouTube" rule is gone; YouTube remains the
 * explicit "Explore in YouTube" button in the Topic Reveal dialog).
 */
fun buildExploreSearchUrl(topic: CurioTopic): String =
    buildEngineSearchUrl(topic)

/**
 * v107 — a trailing parenthesized year on a topic name ("Purple Rain
 * (1984)"). The iTunes term API returns ZERO results when the term
 * contains one, so it's stripped before the catalog search.
 */
private val TRAILING_YEAR_IN_PARENS = Regex("\\s*\\(\\d{4}\\)\\s*$")

/** v107 — the iTunes API appends `&uo=4` to its view URLs; not needed in the deep link. */
private val UO_TRACKING_PARAM = Regex("[?&]uo=\\d+")

/** Year from the topic name ("Citizen Kane (1941)" → "1941"), else era tag. */
private fun extractYear(topic: CurioTopic): String? {
    val inName = Regex("\\b(18|19|20)\\d{2}\\b").find(topic.name)?.value
    if (inName != null) return inName
    return topic.tags.firstOrNull { it.matches(Regex("\\b(18|19|20)\\d{2}s\\b")) }
}

/** Best-effort artist extraction from an album teaser. */
private fun extractArtist(teaser: String): String? {
    Regex("\\b(18|19|20)\\d{2}\\s+([A-Z][\\w'.-]*(?:\\s+[A-Z][\\w'.-]*){0,2})")
        .find(teaser)?.let { match ->
            val artist = match.groupValues[2].trim()
            if (artist.isNotBlank() && artist.split(' ').size <= 3) return artist
        }
    Regex("([A-Z][\\w'.-]*(?:\\s+[A-Z][\\w'.-]*){0,2})\\s+\\b(18|19|20)\\d{2}\\b")
        .find(teaser)?.let { match ->
            val artist = match.groupValues[1].trim()
            if (artist.isNotBlank() && artist.split(' ').size <= 3) return artist
        }
    return null
}

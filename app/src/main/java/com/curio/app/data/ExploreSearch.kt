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
    runCatching {
        // v107 — focused catalog query: artist + title. byline IS the
        // artist for Album/Song topics; a trailing "(1984)" makes the API
        // return zero results, so strip it. The subtype word ("Song") adds
        // no signal here.
        val byline = topic.byline.takeIf { it.isNotBlank() }
            ?: if (topic.subtype.equals("Album", ignoreCase = true)) extractArtist(topic.teaser) else null
        val title = topic.name.replace(TRAILING_YEAR_IN_PARENS, "").trim()
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
                "album" -> first.optString("collectionViewUrl")
                "song" -> first.optString("trackViewUrl")
                else -> first.optString("artistLinkUrl")
            }
            if (web.isBlank()) return@runCatching null
            "music://" + web
                .removePrefix("https://")
                .removePrefix("http://")
                .replace(UO_TRACKING_PARAM, "")
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
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

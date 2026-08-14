package com.curio.app.data

import android.net.Uri
import java.util.Locale

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
            "https://music.apple.com$path?term=$q"
        }
        // Spotify's web search path (/search/{query}) is the correct deep
        // link: it hands off into the installed app or opens the web player.
        MusicService.SPOTIFY -> "https://open.spotify.com/search/$q"
    }
}

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

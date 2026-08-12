package com.curio.app.data

import android.net.Uri

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

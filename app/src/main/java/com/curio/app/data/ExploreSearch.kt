package com.curio.app.data

import android.net.Uri

/**
 * Builds the search URL opened when the user taps an Explore provider.
 * Music defaults to YouTube; every other category defaults to Google.
 */
fun categoryOpensYouTube(categoryId: CategoryId): Boolean =
    categoryId == CategoryId.ALBUMS || categoryId == CategoryId.ARTISTS

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

fun buildExploreSearchUrl(topic: CurioTopic): String =
    if (categoryOpensYouTube(topic.categoryId)) buildYouTubeSearchUrl(topic)
    else buildGoogleSearchUrl(topic)

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

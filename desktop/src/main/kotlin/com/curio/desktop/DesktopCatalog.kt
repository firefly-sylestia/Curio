package com.curio.desktop

import com.google.gson.Gson
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop mirror of the Android `assets/topics/*.json` schema.
 *
 * Fields that are absent from some category files (`byline` in legacy lanes,
 * `tier` in a few older topics) are nullable — Gson instantiates Kotlin data
 * classes via Unsafe, so default parameter values do NOT apply. Always access
 * through the `safe*` helpers below.
 */
data class DesktopExploreAction(
    val verb: String,
    val targetName: String,
    val durationMinutes: Int,
    val instruction: String
)

data class DesktopTopic(
    val id: String,
    val categoryId: String,
    val subtype: String,
    val name: String,
    val teaser: String,
    val imageUrl: String?,
    val exploreAction: DesktopExploreAction,
    val tags: List<String>?,
    val tier: Int?,
    val byline: String?
) {
    val safeTier: Int get() = tier ?: 1
    val safeByline: String get() = byline ?: ""
    val safeTags: List<String> get() = tags ?: emptyList()
}

/** Loads a single category file by its route slug (e.g. "animals"). */
data class DesktopCategory(
    val id: String,      // CategoryId value, e.g. "ANIMALS"
    val slug: String,    // route slug / JSON file name, e.g. "animals"
    val displayName: String
)

object DesktopCatalog {

    private val gson = Gson()
    private val cache = ConcurrentHashMap<String, List<DesktopTopic>>()

    /** All 36 lanes, in the Android app's default chip order (wildcard last). */
    val categories: List<DesktopCategory> = listOf(
        DesktopCategory("ARTISTS", "artists", "Artists"),
        DesktopCategory("ALBUMS", "albums", "Albums"),
        DesktopCategory("SONGS", "songs", "Songs"),
        DesktopCategory("DIRECTORS", "directors", "Directors"),
        DesktopCategory("FILMS", "films", "Films"),
        DesktopCategory("SERIES", "series", "Series"),
        DesktopCategory("AUTHORS", "authors", "Authors"),
        DesktopCategory("BOOKS", "books", "Books"),
        DesktopCategory("PAINTERS", "painters", "Painters"),
        DesktopCategory("ARTWORKS", "artworks", "Artworks"),
        DesktopCategory("SCIENTISTS", "scientists", "Scientists"),
        DesktopCategory("DISCOVERIES", "discoveries", "Discoveries"),
        DesktopCategory("ANIME", "anime", "Anime"),
        DesktopCategory("MANGA", "manga", "Manga"),
        DesktopCategory("MANHWA", "manhwa", "Manhwa"),
        DesktopCategory("GAMES", "games", "Games"),
        DesktopCategory("MYTHOLOGY", "mythology", "Mythology"),
        DesktopCategory("SPORTS", "sports", "Sports"),
        DesktopCategory("FOOD", "food", "Food"),
        DesktopCategory("INTERNET", "internet", "Internet"),
        DesktopCategory("BIOLOGY", "biology", "Biology"),
        DesktopCategory("CHEMISTRY", "chemistry", "Chemistry"),
        DesktopCategory("ANIMALS", "animals", "Animals"),
        DesktopCategory("PLANTS", "plants", "Plants"),
        DesktopCategory("TECHNOLOGIES", "technologies", "Technologies"),
        DesktopCategory("ASTRONOMY", "astronomy", "Astronomy"),
        DesktopCategory("HISTORY", "history", "History"),
        DesktopCategory("GEOLOGY", "geology", "Geology"),
        DesktopCategory("MEDICINE", "medicine", "Medicine"),
        DesktopCategory("PSYCHOLOGY", "psychology", "Psychology"),
        DesktopCategory("MATHEMATICS", "mathematics", "Mathematics"),
        DesktopCategory("ECONOMICS", "economics", "Economics"),
        DesktopCategory("LANGUAGE", "language", "Language"),
        DesktopCategory("ENGINEERING", "engineering", "Engineering"),
        DesktopCategory("OCEANS", "oceans", "Oceans"),
        DesktopCategory("WILDCARD", "wildcard", "Wildcard")
    )

    private val slugToName: Map<String, String> =
        categories.associate { it.slug to it.displayName }

    /**
     * Loads (and caches) the topic pool for a category slug. Wildcard merges
     * every non-wildcard lane into one big pool, mirroring the Android loader.
     */
    fun load(slug: String): List<DesktopTopic> = cache.getOrPut(slug) {
        if (slug == "wildcard") {
            categories
                .filter { it.slug != "wildcard" }
                .flatMap { load(it.slug) }
                .distinctBy { it.id }
        } else {
            parseAsset(slug)
        }
    }

    fun displayName(slug: String): String = slugToName[slug] ?: slug

    private fun parseAsset(slug: String): List<DesktopTopic> {
        val stream = javaClass.classLoader.getResourceAsStream("$slug.json")
            ?: error("Topic asset missing: $slug.json")
        val json = stream.bufferedReader().use { it.readText() }
        return gson.fromJson(json, Array<DesktopTopic>::class.java).toList()
    }
}

package com.curio.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Curio topic catalog — thin wrapper over [TopicJsonLoader].
 *
 * Topics live in `assets/topics/{categoryId}.json` and are loaded
 * lazily + cached on first request. This class provides suspend
 * helpers that all delegate to the loader, plus a sync accessor for
 * cases where the caller already knows the data is loaded (e.g.
 * Compose state that has run a LaunchedEffect).
 *
 * ## Usage pattern
 *
 * In a Composable screen, prime the cache once and then read it
 * synchronously:
 *
 * ```kotlin
 * val topics by produceState<List<CurioTopic>>(emptyList(), cat.id) {
 *     value = TopicCatalog.poolFor(cat.id)
 * }
 * ```
 *
 * Or use [randomFor] inside an event handler (it's a suspend fun so
 * wrap in rememberCoroutineScope + launch).
 *
 * ## Wildcard handling
 *
 * [poolFor] returns the full wildcard pool when [CategoryId.WILDCARD]
 * is requested. [randomFor] uniformly picks one of the 10 other
 * categories, then picks a random topic from it — keeps the Wildcard
 * spin from being dominated by whichever category happens to have
 * the most topics.
 */
object TopicCatalog {

    /**
     * Returns all topics in [id]'s pool, loading + parsing the JSON
     * file on first access. Suspends on first call only.
     */
    suspend fun poolFor(id: CategoryId): List<CurioTopic> =
        TopicJsonLoader.load(id)

    /**
     * Returns a random topic from [id]'s pool. For WILDCARD, picks a
     * uniform-random non-wildcard category first, then a random topic
     * from it (prevents category-size imbalance from biasing the
     * shuffle).
     */
    suspend fun randomFor(id: CategoryId): CurioTopic {
        val pool = poolFor(id)
        if (pool.isEmpty()) {
            throw IllegalStateException(
                "Topic pool for ${id.name} is empty — missing JSON file " +
                "or malformed content."
            )
        }
        if (id != CategoryId.WILDCARD) return pool.random()
        // Wildcard: pick a uniform-random non-wildcard category, then
        // a random topic from it.
        val nonWildcard = CategoryId.values().filter { it != CategoryId.WILDCARD }
        val chosenCat = nonWildcard.random()
        val subPool = poolFor(chosenCat)
        return subPool.randomOrNull() ?: pool.random()
    }

    /**
     * Looks up a topic by exact name across all categories.
     * Searches the already-loaded pools; returns null if no pool
     * with [name] has been loaded yet.
     *
     * Use [findByNameAcrossAll] for a guaranteed exhaustive search
     * (suspends to load every category).
     */
    fun findByName(name: String): CurioTopic? {
        CategoryId.values().forEach { id ->
            TopicJsonLoader.cached(id)?.firstOrNull { it.name == name }
                ?.let { return it }
        }
        return null
    }

    /**
     * Exhaustive lookup — loads every category's JSON if not yet
     * cached, then searches for [name]. Prefer [findByName] when
     * the topic is expected to be in the already-loaded pool.
     */
    suspend fun findByNameAcrossAll(name: String): CurioTopic? {
        // Preserve exhaustive lookup semantics without forcing a wildcard
        // merge into the cache; this remains an explicit, potentially large
        // operation for callers that truly need it.
        TopicJsonLoader.preloadAll()
        return findByName(name)
    }

    /**
     * Returns all unique tags used by topics in [id]'s pool. Used by
     * the Spin screen to render dynamic filter chips (replaces the
     * old hardcoded MusicGenre enum).
     */
    suspend fun tagsFor(id: CategoryId): List<String> =
        poolFor(id).flatMap { it.tags }.distinct().sorted()

    // ── Sample entries (sync, after preload) ───────────────────────────────
    //
    // CabinetScreen + EntryDetailScreen + TopicHistoryScreen use these
    // as visual mocks. Phase 4 swaps for Room-backed real persistence.

    /**
     * Returns a small set of pre-baked sample entries for the Cabinet
     * grid + EntryDetail preview. Each sample loads only its own category,
     * avoiding the old all-catalog preload when promo content is opened.
     *
     * The samples are constructed in-memory from a curated subset of
     * topics; they don't come from JSON because they're
     * "the user already saved this" not "this topic exists".
     */
    suspend fun sampleEntries(): List<CurioEntry> = withContext(Dispatchers.Default) {
        listOfNotNull(
            sampleFor("artist-david-bowie", CategoryId.ARTISTS, daysAgo = 1, format = CaptureFormat.SoundBite),
            sampleFor("album-david-bowie-ziggy-stardust", CategoryId.ALBUMS, daysAgo = 2, format = CaptureFormat.ReelNotes),
            sampleFor("film-2001-a-space-odyssey", CategoryId.FILMS, daysAgo = 4, format = CaptureFormat.Marginalia),
            sampleFor("book-beloved", CategoryId.BOOKS, daysAgo = 6, format = CaptureFormat.Marginalia),
            sampleFor("painter-frida-kahlo", CategoryId.PAINTERS, daysAgo = 8, format = CaptureFormat.GalleryWall),
            sampleFor("discovery-exoplanet-atmosphere", CategoryId.DISCOVERIES, daysAgo = 11, format = CaptureFormat.FieldNotes)
        )
    }

    private suspend fun sampleFor(
        topicId: String,
        categoryId: CategoryId,
        daysAgo: Int,
        format: CaptureFormat
    ): CurioEntry? {
        val topic = TopicJsonLoader.load(categoryId).firstOrNull { it.id == topicId } ?: return null
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val capturedAt = now - (daysAgo * oneDay)
        return CurioEntry(
            id = "sample-${topicId}",
            topic = topic,
            format = format,
            captureData = when (format) {
                CaptureFormat.SoundBite -> CaptureData.SoundBite(
                    durationSeconds = 45,
                    title = topic.name
                )
                CaptureFormat.ReelNotes -> CaptureData.ReelNotes(
                    rating = 4,
                    reviewText = "A fascinating exploration of ${topic.name}. Highly recommend diving into this one.",
                    imageCount = 1
                )
                CaptureFormat.Marginalia -> CaptureData.Marginalia(
                    journalText = "Spent some time reflecting on ${topic.name} today. There's so much depth here.",
                    quotes = listOf("\"Every moment is a fresh beginning.\", T.S. Eliot")
                )
                CaptureFormat.GalleryWall -> CaptureData.GalleryWall(
                    imageCount = 3,
                    caption = "A visual journey through ${topic.name}"
                )
                CaptureFormat.FieldNotes -> CaptureData.FieldNotes(
                    observed = "Noticed the intricate patterns in ${topic.name}'s work.",
                    surprised = "Was surprised by the emotional depth.",
                    learnNext = "Want to explore more from this era."
                )
                CaptureFormat.OpenNotebook -> CaptureData.OpenNotebook(
                    subFormat = CaptureFormat.ReelNotes,
                    subData = CaptureData.ReelNotes(3, "Quick notebook entry about ${topic.name}", 0)
                )
            },
            capturedAtMillis = capturedAt
        )
    }
}
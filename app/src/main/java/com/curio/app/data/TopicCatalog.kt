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
     * Looks up a topic by name across all categories. Searches the
     * already-loaded pools; returns null if no pool with [name] has been
     * loaded yet.
     *
     * v135 — tolerant matching: saved entries reference the topic name AS
     * IT WAS when saved, and data edits (the books dedupe collapsed
     * year-less entries into canonical "Name (Year)" entries) rename
     * topics underneath them — an old entry's "The Odyssey" would fail an
     * exact-name lookup against "The Odyssey (c. 8th century BCE)" and
     * the reveal would show "Loading topic…" forever. Exact match first,
     * then base-name (strip a trailing "(…)" qualifier) + containment
     * ("Moby-Dick; or, The Whale" ↔ "Moby-Dick (1851)") via
     * [CurioTopic.matchesSavedName].
     *
     * Use [findByNameAcrossAll] for a guaranteed exhaustive search
     * (suspends to load every category).
     */
    fun findByName(name: String): CurioTopic? {
        val wanted = name.trim()
        if (wanted.isEmpty()) return null
        CategoryId.values().forEach { id ->
            TopicJsonLoader.cached(id)?.firstOrNull { it.matchesSavedName(wanted) }
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

    /**
     * The app's TOTAL topic count across the ten canonical lanes
     * (wildcard excluded — it only mirrors them). Sync: reads the warm
     * cache, which the splash preload fills before the UI renders; an
     * uncached lane just contributes zero until it loads. Used by the
     * Home hero's "Topics" stat.
     */
    fun totalTopicCount(): Int =
        CategoryId.values()
            .filter { it != CategoryId.WILDCARD }
            .sumOf { TopicJsonLoader.cached(it)?.size ?: 0 }

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
            },            capturedAtMillis = capturedAt
        )
    }
}

/**
 * v135 — tolerant saved-name matching for topics whose canonical name
 * changed under a saved entry (the books dedupe collapsed "The Odyssey"
 * into "The Odyssey (c. 8th century BCE)", "Moby-Dick; or, The Whale"
 * into "Moby-Dick (1851)", etc.). Tiered: exact (case-insensitive) →
 * base-name (strip a trailing "(…)" / "— …" qualifier from either side)
 * → containment (min 4 chars, so a bare "The" can never match
 * everything). Used by [TopicCatalog.findByName] and the reveal's
 * per-pool fallback.
 */
internal fun CurioTopic.matchesSavedName(requested: String): Boolean {
    val wanted = requested.trim()
    if (wanted.isEmpty()) return false
    if (name.equals(wanted, ignoreCase = true)) return true

    fun base(s: String): String {
        val cut = s.substringBefore(" (").substringBefore(" — ")
        return cut.trim().removeSuffix(";")
    }
    val nameBase = base(name)
    val wantedBase = base(wanted)
    if (nameBase.isNotBlank() && nameBase.equals(wantedBase, ignoreCase = true)) return true

    // Containment needs a real word to anchor on — never match on a
    // 1–3 char fragment ("The", "198", "La").
    if (nameBase.length >= 4 && wantedBase.length >= 4) {
        if (nameBase.contains(wantedBase, ignoreCase = true)) return true
        if (wantedBase.contains(nameBase, ignoreCase = true)) return true
    }
    return false
}

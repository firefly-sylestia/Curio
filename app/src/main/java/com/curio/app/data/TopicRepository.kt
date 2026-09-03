package com.curio.app.data

import android.content.Context
import android.util.Log
import com.curio.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * v294 — Room-backed topic repository. Replaces TopicJsonLoader for
 * the Topic Database screen and any bulk-load path.
 *
 * Migration strategy:
 * 1. On first launch, populate Room from JSON assets (background)
 * 2. On subsequent launches, read directly from Room (instant)
 * 3. Room provides indexed search, pagination, and reactive counts
 */
object TopicRepository {

    private val initializationMutex = Mutex()
    @Volatile private var initialized = false

    /** Check if repository has been initialized (Room populated). */
    fun isInitialized() = initialized

    private var appContext: Context? = null

    /** Load topics directly from Room (for TopicJsonLoader fast path). */
    suspend fun loadFromRoom(categoryId: CategoryId): List<CurioTopic> {
        if (!initialized || appContext == null) return emptyList()
        return try {
            val db = CurioDatabase.getInstance(appContext!!)
            val dao = db.topicDao()
            dao.getByCategory(categoryId.name).map { it.toCurioTopic() }
        } catch (_: Exception) { emptyList() }
    }

    /**
     * Initialize the repository. Call from Application.onCreate.
     * Populates Room from JSON if the topics table is empty.
     */
    suspend fun init(context: Context) {
        if (initialized) return

        initializationMutex.withLock {
            if (initialized) return

            appContext = context.applicationContext
            val db = CurioDatabase.getInstance(context)
            val dao = db.topicDao()
            val count = dao.getTotalCount()

            // Populate before marking the repository ready — reading an empty
            // Room table while the import is still running would make the
            // splash hand off an empty catalog (the old code set
            // initialized=true first, letting screens query an empty table).
            if (count == 0) {
                populateFromJson(context, dao)
                // A fresh import writes the FULL catalog from JSON — remember
                // this version so the upgrade-only sync below doesn't re-parse
                // every lane again in the same release.
                AppPreferences.setTopicCatalogSyncVersion(context, BuildConfig.VERSION_CODE)
            }

            val importedCount = dao.getTotalCount()
            if (importedCount > 0) {
                // Re-sync from the JSON assets only when the app was UPDATED
                // (newly authored topics/content ship in releases), never on
                // every process restart — the parsed catalog is immutable
                // between versions, so per-launch syncs would re-parse every
                // lane of JSON for no reason. Room already holds it.
                val currentVersion = BuildConfig.VERSION_CODE
                if (AppPreferences.getTopicCatalogSyncVersion(context) != currentVersion) {
                    syncCatalogFromJson(context, dao)
                    AppPreferences.setTopicCatalogSyncVersion(context, currentVersion)
                }
                initialized = true
                // v294 — Pre-warm TopicJsonLoader caches from Room so
                // counts and topic data are available immediately on
                // restart (the in-memory caches are empty after process
                // death). Loader reads then hit the warm cache / Room fast
                // path instead of re-parsing JSON files.
                warmLoaderFromRoom(dao)
            } else {
                Log.e("TopicRepository", "Topic import completed with zero rows; will retry next launch")
            }
        }
    }

    /**
     * v294 — Pre-warm TopicJsonLoader's in-memory caches from Room data.
     * Called after init() confirms Room has topics. This prevents the
     * loader from re-parsing JSON files on every process restart.
     */
    private suspend fun warmLoaderFromRoom(dao: TopicDao) = withContext(Dispatchers.IO) {
        try {
            // Warm the per-category counts cache.
            val counts = mutableMapOf<CategoryId, Int>()
            for (cat in CategoryId.values()) {
                if (cat == CategoryId.WILDCARD) continue
                val catCount = dao.getCount(cat.name)
                if (catCount > 0) {
                    counts[cat] = catCount
                }
            }
            TopicJsonLoader.warmCountsFromRoom(counts)
            // Warm the full topic cache per category.
            for (cat in CategoryId.values()) {
                if (cat == CategoryId.WILDCARD) continue
                try {
                    val topics = dao.getByCategory(cat.name).map { it.toCurioTopic() }
                    TopicJsonLoader.warmCacheFromRoom(cat, topics)
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            Log.w("TopicRepository", "Failed to warm loader from Room: ${e.message}")
        }
    }

    /**
     * Sync the bundled catalog for every canonical category on app update.
     * Missing topics are inserted, while existing rows only receive newly
     * authored synopsis/chapter content so local catalog edits are preserved.
     */
    private suspend fun syncCatalogFromJson(context: Context, dao: TopicDao) {
        TopicJsonLoader.install(context)
        withContext(Dispatchers.IO) {
            CategoryId.values()
                .filter { it != CategoryId.WILDCARD }
                .forEach { category ->
                    runCatching {
                        val entities = TopicJsonLoader.reloadFromAssets(category)
                            .map(TopicEntity::fromCurioTopic)
                        if (entities.isNotEmpty()) {
                            dao.insertMissing(entities)
                            entities.forEach { entity ->
                                if (entity.synopsis.isNotBlank() || entity.chapters.isNotBlank() || entity.tracks.isNotBlank()) {
                                    dao.backfillContent(entity.id, entity.synopsis, entity.chapters, entity.tracks)
                                }
                            }
                        }
                    }.onFailure { error ->
                        Log.w("TopicRepository", "Failed to sync ${category.name}: ${error.message}")
                    }
                }
        }
    }

    /**
     * Populate Room database from JSON assets as a compatibility fallback.
     * Runs once on first launch, then topics are served from Room.
     *
     * KEY FIX: TopicJsonLoader.load() is a suspend fun that already awaits
     * its internal parse. We call it to get the parsed list, then write
     * directly to Room. Relying on parseAndCache's internal Room write
     * fails on first launch because `initialized` is still false when it
     * runs — so we do the Room write ourselves, right here.
     *
     * Also: do NOT set `initialized` here — the caller (init()) verifies
     * Room was actually populated before marking ready.
     */
    private suspend fun populateFromJson(context: Context, dao: TopicDao) {
        withContext(Dispatchers.IO) {
            // Ensure TopicJsonLoader is installed
            TopicJsonLoader.install(context)

            val categories = CategoryId.values().filter { it != CategoryId.WILDCARD }
            var totalInserted = 0
            for (cat in categories) {
                try {
                    // load() suspends and returns the parsed topic list
                    val topics = TopicJsonLoader.load(cat)
                    if (topics.isNotEmpty()) {
                        val entities = topics.map { TopicEntity.fromCurioTopic(it) }
                        dao.insertAll(entities)
                        totalInserted += entities.size
                    }
                } catch (e: Exception) {
                    Log.w("TopicRepository", "Failed to load category ${cat.routeSlug}: ${e.message}")
                }
            }
            Log.i("TopicRepository", "JSON→Room import: $totalInserted topics across ${categories.size} categories")
        }
    }

    /** Get all topics for a category (from Room, instant). */
    suspend fun getTopicsForCategory(context: Context, categoryId: CategoryId): List<CurioTopic> {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.getByCategory(categoryId.name).map { it.toCurioTopic() }
    }

    /** Rows hydrated once per process (a stale row triggers one JSON reload;
     *  later visits of the same row return instantly). */
    private val hydratedIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Resolve a topic within its category so duplicate names cannot cross
     * lanes. v316 — an indexed SQL LIMIT 1 lookup replaces the old
     * full-lane fetch + Kotlin scan (which mapped every row of a 10k-topic
     * lane on every reveal open).
     */
    suspend fun findTopic(context: Context, categoryId: CategoryId, name: String): CurioTopic? {
        val dao = CurioDatabase.getInstance(context).topicDao()
        val entity = dao.findByCategoryAndName(categoryId.name, name)
            ?: return null

        // Room rows can carry stale authored fields (imported before a data
        // update). Hydrate the visible topic from the JSON assets on demand
        // and persist the fresh fields. This only runs ONCE per topic per
        // process (and only for genuinely content-incomplete rows), so a
        // full-lane JSON parse never blocks the reveal.
        val needsHydration = entity.teaser.isBlank() ||
            (categoryId == CategoryId.BOOKS && entity.synopsis.isBlank()) ||
            (categoryId == CategoryId.ALBUMS &&
                (entity.tracks.isBlank() || entity.geniusUrl.isBlank()))
        if (needsHydration && hydratedIds.add(entity.id)) {
            runCatching {
                TopicJsonLoader.install(context)
                val fresh = TopicJsonLoader.reloadFromAssets(categoryId)
                    .firstOrNull { it.id == entity.id || it.name.equals(entity.name, ignoreCase = true) }
                if (fresh != null) {
                    val freshEntity = TopicEntity.fromCurioTopic(fresh)
                    dao.updateContent(
                        id = freshEntity.id,
                        teaser = freshEntity.teaser,
                        imageUrl = freshEntity.imageUrl,
                        byline = freshEntity.byline,
                        tags = freshEntity.tags,
                        synopsis = freshEntity.synopsis,
                        chapters = freshEntity.chapters,
                        tracks = freshEntity.tracks,
                        geniusUrl = freshEntity.geniusUrl
                    )
                }
            }
        }
        return dao.findById(entity.id)?.toCurioTopic() ?: entity.toCurioTopic()
    }

    /** Get a random topic for a category. */
    suspend fun getRandomTopic(context: Context, categoryId: CategoryId): CurioTopic? {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.getRandom(categoryId.name)?.toCurioTopic()
    }

    /**
     * v316 — force one lane's Room copy to match the shipped JSON. The
     * version-gated sync only re-imports on app updates, so topics added to
     * the JSON between releases can be ABSENT from Room — and because
     * [TopicJsonLoader.load] serves Room rows on its fast path, the loader
     * would then never see the new topic either (the reveal's fallback was
     * silently masked by stale Room rows). Parses the bundled asset
     * directly (bypassing the Room mask), REPLACE-upserts the whole lane
     * back into Room, and returns the fresh pool. Null only if the asset
     * parse itself failed.
     */
    suspend fun refreshLaneFromAssets(context: Context, categoryId: CategoryId): List<CurioTopic>? =
        withContext(Dispatchers.IO) {
            runCatching {
                TopicJsonLoader.install(context)
                val parsed = TopicJsonLoader.reloadFromAssets(categoryId)
                if (parsed.isNotEmpty()) {
                    val dao = CurioDatabase.getInstance(context).topicDao()
                    dao.insertAll(parsed.map { TopicEntity.fromCurioTopic(it) })
                }
                parsed
            }.getOrNull()
        }

    /** Search topics across all categories (title-first priority). */
    suspend fun search(context: Context, query: String, categoryId: CategoryId? = null): List<CurioTopic> {
        val dao = CurioDatabase.getInstance(context).topicDao()
        val entities = if (categoryId != null) {
            dao.searchInCategory(categoryId.name, query)
        } else {
            dao.search(query)
        }
        return entities.map { it.toCurioTopic() }
    }

    /** Get topic count for a category. */
    suspend fun getCount(context: Context, categoryId: CategoryId): Int {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.getCount(categoryId.name)
    }

    /** Get total topic count across all categories. */
    suspend fun getTotalCount(context: Context): Int {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.getTotalCount()
    }

    /** Find topic by name. */
    suspend fun findByName(context: Context, name: String): CurioTopic? {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.findByName(name)?.toCurioTopic()
    }

    /** Find topic by ID. */
    suspend fun findById(context: Context, id: String): CurioTopic? {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.findById(id)?.toCurioTopic()
    }

    /** Get paginated topics for a category. */
    suspend fun getPaginated(context: Context, categoryId: CategoryId, page: Int, pageSize: Int): List<CurioTopic> {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.getPaginated(categoryId.name, pageSize, page * pageSize).map { it.toCurioTopic() }
    }

    /** Get paginated topics across all categories. */
    suspend fun getAllPaginated(context: Context, page: Int, pageSize: Int): List<CurioTopic> {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.getAllPaginated(pageSize, page * pageSize).map { it.toCurioTopic() }
    }

    /** Check if a category has topics loaded. */
    suspend fun hasTopics(context: Context, categoryId: CategoryId): Boolean {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.hasTopics(categoryId.name)
    }

    /**
     * Persist an EXPLORED topic into the durable `cached_topics` table so it
     * survives even a catalog-table wipe and never needs re-parsing. Called
     * from the reveal the moment a topic resolves (cheap insurance for
     * "always keeps loaded"). Saving to the Cabinet already upserts via this
     * same table (CaptureRepository.save).
     */
    suspend fun rememberTopic(context: Context, topic: CurioTopic) {
        runCatching {
            CurioDatabase.getInstance(context).cachedTopicDao()
                .upsert(CachedTopicEntity.fromCurioTopic(topic))
        }
    }
}

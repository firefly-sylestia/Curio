package com.curio.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
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
  val bundledCount = TopicAssetStore.count(context)
  if (bundledCount > 0) {
  initialized = true
  Log.i("TopicRepository", "Opened bundled topic database with $bundledCount rows")
  } else {
  Log.e("TopicRepository", "Bundled topic database is empty; will retry next launch")
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

    /** Rebuild the Room topics table from the bundled SQLite asset. */
    suspend fun importBundledRoomDatabase(context: Context): Int = withContext(Dispatchers.IO) {
        initializationMutex.withLock { importBundledRoomDatabaseLocked(context) }
    }

    private suspend fun importBundledRoomDatabaseLocked(context: Context): Int = withContext(Dispatchers.IO) {
            val db = CurioDatabase.getInstance(context)
            val dao = db.topicDao()
            val assetFile = File(context.cacheDir, "topics-import.db")
            try {
                context.assets.open("topics.db").use { input ->
                    assetFile.outputStream().use { output -> input.copyTo(output) }
                }
                SQLiteDatabase.openDatabase(assetFile.path, null, SQLiteDatabase.OPEN_READONLY).use { source ->
                    source.rawQuery("SELECT * FROM topics", null).use { cursor ->
                        val imported = mutableListOf<TopicEntity>()
                        while (cursor.moveToNext()) {
                            imported += TopicEntity(
                                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                                categoryId = cursor.getString(cursor.getColumnIndexOrThrow("categoryId")),
                                subtype = cursor.getString(cursor.getColumnIndexOrThrow("subtype")),
                                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                                teaser = cursor.getString(cursor.getColumnIndexOrThrow("teaser")),
                                imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("imageUrl")),
                                byline = cursor.getString(cursor.getColumnIndexOrThrow("byline")),
                                tags = cursor.getString(cursor.getColumnIndexOrThrow("tags")),
                                tier = cursor.getInt(cursor.getColumnIndexOrThrow("tier")),
                                exploreVerb = cursor.getString(cursor.getColumnIndexOrThrow("exploreVerb")),
                                exploreTargetName = cursor.getString(cursor.getColumnIndexOrThrow("exploreTargetName")),
                                exploreDurationMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("exploreDurationMinutes")),
                                exploreInstruction = cursor.getString(cursor.getColumnIndexOrThrow("exploreInstruction")),
                                pageCount = cursor.getNullableInt("pageCount"),
                                episodeCount = cursor.getNullableInt("episodeCount"),
                                altPageLabel = cursor.getString(cursor.getColumnIndexOrThrow("altPageLabel")),
                                altPageCount = cursor.getNullableInt("altPageCount"),
                                synopsis = try { cursor.getString(cursor.getColumnIndexOrThrow("synopsis")) } catch (_: Exception) { "" },
                                chapters = try { cursor.getString(cursor.getColumnIndexOrThrow("chapters")) } catch (_: Exception) { "" }
                            )
                        }
                        require(imported.isNotEmpty()) { "Bundled topics.db contains no topics" }
                        dao.deleteAll()
                        dao.insertAll(imported)
                        initialized = true
                        imported.size
                    }
                }
            } finally {
                assetFile.delete()
            }
        }

    private fun android.database.Cursor.getNullableInt(column: String): Int? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getInt(index)
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
                                if (entity.synopsis.isNotBlank() || entity.chapters.isNotBlank()) {
                                    dao.backfillContent(entity.id, entity.synopsis, entity.chapters)
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
        try {
            importBundledRoomDatabaseLocked(context)
            return
        } catch (error: Exception) {
            Log.w("TopicRepository", "Bundled Room asset unavailable; falling back to JSON", error)
        }
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
  suspend fun getTopicsForCategory(
  context: Context,
  categoryId: CategoryId,
  limit: Int = 50,
  offset: Int = 0,
  ): List<CurioTopic> = withContext(Dispatchers.IO) {
  TopicAssetStore.byCategory(context, categoryId.name, limit.coerceIn(1, 100), offset.coerceAtLeast(0))
  .map { it.toCurioTopic() }
  }

    /** Resolve a topic within its category so duplicate names cannot cross lanes. */
    suspend fun findTopic(context: Context, categoryId: CategoryId, name: String): CurioTopic? {
        val dao = CurioDatabase.getInstance(context).topicDao()
        val entity = dao.getByCategory(categoryId.name)
            .firstOrNull { it.name == name || it.name.equals(name, ignoreCase = true) }
            ?: return null

        // A bundled topics.db can contain the catalog shell while newer
        // authored fields live in the JSON assets. Hydrate the visible topic
        // on demand, persist it, and return the hydrated row immediately.
        if (entity.teaser.isBlank() || entity.synopsis.isBlank() && categoryId == CategoryId.BOOKS) {
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
                        chapters = freshEntity.chapters
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
}

package com.curio.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        initialized = true
        appContext = context.applicationContext

        val db = CurioDatabase.getInstance(context)
        val dao = db.topicDao()

        // Check if topics table is empty (first launch)
        val count = dao.getTotalCount()
        if (count == 0) {
            // Populate from JSON in background
            populateFromJson(context, dao)
        }
    }

    /**
     * Populate Room database from JSON assets.
     * Runs once on first launch, then topics are served from Room.
     */
    private suspend fun populateFromJson(context: Context, dao: TopicDao) {
        withContext(Dispatchers.IO) {
            // Ensure TopicJsonLoader is installed
            TopicJsonLoader.install(context)

            val categories = CategoryId.values().filter { it != CategoryId.WILDCARD }
            for (cat in categories) {
                try {
                    val topics = TopicJsonLoader.load(cat)
                    val entities = topics.map { TopicEntity.fromCurioTopic(it) }
                    dao.insertAll(entities)
                } catch (_: Exception) {
                    // Skip categories that fail to load
                }
            }
        }
    }

    /** Get all topics for a category (from Room, instant). */
    suspend fun getTopicsForCategory(context: Context, categoryId: CategoryId): List<CurioTopic> {
        val dao = CurioDatabase.getInstance(context).topicDao()
        return dao.getByCategory(categoryId.name).map { it.toCurioTopic() }
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

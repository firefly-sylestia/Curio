package com.curio.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * v294 — Room DAO for topics. Provides instant indexed queries
 * replacing the slow JSON parse on every category load.
 */
@Dao
interface TopicDao {

    /** Insert all topics for a category (bulk, replaces on conflict). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<TopicEntity>)

    /** Add newly authored topics without overwriting existing local rows. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(topics: List<TopicEntity>)

    /** Get all topics for a category, sorted by name. */
    @Query("SELECT * FROM topics WHERE categoryId = :categoryId ORDER BY name ASC")
    suspend fun getByCategory(categoryId: String): List<TopicEntity>

    /** Get all topics across all categories, sorted by name. */
    @Query("SELECT * FROM topics ORDER BY name ASC")
    suspend fun getAll(): List<TopicEntity>

    /** Get a random topic for a category. */
    @Query("SELECT * FROM topics WHERE categoryId = :categoryId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(categoryId: String): TopicEntity?

    /** Get a small random sample of topics for a category (indexed LIMIT
     *  query — never maps the whole lane). Seeds the Spin deck instantly
     *  while the full pool loads. */
    @Query("SELECT * FROM topics WHERE categoryId = :categoryId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomTopics(categoryId: String, limit: Int): List<TopicEntity>

    /** Search topics by name, byline, teaser, or tags. Title matches first. */
    @Query("""
        SELECT * FROM topics 
        WHERE name LIKE '%' || :query || '%' 
           OR byline LIKE '%' || :query || '%'
           OR teaser LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN name LIKE :query || '%' THEN 0
                 WHEN name LIKE '%' || :query || '%' THEN 1
                 ELSE 2 END,
            name ASC
    """)
    suspend fun search(query: String): List<TopicEntity>

    /** Search within a specific category. */
    @Query("""
        SELECT * FROM topics 
        WHERE categoryId = :categoryId
          AND (name LIKE '%' || :query || '%' 
               OR byline LIKE '%' || :query || '%'
               OR teaser LIKE '%' || :query || '%'
               OR tags LIKE '%' || :query || '%')
        ORDER BY 
            CASE WHEN name LIKE :query || '%' THEN 0
                 WHEN name LIKE '%' || :query || '%' THEN 1
                 ELSE 2 END,
            name ASC
    """)
    suspend fun searchInCategory(categoryId: String, query: String): List<TopicEntity>

    /** Backfill newly authored content without replacing existing values. */
    @Query("""
        UPDATE topics
        SET synopsis = CASE WHEN synopsis = '' THEN :synopsis ELSE synopsis END,
            chapters = CASE WHEN chapters = '' THEN :chapters ELSE chapters END,
            tracks = CASE WHEN tracks = '' THEN :tracks ELSE tracks END
        WHERE id = :id
    """)
    suspend fun backfillContent(id: String, synopsis: String, chapters: String, tracks: String)

    /** Update the authored fields for a topic hydrated on the reveal screen. */
    @Query("""
        UPDATE topics SET teaser = :teaser, imageUrl = :imageUrl, byline = :byline,
            tags = :tags, synopsis = :synopsis, chapters = :chapters, tracks = :tracks,
            geniusUrl = :geniusUrl
        WHERE id = :id
    """)
    suspend fun updateContent(
        id: String,
        teaser: String,
        imageUrl: String,
        byline: String,
        tags: String,
        synopsis: String,
        chapters: String,
        tracks: String,
        geniusUrl: String
    )

    /** Get topic count for a category. */
    @Query("SELECT COUNT(*) FROM topics WHERE categoryId = :categoryId")
    suspend fun getCount(categoryId: String): Int

    /** Get total topic count across all categories. */
    @Query("SELECT COUNT(*) FROM topics")
    suspend fun getTotalCount(): Int

    /** Check if a category has any topics loaded. */
    @Query("SELECT COUNT(*) > 0 FROM topics WHERE categoryId = :categoryId")
    suspend fun hasTopics(categoryId: String): Boolean

    /** Get all unique category IDs that have topics. */
    @Query("SELECT DISTINCT categoryId FROM topics")
    suspend fun getLoadedCategories(): List<String>

    /** Find topic by name (exact match). */
    @Query("SELECT * FROM topics WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TopicEntity?

    /** v316 — indexed SQL lookup within one lane (case-insensitive on the
     *  display name): the reveal's resolution used to fetch the WHOLE lane
     *  and scan it in Kotlin — a 10k-row mapping per open. LIMIT 1 at the
     *  database means microseconds for any topic. */
    @Query("""
        SELECT * FROM topics
        WHERE categoryId = :categoryId AND (name = :name OR name = :name COLLATE NOCASE)
        LIMIT 1
    """)
    suspend fun findByCategoryAndName(categoryId: String, name: String): TopicEntity?

    /** Find topic by ID. */
    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TopicEntity?

    /** Delete all topics for a category (for re-download). */
    @Query("DELETE FROM topics WHERE categoryId = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    /** Delete all topics (for full refresh). */
    @Query("DELETE FROM topics")
    suspend fun deleteAll()

    /** Get paginated topics for a category. */
    @Query("SELECT * FROM topics WHERE categoryId = :categoryId ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getPaginated(categoryId: String, limit: Int, offset: Int): List<TopicEntity>

    /** Get paginated topics across all categories. */
    @Query("SELECT * FROM topics ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllPaginated(limit: Int, offset: Int): List<TopicEntity>

    /** Observe topic count for reactive UI. */
    @Query("SELECT COUNT(*) FROM topics")
    fun observeTotalCount(): Flow<Int>
}

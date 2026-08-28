package com.curio.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * v294 — Room DAO for cached topics. Stores full [CurioTopic] data at
 * save-time so entries survive topic deletion from bundled JSON assets.
 *
 * Idempotent: REPLACE on conflict ensures re-saving the same topic
 * updates its cached data without error.
 */
@Dao
interface CachedTopicDao {

    /** Cache a topic (insert or update on conflict). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(topic: CachedTopicEntity)

    /** Cache multiple topics at once. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(topics: List<CachedTopicEntity>)

    /** Look up a cached topic by ID. Returns null if not cached. */
    @Query("SELECT * FROM cached_topics WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedTopicEntity?

    /** Look up multiple cached topics by IDs. */
    @Query("SELECT * FROM cached_topics WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<CachedTopicEntity>

    /** Check if a topic is already cached. */
    @Query("SELECT EXISTS(SELECT 1 FROM cached_topics WHERE id = :id)")
    suspend fun isCached(id: String): Boolean

    /** Count of cached topics. */
    @Query("SELECT COUNT(*) FROM cached_topics")
    suspend fun getCount(): Int

    /** Delete a cached topic by ID. */
    @Query("DELETE FROM cached_topics WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Delete all cached topics. */
    @Query("DELETE FROM cached_topics")
    suspend fun deleteAll()
}

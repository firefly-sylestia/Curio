package com.curio.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(capture: CaptureEntity)

    @Delete
    suspend fun delete(capture: CaptureEntity)

    // v5 — every live-list query filters out recycled captures
    // (`deletedAt IS NULL`); the recycle bin queries handle the rest.
    @Query("SELECT * FROM captures WHERE deletedAt IS NULL ORDER BY capturedAtMillis DESC")
    fun getAllFlow(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE deletedAt IS NULL ORDER BY capturedAtMillis DESC")
    suspend fun getAll(): List<CaptureEntity>

    @Query("SELECT * FROM captures WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY capturedAtMillis DESC")
    suspend fun getByCategory(categoryId: String): List<CaptureEntity>

    @Query("DELETE FROM captures WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM captures WHERE deletedAt IS NULL")
    suspend fun count(): Int

    /** Wipe every capture — used by restore-from-backup before re-inserting. */
    @Query("DELETE FROM captures")
    suspend fun clearAll(): Int

    // ── Recycle bin (v5) — soft delete, restore, permanent purge ───────────

    /** Move a capture to the recycle bin (stamps the soft-delete timestamp). */
    @Query("UPDATE captures SET deletedAt = :now WHERE id = :id")
    suspend fun softDeleteById(id: String, now: Long)

    /** Move several captures to the recycle bin in one statement. */
    @Query("UPDATE captures SET deletedAt = :now WHERE id IN (:ids)")
    suspend fun softDeleteByIds(ids: List<String>, now: Long): Int

    @Query("SELECT * FROM captures WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedFlow(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun getTrashedById(id: String): CaptureEntity?

    @Query("UPDATE captures SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreById(id: String): Int

    @Query("UPDATE captures SET deletedAt = NULL")
    suspend fun restoreAll(): Int

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun purgeById(id: String)

    @Query("DELETE FROM captures WHERE deletedAt IS NOT NULL")
    suspend fun purgeTrashed(): Int

    @Query("SELECT COUNT(*) FROM captures WHERE deletedAt IS NOT NULL")
    suspend fun countTrashed(): Int
}

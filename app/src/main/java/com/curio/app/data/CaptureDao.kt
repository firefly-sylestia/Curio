package com.curio.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * v3xx37 — the LIGHT row projection for the Cabinet list flows: every
 * column the grid screens need (topic identity, format, timestamps, tags,
 * legacy flag, progress metadata) WITHOUT the big payload blobs
 * (`formatDataJson` / `sessionNote` / `sessionScreenshotsJson`). A large
 * archive's payload JSON was being re-read from SQLite and re-allocated as
 * fresh Strings on EVERY flow emission (Room materializes every selected
 * column on every re-query), which is what made the Cabinet lag — GBs of
 * churn over a session, GC pause after GC pause. Only REELNOTES rows keep
 * their payload (`formatDataJsonLight`): the Everything review cards render
 * the rating + review text. Everything else arrives payload-free — the grid
 * cards never render payload content (title / format / time only), and
 * multi-section Portfolio takes fall back to the single-glyph badge in the
 * grid (the payload is only needed for the stacked badge).
 */
data class CaptureEntityLight(
    val id: String,
    val topicId: String,
    val categoryId: String,
    val topicName: String,
    val topicSubtype: String,
    val topicTeaser: String,
    val format: String,          // CaptureFormat enum name
    val capturedAtMillis: Long,
    val title: String?,
    val tagsJson: String,
    val isLegacy: Boolean,
    val sessionTimeMillis: Long,
    val pageCount: Int?,
    val episodeCount: Int?,
    /** ReelNotes rows only — the payload of every other row stays in the
     *  DB (never read). */
    val formatDataJsonLight: String?
)

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

    // v3xx37 — the LIGHT projection: same ordering/filter as [getAllFlow]
    // but the payload blobs stay unread (only ReelNotes rows carry theirs,
    // aliased so Room maps it into [CaptureEntityLight.formatDataJsonLight]).
    @Query(
        """SELECT id, topicId, categoryId, topicName, topicSubtype, topicTeaser,
           format, capturedAtMillis, title, tagsJson, isLegacy,
           sessionTimeMillis, pageCount, episodeCount,
           CASE WHEN format = 'ReelNotes' THEN formatDataJson ELSE NULL END AS formatDataJsonLight
           FROM captures WHERE deletedAt IS NULL ORDER BY capturedAtMillis DESC"""
    )
    fun getLightFlow(): Flow<List<CaptureEntityLight>>

    @Query("SELECT * FROM captures WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): CaptureEntity?

    // v3xx — targeted row observer for the entry-detail screen: the detail
    // page used to collect the WHOLE table and linear-scan for its id on
    // every emission (plus rebuilding all sample entries on a miss) — a
    // full-table decode + scan per DB change. Observing the single row by
    // primary key keeps the open-edit-reflect cycle O(1).
    @Query("SELECT * FROM captures WHERE id = :id AND deletedAt IS NULL")
    fun getByIdFlow(id: String): Flow<CaptureEntity?>

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

    /** One-shot snapshot of every recycled capture (for the auto-delete sweep). */
    @Query("SELECT * FROM captures WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getTrashed(): List<CaptureEntity>

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

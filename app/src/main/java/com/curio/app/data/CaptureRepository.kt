package com.curio.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import java.util.UUID

/**
 * Repository wrapping Room DAO for capture CRUD operations.
 *
 * Provides coroutine-friendly APIs and handles entity ↔ domain model conversion.
 */
class CaptureRepository(private val dao: CaptureDao) {

    /** Observe all captures as [CurioEntry] flow for reactive UI updates. */
    fun observeAll(): Flow<List<CurioEntry>> =
        // Entity → domain conversion includes Gson decoding and topic lookup;
        // keep that work off the Compose/main collector so opening Cabinet
        // stays responsive even with a large restored FieldMind archive.
        dao.getAllFlow()
            .map { entities -> entities.map { it.toEntry() } }
            .flowOn(Dispatchers.Default)

    /** Get all captures (one-shot). */
    suspend fun getAll(): List<CurioEntry> =
        dao.getAll().map { it.toEntry() }

    /** Get captures filtered by category. */
    suspend fun getByCategory(categoryId: CategoryId): List<CurioEntry> =
        dao.getByCategory(categoryId.name).map { it.toEntry() }

    /** Save a new capture. Returns the generated entry ID. */
    suspend fun save(entry: CurioEntry) {
        dao.insert(entry.toEntity())
    }

    /** Get a single capture by ID. */
    suspend fun getById(id: String): CurioEntry? =
        dao.getById(id)?.toEntry()

    /** Permanently delete a capture by ID (recycle-bin purge / import cleanup). */
    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }

    /** Permanently delete several captures (recycle-bin purge). */
    suspend fun deleteByIds(ids: Collection<String>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids.toList())
    }

    // ── Recycle bin (v5) ───────────────────────────────────────────────────

    /** Move a capture to the recycle bin — it stays recoverable. */
    suspend fun softDeleteById(id: String) {
        dao.softDeleteById(id, System.currentTimeMillis())
    }

    /** Move several captures to the recycle bin in one statement. */
    suspend fun softDeleteByIds(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        return dao.softDeleteByIds(ids.toList(), System.currentTimeMillis())
    }

    /** Observe recycled captures (newest-deleted first) as [CurioEntry]s. */
    fun observeTrashed(): Flow<List<CurioEntry>> =
        dao.getTrashedFlow()
            .map { entities -> entities.map { it.toEntry() } }
            .flowOn(Dispatchers.Default)

    /** One-shot snapshot of every recycled capture (for the expiry sweep). */
    suspend fun getTrashed(): List<CurioEntry> =
        dao.getTrashed().map { it.toEntry() }

    /** Get one recycled capture by ID (null when not in the bin). */
    suspend fun getTrashedById(id: String): CurioEntry? =
        dao.getTrashedById(id)?.toEntry()

    /** Restore a capture from the recycle bin back into the Cabinet. */
    suspend fun restoreById(id: String) {
        dao.restoreById(id)
    }

    /** Restore every recycled capture at once. */
    suspend fun restoreAll() {
        dao.restoreAll()
    }

    /** Permanently delete one recycled capture. */
    suspend fun purgeById(id: String) {
        dao.purgeById(id)
    }

    /** Permanently delete every recycled capture. Returns the deleted count. */
    suspend fun purgeTrashed(): Int = dao.purgeTrashed()

    /** Count captures currently in the recycle bin. */
    suspend fun countTrashed(): Int = dao.countTrashed()

    /** Count total captures (live entries only). */
    suspend fun count(): Int = dao.count()

    /** Wipe every capture (restore-from-backup). Returns deleted count. */
    suspend fun clearAll(): Int = dao.clearAll()

    companion object {
        fun createId(): String = UUID.randomUUID().toString()
    }
}

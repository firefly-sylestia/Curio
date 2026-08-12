package com.curio.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v27 — recycle-bin auto-delete: soft-deleted captures older than the user's
 * chosen retention window are permanently purged (with their attached media)
 * whenever the app starts and whenever the recycle bin opens. The window is
 * customizable from the Recycle bin screen (AppPreferences, 0 = keep forever).
 */
object RecycleBinExpiry {

    /** Purge recycled captures whose [CurioEntry.deletedAt] has passed the window. */
    suspend fun purgeExpired(context: Context) {
        val days = AppPreferences.getRecycleBinExpiryDays(context)
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        val trashed = CurioRepositoryHolder.repo.getTrashed()
        if (trashed.isEmpty()) return
        // Media deletion is plain file I/O — keep it off the main thread (this
        // also runs on every cold start). Room calls offload on their own.
        withContext(Dispatchers.IO) {
            trashed.forEach { entry ->
                val deletedAt = entry.deletedAt ?: return@forEach
                if (deletedAt < cutoff) {
                    // Permanent: only now are the recording + images finally
                    // removed, mirroring RecycleBinScreen.purgeWithMedia.
                    entry.captureData.audioFilePaths().forEach { path ->
                        AudioStorageManager.deleteAudio(context, path)
                    }
                    ImageStorageManager.deleteImagesForEntry(context, entry.id)
                    runCatching { CurioRepositoryHolder.repo.purgeById(entry.id) }
                }
            }
        }
    }
}

package com.curio.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File

/**
 * v27 — owns the app-private session-screenshot files.
 *
 * Screenshots captured during an explore session (the bubble's capture
 * button via MediaProjection, or auto-attached device screenshots) are
 * COPIED into `filesDir/session-shots/` so the session note page and saved
 * entries always have a stable, private path — independent of MediaStore
 * cleanup or permission changes. The saved entry stores these paths in
 * `sessionScreenshotsJson`.
 */
object SessionShots {

    private const val SHOT_DIR = "session-shots"

    private fun shotsDir(context: Context): File =
        File(context.filesDir, SHOT_DIR).apply { mkdirs() }

    /** Saves a captured frame as a PNG and returns its absolute path. */
    fun save(context: Context, bitmap: Bitmap): String {
        val file = File(shotsDir(context), "shot-${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    /**
     * Copies the image at [uri] (a MediaStore content URI from the photo
     * picker or the device-screenshot watcher) into the session-shots
     * directory. Returns the new absolute path, or null when the copy fails.
     */
    fun copyFrom(context: Context, uri: Uri): String? = try {
        val dest = File(shotsDir(context), "shot-${System.currentTimeMillis()}.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        dest.absolutePath
    } catch (_: Exception) {
        null
    }

    /**
     * Deletes one session screenshot. Hardened like [ImageStorageManager]:
     * only ever removes files inside the session-shots directory, so a
     * crafted path can never delete outside it.
     */
    fun delete(context: Context, path: String) {
        runCatching {
            val file = File(path)
            val root = shotsDir(context).canonicalPath
            if (file.canonicalPath.startsWith(root)) file.delete()
        }
    }
}

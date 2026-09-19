package com.curio.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages persistent audio file storage beyond the temp cache.
 *
 * Audio recordings start in the cache directory (created by [AudioRecorder]).
 * On save, they're copied to the app's internal storage (`filesDir/audio/`)
 * so they survive cache eviction. On entry deletion, the file is cleaned up.
 *
 * v405 — AND A JOURNAL VOICE NOTE ALSO LEAVES THE APP ENTIRELY. The internal
 * copy dies with the app: uninstall it by mistake and every recording the member
 * ever made goes with it (the member's ask: "those voice recording saves in the
 * device too woth a folder named your journal voices so it doesnt delete even
 * after deleing the app by mistake"). [publishVoice] writes the recording into
 * the device's own media store, under `Music/Your Journal Voices`, named by the
 * day it was made. Per Android's own storage contract, files contributed to the
 * media store **remain on the device after the app is uninstalled** — that is
 * the whole point of the folder, and why the note is read back through a
 * `content://` URI rather than a path. The internal copy is kept only as the
 * FALLBACK for a platform that refuses the publish (see [deleteAudio] for the
 * matching delete rule).
 */
object AudioStorageManager {

    private const val AUDIO_DIR = "audio"

    /**
     * The device folder every journal voice note is copied into, inside the
     * user's own music library. Shown in Files, in any music player and in
     * Android's own media scans, which is what makes a mistaken uninstall
     * survivable.
     */
    private const val PUBLIC_VOICE_FOLDER = "Your Journal Voices"

    /**
     * The name a voice note is filed under: the DAY and the moment it was made,
     * to the second, so a folder of them reads as a diary in date order (the
     * member's ask: "name the voice note based on date"). Local time, because
     * that is the time the member was living in.
     */
    private fun voiceStamp(whenMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.US).format(Date(whenMillis))

    /** Result of persisting an audio file: the destination path and file size. */
    data class PersistResult(
        val persistentPath: String,
        val fileSizeBytes: Long
    )

    /**
     * Copy an audio file from a cache path to persistent internal storage.
     *
     * @param context       Android context for accessing filesDir.
     * @param cacheFilePath Absolute path to the temp cache file.
     * @param entryId       The capture entry ID (used as the persistent filename).
     * @return [PersistResult] with the persisted path and file size in bytes.
     * @throws IllegalArgumentException if the source is missing/empty or the
     *         destination copy does not produce a non-empty file.
     */
    fun persistAudio(context: Context, cacheFilePath: String, entryId: String): PersistResult {
        val cacheFile = File(cacheFilePath)
        require(cacheFile.isFile && cacheFile.length() > 0L) {
            "The temporary recording is missing or empty."
        }

        require(isSafeStorageSegment(entryId)) {
            "Unsafe audio entry id."
        }
        val audioDir = File(context.filesDir, AUDIO_DIR).apply { mkdirs() }
        val destFile = File(audioDir, "${entryId}.m4a")
        require(isContainedFile(audioDir, destFile)) {
            "The audio destination escaped app-private storage."
        }
        // Editing an existing voice note already points at this destination.
        // Do not copy a file onto itself; just reuse the verified persistent
        // file so editing title/notes never makes the entry disappear.
        if (cacheFile.canonicalFile != destFile.canonicalFile) {
            cacheFile.copyTo(destFile, overwrite = true)
        }
        require(destFile.isFile && destFile.length() > 0L) {
            "The recording could not be persisted."
        }
        return PersistResult(destFile.absolutePath, destFile.length())
    }

    /**
     * Restore an audio file bundled in a backup into persistent storage.
     *
     * Writes the exact bytes to `filesDir/audio/{entryId}.m4a` — the same
     * name convention as [persistAudio] — so the restored capture's
     * `audioFilePath` resolves immediately.
     *
     * @param entryId The capture entry ID (used as the persistent filename).
     * @param bytes   The audio bytes from the backup payload.
     * @return The absolute path the file was written to.
     */
    fun restoreAudio(context: Context, entryId: String, bytes: ByteArray): String {
        require(isSafeStorageSegment(entryId)) {
            "Unsafe audio entry id."
        }
        val audioDir = File(context.filesDir, AUDIO_DIR).apply { mkdirs() }
        val destFile = File(audioDir, "${entryId}.m4a")
        require(isContainedFile(audioDir, destFile)) {
            "The audio destination escaped app-private storage."
        }
        destFile.writeBytes(bytes)
        return destFile.absolutePath
    }

    /**
     * Copy a finished journal recording into the DEVICE's own media store, under
     * `Music/Your Journal Voices`, named by the day it was made.
     *
     * This is what makes a voice note outlive the app: a file the app contributes
     * to the media store stays on the device when the app is uninstalled, so an
     * accidental uninstall cannot take the member's recordings with it.
     *
     * @param sourcePath the app's own copy of the recording (the persistent
     *        `filesDir/audio/…` file — publishing from the copy means a refused
     *        publish still leaves a playable note behind).
     * @param whenMillis the moment the recording was made, which becomes its
     *        name in the folder.
     * @return the `content://` URI the note now lives at, or **null** when the
     *         platform would not take it — Android 9 and below have no scoped
     *         media store, and any failure (no space, a locked volume) is a
     *         reason to keep the internal copy rather than lose the note.
     */
    fun publishVoice(context: Context, sourcePath: String, whenMillis: Long): String? {
        // RELATIVE_PATH and a scoped insert are Android 10+. Below that the
        // device's shared storage is a plain filesystem this app has no write
        // permission for (the manifest declares no WRITE_EXTERNAL_STORAGE), so
        // the internal copy is the honest answer there.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val source = File(sourcePath)
        if (!source.isFile || source.length() == 0L) return null

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "${voiceStamp(whenMillis)}.m4a")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(
                MediaStore.Audio.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MUSIC}/$PUBLIC_VOICE_FOLDER"
            )
            // IS_PENDING keeps the half-written file out of every other app's
            // media scan; it is cleared once the bytes are all there.
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val uri = runCatching {
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return null

        val written = runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("the media store gave no output stream")
        }.isSuccess

        if (!written) {
            // A partial row would show up as a broken file the member cannot
            // play, so it goes before the fallback is reported.
            runCatching { resolver.delete(uri, null, null) }
            return null
        }

        runCatching {
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null
            )
        }
        return uri.toString()
    }

    /**
     * Delete an audio file at [audioFilePath]. Safe to call with any path — only
     * deletes if the file exists under `filesDir/audio/`.
     *
     * v405 — AND A PUBLISHED VOICE NOTE IS DELETED THROUGH ITS OWN URI. A journal
     * recording now lives in the device's media store, so deleting the note it
     * belongs to has to remove the media row (and, with it, the file); nothing
     * under `filesDir/audio/` is involved. Deleting a note still deletes its
     * recording — the folder exists to survive an *uninstall*, not a delete.
     */
    fun deleteAudio(context: Context, audioFilePath: String?) {
        if (audioFilePath.isNullOrBlank()) return

        if (audioFilePath.startsWith("content://")) {
            runCatching { context.contentResolver.delete(Uri.parse(audioFilePath), null, null) }
            return
        }

        val file = File(audioFilePath)
        if (!file.exists()) return

        // Only delete files under our audio directory (safety guard)
        val audioDir = File(context.filesDir, AUDIO_DIR)
        if (isContainedFile(audioDir, file)) {
            file.delete()
        }
    }

    /** Delete all audio files in the persistent audio directory. */
    fun deleteAllAudio(context: Context) {
        val audioDir = File(context.filesDir, AUDIO_DIR)
        if (audioDir.exists()) {
            audioDir.listFiles()?.forEach { it.delete() }
        }
    }
}

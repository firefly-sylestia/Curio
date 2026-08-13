package com.curio.app.infrastructure

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.SessionShots
import java.util.Collections
import java.util.HashSet

/**
 * v27 — auto-attaches device screenshots to the active explore session.
 *
 * When the user takes a screenshot with their own phone buttons while an
 * explore session is running (or while a handed-off "write it down" package
 * is still pending after the session ends), the shot appears in
 * Pictures/Screenshots. This watcher registers a [ContentObserver] on
 * MediaStore, and every new screenshot is copied into app-private storage
 * ([SessionShots]) and appended to the session's screenshot list — so the
 * bubble, the note page, and every entry saved from the session share it.
 *
 * "Even after the session finishes": the watcher keeps appending to the
 * PENDING write package (the handoff survives the session being cleared),
 * so a screenshot taken while the user is on the write-it-down page still
 * lands in that entry.
 *
 * Lifecycle: registered from MainActivity while the app is alive; each
 * change event does a cheap MediaStore query for rows newer than the last
 * seen one. Requires READ_MEDIA_IMAGES (33+) / READ_EXTERNAL_STORAGE —
 * when missing, the watcher silently no-ops.
 */
object DeviceScreenshotWatcher {

    private var observer: ContentObserver? = null
    // The most recent MediaStore row id we've already processed — the query
    // only fetches rows strictly newer than this, so the scan is cheap and
    // nothing is ever double-attached.
    private var lastSeenId = -1L
    private val seenPaths: MutableSet<String> = Collections.synchronizedSet(HashSet())

    // v27t — the scan does a MediaStore query plus a FULL FILE COPY of the
    // screenshot (SessionShots.copyFrom). Running that on the main thread
    // froze the app at the exact moment the system is still writing and
    // indexing the file, so the heavy work runs on a single serialized
    // background thread and only the session-state update hops back to main.
    private val scanThread by lazy {
        HandlerThread("curio-screenshot-scan").also { it.start() }
    }
    private val scanHandler by lazy { Handler(scanThread.looper) }
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Registers the MediaStore observer (idempotent). */
    fun start(context: Context) {
        if (observer != null) return
        // Seed the watermark with the current newest row so only screenshots
        // taken AFTER registration are considered — never the whole album.
        lastSeenId = newestRowId(context.contentResolver)
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scanAsync(context)
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer!!
            )
        }
    }

    /** Unregisters the observer (idempotent). */
    fun stop(context: Context) {
        observer?.let {
            runCatching {
                context.contentResolver.unregisterContentObserver(it)
            }
            observer = null
        }
        seenPaths.clear()
    }

    /**
     * v27t — queues a scan on the serialized background thread (a burst of
     * screenshots is processed one at a time, never in parallel). The
     * observer still fires on the main thread, but [scan]'s query + file
     * copy never touch it.
     */
    private fun scanAsync(context: Context) {
        scanHandler.post { scan(context) }
    }

    private fun hasReadPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun newestRowId(resolver: ContentResolver): Long {
        return runCatching {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media._ID} DESC LIMIT 1"
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else -1L
            } ?: -1L
        }.getOrDefault(-1L)
    }

    /**
     * Queries for new screenshots (rows with id > [lastSeenId] whose
     * display name / relative path look like a screenshot) and appends each
     * one to the active session or the pending write package.
     */
    private fun scan(context: Context) {
        val target = targetFor(context) ?: return
        if (!hasReadPermission(context)) return
        val resolver = context.contentResolver
        // RELATIVE_PATH is API 29+ only; filtering on DISPLAY_NAME + DATA
        // keeps the query working across every supported Android version.
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )
        val selection = "${MediaStore.Images.Media._ID} > ?"
        val args = arrayOf(lastSeenId.toString())
        runCatching {
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                "${MediaStore.Images.Media._ID} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    if (id > lastSeenId) lastSeenId = id
                    val name = cursor.getString(nameCol).orEmpty()
                    if (!looksLikeScreenshot(name)) continue
                    val filePath = cursor.getString(pathCol)
                    if (filePath.isNullOrBlank()) continue
                    if (!seenPaths.add(filePath)) continue
                    val shotUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val saved = SessionShots.copyFrom(context, shotUri)
                    if (saved != null) {
                        val attach = target
                        // State updates (session screenshot list, pending
                        // write package) hop back to the main thread — the
                        // file copy itself stays off it.
                        mainHandler.post { attach(saved) }
                    }
                }
            }
        }
    }

    /** True for the common screenshot naming patterns across OEMs. */
    private fun looksLikeScreenshot(name: String): Boolean {
        val lower = name.lowercase()
        return lower.startsWith("screenshot") ||
            lower.startsWith("screen shot") ||
            lower.startsWith("screencapture") ||
            lower.startsWith("screenshot_") ||
            lower.contains("screenshot")
    }

    /**
     * Resolves where a new screenshot should land right now. The pending
     * write package (session finished, write page open) takes priority so a
     * shot taken "after the session finishes" still attaches; otherwise the
     * live active session. Null when neither exists (no session at all).
     */
    private fun targetFor(context: Context): ((String) -> Unit)? {
        val pending = ExploreSessionStore.pendingWriteTarget()
        if (pending != null) {
            return { path ->
                ExploreSessionStore.appendPendingScreenshot(
                    context, pending.first, pending.second, path
                )
            }
        }
        if (ExploreSessionStore.activeSessionState != null) {
            return { path -> ExploreSessionStore.addSessionScreenshot(context, path) }
        }
        return null
    }
}

package com.curio.app.infrastructure

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.SessionShots
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * v266 — SESSION SCREENSHOT WATCHER (the ScreenshotJanitor idea, scoped).
 *
 * While the floating explore BUBBLE is showing over other apps, any device
 * SCREENSHOT the user takes is attached — exactly one copy, deduped — to the
 * active session's screenshot list for that topic. When the session has
 * already been finished but its write handoff is pending, the shot joins the
 * pending write package instead (same destination: that topic's entry).
 *
 * Gating (all required):
 *  - the bubble window must be attached ([start]/[stop] are called from
 *    [ExploreSessionService] show/remove — no bubble, no watching);
 *  - the media-read permission must be granted (requested alongside the
 *    bubble toggle in Settings; without it this watcher stays inert);
 *  - the new image must look like a screenshot (path/name heuristics below)
 *    AND be younger than [MAX_AGE_SECONDS], so scrolling a gallery doesn't
 *    re-import old shots;
 *  - each MediaStore id is handled once ([lastHandledId]).
 *
 * All MediaStore work runs off the main thread; every step is guarded so a
 * revoked permission or storage hiccup can never crash the session service.
 */
class SessionScreenshotWatcher(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val started = AtomicBoolean(false)
    private var lastHandledId = Long.MIN_VALUE

    private val observer = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            handle()
        }
    }

    /** Starts watching (no-op when already running). Call when the bubble shows. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        runCatching {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        }.onFailure {
            started.set(false)
            Log.w(TAG, "Screenshot watcher register failed", it)
        }
    }

    /** Stops watching and releases the observer. */
    fun stop() {
        if (!started.compareAndSet(true, false)) return
        runCatching { context.contentResolver.unregisterContentObserver(observer) }
    }

    private fun handle() {
        io.execute {
            runCatching { handleInner() }
                .onFailure { Log.w(TAG, "Screenshot attach failed", it) }
        }
    }

    private fun handleInner() {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Images.Media.RELATIVE_PATH
            else MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        // Newest image first; we only ever care about the single latest row.
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        ) ?: return
        cursor.use { c ->
            if (!c.moveToFirst()) return
            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            if (id == lastHandledId) return
            val dateAddedSec = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
            val nowSec = System.currentTimeMillis() / 1000L
            if (nowSec - dateAddedSec > MAX_AGE_SECONDS) return
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
            val path = c.getString(c.getColumnIndexOrThrow(pathCol)) ?: ""
            val name = (c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: "")
                .lowercase()
            // Screenshot heuristics: the standard Screenshots folder, or a
            // "screenshot"/"screenshot_"-style file name (OEM variance).
            val looksLikeShot =
                path.contains("screenshot", ignoreCase = true) ||
                    name.contains("screenshot") ||
                    name.startsWith("screen_")
            if (!looksLikeShot) return
            lastHandledId = id

            val imageUri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toString()
            )
            val copied = SessionShots.copyFrom(context, imageUri) ?: return

            // Attach ONE copy: to the live session, or to the pending write
            // handoff when the session just finished.
            if (ExploreSessionStore.getActiveSession(context) != null) {
                ExploreSessionStore.addSessionScreenshot(context, copied)
            } else {
                val target = ExploreSessionStore.pendingWriteTarget() ?: return
                ExploreSessionStore.appendPendingScreenshot(context, target.first, target.second, copied)
            }
        }
    }

    companion object {
        private const val TAG = "SessionShotsWatcher"
        private const val MAX_AGE_SECONDS = 20L

        /** Whether the current grants allow watching at all. */
        fun hasPermission(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
    }
}

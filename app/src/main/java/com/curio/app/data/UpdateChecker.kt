package com.curio.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.curio.app.BuildConfig
import com.curio.app.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The latest published Curio release (or newest git tag), as reported by the
 * GitHub API.
 */
data class UpdateInfo(
    val tagName: String,
    /** Page to open when an update is available (release page / tag page). */
    val htmlUrl: String,
    /** Release notes body — present only when a real release exists. */
    val releaseNotes: String? = null,
    /** Direct download URL for the release's APK asset — null when the
     *  release has no APK attached (then only the release page is offered). */
    val apkUrl: String? = null
)

/**
 * Checks the latest published Curio release on GitHub.
 *
 * Releases are published from git tags (`v*` — see `.github/workflows/release.yml`),
 * so the latest release's tag (e.g. "v1.0.1") is the authoritative build tag
 * for the newest version. The installed version is `BuildConfig.VERSION_NAME`
 * (e.g. "1.0.0") — exactly the tag the APK was built from, minus the leading
 * "v" — so the comparison is a straight version-component compare.
 *
 * The check hits the releases LIST (which includes prereleases — the
 * /releases/latest endpoint silently skips them, so beta tags like
 * "v1.0-beta2" were never detected) and falls back to the newest git tag
 * only when no release has been published yet.
 *
 * No new dependencies: a plain [HttpURLConnection] GET against the public
 * GitHub API (no auth needed; the unauthenticated rate limit is plenty for a
 * manual, user-initiated check).
 */
object UpdateChecker {
    private const val REPO = "firefly-sylestia/Curio"
    // The releases LIST (newest first) instead of /releases/latest — the
    // latest endpoint silently SKIPS prereleases, and this repo's beta tags
    // (v1.0-beta, v1.0-beta2) are published as prereleases. The list returns
    // every release, so a beta tag is detected as an update too.
    private const val RELEASES_URL = "https://api.github.com/repos/$REPO/releases?per_page=50"
    private const val TAGS_URL = "https://api.github.com/repos/$REPO/tags"

    /**
     * Fetches the latest release (or tag). Returns null on ANY failure —
     * offline, HTTP error, or a parse problem — so the UI can show a neutral
     * "couldn't check" state instead of crashing.
     */
    suspend fun fetchLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        // try/catch (not runCatching) so coroutine CancellationException is
        // rethrown — a cancelled check (user left the screen) must propagate
        // instead of being swallowed into a misleading "failed" state.
        try {
            // 1) Releases list, newest first — INCLUDES prereleases (the
            //    /releases/latest endpoint skips them, which is why beta tags
            //    were never detected). Pick the newest published release,
            //    carrying release notes + release page + APK assets.
            val release = fetch(RELEASES_URL) { raw ->
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    if (obj.optBoolean("draft")) continue
                    val tag = obj.optString("tag_name").takeIf { it.isNotBlank() } ?: continue
                    return@fetch UpdateInfo(
                        tagName = tag,
                        htmlUrl = obj.optString("html_url").ifBlank {
                            "https://github.com/$REPO/releases/tag/$tag"
                        },
                        releaseNotes = obj.optString("body").takeIf { it.isNotBlank() },
                        apkUrl = parseApkAsset(obj.optJSONArray("assets"))
                    )
                }
                null
            }
            if (release != null) return@withContext release
            // 2) No release published yet — fall back to the newest git tag
            //    (the authoritative build tag), so the check reports "up to
            //    date" once a matching tag exists instead of failing. The
            //    tags endpoint doesn't guarantee order, so pick the tag that
            //    compares NEWEST by version instead of trusting the array
            //    position ("v1.0.0.1-test" beats "v1.0-beta" wherever it
            //    sits in the list).
            fetch(TAGS_URL) { raw ->
                val arr = JSONArray(raw)
                if (arr.length() == 0) return@fetch null
                val tag = (0 until arr.length())
                    .mapNotNull { arr.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() } }
                    .maxWithOrNull(
                        Comparator { t1, t2 ->
                            compareVersions(t1.removePrefix("v"), t2.removePrefix("v"))
                        }
                    )
                if (tag.isNullOrBlank()) null
                else UpdateInfo(
                    tagName = tag,
                    htmlUrl = "https://github.com/$REPO/releases/tag/$tag",
                    releaseNotes = null
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * True when [latestTag] ("v1.0-beta2") is newer than the installed
     * [currentVersion] ("1.0.0").
     *
     * This repo ships every build as a git tag, so any tag that differs from
     * the installed one is a newer build — pure numeric comparison breaks on
     * the beta scheme ("1.0.0.1-test" outranks "1.0-beta2" numerically even
     * though beta2 is the newest published release). Different tag ⇒ update.
     */
    fun isNewer(latestTag: String, currentVersion: String): Boolean {
        val a = latestTag.removePrefix("v").trim()
        val b = currentVersion.trim()
        return a != b
    }

    /**
     * Compares two version strings component-by-component. Tolerant of the
     * prerelease-style suffixes this repo actually tags with
     * ("v1.0.0.1-test", "v1.0-beta") — segments that used to be silently
     * dropped by `toIntOrNull` ("1-test" → null) so a newer tag compared
     * equal to the installed version and the check wrongly said "up to
     * date". Returns > 0 when [a] is newer than [b].
     */
    private fun compareVersions(a: String, b: String): Int {
        val aParts = a.split('.')
        val bParts = b.split('.')
        val len = maxOf(aParts.size, bParts.size)
        for (i in 0 until len) {
            val x = parseSegment(aParts.getOrElse(i) { "" })
            val y = parseSegment(bParts.getOrElse(i) { "" })
            if (x.num != y.num) return x.num.compareTo(y.num)
            // Equal numeric cores: a bare number beats a prerelease suffix
            // (1.0.0 > 1.0.0-beta); when both carry suffixes compare them
            // as text ("1-test" > "0-beta").
            if (x.suffix != y.suffix) {
                if (x.suffix.isEmpty()) return 1
                if (y.suffix.isEmpty()) return -1
                return x.suffix.compareTo(y.suffix)
            }
        }
        return 0
    }

    /** One dotted version segment — its numeric core + any prerelease tail. */
    private data class VersionSegment(val num: Int, val suffix: String)

    /** "1-test" → (1, "-test"), "0" → (0, ""), "" → (0, ""). */
    private fun parseSegment(raw: String): VersionSegment {
        val digits = raw.takeWhile { it.isDigit() }
        val num = digits.toIntOrNull() ?: 0
        return VersionSegment(num, raw.removePrefix(digits))
    }

    /**
     * Finds the release's APK asset — the first `.apk` in the GitHub
     * release's `assets` array. The release workflow uploads the signed
     * release APK (from the `apk/release` output dir) to every release, so
     * this is the direct download used by the in-app updater.
     */
    private fun parseApkAsset(assets: JSONArray?): String? {
        if (assets == null) return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                return asset.optString("browser_download_url").takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    /**
     * Downloads [url] (the release APK) into [target], reporting progress
     * via [onProgress] (bytes received, total — total is 0 when unknown).
     *
     * Returns true on a complete download, false on ANY failure (offline,
     * HTTP error, IO error). Cancellation rethrows so a cancelled download
     * (user left the screen) propagates instead of being swallowed.
     */
    suspend fun downloadApk(
        url: String,
        target: File,
        onProgress: (received: Long, total: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                // GitHub asset URLs redirect to objects.githubusercontent.com.
                conn.instanceFollowRedirects = true
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext false
                val total = conn.contentLengthLong
                target.outputStream().use { out ->
                    conn.inputStream.use { input ->
                        val buf = ByteArray(64 * 1024)
                        var received = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            received += n
                            onProgress(received, total)
                        }
                    }
                }
                true
            } finally {
                conn.disconnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** GETs [url] and parses the 200 body with [parse]; null on any non-200. */
    private fun fetch(url: String, parse: (String) -> UpdateInfo?): UpdateInfo? {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            return parse(raw)
        } finally {
            conn.disconnect()
        }
    }

    private const val UPDATE_CHANNEL_ID = "curio_updates"
    private const val UPDATE_NOTIFICATION_ID = 7007

    /**
     * v53 — background update notifier, run on app start. When the latest
     * release is newer than the installed build:
     *  - an IN-APP DIALOG ([CurioUpdatePrompt], v227d — the old corner
     *    toast pill is gone) offers the Updates page, waiting a few seconds
     *    past launch so it isn't glued to the start screen, and
     *  - a NOTIFICATION fires alongside it.
     * Both are ONCE PER VERSION — [AppPreferences] remembers the last
     * announced tag, so a pending update is announced on the first launch
     * that finds it and never again (v63b: the old toast repeated on every
     * launch; the once-gate now sits BEFORE both announcements).
     * Any failure (offline, API error) is silently ignored — the manual
     * check in Support & diagnostics remains the authoritative path.
     */
    suspend fun notifyIfUpdateAvailable(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val release = fetchLatestRelease() ?: return@withContext
        if (!isNewer(release.tagName, BuildConfig.VERSION_NAME)) return@withContext
        // v63b — ONCE per version for BOTH the toast and the notification:
        // the announced tag is recorded (and the gate consumed) BEFORE any
        // announcement, so a pending update never nags on every launch.
        val lastNotified = AppPreferences.getLastNotifiedUpdateVersion(appContext)
        if (lastNotified == release.tagName) return@withContext
        AppPreferences.setLastNotifiedUpdateVersion(appContext, release.tagName)
        withContext(Dispatchers.Main) {
            // v227d — IN-APP DIALOG instead of the corner pill: the
            // NavHost observes [CurioUpdatePrompt.pending] and renders the
            // themed offer. Global state survives the check racing the
            // UI's first frame. v99's delay stays so the dialog reads as a
            // later announcement, not part of the start screen (the
            // notification below still fires immediately).
            delay(4_000)
            CurioUpdatePrompt.show(release.tagName)
        }
        // Notification — same once-per-version gate as the toast above.
        runCatching { ensureChannel(appContext) }
        val openApp = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?: Intent(Intent.ACTION_MAIN)
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Curio ${release.tagName} is available")
            .setContentText("A newer version is ready — open Curio and update from the Updates page.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A newer version is ready — open Curio and update from the Updates page.")
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        // POST_NOTIFICATIONS is runtime-gated on 13+ (a no-op below API 33 —
        // treated as granted); without the grant notify() throws a
        // SecurityException. The toast above already announced the update, so
        // just skip the notification when the permission is missing.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext
        }
        NotificationManagerCompat.from(appContext).notify(UPDATE_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                UPDATE_CHANNEL_ID,
                "Curio updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New Curio releases"
            }
        )
    }
}

/**
 * v227d — the pending in-app UPDATE PROMPT version tag (null = nothing to
 * show). The NavHost renders it as a themed AlertDialog offering the
 * Updates page; this replaces the old corner toast pill entirely. Global
 * snapshot state so the check (a data-layer coroutine) can raise it before
 * or while the UI composes.
 */
object CurioUpdatePrompt {
    var pending by mutableStateOf<String?>(null)
        private set

    fun show(versionTag: String) {
        pending = versionTag
    }

    fun dismiss() {
        pending = null
    }
}

package com.curio.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
 * The latest-release endpoint 404s until the FIRST release is actually
 * published, so [fetchLatestRelease] falls back to the newest git tag — the
 * checker works from day one instead of showing a failure state.
 *
 * No new dependencies: a plain [HttpURLConnection] GET against the public
 * GitHub API (no auth needed; the unauthenticated rate limit is plenty for a
 * manual, user-initiated check).
 */
object UpdateChecker {
    private const val REPO = "firefly-sylestia/Curio"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPO/releases/latest"
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
            // 1) Latest published release — carries release notes + release page.
            val release = fetch(LATEST_RELEASE_URL) { raw ->
                val obj = JSONObject(raw)
                UpdateInfo(
                    tagName = obj.optString("tag_name"),
                    htmlUrl = obj.optString("html_url"),
                    releaseNotes = obj.optString("body").takeIf { it.isNotBlank() },
                    apkUrl = parseApkAsset(obj.optJSONArray("assets"))
                )
            }
            if (release != null && release.tagName.isNotBlank()) return@withContext release
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

    /** True when [latestTag] ("v1.0.1") is newer than [currentVersion] ("1.0.0"). */
    fun isNewer(latestTag: String, currentVersion: String): Boolean =
        compareVersions(latestTag.removePrefix("v").trim(), currentVersion.trim()) > 0

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
}

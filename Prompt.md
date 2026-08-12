# Prompt.md — Research & Analysis Tracking

## Current Request (IN PROGRESS — NOT COMMITTED, per user: "dont push unless i say"): GitHub-only in-app updater; remove Play code

**Date:** 2026-08-12

**What was asked:** "how do we add in app updates though with only github" → user decided (ask_user): build the GitHub in-app updater AND keep a short browser link; ALWAYS-ON (no toggle); the USER confirms the install (never auto-install). Then: "remove the play something and also dont push unless i say."

**Key design constraint:** Google Play's in-app-update API is hardwired to Play-installed apps and can never work for GitHub APKs — so the updater downloads the release APK from GitHub and hands it to the system installer.

**Done (all uncommitted in the working tree):**
- **Play Core removed entirely:** deleted `infrastructure/CurioInAppUpdate.kt`, removed `CurioInAppUpdateHost()` from MainActivity, removed the Play path from SupportScreen (imports, `playUpdateInfo`, `updateManager`, `updateLauncher`, `PlayAvailable` enum state, the async play check), removed `playAppUpdate = "2.1.0"` from libs.versions.toml and `implementation(libs.google.play.app.update)` from app/build.gradle.kts.
- **GitHub in-app updater built:**
  - `UpdateChecker`: `UpdateInfo.apkUrl` (parsed from the release's GitHub API `assets` array, first `.apk`), `parseApkAsset()`, and `downloadApk(url, targetFile, onProgress)` — streams with a 64 KB buffer on Dispatchers.IO, follows redirects, rethrows CancellationException.
  - `SupportScreen`: `UpdateDownloadUi { Idle, Downloading, Failed }` + `downloadProgress`; `downloadAndInstall()` downloads to `cacheDir/downloads/curio-<tag>.apk`, then launches the installer via `FileProvider` (`ACTION_VIEW`, `application/vnd.android.package-archive`, `FLAG_GRANT_READ_URI_PERMISSION`) — the USER confirms. UpdateResultCard shows "Update now" (in-app), a short "Open release" link (replaces "Get it on GitHub"), a LinearProgressIndicator + percent while downloading, and a retry line on failure.
  - `xml/file_paths.xml`: added `cache-path apk_downloads` → `downloads/`.
  - `AndroidManifest.xml`: added `REQUEST_INSTALL_PACKAGES`.
- **Bug found while validating:** my parseApkAsset KDoc contained the literal `/*` sequence (`apk/release/*.apk`). Kotlin block comments NEST, so it opened a nested comment, the KDoc's `*/` only closed depth 1, and the rest of the file became an unterminated comment — a REAL compile error CI would have caught. Reworded the KDoc. (The braces checker + line-bisection pinpointed it.)
- Docs updated: changelog (20260919.txt), app/AGENTS.md (v25 GitHub updater bullet + `/*`-in-comment gotcha).

**Validation:** braces + `git diff --check` clean; zero `CurioInAppUpdate`/`AppUpdate`/Play references remain in app/, toml, or build.gradle.kts.

**NOT done (user instruction):** no commit, no push. Changes sit in the working tree for review.

**Next:** user reviews → then commit + push on their say-so.

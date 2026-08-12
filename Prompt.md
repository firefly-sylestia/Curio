# Prompt.md — Request Log

## Current Request (COMPLETE): Update check redo (in-app updates + full notes) + merge Support/Diagnostics/About into one page

**Date:** 2026-08-12

**What was asked (Android only):**
1. Redo the "version check for updates" — it's not that good; does it show the notes; can we do in-app updates?
2. Merge Support + Diagnostics + About Curio into ONE screen (they feel duplicated) and keep the entry point reachable from BOTH Settings and Profile.

**Answer to the questions:** the update check already fetched release notes but only showed a 5-line clipped preview; now it shows FULL notes (Show more/less). In-app updates: YES — added Google Play Core `app-update` 2.1.0 with the FLEXIBLE flow (only works for Play-installed apps; sideloaded APKs report NOT_AVAILABLE and fall back to the GitHub release check).

**Changes:**
- `gradle/libs.versions.toml` + `app/build.gradle.kts` — added `com.google.android.play:app-update:2.1.0` (verified from Google Maven AAR; no javadoc/sources published).
- NEW `app/src/main/java/com/curio/app/infrastructure/CurioInAppUpdate.kt` — `CurioInAppUpdate.available(context)` suspends on the Play query (UPDATE_AVAILABLE + isUpdateTypeAllowed(FLEXIBLE), null on sideload/failure); `CurioInAppUpdateHost` root composable registers the InstallStateUpdatedListener (finishes on DOWNLOADED) + ON_RESUME finish; `finishInstall()` chains a Task call so `completeUpdate()` resolution is unambiguous.
- `MainActivity.kt` — hosts `CurioInAppUpdateHost()` above `CurioNavHost()`.
- `features/support/SupportScreen.kt` — update check now queries Play first then GitHub; new `PlayAvailable`/`GithubAvailable` states; result card shows FULL release notes with Show more/less and an "Update now" (flexible, via `StartIntentSenderForResult` launcher) or "Get it on GitHub" button; merged the About content in as an "About Curio" section (Replay intro + GitHub repository).
- Merge cleanup: removed `SettingsPage.ABOUT` + `AboutSection` (SettingsSectionScreen), `SETTINGS_ABOUT` route (CurioRoutes + CurioNavHost), deleted `ui/components/CurioUpdateCheckRow.kt`; Settings hub "About Curio" row → renamed "Support & diagnostics" → `SUPPORT`; deep-search rows (Replay intro / Version / Check for updates) repointed to `SUPPORT`.
- Docs: fastlane changelog bullets, app/AGENTS.md v24 bullet, this log.

**Validation:** braces check + `git diff --check` clean; grep confirms zero leftover `SETTINGS_ABOUT`/`SettingsPage.ABOUT`/`CurioUpdateCheckRow`/`about-*` refs; Play Core API signatures verified against the 2.1.0 AAR (ActivityResultLauncher<IntentSenderRequest> overload + Task chaining). No local Gradle (project rule — CI validates on push). Reviewed by code-reviewer-deepseek-flash.

## Previous Requests

### Explore polish pass (Android)

**Date:** 2026-08-12 — session time on the save/edit strip, slow knob-only scroll indicator, hidden-by-default dialog bubble + Notifications re-show toggle, onboarding SEARCH slide + bubble opt-in in the overlay card, "any search engine" copy, Appearance cleanup (floating pet / pet brain / auto-open always on, custom reaction lines off).

### Web app full parity pass, excluding pet (web — untouched by later Android requests)

### CI fix — settings-import package + indicator drag API (Android)

**Date:** 2026-08-12 — TopicHistoryScreen imports + CurioScrollIndicator drag on `detectVerticalDragGestures`.

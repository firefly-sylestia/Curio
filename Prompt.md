# Request — CI: PRs build release only; tag releases ship universal + per-ABI APKs with device labels

## Analysis

- PR CI (`android.yml`) built BOTH `assembleDebug` and `assembleRelease` and uploaded
  both APKs. The user wants PRs to generate **only the release build** (debug stays
  available for local dev via the debug build type; CI just stops producing it).
- Tag releases (`release.yml`) built a single fat `assembleRelease` APK. The user wants
  a **universal APK plus per-ABI APKs** (arm v7 / arm64 / x86 / x86_64) so each device
  installs the smallest matching file, and an easy way to know **which APK is for which
  device** — the chosen approach: rename APKs to
  `Curio-{versionName}-{versionCode}-{abi}-Android8.0+.apk` (Android 8.0+ = minSdk 26)
  and publish an install-guide table in the release body.
- Builds stay GitHub-only (no local Gradle per DOX rules).
- **AGP 9.2.1 constraint (verified against the AGP 9.0.1 release notes):** the legacy
  `applicationVariants`/`outputFileName` API was REMOVED, and DENSITY splits were
  removed — but ABI splits (`splits { abi { } }`) are still supported. So per-ABI APKs
  come from the `splits` DSL, and the device-friendly renaming happens in the CI
  workflow (no removed-API usage).

## Changes

- `app/build.gradle.kts`
  - Added `splits { abi { isEnabled; reset; include(armeabi-v7a, arm64-v8a, x86, x86_64); isUniversalApk } }`
    so every release build emits a universal APK + one APK per ABI.
  - Added a `printReleaseVersion` task printing `versionName:versionCode` (string
    captured at configuration time); the release workflow reads it so APK names never
    drift from `defaultConfig`.
- `.github/workflows/android.yml` (PR CI)
  - Now runs `lintDebug validateTopics assembleRelease` — **release-only**, no debug APK.
  - Signature verification loops over **all** release APKs (was one); uploads release
    APKs only (debug glob removed).
- `.github/workflows/release.yml`
  - Builds + verifies the universal and per-ABI APKs (signature loop over all).
  - Reads the version via `printReleaseVersion`, renames APKs to
    `Curio-{versionName}-{versionCode}-{abi}-Android8.0+.apk`, and writes a
    `release-body.md` install guide (table: file / which devices / size + "how do I
    know" instructions).
  - Hard guard: fails loudly if any of the 5 expected split APKs is missing after
    rename (protects against AGP naming drift, which can't be verified locally).
  - Publish step uses `body_path` + `generate_release_notes: true` (install guide is
    prepended to GitHub's auto notes).
- DOX pass: `.github/AGENTS.md` (Android CI + Release workflow contracts) and
  `app/AGENTS.md` (CI expectations + corrected version identity) updated.

## Validation

- Both workflows parse with `js-yaml` (node via npx).
- The rename + release-body shell logic was simulated end-to-end against fake
  `app-{abi}-release.apk` files: names and markdown table render correctly, the
  5-file assertion passes.
- `git diff --check` clean. Code-reviewed by code-reviewer-deepseek-flash; its two
  fixes (config-time version capture, post-rename assertion) were applied.
- Gradle builds remain CI-only per DOX rules; CI is the compile gate.

## Completion

Committed and pushed. Unrelated working-tree changes (`docs/app/QUEST_AND_PET_REDESIGN_SPEC.md`
deletion, untracked `docs/plans/`) were left untouched and out of the commit.

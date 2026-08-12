# Prompt.md — Research & Analysis Tracking

## Current Request (COMPLETE): Play Store update prompt on GitHub sideloads

**Date:** 2026-08-12

**What was asked:** "Why does the app ask for an in-app update from the Play Store when the app isn't on Play Store — it's only on GitHub?"

**Root cause:** The v24 update check gated the Play path on Play Core's *availability answer* (`updateAvailability() == UPDATE_AVAILABLE && isUpdateTypeAllowed(FLEXIBLE)`). Play Core does NOT reliably distinguish Play-installed from sideloaded apps — on GitHub/APK installs it can still report UPDATE_AVAILABLE, so the Support page showed "Update available on Google Play" + an "Update now" button that could never actually work.

**Fix (v25) — installer gate:** `CurioInAppUpdate.isInstalledFromPlay(context)` checks `getInstallerPackageName(packageName) == "com.android.vending"`. `available()` now returns null immediately on non-Play installs (no Play query at all), and `CurioInAppUpdateHost` doesn't even create the Play Core manager unless the app came from Play. GitHub/ADB/file-manager installs always go straight to the GitHub release check (which always works). The Play path stays fully functional for a future Play launch.

**Files:** `app/src/main/java/com/curio/app/infrastructure/CurioInAppUpdate.kt` (new `isInstalledFromPlay`, gated `available()` + host; KDoc updated). SupportScreen needed no change — `playUpdateInfo` can only be non-null on Play installs now.

**Validation:** braces + `git diff --check` clean; `isInstalledFromPlay` referenced at all three intended sites.

**Next:** none pending.

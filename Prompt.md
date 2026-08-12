# Prompt.md — Research & Analysis Tracking

## Current Request (COMPLETE): Keep Play in-app-update code, fix the CI compile errors

**Date:** 2026-08-12

**What was asked:** User first suggested removing the Play in-app-update code entirely, then reconsidered: "why u deleting it fi it maybe or tell me how to add things if needed and make it not fail the ci" — KEEP the code (in case it's needed later), explain how it works, and make CI pass.

**Root cause of CI failure:** `CurioInAppUpdate.kt` has `object CurioInAppUpdate` (members: `isInstalledFromPlay`, `available`, `finishInstall`) plus a top-level `@Composable fun CurioInAppUpdateHost()`. Inside the host, the calls were written UNQUALIFIED (`isInstalledFromPlay(context)`, `finishInstall(manager)`). Object members are only in scope inside the object's own body — from the top-level host they are `Unresolved reference` compile errors. (This was present since the v24 addition; the AppUpdateType import fix masked it in earlier logs.)

**Fix:** qualified the three host call sites — `CurioInAppUpdate.isInstalledFromPlay(context)` (host manager gate) and `CurioInAppUpdate.finishInstall(manager)` ×2 (install listener + ON_RESUME observer). `available()`'s internal `isInstalledFromPlay` call is fine (same-object member). Added a comment in the host + an AGENTS.md note documenting the gotcha.

**How the feature works (kept as-is):**
- `SupportScreen` "Check for updates" runs `CurioInAppUpdate.available(context)` (Play Core `app-update` 2.1.0) and the GitHub `UpdateChecker.fetchLatestRelease()` concurrently. If Play reports a FLEXIBLE update AND the app was installed from Play (`getInstallerPackageName == com.android.vending`), the card shows "Update available on Google Play" + "Update now" (`startUpdateFlowForResult` via `StartIntentSenderForResult` launcher). Otherwise it falls back to the GitHub release card with full expandable notes.
- `CurioInAppUpdateHost()` in MainActivity registers an `InstallStateUpdatedListener` + ON_RESUME observer that auto-completes a finished flexible download.
- Re-enabling later: nothing to do — code + dependency (`playAppUpdate = "2.1.0"` in libs.versions.toml, `libs.google.play.app.update` in app/build.gradle.kts) are intact. To hard-disable: remove the dependency, the file, the host call in MainActivity, and the Play path in SupportScreen.

**Validation:** braces + `git diff --check` clean; all three host references qualified.

**Next:** push + CI should go green.

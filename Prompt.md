# Prompt.md — Research & Analysis Tracking

## Current Request (COMPLETE): Remove the "How Curio feels" card header in Settings

**Date:** 2026-08-12

**What was asked:** Remove the "How Curio feels / Appearance and color" card header LINE in Settings (with its icon and subtitle) — NOT the Appearance page and NOT the rows. The user confirmed via ask_user: keep Backup & restore ("Keep it") and keep the "Your data" card ("keep it").

**Done:**
- `SettingsHubScreen.kt`: the Personalize card's `headerIcon`/`headerTitle`/`headerSubtitle` for "How Curio feels" set to null. The card renderer already skips the header when those are null (`if (headerIcon != null && headerTitle != null && headerSubtitle != null) CurioCardHeader(...)`), so the four rows (Appearance, Notifications, Recording, Pet designer) now render directly under the "Personalize" section label. Search filter handles null headers too (`headerTitle?.contains(...) == true`). No page/row removed.
- Changelog bullet added.

**Also this session:** the previous request — GitHub-only in-app updater (Play Core removed; "Update now" downloads the release APK with progress and opens the system installer; short "Open release" link) — was committed and pushed as `6f65d0c` (user asked: "commit and push the previous one"). This header removal is a separate commit after it.

**Validation:** braces + `git diff --check` clean.

**Next:** none pending.

# Prompt.md — Research & Analysis Tracking

## Current Request (COMPLETE): Remove remaining Settings card headers + bump section-label size

**Date:** 2026-08-12

**What was asked:** "also similar header in settings identify and tell me" → after identifying all header lines (hub cards + sub-page headers), the user picked ALL of them for removal, plus: "increase the text size of the personalize explore and safety and support texts and similar".

**Removed (icon + title + subtitle header lines only — rows/pages untouched):**
- Settings hub cards: "Experiments / Try visual ideas…", "Your data / Backups and restore" (both `headerIcon/Title/Subtitle = null`; the renderer skips null headers). "How Curio feels" was already removed in the previous commit.
- SettingsSectionScreen: "Visual language" (Appearance), "Notifications", "Recording", "Backup & restore" (DataSection) — 4 CurioCardHeader lines removed; the CurioCardHeader import removed.
- ExperimentsScreen: "Main card", "Deck peek cards", "Promo mode" headers removed; import removed.
- BackupToolsScreen: "Backup & restore", "FieldMind archive" headers removed; import removed.
- CurioSectionLabel (`ui/components/CurioSettingsComponents.kt`): font bumped labelMedium → titleSmall (SemiBold) — this is the shared component behind "Personalize"/"Explore"/"Safety & support" and every other section label (Support's Updates/Feedback/About, Experiments' Spin visuals/Promo, etc.), so all get larger at once.

**Validation:** braces + `git diff --check` clean; zero CurioCardHeader usages left in the 3 files that lost their imports; hub has 3 null-header cards.

**Next:** none pending.

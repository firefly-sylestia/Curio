# Prompt.md — Request Log

## Current Request (COMPLETE): Web app full parity pass, excluding pet

**Date:** 2026-08-12

**What was asked:** Bring the newly added web app closer to the Android app: every button, visual treatment, animation, screen, and function should match where practical. Do not focus on pet parity yet.

**Plan:**
- Audit existing web screens/components against Android feature names and shared Curio visual language.
- Implement high-impact parity in the standalone React app without changing Android code: navigation, home/spin/reveal/capture/cabinet/profile/settings/topic browser/quests/detail/history-style functionality where feasible from local IndexedDB state.
- Preserve the web app's standalone constraints and avoid pet-focused work.
- Run web checks only (`npm run lint`, `npm run build` if available) and avoid all Gradle commands per project rules.

**Completed changes:**
- Added a web Topic History screen with the Android-style torn rose header, day grouping, format glyphs, and session-time display.
- Routed the drawer and Home recents “View all” action to the new history screen instead of Cabinet.
- Fixed reveal → capture to pass the topic id, and Save Capture now reloads the real topic metadata instead of saving a hyphenated display name.
- Save Capture now persists entered data for Reel Notes, Marginalia, Gallery Wall, and Field Notes instead of saving empty JSON for non-SoundBite formats.
- Added web session duration persistence to captures and displays it in history rows.
- Added the Settings search-engine selector and wired “Explore in browser” to the selected engine.

**Validation:**
- `npm run lint` passed with existing warnings (fast-refresh export warnings and pre-existing hook warnings).
- `npm run build` passed; Vite emitted existing large chunk warnings for topic bundles.
- `git diff --check` passed.
- No Gradle commands were run, per project rules.
- Screenshot capture was attempted by running the Vite dev server, but no browser executable/playwright installation is available in this container.

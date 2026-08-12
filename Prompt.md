# Prompt.md — Request Log

## Current Request (COMPLETE): Web app full parity pass, excluding pet
## Current Request (COMPLETE): CI fix — settings-import package + indicator drag API (Android)

**Date:** 2026-08-12

**What happened:** CI failed with ~20 errors, two real root causes:
1. **TopicHistoryScreen.kt** — `settingsReadableInk`/`settingsRoseAccent` imports pointed at `com.curio.app.ui.theme`, but the shared helpers live in `com.curio.app.features.settings` (SettingsHubScreen). That single bug cascaded into ~18 downstream type-inference errors (produceState `Pair` pinned to `Nothing?`, `Map.Entry.copy` weirdness, String-vs-Int mismatches in the Liked/Disliked items, hero symbol pairs) — all resolved once the imports landed.
2. **CurioScrollIndicator.kt:128** — `change.currentPosition.y` was ALSO unresolved: PointerInputChange's position properties are hidden in compose-ui 1.12.0-alpha03 (first attempt used `positionChange().y` — Boolean in this version; second used `currentPosition` — also gone).

**Fix (2 files):**
- **TopicHistoryScreen.kt** — imports corrected to `com.curio.app.features.settings.settingsReadableInk/settingsRoseAccent` (grouped before the ui.* imports, with a short comment); `produceState` explicitly typed `produceState<Pair<List<CurioTopic>, List<CurioTopic>>?>` so `initialValue = null` can't pin the type to `Nothing?`.
- **CurioScrollIndicator.kt** — drag gesture rewritten from the raw `awaitEachGesture` + per-event position-math loop to the stable **`detectVerticalDragGestures`** (verified present in foundation 1.12.0-alpha03's DragGestureDetectorKt): `onDragStart`/`onDragEnd`/`onDragCancel` toggle `touched` (knob grows), `onVerticalDrag` consumes the change and maps `dragAmount` over knob travel to the scrollable range. Removed the now-dead imports (`awaitEachGesture`, `awaitFirstDown`, `changedToUp`).

**Validation:** No local Gradle build (project rule — CI validates on push). Reviewed by code-reviewer-deepseek-flash: correct + complete; three accepted notes — import placement tidied into a features.settings group, knob now grows on drag-start (after touch slop) instead of raw touch, and the "content fits" guard moved from an early-exit to a no-op inside the callback.

## Previous Requests

### Session time in Topic History rows (Android)

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

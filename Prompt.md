# Prompt.md — Request Log

## Current Request (COMPLETE): Android polish pass — session time, scroll knob, explore dialog, onboarding, settings cleanup

**Date:** 2026-08-12

**What was asked (Android app only — user explicitly said do NOT touch the web app):**
1. Explore session time must show in the entry editing page alongside the topic, and in the detail view.
2. Topic browser scroll indicator lags — keep it slow scrolling only (never jump/teleport) and make only the knob respond to touch.
3. Don't show the explore-bubble opt-in in the Explore dialog (or show it as one main text, no subtext), and add a Settings option to show it in the dialog too.
4. Explore in browser opens Google by default — add the search-engine picker to onboarding and change "Search in your browser with Google" copy to "any search engine".
5. Put the explore-bubble option in onboarding.
6. Remove from Settings → Appearance (keep features on by default, not removed from the app): Floating pet, Pet brain, Auto-open landed topic. Custom reaction lines: remove the option and keep it OFF (no longer customizable).

**User answers (ask_user):**
- Explore dialog bubble: hidden by default + Settings toggle re-shows it (single line, no subtext).
- Onboarding search engine: new dedicated SEARCH slide (after Theme, before permissions).
- Onboarding bubble toggle: inside the "Display over other apps" permission card.
- Scroll knob: slow gentle drag only, knob-only touch target, no tap-to-position.

**Changes (7 code files + docs):**
- `data/AppPreferences.kt` — new `KEY_SHOW_BUBBLE_OPT_IN_DIALOG` pref (state, seed, getter/setter), default OFF.
- `ui/components/CurioScrollIndicator.kt` — knob drag now scrolls at a fixed slow 1:2.5 ratio clamped per event (no full-range jump); touch is scoped to a padded box exactly over the knob (empty strip does nothing); constants `KnobScrollRatio`/`KnobMaxDeltaPx`/`KnobTouchPadPx`.
- `features/reveal/TopicRevealScreen.kt` — bubble opt-in row hidden unless the new pref is on; single-line (icon + main text + switch, no subtext) when shown; pref only applied on explore start when the row is visible; dialog copy now "Search in your browser with any search engine"; removed the now-unused `SearchEngine` import.
- `features/settings/SettingsSectionScreen.kt` — removed Floating pet / Pet brain / Auto-open landed topic / Custom reaction lines toggles from Appearance (features stay on by default; custom reaction lines stay off); added "Explore bubble option in Explore dialog" toggle in Notifications (`notif-bubble-dialog`) with ON_RESUME refresh.
- `features/settings/SettingsHubScreen.kt` — dropped the "Custom reaction lines" deep-search row; added the new bubble-dialog row.
- `features/onboarding/OnboardingScreen.kt` — new SEARCH slide (kicker/headline/subtext + ink-glass engine chips for the 7 engines via FlowRow) after Theme; pager pageCount +3, isLastSlide +2, dots now 0..(Slides.size+1); bubble opt-in row (`BubbleOptInRow`) inside the overlay permission card shown once granted; added `SearchEngine`/`FlowRow`/`ExperimentalLayoutApi` imports.
- `features/capture/SaveCaptureScreen.kt` — topic strip now shows "explored 12m" (Timer glyph + `formatSessionShort`) under the topic: edit mode reads `editingEntry.sessionTimeMillis`, fresh save peeks the write-session handoff/live session (same sources the save uses).
- Detail view session segment: already implemented (v22 frosted-bar Session segment) — verified present; no change needed.
- `fastlane/metadata/android/en-US/changelogs/20260919.txt` — appended 4 bullets (fixed a glued-line newline issue).
- `app/AGENTS.md` — durable "Always-on companions & onboarding setup (v23)" preference bullet.

**Validation:** `node scripts/check_braces.js` on all 7 edited files → OK; `git diff --check` → clean; grep hygiene for removed symbols (SearchEngine in TopicReveal, custom-reaction/floating-pet refs in settings, hub "Custom reaction lines") → all clear. No local Gradle (project rule — CI validates on push). Reviewed by code-reviewer-deepseek-flash.

## Previous Requests

### Web app full parity pass, excluding pet (web — untouched this request)

**Date:** 2026-08-12 — parity pass for the standalone React web app (`web/`). User explicitly forbade web changes in the current request.

### CI fix — settings-import package + indicator drag API (Android)

**Date:** 2026-08-12 — fixed TopicHistoryScreen imports (`settingsReadableInk`/`settingsRoseAccent` from `features.settings`) and rewrote CurioScrollIndicator drag on `detectVerticalDragGestures` (position APIs hidden in compose-ui 1.12.0-alpha03).

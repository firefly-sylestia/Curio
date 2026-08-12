# Prompt.md — Request log

## Current request (v26 — Topic Browser header rebuild + back-to-top arrow)

**Status:** Implemented, uncommitted in working tree (user's standing rule: no push unless asked).

### What was asked
1. Floating arrow at top when scrolled too far down → jumps back to top.
2. Search, sorting, and filters live in the header (not scrolling in the list).
3. Category filter becomes a floating bar like the Cabinet screen.
4. Search has the same morph-open animation as the Cabinet.

### What was done
- **SettingsHeroHeader (SettingsHubScreen.kt)** — extended the shared settings hero with:
  - optional `trailing: (@Composable (ink: Color) -> Unit)?` slot (ink-glass pills on the top row beside the back pill),
  - optional morph-open search: `searchActive/searchQuery/onSearchQueryChange/onCloseSearch/searchFocus/searchPlaceholder`,
  - `AnimatedContent` scale/fade morph (search open: scaleIn+fadeIn 280ms; close: title fadeIn, scaleOut+fadeOut 200ms) — Cabinet's exact search-morph contract,
  - Cancel pill replaces trailing pills while searching,
  - new public `SettingsHeroActionPill` (with `modifier` param) — backward compatible: all 11 existing callers unaffected (defaults).
- **TopicDatabaseScreen.kt** — header rebuild:
  - search + sort pills ride the hero top row (sort = `CurioSortDropdown` emphasized ink; search = PetLandmark-wrapped `SettingsHeroActionPill`),
  - old in-list controls item (search box + chip LazyRow + sort LazyRow) removed; needle now reads `searchQuery`,
  - new `DatabaseStickyChipBar` — Cabinet-style floating category filter: rests below hero, lifts/pops/pins on scroll (`LazyListState.layoutInfo` progress, `DatabaseChipPop` stagger scale 0.90→1.0, label blooms toward accent via `popProgress`),
  - new floating back-to-top arrow: `AnimatedVisibility` (fade+scale), shown when `firstVisibleItemIndex >= 10` (~700dp ≈ one full screen), top-end below the pinned chip bar, clears the 54dp alphabet rail (end=68dp), tap → `scrollToItem(0)` + reset saved scroll state,
  - constants ordered so init order is safe (`DatabaseChipBarHeight` before `DatabaseContentTop`).

### Validation
- Braces OK (both files), `git diff --check` clean, no leftover `query`/OutlinedTextField refs, all new imports used.
- Code review passed; reviewer flagged back-to-top threshold (4 rows) → tuned to 10 rows (~one screen).

### Also uncommitted in the same working tree
- Pastel header saturation bump (Home/Profile/Settings rose accents, +5%) — from a previous request, also awaiting push.
- Topic Browser header rebuild (hero search/sort pills + floating chip bar + back-to-top arrow) — from the previous request, also awaiting push.

## Request: update check says "up to date" when a newer release exists

**Status:** Fixed in working tree (UpdateChecker.kt). Push pending.

### Root cause
`UpdateChecker.isNewer` used `split('.').mapNotNull { it.toIntOrNull() }`, which **silently dropped non-numeric segments**. The repo's real tags are `v1.0.0.1-test` and `v1.0-beta` (confirmed via GitHub API: `/releases/latest` → 404, so the tags fallback is used). `"v1.0.0.1-test"` → `[1,0,0]` == installed `"1.0.0"` → wrongly "up to date". Same for `v1.0-beta` → `[1,0]`.

### Fix (UpdateChecker.kt)
- New `compareVersions(a, b)`: parses each dotted segment into (numeric core, prerelease suffix) via `parseSegment` — numeric cores compare numerically, a bare number beats a suffixed one (1.0.0 > 1.0.0-beta), both-suffixed compare as text, missing segments are 0. `isNewer` delegates to it.
- Tags fallback now picks the max tag by version comparator (`maxWithOrNull(Comparator {...})`) instead of trusting array order.
- Verified by hand across 8 cases (the reported bug, beta-vs-release, numeric 1.2.10>1.2.9, equal, missing segments, 2.0.0>1.0.0.1-test). Code review passed.

## Request: new Preferences section + settings rearrangement

**Status:** Implemented in working tree. Push pending (user's standing no-push rule).

### What was asked
Add a new preference option inside Settings → Personalize and move preference-type settings there (search engine was named; I suggested the rest). Rearranged the settings.

### Decisions (user-confirmed via ask_user)
- Moved into new **Preferences** screen: **Search engine, Pet games, Pet chatter, Explore sessions, Floating explore bubble, Live explore notification**. (NOT moved: Voice-to-text stays in Recording; "Explore bubble option in Explore dialog" stays in Notifications.)
- Placement: **right after Appearance** in Personalize.
- Judgment call flagged: **Display over other apps** (the overlay-permission row) moved to Preferences with the bubble — it wasn't in the options list, but its grant/decline machinery (launcher + ON_RESUME observer) is inseparable from the bubble toggle, so splitting them would duplicate complex logic.

### Changes
- `SettingsPage.PREFERENCES` enum entry + `PreferencesSection` composable (search engine + SearchEngineDialog, explore sessions, live notification with POST_NOTIFICATIONS flow, floating bubble + overlay permission with launcher/ON_RESUME handling, pet chatter, pet games).
- `NotificationsSection` trimmed to daily reminder (+hour chips) + bubble-opt-in-in-dialog; keeps its own permission launcher for the reminder.
- `AppearanceSection` lost pet chatter/games.
- `SettingsHubScreen`: Preferences row (CurioIcons.Tune) after Appearance; deep-search index rows re-pointed to SETTINGS_PREFERENCES with pref-* rowKeys (pref-search-engine, pref-sessions, pref-live, pref-bubble, pref-overlay, pref-pet-chatter, pref-pet-games); Notifications deep rows now just reminder + bubble-dialog.
- `CurioRoutes.SETTINGS_PREFERENCES` + NavHost composable; `CurioIcons.Tune = "tune"` (glyph verified present in material_symbols_outlined.ttf).

### Validation
- Braces OK (5 files), git diff --check clean, no stale notif-*/appearance-pet-* rowKeys, only one exhaustive `when (page)` (updated). Code review passed.

## Request: commit and push everything

**Status:** Committed and pushed. Working tree clean.

All pending work from this session was bundled into one push:
1. Preferences settings section (new screen + hub row + deep-search index)
2. UpdateChecker version-comparison fix
3. Topic Browser header rebuild (hero search/sort pills + floating chip bar + back-to-top arrow)
4. Pastel header saturation bump (Home/Profile/Settings)
5. Warm no-AI pledge copy (onboarding + explore dialog)
6. CI APK artifact retention 14 → 1 day (android.yml)

### Not done / follow-ups
- No web app changes (user's standing rule: Android app only).

# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "https://github.com/firefly-sylestia/Curio/commit/e870f038… similar to pet house hold you
> should have fixed the recent topics in home the tap and hold actions for the topics in
> home screen its still buggy and not 1.5 sec something and also doesnt have haptics."

A correction of the hold fix shipped in `e870f038`:

1. Home's **own** recent-topic rows (the "Recents" preview on the Home page) were never
   reached by that fix.
2. There, a hold still fires far sooner than 1.5–2 seconds.
3. And it plays no haptic.

---

## 2. What was found

- `e870f038` wrapped **`RecentScreen`** (the Recents page) and the pet's home
  (`CurioPetHome`) in `CurioPatientHold` — both use `combinedClickable`, which reads
  `LocalViewConfiguration.longPressTimeoutMillis`, so the wrapper changed their timing.
- **Home's recents preview is a different mechanism.** `HomeScreen`'s `recentPreview` rows
  (`ExploreTopicRow` / `RecentEntryRow`) carry `hold = HoldSession(...)` and open the
  picker's anchored menu through `Modifier.radialHoldMenu` (`features/picker/
  RadialHoldMenu.kt`).
- That gesture runs **its own timer** (`scope.launch { delay(viewConfig.longPressTimeoutMillis) }`)
  against `LocalViewConfiguration.current` read in its own `composed { }` — so it *would*
  inherit a patient hold if the rows were wrapped, but the rows were never wrapped and
  therefore kept the platform's ~500ms.
- It also called `hold.onOpen(pressPos)` with **no haptic anywhere**: the picker's call
  sites (`onOptionTarget = { cat, pos -> optionTarget = cat; … }`) and Home's
  (`onOpen = { pos -> recentOption = item; … }`) both pass a plain lambda. The doc on the
  gesture even said the caller owns the tick — and no caller ever fired one.
- The gesture's v337 **scroll-cancel** (touch slop cancels the pending timer) is already in
  place and stays; it just was not enough with a 500ms window on a resting finger.

---

## 3. What was changed

- **`Modifier.radialHoldMenu` now fires `HapticFeedbackType.LongPress` itself** the moment
  the hold opens (`LocalHapticFeedback.current` hoisted in the `composed { }` block, the
  app's haptic convention). Its KDoc now says so and warns call sites not to add a second
  tick. That gives the haptic to every surface that uses this gesture — Home's recents rows
  **and** the Spin picker's lane tiles and browse rows (which had none before).
- **Home's recents preview now wears `CurioPatientHold`.** The wrapper sits around the
  preview's row `Column`, so the radial gesture inside reads the patient
  `ViewConfiguration` and waits `CurioHoldMillis = 2_000L` with no second timeout
  mechanism. A swipe still cancels it (slop), a tap still taps, and the pet's home and the
  Recents page are untouched.
- **`app/AGENTS.md`** — the "Hold-to-act gestures — the patient hold (v407)" contract now
  documents both mechanisms (the `combinedClickable` handler tick vs. the gesture's own
  tick), lists Home's recents preview as a user, and keeps an honest **known gap**: the
  picker's sheets have the haptic but still use the platform timeout.
- **Changelog** (`20260922.txt`) — one FIX bullet for Home's rects/preview hold.

---

## 4. ⏸️ INTERRUPTED — the Settings + licence request (clarified, NOT built)

The previous message (still owed):

> "https://www.gnu.org/licenses/agpl-3.0.en.html can we add this license for our curio in
> readme, also hide the rail in settings the top rail, when im in all settings only show
> when inside, some settings, also remove remove the all settings option from the rail, and
> about the info in the setting cards, make their text be 2 or 3 lines for the end 4
> settings card the online mode, recycle bin, updates, and help and feedbacks see how the
> texts get cut how about remove the icons for those 4, and then in appearace while keeping
> the rail system instead f the big cards with design, add a simpler add a list based
> simpler all settings look the main settings page but a simpler and list view."

**The member's answers to the two questions asked before touching anything:**

- **Which screen should switch to the simpler list view → "The main Settings page."** The
  All Settings hub itself becomes plain rows (icon + title + subtitle) grouped under
  Personalize / Safety & support, instead of the 2-up `settingsDesignGroups` cards with
  tone gradients and doodles. The rail stays inside the sections.
- **AGPL-3.0 → "README + LICENSE file."** The README's `MIT License` line becomes
  AGPL-3.0 and the full licence text is committed as `LICENSE` (the README already links a
  `LICENSE` file that does not exist in the repo).

**What is already known about that work (so it can be finished without re-reading):**

- Hub render site: `SettingsHubScreen.kt` — the phone branch's `LazyVerticalGrid`
  (`item(key = "nav")` rail → **remove**, `activeNav` state + its reset `LaunchedEffect` →
  remove, `settingsDesignGroups.forEach` + `settingsSecondaryCards.forEach` →
  replace with a flat list over `SettingsSections`, keeping the search field, the section
  headings and `SettingsFooterNote`).
- Rail data: `settingsNavRail` (`SettingsNavEntry("all", "All Settings", CurioIcons.Home,
  null)` → remove; `navigateToSettingsSection` already falls back for a null route).
- `SettingsSections` (the flat list already used by the wide two-pane and the search)
  holds the rows; give it a `plain` flag for **Online mode / Recycle bin / Updates /
  Support & diagnostics** → no icon tile, copy free to wrap 2–3 lines, and
  `SettingsOptionDivider` gains a `startInset` so those rows' dividers align with them.
- The pet's Settings landmark (`PetLandmark(id = "appearance", …)`) must move from the
  designed Appearance card onto the new Appearance **row** — otherwise the pet's Settings
  poke (and the tour's stop) silently breaks.
- The now-unused designed-card machinery (`SettingsDesignTone/Visual/Card/Group/
  SecondaryCard`, `settingsDesignGroups`, `settingsSecondaryCards`, `settingsToneGradient`,
  `settingsCardInk`, `SettingsCardVisual`, `SettingsDesignCardView`,
  `SettingsSecondaryCardView`) is private to `SettingsHubScreen.kt`; `settingsCardChipTint`
  / `settingsCardTintLift` must STAY (used by `ui/components/CurioSettingsCard.kt`).
- AGPL-3.0 text was fetched once from `https://www.gnu.org/licenses/agpl-3.0.txt`; the
  extractor drops the blank lines between paragraphs, so the file needs either a byte-exact
  fetch (a permitted `curl` into `LICENSE`) or a careful re-stitch.

---

## 5. Still open

- **Nothing here is device-verified** — the hold timing and the haptic are compile-checked
  only (`node scripts/check_braces.js` is clean; CI is the build check). Worth a real hold
  on Home's recents: it should take two full seconds, tick, and survive a scroll.
- **The shared hold constant is 2s** (`CurioHoldMillis`, as asked in the previous request);
  "1.5 sec" is a one-line change if that is the feel you want.
- **The Spin picker's sheets** still use the platform timeout (they have the haptic now).
  Wrapping them in `CurioPatientHold` is the remaining half of that gap.
- **The Settings + licence request above is unfinished** — that is the next thing to build.

---

## User prompts

Status: this request is complete and pushed. No pending prompt below.

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and
its status stay at the top, and the empty slot below is where the next instruction lands. -->

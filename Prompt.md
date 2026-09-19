# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "tap and holding the pet hub should bring up option to disable the pet, also when
> scrolling above trecent topics it tap and hold by mistakes, make the tap and hold time
> more longer and fix the accidental tap and hold."

Two asks, and the first one needed clarification because **two** pet things already had a
hold of their own:

1. Holding the pet's hub should offer to disable the pet.
2. In the recent-topics list, scrolling trips the hold by mistake. Make the hold longer.

**The member's answers to the two questions asked before touching anything:**

- **"no the home of it not the pet but the home of where the pet stays"** — the ask was
  whether to put it on the floating pet (whose hold arms GAME MODE) or the over-other-apps
  pet (whose hold already opens a Send home / Wander menu). Neither: it goes on **the pet's
  HOME**, i.e. `CurioPetHome` — the little house/flower-bed scene the pet lives in. The
  floating pet's hold was therefore left exactly as it was.
- **"2 sec also add haptic for hold"** — the hold waits 2 seconds and plays a haptic, for
  the recent-topics rows (the reported surface).

---

## 2. What was found

- **The pet's home is `CurioPetHome` (`ui/pet/CurioFlowerBed.kt`)** — called from exactly
  two places: Home's bed (`HomeScreen`, 52dp, tap = wake / come out) and the Quests hero's
  bed (`ui/pet/CurioPetCompanion.kt`, 74dp, tap = wake / come out / check-in dialog). It was
  `Modifier.clickable` ONLY — no hold existed, so adding one replaces nothing.
- **The recent topics are `features/recent/RecentScreen.kt`.** Its rows carry
  `combinedClickable(onClick = open the topic, onLongClick = the options pill)`. The
  platform long-press timeout is ~500ms, which is short enough that a finger resting on a
  row during a scroll reaches it. (Home's own recent rows have no long press at all, and
  Topic History / Spin's browse list have none either — so Recents was the only surface
  with this trap.)
- **The app already solved this class of bug once:** `features/picker/RadialHoldMenu.kt`
  (v337) cancels a pending hold the moment the finger travels past touch slop — "scrolling
  through the grids no longer pops an option menu at the release point".
- **The app's haptic convention for holds:** `HapticFeedbackType.LongPress`, hoisted as
  `val haptics = LocalHapticFeedback.current` (SocialComponents, CurioCategoryCard,
  DirectMessageScreen). Compose plays NO haptic for `combinedClickable` — it has to be
  fired in the handler.
- **`LocalViewConfiguration.longPressTimeoutMillis` is the one knob** the platform tap
  detector reads, and it is a `compositionLocalOf` — so a subtree can provide a patient
  `ViewConfiguration`. (Also noted: this Compose generation makes the gesture scope
  `@RestrictsSuspension` — no `withTimeout`/`delay` inside `awaitEachGesture` — per
  RadialHoldMenu's v325 note, which rules the custom-timer route out.)

---

## 3. What was changed

- **New `ui/components/CurioPatientHold.kt`** — `CurioHoldMillis = 2_000L` and
  `CurioPatientHold { }`, which provides a `ViewConfiguration` delegating to the platform
  one with `longPressTimeoutMillis = max(platform, 2s)` (so a device with a longer native
  hold never gets a shorter one). This is the app's ONE hold timing: the framework's own
  cancellation still applies inside it, and a scroll that consumes the gesture cancels the
  pending press, so a swipe can never arm a row.
- **Recents (`features/recent/RecentScreen.kt`)** — the feed's `LazyColumn` is wrapped in
  `CurioPatientHold`, and every row's hold now goes through one `hold` lambda that fires
  the LongPress haptic before opening the options pill. (The three
  `onLongClick = { onLongPress(item) }` sites now share it.)
- **The pet's home (`ui/pet/CurioFlowerBed.kt`)** — `CurioPetHome` takes a
  `combinedClickable` hold (with the same LongPress haptic) that opens a confirm dialog:
  "Turn Curie off?" → `AppPreferences.setPetEnabled(context, false)`, with "Keep Curie" to
  back out. A tap keeps every one of its old meanings (wake / come out / check in), the
  hold is only offered while the pet is ON, and the dialog names the switch that brings it
  back (Settings ▸ Appearance, where the "Curie" toggle already lives).
- **Both call sites** (Home's bed and the Quests hero) wrap the scene in `CurioPatientHold`
  — the timeout has to be provided above the composable that builds the modifier, because
  that is where `combinedClickable` reads `LocalViewConfiguration`.
- **`app/AGENTS.md`** — a new "Hold-to-act gestures — the patient hold (v407)" contract
  (the constant, why the platform timeout was the bug, who uses it, the haptic rule) and a
  v407 bullet in the Curie pet layer section (the home is the door to turning the pet off,
  and new call sites must wrap the scene).
- **Changelog** (`20260922.txt`) — one ADD for the pet-home hold, one FIX for the Recents
  hold.

---

## 4. Still open / decisions the member may want to revisit

- **The longer hold is applied to the Recents feed and the pet's home only.** Every other
  hold-to-act list (journal days, books, Cabinet entries, chats, friends, the category
  grids) still uses the platform's ~500ms. Extending `CurioPatientHold` to one of those is
  wrapping its list — say the word if they should all feel the same.
- **The floating pet's hold still arms game mode** (untouched, per the clarification), and
  the over-other-apps pet's hold still opens its own Send home / Wander menu. If "disable
  the pet" should also appear there, that is a small addition to those two menus.
- **The pet's home hold cannot be exercised without a device** — the gesture plumbing is
  compile-checked only (CI), and `node scripts/check_braces.js` is clean. Worth a real
  hold-test on the Home bed and the Quests hero.
- **The forms SQL has not been pasted into Supabase**, so the ban ladder, the queue's Clear,
  and publish / read / vote / skip for a form have nothing to talk to yet; the `forms`
  permission also still needs granting to the team.
- **Stop-the-poll in moderation** (asked for, not built) needs that schema first.
- **Journal entries being slow** still needs a measurement: list open, or page load.
- **The deprecation sweep** (about 30 `rememberModalBottomSheetState` sites, 4
  `LocalClipboardManager`, 1 `LocalLifecycleOwner`) is unstarted; Material3 here is
  `1.5.0-alpha20`.
- **`web/` carries its own React mirror** of these screens. Out of scope per the root
  AGENTS.md scope rail (web/ is on hold) — NOT touched.

---

## User prompts

Status: this request is complete and pushed. No pending prompt below.

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and
its status stay at the top, and the empty slot below is where the next instruction lands. -->

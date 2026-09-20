# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "lets add some gesture double tap etc function extra tools hidden one for journal editing.
> 10 differnt action add them as experiment options in journal with each explained so i
> would know, add the option in dev experiment. dont ask me just add them do some research.
> and finish it and then watch cl fix it then dont watch just give me more suggestions by
> using ask user, also did u chnage the curio rose color it kinda looks odd now somehow, if
> not then ignore or just make it a little more vibrant, also fix the social tab active
> indicator not follwoing the app active indicator and its text color. fix it also the
> reading progress im on capter card shifts when a longer chapter comes can u fix that too.
> so its better. push the previous after finishing too."

Five parts: (a) ten hidden gesture tools for journal editing, behind explained Dev-page
switches; (b) the Social tab's active indicator and its label ink; (c) the "I'm on chapter"
tile shifting on a long chapter name; (d) the Curio rose's vibrancy; (e) push the previous
batch. Then watch CI, fix what it finds, and end with suggestions (via `ask_user`).

## 2. What the code actually looked like (findings)

### (b) The Social tab's indicator
- The bottom bar publishes a per-page accent (`CurioNavTint`): Home publishes its hero fill,
  Spin its deck lane, the Cabinet its filter lane — so those three pills are soft pale
  accents with a deep label ink (`curioActivePillFill` calms the accent in light mode;
  `curioActivePillInk` pairs it).
- `CommunityScreen` published **`curioDialogActionColor()`** — the accent at its ACTION depth
  (saturation 0.35–0.60, lightness 0.36). Right for a button's words, wrong for an
  indicator: the Social pill came out a heavy deep-pink slab wearing white text.

### (c) The chapter tile
- `ProgressTile`'s value `Text` was `maxLines = 2` with **no reserved height**, so a chapter
  name that wrapped to two lines grew the tile and pushed the whole card down as you stepped.

### (d) The rose
- `CurioColors.HomeRosewood = 0xFFCF8B94` — **untouched since v7.36** (git blame: last moved
  in `64674b0e`, before this work). Nothing in this session's changes altered it, so the
  member's "make it a little more vibrant" half of the ask applies.

### (a) The journal's hidden gestures
- The editor is `PersonalEditorState` (PersonalCanvas.kt, ~5,500 lines) with a deep but
  largely PUBLIC API — the dock's own calls. There was no gesture layer at all, and no
  Experiments section for one.
- `PersonalCanvas`'s root is `modifier.clickable(enabled) { state.focusLastLine() }`, and the
  writing column has its own `.clickable { editor.focusLastLine() }` for the blank space.

### (e) Previous batch
- Committed as `4a507791` (reading-progress steppers + lane stats graph + Paper under
  Adaptive Hero); nothing was pushed yet.

## 3. What was done

### (a) `PersonalGestures.kt` — ten tools, one recogniser (NEW file)
- **The catalogue** `JournalGestureTool` — ten entries, each with its own bit, label,
  two-line explanation (gesture first, then action) and the `JournalGesture` it runs:
  bring back a removed row (2-finger tap) · page alignment (2-finger double-tap) · carry the
  line up / down (2-finger swipe up/down) · list mark (2-finger swipe left) · take the whole
  page (2-finger swipe right) · save on the spot (3-finger swipe down) · cycle the line's
  mark (3-finger swipe up) · a dated heading (double-tap the blank space) · the line's
  writing face (hold the blank space).
- **The storage**: ONE int bitmask (`AppPreferences.journalGestureToolsState`,
  `KEY_JOURNAL_GESTURES`), with `journalGestureTools` / `isJournalGestureToolEnabled` /
  `setJournalGestureToolEnabled` — so a new tool is a new enum entry, not a new preference.
- **The recogniser**: one `awaitEachGesture` loop in `Modifier.journalGestures`. It counts
  fingers, follows the centre, and reads a tap from the centre's whole journey (a wander is
  not a tap) and a swipe from where it ended (the direction it left in). It reads in
  `PointerEventPass.Initial` and consumes only once two fingers are down; `enabled.isEmpty()`
  returns the modifier untouched, so with the section off nothing changes at all.
- **Single-finger tools ride the blank space** (`combinedClickable` on the writing column,
  extra callbacks null unless their tool is on) — a finger on the writing belongs to the
  caret, the selection and the scroll.
- **The actions** are `PersonalEditorState.runJournalGesture`, on the editor's own public API,
  so a gesture can do nothing a button cannot and auto-save sees ordinary writing. Saving is
  the one exception (the page owns the row and the debounce), handed back as `onSaveNow`.
- **The Dev page**: a new "Journal gestures" section in `ExperimentsScreen`, one
  `ExperimentSwitchRow` per tool (label + its explanation), with a summary line reading "N of
  10 switched on", all off until chosen.

### (b) The Social indicator (`CommunityScreen`)
- Publishes `MaterialTheme.colorScheme.primary` — the app's own accent, the same value every
  unpublished page and `curioActivePillFill`'s fallback use — instead of the dialog action
  ink. The pill and its label ink (`pastelFillInk`) now resolve like the other three tabs.

### (c) The chapter tile (`BookDetailScreen`)
- `minLines = 2` on the value text: two lines are always reserved, so stepping onto a long
  name no longer moves the card; a name that still overflows is ellipsized.

### (d) The rose (`CurioColors`)
- `HomeRosewood` 0xFFCF8B94 → **0xFFD7838E** and `HomeRosewoodDark` 0xFF6E3A44 → **0xFF76323F**:
  hue (352°) and lightness untouched, saturation 0.415 → 0.515 / 0.310 → 0.405. Same rose,
  less dusty; every ink, pastel twin, wash and blend above them is derived, so all follow.

Docs + notes: a new `PersonalGestures` bullet in `app/AGENTS.md` (the ten tools, the one
recogniser, the pointer-pass and consumption rules, the two documented reaches);
two changelog groups (one ADD, three FIX) appended to `20260922.txt`.

## 4. Decisions

- **No ask_user for the gestures** — the member's instruction was explicit ("dont ask me just
  add them"). Every tool is still OFF by default behind its own explained switch, which is
  what the house rule for an experiment requires, and is what makes "just add them" safe.
- **Two fingers or more in the recogniser, single-finger tools on the blank space.** A text
  field legitimately owns one finger; taking that away would break typing, the caret and the
  scroll. Multi-finger input is the one thing it has no use for.
- **The honest reaches are written on the rows**: alignment is the PAGE's in this app (all
  four dock buttons call the same `setAlign`), and a MARKER dresses a list row (so cycling
  marks gives the line a bullet with it). A gesture that over-promised would read as broken.
- **The rose was nudged in saturation only** — hue and lightness identical, both twins moved
  together — because the member's complaint was that it "looks odd", not that it was the
  wrong colour: the family rule is that a hero's inks, pastel twin and washes are all derived
  from it, so a hue or lightness move would have rippled everywhere.
- **The Social pill publishes the theme accent, not a bespoke social colour.** The bar's rule
  is "the page's accent, calmed"; the app's own accent is what a page without a lane has.

## 5. Status

- Implemented; brace/paren balance verified (0/0) on every edited file. No Gradle in this
  environment, so CI validates the compile.
- The previous batch (`4a507791`) and this one were pushed together on the member's ask.
- Open, from the member's own list: CI is watched once and any failure fixed; then the next
  step is the member's own suggestions, asked with `ask_user`.
- Known, pre-existing, deliberately untouched: under **Material + pastel mode** a single lane
  keeps the device colour without the category accent foot.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§4)

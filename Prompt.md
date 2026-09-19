# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "lso now lets do something to the default rose and azure theme colors i think with white pages
> they look good, just the cream color is too much maybe subtle cream, and also soft shadow no
> border just shadow instead of borders. elevation depth also for journal introduce its own colors.
> also the voice note wave in journal its 2 wave and looks weird fix it please and use 1 wave style
> and proper depth, also whats new version was wrong. also in book view there are 2 progress bars
> now which is again bad can u please fix its design and the way we choose chapters and pages,
> please redesign its visual and options the im on chapter the book has that etc etc, also did the
> pantone color in their own accent isnt app wide?"

Six separate things, all of them second-pass corrections to the v411 work that is already
pushed. No new ask_user round was needed: every item named its own defect.

## 2. What the code actually looked like (findings)

- **Cream.** The shipped light scheme (`CurioWhitePageLightScheme`, white page + cream cards)
  put the card at `#FAF3E4` with the deep step at `#E7DABE` — a tan plate, not a hint of
  cream — and every card ALSO drew a hairline (`outlineVariant`) on top of its fill.
- **The voice note was drawn twice.** `PersonalVoiceBar`'s Canvas built TWO paths (`upper`
  and `lower`) as a mirrored envelope and stroked both — literally the "2 wave" the member
  reported — and neither had any depth.
- **What's New printed the wrong version**: `WHATS_NEW_RELEASES` carried
  `versionName = "1.1.1"` while `app/build.gradle.kts` builds `1.3.0`, and the page prints
  `v${versionName} · build ${versionCode}`.
- **The book card drew TWO bars**: a page `Box` gauge AND, under it, a `repeat(total)` row of
  chapter ticks, each with its own caption — two answers to "how far" in two units. The three
  controls below were three identical `ProgressStepperRow`s ("I'm on chapter" / "Page" /
  "The book has").
- **The journal had no identity of its own**: journal rows, the empty card, the editor's mood
  pill and chips all wore `surfaceContainer*`, and the canvas's prints were a hardcoded
  `#FCF8F1` / `#2B2723`.
- **Pantone was only partly app-wide**: the scheme, the shared heroes and the settings accents
  read it (v411), but `curioRoseInk()` and `settingsCardChipTint()` still returned the fixed
  coral, so icon chips and plate tints stayed rose under a Pantone theme.

## 3. What was done

### One flat card language (cream + no borders + soft shadow)

- `CurioWhitePageLightScheme`'s card ladder pitched LIGHTER (card `#FCF7EC`, then `#F7F0E2` /
  `#F2E8D5` / `#ECE1C8`) — a whisper of cream on a white page, steps still even.
- **`curioCardEdgeColor(fill)` now answers `fill` unconditionally** (no card border in ANY
  theme; `outlineVariant` is left for dividers), and the new
  **`Modifier.curioCardShadow(shape, elevation)`** — a warm DeepPlum-tinted, low-alpha shadow,
  applied BEFORE the fill and a no-op in dark — is what lifts a card off the page.
- Witnessed at: `CurioSettingsCard` (28dp), `SettingsOptionCard` (20dp), `StatsCard` (22dp),
  `StatsDoorChip` (2dp), the rail tab (border simply removed — an opaque tile on a card needs
  neither), the quick-tool chip and the search card (2dp), the lane tile (selected only, 3dp
  and the ring kept as selection feedback).

### The journal's own colours and depth

- New tokens in `PersonalTheme.kt`: `journalPaper()` (a warm parchment carrying a whisper of
  `personalAccent()`, so a Pantone theme, a lane hero and the rose all flow through it),
  `journalPaperRaised()`, `journalInk()` and `journalRule()`.
- Applied to the journal list rows, the empty card, the editor's mood pill and mood chips,
  and the canvas's two print frames (a print and the page under it are one paper) — each with
  a soft shadow for depth.

### ONE wave in a voice note

- `PersonalVoiceBar` draws ONE stroke now, bucketed to ~26 steps (the loudest sample of each
  bucket wins, so a fast passage keeps its peaks), crossing the centre on every step, with a
  soft under-stroke 5% lower for depth. The strip stays backgroundless and shadowless (v404).

### One progress bar, two tiles (the book page)

- **`ReadingGauge`** replaces the gauge + tick row: one bar whose fill is the page fraction,
  with each chapter's opening drawn as a notch INSIDE the track (`chapterPages` is passed in
  as the 1-based page each chapter opens at, `MAX_GAUGE_NOTCHES = 80` caps the hairlines).
- **`ProgressTile` + `TileStepButton`** replace the three identical stepper rows: "I'm on
  chapter" (which NAMES the chapter) and "Page" are tiles side by side, and "This book has"
  is a quiet row under them. `ProgressStepperRow` is deleted.

### What's New

- The release entry's `versionName` is `1.3.0` to match the build.

### The Pantone accent, app-wide

- `curioRoseInk()` and `settingsCardChipTint()` answer the Pantone palette first. NOT
  recoloured on purpose: the gold/mint brand inks and the 36 lane accents (documented in
  `app/AGENTS.md`).

## 4. Still open / worth a device pass

- **Nothing here is device-verified.** No Gradle in this environment: `node scripts/check_braces.js`
  passes (283 files) and every new symbol was checked against its real definition by reading it;
  **CI is the compile check**.
- Worth watching on device: the shadow-only cards on a bright screen (that is the whole point of
  the change — if a cream card on a white page now reads as flat, the fix is the shadow's
  `elevation`, not a border returning); the voice note's one wave over a quiet recording (the 8%
  floor is what keeps a silent passage a line and not a break); the book card with a 300-page PDF
  and a 60-chapter outline at once; and a Pantone theme on the Journal and the Stats page.
- The `MAX_TICKS` constant is now unused (the tick row it capped is gone) but kept for the
  history in the file.

---

## User prompts

Status: **done — committed and pushed on `main`.**

> "lso now lets do something to the default rose and azure theme colors i think with white pages
> they look good, just the cream color is too much maybe subtle cream, and also soft shadow no
> border just shadow instead of borders. elevation depth also for journal introduce its own colors.
> also the voice note wave in journal its 2 wave and looks weird fix it please and use 1 wave style
> and proper depth, also whats new version was wrong. also in book view there are 2 progress bars
> now which is again bad can u please fix its design and the way we choose chapters and pages,
> please redesign its visual and options the im on chapter the book has that etc etc, also did the
> pantone color in their own accent isnt app wide?"

All six are implemented as described above. The Pantone question is answered in the code and in
`app/AGENTS.md`: the accent roles are app-wide now, while the gold/mint brand inks and the lane
accents stay their own colours.

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and its
status stay at the top, and the empty slot below is where the next instruction lands. -->

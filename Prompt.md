# Prompt.md — Request log

## Current request — COMPLETED: cosmic icon from the designer PNG + detail/filter/icon/pet-hero polish

All of this session's work is done, committed and pushed (`258f533`).

### 1. Launcher icon — reapplied from the designer's PNG (v113)
The user rejected the earlier hand-converted VECTOR icon ("the design is
broken and its not properly placed") and supplied a raster:
`svgviewer-output (3).png` (2048×2048 RGBA — the same mint planet / pink
moon / gold waves / midnight sky in a rounded white-framed card).
- **Root cause of "broken":** the card spans ~84–88% of the 2048 canvas.
  At the adaptive-icon canvas (108dp) the launcher mask's 66dp safe zone
  sliced the frame, top stars and bottom waves.
- **Fix:** the PNG is used DIRECTLY (no vector conversion):
  `drawable-nodpi/ic_launcher_art.png`; `ic_launcher_foreground.xml` is
  now `<inset android:inset="28dp">` around the bitmap → the card renders
  at ~44×47dp, fully inside the safe circle, floating on the sky.
  `ic_launcher_background` (full-bleed sky gradient + stars) kept — its
  gradient matches the card's own sky so the two composite behind the
  frame. Monochrome + notification silhouettes kept. The splash now
  renders `@drawable/ic_launcher_art` directly (not the inset foreground)
  so the splash logo keeps its full size. PNG archived at
  `design/launcher-icon/curio-launcher-icon.png` (the old SVG stays).
- Geometry checked from the font/PNG data: card bbox x[160,1888] y[76,1884]
  of 2048; inset 28 → corner distance ≈ 32dp ≤ mask r=33.

### 2. Detail hero — duplicate "explored" pill removed
The hero's "explored 12m" pill above the Date · Mood · Session · Type card
was removed (it duplicated the Session segment inside the card). Spacer
after the title/paper-cuts normalized to 18dp.

### 3. Spin filter sheet — inactive group-pill icons visible in light mode
`FilterGroupPill` tinted the closed pill's glyph with raw `accent`, which in
pastel LIGHT is an airy pastel — invisible on the 22%-accent fill. Now uses
`pastelFillInk(accent)` (deep same-hue ink, L≈0.24) when light+pastel;
dark and non-pastel keep `accent`.

### 4. CurioIcon glyphs cut at the top in buttons (many places)
The 1dp `graphicsLayer { translationY = -1dp }` "optical lift" drew the ink
1dp ABOVE the icon's layout box. The Material Symbols font's line box is
1.2em (hhea ascent 1056 + descent 96 vs 960 upem); near-top-bearing glyphs
(timer, auto_awesome, sparkle tips — 40/960-unit bearings) sat exactly at
the box top, so clipped parents (every M3 Surface with a shape clips)
sliced the top. Verified by parsing the font's cmap/glyf bounds. The lift
is removed — glyphs render centered per the font's design bearings.

### 5. Pet Designer hero floating mid-screen
The v109 scroll-away hero translated by `-viewportStartOffset`, but that
property is the viewport start in CONTENT coordinates — NEGATIVE by the top
content padding (`SettingsHeroTotalHeight + 8`) at rest, so the hero sat
~heroHeight down the screen ("stuck in the middle floating"). Fixed:
`translationY = -(viewportStartOffset + beforeContentPadding)` = 0 at rest,
-S when scrolled. (Semantics confirmed from the AndroidX
`LazyListLayoutInfo` source: "usually 0, but negative if non-zero
beforeContentPadding was applied".)

### Validation
All XML re-validated with ElementTree; font geometry verified with a pure-
Python TTF parser; `git diff` reviewed; no Gradle locally (env rule) — CI
validates on push. Pushed to `main`.

## Follow-ups / notes
- The update-checker toggle ships OFF by default (opt-in) — once settled,
  remove the toggle and hardcode the winner (experiment rule).
- Auto-backup reuses ONE persisted document URI; "pick a folder" is
  implemented as "pick a document once" — revisit if a true folder flow is
  wanted.
- The launcher icon: if the user later wants the card BIGGER, the inset can
  drop to ~24dp (card ≈ 48×51dp, frame corners just inside the 66dp circle)
  — but 28dp is the safe default across circle/squircle masks.

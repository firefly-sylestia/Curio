# Prompt.md — Request log

## Current request — COMPLETED: stock buttons converted to the pill/chip language

All of this session's work is done, committed and pushed (`4bbcc64`).

Swept all `Button`/`OutlinedButton` call sites (167 matches) for stock M3
styling sitting next to the custom pill family. Fixed:
1. **Tour Skip/Next (CurioNavHost)** — 16dp corners → full capsules
   `RoundedCornerShape(50)` (54dp tall, in the tour bar).
2. **Crash screen (CurioCrashScreen)** — three `OutlinedButton`s 16dp →
   24dp (the Mix-button language); the 28dp primary CTA kept.
3. **FieldMind (FieldMindObservationScreen)** — Finish + Save were fully
   stock (default 20dp corners, primaryContainer fill, hardcoded white
   icon): Finish → pill 50, Save → 24dp, both theme-primary fill with
   `onPrimary` icon/text; removed the now-unused `Color` import.
4. **Sound Bite trim pair (SoundBiteFormat)** — Keep full + Apply Trim
   16dp → 24dp, kept as a matched pair.

Intentional keepers (documented in the AGENTS.md v114 bullet): dialog
`TextButton`s (`curioDialogActionButtonColors`), themed `RadioButton`s,
the M3 `SegmentedButton` in Settings, and self-contained flows
(onboarding 18/26dp, bug-report 28dp) keep their own styling.

### Validation
diff reviewed; imports checked (ButtonDefaults added to FieldMind;
Color removed); no Gradle locally (env rule) — CI validates on push.
Pushed to `main`.

## Previous request — COMPLETED: pet eyes flick on touch scrolls + glitchy eye pixels

All of this session's work is done, committed and pushed (`bd95876`).

### 1. Pet eyes STILL reacted to touch scrolls (PetPointer tracker)
The eyes aimed on `Press`, so every touch-scroll began with a visible
snap toward the finger's touchdown point — the 8dp drag-cancel only
fired once the finger actually moved, and the 2s look-timeout kept the
snap visible. (Also: `press` was set on Press, so the look never faded
until Release.)
- **Fix:** the tracker now only REMEMBERS the press point on Press and
  commits the aim on Release when the gesture was a clean tap (no
  drag) — via `position` (the hover path), so the sprite's existing 2s
  look-timeout fades the aim back to neutral exactly like a desktop
  click. A scroll's Release arrives with `pressStart` already nulled by
  the drag-cancel, so scrolling never aims the eyes. Mouse hover/click
  behavior unchanged. (`press` field stays null-on-hold; the sprite's
  fade logic keys off it correctly.)

### 2. Scaled eye pixels looked glitchy (CurioPetSprite eye rendering)
The v71 eye-size presets scaled the DRAW SPACE (`DrawScope.scale` at
0.72/1.0/1.35 around each eye's center): each eye cell became a
fractional-size rect on a fractional grid, so cells landed BETWEEN
device pixels and rendered as misaligned glitchy lines.
- **Fix:** the scaled eyes are now drawn on an INTEGER device-pixel
  lattice — each cell is `round(scaleF × opx)` px (≥1), snapped to the
  lattice around the eye's center (4.5/7 and 10.5/7). Crisp at every
  preset; at scaleF = 1 the cells land exactly on the face's pixel
  grid (floor/ceil lattice), so Medium eyes are byte-identical to the
  original rendering.

### Validation
Imports checked (`roundToInt` added; `scale`/`translate` still used by
`drawDetailLayer`/eyes); diff reviewed; no Gradle locally (env rule) — CI
validates on push. Pushed to `main`.

## Previous request — COMPLETED: icons pushed down/cut at the bottom + pill glow peeking past the pill

All of this session's work is done, committed and pushed (`835677b`).

### 1. CurioIcon glyphs pushed DOWN and cut at the BOTTOM (font-size driven)
The user clarified the icon-cutting was font-size related: at large system
font sizes the glyphs sat low and their ink bottoms were sliced (they
confirmed the previous "optical lift" fix addressed the wrong direction).
- **Root cause (measured, not guessed):** the icon `Text` used
  `includeFontPadding = false` + `LineHeightStyle.Trim.Both` +
  `lineHeight = 1.0em`. Trimming below the font's NATURAL 1.2em line box
  (hhea ascent 1.1em / descent 0.1em) plus the trim's int rounding
  (ascent −69 → −68, descent 6 → −5 at 24dp/420dpi) dropped the baseline
  ~5px below the icon box → ink center ~2dp low, ink bottom ~1dp past the
  box → cut by clipped button shapes. Verified against the REAL glyph ink
  bounds parsed from the bundled TTF (ink spans +0.04em..+0.96em above
  baseline for every glyph — never below baseline — so the ink is
  designed to center in the natural line box; the natural box's center IS
  the ink center). Also confirmed this Compose version (BOM 2026.05.01,
  plain `Density` with raw `configuration.fontScale`) compensates
  fontScale EXACTLY — the em is constant, so the bug showed at every
  scale ≥ 1.0 and "fixed" only below 1.0 via the `coerceAtLeast(1f)`
  (glyph smaller than box).
- **Fix:** keep the fontScale compensation
  (`size / fontScale.coerceAtLeast(1f)`), restore the natural line box:
  `PlatformTextStyle(includeFontPadding = true)` (default) and NO
  line-height trim — `lineHeight = 1.0em` acts as a Minimum. The 1.2em
  natural box centers the ink in the icon's layout box with ~1dp margin
  top and bottom, at every font scale. `LineHeightStyle` import removed.
  Do NOT reintroduce the trim/padding combo (AGENTS.md v114 bullet has
  the full derivation).

### 2. Pill glow leaking past the pill shape (dark mode)
The user pinned the leak to: progress pill, filter chips / Show all,
category picker tabs/presets — and confirmed it's a shape-mismatch issue
(the glow paints against the pill's bounding box, crossing the capsule's
curved ends). `curioDarkGlow` is a retired no-op, so the culprits were the
One UI dark-mode treatments.
- **curioGlassEdge (CurioGlassEffects.kt):** the top catch is now clipped
  to a capsule mask hugging the pill's TOP contour — 10% side insets,
  55% band height, matching corner radius, top edge glued to the pill's
  top — so the bright band fades before the rounded ends. A mirrored
  BOTTOM band preserves the "shiny glass" bottom catch when the Subtle
  option is off.
- **curioInnerGlow:** radial stays inside the pill's curved rim (capped
  reach + pill-outline clip).
- **Call sites:** category picker `PickerPageTab` + `PickerPresetChip` +
  Mix button and the reveal `RevealAlreadyButton` (all capsule pills)
  switched from the full-width `categoryEdgeShine` band to
  `curioGlassEdge` (+ `curioInnerGlow` 0.12, matching the Spin
  filter-chip family). Modest-corner cards (Start-exploring 24dp,
  topic/settings/hero cards) keep `categoryEdgeShine`.

### Validation
Font geometry verified by parsing the bundled TTF (head/hhea/loca/glyf/
post); Compose trim + Density behavior verified against the androidx source;
imports checked for orphans; no Gradle locally (env rule) — CI validates on
push. Pushed to `main`.

## Previous session — COMPLETED: draft recovery keeps its take + filter Apply pill matches chips

All of that session's work is done, committed and pushed (`e297f91`).

### 1. Draft recovery restored the WRONG take (SaveCaptureScreen)
When "Express yourself" opened with a default take (sound bite, etc.), the
user switched takes, typed, discarded, then tapped "Recover your draft" —
the draft was recreated with `defaultFormat` instead of the take the draft
was actually written in, so it never restored the other take's draft.
- **Fix:** the resume path now uses the draft's own format
  (`draft.format ?: defaultFormat`), so recovery restores the exact take
  the draft belonged to. Verified `CaptureData.format` is the source of
  truth for the take.

### 2. Filter sheet "Show all" pill didn't match the filter chips (SpinScreen)
The bottom "Show all" button used a stock Material3 `Button` while the
filter chips use the custom `CurioCategoryChip` treatment.
- **Fix:** restyled the "Show all" pill to match the chip look —
  rounded-pill shape, accent-tinted fill with deep same-hue ink, the
  chip's tap/press affordances — so the sheet reads as one family.
  Existing imports (Button/ButtonDefaults/PaddingValues) are still used
  elsewhere in the file, so nothing was orphaned.

### Validation
`git diff` reviewed; no Gradle locally (env rule) — CI validates on push.
Pushed to `main`.

## Previous session — COMPLETED: cosmic icon from the designer PNG + detail/filter/icon/pet-hero polish

All of that session's work is done, committed and pushed (`258f533`).

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

# Prompt.md — Request log

## Current request — COMPLETED: detail-screen tear corners, quick-fact box, "…" fold toggle

All of this session's work is done, committed and pushed (`1ab650e`).

The user: "oh yeah the tear is looking good but in some places it reaches
the set and looks cut and please fix the ter style of detail screen. and
also make the box behind the quick fact better and remove the more and
less text, just show ... and nothing hen expanded but keep its touch to
more and less function."

### 1 — Detail tear "reaches the edge and looks cut"
Root cause: the seeded torn bottom edge (`SoftTearParams.broadDisp` =
waves + value noise + the tilted slant `tilt * (nx - 0.5f)`, which alone
drifts ±up-to-10dp between the corners) can notch the hero's corners AT
the screen's left/right edges and read as "cut". Worst on the
edge-to-edge detail hero, whose seed is the ENTRY HASH — so some entries
drew a deep up-bite at a corner. Fix in the SHARED tear path
(PaperCard.kt): `buildSoftTornPath` now fades the displacement to zero
over the last ~5% at each end, and `buildSoftSheetPath` applies the SAME
fade to its torn top + lip wobble so hero and under-sheet stay
pixel-aligned (sheet top stays 4dp behind the hero everywhere). The
middle 90% keeps its full torn character; corners meet the nominal edge.
Applied to every torn hero (Home/Profile/Settings/Cabinet/Detail) since
they share the shape — verified via a python mirror: middle fade factor
1.0, corners ramp to zero.

### 2 — Quick-fact box
`QuickFactCard`'s translucent white @38% plate washed out against the
tinted page wash in light and glowed like a bright sheet in dark. Now a
theme-aware OPAQUE plate: `lerp(surfaceContainerLow, categoryInk, 0.06f)`
light / `lerp(surfaceContainerHigh, ink, 0.10f)` dark, plus a hairline
category-ink rim in dark mode (`Modifier.border`, import added) — the
settings-card language.

### 3 — Fold toggle
"…more" / "…less" words removed: collapsed shows a lone "…" affordance;
expanded turns it `Color.Transparent` — visually nothing, but the tap
target stays the same size, so tapping the same spot still folds it back.

### Validation
Brace balance (both files depth 0); `git diff --check` clean; fade
behavior verified via python mirror; no Gradle locally (env rule) — CI
validates compile on push. Pushed to `main`.

## Previous request — COMPLETED: Updates page redesign (saved notes, markdown, status header)

All of this session's work is done, committed and pushed (`8b0bf57`).

The user: "the updater page looks boring and the update it notes it shows
thats also bad. and why cant it just saves that note intead of reloading
it."

### 1 — Notes are now SAVED (no reload on open)
`AppPreferences.getCachedUpdateInfo` / `setCachedUpdateInfo` cache the
last successful check (tag, notes, htmlUrl, apkUrl). `UpdatesScreen`
loads the cache into `updateInfo` + state BEFORE any network call, so the
notes show instantly on every open; the auto-check then refreshes
silently in the background via `runCheck(keepResult = …)` (keeps the
saved result visible while Checking) and only replaces it on success — a
failed refresh keeps the saved notes and marks the row "Couldn't refresh
· tap to retry". Every successful fetch re-saves, so after updating, the
installed version's notes show. Kotlin gotcha handled: `updateInfo` is a
delegated var — won't smart-cast after null-check, so the failure path
captures it into a local `val saved` first.

### 2 — Release notes render markdown-lite
Hand-rolled parser (`parseReleaseNotes` / `parseInline` /
`ReleaseNotesBlock`) — the project has NO markdown dependency, so the
common GitHub-body syntax is parsed by hand: `#/##/###` headers (bold
title lines), `-`/`*`/`•` bullets (accent dots), `---` dividers
(`HorizontalDivider`), `**bold**` spans (`buildAnnotatedString` +
`withStyle`), `[label](url)` links stripped to labels, `` `code` `` to
plain text. Verified against a realistic body via a python mirror. Used
`StringBuilder.setLength(0)` (not `.clear()` — API-availability safety).

### 3 — Page is no longer boring
Status header: accent status dot (44dp circle, tinted by state — rose
`curioRoseInk` download / sage `curioSageInk` check / error), headline,
subline, and a `v{version}` pill chip — replaces the plain version row.
The update action moved into an accent-tinted banner inside the status
card (Update now title + Download & install pill / progress bar / retry
states / Open release on GitHub). Notes live in their own "What's new"
card with a "View release on GitHub" link when up to date. The update
checker toggle stays at the bottom of the status card; the "Need help?"
card is unchanged. The old `UpdateResultCard` + `AnimatedVisibility` +
`notesExpanded` state are gone.

### Validation
Brace balance (depth 0) + unused-import sweep (only the expected
`getValue`/`setValue` delegate false-positives) + `git diff --check`
clean; parser unit-tested via python mirror; no Gradle locally (env
rule) — CI validates compile on push. Pushed to `main`.

## Previous request — COMPLETED: bare settings icons + carded sub pages + deeper icon lifts

All of this session's work is done, committed and pushed (`8d4ed47`).

The user: "the profile and settings buttons glyph icons are still little
down and also dont give the icons a colored box background just keep the
icon and also unify the sub pages to look like settings options not
transparent texts".

### 1 — Profile/Settings button glyphs STILL low
First-pass lifts (-1/-1.5dp) weren't enough. Deepened: Home `TopBarPill`
(Menu + Person) -1.5 → -2dp; `ProfileSearchPill` -1 → -2dp;
`SettingsNavCard` cog -1 → -2dp; and `CurioBackButton`'s chevron gained
its first lift (-1dp) — the shared back pill on the Profile sticky bar
and every Settings hero. Per-site offsets only — do NOT re-add a global
`CurioIcon` lift (v113 clipping lesson, AGENTS.md v114/v115 bullets).

### 2 — Icons out of colored boxes
`CurioSettingsRow` and `CurioCardHeader` dropped the coral/rose tinted
chip box behind their glyphs — bare 21dp accent-ink icon
(`settingsCardAccentInk`), the `CurioSettingsInfoRow` language. Profile's
`SettingsNavCard` cog also lost its blue gradient block (bare accent
cog, -2dp lift kept). Removed now-orphaned imports in CurioSettingsCard.kt
(background/Box/clip/size/fillMaxSize) + CurioGradients in ProfileScreen.

### 3 — Sub pages carded like hub options
Flat rows that floated transparently on the watermark backdrop now sit in
`CurioSettingsCard(shadowElevation = 0.dp)`: the four section pages via
`SettingsPageContent` (also card-wraps the two-pane hub's right pane),
Experiments (4 sections + info row), Backup tools (3 sections), Support
(3 sections) and the Updates sub-page (2 sections). Promo's showcase
page keeps its preview cards (previews + share buttons, not option
rows).

### Validation
Brace balance verified per file (python tokenizer: all depth 0);
`git diff --check` clean; orphan-import sweeps done; no Gradle locally
(env rule) — CI validates compile on push. Pushed to `main`.

## Previous request — COMPLETED: launcher icon from the designer's new source SVG

All of this session's work is done, committed and pushed (`0310e5d`).

The user: "output 8 is the main icon and output 7 is without the icon
just the background, output 8 have both icons and background. now
properly use this and remove the old png. and make sure the design stays
the same" — then corrected: "no no i uploaded another svg use that for
icon keep the splash screen as it is that good just the icon now".

### What shipped
- The user supplied several `svgviewer-output (N).svg` files at the
  repo root. Outputs 7/8 were a background-only / icon+background pair of
a DIFFERENT (simpler, no-waves, no-frame) design — rejected. The final
upload, `svgviewer-output (5).svg`, matches the current shipped design:
same planet cx=380 cy=320 r=122, moon cx=650 cy=500 r=55, layered waves
at the bottom — pixel-diff vs the old PNG ≈ 2.9%, the ONLY material
change is the missing white frame.
- Rendered to `drawable-nodpi/ic_launcher_icon.png` (2048×2048 via
  sharp, at /tmp) with the SAME card geometry as the old art (~84–88%
  of canvas, same center) → the existing 28dp inset in
  `ic_launcher_foreground.xml` still applies unchanged (card ~44×47dp
  inside the 66dp launcher safe zone).
- `ic_launcher_foreground.xml` now references `@drawable/ic_launcher_icon`
  (comment rewritten v113 → v115: white frame gone, geometry same).
- The SPLASH keeps rendering `@drawable/ic_launcher_art` (the v113
  raster, `drawable-nodpi/ic_launcher_art.png`) untouched — user
  approved it as-is, "just the icon now".
- `ic_launcher_background.xml` (full-bleed sky + stars), monochrome
  (planet+moon silhouette — positions match output 5 exactly) and
  `ic_notification` (path silhouette, same geometry) unchanged.
- New SVG archived at `design/launcher-icon/curio-launcher-icon-v2.svg`;
  root `svgviewer-output` files cleaned up (7/8 had already been
  removed earlier).

### Validation
Geometry verified vs the old raster (84%×88% card, centered); XML
rewrites are trivial (drawable ref swap + comment); no Gradle locally
(env rule) — CI validates on push.

## Previous request — COMPLETED: mood board dual-flow revert + glow, dark Home recents, icon lifts

All of this session's work is done, committed and pushed (`75abe24`).

The user (referencing commit `c5ee2ac`/`12f3ea1`): "i asked you to add a
copy board button which just copies what the outside board had on inside
and it was two different editing and save flow. but you merged it again,
revert that please. and also the glow in dark mode you messed it up, now
the length of the glow is inaccurate, its not covering fully the button.
its visible in the middle only. and bring back the dark mode home screen
recents not being colored. and still the profile and settings icon and
now also the drawer menu and profile button are little to the down but
the icon being cut from button is fixed."

### 1 — Mood board: revert the v114 shared-arrangement merge
`12f3ea1` had merged the inline + full-screen boards onto ONE shared
arrangement; the user's intent was always TWO separate editing/save
flows, with a copy button that copies the INLINE board into the
full-screen editor ("copies what the outside board had on inside").
Restored the v57 dual-list design (`fullTiles`/`fullQuotePositions`
→ `tileLayoutsFull`/`quotePositionsFull`, edited only by the full-screen
canvas) by checking out the pre-merge file, then added a **Copy board**
pill in the full-screen dialog (BottomEnd, above Add images) shown when
`fullTiles.isEmpty()` but the inline board has content: copies inline
tiles into `fullTiles` + inline quote placements into
`fullQuotePositions` (index-aligned; text/style/tilt/width stay shared).
`canSave` keeps counting `fullTiles`. AGENTS.md v114 bullet rewritten
(v114 → v115).

### 2 — Dark-mode glow shrank to the middle
`curioGlassEdge` (from `835677b`) masked its catch to a capsule with 10%
side insets + 55% band height, so the glow only showed mid-button.
Now the vertical-gradient band is FULL-WIDTH and clipped to the pill's
own outline (the curved rim trims the ends) — covers edge-to-edge,
stays inside the shape; the non-subtle option's bottom whisper is the
single gradient's final stop. Dropped the now-unused RoundedCornerShape
import; RoundRect still used by `toPath()`.

### 3 — Dark-mode Home recents uncolored
`RecentEntryRow` + Home `ExploreTopicRow` now use
`surfaceContainerLow` instead of `categorySurface()` when
`isCurioDarkTheme()`; the Recents PAGE (RecentScreen) keeps its tinted
rows. AGENTS.md v89 bullet annotated.

### 4 — Icon spots still a hair low (cut is fixed)
The v114 natural-box centering left a few glyphs' OPTICAL weight low in
compact circular/pill buttons. Per-site lifts: Home `TopBarPill`
(Menu + Person) −0.5dp → −1.5dp; drawer `DrawerNavItem` chips −1dp;
Profile `ProfileSearchPill` magnifier −1dp; `SettingsNavCard` cog −1dp.
AGENTS.md v114 icon bullet annotated (do NOT re-add a global lift — the
old 1dp `graphicsLayer` lift was removed in v113 for clipping).

### Validation
`git diff --cached --check` clean; full GalleryWallFormat diff reviewed
(revert + copy button coherent; no stale v114 references; no external
callers of the removed override params). No Gradle locally (env rule) —
CI validates compile on push.

## Previous request — COMPLETED: songs lane expanded to 1,000 real songs

All of that session's work is done, committed and pushed (`36d5d04`).

The user: "increase the songs to 1000 and use proper real facts and add
some good songs new one 2000s and above."

### What shipped
- `app/src/main/assets/topics/songs.json`: **60 → 1,000 entries** (the
  original 60 preserved byte-for-byte), all tier 1, schema-validated.
- Decade mix: 1960s (66) · 1970s (119) · 1980s (155) · 1990s (118) ·
  2000s (165) · 2010s (273) · 2020s (104) — **~54% are 2000s or newer**
  per the user's "new songs 2000s+" ask.
- Every entry: real song + artist + year, a factual teaser (a quirky
  backstory — sample-based, record-breaking, behind-the-scenes), a
  curiously-framed listen instruction, and Listen action
  (`durationMinutes: 3`).

### How it was authored
One-off generator at `/tmp/gen_songs.py` (per-decade batches) → dedupe
by name+artist+year → ID slug seeded with the existing 60 song IDs so
same-name songs get `-{year}` / `-{artist}-{year}` suffixes → merged with
originals → validated: unique ids across ALL topic files, ≤450-char
teasers, ≤600-char instructions, name ≤80, `verb: "Listen"`, tier 1–3,
`imageUrl: ""`. Trimmed 18 filler entries to land exactly at 1,000.

### Validation
Python mirror of the `validateTopics` Gradle task: 0 errors; cross-file
ID uniqueness vs all 20 other topic files (17,070 topics total now).
Changelog + AGENTS.md updated. No Gradle locally (env rule) — CI runs
`validateTopics` on push.

## Previous request — COMPLETED: mixed-deck colors vivid + smooth hero gradient

All of that session's work is done, committed and pushed (`cfc43d1`).

The user: "fix the new mixed colors some are bad and dont use gradints with
line." Asked which families were bad → **green/teal, magenta/purple, blue**
(red/coral were fine); for the gradient asked for **rounded/random styles**
(the seed-varied brush) but without band lines.

### Root cause 1 — near-black mud in the flagged families
Measured WCAG contrast of every curated pair/triple blend: the flagged
families were the DARK ones (Rose+Teal 0xFF4A12A8 ≈ 11% lightness, dark
blues 0xFF1649C4, dark teals 0xFF15875A) — they'd been "deepened until
4.5:1 vs white" per the docstring, which is unnecessary: the peek cards
deepen each stop per-card (HSL lightness drop) with same-hue deep ink
([pastelFillInk]), and the hero rides the theme-resolved gradient like
single decks. The reds the user liked (0xFFEE0505 etc., contrast 1.2–1.3)
were never over-darkened.
- **Fix:** retuned the flagged pair/triple blends to vivid, clean
  mid-tones in the same hue families (Tailwind 500/600-style): violet
  0xFF8B5CF6, fuchsia 0xFFC026D3, blue 0xFF2563EB, jade 0xFF0BA36D, teal
  0xFF0FA3A3, etc. Contrast ≥ the reds' floor. Red/orange blends untouched.

### Root cause 2 — band "lines" on the hero card
`mixedDeckGradient` emitted accent → curated seam → accent stops (a
handful of saturated stops), which painted visible STRIPES across the
hero in the diagonal/reversed/radial brush.
- **Fix:** theme-resolve each accent (darkAccent / pastelAccent / raw per
  theme), then OKLab-interpolate ~7 fine steps between consecutive
  accents ([oklabGradientStops]) → smooth multi-hue glide, no seams.
  The per-deck brush styles (diagonal / reversed / radial by seed) are
  KEPT per the user's "rounded or random" ask. 4+ accents still fall to
  [oklabCentroid].
- Corrected the object docstring's stale "every blend clears 4.5:1
  against white" claim.

### Validation
Contrast verified with a python WCAG script (candidates ≥ the reds' 1.2–
1.3 floor); diff + braces reviewed; `git diff --check` clean; no Gradle
locally (env rule) — CI validates on push. Pushed to `main`.

## Previous request — COMPLETED: mood board full-screen editor save bug + Copy board button

All of that session's work is done, committed and pushed (`12f3ea1`).

The user: "the moodboard full screen editor needs fixing too when it saves
it changes and also keep the photo and quote add different for both but
also add a copy button if one is empty and other one have something."
Clarified: full-screen arrangement was cut on save (not "saved differently"),
quote cards saved small/out of position; copy = duplicate the existing
content. Asked for the copy-button behavior — user chose **Copy whole
board** (duplicates ALL tiles + quote cards, always available when the
board has anything).

### Root cause — dual board arrangements (`fullTiles` / `fullQuotePositions`)
The format kept a SECOND tile list + quote-position list for the
full-screen editor (saved to `tileLayoutsFull` / `quotePositionsFull`),
seeded from the full layouts and edited ONLY by the expanded canvas:
- Arranging in full-screen changed only the full list → the saved small
  card (which renders the INLINE `tileLayouts`) showed a different, "cut"
  board; fresh boards opened the full-screen editor EMPTY (the full list
  never synced from the inline adds).
- The quote size/position save bug is the same root cause: the small card
  rendered the inline positions (never-dragged `(-1,-1)` → deterministic
  slots against an EMPTY extent) instead of the arranged placements.

### Fix — ONE shared arrangement
- Both canvases (inline + full-screen) now edit the SAME `tiles` list and
  `quoteCards` state. Save still writes `tileLayoutsFull` /
  `quotePositionsFull` as MIRRORS of the shared lists (legacy readers of
  the expanded board / export fall back to them).
- Legacy migration: editing a dual-list entry seeds the shared list from
  the FULL layouts when the inline ones are empty ("if one is empty and
  the other has something, copy the populated one") — legacy
  full-screen arrangements survive a re-save.
- Dead v57 plumbing removed: `quotePositionsOverride`,
  `onMoveQuoteOverride`, `onResizeQuoteOverride` params, `fullTiles`,
  `fullQuotePositions`, the index-alignment `onCardRemoved` hook.
- **Copy board button** (v114): pill (28dp, `CurioIcons.ContentCopy`),
  inline BottomCenter / full-screen BottomEnd above Add images, shown
  when `tiles.isNotEmpty() || quotes.isNotEmpty()`. Copies every tile
  with a +28dp nudge + 5° rotation, every quote card with same
  text/spans/tilt/style/color/width and a fresh `(-1,-1)` position → its
  own deterministic slot.

### Validation
diff reviewed; dead refs confirmed gone (grep); imports still used
(LaunchedEffect/Random/NotePaper*); no Gradle locally (env rule) — CI
validates on push. Pushed to `main`.

## Previous request — COMPLETED: stock buttons converted to the pill/chip language

All of that session's work is done, committed and pushed (`4bbcc64`).

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

# Prompt.md — Request log

## Current request — color-mix research for dark mode (v83)

### What was asked
"doo the color mix research" — the "mixed color shades" research from the
v81 dark-mode spec (main card shades + mixed color shades).

### What the app does today
- Mixing is HSL-space: `hslBlend` (midpoint blend, +0.05 sat boost),
  `hslCentroid` (circular hue mean + sat boost + WCAG 4.5:1-vs-white
  lightness steering), `hslGradientStops` (HSL interp with achromatic
  anchoring), all via local `toHsl`/`fromHsl` (no Compose color accessors
  in the BOM — conversions are hand-rolled).
- `CurioMixedDeck.PairBlends`/`TripleBlends` — hand-curated premium blends
  for the six research accents, steered off the olive dead zone.
- Dark mode: singles resolve `darkAccent` (×0.80 sat, L≈0.44 — matches
  the ~20% desaturation consensus); pastel-dark pairs/triples soften via
  `pastelAccent(blend, dark=true)`; NON-pastel dark pairs/triples reuse
  the light-designed curated deep blends as-is; gradients end on black.

### What the research says (sources below)
1. **OKLab is the interpolation gold standard** — perceptually uniform
   lightness + hue-linear: no gray banding (RGB), no purple tints
   (CIELAB blue paths), no non-perceptual lightness (HSL yellow-vs-blue).
   CSS Color 4 ships `in oklab`/`color-mix` on it; Tailwind moved to it;
   matplotlib viridis descends from it. Conversion is cheap: gamma → 3×3
   (LMS) → cube root → 3×3. Cartesian OKLab for interpolation/gradients;
   cylindrical OKLCH for building palette steps; when one endpoint is
   achromatic (black/white/gray) interpolate in OKLab, not OKLCH.
2. **Dark-mode mixing**: desaturate accents 15–25% (~20 pts consensus),
   never 100%-saturated accents on black, elevation via luminance not
   shadows, keep WCAG AA on fills.
3. **Dead zones**: RGB/HSL cross muddy midtones (amber↔teal olive);
   OKLab interpolation natively avoids them — the curated tables exist
   because HSL still isn't perceptual enough.

### Gap analysis
- HSL is already better than RGB, but blends average lightness NUMERICALLY
  not perceptually; unmapped pairs + 4+ mixes swing hue where the curated
  tables don't cover; the tables are designed for LIGHT.
- Dark-specific gap: non-pastel dark pairs/triples don't get the same
  ~20% desat treatment as singles.

### Recommendations (mapped to code)
1. Add local `oklab*` conversions (~50 lines pure math, same version-proof
   pattern as `toHsl`/`fromHsl`, no deps).
2. `oklabBlend` replaces `hslBlend` fallback; `oklabCentroid` replaces
   `hslCentroid` (perceptual mean over hue-circle mean) for 4+/unmapped.
3. `oklabGradientStops` for `cardGradient`/`mixedDeckGradient`/
   `heroBlendGradient` (achromatic endpoint → OKLab cartesian).
4. Keep curated Pair/Triple tables as premium overrides.
5. Dark non-pastel: apply the `darkAccent` recipe to blends so mixed
   shades get the same new-shade-of-the-same-spectrum treatment as
   singles; keep 4.5:1-vs-white steering for light fills.
6. Optional: steer lightness in OKLCH (chroma-preserving) instead of HSL.

### Sources
- Evil Martians — OKLCH in CSS: why we moved from RGB and HSL.
- Chris Coyier — "Two Things Not Great About OKLCH" (Oklab for gradients).
- Tailwind Labs issue #14955 (uniform OKLab for interpolation).
- pavelp.cz — RGB vs CIE LCH vs OKLCH gradients (blue-purple tint demo,
  achromatic fallback rule).
- Eleken / ColorArchive / Nate Balcom / paletacolorpro — dark palettes:
  15–25% desaturation, no full-sat accents, elevation via luminance.

### Status
Research delivered (v83). Awaiting user decision on implementing the
OKLab swap + dark blend treatment (not yet coded).

## Prior — dark-mode audit: everything left light-only (v82)

### What was asked
"continue audit the app for anything thats left in dark mode."

### What was found + fixed
Systematic sweep of every screen/component for light-only fills, ink and
surfaces:
1. **PaperStatCard** — the paper fill was warm cream in dark (glared on
   black): `paperStatCardColor` now resolves a deep near-black warm tint of
   the base hue in dark. The steel-ring shading used hardcoded dark greys
   that vanished on the dark paper: threaded a `dark` flag through
   `paperStatCardFill` → all three ring styles, flipping the back arcs /
   dives / split gap to LIGHT metal tones in dark (Home + Detail + Profile
   callers pass `isCurioDarkTheme()`).
2. **Streak pill** — DeepPlum text on translucent butter vanished on black:
   ink flips to bright ButterYellow in dark.
3. **CurioHeroShuffleCard** — the wildcard hero kept the pale CoralBlush
   fill in dark: now wears `HomeRosewoodDark`; pastel content ink flips to
   the bright twin.
4. **Home empty-state "Surprise me"** — DeepPlum icon/text on the deep rose
   pill → bright twin in dark.
5. **BugReport + Support CTAs** — pale CoralBlush fill + DeepPlum content
   glared on black: deep rose fill + bright twin ink.
6. **Reveal pastel hero gradient** — the pastel-light stops rendered as a
   bright pastel on black (missed in v81): now the muted deep-pastel twin.
7. **Paper control accent** — the v78 removal deleted its dark branch, so
   the rich-text toolbar/cursor/chips sat on the dark page in dark amber:
   `paperControlAccent` flips to bright ButterYellow in dark. (The note
   PAPER sheets themselves stay theme-agnostic by design — documented
   contract: a note reads as a physical slip.)
8. **Quests** — the Claim pills used the bright accent twins as fills in
   dark (white text washed out): now deep `CoralInk` / `GoldInk` /
   `CategoryTeal` fills (light behavior unchanged); the weekly teal pill
   gets a deep fill in both modes (the soft legacy teal never carried
   white); two small coral icon chips flipped to deep rose in dark.
9. **CurioEmptyState CTA** — pale CoralBlush fill + near-white content in
   dark: deep rose fill + bright twin.

Checked-and-clear: badge metals / brand colors / category ink twins / pet
sprite + flower-bed (has its own night palette) / keyboard illustration /
promo export poster (self-contained asset) are intrinsic; dropdowns, search
field, sort pill, top bar, toast, bubble, session service, Topic Database,
RecycleBin all resolve through the dark scheme or the theme-aware helpers.

### Validation
All touched files brace/paren-balanced (Reveal keeps its pre-existing
string-literal 1-off, unchanged by these edits), `git diff --check` clean,
no duplicate imports. No Gradle locally (env rule) — CI on push.

## Prior — reimagined dark mode: pitch black + Samsung One UI 9.5 glow (v81)

### What was asked
"now lets reimagine the dark mode… background pitch black, torn header a NEW
SHADE of the same color spectrum, buttons/texts reversed (light ink on dark
fills), proper dark-mode color research for shades, no background tint in
dark (watermarks carry identity), colorful things glow reflected INSIDE the
pill (One UI 9.5 language), 1% whitish gradient pill edges (not a border),
and a Samsung-style formula for the spin shuffle page. Consider every
screen."

### Research done
- **One UI 9.5 visual depth** (Samsung developer docs + Aug 2026 coverage):
  floating pills with a gradient INNER glow, a soft radial highlight, frosted
  "lens" glass, shiny glass edges — no border outlines. Mapped 1:1 to the
  ask: `curioInnerGlow` (radial accent-light pushed in from top-left, clipped
  inside the shape) + `curioGlassEdge` (1% whitish top-lit gradient, not a
  border), dark-only no-ops.
- **Dark-mode color science**: desaturate accents ~20 points (saturated
  colors vibrate on black); communicate elevation by stepping surface
  lightness UP; never invert light colors — build dark-specific shades.

### What was done (user chose: Light/Dark/System picker AND everything in one pass)
1. **Pref** — `themeModeState` (Light/Dark/System) re-added to AppPreferences
   + `initThemeMode` seeding.
2. **Seam** — `isCurioDarkTheme()`/`isCurioDarkThemeForContext()` now read
   the pref (System follows device).
3. **Dark scheme** — `CurioDarkColorScheme`: pitch-black background, surfaces
   step up through near-black greys (elevation via lightness), bright accent
   roles.
4. **CategoryInk** — dark branches everywhere: `categoryInk`/`readableAccentInk`
   → the LIGHT 300 twin (the reversal); `themedAccent` → `darkAccent` (same
   hue, L≈0.44, ~20% desat); `headerAccent` → same-hue dark hero shade
   (L≈0.34); `categoryBackgroundWash` → pure black (no background tint in
   dark); surfaces → `darkSurfaceTint`/`darkChipTint` (hue at near-black
   lightness).
5. **CurioColors** — dark branches for card gradients + the dark hero twins.
6. **New modifiers** (`CurioGlassEffects.kt`) — `curioGlassEdge` (1% whitish
   edge) + `curioInnerGlow` (Samsung radial glow), dark-only. Wired into the
   shared edge shine (`categoryEdgeShine` draws the whitish edge in dark),
   the search field, sort pill, settings card and progress pill; SpinButton
   wears the inner glow (Samsung shuffle formula) and the orbit dots flip to
   the accent's light twin so they glow on black.
7. **Pickers** — Settings → Appearance and onboarding theme step re-gain the
   Light/Dark/System choice.
8. **Screens** — Home, Profile, Settings family, Cabinet, TopicHistory,
   Onboarding, Pet designer canvas, Detail and Reveal got dark branches for
   every hardcoded light fill (sticky pills, hero paper, frosted glass,
   progress dialog).

### Validation
Brace/paren deltas vs HEAD are additions of balanced blocks (all open==close
per file), `git diff --check` clean, no duplicate imports, watermark already
carries its dark alphas. No Gradle locally (env rule) — CI on push.

## Prior — category picker: smaller two-line title, hint removed (v80)

### What was asked
"the theme removal got pushed? and also decrease the text size in category
picker What are e exploring and remove the text below that. and make the
title go What are we then next line Exploring?"

### What was done
1. **Push confirmed** — yes: pushing the v79 pill fix carried the v78
   light-only removal along (same branch); both are on `origin/main`
   (`f59ac52` + `1714ea4`).
2. **CategoryPickerSheet banner (SpinScreen.kt):** the title stepped down
   34 → 28sp and now wraps onto two lines via an explicit line break
   ("What are we\nExploring?") with a tight 34sp line height; the mode-
   hint subtitle ("Tap a deck to spin it. Hold to pick several." / "Tap
   to toggle decks · Done to spin together") was removed entirely. The
   deck-status chip (category name / "N selected") stays. Note the old
   `maxLines = 1` had to go with the manual newline or the second line
   would have been ellipsized away.

### Validation
Brace/paren delta vs HEAD = 0, `git diff --check` clean, old hint strings
gone from the file. No Gradle locally (env rule) — CI on push.

## Prior — Cabinet + Topic Browser: sort pill and search pill equal middle size (v79)

### What was asked
"in cabinet and topic browser, can u make the search icon shape and the sort
icon shape which gets expanded. give them the equal middle size treatment.
all of its dimention needs to be same, coz rn the sort pill looks big and
the search icon pill small"

### What was done
Root cause: the sort dropdown's Row carried `heightIn(min = 52.dp)` (its
v43 comment still claimed "the same 42dp height") while the icon-only
Search pills ([SettingsHeroActionPill] / [CabinetHeroActionPill]) were
42dp with 22dp glyphs — so the sort pill read big next to the small
search pill. Unified at the middle:

1. **Height 46dp for both** — `CurioSortDropdown` 52 → 46dp;
   `SettingsHeroActionPill` + `CabinetHeroActionPill` 42 → 46dp (shared
   components, so Settings/Profile/Support hero pills stay uniform too).
2. **Glyphs 20dp for both** — the sort-type icon 16 → 20dp; the hero
   action-pill glyphs 22 → 20dp.
3. Everything else already matched: full 50dp capsule, 3dp elevation,
   same fills. Width stays content-driven (the sort pill carries the
   label + chevron + direction arrow, so it can't be width-equal to an
   icon-only pill).

### Validation
Brace/paren deltas vs HEAD = 0 on all three files, `git diff --check`
clean. No Gradle locally (env rule) — CI on push.

## Prior — light-only: dark/AMOLED/Material removed (v78)

### What was asked
"remove the dark mode and amoled mode entirely. just light mode, no dark
or amoled mode. and dont push this just commit and sk me if to commit"

User confirmed via ask_user: **Curio light only**; **full code clean up**
but keep a clean seam for the future dark system (no scattered hardcoding);
**commit locally, no push**.

### What was done
28 files, −1494 / +375 lines. The entire dark/AMOLED/Material machinery is
gone; `isCurioDarkTheme()` / `isCurioDarkThemeForContext()` remain as
documented `false` seams for the future dark system.

1. **AppPreferences** — `themeStyleState` + `themeModeState` prefs,
   constants, getters and setters deleted (Theme style / Theme rows already
   removed from Settings → Appearance, which now keeps only Category tint,
   Pastel colors, Hero picker and Adaptive Hero).
2. **CurioTheme** — dark/AMOLED ColorSchemes + Material dynamic palette
   deleted; `curioTheme()` collapses to the Curio light scheme; the two
   seam helpers return `false`; the few remaining `if (isCurioDarkTheme())`
   sites (pill lifts, background ink) keep resolving to the light path as
   the future switch points.
3. **CategoryInk** — Material/AMOLED/dark branches stripped from the ink,
   accent and button-fill resolvers; dark-wash tuning table deleted;
   `categoryInkFor`/`themedAccentFor`/`pastelAccent` keep their `dark`
   params, fed by the `false` seam (watermark backdrop, session service).
4. **CurioColors** — dark branches removed; `categoryCardFill` loses its
   `dark` arg; dead dark constants kept minimal. `PaperPalette` collapsed.
5. **Screens** — every `isCurioDarkTheme` / `THEME_STYLE_*` branch in
   Profile, Home, Spin (deck gradients, ticket brush, shuffle plate, deck
   controls, sheets), Detail (hero start, frosts, waveform inks, mood
   board), Reveal (band paper, hero brush, pills), Onboarding (theme step
   is now a single pastel toggle; `ThemeModeChip` deleted), Category
   Picker, DeckPresets, PetDesigner dialog, CaptureFormatComponents,
   RecycleBin and the shared components collapsed to their light paths;
   unused imports cleaned (incl. the dead private `contrastRatio` +
   `kotlin.math.pow` in Detail).
6. **Seam kept** — `isCurioDarkTheme()`/`isCurioDarkThemeForContext()`
   stay as the future dark-mode switch points; watermark + session-service
   plumbing (which passes the seam through) is untouched by design.

### Validation
Brace/paren deltas vs HEAD = 0 on all 32 touched files, `git diff --check`
clean, zero references left to `themeStyleState`/`themeModeState`/
`THEME_STYLE_*`/`THEME_MODE_*`/`setThemeMode`/`setThemeStyle`, no code-level
AMOLED branches remain (comments only). No Gradle locally (env rule).
**Committed locally — NOT pushed** (per user).

## Prior — back buttons: opaque theme-aware pills (v77)

### What was asked
"the back button in settings and its sub pages and in many page are
transparent so fix that by using the same non transparent theme aware type"

### What was done
1. **Root cause:** the settings-family heroes' back pills were the last
   translucent holdout — `symbolTint.copy(alpha = 0.18f)` (Settings hub +
   every sub page via `SettingsHeroHeader`) and `ink.copy(alpha = 0.18f)`
   (Topic History) — while the hero ACTION pills were already converted to
   an OPAQUE fill in v27n (`lerp(backdrop, curioPillTintLift(), 0.38f)`).
2. **Fix:** both back buttons now use the action-pill construction
   exactly: `containerColor = lerp(fill, curioPillTintLift(), 0.38f)`
   (theme-aware: rose-kissed page lift in light, white lift in dark, grey
   glass in AMOLED) + `shadowElevation = 3.dp` so they read raised like
   the pills beside them. Glyph keeps the hero's readable ink.
3. Unchanged: the default `surfaceVariant` pill (BugReport / Category
   Picker / FieldMind / SaveCapture), Profile's solid hero-fill pill, and
   the detail page's near-opaque frosted sticky plate (deliberate design).

### Validation
Brace/paren balance clean (SettingsHub 172/172 + 547/547, TopicHistory
148/148 + 355/355), no `copy(alpha = 0.18f)` left, `git diff --check`
clean. No Gradle locally (env rule) — CI on push.

## Prior — detail page Date · Mood · Session · Type card: opaque theme-aware pane (v76)

### What was asked
"do the same with the detail page date type that stat card"

### What was done (EntryDetailScreen.kt — the hero's Date · Mood · Session ·
Type meta card)
1. **Default fill is now OPAQUE and theme-aware** — the old frosted-glass
   default (a translucent `heroStart` bloom at 30/16% alpha + a white or
   midnight `heroFrostBrush` glass) is replaced by a single opaque vertical
   gradient: `lerp(heroSheetColor, heroStart, 0.30f)` → `lerp(heroSheetColor,
   heroStart, 0.16f)`. `heroSheetColor` is the page's theme-aware sheet
   (near-white light / midnight dark + AMOLED), so the pane keeps the same
   perceived category bloom while the elevation shadow renders clean behind
   it — the same language as Profile's stat pane / Home's Streak card. The
   hero's ink (`heroCardInk`) contrast is preserved (the pane luminance
   doesn't change — unlike Home/Profile, the detail hero is a deep category
   color, so the lerp is off the light/dark sheet, not the banner fill).
2. **Elevation + dark glow always on** — `shadowElevation` 3dp and
   `curioDarkGlow(3.dp, metaShape)` now always apply (they were 0dp/flat
   for the frosted default), mirroring Home/Profile.
3. `heroFrostBrush` removed (no consumer left); `curioDarkGlow` imported.
4. The "Paper stat card" experiment branch is untouched.

### Validation
Brace/paren balance clean (519/519, 1935/1935), `git diff --check` clean,
`lerp`/`Color`/`Brush` imports already present. No Gradle locally (env
rule) — CI on push.

## Prior — Home stat card: opaque theme-aware pane like Profile (v75)

### What was asked
"the home screen stat card you see how its transparent, make it be similar
to the profile screen stat card with theme aware"

### What was done (HomeScreen.kt — the Streak · Cabinet · Topics pane)
1. **Default fill is now OPAQUE and theme-aware** — the old transparent
   glass (`statGlass.copy(alpha = 0.12f)` → 55% white-lift) read as a see-
   through wash and let the elevation shadow bleed. It now uses the exact
   construction Profile's stat pane wears: an opaque vertical gradient of
   `lerp(fill, White, 0.06f)` → `lerp(fill, White, 0.26f)`, with an AMOLED
   step toward `HomeRosewood` at 0.30 (Profile's v27n treatment).
2. **Elevation + dark glow always on** — `shadowElevation` 3dp and
   `curioDarkGlow` previously applied only under the "Paper stat card"
   experiment; they now always apply (mirroring Profile), so the opaque
   pane reads raised off the banner in every theme instead of flat.
3. The "Paper stat card" experiment branch is untouched.

### Validation
Brace/paren balance clean (284/284, 979/979), `git diff --check` clean.
No Gradle locally (env rule) — CI on push.

## Prior — category picker sheet: tear hero to the status bar, no dismiss button (v74)

### What was asked
"Apply the same status-bar tear treatment to the category picker sheet on
Spin and remove that dismiss button, and the sub text tap a deck to spin
it. hold to pick several."

### What was done (all in SpinScreen.kt `CategoryPickerSheet`)
1. **Tear hero to the status bar** — same treatment as the filter sheet
   (v70): `shape = RectangleShape` (flush top corners), `dragHandle = null`,
   `contentWindowInsets = { WindowInsets.navigationBars.union(WindowInsets.ime) }`
   so only the bottom + IME insets are consumed and the banner fills the
   very top edge behind the status bar. Banner height grows with the
   status-bar inset (`118.dp + statusBar`), title/hint/chip clear it with
   `statusBarsPadding()`. The banner is full-bleed; the deck grid + action
   row stay centered in the `CurioContentMaxWidth` column on wide windows.
   Distinct tear seed (`0xC4A71E`), category gradient + watermark glyphs
   (the FilterSheet construction).
2. **Dismiss button removed** — the Close X (and the floating drag handle)
   are gone; swipe-down, scrim tap and Mix/Cancel still dismiss. The
   header content moved onto the banner: 34sp title, the mode hint as
   subtitle, and the current-deck / "N selected" chip as a hero-glass pill.
3. **Sub text** — single-select hint now reads "Tap a deck to spin it.
   Hold to pick several." (period instead of the · separator); the
   multi-select hint stays "Tap to toggle decks · Done to spin together".
4. Column dropped its `navigationBarsPadding()` (the contentWindowInsets
   handle the bottom inset now, mirroring the filter sheet) and unused
   imports removed (`navigationBarsPadding`, `BottomSheetDefaults`).

### Validation
Brace/paren balance clean (545/545, 2037/2037), `git diff --check` clean,
no stale references. No Gradle locally (env rule) — CI on push.

## Prior — filter-sheet group pills carry their group icons (v73)

### What was asked
"Give the Spin filter sheet group pills (Type/Genres/Era/Origin/Franchise)
the same per-group icons as their labels"

### What was done
The v70 section labels already wore a per-group glyph (`FilterGroupKey.glyph`:
category / style / history / public / movie), but the accordion PILLS
(`FilterGroupPill`) were label + badge + chevron only. The pill now leads
with the same glyph (accent-tinted when closed — the SectionLabel's icon
look — content `ink` when open on the accent fill), and the row's start
padding trimmed 20 → 18dp so the icon sits comfortably. Call site passes
`glyph = key.glyph`.

### Validation
Brace/paren balance clean (541/541, 2002/2002), `git diff --check` clean.
No Gradle locally (env rule) — CI on push.

## Prior — Settings/Profile option cards + icons follow the hero color (v72)

### What was asked
"in profile screen and setting screen, the background of the options not
the main background, the background card the options sit on and the icons
they have. well they don't match the hero color, it's always rose color...
give me proper options so that i can point out you're correct and then
you can continue your work"

User chose: **Full match — cards + icons take the lane color; Always
follow the hero's color.**

### What was done
1. **Root cause:** the shared option-card primitives
   (`CurioSettingsCard` fill, `CurioCardHeader` + `CurioSettingsRow`
   icon chips + glyphs) were hardcoded rose — `curioPillTintLift()` for
   the card fill, `CoralBlush` chips with `curioRoseInk()` glyphs —
   while the hero banner wears the Spin lane's accent (Adaptive Hero) or
   the sky-azure, so the option cards never matched the hero.
2. **Three new hero-aware resolvers in SettingsHubScreen.kt** (next to
   the other shared hero-family helpers): `settingsCardAccentInk()`
   (glyph ink: lane → `categoryInk()`, azure → deep azure twin in light /
   pale azure in dark, else rose), `settingsCardChipTint()` (chip hue:
   lane → `themedAccent()` light / `lightAccent` dark, azure, else coral
   — dark keeps the pale-glass chip look), and `settingsCardTintLift()`
   (card-fill twin of `curioPillTintLift`: identical construction and
   strength, hue follows the hero). Material/AMOLED keep the rose — their
   banners wear scheme roles, same gating as `settingsRoseAccent()`.
3. **CurioSettingsCard.kt:** the card fill, header chip + glyph, and row
   chip + glyph now resolve the hero's hue, so Profile + Settings (and
   every screen sharing these primitives — Support, Backup, Experiments,
   Quests) match the banner in every theme mode.

### Validation
Brace/paren balance on both edited files, `git diff --check` clean, no
unused imports (CurioColors / curioRoseInk / curioPillTintLift removed
from CurioSettingsCard.kt). No Gradle locally (env rule) — CI on push.

## Prior — pet designer: eye-size presets fixed + whole-pet size option (v71)

### What was asked
"the eye size in pet designer doesnt work, and also we can scale up the
pet right, so give an option to scale up the custom pet as well."

### What was done
1. **Eye-size fix (root cause found).** The presets scaled each eye's
   pixels AROUND the eye center and snapped to integer cells
   (`(center + (cell - center) * scaleF).roundToInt()`). The default
   eyes are only 2px wide (OPEN/BLINK/CLOSED/WIDE = ±0.5 cells from the
   center; STAR/DIZZY = ±1.5), so the 0.85/1.2 factors shifted every
   pixel by < 0.5 cells and rounded RIGHT BACK to the authored cells —
   Small/Medium/Large were pixel-identical, i.e. the buttons did
   nothing. `CurioPetSprite` now scales the eye art in DRAW space around
   each eye's center (the detail-layer transform trick: `DrawScope.scale`
   per eye at pivot 4.5/7 and 10.5/7) with stronger factors
   (0.72 / 1.0 / 1.35), so every eye style visibly shrinks/grows, and
   the placement offset applies unscaled after the scale.
2. **Whole-pet size option.** New `PetDesign.petScale` preset
   (0 small / 1 medium / 2 large), serialized as `petscale=` and parsed
   tolerantly (legacy designs → 1). `CurioPetSprite` multiplies its
   sprite box by the preset (0.8 / 1.0 / 1.3) on top of the caller's
   stage `sizeScale`, so the custom pet scales up EVERYWHERE it renders
   (floating pet, flower bed, quests, all designer previews). The Pet
   Designer Settings page gained a **"Pet size"** card (before the Eyes
   card) with a live preview + Small/Medium/Large + Reset size, writing
   `design.copy(petScale = ...)` with undo — same pattern as the Eyes
   section.

### Validation
Brace/paren balance: CurioPetSprite 127/127 + 508/508, PetDesign
234/234 + 1040/1040, PetDesignerScreen matches its HEAD baseline (+4/+4
braces, +7/+7 parens — the +4 brace skew is pre-existing HEAD noise from
comment/string content, not my edits). `git diff --check` clean. No
Gradle locally (env rule) — CI on push.

## Prior — filter sheet tear-to-status-bar + watermark + group-label icons (v70)

### What was asked
"v68 — the header text steps up (30sp)... in here increase the category
text size and make the hero tear go all the way up to the status bar (spin
screen filter page). remove the active filters extra thing and also its
divider. the text below the groups (Type · Genres · Era · Origin ·
Franchise) — make it a little better, fix its margin, and give each group
an icon as well. similar to other backgrounds, give the filter page
background a watermark style too."

### What was done (all in SpinScreen.kt `FilterSheet`)
1. **Tear hero to the status bar** — the sheet dropped its 28dp rounded
   top corners + floating drag handle (`shape = RectangleShape`,
   `dragHandle = null`) and consumes only bottom + IME insets
   (`contentWindowInsets = { WindowInsets.navigationBars.union(WindowInsets.ime) }`),
   so the category banner fills the very top edge behind the status bar
   like every page hero. Banner height grows with the status-bar inset
   (`118.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`)
   and the title/Clear-all row applies `statusBarsPadding()`.
2. **Category name 30 → 34sp** in the tear hero.
3. **Active-filters strip + divider removed** — the selected-chip summary
   was redundant; the dead `ActiveFilterChip` composable was deleted with it.
4. **Group section labels upgraded** — `FilterGroupKey` gained a per-group
   glyph (`category`/`style`/`history`/`public`/`movie`, all verified
   present in the bundled Material Symbols font via the post-table glyph
   names); `SectionLabel` renders an accent-tinted glyph + 17sp ExtraBold
   text with cleaner 10/8 margins; the accordion top margin tightened
   14 → 6dp (divider gone).
5. **Watermark backdrop** — the sheet body now wears
   `CurioWatermarkBackdrop` (`alphaScale = 0.5f`, `topClearance` = hero
   height), matching the muted category-glyph collage of other pages.

### Validation
Brace/paren balance OK (SpinScreen 534/534, 1653/1653); `git diff --check`
clean; no `ActiveFilterChip` references remain; five group glyphs verified
in the bundled font. No Gradle locally (env rule) — CI on push.

## Prior — mood board editor fixes + chip-bar animation (v69)

### What was asked
"in moodboard make the import be universal. and fix the inline editor
still behaving differently with glitches and saving it in detail view
doesnt show the same inline view as it was in editing. and its still
resizing. make the mood collapse once a mood is selected. and in cabinet
and topic browser the category row appear is glitchy it appears with a
delay and with no animation so fix that."

### What was done
1. **Universal import** — mood board image picker switched from
   `OpenMultipleDocuments` (raw documents UI) to the Android Photo
   Picker (`PickMultipleVisualMedia` + `PickVisualMediaRequest(ImageOnly)`).
2. **Inline editor == saved view** — `MoodBoardCanvas`'s crop extent was
   frozen once per session (stableBoardMaxX/Y + seeding LaunchedEffect),
   which diverged from the saved card the moment a tile was added or
   dragged past it (saved view re-fitted → "resizing"). The extent is now
   the live tile-set bounding box — same formula as the saved view; the
   drag preview lives inside the tile, so the fit stays constant mid-drag.
3. **Mood collapse on pick** — `moodSelectorOpen = false` inside
   `MoodChipsRow.onMoodChange`.
4. **Chip-bar animation** — the Cabinet/Topic DB chip bars sit at a large
   `.offset(y = barTop)`, so `expandVertically` (height+clip) hid them
   until the clip finished (delayed pop). Enter/exit switched to
   `slideInVertically/slideOutVertically` + fade so the chips slide out
   from under the torn hero.

### Validation
Brace balance OK (4 files); `git diff --check` clean. No Gradle locally
(env rule) — CI on push.

## Prior — hero-tinted icons, pill search, tear filter hero, sort icons (v68)

### What was asked
"the settings and profile icon get rose color but it should be theme aware
with the hero color and also the cream background its not getting the tint.
in filters and settings make the search bar match the app look and pill
shaped like in cabinet. and the filters give it the tear hero style too
with increasing the text size of header books or category name and also
give proper margin as the filters type gets off set. and in cabinet the
sort pill give it the icon of the sorting type and make the pill slimmer
please."

### What was done
1. **Settings/Profile icons theme-aware** — hero watermark symbols/back
   pill dropped the AMOLED HomeRosewood override and ride the hero's
   readable ink (already theme- + lane-aware); the cream paper under the
   tear lerps 10% toward the hero fill.
2. **Search bars pill-shaped** — `CurioSearchField` 16dp box → 50dp pill
   (Settings hub, Spin filter sheet, Topic History).
3. **Filter sheet tear-hero header** — category banner with SoftTornBottomShape,
   torn-edge shadow, watermark glyphs, 26sp category name + Clear-all pill
   riding it; groups column margins fixed (14dp top, section label
   6/6 top/bottom).
4. **Sort pill sort-type icon + slimmer** — `CurioSortOption.glyph`
   (CalendarToday/FormatText/Tune for Cabinet; AutoAwesome/FormatText/
   CalendarToday for Topic DB), icon shown before the label, padding
   trimmed 8/6→6/4, min-width 88→76dp.

### Validation
Brace balance OK (7 files); `git diff --check` clean. No Gradle locally
(env rule) — CI on push.

## Prior — progress dialog colors off on the reveal page (v67)

### What was asked
"inside topic reveal page progress dialog the color are still very off,
fix it"

### What was done
Root cause: the reveal pill passes the RAW category accent into
`CurioProgressEditorDialog`, which used it for the −/+ stepper glyphs and
Save label ON the theme's onSurface — deep accents (navy/indigo) went
dark-on-dark. Reworked the dialog so [contentColor] drives everything:
- Reveal + detail pills now pass `dialogContentColor = cat.categoryInk()`
  (readable deep accent in light / light twin in dark).
- Ring, steppers, slider, title, % all ride contentColor.
- `StepButton`: soft 14% tint circle + full-color glyph (was solid
  onSurface circle + raw accent glyph).
- Save button: contentColor container paired with
  `MaterialTheme.colorScheme.surface` label (contrasts in both modes).
- Removed the dialog's now-unused `accent` parameter (only caller was
  the pill).

### Validation
Brace balance OK (3 files); `git diff --check` clean. No Gradle locally
(env rule) — CI on push.

## Prior — progress visibility (cabinet line + detail pill) (v66)

### What was asked
"the progress indicator in cabinet isnt really visible in light mode pastel
and please place the progress pill of detail view which is at hero buttom
corner to the screen buttom corner. and fix its progress indicator too"

### What was done
1. **Cabinet progress line** (`CurioTopicCard` strip under the hero):
   themedAccent resolves to a light pastel twin in pastel-light that
   washed out on the cream hero — fill now uses `categoryInk()` in light
   mode (hue-preserving deep accent) / accent in dark; track alpha
   0.18 → 0.32, line 4 → 5dp.
2. **Detail pill moved** to the screen bottom-right corner (out of the
   hero Box into the screen-level Box beside `DetailStickyBar`, BottomEnd
   + 16dp; no navigationBarsPadding — the NavHost Scaffold already pads).
3. **Pill bar fixed** (`CurioProgressPill` slim bar): fill was accent
   (pastel-light invisible) → deep category ink in light / accent in
   dark; track 0.25 → 0.30 alpha. Reveal count-only badge untouched.

### Validation
Brace balance OK (3 files); `git diff --check` clean. No Gradle locally
(env rule) — CI on push.

## Prior — revert f991db1 (serif body text toggle) + new pet designer tools (v65)

### What was asked
"...also revert this commit font changes" (f991db1 — toggleable serif body
text) + "add an advance animation system that automatically adjusts the
pixel and things based on the image the user imports. and remove that zoom
slider and add brush size incrase size with brush indicator. and in pen
fill etc show which color is selected alongside them. and add 3 preset to
set the size of the eyes and a eye placement adjuster showing the previe
animation and user can adjust the placement with arrows."

### What was done (font revert — code part)
1. **Reverted f991db1 exactly** (all 5 code files byte-match `f991db1^`):
   - `CurioTypography.kt` — removed `CurioLoraBodyTypography` +
     `curioAppTypography()` + unused imports; labelMedium/labelSmall
     tracking back 0.3 → 0.5sp.
   - `CurioTheme.kt` — typography back to `CurioTypography`.
   - `AppPreferences.kt` — removed KEY_LORA_BODY, `loraBodyState`, the
     initThemeMode seed, `isLoraBodyEnabled`/`setLoraBodyEnabled`.
   - `SettingsSectionScreen.kt` — removed the "Serif body text" row.
   - `ExploreSessionService.kt` — bubble typography back to
     `CurioTypography`.
2. **Docs:** app/AGENTS.md v59.3 bullet rewritten as REVERTED (v64),
   noting the v35 Lora reading serif stays; changelog Settings line drops
   "serif body toggle".

### Validation
All 5 code files byte-match the pre-revert state; `git diff --check`
clean; brace balance OK. No Gradle locally (env rule) — CI on push.

### DONE — the pet designer tools request (v65)
The user's answers to the clarifying questions: "yess all of it and also
auto save so it doesnt get lost" (import options: auto-colors + full pet
+ bob + auto-save), "Both" (both eyes targets), "both and also add color
removed like if a backgroud with certain color got added add a way to
clear it, maybe fill with transarecy" (brush indicator + fill-with-
transparency Clear tool).

1. **Auto-import** — new "Auto-import image" entry in the import menu:
   dominant colors → 4 custom palette slots → full body + curled grids →
   4-frame bob animation → immediate save (autoImportNext flag + the
   pipeline in the PNG picker callback).
2. **Zoom slider removed** from the pixel editor → `BrushSizeRow` (1–4)
   + `brushSize` state; Brush/Erase paint squares; `PixelGrid` draws a
   live on-canvas ring of the brush footprint.
3. **CLEAR tool** — fill-with-transparency flood fill (removes a solid
   background), once-per-gesture like FILL.
4. **Selected-color chip** — `ToolTray` shows a paint swatch up front
   (tappable → Brush); tray row scrolls horizontally.
5. **Eyes section** (Settings) — `EyeControls`: Small/Medium/Large
   presets (`eyeScale` 0/1/2), arrow cross pad (`eyeOffsetX/Y` −6..6),
   live bobbing sprite preview, Reset. New `PetDesign` fields serialize
   (`eyesize=`/`eyeoffx=`/`eyeoffy=`, tolerant parse) and
   `CurioPetSprite` scales each eye around its center then offsets —
   live pet + previews + saved designs all match.

### Validation
Brace balance OK (3 files); `git diff --check` clean; `validate_petdesigner.py`
fails on HEAD too (pre-existing, unrelated). No Gradle locally (env rule) — CI on push.

## Prior — release-note format + per-commit update rules (v64)

### What was asked
"Similar to Prompt.md make the release note update by yours add
instructions... similar to app notes use less words and simpler arrange
them like fix add remove, and write with all of the apps feature in that
format, just add and fix as it will be first release. And you've to update
them each commit and remove something if they got removed without ever
getting pushed then no need to add removed note."

### What was done
1. **Changelog rewritten leaner** — `fastlane/metadata/.../changelogs/
   20260919.txt` now uses short `ADD` / `FIX` bullets covering every
   shipped feature (first release → no REMOVE section). Voice = the
   app/AGENTS.md version bullets, not store copywriting.
2. **Per-commit rule added to the DOX chain**:
   - Root `AGENTS.md` → "Updating What's New" now mandates updating the
     release notes on EVERY commit that ships a user-visible change, and
     documents the ADD/FIX/REMOVE bullet format + the "REMOVE only for
     shipped features" rule (never-shipped removals get no note).
   - `fastlane/AGENTS.md` → new "Changelog Format Contract" (concise
     one-line bullets, edit the current versionCode file in place, new
     file only on versionCode bump).
3. **REMOVE discipline** — a feature removed before it ever reached a
   pushed release gets NO note; only shipped features get a REMOVE line.

### Validation
`git diff --check` clean. Text/docs only — no Gradle needed (env rule).
CI on push is the gate.

## Prior — update notice → in-app toast (v63)

### What was asked
"remove the toast for update replace it with in app toast not the android
toast"

### What was done
1. **New in-app toast bus** (`ui/components/CurioInAppToast.kt`):
   `object CurioToast` (snapshot state, `show(text, glyph)` /
   `dismiss(id)`, main-thread) + `CurioInAppToastHost` composable — a
   themed pill (curioDialogContainerColor, primary glyph, 6dp lift) that
   slides up, holds ~3.5s, fades. Generic — future background notices
   reuse it.
2. **Hosted at the NavHost root** (CurioNavHost's root Box, after the
   Scaffold): floats above every screen, cleared past the bottom nav
   (navigationBarsPadding + 96dp). State set before the UI composes is
   picked up on the host's first frame.
3. **UpdateChecker** no longer uses `android.widget.Toast` (import
   removed) — `CurioToast.show(text, glyph = CurioIcons.Download)`;
   the once-per-version notification is untouched.

### Validation
Brace/paren balance: new file 32/32; CurioNavHost 448/448 (delta +5
pairs); UpdateChecker keeps its pre-existing +2 close comment-parenthesis
imbalance with a balanced delta. `git diff --check` clean. No Gradle
locally (env rule) — CI on push is the gate.

## Prior — sort pill slimmer in Cabinet + Topic Browser (v62)

### What was asked
"and stil the sort pill in cabinet and topic browser is too wide please
reduce it please or mak eth search icon be the same size too"

### What was done
`CurioSortDropdown` (shared by both heroes; user preferred the reduce
option over growing the Search pill): label-zone padding 10/8 → 8/6,
chevron 18 → 16dp, zone gap 6 → 5dp, arrow-zone horizontal padding 6 →
4dp, direction arrow 22 → 20dp, widthIn floor 96 → 88dp. Uniform 42dp
height untouched; the pill now hugs its label ("Category" / "Default" /
"Year") next to the icon-only Search pill.

### Validation
Brace/paren balance 57/57 unchanged, `git diff --check` clean. No Gradle
locally (env rule) — CI on push is the gate.

## Prior — Spin filter sheet: chips + typography bigger (v61)

### What was asked
"make the filters in spin page the filters chips even bigger whole filter
page typography and heracry even bigger"

### What was done (all in SpinScreen.kt + one optional param)
1. **CompactChip bigger still** — 16 → 18sp label, padding 16/11 → 20/13,
   glyph 17 → 19dp, gap 7 → 8dp, inactive weight Medium → SemiBold
   (third bump: v44 15sp → v52b 16sp → v61 18sp).
2. **Whole-sheet type/hierarchy scaled up:** header 22 → 24dp glyph +
   24sp title; subtitle bodySmall → bodyMedium; `CurioSearchField` gained
   an optional `textStyle` param (default bodyLarge — Settings hub /
   Topic History unchanged) and the sheet passes 18sp; "Active filters"
   label → labelLarge; `SectionLabel` → 16sp ExtraBold (0.3sp tracking);
   `FilterGroupPill` → 17sp label, 20/14/12/12 padding, 18dp chevron,
   bigger count badge; `ActiveFilterChip` → labelLarge + roomier padding
   + 16dp close; group + chip FlowRow gaps 8 → 10dp; Apply CTA 17sp,
   14dp vertical padding, 20dp icon.

### Validation
Brace/paren balance on both files (SpinScreen 1992/1992 vs HEAD
1991/1991 — +1 balanced pair; CurioSearchField 31/31 unchanged),
`git diff --check` clean. No Gradle locally (env rule) — CI on push is
the gate.

## Prior — session-screenshot removal + mood-board crash + reveal strip polish (v60)

### What was asked
"why the sessio screenshot attahes my old screenshots like all of it,
remove that feature and its permission its too scary" — plus a crash
report (Collection.isEmpty() NPE during measure) and three UI fixes:
the small inline mood-board editor's quote looks too huge, the reveal
bottom strip's tags can still go a little above, and the Like/Dislike
active state is too vague.

### What was done
1. **Session-screenshot feature removed.** The save page's "Session
   screenshots" section (SessionAttachmentsCard + add-from-gallery +
   per-shot remove + SessionShots/SessionShots.copyFrom/delete) is gone;
   the manifest drops READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE (≤32) and
   FOREGROUND_SERVICE_MEDIA_PROJECTION (no MediaProjection code exists
   in-tree — v55 removed the watcher). The shared session NOTE survives
   (SessionNoteFloatingPill + peekWriteSessionNote unchanged). Legacy
   data paths kept inert + read-only: ExploreSession.screenshotPaths,
   pending-write shots, CaptureEntity.sessionScreenshotsJson, SessionShots,
   backup/restore, and EntryDetail's display of already-saved shots.
2. **Crash fixed (the reported NPE):** `ExpandedMoodBoardDialog` called
   `.ifEmpty{}` on `tileLayoutsFull`/`quotePositionsFull`, which Gson
   decodes to NULL for pre-v57 entries (bypasses Kotlin defaults) →
   `Collection.isEmpty()` NPE on the dialog's first measure. Now
   `orEmpty().ifEmpty{...}`; same guard on MoodBoardExport's three
   `.isNotEmpty()` sites (save/share path).
3. **Inline mood-board quote cards no longer balloon:** when the collage
   is smaller than the canvas it zooms to fill (scale > 1) and the raw
   slot width (~41% of board) multiplied by that zoom. MoodBoardFloatingCards
   now caps the DISPLAY scale of never-resized fallback cards at ~44% of
   the canvas (`displayScale`); user-resized cards keep the full scale.
4. **Reveal strip:** tags row top inset 10 → 6dp (clearer clearance);
   SentimentButton active now scales to 1.08 with a category glow
   (curioDarkGlow 4dp), ExtraBold label and 17dp icon.

### Validation
Brace/paren balance on all 5 code files (reveal keeps its pre-existing
+1 comment-parenthesis imbalance, delta exact vs HEAD), `git diff --check`
clean, no leftover refs (SessionAttachmentsCard / sessionScreenshots /
addScreenshotLauncher / SessionShots / .ifEmpty / .isNotEmpty on the
full-layout fields all gone). No Gradle locally (env rule) — CI on push
is the gate.

## Prior — toggleable serif body text + tighter label tracking (v59.3)

### What was asked
"in the drawer use less watermark icons, and in other screen use
watermark icons according to that screen."

### User decisions (ask_user)
- Drawer icon count: **3 pairs but smaller + fainter** (was 5 pairs).
- Scope: **all torn-hero screens** get purpose-matched watermark icons.

### What was done
1. **Drawer (HomeScreen.kt `HomeDrawerContent`):** 5 mirrored pairs → 3,
   sizes 44–56dp → 34–42dp, alphas 0.11–0.14 → 0.07–0.08, so the brand
   + greeting dominate. Symbols swapped to navigation-flavored
   `CurioIcons.drawerHeroSymbols()` (menu / explore / auto_awesome /
   star / diamond / bolt).
2. **Settings hub hero:** `CurioIcons.settingsHeroSymbols()` — gears,
   sliders, appearance + backup glyphs (was generic wildcard set).
3. **Topic History hero:** `CurioIcons.historyHeroSymbols()` — clocks,
   restores + a book (was the BOOKS family).
4. **Kept by design:** Home hero stays wildcard (its quest is wildcard);
   Profile + Cabinet keep their lane/category echo (deliberate Adaptive
   Hero behavior); Onboarding keeps wildcard (welcome). All new glyphs
   already used elsewhere in the app — verified in the bundled Material
   Symbols subset (no tofu).

### Validation
Brace/paren balance on all 4 files (CurioIcons 13/13 + 68/68, Home
261/261 + 865/865, SettingsHub 163/163 + 460/460, TopicHistory 136/136
+ 331/331), `git diff --check` clean, stale comments updated. Committed
locally; push pending user confirmation.

## Prior — CI compile fix: v58 save-screen refactor (v59.1)

### What was asked
"fix this" — CI failed compiling the v58 save-page refactor in
`SaveCaptureScreen.kt` + `MarginaliaFormat.kt` (unresolved
`SnapshotStateList`, ~40 unresolved take-state references, bad
`tintedTileInk` import).

### Root causes
1. `import androidx.compose.runtime.SnapshotStateList` — wrong package;
   the type lives in `androidx.compose.runtime.snapshots`. The broken
   import invalidated the `FormatBodyForCategory`/`CaptureTakeTabs`
   signatures and cascaded into the downstream noise (`sections`,
   `activeIndex`, `format` vs `CaptureFormat`, "not a @Composable
   context" inside `forEachIndexed`, etc.).
2. The two take-management dialogs (remove-take + switch-format) sat
   AFTER the screen's root `Column` (closed at old line 1092) while the
   v58-hoisted state (`sections`/`activeIndex`/`nextId`/
   `pendingRemoveIndex`/`pendingFormatSwitch` + `removeSection`/
   `applyFormat`) lives INSIDE it — Kotlin block scoping made every
   reference in the dialogs unresolved.
3. `MarginaliaFormat.kt` imported `com.curio.app.ui.theme.tintedTileInk`
   — the helper is `internal` in
   `com.curio.app.features.capture.formats.CaptureFormatComponents`.

### What was done
1. Import → `androidx.compose.runtime.snapshots.SnapshotStateList`.
2. Moved both dialogs INSIDE the root Column (before its closing brace,
   re-indented +4) so they see the take state; the Column closes after
   the second dialog, and the screen-level leave dialog
   (`showDiscardDialog`, which uses function-level state) stays at
   function level as before.
3. MarginaliaFormat import →
   `com.curio.app.features.capture.formats.tintedTileInk`.

### Validation
Brace/paren balance on both files (310/310 + 754/754, 69/69 + 167/167),
`git diff --check` clean, repo-wide grep for the wrong import paths
clean. Committed locally; push pending user confirmation.

## Prior — deck excludes only saved entries + uniform Cabinet card height + shuffle explanation (v59)

### What was asked
1. Cabinet topic cards get different heights with title length — make one
   fixed height without cutting the title.
2. Home tap-and-hold on Recents/Saved rows — user retracted: "nevermind
   ignore that".
3. "Make it so if a topic is in recents but unexplored and there is no
   saved for it, it will appear in the deck — it only goes away when it
   gets logged."
4. "Tell me actually how the randomiser shuffle works — does everything
   have equal chances or is it fixed? Give me suggestions."

### User decisions (ask_user)
- Tap-and-hold on Home rows: IGNORED (user retracted).
- Deck rule: **"Only saved entries leave"** — a topic stays dealable
  until it has a SAVED entry in the Cabinet; exploring without saving no
  longer removes it.

### What was done
1. **Deck exclusion → saved-only** (SpinScreen.kt): the old v7.80 done-set
   exclusion (explored or "Already …" topics left the deck forever) is
   gone. `deckPool` (fan/peek) and the landed `pickFrom` call now exclude
   by `savedTopicIds` only — a reactive set from a new
   `produceState<List<CurioEntry>>` over `repo.observeAll()` (the Cabinet
   flow; replaces the per-spin `repo.getAll()` + `doneIds` block).
   `pickFrom`'s `exploredIds` param renamed `savedIds`, docs updated;
   `ExploreSessionStore` import removed from Spin (no longer referenced).
   The done set + `recordExplored`'s `addDone` stay intact — they still
   drive the reveal's "Already …" marked state and Topic Database done
   markers, just not deck exclusion.
2. **Uniform Cabinet card height** (CurioTopicCard.kt): title reserves
   exactly two lines (`minLines = 2` + `maxLines = 2`, ellipsis) so every
   grid card is identical height; short titles leave a blank second line,
   long ones ellipsize instead of cutting or stretching the card.
3. **Shuffle explanation** — delivered in the reply (see below).

### How the shuffle actually works (answered in-reply)
- NOT equal chances. Pool = filtered category pool minus (a) the last 20
  spun topics (anti-repeat), (b) saved Cabinet entries. Then a WEIGHTED
  draw: tier 1 marquee = 100, tier 2 = 60, tier 3 = 20 (≈5:3:1); liked
  topics 2×, disliked 0.25×; category affinity up to 2.5× / down to
  0.25×; Films get a recency boost (2020s 1.6× → pre-1980 0.55×). The
  6-card fan is a plain `shuffled()`. Suggestions offered: a "Pure
  random" Experiments toggle, gentler tier weights, adjustable anti-repeat
  window, and a "why this topic" hint — pending user pick.

### Validation
Brace/paren balance on SpinScreen + CurioTopicCard (OK); no
`ExploreSessionStore` refs left in Spin; `minLines` proven available
(already used in FieldMindObservationScreen); `git diff --check` clean.
No Gradle locally (env rule) — CI on push is the gate.

## Prior — save page: chips + take tabs pin under the topic strip; mood pill in the strip; attach-tile ink (v58)

### What was asked
"In journal, field notes and review the attach option (+ icon and text)
isn't visible, and in journal the same with 'Record a voice note'. Make
the 'How do you want to capture this one?' text go away, and make the
voice / review / journal option float attached to the topic. Also the
universal 'How did it make you feel?' doesn't work when adding a new
take — fix it and keep it universal. And remove 'How did it make you
feel' and add a mood status option inside the topic bar at the right
side with a mood+ icon — tapping it opens the mood selector."

### User decisions (ask_user)
- Format chips + take tabs sit in a FIXED compact row under the topic
  strip (floating with the topic); keep the row slim so it doesn't
  take the whole screen.

### What was done
1. **Attach-tile ink** — `AddImageButton`, `ImageThumb` (empty state)
   and `JournalVoiceNoteRow` drew icon+label in raw `accent` on the
   16% `categoryTintFill` tile (invisible in pastel light). New
   `internal fun tintedTileInk(accent)` (CaptureFormatComponents):
   deep same-hue ink in light (`readableLightInk`), light twin in dark
   (lerp toward white); applied to every attach tile (Journal / Field
   notes / Review) and the voice-note row (IDLE mic+label, RECORDING
   mic, STOPPED play).
2. **Header hoisted to the topic** — the multi-take section state
   (`sections`/`activeIndex`/`nextId`/`pendingRemoveIndex`/
   `pendingFormatSwitch` + `snapshotActive`/`removeSection`/
   `applyFormat` + the aggregate `allReady`/`combinedData`/
   `anyTakeDraft`/`sectionDraftData` emissions) moved OUT of
   `FormatBodyForCategory` into `SaveCaptureScreen` (remember keys:
   `editingEntry?.format` + `editingEntry?.captureData ?: resumedDraftData`
   — same re-init timing as the old body-level state). The format chips
   (now ALWAYS one horizontally-scrollable row — the wide FlowRow wrap
   is gone) + the take tabs pin in a compact fixed row UNDER the topic
   strip, and the "How do you want to capture this one?" header text is
   removed. `FormatBodyForCategory` is now editor-only (active take
   under `key(current.id)`); the remove-take + switch-format confirm
   dialogs moved to the screen next to the leave dialog. The emissions
   `LaunchedEffect` also keys on `topic` so the Save CTA enables once
   the topic loads. New `CaptureTakeTabs` composable (extracted tabs +
   Add take); edit-mode header gated until the entry loads.
3. **Mood in the topic strip** — the body's universal mood row is
   gone. A capsule pill on the RIGHT side of the topic strip shows the
   active take's mood (or a "Mood +" affordance: MoodHappy glyph + Add
   "+") and toggles the shared mood selector pinned under the strip
   (`MoodChipsRow(header = null)` — new optional `header` param). The
   selector writes into the ACTIVE section (`mood` + stamp into
   `data?.withMood(m)`), so it works identically for newly added takes.

### Validation
Brace/paren balance on all 3 files (SaveCaptureScreen 314/314 + depth
0; CaptureFormatComponents + MarginaliaFormat OK); `git diff --check`
clean; no stale refs (onCanSaveChange / onDraftDataChanged / val wide /
windowWidthSizeClass / isWide / moodCapable / "How do you want to
capture this one?" all gone); `glyph` (JournalMood.glyph) +
SnapshotStateList imports added, adaptive imports removed; single `when
(current.format)` opener (the split briefly duplicated it — removed).
No Gradle locally (env rule) — CI on push is the gate.

## Prior — mood board dual layouts (inline vs full-screen) + quote pinch-to-expand (v57)

### What was asked
"The moodboard editing has become so inaccurate — where I place in
inline and then where I place in expanded view both became different in
save. How about save both inline editor view and full screen view
separately — use a different view so I can save 2 different views. And
in save and share, save both of the views. Also the quote card can be
stretched but it can't be expanded — add pinch-to-expand too, like
zoom-expand."

### User decisions (ask_user)
- Saved card shows the INLINE layout; the expanded dialog shows the
  FULL-SCREEN layout — give an option for each (save/share).
- Tiles AND quote cards are separate per view.

### What was done
1. **Data model** (`CaptureData.GalleryWall`): new `tileLayoutsFull` +
   `quotePositionsFull` (Gson default-empty — legacy entries fall back
to the inline ones in every consumer). `withImageUris` remaps both
layout lists.
2. **Editor** (`GalleryWallFormat`): second `fullTiles` state list +
   `fullQuotePositions` state list, both seeded from the saved full
data (fallback: inline). The full-screen canvas now edits `fullTiles`
and routes quote moves/resizes to `fullQuotePositions` via new
`MoodBoardCanvas` params (`quotePositionsOverride` /
`onMoveQuoteOverride` / `onResizeQuoteOverride`; null = inline board
unchanged). `QuoteCardsState` gained an `onCardRemoved` hook so the
index-aligned full-screen list trims on card delete. `canSave` counts
`fullTiles`; the save `LaunchedEffect` keys on both full lists so
full-screen edits re-emit the entry.
3. **Detail** (`EntryDetailScreen`): the expanded dialog renders
   `tileLayoutsFull`/`quotePositionsFull` (fallback inline); the small
   saved card keeps rendering the inline `tileLayouts`.
4. **Export** (`MoodBoardExport`): new `MoodBoardLayout` enum
   (INLINE/FULL) threaded through save/share → exportBoard →
   renderBoardBitmap → MoodBoardShareCard. Bitmaps preload against the
   SAME chosen layout's tiles (indices line up).
   `MoodBoardExportActions` gained an Inline view / Full-screen view
   frosted-pill picker above the Save/Share buttons.
5. **Quote pinch-to-expand** (`MoodBoardZoom.MoodBoardFloatingCard`):
   editor-only 2-finger pinch handler placed BEFORE the drag handler
   (only consumes once a 2nd finger lands, so 1-finger drags pass
   through): live-resizes the card's width like the resize grip and
   commits on release (clamped to the same min/max). The drag handler
   skips deltas while `resizing` so a pinch never slides the card.

### Validation
Brace/paren balance on all 6 files vs baselines (CaptureData's −1 is
pre-existing); `git diff --check` clean; imports verified (MoodBoardExport
import in detail, CaptureData in the format, gesture imports already in
MoodBoardZoom); export callers updated in lockstep. No Gradle locally
(env rule) — CI on push is the gate.

## Prior — topic dataset thread lifecycle (lag + device heating) (v56)

### What was asked
"The dataset thread — manage it properly, check its lifecycle —
sometimes it's lagging a lot and heating my device."

### What was done (TopicJsonLoader + MainActivity)
1. **Bounded parse concurrency:** new `Semaphore(2)` + `gated {}` helper
wraps every file read+parse (`parseAsset`, and `countFor`'s length
read). The cold-start prewarm, a wildcard merge and several screens
request lanes together; without a gate they parsed every file in
parallel and saturated all cores — lag + heat. Blocking acquires are
fine on Dispatchers.IO (pool ≫ 2); nothing nests gated sections, so no
deadlock (checked: wildcard merge holds no gate while calling `load`;
`countCanonicalTopics`→`countFor` never recurses on WILDCARD).
2. **Wildcard merge via shared `load()`:** the merge used to parse each
uncached lane DIRECTLY — double-parsing files the prewarm was already
loading. Now `load(otherId)` (in-flight dedupe + gate) — lanes in flight
are shared, never re-parsed.
3. **Per-lane count cache (`countsCache`):** `countFor` re-read +
re-parsed the whole category file on EVERY Spin deck change / picker
recompute just for a length (the hot "Mixed · N" label path). Cached
once per lane; `countCanonicalTopics` now derives from it (one parse per
file shared across both). Cleared with the pools on pressure.
4. **Tiered memory shed:** `shedForMemory(level)` replaces
`clearCache()` — RUNNING_LOW drops pools + counts (cheap single-file
rebuilds) but KEEPS the 16k-entry prebuilt index (its rebuild is the
single heaviest parse and the Topic Database re-requests it the moment
it opens; v51's full shed is what made a trim re-parse EVERYTHING — the
reported lag/heat). Index drops only at RUNNING_CRITICAL (100) /
COMPLETE (150). Generation guard unchanged.
5. **Prewarm lifecycle:** the MainActivity warmup now runs under
`withContext(NonCancellable)` — a rotation (activity destroy) mid-warmup
used to cancel `loadIndex` and restart the whole parse.

### Validation
`clearCache()` fully replaced (no references); brace/paren balance on
both files; `git diff --check` clean; deadlock/recursion traced (no
nested gate holds; count sum never recurses into WILDCARD). No Gradle
locally (env rule) — CI on push is the gate.

## Prior — remove device-screenshot auto-attach (lag on screenshot) (v55)

### What was asked
"When I take a screenshot the app starts to lag. Remove that auto attach
feature of screenshot — just keep the option in save your take, but
remove its permission or logic for the auto attach."

### What was done
- **Deleted `DeviceScreenshotWatcher`** (the MediaStore ContentObserver
that watched for new screenshots while a session / pending write
package was live, then copied each shot into app storage and appended it
— the lag: the observer fired on EVERY media-library change (even with
no session active), and each new shot triggered a MediaStore query + a
full FILE COPY at the exact moment the system was still writing and
indexing the file).
- **Removed its wiring:** MainActivity's `DeviceScreenshotWatcher.start()`
call; the reveal's `requestMediaRead` launcher and the READ_MEDIA_IMAGES
permission request in `beginExploreSession` (that permission existed
only for auto-attach).
- **Kept everything the save page needs:** the manual add-from-gallery
uses the system Photo Picker (`PickVisualMedia` — NO storage
permission) + `SessionShots.copyFrom` + `appendPendingScreenshot`, the
remove-thumbnail option, and session-screenshot backup/restore are all
touched by nothing. `ExploreSessionStore.addSessionScreenshot` stays as
an unused data-layer helper (harmless).

### Validation
No `DeviceScreenshotWatcher` / `requestMediaRead` references remain;
balance holds (reveal hunks removed −3/−3 and −4/−4, each internally
balanced; file keeps its pre-existing +1 comment imbalance; MainActivity
51/51); `git diff --check` clean. No Gradle locally (env rule) — CI on
push is the gate.

## Prior — update toast + once-per-version notification + editable Profile tagline + progress dialog tint (v54)

### What was asked
1. "Add a toast update notifier, and also update notification once the
update comes — not always."
2. "In profile, let user change the 'keep your spark going' (the streak
tagline)."
3. "In topic reveal the progress dialog — fix its background color, it's
so bad — make it take the background tint."

### What was done
1. **Update notifier** (`UpdateChecker.notifyIfUpdateAvailable`, called
from MainActivity's startup coroutine right after the catalog prewarm):
fetches the latest release (reuses `fetchLatestRelease` + `isNewer`); if
newer than `BuildConfig.VERSION_NAME` — a TOAST on every check that
finds one ("Curio {tag} is available — update in Support &
diagnostics"), and a NOTIFICATION only when the tag wasn't announced
before (`getLastNotifiedUpdateVersion` / `setLastNotifiedUpdateVersion`
in AppPreferences — the "once, not always" dedupe). Notification:
`curio_updates` channel (IMPORTANCE_DEFAULT), `ic_notification` small
icon, BigText style, launcher-intent PendingIntent (FLAG_IMMUTABLE),
`runCatching` around notify (POST_NOTIFICATIONS runtime gate on 13+ —
the toast already announced it). Silent on offline/API failure; the
manual Support & diagnostics check stays authoritative.
2. **Editable tagline**: the Profile hero tagline (streak-derived) is now
tappable → AlertDialog (background-tint container, CurioDialogShape)
with an OutlinedTextField. Save persists `custom_streak_tagline`;
empty = automatic (`taglineForStreak`); "Use automatic tagline" button
in the dialog body resets it; Cancel/back dismisses. `heroTagline`
`remember(taglineRevision, displayStreak)` re-reads the pref; wired
through ProfileHero's new `tagline` + `onEditTagline` params and
ProfileDialogs' new params.
3. **Progress editor dialog**: `containerColor` `accent` →
`curioDialogContainerColor()` (the standard page-background tint);
`dialogContentColor` default `ink` → `MaterialTheme.colorScheme.onSurface`;
the reveal's explicit `dialogContentColor = cat.onAccent()` override
removed (it existed for the accent container — with the tinted container,
accents text would be invisible). Category accent still drives the ring,
steppers and the Save button fill.

### Validation
Brace/paren balance on all 6 files vs baselines (UpdateChecker −2 and
reveal +1 are pre-existing comment imbalances — deltas exact);
`git diff --check` clean. Imports verified (androidx.core
NotificationCompat/ManagerCompat, BuildConfig/R, launch-intent pattern,
clickable/PaddingValues already present in Profile). No Gradle locally
(env rule) — CI on push is the gate.

## Prior — Apple Music item resolution + saved-progress fix + filter chips + chip-bar animation (v53)

### What was asked
1. "The Apple Music link still opens up the browser after opening Apple
   Music" (the v52 music:// search link still hit the in-app browser
   banner).
2. Topic Reveal bottom strip: tags overlap the Like/Dislike row — move
the tags a little above; give Like/Dislike a theme-aware color.
3. "The progress isn't showing in cabinet (thin line) neither in detail
view" — and place the detail progress at the right bottom corner.
4. Sort date/title chip pill shape still wrong — report its dimensions
(user will say how much to decrease).
5. Animate the categories row appearing (Cabinet + Topic Browser).
6. Filter chips: give each an icon, make them even bigger, darker chip
color in light mode.

### What was done
1. **Apple Music — resolve to a real item.** Root cause: the Android
Apple Music app only handles ITEM pages natively; BOTH https:// and
music:// search URLs render the web search page in an in-app browser
with the "Open in browser" banner. New `resolveAppleMusicItemUrl(topic)`
(ExploreSearch.kt): public iTunes Search API (entity album / song /
musicArtist by subtype, media=music, limit=1, 8s timeouts, org.json,
plain HttpURLConnection like UpdateChecker), returns
`music://music.apple.com/{cc}/album|song|artist/{id}`; the reveal's
Watch-in button for APPLE_MUSIC now launches it in a coroutine
(`revealScope`) and starts the session with the resolved URL, falling
back to the music:// search link on failure. INTERNET permission +
coroutines already present.
2. **Reveal strip**: tag chips top inset 14 → 10dp; SentimentButton
slimmed (12/6 → 10/5 padding, 16 → 15dp icon, onSurfaceVariant ink) and
inactive fill switched from `surfaceVariant` to `curioPillTintLift()`
(theme-aware tinted glass).
3. **Progress (root cause found):** `CaptureEntity.toEntry()`'s fallback
topic (built until the catalog lane cache loads) omitted
`pageCount`/`episodeCount` → `progressTarget` null → no progress UI on
saved entries. Added nullable `pageCount`/`episodeCount` to the entity,
persisted in `toEntity()`, restored in the fallback, Room v6→v7
(`MIGRATION_6_7`); backup export already round-trips them (gson.toJson
of the entity). Cabinet line was ALSO white-on-light
(`cat.onAccent()`) — now `themedAccent()` fill + always-visible faint
track. Detail pill stays anchored at the hero BottomEnd (12dp).
4. **Sort pill dims REPORTED to the user** (no code change — awaiting
their decrease number): 50dp full capsule, 42dp min height, label-zone
padding 14/10/10/10, labelLarge Bold 14sp, 18dp chevron, 22dp direction
arrow zone (8dp horizontal padding, min 40dp), 3dp elevation.
5. **Chip-bar animation**: Cabinet + Topic Browser sticky chip bars now
`AnimatedVisibility` (expandVertically + fadeIn / shrinkVertically +
fadeOut, 300/220/260/160ms, FastOutSlowIn) instead of popping. Both
bars are BoxScope extensions — they resolve through the outer Box
receiver inside the AnimatedVisibility lambda (legal).
6. **Filter chips**: `CompactChip` gained a keyword-mapped per-chip icon
(`filterChipIcon`: horror→psychology, history/era→history, music→
music_note, books→menu_book, film→movie, art→palette, sports→trophy,
science→science, food→local_cafe, origin→globe, war→flag, decade→
calendar, pets→pets, podcast→mic, default→auto_awesome — all verified
in the bundled subset); chips grew (16sp label, 16/11 padding, 17dp
icon); light-mode inactive fill darkened 0.82 → 0.5 lerp toward
`curioPillTintLift` (also on closed FilterGroupPill) so chips read as
solid mid-tones off the pale sheet.

### Validation
Brace/paren balance on all 8 edited files (reveal keeps its pre-existing
+1 comment imbalance), braces balanced, `git diff --check` clean;
imports verified (resolveAppleMusicItemUrl + curioPillTintLift in the
reveal, expandVertically/shrinkVertically + AnimatedVisibility in the
screens, HttpURLConnection/URL/JSONObject/Dispatchers/withContext in
ExploreSearch); INTERNET permission present; entity + migration + backup
round-trip consistent. No Gradle locally (env rule) — CI on push is the
gate.

## Prior — Apple Music deep link opens native search + backup-restore CI fix (v52)

### What was asked
1. CI compile failure on the v48 streaming-restore push: `Cannot infer
type for type parameter 'T'` + cascading unresolved `id` /
`format` / `formatDataJson` at CurioBackupManager.kt 347/349/355/359/365
and 623-626 (compileDebug + compileRelease).
2. "Fix the Apple Music link too — it opens the app but doesn't search
or show a result; it shows a prompt to open the browser. Do a proper
research and fix it."

### What was done
1. **CI fix:** Gson has NO `fromJson(JsonReader, Class<T>)` overload —
only `fromJson(JsonReader, Type)` — so the two un-typed
`gson.fromJson(reader, CaptureEntity::class.java)` calls couldn't infer
T and bound to `Any!` (that's the cascade). Both now declare the type
explicitly (`val capture: CaptureEntity` / `val cap: CaptureEntity`).
2. **Apple Music deep link — root cause + fix (researched):** the old
URL was `https://music.apple.com/{cc}/search?term=…` — a web-only page.
The Android Apple Music app claims music.apple.com as an App Link, so
the link opens the app, but the app has no NATIVE handler for the
/search route and renders the web page in an in-app browser with an
"Open in browser" banner — exactly what the user saw. The fix uses
Apple's registered URL scheme: `music://music.apple.com/{cc}/search?
term=…` — the app-urls reference documents that "any music.apple.com
link works by switching out https with music"; the custom scheme routes
through the app's native router (its search tab), bypassing the web page
entirely. New shared `openSearchUrl(context, url)` catches
`ActivityNotFoundException` (Apple Music not installed → custom scheme
has no handler) and falls back to the https equivalent, preserving the
old browser behavior. Both launch sites now use it:
TopicRevealScreen.openExploreBrowserAndGoHome + HomeScreen's
keep-exploring (the only two ACTION_VIEW sites for session.searchUrl;
ExploreSessionService never opens URLs). Dead `Intent`/`Uri` imports
removed from HomeScreen; Intent/Uri still used in the reveal (uninstall
intent) so its imports stay.

### Validation
Brace/paren balance on all four files (reveal matches its HEAD baseline
delta −2/−2 on the pre-existing +1 comment imbalance; ExploreSearch and
HomeScreen balanced exactly); `git diff --check` clean; no leftover
old-URL launches; music:// + openSearchUrl wired in both screens. No
Gradle locally (env rule) — CI on push is the gate.

## Prior — reveal light-mode pill tints + bigger corner chips + lag log (v51)

### What was asked
1. "In light mode I still feel the author / Watch-for / tag chips are
   whitish too much — give me a solution."
2. Make the corner category chip + pin + dismiss a little larger.
3. A log dump: constant `Background concurrent mark compact GC` lines
   (every ~1s, heap 110-160MB), `Skipped 3320 frames` + `Skipped 849
   frames` (cold start), `Suspending all threads took ~11ms`, app
   eventually killed + restarted — "the app was lagging too much, and
   constantly that runs on background".

### What was done
1. **Hero pill glass deepens** (`HeroCard.pillGlass`): pastel light
   0.80 → 0.60 toward white, non-pastel light 0.50 → 0.42 — the lane
   accent visibly tints the action / byline / subtype pills.
2. **Strip tag chips carry color:** opaque tinted fill
   `lerp(surface, themedAccent(), 0.22 → 0.32)`.
3. **Corner controls larger:** category chip padding 12/7 → 14/9 + 18dp
   glyph; pin/close circles 22dp/8dp pad → 24dp/9dp pad (42dp circle).
4. **Lag log — diagnosis + retained-footprint fix:** the log shows a
   near-full heap (66-160MB, mid-range A356E) with a full-heap
   (mark-compact) GC roughly every second and main-thread stalls — the
   app was allocating hard against a heap that stayed near-full.
   `TopicJsonLoader.clearCache()` (fired on `TRIM_MEMORY_RUNNING_LOW`)
   only dropped the per-category pools, leaving the 16k-topic prebuilt
   index + lowercased key strings (~30-60MB) resident — so a trim never
   actually relieved pressure. Now `clearCache()` also nulls
   `indexCache` + resets `canonicalTopicCount`; all three rebuild
   lazily on next use (index is one file, cached after). This is the
   retained-footprint lever; the streaming/parallel-load work from v49
   already addressed transient parse churn. (The per-frame pet idle
   recomposition remains a candidate on-screen jank source but was NOT
   touched — too risky to refactor the sprite blind.)

### Validation
Brace/paren balance matches each file's baseline (+3/+3 on the reveal's
pre-existing +1 comment imbalance; loader clean); `git diff --check`
clean. Changelog + `app/AGENTS.md` v51 bullet updated.

## Prior — Topic Reveal: Like/dislike into the strip + one editorial font (v50)

### What was asked
"In topic reveal screen: can we place the like and dislike button at the
strip and the tags below, properly placing it — moving the tags a little
higher and not increasing the height of the strip. Also the font size of
the quick fact and instructions doesn't match — match it, keeping it
smaller than the current quick fact font."

### User clarification
Not the Spin deck — the reveal's bottom band. The pair goes below the
tags (the tags are raised a touch), strip height stays fixed.

### What was done
1. **Sentiment pair into the bottom band** (TopicRevealScreen): the
   section-6.5 row that scrolled in the body below the ActionPromptCard
   is removed; Dislike/Like now sit in the fixed bottom band below the
   tag chips (bottom-aligned, `navInset + 8dp` clearance). Tag row top
   inset 24 → 14dp; `RevealBottomBarHeight` unchanged (80dp) — the strip
   never grows. `SentimentButton` slimmed to fit (12/6dp padding, 16dp
   icon, labelMedium). Browse mode still hides the pair; tags render
   independently.
2. **One editorial paragraph voice:** new file-level `RevealEditorialBody`
   (`CurioEditorialBody.copy(fontSize = 15.sp, lineHeight = 23.sp)` — a
   notch below the old 17sp fact) used by BOTH the TeaserCard quick fact
   (was 17sp `CurioEditorialBody`) and the ActionPromptCard instruction
   (was an inline 15sp copy) — matched exactly, and smaller than the old
   quick fact. `TextStyle` import added.

### Validation
Brace/paren balance matches the HEAD baseline (+8/+8 on top of the
pre-existing comment imbalance); `git diff --check` clean. Changelog +
`app/AGENTS.md` v50 bullet updated.

## Prior — topic-load speed + smooth Topic Browser wheel + Home spacing (v49)

### What was asked
1. "Topics loading is so much faster now, but sometimes it doesn't load
   when restarting and sometimes it causes lag. Tell me how to make it
   even faster."
2. "The scrolling wheel in the Topic Browser is so laggy — give me a
   solution."
3. Home: too much empty space around the pet house + Shuffle the deck
   card below the hero tear (user clarified: the gaps between the card
   and Saved, and between Saved and Recents).
4. "Fix the color of the 'View all' text and match its icon" (user
   picked: match the section-title ink).

### What was done
1. **TopicJsonLoader shares in-flight parses instead of serializing**
   (`load` + `loadIndex`): the old single global `cacheMutex` was held
   for the ENTIRE parse — the cold-start prewarm queue (`loadIndex` +
   `preloadAll`) blocked Spin's load of an unrelated lane until every
   category finished, and the Topic Browser's index load double-parsed
   the merged 16k-topic index alongside the prewarm (the restart
   stall/lag). Now: per-lane `inFlight` map + short-held `inFlightMutex`
   + `loadScope` — DIFFERENT lanes parse in parallel, the same lane's
   concurrent callers await ONE shared parse (creator-only
   compare-and-remove, shared parse survives creator cancellation);
   `loadIndex()` gained its own `indexMutex` + double-check so prewarm +
   screen share one parse. `parseAndCache()` holds the old wildcard
   merge + generation-guarded cache write.
2. **Topic Browser fallback never fatal:** a failed per-category load in
   the no-index fallback used to throw inside the `produceState`
   producer and freeze the screen on "Loading topics…" forever — now
   `mapNotNull` + `runCatching` skips just that lane.
3. **Wheel-scroll smoothness:** the scroll-persist snapshotFlow
   collected `(index, offset)` and wrote to the saveable registry on
   EVERY scroll frame (~60x/s over a 16k list) — it now persists only
   when `firstVisibleItemIndex` changes (`distinctUntilChanged`; restore
   lands at the row top), and the list `items(rows, …)` gained
   `contentType` (section vs topic) so LazyColumn recycles the right
   slot types.
4. **Home spacing:** one consistent 12dp section rhythm below the
   shuffle deck — the 20dp section-end spacers stacked with the
   unconditional pre-Saved 20dp spacer (40dp of dead space between the
   Shuffle the deck card and Saved when no session/queue is live); the
   doubled spacer is removed, all section gaps are 12dp.
5. **View all pills (Saved + Recents):** text + icon now use
   `onBackground` — the section-title ink — instead of the washed
   theme-primary mauve on the cream pill.

### Validation
Brace/paren balance + `git diff --check` clean on all 3 files (no Gradle
locally — CI on push is the gate). Changelog + `app/AGENTS.md` v49
bullet updated.

## Prior — sort pill matches the Category pill capsule + padding (micro-fix)

### What was done
`CurioSortDropdown` (shared by the Cabinet + Topic Browser heroes) now
matches `CabinetHeroActionPill` exactly: v42 18dp corners → full 50dp
capsule, label-zone padding 12/8/7/7 → 14/10/10/10, same 42dp min
height. Single-file styling fix — no version bump; committed `c47a183`.

## Prior — streaming backup RESTORE (OOM fix, mirrors the export) (v48)

### What was asked
Apply the same streaming treatment to backup restore so restoring a
huge file can't OOM either (follow-up to the v45 streaming export).

### What was done
1. **Restore now streams** (`CurioBackupManager.restore`): the old path
   read the whole file into a String, built a JSONObject tree, then
   Gson-parsed the ENTIRE payload — every media byte[] decoded and
   resident at once (the same OOM class as the old export). Restore now
   reads the file TWICE with a streaming `JsonReader`:
   - **Pass 1** (`validateBackupStream`): enforces the same pre-flight —
     envelope format/version, captures array present with
     unique/safe/well-formed records (id, format, formatDataJson
     deserializable, safe storage segment), preferences restricted to
     the known files. Nothing held, nothing written — a
     truncated/crafted file never touches live data.
   - **Pass 2**: walks the sections — captures + preferences parsed
     per-section via `gson.fromJson(reader, ...)`; each media file
     decoded + written ONE AT A TIME (audio by capture id with orphan
     keys skipped, images to their per-capture destinations via a
     uri→(captureId,index) map, session shots via the shared
     `shotIndexByPath`, pending-write shots included). Only tiny path
     maps are recorded; then the database is wiped + re-inserted in
     one transaction, prefs/species-catalog/pending-write restored as
     before. Same error messages and semantics; peak memory is one
     media file's bytes instead of the whole archive.
2. Imports: `JsonReader`/`JsonToken`/`JsonIOException`/`JsonSyntaxException`/
   `InputStreamReader`/`IOException`/`Type` added; `org.json.JSONObject`
   removed; shared `PREFERENCES_TYPE` used by both export and restore.

### Validation
No Gradle locally (env rule). Brace/paren balance + `git diff --check`
clean; symbol scan shows every external type imported (same-package types
need none). CI on push is the gate. Changelog + `app/AGENTS.md` v48
bullet updated.

## Prior — dark mode: deep high-contrast background tints + blackish picker idle cards (v47)

## Prior — progress UI: reveal dialog fix + cabinet visual-only line + detail corner (v46)

## Prior — streaming backup export (OOM fix) + picker draft persistence (v45)

## Prior — Spin filter sheet: bigger color-tinted chips + flow Type group (v44)

## Prior — mood-board editor fixes + tinted-glass styling + azure default (v42)
- Shelf ordering: **Merged + labeled sections** — one medal per chain,
  earned first by rarity, with Earned / Locked section labels.
- Badge tap: **Badge detail dialog** (name, tier, description, progress).
- Paths redesign: **Card-per-path grid** (two-column tappable cards).

### What was done
1. **CurioBadges.kt** — `MergedChainBadge` + `mergedChainBadges()`: one
   medal per chain at its best-earned tier (silhouette of its best rarity
   when locked; SECRET never shows locked). `CurioBadgeDetailDialog`:
   medal, tier chip, name, description, +XP and live progress bar.
   `CurioBadgeStrip` now renders the MERGED shelf and takes `onBadgeClick`.
2. **QuestsScreen.kt** — `PathsCard` rebuilt: card-per-path 2-col grid
   (glyph, progress bar, merged medal, tier chip); tap → `PathDetailDialog`
   (stage trail + Go chips); merged pinned strip + `MergedBadgeShelfDialog`
   (Earned / Locked sections, "· upgraded" chip when earlier rarities were
   earned). Removed dead `BadgeShelf`/`BadgeTile` grid.
3. **ProfileScreen.kt** — strip badges (earned + locked) open
   `CurioBadgeDetailDialog`; Edit/streak/level pills now OPAQUE
   (lerp toward white / rose twin on AMOLED, 2dp shadow + dark glow);
   "YOUR PROFILE" kicker labelSmall → labelMedium with wider tracking;
   pill row spacing tightened (8dp gaps).

### Validation
No Gradle locally (env rule). Brace balance + `git diff --check` clean.
CI on push is the gate. Changelog + `app/AGENTS.md` v42 bullet updated.

### What was asked
1. Topic Reveal: a small glitch when it opens — a white/creamy strip at
   the bottom (the band behind the tags) looks weird; the hero opens
   smoothly, make the whole screen even smoother.
2. Opening the Cabinet from Profile shows the old animation.
3. Lane icon colors are whitish in pastel mode.
4. Make the lanes open that lane in Cabinet (pre-filtered).

### What was done
1. **Reveal bottom band = page wash:** `bandPaper` now resolves
   `cat.categoryBackgroundWash()` (Curio) instead of
   `categorySurface(surfaceContainer)` — the old lighter strip read as a
   separate cream slab behind the tags during the open fade; the reveal
   is one continuous surface now (Material/AMOLED unchanged).
2. **Tab switches crossfade:** `CurioNavHost` tab-switch enter/pop-enter
   replaced `scaleIn(0.97f)+fadeIn` with a clean `fadeIn` — the scale
   read as the old zoom animation opening Cabinet from Profile.
3. **Lane glyph readability:** Profile's lane tiles now tint the icon
   with `category.categoryInk()` (deep same-hue twin in light/pastel)
   instead of `themedAccent()` which resolved near-white in pastel light.
4. **Lanes open the Cabinet pre-filtered:** new `PendingCabinetFilter`
   handoff in CurioRoutes (request/trigger/take, mirrors PendingEntryOpen);
   `LanesCard` tiles are clickable (`onOpenLane(CategoryId)`);
   `CabinetScreen` consumes the pending filter in a
   `LaunchedEffect(trigger)` and sets `selectedFilter` (clearing
   legacy/search state).

### Validation
No Gradle locally (env rule). Brace balance + `git diff --check` clean;
`scaleIn`/`scaleOut` imports still used (detail/pop routes). CI on push is
the gate. Changelog + `app/AGENTS.md` v40 bullet updated.

### What was asked
1. Filter page: background vs chip contrast is still bad — fix it; give
   BOTH active and inactive filter chips an elevation of 3.
2. Device-log warnings: `libc access denied finding property
   "vendor.perf.ems.egg"`, `ashmem pinning is deprecated since Android Q`,
   `Suspending all threads took 11.661ms` (repeats).
3. App sometimes freezes — especially when adding too many entries.

### What was done
1. **Contrast + elevation:** `CompactChip` / `FilterGroupPill` light-mode
   inactive fills lift 0.55 → 0.82 toward `curioPillLift()` (neutral cream
   vs the pale pastel wash — the old lift still read same-y); both states
   get `shadowElevation` + `curioDarkGlow` 2 → 3dp. Same treatment on
   `PickerPageTab` / `PickerPresetChip` (0.60 → 0.82, 3dp).
2. **Freeze (real fix):** `CaptureRepository.observeAll()` gained a decode
   cache keyed by a content signature — Room re-emits the full list on
   every insert, so a large archive re-ran Gson decoding (and fresh Gson
   allocations per `deserializeCaptureData`) for EVERY row per save; those
   GC pauses are the freeze. Only new/changed rows decode now.
3. **Device-log noise (NOT app bugs — no code change):**
   - `libc access denied finding property "vendor.perf.ems.egg"` — the
     system/vendor property (a Qualcomm perf hint) is restricted; the
     framework probes it, not Curio. Cannot be silenced from an app.
   - `ashmem pinning is deprecated since Android Q` — a deprecation log
     from Android's native memory-mapping internals (typically triggered
     by the OS/WebView). Apps can't opt out.
   - `Suspending all threads took X ms` — ART GC pause log; it fired
     because of the decode churn fixed in (2). It should become rare.

### Validation
No Gradle locally (env rule). Brace balance + `git diff --check` clean;
cache map only touched on the flow's single collection dispatcher
(flowOn(Dispatchers.Default)). CI on push is the gate. Changelog +
`app/AGENTS.md` v39 bullet updated.

### What was asked
"In intro the weird spacing between the above Curio and middle things —
make the tear even go down for proper size adjustments. Fix the pills
indicator of page — they look odd — fix the placement and spacing. Also
in reveal screen I didn't like the quick fact text typography, return it
to what it was — just the quick facts one."

### What was done
1. **Intro hero/tear deeper:** `fillMaxHeight(0.70f → 0.76f)` — the tear
   now sits just above the page pills, killing the dead band between the
   dots and the Skip/Next controls.
2. **Wordmark ↔ slide spacing:** the 6dp spacer under the pledge became
   10dp; the pager gained `top = 8.dp` (content no longer crowds the
   wordmark) and `bottom = 26.dp`.
3. **Page indicator → proper pills:** `PageDot` active = 22×8dp capsule,
   inactive = 8dp circles, same 8dp baseline, even 3dp gaps; row padding
   14 → 12dp vertical. The old 12dp box × 1.2 scale blob is gone (the
   `ui.draw.scale` import was removed; `foundation.layout.width` added).
4. **Reveal quick fact reverted:** `TeaserCard`'s fact body is back on
   `MaterialTheme.typography.bodyLarge` (was `CurioEditorialBody` Lora);
   spacer 12 → 10dp. Lora stays on the ActionPromptCard instruction and
   the onboarding subtext — only the quick fact went back.

### Validation
No Gradle locally (env rule). Brace balance + `git diff --check` clean;
imports verified (`width` added, unused `scale` removed). CI on push is
the gate. Changelog + `app/AGENTS.md` v38 bullet updated.

### What was asked
"In cabinet and topic browser what you've done — the sorting/search
buttons and the delete/cancel/select-all buttons are all out of the hero
now, I wanted them inside that. Redesign them with proper pills and
matching theme and style, and redesign the dropdown for sorting too. In
topic reveal, remove the duplicate category pill chip from inside the
hero. Give the corner category chip, pin and dismiss chip the theme-aware
treatment. Make the font of Express yourself and Start exploring bold.
Remove that useless arrow from the instruction. Change the pill background
of listen category and artist/author or related inside the hero to
something better. In wildcard, make the types filter more compact and
more universal (too many types), only in wildcard, and add more filters
per sub-type (types, genre, etc.) if some have fewer than 4."

### What was done
1. **Controls back INSIDE the heroes (Cabinet + Topic Browser):** the
   v34 below-hero controls row is removed; Sort + Search (and selection
   pills Clear/Select-all, Delete, Cancel) ride the hero's top row again
   via the `trailing` slot. `contentTop` reserves only the Category pill
   row below the banner; `CabinetControlsRowHeight` /
   `DatabaseControlsRowHeight` are deleted. Sort dropdown unchanged in
   shape (capsule 50dp pill, `curioDarkGlow`, 42dp) and menu
   (`CurioDropdownMenu` accent-tinted, accent-lit selected row) — the
   "redesign" was already the shared v30 language, applied with the
   hero ink/backdrop again.
2. **Reveal cleanup:** duplicate category eyebrow removed from the
   HeroCard; top-bar category chip + pin + close now wear
   `cat.categorySurface(...)` (theme-aware in every mode); Express
   yourself Bold → ExtraBold (Start exploring was already ExtraBold);
   ActionPromptCard's trailing arrow deleted; hero pills (action badge,
   byline, subtype) now use a proper frosted `pillGlass` (strong white
   glass on pastel-light, `curioPillLift()` on dark) instead of the
   washed `ink.copy(alpha = 0.18f)`.
3. **Wildcard filter compaction (FilterSheet):** Type caps at the top-8
   most frequent subtypes for any pool larger than 8 (wildcard's merged
   pool was a 60+ chip wall; individual categories keep their full
   list) and renders in a compact 2-column `LazyVerticalGrid` (max
   160dp, no scroll). Genres/Eras/Origins caps raised 4/4/3 → 8/6/6 so
   sparse categories show more filters.

### Validation
No Gradle locally (env rule). Brace balance + `git diff --check` clean;
`heightIn` import added; `fill`-was-undefined avoided by lerping off
`accent` in `pillGlass`. CI on push is the gate. Changelog +
`app/AGENTS.md` v37 bullet updated.

## Prior — hero/category-chip/cream-tint fixes + faster Home (v31)

### What was asked
1. "The filter chip in cabinet and topic browser is even bigger now — the
date default chip looks rounded and fat, fix it."
2. "Place the category chip below the search and sort buttons and fix the
header text going down as the category chip is taking its place."
3. "When I go back to the home screen it opens up a little late, like
it's lagging — fix that."
4. "The profile isn't following the hero follows spin lane option."
5. "The XP progress background, your lanes background, settings and
preferences, and support and diagnostics backgrounds — make all of them
get a small tint of the background color shade instead of looking cream,
same in other themes too."
6. "Gray out the sky azure hero and make it a 2 option with sky azure
hero and rose hero, and when the hero follows spin lane is active gray
out that."
7. "Change the hero follows spin lane text name to Adaptive Hero."

### User decisions (ask_user)
- Cream tint source: **tint toward the page background** — no matter the
theme or color (even when the spin-lane category option is on); applies
not just in Profile but Settings and its sub-pages too, plus other
cream-looking buttons and the dialog color — keeping text visible in
both themes and pastel.
- Sky azure: **grayed but visible, can't be picked** (the "Material ·
coming soon" pattern); the whole hero picker grays while Adaptive Hero
is on.

### What was done
1. **Sort pill slimmed** (`CurioSortDropdown`): fully-rounded 50dp
   capsule → 16dp corners, tighter horizontal padding; keeps the uniform
   42dp height.
2. **Category pill moved below the hero** (Cabinet + Topic Browser): it
   now rides its own fixed row just below the hero (page-level pill —
   on-surface ink over surface-high glass) instead of a second row inside
   the hero. Heroes returned to their original heights (Cabinet
   232→180 / compact 192→140; the settings `extraRow` slot +
   `SettingsHeroExtraRowHeight` removed) so the header text never moves
   down. The sticky chip bar sits below the pill row; Cabinet's chip-bar
   offsets now derive from a `barTop` parameter (wide-screen correct).
3. **Home lag:** `TopicJsonLoader.countCanonicalTopics()` re-parsed the
   whole ~14k-topic catalog on every return to Home (Home's Topics stat
   produceState restarts on each tab switch back) — the count is now
   cached in memory (one parse per process).
4. **Profile follows the lane:** `profileRoseAccent()` gained the same
   `heroLaneCategory()?.headerAccent()` check Home/Settings already had.
5. **Cream → small tint of the page background, every theme:**
   `CurioSettingsCard` (Profile + Settings hub + sub-pages) lerps its
   surface 30% toward `background`; ink-glass hero pills + sort pill
   lift toward the page background in light mode via a new
   `curioPillLift()` helper (dark/AMOLED keep the white lift for
   visibility); `curioDialogContainerColor()` pulls every theme's
   dialog toward the background; the settings-family sub-pages
   (Appearance/Preferences/Support/Backup/Experiments/Promo) wear the
   same rose-lean page tint as the hub/Profile
   (`heroPageBackground(lerp(background, settingsRoseAccent(), 0.10f))`)
   — the spin-lane wash still wins when Adaptive Hero is on.
6. **Hero picker:** "Sky azure hero" switch → 2-option segmented control
   (Rose hero / Sky azure hero), Sky azure grayed/unselectable with a
   one-time migration off azure, whole control grayed when Adaptive Hero
   is on.
7. **Renamed "Hero follows Spin lane" → "Adaptive Hero"** (Appearance
toggle + Settings hub deep row).

### Validation
No Gradle locally (env rule). Brace balance + `git diff --check` clean
on all edited files; CI on push is the gate. Changelog
(`fastlane/.../20260919.txt`) + `app/AGENTS.md` v31 bullet updated.

## Prior — no tool/Codebuff commit footers (agent instruction)

### What was asked
1. "Add instruction of how to download the windows version of the app."
2. "Return the PR push to build just the APK, and make the tag push to
   release both desktop and APK."

### What was done
- **README.md:** new **"Install on Windows (Desktop App)"** section under
  Quick Start — Releases-page download steps, portable zip vs `.msi`
  choice, SmartScreen "Run anyway" note (not code-signed), data location
  (`%USERPROFILE%\.curio`), and a not-yet-ported disclaimer. System
  Requirements table gained a Windows row (Windows 10/11 64-bit).
- **desktop-release.yml → tag-only:** removed the `push`/`pull_request`
  triggers (my earlier v28 addition) — PR/push CI now builds just the
  Android APK via android.yml. The workflow runs only on `v*` tags (+ manual
  dispatch): builds `Curio.exe` + `.msi` and attaches the portable zip +
  installer to the release next to the APKs from release.yml. The artifact
  upload step is now manual-dispatch-only (`if: !startsWith(ref,
  'refs/tags/')`) since tag runs attach the files to the release.
- **android.yml:** `push`/`pull_request` branches now `[main, Alpha]` so
  the PR/push APK build runs on the active branch. The desktop JVM compile
  job stays (compile gate only, no .exe).
- Docs: `.github/AGENTS.md` desktop-release + android sections updated.

### Validation
YAML re-validated (`npx yaml-lint`), `git diff --check` clean; CI on
push/tag is the gate.

### CI fix (same push)
CI on the 2d6d182 push failed compiling `:app` (the 9f5b2c7 hero-follows-
lane change had never been compiled):
- `headerAccent()` / `categoryBackgroundWash()` are extension functions in
  `ui/theme/CategoryInk.kt` — HomeScreen lacked the `headerAccent` import
  and SettingsHubScreen lacked BOTH (Cabinet/EntryDetail already had
  them). Added the imports.
- `heroLaneCategory()` called `LocalContext.current` INSIDE a `runCatching`
  lambda — a non-composable context can't contain @Composable invocations.
  Hoisted `val context = LocalContext.current` above the `runCatching`s.

## Prior — Category pill chevron (v30 follow-up)

### What was asked
"Give the Category pill a chevron that flips up/down when the chips are
open."

### What was done
`CabinetHeroActionPill` + `SettingsHeroActionPill` gained optional
`trailingGlyph` / `trailingContentDescription` (a small 18dp trailing icon
after the label, ink at 0.85). The Category pill in both the Cabinet and
Topic Browser heroes passes `KeyboardArrowDown` when closed and
`KeyboardArrowUp` when open, with matching show/hide content
-descriptions — the chips-open state reads as an accordion.

### Validation
Brace check OK (3 files), `git diff --check` clean; CI on push is the gate.

## Prior — shared hero follows the Spin lane + settings declutter (v30)

### What was asked
1. Remove the "Glow shadows" option from Appearance ("it's not good").
2. Remove the Home tint experiments: Home tint, Hero tint too, Follow my
   Spin lane (and the tint picker).
3. Add an Appearance toggle that makes the shared hero + its below
   background take the color/shade like the Cabinet — the category chosen
   on Spin applies to the whole shared hero header and its background,
   from Home to Profile, Settings, drawer, everywhere the rose/azure hero
   is shared.
4. Remove the "Entry date & mood" option — always on.
5. Merge "Floating explore bubble" + "Display over other apps" (they are
   the same): enabling without the permission asks for it; turning the
   bubble OFF shows an inline option to remove the overlay permission.

### What was done
- **New pref + helpers:** `heroFollowLaneState` (Appearance toggle),
  `heroLaneCategory()` (single Spin lane or null) and
  `heroPageBackground(default)` in SettingsHubScreen.kt; `settingsRoseAccent()`
  + `homeRoseAccent()` return `cat.headerAccent()` when the lane is active
  (Curio style only; Material/AMOLED keep their scheme roles) — the whole
  shared hero family follows automatically (Home/Profile/Settings/Cabinet-
  All/Quests/Recent/Support/drawer/…). Page backgrounds use
  `heroPageBackground()` (Home inline + Profile/Settings keep the rose-lerp
  default; the settings-family screens keep plain; Cabinet-All falls back
  to the lane wash) — zero default change until the toggle is flipped.
- **Removed:** Home tint experiments (Experiments section + picker dialog
  + state, prefs dormant), Glow shadows row (`curioDarkGlow` is now a no-op
  pass-through; the glow is retired, `darkGlowState` dormant), Entry date &
  mood row + hub deep rows (SaveCapture + Marginalia gates hardcoded to
  always-on via `run {}` / condition drop).
- **Bubble/overlay merge:** single "Floating explore bubble" toggle with
  live grant state in the subtitle; enabling without permission opens the
  system overlay page (existing ask); when OFF + permission granted, an
  inline "Remove overlay permission" row opens the system page with a
  revoke-trip flag so the return refreshes the grant without re-enabling
  the bubble. The separate "Display over other apps" row + hub deep row are
  gone; the "Explore bubble option in Explore dialog" row stays.

### Validation
No Gradle locally (env rule). Brace check clean on all edited files
(pre-existing checker artifacts unchanged), stale anchors gone
(appearance-glow / appearance-entry / pref-overlay), `git diff --check`
clean; CI on push is the gate.

## Prior — Cabinet + Topic Browser: sort pill height, uniform pills, Category pill (v30)

### What was asked
"In cabinet and topic browser the sorting pill is too thick — fix it and
keep all pills with same height. And below those 3 pills place the category
choose pill which shows the category chips same as when it shows when i tap
the search."

### User decisions (ask_user)
- Topic Browser category chips: HIDE by default, reveal via the new pill
  (or search) — matching the Cabinet.
- Category pill placement: INSIDE the hero, directly under the top pill
  row — the hero banner grows taller to make room.

### What was done
- **Uniform 42dp hero pills:** `CurioSortDropdown` min 44→42dp;
  `CabinetHeroActionPill` + `SettingsHeroActionPill` label-only pills gain
  `heightIn(min = 42.dp)` (they were 40dp vs the 22dp-glyph pills' 42dp) —
  Select / Sort / Search now read the same height in both heroes.
- **Category pill:** a second row directly under the top pill row (Tune
  glyph + active-filter label, `emphasized` when open) toggles the sticky
  category chip bar — the same chips search shows — in the Cabinet and
  the Topic Database. Heroes grow +52dp: `CabinetHeroBannerHeight` 180→232
  (compact 140→192); settings hero adds `SettingsHeroExtraRowHeight = 52.dp`
  when its new `extraRow` slot is used (only the DB passes it).
- **DB chips hidden by default:** `DatabaseStickyChipBar` renders only when
  `categoryFilterOpen || searchActive`; the DB derives all offsets from a
  new `DatabaseHeroTotalHeight`. Both screens' content top-padding shrinks
  when the chips are hidden (the 52dp chip-bar reservation only applies
  while visible).
- Docs: `app/AGENTS.md` v30 bullet.

### Validation
No Gradle locally (env rule). Source-audited imports (heightIn added to
Cabinet/Settings), braces balanced, `git diff --check` clean; CI on push is
the gate.

## Prior — pastel-mode FilterSheet chips: invisible elevation (v29 follow-up)

### What was asked
"In pastel mode the filter chips elevation isn't visible as they have the
same shade — fix it."

### Root cause
In the Spin FilterSheet (`CompactChip` in `SpinScreen.kt`), the sheet
container resolves `cat.categorySurface(surfaceContainerLow)` and the chips
resolve `cat.categorySurface(surfaceContainerHigh)` — but in LIGHT mode
`categorySurface()` IGNORES its `base` parameter and always returns
`lightSurfaceTint(accent)` (pastel: `lightAccentTint(accent, 0.28, 0.86)`).
So sheet and chips were the SAME airy pastel; the v29 "whisper" lift
(`lerp(chipSurface, White, 0.10)`) was invisible on the near-white pastel,
and the 2dp elevation read as nothing. (Dark mode was already fine: the chip
is a stronger tint blend than the sheet there + wears `curioDarkGlow`.)

### What was done
`CompactChip`'s light-mode inactive fill lift raised 0.10 → 0.32 toward
white — a real surface step so unselected chips visibly stand off the
tinted sheet in pastel AND plain light mode; dark keeps 0.04 + glow.
Docs: `app/AGENTS.md` v29 FilterSheet bullet extended.

### Validation
No Gradle locally (env rule). Braces/imports unchanged (single constant
edit); CI on push is the gate.

## Prior — desktop-release: missing Curio.exe app image + PR builds (v28)

### What was asked
Fix the desktop tag-release CI failure (`Write-Error: No Curio.exe app image
was produced under desktop/build/compose/binaries/main`) and make PR pushes
build the proper `.exe` too.

### Root cause
The workflow ran only `:desktop:packageDistributionForCurrentOS`, which is a
lifecycle task aggregating the package tasks. `packageMsi` builds its own
jpackage image INTERNALLY and consumes it — the app image folder (with
`Curio.exe`) is never left on disk, so the portable-zip step's recursive
`Curio.exe` search found nothing. Only `:desktop:createDistributable`
("Creates a final application image without creating an installer")
leaves the image under `build/compose/binaries/main`.

### What was done
- `desktop-release.yml` now runs BOTH
  `:desktop:createDistributable` and `:desktop:packageDistributionForCurrentOS`
  so the `Curio.exe` app image exists for the portable zip; the collect step
  derives the zip version from `RELEASE_VERSION` (fallback `1.0.0` for
  non-tag runs) so PR ref names can't leak into artifact names.
- Added `push` + `pull_request` triggers on `main`/`Alpha`: PR/push runs
  build the `.exe` + `.msi` and upload them as run artifacts
  (`curio-desktop-windows-*`, 7-day retention). `RELEASE_VERSION` is only
  set for tag runs (jpackage rejects non-numeric versions); release-only
  steps (body, prerelease, gh-release) are gated on
  `startsWith(github.ref, 'refs/tags/')`; concurrency switched to
  cancel-in-progress like android.yml.
- Docs: `.github/AGENTS.md` + root `AGENTS.md` desktop sections updated.

### Validation
No Gradle locally (env rule). YAML re-validated with `npx yaml-lint` +
`git diff --check`; CI on PR/push/tag is the gate.

## Current request — restore Spin BoxWithConstraints import

### What was asked
Fix CI compilation failures caused by the previous Spin lint cleanup.

### What was found
The outer Spin layout still uses `BoxWithConstraints` for `maxHeight` and `maxWidth`; only its import had been removed. This caused the unresolved reference and all cascading scope/composable errors.

### What was done
Restored the `BoxWithConstraints` import. The compact filter grid remains a plain `Box`, so the original unused-scope lint fix is preserved.

### Validation
No Gradle commands were run locally per repository instructions. Verify with CI and `git diff --check`.

## Prior — Apple Music Explore, YouTube preference, and CI lint fix

### What was asked
Fix Apple Music Explore URL handling without forcing `/us`, add plain YouTube alongside YouTube Music for music preferences with YouTube as the default, and resolve the SpinScreen CI lint failure.

### What was done
- Apple Music URL generation now derives a two-letter storefront from the device locale and uses a locale-free fallback instead of hardcoding `/us`.
- Added plain YouTube to the music-service picker and made it the default for new and unset preferences.
- Replaced the unused `BoxWithConstraints` around the fixed Spin grid with `Box` and removed its import.

### Validation
No Gradle compile/build/lint/test commands were run locally per repository instructions. Use source audits, `git diff --check`, and CI as the build gate.

## Prior — font-scale icon alignment audit

### What was asked
Fix misaligned top-corner/menu/profile icons at high system font scale, audit the app for similar icon issues, and correct all affected shared controls.

### What was done
- `ui/theme/CurioIcons.kt`: made bundled Material Symbols render at a stable dp-equivalent size by compensating their text `sp` size for `LocalDensity.current.fontScale`; all shared icons now remain centered and do not grow out of their slots when accessibility text is enlarged.
- `ui/components/CurioSortDropdown.kt`: changed fixed control heights to `heightIn(min = ...)` so text can accommodate font scaling without displacing or clipping the sort icons.

### Validation
Source audit completed across shared UI components. No Gradle compile/build/lint/test commands may be run locally; use `git diff --check` and CI as the build gate.

## Current request — app theme, filters, Cabinet controls, and desktop artifact collection

### What was asked
Update only the Android app UI under `app/` for global Rose/Azure Sky hero tinting, default-off glow shadows, larger left-aligned three-column Spin filters, and collapsible Cabinet categories shown during active search. Also fix the desktop tag-release workflow's portable app-image discovery.

### Decisions
- Hero tint is global for Home, Profile, Settings, and navigation chrome: Rose by default, Azure Sky when selected.
- Glow shadows are off by default; the existing Appearance setting remains available.
- Cabinet category chips are hidden in the normal compact state and shown while search is active, disappearing when search is dismissed.
- The workflow file is an explicit exception to the app-only scope.

### Changes in progress
- `AppPreferences.kt`: dark glow default changed to false.
- `HomeScreen.kt`: plain Home wash now follows the shared hero tint subtly.
- `SpinScreen.kt`: fixed three-column filter grid, larger filter typography, and 2dp active-chip elevation.
- `CabinetScreen.kt`: category chip bar is shown during active search.
- `.github/workflows/desktop-release.yml`: locate `Curio.exe` recursively so both `app` and `app-image` Compose output layouts work.

### Validation
No Gradle compile/build/lint/test commands may be run locally. Use source audits, `git diff --check`, and CI as the build gate.


## Prior — app-only quick-fact check: NOTHING under 30/50 words (v28)

### What was asked
"Ignore the web — just focus on the app. Re-analyse and tell me [what has
under 30 words]."

### What was done
- Dropped the web topic work entirely: the internet handcraft batch was
  reverted (uncommitted), the under-30 report file deleted. The food web
  batch (f19a973) is still committed from when the user approved it —
  offered to revert on request.
- **App audit (app/src/main/assets/topics/, 14,226 topics, 29 populated
  categories): ZERO teasers under 50 words — and none under 40/30/20.**
  The minimum teaser across the whole app is 70 words (artists, artworks,
  books, directors, discoveries, films, geology, plants all sit at exactly
  70 min); medians run 83-190. The app's quick-fact content is fully
  complete — nothing to fix.
- **Real gap found:** 7 app category files are EMPTY arrays ("[]"):
  medicine, psychology, mathematics, economics, language, engineering,
  oceans — yet all 7 are declared categories in `Category.kt` (lines
  63-69 + family lists). Those lanes ship zero topics in the app.

### Validation
Analysis-only (no code changes). Prompt.md committed per docs rule.

## Current request — handcrafted quick facts for web topics under 50 words (v28)

### What was asked
"Noooooo — I want full real HANDCRAFTED quick facts from web, not that
[Android-text sync]. Revert and do it again properly." (Earlier: "the
quick fact check — the topics with less than 50 words, do it on your own.")

### What was done
1. **Reverted the Android-teaser sync** (`2f5ab5e`) — web JSONs restored,
   `scripts/sync_web_teasers.js` + the AGENTS.md note removed. Copying
   Android text is NOT what the user wants: each web topic needs an
   ORIGINAL, handcrafted quick fact in the web's own voice.
2. **Audit:** 4,494 web topics have teasers under 50 words (of 6,497),
   across 20 remaining categories (directors 440, films 429, painters
   428, authors 424, artists 419, scientists 394, wildcard 394, books
   335, discoveries 205, albums 202, artworks 165, then the small ones:
   sports 76, manga 75, manhwa 64, games 65, anime 61, internet 61,
   mythology 60, series 60, songs 60).
3. **Batch 1 DONE — food.json (77/77):** every dish now has an original
   handcrafted 50+ word quick fact (history, technique, story), written
   fresh, no Android text. Min 60 words, 0 under 50. Committed `f19a973`
   (teaser-only diff, 2-space indent preserved).

### Plan (continuing, category by category)
Each batch is handcrafted prose (no templating), delivered via a one-off
script that writes teaser-only diffs, then the script is deleted. Batches
remain: directors, films, painters, authors, artists, scientists,
wildcard, books, discoveries, albums, artworks, sports, manga, manhwa,
games, anime, internet, mythology, series, songs. Not pushed (user's
standing instruction).

## Current request — Settings tear color in dark mode + detail hero tear flatlines (v28)

### What was asked
1. "In dark mode the tear color is different inside settings — fix it."
2. "Fix the tears in detail view: some get huge straight lines on the hero
   card; only the white tear stays consistent."

### What was done
1. **Settings hero tear now white in dark mode** (`SettingsHubScreen.kt`)
   — the under-sheet was the ONLY hero using
   `MaterialTheme.colorScheme.surface` (midnight in dark) with an
   `onSurface.copy(0.20)` rim (white-ish in dark), so the Settings tear
   read dark/gray while Home/Profile/Cabinet/History tears stayed white.
   Now matches the app-wide pattern: warm cream `0xFFFDFCF9` sheet +
   Home's black 0.20 rim, in every theme (AMOLED keeps its rose 0.45
   sheet so the seam reads through the pure-black banner).
2. **Detail hero tears never flatten** (`PaperCard.kt`) — the detail-only
   guaranteed-movement oscillation was a single 5.6π sine (~2.8 cycles),
   nearly the SAME wavelength as the main wave, so for unlucky seeds it
   reinforced the wave's flat plateaus → long straight stretches on the
   hero's torn edge (the white sheet stayed bumpy because its exposed lip
   uses its own restrained rhythm). Replaced with two phase-offset,
   incommensurate mid-frequency octaves (17π ≈ 8.5 and 23π ≈ 11.5 cycles,
   2.1dp + 1.3dp) — they can't both sit flat at the same spot, so the
   seam ALWAYS meanders on a ~35-45dp scale. Amplitudes hoisted to
   `meanderA`/`meanderB` constructor props (`density` is only in
   constructor scope). Hero clip + aligned under-sheet share the same
   `disp`, so they stay pixel-aligned.

### Validation
Brace balance OK (2 files), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v28 bullet.

## Current request — hole rings must THREAD through the hole (v28)

### What was asked
"Those rings styles, all 3 of them — I don't see them going through the
hole. All just changes the look of the hole."

### What was done
`PaperStatCard.kt` — all three ring styles were flat ellipses LARGER than
and centered on the punch hole, so they read as rings drawn AROUND the
hole. All three now share a real through-hole structure:
1. `drawHoleInterior` — shades the punched opening dark (deep pocket
   cue), so anything drawn inside reads as behind the paper.
2. BACK arc — dark, smaller-radius arc recessed inside the hole
   (coil 0.72×holeR at the hole bottom; split 0.82×holeR bottom half with
   a rim shade where the hole edge passes in front; oblique 0.72×holeR).
3. FRONT arc — bright steel riding the hole rim over the paper (coil
   1.02×holeR from 145°→395°; split 1.05×holeR top half 160°→360° with
   the split gap at 260° + glint; oblique a foreshortened 1.35×holeR
   ellipse bulging out, per-hole tilted).
4. DIVES — a darkened 26° run at each end of the front arc where the
   wire visibly sinks back into the hole, + the shared contact shadow.

### Validation
Brace balance OK (1 file), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v28 bullet.

## Current request — pet looks UP when scrolling DOWN on touch (v28)

### What was asked
"The pet isn't following my finger scroll — it just changes its eye
direction based on where I'm swiping. Even though it's above and my
scrolling is happening down, it looks up. Fix it."

### What was done
`CurioPetSprite.kt` — the touch-drag branch fed the raw finger delta into
the SAME scrollDir mapping as the mouse wheel, but on touch the finger
moves OPPOSITE to the content: swiping UP scrolls the page DOWN. So on a
phone, scrolling down made the pet look UP (following the finger, not the
scroll). The touch branch now INVERTS the finger delta
(`dy > 0 → scrollDir -1`, `dy < 0 → +1`), so the pet always looks the way
the CONTENT moves — consistent with the wheel branch (scrolling down =
look down). Doc comments updated to the content-direction definition.

### Validation
Brace balance OK (1 file), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v28 bullet.
Commit made, NOT pushed (user: "don't push anything yet, I'll say when").

## Prior — AMOLED border-removal audit + category picker rows (v28)

### What was asked
1. "In AMOLED mode the app still uses borders — do a FULL border removal
   audit, and suggest something else for a better look."
2. "The category picker screen (from Spin): the preset and Original/New
   rows have HUGE borders and the spacing between those two rows is high
   — fix it."

### Border audit — the two systematic sources, both removed from AMOLED
1. **`Modifier.categoryEdgeShine`** — drew a full-edge HAIRLINE RING on
   every card/pill/button in AMOLED + Material (white @ 0.10–0.26 alpha).
   In AMOLED the ring is now skipped (`hairlineAlpha = 0`); the TOP-LIT
   GLASS shine is strengthened (0.45→0.52 with accent / 0.22→0.30
   without) because it's now the sole edge cue. Material keeps its accent
   rim (its identity). The default Curio style was already border-free.
2. **`curioDarkOutline`** (v28 hairline) — now never draws in AMOLED
   (pure black wants no rings).

### The "better look" for AMOLED (suggested + implemented)
Raised surfaces on pure black now read as **top-lit glass + soft glow**
(no rings, no borders): the strengthened top-edge shine gives the
"light catching the top edge" cue and the v28 `curioDarkGlow` (soft
white 16% shadow) gives the lift. Intentional design borders kept:
CurioBadges coin rims + the Quests passport stamp ring (element
identity, not elevation).

### Category picker fixes (CategoryPickerScreen.kt + DeckPresets.kt)
- The "huge borders" were `categoryEdgeShine`'s AMOLED white ring on the
  preset chips + Original/New tabs + Mix button — gone with the audit.
- Row spacing tightened: preset row padding vertical 6dp → top 4 / bottom
  1; tabs row top 1 / bottom 4; chip internal vertical padding 6dp → 4dp
  (both pills) — the two rows sit together now.
- Both pills + the Mix button gained `.curioDarkGlow` so the AMOLED look
  is complete (top shine + glow, no ring).

### Validation
Brace balance OK (4 files), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v28 bullets.
Commit made, NOT pushed (user: "don't push anything yet, I'll say when").

## Prior — dark-mode elevation visibility + reveal gradient match (v28)

### What was asked
1. "In dark mode the elevation isn't visible — what can we do about that?"
2. "In LIGHT mode the Topic Reveal screen hero gradient doesn't match the
   main card (Spin) hero gradient; dark mode is perfect — fix light mode."

### User decision (ask_user)
Dark elevation: BOTH a faint light outline AND a soft light glow — "make
 the soft light glow default on, add the other as option in Appearance".
Implemented as two Appearance toggles: "Glow shadows" (default ON) and
"Card outlines" (default ON), both dark-mode-only.

### Done — dark elevation (ui/components/DarkElevation.kt, NEW)
- `Modifier.curioDarkGlow(elevation, shape)` — dark mode only: a soft
  WHITE-tinted shadow (16% alpha) so elevation reads as a gentle lift on
  near-black (black shadows are invisible there). Light mode adds nothing.
- `Modifier.curioDarkOutline(shape)` — dark mode only: a faint light
  hairline (12% white) along the surface edge.
- Prefs `darkGlowState` (default true) + `darkOutlineState` (default
  true), loaded in init; Appearance rows added after "Sky azure hero".
- Wired into the shared elevated components + main screens: CurioSettings-
  Card, CurioSearchField, CurioEntryCard, CurioCategoryCard, FilterChipLite
  (Cabinet chips), Settings/Cabinet hero action pills, CurioSortDropdown
  (both pills), CurioTopBar, PaperCard surfaces (paper card, torn slip,
  CompactPaperChip, paper toggle, color dots), Settings dialog option rows,
  Home (stat card, both recents rows, sticky top pills, session card, stop
  button, pick-a-lane), Profile (paper stat pane, lanes tiles), Topic
  Reveal (already-there button, teaser card). Glow precedes the fill in
  every chain (rule 11).

### Done — reveal gradient light-mode fix (TopicRevealScreen.kt)
- Root cause: the reveal `HeroCard` used `cat.headerAccent()` (light-mode
  factor 0.88 — a shade DARKER than the Spin ticket's `themedAccent()`)
  AND rebuilt the gradient via `cardGradient` while the ticket uses a
  different pastel-light recipe (ticket's 2nd stop IS the on-hue tint,
  cardGradient's is only 30% toward it). Dark's 0.94 factor hid the accent
  gap, so dark looked "perfect".
- Fix: the hero now mirrors the ticket EXACTLY — `themedAccent()` + the
  same pastel-light stops (`lerp(accent, Black, 0.05)` →
  `lightAccentTint(accent, 0.22, 0.80)`) in pastel light, `cardGradient`
  everywhere else. Pixel-identical morph in every theme. `headerAccent`
  import removed, `lightAccentTint` added.

### Validation
Brace balance OK (15+ files), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v28 bullets.
Commit made, NOT pushed (user: "don't push anything yet, I'll say when").

## Prior — pet eyes: scroll look up/down in a line, no circular spin (v28)

### What was asked
"What you did to the pet eyes — now they're not moving naturally, and they
should look in a line, not like always spinning the eyes whenever I scroll.
They are not looking up and down — they fully spin the eyes in a circle
every time."

### Root cause
The v27v "eye-roll on scroll" animated the eyes through a FULL 2π CIRCLE
(`rollAnim.animateTo(2π, tween(700))`) on every scroll event — wheel
scroll AND each 24dp of accumulated touch-drag travel. That's the
"spinning in a circle" the user saw on every scroll.

### Fix (`ui/pet/CurioPetSprite.kt`)
- `PetPointer` now exposes `scrollDir` (+1 down / -1 up) + `scrollTick`
  (increments per scroll event) instead of `rollTick`: wheel scrolls take
  the sign of `scrollDelta.y`; touch-drag scrolls use the finger's
  INCREMENTAL vertical travel (2dp threshold + 60ms gate so micro-jitter
  and direction flips track cleanly).
- Each sprite runs a `scrollLook` Animatable keyed on `scrollTick`: eases
  to the scroll direction (150ms), HOLDS while scroll events keep
  arriving (each tick restarts the effect), then settles back to neutral
  ~400ms after the last event. The eyes move STRAIGHT UP/DOWN along the
  scroll line (pure Y offset — x stays 0).
- The scroll look wins over the pointer aim while active, so a drag never
  mixes finger-aim with a spin; `lookCells` picks the scroll look first,
  then the aim, then neutral. Removed `rollAnim` + `cos` import.

### Validation
Brace balance OK, `git diff --check` clean. No Gradle locally (env rule) —
CI on push is the gate. Docs: app/AGENTS.md v28 bullet. Commit made, NOT
pushed (user: "don't push anything yet, I'll say when").

## Prior — dark-mode hero header text white/creamish on all screens (v28)

### What was asked
"In hero header text in all screen keep the text white or creamish in dark
mode only."

### What was done
New `CurioCategory.heroHeaderInk()` in `CategoryInk.kt`: LIGHT mode keeps
the existing pastel-aware `onAccent()` resolution exactly (deep ink on
airy pastel fills, white on deep accents); DARK mode always resolves
WHITE/creamish — `pastelFillInk(themedAccent())`, the same cream-white
blend the shared rose heroes already use (85% toward white keeps a hue
whisper, never a tinted light twin as title text). Applied to the three
category-tinted heroes that used `onAccent()` directly:
- Cabinet's active-filter banner (`CabinetHeroHeader` — `targetInk`).
- The saved-entry detail hero (`heroInk` in `EntryDetailScreen` — title,
  back button, meta pills).
- Home's hero-tint experiment title + sticky menu/profile pills
  (`questInk` in `HomeScreen`).
Home/Profile/Settings/History/Quests heroes already resolved creamish via
their `*ReadableInk` helpers — unchanged. `onAccent` import removed from
HomeScreen (now unused there); EntryDetail/Cabinet keep theirs (still
used elsewhere in those files).

### Validation
Brace balance OK (4 files), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v28 bullet.
Commit made, NOT pushed (user: "don't push anything yet, I'll say when").

## Prior — 3D steel rings (3 styles), Home tint pills, torn-edge rework, paper detail card (v27v)

### What was asked
"The hole rings aren't that — I meant spring-like rings through the holes
in a spiral look, sticking out, steel type, 3D. Research properly and ask
me again for confirmation. Also: Home tint — follow-my-Spin-lane must NOT
tint the hero; when Hero tint is on, the profile + menu pills should get
the color too. Torn paper edges: change the upper torn style/seed and the
other sides look too sharp. Expand the paper & headers experiment to the
detail page's mood/date stat card too."

### User decisions (ask_user)
- Rings: build ALL THREE styles as options with preview — pick the final
  one later.
- Home tint: all three items (follow-lane never tints hero; pills follow
  hero tint; existing toggles otherwise unchanged).
- Torn edges: new top tear style + soften sides.
- Detail page: meta card + title lines.

### Done
- **PaperStatCard.kt** — three 3D steel ring looks behind a
  `paperHoleRingStyleState` pref ("coil" | "split" | "oblique"): coil =
  spring wire through the hole (bright front arc over paper, dark back arc
  into the hole + specular); split = closed metal torus with a split gap;
  oblique = short coil segments springing diagonally out of the hole.
  Shared steel gradient + contact shadow. Experiments gained a "Ring
  style" picker row (enabled with Hole rings). Home/Profile/Detail pass
  the style through `paperStatCardFill`.
- **HomeScreen** — `heroTintOn`/`heroFill`/`questInk` hoisted to the top
  so the sticky MENU + PROFILE pills wear the hero tint when "Hero tint
  too" is on; follow-my-Spin-lane still never tints the hero.
- **PaperCard.kt** — `TornStatPaperShape` rework: new re-seeded top tear
  (soft waves + gentle raggedness, not the inverted hero seam) and softer
  sides (3.5→2.2dp amplitude, high-frequency octave faded).
- **EntryDetailScreen** — the Date · Mood · Session · Type meta card swaps
  frosted glass for the shared paper surface when "Paper stat card" is on
  (same holes/rings/torn toggles, per-entry torn seed).

### Validation
Brace balance OK (7 files), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v27v bullet.
Commit made, NOT pushed (user: "don't push anything yet, I'll say when").

## Prior — Custom pet design save + always-on + custom-pet look (v27v)

### What was asked
"When I tap save it says the design is saved, but the design doesn't
change. Fix it while keeping all animations and sides and sleep matching
the user's design. Let the user add the custom pet design — it wasn't
working — and it should work regardless of the level or the pet; it should
act as a new pet with the new animation system. Custom accessories and
antenna should be OFF; only blush, eyes and effects enabled in the custom
pet."

### Root cause
`CurioPetSprite`'s design resolution had:
`stage == BABY -> PetDesign.evolutionDesign(BABY, null)` — ANY baby-stage
pet (level < 15, i.e. most users) forced the baby default art and IGNORED
the saved custom design. "Save" persisted, but the sprite never used it.

### Fix
- **Sprite** (`ui/pet/CurioPetSprite.kt`): a saved custom design now
  ALWAYS wins, regardless of stage; stage-based evolution art only applies
  when no custom design exists. Animations, view angles and the curled
  sleep pose already flow from the winning design, so a custom pet is its
  own new pet automatically.
- **PetDesign** (`data/PetDesign.kt`): new `withCustomPetDefaults()` —
  procedural map: tail/belly/accessories/antenna OFF, effects ON (blush +
  eyes are FACE features, not procedural layers, so they stay on).
- **PetDesignerScreen**: `saveAsNewPet()` and saving-into-a-custom-slot
  both stamp the defaults; the plain Curie save keeps the working design
  exactly as edited.

### Validation
Brace balance OK (3 files), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v27v bullet.
Commit made, NOT pushed (user: "don't push anything yet, I'll say when").

## Prior — Pet eyes: stop looking after 2s, respond to touch scroll (v27v)

### What was asked
"The pet keeps looking while following the scroll and tap — it should stop
after ~2s and not keep animating when I'm not scrolling. Does it respond to
vertical scrolling? And tapping something / tap-and-holding a button didn't
make it look."

### Root cause
`PetPointer.position` was NEVER cleared — the eyes aimed at the last
pointer position forever (the "keeps staring" bug). Touch vertical
scrolling is a DRAG of `Move` events; wheel-only `Scroll` events never fire
on phones, so the eye-roll only ever worked with a mouse wheel. And the
look was keyed to the raw press/position, so it vanished with the event.

### Fix (CurioPetSprite.kt)
- `PetPointer.activityTick` bumps on EVERY pointer event; each sprite runs
  a `lookStrength` Animatable keyed on it: snap full while events arrive,
  then ease to neutral 2s after the last one (or when a held press
  releases) — the pet stops staring when idle.
- Touch-scroll detection: the tracker accumulates the vertical travel of a
  press-drag and fires `rollTick` once it clearly scrolls, gated to one
  roll per ~350ms so a fast fling rolls a few discrete times.
- The look now holds while the finger is down (tap-and-hold / button
  hold) and lingers 2s after a quick tap — taps visibly aim the eyes.

### Validation
Brace balance OK, `git diff --check` clean. No Gradle locally (env rule) —
CI on push is the gate. Docs: app/AGENTS.md v27v bullet. Commit made, NOT
pushed (user: "don't push anything yet, I'll say when").

## Prior — Home Recents rows lift + tag pills trim (v27u)

### What was asked
"In Home screen recent give the recent topics an elevation of 2 and the
pills of unexplore/resume etc an elevation of 1."

### Done
- `ExploreTopicRow` (recent topic rows: Explored / Unexplored / Resumed)
  and `RecentEntryRow` (recent saved entries — same feed, same card
  family) lift from `shadowElevation = 0.dp` to `2.dp`. Both fills are
  opaque `category.categorySurface()` (v27n-safe: no shadow bleed).
- The small Unexplored/Resumed tag pills inside `ExploreTopicRow` trim
  from `2.dp` to `1.dp` so they read as chips on the card rather than
  floating tiles (fill is the opaque `lerp(surfaceContainerLow, accent,
  0.14f)`, so the 1dp shadow still renders cleanly).

### Validation
Brace balance OK, `git diff --check` clean. No Gradle locally (env rule) —
CI on push is the gate. Docs: app/AGENTS.md v27u bullet. Commit made, NOT
pushed (user: "don't push anything yet, I'll say when").

## Prior — Home tint experiments: bg + navbar take the category tint (v27u)

### What was asked
"Make the Home screen background + its navbar take the color of the
category tint (not the hero) — an option in Experiments, plus another
option with the hero taking the color as well. And the Streak·Cabinet stat
card background takes a little of that color too — creamy, ~5% of the
category tint."

### User decisions (ask_user)
- Tint source: the category chosen on the SPIN page — as an optional
toggle. The 2 existing color options stay; they turn off + gray out when
that (follow-lane) experiment is on. Add an option to explicitly pick a
category color; when off, keep the previous (rose/cream) colors.
- Navbar: the BOTTOM nav bar.

### Design
New "Home tint" section in Experiments (all default OFF):
1. **Home tint** — Home background + bottom nav wear the category's
   `categoryBackgroundWash()` (Home was always plain before). The wash is
   published to the Scaffold-level nav chrome via the existing
   `CurioNavTint` out-of-band pattern (new `homeWash`; the bar/rail read it
   for the HOME route, animated 420ms like the others).
2. **Hero tint too** — the quest hero swaps rose for `themedAccent()` +
   `onAccent()` ink (grayed when Home tint off or follow-lane on).
3. **Follow my Spin lane** — tint follows `getLastSpinCategories` (single
   lane; mix/empty → Wildcard). ON → the manual toggles (Hero tint + Tint
   category) gray out; the lane wins.
4. **Tint category** — manual single-select chip picker (default Surprise/
   Wildcard), persisted `homeTintCategoryIdState`.
The Streak · Cabinet · Topics card takes a 5% whisper of the category
shade (`lerp(fill, accent, 0.05f)`) on both the paper fill and the glass
gradient stops — creamy, not colored.

### Files
AppPreferences (4 toggles + category pref), CurioBottomNav (homeWash +
container color), HomeScreen (bg + hero + stat 5% + wash publish),
ExperimentsScreen (section + picker dialog with CurioCategoryChip).

### CI fix (same push)
CI on the previous push failed compiling `CurioTheme.kt`: `containerColor:
Color?` (from the v27u pill change) isn't valid for
`ButtonDefaults.textButtonColors(containerColor: Color)` — non-nullable.
Fixed by defaulting to `Color.Unspecified` instead of null (call sites
either omit it or pass a computed non-null `lerp` Color, so no call-site
changes).

### Validation
Brace balance OK (4 files), `git diff --check` clean. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v27u bullet.

## Prior — CI fix: WiX path check failed on the runner's WiX 3.14

### What was asked
User pasted a desktop-release CI failure: `choco install wixtoolset` reports
"wixtoolset v3.14.1.20250415 already installed" (0/1 packages), then
`Write-Error: WiX Toolset not found at C:\Program Files (x86)\WiX Toolset
v3.11\bin after install` — exit 1.

### Root cause
The workflow hardcoded the WiX path to `...\WiX Toolset v3.11\bin`, but the
windows-latest runner image now ships WiX **3.14** at `...\WiX Toolset
v3.14\bin` — the `Test-Path` check failed.

### Research (JDK source)
Fetched `jdk.jpackage.internal.WixTool` (OpenJDK master): jpackage NEVER
reads the `WIX` env var. It first looks for tools on the PATH, then scans
`%ProgramFiles%` / `%ProgramFiles(x86)%` for dirs matching `WiX Toolset v*`
(newest first) and resolves `<dir>\bin\candle.exe` / `light.exe`. So the
workflow's WIX value was inert, and the whole hardcoded-path approach was
fragile by construction.

### Fix
`desktop-release.yml` — the WiX step now DISCOVERS the install folder
(`Get-ChildItem "C:\Program Files (x86)\WiX Toolset*" -Directory` sorted
by name descending, first hit), verifies `bin\candle.exe`, then sets
`WIX` to the installation ROOT (standard `%WIX%` convention) and adds the
`bin` dir to PATH (belt-and-braces — jpackage finds the toolset itself).
Comment updated. `.github/AGENTS.md` desktop-release bullet updated
(versioned folder, discovery, WIX=root).

### Validation
No local PowerShell/Windows (env rule) — the workflow runs on tag push /
manual dispatch. PowerShell syntax follows the file's existing pwsh style.

## Prior — cut-line rework + book rings in holes + paper card expansion (v27u)

### What was asked
1. "Title cut lines" experiment: the two lines need to be a little SHORTER,
   more natural stroke, and the 2-line placement feels wrong — research how
   they should be drawn and fix it.
2. Place rings inside the pin holes like book rings — only 3, tilted look —
   as a DIFFERENT look option with a NEW toggle in Experiments.
3. Expand the paper stat card to all stat-card screens too (all experiments
   stay behind their toggles).

### Research
Double-underline conventions: two strokes close together, NOT parallel —
slight convergence like one continuous pen motion; the lower line usually a
little longer; gentle waviness + rounded/felt ends read "hand-drawn". The
old implementation's deep quadratic sag (control y 0.64 on a 0.40–0.47
stroke) dipped INTO the second stroke — that crossing is the wrong
placement.

### Fix
- **PaperTitleLines.kt** — lines now span ~88% of the title width (was
  (len+3)·0.62em — stretched past the text), floor 1.8em / cap 11em +
  220dp. Two cubic-bezier strokes in the lower half with a steady gap,
  converging slightly toward the right (top 0.34→0.40, bottom 0.74→0.68),
  bottom line longer + offset right, felt-pen double pass, -2° tilt.
- **PaperStatCard.kt (new, shared)** — `paperStatCardFill(shape, fill,
  holesOn, ringsOn, ink)` draws the opaque paper fill + 3-hole EvenOdd
  punch (left edge, 5.5dp @ 14dp) + per-hole pressed rims OR tilted metal
  book rings (foreshortened ellipse rx=holeR+0.8dp, ry=0.78·rx, per-ring
  tilt -9°/-3°/3°, metal vertical gradient, white specular top arc,
  contact shade bottom-right). `paperStatCardColor(base)` = the shared
  cream/rose-brown paper blend (light lerp 0xFFF6EB 0.62 / dark 0x2A211C
  0.50).
- **AppPreferences** — new `paperHoleRingsState` (default false, KEY
  paper_hole_rings) + is/set; loaded in init.
- **ExperimentsScreen** — new "Hole rings" row under Stamped pin holes
  ("needs Stamped pin holes on"); Paper stat card subtitle now "Home +
  Profile".
- **HomeScreen** — stat card refactored onto `paperStatCardFill` (rings
  honored); paper color via `paperStatCardColor(heroFill)`; 10 dead drawing
  imports removed.
- **ProfileScreen** — Level · Saved · Lanes pane wears the same paper card
  when the toggle is on (holes/rings/tear follow the same toggles; tear
  seed 0x6B4E3E).
- Cut lines confirmed: they were already on ALL hero-title screens (Home,
  Profile, Cabinet, Entry Detail, Settings).

### Validation
Brace balance OK (6 files), `git diff --check` clean, no leftover
Path/drawWithCache/StrokeCap/Outline refs in HomeScreen. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v27u bullet.

## Prior — explore-dialog pills: clean glyphs, visible pill fill, spacing (v27u)

### What was asked
"The Watch in YouTube icon looks bad, same for Explore in browser — give the
two proper spacing and give the whole button a pill shape." (Also: drop the
Codebuff commit footer from now on.)

### Root cause
The two explore actions were `TextButton`s with `RoundedCornerShape(50)` but
`curioDialogActionButtonColors()` was TRANSPARENT (textButtonColors has no
container color) — so the pill shape was invisible; what rendered was just
the tiny 18dp v27s brand square (red YouTube tile / engine letter monogram)
floating next to the label, which read as a bad icon + no button at all.

### User decision (ask_user)
Clean glyph icons: no brand tiles. Globe (travel_explore) on the browser
button, the service's glyph (youtube_activity / play_circle / music_note)
on the watch button, tinted with the pill ink, on visible soft-tinted pills,
12dp apart.

### Fix
- `CurioTheme.kt` — `curioDialogActionButtonColors(containerColor: Color?
  = null)` so dialog actions can wear a visible container fill.
- `TopicRevealScreen.kt` — the two pills now use
  `curioDialogActionButtonColors(containerColor = pillFill)` with
  `pillFill = lerp(curioDialogContainerColor(), pillInk, 0.14f)` (opaque,
  v27n-safe), pillInk = `curioDialogActionColor()`; clean CurioIcon glyphs
  at 20dp tinted pillInk; spacing 8→12dp; contentPadding 14→16dp horizontal.
  `watchGlyph` stays service-aware via `watchService.brandTile().second`;
  removed the now-unused `SearchEngine` import + `BrandMonogram` import.
  BrandMonogram/brandTile helpers stay in CurioIcons.kt (doc updated).

### Validation
Brace balance OK (3 files), `git diff --check` clean, no leftover
BrandMonogram/engineTile/watchTile/SearchEngine refs. No Gradle locally
(env rule) �� CI on push is the gate. Docs: app/AGENTS.md v27u bullet.

## Prior — main-card border removal + save-page opaque fills (v27u)

### What was asked
"Leftover border cleanup: the main card still has the border. And on 'Save
your take', the topic-name card at the top and the attach photo have a
visual transparency issue — make it opaque, no transparency."

### User decisions (ask_user)
- Border scope: remove the drawn rim border from BOTH the Spin main card AND
  the Topic Reveal hero, and ALSO remove the main card's AMOLED rim light.
- Save page: topic strip fill + icon plate BOTH opaque.

### Root causes
- **Spin main card** (`SpinScreen.kt`) drew a 1.5dp gradient rim border +
  1dp bevel (`heroBorderOn` ← `AppPreferences.heroBorderState`, default
  true) plus an AMOLED `categoryEdgeShine` rim light — leftover borders from
  the v27n border-removal pass.
- **Topic Reveal hero** (`TopicRevealScreen.kt`) mirrored the same 1.5dp
  gradient rim (must change with the ticket or the shared-element morph
  would show the border popping in mid-morph).
- **SaveCapture topic strip** used `cat.tint` = accent @ 20% alpha under a
  3dp `shadowElevation` — translucent fill bleeds the shadow (v27n rule),
  the exact transparency issue the user saw; its icon plate was
  `themedAccent().copy(alpha = 0.15f)` (also translucent). The attach-photo
  add tile was already opaque (v27n lerp) — nothing more to change there.

### Fix
- Removed the gradient rim + bevel `drawBehind` blocks and the AMOLED
  `categoryEdgeShine` from the Spin main card; removed the matching rim from
  the Reveal hero. Dropped now-unused imports (drawBehind, CornerRadius,
  Stroke) in both files; the `categoryEdgeShine` import stays (4 other uses
  in SpinScreen). `heroBorderState` pref API left dormant (nothing reads it
  now — consistent with v25's dormant-pref convention).
- SaveCapture strip fill → opaque `lerp(surfaceContainerHigh, cat.accent,
  0.20f)` (preserves the tinted look, clean 3dp shadow); icon plate → opaque
  `lerp(surfaceContainerHigh, themedAccent(), 0.15f)`.

### Validation
Brace balance OK (3 files), `git diff --check` clean, no leftover
heroBorderOn/drawBehind/CornerRadius/Stroke refs in either file. No Gradle
locally (env rule) — CI on push is the gate. Docs: app/AGENTS.md v27u note.

## Prior — CI fix: jpackage "Illegal version" for the v1.0.2-beta tag

### What was asked
User pasted two identical CI failures (Windows desktop-release + Linux
release/verify) — both fail configuring `:desktop`:

```
* Illegal version for 'Dmg': '1.0.2-beta' is not a valid version.
  Correct format: 'MAJOR[.MINOR][.PATCH]'
* Illegal version for 'Msi': '1.0.2-beta' is not a valid version.
  Correct format: 'MAJOR.MINOR.BUILD'
```

### Root cause
`settings.gradle.kts` includes `:desktop`, so EVERY Gradle invocation
configures it (Android release, per-push desktop job, desktop-release).
`desktop/build.gradle.kts` feeds the tag minus `v` straight into
`nativeDistributions.packageVersion`; on the `v1.0.2-beta` tag that becomes
`1.0.2-beta`, which jpackage rejects (numeric-only). The Android
`versionName` is a plain string and tolerates the suffix — the desktop
installer metadata does not.

### Fix
- **`desktop/build.gradle.kts`** — sanitize `envDesktopVersion`: strip
  prerelease/build suffixes (`substringBefore('-')`, `substringBefore('+')`)
  so `v1.0.2-beta` → packageVersion `1.0.2` (v27u comment).
- **`.github/workflows/desktop-release.yml`** — the release body named the
  MSI from the full tag (`Curio-1.0.2-beta.msi`) but jpackage names it from
  the numeric package version (`Curio-1.0.2.msi`); the body now derives the
  same numeric `msiVersion` while the portable zip keeps the full tag (so
  prerelease artifacts stay distinguishable from later stable ones).
- **`.github/AGENTS.md`** — desktop-release contract documents the
  numeric-only packageVersion rule and the naming split (zip = full tag,
  MSI = numeric core).

### Validation
No Gradle locally (env rule) — CI on push/tag is the gate. PowerShell and
Kotlin logic hand-verified (`1.0.2-beta`.Split('-')[0] = 1.0.2; stable tags
pass through unchanged).

## Prior — CI fix: desktop build + Android app compile errors (b669f0c…5d01a18) + desktop JAR artifact (d648350)

### Round 1 — plugin collision (b669f0c)

### What was asked
User pasted a CI failure: `desktop/build.gradle.kts` line 10 fails with `Error resolving plugin [id: 'org.jetbrains.kotlin.jvm', version: '2.3.21']` → "already on the classpath with an unknown version, so compatibility cannot be checked".

### Root cause
`org.jetbrains.kotlin.plugin.compose` (declared `apply false` in the root build) pulls the Kotlin Gradle Plugin onto the shared buildscript classpath transitively, which leaves `org.jetbrains.kotlin.jvm` on the classpath with an *unknown* version. The `:desktop` module then requests `org.jetbrains.kotlin.jvm` with an explicit version (from the catalog) and the `AlreadyOnClasspathPluginResolver` can't verify compatibility. The two prior `resolutionStrategy.eachPlugin { useVersion("2.3.21") }` attempts didn't help because `useVersion` pins the *requested* version, not the *classpath* version.

### Fix
`build.gradle.kts` — added `alias(libs.plugins.kotlin.jvm) apply false` to the root `plugins {}` block, so the Kotlin JVM plugin lands on the shared classpath with a *known* version (2.3.21) and the desktop request resolves cleanly. Committed + pushed to Alpha for CI validation.

### Validation
No Gradle locally (env rule) — the `desktop` CI job on push is the gate.

### Round 2 — script compile errors (0f63f12)
After round 1 landed, CI got past plugin resolution but `desktop/build.gradle.kts` failed to compile as a Kotlin DSL script:
- `TargetFormat` unresolved → added `import org.jetbrains.compose.desktop.application.dsl.TargetFormat`.
- `compose.material3` (String accessor) deprecated → a script error in CMP 1.11+. Replaced with a direct catalog dependency: added `composeMaterial3 = "1.11.0-alpha07"` + `compose-material3` to `libs.versions.toml` (the material3 version bundled with CMP 1.11.1, decoupled from the CMP plugin version since 1.8) and `implementation(libs.compose.material3)` in the desktop module. TOML re-validated with `tomllib`.

### Round 3 — desktop Kotlin source errors (e27615d)
Once the desktop script compiled, `:desktop:compileKotlin` surfaced three independent issues (the desktop sources had never compiled — CI was blocked at plugin resolution until round 1):
1. **DesktopCatalog.kt nested comment** — Kotlin supports NESTED block comments, so the `/*` in the doc comment's `topics/*.json` opened a nested comment, swallowing the rest of the file ("Unclosed comment" at EOF). This cascaded into every `Unresolved reference 'DesktopTopic'/'DesktopCatalog'` error. Reworded to `app's \`assets/topics\` JSON schema`.
2. **DesktopPill** — `onClick` wasn't the last param, so trailing-lambda call sites (`DesktopPill("Light", active) { ... }`) bound the lambda to `enabled` (Boolean) → "No value passed for onClick". Moved `onClick` last (Compose convention).
3. **Main.kt window API** — CMP 1.6+ sealed `WindowPosition`: `WindowPosition(x, y)` → `WindowPosition.Absolute(x.dp, y.dp)`; `state.position.x/.y` are `Dp` → `.value.toInt()` under an `is WindowPosition.Absolute` cast.

### Round 4 — "other" APK build failure (kotlin.jvm not found — TRANSIENT)
A separate CI run failed at the ROOT `build.gradle.kts` with `Plugin [id: 'org.jetbrains.kotlin.jvm', version: '2.3.21', apply: false] was not found` (could not resolve the plugin marker). Verified the marker `org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.3.21` EXISTS on Maven Central, and the sibling desktop job resolved it fine in the same push — so this is a transient resolver/network blip, not a code issue. Re-run if it recurs.

### Round 6 — upload desktop JAR artifact (d648350)
User asked why the JVM job uploaded no result / whether it built the installer. Answered: the per-push `desktop` job is a compile gate with no artifact, and the installer comes from `desktop-release.yml` on `v*` tags. User chose to upload the desktop JAR per-push: added an `upload-artifact` step (`curio-desktop-jar-*`, `desktop/build/libs/*.jar`, `if-no-files-found: error`, 14-day retention) to the `desktop` job, with a comment noting it's a compile artifact (not runnable standalone — runnable bundles stay tag-only via jpackage). Updated root AGENTS.md CI line.

### Round 5 — Android app compile error (5d01a18)
With the desktop build finally compiling, the verify job advanced to the Android app and hit a pre-existing bug: `HomeScreen.kt:593` used `Size(...)` (the `androidx.compose.ui.geometry.Size` constructor, in the diary-spiral punch-hole ring drawing) without importing it. Added `import androidx.compose.ui.geometry.Size`. This had been masked the whole time because CI never got past the `:desktop` build. Only this one error was reported (debug + release), so the app should now compile.

## Prior — desktop full parity (milestone 3) + workflow hardening (v27t)

### What was asked
"Do the rest of the desktop things full parity. And workflow build properly."

### What was done
Desktop app restructured from a single-pane shell into Android-parity four-tab layout:
- **`Main.kt`** — now just the entry: window geometry, theme schemes, shared `CurioShellState`/`shell` (internal, with a persisted `screen` tab), bottom nav (Home · Spin · Cabinet · Settings). The old sidebar + mode-pill header are gone (lanes moved to a chip bar).
- **`DesktopCommon.kt`** — `DesktopPill` (with `enabled`), `ScreenHeader`, `LaneChipsRow` (36 lanes, tap → select + jump to Spin).
- **`DesktopHome.kt`** — rose hero banner (Streak · Cabinet · Topics stat chips; streak = consecutive save-days, topics total computed off the UI thread via `produceState`+`Dispatchers.Default`), lane chips, SPIN A LANE CTA, saved-count link to Cabinet.
- **`DesktopSpin.kt`** — lane chip bar + Spin/Browse pills + deck (2 peeks) + reveal + **Save to Cabinet** pill + browse list (moved from Main).
- **`DesktopCabinet.kt`** — saved discoveries from `DesktopEntryStore` (new JSON store at `~/.curio/entries.json`, reactive via Compose state): open (jumps to Spin reveal) / remove / empty state.
- **`DesktopSettings.kt`** — Appearance (Light/Dark), Data (Clear entries, Reset all preferences via `DesktopPreferences.clear()`), About.
- **Workflow hardening** — `desktop-release.yml` now compiles `:desktop:build` FIRST (fast-fail before WiX/jpackage) so code errors surface clearly.

### Validation
Braces balanced (9 files), `git diff --check` clean, no stale refs to the removed sidebar/MainPane/ModePill, YAML still parses, imports verified per file. No Gradle locally (env rule) — the `desktop` CI job gates compile on push; the release workflow exercises packaging on tag/manual dispatch.

## Prior — Windows .exe installers in CI (v27t, desktop release workflow)

### What was asked
"Build the Windows .exe installers in CI with a desktop release workflow."

### What was done
- **`desktop/build.gradle.kts`** — `packageVersion` now follows the tag: the workflow exports `RELEASE_VERSION` (tag minus `v`) and jpackage versions the installer from it, mirroring the Android `versionName` convention; local builds keep `1.0.0`.
- **`.github/workflows/desktop-release.yml` (new)** — runs on `v*` tags + manual dispatch, `windows-latest`, `contents: write`:
  - Installs WiX Toolset via chocolatey (jpackage needs it for `.msi`), exposed via `WIX` env + PATH.
  - Runs `./gradlew.bat :desktop:packageDistributionForCurrentOS` → builds the app image (contains `Curio.exe`) + the `.msi` (Dmg/Deb are non-Windows formats, skipped).
  - Hard guards: `.msi` must exist, app image must contain `Curio.exe`.
  - Zips the app image into `Curio-Windows-{version}-portable.zip` and publishes BOTH the `.msi` and the portable zip to the GitHub release with a Windows install-guide body.
  - Same `alpha/beta/rc` prerelease detection as Android; `update_release_body: false` so it never clobbers the Android release workflow's body when both run on the same tag (race-safe both directions).
- Docs: `.github/AGENTS.md` ownership + new "Desktop release workflow" contract section; root AGENTS.md desktop CI + not-yet-ported lines.

### Validation
No YAML lint locally — the workflow is only exercised on tag push / manual dispatch. Structure mirrors the battle-tested `release.yml` (same action versions, prerelease logic, contents: write).

## Prior — desktop persistence + preferences store (v27t, milestone 2)

### What was asked
"Continue the desktop port: add persistence and a preferences store so the deck and settings survive restarts."

### What was done
- **`DesktopPreferences.kt` (new)** — tiny pretty-printed JSON store at `~/.curio/prefs.json` (Gson, same serializer as the topic loader); best-effort load (corrupt file → defaults); `get/set` + `getBoolean/setBoolean` + `getInt/setInt`.
- **Main.kt — deck persistence:** `CurioShellState.selectedSlug` now seeds from prefs; `LaunchedEffect`s persist the lane, the landed topic id, and the theme as they change; on cold start the last landed topic is re-found in the lane's pool.
- **Main.kt — settings persistence:** a Light/Dark theme toggle pill (persisted) + full dark color scheme (warm near-black paper, light coral primary); all components now read `MaterialTheme.colorScheme.*` instead of hardcoded light constants (brand `Coral` stays as a translucent tint on both themes).
- **Main.kt — window geometry:** `rememberWindowState` restores saved size; saved position restored only when non-negative (off-screen guard); `saveWindowGeometry` persists size + position on close (guards `isSpecified`).
- **CI fix (separate commit `258be7b`):** the v27t comment in `libs.versions.toml` used Kotlin `//` comments — TOML rejects them ("Unexpected '/'" at line 43), breaking catalog parsing for the whole build. Converted to `#`; verified with `tomllib`. Pushed first to unblock.

### Validation
Braces balanced (Main.kt + DesktopPreferences.kt), `git diff --check` clean, no leftover hardcoded light colors outside the palette/scheme blocks, no stale `rememberWindowStateSafe` reference. No Gradle locally (env rule) — the `desktop` CI job gates on push.

## Prior — pet studio save fix + paper experiment reworks (v27t)

### What was asked
1. "Curie custom design isn't saving" — it should apply regardless of the pet and save as a new pet.
2. "Stamped pin holes" experiment: make them DIARY-SPIRAL looking (like a ring-bound diary), only 3, proper look.
3. "Title cut lines": move more to the end of the hero text, size to the hero text, a little longer, slight curve (not too much), pen-line like, slightly tilted.

### Root cause (pet save)
`saveAsNewPet()` / `selectCustomPet()` wrote the design only into the custom SLOTS (`setCustomPet`) — but the pet sprite + floating pet read ONLY the ACTIVE design (`AppPreferences.petDesignState`). So a "saved" custom pet never appeared outside the studio.

### What was done
- **PetDesignerScreen.kt:** `saveAsNewPet` + `selectCustomPet` now ALSO `setPetDesign` (persist as active). `selectPet` persists the re-tagged custom design (`setPetDesign` when custom — custom follows the species; `clearPetDesign` when the default look is picked). `deleteCustomPet` clears the active design when the deleted slot was active.
- **PaperTitleLines.kt rework:** signature now `(ink, title, fontSize)` — length scales with text length + font size (≈0.62em/char + 3 chars past the end, cap 16em/300dp, floor 5em); two quadratic-bezier pen strokes (slight sag top, slight rise second), felt-pen edge (wide faint pass under narrow dark pass, round caps), -2° hand tilt. All 5 call sites updated (Home name 36sp, EntryDetail topic name headlineMedium, Settings/Profile/Cabinet headlineSmall).
- **HomeScreen.kt diary-spiral holes:** 3 punch holes (5.5dp @ 14dp from left) with proper two-tone pressed rims — faint 1dp lip ring + white highlight arc top-left (160°→290°) + ink shadow arc bottom-right (340°→110°). `StrokeCap` import added.

### Validation
Braces balanced (7 files), `git diff --check` clean. No build locally (env rule) — CI on push.

## Prior — start the Compose Multiplatform desktop port (v27t, pushed 40e3c21)

### What was asked
"fix the app freeze when i take a screenshot during explore session. and fix cl and start the desktop port" (CL = the pet-eyes CI log the user pasted).

### What was done
1. **CI fix (pushed `805a430`)** — `CurioPetSprite` pointer tracker used wrong APIs: `awaitPointerEventScope` doesn't exist on `PointerInputScope` (removed), `PointerEventType` vs `PointerType` swapped in the when (now compares `event.type` to `PointerEventType.*`), `isSpecified` doesn't exist on `Offset` (null checks instead).
2. **Screenshot freeze (pushed `3a52267`)** — `DeviceScreenshotWatcher.scan()` ran on the MAIN thread (ContentObserver with main Looper) and did a MediaStore query + full file copy (`SessionShots.copyFrom`) right as the system was still writing the screenshot → the app froze during explore sessions. Fixed: the observer now spawns a `Dispatchers.IO` coroutine for query+copy; the main thread only toggles a `needsScan` flag.
3. **Desktop port started (this commit)** — new `:desktop` module (Compose Multiplatform 1.11.1, JVM 17, `org.jetbrains.compose` plugin already added to the catalog):
   - `desktop/build.gradle.kts` — CMP desktop application (mainClass `com.curio.desktop.MainKt`, jpackage `nativeDistributions` for Msi/Dmg/Deb); resources point at `../app/src/main/assets/topics` so the desktop build reads the SAME topic JSON files (no asset duplication).
   - `DesktopCatalog.kt` — desktop mirror of the topic schema (nullable `byline`/`tier` + `safe*` accessors — Gson bypasses Kotlin default-arg constructors, verified 6675 missing bylines / 591 missing tiers in the real files) + the 36-lane table + wildcard merge.
   - `Main.kt` — window shell: brand sidebar (all 36 lanes with live counts), Spin/Browse mode pills, the spin deck (front ticket + 2 peek cards — kept 2 per the standing rule), reveal card (teaser, byline pill, explore action), browse list. Palette mirrors CurioColors (SoftCream/SoftSand/CreamWhite/CoralBlush/CoralInk/Butter).
   - `settings.gradle.kts` — `include(":desktop")`.
   - CI — new `desktop` job in `.github/workflows/android.yml` runs `:desktop:build` on every push so the port can't rot (native packaging deferred to a future release workflow).
   - Docs — root AGENTS.md gained a Desktop App (desktop/) section + child index entry.

### Not yet ported (milestone 2+)
Room data layer, preferences store, capture/sessions, notifications, floating overlay — each needs a desktop stub. UI parity with Android (incl. the tablet-layout pass + web parity effort) is the ongoing goal; the shuffle deck stays 2 peek cards.

### Validation
Brace balance clean on both new files; full code re-read for API correctness (desktop CMP APIs only: `rememberWindowState(width,height)`, `lightColorScheme`, `CardDefaults.cardElevation`, `Modifier.offset(x,y)`); no Gradle locally (env rule) — the new `desktop` CI job is the gate. 

## Prior this-session (pushed before the desktop port)
- Preset toggle-undo in the category picker + removed the Everything preset (wildcard covers it) — pushed `61c4396`.
- Tablet/landscape layout pass: shell cap 720→880dp (inner 640→800dp), Settings two-pane master-detail on wide windows (full nav list left + page content right), spin deck wide-fit cap 1.0→1.6 (deck + 2 peeks scale up) — pushed `e7b5600`.
- Pet eyes follow the pointer (hover/press/wheel, saturates ~200dp) — pushed `75216ef` (compile-fixed in `805a430`).
- Music-service setting (YouTube Music/Apple Music/Spotify) + engine brand monogram tiles on the Explore pill + "Watch in" pill + avoid-AI pledge copy — pushed `a17739e` (web parity included).

## Older — elevation/blurry-background saga (fe3da7a origin)

## v27r — readability/saturation audit, hero sort+search, section headers, browser UX (fifth request)

### What was asked
1. The new solid category-accent fills can cause text-readability issues and look too saturated sometimes → full app audit + fixes. **User decision: category chips/cards keep solid; only the FIXED-COLOR paper toolbar controls (amber/brown) get a moderated tint fill + readable ink.**
2. Cabinet sort arrow + search button: bigger + a good color; same in the Topic Browser.
3. Section headers in lists of both cabinet and browser: place the category a little higher.
4. Topic Browser: auto-scroll to top when switching category; the back-to-top arrow is too big — shrink it.
5. **CI compile failures from e34a79e (user pasted the log):** `OpenNotebookFormat` unresolved `tint` (5 sub-format calls), `TopicDatabaseScreen` unresolved `themedAccent` (missing import). User asked to push the fix.

### What was done
- **CI fixes (pushed):** re-added `tint: Color` to `OpenNotebookFormat` + caller (the sub-format composables still need it); added `themedAccent` import to TopicDatabaseScreen.
- **Readability/saturation audit:** verified every v27q solid-accent fill's on-accent ink (category chip, Cabinet pills, database chips, Spin chips + deck controls, format chips, sentiment buttons, notebook rows, mood chips, picker tabs/presets, pet designer, dialog rows, onboarding — all pastel-aware ✓). Found the real bugs in the FIXED-COLOR paper controls and fixed them: `FormatToolButton` (paper mode) + `CompactPaperChip` + `NotePaperColorToggle` now use a moderated tint `lerp(surfaceContainerHighest, accent, 0.45f)` with the accent as glyph/label ink (was solid accent + pastelFillInk → white-on-amber in dark paper mode, dark-on-dark in pastel light). Threaded a `paper` flag through FormatToolbar/SelectionFormatBar/SizePickerButton.
- **Sort arrow + search (shared components, Cabinet + Topic Browser):** `CurioSortDropdown` arrow 18→22dp glyph (dropped the conflicting 20dp box), chevron full-opacity ink + 16dp, pill fills deepened (emphasized 0.45→0.35, plain 0.70→0.55); `SettingsHeroActionPill` + `CabinetHeroActionPill` glyph 18→20dp, fills 0.45/0.70→0.35/0.55 (destructive 0.35→0.30).
- **Section headers higher:** Android `DatabaseSectionHeader` top padding 14→6dp; web TopicBrowser section header `pt-4`→`pt-2`; web Cabinet group gap `space-y-6`→`space-y-4` (the Android Cabinet has no section headers — flat grid).
- **Topic Browser UX:** `LaunchedEffect(sortMode, effectiveCat)` now scrolls to top on category switch too (was sort-only); back-to-top arrow shrunk 20dp/11dp/6dp-shadow → 16dp/7dp/2dp-shadow.

### Validation
Braces balanced (10 files), `git diff --check` clean, unused `size` import removed from CurioSortDropdown, `pastelFillInk` import removed from PaperCard (no longer used), `lerp` import verified in RichTextEditor. Docs: `app/AGENTS.md` v27r fixed-color rule. Committed + PUSHED (user asked to push the CI fix).

## v27r — explore dialog buttons, badge borders, passport check (fourth request)

### What was asked
1. "Explore in browser" / "Explore in YouTube" text gets cut in the explore dialog → resize or swap in a browser icon (for the user's chosen default engine) + a YouTube icon, and give the buttons a pill shape.
2. Badges look bad / "weirdly getting cut" → **return the border**. (The category-pill elevation part of the earlier message was explicitly ignored.)
3. Check the quest passport stamps are alright.

### What was done
- **Explore dialog (TopicRevealScreen)** — the two `TextButton`s are now PILL-shaped (`RoundedCornerShape(50)`) with a leading icon + short label so nothing truncates: `travel_explore` globe + "Explore" (opens the user's chosen engine via `buildEngineSearchUrl`) and `youtube_activity` rounded play tile + "YouTube" (opens `buildYouTubeSearchUrl`). Icons tinted with `curioDialogActionColor()`, 18dp + 6dp gap. Both glyphs verified present in `material_symbols_outlined.ttf`; new `CurioIcons.TravelExplore` / `CurioIcons.YouTubeActivity` constants added (no brand glyphs exist in the Material Symbols font, so the generic globe/play-tile stands in for the engine / YouTube — the Settings row already names the chosen engine).
- **Badges (CurioBadges.kt)** — restored the pre-fe3da7a ring borders and REMOVED all shadows (the outer coin shadow clipped at the shelf edges — the "weirdly getting cut"): inner glyph plate ring (1.5dp white@0.55 unlocked / 1dp outlineVariant@0.5 locked), ribbon gem rim (1dp white@0.85), earned-marker rim (1.5dp white), locked-silhouette rim (1dp outlineVariant@0.7), and the "+N" tile's sage ring (`BorderStroke(1.dp, sage@0.28)`). v27n's opaque fills kept. `androidx.compose.ui.draw.shadow` import removed (no shadows left).
- **Passport (QuestsScreen PassportStamp)** — checked and verified structurally sound (opaque fills + flat 2dp); restored the stamp's 1dp ring border (accent ring for UNSEEN, neutral outline otherwise) so stamps read like stamps again, keeping the flat 2dp elevation.

### Validation
Braces balanced, `git diff --check` clean, no leftover `shadow` refs in CurioBadges.kt, glyph names confirmed in the icon font, imports verified (BorderStroke added to CurioBadges + QuestsScreen). Docs: `app/AGENTS.md` v27r note. No build locally — CI on push.

### What was asked
After commit `fe3da7a` ("elevation over borders app-wide") many buttons/cards show **blurry + broken backgrounds**; the user listed: badges next to your name in Profile, the Profile Level·Saved·Lanes pane, the Lanes card, a "C avatar in cabinet", the category chips, and the Spin deck peek cards (which show a "boxy thing" while animating). User instruction: identify + confirm the issue, ask, then fix.

### Root cause (two Compose shadow rules being violated)

**A. `Modifier.shadow()` placed AFTER `.background()` in a modifier chain** — the shadow modifier is INNER, so its `drawBehind` runs after the background draws → the dark blurred shadow is painted **ON TOP of the fill**. Correct order is `shadow → clip → background` (shadow behind, fill covers the inner blur).
- `CurioBadges.kt` — 4 spots: locked silhouette (L350), inner glyph plate (L359), ribbon gem (L397), earned marker (L408) — all `.clip(CircleShape).background(...).shadow(2.dp, CircleShape)`.
- `ProfileScreen.kt` L657 — profile avatar `.clip(CircleShape).background(fill).shadow(2.dp, CircleShape)`.
- `SpinScreen.kt` PeekCard — `Surface(color = Transparent, shadowElevation = 2.dp)` with the fill applied via a `.background()` on the Surface's own modifier param (OUTSIDE the surface internals) → fill draws first, then the Surface shadow paints on top of the card. Also **v24 explicitly REJECTED deck-card shadows** ("weird look while the cards animate", `shadowsOn = false` hardcoded) — the 2dp Surface shadow re-introduces exactly that (the "boxy thing").

**B. `shadowElevation` on a Surface whose fill is TRANSLUCENT (alpha < 1)** — the shadow is drawn behind the shape and shows **THROUGH the translucent fill** → muddy/broken interior. Elevation only renders cleanly on opaque fills.
- `ProfileScreen.kt` L~730 — Level·Saved·Lanes pane: Surface `color = Transparent`, `shadowElevation = 3.dp`, inner Box wears a 12–55% alpha gradient → shadow visible through it.
- `ProfileScreen.kt` L~1139 — LanesCard lane tiles: `color = accent.copy(alpha = 0.14f)`, `shadowElevation = 2.dp`.
- `CabinetScreen.kt` L~1074 — CabinetHeroActionPill (Search/Sort/Select/Cancel glass pills): `color = ink@30–65%`, `shadowElevation = 3.dp`. (commit ae83130 had deliberately added the border for these to read on the rose banner; the elevation commit removed it.)
- `CurioBadges.kt` L~471 — "+N" tile: `color = sage@13%`, `shadowElevation = 2.dp`.
- `CurioCategoryChip.kt` — `FilterChip` elevation; selected container is `category.tint` = accent **@20% alpha** → the chip shadow shows through the translucent selected fill.

### Verified NOT broken (no change needed)
- `CurioSettingsCard` (opaque `surfaceContainerLow` fill), `CurioEntryCard` (opaque fill), `CurioCategoryCard` (fill is inside Surface content → covers shadow), `FilterChipLite` (same), `CurioSearchField` (opaque).

### Proposed fix (awaiting user approval)
1. Reorder shadow modifiers to be BEHIND the fill where the fill is opaque (profile avatar).
2. Remove the blurry inner `.shadow()`s in the badges and restore the crisp ring borders they replaced (the coin design) — or one clean outer shadow behind the whole medal.
3. Remove `shadowElevation` from translucent surfaces (stat pane, lane tiles, cabinet glass pills, "+N" tile, selected chips) and restore the hairline borders they had before fe3da7a (ae83130's legibility intent).
4. Deck peek cards: `shadowElevation` back to `0.dp` (Surface stays flat per its own comment; layered shadow stays off per v24 closeout) → kills the boxy animation artifact.

### User decisions (ask_user)
1. **"C avatar in cabinet"** = the round **profile avatar** (initial letter) — they misremembered its location; no avatar exists in CabinetScreen.
2. **Strategy:** keep shadows everywhere, **make translucent fills opaque** (not full revert, not border-restore).

### What was done — first pass (user's listed items)
Per the confirmed strategy — elevation stays, fills become opaque, misplaced shadows reordered, deck shadow removed:
- **CurioBadges.kt** — ONE clean shadow on the outer coin Box (behind the opaque metal); locked silhouette + gem + earned marker shadows moved BEFORE their clip+fill (was: smear on top); glyph-plate shadow removed (it had no fill — pure blur over the metal); locked fills made opaque (secret keeps a darker blend); "+N" tile fill → opaque `lerp(surfaceContainerLow, sage, 0.13f)`.
- **ProfileScreen.kt** — avatar: `.shadow(2.dp, CircleShape)` moved before `.clip().background(fill)`; Level·Saved·Lanes pane gradient made opaque (12–55% alpha → opaque lerps resolving to the same tints); LanesCard tiles → opaque `lerp(surfaceContainerLow, accent, 0.14f)`.
- **CurioCategoryChip.kt** — selected fill `category.tint` (accent @ 20% alpha, bled the chip shadow) → opaque `lerp(surface, accent, 0.20f)`.
- **SpinScreen.kt PeekCard** — `shadowElevation` 2dp → 0dp: the elevation commit re-added the v24-REJECTED deck shadow ("weird look while cards animate") — that is the boxy thing during the reel; Surface stays flat per its own contract.

### What was done — app-wide extension pass (second request, 24 files)
User asked to extend the same opaque-fill fix app-wide (hero glass pills, Quests, Reveal, Entry Detail, coming-soon tiles, the small explored/unexplored pills in Home/Recent, and everything else the sweep found). Two rules applied consistently:
1. **Opaque-lerp fill** (`lerp(backdrop, color, oldAlpha)`) wherever the element sits on a flat surface — kills the shadow bleed with a pixel-similar tint.
2. **`shadowElevation = 0`** on true frosted glass over heroes/imagery — glass can't hold a shadow (it bleeds through); the frost defines it.
3. **Reorder `.shadow()` before `.clip()/.background()`** for the order-bug chains (shadow was painting on top of fills).

Fixed: hero pills — CabinetHeroActionPill (opaque `lerp(ink, bannerFill, 1-alpha)`, banner fill threaded through the hero trailing slot + all 7 call sites), SettingsHeroActionPill (`backdropOverride ?: settingsRoseAccent()`), CurioSortDropdown (required `backdrop`, both call sites updated). Chips/pills/tiles: SaveCapture tag chips + add-gallery tile, SoundBite record card + mic tile, CaptureFormatComponents add-quote, Home explored-tag pill + currently-exploring stop button, Recent tag pill, Quests +XP pill + quest stage cards + passport stamps + dim tile (also reordered), TopicReveal tag chips + disabled action, EntryDetail #tags + transcribing note card + voice button + structured rows (opaque) + hero session pill + frosted button + frosted hero pane (shadow 0), GalleryWall inline mood board (opaque accent-lerp; full-screen keeps translucent tint, has no shadow), CategoryPicker + DeckPresets option rows, Onboarding option pills, PetDesigner palette slots + editor target + prompt + 4 swatch/canvas order bugs, PromoStatusCard + promo chips (opaque rose-lerp + reorder), Support update card, Settings/RecycleBin dialog rows (opaque selected, transparent rows get NO shadow), RichTextEditor + PaperCard toolbar chips (opaque active) + color-dot order bugs, CurioCategoryCard coming-soon tiles (opaque).

Left translucent by design (negligible or intentional frosted glass over imagery): ≥90% alpha fills (GalleryWall 780/923, EntryDetail 1071/3100/3122, MoodBoardZoom 634), OpenNotebook radio (selected opaque / unselected no shadow), Spin 3D button (gradient is content, covers the shadow), Home stats (opaque when shadowed), Profile/Reveal 18% glass pills at 0dp.

### What was done — v27q selection-flatten pass (third request)
User: "keep selected 2 and else 2 too in elevation, and use select highlight color change for selected" → confirmed **all selected-raise elements app-wide**, selected state reads as a **SOLID accent fill**. Executed:
- **Elevation flattened to a flat 2.dp in BOTH states** for every selectable chip/card/row/tile: category chip, topic card (was 8/3), category card (8/3), spin compact chips + deck control pills (6/3), database filter chips (4/2), Cabinet FilterChipLite (was `elevation.coerceAtLeast(3.dp)`), picker page tabs + preset chips, settings hour chips (3/1), onboarding theme/search chips (4/2), PetDesigner (armed slot, used swatch, color swatches, frame thumbs, picker cards, palette rows, palette dots, custom-pet card, library card), Reveal sentiment button, EntryDetail/SaveCapture section chips, mood chips, notebook choice rows, RecycleBin/Settings dialog rows (3/0), RichTextEditor + PaperCard toolbar chips (3/1), PaperCard color swatches.
- **Solid accent selected fills** (content flips to on-accent ink): category chip `themedAccent()`+`onAccent()`; picker/preset `primary`+`onPrimary`; dialog rows `curioDialogActionColor()` with `dialogRowSelectedInk()`/`recycleRowSelectedInk()` (white, black in AMOLED) + unselected rows now opaque `surfaceContainerHigh` so the flat 2dp shadow is clean; database + Cabinet pills + notebook rows `accent`+`pastelFillInk(accent)` (Cabinet category accent switched `categoryInk()`→`themedAccent()`, ink→`onAccent()`); editor/paper toolbar `accent`+`pastelFillInk(accent)`; SaveCapture wash-ON fill → opaque `lerp(surface, accent, 0.20f)`; PetDesigner armed slot `primary`+`onPrimary`, swatch selection via contrast-aware check (no shadow).
- **Existing non-elevation cues kept**: topic-card check badge, category-card solid gradient, pet "Your pet" pill, paper swatch check, palette dot size raise (38 vs 32dp).
- **Deliberately left**: non-selection toggles (3D button, paper-stats toggle, field-border, fullscreen capture) keep their own elevation; fan-deck per-card depth shadows are deck order; OpenNotebook's tiny radio dot keeps its 2dp/0dp (opaque fill, anti-smudge).

### Validation
- Braces balanced (full-repo check), `git diff --check` clean, every `lerp(` import verified present (stale imports removed from CurioCategoryChip/SettingsSharedComponents/RecycleBin/CategoryPicker), all `backdrop` call sites wired (Cabinet 25 uses, SortDropdown 3, SettingsHeroActionPill default), new `pastelFillInk`/`onAccent`/`themedAccent` imports verified used.
- No compile/build possible locally (no SDK) — CI on push is the gate.
- DOX pass: root `AGENTS.md` compile-safety rule 11 (shadow order + opaque fills + no deck elevation); `app/AGENTS.md` v27n note updated for the app-wide scope + v27q flat-2dp no-selection-raise rule.

## Previous request (v26 — Topic Browser header rebuild + back-to-top arrow)


## Previous request (v26 — Topic Browser header rebuild + back-to-top arrow)

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
- Judgment call flagged: **Display over other apps** (the overlay-permission row) moved to Preferences with the bubble ��� it wasn't in the options list, but its grant/decline machinery (launcher + ON_RESUME observer) is inseparable from the bubble toggle, so splitting them would duplicate complex logic.

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

---

## v27 — Experimental paper headers (UNCOMMITTED, not pushed per user)

Request: header "cut look" (2 tilted corner strokes + 3 small lines top-right), diary-style stamped pin holes on the left edge (NOT spiral rings — user clarified), and paper-style soft rose/creamy cards for the Home Streak · Cabinet · Topics bar instead of the frosted/translucent pane. All behind experiments, OFF by default.

Shipped in working tree (6 files + 1 new):
- AppPreferences: 3 flags (paperHeaderCutsState, paperHeaderHolesState, paperStatCardsState), default false, with is/set + init.
- New ui/components/PaperHeaderAccents.kt: Canvas accents — pin-hole column (pressed rim + deeper disc + bottom highlight), two rotated corner strokes (bottom-left), three fading ticks (top-right); size-shadowing avoided (w/h).
- ExperimentsScreen: "Paper & headers" section with 3 toggles.
- Wired as FIRST child (drawn behind content, torn-clipped) in SettingsHeroHeader (12 screens), CabinetHeroHeader, ProfileHero, Home quest hero. Settings/Profile pass symbolTint (AMOLED-consistent), Cabinet/Home pass ink/questInk.
- Home stat bar: when paperStatCardsState, solid paperStatBg (light lerp(heroFill,0xFFFFF6EB,0.62), dark lerp(heroFill,0xFF2A211C,0.50)) + 3dp elevation; Box background branches at Modifier level (Color vs Brush).

Validation: braces OK (7 files), diff --check clean, code review passed (2 cosmetic fixes applied: symbolTint on AMOLED). Reviewer note: Home corner strokes sit behind the stat card (partially hidden) — acceptable for the experiment; holes + ticks still show on Home.

CI fix pushed earlier this turn: 28122f2 (Cabinet LazyGridItemInfo.offset.y — IntOffset vs LazyListItemInfo.offset Int).
- Entry Detail hero also wired (per-category heroInk). Committed + pushed on Alpha; PR Alpha → main tracks the branch, so every future push keeps it updated.
- v27b: Notifications section removed, all notification rows (daily reminder + hour chips, bubble-in-dialog) merged into Preferences; evolution level raised 7 to 15 (CurioPet gates, label, hint, comments); DEFAULT_CURLED_16/32 sleep sprites redrawn to match the standing pet (head/ears/scarf/tucked feet). Committed + pushed (PR #17 auto-updates).
- v19: pet games isolated — camouflage is now a find-me round (tap the faint ghost to win; the old visible edge-dash teleport is gone, it fades in place and slips away invisibly), taps mid-game only interact with the game (no boop/dart queued), and all three games (hide-and-seek, camouflage, spark) wind down into a ~3.2s touch-interruptible idle with pokes/peeks suppressed afterward. Committed + pushed (PR #17 auto-updates).
- v20: navigateToTab now treats a pushed tab-route instance (Cabinet opened from Profile, stack HOME→PROFILE→CABINET) like any pushed screen — pops back to HOME first so the popUpTo+singleTop navigate can no longer self-cancel into a dead Home tap; genuine tab instances (entry directly on HOME) keep save/restore tab-state behavior. Committed + pushed (PR #17 auto-updates).
- v20b: light-mode wash-out fix — new theme-aware ink helpers curioRoseInk/curioGoldInk/curioSageInk (deep CoralInk/GoldInk/SageInk on light cream, pastels on dark/AMOLED) applied to every pastel-as-ink spot: Profile XP card, shared card headers, Home drawer, onboarding permissions, topic-history bookmark, quests (trophy/progress/chips/badges/stamps/dailies), support status+links+download progress, promo card, topic-db explored chips, crash screen, badge overflows; bonusGold dedupes to curioGoldInk. Committed + pushed (PR #17 auto-updates).
- v27b: paper experiments reworked to the intended placement — PaperTitleLines (2 short lines) under the title text in all 5 hero families (settings hub, cabinet, profile, home, entry detail) gated by Title cut lines; hero-edge accents (corner strokes/ticks/left holes) removed (PaperHeaderAccents.kt deleted); Stamped pin holes now punch SEE-THROUGH EvenOdd holes into the Home Streak·Cabinet·Topics paper card (Surface transparent when holes on, pressed-rim rings, border+shadow kept); experiment labels updated. Committed + pushed (PR #17 auto-updates).
- v26c: Topic Browser scroll rework — CurioScrollIndicator now maps knob travel 1:1 onto the whole list (scrollable/travel ratio + 1..2x ramp) and drains accumulated deltas once per frame (LaunchedEffect + withFrameNanos) instead of a coroutine per drag event (fixes lag + slow scroll); gesture rewritten on awaitEachGesture/awaitVerticalTouchSlopOrCancellation so a pure tap toggles the A��Z rail (drag gestures never fire onDragEnd for a tap, so it could never open); back-to-top arrow centered on the screen with the glyph centered in the circle (M3 Surface has no contentAlignment). Committed + pushed (PR #17 auto-updates).
- v27: hero ink-glass pills deepened — SettingsHeroActionPill, CabinetHeroActionPill and CurioSortDropdown fills went from 18%/42% (55% destructive) to 30%/55% (65% destructive) alpha with the border raised 28%→42%, and the sort dropdown gained its missing border; fixes search/sort/select pills being nearly invisible on the rose banner in Cabinet and Topic Browser (and consistently across the settings-family heroes). Committed + pushed (PR #17 auto-updates).
- v27: explore-session attachments — ExploreSession gained shared note + screenshotPaths (JSON-persisted); pending-write package now carries note+screenshots and survives session clear (append/remove/set + peek accessors, hasPendingWriteFor); CurioEntry/CaptureEntity gained sessionNote + sessionScreenshots (Room v6 migration 5→6); new SessionShots (app-private PNG store), ScreenFrameCapturer (single-frame MediaProjection → PNG), ScreenCaptureRequestActivity (transparent consent host), DeviceScreenshotWatcher (MediaStore ContentObserver auto-attach, permission-gated) registered from MainActivity; ExploreSessionService: captureConsent static + ACTION_CAPTURE + captureScreenshot with Android-14 mediaProjection FGS promotion, FLAG_SECURE on the bubble window (timer never appears in shots), finishToWritePage via ACTION_STOP, note-focus window flag flip; bubble reworked — NO morph animation (instant swap + one resize burst), icon-only pause/hide, note field (local draft → setSessionNote), screenshot button with count badge, Finish & write it down; ExploreReminderReceiver ACTION_STOP hands off note+screenshots; reveal flow asks READ_MEDIA_IMAGES once; SaveCaptureScreen — floating note button + live-reactive screenshots section (add via PickVisualMedia, remove with X) + attach on save; EntryDetail shows note + lightbox-tappable thumbnails; manifest: READ_MEDIA_IMAGES/READ_EXTERNAL_STORAGE/mMediaProjection FGS permission, service type specialUse|mediaProjection, ScreenCaptureRequestActivity registered. Committed + pushed (PR #17 auto-updates).
- v27c: backup/restore v5 — session screenshots join the in-app backup: export bundles filesDir/session-shots bytes keyed by original path (deduped across shared entries) into a new BackupPayload.sessionShots map; restore rewrites each capture sessionScreenshotsJson to restored paths via a whole-restore shared index (one original path -> one restored file, preserving one-session-shared shots) using hardened SessionShots.restore(key, bytes) (path-traversal guarded like ImageStorageManager); CaptureEntity.deserializeStringList made internal for reuse; validatedCaptures now normalizes sessionScreenshotsJson to "[]" for pre-v6 backups (NOT NULL column, Gson Unsafe skips defaults — restores of old backups no longer crash); data_extraction_rules comment notes session-shots live in the excluded files/ domain like audio. Committed + pushed (PR #17 auto-updates).
- v27d: CI compile fixes + recycle-bin expiry. Compile fixes: SaveCaptureScreen ExploreSessionStore handoff calls now pass context (appendPendingScreenshot/removePendingScreenshot/setPendingNote/clearWriteSessionHandoff); removed the bare non-composable {} block around the Session screenshots section; CurioCrashScreen detectCategory is @Composable (curioSageInk); EntryDetailScreen dropped the duplicate java.io.File import, the duplicate @Composable on SessionNoteBlock and restored @Composable on GalleryWallRender; HomeScreen punched holes with addOval(Rect(center,radius)) since Path.addCircle does not exist in compose-ui 1.12 alpha (verified from the cached jar); CurioScrollIndicator drag delta now change.position.y - change.previousPosition.y (positionChange became a Boolean in the alpha); CurioRoutes uses NavController.previousBackStackEntry (NavBackStackEntry accessor removed in navigation 2.9); ScreenFrameCapturer smart-casts projection via explicit null check. Feature: recycle-bin expiry — AppPreferences gained recycleBinExpiryDaysState (default 30, 0=keep forever) with get/setRecycleBinExpiryDays; CaptureDao+CaptureRepository gained one-shot getTrashed(); new RecycleBinExpiry.purgeExpired(context) purges entries past the window with media deletion off the main thread (Dispatchers.IO); MainActivity purges on cold start via lifecycleScope; RecycleBinScreen gained an Auto-delete after row (Keep forever/7/30/90 days radio dialog mirroring SearchEngineDialog), purges on open, re-applies immediately on window change, and shows a bottom hint when the bin is empty. Reviewed, braces/diff clean.
- v27e: backup v6 — the pending (unsaved) write handoff rides the in-app backup. ExploreSession gained clearPendingWrite(context) (topic-agnostic). CurioBackupManager FORMAT_VERSION 6 with a new PendingWriteBackup(categoryId, topicName, elapsedMillis, note, screenshotPaths) + BackupPayload.pendingWrite; export reads pendingWriteTarget() once (pwTarget) and merges its screenshot paths into the sessionShots bundle (deduped with capture shots) then builds the payload object from peekWriteSessionMillis/Note/Screenshots; restore, after ExploreSessionStore.seed, remaps the pending write screenshot paths through the SAME shared shotIndexByPath (shot-$idx, so a shot shared by an entry and the pending write restores once) and re-handoffs via handoffWriteSession, else (pre-v6 backups) clears the stale prefs-resurrected package so the write page never shows dangling attachments. Reviewed; braces/diff clean.
- v27f: explore-session duration shown alongside the topic. SaveCaptureScreen topic strip: the explored row moved from under the topic to sit INLINE next to the topic name (Timer glyph + formatSessionShort, topic ellipsizes via weight(1f, fill=false) + TextOverflow, added import; comment updated). EntryDetailScreen hero: when sessionTimeMillis > 0 a centered pill (Timer + explored X, heroCardInk 12% fill + 28% border) renders between the title and the frosted meta bar, with the trailing spacer tightened to 10dp when it shows so the fixed 400dp hero never overflows. Reviewed; braces/diff clean.
- v27g: CI fix — HomeScreen paper-hole punch now uses Path.Direction.Clockwise (compose-ui 1.12 alpha renamed the enum constants from CW/CCW to Clockwise/CounterClockwise; verified from the cached ui-graphics classes.jar constant pool).
- v27h: Home paper-card + session fix batch. TornStatPaperShape (PaperCard.kt) — stat card wears a torn outline (EXTENDED bold soft tear on the top edge, corner-faded; sharper 3.5dp value-noise jitter on the other three) behind a new Torn paper edges experiment toggle (AppPreferences.paperStatTearState + Experiments row). HomeScreen: stat card shape switches torn/rounded, stamped holes moved to a VERTICAL column down the LEFT edge (EvenOdd punch reuses the Surface own outline via createOutline), Topics stat always shows the true total (produceState seeded from the warm cache, refreshed with TopicJsonLoader.countCanonicalTopics). ExploreSessionService: FLAG_SECURE removed from the bubble overlay + note-focus flags so device screenshots work during sessions. SaveCaptureScreen: attachments section also shows when a note/screenshots exist; note editor wears note-paper colors (theme-aware cream/dark + paper ink), floating note button is a solid accent pill. EntryDetailScreen: SessionNoteBlock wears the same note-paper surface (dead tintWash local removed). SoundBiteFormat: Delete recording action (AudioStorageManager.deleteAudio + idle reset). Reviewed; braces/diff clean.
- v27i: content-expansion batch B1 — 15 new category lanes. Category.kt: CategoryId grew BIOLOGY, CHEMISTRY, ANIMALS, PLANTS, TECHNOLOGIES, ASTRONOMY, HISTORY, GEOLOGY, MEDICINE, PSYCHOLOGY, MATHEMATICS, ECONOMICS, LANGUAGE, ENGINEERING, OCEANS (routeSlug + defaultOrder + newLanes set; family maps into EXISTING families SCIENCE/BOOKS so no new CategoryFamily values and the CurioIcons when(family) stays safe); 15 CurioCategory entries with 15 new color triplets in CurioColors; `visible` now excludes unshipped new lanes (id in newLanes && !isReady) so Home chips/Spin sheet/Cabinet never show dead empty decks, and HISTORY is isReady=true (117 topics). CaptureEntity verb when + ExploreSession reflectionQuestion when cover all 36 values (verified exhaustive, 36 refs each). CurioCategoryCard gained comingSoon param (dimmed border/surface/icon, label swaps to Coming soon, combinedClickable enabled=!comingSoon, edge shine intensity 0.25). CategoryPickerScreen rewritten: 2-page HorizontalPager (Original = visible minus newLanes; New = all newLanes minus hidden, coming-soon tiles for !isReady), 5 preset chips (Brainy/Stories/Screens/Sounds/Everything, Everything resolves to all visible at tap; tap enters multi-select pre-ticked so the mix is visible+editable) replace the old subtitle, page-tab pills (Original/New with counts) animateScrollToPage, tap/hold hint restored in the tabs row. Assets: 14 empty [] lane files (wildcard merge iterates every CategoryId — empty files load fine) + history.json with 117 schema-compliant topics (all Read verb, HISTORY categoryId, ≤60min, no dup IDs across all 36 files). Reviewed; braces/diff clean.
- v27k: batch — (1) in-app MediaProjection screenshot capture REMOVED entirely (freeze fix): deleted ScreenFrameCapturer.kt + ScreenCaptureRequestActivity.kt; ExploreSessionService stripped of ACTION_CAPTURE/captureConsent/captureNow/captureScreenshot/onScreenshot wiring; ExploreBubbleContent lost the Screenshot button + count badge (controls are now just Pause + Hide); manifest dropped the mediaProjection FGS type + consent activity; DeviceScreenshotWatcher auto-attach kept and doc updated; TopicRevealScreen comment updated. (2) SaveCaptureScreen: scrollable format body wrapped in a Box; new SessionNoteFloatingPill (bottom-end, imePadding, accent pill showing the note text, toggles a paper note-editor popup above the save CTA); SessionAttachmentsCard is now title + screenshots only (note moved to the pill). (3) CaptureFormatComponents ImageThumb now renders a remove × badge (onRemove was wired but never drawn — Field Notes/Reel Notes/Marginalia attachments are now deletable; Gallery Wall already had remove-with-confirm). (4) Deck presets reworked in DeckPresets.kt: Science / Entertainment / Arts & Stories / History & Ideas / Everything / Clear (clearAll flag); TopicJsonLoader.countFor(id) added; "Mixed · N" labels (Spin DeckBar, BottomCta, both pickers' Mix buttons) now show the TOTAL topic count (LaunchedEffect + countFor, seeded with lane count so no "Mixed · 0" flash); Spin's inline CategoryPickerSheet gained the Original/New HorizontalPager 2-page layout + PickerPageTab (made public, shared from CategoryPickerScreen) + Clear-chip handling in both pickers. Committed + pushed.
## Batch: Technologies lane → 1000 + header depth toggle (v27j)

- Technologies: 1000 topics authored in 50 chunks (computing, internet, AI, robotics, cybersecurity, privacy, web economy, mobile, hardware, communications, fintech, gaming, VR/AR, data, smart home, health tech, everyday tech, tech history, creators, careers, education, energy, transport, aerospace, biotech, food tech, materials, manufacturing, semiconductors, browsers/search, e-commerce). Merged into technologies.json, deduped (1 slug collision renamed), local validator 0 problems, TECHNOLOGIES flipped isReady=true in Category.kt.
- CI fix: SessionNoteFloatingPill's root Column used BoxScope-only Modifier.align — moved the align into a Box wrapper at the call site.
- New "Deeper header color" preference (default ON) in Experiments → Paper & headers: CurioCategory.headerAccent() darkens themedAccent hue-preservingly (light 0.88, dark 0.94 lightness) and is applied to the three category-colored torn-hero fills (Cabinet, Entry Detail, Topic Reveal). Watermarks/ink untouched.
- Committed + pushed (205b1d4 for astronomy; this batch on top).
- Content marathon: geology.json 1000 topics merged + GEOLOGY isReady (v27k). Next lanes: medicine, psychology, mathematics, economics, language, engineering, oceans.
- v27l: updater rewritten to hit releases LIST (prerelease-aware) + isNewer = different-tag; heroBlueState pref (OFF default) + HomeAzure/HomeAzureDark; azure branch in homeRoseAccent/profileRoseAccent/settingsRoseAccent; Quests CurrentQuestCard routed through settingsRoseAccent; Appearance toggle added; pushed.
- v27l teaser rewrite: astronomy.json done (1000/1000 rich, 74-134w each, avg 103w; 0 validation problems; committed). Next: technologies (1000), geology (1000).
- v27m teaser rewrite: technologies.json done (1000/1000 rich, 101-260w each, 0 validation problems). Next: elevation-vs-borders app-wide conversion (user: use elevation everywhere, all themes incl AMOLED).
- v27n elevation pass: app-wide border→elevation conversion complete (37 files). All BorderStroke/Modifier.border card outlines replaced with shadowElevation (+ tonal steps); AMOLED black fills → surfaceContainerLow step (shadows invisible on pure black); selected states raise elevation (4–8dp). Only exception: M3 FilterChip's required border param is 0dp transparent (canonical no-border). Technologies 1000/1000 teasers done + pushed earlier. CI-compile checks (braces, imports, remnants) all pass.
- v27o CI-compile fixes for the elevation pass (10 spots): duplicate shadowElevation args in EntryDetail mood board, Home floating pill + session card, TopicReveal tags pill, Spin deck surface, ExploreBubble icon button; OutlinedButton (Home) now uses ButtonDefaults.buttonElevation + 0dp border; PetDesigner Card uses CardDefaults.cardElevation; CurioCategoryChip drops the unsupported selectedElevation; ExploreBubble overlay stays flat (windows clip shadows). Plus the "Explore in YouTube" dialog button no longer wraps (maxLines=1 + ellipsis + tighter padding). All brace/dup/elev checks pass.
- v27p ink-contrast fix (CategoryInk.kt): computed WCAG contrast for every old+new accent in light/pastel — mid-lightness accents (green/lime/sky/amber/emerald/teal/red/fuchsia/blue/coral) read 2.0-4.0:1 as text ink. Added readableLightInk (same-hue, L=0.24) + needsLightDeepInk (luminance>0.105) rule; categoryInk/readableAccentInk/categoryInkFor deepen only mid-lights, onAccent pastel-light always uses the deep twin, pastelFillInk light branch deepened 0.30->0.24. Verified all 22 accents >= 4.5:1 on wash/card/pastel fills.
## v29 UI batch — hero controls, filters, links, save-capture boxes (committed, pushed)

- **Hero sort/search/select (Cabinet + Topic Database):** CurioSortDropdown is now ONE pill with two tap zones (label+chevron opens the menu, VerticalDivider, arrow toggles direction) — bigger (44dp, labelLarge, 22dp glyphs). Dropdown redesigned (20dp corners, tonal, "Sort by" header, check on active field). Hero pills + sort pill + both heroes' SEARCH FIELDS moved off the too-dark ink-lean fills to a LIGHT frosted glass (`lerp(backdrop, White, 0.24/0.38)`; destructive black-lean 0.14; search container `lerp(fill, White, 0.30)` with full-ink borders). Pills grew to 14/10dp padding + 22dp glyphs.
- **Spin FilterSheet:** chip grid capped at 2–4 columns (`(maxWidth/92dp).toInt().coerceIn(2,4)` via BoxWithConstraints) — no more two huge slab-chips on phones. CompactChip: inactive fill lifts white (0.04 dark/0.10 light), 2dp shadow in both states + curioDarkGlow — unselected chips read as raised pills.
- **Spotify/Apple links:** Apple Music URL fixed with the required `/us/` storefront segment (without it the app never recognizes the link); Spotify `/search/` verified correct. Tapping Explore / Watch in AUTO-COPYs the search query to the clipboard (toast confirms) so the user can paste the topic into the app's own search box (in-app handoff doesn't work for search). Added CurioIcons.ContentCopy (glyph verified in font).
- **Save your take:** attach boxes opaque — new `categoryTintFill(accent)` helper (opaque lerp of accent into surfaceContainerHigh) used by ImageThumb, AddImageButton (Reel Notes review + Field Notes) and JournalVoiceNoteRow (Marginalia journal). Topic strip now wears `cat.categorySurface(...)` (matches cards, no transparency) + curioDarkGlow for dark-mode elevation + categoryInk text (readable in light/dark/pastel).
- Validated: braces balanced across all 10 edited files, glyphs verified, LocalClipboardManager/VerticalDivider patterns match existing usage. Committed; pushed for CI.
## v29 UI batch 2 — progress redesign, chip growth, screenshots, DB no-loading

- **Progress pill + editor redesigned** (`ui/components/CurioProgressPill.kt`): compact opaque pill (new signature topic/accent/ink/background/showBar) + new `CurioProgressEditorDialog` — category-accent container, circular ring with big % + count, −/+ steppers, stepped slider, Finish + Save ONLY (no Reset/Cancel).
- **Placements:** Topic Reveal hero → small opaque frosted count badge TOP-RIGHT (old bottom-straddling pill that clipped in the morph removed; solid fill fixes the "pill color not good/transparent" complaint). Entry Detail hero → small bottom-RIGHT pill (tint bg + accent bar). Cabinet card hero → rising progress FILL (50% = half filled, done = fully colored, AMOLED-safe) + small count pill bottom-right; bottom body strip removed.
- **Chips:** Cabinet + Topic DB sticky chip bars rest at FULL scale (was 0.90 → looked like growing on entry); pop now 1.0→1.05 on scroll only. Colors stay opaque solid pills.
- **Screenshots:** watcher coalesces bursts, adds a 1.5s delayed re-pass for late-indexed MediaStore rows, and matches /Screenshots/ paths (OEM IMG_ names). Fixes fresh screenshots not auto-attaching + scan pile-up freeze.
- **Progress durability:** TopicProgressStore uses commit() (lost async apply() writes = vanishing progress) + MainActivity onResume re-seeds.
- **Topic Database zero-loading:** new prebuilt `topic_index.json` (generated by gitignored `scripts/build_topic_index.py`; regenerate after topic edits) — single-file merged catalog with lowercase keys + year precomputed at build time; `TopicJsonLoader.loadIndex()` cached + prewarmed at app start; DB renders from it instantly with a graceful per-category fallback. 16,130 topics, 0 dupes, year logic verified identical, ~0.8MB APK delta.
- **20k+ scaling alternatives (per user request, no lazy loading):** (1) this prebuilt index is the first step — precompute ALL derivations (lowercase keys, years, sort orders) at build time so runtime work stays O(0). (2) Ship a prebuilt SQLite DB (assets, copied to app dir on first run) with an FTS5 search index — instant LIKE/FTS queries at any size, standard "app element" feel. (3) FlatBuffers/ProtoBuf binary index — parse-free, mmap-able, instant at millions. (4) Move sort/search into a build-time-generated sorted structure the screen just walks. All avoid runtime parsing of 20k+ topics; the index implemented here is the lightweight first step.

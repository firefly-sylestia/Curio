# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "now what did u do to the drawer, why did u add your lanes in drawer burh, remove it and do
> something else, with the graph or something. also just like how when chnaging pages the chapter
> also updates, do the same for pages too, and also the progress overriried based on the user pdf
> progress if they open again and read some it will be updated to that and add holding the arrow
> for faster page forward. extend the home screen tear more and put the todays quest shuffle the
> deck inside it remove the a fresh mix of ideas picked for you text, and also make the home screen
> welcome back curiours explorer, and todays quest etc font hirarcy more better bigger cleaner.
> lets revamp the apperance theme options, so in apperance make the theme option as top and do this,
> light dark and system they became one row light gets sun icon, night gets moon icon, system gets
> half moon half sun style icon. no tick and make it proper toggle style which moves smothly on
> switching then below the main theme options, instead of material theme being a toggle, the hero
> being a toggle and adaptive hero, hero rose and azure being a toggle, make it into one buttom
> sheet, which opens when i tap the new color theme button, it shows its preview with a hint of
> colors and each theme gets a row, pastel color stays on apperance and category tint too. now lets
> add new color scemes with proper pantone colors, 1st pantone p 7-9 u as backgroud, hero and cards
> and button backgrouds p 109-10 u, and font text and icon color p 101-16 u, next one backgroud
> pantone p 24-9 u hero and cards pantone 2350 u, and text colors and icons p 14-8 u. another
> backgroud, p 1-3 c hero p 163-8 c and text colors p 102-6 c"

> "i forgot to mention in those new accent colors ddont use any border for cards and dont use
> transparent colors"

### Clarified with the member (earlier in this workstream, ask_user)

1. The drawer's lane grid is going — its replacement is **"a unique graph look with star style
   something, but not progress style graph."**
2. The home tear grows **"taller enough to hold the quest inside it (~+80dp)."**
3. Each Pantone theme gets **"a dark twin"** (they are all light palettes).
4. Inside a Pantone theme: **"Pantone ink for accents, a deeper shade for body text."**
5. Then the addendum above: **no card borders and no transparency** in those themes.

## 2. What the code actually looked like (findings)

- **The drawer carried a lane grid** (`CurioLaneGrid`, 4 columns of tiles with a knowledge
  meter) — a progress chart parked in a navigation panel. It is the thing the member asked to
  replace, and `CurioLaneGrid` is still the right component for the Stats page.
- **The quest sat BELOW the hero** (`QuestShuffleCard` in a centered column under a 26dp
  spacer), and it reached for `MaterialTheme.colorScheme.onSurface` for its title — a dark plum
  that would read as a stain the moment the block moved onto the rose banner.
- **The Appearance page had three switches that were one question**: "Material theme", "Hero"
  (rose/azure) and "Adaptive Hero" — plus a `CompactSegmentedRow` for Light/Dark/System.
- **The book page's two steppers only agreed one way**: `onPage` carried the chapter with it
  (`chapterForPage`), but `onChapter` wrote only the chapter — the page stayed where it was.
- **The book's page mark could be stale forever**: the progress card picks between the book
  row's `currentPage` and the reader's position by *most recent write wins*, and resolving a
  cover / saving a blurb / attaching the file bumps the ROW's stamp without moving the page —
  so a hand-set page could outlive the page the member was really reading.
- **The page bar's arrows turned exactly one page per tap** (`ReaderChromeButton` +
  `Surface(onClick)`), with no way to get through a chapter quickly.

## 3. What was done

### Appearance: one color-theme door, three Pantone palettes (v411)

- **`PantoneThemes.kt` (new)** — `PantoneTheme` holds the three palettes the member specified
  (P 7-9 U / P 109-10 U / P 101-16 U, P 24-9 U / 2350 U / P 14-8 U, P 1-3 C / P 163-8 C /
  P 102-6 C) as page / hero / ink, each with a LIGHT scheme, a DARK twin built from the same
  three numbers, and an ink ladder (`inkOn`, `pageInkFor`, `cardInkFor`, `accentFor`,
  `onHeroFor`) because two of the inks cannot carry body text on their own page.
- **Both of the addendum's rules are in the palette itself**: every colour is OPAQUE (a faded
  role is a `lerp` mix, never `copy(alpha = …)`), and `outlineVariant` is the DIVIDER hairline
  rather than a card edge.
- **New shared helpers in `CurioTheme.kt`**: `curioCardEdgeColor(fill)` (the theme's card edge,
  answering `fill` itself under a Pantone theme → no border) and `curioTintOn(base, tint, alpha)`
  (an alpha tint, resolved solidly under a Pantone theme). Applied at the card sites:
  `CurioSettingsCard`, `SettingsOptionCard`, `StatsCard`, the `StatsDoorChip`, the settings rail
  tab / quick-tool chip / search card, and the lane tile's ring.
- **`AppPreferences`** gained `colorThemeState` + `setColorTheme` + `getColorTheme` (the one
  choice; the three legacy switches stay in step, and the value is DERIVED from them for anyone
  who has never picked). `curioColorScheme()` answers a Pantone theme before the Curio schemes,
  and `settingsRoseAccent` / `settingsReadableInk` / `settingsCardAccentInk` read the Pantone
  hero/ink first, so the torn heroes and option cards wear it too.
- **The Appearance page**: the Theme option leads, as one sliding Light/Dark/System switch with a
  sun, a moon and a drawn half-and-half mark (no ticks), and the three old switches are ONE
  "Color theme" row opening a bottom sheet of previews (three swatches + the Pantone reference per
  row). Pastel colors and Category tint stay on the page; the deep-search keys for the removed
  rows now point at `appearance-color-theme`.

### Home: one hero, and the drawer's star map

- **The quest moved INSIDE the torn banner**, which grew 300 → 380dp (landscape 230 → 296dp).
  `QuestShuffleCard(plate, ink, copyInk, …)` now takes every colour as a parameter: a paper-white
  disc on the banner with the banner's own ink. The shuffle action is hoisted into
  `onQuestShuffle` so the tour step and the fresh-deck draw live in one place.
- **"A fresh mix of ideas, picked for you" is gone**, and the hierarchy is bigger: the eyebrow is
  `labelLarge` over a `headlineMedium` 28sp title.
- **The greeting is one fixed line** — `homeGreeting()` returns "Welcome back" and the name under
  it is the star (40sp ExtraBold against a 26sp SemiBold greeting). Hero and glass header share
  the string.
- **`DrawerLaneStarMap` replaces the lane grid in the drawer**: one star per lane, phyllotaxis
  positions keyed to the lane COUNT (so a star never moves as knowledge changes), size and
  brightness from knowledge, colour from the lane accent, hairlines to each star's two nearest
  neighbours, a 30dp tap halo and an orbit on the pick. `CurioLaneDetailStrip` names the pick
  below the map. Nothing is transparent (opaque `lerp` mixes) and the light-up is a one-shot
  `Animatable` — the drawer is composed while closed, so an idle twinkle would cost battery.

### Books: the two steppers agree, and the reader owns the page

- **A chapter move carries the page** (`chapterStartPage`, the reverse of `chapterForPage`).
- **The reader writes its live page onto the book row** — `repo.setPage(bookId, index + 1)`,
  debounced, for real pages only — so "reopen and read some" is what the book shows.
- **`ReaderHoldButton`** is the page bar's arrow: tap one page, hold to keep turning.

## 4. Still open / worth a device pass

- **Nothing here is device-verified.** No Gradle in this environment: `node scripts/check_braces.js`
  passes (283 files) and every new symbol was checked against its real definition by reading it,
  but **CI is the compile check**.
- Worth watching on device: the star map's tap target with 30+ lanes and whether the one-shot
  light-up reads on a 60Hz panel; the hero at a large system font scale (the banner now holds
  greeting + name + stat bar + quest); the Pantone themes' card ladder on Home and the Cabinet
  (no borders anywhere) and the dark twins' contrast; a PDF's page mark after reading (open the
  book page and confirm the number is the page you stopped on).
- The Pantone hex values are the published sRGB equivalents of the Pantone references — a Pantone
  number is an ink, so its screen value is the closest sRGB match. The reference itself is carried
  in each `PantoneTheme` entry and printed in the sheet, so the numbers stay the source of truth.

---

## User prompts

Status: **done — committed and pushed on `main`.**

> "now what did u do to the drawer, why did u add your lanes in drawer burh, remove it and do
> something else, with the graph or something. also just like how when chnaging pages the chapter
> also updates, do the same for pages too, and also the progress overriried based on the user pdf
> progress if they open again and read some it will be updated to that and add holding the arrow
> for faster page forward. extend the home screen tear more and put the todays quest shuffle the
> deck inside it remove the a fresh mix of ideas picked for you text, and also make the home screen
> welcome back curiours explorer, and todays quest etc font hirarcy more better bigger cleaner.
> lets revamp the apperance theme options, so in apperance make the theme option as top and do this,
> light dark and system they became one row light gets sun icon, night gets moon icon, system gets
> half moon half sun style icon. no tick and make it proper toggle style which moves smothly on
> switching then below the main theme options, instead of material theme being a toggle, the hero
> being a toggle and adaptive hero, hero rose and azure being a toggle, make it into one buttom
> sheet, which opens when i tap the new color theme button, it shows its preview with a hint of
> colors and each theme gets a row, pastel color stays on apperance and category tint too. now lets
> add new color scemes with proper pantone colors, 1st pantone p 7-9 u as backgroud, hero and cards
> and button backgrouds p 109-10 u, and font text and icon color p 101-16 u, next one backgroud
> pantone p 24-9 u hero and cards pantone 2350 u, and text colors and icons p 14-8 u. another
> backgroud, p 1-3 c hero p 163-8 c and text colors p 102-6 c"

> "i forgot to mention in those new accent colors ddont use any border for cards and dont use
> transparent colors"

All of it is implemented as described above, including the addendum (opaque Pantone palettes and
no card borders under them). Earlier prompts in this workstream are recorded in git history.

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and its
status stay at the top, and the empty slot below is where the next instruction lands. -->

# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "now the new pantone colors we added, well the home profile hero etc they dont get the
> pantone colors why? i want all of them to get the colors no other colors, nand if
> possible add some more colors er pantone theme for things that dont have colors"

Answered on ask_user: **every** listed surface switches to the Pantone palette — the Home
& Profile heroes and inks, the lane page washes, the error/warning red, the gold/mint/sage
brand inks, and the 36 lane/category accents. The extra colours are to be **derived from
the theme's own three numbers** (no new Pantone references, nothing invented from grey).

## 2. What the code actually looked like (findings)

- **The reported bug is a missing resolver branch, not a missing colour.**
  `settingsRoseAccent()` / `settingsReadableInk()` (SettingsHubScreen.kt) had a
  `activePantoneTheme()?.let { … }` first branch, so Settings and the Cabinet hero wore
  the Pantone palette. Home and Profile did not use those functions — they had their own
  copies, `homeRoseAccent()` / `homeReadableInk()` (HomeScreen.kt) and
  `profileRoseAccent()` / `profileReadableInk()` (ProfileScreen.kt), and **none of the four
  ever called `activePantoneTheme()`**. They fell straight through to
  Material-container → the last Spin lane → azure → rose-wood.
- **`heroPageBackground()`** only overrides the page when a lane is active, and
  `CurioCategory.categoryBackgroundWash()` (CategoryInk.kt) painted the lane wash on every
  category page — so under a Pantone theme the pages were still lane-tinted.
- **The schemes were only partly Pantone.** `secondary`, `secondaryContainer` and
  `tertiary` were literally the hero again, and `error` was `CurioColors.WarmCoralRed`
  (light) / `Color(0xFFE0706A)` (dark) — the app's own coral, not a Pantone number.
- **Two brand inks had no Pantone branch at all:** `curioGoldInk()` (streak flame, XP) and
  `curioSageInk()` (done ticks, mastery) returned `CurioColors.ButterYellow/GoldInk` and
  `Sage/SageInk` unconditionally.
- **The 36 lane accents had one central home:** almost every category colour in the app
  goes through the `CurioCategory` extensions in `ui/theme/CategoryInk.kt`
  (`themedAccent`, `categoryInk`, `headerAccent`, `onAccent`, `heroHeaderInk`,
  `categoryBackgroundWash`, `categorySurface`, `categoryChipSurface`, …), which is what
  made the sweep tractable. Only three Android call sites used the raw `accent` field:
  `CurioCategoryCard`'s selected crown, the reveal hero's `CurioProgressPill`, and
  `ExploreSessionService`'s notification tint.

## 3. What was done

- **`ui/theme/PantoneThemes.kt`** — five derived roles, all readings of the same three
  numbers (opaque `lerp`/HSL, never `copy(alpha)`, so the two Pantone rules still hold):
  `secondaryFor` (hero one step deeper), `tertiaryFor` (the ink as a fill), `goldInkFor`
  (the hero at ink depth), `sageInkFor` (the ink, desaturated), `errorFor` (the ink at
  alert depth). The light and dark schemes now use them for `secondary`,
  `secondaryContainer`, `tertiary` and `error`/`onError`.
- **`ui/theme/CurioTheme.kt`** — `curioGoldInk()` / `curioSageInk()` answer the Pantone
  palette first; added `activePantoneThemeNow()`, the non-composable twin of
  `activePantoneTheme()` (`activePantoneTheme()` now delegates to it), for the paths that
  cannot read composition state.
- **`features/home/HomeScreen.kt` / `features/profile/ProfileScreen.kt`** — all four hero
  resolvers take the Pantone branch FIRST; Home's page background skips the lane wash under
  a Pantone theme and keeps the Pantone-tinted page every other screen wears.
- **`ui/theme/CategoryInk.kt`** — the Pantone branch at the top of `categoryInk`,
  `themedAccent`, `headerAccent`, `readableAccentInk`, `onAccent` (converted from an
  expression body to a block body), `heroHeaderInk`, `categoryBackgroundWash` (→ the
  scheme background: no lane wash), `categorySurface` / `categoryChipSurface` /
  `categorySurfaceMoodBoard` (→ the passed `base`, the scheme ladder),
  `notesSheetContainerColor` (→ the Pantone dialog surface),
  `notesSheetContainerColorForCover` and `notesSheetPalette` (→ null, so the book/album
  sheets fall back to the Pantone surfaces), and the non-composable twins
  `categoryInkFor` / `themedAccentFor` (via `activePantoneThemeNow()`).
- **Three raw-accent sites closed:** `CurioCategoryCard`'s `saturated` crown, the reveal
  hero's `CurioProgressPill` (accent + ink + frosted background), and
  `ExploreSessionService.notificationAccent` (the service's non-composable path).

Docs + notes: a new `### v412 — a Pantone theme paints the WHOLE app` section in
`app/AGENTS.md`, and two bullets (one ADD, one FIX) in the 20260922 changelog.

## 4. Decisions

- One `ask_user` round, because "all of them" could have meant only the heroes, and adding
  Pantone references needs the member's own numbers. The answers fixed both: everything,
  derived from the three numbers.
- **Deliberate consequence:** under a Pantone theme the 36 lanes no longer carry a
  per-category hue — every card, chip, banner and page wears the member's palette. The
  ask_user option said so explicitly and the member selected it.
- The cover-artwork sheet palettes were included: a cover's dominant swatch is not one of
  the member's three numbers, so those sheets fall back to the Pantone surfaces.
- `activePantoneThemeNow()` was added rather than making the watermark map composable: its
  `remember` calculation lambda is `@DisallowComposableCalls`, and the existing call sites
  already key on the theme, so a plain pref read there is safe.
- Not chased to exhaustion: direct `CurioColors.*` reads that are neutral (e.g. a
  `CreamWhite` lerp) or sit behind a non-Pantone branch were left alone.

## 5. Status

- Implementation done across 8 Android files; brace/paren balance verified on each (the
  one pre-existing +1 paren in `TopicRevealScreen.kt` is unchanged by this diff). CI to
  confirm compile on push.
- No pending follow-up.

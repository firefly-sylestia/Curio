# Prompt Log — current request

## Request (2026-09-07, active — Cabinet v2 build in progress)

### Done this session
- **v3xx11** (`5ee6f232`): CI compile fixes + Underline (`format_underlined`)
  & `link` icon glyphs re-subset into the bundled icon font (zero lost).
- **v3xx12** (`4684b1a7`): global persistent **text history** (TextHistory.kt)
  — pause / every-10-words / editor-close capture; pill + browser w/ pin,
  copy, restore-into-active-field, delete; wired into the share sheet,
  full-screen editor and Enlarge writing sheet.
- **v3xx13** (`05cf3271`): full-screen editor polish — ICON-ONLY top pills
  (Close/Aspect/Layout/Stickers/Polaroid/Text, scrollable cluster), panels
  confirmed below the card, polaroid no longer sliced by its selection
  outline (floating 8dp padded outline), print-size slider real travel
  (36→44% width, 46→55% height caps), sticker tap-outside deselect, smooth
  AnimatedVisibility panel open/close.

### Cabinet v2 — plan (user answers recorded)
User answers: experimental toggle **Settings → Experiments, default OFF**;
Home Save is **moved not deleted, only while the toggle is on** (old view
returns when off); sources = **liked + saved entries**; covers = **jacket
art, contain-fit** (books half-book w/ spine+sheen, albums square, series
poster — never stretched).

Grounded findings: liked books = `KEY_BOOK_FAVORITES` set in AppPreferences
(`getBookFavorites/toggleBookFavorite`); series = `KEY_SERIES_FAVORITES`;
covers = `KEY_BOOK_COVER_URLS` map + share-card cover fetch; saved things =
the Cabinet screen (`features/cabinet/CabinetScreen.kt`, 1631 lines) opened
with `screen = "cabinet"` from Home/other nav; experiments list =
`features/settings/ExperimentsScreen.kt` (CurioSectionLabel +
CurioSettingsCard + ExperimentSwitchRow); reactive boolean prefs pattern =
`XState by mutableStateOf` + KEY const + isX/setX + sync line.

Implementation slices (planned order):
1. ✅ **Toggle foundation** (this commit): `cabinetV2EnabledState` (default
   false) + `KEY_CABINET_V2`, is/set + sync; Experiments row under a new
   "Cabinet v2" section.
2. **View gating**: read the pref in the Home save/Cabinet affordances and
   the Cabinet screen — when ON show the v2 view (and repoint Home Save),
   when OFF current behaviour.
3. **Cabinet v2 screen** (`CabinetV2` route or mode inside CabinetScreen):
   collections of saved entries + liked books/series/albums; rows/cards with
   jacket-art cover component (contain-fit: book = half-book jacket w/ spine
   + sheen, album = square, series = poster) — reuse/abstract the share-card
   cover art renderer.
4. **Thoughtful features**: blur/glass toolbar, search/grouping, multi-select
   batch, customisation — to be prioritised with the user as slices land.

Backlog: multi-select stickers, sparkle info snap, quick-fact tap-out reset,
wider text-history coverage.

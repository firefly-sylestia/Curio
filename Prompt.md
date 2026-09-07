# Prompt Log — current request

## Request (2026-09-07, active — Cabinet v2 build in progress)

### Completed — share-editor batch: polaroid everywhere, sparkle solver, editor fixes (2026-09-07)

**Request (10 items):** polaroid editing in the bottom sheet + auto-open;
fix the Sunglow polaroid style + add more; polaroid on other styles; the
sparkle's title↔fact overlap fix sometimes doesn't fix ("use an advanced
system, research it, extend smart fit"); laggy/buggy title↔fact selection
switching; emoji overlapping a box can't be tapped to select; full-screen
preview shrinks when a panel opens below; fact text shifts left when the
box is widened; Collage favorites split over the two tones (raise the
tear); Paper footer hidden behind the bottom design / off-screen.

**All in `TopicShareCard.kt` (+ changelog):**
1. **Polaroid in the bottom sheet:** new `toolOpen == "polaroid"` panel
   (style/filter/size), toolbar pill, and tapping the print auto-opens it
   (both modes); the full-screen Polaroid button is no longer Collage-only.
2. **Polaroid styles:** Sunglow reworked (warm ivory + honey tape, the old
   flat butter-yellow read as a cheap sticker); added CANDY (rose + blush)
   and NOIR (charcoal + silver, light caption ink) — 7 looks total.
3. **Polaroid on other styles:** extracted the print into `PolaroidPrint`
   (Collage still calls it inline); TopicShareCard renders it as a shared
   overlay on non-Collage styles when a user photo is on the card (so
   default cards stay clean). Defaults right side, upper-middle.
4. **Advanced sparkle solver:** title lift (capped at pill/edge) → fact
   push (capped at the card bottom) → residual overlap SHRINKS the fact
   box (`factShrinkDp` on the plan, applied to factHeightFrac). Smart fit
   budgets bumped: Paper 2.0/1.8 → 2.2/2.0, mid-flow 1.5/1.4 → 1.6/1.5.
5. **Selection switching:** title tap box no longer paints a ripple
   (instant selection toggle on overlapping boxes).
6. **Emoji tap:** StickerEditOverlay is now live whenever stickers exist
   (was gated on the panel being OPEN — a placed emoji overlapping a box
   could never be selected again).
7. **Full-screen preview stability:** the card area is now a weight(1f)
   Box with the card at fillMaxSize; the Text/Stickers/Polaroid panels
   float over it (align BottomCenter) instead of shrinking it.
8. **Fact width centering:** `moveFact` widened branch no longer forces
   minWidth = target; the child measures naturally and is centered in the
   widened box (short facts stay centered).
9. **Collage fav strip:** top 112 → 100dp so its last row clears the torn
   seam (the "divided" look); the tear itself rises 0.42 → 0.40.
10. **Paper footer:** smaller (12dp icon, 9sp type) + Column bottom
    padding 28 → 46dp so it clears the torn bottom strip.

**Not done / needs device:** the "collage preview glitched from the
sides" report couldn't be reproduced from code (likely the asymmetric
footer-wave ends or rounded-corner clip of the tear teeth at certain
aspect ratios) — left untouched to avoid a visual regression; the
overlapping title/fact switching jank may need on-device reproduction.

### Completed — sparkle auto-layout vs category pill overlap (2026-09-07)

**Request:** the auto-layout sparkle pill on share cards ignores the category
pill's position and overlaps it; make the adjuster smarter.

**Fix (TopicShareCard.kt):** the sparkle's measured-bounds collision repair
only watched title / fact / info rows / fav strip — the category pill
(badge) was invisible to it, so a grown fact box or a size-lifted title
could bury the pill. Now:
1. **Badge measured + threaded:** `onMeasuredBounds` reports a 5th rect
   (the badge); `measuredBadge` feeds `autoLayoutPlan`.
2. **Title lift capped at the pill:** the lift stops at the badge's bottom
   edge (the shortfall falls back to pushing the fact down), so a grown
   box can never shove the title into the pill.
3. **Pill joins the pushes:** a badge above the fact/meta/fav pushes those
   down; a badge dragged down onto the title is lifted back up; a
   fact/fav grown over it from above pushes it down (signed `badgeLift`,
   caps 48/64dp).
4. **Out-of-card clamp for the pill** (`fixBadgeX/Y`) like the other
   elements; changes persist into the move (`badgeDx/badgeDy`) so preview,
   saved card and export match; Reset Layout clears it.
5. **Bounds report in BOTH modes:** the measured-rect wiring only ran
   inside the edit-mode overlay, so a sparkle tap on the resting preview
   repaired nothing — the reporting is hoisted out of `editMode`.

Badge-free behavior is byte-identical (all new channels are 0 when no
badge/rects). Changelog bullet added. No settings toggle (fix of existing
behavior, per root AGENTS.md).


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

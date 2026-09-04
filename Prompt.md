# Request Log — share-card editor batch + topic-browser loading/search lag

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user, paraphrased)
1. The album favorites (heart-picked tracks) view is bad — it was adapted
   from Vinyl and doesn't fit the other card styles; it needs its own unique
   look per style.
2. Reposition the book cover on cards properly per style.
3. In card editing on Clean the title doesn't go too high when trying (move
   clamp bug) — fix those kinds of things.
4. When there's a lot of text it all looks visible in the preview but on save
   the font size differs slightly and the text gets cut — add a precise
   font-size control with a precise haptic slider instead of buttons.
5. Add centering hints / same-side hints like PicsArt while moving elements.
6. (User re-report, same batch) The topic browser is STILL very laggy — not
   just scrolling but loading and search — fix it.

## Root causes
1. The FavoriteTracksBadge was a single shared cream/rose "vinyl sticker"
   drawn at the same bottom-start corner on EVERY style, regardless of the
   design underneath. Vinyl also had its own typed favorite-song chip at the
   same corner.
2. The book cover always defaulted to the same top-right pocket, landing on
   top of each style's own corner art (glyph/crest/masthead).
3. Every MoveHandle drag clamped the cumulative offset against the ALREADY-
   MOVED rect, so each grab after the first re-clamped against an
   ever-shrinking range — elements got stuck partway (the Clean title that
   "wouldn't go high").
4. Text size used a row of fixed % buttons (0.5/0.7/0.85/1.0/1.15/1.3/1.5x)
   — coarse steps jumped from too-small to cut-off for long titles/facts.
5. No alignment feedback while dragging.
6. `produceState(initialValue = <16k inline map>)` re-evaluated the seed on
   EVERY recomposition (each keystroke, panel toggle, navigation return), and
   the index-branch remap inside the producer ran on the MAIN thread — the
   "loading/search is laggy" feel that survived the earlier identity fixes.

## What was done (app/src/main/.../TopicShareCard.kt, TopicDatabaseScreen.kt)
### Per-style favorite-tracks strips
- `FavoriteTracksBadge` now takes `style` + `palette` and picks per-style
  tokens (FavStripTokens: bg/border/ink/heart/typography/radius/alpha):
  Paper + Editorial + Collage parchment w/ serif italics, Vinyl keeps the
  cream chip, Minimal a square-cornered sans slug, Neumorphic a rounded dark
  pill with white type, Signature/Custom the tone palette.
- Call site anchors each style's strip in a per-style corner inset; Vinyl's
  typed favorite-song chip is suppressed via `hideTypedFavSong` when album
  favorites exist so the two never overlap.
### Per-style book-cover pockets
- `coverSlot` maps each style to its free corner: Paper/Vinyl top-right,
  Neumorphic below its big glyph, Editorial bottom-right, Minimal top-right,
  Signature/Custom below the crest; move offsets still apply on top.
### Drag-clamp fix (base-rect clamps)
- All five MoveHandle onDelta bodies now compute base rect = reported bounds
  minus current offset and clamp against the UNMOVED rect (title, fact, meta
  w/ its mPad, badge w/ its 2dp overhang, cover), so the reachable range
  never shrinks between grabs.
### Precise haptic text-size slider
- New `TextSizeSliderColumn`: 0.5x–2.0x, 1% (0.01) steps, haptic tick per
  step + confirm on release, live readout, replaces the pill row; Title vs
  Quick-fact context preserved (titleScale vs bodyScale).
### PicsArt-style alignment guides + magnet snap
- New `magnetAxis()` helper + `DragGuides` state in `ArrangeableCard`; each
  handle's onDelta snaps edges/center to card edges/center within 6dp
  (respecting per-element clamps) and reports active lines.
- MoveHandle gained onDragStart/onDragEnd hooks; a full-card Canvas overlay
  draws a faint center crosshair while any grip is live and bright
  halo+cored guide lines at snapped positions (draw-scope reads only, so
  drag updates redraw the canvas, not the overlay).
### Topic browser loading/search
- `initialValue` of the indexedTopics produceState is now a REMEMBERED map
  keyed on the cached-index identity (runs once per index instance instead
  of per recomposition); the index branch of the producer remaps under
  `withContext(Dispatchers.Default)` like the catalog branch.

## Validation
- No local Gradle (forbidden in this environment) — structural checks only:
  brace/paren balance of TopicShareCard.kt matches HEAD, single definitions
  of the new helpers, five onDragEnd sites, guide Canvas placed inside the
  edit overlay. CI compile + device behavior are the real check (pushed).

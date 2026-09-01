# Prompt.md — current request log

## Request (DONE): Follow-up fixes — picker crash, Editorial overlap, mixes grid, no cream

User (after the signature redesign shipped): fix the crash when opening
the category picker; fix the Editorial card quick-fact text still
overlapping itself; fix the grid view of "Your mixes" (looks bad); and
please don't use cream color (in the new signature designs).

### What shipped (this turn)

- **Category-picker crash fix** (`NewCategoryPicker.kt`) — the crash
  "Vertically scrollable component was measured with an infinity maximum
  height constraints" came from `Modifier.weight(1f, fill = false)` on (a)
  the sheet's classic/new `HorizontalPager` and (b) the `MixEditorSheet`
  category grid: `weight(fill = false)` measures the child with an
  INFINITE max height, which the pages' LazyColumn / LazyVerticalGrid
  passed through and crashed on. Both now use `weight(1f)` (fill = true)
  so the ModalBottomSheet's bounded height reaches the scrollables.
- **Editorial card overlap fix** (`TopicShareCard.kt`) — the drop-cap body
  rendered its wrap-row + full-width rest text as siblings in a
  `BoxWithConstraints`, which STACKS children at the same slot, so the
  rest text drew on top of the wrapped block. The pair now lives inside a
  `Column` (top-to-bottom layout).
- **Your mixes grid polish** (`NewCategoryPicker.kt`) — `NewMixCard` cells
  are a uniform 122dp height with the Spin pill bottom-anchored (was
  ragged per-teaser heights); the 3-dot menu is now an M3 `DropdownMenu`
  popup (always on top, anchored in a Box) instead of the inline
  `DropdownMenuSurface`/AnimatedVisibility that shoved the row when
  expanded. The custom surface + now-unused animation imports + unused
  `Color` import were deleted.
- **No-cream signature backgrounds** (`TopicShareCard.kt`) — per user
  direction, the new signature scenes swapped warm cream/beige fills for
  cool paper-white tones in BOTH normal and Deepen: Animals `EFF3F0`
  (cool sage paper), Anime `F5F6F8` (cool paper white), Artworks
  `ECEFF2` (gallery white), Authors `F1F3F6` (manuscript paper). Classic
  styles (Paper/Editorial/Minimal + `signatureDesignClassic`) keep their
  cream — the user likes those.
- DOX: app/AGENTS.md (v3xx3 color mentions + new v3xx4 bullet) + fastlane
  changelog updated.

### Progress
- [x] Fix picker crash (pager + mix-editor infinite-height scrollables).
- [x] Fix Editorial drop-cap/rest-text overlap (Box stack → Column).
- [x] Redesign mixes grid cards (uniform height, popup menu).
- [x] Swap cream backgrounds → cool paper (normal + Deepen).
- [x] DOX pass (AGENTS.md v3xx4 + changelog).
- [x] Syntax sanity (brace/paren balance OK; TopicShareCard paren delta is
      the pre-existing ±1, edits are paren-neutral).
- [x] Commit & push.

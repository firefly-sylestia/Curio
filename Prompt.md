# Prompt.md — current request log

## Request (ACTIVE → DONE this turn): Unify the share screen + mood-board quote option

User: "polish share hub, use the same share screen that used in topic reveal
share, the exact same in detail screen too. we will be adding ability to add
your quotes, or review etc instead of quick fact from detail view, not
editing but using the same from the detail saved entries, and in moodboard
save your entry, add the add quote option too, not inside the moodboard,
outside of it, did u do it? do this please"

Design answers already captured (from the earlier ask):
- Full swap to the exact reveal sheet; "Share as text" in the sheet too.
- Hub Share button opens the sheet + is a floating button (not bottom-of-grid).
- Mood-board quote option appears in save (during editing/adding), outside
  the board; saved board quotes feed the share sheet's Quote source.

### What shipped (this turn) — 4 files, one coherent change

**1. TopicShareSheet is THE share sheet** (`ui/components/TopicShareCard.kt`)
- New params: `initialStyle: Int = 0`, `initialClassicSignature: Boolean =
  false` (hub opens on the picked design), `shareAsText: (() -> String)? =
  null` (detail supplies its entry-aware payload; default = topic + fact).
- "Share as text" TextButton added BELOW Save/Share — every caller (reveal,
  detail, hub) gets it.
- The old `ShareHubBody` (the reduced preview + pills used only by the old
  EntryShareSheet) is deleted — dead code now.

**2. Detail screen uses the EXACT same sheet** (`features/detail/EntryDetailScreen.kt`)
- The More-menu share now opens `TopicShareSheet` with savedSources built from
  the entry's capture data: Quote (ReelNotes/Marginalia/SoundBite/**Gallery
  Wall mood-board quotes**), Review (ReelNotes text + star rating), session
  Note. QUOTES topics prepend the byline (same as before).
- The old `EntryShareSheet` composable + `ShareFormatPill` helper are deleted;
  `entryShareText` stays (used as the sheet's shareAsText payload, and its
  isQuote branch now also reads GalleryWall quotes).

**3. Share Hub opens the sheet from a floating pill** (`features/settings/ShareHubScreen.kt`)
- The long-grid bottom Share button is gone; a floating Share pill (bottom
  end, nav-bars padded) opens `TopicShareSheet` with the picked topic + the
  picked design preselected via `initialStyle`/`initialClassicSignature`
  (style index mapped through `availableStylesForFamily`).
- Aspect pills stay; removed now-unused imports (Button, ButtonDefaults,
  DpSize, shareComposableCard).

**4. Mood-board add-quote OUTSIDE the board** (`features/capture/formats/GalleryWallFormat.kt`)
- The below-board `QuoteCardsSection` (its own Add button + paper-style
  toggle) is ALWAYS shown again; the on-board floating-quote chip + canvas
  cards stay hidden (they misbehaved on the canvas). Below-board cards save
  into `CaptureData.GalleryWall.quotes` → surface as the Quote source pill on
  the entry share sheet (wired in #2).

### Progress
- [x] TopicShareSheet: initialStyle/initialClassicSignature + shareAsText + UI.
- [x] ShareHubBody removed.
- [x] Detail screen swapped to TopicShareSheet; old sheet + pill removed.
- [x] entryShareText gained GalleryWall quote support.
- [x] Hub floating Share pill + sheet launch with preselect.
- [x] GalleryWall below-board QuoteCardsSection restored; canvas chip stays hidden.
- [x] Balance verified 0/0/0 on all four files; changelog + this log updated.
- [ ] Commit & push (user asked for it: "do this please").
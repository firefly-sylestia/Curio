# Prompt.md — current request log

## Request: Reveal synopsis 5-line teaser + merged book-notes sheet; "Also in" from All; share-card editor overhaul; cover fetch → Experiments with Cancel

User request (verbatim, condensed):

> Why does the synopsis now show fully in Topic Reveal? Show only 5 lines
> and the full text in the bottom sheet. Merge chapters and synopsis in the
> SAME bottom sheet, so the sheet isn't too small and expands to the top.
> In Topic Browser, when the category is All and I'm searching, it doesn't
> show the "Also in" pills — it should. In the share sheet (hold to edit):
> while editing, remove the "Share this topic" header above so we have
> plenty of space; add haptics to the sliders; in the Paper style, editing
> the quick fact doesn't change its height — fix it; the cursor / text
> selection in inline editing still isn't accurate — fix it; change the
> T / F / M / B letters and their colors to ONE move icon with a darker
> coffee border; the category-chip move is inaccurate (the B grip shows at
> the same position even when the chip is lower) — anchor it to the real
> chip, and the bulb icon should move with it; in styles with lines above
> the quick fact, those should auto-adjust and move with the fact box; make
> the title and the info (author etc.) move together when they're
> adjacent. The book-cover fetching has no cancel option — add one and put
> it inside Experiments.

### What shipped (this turn)

**A. Topic Reveal — synopsis teaser + ONE merged book-notes sheet
(`TopicRevealScreen.kt`):**
- The page's `BookSynopsisCard` now shows a `maxLines = 5` synopsis teaser
  (poster beside it) + a "Read the full synopsis →" hint when long.
- `BookNotesSheet` is now ONE tall ModalBottomSheet (body
  `fillMaxHeight(0.92f)` so it expands toward the top, inner body scrolls)
  hosting BOTH sections behind segmented Synopsis | Chapters pills. The tab
  it opens on mirrors what you tapped (synopsis card → Synopsis, chapter
  chip → Chapters), and you switch in place inside the sheet — never too
  small, never a slim dialog.
- Page chapter preview chips stay compact (118dp, 2-line previews).

**B. Topic Browser — "Also in" from All (`TopicDatabaseScreen.kt`):**
- The search suggestion row was gated on `effectiveCats.isNotEmpty()`; now
  it also renders when searching from ALL, surfacing the lanes the flat
  results came from (top 6 by hit count), each pill one tap away.

**C. Share-card editor overhaul (`TopicShareCard.kt`):**
- Sheet header ("Share this topic") + style label + style dots all hide
  while editing, giving the card + controls the full sheet.
- `SizeSliderColumn` now ticks per snap step while dragging + a confirm on
  release (haptics captured in the composable scope — not inside the
  Slider's plain callbacks — so it compiles).
- Quick-fact editing: the invisible `BasicTextField` is now
  `maxLines = 24` with `heightIn(min = factHeight)` so the box AUTO-GROWS as
  you type (Paper included), and every style reports the exact glyph box +
  the exact TextStyle it rendered (`EditBoundsCallbacks.onFactStyle` +
  glyph-box reporting moved INSIDE FrostPane/cream padding) so the caret
  sits on the real letters and wraps identically.
- The per-box T / F / M / B letter handles are GONE — one uniform
  `MoveHandle` (DragHandle icon, `CoffeeChromeDeep` circle, white ring) and
  darker coffee box borders (`CoffeeChrome` / `CoffeeChromeDeep`).
- Badge grip: anchors to the reported PILL bounds (full-width chip rows
  report the pill Surface, not the row — reporting the row would zero the
  drag clamp), and the pill + bulb move as ONE group (moveBadge on the
  row, onBadge on the pill), so the bulb rides the chip.
- Rules above the quick fact (Vinyl underline, Editorial hairline, Minimal
  accent rule) get `factShift(move)` so they travel with the F drag;
  meta/byline rows under titles get `titleShift(move)` so title + info
  move together when adjacent.

**D. Settings — cover fetch → Experiments + Cancel (`BookCoverFetch.kt`,
`SettingsHubScreen.kt`, `UserExperimentsScreen.kt`):**
- `BookCoverFetchRow` moved out of the Safety & support hub into
  `UserExperimentsScreen` (new "Content tools" section) with a Cancel pill
  that cancels the in-flight fetch job (whatever was cached stays); dead
  hub special-case branches and the ROUTE const deleted.

### Notes
- Changelog + `app/AGENTS.md` (v317 bullet) updated. CI compiles on push;
  delimiters/imports/APIs hand-audited across the six files.
- Next up (user's queue): "save your take" redesign.

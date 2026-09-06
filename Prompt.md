# Prompt Log — current request

## Request (2026-09-06, active)

"now in book albumn etc buttom sheet, colors, so the page number albumn number
icon isnt visible when selected and also the like button and also the book
icon and same for albumns series and when selected its even more bad, the
colors etc is bad and also the color pallete should be remeberstae like even
when after resrart it goes back to defakt and switches after a second when it
should be instant. and then the enlarged text box text editing well it have
pick outline and that editing so fix that and add the save you text style
format text editing with highlights etc which stays when sharing too. also in
the full screen card editor the highligh bold italic etc are inside the tool
box when they should show as floating when selecting the text and only apply
to them if text are selected bnot entirely always and use the similiar logic
from save your take not editing, same for the full screen text editor too."

Ask answers: selection formatting for the FACT text only; keep the whole-element
toggles too ("keep both"); both enlarged dialogs get rich text; and fix the
crash + slider/swipe/glitch regressions from commit `11e566a5` (the full-screen
Text dropdown: infinite-constraint crash + its scrollable stealing drags).

## Implemented (v375)

1. **Notes-sheet palette INSTANT + remembered.** Cover swatches are cached per
   artwork URL and albums/series persist their resolved artwork URL
   (`AppPreferences`: `KEY_COVER_SWATCH_CACHE`, `KEY_SHEET_ART_URLS`,
   `bookChapterNoteSpansState` too; `CoverPalette.kt`:
   `coverSwatchesToArgbs`/`coverSwatchesFromArgbs`). All three sheets
   (Book/Album/Episode) seed their palette synchronously on first composition
   and refresh the cache in the background (keep cached on fetch miss).
2. **Selected-state contrast on light covers.** `notesSheetPalette.onAccent`
   now keys on HSL lightness (≥0.52 → dark ink) instead of linear luminance;
   open/tinted-row hearts use the palette ink, album selected hearts get
   full-strength `onAccent`.
3. **Rich-text fact editing that survives sharing.** `TopicShareCard` threads
   a `factSpans: List<TextSpan>` param through every style → `FactBody`
   (incl. the BOOK two-column + EDITORIAL drop-cap splitters via `richSlice`)
   with the translucent amber `ShareFactMarker`. Sheet state:
   `editedFactSpans`/`customSpans` (persist via `spansToJson`/`spansFromJson`),
   `routeFactChange` rebases spans on inline typing, `routeRichFact` carries
   text+spans from the Enlarge dialog, which now hosts `RichTextEditor`
   (Save-your-take style) — the chapter-note dialog too, and the note spans
   ride "Share as Chapter review" (`pendingChapterShare` Triple →
   `seedReviewSpans`). Save/Share exports pass the spans. Chapter-review cards
   shift spans past the "CH n · title" chip prefix (`reviewChipText` +
   `reviewChipPrefixLen` + `shiftSpans` → `cardFactRenderSpans`).
4. **Full-screen selection formatting.** The full-screen card's fact field
   keeps a REAL `TextFieldValue` (`richFactTfv` + `factTextLayout`,
   reseeded only on text change) and floats a non-focusable Popup B / I /
   highlight bar anchored to the live selection caret (field origin + caret
   rect through `editDensity`). Taps toggle `RichFlag` over exactly [s, e) in
   the active spans list (`toggleFactSelectionFormat`; `RichFlag`,
   `spansFullyCovered`, `toggleSpanFlag`, `FormatToolButton` made internal in
   RichTextEditor.kt). Whole-element toggles stay in the Text panel.
5. **Crash + glitch fixes (`11e566a5`).** Full-screen Text tools moved from a
   DropdownMenu (verticalScroll under unbounded height → the reported
   IllegalStateException, and the popup scrollable stealing slider/swipe
   drags) into an inline bounded panel (`heightIn(max = 300.dp)` + scroll)
   below the top bar in the dialog Column.

## Notes for next request

- CI will compile-check (no Gradle here). Riskiest spots: the ArrangeableCard
  rich branch + Popup geometry (full screen runs under a scaled Density —
  offsets go through `editDensity`), the shifted `cardFactRenderSpans` for
  chapter reviews, and the RichTextEditor param sets used in both dialogs.
- Braces verified balanced in all 6 edited files with a template-aware
  tokenizer.
- Changelog (fastlane 20260921.txt) + app/AGENTS.md v375 bullet added.

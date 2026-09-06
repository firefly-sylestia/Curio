# Prompt Log — current request

## Request (2026-09-06 — v379 line: full-screen text tools + follow-ups)

User follow-up (after v379 pushed): "no need to watch the cl continue the
task and at last use ask user". Their ask-user reply picked: floating bar
in bottom-sheet editing; an explanation of the size-multiplier question
(and whether smart fit uses its own hidden size system); a
`ShareCardMove` logic analysis for more refinements; fixing share-fact
text that still reads dark in dark mode; fixing the failed v379 CI.

## Completed

1. **v379 (aa57fb89)** — full-screen text-tool polish: `TextSpan.underline`
   end-to-end (build/extract/merge/rebase/JSON/richSlice), floating-bar U
   button, whole-element Underline in the sheet Format tool, icon-only
   round B/I/U in full screen, Highlight swatch row removed, Justify in
   the full-screen Align row, Book-page Column-gap slider (sheet Align +
   full screen), permanent tool captions. Docs + commit + push.
2. **v379b (68999e7c)** — CI fix: the selection bar's gate
   (`format != null || underline != null`) killed Kotlin's smart-cast of
   `onFormatFactSelection`; B/I/highlight now safe-invoke + gate on their
   own enable state.
3. **v379c (working)** — bottom-sheet `ArrangeableCard` calls (pager +
   single-style) now pass `richFactTools = true`, the fact spans and the
   format/underline channels, so the floating B/I/U/highlight bar works
   on inline fact selections in the sheet preview too. Stale comments
   updated. Docs updated. Committed + pushed.

## Open items (to finish this turn)

- Answer the user's size question in prose (see analysis below).
- Analyse `ShareCardMove` logic for refinements + report.
- Dark-mode share-fact text: NOT yet reproduced in code — every card
  style paints fixed palette surfaces/inks (no theme-driven fills), so
  the exact target must be pinned down with the user (ask_user at end).
- Final ask_user (user requested it at the end of the task).

## Size-multiplier / smart-fit explanation (verified against code)

Rendered fact text = style base × bodyScale × autoFit.textScale ×
factScale × factZoom, where:

- `bodyScale` — the Size slider's base (what the user sets).
- `autoFit.textScale` — smart fit's automatic shrink, active ONLY while
  the fact is untouched and Smart fit is ON; identity otherwise. It is a
  multiplier folded into the render, not a separate font engine — but it
  IS invisible on top of the user's slider base.
- `factScale` — after the user first grabs/resizes the fact, the fit
  shrink is seeded into `factScale` ("manual wins", no text pop), and it
  becomes a normal whole-fact text multiplier.
- `factZoom` — legacy whole-fact text multiplier (old whole-box zoom),
  now redundant with `factScale` after the Whole-box slider became
  independent.

So: smart fit does NOT have its own hidden font-size system — it nudges
the same bodyScale-type channel the slider drives — but it auto-adjusts
ON TOP of the slider's base, and three of the four factors are invisible
to the user. The Size thumb since v378 shows the COMBINED (rendered)
value; the fact-height slider still shows the RAW base without the fit's
box growth (an inconsistency worth fixing). Consolidation candidate:
drop `factZoom` (merge into `factScale`) and make the fit show its hand
in the sliders.

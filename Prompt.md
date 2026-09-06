# Prompt Log — current request

## Request (2026-09-06, shipped as v379 — full-screen text-tool polish)

User: "in full scren text editor the icon for b i and underline is weird
fix it. also add underline in tool bar too also full screen the highliter
in the buttom sheet of that text editor is bad remove it. and also add
justify aling format too abd in bok page fact layout add space adjust
between that. and also show the hint text for tool nme below always in the
tool bar. and fix some more functinal issues properly analyse it and tell
me what needs fixinf or missing use ask user and i will tell… and no need
to watch cl."

Ask-user answers captured: B/I/U icon-only round toggles; remove the
full-screen whole-element Highlight swatch row; Justify in the full-screen
align row; Underline in the floating selection bar; whole-element Bold /
Italic / Underline in the bottom-sheet Format tool; Book-page column-gap
slider; always-on toolbar captions; then (after push) deliver a full
share-card system + logic analysis for the user.

## Completed (v379)

1. `TextSpan.underline` (CaptureData) + full codec plumbing in
   RichTextEditor.kt: buildRichAnnotated renders a text decoration,
   extractRichSpans/merged/rebaseSpans preserve it, `toggleSpanUnderline`
   + `spansUnderlineCovered` toggle a selection's underline without
   touching the RichFlag trio. Added the missing TextDecoration import.
2. Share-card persistence: `spansToJson`/`spansFromJson` write/read a "u"
   key; `richSlice` (book two-column split) carries underline through.
3. Floating selection bar in ArrangeableCard gained a U button (new
   `onToggleFactUnderline` param, sheet wiring mirrors the flag toggle);
   bar width 132→172dp for the four tools.
4. Full screen: B/I/U are icon-only round EditToolPills; Highlight swatch
   row removed (unused `highlightPresets` dropped); Align adds Justify.
5. Bottom sheet Format tool: whole-element Underline pill (title + fact).
6. Book-page fact layout: `ShareCardMove.factGutter` persisted/parsed/
   reset; `FactBody`/`BookPageText` thread gutterFrac to the column
   spacer; Column-gap slider in the sheet Align tool AND the full-screen
   Text panel (BOOK format only).
7. Toolbar: `ToolWithCaption` lost its `show` gate — captions permanent
   under every pill; stale v377 comments updated.
8. Docs: changelog top entries, AGENTS.md v379 bullet, Prompt.md.
9. Commit + push. No CI watch per user request.

## Follow-up owed

- Full share-card system analysis (what can be improved / what's not
  right / unexpected behaviour) — to be delivered to the user after the
  v379 push.

# Prompt Log — current request

## Request (2026-09-06 — v379d/e: AUTO-LAYOUT pill + smart-fit transparency)

User picks (ask-user round): A (drop orphaned factZoom), B (Reset Layout
restores smart fit), C (hide Title-height when the title fits one line),
D (fold the cover title shrink), text-first smart fit (box stays 100%,
text shrinks with length; the Smart-fit toggle re-fits the box after
manual edits), remove the Whole-box slider from the UI, fix the bouncing
title-into-quick-fact collision, Paper quick-fact ink on dark premium
tones, and a full-screen-vs-sheet difference in the quick-fact lines.
No CI watching.

## Completed

1. **v379/v379b/v379c** — aa57fb89, 68999e7c, 1eae8e7b (earlier).
2. **v379d — 3fcfa4fa** — factZoom deleted; Reset Layout clears the fit
   seed; honest fact-height thumbs; cover-title-shrink folded into title
   thumbs; Paper qStyle/frostStyle palette ink; AUTO-LAYOUT sparkle pill
   (planner `autoLayoutPlan`/`autoFitSizing`, sheet `runAutoLayout`
   cycles attempts and commits into the per-style move; tall 9:16 when a
   3:4 card is capped; no-op attempts auto-skip; manual drags untouched).
3. **v379e — working tree (this commit)** — user follow-ups:
   - `autoFitShape` TEXT-FIRST (box keeps 100% height, text shrinks with
     length; box grows only past the text floor, capped by budget);
     `smartAutoFitDelta` = toggle/touched gate; pill attempts layered on
     the shape.
   - Smart-fit switch ON clears a manual box (width/height/scale →
     1; position drags stay) so it re-fits after manual edits; copy
     rewritten.
   - **titleLift unit bug**: lift computed in px but applied as dp — ~3×
     overshoot → clamped → the bounce. Now converted to dp, 0.5dp
     dead-zone, and guarded by `titleGrabbed || factGrabbed`.
   - Whole-box sliders removed from the Crop tool + full-screen Box
     section (title/fact/fav rows).
   - Title-height slider hidden when the displayed title is short
     (`editedTitleOrDisplay.length <= 22`).
   - Docs updated (changelog, AGENTS.md).

3. **v379f (working tree, this commit)** — user follow-up on the dark
   premium-tone text: Paper quote + frost text now use
   `ShareCardPalette.frostInk()` — near-WHITE when the blended pane
   (bgMid + 35% FrostPane white) is dark, near-black when light — so the
   quick fact never renders dark-on-dark on Onyx/Midnight/Wine/Cocoa etc
   in any app theme.

## Open (needs user)

- **Full-screen vs bottom-sheet quick-fact LINE differences**: both
  render the same TopicShareCard with the same move/bodyScale/spans and
  the density-zoom is uniform (dp+sp scale together) — static review
  finds no mechanism for different wrap. A screenshot of both side by
  side (or the design name + text) is needed to pin it down.

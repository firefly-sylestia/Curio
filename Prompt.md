# Prompt Log — current request

## Request (2026-09-06 — v379d: smart-fit transparency, dark-premium Paper
## ink, and the AUTO-LAYOUT sparkle pill)

User (ask-user custom reply): implement A (remove orphaned factZoom),
B (Reset Layout restores smart fit), C (title-height dead slider),
D (cover title shrink), plus: smart fit should use the box-height
adjuster too, add a spark round pill floating on the card corner that
auto-smart-fits (shows even when Customise is off), make it advanced —
nothing overlaps, user edits stay but it adjusts, box height/width/text
size adjust, too much text → the card automatically becomes 9:16 — and
tapping again cycles to another arrangement. Also fix share-fact text
that is still dark on dark premium tones / in dark mode. No CI watching.

## Completed

1. **v379 / v379b / v379c** pushed earlier (aa57fb89, 68999e7c, 1eae8e7b).
2. **v379d (working tree, next commit):**
   - **factZoom removed** (parse folds legacy value into factScale; field,
     render multiplier, slider write-back divisors, persistence gone).
   - **Reset Layout clears the fit seed** (factScale) so smart fit returns
     after a reset; resetLayout KDoc rewritten.
   - **Box-height thumbs honest**: sheet Crop tool + full-screen Box show
     factHeightFrac × fit.heightFrac and write the base back.
   - **Cover title-shrink folded** into the Title-size thumbs (sheet +
     full screen) for Signature/Custom + cover.
   - **Paper dark-mode fix**: qStyle/frostStyle carry explicit
     palette.ink (were inheriting the app theme's onSurface → dark-on-dark
     on dark premium Paper tones).
   - **AUTO-LAYOUT pill**: AutoLayoutPill UI + autoLayoutPlan /
     autoFitSizing planners + runAutoLayout() state machine in the sheet,
     pills over the current card in both the single-style and the pager
     branches. Commits box/text/format into the per-style move so preview
     + save + export match; attempts cycle fit → condensed → book →
     tall 9:16; no-op attempts auto-skip; manual drags untouched.
3. Docs updated (changelog + AGENTS v379d). Brace/delta checks clean.

## Open (closing ask_user)

- C (title-height dead slider) — deferred: needs a design decision
  (hide for short titles vs reinterpret). Put to the user.
- Dark mode: verify the Paper fix covers their sighting; if they saw it
  on another design, need the style name.

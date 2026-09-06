# Prompt Log — current request

## Request (2026-09-06, active)

"next in size the font size tool, why have you placed the smart auto fit in
there and also the auto fit densities remove them. also the smart fit should be
differnt toggle, and the smart fit itself should be smart and consideres the
entire share card no just the quick fact box, and also it should use the sliders
size etc for adjustments not its own differnt size logic, it will be confusing,
when the text gets larger too much it will use the wuick fact text size and
decrase it and it will incrase the fact height and fact wiidth. and for
arrangmemnt it will use smart collide detection so in automatic defult card
desotn go outsite of the card and and when needed the text sizes gets smaller
with the slider given no its own hidden size adjuster. and next this is small so
the auto colors for share card, so sometimes when the level unlocked color is
availabe the auto color is picking that u which should not happen so fix it. and
similiar to chapter progress and custom fact working togerther, make quick fact
work in the same way too like with chapter progress i can add the quick fact
too."

## Implemented (v374, commit pending)

1. **Smart fit rework (whole card, slider channels only)** — the old
   auto-fit (intensity presets, hidden factScale/titleScale shrink curves,
   title lift + fact/info nudge `dy`s) is GONE. `ShareAutoFitDelta` is now
   just `heightFrac` / `widthFrac` / `textScale`, applied through the SAME
   channels the user's sliders drive: `effectiveMove.factHeightFrac` (+
   factWidthFrac) and `effectiveBodyScale` (the bodyScale the Size slider
   sets). `autoFitGrow(len)` is one length curve (no presets) and
   `factFitBudget(style, aspect)` is the WHOLE-CARD collision budget (max
   box-height × + min text scale per design — Collage's fixed band barely
   grows, Editorial's byline→colophon slot a little, bottom-anchored
   Clean/Minimal grow into the free middle). Past the cap the box stops
   growing and the TEXT shrinks by exactly the overflow ratio (clamped to
   the style floor). Nothing is moved/shrunk except the fact box + fact
   text; manual box edits still win; the first-grab handoff seeds
   `move.factScale` with the fit's textScale so nothing pops.
2. **Smart fit moved out of the Size tool** — removed the Smart auto-fit
   switch + the Auto-fit intensity (Balanced/Compact/Airy) pills from the
   Size panel; `autoFitIntensity` removed from `ShareCardMove` +
   persist/parse. It's now its OWN toolbar tool (glyph
   `photo_size_select_large`, verified in the bundled icon font) opening a
   panel with just the on/off switch + explanation.
3. **Auto-tone fix** — `paletteFor`'s automatic per-category rotation now
   cycles ONLY the always-available base tones (`unlockLevel == null`);
   a level-locked premium tone never shows up automatically — it only
   appears when explicitly picked in the Tone tool (the override index still
   maps into the unlocked pool).
4. **Quick fact + Reading progress stacking** — like the custom fact, the
   QUICK fact now stacks under the progress bar: `chapterFactForCard`
   returns `editedFact ?: quick.text` when the quick fact is active and
   progress is on, and the content pills keep progress ON when picking the
   Quick fact (previously they turned it off).

## Notes for next request

- CI will compile-check (no Gradle here). Riskiest spots: the `smartAutoFitDelta`
  signature change (now 4 args — call sites in TopicShareCard itself + the 3
  ArrangeableCard sites updated), the `ShareAutoFitDelta` field renames
  (dy/titleDy/titleScale/factScale → widthFrac/textScale) through
  effectiveMove/effectiveBodyScale + the first-grab seed, and the removed
  `autoFitIntensity` (persist/parse + panel). Braces verified balanced with
  a template-aware tokenizer.
- Changelog (fastlane 20260921.txt) + app/AGENTS.md v374 bullet added.
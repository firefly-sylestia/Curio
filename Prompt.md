# Prompt.md — current request log

## Request: Share-card editor — precise sliders, accurate typing, Editorial wrapping

User request (verbatim):

> "the shar card hegh and width editing with sliders are bad, like i cant know which
> im editing, and the sliders are not very precise, so fix that, and also when typing
> the cursor and position its actually typing is accurate too so fix that, and in
> editorial text wrapping for quick facts, well only do 2 lines alongside the first
> letter, and start from the same position of the top, so its much better."

### What shipped (this turn, all in `ui/components/TopicShareCard.kt`)

1. **Sliders — explicit labels + readout + snap steps.** Edit mode previously showed
   TWO sliders whose labels swapped between "Title"/"Fact" based on whichever box was
   selected — with no value readout and a loose continuous drag. Now FOUR sliders are
   always visible — **Title width / Title height / Fact width / Fact height** — each
   with its own label, a live percent readout (`SizeSliderColumn` prints e.g. "Title
   width · 78%"), and snap steps (width 1%, height ~5% since they drive whole-line
   counts). Selection state still drives the box outlines/handles on the card.

2. **Typing caret accuracy.** The transparent quick-fact `BasicTextField` had a 6dp
   content inset AND typed with `bodyMedium` (14sp/20sp) while the card renders its
   fact in Lora ~9–12sp with ~1.5× leading — so the caret drifted off the visible
   text and typing landed on different lines/wraps. It now types with **card-matching
   Lora metrics** (`factFieldStyle` in TopicShareSheet: Lora ~11sp × bodyScale × 1.5
   leading, transparent color) and **no padding**, so it sits exactly where the card's
   fact renders and the caret/line-wraps track the visible text.

3. **Editorial drop cap = 2 wrap lines, top-aligned.** The drop-cap initial was a
   measured 3-line wrap with a 3×-sized letter. Per request it is now a **2-line
   wrap** (wrap measure + wrap Text maxLines 3 → 2, initial fontSize 3× → 2×), and
   `LineHeightStyle.Alignment.Top` + `Trim.None` pins the letter to the TOP of its
   2-line box so it starts level with the first text line (a 2× line height alone
   vertically centers the glyph).

### Docs

- Changelog (`fastlane/metadata/android/en-US/changelogs/20260921.txt`) — 3 new FIX
  bullets on top.
- `app/AGENTS.md` — updated the v228 Editorial drop-cap description (2-line wrap +
  top alignment) and added a `v228b` bullet covering the three changes.

### Verification

- Braces balanced (931/931); imports audited (`LineHeightStyle` added; the pre-edit
  file already had `LoraFontFamily` + `roundToInt`; `Slider`/`BasicTextField` already
  imported).
- `SizeSliderColumn` takes a `modifier` param — `Modifier.weight(1f)` is applied at
  the Row call sites (it has no RowScope receiver at the helper's top level).
- CI compiles on push (no Gradle in this environment).
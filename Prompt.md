# Prompt Log — current request

## Request (2026-09-06, active)

"the corner expand button it doesnt expand the whole box its only behaving
as the width or hight button not as that enlarge for the whole box along
with the text, also remove the selected outline when editing in inline the
handle and that box hihglith, also add a full screen button for the share
card with just text format and text editing features in full screen with
only one pill and drop down style… the background color would be category
tint. analyze the feature and ask me with suggestions what i wanna do"
(answers: box + text zoom together; hide chrome in TEXT-edit mode only —
handle + highlight return when exiting; full screen shows the card itself
large with floating tools, whole-text per-element formatting like the sheet
already has, ALL text tools in ONE menu).

## Implemented (this turn, commit pending)

1. **Corner drag = true ZOOM** — `ShareCardMove.factZoom` (0.5–4×): the
   fact's corner grip scales the box AND the fact font together
   (photo-zoom); `effectiveBodyScale` includes `move.factZoom` so preview,
   export and typing caret match; persisted per style, cleared by Reset,
   counts as "touched" so auto-fit hands over.
2. **Selection chrome hides while typing** — `factEditMode` makes the fact
   border transparent, skips the tap-to-select layer and the FACT case
   hides MoveHandle + CornerResizeHandle; chrome returns when editing ends.
3. **Full-screen editor** — Full screen pill next to Customise opens a
   full-display `Dialog` on a category-tint wash
   (`lerp(surface, accent, 0.12f)`); the card renders LARGE centered via
   the same ArrangeableCard/TopicShareCard pair (export == preview); one
   Text pill → single DropdownMenu with every text tool: font, size
   slider, B/I/U + highlight swatches, alignment, fact format + drop-cap.
4. **Rich-text-lite** — `factUnderline/factHighlight/titleUnderline/
   titleHighlight` (Color?) applied in factBodyStyle/titleStyle
   (`TextDecoration`, `background`), persisted with the move.

## Earlier in this thread (pushed: 2c2502e0, 66b07a62, 2707bdb3)

- Fact formats (Condensed / Book page / Editorial drop-cap) + writing box
  with Enlarge; album/series cover fetch in the share editor; chapter
  note → Share as chapter review; collision-push fact drag; album fav
  Whole-box slider; real cover colours (pixel-vote histogram, dominant
  wash, resolved artwork URL for albums/series); expanded chapter row
  tinted; Google Books removed as cover source (ratings/ISBN stay).
- Auto-adjuster: `factScale` fact-font auto-shrink (~150 chars up),
  Balanced curve starts at 90 chars, handoff seed, Reset restores.

## Notes for next request

- CI will compile-check (no Gradle here). Riskiest spots this round: the
  full-screen Dialog block (imports verified: DropdownMenu, BorderStroke,
  CircleShape, toArgb, TextDecoration; `Color.lerp` corrected to top-level
  `androidx.compose.ui.graphics.lerp`), the `factZoom` plumbing through
  parse/persist, and the `factEditMode` chrome gating. Braces verified
  balanced via a tokenizer (raw-string `"image/*"` breaks naive checkers).
- Changelog (fastlane 20260921.txt) + app/AGENTS.md v372 bullet updated.
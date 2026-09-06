# Prompt Log — current request

## Request (2026-09-06, active)

"starting with share card, so in share card the book cover size well in this
commit [ea47f1b] the commit itself doesn't target that size change but you can
see and use it, the size of the book cover was perfect in share card in that
commit, so i want you to fix it and take that size, and for album cover well
album covers are square not rectangular and its stretching it to rectangular so
fix that too and take the similar size again and same in series. next the full
screen button it looks transparent and doesnt match the customise button look so
fix that and also keep the full screen button when editing too in customise, and
then inside the full screen edit the share card preview well it looks stretched
and not accurate of what it was looking before in the bottom sheet, the text
size placements etc that isnt accurate, also in full screen add the dimension
change button. and in full screen add the box size editor too. next in the crop
size change the whole box should not depend on the width or height its separate
and independent. so fix that."

(ask_user answers: keep the side layout with the SMALL cover (44×66 book/series,
66×66 square album); series covers stay 2:3 like books.)

## Implemented (v373, commit pending)

1. **Cover sizes + no album stretch** — `TopicShareCard` gained
   `isSquareCover` (the sheet passes `isAlbumTopic` at all 5 call sites incl.
   the Save/Share exports): books/series render the 2:3 jacket at the old
   perfect 44×66, albums render square 66×66 (no more stretching square art
   into the 92×136 rectangle). The side layout stays, but the title shift /
   width crop / title shrink are now DERIVED from the cover's real width
   (was the fixed 108f/0.74/0.9 for the 92dp cover) so the smaller jacket
   hugs the title instead of leaving a void.
2. **Whole-box independence** — `ShareCardMove` gained
   `titleBoxScale` / `factBoxScale` / `favBoxScale` (1f default, persisted +
   parsed). `TopicShareCard` applies them in a new `boxScaledMove` (multiplies
   both width AND height fractions on top of auto-fit), the three "Whole box"
   sliders now bind to their OWN value (dragging width/height no longer yanks
   the thumb), the corner-grip base math divides by the scale, and the scales
   count as "touched" so auto-fit hands over. Reset clears them (moves wipe).
3. **Full screen button** — restyled to match Customise exactly
   (surfaceContainerHigh + onSurfaceVariant instead of the secondaryContainer
   chip that read as transparent) and now stays visible while editing.
4. **Full-screen preview accuracy** — the card now renders at the sheet's own
   280dp base and is zoomed through a scaled `Density`
   (`CompositionLocalProvider(LocalDensity provides ...)`) so text sizes,
   spacing and placements scale TOGETHER — an exact zoom of the bottom-sheet
   card (the old approach laid the dp content out in a much bigger box, so
   text stayed tiny and SpaceBetween re-spread the layout). Drag deltas convert
   through the same scaled density, so persisted offsets stay card-local.
5. **Full screen tools** — a Dimensions pill (AspectRatio icon + live 3:4/9:16
   label) sits next to the Text pill and toggles the aspect; the Text dropdown
   gained a Box size section (width / height / whole-box sliders for the
   selected title or fact) and the menu is now vertically scrollable.

## Notes for next request

- CI will compile-check (no Gradle here). Riskiest spots: the scaled-Density
  full-screen block (fully-qualified LocalDensity/Density/
  CompositionLocalProvider — no new imports added), the extra closing brace
  for the new `CompositionLocalProvider`, the dropdown scroll, and the
  `isSquareCover` param threading through all 5 TopicShareCard call sites.
  Braces verified balanced with a template-aware tokenizer (naive brace
  counting trips on `${...}` string templates).
- Changelog (fastlane 20260921.txt) + app/AGENTS.md v373 bullet added.
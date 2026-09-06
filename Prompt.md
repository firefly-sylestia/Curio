# Prompt Log — current request

## Request (2026-09-06, active)

"the cover position isnt right in share card, in paper design its top left
corner and not to the side of the title and author and also the space can be
decrase, in vinyls the text quick fact should move a little down and the
cover for albumn and the title itself should be a little down too do it
doesnt verlap on category pill, in editorial its near perfect, the cover
should move a little don matching the title starting point and the title can
move a little closer to the cover, same for minimal too. and for clean a
similiar the positioning is good the ittle move closer, and then the cover
and albumn should move together also when the quick fact gets moved along
with title the cover should move too by collide logic i think while keeping
the positio with the title, and also in the auto text the collision should
work when the height of the quick fact gets tuned manually and the title
moves up automatically along with cover if present and it comes don if with
the slider it gets lowered and same for width chnages too, so apply it an
dmak eit more better and smart."

Ask answer: "Glue cover to the title block; title drag moves cover too and
also the title move during collision moves it too; Saved lift".

Also noted by user: watch CI after push (deferred until everything is done).

## Implemented (v376)

1. **GLUED covers (Paper / Vinyl / Clean / Editorial / Minimal).**
   `TopicShareCard` now computes `glueCoverStyle` + `gluedCover` and passes
   the artwork (with natural `coverW`/`coverH` — 44×66 jacket, 66×66 album)
   into those five style cards. The old side-layout overlay + title-dx shift
   stays only for Signature/Custom. New `GluedCover` composable renders the
   jacket INSIDE each style's title block as the leading item of the Row
   that carries the title's move (`glueTitleMove` = drag offset + auto lift;
   `titleSize` = font scale + width crop applied to the TEXT ONLY), so the
   cover sits exactly beside the title at the design's natural flow spot and
   rides every title drag / collision push / auto lift. The cover keeps its
   own fine-position offset (`coverDx`/`coverDy`) inside the group.
2. **Per-style placement pass.**
   - Paper (MiddleContent): cover | title+author Row beside the headline
     area, snug `CoverTitleGap` (12dp).
   - Vinyl: cover | title+byline Row; title block lowered (18dp spacer with
     cover) so it clears the category pill; quick fact nudged down (14dp
     spacer with cover).
   - Editorial: cover | headline+deck Row aligned to the headline start
     (topPad 5).
   - Minimal: cover | title+byline Row (topPad 4).
   - Clean (Neumorphic): cover beside the CENTERED title block
     (CenterStart, CenterVertically).
3. **Auto title-lift collision (`move.titleLift`, dp, saved).**
   In `ArrangeableCard`'s edit overlay, a `LaunchedEffect` (editMode,
   non-quote) watches measured title/fact rects + fact-box fractions; when a
   manually grown fact box (height / width / whole-box sliders or the corner
   grip) would draw over the title, it computes the needed lift from the
   title's UN-lifted base (measured bottom + current lift → one-step
   convergence), clamps to the card top, and writes it into the move via
   `onMove`. `moveTitle`/`titleShift`/`glueTitleMove` apply the lift as an
   upward offset everywhere (sheet preview + export); `titleLift` is parsed
   and persisted in the move JSON. Lowering the box drops the needed lift →
   the title settles back. TITLE drag clamp adds the lift back to keep the
   natural base correct.

## Notes for next request / CI

- CI will compile-check on push (no Gradle in this environment). Watch for
  the `glueTitleMove`/`titleSize`/`GluedCover` wiring and the
  `ArrangeableCard` LaunchedEffect (scaled-density rects go through
  `editDensity`).
- Braces verified balanced in TopicShareCard.kt with a template-aware
  tokenizer; per-hunk diff nets all zero.
- Changelog (fastlane 20260921.txt) + app/AGENTS.md v376 bullet added.
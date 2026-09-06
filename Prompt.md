# Request Log — share-card smart auto-fit + corner whole-box scale

## Status: done (pushed)

## The request (user)
Share-card editor: (1) whole-box size adjustment from the box's corner —
"i know i can adjust width and height but not that the whole box size
itself… like it can expand more"; (2) smart auto-adjust default ON so
adding a long custom fact auto-adjusts; (3) the smart adjuster is not
refined: Collage long quick facts overlap the category pill, the fact
doesn't expand (text stays tiny), spacing between fact and title too much,
and the title goes OUT of the card; Clean — title can go a little higher
and the fact won't overlap; Editorial — title goes above the line and
overlaps, sometimes overlapping the author/year; Minimal same; elements
don't consider space between each other, and the title doesn't shrink when
it doesn't have the space. "Make the auto smart adjuster more smart so
user will have 0 need to manually edit things."

## Analysis (current state at fbd05a0c)
- The smart auto-fit (`shareAutoFitDelta`, v369) applied ONE blind curve to
  every style: `dy = -(h-1)*16` nudged the fact AND title up regardless of
  what sits above them — the Collage pill (fact overlapped it), the
  Editorial masthead/byline (title/fact overlapped them), and the card top
  (Collage title went off-card). That is the root of every listed bug.
- Fact height growth only raises `maxLines`; the fact text sits in bounded
  in-flow containers, so expansion is the container's remaining space — the
  fix is bigger long-text fonts + per-style room management, not more
  maxLines.
- CornerResizeHandle already existed for title/fact but NOT meta/fav; width
  is physically capped at the card width, so "expand" must come from height
  growth + font floors. The fact sliders topped out at 5x (grip 8x).
- Auto-fit only watched the QUICK fact (`editedFact ?: factText`) — a long
  stacked custom fact (`chapterFact`) never triggered it.
- Export path (`ShareCardCapture.kt`) renders the same composable and draws
  after ONE layout pass → auto-fit must stay a deterministic function of
  (move, lengths, style, aspect), no measure-feedback loop.

## Implementation (TopicShareCard.kt)
- `smartAutoFitDelta(move, factLen, titleLen, style, aspect)` replaces
  `shareAutoFitDelta`: same curve (Balanced/Compact/Airy via
  `autoFitIntensity`), same default-ON pref + "manual edits win", but now
  per-style CLAMPED:
  - Collage: factUp 0 / titleUp 0 (pill above fact, title at the edge).
  - Editorial: 0 / 0 (masthead + byline) — fact grows down to colophon.
  - Clean (Neumorphic): factUp 0, titleUp ≤28/24 (bottom-anchored fact
    grows up; title lifts to clear it).
  - Minimal: factUp 0, titleUp ≤22/18.
  - Paper/Vinyl/Signature/Custom: modest ≤14/12 both.
  - `titleTouched` → no auto title lift/shrink (per-box manual wins).
- `autoTitleScale(style, len)`: long-title shrink 0.70–0.95 per style,
  only while auto-fit is active; gated ≤18 chars no-op.
- `factLen = maxOf((editedFact ?: factText).length, chapterFact.length)` —
  long custom facts auto-fit too. `titleLen` from the shown display.
- effectiveMove now applies `titleDy` + `titleScale` (was: title followed
  the fact's dy blindly); meta still follows the fact nudge.
- First-grab seed (ArrangeableCard) transfers titleDy + titleScale too, so
  the box never jumps when auto-fit hands off.
- Corner whole-box grip added to META (metaWidth/HeightFrac) and
  FAVTRACKS (favWidth/HeightFrac); fact "Fact height" + "Whole box"
  sliders raised 0.35..5 → 0.35..6 (steps 56).
- Long-text font floors raised: Collage 10/11/12 → 10.5/11/11.5/12.5,
  Clean 8/9/9.5/10.5 → 8.5/9.5/10/11, Editorial 8.5/9.5/10/11 →
  9/9.5/10.5/11.5, Minimal 8.5/9.5/10.5/11.5 → 9/9.5/11/12.

## Docs / release notes
- app/AGENTS.md: v370 bullet added (smart auto-fit clamps, corner grip
  coverage, font floors).
- fastlane changelog 20260921.txt: 3 FIX bullets (per-style auto-fit,
  custom-fact auto-fit + fonts + 6x sliders, corner grip on info row +
  fav strip).

## Followup after push
Ask the user to test the share-card editor on the 4 called-out styles
(Collage / Clean / Editorial / Minimal) with long quick + custom facts and
the corner grip, then fix any remaining overlap.
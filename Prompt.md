# Request Log — share-card fixes: Minimal fact rule moves, Collage/Signature favorite-tracks strip

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user)
"for the minimal card style theres a line above the quick fact or custom
fact which should move along with the move, also the favrptire tracks box
isnt matching in collage, and signature styles so fix it, and properly
place them so that they dont overlap"

Three parts:
1. Minimal card: the accent rule above the quick/custom fact must move
   with the fact box (it currently drifts).
2. Collage + Signature: the favorite-tracks strip "isn't matching" the
   card's design.
3. Proper placement so the strip doesn't overlap other elements.

## What was done
### TopicShareCard.kt
- **Minimal fact group (v359):** the thick accent rule, the 16dp gap and
  the fact text are now ONE `Column` under `moveFact(move)` (previously
  the rule used `factShift(move)` separately from the text's `moveFact`,
  so a drag could desync them). The rule now travels with the box on drag
  and stays glued while the box resizes.
- **Collage strip (v359):** the collage's bottom is a dark band under the
  torn seam, so the strip's tokens flipped from the warm-white tone box to
  a translucent DARK slip — black 34% bg + white hairline border + white
  serif type + the polaroid's gold-tape heart. Reads on the band AND
  anywhere the user drags it.
- **Signature/Custom strip (v359):** backgrounds vary per category
  (paper-white and dark scenes alike), so the strip is now a dark stamp
  pill — black 55% + white border + white type + accent heart — instead
  of a tone-palette box that clashed with the card's own colors.
- **Placement (v359):** Collage raised 40/36dp → 84/80dp bottom;
  Signature/Custom raised 62/58dp → 84/80dp bottom (classic/normal) so
  the strip clears the collage's torn-seam footer wave and the signature
  footer line. Still movable via the editor's F-handle.

### Docs
- app/AGENTS.md: **v359** entry (after v358).
- fastlane changelog 20260921.txt: FIX bullet at the top.
- Prompt.md: this log.

## Verification
- Braces balanced (checked with grep counts). `factShift` still used by
  the Paper + Editorial rules, so no dead code. The Collage footer wave
  and Signature footer sit at ~22dp bottom padding with the strip default
  at 80dp+ — no overlap. CI will validate the real compile.
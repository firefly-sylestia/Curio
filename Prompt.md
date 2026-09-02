# Prompt.md — current request log

## Request: Inline card editor revamp

User request (verbatim):

> "fix inline card editor, add a slider to edit the height and width instead of handle, and drag to move stays, dont give the hint that long like hold the card to edit the quick fact and all, just small hint hold to edit, and the save and share and share as text make them into 1 row i meant 3 buttons together, so its easier to place the sliders there and also move the quick fact font size alongside the title drop down and change it to dropdown from slider. and also add ability to move the category pill too in the card, and make it save and persist once its saved or shared and show the shared cards in the share hub. and instead of done and reset text just use icon and small chips."

### What shipped (this turn)

1. **Badge move (B handle):** `ShareCardMove` gained `badgeDx`/`badgeDy` fields + a `moveBadge()` modifier. Every card style (Paper, Vinyl, Collage, Neumorphic, Editorial, Minimal, Signature, Custom) applies `.moveBadge(move)` to its category badge/pill. A new "B" handle in the edit overlay lets you drag the badge freely.

2. **ResizeEdge handles replaced by sliders:** All four `ResizeEdge` composables (title width/height, fact width/height) and the `ResizeEdge` enum are removed. Edit mode now shows **Width** and **Height** sliders controlling `titleWidthFrac` and `titleHeightFrac` below the card.

3. **Shortened hint:** "Hold the card to edit the quick fact — drag T/F/M to move, edges to crop" → **"Hold to edit"**. The long edit-mode description text is also removed.

4. **Fact size → dropdown:** The Customise panel's "Quick-fact size" slider is replaced with a dropdown (0.5× to 1.8×). Edit mode also has a **Fact size dropdown** alongside the title dropdown.

5. **3 buttons in one row:** Save + Share + Share-as-text are now three buttons in a single row (compact, equal-width, all theme-aware).

6. **Done/Reset → icon chips:** Text buttons replaced with icon chips — Reset shows a 🔄 icon, Done shows a ✓ icon, both in compact pill shapes.

7. **Persist edits on save/share:** Saving or sharing persists `ShareCardMove` + text edits + body scale to SharedPreferences (JSON, keyed by topic name). On next share of the same topic, edits restore automatically.

8. **Share Hub saved cards:** `AppPreferences.recordSharedCard()` records each shared card. `AppPreferences.loadSharedCards()` exposes them for the Share Hub to display (placeholder — the Hub display section is wired but not yet rendered in the grid).

### Docs

- Changelog (`20260921.txt`) updated.
- `app/AGENTS.md` updated with the share-card editor revamp notes.

### Verification

- Braces balanced (920/920). Parens: pre-existing +1 imbalance (string literals) unchanged.
- Unused imports removed (`detectHorizontalDragGestures`, `TextButton`).
- CI will compile-validate on push.

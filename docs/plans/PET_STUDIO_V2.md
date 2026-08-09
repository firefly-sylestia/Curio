# Pet Studio v2 — Editor, Pet, Home, Behavior Overhaul

User direction (ask_user, 2026-08-09):
- **Do everything**, in this order: Editor rebuild → Settings overhaul → Default pet + home upgrade → Pet behavior & interactions.
- Keep **Pets as the default studio tab** (the "editor isn't first" complaint is about the editor not reflecting frame changes, not the tab order).
- Default pet becomes **32×32 with fully redrawn art** — proper ears, fluffy tail, hands, blush; sleep pose and animations redrawn; more accessories.
- The typing keyboard is NOT removed — rebuilt as a **real small premium keyboard with a proper hand** pressing keys in the right position.

## Phase 1 — Editor rebuild
- Animation frame editing stays IN the editor: the timeline's big preview must live-track the selected frame (zoom + rotate visible), and the pixel grid edits the selected frame's body/curled/eyes layers. No separate player required to edit frames.
- Face editor: per-mood blueprint (the actual face drawn on the pet) + eyes/mouth editable per face + hand-drawn eye layer.
- Detail layers (tail/accessories/effects/antenna): each body part is its own layer, POSITION-adjustable (nudge arrows + on-canvas drag) via a new `detailOffsets` map, with the live placement preview center-stage.
- Zoom on every pixel grid (1×/2×/3×).
- No duplicate actions; one Save path (the toolbar).

## Phase 2 — Settings overhaul
- Dialog lines editor per reaction event (lines already exist in the model — surface them).
- Belly: drawn belly layer + visibility toggle.
- Accessories: add/remove flowers & props from the Settings page (AccessoriesDialog).
- Personality presets (PetFacePresets: Shy/Party/Sleepyhead) surface in Settings.
- More emotions (SHY, GRUMPY, PLAYFUL) across PetFaceMoods + CurioPet.Mood + DEFAULT_FACES.

## Phase 3 — Default pet + home upgrade
- `DEFAULT_GRID_SIZE` → 32; new 32×32 default body (ears, tail, hands, blush, fluff), curled sleep pose, redrawn built-in animations; a default drawn `accessories` flower layer so the flower is editable.
- Home flower bed: richer 32×32-feel diorama, real night adaptation (stars/lamp/moon driven by `CurioPet.timeOfDay()`), home editor (follow-up phase if budget runs out).

## Phase 4 — Pet behavior & interactions
- Slow the reaction hop / mood transitions (calmer, more premium).
- Rebuild `TypingKeyboard` into a real mini keyboard with a paw hand pressing keys.
- More dialogue lines + more default actions (TOUCH / PLAY / LEVEL_UP event lines).

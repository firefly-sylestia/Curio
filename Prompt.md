# Prompt — Curio request log

## Active request: v8.8 pet upgrade — touchable, floating, wandering pet

User asked: improve the pet AI — touch-interactive, movable anywhere on screen,
one default look (no theme adaptation), more detailed and cuter, proper idle +
dynamic animations, a smart pet that moves around, can be dragged, and reacts.

User decisions (ask_user): **floating pet behind a Settings toggle, default ON** ·
the floating pet **replaces the old spots**, and the old pet places become a
**cute pixel flower bed** — the pet sleeps there when the app opens and stays
asleep until tapped.

### Delivered (uncommitted until commit+push)

- **`AppPreferences`**: `floatingPetEnabledState` (default ON) + seed + setter —
  new "Floating pet" toggle in Appearance next to "Curio pet".
- **`CurioPet` brain**: `awake` in-memory state + `wake()` / `settleToSleep()`
  (resets each launch — the pet is asleep in bed when the app opens); dialogue
  variety arrays per mood + `touchReaction()` burst lines ("Boop!", "Hehe!").
- **`CurioPetSprite` rewritten**: ONE fixed look on every theme (cream body +
  warm ink, no dark-mode twin); richer pixel grid (ear nubs, feet) + belly patch
  and waggy tail as Canvas overlays; new params `moving`, `dragged`, `facing`,
  `squishKey`; new animations — fast walk bob with lean, dragged stretch with
  wide startled eyes, one-shot touch squish, curious head tilt; kept idle
  bob/blink, sleep breathing + Z's, excited wiggle + star eyes, proud face,
  stage accessories (sprout/satchel/book/aura/halo). Flip layer via
  `graphicsLayer scaleX`.
- **`CurioFlowerBed` (new)**: pixel wooden bed, flower pillow, grass base,
  soft shadow; pet inside asleep (with Z's) or sitting awake; tap to wake.
- **`CurioFloatingPet` (new)**: global overlay rendered at NavHost root (over
  the bottom bar too). Wanders autonomously to random spots (disabled under
  reduced-motion), draggable anywhere with edge clamping + lift-stretch pose,
  tap → squish + reaction bubble, mood-flip → celebration hop + line, auto-nap
  back to bed after 8 min idle (app activity refreshes the timer), entrance
  pop. Touch plumbing only on the pet element — the overlay never blocks the
  UI beneath.
- **Hero card (Quests)** + **Home corner**: sprite replaced by the flower bed
  (pet inside when asleep or when floating is off; vacant when out). Tapping
  wakes the pet; when awake, tapping the hero bed opens the check-in dialog.
- **NavHost**: hosts `CurioFloatingPet` with the recommended lane's accent.
- **Settings**: "Floating pet" toggle row.

### Validation
- String-aware balance ALL OK on the 9 touched files; `git diff --check` clean.
- Code review applied: reaction bubble lifted ABOVE the pet (was overlapping
  the head — padding can't be used to move it, so the box is offset up in px +
  bottom-aligned), infinite-animation specs hoisted with `remember(moving)` so
  recompositions don't restart the loops, nap timer now refreshed by app
  activity (mood celebrations), wander loop keyed on `CurioPet.awake` so it
  restarts on wake.
- CI validates the real build on push.

### Notes / open
- The floating pet draws above the tour pill if they overlap (pet is draggable
  out of the way) — acceptable.
- Bubble clamps x to avoid off-screen overflow.

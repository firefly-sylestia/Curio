# Prompt — Curio request log

## Active request: v8.11 — smarter, more playful, natural pet

User asked (after v8.10 polish): increase the reaction texts when interacting,
increase the animations, make it show a reaction for a second then go idle,
make it smart and playful — a natural pet that sometimes reacts to touch and
wants to play — and make its movement smart too. ("Full proper research and
do it" — grounded in virtual-pet behavior research: escalating touch tiers,
anticipation signals, playful darts, unprompted play initiation.)

### Delivered (commit pending)
- **`CurioPet` brain**: `touchReaction(tier)` with escalating pools — tier 1
  soft boops ("Boop!", "You found me!"…), tier 2 playful ("Zoomies!",
  "Tag — you're it!"…), tier 3+ zoomies ("Wheeee—!", "Spinny!"…); new
  `playInitiation()` lines for when the pet starts a game itself.
- **`CurioPetSprite`**: new `playKey` + `spinKey` one-shots — a play-bow
  (dips down, springs up with a bounce + squeeze) and a full 360° twirl
  (rotation + scale pulse); mid-play face (STAR eyes, WIDE open smile) and a
  faster tail wag. Dragged-still-wins the startled face if grabbed mid-play.
- **`CurioFloatingPet`**: touch escalation — rapid taps (within 1.6s) push
  boop → play-bow → zoomies spin with matching lines + hearts; after a tap
  the pet queues a playful dart to a nearby spot that the wander loop answers
  within ~200ms (fast dash); ~12% of wander cycles the pet initiates play on
  its own (bow + "Catch me!" + zoom); wander y is downward-biased to stay
  grounded, with think pauses before/after walks; reaction bubble shows for
  1.5s then it settles back to idle; drag no longer busy-spins the loop
  (300ms pause), darts aren't queued while watching the Spin deck, long-press
  resets the tap streak before going home.

### Validation
- String-aware balance ALL OK; `git diff --check` clean; `touchReaction`
  caller updated (sole caller); sprite params defaulted so the bed/dialog/
  banner callers are unaffected.
- Code review applied: face-override order (dragged wins), no dart while
  watching, streak reset on long-press. No compile risks flagged (local
  suspend walkTo is valid Kotlin; Dp/Float math type-correct).
- CI validates the real build on push.

---

## History (committed)

### v8.10 — pet polish: one coral theme, happy smile, chubby, send home, no duplicate pet (`eb5820e`)
- ONE fixed color everywhere — sprite scarf/aura always the Curio light-theme
  brand coral (no category pastels, no dark-mode twin); accent params removed
  from Sprite/FlowerBed/FloatingPet/HeroCard + NavHost accent computation.
- Happy face: the smile mouth was upside down (a frown) — flipped; excited
  mouth is now a wide open smile. Chubby body rows widened.
- Removed the round glow disc behind the floating pet + the round XP ring
  around the hero bed.
- Long-press the floater fades it out and sits it in its bed (`atHome` +
  `goHome`/`comeOut`); beds show the pet sitting until tapped to come out.
- `CurioPet.dialogOpen` hides the floater while the check-in dialog is open —
  never two pets on screen.

### v8.10 (quest) — discovery daily completes on spinning the target lane (`fe9c336`)
- `CurioQuests.onSpin(context, categoryId)`; spinning the passport's
  least-engaged lane (Wildcard included) completes the "New lane" daily at
  spin time — the Wildcard merge could never complete via topic-open.

### v8.9 — pet: extra idle behaviors, per-screen reactions, cuter sprite (`5e5a4f6`)
### v8.8 — touchable floating pet + flower bed (`f1869d6` + CI fixes)

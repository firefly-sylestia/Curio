# Request — v8.16: smarter pet landmark interactions + auto-open topic-reveal toggle

## What the user asked

1. Make the pet's animation/wander **smarter, not just random** — it should interact
   with things on screen (the spin button, the profile icon, headings/text) and
   those things should **react** when it interacts with them — WITHOUT changing the
   current layout of any screen.
2. The pet's movement should adapt to what it's approaching.
3. Add a toggle to NOT auto-open the topic reveal after the deck lands; when the
   toggle is OFF, the "open it" dialogs/tour prompts should be off too. Default: OFF.

## Analysis

### Pet landmarks (new system)
- The floating pet wandered to purely random points every beat — it never noticed
  the UI around it, and UI never reacted to it.
- Plan: a landmark registry — screens publish a few "interesting things" (bounds
  only, via `onGloballyPositioned`, ZERO layout impact) and the pet's wander loop
  occasionally targets one, walks over with a kind-adapted gait, pokes it, and the
  thing springs a beat (`graphicsLayer` scale pulse).
- Screen-scoped: landmarks keyed by route prefix ("home", "spin", "profile"), so
  navigating clears one screen's set and the pet only ever sees the current
  screen's things. Registration is snapshot-state, so the overlay recomposes.

### Auto-open toggle
- `SpinScreen` auto-navigated to the reveal in the settle effect (line ~774-798),
  and the First Journey tour step "Open the landed topic" said "It's already open"
  — a contradiction the user saw as an "open it open it dialog".
- Plan: `AppPreferences.autoOpenRevealState` (KEY_AUTO_OPEN_REVEAL, **default
  FALSE**) + a Settings → Appearance row. When OFF, the deck lands and the front
  card stays tappable (manual `onDeckCardTap` already existed — that's the manual
  open path). Tour step 3/4 copy adapts to the toggle.

## Changes (9 files)

| File | Change |
| --- | --- |
| `ui/pet/PetLandmarks.kt` | NEW — landmark registry (`Kind.FUN`/`CURIOUS`, upsert/remove/poke, `forScreen`) + `PetLandmark` composable (bounds tracking + poke pulse, zero layout impact) |
| `data/CurioPet.kt` | `landmarkLine(funThing)` — 16 cute reaction lines for landmark pokes |
| `ui/pet/CurioFloatingPet.kt` | Wander loop: 45% chance targets a FUN/CURIOUS landmark instead of a random point; FUN = eager quick steps + boop + hearts; CURIOUS = slow tiptoe + read-tilt + poke; gated on `!CurioPet.spinning` so the pet stays glued while the deck reels |
| `features/home/HomeScreen.kt` | Greeting wrapped in `PetLandmark("greeting", CURIOUS, "home")` |
| `features/profile/ProfileScreen.kt` | Avatar wrapped in `PetLandmark("avatar", FUN, "profile")` |
| `features/spin/SpinScreen.kt` | Shuffle CTA wrapped in `PetLandmark("spin", FUN, "spin")`; auto-open gated on `AppPreferences.autoOpenRevealState` (manual tap path unchanged) |
| `data/AppPreferences.kt` | `KEY_AUTO_OPEN_REVEAL` + getter/setter + `autoOpenRevealState` seed (default off) |
| `features/settings/SettingsSectionScreen.kt` | Appearance row "Auto-open landed topic" |
| `data/QuestGuide.kt` | Tour step 3/4 copy conditional on `autoOpenRevealState` (no more "It's already open" when auto-open is off) |

## Review fixes applied

1. **Landmark block ran during an active spin** — the landmark block preceded the
   `watching` gate, so the pet could wander off mid-reel. Gated with
   `!CurioPet.spinning` (SpinScreen flips it at 684/728). Reviewer #2 concern
   (second revealFor at ~845) verified SAFE — that's the manual `onDeckCardTap`
   path and must stay ungated.
2. **Tour copy assumed auto-open** — now conditional; with the toggle OFF the tour
   says "Tap the card to open the teaser".
3. **Pet could hyper-boops the spin button** — on the Spin screen the wander
   beat loops every ~300ms (the watching gate exits the wait loop early), so
   the 45% landmark roll re-rolled constantly → a poke roughly every second.
   Added a 4s `lastPokeAt` cooldown in `CurioFloatingPet` so pokes stay
   occasional even where the beat loop cycles fast.
4. **"Open it, open it!" contradicted auto-open** — the pet's REVEAL_OPEN cheer
   said "Open it, open it!" right as the reveal opened BY ITSELF. The line now
   adapts to `autoOpenRevealState` (auto-open ON → "There it is!" / "It opened
   itself!"; OFF → the eager "Open it!" cheer stays for the manual tap).
5. **`upsert` churn** — `onGloballyPositioned` fires every layout pass; upsert
   now compare-and-sets (data-class equality) so unchanged landmarks never
   rewrite the map.

## Validation

- Brace balance ALL OK (9 files), `git diff --check` clean.
- Reviewer (code-reviewer-deepseek-flash) passed after the fixes above; the
  remaining notes (landmark pulse on screen re-entry — cosmetic, accepted).

## Completion summary

v8.16 shipped: landmark interactions (pet pokes things, things react, movement
adapts to kind, screen-scoped, zero layout impact, poke cooldown) + Auto-open
landed topic toggle (Settings → Appearance, default OFF; tour copy + pet
REVEAL_OPEN line adapt). Pushed to Alpha.

# Request — v8.17: PLAY landmark kind (the pet's jig)

Follow-up to v8.16 (smarter pet landmark interactions + auto-open topic
toggle, `60ddec7`, CI-fixed in `927276f`).

## What the user asked

Add a third landmark kind — a special "tappable" spot where the pet does a
little jig / play animation when it pokes it.

## Analysis

- v8.16's landmark system has `Kind.FUN` (boop a gadget) and `Kind.CURIOUS`
  (read some text). A third kind needs a distinct gait + reaction, one
  "special spot" wired into a screen, and a matching line.
- The pet's own FLOWER BED on Home is the perfect special spot: when the pet
  is awake and floating, the bed sits vacant, so the pet dashing home and
  dancing at its own bed is charming and self-consistent (the overlay is
  hidden while the pet is asleep/at home, so it never jigs at an occupied
  bed).

## Changes (4 files)

| File | Change |
| --- | --- |
| `ui/pet/PetLandmarks.kt` | `Kind.PLAY` — a special spot: eager dash + poke + jig |
| `data/CurioPet.kt` | `jigLine()` — 9 dance-y reaction lines ("Tippy tap tap!", "Dance break!", …) |
| `ui/pet/CurioFloatingPet.kt` | PLAY branch in the landmark poke `when`: eager walk (15ms steps) → poke (spot springs a beat) → squish bounce → play-bow → celebration hop + twirl → hearts + `jigLine()` bubble. The `when` is now exhaustive (FUN/CURIOUS/PLAY). |
| `features/home/HomeScreen.kt` | Flower bed wrapped in `PetLandmark("bed", PLAY, "home")` — the landmark modifier is handed to `CurioFlowerBed`'s root `modifier` (bounds-only, zero layout impact; bed keeps its own tap-to-wake). |

## Review fixes applied

1. Reviewer passed with no blocking issues; accepted one polish: the jig now
   fires `celebrateKey++` with the twirl so the moment reads as a real dance
   instead of a generic spin.
2. Left as-is per review: mid-jig tap latency (~2.5s, same pre-existing trait
   as the CURIOUS tiptoe branch — the 4s `lastPokeAt` cooldown still applies),
   and PLAY being exposed on only the one special spot (a product call).

## Validation

- Brace balance ALL OK (4 files), `git diff --check` clean.
- Reviewer (code-reviewer-deepseek-flash) passed.

## Completion summary

v8.17 shipped: a third `PetLandmarks.Kind.PLAY` — the pet dashes to its
flower bed on Home and does a little jig (squish → bow → hop+twirl, hearts,
a dance line) while the bed springs a beat. Pushed to Alpha.

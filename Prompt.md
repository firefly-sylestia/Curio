# Request — v8.20: pet interacts with its home (drop-to-home) + cloud-ride walk

Follow-ups v8.18 (landmarks on capture/cabinet/quests, `1fb4ea6`) and v8.19
(re-entry pulse fix, `ac521ce`) were pushed at the user's request before
this work began.

## What the user asked

1. Make the pet interact with its **home** (the flower bed) — beyond the
   v8.17 jig, make it possible to **drag the pet over the house and drop it
   to place it** (go home).
2. Add **cute walk animations** — the pet rides a little cloud while it
   walks.

## Changes (2 files)

| File | Change |
| --- | --- |
| `ui/pet/PetLandmarks.kt` | Added a hover channel for drop targets: `hoveredIds` snapshot map + `isHovered(id)` / `setHovered(id, boolean)` (no-op when unchanged — the drag loop reports every frame). The `PetLandmark` composable now animates a sustained **1.10 hover scale** (`animateFloatAsState` spring, multiplied into the poke pulse in the `graphicsLayer`) so the bed glows while the pet is dragged over it; `onDispose` also clears the hover. |
| `ui/pet/CurioFloatingPet.kt` | **Drop-to-home**: the drag `pointerInput` is now keyed on `routePrefix` (stale `watching` capture in the tap handler got the same keying). While dragging on Home, the pet's 72dp rect vs the `bed` landmark's bounds (inflated by 12dp forgiveness) drives the hover glow; dropping over the bed squishes + hearts + "Home sweet home!" then fades the pet home (`leavingHome → appear.animateTo(0) → CurioPet.goHome()`). `onDragStart/Cancel` clear the hover. **CloudRide**: a new `CloudRide` sibling drawn UNDER the sprite — three white puffs + flat base + soft shade, alpha-fading in with `moving`, bobbing ±1.5dp with a gentle swell. The bob is an `Animatable` driven by `LaunchedEffect(visible)` so it **only runs while the cloud is shown** (no forever-ticking transition for an invisible cloud). |

## Review fixes applied

1. **Unresolved `bobPhase`** — the first fix pass for the "ticks forever"
   nit removed the infinite transition but left `bobPhase` dangling (a CI
   failure). Rebuilt CloudRide around an `Animatable` bob + visible-gated
   loop, and dropped the now-unused `RepeatMode` / `infiniteRepeatable` /
   `rememberInfiniteTransition` imports.
2. **Stuck `dragged` on mid-drag navigation** — keying the drag handler on
   `routePrefix` means a tab switch during a drag cancels the pointerInput
   coroutine WITHOUT firing `onDragCancel`, leaving `dragged = true` (wander
   pause + no auto-nap forever). Self-healed by clearing `dragged = false`
   and the bed hover at the top of the pointerInput block (the new
   coroutine starts immediately on restart).

## Validation

- Brace balance ALL OK (2 files), `git diff --check` clean, no `bobPhase`
  refs, no stale transition imports.
- Reviewer (code-reviewer-deepseek-flash) passed after fix #2.

## Completion summary

v8.20 shipped: the pet now interacts with its home — drag it over the
flower bed (it glows as the drop target) and release to tuck it in; and it
rides a cute bobbing cloud while it walks, fading in only on the move. Two
review fixes applied (dangling `bobPhase` + stuck-`dragged` self-heal).
Pushed to Alpha.

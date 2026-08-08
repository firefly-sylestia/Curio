# Request — v8.31: swipe the deck's front card through the visible fan

## What the user asked

"Start with viewed cards stack now" (parked spec #5) — then narrowed it:
**"you dont have to redesign anything just make the swipes to be able to
swap trough the only visible things from the deck cards"** — no new UI, no
viewed-cards history; just make the front deck card swappable by swiping
through the cards already visible in the fan.

## How the deck works (context)

- `SpinScreen` holds `hand` (up to 6 topics, keyed on `filteredPool`) and
  `cycleIndex` (`mutableIntStateOf`).
- `Carousel` draws slots -2, -1, +1, +2 as `PeekCard` and slot 0 as
  `HeroTicketCard`, each resolved by `resolveTopicForSlot(slot, hand,
  cycleIndex, landedTopic)` — front = `hand[cycleIndex]`, neighbors =
  `hand[cycleIndex±1]`, `hand[cycleIndex±2]`. So nudging `cycleIndex` ±1
  rotates the whole fan through exactly the visible cards.
- The front card's title already animates via `AnimatedContent`, so a
  cycleIndex change glides the swap — no redesign needed.

## Fix (this request)

- **SpinScreen.kt**
  - New `onDeckCycle(delta)` callback: guarded by `!shuffling &&
    filteredPool.isNotEmpty() && !isOpening && hand.isNotEmpty()`, clears
    the landed pin (unconditionally — the `pointerInput` block captures the
    lambda once, so the recomputed `landedTopic` val would go stale in the
    captured closure; the MutableState delegate write is always live, and
    setting the same value is a no-op recompose), then wraps `cycleIndex`
    by `hand.size`.
  - `Carousel` gains `onCycle: (Int) -> Unit`, wired at all 3 call sites
    (compact / extra-compact / normal layouts).
  - Slot 0 wrapped in a `Box` with `Modifier.pointerInput(enabled,
    shuffling, opening)`: `detectHorizontalDragGestures` in a `while(true)`
    loop, 48dp threshold, drags consume changes — on drag end, swipe left
    → `onCycle(1)`, swipe right → `onCycle(-1)`. Taps under slop still
    reach the card's clickable and open the shown topic.
  - New imports: `detectHorizontalDragGestures`, `pointerInput`.

## Behavior

- Idle deck: swipe the front card to rotate through the visible fan cards.
- Landed card fronting the deck: the first swipe dismisses it (clears the
  pin) and lands on the neighbor that was visible in the swiped direction.
- Short flicks under 48dp snap back (no visual offset rendered — per
  "don't redesign", the card doesn't follow the finger; the AnimatedContent
  glide covers the swap).

## Validation

Brace balance + `git diff --check` pass; code review done (one robustness
fix applied: unconditional pin clear to avoid stale lambda capture). CI on
push is the compile gate.

## Parked v8.28 hooks spec (user picks, build later)

1. Topic of the day (Home) — gold must-see card, deterministic rotation.
2. Come-back teaser (Home) — rotating mix: pet missed you + what's waiting
   + streak warning.
3. Spin streak combo — XP multiplier up to 2x + "Spin Storm" meter.
4. Rare card moments — ~1 in 20, pet sniffs out / telegraphs.
5. Mystery card slot + smooth scrollable viewed-cards stack behind the
   landed topic (UX-first). — PARTIALLY DONE: swipe-through-fan shipped
   (v8.31); the viewed-cards HISTORY stack is still parked.
6. Streak freeze (7-day milestones) + revival (XP scaled by streak).
7. Weekly rotating themed chain (e.g. "Explorer Week").
8. User's own Pet/Cabinet/Profile ideas, to share later.

Always-on unless the user asks for a toggle.

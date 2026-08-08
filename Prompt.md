# Request — v8.32: peek cards animate one after another (cascade wave)

## What the user asked

Modify the peek cards animation during shuffling: "the top card goes first
then the buttom card then like that not together but one after another
animates and each time they animate the main cards does its animation too
and without making the animation feel janky it will feel faster when
shuffling." So — stagger the peeks top→bottom instead of wiping all at
once, keep the hero card animating every tick too, and make the whole thing
feel faster/snappier without jank.

## How it worked before

On every shuffle tick, `cycleIndex = ++tick` changed all five fan slots'
topics simultaneously, so all four peeks wiped in unison (PeekWipeInMs 320 /
OutMs 300) and the hero glided (300ms) + pulsed (tickPulse) at the same
moment. Felt like one block flipping.

## Fix (this request)

- **Constants** — `PeekWipeInMs/OutMs` (320/300) replaced with
  `PeekWaveInMs = 120`, `PeekWaveOutMs = 110`, `PeekWaveStaggerMs = 45`.
  Idle re-fan constants (PeekIdle 300/280) unchanged.
- **PeekCard transitionSpec (shuffling)** — each peek's enter AND exit
  specs are `delayBy(waveTurn * PeekWaveStaggerMs)` where waveTurn is
  slot -2→0, -1→1, 1→2, else→3 (top-to-bottom waterfall). The card holds
  its old topic until its turn, then swaps fast (delayBy on both sides
  keeps the old content visible during the wait — no blank flash).
- **HeroTicketCard content reel (shuffling)** — joined the wave at its
  center slot: `heroDelay = 2 * PeekWaveStaggerMs` (90ms) applied via
  delayBy, wipe shortened 300→180ms / 260→160ms so it lands under the floor.
- **Hero tickPulse** — `delay(2 * PeekWaveStaggerMs)` before the bounce so
  the pulse lands mid-ripple, synced with the reel swap.
- **Import** — `androidx.compose.animation.core.delayBy` added
  (alphabetically placed).

## Timing math (no jank)

Tick floor ≈ 340ms (interval 340–520ms). Last peek starts at 3×45=135ms
and finishes ~255ms; hero finishes ~270ms. Whole wave < 340ms, so no
overlap on the fastest ticks — reads faster than the old 320ms unified
wipe.

## Validation

No stale PeekWipe refs; brace balance + `git diff --check` pass; code
review done (import-order nit fixed). CI on push is the compile gate.

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

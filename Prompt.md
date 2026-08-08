# Request — v8.33: CI fix (delayBy unresolved) + bonus quest gold readable in light mode

## What the user asked

"fix this first then push and continue and ask me later for the plan also
change the yellow golden color of bonus quest as its not visible in light
mode" — with the CI compile errors pasted. So: (1) fix the SpinScreen
compile failure, (2) make the bonus quest gold readable on light
backgrounds, (3) push, then continue the pet-designer plan later (asked
the user about it separately — NOT implemented yet).

## CI failure (SpinScreen.kt)

`androidx.compose.animation.core.delayBy` was unresolved in this Compose
version (broke import + type inference on every chained tween), and
`delay(2 * PeekWaveStaggerMs)` passed an Int where Long was expected.

### Fix (pushed)

- Replaced all 8 `.delayBy(x)` chained specs with `tween(duration,
  delayMillis = x, easing = ...)` — the built-in `TweenSpec.delayMillis`
  parameter, version-safe, no new import. Same 180/160 hero + 120/110 peek
  timings, same stagger values, zero behavior change.
- `delay((2 * PeekWaveStaggerMs).toLong())` for the hero bounce.
- Removed the `delayBy` import.

## Bonus quest gold (light mode)

`CurioColors.ButterYellow` (0xFFFFD97D) is a pale pastel that vanished on
the cream background — the "Bonus quests unlocked!" banner, the BONUS
label, the bonus icon/progress/Claim pill all washed out in light mode.

### Fix (pushed)

- **CurioColors.kt** — new `GoldInk = Color(0xFFB8860B)` (dark goldenrod,
  readable on light).
- **QuestsScreen.kt** — new `@Composable private fun bonusGold()`:
  ButterYellow in dark mode (pops there), GoldInk in light. Applied to the
  bonus accent (drives icon box, progress bar, Claim pill, Go chip), the
  unlock banner icon + text, and the BONUS label. The Claim pill was
  white-on-pale-butter before — dark gold is a strict contrast win.
- Rank trophy / rank badge / medal check intentionally left ButterYellow
  (user asked about bonus quests specifically; can extend later if needed).

## Validation

No `delayBy` refs remain; brace balance + `git diff --check` pass on all 3
files; code review done. CI on push is the compile gate.

## Parked: pet designer playground (user's next feature)

DONE — v8.34 shipped: PetDesign model + hex text import/export format,
PetDesignerScreen (Settings → Pet designer) with live preview, 16×16 pixel
grid editor (body/asleep), palette recoloring + quick picks, preset shapes,
randomizer, clipboard import/export, save/reset (always-on when saved).
Sprite reads saved design reactively via AppPreferences.petDesignState.
Follow-ups if wanted: eye/mouth position presets for custom bodies, more
preset shapes, undo history.

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

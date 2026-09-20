# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "now that +- chnage pages and page number but make the plus holdable and when holded it
> goes fast the page count, and also add haptics to it."

(The book page's reading-progress card, rebuilt in the previous request — commit `b812027c`.)

## 2. What the code actually looked like (findings)

- The card's stepper ends are one shared composable, `TileStepButton` (used by both
  `ProgressTile`s and by `BookLengthRow`). It was a `Surface(onClick = …)` — tap only, no
  repeat, no haptics, and the only feedback was the Material ripple.
- The app already had the exact pattern this needs: `ReaderHoldButton` in
  `BookReaderScreen.kt` (`PageTurnHoldDelayMs = 320L`, `PageTurnHoldRepeatMs = 150L`) —
  a `detectTapGestures(onPress = …, onTap = …)` where the press owns a coroutine that
  waits out the delay, then repeats until `tryAwaitRelease()` returns, with a plain
  `booleanArrayOf` flag so the hold never also fires the trailing tap. AGENTS.md records
  why: one detector, one code path.
- Haptics convention in this codebase: `HapticFeedbackType.TextHandleMove` for scrub/step
  ticks (used for sliders and steppers), `KeyboardTap`/`LongPress`/`Confirm` elsewhere.

## 3. What was done

All in `app/src/main/java/com/curio/app/features/personal/BookDetailScreen.kt`:

- **`TileStepButton` is a hold button now.** A tap moves one step; a hold past
  `TileStepHoldDelayMs` (340ms) starts a repeat whose interval ACCELERATES from
  `TileStepRepeatStartMs` (160ms) down to `TileStepRepeatMinMs` (50ms), shaving
  `TileStepRepeatAccelMs` (16ms) after every tick — so the page count visibly runs the
  longer the finger stays.
- **A haptic tick on every step** — `HapticFeedbackType.TextHandleMove`, once for the tap
  and once per repeat tick, so the count can be felt without watching the number.
- **A `0.9f` press scale** (`animateFloatAsState` + `Modifier.scale`) replaces the ripple
  the `Surface(onClick)` used to draw, so the button still answers the finger.
- Gesture owned by one `detectTapGestures(onPress/tryAwaitRelease, onTap)` with the
  `held` flag, exactly like `ReaderHoldButton`; the repeat lives in the composition's own
  `rememberCoroutineScope`, so it dies with the card. Constants added beside
  `MAX_GAUGE_NOTCHES`.
- Applies to both tiles' `−`/`+` and to the `BookLengthRow` pill — one shared component,
  one behaviour, no special case.

Docs + notes: `app/AGENTS.md` (the card bullet records the held stepper), the 20260922
changelog (one ADD bullet).

## 4. Decisions

- Accelerating rather than a flat repeat interval: the ask was "goes fast the page count",
  and a flat 150ms would still take ~45s to cross 300 pages.
- Behaviour is uniform across all three stepper sites rather than page-only. Holding a
  book-length stepper to move quickly is equally useful, and a shared component with an
  invisible special case would be worse than the consistent one.
- No ask_user round: the request named the control, the behaviour and the haptics.

## 5. Status

- Implementation done; brace/paren balance verified (0/0). CI to confirm compile on push.
- No pending follow-up.

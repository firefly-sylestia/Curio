---
name: curio-design
description: Curio's own design and motion contract for the Android app — dynamic motion, visual consistency, tokens, glass/frost, elevation, and press feedback. Use when adding or changing ANY Curio UI: a composable, screen, sheet, pill, card, animation, transition, duration, easing, spring, colour, shape, typography, glass surface, or press/touch feedback. Use it before inventing a colour, a corner radius, or a millisecond, and whenever a change touches how the app MOVES or how one surface compares to another.
---

# Curio Design & Motion

Curio is one app with one motion language and one token set. This skill is the
local contract: it says where the numbers live, which of them to spend, and the
five rules that keep a new surface from looking like it came from a different
app. It is distilled from the Curio codebase itself and two community motion
skills (`motion-system`, `animation-principles`).

**Read the code, not this summary, before you spend a token.** The tokens are
the source of truth:

| What | Where |
|---|---|
| Motion tokens, springs, pill clock | `app/src/main/java/com/curio/app/ui/theme/CurioMotion.kt` |
| Shapes | `ui/theme/CurioShapes.kt` |
| Colours + category accents | `ui/theme/CurioColors.kt`, `ui/theme/CategoryInk.kt` |
| Lite mode / ambient gating | `ui/theme/CurioLiteMode.kt` |
| Touch feedback | `ui/theme/CurioPressIndication.kt` |
| Glass / frost surfaces | `ui/components/LiquidGlassPills.kt` |
| Full design decision log | `app/AGENTS.md` (the design contract in practice) |
| One worked spec (tokens → states → a11y) | `docs/design.md` |

## When to Activate

- Adding or editing any composable, screen, sheet, dialog, pill, or card.
- Touching an animation, transition, spring, duration, or easing.
- Choosing a colour, corner radius, elevation, shadow, or glass/frost fill.
- Making a new surface — the consistency question is always "does this look
  like the eight screens beside it?"
- Reviewing a diff that changes how the app moves or how it looks.

## Rule 1 — One clock: no floating surface invents its own milliseconds

A duration written as a literal (`.tween(220)`) next to a pill **is a bug**.
Every animated surface spends a token from `CurioMotion`, so a pill arriving in
190ms never sits beside one arriving in 220ms — that mismatch, not any single
wrong number, is what reads as "the animations are still bad".

The floating furniture (reader head/foot/search/scrubber, journal dock, copy
box, undo pill, mic, sheets) uses the **pill clock**:

| Token | Value | Use |
|---|---|---|
| `ENTER_MS` | 190ms | what arrives |
| `EXIT_MS` | 130ms | what leaves |
| `EMPHASIS_MS` | 320ms | a change worth watching (panel grows in place) |
| `TICK_MS` | 90ms | the tick under a press, a drag that let go |
| `SETTLE_DEBOUNCE_MS` | 80ms | throw-vs-nudge decision pause |
| `SETTLE_FRACTION` | 1/6 | drift on arrival, as a fraction of the thing's own height |
| `POP_SCALE` | 0.80 | start/end scale for a free-floating round control |

Spend them through the factories — do not hand-build the spec:
`arriveFade()`, `leaveFade()`, `pillArrive(fromTop)`, `pillLeave(fromTop)`,
`popArrive()`, `popLeave()`; and `settle(height)` for a measured drift.

Easings come from the same file: `Enter` (`FastOutSlowInEasing`), `Exit`
(`FastOutLinearInEasing`), `Soften` (`LinearOutSlowInEasing`), `Emphasis`
(`CubicBezierEasing(0.2f, 0f, 0f, 1f)`).

For screen-to-screen motion the clocks are `Durations.Push` (260ms) and
`Durations.Pop` (220ms) — **never `Deliberate` (500ms) for a page appearing**;
half a second reads as the app thinking, and it is what made the journal open
feel clanky.

## Rule 2 — Enter slower than exit, always; travel ∝ the thing that moves

On the way in the member is deciding; on the way out the system is getting out
of the way. That asymmetry is why an exit never reads as slow even when it is
not short. A pop is a verdict, so it is quicker than the push it mirrors.

Travel scales with the mover: a 42dp pill and a 58dp pill drifting "one nudge"
must not travel the same fixed dp — hence `SETTLE_FRACTION`. The same reasoning
applies to any new surface: express the distance as a fraction of the element's
own size, never a hard-coded offset.

## Rule 3 — Springs are a vocabulary, not a menu

Pick by *meaning*, and never restate the numbers at the call site:

| Spring | Physics | Meaning |
|---|---|---|
| `Springs.Snappy` | 1.0 / 1800 | decisive, no overshoot — chip toggles, drawer mounts, modal mounts |
| `Springs.Calm` | 1.0 / 750 | screen entrances that must not zoom back |
| `Springs.Bouncy` | 0.55 / 380 | reward + playful arrivals — dial settling, card mounting, save success |
| `Springs.Deliberate` | 0.85 / 250 | big elements, long distances, controlled overshoot |
| `Springs.Morph` | 0.92 / 120 | organic shape/size morph, water-droplet calm |
| `Springs.Elastic` | 0.45 / 340 | dramatic entrances — use sparingly, it is the show-off spring |
| `Springs.Press` | 0.65 / 800 | press-down to 0.94 with a quick snap-back |
| `Springs.BouncyDp` | 0.55 / 380 | the same bounce, typed for a `Dp` (a width cannot take a `SpringSpec<Float>`) |

Durations (ms): `Quick` 150 · `Standard` 300 · `Deliberate` 500 · `Morph` 450 ·
`Reveal` 650 · `SpinMin/SpinMax` 2800/3600 · `Confetti` 600 / `ConfettiLong`
1200 · `SparkleTrail` 400 · `RevealHold` 400 · `Shimmer` 1500 · `Breathe` 3200 ·
`SpeakRest` 4200.

## Rule 4 — Consistency: spend a token or add one

- **Colour:** theme roles only — no raw `Color(0x…)` in a feature. Accent text
  and icons on a light paper surface use the readable ink twins
  (`CurioColors.CoralInk`, `GoldInk`) because the pastel primaries vanish on
  cream; on dark surfaces use the light 300-level ink twin resolved by
  `categoryInk()`. Category accents stay Tailwind-700 deep so white content
  clears WCAG AA on them.
- **Shape:** `CurioShapes` 8 / 16 / 24 / 32 / 48 dp. Nothing in the app carries
  a hard 90° corner except dividers and rules.
- **Typography:** `geom.ttf` for display/headline/label, M3 default for body.
- **Touch:** the app-wide pressed look (own pixels tinting, ~90ms in / ~260ms
  back out via `CurioPressIndication`) — not a Material ripple. A ripple on a
  rounded card reads as a foreign blob.
- **If no token fits, add the token to the theme file** (and its comment
  explaining the meaning), rather than a literal at one call site. A literal
  that fits nowhere is how the vocabulary fractures.

## Rule 5 — Depth, glass, and what must not be elevated

- `Modifier.shadow()` goes **before** the fill in the chain —
  `.shadow(e).clip(shape).background(color)`. A shadow after the background
  paints a dark blur on top of the fill.
- `shadowElevation` renders cleanly only over an **opaque** fill. For translucent
  glass use an opaque `lerp(fill, accent, alpha)` blend, never
  `color.copy(alpha = …)`.
- **Never add elevation to animating deck cards** — a halo regresses into a boxy
  artifact during the reel. This has been reverted twice; do not re-add it.
- Frosted glass (heavily blurred, near-opaque whitish frost) is the default look
  for floating chrome and sheets; Settings → Experiments → Liquid glass exposes
  the clear refracting alternative. Never drop per-frame refraction onto a page
  that is itself redrawing — that shimmer is a known glitch.

## Lite mode: gate ambience, never meaning

`isLiteMode` / `isAmbientMotionOn` / `rememberAmbientTransition(label)` are the
single door every "may this decorative motion play?" question goes through.

- **Gate:** ambient clocks (shimmer, twinkle, breathe, idle wobble) and
  blur/refraction passes.
- **Never gate:** screen transitions, a sheet opening, a press, a state change, a
  progress bar answering a real action, a page turn. Freezing those makes the app
  feel broken, which is exactly what Lite mode must not do.
- Lite mode changes no layout, no colour and no state — only the loops.

## The five questions before you call a surface finished

1. Does any literal millisecond survive in my diff? (It should be a token.)
2. Does the thing arrive slower than it leaves, and does its travel scale with it?
3. Is every colour, radius and font a token, and is this the same answer the
   neighbouring screens give?
4. Does it still move correctly in Lite mode — and does it *stop* moving when it
   should?
5. Read the two things back to back: this surface and the closest existing one.
   If they disagree, one of them is wrong and it is probably the new one.

## Verification

- Compose cannot be built in this environment (`app/AGENTS.md` / root `AGENTS.md`):
  compile validation is CI. So verify by reading the seams — `grep -n -B2` the
  anchor before any insertion that sits above a declaration (an annotation binds
  to the next declaration), and re-read the five lines either side of any cut.
- `DevFullAppTestRunner` in Developer Settings smoke-tests entity/data changes;
  use it for settings toggles and state, not for look.
- Motion and look are settled by the user seeing the build — do not declare a
  motion change correct from the diff alone.
- Cross-check against `docs/design.md` when writing a new component spec: it is
  the one written example of tokens → anatomy → interaction model → states → a11y.

## Sources

Community skills this one leans on (load them for depth, not instead of the
tokens): `motion-system` (duration/easing/choreography vocabulary, reduced
motion) and `animation-principles` (easing, staging, follow-through for one
specific motion).

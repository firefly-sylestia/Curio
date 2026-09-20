# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "now for the material theme well the shuffle page cards they have differnt colors right i
> want them to keep the color how about you use the mterial theme device color as the fill
> which goes from the top side to the buttom side but not full to the butom 70% then 30% is
> the category accent, wouldnt that be cool, we already have grdint 2 colos for one
> category how about we use that style and for multiplre mix just use the full all material
> device color mixes with differnt gradient styles"

Answered on ask_user: the **whole deck family** (front ticket, both peek slabs, the spin
button and the confetti accent); the 70/30 join is **blended**, not a hard band; a MIXED
deck is the **full device colour with the existing per-deck gradient styles**.

## 2. What the code actually looked like (findings)

- The deck's colour pipeline is two functions on `CurioMixedDeck` in `CurioColors.kt`:
  `mixedDeckAccent(accents, pastel, dark, materialPrimary)` — the single accent the peeks,
  spin button and confetti derive from — and `mixedDeckGradient(accents, materialPrimary)`
  — the hero ticket's stop list.
- Under the Material theme both took a `materialPrimary` (resolved at the Spin call site as
  `settingsRoseAccent()`, i.e. the scheme's `primaryContainer`) and fell back to a lane's
  muted **family fill** — so a single-lane deck was per-category family colours, and a mixed
  deck was one family tone rendered as `CurioGradients.cardGradient(raw)`.
- The hero ticket's brush (`HeroTicketCard`) had exactly two paths: `isMixed` →
  `CurioMixedDeck.mixedDeckHeroBrush` (diagonal / reversed-diagonal / radial, keyed off the
  deck's category set), else the enhanced diagonal crown→base sweep.
- Peek slabs derive their fills by deepening the deck's gradient stops (HSL lightness drop
  + saturation pull), so they follow whatever `gradient` holds — no separate plumbing.
- Card inks came from `cat.onAccent()` (the family on-fill) — which is wrong once the fill
  is the device colour.

## 3. What was done

All in `ui/theme/CurioColors.kt` and `features/spin/SpinScreen.kt`:

- **`CurioGradients.materialDeckBlend(device, accent)`** — 10 evenly-spaced stops: the first
  seven hold the device colour (positions 0.00–0.667 = the top ~70% of the card), the last
  three ramp through OKLab into the lane's accent. A plain `Brush.verticalGradient` then
  renders the 70/30 split with a blended seam, with **no stop-position plumbing** — the hold
  is just the same colour repeated across evenly spaced stops.
- **`mixedDeckAccent` / `mixedDeckGradient` gained a `materialDevice: Color?` parameter.**
  `mixedDeckAccent` returns it (pastel-softened in pastel mode) as the deck accent, so the
  peeks, spin button and confetti follow. `mixedDeckGradient` uses `materialPrimary != null`
  as the mixed signal: mixed → `[device, device deepened at the foot]` for the existing
  per-deck brushes; single lane → `materialDeckBlend(device, laneAccent)`.
- **`SpinScreen`** resolves `materialDeckFill = MaterialTheme.colorScheme.primary` once and
  passes it to both; `HeroTicketCard` gains a `materialThemeOn` brush branch
  (`Brush.verticalGradient(gradient)`) ahead of the diagonal sweep — the mixed branch above
  it still takes the style-varied brush; and both the hero card's and the peek slab's ink
  take a `materialThemeOn` branch through `curioFillInk(...)` so the words read on the
  device fill.

Docs + notes: a new `### v413 — the Material deck wears the device colour` section in
`app/AGENTS.md` and one ADD bullet in the 20260922 changelog.

## 4. Decisions

- The device colour is the scheme **primary** (the wallpaper-derived tone) — that is what
  "device color" means in Material, and it is what the deck now wears.
- The 70/30 hold is expressed by repeating a colour across evenly spaced stops rather than
  by threading `Pair<Float, Color>` stop positions through `List<Color>` signatures used by
  three call sites and the peek deepen — same visual result, no API churn.
- The mixed-deck **different gradient styles** were already there (`mixedDeckHeroBrush`);
  the change is only what it paints with, so no new style system was invented.
- **No new experiment toggle.** The Material theme is itself an opt-in Appearance choice, so
  choosing Material is choosing this fill — per AGENTS.md's rule that a settings gate is
  about how an experiment ships, and this one ships inside an existing user-facing toggle.
- Pastel mode + Material keeps the device colour (softened) without the accent foot — the
  pastel single-lane branch in `SpinScreen` builds its own two stops before
  `mixedDeckGradient` is consulted. Accepted: the requested recipe is the non-pastel look.

## 5. Status

- Implemented; brace/paren balance verified on both files (deltas unchanged: 0/0). The
  previous batch is committed as `3770d967`.
- **Nothing has been pushed** — the user asked to hold the push.
- No pending follow-up.

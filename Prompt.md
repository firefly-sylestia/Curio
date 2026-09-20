# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "now lets fix pantone colors. all 3 are bad, dont use too deep colors for the cards or
> backgroud, can u fix all of them see whats right, and chnage the hero etc on the basic of
> the colors use new colors if needed. and do it all. fully revamp the hirarcy contrast etc
> properly use more shades pallete per pantone theme"

One target: `ui/theme/PantoneThemes.kt` and the scheme it hands to `CurioTheme`. The member
authorised judgement ("see whats right", "use new colors if needed"), so the only plan step
was the palette maths itself — done with a Python mirror of `toHsl`/`fromHsl`/`luminance` so
every tone and every pair could be measured before it was written into Kotlin.

The previous request (the empty Pages / My shelf doors) was committed as `966ec4c1` and left
unpushed at the member's own instruction; it rides along with this commit.

## 2. What was wrong (findings)

1. **The card ladder deepened the HERO.** v412's `surfaceContainer*` were `deepen(hero, 0.05…
   0.15)`. Pantone Terracotta's hero (2350 U, `#9E483F`) therefore made every sheet, settings
   card and journal block a deep brick red, each nested step darker — "too deep colors for the
   cards". On Cream (`#B6CADF`) and Lime (`#CDD325`) it walked into grey-olive mud.
   The hero was being asked to be both the page's furniture and the page's accent.
2. **Three fills, one colour.** `primary`/`primaryContainer` were both the hero; v412's
   `secondary` was the hero one step deeper. A chip and the button beside it were the same
   colour.
3. **Derived roles were picked by the wrong number.** `goldInkFor` off the HERO (Lime's hero is
   a yellow-green → a green "amber" streak flame) and `sageInkFor` off the ink desaturated
   (Terracotta's → a second brick). `errorFor` off the ink's hue (Lime's indigo → an
   indigo "delete").
4. **`errorContainer` / `onErrorContainer` / `scrim` were never set**, so they fell through to
   Material's baseline palette — a PINK error container in a theme whose rule is "no other
   colors". 11 + 9 + 1 call sites read those roles.

## 3. What was built

- **The ladder is the PAGE's own hue and it climbs:** `surfaceContainerLowest` 0.975, `Low`
  0.945, `Mid` 0.915, `High` 0.882, `Highest` 0.845 (light) — cards separate from the page by
  LIGHTNESS, the app's own rule, and nothing is a dark block. Night: 0.115 page, then 0.13 /
  0.165 / 0.195 / 0.21 / 0.24 (small steps, because dark steps must be small).
- **The hero is the accent again** with a PALE container twin: `primaryContainer` = hero at
  0.90, `onPrimaryContainer` = hero at 0.28. `secondary` = the INK at accent depth (0.40) with
  its own container twin; `tertiary` = the PAGE at accent depth (0.30). Night gets the
  mirror set.
- **Derived roles are picked, then walked:** `warm` = the most SATURATED number in the amber
  band 15°–70° (Terracotta's page 40°/0.68 vs ink 28°/0.83 → the ink wins, it is the real
  amber); `cool` = the coolest number; `goldInkFor` = warm at ink depth; `sageInkFor` = the
  cool hue walked FORWARD toward 250° by ≤45° (shortest-path walking swung Terracotta back
  through red into a brick — olive is the honest cool ink for an all-warm brief);
  `errorFor` = the warm hue walked toward red by ≤35° (`alert()`), so Lime's delete is a deep
  violet-red and not its indigo.
- **`errorContainer` / `onErrorContainer` / `scrim` now come from the palette** (`alert()` at
  0.90/0.26 light and 0.28/0.88 dark; scrim = the ink at 0.10 light / 0.08 dark).
- **`warm`/`cool`/`coolHue` are `val`s computed once** with the enum, not per composition.
- **Not changed, deliberately:** lane/category FILLS still resolve to the hero number
  (`themedAccent`) — a lane card is a fill, not a plate, and it is the one place the palette is
  allowed to be saturated (the app's own lane cards are 700-level deep). Also unchanged: the
  no-alpha rule, the no-card-border rule, the pure-`at()` derivation (nothing outside the three
  numbers is painted) and the ink ladder's role names, so no call site moved.

## 4. The measured palette (light / dark, per theme)

Every pair below cleared 4.5:1 on its own page AND on the deepest card step; container twins
were checked against their own contents; white was checked on the light error fill.

| role | Cream | Terracotta | Lime |
|---|---|---|---|
| page | `#F7E9C6` | `#F5E1C5` | `#FAF4D3` |
| card ladder (5 steps) | `#F5F3EC` → `#E5DDCA` | `#F5F2EC` → `#E5DACA` | `#F5F4EC` → `#E5E1CA` |
| body ink | `#3E353D` | `#4C3A27` | `#2A2C48` |
| muted ink | `#5D505C` | `#6C5741` | `#41436C` |
| accent (= the Pantone ink) | `#4D424C` | `#7C3831` (ink is orange, 1.74:1 → hero at 0.34) | `#5F62A1` |
| hero + its words | `#B6CADF` / `#3E353D` | `#9E483F` / white | `#CDD325` / `#2A2C48` |
| second fill | `#6E5E6C` | `#996833` | `#4B4E81` |
| third fill | `#6F5B2A` | `#6F522A` | `#6F642A` |
| gold | `#785B11` | `#784711` | `#786911` |
| sage | `#3F3960` | `#536039` (olive) | `#3F3960` |
| error | `#A53827` | `#A53827` | `#A54827` |
| night page | `#232017` | `#231E17` | `#232117` |

One named margin: Lime's indigo accent is 4.26:1 on the deepest nested step (0.845) — above the
3:1 bar icons and large labels are held to, below the body-text bar — which is why the accent
stays the real Pantone ink instead of deepening into a colour indistinguishable from body text.

## 5. Status

- Implemented in `PantoneThemes.kt` (roles, both schemes, palette maths) + `app/AGENTS.md`
  ("The Pantone tone ladder (v413)") + three FIX bullets in `20260922.txt`.
- Brace/paren/bracket balance verified 0/0; no stale references to the removed private helpers
  (`inkOn`, `paleInk`, `deepen`, `lift`, `shade`) anywhere in the app; all role function names
  the rest of the app calls are unchanged.
- No Gradle in this environment — CI validates the compile.
- Pushed together with the held `966ec4c1`; CI watched and any failure fixed.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§4)

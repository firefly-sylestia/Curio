# Curio motion system — the Felicity plan

**Owner:** `app/` (Android). Companion docs: `app/AGENTS.md` (the release log),
`app/APP_AUDIT.md` (defects), `app/src/main/java/com/curio/app/ui/theme/CurioMotionSystem.kt`
(the implementation), `ui/theme/CurioMotion.kt` (the pre-existing springs + pill clock).

**Source of the design:** [`firefly-sylestia/Felicity`](https://github.com/firefly-sylestia/Felicity)
(`decorations/src/main/java/app/simple/felicity/decorations/transitions/`,
`.../itemanimators/`, `music/src/main/java/app/simple/felicity/utils/AnimationUtils.kt`).
Felicity is **AGPL-3.0** and so is Curio — the same licence, so the design and code
port are licence-clean. Port the *design* and rewrite it for Compose; do not paste
View/`ViewPropertyAnimator` code into a Compose tree.

**The switch:** Experiments → **Motion → "Smoother transitions"**
(`AppPreferences.motionSystemState`, key `motion_system_v1`, default **OFF**).

---

## 1 · What Felicity actually does (read from its own sources)

| Felicity | Its own numbers | What it means |
|---|---|---|
| `SeekableSharedAxisXTransition` | `TRANSLATION_FRACTION = 0.25`, 500ms | A screen drifts **a quarter of the scene's width** *while* fading; both screens are on screen for the whole 500ms. A crossfade **with drift**, never a full slide. |
| `SeekableSharedAxisZTransition` | `SCALE_IN_FROM = 0.5`, `SCALE_OUT_TO = 1.5` | Depth: forward, the newcomer comes forward *from behind* (`0.5 → 1`); on the way back it recedes in from *in front* (`1.5 → 1`). |
| `SeekableSharedAxisFadeTransition` | 500ms | Peers cross-fade. |
| `BaseSeekableTransition` | `DECELERATE_FACTOR = 3` | `DecelerateInterpolator(3f)` while free-running = `t = 1 − (1−t)⁶`; **`LinearInterpolator` while the gesture is seeking it**. The transition is a *function of progress*, so the back gesture scrubs it and it only settles on release. |
| `FelicityDefaultAnimator` | add 300ms, remove 500ms, `SCALE_START = 0.85` | An item arrives as `alpha 0→1` + `scale 0.85→1`; it leaves the same way, slower. |
| `FlipItemAnimator` | `changeDuration = 400` | A **content swap** in place: the old content fades out, the new one flips in (`rotationX −180 → 0`) with a fade. |
| `AnimationUtils.animateToZeroScale` | 500ms | Collapse-and-fade, with a callback. |

The one sentence that matters: **Felicity's motion is a crossfade with drift, seekable
by the finger.** Not a slide, not a bounce.

## 2 · Where each piece lives in Curio

| Felicity | Curio | State |
|---|---|---|
| Shared axis **X** | `sharedAxisXEnter/Exit(forward)` in `CurioMotionSystem.kt`; the `else` branch of all four NavHost transitions | **shipped** |
| Shared axis **Z** | `sharedAxisZEnter/Exit`, `sharedAxisZPopEnter/PopExit`; used by `isDetailRoute` / `isPopScreenRoute` | **shipped** |
| Shared axis **F** (fade) | `sharedAxisFadeEnter/Exit`; tab switches, the settings family (the rail's pill is the shared element), Topic Reveal, the Pet Designer, splash → home | **shipped** |
| The settle curve | `CurioMotionSystem.Settle` = `1 − (1−t)⁶` | **shipped** |
| The **seeked** curve | `CurioMotionSystem.Track` = linear — handed to navigation's predictive-pop parameters in phase 2 | **token shipped, not yet wired** |
| Item ADD | `Modifier.curioItemIn(key, order)` — drawn in the draw phase, staggered | **shipped**, adopted on Home's recents rows |
| Item REMOVE | not ported (see §5) | — |
| `FlipItemAnimator` | not ported (see §5) | — |
| `animateToZeroScale` | not ported (see §5) | — |

## 3 · The rules this port is held to

1. **The transition is a function of progress.** No `LaunchedEffect`, no state, no
   side effects inside a transition lambda — that is what lets navigation-compose
   (2.8+) *seek* it under a predictive-back gesture. A transition with a side effect
   plays a fixed clip and the seek dies.
2. **One clock for the screen layer:** `NAV_MS = 500` for every navigation branch.
   A drift and its crossfade finish together, or the page reads as gliding on ice.
   Its **Lite-mode twin** is `NavMs` (= 320): Lite mode shortens motion rather than
   removing it, because a screen open is not useless animation — the vocabulary is
   identical, only the tempo changes.
3. **Where a shared element is the animation, the screen fades.** Topic Reveal's hero
   grows out of the Spin ticket; the settings rail's pill moves between chips. A drift
   there would move the page out from under the element that is already travelling —
   so those routes get the fade branch, and that is a *correctness* rule, not a taste.
4. **Old motion is not consulted while it is on.** With the switch on, the
   `CurioMotion.Durations` branches in `CurioNavHost` are unreachable, and the screen
   reveal stands down (`screenRevealActive` in `CurioRevealNav.kt`) — two
   screen-switching experiments must never both be on: the reveal freezes the outgoing
   screen into a bitmap and sweeps it, the motion system drifts the live pages.
   The reveal's own preference is untouched, so turning the motion system off restores
   it exactly.
5. **Nothing is deleted.** The old branches stay in the file behind the gate. Removing
   them is phase 5, once the new system has been lived with.
6. **Interaction motion is not the screen layer.** `CurioMotion`'s springs and its pill
   clock (arrive/leave) stay exactly as they are: they are tuned per surface, they are
   not "opening" motion, and swapping them would make every pill in the app slower for
   no visible gain. `Curio Alive` (the interaction experiment) is a different switch
   from this one, and both can be on.

## 4 · Phases

**Phase 1 — the screen vocabulary (SHIPPED, v455).**
`CurioMotionSystem.kt` + the four NavHost transitions + the reveal stand-down + the
Experiments switch + the item-entrance primitive on Home's recents.

**Phase 2 — the predictive-pop overload (NEXT, needs a dependency bump).**
`navigation-compose` 2.10.0 added `predictivePopEnterTransition` /
`predictivePopExitTransition`. Wiring them means passing the same X/Z shapes with
`easing = CurioMotionSystem.Track` (linear), which is exactly Felicity's
"linear while seeked" rule — today the gesture seeks the *settle* curve, so the
content moves ahead of the finger at the start of the drag.
**Before bumping:** the version catalog must move `navigationCompose` 2.9.8 → 2.10.1
*and* the pairing with the Compose BOM (`composeBom = 2026.05.01`) has to be checked —
a nav library newer than the BOM can link against compose APIs the runtime does not
have, which fails on a device and not in CI. Verify on a device before shipping.

**Phase 3 — sheets and dialogs.** A sheet is a screen that does not change route.
Felicity's Z is the natural fit (depth, not sideways travel) and this is where the app
still uses per-sheet durations. Do it one sheet family at a time, and keep
`CurioMotion`'s pill clock for the *furniture inside* the sheet.

**Phase 4 — the item animators app-wide.** `curioItemIn` is on Home's recents only.
Adopt it on the Cabinet grid, the journals list and the topic database, then decide
whether the REMOVE animation and the flip (a content swap in place — the natural home
would be a card whose topic changes) are wanted.

**Phase 5 — retire the old branches.** Once phase 1–4 have been lived with: delete the
`CurioMotion.Durations` nav branches, and decide whether the switch itself dies
(root `AGENTS.md`: a settled experiment is hardcoded, not kept as a fixture).

## 5 · Deliberately NOT ported (and why)

- **REMOVE (alpha→0 + scale→0.85 over 500ms).** A list that removes a row while
  animating its collapse fights the layout pass in Compose; `LazyColumn`'s
  `Modifier.animateItem()` covers placement/fade, and a bespoke remove animation is
  how a list becomes janky. Revisit only if a specific list needs it.
- **`FlipItemAnimator` (rotationX).** A 3D flip is a *change* animation for a recycled
  ViewHolder; in Compose the equivalent (a card whose content swaps in place) is rare,
  and a flip in a list reads as a glitch rather than as depth. No current surface needs
  it. If one ever does, port the *fade pair* without the rotation first.
- **`animateToZeroScale`.** It is a View helper for dismissing a view; Compose has
  `AnimatedVisibility` + `scaleOut` and the app already uses them.
- **The IME/insets animation callbacks** (`HeightDeferringInsetsAnimationCallback`,
  `TranslateDeferringInsetsAnimationCallback`, `SimpleImeAnimationController`). Compose
  handles ime insets differently; the app's own `imePadding`/`WindowInsets` rules are
  the Compose-native equivalent and are already in place. Porting the View callbacks
  would be a regression, not a port.
- **`SlideFastScroller`, `FelicitySlider`, pager helpers.** These are widgets, not the
  motion system. Curio has its own.

## 6 · Verification checklist (before this is called done)

- [ ] Every transition lambda is **pure**: `grep -n "LaunchedEffect\|remember" CurioNavHost.kt`
      inside the four transition blocks returns nothing.
- [ ] Both directions are mirrored: a route's enter is its pop-exit reversed, offset
      and scale sign included (`sharedAxisXExit(forward=false)` etc.).
- [ ] The reveal cannot run at the same time: `screenRevealActive` is the only check
      used by `armReveal`/`playReveal`.
- [ ] Nothing else reads `AppPreferences.screenRevealEnabledState` (it is now read in
      exactly one place).
- [ ] The switch's off-state is byte-for-byte the old behaviour: with the experiment
      off, none of the `sharedAxis*` factories are called.
- [ ] Turned on, a push, a pop, a tab switch, a settings sub-page, Topic Reveal and the
      Entry Detail each look right **and** the back gesture scrubs the page.
- [ ] A predictive-back *cancel* (drag out, release, do not cross) settles back with no
      leftover offset.

**Unverified in this environment:** everything above that needs a device. CI compiles
the app; it cannot see a gesture. The member's own eyes are the last step of every
phase — that is why the switch exists.

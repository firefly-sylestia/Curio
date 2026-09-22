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
| The **seeked** curve | `CurioMotionSystem.Track` = linear — handed to navigation's predictive-pop parameters (`predictivePop*X/Z/Fade`) | **shipped (phase 2)** |
| Panels (phase 3) | `PanelEnterMs` / `PanelExitMs` on `Settle`, adopted by the reader's `ReaderSheetFrame`; Material's own sheets wait for M3 1.5 | **part shipped** |
| Item ADD | `Modifier.curioItemIn(key, order, arrivals)` — drawn in the draw phase, staggered, plus `CurioArrivals` so a lazy list animates a row once | **shipped** — Home's recents, the Cabinet grid, the journals list |
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

**Phase 2 — the predictive-pop overload (SHIPPED, v455).**
`navigation-compose` moved 2.9.8 → **2.10.1** and the NavHost now passes
`predictivePopEnterTransition` / `predictivePopExitTransition`, whose lambdas repeat the
X / Z / fade pop shapes on the **linear** `CurioMotionSystem.Track` curve — Felicity's
"linear while seeked, decelerate when free" rule, at last wired. The gesture therefore
moves the page 1:1 with the finger and only *settles* on release. With the experiment
OFF the lambdas hand back `DefaultNavTransitions.predictivePopEnterTransition` /
`…ExitTransition` (the library's own defaults — exactly the behaviour of not passing
them).
**The pairing was verified before the bump** (this is the part that has to be right):
`navigation-compose:2.10.1` declares `compose.animation/ui/runtime 1.10.5`, and the
BOM `composeBom = 2026.05.01` pins **1.11.2** — the resolved compose is *newer* than the
library asks for, which is the safe direction (the reverse links against APIs the
runtime does not have, and that fails on a device, not in CI). The API names and
signatures were read from `navigation-compose`'s own public API surface: the predictive
parameters are `AnimatedContentTransitionScope<NavBackStackEntry>.(swipeEdge: Int) ->
EnterTransition/ExitTransition`.

**Phase 3 — sheets and dialogs (PART SHIPPED, v455; the rest is GATED ON MATERIAL 3).**
A sheet is a screen that does not change route.

* **Shipped:** the panel clock — `CurioMotionSystem.PanelEnterMs` (= `NavMs`, so a sheet
  and a screen arrive together) and `PanelExitMs` (deliberately shorter: a member who
  dismissed a sheet has decided), both on the `Settle` curve. The reader's
  `ReaderSheetFrame` is re-timed with them — the app's largest custom panel family
  (dictionary, ⋯ menu, notes, highlights, contents, marks) — while its *structure* is
  untouched: it still travels by its own measured height, and a finger on the sheet is
  still never interpolated. OFF keeps the pill clock's 190/130ms pair, which was tuned
  for small floating pills.
* **GATED, and the exact blocker:** the app has ~140 `ModalBottomSheet` call sites whose
  show/hide motion we do **not** own — Material 3 animates them itself through
  `MaterialTheme.motionScheme` (`ModalBottomSheet.kt` asks for `DefaultSpatial` /
  `FastEffects`). Handing the theme a scheme of ours would re-time every one of them at
  once, in one line, with no call site touched. **In Material3 1.4.0 (this BOM)
  `MotionScheme`, `LocalMotionScheme`, `MaterialExpressiveTheme` and the
  `MaterialTheme(motionScheme = …)` parameter are all `internal`** — the public API is
  1.5+, and 1.5 is not stable yet (1.5.0-alpha28 at the time of writing).
  **When M3 1.5 goes stable:** write the scheme (six methods; each a `tween` on
  `CurioMotionSystem.Settle` at the 300 / `NavMs` / `SLOW_MS` clocks, spatial and
  effects on the *same* clock so a panel and its scrim arrive as one object), pass it in
  the one `MaterialTheme(...)` call in `CurioTheme` when the experiment is on, and hand
  `MotionScheme.standard()` over when it is off. Do not reach for 1.5.0-alpha to get
  this early: it is the app's top-level theme.

**Phase 4 — the item animators (SHIPPED for the lists that suit it).**

* **The rule a lazy list must obey (`CurioArrivals`).** On a `Column` an item that is
  composed is present, so `remember(key) { Animatable(0f) }` is enough. On a
  `LazyColumn`/`LazyVerticalGrid` an item is composed **when it scrolls into view** — so
  the primitive alone would make every row animate again on every scroll back to it. That
  is a stutter, not an entrance. A lazy list therefore declares one `rememberCurioArrivals()`
  and hands it down: **a key animates the first time THAT list sees it and never again.**
  The record is deliberately not snapshot state — nothing draws it.
* **Adopted:** Home's recents (a scrolling `Column`, so nothing extra was needed), the
  **Cabinet grid** (`itemsIndexed` + the arrivals record), and the **journals list**
  (`JournalRow` gained a `modifier`). All three are lists a member adds to and returns to,
  which is where an arrival is informative.
* **Deliberately NOT adopted — the Topic Database.** 16k rows that mix topic rows with
  section headers: an entrance on the headers would make the catalog pop as the member
  scrolls, and per-row animation work is the last thing a 16k-row list needs. The plan's
  own question (“does it want this?”) is answered **no**.
* **Still open:** the REMOVE animation and the flip (a content swap in place). REMOVE is
  not ported at all (see §5); the flip has no natural home yet and a 3D rotation in a list
  reads as a glitch.

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

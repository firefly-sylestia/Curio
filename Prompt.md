# Prompt — FieldMind/Curio request log

## Active request: REAL cause of the blank Topic Reveal + CI compile fixes

### Symptom (user-confirmed)
Topic Reveal shows ONLY the two dock buttons ("Already watched" / "Start
exploring") — everything else on the page is invisible. User confirmed it was
STILL blank after the previous containerColor revert (4d70eba). User also
reported CI compile failures on the pet commit (5f6e41c).

### THE REAL ROOT CAUSE (found this pass)
The containerColor theory from 4d70eba was WRONG (or at least incomplete). The
actual trigger is a **dock height mismatch**:

- The reveal dock lives in the Scaffold bottomBar slot — OUTSIDE
  `SharedTransitionLayout`, which is why the two buttons render while the whole
  page (inside the layout) is invisible.
- The NavHost reserves the bottom bar's measured height (80dp — M3 NavigationBar
  consumes the nav-bar inset inside its 80dp min height) via an invisible
  placeholder so Scaffold innerPadding stays CONSTANT across the Spin→Reveal
  shared-element morph.
- af10023 changed the dock from the working fixed `height(80.dp)` (as in
  untitled-chat) to `heightIn(min = 80.dp) + windowInsetsPadding(navigationBars)`
  = 80dp + nav inset (~104dp). When the reveal's dock swapped in for the
  placeholder MID-transition, innerPadding grew by the inset →
  `SharedTransitionLayout` re-laid out mid-morph → the shared-element animation
  froze → the entire reveal page stayed invisible forever.
- 4d70eba removed the containerColor change but KEPT the height mismatch → still
  frozen. This pass fixes the height.

### Fix (implemented)
1. **TopicRevealScreen.RevealActionDock** — `height(80.dp).windowInsetsPadding(
   navigationBars)`: fixed 80dp TOTAL with the nav-bar inset consumed INSIDE
   (height first, inset as internal padding — same as M3 NavigationBar), so the
   dock exactly matches the reserved placeholder/real bar → innerPadding NEVER
   changes → the morph completes and the page renders. Buttons still clear the
   gesture bar; compact button padding (vertical 10dp) so they fit the
   inset-aware content area.
2. **CurioNavHost.RevealBottomBarPlaceholder** — fallback (unmeasured) branch
   also `height(80.dp).windowInsetsPadding(...)` so placeholder → dock swap is
   height-constant in every path (deep-link straight into Reveal included).
3. Removed now-unused `heightIn` imports from both files.

Everything else the user asked for is preserved: transparent buttons with
category ink, Already/Undo left + Start right, category undo labels,
wash-backed dock (no cream flash), constant Scaffold containerColor.

### CI compile fixes (pet commit 5f6e41c — was RED)
- CurioPassport.allProgress: `associateWith` produced Map<CurioCategory,…>;
  now `associate { it.id to progress(...) }` → Map<CategoryId,…>.
- QuestsScreen: added `import com.curio.app.ui.theme.themedAccent` (cleared the
  unresolved-reference cascade at 126/927 + the bogus Color&Map.Entry errors at
  948–955 which were compiler recovery from the error type).
- CurioPetSprite: `-hop.value * 10.dp * (...)` → `10.dp * (-hop.value) * (...)` —
  this Compose has no Float×Dp operator; Dp×Float is fine.

### Validation
- Brace balance equal on all touched files, `git diff --check` clean, no
  Float×Dp left, `heightIn` imports removed cleanly, Kotlin 2.3.21 (Stage.entries
  OK). Code review passed with 2 notes: (a) on 3-button-nav the 80dp total leaves
  ~32dp content — compacted button padding mitigates; (b) hardcoded 80dp vs the
  measured placeholder assume the M3 bar is 80dp — consistent with the NavHost's
  own documented assumption.
- No Gradle builds in this environment (project rule) — CI validates on push.

### Next
- Commit + push to Alpha (updates PR #3), then USER MUST REBUILD from the latest
  Alpha (CI was red on 5f6e41c, so any build they tested before this push was
  either 4d70eba or older). If the blank persists on a FRESH build, the remaining
  suspects are the hero key(resolved?.id) remount re-registering the shared
  element mid-morph and device-specific bar heights (thread bottomBarHeightPx
  into the dock).

# Prompt — Curio request log

## Active request: v8.13 — smart pet + real badges + silent-explore smarts (uncommitted)

Two stacked batches, still in the working tree (NOT pushed — user wants to be
asked before pushing):

**A. v8.12 (pending, from the previous request)**
- Silent "Explore" buttons on Topic Database rows + browse-mode reveal
  (`openSilentExplore` in ExploreSession.kt) — pure out-of-app search, no
  quests/XP/recents/done-mark/timer.
- Tutorial: capture step's pill lifted above the Save button; wait steps
  (Explore, Capture, Save) become skippable ("Skip for now"); overlay moved
  to a LOWER position + skip action wired through QuestGuideToast/NavHost.
- Pet: EXCITED/PROUD are one-shot bursts tied to the hop (no more 60-90s
  stuck reactions); personality (CUDDLY/BOUNCY/EXPLORER/SPARKY) built from
  persisted boop/play/explore counts feeding `playfulBias`.

**B. v8.13 (this request)**
1. **CI fix** — CurioPetSprite.kt had `val auraColor = … else accent` glued
   to `Box(` (broken merge) — restored the newline; that single glitch caused
   the whole cascade of unresolved-reference errors.
2. **Smarter pet** — `CurioPet.leastExploredLane(context, explored)` now also
   requires the passport stamp to be UNSEEN: a lane the user peeked/explored/
   saved ANYWHERE (including via the silent buttons) is never called
   "haven't tried". `openSilentExplore` now feeds `CurioPassport.noteExplore`
   (engagement awareness only — zero quest progress/XP), which also stops the
   "New lane" discovery daily from suggesting already-tried lanes.
3. **Badge shelf upgrade** — BadgeTile is now a round MEDAL: per-chain color
   gradient + inner ring + gold check when earned; every stage wears its own
   glyph (`badgeGlyph`, 48 stage ids + QuestKind fallback, all proven in the
   bundled font); earned badges show the badge IN FULL ("Earned · +XP", no
   progress bar, no task text); locked badges are silhouettes with progress.
4. **User follow-up (asked before push)** — silent explores now ALSO award
   the tiny exploration XP (`CurioQuests.awardXpOnly(context, 5)` — XP +
   pet mood timestamps only, chains/dailies/recents/done-mark untouched), and
   the passport stamp now treats a SPIN as a peek (`spins > 0 -> PEEKED`), so
   the WILDCARD lane (which only ever accumulates spins — its reveals/
   explores/saves resolve to real categories) finally shows progress on the
   passport instead of "New · spin!" forever.
4. **Pet polish** — hearts moved OUT of the pet box into a sibling
   `HeartsOverlay` above the head (never covers the face; clamped so it can't
   fly off-screen at the top, lifted clear of the speech bubble); eyes moved
   up one row + gentler squash (bowDip 4dp, bowSquash 0.045, spinPulse 0.03)
   so the eyes/mouth never join each other or the scarf; CAT EARS added to
   the 16×16 grid (rows 1-2); blush is now celebration-only (excited/proud/
   bouncy/play/spin — NOT plain idle happy); two new moods — FOCUSED (capture
   screen: quiet, encouraging) and BOUNCY (within 5 min of a play session);
   `CurioPet.spinning` + `setSpinning()` hooked around the SpinScreen shuffle
   → the pet cheers the reeling deck ("Go, go, go!") with a star-eyed wide
   smile + wiggle.

### Validation
- Balance ALL OK on all touched files; `git diff --check` clean.
- Code review applied: blush excluded from idle-HAPPY, hearts overlay clamped
  + lifted, capture routePrefix verified (`substringBefore("/")` → "capture").
- User approved the commit + push after being asked.

---

## History (committed)

### v8.11 — playful pet: escalating touch, play-bow + spin, self-initiated games (`fb98b49`)
- `CurioPet.touchReaction(tier)` escalating pools (boop → playful → zoomies);
  `playInitiation()` lines; sprite playKey/spinKey one-shots (bow + 360°
  twirl), mid-play face + faster tail wag; floating pet tap-streak escalation,
  playful darts, ~12% self-started games from `playfulBias`, downward-biased
  wander with think pauses, 1.5s reaction window.

### v8.10 — pet polish: one coral theme, happy smile, chubby, send home, no duplicate pet (`eb5820e`)
- ONE fixed color everywhere (no category pastels, no dark-mode twin); smile
  flipped from frown; chubby body; glow disc + XP ring removed; long-press
  sends it home to sit in the bed; `dialogOpen` hides the floater during
  dialogs — never two pets on screen.

### v8.10 (quest) — discovery daily completes on spinning the target lane (`fe9c336`)
- `CurioQuests.onSpin(context, categoryId)`; spinning the passport's
  least-engaged lane (Wildcard included) completes the "New lane" daily at
  spin time — the Wildcard merge could never complete via topic-open.

### v8.9 — pet: extra idle behaviors, per-screen reactions, cuter sprite (`5e5a4f6`)
### v8.8 — touchable floating pet + flower bed (`f1869d6` + CI fixes)

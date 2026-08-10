# Prompt.md — Request Log

## Current Request (COMPLETED): Pet fixes (spin-dialog loop, walk-to-button, tiny keyboard) + approved Spin pool-loading fix

**Date:** 2026-08-10

### What the user asked
1. Implement the previously approved Spin "Nothing here yet" pool-loading fix.
2. Make the pet keyboard 5× smaller (a tiny thing).
3. Fix pet interaction: wrong dialogs, not walking up to buttons (interacts from far).
4. Fix the pet stuck in a loop of Spin-page dialogs on other screens.

### Root cause (all three pet issues = one bug)
`CurioPet.spinning` gets STUCK true: SpinScreen flips it in `LaunchedEffect(shuffleCount)` (true at start, false at settle); leaving mid-spin cancels the effect so `noteSpinning(false)` never runs. With the flag stuck: the pet's spin-cheer effect fires on EVERY screen (no screen gate) → the "loop" + "wrong dialogs"; the landmark-poke branch is gated on `!spinning` → the pet never walks to buttons; spin-flavored lines fire from anywhere.

### Changes made
- `SpinScreen.kt` — `DisposableEffect(Unit) { onDispose { CurioPet.noteSpinning(false) } }` guarantees the flag resets whenever Spin leaves composition.
- `CurioFloatingPet.kt` — spin cheer gated on `watching` (Spin screen only); landmark pokes now walk to the landmark's NEAREST EDGE (window→local bounds conversion, nearest-side stand, vertical-center align, small gap) and only poke if the pet actually arrived (interrupted drag/glide can never interact from afar).
- `CurioFloatingPet.kt` — typing keyboard: new `TYPING_SCALE = 0.2f` + `scale` param on `TypingKeyboard` (scales the canvas size AND the base dp unit uniformly); call-site size/offset scaled.
- `SpinScreen.kt` (approved plan) — pool seeded from `TopicJsonLoader.cached()` (warm return = instant deck); `poolLoading` flag distinguishes loading vs genuinely-empty; Carousel shows new `DeckLoadingHint` ("Gathering the deck…") while loading, `EmptyPoolHint` only when truly empty; load wrapped in runCatching; threaded `poolLoading` through SpinDeckSection + 3 call sites.

### Validation
- Brace checks on both edited files (to run), `git diff --check`, import audit (no new imports added; Rect/overlayOrigin already in scope in CurioFloatingPet).

---

## Prior requests (archive)
- Spin button shrinks during shuffle, orbit dots stay put (`5532bb7`).
- Release analysis + remove the Spin landing FX experiment (`c1369e4`).
- Fix CI compile errors (missing `AppPreferences` imports) + reveal tear navbar footprint (`ccf4a3f`).
- Fix `CategoryEdgeShine` Density argument (`b57c0f7`).

**Date:** 2026-08-10

### What the user asked
"Make the spin button smaller during the animation and keep the outside small balls at their place." (Clarified: the button is already round — the ask is about the shuffle-time animation.)

### Change made
- `SpinScreen.kt` — `buttonPulse` target during shuffle flipped from **grow 1.06 → shrink 0.92** (`if (shuffling) 0.92f else 1f`). The button plate now tucks in while the `OrbitRing` dots keep their fixed radius (the ring lives on the unscaled 176dp container), so the spin reads as the center pulling away from the living ring. The `0.92` value sits inside the existing `.scale(pulseScale.coerceIn(0.9f, 1.10f))` clamp. Rest state unchanged (1.0).

### Validation
- `check_braces.js` OK, `git diff --check` OK, diff is a single targeted hunk.

### Notes
- The Home screen's small Shuffle button has no orbit ring — untouched.

---

## Prior requests (archive)
- Release analysis + remove the Spin landing FX experiment (`c1369e4`).
- Fix CI compile errors (missing `AppPreferences` imports) + reveal tear navbar footprint (`ccf4a3f`).
- Fix `CategoryEdgeShine` Density argument (`b57c0f7`).

## Historical (pre-analysis) requests

**Date:** 2026-08-10

### What the user asked
"We are near release — analyse the full app and give me a full analysis of what should be refined and what else. Also we are removing the fx option and not implementing it."

### FX experiment removed (decision: NOT implementing)
The v9.1 "Spin landing FX" experiment is gone entirely; the classic spin feel (the pre-experiment shipped default) is hardcoded:

- `AppPreferences.kt` — deleted the 4 `KEY_SPIN_*` constants, 4 `spin*FxState` vars, 4 `initThemeMode` seed lines, and the 8 getter/setter functions.
- `ExperimentsScreen.kt` — deleted the whole "Spin landing FX" item (master switch + Buttery reel / Spring catch / Sparkle burst sub-switches).
- `SpinScreen.kt` — restored the classic path byte-for-byte (exactly what ran with the master toggle OFF): sine ease-out reel + 340→520ms interval, `tickPulse.snapTo(1.02f)` + `spring(0.85, 420)`, `CurioMotion.Springs.Deliberate` settle (no 0.97 catch squish), and removed the sparkle burst + catch glow Canvas layers, their `LaunchedEffect(landed)` + Animatable state, and the now-unused `StrokeCap` import. Brace structure after removal matches original nesting.
- `fastlane/.../changelogs/20260915.txt` — removed the FX bullet (the store listing has never shipped; no changelog may advertise a feature that will never exist).

### Validation
- `scripts/check_braces.js` passes on all 3 edited Kotlin files (SpinScreen's hero-card closers were repaired to 28/24/20/16/12/8/4/0 after the FX-canvas removal).
- No stale FX references anywhere in `app/src/main/java` (only the unrelated `KEY_SPINS` in CurioPassport remains).
- `git diff --check` clean. Import audit: only `StrokeCap` became unused → removed; every other symbol still has uses elsewhere.
- Code review (deepseek-flash) passed — flagged + fixed the 20260915 changelog mention.

### Release-readiness analysis (delivered to user — full detail in chat)
- **CI**: last fix (`b57c0f7`, Density in `CategoryEdgeShine`) was in progress when this request started; verify green before release.
- **versionCode/changelog mismatch**: `versionCode = 20260918` but latest staged changelog is `20260919.txt` — bump versionCode to 20260919 (or rewrite the changelog) before the release tag.
- **Play Store listing missing**: `fastlane/metadata/android/en-US/` contains only `changelogs/` — no title, short/full description, screenshots, feature graphic, or data-safety form. If Play is a target, this is the biggest blocker; the release workflow only publishes GitHub releases today.
- **Permissions / Play policy**: RECORD_AUDIO (data-safety declaration + audio-recording policy), SYSTEM_ALERT_WINDOW (overlay bubble — must be declared), FOREGROUND_SERVICE_SPECIAL_USE (special-use declaration + Play Console rationale), POST_NOTIFICATIONS.
- **Dead / unfinished code**: `ReelNotesFormat.kt:221` has a dead `onClick = { /* TODO Phase 4: open lightbox */ }` — wire the Lightbox or drop the tap affordance.
- **Experiments screen cleanup**: many default-OFF experimental toggles (peek deck redesign, hero redesign) — per repo rules, decide winners and hardcode/remove before 1.0, or they ship as settings clutter.
- **app/AGENTS.md is stale** (says Phase 2 / "no persistence yet" / 6 categories / 11 placeholder stubs — all outdated) — DOX pass recommended.
- **Update checker** (Support screen) queries GitHub releases — fine for GitHub-distributed builds; consider gating for a store build.

### Notes / follow-ups
- Watch CI for the Density fix + this FX-removal push.
- Decide: versionCode bump, Play listing work, Experiments close-out, ReelNotes lightbox TODO.

---

- Material theme adopts device colors with category accent shine (`b8e3b7c`, `c151f1d`).
- AMOLED theme polish — pitch-black cards + edge shine + unified heroes (`b351b42`, `e38a6c3`).
- Theme-aware tear strip on the reveal (`ddec939`, `b74c7f5`).
- Pet-led tour completion + reveal torn bottom edge (`5464cd4`, `1265c75`).
- Pet starts at its home, redesigned speech bubble, scaled ride cloud (`51987cc`, `c38c4fd`).

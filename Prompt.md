# Current Request — v208f: ring mirror, pill vanish-on-back, smaller constellation dots, drawer footer flush

## Status: DONE (committed, pushed)

## Request (user, verbatim)
"see the svg its inverted of what its in the app youre plaing it wrngly fix it. and why th elike and dislike pill now staying longer make it vanish like before just when i tap back from the reveal screen. the open is fine for now. and make the costeellation dots smaller they are too big give it a size limit"
"also please why my drawer footer is floating?? please fix it and the collapsed options is sghowing behind the drawer, fix it, and also cut the footer from button 44 units the footer svg cut it from buttom and place it properly"

## What changed
1. **Ring mirrored** — PaperStatCard CoilOutlineNorm + CoilSpecularNorm
   flipped to match the SVG's `matrix(-1,0,0,1,0,0)`: start bottom-right
   (inside the hole), arch over the top, LEFT leg dives past the card's
   left edge to an open round-capped end.
2. **Sentiment pill vanish-on-back** — NavHost SentimentPillHost overlay
   gated on `isRevealRoutePrefix`; the pill disappears the moment you tap
   back (route flips before the exit transition ends).
3. **Constellation dots** — radius `4.5+sqrt(count)×2.4` capped 12dp
   (was 5.5+sqrt×7 capped 60 → dots ballooned to ~120dp).
4. **Drawer footer** — (a) footer Box gets `navigationBarsPadding()`
   (sheet is edge-to-edge; the art's bottom hid behind the gesture bar
   → floating; now flush above it; credits' own navBarPadding removed);
   (b) `drawer_footer.svg` viewBox 760 → 716 (cut 44 units from the
   bottom, per the user's explicit instruction). v207 structure
   (footer in normal flow below the weight(1f) list) confirmed intact.

## Files
- `ui/components/PaperStatCard.kt` (mirrored coil + specular)
- `navigation/CurioNavHost.kt` (pill slot gated on reveal route)
- `ui/components/CurioConstellation.kt` (dot size limit)
- `features/home/HomeScreen.kt` (footer navBarPadding + credits)
- `app/src/main/res/raw/drawer_footer.svg` (viewBox 760 → 716)
- Docs: `app/AGENTS.md` (v208f), changelog ADD bullets, this Prompt.md.

---

# Previous — v208e: nav pill syncs TO the Like/Dislike + pill z-index above the nav pill

## Status: DONE (committed, pushed)

## Request (user, verbatim)
"no bro the like and dislike starting time was fine i just asked you to tune the navpil home one to sync properly. and also how about place the like and dislike pill z index above the home nav pill. so it looks even smoother. and also keep it overlap"

## What changed (rejected v208d's pill-delay; different approach)
- REVERTED the v208d `sentimentPillEntered` gating — the Like/Dislike
  keeps its natural start time (t≈0, 220ms slide).
- TUNED THE NAV PILL instead: `FloatingNavCollapseHoldMillis` 460 → 240ms
  (the pill's 220ms entrance + a hair) — the bar vanishes right as the
  pill lands.
- Z-INDEX: new `SentimentPillHost` (CurioRoutes.kt, LightboxTarget-style
  out-of-band object) holds the pill composable; the reveal registers it
  in a `SideEffect` (+ `DisposableEffect` clears on route leave) and the
  NavHost composes the slot AFTER the floating bar — the Like/Dislike
  draws ON TOP of the collapsing nav pill during the overlap. Slot's
  wrapper Box has no pointer input (touches pass through); the pill's
  scroll hide/show is captured by the lambda.

## Files
- `navigation/CurioRoutes.kt` (SentimentPillHost)
- `navigation/CurioNavHost.kt` (slot after the bar + hold comment)
- `ui/components/CurioBottomNav.kt` (constant 240)
- `features/reveal/TopicRevealScreen.kt` (revert + portal)
- Docs: `app/AGENTS.md` (v208e), changelog ADD bullet, this Prompt.md.

---

# Previous — v208d: nav pill vanish syncs with the Like/Dislike appearance

## Status: SUPERSEDED by v208e (user rejected the pill-delay approach)

## Request (user, verbatim)
"the nav pill collapse is fine, but the vanishing can be done more better, like the exact moment the like and dislike appears?"

## What changed (v208d, now reverted)
- Shared constant `FloatingNavCollapseHoldMillis = 460`; NavHost used it;
  the pill's first entrance was gated on the same hold. User said the
  pill's starting time was fine — REVERTED; the nav pill is tuned instead.

---

# Previous — v208: CURIO BRAIN STATS — science-based cognitive system replaces constellation save-count stars

## Status: DONE (committed, pushed)

## Request (user, verbatim)
"a fresh system of my on, i want a real science based stats system that will help user improve their brain in a certain ways and it shows the knowledge based on the category user explored and the amout of wrting user does etc etc, and this will be rplaced the category stars glow from costelation"

## What changed
1. **New `data/BrainStats.kt`** — the science-based model, computed from REAL data (passport per-lane counters, saved captures, lifetime counters, streak):
   - `LaneKnowledge` per lane: saves/explores/spins/words + `score = explores*30 + saves*40 + words/20 + spins*2`, `lastAt` drives glow, `explored` flag.
   - `laneKnowledge(progress, entries)` merges passport counters with words written per lane from saved captures.
   - `CaptureData.wordCount()` — real words across every text field (journal, review, notes, field notes, captions, quotes), recursive through portfolios + wildcard notebook; voice transcripts NOT counted.
   - `brainProfile(...)` — six cognitive dimensions, each mapped to a real learning-science mechanism: Knowledge (breadth×depth), Memory (saves+pins+quotes → retrieval practice), Expression (words → generation effect), Focus (explores + daily quests), Consistency (best streak → spacing effect), Curiosity (spins + lanes sampled). 0–100 score, level label (Awakening → Mastered), science-based improvement tip.
2. **Constellation fed by knowledge** (both the drawer AND the Your Curiosity page — shared component, can't drift):
   - `laneCounts = knowledge scores`, `laneRecent = lastAt`.
   - Drawer popover now shows "N knowledge" instead of "N saved".
   - `CurioConstellation` star radius switched from linear `min(count,60)*0.3` to sqrt-scaled `5.5 + sqrt(count)*7`, capped 60 — knowledge scores run far higher than saved counts and would have pinned every star at max.
3. **Your Curiosity page** — new `BrainProfileCard` (6 color-coded dimension bars + level labels + tips + words-written line); constellation card copy updated ("bigger means more knowledge built there", "Knowledge score N", chip label "knowledge").

## Files
- NEW `data/BrainStats.kt`
- `features/stats/StatsScreen.kt` (BrainProfileCard + knowledge feed + copy)
- `features/home/HomeScreen.kt` (drawer knowledge feed + popover)
- `ui/components/CurioConstellation.kt` (sqrt star radius)
- Docs: `app/AGENTS.md` (v208), changelog ADD bullets, this Prompt.md.

## NOTE
Pushed. (v206 splash + v207 were pushed earlier in the session.)

## CI FIX (v208b) — pushed `ea44bba`+1
- `BrainStats.kt`: `CaptureData`'s subclasses are NESTED — the
  `wordCount()` `when` needed `CaptureData.`-qualified branches.
- `SplashScreen.kt`: wordmark gradient `brush` moved into
  `style.copy(brush = …)` (Text has no `brush` param).

## RING SVG v3 (v208c) — pushed `d5f57f4`+1
- User supplied `svgviewer-output (15).svg` (the new coil reference).
  `CoilOutlineNorm` in PaperStatCard.kt now matches it EXACTLY: no left
  hook (start at the box's bottom-left corner, round cap), arch up/over,
  and the right leg DIVES below the box through the hole to an open end
  at (0.712, 1.342) — the old blunt stop at (1.0, 0.737) is gone.
- Verified numerically: start protrudes 6.6dp past the card's left edge;
  the dive threads the hole; the open end stays inside the card.
- Colors untouched (user: "later we will tweak its color"). The dump SVG
  was removed from the repo (git rm).

---

# Previous — v207: drawer footer float + About-behind-footer, coil left-end hook, stats sun/moon, smoother nav collapse, Like/Dislike height

## Status: DONE (committed, pushed)

## Request (user, verbatim)
"why the footer is floating now and when the about gets expanded its behind the footer. also the 3d hole is good. now mak the left end the side its out curve a little so it looks seemless connected also in your curiocity page place the drawing of sun and moon a little below the start bar just the moon and the sun not the whole drawing. and only in your curiocity page as a separate maybe. and the collapse of home nav pil can be more smoother, and the like and dislike size still doesnt match with home nav pill, like its height"

## What changed
1. **Drawer footer** — v203 pinned the footer as an OVERLAY over the list tail; expanded sections slid under its fade and it read as floating. Now `HomeDrawerContent` wraps the rows in a Column: the list sits in a weight(1f) Box ABOVE the footer, which is in normal flow pinned to the sheet bottom — never floats, rows never hide behind it.
2. **Coil left end** — prepended a small hook (moveTo 0.16,1.34 → cubic into 0,1.0) to `CoilOutlineNorm` in PaperStatCard.kt, so the protruding left end curves like a wire wrapping around the card edge.
3. **Stats sun/moon** — new private `StatsCelestialBody` on the Your Curiosity page only: gold sun + glow in light, cream crescent carved by the sky mid-tone in dark (mirrors the SVG construction), floating just below the status bar (TopEnd). Not interactive.
4. **Nav collapse smoother** — pill family 150 → 120 stiffness; NavHost hold 420 → 460ms; Reveal + Pet Studio springs follow to 120 (lockstep).
5. **Like/Dislike height** — sentiment capsule Row padding 7 → 8dp, gap 6 → 10dp → capsule 68dp tall, exactly the nav bar's capsule.

## Files
- `features/home/HomeScreen.kt` (drawer Column restructure)
- `ui/components/PaperStatCard.kt` (coil hook)
- `features/stats/StatsScreen.kt` (StatsCelestialBody)
- `ui/components/CurioBottomNav.kt`, `navigation/CurioNavHost.kt`, `features/reveal/TopicRevealScreen.kt`, `features/petdesigner/PetDesignerScreen.kt` (springs + capsule padding)
- Docs: `app/AGENTS.md` (v207), changelog FIX bullets, this Prompt.md.

---

# Previous — v206: splash redesign (bigger gradient wordmark, warm tagline, bottom ground)

## Status: DONE (committed, pushed)

## Request (user, verbatim)
"okay heres the redesigned spalsh screen, make the Curio tet bigger and with gradient basced on the dark or light mode. and at the buttom it have a similiar gradient backgroud of the app backgroud. not full just at the buttom and also discover something that text gets a little bigger too and warmer in dark mode light mode you figure it out."

## What changed — `features/splash/SplashScreen.kt`
1. **Wordmark**: "Curio" 36 → 72sp (same Geom Bold displaySmall), painted with a theme-aware horizontal gradient echoing the cosmic mark: dark = SkyMint → ButterYellow (bright on the dark sky); light = CoralInk → GoldInk (deep, readable on the cream).
2. **Bottom ground**: a bottom-anchored band (bottom 34% of the screen) fading transparent → warmed app-background tone (dark: `lerp(background, CoralBlush, 0.05)`; light: `lerp(background, ButterYellow, 0.14)`). Not full-bleed.
3. **Tagline**: 14 → 18sp, warmer in both themes — parchment #D8CDB4 on dark, warm khaki #7E6E50 on light (replaces cool onBackground @ 0.62).
4. Unchanged: logomark + shimmer, animated halo, 3-dot loader. Brace/paren-balanced.

## Files
- `features/splash/SplashScreen.kt`
- Docs: `app/AGENTS.md` (v206), changelog ADD bullet, this Prompt.md.

## Also in this session (already pushed)
- v205 (`0e638f8`): app-size diet — release APK ~20MB smaller via `ndk.abiFilters` arm-only (x86/x86_64 emulator-only Vosk libs dropped from release; debug keeps all ABIs; release.yml hard guard now expects universal + 2 arm splits).

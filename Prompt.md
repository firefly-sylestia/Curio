# Prompt — Curio request log

## Active request: v8.14 — pet home/sleep + time-of-day + 4 AM rollover (uncommitted)

Working tree (8 files, NOT pushed — user wants to be asked before pushing):

**User choices (from ask):** cozy bed upgrade (headboard + blanket + lamp),
nightcap on the sleeping pet, moon/stars + sun backdrop, dream bubbles while
asleep; curled-up lying pose, pronounced Z's + breathing, sleepy twitches,
sleep-startle twitch (random dynamic ones); 4 time-of-day phases; pet sleeps
at night, wakes for morning, time-aware pet lines, morning-energy vs
night-lazy behavior; **daily quests reset at 4 AM**.

1. **CI fix** — `CurioPet.var spinning ... private set` emits a JVM
   `setSpinning(Z)V` setter that clashed with the explicit
   `fun setSpinning(value)` (Platform declaration clash). Renamed the
   function to `noteSpinning(value)` and updated both SpinScreen call sites
   (682, 726). No `setSpinning` references remain.

2. **TimeOfDay (CurioPet)** — new 4-phase enum (MORNING/AFTERNOON/EVENING/
   NIGHT) read from `Calendar`; `wakeForMorning()` clears the sleepy flag;
   `playfulBias(context)` scales energy by the hour (morning high, night
   low); `lineFor()` gained time-aware copy ("Good morning!", "Night-night
   soon…", etc.).

3. **4 AM daily rollover** — `CurioQuests.todayEpochDay()` and
   `StreakTracker.todayEpochDay()` now truncate at 04:00 (not midnight) so a
   late-night session never wipes the day's quests mid-celebration; Quests
   daily header now says "Resets at 4 AM". Grep confirmed no other
   midnight-based "today" math remains.

4. **Sleep animation (CurioPetSprite)** — curled-up lying pose (new grid
   rows, all 16 chars, every char mapped), periwinkle nightcap with cream
   trim, pronounced breathing swell, floating Z's, random sleep-STARTLE
   (tiny jump + eyes flash open, on a random 9–22s beat while asleep) and
   sleepy twitches; `sleeping = SLEEPY && !moving && !dragged` gate.

5. **Flower bed diorama (CurioFlowerBed rewrite)** — time-of-day sky
   (warm morning / blue afternoon / amber-evening gradient / moonlit night),
   sun or crescent moon in the corner, twinkling stars at night (real
   infinite-transition phase), cozy bed with headboard + coral blanket +
   lamp glow (stronger at night), grass base, and a dream bubble cycle
   (symbol pops, rises, fades every ~6s). Fixed a missing
   `foundation.background` import.

6. **MainActivity** — calls `CurioPet.wakeForMorning()` on launch (import
   added).

**Validation** — balance + whitespace clean; sprite imports (LinearEasing/
spring/delay/tween) present; no unmapped sprite chars; 16-char grids; code
review applied (only concrete fix was the missing background import, which
is in). CI runs on push.

## Done previously (pushed)

- **v8.13** (`04beae0`) — smarter pet (passport-aware leastExploredLane),
  medal badges (per-stage glyphs, earned-in-full state), silent-explore +5 XP
  + wildcard passport peek fix.
- **CI fix** (`d0669a5`) — hoisted LocalContext out of the silent-explore
  lambda in TopicDatabaseScreen; declared `blushing` after
  excited/proud/playing in the sprite.

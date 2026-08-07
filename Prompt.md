# Prompt — Curio request log

## Active request: v8.10 — fix the stuck "spin Wildcard" discovery quest

User reported: the "New lane" discovery daily is bugged when it targets the
Wildcard lane — there is no proper Wildcard category (Wildcard is a merge of
all real categories), so the quest can never complete. User asked: make
**spinning in that category update the quest** instead of requiring the user
to open "that topic page".

### Root cause
- `CurioPassport.leastEngaged()` iterates every visible category INCLUDING
  WILDCARD, so once the 10 real lanes are engaged it returns Wildcard (whose
  counters stay at 0 forever).
- The discovery daily then says "New lane — try Wildcard" and routes to
  `spin/wildcard`. Every wildcard spin lands on a topic from a random REAL
  category, so `noteReveal`/`noteExplore`/`noteSave` all report the real
  category — the completion check in `onExplore`
  (`categoryId == discoveryTarget.id`) can never match WILDCARD. The quest
  is permanently stuck.

### Fix (delivered, commit pending)
- `CurioQuests.onSpin(context, categoryId)` — SpinScreen now passes the spun
  lane (`activeCategory.id`, which is WILDCARD when the surprise deck is
  spun). Inside `onSpin`, if the spun category matches the passport's
  least-engaged lane, bump the DISCOVERY daily immediately — the spin itself
  completes "try a new lane", no topic-open required (spec §6.3). Real lanes
  keep the existing explore-time completion in `onExplore` too.
- SpinScreen settle: `CurioQuests.onSpin(context)` → `CurioQuests.onSpin(context, activeCategory.id)`.

### Validation
- Sole caller updated (QuestGuide.kt:26 is a doc reference only); string-aware
  balance ALL OK; `git diff --check` clean.
- Code review: change is minimal + mirrors the existing `onExplore` guard;
  no double-claim (progress overshoot harmless); `leastEngaged` cost per spin
  matches the pre-existing onExplore pattern.

### Known related (not in this fix, per user's scope)
- Endgame: with all 10 real lanes mastered, `leastEngaged` returns WILDCARD
  forever (its stamp can never advance — no hook reports WILDCARD), so the
  discovery daily is "try Wildcard" every day. Now COMPLETABLE thanks to this
  fix; if the user wants it to end, treat wildcard as mastered once all real
  lanes are mastered.
- Passport header reads "X of 11 lanes mastered" while only 10 are masterable;
  the wildcard stamp shows "New · spin!" indefinitely. Cosmetic follow-up.

---

## History (committed)

### v8.9 — pet: extra idle behaviors, per-screen reactions, cuter sprite (`5e5a4f6`)
- `CurioPet`: `Event` enum (SPIN_LANDED/REVEAL_OPEN/EXPLORE/SAVE),
  `eventCount`/`lastEvent` + `reactTo()` + cute `eventLine()` per event.
- `CurioPetSprite`: `thinking` + `watching` poses (head "?" / lifted eyes),
  slow glance, ear flick, antenna glint, shaded 16×16 grid + belly patch.
- `CurioFloatingPet`: route-aware (watches the Spin deck), thinks before
  wandering, reacts to real events (hop + line, hearts on save), fixed stale
  `watching` closure + tilt stacking.
- Real-action hooks in SpinScreen / TopicRevealScreen / SaveCaptureScreen /
  CurioQuests.onExplore; NavHost passes `routePrefix`.

### v8.8 — touchable floating pet + flower bed (`f1869d6` + CI fixes)
- Fixed one-look sprite, flower-bed home, draggable/wandering global overlay,
  `floatingPetEnabledState` toggle (default ON).

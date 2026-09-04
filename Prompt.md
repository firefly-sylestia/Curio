# Request Log — browser category Done bug + spin button size

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user, paraphrased)
After the browser-lag work, the user pointed at commit d9a376d2 as the good
reference for loading behavior and asked me to analyse + ASK before touching
anything, covering:
1. Topic Browser: search "got unselected" when a category was picked, "Also
   in" didn't show all categories, and search used to feel faster; the new
   category picker + smart search should stay.
2. Whether the Spin (shuffle) page on phones changed — the circular spin
   button looked bigger than in the reference commit (tablet redesign was
   suspected).

## What the user decided (ask_user answers)
- Ignore the earlier search/Also-in answers. The one real bug to fix:
  selecting a category in the browser's category panel (or chip) and tapping
  Done did not apply the selection.
- Spin: compare vs current; if nothing differs, just decrease the circular
  spin button size, including the size it takes while spinning.

## Analysis
- SpinScreen.kt phone path was byte-identical between d9a376d2 and HEAD
  (same 114dp idle / 98dp landed plate); the tablet redesigns were already
  reverted wholesale. So per the user's instruction the button was simply
  made smaller.
- TopicDatabaseScreen: the category panel stages picks in `pendingCats` and
  Done ran `(pendingCats ?: selectedCats).let { commitCats { it } }` — inside
  the trailing lambda `it` is the CURRENT committed set commitCats passes in,
  so Done committed the old selection and silently discarded the pending
  pick. That is exactly "select a category, tap Done, nothing happens".

## Changes
1. TopicDatabaseScreen.kt — Done now commits the pending set:
   `commitCats { pendingCats ?: it }` (returns the pending set explicitly).
2. SpinScreen.kt — spin button dialed down ~10%: plate 114/98 → 102/88
   (non-compact), 102/90 → 92/82 (compact); orbit box 176/156 → 166/146;
   casino glyph 60/52 → 54/47; shuffle glyph 72 → 64.
3. Changelog: two FIX bullets added at the top.

## Open / follow-up
- "Also in" and "search unselected" concerns were explicitly deprioritized by
  the user; left untouched. Smart search + new picker stay.
- CI (GitHub Actions) validates compilation — local env forbids Gradle.

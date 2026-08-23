# Prompt.md — current request log

## Request (complete): FULL ROLLBACK to commit 7373012

User: "revert anything after this, i meant restore to this commit state and
revert each commits that happened after this" (737301278d7d… = "fix: correct
offset import package in CurioIcons", the state right after v233).

### What was rolled back (8 commits)
- 2569b76 v234 in-screen liquid glass (Pet Designer / Home / detail local
  backdrops + "In-screen glass" toggle) — REMOVED
- 6de2d5e detailScroll hoist — superseded by the revert
- 2fbd82b v235 blob fixes + darker light-mode ink — REMOVED
- b8d43c7 v236 press-blob everywhere + studio tab bar — REMOVED (was already
  mostly reverted in v239)
- 5975838 v237 smudge revert + size-capped lens + ink — REMOVED
- c8d6e45 v238 tilt-glow top arc — REVERTED to the original v233 rim glow
- 20171ef v239 neutral indicator — REMOVED
- 55f9b68 v240 crisp spec + Profile glass + black text — REMOVED

### Method
`git checkout 7373012 -- <16 affected paths>` (all changes were modifications;
no files were added or deleted after 7373012). Verified: staged diff of
`app/src/main/java` vs 7373012 is EMPTY. `web/` untouched — including the
pre-existing uncommitted `web/package-lock.json` change.

### State after rollback (= post-v233)
- Liquid glass ONLY on the bottom nav bar (+ Reveal/PetStudio capsules via the
  global capture + v228 guard); Experiments has Liquid glass pills, Clear glass,
  Glass parallax tilt (original rim-ring glow).
- Active indicator: accent wash + press-scaled lens (v232-era look); active ink:
  classic `curioActivePillInk`.
- No touch blob anywhere; no In-screen glass toggle; native-crash reporter back
  to its v232 form.

NOTE for future requests: the user chose this state knowingly — readability ink
tweaks, indicator transparency, and Profile/in-screen glass are GONE. If asked to
re-add any, the v234–v240 history contains the implementations (local-backdrop
pattern is required for in-screen glass; never blur the indicator sample).

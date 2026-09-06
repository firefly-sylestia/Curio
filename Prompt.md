# Prompt Log — current request

## Request (2026-09-06, active → v379e shipped, v380 in progress)

Continuing the long share-card editor refinement cycle. After v379e
(`bb94ee2a`, text-first smart fit + re-fit toggle + lift unit fix +
frostInk dark-pane ink) the user confirmed in ask_user:

- ✅ Dark-on-dark Paper text is FIXED (frostInk) — confirmed.
- ✅ Quick-fact lines now wrap identically full screen vs the sheet.
- ❗ Then reported: (1) the title↔fact collision still bounces when
  moving the title to overlap the quick-fact box manually — he wants to
  be ABLE to overlap them with no problem, and to check for more issues
  in the collision logic; (2) the fact box height is not at 100% by
  default (wants the natural 100% box with text shrinking by length);
  (3) the auto-layout sparkle pill's logic "isn't right / not refined".

### v380 — collision fix (DONE, committing)

Root cause found: after ANY title drag the auto-lift effect (v376/v379e)
re-engaged the instant the guard cleared, shoving the title back above
the fact — you could never park it over/inside the box; it fought at
release and read as the bounce. Fix:

- New persisted `ShareCardMove.titlePlaced` flag. The auto-lift effect
  returns early when it is set → the lift only ever rescues a title the
  user never dragged (natural, or pushed aside by the FACT handle's own
  collision push → those still get lifted when the fact box grows).
- A TITLE drag end now folds any prior lift into `titleDy`
  (`dy -= lift`, `lift = 0`) and marks `titlePlaced = true`, so the
  title freezes EXACTLY where the finger left it — no snap on release,
  no bounce-back, deliberate overlap sticks.
- Reset layout clears the flag (title returns to automatic collision
  behaviour). Old saves parse false → unchanged behaviour.
- Files: TopicShareCard.kt (field, parse, persist, effect guard, title
  handle onDragEnd), changelog, app/AGENTS.md (v380 bullet).

### OPEN — needs user answers before implementing

1. **Default fact height not 100%.** Root cause: `autoFitShape` grows
   the box as soon as the length-shrink passes each design's TEXT FLOOR
   — and Paper's floor is 0.96, so nearly any >90-char fact grows the
   box immediately (Minimal/Editorial floors 0.82–0.85 grow from ~120
   chars). The v379d-era complaint ("Paper text too small — expand the
   box instead") pushed Paper's floor up; the v379e-era complaint wants
   the 100% box + text-shrink-first. These conflict → ask the user to
   pin the default policy (how far text shrinks before the box grows).
2. **Sparkle-pill logic refinement.** Grounded issues: pill commits
   factScale/factHeightFrac even when Smart fit is OFF; attempt 2 jumps
   straight to max-box + format change (two channels at once); on the
   RESTING preview/export a grown box can overlap the title (the auto
   lift only runs in edit chrome); no visible cycle state; aspect flip
   to 9:16 is a big jump with no soft landing. → propose refinements +
   ask.

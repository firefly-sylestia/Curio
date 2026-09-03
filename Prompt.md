# Prompt.md — current request log

## Request: Level milestone roadmap, pet-house cleanup, and saved-mix visual polish

User request (condensed):

> Add a level milestone roadmap, remove the Play button in front of the pet
> house, and fix the Your mixes category tint icon in light mode. Use a
> different dark color per mix instead of category colors, and add a selected
> highlight color.
>
> Follow-up CI failure: fix unresolved `roundToPx` and `offset` references in
> `NewCategoryPicker.kt`.

## Completed

- Added the Quests level-milestone roadmap: unlocked milestones use the gold
  treatment, the next reward gets a ring, and locked rewards remain muted.
- Removed the separate Home pet-house Play bubble; the pet's own interaction
  remains the way to play.
- Saved-mix cards now resolve one stable identity tone from each mix's
  `createdAtMillis` instead of tinting every category icon independently.
  The tone is lightened for dark mode, reused by the cover/icon chips and
  Active label, and active mixes receive a matching border and soft fill
  highlight.
- Fixed the anchored hold-pill compiler errors in `NewCategoryPicker.kt`:
  imported `Modifier.offset`, removed the unavailable top-level `roundToPx`
  import, and converted the 14dp anchor gap with the offset lambda's density.
- Updated `app/AGENTS.md` and the current Fastlane changelog to describe the
  roadmap, pet-house behavior, mix highlight, and compile fix.

## Verification

- `git diff --check` passes.
- No Gradle compile/build/lint/test command was run because the project DOX
  explicitly forbids Android Gradle validation in this environment; CI is the
  verification path.
- `ANALYSIS.md` was pre-existing and remains untouched/untracked.

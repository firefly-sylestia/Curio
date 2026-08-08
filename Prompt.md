# Request — Reaction lines editor + custom pet save crash fix (v8.39)

## Completed (v8.39)

### Custom reaction lines

- Added `PetReaction.lines`, persisted safely in the existing PetDesign text
  format as URL-encoded `lines=` content. Multiple lines are separated by
  newline before encoding, so semicolons, equals signs, percent signs, plus
  signs, spaces, and Unicode cannot corrupt the parser.
- Added a multiline editor under each selected reaction event in Settings →
  Pet designer. The draft is bounded to 8 lines × 120 characters and keeps
  the raw draft separate from normalized persisted lines so ordinary typing
  does not move the cursor.
- Curie chooses a random saved line for configured events and touch reactions
  only when the new **Settings → Appearance → Custom reaction lines** toggle
  is ON. The setting defaults OFF, and missing lines always fall back to
  Curie's existing built-in dialogue.
- The TOUCH reaction now honors its existing enabled toggle consistently;
  disabled TOUCH suppresses its configured face, motion, and line.
- Legacy designs without `lines=` continue to parse unchanged.

### Crash fix

- Fixed `PetDesign.toParsedOr` parsing of `face=MOOD;...` and
  `react=EVENT;...` lines. The old code used the equals-sign index as the
  substring end after the name prefix, causing `substring(5, 4)` / the
  reported `StringIndexOutOfBoundsException` when loading a saved custom
  design after PNG import/shape editing.
- Parsing now uses the first semicolon after the mood/event name and safely
  handles malformed or empty entries.

## Files changed

- `app/src/main/java/com/curio/app/data/PetDesign.kt`
- `app/src/main/java/com/curio/app/data/AppPreferences.kt`
- `app/src/main/java/com/curio/app/features/petdesigner/PetDesignerScreen.kt`
- `app/src/main/java/com/curio/app/features/settings/SettingsSectionScreen.kt`
- `app/src/main/java/com/curio/app/features/settings/SettingsHubScreen.kt`
- `app/src/main/java/com/curio/app/ui/pet/CurioFloatingPet.kt`
- `app/AGENTS.md`, `Prompt.md`, version/changelog metadata

## Validation

- Robust delimiter balance: all changed Kotlin files BALANCED.
- `git diff --check`: clean.
- Final code review: no concrete Kotlin/Compose blocker found.
- Gradle compile/build/lint/test was not run because repository DOX rules
  forbid local Android builds; CI on push is the compile gate.

# Request — Fix CurioPetSprite color parsing

## Completed

Fixed the invalid Kotlin `Color(0xFF${...})` expressions in `CurioPetSprite.kt` without removing any features. All active-design palette colors and the inline `S` color now prepend the alpha channel and parse the six-digit hex string with `toLong(16).toULong()`.

## Validation

- Confirmed no invalid `0xFF${...}` color expressions remain.
- `git diff --check` passed.
- No Gradle build or compile command was run, per repository instructions.
- Committed as `6055919` and pushed to `Alpha`.

## Preserved

Sprite animations, moods, poses, accessories, and Pet Designer recoloring behavior remain unchanged.

## Follow-up

CI remains the compile gate for this Android project.

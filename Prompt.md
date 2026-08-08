# Request — Fix CurioPetSprite crash

## Completed

Fixed the reported `ArrayIndexOutOfBoundsException: length=20; index=61` crash in `CurioPetSprite.kt`. The previous `toLong(16).toULong()` conversion passed raw ARGB bits as Compose's packed color value, which made Compose interpret `61` as an invalid color-space index. All custom palette colors now use Android's ARGB parser before constructing the Compose `Color`.

## Validation

- Confirmed every active-design palette color uses `petDesignColor`.
- Confirmed the inline `S` palette color uses the same safe conversion.
- `git diff --check` passed.
- No Gradle build or compile command was run, per repository instructions.

## Preserved

Sprite animations, moods, poses, accessories, and Pet Designer recoloring behavior remain unchanged.

## Follow-up

CI remains the compile gate for this Android project.

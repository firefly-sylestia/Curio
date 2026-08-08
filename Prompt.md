# Request — Curie drawable details, improved Robot/Ghost presets, and full visual editing (v8.43)

## Completed

- Added four optional transparent detail layers to `PetDesign`: `tail`,
  `accessories`, `effects`, and `antenna`.
- Added per-element procedural visibility settings for tail, belly,
  accessories, effects, and antenna extras. Missing settings remain enabled,
  preserving every older saved design.
- Added URL-encoded `detail=` and `procedural=` lines to the text format;
  parsing, grid resizing, and old-format fallback remain tolerant.
- Added a focused Details tab to the Pet designer. It reuses the protected
  Draw mode, brush/fill/erase/eyedropper tools, quick palette, clear-layer
  action, and 24/32-pixel canvas behavior.
- Custom detail layers render as the final visual layer, so users can draw
  over generated art without changing Curie's existing motion or animation.
- PNG export now includes all four custom detail layers.
- Improved Robot and Ghost presets with detailed panel/window pixel art and
  dedicated, validated curled asleep poses.
- Preset body and curled grids are applied together so Sleepy preview matches
  the chosen Robot/Ghost shape.
- Clarified that the antenna toggle controls generated antenna extras (glint,
  nightcap, thinking mark); the base antenna remains directly editable in the
  Body canvas.

## Compatibility

- Existing saved designs without `detail=` or `procedural=` lines use the
  previous procedural behavior unchanged.
- Existing positional `PetDesign` construction remains source-compatible via
  default values for the new fields.

## Validation

- Changed Kotlin files pass delimiter-balance checks.
- Robot/Ghost body and curled presets each contain 16 rows of 16 characters.
- `git diff --check` passes.
- Static blocker review found no remaining compile blocker; CI remains the
  final Android compile gate.
- No Gradle build, compile, lint, or test command was run because repository
  DOX rules forbid local Android build commands in this environment.

# Request — show current detail parts with live placement and blueprint

## Completed

- The Details editor now opens with the selected part's effective current pixels visible instead of a blank transparent grid.
- Existing authored detail pixels are preserved over the projected current part; empty procedural layers are projected into the editable design grid using their current static placement.
- The first edit materializes the visible current part into the selected custom layer and disables only that layer's generated art, so the live preview matches the editable grid.
- Added a live full-Curie preview and clear placement copy for the selected Tail, Accessories, Effects, or Antenna layer.
- Added a “Show before-edit blueprint” toggle that displays the original current pixels beneath edits.
- External design replacements (resize, presets, randomize, imports, reset, and procedural toggles) clear temporary editor snapshots so stale current/blueprint art cannot persist.
- Authored pixels overlay the current generated projection, while disabled procedural layers show only their authored content.

## Validation

- PetDesignerScreen delimiter balance passed.
- `git diff --check` passed.
- Static review found no remaining concrete Kotlin/Compose blocker.
- Procedural projection is intentionally a static editor placement; runtime animation remains owned by CurioPetSprite.
- Gradle builds are forbidden locally by the Curio DOX rules; CI remains the compile gate.

# Request — Curie Pet Designer drawn faces, focused editing, and easier colors (v8.42)

## Completed

- Added backward-compatible transparent pixel face overlays to `PetFace`.
  Existing saved designs without `grid=` continue using procedural eyes,
  mouth, blush, and sparkles as before.
- Added mood and reaction face overlay editing in the Pet designer. Drawn
  overlays are serialized URL-encoded in existing `face=` / `react=` config
  lines and resized with the body canvas.
- Updated `CurioPetSprite` to render custom face overlays while preserving
  the body, motion, tail, accessories, sparkles, and sleep pose. Procedural
  face art is suppressed only when a custom overlay exists.
- Refactored Pet designer navigation into focused Preview, Body, Faces,
  Colors, and Tools tabs so editing options no longer form one long scroll.
- Added explicit Draw mode. When off, body and face canvases do not consume
  scroll gestures; when on, brush/fill/erase/eyedropper gestures are active.
- Docked a quick body palette beside the canvas and retained the full palette
  in Colors. Existing hex + HSL picker remains available via palette editing.
- Face eyedroppers return to Brush mode after selecting a color.
- Stabilized body and face pointer-input handlers with updated callbacks, so
  recomposition during a drag no longer cancels continuous painting.
- Empty custom grid values no longer suppress legacy procedural faces.

## Files changed

- `app/src/main/java/com/curio/app/data/PetDesign.kt`
- `app/src/main/java/com/curio/app/features/petdesigner/PetDesignerScreen.kt`
- `app/src/main/java/com/curio/app/ui/pet/CurioPetSprite.kt`
- `Prompt.md`

## Validation

- Kotlin delimiter balance passed for all three changed Kotlin files.
- `git diff --check` passed.
- Static review checked serialization fallback, custom rendering, draw-mode
  gesture protection, tab helper signatures, reaction eyedropper state, and
  pointer-input stability.
- No Gradle command was run because repository DOX rules forbid local Android
  builds; CI on push remains the compile gate.

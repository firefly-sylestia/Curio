# Request — PNG import: eyedropper "add custom color" step (v8.37)

## Completed (v8.37)

Shipped (refinement of the existing v8.35 PNG import — always-on, no toggle):

1. **Import review step.** Picking a PNG no longer snaps instantly. The image
   is resampled to the canvas and a review dialog opens showing the RAW
   pixels (large, pixelated) plus the design's four custom palette slots.
2. **Eyedropper picking.** Tapping a pixel on the preview fills a custom
   slot (c → C → d → D in order). Tapping a slot chip arms it as the
   eyedropper target (ring highlight), then one tap fills it; picks disarm
   so repeated taps walk the slots.
3. **Quick picks.** The image's 8 dominant quantized colors (4-bit/channel
   merge, sorted by pixel count) appear as swatches — tap one to fill the
   armed-or-next slot. Swatches already in a custom slot get a primary ring.
4. **Apply.** Fold the review's custom colors into the palette
   (`withPaletteColor` over c/C/d/D), then snap every pixel to the nearest
   of all 13 keys with that extended palette (the picked colors win for
   matching pixels instead of being flattened to the 13 defaults) and apply
   to the body/curled grid. Toast reports how many custom colors were added.

## Files changed

- `app/src/main/java/com/curio/app/features/petdesigner/PetDesignerScreen.kt`
  — `ImportReview`/`ImportedColor` data classes, `CUSTOM_SLOTS`,
  `buildImportReview`/`addCustomColor`/`pickColorFromCell`/`rgbToHex`/
  `snapPixelGrid`/`applyImport` helpers, `ImportPngDialog` composable
  (Canvas preview + slot chips + quick picks + Apply/Cancel), picker
  callback defers to review, Import PNG button subtitle updated.
- `app/build.gradle.kts` → 20260827; new store changelog.

## Validation

- Robust delimiter balance on PetDesignerScreen.kt (char-by-char,
  block-comment + string aware) — BALANCED; `git diff --check` clean.
  (The quick regex checker gave a false positive.)
- code-reviewer-glm caught a REAL bug: the Canvas `pointerInput` only
  restarts on key change, so its captured `onPickCell`/`review` went stale —
  repeated taps would recompute from the pristine first review and drop
  earlier picks. Fixed with `rememberUpdatedState(onPickCell)` inside the
  dialog; also take(8) directly in buildImportReview.
- No Gradle build in this environment (repo rule) — CI on push is the gate.

## Notes

- The 4 custom slots are the model's only extension mechanism (PetDesign
  text format + sprite + editor palette all assume 13 keys). If the user
  wants MORE than 4 importable colors, that needs new palette keys across
  PetDesign/PetSprite/editor — offered as a follow-up.

# Request — Ready-made face & reaction presets: Shy, Party, Sleepyhead (v8.38)

## Completed (v8.38)

One-tap personality presets for the Pet designer's Face & reactions editor
(always-on — same class of editor convenience as the v8.37 PNG eyedropper;
no toggle needed since it only writes settings the user can already set):

1. **Model** — new `PetFacePresets` object in `PetDesign.kt` with a `Preset`
   data class (name, tagline, faces for all 7 moods, reactions for all 7
   events) and `applyTo(design)` which paints every face and rule in one
   pass via the existing `withFace`/`withReaction`.
2. **Three presets**:
   - **Shy** — bashful: wide/HAPPY eyes, blush everywhere, quiet "o" mouths,
     gentle SQUISH/HOP reactions.
   - **Party** — sparkly: STAR eyes + WIDE mouths + sparkles on every face,
     BOUNCE/SPIN reactions.
   - **Sleepyhead** — dozy: CLOSED/BLINK eyes, minimal mouths, mostly NONE
     or gentle HOP/SQUISH reactions.
3. **UI** — "One-tap presets" row at the top of the Face & reactions card:
   three `PresetCard`s (tappable surfaces in a Row) each showing a LIVE
   mini `CurioPetSprite` preview of that personality, a name and a 2-line
   tagline. Tapping applies the preset and confirms with the existing toast.

## Files changed

- `app/src/main/java/com/curio/app/data/PetDesign.kt` — `PetFacePresets`
  object (Preset + SHY/PARTY/SLEEPYHEAD + ALL + applyTo).
- `app/src/main/java/com/curio/app/features/petdesigner/PetDesignerScreen.kt`
  — imports (`PetFacePresets`, `TextOverflow`, `RowScope`), presets row in
  the Face & reactions SectionCard, `RowScope.PresetCard` composable.
- `app/build.gradle.kts` → 20260828; new store changelog.

## Validation

- Robust delimiter balance on both files (char-by-char, comment + string
  aware) — BALANCED; `git diff --check` clean.
- code-reviewer-glm caught a REAL compile blocker: `Modifier.weight(1f)`
  inside `PresetCard` has no scope receiver — fixed by declaring it
  `private fun RowScope.PresetCard(...)` + `RowScope` import. Also caught
  during self-review: my anchor-based insertion stole `SmallAction`'s
  `@Composable` + KDoc — restored both.
- No Gradle build in this environment (repo rule) — CI on push is the gate.

# Current Request — Material theme fixes: pastel cards, mix → primary, adaptive-hero contrast, M3 nav roles

## Status: DONE (committed + pushed to Alpha)

## Request (user, paraphrased)
"the material main card colors are good but in pastel mode they are not.
and also in material theme dont let the mix color come, make it the
material color when they get mixed. also in light mode adaptive hero the
hero card looks washed out along with the glyphs and the texts and the
box. fix that keep the material color for it but fix it. and fix the nav
bar material color as they are bad"

## Clarified via ask_user (user's answers)
1. **Mix color**: scheme PRIMARY — every mixed deck wears the Material
   theme's one primary color (no blend).
2. **Adaptive hero**: DEEP material banner with DARK ink (custom answer:
   "deep material banner dark ink").
3. **Nav bar**: PURE M3 ROLES — neutral container + soft secondaryContainer
   indicator, no lane colors.

## What changed (v190)
- **Pastel cards** (`CurioColors.kt`): the Material branches of
  `cardGradient` / `heroBlendGradient` / `mixedDeckAccent` ignored Pastel
  mode — the material main card kept full-strength family fills while the
  rest of the app softened. All material card fills now resolve through
  `pastelAccent(fill, dark)` when Pastel is on.
- **Mixes → primary** (`CurioColors.kt` + `SpinScreen.kt`):
  `mixedDeckAccent` / `mixedDeckGradient` gained `materialPrimary: Color?`
  (default null); SpinScreen passes `colorScheme.primary` when Material is
  on and >1 lane is selected. A mixed deck now renders the standard quiet
  `cardGradient` from the single primary — the multi-hue family sweep is
  gone. Same-family mixes keep the family fill; pastel softens the primary.
- **Adaptive hero** (`MaterialFamilies.kt` + `CategoryInk.kt`): light-mode
  `materialHeaderAccent()` no longer uses the pale T90 scheme containers —
  it wears the RICH family color (family fill lifted to L=0.70, sat capped
  0.55); new `materialHeroInk()` (deep same-hue twin via `readableLightInk`)
  pairs with it through `heroHeaderInk()`'s Material-light branch. Dark
  keeps the deep T30 containers + light ink (unchanged).
- **Nav M3 roles** (`CurioBottomNav.kt`): under Material,
  `curioNavContainerColor` / `curioFloatingNavContainerFor` →
  `surfaceContainer`; `curioActivePillFill` / `curioActivePillInk` →
  `secondaryContainer` / `onSecondaryContainer`. Covers the floating pill
  bar, the wide-window rail and the reveal Like/Dislike capsule.

## Docs
- `app/AGENTS.md` — v190 entry + v185 system bullet updated.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 4 FIX bullets.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). Greps confirm no
  leftover references; all call sites of the changed functions updated
  (`mixedDeckAccent`/`mixedDeckGradient` only called from SpinScreen +
  the non-material fallback inside `mixedDeckGradient`).

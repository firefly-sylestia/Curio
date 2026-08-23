# Prompt.md — current request log

## Request (complete): v239 — full revert of b8d43c7 + refraction/indicator corrections

User: revert commit b8d43c7 fully (the v236 press-blob rollout); keep the light
mode text fix; no color on the active indicator; restore the original (Apple-
style) select blob behavior of the nav bar; floating pills had become less
refractive; indicator was non-refracting; Pet Designer middle tab showed
duplicated text/icon at the sides while dragging.

### What shipped
1. **`git revert --no-commit b8d43c7`** — press-blob helper and every call site
   gone (Home menu/profile, detail back/more, Reveal favorite, tour dock glass +
   buttons); Pet Designer studio bar back to the v234 simple glass capsule
   Surface. Conflicts resolved by hand in LiquidGlassPills.kt (kept the v238 tilt
   arc, dropped the helper) and PetDesignerScreen.kt (kept the classic branch).
2. **Refraction restored**: undid the v237 size-cap — fixed lens heights
   (`clear ? 14/18dp : 24/24dp`) back in `liquidGlassCapsule` and the tab bar.
3. **Indicator**: accent fill REMOVED (neutral black shading only); always mildly
   refracting at rest (`lens 6→12dp` with press), blur only while pressed/dragging
   so mid-drag neighbor copies can't duplicate on the Pet Designer middle tab.
4. **Light-mode active ink**: plain dark `colorScheme.onSurface`; dark mode keeps
   `curioActivePillInk`. Applied in CurioBottomNav; Pet Designer no longer has its
   own ink (branch reverted).

Files: LiquidGlassPills.kt, CurioLiquidGlassTabBar.kt, CurioBottomNav.kt,
CurioTopBar.kt, CurioNavHost.kt, HomeScreen.kt, EntryDetailScreen.kt,
TopicRevealScreen.kt, PetDesignerScreen.kt (+ docs). Balance checks OK ×9;
zero `curioGlassPressBlob` references remain. CI validates compilation.

Lesson: the vFlow select blob belongs ONLY to the bottom-nav draggable indicator;
bolting glow-blob presses onto ordinary pills read as blur. Refraction heights are
tuned values — don't "fix" them without a reproduced visual bug.

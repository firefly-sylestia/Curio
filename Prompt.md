# Prompt.md — current request log

## Request (complete): v240 — crisp touch spec re-roll + transparent idle indicator + Profile glass

User: add the touch blob back to other screens but FIXED (it was blurry when
touching/moving; commit 106da72's blob wasn't); idle indicator should be fully
transparent ("you just reversed it"); active text still not readable → darker;
extend floating-pill glass to the Profile page.

### Root causes / changes
1. **Blurry-while-moving**: my v239 `blur(7dp × pressProgress)` on the indicator
   was the culprit — 106da72 had zero blur. REMOVED. (Pet Designer middle-tab
   duplicates stay solved by the hidden row recording plain content only.)
2. **Crisp helper re-added**: `curioGlassPressBlob` = spring scale 1.05× + a TIGHT
   additive specular spot (radius ≈45% of pill, white .60→.20→0, BlendMode.Plus)
   tracking the finger — a refraction glint, not v236's full-pill fog.
   Wired on: Home menu/profile, detail back/more (CurioBackButton regained optional
   interactionSource), Reveal favorite, tour dock glass + both buttons.
3. **Idle indicator**: `onDrawSurface = null` — fully transparent glass, no accent,
   no neutral shade.
4. **Light-mode active ink**: pure BLACK (`Color.Black`), third darkening step.
5. **Profile page** joins in-screen glass: local capture wrapper around watermark+
   list; sticky back/search pills morph solid→real glass sampling the local capture
   (fill fades to transparent at endpoint); crisp spec on both pills.

Files: LiquidGlassPills.kt, CurioLiquidGlassTabBar.kt, CurioBottomNav.kt,
CurioTopBar.kt, CurioNavHost.kt, ProfileScreen.kt, TopicRevealScreen.kt,
HomeScreen.kt, EntryDetailScreen.kt (+ docs). Balance OK ×8; CI validates.

Standing lessons: never blur the nav indicator sample; press feedback must be
crisp+small (additive spot), never soft large gradients; idle indicator carries no
fills of any kind.

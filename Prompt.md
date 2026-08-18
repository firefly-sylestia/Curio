# Current Request — v201: ring cut fix + nav pill collapse + pill-size parity

## Status: DONE (committed, push follows)

## Request (user, verbatim)
- "the 3d ring should be shouwn fully without getting cut thats what i meant"
- "make the home nav pill collapse even smoother like make it collape even more and make the like dislike button match the text and size of the nav bar pill and same in pet designer" (plus "okay thats fine for now, you can push it and remove the useless dump files" — the v200 content push happened first)

## What changed (v201)

### 1. Hole-ring coil cut at card edge — ROOT CAUSE FOUND
Material3 1.5's `Surface` ALWAYS clips children to the shape (`.clip(shape)` at the end of its implementation) — the v74 "Surface does not clip" note was true only for M3 1.0/1.1. The coil's left peek (drawn at −6.5dp) was being cut at the card edge. Fix: the three stat-pane call sites (Home, Profile, EntryDetail) swap the clipping `Surface` for a plain `Box` carrying `Modifier.shadow(elevation, shape, clip = false)` + the paper fill — the fill self-clips to the outline path, so the coil escapes past the left edge. All three sites have ≥28dp container padding, so the peek clears the screen edge.

### 2. Nav pill collapse — smoother + deeper
- Pill spring family 240 → 150 stiffness (longest calm critically-damped glide).
- The leave-hold collapse now targets `FloatingPillCollapsedWidth` (44dp — tighter than the idle 64dp icon pill), so the pill visibly cinches in before the bar unmounts. `FloatingNavPill` gained a `collapsing` param; the NavHost leave-hold extended 380 → 420ms to match the slower settle (still exactly the spring's settle time — no dead pause).

### 3. Like/Dislike + Pet Studio bars match the nav pill exactly
- `RevealSentimentPill` (Topic Reveal): 64/136dp + 52dp height + 26dp icon, springs 400 → 150, label → Changa One 15sp Normal (was labelMedium Bold).
- `PetStudioTab` (Pet Designer): same 64/136dp + 26dp icon, springs 400 → 150, label → Changa One 15sp Normal.

## Files
- `ui/components/CurioBottomNav.kt` — collapsing param + collapsed width + 150 springs.
- `navigation/CurioNavHost.kt` — hold 380 → 420ms.
- `features/reveal/TopicRevealScreen.kt` — sentiment pill parity.
- `features/petdesigner/PetDesignerScreen.kt` — studio tab parity.
- `features/home/HomeScreen.kt`, `features/profile/ProfileScreen.kt`, `features/detail/EntryDetailScreen.kt` — Surface → Box + shadow(clip=false) at the three stat-pane sites.

## Docs
- `app/AGENTS.md` — v201 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — FIX bullets.
- This Prompt.md.

## Verification
- All touched files brace-balanced (tokenizer check) — Home/Profile/Detail 300/220/518, nav/reveal/pet clean.
- CI validates the compile on push.

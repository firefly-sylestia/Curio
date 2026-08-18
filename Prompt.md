# Current Request — Animated Movies category (v200) + housekeeping

## Status: DONE (committed + pushed to Alpha)

## Request (user, verbatim)
"continue the expansion of topics and add animated movies section as a ne category and separate animated movies from films and make them 1000+ and anduse real quick facts and push after its fully done" (+ "anime and animation movies are differnt btw")

## What changed (v200)

### 1. New category: Animated Movies (ANIMATED_MOVIES)
- **Anime ≠ animated movies** (user note): the 6 anime films in films.json stay put (Akira, Grave of the Fireflies, Totoro, Princess Mononoke, Spirited Away, The Boy and the Heron). The new lane is non-anime animation only.
- **Registration:** `Category.kt` (enum + newLanes + order + slug `animated-movies` + Entertainment family), `CurioColors.kt` (palette constants), the three exhaustive `when`s (CaptureEntity.kt, ExploreSession.kt, TopicRevealScreen.kt), and the Entertainment quick-mix preset (DeckPresets.kt). Gradle `validateTopics` derives expected categoryId from the filename — no validator list change needed.
- **Separation:** `scripts/extract_animated_from_films.py` moved 52 non-anime animated films out of films.json (948 remaining) into the new animated-movies.json. First tag-based attempt false-positived (live-action "Pixar"-tagged films) and was reverted — switched to an explicit title list.
- **Content:** ~540 more real entries authored across scripts/batch_animated_1..11.py — Disney theatrical + DTV, Pixar, DreamWorks, Illumination, Blue Sky, Sony, Aardman, Laika, stop-motion indie, Don Bluth + 80s/90s classics, classic 30s–60s + Rankin/Bass, international (French, Irish, Chinese, Latin American, Indian, Australian, Russian), and franchise DTV (Barbie, Scooby-Doo, Tom & Jerry, DC/Marvel animated, DisneyToon sequels). **591 entries total this push** — the 1000+ top-up continues in a later pass (user approved pushing at 591).
- **Validation:** check_assets.py clean, all 18,071 ids unique across the catalog (no cross-file collisions).

### 2. Housekeeping
- Removed root-level reference dump SVGs: `svgviewer-output (12).svg`, `curio_planet_cropped_bottom_264.svg`, `footer.svg` (real drawer art lives in res/raw/).

## Files
- `data/Category.kt`, `ui/theme/CurioColors.kt`, `data/CaptureEntity.kt`, `data/ExploreSession.kt`, `features/reveal/TopicRevealScreen.kt`, `features/picker/DeckPresets.kt` — category registration.
- `app/src/main/assets/topics/animated-movies.json` — 591 entries.
- `app/src/main/assets/topics/films.json` — 948 (52 animated removed, anime intact).
- `scripts/extract_animated_from_films.py`, `scripts/batch_animated_1..11.py` — authoring scripts.

## Docs
- `app/AGENTS.md` — v200 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — ADD bullet.
- This Prompt.md.

## Next steps (user's follow-up asks, queued)
1. 3D ring on PaperStatCard: still cut at the card's left edge — make it actually peek out (needs the peek to escape the clip, not just draw at negative x).
2. Home nav pill collapse: even smoother / collapses more.
3. Like/Dislike button: match the text and size of the nav bar pill — same in Pet Designer.

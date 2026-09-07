# Prompt Log — current request

## Request (2026-09-06, active → v383 committing)

Share-card refinement cycle — continuing from v379d–v382 (all pushed):
smart-fit honesty, auto-layout sparkle pill, title-ownership (overlap
freedom), Signature glued covers. This round added two brand-new asks on
top plus a CI fix for v382.

- ✅ v379d–v382 pushed (`3fcfa4fa` … `aad184ba`): pill + smart-fit text-first
  + reset restores fit + factZoom removed + whole-box sliders removed +
  title-height hides for short titles + dark-text frostInk + Paper quote
  ink + fact-line parity, hand-placed title owns its spot (no bounce),
  pill 9:16 bigger text, Collage dark-tone + blended tear, Signature
  covers glued.
- ✅ v383 (THIS COMMIT): **Link share + fact-width > 100% + v382 CI fix.**
  1. **Link share** (user: \"deep link style share — open it and it opens the
     topic; I choose the text\"; answered: albums+artists+songs → music
     service, others → Google; URL + editable caption). `ExploreSearch.kt`
     gained `shareLinkForTopic(topic)` (re-reads the Settings MusicService
     at share time). `TopicShareSheet` gained `shareLinkUrl: (() -> String)?`
     + a Link pill in the actions row (link icon, opens a caption dialog
     seeded with the topic name, shows the tap-to-open URL in a preview
     chip, Share posts ACTION_SEND of caption+URL and dismisses). Wired all
     three callers: TopicRevealScreen (`floatingTopic`), EntryDetailScreen
     (`resolvedEntry.topic`), ShareHubScreen (`topic`).
  2. **Fact width past 100%** (user: \"box looks small, side space unused,
     width caps at default\"; asked where → \"paper mainly then others\").
     Fact-width sliders (full-screen + sheet Crop) now run 0.3x–1.2x
     (steps 89). `moveFact` uses a custom layout: ≤1x = identical to old
     fillMaxWidth (natural wrap, place 0 — zero pixel change for existing
     cards); >1x = measure pane at columnWidth×frac and recentre the
     overhang so the box eats the design's side gutters. Render-path
     `effectiveMove`/`boxScaledMove` width clamps raised 1f → 1.2f; the
     untouched auto-fit seed stays ≤1x. Note: a phantom \"+1 brace\" scare
     was a scanner artifact from a nested-quote `${topicName...\" (\"...}`
     template (line ~10024) — replaced with a precomputed `displayTopic`
     val (cleaner Kotlin, file verified balanced with a real stack scan).
  3. **CI fix for v382**: Signature SIDE layout used
     `Modifier.align(Alignment.CenterHorizontally)` inside a nested
     Box — compile error \"cannot be called in this context with an implicit
     receiver\". Fixed with `Box(contentAlignment = Alignment.Center)`.
- NEXT UP (user-declared): the dedicated Signature background treatment
  round.

# Prompt.md — Request log

## Current request — COMPLETED: reveal screen band → floating sentiment pill + tags under hero + CI compile fix

The user asked: remove the reveal's bottom scaffold/band, add a floating Like/Dislike
pill that hides on scroll and reappears, move the tags back below the header, match the
hero pill color with the main shuffle card in dark mode and the author pill in light
mode — plus a CI compile failure (`commitUtterance` unresolved reference) to fix and push.

### 1 — CI compile fix (SoundBiteFormat.kt)
CI failed: `commitUtterance` is a LOCAL function referenced from the dictation
listener inside `startDictation`, which was declared BEFORE it — Kotlin local functions
are scoped from their declaration point onward (same class of error as the v126 note).
`commitUtterance` moved above `startDictation`; resolved.

### 2 — Reveal bottom band removed (TopicRevealScreen + CurioNavHost)
- `RevealBottomBarHeight = 80.dp` (TopicRevealScreen) and the NavHost's matching
  `RevealBottomBarPlaceholderHeight` reservation + `.padding(bottom = ...)` are gone.
  The reveal now runs full-bleed like the tab pages (no Scaffold slot, no painted band,
  no 80dp dead strip at the bottom).

### 3 — Floating Like/Dislike pill (hide on scroll-down, reappear on scroll-up)
- `RevealSentimentPill` mirrors the bottom nav's floating pill bar: raised
  `surfaceContainerHigh` capsule (6dp shadow, `navigationBarsPadding` + 12dp air gap)
  with two `SentimentSegment`s; the active segment fills with the category accent +
  on-accent ink (v27q solid-selection contract).
- Scroll direction is tracked with `snapshotFlow` over the page's hoisted
  `ScrollState` (3px dead-band): scrolling down hides the pill, scrolling up brings it
  back. `AnimatedVisibility` slides it out/in.
- Kept the old band's gates: hidden in Browse-Topics mode (`browseMode`, read-only),
  only rendered once the topic resolves. Bottom spacer in the scroll body: 100dp so the
  last card clears the pill. Old `SentimentButton` (band version) removed.

### 4 — Tags back below the hero
- Tags moved out of the (removed) band into the scroll body directly below the hero:
  hero → tags → actions → teaser → prompt. New `TagsRow` composable reuses the band's
  chip recipe (accent-tinted surface @32%, 2dp lift, weight-capped thirds).
- The action row takes a 16dp gap when tags are present, else the old progress-pill
  clearance (`progressFloatGap`).

### 5 — Hero pills match the Spin ticket
- The hero's opaque `pillGlass` (frosted/jewel glass, theme-varied) is replaced by the
  Spin main card's exact pill recipe `ink.copy(alpha = 0.18f)` for the action badge,
  byline ("Author · …") and subtype pills — in BOTH light and dark, so the shared-element
  morph reads as the same card and the reveal hero stops looking like white blobs next to
  the ticket's subtle tint. Dead `pillGlass` block + unused imports removed.

### Files touched
- `app/src/main/java/com/curio/app/features/capture/formats/SoundBiteFormat.kt` — CI fix (commitUtterance order)
- `app/src/main/java/com/curio/app/features/reveal/TopicRevealScreen.kt` — band removal, floating pill + scroll-hide, TagsRow, hero pill color
- `app/src/main/java/com/curio/app/navigation/CurioNavHost.kt` — reveal placeholder reservation removed
- `app/AGENTS.md` — v132 bullet
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — FIX bullets

Not done: no Gradle build here (env forbids it) — CI validates on push.

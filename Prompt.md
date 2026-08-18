# Current Request — Shuffle main card: remove the year pill (reveal keeps it)

## Status: DONE (committed + pushed to Alpha)

## Request (user, verbatim)
"in shuffle main card dont show the year pill just inside the topic
reveal."

## What changed (v192)
- `SpinScreen.kt` — the Spin ticket's top-left pill row (v141: byline +
  year qualifier) now renders ONLY the byline pill. The year pill (the
  Schedule-glyph chip fed by `yearQual` from `titleAndYearQualifier`) is
  removed from the shuffle card.
- The Topic Reveal hero is untouched — it keeps its year pill (top bar
  next to the category chip, TopicRevealScreen ~line 663).
- The card title still drops the trailing year ("Moby-Dick (1851)" →
  "Moby-Dick"), so the shared-element morph stays clean (no year popping
  in/out of the title during the card grow).
- `CurioIcons.Schedule` no longer used in SpinScreen (it's an object
  property — no import cleanup needed).

## Docs
- `app/AGENTS.md` — v192 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 1 FIX bullet.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). Grep confirms the
  year pill code is gone from SpinScreen and still present in
  TopicRevealScreen.

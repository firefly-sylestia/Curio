# Request — v8.25: reveal title inside the hero card + bottom-strip cream flash fix

## What the user asked

1. On the topic reveal page, put the title inside the hero card so it looks
   like the title stayed in its place (it should morph with the card instead
   of popping in below as a separate headline).
2. When the topic reveal page opens, the bottom nav area changes back to the
   plain cream color for a moment — fix that flash.

## Analysis

- The reveal hero card is the shared-element morph target for the Spin front
  ticket. The topic name was rendered BELOW the hero as a standalone 40sp
  centered headline (v7.16 removed the title from the shared element because
  a separate shared title stretched text). Moving the title INSIDE the hero
  card makes it card content, so the whole card (gradient, watermark, pills,
  name) morphs as one unit — the title reads as staying put.
- The bottom-strip flash: on the reveal route, the NavHost swaps the bottom
  bar for `revealBottomBarContent` (the action dock) or an invisible
  `RevealBottomBarPlaceholder` until the dock registers a frame or two later.
  The placeholder was a bare transparent Spacer → the Scaffold's plain cream
  surface showed through in the strip for that gap.

## Changes (3 files)

| File | Change |
| --- | --- |
| `features/reveal/TopicRevealScreen.kt` | The topic name moved INSIDE the hero card: a `Column` (20dp padding, SpaceBetween) now holds the verb+duration badge (top), the name (middle — 34sp/38sp geom ExtraBold, same style as the Spin ticket title, card ink, max 3 lines), and the byline + subtype pills (bottom row, one per corner, blank spacers keep a lone pill pinned to its corner). The standalone headline below the hero is gone; the tags row follows the hero directly (top padding 20dp). `TextAlign` import removed. |
| `navigation/CurioNavHost.kt` | `RevealBottomBarPlaceholder` now paints the reveal page's category wash (`background` param) instead of showing the Scaffold's cream surface. The wash is resolved from the reveal route's `categorySlug` (`revealCat`, keyed on the back-stack entry) so the strip matches the page from the very first frame — including deep links and the fallback 80dp branch. |
| `fastlane/metadata/android/en-US/changelogs/20260811.txt` | NEW — release notes for the two fixes. |

## Validation

- Brace balance ALL OK (all edited files), `git diff --check` clean.
- No compile/build commands run (environment has no Android SDK — CI gates
  compilation on push per root AGENTS.md).
- Reviewer (code-reviewer-deepseek-flash) passed after two fixes: the
  hero's content column now uses explicit weighted spacers (title stays
  centred even while the topic loads and the badge is absent), the byline
  pill is bounded with `weight(1f, fill = false)` so a long byline
  ellipsizes instead of overflowing the bottom row on narrow screens, and
  SpinScreen's stale "title is not a shared element" comment was updated.

## Completion summary

v8.25 shipped: the reveal topic title now lives inside the morphing hero
card (same 34sp geom style as the Spin ticket, so the shared-element morph
grows it in place), and the reserved bottom strip wears the reveal page's
category wash from the first frame — no more cream flash when the topic page
opens. Pushed to Alpha.

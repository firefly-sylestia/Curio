# Request — remove the detail page's reserved bottom strip; pop-up entrance animation

## Analysis

- The detail page (`EntryDetailScreen`) published an 80dp wash-colored spacer
  into the NavHost Scaffold's reserved bottom slot (v8.36). It exists so the
  Cabinet→Detail shared-element morph runs on a stable layout: if the bottom
  bar's space vanished the instant the route switched, Scaffold `innerPadding`
  would change mid-morph, re-laying out the `SharedTransitionLayout` and
  jolting the expansion.
- The user saw that fixed, non-scrolling band as a "placeholder scaffold" at
  the bottom of the page and asked to remove it, and to change the page's
  entrance from slide/fade to a center pop-up. When asked about a follow-up
  option, the user chose: remove the strip entirely and keep the page's plain
  tinted background (no inset-height spacer either).

## Changes

- `EntryDetailScreen.kt` — removed the bottom-slot registration entirely
  (`onBottomBarContentChanged` / `onBottomBarContentCleared` params, the
  `detailBottomBar` Surface, and its publishing `DisposableEffect`), and
  removed the hero's Cabinet shared-element morph (`heroMorphMod` /
  `sharedElement("cabinet-{entryId}")`) — the pop-up replaces the card
  expansion. The page keeps its category-wash background (`background(wash)`)
  as the only bottom treatment. Orphaned imports (`LocalRevealSharedScope`,
  `LocalRevealVisibilityScope`, `CabinetBoundsTransform`, `WindowInsets`,
  `navigationBars`, `windowInsetsPadding`) removed.
- `CurioNavHost.kt` — Entry Detail no longer reserves the Scaffold bottom slot
  (only the Reveal route keeps the reserve + its wash placeholder). The
  `ENTRY_DETAIL` route now: enters with `scaleIn(0.88) + fadeIn` (center pop),
  has its underlying screen exit with a matched 450ms fade (no slide), pops
  back out with `scaleOut(0.88) + fadeOut` at the same 450ms, and the page
  below fades back in. Removed the dead `CurioNavTint.cabinetWash` branch +
  import.
- `CabinetScreen.kt` — removed the card's shared-element declaration (now
  inert; the detail hero no longer declares the matching key) + its three
  imports (`LocalRevealSharedScope`, `LocalRevealVisibilityScope`,
  `CabinetBoundsTransform`).
- `app/build.gradle.kts` — versionCode 20260905 → 20260906.
- `fastlane/metadata/android/en-US/changelogs/20260906.txt` — store note.

## Notes

- No strip is published at all (per user): the system-nav inset region at the
  physical bottom shows the theme surface, which matches how every other
  no-bottom-bar wash screen (e.g. Save capture) already behaves.
- Removing the reserve means the exiting screen re-lays out (grid snaps ~80dp)
  at navigation start in both directions; the matched 450ms fades mask it.
- The NavHost still provides the shared-element CompositionLocals for the
  detail composable — harmless (nobody reads them there now).

## Validation

- `git diff --check` passed; delimiter balance checked on all edited Kotlin
  files; removed imports verified unused via code search.
- Gradle builds are forbidden locally by the Curio DOX rules; CI remains the
  compile gate.

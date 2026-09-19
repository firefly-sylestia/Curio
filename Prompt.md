# Prompt.md — current request

Branch: **`ci/workflow-redesign`** (nothing pushed; every change here is committed
only, per the member's instruction).

## 1. The CI run's surfaces (the headline of this branch)

The member's ask: *"github workflow desisgn in a new branch with more suggestions
for screens"*, after *"mine workflow run looks very simple, i dont have the check
run too etc etc yk what ould be a better workflow run."*

The run had ONE surface (a job whose only output was a wall of Gradle log) and
now has four, each read by a different person at a different moment:

| Surface | Where | Written by |
| --- | --- | --- |
| Build summary | run page, bottom | `.github/scripts/build-summary.sh` |
| Checks tab | annotations on the changed files | `.github/scripts/annotate-gradle.sh` |
| Report check | a second job beside `verify` | the workflow itself |
| PR comment | the pull request | `gh pr comment`, same-repo PRs only |

Decisions worth keeping:

- **The scripts only READ.** `validateTopics` stays the authority for topic data;
  the CI scripts derive the summary from the files the build left (the APK, the
  lint report, the bundled catalogs), so re-ordering steps cannot make the
  summary lie, and a missing log exits 0 instead of failing a step.
- **The build step is `continue-on-error: true`.** A gate step re-fails the run
  afterwards. That is the only way the annotation and summary steps run on a
  FAILED build, which is when the compiler's own errors matter most.
- **The PR comment is upserted**, matched by the `<!-- curio-ci-report -->`
  marker, so there is one live comment per PR and not one per push — and it is
  gated to same-repo PRs, because a fork PR's token is read-only.
- **`report` needs only `verify`'s step outputs**, so anything the report must
  say has to be published in `verify`'s `outputs:` block.
- Further surfaces that are NOT built are listed in `.github/AGENTS.md` (timing
  table, APK size trend, catalog diff, failure digest, coverage row, …).

## 2. The deprecated Kotlin warnings

Fixes done on this branch:

- **`quadraticBezierTo` → `quadraticTo`** — a straight rename, 75 call sites:
  `SocialAvatar.kt` (62), `TopicShareCard.kt` (9), `CurioPetCompanion.kt` (4).

Deliberately NOT done, with the reason each one needs a real compile to be worth
attempting:

- **`rememberModalBottomSheetState` (~30 sites).** Material3 here is
  `1.5.0-alpha20`, which introduced `rememberBottomSheetState` as the unified
  API. The replacement takes the sheet's initial value (`SheetValue.Hidden`) and
  the release notes say the PartiallyExpanded anchor is no longer removed
  automatically — i.e. it is a BEHAVIOUR change behind a rename, and the exact
  parameter list could not be confirmed from the published docs. Needs a compile
  (or the artifact's own sources) before it is trusted.
- **`LocalClipboardManager` → `LocalClipboard` (4 sites).** Not a rename: the new
  API is suspend-based, so every call site's control flow changes.
- **`LocalLifecycleOwner` (`IsbnScannerScreen`).** The new home is
  `androidx.lifecycle.compose.LocalLifecycleOwner`, from
  `lifecycle-runtime-compose` — which is in the version catalog but is **not a
  dependency of `:app`**, so this needs a dependency line first.
- **The `Unnecessary safe call` / `Condition is always true` / `Elvis always
  returns the left operand` / `Redundant call of conversion method` families
  (~150 warnings).** Every one is a site-specific judgement (`?.` on a
  smart-cast value, an `else` on an exhaustive `when`), and there is no compiler
  in this workspace to catch a wrong call. They want a pass of their own, in
  small batches, per file.

## 3. The journal voice note

- **No shadow.** v403 lifted the strip with `.shadow(1.5.dp, …)`; the member
  called it back — the block casts nothing and has no background, so it reads as
  part of the writing (the recording capsule's own elevation is untouched: that
  is the recording UI, not the page).
- **The graph is a hand-drawn pulse.** The rounded-bar chart is gone; the voice's
  envelope is now ONE inked line — an upper and a lower contour stroked in the
  page's ink, the played part in the note's own accent, cut at the playhead by
  `clipRect`. The wobble that makes it read as drawn is a HASH of the sample's
  index rather than a random number, so the ink is identical on every frame and
  the line never crawls while it plays.
- **The play button is a dark, solid, filled disc** with the glyph knocked out of
  it in the page's paper — the same treatment the record button wears, so the two
  controls are one shape in one ink.

## 4. The "fetched poster is lost after a restart" investigation (findings only — no code changed)

The member's report: *"its persistent the book covers etc loses its fetched
poster and it reloads after restart"*. What the code actually shows:

- **It is NOT "not persisted".** `AppPreferences.setSheetArtUrl` writes the whole
  map into SharedPreferences as JSON and updates `sheetArtUrlsState`
  (`data/AppPreferences.kt`), and the app-level init loads it back into that state
  (`sheetArtUrlsState = getSheetArtUrls(context)`). The read/write pair is sound.
- **What IS fragile is how the consumers seed themselves.** `ArtworkSheet.kt`
  (and ~12 call sites in `TopicRevealScreen.kt`) do:
  `var artUrl by remember(topic.imageUrl) { mutableStateOf(sheetArtUrlsState[artKey] ?: topic.imageUrl) }`
  and then a `LaunchedEffect(topic.imageUrl, fetchConsent)` re-reads `stored`
  INSIDE the effect and re-fetches when it is null. Neither the `remember` key nor
  the effect key includes the cached value, so **a cache that loads after the
  sheet composes is never noticed** — the value stays missed for that whole
  visit, and the effect (already run) does not re-run. A restored sheet on a cold
  start is exactly that case.
- **And the remembered value is a REMOTE URL, with the bytes on disk unused.**
  `features/cabinet/CoverCache.kt` already downloads covers to a local file
  (`ensureLocalCover` / `localCoverFile`) and can report a persisted URL
  (`persistedUrl`), but the sheets never prefer the local file — so a remote URL
  that has gone stale (signed/expiring CDN links) shows as "lost" and re-fetches.

The fix is therefore two small things, at every art call site: **key the seed and
the effect on the cached value** (so a late load is picked up), and **read the
locally stored cover file before the remote URL**. Not attempted yet: ~13 call
sites, and there is no compiler in this workspace.

## 5. Still outstanding

- The warning families above (bottom-sheet migration, clipboard, lifecycle
  dependency, and the ~150 site-specific ones).
- Nothing on this branch has been pushed, so none of it is CI-verified yet.

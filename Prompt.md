# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "check these [the CI failure + the post-job caching log], also tell me how can we make our
> builds more faster. also turn on cover fetching and also the take studio on by default.
> also the tool that shows under the notes in save your take its hiding behind the keyboard
> when the note is too below, can u fix that and make it open as overlay which shows above
> keyboard not belo the note. and then do the next tag release with no beta, proper release,
> edit the release notes thats connected to release tag which gets added, update the notes
> and make sure not to use erm dashes."

Member's answers to the two questions asked before the release:

- **Tag**: **v1.3.0** (typed as a custom answer; the offered v1.2.0 was not taken).
- **Push**: "You push the commits and the tag."

---

## 2. The CI failure that was pasted

```
e: .../features/personal/BookReaderScreen.kt:390:17 Unresolved reference 'pendingBlock'.
```

Cause: v406 hoisted a `pendingBlock` state so `jumpToMark` could hand a block to whichever
reading surface is showing, but the declaration was left **below** the local functions that
read it, and a Kotlin local cannot see a local declared later in the same body.

Fix (already in the working tree, and carried by this commit): the state is declared up with
`pendingPage`, under a comment that says why. `node scripts/check_braces.js` is clean.

The rest of the pasted log is the **post-job cleanup**, not a failure: the Gradle User Home
cache was restored (8 entries, 825 MB), 1 entry (20 MB) saved, daemons stopped, and the job
summary generated with a green outcome. No action needed there.

---

## 3. Why the build was losing time, and what was done

What the log shows: Gradle 9.4.1, configuration cache on ("Configuration cache entry
stored"), `gradle/actions/setup-gradle` carrying `/home/runner/.gradle/caches` between runs,
and `56 actionable tasks executed` on a commit that touched three Kotlin files.

Two cheap causes found by reading the repo, and both are fixed:

1. **The build cache was never turned on.** `org.gradle.caching` was absent from
   `gradle.properties`, so every task re-ran even when its inputs were identical, and the
   restored Gradle User Home could only help with dependency resolution. It is on now, and
   `org.gradle.parallel=true` with it (the modules that exist are independent).

2. **`validateTopics` could never be skipped.** It declared `inputs.dir(topicsDir)` and
   **no output**, and Gradle can only mark a task UP-TO-DATE (or restore it from the cache)
   when it has both. So the whole catalog, 20,877 topics across every JSON file, was parsed
   and re-validated on every single build. It now writes a stamp under
   `build/curio/validate-topics.stamp`, declared with `outputs.file(...)` and
   `outputs.cacheIf { true }`, written LAST so a failing validation fails before the stamp
   exists. Unchanged JSON means the parse is skipped; on CI it can be restored outright.

Still on the table, **not done** (each changes what CI checks, so they are the member's call):

- **`lintDebug` compiles the whole debug variant** (`kspDebugKotlin`, `compileDebugKotlin`,
  debug resources) on top of the release variant the job actually packages. Roughly half the
  compile work in that job exists to run a lint pass. Either `lintRelease` (one variant
  instead of two) or a **separate parallel lint job** (wall clock becomes the slower of the
  two, not the sum).
- **The `cp data/topics/*.json` asset copy** rewrites the whole asset tree on each run
  before the build. A Gradle `Sync`/copy task with declared inputs and outputs would let
  Gradle skip the merge and packaging work when the catalog has not changed.
- **A fast compile-only job** for feedback (`compileReleaseKotlin`), with packaging behind
  it, so a red compile is reported in a fraction of the time.
- **AJDK 21** (LTS) instead of 17: the Kotlin and R8 toolchains are faster on it, and AGP
  supports it. The project pins 17 in both workflows.

---

## 4. What was changed

### Turned on by default (both keep their Settings switch)

- `AppPreferences.isCoverFetchEnabled` — the merged cover consent defaulted OFF, so a fresh
  install opened on placeholder art. A stored choice still wins (someone who turned the old
  per-kind switches off keeps them off); no stored choice at all, which means nobody has
  ever answered, now means yes.
- `AppPreferences.isCaptureStudioEnabled` — default `true`.

### The note's tools no longer hide under the keyboard (`RichTextEditor.kt`)

The `DOCK` toolbar used to be laid out **inside** the note, at the foot of its own field,
which is the one place an IME can cover. It is a `Popup` now, `BottomCenter` aligned with
`focusable = false`, lifted by `maxOf(ime insets, navigation bar insets)` so it sits just
above the keys when one is up and just above the nav bar when none is. Content is capped at
`560.dp` and centred so a wide window does not stretch a row of tools across the screen, and
the enter/exit is a slide instead of a vertical expand. MainActivity calls
`enableEdgeToEdge()`, which is what makes the ime inset real rather than consumed by
`adjustResize`.

### Build speed

`gradle.properties` (build cache + parallel) and `validateTopics` (declared output) as above.

### The release

- `RELEASE_NOTES.md` is rewritten in full for **v1.3.0**, in the style the release body wants:
  grouped sections, one line per thing, and **no em dashes or en dashes anywhere** (verified
  with a grep for U+2014 and U+2013: 0 matches). `release.yml` embeds this file at the top of
  the release body, above the generated install table, and appends GitHub's own commit list.
- `versionName`'s local/PR default moves from `1.1.1` to `1.3.0`, so a build from main
  reports the release it belongs to (a `v*` tag still overrides it through `RELEASE_VERSION`).
  `versionCode` stays `20260922`, whose `fastlane` changelog is the one this release ships.
- The tag itself is `v1.3.0` (no `beta`, no `rc`, so `release.yml`'s prerelease check
  resolves to a full release).

---

## 5. Still open (carried over, unchanged by this request)

- **The forms SQL has not been pasted into Supabase**, so the ban ladder, the queue's Clear,
  and publish / read / vote / skip for a form have nothing to talk to yet; the `forms`
  permission also still needs granting to the team.
- **Stop-the-poll in moderation** (asked for, not built) needs that schema first.
- **Journal entries being slow** still needs a measurement: list open, or page load.
- **Episode lists**: only 2 of the topics carry an authored `episodes` array; content, not
  code.
- **The deprecation sweep** (about 30 `rememberModalBottomSheetState` sites, 4
  `LocalClipboardManager`, 1 `LocalLifecycleOwner`, and roughly 150 site-specific warnings)
  is unstarted; each needs a compile to verify, and Material3 here is `1.5.0-alpha20`.
- **The take editor's per-keystroke rebuild** was found by inspection only and never
  profiled.

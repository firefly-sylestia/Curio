# Curio GitHub Configuration — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. It follows the root `AGENTS.md` as its parent DOX rail.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `.github/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then this file for GitHub-specific contracts.

## Purpose

GitHub Actions automation and contributor templates for the Curio Android repository.

## Ownership

- `.github/workflows/android.yml` — Branch and pull-request verification
- `.github/workflows/release.yml` — Tag-triggered signed release publishing (Android APKs)
- `.github/workflows/desktop-release.yml` — Windows desktop installers (.exe app image + .msi) on tag releases (manual dispatch for testing)
- `.github/ISSUE_TEMPLATE/bug-report.yml` — Curio Android bug report form
- `.github/ISSUE_TEMPLATE/feature-request.yml` — Curio product and UX request form
- `.github/PULL_REQUEST_TEMPLATE.md` — Curio pull-request review template

## Local Contracts

### Android CI workflow

`android.yml` runs on pushes and pull requests targeting `main`/`Alpha`, plus manual dispatch. It has FIVE JOBS: `build` and `lint` (**each a two-job MATRIX — two editions × two phases**, see below), `report` (the reader-facing summary), `cache-cleanup`, and the disabled `desktop`.

**v465e — `build` IS A MATRIX: ONE PARALLEL JOB PER EDITION (`Curio Android · core · build`, `Curio Android · full · build`).** One job used to build both editions in a single Gradle invocation, which is why a run took ~30 minutes; `fail-fast: false` so a `core` failure does not cancel `full` — this job's whole purpose is proving BOTH build, and the run where one broke is exactly when the other's answer is most wanted.

**v466 — AND LINT IS A SECOND MATRIX BESIDE IT (`Curio Android · core · lint`), WHICH IS WHERE THE SPEED ACTUALLY CAME FROM.** Measured from a green run's own log: the job wall was **1056s**, of which **1007s was one Gradle invocation**, and inside that `lintReportCoreRelease` was **~400s**, `minifyCoreReleaseWithR8` **~362s** and `compileCoreReleaseKotlin` **~155s** — those three tasks were **91% of the run**. Critically, **lint ran LAST, after the APK had already been built, signed and verified**: 40% of the work and NONE of the critical path. Splitting it makes the path `max(build, lint)` instead of `build + lint`, so ~9.0 min against ~9.7 min lands near **10 minutes instead of 17.6** — ~43% off every push and PR, with the same APK and the same lint signal. **What it costs, stated plainly:** four runners instead of two, and the Kotlin compile is done **twice per edition** (lint needs the same compiled classes the build produces; the two jobs run simultaneously, so neither can borrow the other's output or share a Gradle cache key — that ~155s is the price of not serialising the other ~400s).

**Two things were asked for here that are NOT possible or NOT useful, so they are recorded rather than silently dropped:** (1) *"make the post set up and set up be one"* — `Post Set up Gradle` is not a second setup, it is the **teardown hook** of the same action, and its 24.6s is spent **uploading the Gradle cache** (the thing that makes the next run fast). Merging the steps would mean not saving the cache at all. The achievable half of that request is applied: the **lint** job runs `cache-read-only: true`, so it restores the cache and pays none of the upload. (2) *"build the release APK differently so it's faster"* — **the APK is not the cost.** Packaging, signing and the signature check are about **10 seconds**; the 362s is R8 in full mode over ~700 files, and turning R8's full mode off in CI only would mean CI validating a differently-built APK than `release.yml` ships. Rejected on purpose.

**⚠️ NEITHER JOB HAS A JOB-LEVEL `outputs:` BLOCK — THAT IS THE DESIGN, NOT AN OMISSION.** The build job used to publish eleven step outputs for `report` to quote. **`needs.<matrixed job>.outputs` is an aggregation whose winner GitHub does not specify**, so a report built on it would print ONE job's numbers under a heading implying all four — worse than printing nothing. Instead each job writes **`ci-report/curio-<edition>-<phase>.json`** (from `.github/scripts/build-summary.sh`), uploads it as **`curio-ci-report-<edition>-<phase>`**, and `report` merges whatever arrives via `.github/scripts/run-report.py` (which also writes the PR comment and its `<!-- curio-ci-report -->` marker). **The phase is in the artifact name and the file name on purpose: two of the four rows come from the SAME edition, so a name without it would be one artifact name with two writers.** **Adding an edition means adding a matrix entry to BOTH jobs and, if it is not `core`/`full`, a name in `run-report.py`'s `EXPECTED_EDITIONS`.** Do not reintroduce `needs.build.outputs.*` in `report`.

`build`:

- Validates all topic catalogs with the self-contained Gradle `validateTopics` task (wired into `preBuild`). The CI **build** scripts live in `.github/scripts/` — `validateTopics` is still the authority for topic data, and the scripts there only READ what the build produced (see the surfaces below); nothing else is shipped to the runner.
- Bundles `data/topics/*.json` into `app/src/main/assets/topics/` first and fails if the bundle is empty. **The JSONs are NOT in git** — that directory holds only `SCHEMA.md` — so any summary that counts catalogs without running this step reports `0 file(s) · 0 topics`, which is the *exact* v412 failure the step exists to catch. That is why `build-summary.sh` prints `— (bundled by the build runners)` for a non-build phase instead of counting.
- Runs `validateTopics assemble${{ matrix.cap }}Release` — **ONE edition's tasks per runner** (`${{ matrix.cap }}` is `Core`/`Full`, to match Gradle's own capitalisation) — using the hosted Android toolchain. **Release only**, no debug APK. **The tasks are spelled out per edition on purpose (v465b):** under the `edition` flavor dimension the bare `assembleRelease` name is AMBIGUOUS, and Gradle kills the whole invocation at task selection (in ~50s, before one line of Kotlin compiles) with `Task 'lintRelease' is ambiguous in root project 'Curio' and its subprojects` — which reads as a mysterious build failure, not a naming one. Never reintroduce an unqualified `assemble*`/`lint*` task here. **v466 removed the `lint<Cap>Release` prefix that used to sit first here** — it was there so the assemble could reuse the variant's compilation, which is what made the runner take 17.6 minutes. `validateTopics` stays. The build step is `continue-on-error: true` and a gate step re-fails the run afterwards, purely so the annotation and summary steps below still run on a FAILED build: the compiler's own errors are exactly what the Checks tab should be showing.
- Uploads the Gradle log and the universal release APKs — **one per edition (v465)** — (splits are disabled for PR/push via `-PcurioAbiSplits=false`) as throwaway artifacts with **1-day retention** (the tag release workflow attaches the permanent APK set to GitHub Releases instead). Uploads are best-effort and skip silently when Gradle failed before producing anything; the Gradle check remains authoritative.

`lint` (v466):

- Runs `lint${{ matrix.cap }}Release` — the RELEASE variant only, matching what the build ships. Lint runs the variant's compile as a dependency, which is the duplicated ~155s this job accepts in exchange for lifting the ~400s lint phase off the build runner's tail.
- **Deliberately drops three things the build job has**, and none of them is an oversight: `cache-read-only: true` (it restores the cache and pays none of the 24.6s upload teardown; it also stops a concurrent writer racing the two build runners' saves), no keystore decode (it never assembles, signs or verifies an APK), and no topic-catalog bundling (that belongs to the thing that packages the catalogs, and running it twice would report one fact under two headings). It still configures the release variant cleanly without the `KEYSTORE_*`/provider secrets because every env read in `app/build.gradle.kts` is nullable (`takeIf { it.isNotEmpty() }`) and falls back to the debug-signing config.
- Tees to **`gradle-lint.log`** (the build job writes `gradle-build.log`), and passes `GRADLE_LOG=gradle-lint.log` into `build-summary.sh`. **Without that env var a red lint run shows an empty "Failed tasks" section**, because the postmortem greps the log by name — the one page a red run is read for.
- Uploads its own Gradle log, its own lint reports, and its own report row.
- Signs the release variant with the same `KEYSTORE_*` signing secrets as the release workflow when GitHub provides them (pushes to `main`, same-repo PRs, manual dispatch) and verifies **every** release APK's signature is not the debug key, publishing its name, size and SHA-256 as outputs. On fork PRs, where GitHub strips secrets, the release variant falls back to the app module's debug-signing config so CI still passes, and the summary says so.
- Cancels an older in-progress run for the same ref when a newer run starts.
- Runs a `cache-cleanup` job on branch pushes (not PRs) that deletes GitHub Actions cache entries not accessed in the last 2 days — `gradle/actions/setup-gradle` keys the Gradle User Home cache with the commit SHA, so every push otherwise leaves fresh entries behind until GitHub's 7-day eviction.

### The run's surfaces (v405) — what a run SHOWS

A run answered exactly one question ("did it pass?") and made you read a 12,000-line log to find out why. **Four surfaces now, each read by a different person at a different moment — and this is the contract for where any new presentation belongs:**

1. **The build summary** (`$GITHUB_STEP_SUMMARY`, rendered bottom-of-page, **one page per job since v466**) — version code and name, the catalogs bundled and the topics they carry, the lint totals, the signing story, the APK's name/size/SHA-256, and which optional keyed providers this build carried. Written by `.github/scripts/build-summary.sh [signing-story] [outcome] [edition] [phase]`, which derives everything from the files on disk, so it stays correct when steps are re-ordered. Never fails the run.

   **⚠️ v466 — A NON-BUILD PHASE MUST NOT REPORT THINGS IT CANNOT KNOW.** The lint runner has no APK, did not bundle the catalogs and carries none of the provider secrets. Printing the raw values would have produced three invented findings — `Lint: no report` on a build that never lints, `Topic catalogs: 0` on a runner that never copied them (the v412 failure signature, on a runner that packages nothing), and `Keyed providers: none (keyless build)` describing an APK that runner never made. The `phase` argument gates those rows: a build runner says **`Lint — (lint runs in its own job)`** and a lint runner says **`Topic catalogs — (bundled by the build runners)`**, and the JSON row blanks `signing`/`apk`/`sha` for a non-build phase. **When you add a row to this table, ask which phases can honestly answer it.**
2. **The Checks tab** — every Kotlin `e:`/`w:` line re-emitted as an annotation with its file, line and column, written by `.github/scripts/annotate-gradle.sh [log-path]`. Both jobs run it (the build runner against `gradle-build.log`, the lint runner against `gradle-lint.log`), so a compile error can appear on both Checks entries — honest, and better than a runner whose log is readable only by downloading an artifact. **v466 added `.github/scripts/annotate-lint.py`** for lint's OWN findings, which are `lint-results-*.xml` and not compiler diagnostics: without it the new lint job would be strictly worse to read than the old combined one on exactly the runs it exists for (a lint error fails the job, and the tab would name no file and no rule). It maps Fatal/Error → `::error`, Warning → `::warning`, Information → `::notice`, prefixes module-relative paths with `app/` (AGP writes them without it, and GitHub silently drops an unresolved path), and caps at 25 per severity so a huge resource change cannot spend the whole budget on its first twenty. All three scripts are READERS of what the build produced and exit 0 on a missing log.

   **⚠️ v466 ALSO FOUND A BASH TRAP THAT HAD BEEN BREAKING THE POSTMORTEM: `echo "```"` IS NOT A MARKDOWN FENCE.** Inside DOUBLE quotes bash reads a backtick as **command substitution**, so the first two are an empty substitution, the third **opens** one, and the closing fence further down closes it — every line in between is then **EXECUTED**. In `build-summary.sh`'s "First compiler errors" block that meant bash trying to run `printf …` as a command named after the first compiler error, so the section printed its heading and then **nothing**: no red run has ever shown its compiler errors on the summary page. It also meant any `$(…)` or `$VAR` inside a compiler message was evaluated. **Fences are written with SINGLE quotes (`echo '```'`).** `bash -n` does NOT catch this — it parses cleanly — so the only way to find it is to RUN the script against a log that has an error in it and look at the page.

   **⚠️ v465e — THIS SURFACE HAD BEEN ANNOTATING NOTHING, AND THE REASON IS A ONE-CHARACTER TRAP WORTH KNOWING.** It stripped the `e: ` prefix with `rest="${line#*: }"`, which in bash is **GREEDY**: it removes the longest prefix ending in a colon-space, and `e: file:///…/File.kt:902:36 Unresolved reference 'Context'.` contains **two**. So the path became `Unresolved`, `lineno` became `reference`, the numeric guard `continue`d, and every error was dropped on the floor — a red run showed an **empty Checks tab** and the only way to read it was to download the Gradle log artifact by hand. It cost real time twice (the v463 name clash, and the v465e missing import). **The prefix is now dropped by LENGTH** (`rest="${line:10}"` — `"e: file://"` and `"w: file://"` are both exactly 10), which no amount of re-escaping can get wrong. A quoted pattern inside a parameter expansion is itself a trap: the first attempt at this fix landed as `#\"e: …\"`, whose pattern contains literal backslashes and matched nothing either. **Verify this surface after ANY change to it** by running the script against a saved log and confirming it prints `::error file=…,line=…,col=…`, not just that it exits 0.
3. **The `report` job** — one extra check whose entire body is the run report, **built from artifacts, never from step outputs** (`needs: [build, lint]`; `if: always() && needs.build.result != 'cancelled'`, so a failed build still produces it). It has a 4-column table — two editions × build|lint — where a missing row is **named** (`❌ no report (the job did not reach its summary step)`) rather than left quietly empty, and the APK/signing rows are blanked for a lint column. Anything the report must say therefore belongs in the **JSON row** each job writes, not in a step output.
4. **The pull request comment** — the same report, upserted (its own previous comment is deleted first, matched by the `<!-- curio-ci-report -->` marker, so there is one live comment per PR and not one per push). **Same-repo PRs only:** `permissions: pull-requests: write` lives on the `report` job, and a fork PR's token is read-only, so the step is gated rather than failing.

Artifact/annotation/comment copy stays Curio-specific; never echo a secret's value into a summary (only whether it was present).

**Ideas for more surfaces, not yet built** (pick one and it belongs in the list above): a **timing table** (per-Gradle-task wall time, parsed from `--profile` or the log's own `Task :x` stamps) so a slow build says WHICH task got slower; a **build-size trend** (the APK's bytes committed as a tiny JSON and compared with the previous run's, so "the APK grew 2 MB" is a sentence and not a discovery); a **catalog diff** (which `data/topics/*.json` changed and by how many topics, from the git diff of the base ref); a **step-level timing/expense line** (GitHub's own billable minutes per job); a **failure digest** that groups the errors by file into one comment instead of 260 annotations; a **coverage/unit-test row** the moment `./gradlew test` is wired into the job; and a **sticky status comment on the issue a PR closes** ("this fix is in 1.1.2") once releases are tagged from the same pipeline.

### Release workflow (Android)

`release.yml` runs only for `v*` tags. It:

- Requires `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
- **Bundles the topic catalog first** (`rm -rf app/src/main/assets/topics && cp data/topics/*.json app/src/main/assets/topics/`, fail if empty — the same step `android.yml` runs): the JSON lives in `data/topics/` in git and `assets/topics/` ships only `SCHEMA.md`, so a release build without this step produces an APK with NO topic data — Room has nothing to import, and the Topic Database opens empty. This exact gap shipped in every release before it was caught.
- Decodes the repository keystore, runs `validateTopics assembleCoreRelease assembleFullRelease` (per-edition names — the bare `assembleRelease` is ambiguous once flavors exist, see the `build` job above), and verifies the signature of **every** produced APK is not the Android debug key using the available Android build-tools. **This workflow is deliberately NOT split into per-edition jobs the way `android.yml` is (v466): a tag release publishes one signed set, and the release body and the "expected splits" guard read it as one artifact group.**
- Produces a **universal APK plus per-ABI APKs** (armeabi-v7a, arm64-v8a — v204: the release buildType's `ndk.abiFilters` restrict native libs to the two arm ABIs; x86/x86_64 are emulator-only legacy and no longer shipped) via the ABI splits in `app/build.gradle.kts`, so each device can install the smallest file that matches its CPU. The release ABI diet halves the bundled Vosk `libvosk.so` footprint in the universal release APK (4 ABIs ≈ 38MB → 2 ABIs ≈ 19MB); debug builds keep all four ABIs for emulator testing.
- Renames every APK to a device-friendly name — `Curio-{versionName}-{versionCode}-{edition}-{abi}-Android{min}+.apk` (Android 8.0+ = `minSdk 26`) — using version numbers read from the `printReleaseVersion` Gradle task (single source of truth: `defaultConfig`), and publishes a release body that explains which APK fits which device.
- **v465 — TWO EDITIONS SHIP FROM EVERY TAG.** the `edition` flavor dimension (`app/build.gradle.kts`) produces `core` (`com.curio.app`, the smaller app) and `full` (`com.curio.app.full`), so: the APKs live at **`app/build/outputs/apk/<flavor>/release/`** — one directory level deeper than before, and a glob still reading `apk/release/` matches NOTHING (the failure reads as "No signed release APK was produced", i.e. a signing fault rather than a path fault, which is why it is called out here); the rename step walks `*/release/*.apk`, reads the edition from the first path segment, and renames **in place** so the publish step's `app/build/outputs/apk/*/release/*.apk` glob still finds the files; and the belt-and-braces "expected splits" guard now checks **every edition × ABI**, so a release that silently lost an edition fails instead of publishing a partial set. The job timeout went 30 → 45 minutes because the compile and lint work doubles. **A third edition means touching that guard, the release body's edition list and `UpdateChecker`'s token match together.**
- **Tag version is the build version:** the workflow exports the tag (e.g. `v1.2.3`) as `RELEASE_VERSION`, and `app/build.gradle.kts` uses it as `versionName` with the leading `v` stripped (`1.2.3`; prerelease suffixes like `-alpha` survive). Local dev and PR CI don't set the env var, so the default `1.0.0` applies there. `versionCode` stays the date-based value — only the version name follows the tag.
- Publishes the release APKs through a GitHub Release, marking `alpha`, `beta`, and `rc` tags as prereleases.
- Never falls back to debug signing for a published release.

### Desktop CI job (android.yml) + desktop release workflow

**DISABLED until the desktop app is finished.** Both desktop build paths
are gated with `if: false` (the `desktop` job in `android.yml` and the
`windows` job in `desktop-release.yml`), so neither push/PR CI nor tag
pushes build the desktop module. Re-enable by flipping both gates to
`if: true`. When active:

- The `desktop` job in `android.yml` compiles the JVM/desktop module on
  **every push and PR** so the port can't silently rot. The compiled JAR
  is uploaded as an artifact **only on branch pushes** (`main`/`Alpha`) —
  PR runs skip the upload (a 4MB jar per PR commit was piling up in
  artifact storage) — and with **1-day retention**, matching the
  release-APK policy.
- `desktop-release.yml` (below) is **tag-only**: it runs on a
  **windows-latest** runner on the same `v*` tags as the Android release
  workflow (plus manual dispatch for testing — PR/push CI builds just the
  Android APK, see `android.yml`). It:

- Requires NO secrets — the desktop port has no signing story yet (jpackage
  code signing is optional and unconfigured).
- Installs the **WiX Toolset** via chocolatey (jpackage needs it to build
  the `.msi`) and exposes it via `WIX`/`PATH`. The install folder is
  VERSIONED (v3.11, v3.14, ...) and changes between runner images — the
  step DISCOVERS it (`WiX Toolset*` glob) instead of hardcoding a version.
  jpackage itself locates the toolset by scanning Program Files, so
  `WIX`/`PATH` are belt-and-braces: `WIX` = installation root (the standard
  `%WIX%` convention), `PATH` = the `bin` dir.
- Compiles the module FIRST (`:desktop:build`) so code errors fail fast
  with a clear log before the slow WiX/jpackage steps, then runs
  `:desktop:createDistributable` AND `:desktop:packageDistributionForCurrentOS`
  — on Windows this builds the app image (contains `Curio.exe`, left on
  disk by `createDistributable`) plus the `.msi` installer (built by
  `packageDistributionForCurrentOS`; its `packageMsi` task consumes its own
  jpackage image internally and does NOT leave the app image behind, so
  both tasks must run for the portable zip to exist); `Dmg`/`Deb` are
  macOS/Linux formats and are skipped.
- Zips the app image into a **portable** `Curio-Windows-{version}-portable.zip`
  and attaches both it and the `.msi` to the GitHub release on tags, next
  to the Android APKs published by `release.yml`. Manual-dispatch runs
  (no tag) upload the same two files as run artifacts
  (`curio-desktop-windows-*`, 7-day retention) so the proper `.exe` can be
  downloaded for testing — the release-only steps (release body, prerelease
  detection, `action-gh-release` publish) are gated on
  `startsWith(github.ref, 'refs/tags/')`. `RELEASE_VERSION` is only set for
  tag runs; manual-dispatch runs keep the module's default `1.0.0` package
  version (and the zip's default name) because jpackage rejects
  non-numeric versions.
- **Tag version is the package version:** exports `RELEASE_VERSION` (tag
  minus `v`) so `desktop/build.gradle.kts` versions the installer from the
  tag, mirroring the Android convention. jpackage validates
  `packageVersion` per bundle format, and the Windows MSI is the strict one:
  it requires exactly `MAJOR.MINOR.BUILD` (255 / 255 / 65535). The desktop
  module therefore NORMALIZES the tag instead of trusting it — prerelease/
  build suffixes stripped (`v1.0.2-beta` → `1.0.2`) and missing components
  padded with 0 (`v2.1-beta6` → `2.1.0`, `v2` → `2.0.0`) — and a tag that
  yields nothing jpackage accepts falls back to `1.0.0` with a build
  warning. A two-component tag used to fail **configuration**
  (`Illegal version for 'Msi': '2.1'`), and since Gradle configures every
  project before running `:app:assembleRelease`, that broke the Android
  release on the same tag (v2.1-beta6). The Android `versionName` is a plain
  string and keeps its suffix. The portable zip name keeps the full tag
  (distinguishes prerelease from later stable artifacts); the release body's
  MSI row uses the installer's ACTUAL file name (`CURIO_MSI_NAME`, published
  by the collect step) rather than re-deriving the rule, so the body can
  never name a file the build did not produce.
- Publishes through GitHub Releases with the same `alpha`/`beta`/`rc`
  prerelease detection as the Android workflow, and `update_release_body:
  false` so it never clobbers the Android workflow's release body when both
  run on the same tag.
- Validates the build output with hard guards: the `.msi` must exist, the
  app image must contain `Curio.exe`; unmatched upload files fail the run.

### Editable release note (`docs/RELEASE_NOTES.md`)

`docs/RELEASE_NOTES.md` is the EDITABLE release note the user maintains.
**It was `RELEASE_NOTES.md` at the repo root until the root-cleanup pass**
(the root keeps its DOX rail, its build files and its two standard files);
the path is read literally by both workflows below, so moving it again means
editing them too — the "file is missing" branch is silent, and a misplaced
note would simply stop reaching releases. Both tag workflows embed it at the
TOP of the GitHub release body when they create the release:

- `release.yml` (Android) and `desktop-release.yml` (Windows) read the
  file on tag runs and prepend its content, followed by a `---` rule,
  ABOVE their auto-generated install guide (the APK table for Android;
  the portable zip / MSI table for desktop). The install help is always
  appended after the note, never replaced by it.
- The file is always included when present — no template guard. The user
  updates it before tagging; the v1.0 launch copy is the shipped default.
- The desktop workflow only writes a body when IT creates the release
  first (same `update_release_body: false` race handling), so either
  workflow winning the race still ships the note.

### Contributor templates

- Bug reports collect reproducible steps, expected and actual behavior, Curio area, app/device versions, logs, and sanitized screenshots.
- Feature requests collect the user problem, proposed experience, product area, expected scope, alternatives, and references.
- Pull requests identify change type, affected Curio experience, validation, visual evidence, data/permission impact, and reviewer checks.

### Secrets

The release workflow requires the signing secrets; the Android CI workflow consumes them when GitHub provides them (fork PRs do not receive secrets):

- `KEYSTORE_BASE64` — Base64-encoded Android keystore
- `KEYSTORE_PASSWORD` — Keystore password
- `KEY_ALIAS` — Signing key alias
- `KEY_PASSWORD` — Signing key password

Optional build-config secrets are exported to the Gradle build and baked into `BuildConfig`; when unset, the feature they configure degrades quietly instead of failing the build:

- `SUPABASE_URL` + `SUPABASE_PUBLISHABLE_KEY` (or `SUPABASE_ANON_KEY`) — Supabase project URL and public client key for account sign-in and Online Mode. **Both** `android.yml` and `release.yml` export it, and `app/build.gradle.kts` reads `SUPABASE_PUBLISHABLE_KEY` before falling back to `SUPABASE_ANON_KEY`, so the repo works whichever name is configured; `android.yml` logs a warning when neither is present so an unconfigured APK is visible in the log. The **service-role key must never be added as a secret or exported to a build** — it would ship inside the APK.
- `CURIO_AUTH_SITE_URL` — the Curio account site (`auth-web/`, deployed on Vercel). It is the `redirect_to` every confirmation and password reset email carries and the URL behind the sign-in form's "Forgot your password?" row. **Optional and empty by default**: unset leaves Supabase's own Site URL in charge of email links (which is what sent members to `http://localhost:3000`) and hides the recovery row, so a build from before the site exists is unchanged. Both `android.yml` and `release.yml` export it.
- `GOOGLE_BOOKS_API_KEY`, `LIBRARY_THING_API_KEY` — keyed cover providers (unset keeps the keyless paths).
- `SPOTIFY_CLIENT_ID` + `SPOTIFY_CLIENT_SECRET` — Spotify client-credentials flow for music topics (unset keeps the search links).
- `OMDB_API_KEY` — OMDb key (free tier: 1,000 requests/day) for a FILM's or a SERIES' plot, poster, IMDb rating and runtime **asked by name**. It is the second keyed door of its kind (v429): every door in front of it may be empty for a title a catalogue files differently, and OMDb is the one that answers a *description* where the keyless pair does not. **Exported by BOTH `android.yml` and `release.yml`**, empty string when unset (a keyless build never makes an OMDb request). Its fetcher is `features/reveal/OmdbFetch.kt`; its consumers are the Incursion row chain (`features/incursion/IncursionSources.kt`) and, behind the keyless pair, a row's own artwork. The source lab carries an `omdb` row with this key's name, so the Dev page reports it exactly as the app would.
- `TMDB_API_KEY` — TMDB (The Movie Database) key for film/anime artwork, a film's own facts and a show's episode list. **Exported by BOTH `android.yml` and `release.yml`** (before v389f only `android.yml` had the optional provider keys, so a tagged release shipped without them while CI builds carried them — the published APK is the one that matters).

### Adding a keyed provider (worked guide: TMDB)

The optional keys above are not magic: each one is an explicit fetcher reading a `BuildConfig` field. Adding a new provider is four edits, and TMDB (`v389f`) is the reference for all four:

1. **`app/build.gradle.kts`** — read the env var at the top of the file next to `envGoogleBooksApiKey`, escape it for the generated string literal (the `replace("\\", …)` / `replace("\"", …)` pair every other key uses), then declare it in `defaultConfig` with `buildConfigField("String", "TMDB_API_KEY", "\"$tmdbEscaped\"")`. An unset key must become an EMPTY STRING, never the literal text `null` — every fetcher checks `isNotBlank()` before it builds a URL.
2. **Both workflows** — add the secret to the build step's `env:` block in `android.yml` AND `release.yml`. A key in only one of them means the published release and the CI build disagree about what the app can do, which is exactly the bug v389f fixed for the older provider keys. Do NOT add a workflow-level `env:`; it belongs on the build step so the secret's reach is visible in the log.
3. **`app/src/main/java/com/curio/app/features/reveal/…Fetch.kt`** — the fetcher itself. Read the key through `BuildConfig.TMDB_API_KEY`, keep it keyless-first where a free source already answers (the project's rule is FREE SOURCE FIRST, keyed source as the upgrade), memoise the answer per query in a `ConcurrentHashMap` so a reopen never re-asks, and wrap every call in `runCatching` — a keyed provider that fails must degrade to the keyless one, never to an empty screen.
4. **`.env.example`** — add the source to the map at the top (what it feeds, the free tier it actually enforces, and its `→` env var) and a blank `TMDB_API_KEY=` line under the optional keys. A provider with no row in that map is invisible to the next person setting a build up.

**TMDB's SECOND credential (v428).** TMDB issues both a **v3 API key** (`TMDB_API_KEY`, sent as `?api_key=`) and an **API Read Access Token** (`TMDB_READ_TOKEN`, a JWT sent as `Authorization: Bearer …`, accepted on v3 and v4 — see its application-authentication page). `TmdbFetch` reads the token first and falls back to the key, and a JWT pasted into `TMDB_API_KEY` is recognised as the token, so either secret alone is enough; both are optional and an unset one is an empty string. If both are set, both must be exported by `android.yml` and `release.yml` like every other provider key.

**Setting the secret in GitHub:** repo **Settings > Secrets and variables > Actions > Secrets tab > New repository secret**, name it exactly `TMDB_API_KEY` (or `TMDB_READ_TOKEN`), paste the vendor's key, save. Fork pull requests never receive secrets — GitHub strips them — so a fork PR builds keyless and must still pass; that is why every fetcher degrades instead of failing. To rotate a key, update the secret in place (no workflow edit needed) and re-run the workflow.

**Never commit a key.** `.env.example` ships with every key blank on purpose, and no workflow ever echoes a key value.

## Work Guidance

- Keep workflow names, artifact names, and user-facing copy Curio-specific.
- Use the current Node 24-compatible artifact action (`actions/upload-artifact@v6`); do not opt into deprecated Node 20 with `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION`.
- Keep every workflow and template focused on the current Curio product and its Android delivery path.
- Keep release signing mandatory and never commit keystores or decoded credentials.
- Update this contract whenever workflow triggers, required secrets, artifact behavior, or template fields change.
- Do not run Gradle compile, build, lint, or test commands in the local workspace; CI performs those checks.

## Verification

- Validate changed YAML with a YAML parser or GitHub's workflow checks when available.
- Run `git diff --check` and inspect the rendered template structure.
- The Gradle `validateTopics` task is the CI-authoritative topic validation. Authoring/validation scripts under `scripts/` are untracked (kept on disk only) — see the `.gitignore` note.
- Confirm no secrets, generated APKs, or release keystores are tracked.

## Child DOX Index

No child AGENTS.md files defined yet.

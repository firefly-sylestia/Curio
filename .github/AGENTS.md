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

`android.yml` runs on pushes and pull requests targeting `main`/`Alpha`, plus manual dispatch. It has FOUR JOBS: `verify` (the build), `report` (the reader-facing summary), `cache-cleanup`, and the disabled `desktop`. `verify`:

- Validates all topic catalogs with the self-contained Gradle `validateTopics` task (wired into `preBuild`). The CI **build** scripts live in `.github/scripts/` — `validateTopics` is still the authority for topic data, and the two scripts there only READ what the build produced (see the surfaces below); nothing else is shipped to the runner.
- Bundles `data/topics/*.json` into `app/src/main/assets/topics/` first, records the catalog count and the topic total as step outputs, and fails if the bundle is empty.
- Runs the Gradle `lintDebug`, `validateTopics`, and `assembleRelease` checks in GitHub Actions using the hosted Android toolchain — **release build only**, no debug APK is produced (debug remains available for local development via the app's debug build type). The build step is `continue-on-error: true` and a gate step re-fails the run afterwards, purely so the annotation and summary steps below still run on a FAILED build: the compiler's own errors are exactly what the Checks tab should be showing.
- Uploads the Gradle log, the lint reports and the universal release APKs — **one per edition (v465)** — (splits are disabled for PR/push via `-PcurioAbiSplits=false`) as throwaway artifacts with **1-day retention** (the tag release workflow attaches the permanent APK set to GitHub Releases instead). Uploads are best-effort and skip silently when Gradle failed before producing anything; the Gradle check remains authoritative.
- Signs the release variant with the same `KEYSTORE_*` signing secrets as the release workflow when GitHub provides them (pushes to `main`, same-repo PRs, manual dispatch) and verifies **every** release APK's signature is not the debug key, publishing its name, size and SHA-256 as outputs. On fork PRs, where GitHub strips secrets, the release variant falls back to the app module's debug-signing config so CI still passes, and the summary says so.
- Cancels an older in-progress run for the same ref when a newer run starts.
- Runs a `cache-cleanup` job on branch pushes (not PRs) that deletes GitHub Actions cache entries not accessed in the last 2 days — `gradle/actions/setup-gradle` keys the Gradle User Home cache with the commit SHA, so every push otherwise leaves fresh entries behind until GitHub's 7-day eviction.

### The run's surfaces (v405) — what a run SHOWS

A run answered exactly one question ("did it pass?") and made you read a 12,000-line log to find out why. **Four surfaces now, each read by a different person at a different moment — and this is the contract for where any new presentation belongs:**

1. **The build summary** (`$GITHUB_STEP_SUMMARY` in `verify`, rendered bottom-of-page) — version code and name, the variant, the catalogs bundled and the topics they carry, the signing story, the APK's name/size/SHA-256, the lint totals, and which optional keyed providers this build carried. Written by `.github/scripts/build-summary.sh [signing-story]`, which derives everything from the files on disk, so it stays correct when steps are re-ordered. Never fails the run.
2. **The Checks tab** — every Kotlin `e:`/`w:` line re-emitted as an annotation with its file, line and column, written by `.github/scripts/annotate-gradle.sh [log-path]` (counts → step outputs, the per-file warning table → the summary). Both scripts are READERS of what the build produced and exit 0 on a missing log.
3. **The `report` job** — one extra check whose entire body is the run report, built from `verify`'s step outputs (`if: always() && needs.verify.result != 'cancelled'`, so a failed build still produces it). Anything the build must publish for it to say belongs in `verify`'s `outputs:` block.
4. **The pull request comment** — the same report, upserted (its own previous comment is deleted first, matched by the `<!-- curio-ci-report -->` marker, so there is one live comment per PR and not one per push). **Same-repo PRs only:** `permissions: pull-requests: write` lives on the `report` job, and a fork PR's token is read-only, so the step is gated rather than failing.

Artifact/annotation/comment copy stays Curio-specific; never echo a secret's value into a summary (only whether it was present).

**Ideas for more surfaces, not yet built** (pick one and it belongs in the list above): a **timing table** (per-Gradle-task wall time, parsed from `--profile` or the log's own `Task :x` stamps) so a slow build says WHICH task got slower; a **build-size trend** (the APK's bytes committed as a tiny JSON and compared with the previous run's, so "the APK grew 2 MB" is a sentence and not a discovery); a **catalog diff** (which `data/topics/*.json` changed and by how many topics, from the git diff of the base ref); a **step-level timing/expense line** (GitHub's own billable minutes per job); a **failure digest** that groups the errors by file into one comment instead of 260 annotations; a **coverage/unit-test row** the moment `./gradlew test` is wired into the job; and a **sticky status comment on the issue a PR closes** ("this fix is in 1.1.2") once releases are tagged from the same pipeline.

### Release workflow (Android)

`release.yml` runs only for `v*` tags. It:

- Requires `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
- **Bundles the topic catalog first** (`rm -rf app/src/main/assets/topics && cp data/topics/*.json app/src/main/assets/topics/`, fail if empty — the same step `android.yml` runs): the JSON lives in `data/topics/` in git and `assets/topics/` ships only `SCHEMA.md`, so a release build without this step produces an APK with NO topic data — Room has nothing to import, and the Topic Database opens empty. This exact gap shipped in every release before it was caught.
- Decodes the repository keystore, runs `validateTopics assembleRelease`, and verifies the signature of **every** produced APK is not the Android debug key using the available Android build-tools.
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

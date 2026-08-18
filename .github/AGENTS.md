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

`android.yml` runs on pushes and pull requests targeting `main`/`Alpha`, plus manual dispatch. It:

- Validates all topic catalogs with the self-contained Gradle `validateTopics` task (wired into `preBuild`); no external scripts are shipped in the repo.
- Runs the Gradle `lintDebug`, `validateTopics`, and `assembleRelease` checks in GitHub Actions using the hosted Android toolchain — **release build only**, no debug APK is produced (debug remains available for local development via the app's debug build type).
- Uploads lint reports plus the single universal release APK (splits are disabled for PR/push via `-PcurioAbiSplits=false`) as throwaway artifacts with **1-day retention** (the tag release workflow attaches the permanent APK set to GitHub Releases instead). Lint-report upload is best-effort and silently skips the artifact when Gradle fails before producing reports; the Gradle check remains authoritative.
- Signs the release variant with the same `KEYSTORE_*` signing secrets as the release workflow when GitHub provides them (pushes to `main`, same-repo PRs, manual dispatch) and verifies **every** release APK's signature is not the debug key. On fork PRs, where GitHub strips secrets, the release variant falls back to the app module's debug-signing config so CI still passes.
- Cancels an older in-progress run for the same ref when a newer run starts.
- Runs a `cache-cleanup` job on branch pushes (not PRs) that deletes GitHub Actions cache entries not accessed in the last 2 days — `gradle/actions/setup-gradle` keys the Gradle User Home cache with the commit SHA, so every push otherwise leaves fresh entries behind until GitHub's 7-day eviction.

### Release workflow (Android)

`release.yml` runs only for `v*` tags. It:

- Requires `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
- Decodes the repository keystore, runs `validateTopics assembleRelease`, and verifies the signature of **every** produced APK is not the Android debug key using the available Android build-tools.
- Produces a **universal APK plus per-ABI APKs** (armeabi-v7a, arm64-v8a, x86, x86_64) via the ABI splits in `app/build.gradle.kts`, so each device can install the smallest file that matches its CPU.
- Renames every APK to a device-friendly name — `Curio-{versionName}-{versionCode}-{abi}-Android{min}+.apk` (Android 8.0+ = `minSdk 26`) — using version numbers read from the `printReleaseVersion` Gradle task (single source of truth: `defaultConfig`), and publishes a release body that explains which APK fits which device.
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
  tag, mirroring the Android convention. jpackage requires a strictly
  numeric version (`MAJOR[.MINOR][.PATCH]`) for DMG/MSI metadata, so the
  desktop module strips prerelease/build suffixes (`v1.0.2-beta` → `1.0.2`)
  from `packageVersion` — the Android `versionName` is a plain string and
  keeps the suffix. The portable zip name keeps the full tag (distinguishes
  prerelease from later stable artifacts); the MSI is named from the numeric
  package version, and the release body mirrors that (`msiVersion`).
- Publishes through GitHub Releases with the same `alpha`/`beta`/`rc`
  prerelease detection as the Android workflow, and `update_release_body:
  false` so it never clobbers the Android workflow's release body when both
  run on the same tag.
- Validates the build output with hard guards: the `.msi` must exist, the
  app image must contain `Curio.exe`; unmatched upload files fail the run.

### Editable release note (`RELEASE_NOTES.md`)

`RELEASE_NOTES.md` at the repo root is the EDITABLE release note the
user maintains. Both tag workflows embed it at the TOP of the GitHub
release body when they create the release:

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

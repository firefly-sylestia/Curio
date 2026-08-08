# Curio GitHub Configuration — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. It follows the root `AGENTS.md` as its parent DOX rail.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `.github/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then this file for GitHub-specific contracts.

## Purpose

GitHub Actions automation and contributor templates for the Curio Android repository.

## Ownership

- `.github/workflows/android.yml` — Branch and pull-request verification
- `.github/workflows/release.yml` — Tag-triggered signed release publishing
- `.github/ISSUE_TEMPLATE/bug-report.yml` — Curio Android bug report form
- `.github/ISSUE_TEMPLATE/feature-request.yml` — Curio product and UX request form
- `.github/PULL_REQUEST_TEMPLATE.md` — Curio pull-request review template

## Local Contracts

### Android CI workflow

`android.yml` runs on pushes and pull requests targeting `main`, plus manual dispatch. It:

- Validates all topic catalogs with `python3 scripts/validate_topics.py`.
- Runs the Gradle `lintDebug`, `validateTopics`, and `assembleRelease` checks in GitHub Actions using the hosted Android toolchain — **release build only**, no debug APK is produced (debug remains available for local development via the app's debug build type).
- Uploads lint reports plus the release-variant APKs (universal + per-ABI splits) for 14 days. Lint-report upload is best-effort and silently skips the artifact when Gradle fails before producing reports; the Gradle check remains authoritative.
- Signs the release variant with the same `KEYSTORE_*` signing secrets as the release workflow when GitHub provides them (pushes to `main`, same-repo PRs, manual dispatch) and verifies **every** release APK's signature is not the debug key. On fork PRs, where GitHub strips secrets, the release variant falls back to the app module's debug-signing config so CI still passes.
- Cancels an older in-progress run for the same ref when a newer run starts.

### Release workflow

`release.yml` runs only for `v*` tags. It:

- Requires `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
- Decodes the repository keystore, runs `validateTopics assembleRelease`, and verifies the signature of **every** produced APK is not the Android debug key using the available Android build-tools.
- Produces a **universal APK plus per-ABI APKs** (armeabi-v7a, arm64-v8a, x86, x86_64) via the ABI splits in `app/build.gradle.kts`, so each device can install the smallest file that matches its CPU.
- Renames every APK to a device-friendly name — `Curio-{versionName}-{versionCode}-{abi}-Android{min}+.apk` (Android 8.0+ = `minSdk 26`) — using version numbers read from the `printReleaseVersion` Gradle task (single source of truth: `defaultConfig`), and publishes a release body that explains which APK fits which device.
- Publishes the release APKs through a GitHub Release, marking `alpha`, `beta`, and `rc` tags as prereleases.
- Never falls back to debug signing for a published release.

### Release workflow

`release.yml` runs only for `v*` tags. It:

- Requires `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
- Decodes the repository keystore, runs `validateTopics assembleRelease`, and verifies the APK signature is not the Android debug key using the available Android build-tools.
- Publishes the release APK through a GitHub Release, marking `alpha`, `beta`, and `rc` tags as prereleases.
- Never falls back to debug signing for a published release.

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
- Run `python3 scripts/validate_topics.py` when repository changes touch the Curio data or CI validation path.
- Confirm no secrets, generated APKs, or release keystores are tracked.

## Child DOX Index

No child AGENTS.md files defined yet.

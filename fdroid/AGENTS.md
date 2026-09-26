# fdroid/ — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. It follows the root `AGENTS.md` as its parent rail.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `fdroid/AGENTS.md` (this file)

## Purpose

Material for listing Curio on **F-Droid** — a store where F-Droid's own builder compiles the app from the tagged source. **Nothing in this folder participates in the app's build or CI**; it is merge-request material for the [`fdroid/fdroiddata`](https://gitlab.com/fdroid/fdroiddata) repository.

## Ownership

- `fdroid/com.curio.app.yml` — the build-metadata DRAFT. Copy to `fdroiddata → metadata/com.curio.app.yml` when opening the inclusion MR. It is NOT read by Gradle, this repo's workflows, or the app.
- `fdroid/README.md` — the inclusion checklist: what is done, what is missing, and the eight steps from fork to live listing.

## Local Contracts

- **F-Droid builds the CORE flavor** (`gradle: core`), not this repo's `fdroid` build type — the build type only produces this repo's own comparable artifact and is currently OFF in the release workflow (v486 markers in `.github/workflows/release.yml`).
- **The `prebuild` line in the metadata is load-bearing**: the topic catalog ships as `data/topics/*.json` and the app's own CI copies it into `app/src/main/assets/topics/` at build time. A clean F-Droid checkout without that copy ships an EMPTY topic catalog.
- **The `scandelete` line covers the vendored AAR**: `app/libs/sherpa-onnx-1.13.8.aar` is committed in the tree (full-edition only) and F-Droid's blob scanner will flag it; the metadata removes it from their build.
- **The updater gate stays on regardless of what this repo publishes**: `UpdateChecker.isFdroidBuild` silences in-app updates on any `-fdroid` versionName, because F-Droid policy forbids self-updating apps.
- Version blocks in the metadata must match the tag's OWN versionCode/versionName in `app/build.gradle.kts` (v1.4.0 → 20260923/1.4.0 verified).

## Work Guidance

- Update `com.curio.app.yml` (in this folder AND in the fdroiddata MR) when a future build needs a new `prebuild`/`scandelete` shape — keep the draft the single source.
- Do not wire anything here into CI. F-Droid builds happen on F-Droid's infrastructure.
- When the listing goes live, update the README's distribution section and remove the "switched off" note on the `fdroid` build type if it is being re-enabled.

## Verification

- Metadata correctness is validated by `fdroid lint` / fdroiddata's GitLab CI, not by this repo's CI.
- The facts this draft relies on are checked against git tags and `app/build.gradle.kts`; re-verify the version pair before submitting.

## Child DOX Index

No child AGENTS.md files defined yet.

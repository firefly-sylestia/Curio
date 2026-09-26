# F-Droid inclusion — the step-by-step for Curio

The metadata draft lives in [`com.curio.app.yml`](com.curio.app.yml) in this
folder. This README is the checklist for turning it into a live listing.
**Nothing in this folder affects the app's own build** — it is MR material
only.

## Where things stand (checked 2026-09-26)

| Requirement | Status |
|---|---|
| Public source repo | ✅ github.com/firefly-sylestia/Curio |
| FOSS license file | ✅ AGPL-3.0 (`LICENSE`) |
| FOSS dependencies only | ✅ core flavor is FOSS by construction (all non-free deps are `fullImplementation`) |
| Buildable from command line | ✅ Gradle, no proprietary tooling |
| Author consent | ✅ (you are the author) |
| Version tags | ✅ every release is tagged (`v1.4.0`, …) |
| Fastlane descriptions | ✅ `short_description.txt` + `full_description.txt` added (F-Droid reads this exact structure) |
| In-app updater off on store builds | ✅ `UpdateChecker.isFdroidBuild` gates it (F-Droid policy) |
| `fastlane/.../images/icon.png` | ❌ TODO — copy from `app/src/main/res/drawable-nodpi/ic_launcher_icon.png` (a 2048×2048 PNG already in the repo) |
| `fastlane/.../images/phoneScreenshots/` | ❌ TODO — capture 2+ phone screenshots (see `docs/screenshots/README.md` for framing) |

## Steps

1. **Finish the ❌ rows above.** Add `icon.png` (resize the launcher icon to
   512×512) and at least two `phoneScreenshots/` PNGs. Commit straight to
   `main` — these files are what the F-Droid client renders for the listing.

2. **Push the `v1.4.6` tag** if it does not exist yet — the second build
   block in the metadata references it. (First block, v1.4.0, is already
   tagged.) Alternatively drop the 1.4.6 block and let auto-update add it
   after inclusion.

3. **Fix the stray `v2.1-beta6` tag** (2026-09-14 — looks like a typo of
   `v1.2.0-beta6`). It sorts above v1.4.x and will confuse the update check.
   Delete it, or keep the `UpdateCheckIgnore` line in the metadata.

4. **Fork [fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata)** on
   GitLab, clone it, and branch `com.curio.app` off `master`.

5. **Copy the draft in**: `cp com.curio.app.yml <fork>/metadata/`. Then run
   `fdroid rewritemeta com.curio.app` + `fdroid lint com.curio.app` if you
   have fdroidserver installed (lint needs Python; it is worth it once).

6. **Test the build** the easy way: push your fork — fdroiddata's GitLab CI
   builds every metadata MR automatically. If it goes red, read the log; the
   most likely failure is the `prebuild` path (topic-JSON copy) or the
   `scandelete` path (the vendored AAR at v1.4.6).

7. **Open the MR** to fdroid/fdroiddata with the **New App** label. A
   packager reviews the source (licenses, blobs, deps, tracking). Expect
   questions — especially about `app/libs/sherpa-onnx-1.13.8.aar`, which
   their scanner will notice is committed in the tree even though it never
   enters a core APK. The answer: it is `fullImplementation`-only and the
   `scandelete` line removes it from every build they make.

8. **After inclusion**: every new `v<version>` tag produces an automatic
   update MR in fdroiddata. Nothing more to do per release, as long as
   versionCode keeps increasing (it is date-based, so it does) and the core
   flavor stays FOSS (the standing rule: non-free deps only ever enter via
   `fullImplementation`).

## Notes

- F-Droid builds are **debug-signed with F-Droid's key**; users pick either
  their build or this repo's APKs, never both interchangeably. The
  `com.curio.app` applicationId is the same in both.
- The repo's own `fdroid` build type (in `app/build.gradle.kts`) is NOT what
  F-Droid runs — they run `assembleCoreRelease` via `gradle: core`. The
  build type exists only for this repo's own comparable artifact, and it is
  currently switched OFF in the release workflow (`v486` markers).
- Reproducible builds are optional but encouraged; core has no native code,
  which is the easy case. Worth attempting after inclusion, NOT before —
  switching signing keys later is a user-hostile reinstall.

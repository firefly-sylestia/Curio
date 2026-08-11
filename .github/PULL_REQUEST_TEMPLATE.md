## Summary

<!-- What changed, and why? Link the issue with `Fixes #123` or `Related to #123` when applicable. -->

## Change type

- [ ] Bug fix
- [ ] User-facing feature or interaction
- [ ] UI/visual refinement
- [ ] Topic data/content change
- [ ] Persistence, backup, or migration change
- [ ] Build, CI, or release change
- [ ] Documentation or repository maintenance
- [ ] Refactor with no intended behavior change

## Curio experience

<!-- Which screen, route, capture format, topic catalog, or workflow is affected? -->

## What changed

<!-- List the important implementation details and any intentional behavior changes. -->

## Validation

- [ ] Topic data was validated (`./gradlew validateTopics` — the CI-authoritative check) when topic files were affected.
- [ ] I ran relevant safe static checks.
- [ ] I tested the affected flow on an Android device or emulator.
- [ ] I checked loading, empty, error, and long-content states where relevant.
- [ ] I verified the change does not remove existing content or user data unexpectedly.
- [ ] CI will run the required Gradle lint, validation, debug build, and release-variant build checks.

> Local Gradle compile, build, lint, and test commands are intentionally not run in this repository workspace; GitHub Actions is the build source of truth.

## Visual evidence

<!-- Add before/after screenshots or a short recording for UI or interaction changes. Remove this section if not applicable. -->

## Data, permissions, and release notes

- [ ] No new permission, storage, network, analytics, or tracking behavior.
- [ ] Any new permission or behavior is explained here: <!-- details -->
- [ ] Backup/restore compatibility was considered where data models changed.
- [ ] User-visible release notes are updated when needed.

## Reviewer checklist

- [ ] The implementation follows the nearest `AGENTS.md` contract.
- [ ] I read the diff as a reviewer and removed debug logs or generated files.
- [ ] The change is scoped to the stated problem.
- [ ] New settings or experiments are discoverable and follow the project's toggle rules.
- [ ] No secrets, signing files, or private data are included.
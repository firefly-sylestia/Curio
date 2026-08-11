# Contributing to Curio

Thanks for wanting to help make Curio better! Curio is a small, maintainer-run project, and every issue, idea, and pull request genuinely matters. This guide keeps things simple — here's how to jump in.

## Ways to contribute

- **Report a bug** — found something broken? Tell us exactly what happened.
- **Request a feature or topic** — have an idea for the app, or a topic you'd love to discover?
- **Fix an issue or add content** — pick something up and open a pull request.

## Reporting bugs

Open a [GitHub Issue](https://github.com/firefly-sylestia/Curio/issues) using the **Bug Report** template. The more you include, the faster it gets fixed:

1. **What you did** — steps to reproduce.
2. **What you expected** vs. **what actually happened**.
3. **App version + device** — the version lives at *Profile → Support & diagnostics → Version* (e.g. `1.0.0 · build 20260804`), plus your device model and Android version.
4. **Screenshots or screen recordings** if they help.

You can also file a report straight from the app: *Profile → Support & diagnostics → **Report a bug***, which opens a pre-filled GitHub issue.

## Requesting features or topics

Open a [GitHub Issue](https://github.com/firefly-sylestia/Curio/issues) using the **Feature Request** template. For a topic, name the category and the specific topic you'd like to see — hand-curated descriptions and facts are what make Curio special, so tell us what would make you want to explore it.

## Development setup

Curio is a single-module Android app:

- **Android Studio** (latest stable) with **JDK 17**.
- Open the repo root; the app module is `:app` (Kotlin + Jetpack Compose).
- Dependency versions live in the Gradle version catalog: `gradle/libs.versions.toml`.
- Build locally with `./gradlew assembleDebug` — CI additionally runs lint, topic validation, and `assembleRelease`.

### Topic content

- Topic data lives in `app/src/main/assets/topics/{category}.json` — read `app/CURIO_DATA_PLAN.md` (the data contract) and `app/src/main/assets/topics/SCHEMA.md` before editing.
- Validate any content change with `./gradlew validateTopics` — the CI-authoritative check (IDs must be unique, schema must pass). Topic authoring helpers under `scripts/` are kept on disk locally but are untracked and never shipped in commits.

## Pull requests

1. Fork the repo and create a branch.
2. Keep changes small and focused; follow the surrounding code conventions.
3. Use [conventional commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, …).
4. Fill out the checklist in `.github/PULL_REQUEST_TEMPLATE.md`.
5. Reference the issue you're fixing (`Fixes #123`).

CI runs on every push — lint, topic validation, and both debug and release builds. The release workflow is tag-triggered (`v*`) and requires the maintainer's signing secrets, so don't worry about signing in your PR.

## Code of conduct

Everyone is expected to follow our [Code of Conduct](CODE_OF_CONDUCT.md). Be kind, assume good intent, and help make this a welcoming space.

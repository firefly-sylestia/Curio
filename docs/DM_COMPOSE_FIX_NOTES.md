# DM / Social Composer follow-up

- CI exposed Compose compatibility errors in `DirectMessageScreen.kt`: graphics layer import, pointer consumption, weight usage, and legacy positional `CurioIcon` calls.
- The DM interaction work remains UI-only for replies because `CurioDirectMessage` / `dm_messages` do not expose a reply reference.
- `CommunityPostScreen.kt` is now reachable from the Community destination through the navigation-layer entry wrapper, so the Community post action opens the dedicated full-screen composer rather than the legacy bottom-sheet composer.
- No local Gradle build/lint/test commands were run. GitHub Actions CI is authoritative.

# DM / Social Composer follow-up

- CI exposed Compose compatibility errors in `DirectMessageScreen.kt`: graphics layer import, pointer consumption, weight usage, and legacy positional `CurioIcon` calls.
- The DM interaction work remains UI-only for replies because `CurioDirectMessage` / `dm_messages` do not expose a reply reference.
- `CommunityPostScreen.kt` exists as the new full-screen composer, but the merged `CommunityScreen.kt` still owns the legacy `CommunityComposerSheet` entry point. The new screen therefore needs to replace/wire that entry point before it is reachable from the Community UI.
- No local Gradle build/lint/test commands were run. GitHub Actions CI is authoritative.

# Curio Online Layer (`data/supabase`) — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. Its parent DOX rail is `app/AGENTS.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` ← `app/src/main/java/com/curio/app/data/supabase/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then `app/AGENTS.md`, then this file.

## Purpose

Curio's entire online layer for the Android app: the Supabase REST/Auth client, the stored session, and the observable account state the Online Mode UI reads. Deliberately small and Android-only.

## Ownership

- `SupabaseClient.kt` — REST/Auth calls (sign up, sign in, refresh session, sign out, profile Online Mode PATCH). Hand-rolled OkHttp + `JSONObject`.
- `SupabaseSessionStore.kt` — session persistence (SharedPreferences, rewritten on sign-in, cleared on sign-out).
- `OnlineAccount.kt` — observable `Compose` state plus the sign-in / sign-up / sign-out / Online Mode actions, and `onlineAuthMessage` (transport failure → UI-safe copy).

## Local Contracts

- **No service-role key, ever.** Only public client credentials are read (`BuildConfig.SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` with a `SUPABASE_ANON_KEY` fallback). Never read, store, export, log or ask for a service-role key.
- **Offline-first.** Room is the source of truth for captures; nothing here blocks local use. Online Mode (`AppPreferences.isOnlineModeEnabled`, default OFF) gates every online action, and the local preference wins over the server mirror.
- **Text only.** No capture media (images, audio, screenshots) may be uploaded. A capture that carries media is skipped, never trimmed.
- **UI-safe errors.** Failures surface through `onlineAuthMessage`: rate limits, unconfirmed emails and invalid credentials stay actionable; anything unrecognised collapses to one generic line so a raw response body can never reach the UI.
- `OnlineAccount.restore` is safe to call from any entry point — an in-memory session is never overwritten by the stored one.

## Work Guidance

- Keep the layer thin; do not add a Supabase SDK dependency or a new networking library without asking the user first.
- Screens observe `OnlineAccount.state` instead of holding their own session copies.
- New online surfaces wear the settings design language (see `app/AGENTS.md` → UI) and carry the settings nav rail.

## Verification

- Gradle build/lint runs in CI only (this workspace has no Android SDK): validate with static checks (`git diff --check`, delimiter balance, grep for stale references) and confirm the CI run on the branch.
- CI needs `SUPABASE_URL` + one publishable/anon key secret; `android.yml` logs a warning when they are absent (the app then reports Online Mode as not set up instead of failing).

## Child DOX Index

No child AGENTS.md files defined yet.

# Supabase Backend — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. Its parent DOX rail is the root `AGENTS.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `supabase/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then this file.

## Purpose

The backend artifacts for Curio's online layer: the database schema and its Row Level Security policies. The Android client that consumes them lives in `app/src/main/java/com/curio/app/data/supabase/`.

## Ownership

- `schema.sql` — the whole online schema in one idempotent script: `profiles`, `cloud_captures`, `community_cards`, `community_reactions`, `community_reports`, their RLS policies, indexes, the expiry sweep function and a self-check block.

There is no deployment automation: the file is pasted into the Supabase dashboard's SQL Editor (Database → SQL Editor → Run). Re-running it is safe by design.

## Local Contracts

- **RLS is the security boundary.** The Android app ships only the public URL + publishable/anon key, so every table must keep RLS enabled with policies for `authenticated` only. Never add an `anon` policy, never disable RLS, and never grant the app a service-role key.
- **Text only.** The tables carry no media columns. Capture sync mirrors text metadata and serialized text content; images, audio and screenshots never sync, and community cards are rebuilt on each device with the app's own share-card renderer.
- **24-hour cards.** `community_cards.expires_at` defaults to `now() + interval '24 hours'` and the insert policy refuses anything longer than 25 hours. Every read filters `expires_at > now()`, so expiry holds even before the sweep runs; `curio_purge_expired_cards()` is housekeeping (optionally scheduled with `pg_cron`). Replies (`community_comments`) cascade off the card, so a comment can never outlive its 24-hour card.
- **Captions and replies are text.** A card carries the card's own `fact_text` plus the poster's optional `caption` (≤180 chars), and each reply is ≤400 chars — both enforced by check constraints, both with no media column anywhere in sight.
- **Evolution stays idempotent.** `create table if not exists` never touches an existing table, so columns added later (e.g. `caption`) also appear as `alter table … add column if not exists` plus a guarded constraint block, and the table list in the self-check is updated in the same edit.
- **Identity comes from the token, not the client.** `owner` / `user_id` / `reporter` default to `auth.uid()`, so the app never sends an identity column and cannot attempt to post or react as somebody else.
- **Online Mode gates community access** on both sides: the policies require the acting user's and the card author's `profiles.online_mode_enabled` to be true, and the client mirrors the switch through `SupabaseClient.updateOnlineMode` (an upsert, so a brand-new account gets its row).
- Cards are immutable once posted (there is deliberately no update policy), and only the author may delete one early.

## Work Guidance

- Keep `schema.sql` idempotent: `create … if not exists` plus `drop policy if exists` before each `create policy`, so the file can be re-pasted after an edit.
- Add a table only together with its RLS policies, indexes and grants in the same edit, plus a line in the self-check block at the end.
- Keep the self-check block honest: it prints PASS/FAIL notices for RLS coverage, anon exposure and missing tables after a paste.

## Verification

- Run the file in the SQL Editor and read the `NOTICE`/`WARNING` lines the self-check block raises (every table exists, RLS on, no anon policies).
- Verify from the app: signed out → the feed is locked; Online Mode off → the feed is locked even when signed in (RLS refuses too, by design).
- There is no local Postgres in this workspace, so schema changes are validated by reading the policies and by the dashboard run — never by a local migration tool.

## Child DOX Index

No child AGENTS.md files defined yet.

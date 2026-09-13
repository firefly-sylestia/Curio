# Prompt Log — current request

## Request (2026-09-13, COMPLETE — Supabase realtime setup guide; nothing pushed)

User (chat): write a proper guide for setting up Realtime in Supabase (with
policies etc.), do NOT push it yet, and explain the untracked files that
appeared after they "installed Supabase".

### What was asked and what was done

1. **Guide** → `docs/REALTIME_SETUP.md` (398 lines): what "live" means in Curio
   (the hand-rolled client contract, one `realtime:curio` channel, push-is-a-hint,
   timer fallbacks), the publication SQL (§5e/§5g), replica identity for the
   three filtered tables, a copy-paste verification query with expected output,
   how RLS + the user's JWT filter every change, the 1-hour token rule, the
   optional private-channel path for Broadcast/Presence (requirements, topic
   design, `realtime.messages` policies, the client change it would need), a
   troubleshooting table, an on-device verification checklist, the non-negotiable
   security rules, and a short note on the CLI.
2. **Pointer added** to `supabase/AGENTS.md` (the realtime contract's home) so the
   guide is discoverable from the DOX chain.
3. **Untracked files explained**: `package.json` + `package-lock.json` +
   (git-ignored) `node_modules/` are a root Node install of `@supabase/server`
   (Supabase's server-side SDK, beta) that nothing in Curio uses; `supabase/.temp/`
   is the Supabase CLI's scratch dir (its `cli-latest` says v2.117.0), so the CLI
   ran at least once. It was never linked: `supabase/.temp/project-ref` is empty
   and there is no `supabase/config.toml`, so nothing reached the project.

### State

- NOT committed and NOT pushed (user's explicit instruction): `docs/REALTIME_SETUP.md`
  and the one-line pointer in `supabase/AGENTS.md`.
- The previous request's slice 1 (optimistic likes and instant posting on the
  wall) is already committed AND on `origin/main` as `abebf625`, together with
  the encrypted-DM envelope fix `4373edfc`.
- An old `stash@{0}` (a one-line Prompt.md tweak) is left untouched.

## Interrupted request (2026-09-13, PARTIAL — slice 1 shipped)

User (chat): friends screen (online indicator, delete chat on hold, avatar
cut-out, faster syncing), instant likes/dislikes and posting, faster sending,
revamped "post a topic" and comments sheets, proper swipe refresh with the
refresh button removed, Friends as a sidebar with the current screen becoming
Chats, avatar cut-out and a new Friends icon, share-with-link + share-to-social
from the card editor, an Instagram-style member profile, and realtime reply
notifications.

Shipped (`abebf625`), plus the member-profile redesign in this slice:

- Wall and card-page likes/dislikes are optimistic (`toggleLike`/`toggleDislike`
  in `SocialComponents.kt`), so a tap answers itself; only a rejected call
  resyncs.
- `CommunityApi.post` returns the stored `CommunityCard`, so a new post is on the
  wall the moment the sheet closes.
- `loading` no longer drives `PullToRefreshBox`; a new `refreshing` flag belongs
  to the user's gesture, and the wall's `Refresh` button is gone.
- The replies sheet refreshes quietly instead of rebuilding the wall.
- **Member profile redesigned** (`SocialProfileScreen.kt`): a `LazyVerticalGrid`
  (2 preview columns on a phone, 3 wide) with a full-width Instagram-style
  identity block — 84dp portrait, display name, @handle, three counts (posts /
  likes / replies) computed from the very cards the grid shows, the bio, one
  action pill (Message / Add friend / own-page note), and Block moved behind a
  ⋮ `CurioDropdownMenu`. Each post is a small preview tile: the real share card
  clipped to the tile for a topic card, the words for a note or quote. Presence
  is deliberately NOT drawn on a profile (activity stays inside direct chats).

Still open (needs the user's answers, they are design decisions): the
Friends/Chats restructure, inbox online indicator, delete-chat on long-press,
avatar cut-out, comment sheet redesign, post-a-topic sheet revamp,
share-with-link + share-to-social, member-profile redesign, reply notifications.

## Archive

Previous request logs were replaced at the start of this request. They remain
available in Git history (`git log -p -- Prompt.md`).

## User prompts

### Prompt (2026-09-13) — COMPLETE

Create the Supabase realtime setup guide (with policies), do not push it yet,
and explain the untracked files left by the Supabase install.

### Next prompt (the next instruction goes here — never cleared by an agent)

_No pending prompt._

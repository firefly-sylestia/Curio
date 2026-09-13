# Prompt Log — current request

## Request (2026-09-13, COMPLETE — encrypted send blocked by envelope validator + realtime hardening)

User (chat): encrypted DMs went through several states today — first encryption did
not work, then it worked but messages did not decrypt, and now sending fails with
`curio: envelope conversation does not match participants`. Fix that, and enable
proper realtime.

### Diagnosis and plan

1. The error is raised by exactly one place in `supabase/schema.sql`:
   `curio_validate_dm_envelope()`. It was ADDED in `573a6644` (same commit that
   began wrapping the conversation key for BOTH participants' devices), and it
   computes the canonical conversation from `auth.uid()` and `new.recipient`
   only. A sender's own-device envelope has `recipient = auth.uid()`, so the
   canonical it computes is `me:me` — which can never equal the two-party id
   the client sends. That envelope is REQUIRED by
   `curio_enforce_dm_message_envelopes()`, so every encrypted send dies on it.
2. Fix the validator: accept that case against the canonical conversation named
   by the row (peer recovered from the id, which must be canonical and must name
   an accepted friend of the sender). One shared
   `curio_dm_conversation_of()` replaces the three inline copies of the
   canonical-id fold.
3. Realtime gaps found while reviewing: a `phx_close` frame (the server closing
   the channel — an expired access token) was ignored, leaving `isLinked` true
   while nothing arrived; `phx_error` was treated as permanent; a screen that
   re-declared a different binding set kept hearing the OLD bindings; the access
   token was never refreshed while the app ran, so the channel died an hour in;
   and the conversation's typing indicator/reactions were still polled.
4. Ship: schema + client fixes, docs, changelog. Re-paste `supabase/schema.sql`
   (the envelope fix is server-side; there is no migration tool in this repo).

### Completion

- `curio_dm_conversation_of(a, b)` is the ONE canonical conversation-id
  definition, shared by the envelope validator and both `dm_messages` triggers.
  `curio_dm_conversation_peer(conversation, actor)` recovers the other party of
  a canonical id (or null when it is not canonical / does not name the actor).
- The validator now accepts a sender's own-device envelope when the id is the
  canonical conversation of the sender with its single other party, and that
  party is an accepted friend. Everything else is still refused, device
  registration is still required, and recipient-only envelope reads are
  untouched.
- Realtime: `phx_close` and `phx_error` are both a LOST link (so `isLinked`
  tells the truth and screens fall back to their fast tick); a changed binding
  set re-joins the channel; `OnlineAccount.keepSessionFresh` refreshes the
  session ~5 min before the JWT `exp` (retried, never a sign-out) so screens
  rebuild the channel with a fresh RLS context.
- An open conversation now watches four server-filtered bindings — their
  message (INSERT), my read receipt (UPDATE), their typing row, and their
  reactions — and pushes typing/reactions straight into the thread instead of
  waiting for the next tick. `replica identity full` was added to
  `dm_reactions` (filtered DELETE needs the old row).

## Archive

Previous request logs were replaced at the start of this request. They remain
available in Git history (`git log -p -- Prompt.md`).

## User prompts

### Prompt (2026-09-13) — COMPLETE

Fix the encrypted-DM failure ("curio: envelope conversation does not match
participants") and make realtime actually live.

### Next prompt (the next instruction goes here — never cleared by an agent)

_No pending prompt._

# Prompt Log — current request

## Request (2026-09-13, COMPLETE — DM envelope RLS regression repair)

User (chat): One device now reports a `dm_key_envelopes` row-level-security
violation and delivery intermittently fails on another device. Repair the
regression from the preceding encryption audit without relaxing recipient-only
envelope reads or weakening the cryptography.

### Diagnosis and plan

1. The previous change used PostgREST `on_conflict` on an envelope table where
   the sender intentionally cannot SELECT the recipient's existing envelope.
   Even conflict-ignore can enter PostgREST's upsert/RLS path and reject the
   write before it becomes an innocuous duplicate.
2. Send envelopes with a plain INSERT instead. Treat only a duplicate-key race
   as success; propagate a real RLS/network/validation failure. This retains
   immutable envelopes and recipient-only reads.
3. Keep all existing device/version/canonical-conversation checks, run static
   verification, and commit the focused repair.

### Completion

- The regression was caused by the previous recipient-only read policy being
  combined with a PostgREST `on_conflict` envelope upsert. An ordinary duplicate
  upsert can enter PostgREST's conflict visibility path, which the sender is
  correctly not allowed to pass.
- Envelope writes are now plain immutable INSERTs. A duplicate primary-key race
  is idempotent success; actual RLS, canonical-device validation, and network
  failures still reach the sender instead of being concealed. Recipient-only
  reads and all encryption properties remain unchanged.

## Archive

Previous request logs were replaced at the start of this request. They remain
available in Git history (`git log -p -- Prompt.md`).

## User prompts

### Prompt (2026-09-13) — COMPLETE

Fix the `dm_key_envelopes` RLS regression introduced by the encrypted-DM
audit. Keep recipient-only envelope reads and do not weaken cryptography.

### Next prompt (the next instruction goes here — never cleared by an agent)

_No pending prompt._

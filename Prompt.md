# Prompt Log — current request

## Request (2026-09-13, COMPLETE — end-to-end DM encryption audit)

User (chat): Audit and fix Curio's full encrypted-DM lifecycle without
replacing its AES-GCM + RSA-OAEP architecture. Trace device identity
registration, versioned envelopes, Supabase persistence/RLS, receiver restore,
and exact-version AES-GCM decryption. Add safe diagnostics and deterministic
crypto coverage; do not weaken authentication, AAD, nonce uniqueness, or local
private-key handling.

### Audit plan

1. Trace every identity, key, envelope, message, and receiver-read path in the
   Android app and `supabase/schema.sql`; verify canonical conversation IDs,
   exact key versions, and multi-device fan-out.
2. Correct the smallest root cause(s), especially identity persistence and
   envelope access/validation, while keeping private keys local.
3. Make receiver ordering explicit: obtain all exact-version envelopes,
   install them, classify failures, then decrypt. Add redacted diagnostic logs.
4. Add deterministic crypto tests for valid flows, tampering/wrong inputs,
   version rotation, persistence, and two-device envelopes.
5. Run static checks only (Gradle is prohibited), review the diff, update the
   release note if a user-visible behavior changes, commit, push if the branch
   can be configured for a remote, then open the required PR.

### Completion

- Removed silent device-identity replacement on normal secure-store or RSA-key
  reads. A persisted public-key/device binding detects a real Keystore reset
  and deliberately registers it as a new cryptographic device, while partial
  binding loss fails safely instead of orphaning old envelopes invisibly.
- Made envelopes immutable conflict-ignore writes, recipient-only reads, and
  multi-device fan-out for both participants. The SQL now validates canonical
  participant/device bindings and refuses a ciphertext until every current
  participant device has its exact-version envelope.
- Receiver loading now separates envelope retrieval, missing-envelope, invalid
  RSA-envelope, and AES-GCM authentication errors. Redacted structural logs
  contain versions, hashed identifiers, lengths, lifecycle state, and exception
  class only.
- Added JVM crypto tests for same-device AES-GCM, cross-device RSA unwrap,
  authentication failures, canonical versions, multi-version restores, and
  two-device envelope fan-out. Android Keystore/process-death and live
  Supabase/RLS execution remain CI/dashboard validation because this workspace
  forbids Gradle and has no Supabase project access.

## Archive

Previous request logs were replaced at the start of this request. They remain
available in Git history (`git log -p -- Prompt.md`).

## User prompts

### Prompt (2026-09-13) — COMPLETE

Audit and fix Curio's end-to-end encrypted DM flow, retaining AES-256-GCM with
12-byte unique nonces, canonical conversation-ID AAD, RSA-OAEP-SHA256
per-device envelopes, exact message key versions, local private keys, and safe
diagnostics. Add deterministic lifecycle and tampering tests.

### Next prompt (the next instruction goes here — never cleared by an agent)

_No pending prompt._

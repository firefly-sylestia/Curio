# Prompt.md — Request log

## Current request — COMPLETED: workflow/instruction changes (commit only, no push)

User asked for three durable rules added to the instructions, and that
THIS change be committed but NOT pushed ("dont push this just commit").

### Changes (root `AGENTS.md`)
1. **git pull first** — General Workflow now starts at step 0: run `git
   pull` before the first work of any session, so work always builds on
   the latest remote state.
2. **Ask before deleting/replacing anything** — the durable preference in
   "ASK WHEN UNSURE" now explicitly covers deleting, replacing, or
   overwriting ANY file, data entry, or content (topic JSON entries,
   strings, assets, docs) — not just features/UI/code paths.
3. **Text/docs changes commit but never push alone** — the old "SMALL
   TEXT-ONLY CHANGES — DO NOT PUSH" section became "TEXT-ONLY / DOCS
   CHANGES — COMMIT, BUT PUSH ONLY WITH THE NEXT REAL CHANGE": text/doc
   edits (including AGENTS.md / master.md / Prompt.md) are committed but
   pushed only together with the next functional or user-visible change.
   The DO-COMMIT-AND-PUSH section and General Workflow step 8 now carry
   the same exception pointer.

### Git state
- Committed locally per the user's explicit instruction ("dont push this
  just commit") — NOT pushed. This commit rides along with the next real
  change per the new rule.
- Note: the OLD rule required instruction changes to be pushed immediately;
  the new user preference supersedes it.

### Verification
Docs-only change; no app behavior touched.

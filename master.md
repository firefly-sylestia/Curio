# DOX framework

- DOX is highly performant AGENTS.md hierarchy installed here
- Agent must follow DOX instructions across any edits

## Core Contract

- AGENTS.md files are binding work contracts for their subtrees
- Work products, source materials, instructions, records, assets, and durable docs must stay understandable from the nearest applicable AGENTS.md plus every parent AGENTS.md above it

## Read Before Editing

1. Read the root AGENTS.md
2. Identify every file or folder you expect to touch
3. Walk from the repository root to each target path
4. Read every AGENTS.md found along each route
5. If a parent AGENTS.md lists a child AGENTS.md whose scope contains the path, read that child and continue from there
6. Use the nearest AGENTS.md as the local contract and parent docs for repo-wide rules
7. If docs conflict, the closer doc controls local work details, but no child doc may weaken DOX

Do not rely on memory. Re-read the applicable DOX chain in the current session before editing.

## Update After Editing

Every meaningful change requires a DOX pass before the task is done.

Update the closest owning AGENTS.md when a change affects:

- purpose, scope, ownership, or responsibilities
- durable structure, contracts, workflows, or operating rules
- required inputs, outputs, permissions, constraints, side effects, or artifacts
- user preferences about behavior, communication, process, organization, or quality
- AGENTS.md creation, deletion, move, rename, or index contents

Update parent docs when parent-level structure, ownership, workflow, or child index changes. Update child docs when parent changes alter local rules. Remove stale or contradictory text immediately. Small edits that do not change behavior or contracts may leave docs unchanged, but the DOX pass still must happen.

## Hierarchy

- Root AGENTS.md is the DOX rail: project-wide instructions, global preferences, durable workflow rules, and the top-level Child DOX Index
- Child AGENTS.md files own domain-specific instructions and their own Child DOX Index
- Each parent explains what its direct children cover and what stays owned by the parent
- The closer a doc is to the work, the more specific and practical it must be

## Child Doc Shape

- Create a child AGENTS.md when a folder becomes a durable boundary with its own purpose, rules, responsibilities, workflow, materials, or quality standards
- Work Guidance must reflect the current standards of the project or user instructions; if there are no specific standards or instructions yet, leave it empty
- Verification must reflect an existing check; if no verification framework exists yet, leave it empty and update it when one exists

Default section order:
- Purpose
- Ownership
- Local Contracts
- Work Guidance
- Verification
- Child DOX Index

## Style

- Keep docs concise, current, and operational
- Document stable contracts, not diary entries
- Put broad rules in parent docs and concrete details in child docs
- Prefer direct bullets with explicit names
- Do not duplicate rules across many files unless each scope needs a local version
- Delete stale notes instead of explaining history
- Trim obvious statements, repeated rules, misplaced detail, and warnings for risks that no longer exist

## Closeout

1. Re-check changed paths against the DOX chain
2. Update nearest owning docs and any affected parents or children
3. Refresh every affected Child DOX Index
4. Remove stale or contradictory text
5. Run existing verification when relevant
6. Report any docs intentionally left unchanged and why

## User Preferences

When the user requests a durable behavior change, record it here or in the relevant child AGENTS.md

### ❓ Ask When Unsure

If an agent understands the user's request less than ~80%, it MUST ask for
confirmation before proceeding — never guess and run with the most plausible
reading. Use the ask_user tool to clarify ambiguous, missing, or conflicting
context first. See root `AGENTS.md` "ASK WHEN UNSURE".

### ✅ Commit and Push After Every Change

Agents MUST commit and push all changes before the task is complete. Do not leave changes uncommitted. Follow the commit workflow in the root `AGENTS.md`:

1. **Stage:** `git add -A`
2. **Commit:** `git commit -m "type: concise description"` (conventional format)
3. **Push:** `git push`

This is mandatory — do not skip unless the user explicitly says not to push.

### 📝 Small Text-Only Changes — Do NOT Push

Small text-only changes that do **not** affect app functionality (comment
rewordings, doc tweaks, formatting fixes) must NOT be committed and pushed
on their own — leave them uncommitted so they ride along with the next real
change. This does NOT apply to: changes to agent instructions (AGENTS.md,
master.md) or the Prompt.md request log; changes to user-visible text
(strings, What's New, changelogs); or any change that alters behavior,
layout, or compiled output — those still get pushed. See root
`AGENTS.md` "SMALL TEXT-ONLY CHANGES — DO NOT PUSH".

## DOX Tree Diagram

See [`DOX_TREE.md`](docs/DOX_TREE.md) for a visual Mermaid diagram and ASCII tree of the complete AGENTS.md hierarchy.

## Child DOX Index

- `AGENTS.md` (root) — Project-wide DOX rail: environment rules, workflow, Prompt.md management, and release-note guidance
  - `app/AGENTS.md` — Active Curio Android app module
    - `app/CURIO_DATA_PLAN.md` — Topic taxonomy, schema, and authoring contract
    - `app/src/main/assets/topics/SCHEMA.md` — Topic JSON quick reference
  - `gradle/AGENTS.md` — Version catalog and Gradle wrapper
  - `fastlane/AGENTS.md` — Android store metadata and release notes
  - `.github/AGENTS.md` — GitHub CI/CD and issue templates

### Visualization

A full interactive Mermaid diagram and ASCII tree is available in [`DOX_TREE.md`](docs/DOX_TREE.md).
# Skills Catalog — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. It follows the
root `AGENTS.md` as its parent DOX rail.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `.agents/skills/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then this file before loading any
skill.

## Purpose

The `.agents/skills/` tree holds the reusable instruction sets an agent loads
for a task. This file is the catalog and the rule that makes them useful: **an
agent must load the skill that matches the work before doing the work** — skills
that nobody reads are just files.

## Ownership

- `curio-design/` — **ours**, authored for Curio from the app's own tokens and
  the root AGENTS.md compile-safety rules. Curio's motion + consistency contract.
- Everything else here is a **third-party skill** installed from a public repo
  (`npx skills add <owner/repo> --skill <name>`). Unvetted and unowned: treat
  them as reference material, never as authority over this repo's contracts.
  Root `AGENTS.md` + `master.md` + child AGENTS.md files always win.

## Local Contracts

### Load the skill that matches the work (the rule)

1. **Before starting a task**, read the table under "Skill index" and load every
   skill whose "load when" matches the work. Load it with the `skill` tool by
   name; the contents are read fresh from disk.
2. **Load before editing, not after.** A skill read once the change is written
   can only produce a rewrite.
3. **Matches are cumulative.** A Compose change to a card's entrance animation
   loads `curio-design` + `animation-principles` (and `mobile-android-design` when
   it touches Compose/theme structure). Do not stop at the first match.
4. **More than two skills for one task is a smell.** Prefer the smallest set that
   covers the work; `curio-design` is the default for anything visual in `app/`.
5. **Project contracts outrank skills.** If a skill disagrees with root
   `AGENTS.md`, `master.md`, an `app/AGENTS.md`, or `curio-design`, the repo doc
   wins and the skill is simply not applicable to that call site.
6. **Record it in the plan.** When you load a skill for a task, say so in
   `Prompt.md` (which skill, why) so the next agent can tell what shaped the work.

## Work Guidance

### Skill index

| Skill | Source | Load when |
|---|---|---|
| `curio-design` | ours | **Any** UI work in `app/`: motion, animation, duration, easing, spring, transition, colour, shape, typography, glass/frost, elevation, press feedback, a new surface, or a consistency review. The default for anything visual. |
| `mobile-android-design` | `wshobson/agents` | Building Compose UI or theming that follows Material Design 3 structure — theming, navigation, component anatomy. |
| `motion-system` | `owl-listener/designer-skills` | Defining or extending product-wide motion tokens — durations, easing vocabulary, choreography, reduced-motion handling. |
| `animation-principles` | `owl-listener/designer-skills` | Tuning how ONE animation feels — easing, staging, follow-through. |
| `android-clean-architecture` | `affaan-m/ecc` | Module/layer structure, dependency rules, UseCases, Repositories, data-layer patterns. |
| `kotlin-coroutines-flows` | `affaan-m/ecc` | Writing coroutines or Flow, `StateFlow`, error handling, cancellation/concurrency bugs, or reviewing a hot path. |
| `code-review-and-quality` | `addyosmani/agent-skills` | The review step of any non-trivial change (root AGENTS.md workflow step 6), or when asked to review a diff. |

### Installing, updating, and removing

- Install: `npx skills add <owner/repo> --skill <name> --yes` — **confirm the
  repo and skill with the user first**; community skills are unvetted and run
  with full agent permissions. Preview with `--list`.
- Update: re-run the same install command; review the diff before committing.
- Remove: delete the skill's folder here, drop its row from the table above, and
  say so in the summary. **Ask the user before removing a skill** (root
  `AGENTS.md` "ask before deleting").
- A newly installed skill must get a table row in the same change, or it will
  never be loaded.

### Authoring our own skills

- New skills go in `.agents/skills/<kebab-name>/SKILL.md` with YAML frontmatter
  (`name`, `description`). The description is the trigger text: write it as
  "use when …" so it can be matched against a task.
- Our skills describe **this repo's** contracts and point at the code that owns
  them; they do not restate a whole framework.
- Keep a skill's numbers tied to the file that owns them. If a token changes in
  `CurioMotion.kt`, the skill that names it is stale in the same commit.

## Verification

- No automated check exists for skill content. Verify by matching: every skill
  folder here has a row in the index above, and every row points at a folder that
  exists.
- `ls -1 .agents/skills` against the index table is the whole check — run it
  after any install, removal, or rename.
- Skills are not compiled or tested by CI; they are instructions. The app's CI
  validates the code a skill produces, not the skill.

## Child DOX Index

- `curio-design/SKILL.md` — Curio's own motion + consistency contract
- `mobile-android-design/SKILL.md` (+ `references/`) — third-party, Material 3 / Compose
- `motion-system/SKILL.md` — third-party, motion tokens
- `animation-principles/SKILL.md` — third-party, single-animation craft
- `android-clean-architecture/SKILL.md` — third-party, Android/KMP layering
- `kotlin-coroutines-flows/SKILL.md` — third-party, coroutines + Flow
- `code-review-and-quality/SKILL.md` — third-party, multi-axis code review

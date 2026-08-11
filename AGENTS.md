# Curio Project — Root AGENTS.md (DOX Rail)

## DOX Framework

This file is part of the **DOX framework** defined in `master.md`. All agents MUST follow the DOX hierarchy:

1. **`master.md`** — DOX framework definition (core contract, read/edit workflow, style, closeout)
2. **`AGENTS.md`** (this file) — Project-wide DOX rail: environment rules, workflow, Prompt.md, What's New guidance
3. **Child AGENTS.md files** — Domain-specific contracts for each subtree

**Every agent MUST read `master.md` + the root `AGENTS.md` + the nearest child AGENTS.md along every path they touch before editing.** Do not rely on memory.

## Purpose

Top-level instruction file for all AI agents (Codebuff/Buffy and spawned sub-agents) working on the Curio Android project. Project-wide rules, global preferences, and the top-level Child DOX Index.

## ❓ ASK WHEN UNSURE

If you understand the user's request less than ~80%, **ask for confirmation
before doing anything**. Do not guess, do not assume, do not pick the most
plausible interpretation and run with it. A wrong guess wastes a full cycle
(edit → review → commit → push → CI → revert) and can ship an unwanted
change.

**Durable user preference:** Before removing an existing feature, behavior,
UI element, or code path, ask the user for confirmation first. Refinements
may change implementation details only when the existing user-visible
behavior is preserved; if removal is part of the proposed fix, pause and ask. When in doubt, use the ask_user tool to clarify the request, and
only proceed once the user confirms.

This rule covers ambiguous phrasing, missing context, conflicting
instructions, and any request where multiple readings would lead to
different implementations. Spawned sub-agents don't have the ask_user tool
— when they hit this uncertainty they must report it back to the parent
agent, who asks the user.

## Critical Environment Rules

### ❌ NEVER RUN COMPILE OR BUILD COMMANDS

**Do not run any Gradle compile, build, assemble, or lint commands in this environment.** This includes but is not limited to:

- `./gradlew assemble*`
- `./gradlew compile*`
- `./gradlew build`
- `./gradlew lint`
- `./gradlew ksp*`
- `./gradlew ktlint*`
- `./gradlew test`
- `./gradlew check`

**Reason:** The development environment (IDX/workspace) does not have the full Android SDK, NDK, or build tools configured. Running these commands will fail. All compilation and build validation is handled by CI (GitHub Actions) on push.

### 🛡️ COMPILE-SAFETY RULES (read before ANY edit)

These rules were derived from actual CI compilation failures. Every error was avoidable. Follow these rules to prevent repeating them.

1. **READ BEFORE WRITING** — Before constructing any entity, ViewModel,
   settings, or data class constructor call, **read the actual data class
definition file**. Do not assume parameter names from memory.

2. **CHECK COMPOSE BOM** — Before using a Material3 API, check
   `gradle/libs.versions.toml` for the Compose BOM version. Cross-reference
   with the Material3 changelog to confirm the API exists in that version.
   (E.g. `Card(onClick=…)` requires Material3 1.2+, `tonalElevation`
   requires a later version.)

3. **NON-COMPOSABLE LAMBDAS** — `BackHandler`, `onClick`, `onValueChange`,
   `onCheckedChange`, `LaunchedEffect` key lambdas, and any
   `callback: () -> Unit` are **NOT** @Composable contexts. Do not call
   `remember`, `mutableStateOf`, `LocalFoo.current`, or any @Composable
   function inside them. Extract those calls to the enclosing @Composable
   scope.

4. **NO SED FOR KOTLIN** — Never use `sed -i` to insert multiline Kotlin code.
   Always use `str_replace` with exact old/new string matching. If you must
   use a terminal command for insertion, verify the output afterward.

5. **IMPORTS** — When removing an import, verify **all references** to the
   type are also removed/updated. `CardColors`, `CardElevation`,
   `RoundedCornerShape`, `Shape`, and similar Material3 types are often used
   in function signatures — removing their imports while they're still
   referenced causes compile failures.

6. **MODIFIER ORDER** — When stacking interaction modifiers, order matters:
   press-detection modifiers (`expressiveCardPress`, `pointerInput`) come
   **before** click-consumption modifiers (`clickable`, `combinedClickable`).
   The first modifier in the chain has priority for pointer events.

7. **CANVAS PARAMETER NAMES** — Never name a parameter `size` in a function
   that contains a `Canvas {}` block. Use `iconSize`, `imageSize`,
   `tileSize`, etc. to avoid shadowing `DrawScope.size`.

8. **COMPOSABLE IS A FUNCTION** — `@Composable` only applies to functions,
   never to property getters. Use `@Composable fun foo(): Type` not
   `val foo: Type @Composable get()`.

9. **VERIFY ONE-CYCLE** — After pushing a CI fix, wait for the CI result.
   If the CI log shows NEW errors in files you didn't touch, the previous
   fix may have been incomplete. Do not assume a commit is final until CI
   passes.

10. **TEST SMOKE** — For entity/data-layer changes, the
    `DevFullAppTestRunner` in Developer Settings can verify constructors,
    settings toggles, and database operations without a full Gradle build.

### ✅ DO COMMIT AND PUSH AFTER EVERY FIX

After **every completed fix or change**, agents MUST commit and push before
ending the task — including Kotlin fixes, documentation updates, and changes
to agent instructions. Do not leave a completed fix uncommitted or wait for
another request to ask for the commit.

Use this git workflow:

1. **Stage changes:** `git add -A` (or specific files)
2. **Commit with descriptive message:** `git commit -m "type: concise description of changes"`
3. **Push:** `git push`

Follow conventional commit format: `feat:`, `fix:`, `refactor:`, `docs:`, `style:`, `chore:`, etc.

### 📝 SMALL TEXT-ONLY CHANGES — DO NOT PUSH

Small text-only changes that do **not** affect app functionality — comment
rewordings, doc tweaks, formatting fixes, dead-comment cleanups — must
**NOT** be committed and pushed on their own. They add noise to git history
and trigger a CI build for zero behavior change. Leave them uncommitted in
the working tree so they ride along with the next real change (or get
dropped). This does NOT apply to:

- Changes to agent instructions (AGENTS.md files, master.md) or the
  Prompt.md request log — those MUST be committed and pushed so every
  agent sees them.
- Changes to user-visible text (strings, What's New, changelogs).
- Any change that alters behavior, layout, or compiled output.

### 🆕 NEW FEATURES — ASK THE USER: TOGGLEABLE OR NOT?

Whenever an agent is ADDING A NEW MEASURE — a new feature, capability, or
behavior the app didn't have before — ask the user whether they want it
**toggleable** (behind a user-facing Settings option) or **always-on**.
Use the ask_user tool BEFORE implementing and follow their answer. This
ask does NOT apply to refinements or fixes of existing behavior — those
ship as-is without the toggleable question.

**Reminder — the toggle is NOT permanent.** Once a toggleable feature is
decided/settled (the experiment concludes, the winning path is clear),
REMOVE the toggle and hardcode the winning behavior — see rule 3 of the
🧪 EXPERIMENTAL CHANGES section below. A toggle decided at ask-time is a
ship vehicle, not a permanent Settings fixture.

### 🧪 EXPERIMENTAL CHANGES — MUST BE SETTINGS-OPTIONAL

Whenever a change is **experimental or being tested** (a visual A/B, a new
rendering/animation strategy, a provisional behavior, a tuning experiment),
do NOT hardcode it as the only behavior. Gate it behind a **user-facing
settings option** (a toggle in the app's Settings screen) so it can be
A/B-compared against the current behavior and reverted without a code change.

Rules:

1. Experiments ship as an **opt-in settings toggle**, never as a silent
   behavior swap.
2. The toggle must be **discoverable in the app's Settings screen**, not a
   hidden flag.
3. When the experiment concludes, **remove the toggle** and hardcode the
   winning path.

Note: settings-gating is about *how* an experiment ships, not *whether* to
commit it — the **DO COMMIT AND PUSH AFTER EVERY FIX** rule above still
applies to settings-gated experiments.

## Prompt.md — Research & Analysis Tracking

`Prompt.md` at project root is the running log of the current request. See `Prompt.md` itself for its own rules. Agents must update Prompt.md when:
- Starting a new request (replace entirely with fresh analysis)
- A request is interrupted or half-done (capture progress, remaining work, decisions)
- A request is completed (add completion summary)

## General Workflow

1. **Read DOX chain** — `master.md` → `AGENTS.md` → child AGENTS.md along every path you touch
2. **Read Prompt.md** — check for existing context or half-finished work
3. **Gather context** — read relevant files, search codebase, research APIs before making changes
4. **Plan** — write analysis and plan to Prompt.md, then update todos
5. **Implement** — make targeted, minimal changes
6. **Review** — spawn code-reviewer-deepseek-flash for non-trivial changes
7. **DOX pass** — update nearest owning AGENTS.md if change affects purpose, ownership, contracts, workflows, or structure (see `master.md` "Update After Editing")
8. **Commit & push** — stage, commit with descriptive message, push (skip
   for small text-only changes that don't affect app functionality — see
   "SMALL TEXT-ONLY CHANGES — DO NOT PUSH" above)
9. **Update Prompt.md** — with completion summary and any follow-up notes

## Updating "What's New" (In-App Changelog)

Whenever you make significant changes, you MUST update the "What's New" section inside the app:

### What to Update

1. **In-App Changelog** — only when the active `app/` module has a changelog screen. The Curio app has no changelog screen yet. When a changelog screen exists, add a new entry at the top of its list following the existing entry structure and style.

2. **Fastlane Store Changelog** — `fastlane/metadata/android/en-US/changelogs/{versionCode}.txt`
   - See `fastlane/AGENTS.md` for store conventions (≤500 chars, versionCode naming)

### What NOT to Update

Do not create historical design/status documents for routine changes. Keep durable guidance in the active DOX files and Curio data contract.

### Version Consistency

- In-app version string should match current app version context
- Store changelogs use `versionCode` (integer) — see `fastlane/AGENTS.md`
- In-app changelog: detailed (unlimited). Store changelog: brief (≤500 chars)

## Web App (web/)

The `web/` directory contains a standalone React + TypeScript web application that mirrors the Android app's UI and functionality. It is a separate project from the Android app and is NOT included in the Android build.

**Tech stack:**
- React 18 with TypeScript
- Vite for build tooling
- Tailwind CSS for styling
- IndexedDB for local storage (mirrors Room database)
- React Router for navigation

**Key features:**
- Full UI parity with Android app (Home, Spin, Cabinet, Profile, Settings)
- 21 categories with matching colors and themes
- Theme system (Curio/AMOLED/Material styles)
- Local data persistence via IndexedDB
- No authentication required

**To run the web app:**
```bash
cd web
npm install
npm run dev
```

## Child DOX Index

- [app/AGENTS.md](app/AGENTS.md) — Active Curio Android app module
- [app/CURIO_DATA_PLAN.md](app/CURIO_DATA_PLAN.md) — Curio topic data contract
- [gradle/AGENTS.md](gradle/AGENTS.md) — Gradle version catalog and wrapper
- [fastlane/AGENTS.md](fastlane/AGENTS.md) — Android store metadata and release notes
- [.github/AGENTS.md](.github/AGENTS.md) — GitHub CI/CD and issue templates

# Curio Project — Root AGENTS.md (DOX Rail)

## DOX Framework

This file is part of the **DOX framework** defined in `master.md`. All agents MUST follow the DOX hierarchy:

1. **`master.md`** — DOX framework definition (core contract, read/edit workflow, style, closeout)
2. **`AGENTS.md`** (this file) — Project-wide DOX rail: environment rules, workflow, Prompt.md, What's New guidance
3. **Child AGENTS.md files** — Domain-specific contracts for each subtree

**Every agent MUST read `master.md` + the root `AGENTS.md` + the nearest child AGENTS.md along every path they touch before editing.** Do not rely on memory.

## Purpose

Top-level instruction file for all AI agents (Codebuff/Buffy and spawned sub-agents) working on the Curio Android project. Project-wide rules, global preferences, and the top-level Child DOX Index.

**⚠️ SCOPE: This project's active workstream is the Android app (`app/`) plus
the account site (`auth-web/`, which is live infrastructure for that app's
accounts, not a port) and now the Windows/iOS client (`curio_flutter/`, Windows
first). `web/` is a separate project on hold — do not touch it unless the user
explicitly asks (see the 🔒 Scope section below).**

## ❓ ASK WHEN UNSURE

If you understand the user's request less than ~80%, **ask for confirmation
before doing anything**. Do not guess, do not assume, do not pick the most
plausible interpretation and run with it. A wrong guess wastes a full cycle
(edit → review → commit → push → CI → revert) and can ship an unwanted
change.

**Durable user preference — always ask before DELETING or REPLACING
anything:** Before removing an existing feature, behavior, UI element, or
code path — and before deleting, replacing, or overwriting ANY file, data
entry, or content (topic JSON entries, strings, assets, docs) — ask the
user for confirmation first. Refinements may change implementation
details only when the existing user-visible behavior is preserved; if
removal or replacement is part of the proposed fix, pause and ask. When
in doubt, use the ask_user tool to clarify the request, and only proceed
once the user confirms.

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

### 👀 NEVER WAIT ON A CI RUN — ASK WHETHER IT IS GOING, AND CARRY ON (user directive, 2026-09-21)

**Do NOT sit and watch a CI run.** While a run is `in_progress`, keep working:
answer the member, do the next item on the plan, start the next fix. Watching a
build burns the session and delivers nothing.

Check a run only to make a DECISION, and only with a single quick call
(`gh run list --limit 3`):

1. **The run is `in_progress`** → say so only if it is what was asked, and carry
   on with the work. Do not poll it again in the same task.
2. **The run has `failed`** → read its errors (`gh run view <id> --log-failed`),
   fix them, PUSH the fix, and then answer the member. The one moment a run must
   be looked at is just before a push, so a red build never becomes the pushed
   state twice in a row.
3. **The run is `success`** → nothing to do; keep working.

Answering the member NEVER waits on CI. If a fix was just pushed and the run is
still going, say what was pushed and what it addresses — the result is the next
session's business, or this session's if the member asks.

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

9. **VERIFY ONE-CYCLE** — a pushed CI fix is checked ONCE, not watched
   (see "NEVER WAIT ON A CI RUN" above): look at the run when a decision
   needs it — before the next push — and read the FAILED log for errors in
   files you did not touch, because a previous fix may have been
   incomplete. Never idle on a run that is still going.

10. **TEST SMOKE** — For entity/data-layer changes, the
    `DevFullAppTestRunner` in Developer Settings can verify constructors,
    settings toggles, and database operations without a full Gradle build.

11. **SHADOW ORDER + OPAQUE FILLS** — `Modifier.shadow()` must come BEFORE
    the fill in the chain (`.shadow(e).clip(shape).background(color)`), never
    after — a shadow placed after the background paints a dark blur ON TOP of
    the fill ("blurry broken background"). `shadowElevation` on a Surface only
    renders cleanly when the fill is OPAQUE: translucent/glass fills (alpha <
    1) let the shadow bleed through, so use an opaque `lerp(fill, accent,
    alpha)` blend instead of `color.copy(alpha = …)`. Never add elevation to
    ANIMATING deck cards — v24 rejected deck shadows ("weird look while the
   cards animate"); the v27n elevation pass silently re-added a 2dp halo and
   it regressed into a boxy artifact during the reel.

12. **RE-READ THE SEAMS AFTER A REMOVAL** — when a `str_replace` deletes lines
    and its `oldString` starts on a line of its own, a `newString` that does
    not end in a newline pulls the NEXT thing up into the line above it. A
    removal of three dependency lines in `app/build.gradle.kts` left
    `implementation(libs.com.alphacephei.vosk.android)` inside the Vosk
    COMMENT — the dependency was gone from the build with its text still on
    the page, so no grep for it would ever have noticed (v458). After any
    deletion, read the five lines either side of the cut, and `grep -n` the
    block for anything that has lost its own line.

13. **AN INSERTION TAKES THE ANNOTATION ABOVE IT** — when a new declaration is
    inserted *before* an existing function, the `@Composable` (or `@OptIn`, or
    any other) annotation that sat directly above that function now binds to
    the NEW declaration: **an annotation binds to the next DECLARATION, and a
    doc comment in between is not a barrier.** The function left below loses its
    annotation, and if the insertion carries an annotation of its own, the same
    one appears twice — the build then says `This annotation is not repeatable`
    and `@Composable invocations can only happen from the context of a
    @Composable function`, neither of which points at the real mistake. So:
    **read the two lines above any insertion point**, keep the annotation with
    the function it belongs to (re-add it explicitly when the insertion moved
    past it), and make any checker treat *the same annotation twice in one run*
    as a failure — a scan that accepts "annotation, comment, annotation" (two
    DIFFERENT annotations may stack) hides this bug exactly.

    **The shape when the stolen annotation lands on a PROPERTY**, which is what
    a `private val` insertion directly under it produces, reports as: `This
    annotation is not applicable to target 'top level property with backing
    field'` — at the FIRST inserted declaration — followed by thirty-odd
    `@Composable invocations can only happen from the context of a @Composable
    function` errors INSIDE the function that lost it. Neither line names the
    insertion. The checker must therefore flag THREE shapes: the same annotation
    twice in one run; an annotation whose next declaration is a
    `val`/`var`/`const val` (a *local* `@Suppress` on a local `val`, and a local
    `@Composable fun`, are legal and must not be flagged); and — v467 — **a doc
    comment IMMEDIATELY followed by an annotation IMMEDIATELY followed by
    ANOTHER doc comment**.

    **The third shape is the quietest one and it is worth knowing why.** When an
    insertion lands *between* a declaration's doc comment and its annotation, the
    annotation is captured by the inserted function and the ORIGINAL function is
    left with no annotation at all — so nothing is duplicated and the
    "appears twice" check cannot see it. **The tell is structural: two doc blocks
    in a row with an annotation between them means that annotation has been
    displaced from the function the FIRST block describes.** It compiles more
    often than the other two shapes (the annotation may be redundant where it
    lands, or the function that lost it may not need it), which is exactly why it
    survives for months. v461's `JournalGridCell` did this to `JournalRow`'s
    `@OptIn(ExperimentalFoundationApi::class)` in `JournalListScreen.kt`; it was
    found in v467 only because the cell was being deleted and the strand was
    visible at the seam.

    Five of these landed in one session (v458's snapped `@Composable`, v461's
    `SearchSlide` and the v460 gradient helper, v462's `JournalGridCell` and
    `AdvancedSection`, and v462's `ReaderDictionaryPage` — the last one AFTER
    this rule was written, which is the point: **the rule only works if the
    scan runs after the edit**, on the whole tree, not on the file you were
    reading). It is the most expensive habit in this codebase.

    **⚠️ AND THE PROOF OF THAT IS `JournalGridCell` ITSELF: this rule named it in
    v462 and the damage was STILL IN THE TREE until v467**, when the cell's
    deletion exposed the displaced `@OptIn` and `JournalRow`'s stranded doc
    comment. A named instance is not a fixed one. **There is no checker script in
    this repository** (`.github/scripts/` holds the CI readers only) despite this
    rule requiring one — so the scan is done by hand or not at all, and the v467
    run of it found the tree otherwise clean for all three shapes.

    **The habit that prevents all of it:** before writing an insertion whose
    anchor is a declaration, `grep -n -B2` that anchor — if either line above
    is an annotation, put the annotation back on the anchor inside the SAME
    `str_replace`, or anchor on something above the annotation instead.

### ✅ DO COMMIT AND PUSH AFTER EVERY FIX

After **every completed fix or change**, agents MUST commit and push before
ending the task — including Kotlin fixes, documentation updates, and changes
to agent instructions. Do not leave a completed fix uncommitted or wait for
another request to ask for the commit.

**Exception — text/docs-only changes are committed but NOT pushed on their
own:** a change that is purely text or documentation (including edits to
agent instructions / AGENTS.md / master.md / Prompt.md) is committed but
pushed only together with the next real change — see "📝 TEXT-ONLY / DOCS
CHANGES" below.

Use this git workflow:

1. **Stage changes:** `git add -A` (or specific files)
2. **Commit with descriptive message:** `git commit -m "type: concise description of changes"`
3. **Push:** `git push`

Follow conventional commit format: `feat:`, `fix:`, `refactor:`, `docs:`, `style:`, `chore:`, etc.

**NEVER append tool/generated-by footers to commit messages** — no
"Generated with Codebuff", no `Co-Authored-By: Codebuff` (or any other
AI/tool attribution) lines. Commit messages are plain conventional-format
text only.

### 🔢 EVERY PUSH BUMPS THE VERSION (user directive, 2026-09-24)

**Every `git push` carries a version bump.** In `app/build.gradle.kts`:

- `versionName` **+0.0.1** — `1.4.0` → `1.4.1`
- `versionCode` **+1** — `20260923` → `20260924` (it is date-based today; a
  decimal cannot go into an integer, so `0.0.1` means *the next one*)

and because the store changelog is NAMED after the `versionCode`, the bump also
creates the notes file for the new code:
**`fastlane/metadata/android/en-US/changelogs/{newVersionCode}.txt`**, carried
over from the previous file with this push's changes in it.

⚠️ **THE OLD FILE IS NOT RENAMED AND NOT DELETED.** `{versionCode}.txt` is the
record of the build that code shipped as, so the previous file stays exactly as
it was — the notes for the new code are a COPY that then moves forward. Renaming
it would take the shipped release's notes away from the release that owns them
(see `fastlane/AGENTS.md`).

Do this in the same push, as part of it — a `chore(release)` step, not a
separate request. A push without the bump is a push that overwrites the previous
build's identity.

### 📝 TEXT-ONLY / DOCS CHANGES — COMMIT, BUT PUSH ONLY WITH THE NEXT REAL CHANGE

Text-only and documentation changes — comment rewordings, doc tweaks,
formatting fixes, dead-comment cleanups, and edits to agent instructions
(AGENTS.md files, master.md) or the Prompt.md request log — are always
COMMITTED (they stay in git history), but are **never pushed on their
own**. They ride along with the next real change (a commit that alters
behavior, layout, or compiled output) or a user-visible change (strings,
What's New, changelogs — which ship as part of the feature commit they
describe). A docs-only push triggers a CI build for zero behavior change
and leaves the repo ahead of origin with nothing shipped. When a task
produces ONLY text/doc edits: commit them, and leave the push until a
functional change lands (or the user explicitly asks to push).

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

## CI Discipline (short form)

One quick `gh run list` per decision; never a wait loop. Running → keep working.
Failed → fix, push, then answer. Answering the member never waits on CI.

## Prompt.md — Research & Analysis Tracking

`Prompt.md` at project root is the running log of the current request. See `Prompt.md` itself for its own rules. Agents must update Prompt.md when:
- Starting a new request (replace entirely with fresh analysis)
- A request is interrupted or half-done (capture progress, remaining work, decisions)
- A request is completed (add completion summary)

### 📥 CHECK-AFTER-EVERY-PUSH CONTRACT (user directive, 2026-09-07)

The LAST section of Prompt.md — the "User prompts" section at the END of
Prompt.md — is where the user drops new instructions. It is NEVER cleared
(see Prompt.md for its own layout: the pending prompt + its status at the
top of that section, then an empty slot for the next prompt).

1. **After EVERY push, read that section before ending the task.** If a
   pending prompt is present, follow it properly (you may rephrase it in
   Prompt.md for clarity — never silently drop parts). When the prompt is
   done, update its status and move it into the request log above.
2. **No pending prompt = task complete.** If the section is empty, the
   push closes the task — do not invent follow-up work.
3. **Per-change discipline** (every prompt, and every visual / big-logic /
   UX change): do thorough research, run a quality check, and make a
   proper plan BEFORE implementing. Review each change for what the user
   may have missed but is necessary.
4. **Design rules**: never add useless hint texts; keep everything premium
   and minimal; stay design-consistent with the surface you touch; make
   new surfaces liquid-glass-ready; exceed expectations with proper,
   beautiful animations and clean close interactions.
5. **Research + confirm**: do your own research; when you find something
   the user missed and think should be added, ASK for confirmation before
   adding it (ask_user).
6. **Logic/performance review**: check persistence, state, and hot paths —
   never make the app slow.

### How to update this contract
If the user refines this workflow, update this section AND the Prompt.md
"User prompts" section's own rules together — they are one contract.

## General Workflow

0. **git pull FIRST** — run `git pull` before starting any work in a
   session, before the first edit. Always sync with origin first so you
   build on the latest remote state (other agents/sessions may have
   pushed while you were away). Never start editing on a stale checkout.
1. **Read DOX chain** — `master.md` → `AGENTS.md` → child AGENTS.md along every path you touch
2. **Read Prompt.md** — check for existing context or half-finished work
3. **Gather context** — load the skill that matches the work (read the index in [`
   .agents/skills/AGENTS.md`](.agents/skills/AGENTS.md) and load every match
   BEFORE editing), then read relevant files, search the codebase and research
   APIs before making changes
4. **Plan** — write analysis and plan to Prompt.md, then update todos
5. **Implement** — make targeted, minimal changes
6. **Review** — spawn code-reviewer-deepseek-flash for non-trivial changes
7. **DOX pass** — update nearest owning AGENTS.md if change affects purpose, ownership, contracts, workflows, or structure (see `master.md` "Update After Editing")
8. **Commit & push** — stage, commit with descriptive message, push (for
   text/docs-only changes: commit but push only with the next real change
   — see "TEXT-ONLY / DOCS CHANGES — COMMIT, BUT PUSH ONLY WITH THE NEXT
   REAL CHANGE" above)
9. **Update Prompt.md** — with completion summary and any follow-up notes

## Updating "What's New" (Release Notes)

**The release notes are updated on EVERY commit that ships a user-visible change** — not just significant ones. Same discipline as Prompt.md: the log moves with the code. Keep the notes short and scannable; never write prose paragraphs.

### What to Update

1. **In-App Changelog** — only when the active `app/` module has a changelog screen. The Curio app has no changelog screen yet. When a changelog screen exists, add a new entry at the top of its list following the existing entry structure and style.

2. **Fastlane Store Changelog** — `fastlane/metadata/android/en-US/changelogs/{versionCode}.txt`
   - See `fastlane/AGENTS.md` for the format contract (concise `ADD` / `FIX` / `REMOVE` bullets, per-commit updates, removal rules).
   - Edit the CURRENT `{versionCode}.txt` in place as changes land; only create a new file when the versionCode bumps.

### Release-Note Format

- Group bullets under `ADD` / `FIX` / `REMOVE` headers.
- One short line per change — feature name first, no fluff: "Cabinet: search, sort and category filter chips."
- **REMOVE is for shipped features only.** If a feature is removed before it ever reached a pushed release, do NOT add a REMOVE note — it never existed for users.

### What NOT to Update

Do not create historical design/status documents for routine changes. Keep durable guidance in the active DOX files and Curio data contract.

### Version Consistency

- In-app version string should match current app version context
- Store changelogs use `versionCode` (integer) — see `fastlane/AGENTS.md`
- In-app changelog: detailed (unlimited). Store changelog: brief (≤500 chars)

## 🗺 Plans and idea backlogs — `docs/plans/`

Durable multi-step feature plans and researched idea backlogs live under
[`docs/plans/`](docs/plans/). Currently:

- [`curio-idea-agenda.md`](docs/plans/curio-idea-agenda.md) — the researched
  feature backlog: the evidence it is built on (why engagement only helps
  learning when it drives retrieval), every idea with the interaction it should
  have and the code it hooks into, the decisions taken, and the build order.
  **Read it before proposing or starting a new feature**, and update it when an
  idea is built or dropped. Once built, the binding description belongs in the
  owning `AGENTS.md`, not in the agenda.

## 🔒 Scope — Android app + the Windows/iOS client (web/ on hold)

**Do NOT edit, build, or touch anything under `web/` unless the user explicitly
asks for it in the current request.** The active workstreams are the **Android
app** (`app/`) and the **Windows/iOS client** (`curio_flutter/`) — two separate
apps in two languages, which share content and lane declarations and nothing
else.

- The web (React/TS) port is a **separate project on hold** — do not treat it as
  part of an Android or Flutter task, do not "keep it in sync" with either, and
  do not apply Android fixes/features to it.
- **`curio_flutter/` is its own app.** Do not "port" an Android widget into it
  line by line, and do not apply a Flutter change to the Android app. What the
  two DO share: `data/topics/*.json` (the one catalog), and the lane slugs,
  labels and accents that `curio_flutter/tool/sync_lanes.py` generates from the
  Kotlin catalog.
- This includes data mirrors: Android data fixes (e.g. topic JSON dedupes)
  apply to `data/topics/` (the shared source) — do not touch `web/src/data/topics/`
  unless the user says so.
- If a request is ambiguous about scope, default to the Android app and note the
  `curio_flutter/` and web impact in your summary so the user can opt in.

## Windows + iOS App (`curio_flutter/`) — ACTIVE, Windows build first

The desktop app is **no longer the Compose Multiplatform `desktop/` module** —
that module was deleted on the member's instruction (2026-11-03) and replaced by
a **separate Flutter/Dart client in `curio_flutter/`**, built for **Windows** and,
next, for **iOS** from the same code. It is a NEW app, not a port: it shares the
content and the lane declarations, never the Kotlin.

**The member's decisions (asked and answered, 2026-11-03):** stack **Flutter
(Dart)**; the old `desktop/` module **replaced/deleted**; **Windows .exe first**;
iOS **26+ only** — the native Liquid Glass is used, with no fallback below it.

- **Layout:** a Windows window gets its own frame — a rail of four rooms
  (Home · Spin · Cabinet · Settings), a context column (lanes / saved /
  device), and the page — decided in `curio_flutter/lib/shell/app_shell.dart`.
  The iOS shell keeps the same state and swaps this frame for a tab bar and one
  column, the way Android looks, with native glass surfaces.
- **Content is shared, never copied into git here:** `tool/sync_assets.sh`
  bundles the repo's `data/topics/*.json` into `assets/topics/`, exactly as the
  Android CI does, and `tool/sync_lanes.py` regenerates
  `lib/data/lanes.dart` (slug, label, accent) from `Category.kt` and
  `CurioColors.kt`. A lane is still declared ONCE, in Kotlin.
- **State:** two plain JSON files (`prefs.json`, `cabinet.json`) under
  `%APPDATA%\Curio` on Windows (`~/.curio` elsewhere), written with `dart:io`.
  No database, no account, nothing leaves the machine.
- **CI:** `.github/workflows/curio-flutter.yml` (windows-latest) generates the
  Windows runner, bundles the catalog, analyses and builds the app. The
  development environment has **no Flutter SDK**, so CI is the only compile.
  `windows/` and `ios/` are generated by `flutter create` and are `.gitignore`d
  until a runner has been produced and committed.
- **No Gradle involvement.** `settings.gradle.kts` no longer includes `:desktop`,
  and `android.yml` no longer has a desktop job. Deleting `:desktop` did NOT
  touch the Android build's dependencies.

**iOS, when it starts:** same project, `flutter create --platforms=ios .`, then a
Swift platform view wrapping iOS 26 `UIGlassEffect` for the glass surfaces — the
one place Swift has to exist, because "native Liquid Glass" cannot be faked in
Dart. No iOS job exists yet.


## Web App (web/) — ⛔ ON HOLD (do not touch unless asked)

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

## Curio Account Web (auth-web/) — ACTIVE (account infrastructure)

The `auth-web/` directory is **the web side of Curio's online accounts**: the
pages Supabase email links land on (email confirmation, sign-in link, password
reset) plus a small account desk that can create an account with a password,
sign in, review the account and delete it. It exists because a Supabase email link has to open somewhere, and without a
real destination the project's Site URL answers instead, whose default is
`http://localhost:3000`.

**Key facts:**- **A static site with two serverless functions and no build step.** Plain
  HTML/CSS/JS, no framework, no npm dependencies: `index.html`, one folder per
  page (`confirm/`, `link/`, `reset/`, `signup/`, `signin/`, `account/`,
  `support/`, `privacy/`, `terms/`), `assets/theme.css`, `assets/curio.js`,
  `api/config.js`, `api/delete-account.js`. Nothing here participates in the
  Android, desktop or `web/` builds.
- **The one secret is server-side.** `/api/config` serves only the public project
  URL + anon key (the same pair the APK carries) from Vercel environment
  variables; `/api/delete-account` is the only holder of the service-role key and
  it verifies the caller's own access token against GoTrue before deleting the id
  that call returns. A client-supplied user id is never honoured.
- **Vercel:** Root Directory `auth-web`, no build command, no output directory.
  Environment variables and the Supabase dashboard checklist (Site URL, Redirect
  URLs, `{{ .RedirectTo }}` email templates) are in `auth-web/README.md`.
- **The app points at it through `BuildConfig.CURIO_AUTH_SITE_URL`** (see
  `app/build.gradle.kts`): an empty value sends no `redirect_to` and hides the
  sign-in form's password-recovery row, so a build without the site behaves
  exactly as before. Set the `CURIO_AUTH_SITE_URL` repo secret to switch it on.
- **Read [`auth-web/AGENTS.md`](auth-web/AGENTS.md) before editing it.**

## Child DOX Index

- [app/AGENTS.md](app/AGENTS.md) — Active Curio Android app module
- [auth-web/AGENTS.md](auth-web/AGENTS.md) — Curio account web (Supabase email links, password reset, account desk)
- [app/APP_AUDIT.md](app/APP_AUDIT.md) — App audit: fixed defects, Lite-mode gating rules, and the deliberately separate UNVERIFIED leads
- [app/CURIO_DATA_PLAN.md](app/CURIO_DATA_PLAN.md) — Curio topic data contract
- [.agents/skills/AGENTS.md](.agents/skills/AGENTS.md) — Skills catalog + the rule to load the matching skill before editing
- [gradle/AGENTS.md](gradle/AGENTS.md) — Gradle version catalog and wrapper
- [supabase/AGENTS.md](supabase/AGENTS.md) — Online backend schema + RLS (`schema.sql`, pasted into the Supabase dashboard; RLS is the security boundary)
- [fastlane/AGENTS.md](fastlane/AGENTS.md) — Android store metadata and release notes
- [fdroid/AGENTS.md](fdroid/AGENTS.md) — F-Droid listing material (fdroiddata metadata draft + inclusion checklist; not part of the app build)
- [curio_flutter/AGENTS.md](curio_flutter/AGENTS.md) — the Windows/iOS client
  (Flutter/Dart; see the Windows + iOS section above)
- [.github/AGENTS.md](.github/AGENTS.md) — GitHub CI/CD and issue templates

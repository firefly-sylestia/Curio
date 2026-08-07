# Prompt.md — Current Request Log

## Request (2026-08-07): Create `Alpha` branch with reveal action-dock work and open a PR to `main`

**User request:** "create a new branch from this names Alpha and commit and push to the upstream and create a pr to main"

### Analysis
- `main` had two direct commits (reveal action dock `6c9b8a6` + its Prompt.md log `8643749`) pushed earlier this session.
- User confirmed (ask_user) they want the **reveal-dock work as the PR content**, carved out of `main` into a dedicated `Alpha` branch, then a PR `Alpha → main`.

### Plan / steps taken
1. **Backup:** `backup/main-with-dock` branch created at `8643749` (local safety net; the old main state is fully recoverable).
2. **`Alpha` branch** created from `6c9b8a6` (reveal action dock commit, parent `7709f70` = pre-dock main).
3. **`main` rewound** to `7709f70` (pre-dock) and force-pushed with `--force-with-lease`, so the PR diff shows exactly the reveal-dock change (dock work returns to main via the PR merge).
4. **Push** `Alpha` to `origin`.
5. **PR opened** `Alpha → main` via GitHub API (the `gh` binary is broken in this env — auth done with the token from `~/.config/gh/hosts.yml`).

### Validation
No Gradle in this env (per AGENTS.md). Branch topology verified: `Alpha` = `7709f70` + `6c9b8a6` (+ this log commit); `main` = `7709f70` (clean). PR diff = 3 code files from the dock change. CI on the PR is the compile gate.

### Follow-ups
- After the PR merges, `main` regains the reveal-dock work (as a merge commit). The `backup/main-with-dock` local branch can be deleted once merged.
- Stray untracked `result` symlink (Nix OpenJDK artifact) still in repo root — not part of any commit.

## Follow-up request (same session): polish the reveal action dock on Alpha

**User request:** fix a light-mode glitch where the navbar flashes cream when the reveal opens; make the Start exploring + Undo button backgrounds and the dock fully transparent; swap them (Undo left, Start exploring right); make Undo say the category mirror (Unwatched/Unread/…) like Already watched.

### Changes (both on `Alpha`, updating PR #3)
- **`features/reveal/TopicRevealScreen.kt`** — dock + both buttons now `Color.Transparent` (buttons float on the page wash; Start ink = `categoryInk()`, Already ink = categoryInk when done / onSurfaceVariant idle, disabled fades ink); dock uses `heightIn(min = 80.dp)` + `windowInsetsPadding(navigationBars)`; buttons swapped (Already/Undo LEFT, Start RIGHT); new `undoLabel()` mirrors `alreadyDoneLabel` (Unwatched/Unlistened/Unread/Unseen/Unexplored); removed the now-dead `pillBg`/`pillInk` animateColorAsState plumbing.
- **`navigation/CurioNavHost.kt`** — `Scaffold(containerColor = revealWash ?: background)` where `revealWash` = the reveal route's `categoryBackgroundWash()` (computed directly, NOT in `remember` — the helper is @Composable), so the bottom strip + navbar area is seamless with the page and never flashes cream in light mode.

### Validation
String-aware brace balance OK both files; `git diff --check` clean; no lingering `pillBg`/`pillInk`/`animateColorAsState` refs; `BorderStroke` import retained (SentimentButton still uses it); review ran (flagged wide-window dock absence as pre-existing, not addressed). CI on push is the compile gate.

### Known (pre-existing, not addressed)
- On wide/tablet windows the dock doesn't render at all (the `!wide && reserveBarSpace` gate) — the reveal actions vanish on tablets; inherited from the untitled-chat dock design.

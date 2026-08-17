# Prompt.md — Request log

## Current request — SCOPE: web/ + desktop/ on hold (Android-only workstream)

User: "separate web and desktop work dont do it untill i mention it, add it in your instructions"

### Done (this turn)
- Added a 🔒 Scope rail to root `AGENTS.md`: **do NOT edit/build/touch
  anything under `web/` or `desktop/` unless the user explicitly asks in
  the current request.** Includes data mirrors — Android data fixes (topic
  JSON dedupes etc.) apply to `app/src/main/assets/` ONLY, never
  `web/src/data/topics/` or desktop data. Ambiguous requests default to
  Android-only, with the web/desktop impact noted in the summary.
- Marked both the "Desktop App" and "Web App" section headers in
  `AGENTS.md` as ⛔ ON HOLD, and added a scope warning line to the Purpose
  section.
- Committed + pushed (instruction changes must be committed per AGENTS.md).

### Pending — duplicate topics question (user: "why the duplicate books got separated again")
Investigation so far:
- `app/src/main/assets/topics/books.json` is CLEAN: 444 topics, 0 dup
  names, 0 dup ids. The dedupe (commit `620ae50`, 500→444) is intact; the
  later `4558e99` only added altPageCount/altPageLabel fields. Web mirror
  `web/src/data/topics/books.json` is also clean (444, 0 dups).
- BUT other Android topic files DO have duplicate names (same topic twice
  under different ids — tier1 short-id + tier2 full-name-id pairs):
  - authors.json — 38 dups (e.g. author-cervantes t1 + author-miguel-de-cervantes t2;
    tolstoy/leo-tolstoy; melville/herman-melville; kafka; proust…)
  - astronomy.json — 89 dups, songs.json — 26, geology.json — 11,
    animals.json — 10, technologies.json — 3, chemistry.json — 1
- No duplicate ids anywhere, so saved-entry id lookups can't collide; the
  duplicates are same-name cards in Browse/Reveal flows.
- NOT YET FIXED. Next step (when user confirms): dedupe the affected
  Android files the same way as books (keep richer entry, merge tags, keep
  tier-1 marquee), Android assets ONLY per the new scope rule.

### Verification
No Gradle build here (CI validates on push). Instruction change only — no
app behavior touched this turn.

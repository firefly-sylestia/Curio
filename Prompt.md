# Request — UI polish: reveal dock small-screen fix, detail-style pop animations, Topic History redesign

## Part 1 — Topic Reveal action dock cut off on small screens (DONE)

- `TopicRevealScreen.kt` — the dock stays a fixed 80dp (the reveal morph's reserved
  slot depends on it). Root cause: button labels wrapped to 2 lines and vertical
  paddings + nav-bar inset left too little room, so content spilled below the visible
  area. Fix: `BoxWithConstraints` with a `compact` flag (maxWidth < 440dp ||
  maxHeight < 44dp) tightening paddings/typography on phones only; both button texts
  get `maxLines = 1` + ellipsis. Tablets/wide keep the original metrics. Committed
  `0db21b7`.

## Part 2 — Detail-style center pop for more screens (DONE)

- User selected (ask_user): Save/Capture (+edit routes), Profile, Quests, Settings
  hub + all pages, Pet Designer, Topic History, Manage Categories, Recents,
  Support/Bug Report, Topic Database, and a subtle scale-fade for Home/Spin/Cabinet
  tab switches. Excluded: Lightbox, Category Picker (kept slide), Reveal (own morph),
  boot gates.
- `CurioNavHost.kt` — added `popScreenRoutePrefixes` + `isPopScreenRoute`; the
  selected screens share the detail page's center pop (scaleIn 0.88 + fadeIn,
  scaleOut 0.88 + fadeOut, underlying screen dims over the same 450ms). Tab switches
  now `scaleIn(0.97) + fadeIn` / matched fadeOut. A pop screen that opens a non-pop
  push (e.g. Settings → Lightbox) also shrinks out. Committed `468869a`.

## Part 3 — Topic History redesign (DONE)

- User direction: search & filter, polish the list, match the header style.
- `TopicHistoryScreen.kt` — header now matches the shared push-screen style
  (ExtraBold title + muted subtitle, like Recents); added the shared search field
  (matches topic names AND category display names) + a horizontally scrolling
  single-select `CurioCategoryChip` filter row (only categories present in history);
  day-group headers show entry counts; a "No matches — Clear filters" empty state
  (inside ScreenEntrance like the list); list spacing breathing-roomier.
- `ui/components/CurioSearchField.kt` (NEW) — the canonical search box (rounded
  surface + magnifier + live query + one-tap clear), extracted from the Settings
  hub's private copy; `SettingsHubScreen.kt` now uses it (private `SettingsSearchField`
  deleted, orphaned imports removed). Committed `6f6609f`.

## Validation

- Kotlin delimiter balance OK on every edited file; `git diff --check` clean;
  no stale references to deleted private functions; code-reviewed after each part
  (reviewer fixes applied: config-time version capture, post-rename assertion,
  pop-screen shrink-out, shared search component, ScreenEntrance wrap).
- Gradle builds remain CI-only per DOX rules; CI is the compile gate.

## Notes

- CI workflow release work (release-only PR builds + per-ABI split APKs) was
  completed and pushed earlier this session (commit 3425486).
- Unrelated working-tree changes (`docs/app/QUEST_AND_PET_REDESIGN_SPEC.md` deletion,
  untracked `docs/plans/`) remain untouched and out of commits.

## Completion

All three parts committed and pushed (`0db21b7`, `468869a`, `6f6609f`).

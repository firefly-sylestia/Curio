# Prompt.md — current request log

## Request: Category picker UX refinement (auto-select bug, page-1 modes, tinted selection, live state fixes)

User direction (verbatim, lightly cleaned): "we will be refining and fixing things, starting with the category picker, it has a really bad user experience — when selecting things to mix it sometimes auto-selects 2; remove the presets of science etc from page 1 and show the Curio, Knowledge and Mix options; use the classic category picker's category-tint style when selecting things in the new picker; also it was less dark/creamy in light mode; on page 2 (the new picker) show 6 your-mixes instead of 5 then 'show all' if more; in Continue exploring below that, remove the 'hold to remove' text, add a tap-and-hold action; fix the update when going to add / selecting-or-unselecting — it doesn't update, tapping back also doesn't update, only closing the picker and reopening updates it; same when creating a mix — fix these."

Clarifications asked (ask_user):
1. "Auto-select 2" — user's answer: "opening page 1 and when holding to start a mix it selects 2, same can be in other too so analyse" → the hold-to-start-a-mix path was double-selecting.
2. Light-mode creaming — user picked "Tiles + panels" (not the sheet background).

### Analysis — root causes found (all in the new picker, `features/picker/`)

- **"Holding to start a mix selects 2"** — `ClassicPickerPage` (new picker page 1) seeded `selectedSlugs` from the persisted deck (`getLastSpinCategories`) and `multiSelectMode` from `persistedVisible.size > 1` (the old v26 model the user had already rejected in v196 for the classic sheet). With a single-lane deck, the lane was already IN the set but not rendered (multiSelect off); the first long-press switched multiSelect on → the persisted lane + the held lane both lit up. Same bug existed in the classic `CategoryPickerContent` (draft ?: persistedVisible seeding).
- **Mix editor "doesn't update until reopen" + batch "auto-select 2"** — `MixEditorSheet` toggled a remembered `MutableSet` IN PLACE and wrote the SAME instance back (`selected = selected.apply { if (!add(slug)) remove(slug) }`). `mutableStateOf`'s structural equality sees the same instance → NO recomposition → ticks and the Save-label count never updated live; when an unrelated recomposition finally ran, several taps appeared at ONCE (the "it selects 2 sometimes" perception).
- **Add/Remove in Continue exploring "only updates on reopen"** — `AddSuggestionSheet` and `ContinueExploringSection` snapshotted `pickerSuggestionsState` with `remember`, so writes never reflected until the sheet was disposed and recreated. PLUS unchecking a DEFAULT suggestion was a no-op (`removePickerSuggestion` wrote against an empty user list, so defaults kept showing).
- **Preset chips vs modes** — page 1 showed Science/Entertainment/Arts & Stories/History & Ideas preset chips; the user wants the classic picker's Curio / Knowledge / Mix mode tabs instead.

### What shipped (this turn)

- **Page 1 rebuilt as a Curio / Knowledge / Mix mode picker** (`ClassicPickerPage`): mode tabs (classic tint style: `washCat.themedAccent()` selected) with grouped tap-to-open Curio/Knowledge decks and the Mix multi-select grid + Mix/Cancel row. Preset chips removed from the new picker ONLY (`CategoryPickerContent` classic keeps them). Reused `PickerMode` + `curioModeGroups`/`knowledgeModeGroups`/`PickerGroup` (made internal in `CategoryPickerScreen.kt`).
- **Clean start** — page 1 opens `multiSelectMode = false` + empty selection (v196 model: tap opens a lane, hold is the ONLY way into multi-select → holding selects exactly one). Applied to the classic `CategoryPickerContent` too (mid-session `CategoryPickerDraft` restore kept).
- **Category-tint selection** — `NewPickerTile` selected state = `themedAccent()` fill + `onAccent()` ink (icon, label, check badge, accent-ink ring, icon-plate tint) — the classic `PickerIconTile` style — everywhere selection renders.
- **Cream light mode** — new `newPickerIdleFill()` (classic cream-pill recipe `lerp(base, curioPillLift(), 0.82f)` in light; unchanged in dark) applied to every new-picker tile/pill/panel (sheet + Browse page).
- **Your mixes: 6 visible** then "Show all" (was 5); threshold + "Show less" updated.
- **Continue exploring** — "hold to remove" hint text removed; holding a lane opens a Remove pill (`CategoryOptionPill` gained optional `onRemove`); section reads `pickerSuggestionsState` reactively (live).
- **Live-state fixes** — `MixEditorSheet` uses an immutable `Set` (recomposes every tap); `AddSuggestionSheet` reads reactive state + toggles the EFFECTIVE list so unchecking defaults works; `AppPreferences.removePickerSuggestion` seeds from the effective list (defaults) before removing.
- **Fixes the mix grid at 6**: `mixes.take(6)` / `mixes.size > 6`.

### Follow-up (same request): pinned hints removed + mixes cards redesigned

User: "also in the new picker page 2 remove the hold for options hint, for pinned, and redesign the your mixes cards — they look very bad, so redesign it."

- Pinned section label hint ("hold for options") removed on page 2; the Browse Pins tab row subtitle ("Hold for options · tap to spin") removed too — the hold interaction itself is unchanged (option pill still opens).
- `NewMixCard` redesigned as a compact "mix stamp" (112dp, was 122dp): leading 38dp lane plate (first lane glyph, Tune fallback), ExtraBold name + one-line teaser, footer row of up to 4 tiny lane-composition dots tinted per lane via `categoryInk()` (16% alpha fill + glyph) with a "+N" overflow chip, and an inline Spin pill bottom-right (no more floating bottom-anchored pill); the 3-dot Edit/Delete popup stays.

Verification: compile/build/lint forbidden in this env (CI validates on push). Braces/parens/brackets balanced via script for the picker files; import set verified for `Color`/`lerp`/`curioPillLift`/`isCurioDarkTheme`/`themedAccent`/`onAccent`/`categoryInk`/`width`/`GridItemSpan`; nested-lazy-grid crash rule respected (page-1 grids are top-level in the pager page; continue-exploring stays manual chunked rows).

### Progress
- [x] Ask clarifying questions (auto-select location + light creaming scope).
- [x] Page 1: mode tabs + clean start (removed preset chips + persisted-deck seeding).
- [x] Category-tint selected tiles + cream-lift idle fills (sheet + Browse).
- [x] Mixes 6 + Show all; Continue-exploring remove pill + live reads; Add sheet + removePickerSuggestion no-op fixes; MixEditorSheet immutable set.
- [x] Classic `CategoryPickerContent` clean start (same auto-select-2 root cause).
- [x] Changelog (20260921.txt) + DOX pass (app/AGENTS.md v3xx7).
- [x] Follow-up: pinned hints removed, mixes cards redesigned (v3xx8), changelog update.
- [x] Commit & push.

### Follow-up (same request): full-app audit → user picked fixes 1 + 2

Delivered a code-verified UX audit (dead code, dead tap, pager default-page trap, Surprise-me no-undo, Continue-exploring data confusion, hidden Experiments, doc drift, duplicate picker/theme systems). User: "fix 1 and 2".

1. **Dead code removed:** legacy never-called `CategoryPickerSheet` (~480 lines) deleted from SpinScreen (verified range-only deletion + balance checks); unused `PickerPageTab` removed from CategoryPickerScreen; orphaned `getRecentCategories` / `noteRecentCategory` / `KEY_RECENT_CATEGORIES` removed from AppPreferences; stale `PickerPageTab` comment refs in DeckPresets updated.
2. **Dead image tap fixed:** `ImageThumb` in ReelNotes / Marginalia / FieldNotes now open the full-screen Lightbox — new `onImageTap` hook threaded `SaveCaptureScreen → FormatBodyForCategory → formats (+ OpenNotebookFormat sub-formats)` with `launchSingleTop`; photo-picker URIs go through `LightboxTarget` byte-for-byte. The "TODO Phase 4" / empty onClick lambdas are gone (verified 0 remaining; 16 hook wiring points).
3. Doc drift fixed: AGENTS.md `versionCode` 20260920 → 20260921 (matches the current changelog). v3xx9 DOX entry added.

Verification: brace/paren/bracket balance OK on all 9 touched files; scope check (openLightbox + call site both in SaveCaptureScreen); imageUris types confirmed `List<String>` (null-guards compile); seam read after SpinScreen range deletion; no compile/build run (forbidden — CI validates on push). Deletions done as verified line-range removals (not sed insertion).
- [x] Commit & push (lightbox + dead-code cleanup).

### Follow-up (this turn): liquid-glass theme reveal + mix-card pill + dark-mode borders

User: "in liquid glass toggle on, switching between theme doesn't play that transition animation; also in the new picker your mixes remove the spin and spinning pill; in dark mode remove that weird white border around category options, in both page 1 and 2."

- **Theme reveal with Liquid glass ON** — root cause: `startTransition`'s first capture path
  (`captureLayer.record { drawContent() }` → `GraphicsLayer.toImageBitmap`) re-invokes the
  whole Compose draw chain every frame, nested inside the kyant `layerBackdrop` record pass;
  over the glass backdrop the readback came back blank on some devices → `isBlank()` guard →
  instant flip (no animation). Fix: NEW `windowFrame()` via `PixelCopy.request(window, bitmap,
  listener, mainHandler)` runs FIRST (API 26+ gate; gated on O, glass runs on 31+) — it copies
  the actual hardware-composited window pixels (glass blur preserved) without replaying Compose
  draw. Fallbacks unchanged (layer → view → instant). Verified against AOSP
  `AndroidGraphicsLayer.android.kt` snapshot dispatch (V28 hardware path) and the kyant
  1.0.6 `LayerBackdropModifier.kt` record semantics from the repo's KMP mirror.
- **Your mixes Spin/Spinning pill removed** — `NewMixCard` footer drops the inline Spin pill
  (both "Spin" idle and "Spinning" active states); tapping the card remains the spin target;
  the now-unused `active` param removed from the signature and the call site (its
  `mix.laneIds == deckIds` expression dropped; `deckIds` still used elsewhere).
- **Dark-mode white borders** — dark scheme `outlineVariant` = cream `EDE7DC` @10%, so the
  1.5dp rings read as whitish edges; `NewPickerTile` (pinned/selected ring) and
  `AddSuggestionTile` (+ Add tile) now draw borders in LIGHT mode only (`!isCurioDarkTheme()`
  gate). Selection still reads via the solid category-tint fill + check (classic style intact).
- Docs: changelog 20260921 updated (mix-stamp bullet amended + 2 new FIX bullets); app/AGENTS.md
  v3xx10 entry added.

Verification: git diff reviewed hunk-by-hunk (picker + transition); brace balance sanity via
naive counter is unreliable on these files (nested `"${...}"` strings fool it — baseline reads
"unbalanced" too), so correctness was verified by diff review instead; imports verified
(`PixelCopy`, `Build`, `Handler`, `Looper`, `Activity`, `suspendCancellableCoroutine`,
`runCatching` is stdlib); no compile/build run (forbidden — CI validates on push).

CI round-trips: fb4d335c → CI failed (`android.graphics.PixelCopy` — it's `android.view.PixelCopy`);
8c3177cc fixed the import → CI failed on `cont.resume(result)` needing `onCancellation`
(requires-param extension in this stdlib); c93cba0c switched to the stable
`cont.resumeWith(Result.success(result))` member → CI stayed green.

### Follow-up (this turn): remove selection tick, dark white-borders, pinned hold broken

User: "remove the tick when selecting; and in dark mode the category options still have white
borders, remove it; and in new picker the pinned ones doesn't show tap and hold actions".

- **Selection tick removed** — `NewPickerTile`'s 18dp `catInk` check badge (the
  `CurioIcons.Check` circle at BottomEnd of every selected tile) deleted. The classic
  tint fill + ExtraBold label alone carry selection now. This badge was ALSO the
  "white border" still visible in dark mode: `catInk` on the dark scheme is a bright
  pastel, so the circle read as a pale dot on the selected tile — removing it clears
  the last pale element (tile/Add-tile outline borders were already light-only).
- **Option-pill overlay placement fixed ("pinned doesn't show tap-and-hold")** — root
  cause: `NewCategoryPickerSheet` emitted `CategoryOptionPill` as COLUMN SIBLINGS after
  the picker Box inside SpinScreen's ModalBottomSheet. The pill's `.fillMaxSize()`
  scrim therefore only covered the leftover space BELOW the picker content (≈0 when
  the sheet filled its max height) — the centered Surface could render entirely off
  the visible area, so hold on a Pinned pill showed nothing. Both overlays
  (optionTarget + removeTarget) moved INSIDE the picker Box as its last children, so
  the scrim + pill now cover the whole picker. The long-press wiring itself was
  already correct (`NewPinnedPill` onLongClick → `onOptionTarget`;
  `PinsTabContent` in Browse likewise).
- Verified: no other stroke/ring renderers in the picker files (no `drawRoundRect`/
  `Stroke(`/`borderTint`); `catAccent`/`CircleShape`/`CurioIcons.Check` still used
  elsewhere; diff reviewed hunk-by-hunk; no compile/build run (CI validates).
- Docs: changelog 20260921 + app/AGENTS.md v3xx11 + this log.
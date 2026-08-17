# Prompt.md — Request log

## Current request (completed) — remove Full Vosk models + dictation mic on every note/quote box — commit only, NO push

User: "remove the full models as they are laggy and crashing the app along
with my phone. and add medium model if theres more. in voice model. and the
voice bubble in save your take show it in each note and quote text box not
just in sound bite".

### Part 1 — models (done)
- Removed the 3 Full server-grade models (Full English ~1.8 GB, Gigaspeech
  English ~2.3 GB, Full Indian English ~1 GB) from `VoskModels.CATALOG`;
  dropped `Tier.FULL` (enum + rose badge tint + picker copy).
- RESEARCHED medium: alphacephei.com's catalog has NO English "medium" —
  ladder is Small ~40–60 MB / Large ~128 MB / server-grade 1–2.3 GB, so
  nothing was added (told the user).
- Startup prune (`MainActivity` → `VoskModels.pruneRemovedModels`): deletes
  installed dirs/zips whose id left the catalog, clears a stale saved
  selection; detail Transcribe button also guards `byId(modelId) != null`.

### Part 2 — dictation everywhere (done)
- NEW shared `DictationMic` (features/capture/formats/DictationMic.kt):
  owns recognizer (lazy, destroyed on dispose), RECORD_AUDIO permission
  flow, live-preview dialog; reports live-listening via `onListeningChange`.
- SoundBiteFormat refactored onto it (~330 lines of inline recognizer +
  private DictationDialog removed); dictation still counts as busy for the
  format-switch guard via a local `dictating` state.
- Mic wired into the tool dock (`trailingAction`) of EVERY note box:
  FieldNotes ×3, Marginalia journal, ReelNotes review, SoundBite note,
  GalleryWall caption (PaperLineField label row) + every quote card via
  the shared `QuoteCardEditor` (covers all formats + mood board + floating
  quote dialog). All gated on `AppPreferences.voiceToTextEnabledState`.
- Insert appends the transcript; quote cards preserve spans via
  `QuoteCardsState.setText` clamping.

### Docs
Changelog (ADD line rewritten for Large-only ladder + dictation-everywhere;
picker badge FIX line dropped the Full tier; REMOVE-style note for the Full
models; the stale "mics are gone" FIX line rewritten), AGENTS.md v158 note,
this file.

### Git state
Committed only — user still holds the push (pet designer rework d6bda78 and
rim removal a8a381b are also unpushed). CI validates on push.

## Earlier completed request — remove dark-mode hairline rims (floating nav bar + detail quick-fact box) — commit only, NO push

- `CurioFloatingNavBar` (CurioBottomNav.kt): removed the v149 dark-mode
  `BorderStroke(1.dp, White@10%)` capsule rim. `BorderStroke` import removed.
- `QuickFactCard` (EntryDetailScreen.kt): removed the v115 dark-mode
  `Modifier.border(1.dp, ink@18%)` plate rim. `foundation.border` import removed.
- Same rim still exists on the tour dock, reveal Like/Dislike pill, pet
  studio bar + floating action capsule (offered to the user).

## Earlier completed request — Pet Designer layout rework (compact nav, floating top actions, tear scrolls away) — commit only, NO push

User-confirmed: bottom nav = compact centered capsule; actions = floating
pill pinned while scrolling; tear = banner becomes the first scrollable item.

1. `PetStudioBottomNav`: dropped `fillMaxWidth()` — content-sized capsule
   centered at the bottom.
2. `EditorToolbar` → `StudioFloatingToolbar`: one rounded capsule pinned
   TopEnd below the status bar (Save pill + dirty dot, Undo/Redo/Reset/
   Share/Import circles; `ToolbarIcon` gained a `size` param). Toasts
   auto-clear after 3s.
3. Torn banner moved in-flow as the list's first item; overlay Box +
   stickyHeader + `SettingsHeroTotalHeight` top padding gone.

## Earlier completed request — light-mode nav capsule tint + smoother pill animations

- `curioFloatingNavContainer` light-mode lift 0.55 → 0.30 lerp so the page
  tint shows through the capsule (dark unchanged).
- Smoother pills (nav bar + reveal Like/Dislike): width spring damping 0.9,
  active fill fades via animateColorAsState synced to the spring, icon tint
  crossfades (200ms FastOutSlowIn), label fade 240ms FastOutSlowIn.

## Earlier completed request — reveal Like/Dislike pill matches the bigger 60dp nav-bar pill

- `RevealSentimentIconWidth/ExpandedWidth/Height` 52/96/48 → 60/128/60dp,
  segment icon 20 → 26dp, inner Row padding/spacing 7/6dp — identical to the
  nav bar pills.

## Earlier completed request — revert the nav-bar → sentiment-pill shared morph, keep the bigger bottom pill

Commit `55ebc74` added bigger pills (60/128/60 + 26dp) + a shared-element
morph (SentimentSharedElementKey / NavPillBoundsTransform). Reverted the
morph, kept the size. Code files match the pre-morph parent except size;
docs updated (AGENTS.md v153, changelog FIX line).

## Earlier completed request — workflow/instruction changes (commit only, no push)

Added to root AGENTS.md: git pull first, ask before deleting/replacing
anything, text/docs changes commit but push only with the next real change.

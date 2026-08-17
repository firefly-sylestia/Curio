# Prompt.md — Request log

## Current request — COMPLETED: voice-to-text (models / mic position / pause behavior) + dark-mode row labels

The user (one message, five asks):

1. "why there is only small model is available add more with option" — the offline
   Vosk model picker only offered three small models; add bigger tiers with the option
   to choose.
2. "the new floating voice its showing below the page which it should have shown above
   so it doesnt hide behind keyboard" — the floating dictation mic rendered below the
   visible page (behind the keyboard) when the note field was focused.
3. "when i speak it write and if i take a break and speak again it clears the previous
   one so fix that" — dictation wiped the transcript on a pause.
4. "is it possible to use the break as a full stop indicator" — pauses should insert
   periods (asked via ask_user → "Always on", no toggle).
5. "in profile and settings and its sub pages the main texts in dark mode the tets are
   black colored ? its not visible. fix it and cehck if it happened in more screens" —
   clarified via ask_user → only the ROW LABELS (not subtitles) on the option cards.

### 1 — More offline models (`VoskModels.CATALOG`)
The catalog only had the three ~40MB smalls. Added four bigger tiers (sizes verified
against alphacephei.com/vosk/models):
- `vosk-model-en-us-0.22-lgraph` — Large · English (US), ~128 MB (phone-friendly, notably
  more accurate)
- `vosk-model-en-us-0.22` — Full · English (US), ~1.8 GB
- `vosk-model-en-us-0.42-gigaspeech` — Full · English (US) — Gigaspeech, ~2.3 GB
- `vosk-model-en-in-0.5` — Full · English (India), ~1 GB
The picker's intro copy now warns the big models are heavy downloads needing real
storage + memory. No engine/transcriber change needed (it loads whatever model dir is
selected); the in-app download + delete flow already handles large files.

### 2 — Floating mic position (SoundBiteFormat)
The mic was a 52dp FAB rendered AFTER the RichTextEditor inside the scroll flow — when
the keyboard opened, it sat below the visible viewport. Moved it into the note box's own
tool dock via `RichTextEditor.trailingAction` (the slot documented as "a small dictation
button"), a 36dp tertiary circle rendered ABOVE the field. Still gated on
`voiceToTextEnabled && noteFocused && !dictationOpen`. The mic is now always visible
while typing, never behind the keyboard.

### 3 + 4 — Pause no longer wipes + break = full stop (SoundBiteFormat, always-on)
Root cause: every `onPartialResults` REPLACED the whole preview, so a blank/refreshed
partial during a pause cleared the text, and each `onResults` replaced the previous
utterance. Fix — the transcript now ACCUMULATES:
- `dictatedText` (committed utterances) + `partialTranscript` (live words of the current
  utterance) + `speechEnded` flag (pause since last commit).
- `onResults` APPENDS via `commitUtterance()` (never replaces); blank partials during a
  pause are ignored (no clear); a fresh partial after `onEndOfSpeech` commits the old
  partial first (with a prefix-match guard so same-utterance refinements aren't
  double-committed).
- Committed utterances join with ". " and the next sentence is capitalized — a break IS
  a full stop. `insertDictation`/dismiss/reset paths clear the new state; the dialog
  preview shows committed + live partial (`dictationPreview()`).

### 5 — Dark-mode row labels (CurioSettingsCard)
The card fill is a CUSTOM lerp (not a scheme token), and the uncolored row titles rely
on `LocalContentColor` — the default `contentColorFor(customFill)` resolved BLACK in
dark mode, making every row label invisible on the near-black card (subtitles were fine:
they set explicit `onSurfaceVariant`; that's why the user only saw labels break).
Fix: `CurioSettingsCard` pins `contentColor = MaterialTheme.colorScheme.onSurface`
(dark plum in light — unchanged; cream in dark — fixed). One edit covers Profile, the
Settings hub, every settings sub-page, Support, Updates, Experiments, Quests, Backup and
Onboarding cards (all 27 call sites share the component). Sweep: no other screens use a
custom-fill Surface with uncolored titles; remaining `Color.Black` usages are shadows /
scrims / explicit-ink pills only.

### Files touched
- `app/src/main/java/com/curio/app/data/OfflineTranscriber.kt` — catalog +4 models
- `app/src/main/java/com/curio/app/features/settings/SettingsSharedComponents.kt` — picker copy
- `app/src/main/java/com/curio/app/features/capture/formats/SoundBiteFormat.kt` — mic relocation + accumulation
- `app/src/main/java/com/curio/app/ui/components/CurioSettingsCard.kt` — contentColor pin
- `app/AGENTS.md` — v131 bullet
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — ADD/FIX bullets

Not done: no Gradle build here (env forbids it) — CI validates on push. The `ProfileScreen.kt`
working-tree tweak (v130 caption removal) from before this request rides along uncommitted;
it's a small text-only change, folded into the next real commit per the "no text-only push" rule.

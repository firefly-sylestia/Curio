# Prompt.md — current request

## The ask

1. **The caption's own tools and a date — the full feature.** A print's caption
   stops being a string under a picture and becomes the LABEL the print wears:
   its own face, its own size, and a date.
2. **"fix this"** — the CI failure pasted with the request
   (`PersonalTodoRow.kt:438:33 @Composable invocations can only happen from the
   context of a @Composable function`).

## The member's answers (asked before writing, per the root rail)

| Question | Answer |
| --- | --- |
| How should the date live in a caption? | **Its own line**, always formatted live (never words inside the caption) |
| Where does the date's order live? | **Both** — an app-wide preference AND a per-caption override |
| Which faces may a caption wear? | A print's own typography, plus new ones complimenting the polaroid style |
| What does the dock show while a caption has the caret? | **Face + date + size**, and the date tool is always on |

## What shipped

- `PersonalDoc.kt` — `PersonalBlock` gains `captionDateMillis` / `captionFace` /
  `captionSize` / `captionOrder` (codec keys `cdt` / `cfc` / `csz` / `cor`, each
  omitted at its default, plus a `JsonObject.long` reader). A date is a DATE, not
  text, so the order stays re-writable.
- `PersonalCaptionLabel.kt` (new) — `PersonalCaptionFace` (7 bundled faces, `""`
  = the print's own), `PersonalCaptionLabelSize` (small / standard / large as a
  multiplier on the frame's own size), `PersonalCaptionDateOrder` (day / month /
  year first) and `PersonalCaptionDates` (the app-wide order in snapshot state,
  persisted to `curio_personal_writing`), with the formatter and the today /
  yesterday stamps.
- `PersonalCanvas.kt` — the editor state's caption accessors + `captionFocusedId`;
  `PersonalPhotoBlock` draws the label (face, size, the date on its own line) and
  reports its focus, wired at all three call sites; the read view draws the same
  label from the same formatter; `PersonalToolDock` crossfades to the new
  `PersonalCaptionTools` (date · face · size · back to the writing tools) while a
  caption has the caret.
- The CI failure was already fixed by `8ef16daf` (the paper is read in the
  composition, not inside the `drawBehind` lambda) — the run pasted with this
  request was `6d293e1b`'s, before that fix.

## Open questions for the member

1. **"The date is always on"** — read here as *the date tool is always in the
   dock*. The alternative reading is *every new caption is stamped with today by
   itself*; say the word and it is a one-line change.
2. **The separator** — the request said `dd:mm:yyyy`; the labels write
   `14/03/2026` because a colon reads as a clock time. Easy to switch.
3. **The faces** — the seven offered are the app's own bundled ones. If a tenth
   face should exist just for labels, that is a font file plus one enum entry.

## Still queued (from the same request, not yet written)

- Nothing from this request — it is complete pending the three answers above.

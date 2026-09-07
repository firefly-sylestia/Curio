# Prompt Log — current request

## Request (2026-09-07, active → v3xx12 committing)

Share-card editing continues: the CI failures from the v3xx10 push, the
broken format-row icons, and a NEW global **text-history** feature the user
specified in clarifying answers:

> "add a history icon … every 10 words it saves a history or when pasted or
> edited or deleted each one saves a history globally … pill … shows up
> depending on the field in the share card it shows up top left corner …
> inside full screen text editor top right … and even in save your entry …
> text history stays for all fields same it doesn't reset even after reset
> and it will have pin system too copy and paste etc with icon and preview
> of the texts with time and date and a full preview too … overlay dialog
> box in some places and bottom sheet in some … covers every editing field
> except search or name text boxes"

Clarifications asked + answered:
- Ship mode: **always-on** (no Settings toggle).
- Field scope: **whole app** eventually (excludes search/name boxes);
  this commit wires the share-card editor family.
- Pill placement: NOT inside the field/textbox — a corner affordance of the
  sheet/editor screen, opening the whole global history.
- Capture timing: **pause + events** (stop typing / leave the field, on
  paste/edit/delete, plus every 10 words).
- "fix the cl" = fix the CI **workflow run** (compile errors) — done and
  pushed in `5ee6f232` (v3xx11). Italic "not yet" verified — the italic
  glyph was already in the font; the actually-broken glyph was UNDERLINE.

**What was done:**

1. **v3xx11 (`5ee6f232`, pushed):** CI compile fixes — polaroid filter
   matrices wrapped in `ColorMatrix`, `photoH.dp.toPx()` (Float has no
   `toPx`), proper `detectTransformGestures` import for sticker pinch/rotate.
   Icon fixes — `CurioIcons.FormatUnderline` pointed at `format_underline`
   (not in the Material Symbols catalog → literal text); now
   `format_underlined` and the icon font was re-subset with pyftsubset
   (documented flags: `--no-layout-closure --layout-features=rlig
   --glyph-names --symbol-cmap --name-IDs='*'`, text = all 284 existing
   ligature names + glyph-name list so non-PUA glyphs like `visibility_off`
   survive) to add `format_underlined` + `link`; verified zero icons lost.

2. **v3xx12 (this commit):** `ui/components/TextHistory.kt` — global
   persistent feed:
   - `TextHistoryStore`: SharedPreferences JSON (`curio_text_history`),
     cap 300, dedupe blanks/consecutive repeats.
   - `rememberTextHistoryCapture(ctx, field, text, resetKey)`: pause ~1.3s
     debounce (cancelled by keystrokes), immediate at each 10-word bucket,
     final snapshot on dispose; resetKey = `topic·activeId`.
   - `TextHistoryPill` + `TextHistoryBrowser`: centered overlay w/ newest-
     first list, pinned float, field label + time (Just now / Xm / Xh /
     d MMM · HH:mm), tap for full preview, Pin/Copy/Restore/Delete + two-tap
     Clear. Restore writes into the active field.
   - Wired in `TopicShareSheet`: capture for the fact text (label by
     activeId) + polaroid photo caption; pill at sheet-tools corner, the
     full-screen editor's top bar and the Enlarge writing sheet header.

**Pending / next:**
- User verifies italic icon on device (nothing to do unless still wrong).
- Remaining back-burner asks from earlier batches: multi-select stickers,
  sparkle info snap "always under title", quick-fact tap-out edit reset.
- Extend capture to other screens (journal / saved-entry / caption editors)
  as the user points at them.

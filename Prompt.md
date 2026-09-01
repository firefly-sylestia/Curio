# Prompt.md — current request log

## Request (ACTIVE): Share-card text formats (Word-style), Editorial drop cap, info-row moves, title no-editing

User: "in ms words how text have different formats, well lets add that
maybe" — add text formats to the quick-fact body; Editorial uses the
first-letter-big format with wrap (~3 lines) then text below; add MOVE for
the info rows (author name, year etc.) but NOT editing; title no longer
type-editable (moving/resizing guests stay). Clarified via ask_user:
- Formats: **Font family choices + Alignment** (not bold/italic, not
  letter-spacing — those weren't picked).
- Drop cap: **First letter** (classic single big initial, not first word).
- Edit scope: **Yes, exactly that** — title move/resize only, quick fact
  keeps typing, info rows movable but not editable.
- Ship mode: **Always-on** (no Settings toggle).

### Implementation (app/src/main/java/com/curio/app/ui/components/TopicShareCard.kt)

- **ShareCardMove** extended with `factFont: FontFamily?`, `factAlign:
  TextAlign?`, `metaDx/metaDy` (info-row offset). Because `move` is already
  threaded through TopicShareCard → every style → the export lambdas, the
  format + info offset apply live to the preview AND bake into the saved
  PNG for free (no extra plumbing).
- New helpers: `factBodyStyle(base, move)` applies font+align over the
  style; `moveMeta(m)` offsets the info rows.
- **Text formats in Customise**: "Fact font" pills (Serif / Sans / Type /
  Display / Elegant → null/Sora/SpaceMono/Playfair/DMSerif) and "Fact
  alignment" pills (Left/Center/Right) under the quick-fact size slider.
- **Formats applied to the fact body in all 8 styles** (Vinyl, Collage,
  Neumorphic, Editorial, Minimal, Signature, Custom, Paper/MiddleContent).
- **Editorial drop cap rework**: the old baseline-aligned 2-line initial is
  replaced with a measured wrap — TextMeasurer (TextLayoutInput API) finds
  how much body fits in the first 3 lines beside the 3x initial, that chunk
  renders beside it, the remainder continues full-width below. Single-char
  bodies fall back to plain text.
- **Info rows movable (M handle, never editable)**: moveMeta applied to
  byline/year/footer/colophon in every style; edit mode gained an "M"
  MoveHandle (bottom-right) that drives metaDx/metaDy.
- **Title no longer type-editable**: the title BasicTextField is replaced
  by a move/crop outline box only (T handle + resize edges remain); the
  quick-fact field keeps typing. ArrangeableCard dropped the editTitle/
  onTitleChange params; reset still clears everything (move = ShareCardMove()).
- **Fix**: the single-style preview branch didn't pass `move` — edit
  adjustments were invisible in single-style categories; now passes move so
  preview == export.

### Progress
- [x] ShareCardMove fields + helpers; imports (AnnotatedString, Constraints).
- [x] Editorial drop cap (TextLayoutInput-measured 3-line wrap).
- [x] factBodyStyle applied at all 8 body render sites.
- [x] moveMeta applied to all info rows; export-safe.
- [x] Edit mode: title box (no typing), F field kept, M handle added;
      hint texts updated.
- [x] Customise panel font + alignment pills.
- [x] Balance verified (code-balance delta 0/0/0 vs HEAD); single-style
      preview move passthrough fixed.
- [x] Prompt.md + changelog updated.
- [ ] DOX note in app/AGENTS.md.
- [ ] Commit & push.

### Verification status
CI validates compilation on push (this environment forbids Gradle builds) —
watch the run after pushing.
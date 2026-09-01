# Prompt.md — current request log

## Request: Signature redesign follow-ups — icon watermarks (first 7), LANGUAGE minimal, picker crash fix, CI compile fix

User direction (in order):
1. "minimal pass on the previous 7 category, and use this style where in backgrou u use letter or symbol or icons instead of drawing things, and for language just use many language texts that style"
2. Asked which categories; answer: "first 7 letter crestes dont touch album design. and fix the cl error too" (CI: unresolved `BioRhymeFontFamily`/`FrauncesFontFamily`/etc. — the 7 new font imports were missing from TopicShareCard.kt).
3. "fix this cl error and push it, then continue the task, and dont just use letter in every design bruh, be unique and creative per category"

### What shipped (this turn)

- **CI compile fix pushed** (`52a460e1`) — added the 7 missing font-family
  imports (`BioRhymeFontFamily` … `AntonFontFamily`) to TopicShareCard.kt,
  resolving all `Unresolved reference` errors in compileDebug/ReleaseKotlin.
- **Unique icon watermarks for the first-7 categories** (normal + Deepen):
  the tiny drawn crests (spotlight / paw / star / sun / frame / quill) are
  removed from `drawBackground`; instead each category renders a giant faint
  Material-Symbols glyph watermark bottom-right via `CurioIcon` — one
  UNIQUE icon per category (not a letter — all 7 start with "A" so letters
  would collide), all verified ligatures in the bundled font subset:
  Artists→brush, Animals→pets, Animated Films→movie_filter, Anime→
  auto_awesome, Artworks→museum, Authors→edit_note, Astronomy→nightlight.
  Implemented as `SignatureDesign.watermark: String?` (default null),
  rendered in `SignatureCard` after the LANGUAGE polyglot overlay
  (120.dp, bottom-end, −6° tilt, title-color tint at 12% alpha).
- **LANGUAGE minimal (normal + Deepen)** — chat bubbles + calligraphy
  strokes + dots scrapped; now flat gradient + hairline frame only (Deepen
  keeps its soft warm glow). The many-language words overlay
  (言語/Sprache/langue/idioma/lingua/… drawn via TextMeasurer at the
  composable level) is the background decoration now, per user direction.
- Album untouched (user: "dont touch album design"); classic + all other
  categories untouched.

### Progress
- [x] Push CI font-import fix (`52a460e1`).
- [x] Add `SignatureDesign.watermark` field + `CurioIcon` render site.
- [x] First-7 normal + detailed: remove tiny crests, add unique icon watermarks (14 branches).
- [x] LANGUAGE normal + detailed: strip bubbles/strokes → minimal gradient + hairline.
- [x] Verify classic untouched, crest remnants = 0, brace/paren balance clean (parens ±1 pre-existing).
- [x] DOX pass (AGENTS.md v3xx6, changelog, Prompt.md).
- [x] Commit & push (`398ee9ea`).

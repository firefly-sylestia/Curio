# Request — Curie pet overhaul (designer + behavior)

## Completed (v8.35)

All items shipped (always-on, per user decision):

1. **Name:** user-visible pet is now **Curie** (guide intro, settings label,
   content descriptions, quests line, designer subtitle). App stays "Curio".
2. **Hi-res canvas:** PetDesign supports 16/24/32 grids; the designer
   toggles 24×24 ↔ 32×32 with dominant-key resample conversion. Default art
   is a 1.5× upscale of the original 16×16. Old saved designs still parse.
3. **Painting tools:** brush (drag painting), fill bucket, eraser,
   eyedropper; 13 palette slots; hex + HSL slider color editor.
4. **Face & reactions editor:** per-mood eyes/mouth/blush/sparkles, an
   excitement editor (the EXCITED mood), and per-event reaction rules
   (enabled toggle + animation HOP/SPIN/SQUISH/BOUNCE/NONE + face), all
   serialized into the design text.
5. **PNG import/export:** export the current pose as a pixel-perfect PNG
   (FileProvider share) and import one back (snapped to nearest palette
   key); text format kept as the advanced option.
6. **Typing reaction:** when the IME opens, Curie hops above the keyboard,
   shows a tiny pixel keyboard and types along (once per screen, 60s
   cooldown).
7. **Hide-and-peek + smarter movement:** Curie crouches behind buttons /
   the bottom edge and peeks out occasionally; wander targets avoid
   landmark bounds.
8. **Touch reactions:** more animation (tier-3 adds a twirl), ~60% fewer
   speech lines.

## Validation

- Delimiter balance on all 12 touched Kotlin files + build.gradle.kts (raw
  paren count balanced). `git diff --check` clean.
- Glyph names (brush, format_paint, ink_eraser, colorize, keyboard,
  wallpaper) verified present in the bundled font subset via fontTools.
- No Gradle build run (per repo rules) — CI on push is the compile gate.
- code-reviewer-glm findings addressed: `drawPx` kept Int params (no
  implicit Int→Float), peek branches gated off `watching`/`spinning`,
  `toParsedOr` validates declared size against row count, unused imports
  removed, leftover duplicate `blushing` declaration removed.

## Follow-up

- Quests/guide copy beyond the renamed strings was left as-is.
- The DOX note for the pet layer lives in app/AGENTS.md "Curie pet layer".


## User decisions (asked via ask_user)

1. **Grid size:** support BOTH 24×24 and 32×32 canvases in the pet designer,
   with one-tap conversion between them (24 → 32 and back). Default pet art
   is 24×24. Existing 16×16 saved designs still parse and render.
2. **New features:** all always-on (no Settings toggles for the reaction
   editor, typing reaction, or hide-and-peek play).
3. **Import/export:** add PNG image export/import alongside the text format
   (user confirmed: "png works great… will it translate properly to pixel? if
   yes then do it"). PNG is rendered from the actual pixel grid, so it is
   pixel-perfect. Text stays as the advanced option.
4. **Name:** user-visible pet name becomes **Curie** (speech lines, guide,
   content descriptions, Settings label). App name / code identifiers stay
   "Curio".

## Scope (from the request)

- Rename pet → Curie (cute).
- Increase pet + editor pixels (24/32, convertible).
- Easier coloring: brush (drag painting), fill bucket, eraser, eyedropper,
  advanced color picker (HSL sliders + hex + quick picks), more palette slots.
- Reaction editor: customize eyes/mouth/blush/sparkles per mood, an
  excitement editor, and per-event reaction customization (which events
  react, which animation plays, which face is worn).
- Customizer for existing actions (event reaction config above).
- Better import/export: PNG image share/import (pixel-perfect) + text format.
- Pet reacts to typing: when the on-screen keyboard opens, Curie comes over,
  shows a tiny keyboard and "types along".
- Smarter random movement: avoid wandering over buttons/landmarks.
- Hide behind buttons when it wants to play (peek-a-boo).
- More visual touch reactions, fewer speech lines on touch.

## Implementation plan

1. `data/PetDesign.kt` — grid-size-aware model, dominant-key resample for
   24↔32, extended palette (b B o s S G g + c C d D custom + r blush + y eye),
   `PetFace` + `PetReaction` configs, tolerant parse of all sizes, text
   format gains `face=` / `react=` / `size=` lines.
2. `ui/pet/CurioPetSprite.kt` — dynamic grid size; procedural face overlays
   scale from a 16-grid coordinate space; render every palette key; blush
   ('r') and eye ('y') colors from palette; `faceOverride` param; sparkles
   toggle; `peeking` param for hide-and-peek.
3. `features/petdesigner/PetDesignerScreen.kt` — 24/32 size toggle with
   conversion, tool row (brush/fill/eraser/eyedropper), drag painting,
   HSL color editor, PNG export/import, Face & reactions section
   (per-mood face editor + excitement editor + event reaction customizer),
   preview mood chips extended.
4. `ui/pet/CurioFloatingPet.kt` — typing reaction on IME open, hide-and-peek
   behavior, landmark-aware wander, touch reactions with more animation /
   fewer lines, reaction config drives sprite face + animation.
5. Rename user-visible "Curio" → "Curie".
6. Version bump + `fastlane/.../changelogs/{versionCode}.txt` + DOX pass +
   Prompt.md summary.
7. Static validation (delimiter balance, `git diff --check`) + code review.

## Notes

- No Gradle build in this environment — CI on push is the compile gate.
- Pet designer route registration already exists (CurioNavHost line ~779).

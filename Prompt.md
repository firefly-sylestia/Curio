# Prompt Log — current request

## Request (2026-09-06, active)

Share-card editor (Customise) UI declutter + structure:

1. "the style button should only show when signature style is active and
   tapping it should switch between the 2 differnt signature style no need
   for the design options below in tool bar"
2. "the ratio of 3:4 9:12 dimention chnage make it chnage without closing
   the other tool if its open"
3. "remove that tap a thing to select swipe for another design text, and
   instead show a small text per tool, like ratio, font, style, crop,
   color, these hint text below tools when selected"
4. "the layout f standard condenced book page etc, move them inside the
   alingment tool"
5. "the content one make the no fact just eye cross icon so it hides the
   fact box with just icon no text, and similiar more improvements and
   usless text remove making the ui clutter free and more beautiful and
   easy to use"
6. "you can suggest me more features too"

Ask answers: (a) tool captions UNDER THE OPEN TOOL ONLY — the label moves
when you switch tools; (b) No-fact = eye-cross icon pill in the Content
panel AND the eye-cross replaces the words in the bottom content toggle;
(c) the ratio icon keeps a tiny always-on caption showing the active size
("3:4" / "9:16").

## Implemented (v377)

1. **Design options removed; Style tool = Signature-only variant flip.**
   The `toolOpen == "style"` panel (styles list + Current/Classic rows) is
   deleted along with the now-unused `setStyle`/`scope`. Designs switch by
   swiping the card carousel. In the edit toolbar a Style toggle appears
   ONLY when `currentStyle == SIGNATURE`: one tap flips `classicDesign`
   (haptic), caption under the icon shows the active variant
   (Current/Classic). No more design pills/panel under the toolbar.
2. **Ratio toggle**: `toolOpen = null` removed (changing 3:4 ↔ 9:16 no
   longer closes an open tool panel) + always-on caption `aspect.label`
   under the icon.
3. **Hint text removed**: "Tap a thing to select · swipe for another
   design" is gone ("Hold to edit" before editing stays). Tool captions:
   new `ToolWithCaption` wraps every pill — its tiny name (Text / Size /
   Crop / Fit / Font / Color / Adjust / Align / Format / Content) shows
   under the pill only while that tool's panel is open.
4. **Fact layout under Alignment**: Standard / Condensed / Book page /
   Editorial (+ Editorial-only Drop cap) moved from the Bold/Italic
   (format) tool into the ALIGN tool panel (fact selected); full-screen
   editor's adjacent section header renamed "Fact layout" for parity.
5. **No fact = eye-cross**: new `CurioIcons.VisibilityOff` glyph added to
   the bundled Material Symbols subset (fontTools rlig surgery on the
   existing subset: +1 rlig name, 0 lost, no cmap change, ~300 bytes —
   full-font pyftsubset explodes the glyph closure on Material Symbols'
   first-letter ligatures, so the glyph outline + rlig record were copied
   straight into the current font). Content panel's No-fact option is now
   an icon-only `IconPill`; the bottom content toggle shows just the eye-
   cross + chevron when No fact is active (no text).
6. **Copy trims**: Smart-fit panel paragraph and Adjust footer line cut to
   one line each.

Files touched: TopicShareCard.kt (toolbar/panels/hints/copy +
ToolWithCaption/IconPill helpers), CurioIcons.kt (VisibilityOff const),
material_symbols_outlined.ttf (subset +visibility_off), app/AGENTS.md
(v377), fastlane changelog.

## Notes for next request / CI

- Braces balanced in all edited Kotlin files (template-aware tokenizer).
- CI compiles on push (no Gradle here). The font change is runtime-only
  (glyph ligature verified present after surgery); Kotlin is the compile
  surface. Watch the new ToolWithCaption/IconPill wiring + removed
  style-panel references.
- Feature suggestions offered to the user at close (smart-fit per content
  type; full-screen editor tool grouping/parity; lock styles to content;
  presets/favorites for a design).

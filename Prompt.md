# Prompt.md — current request log

## Request: Picker polish + share-editor icon-only rework (batch)

User request (condensed):

> In the Spin category picker there are now 2 mix buttons — remove the top one.
> Your-mixes cards can be shorter (empty space) and don't need the "scientist ·
> films" text since the icons already show it. The morphing hold-pills open in
> the middle — they should pop in where I tapped-and-held. Back from the Browse
> category still doesn't re-open the picker. Give Browse/Mixes/Pins different
> colors, opaque when active, a bit bigger.
> Share hub editing: icons and circular pills ONLY — font-size gets its own
> icon + size dropdown, box-size a box icon + one small overlay. Change the
> Customise button to open the editing (like tap-and-hold) instead of the
> overlay. Style button → row to switch design. Aspect → toggle between the 2
> (3:4 the default). New font icon + font options + MORE fonts. Fact
> alignments + another format button + more alignments. Editing logic: nothing
> gets selected/shown on hold — tapping a thing selects it and its
> customization circular pills show up. Font change supported by ALL things.
> Paper: the category chip still drags the bulb icon along — fix; fact width
> isn't editable in Paper — fix.

## What was done

### Part A — Category picker (`NewCategoryPicker.kt`, `NewCategoryPickerBrowse.kt`)
1. **One mix button**: the Classic page's in-page "Mix · N" capsule during
   multi-select is removed; the shared bottom-row capsule ("Surprise me" ⇄
   "Mix · N") applies the pending selection; the floating Cancel pill stays
   (right-aligned).
2. **Compact mix cards**: `NewMixCard` 122→96dp; the lane-teaser text
   ("Scientist · Films · +2") is gone — the icon chips carry the composition;
   card keeps name + Active.
3. **Hold-pills at the finger**: `HoldActionsPill` gains an `anchor` Offset
   (screen coords); every long-press surface (pinned pill, mix card,
   continue-exploring tile, Browse grid, Mixes row, Pins row) reports its own
   center via `onGloballyPositioned`; the pill pops in just ABOVE the held
   spot, centered + clamped. Fallback stays centered.
4. **Browse back re-opens the picker**: `BackHandler` added to
   `CategoryPickerBrowseScreen` (pops + sets `SpinPickerRequest.pending`),
   matching the on-screen arrow.
5. **Per-tab colors + bigger**: `BrowseTab.accent()` (rose/gold/sage via
   `curioRoseInk/curioGoldInk/curioSageInk`) + `accentInk()`; active tab
   fills SOLID opaque with its accent, idle keeps the neutral fill with an
   accent-tinted icon; capsules 46→54dp.

### Part B — Share-card editor (`TopicShareCard.kt`)
1. **Per-element formats**: `ShareCardMove` grows `titleFont/Align/Bold/Italic`,
   `factBold/Italic`, `metaFont/Bold/Italic`, `badgeFont/Bold/Italic`; new
   `titleStyle`/`metaStyle`/`badgeStyle` wrappers + `factBodyStyle` bold/italic,
   threaded through all 8 card styles (title / meta / badge render sites) — the
   font + format tools act on ANY selected element, preview AND export.
2. **Selection model**: `ShareCardResizeTarget` gains NONE/META/BADGE; on
   entering edit mode NOTHING is selected/shown; tapping title / fact / meta /
   badge selects it (coffee outline + ONE move grip); unselected boxes show a
   faint outline so tap targets are discoverable; the fact typing field stays
   present (tap → focus → select).
3. **Icon-only toolbar**: the Customise overlay panel + labeled sliders are
   REPLACED by a scrollable row of circular icon pills:
   Design · Aspect · Size · Box · Font · Align · Format · Content · Reset · Done.
   Each tool opens ONE small panel under the toolbar:
   - Design → style row (all designs) + Current/Classic for Signature
   - Aspect → INSTANTLY toggles 3:4 ↔ 9:16 (default is now 3:4 / CLASSIC)
   - Size → size pills for the SELECTED element (title/fact)
   - Box → width + height sliders for the selected element
   - Font → 13 families (`shareFonts`: Serif/Sans/Mono/Elegant/Classic/Old
     Style/Bookish/Rounded/Handwritten/Condensed/Modern/Grotesk/Bouncy)
   - Align → Left / Center / Right / Justify
   - Format → Bold / Italic toggles
   - Content → source list, custom-fact field, collage photo+caption, vinyl song
   - Reset / Done
   The floating button now toggles EDIT MODE (Customise ⇄ Done) instead of a
   panel; "Hold to edit" hint becomes "Tap a thing on the card to edit it"
   while editing with nothing selected.
4. **Paper fixes**: the category chip now moves ALONE (bulb stays anchored
   top-right) — HeaderRow + Vinyl's badge row; fact width is editable —
   `moveFact` applies `fillMaxWidth` unconditionally and `FrostPane` drops its
   redundant outer fill, so the crop genuinely narrows Paper's frost pane.

### Docs
- `app/AGENTS.md` v322 bullet; changelog `20260921.txt` ADD/FIX lines;
  this Prompt.md.

## Verification
- Brace/paren balance verified on all 3 changed files vs HEAD (delta balanced).
- Toolbar glyphs verified present in `material_symbols_outlined.ttf`
  (text_increase, crop, title, notes, format_bold, format_italic, auto_awesome,
  aspect_ratio, edit, refresh, check, photo_library).
- No Gradle build in this environment (CI validates on push).

## Next steps / notes
- The new `ShareCardMove` format fields are session-only (not persisted to
  prefs) — consistent with the existing factFont/factAlign behavior.
- CI will confirm compilation; watch for any missed import on the picker /
  share-card files.
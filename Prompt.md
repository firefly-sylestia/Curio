# Prompt.md — current request log

## Request (ACTIVE): Brand-new category picker — "Category Mix Studio" (default) + classic picker toggle

User wants a FULL redesign of the category picker. Nothing of the current
picker's style may be reused. Requirements gathered via ask_user:

- **Default/always new, classic kept as a toggle**: "no just keep the old
  one as a toggle make the new one default" — the NEW picker is the default;
  the OLD glass-pill picker returns via a Settings experiment toggle
  ("Classic category picker", UserExperimentsScreen, default OFF).
- **Surface**: "sheet + full browse page where you can customise the sheet,
  and use bottom nav style" — a quick sheet from Spin (as today) plus a
  FULL-SCREEN Browse page (route PICKER) with an in-page BOTTOM NAV
  (Browse / Mixes / Pins tabs).
- **Your mix = several named mixes**: users create, name, edit, rename,
  delete, and apply multiple saved mixes (persisted JSON in AppPreferences).
  Seeded on first use from the old deckPresets (Science / Entertainment /
  Arts & Stories / History & Ideas) as named mixes.
- **Pinning = long-press**: long-press any category tile anywhere in the new
  picker pins/unpins it; pinned lanes surface as a quick-access row in the
  sheet + a Pins tab in the browse page (persisted set).
- **Surprise me = random mini-mix**: a primary capsule that shuffles ~4-6
  random ready visible lanes into a mix, applies it as the deck (spin
  re-deals straight away). Glyph: shuffle.
- **Style**: "use that liquid glass edge style, and premium minimal" — soft
  flat tiles (rounded 20dp, surface fills, hairline liquid-glass edge), no
  raised glow pills / no solid saturated selection; big friendly type; real
  liquidGlassCapsule on action capsules when the Liquid glass experiment is
  on, clean fallback otherwise. NO curioDarkGlow-pill style from the old
  picker.

### Font work (done, verified)
`material_symbols_outlined.ttf` re-subset from
`tools/fonts/material_symbols_outlined_full.ttf` adding ligatures
`shuffle`, `grid_view`, `apps` (push_pin already present). Verified 0 lost
codepoints (250→253) and 0 lost rlig ligature names (277→280) vs the old
subset. New constants added to CurioIcons.kt.

### Progress
- [x] AppPreferences: `NamedMix` data class + savedMixesState /
      classicPickerEnabledState / pickerMixesSeededState (JSON persistence +
      initThemeMode seeding). Pinned lanes reuse the EXISTING
      getPinnedCategories/togglePinnedCategory API (defaults Wildcard +
      Artists/Films/Books/Scientists, max 5).
- [x] CurioIcons: Shuffle / GridView / Apps / PushPin constants (font
      re-subset verified: 250→253 cps / 277→280 rlig names, 0 lost).
- [x] NewCategoryPicker.kt — NewPickerTile (premium-minimal: flat soft
      fill, accent-tinted glyph plate, hairline curioGlassEdge, thin accent
      ring selection, pin badge) + NewCategoryPickerSheet (header, Pinned
      row with long-press unpin, Your mixes with Spin/Spinning +
      long-press delete, Now spinning deck chips, bottom row = Surprise me
      shuffle capsule + Create (+) circle + Browse (grid) circle) +
      MixEditorSheet (name field + 3-col multi-select grid, save also
      applies the deck; edit keeps createdAtMillis id).
- [x] NewCategoryPickerBrowse.kt — CategoryPickerBrowseScreen with top bar
      + in-page bottom nav OVERLAY (Browse grid / Mixes CRUD / Pins list;
      nav capsules are the ONLY liquid-glass elements, sibling-sampling a
      local layerBackdrop over the tab-content area — v228/v241 safe
      pattern; everything else solid + glass edge).
- [x] Wiring: CurioNavHost PICKER route conditional (new browse page vs
      old CategoryPickerScreen via classicPickerEnabledState); SpinScreen
      sheet branch conditional (new NewCategoryPickerSheet vs old
      CategoryPickerContent, shared pickCategory/mixCategories lambdas,
      onBrowse = dismiss + navigate PICKER); UserExperimentsScreen
      "Category picker" section + "Classic category picker" toggle.
- [x] fastlane 20260921.txt ADD bullet + app/AGENTS.md v3xx DOX note.
- [x] Prompt.md summary.
- [ ] Commit & push.

### Glass safety notes (from app/AGENTS.md v228/v241 lessons)
No liquid-glass capsule may sample a layerBackdrop that contains its own
subtree (RenderThread SIGSEGV risk). The sheet uses NO real-glass sampling
(flat premium fills + curioGlassEdge hairline only); the Browse page's
bottom-nav capsules sample a LOCAL backdrop that records ONLY the tab
content Box (the nav is an overlaid sibling below it, so the captured pixels
are never the pills themselves).
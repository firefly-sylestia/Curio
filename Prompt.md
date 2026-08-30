# Prompt.md — current request log

## Request: Save-to-gallery fix + Songs/Alnums share-card tweaks (COMPLETE)

- User: the topic share sheet's Save button was opening the share panel
  instead of saving; Songs bars should keep a real width (they didn't ask
  for thin) but the top-to-bottom SPAN should shrink; move the Albums vinyl
  a bit down and right.
- shareComposableCard gained saveToGallery: Boolean = false — when true
  it writes the PNG to the gallery instead of launching the ACTION_SEND
  chooser (MediaStore insert on API 29+, Pictures/Curio + scan on 26-28,
  mirroring MoodBoardExport.saveBitmapToGallery) and calls onShared.
- TopicShareSheet Save button now passes saveToGallery = true.
- ShareHubBody (used by the ENTRY DETAIL share view) replaced its single
  "Share image card" button with a Save + Share side-by-side row — Save
  passes saveToGallery = true — so detail-view sharing gets the same Save
  behaviour. (TopicShareSheet already had its own Save+Share row.)
- SONGS base: bars back to w*0.016 width (real bars, not thin slivers);
  vertical span reduced via hgt = h*(0.14 + 0.22*sin) so the waveform
  stays compact around the centre.
- ALBUMS base: vinyl centre moved down + right (0.30w/0.42h -> 0.34w/
  0.47h).
- Verified: both edited files stack-balanced; OutlinedButton already
  imported in TopicShareCard.kt. Changelog bullets added. Committed &
  pushed.

---

## Request: Share hub in Settings — design grid + topic search + share (COMPLETE)

- User: "make a share hub in settings where all of the designs shows in
  full preview grid and user can select that design and pick the topic by
  search and share that topic with that design."
- Added CurioRoutes.SHARE_HUB ("share-hub") + NavHost registration
  (ShareHubScreen) + a Settings row in SettingsSections ("Share hub",
  CurioIcons.Share, subtitle "Browse every design, pick a topic, share a
  card") under Personalize next to Topic history.
- New app/src/main/java/com/curio/app/features/settings/ShareHubScreen.kt:
  - Settings-family hero (SettingsHeroHeader + glass backdrop + watermark
    backdrop + heroPageBackground tint, sticky-hero scroll pattern).
  - Topic search: CurioSearchField filtering the full topic index
    (TopicJsonLoader.loadIndex) with the Topic-Database title-first
    ranking; live results list (glyph, name, byline · category, pick
    check) capped at 40; tapping a result picks the topic and clears
    the query.
  - Picked-topic chip: category-tinted capsule with glyph + name ·
    category + clear (X).
  - Design grid: LazyVerticalGrid (2 cols compact / adaptive wide) with
    one HubDesignCell per design — a full mini TopicShareCard preview at
    the chosen aspect, accent border on the selected cell, label below.
    HubDesigns = Paper, Vinyl, Collage, Clean, Editorial, Minimal,
    Signature, Signature·Classic, Custom (9 cells — Signature twice, the
    classic variant via classicSignature=true).
  - Preview renders the picked topic (or a sample "Curiosity" wildcard
    card before any topic is chosen) so the grid always shows something.
  - Aspect pills 9:16 / 3:4 + a Share button (enabled once a design AND
    a topic are picked) that exports the full-size card through
    shareComposableCard (280dp wide, 4x density) with the topic's
    real accent/glyph/family/byline; Quotes category passes the quote
    as quoteText + author.
- Verified: stack brace/paren check balanced, all icons exist (Share,
  Check, Close, ChevronRight), CategoryId.QUOTES handled, Surface(onClick)
  pattern matches existing M3 usage, two-pane hub navigates the new row
  like other non-section rows. Changelog bullet added. Committed & pushed.

---

## Request: chemistry hexagon, thin song bars, FOOD rework + detailed Books/Films/Games (COMPLETE)

- User follow-up: chemistry WITHOUT deepen still looked weird/broken;
  give Books (and etc.) the detailed look too; make the SONGS soundwave
  bars thinner; ask/analyze which else needs changes.
- ask_user answers: All three (Books, Films AND Games) get detailed
  designs; also rework FOOD.
- Base CHEMISTRY rebuilt: replaced the scattered periodic tiles + broken
  flask with the CONNECTED hexagon lattice (honeycomb of benzene rings
  sharing edges, hero benzene with double bonds + glow, bond to lattice,
  water molecule) — same concept the user liked in the detailed set, in
  the base line-art style with a deep indigo-blue gradient.
- Base SONGS: waveform bars thinned w*0.016 → w*0.009.
- Base FOOD: reworked into a cohesive overhead table — warm linen
  gradient, hero plate centered-left with food + herbs, steaming bowl on
  the same table, steam wisps, basil leaves, crumbs.
- DETAILED set: removed the FILMS/BOOKS/GAMES early-return so all three
  now get rich detailed designs when the experiment is ON:
  - BOOKS: lamp-lit cozy library — stacked books with gold spine bands on
    a shelf, open book on a desk, gold leaf ornament.
  - FILMS: cinema — marquee light string, velvet curtain drapes, film
    reel with sprocket holes, projector beam, film strip fragment.
  - GAMES: neon arcade — perspective neon grid floor, glowing coin,
    controller with buttons, floating pixel blocks.
- Verified: stack brace/paren check OK, raw braces 861/861, detailed cat
  branches 38, early-return gone. Changelog bullet added.

---

## Request: audit + polish drawn signature-card designs (base first, then detailed) (COMPLETE)

- User: many drawn designs are broken/random/not detailed enough in BOTH
  modes (deepen on and off). Fix the non-deepened (base) designs FIRST,
  then check which detailed ones need work.
- Audited all 38 base signatureDesign branches. Flagged as broken/weak:
  ARTISTS, PAINTERS, OCEANS, SERIES, ANIMATED FILMS, FILMS, MANHWA.
- Rebuilt base designs (all keep the same text/badge colors, only the
  background changes):
  - ARTISTS: full stage scene — gradient stage, visible spotlight cones,
    light pools, singer silhouette at a round-head mic, monitor wedge
    speaker, falling confetti, floating music notes.
  - PAINTERS: easel A-frame, canvas with colour-field painting, kidney
    palette (two overlapping ovals + thumb hole) with wells around the
    rim, brush with ferrule + bristles, paint drips under the canvas.
  - OCEANS: deep azure gradient, sun shaft, surface shimmer, fish school
    (3 fish), rising bubbles, mid-water waves, coral + seaweed bed.
  - SERIES: maroon vignette + red glow, TV playing a dusk scene (mountain
    silhouettes, sun, progress bar), TV stand, episode chips on the right,
    binge-tracker dots below.
  - ANIMATED FILMS: shared-center rainbow ribbon (all arcs one center,
    tightly stacked), bouncing star trail riding the ribbon, colour orbs,
    sparkles.
  - FILMS: TWO prominent film strips down the left edge (bright grey
    bands at 0.34 alpha with clearly punched sprocket holes) + projector
    flicker beam; ALL text colors kept identical.
  - MANHWA: pastel gradient, bokeh blobs, arch with posts, floating heart
    inside the arch, orbiting sparkles.
- Verified: stack-based brace/paren check OK (current + HEAD both OK),
  raw braces 846/846, 38 detailed branches intact, all gradient drawRect
  calls properly closed. Changelog bullet added. Committed & pushed.
- NOTE: the detailed set was already rebuilt/verified in the previous
  session (ARTISTS stage, PAINTERS easel, OCEANS, SERIES, ANIMATED
  MOVIES all rich); spot-checked DISCOVERIES/SPORTS/MANHWA/WILDCARD/
  ARTWORKS — all cohesive, so no further detailed edits this round.

---

## Request: restore classic signature designs as an extra + fix collage caption (COMPLETE)

- User pointed at commit f6dd7f19 (the 13-category family-based signature
  redesign) and asked to restore those designs as an EXTRA beside the
  current per-category ones, for ALL categories, selectable per card.
- Chose (via ask_user): a "Design" pill row (Current / Classic) in the
  share sheet, next to the Aspect picker; all categories get their classic
  family design.
- Extracted f6dd7f19's signatureDesign byte-identical into
  signatureDesignClassic() (family-based branches + wildcard fallback),
  verified every lane is covered by a branch.
- TopicShareCard + SignatureCard gained classicSignature: Boolean = false;
  SignatureCard dispatches Classic > Detailed-experiment > Current.
- TopicShareSheet: classicDesign state + Design pill row (only when the
  Signature style is active), threaded to all 4 card renders.
- ShareHubBody: classicSignature/onClassicSignatureChange params + same
  pill row; EntryShareSheet holds entryClassicSignature and passes it.
- Collage caption fix: the handwritten polaroid credit used a Canvas +
  unconstrained measure (long text clipped) with lineHeight possibly
  smaller than fontSize (squished). Replaced with a width-constrained Text
  (ellipsis) and lineHeight = 1.2 x fontSize; removed now-unused
  drawText/rememberTextMeasurer imports.
- Verified: classic body identical to f6dd7f19, braces balanced (raw
  834/834), dispatcher + pickers in place, no leftover refs. Changelog
  bullet added. Committed & pushed.

---

## Request: refine deepened signature card scenes per user feedback (COMPLETE)

- User feedback on the detailed (opt-in) signature designs: chemistry flask
  looked wrong (preferred the hexagon style), art card text unreadable,
  elements felt disconnected/random, and designs stretched between 9:16
  and 3:4.
- CHEMISTRY detailed: replaced flask + tiles with a CONNECTED hexagon
  lattice — honeycomb of benzene rings sharing edges (3x3, alternating
  colors, periodic double bonds), a hero benzene ring with glow, a bond
  linking hero ring to the lattice, and a small water molecule. All
  elements bond to each other; nothing floats.
- ARTWORKS detailed: frames moved BELOW the title zone (h 0.66/0.70) and
  darkened (0xFF1C1C1F interiors) so the light title always reads against
  the dark wall; w-based frame sizing; dimmed spotlights.
- ASPECT-SAFE sizing: converted every mixed w/h-sized shape in the
  detailed set to w-based (TV, clapperboard, palette, arches, scroll,
  beaker, capsule, browser bar, laurel, contour rings, film strip) so
  proportions hold on both 9:16 (405x720) and 3:4 (450x600).
- COHESION: Discoveries trail now leads INTO the compass rose; Mythology
  laurel anchored at the temple base (with stylobate step); Manhwa heart
  floats out of the arch with orbiting sparkles; Food plate + bowl + crumbs
  share a table line.
- Verified: braces 755/755, 36 branches, 0 markers, only full-card
  gradient fills still use h (intentional). Changelog bullet added.
  Committed & pushed.

---

## Request: detailed signature card elements as an opt-in experiment (COMPLETE)

- User: many categories lack elements/designs and some don't suit — make new
  ones highly detailed, behind an opt-in Experiments toggle ("deepen").
- Added AppPreferences.detailedSignatureElementsState (default OFF) + key +
  is/set functions + loadPrefs wiring.
- Added ExperimentsScreen toggle row under a new "Share cards" section.
- SignatureCard dispatches: detailed set when toggle on, else the classic
  per-category set. Films/Books/Games keep their classic designs in BOTH.
- Wrote signatureDesignDetailed() in TopicShareCard.kt: 35 rich layered
  scenes (gradient atmospheres, radial glows, vignettes, hand-drawn art) —
  Artists twin spotlights, Albums turntable vinyl, Songs waveform/notes,
  Directors clapperboard/reel, Animated Films rainbow swoosh, Authors
  inkwell/manuscript, Painters palette/brush, Artworks gallery wall,
  Scientists blueprint/molecule/beaker, Discoveries compass/trail,
  Series TV/episode dots, Anime sakura/sunburst, Manga speedlines/
  screentone, Manhwa pastel arch, Mythology meander/temple, Sports
  floodlights/trophy, Food plate/bowl, Internet globe/nodes, Biology
  glowing helix, Chemistry tiles/flask, Animals paw trail, Plants
  botanical leaf, Technologies circuits/chip, Astronomy galaxy/planet,
  History scroll/hourglass, Geology strata/crystals, Medicine EKG/
  capsule, Psychology brain/bubbles, Mathematics golden spiral,
  Economics bars/coins, Language bubbles/calligraphy, Engineering
  blueprint/gear, Oceans rays/waves/fish, Quotes giant marks, Wildcard
  comet/orb + quiet fallback.
- Verified: 36 branches (+3 via early return = 38 total + fallback), 0
  leftover markers, braces balanced (753/753 file-wide). Changelog bullet
  added to 20260921.txt. Committed & pushed.

---

## Request: per-category signature share-card redesign (COMPLETE)

- User asked: every lane gets its OWN signature design (no shared layouts/
  design language), researched premium palette per category; keep Games,
  Films and Books as-is; Biology keeps its DNA helix.
- Rewrote signatureDesign() in TopicShareCard.kt from family-grouped
  branches to one branch per category (38 exact display-name matches +
  fallback). 35 brand-new designs: Artists (stage spotlights/mic),
  Albums (vinyl), Songs (waveform+notes), Directors (clapperboard/reels),
  Animated Films (rainbow swoosh/stars), Authors (inkwell/manuscript),
  Painters (palette/brush), Artworks (gallery wall of mini-frames),
  Scientists (molecule/beaker/blueprint), Discoveries (compass/trail),
  Series (TV/episode dots), Anime (sakura/sunburst), Manga (b/w speed
  lines/screentone), Manhwa (pastel blobs), Mythology (gold Greek-key
  meander + columns on dark marble), Sports (floodlights/field/trophy),
  Food (overhead plate/bowl/herbs), Internet (globe/nodes/browser),
  Biology (glowing helix + cells + chromosomes), Chemistry (periodic
  tiles/flask/molecule), Animals (paw trail/grass), Plants (botanical
  leaf/droplets), Technologies (circuits/chip/binary), Astronomy
  (galaxy/planet), History (scroll/hourglass/timeline), Geology
  (strata/crystals), Medicine (EKG/capsule), Psychology (brain/thought
  bubbles), Mathematics (golden spiral/geometry), Economics
  (chart/coins), Language (bubbles/calligraphy), Engineering
  (blueprint/gear), Oceans (rays/waves/fish), Quotes (giant marks/gold
  rules), Wildcard (comet/coral glow), plus a quiet neutral fallback.
- Palettes researched (science=slate navy/teal, medical=clinical teal,
  tech=deep navy/cyan, mythology=gold on marble, literature=blue/brown/
  gold, anime=sakura pink, manga=monochrome+red, webtoon=pastel).
- Verified: 38 branches, 0 empty lambdas, braces balanced (159/159),
  old family branches gone, STYLE 7/CustomCard untouched. Changelog
  bullet updated in 20260921.txt. Committed & pushed.

---

## Request: declutter signature share-card designs per category (COMPLETE)

- Presented the full element catalog of signatureDesign() in TopicShareCard.kt
  (16 category designs) and let the user pick removals per category via ask_user.
- REMOVED (Book: corner curl + gold leaf; Astronomy: ringed planet + moon;
  Biology: cell membranes + leaf veins + now-unused `s` seed; Chemistry:
  flask + bubbles; Visual Art: brushstrokes).
- EMPTIED to clean solid backgrounds (drawBackground = { _, _ -> }): Science,
  Anime/Comics, Mythology, Sports, Food, Internet, Quotes, Default/Wildcard.
- UNTOUCHED: Music, Movies, Games (user kept everything). Topic-specific
  variants (topicVariant) untouched.
- Verified: 8 empty lambdas, 0 leftover refs to removed vars, Stroke/Path
  still widely used (no import fallout). Changelog bullet added to
  20260921.txt. Committed & pushed.

---

## Request: revert 3a93fed1..HEAD — keep CurioCategoryCard, drop TopicShareCard (COMPLETE)

- Discarded the half-finished conflicted `git revert 3a93fed1` that was in
  the codespace (working tree + sequencer state).
- Reset main to `7e2cef03` (3a93fed1's parent), removing 3a93fed1,
  4c43ce85, and the merged PR block (c0e18b5c, c252b9a4, 9ba38f0b) from
  history.
- Re-applied CurioCategoryCard.kt from the old HEAD (9ba38f0b) as a new
  commit (`2d50b395`) — the category signature card refinement is kept,
  including the 4c43ce85 duplicate-annotation fix.
- TopicShareCard.kt is back to its pre-3a93fed1 version (signature redesign
  dropped).
- Verified: diff vs old HEAD touches only TopicShareCard.kt; diff vs reset
  base touches only CurioCategoryCard.kt.
- Force-pushed to origin/main (user approved reset + push). Working tree
  clean.

---

## Request: v293 — category picker topic count removal + database restart fix (COMPLETE)

- CATEGORY COUNT REMOVED: CurioCategoryCard no longer shows the "N topics"
  subtitle under each category name; only "Coming soon" and "Surprise mix"
  labels remain on their respective tiles.
- DATABASE RESTART FIX: TopicRepository.init() now pre-warms TopicJsonLoader's
  in-memory caches (countsCache + cache) from Room after confirming data
  exists. This prevents the flash of "0 topics" on restart and eliminates
  the redundant JSON re-parse that ran every process start.
- Status: committed & pushed.

---

## Request: v292k — smooth 120Hz share morphs and focused quote cards (COMPLETE)

- MORPH: switched the Shuffle → Topic Reveal bounds transform to a critically damped spring for smoother high-refresh-rate handoffs without easing snaps or overshoot.
- QUOTE SOURCES: quote-category sharing now exposes only the full quote, never the quick fact, while detail sharing retains individual quote, note, and review choices.
- QUOTE CARD: quote text remains the sole main content, is scaled to avoid truncation, and the author is anchored in the bottom-right attribution area.
- WATERMARK: replaced irregular size/gradient variation with a consistent, theme-independent gradient treatment and evenly weighted glyph placement.
- Status: static review complete; CI validation pending.

---


- ROOT CAUSE: Topic Reveal resolved its topic through `produceState`, so the shared-element destination rendered one frame with the raw route name before the canonical topic loaded. That exposed years in titles and quote text during the morph.
- FIX: Resolve the topic with `remember(topicName, cat.id)` before rendering the shared hero, giving the morph a single stable display model.
- Status: static review complete; CI validation pending.

---

## Request: v292i — Topic Database fixes: page persistence, nav spacing, search (COMPLETE)

- PAGE PERSISTENCE: currentPage saved in TopicBrowserSession (process-scoped
  singleton, same as selectedSlug) so it survives navigation to topic
  reveal and back. Previously rememberSaveable died with the composable.
- NAV BUTTONS: switched from Box(fillMaxWidth) to Row with 12dp spacing
  so prev/next sit close to the pill instead of at screen edges. Prev
  button reserves space even when invisible (fixed-size Box wrapper)
  so the pill never shifts.
- SEARCH: forces effectiveCat=null when searching so results span ALL
  categories regardless of selected filter. Results are globally sorted
  by name relevance (exact match → startsWith → contains → other fields)
  instead of per-category grouping.
- Status: committed & pushed.

---

## Request: v292h — share card redesign + quote mode + rating labels (COMPLETE)

- WATERMARK: replaced single-glyph grid with multi-glyph scatter using
  `heroWatermarkSymbols(family)` — 12 category-family icons with varied
  size (32-64dp), rotation (-20° to +20°), and organic positioning
  seeded from topic name hash. No two cards look the same.
- QUICK FACT TEXT: reduced from bodyMedium (14sp) to bodySmall (12sp)
  with tighter lineHeight (18sp) + increased maxLines so the full fact
  is visible.
- SPARKLE ICON: replaced AutoAwesome with Lightbulb in top-right and
  footer (fits discovery theme better).
- RATING LABELS: share card now shows the descriptive rating text
  ("Not for me" / "It was okay" / "Pretty good" / "Really liked it" /
  "Loved it") below the star row when a review is selected.
- QUOTE MODE: when the Quote source is selected, the card renders the
  quote as big headline text (ChangaOne) with "— Author" below, quote
  icon at top, no frost pane. Quick fact pill is hidden in quote mode.
- QUOTE TEXT SHARE: plain-text share for quotes shows just the quote
  with curly quotes + author name, no quick fact or footer.
- QUOTE FOOTER: "Shared by Name ~ Stay Curious" instead of
  "via Curio" when in quote mode.
- Status: committed & pushed.

---

## Request: v292g — Room persistence fix + smooth splash progress bar (COMPLETE)

- ROOM PERSISTENCE: `TopicRepository.populateFromJson()` was never setting
  `initialized = true` after the JSON fallback import succeeded (the bundled
  `topics.db` asset doesn't exist, so it always falls to JSON). The flag
  stayed false, causing Room re-import on every restart. Fixed by adding
  `initialized = true` after the JSON populate completes.
- SPLASH SMOOTHNESS: warm starts (Room already populated) jumped 0→100%
  in one step after a 1s delay. Replaced with a smooth 6-step ramp
  (150ms per step) so the progress bar fills incrementally — same total
  duration, no jarring jump.
- Status: committed & pushed.

---

## Request: v292f — share preview/export sync, splash speed, chip frost (IN PROGRESS)

- SHARE PREVIEW: preview renders the card at a FIXED 280dp width (matching
  the export width exactly), centered in the sheet. No more graphicsLayer
  scaling down from the full 405/450 dp card size. Export captures at the
  SAME dp dimensions via shareComposableCard with exportDensity=4x for a
  sharp PNG — preview and export are pixel-perfect.
- SPLASH: minimum delay reduced 400ms→250ms, warm-up timeout 4s→2.5s,
  entrance animation delay 60ms→30ms, loading line swap 1.1s→800ms.
- CHIPS FROST: Cabinet + Topic Database chips switched from alwaysClear
  (clear glass, compact blur) to full liquid glass with stronger wash
  (0.55-0.68) — opaque Samsung frosted look over blurred backdrop.
- Status: testing.

---

## Request: v292e — unified share hub + accurate export (COMPLETE)

- ONE SHARE HUB: `ShareHubBody` in TopicShareCard.kt is shared by the topic
  sheet AND the detail-entry sheet. EntryShareSheet now builds content pills
  from what the entry actually saved — Quote / Review (carries its star
  rating, rendered as a gold star row on the card) / Note from sessionNote,
  per capture format; Quick fact + Custom fact always offered; "Share as text
  instead" stays as a quiet TextButton (unchanged payload).
- CARD REDESIGN: torn-paper footer REMOVED (gradient only); watermark now
  tiles the CATEGORY GLYPH itself (GlyphWatermark via BoxWithConstraints +
  CurioIcon cells, seeded wobble) instead of the generic ✦; frost pane is a
  soft wash + hairline rim; footer is sparkle + single-line ellipsized
  "$name · via Curio" (cut-proof).
- PREVIEW ACCURACY: preview renders the card at FULL export dp
  (ShareCardAspect.widthDp × heightDp) scaled down with graphicsLayer
  TransformOrigin(0,0) — identical layout math to shareComposableCard's
  off-screen capture, so wrapping/placement match exactly.
- REVIEW LABEL: ReelNotesFormat helper line updates with stars — Not for me /
  It was okay / Pretty good / Really liked it / Loved it (0 stars = "Rate
  quality").
- TopicRevealScreen call site verified against the new TopicShareSheet
  signature (savedSources defaults empty). Balance-checked all touched files.
- NOTE: old private CurioShareCard in EntryDetailScreen is now unused (kept;
  removal pending user OK per delete-confirmation rule).
- web/package-lock.json user change untouched and NOT committed.
- Status: committed & pushed; CI validates compilation.

---

## Request: v292 — topic share card + wallpaper dead-code removal (COMPLETE)

- TOPIC SHARE CARD (`TopicShareCard.kt`): off-screen-capturable composable
  (software-Canvas safe: simulated frost washes + rim, seeded ✦ watermark
  pattern, torn-paper footer, "via Curio ✦" + display name). `TopicShareSheet`
  gives live preview + aspect picker (9:16 story / 3:4 classic) + fact source
  (Quick fact / Custom fact / Review with editable text), then exports the
  exact card via the shared `shareComposableCard` pipeline.
- WIRED into Topic Reveal's floating sentiment/favorite bar: new icon-only
  Share pill (`RevealCategoryFavoriteBar(onShare=…)`) opens the sheet with
  the resolved topic's name/teaser and category accent/glyph.
- WALLPAPER DEAD CODE GONE (user asked to confirm): GlassLabWallpaperService,
  GlassLabComposition, glass_lab_wallpaper.xml, manifest service entry and
  its strings removed. Lab preview still reads the wallpaper-image pref for
  its own backdrop (kept). Remaining AppPreferences glass_lab keys are lab
  composition persistence, unrelated to the service.
- web/package-lock.json user change untouched and NOT committed.
- Status: committed & pushed; CI validates compilation.

---

## Request: v291 — optimization pass: remove custom blur, speed up splash, reduce lag (COMPLETE)

- REMOVED CUSTOM BLUR ENGINE: deleted CurioBlur.kt (CPU box blur), removed
  the custom blur engine toggle from AppPreferences + ExperimentsScreen.
  Widget provider restores original Samsung system blur path (pane GONE
  for default style; custom/non-default styles still show the gradient pane).
  GlassLabWallpaperService now has its own private boxBlur() function.
- PARALLEL SPLASH PREWARM: splash screen category loading changed from
  sequential forEach to parallel launch (all lanes load at once, TopicJsonLoader's
  parseGate throttles disk I/O to 2 concurrent). Minimum splash delay
  reduced 800ms → 400ms, timeout cap 6s → 4s.
- CABINET CHIPS: added stable key(cat.id.name) to category LazyRow items
  to prevent unnecessary recompositions.
- LIQUID GLASS FPS: Kyant backdrop library is GPU-accelerated — FPS drops
  are inherent to the per-frame backdrop capture + effects pipeline and
  cannot be reduced without modifying the rendering (user requested no
  rendering changes).
- Status: pushed, CI green.

---

## Request: v287 — sharp backdrop, Kyant credit, liquid-wallpaper research (COMPLETE)

- WHOLE-WALLPAPER BLUR FIX: decode cap 1600→2560px max-dim (upscaled softness
  was visible fullscreen) + sharp backdrop now drawn with FILTER_BITMAP|DITHER
  paint (null paint = unfiltered scaling).
- CREDIT: Support screen row "Liquid glass by vFlow" → "Liquid glass by
  Kyant" (github.com/Kyant0/AndroidLiquidGlass). App depends on
  io.github.kyant0:backdrop 1.0.6 — factually correct. NOTE: tab bar file
  header still says adapted-from-vFlow (GPL) — kept for license hygiene.
- RESEARCH VERDICT (user asked): real per-pixel liquid glass in a live
  wallpaper IS possible on API 33+ via lockHardwareCanvas + AGSL
  RuntimeShader (PrismalAGSL approach; Prismal's GLES GLSurfaceView is
  View-bound and can't run in WallpaperService; Kyant lib is Compose-only).
  Proposed next step, not yet built: AGSL lens-refraction pipeline in the
  wallpaper engine, baked-frost fallback below API 33.

---

## Request: v286 — gaussian frost + decode fix + widget editor polish (COMPLETE)

- WALLPAPER GONE BUG: previous two-pass bounds+full decode reused the same
  stream/file-descriptor — first pass consumed it, second decoded null,
  silent gradient fallback. Both sources now read to ByteArray once and
  decode from the buffer (bounds + sampled passes on the bytes).
- BLUR PARITY: bilinear pyramid replaced with ScriptIntrinsicBlur (same
  gaussian family as lab's RenderEffect), downscale factor = radiusPx/25
  so any dp maps inside kernel cap; progressive doubling up; cached per
  quantized level. Vibrancy stays at draw time (saturation 1.25).
- WIDGET EDITOR (carried from prior request): provider now READS saved
  corner (was hardcoded 28dp); applyCornerShape() clips root outline
  (API31+) + rounded tint buckets r12/20/28/36 for non-Samsung;
  onAppWidgetOptionsChanged re-renders on resize (was fitXY stretching);
  minWidth 180→120dp / targetCellWidth 3→2 so pill shrinks; StyleChip is
  now a compact inline pill row with horizontal scroll.

---

## Request: v285 — live wallpaper fidelity pass (COMPLETE, CI green)

- FONT GLYPH FIX: bundled Material Symbols subset was MISSING battery_full.
  Re-subset from tools/fonts full font per CurioIcons contract (+0xE1A4/5);
  HarfBuzz verifies ligatures incl. battery_full + local_fire_department.
  Service now draws icons as CODEPOINTS (U+EF55 fire / U+E1A5 battery) —
  legacy drawText doesn't reliably apply liga (was leaking literal text).
- BLUR QUALITY: old ensureBlurred jumped small→full in 2 steps (/14 then
  up) = pixelated. New blurredFor(): progressive bilinear halving down /
  doubling up, cached PER quantized blur level (honors shape.blurDp).
- BACKDROP: decodeBackdropBounded() two-pass bounds+sample (~1600px),
  fresh stream per pass; engine caches decoded bitmap keyed by URI pref
  (no per-frame decode = no jank, no OOM silent wrong-wallpaper fallback).
- PANE PARITY: rim stroked INSIDE clip (inset rect); content re-clipped;
  added lab's 12% surface veil; typefaces match lab (bold values, medium
  pills, no shadow). Analog dial outline added; minute-hand precedence fix.
- TICK: 1s handler tick while visible so baked clocks/dates stay live.

---

## Request: v283 — live wallpaper fidelity + lab persistence (COMPLETE)

CI: corner-roundness push failure was the same NonObservableLocale lint,
already fixed in 602360e (passed); latest runs green/in-progress.

- BACKDROP: service loadBackdrop now - picked image URI first; "auto" tries
  device wallpaper ladder (getWallpaperFile -> getDrawable) before gradient
  fallback. Fixes "doesnt pick the wallpaper ive choosen".
- LIVE DATA: engine liveTitle() computes per-frame real HH:mm / session
  elapsed ("Exploring - Xm"/"Explored"), streak, battery %, date — baked
  widgets match the app ones. Refraction stays impossible in wallpapers
  (baked frost), but content is now live.
- PERSISTENCE: GlassLabComposition.Shape gains blurDp+visible (JSON);
  canvasSize saved at save-time; lab restores pos/scale/blur/text/visible
  from prefs via LaunchedEffect(Unit) on entry ("page remembers").
## Request: v282 — config UI redesign + lab size/hide fixes (COMPLETE)

CONFIG: mode cards -> 2x2 grid pills; preview = NEUTRAL wallpaper stand-in
(no rainbow bands) + accurate pane/icon/text at widget ratios, corner
slider scales preview live; custom picker in roomy 18dp-padded Surface;
Apply button label. File rewritten cleanly (v282).
LAB ROOT CAUSE of "huge widgets": v279 rebuild dropped the fixed-size
modifiers - shapes wrapped content unbounded. Restored: clock/analog 112dp,
timer 196x58, streak/battery 92dp circles, date 170x58. Glyph (tick) tile
REMOVED entirely (state/block/save/mapping). Selection ring -> drawBehind
white stroke only while selected (no resting border). Per-widget visibility:
LabShapeState.visible + if-gate per shape; editor sheet "Hide" button hides
just that widget; top pill is now Hide all / Show all (resets individual).
## Request: v281 — corner editing + in-app widget editor (COMPLETE)

- CORNER: GlassWidgetPane.read/writeCorner (corner_$id dp, default 28);
  provider passes corner*density to render; config gets "Corner roundness"
  slider (8-32dp) shown for any non-default style; preview scales its
  corner ratio live. onDeleted cleans corner key.
- IN-APP EDITOR: WidgetEditorScreen (route GLASS_WIDGET_EDITOR, Experiments
  row under lab) lists placed Curio glass widgets via
  AppWidgetManager.getAppWidgetIds(ComponentName(GlassWidgetProvider)),
  shows mode + style per id, refreshes on ON_RESUME; tap opens
  GlassWidgetConfigActivity directly with EXTRA_APPWIDGET_ID - bypasses
  launcher reconfigurable support entirely. Save/Apply re-renders the
  widget's RemoteViews immediately.
## Request: v280 - lock-screen clocks + analog widget (COMPLETE)

- CI FIX: GlassWidgetConfigActivity had two imports glued on one line
  ("clipimport"); split + Brush.solidColor -> verticalGradient (unavailable).
- ANALOG WIDGET: lab shape id "analog" - Canvas face, live hour/min hands
  from the 1s ticker, orange pin; saved in composition; service draws real
  hands each frame.
- LOCK SCREEN MODE (service): KeyguardManager.isKeyguardLocked filters
  composition to clock+analog only; on unlock nonClockAlpha animates 0->1
  over 450ms via Handler frame loop ("pops back beautifully").
## Request: v279 — Glass Widget Lab overhaul + live wallpaper (COMPLETE)

- SHAPE SYSTEM: LabShapeState (id/pos/scale/blurDp/textColor) per shape;
  LiquidLabShape(state, selectedId: MutableState<String?>, backdrop) draws
  selection ring, graphicsLayer scale (top-start origin), per-shape
  blur(state.blurDp). Tap = select (clickable), drag unchanged.
  SHAPES: clock(real HH:mm ticker), session pill (ExploreSessionStore active
  -> "Exploring - Xm" live; else "Explored"), NEW streak ring
  (arc=streak/30, fire glyph LocalFire, count), NEW battery tile (real
  BATTERY_PROPERTY_CAPACITY), NEW date pill, glyph tile, frost tile.
- SHOW/HIDE: "Hide widgets"/"Show widgets" pill top-end.
- EDITOR SHEET: bottom panel for selected shape - Size slider (0.6-1.8),
  Liquid blur (2-20dp, frost exempt), text-hue slider.
- LIVE WALLPAPER: GlassLabComposition.save/load (JSON prefs glass_lab);
  lab "Set as live wallpaper" persists visible shapes as screen-fraction
  coords then opens ACTION_CHANGE_LIVE_WALLPAPER targeted at our component.
  GlassLabWallpaperService (BIND_WALLPAPER, xml/glass_lab_wallpaper)
  draws persisted wallpaper URI (AppPreferences.getGlassLabWallpaperUri,
  gradient fallback) + baked frost panes + text/glyph via symbols font.
  Static scene by design - honest note: no refraction in wallpapers.
## Request: v278 - real HSV picker + accurate preview (COMPLETE)

- HSV PICKER: SV square (white->pureHue horizontal + transparent->black
  vertical overlay) with dual-ring pointer; hue bar below; both custom-
  drawn Canvas with tap+drag in SEPARATE pointerInput nodes (sequential
  detect* calls deadlock - tap suspends forever). Opacity = independent
  Material Slider, never touches color. State: hue/sat/val floats;
  customRgb = HSVToColor persisted to customColor_$id.
- SHARED MATH: GlassWidgetPane.gradientColors(baseRgb, opacity) is now the
  single source for custom stops (top=alpha base, bottom=30% darker,
  alpha*0.85); resolveColors CUSTOM branch delegates; config preview calls
  the SAME function so preview == rendered widget.
- ACCURATE PREVIEW: miniature widget (wallpaper bands, 20dp-corner pane or
  bare tint in Default, icon circle w/ symbols-font glyph via TextMeasurer
  drawText, mode label bold + sample info line).
## Request: v277 - chip selection fix + widget icons + Default card (COMPLETE)

- ROOT BUG (unselectable chips): v276 rewrite's StyleChip had NO clickable
  modifier - Canvas+Text Column was inert. Fixed with clickable(onClick).
  Same root cause covered "preview custom color not selectable".
- SCROLL: root Column now verticalScroll(rememberScrollState()) - fixes
  misplaced/unreachable custom section on short screens.
- DEFAULT: full-width "Default - Samsung blur" radio card above the preset
  chips (= STYLE_DEFAULT, pane GONE, pure One UI blur).
- WIDGET CONTENT: GlassWidgetMode gains glyph (local_fire_department /
  emoji_events / auto_stories / explore); resolveContent -> Triple(glyph,
  title, info) with fuller lines ("5-day explore streak", "Level 4 -
  940 XP quest XP", "3 saved discoveries", "session live right now");
  GlassWidgetPane.renderIcon draws circle tile + white glyph from
  material_symbols_outlined.ttf via ResourcesCompat; layout adds
  glass_widget_icon ImageView + two-line vertical text column.
## Request: v276 - config UI rebuilt with live preview (COMPLETE)

- LIVE PREVIEW canvas (stand-in wallpaper bands + exact resulting pane
  gradient; Blur state = tint-only wash) updates with selection/sliders.
  Caption states current mode. Fixes "can't see which color".
- Style chips: single row - Blur / presets / Custom (rainbow swatch),
  52dp rounded swatches, bold primary ring + label when selected;
  custom sliders appear ONLY in custom mode.
- Save always writes customColor/Opacity (not gated on style), so slider
  tweaks persist even if user re-taps a chip after sliding.
- Removed "Long-press ... Edit to change it later" sentence (redundant).
- Provider pane bitmap height: full hDp*density (was *0.9 leaving a bare
  root-tint strip at the bottom).
## Request: v275 - widget blur regression + white border fix (COMPLETE)

ROOT CAUSE (device-verified): One UI's wallpaper-blur detection triggers on
the widget ROOT background being a PLAIN TRANSLUCENT COLOR (#66FFFFFF).
Swapping it to any drawable (frost layer-list OR near-invisible tint)
silently disabled the blur. Root bg restored to #66FFFFFF; glass_widget_root
drawable deleted. Layout comment documents this contract.
WHITE BORDER: was the rim stroke baked into GlassWidgetPane.render() -
removed entirely (fill-only gradient), resolveColors -> Pair(top,bottom).
DEFAULT STYLE: STYLE_DEFAULT ("default") = no pane bitmap
(setViewVisibility GONE) - pure One UI blur look; config gets a "Blur" chip
first; any preset/custom draws translucent fill OVER the blur.
## Request: v274 - pane customization + lab frost tile (COMPLETE)

1. PANE ENGINE: GlassWidgetPane.kt renders the widget pane programmatically
   (vertical gradient + rim stroke bitmap). Per-widget prefs in
   glass_widget_config: style_$id (preset name | "custom"),
   customColor_$id ARGB, customOpacity_$id 0..1. Presets: LIGHT / DARK /
   CLEAR / ROSE / SKY; custom = hue slider (sat .55, val 1) + opacity
   slider (.05-.9), bottom color darkened 30% for depth.
2. LAYOUT: root bg -> @drawable/glass_widget_root (near-invisible #17FFFFFF
   rounded tint, only satisfies One UI alpha detection); visible pane =
   ImageView @id/glass_widget_pane fed setImageViewBitmap sized from
   OPTION_APPWIDGET_MIN_WIDTH/HEIGHT * density. Old glass_widget_bg(.dark)
   drawables deleted; readDarkFrost/writeDarkFrost removed.
3. CONFIG UI: preset swatch row (real gradient previews) + custom surface
   with hue sweep-gradient swatch and sliders.
4. LAB: fourth draggable tile - One UI FROST replica (baked gradient +
   rim, no refraction by design) to compare vs liquid-glass shapes.
5. PIXEL BLUR: impossible - Pixel/stock launchers have no equivalent of
   One UI's widgetStyle blur hook; nothing app-side can trigger it.

## Request: v272/273 — widget customization + lab All-files access (COMPLETE)

1. WIDGET CONFIG: GlassWidgetConfigActivity (APPWIDGET_CONFIGURE +
   reconfigurable) opens on placement and long-press Edit. Per-widget prefs
   ("glass_widget_config": mode_$id, dark_$id). Modes: STREAK / QUESTS /
   CABINET / SESSIONS. Provider renders per mode from RAW prefs
   (curio_quests xp; Room count via runBlocking on CurioDatabase;
   curio_prefs active/queued session strings) so cold-process updates are
   correct without hydrated singletons. Dark frost variant drawable
   (glass_widget_bg_dark) swapped via setInt background resource.
   onDeleted cleans up per-widget keys.
2. SQUARE FIX: layout root now wears @drawable/glass_widget_bg (translucent,
   rounded 28dp) instead of plain #66FFFFFF color - corners on all launchers.
   RISK: One UI blur detection expects translucent root; all layers are
   alpha<255 so it should still qualify - verify frosted blur still triggers.
3. LAB PERMISSION: MANAGE_EXTERNAL_STORAGE in manifest (tools:ignore).
   "Grant access" pill opens ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
   (fallback ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION); ON_RESUME observer
   re-runs autoDetect after return via pendingDetectAfterGrant flag.
4. BLUR CUSTOMIZATION: not possible - One UI Home's widget-blur params are
   launcher-internal, no system service exposes them even via Shizuku.
   Customizable surface = frost tint (light/dark) + content mode.

## Request: v271 — lab wallpaper auto-detect + One UI real widget blur (COMPLETE)

1. LAB WALLPAPER: persisted via KEY_GLASS_LAB_WALLPAPER (uri string or
   "auto"); entry reloads it (takePersistableUriPermission for picker uris).
   Auto-detect button tries WallpaperExport-style ladder:
   getWallpaperFile(FLAG_SYSTEM) → getDrawable → peekDrawable; on 13+ these
   are MANAGE_EXTERNAL_STORAGE-gated (per cvzi/WallpaperExport) so failure
   shows an honest status line pointing at the manual picker. Top-end Row:
   Auto-detect + Set wallpaper image pills + status chip.
2. REAL WIDGET GLASS (researched): RemoteViews can't sample wallpaper, BUT
   Samsung One UI 7+ launchers natively blur wallpaper behind third-party
   widgets when (a) root view id = @android:id/background with alpha 1..254
   background, (b) provider declares app:widgetStyle="colorful" + a real
   app:widgetSize (attrs.xml flags added). Implemented: layout rewritten with
   #66FFFFFF root tint + content; provider xml updated. Non-Samsung keeps
   translucent tile. Sources: thatjoshguy67/blur-widget-demo wiki.

## Request: v270 — parallax removed + material hero/pill family (COMPLETE)

1. PARALLAX REMOVED (approved): GlassParallax.kt deleted; toggle row,
   prefs (state/seed/getter/setter/KEY), NavHost sensor LaunchedEffect,
   CurioCrashReporter self-heal line all gone. tiltGlowOffset() kept as a
   constant-offset stub so TabBar call sites stay untouched.
2. SPIN heroes material look: filterHero + pickerHero fill/ink swap to
   primaryContainer/onPrimaryContainer when materialThemeOn — pills derived
   from hero ink follow automatically. Apply button → primary/onPrimary.
3. DETAIL hero under Material tears: primaryContainer (+ onPrimaryContainer
   ink), the shared Home/Profile family color — was `primary` (dark block).
4. setMaterialThemeEnabled(true) now co-enables materialHeroTears (first-run
   discoverability); explicit tears-off still sticks.

## Request: v269 — legacy blur diagnosis + 4 fixes (COMPLETE)

1. LEGACY BLUR not working on device: symptom pattern (nav solid+faux sheen,
   reveal transparent-no-blur) proves snapshot never published — toImageBitmap
   "succeeds" but replays NOTHING on some pre-12 software paths, publishing a
   BLANK image which the capsule drew as empty glass. Fix: blank-snapshot
   guard (alpha scan; blank = failure) + retry budget (4 failures before
   readbackBroken latch). If a device can't produce pixels it now falls back
   to faux cleanly instead of half-glass.
2. LAB wallpaper: added "Set wallpaper image" button (PickVisualMedia, no
   permission) since WallpaperManager.getDrawable is permission-gated and
   fails silently on many devices.
3. BUBBLE expand glitch+shift: removed the window re-centering entirely
   (view.post lagged one frame behind the resize = visible shift); window is
   TOP|START anchored so growth reads stable. Scale animations now tween-only
   (spring overshot past 1.0 → content clipped at window edge = flicker);
   corner spring critically damped.
4. PET overlay richer: idle play-bow hop every 6–14s (playKey), double-tap
   spin (spinKey), wired through service states.

## Request: v268 — watcher reverted; lab fixed + real widget (COMPLETE)

1. Screenshot watcher REVERTED per user: SessionScreenshotWatcher.kt deleted;
   service wiring, manifest permissions, Settings permission request removed.
2. Glass widget lab KEPT + refraction FIXED: glassOn now SDK>=31 only
   (decoupled from Liquid-glass toggle); LabGlassShape + back pill use RAW
   drawBackdrop (vibrancy+blur(8dp)+lens) directly — no liquidGlassCapsule
   fallback path, no clip in front. Hint text updated.
3. REAL HOME-SCREEN WIDGET (RemoteViews; no per-pixel refraction possible in
   launcher process): glass_widget_info.xml (3x1 resizable),
   drawable/glass_widget_bg.xml (frost gradient + rim + shadow base,
   28dp corners), layout/glass_widget_layout.xml (title + streak),
   GlassWidgetProvider (streak via StreakTracker.getStreak, tap → MainActivity,
   pushAll helper), manifest receiver, strings added.

## Request: v267 — revert glyph crossfade + always-glass pills (COMPLETE)

1. REVERTED the v266 nav glyph Crossfade (CurioBottomNav) — instant swap is
   back; import removed.
2. ALWAYS GLASS (dark mid-scroll killed at the root, user's proposal):
   HomeScreen menu/profile, ProfileScreen back/search — when glass on, pill
   bg = Transparent from rest (no hero-fill lerp), rim/ink pinned to scrolled
   values, glass modifier applied unconditionally (no frostShift gate).
   EntryDetailScreen detailGlassActive = glassOn (no scroll threshold) so
   back/more pills + glass more-menu are live from rest. Non-glass paths
   untouched (classic hero-fill → frost morph preserved everywhere).

## Request: v266 — session screenshots + snappy bubble + nav glyph crossfade (COMPLETE)

User correction: NO traveling blob on the classic nav — keep capsule
grow/minimize visuals; only smooth the motion.

1. SESSION SCREENSHOT WATCHER (SessionScreenshotWatcher.kt): ContentObserver
   on MediaStore.Images; gates = bubble window attached (service
   show/removeBubble) + READ_MEDIA_IMAGES/READ_EXTERNAL_STORAGE grant +
   screenshot heuristics (RELATIVE_PATH/DATA or DISPLAY_NAME contains
   "screenshot") + age < 20s + per-id dedupe. One copy via SessionShots.copyFrom
   → active session's screenshotPaths, else pending write handoff. Manifest:
   added both permissions. SettingsSectionScreen bubble toggle now also fires
   RequestPermission for media read (denial = watcher inert).
2. BUBBLE EXPAND SNAPPY (ExploreBubbleContent): corner spring 380→700
   stiffness / 0.9 damping (~180ms settle); AnimatedContent fade 90/100ms,
   scaleIn spring(0.8,700), scaleOut 0.94@100ms.
3. NAV MOTION POLISH (CurioBottomNav FloatingNavPill): glyph Crossfade(160ms)
   replaces the instant icon swap — the only hard-cut in an otherwise
   lockstep transition. No layout/visual change; classic blob untouched.

## Request: v265 — widget lab + dark-morph fix + bubble depth (COMPLETE)

1. GLASS WIDGET LAB (GlassWidgetLabScreen.kt, route GLASS_WIDGET_LAB,
   entry row in Experiments): loads current wallpaper via WallpaperManager
   (gradient fallback), records it into a LayerBackdrop, three draggable
   REAL liquid-glass shapes (112dp clock / 196×58 timer pill / 64dp glyph,
   white ink) + a glass back pill. Hint card when glass off/<12. Told user:
   real RemoteViews widgets CANNOT sample wallpaper per-pixel — lab is the
   design test bed; winning design ships as closest static treatment.
2. DARK MID-MORPH FIX (HomeScreen + ProfileScreen): with glass on, the
   solid hero fill now commits to fully-clear glass by ~40% of scroll
   (fastGlassShift = eased stickyProgress*2.5) instead of fading across the
   whole range — no more semi-transparent dark fill stacked on the blurred
   backdrop mid-scroll. Non-glass frost morph timing unchanged.
3. BUBBLE 3D DEPTH (ExploreBubbleContent): MinimizedPill = painted drop
   shadow + broad category glow halo (2 discs, +5dp/+9dp), domed radial fill
   (light 34%/28% → base → shade), 3dp sweep-gradient border (accent→white);
   paused keeps outlineVariant ring. Expanded panel = vertical sheen overlay
   (white10%→transparent→black12%) + hairline panelRim via drawWithContent.
   All painted (overlay windows clip real elevation).

## Request: v264 — legacy glass blur (pre-Android-12 real frosted glass) (COMPLETE)

User decisions: opt-in Experiments toggle; scope = bottom nav + Topic Reveal only.

1. AppPreferences: legacyGlassBlurState (+KEY_LEGACY_GLASS_BLUR, seed, setter).
   NOTE: str_replace buffer went stale on this file mid-edit — scripted insert
   used for the setter block; duplicate KEY line removed.
2. LegacyGlassBlur.kt: CurioLegacyBlur singleton (snapshot/captureSize/
   captureOrigin/readbackBroken latch), curioLegacyCapture (records pages Box
   into our own GraphicsLayer), geometry tracker, CurioLegacyBlurSnapshotter
   (125ms throttle, downscale ≤160px, pure-Kotlin stack blur ×2 rounds),
   curioLegacyGlassCapsule (maps blurred snapshot through root coords,
   clips stadium, shared veil/sheen/rim finish). Readback failure → latch off
   → faux fallback, never crash.
3. liquidGlassCapsule <31 branch: serves legacy capsule when engine active+
   snapshot ready; faux otherwise. Scope automatic — in-screen pills gated to
   12+ by isInScreenGlassActive(), so only bottom nav + Topic Reveal hit it.
4. CurioNavHost: rememberGraphicsLayer + capture/geometry modifiers on the
   same pages-only Box as the Kyant layer; snapshotter composed when active.
5. ExperimentsScreen: "Real blur (older devices)" row after parallax tilt.

## Request: v264 — Room topic migration crash fix (COMPLETE)

1. Room schema bumped from 8 to 9.
2. Topic table defaults now match TopicEntity exactly; undeclared legacy indexes are removed.
3. Migration 8→9 rebuilds topics without deleting rows and preserves existing topic data.

## Request: v263 — stable bubble drag + full pet overlay animations (COMPLETE)

1. Bubble drag: service-side onDragBy clamps x/y against display bounds LIVE
   (no off-screen drift, no release rubber-band). Expanded panel drag moved
   to its header row only (panelHeaderDragModifier keyed on `minimized`);
   whole-surface drag only while minimized — buttons/note field never fight
   the move gesture.
2. Bubble feel: raw press state tracked in interactionModifier →
   pressedScale spring squish (0.94) on the pill; AnimatedContent expand now
   one shared spring (dampingRatio 0.75/stiffness 380) + fades; corner
   radius spring unchanged.
3. Pet overlay: movingState set during wander glide frames + cleared at end;
   dragged cancels walk pose; dizzy = dragged || recoveringState (900ms post-
   drop beat, PET_RECOVER_MS). Sprite gets moving/dizzy wired — walk bob,
   lean, tail wag, lifted pose and recovery wobble all play now.
4. New changelog file 20260921.txt (versionCode bumped in 2d486d1).

## Request: v261 — hero geometry + floating pet (COMPLETE)

1. CI fix: LazyGridItemInfo uses rowIndex/columnIndex (not `path`).
2. FullBleedHeroItem rewritten MEASURED: reads slot distance from window
   edge via findRootCoordinates + offsets/resizes exactly — kills the
   left-shifted tear / right gap regardless of nesting. Pet Designer hero
   now shares this helper (was its own inline copy).
3. Floating pet overlay: position persisted (stays where placed), wander
   loop (idle strolls with eased glide + facing flips), long-press opens an
   in-window menu (Send home / Wander toggle) instead of silently exiting,
   sprite rendered directly at 84dp (ghost shadow disc removed).

## Request: v260 — crash + duplicate fixes (COMPLETE)

1. RenderThread SIGSEGV on settings sub-pages: in-screen pills (sticky back
   pills, tuning preview, Topic Reveal) fell back to the GLOBAL NavHost
   capture they sit inside -> cyclic render node. liquidGlassCapsule now
   requires opt-in (useGlobalCapture) for the global layer; others get the
   safe simulated recipe. Bottom nav is the only opt-in.
2. Tab-bar duplicate icon/label: blob's combined backdrop sampled a hidden
   copy of the tab row; default style now samples page only.
3. Nav glow toned down (highlight capped 55%, resting shadow 0.22->0.12).
4. Duplicate back pills: isPastHero waits until <45% of hero remains.
5. Hero tear flush at top (contentPadding top 0) across converted screens.
6. Explore bubble: AnimatedContent fade+scale expand; sheen corner matches
   the animated radius.

# Prompt.md — current request log

## Request: v259 — CI fixes for sticky-back rollout; real tuning preview; panel blur; defaults reverted

1. **CI**: AnimatedVisibility import in SettingsHubScreen; the Support/
   Updates/Promo sticky pills had landed OUTSIDE their composables
   (rindex anchor hit helper functions) — all three relocated INSIDE the
   ScreenEntrance lambda via brace-matching, wrapped in an explicit
   Box(fillMaxSize) for align().
2. **Tuning dialog real preview**: Canvas fake replaced by a real
   liquidGlassCapsule pill, draggable over a gradient collage with text —
   sliders write the same preference state real capsules read.
   drawGlassPreviewCapsule painter deleted + dead imports cleaned.
3. **Detail more-panel**: dropped alwaysClear → standard 8dp×scale blur by
   default (back/more pills keep alwaysClear).
4. **Defaults reverted**: Liquid glass + Clear glass back to OFF.

## Status: complete — commit & push this turn.

---

# Prompt.md — current request log

## Request: v258 — CI fix for PetOverlayService; hero side-cut + sticky back regressions

1. **CI**: PetOverlayService missing `android.app.PendingIntent` +
   `androidx.core.view.doOnLayout` imports → added.
2. **Hero cut from the sides** (v255 regression): heroes became list items
   measured inside contentPadding start/end. New shared
   `FullBleedHeroItem(edgePad)` in SettingsHubScreen.kt applies the
   negative-offset/requiredWidth viewport trick; applied to hub grid,
   SettingsSection, Experiments, Backup, Support, Updates, Promo, Quests,
   RecycleBin, Recent, ManageCategories.
3. **Sticky back with glass morph**: new shared `SettingsStickyBackPill`
   (fades/scales in once scrolled past hero top; wears liquidGlassCapsule
   when in-screen glass active, solid surfaceVariant otherwise). Wired on
   all the above + LazyListState/LazyGridState.isPastHero() helpers;
   added rememberLazyListState where screens lacked one.

## Status: complete — commit & push this turn.

---

# Prompt.md — current request log

## Request: v257 — bubble expand glitch + redesign; pet-designer bottom strip

1. **Bubble glitch root cause**: AnimatedContent's SizeTransform animated the
   container size every frame WHILE the service re-centered the WRAP_CONTENT
   overlay window on each onSizeChanged — two loops fighting over window
   geometry. Fix: Crossfade content (180ms) + corner-radius spring; panel is
   FIXED 236dp wide, pill fixed → one-step window resize.
2. **Panel redesign**: header (chip + marquee topic + minimize), big tnum
   chronometer centered with PAUSED/status caption, three equal-weight
   labeled tonal controls (Pause/Resume · Hide · Cancel in error tone),
   note field (min-width pin removed — fixed parent), Finish button.
   compactElapsed restored for the big readout.
3. **Pet designer strip**: route wasn't full-bleed-bottom → NavHost applied
   navigationBars inset padding leaving a bare background band under the
   studio capsule. Added PET_DESIGNER to fullBleedBottomRoutePrefixes +
   navigationBarsPadding on PetStudioBottomNav's wrapper Box.

## Status: complete — commit & push this turn.

---

# Prompt.md — current request log

## Request: v256 — defaults, pre-A12 glass scoping, icon optics, bubble glass, pet outside app

1. **Defaults**: Liquid glass ON, Clear glass ON (getBoolean defaults flipped).
2. **Pre-A12 scoping** ("only nav + topic reveal get it"):
   - `isInScreenGlassActive()` re-gated on SDK >= 31 → all in-screen pills
     (Home menu/avatar, Profile, Detail back/more, chip bars, Pet studio
     bar) fall back to solid; detail more-menu returns to the classic
     popup below 12 (fixes the ugly morph).
   - Bottom nav keeps its `curioFauxGlassSheen` coat; Reveal's
     `liquidGlassCapsule` still falls back to `fauxGlassCapsule`.
   - Cabinet/TopicDB chip bars switched to `isLiquidGlassPillsActive()`.
3. **Icon optics**: CurioIcon measured-ink shift += 4% of box height DOWN.
4. **Bubble glass**: ExploreBubbleContent Surface translucent (0.74 alpha) +
   curioFauxGlassSheen when Liquid glass on (cross-window capture impossible).
5. **Pet outside app**: new PetOverlayService (overlay window, OverlayOwner
   plumbing mirroring ExploreSessionService), manifest specialUse entry,
   AppPreferences petOutsideAppState (+KEY), toggle card in Pet Designer >
   Settings with overlay-permission intent. Tap = hop, drag + edge snap,
   long-press sends home & clears pref.

## Status: complete — commit & push this turn.

---

# Prompt.md — current request log

## Request: v255 — scrolling-hero conversion for ALL pinned-hero screens (+ CI import fix)

- CI fix first (bf2b037): GlassTuningDialog needed geometry.CornerRadius +
  CurioCardHeader import; deduped 5 duplicate imports.
- User confirmed scope: ALL screens with the shared SettingsHeroHeader.
- Converted pinned overlay hero → first scroll item (Home/Profile way) in:
  SettingsHubScreen (compact grid; two-pane hub hero was already static
  flow), SettingsSectionScreen (scrollToItem highlight 1→2), Experiments,
  BackupTools, Support, Updates, PromoMode, Quests, RecycleBin (incl. empty-
  state Column variant), Recent, ManageCategories.
- NOT converted: TopicDatabaseScreen — its pinned hero IS the toolbar
  (search morph + Category pill + chip-bar/back-to-top geometry keyed to
  DatabaseHeroTotalHeight). Flagged to user as a separate surgery if wanted.
- PetDesignerScreen already scrolled its hero — untouched.

## Status: complete — commit & push this turn.

---

# Prompt.md — current request log

## Request: v254 — hero-tear revert + explore bubble pass (COMPLETE, pushed)

1. **Revert the v251 sticky hero tear** (f7e9ad7): user wanted the OPPOSITE
   direction — screens with sticky heroes should act like Home/Profile, not
   Home/Profile like Settings. `git checkout f7e9ad7^ --` on HomeScreen.kt +
   ProfileScreen.kt; verified nothing from 6566ad0 was lost (its only
   Profile change removed a wrapper f7e9ad7 introduced).
2. **Explore bubble** (ExploreBubbleContent.kt + ExploreSessionService.kt):
   - Minimized pill is now icon-ONLY (46dp glyph circle; pause glyph when
     paused). Topic + timer live only in the expanded panel.
   - Smooth expand: AnimatedContent morph (fade+scale+SizeTransform spring),
     animated corner radius; RESIZE_BURST_MS 120→600 to cover the spring.
   - Edge dock: service publishes snap side via bubbleEdgeSnap state;
     after 4s idle at an edge the pill slides mostly off-screen (14dp peek,
     graphicsLayer translate); any touch undocks.
   - Auto-collapse: an untouched panel (12s) folds back to the pill unless
     the note field is focused — stops it covering what's being watched.

### Follow-ups / notes
- Pet outside the app still open (needs overlay-window service; faux glass
  only — backdrop capture can't cross windows).
- Bubble liquid glass: same cross-window limitation.

## Status: complete — commit & push this turn.

---

## v254 batch (PARTIAL - remaining items listed)

Shipped:
1. Pet games (CurioFloatingPet.kt):
   - Hide-seek teleports now pick from EIGHT perimeter spots (corners + all
     four side middles), ending the same-corner repeats.
   - Star-catch: fall speed 0.06->0.14 px/tick and speed range 70-160,
     spawn gap 700-1200ms - faster stars that no longer pile up together.
   - Tap during an ACTIVE round no longer queues the wander-dart that was
     yanking the pet off its chase/hide spot (the tap-cancels-game bug).
2. Appearance: the four inline glass sliders replaced by a single "Tune
   glass" row opening a dialog with a LIVE PREVIEW capsule (veil=shear blur,
   sheen=reflection, rim=refraction over a colorful collage). The Indicator-
   shadow slider row is REMOVED (pref stays functional, just unexposed).

NOT shipped this turn (need their own pass):
- Explore bubble: icon-only pill / smooth expand / edge-collapse / note-sheet
  cover / liquid glass (ExploreSessionService is a 964-line service; the
  overlay window cannot sample app content for real kyant backdrop - faux
  glass only).
- Pet outside the app as an option: requires a new system-overlay window
  service (same infra as the bubble).
## v253: BoxScope wrapper fix + vFlow credits

CI: matchParentSize/align still rejected - K2 will not resolve BoxScope
members against a function's EXTENSION receiver alone. Fixed by wrapping the
scrim + glass panel in an explicit `Box(Modifier.fillMaxSize())` whose content
lambda provides BoxScope as dispatch receiver. Also: About Curio gains a
"Liquid glass by vFlow" row (github.com/ChaoMixian/vFlow, GPL-2.0) and README
Credits gained an Open source section crediting vFlow's LiquidGlassBottomBar.

## v252 batch

1. BLOB: reverted v250's page-only sample back to COMBINED (user said it
   flattened the capsule-inside-blob look while pressing/moving). Doubles are
   instead solved by gating: crisp overlay renders ONLY in solid mode (classic
   mode shows the sampled row through clear glass) and FADES OUT with press so
   the refracted sample is the single image while touching.
2. HOME: quest hero moved OUT of the scroll column into a pinned overlay Box
   (inside the capture wrapper); list gets top padding = hero height - rows
   slide under the ragged tear, Settings-style. Profile: ProfileHero item
   removed from LazyColumn into an overlay Box; list contentPadding top =
   ProfileHeroTotalHeight; pills' existing frost morph now reacts to content
   scrolling under the tear.
3. SEARCH: CurioSearchField restyled iOS - flat systemGray capsule (no
   border/shadow/glow), 42dp, gray magnifier/placeholder, clear button, and
   Cancel sliding in while focused (fade+expand), which clears query + drops
   focus. Heroes passing custom ink/fill keep their tints minus the chrome.
4. Back buttons on pinned-hero screens ride the pinned bars and keep their
   existing glass-on-scroll morph; no further change needed there.

Balance-checked all touched files; CI validates.

## v251: detail more-menu glass morph + moodboard quote card from m3-layout-sweep

User: (1) detail page 3-dot should MORPH open into its dropdown with liquid
glass, iOS-smooth; (2) quotes STILL not fixed - use m3-layout-sweep branch
as-is for the inside-moodboard quote card, editor and save.

1. MoodBoardZoom.kt taken verbatim from origin/m3-layout-sweep (user-confirmed
   "as-is"): slot width coerceIn(120,240), maxW 60% board, fixed .width(renderW)
   slip + fillMaxWidth paper, textScale floor 0.5 without the 1.6 cap,
   padding back to 10/8. NOTE: this intentionally supersedes v231/v246/v248
   tweaks in that file per explicit user instruction.
2. Detail more-menu (EntryDetailScreen): with detail glass ON, tapping the dot
   crossfades the pill out while a liquid-glass panel (same capsule recipe +
   backdrop) blooms from its corner - spring(0.85,420), scale 0.55->1 anchored
   TransformOrigin(1f,0f), full-screen scrim dismiss + BackHandler. Classic
   path keeps CurioDropdownMenu popup untouched. New MoreMenuWidth=236dp;
   imports: BackHandler, animateContentSize(unused-safe), spring, ui.util.lerp,
   TransformOrigin, wrapContentSize.

Balance-checked both files; CI validates.

## v250: press ghost fix + iOS tab glide

User: touching the blob showed DUPLICATE text/icons over it (also Pet
Designer); tab switches snapped instead of gliding.

1. Ghost fix: the pill's sample went back to PAGE-ONLY. The v246 combined
   sample (page + hidden tab-row copy) re-introduced blurred ghost labels
   under the v247 crisp overlay whenever the fill faded on press - a double
   image, worse in classic mode. With the overlay guaranteeing visible ink,
   the sample no longer needs the tab row at all. rememberCombinedBackdrop
   import removed; hidden row now unsampled (harmless).
2. Glide: DampedDragAnimation.animateToValue gains an optional AnimationSpec;
   tab bar passes spring(0.82, 380) for tap switches and drag release -
   ~350ms iOS-style glide with gentle settle instead of the default 1000-
   stiffness snap.

Balance-checked both files; CI validates.

## v249: classic active indicator experiment

User asked for the previous liquid-glass style active indicator (transparent,
always-refracting, pre-v247) as an Experiments option. Added
`glassClassicIndicatorState` (default OFF = current solid white/black pill):
state + key + is/set + load in AppPreferences, an Experiments switch row, and
a branch in CurioLiquidGlassTabBar's pill recipe (always-on blur + 24dp lens +
full highlight + press-gated-only shadow + fully transparent surface when ON;
solid fill and gentle press-glass when OFF). Crisp ink overlay stays in both
modes. Balance-checked; CI validates.

## Addendum (v248): mood-board quote slip still max-sized

User: the quote card is ALWAYS at the max — not fixed by the spare-line pass.
Root cause: the floating card's Box forced `.width(renderW)` and NotePaperCard
did `fillMaxWidth()`, so every slip stretched to the full slot/resize width no
matter how short the quote. Fix: Box now `widthIn(max = renderW)` (slot width
or user resize = MAXIMUM, not fixed) and NotePaperCard wraps content with a
96dp floor for tappability. Height already wrapped; drag/resize mechanics
unchanged. File: MoodBoardZoom.kt (+widthIn import).

## Request: v247 - solid idle blob, gentle press refraction, Apple press feel (COMPLETE, pushed)

User (after v246 build): home active indicator is good, but (1) its refraction
is too high, (2) make the IDLE active pill SOLID white (light) / black (dark)
instead of transparent/reflective - while keeping the text - and restore the
blob functionality from commit d442219, (3) better touch interaction with
proper Apple-like animation.

Also fixed the v246 CI failures first (pushed 17c693b): broken `import import`
line in LiquidGlassPills.kt, IntOffset imported from the wrong package
(geometry -> unit), and a duplicate `modifier =` argument on Cabinet's grid.

## v247 implementation

1. Solid idle blob (CurioLiquidGlassTabBar.kt): onDrawSurface draws White/Black
   at alpha 1f - pressProgress; quiet resting shadow lifts the solid pill.
2. Refraction tamed: indicator-only press-gated recipe from d442219 -
   blur*xProgress, lens(10dp*p, 14dp*p, adaptive), highlight on press. Bar
   capsule keeps its always-on recipe.
3. Crisp ink overlay: third tab-row copy renders ABOVE the solid pill via new
   LocalLiquidGlassTabOverlay; items strip clickables there so touches fall
   through to the real tabs and the blob drag handlers below.
4. Apple-style press feel (LiquidGlassPills.kt): asymmetric spring - fast
   crisp press-in (stiffness 900 / damping 0.85), soft underdamped release
   (380 / 0.55) with one gentle overshoot.

Verified: balance-checked both files; CI validates compilation.

# Prompt.md — current request log

## Request: v246 — chip-bar glass, blob visibility, press feel, icon centering (COMPLETE, pushed)

User's batched asks across the session:

1. **Floating category pill in Cabinet + Topic Database → liquid glass**, with
   **one theme-only ink** (no per-category colors).
2. **Active tab icon + label vanished under the indicator** — only where the
   blob sat there was no icon/text.
3. **Touch press effect on capsules returned** — pill shrinks toward its
   middle while held + refraction blooms at the corners (from previous
   commits).
4. **Icon centering** in the search / back / home drawer-menu / avatar pills —
   still off regardless of font size.
5. **Moodboard quotes**: don't remove them; height grows with the text and
   keeps only one extra line of space (was stretching fully by height).

## What shipped

1. **Cabinet + Topic Database sticky chip bars are liquid glass now**
   (`CabinetScreen.kt`, `TopicDatabaseScreen.kt`). Each screen's scrolling
   list records a LOCAL `LayerBackdrop`; the chips are sibling overlays that
   sample it with `liquidGlassCapsule(alwaysClear = true)` — the same
   crash-safe architecture as every other in-screen pill. Labels use ONE
   theme ink: `Color.White` in dark, `Color.Black` in light, no per-category
   colors. Fixed two missing commas my interrupted script left behind.

2. **Active-tab content visible under the blob again**
   (`CurioLiquidGlassTabBar.kt`). Root cause of the vanish: v244 pointed the
   indicator's `drawBackdrop` at the page-only capture, which paints blurred
   page OVER the visible tab row sitting beneath it in z-order. Fix: restore
   the combined sample `rememberCombinedBackdrop(page, tabsBackdrop)` but
   make the hidden tab-row copy UNTINTED (removed its accent ColorFilter) —
   so icons/labels refract through the pill while the ink stays pure
   black/white (the old category-color ghost came from the tint, not from
   sampling).

3. **Press feel on floating capsules** (`LiquidGlassPills.kt`).
   `liquidGlassCapsule` gains an optional `interactionSource`: a spring
   Animatable drives (a) ~4% shrink toward the middle while held via
   `graphicsLayer`, and (b) lens refraction deepening ×(1 + 0.45·press).
   Wired on Home menu + avatar pills (`TopBarPill` new `pillInteraction`
   param), Profile back + search pills (`CurioBackButton` +
   `ProfileSearchPill` new params), Detail back + more pills. Call sites
   hoist one `MutableInteractionSource` shared by click + capsule.

4. **Measured icon centering** (`CurioIcons.kt`). `CurioIcon` now reads the
   glyph's real ink bounds from the text layout (`getBoundingBox`) and
   offsets by the delta between line-box center and ink center — every glyph
   self-centers at any font scale. Removed ALL `curioGlyphInkNudge` call
   sites (HomeScreen ×4, ProfileScreen ×2, SpinScreen ×2, CurioTopBar ×1)
   since they would double-correct; helper kept defined.

5. **Moodboard quote slip** (`MoodBoardZoom.kt`): keeps wrap-to-text height
   (two preview lines max) and adds ONE spare ruled line below the last text
   line (bottom padding 8→24dp). No fixed tall box, no full-board stretch.

## Verification

- Balance-checked all touched files (braces/parens green).
- CI validates compilation on push.
- Follow-ups to watch: chip-bar legibility over busy content; blob sample
  alignment during fast drags.

## Notes

- The moodboard quote REMOVAL request was superseded by this fix per user.
- `web/package-lock.json` user change untouched and uncommitted.


## 2026-08-23 — Bubble/pet free placement + scoped ghost fix (v262)
- Explore bubble: edge snapping/docking fully disabled — it stays wherever dropped; liquid glass removed from pill AND expanded panel (solid pane again).
- Floating pet overlay: no more auto corner/edge parking — clamps in-bounds without repositioning; position stays where the user drops it.
- Tab-bar ghost-text fix now SCOPED: new `ghostFreeTabs` param — only Pet Designer's studio bar opts out (page-only sample, crisp overlay). Home nav keeps its combined-sample blob effect (restored, per user).
- Mood-board quotes hidden behind `MoodboardQuotesHidden` flag (from previous request).
- Status: pushed. CI validates compilation.

## 2026-08-23 — Settings hero presence + true morph back pill (v263)
- SettingsStickyBackPill rebuilt as the Home sticky-bar language: scroll-LINKED progress (`heroExitProgress()` LazyList+LazyGrid variants replace boolean isPastHero) drives a scrubable fade/scale/lift morph; glass handoff via liquidGlassCapsule sampling a LOCAL layerBackdrop recorded by each screen's list (pill = sibling overlay, no self-capture cycle); null backdrop keeps the safe simulated pane.
- All 11 converted screens wired: list/grid marked `.layerBackdrop(listBackdrop)`, pill gets progress+backdrop.
- SettingsHeroBannerHeight 180→216dp for Home-like hero presence.
- Home blob question: confirmed restored — CurioBottomNav uses default ghostFreeTabs=false → combined page+tab sample; only Pet Designer opts out.
- Status: pushed. CI validates.

## 2026-08-23 — Sticky hero restore + glassy back pill + tuning preview fix (v263)
- Settings + all sub-page screens restored to STICKY (pinned) hero: banner overlay on top, content scrolls behind the ragged tear (the original construction before the v255 scrolling conversion).
- Hero's own back pill now wears REAL liquid glass when liquid-glass is enabled: each screen's scroll list records into a local `layerBackdrop`, the hero sits OUTSIDE that capture (no self-sample cycle), CurioBackButton wrapped in `liquidGlassCapsule(backdrop, alwaysClear=true)`.
- Floating morph pill (`SettingsStickyBackPill`) + `heroExitProgress` helpers fully removed (no longer needed with pinned hero).
- SettingsHeroHeader gained `glassBackdrop: LayerBackdrop? = null` param; Topic Database / other unconverted callers unaffected (null → classic opaque pill).
- `SettingsHeroBannerHeight` reverted 216→180dp (original value).
- Glass-tuning dialog preview FIXED: the preview capsule now records the colorful gradient card into its OWN local `dialogBackdrop` and wraps `liquidGlassCapsule` with that backdrop (real refraction); the `Container.removeClipToPadding()`-style fix: the capsule is a SIBLING overlay outside the captured card, outer box sized 230dp + `.background(brush, RoundedCornerShape(18.dp))` (background-only clip) so the pill can drag freely beyond the card bounds without being clipped.
- All 11 converted screens verified balanced + imports cleaned.
- Status: pushed. CI validates compilation.

## 2026-08-23 — Pet Designer hero, pill sizes, content gap, detail morph fix (v264)
- Pet Designer converted to sticky hero: banner removed from LazyColumn, pinned as overlay outside the Column (after Column close), contentPadding top = SettingsHeroTotalHeight, back pill wears glass via petGlassBackdrop.
- CurioBackButton pill size increased: padding 10dp→12dp (44→48dp) for better touch targets.
- Content gap fix: ALL converted screens' contentPadding top changed from `SettingsHeroTotalHeight + 8.dp` to `SettingsHeroTotalHeight` so content starts flush with the tear edge instead of 8dp below.
- Detail morph back button unblocked: removed the full-screen scrim Box that intercepted all touches including the back button; BackHandler (line 1156) handles system-back dismiss; dropdown item onClicks handle their own dismiss.
- Tuning preview: outer Box height 230→260dp so the draggable capsule can roam freely beyond the gradient card bounds.
- Glass tap effect: backInteraction wired through SettingsHeroHeader's CurioBackButton + liquidGlassCapsule.
- Status: pushed. CI validates.

### v284 — Lab content restored + real baked-glass live wallpaper (current)
- **Lab shapes were empty**: the git-restore redo pass dropped every LiquidLabShape
  content lambda. Restored clock text, analog hands (Canvas), timer/session text,
  streak fire icon + count, battery icon + %, date pill.
- **Frost tile didn't hide per-widget** — missing `frostState.visible` gate added.
- **Live wallpaper was a flat translucent veil** — rewrote GlassLabWallpaperService:
  each pane now samples the backdrop ALIGNED underneath it through a cached
  downscale/upscale blur with 1.25x saturation (vibrancy), plus top gloss,
  bottom depth and rim stroke — the baked equivalent of the Kyant recipe
  (blur + vibrancy). Per-pixel lens refraction still has no wallpaper-space API.
- **Service size math was wrong** (`/360f*base`): now exact dp sizes x density x
  scale matching the lab (112dp clocks, 92dp tiles, 196x58 pills), glyph+text
  layout mirroring the lab Column.
- Balance checks green; CI validates.

### v292b — CI fix + nav blob/chip frost revert (current)
- **CI failure on 3c64da5** — TopicShareCard.kt: (1) `drawRect(cornerRadius=)` isn't a valid
  DrawScope API → `drawRoundRect`; (2) missing `import androidx.compose.ui.graphics.drawscope.rotate`;
  (3) `shareComposableCard` trailing lambda couldn't bind (`card` is not the last parameter) → named
  `card = { ... }` argument.
- **Nav active blob reverted** (user call): v292's frost-at-rest removed. Resting pill is SOLID again,
  still wearing the Appearance indicator fill (auto theme / white / black); press eases the fill away so
  the touch-glass effect shows through (v247 behaviour). Blur/lens re-gated by press progress.
- **Chips frostier**: cabinet + topic-database chips read clear-plastic because `alwaysClear` cut the
  wash to ~20%. Compact surfaces now keep ~55% wash in clear mode, base ×1.6, blur 4dp.
- Changelog updated. Pushing; watching CI.

### v292c — Indicator frost back + opacity customization (current)
- **CI failure on 334031b** — the `card = {` fix omitted the closing `)` for
  `shareComposableCard(...)`; syntax restored.
- **Indicator frost-at-rest RESTORED** (user reverted my revert): idle active pill is frosted
  (vibrancy + blur + soft lens over backdrop) again; on press, frost + fill ease OFF so the
  touch blob's small press capsule shows exactly as before — untouched.
- **NEW: Indicator opacity slider** (Appearance glass-tuning dialog): 0–100% transparency of
  the resting pill's frosted wash, default 55% (v292 value). `AppPreferences.navIndicatorOpacityState`
  + get/set persisted; `CompactSliderRow` gained `maxValue`/`steps` params (opacity uses 0..1,
  steps=4 → 0/25/50/75/100).
- Pushing; watching CI.

### v292d — CI fix + category chips + pet designer duplicate text + widget icons

**Changes (uncommitted, in working tree):**

1. **CompactSliderRow CI fix** (commit `879d358`): Reordered params so `onValueChange` is last — fixes trailing-lambda binding at all call sites.

2. **Detail page bar alignment** (`EntryDetailScreen.kt`): Changed `DetailStickyBarRestTop` from `72dp` to `10dp` and side padding from `16dp` to `20dp`. The expanded glass menu panel now anchors level with the pills — back and 3-dot line up on every screen.

3. **Category chips liquid glass** (`LiquidGlassPills.kt`): Reverted v292b frost boost. Compact blur back to `0.5dp`, highlight back to `reflScale` (not `reflScale * 1.2f`), wash back to standard `0.20f` clear factor. Chips read as clear liquid glass again, not milky plastic.

4. **Pet Designer duplicate text** (`CurioLiquidGlassTabBar.kt`): Ink overlay now skips entirely when `ghostFreeTabs = true` (Pet Designer). The overlay's `inkOverlayAlpha` also targets 0 for ghostFreeTabs. Eliminates the double-text when holding the blob.

5. **Widget icon sizes** (XML layouts): Fire widget icon 26dp → 36dp, count text 16sp → 18sp. Clock widget margin 8dp → 4dp.

**Remaining:**
- Liquid glass 120fps: the per-chip drawBackdrop calls scale linearly with visible chip count. Current compact mode (no lens, 0.5dp blur) is already optimal per-chip. Full 120fps with many visible chips requires architectural batching (render all chips as one backdrop region).
- Pushing; watching CI.

## Current request — share-card visual fixes
- Scope: Android app only. Do not touch web/desktop.
- User asked: Vinyl info block text should sit at the bottom-left corner only with no enclosing box.
- User asked: Paper 9:16 share-card text is still getting cut; reduce quick-fact text size by 1sp.
- User asked: Clean share-card design is bad; scratch/rework that style into a monochrome look with a new high-depth style.
- User asked: Collage middle design looks cut/disconnected; fix it so it connects better.
- User preference for this request: add to Prompt.md; do not delete/replace existing Prompt.md content.

## Current request follow-up — share-card polish revision
- Keep Android-only scope.
- Vinyl: improve bottom-left info placement/size and preserve text color with a subtle white lift/shadow.
- Clean: replace the monochrome portal attempt again with a category-unique depth poster.
- Collage: remove the cream/green seam glitch with a more natural torn-paper seam, add more watermark glyphs/details, keep tape inside bounds, allow 4-line title without polaroid overlap, make polaroid caption editable, and move via-Curio text into the polaroid/footer without the leading icon.

# Prompt Log — current request
## Request (2026-09-10, completed — Everything wall: no seam + uniform covers; Text history gets the JSX concept feature set)

User: "the cabinets arrangement isn't good — remove the background fill,
and the covers are getting different shapes but I want them to have the
shape they are in and arrange in that shape… they can be 2x 3x .5x etc
but in both width and height… also you see this jsx — it has better text
history features and options — implement all of the features of text
history properly in detail and properly and they should be working and a
lot better, the jsx is a concept show." (Also: "don't clear the full
prompt / it erases the prompt I added in the end" — the queued prompt is
now preserved in the ## next prompt section.)

**Shipped (2 files + docs, this commit):**
1. **CabinetV2Content.kt — Everything wall fixed:** the SEAM plate (the
   grid's background fill that boxed every gap between covers) is
   REMOVED — covers sit clean on the page's own surface. The size tiers
   are now UNIFORM scales instead of aspect-dividing: every cover keeps
   its OWN shape (book 0.667 / album 1 / series 0.72) and the tier
   widens AND tallens it together. The grid runs on 8 base columns and
   each tier is a span (`(tier * 2).toInt().coerceIn(1, 8)`): 3x → 6
   spans, 2x → 4, 1.5x → 3, 1x → 2, 0.5x → 1 — a REAL half-size cover
   (the old 4-column grid could never render 0.5x).
2. **TextHistory.kt — the JSX concept feature set:**
   - SEARCH: frosted `HistorySearchBox` pill (placeholder + clear),
     matches text or field label, live in List AND Tree views.
   - FILTER chips: All / Edits / Initial / Pinned (Initial = each
     field's first snapshot; Edits = the rest); the tree is rebuilt
     from the filtered set; a SearchOff no-results state offers Clear
     filters; the header reads "X of Y snapshots" while filtering.
   - COMPARE: a Layers action on every row (list + tree) arms a banner
     ("Comparing {time} — tap another snapshot's compare"), the second
     pick opens a WORD-LEVEL LCS diff dialog — removed words struck in
     red, added words warm-highlighted, two version cards + removed /
     added word-count chips (the JSX compare panel).
   - COLLAPSIBLE groups: field-card headings toggle with a chevron
     (KeyboardArrow) + AnimatedVisibility (the JSX group collapse).
   - CURRENT pills on each field's newest snapshot + +/− word-delta
     badges ("+3 words" / "Original") in list rows and tree nodes.
   - Full-text preview gained stats (words · characters) + a changes
     note (vs the field's previous snapshot) + Compare / Restore
     actions (the JSX detail panel).

**Scope:** Android app only (web/ + desktop/ untouched).

**Status:** committed + pushed; CI validates. A NEW prompt is queued in
## next prompt below (shelf-art redraws, top-nav highlight animation, back
mid-animation glitch).

## Request (2026-09-10, completed — empty-state tree removed, shelf arts rebalanced, per-card random texture, rail centering, quick-tools back-stack fix, backup box doodle, drag visibility, footer fix, secondary card doodles)

User: the cabinet empty state of collections — the tree isn't right, you
can remove it; in collections each drawing (Curiying now, Saved entries,
Completed, and the custom ones) is not detailed/clean/good so redraw them
properly; the settings card bubbles and sparkles — make them random, not
the same for all; the auto smooth scroll — keep the ACTIVE scroll in the
MIDDLE, not always on the left side; the quick settings create multiple
backs — fix that; redraw the Backup & restore one too, it matches the
icon too much; the Manage Categories hold action is good only the
selected category isn't visible when tap-holding and moving it — fix it;
the settings footer's dots aren't good looking and the footer color too —
fix both; and give the card-doodle design to Recycle bin, Updates and
Help & feedback too.

**Shipped (3 files + docs, this commit):**
1. **CurioDoodleEmptyState.kt — the "tree" is gone:** the leaf sprig
   (stem + 4 leaves on the left) removed — it read as a lopsided tree.
   The book stack breathes wider (0.64w books, a spine tick on the
   bottom book) and the note card leans against the stack with a folded
   corner + fold line. Twinkle stars stay (scene accents).
2. **CabinetShelves.kt — shelf arts REBALANCED (bigger, centred,
   grounded, more detail):**
   - New shared `groundShadow()` helper — a soft ink ellipse under every
     subject, tying the art to the card's foot (replaces the old
     far-off filler dots).
   - Curiying now (READING): bigger centred book (page half 0.36w),
     spine crease line beside the white join, a proper ROUNDED mug
     (tapered body + saucer line + handle ring + two taller S-steam
     wisps).
   - Completed (PEAK): TALLER true-peak near ridge (apex 0.38h), the
     flag planted ON the summit (was floating at 0.48h offset), a
     zigzag-hem snow cap draped over the apex, thinner ground band,
     sun/birds/badge repositioned around the centred subject.
   - Saved entries (PHOTOS): the three prints now FAN from a shared
     base point at bottom-centre (bigger 0.34w prints), photo
     inner-shadow hairline, ANGLED tape strips, and a doodle paperclip
     on the front print's corner.
   - Custom pool: StarArt centred (0.50/0.52 at 0.38u) with a grounding
     shadow; the four MINIMAL_* scenes re-centred and detailed — the
     sun now RISES from a horizon line between two soft hills with a
     5-tick ray fan, the rings gained a second orbit dot on the outer
     ring, the wave got two drops above it, and the dots drift along a
     drawn connecting arc (no lone filler dots anywhere).
3. **SettingsHubScreen.kt — hub polish (6 of the 9 items):**
   - PER-CARD RANDOM TEXTURE: new `SettingsCardTexture(seed)` — 3
     outlined bubbles + 5 speckle dots seeded from the card id via
     `kotlin.random.Random(id.hashCode())` and `remember(card.id)`, so
     every card wears a DIFFERENT-but-stable scatter (bubbles hug the
     top-right / bottom-left zones; speckles scatter the lower band).
   - RAIL AUTO-CENTRE: `SettingsNavRail` gained a `LaunchedEffect(active)`
     that measures the active chip and glides the row with
     `animateScrollBy` (calm spring, 0.95/200) so the ACTIVE chip sits
     MID-VIEWPORT instead of stuck at the left edge; skips when the
     drift is <10% of the viewport (no twitch), one frame settle first,
     end chips clamp naturally.
   - QUICK TOOLS BACK-STACK FIX: the quick-tool chips now navigate with
     `popUpTo(SETTINGS) { inclusive = false }` exactly like the rail
     chips — deep pages REPLACE each other instead of stacking, so the
     hub is always one back away.
   - BACKUP DOODLE REDRAWN: the CLOUD visual is now an open ARCHIVE BOX
     — tilted lid, label plate, two file folders peeking out, tapered
     slate body + a curved restore arrow looping back INTO the box —
     the old four-lobe cloud + up arrow was literally the Backup icon
     drawn bigger.
   - FOOTER: the ✦✧✦ dots line removed; the light-mode panel is now
     `lerp(background, settingsRoseAccent(), 0.07f)` with a hairline
     border (was the hard beige #E9DFD4 block).
   - SECONDARY CARDS = CARD DOODLES: Recycle bin / Updates / Help &
     feedback traded their plain white rows for compact members of the
     big-card family — tone gradients (STEEL / SAGE / LAVENDER),
     frosted icon tile, round arrow, and three NEW visuals: TRASH
     (ribbed tapered can + tilted lid + crumpled paper ball), REFRESH
     (two chase arcs with arrowheads around a version chip), CHAT
     (big question bubble with a drawn hook-and-dot ? + small reply
     bubble with ink lines). `SettingsDesignVisual` gained TRASH /
     REFRESH / CHAT; `SettingsSecondaryCard` carries tone + visual.
4. **ManageCategoriesScreen.kt — dragged row visible:** while dragging,
   the row swaps to a LIFTED CARD shell — `shadow(6dp)` → clip(14dp) →
   opaque fill (surfaceContainerHighest dark / #F7F1E6 light) → 1.5dp
   primary outline — with FULL alpha (the hidden fade suppressed) and
   zIndex 1, so the lane being held is always readable against the rows
   it slides over (shadow BEFORE fill per the shadow-order rule).

**Scope:** Android app only (web/ + desktop/ untouched).

**Status:** committed + pushed; CI validates. No pending prompt.

## Archive
Older completed request logs (2026-09-08 → 2026-09-10) were trimmed from
this file on 2026-09-10 to keep it short. They live in git history
(git log -p -- Prompt.md) if anything needs revisiting.

## next prompt

**QUEUED 2026-09-10 (user, after "continue"):**

1. After the current Everything-page + TextHistory work is pushed,
   redraw these shelf/collection doodles PROPERLY:
   - Cabinet collections "Curiying now" — the BOOK itself is bad;
     redraw just the book (keep the mug/etc).
   - "Saved entries" — redesign it properly.
   - "Completed" — keep it MINIMAL.
   - All the CUSTOM collection ones — do something UNIQUE for each
     (not the current generic scenes).
   - Settings "Help & feedback" — redraw its doodle too.
2. TOP NAV BAR animation: the smooth-transition animation is NOT smooth
   — when tapping a nav item it snaps to a solid color without the
   moving highlight; fix so the active-highlight glides to the tapped
   chip (no solid-color jump).
3. BACK GLITCH: if the user taps back MID-animation and goes to "All
   settings", the hub still shows the page that was open — make the
   back/close mid-animation smooth (finish/cancel the transition
   cleanly instead of leaving the old page stuck).

Status: queued — finish Everything page + TextHistory first, then push,
then follow this prompt.

# Prompt Log — current request

## Request (2026-09-12, IN PROGRESS — social layer: identity, profiles, the community wall and messages)

User (rephrased, in their words): the drawn profile avatar is not good and the
community page does not show an avatar at all; there is no way to open a
profile from a post or from a comment; a username change does not update the
names on older posts; the comments, the action buttons and the post flow look
bad; sharing forces you to type a topic by hand; remove the old Share a card
button and give me a floating plus that shares a TOPIC as a card, plus a share
to community option inside the share dialog; remove the community tab option
from Profile and put sign-in and the username in Edit Profile; add proper hint
texts about what is private and what is not; no em dashes and no lowercase
sentence starts; the username save flow says nothing when a name is invalid or
taken; the sign-in and create-account flow is bad (one screen, one button);
the card in the community feed is too big; the comments lack polish; Friends,
the inbox and a conversation all need a redesign (no reactions, no typing
indicator, no profile header in a conversation, messages do not persist and do
not sync fast); the message box rises far too high while typing in a friends
conversation; and the whole social UI lacks proper box cards and pill icons.
They asked for real research into social UI, a custom Curio look, and then a
detailed report of what we will do and what to borrow from other apps.

Follow-up instruction (same request): build it all, and "focus more on the ui
itself — i want beautiful ui and buttons" — account & profile, the Messages and
Friends redesign, the share flow, reactions plus a typing indicator (backend
included), a proper reply-with-mention in comments, and a friend flow that
knows when you are already friends. Asked whether Supabase stores messages
encrypted (it does not — `dm_messages.body` is plain `text`, readable by the
two participants only through RLS, so the product line stays "private, not
end-to-end encrypted"). Also reported a CI failure from the previous push and
asked for that to be fixed and pushed before anything else.

**Slice 1 — SHIPPED this commit (identity + profile, the root cause of five of
the complaints above).**

1. **`SocialAvatar.kt` rewritten.** The old renderer was four shapes cycling
   through four colours. It is now 16 genuinely DIFFERENT characters (beanie,
   bob plus glasses, curls, cap plus headphones, long hair plus earring, bead,
   pigtails, wizard hat, leaf crown, goggles, beret, top bun, freckles, hood,
   space helmet), each with its own ground, garment, skin and hair tones, all
   drawn on a shared 100x100 design grid with a soft inner rim, so an avatar
   reads the same at 24dp in a row and at 96dp on a profile.
2. **Identity is resolved LIVE (`CommunityApi`).** The stored `author_handle`
   is stamped at insert time by a database trigger, so on its own it freezes a
   member's name at post time. `feed` / `card` / `comments` now enrich every
   row through `withAuthors` / `withCommentAuthors`, which call
   `SocialApi.people(token, ids)` once per screen and fill `authorName` plus
   `authorAvatar`; `CommunityCard` and `CommunityComment` gained `authorId`,
   `authorName`, `authorAvatar` and an `authorLabel` accessor (live username
   first, snapshot second). That fixes BOTH the missing portrait and the stale
   username, and it is also why the exported share card now carries the
   current name.
3. **`SocialProfileScreen.kt` (NEW) plus the `person/{userId}` route.** The
   missing destination: portrait, username, an Add friend / Message /
   this-is-you action, a plain-language privacy line, and the member's live
   24-hour cards. It is opened from a card's author row, a reply's author, and
   the conversation header, and it is wrapped in `SettingsSharedScope` like the
   card view. It reads only the public half of `profiles` (username + avatar
   style) through the same `SocialApi.people` call the rest of the layer uses.
4. **The wall was rebuilt around that identity.** Each card is a box card now:
   the author's portrait and live username on top (tap opens the profile), the
   caption, the card art at 74 percent of the width so the wall reads as a
   wall, and one action row. The header Share a card button is GONE, replaced
   by a floating Share a topic button (ExtendedFloatingActionButton, cleared
   over the floating nav bar when Community is a tab root) plus a You shortcut
   into your own profile.
5. **The composer picks a TOPIC instead of a name.** A search box over
   `TopicJsonLoader.loadIndex()` (falling back to the warm lane pools) finds
   real topics; picking one supplies the name, lane, glyph and accent, so a
   card can never disagree with the topic it names. The manual LANE chips went
   away with the free-text field. The labels are clearer too (A caption above
   the card / The words on the card).
6. **Replies are cards now.** `CommunityReplyRow` renders the author's
   portrait and live username (both tap through to the profile), the words in
   a rounded card, and exactly one pill action: Remove on your own reply, Add
   on someone else's. `CommunityCommentsSheet` gained an `onOpenProfile`
   callback, wired from both hosts.
7. **Messages got a real header and an honest privacy line.**
   `DirectMessageScreen` used to be handed an EMPTY handle by the NavHost, so
   it always titled itself Message. It now resolves the other person itself
   (`SocialApi.people`) and shows a tappable header card with their portrait,
   live username and the hint: only the two of you can see this thread,
   messages are stored on Curio's server so they can be delivered, which means
   they are private but not end-to-end encrypted.
8. **The message box no longer over-lifts.** The screen had `imePadding()` on
   the composer WHILE the window also moved for the keyboard, so the box was
   lifted twice. The inset is now consumed once, at the root, with
   `WindowInsets.navigationBars.union(WindowInsets.ime)`, and the composer
   rides directly above the keyboard with no gap underneath.

**Verified statically** (Gradle is prohibited here): a delimiter-balance check
over all nine touched files, a symbol audit of every shared component the new
screen uses (`SettingsHeroHeader`, `SettingsOptionCard`, `SettingsOptionInfoRow`,
`curioDialogActionButtonColors`, `CommunityCardCanvas`, `SocialAvatar`), a
scope audit of the reply/author rows, and a construction audit proving
`CommunityCard` / `CommunityComment` are only built inside `CommunityApi` (so
adding required fields cannot break another call site). CI is the compile
check.

**Still queued for the next slice (the research + plan the user asked for is
in the reply that accompanies this commit):** the Friends list, inbox and
conversation redesign; reactions beyond a single like; a typing indicator;
local persistence plus faster sync for messages; splitting sign-in from
create-account with real validation messages; Edit Profile carrying sign-in,
username and portrait; removing the community tab option from Profile; the
share-to-community entry inside the share dialog; and the inline share mode
for text, quotes and paper notes.

## Request (2026-09-12, in progress — CAPTURE STUDIO: Save your take revamp)

User (verbatim intent, rephrased): *revamp "save your take" and ship it as an
Experiments option — make it fluid and greater, with good animations while
recording, a redesigned tools-bar picker (format colour etc.), a proper view
and a proper layout change — a full redesign of Save your take. Keep the core
feature (any take type in any topic) and do NOT touch the note-paper style of
the notes; everything else is mine to redesign better — better flow, UI, UX
and functionality, no confusion, and do not remove things. It is an experiment
toggle, so it must not affect the current flow; it should look nothing like
the current one. Add this prompt to Prompt.md (rephrased) as progress, and add
the next-prompt space too.*

**Read first:** `master.md`, root `AGENTS.md`, `app/AGENTS.md`,
`features/capture/*` (SaveCaptureScreen's state + save pipeline, the six
formats, the shared components), `AppPreferences` experiment plumbing and
`UserExperimentsScreen`.

**Design decision (why this shape).** The classic page stacks four chrome
bands (topic strip → format chips → take tabs → body) over the take. The
studio instead splits the page into **hero → canvas → tray**: the topic,
lane, session duration and mood move into a tinted hero; the canvas holds the
paper note and nothing else; the takes, the tools door and Save move to a
bottom tray (thumb reach). Format + mood + tags — three chrome bands —
collapse into ONE tools bottom sheet with a two-up format grid that describes
what each note actually captures.

**CRITICAL constraint honoured:** the studio is a SHELL, not a fork. It takes
the classic page's state and hands the classic page's own guards back as
callbacks (`onPickFormat` → the fill-then-confirm rule, `onRequestRemoveTake`,
`onAddTake`, `onSave` → the real `performSave`), renders the SAME
`FormatBodyForCategory` / `TagEditorRow` / `SessionNoteFloatingPill`, and the
classic page keeps every dialog. So the experiment cannot change what gets
saved, and the paper notes are byte-identical.

**Shipped (3 code files + docs, this commit):**

1. **`data/AppPreferences.kt`** — `captureStudioState` + `KEY_CAPTURE_STUDIO`
   + `is/setCaptureStudioEnabled` (default OFF, loaded at startup).
2. **`features/settings/UserExperimentsScreen.kt`** — a new **Capture**
   section with the *Take studio* switch.
3. **`features/capture/CaptureStudio.kt` (NEW)** — the workspace:
   - **Top bar** — back + title + one tools door (no competing bands).
   - **Hero** — lane medallion, topic, lane · session duration, and a mood
     chip that expands the shared `MoodChipsRow` INSIDE the card; while the
     active take is recording the chip is replaced by a pulsing "Recording"
     badge (`CaptureSectionState.busy`, so it is never a lie). One-shot rise +
     fade entrance.
   - **Canvas** — the untouched format body, the take's tags under it, and the
     floating session-note pill. Switching takes plays a fast scale/alpha flip
     driven by ONE `Animatable` (never composing two heavy boards at once).
   - **Tray** — the take RAIL (active pill springs larger and wears the
     accent; a recording take's pill swaps its glyph for a pulsing dot) plus
     the dock: the current format pill (opens the tools sheet) and the Save
     button, which pops once on `CurioMotion.Springs.Bouncy` when the take
     becomes savable.
   - **`CaptureToolsSheet`** — one settings-styled sheet (drag handle only, no
     close cross): the two-up format grid with per-card animated selection
     (accent fill + border + scale), the mood row and the tag editor. Every
     choice CLOSES the sheet before it acts, so a confirmation dialog can
     never animate in behind a leaving sheet.
4. **`features/capture/SaveCaptureScreen.kt`** — `if (!studioOn)` wrappers
   around the classic top bar and the classic strip/header (their declarations
   stay unconditional so the shared state keeps one home), and one branch at
   the body: `CaptureStudio(...)` (every callback expressed once, from the
   classic page's own rules) or the classic body + CTA. `TagEditorRow` gained
   a `modifier` parameter; the shared capture helpers are `internal` so the
   studio file can use them rather than duplicating them.

**Verification (Gradle is prohibited in this workspace):** delimiter-balance
check on both Kotlin files, an import sweep, and a symbol audit of every
shared component the studio calls (`Surface(onClick, interactionSource)`,
`MoodChipsRow`, `TagEditorRow`, `SessionNoteFloatingPill`,
`FormatBodyForCategory`, `rememberCurioPressSource`, `curioDarkGlow`,
`JournalMood.glyph`, `formatGlyph`, `shortName`, `formatSessionShort`) plus a
scope audit proving nothing in the classic body uses a variable declared
inside the wrapped chrome blocks. CI is the compile check.

**Scope:** Android app only (web/ + desktop/ untouched).

**Status:** committing + pushing now; CI validates. The experiment is OFF by
default, so the classic page is what every user sees until it is flipped on.

## Request (2026-09-12, in progress — settings rail label, appearance gates, pickers, reminder clock, fetch consent, Cabinet collections)

The previous queued prompt (settings rail active-pill label, material-hero-tears
removal, hero/adaptive gating under Material, pet-designer greying, search +
music pickers as settings-styled sheets, the daily-shuffle clock, recordings
defaulting to High, the merged cover-fetch consent, and the Cabinet collection
art redraw) was completed and **pushed by the user manually** before this
request landed, so it is no longer open here.

## Request (2026-09-12, in progress — social composer and code-only identity)

User requested a floating Community composer that chooses message or card,
direct quote/note posting from Topic Reveal, sorting, openable social profiles,
notification/mute controls, long-press reactions, improved comments/replies,
and a choice of 10–20 code-only social avatars with no photo uploads.

**Completed in this slice:** the account page now offers 16 code-drawn social
avatar styles. The chosen style is persisted locally and saved to `profiles`,
and Friends/DM person badges render the style returned from the public profile
instead of a generic letter disc. `profiles.avatar_style` is constrained to
0–15 in the idempotent schema. No image column, upload path, or user photo is
introduced. The remaining Community composer, topic-reveal posting, sorting,
profile, notification/mute, and reaction work is queued for the next slice.

## Request (2026-09-12, in progress — restore Curio social headers + social reliability)

User rejected the pixel header change. Keep the established Curio headers and
redesign the Community/Friends/DM content itself instead. They also requested
direct posting from Topic Reveal's share-card sheet, cached social page loads,
message notifications and muting, long-press reactions, improved comments and
reply UI, proper add-friend icons, and correctly updating usernames.

**This commit:** restores the original Curio headers exactly and adds a signed-in
account username editor. Existing friendships resolve profile identity on each
load, so no friend needs to be removed/re-added: save the username, then reopen
or refresh Friends after the server schema has been re-pasted. The larger social
content/UI, notifications, mute, reaction and direct-posting work remains the
next implementation slice; it needs coordinated API/schema and UI work rather
than a header-only substitution.

## Request (2026-09-12, in progress — social hub revamp + security audit)

User asked for a complete nostalgic-pixel visual revamp of the independent
Community, Friends and Direct Messages surfaces; a Profile entry point for
account login; a Settings-only Community toggle; correct friend names; richer
social actions; and an exploit/security audit.

**Plan:** (1) audit the existing Supabase RLS and client identity handling,
(2) replace settings-family social chrome with a dedicated, solid-fill pixel
social shell, (3) surface online account/community entry points in Profile,
(4) fix identity fallbacks so routes never display a generic exploration label,
(5) statically validate only (Gradle is prohibited here), update release notes,
then commit/push and open a PR.

**Completed:** The Community, Friends and DM screens now use a dedicated solid
arcade/pixel social header rather than the Settings hero. Profile restores the
online session and gives the account a Community entry point: signed-out users
land at sign-in; signed-in users enter the independent hub when Community is
enabled. Identity fallback now uses the username before generic copy, fixing
the incorrect explorer-style friend labels. The security audit found two
defence-in-depth gaps: a modified client could forge community author handles,
and social read policies did not enforce the Online Mode consent gate. The
schema now stamps handles from the authenticated profile on card/comment insert
and requires Online Mode for friend/DM reads. The handle trigger has no public
execute grant. Static checks passed; Android Gradle validation is prohibited in
this environment. The SQL still needs to be re-pasted in the Supabase SQL
Editor to apply the server-side protections.

**Status:** ready to commit, push and open a PR.

## Request (2026-09-11, in progress — Cabinet loading skeleton + settings rail label + collections redraw)

User (queued prompt): "the cabinet loading is still kinda bugged the saved
entryies now open fast but cabinet opening is bad, also use a skeleton system
maybe for accurate amount exactly the number of saved entries are there like
that..and redraw the collections cutsom ones and curying now one do it
properly and also the settings nav rail the indicator istelf does goes fast
and its better but the text disappers for a momemt for the acive indicator
and it slowly comes to a rest so ix that". Earlier in the same thread: "do
what u can do then after doing it ask me questions and doubts".

**Diagnosis**

1. **Cabinet opening.** The V2 Cabinet home treated "entries is still empty"
   as "you have nothing", so a cold open flashed the
   suggestions/empty state and then swapped in the real shelves — the same
   class of bug the classic Cabinet already guarded with `entriesReady`, but
   V2 had no such gate for its home/shelf levels. And the loading state it
   did have was four generic full-width boxes, nothing like the 2-column
   card grid.
2. **Settings rail.** The active chip's label is cream; the chip painted NO
   fill of its own (the accent pill was only the shared-element overlay, and
   that instance is hidden while the glide runs) → cream on the pale
   frosted tile until the pill landed. The 0.9-damped spring also overshot,
   so the pill visibly "came to rest" after arriving.
3. **Collections redraw — AMBIGUOUS.** "custom ones and curying now one do it
   properly" has several readings (the collection pages' member layout, the
   collection CARDS on the Cabinet home, the empty state, or the untracked
   `Curio_WantToRead_EmptyState.jsx` concept). Per the root AGENTS.md
   ask-when-unsure rule (and the user's own "then ask me questions"), the
   redraw is queued as a question instead of guessed at.

**Shipped (5 code files + docs, this commit):**

1. **`ui/components/CurioSkeleton.kt` (NEW)** — the app-wide skeleton
   system: `CurioEntrySkeletonCard` (20dp radius, 96dp hero header, two text
   bars, shimmer read in the draw phase so it never recomposes) and
   `CabinetEntrySkeletonGrid(count, topInset, wide)` which mirrors the real
   grid's columns/paddings/gaps.
2. **`AppPreferences.kt`** — `getCabinetEntryCount` / `setCabinetEntryCount`
   (key `cabinet_entry_count`): the last known saved-entry count, so the
   skeleton is sized to the real archive on the next cold open.
3. **`CabinetV2Content.kt`** — `archiveReady` (false until the first
   `observeLight()` emission) gates `showSuggestions`, and the home's
   Saved-entries slot plus the Saved entries / Notes shelves hold
   `v2SkeletonItems(skeletonCount)` until then; the count is persisted for
   the next launch.
4. **`CabinetScreen.kt`** (classic) — the four wide boxes became
   `CabinetEntrySkeletonGrid`, sized from the persisted count.
5. **`SettingsHubScreen.kt`** — the active chip paints `SettingsRailAccent`
   in its own layer (identical to the gliding overlay, so the glide is
   unchanged once it lands) and the bounds spring is critically damped
   (1.0 / 420) so the highlight lands and stops.

6. **Switch haptics completed (v3xx51)** — the previous task's last open
   item ("extend press feedbacks and haptics for sheet toggles etc"): the
   shared `rememberCurioControlTick()` now also wraps the switches outside
   the settings family — Bug report's crash-log toggle, Onboarding's
   reminder / explore-bubble / pastel toggles, the Pet designer's element +
   face/reaction toggles, the reveal dialog's bubble opt-in and the share
   sheet's four toggles. (The sheet's own drag handle + the ripple→pressed
   look landed in the earlier commits; no sheet-open buzz was added because
   it would fire on programmatic opens too.)

**Open questions for the user (asked at the end of this turn):** what
"redraw the collections ... do it properly" should change; whether the
built-in starter shelves should keep their destructive "Delete collection"
menu entry (deleting one currently loses its members for good); whether the
collection pages' member list should become the concept's row list.

**Status:** committing + pushing now; CI validates.

## Request (2026-09-11, completed — screen-reveal transitions toggle, packed Cupboard shelves, .jsx text-history tree, caption box + cover-true album sheet)

User (queued prompt + ask_user answers): test results "Haven't built it yet";
the album sheet's colours are wrong "from the album's real cover art"; the
Cupboard wall leaves empty space ("the first image is good and then to its
side there are 2 things placed but then theres space left below those 2 so
properly fix the spaces"); text history should match the concept `.jsx`
("Tree view layout, Version cards' content, Replace Changes with Compare,
also make the current text at the top … show its changes in the top"); the
caption field "is note paper style change it to just a text box"; the book
sheet's add-note box + text colours are still wrong in dark mode; and
"similar to the dark mode and light mode transition cant we use that for
like settings or profile open, and the screen changes in that transition
style, make it a toggle and also make it faster a little" (plus a wider
animations/haptics wish-list logged as pending).

**Shipped (7 code files + docs, this commit):**

1. **`navigation/CurioRevealNav.kt` (NEW) + `ThemeTransition.kt` +
   `CurioNavHost.kt` + `AppPreferences.kt` + `ExperimentsScreen.kt` —
   SCREEN REVEAL experiment (Settings ▸ Experiments, default OFF). The
   theme flip's feathered iris now opens screens too: the frame is captured
   on pointer-DOWN (`trackRevealTaps()`), stashed, and played from the
   NavController's destination listener via the new non-suspend
   `startTransitionWithFrame`; while it owns a navigation the NavHost
   returns `EnterTransition.None`/`ExitTransition.None` so the iris is the
   only motion. 440ms (vs the flip's 680ms) + a 32ms settle, both restored
   after the reveal. Shared-element routes (`reveal`, `pet-designer`) opt
   out; no fresh frame = the normal transitions, untouched.
2. **`CabinetV2Content.kt` — Cupboard wall packed into shelves.**
   `BoxWithConstraints` measures the wall; `buildCupboardShelves()` packs
   covers into rows at one shared height (width = height x the cover's own
   aspect), so widths + gaps fill the width exactly and no hole is ever
   left. The 8-column grid span cycle + `mediaTierIndex` are gone.
3. **`ui/components/TextHistory.kt` — the `.jsx` tree.** `LineageRail`: one
   rail, each version a node card, newest first (current at the top with
   its badge), sessions newest-first, and a node tap opens the word-level
   COMPARE against the version before it — the inline changes list and its
   +/− counts are removed.
4. **`CaptureFormatComponents.kt`** — `PaperLineField(paper = false)` is a
   plain text box (theme `surface` + hairline outline).
5. **`TopicRevealScreen.kt`** — the book sheet's "Add a note…" placeholder
   drops its 0.85-alpha fade (dark-mode readability); `AlbumNotesSheet`
   keys its palette off the AUTHORED cover first, so the sheet matches the
   poster's art.

**Scope:** Android app only (web/ + desktop/ untouched; the user's untracked
concept `.jsx` files were left untracked).

**Status:** committed + pushed; CI validates.

**CI fix (second commit, `4b774680`):** four compile errors in the first
push — a missing `LazyListScope` import in `CabinetV2Content` (which cascaded
into unresolved `item` / `animateItem` plus the tween/spring inference
failures inside the item lambda), `Int * Dp` in the settings rail's initial
offset, a delegated `paletteUrl` that cannot smart-cast for the album sheet's
swatch-cache write, and an `Int` constant assigned to the `Long`
`revealSettleDelayMs`.

**Press feedback + haptics (third commit, v3xx46):** new
`ui/components/CurioPressFeedback.kt` — `Modifier.curioPressClickable` squishes
on `CurioMotion.Springs.Press`, fires one light haptic on the DOWN edge only,
and keeps the ripple via `LocalIndication.current`; the scale is a parameter so
a chip (0.96) and a full-width card (0.985) both read as a press. Applied to
`SettingsOptionRow` (every settings-family row), the hub's tone + secondary
cards, the quick-tool chips, and Home's saved / pinned rows.

**Fourth commit (v3xx47, NOT pushed — user said "dont push this"):** the user
clarified that by "selection highlight" they meant the **TOUCH highlight**, not
the selected state ("for all around the app not the pressed or selected but the
touch … maybe with animation or a pressed look"). New
`ui/theme/CurioPressIndication.kt` — an `IndicationNodeFactory` that paints the
touched element into an offscreen layer and tints it with `BlendMode.SrcAtop`
(in 90ms, out 260ms, `onSurface` @ 0.14), so the press wash only lands on the
pixels the element already drew and follows its own rounded shape. Provided at
the `CurioTheme` root via `LocalIndication`, replacing Material's ripple
app-wide (bottom sheets included). Trade-off: a background-less label (a bare
`TextButton`) now only tints its glyphs instead of getting a circle.

**Fifth commit (v3xx48):** the user pushed the local commits and asked for:
reveal faster + tappable mid-transition; the glass header reaching the status
bar (Home + Profile, glitchy on Profile) and its extended area actually
collapsing ("it stays in its initial size where the stats was") — also without
liquid glass; the Cupboard's 3x/0.5x size variety back; the category-switch
animation back; and the text-history list-view CURRENT pill fixed.
Shipped: (1) `CurioGlassToolbarMorph` clip moved OUTSIDE the size-reporting
`.layout {}` so the glass is trimmed to the animated height instead of
painting the full hero; (2) screen reveal 440→300ms, settle 20ms, and the
frozen frame no longer consumes pointer events; (3) `buildCupboardShelves`
cycles four shelf heights (0.50/0.28/0.34/0.21 × wall width) and a filter
switch fades the wall (`wallSwap` Animatable) since packed shelves can't slide
covers to new slots; (4) `HistoryCurrentPill` wears the theme accent with a
rim and never wraps, and the list row's time is flexible so the pill can't be
clipped.

**Still pending (user's wish-list):** the rest of the animations pass — motion
specific to navigation and bottom sheets beyond the page transitions already
refined, further glass-header morph polish (it collapses correctly now; more
needs their eyes on it), and haptics beyond the surfaces above (the nav bar
already ticks). The user's build result for this batch is still outstanding.

## Request (2026-09-11, completed — settings rail highlight speed/feel + text-history tree badges)

User test results (ask_user): "All landed well" for the Cabinet/header batch;
next up selected: **Animations refinement pass**, **Text history tree**, and the
concrete report "the active indicator in settings top rail too slow and feels
broken".

**Shipped (2 code files + docs, this commit):**

1. **SettingsHubScreen.kt — the rail highlight lands with the tap.**
   `SettingsRailBoundsTransform` 0.8/140 → **0.9/320** (~350ms → ~200ms
   settle: responsive, still a visible glide rather than the old stiffness-500
   instant snap). The "feels broken" part was the POST-COMPOSITION centring
   correction: `LaunchedEffect(active)` + `scrollToItem` snapped the whole
   header sideways one frame after every section switch, right under the
   gliding pill. The rail geometry is fixed (82dp chip + 7dp gap), so the
   centred offset is now computed up front from
   `LocalConfiguration.screenWidthDp` and passed as
   `initialFirstVisibleItemScrollOffset` — the row composes already centred and
   nothing scrolls after composition (the effect is gone).
2. **TextHistory.kt — tree refinements.** (a) A version node's +/− badge now
   compares against the FIELD's previous snapshot across session boundaries
   (running `prevEntry` in `HistoryFieldCard`) — a new session's first node
   used to report its whole text as "+N". (b) The version connector Box is a
   fixed 18dp box instead of `fillMaxHeight()`, which measured 0 height inside
   an unbounded LazyColumn item (the stub + dot drew outside their own
   bounds).

**Scope:** Android app only (web/ + desktop/ untouched).

**Status:** committed + pushed; CI validates.

**Remaining:** the broader ANIMATIONS pass needs specifics — I fixed the
highest-signal animation complaint (the rail). More text-history tree
refinements are queued for the user's next specifics.

## Request (2026-09-11, completed — Favorites = liked topics, Everything → Cupboard, stable wall, instant Cabinet, glass header to the top + a real collapse)

User (ACTIVE 2026-09-10, after a CI failure was pasted in):

1. COLLECTIONS: the shelf name is already in the page header but the
   card/shelf repeats it below → remove the duplicate; AUDIT every collection.
2. FAVORITES vs EVERYTHING are basically the same — Favorites should show the
   FAVORITE TOPICS (the topics you liked), not the saved entries; Everything
   shows only books / series / albums.
3. RENAME Everything → "Cupboard" and design the page properly.
4. The Cupboard wall SHIFTS its size (tiles resize when it reflows) — fix.
5. Fix the workflow too (add / save flow into the renamed page).
6. CABINET saved entries still doesn't load INSTANTLY — it shows a loading
   state; fix the loading properly.
7. PROFILE + HOME glass header: the top doesn't reach the status bar — fix.
8. Glass header morph-collapse: the expanded look doesn't COLLAPSE — fix.
9. Commit and push everything.

Queued follow-up (same file, later): "use ask user for test results … more
refinements in animations and all more refinements in the text history tree
too … the add a caption field is note paper style change it to just a text box
… in dark mode the book bottom sheet is still inaccurate colors the box and
text the add note one … and the albums colors are fully different fix them."

Answered by the user before implementing (ask_user): Favorites = topics you
liked (the reveal heart); Cupboard's Add = search the whole catalog; the book
box = the book sheet's "add note" field; the albums complaint = the ALBUM
BOTTOM SHEET's colors.

**Shipped (7 code files + docs, this commit):**

1. **CabinetV2Content.kt**
   - **No duplicate shelf names:** `V2DetailHeader` lost its name/count block
     (the pinned hero already prints the collection name + item count) and its
     `name`/`count` params — it is the actions strip now (Add pill + kebab,
     end-aligned). `v2VirtualShelfItems` dropped its `v-head` section header
     entirely (same duplication on Saved entries / Notes) and its unused
     `onAdd` param. Audited the rest: the home page's section headers
     ("Saved entries", "Collections") label page SECTIONS, not the page title,
     so they stay.
   - **Favorites = the liked-TOPIC shelf:** new `likedTopics` reads
     `AppPreferences.topicSentimentsState` (SENTIMENT_LIKE, key
     `CATEGORY:topicId`), resolves each id against the warm lane pools
     (`TopicJsonLoader.cached`, behind a `catalogReady` poll capped at ~3s so
     a broken install can't hold skeletons forever), dedupes + name-sorts.
     Rendered by the new `v2LikedTopicItems` / `V2LikedTopicRow` (category
     glyph tile + name / byline / lane, opening the topic's real reveal).
     `shelfCounts[FAVORITES]` + the hero subtitle now count liked topics.
   - **Everything → CUPBOARD:** hero title/subtitle ("Books · albums ·
     series"), the home card (title, "Every book, album and series you keep",
     liked-only count, empty-rail copy, a11y description), the no-match state
     and the home fallback line ("Collections · Cupboard · your keepsakes").
     The internal level key stays `"everything"` to avoid saveable churn.
   - **Stable wall sizes:** `mediaTierIndex` maps `KIND|NAME` → the item's
     position in the FULL name-sorted media list; `v2EverythingMasonryItems`
     takes it and derives each cover's tier from that stable position instead
     of its filtered rank — filtering re-flows the wall without resizing a
     single cover.
   - **Cupboard Add = catalog search:** new `V2CupboardAddSheet`
     (`showCupboardAdd`) searches the BOOKS / ALBUMS / SERIES lane pools
     synchronously and toggles the picked media's favorite (`toggleLikedFavorite`)
     — pinning IS liking, so the wall updates live; an empty query lists what
     is on the wall (the sheet doubles as the manager). The old
     Add-to-Favorites sheet mode is now unreachable (the reveal heart owns
     topic liking).
2. **CaptureRepository.kt** — `lightSnapshot` (`@Volatile`, written from
   `observeLight`) + `peekLight()` for a synchronous first frame.
3. **CabinetScreen.kt** — `produceState` seeded from `peekLight()` plus
   `entriesReady` (flipped by the first real DB emission); a cold first frame
   renders quiet card skeletons instead of the "Your Cabinet is empty" doodle.
4. **CabinetV2Content.kt** — same `peekLight()` seeding.
5. **CurioGlassToolbar.kt** — the glass reaches the status bar:
   `statusBarsPadding()` moved off the bar and onto its content (the leading
   Row, the morph bar's full Column, the compact Row); the morph bar's
   collapsed target is now `compactH + WindowInsets.statusBars.getTop()` so
   the compact row is never clipped.
6. **HomeScreen.kt / ProfileScreen.kt** — the morph header really collapses:
   the collapse clock is hoisted above the scroll content
   (`homeStickyProgress` / `profileStickyProgress`) and drives the reservation
   spacer too (`lerp(full, compact + inset, eased)`), so the page rises with
   the bar; `ProfileHero` gained a `reserveHeight` param (default
   `ProfileHeroTotalHeight`).
7. **Queued polish** — GalleryWall caption is a plain text box
   (`PaperLineField(paper = false)`; the per-field paper values still feed the
   board's quote cards + the saved payload); `ChapterNoteField` wears the
   sheet's `surfaceHigh` / `onSurface` / `onSurfaceVariant` + a 1dp hairline
   instead of ink-alpha washes; `AlbumCoverPoster` gained `resolvedUrl` and the
   album notes sheet seeds it from its palette URL so the poster and the
   sheet's colors can never disagree.

**Scope:** Android app only (web/ + desktop/ untouched).

**Status:** committed + pushed; CI validates.

**Remaining (user's own follow-up list):** more animations refinements, more
Text-history TREE refinements, and the test results they will report.

## Request (2026-09-10, completed — shelf art final pass: book/saved/completed/customs + help&feedback; nav rail highlight glide + back-glitch fix)

User (queued after the Everything/TextHistory push): "redraw cabinets
collections curying now the book itself is bad just the book, the saved
entries properly redesign that, completed too keep it minimal, and all the
custom ones do something unique, and the help and feedback one from
settings, and also the top nav bar that smooth transition animation isn't
that smooth when i tap something it goes solid color without that
highlight so fix that and a glitch when if mid animation i tap back and
go to all settings it still stays in that page it was open so make it a
little bit smooth."

**Shipped (3 code files + docs, that commit):**

1. **CabinetShelves.kt — art final pass:** the Curiying-now BOOK redrawn
   (dark cover slab under three stepped page layers, typed paragraph lines,
   spine crease + knotted ribbon; mug kept), SAVED ENTRIES became a collage
   (big front polaroid + washi tape + star mark + two tilted prints behind),
   COMPLETED kept minimal (one summit + snow tip + planted flag), and the
   custom arts became unique scenes (balloon, planet, sailboat, kite).
2. **SettingsHubScreen.kt — Help & feedback + rail animation:** the chat
   doodle became a support-chat window; `SettingsRailBoundsTransform`
   0.95/500 → 0.8/140 so the highlight visibly glides; rail centering is
   instant (`scrollBy` after one frame).
3. **CurioNavHost.kt — settings-family transitions are pure tweens:**
   enter/exit/popEnter/popExit dropped the `scaleIn/scaleOut` spring for
   `fadeIn/fadeOut(tween(Morph))`, fixing both the solid-colour jump and the
   back-mid-animation stuck page.

**Status:** committed + pushed.

## Request (2026-09-10, completed — Everything wall: no seam + uniform covers; Text history gets the JSX concept feature set)

User: "the cabinets arrangement isn't good — remove the background fill, and
the covers are getting different shapes but I want them to have the shape they
are in and arrange in that shape… they can be 2x 3x .5x etc but in both width
and height… also you see this jsx — it has better text history features and
options — implement all of the features of text history properly in detail."

**Shipped (2 files + docs):** the Everything wall lost its seam plate and the
tiers became uniform scales on an 8-column grid (0.5x → 3x in both
dimensions); TextHistory.kt gained search, filter chips, word-level COMPARE,
collapsible field groups, CURRENT pills, +/− word deltas and preview stats.

**Status:** committed + pushed.

## Request (2026-09-11, completed — CI break on v0/fix-settings-compose-import: SupabaseClient parseSession)

User pointed at the branch and pasted the CI log:
`SupabaseClient.kt:48:36 Inapplicable candidate(s): fun parseSession(body: String)`
on both compile tasks.

**Cause:** `refreshSession` called `execute(request).let(::parseSession)`, but
`execute` is a `Unit` helper — `::parseSession` takes a `String`, so the
reference could never apply. `refreshSession` therefore never parsed the token
response (it would also have returned a Session built from a Unit).

**Fix (1 line):** call `executeBody(request).let(::parseSession)` — the same
body-returning helper `authRequest` already uses, so the refresh token flow
parses the access/refresh/user fields it needs.

**Also (same branch, same root cause as the earlier rail fix):** the branch's
`eb740255`/`85a3b1c2` read the rail's `matchParentSize` failure as "API
unavailable" and swapped it for `fillMaxSize()`. `matchParentSize` is a
`BoxScope` extension, so the real problem was the lambda having no receiver.
Restored `.matchParentSize()` and declared `chipContent` as
`@Composable BoxScope.(Boolean) -> Unit` — identical to the fix already on
`main`. This matters because `fillMaxSize()` in a bounded host stretches the
82dp chip: `WidgetEditorScreen` (and other drill-in pages) host the rail in a
plain `Column(fillMaxSize())`, so a chip would fill the whole remaining page
height; `matchParentSize` sizes the content to the chip's own Box instead.
This leaves `SettingsHubScreen.kt` byte-identical to `main`, so the merge
takes it cleanly.

**Status:** committed + pushed.

## Request (2026-09-11, completed — CI compile break: `matchParentSize` in the rail chip)

User pasted the CI failure: `SettingsHubScreen.kt:2706:30 Unresolved reference
'matchParentSize'` on both `compileDebugKotlin` and `compileReleaseKotlin`.

**Cause:** the settings rail's `chipContent` lambda was declared as a plain
`@Composable (Boolean) -> Unit`, but its body calls `Modifier.matchParentSize()`
— a `BoxScope` extension. A plain lambda has no `BoxScope` receiver, so the
call could not resolve (even though both call sites sit inside `Box` content).

**Fix (1 line, `SettingsHubScreen.kt`):** declare the lambda as a `BoxScope`
extension — `@Composable BoxScope.(Boolean) -> Unit`. Both call sites (the
shared-element `Box { chipContent(true) }` and the fallback `chipContent(selected)`
in the outer `Box` content) already have a `BoxScope` receiver in scope, so the
receiver resolves at both and the chip keeps filling its parent Box.

**Status:** committed + pushed (main).

## Request (2026-09-11, completed — Supabase plan audit: what is actually built, and making the new secrets count)

User asked how far the 6-item Supabase plan has been implemented, noted the
repository secrets for `SUPABASE_URL` + the publishable key were just added,
asked for item 1 to be re-verified, and listed the plan: (1) Supabase client
support via the version-catalog path, (2) auth/session data layer + Online
Mode UI, (3) Room capture sync, (4) 24-hour text share cards + Community
interactions, (5) schema/RLS security, (6) verification.

**Verified against the branch (nothing guessed):**

1. **PARTIAL.** `data/SupabaseClient.kt` + `data/SupabaseSessionStore.kt`
exist and compile (CI error fixed this branch). Gradle wiring is sound —
`libs.com.squareup.okhttp3.okhttp` is a real catalog alias (okhttp 5.3.2,
gradle/libs.versions.toml:110) and only the URL + publishable/anon key are
read (`build.gradle.kts:79-90,131-132`); the service-role key is never
referenced, so nothing privileged can ship in the APK. Deviations from the
plan: it is a hand-rolled OkHttp/JSONObject client, NOT the Supabase Kotlin
client, no catalog alias was added for it, and the files sit in
`com.curio.app.data` rather than a `data/supabase` package.
2. **NOT STARTED.** `AppPreferences.isOnlineModeEnabled/setOnlineModeEnabled`
exist but are called from NOWHERE (no settings row, no profile surface, no
auth screen, no nav route — `CurioRoutes` has no auth entry).
3. **NOT STARTED.** No sync/upload code references Room captures.
4. **NOT STARTED.** No Community feed, no share-card upload, no expiry logic.
5. **NOT VERIFIABLE FROM HERE.** No SQL/migrations in the repo, and no local
Supabase access, so profiles/captures/share-cards/reactions tables and RLS
cannot be inspected; the SQL still needs to be run/pasted.
6. **CI could not verify anything** — see the finding below.

**Finding (the real blocker):** both workflows exported `KEYSTORE_*`,
Google Books, LibraryThing and Spotify to Gradle but **never**
`SUPABASE_URL`/`SUPABASE_PUBLISHABLE_KEY`, so the secrets the user added
reached nothing and every CI/release APK baked an empty
`BuildConfig.SUPABASE_URL` (`SupabaseClient.isConfigured` = false — Online
Mode could only ever report unconfigured). Also, PR #110 sat
`CONFLICTING` against `main`, and a conflicting PR gets no `pull_request`
run — 0 workflow runs exist for the pushed commit.

**Shipped:** `android.yml` + `release.yml` now export `SUPABASE_URL` and
both key names (`SUPABASE_PUBLISHABLE_KEY`, falling back to
`SUPABASE_ANON_KEY`) with a non-secret warning when absent; the contract is
recorded in `.github/AGENTS.md` (service-role key stays out of every build).
`origin/main` was merged into this branch and the `Prompt.md` conflict
resolved by keeping both entries, which clears the PR conflict so CI can run.

**Status:** committed + pushed. Follow-up work (auth UI, sync, community) is
tracked in the entry below; the branch is NOT merged to `main` (user
directive).

## Request (2026-09-11, completed — plan item 2: the auth/session layer + Online Mode UI)

User: "don't merge it to main yet, continue."

**Shipped (branch `v0/fix-settings-compose-import` only):**

1. **`data/supabase/` package** (the plan's stated location): `SupabaseClient`
   + `SupabaseSessionStore` moved there via `git mv` (package line updated,
   no external references existed).
2. **`data/supabase/OnlineAccount.kt` — the auth/session layer:**
   observable `state` (session / busy / error / notice) that the UI reads,
   plus `restore`, `signIn`, `signUp`, `signOut` and `setOnlineMode`.
   Sign-in stores the session and turns Online Mode on; sign-out always
   clears locally and turns Online Mode off; a sign-up that needs email
   confirmation reports a notice instead of pretending to sign in.
   `onlineAuthMessage` keeps rate-limit / unconfirmed-email / bad-credential
   failures actionable and collapses everything else to one generic line, so
   a raw response body can never reach the UI.
3. **`features/settings/OnlineModeScreen.kt`** (route
   `CurioRoutes.SETTINGS_ONLINE`): the account form (frosted email + password
   fields matching the family's search-field surface, reveal toggle,
   rose `Sign in` pill + `Create account`), the signed-in account row +
   `Sign out`, and the **Online mode** switch that states the text-only
   contract (photos/audio/screenshots never leave the device) and is only
   live while signed in. Built from the shared settings components
   (`SettingsHeroHeader` + pinned phone hero, `SettingsNavRail`,
   `SettingsOptionCard`/`Row`/`InfoRow`/`SwitchRow`, Playfair section
   headings), so it reads as part of the settings family.
4. **Wiring:** route constant, `CurioNavHost` composable inside
   `SettingsSharedScope`, hub design card (*Your data & privacy*), settings
   rail chip ("Online"), hub search row + deep-index row.

**Docs:** `app/AGENTS.md` gained the *Online layer (Supabase)* contract and
the child index entry; new `data/supabase/AGENTS.md` child doc (public
credentials only, no media upload, offline-first, UI-safe errors); the
current store changelog gained the user-visible ADD line.

**Static verification (this workspace has no Android SDK, so no Gradle):**
`git diff --check` clean, delimiter balance + an unused-import sweep over the
new files, and a symbol audit that caught two real breaks — `CurioIcon`
needed its own import (only `CurioIcons` was imported) and the rail chip's
`BoxScope` receiver. CI on the branch is the authoritative compile check.

**Status:** committed + pushed (branch only, no merge to `main`).

## Request (2026-09-11, in progress — plan items 4+5: the schema, the 24-hour cards and the Community feed)

User: "Build the 24-hour text share cards and the Community feed on top of the
new schema."

**Order of work:** the schema had to exist first (it did not — nothing SQL was
in the repo), so the backend script went in with the feature.

**Shipped:**

1. **`supabase/schema.sql`** (new, with its own child AGENTS.md): idempotent
   script for `profiles`, `cloud_captures`, `community_cards`,
   `community_reactions`, `community_reports` + RLS policies + indexes + the
   `curio_purge_expired_cards()` sweep + a PASS/FAIL self-check block. Key
   choices: RLS is the boundary (policies for `authenticated` only, zero
   `anon` policies, no service-role key anywhere); identity columns default
   to `auth.uid()` so the client never sends one; community reads AND writes
   require Online Mode on for both the reader and the card's author; the
   insert policy refuses a lifetime over 25 hours; `expires_at` defaults to
   now() + 24 hours and every read filters `expires_at > now()`.
2. **`data/supabase/CommunityApi.kt`**: the feed (live cards newest-first,
   likes embedded through PostgREST so one request carries counts + whether
   you liked it), post, like (idempotent), unlike, report (one per card per
   user) and author delete, with `draftProblem()` + `communityMessage()`
   keeping every failure safe to render. `CommunityCardDraft` has no field
   for an image, audio or screenshot — text-only is structural, not a rule
   the UI has to remember.
3. **`features/community/CommunityScreen.kt`** (route `CurioRoutes.COMMUNITY`,
   reached from the Online mode page's *Community* row): the 24-hour wall,
   each card rendered by the real `TopicShareCard` scaled into the feed
   width, with like / report / take-down, an hours-left label, and the
   composer sheet (topic + words + lane + style, caps + validation).
   Eligibility gates first: not configured, not signed in, or Online Mode
   off → a locked card that opens Settings → Online mode.
4. **`SupabaseClient.updateOnlineMode` is now an UPSERT** (`on_conflict=id`,
   merge-duplicates). It was a PATCH, and a brand-new account has no
   `profiles` row — so the switch looked on in the app while the server still
   refused every community call. `requestBuilder`/`executeBody` became
   `internal` so the community layer shares the one HTTP client.

**Docs:** `supabase/AGENTS.md` (new) + root AGENTS.md child index;
`app/AGENTS.md` online-layer section extended with the community contract;
store changelog ADD line.

**Verified statically** (no Android SDK here): delimiter balance + unused-import
sweep on all new files, `git diff --check`, and signature checks for every
shared component used (`SettingsOption*`, `TopicShareCard`, `ShareCardStyle`,
`ShareCardAspect`, `CurioCategories.byId`). CI on the branch is the compile
check.

**Still open:** the reveal-page share sheet does not yet offer "post to
community" (the API is ready for it — that is the next slice), and the SQL
must be pasted into the Supabase dashboard by the user (no project access
here).

**Status:** committing + pushing (branch only, no merge to `main`).

## Request (2026-09-11, in progress — community hub refinement: captions, sharing, the card view and replies)

User: "dont push it yet properly refine things, and did u redraw things i asked,
also proper buttom sheet comments style etc and what features does the
community hub have where to acess and is it always on". Their earlier answers:
the built-in collection cards to redraw are Favorites / Curiying now /
Want to Read / Saved entries / Completed (NOT Notes, Personal or the custom
balloon art), the starter shelves keep their destructive delete, the member
lists keep their current cards — and the Community hub is the priority.

**Shipped this turn (locally committed, NOT pushed — user asked to hold):**

1. **Captions.** `community_cards.caption` (≤180 chars, DB check constraint),
   posted from the composer's new "Your line above the card" field, shown
   above the card in the wall and in the card's own view.
2. **Replies in a proper bottom sheet.** New `CommunityCommentsSheet` — one
   sheet used by BOTH the wall and the card view: drag handle, swipe-down or
   back to close (no close cross, per the app's sheet rule), composer riding
   above the keyboard, the reply list capped with `heightIn` (a weighted
   scrollable inside a sheet's wrap-content column is measured with an
   infinite max height and crashes). `community_comments` table + RLS: a
   reply is visible only while its card is live and both sides have Online
   Mode on, only its author may delete it, and it cascades with the card so
   nothing outlives the 24 hours. Schema additions are idempotent
   (`alter table … add column if not exists` + a guarded constraint block),
   so re-pasting the file upgrades an existing install.
3. **The card's own view** (`CommunityCardScreen`, route
   `community/{cardId}`, opened by tapping a card): the full card at the
   feed's width, the poster and remaining life, and the actions — like,
   comment, **share as the same PNG the reveal page produces**
   (`shareComposableCard` + the FileProvider authority), report, and take
   down your own.
4. **Feed polish:** captions render above each card, the comment action
   shows the live reply count and opens the sheet in place, and the whole
   card is tappable into its view.

**Slice 2 — CI repair (shipped as `c71681a4`).** The slice-1 push failed to
compile: `animateColorAsState` was imported from the wrong package in
`CaptureStudio`, `Surface` and `LauncherEffect` imports were missing from
`CommunityScreen`, and a mangled line break in `DirectMessageScreen` broke the
ime padding plus a `let` chain. All four fixed; also replaced the
`WindowInsets.union` call (an experimental opt-in) with two chained inset
consumers so the composer sits exactly `max(nav bar, keyboard)` above the
bottom edge.

**Slice 3 — account and Edit profile (this commit).**

1. **`CurioAccountComponents.kt` (new).** Sign in and Create account are two
   MODES with ONE primary action, not a field row with a stray "create
   account" text button beside a "sign in" button. Create asks for the
   password twice, every rule is validated BEFORE the network call, and the
   server's own answer is shown in a soft message box — which is why a taken
   username finally says it is taken instead of appearing to do nothing. The
   signed-in card carries the username (rules stated up front, server verdict
   shown) and the 16-portrait picker.
2. **Online mode page** now renders those two components instead of its own
   copy of the form, so Settings and Edit profile can never disagree about
   what an account is.
3. **Edit profile** gained a "Curio account" section (sign in or the username
   and portrait), and its body scrolls now that it holds four sections.
4. **The Community row left Profile** (as asked).

**Slice 4 — Friends and Messages, plus the reactions + typing backend (this
commit).**

1. **`SocialComponents.kt` (new) — Curio's social language.** Box cards
   (`SocialCard`), pill actions with glyphs (`SocialPill` / `SocialIconPill`),
   person cards, thread cards, empty cards, day rules, the social search pill
   and the reaction palette. Every social surface is built from this kit, which
   is why a name, a portrait and a set of actions look the same everywhere.
2. **`SocialRelation`.** One derived answer per person (NONE / OUTGOING /
   INCOMING / FRIEND / SELF) decides which pills a card offers, so "Add
   friend" can no longer appear on someone who is already a friend, already
   asked, or is you. That was the "add friend goes even if already friends"
   bug.
3. **The conversation screen rewritten.** It opens from the DEVICE's own copy
   (`SocialMessageCache`, newest 200 lines, text only) before the network is
   asked anything; day rules and sender runs with one timestamp each; "Seen"
   on the newest of my delivered lines only; tap a bubble for the reaction
   palette; a polled "Typing…" line in the header and a breathing bubble in
   the thread; optimistic send with a spring entry; a frosted-pill composer
   with a round send button that grows when there is something to send.
4. **Schema: `dm_typing` + `dm_reactions`** (idempotent, with RLS, grants,
   the anon revokes, the self-check table lists, and a `curio_stamp_typing`
   trigger so the freshness stamp is the SERVER's clock). `SocialApi` gained
   `setTyping` / `isTyping` / `reactions` / `react` / `clearReaction`.
   **The typing and reactions features need the schema re-pasted in the
   Supabase SQL editor** — until then `isTyping` answers false and reactions
   simply do not render, and nothing else breaks.

**Still not done (queued):** the built-in collection-card redraw the user
listed; inline sharing of notes / quotes; reply-with-mention in comments; and
an unread divider in the inbox.

**Status:** pushed (`c71681a4` for the CI repair); this commit carries slices 3
and 4.

## Archive

Older completed request logs (2026-09-08 → 2026-09-10) were trimmed from this
file on 2026-09-10 to keep it short. They live in git history
(`git log -p -- Prompt.md`) if anything needs revisiting.

## next prompt

_No pending prompt._ (Both the ACTIVE list and the follow-up queued for this
cycle are logged above. The next user instruction goes here — it is never
cleared by an agent; the completed one moves up into the request log.)

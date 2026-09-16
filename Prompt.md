# Prompt Log — current request

## Request (2026-09-16, COMPLETE — revert the quote-repost feature and the social wall's card size)

Verbatim: "from this revert the quote repost feature only" (commit `2a9d4ea3`,
which shipped Follow members + Quote-repost together) "and " (commit
`6ffb0272`) "revert the topic card view in social page, i mean revert its size
which got chnaged in that commit".

### Decisions (ask_user, before touching anything)

`6ffb0272` changed TWO sizes on the wall, so the pick was put to the user:

1. **"Both of them"** — the topic card ART (that commit rewrote
   `CommunityCardCanvas` to lay the card out at its own design size and scale
   it as one layer, which is the "shrunken art" `86d37814` had already fixed
   on 2026-09-14) AND the post ROW (compact padding / 32dp portraits). Both go
   back to their pre-commit state.
2. **"Yes — remove it all"** — the quote-repost database side comes out of
   `supabase/schema.sql` too (the two columns and the widened kind CHECK).
   Nothing has to be re-pasted: the file only ever ADDS columns, so a live
   database that already ran the newer file keeps them, unused.
3. **"Remove the switch (recommended)"** — the wall's density experiment is
   settled, so the "Roomy social wall" User-experiments row and the
   `communityRoomyWall` preference key are gone with the compact spacing
   (root AGENTS: an experiment's toggle is removed once the A/B is decided).

### Reverted — the quote-repost (v389)

- **`CommunityApi.kt`:** `CommunityCard.quoteSourceId/quoteWords/quoteSource`,
  `CommunityCardDraft.quoteSourceId/quoteWords`, the `KIND_REPOST` constant and
  the widened `isTextOnly`, the `quote_source:…` self-join in `CARD_COLUMNS`,
  the REPOST branches in `draftProblem`, the `quote_source_id`/`quote_words`
  payload block, and the three parsed quote fields.
- **`CommunityPostScreen.kt`:** the `quoteSource` parameter, the REPOST
  starting kind, the Repost pill (`PostKindRail.quotedPostAvailable`), the
  quoted-post chip (`QuoteSourceChip`, deleted), the nested `quoted` block in
  `TextPostPreview`, the `placeholder` parameter on `ComposerEditorField`, and
  the two REPOST cases in the preview.
- **`CommunityScreen.kt`:** the `quoting` state and `onQuote` (the wall's
  second quote icon), and the `quoteSource` wiring into the composer.
- **`SocialComponents.kt`:** the whole `KIND_REPOST` branch of
  `SocialTextPost` (the nested quoted card) and the import.
- **`supabase/schema.sql`:** the two `add column` lines, the guarded
  `community_cards_kind_v2` constraint block, and the kind CHECK back to
  `('CARD', 'NOTE', 'QUOTE')`. The `member_follows` table, its policies and
  its entry in the ban guard's table list STAY (follows were not reverted).

### Reverted — the wall's card size (v388)

- **`CommunityCardCanvas`** is back to the v86d37814 shape: the real share card
  laid out DIRECTLY at the width the row offers, capped at the aspect's design
  width (`minOf(maxWidth, aspect.widthDp.dp)` + `aspectRatio`) — no
  `graphicsLayer` scaling at the design size, no pinned font scale, so the wall
  row, the composer preview and the post's page all draw the card at their own
  width again. The doc block, the now-unused
  `CompositionLocalProvider`/`LocalDensity`/`Density`/`TransformOrigin` imports
  and the duplicate `graphicsLayer` import the commit added went with it.
- **`CommunityCardItem`** is the airy row again: 24dp shape, 12dp padding,
  36dp portrait, 10dp name inset, 8dp seams. The `compact` parameter, its
  `AppPreferences.communityRoomyWallState` call site, the state field, its
  loader line, `KEY_COMMUNITY_ROOMY_WALL`, both preference functions and the
  Experiments switch row were deleted.
- **Kept** (not part of either size): the rose **accent fill**
  (`communityCardFill()`), the pull-quote (`SocialPullQuote`), drafts + locally
  kept deleted posts, the Post pill's scroll hiding, the Following chip and the
  follow pill.

### Docs

`app/AGENTS.md` — the wall's-look bullet now says the airier row is shipped and
names the removed switch; the "Follows + quote-reposts" bullet is "Follows"
again and records that the quote-repost was built and then reverted. The store
changelog `20260922.txt` lost the quote-repost ADD line (it never shipped) and
the "wall cards are tighter" half of its FIX line; the "topic cards render
whole and crisp" FIX line stays — it describes the rendering this revert
restores.

### Verified

Brace balance on all six touched Kotlin files (`scripts/check_braces.js`),
`git diff --check` clean, and a repo-wide sweep for `KIND_REPOST`,
`quoteSource(Id)`, `quoteWords`, `quote_source`, `communityRoomyWall`,
`CompositionLocalProvider`, `TransformOrigin` and `LocalDensity` inside
`app/src/main/java/com/curio/app/features/community` + `data/supabase` returns
nothing. One deliberate near-revert: `parseOneCard` stays (the feature commit
extracted it for the nested parse), but it is honestly non-null now instead of
returning a `?` the loop had to unwrap.

Not compiled here (no Android SDK — root AGENTS rule); CI is the compile check.

### Still open (named, not silently dropped)

Prompt.md's "Next prompt" slot still holds the member's separate request about
chat notifications, the journal/book pages, the formatting tools and the +
sheet — it was NOT part of this request and is left pending, surfaced to the
user.

---

## Request (2026-09-16, SHIPPED (`736cdb9b`) — the compile fix, then FOLLOW MEMBERS + QUOTE-REPOST built on top)

The CI log the user pasted listed eight broken files; every one was a real
missing symbol in the feature commit: a lost newline in AppPreferences that
merged two statements, `routeSlug` living on `CurioCategory.id` not the
category, missing imports (`TextButton` — whose absence caused the whole
"@Composable invocations" cascade — `lerp`, `BAN_CONTENT`, `CurioRoutes`,
`Dp`, `SocialModerationHistoryCard`), `LivePostPreview` missing its `accent`
parameter, and the ask-friend call reading the nullable token straight into a
String parameter. Fixed, pushed as `736cdb9b`.

Then the two picks the user approved (Follow members, Quote-repost) were
built in the same pass:

- **Follows:** `member_follows` (schema, RLS, ban-guarded),
  `CommunityApi.follow/unfollow/followingIds/followerCount`, a second hero
  pill on member profiles (optimistic, rolls back on refusal), and a
  Following/Everyone chip on the wall that filters the feed client-side.
- **Quote-repost:** a fourth post KIND (`REPOST`) with `quote_source_id` +
  `quote_words` columns (the kind CHECK widened through a guarded
  constraint replacement), a PostgREST self-join in `CARD_COLUMNS` so the
  quoted card arrives with the row (`parseOneCard` split out to parse it
  with a null viewer), the wall's quote icon opening the composer with the
  card attached, a Repost pill in the kind rail, a quiet quoted-post chip,
  and preview + wall rendering through the same nested shape (a quoted
  QUOTE keeps its pull-quote).

Verification this pass: brace balance on all five touched Kotlin files,
`git diff --check`, an import sweep over every new symbol, and the schema
edited alongside (the guard's table list gained `member_follows`). CI is
the first real compile again — if it reports, send the log.

---

## Request (2026-09-16, AWAITING VERIFICATION → SHIPPED `6ffb0272` — the ban ladder, the wall's look, profiles in the tear)

Verbatim (one message, landed AFTER the previous request shipped as `477e0d08`): "when i ban a member it should show inside the moderation page, also permanent account ban option too, they can only view posts, and another where i can ban them from social feature too, also similiar to the home + button hide when scroll add similiar for the post button too hides when scrolled down reappears when srolled above. and also suggest more features, refine quote post and preview both, properly refine so it looks proper quote, and more features suggestions and reifnements. and before pushing ask me also the book select and read use the app book catalog too. first the app books if not then openlibrary, also the new redrawns are bad, i want cute chibby style but rn the new avatars are weird eyes and specially weird not matching eyebrows and also weird neck tint fix it, also similiar to the socail page inside tears chat friends you, i want you to move the profile inisde the headrer tears too, remove the profile a memeber of the community text, and full put the profile with the similiar hero accent insid ethe tear by expanding the tear. proper messege button too. also the posts below, make them more compact its too much space taking, also give it proper accent color too not cream."

### Status: BUILT (unpushed) — ask_user sent, push held until the user answers

### What the request turned into (the decisions, all ask_user-answered)

1. **Ban levels: "Four levels, picked per ban"** — content (hide + no posting),
   read_only (view the wall only), social (wall works, friends/messages
   paused), account (permanent lock). Each ban also carries its own clock
   (24h / 7d / 30d / forever), because "permanent account ban" was named
   beside two obviously reversible tiers — the reach and the duration are
   separate choices.
2. **Scope: "Everything in one pass"**, database "**App + database, no new
   server function**" for the *mechanism* — the ladder still needed its RPCs
   (ban / lift / list / history) because RPCs are how every moderation action
   already reaches the DB; nothing else about the deployment changed.
3. **Also asked in the same answers** (all built): moderation history per
   member + on your own Profile tab; compact wall cards; wall accent instead
   of cream; proper pull-quote post (preview + wall through ONE renderer);
   profile identity inside the hero tear; avatar chibi correction; app book
   catalog searched BEFORE Open Library; composer drafts; locally-kept deleted
   posts that can be re-posted; the Post button hiding on scroll.

### Shipped (the build, all unpushed)

- **The ban ladder (v388).** `profiles.ban_kind` ('' | content | read_only |
  social | account) + `banned_until`; `curio_ban_kind()` (lapsed = no ban),
  `curio_member_hidden` (content/account), `curio_can_post` ('' / social),
  `curio_can_social` ('' / content). **Enforcement is a BEFORE trigger**
  (`curio_ban_guard`) on every writable table — RPCs bypass RLS, so a
  policy-only tier would have been decoration. The moderation page's third
  tab is the BAN LIST (live first, then lifted/lapsed as quiet history rows
  with tier chip, clock, reason, setter, Change-tier / Lift). The ban sheet
  (`ModerationBanDialog`) picks tier + clock + required reason in one place,
  pre-picks the tier in force, and offers the lift at its foot; it is opened
  from the queue's hide action, from the ban list's Change tier, and from a
  profile's ⋮. `curio_moderate_member_history` feeds the member's own
  moderation record on their profile AND on your Profile tab.
- **The wall (v388).** Post pill hides while scrolling down, returns on the
  way up / at the top (the Home `+` pattern). Cards are COMPACT by default
  (`communityRoomyWall` in User experiments is the roomy A/B escape) and wear
  the rose accent family instead of the capture creams. Quotes render through
  `SocialPullQuote` on BOTH the composer preview and the wall — one shape: an
  accent rule, an oversized opening mark, the serif face, the credit tied on
  by a dash.
- **Profiles in the tear (v388).** `SocialProfileScreen` extends the hero
  tear (`profileTearHeight`) and draws the portrait, name, @handle, counts,
  bio and action pills on the banner; the "a member of the community" line is
  gone; Message is a proper hero pill. Own Profile tab keeps the same record.
- **Chibi avatars (v388 correction).** One big dark chibi eye (deep centre +
  two highlights + one lid stroke), brows mirrored from the SAME points about
  the midline so they always match, and a neck in the skin's own shade with a
  thinner jaw shadow — the ink-wash "weird neck tint" is gone.
- **Books: app catalog first (v388).** `BookCatalog` (~370 curated entries,
  by-title and by-author indexes, `matchQuality` ranking) is searched BEFORE
  Open Library in the add-book sheet; the section header says where each hit
  came from, and the manual door stays.
- **Composer memory (v388).** Drafts are kept per-kind locally and offered
  back once per kind; deleted posts are remembered locally, listed behind a
  small door at the foot of the wall, re-postable through the normal pipeline
  or forgettable; both are wiped on sign-out (`OnlineAccount.signOut` →
  `SocialPostArchive.clear`).

### Verification status

This environment has no Android SDK — CI is the first real compile. Static:
`scripts/check_braces.js` on all 22 touched Kotlin files (OK),
`git diff --check` clean, every new import/symbol traced. **Push is held for
the user's answer**; the feature commit + Prompt.md log go out together when
they say go.

---

## Request (2026-09-16, SHIPPED (pushed `477e0d08` + `f9927f6c`) — journals & books, Add take, the browser lag)

Verbatim (one message): take the create-entry-flow ideas from
`feature/create-entry-flow` "but our design" — a **+** on Home that disappears on
scroll like the similar button; the sheet it opens starts a **journal** and a
**book** ("no write a book", but a flow where you pick a book, then write
per-chapter reviews and progress); journals and books get their **own store and
their own unique saved view**, independent of the app's saved detail view, easy
to edit and view IN THAT SAME PAGE with **persistent auto-save** (nothing lost on
an app switch); the **book reader is a shelf of covers** with the name and
progress under each, opening a book shows the chapter reviews you wrote (or a
new creating → saved → view flow), all kept in a personal collection with its own
small view; the **journal has date + date changer, emotion, title and writing**,
with bold/italic/underline/strikethrough/quote/left formatting and photos added
in the same writing canvas, the tools shown **above the keyboard as icons** and
applied to **the line the caret is on**, the sheet **auto-scrolling** so the field
never goes under the keyboard; bring back the **"Add take"** row action from that
commit (the picker stays, the new take then appears beside the previous ones); and
on **Home**, remove the saved row and instead show **journal and book chips in a
horizontal row** — small, fixed-shape previews that open directly, wearing the
app accent ("not the cream colors").

Then: "fix this" (a CI compile failure: `SocialAvatar.kt:1162` missing `u`,
`SettingsHubScreen.kt:629` duplicate `@Composable` eating the one
`SettingsHeroActionPill` needed) "and the topic browser scrolling and loading
too and push both of them fast, then finish the result but dont push untill i say
so".

### Shipped (pushed first, as asked)

- **Compile fixes.** `avatarBow(o(50f, 21f), …)` → `o(50f, 21f, u)` (the `u`
  argument was dropped from one call in the bow ornament), and the stray
  `@Composable` sitting above `settingsHeroPillFill`'s SECOND doc block (which
  made the annotation "not repeatable" and left `SettingsHeroActionPill` — and
  therefore the three composable calls inside it — without one) moved onto
  `SettingsHeroActionPill` itself.
- **Topic browser scroll + load (v387).** See `app/AGENTS.md`'s catalog bullet:
  browse rows read the loader's parsed lane lists directly, the 16k index /
  id-map / `groupBy` are search-only, the last browse row set is cached for an
  instant first frame, and the indicator + back-to-top reads moved into a
  `BoxScope.ScrollScopedRead` so a scroll step no longer recomposes the page.

### Decisions (ask_user, before building the writing system)

1. **Home rows:** the **Saved section goes AND the saved-capture rows leave
   Recents** — Home's shelf slot is the member's own writing now. (The saved
   shelf keeps its data and its doors: Topic History for quotes + pins, the
   Cabinet / Recents page / detail view for saved captures.)
2. **Markers:** there are none. Formatting is stored BESIDE the text (a
   per-character bitmask → merged `PersonalRun` ranges), so the words stay
   plain prose and auto-save is exact.
3. Earlier answers that shaped the build: **their own store, shown as their own
   collection views**; **book search online with a manual fallback**; **photos
   as inline blocks in the page**; **a tool applies to the selection, else to
   that line**; the feature is **always on**.

### Built (UNPUSHED, awaiting the user's go-ahead)

New files — `data/PersonalDoc.kt` (block document + JSON codec + moods + ids),
`data/PersonalEntity.kt` (`personal_notes` / `personal_books`),
`data/PersonalDao.kt` (DAO + `PersonalRepository` + `PersonalRepositoryHolder`),
`features/personal/PersonalRuns.kt` (the bitmask engine),
`PersonalCanvas.kt` (canvas + state + read-only view + tool dock),
`JournalEditorScreen.kt`, `JournalListScreen.kt`, `BookShelfScreen.kt`
(shelf + add-book search/manual), `BookDetailScreen.kt`, `PersonalHome.kt`
(the `+` launcher, its sheet, the Home chips row).

Edited — `CurioDatabase` (v15 + `MIGRATION_14_15`), `MainActivity` (installs
`PersonalRepositoryHolder`), `CurioRoutes` + `CurioNavHost` (4 routes: journals,
a journal page, the shelf, a book), `HomeScreen` (Saved section replaced by
`PersonalChipsRow`, saved entries filtered out of Recents, `+` launcher with
scroll-direction hiding), `CaptureStudio` (+`onSelectTake`, rail pills + an
**Add take** door, the picker's title now says Add take) and
`SaveCaptureScreen` (wires `onSelectTake`).

The Cabinet's **Personal shelf** is wired too (user decision: "push, and wire the
Cabinet Personal shelf first"): `SHELF_LEVEL_PERSONAL` + `CabinetPersonalShelf.kt`
show the journals and books in their own small tiles inside the Cabinet grid, the
shelf card counts journals + books, and a `Saved in Personal` door at the foot
keeps the shelf's old saved members reachable (nothing was removed).

Still open, named so it is not mistaken for shipped: the personal store is NOT in
the Backup & restore export (`CurioBackupManager` streams the capture table only);
and a photo inserted at the caret is a block (it splits the paragraph around it)
rather than a picture flowing mid-sentence.

## Request (2026-09-16, COMPLETE — portraits, the Social header, edit-by-default, the category panel)

Verbatim (one message, four parts): the profile avatars "are not good enough and
beautiful and better detailed enough, redesin all"; the social page's Chats /
Friends / You "appear late", and should be "put inside the header tear by
extending the header"; "turn on edit messeges by default"; and in the topic
browser's category picker, selecting a category then closing the box applies it
"even though i didnt press done".

### Decisions (ask_user, before building)

1. **Avatars: "Illustrated, much more detail"** — keep the same 28 characters,
the same palette rows and the same public API, and rebuild the DRAWING.
2. **The category picker: apply the pick WITHOUT Done.** Their words: "noo i
want it to apply the pick without done too not the other way fix" — so Browse
Topics drops the staged pending-set (a tap commits) and its Done simply
collapses the panel, exactly like the Cabinet's panel already did.
3. **The doors go in the header for everyone** (signed out included); both
destinations already carry their own "Sign in…" card.

### Root causes / shape of the work

1. **The portraits were built for 40dp and judged at hero size.** A circle head
(r=22), hair as ONE rounded rectangle, a mouth as a single arc, no brows in
some passes, a beard as a half-disc, hats as bare fills. Nothing was wrong
mechanically — the construction was simply too thin to survive a large size.
   Also found: the garment rect (`x 20..80, y 74..118`) reached past `y≈90`,
   where the disc is already narrower than the rect, so every portrait had
   square garment corners OUTSIDE its circle — nothing clipped the Canvas.
2. **The doors were a list item under "Last 24 hours" inside the
`else { eligible }` branch**, so they only rendered after the account/profile
check came back — the row was missing on every open.
3. **`isSocialTextEditingEnabled` read `KEY_SOCIAL_TEXT_EDITING` with a `false`
default**, so Edit was absent until the Experiments switch was found.
4. **`TopicDatabaseScreen` kept a `pendingCats` set**: taps staged, Done
committed, closing with the pill discarded — and the committed chips row behind
the panel told a different story than the panel did.

### Shipped

- **`SocialAvatar.kt` rewritten (v386).** Shared 100×100 grid helpers (`o`, `s`,
  `cr`, `Path.mv/ln/qd/cu`), tones derived per row (`darken`/`lighten`).
  `drawCharacter` runs four passes: `drawHairBack` (masses, hood shell, helmet
  glass, ponytails, braids, curls) → bust (`drawShoulders` with a collar trim,
  folds and a lit shoulder) + `drawNeck` (jaw shadow, throat light) +
  `drawHead` (ears, tapered jaw/chin, cheek warmth, forehead shade) →
  `drawHairFront` (fringe/hat per style + ornaments) → `drawFace` (brows, eyes
  with an eyelid crease and lower lid, nose bridge + nostrils, two-part mouth +
  chin crease) and the face accessories (glasses, earring, freckles). The
  character is drawn inside `clipPath(circle)`, so no geometry can ever spill
  the disc. `avatarFlower`/`avatarBow`/`avatarLeaf` were rebuilt with shading;
  the star/heart clip paths are unchanged.
- **Hero footer slot (`SettingsHeroHeader`, v386).** `footer` +
  `footerHeight`, `bannerHeight + footerHeight`, a 12dp `SettingsHeroFooterGap`,
  extra bottom clearance for the tear, the GLASS toolbar path stacking the
  footer under the bar, and a public `settingsHeroTotalHeight(footerHeight)`
  the caller reserves. Also extracted the hero pill's opaque fill into
  `settingsHeroPillFill()` (shared by the back pill, the hero action pills and
  the new door tiles — v27n: opaque or the elevation shadow smears).
- **Social screen:** the door row left the list and rides the banner
  (`doorsFooter` lambda, both the pinned phone hero and the wide in-list hero),
  `SocialDoorRowHeight = 58.dp` reserved in the content padding, and
  `CommunityDoorTile` now takes the hero's `ink` and paints hero glass.
- **Edit by default:** `isSocialTextEditingEnabled` defaults `true`, the state
  field seeds `true`; the Experiments switch still turns it off (explicit off
  wins), so no render site changed.
- **Category panel applies live:** `pendingCats` and its seeding effect deleted
  from `TopicDatabaseScreen`; `onPanelToggle` commits through `commitCats`,
  `onPanelClearAll` clears, `onPanelDone` collapses, and both call sites pass
  `selected = effectiveCats`.

### Detail notes from the build (worth keeping)

- **Brows had to move UNDER the hair.** With the brows inside the face pass
  (painted after the hair), a dark arc landed ON a beanie cuff and, on light
  hair, across the fringe. They are now their own `drawBrows` pass, called
  between the head and the front hair, and the shared fringe arc was shortened
  (`fringe(top, 30f)` instead of 38) so it stops just above the brow line
  instead of swallowing the forehead.
- **The Canvas was never clipped.** The old garment rect ran to `y 118` and the
  disc is narrower than the rect below `y≈90`, so every portrait had square
  garment corners OUTSIDE its circle. The character now draws inside
  `clipPath(circle)`, which also lets the new bust reach the bottom of the frame.
- **`avatarBow` is the one accessory drawn in CANVAS pixels** (it is placed in a
  rotated frame), so it uses the raw `Path.moveTo/lineTo` — the `mv/ln/qd/cu`
  helpers are design units and would have silently mis-scaled it.
- **Not changed on purpose:** the `0–27` style count and the 28-row palette
  table (a widening would need the Supabase check + `SOCIAL_AVATAR_STYLE_COUNT`
  to move together), the Experiments "Social text editing" toggle (the default
  moved to ON; an explicit OFF still wins), and the store changelog file
  (`20260922.txt` — the versionCode did not bump).

## Request (2026-09-16, COMPLETE — the Wildcard lane, restart speed, the composer and the chat surface)

Verbatim (one message, seven parts): the reply box in chats looks too big and too
wide even though the text is small; Enter should make a new row in a message
instead of sending it; there is no new chat button in Chats; in the post page the
topic picker is expanded by default and too long (collapse it) and its topic
search is bad; the app's Wildcard shows topic loading — exclude other category
topics from Wildcard; the Topic Database is too laggy and takes too long to load
the topics; and a restart now shows "Gathering the deck" where it did not before
(they pointed at commit `1ad9c832`). Plus: the Supabase URL configuration keeps
flipping to a preview URL and `https://curio-dwnz.vercel.app/` should be the
default.

### Decisions (ask_user, before building)

1. **Wildcard = `wildcard.json` alone.** The deck's Wildcard stops merging every
   lane (that merge is the "topic loading"): it becomes the ~500 hand-curated
   curiosities, exactly what the Topic Database's Wildcard lane has always shown.
2. **"The reply box" = the quote drawn INSIDE a sent answer** (their words: "the
   reply box that shows after I send"), not the typing pill.
3. **A compose button in Chats** that opens a friend picker, with a door into
   Friends for anyone not a friend yet.
4. **The domain**: `/api/config` now serves the deployment's canonical address and
   every emailed `redirect_to` is built from it, so a preview deployment can no
   longer mint preview links.

### Root causes found (in the code, not guessed)

1. **`ReplyQuoteRow` was `fillMaxWidth()`** with a 34dp accent bar. A fill-width
   child forces its parent to that width, so every answer ballooned to the full
   thread width around ONE small line of quoted words.
2. **The composer was `singleLine = true` with `ImeAction.Send`** — Enter sent.
3. **ChatsScreen had no create path at all**; the only door was Friends, and the
   empty state said so in prose.
4. **The composer opened the picker the moment Topic was selected
   (`topicPickerOpen = topic == null`)** and rendered up to 18 results with two
   lines of teaser each inside the scrolling column.
5. **Its search sorted the WHOLE 16k-entry index on every keystroke** (a
   comparator calling `contains` per name) before filtering, then ordered what was
   left alphabetically; teaser hits could outrank a name that starts with the
   query, and a name hit in a teaser read as a near-miss.
6. **`load(WILDCARD)` merged every lane** (bounded to two parses at a time), so
   selecting the Wildcard deck — the DEFAULT spin set on a fresh install — paid
   for the whole catalog and showed "Gathering the deck…" while it did.
7. **v348 made the asset the read path and dropped the Room pool warm
   (`warmLoaderFromRoom` is counts-only now)**, so on every restart every lane
   re-parsed AND `parseAndCache` re-wrote the whole catalog back into Room
   (delete + ~14k inserts with chapter/track JSON) on the same IO threads, and
   `poolLoading` only cleared on the FULL pool. That is the "Gathering the deck"
   regression the user reported against `1ad9c832`.
8. **`cleanText` built four `Regex` objects per call** — four calls per topic,
   so ~16 pattern compilations for each of ~14k topics per cold catalog load.
9. **`sampleTopics` was gated on `isInitialized()`**, i.e. the deck's instant
   Room seed was skipped exactly when it was needed (composing before the
   repository flipped the flag).
10. **auth-web's `siteUrl()` used `location.origin` only** (by design, per its own
    AGENTS.md), so a preview deployment's hostname rode into every email link
    asked for from a preview.

### Shipped

- **Wildcard is a lane** (`data/TopicJsonLoader.kt`, `data/TopicRepository.kt`):
  one file, no merge; `countFor(WILDCARD)` counts it; `sampleTopics` samples that
  lane first (the every-lane sweep survives only for a database mirrored before
  the change). Deck, reveal fallback and the browser now agree.
- **Speed**: `cleanText`'s Regexes are file-scope constants; the Room mirror
  skips a lane whose Room count already matches the parsed count; `sampleTopics`
  is no longer gated on `isInitialized()`; Spin clears `poolLoading` as soon as
  the seed lands.
- **Chats (v385)**: compose button + `NewChatSheet` (friends via
  `SocialApi.friends`, `SocialSidebarRow`, read once per opening, 72dp of list
  padding under the last row so the button never covers a chat).
- **Direct messages**: `ReplyQuoteRow` hugs its text (one line tall, capped
  width); the composer grows a row at a time and Enter breaks the line
  (`ImeAction.Default`, up to five rows, then it scrolls) with the round button as
  the only send.
- **Composer**: the topic chooser stays COLLAPSED (and the kind rail collapses it
  on every switch); `searchTopics`/`topicMatchRank` filter first and rank by
  quality (prefix → word start → substring → byline/subtype → tag → teaser, 8
  results, 5 suggestions), each row names its lane, tighter one-line rows, and a
  "no topic matches" line; the index is seeded from `cachedIndex()`.
- **auth-web**: `/api/config` serves `siteUrl` (from `SITE_URL`, else Vercel's
  `VERCEL_PROJECT_PRODUCTION_URL`, normalised to a scheme), `curio.js` builds
  every `redirect_to` from it, and `README.md` gained a "One domain for the
  links" section naming all three places (Vercel `SITE_URL`, the Supabase Site
  URL + Redirect URLs, the `CURIO_AUTH_SITE_URL` repo secret).

### Verified

- `node --check` on every modified JavaScript file.
- Kotlin delimiter balance on all six edited files (python scanner).
- Not compiled locally (root AGENTS rule): CI on this push is the compile check.

### Notes for the next pass

- The picker's "Surprise mix" label still reads as a mix; the pool is now the
  curated Wildcard curiosities (user-visible copy, left for the user to decide).
- `/reset` on the account site still requires 8 characters while `/signup`
  requires 6 (the app's own rule).

## Request (2026-09-16, COMPLETE — account site: create an account WITH a password)

Verbatim: the auth web has no create-account-with-password page, it just sends a
link that creates the account and logs in with no password. Fix that.

### Decisions (ask_user, before building)

1. **The sign-in page's "Create a Curio account if this address is new"
   checkbox stays**, but ticking it no longer mails a passwordless account: it
   hands the address to the new `/signup` page. Existing accounts keep the link
   form; the address travels in `sessionStorage` (`curio.signup.email`), never
   in the URL.
2. **A dedicated `/signup` page** (the site's one-URL-per-flow shape: a new
   folder, its own `data-page`, its own ids in the `PAGES` switch), not mode
   pills on `/signin`.
3. **6-character minimum**, the rule the app's create mode states.
4. **The terms acceptance is required**, the same gate the app's create mode
   applies (`canSubmit` includes `termsAccepted`), linking the existing
   `/terms` and `/privacy` pages.

### What was wrong (in the code, not guessed)

`Auth.signUp(email, password, redirectTo)` already existed in
`assets/curio.js` and had **no caller**: the only way to get an account on the
site was `signin/`'s link form, which called `POST /auth/v1/otp` with
`create_user: true` (checkbox `checked` by default). GoTrue creates the account
and signs it in from the emailed link, so the account ended up with **no
password at all** and could only ever be entered through another link. The
Android app has always created an account with email + password (confirmed via
`/auth/v1/signup` with `redirect_to=<site>/confirm`), so the site was the odd
one out.

### Shipped

- **New `auth-web/signup/index.html`** (`data-page="signup"`): email, password,
  repeat, terms acceptance, one primary action; a `sent` state that says the
  account exists and the confirmation link is the next step (with a door to
  `/confirm` for a fresh email, and the honest note that an address which
  already had an account gets no second email).
- **`initSignup(auth)`** in `assets/curio.js`: validates locally first (address,
  6 characters, the two passwords match, terms agreed), calls
  `auth.signUp(email, password, siteUrl('/confirm'))`, writes the session and
  lands on `/account` when the project has email confirmation OFF, and shows the
  `sent` state when a confirmation email is required. Registered as `signup` in
  the `PAGES` switch, so the unconfigured-deployment notice covers it like every
  other flow page.
- **`initSignin`**: the create checkbox drives the button label (`Send me a
  link` / `Create my account`) and carries the address to `/signup`;
  `magicLink(..., false)` now pins `create_user: false`, so a link can only sign
  an existing account in. The checkbox is unchecked by default (a sign-in page
  should not pre-answer "is this address new?"), and the method card gained a
  "New here? Create an account with a password" row plus an escape hatch in the
  `sent` state.
- **Discoverability**: a `Create account` entry in the top bar of all 10 pages
  (`flex-wrap` added to `.topnav`, which five destinations no longer fit beside
  the brand on a narrow phone), a hero button and a first tile on the landing
  page, and a support-page paragraph that now says an address with no account is
  the one to create.
- **Docs**: `auth-web/AGENTS.md` (page list + the new local contract "an
  account's first password is set on /signup; a link never creates one"),
  `auth-web/README.md` (page table + the create-account explanation), root
  `AGENTS.md` (auth-web purpose + page folders), this log.

### Verified

- `node --check` on all three JavaScript files.
- A throwaway site checker (`.tmp-auth-web-check.mjs`, deleted after the run):
  tag balance on all 10 pages, `data-page` and `PAGES` coverage, every
  `need()`/`value()` id present on the page that needs it, every `showState()`
  value backed by a `data-state`, no inline `<script>`/`style=`, no baked-in
  Supabase value, and the magic-link/signup flags. All checks pass.
- Not the Android module: nothing under `app/` changed, so CI's build is
  unaffected; a Vercel preview deploy is the real check for the site.

### Notes for the next pass

- The `/reset` page still requires 8 characters while `/signup` (matching the
  app) requires 6. Left alone: it is a stricter pre-existing rule, not part of
  this request.
- `Auth.magicLink`'s `createUser` parameter is now only ever called with
  `false`. Kept (the client mirrors GoTrue's endpoint) but it has no live
  caller that passes `true`.

### CI fix (same request, 2026-09-16)

The first push failed CI: the KDoc on the new `roomFallback` contained the glob
`` `assets/topics/*.json` `` — in Kotlin, `/*` inside a block comment opens a
NESTED comment, so the doc's own closer only closed that one and the rest of
`TopicJsonLoader.kt` was swallowed ("Missing '}' at 162" + "Unclosed comment at
695"). Every other compile error in the log (14 files: `cached`, `loadIndex`,
`TopicIndexEntry`, `countFor`, `shedForMemory`, …) was a cascade of the loader
not resolving. Fixed the doc text, left a NOTE in it about the trap, and gave
MainActivity's Cabinet warm-collector an explicit empty lambda
(`collect { }` — parameterless `collect()` needs the CollectSubscriber overload
the project doesn't import). A scanner for nested-`/*` hazards now runs before
every push.

## Request (2026-09-16, COMPLETE — instant open + the create-entry composer)

Verbatim: compare `v1.1.1-beta5` and `feature/create-entry-flow` with current
main. In the tag, the Topic Database opened with topics instantly available and
the Cabinet was equally instant; find why and do the same to the current UI.
From the branch, bring the new post screen with refinements — user picked via
ask_user: the live-preview composer, the icon aliases, refinements (topic at
the top, card-vs-text choice for topics, look controls only when cards are
selected, keep the current quote creation flow), always-on live preview, and
mirror `android.yml`'s topic bundling into `release.yml`.

### Root causes (measured in the code, not guessed)

1. **Browser:** v347 replaced the tag's warm-index seed
   (`initialValue = cachedIndex()...`, precomputed keys) with
   `initialValue = emptyList()`, so every open re-lowercased the whole catalog
   and re-derived every year on Dispatchers.Default before the first row.
2. **Startup:** the prewarm was queued behind `TopicRepository.init()`, whose
   first launch / version-gated re-sync parses all 38 lanes before the browser's
   index could start building.
3. **Loader:** `load()` served Room's "fast path" first — row mapping + per-row
   Gson decode of chapters/tracks/episodes, which is SLOWER than parsing the
   JSON once, and masked the shipped content.
4. **Cabinet:** `peekLight()` is only non-empty after something collects the
   light flow, and nothing did at startup — a cold start into Cabinet had
   nothing to seed from.
5. **Releases (not asked, found):** since `a365cd76` the JSON lives in
   `data/topics/`; `android.yml` copies it back but `release.yml` never did —
   every tagged release shipped an APK with ZERO topic data.

### Shipped

- `TopicDatabaseScreen.buildIndexedTopics()`: catalog-driven rows whose keys and
  years come from the warm merged index when it holds the topic, else derived.
  `indexEntries` produceState seeds from `cachedIndex()` synchronously.
- `TopicJsonLoader.load()`: JSON pools authoritative, Room fallback only for
  asset-less builds (`roomFallback`). `warmLoaderFromRoom` warms counts only.
- `MainActivity`: prewarm un-queued from the Room import; a new warm collector
  fills the Cabinet's light snapshot (`observeLight().collect()`).
- Composer rewritten: branch structure (kind rail, always-on live preview,
  topic picker with chevron aliases in the new `CurioIconAliases.kt`), user's
  refinements (topic first + auto-open, Card|Note presentation toggle in the
  preview header, look controls only for cards, quote flow = main's credit
  field), main's preserved behaviours (caption for every kind, real-identity
  preview card, 700/1200/180/120 caps, teaser seed on topic pick).
- `release.yml` now bundles `data/topics/*.json` into `assets/topics/` with the
  same fail-if-empty guard as `android.yml`.
- Docs: `app/AGENTS.md` (v348 catalog contract, v349 composer),
  `.github/AGENTS.md` (release bundling), changelog.

### Verified

Delimiters balanced on all six edited Kotlin files; no unused imports remain in
the composer; `CommunityCard`/`CommunityCardDraft` constructors re-read before
use; `git diff --check` clean. Not compiled here (root AGENTS rule) — CI is the
compile check. NOT ported: the branch's journal/book/create flow (out of scope)
and its DM/Friends edits (conflict with the shipped fixes).

## Request (2026-09-16, COMPLETE — Curio account site + email links)

Verbatim: fix the link so we have a web version, because the new "congrats you
have verified", reset password and similar Supabase emails open on localhost,
which is bad. Create a new directory for the email account pages (reset
password, confirm email, etc.), the user will add the .env in the Vercel secrets
and launch it, with a beautiful UI, a delete-account button, and more support.

### Decisions (asked before building)

1. **`auth-web/`, static, zero build** (the user picked this): plain HTML/CSS/JS
   plus two Vercel functions, so nothing here can break the Android CI, no
   dependency install is needed, and the env vars work through `/api/config`.
2. **All six page groups**: confirmation landing, reset password (request and
   set), magic-link sign-in, account page with delete, privacy and terms, and a
   support page.
3. **Deletion through a Vercel function** holding the service-role key, which
   re-derives the caller's identity from their own token before deleting.
4. **Wire the Android app too**: the sign-up redirect plus a "Forgot your
   password?" row.

### Root cause (in the code, not guessed)

The Android client called `POST /auth/v1/signup` with **no `redirect_to`**, so
Supabase fell back to the project's **Site URL**, whose default is
`http://localhost:3000`. There was also no `curio://` scheme registered in
`AndroidManifest.xml` and no reset UI anywhere in the app, so a recovery email
had no destination at all. Both halves are now fixed: a real site exists, and the
app says where the emails should land.

### Shipped

**New `auth-web/`** (static site + 2 functions, documented in `README.md` and
`AGENTS.md`):
- `index.html` landing, `confirm/`, `link/` (magic link), `reset/` (request and
  set new password), `signin/` (link or password), `account/` (profile, counts,
  sign out, delete with a typed confirmation), `privacy/`, `terms/`, `support/`.
- `assets/theme.css` is a full design system (aurora backdrop, glass cards,
  Fraunces + Sora, light-mode override, reduced-motion, print styles) and
  `assets/curio.js` is the only script: config fetch, GoTrue client, session
  store, all three email-link shapes (`#access_token`, `?token_hash`, error
  fragments), friendly copy, and the per-page wiring keyed on `data-page`. No
  inline scripts or styles anywhere, so the CSP is strict.
- `api/config.js` serves only public values (project URL + anon key, support
  address, download link) from Vercel env vars; `api/delete-account.js` verifies
  the caller's token against GoTrue and deletes the id that call returns, then
  re-checks the typed confirmation server-side.
- `vercel.json` carries the security headers (no-referrer, frame denial, strict
  CSP) and asset caching.
- `privacy/` and `terms/` publish `docs/ONLINE_PRIVACY.md` and
  `docs/ONLINE_TERMS.md` without their internal launch-review banners.

**Android wiring**:
- `CURIO_AUTH_SITE_URL` build field (env-driven, empty by default) exported by
  both `android.yml` and `release.yml`.
- `SupabaseClient.signUp(email, password, confirmationRedirect)` sends GoTrue's
  `redirect_to`; `OnlineAccount.confirmationRedirect()` builds `<site>/confirm`.
- `CurioAuthCard` gained a "Forgot your password?" row that opens `<site>/reset`
  in the browser, hidden while the build carries no site URL.

**Docs**: root `AGENTS.md` scope + a new `auth-web/` section and index entry,
`auth-web/AGENTS.md`, `auth-web/README.md`, `master.md` tree, `docs/DOX_TREE.md`,
`.github/AGENTS.md` (the optional secret), `app/AGENTS.md` (the email-link
contract), and the store changelog.

### USER ACTION REQUIRED

1. Deploy `auth-web/` to Vercel (Root Directory `auth-web`) and add
   `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`,
   `SUPPORT_EMAIL` (and optionally `APP_DOWNLOAD_URL`).
2. In Supabase: set the Site URL to the domain and add `https://<domain>/**` to
   the Redirect URLs, or Supabase silently ignores the app's redirect.
3. Add the `CURIO_AUTH_SITE_URL` repo secret so the app's emails and its
   recovery row point at the site.
4. Optional but recommended: switch the email templates to `{{ .RedirectTo }}`
   (see `auth-web/README.md`); the classic `{{ .ConfirmationURL }}` works too.

### Verified

- `node --check` on all three JavaScript files; HTML tag balance on all nine
  pages; a DOM-contract check that every element id the script needs exists on
  the page that needs it (0 problems); no inline `style=` or bare `<script>`.
- Delimiter balance on the edited Kotlin files; `git diff --check` clean.
- No Gradle in this environment (root AGENTS rule); CI is the compile check for
  the app-side changes, and a Vercel preview deploy is the check for the site.

## Request (2026-09-16, COMPLETE — reply threads, reply hearts, compact comments, Friends strip)

Verbatim: reply threads are glitchy — they vanish when a new reply branch is
added, sometimes ALL of them vanish, and sometimes it will not let you keep
replying inside a thread. Also: the Friends tab should show new friends at the
bottom to start new chats, comments should be compact per message, comments
need like buttons and a Reply that branches below as many times as you like,
with proper hierarchy.

### Root causes found (in the code, not guessed)

1. **The renderer only placed TWO levels of the tree.** `branchRenderList`
   rendered each top-level reply plus its DIRECT answers and nothing else, so
   an answer to an answer was not a child of anything it could place — it fell
   into the final "anything not seen" sweep and surfaced at the TOP level of
   the thread, out of order. That is "it doesn't let me keep replying in a
   thread".
2. **The same sweep also swallowed the answers a FOLD was hiding.** A reply
   with more than `BRANCH_PREVIEW` answers showed the first three and pushed the
   rest into that sweep — so they reappeared as flattened duplicates at the
   bottom of the sheet, and the whole thread looked rearranged. Nothing could be
   unfolded below the top level either, because the fold was keyed on roots.
3. **The sheet rendered the two row kinds in two separate `items()` blocks**, and
   a `LazyListScope` puts the second block's rows AFTER the first — every "show
   more" door landed at the very bottom of the sheet, away from the branch it
   belonged to.
4. **A busy card's newest replies fell outside the page.** `comments()` asked for
   `order=created_at.asc&limit=200`: the OLDEST 200 replies. On a card with more
   than that, a reply that had just been written was not in the response at all,
   so the sheet's reload after Send drew the thread without it — the "it
   vanishes when a new reply branch is added" report, and occasionally the whole
   list looked wrong when the fold/orphan mess landed on top of it.
5. **You could not reply to your own line** — the Reply pill was inside the
   `else` branch of `if (reply.mine)`, so a thread could only be continued by
   the other person.
6. **`SocialCommentsCache` threw away part of every row it stored** (no
   `edited_at`) and capped a card at 60 replies against a 200-reply read, so the
   first frame of a busy thread was a different thread from the one the network
   drew.

### Shipped

- **`branchRenderList` rewritten as a depth-first walk** that emits every reply
  EXACTLY once at its real depth, folds each reply's OWN answers behind its own
  `More` node (any level expandable, `More` now carries `depth`), keeps the
  indent bounded (`MAX_REPLY_INDENT` steps of `REPLY_INDENT_STEP_DP` plus a
  hairline thread rule beside every branch) and promotes only a reply whose
  parent is genuinely absent from the page — a merely folded reply stays folded,
  so nothing is duplicated and nothing written is hidden. `BranchRender.key`
  gives every row (`r-<id>` / `m-<id>`) a stable identity; the dead
  `branchOrder()` is gone.
- **The sheet renders that list in ONE lazy pass** keyed on `BranchRender.key`,
  with a `LazyListState` that scrolls a just-sent reply into view; the card
  screen's inline replies use the same list in order, and both share the new
  `ShowMoreReplies` door.
- **`CommunityReplyRow` is three compact lines** (name · age · edited, body,
  actions) with the avatar at 26dp: the heart (new), Reply on ANY reply,
  then the owner's or reader's ICON-ONLY pills (`ReplyPill(label = null)`), which
  is what keeps four actions on one line at the deepest indent.
- **Reply hearts are real now:** `community_comment_reactions` (§4c — its own
  table, because `community_reactions` is keyed by card + member and cannot
  carry a per-reply heart), embedded in the reply read, parsed into
  `CommunityComment.likes/likedByMe`, toggled through
  `CommunityApi.likeComment/unlikeComment` optimistically from the sheet and the
  card view, and cached. One heart glyph (`CurioIcons.Favorite`) in two tones —
  the bundled Material Symbols subset has no `favorite_border`, so an outline
  heart would draw the literal word.
- **Send no longer needs a reload:** `CommunityApi.comment` uses
  `return=representation` and the sheet inserts the server's own row, expands
  the branch it landed in and scrolls to it. `comments()` asks for the NEWEST
  page and re-sorts ascending. `SocialCommentsCache` keeps the whole row
  (edited stamp included) and the same 200-reply window as the read.
- **Friends:** a "Start a chat" strip at the bottom of the contacts sidebar —
  `RecentFriendTile` faces for the newest friendships (`SocialApi.friends`
  already orders by `responded_at desc`), each one tap from a conversation.
- **Copy:** a server that predates the app now says "Paste supabase/schema.sql"
  for a missing TABLE (PGRST205) and a missing embedded RELATIONSHIP (PGRST200)
  too, not just a missing function.

USER ACTION REQUIRED: re-paste `supabase/schema.sql` (Database → SQL Editor).
The new `community_comment_reactions` table (§4c) is what reply hearts live in;
until it exists, hearts report the paste-the-schema message. Threading, the
Friends strip and every other fix here work with or without the re-paste.

### Verified

- Delimiter balance + brace count on every edited file (python), stale-token
  grep (`rootId`, `expandedRoots`, `branchOrder`) returns nothing.
- No Gradle in this environment (root AGENTS rule); CI on this push is the
  compile check.

## Request (2026-09-16, COMPLETE — DM delivery, receipts, edits, session)

Verbatim: offline/slow network reports "jwt expired something"; an edit shows
as edited but the marker is gone when the chat is reopened and the edit never
reaches the other device; sending is slow and the new message only appears
after leaving and reopening the DM; the inbox sometimes shows a message as new
after it has been read; the double tick is faulty (it shows before the
receiver has read). Fix it all.

### Root causes found (in the code, not guessed)

1. **"JWT expired" was the SERVER's own text, rendered verbatim.**
   `SupabaseClient.executeBody` threw the raw `msg`, and `communityMessage`'s
   `failure is IllegalStateException -> raw` branch passed it straight to the
   UI. Nothing ever refreshed a 401: a token that expired while offline (or
   simply aged out before the refresh loop ran) failed EVERY action until
   something else happened to refresh the session.
2. **The double tick was a parsing bug, not a clock.** `optString` answers the
   STRING `"null"` for a JSON null, so an unread row's `read_at` parsed as
   `epochMillis("null")` = `0L` — not null — and the bubble draws `✓✓`
   whenever the receipt is not null. Every sent message claimed to be read.
3. **Reading a thread never stamped it.** `markRead` was only called when a NEW
   row arrived, so a conversation you opened and read stayed unread on the
   server: the inbox badge stayed up and the sender stayed on a single tick.
4. **An edit was invisible to every live path.** `messagesSince` filtered
   `created_at > anchor` only, and an edit does not move `created_at`; the
   realtime UPDATE hint existed but the revision sweep was the only thing that
   could apply it. The editor's own copy also looked right for one beat and
   then lost its marker: `load()` merged `(known + fresh)` with the CACHED row
   winning, and the cache's serialized shape never carried `edited_at` or
   `reply_to` at all.
5. **A session's bindings were never joined once the socket was open.**
   `SupabaseRealtime.watch` treated a brand-new owner as "nothing changed" and
   fell through to `connect()`, which returns early on an open socket — so the
   DM screen (entered after the inbox had already linked) heard NO pushes and
   fell back to a 20s safety tick. That is the "I have to reopen the chat"
   report, and it also meant every keystroke from the peer bought a 200-row
   page read (typing and reaction pushes raised the revision flag).
6. **A send cost a whole page re-read** (200 rows + reactions) and its
   optimistic bubble was dropped on any failure — including a read timeout,
   which is OUR deadline and not the server's refusal, so a delivered message
   could look lost until the chat was reopened.

### Shipped

- **Session:** `SupabaseClient` now refreshes and RE-SENDS once on a 401
  (`sessionRefresher` → `OnlineAccount.refreshForRetry`, blocking and on the
  REST layer's own IO thread, distincting offline (`Unreachable`) from a
  refused refresh), maps every refusal to honest copy, and uses generous
  timeouts (connect 15s, read/write 30s). `communityMessage` maps stale-session
  and missing-server-function text BEFORE its pass-through of written copy.
- **Receipts:** the `read_at` parse is fixed (`blankOrNull`), reading a thread
  stamps it on the server (paced retry), and the receipts ride the delta.
- **Edits/live thread:** `messagesSince` asks about `created_at` OR `edited_at`
  OR `read_at` per direction (one small request replaces the old delta + a
  separate receipt read), `pullDelta` merges in place (`applyMoved`), and a new
  page read happens only on a realtime revision hint (a DELETE has no stamp).
  Two realtime owners (`dm:<id>` messages, `dm:<id>:live` typing/reactions) and
  a union-signature re-join in `SupabaseRealtime`.
- **Cache/merge:** the cached row keeps `edited_at`/`reply_to`, and the
  SERVER's copy wins the merge (`fresh + known`).
- **Sends:** `sendPlaintext` returns the created row
  (`Prefer: return=representation`), so the real message replaces the stand-in
  with no page re-read; a transport failure runs `confirmDelivered` once before
  the draft is handed back.
- Safety tick 20s → 4s; docs (app/AGENTS.md, data/supabase/AGENTS.md),
  changelog and this log updated.

### Verified

- Brace/paren balance on all seven edited files (python), no leftover
  references to the removed `readStamps`/`RECEIPT_LIMIT`.
- No Gradle in this environment (root AGENTS rule); CI on this push is
  authoritative.

## Request (2026-09-16, COMPLETE — tag release fix: jpackage MSI version)

Verbatim: releasing the `v2` tag failed with `A problem occurred configuring
project ':desktop' > Illegal version for 'Msi': '2.1' is not a valid version` —
fix it, without tagging/releasing anything.

Root cause: the newest tag is `v2.1-beta6`, and `release.yml` exports
`RELEASE_VERSION` (the tag) for EVERY tag run. Gradle configures every project
before running `:app:assembleRelease`, so `:desktop`'s `packageVersion` was
validated too: the tag normalizes to `2.1`, and jpackage's MSI metadata needs
all three numeric components (`MAJOR.MINOR.BUILD`) — the config failure killed
the whole invocation, Android release included. The desktop module never even
had to be built for this to break.

Fix:
- `desktop/build.gradle.kts` — the tag is no longer trusted as-is. A small
  `jpackagePackageVersion` helper pads the numeric core to three components
  (`v2.1-beta6` -> `2.1.0`, `v2` -> `2.0.0`), keeps the range ceilings
  (255/255/65535), and returns null (module default 1.0.0 + a build warning)
  when a tag yields nothing jpackage accepts, so a malformed tag can never fail
  configuration again. Prerelease/build suffix stripping (v27u behavior) is
  kept. Verified against all nine existing tags: `1.0.1`, `1.2.0`, `2.1.0`, ...
- `.github/workflows/desktop-release.yml` — the release body's MSI row now uses
  the installer's ACTUAL file name (`CURIO_MSI_NAME`, published by the collect
  step) instead of re-deriving the old no-padding rule, which would have named
  `Curio-2.1.msi` while jpackage produced `Curio-2.1.0.msi`.
- `.github/AGENTS.md` — desktop workflow contract updated (normalization +
  fallback + the file-name-driven body).

No tag, release, or workflow run was triggered; the fix lands on `main` for the
next tag push to pick up.

## Request (2026-09-14, COMPLETE — schema.sql drop-order fix: delivery-mode trigger)

Re-pasting schema.sql failed with `2BP01: cannot drop function
curio_enforce_dm_delivery_mode() because other objects depend on it` — the
trigger `dm_messages_enforce_delivery_mode` on `dm_messages` still referenced
it. The §6d encryption-removal block dropped the function but never its
trigger. Fixed: `drop trigger if exists dm_messages_enforce_delivery_mode on
public.dm_messages;` now precedes the function drop (committed with the
trigger fix, pushed as `80c747be`). Audited the rest of the removal list:
`curio_validate_dm_envelope` and `curio_pin_dm_conversation_parties` have no
surviving triggers on the file's tables, so no other drop can hit 2BP01.
USER ACTION: re-paste schema.sql — it now runs clean.

## Request (2026-09-14, COMPLETE — CI fix: hideMember door + suspend-in-mapped)

CI failed on two errors from the moderation batch (`ad9a41b5`):

1. `SocialProfileScreen.kt:376/403 Unresolved 'hideMember'` (+ four
   cannot-infer-T fallout): the profile screen's Hide/Restore dialogs called
   `SocialApi.hideMember(...)`, but the ban RPC lives on **CommunityApi** —
   SocialApi only got `moderationStatus` and `findByUsername`. Fixed by
   pointing both calls at `CommunityApi.hideMember` (already imported).
2. `CommunityApi.kt:859 Suspension functions can only be called within
   coroutine body`: `isAdmin` invoked suspend `myAdminRow` inside the
   non-inline `mapped { }` lambda. Fixed by dropping the wrapper —
   `isAdmin = myAdminRow(...).map { it != null }`, which keeps the suspend
   identity legal and the same Result contract.

Lesson (same family as the 2026-09-13 one, now twice): a helper placed in the
wrong API object compiles at write time in the author's head and fails only in
CI — grep the receiver type of every new cross-file call before pushing.

## Request (2026-09-14, DONE — chat bubble/swipe/edit fixes + the community moderation system)

Verbatim asks (one message, many parts): the recent chat-bubble change is bad
— "the bubble for the receiver is on the right side now" and the "delete for
everyone" button is transparent (make it a solid fill); a long message cannot
be scrolled because "swipe to reply dominates"; rename Reported to Moderation
and make the moderation UI better; add more moderation tools, a reason when
deleting, comment reports and id reports; "when i try to report again, it doesnt
let me" — fix that; add report reasons for users; add an admins option with
permission options and make the current jugnu the owner; the comments view is
"kinda bad" — make the comment typing box match the UI with a better border,
don't show the @username in the sheet (name only) and make it compact; editing
fails with "column pgsrt_body.id does not exist"; the DM edit cannot edit and
the reply preview logic is glitchy — "use delete and replace logic for edits in
server".

### Confirmed with the user (ask_user)

- Edits: **"Edit in place via a server function"** (not delete+replace).
- Grantable permissions: Delete posts, Delete replies, Handle reports, Manage
  admins, Ban / unban members (all five shipped as independent switches).
- A ban: **"Hide their content only"** (the account keeps working).
- Re-reporting: **"Refresh my existing report"**.

### Root causes found (not guessed)

- The received bubble sat right because a weighted spacer pushed short rows to
  the end; the horizontal swipe-to-reply consumed vertical drags; the quote
  inside a bubble was hardcoded white; legacy plain rows rendered a
  placeholder instead of their words.
- `pgrst_body.id does not exist` came from a filtered PUT on a table whose
  PostgREST `body` column collides with the PUT's own `body` filter — and a
  PATCH the policies did not expose answered 204 over an unchanged row. Both
  edits now go through `security definer` server functions.
- "Report again" was refused by `community_reports`'s `(card_id, reporter)`
  unique constraint, and a report could only ever name a CARD.

### Shipped — commit 1 (`ac53cf30`, already pushed)

Chat bubbles (left/right), the vertical scroll vs. swipe fix, the solid
"Delete for everyone" chip, server-function edits for DMs AND replies (with the
new `edited_at` path), the legacy-body fallback, the compact reply sheet
(age instead of @username) and the app's own bordered typing box.

### Shipped — commit 2 (`ad9a41b5`, pushed) — the moderation system

- **Schema** (`supabase/schema.sql`): `community_reports` gains `comment_id` /
  `target_user` / `status` / `resolution` / `handled_by` / `handled_at` /
  `updated_at`, the one-target CHECK, per-kind partial unique indexes and the
  old `(card_id, reporter)` constraint dropped; `community_admins` gains
  `role` + five permission columns and seeds `@jugnu` as the protected OWNER;
  new RESTRICTIVE policies hide a banned member's cards/replies and block their
  posting; new `moderation_actions` audit table; and seven `security definer`
  functions (`curio_file_report`, `curio_handle_report`,
  `curio_moderate_remove_card` / `_remove_comment` / `_hide_member`,
  `curio_set_community_admin`, `curio_remove_community_admin`) plus the
  `curio_admin_can` / `curio_member_hidden` tests.
- **API**: `CommunityReport` now names a card, a reply or a member and carries
  its status/resolution; `CommunityAdminRow` + `CommunityReportReasons` +
  `ModerationReasons`; new calls `cardsByIds` / `commentsByIds` / `myAdminRow` /
  `admins` / `setAdmin` / `removeAdmin` / `handleReport` /
  `removeCardWithReason` / `removeCommentWithReason` (all RPCs);
  `SocialApi.findByUsername` / `hideMember` / `moderationStatus`.
- **UI**: `ModerationScreen.kt` replaces `ReportedScreen.kt` (route
  `CurioRoutes.MODERATION`) — a queue with Open/All, per-report content preview,
  Remove / Hide author / Dismiss / Reopen, and a Team half (add by @username,
  five switches each, owner protected); `ModerationDialogs.kt` (the shared
  member report sheet + the moderator reason sheet); a reply row now offers
  Report to members and Remove-with-reason to moderators; a post's page offers
  the same moderator removal; a profile's ⋮ offers Report member and
  Hide/Restore member; a hidden member sees WHY on the wall.

### Needs the user

- **Re-paste `supabase/schema.sql`** (Dashboard → SQL Editor → Run; idempotent).
  Until then the new queue/team calls will fail — the old schema has none of
  the RPCs, the new report columns or the owner seed.

## Request (2026-09-14, COMPLETE — social card render fix + topic-only profile tiles)

User pointed at commit `541c3c6` (Sep 12): the topic card rendered fine in the
Social page there, but now "the social topic share card rendering is so much
worse" — bad in the profile grid, bad in the post topic preview, "etc etc" —
and asked for the profile preview to become "just the topic with topic icon no
rendering there".

### Root cause (found by walking the commit range, not by guessing)

`CommunityCardCanvas` (CommunityScreen.kt) renders the real share card scaled
as a LAYER. Two separate regressions landed after `541c3c6`:

1. `19f17ce7` replaced the old `<size> + graphicsLayer` wrapper with
   `requiredSize(...)` on BOTH the footprint and the art, and `69e5113b`
   replaced the fill-mode `TopStart`/`TopCenter` alignment with plain
   `Alignment.Center`. Net effect: every surface drew a card whose content was
   laid out at the 405×720 design geometry and then shrunk 0.77× — text that
   is 14–23% smaller *relative to the card* than the version the user liked,
   which is exactly the reported "too small" — and the square profile tile
   centre-cropped the art (title gone, card cut in half).
2. `3acd25ab` shrank the wall card to 0.88 of its row and the composer preview
   to 0.72, which piled more dead space around art that was already reading
   small.

The card is authored for a DIRECT layout: the share sheet renders its own
preview at a fixed 280dp width with no layer transform, and every smart-fit
constant (`factAvailHeightDp`, `factLineHeightDp`, the auto-tall detector) is
calibrated on that 280dp base. Layer-scaling it to a different width changes
the ratio of type to card — that is the whole "too small"/"not the real card"
complaint.

### Confirmed with the user (ask_user)

- What is wrong now: *too small with dead space*, *cut off / cropped* and
  *off-centre / not aligned*.
- Sizing: "both at the sep 12 style look but more smaller also less empty
  space below them for the like and dislike".

### Shipped

- `CommunityCardCanvas` rewritten (CommunityScreen.kt): the card is laid out
  DIRECTLY at the width its row offers — `width(targetWidth).aspectRatio(h/w)`
  with the target capped at the card's design width (405/450dp) — instead of a
  layer-scaled miniature. No crop, no dead band, no double scaling; the card's
  own workspace/fit handles the size (the editor's own 280dp base). The square
  "fill" crop mode is retired with the profile tile that used it.
- Wall: `FEED_CARD_WIDTH` 0.88f → 0.78f (a ~277dp card on a phone, just under
  the editor's base) and the item's uniform spacing replaced with explicit
  8dp seams and a 4dp seam between the art and the like/dislike row.
- Composer preview: 0.72f → 0.85f, so the live preview is the wall's card.
- `SocialProfileTile`: a TOPIC post previews as the topic — lane accent wash
  (lifted twin in dark mode), lane glyph in a chip, topic name — with no card
  art in the grid. Notes and quotes keep their word tiles.
- `app/AGENTS.md`: the direct-layout contract, the wall/preview/own-page
  fractions, and the topic-tile profile preview. Changelog tightened (the
  stale 88%/compact/cropped bullets replaced).

### Verified

- Brace/paren balance on all three edited Kotlin files (python).
- Grepped every removed symbol (`requiredSize`, `TransformOrigin`,
  `LocalDensity`) — no references left; `aspectRatio`/`height` imports added
  and used; `parseAccent` is `internal` in the same package for the tile.
- No Gradle in this environment (root AGENTS rule); CI on this push is
  authoritative.

## Request (2026-09-14, COMPLETE — CI fix + DM header/ticks/scroll polish)

User pasted the CI log (Unresolved 'mineGlyph'/'others' — my MessageBubble
rewrite dropped the two vals while moving the color logic) and asked: remove
the em dash + the "gone in 24 hours" hint from the Social/Chats heroes; the DM
header must show the peer's @username under the name; the Seen mark lies (says
Seen before a read) — make it WhatsApp-style ticks; the thread must stay
pinned to the bottom as messages send.

Shipped (one commit):
- DirectMessageScreen: mineGlyph/others restored (the CI fix); per-row tick
  language (1 tick = sent, ✓✓ in accent = read; receipt passed per-row, the
  newest-row seenIndex inference deleted); hero subtitle = the peer's
  @handleLabel (Typing… keeps priority, wide + narrow paths); auto-scroll
  pinned to newest with the yank guard (no scroll while the member has
  scrolled up to read history).
- CommunityScreen + ChatsScreen: expiry hint subtitles emptied, the "Posted"
  notice shortened, em-dash phrasing cleaned in the changelog.
- Changelog updated (ticks + header + auto-scroll + cleaner headers).

Lesson (repeat of the brace lesson): deleting a line that carries vals while
restructuring a composable body must be re-verified against the body's own
reads — count braces AND grep every symbol the old body named.

## Request (2026-09-14, COMPLETE — composer remake + DM Instagram pass)

User asks (post-PR-129 review): the new post screen — is it good and wired?
Remove it and remake it. Plus: swipe-to-reply in DMs; the reaction/edits dialog
is bad — make it Instagram style; give send/receive bubbles distinct colors
and make the bubble better.

Decisions confirmed with the user (ask_user):
- Replies are TRUE threaded rows (schema change accepted; user re-pastes
  schema.sql), not quote-text.
- Gestures are Instagram-exact: double-tap = heart; hold = floating bar with
  the emoji palette + Copy/Edit/Remove; no dialogs.
- The old bottom-sheet composer is DELETED (one composer only).

Shipped:
- Schema §5e: `dm_messages.reply_to` (FK, on delete set null) + index +
  `curio_check_dm_reply` trigger (same-conversation, one level deep, no
  self-reply). USER ACTION REQUIRED: re-paste supabase/schema.sql.
- SocialApi: `reply_to` in MESSAGE_COLUMNS + `CurioDirectMessage.replyTo`,
  `replyTo` param on sendPlaintext/sendEncrypted, `replyPreview()` read.
- DirectMessageScreen: swipe-to-reply (bubble leans, 60% threshold arms the
  composer's reply banner), double-tap heart, hold = `MessageActionSheet`
  (floating pills, no dialog — the old AlertDialog is deleted), quote inside
  the bubble (`ReplyQuoteRow`), screen-level `replyQuotes` map fetching
  out-of-window parents OUTSIDE composition (no suspend-in-composition),
  distinct fills: mine = `curioDialogActionColor()` (brand rose), theirs =
  neutral raised surface, both opaque + 1dp shadow.
- CommunityPostScreen.kt remade (full-screen composer, all helpers restored
  from the merged version with CI's categorySlug fix; keyboard options +
  AnimatedContent polish added) and wired: the wall's floating Post button
  opens it directly. Old `CommunityComposerSheet` (540 lines) + its private
  helpers (ComposerPill, quickFactOf, TopicPickRow, hexOf) deleted; seed
  params had no external callers (verified).
- Changelog 20260921.txt updated (4 new ADD bullets).
- app/AGENTS.md: posting-door contract + new DM-gestures contract updated.

Notes for the next pass:
- The realtime delta pull unchanged: replies arrive as normal dm_messages
  rows (reply_to rides MESSAGE_COLUMNS), so threading needs no new bindings.
- React-toggle semantics: pickReactionScreen already toggles; the sheet's
  palette and the double-tap both land there.
- CI on this push is authoritative (no Gradle in this environment).

## Request (2026-09-14, COMPLETE — PR #129 merge review)

User asked to review https://github.com/firefly-sylestia/Curio/pull/129
(feat: Curio Alive animations + Phase 4 social polish, merged as 4fb0012)
and confirm everything is alright. Review only — no code changes.

Findings:
- Merge itself is sound: 6/6 checks green at merge, local checkout already
  contains it, and the stale `categorySlug` CI error was fixed inside the PR
  (4a53ff41). Curio Alive ships correctly as an opt-in Experiments toggle with
  classic motion preserved when OFF. The notification-ID change (per-peer/card
  hashed ids over the 7311/7312 bases) is collision-safe across channels and
  keeps the POST_NOTIFICATIONS guard. Scope discipline held: Android only.
- ⚠️ The full-screen composer (CommunityPostScreen.kt, 716 lines) is merged
  but NOT reachable: the only wiring commit (c1e2ac5a, CommunityScreenEntry.kt)
  was reverted in 005dce82 shortly after. The wall still opens the old
  CommunityComposerSheet bottom sheet in CommunityScreen.kt, so the PR's
  headline composer claim is not user-live. Asked the user whether the revert
  was intentional.
- ⚠️ No fastlane changelog entry for the PR's user-visible changes (Alive
  toggle, notification improvements, icon/nav fixes) — 20260921.txt untouched
  by the merge.
- CI on HEAD (the revert commit 005dce82) was still in progress at review
  time; the merge commit itself is green. The revert only deletes an
  unreferenced file, so risk is minimal.
- Minor: CurioAlivePreferences.enabledState is consumed only by
  UserExperimentsScreen (which seeds it on entry); all motion call sites read
  isEnabled(context) directly — correct, slightly fragile.

## Request (2026-09-14, IN PROGRESS — full-screen composer + canonical social card polish)

User asked to continue the same Phase 4 branch after the composer redesign, fix the remaining CI compiler error, and continue the quality pass. The Social post composer should remain a dedicated full-screen creation flow rather than a bottom sheet, and topic posts must render using the exact share-card visual system rather than a lookalike.

Latest CI error received:
- `CommunityPostScreen.kt:693:9 No parameter with name 'categorySlug' found.`

Fix applied:
- Removed the stale `categorySlug` argument from the `CommunityCard(...)` preview construction. `CommunityCardDraft` still retains its `categorySlug` field because the draft/API model uses it; only `CommunityCard` no longer receives it.
- Restored the full composer implementation after the correction rather than keeping an accidental simplified rewrite.

Remaining quality pass:
- Watch authoritative Android CI for the latest commit before calling the branch green.
- Verify the topic renderer at each display size against the exact 405×720 / 450×600 logical share-card geometry used by export. Avoid double-scaling or alternate card geometry.
- Keep the same canonical `TopicShareCard` implementation for composer, feed, full-card view and export; improve only the surrounding sizing/measurement wrapper where needed.
- Add tasteful Alive-aware tactics: selection morphs, stronger but controlled Post readiness/press feedback, animated style/aspect choices where useful, smoother topic-picker transitions, and keyboard-aware editing without turning the UI into motion noise.
- Update this log again when the social polish batch is complete.

## Request (2026-09-13, LATEST: composer redesign + DM feel + icon marks)

User asks (summarised): the community "post a topic" sheet feels like filling
in forms — rebuild it (write-first, notes default); chats/friends icons don't
match; hide the red tester-looking error notes; sending feels glitchy (bubble
vanishes then returns); bubbles are translucent with a bad shape and no
animation; tapping bubbles should NOT open emoji/delete (that belongs to hold);
the DM header says "Curious Explorer · Private messages" instead of showing the
person; the share-card preview sits too far left on the wall and top-left in
the profile grid; profile post-count text unreadable.

Done in this batch:
- CommunityCardCanvas: painted footprint box (requiredSize cardWidth*scale ×
  cardHeight*scale) inside a CENTERING parent — kills the left-hug on wide
  walls and the top-left pin in fill mode (profile grid TopCenter).
- Composer: full rewrite in place. Opens on a borderless writer (Note default),
  live canvas crossfades Note/Topic/Quote as you type, Post top-right with a
  scale pop when valid, counter only appears past 80% of budget, topic/style/
  shape/credit are compact pills with inline trays, focus requester arms the
  keyboard. The two-step Preview dialog is GONE. FAB label now "Post".
- Icon font rebuilt: chat_bubble, groups, send, more_horiz ligatures merged
  from the full Material Symbols font (gids-only subset + feaLib-compiled
  liga for exactly the retained icons; verified every name shapes to one
  outlined glyph). CurioIcons.Chats/Friends/Send/MoreHoriz added; wall door
  tiles + empty cards use them.
- DM: hero header now carries the peer (avatar + name + @handle via
  titleTrailing; Typing… replaces the handle live). MessagePeerHeader card
  stays below for taps.
- DM send flicker: sentShadow stage — an optimistic bubble moves to the
  shadow list when the request succeeds and retires only when a real server
  row with the same words lands, so the bubble never disappears.
- Bubbles: opaque fills (light: primary vs warm paper; dark: deep rose vs
  raised surface), tighter tail rounding, press squish + long-press haptic;
  tap = reactions only, HOLD = the action dialog which now hosts the reaction
  palette inline (pickReactionScreen hoisted to screen scope for the dialog).
- Red tester errors: SocialNote + the wall/card/profile error lines are quiet
  on-surface ink with a small warning glyph.
- Profile stats: numbers now onSurface (rose washed out on the page), streak
  flame matches.

## Request (2026-09-13, IN PROGRESS — encryption envelope fix + redesign batch)

### Batch A: the envelope failure (user: "it still says encrypted message is
missing a device envelope for its key version")

Root cause chain (server-side completeness check vs. device lifecycle):
1. `curio_enforce_dm_message_envelopes` requires an envelope for EVERY active
   device of BOTH participants at the message's key version.
2. A reinstall publishes a SECOND active device row (nothing ever retires the
   old one), so the sender must wrap a key for hardware that no longer holds
   the account. Permanent failure once a stale row exists.
3. A device registered moments before the send (friend's second phone, or the
   publisher itself on a reinstall) had no envelope yet, failing a send the
   sender had done correctly.

Fix (all three legs):
- Schema: the envelope check now gives devices registered in the last TWO
  MINUTES a grace (the publisher wraps and stores everything BEFORE pushing
  its row, so a brand-new device between those steps is not a sender's
  fault); devices older than the grace are still strictly checked. New
  `curio_retire_dm_device(text)` security-definer RPC (own rows only, grants
  to authenticated; the older two-arg prototype is dropped).
- SocialApi: `retireDmDevice` calls the RPC.
- DirectMessageScreen: BOTH `load()` and the encrypted send path retire every
  OTHER active identity of MINE before publishing, so one device per side is
  active; the friend's rows are theirs. Old envelopes stay historically
  valid (retired devices keep their delivered envelopes for reads).
- Encrypted-failure UX: the inline advice line is replaced by a DIALOG
  (SocialConfirmDialog, non-destructive) that states the reason, states the
  version-mismatch reality, and offers "Send without encryption" — which
  flips the shared mode off for BOTH, then sends the failed text (kept in
  `failedDraft`) with the optimistic bubble reused. Plaintext failures keep
  the plain error line. `encryptedSendAdvice` is gone.

Batch A needs: re-paste schema.sql (envelope grace + retirement RPC).

### Batch B (this push, then watch CI once at the end) — SHIPPED
- Profile grid: tiles are square (aspectRatio 1f) and the card canvas gained a
  FILL mode (`widthFraction = 0f`: scale driven by height, top-aligned crop), so
  a share card fills its tile instead of floating small. Notes and quotes fill
  the same square with words pinned to the bottom.
- Profile header: Instagram shape — portrait + identity row, counts as three
  centred COLUMNS beneath, bio, then a full-width action. The privacy
  narration paragraph and the two explanatory status texts are gone (the
  action row states everything now: Edit profile / Message / Request sent /
  Add friend); blocking stays behind the ⋮. "Edit profile" on your own
  profile navigates to CurioRoutes.PROFILE.
- Composer: two steps. Step one is the compact sheet (kind chips, topic
  search/pick, words, style). "Preview" opens a full-height Dialog editor:
  the card drawn via CommunityCardCanvas at full width (SocialTextPost still,
  now nullable-onClick), SHAPE chips (PORTRAIT/CLASSIC — new state), caption,
  credit and wording all editable, Post from the header. The preview draws a
  CommunityCard rebuilt from the draft.
- Wall: rows animate in with `Modifier.animateItem()` inside a Box; take
  down/report are ICON-only pills (blank label, richer contentDescription,
  square padding); the stale duplicate-author comment removed.
- SocialTextPost.onClick is now nullable (preview renders it as a still).

CI fix carried in this push: the encryption dialog referenced activeToken /
activeUserId, which only exist inside the eligible branch; it now reads
token / myUserId from the screen's own state and bails cleanly without a
session.

## Previous request (shipped: d9bcf3ed, cfc8fb4c, 0c530794 — CI green)

Profile + settings polish, account lifecycle, encryption default — see git
log for the details; the CI-lesson notes (suspension-in-mapped, the Row
brace) are recorded below in Verification status.

### User asks, verbatim intent

1. Profile page: remove the bio/streak card **below** achievements; bio and
   streak stay in the hero only.
2. Edit profile: background must match theme colours, not coffee-cream;
   professional look and proper animations; move the profile ICON picker to
   where the "Profile picture" text is (keep the photo, the Add photo pill and
   the circle); remove unnecessary texts; the "that is already your username"
   line must not show permanently; a better username editor; add Sign out.
3. Settings: remove the Privacy card; put the privacy options inside the
   Online mode screen. Turning Online mode ON should bring the Social tab with
   it.
4. Username ownership: after logout the old handle stayed on the device (the
   next account inherited it and its chats). Fix the leak.
5. New accounts must get a random usable username automatically (otherwise
   they cannot add friends at all).
6. Sign-up flow: with email confirmation ON, the form said "something went
   wrong" and the user had to switch to sign-in manually to be told to confirm
   their email. Fix the flow.
7. Encryption OFF by default; if an encrypted send fails, advise turning
   encryption off and say encryption is being reworked/discontinued.
8. Messages must send even when the receiver has not updated (version
   mismatch) — covered by 7 (plaintext default) + the trigger accepting
   `legacy`.
9. Tell the user what else is missing. No em dashes.
10. From an earlier turn, still pending: fix the CI error, then use ask_user
    for tests AFTER the task is finished (not now).

### Shipped in this batch (uncommitted until now)

1. **ProfileScreen.kt** — the bio/streak card under achievements is gone (the
   hero keeps both: tagline = bio, streak pill). The duplicated card was
   saying the same thing twice in one scroll.
2. **Edit profile dialog** (ProfileScreen.kt + CurioTheme.kt +
   CurioAccountComponents.kt):
   - `curioProfileDialogColor()` — the dialog container is the page's own
     surface with the brand rose breathed in (light), or the settings-glass
     construction (dark). The tan `surfaceContainerHigh` read as a
     coffee-cream slab on this page.
   - Body arrives with a fade + small rise (one `MutableTransitionState`
     reveal, the app's sheet motion), and the body scrolls now that the
     account section lives here.
   - The ICON picker moved UP beside the photo: one "Profile icon" section
     holds the 84dp circle (photo or initial), the Add/Change/Remove photo
     pills and the portrait picker row. `CurioAccountIdentityCard` gained
     `includeAvatarPicker = false` for this page, and the picker itself is now
     the shared `SocialAvatarPickerRow` (28 tiles) used by both surfaces.
   - Unnecessary texts removed: the "icon travels with you / nothing is
     uploaded" helper lines are gone (one short line remains under the
     section label).
   - Sign out is in the dialog under Account (destructive pill, confirmed by
     the existing `SocialConfirmDialog` in the screen), beside Privacy (which
     navigates to `SETTINGS_PRIVACY` and closes the dialog first).
3. **Username editor** (CurioAccountComponents.kt) — the permanent "That is
   already your username." status is gone: the line under the field now only
   appears when there is something to say (a broken rule, the server's
   verdict, a free-to-claim hint, or the sign-in nudge). The helper above the
   field is adaptive: an unnamed account is invited to claim one; a named
   account sees "You are @handle." The redundant trailing "Friends find you by
   this name." text is gone.
4. **Privacy merge** (PrivacyScreen.kt rewritten + OnlineModeScreen.kt +
   SettingsHubScreen.kt) — `SocialPrivacyOptions()` is one shared composable
   hosted on the Online mode page (a Privacy section under Social). The
   settings hub's Privacy card, deep-link row and rail entry are removed; the
   `SETTINGS_PRIVACY` route still exists and renders the same options as a
   page for the Edit profile shortcut.
5. **Online mode brings Social** (OnlineAccount.kt) — `enableOnlineMode()`
   turns the community tab on only on a real off-to-on transition (a member
   who switched the tab off on purpose keeps it off until they cycle Online
   mode). Sign-in, sign-up, restore and `setOnlineMode(true)` all go through
   it.
6. **Sign-up email-confirmation flow** (SupabaseClient.kt +
   OnlineAccount.kt) — `signUp` parsed every response as a session and threw
   on the missing `access_token` when the project has email confirmation ON,
   which surfaced as "Something went wrong". It now returns `null` (a success
   with a step left), and the form shows "Account created. Confirm the link we
   emailed to …, then sign in with the same email."
7. **Username leak on sign-out** (OnlineAccount.kt) — `signOut` now clears
   the local username; the handle belongs to the account, not the device.
8. **Reconcile on sign-in** (OnlineAccount.kt) — `publishIdentity` +
   `restoreProfileIdentity` are one `reconcileIdentity`: the account's handle
   wins (locally cleared or not), the account's name/bio are only filled FROM
   the device when the account has none, and a device name is pushed only
   into an empty account, so a sign-in can never rewrite an existing member's
   name.
9. **Generated usernames** (SocialApi.kt + OnlineAccount.kt) —
   `suggestUsername()` (word_word_digits, ≤20 chars) and
   `claimGeneratedUsername()` give an account with no handle one at
   sign-in/restore, so adding friends works from the first minute. One
   attempt: the rename cooldown would refuse a second try anyway.
10. **Encryption OFF by default** (schema.sql + SocialApi.kt +
    DirectMessageScreen.kt) — `encryption_enabled` defaults to false (column +
    trigger), a conversation with NO row is plaintext (the trigger's
    `coalesce` fallback flipped from true to false), and `legacy` is accepted
    beside `plaintext` while off, which is what fixes "the message doesn't
    send until the receiver updates". Existing rows keep their mode (the
    alter only changes the default).
11. **Failed encrypted send advice** (DirectMessageScreen.kt) — an encrypted
    failure appends the actionable line: both people need a version that
    supports it, turn encryption off for this chat to keep messaging, and
    encryption is experimental and may be withdrawn. Plaintext failures are
    not dressed up with encryption advice.

### Verification status

- Braces balance-checked (python) on ProfileScreen.kt and PrivacyScreen.kt.
- Same-package symbols verified (SettingsOptionRow family, ChatsScreen
  import fix already committed in `1eef908a`).
- No Gradle build is allowed in this environment (root AGENTS rule); CI
  typechecks on push.
- **ask_user for tests is owed after this task closes** (user directive).
- CI on `d9bcf3ed`'s parent (`1eef908a`) failed on
  `SocialApi.kt:741 Suspension functions can only be called within coroutine
  body`: the batch had made `hiddenConversations` a suspend helper while its
  only caller invokes it inside the non-suspend `mapped {}` lambda. Fixed by
  making it blocking like the file's own convention (`presenceOf`), which is
  safe because the only caller is already on the IO dispatcher.
- CI on that fix then failed across `CurioAccountComponents.kt` and
  `ProfileScreen.kt` with dozens of "Unresolved reference": removing the
  username row's trailing text had also removed the ROW's closing brace, so
  every helper defined later in the file became a local function of
  `CurioAccountIdentityCard` ("Modifier 'internal' is not applicable to 'local
  function'" was the tell). One restored brace fixes all of it. Lesson: a
  str_replace that deletes trailing content must account for the braces the
  deleted block was carrying.

### Still open (tracked, next slices)

- Post-a-topic composer: two-step flow (quick sheet → full-screen editor),
  compact professional redesign, proper animations.
- Comment sheet redesign.
- Share-card editor → "Share to social" (+ share with link) in the share
  dialog.
- Realtime reply notifications (`community_comments` for my cards, `dm_messages`
  while another screen is open).
- DM send animation polish.

### ⚠️ Needs the user

- Re-paste `supabase/schema.sql` (Database → SQL Editor → Run, idempotent):
  this batch changes the encryption default + trigger fallback, and the
  earlier §5h `curio_delete_dm_conversation()` is in the same file.

## Archive

- The social restructure slice (Friends ↔ Chats split, conversation deletion,
  presence, icons) shipped in `1296dca0` / `7d656caf` / `1eef908a`.
- The social-perf slice is `abebf625`; the DM envelope fix is `4373edfc`.
- `docs/REALTIME_SETUP.md` + the `supabase/AGENTS.md` pointer remain LOCAL
  (unpushed) by the user's explicit request.

## Shipped in this batch (2026-09-13, latest)

Encrypted sends: the server is the bookkeeper now — a new RPC
`curio_dm_missing_envelopes(conversation, version)` returns the devices still
missing an envelope WITH their public keys (same rules as the insert trigger,
grace window included), so the client wraps exactly what the server demands
instead of guessing from a friend-key read that RLS may have emptied. Publish
RPC now also retires the account's other devices, keeping one-identity-per-
account an invariant instead of a hope.

Feature opt-in: "Encrypted messages" switch in Settings → Online mode gates
the per-chat pill (hidden unless opted in; a chat already ON keeps its pill).

Gestures: messages act on HOLD (Copy / Edit for my plaintext / Remove); the
old always-visible Delete-for-me + Unsend pills are gone from the reading
flow. Replies gain Edit ("edited" mark, server-stamped). Branches: answers
render under their root, long branches fold behind "show N more".

Surfaces: community profile header wears the app's stat style (rose numbers
over labels), shows the bio, adds the streak (flame + days) on your own page.
Post previews fill their square tile completely (requiredSize + cover scale —
the old `size` was coerced into the tile constraints and then scaled down,
which is where quarter-size previews and the wall's dead space came from).
The card page gains the dislike pill; likes and dislikes POP (spring).

USER ACTION REQUIRED: re-paste supabase/schema.sql — the new publish +
missing-envelopes RPCs, comment edit column/trigger/policy, dm_messages
edited_at + edit guard/policy are all in the file but NOT live until pasted.

## User prompts

### Prompt (2026-09-16, ANSWERED — push approved; Follow members + Quote-repost chosen as the next build)

The verification ask came back: **"Push everything now"**, **Follow members**
and **Quote-repost any post** (bookmark, wall search, notifications, pin,
ban-list search and the draft pulse were offered and not picked). Push first,
then build the two picks on top.

### Prompt (2026-09-16, DONE in this push — `477e0d08`) — your own writing: journals, books, the writing canvas, Add take

Verbatim: take the `feature/create-entry-flow` ideas "but our design" — a **+**
on Home that disappears on scroll, opening a sheet that starts a **journal** or a
**book**; journals and books get their **own store and their own unique saved
view**, independent of the app's saved detail view, editable and viewable in that
same page with **persistent auto-save** for app switches; the book reader is a
**shelf** of covers with the name and progress under each, and opening one shows
the chapter reviews written or lets you write them in place; a personal collection
with its own small view; the journal has date + date changer, emotion, title and
writing with **bold / italic / underline / strikethrough / quote / left**
formatting and photos in the same canvas, tools shown **above the keyboard as
icons** applying to **the line the tool was used on** (selection, else the line),
the writing never hiding under the keyboard; bring back the **"Add take"** row
action from that commit (the picker stays, the new take lands beside the previous
ones); and on Home remove the saved row and show **journal + book chips in a
horizontal row**, small and fixed-shape, in the app accent ("not the cream
colors"). Then: fix the CI compile breaks and the topic browser's scrolling and
loading, push those, "then finish the result but dont push untill i say so".

Status: DONE in this push (the compile breaks + browser fixes went out first as
the user asked, through their own `fix cl` commit `c82b9e7c`). Decisions taken
with ask_user: the Saved section goes AND saved captures leave Home's recents;
the personal collection lives in its own store and shows on the Cabinet's
Personal shelf; books are found online with a manual fallback; photos are inline
blocks; a tool applies to the selection else the line; the feature is always on;
and **push + wire the Cabinet shelf first**. Still open and named in the request
log: the personal store is not in Backup & restore, and a photo is a block (it
splits the paragraph) rather than a picture flowing mid-sentence.

### Prompt (2026-09-14, DONE in this push) — chat bubbles/swipe/edit + the moderation system

Verbatim: the chat bubble for the receiver sits on the right and the "delete
for everyone" button is transparent (make it solid); a long message cannot be
scrolled above because swipe-to-reply dominates; rename Reported to Moderation,
making the moderation UI better; add more moderation tools, a reason when
deleting, comment reports and id reports; re-reporting is refused; add report
reasons for users; add an admins option with permission options and make the
current jugnu the owner; the comments view is bad — the typing box should match
the UI with a better border, the sheet should show the name and not the
username, and be compact; editing says "column pgsrt_body.id does not exist";
the DM edit cannot edit and the reply preview is glitchy — "use delete and
replace logic for edits in server".

Status: DONE. Chat bubble/swipe/solid-chip/edit-path/reply-sheet work pushed as
`ac53cf30`; the moderation system (schema + API + `ModerationScreen` + dialogs +
report/hide/restore doors) pushed as `ad9a41b5`. Decided with ask_user: edit in
place via a server function, the five granular permissions, hide-content-only
bans, and a repeat report REFRESHES the reporter's own row.

USER ACTION REQUIRED: re-paste `supabase/schema.sql` for the moderation schema
to go live.

### Prompt (2026-09-14, DONE) — social card render regression + profile "topic with topic icon"

Verbatim: at commit `541c3c6` the topic card rendered fine in the Social page,
but today "the social topic share card rendering is so much worse" — bad in the
profile grid, bad in the post topic preview, "etc etc" — fix it, and in the
profile change the preview to "just the topic with topic icon no rendering
there".

Status: DONE in this push (see the request log at the top: the canvas lays the
card out directly at the row's width, the wall/preview sizes were retuned with
a tighter seam above the like/dislike row, and the profile grid tile is the
topic's own colour, glyph and name). Also closes items (2) and (3) of the
2026-09-13 prompt below.

### Prompt (2026-09-13) — DM header side, card render accuracy, composer topic search + preview

Verbatim asks: (1) the chat screen "Curious Explorer" header shows on the RIGHT
side which is wrong; (2) the share-card render is still wrong — not accurate to
the real card, sits too far left and cuts out of the screen, same in the
profile grid view; (3) the post bottom sheet is still bad — the TOPIC posting
flow is bad, "where did the topic search go", the card preview is bad, "the
previous one was better, at least I was able to choose the card"; (4) a BLANK
BOX appears above the "What's catching your eye?" writer — remove it; (5)
finish the previous request too, be faster; prompt logged here FIRST before
implementation.

Status: (2) card render accuracy DONE 2026-09-14 (card laid out at the row's
width, no crop, no dead band; profile tile is topic + glyph). (3) the flow it
describes no longer exists: the bottom sheet was replaced by the full-screen
`CommunityPostScreen`, which HAS the topic search and now previews the card at
the wall's own size. (1) the DM header is person-first now: the peer's portrait,
name and @username ride the title bar with a live "Typing…" line. (4) the
blank box it saw came from that deleted sheet — re-check on the current
composer before acting. (5) superseded by the batches above.

### Prompt (2026-09-13, latest done) — identity publish RLS failure + door tile captions

Symptoms: "this device could not register its encrypted-message identity" even
with encryption off, and messages not going through. Root causes found: (1) the
schema file carried a stray `drop policy if exists
dm_device_keys_select_participant` near the retire RPC, so every re-paste
silently deleted the friend key-read policy; (2) the client published the
device identity with a raw REST upsert that fails the moment the live
database's policies drift from the file; (3) `load()` aborted rendering the
whole page when the publish failed, so even a successful plaintext send never
appeared. Fixes: publish now goes through a new security-definer RPC
(`curio_publish_dm_device`), the stray drop is removed and guarded by comment,
and `load()` degrades gracefully when registration fails (plaintext always
works; ciphertext says why). Also removed the door tiles' subtitles per the
user's request.

USER ACTION REQUIRED: the schema changes are not live until the user re-pastes
supabase/schema.sql into the Supabase dashboard (or at minimum runs the retire
+ publish RPC definitions). The client works with or without the re-paste
except that encrypted sends need the RPC present.

### Prompt (2026-09-13, later) — shipped in c08fdb54 + this working tree

The chats/friends doors were bare text with mismatched icons; an open post kept
the bottom nav and showed a full "Report" button; the card and profile pages
had too much empty space below. Shipped: door tiles with icons, nav bar hidden
on the card route (it shares the wall's prefix), report/take-down icon-only on
the card page, replies inline under the post (same list, same realtime), and

the tile cover-mode crop that keeps a card's title visible.

Profile polish (remove the bio/streak card below achievements; keep both in
the hero), a professional themed Edit profile with the icon picker beside the
photo, fewer texts, a better username editor and Sign out; Privacy merged into
Online mode; Online mode brings the Social tab; sign-out must forget the
username; generated usernames for new accounts; a working sign-up-with-email-
confirmation flow; encryption off by default with honest failure advice; tell
the user what else is missing. No em dashes. After the task: ask_user for
tests.

### Next prompt (the next instruction goes here — never cleared by an agent)

now lets fix the chat messege notifications, with proper avatar view and like reply and mute options. and reply opens in notification messege box which sends the messege too with reply attached with the last messege, and now the journal and book the new pages th journals saved page too and books sheleve too. they all are rechiing the statsus bar, every new page u added in that check and fix, then the bold option doesnt work it works with italic but doesnt show as working on its own, the quote just hihglights the text instead it should show the quote style a little smaller text with bold look if user wants, also fix its buttons accent and colors they dont match the theme and colors of the hero, also per new line it says write write which is bad view fix that too. also the button sheet of the + fix colors and proper darker accent for icons in ligh mode, and then for chapter write open a page like journal style for chapters not inside editing, show previe in inline also in the saved view, dont show all the mood icons or the fomat icons show a proper view olny and when editing smoothly show the options in the same page with animations and fix the colors and all too also the book chapters auto fetch if possible with pages if thats possible too. and its synopsis too if any provides, or if its in app then use that info. fully refine it and make it professional. also fix icons cutting
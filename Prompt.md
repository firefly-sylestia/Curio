# Prompt Log — current request

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

_No pending prompt._

# Prompt Log — current request

## Request (2026-09-14, IN PROGRESS — full-screen composer + canonical social card polish)

User asked to continue the same Phase 4 branch after the composer redesign, fix the remaining CI compiler error, and continue the quality pass. The Social post composer should remain a dedicated full-screen creation flow rather than a bottom sheet, and topic posts must render using the exact share-card visual system rather than a lookalike.

Latest CI error received:
- `CommunityPostScreen.kt:693:9 No parameter with name 'categorySlug' found.`

Fix applied:
- Removed the stale `categorySlug` argument from the `CommunityCard(...)` preview construction. `CommunityCardDraft` still retains its `categorySlug` field because the draft/API model uses it; only `CommunityCard` no longer receives it.
- Restored the full composer implementation after the correction rather than keeping an accidental simplified rewrite.

Current composer direction:
- Full-screen Dialog creation destination.
- Note / Topic / Quote modes.
- Large live Topic preview through `CommunityCardCanvas`.
- Topic search/pick, Look, Shape and Text size controls.
- Writer focus, character limits, live preview, haptic topic selection and a springing Post action.
- Topic preview continues to use the real `CommunityCardCanvas` / `TopicShareCard` renderer instead of introducing a second visual implementation.

Remaining quality pass:
- Watch authoritative Android CI for the latest commit before calling the branch green.
- Verify the topic renderer at each display size against the exact 405×720 / 450×600 logical share-card geometry used by export. Avoid double-scaling or alternate card geometry.
- Keep the same canonical `TopicShareCard` implementation for composer, feed, full-card view and export; improve only the surrounding sizing/measurement wrapper where needed.
- Add tasteful Alive-aware tactics: selection morphs, stronger but controlled Post readiness/press feedback, animated style/aspect choices where useful, smoother topic-picker transitions, and keyboard-aware editing without turning the UI into motion noise.
- Update this log again when the social polish batch is complete.

## Previous request (2026-09-13, composer redesign + DM feel + icon marks)

User asks (summarised): the community "post a topic" sheet feels like filling
in forms — rebuild it (write-first, notes default); chats/friends icons don't
match; hide the red tester-looking error notes; sending feels glitchy (bubble
vanishes then returns); bubbles are translucent with a bad shape and no
animation; tapping bubbles should NOT open emoji/delete (that belongs to
hold); the DM header says "Curious Explorer · Private messages" instead of
showing the person; the share-card preview sits too far left on the wall and
top-left in the profile grid; profile post-count text unreadable.

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
2. A reinstall publishes a SECOND active device row (nothing ever retires
   the old one), so the sender must wrap a key for hardware that no longer
   holds the account. Permanent failure once a stale row exists.
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
- Profile grid: tiles are square (aspectRatio 1f) and the card canvas gained a FILL mode (`widthFraction = 0f`: scale driven by height, top-aligned crop), so a share card fills its tile instead of floating small. Notes and quotes fill the same square with words pinned to the bottom.
- Profile header: Instagram shape — portrait + identity row, counts as three centred COLUMNS beneath, bio, then a full-width action. The privacy narration paragraph and the two explanatory status texts are gone (the action row states everything now: Edit profile / Message / Request sent / Add friend); blocking stays behind the ⋮. "Edit profile" on your own profile navigates to CurioRoutes.PROFILE.
- Composer: two steps. Step one is the compact sheet (kind chips, topic search/pick, words, style). "Preview" opens a full-height Dialog editor: the card drawn via CommunityCardCanvas at full width (SocialTextPost still, now nullable-onClick), SHAPE chips (PORTRAIT/CLASSIC — new state), caption, credit and wording all editable, Post from the header. The preview draws a CommunityCard rebuilt from the draft.
- Wall: rows animate in with `Modifier.animateItem()` inside a Box; take down/report are ICON-only pills (blank label, richer contentDescription, square padding); the stale duplicate-author comment removed.
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

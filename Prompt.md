# Prompt Log — current request

## Request (2026-09-13, IN PROGRESS — social redesign pass, slice 1 shipped)

User (chat), one message, many asks:

1. Friends screen: proper online indicator, recent-message previews, delete a
   chat by tap-and-hold, proper avatar cut-out, faster syncing.
2. Social speed: likes/dislikes must not wait, posting must not wait, faster
   sending, no "slow waiting" feel — better animation flow.
3. "Post a topic" bottom sheet: full revamp/redesign — professional, compact,
   easier, properly animated.
4. Swipe-down refresh: proper animation; REMOVE the refresh button.
5. Replies/comments bottom sheet: proper redesign.
6. Avatar cut-out; change the Friends icon; make Friends a SIDEBAR and turn the
   current Friends screen into CHATS.
7. Share-card editor: share with a link, and a "share to social" button in the
   share dialog.
8. Other member's profile: full redesign, Instagram-style, bio visible, small
   post previews.
9. Realtime notifications for replies.

### Diagnosis and plan

The speed complaint had a concrete, mechanical cause (fixed in slice 1):

- Every like/dislike on the wall called `CommunityApi.like(...)` and then
  `load()` — a FULL feed read that also set `loading = true`. `loading` drives
  both the page skeleton AND `PullToRefreshBox.isRefreshing`, so one tap
  flashed a refresh spinner, rebuilt every card, and only then moved a count.
- Posting did the same: `post()` replied `return=minimal`, so the app could not
  draw the new card until a full feed read came back.
- A card's own page had the identical pattern on its Like pill.
- The wall's header carried an explicit `Refresh` TextButton (the button the
  user wants gone) plus a spinner that only existed because of the above.

### Completion (slice 1 — shipped)

- Wall like/dislike are OPTIMISTIC: the pill and its count move with the tap,
  the server is told behind it, and only a rejected call resyncs the wall from
  the server. `toggleLike`/`toggleDislike` live in `SocialComponents.kt` (the
  one social kit) and keep the one-reaction-per-person rule in step (a like
  takes back a dislike).
- `CommunityApi.post` now returns the stored `CommunityCard` (PostgREST
  `return=representation`), so the wall puts the post up the instant the sheet
  closes, wearing this device's own name and portrait, and reconciles the rest
  of the feed quietly afterwards.
- `loading` no longer drives the pull-to-refresh indicator: a new `refreshing`
  flag belongs to the user's gesture alone, so a background read can never
  flash a spinner.
- The `Refresh` TextButton is REMOVED from the wall header.
- A card's own page likes optimistically too.
- The replies sheet's `onChanged` refreshes quietly instead of spinning the
  whole wall.

### Still to do (needs the user's answers — see below)

Friends/Chats restructure, online indicator in the inbox, delete-chat on
long-press, avatar cut-out, comment sheet redesign, post-a-topic sheet revamp,
share-with-link + share-to-social, other-profile redesign, reply notifications.

## Archive

Previous request logs were replaced at the start of this request. They remain
available in Git history (`git log -p -- Prompt.md`).

## User prompts

### Prompt (2026-09-13) — IN PROGRESS

Social redesign pass: friends/inbox (online indicator, delete chat on hold,
avatar cut-out, speed), instant likes/dislikes and posting, revamped compose
and comments sheets, swipe-refresh without the refresh button, Friends-as-
sidebar + Chats, share-with-link and share-to-social, Instagram-style member
profile, realtime reply notifications.

### Next prompt (the next instruction goes here — never cleared by an agent)

_No pending prompt._

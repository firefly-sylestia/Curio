# Prompt Log — current request

## Request (2026-09-13, IN PROGRESS — social restructure, slice 2 of N)

User (chat, continued from the social-perf slice): fix the CI compile error,
the chat message box sitting under the keyboard, and then the big list —
friends/chat split with long-press delete, composer + comment sheet revamps,
share-to-social, realtime reply notifications. Three design questions were
asked and answered:

- Friends ↔ Chats = **two separate screens**.
- Long-press a chat row = **option to delete the chat for me or for both of us**.
- The post-a-topic composer = **two-step: quick sheet, full-screen editor only
  if the user asks for it** (and notes/quotes stay).

### What this slice shipped

1. **CI fix** (`3a077c29`) — my previous slice put `toggleLike`/`toggleDislike`
   between `@Composable` and `SocialTextPost`, so the annotation landed on the
   helpers: every non-composable call site failed, and `SocialTextPost` lost its
   annotation. Helpers moved above it. Same mistake class as AGENTS rule 8.
2. **Composer under the keyboard** (`7435b55c`) — the app is edge-to-edge
   (`setDecorFitsSystemWindows(false)`) and the NavHost delivers only the
   navigation-bar inset, so `MessageComposer` needed `Modifier.imePadding()`.
3. **Friends / Chats split** —
   - `CurioRoutes.CHATS` + `ChatsScreen.kt` (new): the inbox — dense rows, the
     last line, a stamp, an unread badge, a live dot, `PullToRefreshBox`, and a
     long-press sheet with "Delete for me" / "Delete for both of us".
   - `FriendsScreen.kt` rewritten as a **contacts sidebar**: search, incoming
     requests, outgoing asks, then the friends list filed A–Z under
     `SocialLetterHeader`; tap a friend opens the chat, long-press offers
     Remove; no conversation list left on this screen.
   - `SocialComponents.kt`: `SocialThreadCard` (a box card, chats-only) replaced
     by `SocialSidebarRow` — ONE dense row used by both lists — plus
     `SocialLetterHeader` / `socialLetterOf`. `SocialAvatar` grew
     `online = …` (a green dot cut out of the portrait, valid only from
     `CurioPerson.isActiveNow`), used by the row, the person card and the DM
     header, so the dot and the "Active now" line can never disagree.
   - `SocialInboxCache` gained `replaceThreads` / `replaceContacts` so the two
     screens never blank each other's half of the snapshot.
4. **Conversation deletion** —
   - `dm_conversation_hidden` (already in the schema, unused by the client) is
     now written by `SocialApi.hideConversation`; `threads()` filters a thread
     out only while its newest message is OLDER than `hidden_at`, so a reply
     brings it back.
   - `supabase/schema.sql` §5h: `curio_delete_dm_conversation(other uuid)` —
     security definer, participant-only, deletes the pair's messages, both
     hidden markers and the conversation row; granted to `authenticated`.
     Deliberately not gated on the friendship still existing.
   - `SocialMessageCache.forget` wipes this device's copy on either path.
5. **Presence** — `SocialApi.presenceOf` (opt-in `PERSON_COLUMNS_PRIVACY` read,
   60s in-process cache) folded into `threads()` via `CurioPerson.withPresence`,
   so an inbox row has a real last-active stamp without a second request per row.
6. **Icons** — the wall's action row is now `Chats` (`BubbleChart`) + `Friends`
   (`Hub`, was the `notes` pad) + `You`.

### Still open (this request is not finished)

- Post-a-topic composer: two-step flow (quick sheet → full-screen editor),
  compact/professional redesign, proper animations.
- Comment sheet redesign; the composer field states.
- Share-card editor → "Share to social" (+ share-with-link) inside the share
  dialog.
- Realtime reply notifications (`community_comments` for my cards,
  `dm_messages` arriving while another screen is open).
- Faster-sending animation flow polish in the DM thread.

### ⚠️ Needs the user

Re-paste `supabase/schema.sql` into the dashboard: the "delete for both of us"
option calls the new §5h function, and until the paste it answers a clear error.
Re-running the file is idempotent.

## Archive

The Supabase realtime setup guide (`docs/REALTIME_SETUP.md`, still unpushed by
the user's request) and the social-perf slice (`abebf625`) are logged in Git
history (`git log -p -- Prompt.md`).

## User prompts

### Prompt (2026-09-13) — IN PROGRESS (slice 2 shipped, see above)

Fix the CI compile error and the chat message box under the keyboard; then
split Friends into a sidebar and a Chats screen (two screens), long-press a
chat to delete it for me or for both of us, two-step post-a-topic composer
(full-screen editor only on request, keep notes and quotes) — plus the rest of
the social list (comment sheet, share to social, realtime reply notifications,
faster sending).

### Next prompt (the next instruction goes here — never cleared by an agent)

_No pending prompt._

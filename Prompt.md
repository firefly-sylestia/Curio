# Prompt Log — current request

## Request (2026-09-13, IN PROGRESS — profile + settings polish, account lifecycle, encryption default)

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
- Same-package symbols verified (SettingsOptionCard family, ChatsScreen
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

## User prompts

### Prompt (2026-09-13) — batch in progress, see "Shipped in this batch"

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

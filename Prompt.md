# Prompt.md — current request

## 1. Moderation: the queue redesign, and the ban that would not work

The member's ask: *"Redesign the moderation queue with per-report Dismiss and
Remove, a delete reports button, and bans as open-only buttons"*, plus, in the
same breath, *"i am not able to ban any members the ban button isnt working."*

Three answers were taken from the member before any code moved:

| Question | Answer |
| --- | --- |
| How should Dismiss and Remove behave? | **Keep the sheet for both** (reason + note), and let them be the row's two primary buttons. |
| What should delete-reports clear? | **Handled reports only** — open ones stay. |
| What does "bans open only" mean? | **Ban list: one Open button** per row; change tier and lift live in the sheet it opens. |

### What was built

**The ban that would not work.** Three silent dead ends were found by reading
the flow, and each is now a visible one:

1. **A refusal was written behind the dialog.** Every moderation decision is a
   server function, and a failure landed on the page UNDER the open sheet, where
   nobody reads it while a dialog is up. `ModerationReasonDialog` and
   `ModerationBanDialog` now both take `error` and print it under the dialog's
   own title (the one part of a dialog nothing can scroll away), and each sheet
   opens on a clean slate (`error = null` at the call site).
2. **The required reason sat below the fold.** The ban sheet's eight reasons
   were full-width rows under the tier ladder and the clock, so a moderator
   could pick a tier and a clock and then find a Ban button that never lit up.
   They are wrapping `BanReasonChip`s in a `FlowRow` now, and the disabled
   button says what it waits for ("Pick a reason").
3. **The lift needed a list the queue had never read.** `lifting` was a row of
   the BAN list, but "Lift the ban instead" is reachable from a REPORT too,
   where that list is empty — so it silently did nothing. It is now a
   `(userId, label)` pair, carried by the sheet that asks for it.

**The queue.** `Clear N handled` on the list's own head (only when something IS
handled), behind `SocialConfirmDialog` → `CommunityApi.deleteHandledReports` →
a new `curio_moderate_delete_reports()` that deletes `status <> 'open'` in the
DELETE itself (a sweep can never drop an unread report), records one
`moderation_actions` row, and answers with the count. The row's actions are now
Remove + Dismiss as the pair, with `ModerationQuietAction("Hide the author")` as
the quiet line beside them.

**The refresh.** The first read is guarded by `loaded` (it also verifies the
team row and reads the roster), and every action called it back and got nothing
— a report that had just been decided stayed in the list until the screen was
reopened. `refreshQueue` + `absorbQueue` and `refreshTeam(quiet)` are the
re-readable halves, and every write goes through one of them.

**The ban list.** One `Open` per row (filled while a tier is in force, outlined
on a lapsed ban), a quiet `Profile`, and the tier chip. Change tier and lift
live inside the sheet. A lapsed ban passes `currentKind = ""` so the sheet opens
ready to SET a tier instead of "changing" one nobody is under.

## 2. Still open from the last batch

- The member should re-paste `supabase/schema.sql` for the ban ladder and the
  new delete-reports function; if the ban RPC was missing, the sheet now SAYS so
  instead of looking dead.
- CI unconfirmed on the last two commits.

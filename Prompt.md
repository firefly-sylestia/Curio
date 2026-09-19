# Prompt.md — current request

## 1. What's New screen — DONE (pushed with the batch)

A release-highlights page that opens itself **once per version** (fresh install
and update alike), with a "Take me there" door on every card.

| Where | What |
| --- | --- |
| `features/updates/WhatsNewScreen.kt` (new) | `WHATS_NEW_RELEASES` (hand-authored, ships in the APK, offline), `WhatsNewRelease` / `WhatsNewItem`, the settings-family page. |
| `data/AppPreferences.kt` | `get/setWhatsNewSeenVersion` (key `whats_new_seen_version`). |
| `navigation/CurioRoutes.kt` | `WHATS_NEW = "whats-new"`. |
| `navigation/CurioNavHost.kt` | Route + `SettingsSharedScope`, membership in `settingsFamilyRoutePrefixes`, the once-per-version auto-open (quiet starts only). |
| `features/settings/SettingsHubScreen.kt` | "What's New" row in the Updates group + the deep row. |
| `app/AGENTS.md` | The v403 contract (how a release adds its entry). |

Content chosen by the member: the journal page, the rebuilt Cabinet, the book
screen, Online mode, and the one sheet on Home that opens them all.

**House rule the member set:** no em dashes in user-visible copy.

## 2. Form feedback — BUILT (in the same batch)

- Server: `supabase/schema.sql` §6f (tables, policies, publish/close/tally
  functions, the new `forms` team switch, a self-check that fails the paste if
  an identity column is ever added to an answer table).
- App: `data/supabase/FeedbackApi.kt`, `features/feedback/FeedbackFormScreen.kt`
  (state holder, the standout card, the answering sheet), and
  `features/community/ModerationForms.kt` (builder, test runs, publish with the
  owner override, results with percentages, post-the-result-to-the-wall).
- Where it shows: Home's card under the deck, Support's card at the top plus a
  row in the Feedback card, and the moderation room's Forms tab.

## 3. THIS BATCH — the writing page's drag/drop, grouping and previews — DONE

The member's report, in their words:

1. **The inline attachment is gone.** "Remove the inline photo and keep what it
   was before (e4c95278 or before)" — reverts `72ca6663` and its follow-up
   `3695d043`, so a photo or a voice note is a BLOCK again: it takes a line of
   its own, drags, resizes, and carries its own caption. `PERSONAL_INLINE_MARK`,
   `PersonalBlock.inlineRefs` / `inlineCount` / `inlineOffsets`, the `"inr"`
   codec key, the editor's inline skip and `ChapterNoteBridge`'s mark surgery
   are all gone; `chapterNoteText` / `chapterNoteSpans` join blocks with a plain
   newline again. Verified: no reference to any of those names survives in
   `app/src/main/java/` or `app/src/test/`.
2. **The voice note goes back to blending with the canvas** (the look at
   `f14745ec`), with a shadow added to the bar (`PersonalVoiceBar`:
   `.shadow(1.5.dp, RoundedCornerShape(14.dp))`, no container).
3. **Grouping is predictable**, and the reasons were found rather than guessed:
   - a run's members after the first are SKIPPED by the drawing pass and never
     measured, so a drop "on" a pair landed BETWEEN its members and made a
     three (`printDropIndex`, v403: snap to the run — first cell coming down,
     last going up);
   - a print already in (or already touching) the run used to be thrown to the
     run's head on the next press, so it now stays exactly where it is, and only
     a print arriving from elsewhere is brought against the run;
   - PAGE is the size a print ARRIVES with, and a PAGE cell of a row was the
     "they won't group" the member felt, so `normaliseRowSizesAt` turns PAGE
     into HALF once a print stands beside another — called from `moveBlock`
     BEFORE its single `onDocChanged`, so a grouping drop is one move to undo
     (not a move plus a resize), and a size the member picked is never touched.
4. **No more line previews**: the flat rule on the landing edge is gone; the
   hover is the block's own dashed room, drawn as a half-width CELL when a print
   is coming down on a print (same change as 3's "auto adjust while holding").
5. Kept: per-cell sizes inside a group, the caption's face / size / date, and
   the caption dock.

Pushed all together with the reverted work (one push), per the member's "push
the before commits all together".

### Follow-up in the same area — DONE

**Two photos added one after another now group on their own.** The picture tool
SPLITS the line under the caret, so the second picture arrived with an empty
piece of that split standing between it and the first: two prints with no shared
edge, which is why they had to be dragged together. `insertAtCaret` now drops
that piece when it is EMPTY (no words of the member's) and the line above it is
already a picture (`keepHead`), and it calls `normaliseRowSizesAt` before its
own `onDocChanged`, so the pair lands at a cell's size in the same frame. A
piece with words on it, a picture added at the top of the page and every voice
note keep the empty line they always got (a voice note never groups, so nothing
about it changed).

## 4. Open work (after this batch)

1. Series and anime episode data is fetched, not authored.
2. "Save your take" rebuilds the whole `AnnotatedString` per keystroke.
3. The caption questions (date separator, "always on" reading).
4. The moderation queue redesign (Dismiss / Remove per report, bans as
   open-only buttons) — the member's earlier words, not yet built.
5. GitHub workflow redesign (deferred by the member).

# Prompt.md — current request

## Done and pushed

| Commit | What |
| --- | --- |
| `c0ac876c` | A print's caption is a label of its own: face, size and a date. |
| `5572e835` | The pinned heading reports where it is on the screen (window coordinates, one entry per side, live numbers, tap that lands on the heading) + its view redrawn. |
| `f28cd156` | The three defects that pass introduced (unterminated KDoc swallowing `PersonalCanvas.kt`, the non-nullable local, the stale `pinnedSection.value`), the avatar collar hairline, and lint's `NonObservableLocale` in the reader's marks list. |

## The member's answers to the questions asked (this is the plan for the rest)

**The What's New screen**
- It should read like the updater's release notes — `features/updates/UpdatesScreen.kt`
  already parses notes into `NoteBlock`s and renders them on a hero — with a
  **"Take me there"** action per highlight that navigates to the feature.
- Shows **once per version**, install and update alike (keyed on `versionCode`),
  with a way back to it from Settings.

**Forms (the big one)**
- Answers live **on the server (Supabase)**, anonymous to the device: totals
  aggregate across members, nothing links an answer to an account, and the device
  keeps a local "already answered" flag. Needs new tables + RLS (owner/admin
  write, authenticated insert-only for answers, no anon).
- **Results**: owner and approved admins only, shown as **percentages**; the owner
  can post a form's result **from the moderation screen** as a post.
- **Question types**: all three — single choice, multi-select and a short written
  answer — each chosen by the form maker per question.
- **Limits**: up to 5 questions, up to 4 options each.
- **Cadence**: one publish every 14 days, testing does not count, **the owner can
  override**, and **publishing a new form cancels the old one** (only the newest
  is live).
- Members can **skip** a form, or **never show** it again — both outcomes visible
  in moderation.
- Only the **owner** may create forms; the owner can grant other admins the
  permission.

**The moderation room**
- The queue is redesigned; teams bans become open-only buttons; each report
  carries **two** actions: **Dismiss** (clear the report, keep the post) and
  **Remove** (take the post down and clear every report on it).

**Workflow** — deferred by the member ("skip the workflow change for later").

## Still open from the caption work

1. "The date is always on" — read as *the tool is always in the dock*; the other
   reading is *a new caption is stamped with today by itself*.
2. The date's separator: labels write `14/03/2026`, the request said `dd:mm:yyyy`.
3. The seven label faces are the app's bundled ones; a label-only face is a font
   file plus one enum entry.

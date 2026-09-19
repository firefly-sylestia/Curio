# Prompt.md — current request

## 1. What's New screen — DONE (this commit)

A release-highlights page that opens itself **once per version** (fresh install
and update alike), with a "Take me there" door on every card.

| Where | What |
| --- | --- |
| `features/updates/WhatsNewScreen.kt` (new) | `WHATS_NEW_RELEASES` (hand-authored, ships in the APK, offline), `WhatsNewRelease` / `WhatsNewItem`, the settings-family page. |
| `data/AppPreferences.kt` | `get/setWhatsNewSeenVersion` (key `whats_new_seen_version`). |
| `navigation/CurioRoutes.kt` | `WHATS_NEW = "whats-new"`. |
| `navigation/CurioNavHost.kt` | Route + `SettingsSharedScope`, membership in `settingsFamilyRoutePrefixes`, and the once-per-version auto-open (quiet starts only: splash / onboarding / home). |
| `features/settings/SettingsHubScreen.kt` | "What's New" row in the Updates group + the deep row. |
| `app/AGENTS.md` | The v403 contract (how a release adds its entry). |
| changelog `20260922.txt` | One ADD bullet. |

**The member chose the content** — big screen-level updates only, not the small
fixes (their words: *"recent ones, the big screen updates like the journal page
the new cabinet, the online mode, the book screen the new one and the bottom
sheet to access them only"*). The five cards are:

1. The journal page → `JOURNALS`
2. The Cabinet, rebuilt → `CABINET`
3. The book screen → `CABINET`
4. Online mode → `SETTINGS_ONLINE`
5. One sheet opens them all (Home's `+`) → `HOME`

**House rule the member set:** no em dashes in user-visible copy.

## 2. Form feedback — the full system (NEXT)

Decisions already given (this session):

- **Where the door lives:** Support. **A filled accent card at the top while a
  form is live, with the row below it.** The member also wants *that form row
  card* to be the page's standout filled element.
- A member can take the form **later** if they do not want it now.
- **Answers:** on the server (Supabase), anonymous to the device; the device
  keeps only a local "already answered" flag.
- **Results:** owner + approved admins, as percentages; the owner can post a
  result to the wall from the moderation screen.
- **Questions:** single choice, multi-select and a short written answer — chosen
  per question by the form maker. Max 5 questions, max 4 options each.
- **Cadence:** one publish per 14 days, tests do not count, the owner can
  override, and publishing a new form cancels the previous one.
- **Skip** and **Never show** buttons on the form itself; both outcomes are
  visible in moderation.
- Only the **owner** creates forms; the owner can grant other admins the
  permission.

## 3. The moderation room (after the forms)

Queue redesigned, team bans as open-only buttons, and each report carrying
**Dismiss** (clear the report, keep the post) and **Remove** (take the post down
and clear every report on it).

## 4. Open work from earlier sessions (after that)

1. Series and anime episode data is fetched, not authored: every series has a
   synopsis, only two carry a hand-written episode list, and anime has no
   per-episode synopses.
2. "Save your take" still rebuilds the whole `AnnotatedString` per keystroke
   (found by inspection, never profiled).
3. The date questions from the caption work (separator, "always on" reading).
4. GitHub workflow redesign (the member deferred it: "skip the workflow change
   for later").

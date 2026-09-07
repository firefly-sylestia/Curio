# Curio — Full App Analysis & Product Roadmap

> Snapshot date: Sep 2, 2026 · Scope: the Android app (`app/`) only — `web/` and `desktop/` are separate projects on hold.
> This document is a working analysis: **feature refinement, redesign, UI/UX (what a user sees and taps), and new-feature ideas**, grounded in the actual code (I audited the live surfaces) plus app-pattern research. It is intentionally **not committed or pushed** — read it here.

---

## 1. Executive summary

Curio is a "curiosity app": a catalog of 36+ lanes (categories) with curated topics, a spin-deck discovery mechanic, a reveal flow (read/watch), a Cabinet of saved discoveries, capture formats for notes/recordings, a share-card studio, a pet + quests gamification layer, an ambitious liquid-glass visual system, and home-screen widgets. The core loop (spin → reveal → save → capture) is sound and the product has genuinely premium moments (the reveal morph, signature share cards, the glass system). The weaknesses are **discoverability, hierarchy, and unfinished seams** — several features exist but are buried (Experiments), under-terminalled (mix cards), or silently dead (a few dead taps already fixed).

**Top quick wins (days):**
1. Picker/mixes: drag-to-reorder mixes, search inside mixes, auto-named mix cover from lanes (partially done).
2. "Surprise me" undo + peek (see Spin refinements).
3. Make Experiments discoverable (it hides Liquid Glass + Material themes behind 5 taps).
4. Continue-exploring reset ("restore defaults") + distinguish history-lanes from curated ones visually.

**Top big bets (weeks):**
1. **Cabinet 2.0** — cabinets as collections/folders with covers (it's already a "cabinet" metaphor — make it physical).
2. **Curio Journey** — a per-lane mastery/progress path (read/watch history, "continue", level badges) wired into the existing progress store.
3. **Daily Curio** — a daily-mix ritual (a new day = a fresh surprise deck, streak ties into it).
4. **Share/export hub** — template saving, custom aspect presets, "share as link".

---

## 2. Surface map (what the app has, from code)

| Surface | What the user sees | Key taps | Notes from audit |
|---|---|---|---|
| **Splash → Onboarding** | Boot gates, theme pick, first-run tour | Choose light/dark, start | Theme flip plays the circular reveal ✓ |
| **Home** | Rose hero, Streak · Cabinet · Topics stats, lane chips, spin CTA, hamburger → drawer | Lane chip, Spin, streak, drawer | Drawer hosts Settings/Quests/etc. Home anchors all tab navigation (good) |
| **Spin** | Deck carousel (front ticket + 2 peeks), spin, reveal page, Like/Dislike pill, Save to Cabinet | Spin, reveal interactions, save | Reveal morph is a highlight. "Surprise me" has no undo (see 4.2) |
| **Category picker (sheet + Browse)** | Pinned lanes, named mixes, Continue exploring, mode pages, bottom action row | Hold → option pill; mix cards; Surprise me; Browse | Just refined: mix cards now have tinted covers, Active label, explicit Edit/Delete. Further: see 4.1 |
| **Cabinet** | Saved discoveries, search/sort/category filter chips | Open, share, archive?, recycle bin | Solid; collections/folders missing (5.1) |
| **Capture** | Format picker per category: Reel Notes, Field Notes, Journal, Sound Bite, Open Notebook | Record, type, attach, save | Image thumbs now open Lightbox ✓ |
| **Entry Detail / Share** | Moodboard editor, share-card studio (35 designs × styles × Customise), save/share | Style carousel, hold-to-edit, Customise | Very rich; template save + "your designs" missing |
| **Profile / Quests / Pet** | Pet (Curie), XP, daily quests, games (hide-and-seek, POP!) | Play, collect, groom | Charm; economy shallow (see 4.6) |
| **Topic Database / Manage / Recent / History / Stats** | Full catalog browser, per-lane management, recents, history, stats | Filter, open, search | Discoverability of "continue" progress missing |
| **Settings hub** | Appearance, Preferences, Recording, Data/backup, Share Hub, Updates, Support, Experiments | Theme, glass tuning, backup | Experiments = hidden room (4.7). Backups solid |
| **Glass widgets / Widget lab / Live wallpaper** | Frosted home-screen widgets, lab editor, wallpaper compositor | Add, edit size/blur/color | Excellent differentiator; needs discovery + a "why" |
| **Recycle bin / Crash screen / Promo mode** | Safety nets | Restore, report | Double-confirm on purge ✓ |

The one-sentence takeaway: **the app's depth is its strength and its problem — dozens of capable surfaces with inconsistent discoverability, hierarchy and micro-interaction polish.**

---

## 3. Verified strengths (keep, don't break)

- **Shared-element reveal morph** (Spin ticket → Reveal hero) — the app's signature moment.
- **HOME-anchored navigation discipline** (`popUpTo(HOME)` root) — Back never exits accidentally; tab state preserved.
- **Out-of-band handoffs** (`LightboxTarget`, `PendingEntryOpen`, `SpinPickerRequest`) — URI-encoding-safe patterns that avoided real bugs.
- **Recycle-bin double-confirm** — destructive flows are protected.
- **Glass crash-recovery system** — auto-disables glass after repeated native crashes; device-capability checks.
- **Reactive prefs usage** — picker state now writes/reads reactively (no more restart-to-see).
- **Theme transition** — circular reveal now works with glass ON (PixelCopy capture).
- **Changelog discipline** — every user-visible change is logged.

---

## 4. UX weaknesses & concrete refinements (what users see/tap)

### 4.1 Category picker — mixes (continuation of the refinement just shipped)

Just shipped in this session: lead-lane tinted covers, "Active" label, explicit Edit/Delete buttons, count in the header, empty-state CTA.

Next refinements:

| # | What the user sees/taps today | Problem | Refinement |
|---|---|---|---|
| 1 | Mix cards in fixed creation order | No way to organise | **Long-press-drag to reorder mixes** (grid reorder with haptics); order persisted |
| 2 | Mix name typed manually | Naming friction | **Auto-suggest names** from the picked lanes ("Cinema nights", "Quiet science") with the field pre-filled, editable |
| 3 | Cover is a single tinted plate | Repetitive | **Composition cover**: 2–3 overlapping lane plates (like Spotify multi-artist covers) auto-generated from the mix; optional manual cover pick later |
| 4 | "Show all / Show less" expands in place | Fine but blunt | Expand with animation + a **count of hidden mixes** on the button |
| 5 | Continue-exploring mixes history + curated invisibly | Confusing (audited issue) | **Two labelled groups** ("Recently spun" vs "Suggestions") + **"Restore defaults"** on suggestions; history lanes show an ♥/history glyph instead of looking removable |
| 6 | Tile selection = tint fill only | OK now | Add **selected-count header** ("3 picked") in Mix mode so multi-select feels controlled |
| 7 | Mix editor grid | Fine | Add **"Add all/lane groups" quick chips** (Curio/Knowledge groups) to compose mixes fast |

### 4.2 Spin deck & reveal

| # | Issue | Refinement |
|---|---|---|
| 1 | **"Surprise me" has no undo** and instantly replaces the deck + closes the sheet | Add an **Undo pill** ("shuffled — Undo") for ~6s after Surprise me; mis-tap protection |
| 2 | Peek cards show only 2 | Fine (user mandated) — keep |
| 3 | Spin outcome randomness invisible | Show a tiny **"why this?"** line on reveal ("Most-spun lane", "From your X mix", "First time in Films") — serendipity with story |
| 4 | Reveal Like/Dislike is binary | Add **long-press on a reveal → mini actions** (Save, More like this, Not now) mirroring the picker's hold-pill pattern |
| 5 | Repeat topics feel repetitive | **Recency-weighted shuffle**: avoid repeating the same topic within N spins per session (progress store exists — use it) |
| 6 | No "skip" on reveal | Add **Skip** as a secondary action (doesn't count as explored) so users don't game the streak by force-landing |

### 4.3 Home

| # | Issue | Refinement |
|---|---|---|
| 1 | Hero is static rose; stats are passive | Make the hero **stateful**: shows today's mini-goal ("Spin 3 today"), reflects streak progress ring, and the large CTA is contextual ("Continue your Film journey", "Your daily mix is ready") |
| 2 | Lane chips are a flat row | Add **recency interaction**: the most-recent lane chip gets a subtle "last spun" dot |
| 3 | Drawer is dense | Group into **three sections** (Explore / My stuff / App) with icons; add "Resume last session" at top |
| 4 | Streak shown but it's passive | **Tomorrow preview** ("Next streak: tomorrow") + freeze shield later (see new features) |

### 4.4 Cabinet

| # | Issue | Refinement |
|---|---|---|
| 1 | Flat list + filters | **Collections** ("moodboards as folders" — move/split existing moodboards into it). Cabinet becomes the "keep" home: covers per collection, reorder, art-style headers |
| 2 | No multi-select | **Select mode** (long-press entry → checkbox) → batch share/export/recycle |
| 3 | No export of one entry | Add **Export entry** (markdown/JSON/text) via the existing backup engine |
| 4 | Empty state weak | Empty state with **3 suggested next discoveries** + a "Surprise me again" CTA |
| 5 | Search/sort exists (good) | Add **filter by captured format** (notes vs voice vs quotes) chips |

### 4.5 Capture & detail

| # | Issue | Refinement |
|---|---|---|
| 1 | Format picker per category is implicit | Show a **format hint chip** on each capture tile (🎙 voice, ✍ notes) |
| 2 | Sound Bite recording UX | Add **pause/resume + waveform scrub** during recording; trim on save |
| 3 | Session timer (explore sessions) lives in notifications/bubble | Surface a **session chip on the reveal page** (elapsed time, pause) so sessions are felt in-app |
| 4 | Reel/Field Notes | Add **templates** for common captures (Quote / Idea / Fact) with one tap |
| 5 | Detail page is rich | Add **"View in collection" + "More like this"** rows at the bottom |

### 4.6 Pet, quests & XP

| # | Issue | Refinement |
|---|---|---|
| 1 | XP economy is shallow | **Named levels + rewards** (unlock: new pet outfits, new share-card palettes, custom lane order) — give XP a reason |
| 2 | Daily quests generic | Align quests with real loop: "Reveal 2 new topics", "Save 1 discovery", "Capture with voice once" — and show **quest progress on Home** |
| 3 | Pet games hidden in profile | **Pet reacts on Home** (nudges when quests complete) + quick "Play" bubble |
| 4 | No pet customisation payoff | Outfit **shop** funded by streak/quests (pure cosmetics, no IAP pressure) |

### 4.7 Settings & Experiments (biggest discoverability problem)

| # | Issue | Refinement |
|---|---|---|
| 1 | Experiments (Liquid Glass, Material theme, classic picker) hidden behind 5 taps | Add a **"Explore" section** in Settings with one-line descriptions + toggles; or surface the main ones (glass, Material) directly in Appearance with "More experiments" link |
| 2 | Backup good but invisible | Add **"Last backup" + one-tap "Back up now"** on Home drawer |
| 3 | Updates page only checks on demand | Keep (data-conscious) but show **version + "What's new"** inside Support |

### 4.8 Global UX polish

| # | Issue | Refinement |
|---|---|---|
| 1 | Touch targets | Audit all icon-only buttons ≥24dp → 28dp+ (mix buttons were just fixed to 28dp) |
| 2 | Haptics | Add **light haptics** on spin landing, save-to-cabinet, quest complete, pin/unpin (respect "reduce motion") |
| 3 | Motion language | One shared set of spring/tween tokens (already centralized in `CurioMotion`) — apply to all new surfaces |
| 4 | Dark-mode contrast audit | `outlineVariant`-style pale creams keep leaking into rings (fixed twice already) — add a **"no pale borders in dark" rule to the DOX style contract** |
| 5 | Empty states everywhere | Standardise: icon + one line + CTA (mix empty state was just upgraded) |
| 6 | Accessibility | TalkBack contentDescriptions on icon-only buttons; verify font-scale at 1.3×; text contrast ≥4.5:1 (some `onSurfaceVariant @ 0.7` labels are close) |
| 7 | Back handling | Standardise: option pills dismiss on back; sheets swipe-down (mostly done) |

---

## 5. Redesign proposals (bigger, flagship-level)

### 5.1 Cabinet 2.0 — "your collections"
- Turn Cabinet into **collection cards** (3×2 grid of entry covers styled like the share cards) + an "Everything" collection.
- Create from a moodboard (it already has a board metaphor); naming, cover pick, reorder.
- "Pin discovery directly into collection" from the reveal page (hold → pill → "File to…").
- Impact: turns a list into a keepsake surface; ties into share-card art.

### 5.2 Curio Journey — per-lane progression
- Every lane gets a **progress spine**: Explore (articles) → Watch → Collect (3 badges) + "Continue" resume.
- The existing `TopicProgressStore` already tracks reading/watching — surface it: per-lane "X of Y explored", unread count, "Keep going" chips.
- Reveal screen shows **what's next in this lane**.
- Impact: gives repeat visitors a reason to return per lane (retention).

### 5.3 Daily Curio — the ritual
- A **daily mix card on Home**: "Today's deck" (5 lanes rotated daily from your pinned/most-loved/seeded mix).
- Spinning it awards a **daily bonus XP + streak shield** mechanic.
- A morning notification (opt-in) "Your deck is ready".
- Impact: habitual loop → retention; matches the streak framework already in app.

### 5.4 Share studio — "your designs"
- Save **custom card layouts as named templates** (style + placements + fact source) and reuse across topics.
- Custom aspect presets (9:16, 3:4, square — exists) + **export SNS-ready crops**.
- "Share as text/link" pill already exists — add **rich-link cards** (topic metadata card shared to WhatsApp/Discord).

### 5.5 Browse 2.0 — topic database as a library
- Add **shelf grouping** (by lane progress), "unexplored" filter, and list/grid toggle.
- In-DB search is already strong — add **voice search** (Vosk engine exists for capture!).

---

## 6. New features (researched, prioritised)

Priority legend: **P0** = ships value now / cheap · **P1** = worthwhile, medium effort · **P2** = bigger bet.

| Feature | What the user gets | Priority / effort | Research note |
|---|---|---|---|
| **Surprise-me Undo** (4.2.1) | Safety on a destructive tap | P0 · 1d | Standard pattern: destructive actions need undo affordance within seconds (Apple HIG / Material). |
| **Recency-weighted shuffle** (4.2.5) | Less repetition | P0 · 2d | Spotify's "discover weekly" avoids repeats; user retention rises with novelty control. |
| **Continue exploring reset** (4.1.5) | Control over suggestions | P0 · 1d | Users need a restore path for algorithmic lists (Netflix "reset taste"). |
| **Collections in Cabinet** (5.1) | Organised keepsakes | P1 · 2–3w | Playlist/board metaphors are the most-used sharing patterns (Spotify, Pinterest). |
| **Per-lane Journey** (5.2) | Progress + return reason | P1 · 2–3w | Progress bars lift retention in learning apps (Duolingo, Blinkist). |
| **Daily Curio + streak shield** (5.3) | Daily ritual | P1 · 2w | Streaks + daily content are the proven habit-loop (Duolingo, Headspace). |
| **"Why this?" on reveal** (4.2.3) | Serendipity with story | P1 · 2d | Explanation UI increases trust in recommendations (Spotify "because you…"). |
| **Voice search in DB** | Hands-free search | P1 · 1w | Vosk models already ship for capture — reuse the engine. |
| **Pet outfit shop (XP-funded)** (4.6) | Customisation payoff | P1 · 1–2w | Cosmetics funded by in-app effort (no IAP) keep gamification virtuous. |
| **Share templates (5.4)** | Reuse your designs | P1 · 2w | Personalisation drives share loops (Canva templates). |
| **Batch select in Cabinet** (4.4.2) | Bulk share/export | P1 · 2–3d | Standard list pattern; missing today. |
| **Session chip on reveal** (4.5.3) | Feel sessions | P1 · 1d | Sessions exist; they're invisible in-app. |
| **Undo on deck edits** (broader) | All deck changes undoable | P1 · 2–3d | Shell for the Surprise-me undo; apply to apply-mix too. |
| **Streak freeze shields** (earn via quests) | Streak resilience | P2 · 1w | Proven retention lever (Duolingo streak freezes). |
| **Rich-link share cards** (5.4) | SNS-ready metadata cards | P2 · 1–2w | WhatsApp/Discord link previews drive growth. |
| **Watch/complication support** | Wearable peek | P2 · 3w+ | Niche but differentiator; only if core polish is done. |
| **Onboarding mini-quiz** (lane personality) | Personalised start | P2 · 1w | First-run personalisation lifts early activation (many discovery apps). |
| **Offline-first synced Cabinet (manual)** | Device-to-device continuity | P2 · 3–4w | Backups exist — optional manual export/import of Cabinet content. |

**Deliberately not proposed:** social feeds, IAP pressure, ads, notifications spam. They would erode the calm-curiosity identity.

---

## 7. Design-system refinements (cross-cutting)

1. **Border rule**: never use `outlineVariant`-style pale creams for rings in dark mode (already burned twice). Encode in the DOX style contract.
2. **Elevation language**: keep shadow-over-fill rules (root AGENTS.md) — applied consistently already.
3. **Tint identity per surface**: category tint on tiles/heroes is a signature — extend it to mix covers (done) and Cabinet collection covers.
4. **Iconography**: the glyph set is good; standardise **empty-state illustrations** later (can ship as glyph + wash first).
5. **Type**: ExtraBold titles + bodySmall captions is a strong pairing — add a **display style** for the Daily Curio card.
6. **Micro-copy**: replace developer-y strings ("No mixes yet") with personality; keep the voice warm, never cute-toothless.

---

## 8. Metrics worth instrumenting (if not present)

- Spin → reveal conversion; save-per-reveal; capture-per-save.
- Lane repeat-rate before/after recency shuffle.
- Mix creation + re-spin of saved mixes (do mixes stick?).
- Session length + return-day-3 rate; streak length distribution.
- Share-card export/share counts per design (identify best templates).
- Experiments toggle adoption (glass on/off) — proves discoverability fix.

---

## 9. Suggested sequencing

**Sprint 1 (polish, low risk):** Surprise-me undo · Continue-exploring reset · recency shuffle · session chip · haptics pass · dark-border rule (docs).
**Sprint 2 (hierarchy):** Experiments discoverability · Cabinet batch select · collections MVP · mix reorder + auto-name.
**Sprint 3 (retention):** Daily Curio + streak shield · per-lane Journey · XP-funded pet rewards.
**Sprint 4 (share loop):** share templates · rich-link cards · export entry.

Everything above is compatible with the existing architecture (reactive prefs, Room capture store, progress store, backup engine, share-card engine) — the app already has the plumbing; most items are UI layer work.

---

## 10. Appendix — research grounding (patterns referenced)

- **Spotify playlists/Blend** — mix-creation flows, multi-artist covers (Blend), "because you listened to…" explanation UI for trust (engineering.atspotify.com, uxplanet/uxdesign analyses, Spotify support docs on create/edit/delete).
- **Duolingo** — streaks, streak freezes, daily quests, level→reward mapping (public product analyses).
- **Pocket/Blinkist** — saved-list organisation, progress spines for content.
- **Pinterest boards / Apple Music playlists** — collection-as-canvas (covers, reorder).
- **Material design + Apple HIG** — destructive-action undo, touch targets ≥44pt/48dp, empty states with actions.
- This app's own history (AGENTS.md DOX): rules distilled from past user calls — no pale borders in dark, no elevation on animating decks, shadow-before-fill, tint-based selection states, read-before-write around prefs.
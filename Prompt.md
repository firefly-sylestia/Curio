# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

Three requests arrived back to back; the third is the live one.

**(a) Settings + licence** (owed from an earlier session):

> "…can we add this license [AGPL-3.0] for our curio in readme, also hide the rail in
> settings the top rail, when im in all settings only show when inside, some settings, also
> remove remove the all settings option from the rail, and about the info in the setting
> cards, make their text be 2 or 3 lines for the end 4 settings card the online mode, recycle
> bin, updates, and help and feedbacks see how the texts get cut how about remove the icons
> for those 4, and then in appearance while keeping the rail system instead of the big cards
> with design, add a simpler list based simpler all settings look the main settings page but
> a simpler and list view."

**(b) Blending**:

> "also the background and the cards and tabs some were blending too much, the journals in the
> home screen the preview of them get the background color and blends, an u fix this issue.
> properly adress this blending issue. and use white cards and white tabs backgroud for cards,
> instead of cream, but keep the main app backgroud the current color, just the card colors
> etc app wide, or card area such as in profile the quests settings etc where they are prsent
> its card color is creamy and blends too much, add proper formula for hirarcy and proper
> separations, per clicable elemets and all app wide"

**(c) Books**:

> "for the book view replace the buttom sheet of the book from topic reveal screen with the new
> book screen, also in the new book screen add reading progress with pages and chapter page
> being the more noticable, in the place of chapter 0 im on and the book has that card area,
> redesign it to have proper progress from the pdf or epub tracking, and also proper chapter
> updates if the pdf had it with proper progress, and use can chnage the progress like before
> but the pdf reading for pages will be usng from pdf, and for chapter too also if user updates
> the chapter and progress in that edit it still shuld keep where the user had left reading, and
> for books added directly from file please use the file name for proper detetction of book and
> let the user edit the book name if it cant detect properly."

**(d) THE LIVE REQUEST — a full-app redundancy audit**:

> "also many screen have redundancy alot of redundancy with the app, tabs etc, and and
> unecessary texts duplicate tabs etc etc do a full app audit."

---

## 2. What was changed for (a), (b) and (c)

### Licence
- `LICENSE` committed with the byte-exact AGPL-3.0 text (fetched from
  `https://www.gnu.org/licenses/agpl-3.0.txt`; 661 lines) — the README already linked a
  `LICENSE` file that did not exist.
- `README.md`'s Licence section: MIT → AGPL-3.0, with a short plain-language note about
  §13 (a hosted fork has to offer its source too) and the copyright line.

### Settings hub → a plain list
- The hub's phone branch now renders `SettingsSections` as a heading per section plus ONE
  white `SettingsOptionCard` of rows — `GridCells.Fixed(1)`, everything full-span.
- The rail is gone from the hub, and its leading **"All Settings"** chip is gone from
  `settingsNavRail`. The rail lives on the SECTION pages only (no `activeNav` state remains).
- `SettingsRowEntry.plain = true` for **Online mode / Recycle bin / Updates / Support &
  diagnostics**: no icon tile, taller row, subtitle wraps to three lines, divider flush
  (`SettingsOptionDivider(startInset = 0.dp)`). Online mode's subtitle took the full sentence
  the old secondary card carried.
- `PetLandmark(id = "appearance")` moved from the deleted Appearance card onto the
  Appearance ROW (the pet's poke and the tour's stop still land).
- **~1,000 lines of designed-card machinery deleted** (`SettingsDesignTone/Visual/Card/Group`,
  `settingsDesignGroups`, `settingsSecondaryCards`, `settingsToneGradient`, `settingsCardInk`,
  `SettingsCardTexture`, `SettingsDesignCardView`, `SettingsSecondaryCardView`,
  `SettingsCardVisual`). `settingsCardChipTint()` / `settingsCardTintLift()` stay — they are
  the shared card-tint family.

### The blending fix (the root cause, in the theme)
- **Light scheme, new container ladder:** the page keeps `SoftCream`; `surfaceContainerLowest`
  and `surfaceContainerLow` are now WHITE (cards), `surfaceContainer` a nested off-white,
  `surfaceContainerHigh` the floating step, `surfaceContainerHighest` the anchor. Cards used
  to be a DARKER cream than the page, which is why they vanished into a lane wash.
- **Dark ladder lifted** (`#121212 → #161616 → #1C1C1C → #242424 → #2C2C2C`) so a dark card is
  a plate on the black page instead of a `#101010` smudge.
- `outline` / `outlineVariant` carry real alpha again (0.24 / 0.13 light, 0.26 / 0.14 dark).
- `settingsCardTintLift()` (light) is white-with-hero-ink, not the page background;
  `curioDialogContainerColor()` (light) is the white panel with the rose whisper.
- **Every translucent "cream glass" card became opaque**: the `else Color.White.copy(alpha =
  0.6x–0.72f)` family in `TextHistory`, `TopicHistoryScreen`, `RecycleBinScreen`,
  `BookCoverHubScreen`, `BookBrowserScreen`, `CurioAccountComponents`,
  `ManageCategoriesScreen`, `SocialComponents`, `SettingsPageComponents`; the settings rail
  chips and the quick-tool chips and the search field (each with a hairline edge);
  `CurioSettingsCard` gained the shared hairline `BorderStroke`.
- Net effect on the journal preview rows on Home: they are white on the lane wash now.

### Books
- **The reveal's book sheet is gone.** Synopsising a book topic or tapping a chapter chip now
  resolves (or additively shelves) the topic's shelf book by catalog id and navigates to
  `BookDetailScreen`. `BookNotesSheet`, `BookNotesMode`, `ChapterNoteField` and the
  `pendingChapterShare` share seed were deleted.
- **`ProgressCard` rebuilt from the file:** a page bar + "page N of M" (only for a real PDF —
  `pdfPageCount`), the chapter (the file's own `documentChapters` names), the chapter ticks,
  and the manual steppers KEPT ("I'm on", "Page", "The book has").
- **Two clocks, and who wins** (the member's "it still should keep where the user had left
  reading"): a hand edit writes the book ROW only; the reader's POSITION row is never touched
  by it, so the real reading place can never be yanked. `PersonalDao.observeReaderPosition`
  was added because every mark query excludes `kind = 'position'`.
- **File import:** `BookFiles.displayName` reads the provider's real DISPLAY_NAME (the old code
  named books after `lastPathSegment`, i.e. `msf:1000000042`), `detectBookFromFileName` cleans
  it (extension, download litter, brackets, ISBN prefixes, "Title - Author" / "by"), and
  `BookImportConfirm` shows editable title/author fields before the book is created. `addBook`
  now adopts the file's own pages and chapters on import.

---

## 3. THE FULL-APP REDUNDANCY AUDIT (in progress)

**Method used:** (1) inventory every chrome surface and which routes render each; (2) mine every
screen for UI copy that repeats within the file; (3) count `CurioRoutes.X` uses per file to find
two doors to one destination on one screen; (4) read each hit to separate *per-branch* copies
(phone vs wide, full vs compact — legitimate) from *co-visible* duplicates (a real finding).

### Findings — duplicate doors (same destination, one screen)
1. **Home's drawer has two doors to Stats & insights.** The curiosity-map / stat-strip card
   opens `STATS` (`HomeScreen.kt:2471`, `:2474`), and the "Your Curiosity" group holds a
   second row, "Stats & insights", opening the same screen (`:2526`).
2. **Profile has two doors to Settings.** The header's Settings pill (`glassSettingsPill` in the
   glass branch; `ProfileSearchPill` in the classic branch, `:1004`) opens the hub, and the
   list also carries a full `SettingsNavCard` (`:677`).
3. **Profile offers Quests three times** — the bar's streak pill (`onStreakClick`, `:797`), the
   `fullActions` streak pill (`:846`) and the achievements card's `onOpenQuests` (`:628`). The
   first two are the same bar in its two states; the third is a separate card.
4. **Cabinet: two "open the whole shelf" tails** — `onOpenShelf` → `BOOKS` from the
   reading-now shelf (`CabinetV2Content.kt:864`) and from the personal shelf (`:985`).

### Findings — copy that says the same thing twice
5. **Home's drawer stacks three near-synonyms:** the "Your Curiosity" heading with the subtitle
   "Stats, streaks & insights", its own child row "Stats & insights", and the "Quests & Levels"
   row above it with "Track your journey" (`HomeScreen.kt:2483`, `:2504`, `:2526`).
6. **Online mode's page names itself inside itself:** the hero reads "Online mode / Account and
   sync" and a switch row below is titled "Online mode" (`OnlineModeScreen.kt:108`, `:213`).

### Findings — chrome (checked, NOT redundant)
- The **settings rail on 19 screens** is the design (the settings family's own nav); the user
  asked to keep it. The hub no longer has one (v408).
- **No tab screen prints its own name**, and the bar's tabs are distinct
  (Home / Shuffle / Cabinet / Social-opt-in). The **wide-window rail** and the **bottom pill bar**
  never render together (`CurioNavHost` picks one). `LiquidGlassPageNav` is an in-page *page
  turner* (prev / "3 / 12" / next + a jump sheet), not a second tab strip, and it only appears
  where the bottom bar does not.
- The **"repeated" titles** in `RecentScreen`, `RecycleBinScreen`, `BookBrowserScreen`,
  `OnlineModeScreen` are one copy per BRANCH (phone vs wide two-pane) — not visible at once.

### Fixed now (no behaviour removed, so no confirmation needed)
- The last **translucent card fills** in the v408 ladder world became opaque scheme steps:
  `surfaceContainerHigh.copy(alpha = 0.45f)` in Home's drawer group (`HomeScreen.kt:2519`,
  `:2570`), the Cabinet's empty rails (`CabinetV2Content.kt:2011`) and the Community replies
  (`CommunityCommentsSheet.kt:661` — a branch that resolved to very nearly its parent card).
  (Skeleton shimmer placeholder fills are deliberately faint and were left alone.)

### Recommended fixes — ASKED, awaiting the member's pick
- **(a)** Drop Home's duplicate **"Stats & insights"** row (the card above already opens it).
- **(b)** Drop Profile's **`SettingsNavCard`** (the header pill already opens Settings).
- **(c)** Reword the overlapping copy (Home's drawer trio; Online mode's self-naming row) — text
  only, nothing removed.

---

## 4. Still open

- **Nothing here is device-verified.** No Gradle in this environment: every change is
  `node scripts/check_braces.js`-checked, diff-reviewed and CI-built only.
- The gated rails: Home's recents hold (2s + haptic), the pet's home hold, the reader's
  position write — all still want a real device pass.
- The card ladder's white-on-cream values may want a taste pass on a device (one constant per
  step, in `CurioTheme.kt`).

---

## User prompts

Status: (a), (b) and (c) are built and pushed. **(d) the full-app redundancy audit** is
written up above; its three recommended fixes are asked (see "Recommended fixes").

> "also many screen have redundancy alot of redundancy with the app, tabs etc, and and
> unecessary texts duplicate tabs etc etc do a full app audit."

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and its
status stay at the top, and the empty slot below is where the next instruction lands. -->

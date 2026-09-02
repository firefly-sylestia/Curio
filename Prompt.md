# Prompt.md — current request log

## Request: App revamp — step 1: Topic Browser (category-filtered search, dynamic chips, one-category browse)

User direction (verbatim, lightly cleaned):

> "We will be revamping the experience of the app for users, like fully from changing UI to functionality etc based on what user wants, starting with [the] topic browser: let user change category and act that category as filters for the search, so it doesn't always stay on and show all categories when one category is selected; show smart suggestion in a small pill if a result has from another category; make the category chips dynamic — the number it shows if it has that search result, and if it doesn't have that, that category should hide; and in the list when changing category not in search the old category gets shown in the list too which is bad — only 1 should be shown with a button like an arrow and then that category at the top only. Finish this, then we will go to the next step."

### Clarifications asked (ask_user)

1. **Browse list** (Q1) — user answered via Other: "i am not talking about the category chips but the category names that show in the list, and for that one use one category and top arrow." → the complaint is the in-list category section headers; the desired end state = ONE category shown at a time with a top arrow bar. (All keeps its sections.)
2. **Suggestions placement** — "Pill row above results" (a row above/at the top of the result list, tap switches the category filter, search stays).
3. **Dynamic chips scope** — "Search only" (live match counts + hide zero-match lanes while searching; browsing keeps full per-lane totals).
4. **Pill action** — "Switch to that category" (tapping a suggestion pill makes it the active filter).

### Analysis — what existed before this turn (`features/database/TopicDatabaseScreen.kt`)

- **Search ignored the selected category (real bug):** the SEARCH-mode rows builder iterated the whole `catalog` and returned every lane's matches regardless of `effectiveCat` — exactly the "shows all categories when one is selected" complaint.
- **Chip counts were static full-lane totals:** `DatabaseStickyChipBar` took `catalog` + `totalTopics` and rendered every lane chip with `list.size`.
- **Browse with a lane selected** already filtered to that lane (no header), but the All view interleaves per-category section headers — the "category names in the list" the user flagged. No top bar existed.
- No suggestion mechanism existed for cross-category hits while searching.

### What shipped (this turn)

- **Search respects the lane filter:** the search-mode builder now `return@forEach`s any lane ≠ `effectiveCat` (only that category's matches).
- **Dynamic chips (search only):** new off-thread `catHitCounts` produceState (`catalog` + hoisted `indexById` + `matches`), deriving `chips` (hit-count lanes only) + `allChipsCount`; `DatabaseStickyChipBar` now takes precomputed `chips`/`allCount` — chips show live hit counts and zero-hit lanes hide while searching; "All" always visible; browse keeps totals.
- **One category + top arrow bar (browse):** when a lane is selected (not searching), a new `DatabaseCategoryTopBar` ("← Films · 342 topics", the whole pill is a back-to-All button) tops a list that shows only that lane; section headers remain only for All.
- **"Also in" suggestion pills (search):** new `SearchSuggestionRow` renders above the results (and above the empty state) when searching inside a lane and other lanes match — per-lane pills "Films · 4", tap switches `selectedCat` keeping the query.
- Fixed a +1 brace imbalance my first edit introduced in the LazyColumn restructure (added the outer-else close; verified 0/0/0 + empty stack).

### Verification

- Compile/build/lint forbidden in this env (CI validates on push).
- Braces/parens/brackets balanced + no unmatched opens (stack check) on `TopicDatabaseScreen.kt` (original was 0/0/0; mine now 0/0/0).
- `totalTopics =`/`catalog =` no longer passed to the chip bar; `CurioCategories.all`, `CurioIcons.ChevronLeft`, `categorySurface`/`categoryInk` verified to exist.
- Changelog (20260921.txt) + DOX (app/AGENTS.md v313) updated.

### Progress

- [x] Ask clarifying questions (browse layout, suggestions, chip dynamics, pill action).
- [x] Search filtered by the selected category.
- [x] Dynamic chips with live hit counts + hidden zero-match lanes (search only).
- [x] Single-category browse with top arrow bar.
- [x] "Also in" suggestion pill row (search) that switches the filter.
- [x] Changelog + DOX + docs.
- [x] Commit & push.
- [x] CI fix: page-0 scroll state is a `LazyVerticalGrid` → `classicScroll` is now `rememberLazyGridState()` / `ClassicPickerPage.scrollState: LazyGridState` (compile `Argument type mismatch … LazyGridState was expected`); balance re-verified 0/0/0, re-pushed.
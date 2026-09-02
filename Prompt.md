# Prompt.md — current request log

## Request: Category picker — remove all tile borders, hide mix actions behind tap-and-hold, drop the header "New" pill, persist page + scroll

User direction (verbatim, lightly cleaned):

> "In dark mode, the continue-exploring categories, browser categories and page-1 categories all have that white border — please remove, I don't want any border at all. And in Your mixes don't show any 3-dot or edit icon or delete button, only show when you tap and hold. And also in Your mixes remove that New button as there is one at the bottom already. And make page 1 or 2 remember state persistent, and even the scroll, and the page should stay default when the user switches it just as it is now — intended feature."

### Clarifications asked (ask_user)

1. **Border scope** — user picked **"Remove ALL of them"**: tiles and the "+ Add" tile become borderless in light AND dark; the playing mix card's Active ring goes too (the "Active" label stays).
2. **Empty-state button** — user picked **"Keep it"**: the zero-mixes "Build your first mix" CTA stays; only the header "New" pill is removed.

### Analysis — what existed before this turn

- **Borders:** `NewPickerTile` and `AddSuggestionTile` already drew their outline rings LIGHT-mode only (fb4d335c / 5e0321de killed the dark-mode rings; v3xx11 removed the check tick). But the light-mode 1.5dp rings (`outlineVariant` / `catInk`), the "+ Add" ring and `NewMixCard`'s Active ring (`leadAccent @ 55%`) still rendered — user wanted zero borders at all.
- **Mix actions:** v3xx12 added EXPLICIT Edit/Delete buttons to `NewMixCard` (reversing the hidden 3-dot); the Browse Mixes tab `BrowseMixRow` still showed an always-visible 3-dot (with a long-press DropdownMenu already wired). User wants all actions hidden until tap-and-hold.
- **"New" pill:** the Your mixes header row had a `Surface` "New" pill next to the label — redundant with the bottom action row's "+" (Create a mix). Empty-state CTA stays.
- **Page/scroll persistence:** the sheet's default page already persisted (`pickerDefaultPageState` + LaunchedEffect write — the "intended feature" to keep). Scroll positions were NOT persisted anywhere; each page's LazyListState reset on close/reopen, and page flips disposed the off-screen page.

### What shipped (this turn)

- **Borders removed in BOTH themes** (`NewCategoryPicker.kt`): `NewPickerTile` drops the selected/pinned ring, `AddSuggestionTile` drops its ring, `NewMixCard` drops the Active ring. Selection reads via the solid category-tint fill, pinned via the pin badge, playing mix via the "Active" label. `import androidx.compose.foundation.border` deleted (only `.border(` site left is none — verified 0 matches in the picker dir).
- **Mix actions behind tap-and-hold:** `NewMixCard` loses `onEdit`/`onDelete` footer buttons, gains `onLongClick` → new centered `MixOptionPill` overlay (Edit · Delete; mirrors `CategoryOptionPill` styling) driven by a sheet-level `mixOptionTarget` and `NewPickerPage.onMixOption`. `BrowseMixRow` (Browse Mixes tab) drops the always-visible 3-dot trigger — the row's existing long-press DropdownMenu is the only entry point.
- **Header "New" pill removed:** the Your mixes label row is now just `NewSectionLabel("Your mixes · N")`; the bottom action row's "+" creates mixes; empty-state "Build your first mix" CTA kept per user.
- **Page + scroll persistence:** `classicScroll` / `newScroll` `LazyListState`s hoisted into `NewCategoryPickerSheet` (page flips keep position), restored on open with `runCatching { scrollToItem(index, offset) }`, saved debounced (300ms `snapshotFlow` + `drop(1)`) to `KEY_PICKER_PAGE0_SCROLL` / `KEY_PICKER_PAGE1_SCROLL` ("index:offset") via new `AppPreferences.PickerScrollPos` get/set helpers. Default-page behavior untouched (intended).

### Verification

- Compile/build/lint forbidden in this env (CI validates on push).
- Braces/parens/brackets balanced for `NewCategoryPicker.kt` and `NewCategoryPickerBrowse.kt` (script check).
- `.border(` / `MoreVert` / `val dark = isCurioDarkTheme` — 0 matches left in `features/picker/`.
- `kotlinx.coroutines.flow.debounce/drop` + `snapshotFlow` + `rememberLazyListState` imports verified available (flow already used elsewhere in the module).
- Changelog (20260921.txt) + DOX pass (app/AGENTS.md v3xx13) updated.

### Progress

- [x] Ask clarifying questions (border scope + empty-state button).
- [x] Remove all tile borders (NewPickerTile / AddSuggestionTile / NewMixCard Active ring) in both themes.
- [x] Mix actions behind tap-and-hold (MixOptionPill in sheet; 3-dot dropped from Browse Mixes tab).
- [x] Remove header "New" pill; keep empty-state CTA.
- [x] Persist page default (kept) + per-page scroll positions (new).
- [x] Changelog + DOX + docs.
- [x] Commit & push.
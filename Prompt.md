# Prompt.md — current request log

## Request: Share-card editor refinements + book covers not rendering

User report (condensed):

> Card editing needs refinements: (1) when using a tool and switching
> between options, the tool always collapses and you can't continue
> editing; (2) the quick-fact text editing gets stuck — after tapping the
> title (or other element) to move it, tapping the quick-fact box doesn't
> select it for move/edit, text editing always stays for the quick fact;
> (3) while editing, the bottom sheet sometimes closes by mistake — figure
> something out; (4) check recent commits — book covers aren't showing in
> the reveal or synopsis sheet, just a gradient.

## Decisions (asked the user)

- **Tool panels:** keep open while picking options — tap the tool icon
  again to close (user chose "Keep it open").
- **Quick-fact tap:** select-only-first — tapping the fact selects it for
  moving (grip, no keyboard); text editing only via a separate action
  (user chose "Select-only first").

## Root causes found

1. Every option pill in the editor's tool panels ran `toolOpen = null`,
   collapsing the panel on each selection.
2. The fact `BasicTextField` kept focus after tapping title/meta/badge, so
   the keyboard stayed up and the fact stayed "selected" for text editing;
   re-tapping it just moved the cursor. Root fix: explicit focus clearing +
   a disabled-by-default field so taps select for move instead.
3. `TopicShareSheet`'s `ModalBottomSheet` stayed swipe/back/scrim-dismissible
   while editing, discarding edits.
4. Commit `c4f1deb7` (Merge branch 'Alpha' into main, per user) restructured
   `BookCoverPoster`: the gradient placeholder `Box` became a LATER sibling
   child of the `AsyncImage`, so it painted ON TOP of every loaded cover —
   "just a gradient showing".

## Completed

- `app/src/main/java/com/curio/app/ui/components/TopicShareCard.kt`:
  - Tool panels (Design / Text size / Box size / Font / Alignment /
    Format) now stay open while picking options; tap the tool icon to close.
  - Quick fact is select-only-first: the fact `BasicTextField` is
    `enabled = factEditMode` (inert by default), an invisible tap layer
    selects the box for moving (grip, no keyboard), and a new conditional
    **Edit text** tool pill (when FACT is selected) arms + focuses the field
    via `FocusRequester` + `LaunchedEffect`. Tapping Title / Info row /
    Category chip calls `LocalFocusManager.clearFocus()` before switching
    selection, and `onFocusChanged(false)` drops `factEditMode`, so text
    editing never sticks and the keyboard never lingers.
  - Sheet can't be dismissed while editing: `confirmValueChange = { if
    (editMode) it == SheetValue.Expanded else true }` on the sheet state
    (blocks swipe/drag-handle) + `onDismissRequest = { if (!editMode)
    onDismiss() }` (blocks back/scrim). Save/Share/Text still dismiss;
    Done re-enables.
- `app/src/main/java/com/curio/app/features/reveal/TopicRevealScreen.kt`:
  `BookCoverPoster` — gradient moved to the outer Box's own
  `Modifier.background` (behind the image) instead of a later sibling child,
  restoring covers on the reveal page + book-notes sheet.
- Updated `fastlane/metadata/android/en-US/changelogs/20260921.txt` (4 FIX
  bullets) and `app/AGENTS.md` (new v323 bullet).

## Verification

- `git diff --check` passes.
- No Gradle compile/build/lint/test command was run because the project DOX
  explicitly forbids Android Gradle validation in this environment; CI is
  the verification path.
- `ANALYSIS.md` was pre-existing and remains untouched/untracked.
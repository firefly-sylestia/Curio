# Prompt.md — Request log

## Current request — COMPLETED: offline model picker is a full-height scrolling sheet

The user reported the voice-model picker (Settings → Recording → Offline model) looks
"squished" and you "can't see below" the list, and asked for more UI polish.

### Root cause
`OfflineModelDialog` (SettingsSharedComponents.kt) was an `AlertDialog`. AlertDialog has
a fixed max height (~76% of screen, minus insets), and the picker's model list was a
bare `Column` inside the dialog's `text` slot — with the header + intro copy eating
vertical space, the seven catalog rows (small + large/full tiers added in v131) got
squeezed and the bottom rows were clipped out of reach ("can't see below").

### Fix — ModalBottomSheet with a scrolling list (v136)
- Converted to a full-height `ModalBottomSheet` (the app's established sheet pattern:
  `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, `curioDialogContainerColor()`,
  rounded 28dp top corners, `BottomSheetDefaults.DragHandle()`), centered at
  `CurioContentMaxWidth` on wide windows like the other sheets.
- Header: title (titleLarge ExtraBold) + circular close Surface on the right.
- Intro copy (model tiers / download-size warning) stays, plus the error line.
- The model list is now a `LazyColumn` with `weight(1f, fill = false)` — it scrolls
  within the full-height sheet, so all seven models fit with breathing room; a 20dp
  bottom spacer keeps the last row off the gesture bar (the sheet's own insets handle
  navigation bars).
- Polish: row padding 12 → 14×13dp, item spacing 8 → 10dp, glyph spacing 10 → 12dp.
- Cleaned up leftovers from the conversion: the trailing duplicate `downloadError`
  Text inside the LazyColumn (a raw composable in `LazyListScope` — wouldn't compile)
  and the AlertDialog's `confirmButton` closing block. New imports: ModalBottomSheet,
  BottomSheetDefaults, rememberModalBottomSheetState, LazyColumn/items, CircleShape,
  PaddingValues/height/widthIn, CurioContentMaxWidth.

LESSON (recorded in app/AGENTS.md v136): AlertDialog has a fixed max height and clips
long lists — a scrolling list belongs in a bottom sheet or a LazyColumn inside a custom
dialog, never a bare Column in an AlertDialog `text` slot.

### Verification
No Gradle build in this environment (project rule — CI validates on push). Suggested
on-device check: open Settings → Recording → Offline model, confirm the sheet fills the
screen, all seven models scroll into view, and a large model's row shows the Download
action without clipping.

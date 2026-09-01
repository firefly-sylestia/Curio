# Prompt.md — current request log

## Request (ACTIVE): Collage caption box cut off + add height resize to hold-to-edit

User: in Customise for the Collage share card, the caption edit text box is
cut off — the typed/placeholder text inside isn't visible. Also, when you
hold the card to edit, only the LENGTH (width) can currently be changed —
add the HEIGHT too.

### Changes (app/src/main/java/com/curio/app/ui/components/TopicShareCard.kt)

- **Collage caption fix**: the caption `OutlinedTextField` in the Customise
  panel was forced `.height(40.dp)` inside the photo/caption Row, clipping
  M3's 56dp outlined field so the typed text + placeholder were invisible.
  Removed the forced height — the field now gets its natural 56dp row
  height (placeholder txt visible, typing readable, 36-char cap kept).
- **Height resizing in hold-to-edit**:
  - `ShareCardMove` already had `factHeightFrac` (unused) — now applied, and
    a new `titleHeightFrac` added for the title box.
  - `moveTitle`/`moveFact` now apply `fillMaxHeight(frac).clipToBounds()`
    (plus existing width) so the boxes crop on BOTH axes, and the crop
    matches between the live preview and the exported image (the Save/Share
    cards pass `move = move` through `shareComposableCard`).
  - New BOTTOM `ResizeEdge` tabs under the title box (primary color) and
    fact box (tertiary color) — drag to crop height, parallel to the
    existing RIGHT edges for width. `ResizeEdgeSide` enum already supported
    RIGHT + BOTTOM; the ResizeEdge composable's tab geometry already
    flips w/h per side.
  - Inline edit fields (`ArrangeableCard`) got a matching hairline border to
    mark the crop box while editing, plus the title/fact preview uses the
    same width+height math. Edit hint text updated to mention width & height.

### Progress
- [x] TopicShareCard.kt edits (import clipToBounds, data class, helpers,
      BOTTOM resize edges, inline field borders, caption fix).
- [x] Verified ResizeEdgeSide.BOTTOM exists + ResizeEdge handles BOTTOM;
      verified Save/Share exports pass `move` so preview == export.
- [x] Brace/paren balance check (delta clean vs HEAD baseline).
- [x] Committed + pushed (`da226d67`).
  
### CI fix (2fc58b51) — new picker compile errors
CI flagged 9 errors in NewCategoryPicker.kt (all four root causes):
1. `padding(vertical = 2.dp, bottom = 6.dp)` — invalid arg mix → split
   into `padding(top = 2.dp, bottom = 6.dp)`.
2. `androidx.compose.foundation.border(...)` called as a bare extension
   (no receiver) inside Box scope → moved into the Modifier chain
   (`.border(width, color, shape)`) + added the foundation border import.
3. `com.curio.app.ui.theme.onAccent(category)` — onAccent is a
   CurioCategory extension → `category.onAccent()`.
4. Mix editor `selected` state: the elvis
   `laneIds?.toMutableSet() ?: mutableSetOf()` inferred a projected
   `MutableSet<out CategoryId>`, breaking the state delegate setValue and
   cascading into unresolved add/remove/isEmpty/contains at lines
   880–915 → explicit `mutableStateOf<MutableSet<CategoryId>>(...)`.
Pushed — CI revalidates on the new push.

### Verification status
CI validates compilation on push (this environment forbids Gradle builds) —
watch the run after pushing 2fc58b51.
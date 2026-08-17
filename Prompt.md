# Current Request — crash fixes (drawer OOB, Pet Designer negative padding) + FilterSheet floating Apply

## Status: DONE — committed and pushed

## Request
User: "fix this app crah on drawer open and also in category picker theres still soemthing at the button and do the same with filters tooof spin screen" — with a crash report (`IndexOutOfBoundsException: Index 29 out of bounds for length 29`, at draw, drawer open). Then: "another crash in pet designer ui" — `IllegalArgumentException: Padding must be non-negative`.

## Root causes
1. **Drawer crash** — `DrawerLaneConstellation` (HomeScreen.kt) grid web: right/down link guards checked only grid position (`col < c-1`, `row < r-1`), not the array length. With 29 lanes → 6×5 grid (last row 5 nodes), the last node of a short row drew `pts[i+1]`/`pts[i+c]` past the end → OOB at draw on drawer open.
2. **Pet Designer crash** — v179 full-bleed hero used `padding(horizontal = -edgePad)`; Compose throws "Padding must be non-negative" at layout.
3. **FilterSheet** — "do the same with filters": Apply was a full-width bar below the chips; now floats like the picker's Mix/Cancel.
4. **Category picker bottom** — already clean in current code (v180 rework removed the footer; user's crash build predated it).

## Fixes
- HomeScreen.kt: length guards `i + 1 < n` / `i + c < n` added to both link draw branches.
- PetDesignerScreen.kt: negative padding replaced with `BoxWithConstraints` → `offset(x = -edgePad)` + `requiredWidth(maxWidth + edgePad * 2)`; added `requiredWidth` import (BoxWithConstraints/offset already imported).
- SpinScreen.kt FilterSheet: Apply/Surface moved out of the Column, now `align(BottomCenter)` + `padding(bottom = 26.dp)` floating pill (same accent fill/glow/glass); chips scroll column bottom padding 20dp → 88dp so the pill never covers the last row. Braces verified balanced (2066 closes Column, 2102 Box, 2103 ModalBottomSheet, 2104 function).

## Lesson learned (written to app/AGENTS.md v182)
- DrawScope grid-web loops must guard BOTH grid position AND array length (`i + c < n`), not just `col < c-1` — grid math and list length diverge for non-rectangular counts.
- NEVER emit negative `padding()` in Compose — use offset + requiredWidth for full-bleed.

## Docs updated
- fastlane changelog: 3 FIX bullets (drawer crash, pet designer crash, floating filter Apply).
- app/AGENTS.md: v182 entry.
- Prompt.md: this summary.

## Files changed
- app/src/main/java/com/curio/app/features/home/HomeScreen.kt
- app/src/main/java/com/curio/app/features/petdesigner/PetDesignerScreen.kt
- app/src/main/java/com/curio/app/features/spin/SpinScreen.kt
- fastlane/metadata/android/en-US/changelogs/20260920.txt
- app/AGENTS.md
- Prompt.md

## Follow-ups
- Watch CI for the v182 push — the two crashes were runtime bugs CI can't catch; the previous compile fixes are already in.

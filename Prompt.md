# Request Log — Share-card magnet feel, cross-element guides, content persistence

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user, paraphrased)
1. The magnetic snap in the share-card editor is too strong — soften it.
2. Show alignment guides against OTHER elements too (not just the card frame).
3. Custom-fact editing and selecting chapter progress don't survive sharing the
   card: reopening it keeps only the size/position changes. Make content picks
   and the live text persist too, "for better persistent".

## What changed (all in app/src/main/java/com/curio/app/ui/components/TopicShareCard.kt)

### 1 + 2. Magnet + cross-element alignment guides (v342)
- Every drag handle previously ran one-axis `magnetAxis` with a fixed 6 dp
  reach against the card's left/centre/right (top/centre/bottom) lines only.
- `magnetAxis` now takes a `snap` and a `hint` reach plus an `extra` candidate
  list, and returns an `AxisSnap(offset, snapLine, hintLine)`:
  - Snap reach halved 6 -> 3 dp (SNAP_REACH): the box only STICKS when the
    user is actually aiming at a guide (fixes "too strong").
  - Hint band 8 dp (HINT_REACH): a box near a line but past the snap zone
    does NOT grab; it only reports a faint guide line (alignment preview).
- ArrangeableCard computes every selectable element's card-local bounds ONCE
  (`rTitle/rFact/rMeta/rBadge/rCover`, zero = absent) and `alignOthers` /
  `hCands` / `vCands` build the other boxes' left/centre/right (and
  top/centre/bottom) candidates for the dragged box; each of the 5 handles
  (title, fact, meta, badge, cover) feeds those into `magnetAxis`.
- `DragGuides` gained `hintVx/hintHy`; the overlay Canvas draws faint thin
  hint lines under the bright snapped guides. Centring crosshair unchanged.
- Meta keeps its padded clamp; all candidates outside an element's own clamp
  range are ignored, so snapping never pushes a box out of the card.

### 3. Persistence of the picked content + live text (v342)
- `persistEdits()` now also writes: `selectedId`, `customText`,
  `polaroidCaption`, `showChapterProgress`, `reviewChapterNumber`.
- The saved-edit restore effect reads them back; `reviewChapterNumber` was
  moved up to the sheet-state block so the restore can seed it.
- The restored `selectedId` is re-validated: `activeId` only honours it when
  the id names one of the topic's current contents, else the default content
  is used (a stale save can never render a phantom pick).
- Sizes/moves/bodyScale/editedTitle/editedFact keep their old behaviour;
  clearing on leaving the Topic Reveal screen is unchanged.

## Verification
- Brace/paren deltas balanced (+0/+0 braces; +43/+43 parens — the one-paren
  residual imbalance in the file predates these edits, confirmed against
  HEAD). No leftover `nxLine`/`nyLine` or old 6f call sites; 10 call sites
  updated + 1 definition.
- CI (GitHub Actions) validates the compile; this environment forbids Gradle.

## Version note
versionName 1.1.1, versionCode 20260921.

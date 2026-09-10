# Prompt Log — current request
## Request (2026-09-10, completed — settings rail morph, buttery lane drag-reorder + auto-scroll; app animation audit)

User: the sub-settings page + nav-rail switch transition isn't smooth /
morph-style — make it smooth as butter; the "Your lanes" reorder drag
doesn't work (the scroll takes over when trying to drag) — make the
whole row draggable, auto-scroll while dragging, smooth; then analyse
the app and tell what else needs similar smooth animations.

**Shipped (3 files, pushed):**
1. **Nav-rail active pill is now a SHARED ELEMENT** — every settings-
   family destination (hub + all 17 rail-bearing sub-pages/drill-ins) is
   wrapped with the shared-transition scopes (the Spin/Reveal locals),
   and each rail marks its ACTIVE chip with the same
   `settings-rail-active` key, so switching sections MORPHS the highlight
   pill from the old screen's chip to the new screen's chip while the
   pages crossfade (iOS-style rail glide, near-critical spring bounds
   transform). Unselected chips keep their frosted tile; falls back to a
   plain pill when scopes are absent.
2. **Settings-internal page transition upgraded** from a flat crossfade
   to a morph: scale 0.985 (Calm spring) + fade on enter/exit/pop — the
   pages lift in gently instead of dissolving flat.
3. **Manage Categories drag-reorder rebuilt (v3xx)**:
   - The WHOLE row is now the long-press drag surface (was a 40dp handle
     column) — the list scroll can no longer steal the gesture; taps
     still reach the switch/steppers.
   - The dragged row FOLLOWS THE FINGER exactly (snapTo per frame),
     neighbours shift one slot with springs as slots are crossed
     (placeholder math on a FROZEN draft), and on release the order is
     committed once and every row springs to its final slot — the buttery
     settle instead of a snap.
   - AUTO-SCROLL while dragging: a 16ms loop scrolls the list when the
     finger is in the top/bottom 120dp edge zones, speed proportional to
     depth; the scrolled distance feeds the slot math so a stationary
     finger at the edge keeps swapping lanes.
   - Geometry held in non-state holders (WindowPosRef) so layout writes
     never recompose the list mid-drag.

**App animation audit (what else wants the same treatment — analysis
only, not shipped):**
- **Cabinet Everything filter rail (`V2FilterRail`)** — the selection
  accent color snaps instantly; same shared-pill / animateColorAsState +
  scale-pop treatment as the settings rail.
- **Settings quick-tools chips (`SettingsQuickTools`)** — static frosted
  chips, no press/selection animation.
- **Topic Database category panel + active-filter chips** — instant state
  changes (the panel opens, but chip selection doesn't animate).
- **Reveal/Pet-Designer chip rows (`CabinetShelfToggleChips`,
  `LabeledChips`, chapter/episode chips)** — instant selected-state
  swaps; the same animateColorAsState + pop would unify them.
- **Category picker chips** (Picker + Spin sheet) — same instant swap.
  (The bottom nav pill glide and drawer are already animated.)

## Archive
Older completed request logs (2026-09-08 → 2026-09-09) were trimmed from
this file on 2026-09-10 to keep it short. They live in git history
(git log -p -- Prompt.md) if anything needs revisiting.

## next prompt 
(empty — awaiting the next instruction.)
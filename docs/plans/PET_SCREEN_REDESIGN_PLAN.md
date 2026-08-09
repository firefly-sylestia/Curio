# Pet Screen Redesign Plan — Pets / Editor / Settings

## 1. Product goal

Turn the Pet Designer into a clean three-page studio: **Pets** (pick your
companion, see its animations), **Editor** (one focused workspace: choose
what to edit from a dialog, then just the editor), and **Settings** (look
toggles, accessories, personality presets, shapes, export). Remove every
duplicate affordance, make every control discoverable, and keep the
pixel-art warmth of the existing tool.

## 2. Design principles (new)

- **No duplicate buttons.** One save, one chooser, one import/export, one
  place for each setting. Every action exists in exactly one spot.
- **One screen = one job.** Pets page picks the pet; Editor page edits;
  Settings page configures look & extras.
- **Choose, then focus.** The editor shows a chooser dialog at the top;
  once a target is chosen, the editor below is the ONLY content (no
  competing cards).
- **Compact, pinned, reachable.** Save/Undo/Redo/Import live in one slim
  sticky toolbar above the editor — never a second full-width footer.
- **Blueprint everywhere.** Every pixel editor (body, details, eyes) can
  show the pre-edit art ghosted behind the canvas for easy alignment.
- **Advanced but cozy.** Keyboard-free, thumb-friendly touch targets
  (≥48dp), rounded pill controls, soft shadows, springy micro-interactions.

## 3. Information architecture

### 3.1 Nav bar (top, 3 buttons, same style as today)

`Pets · Editor · Settings`

### 3.2 Page 1 — Pets

- **Hero row** of pet cards (`PetRegistry.all`) + a dashed **"More pets
  coming soon"** card (non-blocking toast on tap).
- Tapping a pet selects it: the working design is re-tagged
  (`petSpeciesId`), and if the design is still untouched it is replaced by
  that pet's `defaultDesign` so the new species' art shows immediately.
- **Below the cards, only for the selected pet:** a **Live preview** panel
  (sprite + mood/peeking chips) and the **Animations gallery** (looping
  preview cards) — the two sections that show off the pet.
- No editor controls on this page (no duplicates — editing lives in Editor).

### 3.3 Page 2 — Editor

- **Sticky toolbar (slim, always visible on every page):** Save (primary
  pill), Undo, Redo, Reset, Import, Export, and a dirty dot. One place for
  all of these — the old pinned footer SaveArea is deleted.
- **Sticky chooser strip ("Draw & switch"):** one labeled **"Choose what to
  edit"** trigger + category chips (Body & pose · Faces · Details ·
  Animations · Actions). Tapping opens the **chooser dialog** with preview
  cards per target (existing DrawPickerDialog + new Actions category).
- **Landing (nothing chosen):** the Animation gallery + Live preview
  (showcase the pet while you decide).
- **Chosen target:** ONLY that editor renders below the strip — body/curled
  pixel grid, detail layers, faces, colors, reactions, or the animation
  timeline (which now includes per-frame **eyes** — see §5).
- Animation gallery cards still show custom frames; timeline previews play
  at drawing size.

### 3.4 Page 3 — Settings

- **Accessories & look:** an **"Accessories"** card opens a dialog listing
  each accessory/part (Scarf, Tail, Effects, Antenna, Belly) with a live
  thumb, an enable switch, and a **"Draw it"** shortcut that jumps to the
  Editor page with that part selected. Procedural **disable toggles** sit
  here too (tail / belly / accessories / effects / antenna).
- **Personality presets** (one-tap faces+reactions) — moved here from the
  old Actions landing.
- **Shapes & inspiration** (Default / Robot / Ghost / Random palette /
  Reset all).
- **Export PNG** (Import moves to the Editor toolbar — one entry only).
- Pet library REMOVED here (it's the Pets page).

## 4. Logic redesign

- `PetDesignerPage` → `PETS, EDITOR, SETTINGS` (replaces ANIMATIONS/ACTIONS
  naming; the old Actions page content becomes an Editor target category).
- Pet selection: `design.copy(petSpeciesId = pet.id)`; untouched designs
  swap to `pet.defaultDesign`. Save persists the species via the existing
  `# pet=` text line.
- Target model unchanged (`PetEditorTarget`) — Actions just gains a dialog
  category; the universal editor still renders whatever target is chosen.
- State is hoisted in `PetDesignerScreen` exactly as today (working copy +
  Save toolbar + undo/redo snapshot stack).

## 5. Eyes editor (per animation frame) — new

- **Data:** `PetAnimationFrame.eyeGrid: List<String>?` — a fixed 16×16 eye
  authoring layer (the same space the procedural eyes use), serialized as
  `e=` inside `frame=` text lines; parsed + padded to 16 rows.
- **Sprite:** the hardcoded `when (eyes)` art is refactored into an
  `EYE_STYLE_PIXELS` data table (shared by the renderer AND the editor
  blueprint). New `eyeOverride` param: when a frame carries `eyeGrid`, it is
  drawn (palette-aware, inside the same glance translate) instead of the
  procedural style — so eyes can blink/wink/glance frame by frame.
- **Editor:** the animation timeline's frame drawer gains an **Eyes** tab
  next to Body/Asleep. It renders a 16×16 grid with the procedural eye art
  of the frame's mood as a locked **blueprint behind**, the same palette +
  brush/fill/eraser/eyedropper, and a live mini sprite preview wearing the
  layer. Per-frame, per-animation — persists with the design.
- Thumbnails + big preview + gallery all render `eyeGrid` automatically.

## 6. UX / beauty checklist (applies to every screen)

- Every tappable ≥48dp, pill-shaped, with a pressed state (Surface
  onClick), labeled text, and an icon where it clarifies.
- No two controls in the same row do the same thing; primary actions use
  the primary color, secondary actions are tonal, destructive are quiet.
- Empty states explain themselves ("More pets coming soon", "Pick a tool to
  start editing").
- Sticky toolbar + strip never overlap content (stickyHeader + padding).
- Preserve the cream card style (`SectionCard`), rounded corners, soft
  tonal elevation, existing palette — this is a reorganization, not a
  reskin.

## 7. Out of scope (this pass)

- New pet species art (registry has Curie only; the UI is ready for more).
- Server AI, social features, monetization.
- Full 3D/vector rendering — stays pixel art.

## 8. Implementation order

1. Models: page enum rename, `eyeGrid` + serialization, `EYE_STYLE_PIXELS`
   + `eyeOverride` in the sprite.
2. Sticky save/import toolbar (delete pinned SaveArea).
3. Pets page (library + previews below) + pet selection.
4. Editor page (chooser strip + dialog + Actions category) and Settings
   page (accessories dialog, toggles, presets, shapes, export).
5. Eyes tab in the animation timeline (grid + blueprint + preview).
6. Validate, review, changelog, commit.

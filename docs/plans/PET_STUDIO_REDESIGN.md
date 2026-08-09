# Pet Studio Redesign — v9 (fresh plan, user-directed)

> Replaces the deleted PET_SCREEN_REDESIGN_PLAN.md and
> PET_DESIGNER_UNIVERSAL_EDITOR_PLAN.md. The user rejected those plans
> wholesale ("the current design plan is bad… make a new one") and answered
> design direction questions in one session. **Do not consult the old docs.**

## 1. Product goal

A cute, premium pet studio with a real app feel: a **bottom navigation bar**
(Pets · Editor · Settings), a pet library that includes the user's own
**custom pets (2 slots, always-on)**, and an Editor that is the **center of
the screen** — one picker dialog is the only chooser, and once a target is
chosen ONLY that editor renders, so it's always obvious what you're editing.

## 2. Design principles

- **Bottom bar, icons + labels** — M3 NavigationBar mirroring the main app
  (Pets 🐾 · Editor 🖌 · Settings ⚙). Always visible on the studio screen.
- **Editor is the center of the screen.** One "What do you want to edit?"
  button → one picker dialog (everything inside). After choosing, a clear
  "Editing: {title}" header with a Change chip, and just that editor below.
  No competing cards, no strips, no galleries on the Editor page.
- **Custom pets.** The user's saved looks live as real pet cards (2 slots,
  always-on — no Settings toggle). "Save as new pet" fills a slot; Save
  refreshes whichever pet is being edited.
- **No dead taps.** Tapping an animation ALWAYS opens a full-screen player
  (fixes the old "tapping Animations did nothing" bug — the gallery used to
  set a hidden editor target on the wrong page).
- **Keep every feature, one at a time.** Body, curled pose, detail layers,
  faces, reactions, dialogue, custom actions, colors, shapes all stay — but
  presented one editor at a time so it never feels like "too many buttons".
- **Premium cute.** Soft rounded cards, tonal surfaces, springy pill
  controls, live sprite previews everywhere, playful copy.

## 3. Information architecture

### 3.1 Bottom navigation (M3 NavigationBar, icons + labels)

| Tab      | Icon                | Page                                  |
|----------|---------------------|---------------------------------------|
| Pets     | CurioIcons.Pets     | Pet library + previews + animations   |
| Editor   | CurioIcons.Brush    | The one-at-a-time editor (center)     |
| Settings | CurioIcons.Settings | Look toggles, presets, shapes, export |

Switching pages clears the editor target (land clean on every page).

### 3.2 Page 1 — Pets

- **"My pets"** section: Curie (built-in) card + **2 custom pet slots** +
  a dashed "More pets coming soon" placeholder (kept — user likes it).
  - Custom slot filled: shows the saved design's sprite, name
    ("Custom 1"/"Custom 2"), a "Your pet" badge when it's the active pet,
    and a small ✕ delete affordance.
  - Custom slot empty: a dashed "Save as new pet" card — tapping saves the
    current editor design into that slot (fills the first empty one).
  - Section header row has a "+ Save as new pet" pill too.
- **Live preview** (active pet's design, mood chips) — unchanged feel.
- **Animations gallery** — tapping a card opens the **full-screen player**
  (not a dead tap): big looping preview, play/pause, frame-step, and an
  "Edit frames" button that jumps to the Editor page with that animation
  open.

### 3.3 Page 2 — Editor (the center of the screen)

- No target chosen: one big primary **"What do you want to edit?"** card +
  a friendly hint ("Pick something and it opens right here — one editor at
  a time"). That's the ONLY affordance on the page.
- Target chosen: sticky **"Editing — {title}"** header (icon + title +
  "Change" chip that reopens the picker) and then ONLY that editor card:
  Body / Curled pose / Colors / Detail layer / Face / Reaction / Custom
  action / Animation timeline. Nothing else competes.
- The picker is ONE dialog with category chips INSIDE it
  (Body & pose · Faces · Details · Animations · Actions) — every target is
  reachable from the same dialog, exactly as requested ("picker with dialog
  which opens up that only").

### 3.4 Page 3 — Settings

Unchanged content, reorganized: Accessories & look (dialog with live
thumbs + "Draw it" shortcuts), one-tap personality presets, Shapes &
inspiration (Default / Robot / Ghost / Random palette / Reset all),
Export PNG.

### 3.5 Sticky toolbar (all pages)

Save (primary pill) · Undo · Redo · Reset · Import · Export + status line
(dirty dot / toast / hint). Save writes the design globally (the floating
pet wears it) AND refreshes the active custom pet slot when editing one.

## 4. Custom pet data model

- `AppPreferences`: `KEY_PET_CUSTOM_1 = "pet_custom_1"`,
  `KEY_PET_CUSTOM_2 = "pet_custom_2"` — each stores a full `PetDesign`
  text. Reactive `customPetsState: List<String?>` (2 entries) seeded in
  `loadAll`.
- `getCustomPets(context)` / `setCustomPet(context, index, text)` /
  `clearCustomPet(context, index)`.
- Screen state: `activeCustomSlot: Int?` (null = built-in Curie; session
  state only — the designer opens on the global design = Curie).
- **Save**: `setPetDesign` (global, as today) + if `activeCustomSlot !=
  null`, `setCustomPet(slot, design.toText())`.
- **Save as new pet**: fills the first empty slot with the current design
  and sets `activeCustomSlot`; when both slots are full the toast explains
  "pick a custom pet to replace it" (replace = delete slot first).
- **Select Curie**: existing `selectPet` (re-tags species), clears
  `activeCustomSlot`.
- **Select custom slot**: parses the slot's design text as the working copy,
  sets `activeCustomSlot`.
- **Delete slot**: clears the slot; if it was active, back to Curie.

## 5. UX / beauty checklist

- Every tappable ≥44–48dp, pill-shaped, pressed state, icon where helpful.
- Primary actions primary-colored; destructive quiet; empty states explain
  themselves ("Save as new pet", "Pick a tool to start editing").
- One editor at a time; the Editing header always answers "what am I
  editing?".
- Full-screen player: dark scrim, big centered sprite, obvious controls.
- Preserve the cream card style (`SectionCard`), rounded corners, soft
  tonal elevation, existing palette.

## 6. Out of scope (this pass)

- New built-in pet species art (registry stays Curie-only; custom pet SLOTS
  are user designs, not species definitions).
- Renaming custom pets (names are slot-based "Custom 1/2").
- Server AI, social, monetization, 3D rendering.

## 7. Implementation order

1. `AppPreferences`: custom pet slots (keys, reactive state, seed, helpers).
2. Bottom `NavigationBar` (Pets/Editor/Settings) replacing the top tabs;
   delete `PetDesignerNavbar` + `DrawPickerStrip`.
3. Pets page: custom pet cards + Save-as-new-pet + delete + placeholder.
4. Editor page: picker trigger / Editing header + only-one-editor.
5. Full-screen animation player + gallery wiring (dead-tap fix).
6. DrawPickerDialog: internal category chips.
7. Toolbar Save/custom-pet wiring; version bump + changelog; Prompt.md;
   validate; review; commit & push.

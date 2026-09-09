# Field Menu — 3-Dot Overflow Menu Spec

A round icon-trigger overflow menu (⋮) used anywhere a card or row needs secondary actions. Same interaction family as any dropdown in the app: tap to open and tap to choose, or press-hold-drag-release without lifting your thumb.

## 1. Design tokens

### Color
| Token | Hex | Use |
|---|---|---|
| `surface` | `#FFFFFF` | Menu panel, trigger fill on press |
| `ink` | `#1E2A1C` | Item labels |
| `ink-muted` | `#626B5A` | Icons, dividers, the dot glyph itself |
| `line` | `#E1E4D6` | Panel border, dividers |
| `moss` | `#5C7A52` | Hover / drag-over highlight, focus ring |
| `moss-tint` | `rgba(92,122,82,0.08)` | Hover/drag-over fill |
| `ochre` | `#B8853B` | Hold-mode indicator on the trigger and the armed item's left edge |
| `brick` | `#9B4B3F` | Destructive item label + icon |
| `brick-tint` | `rgba(155,75,63,0.08)` | Destructive item hover/drag-over fill |

### Type
System sans, 14.5px / 400 for items, 600 for a destructive item's icon weight only (visual weight, not boldface text — a destructive action should feel heavier without shouting in caps or color alone).

### Radius
- 16px — menu panel
- 10px — each item's hover/drag-over highlight
- Trigger is a full circle (999px)

### Spacing
- Trigger: 40×40px circle, dot glyph centered
- Item height: 48px (touch target floor, matches every other menu in the app)
- Item horizontal padding: 12px, icon-to-label gap: 10px
- Panel padding: 6px, min width 210px
- Divider: 1px `line`, 6px vertical margin, inset 4px from panel edges

## 2. Anatomy
```
              ⋮   ← trigger, 40×40 circle
              ┌───────────────────────┐
              │ ✎  Edit entry         │
              │ ⧉  Duplicate          │
              │ ↗  Share              │
              │ ▢  Archive            │
              ├───────────────────────┤
              │ ⌫  Delete             │  ← destructive, brick ink
              └───────────────────────┘
```
Menu is right-aligned to the trigger (kebabs usually sit at a card's top-right corner, so the panel should open toward the content, not off the edge of the screen).

## 3. Interaction model

**A. Tap flow**
1. Tap the dot trigger → panel opens (140ms scale+fade from the top-right corner).
2. Tap an item → action fires, panel closes.
3. Tap outside, or press Escape → closes with no action. Arrow keys move a focus highlight; Enter activates it.

**B. Hold-drag flow**
1. Press and hold the trigger (~180ms) → the trigger gets an `ochre` ring and the panel opens instantly under the thumb, no animation delay.
2. Drag over items → the one under the thumb highlights (`moss-tint` fill, `ochre` left edge), or `brick-tint` / brick edge if it's the destructive item — the color change is itself a warning that lifting here deletes something.
3. Lift over an item → it fires, panel closes.
4. Lift outside any item → panel closes, nothing fires.

This is the same shape as a stock Android launcher's press-hold-drag-to-shortcut gesture — hold the icon, drag to the option you want without a second tap, release to commit.

### Making the drag actually track the pointer
The trigger explicitly captures the pointer on press (`setPointerCapture`), rather than relying on the browser's default behavior — default capture only applies automatically to touch input, so on a mouse or trackpad the drag would stop tracking the instant the cursor left the button. Explicit capture keeps `pointermove`/`pointerup` reporting to the trigger regardless of input type, so hit-testing which item is under the pointer works the same on a phone or a laptop trackpad.

## 4. States
- **Default trigger** — `ink-muted` dots, transparent fill.
- **Pressed (tap, pre-threshold)** — `moss-tint` fill fades in, dots stay `ink-muted`.
- **Held (drag mode)** — `ochre` ring around the trigger; this ring is the only place a person sees they've crossed into drag mode rather than a normal open.
- **Item hover/drag-over** — `moss-tint` fill, `moss` 3px left edge, `moss` icon tint.
- **Destructive item hover/drag-over** — `brick-tint` fill, `brick` 3px left edge.
- **Disabled item** — 35% opacity, no highlight on hover or drag-over.

## 5. Accessibility
- Trigger: `aria-haspopup="menu"`, `aria-expanded`, `aria-label="More actions"` (the dots alone say nothing to a screen reader).
- Panel: `role="menu"`, items `role="menuitem"`.
- Full keyboard path independent of the drag gesture: Enter/Space opens, arrow keys move, Enter chooses, Escape closes.
- Destructive action is marked by more than color — it also sits below a divider, separated from routine actions, so a mistaken tap requires deliberately reaching past the gap.

## 6. Do / Don't
- Do keep the 180ms hold threshold identical to every other hold-capable menu in the app — inconsistent timing is what makes a gesture feel unreliable.
- Do explicitly capture the pointer on press; don't depend on implicit touch capture.
- Don't place more than one destructive item per menu, and don't put it anywhere but last, below a divider.
- Don't let the panel render off the right edge of the viewport — flip its anchor to the left if there isn't room.

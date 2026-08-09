# Pet Designer Universal Editor Redesign Plan

## 1. Vision

Redesign the Curio Pet Designer into a single, universal creation studio where the user chooses **what** to edit first, then edits that target in one consistent editor. The new designer must be clearer than the current multi-tab playground, support future pets, and prepare for frame animation, dialogue, action, and custom behavior editing without another redesign.

The redesigned system should feel like a small character editor:

- Pick a pet.
- Pick an animation, action, part, face, or setting category.
- Edit it in one universal editor.
- Watch a real live preview update immediately.
- Save from one obvious save area below the editor.

## 2. Product Goals

1. **One universal editor**
   - Replace separate confusing editing tabs with one editing surface.
   - The selected target controls the editor mode.
   - The editor should be reusable for body pixels, detail layers, eyes, mouth, face moods, animation frames, actions, dialogue, and future custom actions.

2. **Clear navigation**
   - Add a local Pet Designer navbar with three pages:
     - `Animations`
     - `Actions`
     - `Settings`
   - The navbar is local to Pet Designer and separate from the app-wide nav.

3. **Live preview everywhere**
   - Editing must show the real pet using the current unsaved working design.
   - The preview should not wait until Save.
   - Animation/action previews should replay in the editor.

4. **Blueprint-first editing**
   - Every editable target should have a blueprint/reference layer.
   - Users can compare against default art, saved art, previous frame, or anchor guides.

5. **Better drawing**
   - Remove the confusing draw toggle.
   - Tapping Brush/Fill/Eraser/Eyedropper enters edit mode automatically.
   - Tool state must be obvious.
   - Add Undo.
   - Improve canvas clarity, grid visibility, and pixel feedback.

6. **Professional color workflow**
   - Add a proper color picker with a preview, swatches, hex input, recent colors, and a color wheel or hue/saturation/lightness controls.

7. **Future pets and actions**
   - Structure the UI and data model so more pets, more animations, and custom actions can be added later.

## 3. Existing Code Touchpoints

Primary files likely involved:

- `app/src/main/java/com/curio/app/features/petdesigner/PetDesignerScreen.kt`
  - Current editor UI, tool state, import/export, reaction editing, color editing, and preview controls.
- `app/src/main/java/com/curio/app/data/PetDesign.kt`
  - Current saved design model.
  - Stores palette, body rows, curled rows, grid size, faces, reactions, details, and procedural visibility.
- `app/src/main/java/com/curio/app/data/CurioPet.kt`
  - Current pet moods/reaction concepts.
- `app/src/main/java/com/curio/app/ui/pet/CurioPetSprite.kt`
  - Actual pet sprite rendering used by the app.
- `app/src/main/java/com/curio/app/ui/pet/CurioFloatingPet.kt`
  - Floating pet behavior.
- `scripts/validate_petdesigner.py`
  - Static validation for Pet Designer. Update this if the required structure changes.

## 4. Core User Flow

### 4.1 Open Pet Designer

User enters Pet Designer from Settings or the existing route.

The screen shows:

```text
┌────────────────────────────────────────────┐
│ Top app bar                                │
│ Title: Pet Designer                        │
│ Optional: current pet chip                 │
├────────────────────────────────────────────┤
│ Local Pet Designer navbar                  │
│ [Animations] [Actions] [Settings]          │
├────────────────────────────────────────────┤
│ Page-specific picker                       │
│ - Animation preview cards, OR              │
│ - Action/dialogue cards, OR                │
│ - Saved/settings cards                     │
├────────────────────────────────────────────┤
│ Universal Editor                           │
│ - Selected target title                    │
│ - Blueprint controls                       │
│ - Live preview                             │
│ - Editable canvas/form/timeline            │
│ - Tool tray                                │
│ - Color preview                            │
├────────────────────────────────────────────┤
│ Save area                                  │
│ [Save pet] [Undo] [Reset changes]          │
│ Optional: unsaved changes text             │
└────────────────────────────────────────────┘
```

### 4.2 Choose What To Edit

The user does not first choose a tool. They first choose the target:

- Animation
- Frame
- Body
- Curled/asleep pose
- Tail
- Antenna
- Accessory
- Effect layer
- Eyes
- Mouth
- Mood face
- Reaction/action
- Dialogue
- Settings/saved design

The chosen target opens in the same universal editor.

### 4.3 Edit

The editor adapts:

- Pixel targets show canvas tools.
- Face targets show eye/mouth/blush/sparkle controls plus preview.
- Animation targets show timeline/frame tools plus preview.
- Action targets show trigger, animation, face, and dialogue controls.
- Settings targets show saved design/import/export controls.

### 4.4 Save

Save must be below the editor.

The user should always see a primary button:

- `Save pet`

Secondary actions:

- `Undo`
- `Redo` later
- `Reset changes`
- `Export` from Settings

## 5. Local Navbar Design

### 5.1 Navbar Pages

#### Page 1: Animations

Purpose: edit the pet's look, animation poses, frame art, body details, and faces.

Contains:

- Animation preview gallery
- Mood/pose previews
- Body/detail part picker
- Frame timeline once an animation is selected

Suggested sections:

1. `Animation previews`
2. `Body & poses`
3. `Face parts`
4. `Detail layers`

#### Page 2: Actions

Purpose: edit what the pet does and says.

Contains:

- Built-in reaction cards
- Dialogue editor
- Action animation editor
- Face used during action
- Future custom action editor

Suggested built-in actions:

- Touch
- Spin landed
- Topic reveal
- Explore started
- Capture saved
- Play
- Level up
- Idle moment
- Sleep/wake
- Streak achieved later

#### Page 3: Settings

Purpose: manage saved designs and editor preferences.

Contains:

- Save slots later
- Current saved design
- Import design
- Export design
- Reset to default
- Duplicate design later
- Rename design later
- Grid size conversion
- Blueprint default preferences
- Pet selector later

### 5.2 Navbar Visual Requirements

- Use a segmented control or compact tab row.
- Labels should be simple: `Animations`, `Actions`, `Settings`.
- Selected page should have a strong active state.
- Do not overload the navbar with every editable part.
- The page content below the navbar handles detailed selection.

## 6. Universal Editor Model

Introduce a target-based editing model.

Suggested Kotlin models:

```kotlin
private enum class PetDesignerPage {
    ANIMATIONS,
    ACTIONS,
    SETTINGS
}

private sealed interface PetEditorTarget {
    val id: String
    val title: String

    data object Body : PetEditorTarget {
        override val id = "body"
        override val title = "Body"
    }

    data object CurledPose : PetEditorTarget {
        override val id = "curled"
        override val title = "Curled pose"
    }

    data class DetailLayer(
        val key: String,
        override val title: String
    ) : PetEditorTarget {
        override val id = "detail:$key"
    }

    data class Face(
        val mood: String
    ) : PetEditorTarget {
        override val id = "face:$mood"
        override val title = "$mood face"
    }

    data class Animation(
        val animationId: String,
        override val title: String
    ) : PetEditorTarget {
        override val id = "animation:$animationId"
    }

    data class AnimationFrame(
        val animationId: String,
        val frameIndex: Int
    ) : PetEditorTarget {
        override val id = "animation:$animationId:frame:$frameIndex"
        override val title = "Frame ${frameIndex + 1}"
    }

    data class Action(
        val eventId: String
    ) : PetEditorTarget {
        override val id = "action:$eventId"
        override val title = "$eventId action"
    }

    data class Dialogue(
        val eventId: String
    ) : PetEditorTarget {
        override val id = "dialogue:$eventId"
        override val title = "$eventId dialogue"
    }

    data object SavedDesigns : PetEditorTarget {
        override val id = "settings:saved"
        override val title = "Saved designs"
    }
}
```

The exact model can change, but the important design is that the editor receives a target instead of hardcoding separate tabs.

## 7. Universal Editor UI

The universal editor should have stable zones:

```text
Universal Editor
┌────────────────────────────────────┐
│ Target header                      │
│ Example: Editing Happy animation   │
├────────────────────────────────────┤
│ Preview + blueprint controls       │
├────────────────────────────────────┤
│ Main editing area                  │
│ - Pixel canvas, OR                 │
│ - Face controls, OR                │
│ - Timeline, OR                     │
│ - Dialogue/action form             │
├────────────────────────────────────┤
│ Contextual tools                   │
│ Brush / Fill / Eraser / Color etc  │
└────────────────────────────────────┘
```

### 7.1 Target Header

Show:

- Current target name
- Breadcrumb if helpful
  - Example: `Animations / Happy / Frame 2`
- Unsaved marker if this target has changes
- Small reset-target action

### 7.2 Preview Area

Show:

- Live pet preview using the current working design
- Animation play/pause if editing animation/action
- Current frame indicator when editing frame
- Dialogue bubble when editing dialogue/action

### 7.3 Main Editing Area

Mode depends on target:

- Pixel grid for body/detail/frame targets
- Face controls for face targets
- Timeline for animation targets
- Dialogue/action form for action targets
- Saved design cards for Settings targets

### 7.4 Contextual Tool Tray

Only show tools that make sense for the selected target.

Pixel targets:

- Brush
- Fill
- Eraser
- Eyedropper
- Undo
- Color picker
- Blueprint toggle
- Grid toggle
- Mirror tool later

Face targets:

- Eye selector
- Mouth selector
- Blush toggle
- Sparkles toggle
- Face overlay pixel edit later
- Undo

Animation targets:

- Play/pause
- Add frame
- Duplicate frame
- Delete frame
- Frame duration
- Previous frame blueprint
- Undo

Action targets:

- Enabled toggle
- Animation selector
- Face selector
- Dialogue editor
- Preview action
- Undo

Settings targets:

- Import
- Export
- Reset default
- Manage saved designs

## 8. Blueprint System

Blueprints must make editing easier and reduce confusion.

### 8.1 Blueprint Modes

Suggested enum:

```kotlin
private enum class PetBlueprintMode {
    OFF,
    DEFAULT_REFERENCE,
    SAVED_VERSION,
    PREVIOUS_FRAME,
    ANCHOR_GUIDES,
    SYMMETRY_GUIDE
}
```

### 8.2 Blueprint Use Cases

Body editing:

- Default body blueprint
- Saved body blueprint
- Symmetry centerline

Curled/asleep pose editing:

- Default curled pose blueprint
- Body pose ghost

Detail layer editing:

- Body underneath as faint guide
- Anchor guides for tail, antenna, accessories, effects

Face editing:

- Eye anchor points
- Mouth anchor points
- Face safe zone

Animation frame editing:

- Previous frame ghost
- First frame ghost
- Saved frame ghost
- Motion onion-skin later

Action/dialogue editing:

- Preview state blueprint is less important, but show trigger/action summary.

### 8.3 Blueprint Visual Style

- Render behind editable pixels.
- Use low opacity, around 20-35%.
- Use dashed/outlined guides for anchors.
- Give users a clear label such as `Blueprint: Default body`.
- Include quick toggle: `Blueprint on/off`.

## 9. Live Preview System

### 9.1 Requirements

The live preview should always use the unsaved working design.

It should reflect:

- Palette changes
- Body grid changes
- Curled grid changes
- Detail layers
- Procedural visibility
- Face overrides
- Reaction changes
- Dialogue changes
- Animation frame changes when implemented

### 9.2 Preview Modes

Suggested preview modes:

- `Still`
- `Play animation`
- `Current frame`
- `Action preview`
- `Dialogue preview`
- `In-app size`
- `Large editor size`

### 9.3 Preview Controls

Show controls based on target:

- Animation target: Play/Pause, speed, frame step
- Action target: Preview action button
- Dialogue target: Preview speech bubble
- Face target: Mood selector
- Pixel target: Toggle blueprint/grid

## 10. Drawing Interaction Redesign

### 10.1 Remove Draw Toggle

The current drawing toggle should be removed.

New behavior:

- No active drawing tool means canvas is safe to scroll/inspect.
- Selecting Brush/Fill/Eraser/Eyedropper activates edit mode.
- Tapping outside tools or selecting preview mode can deactivate edit mode.

Suggested state:

```kotlin
private enum class PaintTool {
    BRUSH,
    FILL,
    ERASER,
    EYEDROPPER
}

private data class ToolState(
    val activeTool: PaintTool? = null,
    val selectedPaletteKey: Char = 'b',
    val showGrid: Boolean = true,
    val blueprintMode: PetBlueprintMode = PetBlueprintMode.DEFAULT_REFERENCE
)
```

### 10.2 Canvas Feedback

When editing is active:

- Highlight the canvas border.
- Show label: `Editing with Brush`.
- Show selected color chip.
- Show tapped/dragged cell feedback.

When editing is inactive:

- Show label: `Choose a tool to edit`.
- Allow normal scroll behavior.

### 10.3 Drag Painting

Brush and eraser should support drag painting.

Rules:

- Paint only when active tool is Brush or Eraser.
- Avoid duplicate history entries for every tiny drag cell; group a drag gesture into one undo step.
- Fill and Eyedropper should be tap actions.

## 11. Undo and Redo

### 11.1 Undo Requirements

Undo should support every user-visible edit:

- Pixel brush stroke
- Fill bucket
- Eraser stroke
- Eyedropper does not need undo because it does not mutate design
- Palette color change
- Face control changes
- Reaction/action changes
- Dialogue changes
- Detail layer changes
- Animation frame changes
- Settings changes that mutate the working design

### 11.2 Initial Implementation

Use full `PetDesign` snapshots first.

Suggested model:

```kotlin
private data class PetEditorHistory(
    val undoStack: List<PetDesign> = emptyList(),
    val redoStack: List<PetDesign> = emptyList()
)
```

Before mutation:

1. Push current `design` onto `undoStack`.
2. Clear `redoStack`.
3. Apply mutation.

Undo:

1. Pop last item from `undoStack`.
2. Push current design to `redoStack`.
3. Replace working design with popped design.

Redo can be added immediately if simple; if not, reserve the model for later.

### 11.3 Gesture Grouping

Brush drag should create one undo entry per drag gesture, not per cell.

Implementation approach:

- On pointer down: push one snapshot.
- During drag: mutate cells without pushing more snapshots.
- On pointer up: finish gesture.

## 12. Tool Tray Redesign

### 12.1 Primary Tools

Always available for pixel-editable targets:

- Brush
- Fill
- Eraser
- Eyedropper
- Undo
- Color

### 12.2 Secondary Tools Later

Future tools:

- Redo
- Mirror X
- Mirror Y
- Line
- Rectangle
- Circle
- Move selection
- Copy selection
- Paste selection
- Rotate frame
- Flip frame
- Clear layer
- Copy body to curled
- Copy frame to next frame

### 12.3 Tool Visual Design

Each tool button should have:

- Icon
- Label or tooltip
- Active state
- Disabled state when unavailable
- Short helper text under the tray when active

Examples:

- Brush active helper: `Drag on the grid to paint Body color.`
- Fill active helper: `Tap a region to fill it.`
- Eyedropper active helper: `Tap a pixel to pick its color slot.`
- Eraser active helper: `Drag to erase pixels.`

## 13. Color Picker Redesign

### 13.1 Color Picker Requirements

The color picker should include:

- Large selected color preview
- Active palette slot name
- Active palette key
- Hex input
- Quick swatches
- Recent colors
- Color wheel or hue strip
- Saturation/lightness controls
- Before/after comparison
- Apply/cancel actions

### 13.2 Color Storage

Keep storing palette colors as six-character `RRGGBB` strings to remain compatible with `PetDesign`.

### 13.3 Palette Slots

Current slots should remain:

- Body
- Shade
- Ink
- Scarf
- Scarf dark
- Gold
- Gold deep
- Custom 1
- Custom 2
- Custom 3
- Custom 4
- Blush
- Eyes

### 13.4 Recent Colors

Store recent colors either:

- In editor state only for the first implementation, or
- In preferences later if users expect persistence.

Keep recent colors deduplicated and capped, e.g. 12 colors.

### 13.5 Color Wheel Implementation

If no suitable Material/Compose color wheel API exists in the project versions, implement it with a custom `Canvas`.

Important compile-safety note:

- Do not name any parameter `size` inside a function that contains `Canvas {}`.
- Use names like `wheelSize`, `canvasSize`, or `pickerSize`.

## 14. Animation System

### 14.1 Animation Preview Gallery

The Animations page should start with visual cards.

Each card should show:

- Animation name
- Looping mini preview
- Current pet design
- Optional frame count
- Optional edited marker

Suggested animation cards:

- Idle
- Happy
- Excited
- Sleepy
- Curious
- Proud
- Bouncy
- Focused
- Touch
- Spin landed
- Reveal
- Explore
- Save
- Play
- Level up

### 14.2 Animation Timeline

When an animation is selected, the universal editor should show:

- Frame thumbnails
- Selected frame highlight
- Add frame
- Duplicate frame
- Delete frame
- Reorder later
- Duration field/chip
- Play/pause
- Previous/next frame step
- Onion-skin/blueprint toggle

### 14.3 Frame Editing

Each frame can include:

- Body/pose override
- Detail layer override
- Face override
- Optional transform later:
  - Offset X/Y
  - Scale
  - Rotation
  - Bounce height

### 14.4 Suggested Data Model

Future-safe model:

```kotlin
data class PetAnimation(
    val id: String,
    val name: String,
    val frames: List<PetAnimationFrame>,
    val loop: Boolean = true,
    val defaultFrameDurationMs: Int = 180
)

data class PetAnimationFrame(
    val id: String,
    val durationMs: Int = 180,
    val bodyRows: List<String>? = null,
    val curledRows: List<String>? = null,
    val detailOverrides: Map<String, List<String>> = emptyMap(),
    val face: PetFace? = null,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f
)
```

Implementation should keep old designs working if `animations` is absent.

## 15. Face, Eyes, and Mouth Editing

### 15.1 Face Editor Targets

Face editing should be part of the universal editor, not a separate confusing tab.

Targets:

- Happy face
- Excited face
- Sleepy face
- Curious face
- Proud face
- Bouncy face
- Focused face
- Action-specific face override

### 15.2 Eye Editing

Eye editor should include:

- Eye style selector
- Eye color slot picker
- Eye anchor preview
- Optional pixel overlay later
- Preview across moods

Current eye styles should remain compatible with existing `EyeStyle`.

### 15.3 Mouth Editing

Mouth editor should include:

- Mouth style selector
- Mouth anchor preview
- Preview across moods

Current mouth styles should remain compatible with existing `MouthStyle`.

### 15.4 Expression Controls

Controls:

- Eyes
- Mouth
- Blush
- Sparkles
- Optional custom face overlay later

## 16. Detail Layer Editing

Editable detail layers should include:

- Tail
- Accessories
- Effects
- Antenna
- Future custom detail layers

Each layer should:

- Open in the universal pixel editor
- Show body as a blueprint underneath
- Support hiding/showing procedural default art
- Support reset layer
- Support copy from default

Suggested UI:

```text
Detail layers
[ Tail ] [ Antenna ] [ Accessories ] [ Effects ] [+ Custom later]

Universal Editor:
- Canvas edits selected layer
- Body blueprint behind it
- Toggle: show procedural default
- Toggle: show other layers
```

## 17. Actions Page

### 17.1 Built-In Actions

The Actions page should list built-in actions as cards.

Each action card:

- Name
- Trigger summary
- Current animation
- Current face preview
- Dialogue preview snippet
- Enabled/disabled state
- Edited marker

Built-in action examples:

- Touch
- Spin landed
- Reveal
- Explore
- Save
- Play
- Level up

### 17.2 Action Editor

Selecting an action opens universal editor in action mode.

Action editor fields:

- Enabled
- Animation
- Face expression
- Dialogue lines
- Preview action
- Reset action

### 17.3 Dialogue Editor

Dialogue controls:

- Multi-line editor
- One dialogue option per line
- Live speech bubble preview
- Random line preview button later
- Reset to default

### 17.4 Action Preview

Action preview should show:

- Pet using edited action animation
- Edited face
- Speech bubble
- `Preview action` replay button

## 18. Custom Actions

### 18.1 Future Custom Action Goals

Custom actions should allow users to create new pet behaviors later.

A custom action should include:

- Name
- Trigger
- Animation
- Face
- Dialogue
- Enabled state
- Optional icon later
- Optional sound later
- Optional probability/conditions later

### 18.2 Trigger Types

Future trigger types:

- Manual tap
- Long press
- App open
- Topic revealed
- Topic saved
- Capture saved
- Streak achieved
- Level up
- Idle for a while
- Time of day
- Random event
- Specific category explored

### 18.3 Custom Action Data Model

Suggested model:

```kotlin
data class CustomPetAction(
    val id: String,
    val name: String,
    val trigger: PetActionTrigger,
    val animationId: String,
    val face: PetFace? = null,
    val dialogueLines: List<String> = emptyList(),
    val enabled: Boolean = true
)

sealed interface PetActionTrigger {
    data object ManualTap : PetActionTrigger
    data object LongPress : PetActionTrigger
    data object AppOpen : PetActionTrigger
    data object TopicReveal : PetActionTrigger
    data object CaptureSaved : PetActionTrigger
    data object LevelUp : PetActionTrigger
    data class TimeOfDay(val hour: Int) : PetActionTrigger
    data class RandomIdle(val minimumIdleSeconds: Int) : PetActionTrigger
}
```

Do not implement all runtime triggers at once. First prepare the editor and model.

## 19. Settings Page

### 19.1 Settings Sections

The Settings navbar page should include:

1. `Saved designs`
2. `Import & export`
3. `Canvas settings`
4. `Blueprint settings`
5. `Pet library` later
6. `Danger zone`

### 19.2 Saved Designs

Initial:

- Current active design
- Export current design
- Import design
- Reset to default

Future:

- Multiple saved designs
- Rename
- Duplicate
- Delete
- Thumbnail preview
- Apply saved design
- Per-pet saved designs

### 19.3 Canvas Settings

Options:

- Grid size: 24 or 32
- Show grid by default
- Blueprint opacity
- Transparent background pattern
- Preview size

### 19.4 Pet Library

Prepare for:

- Current pet: Curie
- Future pets
- Per-pet definitions
- Per-pet animation/action capabilities

## 20. Multi-Pet Architecture

### 20.1 Problem

The current designer is built around the current pet. The app will add more pets, so the designer should become pet-definition-driven.

### 20.2 Pet Definition

Suggested model:

```kotlin
data class PetDefinition(
    val id: String,
    val displayName: String,
    val defaultDesign: PetDesign,
    val editableParts: List<EditablePetPart>,
    val animations: List<PetAnimationDefinition>,
    val actions: List<PetActionDefinition>
)
```

### 20.3 Editable Part Definition

```kotlin
data class EditablePetPart(
    val id: String,
    val displayName: String,
    val type: EditablePetPartType,
    val supportsPixelEditing: Boolean,
    val supportsColorEditing: Boolean,
    val supportsBlueprint: Boolean,
    val defaultBlueprintMode: PetBlueprintMode
)

enum class EditablePetPartType {
    BODY,
    POSE,
    DETAIL_LAYER,
    FACE,
    EYES,
    MOUTH,
    ACCESSORY,
    EFFECT,
    ANIMATION_FRAME,
    ACTION,
    DIALOGUE
}
```

### 20.4 Compatibility

Existing designs without a pet id should be treated as the current default pet.

If a future design has an unknown pet id:

- Show a readable error.
- Offer import as compatible pixel art if possible.
- Do not crash.

## 21. Data Model Evolution

### 21.1 Current Fields To Preserve

Do not remove or break:

- `palette`
- `bodyRows`
- `curledRows`
- `gridSize`
- `faces`
- `reactions`
- `details`
- `procedural`

### 21.2 Future Fields

Potential additions:

```kotlin
val petSpeciesId: String = "curie"
val animations: Map<String, PetAnimation> = emptyMap()
val customActions: List<CustomPetAction> = emptyList()
val metadata: PetDesignMetadata = PetDesignMetadata()
```

Suggested metadata:

```kotlin
data class PetDesignMetadata(
    val name: String = "My pet",
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    val editorVersion: Int = 1
)
```

### 21.3 Serialization

The existing plain-text design format should remain supported.

Add optional lines for new fields. Example:

```text
# pet=curie
# editorVersion=2
animation=idle;frame=0;duration=180
customAction=id123;name=Wave;trigger=ManualTap;animation=wave;enabled=1
```

Old imports should continue to work if these lines are absent.

## 22. Performance Requirements

### 22.1 Recomposition

Avoid recomposing the full screen on every pixel if possible.

Guidelines:

- Keep pixel grid state localized.
- Use stable data models where possible.
- Avoid rebuilding large lists in every composable call.
- Use `remember` and derived values for previews where appropriate.

### 22.2 Undo Memory

Full `PetDesign` snapshots are acceptable initially because grids are small.

If memory becomes an issue:

- Store command diffs instead of full snapshots.
- Store only changed cells per gesture.
- Cap undo history to a reasonable count, such as 50.

### 22.3 Preview Animation

Preview animations should avoid expensive work per frame.

Guidelines:

- Precompute frame data when selected animation changes.
- Do not parse serialized design text during animation frames.
- Render from working in-memory `PetDesign`.

### 22.4 Color Picker

Custom color wheel should be efficient:

- Cache generated bitmap if needed.
- Avoid recalculating every pixel on every recomposition.
- Use Compose `Canvas` carefully.

## 23. Accessibility Requirements

- Tool buttons need content descriptions.
- Color swatches need labels with slot name and hex.
- Do not rely on color alone for active state.
- Buttons should be large enough for touch.
- The selected target should be announced with text.
- Dialogs should have clear title and dismiss/apply actions.
- Text fields should have labels.

## 24. Responsive Layout

### 24.1 Phone Portrait

Use vertical layout:

1. Header
2. Navbar
3. Picker carousel/grid
4. Preview
5. Editor
6. Tools
7. Save area

### 24.2 Wide Screens / Tablets

Use split layout:

```text
┌──────────────────────────────────────────────┐
│ Navbar                                       │
├───────────────┬──────────────────────────────┤
│ Picker/sidebar │ Universal editor + preview  │
├───────────────┴──────────────────────────────┤
│ Save area                                    │
└──────────────────────────────────────────────┘
```

The save area should still be below the editor area.

## 25. Visual Design Direction

The UI should be playful but structured.

Recommended design qualities:

- Soft cards
- Clear active states
- Large preview area
- Tool tray that feels like a drawing app
- Avoid dense walls of controls
- Use section labels and short helper text
- Use thumbnails/previews instead of text-only lists
- Keep destructive actions separated

Suggested labels:

- `Choose what to edit`
- `Live preview`
- `Blueprint`
- `Tools`
- `Selected color`
- `Save pet`
- `Unsaved changes`

## 26. Implementation Phases

### Phase 1 — Universal Editor Shell

Deliver:

- Local navbar with Animations, Actions, Settings
- `PetEditorTarget` model
- Universal editor shell
- Save button below editor
- Route current body/detail/face/reaction editing into the universal editor

Acceptance:

- User can choose a target and edit it in one consistent editor area.
- Save remains obvious below the editor.

### Phase 2 — Better Drawing UX

Deliver:

- Remove draw toggle
- Active tool editing mode
- Improved tool tray
- Brush/fill/eraser/eyedropper clarity
- Canvas grid/blueprint/feedback improvements
- Undo stack

Acceptance:

- Tapping Brush starts drawing mode.
- Undo works for pixel changes.
- Canvas is clearer and less confusing.

### Phase 3 — Color Picker Upgrade

Deliver:

- Color preview chip
- Better color dialog
- Hex input
- Quick swatches
- Recent colors
- Hue/color wheel controls
- Before/after preview

Acceptance:

- User can pick colors visually and see what will be painted.

### Phase 4 — Animation Preview and Timeline

Deliver:

- Animation preview gallery
- Selected animation target
- Timeline UI
- Frame thumbnails
- Play/pause preview
- Previous-frame blueprint

Acceptance:

- User chooses an animation from previews and edits its frames in the universal editor.

### Phase 5 — Actions and Dialogue

Deliver:

- Actions page
- Reaction/action cards
- Dialogue editor
- Action preview
- Face + animation selection for actions

Acceptance:

- User edits what the pet says and does from the Actions page.

### Phase 6 — Multi-Pet Foundations

Deliver:

- Pet definition concept
- Editable part registry
- Future pet selector placeholder
- Backward compatibility for current Curie design

Acceptance:

- Adding another pet later does not require redesigning the editor structure.

### Phase 7 — Custom Actions

Deliver:

- Custom action model
- Add custom action UI
- Trigger selector
- Dialogue/action/animation integration

Acceptance:

- User can define custom pet actions when runtime trigger support is added.

## 27. Suggested File Split

As the redesign grows, split the large screen into smaller files:

```text
app/src/main/java/com/curio/app/features/petdesigner/
├── PetDesignerScreen.kt
├── PetDesignerModels.kt
├── UniversalPetEditor.kt
├── PetDesignerNavbar.kt
├── PetAnimationPicker.kt
├── PetActionPicker.kt
├── PetDesignerSettingsPage.kt
├── PetEditorCanvas.kt
├── PetEditorToolTray.kt
├── PetColorPicker.kt
├── PetAnimationTimeline.kt
├── PetActionEditor.kt
├── PetBlueprintOverlay.kt
├── PetSavedDesigns.kt
└── PetDesignerPreview.kt
```

Data/model files may later move to:

```text
app/src/main/java/com/curio/app/data/
├── PetDesign.kt
├── PetAnimation.kt
├── PetAction.kt
└── PetDefinition.kt
```

## 28. Validation Plan

Local Gradle compile/build/test/lint commands must not be run in this environment.

Allowed local checks:

- Static file inspection
- `git diff --check`
- Existing non-Gradle validation scripts, such as `python3 scripts/validate_petdesigner.py`, if still applicable

CI remains the compile gate.

## 29. Important Guardrails

- Do not remove existing pet features without user confirmation.
- Since this redesign adds new features, ask whether new behavior should be toggleable or always-on before implementation.
- If shipping as experimental, add a user-facing Settings toggle.
- Keep saved design import/export backward-compatible.
- Keep the current Curie pet working if new animation/action fields are absent.
- Before changing constructors or serialized fields, read the actual data class definitions.
- Before using Material3 APIs, check `gradle/libs.versions.toml` for the Compose BOM version.
- Do not call composable APIs inside non-composable callbacks.
- Do not name a parameter `size` inside a function containing a `Canvas {}` block.
- Do not use `sed -i` for multiline Kotlin edits.

## 30. Final Acceptance Checklist

The redesigned Pet Designer is complete when:

- [ ] There is one universal editor.
- [ ] Save button is below the editor.
- [ ] Local navbar has Animations, Actions, Settings.
- [ ] Animations page shows previews before editing.
- [ ] Editing shows real live preview.
- [ ] Every major editable target supports blueprint/reference view.
- [ ] Drawing starts by selecting a tool, not by enabling a confusing toggle.
- [ ] Undo works.
- [ ] Color picker has preview, swatches, hex, recent colors, and visual wheel/slider controls.
- [ ] Actions page supports dialogue and reaction editing.
- [ ] Settings page owns saved/import/export/reset options.
- [ ] Data model remains backward-compatible.
- [ ] Future pets can be added through definitions/registries instead of another full redesign.
- [ ] Custom actions are planned in the model and UI structure.

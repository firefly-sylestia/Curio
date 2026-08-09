package com.curio.app.features.petdesigner

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategories
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioPet
import com.curio.app.data.EyeStyle
import com.curio.app.data.MouthStyle
import com.curio.app.data.CustomPetAction
import com.curio.app.data.PetActionTrigger
import com.curio.app.data.PetDesign
import com.curio.app.data.PetFaceMoods
import com.curio.app.data.PetFacePresets
import com.curio.app.data.PetReaction
import com.curio.app.data.PetReactionEvents
import com.curio.app.data.BUILTIN_ANIMATIONS
import com.curio.app.data.PetAnimation
import com.curio.app.data.PetAnimationFrame
import com.curio.app.data.PetDefinition
import com.curio.app.data.PetRegistry
import com.curio.app.data.ReactionAnim
import com.curio.app.data.animationById
import com.curio.app.data.definition
import com.curio.app.data.petAnimationName
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.ui.draw.alpha
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.pet.BED_GRID_W
import com.curio.app.ui.pet.BED_GRID_H
import com.curio.app.ui.pet.BED_ROWS
import com.curio.app.ui.pet.CurioFlowerBed
import com.curio.app.ui.pet.CurioPetSprite
import com.curio.app.ui.pet.EYE_STYLE_PIXELS
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import java.io.File

/** One editable color in the palette — its grid key, name and hex. */
private data class PaletteSlot(val key: Char, val name: String)

private val PALETTE_SLOTS = listOf(
    PaletteSlot('b', "Body"),
    PaletteSlot('B', "Shade"),
    PaletteSlot('o', "Ink"),
    PaletteSlot('s', "Scarf"),
    PaletteSlot('S', "Scarf dark"),
    PaletteSlot('G', "Gold"),
    PaletteSlot('g', "Gold deep"),
    PaletteSlot('c', "Custom 1"),
    PaletteSlot('C', "Custom 2"),
    PaletteSlot('d', "Custom 3"),
    PaletteSlot('D', "Custom 4"),
    PaletteSlot('r', "Blush"),
    PaletteSlot('y', "Eyes")
)

/** The paint tools (v8.35): brush, fill bucket, eraser, eyedropper. */
private enum class PaintTool { BRUSH, FILL, ERASER, EYEDROPPER }

// v8.45 — the old editor tabs are replaced by the universal-editor model in
// PetDesignerModels.kt (PetDesignerPage + PetEditorTarget).

/** A curated palette of pleasant hex colors for the quick-pick swatches. */
private val QUICK_HEX = listOf(
    "FFF3DC", "FFE0C2", "FFC9A3", "FFB1A0", "FF8FA3", "FF6F61",
    "F6D5B3", "EFE0C8", "E8D8F0", "C9B8E8", "A3C9E8", "8FC9E0",
    "B3E0C9", "8FE0C9", "D8E8A3", "E0D8A3", "E8C9A3", "F0C0C0",
    "4A3426", "5C4436", "6E5448", "3E4A5C", "4A5C6E", "5C4A6E",
    "FFD97D", "E0B050", "B8860B", "C0A040", "D9C060", "F0C060"
)

/** v8.56 — the gentle looping animation pet cards play (idle, safe fallback). */
private val PET_CARD_ANIMATION: PetAnimation =
    animationById("idle") ?: BUILTIN_ANIMATIONS.first()

/** Hex parse with the same tolerance as [PetDesign]. */
private fun parseHex(text: String): String? {
    val clean = text.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.uppercase()
    return if (clean.length >= 6) clean.take(6) else null
}

/** Renders one hex as a Compose color (safe fallback). */
private fun hexColor(hex: String): Color = runCatching { Color(0xFF000000L or hex.toLong(16)) }
    .getOrDefault(Color(0xFF4A3426))

/** Hex → HSL (h in 0..360, s/l in 0..1) — for the advanced color editor. */
private fun hexToHsl(hex: String): Triple<Float, Float, Float> {
    val r = hex.substring(0, 2).toInt(16) / 255f
    val g = hex.substring(2, 4).toInt(16) / 255f
    val b = hex.substring(4, 6).toInt(16) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    if (d == 0f) return Triple(0f, 0f, l)
    val s = d / (1f - kotlin.math.abs(2f * l - 1f))
    val h = when (max) {
        r -> ((g - b) / d) % 6f
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } * 60f
    return Triple((h + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

/** HSL → hex (RRGGBB). */
private fun hslToHex(h: Float, s: Float, l: Float): String {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = (h % 360f + 360f) % 360f / 60f
    val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
    val (r1, g1, b1) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    fun toByte(v: Float): Int = ((v + m) * 255f).toInt().coerceIn(0, 255)
    return "%02X%02X%02X".format(toByte(r1), toByte(g1), toByte(b1))
}

/**
 * v8.35 — the Pet designer playground (Settings → Pet designer): a working
 * copy of the pet's look you can reshape live. Two canvases (24×24 and
 * 32×32, convertible), paint tools (brush / fill bucket / eraser /
 * eyedropper) with drag painting, palette recoloring with hex + HSL
 * sliders, preset shapes, a randomizer, a Face & reactions editor (per-mood
 * eyes/mouth/blush/sparkles + per-event reaction rules), and import/export
 * as PNG images or plain text. Saving applies the design EVERYWHERE
 * (always-on — [AppPreferences.setPetDesign]); the pet sprite reads it
 * reactively.
 */
@Composable
fun PetDesignerScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val savedText = AppPreferences.petDesignState
    // v8.56 — the two user-saved custom pet slots (reactive list of 2).
    val customPets = AppPreferences.customPetsState
    // v8.49 — the design as it was when the editor opened; used for the
    // "Unsaved changes" indicator in the footer.
    val initialDesign = remember(savedText) {
        savedText?.let { PetDesign.DEFAULT.toParsedOr(it, PetDesign.DEFAULT) } ?: PetDesign.DEFAULT
    }
    // Working copy — starts from the saved design (or default), edited live.
    var design by remember(savedText) {
        mutableStateOf(initialDesign)
    }
    // Which grid is being edited: "body" or "curled".
    var editingGrid by rememberSaveable { mutableStateOf("body") }
    // The currently selected palette paint key.
    var paintKey by rememberSaveable { mutableStateOf('b') }
    // v8.46 — the active paint tool. Picking a tool arms editing; null
    // means no tool is selected so the canvas scrolls safely. The old
    // explicit draw toggle is gone — the tool tray is the edit-mode switch.
    var activeTool by rememberSaveable { mutableStateOf<PaintTool?>(null) }
    // v8.46 — undo/redo: full design snapshots capped at 50 (Phase 2 spec §11).
    var undoStack by remember { mutableStateOf<List<PetDesign>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<PetDesign>>(emptyList()) }
    // v8.45 — universal editor: the local page (Animations/Actions/Settings)
    // + the selected edit target. One editor renders whichever target is
    // chosen; Colors is an Animations-page target, reactions are
    // Actions-page targets.
    var page by rememberSaveable { mutableStateOf(PetDesignerPage.PETS) }
    var target by rememberSaveable { mutableStateOf<PetEditorTarget?>(null) }
    // Detail layers share the same protected canvas and palette as body art.
    var detailLayer by rememberSaveable { mutableStateOf("tail") }
    // The Details tab starts with the effective current part (including
    // procedural art) so users can redraw what Curie already wears.
    var detailEditorDrafts by remember(savedText) {
        mutableStateOf<Map<String, List<String>>>(emptyMap())
    }
    // Compare edits against the part as it looked when this designer opened.
    var showBlueprint by rememberSaveable { mutableStateOf(false) }
    var detailEditorRevision by remember { mutableStateOf(0) }
    fun resetDetailEditor() {
        detailEditorDrafts = emptyMap()
        showBlueprint = false
        detailEditorRevision++
    }
    // When non-null, the color editor dialog is open for this palette key.
    var editingColorKey by rememberSaveable { mutableStateOf<Char?>(null) }
    // v8.49 — which preview picker dialog is open (body / faces / details / animations).
    var pickerCategory by remember { mutableStateOf<String?>(null) }
    // v8.52 — the studio toolbar's import menu (PNG vs paste-text).
    var importMenuOpen by remember { mutableStateOf(false) }
    // v8.52 — the Settings → Accessories dialog.
    var accessoriesOpen by remember { mutableStateOf(false) }
    // v9.3 — home editor: the flower bed design dialog.
    var bedEditorOpen by remember { mutableStateOf(false) }
    // v8.56 — which custom-pet slot the working design belongs to (null =
    // the built-in pet). Plain `remember` (NOT saveable) to match the
    // working design's own lifetime: `design` re-parses from the global
    // saved text on rotation, so a surviving slot pointer would let Save
    // clobber a custom pet with the wrong design.
    var activeCustomSlot by remember { mutableStateOf<Int?>(null) }
    // v8.56 — the full-screen animation player (Pets page gallery tap).
    var playerAnimation by remember { mutableStateOf<PetAnimation?>(null) }
    // v8.47 — recent colors for the professional color picker (persisted, capped at 12).
    var recentColors by remember { mutableStateOf(AppPreferences.getPetRecentColors(context)) }
    fun rememberColor(hex: String) {
        recentColors = (listOf(hex) + recentColors.filter { it != hex }).take(12)
        AppPreferences.setPetRecentColors(context, recentColors)
    }
    // When non-null, the import/export text dialog is open with this draft.
    var importDraft by rememberSaveable { mutableStateOf<String?>(null) }
    // A transient confirmation ("Saved!" / "Copied!") shown under the actions.
    var toast by remember { mutableStateOf<String?>(null) }
    // Preview mood so the user can see the design in different poses.
    var previewMood by rememberSaveable { mutableStateOf(CurioPet.Mood.HAPPY) }
    // v8.35 — the face editor's selected mood + the reaction editor's event.
    var faceMood by rememberSaveable { mutableStateOf(PetFaceMoods.HAPPY) }
    // v8.36 — face editor: blueprint ghost + zoom.
    var faceBlueprint by rememberSaveable { mutableStateOf(true) }
    var faceZoom by rememberSaveable { mutableStateOf(1f) }
    // v8.36 — body/detail editor zoom.
    var gridZoom by rememberSaveable { mutableStateOf(1f) }
    var reactEvent by rememberSaveable { mutableStateOf(PetReactionEvents.TOUCH) }
    // Keep raw editor text separate from the normalized persisted lines so
    // typing does not trim the field or jump the cursor on every keystroke.
    var reactionLineDraft by remember(savedText) {
        mutableStateOf(
            design.reactionFor(PetReactionEvents.TOUCH).lines.joinToString("\n")
        )
    }
    // v8.53 — custom action editor drafts (Phase 7): the action's name and
    // dialogue lines stay in raw text while typing; they are committed to
    // the design on every change like the reaction lines above.
    var customActionLineDraft by remember(savedText) { mutableStateOf("") }
    var customActionNameDraft by remember(savedText) { mutableStateOf("") }
    // v8.35 — preview the hide-and-peek crouch.
    var previewPeek by rememberSaveable { mutableStateOf(false) }
    // v8.35 — which grid a picked PNG should land on (1 = body, 2 = curled).
    var importPngTarget by remember { mutableStateOf<Int?>(null) }
    // PNG import review: the picked image opens a guided color step. Users
    // can sample colors from the image, name them by purpose, then apply.
    var importReview by remember { mutableStateOf<ImportReview?>(null) }

    // v8.35/v8.37 — PNG import: pick an image, resample to the canvas, then
    // open the review step instead of snapping immediately.
    val pngPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val target = importPngTarget ?: return@rememberLauncherForActivityResult
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return@rememberLauncherForActivityResult
        val grid = design.gridSize
        val scaled = runCatching { Bitmap.createScaledBitmap(bitmap, grid, grid, true) }
            .getOrNull() ?: return@rememberLauncherForActivityResult
        val pixels = IntArray(grid * grid)
        scaled.getPixels(pixels, 0, grid, 0, 0, grid, grid)
        importPngTarget = null
        importReview = buildImportReview(pixels, grid, design, target)
    }

    // v8.46 — snapshots the current design before the next mutation so one
    // Undo restores exactly where the user was (gesture grouping: pass
    // snapshot=false on drag-continuation cells, snapshot=true on tap/start).
    fun pushUndo() {
        undoStack = (undoStack + design).takeLast(50)
        redoStack = emptyList()
    }

    // v8.53 — refreshes the custom-action editor drafts from [design] (used
    // after undo/redo/import so the fields never show stale text).
    fun refreshCustomActionDrafts() {
        val actionTarget = target as? PetEditorTarget.CustomAction
        val action = actionTarget?.let { design.customActionFor(it.actionId) }
        customActionNameDraft = action?.name ?: customActionNameDraft
        customActionLineDraft = action?.dialogueLines?.joinToString("\n") ?: customActionLineDraft
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack = redoStack + design
        design = undoStack.last()
        undoStack = undoStack.dropLast(1)
        resetDetailEditor()
        reactionLineDraft = design.reactionFor(reactEvent).lines.joinToString("\n")
        refreshCustomActionDrafts()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack = undoStack + design
        design = redoStack.last()
        redoStack = redoStack.dropLast(1)
        resetDetailEditor()
        reactionLineDraft = design.reactionFor(reactEvent).lines.joinToString("\n")
        refreshCustomActionDrafts()
    }

    // v8.35 — applies the active tool at a grid cell. Snapshot is taken once
    // per gesture (on the tap/start), never per dragged cell.
    fun applyTool(row: Int, col: Int, targetGrid: String = editingGrid, snapshot: Boolean = true) {
        val tool = activeTool ?: return
        if (tool != PaintTool.EYEDROPPER && snapshot) pushUndo()
        val grid = targetGrid
        when (tool) {
            PaintTool.BRUSH -> {
                design = if (grid.startsWith("detail:")) {
                    design.withDetailPixel(grid.removePrefix("detail:"), row, col, paintKey)
                } else design.withPixel(grid, row, col, paintKey)
            }
            PaintTool.FILL -> {
                design = if (grid.startsWith("detail:")) {
                    design.withDetailFloodFill(grid.removePrefix("detail:"), row, col, paintKey)
                } else design.withFloodFill(grid, row, col, paintKey)
            }
            PaintTool.ERASER -> {
                design = if (grid.startsWith("detail:")) {
                    design.withDetailPixel(grid.removePrefix("detail:"), row, col, '.')
                } else design.withPixel(grid, row, col, '.')
            }
            PaintTool.EYEDROPPER -> {
                val rows = when {
                    grid.startsWith("detail:") -> design.detailFor(grid.removePrefix("detail:"))
                    grid == "curled" -> design.curledRows
                    else -> design.bodyRows
                }
                val ch = rows.getOrNull(row)?.getOrNull(col) ?: '.'
                if (ch != '.') {
                    paintKey = ch
                    activeTool = PaintTool.BRUSH
                } else {
                    activeTool = PaintTool.ERASER
                }
            }
        }
    }

    // v8.45 — picking a target in the universal editor also syncs the legacy
    // editor states (grid, detail layer, face mood, reaction event) so the
    // existing editor bodies keep working unchanged.
    fun selectTarget(newTarget: PetEditorTarget) {
        target = newTarget
        when (newTarget) {
            is PetEditorTarget.DetailLayer -> detailLayer = newTarget.key
            is PetEditorTarget.Face -> faceMood = newTarget.mood
            is PetEditorTarget.Reaction -> {
                reactEvent = newTarget.event
                reactionLineDraft = design.reactionFor(newTarget.event).lines.joinToString("\n")
            }
            // v8.53 — custom actions: the NEW sentinel creates a fresh action
            // (named uniquely) and selects it; an existing id just syncs the
            // editor drafts.
            is PetEditorTarget.CustomAction -> {
                if (newTarget.actionId == PetEditorTarget.NEW_CUSTOM_ACTION_ID) {
                    pushUndo()
                    var n = 1
                    while (design.customActionFor("custom-$n") != null) n++
                    val id = "custom-$n"
                    design = design.withCustomAction(
                        CustomPetAction(
                            id = id,
                            name = "New action",
                            trigger = PetActionTrigger(PetActionTrigger.TAP),
                            animationId = "happy"
                        )
                    )
                    target = PetEditorTarget.CustomAction(id)
                    customActionNameDraft = "New action"
                    customActionLineDraft = ""
                } else {
                    val action = design.customActionFor(newTarget.actionId)
                    customActionNameDraft = action?.name ?: ""
                    customActionLineDraft = action?.dialogueLines?.joinToString("\n") ?: ""
                }
            }
            PetEditorTarget.CurledPose -> editingGrid = "curled"
            PetEditorTarget.Body -> editingGrid = "body"
            PetEditorTarget.Colors -> Unit
            // Animation is a data class (carries animationId), so it must be
            // matched with `is` — a bare `PetEditorTarget.Animation` reference
            // would be an unresolved companion-object expression (CI fix).
            is PetEditorTarget.Animation -> Unit
        }
    }

    // v8.52 — Pets page: switch the working design to another species. An
    // untouched design is replaced by the pet's default art so the new look
    // shows immediately; custom designs keep their pixels but get re-tagged.
    // v8.56 — selecting the built-in pet also leaves any custom slot.
    fun selectPet(pet: PetDefinition) {
        if (design.definition.id == pet.id && activeCustomSlot == null) {
            toast = "\u201c${pet.displayName}\u201d is already your pet"
            return
        }
        pushUndo()
        resetDetailEditor()
        activeCustomSlot = null
        design = if (design.isCustom) design.copy(petSpeciesId = pet.id)
        else pet.defaultDesign.copy(petSpeciesId = pet.id)
        toast = "\u201c${pet.displayName}\u201d is now your pet"
    }

    // v8.56 — load a saved custom-pet slot into the working design.
    fun selectCustomPet(slot: Int) {
        val text = customPets.getOrNull(slot) ?: return
        if (activeCustomSlot == slot) {
            toast = "You're already editing Custom ${slot + 1}"
            return
        }
        val parsed = PetDesign.DEFAULT.toParsedOr(text, design)
        pushUndo()
        resetDetailEditor()
        design = parsed
        activeCustomSlot = slot
        reactionLineDraft = parsed.reactionFor(reactEvent).lines.joinToString("\n")
        toast = "\u201cCustom ${slot + 1}\u201d is now your pet"
    }

    // v8.56 — copy the working design into the first empty custom slot.
    // No pushUndo: the working design itself doesn't change (the slot write
    // is its own persisted copy, not an editable edit).
    fun saveAsNewPet() {
        val slot = customPets.indexOfFirst { it == null }
        if (slot == -1) {
            toast = "Both custom pet slots are full — delete one to make room"
            return
        }
        AppPreferences.setCustomPet(context, slot, design.toText())
        activeCustomSlot = slot
        toast = "Saved as Custom ${slot + 1} — it's now your pet"
    }

    // v8.56 — delete one custom-pet slot (if it was active, back to Curie).
    fun deleteCustomPet(slot: Int) {
        AppPreferences.clearCustomPet(context, slot)
        if (activeCustomSlot == slot) activeCustomSlot = null
        toast = "Custom ${slot + 1} removed"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        val wide = windowWidthSizeClass().isWide
        Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                top = SettingsHeroTotalHeight + 8.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Sticky studio toolbar: the ONE place for save / import /
            //    export / undo / redo / reset (v8.52 — the old pinned
            //    footer SaveArea is gone, so no duplicate buttons). ──────
            stickyHeader {
                Column {
                    EditorToolbar(
                        design = design,
                        dirty = design != initialDesign,
                        toast = toast,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        onUndo = { undo() },
                        onRedo = { redo() },
                        onReset = {
                            pushUndo()
                            resetDetailEditor()
                            activeCustomSlot = null
                            design = PetDesign.DEFAULT
                            reactionLineDraft = PetDesign.DEFAULT.reactionFor(reactEvent).lines.joinToString("\n")
                        },
                        onSave = {
                            if (design.isCustom) {
                                AppPreferences.setPetDesign(context, design.toText())
                                // v8.56 — saving while editing a custom pet
                                // also refreshes its slot so the Pets page
                                // card always shows the latest look.
                                val slot = activeCustomSlot
                                if (slot != null) {
                                    AppPreferences.setCustomPet(context, slot, design.toText())
                                    toast = "Saved — Custom ${slot + 1} updated"
                                } else {
                                    toast = "Saved — Curie wears it everywhere"
                                }
                            } else {
                                AppPreferences.clearPetDesign(context)
                                // Using the default look also detaches from
                                // any custom pet slot being edited.
                                activeCustomSlot = null
                                toast = "Default look restored"
                            }
                        },
                        onImport = { importMenuOpen = true },
                        onExport = {
                            val exportMood = previewMood.name
                            val exportGrid = if (previewMood == CurioPet.Mood.SLEEPY) "curled" else "body"
                            val uri = exportPngUri(context, design, exportGrid, exportMood, CurioPet.currentStage())
                            if (uri != null) sharePng(context, uri) else toast = "Couldn't render PNG"
                        }
                    )
                }
            }

            // ── Editor page: picker trigger / Editing header (v8.56) ──
            //    The editor is the center of the screen — one dialog is the
            //    only chooser, and after that ONLY the chosen editor renders.
            item {
                if (page == PetDesignerPage.EDITOR) {
                    // Local val so the nullable target smart-casts cleanly.
                    val currentTarget = target
                    if (currentTarget == null) {
                        EditorPickPrompt(onOpenPicker = { pickerCategory = "body" })
                    } else {
                        EditorTargetHeader(
                            target = currentTarget,
                            onChange = { pickerCategory = "body" }
                        )
                    }
                }
            }

            // ── My pets (Pets page, v8.56) — built-in + custom slots ──
            item {
                if (page == PetDesignerPage.PETS) SectionCard(
                    "My pets",
                    "Your companion, plus up to two custom pets you saved"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PetRegistry.all.forEach { pet ->
                            PetLibraryCard(
                                pet = pet,
                                // The card animates the pet's real saved look.
                                design = initialDesign,
                                current = activeCustomSlot == null,
                                onClick = { selectPet(pet) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        CustomPetCard(
                            slot = 0,
                            designText = customPets.getOrNull(0),
                            active = activeCustomSlot == 0,
                            onClick = {
                                if (customPets.getOrNull(0) == null) saveAsNewPet()
                                else selectCustomPet(0)
                            },
                            onDelete = { deleteCustomPet(0) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomPetCard(
                            slot = 1,
                            designText = customPets.getOrNull(1),
                            active = activeCustomSlot == 1,
                            onClick = {
                                if (customPets.getOrNull(1) == null) saveAsNewPet()
                                else selectCustomPet(1)
                            },
                            onDelete = { deleteCustomPet(1) },
                            modifier = Modifier.weight(1f)
                        )
                        // Placeholder for future pets — the section lists the
                        // registry, so a new entry appears here automatically.
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            onClick = { toast = "More pets are on the way!" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Pets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    size = 28.dp,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                                )
                                Text(
                                    "More pets\ncoming soon",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                        }
                    }
                    if (customPets.any { it == null }) {
                        Spacer(Modifier.height(10.dp))
                        SmallAction("＋ Save as new pet", enabled = true) { saveAsNewPet() }
                    }
                }
            }

            // ── Live preview (Pets page) ─────────────────────────────
            item {
                if (page == PetDesignerPage.PETS) SectionCard("Live preview", if (design.isCustom) "Your custom look" else "The default look — make it yours!") {
                    CurioPetSprite(
                        stage = CurioPet.currentStage(),
                        mood = previewMood,
                        spriteSize = 110.dp,
                        design = design,
                        peeking = previewPeek
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurioPet.Mood.entries.forEach { mood ->
                            MoodChip(
                                label = mood.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = previewMood == mood && !previewPeek,
                                onClick = { previewMood = mood; previewPeek = false }
                            )
                        }
                        MoodChip(
                            label = "Peeking",
                            selected = previewPeek,
                            onClick = { previewPeek = !previewPeek }
                        )
                    }
                }
            }

            // ── Animation gallery (Pets page, v8.56) — tapping a card
            //    opens the full-screen player (fixes the old dead-tap bug:
            //    the target used to be set on the wrong page, so tapping
            //    Animations did nothing). ───────────────────────────────
            item {
                if (page == PetDesignerPage.PETS) SectionCard(
                    "Animations",
                    "Tap one to watch it full screen — then edit its frames"
                ) {
                    BUILTIN_ANIMATIONS.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { anim ->
                                AnimationGalleryCard(
                                    animation = anim,
                                    design = design,
                                    onClick = { playerAnimation = anim },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // ── Animation timeline editor (Editor target, v8.48) ─────
            item {
                val animTarget = target as? PetEditorTarget.Animation
                if (page == PetDesignerPage.EDITOR && animTarget != null) AnimationTimelineEditor(
                    animationId = animTarget.animationId,
                    design = design,
                    onDesignChange = { design = it },
                    onPushUndo = { pushUndo() },
                    onEditColor = { editingColorKey = it }
                )
            }

            // ── Body / curled pose pixel editor (Editor targets) ──────
            item {
                if (page == PetDesignerPage.EDITOR && (target == PetEditorTarget.Body || target == PetEditorTarget.CurledPose)) SectionCard(
                    "Pixel grid",
                    "Paint with a brush stroke, fill, erase, or pick colors from the canvas"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Canvas",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                "Convert between sizes — pixels keep their palette",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            GridTab("24×24", design.gridSize == 24) {
                                pushUndo()
                                resetDetailEditor()
                                design = design.withSize(24)
                            }
                            GridTab("32×32", design.gridSize == 32) {
                                pushUndo()
                                resetDetailEditor()
                                design = design.withSize(32)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        GridTab("Body", editingGrid == "body") { editingGrid = "body" }
                        GridTab("Asleep", editingGrid == "curled") { editingGrid = "curled" }
                    }
                    Spacer(Modifier.height(12.dp))
                    CanvasStatus(activeTool = activeTool)
                    Spacer(Modifier.height(10.dp))
                    QuickPaletteRow(
                        selectedKey = paintKey,
                        design = design,
                        onSelect = {
                            paintKey = it
                            activeTool = PaintTool.BRUSH
                        },
                        onEdit = { editingColorKey = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    PixelGrid(
                        design = design,
                        grid = editingGrid,
                        tool = activeTool,
                        zoom = gridZoom,
                        onTool = { row, col, continuous ->
                            // Fill + eyedropper act once per gesture; brush
                            // and eraser paint continuously while dragging.
                            // One undo snapshot per gesture, not per cell.
                            val mutating = activeTool == PaintTool.BRUSH ||
                                activeTool == PaintTool.FILL || activeTool == PaintTool.ERASER
                            if (mutating && !continuous) pushUndo()
                            if (activeTool == PaintTool.FILL || activeTool == PaintTool.EYEDROPPER) {
                                if (!continuous) applyTool(row, col, snapshot = false)
                            } else {
                                applyTool(row, col, snapshot = false)
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    ToolTray(
                        activeTool = activeTool,
                        onSelect = { activeTool = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    SliderRow(label = "Zoom", value = gridZoom, max = 3f) { gridZoom = it }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("Copy body → asleep", enabled = editingGrid == "body") {
                            pushUndo()
                            resetDetailEditor()
                            design = design.copy(curledRows = PetDesign.bodyAsCurled(design.bodyRows))
                        }
                        SmallAction("Clear grid", enabled = true) {
                            pushUndo()
                            resetDetailEditor()
                            val blank = List(design.gridSize) { ".".repeat(design.gridSize) }
                            design = design.withGrid(editingGrid, blank)
                        }
                    }
                }
            }

            // ── Drawable details editor (Editor target) ──────────────
            item {
                if (page == PetDesignerPage.EDITOR && target is PetEditorTarget.DetailLayer) SectionCard(
                    "Draw every detail",
                    "Paint Curie's tail, accessories, effects, or antenna on separate layers"
                ) {
                    Text(
                        "Detail layer",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PetDesign.DETAIL_KEYS.forEach { layer ->
                            ChoiceChip(
                                label = layer.replaceFirstChar { it.uppercase() },
                                selected = detailLayer == layer,
                                onClick = { detailLayer = layer }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Draw mode protects scrolling. The grid starts with the current part in its real placement, so you can redraw it instead of starting from blank pixels.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    CurioPetSprite(
                        stage = CurioPet.currentStage(),
                        mood = previewMood,
                        spriteSize = 150.dp,
                        design = design,
                        contentDescription = "Live placement preview for the $detailLayer layer"
                    )
                    Text(
                        "Live placement — this shows where the selected ${detailLayer.replaceFirstChar { it.uppercase() }} appears on Curie. Edit the matching pixels below; blank cells stay transparent.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    ToggleRow(
                        label = "Show before-edit blueprint",
                        checked = showBlueprint,
                        onCheckedChange = { showBlueprint = it }
                    )
                    Text(
                        "The blueprint marks the original current pixels underneath your redraw.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    CanvasStatus(activeTool = activeTool)
                    Spacer(Modifier.height(10.dp))
                    QuickPaletteRow(
                        selectedKey = paintKey,
                        design = design,
                        onSelect = {
                            paintKey = it
                            activeTool = PaintTool.BRUSH
                        },
                        onEdit = { editingColorKey = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    val blueprintRows = remember(detailLayer, design.gridSize, savedText, detailEditorRevision) {
                        effectiveDetailRows(design, detailLayer)
                    }
                    val editorRows = detailEditorDrafts[detailLayer] ?: blueprintRows
                    PixelGrid(
                        design = design,
                        grid = "detail:$detailLayer",
                        rowsOverride = editorRows,
                        blueprintRows = blueprintRows,
                        showBlueprint = showBlueprint,
                        zoom = gridZoom,
                        tool = activeTool,
                        onTool = { row, col, continuous ->
                            val mutating = activeTool == PaintTool.BRUSH ||
                                activeTool == PaintTool.FILL || activeTool == PaintTool.ERASER
                            if (mutating && !continuous) pushUndo()
                            if (activeTool == PaintTool.FILL || activeTool == PaintTool.EYEDROPPER) {
                                if (!continuous) {
                                    val rows = detailEditorDrafts[detailLayer] ?: blueprintRows
                                    detailEditorDrafts = detailEditorDrafts + (detailLayer to rows)
                                    design = design
                                        .withDetailGrid(detailLayer, rows)
                                        .withProceduralEnabled(detailLayer, false)
                                    applyTool(row, col, "detail:$detailLayer", snapshot = false)
                                    detailEditorDrafts = detailEditorDrafts + (detailLayer to design.detailFor(detailLayer))
                                }
                            } else {
                                val rows = detailEditorDrafts[detailLayer] ?: blueprintRows
                                detailEditorDrafts = detailEditorDrafts + (detailLayer to rows)
                                design = design
                                    .withDetailGrid(detailLayer, rows)
                                    .withProceduralEnabled(detailLayer, false)
                                applyTool(row, col, "detail:$detailLayer", snapshot = false)
                                detailEditorDrafts = detailEditorDrafts + (detailLayer to design.detailFor(detailLayer))
                            }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    ToolTray(
                        activeTool = activeTool,
                        onSelect = { activeTool = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    // v8.36 — nudge the whole part around the body so the
                    // user can reposition the tail, belly, accessories, etc.
                    Text(
                        "Move ${detailLayer} layer",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        "Nudge the whole part one pixel at a time to sit it exactly where you want on the body.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val nudge: (Int, Int) -> Unit = { dr, dc ->
                            pushUndo()
                            val base = detailEditorDrafts[detailLayer] ?: blueprintRows
                            val nudged = nudgeDetailRows(base, dr, dc)
                            detailEditorDrafts = detailEditorDrafts + (detailLayer to nudged)
                            design = design
                                .withDetailGrid(detailLayer, nudged)
                                .withProceduralEnabled(detailLayer, false)
                        }
                        SmallAction("←") { nudge(0, -1) }
                        SmallAction("→") { nudge(0, 1) }
                        SmallAction("↑") { nudge(-1, 0) }
                        SmallAction("↓") { nudge(1, 0) }
                    }
                    Spacer(Modifier.height(8.dp))
                    SliderRow(label = "Zoom", value = gridZoom, max = 3f) { gridZoom = it }
                    Spacer(Modifier.height(10.dp))
                    // v8.52 — the per-element disable toggles moved to Settings →
                    // Accessories (one place to change/disable every part).
                    SmallAction("Clear ${detailLayer} layer", enabled = true) {
                        pushUndo()
                        val blank = List(design.gridSize) { ".".repeat(design.gridSize) }
                        detailEditorDrafts = detailEditorDrafts + (detailLayer to blank)
                        design = design
                            .withDetailGrid(detailLayer, blank)
                            .withProceduralEnabled(detailLayer, false)
                    }
                }
            }

            // ── Palette editor (Editor target) ───────────────────────
            item {
                if (page == PetDesignerPage.EDITOR && target == PetEditorTarget.Colors) SectionCard(
                    "Colors",
                    "Pick the paint color, or tap a swatch's pencil for the advanced editor (hex + HSL sliders)"
                ) {
                    PALETTE_SLOTS.forEach { slot ->
                        PaletteRow(
                            slot = slot,
                            hex = design.colorOf(slot.key),
                            selected = paintKey == slot.key && activeTool != PaintTool.ERASER,
                            onSelect = {
                                paintKey = slot.key
                                activeTool = PaintTool.BRUSH
                            },
                            onEdit = { editingColorKey = slot.key }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // ── Face editor (Editor target) ───────────────────────────
            item {
                if (page == PetDesignerPage.EDITOR && target is PetEditorTarget.Face) SectionCard(
                    "Face per mood",
                    "Customize Curie's face for this mood"
                ) {
                    Text(
                        "Face per mood",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PetFaceMoods.ALL.forEach { mood ->
                            ChoiceChip(
                                label = PetFaceMoods.label(mood),
                                selected = faceMood == mood,
                                onClick = {
                                    faceMood = mood
                                    target = PetEditorTarget.Face(mood)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val face = design.faceFor(faceMood)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CurioPetSprite(
                            stage = CurioPet.currentStage(),
                            mood = moodFromName(faceMood),
                            spriteSize = 64.dp,
                            design = design
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${PetFaceMoods.label(faceMood)} face",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Eyes: ${face.eyes.name} · Mouth: ${face.mouth.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    // v8.36 — quick style controls: pick the eyes / mouth the
                    // face wears without touching a pixel.
                    Text(
                        "Eyes",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EyeStyle.entries.forEach { style ->
                            ChoiceChip(
                                label = style.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = face.eyes == style,
                                onClick = {
                                    pushUndo()
                                    design = design.withFace(faceMood, face.copy(eyes = style))
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Mouth",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MouthStyle.entries.forEach { style ->
                            ChoiceChip(
                                label = style.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = face.mouth == style,
                                onClick = {
                                    pushUndo()
                                    design = design.withFace(faceMood, face.copy(mouth = style))
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    ToggleRow("Blush cheeks", face.blush) {
                        pushUndo()
                        design = design.withFace(faceMood, face.copy(blush = it))
                    }
                    ToggleRow("Sparkle eyes", face.sparkles) {
                        pushUndo()
                        design = design.withFace(faceMood, face.copy(sparkles = it))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Paint this face",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        "The blueprint ghost below marks where the default ${PetFaceMoods.label(faceMood).lowercase()} face sits — paint over it and your pixels replace it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    ToggleRow("Show face blueprint", faceBlueprint) { faceBlueprint = it }
                    Spacer(Modifier.height(8.dp))
                    ToolTray(
                        activeTool = activeTool,
                        onSelect = { activeTool = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    FaceGridEditor(
                        design = design,
                        face = face,
                        tool = activeTool,
                        blueprintRows = faceBlueprintRows(design, faceMood),
                        showBlueprint = faceBlueprint,
                        zoom = faceZoom,
                        onPaint = { row, col, continuous ->
                            if (!(activeTool == PaintTool.FILL && continuous)) {
                                if (activeTool != PaintTool.EYEDROPPER && !continuous) pushUndo()
                                val nextFace = when (activeTool) {
                                    PaintTool.BRUSH -> face.withPixel(row, col, paintKey, design.gridSize)
                                    PaintTool.ERASER -> face.withPixel(row, col, '.', design.gridSize)
                                    PaintTool.FILL -> face.withFloodFill(row, col, paintKey, design.gridSize)
                                    PaintTool.EYEDROPPER -> {
                                        val picked = face.gridRows.getOrNull(row)?.getOrNull(col) ?: '.'
                                    if (picked != '.') {
                                        paintKey = picked
                                        activeTool = PaintTool.BRUSH
                                    }
                                    face
                                    }
                                    null -> face
                                }
                                design = design.withFace(faceMood, nextFace)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    SliderRow(label = "Zoom", value = faceZoom, max = 3f) { faceZoom = it }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Procedural fallback: ${face.eyes.name} eyes · ${face.mouth.name} mouth · blush ${if (face.blush) "on" else "off"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )


                }
            }

            // ── Reaction editor (Editor target) ──────────────────────
            item {
                if (page == PetDesignerPage.EDITOR && target is PetEditorTarget.Reaction) SectionCard(
                    "Reaction",
                    "What Curie does for this moment — and the face it wears while reacting"
                ) {
                    Text(
                        "Reactions",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Choose what Curie does for each moment — and the face it wears while reacting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PetReactionEvents.ALL.forEach { event ->
                            ChoiceChip(
                                label = PetReactionEvents.label(event),
                                selected = reactEvent == event,
                                onClick = {
                                    reactEvent = event
                                    target = PetEditorTarget.Reaction(event)
                                    reactionLineDraft = design.reactionFor(event).lines.joinToString("\n")
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val reaction = design.reactionFor(reactEvent)
                    // v8.50 — live action preview: the pet plays the chosen
                    // animation wearing the reaction face, over a speech bubble.
                    ActionPreview(
                        event = reactEvent,
                        design = design,
                        reaction = reaction,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Reaction lines (optional)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Write one line per row. When Custom reaction lines is enabled in Settings, Curie picks one of these for this event.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            if (reaction.lines.isEmpty()) {
                                Text(
                                    "Boop!\nYou found me!\nWrite up to 8 custom lines…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                            BasicTextField(
                                value = reactionLineDraft,
                                onValueChange = { text ->
                                    val limitedText = PetReaction.limitDraft(text)
                                    reactionLineDraft = limitedText
                                    design = design.withReaction(
                                        reactEvent,
                                        design.reactionFor(reactEvent).copy(
                                            lines = PetReaction.normalizeLines(limitedText)
                                        )
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${reaction.lines.size}/8 lines · 120 characters per line",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    ToggleRow(
                        "React to “${PetReactionEvents.label(reactEvent)}”",
                        reaction.enabled
                    ) {
                        pushUndo()
                        design = design.withReaction(reactEvent, reaction.copy(enabled = it))
                    }
                    LabeledChips(
                        label = "Animation",
                        options = ReactionAnim.entries.map { it.name },
                        selected = reaction.anim.name,
                        onSelect = {
                            pushUndo()
                            design = design.withReaction(reactEvent, reaction.copy(anim = ReactionAnim.valueOf(it)))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction(
                            "Reset action",
                            enabled = reaction != (PetDesign.DEFAULT_REACTIONS[reactEvent] ?: PetReaction())
                        ) {
                            pushUndo()
                            val def = PetDesign.DEFAULT_REACTIONS[reactEvent] ?: PetReaction()
                            design = design.withReaction(reactEvent, def)
                            reactionLineDraft = def.lines.joinToString("\n")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Draw the reaction face yourself. It overrides the procedural face while this event plays.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    ToolTray(
                        activeTool = activeTool,
                        onSelect = { activeTool = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    FaceGridEditor(
                        design = design,
                        face = reaction.face,
                        tool = activeTool,
                        onPaint = { row, col, continuous ->
                            if (!(activeTool == PaintTool.FILL && continuous)) {
                                if (activeTool != PaintTool.EYEDROPPER && !continuous) pushUndo()
                                val nextFace = when (activeTool) {
                                    PaintTool.BRUSH -> reaction.face.withPixel(row, col, paintKey, design.gridSize)
                                    PaintTool.ERASER -> reaction.face.withPixel(row, col, '.', design.gridSize)
                                    PaintTool.FILL -> reaction.face.withFloodFill(row, col, paintKey, design.gridSize)
                                    PaintTool.EYEDROPPER -> {
                                    val picked = reaction.face.gridRows.getOrNull(row)?.getOrNull(col) ?: '.'
                                    if (picked != '.') {
                                        paintKey = picked
                                        activeTool = PaintTool.BRUSH
                                    }
                                    reaction.face
                                }
                                    null -> reaction.face
                                }
                                design = design.withReaction(reactEvent, reaction.copy(face = nextFace))
                            }
                        }
                    )
                }
            }

            // ── Custom action editor (Editor target, v8.53 — Phase 7) ──
            item {
                val actionTarget = target as? PetEditorTarget.CustomAction
                if (page == PetDesignerPage.EDITOR && actionTarget != null) {
                    val action = design.customActionFor(actionTarget.actionId)
                    // v8.53 — if the action no longer exists (deleted via
                    // undo/import), drop back to the Actions landing. Done in
                    // an effect, never by mutating state during composition.
                    LaunchedEffect(actionTarget.actionId, design.customActions) {
                        if (design.customActionFor(actionTarget.actionId) == null) target = null
                    }
                    if (action != null) {
                        CustomActionEditor(
                            action = action,
                            design = design,
                            lineDraft = customActionLineDraft,
                            nameDraft = customActionNameDraft,
                            onNameChange = { text ->
                                val limited = text.take(28)
                                customActionNameDraft = limited
                                design = design.withCustomAction(action.copy(name = limited))
                            },
                            onLineDraftChange = { text ->
                                val limited = PetReaction.limitDraft(text)
                                customActionLineDraft = limited
                                design = design.withCustomAction(
                                    action.copy(dialogueLines = PetReaction.normalizeLines(limited))
                                )
                            },
                            onUpdate = { updated ->
                                pushUndo()
                                design = design.withCustomAction(updated)
                            },
                            onDuplicate = {
                                pushUndo()
                                var n = 1
                                while (design.customActionFor("custom-$n") != null) n++
                                val dup = action.copy(
                                    id = "custom-$n",
                                    name = action.name.ifEmpty { "New action" } + " copy"
                                )
                                design = design.withCustomAction(dup)
                                target = PetEditorTarget.CustomAction(dup.id)
                                customActionNameDraft = dup.name
                                customActionLineDraft = dup.dialogueLines.joinToString("\n")
                                toast = "Action duplicated"
                            },
                            onDelete = {
                                pushUndo()
                                design = design.removeCustomAction(action.id)
                                target = null
                                toast = "Action deleted"
                            }
                        )
                    }
                }
            }

            // ── Accessories & look (Settings page, v8.52) ────────────
            item {
                if (page == PetDesignerPage.SETTINGS) SectionCard(
                    "Accessories & look",
                    "One dialog to change, disable or redraw every accessory"
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        onClick = { accessoriesOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurioIcon(
                                name = CurioIcons.Brush,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                size = 20.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Change accessories",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.weight(1f))
                            CurioIcon(
                                name = CurioIcons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                size = 20.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Disable generated parts (tail, belly, effects, antenna) or draw your own on the detail layers. Every accessory has a live preview in the dialog.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Edit body parts (Settings, v8.36) — jump straight into the
            //    detail editor for any part, belly included. ──────────────
            item {
                if (page == PetDesignerPage.SETTINGS) SectionCard(
                    "Edit body parts",
                    "Redraw or reposition the tail, belly, accessories, effects and antenna"
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PetDesign.DETAIL_KEYS.forEach { layer ->
                            ChoiceChip(
                                label = layer.replaceFirstChar { it.uppercase() },
                                selected = false,
                                onClick = {
                                    selectTarget(PetEditorTarget.DetailLayer(layer))
                                    page = PetDesignerPage.EDITOR
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pick a part to open its editor — redraw it over the blueprint ghost, nudge it around the body, or erase it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Edit home (Settings, v9.3) — open the flower bed editor ──
            item {
                if (page == PetDesignerPage.SETTINGS) SectionCard(
                    "Home",
                    "Redesign the flower bed — change the blanket, pillow, or wood"
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        onClick = { bedEditorOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurioIcon(
                                name = CurioIcons.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                size = 20.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Edit flower bed",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.weight(1f))
                            CurioIcon(
                                name = CurioIcons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                size = 20.dp
                            )
                        }
                    }
                    if (AppPreferences.bedDesignRowsState != null) {
                        Spacer(Modifier.height(8.dp))
                        SmallAction("Reset to default bed") {
                            AppPreferences.clearBedDesignRows(context)
                            toast = "Bed reset to default"
                        }
                    }
                }
            }

            // ── One-tap personality presets (Settings page, v8.52) ───
            item {
                if (page == PetDesignerPage.SETTINGS) SectionCard(
                    "Personality presets",
                    "Set every mood face and every reaction with one tap"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PetFacePresets.ALL.forEach { preset ->
                            PresetCard(
                                name = preset.name,
                                tagline = preset.tagline,
                                preview = preset.applyTo(design),
                                onClick = {
                                    val nextDesign = preset.applyTo(design)
                                    resetDetailEditor()
                                    design = nextDesign
                                    reactionLineDraft = nextDesign.reactionFor(reactEvent).lines.joinToString("\n")
                                    toast = "\u201c${preset.name}\u201d applied — every face & reaction set"
                                }
                            )
                        }
                    }
                }
            }

            // ── Shapes & randomize (Settings page) ───────────────────
            item {
                if (page == PetDesignerPage.SETTINGS) SectionCard(
                    "Shapes & inspiration",
                    "Jump-start the body grid with a preset, or roll a fresh palette"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("Default", enabled = true) {
                            pushUndo()
                            resetDetailEditor()
                            design = design.withGrid("body", upscalePreset(PetDesign.DEFAULT_BODY_16, design.gridSize))
                        }
                        SmallAction("Robot", enabled = true) {
                            pushUndo()
                            resetDetailEditor()
                            design = design.copy(
                                bodyRows = upscalePreset(ROBOT_BODY, design.gridSize),
                                curledRows = upscalePreset(ROBOT_CURLED, design.gridSize)
                            )
                        }
                        SmallAction("Ghost", enabled = true) {
                            pushUndo()
                            resetDetailEditor()
                            design = design.copy(
                                bodyRows = upscalePreset(GHOST_BODY, design.gridSize),
                                curledRows = upscalePreset(GHOST_CURLED, design.gridSize)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("Random palette", enabled = true) {
                            pushUndo()
                            resetDetailEditor()
                            design = design.randomize()
                        }
                        SmallAction("Reset all", enabled = design.isCustom) {
                            resetDetailEditor()
                            activeCustomSlot = null
                            design = PetDesign.DEFAULT
                            reactionLineDraft = PetDesign.DEFAULT.reactionFor(reactEvent).lines.joinToString("\n")
                        }
                    }
                }
            }
        }
        // v8.56 — the studio's bottom navigation bar (Pets / Editor /
        // Settings) — a real app-style bar, always visible.
        PetStudioBottomNav(
            page = page,
            onSelect = { newPage ->
                page = newPage
                target = null
            }
        )
        }

        SettingsHeroHeader(
            title = "Pet designer",
            subtitle = "Draw your own Curie",
            onBack = { navController.popBackStack() },
            compact = wide
        )

        // ── Draw & switch preview picker overlay (v8.49) ─────────────
        pickerCategory?.let { category ->
            DialogScrim(onDismiss = { pickerCategory = null }) {
                DrawPickerDialog(
                    category = category,
                    onCategoryChange = { pickerCategory = it },
                    design = design,
                    selected = target,
                    onSelect = { t ->
                        selectTarget(t)
                        pickerCategory = null
                    },
                    onDismiss = { pickerCategory = null }
                )
            }
        }

        // ── Studio toolbar import menu (v8.52) — one entry point ────
        if (importMenuOpen) {
            DialogScrim(onDismiss = { importMenuOpen = false }) {
                ImportMenuDialog(
                    onPng = {
                        importMenuOpen = false
                        importPngTarget = if (editingGrid == "curled") 2 else 1
                        pngPicker.launch("image/*")
                    },
                    onText = {
                        importMenuOpen = false
                        importDraft = clipboard.getText()?.text ?: ""
                    },
                    onCopy = {
                        clipboard.setText(AnnotatedString(design.toText()))
                        importMenuOpen = false
                        toast = "Copied to clipboard"
                    },
                    onDismiss = { importMenuOpen = false }
                )
            }
        }

        // ── Accessories dialog (Settings, v8.52) ─────────────────────
        if (accessoriesOpen) {
            DialogScrim(onDismiss = { accessoriesOpen = false }) {
                AccessoriesDialog(
                    design = design,
                    onToggleProcedural = { element, enabled ->
                        pushUndo()
                        resetDetailEditor()
                        design = design.withProceduralEnabled(element, enabled)
                    },
                    onDrawLayer = { layer ->
                        accessoriesOpen = false
                        selectTarget(PetEditorTarget.DetailLayer(layer))
                        page = PetDesignerPage.EDITOR
                    },
                    onDismiss = { accessoriesOpen = false }
                )
            }
        }

        // ── Full-screen animation player (v8.56 — Pets page gallery) ──
        playerAnimation?.let { anim ->
            AnimationPlayerDialog(
                animation = anim,
                design = design,
                onEditFrames = {
                    playerAnimation = null
                    selectTarget(PetEditorTarget.Animation(anim.id))
                    page = PetDesignerPage.EDITOR
                },
                onDismiss = { playerAnimation = null }
            )
        }

        // ── Color editor overlay ─────────────────────────────────────
        editingColorKey?.let { key ->
            DialogScrim(onDismiss = { editingColorKey = null }) {
                ColorEditorCard(
                    key = key,
                    initialHex = design.colorOf(key),
                    recentColors = recentColors,
                    onCancel = { editingColorKey = null },
                    onApply = { hex ->
                        rememberColor(hex)
                        pushUndo()
                        design = design.withPaletteColor(key, hex)
                        editingColorKey = null
                    }
                )
            }
        }

        // ── Import/export text overlay ───────────────────────────────
        importDraft?.let { draft ->
            DialogScrim(onDismiss = { importDraft = null }) {
                ImportCard(
                    draft = draft,
                    gridSize = design.gridSize,
                    onCancel = { importDraft = null },
                    onImport = { text ->
                        val parsed = PetDesign.DEFAULT.toParsedOr(text, PetDesign.DEFAULT)
                        // Tolerant parse; treat text that produced no palette
                        // keys AND the default body as unreadable so garbage
                        // can't wipe a design.
                        val looksLikeDesign =
                            parsed != PetDesign.DEFAULT ||
                                text.contains("=") ||
                                text.lines().any { it.length >= design.gridSize }
                        if (looksLikeDesign) {
                            resetDetailEditor()
                            design = parsed
                            reactionLineDraft = parsed.reactionFor(reactEvent).lines.joinToString("\n")
                            importDraft = null
                            true
                        } else {
                            false
                        }
                    }
                )
            }
        }

        // ── PNG import review (v8.37) — eyedropper custom colors ─────
        importReview?.let { review ->
            DialogScrim(onDismiss = { importReview = null }) {
                ImportPngDialog(
                    review = review,
                    onPickCell = { row, col ->
                        importReview = pickColorFromCell(row, col, review) ?: review
                    },
                    onPickColor = { rgb -> importReview = addCustomColor(rgb, review) },
                    onArmSlot = { key ->
                        importReview = review.copy(
                            armed = if (review.armed == key) null else key
                        )
                    },
                    onApply = {
                        resetDetailEditor()
                        pushUndo()
                        design = applyImport(review, design)
                        toast = if (review.touched.isNotEmpty()) {
                            "Imported — ${review.touched.size} custom color(s) added"
                        } else {
                            "Imported PNG — snapped to nearest palette colors"
                        }
                        importReview = null
                    },
                    onCancel = { importReview = null }
                )
            }
        }

        // ── Bed editor (v9.3) — flower bed design dialog ────────────
        if (bedEditorOpen) {
            DialogScrim(onDismiss = { bedEditorOpen = false }) {
                BedDesignDialog(
                    onDismiss = { bedEditorOpen = false },
                    context = context
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// v8.45 — Universal editor shell: local navbar + target picker + save area
// ═══════════════════════════════════════════════════════════════════════════

/**
 * v8.56 — the Pet studio's bottom navigation bar (icons + labels, mirroring
 * the main app's bar). Switching pages clears the editor target so every
 * page lands on its picker first.
 */
@Composable
private fun PetStudioBottomNav(
    page: PetDesignerPage,
    onSelect: (PetDesignerPage) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        // The NavHost content is already padded above the system nav-bar
        // inset, so the bar must not consume it again (double padding).
        windowInsets = WindowInsets(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PetStudioTab(CurioIcons.Pets, "Pets", page == PetDesignerPage.PETS) {
            onSelect(PetDesignerPage.PETS)
        }
        PetStudioTab(CurioIcons.Brush, "Editor", page == PetDesignerPage.EDITOR) {
            onSelect(PetDesignerPage.EDITOR)
        }
        PetStudioTab(CurioIcons.Settings, "Settings", page == PetDesignerPage.SETTINGS) {
            onSelect(PetDesignerPage.SETTINGS)
        }
    }
}

/** One icon + label tab in the studio bottom bar. */
@Composable
private fun RowScope.PetStudioTab(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            CurioIcon(
                name = icon,
                contentDescription = label,
                size = 22.dp
            )
        },
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                )
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/**
 * v8.52 — the sticky studio toolbar: ONE compact row with Save, Undo, Redo,
 * Reset, Export and Import, plus a slim status line. Replaces the old pinned
 * footer SaveArea — every action has exactly one home (no duplicate buttons).
 */
@Composable
private fun EditorToolbar(
    design: PetDesign,
    dirty: Boolean,
    toast: String?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (design.isCustom) "Save design" else "Use default",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                ToolbarIcon(CurioIcons.Undo, "Undo", canUndo) { onUndo() }
                ToolbarIcon(CurioIcons.Redo, "Redo", canRedo) { onRedo() }
                ToolbarIcon(CurioIcons.Refresh, "Reset", design.isCustom) { onReset() }
                ToolbarIcon(CurioIcons.Share, "Export PNG", true) { onExport() }
                ToolbarIcon(CurioIcons.Download, "Import", true) { onImport() }
            }
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    dirty -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Unsaved changes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    toast != null -> Text(
                        toast,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    else -> Text(
                        "Edits apply to your pet — Save keeps them",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

/** One compact circular icon button in the studio toolbar (≥44dp target). */
@Composable
private fun ToolbarIcon(
    icon: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = icon,
                contentDescription = contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                size = 20.dp
            )
        }
    }
}

/**
 * v8.52 — the toolbar's Import menu: PNG or paste-text (one entry point
 * instead of scattered import buttons).
 */
@Composable
private fun ImportMenuDialog(
    onPng: () -> Unit,
    onText: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Import & share",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            "Bring in a design as an image or as text — or copy the current design to the clipboard.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        ImportMenuOption(CurioIcons.Image, "Import PNG", "Sample image colors before applying") { onPng() }
        ImportMenuOption(CurioIcons.FormatText, "Paste design text", "Copy from clipboard, edit by hand") { onText() }
        ImportMenuOption(CurioIcons.Share, "Copy design text", "Share the text format with a friend") { onCopy() }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SmallAction("Close") { onDismiss() }
        }
    }
}

/** One tappable row inside [ImportMenuDialog]. */
@Composable
private fun ImportMenuOption(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurioIcon(
                name = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                size = 20.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CurioIcon(
                name = CurioIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                size = 18.dp
            )
        }
    }
}

/**
 * v8.52 — Settings → Accessories: every part with a live preview, an enable
 * switch, and a "Draw it" shortcut into the editor. The single place to
 * change/disable parts (the old in-editor toggles were removed).
 */
@Composable
private fun AccessoriesDialog(
    design: PetDesign,
    onToggleProcedural: (String, Boolean) -> Unit,
    onDrawLayer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Accessories",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            "Enable, disable, or jump straight to drawing each part.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        PetDesign.PROCEDURAL_KEYS.forEach { element ->
            AccessoryRow(
                element = element,
                design = design,
                onToggle = { enabled -> onToggleProcedural(element, enabled) },
                onDraw = { onDrawLayer(element) }
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SmallAction("Close") { onDismiss() }
        }
    }
}

/** One accessory row: live thumb, name, enable switch, and a Draw shortcut. */
@Composable
private fun AccessoryRow(
    element: String,
    design: PetDesign,
    onToggle: (Boolean) -> Unit,
    onDraw: () -> Unit
) {
    val enabled = design.isProceduralEnabled(element)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (element in PetDesign.DETAIL_KEYS) {
                    MiniPixelThumb(rows = effectiveDetailRows(design, element), design = design)
                } else {
                    CurioIcon(
                        name = CurioIcons.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        size = 18.dp
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    element.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    if (enabled) "Shown on Curie" else "Hidden",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (element in PetDesign.DETAIL_KEYS) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    onClick = onDraw
                ) {
                    Text(
                        "Draw it",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

/** Maps a face-editor mood name to the sprite's Mood enum. */
private fun moodFromName(name: String): CurioPet.Mood =
    runCatching { CurioPet.Mood.valueOf(name) }.getOrDefault(CurioPet.Mood.HAPPY)

/** Upscales a 16×16 preset grid to the current canvas size. */
private fun upscalePreset(rows: List<String>, gridSize: Int): List<String> =
    if (gridSize == 16) rows else PetDesign.resizeGrid(rows, 16, gridSize)

/**
 * Renders a design grid to a pixel-perfect PNG bitmap and returns its
 * shareable FileProvider URI (cache/share, which file_paths.xml exposes).
 */
private fun exportPngUri(
    context: android.content.Context,
    design: PetDesign,
    grid: String,
    moodName: String,
    stage: CurioPet.Stage
): android.net.Uri? {
    val rows = when {
        grid.startsWith("detail:") -> design.detailFor(grid.removePrefix("detail:"))
        grid == "curled" -> design.curledRows
        else -> design.bodyRows
    }
    val n = design.gridSize
    val scale = 12
    val bitmap = Bitmap.createBitmap(n * scale, n * scale, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    fun paintRows(source: List<String>) {
        source.forEachIndexed { r, line ->
            line.forEachIndexed { c, ch ->
                val hex = design.colorFor(ch) ?: return@forEachIndexed
                val color = runCatching { android.graphics.Color.parseColor("#$hex") }.getOrDefault(0xFF4A3426.toInt())
                paint.color = color
                canvas.drawRect(
                    c * scale.toFloat(), r * scale.toFloat(),
                    (c + 1) * scale.toFloat(), (r + 1) * scale.toFloat(),
                    paint
                )
            }
        }
    }
    fun paintProceduralCell(col: Int, row: Int, hex: String, alpha: Float = 1f) {
        val cell = n * scale.toFloat() / 16f
        val color = runCatching {
            android.graphics.Color.parseColor("#$hex")
        }.getOrDefault(0xFF4A3426.toInt())
        paint.color = android.graphics.Color.argb(
            (alpha.coerceIn(0f, 1f) * 255f).toInt(),
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color)
        )
        canvas.drawRect(
            col * cell, row * cell,
            (col + 1) * cell, (row + 1) * cell,
            paint
        )
    }

    paintRows(rows)
    val face = design.faceFor(moodName)
    // Match the static parts of the live sprite that have a meaningful
    // single-frame representation. Motion-only pieces (wag phase, bob,
    // twinkle phase) intentionally use a calm deterministic frame.
    val inkHex = design.colorOf('o')
    val bodyShadeHex = design.colorOf('B')
    val accentHex = design.colorOf('s')
    val goldHex = design.colorOf('G')
    val goldDeepHex = design.colorOf('g')
    if (grid != "curled" && design.isProceduralEnabled("belly")) {
        for (row in 9..11) for (col in 5..10) {
            paintProceduralCell(col, row, if (row == 10) "FFFFFB" else "FFF6E5", 0.85f)
        }
    }
    if (grid != "curled" && design.isProceduralEnabled("tail")) {
        paintProceduralCell(14, 11, bodyShadeHex)
        paintProceduralCell(15, 11, bodyShadeHex)
        paintProceduralCell(15, 12, bodyShadeHex)
    }
    if (grid != "curled" && design.isProceduralEnabled("accessories")) {
        when (stage) {
            CurioPet.Stage.SPROUT -> {
                paintProceduralCell(4, 2, "9CCB8B"); paintProceduralCell(5, 1, "9CCB8B")
                paintProceduralCell(5, 2, "9CCB8B"); paintProceduralCell(5, 3, "9CCB8B")
            }
            CurioPet.Stage.TRAIL_BUDDY -> {
                paintProceduralCell(13, 10, inkHex); paintProceduralCell(14, 10, accentHex)
                paintProceduralCell(15, 10, accentHex); paintProceduralCell(13, 11, inkHex)
                paintProceduralCell(14, 11, accentHex); paintProceduralCell(15, 11, accentHex)
            }
            CurioPet.Stage.ARCHIVE_PAL -> {
                paintProceduralCell(14, 13, "D98BA0"); paintProceduralCell(15, 13, "D98BA0")
                paintProceduralCell(14, 14, "D98BA0"); paintProceduralCell(15, 14, "FFFFFF")
            }
            CurioPet.Stage.SAGE -> {
                paintProceduralCell(4, 0, goldHex); paintProceduralCell(8, 0, goldHex)
                paintProceduralCell(6, 1, goldDeepHex)
            }
            else -> Unit
        }
    }
    if (design.isProceduralEnabled("antenna")) {
        if (grid == "curled") {
            // Match the runtime sleepy nightcap and its pompom.
            paintProceduralCell(7, 0, goldHex); paintProceduralCell(8, 0, goldHex)
            listOf(6, 7, 8, 9).forEach { col ->
                paintProceduralCell(col, 1, "9DB6E8")
                paintProceduralCell(col, 2, "9DB6E8")
                paintProceduralCell(col, 3, "FFF3DC")
            }
        } else {
            paintProceduralCell(7, 0, "FFFFFF", 0.9f)
        }
    }
    if (design.isProceduralEnabled("effects")) {
        if (moodName == CurioPet.Mood.SLEEPY.name) {
            paintProceduralCell(11, 3, inkHex, 0.85f); paintProceduralCell(12, 4, inkHex, 0.85f)
            paintProceduralCell(12, 5, inkHex, 0.65f); paintProceduralCell(13, 6, inkHex, 0.65f)
        }
        if (face.sparkles) {
            paintProceduralCell(1, 2, goldHex, 0.9f); paintProceduralCell(14, 3, goldHex, 0.9f)
            paintProceduralCell(2, 13, goldHex, 0.8f); paintProceduralCell(13, 2, goldHex, 0.8f)
        }
    }
    // Render the selected mood face after the body and generated static art,
    // matching the runtime sprite's visual hierarchy.
    if (face.gridRows.isNotEmpty()) {
        paintRows(face.gridRows)
    } else {
        val ink = design.colorOf('o')
        val blush = design.colorOf('r')
        val white = "FFFFFF"
        when (face.eyes) {
            EyeStyle.OPEN, EyeStyle.WIDE -> {
                val eyeRows = if (face.eyes == EyeStyle.WIDE) listOf(6, 7, 8) else listOf(7, 8)
                eyeRows.forEach { row ->
                    paintProceduralCell(4, row, ink); paintProceduralCell(5, row, ink)
                    paintProceduralCell(10, row, ink); paintProceduralCell(11, row, ink)
                }
                paintProceduralCell(4, 7, white); paintProceduralCell(10, 7, white)
            }
            EyeStyle.BLINK -> {
                paintProceduralCell(4, 7, ink); paintProceduralCell(5, 7, ink)
                paintProceduralCell(10, 7, ink); paintProceduralCell(11, 7, ink)
            }
            EyeStyle.CLOSED -> {
                paintProceduralCell(4, 8, ink); paintProceduralCell(5, 8, ink)
                paintProceduralCell(10, 8, ink); paintProceduralCell(11, 8, ink)
            }
            EyeStyle.STAR -> {
                val star = design.colorOf('y')
                listOf(4 to 6, 5 to 6, 3 to 7, 4 to 7, 5 to 7, 6 to 7, 4 to 8, 5 to 8,
                    10 to 6, 11 to 6, 9 to 7, 10 to 7, 11 to 7, 12 to 7, 10 to 8, 11 to 8)
                    .forEach { (col, row) -> paintProceduralCell(col, row, star) }
                paintProceduralCell(4, 7, white); paintProceduralCell(10, 7, white)
            }
            EyeStyle.DIZZY -> {
                listOf(4 to 6, 5 to 6, 4 to 7, 5 to 7, 4 to 8, 5 to 8, 3 to 7, 6 to 7,
                    10 to 6, 11 to 6, 10 to 7, 11 to 7, 10 to 8, 11 to 8, 9 to 7, 12 to 7)
                    .forEach { (col, row) -> paintProceduralCell(col, row, ink) }
                paintProceduralCell(4, 7, white); paintProceduralCell(5, 6, white)
                paintProceduralCell(10, 7, white); paintProceduralCell(11, 6, white)
            }
            EyeStyle.HAPPY -> {
                listOf(4 to 8, 5 to 7, 5 to 8, 10 to 8, 10 to 7, 11 to 8)
                    .forEach { (col, row) -> paintProceduralCell(col, row, ink) }
            }
        }
        if (face.blush) {
            paintProceduralCell(2, 9, blush, 0.5f); paintProceduralCell(3, 9, blush, 0.5f)
            paintProceduralCell(12, 9, blush, 0.5f); paintProceduralCell(13, 9, blush, 0.5f)
        }
        when (face.mouth) {
            MouthStyle.SMILE -> {
                paintProceduralCell(6, 10, ink); paintProceduralCell(9, 10, ink)
                paintProceduralCell(7, 11, ink); paintProceduralCell(8, 11, ink)
            }
            MouthStyle.WIDE -> {
                paintProceduralCell(6, 10, ink); paintProceduralCell(9, 10, ink)
                (6..9).forEach { col -> paintProceduralCell(col, 11, ink) }
            }
            MouthStyle.O -> {
                paintProceduralCell(7, 10, ink); paintProceduralCell(8, 10, ink)
                paintProceduralCell(7, 11, ink); paintProceduralCell(8, 11, ink)
            }
            MouthStyle.NONE -> Unit
        }
    }
    // Include every user-authored detail layer last, matching the live sprite
    // so custom art remains visibly on top of generated face/effect art.
    PetDesign.DETAIL_KEYS.forEach { layer -> paintRows(design.detailFor(layer)) }

    val dir = File(context.cacheDir, "share")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "curie_${grid}_${n}x$n.png")
    return runCatching {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

/** Shares a PNG via the Android share sheet. */
private fun sharePng(context: android.content.Context, uri: android.net.Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "Curie", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share Curie as PNG"))
    }
}

/** The four custom paint slots an imported image can fill (c C d D). */
private val CUSTOM_SLOTS = listOf('c', 'C', 'd', 'D')

/** A color found in an imported image (rgb 0xRRGGBB) plus its pixel count. */
private data class ImportedColor(val rgb: Int, val count: Int)

/**
 * The in-progress PNG import (v8.37): the raw scaled pixels plus the
 * eyedropper/quick-pick state. The image is NOT applied until the user
 * confirms with Apply, so they can add custom colors first.
 */
private data class ImportReview(
    val pixels: IntArray,
    val gridSize: Int,
    /** The image's dominant colors (quantized), for the quick-pick row. */
    val unique: List<ImportedColor>,
    /** The current colors of the four custom slots (hex). */
    val custom: Map<Char, String>,
    /** Slots the user filled during this import session. */
    val touched: Set<Char> = emptySet(),
    /** The slot the eyedropper fills next, or null for auto-next. */
    val armed: Char? = null,
    /** Which grid the import lands on (1 = body, 2 = curled). */
    val target: Int
)

/**
 * Scans the imported pixels into the review state: the design's current
 * custom-slot colors plus the dominant quantized colors for quick picks
 * (4-bit-per-channel quantization merges near-identical shades so the row
 * reads as distinct swatches instead of JPEG-ish noise).
 */
private fun buildImportReview(pixels: IntArray, gridSize: Int, design: PetDesign, target: Int): ImportReview {
    val counts = HashMap<Int, Int>()
    for (i in pixels.indices) {
        val argb = pixels[i]
        if (((argb ushr 24) and 0xFF) < 128) continue
        val r = ((argb shr 16) and 0xFF) and 0xF0
        val g = ((argb shr 8) and 0xFF) and 0xF0
        val b = (argb and 0xFF) and 0xF0
        val q = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        counts[q] = (counts[q] ?: 0) + 1
    }
    return ImportReview(
        pixels = pixels,
        gridSize = gridSize,
        unique = counts.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { ImportedColor(it.key and 0xFFFFFF, it.value) },
        custom = CUSTOM_SLOTS.associateWith { design.colorOf(it) },
        target = target
    )
}

/**
 * Adds [rgb] to the review's custom slots: the armed slot, else the next
 * untouched one, else the first slot (so a last slot can be overwritten).
 * Disarms after a pick so repeated taps walk through the slots in order.
 */
private fun addCustomColor(rgb: Int, review: ImportReview): ImportReview {
    val slot = review.armed
        ?: CUSTOM_SLOTS.firstOrNull { it !in review.touched }
        ?: CUSTOM_SLOTS.first()
    return review.copy(
        custom = review.custom + (slot to rgbToHex(rgb)),
        touched = review.touched + slot,
        armed = null
    )
}

/** Eyedropper tap on the preview: picks the tapped pixel's color. */
private fun pickColorFromCell(row: Int, col: Int, review: ImportReview): ImportReview? {
    val argb = review.pixels.getOrNull(row * review.gridSize + col) ?: return null
    if (((argb ushr 24) and 0xFF) < 128) return null
    return addCustomColor(argb and 0xFFFFFF, review)
}

/** Formats an 0xRRGGBB int as an uppercase "RRGGBB" hex string. */
private fun rgbToHex(rgb: Int): String {
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    return "${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
        .uppercase()
}

/**
 * Snaps a pixel array to a [gridSize]×[gridSize] grid of palette keys:
 * every opaque pixel is matched to the nearest palette color by RGB
 * distance (fully transparent pixels become empty cells). The palette is
 * read from [design] — so the custom slots' picked colors win for matching
 * pixels on apply.
 */
private fun snapPixelGrid(pixels: IntArray, gridSize: Int, design: PetDesign): List<String> {
    val keys = PetDesign.KEYS
    val paletteRgb = keys.map { key ->
        val hex = design.colorOf(key)
        Triple(
            hex.substring(0, 2).toInt(16),
            hex.substring(2, 4).toInt(16),
            hex.substring(4, 6).toInt(16)
        )
    }
    val rows = MutableList(gridSize) { StringBuilder() }
    for (r in 0 until gridSize) {
        for (c in 0 until gridSize) {
            val argb = pixels[r * gridSize + c]
            val alpha = (argb ushr 24) and 0xFF
            if (alpha < 128) {
                rows[r].append('.')
                continue
            }
            val red = (argb shr 16) and 0xFF
            val green = (argb shr 8) and 0xFF
            val blue = argb and 0xFF
            var best = 0
            var bestDist = Int.MAX_VALUE
            paletteRgb.forEachIndexed { i, (kr, kg, kb) ->
                val dr = red - kr
                val dg = green - kg
                val db = blue - kb
                val d = dr * dr + dg * dg + db * db
                if (d < bestDist) {
                    bestDist = d
                    best = i
                }
            }
            rows[r].append(keys[best])
        }
    }
    return rows.map { it.toString() }
}

/**
 * Folds the review's custom-slot colors into the design's palette, snaps
 * the pixels with that extended palette, and applies the grid to the body
 * or curled pose (per the review's target).
 */
private fun applyImport(review: ImportReview, design: PetDesign): PetDesign {
    val design2 = CUSTOM_SLOTS.fold(design) { d, key ->
        d.withPaletteColor(key, review.custom[key] ?: d.colorOf(key))
    }
    val rows = snapPixelGrid(review.pixels, review.gridSize, design2)
    return design2.withGrid(if (review.target == 1) "body" else "curled", rows)
}

/** A dim full-screen scrim with a centered dialog surface. */
@Composable
private fun DialogScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Consume taps inside the card so they don't dismiss the dialog.
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            content()
        }
    }
}

/**
 * PNG import review: shows the raw image and guides the user through
 * sampling colors into four named saved-color destinations before the image
 * is applied. Tapping the image samples a color; the suggested-color row
 * offers dominant image colors; Apply snaps the image using the saved colors.
 */
@Composable
private fun ImportPngDialog(
    review: ImportReview,
    onPickCell: (Int, Int) -> Unit,
    onPickColor: (Int) -> Unit,
    onArmSlot: (Char) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Choose colors from your image",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (review.armed != null) {
                    "Color target selected: tap a pixel in the image to save that color"
                } else {
                    "Tap a pixel or a suggested color, then choose where to save it. " +
                        "You can use these colors throughout Curie's design."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))

            // ── The imported image — tap to eyedropper a color ────────
            // The gesture coroutine only restarts when its key changes, so
            // it must call the LATEST callback via rememberUpdatedState — a
            // captured stale onPickCell/review would recompute every pick
            // from the pristine first review and silently drop earlier picks.
            val latestOnPickCell by rememberUpdatedState(onPickCell)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .pointerInput(review.gridSize) {
                        detectTapGestures { offset ->
                            val cell = size.width / review.gridSize
                            val col = (offset.x / cell).toInt().coerceIn(0, review.gridSize - 1)
                            val row = (offset.y / cell).toInt().coerceIn(0, review.gridSize - 1)
                            latestOnPickCell(row, col)
                        }
                    }
            ) {
                val cell = size.width / review.gridSize
                for (r in 0 until review.gridSize) {
                    for (c in 0 until review.gridSize) {
                        val argb = review.pixels[r * review.gridSize + c]
                        if (((argb ushr 24) and 0xFF) < 128) continue
                        drawRect(
                            color = Color(argb),
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell, cell)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── Named saved-color targets — tap to choose a destination ─
            Text(
                "Save sampled color as…",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CUSTOM_SLOTS.chunked(2).forEachIndexed { rowIndex, rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSlots.forEachIndexed { slotIndex, key ->
                            val colorHex = review.custom[key] ?: "FFFFFF"
                            val armed = review.armed == key
                            val filled = key in review.touched
                            val label = when (rowIndex * 2 + slotIndex) {
                                0 -> "Main accent"
                                1 -> "Soft accent"
                                2 -> "Highlight"
                                else -> "Extra color"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    width = if (armed) 2.dp else 1.dp,
                                    color = if (armed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                ),
                                onClick = { onArmSlot(key) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(hexColor(colorHex))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            if (filled) "Color saved" else "Choose target",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Select a destination above, then tap the image to sample a color. If you do not choose one, Curie fills destinations in order.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))

            // ── Suggested colors — the image's dominant colors ────────
            if (review.unique.isNotEmpty()) {
                Text(
                    "Suggested colors from your image",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    review.unique.take(8).forEach { imported ->
                        val rgbColor = Color(0xFF000000L or imported.rgb.toLong())
                        val hex = rgbToHex(imported.rgb)
                        val used = review.custom.values.any { it.equals(hex, ignoreCase = true) }
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = rgbColor,
                            border = BorderStroke(
                                2.dp,
                                if (used) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.7f)
                            ),
                            onClick = { onPickColor(imported.rgb) },
                            modifier = Modifier.size(36.dp)
                        ) {}
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(14.dp))

            // ── Actions ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onApply,
                    modifier = Modifier.weight(1.4f)
                ) {
                    Text(
                        "Apply import",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/** Dark or light ink so a letter on a swatch stays readable. */
private fun contrastingInk(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color(0xFF2A2015) else Color.White

/** The hex + HSL color editor card (v8.47 — preview, hue strip, recents). */
@Composable
private fun ColorEditorCard(
    key: Char,
    initialHex: String,
    recentColors: List<String>,
    onCancel: () -> Unit,
    onApply: (String) -> Unit
) {
    var hexDraft by rememberSaveable(key) { mutableStateOf(initialHex) }
    var hsl by rememberSaveable(key) { mutableStateOf(hexToHsl(initialHex)) }
    val hexError = hexDraft.length != 6
    Column(
        modifier = Modifier
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Edit ${PALETTE_SLOTS.firstOrNull { it.key == key }?.name ?: key} color",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // weight(1f) lives at the CALL SITE (inside this Row scope) —
            // ColorPreviewColumn itself is a plain Column, so weight would be
            // unresolved inside its own definition (CI fix).
            ColorPreviewColumn("Original", initialHex, accent = false, modifier = Modifier.weight(1f))
            ColorPreviewColumn(
                "New", hexDraft, accent = !hexError && hexDraft != initialHex,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (hexError) CurioColors.WarmCoralRed else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(hexColor(hexDraft))
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = hexDraft,
                        onValueChange = { input ->
                            hexDraft = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                                .uppercase().take(6)
                            if (hexDraft.length == 6) {
                                hsl = hexToHsl(hexDraft)
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
                Text(
                    "HEX",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Hue — drag the strip",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        HueStrip(hue = hsl.first) { hue ->
            hsl = hsl.copy(first = hue)
            hexDraft = hslToHex(hsl.first, hsl.second, hsl.third)
        }
        Spacer(Modifier.height(10.dp))
        SliderRow("Saturation", hsl.second, 1f) {
            hsl = hsl.copy(second = it)
            hexDraft = hslToHex(hsl.first, hsl.second, hsl.third)
        }
        SliderRow("Lightness", hsl.third, 1f) {
            hsl = hsl.copy(third = it)
            hexDraft = hslToHex(hsl.first, hsl.second, hsl.third)
        }
        if (recentColors.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Recent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentColors.forEach { hex ->
                    val selected = hexDraft == hex
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(hexColor(hex))
                            .then(
                                if (selected) {
                                    Modifier
                                        .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                } else Modifier
                            )
                            .clickable { hexDraft = hex; hsl = hexToHsl(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            CurioIcon(
                                name = CurioIcons.Check,
                                contentDescription = null,
                                tint = Color.White,
                                size = 16.dp
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Quick picks",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        QUICK_HEX.chunked(6).forEach { rowHex ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowHex.forEach { hex ->
                    val selected = hexDraft == hex
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(hexColor(hex))
                            .then(
                                if (selected) {
                                    Modifier
                                        .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                } else Modifier
                            )
                            .clickable { hexDraft = hex; hsl = hexToHsl(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            CurioIcon(
                                name = CurioIcons.Check,
                                contentDescription = null,
                                tint = Color.White,
                                size = 16.dp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (hexError) {
            Text(
                "Enter 6 hex digits (0-9, A-F)",
                style = MaterialTheme.typography.labelSmall,
                color = CurioColors.WarmCoralRed
            )
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallAction("Cancel", enabled = true) { onCancel() }
            SmallAction("Apply", enabled = !hexError) {
                parseHex(hexDraft)?.let { onApply(it) }
            }
        }
    }
}

/** v8.47 — big before/after preview column for the color editor. */
@Composable
private fun ColorPreviewColumn(label: String, hex: String, accent: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(hexColor(hex))
                .border(
                    2.dp,
                    if (accent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    RoundedCornerShape(14.dp)
                )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "#$hex",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** v8.47 — a draggable hue strip (0..360) rendered as a gradient track. */
@Composable
private fun HueStrip(hue: Float, onChange: (Float) -> Unit) {
    val latestOnChange by rememberUpdatedState(onChange)
    val trackHeight = 30.dp
    val thumbRadius = 11.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .pointerInput(Unit) {
                fun moveTo(x: Float, w: Float) {
                    val inset = thumbRadius.toPx()
                    val frac = ((x - inset) / (w - 2 * inset)).coerceIn(0f, 1f)
                    latestOnChange(frac * 360f)
                }
                detectTapGestures { offset -> moveTo(offset.x, size.width.toFloat()) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val inset = thumbRadius.toPx()
                        val frac = ((offset.x - inset) / (size.width.toFloat() - 2 * inset)).coerceIn(0f, 1f)
                        latestOnChange(frac * 360f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val inset = thumbRadius.toPx()
                        val frac = ((change.position.x - inset) / (size.width.toFloat() - 2 * inset)).coerceIn(0f, 1f)
                        latestOnChange(frac * 360f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                brush = Brush.horizontalGradient(*hueStops()),
                cornerRadius = CornerRadius(trackHeight.toPx() / 2f, trackHeight.toPx() / 2f)
            )
            val inset = thumbRadius.toPx()
            val thumbX = inset + (hue / 360f) * (size.width - 2 * inset)
            drawCircle(
                color = Color.White,
                radius = thumbRadius.toPx(),
                center = Offset(thumbX, size.height / 2f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = thumbRadius.toPx(),
                center = Offset(thumbX, size.height / 2f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/** Hue gradient stops for [HueStrip] (full saturation, mid lightness). */
private fun hueStops(): Array<Pair<Float, Color>> =
    listOf(0f, 30f, 60f, 90f, 120f, 150f, 180f, 210f, 240f, 270f, 300f, 330f, 360f)
        .map { h -> (h / 360f) to hexColor(hslToHex(h, 1f, 0.5f)) }
        .toTypedArray()

/** v8.48 — loops an animation's frames and renders the current one. */
@Composable
private fun PetAnimationPreview(
    animation: PetAnimation,
    design: PetDesign,
    spriteSize: Dp,
    playing: Boolean = true
) {
    // v8.52 — resolve the design's custom frames so gallery previews show the
    // per-frame pixel poses the user drew (an absent key = built-in frames).
    val effective = design.animations[animation.id] ?: animation
    var frameIndex by remember(animation.id) { mutableStateOf(0) }
    LaunchedEffect(animation.id, playing, effective.frames) {
        if (!playing || effective.frames.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(effective.frames[frameIndex].durationMs.toLong())
            frameIndex = (frameIndex + 1) % effective.frames.size
        }
    }
    val frame = effective.frames.getOrNull(frameIndex) ?: PetAnimationFrame()
    AnimatedPetSprite(
        animation = effective,
        frame = frame,
        design = design,
        spriteSize = spriteSize,
        ghost = false
    )
}

/**
 * v8.48 — renders the pet at one animation frame (transform + ghost alpha).
 * v8.54 — [staticPose] freezes the sprite's idle animation so the timeline
 * editor previews each frame exactly as authored (no blinking eyes / bobbing
 * body masking the frame's zoom + rotate while editing).
 */
@Composable
private fun AnimatedPetSprite(
    animation: PetAnimation,
    frame: PetAnimationFrame,
    design: PetDesign,
    spriteSize: Dp,
    ghost: Boolean,
    staticPose: Boolean = false
) {
    CurioPetSprite(
        stage = CurioPet.currentStage(),
        mood = runCatching { CurioPet.Mood.valueOf(animation.mood) }.getOrDefault(CurioPet.Mood.HAPPY),
        spriteSize = spriteSize,
        design = design,
        // v8.52 — per-frame pixel layers: a frame's custom pose overrides the
        // design's body/curled grid while it plays.
        bodyOverride = frame.bodyRows,
        curledOverride = frame.curledRows,
        eyeOverride = frame.eyeGrid,
        staticPose = staticPose,
        modifier = Modifier.graphicsLayer {
            translationY = frame.offsetY.dp.toPx()
            scaleX = frame.scale
            scaleY = frame.scale
            rotationZ = frame.rotationDegrees
            alpha = if (ghost) 0.35f else 1f
        }
    )
}

/** v8.48 — one gallery card: looping mini preview + name + frame count. */
@Composable
private fun AnimationGalleryCard(
    animation: PetAnimation,
    design: PetDesign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PetAnimationPreview(animation = animation, design = design, spriteSize = 44.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                animation.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${animation.frames.size} frames · loops",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** v8.48 — the frame timeline editor for one animation. */
@Composable
private fun AnimationTimelineEditor(
    animationId: String,
    design: PetDesign,
    onDesignChange: (PetDesign) -> Unit,
    onPushUndo: () -> Unit = {},
    onEditColor: (Char) -> Unit = {}
) {
    val base = animationById(animationId) ?: return
    // v8.54 — the timeline OPENS PAUSED so the first thing you see is the
    // selected frame standing still (zoom + rotate visible), not an animation
    // racing under you while you try to edit. Press Play to preview motion.
    var playing by rememberSaveable(animationId) { mutableStateOf(false) }
    var selectedFrame by rememberSaveable(animationId) { mutableStateOf(0) }
    var onionSkin by rememberSaveable { mutableStateOf(false) }
    // v8.36 — zoom for the per-frame drawing grid.
    var frameZoom by rememberSaveable(animationId) { mutableStateOf(1f) }
    // v8.52 — per-frame drawing state: which grid ("body"/"curled"), the
    // active paint tool, and the paint color (shares the design palette, but
    // is independent so picking a frame tool never disturbs the main editor).
    var frameGrid by rememberSaveable(animationId) { mutableStateOf("body") }
    var frameTool by rememberSaveable(animationId) { mutableStateOf<PaintTool?>(null) }
    var framePaintKey by rememberSaveable(animationId) { mutableStateOf('b') }
    // v8.52 — in-progress per-frame pixel drafts (frame index → grid kind →
    // rows). Kept separate from the committed design so a stroke renders
    // immediately; the frame is committed to the design on every cell.
    var frameDrafts by remember(animationId) {
        mutableStateOf<Map<Int, Map<String, List<String>>>>(emptyMap())
    }
    // v8.52 — frames derive from the design (not a remember() snapshot), so
    // Undo/Redo from the footer and edits elsewhere stay in sync.
    val frames = design.animations[animationId]?.frames ?: base.frames
    LaunchedEffect(animationId, playing, frames) {
        if (!playing || frames.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(frames.getOrNull(selectedFrame)?.durationMs?.toLong() ?: 180L)
            selectedFrame = (selectedFrame + 1) % frames.size
        }
    }
    val shown = frames.getOrNull(selectedFrame) ?: PetAnimationFrame()
    fun commitFrame(index: Int, frame: PetAnimationFrame) {
        val updated = frames.mapIndexed { i, f -> if (i == index) frame else f }
        onDesignChange(
            design.copy(animations = design.animations + (animationId to base.copy(frames = updated)))
        )
    }
    // The art a frame is currently wearing: its own pixel override when it
    // has one (and it matches the canvas), else the design's grid. The eyes
    // layer is authored in a fixed 16×16 space (the same one the procedural
    // eyes use) and starts blank.
    fun effectiveFrameRows(index: Int, kind: String): List<String> {
        val frame = frames.getOrNull(index)
        val override = when (kind) {
            "curled" -> frame?.curledRows
            "eyes" -> frame?.eyeGrid
            else -> frame?.bodyRows
        }
        val targetSize = if (kind == "eyes") 16 else design.gridSize
        if (override != null && override.size == targetSize &&
            override.all { it.length == targetSize }
        ) return override
        return when (kind) {
            "curled" -> design.curledRows
            "eyes" -> List(16) { ".".repeat(16) }
            else -> design.bodyRows
        }
    }
    fun draftRows(index: Int, kind: String): List<String> =
        frameDrafts[index]?.get(kind) ?: effectiveFrameRows(index, kind)
    fun paintFrame(index: Int, kind: String, row: Int, col: Int) {
        val current = draftRows(index, kind)
        val next = applyToolToRows(current, frameTool, framePaintKey, row, col) ?: return
        frameDrafts = frameDrafts + (index to (frameDrafts[index] ?: emptyMap()) + (kind to next))
        val frame = frames.getOrNull(index) ?: return
        val norm = normalizeFrameRows(next, if (kind == "eyes") 16 else design.gridSize)
        commitFrame(
            index,
            when (kind) {
                "curled" -> frame.copy(curledRows = norm)
                "eyes" -> frame.copy(eyeGrid = norm)
                else -> frame.copy(bodyRows = norm)
            }
        )
    }
    SectionCard(
        "${base.name} timeline",
        if (design.animations[animationId] == null) "Draw each frame's pose below — untouched frames keep the base design"
        else "Custom frames — Save pet keeps them"
    ) {
        // v8.52 — the live preview fills the same width as the drawing grid
        // below ("drawing size"), so the animation plays at the true scale.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            // v8.36 — cap the preview so the frame strip + drawing grid stay
            // reachable together; it still plays at true proportions.
            val previewSize = maxWidth.coerceAtMost(190.dp)
            if (onionSkin && selectedFrame > 0) {
                AnimatedPetSprite(
                    animation = base,
                    frame = frames.getOrNull(selectedFrame - 1) ?: PetAnimationFrame(),
                    design = design,
                    spriteSize = previewSize,
                    ghost = true,
                    staticPose = !playing
                )
            }
            AnimatedPetSprite(
                animation = base,
                frame = shown,
                design = design,
                spriteSize = previewSize,
                ghost = false,
                staticPose = !playing
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportIconButton(CurioIcons.ChevronLeft, "Previous frame", enabled = frames.isNotEmpty()) {
                playing = false
                selectedFrame = (selectedFrame - 1 + frames.size) % frames.size
            }
            Spacer(Modifier.width(8.dp))
            TransportIconButton(
                icon = if (playing) CurioIcons.Pause else CurioIcons.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play"
            ) {
                if (!playing && frames.isNotEmpty()) selectedFrame = selectedFrame % frames.size
                playing = !playing
            }
            Spacer(Modifier.width(8.dp))
            TransportIconButton(CurioIcons.ChevronRight, "Next frame", enabled = frames.isNotEmpty()) {
                playing = false
                selectedFrame = (selectedFrame + 1) % frames.size
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Frame ${selectedFrame + 1} of ${frames.size}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        // v8.54 — the frame's zoom/rotate/lift readout. With the preview now
        // static while paused, these values are exactly what you see applied.
        val transformNote = buildString {
            if (shown.scale != 1f) {
                if (isNotEmpty()) append(" · ")
                append("zoom ${(shown.scale * 100).roundToInt()}%")
            }
            if (shown.rotationDegrees != 0f) {
                if (isNotEmpty()) append(" · ")
                append("rotate ${shown.rotationDegrees.roundToInt()}°")
            }
            if (shown.offsetY != 0f) {
                if (isNotEmpty()) append(" · ")
                append("lift ${shown.offsetY.roundToInt()}px")
            }
        }
        if (transformNote.isNotEmpty()) {
            Text(
                transformNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            frames.forEachIndexed { i, f ->
                FrameThumb(
                    animation = base,
                    frame = f,
                    design = design,
                    index = i,
                    selected = i == selectedFrame,
                    onClick = {
                        selectedFrame = i
                        playing = false
                    }
                )
            }
        }
        // ── Per-frame pixel editor (v8.52) ─────────────────────────
        Spacer(Modifier.height(16.dp))
        Text(
            "Draw this frame",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            when (frameGrid) {
                    "eyes" -> if (base.mood == "SLEEPY") {
                        "Paint this frame's eyes — sleepy (curled) frames keep their own closed-eye look, so the layer applies once the pet stands."
                    } else {
                        "Paint this frame's eyes — your pixels replace the mood's procedural eyes while this frame plays, so the eyes can blink or glance frame by frame."
                    }
                    "curled" -> "Paint this ${base.name} frame's asleep pose."
                    else -> "Paint this ${base.name} frame's pose — drawn frames override the base design while the animation plays. Blank cells stay transparent."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            GridTab("Body", frameGrid == "body") { frameGrid = "body"; playing = false }
            GridTab("Asleep", frameGrid == "curled") { frameGrid = "curled"; playing = false }
            GridTab("Eyes", frameGrid == "eyes") { frameGrid = "eyes"; playing = false }
        }
        Spacer(Modifier.height(10.dp))
        CanvasStatus(activeTool = frameTool)
        Spacer(Modifier.height(10.dp))
        QuickPaletteRow(
            selectedKey = framePaintKey,
            design = design,
            onSelect = {
                framePaintKey = it
                frameTool = PaintTool.BRUSH
                playing = false
            },
            onEdit = onEditColor
        )
        Spacer(Modifier.height(12.dp))
        val paintHandler: (Int, Int, Boolean) -> Unit = { row, col, continuous ->
            // v8.54 — painting a frame pauses playback so the preview and the
            // grid both stay on the frame you're editing.
            playing = false
            val tool = frameTool
            val mutating = tool == PaintTool.BRUSH || tool == PaintTool.FILL || tool == PaintTool.ERASER
            if (mutating && !continuous) onPushUndo()
            if (tool == PaintTool.EYEDROPPER) {
                if (!continuous) {
                    val picked = draftRows(selectedFrame, frameGrid).getOrNull(row)?.getOrNull(col) ?: '.'
                    if (picked != '.') {
                        framePaintKey = picked
                        frameTool = PaintTool.BRUSH
                    } else {
                        frameTool = PaintTool.ERASER
                    }
                }
            } else if (tool != null) {
                // FILL acts once per gesture (mirror the main editor) so
                // a drag doesn't re-run the bucket at every cell.
                if (tool == PaintTool.FILL) {
                    if (!continuous) paintFrame(selectedFrame, frameGrid, row, col)
                } else {
                    paintFrame(selectedFrame, frameGrid, row, col)
                }
            }
        }
        if (frameGrid == "eyes") {
            // v8.52 — per-frame eyes: a fixed 16×16 grid over the mood's
            // procedural eye art as a locked blueprint for easy alignment.
            Text(
                "The eyes are authored on a fixed 16×16 grid — the blueprint behind shows the mood's procedural eyes, and the sprite above previews your drawing live.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // v8.54 — static while editing the eyes so your drawing is
                // never masked by a blinking pet.
                AnimatedPetSprite(
                    animation = base,
                    frame = shown,
                    design = design,
                    spriteSize = 96.dp,
                    ghost = false,
                    staticPose = true
                )
            }
            Spacer(Modifier.height(10.dp))
            PixelGrid(
                design = design.copy(gridSize = 16),
                grid = "frame",
                rowsOverride = draftRows(selectedFrame, frameGrid),
                blueprintRows = eyeBlueprintRows(design, base.mood),
                showBlueprint = true,
                zoom = frameZoom,
                tool = frameTool,
                onTool = paintHandler
            )
        } else {
            PixelGrid(
                design = design,
                grid = "frame",
                rowsOverride = draftRows(selectedFrame, frameGrid),
                zoom = frameZoom,
                tool = frameTool,
                onTool = paintHandler
            )
        }
        Spacer(Modifier.height(10.dp))
        ToolTray(
            activeTool = frameTool,
            onSelect = { frameTool = it; playing = false }
        )
        Spacer(Modifier.height(8.dp))
        SliderRow(label = "Zoom", value = frameZoom, max = 3f) { frameZoom = it }
        Spacer(Modifier.height(12.dp))
        val resetLabel = if (frameGrid == "eyes") "eyes" else "pose"
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction(
                "Reset frame $resetLabel",
                enabled = when (frameGrid) {
                    "eyes" -> shown.eyeGrid != null
                    "curled" -> shown.curledRows != null
                    else -> shown.bodyRows != null
                }
            ) {
                onPushUndo()
                frameDrafts = frameDrafts - selectedFrame
                commitFrame(
                    selectedFrame,
                    when (frameGrid) {
                        "eyes" -> shown.copy(eyeGrid = null)
                        "curled" -> shown.copy(curledRows = null)
                        else -> shown.copy(bodyRows = null)
                    }
                )
            }
            SmallAction(
                "Reset all frames",
                enabled = design.animations[animationId] != null
            ) {
                onPushUndo()
                frameDrafts = emptyMap()
                onDesignChange(design.copy(animations = design.animations - animationId))
                selectedFrame = 0
            }
        }
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            label = "Previous-frame ghost (onion skin)",
            checked = onionSkin,
            onCheckedChange = { onionSkin = it }
        )
    }
}

/** v8.52 — applies a paint tool to one frame's pixel rows (null = no-op). */
private fun applyToolToRows(
    rows: List<String>,
    tool: PaintTool?,
    key: Char,
    row: Int,
    col: Int
): List<String>? {
    if (row !in rows.indices) return null
    if (col !in rows[row].indices) return null
    return when (tool) {
        PaintTool.BRUSH -> setCell(rows, row, col, key)
        PaintTool.ERASER -> setCell(rows, row, col, '.')
        PaintTool.FILL -> floodFillRows(rows, row, col, key)
        PaintTool.EYEDROPPER, null -> null
    }
}

/** Sets one cell of a pixel rows list. */
private fun setCell(rows: List<String>, row: Int, col: Int, key: Char): List<String> {
    val out = rows.toMutableList()
    val chars = out[row].toCharArray()
    chars[col] = key
    out[row] = String(chars)
    return out
}

/** Flood-fills the connected region of the same key at [row]/[col]. */
private fun floodFillRows(rows: List<String>, row: Int, col: Int, key: Char): List<String> {
    val target = rows[row][col]
    if (target == key) return rows
    val work = rows.map { it.toCharArray() }
    val size = work.size
    val stack = ArrayDeque<Pair<Int, Int>>()
    stack.add(row to col)
    while (stack.isNotEmpty()) {
        val (r, c) = stack.removeLast()
        if (r !in 0 until size || c !in 0 until size) continue
        if (work[r][c] != target) continue
        work[r][c] = key
        stack.add(r - 1 to c)
        stack.add(r + 1 to c)
        stack.add(r to c - 1)
        stack.add(r to c + 1)
    }
    return work.map { String(it) }
}

/** Pads/truncates frame pixel rows to the canvas size (same rule as [PetDesign.withGrid]). */
private fun normalizeFrameRows(rows: List<String>, gridSize: Int): List<String> {
    val cleaned = rows.map { (it + ".".repeat(gridSize)).take(gridSize) }
    return if (cleaned.size >= gridSize) cleaned.take(gridSize)
    else cleaned + List(gridSize - cleaned.size) { ".".repeat(gridSize) }
}

/**
 * v8.52 — the mood's procedural eye art as 16×16 blueprint rows for the Eyes
 * editor (white glints excluded so only the ink lines guide the drawing).
 */
private fun eyeBlueprintRows(design: PetDesign, moodName: String): List<String> {
    val style = design.faceFor(moodName).eyes
    val rows = Array(16) { CharArray(16) { '.' } }
    EYE_STYLE_PIXELS[style]?.forEach { (col, row, slot) ->
        if (slot != "white") rows[row][col] = 'o'
    }
    return rows.map { String(it) }
}

/** Mouth glyphs for the face blueprint (16×16 space, matching the eyes). */
private val MOUTH_PIXELS: Map<MouthStyle, List<Pair<Int, Int>>> = mapOf(
    MouthStyle.SMILE to listOf(7 to 10, 8 to 10, 7 to 11, 8 to 11),
    MouthStyle.WIDE to listOf(
        6 to 10, 7 to 10, 8 to 10, 9 to 10,
        6 to 11, 7 to 11, 8 to 11, 9 to 11,
        7 to 12, 8 to 12
    ),
    MouthStyle.O to listOf(6 to 10, 7 to 10, 8 to 10, 9 to 10, 6 to 11, 9 to 11),
    MouthStyle.NONE to emptyList()
)

/**
 * v8.36 — the face blueprint: the mood's procedural eyes + mouth + blush
 * projected onto the full canvas, so painting the face always has the
 * default face as a locked reference underneath.
 */
private fun faceBlueprintRows(design: PetDesign, moodName: String): List<String> {
    val gridSize = design.gridSize
    val face = design.faceFor(moodName)
    val small = Array(16) { CharArray(16) { '.' } }
    EYE_STYLE_PIXELS[face.eyes]?.forEach { (col, row, slot) ->
        if (slot != "white") small[row][col] = 'o'
    }
    MOUTH_PIXELS[face.mouth]?.forEach { (col, row) -> small[row][col] = 'o' }
    if (face.blush) listOf(2 to 9, 13 to 9).forEach { (col, row) -> small[row][col] = 'o' }
    val rows = MutableList(gridSize) { CharArray(gridSize) { '.' } }
    small.forEachIndexed { r, line ->
        line.forEachIndexed { c, ch ->
            if (ch != '.') {
                val col = ((c + 0.5f) * gridSize / 16f).toInt().coerceIn(0, gridSize - 1)
                val row = ((r + 0.5f) * gridSize / 16f).toInt().coerceIn(0, gridSize - 1)
                rows[row][col] = ch
            }
        }
    }
    return rows.map { String(it) }
}

/** v8.48 — one frame thumbnail in the timeline. */
@Composable
private fun FrameThumb(
    animation: PetAnimation,
    frame: PetAnimationFrame,
    design: PetDesign,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedPetSprite(
                animation = animation,
                frame = frame,
                design = design,
                spriteSize = 34.dp,
                ghost = false,
                staticPose = true
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** v8.49 — one category chip in the Draw & switch strip / picker dialog. */
@Composable
private fun StripChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

/** v8.49 — the preview picker dialog: name-tagged cards per category. */
@Composable
private fun DrawPickerDialog(
    category: String,
    onCategoryChange: (String) -> Unit,
    design: PetDesign,
    selected: PetEditorTarget?,
    onSelect: (PetEditorTarget) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "What do you want to edit?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StripChip("Body & pose", category == "body") { onCategoryChange("body") }
            StripChip("Faces", category == "faces") { onCategoryChange("faces") }
            StripChip("Details", category == "details") { onCategoryChange("details") }
            StripChip("Animations", category == "animations") { onCategoryChange("animations") }
            StripChip("Actions", category == "actions") { onCategoryChange("actions") }
        }
        Spacer(Modifier.height(12.dp))
        when (category) {
            "body" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PickerCard(
                        title = "Body",
                        subtitle = "Standing pose",
                        selected = selected is PetEditorTarget.Body,
                        modifier = Modifier.weight(1f),
                        preview = {
                            CurioPetSprite(
                                stage = CurioPet.currentStage(),
                                mood = CurioPet.Mood.HAPPY,
                                spriteSize = 46.dp,
                                design = design
                            )
                        },
                        onClick = { onSelect(PetEditorTarget.Body) }
                    )
                    PickerCard(
                        title = "Curled pose",
                        subtitle = "Asleep",
                        selected = selected is PetEditorTarget.CurledPose,
                        modifier = Modifier.weight(1f),
                        preview = {
                            CurioPetSprite(
                                stage = CurioPet.currentStage(),
                                mood = CurioPet.Mood.SLEEPY,
                                spriteSize = 46.dp,
                                design = design
                            )
                        },
                        onClick = { onSelect(PetEditorTarget.CurledPose) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                PickerCard(
                    title = "Colors",
                    subtitle = "Palette & custom paint",
                    selected = selected is PetEditorTarget.Colors,
                    modifier = Modifier.fillMaxWidth(),
                    preview = {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            PALETTE_SLOTS.take(8).forEach { slot ->
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(hexColor(design.colorOf(slot.key)))
                                )
                            }
                        }
                    },
                    onClick = { onSelect(PetEditorTarget.Colors) }
                )
            }
            "faces" -> {
                PetFaceMoods.ALL.chunked(2).forEach { rowMoods ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMoods.forEach { mood ->
                            PickerCard(
                                title = PetFaceMoods.label(mood),
                                subtitle = "Face",
                                selected = selected is PetEditorTarget.Face && selected.mood == mood,
                                modifier = Modifier.weight(1f),
                                preview = {
                                    CurioPetSprite(
                                        stage = CurioPet.currentStage(),
                                        mood = runCatching { CurioPet.Mood.valueOf(mood) }
                                            .getOrDefault(CurioPet.Mood.HAPPY),
                                        spriteSize = 40.dp,
                                        design = design
                                    )
                                },
                                onClick = { onSelect(PetEditorTarget.Face(mood)) }
                            )
                        }
                        if (rowMoods.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            "details" -> {
                PetDesign.DETAIL_KEYS.chunked(2).forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowKeys.forEach { key ->
                            PickerCard(
                                title = key.replaceFirstChar { it.uppercase() },
                                subtitle = "Drawn layer",
                                selected = selected is PetEditorTarget.DetailLayer && selected.key == key,
                                modifier = Modifier.weight(1f),
                                preview = {
                                    MiniPixelThumb(rows = effectiveDetailRows(design, key), design = design)
                                },
                                onClick = { onSelect(PetEditorTarget.DetailLayer(key)) }
                            )
                        }
                        if (rowKeys.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            "actions" -> {
                Text(
                    "Built-in reactions",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                PetReactionEvents.ALL.chunked(2).forEach { rowEvents ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowEvents.forEach { event ->
                            PickerCard(
                                title = PetReactionEvents.label(event),
                                subtitle = "Reaction",
                                selected = selected is PetEditorTarget.Reaction && selected.event == event,
                                modifier = Modifier.weight(1f),
                                preview = {
                                    ReactionSpritePreview(
                                        design = design,
                                        reaction = design.reactionFor(event),
                                        spriteSize = 40.dp
                                    )
                                },
                                onClick = { onSelect(PetEditorTarget.Reaction(event)) }
                            )
                        }
                        if (rowEvents.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
                // v8.53 — Phase 7: user-defined custom actions.
                Text(
                    "Your custom actions",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                if (design.customActions.isEmpty()) {
                    Text(
                        "None yet — create one below. Custom actions fire on their own trigger (tap, save, a set hour…).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    design.customActions.chunked(2).forEach { rowActions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowActions.forEach { action ->
                                PickerCard(
                                    title = action.name,
                                    subtitle = action.summary(),
                                    selected = selected is PetEditorTarget.CustomAction &&
                                        selected.actionId == action.id,
                                    modifier = Modifier.weight(1f),
                                    preview = {
                                        val anim = design.animations[action.animationId]
                                            ?: animationById(action.animationId)
                                            ?: BUILTIN_ANIMATIONS.first()
                                        PetAnimationPreview(animation = anim, design = design, spriteSize = 40.dp)
                                    },
                                    onClick = { onSelect(PetEditorTarget.CustomAction(action.id)) }
                                )
                            }
                            if (rowActions.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
                PickerCard(
                    title = "＋ New custom action",
                    subtitle = "Name it, pick a trigger",
                    selected = selected is PetEditorTarget.CustomAction &&
                        selected.actionId == PetEditorTarget.NEW_CUSTOM_ACTION_ID,
                    modifier = Modifier.fillMaxWidth(),
                    preview = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurioIcon(
                                name = CurioIcons.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 22.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            CurioPetSprite(
                                stage = CurioPet.currentStage(),
                                mood = CurioPet.Mood.HAPPY,
                                spriteSize = 34.dp,
                                design = design
                            )
                        }
                    },
                    onClick = { onSelect(PetEditorTarget.CustomAction(PetEditorTarget.NEW_CUSTOM_ACTION_ID)) }
                )
            }
            else -> {
                BUILTIN_ANIMATIONS.chunked(2).forEach { rowAnims ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowAnims.forEach { anim ->
                            PickerCard(
                                title = anim.name,
                                subtitle = "${anim.frames.size} frames",
                                selected = selected is PetEditorTarget.Animation && selected.animationId == anim.id,
                                modifier = Modifier.weight(1f),
                                preview = {
                                    PetAnimationPreview(animation = anim, design = design, spriteSize = 40.dp)
                                },
                                onClick = { onSelect(PetEditorTarget.Animation(anim.id)) }
                            )
                        }
                        if (rowAnims.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SmallAction("Close") { onDismiss() }
        }
    }
}

/** v8.49 — a name-tagged preview card in the picker dialog. */
@Composable
private fun PickerCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            preview()
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** v8.49 — a tiny pixel-grid thumbnail for detail layers. */
@Composable
private fun MiniPixelThumb(rows: List<String>, design: PetDesign) {
    // Full rows — truncating to 24 would crop 32×32 detail layers.
    val cell = 3.dp
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        rows.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                line.forEach { ch ->
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .background(
                                if (ch == '.') MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                else hexColor(design.colorOf(ch))
                            )
                    )
                }
            }
        }
    }
}

/** v8.49 — circular icon transport button (play / step). */
@Composable
private fun TransportIconButton(
    icon: String,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = icon,
                contentDescription = contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 20.dp
            )
        }
    }
}



/** One HSL slider with its label. */
@Composable
private fun SliderRow(label: String, value: Float, max: Float, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp)
        )
        Slider(
            value = value.coerceIn(0f, max),
            onValueChange = onChange,
            valueRange = 0f..max
        )
    }
}

/** The import text card. */
@Composable
private fun ImportCard(
    draft: String,
    gridSize: Int,
    onCancel: () -> Unit,
    onImport: (String) -> Boolean
) {
    var text by rememberSaveable(draft) { mutableStateOf(draft) }
    var error by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.padding(18.dp)) {
        Text(
            "Paste a pet design",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "key=HEX palette lines, then $gridSize body rows and $gridSize asleep rows",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (error) CurioColors.WarmCoralRed else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it; error = false },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (error) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Couldn't read a design from that text",
                style = MaterialTheme.typography.labelSmall,
                color = CurioColors.WarmCoralRed
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallAction("Cancel", enabled = true) { onCancel() }
            SmallAction("Import", enabled = true) {
                if (onImport(text)) {
                    error = false
                } else {
                    error = true
                }
            }
        }
    }
}

/** A rounded section card with a title + subtitle. */
@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** A small rounded chip for preview moods. */
@Composable
private fun MoodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/** The Body / Asleep / size tabs. */
@Composable
private fun GridTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        onClick = onClick
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** A paint-tool chip with icon + label. */
@Composable
private fun ToolChip(tool: PaintTool, selected: Boolean, onClick: () -> Unit) {
    val icon = when (tool) {
        PaintTool.BRUSH -> CurioIcons.Brush
        PaintTool.FILL -> CurioIcons.Fill
        PaintTool.ERASER -> CurioIcons.Eraser
        PaintTool.EYEDROPPER -> CurioIcons.Colorize
    }
    val label = when (tool) {
        PaintTool.BRUSH -> "Brush"
        PaintTool.FILL -> "Fill"
        PaintTool.ERASER -> "Erase"
        PaintTool.EYEDROPPER -> "Pick"
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CurioIcon(
                name = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One palette row — swatch + name + hex; select for paint, pencil to edit. */
@Composable
private fun PaletteRow(
    slot: PaletteSlot,
    hex: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(hexColor(hex))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    slot.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Key \"${slot.key}\" · #$hex",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (selected) {
                Text(
                    "Paint",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onEdit
            ) {
                CurioIcon(
                    name = CurioIcons.Edit,
                    contentDescription = "Edit ${slot.name} color",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 16.dp,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

/** v8.46 — tells the user whether editing is armed. Picking a tool in the
 *  tray below arms editing; with no tool the canvas scrolls safely (the old
 *  draw toggle is gone — the tool tray is the edit-mode switch). A clear
 *  armed/off state prevents accidental drawing while the page scrolls. */
@Composable
private fun CanvasStatus(activeTool: PaintTool?) {
    val editing = activeTool != null
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (editing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurioIcon(
                name = if (editing) toolIcon(activeTool) else CurioIcons.Brush,
                contentDescription = null,
                tint = if (editing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 20.dp
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (editing) "Editing with ${toolLabel(activeTool)}" else "Choose a tool to edit",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    if (editing) "Tap the tool again to release it and scroll"
                    else "Pick Brush, Fill, Erase, or Pick below — the canvas stays scroll-safe until then",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** v8.46 — the tool tray: picking a tool arms editing; the helper text
 *  explains the selected tool. */
@Composable
private fun ToolTray(
    activeTool: PaintTool?,
    onSelect: (PaintTool?) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PaintTool.entries.forEach { tool ->
                ToolChip(
                    tool = tool,
                    selected = activeTool == tool,
                    onClick = { onSelect(if (activeTool == tool) null else tool) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            toolHelper(activeTool),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun toolIcon(tool: PaintTool?): String = when (tool) {
    PaintTool.BRUSH -> CurioIcons.Brush
    PaintTool.FILL -> CurioIcons.Fill
    PaintTool.ERASER -> CurioIcons.Eraser
    PaintTool.EYEDROPPER -> CurioIcons.Colorize
    null -> CurioIcons.Brush
}

private fun toolLabel(tool: PaintTool?): String = when (tool) {
    PaintTool.BRUSH -> "Brush"
    PaintTool.FILL -> "Fill"
    PaintTool.ERASER -> "Erase"
    PaintTool.EYEDROPPER -> "Pick"
    null -> "no tool"
}

private fun toolHelper(tool: PaintTool?): String = when (tool) {
    PaintTool.BRUSH -> "Drag on the grid to paint with the selected color."
    PaintTool.FILL -> "Tap a region to fill it with the selected color."
    PaintTool.ERASER -> "Drag to erase pixels back to empty."
    PaintTool.EYEDROPPER -> "Tap a pixel to pick its color, then paint with it."
    null -> "Pick a tool to start editing — with no tool, the canvas scrolls safely."
}

/** Quick body-color access docked beside the canvas; the full palette stays in Colors. */
@Composable
private fun QuickPaletteRow(
    selectedKey: Char,
    design: PetDesign,
    onSelect: (Char) -> Unit,
    onEdit: (Char) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PALETTE_SLOTS.forEach { slot ->
            Surface(
                shape = CircleShape,
                color = hexColor(design.colorOf(slot.key)),
                border = BorderStroke(
                    if (selectedKey == slot.key) 3.dp else 1.dp,
                    if (selectedKey == slot.key) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                onClick = { onSelect(slot.key) },
                modifier = Modifier.size(if (selectedKey == slot.key) 38.dp else 32.dp)
            ) {}
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            onClick = { onEdit(selectedKey) }
        ) {
            Text(
                "Edit ${selectedKey} color",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

/** A face overlay canvas uses the same protected draw mode and palette as the body editor. */
@Composable
private fun FaceGridEditor(
    design: PetDesign,
    face: com.curio.app.data.PetFace,
    tool: PaintTool?,
    blueprintRows: List<String>? = null,
    showBlueprint: Boolean = false,
    zoom: Float = 1f,
    onPaint: (Int, Int, Boolean) -> Unit
) {
    val gridSize = design.gridSize
    val latestOnPaint by rememberUpdatedState(onPaint)
    val rows = if (face.gridRows.size == gridSize) face.gridRows
    else List(gridSize) { ".".repeat(gridSize) }
    val blueprint = if (showBlueprint) blueprintRows else null
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (tool != null) 2.dp else 1.dp,
                color = if (tool != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // v8.36 — zoom + blueprint ghost (same fit-or-scroll behavior as the
        // pixel grid, so faces zoom in for easy eye/mouth editing).
        val density = LocalDensity.current
        val maxWpx = with(density) { maxWidth.toPx() }
        val cellPx = (maxWpx / gridSize) * zoom.coerceIn(1f, 3f)
        val overflows = cellPx * gridSize > maxWpx
        // v8.36 — same fit-vs-overflow pattern as PixelGrid: concurrent
        // tap + drag when the grid fits the screen, tap-only when zoomed
        // past it so horizontal scrolling is never overridden by painting.
        var gestures: Modifier = Modifier
        if (tool != null) {
            if (overflows) {
                gestures = gestures.pointerInput(gridSize, tool) {
                    detectTapGestures { offset ->
                        val (row, col) = cellAtPosition(offset, size.width, size.height, gridSize)
                        latestOnPaint(row, col, false)
                    }
                }
            } else {
                gestures = gestures
                    .pointerInput(gridSize, tool) {
                        detectTapGestures { offset ->
                            val (row, col) = cellAtPosition(offset, size.width, size.height, gridSize)
                            latestOnPaint(row, col, false)
                        }
                    }
                    .pointerInput(gridSize, tool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val (row, col) = cellAtPosition(offset, size.width, size.height, gridSize)
                                latestOnPaint(row, col, false)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val (row, col) = cellAtPosition(change.position, size.width, size.height, gridSize)
                                latestOnPaint(row, col, true)
                            }
                        )
                    }
            }
        }
        val gridContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .width(with(density) { (cellPx * gridSize).toDp() })
                    .then(gestures)
            ) {
                rows.forEachIndexed { rowIndex, line ->
                    Row(modifier = Modifier.height(with(density) { cellPx.toDp() })) {
                        line.forEachIndexed { colIndex, ch ->
                            val filled = ch != '.'
                            val blueprintKey = blueprint?.getOrNull(rowIndex)?.getOrNull(colIndex)
                            val blueprintOnly = !filled && blueprintKey != null && blueprintKey != '.'
                            Box(
                                modifier = Modifier
                                    .width(with(density) { cellPx.toDp() })
                                    .height(with(density) { cellPx.toDp() })
                                    .padding(0.5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            filled -> hexColor(design.colorOf(ch))
                                            blueprintOnly -> lerp(hexColor(design.colorOf(blueprintKey)), Color.Black, 0.35f).copy(alpha = 0.9f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
        if (overflows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                gridContent()
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                gridContent()
            }
        }
    }
}

/**
 * v8.36 — shifts a detail layer's pixels by (dr, dc); pixels pushed past
 * the edge are dropped, transparent cells fill the gaps.
 */
private fun nudgeDetailRows(rows: List<String>, dr: Int, dc: Int): List<String> {
    val n = rows.size
    val out = MutableList(n) { CharArray(n) { '.' } }
    rows.forEachIndexed { r, line ->
        line.forEachIndexed { c, ch ->
            if (ch != '.') {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until n && nc in 0 until n) out[nr][nc] = ch
            }
        }
    }
    return out.map { String(it) }
}

/**
 * Projects the visible procedural part into the editor's design grid. An
 * existing authored layer wins; otherwise the user sees the part Curie is
 * currently wearing instead of a misleading blank canvas.
 */
private fun effectiveDetailRows(design: PetDesign, layer: String): List<String> {
    val authored = design.detailFor(layer)
    if (!design.isProceduralEnabled(layer)) return authored
    // Start from the generated current part, then place authored pixels over
    // it. Transparent authored cells must remain transparent so a partially
    // redrawn layer still shows untouched generated pixels underneath.
    val pixels = when (layer) {
        "tail" -> listOf(
            Triple(14, 11, 'B'), Triple(15, 11, 'B'),
            Triple(15, 12, 'B'), Triple(15, 10, 'B')
        )
        "belly" -> listOf(
            // A soft cream belly patch on the lower torso.
            Triple(5, 10, 'B'), Triple(6, 10, 'B'), Triple(7, 10, 'B'), Triple(8, 10, 'B'), Triple(9, 10, 'B'), Triple(10, 10, 'B'),
            Triple(4, 11, 'B'), Triple(5, 11, 'B'), Triple(6, 11, 'B'), Triple(7, 11, 'B'), Triple(8, 11, 'B'), Triple(9, 11, 'B'), Triple(10, 11, 'B'), Triple(11, 11, 'B'),
            Triple(4, 12, 'B'), Triple(5, 12, 'B'), Triple(6, 12, 'B'), Triple(7, 12, 'B'), Triple(8, 12, 'B'), Triple(9, 12, 'B'), Triple(10, 12, 'B'), Triple(11, 12, 'B'),
            Triple(5, 13, 'B'), Triple(6, 13, 'B'), Triple(7, 13, 'B'), Triple(8, 13, 'B'), Triple(9, 13, 'B'), Triple(10, 13, 'B')
        )
        "accessories" -> listOf(
            Triple(2, 1, 'o'), Triple(2, 0, 'o'),
            Triple(4, 2, 's'), Triple(5, 1, 's'), Triple(5, 2, 's'), Triple(5, 3, 's'),
            Triple(13, 10, 'o'), Triple(14, 10, 's'), Triple(15, 10, 's'),
            Triple(13, 11, 'o'), Triple(14, 11, 's'), Triple(15, 11, 's'),
            Triple(14, 13, 'o'), Triple(15, 13, 'o'), Triple(14, 14, 'o'), Triple(15, 14, 'o'),
            Triple(4, 0, 'G'), Triple(8, 0, 'G'), Triple(6, 1, 'g')
        )
        "effects" -> listOf(
            Triple(1, 2, 'G'), Triple(14, 3, 'G'),
            Triple(2, 13, 'G'), Triple(13, 2, 'G'),
            Triple(1, 6, 'o'), Triple(0, 7, 'o'), Triple(1, 8, 'o'),
            Triple(14, 6, 'o'), Triple(15, 7, 'o'), Triple(14, 8, 'o')
        )
        "antenna" -> listOf(
            Triple(7, 0, 'G'), Triple(8, 0, 'G'),
            Triple(6, 1, 's'), Triple(7, 1, 's'), Triple(8, 1, 's'), Triple(9, 1, 's'),
            Triple(6, 2, 's'), Triple(7, 2, 's'), Triple(8, 2, 's'), Triple(9, 2, 's'),
            Triple(6, 3, 'S'), Triple(7, 3, 'S'), Triple(8, 3, 'S'), Triple(9, 3, 'S')
        )
        else -> emptyList()
    }
    val rows = MutableList(design.gridSize) { CharArray(design.gridSize) { '.' } }
    pixels.forEach { (col16, row16, key) ->
        val col = ((col16 + 0.5f) * design.gridSize / 16f).toInt()
            .coerceIn(0, design.gridSize - 1)
        val row = ((row16 + 0.5f) * design.gridSize / 16f).toInt()
            .coerceIn(0, design.gridSize - 1)
        rows[row][col] = key
    }
    authored.forEachIndexed { row, line ->
        line.forEachIndexed { col, key ->
            if (key != '.' && row in rows.indices && col in rows[row].indices) {
                rows[row][col] = key
            }
        }
    }
    return rows.map { String(it) }
}

/**
 * The pixel editor — tap or drag to paint with the active tool. Brush and
 * eraser paint continuously; fill and eyedropper act once per gesture.
 */
@Composable
private fun PixelGrid(
    design: PetDesign,
    grid: String,
    tool: PaintTool?,
    rowsOverride: List<String>? = null,
    blueprintRows: List<String>? = null,
    showBlueprint: Boolean = false,
    zoom: Float = 1f,
    onTool: (Int, Int, Boolean) -> Unit
) {
    val gridSize = design.gridSize
    val latestOnTool by rememberUpdatedState(onTool)
    val rows = rowsOverride ?: when {
        grid.startsWith("detail:") -> design.detailFor(grid.removePrefix("detail:"))
        grid == "curled" -> design.curledRows
        else -> design.bodyRows
    }
    val blueprint = if (showBlueprint) blueprintRows else null
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (tool != null) 2.dp else 1.dp,
                color = if (tool != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // v8.36 — zoom: cells grow so small parts (faces, details) are easy
        // to edit. When the zoomed grid still fits the screen it stays
        // centered with full tap + drag painting; when it overflows, it
        // becomes horizontally scrollable and painting switches to taps so
        // the brush never fights the scroll.
        val density = LocalDensity.current
        val maxWpx = with(density) { maxWidth.toPx() }
        val cellPx = (maxWpx / gridSize) * zoom.coerceIn(1f, 3f)
        val overflows = cellPx * gridSize > maxWpx
        // v8.36 — when the grid fits, tap + drag paint run as concurrent
        // coroutines (two pointerInput blocks); when zoomed past the screen,
        // only tap paints, so the brush never fights the horizontal scroll.
        var gestures: Modifier = Modifier
        if (tool != null) {
            if (overflows) {
                gestures = gestures.pointerInput(gridSize, tool) {
                    detectTapGestures(onTap = { offset ->
                        val (r, c) = cellAtPosition(offset, size.width, size.height, gridSize)
                        latestOnTool(r, c, false)
                    })
                }
            } else {
                gestures = gestures
                    .pointerInput(gridSize, tool) {
                        detectTapGestures(onTap = { offset ->
                            val (r, c) = cellAtPosition(offset, size.width, size.height, gridSize)
                            latestOnTool(r, c, false)
                        })
                    }
                    .pointerInput(gridSize, tool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val (r, c) = cellAtPosition(offset, size.width, size.height, gridSize)
                                latestOnTool(r, c, false)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val (r, c) = cellAtPosition(change.position, size.width, size.height, gridSize)
                                latestOnTool(r, c, true)
                            }
                        )
                    }
            }
        }
        val gridMod = Modifier
            .width(with(density) { (cellPx * gridSize).toDp() })
            .then(gestures)
        val gridContent: @Composable () -> Unit = {
            Column(modifier = gridMod) {
                rows.forEachIndexed { rowIndex, line ->
                    Row(modifier = Modifier.height(with(density) { cellPx.toDp() })) {
                        line.forEachIndexed { colIndex, ch ->
                            val filled = ch != '.'
                            val blueprintKey = blueprint?.getOrNull(rowIndex)?.getOrNull(colIndex)
                            val blueprintOnly = !filled && blueprintKey != null && blueprintKey != '.'
                            Box(
                                modifier = Modifier
                                    .width(with(density) { cellPx.toDp() })
                                    .height(with(density) { cellPx.toDp() })
                                    .padding(0.5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            filled -> hexColor(design.colorOf(ch))
                                            // v8.49 — blueprint renders as a darker, locked
                                            // reference: it never changes color when painted.
                                            blueprintOnly -> lerp(hexColor(design.colorOf(blueprintKey)), Color.Black, 0.35f).copy(alpha = 0.9f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
        if (overflows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                gridContent()
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                gridContent()
            }
        }
    }
}

/** Maps a pointer position to a (row, col) cell inside the grid. */
private fun cellAtPosition(offset: Offset, w: Int, h: Int, grid: Int): Pair<Int, Int> {
    val cellW = w.toFloat() / grid
    val cellH = h.toFloat() / grid
    val col = (offset.x / cellW).toInt().coerceIn(0, grid - 1)
    val row = (offset.y / cellH).toInt().coerceIn(0, grid - 1)
    return row to col
}

/** A horizontally scrollable row of choice chips. */
@Composable
private fun LabeledChips(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            ChoiceChip(
                label = option,
                selected = selected == option,
                onClick = { onSelect(option) }
            )
        }
    }
}

/** A compact choice chip. */
@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

/** A labeled switch row (face/reaction toggles). */
@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/** v8.38 — a one-tap personality preset card (Shy / Party / Sleepyhead). */
@Composable
private fun RowScope.PresetCard(
    name: String,
    tagline: String,
    preview: PetDesign,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        onClick = onClick,
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurioPetSprite(
                stage = CurioPet.currentStage(),
                mood = CurioPet.Mood.HAPPY,
                spriteSize = 44.dp,
                design = preview
            )
            Spacer(Modifier.height(6.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(2.dp))
            Text(
                tagline,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A compact action pill. */
@Composable
private fun SmallAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick,
        enabled = enabled
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}


/** Detailed mechanical preset: panel seams, visor, and articulated feet. */
private val ROBOT_BODY: List<String> = listOf(
    "......GGGG......",
    ".....oGGGGo.....",
    "....ooGGGGoo....",
    "...oBBBBBBBBo...",
    "..oBbbbbbbbbBo..",
    ".oBbbbbbbbbbbBo.",
    "oBbbbbbbbbbbbbBo",
    "oBbbCbbbbCbbbbBo",
    "oBbbCCCCCCCCbbBo",
    "oBbbCbbbbCbbbbBo",
    "oBbbbbbbbbbbbbBo",
    ".oBbbbbbbbbbbBo.",
    "..oSSssssssSSo..",
    "..oo..oooo..oo..",
    "..oo..oooo..oo..",
    "................"
)

/** Detailed ghost preset: soft windows, spectral bands, and a scalloped hem. */
private val GHOST_BODY: List<String> = listOf(
    "......GGGG......",
    ".....oGGGGo.....",
    "....ooGGGGoo....",
    "...oBBBBBBBBo...",
    "..oBbbbbbbbbBo..",
    ".oBbbbbbbbbbbBo.",
    "oBbbbbbbbbbbbbBo",
    "oBbbbbbbbbbbbbBo",
    "oBbbbDbbbbDbbbBo",
    "oBbbbbbbbbbbbbBo",
    "oBbbbbbbbbbbbbBo",
    ".oBbbbbbbbbbbBo.",
    "..oSSssssssSSo..",
    "..oooooooooooo..",
    "..oo..oo..oo..o.",
    "...oo......oo..."
)

/** Robot sleep pose: compact head, visor, and folded mechanical feet. */
private val ROBOT_CURLED: List<String> = listOf(
    "......GGGG......",
    ".....oGGGGo.....",
    "....oBBBBBo.....",
    "...oBbbbbBbo....",
    "..oBbbbbbbbBo...",
    ".oBbbCCCCbbBo...",
    "oBbbbbbbbbbbBo..",
    "oBbbbbbbbbbbBo..",
    ".oBbbbbbbbbBo...",
    "..oSSssssSSo....",
    "...oBBBBBBo.....",
    "....oBBBBBo.....",
    ".....oooo.......",
    "................",
    "................",
    "................"
).map { it.padEnd(16, '.') }

/** Ghost sleep pose: a hovering, scalloped spectral curl with a soft window. */
private val GHOST_CURLED: List<String> = listOf(
    "......GGGG......",
    ".....oGGGGo.....",
    "....oBBBBBo.....",
    "...oBbbbbBbo....",
    "..oBbbbbbbbBo...",
    ".oBbbbDDbbbBo...",
    "oBbbbbbbbbbbBo..",
    "oBbbbbbbbbbbBo..",
    ".oBbbbbbbbbBo...",
    "..oSSssssSSo....",
    "...oooooooo.....",
    "....oo..oo......",
    "...oo....oo.....",
    "................",
    "................",
    "................"
).map { it.padEnd(16, '.') }

// ═══════════════════════════════════════════════════════════════════════════
// Actions page (Phase 5) — action cards, live preview, speech bubble
// ═══════════════════════════════════════════════════════════════════════════

/** v8.50 — looping reaction preview. Maps the reaction's animation onto the
 *  sprite's one-shot keys and re-triggers them on a timer, so both the
 *  landing card and the editor stage play the move live. A disabled reaction
 *  renders still (and the caller dims it). */
@Composable
private fun ReactionSpritePreview(
    design: PetDesign,
    reaction: PetReaction,
    spriteSize: Dp,
    replayKey: Int = 0,
    modifier: Modifier = Modifier
) {
    var loop by remember(reaction.anim) { mutableStateOf(0) }
    // Loop period tuned to each move's one-shot duration so the preview
    // re-triggers smoothly (a fixed 800ms would chop the 360° spin or leave
    // a dead gap after the short squish).
    val loopPeriod = when (reaction.anim) {
        ReactionAnim.SPIN -> 1000L
        ReactionAnim.HOP -> 700L
        ReactionAnim.BOUNCE -> 760L
        ReactionAnim.SQUISH -> 600L
        ReactionAnim.NONE -> 0L
    }
    LaunchedEffect(reaction.anim, reaction.enabled, replayKey) {
        if (!reaction.enabled || loopPeriod <= 0L) return@LaunchedEffect
        while (true) {
            delay(loopPeriod)
            loop++
        }
    }
    // Gate the keys on enabled too: a disabled reaction must not play a
    // one-shot even when the editor's Replay bumps replayKey (the sprite's
    // internal LaunchedEffect fires on any key > 0).
    val key = if (reaction.enabled) loop + replayKey else 0
    val anim = reaction.anim
    CurioPetSprite(
        stage = CurioPet.currentStage(),
        mood = CurioPet.Mood.HAPPY,
        spriteSize = spriteSize,
        design = design,
        faceOverride = reaction.face,
        celebrateKey = if (anim == ReactionAnim.HOP) key else 0,
        playKey = if (anim == ReactionAnim.BOUNCE) key else 0,
        spinKey = if (anim == ReactionAnim.SPIN) key else 0,
        squishKey = if (anim == ReactionAnim.SQUISH) key else 0,
        contentDescription = "Reaction preview",
        modifier = modifier
    )
}

/** v8.50 — live action preview inside the reaction editor: the pet plays the
 *  reaction's animation wearing its face, over a speech bubble. Replay
 *  re-triggers the move and cycles to the next dialogue line. */
@Composable
private fun ActionPreview(
    event: String,
    design: PetDesign,
    reaction: PetReaction,
    modifier: Modifier = Modifier
) {
    var replayKey by rememberSaveable(event) { mutableStateOf(0) }
    var lineIndex by rememberSaveable(event) { mutableStateOf(0) }
    val lines = reaction.lines.ifEmpty { listOf(PetReactionEvents.defaultLine(event)) }
    val shown = lines[lineIndex % lines.size]
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Live preview",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    "\u201c$shown\u201d",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            ReactionSpritePreview(
                design = design,
                reaction = reaction,
                spriteSize = 84.dp,
                replayKey = replayKey,
                modifier = Modifier.alpha(if (reaction.enabled) 1f else 0.5f)
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallAction("Replay", enabled = true) {
                    replayKey++
                    lineIndex = (lineIndex + 1) % lines.size
                }
                Text(
                    "${reaction.anim.name} · ${if (reaction.enabled) "enabled" else "disabled"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * v8.53 — Phase 7: the custom action editor. Name + trigger (+ param for
 * time/idle triggers) + animation + dialogue lines + enabled, with a live
 * preview wearing the chosen animation and a speech bubble. Duplicate and
 * Delete live here too — the only place custom actions are edited.
 */
@Composable
private fun CustomActionEditor(
    action: CustomPetAction,
    design: PetDesign,
    lineDraft: String,
    nameDraft: String,
    onNameChange: (String) -> Unit,
    onLineDraftChange: (String) -> Unit,
    onUpdate: (CustomPetAction) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val anim = design.animations[action.animationId]
        ?: animationById(action.animationId)
        ?: BUILTIN_ANIMATIONS.first()
    var replayKey by rememberSaveable(action.id) { mutableStateOf(0) }
    val bubble = action.dialogueLines.firstOrNull() ?: "${anim.name}!"
    SectionCard(
        "Custom action",
        "A behavior you made — it fires on its own trigger, wherever your pet is"
    ) {
        Text(
            "Action name",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (nameDraft.isEmpty()) {
                    Text(
                        "Name this action… e.g. “Victory dance”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
                BasicTextField(
                    value = nameDraft,
                    onValueChange = onNameChange,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "When does it fire?",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "The floating pet runs this action when its trigger happens in the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PetActionTrigger.ALL.forEach { kind ->
                ChoiceChip(
                    label = PetActionTrigger.label(kind),
                    selected = action.trigger.kind == kind,
                    onClick = { onUpdate(action.copy(trigger = PetActionTrigger(kind, action.trigger.param))) }
                )
            }
        }
        // v8.53 — trigger-specific tuning: which hour for `time`, how long
        // untouched for `idle`.
        when (action.trigger.kind) {
            PetActionTrigger.TIME -> {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(7 to "7 AM", 9 to "9 AM", 12 to "12 PM", 15 to "3 PM", 18 to "6 PM", 21 to "9 PM")
                        .forEach { (hour, label) ->
                            ChoiceChip(
                                label = label,
                                selected = action.trigger.param == hour,
                                onClick = { onUpdate(action.copy(trigger = PetActionTrigger(PetActionTrigger.TIME, hour))) }
                            )
                        }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Runs when the clock hits the chosen hour — a morning hello or a goodnight.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PetActionTrigger.IDLE -> {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(30 to "30 s", 60 to "1 min", 120 to "2 min", 300 to "5 min")
                        .forEach { (seconds, label) ->
                            ChoiceChip(
                                label = label,
                                selected = action.trigger.param == seconds,
                                onClick = { onUpdate(action.copy(trigger = PetActionTrigger(PetActionTrigger.IDLE, seconds))) }
                            )
                        }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Runs when you have not touched the pet for a while — a gentle nudge.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> Unit
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Animation",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Which move the pet does — pick any animation, including ones you drew in the timeline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        val animationOptions = BUILTIN_ANIMATIONS.map { it.id to it.name } +
            design.animations.keys.filter { animationById(it) == null }.map { it to petAnimationName(it) }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            animationOptions.forEach { (id, name) ->
                ChoiceChip(
                    label = name,
                    selected = action.animationId == id,
                    onClick = { onUpdate(action.copy(animationId = id)) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Live preview — the chosen animation looping with the speech bubble.
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Live preview",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        "\u201c$bubble\u201d",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                // key(replayKey) remounts the preview so Replay restarts the
                // loop from frame one (a plain recomposition would not).
                key(replayKey) {
                    PetAnimationPreview(
                        animation = anim,
                        design = design,
                        spriteSize = 96.dp,
                        playing = true
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallAction("Replay", enabled = true) { replayKey++ }
                    Text(
                        "${anim.name} · ${PetActionTrigger.label(action.trigger.kind)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "What does it say? (optional)",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "One line per row — the pet says one of these when the action fires. Leave empty to stay quiet and just do the move.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                if (action.dialogueLines.isEmpty()) {
                    Text(
                        "Wave, hi!\nOver here!\nTa-da!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
                BasicTextField(
                    value = lineDraft,
                    onValueChange = onLineDraftChange,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${action.dialogueLines.size}/8 lines · 120 characters per line",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        ToggleRow(
            "Action enabled",
            action.enabled
        ) {
            onUpdate(action.copy(enabled = it))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction("Duplicate", enabled = true) { onDuplicate() }
            SmallAction("Delete action", enabled = true) { onDelete() }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Pet library (Phase 6) — registry-driven pet cards
// ═══════════════════════════════════════════════════════════════════════════

/** v8.51 — one pet card in the Settings "Pet library". Renders the pet's
 *  default look and marks whether the current design belongs to it. */
/**
 * v8.56 — one custom-pet slot card on the Pets page. Empty slots are the
 * "Save as new pet" affordance; filled slots show the saved design, a
 * "Your pet" badge when active, and a tiny ✕ delete button.
 */
@Composable
private fun CustomPetCard(
    slot: Int,
    designText: String?,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (designText == null) {
        // Empty slot → save-as-new-pet card.
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            onClick = onClick,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 26.dp,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                )
                Text(
                    "Save as\nnew pet",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
        return
    }
    val parsed = remember(designText) { PetDesign.DEFAULT.toParsedOr(designText, PetDesign.DEFAULT) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        onClick = onClick,
        modifier = modifier
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // v8.56 follow-up — the card plays the pet's idle animation
                // on loop instead of a static sprite (PetAnimationPreview
                // resolves the design's custom frames automatically).
                PetAnimationPreview(
                    animation = PET_CARD_ANIMATION,
                    design = parsed,
                    spriteSize = 56.dp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Custom ${slot + 1}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Your saved look",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        if (active) "Your pet" else "Not selected",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            // Tiny delete affordance (top-right corner).
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CurioIcon(
                        name = CurioIcons.Close,
                        contentDescription = "Delete custom pet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 13.dp
                    )
                }
            }
        }
    }
}

/**
 * v8.56 — the Editor page's single picker trigger (shown while no target is
 * chosen). The editor is the center of the screen: this is the ONLY
 * affordance until the user picks what to edit.
 */
@Composable
private fun EditorPickPrompt(onOpenPicker: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        onClick = onOpenPicker,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurioIcon(
                name = CurioIcons.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                size = 30.dp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "What do you want to edit?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick something and it opens right here — one editor at a time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Choose",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

/** v8.56 — the "Editing — {title}" header with a Change chip. */
@Composable
private fun EditorTargetHeader(
    target: PetEditorTarget,
    onChange: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = targetIcon(target),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    size = 18.dp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Editing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    target.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StripChip("Change", selected = false) { onChange() }
        }
    }
}

/** Small glyph for the Editing header. */
private fun targetIcon(target: PetEditorTarget): String = when (target) {
    is PetEditorTarget.Body, is PetEditorTarget.CurledPose -> CurioIcons.Brush
    is PetEditorTarget.Colors -> CurioIcons.Palette
    is PetEditorTarget.DetailLayer -> CurioIcons.Layers
    is PetEditorTarget.Face -> CurioIcons.Star
    is PetEditorTarget.Reaction -> CurioIcons.AutoAwesome
    is PetEditorTarget.CustomAction -> CurioIcons.Add
    is PetEditorTarget.Animation -> CurioIcons.PlayArrow
}

/**
 * v8.56 — full-screen animation player: big looping preview with play/pause
 * + frame stepping, and an "Edit frames" jump into the editor. Replaces the
 * old dead-tap gallery behavior (the target used to be set on the wrong
 * page, so tapping an animation did nothing).
 */
@Composable
private fun AnimationPlayerDialog(
    animation: PetAnimation,
    design: PetDesign,
    onEditFrames: () -> Unit,
    onDismiss: () -> Unit
) {
    // Resolve the design's custom frames (an absent key = built-in frames).
    val effective = design.animations[animation.id] ?: animation
    var playing by remember(animation.id) { mutableStateOf(true) }
    var frameIndex by remember(animation.id) { mutableStateOf(0) }
    LaunchedEffect(animation.id, playing, effective.frames) {
        if (!playing || effective.frames.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(effective.frames[frameIndex].durationMs.toLong())
            frameIndex = (frameIndex + 1) % effective.frames.size
        }
    }
    val frame = effective.frames.getOrNull(frameIndex) ?: PetAnimationFrame()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: animation name + close.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    animation.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    onClick = onDismiss
                ) {
                    CurioIcon(
                        name = CurioIcons.Close,
                        contentDescription = "Close player",
                        tint = Color.White,
                        size = 20.dp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // Big preview on a soft glass card.
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedPetSprite(
                    animation = effective,
                    frame = frame,
                    design = design,
                    spriteSize = 130.dp,
                    ghost = false,
                    staticPose = !playing
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "${effective.frames.size} frames · loops",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(22.dp))
            // Transport: step back / play-pause / step forward.
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportIconButton(
                    CurioIcons.ChevronLeft,
                    "Previous frame",
                    enabled = effective.frames.isNotEmpty()
                ) {
                    frameIndex = (frameIndex - 1 + effective.frames.size) % effective.frames.size
                    playing = false
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { playing = !playing },
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CurioIcon(
                            name = if (playing) CurioIcons.Pause else CurioIcons.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            size = 30.dp
                        )
                    }
                }
                TransportIconButton(
                    CurioIcons.ChevronRight,
                    "Next frame",
                    enabled = effective.frames.isNotEmpty()
                ) {
                    frameIndex = (frameIndex + 1) % effective.frames.size
                    playing = false
                }
            }
            Spacer(Modifier.weight(1f))
            // Edit frames → the editor.
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                onClick = onEditFrames,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    "Edit frames",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun PetLibraryCard(
    pet: PetDefinition,
    // v8.56 follow-up — the design to animate (the card shows the pet's
    // real current look, not a static default).
    design: PetDesign = pet.defaultDesign,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (current) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // v8.56 follow-up — the card plays the pet's idle animation on
            // loop instead of a static sprite.
            PetAnimationPreview(
                animation = PET_CARD_ANIMATION,
                design = design,
                spriteSize = 56.dp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                pet.displayName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                pet.tagline,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (current) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    if (current) "Your pet" else "Not selected",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// v9.3 — Flower bed editor (Settings → Home → Edit flower bed)
// ═══════════════════════════════════════════════════════════════════════════

/** Bed palette: paint key → display color (matches CurioFlowerBed's palette). */
private val BED_PALETTE: Map<Char, Color> = mapOf(
    '.' to Color.Transparent,
    'w' to Color(0xFFB98A5E),
    'l' to Color(0xFF8A5A33),
    'm' to Color(0xFFFFF6E6),
    'M' to Color(0xFFF0E4CE),
    'F' to Color(0xFFF7B8D0),
    'f' to Color(0xFFE89AB8),
    'g' to Color(0xFFFFD97D),
    'k' to Color(0xFFF2A6B3),
    'K' to Color(0xFFDC8A99),
    'G' to Color(0xFF9CCB8B),
    'D' to Color(0xFF7FB56F)
)

private val BED_PAINT_KEYS = listOf('w', 'l', 'm', 'M', 'F', 'f', 'g', 'k', 'K', 'G', 'D')

/** Preset bed designs the user can pick from. */
private data class BedPreset(val name: String, val rows: List<String>)

private val COZY_BED_ROWS: List<String> = listOf(
    "............wwwwwwww............",
    "..........wwwwwwwwwwww..........",
    "..........wwwwwllwwllwwl.........",
    "..........wwwwwllwwllwwl.........",
    "......wwwwwwwwwwwwwwwwwwwwww....",
    ".......wmmFgFmkkkkFgFmmFgFmmw...",
    ".......wmmFfFmKKKKFfFmmFfFmmw...",
    "......wmmmmmmmmmmmmmmmmmmmmmmw..",
    "......wmmmmmkkkkkkkKKKkkkkkmw..",
    "......wmmmmmKKKKKKKkkKKKKKkmw..",
    "......wmmmmmkkkkkkkKKKkkkkkmw..",
    "......wmmmmmmmmmmmmmmmmmmmmmmw..",
    ".....wwwwwwwwwwwwwwwwwwwwww.....",
    "....wwwwwwwwwwwwwwwwwwwwwwww....",
    "..GGGGGGGGGGGGGGGGGGGGGGGGGGGG..",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    ".D.D..DD..D.D..DD..D.D..DD..D.D.",
    "................................"
)

private val MINIMAL_BED_ROWS: List<String> = listOf(
    "................................",
    "................................",
    "................................",
    "................................",
    "................................",
    "................................",
    "................................",
    "......wmmmmmmmmmmmmmmmmmmw......",
    "......wmmmmmmmmmmmmmmmmmmw......",
    "......wmmmmmmkkkkkkkmmmmmw......",
    "......wmmmmmmKKKKKKKmmmmmw......",
    "......wmmmmmmmmmmmmmmmmmmw......",
    ".....wwwwwwwwwwwwwwwwwwwwww.....",
    "....wwwwwwwwwwwwwwwwwwwwwwww....",
    "..GGGGGGGGGGGGGGGGGGGGGGGGGGGG..",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    ".D.D..DD..D.D..DD..D.D..DD..D.D.",
    "................................"
)

private val GARDEN_BED_ROWS: List<String> = listOf(
    "............wwwwwwww............",
    "..........wwwwwwwwwwww..........",
    "..........wwwwwllwwllwwl.........",
    "..........wwwwwllwwllwwl.........",
    "......wwwwwwwwwwwwwwwwwwwwww....",
    ".......wmmFgFmFgFmFgFmmFgFmmw...",
    ".......wmmFfFmFfFmFfFmmFfFmmw...",
    "......wmmmmmmmmmmmmmmmmmmmmmmw..",
    "......wmmmmmkkkkkkkkkkkkkkkkmw..",
    "......wmmmmmKKKKKKKKKKKKKKKKmw..",
    "......wmmmmmkkkkkkkkkkkkkkkkmw..",
    "......wmmmmmmmmmmmmmmmmmmmmmmw..",
    ".....wwwwwwwwwwwwwwwwwwwwww.....",
    "....GGGwwwwwwwwwGGGGGGGwwwww....",
    "..GGGGGGGGGGGGGGGGGGGGGGGGGGGG..",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    ".D.D..DD..D.D..DD..D.D..DD..D.D.",
    "................................"
)

private val BED_PRESETS = listOf(
    BedPreset("Default", BED_ROWS),
    BedPreset("Cozy night", COZY_BED_ROWS),
    BedPreset("Minimal", MINIMAL_BED_ROWS),
    BedPreset("Garden", GARDEN_BED_ROWS)
)

@Composable
private fun BedDesignDialog(onDismiss: () -> Unit, context: android.content.Context) {
    val savedRows = AppPreferences.bedDesignRowsState
    var rows by remember(savedRows) { mutableStateOf(savedRows?.toList() ?: BED_ROWS) }
    var paintKey by rememberSaveable { mutableStateOf('w') }
    var selectedPreset by rememberSaveable { mutableStateOf("Custom") }
    DialogScrim(onDismiss = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Edit flower bed",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Change the look of Curie's home — pick a preset or paint the 32×18 grid yourself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Preset chips
                Text("Preset", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BED_PRESETS.forEach { preset ->
                        ChoiceChip(
                            label = preset.name,
                            selected = selectedPreset == preset.name,
                            onClick = {
                                selectedPreset = preset.name
                                rows = preset.rows.toList()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Palette swatches
                Text("Paint color", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BED_PAINT_KEYS.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BED_PALETTE[key] ?: Color.Transparent)
                                .then(
                                    if (paintKey == key) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                .clickable {
                                    selectedPreset = "Custom"
                                    paintKey = key
                                }
                        )
                    }
                    // Eraser
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (paintKey == '.') Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                            .clickable { selectedPreset = "Custom"; paintKey = '.' }
                    )
                }
                Spacer(Modifier.height(10.dp))

                // Pixel grid (32 × 18, compact cells)
                Text("Grid (tap to paint)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(6.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                ) {
                    val cellDp = maxWidth / 32f
                    Column(modifier = Modifier.width(maxWidth)) {
                        rows.forEachIndexed { ri, line ->
                            Row(modifier = Modifier.height(cellDp)) {
                                line.forEachIndexed { ci, ch ->
                                    val bg = BED_PALETTE[ch] ?: Color.Transparent
                                    Box(
                                        modifier = Modifier
                                            .width(cellDp)
                                            .height(cellDp)
                                            .padding(0.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (ch == '.') MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else bg)
                                            .clickable {
                                                selectedPreset = "Custom"
                                                val newRows = rows.toMutableList()
                                                val newLine = newRows[ri].toCharArray()
                                                newLine[ci] = paintKey
                                                newRows[ri] = String(newLine)
                                                rows = newRows
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallAction("Cancel", enabled = true) { onDismiss() }
                    Spacer(Modifier.weight(1f))
                    SmallAction("Reset", enabled = rows != BED_ROWS) {
                        AppPreferences.clearBedDesignRows(context)
                        rows = BED_ROWS.toList()
                        selectedPreset = "Default"
                    }
                    SmallAction("Save", enabled = rows != BED_ROWS || savedRows != null) {
                        AppPreferences.setBedDesignRows(context, rows)
                        onDismiss()
                    }
                }
            }
        }
    }
}

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import com.curio.app.data.PetDesign
import com.curio.app.data.PetFaceMoods
import com.curio.app.data.PetFacePresets
import com.curio.app.data.PetReaction
import com.curio.app.data.PetReactionEvents
import com.curio.app.data.BUILTIN_ANIMATIONS
import com.curio.app.data.PetAnimation
import com.curio.app.data.PetAnimationFrame
import com.curio.app.data.ReactionAnim
import com.curio.app.data.animationById
import kotlinx.coroutines.delay
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.pet.CurioPetSprite
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
    // Working copy — starts from the saved design (or default), edited live.
    var design by remember(savedText) {
        mutableStateOf(
            savedText?.let { PetDesign.DEFAULT.toParsedOr(it, PetDesign.DEFAULT) } ?: PetDesign.DEFAULT
        )
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
    var page by rememberSaveable { mutableStateOf(PetDesignerPage.ANIMATIONS) }
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
    var reactEvent by rememberSaveable { mutableStateOf(PetReactionEvents.TOUCH) }
    // Keep raw editor text separate from the normalized persisted lines so
    // typing does not trim the field or jump the cursor on every keystroke.
    var reactionLineDraft by remember(savedText) {
        mutableStateOf(
            design.reactionFor(PetReactionEvents.TOUCH).lines.joinToString("\n")
        )
    }
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

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack = redoStack + design
        design = undoStack.last()
        undoStack = undoStack.dropLast(1)
        resetDetailEditor()
        reactionLineDraft = design.reactionFor(reactEvent).lines.joinToString("\n")
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack = undoStack + design
        design = redoStack.last()
        redoStack = redoStack.dropLast(1)
        resetDetailEditor()
        reactionLineDraft = design.reactionFor(reactEvent).lines.joinToString("\n")
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
            PetEditorTarget.CurledPose -> editingGrid = "curled"
            PetEditorTarget.Body -> editingGrid = "body"
            PetEditorTarget.Colors -> Unit
            PetEditorTarget.Animation -> Unit
        }
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
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CurioSectionLabel("Pet designer") }
            item {
                // v8.45 — the local Pet Designer navbar (separate from the
                // app-wide nav). Switching pages resets the editor target so
                // every page lands on its picker first.
                PetDesignerNavbar(page = page, onSelect = { newPage ->
                    page = newPage
                    target = null
                })
            }
            // ── Choose what to edit (the universal editor target) ────
            item {
                TargetPicker(
                    page = page,
                    selected = target,
                    onSelect = ::selectTarget
                )
            }
            // ── One-tap personality presets (Actions page landing) ────
            item {
                if (page == PetDesignerPage.ACTIONS && target == null) SectionCard(
                    "One-tap presets",
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

            // ── Live preview (Animations page landing) ───────────────
            item {
                if (page == PetDesignerPage.ANIMATIONS && target == null) SectionCard("Live preview", if (design.isCustom) "Your custom look" else "The default look — make it yours!") {
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

            // ── Animation gallery (Animations page landing, v8.48) ──
            item {
                if (page == PetDesignerPage.ANIMATIONS && target == null) SectionCard(
                    "Animations",
                    "Every card plays a looping preview — tap one to open its frame timeline"
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
                                    onClick = { selectTarget(PetEditorTarget.Animation(anim.id)) },
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

            // ── Animation timeline editor (Animations target, v8.48) ──
            item {
                val animTarget = target as? PetEditorTarget.Animation
                if (animTarget != null) AnimationTimelineEditor(
                    animationId = animTarget.animationId,
                    design = design,
                    onDesignChange = { design = it }
                )
            }

            // ── Body / curled pose pixel editor (Animations targets) ──
            item {
                if (target == PetEditorTarget.Body || target == PetEditorTarget.CurledPose) SectionCard(
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

            // ── Drawable details editor (Animations target) ──────────
            item {
                if (target is PetEditorTarget.DetailLayer) SectionCard(
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
                    Text(
                        "Procedural elements",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        "Turn off only the generated part you want to replace with your drawing. The base antenna pixels remain editable on the Body canvas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    PetDesign.PROCEDURAL_KEYS.forEach { element ->
                        ToggleRow(
                            label = if (element == "antenna") "Generated antenna extras" else "Generated ${element.replaceFirstChar { it.uppercase() }}",
                            checked = design.isProceduralEnabled(element),
                            onCheckedChange = { enabled ->
                                pushUndo()
                                resetDetailEditor()
                                design = design.withProceduralEnabled(element, enabled)
                            }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
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

            // ── Palette editor (Animations target) ───────────────────
            item {
                if (target == PetEditorTarget.Colors) SectionCard(
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

            // ── Face editor (Animations page target) ──────────────────
            item {
                if (target is PetEditorTarget.Face) SectionCard(
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
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Draw this face directly — transparent cells leave Curie's normal body visible.",
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
                        face = face,
                        tool = activeTool,
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
                    Text(
                        "Procedural fallback: ${face.eyes.name} eyes · ${face.mouth.name} mouth · blush ${if (face.blush) "on" else "off"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )


                }
            }

            // ── Reaction editor (Actions page target) ────────────────
            item {
                if (target is PetEditorTarget.Reaction) SectionCard(
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
                            design = PetDesign.DEFAULT
                            reactionLineDraft = PetDesign.DEFAULT.reactionFor(reactEvent).lines.joinToString("\n")
                        }
                    }
                }
            }

            // ── Import / export (Settings page) ──────────────────────
            item {
                if (page == PetDesignerPage.SETTINGS) SectionCard(
                    "Import & export",
                    "Share Curie as a PNG image (pixel-perfect), or use the text format for fine control"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            onClick = {
                                val exportMood = previewMood.name
                                val exportGrid = if (previewMood == CurioPet.Mood.SLEEPY) "curled" else "body"
                                val uri = exportPngUri(context, design, exportGrid, exportMood, CurioPet.currentStage())
                                if (uri != null) sharePng(context, uri) else toast = "Couldn't render PNG"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Wallpaper,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    size = 22.dp
                                )
                                Text(
                                    "Export PNG",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Preview pose · ${design.gridSize}×${design.gridSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            onClick = {
                                importPngTarget = if (editingGrid == "curled") 2 else 1
                                pngPicker.launch("image/*")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    size = 22.dp
                                )
                                Text(
                                    "Import PNG",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Sample image colors before importing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Text format (advanced): copy the design as text, paste it in, edit colors by hand, or share it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("Copy design text") {
                            clipboard.setText(AnnotatedString(design.toText()))
                            toast = "Copied to clipboard"
                        }
                        SmallAction("Paste design text") {
                            importDraft = clipboard.getText()?.text ?: ""
                        }
                    }
                }
            }
        }
            // ── Save area — always visible below the editor ──────────
            SaveArea(
                design = design,
                toast = toast,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                onUndo = { undo() },
                onRedo = { redo() },
                onSave = {
                    if (design.isCustom) {
                        AppPreferences.setPetDesign(context, design.toText())
                        toast = "Saved — Curie wears it everywhere"
                    } else {
                        AppPreferences.clearPetDesign(context)
                        toast = "Default look restored"
                    }
                },
                onReset = {
                    pushUndo()
                    resetDetailEditor()
                    design = PetDesign.DEFAULT
                    reactionLineDraft = PetDesign.DEFAULT.reactionFor(reactEvent).lines.joinToString("\n")
                }
            )
        }

        SettingsHeroHeader(
            title = "Pet designer",
            subtitle = "Draw your own Curie",
            onBack = { navController.popBackStack() },
            compact = wide
        )

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
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// v8.45 — Universal editor shell: local navbar + target picker + save area
// ═══════════════════════════════════════════════════════════════════════════

/** The local Pet Designer navbar (Animations / Actions / Settings). */
@Composable
private fun PetDesignerNavbar(
    page: PetDesignerPage,
    onSelect: (PetDesignerPage) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PetDesignerPage.entries.forEach { p ->
            val selected = p == page
            Surface(
                onClick = { onSelect(p) },
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (selected) 2.dp else 0.dp,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (p) {
                        PetDesignerPage.ANIMATIONS -> "Animations"
                        PetDesignerPage.ACTIONS -> "Actions"
                        PetDesignerPage.SETTINGS -> "Settings"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                    ),
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

/** The per-page "choose what to edit" picker (renders nothing on Settings). */
@Composable
private fun TargetPicker(
    page: PetDesignerPage,
    selected: PetEditorTarget?,
    onSelect: (PetEditorTarget) -> Unit
) {
    if (page == PetDesignerPage.SETTINGS) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CurioSectionLabel(
            if (page == PetDesignerPage.ANIMATIONS) "Choose what to edit" else "Choose a reaction to edit"
        )
        if (page == PetDesignerPage.ANIMATIONS) {
            TargetChipRow(
                label = "Animations",
                targets = BUILTIN_ANIMATIONS.map { PetEditorTarget.Animation(it.id) },
                selected = selected,
                onSelect = onSelect
            )
            TargetChipRow(
                label = "Body & pose",
                targets = listOf(PetEditorTarget.Body, PetEditorTarget.CurledPose, PetEditorTarget.Colors),
                selected = selected,
                onSelect = onSelect
            )
            TargetChipRow(
                label = "Detail layers",
                targets = PetDesign.DETAIL_KEYS.map { PetEditorTarget.DetailLayer(it) },
                selected = selected,
                onSelect = onSelect
            )
            TargetChipRow(
                label = "Faces",
                targets = PetFaceMoods.ALL.map { PetEditorTarget.Face(it) },
                selected = selected,
                onSelect = onSelect
            )
        } else {
            TargetChipRow(
                label = "Reactions",
                targets = PetReactionEvents.ALL.map { PetEditorTarget.Reaction(it) },
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

/** One labelled row of target chips in the picker. */
@Composable
private fun TargetChipRow(
    label: String,
    targets: List<PetEditorTarget>,
    selected: PetEditorTarget?,
    onSelect: (PetEditorTarget) -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            targets.forEach { t ->
                TargetChip(
                    target = t,
                    selected = t.id == selected?.id,
                    onClick = { onSelect(t) }
                )
            }
        }
    }
}

/** One tappable target chip (selected state in the container color). */
@Composable
private fun TargetChip(
    target: PetEditorTarget,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Text(
            text = target.title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * The always-visible save area pinned below the editor (Phase 1): the
 * primary Save button plus Reset changes and the transient toast.
 */
@Composable
private fun SaveArea(
    design: PetDesign,
    toast: String?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SaveButton(
                label = if (design.isCustom) "Save custom design" else "Use default look",
                onClick = onSave
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallAction("Undo", canUndo) { onUndo() }
                SmallAction("Redo", canRedo) { onRedo() }
                SmallAction("Reset changes", design.isCustom) { onReset() }
            }
            if (toast != null) {
                Text(
                    toast,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
            ColorPreviewColumn("Original", initialHex, accent = false)
            ColorPreviewColumn("New", hexDraft, accent = !hexError && hexDraft != initialHex)
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
private fun ColorPreviewColumn(label: String, hex: String, accent: Boolean) {
    Column(
        modifier = Modifier.weight(1f),
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
    var frameIndex by remember(animation.id) { mutableStateOf(0) }
    LaunchedEffect(animation.id, playing) {
        if (!playing || animation.frames.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(animation.frames[frameIndex].durationMs.toLong())
            frameIndex = (frameIndex + 1) % animation.frames.size
        }
    }
    val frame = animation.frames.getOrNull(frameIndex) ?: PetAnimationFrame()
    AnimatedPetSprite(
        animation = animation,
        frame = frame,
        design = design,
        spriteSize = spriteSize,
        ghost = false
    )
}

/** v8.48 — renders the pet at one animation frame (transform + ghost alpha). */
@Composable
private fun AnimatedPetSprite(
    animation: PetAnimation,
    frame: PetAnimationFrame,
    design: PetDesign,
    spriteSize: Dp,
    ghost: Boolean
) {
    CurioPetSprite(
        stage = CurioPet.currentStage(),
        mood = runCatching { CurioPet.Mood.valueOf(animation.mood) }.getOrDefault(CurioPet.Mood.HAPPY),
        spriteSize = spriteSize,
        design = design,
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
    onDesignChange: (PetDesign) -> Unit
) {
    val base = animationById(animationId) ?: return
    var playing by rememberSaveable(animationId) { mutableStateOf(true) }
    var selectedFrame by rememberSaveable(animationId) { mutableStateOf(0) }
    var onionSkin by rememberSaveable { mutableStateOf(false) }
    var frames by remember(animationId) {
        mutableStateOf(design.animations[animationId]?.frames ?: base.frames)
    }
    LaunchedEffect(animationId, playing, frames) {
        if (!playing || frames.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(frames.getOrNull(selectedFrame)?.durationMs?.toLong() ?: 180L)
            selectedFrame = (selectedFrame + 1) % frames.size
        }
    }
    val shown = frames.getOrNull(selectedFrame) ?: PetAnimationFrame()
    fun commit(updated: List<PetAnimationFrame>) {
        frames = updated
        onDesignChange(
            design.copy(animations = design.animations + (animationId to base.copy(frames = updated)))
        )
    }
    SectionCard(
        "${base.name} timeline",
        if (frames == base.frames) "Built-in frames — tweak a frame's timing below"
        else "Custom timing — Save pet keeps it"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (onionSkin && selectedFrame > 0) {
                AnimatedPetSprite(
                    animation = base,
                    frame = frames.getOrNull(selectedFrame - 1) ?: PetAnimationFrame(),
                    design = design,
                    spriteSize = 120.dp,
                    ghost = true
                )
            }
            AnimatedPetSprite(
                animation = base,
                frame = shown,
                design = design,
                spriteSize = 120.dp,
                ghost = false
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallAction(if (playing) "Pause" else "Play") {
                if (!playing && frames.isNotEmpty()) selectedFrame = selectedFrame % frames.size
                playing = !playing
            }
            Spacer(Modifier.width(8.dp))
            SmallAction("◀ Frame", enabled = frames.isNotEmpty()) {
                playing = false
                selectedFrame = (selectedFrame - 1 + frames.size) % frames.size
            }
            Spacer(Modifier.width(8.dp))
            SmallAction("Frame ▶", enabled = frames.isNotEmpty()) {
                playing = false
                selectedFrame = (selectedFrame + 1) % frames.size
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Frame ${selectedFrame + 1} of ${frames.size} — ${shown.durationMs} ms",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        Slider(
            value = shown.durationMs.toFloat().coerceIn(60f, 1000f),
            onValueChange = { ms ->
                // Dragging the timing pauses playback so the loop doesn't
                // advance the frame under the finger.
                if (playing) playing = false
                frames = frames.mapIndexed { i, f ->
                    if (i == selectedFrame) f.copy(durationMs = ms.toInt()) else f
                }
            },
            onValueChangeFinished = { commit(frames) },
            valueRange = 60f..1000f
        )
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            label = "Previous-frame ghost (onion skin)",
            checked = onionSkin,
            onCheckedChange = { onionSkin = it }
        )
        Spacer(Modifier.height(8.dp))
        SmallAction("Reset to built-in", enabled = frames != base.frames) {
            commit(base.frames)
            selectedFrame = 0
        }
    }
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
            AnimatedPetSprite(animation = animation, frame = frame, design = design, spriteSize = 34.dp, ghost = false)
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

/** A clear armed/off state prevents accidental drawing while the page scrolls. */
@Composable
/** v8.46 — tells the user whether editing is armed. Picking a tool in the
 *  tray below arms editing; with no tool the canvas scrolls safely (the old
 *  draw toggle is gone — the tool tray is the edit-mode switch). */
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
    onPaint: (Int, Int, Boolean) -> Unit
) {
    val gridSize = design.gridSize
    val latestOnPaint by rememberUpdatedState(onPaint)
    val rows = if (face.gridRows.size == gridSize) face.gridRows
    else List(gridSize) { ".".repeat(gridSize) }
    var gestures: Modifier = Modifier
    if (tool != null) {
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(gestures)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (tool != null) 2.dp else 1.dp,
                color = if (tool != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    line.forEach { ch ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(0.5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (ch == '.') MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                    else hexColor(design.colorOf(ch))
                                )
                        )
                    }
                }
            }
        }
    }
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
    var gestures: Modifier = Modifier
    if (tool != null) {
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(gestures)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (tool != null) 2.dp else 1.dp,
                color = if (tool != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { rowIndex, line ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    line.forEachIndexed { colIndex, ch ->
                        val filled = ch != '.'
                        val blueprintKey = blueprint?.getOrNull(rowIndex)?.getOrNull(colIndex)
                        val blueprintOnly = !filled && blueprintKey != null && blueprintKey != '.'
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(0.5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        filled -> hexColor(design.colorOf(ch))
                                        blueprintOnly -> hexColor(design.colorOf(blueprintKey)).copy(alpha = 0.28f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    }
                                )
                        )
                    }
                }
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

/** The primary save action. */
@Composable
private fun SaveButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
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

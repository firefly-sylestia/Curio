package com.curio.app.features.petdesigner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategories
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioPet
import com.curio.app.data.PetDesign
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

/** One editable color in the palette — its grid key, name and hex. */
private data class PaletteSlot(val key: Char, val name: String)

private val PALETTE_SLOTS = listOf(
    PaletteSlot('b', "Body"),
    PaletteSlot('B', "Shade"),
    PaletteSlot('o', "Ink"),
    PaletteSlot('s', "Scarf"),
    PaletteSlot('S', "Scarf dark"),
    PaletteSlot('G', "Gold"),
    PaletteSlot('g', "Gold deep")
)

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
private fun hexColor(hex: String): Color = runCatching { Color(0xFF000000L or (hex.toLong(16) shl 8)) }
    .getOrDefault(Color(0xFF4A3426))

/**
 * v8.34 — the Pet designer playground (Settings → Pet designer): a working
 * copy of the pet's look you can reshape live — 16×16 pixel grid editor
 * (body + asleep poses), palette recoloring with quick-pick swatches,
 * preset shapes, a randomizer, and plain-text import/export of the design
 * format (see [PetDesign]). Saving applies the design EVERYWHERE (always-on
 * — [AppPreferences.setPetDesign]); the pet sprite reads it reactively.
 */
@Composable
fun PetDesignerScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val savedText = AppPreferences.petDesignState
    // Working copy — starts from the saved design (or default), edited live.
    // remember (not rememberSaveable): PetDesign isn't Bundle-saveable, and
    // re-keying on savedText refreshes the start point when a design is
    // saved or cleared elsewhere.
    var design by remember(savedText) {
        mutableStateOf(
            savedText?.let { PetDesign.DEFAULT.toParsedOr(it, PetDesign.DEFAULT) } ?: PetDesign.DEFAULT
        )
    }
    // Which grid is being edited: "body" or "curled".
    var editingGrid by rememberSaveable { mutableStateOf("body") }
    // The currently selected palette paint key.
    var paintKey by rememberSaveable { mutableStateOf('b') }
    // When non-null, the color editor dialog is open for this palette key.
    var editingColorKey by rememberSaveable { mutableStateOf<Char?>(null) }
    // When non-null, the import/export dialog is open with this draft text.
    var importDraft by rememberSaveable { mutableStateOf<String?>(null) }
    // A transient confirmation ("Saved!" / "Copied!") shown under the actions.
    var toast by remember { mutableStateOf<String?>(null) }
    // Preview mood so the user can see the design in different poses.
    var previewMood by rememberSaveable { mutableStateOf(CurioPet.Mood.HAPPY) }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                top = SettingsHeroTotalHeight + 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CurioSectionLabel("Pet designer") }

            // ── Live preview ─────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 3.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Live preview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (design.isCustom) "Your custom look" else "The default look — make it yours!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        CurioPetSprite(
                            stage = CurioPet.currentStage(),
                            mood = previewMood,
                            spriteSize = 110.dp,
                            design = design
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                CurioPet.Mood.HAPPY to "Happy",
                                CurioPet.Mood.EXCITED to "Excited",
                                CurioPet.Mood.SLEEPY to "Asleep"
                            ).forEach { (mood, label) ->
                                MoodChip(
                                    label = label,
                                    selected = previewMood == mood,
                                    onClick = { previewMood = mood }
                                )
                            }
                        }
                    }
                }
            }

            // ── Pixel grid editor ─────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 3.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Pixel grid",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    "Tap a cell to paint with the selected color",
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
                                GridTab("Body", editingGrid == "body") { editingGrid = "body" }
                                GridTab("Asleep", editingGrid == "curled") { editingGrid = "curled" }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        PixelGrid(
                            design = design,
                            grid = editingGrid,
                            paintKey = paintKey,
                            onPaint = { row, col ->
                                design = design.withPixel(editingGrid, row, col, paintKey)
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallAction("Copy body → asleep", enabled = editingGrid == "body") {
                                design = design.copy(curledRows = PetDesign.bodyAsCurled(design.bodyRows))
                            }
                            SmallAction("Clear grid", enabled = true) {
                                val blank = List(16) { ".".repeat(16) }
                                design = design.withGrid(editingGrid, blank)
                            }
                        }
                    }
                }
            }

            // ── Palette editor ────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 3.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Colors",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Pick the paint color, or tap a swatch's pencil to edit its hex",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        PALETTE_SLOTS.forEach { slot ->
                            PaletteRow(
                                slot = slot,
                                hex = design.colorOf(slot.key),
                                selected = paintKey == slot.key,
                                onSelect = { paintKey = slot.key },
                                onEdit = { editingColorKey = slot.key }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // ── Shapes & randomize ────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 3.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Shapes & inspiration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Jump-start the body grid with a preset, or roll a fresh palette",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallAction("Default", enabled = true) {
                                design = design.withGrid("body", PetDesign.DEFAULT_BODY)
                            }
                            SmallAction("Robot", enabled = true) {
                                design = design.withGrid("body", ROBOT_BODY)
                            }
                            SmallAction("Ghost", enabled = true) {
                                design = design.withGrid("body", GHOST_BODY)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallAction("Random palette", enabled = true) {
                                design = design.randomize()
                            }
                            SmallAction("Reset all", enabled = design.isCustom) {
                                design = PetDesign.DEFAULT
                            }
                        }
                    }
                }
            }

            // ── Import / export / save ────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 3.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Import & export",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "The design is plain text — paste it in, edit colors by hand, or share it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallAction("Copy design text") {
                                clipboard.setText(AnnotatedString(design.toText()))
                                toast = "Copied to clipboard"
                            }
                            SmallAction("Paste design text") {
                                importDraft = clipboard.getText()?.text ?: ""
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tip: the palette is key=HEX lines — change a hex to recolor that part.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        SaveButton(
                            label = if (design.isCustom) "Save custom design" else "Use default look",
                            onClick = {
                                if (design.isCustom) {
                                    AppPreferences.setPetDesign(context, design.toText())
                                    toast = "Saved — your pet wears it everywhere"
                                } else {
                                    AppPreferences.clearPetDesign(context)
                                    toast = "Default look restored"
                                }
                            }
                        )
                        if (toast != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                toast.orEmpty(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        SettingsHeroHeader(
            title = "Pet designer",
            subtitle = "Draw your own Curio",
            onBack = { navController.popBackStack() },
            compact = wide
        )

        // ── Color editor overlay ─────────────────────────────────────
        editingColorKey?.let { key ->
            DialogScrim(onDismiss = { editingColorKey = null }) {
                ColorEditorCard(
                    key = key,
                    initialHex = design.colorOf(key),
                    onCancel = { editingColorKey = null },
                    onApply = { hex ->
                        design = design.withPaletteColor(key, hex)
                        editingColorKey = null
                    }
                )
            }
        }

        // ── Import/export overlay ────────────────────────────────────
        importDraft?.let { draft ->
            DialogScrim(onDismiss = { importDraft = null }) {
                ImportCard(
                    draft = draft,
                    onCancel = { importDraft = null },
                    onImport = { text ->
                        val parsed = PetDesign.DEFAULT.toParsedOr(text, PetDesign.DEFAULT)
                        // Tolerant parse always yields 32 rows; treat text
                        // that produced no palette keys AND the default body
                        // as unreadable so a garbage paste can't wipe a design.
                        val looksLikeDesign =
                            parsed != PetDesign.DEFAULT ||
                                text.contains("=") ||
                                text.lines().any { it.length >= 16 }
                        if (looksLikeDesign) {
                            design = parsed
                            importDraft = null
                            true
                        } else {
                            false
                        }
                    }
                )
            }
        }
    }
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

/** The hex color editor card. */
@Composable
private fun ColorEditorCard(
    key: Char,
    initialHex: String,
    onCancel: () -> Unit,
    onApply: (String) -> Unit
) {
    var hexDraft by rememberSaveable(key) { mutableStateOf(initialHex) }
    val hexError = hexDraft.length != 6
    Column(modifier = Modifier.padding(18.dp)) {
        Text(
            "Edit ${PALETTE_SLOTS.firstOrNull { it.key == key }?.name ?: key} color",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
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
                            .clickable { hexDraft = hex },
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

/** The import text card. */
@Composable
private fun ImportCard(
    draft: String,
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
            "key=HEX palette lines, then 16 body rows and 16 asleep rows",
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

/** The Body / Asleep tab in the grid card. */
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

/** The 16×16 pixel editor — tap a cell to paint it with the active key. */
@Composable
private fun PixelGrid(
    design: PetDesign,
    grid: String,
    paintKey: Char,
    onPaint: (Int, Int) -> Unit
) {
    val rows = if (grid == "curled") design.curledRows else design.bodyRows
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { rowIndex, line ->
            Row(modifier = Modifier.fillMaxWidth()) {
                line.forEachIndexed { colIndex, ch ->
                    val filled = ch != '.'
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (filled) hexColor(design.colorOf(ch))
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .clickable { onPaint(rowIndex, colIndex) }
                    )
                }
            }
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

/** A square robot preset for the shapes row. */
private val ROBOT_BODY: List<String> = listOf(
    ".......GG.......",
    "......ooooo.....",
    ".....obbbbbo....",
    "....obbbbbbbbo..",
    "...obbbbbbbbbbo.",
    "..obbbbbbbbbbbo.",
    ".obbbbbbbbbbbbo.",
    ".obbbbbbbbbbbbo.",
    ".obbbbbbbbbBBbo.",
    ".obbbbbbbbbBBbo.",
    ".obbbbbbbbbBBbo.",
    ".osssssssssssso.",
    ".oSSssssssssSSo.",
    "..oo..oooo..oo..",
    "..oo..oooo..oo..",
    "................"
)

/** A round ghost preset for the shapes row. */
private val GHOST_BODY: List<String> = listOf(
    ".......GG.......",
    "......ooooo.....",
    ".....obbbbbo....",
    "....obbbbbbbbo..",
    "...obbbbbbbbbbo.",
    "..obbbbbbbbbbbbo",
    "..obbbbbbbbbbbbo",
    "..obbbbbbbbbbbbo",
    "..obbbbbbbbbbbbo",
    "..obbbbbbbbbbbbo",
    "..obbbbbbbbbBBbo",
    "..obbbbbbbbbBBbo",
    "..osssssssssssso",
    "..oSSssssssSSo..",
    "..oo........oo..",
    "..oo........oo.."
)

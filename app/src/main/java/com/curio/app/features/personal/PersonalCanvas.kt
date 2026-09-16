package com.curio.app.features.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.curio.app.data.PersonalAlign
import com.curio.app.data.PersonalBlock
import com.curio.app.data.PersonalDoc
import com.curio.app.data.newBlockId
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.LoraFontFamily

/**
 * v387 — THE WRITING CANVAS (journals + chapter reviews share it).
 *
 * The body is a list of BLOCKS: a text block is one paragraph the writer can
 * style, a photo block is a picture they dropped in at the caret. Everything
 * the toolbar does is anchored on the FOCUSED block / the selection inside it,
 * which is how "bold just this line" works without a full document model:
 *
 *  · no selection  → the tool applies to that block (the line the caret is on)
 *  · a selection   → the tool applies to exactly those characters
 *  · an empty line → the tool arms and the next characters typed carry it
 *
 * Enter SPLITS the paragraph instead of inserting a newline, so a block stays
 * "one line" for alignment purposes and the caret flow matches a normal
 * editor (there is no marker syntax anywhere: the styling lives beside the
 * text, never inside it).
 */

// ────────────────────────────────────────────────────────────────────────────
// Style → pixels
// ────────────────────────────────────────────────────────────────────────────

/** The ink of a pulled quote inside the canvas. */
@Composable
internal fun personalQuoteFill(): Color =
    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)

/** Renders one block's text with its per-character flags applied.
 *  [quoteTint] draws the pulled-quote block's rule colour and wash. */
internal fun personalAnnotated(
    text: String,
    mask: IntArray,
    ink: Color,
    quoteInk: Color,
    quoteFill: Color
): AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    append(text)
    var i = 0
    while (i < text.length) {
        val flags = mask.getOrElse(i) { 0 }
        var j = i + 1
        while (j < text.length && mask.getOrElse(j) { 0 } == flags) j++
        if (flags != 0) {
            val decorations = ArrayList<TextDecoration>(2)
            if (flags and FLAG_UNDERLINE != 0) decorations.add(TextDecoration.Underline)
            if (flags and FLAG_STRIKE != 0) decorations.add(TextDecoration.LineThrough)
            addStyle(
                SpanStyle(
                    color = if (flags and FLAG_QUOTE != 0) quoteInk else ink,
                    fontWeight = if (flags and FLAG_BOLD != 0) FontWeight.Bold else null,
                    fontStyle = if (flags and FLAG_ITALIC != 0) FontStyle.Italic else null,
                    textDecoration = when (decorations.size) {
                        0 -> null
                        1 -> decorations.first()
                        else -> TextDecoration.combine(decorations)
                    },
                    background = if (flags and FLAG_QUOTE != 0) quoteFill else Color.Unspecified
                ),
                i, j
            )
        }
        i = j
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Editor state
// ────────────────────────────────────────────────────────────────────────────

/** Where the caret should land after a structural change (a photo insert or
 *  a paragraph split) — the canvas consumes it once. */
internal data class PersonalCaret(val blockId: String, val index: Int)

/**
 * The canvas' brain: the block list, each block's text + style mask, and the
 * toolbar's live state. Deliberately NOT a Compose UI class — a screen can
 * drive it (auto-save, "add a photo", programmatic focus) without touching
 * the field.
 */
internal class PersonalEditorState(initial: PersonalDoc) {

    private val order: SnapshotStateList<String> = mutableStateListOf()
    private val blocks: SnapshotStateMap<String, PersonalBlock> = mutableStateMapOf()
    private val masks: SnapshotStateMap<String, IntArray> = mutableStateMapOf()
    private val selections: SnapshotStateMap<String, TextRange> = mutableStateMapOf()
    private val compositions: SnapshotStateMap<String, TextRange?> = mutableStateMapOf()

    /** Called with the whole document after every change (auto-save). */
    var onDocChanged: (PersonalDoc) -> Unit = {}

    /** The block the keyboard is in — the target of every tool. */
    var focusedId by mutableStateOf<String?>(null)
        private set

    /** Tools switched on with nothing to apply them to (an empty line). */
    var armed by mutableIntStateOf(0)
        private set

    private var caret by mutableStateOf<PersonalCaret?>(null)

    /**
     * Rebuilds the whole canvas from a document that arrived from the store
     * (`LaunchedEffect(entryId)` load). Called only when the SCREEN changes,
     * never on an echo of the writer's own keystrokes — replacing the blocks
     * mid-typing would drop the caret.
     */
    fun replace(document: PersonalDoc) {
        order.clear()
        blocks.clear()
        masks.clear()
        selections.clear()
        compositions.clear()
        focusedId = null
        armed = 0
        caret = null
        val source = if (document.blocks.isEmpty()) listOf(PersonalBlock(id = newBlockId()))
        else document.blocks
        source.forEach { block ->
            val id = block.id.ifBlank { newBlockId() }
            val normalised = block.copy(id = id)
            order.add(id)
            blocks[id] = normalised
            masks[id] = runsToMask(normalised.text.length, normalised.runs)
        }
    }

    /** The caret the canvas should move to (read by every block, consumed by
     *  the one it names). */
    val pendingCaret: PersonalCaret? get() = caret

    /** Called by the block that took the caret, so no other block re-asks. */
    fun consumeCaret(id: String) {
        if (caret?.blockId == id) caret = null
    }

    init {
        val source = if (initial.blocks.isEmpty()) listOf(PersonalBlock(id = newBlockId()))
        else initial.blocks
        source.forEach { block ->
            val id = block.id.ifBlank { newBlockId() }
            val normalised = block.copy(id = id)
            order.add(id)
            blocks[id] = normalised
            masks[id] = runsToMask(normalised.text.length, normalised.runs)
        }
        if (order.isEmpty()) {
            val block = PersonalBlock(id = newBlockId())
            order.add(block.id)
            blocks[block.id] = block
            masks[block.id] = emptyMask(0)
        }
    }

    // ── Reads ──────────────────────────────────────────────────────────

    val blockIds: List<String> get() = order

    fun block(id: String): PersonalBlock? = blocks[id]

    fun text(id: String): String = blocks[id]?.text.orEmpty()

    fun mask(id: String): IntArray = masks[id] ?: emptyMask(text(id).length)

    fun photo(id: String): String? = blocks[id]?.photo

    fun caption(id: String): String = blocks[id]?.caption.orEmpty()

    fun align(id: String): PersonalAlign = blocks[id]?.align ?: PersonalAlign.START

    fun selection(id: String): TextRange? = selections[id]

    fun composition(id: String): TextRange? = compositions[id]

    /** True when nothing has been written yet (the screen's save gate). */
    fun isEmpty(): Boolean = doc().isEmpty

    /** The whole document as it will be stored. */
    fun doc(): PersonalDoc = PersonalDoc(
        order.mapNotNull { id ->
            blocks[id]?.let { block ->
                block.copy(
                    text = block.text,
                    runs = maskToRuns(mask(id)),
                    align = block.align
                )
            }
        }
    )

    // ── Edits ──────────────────────────────────────────────────────────

    /** The writer typed / pasted / deleted inside one block. */
    fun onFieldChange(id: String, value: TextFieldValue) {
        val old = blocks[id] ?: return
        val newText = value.text
        if (newText != old.text) {
            masks[id] = maskAfterEdit(old.text, newText, mask(id), armed)
            blocks[id] = old.copy(text = newText)
            // An armed tool has now been used: what follows continues in the
            // style just typed, so the button stops being "pending".
            if (armed != 0) armed = 0
        }
        selections[id] = value.selection
        compositions[id] = value.composition
        onDocChanged(doc())
    }

    fun onFocusChanged(id: String, focused: Boolean) {
        if (focused) focusedId = id
        else if (focusedId == id) focusedId = null
    }

    /** The flags of whatever the focused tool bar would act on right now —
     *  drives which buttons read as switched on. */
    fun activeFlags(): Int {
        val id = focusedId ?: return armed
        val selection = selections[id]
        // No selection: the LINE decides, exactly like the tap would — a tool
        // lights only when the whole line already carries it, so what the dock
        // shows is what another tap on that button would do.
        val range = if (selection != null && !selection.collapsed) {
            selection.min to selection.max
        } else {
            0 to text(id).length
        }
        val blockMask = mask(id)
        var flags = 0
        ALL_FLAGS.forEach { flag ->
            if (maskCovers(blockMask, range.first, range.second, flag)) flags = flags or flag
        }
        return flags or armed
    }

    /**
     * Toggles one tool on the focused block: the selection when there is one,
     * the whole line otherwise. An empty line arms the tool instead, so the
     * first words typed arrive already styled.
     */
    fun toggle(flag: Int) {
        val id = focusedId ?: return
        val blockMask = mask(id)
        val selection = selections[id]
        val range = if (selection != null && !selection.collapsed) {
            selection.min to selection.max
        } else {
            0 to text(id).length
        }
        if (range.second <= range.first) {
            armed = armed xor flag
            onDocChanged(doc())
            return
        }
        val on = !maskCovers(blockMask, range.first, range.second, flag)
        masks[id] = maskApply(blockMask, range.first, range.second, flag, on)
        armed = armed and flag.inv()
        onDocChanged(doc())
    }

    /** Left / centre for the focused block (a paragraph is the unit a line
     *  tool can point at). */
    fun setAlign(align: PersonalAlign) {
        val id = focusedId ?: order.firstOrNull() ?: return
        val block = blocks[id] ?: return
        blocks[id] = block.copy(align = align)
        onDocChanged(doc())
    }

    /**
     * Drops a photo in at the caret: the block splits into what came before,
     * the picture, and what comes after (a fresh empty block when the
     * paragraph ended there). This is what "photos in the same canvas" means —
     * the picture sits exactly where the writer was standing.
     */
    fun insertPhoto(uri: String) {
        val id = focusedId ?: order.lastOrNull() ?: return
        val block = blocks[id] ?: return
        val caretIndex = (selections[id]?.start ?: block.text.length)
            .coerceIn(0, block.text.length)
        val head = PersonalBlock(
            id = newBlockId(),
            text = block.text.take(caretIndex),
            runs = maskToRuns(mask(id).copyOfRange(0, caretIndex)),
            align = block.align
        )
        val tailText = block.text.drop(caretIndex)
        val tailMask = mask(id).copyOfRange(caretIndex, block.text.length)
        val tail = PersonalBlock(
            id = newBlockId(),
            text = tailText,
            runs = maskToRuns(tailMask),
            align = block.align
        )
        val photoBlock = PersonalBlock(id = newBlockId(), photo = uri)
        val index = order.indexOf(id)
        if (index < 0) return
        // The old block is replaced in place, so the page never jumps.
        order[index] = head.id
        blocks.remove(id)
        masks.remove(id)
        selections.remove(id)
        compositions.remove(id)
        blocks[head.id] = head
        masks[head.id] = runsToMask(head.text.length, head.runs)
        order.add(index + 1, photoBlock.id)
        blocks[photoBlock.id] = photoBlock
        masks[photoBlock.id] = emptyMask(0)
        if (tailText.isNotEmpty()) {
            order.add(index + 2, tail.id)
            blocks[tail.id] = tail
            masks[tail.id] = runsToMask(tail.text.length, tail.runs)
            caret = PersonalCaret(tail.id, 0)
        } else {
            val empty = PersonalBlock(id = newBlockId())
            order.add(index + 2, empty.id)
            blocks[empty.id] = empty
            masks[empty.id] = emptyMask(0)
            caret = PersonalCaret(empty.id, 0)
        }
        focusedId = caret?.blockId
        onDocChanged(doc())
    }

    fun setCaption(id: String, caption: String) {
        val block = blocks[id] ?: return
        blocks[id] = block.copy(caption = caption)
        onDocChanged(doc())
    }

    /** Removes a photo block (and its caption). */
    fun removeBlock(id: String) {
        val index = order.indexOf(id)
        if (index < 0) return
        order.remove(id)
        blocks.remove(id)
        masks.remove(id)
        selections.remove(id)
        compositions.remove(id)
        if (order.isEmpty()) {
            val block = PersonalBlock(id = newBlockId())
            order.add(block.id)
            blocks[block.id] = block
            masks[block.id] = emptyMask(0)
        }
        if (focusedId == id) focusedId = order.getOrNull(index.coerceAtMost(order.size - 1))
        onDocChanged(doc())
    }

    /**
     * Enter: the paragraph splits at the caret and the caret lands at the
     * start of the new one. Style is NOT carried across the break — a new
     * line starts as plain prose unless a tool is armed.
     */
    fun splitAtCaret(id: String) {
        val block = blocks[id] ?: return
        val caretIndex = (selections[id]?.start ?: block.text.length)
            .coerceIn(0, block.text.length)
        val index = order.indexOf(id)
        if (index < 0) return
        val head = block.copy(
            text = block.text.take(caretIndex),
            runs = maskToRuns(mask(id).copyOfRange(0, caretIndex))
        )
        val tail = PersonalBlock(
            id = newBlockId(),
            text = block.text.drop(caretIndex),
            runs = maskToRuns(mask(id).copyOfRange(caretIndex, block.text.length)),
            align = if (block.text.drop(caretIndex).isEmpty()) block.align else PersonalAlign.START
        )
        masks[id] = runsToMask(head.text.length, head.runs)
        blocks[id] = head
        order.add(index + 1, tail.id)
        blocks[tail.id] = tail
        masks[tail.id] = runsToMask(tail.text.length, tail.runs)
        selections[tail.id] = TextRange(0)
        focusedId = tail.id
        caret = PersonalCaret(tail.id, 0)
        onDocChanged(doc())
    }

    /** Focus + caret request the canvas consumes on its next frame. */
    fun requestCaret(id: String, index: Int = 0) {
        caret = PersonalCaret(id, index)
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Canvas
// ────────────────────────────────────────────────────────────────────────────

/**
 * The writing surface. It does NOT scroll: the caller owns the scroll
 * container, so the journal's header (date, mood, title) and the toolbar can
 * frame the page exactly as the design wants.
 */
@Composable
internal fun PersonalCanvas(
    state: PersonalEditorState,
    modifier: Modifier = Modifier,
    ink: Color = MaterialTheme.colorScheme.onSurface,
    accent: Color = MaterialTheme.colorScheme.primary,
    onOpenPhoto: (String) -> Unit = {},
    enabled: Boolean = true
) {
    val quoteFill = personalQuoteFill()
    val quoteInk = ink.copy(alpha = 0.78f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.blockIds.forEach { id ->
            val block = state.block(id) ?: return@forEach
            if (block.isPhoto) {
                PersonalPhotoBlock(
                    uri = block.photo.orEmpty(),
                    caption = state.caption(id),
                    ink = ink,
                    enabled = enabled,
                    onCaption = { state.setCaption(id, it) },
                    onRemove = { state.removeBlock(id) },
                    onOpen = { onOpenPhoto(block.photo.orEmpty()) }
                )
            } else {
                PersonalTextBlock(id = id, state = state, ink = ink, accent = accent, enabled = enabled)
            }
        }
    }
}

@Composable
private fun PersonalTextBlock(
    id: String,
    state: PersonalEditorState,
    ink: Color,
    accent: Color,
    enabled: Boolean
) {
    val text = state.text(id)
    val mask = state.mask(id)
    val align = state.align(id)
    val quoteFill = personalQuoteFill()
    val quoteInk = ink.copy(alpha = 0.78f)
    val focusRequester = remember(id) { FocusRequester() }
    val value = TextFieldValue(
        annotatedString = personalAnnotated(text, mask, ink, quoteInk, quoteFill),
        selection = (state.selection(id) ?: TextRange(text.length))
            .let { if (it.max > text.length) TextRange(text.length) else it }
            .let { if (it.min < 0) TextRange(0) else it },
        composition = state.composition(id)
    )
    val bodyStyle = TextStyle(
        fontFamily = LoraFontFamily,
        fontSize = 17.sp,
        lineHeight = 29.sp,
        color = ink,
        textAlign = if (align == PersonalAlign.CENTER) TextAlign.Center else TextAlign.Start
    )
    BasicTextField(
        value = value,
        onValueChange = { state.onFieldChange(id, it) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { state.onFocusChanged(id, it.isFocused) }
            .onPreviewKeyEvent { event ->
                // Enter makes a NEW line (a block), so a line tool can point
                // at the line the caret is on. Shift+Enter keeps the plain
                // newline inside the paragraph.
                if (
                    enabled &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                    !event.isShiftPressed
                ) {
                    state.splitAtCaret(id)
                    true
                } else {
                    false
                }
            },
        textStyle = bodyStyle,
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default
        ),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        text = "Write…",
                        style = bodyStyle.copy(color = ink.copy(alpha = 0.32f))
                    )
                }
                inner()
            }
        }
    )
    // Structural edits (a photo dropped in, a paragraph split) hand the caret
    // to a block that did not exist a frame ago — the field has to ask for
    // focus itself, in its own composition scope.
    val pendingCaret = state.pendingCaret
    LaunchedEffect(pendingCaret, id) {
        if (pendingCaret?.blockId == id) {
            focusRequester.requestFocus()
            state.onFocusChanged(id, true)
            state.consumeCaret(id)
        }
    }
}

@Composable
private fun PersonalPhotoBlock(
    uri: String,
    caption: String,
    ink: Color,
    enabled: Boolean,
    onCaption: (String) -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(212.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) { onOpen() }
        ) {
            androidx.compose.foundation.Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(212.dp)
            )
            if (enabled) {
                Surface(
                    onClick = onRemove,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.42f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.Close,
                            "Remove photo",
                            tint = Color.White,
                            size = 15.dp
                        )
                    }
                }
            }
        }
        if (enabled) {
            BasicTextField(
                value = caption,
                onValueChange = onCaption,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = LoraFontFamily,
                    fontSize = 13.sp,
                    color = ink.copy(alpha = 0.72f)
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                decorationBox = { inner ->
                    Box {
                        if (caption.isEmpty()) {
                            Text(
                                "Add a caption",
                                style = TextStyle(
                                    fontFamily = LoraFontFamily,
                                    fontSize = 13.sp,
                                    color = ink.copy(alpha = 0.34f)
                                )
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Read-only rendering (the book page's review cards, journal previews)
// ────────────────────────────────────────────────────────────────────────────

/** Renders a saved document without any editing chrome. */
@Composable
internal fun PersonalDocView(
    doc: PersonalDoc,
    modifier: Modifier = Modifier,
    ink: Color = MaterialTheme.colorScheme.onSurface,
    onOpenPhoto: (String) -> Unit = {}
) {
    val quoteFill = personalQuoteFill()
    val quoteInk = ink.copy(alpha = 0.78f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        doc.blocks.forEach { block ->
            if (block.isPhoto) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenPhoto(block.photo.orEmpty()) }
                ) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(block.photo.orEmpty()),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                    if (block.caption.isNotBlank()) {
                        Text(
                            block.caption,
                            style = TextStyle(
                                fontFamily = LoraFontFamily,
                                fontSize = 13.sp,
                                color = ink.copy(alpha = 0.62f)
                            ),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else if (block.text.isNotBlank()) {
                Text(
                    text = personalAnnotated(
                        block.text,
                        runsToMask(block.text.length, block.runs),
                        ink,
                        quoteInk,
                        quoteFill
                    ),
                    style = TextStyle(
                        fontFamily = LoraFontFamily,
                        fontSize = 16.sp,
                        lineHeight = 27.sp,
                        textAlign = if (block.align == PersonalAlign.CENTER) TextAlign.Center
                        else TextAlign.Start
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Toolbar
// ────────────────────────────────────────────────────────────────────────────

/**
 * The tool dock — it rides ABOVE the keyboard (the caller pins it to the
 * bottom of an `imePadding()` column), so the tools are always under the
 * writer's thumb while the words stay above the keys.
 */
@Composable
internal fun PersonalToolDock(
    state: PersonalEditorState,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    surface: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val active = state.activeFlags()
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surface,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PersonalToolButton(
                label = "Bold",
                active = active and FLAG_BOLD != 0,
                accent = accent, ink = ink,
                onClick = { state.toggle(FLAG_BOLD) }
            ) {
                Text("B", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 17.sp))
            }
            PersonalToolButton(
                label = "Italic",
                active = active and FLAG_ITALIC != 0,
                accent = accent, ink = ink,
                onClick = { state.toggle(FLAG_ITALIC) }
            ) {
                Text(
                    "I",
                    style = TextStyle(fontStyle = FontStyle.Italic, fontSize = 17.sp)
                )
            }
            PersonalToolButton(
                label = "Underline",
                active = active and FLAG_UNDERLINE != 0,
                accent = accent, ink = ink,
                onClick = { state.toggle(FLAG_UNDERLINE) }
            ) {
                Text(
                    "U",
                    style = TextStyle(
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
            PersonalToolButton(
                label = "Strikethrough",
                active = active and FLAG_STRIKE != 0,
                accent = accent, ink = ink,
                onClick = { state.toggle(FLAG_STRIKE) }
            ) {
                Text(
                    "S",
                    style = TextStyle(
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.LineThrough
                    )
                )
            }
            PersonalToolButton(
                label = "Quote",
                active = active and FLAG_QUOTE != 0,
                accent = accent, ink = ink,
                onClick = { state.toggle(FLAG_QUOTE) }
            ) {
                CurioIcon(CurioIcons.FormatQuote, null, size = 18.dp)
            }
            PersonalToolButton(
                label = "Align left",
                active = state.alignOfFocused() == PersonalAlign.START,
                accent = accent, ink = ink,
                onClick = { state.setAlign(PersonalAlign.START) }
            ) {
                AlignGlyph(center = false)
            }
            PersonalToolButton(
                label = "Align centre",
                active = state.alignOfFocused() == PersonalAlign.CENTER,
                accent = accent, ink = ink,
                onClick = { state.setAlign(PersonalAlign.CENTER) }
            ) {
                AlignGlyph(center = true)
            }
            PersonalToolButton(
                label = "Add a photo",
                active = false,
                accent = accent, ink = ink,
                onClick = onPickPhoto
            ) {
                CurioIcon(CurioIcons.Image, null, size = 18.dp)
            }
        }
    }
}

/** The alignment tools draw their own glyph (three rules), so the dock never
 *  depends on a font subset that has no align icons. */
@Composable
private fun AlignGlyph(center: Boolean) {
    val ink = LocalContentColor.current
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8f.dp.toPx()
        val width = size.width
        val gaps = listOf(1f, 0.72f, 1f, 0.72f)
        gaps.forEachIndexed { index, fraction ->
            val y = size.height * (0.22f + index * 0.19f)
            val lineWidth = width * fraction
            val x = if (center) (width - lineWidth) / 2f else 0f
            drawLine(
                color = ink,
                start = Offset(x, y),
                end = Offset(x + lineWidth, y),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PersonalToolButton(
    label: String,
    active: Boolean,
    accent: Color,
    ink: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (active) accent.copy(alpha = 0.16f) else Color.Transparent,
        modifier = Modifier.size(38.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides if (active) accent else ink) {
                Box(
                    modifier = Modifier.semantics { contentDescription = label },
                    contentAlignment = Alignment.Center
                ) { content() }
            }
        }
    }
}

/** The focused block's alignment (the dock's lit state). */
private fun PersonalEditorState.alignOfFocused(): PersonalAlign {
    val id = focusedId ?: blockIds.firstOrNull() ?: return PersonalAlign.START
    return align(id)
}

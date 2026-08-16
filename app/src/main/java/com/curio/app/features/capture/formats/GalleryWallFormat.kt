package com.curio.app.features.capture.formats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.curio.app.data.CaptureData
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.curio.app.data.AppPreferences
import com.curio.app.ui.components.CurioMoodBoardBackdrop
import com.curio.app.ui.components.MoodBoardFloatingCards
import com.curio.app.ui.components.MoodBoardZoomOverlay
import com.curio.app.ui.components.NotePaperCard
import com.curio.app.ui.components.moodBoardPainter
import com.curio.app.ui.components.rememberMoodBoardZoomState
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.pastelFillInk
import kotlin.math.roundToInt
import kotlin.random.Random

// ── Mood board tile with pixel-based positioning ─────────────────────

private data class MoodTile(
    val id: Int,
    val uri: String,
    val offsetXPx: Float,
    val offsetYPx: Float,
    val rotationDeg: Float,
    val widthPx: Float,
    val heightPx: Float
)

/**
 * Transient drag/pinch preview for one tile while a finger gesture is in
 * flight. Kept OUTSIDE the [MoodTile] list so per-frame gesture updates
 * recompose only the dragged tile instead of mutating the list (which would
 * re-run the whole save pipeline) on every pointer move. The final values are
 * committed into the list once when the finger lifts.
 */
private data class TileDragPreview(
    val dx: Float,
    val dy: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    /** True when a single-finger drag moved the tile (pin-zone eligible). */
    val byDrag: Boolean = false
)

private fun finiteOr(value: Float, fallback: Float = 0f): Float =
    value.takeIf { it.isFinite() } ?: fallback

private fun positiveFiniteOr(value: Float, fallback: Float): Float =
    value.takeIf { it.isFinite() && it > 0f } ?: fallback

/**
 * Gallery Wall / Mood Board format — CURIO_SPEC §8.4 (Visual Art / Painters).
 *
 * A freeform collage canvas where users can:
 *  - Add images from their gallery
 *  - Drag tiles anywhere on the board
 *  - Tap to bring a tile to the front (z-order)
 *  - Remove tiles via corner × button
 *  - Add an optional caption below
 *  - Expand to a full-screen canvas for precise placement (top-right button)
 *
 * Tiles are placed with random initial positions, slight rotations, and
 * varying sizes to create a natural mood-board collage aesthetic. Tiles
 * render with [ContentScale.Fit] + inner padding — the same logic the saved
 * EntryDetail view uses — so images are never cropped or pixelated while
 * composing.
 *
 * The board canvas sits on a theme-aware watermark backdrop whose random
 * glyph scatter is seeded per board, so every mood board gets its own quiet
 * background pattern. New boards get a fresh random seed; edit mode passes
 * [boardSeed] (the saved entry's id hash) so the editor's pattern matches
 * the saved EntryDetail view exactly.
 *
 * When [initialData] is supplied (edit mode), the board preloads the saved
 * tiles and caption so the user can continue arranging and re-save.
 */
@Composable
fun GalleryWallFormat(
    accent: Color,
    tint: Color,
    onCanSaveChange: (Boolean) -> Unit,
    onDataChanged: (CaptureData?) -> Unit = {},
    initialData: CaptureData.GalleryWall? = null,
    boardSeed: Int? = null
) {
    val tiles = remember(initialData) {
        mutableStateListOf<MoodTile>().apply {
            initialData?.tileLayouts?.forEachIndexed { i, t ->
                add(
                    MoodTile(
                        id = i,
                        uri = t.uri,
                        offsetXPx = t.offsetXPx,
                        offsetYPx = t.offsetYPx,
                        rotationDeg = t.rotationDeg,
                        widthPx = t.widthPx,
                        heightPx = t.heightPx
                    )
                )
            }
        }
    }
    // v57 — the FULL-SCREEN arrangement, saved separately from the inline
    // one. Seeded from the saved full layout (legacy entries: the inline
    // layout, so both views start identical until the user rearranges in
    // the expanded editor). Edited ONLY by the full-screen canvas.
    val fullTiles = remember(initialData) {
        mutableStateListOf<MoodTile>().apply {
            val src = initialData?.tileLayoutsFull
                ?.takeIf { it.isNotEmpty() }
                ?: initialData?.tileLayouts.orEmpty()
            src.forEachIndexed { i, t ->
                add(
                    MoodTile(
                        id = i,
                        uri = t.uri,
                        offsetXPx = t.offsetXPx,
                        offsetYPx = t.offsetYPx,
                        rotationDeg = t.rotationDeg,
                        widthPx = t.widthPx,
                        heightPx = t.heightPx
                    )
                )
            }
        }
    }
    var caption by remember(initialData) { mutableStateOf(initialData?.caption ?: "") }
    // Note-paper style for the caption box — legacy entries lack the field
    // (Gson → null), fall back to the take-level paperStyle → RULED.
    var captionStyle by remember(initialData) {
        mutableStateOf(initialData?.captionStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED)
    }
    // Note-paper color for the caption box — legacy entries lack the field
    // (Gson → null), fall back to CREAM.
    var captionColor by remember(initialData) {
        mutableStateOf(initialData?.captionColor ?: NotePaperColor.CREAM)
    }
    // Quote cards — the SHARED hand-placed paper notecard section (same
    // component Marginalia / Reel Notes / Sound Bite use). Owns the parallel
    // lists (text / spans / tilt / style / color); new cards inherit the
    // caption's current paper style + color.
    val quoteCards = rememberQuoteCardsState(
        initialQuotes = initialData?.quotes.orEmpty(),
        initialSpans = initialData?.quoteSpans.orEmpty(),
        initialTilts = initialData?.quoteTilts.orEmpty(),
        initialStyles = initialData?.quoteStyles.orEmpty(),
        initialColors = initialData?.quoteColors.orEmpty(),
        // v7.20 — per-card board positions (dragged in the editor).
        initialPositions = initialData?.quotePositions.orEmpty(),
        // v7.22 — per-card on-board flag (chip = on the board, bottom
        // button = below the board). Legacy entries lack it → all on-board.
        initialOnBoard = initialData?.quoteOnBoard.orEmpty(),
        defaultStyle = initialData?.captionStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED,
        defaultColor = initialData?.captionColor ?: NotePaperColor.CREAM
    )
    var boardExpanded by remember { mutableStateOf(false) }
    // v57 — the FULL-SCREEN quote placements (the expanded board's own
    // pixel space), aligned 1:1 with the shared cards by index. Text,
    // style, tilt and WIDTH stay shared — only where each card sits differs
    // per view. Padded as cards are added; the remove hook keeps it aligned.
    val fullQuotePositions = remember(initialData) {
        mutableStateListOf<CaptureData.QuotePos>().apply {
            val src = initialData?.quotePositionsFull
                ?.takeIf { it.isNotEmpty() }
                ?: initialData?.quotePositions.orEmpty()
            addAll(src)
        }
    }
    // Keep the full-screen list index-aligned when a card is deleted
    // (additions are padded by the LaunchedEffect below).
    quoteCards.onCardRemoved = { idx ->
        if (idx in fullQuotePositions.indices) fullQuotePositions.removeAt(idx)
    }
    LaunchedEffect(quoteCards.positions.size) {
        while (fullQuotePositions.size < quoteCards.positions.size) {
            fullQuotePositions.add(CaptureData.QuotePos(-1f, -1f))
        }
    }

    // v7.19 — the quote card currently open in the floating edit dialog.
    // The cards themselves float INSIDE the board (see MoodBoardCanvas);
    // tapping one opens this dialog with the full rich-text editor.
    var editingQuoteIndex by remember { mutableStateOf<Int?>(null) }
    // Mood — the shared "How did it make you feel?" row. The board carries
    // its own mood field now (CaptureData.GalleryWall.mood); legacy entries
    // have none (Gson → null).
    var mood by remember(initialData) { mutableStateOf(initialData?.mood) }
    // New board: fresh random pattern. Edit mode: reuse the caller-provided
    // seed (entry-id hash) so the editor matches the saved view's backdrop.
    val seed = remember(boardSeed, initialData) { boardSeed ?: Random.nextInt() }

    // A caption-only board is still a draft — it must save and must trigger
    // the leave / format-switch guards (the old tiles/quotes-only rule let
    // a caption-only take exit silently and lose the caption).
    // v57 — fullTiles counts toward savability too: a board arranged ONLY
    // in the full-screen editor (never touched inline) must still save.
    val canSave = tiles.isNotEmpty() || fullTiles.isNotEmpty() || quoteCards.hasContent || caption.isNotBlank()
    LaunchedEffect(
        canSave, caption, tiles.toList(), captionStyle, captionColor, mood,
        quoteCards.quotes.toList(), quoteCards.spans.toList(), quoteCards.tilts.toList(),
        quoteCards.styles.toList(), quoteCards.colors.toList(), quoteCards.positions.toList(),
        quoteCards.onBoard.toList(),
        // v57 — the full-screen arrangement is part of the save pipeline:
        // moving a tile or quote in the expanded editor must re-emit the
        // entry with the updated full layouts.
        fullTiles.toList(), fullQuotePositions.toList()
    ) {
        onCanSaveChange(canSave)
        onDataChanged(
            if (canSave) CaptureData.GalleryWall(
                imageCount = tiles.size,
                caption = caption,
                imageUris = tiles.map { it.uri },
                tileLayouts = tiles.map { CaptureData.TileLayout(it.uri, it.offsetXPx, it.offsetYPx, it.rotationDeg, it.widthPx, it.heightPx) },
                // v57 — the full-screen arrangement, saved alongside the
                // inline one. Falls back to the inline layout for legacy
                // entries (empty → the expanded view shows the inline board).
                tileLayoutsFull = fullTiles.map { CaptureData.TileLayout(it.uri, it.offsetXPx, it.offsetYPx, it.rotationDeg, it.widthPx, it.heightPx) },
                captionStyle = captionStyle,
                captionColor = captionColor,
                quotes = quoteCards.quotes.toList(),
                quoteSpans = quoteCards.spans.toList(),
                quoteTilts = quoteCards.tilts.toList(),
                quoteStyles = quoteCards.styles.toList(),
                quoteColors = quoteCards.colors.toList(),
                // v7.20 — dragged card positions (editor board px; (-1,-1)
                // = never dragged → saved views use the deterministic slot).
                quotePositions = quoteCards.positions.toList(),
                // v57 — the full-screen editor's card placements.
                quotePositionsFull = fullQuotePositions.toList(),
                // v7.22 — per-card on-board flag (chip vs bottom button).
                quoteOnBoard = quoteCards.onBoard.toList(),
                // Legacy fallback — mirror the caption's style.
                paperStyle = captionStyle,
                mood = mood
            )
            else null
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Nudge past 8 images ──────────────────────────────────────
        if (tiles.size > 8) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurioIcon(
                        name = CurioIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        size = 18.dp
                    )
                    Text(
                        text = "Getting full! Consider saving and starting a new board.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // Inline mood board canvas (editable)
        // ═══════════════════════════════════════════════════════════════
        MoodBoardCanvas(
            tiles = tiles,
            accent = accent,
            tint = tint,
            seed = seed,
            fullScreen = false,
            onExpand = { boardExpanded = true },
            onCollapse = {},
            // v7.19 — floating quote boxes live ON the board.
            quoteState = quoteCards,
            onEditQuote = { editingQuoteIndex = it },
            // v7.22 — the board chip's quotes float ON the board.
            onAddQuote = { quoteCards.addCard(captionStyle, captionColor, onBoard = true) }
        )

        // v7.23 — the inline editor renders the board exactly like the saved
        // small card (same fit + center crop), so a quick nudge to use the
        // full-screen canvas for precise placement.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurioIcon(
                name = CurioIcons.Fullscreen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 13.dp
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Use full-screen editing for precise placement",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Caption field — wears the note-paper slip like the other text
        //    boxes, with its own per-field paper-style toggle.
        PaperLineField(
            value = caption,
            onValueChange = { caption = it },
            label = "Add a caption (optional)",
            paperStyle = captionStyle,
            onPaperStyleChange = { captionStyle = it },
            paperColor = captionColor,
            onPaperColorChange = { captionColor = it }
        )

        // ── Quote boxes — the board chip's cards float ON the board above;
        // THIS bottom section is for the separate BELOW-board cards: the
        // Add button creates one below the board (onBoard = false), and the
        // cards themselves render inline here (only the below-board subset —
        // the on-board ones stay on the collage). The note-paper COLOR tool
        // stays hidden for mood-board quotes (v7.19) while text formatting +
        // paper style remain fully available.
        QuoteCardsSection(
            state = quoteCards,
            header = "Quote boxes",
            newCardStyle = { captionStyle },
            newCardColor = { captionColor },
            showColorTool = false,
            cardsInline = true,
            // v7.22 — only the below-board cards render + count here.
            cardsFilter = { i -> quoteCards.onBoard.getOrElse(i) { true } == false },
            onAddCard = { quoteCards.addCard(captionStyle, captionColor, onBoard = false) }
        )
    }

    // ── Floating quote edit dialog (v7.19) — full rich-text editor for
    // one floating card, color tool hidden. The card's own Remove button
    // in the header removes it; the dialog's Remove closes after removing.
    editingQuoteIndex?.let { idx ->
        FloatingQuoteEditDialog(
            state = quoteCards,
            index = idx,
            accent = accent,
            onClose = { editingQuoteIndex = null }
        )
    }

    // ── Full-screen editing canvas ────────────────────────────────────
    if (boardExpanded) {
        Dialog(
            onDismissRequest = { boardExpanded = false },
            // True full screen: the dialog draws behind the system bars so
            // the board fills the whole display instead of floating like a
            // dialog page. The controls below pad for the bars.
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                MoodBoardCanvas(
                    // v57 — the full-screen editor arranges its OWN copy of
                    // the tiles (fullTiles), saved separately from the
                    // inline arrangement.
                    tiles = fullTiles,
                    accent = accent,
                    tint = tint,
                    seed = seed,
                    fullScreen = true,
                    onExpand = {},
                    onCollapse = { boardExpanded = false },
                    // v7.19 — floating quote boxes also render in the
                    // full-screen editor.
                    quoteState = quoteCards,
                    onEditQuote = { editingQuoteIndex = it },
                    // v7.22 — the board chip's quotes float ON the board.
                    onAddQuote = { quoteCards.addCard(captionStyle, captionColor, onBoard = true) },
                    // v57 — quotes sit at their FULL-SCREEN placements here.
                    quotePositionsOverride = fullQuotePositions.toList(),
                    onMoveQuoteOverride = { i, x, y ->
                        while (fullQuotePositions.size <= i) {
                            fullQuotePositions.add(CaptureData.QuotePos(-1f, -1f))
                        }
                        val w = quoteCards.widths.getOrElse(i) { -1f }
                        fullQuotePositions[i] = CaptureData.QuotePos(x, y, w)
                    },
                    onResizeQuoteOverride = { i, w ->
                        // Widths are shared between the views; mirror the
                        // shared setWidth so the full-screen placement's own
                        // .w stays in sync (the card reads it on render).
                        quoteCards.setWidth(i, w)
                        while (fullQuotePositions.size <= i) {
                            fullQuotePositions.add(CaptureData.QuotePos(-1f, -1f))
                        }
                        val p = fullQuotePositions.getOrElse(i) { CaptureData.QuotePos(-1f, -1f) }
                        fullQuotePositions[i] = CaptureData.QuotePos(p.x, p.y, w)
                    }
                )
            }
        }
    }
}

/**
 * The editable mood-board canvas — shared by the inline card and the
 * full-screen expanded dialog so the same tile interactions (drag, tap to
 * front, drag-to-pin-zone, remove, add, clear) work at any size. Renders
 * tiles with [ContentScale.Fit] + padding exactly like the saved
 * EntryDetail view.
 */
@Composable
private fun MoodBoardCanvas(
    tiles: SnapshotStateList<MoodTile>,
    accent: Color,
    tint: Color,
    seed: Int,
    fullScreen: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    // v7.19 — floating quote boxes rendered INSIDE the board, over the
    // collage. Tapping a card calls [onEditQuote]; [onAddQuote] adds a new
    // card (which then appears on the board).
    quoteState: QuoteCardsState? = null,
    onEditQuote: (Int) -> Unit = {},
    onAddQuote: () -> Unit = {},
    // v57 — full-screen arrangement overrides: when provided, the canvas
    // renders and drags the floating quote cards against THIS position list
    // (the expanded board's own pixels) instead of [quoteState.positions],
    // and routes moves/resizes to the override callbacks. Null = the inline
    // board (cards live in [quoteState]).
    quotePositionsOverride: List<CaptureData.QuotePos>? = null,
    onMoveQuoteOverride: ((Int, Float, Float) -> Unit)? = null,
    onResizeQuoteOverride: ((Int, Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var canvasWPx by remember { mutableFloatStateOf(0f) }
    var canvasHPx by remember { mutableFloatStateOf(0f) }
    var draggingTileId by remember { mutableStateOf<Int?>(null) }
    var inPinZone by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    // Tile × deletion confirms first — edit-mode boards arrive prefilled, so
    // the × must never silently throw away a saved image (same protection as
    // the take-tab × in the universal editor).
    var pendingRemoveTileId by remember { mutableStateOf<Int?>(null) }
    // In-place tile zoom: double-tap springs the image up over the canvas —
    // no separate dialog page. Pinch/pan continue on the zoom overlay.
    // v7.19 — scale + pan animate inside [MoodBoardZoomOverlay] now
    // (call-site springs removed; close uses a fast tween so the minimize
    // animation snaps shut instead of lagging).
    val zoomState = rememberMoodBoardZoomState()
    val pinZoneHeightPx = with(density) { 52.dp.toPx() }
    // Smallest a tile can be pinched to — shared by the live drag preview
    // and the commit so what you see while dragging is exactly what saves.
    val minTilePx = with(density) { 60.dp.toPx() }
    // v7.23 — the INLINE editor renders through the same width-fit the saved
    // small card uses (scale to card width, centered; height-fit fallback
    // for wide/short boards) so the edit preview matches the saved view
    // exactly. Full-screen editing keeps the raw 1:1 board space for precise
    // placement. boardScale/boardOffsetX/Y are threaded into the tiles and
    // floating quote cards (display = raw * scale + offset; commits stay raw).
    //
    // v7.25 — the inline board is CENTERED inside the card, so raw-space
    // drags must be clamped to the BOARD's raw bounds, never the full canvas:
    // a tile clamped to [0, canvasW] could be dragged into the centered
    // margins (displayed outside the visible board — the "inaccurate" small-
    // view dragging). boardMaxX/Y = the collage's raw extent (full canvas in
    // the full-screen editor and on empty boards).
    // v69 — the inline editor fits EXACTLY like the saved card: the crop
    // extent is the CURRENT tile set's bounding box. The drag preview lives
    // inside the tile, so `tiles` only changes on commit — the fit stays
    // constant mid-drag (the tile follows the finger 1:1) and updates once
    // on release, exactly what the saved view recomputes from the saved
    // layouts. The old once-per-session freeze kept the editor stable but
    // diverged from the saved card the moment a tile was added or dragged
    // past the frozen extent — the saved view then re-fitted and the board
    // "resized" between edit and detail (a fresh board even showed 1:1
    // while the saved card zoomed to the content).
    //
    // v108 — FREEZE the fit once the board has content. The live re-fit
    // above made the whole board (tiles AND the floating quote cards on it)
    // visibly resize on every commit: drag a photo inward and the extent
    // shrank, the board zoomed in and every tile jumped — the "the size
    // changes when I move / expand / shrink photos" glitch. The session
    // extent freezes at the first content's bounding box and NEVER shrinks
    // for the rest of the session: every commit (drag, pinch, grow, add)
    // clamps tiles INSIDE it, so no gesture can exceed it — the scale only
    // changes when the board empties (reset to 0, so the next add re-freezes
    // at its own size). The saved card re-fits to the final saved layouts,
    // which all live inside this frozen extent, so edit and detail agree.
    val liveExtentX = tiles.maxOfOrNull {
        finiteOr(it.offsetXPx) + positiveFiniteOr(it.widthPx, 0f)
    }?.takeIf { it.isFinite() } ?: 0f
    val liveExtentY = tiles.maxOfOrNull {
        finiteOr(it.offsetYPx) + positiveFiniteOr(it.heightPx, 0f)
    }?.takeIf { it.isFinite() } ?: 0f
    var sessionExtentX by remember { mutableFloatStateOf(0f) }
    var sessionExtentY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tiles.size) {
        if (tiles.isEmpty()) {
            // Board emptied — clear the freeze so the next add re-freezes
            // at its own size instead of inheriting the old board's zoom.
            sessionExtentX = 0f
            sessionExtentY = 0f
        } else {
            // Grow-only: adopt the current content extent. (Adds can't
            // exceed it — placement clamps — so this fires effectively
            // once, at the board's first content.)
            if (liveExtentX > sessionExtentX) sessionExtentX = liveExtentX
            if (liveExtentY > sessionExtentY) sessionExtentY = liveExtentY
        }
    }
    val boardMaxX = if (fullScreen || tiles.isEmpty()) canvasWPx
        else positiveFiniteOr(sessionExtentX, positiveFiniteOr(liveExtentX, canvasWPx))
    val boardMaxY = if (fullScreen || tiles.isEmpty()) canvasHPx
        else positiveFiniteOr(sessionExtentY, positiveFiniteOr(liveExtentY, canvasHPx))
    val (boardScale, boardOffsetX, boardOffsetY) = if (fullScreen || tiles.isEmpty()) {
        Triple(1f, 0f, 0f)
    } else {
        val maxX = boardMaxX
        val maxY = boardMaxY
        val widthScale = if (maxX > 0f && canvasWPx.isFinite())
            positiveFiniteOr(canvasWPx / maxX, 1f)
        else 1f
        // Same height-fit fallback as the saved card: a width-fit board that
        // would shrink to a sliver (<55% of the card height) fills the height
        // instead, so wide/short collages stay presentable.
        val scale = if (maxY * widthScale < canvasHPx * 0.55f && maxY > 0f)
            canvasHPx / maxY else widthScale
        val boardW = maxX * scale
        val boardH = maxY * scale
        Triple(
            scale.takeIf { it.isFinite() && it > 0f } ?: 1f,
            ((canvasWPx - boardW) / 2f).takeIf { it.isFinite() } ?: 0f,
            ((canvasHPx - boardH) / 2f).takeIf { it.isFinite() } ?: 0f
        )
    }

    // v69 — the mood board imports through the ANDROID PHOTO PICKER (the
    // universal system picker: one consistent gallery/camera grid on every
    // device) instead of the raw documents UI.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        uris.forEach { uri ->
            // v6.1 — size each new tile to the photo's own aspect ratio so
            // ContentScale.Fit fills the rounded box with no bars/cropping.
            val bounds = decodeImageBounds(context, uri)
            val baseW = with(density) { (100..160).random().dp.toPx() }
            val baseH = with(density) { (120..180).random().dp.toPx() }
            val minPx = with(density) { 80.dp.toPx() }
            val maxPx = with(density) { 320.dp.toPx() }
            val (tileW, tileH) = if (bounds != null && bounds.second > 0) {
                val aspect = bounds.first.toFloat() / bounds.second.toFloat()
                if (aspect >= 1f) {
                    // Landscape: anchor width, derive height.
                    val w = baseW
                    val h = (w / aspect).coerceIn(minPx, maxPx)
                    w to h
                } else {
                    // Portrait: anchor height, derive width.
                    val h = baseH
                    val w = (h * aspect).coerceIn(minPx, maxPx)
                    w to h
                }
            } else {
                baseW to baseH
            }
            // v7.25 — new tiles land INSIDE the visible board (its raw
            // bounds), not the full canvas — the board is centered in the
            // inline card, so a tile placed in raw canvas space could render
            // in the empty margins. Empty board → the full canvas.
            val maxX = (boardMaxX - tileW).coerceAtLeast(0f)
            val maxY = (boardMaxY - tileH).coerceAtLeast(0f)
            tiles.add(
                MoodTile(
                    id = (tiles.maxOfOrNull { it.id } ?: -1) + 1,
                    uri = uri.toString(),
                    offsetXPx = if (maxX > 0f) Random.nextFloat() * maxX else 0f,
                    offsetYPx = if (maxY > 0f) Random.nextFloat() * maxY else 0f,
                    rotationDeg = (-12..12).random().toFloat(),
                    widthPx = tileW,
                    heightPx = tileH
                )
            )
        }
    }

    Surface(
        // RoundedCornerShape(0.dp) is a rectangle — RectangleShape isn't
        // available in the Compose BOM this project resolves.
        shape = if (fullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
        // The board background wears the category tint so the collage reads
        // as a tinted surface (same wash language as the page around it);
        // with the tint setting off it returns to a plain transparent board.
        // The AMOLED theme style does NOT black this out — the mood board's
        // tinted canvas is its identity, so only the manual Settings toggle
        // turns it off (tintWashEnabledState, not tintWashEffective).
        // v27n — the INLINE board's tint is OPAQUE (lerp of the accent into
        // the surface at the tint's 20% strength): a translucent board let
        // the elevation shadow bleed through as a blurry wash. The
        // full-screen editor keeps its translucent tint — it has no shadow
        // (shadowElevation 0), so nothing bleeds.
        color = if (AppPreferences.tintWashEnabledState) {
            if (fullScreen) tint else lerp(MaterialTheme.colorScheme.surface, accent, 0.20f)
        } else Color.Transparent,
        tonalElevation = 0.dp,
        // Faint accent rule — the board sits on the tinted page, so a slim
        // category-colored border keeps it from visually blending into the
        // wash (full-screen editor is on a plain dialog background, no need).
        shadowElevation = if (fullScreen) 0.dp else 2.dp,
        // v7.17 — the inline editor canvas draws ABOVE the caption field +
        // quote cards below it, so the double-tap zoom overlay (which
        // overflows the canvas) never hides behind them. The full-screen
        // dialog lives in its own window and needs no lift.
        modifier = if (fullScreen) Modifier.fillMaxSize() else Modifier
            .fillMaxWidth()
            .height(420.dp)
            .zIndex(1000f)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Read actual canvas size from constraints (fixes "stuck on left" bug)
            LaunchedEffect(maxWidth, maxHeight) {
                canvasWPx = with(density) { maxWidth.toPx() }
                canvasHPx = with(density) { maxHeight.toPx() }
            }

            // ── Theme-aware random watermark backdrop ─────────────────
            CurioMoodBoardBackdrop(
                seed = seed,
                accent = accent,
                modifier = Modifier.fillMaxSize()
            )

            // (No board-level pinch in the editor — single-finger drags move
            // tiles; image zoom is per-tile via double-tap or the magnifier.)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (tiles.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.12f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CurioIcon(
                                    name = CurioIcons.Image,
                                    contentDescription = null,
                                    tint = accent,
                                    size = 32.dp
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Start your mood board",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Add images, drag them around",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    tiles.forEachIndexed { i, tile ->
                        MoodBoardEditorTile(
                            tile = tile,
                            index = i,
                            isDragging = draggingTileId == tile.id,
                            canvasWPx = canvasWPx,
                            canvasHPx = canvasHPx,
                            pinZoneHeightPx = pinZoneHeightPx,
                            minTilePx = minTilePx,
                            onBringToFront = { id ->
                                val idx = tiles.indexOfFirst { it.id == id }
                                if (idx >= 0 && idx != tiles.lastIndex) tiles.add(tiles.removeAt(idx))
                            },
                            onRemove = { id ->
                                // Confirm before deleting a tile — the × sits on
                                // a real image, so route through the dialog.
                                pendingRemoveTileId = id
                            },
                            // v42 — grow-in-place: a photo that came out too
                            // small to pinch (two fingers can't fit on the
                            // tile) gets a tap-to-enlarge control that scales
                            // the tile up on the board, keeping its center.
                            onGrow = { id ->
                                val idx = tiles.indexOfFirst { it.id == id }
                                if (idx >= 0) {
                                    val t = tiles[idx]
                                    val factor = 1.45f
                                    val growW = (positiveFiniteOr(t.widthPx, minTilePx) * factor)
                                        .coerceIn(minTilePx, boardMaxX.coerceAtLeast(minTilePx))
                                    val growH = (positiveFiniteOr(t.heightPx, minTilePx) * factor)
                                        .coerceIn(minTilePx, boardMaxY.coerceAtLeast(minTilePx))
                                    val newW = positiveFiniteOr(growW, minTilePx)
                                    val newH = positiveFiniteOr(growH, minTilePx)
                                    val cw = positiveFiniteOr(boardMaxX, canvasWPx)
                                    val ch = positiveFiniteOr(boardMaxY, canvasHPx)
                                    // Keep the CENTER fixed so the growth feels
                                    // anchored, then clamp into the board.
                                    val newX = (finiteOr(t.offsetXPx) + (t.widthPx - newW) / 2f)
                                        .coerceIn(0f, (cw - newW).coerceAtLeast(0f))
                                    val newY = (finiteOr(t.offsetYPx) + (t.heightPx - newH) / 2f)
                                        .coerceIn(0f, (ch - newH).coerceAtLeast(0f))
                                    tiles[idx] = t.copy(
                                        offsetXPx = newX,
                                        offsetYPx = newY,
                                        widthPx = newW,
                                        heightPx = newH
                                    )
                                }
                            },
                            onZoomIn = { uri, tx, ty, tw, th, vw, vh ->
                                zoomState.zoomIn(
                                    uri,
                                    centerX = tx + tw / 2f,
                                    centerY = ty + th / 2f,
                                    tileW = tw,
                                    tileH = th,
                                    viewW = vw,
                                    viewH = vh
                                )
                            },
                            onDragStart = { draggingTileId = it },
                            onPinZoneChange = { if (it != inPinZone) inPinZone = it },
                            onCommit = { id, preview ->
                                val idx = tiles.indexOfFirst { it.id == id }
                                if (idx >= 0) {
                                    val t = tiles[idx]
                                    // Same clamps as the live preview (with the
                                    // same pre-measure fallback), so the tile
                                    // never snaps or collapses when released.
                                    // Drag deltas are SCREEN px — divide by the
                                    // board scale so commits land in raw space.
                                    // v7.25 — clamp to the BOARD's raw bounds
                                    // (matches the preview), so a committed tile
                                    // can never land in the centered margins.
                                    val cw = positiveFiniteOr(
                                        boardMaxX,
                                        positiveFiniteOr(canvasWPx, positiveFiniteOr(t.widthPx, minTilePx))
                                    )
                                    val ch = positiveFiniteOr(
                                        boardMaxY,
                                        positiveFiniteOr(canvasHPx, positiveFiniteOr(t.heightPx, minTilePx))
                                    )
                                    val safeScale = positiveFiniteOr(boardScale, 1f)
                                    val safeGestureScale = positiveFiniteOr(preview.scale, 1f)
                                    val safeDx = finiteOr(preview.dx)
                                    val safeDy = finiteOr(preview.dy)
                                    val baseW = positiveFiniteOr(t.widthPx, minTilePx)
                                    val baseH = positiveFiniteOr(t.heightPx, minTilePx)
                                    val newW = (baseW * safeGestureScale).coerceIn(minTilePx, cw.coerceAtLeast(minTilePx))
                                    val newH = (baseH * safeGestureScale).coerceIn(minTilePx, ch.coerceAtLeast(minTilePx))
                                    val newX = (finiteOr(t.offsetXPx) + safeDx / safeScale)
                                        .coerceIn(0f, (cw - newW).coerceAtLeast(0f))
                                    val newY = (finiteOr(t.offsetYPx) + safeDy / safeScale)
                                        .coerceIn(0f, (ch - newH).coerceAtLeast(0f))
                                    tiles[idx] = t.copy(
                                        offsetXPx = finiteOr(newX),
                                        offsetYPx = finiteOr(newY),
                                        widthPx = positiveFiniteOr(newW, minTilePx),
                                        heightPx = positiveFiniteOr(newH, minTilePx),
                                        rotationDeg = (finiteOr(t.rotationDeg) + finiteOr(preview.rotation)) % 360f
                                    )
                                    // Pin-to-front drop zone: releasing near
                                    // the top pins the tile to the front —
                                    // single-finger drags only, not pinches.
                                    // The zone is at the top of the DISPLAYED
                                    // board (scaled space).
                                    val displayY = finiteOr(newY) * positiveFiniteOr(boardScale, 1f) +
                                        finiteOr(boardOffsetY)
                                    if (preview.byDrag && displayY < pinZoneHeightPx && idx != tiles.lastIndex) {
                                        tiles.add(tiles.removeAt(idx))
                                    }
                                }
                            },
                            onDragEnd = { draggingTileId = null },
                            boardScale = boardScale,
                            boardOffsetX = boardOffsetX,
                            boardOffsetY = boardOffsetY,
                            boardMaxX = boardMaxX,
                            boardMaxY = boardMaxY
                        )
                    }
                }

                // ── Floating quote boxes (v7.20) — hand-placed paper notes
                // floating INSIDE the board over the collage. They start in
                // stable deterministic slots but are now DRAGGABLE: a drag
                // commits the card's position to the entry (persisted via
                // quotePositions), and the saved view renders it exactly
                // where it was left. Tap still opens the edit dialog (full
                // rich-text editor, color tool hidden). Gated on the
                // measured canvas size so the first layout frame
                // (canvasWPx=0) can't stack the cards at the top-left.
                if (quoteState != null && quoteState.quotes.isNotEmpty() &&
                    canvasWPx > 0f && canvasHPx > 0f
                ) {
                    MoodBoardFloatingCards(
                        quotes = quoteState.quotes.toList(),
                        styles = quoteState.styles.toList(),
                        colors = quoteState.colors.toList(),
                        tilts = quoteState.tilts.toList(),
                        positions = quotePositionsOverride ?: quoteState.positions.toList(),
                        // v7.22 — only on-board cards float here; below-board
                        // cards render under the board in their own section.
                        onBoard = quoteState.onBoard.toList(),
                        canvasWPx = canvasWPx,
                        canvasHPx = canvasHPx,
                        // v7.23 — match the inline fit so cards sit on the
                        // same scaled collage the tiles use.
                        boardScale = boardScale,
                        offsetX = boardOffsetX,
                        offsetY = boardOffsetY,
                        // v108 — the full-screen editor is raw 1:1 space:
                        // no display-width cap, so resized cards keep their
                        // exact raw width for precise placement.
                        rawSpace = fullScreen,
                        onEditCard = onEditQuote,
                        // v57 — the full-screen board routes moves to its own
                        // position list; the inline board uses the shared one.
                        onMoveCard = { i, x, y ->
                            if (onMoveQuoteOverride != null) onMoveQuoteOverride(i, x, y)
                            else quoteState.setPosition(i, x, y)
                        },
                        // v42 — a drag on the card's resize grip commits the
                        // card's new width (raw board px) to the entry.
                        // v57 — widths are SHARED between the views (a card
                        // property, not a placement), so both boards commit
                        // through the same setWidth.
                        onResizeCard = { i, w ->
                            if (onResizeQuoteOverride != null) onResizeQuoteOverride(i, w)
                            else quoteState.setWidth(i, w)
                        }
                    )
                }

                // ── Floating "Add quote" chip — bottom-left, mirroring
                // the Add-images button on the right. ────────────────────
                if (quoteState != null) {
                    Surface(
                        onClick = onAddQuote,
                        shape = RoundedCornerShape(28.dp),
                        color = accent.copy(alpha = 0.92f),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            // v7.23 — the full-screen editor's Clear-board button
                            // also lives at BottomStart; keep the Quote chip
                            // above it so both stay reachable (inline: 16dp).
                            .then(if (fullScreen) Modifier.navigationBarsPadding() else Modifier)
                            .padding(start = 16.dp, end = 16.dp, bottom = if (fullScreen) 88.dp else 16.dp)
                            .zIndex(60f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.FormatQuote,
                                contentDescription = null,
                                tint = pastelFillInk(accent),
                                size = 16.dp
                            )
                            Text(
                                text = "Quote",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = pastelFillInk(accent)
                            )
                        }
                    }
                }

                // ── Floating "+" add button ──────────────────────────
                Surface(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    color = accent,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .then(if (fullScreen) Modifier.navigationBarsPadding() else Modifier)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurioIcon(
                            name = CurioIcons.Add,
                            contentDescription = "Add images",
                            tint = pastelFillInk(accent),
                            size = 20.dp
                        )
                        Text(
                            text = "Add images",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = pastelFillInk(accent)
                        )
                    }
                }
            }                // ── Pin-to-front drop zone (appears while dragging) ──────
                if (draggingTileId != null) {
                    val highlight = inPinZone
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .then(if (fullScreen) Modifier.statusBarsPadding() else Modifier)
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                color = accent.copy(alpha = if (highlight) 0.3f else 0.13f),
                                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
                            )
                            .zIndex(500f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurioIcon(
                                name = CurioIcons.KeyboardArrowUp,
                                contentDescription = null,
                                tint = accent,
                                size = 18.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (highlight) "Release to pin to front" else "Drag here to pin to front",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = accent
                            )
                        }
                    }
                }

                // ── Clear board (expanded editor only, hidden when empty) ──
                if (fullScreen && tiles.isNotEmpty()) {
                    Surface(
                        onClick = { showClearConfirm = true },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .then(if (fullScreen) Modifier.navigationBarsPadding() else Modifier)
                            .padding(16.dp)
                            .zIndex(999f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                size = 15.dp
                            )
                            Text(
                                text = "Clear board",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // ── Expand / collapse button ──────────────────────────────
            Surface(
                onClick = { if (fullScreen) onCollapse() else onExpand() },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .then(if (fullScreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(10.dp)
                    .size(36.dp)
                    .zIndex(999f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        name = if (fullScreen) CurioIcons.Close else CurioIcons.Fullscreen,
                        contentDescription = if (fullScreen) "Collapse mood board" else "Expand mood board",
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 18.dp
                    )
                }
            }

            // ── In-place image zoom overlay (double-tap / search — no page) ──
            // v7.24 — glides the tapped image from its spot on the editor
            // canvas to the canvas center (arc), pinch/pan refine, tap closes.
            tiles.firstOrNull { it.uri == zoomState.zoomedUri }?.let { tile ->
                // v7.25 — the overlay lives in DISPLAY space (the canvas), and
                // the inline board is fit-scaled + centered, so report the
                // tile's DISPLAY position (raw × scale + offset), not its raw
                // board position — otherwise the zoom glides from / centers
                // on the wrong spot in the small view.
                MoodBoardZoomOverlay(
                    zoomState = zoomState,
                    tileUri = tile.uri,
                    tileX = finiteOr(tile.offsetXPx) * positiveFiniteOr(boardScale, 1f) + finiteOr(boardOffsetX),
                    tileY = finiteOr(tile.offsetYPx) * positiveFiniteOr(boardScale, 1f) + finiteOr(boardOffsetY),
                    widthPx = positiveFiniteOr(tile.widthPx, minTilePx) * positiveFiniteOr(boardScale, 1f),
                    heightPx = positiveFiniteOr(tile.heightPx, minTilePx) * positiveFiniteOr(boardScale, 1f),
                    viewW = canvasWPx,
                    viewH = canvasHPx
                )
            }
        }
    }


    if (showClearConfirm) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear mood board?") },
            text = { Text("Remove all ${tiles.size} images? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    tiles.clear()
                    showClearConfirm = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirm = false },
                    colors = curioDialogActionButtonColors()
                ) {
                    Text("Keep")
                }
            }
        )
    }

    // ── Confirm before removing a single tile via its × ──────────────
    if (pendingRemoveTileId != null) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { pendingRemoveTileId = null },
            title = { Text("Remove this image?") },
            text = { Text("This will delete the image from your mood board. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = pendingRemoveTileId
                    val idx = tiles.indexOfFirst { it.id == id }
                    if (idx >= 0) tiles.removeAt(idx)
                    pendingRemoveTileId = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRemoveTileId = null },
                    colors = curioDialogActionButtonColors()
                ) {
                    Text("Keep")
                }
            }
        )
    }
}

/**
 * One editable mood-board tile — the photo floats on the board with rounded
 * corners (no card/box behind it). One finger drags it, two fingers pinch to
 * resize + twist to rotate, a tap brings it to the front, and double-tap (or
 * the search button) zooms it in place.
 *
 * The drag/pinch gesture accumulates into a [TileDragPreview] held in
 * per-tile [remember] state — NOT into the [MoodTile] list — so a frame of
 * dragging recomposes ONLY this tile instead of mutating the list (and
 * re-firing the save pipeline) on every pointer move. The final values are
 * committed once through [onCommit] when the finger lifts.
 */
@Composable
private fun MoodBoardEditorTile(
    tile: MoodTile,
    index: Int,
    isDragging: Boolean,
    canvasWPx: Float,
    canvasHPx: Float,
    pinZoneHeightPx: Float,
    minTilePx: Float,
    onBringToFront: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    // v42 — grow a too-small tile in place (tap-to-enlarge for the pinch
    // that two fingers can't reach).
    onGrow: (Int) -> Unit,
    // v7.24 — (uri, tileX, tileY, widthPx, heightPx, viewW, viewH) in
    // VIEWPORT px so the single-image zoom glides from the tile's spot.
    onZoomIn: (String, Float, Float, Float, Float, Float, Float) -> Unit,
    onDragStart: (Int) -> Unit,
    onPinZoneChange: (Boolean) -> Unit,
    onCommit: (Int, TileDragPreview) -> Unit,
    onDragEnd: () -> Unit,
    // v7.23 — the inline editor renders through the SAME width-fit + center
    // the saved small card uses (boardScale/boardOffset), so the edit
    // preview matches the saved view exactly. Tiles stay stored in RAW
    // editor px; only the DISPLAY is scaled — drag deltas are divided back
    // by the scale so commits land in raw space (1.0 = full-screen editor).
    boardScale: Float = 1f,
    boardOffsetX: Float = 0f,
    boardOffsetY: Float = 0f,
    // v7.25 — the board's raw bounds (drag clamps). 0 = fall back to the
    // full canvas (full-screen editor / pre-measure first frame).
    boardMaxX: Float = 0f,
    boardMaxY: Float = 0f
) {
    val density = LocalDensity.current
    // Preview lives INSIDE the tile so per-frame writes recompose only this
    // tile (Compose scopes snapshot reads to the composable that reads them).
    val dragPreview = remember(tile.id) { mutableStateOf<TileDragPreview?>(null) }
    // pointerInput never restarts (its key is tile.id), so the gesture
    // coroutine must read the LATEST tile — never the first composition's.
    val currentTile by rememberUpdatedState(tile)
    // v7.25 — the board scale/offset also change as tiles commit (the inline
    // board re-fits to the collage), so the never-restarting gesture must
    // read them fresh too — stale values made drags/zoom drift in the small
    // view after the first commit.
    val currentBoardScale by rememberUpdatedState(boardScale)
    val currentBoardOffsetX by rememberUpdatedState(boardOffsetX)
    val currentBoardOffsetY by rememberUpdatedState(boardOffsetY)
    // v7.25 — onCommit is captured by the never-restarting pointerInput, so it
    // must also be the LATEST instance — the parent's lambda closes over the
    // current board bounds/scale, and a stale one would commit drags against
    // the pre-fit geometry (snap-back in the small view).
    val currentOnCommit by rememberUpdatedState(onCommit)

    // Before the canvas size is measured (first frame), fall back to the
    // tile's stored size so tiles never flash at 0x0 or drift to the corner.
    val canvasW = positiveFiniteOr(canvasWPx, positiveFiniteOr(tile.widthPx, 1f))
    val canvasH = positiveFiniteOr(canvasHPx, positiveFiniteOr(tile.heightPx, 1f))
    // v7.25 — RAW-space drag clamps are the BOARD's raw bounds (the visible
    // collage), not the full canvas: the inline board is centered inside the
    // card, so canvas-sized clamps let tiles slide into the margins. The
    // full-screen editor passes canvasW/H via boardMaxX/Y = canvas (1:1).
    val clampW = if (boardMaxX > 0f) boardMaxX else canvasW
    val clampH = if (boardMaxY > 0f) boardMaxY else canvasH
    val preview = dragPreview.value
    val scale = preview?.scale ?: 1f
    // Drag deltas arrive in SCREEN px; the display is scaled by boardScale,
    // so the raw-space delta is screenDelta / boardScale.
    val safeBoardScale = positiveFiniteOr(boardScale, 1f)
    val rawDx = finiteOr(preview?.dx ?: 0f) / safeBoardScale
    val rawDy = finiteOr(preview?.dy ?: 0f) / safeBoardScale
    // RAW-space tile geometry (the stored/committed space, clamped to the
    // raw board bounds).
    val safeClampW = positiveFiniteOr(clampW, canvasW)
    val safeClampH = positiveFiniteOr(clampH, canvasH)
    val safeScalePreview = positiveFiniteOr(scale, 1f)
    val safeTileW = positiveFiniteOr(tile.widthPx, minTilePx)
    val safeTileH = positiveFiniteOr(tile.heightPx, minTilePx)
    val rawW = (safeTileW * safeScalePreview).coerceIn(minTilePx, safeClampW.coerceAtLeast(minTilePx))
    val rawH = (safeTileH * safeScalePreview).coerceIn(minTilePx, safeClampH.coerceAtLeast(minTilePx))
    val rawX = (finiteOr(tile.offsetXPx) + rawDx)
        .coerceIn(0f, (safeClampW - rawW).coerceAtLeast(0f))
    val rawY = (finiteOr(tile.offsetYPx) + rawDy)
        .coerceIn(0f, (safeClampH - rawH).coerceAtLeast(0f))
    // SCALED display geometry — what the user sees (matches the saved card).
    val safeOffsetX = finiteOr(boardOffsetX)
    val safeOffsetY = finiteOr(boardOffsetY)
    val renderW = (rawW * safeBoardScale).takeIf { it.isFinite() } ?: minTilePx
    val renderH = (rawH * safeBoardScale).takeIf { it.isFinite() } ?: minTilePx
    val renderX = (rawX * safeBoardScale + safeOffsetX).takeIf { it.isFinite() } ?: 0f
    val renderY = (rawY * safeBoardScale + safeOffsetY).takeIf { it.isFinite() } ?: 0f
    val renderRotation = (tile.rotationDeg.takeIf { it.isFinite() } ?: 0f) +
        (preview?.rotation?.takeIf { it.isFinite() } ?: 0f)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    renderX.takeIf { it.isFinite() }?.roundToInt() ?: 0,
                    renderY.takeIf { it.isFinite() }?.roundToInt() ?: 0
                )
            }
            .zIndex(if (isDragging) 400f else index.toFloat())
            .pointerInput(tile.id) {
                detectTapGestures(
                    onTap = { onBringToFront(currentTile.id) },
                    // Double-tap zooms the image in place instead of opening
                    // a full-screen page.
                    onDoubleTap = {
                        // v7.25 — report the DISPLAY position (raw × scale +
                        // offset) so the zoom overlay glides from the tile's
                        // actual spot on the fit-scaled inline board.
                        val zoomScale = positiveFiniteOr(currentBoardScale, 1f)
                        onZoomIn(
                            currentTile.uri,
                            finiteOr(currentTile.offsetXPx) * zoomScale + finiteOr(currentBoardOffsetX),
                            finiteOr(currentTile.offsetYPx) * zoomScale + finiteOr(currentBoardOffsetY),
                            positiveFiniteOr(currentTile.widthPx, minTilePx) * zoomScale,
                            positiveFiniteOr(currentTile.heightPx, minTilePx) * zoomScale,
                            canvasW, canvasH
                        )
                    }
                )
            }
            .pointerInput(tile.id) {
                // One handler for every move: one finger drags the tile (with
                // pin-to-front drop zone); two fingers pinch to resize and
                // twist to rotate. Updates only the local preview state, so
                // nothing above this tile recomposes mid-gesture.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Touch slop gates only the START of a drag: total travel
                    // from the down point must cross slop before the tile is
                    // claimed, so a tiny jitter on an intended tap never
                    // flashes the pin-to-front zone. Once dragging, EVERY
                    // event delta applies 1:1 — gating each move by slop made
                    // slow/moderate drags stutter, because per-frame deltas
                    // at 60-120Hz sit far below slop and the tile only jumped
                    // when a single event happened to cross it.
                    val slop = viewConfiguration.touchSlop
                    var multiTouch = false
                    var dragged = false
                    var dx = 0f
                    var dy = 0f
                    var gestureScale = 1f
                    var gestureRotation = 0f
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            multiTouch = true
                            gestureScale *= event.calculateZoom()
                            gestureRotation += event.calculateRotation()
                            dragPreview.value = TileDragPreview(dx, dy, gestureScale, gestureRotation, byDrag = dragged)
                            event.changes.forEach { it.consume() }
                        } else if (pressed.size == 1 && !multiTouch) {
                            val change = pressed.first()
                            val dragAmount = change.position - change.previousPosition
                            if (!dragged && (change.position - down.position).getDistance() >= slop) {
                                dragged = true
                                onDragStart(tile.id)
                            }
                            if (dragged) {
                                change.consume()
                                dx += dragAmount.x
                                dy += dragAmount.y
                                dragPreview.value = TileDragPreview(dx, dy, gestureScale, gestureRotation, byDrag = true)
                                // Highlight the pin zone — compare against the
                                // tile's DISPLAY Y (raw × scale + offset), the
                                // same space the zone lives in; raw+screen
                                // deltas were mixed before v7.25.
                                val pinScale = positiveFiniteOr(currentBoardScale, 1f)
                                val pinY = (finiteOr(currentTile.offsetYPx) + finiteOr(dy) / pinScale) *
                                    pinScale + finiteOr(currentBoardOffsetY)
                                onPinZoneChange(pinY < pinZoneHeightPx)
                            }
                        }
                        if (pressed.isEmpty()) break
                    } while (true)

                    if (dragged || multiTouch) {
                        currentOnCommit(tile.id, TileDragPreview(dx, dy, gestureScale, gestureRotation, byDrag = dragged))
                    }
                    dragPreview.value = null
                    onDragEnd()
                }
            }
    ) {
        // Frameless: the photo itself is the tile — rounded corners, no card.
        // Rotate first, then clip, so the rounded shape rotates with the image
        // (clip-after-rotate would slice the corners off).
        Image(
            // v8.2 — FilterQuality.High lives on the Coil REQUEST inside
            // moodBoardPainter (the painter overload of Image lost its
            // filterQuality parameter in foundation 1.4+).
            painter = moodBoardPainter(tile.uri),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(
                    width = with(density) { renderW.coerceAtLeast(1f).toDp() },
                    height = with(density) { renderH.coerceAtLeast(1f).toDp() }
                )
                .rotate(renderRotation)
                .clip(RoundedCornerShape(14.dp))
        )

        // ── Zoom-in-place button (bottom-end) ─────────────────────────
        Surface(
            onClick = {
                // v7.25 — display coords (raw × scale + offset), matching the
                // double-tap path above.
                val zoomScale = positiveFiniteOr(boardScale, 1f)
                onZoomIn(
                    tile.uri,
                    finiteOr(tile.offsetXPx) * zoomScale + finiteOr(boardOffsetX),
                    finiteOr(tile.offsetYPx) * zoomScale + finiteOr(boardOffsetY),
                    positiveFiniteOr(tile.widthPx, minTilePx) * zoomScale,
                    positiveFiniteOr(tile.heightPx, minTilePx) * zoomScale,
                    canvasW, canvasH
                )
            },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.48f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CurioIcon(
                    name = CurioIcons.Search,
                    contentDescription = "Zoom image",
                    tint = Color.White,
                    size = 16.dp
                )
            }
        }

        // ── Grow-in-place button (bottom-start) — v42: enlarges a too-small
        // photo on the board (two fingers can't pinch a tiny tile). Mirrors
        // the × on the opposite corner.
        Surface(
            onClick = { onGrow(tile.id) },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.48f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CurioIcon(
                    name = CurioIcons.PhotoSizeSelectLarge,
                    contentDescription = "Enlarge image",
                    tint = Color.White,
                    size = 16.dp
                )
            }
        }

        // ── × Remove button ───────────────────────────────────────────
        Surface(
            onClick = { onRemove(tile.id) },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CurioIcon(
                    name = CurioIcons.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    size = 13.dp
                )
            }
        }
    }
}

/**
 * Cheap header-only decode of a content-URI image's pixel bounds — used to
 * size each new mood-board tile to the photo's own aspect ratio so
 * [ContentScale.Fit] fills the rounded box with no bars or cropping.
 * Returns null when the image can't be read or has no dimensions.
 */
private fun decodeImageBounds(context: Context, uri: Uri): Pair<Int, Int>? = runCatching {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)
    }
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return@runCatching null
    var width = opts.outWidth
    var height = opts.outHeight
    // Photos shot sideways carry EXIF rotation; Coil renders them rotated, so
    // swap the raw sensor bounds to match the on-screen aspect. Without this,
    // tiles get sized to the wrong aspect and ContentScale.Fit letterboxes.
    // (Framework android.media.ExifInterface exposes the raw orientation tag —
    // no `rotationDegrees` property — so map the ORIENTATION_* constants.)
    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL
    val rotationDeg = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
    if (rotationDeg == 90 || rotationDeg == 270) {
        val swap = width
        width = height
        height = swap
    }
    width to height
}.getOrNull()

// ═══════════════════════════════════════════════════════════════════════════
// Floating quote boxes (v7.19) — hand-placed paper notes that float INSIDE
// the mood board over the collage. Slots are deterministic from the card
// index (a stable bottom rail), so revisits and the saved view look stable.
// ═══════════════════════════════════════════════════════════════════════════


/**
 * Full edit dialog for one floating mood-board quote box — reuses the
 * shared [QuoteCardEditor] (rich text toolbar + paper style) with the
 * note-paper COLOR tool hidden (v7.19).
 */
@Composable
private fun FloatingQuoteEditDialog(
    state: QuoteCardsState,
    index: Int,
    accent: Color,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurioIcon(
                        name = CurioIcons.FormatQuote,
                        contentDescription = null,
                        tint = accent,
                        size = 18.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Edit quote ${index + 1}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onClose) {
                        Text("Done", color = accent)
                    }
                }

                QuoteCardEditor(
                    index = index,
                    state = state,
                    enabled = true,
                    accent = accent,
                    placeholder = "\u201C...\u201D",
                    // Color tool hidden — text + paper style stay fully
                    // functional (the mood-board ask); the header Remove is
                    // hidden too because the dialog must close after removal
                    // (a bare remove would leave a stale index open).
                    showColorTool = false,
                    showRemove = false
                )

                TextButton(
                    onClick = {
                        state.removeCard(index)
                        onClose()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Remove quote", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}


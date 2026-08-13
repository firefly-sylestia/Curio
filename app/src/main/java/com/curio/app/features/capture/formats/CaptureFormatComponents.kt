package com.curio.app.features.capture.formats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.data.CaptureData
import com.curio.app.data.JournalMood
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.data.TextSpan
import com.curio.app.ui.components.NotePaperCard
import com.curio.app.ui.components.NotePaperColorToggle
import com.curio.app.ui.components.NotePaperStyleToggle
import com.curio.app.ui.components.RichTextEditor
import com.curio.app.ui.components.RichTextToolbarMode
import com.curio.app.ui.components.QuoteLimits
import com.curio.app.ui.components.limitQuoteContent
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.glyph
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.notePaperInk
import com.curio.app.ui.theme.paperAccent
import com.curio.app.ui.theme.paperBorder
import com.curio.app.ui.theme.paperInk
import com.curio.app.ui.theme.paperSurface
import com.curio.app.ui.theme.PatrickHandFontFamily
import coil.compose.rememberAsyncImagePainter
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared composable components used by the 6 capture format bodies.
 *
 * Each format owns its own state; these are pure render-only widgets
 * that get composed into the format-specific bodies. Keeping them here
 * avoids copy-paste across the format files (Reel Notes + Open Notebook
 * both need ImageThumb, Field Notes uses CollapsibleSectionHeader, etc.).
 */

/**
 * How many images a take may attach — every capture format caps its picker
 * results at this. Generous on purpose (v7.38): the saved-entry gallery
 * adapts to any count, so there's no reason to keep the old 3/6 limits.
 */
const val MaxAttachedImages = 30

/**
 * A FILLED 5-pointed star with a crisp outline — drawn as a Canvas path
 * with a dimensional crown-to-base gradient.
 *
 * The bundled icon font is Material Symbols *Outlined*, where even the
 * `star` ligature renders as a hollow outline — so filled rating stars
 * were just outlines with a tint. Drawing the path directly guarantees a
 * solid fill in any color. When [filled] is false the star renders as a
 * clean HOLLOW outline (an empty slot), so a rating row reads
 * filled-or-slot, never filled-or-ghost-blob. Both states carry the same
 * crisp outline stroke.
 */
@Composable
fun FilledStar(
    color: Color,
    starSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    Canvas(modifier = modifier.size(starSize)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val star = Path()
        // 5-pointed star: outer points at full radius, inner at ~45% — a
        // slightly fuller, sleeker silhouette than the old 42%.
        for (i in 0 until 10) {
            val angle = Math.toRadians(-90.0 + i * 36.0)
            val r = if (i % 2 == 0) radius else radius * 0.45f
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        val outlineWidth = this.size.minDimension * 0.07f
        if (filled) {
            // Dimensional fill — a vertical gradient from a lighter crown to
            // the base color, so the star reads as lit from above instead of
            // a flat blob. The gradient spans the whole canvas and the outer
            // points touch its edges, so the crown lands on the top points.
            // The crown lightens only moderately (18%) so deep accents get
            // a real shine while bright pastels (dark-mode ink) don't wash
            // out.
            drawPath(
                star,
                brush = Brush.verticalGradient(
                    0f to lerp(color, Color.White, 0.18f),
                    1f to color
                )
            )
            // Crisp outline — the same hue at high alpha, so the filled star
            // keeps a sharp silhouette on light or dark backgrounds.
            drawPath(
                star,
                color = color.copy(alpha = 0.85f),
                style = Stroke(width = outlineWidth)
            )
        } else {
            // Empty slot — a clean hollow outline (no fill), so an unrated
            // star reads as an available slot instead of a washed ghost.
            drawPath(
                star,
                color = color.copy(alpha = 0.38f),
                style = Stroke(width = outlineWidth)
            )
        }
    }
}

/**
 * 1-5 star rating row — CURIO_SPEC §8.2 ReelNotes ("optional star rating,
 * 1-5"). Tap a star to set; tap the currently-set star to clear (return
 * to 0). [accent] controls the filled-star color in the category palette.
 * Stars are [FilledStar] Canvas paths — solid fills with an outline stroke.
 */
@Composable
fun StarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(5) { i ->
            val starNumber = i + 1
            val filled = i < rating
            // Filled stars sit slightly elevated — the same bouncy pop the
            // mood chips use, so selecting a star reads as a physical
            // press-and-settle instead of a hard state snap.
            val starScale by animateFloatAsState(
                targetValue = if (filled) 1.14f else 1f,
                animationSpec = CurioMotion.Springs.Bouncy,
                label = "ratingStarScale$starNumber"
            )
            FilledStar(
                color = if (filled) accent else MaterialTheme.colorScheme.outline,
                filled = filled,
                starSize = 32.dp,
                modifier = Modifier
                    .scale(starScale)
                    .semantics {
                        // Canvas has no automatic label — restore the label
                        // the old CurioIcon used to carry.
                        contentDescription = "$starNumber star${if (starNumber == 1) "" else "s"}"
                    }
                    .clickable {
                        onRatingChange(if (rating == starNumber) 0 else starNumber)
                    }
            )
        }
    }
}

/**
 * Small image placeholder thumbnail — 80dp square with rounded corners.
 * Used by Reel Notes (poster/still attach) and Field Notes (single photo
 * attach). [onClick] opens the image in lightbox (Phase 4), [onRemove]
 * detaches the image from the format's state list.
 */
@Composable
fun ImageThumb(
    index: Int,
    accent: Color,
    tint: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    imageUri: String? = null
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Attached image $index",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                CurioIcon(
                    name = CurioIcons.Image,
                    contentDescription = "Attached image $index",
                    tint = accent,
                    size = 28.dp
                )
            }
        }
        // v27k — the remove badge: a small cross on a dark scrim at the top-
        // end corner so it reads over any photo. [onRemove] was already wired
        // by every format (Field Notes, Reel Notes, Marginalia) but never
        // rendered — attached images had no way to be deleted.
        if (imageUri != null) {
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CurioIcon(
                        name = CurioIcons.Close,
                        contentDescription = "Remove attached image",
                        tint = Color.White,
                        size = 14.dp
                    )
                }
            }
        }
    }
}

/**
 * Dashed-outline placeholder for adding a new image — used as a sibling
 * tile next to existing [ImageThumb]s. Tapping invokes [onClick] which
 * the format uses to append a placeholder to its image list.
 */
@Composable
fun AddImageButton(
    accent: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Add"
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = tint,
        shadowElevation = 2.dp,
        modifier = modifier.size(80.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = "Add image",
                    tint = accent,
                    size = 22.dp
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}

/**
 * Collapsible section header with chevron — CURIO_SPEC §12.2 disclosure
 * pattern. Used by Field Notes (§8.5) and can be reused elsewhere.
 *
 * When [expanded] is false and [preview] is non-null, the preview snippet
 * is shown trailing the header so the user can see a glimpse of what's
 * hidden inside without expanding (per spec §12.2: "If a section has
 * content and gets collapsed, show a single-line grey preview snippet").
 */
@Composable
fun CollapsibleSectionHeader(
    label: String,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    preview: String? = null
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurioIcon(
                name = if (expanded) CurioIcons.KeyboardArrowUp
                        else CurioIcons.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = accent,
                size = 20.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (!expanded && preview != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}

/**
 * A single-line text field that wears the note-paper look — a small paper
 * slip with the text written in paper ink (cream in both themes), so short
 * inputs like titles and captions match the notebook style of the rich-text
 * fields instead of a plain outline box. [paperStyle] picks the slip: the
 * classic ruled [PaperCard], a torn note, or a torn note with ruled lines.
 * A thin [label] sits above the slip (same label language as the other
 * format fields); the field itself is the paper, no inner box or borders.
 * When [onPaperStyleChange] is provided, a compact Ruled/Torn/rules toggle
 * renders next to the label so the field keeps its own paper style.
 */
@Composable
fun PaperLineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    enabled: Boolean = true,
    accent: Color = paperAccent(),
    imeAction: ImeAction = ImeAction.Done,
    /** Note-paper style — ruled page / torn note / torn with ruled lines. */
    paperStyle: NotePaperStyle = NotePaperStyle.RULED,
    /** When provided, shows the per-field style toggle next to the label. */
    onPaperStyleChange: ((NotePaperStyle) -> Unit)? = null,
    /** Note-paper COLOR — cream / butter / pink / mint / sky / lilac. */
    paperColor: NotePaperColor = NotePaperColor.CREAM,
    /** When provided, shows the per-field color swatches next to the label. */
    onPaperColorChange: ((NotePaperColor) -> Unit)? = null,
    /** Optional trailing action (e.g. a small dictation button) shown at the
     *  field's top-right, level with the label. */
    trailingAction: (@Composable () -> Unit)? = null
) {
    // Keep the label, paper/format controls, and paper field visually grouped;
    // the old 8dp rhythm made the compact controls look like separate
    // sections and consumed too much editing space.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (label != null || trailingAction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                trailingAction?.invoke()
            }
        }
        // The style chips live on their OWN full-width scrollable row — six
        // styles + the rules chip overflow a phone-width label row (Rows
        // don't wrap), so the chips scroll instead of pushing the label off.
        if (onPaperStyleChange != null) {
            NotePaperStyleToggle(
                style = paperStyle,
                onStyleChange = onPaperStyleChange,
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // The color swatches live on their OWN row behind a toggle chip.
        if (onPaperColorChange != null) {
            NotePaperColorToggle(
                color = paperColor,
                onColorChange = onPaperColorChange,
                accent = accent
            )
        }
        // Ink follows the chosen sheet color so text stays readable on every
        // pastel — resolved in the composable scope, then passed in.
        val ink = notePaperInk(paperColor)
        // NotePaperCard dispatches EVERY style (ruled / torn / torn+rules /
        // coffee / folded / red-margin) so the single-line field wears the
        // same paper look as the rich-text fields — one source of truth.
        NotePaperCard(
            style = paperStyle,
            modifier = Modifier.fillMaxWidth(),
            paperColor = paperColor,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = PatrickHandFontFamily,
                    color = ink
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = PatrickHandFontFamily,
                                            color = ink.copy(alpha = 0.45f)
                                        )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Quote cards — the hand-placed paper notecards (originally Marginalia's
// "Favorite quotes") extracted into a SHARED section so Reel Notes, Sound
// Bite and the Mood Board can offer the same quote field without copying
// the machinery. One [QuoteCardsState] per format body owns the parallel
// lists (text / spans / tilt / style / color); [QuoteCardsSection] renders
// the header + cards + add button; [RenderQuoteCards] is the saved-view
// counterpart in EntryDetailScreen.
// ═══════════════════════════════════════════════════════════════════════════

/** Hand-placed tilt in degrees (−2.5°..2.5°) for a quote card. */
private fun randomQuoteTilt(): Float = kotlin.random.Random.nextFloat() * 5f - 2.5f

/**
 * State holder for the reusable "Favorite quotes" section — owns the five
 * PARALLEL lists that describe every card (text, rich-text spans, hand-
 * placed tilt, per-card paper style, per-card paper color). All mutations
 * go through [addCard] / [removeCard] / [setText] / [setStyle] / [setColor]
 * so the lists never drift out of sync.
 */
class QuoteCardsState(
    initialQuotes: List<String>,
    initialSpans: List<List<TextSpan>>,
    initialTilts: List<Float>,
    initialStyles: List<NotePaperStyle>,
    initialColors: List<NotePaperColor>,
    defaultStyle: NotePaperStyle,
    defaultColor: NotePaperColor,
    initialPositions: List<CaptureData.QuotePos> = emptyList(),
    initialOnBoard: List<Boolean> = emptyList()
) {
    private val limitedInitial = initialQuotes.mapIndexed { index, quote ->
        limitQuoteContent(quote, initialSpans.getOrNull(index).orEmpty())
    }
    val quotes = mutableStateListOf<String>().apply {
        addAll(limitedInitial.map { it.first })
    }
    val spans = mutableStateListOf<List<TextSpan>>().apply {
        addAll(limitedInitial.map { it.second })
        // Pad any missing per-quote span lists (legacy entries) so the
        // parallel list always matches quotes 1:1.
        while (size < quotes.size) add(emptyList())
    }
    // Hand-placed tilt per card — generated ONCE when a card is born and
    // saved with the entry, so the angle the user adds with is the angle
    // that persists (never re-rolled by recomposition or revisits).
    val tilts = mutableStateListOf<Float>().apply {
        addAll(initialTilts)
        while (size < quotes.size) add(randomQuoteTilt())
    }
    val styles = mutableStateListOf<NotePaperStyle>().apply {
        addAll(initialStyles)
        // Pad any missing per-quote styles so the list matches 1:1.
        while (size < quotes.size) add(defaultStyle)
    }
    val colors = mutableStateListOf<NotePaperColor>().apply {
        addAll(initialColors)
        // Pad any missing per-quote colors so the list matches 1:1.
        while (size < quotes.size) add(defaultColor)
    }
    // v7.20 — per-card placement for the mood board's floating quote boxes
    // (editor board pixels). (-1,-1) = never dragged → renderers fall back
    // to the deterministic slot for that index. Legacy entries lack the
    // field → every card starts as a slot.
    val positions = mutableStateListOf<CaptureData.QuotePos>().apply {
        addAll(initialPositions)
        while (size < quotes.size) add(CaptureData.QuotePos(-1f, -1f))
    }
    // v7.22 — per-card placement flag: true = floats ON the board (added via
    // the board's Quote chip), false = renders BELOW the board (added via
    // the bottom Add-quote button). Parallel to quotes; legacy entries lack
    // the field → every card defaults to on-board (the v7.19 look).
    val onBoard = mutableStateListOf<Boolean>().apply {
        addAll(initialOnBoard)
        while (size < quotes.size) add(true)
    }

    /** Whether any card has real text — drives the format's canSave. */
    val hasContent: Boolean get() = quotes.any { it.isNotBlank() }

    fun addCard(style: NotePaperStyle, color: NotePaperColor, onBoard: Boolean = true) {
        quotes.add("")
        spans.add(emptyList())
        // A fresh card gets its own tilt — and it STAYS that way.
        tilts.add(randomQuoteTilt())
        styles.add(style)
        colors.add(color)
        // A fresh card starts at the deterministic slot until dragged.
        positions.add(CaptureData.QuotePos(-1f, -1f))
        this.onBoard.add(onBoard)
    }

    fun removeCard(index: Int) {
        if (index !in quotes.indices) return
        quotes.removeAt(index)
        if (index < spans.size) spans.removeAt(index)
        if (index < tilts.size) tilts.removeAt(index)
        if (index < styles.size) styles.removeAt(index)
        if (index < colors.size) colors.removeAt(index)
        if (index < positions.size) positions.removeAt(index)
        if (index < onBoard.size) onBoard.removeAt(index)
    }

    /** v7.20 — the mood board commits a dragged card's new top-left here. */
    fun setPosition(index: Int, x: Float, y: Float) {
        if (index !in positions.indices) return
        positions[index] = CaptureData.QuotePos(x, y)
    }

    fun setText(index: Int, text: String, cardSpans: List<TextSpan>) {
        if (index !in quotes.indices) return
        val (limitedText, limitedSpans) = limitQuoteContent(text, cardSpans)
        quotes[index] = limitedText
        spans[index] = limitedSpans
    }

    fun setStyle(index: Int, style: NotePaperStyle) {
        if (index in styles.indices) styles[index] = style
    }

    fun setColor(index: Int, color: NotePaperColor) {
        if (index in colors.indices) colors[index] = color
    }
}

/**
 * Creates the [QuoteCardsState] for a format body, seeded from saved
 * [CaptureData] (edit mode) with [defaultStyle]/[defaultColor] as the
 * fallback paper look for legacy entries that predate per-card styles.
 */
@Composable
fun rememberQuoteCardsState(
    initialQuotes: List<String>,
    initialSpans: List<List<TextSpan>>,
    initialTilts: List<Float>,
    initialStyles: List<NotePaperStyle>,
    initialColors: List<NotePaperColor>,
    defaultStyle: NotePaperStyle,
    defaultColor: NotePaperColor,
    initialPositions: List<CaptureData.QuotePos> = emptyList(),
    initialOnBoard: List<Boolean> = emptyList()
): QuoteCardsState = remember(
    initialQuotes, initialSpans, initialTilts, initialStyles, initialColors,
    defaultStyle, defaultColor, initialPositions, initialOnBoard
) {
    QuoteCardsState(
        initialQuotes, initialSpans, initialTilts, initialStyles, initialColors,
        defaultStyle, defaultColor, initialPositions, initialOnBoard
    )
}

/**
 * The shared "Favorite quotes" editor section — a header row with the card
 * count, one hand-placed paper notecard per quote (each with its own
 * rich-text toolbar + Ruled/Torn/color toggles + Remove), and a dashed
 * "Add quote" button. [newCardStyle]/[newCardColor] supply the paper look
 * NEW cards inherit (e.g. the format's primary field), and [enabled] lets
 * recording formats freeze the cards mid-capture.
 *
 * v7.19 — [showColorTool] hides each card's note-paper COLOR swatches (the
 * mood board keeps the picker hidden while keeping full text editing), and
 * [cardsInline] = false renders ONLY the header + Add button (the cards
 * themselves float on the caller's own canvas instead — the mood board).
 *
 * v7.22 — [cardsFilter] narrows which cards render inline (and the header
 * count) to the indices that pass the predicate — the mood board's bottom
 * section shows only its BELOW-board cards while the on-board ones float on
 * the collage. [onAddCard] overrides what the Add button creates (the mood
 * board adds a below-board card with onBoard = false).
 */
@Composable
fun QuoteCardsSection(
    state: QuoteCardsState,
    modifier: Modifier = Modifier,
    header: String = "Favorite quotes",
    placeholder: String = "\u201C...\u201D",
    enabled: Boolean = true,
    accent: Color = paperAccent(),
    newCardStyle: () -> NotePaperStyle = { NotePaperStyle.RULED },
    newCardColor: () -> NotePaperColor = { NotePaperColor.CREAM },
    showColorTool: Boolean = true,
    cardsInline: Boolean = true,
    cardsFilter: ((Int) -> Boolean)? = null,
    onAddCard: (() -> Unit)? = null
) {
    // Indices rendered inline + counted: the filtered subset when a filter
    // is given (mood board below-board cards), ALL cards otherwise.
    val visibleIndices = state.quotes.indices.filter { i -> cardsFilter?.invoke(i) ?: true }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = header,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (visibleIndices.isNotEmpty()) {
                Text(
                    text = "${visibleIndices.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (cardsInline) {
            visibleIndices.forEach { i ->
                QuoteCardEditor(
                    index = i,
                    state = state,
                    enabled = enabled,
                    accent = accent,
                    placeholder = placeholder,
                    showColorTool = showColorTool
                )
            }
        }

        // Add-quote button (dashed-outline placeholder style)
        Surface(
            onClick = { onAddCard?.invoke() ?: state.addCard(newCardStyle(), newCardColor()) },
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            color = paperSurface().copy(alpha = 0.6f),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = null,
                    // NOT theme-aware: the cream button keeps the warm paper
                    // ink in both themes — the theme's onSurfaceVariant turns
                    // light-gray in dark mode and washes out on the cream.
                    tint = paperInk(),
                    size = 18.dp
                )
                Text(
                    text = "Add quote",
                    style = MaterialTheme.typography.labelLarge,
                    color = paperInk()
                )
            }
        }
    }
}

/**
 * One quote card editor — a hand-placed paper notecard (rotated a few
 * degrees) with its own rich-text toolbar + paper-style/color toggles. The
 * header row + toolbar render ABOVE the paper slip so the ruled lines line
 * up under the text while typing.
 *
 * v7.19 — [showColorTool] = false hides the note-paper color swatches
 * (full text formatting + paper style still available); used by the mood
 * board's floating quote boxes. [showRemove] = false hides the header's
 * Remove button (the mood board's dialog owns removal and must close
 * after it — a bare remove would leave a stale card index open). Public
 * so canvases (the mood board) can open a card in their own dialog.
 */
@Composable
fun QuoteCardEditor(
    index: Int,
    state: QuoteCardsState,
    enabled: Boolean,
    accent: Color,
    placeholder: String,
    showColorTool: Boolean = true,
    showRemove: Boolean = true
) {
    // The tilt SAVED with this card — generated at creation, never re-rolled
    // by recomposition, typing, or section switches.
    val rotation = state.tilts.getOrElse(index) { randomQuoteTilt() }
    val style = state.styles.getOrElse(index) { NotePaperStyle.RULED }
    val color = state.colors.getOrElse(index) { NotePaperColor.CREAM }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(rotation)
    ) {
        // ── Card header — quote mark + number, Remove on the right ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurioIcon(
                name = CurioIcons.FormatQuote,
                contentDescription = null,
                tint = paperInk().copy(alpha = 0.55f),
                size = 16.dp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Quote ${index + 1}",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = paperInk().copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            if (showRemove) {
                Surface(
                    onClick = { state.removeCard(index) },
                    enabled = enabled,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        RichTextEditor(
            modifier = Modifier.fillMaxWidth(),
            text = state.quotes.getOrElse(index) { "" },
            spans = state.spans.getOrElse(index) { emptyList() },
            onRichTextChange = { newText, newSpans -> state.setText(index, newText, newSpans) },
            placeholder = placeholder,
            toolbarMode = RichTextToolbarMode.MAIN,
            minHeight = 64.dp,
            maxCharacters = QuoteLimits.MAX_CHARACTERS,
            maxLines = QuoteLimits.MAX_LINES,
            enabled = enabled,
            ink = paperInk(),
            accent = accent,
            paper = true,
            paperStyle = style,
            onPaperStyleChange = { state.setStyle(index, it) },
            paperColor = color,
            onPaperColorChange = { state.setColor(index, it) },
            // v7.19 — the color swatch picker is gated inside
            // [RichTextEditor] by [showColorTool] (the mood board hides it).
            showColorTool = showColorTool,
            paperContentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

/**
 * The shared "How did it make you feel?" mood row — a horizontally
 * scrollable chip row of every [JournalMood]. Tap a mood to set it, tap
 * again to clear. Rendered ONCE per capture screen (right above the format
 * options, by the caller) so every format/take shares the same picker; the
 * saved-entry meta card reads the stored mood.
 *
 * Selection is animated: the chosen chip pops up on a bouncy spring while
 * its fill + ink crossfade to the accent, so changing moods reads as a
 * physical press-and-settle instead of a hard state snap.
 */
@Composable
fun MoodChipsRow(
    mood: JournalMood?,
    accent: Color,
    onMoodChange: (JournalMood?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "How did it make you feel?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            JournalMood.entries.forEach { m ->
                val selected = mood == m
                // Selected chip springs up (gummy overshoot) + its fill and
                // ink crossfade to the accent — the change animates instead
                // of snapping, matching Curio's chip-selection language.
                val chipScale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 1f,
                    animationSpec = CurioMotion.Springs.Bouncy,
                    label = "moodChipScale"
                )
                val chipBg by animateColorAsState(
                    targetValue = if (selected) accent
                                  else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(CurioMotion.Durations.Standard),
                    label = "moodChipBg"
                )
                val chipInk by animateColorAsState(
                    // v7.5 — pastel mode lightens the selected chip fill, so
                    // the ink flips to a deep ink of the accent.
                    targetValue = if (selected) pastelFillInk(accent)
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(CurioMotion.Durations.Standard),
                    label = "moodChipInk"
                )
                Surface(
                    onClick = { onMoodChange(if (selected) null else m) },
                    shape = RoundedCornerShape(50),
                    color = chipBg,
                    shadowElevation = if (selected) 3.dp else 1.dp,
                    modifier = Modifier.scale(chipScale)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurioIcon(
                            name = m.glyph,
                            contentDescription = null,
                            tint = chipInk,
                            size = 16.dp
                        )
                        Text(
                            text = m.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = chipInk
                        )
                    }
                }
            }
        }
    }
}
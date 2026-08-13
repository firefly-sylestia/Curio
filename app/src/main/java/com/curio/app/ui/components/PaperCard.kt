package com.curio.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioCategories
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.notePaperBorder
import com.curio.app.ui.theme.notePaperInk
import com.curio.app.ui.theme.notePaperRule
import com.curio.app.ui.theme.notePaperSurface
import com.curio.app.ui.theme.paperAccent
import com.curio.app.ui.theme.paperInk
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * A note-paper card — the quotes entry's surface instead of the category
 * tint. Warm cream paper (both themes), with faint horizontal ruled lines
 * (notebook texture) and a soft hairline edge. [rotation] keeps the
 * hand-placed notecard feel in the saved view.
 *
 * Decoration flags extend the plain ruled page into the other notebook
 * styles: [redMargin] draws the classic school-notebook red vertical margin
 * line (text indented past it), [coffeeStains] spills deterministic coffee
 * blotches along the edges, and [folded] folds the top-right corner into a
 * dog-ear (corner cut by a diagonal, flap + crease shadow drawn over it;
 * the content is padded so text never runs under the flap).
 *
 * [contentPadding] defaults to a COMPACT inset (12dp) so quote cards stay
 * tight; pass a larger value for the journal page.
 */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    ruled: Boolean = true,
    rotation: Float = 0f,
    // v7.30 — normal paper now keeps the hero's soft torn bottom seam.
    // [roundedTop] is the selectable normal-paper option for rounded top
    // edges; [corner] remains a compatibility inset for quote callers.
    roundedTop: Boolean = false,
    corner: Dp = 0.dp,
    paperColor: NotePaperColor = NotePaperColor.CREAM,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    /** Minimum card height — a floor so a single-line field still reads as
     *  a proper note slip instead of collapsing to text + padding. The
     *  editor's text fields enforce 96dp; saved views pass the same so the
     *  note keeps its shape between edit and detail. */
    minHeight: Dp = 0.dp,
    /** Red school-notebook margin — a vertical red rule near the left edge
     *  with the text indented past it (the classic ruled-with-red-margin
     *  page). */
    redMargin: Boolean = false,
    /** Coffee-stain blotches along the paper's edges — deterministic per
     *  size (seeded), so typing / recomposition never re-rolls them. */
    coffeeStains: Boolean = false,
    /** Folded (dog-ear) top-right corner — the corner is cut by a diagonal
     *  and the folded flap + crease shadow drawn over it. */
    folded: Boolean = false,
    /** Explicit ruled-line cadence (Dp) — defaults to the bodyLarge line
     *  height. Saved views pass the same value as their note text's
     *  lineHeight so the drawn rules align with the actual text lines
     *  (v7.31 reading bump: bigger text + roomier rules). */
    ruleSpacing: Dp? = null,
    /** Extra bottom space (Dp) — blank ruled lines at the end of the note
     *  (v7.33: saved views pass ~2 lines so a note reads like a page with
     *  room to keep writing, instead of ending flush at the last line). */
    tailSpace: Dp = 0.dp,
    /** Watermark paper — the sheet's background wears a faint scatter of
     *  category icons, the page-backdrop language printed on the paper
     *  (v7.33). */
    watermark: Boolean = false,
    /** v7.37 — stable torn-seam/grain seed. Saved views pass a per-entry
     *  seed so the paper's bottom tear (and its texture) NEVER re-rolls
     *  between opens — the same entry always tears identically. Null keeps
     *  the editor's remember-one-random-per-composition behavior. */
    seed: Int? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    // Effective padding — the red margin and the folded corner need extra
    // inset so the text never runs under the margin line or the flap.
    // calculate*Padding return Dp in this Compose version, so the safe inset
    // is built in Dp directly (no px round-trip); only the Canvas's
    // red-margin rule needs px.
    val marginInsetDp = 22.dp
    val foldInsetDp = 28.dp
    val marginInset = with(density) { marginInsetDp.toPx() }
    val safePadding = PaddingValues(
        // PaddingValues has no `left`/`right` parameters — the horizontal
        // insets are `start`/`end`. The app is LTR-only, so start = left and
        // end = right; the Ltr-direction padding we read above maps straight
        // onto them.
        start = if (redMargin) maxOf(
            contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
            marginInsetDp + 8.dp
        ) else contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
        top = contentPadding.calculateTopPadding(),
        end = if (folded) maxOf(
            contentPadding.calculateRightPadding(LayoutDirection.Ltr),
            foldInsetDp + 2.dp
        ) else contentPadding.calculateRightPadding(LayoutDirection.Ltr),
        bottom = contentPadding.calculateBottomPadding() + tailSpace
    )
    // Normal sheets share the hero's broad, rounded tear at the bottom; the
    // bite marks read against the page wash behind the card.
    // v7.33 — the old full-width backing sheet (offset y=8dp, intended as a
    // bottom lip) sat at the TOP of the card, invisible behind the opaque
    // front page everywhere EXCEPT through the folded dog-ear's cut corner,
    // where its own torn edge showed as a stray corner. Removed: the fold
    // now shows the true page background through the cut.
    // v7.37 — saved views pass a stable per-entry [seed], so the paper's
    // torn bottom seam never re-rolls between opens; null keeps the
    // editor's remember-random behavior (stable within a composition).
    val baseSeed = remember(seed) { seed ?: Random.nextInt(1, 1_000_000) }
    val tearSeed = baseSeed
    val topCorner = if (roundedTop) maxOf(corner, 16.dp) else corner
    val shape = remember(tearSeed, topCorner, folded) {
        NormalPaperShape(tearSeed, topCorner, folded)
    }
    Box(
        modifier = modifier.heightIn(min = minHeight).rotate(rotation)
    ) {
        Surface(
            shape = shape,
            color = notePaperSurface(paperColor),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                // Reserve the soft torn seam's small lower extent inside
                // the measured surface so it cannot collide with the next
                // field in a stacked editor.
                .padding(bottom = 18.dp)
        ) {
        Box(
            // Subtle rigid-card sheen — a whisper of top light + bottom
            // depth so the slip reads as stiff paper stock, not a flat fill.
            modifier = Modifier.background(rigidCardSheen())
        ) {
            // Faint ruled lines behind the content — the notebook texture.
            // (notePaperRule() is @Composable, so resolve it here in the
            // composable scope — the Canvas draw lambda is not composable.)
            val ruleColor = if (ruled) notePaperRule(paperColor) else Color.Unspecified
            // Notebook cadence: rules spaced at the body line height,
            // starting one cadence below the top content padding so the
            // first line of text sits ON the first rule (real paper).
            // Guard against an Unspecified lineHeight (custom typography
            // that omits it) — fall back to the classic 24dp cadence.
            val bodyLineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            val ruleSpacingPx = with(density) {
                if (ruleSpacing != null) ruleSpacing.toPx()
                else if (bodyLineHeight == TextUnit.Unspecified) 24.dp.toPx()
                else bodyLineHeight.toPx()
            }
            val ruleStart = with(density) {
                safePadding.calculateTopPadding().toPx()
            } + ruleSpacingPx
            val paperSurface = notePaperSurface(paperColor)
            val paperEdge = notePaperBorder(paperColor)
            val paperInkColor = notePaperInk(paperColor)
            // Every paper card rolls its OWN texture seed, so no two sheets
            // on the page share the same grain pattern (the old fixed seeds
            // drew the identical scatter on every card). With a stable
            // [seed] the grain derives from it, so saved views keep their
            // texture between opens too.
            val paperSeed = remember(baseSeed) { baseSeed * 0x51A7 + 7 }
            Canvas(modifier = Modifier.matchParentSize()) {
                // Real paper texture — fine grain + soft tonal patches (the
                // crumpled-then-flattened tooth) under the rules and ink.
                // Seeded per card so each sheet's pattern is its own.
                drawPaperTexture(size, density, sharedGrainBrush, paperInkColor, paperSeed)
                if (ruled) {
                    var y = ruleStart
                    while (y < size.height) {
                        drawLine(
                            color = ruleColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += ruleSpacingPx
                    }
                }
                if (redMargin) {
                    drawLine(
                        color = PaperMarginRed.copy(alpha = 0.55f),
                        start = Offset(marginInset, 0f),
                        end = Offset(marginInset, size.height),
                        strokeWidth = with(density) { 1.2.dp.toPx() }
                    )
                }
                if (coffeeStains) drawCoffeeStains(size, density, paperSeed)
            }
            // Watermark paper — a faint scatter of category icons behind the
            // text (over the rules, like a printed page watermark).
            if (watermark) {
                PaperWatermarkGlyphs(ink = paperInkColor, seed = paperSeed)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(safePadding),
                content = content
            )
            // Fold flap drawn ABOVE the content — the cut corner (Surface
            // clips to the shape) shows the background through the missing
            // corner, and the flap triangle + crease shadow sit on the paper.
            if (folded) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawFoldFlap(size, density, paperSurface, paperEdge, paperInkColor)
                }
            }
        }
        }
    }
}

/**
 * The rigid-card sheen every paper style wears — a whisper of top light
 * fading through transparent to a faint bottom depth, so the slip reads as
 * stiff paper stock rather than a flat fill. Strong enough to read on the
 * torn page's grain (where it is drawn ON TOP of the texture), subtle
 * enough to never fight the text.
 */
private fun rigidCardSheen(): Brush = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.10f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.06f)
    )
)

/** The classic school-notebook red margin rule — shared by the ruled page
 *  and the torn slip (universal decoration), so both draw the identical
 *  warm red line at the same inset. */
private val PaperMarginRed = Color(0xFFC4524A)

// ─────────────────────────────────────────────────────────────────────────────
// Universal note-paper model (v7.16) — every decoration (rules / coffee /
// folded / red margin) applies to EITHER base (ruled paper or torn slip),
// instead of the old duplicated per-base options. The enum keeps its
// flat names for persistence; these flag views decode it, and
// [notePaperStyleOf] re-encodes any combination back into one value.
// v7.18 — decorations STACK: coffee / folded / red margin are independent,
// so any combination renders (a folded coffee page with a red margin is
// legal). The flag views decode purely from the enum NAME (startsWith /
// contains), so the appended v7.18 combo values need no per-value logic.
// ─────────────────────────────────────────────────────────────────────────────

/** True when the slip wears the ragged torn outline instead of the sharp ruled page. */
val NotePaperStyle.torn: Boolean
    get() = name.startsWith("TORN")

/**
 * True when the sheet draws the notebook ruled lines. The ruled paper base
 * is always ruled (its name carries no RULED segment — but it isn't torn,
 * so it rules by definition); the torn slip only when its name carries
 * RULED.
 */
val NotePaperStyle.ruled: Boolean
    get() = !torn || name.contains("RULED")

/** True when the sheet wears the coffee-stain blotches along its edges. */
val NotePaperStyle.coffee: Boolean
    get() = name.contains("COFFEE")

/** True when the top-right corner is folded into a dog-ear. */
val NotePaperStyle.folded: Boolean
    get() = name.contains("FOLDED")

/** True when the sheet draws the red school-notebook margin line. */
val NotePaperStyle.redMargin: Boolean
    get() = name.contains("RED_MARGIN")

/** True when normal paper rounds both top corners while keeping its torn bottom. */
val NotePaperStyle.roundedTop: Boolean
    get() = name.contains("ROUNDED_TOP")

/** True when the sheet's background wears a faint scatter of category icons. */
val NotePaperStyle.watermark: Boolean
    get() = name.contains("WATERMARK")

/**
 * Re-encodes a (base + decorations) combination into its [NotePaperStyle]
 * value by composing the enum NAME from its parts. v7.18 — decorations
 * STACK: coffee / folded / red margin are independent flags, so this
 * builds the exact combo name (e.g. torn + rules + coffee + folded →
 * TORN_RULED_COFFEE_FOLDED). The old single-select when-chain is gone.
 * Note: [ruled] is only meaningful when [torn] — the ruled paper base is
 * always ruled by definition, so callers pass ruled=true for paper (the
 * toggle does; direct callers should too).
 *
 * v7.33 — [watermark] paper is a BASE-level decoration: it combines with
 * the base + [roundedTop] ONLY (not with coffee / folded / red margin),
 * so the enum stays manageable. When watermark is set, the other
 * decorations are dropped from the name (the toggle clears them when
 * watermark is turned on, so nothing is silently lost in practice).
 */
fun notePaperStyleOf(
    torn: Boolean,
    ruled: Boolean,
    coffee: Boolean = false,
    folded: Boolean = false,
    redMargin: Boolean = false,
    roundedTop: Boolean = false,
    watermark: Boolean = false
): NotePaperStyle {
    val rounded = if (roundedTop) "_ROUNDED_TOP" else ""
    val name = when {
        watermark -> when {
            !torn -> "WATERMARK$rounded"
            ruled -> "TORN_RULED_WATERMARK$rounded"
            else -> "TORN_WATERMARK$rounded"
        }
        else -> {
            val deco = buildString {
                if (coffee) append("_COFFEE")
                if (folded) append("_FOLDED")
                if (redMargin) append("_RED_MARGIN")
                if (roundedTop) append("_ROUNDED_TOP")
            }
            when {
                !torn -> if (deco.isEmpty()) "RULED" else deco.trimStart('_')
                ruled -> "TORN_RULED$deco"
                else -> "TORN$deco"
            }
        }
    }
    return NotePaperStyle.valueOf(name)
}

/**
 * The paper's own texture — a clean, quiet tooth:
 *
 * Layer 1 — Soft tonal variation: large-scale cloudy patches of slightly
 *   darker/lighter tone, like uneven fiber density in real paper.
 * Layer 2 — Fine grain field: scattered micro-specks (the paper tooth)
 *   drawn as individual dots, denser than the old bitmap but still soft.
 *
 * v7.16 — the old long curved fiber strands and big S-curve crease lines
 * are GONE (they read as muddy streaks on every sheet), and everything is
 * seeded from [seed] so every card wears its OWN pattern instead of the
 * same one on all papers. [grainBrush] stays as a subtle shared whisper
 * underneath (uniform grain, not a pattern) — the per-card seeded layers
 * are what make each sheet unique.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPaperTexture(
    canvasSize: Size,
    density: Density,
    grainBrush: Brush,
    ink: Color,
    seed: Int,
    grainAlpha: Float = 0.30f
) {
    val w = canvasSize.width
    val h = canvasSize.height
    // Subtle base wash — the SHARED grain bitmap as a whisper underneath
    // the per-card layers. Fixed low alpha so every sheet gets a uniform
    // paper-grain feel without repeating a visible pattern (the per-card
    // seeded specks below are what differ between sheets).
    drawRect(brush = grainBrush, alpha = 0.12f)
    // Layer 1 — soft tonal variation: several large cloudy patches of
    // slightly altered tone, like uneven pulp density. Radial gradients
    // at random positions, very low alpha so they whisper.
    val rndTone = Random(seed)
    repeat(4) {
        val cx = w * (0.1f + rndTone.nextFloat() * 0.8f)
        val cy = h * (0.1f + rndTone.nextFloat() * 0.8f)
        val radius = (w.coerceAtMost(h)) * (0.25f + rndTone.nextFloat() * 0.45f)
        val toneAlpha = 0.015f + rndTone.nextFloat() * 0.020f
        val toneColor = if (it % 2 == 0) ink.copy(alpha = toneAlpha)
                        else Color.White.copy(alpha = toneAlpha)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(toneColor, Color.Transparent),
                center = Offset(cx, cy),
                radius = radius
            ),
            radius = radius,
            center = Offset(cx, cy)
        )
    }
    // Layer 2 — fine grain field: individual micro-specks scattered across
    // the page. More numerous and slightly larger than the old bitmap so
    // the tooth actually reads instead of looking like dirt. Seeded per
    // card so no two sheets share the same speckle scatter.
    val rndGrain = Random(seed + 0x1F3D5)
    val specCount = (w * h / (with(density) { 80.dp.toPx() * 80.dp.toPx() })).toInt().coerceIn(60, 300)
    repeat(specCount) {
        val sx = rndGrain.nextFloat() * w
        val sy = rndGrain.nextFloat() * h
        val sr = with(density) { (0.4f + rndGrain.nextFloat() * 0.9f).dp.toPx() }
        val sa = (0.03f + rndGrain.nextFloat() * 0.05f) * grainAlpha
        drawCircle(color = ink.copy(alpha = sa), radius = sr, center = Offset(sx, sy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Folded-corner (dog-ear) page + coffee-stain + red-margin decorations.
// The folded shape cuts the top-right corner along a diagonal (Surface
// clips content to it, so text can never run under the flap); the flap +
// crease are drawn as an overlay. Coffee stains and the red margin line
// render inside the rules Canvas (behind the text, like real ink).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rounded-rect outline with the TOP-RIGHT corner replaced by a diagonal
 * cut — the "folded page" dog-ear. The crease runs from `(w - fold, 0)` to
 * `(w, fold)`; the missing corner above it shows the page background, and
 * [drawFoldFlap] paints the folded flap + crease shadow over the paper.
 * Deterministic pure function of (size, corner, fold) — stable across
 * recompositions, so typing never re-folds the page.
 */
private class NormalPaperShape(
    private val seed: Int,
    private val topCorner: Dp,
    private val folded: Boolean
) : Shape {
    private var cachedSize: Size? = null
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        cachedOutline?.let { cached ->
            if (cachedSize == size) return cached
        }
        val outline = Outline.Generic(buildNormalPaperPath(seed, size, density, topCorner, folded))
        cachedSize = size
        cachedOutline = outline
        return outline
    }
}

private fun buildNormalPaperPath(
    seed: Int,
    size: Size,
    density: Density,
    topCorner: Dp,
    folded: Boolean
): Path {
    val w = size.width
    val h = size.height
    val path = Path()
    if (w <= 0f || h <= 0f) {
        path.moveTo(0f, 0f); path.lineTo(w, 0f); path.lineTo(w, h); path.lineTo(0f, h); path.close()
        return path
    }
    val corner = with(density) { topCorner.toPx() }.coerceIn(0f, minOf(w, h) / 2f)
    val fold = with(density) { 26.dp.toPx() }.coerceIn(0f, minOf(w, h) / 2f)
    val tear = SoftTearParams(seed, density)
    val step = with(density) { 4.dp.toPx() }

    // Top-left rounded corner, straight top, optional dog-ear, straight
    // right side, then the hero-style soft tear from right to left.
    if (corner > 0f) {
        // v7.31 — the top-left arc is the path's FIRST segment: start at
        // the bottom of the corner and round UP to the top edge, so the
        // final close() lands exactly on the start point. The old order
        // (arc first, then close() from the left edge) slammed a straight
        // chord back across the arc, chamfering the corner into a flat
        // diagonal instead of a rounded corner.
        path.moveTo(0f, corner)
        path.cubicTo(0f, corner, 0f, 0f, corner, 0f)
    } else {
        path.moveTo(0f, 0f)
    }
    if (folded) {
        // v7.33 — the dog-ear cut follows the same gentle quadratic curve as
        // the flap's crease (see drawFoldFlap), so the fold reads as a curl
        // instead of a mechanical straight diagonal. Both use control point
        // (w - fold * 0.30, fold * 0.30) so the cut and the flap align.
        path.lineTo(w - fold, 0f)
        path.quadraticTo(w - fold * 0.30f, fold * 0.30f, w, fold)
        path.lineTo(w, h + tear.disp(w, w))
    } else {
        path.lineTo(w - corner, 0f)
        if (corner > 0f) {
            path.cubicTo(w, 0f, w, corner, w, corner)
        } else {
            path.lineTo(w, 0f)
        }
        path.lineTo(w, h + tear.disp(w, w))
    }
    var x = w - step
    while (x > 0f) {
        path.lineTo(x, h + tear.disp(x, w))
        x -= step
    }
    path.lineTo(0f, h + tear.disp(0f, w))
    if (corner > 0f) {
        path.lineTo(0f, corner)
    } else {
        path.lineTo(0f, 0f)
    }
    path.close()
    return path
}

/**
 * Realistic coffee-stain blotches along the paper's edges — the dried-cup
 * look with proper ring-concentrated rims (the classic coffee-ring effect
 * where dissolved solids migrate to the edge and leave a dark, crisp ring
 * with a light translucent body), irregular organic pooling shapes, and
 * directional drip runs. v7.16 — seeded from [seed] (the card's own texture
 * seed) so every coffee paper spills a DIFFERENT stain layout instead of
 * the same blotches on every sheet; deterministic per card, so
 * recomposition/typing never re-rolls them.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoffeeStains(
    canvasSize: Size,
    density: Density,
    seed: Int
) {
    val rnd = Random(seed * 31 + 0xCAFE5EED)
    // Richer warm coffee brown — deeper than the old #6B4226 so the ring
    // reads clearly on cream and pastel sheets.
    val coffee = Color(0xFF5C3620)
    val w = canvasSize.width
    val h = canvasSize.height
    // Main stains pinned to edges/corners; the writing area stays clean.
    val spots = listOf(
        0.11f to 0.14f,
        0.85f to 0.19f,
        0.07f to 0.81f,
        0.84f to 0.82f,
        0.48f to 0.09f
    )
    spots.forEachIndexed { i, (fx, fy) ->
        val center = Offset(
            w * (fx + (rnd.nextFloat() - 0.5f) * 0.04f),
            h * (fy + (rnd.nextFloat() - 0.5f) * 0.04f)
        )
        val r = with(density) { (10 + rnd.nextInt(15)).dp.toPx() }
        val ringAlpha = 0.10f + rnd.nextFloat() * 0.08f
        // Create an organic pooling shape — sampled N points around the
        // circle with radial wobble so the puddle reads as real dried
        // coffee, not a compass-drawn circle.
        fun poolPath(points: Int, wobble: Float, squashX: Float = 1f, squashY: Float = 1f): Path = Path().apply {
            var first = true
            repeat(points) { k ->
                val ang = (k.toFloat() / points) * (Math.PI * 2).toFloat()
                val rr = r * (1f + (rnd.nextFloat() - 0.5f) * wobble)
                val px = center.x + cos(ang) * rr * squashX
                val py = center.y + sin(ang) * rr * squashY
                if (first) { moveTo(px, py); first = false } else lineTo(px, py)
            }
            close()
        }
        // Slightly squashed pools — coffee never dries in a perfect circle.
        val sqX = 0.85f + rnd.nextFloat() * 0.30f
        val sqY = 0.85f + rnd.nextFloat() * 0.30f
        val pool = poolPath(20, 0.32f, sqX, sqY)
        // Bleed halo (v7.33) — the damp ring the liquid left as it soaked
        // into the paper grain around the dried pool, so the stain reads as
        // absorbed into the sheet rather than painted on top of it.
        drawPath(
            pool,
            brush = Brush.radialGradient(
                colors = listOf(
                    coffee.copy(alpha = ringAlpha * 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = r * 1.45f
            )
        )
        // Light translucent body inside the ring — the dried liquid's
        // faint stain, fading toward the rim where the ring concentrates.
        drawPath(
            pool,
            brush = Brush.radialGradient(
                colors = listOf(
                    coffee.copy(alpha = ringAlpha * 0.20f),
                    coffee.copy(alpha = ringAlpha * 0.07f),
                    Color.Transparent
                ),
                center = center,
                radius = r * 1.15f
            )
        )
        // The concentrated ring — darker, slightly wider on the outside.
        // Real coffee rings: dissolved solids migrate to the perimeter,
        // leaving a crisp dark rim. Two passes: a soft wider stroke and a
        // crisp thinner stroke on top for the concentrated edge.
        drawPath(
            pool,
            color = coffee.copy(alpha = ringAlpha * 0.60f),
            style = Stroke(width = with(density) { 2.4.dp.toPx() })
        )
        drawPath(
            pool,
            color = coffee.copy(alpha = ringAlpha * 0.42f),
            style = Stroke(width = with(density) { 1.1.dp.toPx() })
        )
        // Inner ghost ring — some stains have a fainter inner ring where
        // the cup was lifted and set back down (rocking cup effect).
        if (i % 2 == 1) {
            val inner = poolPath(14, 0.18f, sqX * 0.85f, sqY * 0.85f)
            drawPath(
                inner,
                color = coffee.copy(alpha = ringAlpha * 0.50f),
                style = Stroke(width = with(density) { 1.3.dp.toPx() })
            )
        }
        // Directional drip runs (v7.33) — tapered streaks that narrow
        // toward the bottom, like coffee that ran down the page and dried
        // thinner as it stretched.
        repeat(if (i % 3 == 0) 2 else 1) {
            val angle = (-0.3f + rnd.nextFloat() * 0.6f) // roughly downward ±0.3 rad
            val dripStart = Offset(
                center.x + cos(angle) * r * 0.9f,
                center.y + sin(angle) * r * 0.9f
            )
            val dripLen = with(density) { (6 + rnd.nextInt(10)).dp.toPx() }
            val dripEnd = Offset(
                dripStart.x + cos(angle) * dripLen * 0.5f,
                dripStart.y + sin(angle) * dripLen
            )
            val dripMid = Offset(
                (dripStart.x + dripEnd.x) / 2f + (rnd.nextFloat() - 0.5f) * dripLen * 0.25f,
                (dripStart.y + dripEnd.y) / 2f
            )
            val tipW = with(density) { (0.7f + rnd.nextFloat() * 0.8f).dp.toPx() }
            val dripPath = Path().apply {
                moveTo(dripStart.x - tipW, dripStart.y)
                quadraticTo(dripMid.x - tipW * 0.4f, dripMid.y, dripEnd.x, dripEnd.y)
                quadraticTo(dripMid.x + tipW * 0.4f, dripMid.y, dripStart.x + tipW, dripStart.y)
                close()
            }
            drawPath(
                dripPath,
                color = coffee.copy(alpha = ringAlpha * 0.45f)
            )
        }
        // Satellite splatter dots — tiny drops around the main stain.
        repeat(if (i % 2 == 0) 5 else 3) {
            val a = rnd.nextFloat() * (Math.PI * 2).toFloat()
            val d = r * (1.15f + rnd.nextFloat() * 0.65f)
            val drop = Offset(center.x + cos(a) * d, center.y + sin(a) * d)
            if (drop.x in 0f..w && drop.y in 0f..h) {
                drawCircle(
                    color = coffee.copy(alpha = ringAlpha * 0.55f),
                    radius = with(density) { (0.8 + rnd.nextFloat() * 1.5).dp.toPx() },
                    center = drop
                )
            }
        }
    }
    // Faint washed patches — the cup's moist footprint on the page,
    // softer and fewer (v7.33) so they whisper instead of muddying the sheet.
    repeat(2) {
        val px = w * (0.18f + rnd.nextFloat() * 0.64f)
        val py = h * (0.30f + rnd.nextFloat() * 0.45f)
        val pr = with(density) { (20 + rnd.nextInt(28)).dp.toPx() }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coffee.copy(alpha = 0.035f),
                    coffee.copy(alpha = 0.012f),
                    Color.Transparent
                ),
                center = Offset(px, py),
                radius = pr
            ),
            radius = pr,
            center = Offset(px, py)
        )
    }
}

/**
 * The folded flap + crease shadow for the dog-ear (v7.33 realism pass).
 * The flap is the reflected corner triangle with a gentle QUADRATIC crease
 * — the SAME curve the shape's cut uses (see [NormalPaperShape]) — so the
 * fold reads as a curled corner instead of a mechanical straight diagonal.
 * The shading is a radial read: darkest along the crease (the underside of
 * the bend), a soft mid-tone across the body, catching light toward the
 * tip. Every color derives from the PAPER's own palette ([paperInk] /
 * [paperEdge]) so the fold reads naturally on every sheet (cream + the
 * pastel swatches) and in dark mode. A curved specular catches the fold
 * ridge; the drop shadow is a feathered wedge that follows the curved
 * crease and fades to transparent (reads as lift on the torn grain too,
 * instead of a hard black triangle pasted over the rips).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFoldFlap(
    canvasSize: Size,
    density: Density,
    paperSurface: Color,
    paperEdge: Color,
    paperInk: Color
) {
    // Same fold size + clamp as the shape's cut (buildNormalPaperPath) so
    // the flap can never exceed the cut on a tiny card.
    val f = with(density) { 26.dp.toPx() }.coerceIn(0f, minOf(canvasSize.width, canvasSize.height) / 2f)
    val s = with(density) { 4.dp.toPx() }
    val w = canvasSize.width
    // The crease — a gentle quadratic bowing up-right, matched 1:1 by the
    // shape's cut so the flap edge sits exactly on the fold line.
    fun crease(): Path = Path().apply {
        moveTo(w - f, 0f)
        quadraticTo(w - f * 0.30f, f * 0.30f, w, f)
    }
    // Feathered drop shadow — an ink-tinted wedge (NOT raw black: on the
    // torn grain and pastel sheets a hard black triangle reads as pasted
    // on) that casts down-left of the crease, following its curve, and
    // fades out softly so the flap reads as genuinely lifted off the paper.
    drawPath(
        Path().apply {
            moveTo(w - f - s, s)
            quadraticTo(w - f * 0.30f - s * 0.7f, f * 0.30f + s * 0.7f, w - s, f + s)
            lineTo(w - f - s, f + s)
            close()
        },
        brush = Brush.linearGradient(
            colors = listOf(
                paperInk.copy(alpha = 0.17f),
                paperInk.copy(alpha = 0.08f),
                Color.Transparent
            ),
            start = Offset(w - f, s),
            end = Offset(w - f - s * 1.8f, f + s)
        )
    )
    // The flap itself — the paper's BACK side: darkest along the crease
    // (a shade of the sheet's own ink, not black), lightening toward the
    // tip with a faint light catch at the corner. The radial falloff from
    // the crease's midpoint sells the bend's underside darkening toward
    // the fold and brightening where the corner curls up.
    val flap = Path().apply {
        moveTo(w - f, 0f)
        quadraticTo(w - f * 0.30f, f * 0.30f, w, f)
        lineTo(w - f, f)
        close()
    }
    drawPath(
        flap,
        brush = Brush.radialGradient(
            colors = listOf(
                lerp(paperSurface, paperInk, 0.26f),
                lerp(paperSurface, paperInk, 0.13f),
                lerp(paperSurface, Color.White, 0.10f)
            ),
            center = Offset(w - f * 0.50f, f * 0.50f),
            radius = f * 1.05f
        )
    )
    // Specular highlight — a thin light stroke just below the crease on
    // the flap body, following the curve (the folded paper bulges and
    // catches the light along the ridge).
    drawPath(
        Path().apply {
            moveTo(w - f + s * 1.3f, s * 0.6f)
            quadraticTo(w - f * 0.30f + s * 1.2f, f * 0.30f - s * 0.4f, w - s * 0.6f, f - s * 0.7f)
        },
        color = Color.White.copy(alpha = 0.15f),
        style = Stroke(width = with(density) { 1.4.dp.toPx() })
    )
    // Soft crease halo — a wide, faint ink blur along the fold line.
    drawPath(
        crease(),
        color = paperInk.copy(alpha = 0.10f),
        style = Stroke(width = with(density) { 3.5.dp.toPx() })
    )
    // Crisp crease hairline — the actual fold edge, darkened toward the
    // sheet's ink so it stays crisp on cream AND pastel AND dark paper.
    drawPath(
        crease(),
        color = lerp(paperEdge, paperInk, 0.35f),
        style = Stroke(width = with(density) { 1.1.dp.toPx() })
    )
    // Corner-tip lift — the outermost point catches a brighter spot of
    // light, selling the "peeled-up corner" read.
    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        radius = with(density) { 2.8.dp.toPx() },
        center = Offset(w - f, 0f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Torn-note paper — a properly ripped slip. The edge is displaced with the
// SAME soft torn language as the hero's seam (broad rounded waves + finer
// bumpy ripple, v7.33 — the old multi-octave fractal jitter read as
// artificial), and the slip wears a pre-rendered grunge texture instead of
// hundreds of per-frame specks. Both are built ONCE per seed and cached, so
// typing / recomposition never rebuilds them — that was the lag source in
// the earlier implementation.
// ─────────────────────────────────────────────────────────────────────────────

/** Deterministic integer hash → [0, 1) — the lattice for the value noise. */
private fun hash2(seed: Int, x: Int, y: Int): Float {
    var n = x * 374761393 + y * 668265263 + seed * 1274126177
    n = (n xor (n ushr 13)) * 1274126177
    n = n xor (n ushr 16)
    return (n and 0x7fffffff).toFloat() / 0x7fffffff.toFloat()
}

/** Smooth value noise at [x], [y] — interpolated lattice hashes. */
private fun valueNoise(seed: Int, x: Float, y: Float): Float {
    val xi = floor(x).toInt()
    val yi = floor(y).toInt()
    val xf = x - floor(x)
    val yf = y - floor(y)
    val a = hash2(seed, xi, yi)
    val b = hash2(seed, xi + 1, yi)
    val c = hash2(seed, xi, yi + 1)
    val d = hash2(seed, xi + 1, yi + 1)
    val u = xf * xf * (3f - 2f * xf)
    val v = yf * yf * (3f - 2f * yf)
    return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v
}

/**
 * A soft \"torn note\" outline — walks the card perimeter and displaces
 * EVERY side with the hero's soft torn language (broad rounded waves +
 * finer bumpy ripple, see [SoftTearParams]) instead of the old sharp
 * fractal jitter, which read as artificial. Each edge tears with its own
 * seeded personality, and the displacement fades to zero near the corners
 * so the four sides meet cleanly (real tears run out at the corners).
 *
 * The computed outline is CACHED per size: `createOutline` is called on
 * every draw, but the Path only depends on (seed, size, density), so a
 * recomposition with an unchanged size reuses the cached outline instead of
 * rebuilding a ~150-point path every frame.
 */
private class TornPaperShape(private val seed: Int = 7) : Shape {
    private var cachedSize: Size? = null
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        cachedOutline?.let { cached ->
            if (cachedSize == size) return cached
        }
        val outline = Outline.Generic(buildTornPath(seed, size, density))
        cachedSize = size
        cachedOutline = outline
        return outline
    }
}

private fun buildTornPath(seed: Int, size: Size, density: Density): Path {
    val w = size.width
    val h = size.height
    val path = Path()
    if (w <= 0f || h <= 0f) {
        path.moveTo(0f, 0f)
        path.lineTo(w, 0f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        return path
    }
    // v7.33 — every side tears with the hero's SOFT language (broad rounded
    // waves + finer bumpy ripple) instead of the old sharp fractal jitter.
    // Each edge gets its own seeded tear personality, and the displacement
    // is scaled to ~55% of the hero's amplitudes: the hero's seam lives on
    // a banner with deep content insets, but the torn card floors its text
    // at 16dp horizontal / 14dp vertical, so the gentler scale keeps the
    // worst-case bite (~9dp) safely inside the content floor. The edge also
    // fades to zero over the last ~5% at each end so the four sides meet in
    // crisp corners instead of notching into each other.
    val step = with(density) { 4.dp.toPx() }
    fun edgeDisp(p: SoftTearParams, pos: Float, span: Float): Float {
        val t = pos / span
        val fade = when {
            t < 0.05f -> t / 0.05f
            t > 0.95f -> (1f - t) / 0.05f
            else -> 1f
        }
        return p.disp(pos, span) * fade * 0.55f
    }
    val top = SoftTearParams(seed, density)
    val right = SoftTearParams(seed + 0x1A1, density)
    val bottom = SoftTearParams(seed + 0x2B2, density)
    val left = SoftTearParams(seed + 0x3C3, density)
    var first = true
    fun add(p: Offset) {
        if (first) {
            path.moveTo(p.x, p.y)
            first = false
        } else {
            path.lineTo(p.x, p.y)
        }
    }

    // ── Top edge (left → right) ───────────────────────────────────────
    var x = 0f
    while (x <= w) {
        add(Offset(x, edgeDisp(top, x, w)))
        x += step
    }
    // ── Right edge (top → bottom) ─────────────────────────────────────
    var y = 0f
    while (y <= h) {
        add(Offset(w + edgeDisp(right, y, h), y))
        y += step
    }
    // ── Bottom edge (right → left) ────────────────────────────────────
    x = w
    while (x >= 0f) {
        add(Offset(x, h + edgeDisp(bottom, x, w)))
        x -= step
    }
    // ── Left edge (bottom → top) ──────────────────────────────────────
    y = h
    while (y >= 0f) {
        add(Offset(edgeDisp(left, y, h), y))
        y -= step
    }
    path.close()
    return path
}

/**
 * A SOFT torn-paper edge — the hero's torn bottom seam. Unlike the sharp
 * multi-octave [TornPaperShape] (jagged rip), this edge is torn with broad,
 * ROUNDED waves: a continuous seeded wave field plus smooth value-noise
 * displacement sampled at a fine step, so the tear keeps moving across the
 * full width with 2–3 broad undulations, each carrying several smaller,
 * rounded bumps instead of falling into a straight plateau. The whole tear
 * personality — wave count, depths, tilt and phase —
 * is drawn from the tear seed, so every entry tears differently but the
 * same entry always tears identically.
 *
 * Only ONE side is torn — the opposite long side stays straight — because
 * the hero clip tears just its lower edge; the white under-sheet behind it
 * reuses the SAME seeded tear curve (see [SoftTornSheetShape]) so the two
 * edges align pixel-perfect.
 */
private class SoftTornEdgeShape(
    private val seed: Int,
    private val bold: Boolean = false,
    private val detail: Boolean = false
) : Shape {
    private var cachedSize: Size? = null
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        cachedOutline?.let { cached ->
            if (cachedSize == size) return cached
        }
        val outline = Outline.Generic(buildSoftTornPath(seed, size, density, bold, detail))
        cachedSize = size
        cachedOutline = outline
        return outline
    }
}

/**
 * Public bottom-torn clip — the hero's lower edge (meeting point of the
 * blur). [bold] switches to the rougher Home personality (deeper, toothier
 * seam) while staying deterministic; the matching under-sheet must receive
 * the SAME flag so the two edges stay pixel-aligned.
 */
class SoftTornBottomShape(
    seed: Int,
    bold: Boolean = false,
    detail: Boolean = false
) : Shape {
    private val inner = SoftTornEdgeShape(seed, bold, detail)
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        inner.createOutline(size, layoutDirection, density)
}

/**
 * Public under-sheet clip — the SOLID white sheet behind the hero's torn
 * bottom edge. Its top edge is the SAME seeded torn curve as the hero (the
 * two shapes derive their displacement from the same seed, so they align
 * pixel-perfect and the sheet's torn top hides behind the opaque hero — the
 * tear lives ONLY on the hero card). Its bottom edge follows the same
 * broad-and-bumpy rhythm, pushed DOWN by a constant [lip], so the visible
 * white is a deep continuous sheet below the tear everywhere, with the page
 * wash starting right after it — no interlocking halves or gaps through the
 * teeth. [baseline]
 * lifts the torn top above the box's own top edge (behind the hero) so the
 * hero's up-bites still read white.
 */
class SoftTornSheetShape(
    private val seed: Int,
    private val lip: Dp,
    private val baseline: Dp = 0.dp,
    // v7.37 — must match the hero's [SoftTornBottomShape.bold] flag so the
    // under-sheet's top edge reproduces the SAME bold tear curve.
    private val bold: Boolean = false,
    // Detail heroes use a slightly more expressive seeded pattern so an
    // unlucky entry hash cannot make the seam read as a flat line. Other
    // callers retain the established tear personality.
    private val detail: Boolean = false
) : Shape {
    private var cachedSize: Size? = null
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        cachedOutline?.let { cached ->
            if (cachedSize == size) return cached
        }
        val outline = Outline.Generic(buildSoftSheetPath(
            seed, size, density,
            lipPx = with(density) { lip.toPx() },
            baselinePx = with(density) { baseline.toPx() },
            bold = bold,
            detail = detail
        ))
        cachedSize = size
        cachedOutline = outline
        return outline
    }
}

/**
 * The Home Streak · Cabinet · Topics stat card's torn paper outline
 * (experimental "Torn paper edges" look, off by default).
 *
 * The TOP edge carries an EXTENDED tear — a hand-torn slip ripped out of
 * the banner: a NEW re-seeded personality (v27v) mixing the soft broad
 * waves with a gentler medium raggedness (instead of the old single bold
 * inverted seam), mostly extending above the nominal top with a few
 * notches. The other three edges tear with a SOFTER ragged edge than
 * before (v27v — the old 3.5dp high-frequency jitter read as spiky
 * artificial bites; the amplitude is trimmed and the high octave faded), so
 * the slip reads as genuinely ripped on every side while only the top gets
 * the extended look.
 *
 * Deterministic per seed and cached per size — recomposition never
 * re-tears, and the same seed always reproduces the identical outline.
 */
class TornStatPaperShape(private val seed: Int) : Shape {
    private var cachedSize: Size? = null
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        cachedOutline?.let { cached ->
            if (cachedSize == size) return cached
        }
        val outline = Outline.Generic(buildTornStatPath(seed, size, density))
        cachedSize = size
        cachedOutline = outline
        return outline
    }
}

private fun buildTornStatPath(seed: Int, size: Size, density: Density): Path {
    val w = size.width
    val h = size.height
    val path = Path()
    if (w <= 0f || h <= 0f) {
        path.moveTo(0f, 0f); path.lineTo(w, 0f); path.lineTo(w, h); path.lineTo(0f, h); path.close()
        return path
    }
    val step = with(density) { 4.dp.toPx() }
    // Top edge — the EXTENDED tear. v27v — a NEW re-seeded personality: the
    // soft broad-wave seam (salted so it no longer mirrors the hero's bold
    // seam) PLUS a gentle medium-frequency raggedness layered on, so the top
    // reads as a fresh hand-rip out of the banner rather than a duplicate of
    // the hero seam.
    val top = SoftTearParams(seed xor 0x5A7E4D, density, bold = false)
    // The three straight sides tear with a SOFTER ragged edge than the old
    // sharp jitter (v27v): amplitude trimmed from 3.5dp to ~2.2dp and the
    // high-frequency octave faded so the bites read as real paper tears, not
    // spikes — still faded to crisp corners so the four sides meet cleanly.
    fun sharpDisp(edgeSeed: Int, pos: Float, span: Float): Float {
        val t = pos / span
        val fade = when {
            t < 0.04f -> t / 0.04f
            t > 0.96f -> (1f - t) / 0.04f
            else -> 1f
        }
        val amp = with(density) { 2.2.dp.toPx() }
        val n1 = (valueNoise(edgeSeed, pos * 0.19f, 5.3f) - 0.5f) * 2f
        val n2 = (valueNoise(edgeSeed + 7, pos * 0.47f, 9.7f) - 0.5f) * 2f * 0.45f
        val n3 = (valueNoise(edgeSeed + 13, pos * 1.05f, 3.9f) - 0.5f) * 2f * 0.16f
        return (n1 + n2 + n3) * amp * fade
    }
    val right = seed + 0x1A1
    val bottom = seed + 0x2B2
    val left = seed + 0x3C3

    var first = true
    fun add(p: Offset) {
        if (first) {
            path.moveTo(p.x, p.y)
            first = false
        } else {
            path.lineTo(p.x, p.y)
        }
    }

    // ── Top edge (left → right) — EXTENDED soft tear ─────────────────────
    // v27v — the new top: broad soft waves (re-seeded) with a gentle
    // medium-frequency raggedness riding over them, extended ABOVE the
    // nominal top. Fades to zero over the last ~5% at each end so the
    // extended edge meets the side tears in a crisp corner.
    val topSeed = seed xor 0x1F3A5C7D
    var x = 0f
    while (x <= w) {
        val t = x / w
        val fade = when {
            t < 0.05f -> t / 0.05f
            t > 0.95f -> (1f - t) / 0.05f
            else -> 1f
        }
        // Soft wave rhythm (down-biased in SoftTearParams, inverted here so
        // the card extends UP out of the banner) plus a gentle ragged layer.
        val wave = -top.disp(x, w) * 1.2f
        val ragged = (valueNoise(topSeed, x * 0.11f, 13.7f) - 0.5f) * 2f *
            with(density) { 1.6.dp.toPx() }
        add(Offset(x, (wave + ragged) * fade))
        x += step
    }
    // ── Right edge (top → bottom) — sharper ragged tear ─────────────────
    var y = 0f
    while (y <= h) {
        add(Offset(w + sharpDisp(right, y, h), y))
        y += step
    }
    // ── Bottom edge (right → left) ──────────────────────────────────────
    x = w
    while (x >= 0f) {
        add(Offset(x, h + sharpDisp(bottom, x, w)))
        x -= step
    }
    // ── Left edge (bottom → top) ────────────────────────────────────────
    y = h
    while (y >= 0f) {
        add(Offset(sharpDisp(left, y, h), y))
        y -= step
    }
    path.close()
    return path
}

/**
 * The seeded tear personality for a soft torn edge — wave count, depths,
 * tilt and phase are all drawn from ONE random stream derived from the tear
 * seed, so every entry tears differently but the same seed always
 * reproduces the identical edge (constant across reopens). The hero clip
 * and its white under-sheet each build a [SoftTearParams] from the same
 * seed, so their torn edges align pixel-perfect.
 *
 * The displacement is DOWN-BIASED (positive dips scaled to 55%): the tear
 * bites UP into the card in broad rounded waves with a finer bumpy ripple
 * riding over them (white shows behind the card through the bites) while
 * dipping only a modest amount below the nominal edge. The under-sheet uses
 * the same broad-plus-small rhythm on its exposed lower edge and extends
 * far enough below the hero to keep the page wash from peeking through.
 */
private class SoftTearParams(
    private val seed: Int,
    density: Density,
    // v7.37 — BOLDER tear personality: Home's hero tears a touch deeper and
    // toothier than the detail hero's (a visibly different pattern), while
    // its white under-sheet passes the SAME flag so the two edges stay
    // pixel-aligned.
    private val bold: Boolean = false,
    private val detail: Boolean = false
) {
    // Detail uses a salted pattern stream: it remains stable per entry, but
    // avoids the small subset of entry hashes whose phase made the edge look
    // nearly straight. This branch is intentionally opt-in at the shape
    // boundary so every other paper surface stays unchanged.
    private val patternSeed = if (detail) seed xor 0x4D3C2B1A else seed
    private val rnd = Random(patternSeed * 31 + 0x0BADC0DE)
    // 2–3 broad rounded undulations across the full width (a few more in
    // the bold pattern). A separate smaller ripple rhythm rides over them,
    // so the edge is visibly bumpy without becoming a string of oversized
    // waves.
    val waves = (if (detail) 2.8f else 2.2f) + rnd.nextFloat() * (if (detail) 1.0f else 0.8f)
    val waveScale = (if (bold) 1.2f else 1f) * (if (detail) 1.08f else 1f)
    // v7.29 — amplitudes nudged up (~25%) so the tear reads a touch MORE
    // uneven and hand-torn without overwhelming the broad wave rhythm (the
    // worst-case bite still sits ~10dp, far inside the hero's content).
    // v7.37 — bold scales them ~35-50% further for Home's rougher seam.
    val tooth = with(density) { (6.4f + rnd.nextFloat() * 2.2f).dp.toPx() } *
        (if (bold) 1.35f else 1f) * (if (detail) 1.08f else 1f)
    val deep = with(density) { (2.4f + rnd.nextFloat() * 1.5f).dp.toPx() } *
        (if (bold) 1.5f else 1f) * (if (detail) 1.10f else 1f)
    val micro = with(density) { (1.0f + rnd.nextFloat() * 0.8f).dp.toPx() }
    val ripple = with(density) { (1.3f + rnd.nextFloat() * 0.9f).dp.toPx() } *
        (if (detail) 1.12f else 1f)
    val rippleWaves = (if (detail) 8f else 7f) + rnd.nextFloat() * (if (detail) 4.5f else 4f)
    // Seeded tilt — the whole edge drifts from left to right so the torn
    // SEAM visibly cants while the card rectangle stays level (the tilt
    // lives inside the tear path, never a rotation of the card). It's a
    // gentle hand-torn slant — large enough to read (v7.x — raised from
    // the old ±2dp whisper which was invisible once the card itself was
    // no longer rotated), small enough not to overwhelm the broad wave
    // rhythm.
    val tilt = (rnd.nextFloat() - 0.5f) * 2f * with(density) { (6f + rnd.nextFloat() * 4f).dp.toPx() }
    val phase = rnd.nextFloat() * 100f

    /** The torn-edge displacement at horizontal position [x] (px) across a
     *  [w]-wide edge. Broad waves + smaller bumpy ripples + fine fiber
     *  tooth, down-biased (see the class kdoc). */
    fun broadDisp(x: Float, w: Float): Float {
        val normalizedX = x / w
        val slant = tilt * (normalizedX - 0.5f)
        // The low-frequency sine establishes 2–3 large waves. Seeded value
        // noise bends their shoulders so they remain hand-torn rather than
        // reading as a perfect mechanical sine.
        val waveAngle = normalizedX * waves * (Math.PI * 2.0).toFloat() + phase
        val rhythmic = sin(waveAngle) * tooth * 0.58f
        val main = (valueNoise(patternSeed, normalizedX * waves, phase) - 0.5f) * 2f * tooth * 0.58f
        val deepWave = (valueNoise(patternSeed + 101, normalizedX * (waves * 0.42f), phase + 17f) - 0.5f) * 2f * deep
        // Detail seams always carry one deterministic secondary oscillation.
        // It is small enough to stay natural, but guarantees visible movement
        // even when the seeded noise and primary wave happen to cancel out.
        val detailVariation = if (detail) {
            sin(normalizedX * (Math.PI * 5.6).toFloat() + phase * 0.37f) * tooth * 0.22f
        } else {
            0f
        }
        val raw = slant + rhythmic * waveScale + main * waveScale + deepWave + detailVariation
        return if (raw > 0f) raw * 0.55f else raw
    }

    fun disp(x: Float, w: Float): Float {
        val normalizedX = x / w
        val rippleAngle = normalizedX * rippleWaves * (Math.PI * 2.0).toFloat() + phase * 1.7f
        val smallRipple = sin(rippleAngle) * ripple * 0.45f +
            (valueNoise(patternSeed + 71, normalizedX * rippleWaves * 1.35f, phase + 29f) - 0.5f) * 2f * ripple * 0.55f
        val fiber = (valueNoise(patternSeed + 47, x * 0.14f, 3.5f) - 0.5f) * 2f * micro
        val raw = broadDisp(x, w) + smallRipple + fiber
        return if (raw > 0f) raw * 0.55f else raw
    }
}

/**
 * Builds the soft torn outline: three straight edges + one continuously
 * moving, softly torn long edge. The torn edge is sampled every ~4dp along
 * its length with a seeded wave field, smooth noise, and fine fiber
 * micro-texture. Pure function of (seed, size, density), cached per
 * size by [SoftTornEdgeShape]. The tear is WAVY and unique per entry — see
 * [SoftTearParams].
 */
private fun buildSoftTornPath(
    seed: Int,
    size: Size,
    density: Density,
    bold: Boolean = false,
    detail: Boolean = false
): Path {
    val w = size.width
    val h = size.height
    val path = Path()
    if (w <= 0f || h <= 0f) {
        path.moveTo(0f, 0f); path.lineTo(w, 0f); path.lineTo(w, h); path.lineTo(0f, h); path.close()
        return path
    }
    val p = SoftTearParams(seed, density, bold, detail)
    val step = with(density) { 4.dp.toPx() }
    // Clockwise: straight top, straight right, torn bottom (right→left),
    // straight left, close.
    path.moveTo(0f, 0f)
    path.lineTo(w, 0f)
    path.lineTo(w, h + p.disp(w, w))
    var x = w - step
    while (x > 0f) {
        path.lineTo(x, h + p.disp(x, w))
        x -= step
    }
    path.lineTo(0f, h + p.disp(0f, w))
    path.close()
    return path
}

/**
 * Builds the white under-sheet outline behind the hero's torn bottom edge.
 * The sheet is SOLID at the top and only its exposed lower edge is uneven;
 * the tear therefore belongs to the hero side, not to an intersecting second
 * torn edge. The bottom edge follows a restrained shared wave rhythm,
 * pushed down by [lipPx], so the visible white is continuous below the
 * hero with no page wash showing through. [baselinePx] is retained as the
 * seeded lower-edge offset used to keep the lip aligned beneath the hero.
 */
private fun buildSoftSheetPath(
    seed: Int,
    size: Size,
    density: Density,
    lipPx: Float,
    baselinePx: Float,
    bold: Boolean = false,
    detail: Boolean = false
): Path {
    val w = size.width
    val h = size.height
    val path = Path()
    if (w <= 0f || h <= 0f || lipPx <= 0f) {
        path.moveTo(0f, 0f); path.lineTo(w, 0f); path.lineTo(w, h); path.lineTo(0f, h); path.close()
        return path
    }
    val p = SoftTearParams(seed, density, bold, detail)
    val step = with(density) { 4.dp.toPx() }
    // Keep the hero and sheet on the SAME broad wave rhythm, but do not
    // duplicate the hero's fine tooth. The exposed white gets only a small,
    // separately seeded paper wobble so it feels like a second sheet edge
    // following the tear rather than a rigid parallel ruler.
    fun bottomBump(x: Float): Float {
        val normalizedX = x / w
        // Keep the exposed backing genuinely thin: it follows only a
        // restrained fraction of the hero's broad wave, plus a tiny paper
        // wobble. Reusing the full broad displacement here made the lower
        // edge dive into long straight-looking sections and could clip at
        // the sheet box boundary, exposing page-wash gaps.
        val sharedShape = p.broadDisp(x, w) * if (detail) 0.28f else 0.18f
        val rippleAngle = normalizedX * (p.rippleWaves * 0.86f) * (Math.PI * 2.0).toFloat() + p.phase * 1.11f
        val extraRipple = sin(rippleAngle) * with(density) { if (detail) 0.60.dp.toPx() else 0.45.dp.toPx() } +
            (valueNoise(
                if (detail) seed xor 0x4D3C2B1A else seed,
                normalizedX * p.rippleWaves * 0.9f,
                p.phase + 53f
            ) - 0.5f) * 2f * with(density) { if (detail) 0.40.dp.toPx() else 0.30.dp.toPx() }
        return sharedShape + extraRipple
    }
    // Clockwise: a SOLID top (hidden behind the hero), down the right side,
    // then the restrained uneven lower lip. Only the lower edge is torn so
    // anti-aliased hero peaks can never reveal a second sheet intersection.
    // Keep the backing path inside its measured box even for a seeded peak;
    // the overlap behind the hero handles the top edge, while this clamp
    // guarantees the lower white sheet can never be clipped into a gap.
    val topAt: (Float) -> Float = { x -> baselinePx + p.disp(x, w) }
    val bottomAt: (Float) -> Float = { x ->
        (topAt(x) + lipPx + bottomBump(x)).coerceAtMost(h - 0.5f)
    }
    path.moveTo(0f, 0f)
    path.lineTo(w, 0f)
    path.lineTo(w, bottomAt(w))
    var x = w - step
    while (x > 0f) {
        path.lineTo(x, bottomAt(x))
        x -= step
    }
    path.lineTo(0f, bottomAt(0f))
    path.close()
    return path
}

/**
 * Pre-renders the torn note's grunge texture — a tiled bitmap of faint ink
 * speckles + fiber dashes, generated once (see [sharedGrainBitmap]). Drawn
 * as a single shader rect every frame instead of ~300 drawCircle calls (the
 * old lag). The texture is GENERIC — the per-card seed already makes each
 * tear unique — so every torn card reuses ONE shared bitmap instead of
 * building a ~100KB texture per card.
 */
private fun buildGrainBitmap(seed: Int): ImageBitmap {
    val sizePx = 192
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val rnd = Random(seed)
    val inkArgb = 0xFF3B3124.toInt()
    // Faint speckle clusters — the paper grain. Kept sparse + low-alpha:
    // dense, high-alpha specks read as "dirty" rather than paper tooth.
    repeat(150) {
        val x = rnd.nextFloat() * sizePx
        val y = rnd.nextFloat() * sizePx
        val r = 0.5f + rnd.nextFloat() * 1.2f
        val alpha = (3 + rnd.nextInt(14)).coerceAtMost(22)
        paint.color = (alpha shl 24) or (inkArgb and 0xFFFFFF)
        canvas.drawCircle(x, y, r, paint)
    }
    // Sparse fiber dashes — the torn-sheet tooth, also kept faint.
    paint.strokeWidth = 1f
    repeat(18) {
        val x = rnd.nextFloat() * sizePx
        val y = rnd.nextFloat() * sizePx
        val len = 4f + rnd.nextFloat() * 8f
        val ang = rnd.nextFloat() * (Math.PI * 2).toFloat()
        val alpha = (3 + rnd.nextInt(8)).coerceAtMost(16)
        paint.color = (alpha shl 24) or (inkArgb and 0xFFFFFF)
        canvas.drawLine(x, y, x + cos(ang) * len, y + sin(ang) * len, paint)
    }
    return bmp.asImageBitmap()
}

/** One shared grunge texture for ALL torn cards — built lazily, once. */
private val sharedGrainBitmap: ImageBitmap by lazy { buildGrainBitmap(0x1F3D5) }

/**
 * One shared grain BRUSH wrapping [sharedGrainBitmap] — every paper card
 * (torn + ruled) tiles the same texture: the per-card seed keeps each torn
 * EDGE unique, and ruled sheets draw the grain at low alpha so they read as
 * smooth paper with tooth instead of grunge.
 */
private val sharedGrainBrush: Brush by lazy {
    ShaderBrush(ImageShader(sharedGrainBitmap, TileMode.Repeated, TileMode.Repeated))
}

/**
 * The torn-note paper card — a properly ripped paper slip instead of the
 * rounded ruled notebook page. SOFT torn edges on every side (v7.33 — the
 * hero's rounded-wave tear language, deterministic per [seed], so
 * recomposition/typing never re-rolls the tear), a pre-rendered grunge
 * texture (no per-frame speckle loop — the lag fix), and no drop shadow
 * (rasterizing a shadow for a ~150-point outline every frame was the other
 * lag source). [ruled] adds the notebook ruled lines inside the torn
 * outline (the \"rules on torn\" toggle); [coffeeStains] spills the same
 * coffee blotches as the ruled page, [folded] folds the top-right corner
 * into a dog-ear (flap + crease drawn OVER the torn outline — the fold
 * covers the ragged corner, content is inset past the flap), [watermark]
 * prints a faint scatter of category icons on the sheet, and [redMargin]
 * draws the red school-notebook margin line — the universal decorations,
 * all available on the torn slip exactly like the ruled page. Theme-aware
 * cream paper in both themes. [rotation] keeps the hand-placed notecard
 * feel.
 */
@Composable
fun TornPaperCard(
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    seed: Int? = null,
    ruled: Boolean = false,
    coffeeStains: Boolean = false,
    folded: Boolean = false,
    // v7.16 — universal decoration: the red school-notebook margin line can
    // now ride a torn slip too, exactly like the ruled paper.
    redMargin: Boolean = false,
    paperColor: NotePaperColor = NotePaperColor.CREAM,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    /** Same min-height floor as [PaperCard] — saved views pass it so short
     *  torn notes keep a proper slip shape. */
    minHeight: Dp = 0.dp,
    /** Explicit ruled-line cadence (Dp) — defaults to the bodyLarge line
     *  height; see [PaperCard.ruleSpacing]. */
    ruleSpacing: Dp? = null,
    /** Extra bottom space (Dp) — see [PaperCard.tailSpace]. */
    tailSpace: Dp = 0.dp,
    /** Watermark paper — see [PaperCard.watermark]. */
    watermark: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    // Each card remembers its own seed so every torn note gets a distinct
    // tear pattern — stable per composition, so typing never re-rolls the
    // rips mid-edit. Explicit seeds (e.g. a saved entry id) can pin a
    // pattern across views.
    val effectiveSeed = seed ?: remember { Random.nextInt(1, 1_000_000) }
    // The Shape instance is remembered — Surface would otherwise construct a
    // fresh one every recomposition, defeating the outline cache inside it.
    val tornShape = remember(effectiveSeed) { TornPaperShape(effectiveSeed) }
    val surface = notePaperSurface(paperColor)
    val edge = notePaperBorder(paperColor)
    // One shared tiling brush wraps the SHARED texture — no per-card brush
    // allocation (the seed makes each card's EDGE unique, so the generic
    // grain can be shared). The grain itself is drawn at FULL strength here
    // (it IS the torn tooth), with soft creases on top.
    // Hoisted (not built per frame): the torn canvas re-draws on every
    // frame while typing, and per-frame Brush allocation was one of the
    // original lag sources in this file.
    val paperInkColor = notePaperInk(paperColor)
    val sheen = remember { rigidCardSheen() }        // The torn outline can intrude up to ~9dp past the caller's inset
        // (worst case, scaled soft-tear amplitudes). Floor the inset so the
        // ragged edge NEVER clips the field text — especially the first
        // characters near the top-left corner, where two torn edges meet
        // and their inward bites compound diagonally. The red margin needs
        // the same left inset as [PaperCard] so the text never runs under
        // the vertical rule.
        val safeContentPadding = PaddingValues(
            start = maxOf(
                contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
                if (redMargin) 30.dp else 16.dp
            ),
            top = maxOf(
                contentPadding.calculateTopPadding(),
                14.dp
            ),
            // The folded dog-ear needs an end inset so text never runs under
            // the flap (same 24dp floor as [PaperCard]'s foldInsetDp + 2dp).
            end = maxOf(
                contentPadding.calculateRightPadding(LayoutDirection.Ltr),
                if (folded) 24.dp else 16.dp
            ),
            bottom = maxOf(
                contentPadding.calculateBottomPadding(),
                14.dp
            ) + tailSpace
        )
    // Ruled lines (\"rules on torn\"): same notebook cadence as [PaperCard].
    val density = LocalDensity.current
    val ruleColor = if (ruled) notePaperRule(paperColor) else Color.Unspecified
    val ruleSpacingPx = if (ruled) with(density) {
        if (ruleSpacing != null) ruleSpacing.toPx()
        else {
            val lh = MaterialTheme.typography.bodyLarge.lineHeight
            if (lh == TextUnit.Unspecified) 24.dp.toPx() else lh.toPx()
        }
    } else 0f
    val ruleStartPx = if (ruled) with(density) {
        safeContentPadding.calculateTopPadding().toPx()
    } + ruleSpacingPx else 0f

    Surface(
        shape = tornShape,
        color = surface,
        shadowElevation = 3.dp,
        modifier = modifier.heightIn(min = minHeight).rotate(rotation)
    ) {
        Box {
            // One Canvas: the grain texture + soft creases + (optionally)
            // the ruled lines + (optionally) the coffee stains + the
            // rigid-card sheen. Single draw calls per frame — cheap even
            // while typing. The sheen is drawn LAST so it reads ON TOP of
            // the grain — under it, the texture flattens the vertical light
            // gradient and the torn slip looks flat.
            // The torn edge can intrude up to ~3dp past the caller's inset;
            // the margin line is drawn at the same 22dp position [PaperCard]
            // uses, clear of the ragged left edge.
            val marginInset = with(density) { 22.dp.toPx() }
            Canvas(modifier = Modifier.matchParentSize()) {
                drawPaperTexture(
                    size, density, sharedGrainBrush, paperInkColor,
                    seed = effectiveSeed, grainAlpha = 1f
                )
                if (ruled) {
                    var y = ruleStartPx
                    while (y < size.height) {
                        drawLine(
                            color = ruleColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += ruleSpacingPx
                    }
                }
                if (redMargin) {
                    drawLine(
                        color = PaperMarginRed.copy(alpha = 0.55f),
                        start = Offset(marginInset, 0f),
                        end = Offset(marginInset, size.height),
                        strokeWidth = with(density) { 1.2.dp.toPx() }
                    )
                }
                if (coffeeStains) drawCoffeeStains(size, density, effectiveSeed)
                drawRect(brush = sheen)
            }
            // Watermark paper — a faint scatter of category icons behind the
            // text, clipped to the torn outline by the Surface.
            if (watermark) {
                PaperWatermarkGlyphs(ink = paperInkColor, seed = effectiveSeed)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(safeContentPadding),
                content = content
            )
            // Fold flap drawn ABOVE the content — the flap + crease shadow
            // sit on the torn paper, covering the ragged corner like a real
            // dog-ear (Surface clips to the torn outline, so the missing
            // corner shows the page background behind the flap). The flap is
            // shaded with the sheet's OWN ink so it reads as the paper's
            // backside on the torn grain instead of a pasted-on black wedge.
            if (folded) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawFoldFlap(size, density, surface, edge, paperInkColor)
                }
            }
        }
    }
}

/**
 * Dispatch helper — renders a capture's note-paper in the style it was
 * written in: torn styles → [TornPaperCard], otherwise the sharp ruled
 * [PaperCard]. Every decoration (rules / coffee / folded / red margin) is
 * decoded from the style's flags and passed to whichever base, so any
 * combination renders correctly. Used by the saved EntryDetail views so a
 * torn note stays torn and a ruled note stays ruled.
 */
@Composable
fun NotePaperCard(
    style: NotePaperStyle,
    modifier: Modifier = Modifier,
    ruled: Boolean = true,
    rotation: Float = 0f,
    corner: Dp = 0.dp,
    paperColor: NotePaperColor = NotePaperColor.CREAM,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    minHeight: Dp = 0.dp,
    /** Explicit ruled-line cadence (Dp) — forwarded to the paper base; see
     *  [PaperCard.ruleSpacing]. */
    ruleSpacing: Dp? = null,
    /** Extra bottom space (Dp) — forwarded to the paper base; see
     *  [PaperCard.tailSpace]. */
    tailSpace: Dp = 0.dp,
    /** v7.37 — stable torn-seam seed, forwarded to the paper base (see
     *  [PaperCard.seed]). Saved views pass a per-entry seed so the card's
     *  tear never re-rolls between opens. */
    seed: Int? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (style.torn) {
        TornPaperCard(
            modifier = modifier.heightIn(min = minHeight),
            rotation = rotation,
            ruled = style.ruled,
            coffeeStains = style.coffee,
            folded = style.folded,
            redMargin = style.redMargin,
            watermark = style.watermark,
            paperColor = paperColor,
            contentPadding = contentPadding,
            ruleSpacing = ruleSpacing,
            tailSpace = tailSpace,
            seed = seed,
            content = content
        )
    } else {
        // The ruled paper base is always ruled; [ruled] only relaxes the
        // plain RULED style for callers that explicitly ask (none do today).
        PaperCard(
            modifier = modifier.heightIn(min = minHeight),
            ruled = if (style == NotePaperStyle.RULED) ruled else true,
            rotation = rotation,
            roundedTop = style.roundedTop,
            corner = corner,
            paperColor = paperColor,
            contentPadding = contentPadding,
            coffeeStains = style.coffee,
            folded = style.folded,
            redMargin = style.redMargin,
            watermark = style.watermark,
            ruleSpacing = ruleSpacing,
            tailSpace = tailSpace,
            seed = seed,
            content = content
        )
    }
}

/**
 * v7.33 — watermark paper: a faint scatter of category icons printed on the
 * sheet's background — the page-backdrop language (CurioWatermarkBackdrop)
 * shrunk onto the paper. Seeded per card so each watermark sheet scatters
 * differently; drawn BEHIND the text (between the rules and the content)
 * so it never fights the ink; clipped to the paper's shape by the Surface.
 */
@Composable
private fun BoxScope.PaperWatermarkGlyphs(
    ink: Color,
    seed: Int
) {
    val rnd = remember(seed) { Random(seed * 0x51A7 + 7) }
    val glyphs = remember { CurioCategories.all.map { it.iconGlyph } }
    // Edge-biased placements — the writing area stays mostly clear, and the
    // glyphs hug the sheet's perimeter like the page backdrop does.
    val slots = listOf(
        BiasAlignment(-0.88f, -0.84f),
        BiasAlignment(0.90f, -0.72f),
        BiasAlignment(-0.94f, 0.16f),
        BiasAlignment(0.92f, 0.08f),
        BiasAlignment(-0.74f, 0.88f),
        BiasAlignment(0.88f, 0.86f)
    )
    slots.forEachIndexed { i, alignment ->
        val glyph = glyphs[rnd.nextInt(glyphs.size)]
        val size = (22 + rnd.nextInt(14)).dp
        val rotation = -16f + rnd.nextFloat() * 32f
        val alpha = 0.05f + rnd.nextFloat() * 0.05f
        CurioIcon(
            name = glyph,
            contentDescription = null,
            tint = ink.copy(alpha = alpha),
            size = size,
            modifier = Modifier
                .align(alignment)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

/**
 * The per-text-box note-paper picker — v7.16 UNIVERSAL: the base (Ruled /
 * Torn) and stackable decoration chips stay in one compact horizontal strip
 * (v7.53 — wrapping into multiple rows made the expanded editor too tall).
 * v7.18 — decorations STACK: Coffee / Folded / Red Margin are independent
 * toggles, so any combination can be on at once (a folded coffee page with
 * a red margin is legal), and, while the torn base is active, a "+ Rules"
 * chip toggles the ruled lines onto the torn slip. Switching bases keeps
 * the chosen decorations, so nothing is lost. Lives in the field's own
 * toolbar (alongside the format toolbox), NOT in a section-level row — so
 * each text box keeps its own independent paper look.
 */
@Composable
fun NotePaperStyleToggle(
    style: NotePaperStyle,
    onStyleChange: (NotePaperStyle) -> Unit,
    accent: Color = paperAccent(),
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // Keep the base (Ruled / Torn) and decoration chips on one compact
        // horizontal strip. The strip scrolls instead of wrapping, so the
        // picker never grows into a tall block on narrow phones; every
        // option remains available without changing the paper behavior.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Base — Ruled (sharp ruled page) · Torn (ragged slip).
            // Switching keeps the current decoration via [notePaperStyleOf].
            CompactPaperChip("Ruled", !style.torn, accent, enabled) {
                onStyleChange(notePaperStyleOf(
                    torn = false,
                    ruled = true,
                    coffee = style.coffee,
                    folded = style.folded,
                    redMargin = style.redMargin,
                    roundedTop = style.roundedTop,
                    watermark = style.watermark
                ))
            }
            CompactPaperChip("Torn", style.torn, accent, enabled) {
                onStyleChange(notePaperStyleOf(
                    torn = true,
                    ruled = style.ruled,
                    coffee = style.coffee,
                    folded = style.folded,
                    redMargin = style.redMargin,
                    roundedTop = style.roundedTop,
                    watermark = style.watermark
                ))
            }
            // UNIVERSAL decorations — the same chips apply to both bases.
            if (style.torn) {
                // Rules only matter on the torn slip — the ruled page is
                // always ruled by definition.
                CompactPaperChip("+ Rules", style.ruled, accent, enabled) {
                    onStyleChange(notePaperStyleOf(
                        torn = true,
                        ruled = !style.ruled,
                        coffee = style.coffee,
                        folded = style.folded,
                        redMargin = style.redMargin,
                        roundedTop = style.roundedTop,
                        watermark = style.watermark
                    ))
                }
            }
            // Independent STACKABLE toggles (v7.18) — each chip flips ONLY
            // its own flag and passes the others through, so decorations
            // combine instead of cancelling each other.
            CompactPaperChip("+ Coffee", style.coffee, accent, enabled && !style.watermark) {
                onStyleChange(notePaperStyleOf(
                    style.torn, style.ruled,
                    coffee = !style.coffee,
                    folded = style.folded,
                    redMargin = style.redMargin,
                    roundedTop = style.roundedTop,
                    watermark = style.watermark
                ))
            }
            CompactPaperChip("+ Folded", style.folded, accent, enabled && !style.watermark) {
                onStyleChange(notePaperStyleOf(
                    style.torn, style.ruled,
                    coffee = style.coffee,
                    folded = !style.folded,
                    redMargin = style.redMargin,
                    roundedTop = style.roundedTop,
                    watermark = style.watermark
                ))
            }
            CompactPaperChip("+ Red Margin", style.redMargin, accent, enabled && !style.watermark) {
                onStyleChange(notePaperStyleOf(
                    style.torn, style.ruled,
                    coffee = style.coffee,
                    folded = style.folded,
                    redMargin = !style.redMargin,
                    roundedTop = style.roundedTop,
                    watermark = style.watermark
                ))
            }
            // v7.33 — watermark paper: a faint scatter of category icons on
            // the sheet. It combines with the base + rounded top ONLY, so
            // enabling it clears coffee / folded / red margin (and disables
            // those chips while it's on); disabling restores the plain base.
            CompactPaperChip("+ Watermark", style.watermark, accent, enabled) {
                onStyleChange(notePaperStyleOf(
                    style.torn, style.ruled,
                    coffee = false,
                    folded = false,
                    redMargin = false,
                    roundedTop = style.roundedTop,
                    watermark = !style.watermark
                ))
            }
            if (!style.torn) {
                CompactPaperChip("+ Rounded top", style.roundedTop, accent, enabled) {
                    onStyleChange(notePaperStyleOf(
                        torn = false,
                        ruled = true,
                        coffee = style.coffee,
                        folded = style.folded,
                        redMargin = style.redMargin,
                        roundedTop = !style.roundedTop,
                        watermark = style.watermark
                    ))
                }
            }
        }
    }
}

/** Compact paper-style pill chip — label-only, tight padding, tiny font.
 *  v7.98 — theme-aware capsules: inactive chips follow the theme's muted
 *  tokens (outline-variant rim + on-surface-variant ink), active chips
 *  bloom in the paper accent — no hardcoded dark-mode color branches, so
 *  the strip reads correctly on the tool dock AND the tinted page. */
@Composable
private fun CompactPaperChip(
    label: String,
    active: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        // v27r — note-paper style chips are FIXED paper controls: the active
        // fill is a MODERATED tint of the accent (a solid paperAccent block
        // was too saturated and its ink unreadable in pastel mode) with the
        // accent itself as label ink; elevation stays flat 2dp.
        color = if (active) lerp(MaterialTheme.colorScheme.surfaceContainerHighest, accent, 0.45f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 2.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

/**
 * The per-text-box note-paper COLOR picker — a compact row of circular
 * swatches (cream / butter / pink / mint / sky / lilac) shown next to the
 * Ruled/Torn toggle in the field's toolbar. The active swatch wears a
 * check + accent ring. Same always-cream-in-both-themes rule: the sheet
 * color is theme-agnostic, chosen per text box and persisted per field.
 */
@Composable
fun NotePaperColorToggle(
    color: NotePaperColor,
    onColorChange: (NotePaperColor) -> Unit,
    accent: Color = paperAccent(),
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Collapsible: the six swatches hide behind a compact "Color" chip so
    // the toolbar rows stay clean; the chip's own dot shows the current
    // paper color, and tapping expands the swatch row below it.
    var expanded by remember { mutableStateOf(false) }
    // Theme-aware on the dock/page: collapsed follows the theme's muted
    // tokens, expanded blooms in the accent (no hardcoded dark-mode bumps).
    val ink = if (expanded) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(
            onClick = { expanded = !expanded },
            enabled = enabled,
            shape = RoundedCornerShape(10.dp),
            // v27r — see CompactPaperChip: the expanded paper toggle wears a
            // MODERATED accent tint, not a solid block; flat 2dp.
            color = if (expanded) lerp(MaterialTheme.colorScheme.surfaceContainerHighest, accent, 0.45f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live swatch of the current paper color — the border must
                // use CircleShape too, or a square outline would draw over
                // the circular fill (border defaults to RectangleShape).
                Box(
                    modifier = Modifier
                        // v27n — shadow BEFORE the fill (was painted on top
                        // of the color dot).
                        .shadow(1.dp, CircleShape)
                        .size(14.dp)
                        .background(notePaperSurface(color), CircleShape)
                )
                CurioIcon(
                    name = CurioIcons.Palette,
                    contentDescription = null,
                    tint = ink,
                    size = 14.dp
                )
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = ink
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotePaperColor.entries.forEach { candidate ->
                    val selected = candidate == color
                    Surface(
                        onClick = { onColorChange(candidate) },
                        enabled = enabled,
                        shape = CircleShape,
                        color = notePaperSurface(candidate),
                        // v27q — flat 2dp: selection reads through the check.
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(if (selected) 24.dp else 20.dp)
                            .semantics {
                                contentDescription =
                                    "${candidate.name.lowercase()} paper" + if (selected) " (selected)" else ""
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (selected) {
                                // A tiny paper-ink check inside the active
                                // swatch — the accent ring already marks it.
                                CurioIcon(
                                    name = CurioIcons.Check,
                                    contentDescription = null,
                                    tint = notePaperInk(candidate),
                                    size = 13.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


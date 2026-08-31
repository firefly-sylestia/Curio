package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.ui.theme.BungeeFontFamily
import com.curio.app.ui.theme.ChangaOneFontFamily
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.GeomFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.curio.app.ui.theme.PatrickHandFontFamily
import com.curio.app.ui.theme.PirataOneFontFamily
import com.curio.app.ui.theme.PlayfairDisplayFontFamily
import kotlin.math.sin
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton

// ─── Style enum ────────────────────────────────────────────────────────
enum class ShareCardStyle(val label: String, val glyph: String) {
    PAPER("Paper", "article"),
    VINYL("Vinyl", "album"),
    COLLAGE("Collage", "collections"),
    NEUMORPHIC("Clean", "lens"),
    EDITORIAL("Editorial", ".auto_stories"),
    MINIMAL("Minimal", "fiber_new"),
    SIGNATURE("Signature", "diamond"),
    CUSTOM("Custom", "auto_awesome")
}

// ─── Aspect + Content ──────────────────────────────────────────────────
enum class ShareCardAspect(val label: String, val widthDp: Int, val heightDp: Int) {
    PORTRAIT("9:16", 405, 720),
    CLASSIC("3:4", 450, 600)
}

data class ShareCardContent(
    val id: String,
    val label: String,
    val text: String,
    val rating: Int? = null
)

/**
 * Optional per-share arrangement of the title + quick-fact body on a card.
 * Positions are FRACTIONS of the card's width/height (0..1); bodyWidthFrac is
 * the quick-fact box width as a fraction of the card width; bodyScale is the
 * quick-fact text-size multiplier. When null the card uses its default layout.
 */
data class ShareCardArrangement(
    val titleX: Float = 0.5f,
    val titleY: Float = 0.34f,
    val bodyX: Float = 0.1f,
    val bodyY: Float = 0.62f,
    val bodyWidthFrac: Float = 0.78f,
    val bodyScale: Float = 1f,
    val arranged: Boolean = false
)

// ─── Curated share-card palette (3-4 beautiful tones cycling through categories) ───
data class ShareCardPalette(
    val bgBase: Color,
    val bgLight: Color,
    val bgMid: Color,
    val accent: Color,
    val accentDark: Color,
    val ink: Color,
    val inkFaint: Color
)

// Four curated tones — warm, beautiful, NOT derived from category colors.
// Each category maps to one of these by index, so every share card
// looks intentional and visually rich.
private val curatedTones = listOf(
    // Warm Rose — deep muted rose on warm cream
    ShareCardPalette(
        bgBase = Color(0xFFFDF0EE), bgLight = Color(0xFFFDF6F5), bgMid = Color(0xFFF8E0DC),
        accent = Color(0xFFB85C6E), accentDark = Color(0xFF8A3A4C),
        ink = Color(0xFF2C1A1E), inkFaint = Color(0xFF8A6B72)
    ),
    // Soft Sage — deep teal on greenish cream
    ShareCardPalette(
        bgBase = Color(0xFFF0F5F0), bgLight = Color(0xFFF7FAF7), bgMid = Color(0xFFDCE8DC),
        accent = Color(0xFF5E8A72), accentDark = Color(0xFF3D6B52),
        ink = Color(0xFF1A2420), inkFaint = Color(0xFF6B8A7C)
    ),
    // Golden Ochre — warm gold on pale parchment
    ShareCardPalette(
        bgBase = Color(0xFFFAF5EB), bgLight = Color(0xFFFDFBF5), bgMid = Color(0xFFF0E6D0),
        accent = Color(0xFFB08840), accentDark = Color(0xFF8A6520),
        ink = Color(0xFF2A2010), inkFaint = Color(0xFF8A7A60)
    ),
    // Deep Indigo — moody purple on cool parchment
    ShareCardPalette(
        bgBase = Color(0xFFF2EFF8), bgLight = Color(0xFFF8F6FC), bgMid = Color(0xFFE2DDEF),
        accent = Color(0xFF6A5A9A), accentDark = Color(0xFF4A3A7A),
        ink = Color(0xFF1C1630), inkFaint = Color(0xFF7A6A90)
    )
)

private fun paletteFor(accent: Color): ShareCardPalette {
    // Cycle through curated tones using the accent hash
    return curatedTones[Math.abs(accent.hashCode()) % curatedTones.size]
}

// ─── Family → available styles mapping ─────────────────────────────────
fun availableStylesForFamily(family: CategoryFamily, topicName: String = ""): List<ShareCardStyle> {
    val hasCustom = topicVariant(topicName, family) != null
    val custom = if (hasCustom) listOf(ShareCardStyle.CUSTOM) else emptyList()
    return custom + when (family) {
    CategoryFamily.MUSIC -> listOf(ShareCardStyle.PAPER, ShareCardStyle.VINYL, ShareCardStyle.COLLAGE, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.MOVIES -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.BOOKS -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.VISUAL_ART -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.SCIENCE -> listOf(ShareCardStyle.PAPER, ShareCardStyle.NEUMORPHIC, ShareCardStyle.COLLAGE, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.ANIME_COMICS -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.GAMES -> listOf(ShareCardStyle.PAPER, ShareCardStyle.NEUMORPHIC, ShareCardStyle.COLLAGE, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.MYTHOLOGY -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.SPORTS -> listOf(ShareCardStyle.PAPER, ShareCardStyle.NEUMORPHIC, ShareCardStyle.COLLAGE, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.FOOD -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.INTERNET -> listOf(ShareCardStyle.PAPER, ShareCardStyle.NEUMORPHIC, ShareCardStyle.COLLAGE, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
    CategoryFamily.WILDCARD -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC, ShareCardStyle.EDITORIAL, ShareCardStyle.MINIMAL, ShareCardStyle.SIGNATURE)
}

}
const val QUICK_FACT_ID = "quick_fact"
const val CUSTOM_FACT_ID = "custom_fact"
const val NO_FACT_ID = "no_fact"

private fun quoteFontSize(length: Int): TextUnit = when {
    length > 900 -> 15.sp; length > 650 -> 17.sp; length > 420 -> 19.sp
    length > 260 -> 21.sp; else -> 24.sp
}

/** Dynamic font size for quick fact text — scales down for longer facts
 *  so the text never gets cut off. Short facts get a slightly larger,
 *  more impactful size. More aggressive steps for very long texts. */
private fun quickFactFontSize(length: Int): TextUnit = when {
    length > 300 -> 7.5.sp
    length > 220 -> 8.5.sp
    length > 150 -> 9.5.sp
    length > 100 -> 10.5.sp
    length > 50 -> 11.5.sp
    else -> 12.5.sp
}

/** Even smaller for 3:4 aspect ratio. */
private fun quickFactFontSize34(length: Int): TextUnit = when {
    length > 300 -> 5.sp
    length > 220 -> 6.sp
    length > 150 -> 7.sp
    length > 100 -> 8.sp
    length > 50 -> 8.5.sp
    else -> 9.5.sp
}

// ═══════════════════════════════════════════════════════════════════════
// MAIN DISPATCHER
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun TopicShareCard(
    topicName: String,
    categoryName: String,
    categoryGlyph: String,
    accent: Color,
    factText: String,
    sharerName: String,
    aspect: ShareCardAspect,
    modifier: Modifier = Modifier,
    style: ShareCardStyle = ShareCardStyle.PAPER,
    ratingStars: Int? = null,
    categoryFamily: CategoryFamily = CategoryFamily.WILDCARD,
    quoteText: String? = null,
    quoteAuthor: String? = null,
    userPhoto: androidx.compose.ui.graphics.ImageBitmap? = null,
    byline: String = "",
    polaroidCaption: String = "",
    classicSignature: Boolean = false,
    onPhotoTap: (() -> Unit)? = null,
    arrangement: ShareCardArrangement? = null,
    bodyScale: Float = 1f
) {
    val display = topicName.substringBeforeLast(" (")
    // Extract year from trailing parentheses — "Appetite for Destruction (1987)" → "1987"
    val year = topicName.substringAfterLast("(").substringBeforeLast(")").takeIf { it.all { c -> c.isDigit() } && it.length == 4 }
    val palette = paletteFor(accent)
    when (style) {
        ShareCardStyle.PAPER -> PaperCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year, bodyScale, arrangement)
        ShareCardStyle.VINYL -> VinylCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.COLLAGE -> CollageCard(display, topicName, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, userPhoto, byline, year, polaroidCaption, onPhotoTap)
        ShareCardStyle.NEUMORPHIC -> NeumorphicCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.EDITORIAL -> EditorialCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.MINIMAL -> MinimalCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.SIGNATURE -> SignatureCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year, classicSignature)
        ShareCardStyle.CUSTOM -> CustomCard(display, topicName, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 0 — PAPER (main, all categories)
// Category-tinted parchment + torn bottom + frost pane
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun PaperCard(
    display: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null,
    bodyScale: Float = 1f, arrangement: ShareCardArrangement? = null
) {
    val qSize = quoteText?.let { quoteFontSize(it.length) } ?: 0.sp
    val arr = arrangement?.takeIf { it.arranged }
    Box(
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
            .shadow(4.dp, RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(palette.bgLight, palette.bgBase, palette.bgMid)), RoundedCornerShape(6.dp))
    ) {
        // Rich paper texture layers — visible grain + fiber lines + subtle speckle
        Canvas(Modifier.fillMaxSize()) { drawPaperTexture(palette) }
        Canvas(Modifier.fillMaxSize()) { drawPaperFibers(palette) }
        Canvas(Modifier.fillMaxSize()) { drawTornBottom(palette) }
        // Subtle inner shadow around edges + faint border highlight for depth
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.04f), Color.Transparent, Color.Black.copy(alpha = 0.03f))), Offset.Zero, Size(w, h))
            // Extremely subtle inner border highlight
            drawRoundRect(Color.White.copy(alpha = 0.12f), Offset.Zero, Size(w, h), CornerRadius(6.dp.toPx()), style = Stroke(0.8f))
        }
        Watermark(family, categoryGlyph, palette.ink.copy(alpha = 0.06f), display.hashCode())

        if (arr != null) {
            // Arrange mode — title + quick-fact box are free-positioned by the
            // user; header row + footer stay pinned. Positions are fractions of
            // the content box, offsets use the box's top-left as the anchor.
            BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp).zIndex(3f)) {
                val cw = maxWidth.value; val ch = maxHeight.value - 0f
                Column(Modifier.fillMaxWidth()) { HeaderRow(categoryName, categoryGlyph, palette) }
                // Title — positioned by titleX/titleY
                Box(
                    Modifier
                        .offset(x = (cw * arr.titleX).dp, y = (ch * arr.titleY).dp)
                        .widthIn(max = (cw * 0.92f).dp)
                ) {
                    Text(display, style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = ChangaOneFontFamily, lineHeight = 40.sp, color = palette.ink),
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                // Quick-fact box — positioned by bodyX/bodyY, box width by bodyWidthFrac
                Box(
                    Modifier
                        .offset(x = (cw * arr.bodyX).dp, y = (ch * arr.bodyY).dp)
                        .width((cw * arr.bodyWidthFrac).dp)
                ) {
                    val qfs = if (aspect == ShareCardAspect.CLASSIC) quickFactFontSize34(factText.length)
                             else quickFactFontSize(factText.length)
                    val scaled = (qfs.value * bodyScale).sp
                    FrostPane(palette) {
                        Text(factText, style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = LoraFontFamily, fontSize = scaled,
                            lineHeight = (scaled.value * 1.4f).sp),
                            color = palette.ink, maxLines = if (aspect == ShareCardAspect.PORTRAIT) 20 else 14,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                    Footer(sharerName, quoteText, quoteAuthor, palette)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                HeaderRow(categoryName, categoryGlyph, palette)
                MiddleContent(display, factText, aspect, palette, ratingStars, quoteText, qSize, quoteAuthor, byline, year, bodyScale)
                Footer(sharerName, quoteText, quoteAuthor, palette)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 1 — VINYL (MUSIC family primary)
// Scrapbook: title + artist + fact + info panel, vinyl bleeds right
// ════════════════════════════════════════════════════════════════════
@Composable
private fun VinylCard(
    display: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null
) {
    val roseBg = Color(0xFFF5E6E0)
    val roseDusty = Color(0xFFD4A0A0)
    val roseLight = Color(0xFFF0D0C8)
    val inkDark = Color(0xFF3A2820)
    val roseFaint = Color(0xFFE8C8C0)
    // For quote cards, the title is the author/byline — display IS the quote.
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display
    val body = quoteText ?: factText

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(roseBg, RoundedCornerShape(6.dp))) {
        // Paper texture
        Canvas(Modifier.fillMaxSize()) { drawPaperTexture(palette) }

        // Decorative elements — sparkles, dots, music icon
        Canvas(Modifier.fillMaxSize().zIndex(0f)) {
            val w = size.width; val h = size.height
            // Sparkle dots
            drawCircle(roseDusty.copy(alpha = 0.18f), 3f, Offset(w * 0.42f, h * 0.08f))
            drawCircle(roseDusty.copy(alpha = 0.12f), 2f, Offset(w * 0.55f, h * 0.10f))
            drawCircle(roseDusty.copy(alpha = 0.15f), 2.5f, Offset(w * 0.38f, h * 0.12f))
            drawCircle(roseDusty.copy(alpha = 0.10f), 2f, Offset(w * 0.62f, h * 0.06f))
            // Dot grid pattern — right side
            for (row in 0..3) for (col in 0..3) {
                drawCircle(roseDusty.copy(alpha = 0.08f), 1.2f, Offset(w * 0.72f + col * 6f, h * 0.14f + row * 6f))
            }
            // Small music note icon placeholder (faded square)
            drawRoundRect(roseFaint.copy(alpha = 0.30f), Offset(w * 0.78f, h * 0.04f), Size(36f, 36f), CornerRadius(4f))
        }

        // Vinyl record — large, bleeding right edge
        val vinylDiameter = 220.dp
        val sleeveSize = 260.dp
        // Dusty rose sleeve circle behind vinyl
        Canvas(
            modifier = Modifier.align(Alignment.CenterEnd)
                .size(sleeveSize)
                .offset(x = 35.dp, y = 80.dp)
        ) {
            drawCircle(roseLight.copy(alpha = 0.30f), radius = size.minDimension / 2f)
        }
        // Vinyl record
        Canvas(
            modifier = Modifier.align(Alignment.CenterEnd)
                .size(vinylDiameter)
                .offset(x = 25.dp, y = 90.dp)
        ) {
            drawVinylPartial(roseDusty)
        }
        // Thin arcs around vinyl
        Canvas(
            modifier = Modifier.align(Alignment.CenterEnd)
                .size(280.dp)
                .offset(x = 15.dp, y = 75.dp)
        ) {
            drawArc(
                color = roseDusty.copy(alpha = 0.10f), startAngle = -30f, sweepAngle = 60f,
                useCenter = false, style = Stroke(0.8.dp.toPx()),
                topLeft = Offset(0f, size.height * 0.1f),
                size = Size(size.width, size.height * 0.8f)
            )
            drawArc(
                color = roseDusty.copy(alpha = 0.06f), startAngle = 150f, sweepAngle = 60f,
                useCenter = false, style = Stroke(0.6.dp.toPx()),
                topLeft = Offset(size.width * 0.05f, size.height * 0.15f),
                size = Size(size.width * 0.9f, size.height * 0.7f)
            )
        }

        // Watermark glyphs
        Watermark(family, categoryGlyph, roseDusty.copy(alpha = 0.04f), display.hashCode())

        // ── Content layout ──
        Column(modifier = Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 14.dp).zIndex(1f)) {
            // Category pill + lightbulb icon
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(12.dp), color = roseDusty) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        CurioIcon(name = categoryGlyph, tint = Color.White, size = 12.dp)
                        Text(categoryName, style = TextStyle(fontFamily = LoraFontFamily, fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp), color = Color.White)
                    }
                }
                CurioIcon(name = CurioIcons.Lightbulb, tint = roseDusty.copy(alpha = 0.40f), size = 18.dp)
            }

            Spacer(Modifier.height(12.dp))

            // Title — strong serif
            Text(title, style = TextStyle(
                fontFamily = ChangaOneFontFamily, fontSize = 28.sp,
                lineHeight = 32.sp, fontWeight = FontWeight.Normal, color = inkDark
            ), maxLines = 2, overflow = TextOverflow.Ellipsis)

            // Artist / byline
            if (quoteText == null && byline.isNotBlank()) {
                Text(byline, style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 13.sp, color = roseDusty
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (year != null) {
                Text(year, style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 13.sp, color = roseDusty
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Accent underline
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.size(width = 32.dp, height = 2.dp)) {
                drawRoundRect(roseDusty, cornerRadius = CornerRadius(1f))
            }

            Spacer(Modifier.height(10.dp))

            // Body text — Lora serif, with semi-transparent cream background for readability over vinyl
            val bodySize = when {
                body.length > 280 -> 9.sp; body.length > 180 -> 10.sp; else -> 10.5.sp
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFFDF0EE).copy(alpha = 0.85f),
                modifier = Modifier.widthIn(max = 220.dp)
            ) {
                Text(body, style = TextStyle(
                    fontFamily = LoraFontFamily, fontSize = bodySize,
                    lineHeight = (bodySize.value * 1.50f).sp, color = inkDark.copy(alpha = 0.88f),
                    fontStyle = if (quoteText != null) FontStyle.Italic else FontStyle.Normal
                ), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), maxLines = 10, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(10.dp))

            Spacer(Modifier.weight(1f))

            // Footer — centered, subtle
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(width = 60.dp, height = 1.dp)) {
                    drawLine(roseDusty.copy(alpha = 0.20f), Offset.Zero, Offset(size.width, 0f))
                }
                Spacer(Modifier.height(4.dp))
                CurioIcon(name = CurioIcons.Lightbulb, tint = roseDusty.copy(alpha = 0.35f), size = 12.dp)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (sharerName.isNotBlank()) "$sharerName \u00b7 via Curio" else "via Curio",
                    style = TextStyle(fontFamily = GeomFontFamily, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, color = roseDusty.copy(alpha = 0.65f)),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Favorite song — small user-set corner chip (replaces the old info box) ──
        val is34v = aspect == ShareCardAspect.CLASSIC
        val favSong = AppPreferences.favoriteSongState.trim()
        if (favSong.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFDF0EE).copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = if (is34v) 12.dp else 16.dp, bottom = if (is34v) 28.dp else 24.dp)
                    .widthIn(max = if (is34v) 150.dp else 180.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    CurioIcon(name = "headphones", tint = roseDusty.copy(alpha = 0.95f), size = if (is34v) 10.dp else 12.dp)
                    Column {
                        Text("FAVORITE SONG", style = TextStyle(fontFamily = GeomFontFamily, fontSize = if (is34v) 5.sp else 6.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp, color = inkDark.copy(alpha = 0.78f)))
                        Text(favSong, style = TextStyle(fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                            fontSize = if (is34v) 6.5.sp else 8.sp, lineHeight = if (is34v) 8.sp else 10.sp,
                            color = inkDark.copy(alpha = 0.80f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// STYLE 2 — COLLAGE (torn paper + polaroid + observatory)
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun CollageCard(
    display: String, topicName: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    userPhoto: androidx.compose.ui.graphics.ImageBitmap? = null,
    byline: String = "", year: String? = null,
    polaroidCaption: String = "",
    onPhotoTap: (() -> Unit)? = null
) {
    val topCream = Color(0xFFF5EDE0)
    val bottomSage = Color(0xFF6B7C65)
    val bottomDark = Color(0xFF4A5A44)
    val inkDark = Color(0xFF3A4A34)
    val tornEdge = Color(0xFFE8DFD0)
    val sagePill = Color(0xFF4A5A44)

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(topCream, RoundedCornerShape(6.dp))) {
        // ── Layered paper + botanical lower field with a natural torn seam ──
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val tearY = h * 0.42f
            drawRect(bottomSage, Offset.Zero, Size(w, h))
            drawRect(Brush.verticalGradient(listOf(bottomSage, bottomDark)), Offset(0f, h * 0.70f), Size(w, h * 0.30f))
            drawNaturalTearPanel(tearY = tearY, top = topCream, edge = tornEdge, shadow = bottomDark.copy(alpha = 0.22f))
            val footerY = h * 0.86f
            drawPath(
                Path().apply {
                    moveTo(0f, footerY + 4f)
                    cubicTo(w * 0.22f, footerY - 18f, w * 0.43f, footerY + 12f, w * 0.62f, footerY - 8f)
                    cubicTo(w * 0.78f, footerY - 24f, w * 0.90f, footerY + 2f, w, footerY - 14f)
                    lineTo(w, h); lineTo(0f, h); close()
                },
                bottomDark.copy(alpha = 0.82f)
            )
        }

        // Watermark glyphs on both paper and green field so the collage feels intentional.
        Watermark(family, categoryGlyph, inkDark.copy(alpha = 0.045f), display.hashCode())
        Watermark(family, categoryGlyph, Color.White.copy(alpha = 0.045f), display.hashCode() + 17)

        // ── TOP SECTION: title + metadata + quote (left) + polaroid (right) ──
        BoxWithConstraints(Modifier.fillMaxSize().zIndex(2f)) {
            val cw = maxWidth.value; val ch = maxHeight.value

            // Polaroid — right side, tilted, with tape + handwritten name
            val pW = cw * 0.34f; val pH = pW * 1.18f
            val pX = cw * 0.62f; val pY = ch * 0.035f
            val polaroidLabel = polaroidCaption.ifBlank {
                if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio"
            }

            Box(Modifier.offset(pX.dp, pY.dp).size(pW.dp, pH.dp)
                .graphicsLayer {
                    rotationZ = -3f
                    shadowElevation = 6f
                    shape = RoundedCornerShape(3.dp)
                    clip = false
                }
                .background(Color.White, RoundedCornerShape(3.dp))
                .clickable(enabled = onPhotoTap != null && userPhoto == null) { onPhotoTap?.invoke() }
            ) {
                // Tape on top
                Canvas(Modifier.offset((pW * 0.23f).dp, 1.dp).size((pW * 0.46f).dp, 12.dp)) {
                    drawRoundRect(Color(0xFFD9BE8A).copy(alpha = 0.55f), Offset.Zero, Size(size.width, size.height), CornerRadius(2.dp.toPx()))
                }
                // Photo area — tappable when empty
                Canvas(Modifier.offset(5.dp, 5.dp).size((pW - 10).dp, (pH * 0.68f).dp)) {
                    if (userPhoto != null) {
                        drawImage(userPhoto,
                            dstOffset = androidx.compose.ui.unit.IntOffset(0, 0),
                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
                        drawRect(Color(0xFFD4A574).copy(alpha = 0.10f))
                    } else {
                        drawRoundRect(Color(0xFFE0D8CC), Offset.Zero, Size(size.width, size.height), CornerRadius(2.dp.toPx()))
                        // Hint — camera icon + text when no photo
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        // Camera body
                        val camW = size.width * 0.30f
                        val camH = camW * 0.70f
                        drawRoundRect(
                            Color(0xFFB0A898).copy(alpha = 0.35f),
                            Offset(cx - camW / 2f, cy - camH / 2f - 6f),
                            Size(camW, camH),
                            CornerRadius(4f)
                        )
                        // Lens circle
                        drawCircle(Color(0xFFB0A898).copy(alpha = 0.30f), camW * 0.22f, Offset(cx, cy - 6f))
                    }
                }
                // Hint text overlay when no photo
                if (userPhoto == null) {
                    Column(
                        modifier = Modifier
                            .offset(5.dp, (pH * 0.52f).dp)
                            .size((pW - 10).dp, (pH * 0.16f).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CurioIcon(name = "photo_camera", tint = Color(0xFFB0A898).copy(alpha = 0.50f), size = 14.dp)
                        Text("Tap to add photo", style = TextStyle(
                            fontFamily = LoraFontFamily, fontSize = 7.sp,
                            color = Color(0xFFB0A898).copy(alpha = 0.60f)
                        ))
                    }
                }
                // Polaroid finish — inner hairline frame + a glassy sheen band
                // over the photo so it reads as a real instant-print (v...)
                Canvas(Modifier.fillMaxSize()) {
                    val pws = size.width; val phs = size.height
                    drawRoundRect(Color(0xFF70543A).copy(alpha = 0.30f), Offset(5f, 5f), Size(pws - 10f, phs - 10f), CornerRadius(2.dp.toPx()), style = Stroke(1.3.dp.toPx()))
                    val sheen = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.0f), Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.0f)),
                        startY = 5f, endY = phs * 0.68f
                    )
                    drawRect(sheen, Offset(5f, 5f), Size(pws - 10f, phs * 0.68f))
                }

                // Handwritten name below photo — constrained width + lineHeight
                // >= fontSize so long captions ellipsize (not clip) and never squish.
                val capFont = (pW * 0.065f).coerceIn(11f, 15f)
                Text(polaroidLabel, style = TextStyle(
                    fontFamily = PatrickHandFontFamily, fontWeight = FontWeight.Normal,
                    fontSize = capFont.sp, color = inkDark,
                    lineHeight = (capFont * 1.2f).sp
                ), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(8.dp, (pH * 0.78f).dp).width((pW - 16).dp))
            }

            // Title — retro Bungee, dark green, top-left (v... — retro face +
            // tighter leading so long names like "Curious Explorer" never clip)
            Column(modifier = Modifier.offset(22.dp, (ch * 0.05f).dp).width((cw * 0.60f).dp)) {
                Text(if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display, style = TextStyle(
                    fontFamily = BungeeFontFamily, fontSize = (cw * 0.072f).coerceIn(19f, 30f).sp,
                    lineHeight = (cw * 0.082f).coerceIn(22f, 34f).sp,
                    fontWeight = FontWeight.Normal, color = inkDark
                ), maxLines = 5, overflow = TextOverflow.Ellipsis)

                Spacer(Modifier.height(6.dp))

                // Metadata — small caps, letter-spaced
                val metaParts = mutableListOf<String>()
                if (quoteText == null && byline.isNotBlank()) metaParts.add(byline.uppercase())
                if (year != null) metaParts.add(year)
                if (metaParts.isNotEmpty()) {
                    Text(metaParts.joinToString(" \u2022 "), style = TextStyle(
                        fontFamily = LoraFontFamily, fontSize = 9.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold,
                        color = inkDark.copy(alpha = 0.55f)
                    ), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // ── MIDDLE SECTION: category pill + body text + decorative bottom ──
        Column(modifier = Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = if (aspect == ShareCardAspect.PORTRAIT) 214.dp else 190.dp, bottom = 18.dp).zIndex(1f)) {
            // Category pill
            Surface(shape = RoundedCornerShape(14.dp), color = sagePill) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurioIcon(name = categoryGlyph, tint = Color.White, size = 14.dp)
                    Text(categoryName, style = TextStyle(
                        fontFamily = LoraFontFamily, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp), color = Color.White)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Body text — serif, generous line height
            val body = quoteText ?: factText
            val bodySize = when {
                body.length > 280 -> 10.sp; body.length > 180 -> 11.sp; else -> 12.sp
            }
            Text(body, style = TextStyle(
                fontFamily = LoraFontFamily, fontSize = bodySize,
                lineHeight = (bodySize.value * 1.55f).sp, color = Color.White.copy(alpha = 0.92f),
                fontStyle = if (quoteText != null) FontStyle.Italic else FontStyle.Normal
            ), maxLines = 18, overflow = TextOverflow.Ellipsis)

            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(6.dp))
                StarRow(ratingStars, palette)
            }

            Spacer(Modifier.weight(1f))

            // ── Footer area ──
            Spacer(Modifier.height(8.dp))

            // Footer credit
            Text(
                if (sharerName.isNotBlank()) "$sharerName \u00b7 via Curio" else "via Curio",
                style = TextStyle(fontFamily = GeomFontFamily, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = Color.White.copy(alpha = 0.66f)),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// STYLE 3 — CLEAN / NEUMORPHIC (category monolith depth poster)
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun NeumorphicCard(
    display: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null
) {
    val ink = Color(0xFF101010)
    val paper = Color(0xFFF8F6EF)
    val categoryGlow = palette.accent
    val categoryDeep = palette.accentDark
    val body = quoteText ?: factText
    // For quote cards, the title is the author/byline — display IS the quote, so showing both duplicates it.
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(categoryDeep, RoundedCornerShape(6.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(Brush.verticalGradient(listOf(categoryGlow.copy(alpha = 0.95f), categoryDeep, ink)))
            // Soft ambient glow top-left — radial falloff so there's no hard edge
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(w * 0.12f, h * 0.10f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.12f, h * 0.10f)
            )
            // Depth shadow bottom-right — radial falloff
            drawCircle(
                Brush.radialGradient(
                    listOf(categoryDeep.copy(alpha = 0.34f), Color.Black.copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(w * 0.96f, h * 0.74f),
                    radius = w * 0.80f
                ),
                radius = w * 0.80f,
                center = Offset(w * 0.96f, h * 0.74f)
            )
        }



        // Oversized category glyph — signature element of Clean card
        CurioIcon(
            name = categoryGlyph,
            tint = Color.White.copy(alpha = 0.18f),
            size = 150.dp,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 32.dp, y = 18.dp)
                .graphicsLayer { rotationZ = -10f }
        )

        Box(Modifier.fillMaxSize().padding(22.dp).zIndex(1f)) {
            Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.72f), modifier = Modifier.align(Alignment.TopStart)) {
                Text(categoryName.uppercase(), style = TextStyle(fontFamily = GeomFontFamily, fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp), color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }

            Column(Modifier.align(Alignment.CenterStart).padding(start = 4.dp, end = 4.dp, top = 0.dp), horizontalAlignment = Alignment.Start) {
                Text(title, style = TextStyle(
                    fontFamily = ChangaOneFontFamily, fontSize = 31.sp, lineHeight = 33.sp,
                    fontWeight = FontWeight.Normal, color = Color.White
                ), maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.90f))
                val metaParts = mutableListOf<String>()
                if (byline.isNotBlank()) metaParts.add(byline.uppercase())
                if (year != null) metaParts.add(year)
                if (metaParts.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(metaParts.joinToString("  /  "), style = TextStyle(
                        fontFamily = GeomFontFamily, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.3.sp, color = Color.White.copy(alpha = 0.56f)
                    ), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 16.dp)) {
                val bodySize = when { body.length > 350 -> 8.sp; body.length > 260 -> 9.sp; body.length > 180 -> 9.5.sp; else -> 10.5.sp }
                Text(body, style = TextStyle(
                    fontFamily = LoraFontFamily,
                    fontStyle = if (quoteText != null) FontStyle.Italic else FontStyle.Normal,
                    fontSize = bodySize, lineHeight = (bodySize.value * 1.40f).sp,
                    color = Color.White.copy(alpha = 0.88f),
                    shadow = Shadow(Color.Black.copy(alpha = 0.62f), Offset(0f, 2f), 5f)
                ), maxLines = if (aspect == ShareCardAspect.PORTRAIT) 8 else 6, overflow = TextOverflow.Ellipsis)
                if (ratingStars != null && ratingStars > 0) {
                    Spacer(Modifier.height(7.dp))
                    StarRow(ratingStars, palette.copy(accent = Color.White, ink = Color.White, inkFaint = Color.White.copy(alpha = 0.45f)))
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    if (quoteText != null && !quoteAuthor.isNullOrBlank()) "— $quoteAuthor" else if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio",
                    style = TextStyle(fontFamily = GeomFontFamily, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp, color = Color.White.copy(alpha = 0.72f)),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════
// STYLE 4 — EDITORIAL (magazine spread layout)
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun EditorialCard(
    display: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null
) {
    val cream = Color(0xFFFAF7F0)
    val inkDark = Color(0xFF1C1814)
    val accentRule = palette.accent
    val body = quoteText ?: factText
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(cream, RoundedCornerShape(6.dp))) {
        // Subtle texture
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val s = (w * 1000 + h).toInt()
            for (i in 0 until 60) {
                val x = ((s * (i + 1) * 7919) % 10000) / 10000f * w
                val y = ((s * (i + 1) * 6271) % 10000) / 10000f * h
                drawCircle(Color(0xFFD0C8B8).copy(alpha = 0.04f), 1.5f, Offset(x, y))
            }
        }

        // Broadsheet masthead + retro Bungee headline (v... redesign)
        Column(modifier = Modifier.fillMaxSize().padding(start = 26.dp, end = 26.dp, top = 22.dp, bottom = 20.dp)) {
            // Kicker — category in retro Bungee beside an accent slug
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(7.dp).height(15.dp).background(accentRule))
                Text(categoryName.uppercase(), style = TextStyle(
                    fontFamily = BungeeFontFamily, fontSize = 13.sp,
                    letterSpacing = 2.4.sp, color = inkDark
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(7.dp))
            // Double masthead rule — thick flag + hairline
            Canvas(Modifier.fillMaxWidth().height(3.dp)) { drawRect(inkDark.copy(alpha = 0.85f)) }
            Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawRect(inkDark.copy(alpha = 0.28f)) }

            Spacer(Modifier.height(16.dp))

            // Headline — retro Bungee, big
            Text(title, style = TextStyle(
                fontFamily = BungeeFontFamily, fontSize = 30.sp,
                lineHeight = 34.sp, color = inkDark
            ), maxLines = 4, overflow = TextOverflow.Ellipsis)

            // Byline — year deck
            val metaParts = mutableListOf<String>()
            if (quoteText == null && byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text(metaParts.joinToString(" \u2014 "), style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 12.sp, color = inkDark.copy(alpha = 0.55f)
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(15.dp))
            // Hairline under the deck
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(inkDark.copy(alpha = 0.16f), Offset.Zero, Offset(size.width, 0f))
            }
            Spacer(Modifier.height(12.dp))

            // Body — clean serif with a standing Bungee initial
            val bodySize = when {
                body.length > 350 -> 8.5.sp; body.length > 260 -> 9.5.sp
                body.length > 180 -> 10.sp; else -> 11.sp
            }
            val initial = body.take(1)
            val bodyRest = if (body.length > 1) body.drop(1) else ""
            Row(Modifier.fillMaxWidth()) {
                if (initial.isNotEmpty()) {
                    Text(initial, style = TextStyle(
                        fontFamily = BungeeFontFamily, fontSize = (bodySize.value * 2.9f).sp,
                        lineHeight = (bodySize.value * 2.4f).sp, color = accentRule
                    ), modifier = Modifier.padding(end = 5.dp, top = 2.dp))
                }
                Text(bodyRest, style = TextStyle(
                    fontFamily = LoraFontFamily, fontSize = bodySize,
                    lineHeight = (bodySize.value * 1.45f).sp, color = inkDark.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Medium
                ), maxLines = if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, overflow = TextOverflow.Ellipsis)
            }

            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(7.dp))
                StarRow(ratingStars, palette)
            }

            Spacer(Modifier.weight(1f))

            // Colophon — thin rule + italic credit with an accent slug
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(inkDark.copy(alpha = 0.14f), Offset.Zero, Offset(size.width, 0f))
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(6.dp).height(10.dp).background(accentRule.copy(alpha = 0.85f)))
                Text(
                    if (sharerName.isNotBlank()) "$sharerName \u2014 Curio" else "Curio",
                    style = TextStyle(fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                        fontSize = 10.sp, color = inkDark.copy(alpha = 0.50f)),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 5 — MINIMAL (ultra-clean whitespace design)
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun MinimalCard(
    display: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null
) {
    val bg = Color(0xFFFFFDF9)
    val inkDark = Color(0xFF1A1A1A)
    val accent = palette.accent
    val body = quoteText ?: factText
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(bg, RoundedCornerShape(6.dp))) {
        // Hairline inner frame — structure without clutter
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            drawRoundRect(inkDark.copy(alpha = 0.10f), Offset.Zero, Size(size.width, size.height),
                CornerRadius(10.dp.toPx()), style = Stroke(1.dp.toPx()))
        }

        // Giant faint category initial — the signature placement, bottom-right
        if (categoryName.isNotBlank()) {
            Text(categoryName.take(1).uppercase(), style = TextStyle(
                fontFamily = BungeeFontFamily, fontSize = 175.sp,
                lineHeight = 150.sp, color = accent.copy(alpha = 0.10f)
            ), modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer { rotationZ = -6f }
                .offset(y = 18.dp)
                .padding(end = 6.dp))
        }

        Column(modifier = Modifier.fillMaxSize().padding(start = 30.dp, end = 26.dp, top = 34.dp, bottom = 26.dp)) {
            // Category — tiny uppercase Bungee beside a diamond accent
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(8.dp).graphicsLayer { rotationZ = 45f }.background(accent))
                Text(categoryName.uppercase(), style = TextStyle(
                    fontFamily = BungeeFontFamily, fontSize = 10.sp, letterSpacing = 3.sp,
                    color = inkDark.copy(alpha = 0.55f)
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(26.dp))

            // Title — big retro Bungee
            Text(title, style = TextStyle(
                fontFamily = BungeeFontFamily, fontSize = 32.sp, lineHeight = 35.sp, color = inkDark
            ), maxLines = 5, overflow = TextOverflow.Ellipsis)

            // Byline / year
            if (byline.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(byline, style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 12.sp, color = inkDark.copy(alpha = 0.50f)
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (year != null) {
                Spacer(Modifier.height(6.dp))
                Text(year, style = TextStyle(fontFamily = LoraFontFamily, fontSize = 12.sp, color = inkDark.copy(alpha = 0.40f)))
            }

            Spacer(Modifier.weight(1f))

            // Thick accent rule above the body block — editorial anchor
            Box(Modifier.width(56.dp).height(4.dp).background(accent))
            Spacer(Modifier.height(16.dp))

            // Body — serif, bottom-anchored
            val bodySize = when { body.length > 350 -> 8.5.sp; body.length > 260 -> 9.5.sp; body.length > 180 -> 10.5.sp; else -> 11.5.sp }
            Text(body, style = TextStyle(
                fontFamily = LoraFontFamily, fontSize = bodySize,
                lineHeight = (bodySize.value * 1.50f).sp, color = inkDark.copy(alpha = 0.78f)
            ), maxLines = if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, overflow = TextOverflow.Ellipsis)

            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(8.dp))
                StarRow(ratingStars, palette)
            }
            Spacer(Modifier.height(14.dp))

            // Credit — tiny, right-aligned for a deliberate off-balance
            Text(
                if (sharerName.isNotBlank()) "$sharerName \u00b7 Curio" else "Curio",
                style = TextStyle(fontFamily = GeomFontFamily, fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold, color = inkDark.copy(alpha = 0.35f)),
                maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 6 — SIGNATURE (unique per-category design)
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun SignatureCard(
    display: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null,
    classicSignature: Boolean = false
) {
    val body = quoteText ?: factText
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display
    // Design pick: Classic (f6dd7f19 family designs) > Detailed experiment > current
    val sig = when {
        classicSignature -> signatureDesignClassic(categoryName, family)
        AppPreferences.detailedSignatureElementsState -> signatureDesignDetailed(categoryName, family)
        else -> signatureDesign(categoryName, family)
    }

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(sig.cornerRadius.dp)).background(sig.bg, RoundedCornerShape(sig.cornerRadius.dp))) {
        // Background pattern/texture
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            sig.drawBackground(this, w, h)
        }

        // Meta parts (byline / year) shared across layouts
        val metaParts = mutableListOf<String>()
        if (quoteText == null && byline.isNotBlank()) metaParts.add(byline)
        if (year != null) metaParts.add(year)

        // Body text size shrinks for long content
        val bodySize = when {
            body.length > 350 -> sig.bodySize - 2.5f
            body.length > 260 -> sig.bodySize - 1.5f
            body.length > 180 -> sig.bodySize - 0.5f
            else -> sig.bodySize
        }.coerceAtLeast(7f)

        val bodyMaxLines = if (aspect == ShareCardAspect.PORTRAIT) 14 else 10
        val footerText = if (sharerName.isNotBlank()) "$sharerName \u00b7 Curio" else "Curio"

        // ── Category badge (shared) ────────────────────────────
        @Composable
        fun CategoryBadge(centered: Boolean = false) {
            Surface(shape = RoundedCornerShape(sig.badgeRadius), color = sig.badgeColor,
                modifier = if (centered) Modifier else Modifier) {
                Row(Modifier.padding(horizontal = sig.badgeHPadding, vertical = sig.badgeVPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CurioIcon(name = categoryGlyph, tint = sig.badgeInk, size = sig.badgeIconSize)
                    Text(categoryName.uppercase(), style = TextStyle(
                        fontFamily = GeomFontFamily, fontSize = sig.badgeFontSize,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = sig.badgeLetterSpacing,
                        color = sig.badgeInk
                    ), maxLines = 1)
                }
            }
        }

        // ── Title (shared) ─────────────────────────────────────
        @Composable
        fun TitleText(centered: Boolean = false) {
            Text(title, style = TextStyle(
                fontFamily = sig.titleFont, fontSize = sig.titleSize,
                lineHeight = sig.titleLineHeight, color = sig.titleColor
            ), maxLines = 4, overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = if (centered) Modifier.fillMaxWidth() else Modifier)
        }

        // ── Meta (shared) ──────────────────────────────────────
        @Composable
        fun MetaText(centered: Boolean = false) {
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(sig.metaSpacer))
                Text(metaParts.joinToString(sig.metaSeparator), style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = sig.metaSize, color = sig.metaColor
                ), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    modifier = if (centered) Modifier.fillMaxWidth() else Modifier)
            }
        }

        // ── Body (shared) ─────────────────────────────────────
        @Composable
        fun BodyText(centered: Boolean = false) {
            Text(body, style = TextStyle(
                fontFamily = LoraFontFamily, fontSize = bodySize.sp,
                lineHeight = (bodySize * sig.bodyLineHeight).sp,
                color = sig.bodyColor
            ), maxLines = bodyMaxLines, overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = if (centered) Modifier.fillMaxWidth() else Modifier)
        }

        // ── Footer (shared) ────────────────────────────────────
        @Composable
        fun FooterText(centered: Boolean = false) {
            Text(footerText, style = TextStyle(fontFamily = sig.footerFont, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold, color = sig.footerColor),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = if (centered) Modifier.fillMaxWidth() else Modifier)
        }

        when (sig.layout) {
            // ═══ STANDARD — badge top, title, spacer, body bottom, footer ═══
            SignatureLayout.STANDARD -> {
                Column(modifier = Modifier.fillMaxSize().padding(sig.padding)) {
                    CategoryBadge()
                    Spacer(Modifier.height(sig.titleTopSpacer))
                    TitleText()
                    MetaText()
                    Spacer(Modifier.weight(1f))
                    BodyText()
                    if (ratingStars != null && ratingStars > 0) {
                        Spacer(Modifier.height(6.dp))
                        StarRow(ratingStars, palette)
                    }
                    Spacer(Modifier.height(sig.footerSpacer))
                    FooterText()
                }
            }

            // ═══ CENTERED — everything centered, badge top-center ═══
            SignatureLayout.CENTERED -> {
                Column(modifier = Modifier.fillMaxSize().padding(sig.padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top) {
                    CategoryBadge(centered = true)
                    Spacer(Modifier.height(sig.titleTopSpacer))
                    TitleText(centered = true)
                    MetaText(centered = true)
                    Spacer(Modifier.weight(1f))
                    BodyText(centered = true)
                    if (ratingStars != null && ratingStars > 0) {
                        Spacer(Modifier.height(6.dp))
                        StarRow(ratingStars, palette)
                    }
                    Spacer(Modifier.height(sig.footerSpacer))
                    FooterText(centered = true)
                }
            }

            // ═══ BOTTOM — badge top, big spacer, everything stacked at bottom ═══
            SignatureLayout.BOTTOM -> {
                Column(modifier = Modifier.fillMaxSize().padding(sig.padding)) {
                    CategoryBadge()
                    Spacer(Modifier.weight(1f))
                    TitleText()
                    Spacer(Modifier.height(sig.metaSpacer))
                    MetaText()
                    Spacer(Modifier.height(8.dp))
                    BodyText()
                    if (ratingStars != null && ratingStars > 0) {
                        Spacer(Modifier.height(6.dp))
                        StarRow(ratingStars, palette)
                    }
                    Spacer(Modifier.height(sig.footerSpacer))
                    FooterText()
                }
            }

            // ═══ SIDE — left column: badge+title+footer; right: body ═══
            SignatureLayout.SIDE -> {
                Row(modifier = Modifier.fillMaxSize().padding(sig.padding),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Left panel — badge, title, meta, footer
                    Column(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                        CategoryBadge()
                        Spacer(Modifier.height(sig.titleTopSpacer))
                        TitleText()
                        MetaText()
                        Spacer(Modifier.weight(1f))
                        FooterText()
                    }
                    // Right panel — body text
                    Column(modifier = Modifier.weight(0.58f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom) {
                        BodyText()
                        if (ratingStars != null && ratingStars > 0) {
                            Spacer(Modifier.height(6.dp))
                            StarRow(ratingStars, palette)
                        }
                    }
                }
            }

            // ═══ OVERLAY — title overlaid center, body at bottom ═══
            SignatureLayout.OVERLAY -> {
                Column(modifier = Modifier.fillMaxSize().padding(sig.padding)) {
                    CategoryBadge()
                    Spacer(Modifier.weight(1f))
                    TitleText(centered = true)
                    MetaText(centered = true)
                    Spacer(Modifier.weight(1f))
                    BodyText()
                    if (ratingStars != null && ratingStars > 0) {
                        Spacer(Modifier.height(6.dp))
                        StarRow(ratingStars, palette)
                    }
                    Spacer(Modifier.height(sig.footerSpacer))
                    FooterText()
                }
            }

            // ═══ POSTER — badge top, large title center, body+footer bottom ═══
            SignatureLayout.POSTER -> {
                Column(modifier = Modifier.fillMaxSize().padding(sig.padding),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CategoryBadge(centered = true)
                    Spacer(Modifier.weight(0.6f))
                    TitleText(centered = true)
                    Spacer(Modifier.height(2.dp))
                    MetaText(centered = true)
                    Spacer(Modifier.weight(1f))
                    BodyText()
                    if (ratingStars != null && ratingStars > 0) {
                        Spacer(Modifier.height(6.dp))
                        StarRow(ratingStars, palette)
                    }
                    Spacer(Modifier.height(sig.footerSpacer))
                    FooterText(centered = true)
                }
            }
        }
    }
}

// ─── Signature per-category design data ────────────────────────────────
/**
 * Distinct text-placement layouts for signature cards. Each category can
 * pick a layout so the cards don't all look the same — the title, body,
 * badge and footer shift position per-layout.
 *
 * STANDARD  — badge top-left, title below, spacer, body bottom, footer.
 * CENTERED  — badge centered top, title centered, body centered, footer.
 * BOTTOM    — badge top, big spacer, title+meta+body all stacked at bottom.
 * SIDE      — left column: badge+title+footer; right column: body text.
 * OVERLAY   — title overlaid center over the art, body at the very bottom.
 * POSTER    — badge top, title large center, meta under it, body+footer bottom.
 */
private enum class SignatureLayout { STANDARD, CENTERED, BOTTOM, SIDE, OVERLAY, POSTER }

private data class SignatureDesign(
    val bg: Color, val cornerRadius: Float,
    val drawBackground: DrawScope.(w: Float, h: Float) -> Unit,
    val padding: PaddingValues,
    val badgeColor: Color, val badgeInk: Color, val badgeRadius: Dp,
    val badgeHPadding: Dp, val badgeVPadding: Dp,
    val badgeIconSize: Dp, val badgeFontSize: TextUnit, val badgeLetterSpacing: TextUnit,
    val titleTopSpacer: Dp, val titleFont: FontFamily, val titleSize: TextUnit,
    val titleLineHeight: TextUnit, val titleColor: Color,
    val metaSpacer: Dp, val metaSeparator: String, val metaSize: TextUnit, val metaColor: Color,
    val bodySize: Float, val bodyLineHeight: Float, val bodyColor: Color,
    val footerSpacer: Dp, val footerFont: FontFamily, val footerColor: Color,
    val layout: SignatureLayout = SignatureLayout.STANDARD
)


// ═══════════════════════════════════════════════════════════════════════
// TOPIC-SPECIFIC SIGNATURE VARIANTS (50+ popular topics)
// Each popular topic gets a completely unique design — colors, textures,
// decorative elements. keyword match on topic name → unique SignatureDesign.
// ══════════════════════════════════════════════════════════════════════════════════════════════════════
// Helper: draw a snowflake with 6-fold symmetry
private fun DrawScope.drawSnowflake(cx: Float, cy: Float, r: Float, color: Color) {
    for (arm in 0 until 6) {
        val angle = Math.toRadians((60.0 * arm)).toFloat()
        val ex = cx + kotlin.math.cos(angle) * r
        val ey = cy + kotlin.math.sin(angle) * r
        drawLine(color, Offset(cx, cy), Offset(ex, ey), strokeWidth = 1.2f)
        val mx = cx + kotlin.math.cos(angle) * r * 0.55f
        val my = cy + kotlin.math.sin(angle) * r * 0.55f
        val ba = angle + 0.5f; val bb = angle - 0.5f
        drawLine(color, Offset(mx, my), Offset(mx + kotlin.math.cos(ba) * r * 0.25f, my + kotlin.math.sin(ba) * r * 0.25f), strokeWidth = 0.8f)
        drawLine(color, Offset(mx, my), Offset(mx + kotlin.math.cos(bb) * r * 0.25f, my + kotlin.math.sin(bb) * r * 0.25f), strokeWidth = 0.8f)
    }
}

// Helper: draw an 8-point star
private fun DrawScope.drawStar(cx: Float, cy: Float, outerR: Float, innerR: Float, color: Color) {
    val path = Path()
    for (i in 0 until 16) {
        val angle = Math.toRadians((360.0 * i / 16 - 90.0)).toFloat()
        val r = if (i % 2 == 0) outerR else innerR
        val px = cx + kotlin.math.cos(angle) * r
        val py = cy + kotlin.math.sin(angle) * r
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}

// TOPIC-SPECIFIC CUSTOM DESIGNS (50+ popular topics)
// Category-validated: only triggers when topic matches its category.
// ══════════════════════════════════════════════════════════════════════════════════════════════════
private fun topicVariant(topicName: String, family: CategoryFamily): SignatureDesign? {
    val t = topicName.uppercase().trim()

    // ══ GAMES ══
    if (family == CategoryFamily.GAMES) {
        if (t.contains("POKEMON") || t.contains("PIKACHU")) return SignatureDesign(
            bg = Color(0xFFFFF8E1), cornerRadius = 10f,
            drawBackground = { w, h ->
                val s2 = kotlin.math.min(w, h) * 0.30f
                val cx = w * 0.78f; val cy = h * 0.18f
                drawCircle(Color.Black.copy(alpha = 0.18f), s2 * 0.52f, Offset(cx + 2f, cy + 2f))
                drawCircle(Color.White.copy(alpha = 0.90f), s2 * 0.50f, Offset(cx, cy))
                drawRect(Color(0xFFCC0000).copy(alpha = 0.85f), Offset(cx - s2 * 0.50f, cy - s2 * 0.50f), Size(s2, s2 * 0.50f))
                drawCircle(Color(0xFF222222).copy(alpha = 0.70f), s2 * 0.50f, Offset(cx, cy), style = Stroke(2.5f))
                drawLine(Color(0xFF222222).copy(alpha = 0.70f), Offset(cx - s2 * 0.50f, cy), Offset(cx + s2 * 0.50f, cy), strokeWidth = 2.5f)
                drawCircle(Color(0xFF222222).copy(alpha = 0.70f), s2 * 0.12f, Offset(cx, cy), style = Stroke(2.5f))
                drawCircle(Color.White, s2 * 0.09f, Offset(cx, cy))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFCC0000), badgeInk = Color.White,
            badgeRadius = 10.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF333333),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFCC0000),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A4030).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFCC0000).copy(alpha = 0.65f)
        )
        if (t.contains("MARIO")) return SignatureDesign(
            bg = Color(0xFFE52521), cornerRadius = 4f,
            drawBackground = { w, h ->
                val bw = w * 0.10f; val bh = h * 0.035f
                for (row in 0 until 5) { val off = (row % 2) * bw * 0.5f; for (col in 0 until 12) { val x = col * bw + off; val y = row * bh * 1.3f + h * 0.02f; if (x < w * 0.98f && y < h * 0.22f) { drawRoundRect(Color(0xFFB01810).copy(alpha = 0.30f), Offset(x, y), Size(bw * 0.92f, bh), CornerRadius(1.5f)); drawLine(Color(0xFFD04030).copy(alpha = 0.35f), Offset(x + 1f, y), Offset(x + bw * 0.92f - 1f, y), strokeWidth = 0.8f) } } }
                drawRoundRect(Color(0xFFFFD700).copy(alpha = 0.35f), Offset(w * 0.78f, h * 0.04f), Size(w * 0.08f, h * 0.08f), CornerRadius(3f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFCC0000), badgeInk = Color.White,
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFFFFFFF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFFD700),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFFFF0E0).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFFD700).copy(alpha = 0.70f)
        )
        if (t.contains("ZELDA")) return SignatureDesign(
            bg = Color(0xFF1A3C2A), cornerRadius = 6f,
            drawBackground = { w, h ->
                val cx = w * 0.80f; val cy = h * 0.14f; val s2 = w * 0.065f
                val path = Path().apply { moveTo(cx, cy - s2); lineTo(cx + s2 * 0.87f, cy + s2 * 0.5f); lineTo(cx - s2 * 0.87f, cy + s2 * 0.5f); close() }
                drawPath(path, Color(0xFFD4AF37).copy(alpha = 0.35f))
                drawPath(path, Color(0xFFD4AF37).copy(alpha = 0.35f), style = Stroke(1.5f))
                for (i in 0 until 6) { val x = w * 0.08f + i * w * 0.14f; val th = h * 0.08f + ((i * 7919) % 100) / 100f * h * 0.05f; drawLine(Color(0xFF0A2A18).copy(alpha = 0.35f), Offset(x, h * 0.88f), Offset(x, h * 0.88f - th), strokeWidth = 2f); drawCircle(Color(0xFF0A2A18).copy(alpha = 0.35f), th * 0.4f, Offset(x, h * 0.88f - th)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD4AF37), badgeInk = Color(0xFF1A3C2A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD4AF37),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF7AA060),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8E0C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF7AA060).copy(alpha = 0.65f)
        )
        if (t.contains("MINECRAFT")) return SignatureDesign(
            bg = Color(0xFF8B6914), cornerRadius = 2f,
            drawBackground = { w, h ->
                val bs = w * 0.06f
                for (row in 0 until 16) for (col in 0 until 10) { val x = col * bs; val y = row * bs * 0.55f; val c = when((row + col) % 3) { 0 -> Color(0xFF5D8C2E); 1 -> Color(0xFF7A5520); else -> Color(0xFF9B7B3A) }; drawRect(c.copy(alpha = 0.35f), Offset(x, y), Size(bs * 0.92f, bs * 0.50f)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF5D8C2E), badgeInk = Color.White,
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF8E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF5D8C2E),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE8D8B0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF5D8C2E).copy(alpha = 0.65f)
        )
        if (t.contains("FORTNITE")) return SignatureDesign(
            bg = Color(0xFF0B1628), cornerRadius = 4f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 25) { val angle = i * 14f; val rad = Math.toRadians(angle.toDouble()).toFloat(); val r = w * 0.04f + i * w * 0.006f; drawCircle(Color(0xFF00D4FF).copy(alpha = 0.18f), 1.8f, Offset(w * 0.78f + kotlin.math.cos(rad) * r, h * 0.20f + kotlin.math.sin(rad) * r * 0.6f)) }
                for (i in 0 until 50) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.28f), 1f, Offset(x, y)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF00D4FF), badgeInk = Color(0xFF0B1628),
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF00D4FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF0099BB),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFB0C8E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF0099BB).copy(alpha = 0.65f)
        )
        if (t.contains("GTA")) return SignatureDesign(
            bg = Color(0xFF0A0A0A), cornerRadius = 4f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 15) { val x = w * 0.05f + i * w * 0.06f; drawLine(Color(0xFF00C853).copy(alpha = 0.20f), Offset(x, h * 0.02f), Offset(x, h * 0.98f), strokeWidth = 0.5f) }
                for (i in 0 until 10) { val y = h * 0.05f + i * h * 0.09f; drawLine(Color(0xFF00C853).copy(alpha = 0.15f), Offset(w * 0.02f, y), Offset(w * 0.98f, y), strokeWidth = 0.5f) }
                drawCircle(Color(0xFF00C853).copy(alpha = 0.12f), w * 0.15f, Offset(w * 0.80f, h * 0.15f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF00C853), badgeInk = Color(0xFF0A0A0A),
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF00C853),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF00AA44),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFB0D0B0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF00AA44).copy(alpha = 0.65f)
        )
        if (t.contains("PAC-MAN") || t.contains("PACMAN")) return SignatureDesign(
            bg = Color(0xFF000000), cornerRadius = 6f,
            drawBackground = { w, h ->
                for (i in 0 until 20) { val x = w * 0.05f + i * w * 0.047f; drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.20f), 2f, Offset(x, h * 0.50f)) }
                drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.25f), w * 0.10f, Offset(w * 0.75f, h * 0.20f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFEB3B), badgeInk = Color(0xFF000000),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFEB3B),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFCCBB00),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0E0B0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFCCBB00).copy(alpha = 0.65f)
        )
    }

    // ══ FILMS & ANIMATION ══
    if (family == CategoryFamily.MOVIES) {
        if (t.contains("STAR WARS")) return SignatureDesign(
            bg = Color(0xFF0A0A1A), cornerRadius = 6f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 80) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.25f), 1f, Offset(x, y)) }
                for (i in 0 until 4) { drawLine(Color(0xFF4FC3F7).copy(alpha = 0.21f), Offset(0f, h * 0.20f + i * h * 0.18f), Offset(w, h * 0.20f + i * h * 0.18f), strokeWidth = 0.5f) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FC3F7), badgeInk = Color(0xFF0A0A1A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFE0F0FF),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF4FC3F7),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0D0E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF4FC3F7).copy(alpha = 0.65f)
        )
        // FIX: match "AVENGERS" not "MARVEL" (MARVEL matches biology topics)
        if (t.contains("AVENGERS") || t.contains("SPIDER-MAN")) return SignatureDesign(
            bg = Color(0xFF1A0A0A), cornerRadius = 4f,
            drawBackground = { w, h ->
                for (i in 0 until 14) for (j in 0 until 18) { val x = w * 0.02f + i * w * 0.028f; val y = h * 0.50f + j * h * 0.022f; drawCircle(Color(0xFFD32F2F).copy(alpha = 0.25f), (2.0f - j * 0.10f).coerceAtLeast(0.5f), Offset(x, y)) }
                drawRoundRect(Color(0xFFD32F2F).copy(alpha = 0.35f), Offset(w * 0.58f, h * 0.04f), Size(w * 0.38f, h * 0.28f), CornerRadius(2f), style = Stroke(2f))
                drawLine(Color(0xFFFFD700).copy(alpha = 0.35f), Offset(w * 0.06f, h * 0.05f), Offset(w * 0.18f, h * 0.05f), strokeWidth = 2.5f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD32F2F), badgeInk = Color(0xFFFFD700),
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFFFF0E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFD32F2F),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFE0D0C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFD32F2F).copy(alpha = 0.65f)
        )
        if (t.contains("BATMAN")) return SignatureDesign(
            bg = Color(0xFF1A1A2E), cornerRadius = 6f,
            drawBackground = { w, h ->
                for (i in 0 until 25) { val x = ((i * 7919) % 1000) / 1000f * w; val len = 20f + ((i * 3571) % 100) / 100f * 30f; drawLine(Color(0xFF1565C0).copy(alpha = 0.28f), Offset(x, h * 0.10f + ((i * 6271) % 100) / 100f * h * 0.6f), Offset(x, h * 0.10f + ((i * 6271) % 100) / 100f * h * 0.6f + len), strokeWidth = 0.6f) }
                drawCircle(Color(0xFF0D1B3E).copy(alpha = 0.35f), w * 0.18f, Offset(w * 0.78f, h * 0.12f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF1565C0), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFB0C8E0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF1565C0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF90A8C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1565C0).copy(alpha = 0.65f)
        )
        if (t.contains("HARRY POTTER")) return SignatureDesign(
            bg = Color(0xFF1A0A2E), cornerRadius = 8f,
            drawBackground = { w, h ->
                for (i in 0 until 5) { val t2 = i / 5f; drawCircle(Color(0xFFD4AF37).copy(alpha = 0.35f - t2 * 0.10f), 2.5f - t2, Offset(w * 0.85f - t2 * w * 0.12f, h * 0.10f + t2 * h * 0.10f)) }
                for (i in 0 until 30) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h * 0.5f; drawCircle(Color(0xFFD4AF37).copy(alpha = 0.21f), 1.5f, Offset(x, y)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD4AF37), badgeInk = Color(0xFF1A0A2E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD4AF37),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF9A7AD0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8B8E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFD4AF37).copy(alpha = 0.65f)
        )
        if (t.contains("LORD OF THE RINGS") || t.contains("HOBBIT")) return SignatureDesign(
            bg = Color(0xFF2A2A1A), cornerRadius = 8f,
            drawBackground = { w, h ->
                val path = Path().apply { moveTo(w * 0.05f, h * 0.85f); lineTo(w * 0.25f, h * 0.55f); lineTo(w * 0.40f, h * 0.70f); lineTo(w * 0.55f, h * 0.50f); lineTo(w * 0.70f, h * 0.65f); lineTo(w * 0.85f, h * 0.45f); lineTo(w * 0.95f, h * 0.60f); lineTo(w * 0.95f, h * 0.85f); close() }
                drawPath(path, Color(0xFF1A1A0A).copy(alpha = 0.20f))
                drawCircle(Color(0xFFC9A959).copy(alpha = 0.35f), w * 0.055f, Offset(w * 0.80f, h * 0.15f), style = Stroke(2f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A959), badgeInk = Color(0xFF2A2A1A),
            badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFC9A959),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8A7A40),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD0C8A0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A959).copy(alpha = 0.55f)
        )
        if (t.contains("DISNEY")) return SignatureDesign(
            bg = Color(0xFF0D1B3E), cornerRadius = 12f,
            drawBackground = { w, h ->
                val path = Path().apply {
                    moveTo(w * 0.30f, h * 0.80f); lineTo(w * 0.30f, h * 0.50f); lineTo(w * 0.32f, h * 0.45f); lineTo(w * 0.35f, h * 0.50f); lineTo(w * 0.35f, h * 0.42f); lineTo(w * 0.37f, h * 0.38f); lineTo(w * 0.39f, h * 0.42f); lineTo(w * 0.42f, h * 0.48f); lineTo(w * 0.45f, h * 0.35f); lineTo(w * 0.48f, h * 0.48f); lineTo(w * 0.50f, h * 0.42f); lineTo(w * 0.52f, h * 0.38f); lineTo(w * 0.54f, h * 0.42f); lineTo(w * 0.55f, h * 0.50f); lineTo(w * 0.58f, h * 0.45f); lineTo(w * 0.60f, h * 0.50f); lineTo(w * 0.60f, h * 0.80f); close()
                }
                drawPath(path, Color(0xFFFF6B9D).copy(alpha = 0.21f))
                for (i in 0 until 10) { val x = w * 0.15f + i * w * 0.07f; val y = h * 0.08f + kotlin.math.sin(i * 1.2f).toFloat() * h * 0.03f; drawCircle(Color(0xFFFFD700).copy(alpha = 0.35f), 1.5f, Offset(x, y)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6B9D), badgeInk = Color.White,
            badgeRadius = 12.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFB0C8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF6B9D),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C0D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF6B9D).copy(alpha = 0.65f)
        )
        if (t.contains("FROZEN")) return SignatureDesign(
            bg = Color(0xFFE3F2FD), cornerRadius = 10f,
            drawBackground = { w, h ->
                drawSnowflake(w * 0.20f, h * 0.12f, w * 0.08f, Color(0xFF2196F3).copy(alpha = 0.30f))
                drawSnowflake(w * 0.75f, h * 0.08f, w * 0.055f, Color(0xFF2196F3).copy(alpha = 0.32f))
                drawSnowflake(w * 0.50f, h * 0.20f, w * 0.04f, Color(0xFF2196F3).copy(alpha = 0.21f))
                drawSnowflake(w * 0.85f, h * 0.18f, w * 0.03f, Color(0xFF2196F3).copy(alpha = 0.18f))
                drawSnowflake(w * 0.12f, h * 0.22f, w * 0.025f, Color(0xFF2196F3).copy(alpha = 0.14f))
                for (i in 0 until 5) { val x = w * 0.15f + i * w * 0.16f; val y = h * 0.04f + ((i * 3571) % 100) / 100f * h * 0.05f; drawStar(x, y, 3f, 1.5f, Color(0xFF90CAF9).copy(alpha = 0.35f)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF2196F3), badgeInk = Color.White,
            badgeRadius = 10.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF1565C0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF2196F3),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF1A3A5A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF2196F3).copy(alpha = 0.65f)
        )
        if (t.contains("MATRIX")) return SignatureDesign(
            bg = Color(0xFF0A0A0A), cornerRadius = 4f,
            drawBackground = { w, h ->
                for (col in 0 until 18) { val x = w * 0.05f + col * w * 0.053f; for (row in 0 until 22) { val y = h * 0.02f + row * h * 0.042f; drawCircle(Color(0xFF00E676).copy(alpha = if ((col + row) % 4 == 0) 0.12f else 0.04f), 1.5f, Offset(x, y)) } }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF00E676), badgeInk = Color(0xFF0A0A0A),
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = GeomFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF00E676),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF00C853),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFB0D0B0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF00C853).copy(alpha = 0.65f)
        )
        if (t.contains("INTERSTELLAR") || t.contains("BLACK HOLE")) return SignatureDesign(
            bg = Color(0xFF0A0A14), cornerRadius = 6f,
            drawBackground = { w, h ->
                drawOval(Color(0xFFFF6D00).copy(alpha = 0.35f), Offset(w * 0.50f, h * 0.08f), Size(w * 0.40f, h * 0.08f), style = Stroke(2.5f))
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 50) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.21f), 1f, Offset(x, y)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6D00), badgeInk = Color(0xFF0A0A14),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE0D0C0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFFF6D00),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0B0A0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF6D00).copy(alpha = 0.65f)
        )
        if (t.contains("TITANIC")) return SignatureDesign(
            bg = Color(0xFF0A0A1E), cornerRadius = 6f,
            drawBackground = { w, h ->
                val path = Path().apply { moveTo(0f, h * 0.60f); for (i in 0..30) { lineTo(i * w / 30f, h * 0.60f + kotlin.math.sin(i * 0.35f).toFloat() * h * 0.025f) } }
                drawPath(path, Color(0xFFD4AF37).copy(alpha = 0.30f), style = Stroke(1.5f))
                for (i in 0 until 30) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h * 0.50f; drawCircle(Color.White.copy(alpha = 0.21f), 1f, Offset(x, y)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD4AF37), badgeInk = Color(0xFF0A0A1E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFD4AF37),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8A8AA0),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFC0C0D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD4AF37).copy(alpha = 0.55f)
        )
        if (t.contains("COCO")) return SignatureDesign(
            bg = Color(0xFF4A148C), cornerRadius = 8f,
            drawBackground = { w, h -> for (i in 0 until 20) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h; drawCircle(Color(0xFFFF6D00).copy(alpha = 0.25f), 3f, Offset(x, y)) } },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6D00), badgeInk = Color(0xFF4A148C),
            badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFB080),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF6D00),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C0E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF6D00).copy(alpha = 0.65f)
        )
        if (t.contains("LION KING")) return SignatureDesign(
            bg = Color(0xFFFF8F00), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawLine(Color(0xFF5D4037).copy(alpha = 0.30f), Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 1.5f)
                drawCircle(Color(0xFFFFD700).copy(alpha = 0.35f), w * 0.12f, Offset(w * 0.75f, h * 0.30f))
                drawCircle(Color(0xFFFFD700).copy(alpha = 0.28f), w * 0.16f, Offset(w * 0.75f, h * 0.30f), style = Stroke(1f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF5D4037), badgeInk = Color.White,
            badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2010),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF5D4037),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A3020).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF5D4037).copy(alpha = 0.65f)
        )
        if (t.contains("INCEPTION")) return SignatureDesign(
            bg = Color(0xFF0A1428), cornerRadius = 6f,
            drawBackground = { w, h ->
                val cx = w * 0.75f; val cy = h * 0.18f
                for (i in 1..4) { drawOval(Color(0xFFFF8F00).copy(alpha = 0.20f / i), Offset(cx - i * w * 0.04f, cy - i * h * 0.03f), Size(i * w * 0.08f, i * h * 0.06f), style = Stroke(0.8f)) }
                for (i in 0 until 40) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.15f), 0.8f, Offset(x, y)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF8F00), badgeInk = Color(0xFF0A1428),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFD080),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFFF8F00),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0C0D0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF8F00).copy(alpha = 0.65f)
        )
        if (t.contains("AVATAR")) return SignatureDesign(
            bg = Color(0xFF0A2020), cornerRadius = 6f,
            drawBackground = { w, h ->
                for (i in 0 until 30) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h; drawCircle(Color(0xFF00BCD4).copy(alpha = 0.25f), 2f, Offset(x, y)) }
                drawCircle(Color(0xFF00BCD4).copy(alpha = 0.15f), w * 0.12f, Offset(w * 0.80f, h * 0.15f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF00BCD4), badgeInk = Color(0xFF0A2020),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF80DEEA),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF00BCD4),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0D8D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF00BCD4).copy(alpha = 0.65f)
        )
        if (t.contains("TOY STORY")) return SignatureDesign(
            bg = Color(0xFF1565C0), cornerRadius = 6f,
            drawBackground = { w, h ->
                for (i in 0 until 8) { val x = w * 0.10f + i * w * 0.10f; drawStar(x, h * 0.10f + ((i * 3571) % 100) / 100f * h * 0.05f, 3f, 1.5f, Color(0xFFFFEB3B).copy(alpha = 0.35f)) }
                for (i in 0 until 5) { val x = w * 0.15f + i * w * 0.15f; drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.15f), w * 0.04f, Offset(x, h * 0.08f)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD32F2F), badgeInk = Color.White,
            badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFEB3B),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFD32F2F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0D0E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFD32F2F).copy(alpha = 0.65f)
        )
        if (t.contains("FINDING NEMO")) return SignatureDesign(
            bg = Color(0xFF0D47A1), cornerRadius = 8f,
            drawBackground = { w, h ->
                for (i in 0 until 8) { val x = w * 0.10f + i * w * 0.10f; val r = 3f + i * 0.5f; drawCircle(Color.White.copy(alpha = 0.20f - i * 0.02f), r, Offset(x, h * 0.12f + i * h * 0.02f)) }
                val wavePath = Path().apply { moveTo(0f, h * 0.80f); for (i in 0..20) { lineTo(i * w / 20f, h * 0.80f + kotlin.math.sin(i * 0.5f).toFloat() * h * 0.02f) } }
                drawPath(wavePath, Color(0xFF42A5F5).copy(alpha = 0.25f), style = Stroke(1.5f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6D00), badgeInk = Color.White,
            badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFCC80),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF6D00),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0D8F0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF6D00).copy(alpha = 0.65f)
        )
        if (t.contains("WALL-E")) return SignatureDesign(
            bg = Color(0xFF8D4004), cornerRadius = 4f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 30) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.18f), 1f, Offset(x, y)) }
                drawRoundRect(Color(0xFF42A5F5).copy(alpha = 0.20f), Offset(w * 0.75f, h * 0.10f), Size(w * 0.15f, h * 0.12f), CornerRadius(3f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF42A5F5), badgeInk = Color.White,
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFE0B0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF42A5F5),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF42A5F5).copy(alpha = 0.65f)
        )
        if (t.contains("INSIDE OUT")) return SignatureDesign(
            bg = Color(0xFF1565C0), cornerRadius = 8f,
            drawBackground = { w, h ->
                val emotionColors = listOf(Color(0xFFFFEB3B), Color(0xFF2196F3), Color(0xFFF44336), Color(0xFF4CAF50), Color(0xFF9C27B0))
                for (i in 0 until 5) { val x = w * 0.15f + i * w * 0.15f; drawCircle(emotionColors[i].copy(alpha = 0.25f), w * 0.04f, Offset(x, h * 0.15f)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFEB3B), badgeInk = Color(0xFF1565C0),
            badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFBBDEFB),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFFEB3B),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0D0E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFFEB3B).copy(alpha = 0.65f)
        )
    }

    // ══ MUSIC ══
    if (family == CategoryFamily.MUSIC) {
        if (t.contains("BEATLES")) return SignatureDesign(bg = Color(0xFF2A0A3E), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 6) { drawCircle(Color(0xFFFF6D00).copy(alpha = 0.21f), w * 0.05f + i * w * 0.015f, Offset(w * 0.20f + i * w * 0.09f, h * 0.15f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6D00), badgeInk = Color(0xFF2A0A3E), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFB080), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFFF6D00), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C0E0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF6D00).copy(alpha = 0.65f))
        if (t.contains("TAYLOR SWIFT")) return SignatureDesign(bg = Color(0xFFFFF0F5), cornerRadius = 8f, drawBackground = { w, h -> for (i in 0 until 12) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h; drawStar(x, y, 3f, 1.5f, Color(0xFFE91E63).copy(alpha = 0.35f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFE91E63), badgeInk = Color.White, badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF880E4F), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE91E63), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF6A3040).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE91E63).copy(alpha = 0.55f))
        if (t.contains("MICHAEL JACKSON")) return SignatureDesign(bg = Color(0xFF0A0A0A), cornerRadius = 6f, drawBackground = { w, h -> val path = Path().apply { moveTo(w * 0.50f, 0f); lineTo(w * 0.30f, h * 0.60f); lineTo(w * 0.70f, h * 0.60f); close() }; drawPath(path, Color(0xFFFFD700).copy(alpha = 0.21f)); for (i in 0 until 6) { val x = w * 0.42f + ((i * 3571) % 100) / 100f * w * 0.16f; val y = h * 0.05f + ((i * 4201) % 100) / 100f * h * 0.25f; drawCircle(Color(0xFFFFD700).copy(alpha = 0.35f), 1.5f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFD700), badgeInk = Color(0xFF0A0A0A), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFFFD700), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC0A030), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0D0D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFFD700).copy(alpha = 0.65f))
        if (t.contains("PINK FLOYD")) return SignatureDesign(bg = Color(0xFF0A0A2E), cornerRadius = 6f, drawBackground = { w, h -> val cx = w * 0.75f; val cy = h * 0.20f; val s2 = w * 0.07f; val path = Path().apply { moveTo(cx, cy - s2); lineTo(cx + s2, cy + s2); lineTo(cx - s2, cy + s2); close() }; drawPath(path, Color.White.copy(alpha = 0.28f), style = Stroke(1.5f)); val colors = listOf(Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF0088FF), Color(0xFF8800FF)); colors.forEachIndexed { i, c -> drawLine(c.copy(alpha = 0.25f), Offset(cx + s2, cy), Offset(cx + s2 + w * 0.12f, cy - s2 * 0.5f + i * s2 * 0.17f), strokeWidth = 1f) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF7B1FA2), badgeInk = Color.White, badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFE0D0F0), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF7B1FA2), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFC0B0D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF7B1FA2).copy(alpha = 0.55f))
        if (t.contains("NIRVANA")) return SignatureDesign(bg = Color(0xFF0A0A0A), cornerRadius = 4f, drawBackground = { w, h -> val s = (w * 1000 + h).toInt(); for (i in 0 until 60) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.14f), 1.5f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFEB3B), badgeInk = Color(0xFF0A0A0A), badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFFFEB3B), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC0B020), bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFD0D0B0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFC0B020).copy(alpha = 0.65f))
        if (t.contains("DAVID BOWIE")) return SignatureDesign(bg = Color(0xFF1565C0), cornerRadius = 6f, drawBackground = { w, h -> val path = Path().apply { moveTo(w * 0.75f, h * 0.04f); lineTo(w * 0.70f, h * 0.28f); lineTo(w * 0.78f, h * 0.24f); lineTo(w * 0.72f, h * 0.50f) }; drawPath(path, Color(0xFFD32F2F).copy(alpha = 0.30f), style = Stroke(2.5f)); drawStar(w * 0.20f, h * 0.10f, 3f, 1.5f, Color(0xFFD32F2F).copy(alpha = 0.35f)) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFD32F2F), badgeInk = Color.White, badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE0F0FF), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFD32F2F), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0D8F0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFD32F2F).copy(alpha = 0.65f))
        if (t.contains("ELVIS")) return SignatureDesign(bg = Color(0xFFFFD700), cornerRadius = 8f, drawBackground = { w, h -> for (i in 0 until 3) { val x = w * 0.10f + i * w * 0.30f; val path = Path().apply { moveTo(x, h * 0.05f); lineTo(x - 4f, h * 0.18f); lineTo(x + 4f, h * 0.16f); lineTo(x - 2f, h * 0.30f) }; drawPath(path, Color(0xFF0A0A0A).copy(alpha = 0.21f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF0A0A0A), badgeInk = Color(0xFFFFD700), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2810), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF0A0A0A), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A3820).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF0A0A0A).copy(alpha = 0.65f))
    }

    // ══ ANIME ══
    if (family == CategoryFamily.ANIME_COMICS) {
        if (t.contains("NARUTO")) return SignatureDesign(bg = Color(0xFFFF6D00), cornerRadius = 4f, drawBackground = { w, h -> for (i in 0 until 10) { val angle = i * 36f; val rad = Math.toRadians(angle.toDouble()).toFloat(); val r = w * 0.015f + i * w * 0.004f; drawCircle(Color(0xFF1565C0).copy(alpha = 0.35f), 1.5f, Offset(w * 0.80f + kotlin.math.cos(rad) * r, h * 0.15f + kotlin.math.sin(rad) * r)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF1565C0), badgeInk = Color.White, badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF3A2010), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1565C0), bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFF5A3010).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1565C0).copy(alpha = 0.65f))
        if (t.contains("DRAGON BALL")) return SignatureDesign(bg = Color(0xFFFF6D00), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 7) { val x = w * 0.15f + (i % 4) * w * 0.16f; val y = h * 0.08f + (i / 4) * h * 0.10f; drawCircle(Color(0xFFFFD700).copy(alpha = 0.30f), w * 0.025f, Offset(x, y)); drawStar(x, y, 2f, 1f, Color(0xFFFFD700).copy(alpha = 0.28f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF1565C0), badgeInk = Color.White, badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2010), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1565C0), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF5A3010).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1565C0).copy(alpha = 0.65f))
        if (t.contains("ONE PIECE")) return SignatureDesign(bg = Color(0xFFC62828), cornerRadius = 4f, drawBackground = { w, h -> for (i in 0 until 3) { val path = Path().apply { moveTo(0f, h * 0.80f + i * h * 0.04f); for (j in 0..15) { lineTo(j * w / 15f, h * 0.80f + i * h * 0.04f + kotlin.math.sin(j * 0.8f + i).toFloat() * h * 0.02f) } }; drawPath(path, Color(0xFF1565C0).copy(alpha = 0.28f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF1565C0), badgeInk = Color.White, badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF0E0), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1565C0), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0C0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1565C0).copy(alpha = 0.65f))
        if (t.contains("DEMON SLAYER")) return SignatureDesign(bg = Color(0xFF1A0A0A), cornerRadius = 4f, drawBackground = { w, h -> drawArc(Color(0xFFFF3D00).copy(alpha = 0.25f), 200f, 160f, false, Offset(w * 0.55f, h * 0.02f), Size(w * 0.35f, h * 0.28f), style = Stroke(1.5f)); val path = Path().apply { moveTo(w * 0.05f, h * 0.85f); for (i in 0..12) { lineTo(w * 0.05f + i * w * 0.07f, h * 0.85f + kotlin.math.sin(i * 0.8f).toFloat() * h * 0.025f) } }; drawPath(path, Color(0xFF00897B).copy(alpha = 0.35f), style = Stroke(1.5f)) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF3D00), badgeInk = Color.White, badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF0E0), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFFF3D00), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0C0B0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF3D00).copy(alpha = 0.65f))
        if (t.contains("SAILOR MOON")) return SignatureDesign(bg = Color(0xFF0D1B3E), cornerRadius = 8f, drawBackground = { w, h -> drawCircle(Color(0xFFF48FB1).copy(alpha = 0.30f), w * 0.07f, Offset(w * 0.80f, h * 0.12f)); drawCircle(Color(0xFF0D1B3E).copy(alpha = 0.35f), w * 0.07f, Offset(w * 0.83f, h * 0.10f)); for (i in 0 until 10) { val x = ((i * 7919) % 10000) / 10000f * w * 0.85f + w * 0.05f; val y = ((i * 6271) % 10000) / 10000f * h * 0.28f; drawStar(x, y, 2f, 1f, Color(0xFFFFD700).copy(alpha = 0.28f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFF48FB1), badgeInk = Color(0xFF0D1B3E), badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF48FB1), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC08090), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD0C0D8).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFF48FB1).copy(alpha = 0.55f))
    }

    // ══ FOOD ══
    if (family == CategoryFamily.FOOD) {
        if (t.contains("PIZZA")) return SignatureDesign(bg = Color(0xFFC62828), cornerRadius = 8f, drawBackground = { w, h -> drawArc(Color(0xFFFFEB3B).copy(alpha = 0.20f), 0f, 180f, false, Offset(w * 0.30f, h * 0.05f), Size(w * 0.40f, h * 0.35f)); for (i in 0 until 5) { val x = w * 0.35f + i * w * 0.06f; drawCircle(Color(0xFFD32F2F).copy(alpha = 0.25f), 3f, Offset(x, h * 0.18f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFEB3B), badgeInk = Color(0xFFC62828), badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFEB3B), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFFEB3B), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0C0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFFEB3B).copy(alpha = 0.65f))
        if (t.contains("SUSHI")) return SignatureDesign(bg = Color(0xFFFAFAFA), cornerRadius = 8f, drawBackground = { w, h -> for (i in 0 until 3) { val x = w * 0.60f + i * w * 0.08f; drawOval(Color(0xFFD32F2F).copy(alpha = 0.20f), Offset(x, h * 0.12f), Size(w * 0.06f, h * 0.04f)) }; val wavePath = Path().apply { moveTo(0f, h * 0.85f); for (i in 0..20) { lineTo(i * w / 20f, h * 0.85f + kotlin.math.sin(i * 0.6f).toFloat() * h * 0.02f) } }; drawPath(wavePath, Color(0xFF1565C0).copy(alpha = 0.15f), style = Stroke(1f)) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFD32F2F), badgeInk = Color.White, badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF333333), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFD32F2F), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A4A4A).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFD32F2F).copy(alpha = 0.65f))
        if (t.contains("COFFEE")) return SignatureDesign(bg = Color(0xFF2E1A0E), cornerRadius = 8f, drawBackground = { w, h -> for (i in 0 until 3) { val sx = w * 0.40f + i * w * 0.10f; val steamPath = Path().apply { moveTo(sx, h * 0.06f); cubicTo(sx + 4f, h * 0.03f, sx - 4f, h * 0.01f, sx + 2f, h * -0.02f) }; drawPath(steamPath, Color(0xFFD4AF37).copy(alpha = 0.20f), style = Stroke(1f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFD4AF37), badgeInk = Color(0xFF2E1A0E), badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD4AF37), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFD4AF37), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE8D8C0).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD4AF37).copy(alpha = 0.65f))
        if (t.contains("CHOCOLATE")) return SignatureDesign(bg = Color(0xFF3E2723), cornerRadius = 8f, drawBackground = { w, h -> for (i in 0 until 4) { val x = w * 0.65f + i * w * 0.05f; drawOval(Color(0xFFD4AF37).copy(alpha = 0.15f), Offset(x, h * 0.10f), Size(w * 0.04f, h * 0.06f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFD4AF37), badgeInk = Color(0xFF3E2723), badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD4AF37), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFD4AF37), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0C0).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD4AF37).copy(alpha = 0.65f))
        if (t.contains("WINE")) return SignatureDesign(bg = Color(0xFF4A0A0A), cornerRadius = 8f, drawBackground = { w, h -> for (i in 0 until 5) { val x = w * 0.15f + i * w * 0.15f; drawCircle(Color(0xFF7B1FA2).copy(alpha = 0.15f), 3f, Offset(x, h * 0.12f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A959), badgeInk = Color(0xFF4A0A0A), badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFC9A959), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC9A959), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C0C0).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A959).copy(alpha = 0.65f))
    }

    // ══ HISTORY ══
    if (family == CategoryFamily.BOOKS) {
        if (t.contains("EGYPT") || t.contains("PYRAMID")) return SignatureDesign(bg = Color(0xFFF0E0C0), cornerRadius = 8f, drawBackground = { w, h -> val path = Path().apply { moveTo(w * 0.60f, h * 0.08f); lineTo(w * 0.50f, h * 0.35f); lineTo(w * 0.70f, h * 0.35f); close() }; drawPath(path, Color(0xFFC9A959).copy(alpha = 0.20f)); drawPath(path, Color(0xFFC9A959).copy(alpha = 0.20f), style = Stroke(1f)); for (i in 0 until 15) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h * 0.3f; drawCircle(Color(0xFFC9A959).copy(alpha = 0.15f), 1f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A959), badgeInk = Color(0xFF3A2810), badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF5A4020), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9A959), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF6A5030).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A959).copy(alpha = 0.65f))
        // FIX: use "ROME " with context to avoid "Romeo" false positive
        if (t.contains("COLISEUM") || t.contains("ROME ") || t.startsWith("ROME")) return SignatureDesign(bg = Color(0xFFF0EDE8), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 3) { val cx = w * 0.20f + i * w * 0.30f; drawLine(Color(0xFF8B0000).copy(alpha = 0.15f), Offset(cx, h * 0.08f), Offset(cx, h * 0.85f), strokeWidth = 2f) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF8B0000), badgeInk = Color.White, badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2020), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8B0000), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF5A3030).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8B0000).copy(alpha = 0.65f))
        if (t.contains("SAMURAI")) return SignatureDesign(bg = Color(0xFF1A0A0A), cornerRadius = 4f, drawBackground = { w, h -> drawLine(Color(0xFFC62828).copy(alpha = 0.30f), Offset(w * 0.75f, h * 0.05f), Offset(w * 0.75f, h * 0.90f), strokeWidth = 1.5f); for (i in 0 until 5) { val x = w * 0.10f + i * w * 0.04f; drawCircle(Color(0xFFFF80AB).copy(alpha = 0.15f), 3f, Offset(x, h * 0.10f + i * h * 0.03f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC62828), badgeInk = Color.White, badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFCDD2), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC62828), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0C0C0).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFC62828).copy(alpha = 0.65f))
        if (t.contains("VIKING")) return SignatureDesign(bg = Color(0xFF1A1A2E), cornerRadius = 4f, drawBackground = { w, h -> for (i in 0 until 8) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h; drawCircle(Color(0xFFB0BEC5).copy(alpha = 0.18f), 1.5f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFB0BEC5), badgeInk = Color(0xFF1A1A2E), badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFB0BEC5), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFB0BEC5), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0C8D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFB0BEC5).copy(alpha = 0.65f))
    }

    // ══ SPORTS ══
    if (family == CategoryFamily.SPORTS) {
        if (t.contains("OLYMPICS")) return SignatureDesign(bg = Color(0xFF0D1B3E), cornerRadius = 6f, drawBackground = { w, h -> val ringColors = listOf(Color(0xFF0088FF), Color(0xFFFFEB3B), Color(0xFF000000), Color(0xFF00C853), Color(0xFFF44336)); for (i in 0 until 5) { drawCircle(ringColors[i].copy(alpha = 0.25f), w * 0.03f, Offset(w * 0.30f + i * w * 0.08f, h * 0.15f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFD700), badgeInk = Color(0xFF0D1B3E), badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFD700), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFFD700), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0C8E0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFFD700).copy(alpha = 0.65f))
        if (t.contains("CRICKET")) return SignatureDesign(bg = Color(0xFFF5F5F0), cornerRadius = 6f, drawBackground = { w, h -> drawLine(Color(0xFFC62828).copy(alpha = 0.20f), Offset(w * 0.50f, h * 0.05f), Offset(w * 0.50f, h * 0.95f), strokeWidth = 1f); drawCircle(Color(0xFFC62828).copy(alpha = 0.15f), w * 0.03f, Offset(w * 0.65f, h * 0.30f)) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC62828), badgeInk = Color.White, badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF333333), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC62828), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A4A4A).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFC62828).copy(alpha = 0.65f))
    }

    // ══ INTERNET & TECH ══
    if (family == CategoryFamily.INTERNET) {
        if (t.contains("BITCOIN")) return SignatureDesign(bg = Color(0xFFF7931A), cornerRadius = 6f, drawBackground = { w, h -> val s = (w * 1000 + h).toInt(); for (i in 0 until 20) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color(0xFF0A0A0A).copy(alpha = 0.10f), 1f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF0A0A0A), badgeInk = Color(0xFFF7931A), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2810), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF0A0A0A), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A3820).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF0A0A0A).copy(alpha = 0.65f))
        if (t.contains("SPACEX") || t.contains("NASA")) return SignatureDesign(bg = Color(0xFF0A0A14), cornerRadius = 4f, drawBackground = { w, h -> val s = (w * 1000 + h).toInt(); for (i in 0 until 50) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.20f), 1f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF42A5F5), badgeInk = Color(0xFF0A0A14), badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF90CAF9), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF42A5F5), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0C0D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF42A5F5).copy(alpha = 0.65f))
    }

    // ══ WILDCARD ══
    if (family == CategoryFamily.WILDCARD) {
        if (t.contains("PHILOSOPHY")) return SignatureDesign(bg = Color(0xFF1A1A2E), cornerRadius = 6f, drawBackground = { w, h -> drawLine(Color(0xFFC9A959).copy(alpha = 0.20f), Offset(w * 0.12f, h * 0.08f), Offset(w * 0.12f, h * 0.85f), strokeWidth = 2f) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A959), badgeInk = Color(0xFF1A1A2E), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFC9A959), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9A959), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD0C8A0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A959).copy(alpha = 0.55f))
        if (t.contains("PSYCHOLOGY")) return SignatureDesign(bg = Color(0xFF1A0A2E), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 8) { val angle = i * 45f; val rad = Math.toRadians(angle.toDouble()).toFloat(); drawLine(Color(0xFFE91E63).copy(alpha = 0.15f), Offset(w * 0.80f, h * 0.15f), Offset(w * 0.80f + kotlin.math.cos(rad) * w * 0.08f, h * 0.15f + kotlin.math.sin(rad) * h * 0.08f), strokeWidth = 0.8f) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFE91E63), badgeInk = Color.White, badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF8BBD0), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE91E63), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD0C0D8).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE91E63).copy(alpha = 0.55f))
        if (t.contains("OCEAN") || t.contains("SEA")) return SignatureDesign(bg = Color(0xFF0A1428), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 3) { val wavePath = Path().apply { moveTo(0f, h * 0.70f + i * h * 0.06f); for (j in 0..20) { lineTo(j * w / 20f, h * 0.70f + i * h * 0.06f + kotlin.math.sin(j * 0.5f + i).toFloat() * h * 0.02f) } }; drawPath(wavePath, Color(0xFF00BCD4).copy(alpha = 0.20f - i * 0.05f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF00BCD4), badgeInk = Color(0xFF0A1428), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF80DEEA), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF00BCD4), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0D0E0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF00BCD4).copy(alpha = 0.65f))
        if (t.contains("SPACE")) return SignatureDesign(bg = Color(0xFF0A0A14), cornerRadius = 6f, drawBackground = { w, h -> val s = (w * 1000 + h).toInt(); for (i in 0 until 60) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.20f), 1f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF42A5F5), badgeInk = Color(0xFF0A0A14), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF90CAF9), metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF42A5F5), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0C0D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF42A5F5).copy(alpha = 0.65f))
    }

    return null
}

// ═══════════════════════════════════════════════════════════════════════
// CLASSIC SIGNATURE DESIGNS — the pre-detailed family-based designs
// (restored from the f6dd7f19 signature redesign). Selectable per card
// via the share sheet "Design" picker (Current / Classic).
// ═══════════════════════════════════════════════════════════════════════
private fun signatureDesignClassic(categoryName: String, family: CategoryFamily): SignatureDesign {
    val cat = categoryName.uppercase().trim()
    return when {
        // ═══ MUSIC — vinyl grooves on dark amber ═══
        family == CategoryFamily.MUSIC || cat.contains("MUSIC") || cat.contains("ALBUM") || cat.contains("SONG") || cat.contains("ARTIST") -> SignatureDesign(
            bg = Color(0xFF1A1208), cornerRadius = 6f,
            drawBackground = { w, h ->
                for (i in 0 until 24) { drawCircle(Color(0xFFB08840).copy(alpha = 0.35f), w * 0.08f + i * w * 0.035f, Offset(w * 0.76f, h * 0.70f), style = Stroke(1.5f)) }
                drawCircle(Color(0xFFB08840).copy(alpha = 0.25f), w * 0.08f, Offset(w * 0.76f, h * 0.70f))
                drawCircle(Color(0xFF1A1208).copy(alpha = 0.50f), w * 0.025f, Offset(w * 0.76f, h * 0.70f))
                drawCircle(Color(0xFFB08840).copy(alpha = 0.28f), w * 0.5f, Offset(w * 0.3f, h * 0.4f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFB08840), badgeInk = Color(0xFF1A1208),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF5E6D0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFB08840),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE8DCC8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFB08840).copy(alpha = 0.70f)
        )
        // ═══ MOVIES — dark cinematic with film grain ═══
        family == CategoryFamily.MOVIES || cat.contains("FILM") || cat.contains("MOVIE") || cat.contains("SERIES") || cat.contains("DIRECTOR") || cat.contains("ANIMATED") -> SignatureDesign(
            bg = Color(0xFF0D0D12), cornerRadius = 6f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 120) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color.White.copy(alpha = 0.025f), 1.2f, Offset(x, y))
                }
                drawCircle(Color(0xFF6B1A1A).copy(alpha = 0.15f), w * 0.35f, Offset(w * 0.9f, -h * 0.05f))
                for (i in 0 until 8) {
                    val y = h * 0.05f + i * h * 0.12f
                    drawRoundRect(Color.White.copy(alpha = 0.04f), Offset(w * 0.02f, y), Size(w * 0.025f, h * 0.06f), CornerRadius(2f))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8B1A1A), badgeInk = Color.White,
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp,
            badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp,
            titleTopSpacer = 16.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp,
            titleLineHeight = 34.sp, titleColor = Color(0xFFF0F0F0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8B1A1A),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0D0D0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8B1A1A).copy(alpha = 0.70f)
        )
        // ═══ BOOKS — warm library manuscript with leather spine ═══
        family == CategoryFamily.BOOKS || cat.contains("BOOK") || cat.contains("AUTHOR") || cat.contains("HISTORY") || cat.contains("LANGUAGE") || cat.contains("ECONOM") -> SignatureDesign(
            bg = Color(0xFFF5EDE0), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Leather book spine on left edge
                drawRect(Color(0xFF5C3317).copy(alpha = 0.45f), Offset(0f, 0f), Size(w * 0.07f, h))
                drawRect(Color(0xFF7A4B2A).copy(alpha = 0.30f), Offset(w * 0.07f, 0f), Size(w * 0.015f, h))
                // Gold spine bands
                for (i in 0 until 5) {
                    val y = h * 0.12f + i * h * 0.18f
                    drawRect(Color(0xFFC49A3C).copy(alpha = 0.55f), Offset(w * 0.01f, y), Size(w * 0.05f, h * 0.008f))
                    drawRect(Color(0xFFC49A3C).copy(alpha = 0.35f), Offset(w * 0.01f, y + h * 0.025f), Size(w * 0.05f, h * 0.004f))
                }
                // Ruled notebook lines
                for (i in 0 until 20) {
                    val y = h * 0.06f + i * h * 0.045f
                    drawLine(Color(0xFFBFA882).copy(alpha = 0.30f), Offset(w * 0.10f, y), Offset(w * 0.92f, y), strokeWidth = 0.5f)
                }
                // Red margin line
                drawLine(Color(0xFFCC4444).copy(alpha = 0.30f), Offset(w * 0.14f, h * 0.04f), Offset(w * 0.14f, h * 0.96f), strokeWidth = 1.2f)
                // Page corner curl bottom-right
                val cornerPath = Path().apply {
                    moveTo(w * 0.85f, h * 0.88f)
                    quadraticBezierTo(w * 0.92f, h * 0.90f, w * 0.94f, h * 0.96f)
                    lineTo(w * 0.88f, h * 0.96f)
                    quadraticBezierTo(w * 0.86f, h * 0.93f, w * 0.85f, h * 0.88f)
                }
                drawPath(cornerPath, Color(0xFFD4C4A8).copy(alpha = 0.30f))
                // Gold leaf ornament top-right
                drawCircle(Color(0xFFC49A3C).copy(alpha = 0.18f), w * 0.06f, Offset(w * 0.88f, h * 0.08f), style = Stroke(1.5f))
                drawCircle(Color(0xFFC49A3C).copy(alpha = 0.12f), w * 0.04f, Offset(w * 0.88f, h * 0.08f))
            },
            padding = PaddingValues(horizontal = 26.dp, vertical = 22.dp), badgeColor = Color(0xFF5C3317), badgeInk = Color(0xFFF5EDE0),
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF3A2814),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF5C3317).copy(alpha = 0.65f),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF4A3824).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF5C3317).copy(alpha = 0.55f)
        )
        // ═══ ASTRONOMY — deep cosmic with nebula, planets, constellations ═══
        cat.contains("ASTRONOMY") || cat.contains("SPACE") || cat.contains("STAR") || cat.contains("PLANET") || cat.contains("COSMOS") -> SignatureDesign(
            bg = Color(0xFF050510), cornerRadius = 6f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                // Dense starfield with varying sizes
                for (i in 0 until 120) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    val r = 0.3f + ((s * (i+1) * 3571) % 100) / 100f * 2.8f
                    val a = 0.10f + ((s * (i+1) * 4201) % 100) / 100f * 0.30f
                    drawCircle(Color.White.copy(alpha = a), r, Offset(x, y))
                }
                // Large ringed planet top-right
                drawCircle(Color(0xFF2A4A7A).copy(alpha = 0.40f), w * 0.14f, Offset(w * 0.80f, h * 0.15f))
                drawOval(Color(0xFF8AAAE0).copy(alpha = 0.30f), Offset(w * 0.62f, h * 0.10f), Size(w * 0.36f, h * 0.035f), style = Stroke(1.8f))
                // Small moon
                drawCircle(Color(0xFFD4C8A0).copy(alpha = 0.35f), w * 0.035f, Offset(w * 0.55f, h * 0.22f))
                // Nebula clouds - layered translucent
                drawCircle(Color(0xFF4A2A8A).copy(alpha = 0.25f), w * 0.30f, Offset(w * 0.20f, h * 0.70f))
                drawCircle(Color(0xFF2A4A6A).copy(alpha = 0.30f), w * 0.25f, Offset(w * 0.80f, h * 0.50f))
                drawCircle(Color(0xFF6A2A5A).copy(alpha = 0.18f), w * 0.20f, Offset(w * 0.50f, h * 0.40f))
                // Constellation lines
                for (i in 0 until 5) {
                    val x1 = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y1 = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    val x2 = ((s * (i+5) * 7919) % 10000) / 10000f * w
                    val y2 = ((s * (i+5) * 6271) % 10000) / 10000f * h
                    drawLine(Color(0xFF8A9AC0).copy(alpha = 0.20f), Offset(x1, y1), Offset(x2, y2), strokeWidth = 0.6f)
                }
                // Bright star with cross flare
                drawCircle(Color.White.copy(alpha = 0.30f), 3f, Offset(w * 0.40f, h * 0.12f))
                drawLine(Color.White.copy(alpha = 0.25f), Offset(w * 0.40f - 10f, h * 0.12f), Offset(w * 0.40f + 10f, h * 0.12f), strokeWidth = 0.5f)
                drawLine(Color.White.copy(alpha = 0.25f), Offset(w * 0.40f, h * 0.12f - 10f), Offset(w * 0.40f, h * 0.12f + 10f), strokeWidth = 0.5f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4A6A9A), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD0D8F0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF6A7AAA),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0B8D0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF6A7AAA).copy(alpha = 0.65f)
        )
        // ═══ BIOLOGY — organic with DNA helix, cells, leaf veins ═══
        cat.contains("BIOLOGY") || cat.contains("LIFE") || cat.contains("ANIMAL") || cat.contains("PLANT") || cat.contains("NATURE") -> SignatureDesign(
            bg = Color(0xFFF0F8F0), cornerRadius = 8f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                // DNA double helix - right side
                for (i in 0 until 30) {
                    val t = i / 30f
                    val x1 = w * 0.82f + kotlin.math.sin(t * 6.28f * 2).toFloat() * w * 0.06f
                    val x2 = w * 0.82f - kotlin.math.sin(t * 6.28f * 2).toFloat() * w * 0.06f
                    val y = h * 0.05f + t * h * 0.90f
                    drawCircle(Color(0xFF2E7D32).copy(alpha = 0.35f), 2.5f, Offset(x1, y))
                    drawCircle(Color(0xFF388E3C).copy(alpha = 0.30f), 2.0f, Offset(x2, y))
                    // Cross-links every few steps
                    if (i % 3 == 0) drawLine(Color(0xFF4CAF50).copy(alpha = 0.18f), Offset(x1, y), Offset(x2, y), strokeWidth = 0.6f)
                }
                // Cell membrane circles - left side
                for (i in 0 until 6) {
                    val cx = w * 0.15f + ((s * (i+1) * 3571) % 100) / 100f * w * 0.25f
                    val cy = h * 0.10f + ((s * (i+1) * 4201) % 100) / 100f * h * 0.35f
                    val r = w * 0.04f + ((s * (i+1) * 7727) % 100) / 100f * w * 0.05f
                    drawCircle(Color(0xFF2E7D32).copy(alpha = 0.20f), r, Offset(cx, cy), style = Stroke(1.8f))
                    drawCircle(Color(0xFF388E3C).copy(alpha = 0.15f), r * 0.4f, Offset(cx, cy))
                }
                // Leaf vein pattern bottom
                val veinPath = Path().apply {
                    moveTo(w * 0.05f, h * 0.85f)
                    quadraticBezierTo(w * 0.30f, h * 0.78f, w * 0.55f, h * 0.85f)
                }
                drawPath(veinPath, Color(0xFF2E7D32).copy(alpha = 0.20f), style = Stroke(1.5f))
                // Small veins branching
                for (i in 0 until 5) {
                    val bx = w * 0.10f + i * w * 0.09f
                    drawLine(Color(0xFF4CAF50).copy(alpha = 0.15f), Offset(bx, h * 0.85f), Offset(bx + w * 0.03f, h * 0.80f), strokeWidth = 0.6f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF2E7D32), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF1B3A1B),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF2E7D32),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF2A4A2A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF2E7D32).copy(alpha = 0.65f)
        )
        // ═══ CHEMISTRY — hexagonal molecular grid + flask ═══
        cat.contains("CHEMISTRY") || cat.contains("CHEM") -> SignatureDesign(
            bg = Color(0xFFF0F4FF), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Hexagonal benzene ring grid
                val hexR = w * 0.055f
                for (row in 0 until 9) {
                    for (col in 0 until 7) {
                        val x = col * hexR * 1.8f + (row % 2) * hexR * 0.9f + w * 0.06f
                        val y = row * hexR * 1.55f + h * 0.06f
                        if (x < w * 0.90f && y < h * 0.90f) {
                            val path = Path()
                            for (k in 0 until 6) {
                                val angle = Math.toRadians((60.0 * k - 30.0))
                                val px = x + hexR * 0.8f * kotlin.math.cos(angle).toFloat()
                                val py = y + hexR * 0.8f * kotlin.math.sin(angle).toFloat()
                                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            path.close()
                            drawPath(path, Color(0xFF1A5276).copy(alpha = 0.18f), style = Stroke(0.8f))
                            // Molecule node at center
                            if ((row + col) % 3 == 0) drawCircle(Color(0xFF1A5276).copy(alpha = 0.25f), 2.5f, Offset(x, y))
                        }
                    }
                }
                // Flask silhouette bottom-right
                val flaskPath = Path().apply {
                    moveTo(w * 0.82f, h * 0.72f)
                    lineTo(w * 0.78f, h * 0.82f)
                    quadraticBezierTo(w * 0.75f, h * 0.92f, w * 0.82f, h * 0.94f)
                    quadraticBezierTo(w * 0.92f, h * 0.92f, w * 0.88f, h * 0.82f)
                    lineTo(w * 0.84f, h * 0.72f)
                    close()
                }
                drawPath(flaskPath, Color(0xFF1A5276).copy(alpha = 0.12f), style = Stroke(1.2f))
                // Bubbles rising from flask
                for (i in 0 until 4) {
                    val bx = w * 0.82f + ((i * 3571) % 100) / 100f * w * 0.04f - w * 0.02f
                    val by = h * 0.70f - i * h * 0.04f
                    drawCircle(Color(0xFF1A5276).copy(alpha = 0.15f - i * 0.03f), 2f + i * 0.3f, Offset(bx, by))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF1A5276), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF1A2A3A),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1A5276),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF2A3A4A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1A5276).copy(alpha = 0.65f)
        )
        // ═══ SCIENCE — measurement grid + atom model + spectrum ═══
        family == CategoryFamily.SCIENCE || cat.contains("SCIENCE") || cat.contains("PHYSICS") || cat.contains("MEDICINE") || cat.contains("PSYCHOLOGY") || cat.contains("MATHEMAT") || cat.contains("ENGINEER") || cat.contains("TECHNOLOG") || cat.contains("GEOLOG") || cat.contains("OCEAN") -> SignatureDesign(
            bg = Color(0xFFF0F4F8), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Fine measurement grid
                for (i in 0 until 24) { drawLine(Color(0xFFB0C4D8).copy(alpha = 0.20f), Offset(w * 0.04f + i * w * 0.04f, 0f), Offset(w * 0.04f + i * w * 0.04f, h), strokeWidth = 0.3f) }
                for (i in 0 until 20) { drawLine(Color(0xFFB0C4D8).copy(alpha = 0.20f), Offset(0f, h * 0.04f + i * h * 0.048f), Offset(w, h * 0.04f + i * h * 0.048f), strokeWidth = 0.3f) }
                // Atom model - three orbital ellipses
                val atomCx = w * 0.75f; val atomCy = h * 0.20f
                drawOval(Color(0xFF1A5276).copy(alpha = 0.22f), Offset(atomCx - w * 0.12f, atomCy - h * 0.06f), Size(w * 0.24f, h * 0.12f), style = Stroke(0.8f))
                drawOval(Color(0xFF1A5276).copy(alpha = 0.18f), Offset(atomCx - w * 0.10f, atomCy - h * 0.05f), Size(w * 0.20f, h * 0.10f), style = Stroke(0.8f))
                drawOval(Color(0xFF1A5276).copy(alpha = 0.15f), Offset(atomCx - w * 0.08f, atomCy - h * 0.04f), Size(w * 0.16f, h * 0.08f), style = Stroke(0.8f))
                drawCircle(Color(0xFF1A5276).copy(alpha = 0.30f), 3.5f, Offset(atomCx, atomCy))
                // Prismatic spectrum line bottom-left
                val specColors = listOf(Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFDD00), Color(0xFF00CC44), Color(0xFF0088FF), Color(0xFF6600CC))
                for (i in 0 until 6) {
                    drawLine(specColors[i].copy(alpha = 0.22f), Offset(w * 0.08f + i * w * 0.05f, h * 0.88f), Offset(w * 0.08f + i * w * 0.05f + w * 0.04f, h * 0.92f), strokeWidth = 2f)
                }
                // Scatter data points
                drawCircle(Color(0xFF1A5276).copy(alpha = 0.25f), 3f, Offset(w * 0.20f, h * 0.60f))
                drawCircle(Color(0xFF1A5276).copy(alpha = 0.25f), 3f, Offset(w * 0.30f, h * 0.52f))
                drawCircle(Color(0xFF1A5276).copy(alpha = 0.25f), 3f, Offset(w * 0.40f, h * 0.44f))
                drawCircle(Color(0xFF1A5276).copy(alpha = 0.25f), 3f, Offset(w * 0.50f, h * 0.38f))
                drawLine(Color(0xFF1A5276).copy(alpha = 0.20f), Offset(w * 0.15f, h * 0.65f), Offset(w * 0.55f, h * 0.34f), strokeWidth = 0.8f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF1A5276), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF1A2A3A),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1A5276),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF2A3A4A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1A5276).copy(alpha = 0.65f)
        )
        // ═══ ANIME/COMICS — bold manga panels + speed lines + halftone ═══
        family == CategoryFamily.ANIME_COMICS || cat.contains("ANIME") || cat.contains("MANGA") || cat.contains("MANHWA") -> SignatureDesign(
            bg = Color(0xFFF0E8FF), cornerRadius = 4f,
            drawBackground = { w, h ->
                // Bold speed lines radiating from top-right corner
                for (i in 0 until 25) {
                    val angle = -55f + i * 5f
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val thick = if (i % 4 == 0) 2.5f else 1.0f
                    val a = if (i % 4 == 0) 0.14f else 0.06f
                    drawLine(Color(0xFF7D3C98).copy(alpha = a), Offset(w * 0.96f, h * 0.02f),
                        Offset(w * 0.96f + kotlin.math.cos(rad) * w * 0.95f, h * 0.02f + kotlin.math.sin(rad) * h * 0.95f), strokeWidth = thick)
                }
                // Manga panel border bottom-right
                drawRoundRect(Color(0xFF7D3C98).copy(alpha = 0.25f), Offset(w * 0.55f, h * 0.65f), Size(w * 0.40f, h * 0.30f), CornerRadius(3f), style = Stroke(2f))
                // Halftone dots gradient — dense bottom-left, fading to sparse
                for (i in 0 until 12) {
                    for (j in 0 until 14) {
                        val x = w * 0.02f + i * w * 0.032f
                        val y = h * 0.58f + j * h * 0.022f
                        val dotR = 2.0f - j * 0.12f
                        if (dotR > 0.4f) drawCircle(Color(0xFF7D3C98).copy(alpha = 0.25f), dotR, Offset(x, y))
                    }
                }
                // Action burst — star shape top-left
                val burstCx = w * 0.12f; val burstCy = h * 0.12f
                for (i in 0 until 8) {
                    val angle = i * 45f
                    val rad2 = Math.toRadians(angle.toDouble()).toFloat()
                    drawLine(Color(0xFF9C27B0).copy(alpha = 0.18f), Offset(burstCx, burstCy),
                        Offset(burstCx + kotlin.math.cos(rad2) * w * 0.08f, burstCy + kotlin.math.sin(rad2) * h * 0.08f), strokeWidth = 1.5f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF7D3C98), badgeInk = Color.White,
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF2C1040),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF7D3C98),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFF3A2050).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF7D3C98).copy(alpha = 0.65f)
        )
        // ═══ GAMES — dark with neon accents, pixel-grid hint ═══
        family == CategoryFamily.GAMES || cat.contains("GAME") -> SignatureDesign(
            bg = Color(0xFF0A0A14), cornerRadius = 4f,
            drawBackground = { w, h ->
                for (i in 0 until 15) {
                    val x = w * 0.04f + i * w * 0.063f
                    for (j in 0 until 22) {
                        val y = h * 0.02f + j * h * 0.044f
                        if ((i + j) % 2 == 0) drawRect(Color(0xFF00FF88).copy(alpha = 0.04f), Offset(x, y), Size(w * 0.045f, h * 0.03f))
                        else if ((i + j) % 5 == 0) drawRect(Color(0xFF00CCFF).copy(alpha = 0.03f), Offset(x, y), Size(w * 0.045f, h * 0.03f))
                    }
                }
                for (i in 0 until 40) {
                    val y = i * h / 40f
                    drawLine(Color(0xFF00FF88).copy(alpha = 0.015f), Offset(0f, y), Offset(w, y))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF00CC66), badgeInk = Color(0xFF0A0A14),
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp,
            badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp,
            titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp,
            titleLineHeight = 34.sp, titleColor = Color(0xFF00FF88),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF00CC66),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFC0D0C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF00CC66).copy(alpha = 0.65f)
        )
        // ═══ MYTHOLOGY — classical Greek columns + laurel wreath + marble ═══
        family == CategoryFamily.MYTHOLOGY || cat.contains("MYTH") || cat.contains("LEGEND") -> SignatureDesign(
            bg = Color(0xFFFAF5E8), cornerRadius = 8f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                // Marble veining
                for (i in 0 until 40) {
                    val x1 = ((s * (i+1) * 3571) % 10000) / 10000f * w
                    val y1 = ((s * (i+1) * 4201) % 10000) / 10000f * h
                    val x2 = x1 + ((s * (i+1) * 7727) % 100) / 100f * w * 0.20f
                    val y2 = y1 + ((s * (i+1) * 9113) % 100) / 100f * h * 0.12f
                    drawLine(Color(0xFFC8B898).copy(alpha = 0.25f), Offset(x1, y1), Offset(x2, y2), strokeWidth = 0.8f)
                }
                // Greek columns — two fluted pillars
                for (cx in listOf(w * 0.10f, w * 0.90f)) {
                    // Column shaft
                    drawRect(Color(0xFF8B7420).copy(alpha = 0.18f), Offset(cx - w * 0.02f, h * 0.08f), Size(w * 0.04f, h * 0.78f))
                    // Fluting lines
                    for (k in 0 until 3) {
                        drawLine(Color(0xFF8B7420).copy(alpha = 0.12f), Offset(cx - w * 0.015f + k * w * 0.012f, h * 0.10f), Offset(cx - w * 0.015f + k * w * 0.012f, h * 0.84f), strokeWidth = 0.4f)
                    }
                    // Capital (top)
                    drawRect(Color(0xFF8B7420).copy(alpha = 0.22f), Offset(cx - w * 0.03f, h * 0.06f), Size(w * 0.06f, h * 0.03f))
                    // Base
                    drawRect(Color(0xFF8B7420).copy(alpha = 0.22f), Offset(cx - w * 0.03f, h * 0.85f), Size(w * 0.06f, h * 0.02f))
                }
                // Laurel wreath bottom-center
                val wreathCx = w * 0.50f; val wreathCy = h * 0.88f; val wreathR = w * 0.06f
                for (i in 0 until 16) {
                    val angle = i * 22.5f
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val lx = wreathCx + kotlin.math.cos(rad) * wreathR
                    val ly = wreathCy + kotlin.math.sin(rad) * wreathR * 0.7f
                    drawOval(Color(0xFF6B8C2A).copy(alpha = 0.22f), Offset(lx - 2f, ly - 3f), Size(4f, 6f))
                }
                drawCircle(Color(0xFFC49A3C).copy(alpha = 0.15f), wreathR * 0.4f, Offset(wreathCx, wreathCy))
            },
            padding = PaddingValues(horizontal = 24.dp, vertical = 22.dp), badgeColor = Color(0xFF8B7420), badgeInk = Color(0xFFFAF5E8),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF3A2810),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8B7420),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF4A3818).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8B7420).copy(alpha = 0.55f)
        )
        // ═══ SPORTS — dynamic motion + field + scoreboard ═══
        family == CategoryFamily.SPORTS || cat.contains("SPORT") || cat.contains("OLYMPIC") -> SignatureDesign(
            bg = Color(0xFFE8F5E8), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Diagonal motion lines — energy
                for (i in 0 until 12) {
                    val offset = i * w * 0.08f
                    drawLine(Color(0xFF1B5E20).copy(alpha = 0.08f + i * 0.01f), Offset(offset, 0f), Offset(offset + h * 0.3f, h), strokeWidth = 1.5f)
                }
                // Field center circle + lines
                drawCircle(Color(0xFF1B5E20).copy(alpha = 0.22f), w * 0.12f, Offset(w * 0.50f, h * 0.45f), style = Stroke(1.5f))
                drawLine(Color(0xFF1B5E20).copy(alpha = 0.25f), Offset(w * 0.05f, h * 0.45f), Offset(w * 0.95f, h * 0.45f), strokeWidth = 1.2f)
                // Goal posts bottom
                drawLine(Color(0xFF1B5E20).copy(alpha = 0.18f), Offset(w * 0.35f, h * 0.88f), Offset(w * 0.35f, h * 0.94f), strokeWidth = 1.5f)
                drawLine(Color(0xFF1B5E20).copy(alpha = 0.18f), Offset(w * 0.65f, h * 0.88f), Offset(w * 0.65f, h * 0.94f), strokeWidth = 1.5f)
                drawLine(Color(0xFF1B5E20).copy(alpha = 0.18f), Offset(w * 0.35f, h * 0.88f), Offset(w * 0.65f, h * 0.88f), strokeWidth = 1.5f)
                // Medal ribbon top-left
                val medalX = w * 0.12f; val medalY = h * 0.10f
                drawLine(Color(0xFFC49A3C).copy(alpha = 0.30f), Offset(medalX - 4f, 0f), Offset(medalX, medalY), strokeWidth = 3f)
                drawLine(Color(0xFFC49A3C).copy(alpha = 0.30f), Offset(medalX + 4f, 0f), Offset(medalX + 2f, medalY), strokeWidth = 3f)
                drawCircle(Color(0xFFC49A3C).copy(alpha = 0.25f), w * 0.03f, Offset(medalX, medalY + w * 0.03f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF1B5E20), badgeInk = Color.White,
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF1B3A1B),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1B5E20),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFF2A4A2A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1B5E20).copy(alpha = 0.65f)
        )
        // ═══ FOOD — warm recipe card with kitchen motifs ═══
        family == CategoryFamily.FOOD || cat.contains("FOOD") || cat.contains("CUISINE") || cat.contains("RECIPE") -> SignatureDesign(
            bg = Color(0xFFFFF5EE), cornerRadius = 10f,
            drawBackground = { w, h ->
                // Dotted border — recipe card style
                for (i in 0 until 50) {
                    val x = w * 0.04f + i * (w * 0.92f / 50f)
                    drawCircle(Color(0xFFD4845A).copy(alpha = 0.30f), 1.5f, Offset(x, h * 0.03f))
                    drawCircle(Color(0xFFD4845A).copy(alpha = 0.30f), 1.5f, Offset(x, h * 0.97f))
                }
                for (i in 0 until 40) {
                    val y = h * 0.03f + i * (h * 0.94f / 40f)
                    drawCircle(Color(0xFFD4845A).copy(alpha = 0.30f), 1.5f, Offset(w * 0.04f, y))
                    drawCircle(Color(0xFFD4845A).copy(alpha = 0.30f), 1.5f, Offset(w * 0.96f, y))
                }
                // Fork + knife crossed bottom-right
                val fkX = w * 0.82f; val fkY = h * 0.85f
                // Fork
                drawLine(Color(0xFF8B5E3C).copy(alpha = 0.22f), Offset(fkX, fkY), Offset(fkX, fkY + h * 0.10f), strokeWidth = 1.8f)
                drawLine(Color(0xFF8B5E3C).copy(alpha = 0.18f), Offset(fkX - 3f, fkY), Offset(fkX - 3f, fkY + h * 0.04f), strokeWidth = 0.8f)
                drawLine(Color(0xFF8B5E3C).copy(alpha = 0.18f), Offset(fkX + 3f, fkY), Offset(fkX + 3f, fkY + h * 0.04f), strokeWidth = 0.8f)
                // Knife
                drawLine(Color(0xFF8B5E3C).copy(alpha = 0.22f), Offset(fkX + w * 0.06f, fkY + h * 0.02f), Offset(fkX + w * 0.06f, fkY + h * 0.10f), strokeWidth = 1.8f)
                // Plate circle
                drawCircle(Color(0xFFD4845A).copy(alpha = 0.12f), w * 0.08f, Offset(fkX + w * 0.03f, fkY + h * 0.06f), style = Stroke(0.8f))
                // Steam wisps top
                for (i in 0 until 3) {
                    val sx = w * 0.40f + i * w * 0.10f
                    val steamPath = Path().apply {
                        moveTo(sx, h * 0.06f)
                        cubicTo(sx + 4f, h * 0.03f, sx - 4f, h * 0.01f, sx + 2f, h * -0.02f)
                    }
                    drawPath(steamPath, Color(0xFFD4845A).copy(alpha = 0.15f), style = Stroke(1f))
                }
                // Recipe ingredient lines
                for (i in 0 until 3) { drawLine(Color(0xFFD4845A).copy(alpha = 0.15f), Offset(w * 0.08f, h * 0.15f + i * h * 0.05f), Offset(w * 0.40f, h * 0.15f + i * h * 0.05f), strokeWidth = 0.5f) }
            },
            padding = PaddingValues(horizontal = 28.dp, vertical = 24.dp), badgeColor = Color(0xFFD4845A), badgeInk = Color.White,
            badgeRadius = 16.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF5A2A10),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFD4845A),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF6A3A1A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD4845A).copy(alpha = 0.60f)
        )
        // ═══ VISUAL ART — gallery frame + palette + brushstrokes ═══
        family == CategoryFamily.VISUAL_ART || cat.contains("ART") || cat.contains("PAINT") -> SignatureDesign(
            bg = Color(0xFFF8F6F2), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Double gallery frame
                val inset = w * 0.05f
                drawRect(Color(0xFF8A7A68).copy(alpha = 0.30f), Offset(inset, inset), Size(w - inset * 2, h - inset * 2), style = Stroke(2.5f))
                drawRect(Color(0xFF8A7A68).copy(alpha = 0.18f), Offset(inset + 5f, inset + 5f), Size(w - (inset + 5f) * 2, h - (inset + 5f) * 2), style = Stroke(0.7f))
                // Corner ornaments — small diamonds
                listOf(Offset(inset, inset), Offset(w - inset, inset), Offset(inset, h - inset), Offset(w - inset, h - inset)).forEach { p ->
                    drawCircle(Color(0xFF8A7A68).copy(alpha = 0.25f), 3.5f, p)
                }
                // Paint palette swatches — bottom-left
                val swatchColors = listOf(Color(0xFFC0392B), Color(0xFF2980B9), Color(0xFF27AE60), Color(0xFFF39C12), Color(0xFF8E44AD), Color(0xFFE67E22))
                swatchColors.forEachIndexed { i, c ->
                    drawCircle(c.copy(alpha = 0.28f), w * 0.022f, Offset(w * 0.10f + i * w * 0.055f, h * 0.90f))
                }
                // Brushstroke — thick diagonal sweep
                val brushPath = Path().apply {
                    moveTo(w * 0.60f, h * 0.15f)
                    cubicTo(w * 0.65f, h * 0.20f, w * 0.70f, h * 0.30f, w * 0.75f, h * 0.40f)
                }
                drawPath(brushPath, Color(0xFFE67E22).copy(alpha = 0.15f), style = Stroke(4f))
                val brushPath2 = Path().apply {
                    moveTo(w * 0.62f, h * 0.18f)
                    cubicTo(w * 0.67f, h * 0.23f, w * 0.72f, h * 0.33f, w * 0.77f, h * 0.43f)
                }
                drawPath(brushPath2, Color(0xFF2980B9).copy(alpha = 0.12f), style = Stroke(3f))
                // Canvas texture dots
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 60) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color(0xFF8A7A68).copy(alpha = 0.20f), 0.8f, Offset(x, y))
                }
            },
            padding = PaddingValues(horizontal = 32.dp, vertical = 28.dp), badgeColor = Color(0xFF4A4A4A), badgeInk = Color.White,
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.5.sp, titleTopSpacer = 16.dp,
            titleFont = LoraFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF1A1A1A),
            metaSpacer = 6.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8A8A8A),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF3A3A3A).copy(alpha = 0.80f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFAAAAAA)
        )
        // ═══ INTERNET — circuit board + WiFi + binary ═══
        family == CategoryFamily.INTERNET || cat.contains("INTERNET") || cat.contains("TECH") || cat.contains("DISCOVER") -> SignatureDesign(
            bg = Color(0xFFF0F5FF), cornerRadius = 6f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                // Circuit traces — right-angle lines
                for (i in 0 until 20) {
                    val x1 = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y1 = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    val x2 = x1 + ((s * (i+1) * 3571) % 200 - 100) / 100f * w * 0.12f
                    val y2 = y1 + ((s * (i+1) * 4201) % 200 - 100) / 100f * h * 0.08f
                    drawLine(Color(0xFF2563EB).copy(alpha = 0.30f), Offset(x1, y1), Offset(x2, y1), strokeWidth = 0.7f)
                    drawLine(Color(0xFF2563EB).copy(alpha = 0.30f), Offset(x2, y1), Offset(x2, y2), strokeWidth = 0.7f)
                    drawCircle(Color(0xFF2563EB).copy(alpha = 0.25f), 2.0f, Offset(x2, y2))
                }
                // WiFi signal arcs top-right
                val wifiCx = w * 0.85f; val wifiCy = h * 0.12f
                for (i in 1..3) {
                    drawArc(Color(0xFF2563EB).copy(alpha = 0.15f + i * 0.05f), -45f, 90f, false,
                        Offset(wifiCx - i * w * 0.03f, wifiCy - i * h * 0.03f),
                        Size(i * w * 0.06f, i * h * 0.06f), style = Stroke(1f))
                }
                // IC chip outline bottom-right
                drawRoundRect(Color(0xFF2563EB).copy(alpha = 0.18f), Offset(w * 0.72f, h * 0.80f), Size(w * 0.20f, h * 0.12f), CornerRadius(3f), style = Stroke(1f))
                // Chip pins
                for (i in 0 until 5) {
                    drawLine(Color(0xFF2563EB).copy(alpha = 0.22f), Offset(w * 0.74f + i * w * 0.035f, h * 0.80f), Offset(w * 0.74f + i * w * 0.035f, h * 0.77f), strokeWidth = 0.7f)
                    drawLine(Color(0xFF2563EB).copy(alpha = 0.22f), Offset(w * 0.74f + i * w * 0.035f, h * 0.92f), Offset(w * 0.74f + i * w * 0.035f, h * 0.95f), strokeWidth = 0.7f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF2563EB), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF1A2A4A),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF2563EB),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF2A3A5A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF2563EB).copy(alpha = 0.65f)
        )
        // ═══ QUOTES — elegant ornamental frame + large quote marks ═══
        cat.contains("QUOTE") -> SignatureDesign(
            bg = Color(0xFFFDF8F0), cornerRadius = 10f,
            drawBackground = { w, h ->
                // Double decorative border
                drawRect(Color(0xFF8A6B42).copy(alpha = 0.22f), Offset(w * 0.06f, h * 0.04f), Size(w * 0.88f, h * 0.92f), style = Stroke(1.5f))
                drawRect(Color(0xFF8A6B42).copy(alpha = 0.12f), Offset(w * 0.08f, h * 0.06f), Size(w * 0.84f, h * 0.88f), style = Stroke(0.5f))
                // Large opening quote — top-left
                drawCircle(Color(0xFF8A6B42).copy(alpha = 0.22f), w * 0.04f, Offset(w * 0.12f, h * 0.10f), style = Stroke(2.5f))
                drawCircle(Color(0xFF8A6B42).copy(alpha = 0.18f), w * 0.04f, Offset(w * 0.20f, h * 0.10f), style = Stroke(2.5f))
                // Closing quote — bottom-right
                drawCircle(Color(0xFF8A6B42).copy(alpha = 0.18f), w * 0.04f, Offset(w * 0.76f, h * 0.88f), style = Stroke(2.5f))
                drawCircle(Color(0xFF8A6B42).copy(alpha = 0.22f), w * 0.04f, Offset(w * 0.84f, h * 0.88f), style = Stroke(2.5f))
                // Scroll flourish bottom
                val scrollPath = Path().apply {
                    moveTo(w * 0.30f, h * 0.92f)
                    cubicTo(w * 0.40f, h * 0.90f, w * 0.60f, h * 0.90f, w * 0.70f, h * 0.92f)
                }
                drawPath(scrollPath, Color(0xFF8A6B42).copy(alpha = 0.18f), style = Stroke(1f))
                // Quill pen silhouette top-right
                val quillPath = Path().apply {
                    moveTo(w * 0.82f, h * 0.06f)
                    quadraticBezierTo(w * 0.88f, h * 0.12f, w * 0.85f, h * 0.22f)
                    lineTo(w * 0.83f, h * 0.20f)
                    quadraticBezierTo(w * 0.86f, h * 0.12f, w * 0.81f, h * 0.07f)
                    close()
                }
                drawPath(quillPath, Color(0xFF8A6B42).copy(alpha = 0.12f))
            },
            padding = PaddingValues(horizontal = 28.dp, vertical = 24.dp), badgeColor = Color(0xFF8A6B42), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 12.dp,
            titleFont = LoraFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2814),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8A6B42),
            bodySize = 10.5f, bodyLineHeight = 1.65f, bodyColor = Color(0xFF4A3824).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8A6B42).copy(alpha = 0.60f)
        )
        // ═══ DEFAULT / WILDCARD — cosmic compass with starfield ═══
        else -> SignatureDesign(
            bg = Color(0xFF0F1724), cornerRadius = 6f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                // Starfield
                for (i in 0 until 80) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    val r = 0.5f + ((s * (i+1) * 3571) % 100) / 100f * 2.0f
                    val a = 0.08f + ((s * (i+1) * 4201) % 100) / 100f * 0.18f
                    drawCircle(Color.White.copy(alpha = a), r, Offset(x, y))
                }
                // Nebula glow
                drawCircle(Color(0xFF4A3A8A).copy(alpha = 0.25f), w * 0.25f, Offset(w * 0.75f, h * 0.30f))
                drawCircle(Color(0xFF2A4A6A).copy(alpha = 0.20f), w * 0.20f, Offset(w * 0.20f, h * 0.75f))
                // Compass rose bottom-right
                val cx = w * 0.82f; val cy = h * 0.88f; val cr = w * 0.06f
                drawLine(Color(0xFFC0C8E0).copy(alpha = 0.25f), Offset(cx, cy - cr), Offset(cx, cy + cr), strokeWidth = 1f)
                drawLine(Color(0xFFC0C8E0).copy(alpha = 0.25f), Offset(cx - cr, cy), Offset(cx + cr, cy), strokeWidth = 1f)
                drawLine(Color(0xFFC0C8E0).copy(alpha = 0.15f), Offset(cx - cr * 0.6f, cy - cr * 0.6f), Offset(cx + cr * 0.6f, cy + cr * 0.6f), strokeWidth = 0.5f)
                drawLine(Color(0xFFC0C8E0).copy(alpha = 0.15f), Offset(cx + cr * 0.6f, cy - cr * 0.6f), Offset(cx - cr * 0.6f, cy + cr * 0.6f), strokeWidth = 0.5f)
                drawCircle(Color(0xFFC0C8E0).copy(alpha = 0.18f), 2f, Offset(cx, cy))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF6A5A9A), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8E0F0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8A7AB0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8C0D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF8A7AB0).copy(alpha = 0.65f)
        )
    }
}


private fun signatureDesign(categoryName: String, family: CategoryFamily): SignatureDesign {
    val cat = categoryName.uppercase().trim()
    return when {
        // ═══ ARTISTS — stage, singer at the mic, spotlight beams ═══
        cat == "ARTISTS" -> SignatureDesign(
            bg = Color(0xFF171231), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Deep indigo stage with a soft gradient
                drawRect(Brush.verticalGradient(listOf(Color(0xFF241A4E), Color(0xFF171231), Color(0xFF0C091C))), size = Size(w, h))
                // Twin spotlight cones converging onto the stage floor — the
                // POSTER title floats in the beam, singer stands low on stage
                listOf(Offset(w * 0.26f, -h * 0.02f), Offset(w * 0.74f, -h * 0.02f)).forEachIndexed { i, c ->
                    val cone = Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x - w * 0.14f, h * 0.80f)
                        lineTo(c.x + w * 0.16f, h * 0.80f)
                        close()
                    }
                    drawPath(cone, if (i == 0) Color(0xFFC9BFFF).copy(alpha = 0.10f) else Color(0xFFF2C879).copy(alpha = 0.08f))
                }
                // Light pools on the stage floor
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFC9BFFF).copy(alpha = 0.16f), Color.Transparent)), radius = w * 0.22f, center = Offset(w * 0.26f, h * 0.80f))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.13f), Color.Transparent)), radius = w * 0.24f, center = Offset(w * 0.74f, h * 0.80f))
                // Stage floor edge
                drawLine(Color(0xFF8F7BFF).copy(alpha = 0.40f), Offset(w * 0.05f, h * 0.90f), Offset(w * 0.95f, h * 0.90f), strokeWidth = 1.4f)
                // Singer silhouette + mic, small, left of centre on the stage
                val sx = w * 0.40f; val sy = h * 0.72f
                drawCircle(Color(0xFF0B0818), w * 0.024f, Offset(sx, sy - h * 0.05f))  // head
                drawLine(Color(0xFF0B0818), Offset(sx, sy - h * 0.026f), Offset(sx, sy + h * 0.09f), strokeWidth = 3f)  // body
                drawLine(Color(0xFF0B0818), Offset(sx, sy - h * 0.02f), Offset(sx - w * 0.04f, sy + h * 0.03f), strokeWidth = 2f)  // arm
                drawLine(Color(0xFF0B0818), Offset(sx, sy - h * 0.02f), Offset(sx + w * 0.045f, sy + h * 0.02f), strokeWidth = 2f)  // arm to mic
                // Mic stand with round head
                val mx = w * 0.50f
                drawLine(Color(0xFFE9E3FF).copy(alpha = 0.55f), Offset(mx, sy - h * 0.055f), Offset(mx, h * 0.90f), strokeWidth = 1.5f)
                drawLine(Color(0xFFE9E3FF).copy(alpha = 0.45f), Offset(mx - w * 0.02f, h * 0.90f), Offset(mx + w * 0.02f, h * 0.90f), strokeWidth = 1.5f)
                drawCircle(Color(0xFFE9E3FF).copy(alpha = 0.65f), w * 0.02f, Offset(mx, sy - h * 0.065f))
                // Monitor speaker wedge on stage right
                drawPath(Path().apply {
                    moveTo(w * 0.84f, h * 0.84f)
                    lineTo(w * 0.94f, h * 0.84f)
                    lineTo(w * 0.91f, h * 0.90f)
                    lineTo(w * 0.81f, h * 0.90f)
                    close()
                }, Color(0xFF0B0818).copy(alpha = 0.55f))
                // Confetti — colourful flecks falling in the light, upper half
                val s = (w * 1000 + h).toInt()
                val cols = listOf(Color(0xFFF2C879), Color(0xFFC9BFFF), Color(0xFFFF9AB8), Color(0xFF6FE3C1))
                for (i in 0 until 26) {
                    val x = w * 0.06f + ((s * (i+1) * 7919) % 100) / 100f * w * 0.88f
                    val y = h * 0.08f + ((s * (i+1) * 6271) % 100) / 100f * h * 0.55f
                    drawCircle(cols[i % 4].copy(alpha = 0.36f), 1.5f + (i % 3) * 0.5f, Offset(x, y))
                    if (i % 4 == 0) drawLine(cols[i % 4].copy(alpha = 0.24f), Offset(x - 3f, y), Offset(x + 3f, y), strokeWidth = 0.8f)
                }
                // Music notes floating
                listOf(Offset(w * 0.14f, h * 0.20f), Offset(w * 0.88f, h * 0.26f), Offset(w * 0.09f, h * 0.44f)).forEach { p ->
                    drawCircle(Color(0xFFC9BFFF).copy(alpha = 0.45f), w * 0.016f, Offset(p.x, p.y))
                    drawLine(Color(0xFFC9BFFF).copy(alpha = 0.45f), Offset(p.x, p.y), Offset(p.x, p.y - h * 0.045f), strokeWidth = 1f)
                    drawArc(Color(0xFFC9BFFF).copy(alpha = 0.40f), 0f, 180f, false, Offset(p.x - w * 0.004f, p.y - h * 0.05f), Size(w * 0.024f, w * 0.024f), style = Stroke(0.9f))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8F7BFF), badgeInk = Color(0xFF171231),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF2EEFF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8F7BFF),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD9D0F5).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF8F7BFF).copy(alpha = 0.70f),
            layout = SignatureLayout.POSTER
        )
        // ═══ ALBUMS — vinyl record right, grooves, crimson label ═══
        cat == "ALBUMS" -> SignatureDesign(
            bg = Color(0xFF160F14), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Warm dusk gradient — record-collection room mood
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1B1E), Color(0xFF160F14), Color(0xFF0C0709))), size = Size(w, h))
                // Echo arcs on the right
                drawArc(Color(0xFFE8D5B5).copy(alpha = 0.10f), -55f, 80f, false, Offset(w * 0.72f, h * 0.10f), Size(w * 0.34f, w * 0.34f), style = Stroke(1.2f))
                drawArc(Color(0xFFC2402E).copy(alpha = 0.12f), -55f, 80f, false, Offset(w * 0.80f, h * 0.18f), Size(w * 0.26f, w * 0.26f), style = Stroke(1.2f))
                // Vinyl record bleeding off the right edge — STANDARD title
                // column stays left, clear of the disc
                val cx = w * 0.80f; val cy = h * 0.50f
                for (i in 0 until 18) { drawCircle(Color(0xFFE8D5B5).copy(alpha = 0.30f), w * 0.24f - i * w * 0.013f, Offset(cx, cy), style = Stroke(1f)) }
                drawCircle(Color(0xFFE8D5B5).copy(alpha = 0.40f), w * 0.24f, Offset(cx, cy))
                drawCircle(Color(0xFFC2402E), w * 0.09f, Offset(cx, cy))
                drawCircle(Color(0xFF160F14).copy(alpha = 0.55f), w * 0.026f, Offset(cx, cy))
                // Label sheen — a soft highlight arc on the crimson label
                drawArc(Color.White.copy(alpha = 0.10f), 200f, 60f, false, Offset(cx - w * 0.07f, cy - w * 0.07f), Size(w * 0.14f, w * 0.14f), style = Stroke(1f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC2402E), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 30.sp, titleLineHeight = 36.sp, titleColor = Color(0xFFF5E9E2),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8A5A0),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFE0D2CE).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC2402E).copy(alpha = 0.70f),
            layout = SignatureLayout.STANDARD
        )
        // ═══ SONGS — thin glowing waveform bars + floating notes ═══
        cat == "SONGS" -> SignatureDesign(
            bg = Color(0xFF26091B), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Soundwave bars — solid bars grouped low, compact vertical
                // span (not thin lines; the waveform's top-to-bottom extent
                // is kept close to the centre so it never dominates the card)
                for (i in 0 until 22) {
                    val x = w * 0.08f + i * w * 0.038f
                    val hgt = h * (0.14f + 0.22f * kotlin.math.abs(kotlin.math.sin(i * 0.9f)).toFloat())
                    drawRoundRect(Color(0xFFFF8FA3).copy(alpha = 0.45f), Offset(x, h * 0.52f - hgt / 2f), Size(w * 0.016f, hgt), CornerRadius(2f))
                }
                // Music note glyph
                drawCircle(Color(0xFFFFD9A0).copy(alpha = 0.55f), w * 0.035f, Offset(w * 0.78f, h * 0.30f))
                drawLine(Color(0xFFFFD9A0).copy(alpha = 0.55f), Offset(w * 0.78f, h * 0.30f), Offset(w * 0.78f, h * 0.14f), strokeWidth = 1.6f)
                drawArc(Color(0xFFFFD9A0).copy(alpha = 0.50f), 0f, 180f, false, Offset(w * 0.78f, h * 0.12f), Size(w * 0.05f, h * 0.05f), style = Stroke(1.6f))
                // Small note
                drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.35f), w * 0.024f, Offset(w * 0.16f, h * 0.22f))
                drawLine(Color(0xFFFF8FA3).copy(alpha = 0.35f), Offset(w * 0.16f, h * 0.22f), Offset(w * 0.16f, h * 0.10f), strokeWidth = 1.2f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF8FA3), badgeInk = Color(0xFF26091B),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFD9E4),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF8FA3),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE8C4D2).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF8FA3).copy(alpha = 0.70f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ DIRECTORS — velvet curtain, clapperboard, reel, gold accents ═══
        cat == "DIRECTORS" -> SignatureDesign(
            bg = Color(0xFF141416), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Cinema dark with a warm marquee glow from above
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1D18), Color(0xFF141416), Color(0xFF0B0B0D))), size = Size(w, h))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFD9B45B).copy(alpha = 0.10f), Color.Transparent)), radius = w * 0.40f, center = Offset(w * 0.50f, -h * 0.06f))
                // Velvet curtain drapes framing the sides
                listOf(0f, 1f).forEach { side ->
                    val path = Path().apply {
                        moveTo(side * w, 0f)
                        quadraticBezierTo(side * w + (if (side == 0f) 1f else -1f) * w * 0.09f, h * 0.5f, side * w, h)
                        lineTo(side * w, 0f)
                        close()
                    }
                    drawPath(path, Color(0xFF8B1A1A).copy(alpha = 0.35f))
                }
                // Clapperboard, angled centre-top
                val cb = Path().apply {
                    moveTo(w * 0.42f, h * 0.12f); lineTo(w * 0.76f, h * 0.07f)
                    lineTo(w * 0.69f, h * 0.28f); lineTo(w * 0.36f, h * 0.33f); close()
                }
                drawPath(cb, Color(0xFFE8E2D4).copy(alpha = 0.28f))
                drawLine(Color(0xFF141416).copy(alpha = 0.9f), Offset(w * 0.41f, h * 0.16f), Offset(w * 0.72f, h * 0.11f), strokeWidth = 1.6f)
                drawLine(Color(0xFF141416).copy(alpha = 0.9f), Offset(w * 0.39f, h * 0.23f), Offset(w * 0.70f, h * 0.18f), strokeWidth = 1.6f)
                // Film reel bottom-right
                val rcx = w * 0.86f; val rcy = h * 0.78f
                drawCircle(Color(0xFFD9B45B).copy(alpha = 0.35f), w * 0.09f, Offset(rcx, rcy), style = Stroke(1.5f))
                drawCircle(Color(0xFFD9B45B).copy(alpha = 0.30f), w * 0.03f, Offset(rcx, rcy))
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0).toFloat()
                    drawCircle(Color(0xFFD9B45B).copy(alpha = 0.40f), 1.8f, Offset(rcx + kotlin.math.cos(a) * w * 0.06f, rcy + kotlin.math.sin(a) * w * 0.06f))
                }
                // Gold frame corners
                listOf(Offset(w * 0.06f, h * 0.06f), Offset(w * 0.94f, h * 0.06f), Offset(w * 0.06f, h * 0.94f), Offset(w * 0.94f, h * 0.94f)).forEach { p ->
                    drawLine(Color(0xFFD9B45B).copy(alpha = 0.30f), p, Offset(p.x + w * 0.06f, p.y), strokeWidth = 1.2f)
                    drawLine(Color(0xFFD9B45B).copy(alpha = 0.30f), p, Offset(p.x, p.y + h * 0.06f), strokeWidth = 1.2f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD9B45B), badgeInk = Color(0xFF141416),
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 16.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF0E8DA),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFD9B45B),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFCFC8BC).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD9B45B).copy(alpha = 0.60f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ FILMS — dark cinematic, prominent film strips + sprocket holes ═══
        cat == "FILMS" -> SignatureDesign(
            bg = Color(0xFF0D0D12), cornerRadius = 6f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                // Cinema dark with a warm marquee glow from above
                drawRect(Brush.verticalGradient(listOf(Color(0xFF241A18), Color(0xFF0D0D12), Color(0xFF07070A))), size = Size(w, h))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.12f), Color.Transparent)), radius = w * 0.42f, center = Offset(w * 0.50f, -h * 0.04f))
                // Film grain
                for (i in 0 until 120) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color.White.copy(alpha = 0.035f), 1.2f, Offset(x, y))
                }
                // Velvet curtain drapes framing the sides
                listOf(0f, 1f).forEach { side ->
                    val path = Path().apply {
                        moveTo(side * w, 0f)
                        quadraticBezierTo(side * w + (if (side == 0f) 1f else -1f) * w * 0.09f, h * 0.5f, side * w, h)
                        lineTo(side * w, 0f)
                        close()
                    }
                    drawPath(path, Color(0xFF8B1A1A).copy(alpha = 0.30f))
                }
                // Film reel bottom-left — spoked wheel with hub
                val rcx = w * 0.14f; val rcy = h * 0.78f
                drawCircle(Color(0xFFC8C4BC).copy(alpha = 0.30f), w * 0.08f, Offset(rcx, rcy), style = Stroke(1.4f))
                drawCircle(Color(0xFFC8C4BC).copy(alpha = 0.28f), w * 0.025f, Offset(rcx, rcy))
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0).toFloat()
                    drawCircle(Color(0xFFC8C4BC).copy(alpha = 0.35f), 1.7f, Offset(rcx + kotlin.math.cos(a) * w * 0.055f, rcy + kotlin.math.sin(a) * w * 0.055f))
                }
                // Film strip band across the bottom edge with sprocket holes
                drawRoundRect(Color(0xFFC8C4BC).copy(alpha = 0.20f), Offset(w * 0.02f, h * 0.90f), Size(w * 0.96f, h * 0.035f), CornerRadius(1.5f))
                for (i in 0 until 12) {
                    val x = w * 0.035f + i * w * 0.078f
                    drawRoundRect(Color(0xFF0D0D12), Offset(x, h * 0.906f), Size(w * 0.02f, h * 0.018f), CornerRadius(1.5f))
                }
                // Projector flicker beam from the right edge
                drawPath(Path().apply {
                    moveTo(w * 0.99f, h * 0.06f)
                    lineTo(w * 0.86f, h * 0.30f)
                    lineTo(w * 0.90f, h * 0.34f)
                    lineTo(w * 0.99f, h * 0.14f)
                    close()
                }, Color(0xFFC8C4BC).copy(alpha = 0.10f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8B1A1A), badgeInk = Color.White,
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp,
            badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp,
            titleTopSpacer = 16.dp, titleFont = BungeeFontFamily, titleSize = 30.sp,
            titleLineHeight = 36.sp, titleColor = Color(0xFFF5F0E8),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFB84444),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0D0D0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8B1A1A).copy(alpha = 0.70f),
            layout = SignatureLayout.POSTER
        )
        // ═══ ANIMATED FILMS — shared-center rainbow swoosh + star trail ═══
        cat == "ANIMATED FILMS" || cat == "ANIMATED MOVIES" -> SignatureDesign(
            bg = Color(0xFF241A45), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Dreamy dusk gradient + twinkling stars
                drawRect(Brush.verticalGradient(listOf(Color(0xFF4A2A6E), Color(0xFF241A45), Color(0xFF120E24))), size = Size(w, h))
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 26) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h * 0.5f
                    drawCircle(Color.White.copy(alpha = 0.14f + (i % 3) * 0.06f), 0.8f + (i % 2) * 0.5f, Offset(x, y))
                }
                // Colour orbs — warmer + brighter
                listOf(Pair(Offset(w * 0.16f, h * 0.24f), Color(0xFFFF8FA3)), Pair(Offset(w * 0.86f, h * 0.30f), Color(0xFF8FD0FF))).forEach { (c, col) ->
                    drawCircle(brush = Brush.radialGradient(listOf(col.copy(alpha = 0.26f), Color.Transparent)), radius = w * 0.28f, center = c)
                }
                // Rainbow ribbon — one shared arc center, bands stacked tightly
                val rainbow = listOf(Color(0xFFFF8FA3), Color(0xFFFFD9A0), Color(0xFFA6E8A0), Color(0xFF8FD0FF), Color(0xFFC9BFFF))
                val acx = w * 0.52f; val acy = h * 0.50f; val ar = w * 0.36f
                rainbow.forEachIndexed { i, c ->
                    drawArc(c.copy(alpha = 0.55f), 200f, 150f, false, Offset(acx - ar + i * w * 0.012f, acy - ar + i * w * 0.012f), Size((ar - i * w * 0.012f) * 2f, (ar - i * w * 0.012f) * 2f), style = Stroke(w * 0.014f))
                }
                // Bouncing star trail riding the ribbon
                for (i in 0 until 14) {
                    val t = i / 13f
                    val x = w * 0.14f + t * w * 0.74f
                    val y = h * 0.46f - kotlin.math.sin(t * 3.14159f).toFloat() * h * 0.10f
                    drawCircle(Color(0xFFFFD9A0).copy(alpha = 0.60f - t * 0.38f), 2.8f - t * 1.3f, Offset(x, y))
                    drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.24f - t * 0.13f), (2.8f - t * 1.3f) * 1.6f, Offset(x, y))
                }
                // Sparkles scattered (4-point)
                listOf(Offset(w * 0.12f, h * 0.20f), Offset(w * 0.90f, h * 0.16f), Offset(w * 0.86f, h * 0.78f), Offset(w * 0.28f, h * 0.84f)).forEach { p ->
                    drawLine(Color(0xFFFFE08A).copy(alpha = 0.55f), Offset(p.x - 6f, p.y), Offset(p.x + 6f, p.y), strokeWidth = 1f)
                    drawLine(Color(0xFFFFE08A).copy(alpha = 0.55f), Offset(p.x, p.y - 6f), Offset(p.x, p.y + 6f), strokeWidth = 1f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFD9A0), badgeInk = Color(0xFF241A45),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF3D9),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFFD9A0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFDCCFE8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFFD9A0).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ AUTHORS — inkwell, nib, manuscript lines ═══
        cat == "AUTHORS" -> SignatureDesign(
            bg = Color(0xFF101C33), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Simple navy gradient — calm writing desk backdrop
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1A2A4C), Color(0xFF101C33), Color(0xFF0A1220))), size = Size(w, h))
                // Manuscript lines spanning the card, faint and even
                for (i in 0 until 7) {
                    val y = h * 0.62f + i * h * 0.045f
                    drawLine(Color(0xFFE8DCC0).copy(alpha = 0.09f), Offset(w * 0.08f, y), Offset(w * 0.92f, y), strokeWidth = 0.5f)
                }
                // Feather quill — a real spine with barbs, sweeping top-right
                val qs = Offset(w * 0.70f, h * 0.10f); val qe = Offset(w * 0.88f, h * 0.34f)
                drawPath(Path().apply {
                    moveTo(qs.x, qs.y)
                    quadraticBezierTo(w * 0.76f, h * 0.22f, qe.x, qe.y)
                }, Color(0xFFE8DCC0).copy(alpha = 0.50f), style = Stroke(1.8f))
                for (i in 0 until 7) {
                    val t = i / 6f
                    val bx = qs.x + (qe.x - qs.x) * t
                    val by = qs.y + (qe.y - qs.y) * t
                    drawLine(Color(0xFFE8DCC0).copy(alpha = 0.26f), Offset(bx, by), Offset(bx - w * 0.05f - t * w * 0.02f, by - h * 0.018f), strokeWidth = 0.7f)
                }
                // Inkwell — simple jar with a gold rim, bottom-right
                val ix = w * 0.84f; val iy = h * 0.72f
                drawPath(Path().apply {
                    moveTo(ix - w * 0.06f, iy)
                    lineTo(ix + w * 0.06f, iy)
                    lineTo(ix + w * 0.05f, iy + h * 0.14f)
                    lineTo(ix - w * 0.05f, iy + h * 0.14f)
                    close()
                }, Color(0xFF2A2A3E).copy(alpha = 0.85f))
                drawLine(Color(0xFFD9C58A).copy(alpha = 0.50f), Offset(ix - w * 0.065f, iy), Offset(ix + w * 0.065f, iy), strokeWidth = 1.3f)
                // Ink pool + a few splatter dots under the well
                drawCircle(Color(0xFF0A1220), w * 0.042f, Offset(ix - w * 0.02f, iy + h * 0.16f))
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 8) {
                    val x = ix - w * 0.11f + ((s * (i+1) * 3571) % 100) / 100f * w * 0.20f
                    val y = iy + h * 0.19f + ((s * (i+1) * 4201) % 100) / 100f * h * 0.10f
                    drawCircle(Color(0xFFE8DCC0).copy(alpha = 0.16f), 1.0f + (i % 2) * 0.6f, Offset(x, y))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD9C58A), badgeInk = Color(0xFF101C33),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF0E8D4),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFD9C58A),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFC8D2E8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD9C58A).copy(alpha = 0.60f),
            layout = SignatureLayout.SIDE
        )
        // ═══ BOOKS — warm library manuscript with leather spine ═══
        cat == "BOOKS" -> SignatureDesign(
            bg = Color(0xFFF5EDE0), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Warm library wall — soft radial lamp glow from above-left
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFAF3E6), Color(0xFFF5EDE0), Color(0xFFE8DCC8))), size = Size(w, h))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFE9B8).copy(alpha = 0.35f), Color.Transparent)), radius = w * 0.38f, center = Offset(w * 0.16f, h * 0.06f))
                // Shelf — a row of books with varied heights + gold bands
                val shelfY = h * 0.68f
                drawRect(Color(0xFF8A6A42).copy(alpha = 0.55f), Offset(0f, shelfY), Size(w, h * 0.02f))
                val books = listOf(
                    Triple(0.05f, 0.30f, Color(0xFF5C3317)), Triple(0.12f, 0.36f, Color(0xFF7A4B2A)),
                    Triple(0.19f, 0.28f, Color(0xFF2E5A4A)), Triple(0.25f, 0.40f, Color(0xFF8B1A1A)),
                    Triple(0.32f, 0.33f, Color(0xFF3A5A8A)), Triple(0.39f, 0.26f, Color(0xFF6A4A8A)),
                    Triple(0.45f, 0.38f, Color(0xFF5C3317)), Triple(0.52f, 0.30f, Color(0xFF8A6A2E)),
                    Triple(0.59f, 0.36f, Color(0xFF2E5A4A)), Triple(0.66f, 0.28f, Color(0xFF7A4B2A)),
                    Triple(0.72f, 0.34f, Color(0xFF8B1A1A)), Triple(0.79f, 0.40f, Color(0xFF3A5A8A)),
                    Triple(0.86f, 0.30f, Color(0xFF6A4A8A))
                )
                books.forEach { (bx, bw, col) ->
                    drawRect(col.copy(alpha = 0.75f), Offset(w * bx, shelfY - h * bw * 0.9f), Size(w * 0.055f, h * bw * 0.9f))
                    drawRect(Color(0xFFC49A3C).copy(alpha = 0.60f), Offset(w * bx + w * 0.008f, shelfY - h * bw * 0.9f), Size(w * 0.04f, h * 0.008f))
                }
                // Open book on the shelf — pages fan + a reading line
                drawPath(Path().apply {
                    moveTo(w * 0.20f, shelfY - h * 0.012f)
                    quadraticBezierTo(w * 0.30f, shelfY - h * 0.16f, w * 0.42f, shelfY - h * 0.06f)
                    lineTo(w * 0.42f, shelfY - h * 0.012f)
                    close()
                }, Color(0xFFF2EEE6).copy(alpha = 0.85f))
                drawLine(Color(0xFFBFA882).copy(alpha = 0.45f), Offset(w * 0.235f, shelfY - h * 0.055f), Offset(w * 0.34f, shelfY - h * 0.085f), strokeWidth = 0.7f)
                drawLine(Color(0xFFBFA882).copy(alpha = 0.45f), Offset(w * 0.24f, shelfY - h * 0.04f), Offset(w * 0.35f, shelfY - h * 0.07f), strokeWidth = 0.7f)
                // A few floating dust motes in the lamp light
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 14) {
                    val x = w * 0.08f + ((s * (i+1) * 3571) % 100) / 100f * w * 0.30f
                    val y = h * 0.14f + ((s * (i+1) * 4201) % 100) / 100f * h * 0.30f
                    drawCircle(Color(0xFFC49A3C).copy(alpha = 0.20f), 0.9f + (i % 2) * 0.6f, Offset(x, y))
                }
            },
            padding = PaddingValues(horizontal = 26.dp, vertical = 22.dp), badgeColor = Color(0xFF5C3317), badgeInk = Color(0xFFF5EDE0),
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = PlayfairDisplayFontFamily, titleSize = 30.sp, titleLineHeight = 38.sp, titleColor = Color(0xFF3A2814),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF5C3317).copy(alpha = 0.65f),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF4A3824).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF5C3317).copy(alpha = 0.55f),
            layout = SignatureLayout.STANDARD
        )
        // ═══ PAINTERS — easel right, framed canvas, palette, brush ═══
        cat == "PAINTERS" -> SignatureDesign(
            bg = Color(0xFF26211D), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Warm studio gradient with a window-light glow top-left
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3A322B), Color(0xFF26211D), Color(0xFF161310))), size = Size(w, h))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFD9A0).copy(alpha = 0.12f), Color.Transparent)), radius = w * 0.35f, center = Offset(w * 0.14f, h * 0.04f))
                // Easel legs — A-frame, shifted RIGHT so the STANDARD title
                // column (top-left) stays clear of the art
                drawLine(Color(0xFF8A7A5C).copy(alpha = 0.45f), Offset(w * 0.62f, h * 0.90f), Offset(w * 0.80f, h * 0.18f), strokeWidth = 2.2f)
                drawLine(Color(0xFF8A7A5C).copy(alpha = 0.45f), Offset(w * 0.96f, h * 0.90f), Offset(w * 0.80f, h * 0.18f), strokeWidth = 2.2f)
                drawLine(Color(0xFF8A7A5C).copy(alpha = 0.40f), Offset(w * 0.80f, h * 0.18f), Offset(w * 0.93f, h * 0.08f), strokeWidth = 2.2f)  // back leg
                drawLine(Color(0xFF8A7A5C).copy(alpha = 0.35f), Offset(w * 0.66f, h * 0.46f), Offset(w * 0.94f, h * 0.46f), strokeWidth = 1.4f)  // shelf
                // Canvas on the easel — wood frame + gesso board
                val cx = w * 0.80f; val cy = h * 0.16f
                drawRect(Color(0xFF6A5C42).copy(alpha = 0.65f), Offset(cx - w * 0.15f, cy - h * 0.012f), Size(w * 0.30f, h * 0.335f))
                drawRect(Color(0xFFF2EEE6), Offset(cx - w * 0.135f, cy), Size(w * 0.27f, h * 0.31f))
                // Landscape painting on the canvas — sky, sun, hills, foreground
                drawRect(Color(0xFF9BC4E2).copy(alpha = 0.95f), Offset(cx - w * 0.135f, cy), Size(w * 0.27f, h * 0.155f))
                drawCircle(Color(0xFFF2C879), w * 0.026f, Offset(cx - w * 0.07f, cy + h * 0.045f))
                drawPath(Path().apply {
                    moveTo(cx - w * 0.135f, cy + h * 0.19f)
                    quadraticBezierTo(cx - w * 0.04f, cy + h * 0.14f, cx + w * 0.02f, cy + h * 0.19f)
                    quadraticBezierTo(cx + w * 0.10f, cy + h * 0.14f, cx + w * 0.135f, cy + h * 0.19f)
                    lineTo(cx + w * 0.135f, cy + h * 0.31f); lineTo(cx - w * 0.135f, cy + h * 0.31f); close()
                }, Color(0xFF5E8C4E).copy(alpha = 0.95f))
                // Palette — kidney shape on the easel shelf (right side)
                val pcx = w * 0.62f; val pcy = h * 0.70f
                drawOval(Color(0xFFE8E2D4).copy(alpha = 0.55f), Offset(pcx - w * 0.10f, pcy - w * 0.075f), Size(w * 0.20f, w * 0.15f))
                drawOval(Color(0xFFE8E2D4).copy(alpha = 0.55f), Offset(pcx + w * 0.03f, pcy - w * 0.05f), Size(w * 0.13f, w * 0.12f))
                // Thumb hole
                drawCircle(Color(0xFF26211D), w * 0.028f, Offset(pcx - w * 0.045f, pcy + w * 0.02f))
                // Colour wells around the rim
                val wells = listOf(Color(0xFFC0392B), Color(0xFF2980B9), Color(0xFF27AE60), Color(0xFFF39C12), Color(0xFF8E44AD), Color(0xFFE67E22))
                val wellPos = listOf(Offset(-0.065f, -0.055f), Offset(0.02f, -0.07f), Offset(0.075f, -0.02f), Offset(0.065f, 0.05f), Offset(-0.01f, 0.075f), Offset(-0.075f, 0.045f))
                wellPos.forEachIndexed { i, (dx, dy) ->
                    drawCircle(wells[i].copy(alpha = 0.60f), w * 0.018f, Offset(pcx + dx * w, pcy + dy * w))
                }
                // Brush resting on the easel shelf
                drawLine(Color(0xFFC9BFA8).copy(alpha = 0.55f), Offset(w * 0.70f, h * 0.56f), Offset(w * 0.99f, h * 0.26f), strokeWidth = 2.2f)
                drawLine(Color(0xFF6A5C42).copy(alpha = 0.55f), Offset(w * 0.99f, h * 0.26f), Offset(w * 0.995f, h * 0.21f), strokeWidth = 2.0f)  // ferrule
                // Bristles — short fan
                for (i in 0 until 4) {
                    val bx = w * 0.995f
                    val by = h * 0.21f + i * h * 0.008f - h * 0.012f
                    drawLine(Color(0xFFE67E22).copy(alpha = 0.55f), Offset(bx, by), Offset(bx + w * 0.014f, by - h * 0.004f), strokeWidth = 1.1f)
                }
                // Paint drips under the canvas
                listOf(Offset(w * 0.72f, h * 0.50f), Offset(w * 0.80f, h * 0.54f), Offset(w * 0.88f, h * 0.52f)).forEachIndexed { i, p ->
                    drawCircle(wells[i].copy(alpha = 0.85f), 2.2f, Offset(p.x, p.y))
                    drawLine(wells[i].copy(alpha = 0.70f), Offset(p.x, p.y), Offset(p.x, p.y + h * 0.025f), strokeWidth = 1.4f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE67E22), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF5EFE6),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFE67E22),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD8D2C8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE67E22).copy(alpha = 0.65f),
            layout = SignatureLayout.STANDARD
        )
        // ═══ ARTWORKS — museum gallery wall: spotlit framed paintings ═══
        cat == "ARTWORKS" -> SignatureDesign(
            bg = Color(0xFFECE9E4), cornerRadius = 6f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF2F0EC), Color(0xFFECE9E4), Color(0xFFD8D4CC))), size = Size(w, h))
                // Three spotlight cones from above onto the paintings
                listOf(w * 0.24f, w * 0.60f, w * 0.88f).forEach { cx ->
                    val cone = Path().apply { moveTo(cx, 0f); lineTo(cx - w * 0.10f, h * 0.52f); lineTo(cx + w * 0.10f, h * 0.52f); close() }
                    drawPath(cone, Color.White.copy(alpha = 0.12f))
                    drawCircle(brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)), radius = w * 0.08f, center = Offset(cx, h * 0.56f))
                }
                // Three framed paintings high on the wall — STANDARD title
                // column (top-left) sits below the left frame, not on it
                // Frame 1 (left, high)
                drawRect(Color(0xFF6A665E), Offset(w * 0.06f, h * 0.10f), Size(w * 0.24f, h * 0.34f))
                drawRect(Color(0xFFC0392B), Offset(w * 0.08f, h * 0.12f), Size(w * 0.20f, h * 0.30f))
                drawRect(Color(0xFF1A1A2E).copy(alpha = 0.40f), Offset(w * 0.08f, h * 0.12f), Size(w * 0.20f, h * 0.10f))
                // Frame 2 (center, lower)
                drawRect(Color(0xFF6A665E), Offset(w * 0.40f, h * 0.16f), Size(w * 0.22f, h * 0.30f))
                drawRect(Color(0xFF2980B9), Offset(w * 0.42f, h * 0.18f), Size(w * 0.18f, h * 0.26f))
                drawRect(Color(0xFFF2C879).copy(alpha = 0.50f), Offset(w * 0.42f, h * 0.18f), Size(w * 0.18f, h * 0.09f))
                // Frame 3 (right, tall, lower)
                drawRect(Color(0xFF6A665E), Offset(w * 0.72f, h * 0.20f), Size(w * 0.22f, h * 0.36f))
                drawRect(Color(0xFF27AE60), Offset(w * 0.74f, h * 0.22f), Size(w * 0.18f, h * 0.32f))
                drawRect(Color(0xFF8E44AD).copy(alpha = 0.45f), Offset(w * 0.74f, h * 0.22f), Size(w * 0.18f, h * 0.12f))
                // Museum bench silhouette bottom — small, clear of the body text
                drawRect(Color(0xFF4A463E).copy(alpha = 0.22f), Offset(w * 0.28f, h * 0.88f), Size(w * 0.44f, h * 0.035f))
                drawLine(Color(0xFF4A463E).copy(alpha = 0.18f), Offset(w * 0.32f, h * 0.88f), Offset(w * 0.32f, h * 0.95f), strokeWidth = 1.5f)
                drawLine(Color(0xFF4A463E).copy(alpha = 0.18f), Offset(w * 0.68f, h * 0.88f), Offset(w * 0.68f, h * 0.95f), strokeWidth = 1.5f)
            },
            padding = PaddingValues(horizontal = 26.dp, vertical = 24.dp), badgeColor = Color(0xFF4A463E), badgeInk = Color.White,
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.5.sp, titleTopSpacer = 16.dp,
            titleFont = GeomFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF26231E),
            metaSpacer = 6.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF7A766E),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF4A463E).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8A867E),
            layout = SignatureLayout.STANDARD
        )
        // ═══ SCIENTISTS — molecule, beaker, blueprint grid ═══
        cat == "SCIENTISTS" -> SignatureDesign(
            bg = Color(0xFF0E1B2C), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Blueprint grid — finer + a subtle corner registration mark
                for (i in 0 until 16) { drawLine(Color(0xFF2A4A6A).copy(alpha = 0.25f), Offset(i * w * 0.065f, 0f), Offset(i * w * 0.065f, h), strokeWidth = 0.3f) }
                for (i in 0 until 12) { drawLine(Color(0xFF2A4A6A).copy(alpha = 0.25f), Offset(0f, i * h * 0.09f), Offset(w, i * h * 0.09f), strokeWidth = 0.3f) }
                // Molecule top-right — clearer double bond + labelled atoms
                val mcx = w * 0.78f; val mcy = h * 0.16f
                drawLine(Color(0xFF5FD4C8).copy(alpha = 0.60f), Offset(mcx, mcy), Offset(mcx + w * 0.10f, mcy + h * 0.08f), strokeWidth = 1.4f)
                drawLine(Color(0xFF5FD4C8).copy(alpha = 0.60f), Offset(mcx, mcy), Offset(mcx - w * 0.06f, mcy + h * 0.10f), strokeWidth = 1.4f)
                drawLine(Color(0xFF5FD4C8).copy(alpha = 0.60f), Offset(mcx, mcy), Offset(mcx + w * 0.02f, mcy - h * 0.08f), strokeWidth = 1.4f)
                // Double bond to the right atom
                drawLine(Color(0xFF5FD4C8).copy(alpha = 0.35f), Offset(mcx + w * 0.006f, mcy - h * 0.006f), Offset(mcx + w * 0.096f, mcy + h * 0.074f), strokeWidth = 0.8f)
                drawCircle(Color(0xFF5FD4C8).copy(alpha = 0.75f), 4.2f, Offset(mcx, mcy))
                drawCircle(Color(0xFF3A6A8A).copy(alpha = 0.75f), 3.2f, Offset(mcx + w * 0.10f, mcy + h * 0.08f))
                drawCircle(Color(0xFF8FE0D8).copy(alpha = 0.75f), 2.8f, Offset(mcx - w * 0.06f, mcy + h * 0.10f))
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.85f), 2.6f, Offset(mcx + w * 0.02f, mcy - h * 0.08f))
                // Test-tube rack — two tubes with liquid + bubbles
                drawRect(Color(0xFF2A4A6A).copy(alpha = 0.35f), Offset(w * 0.42f, h * 0.34f), Size(w * 0.20f, h * 0.045f))
                drawRect(Color(0xFF2A4A6A).copy(alpha = 0.35f), Offset(w * 0.42f, h * 0.52f), Size(w * 0.20f, h * 0.02f))
                listOf(0.46f, 0.54f).forEachIndexed { i, tx ->
                    drawLine(Color(0xFFB8D0CC).copy(alpha = 0.60f), Offset(w * tx, h * 0.20f), Offset(w * tx, h * 0.55f), strokeWidth = 1.2f)
                    drawRect(if (i == 0) Color(0xFF5FD4C8).copy(alpha = 0.30f) else Color(0xFFF2C879).copy(alpha = 0.30f), Offset(w * tx - w * 0.008f, h * 0.44f), Size(w * 0.016f, h * 0.10f))
                }
                drawCircle(Color(0xFF8FE0D8).copy(alpha = 0.45f), 1.6f, Offset(w * 0.46f, h * 0.40f))
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.45f), 1.6f, Offset(w * 0.54f, h * 0.42f))
                // Erlenmeyer flask with graduated marks + bubbling liquid
                val bk = Path().apply {
                    moveTo(w * 0.16f, h * 0.46f); lineTo(w * 0.22f, h * 0.80f)
                    quadraticBezierTo(w * 0.24f, h * 0.88f, w * 0.32f, h * 0.86f)
                    quadraticBezierTo(w * 0.38f, h * 0.84f, w * 0.36f, h * 0.78f)
                    lineTo(w * 0.30f, h * 0.46f); close()
                }
                drawPath(bk, Color(0xFF5FD4C8).copy(alpha = 0.35f), style = Stroke(1.4f))
                drawRect(Color(0xFF5FD4C8).copy(alpha = 0.25f), Offset(w * 0.23f, h * 0.68f), Size(w * 0.11f, h * 0.15f))
                // Graduated marks on the flask
                drawLine(Color(0xFF5FD4C8).copy(alpha = 0.30f), Offset(w * 0.235f, h * 0.62f), Offset(w * 0.255f, h * 0.62f), strokeWidth = 0.7f)
                drawLine(Color(0xFF5FD4C8).copy(alpha = 0.30f), Offset(w * 0.235f, h * 0.66f), Offset(w * 0.255f, h * 0.66f), strokeWidth = 0.7f)
                for (i in 0 until 5) { drawCircle(Color(0xFF8FE0D8).copy(alpha = 0.50f), 1.8f + i * 0.25f, Offset(w * 0.26f + i * 0.010f * w, h * 0.62f - i * h * 0.04f)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF5FD4C8), badgeInk = Color(0xFF0E1B2C),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE0F2F0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF5FD4C8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB8D0CC).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF5FD4C8).copy(alpha = 0.70f),
            layout = SignatureLayout.SIDE
        )
        // ═══ DISCOVERIES — compass rose, dotted trail, contour map ═══
        cat == "DISCOVERIES" -> SignatureDesign(
            bg = Color(0xFF1E150C), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Warm parchment-brown gradient + sunburst rays from top-right
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1D0E), Color(0xFF1E150C), Color(0xFF120B05))), size = Size(w, h))
                listOf(0f, 30f, 60f, 90f, 120f, 150f).forEach { ang ->
                    val a = Math.toRadians(ang.toDouble()).toFloat()
                    drawLine(Color(0xFFD9A05B).copy(alpha = 0.10f), Offset(w * 0.92f, h * 0.10f), Offset(w * 0.92f + kotlin.math.cos(a) * w * 0.16f, h * 0.10f + kotlin.math.sin(a) * h * 0.16f), strokeWidth = 1.2f)
                }
                // Contour lines — tighter, more organic
                listOf(0.30f, 0.42f, 0.54f, 0.66f).forEachIndexed { i, _ ->
                    val contour = Path().apply {
                        moveTo(w * 0.10f, h * 0.52f)
                        quadraticBezierTo(w * 0.25f, h * 0.30f, w * 0.42f, h * 0.52f)
                        quadraticBezierTo(w * 0.60f, h * 0.74f, w * 0.80f, h * 0.50f)
                    }
                    drawPath(contour, Color(0xFFD9A05B).copy(alpha = 0.15f - i * 0.025f), style = Stroke(0.9f))
                }
                // Dotted trail winding to the X
                for (i in 0 until 16) {
                    val t = i / 15f
                    val x = w * 0.12f + t * w * 0.72f
                    val y = h * 0.82f - t * h * 0.30f + kotlin.math.sin(t * 3.14f).toFloat() * h * 0.03f
                    drawCircle(Color(0xFFE8B86D).copy(alpha = 0.60f), 1.9f, Offset(x, y))
                }
                // X marks the spot — bigger, with a sparkle
                drawLine(Color(0xFFE8B86D).copy(alpha = 0.85f), Offset(w * 0.78f, h * 0.44f), Offset(w * 0.90f, h * 0.56f), strokeWidth = 2.2f)
                drawLine(Color(0xFFE8B86D).copy(alpha = 0.85f), Offset(w * 0.90f, h * 0.44f), Offset(w * 0.78f, h * 0.56f), strokeWidth = 2.2f)
                drawCircle(Color(0xFFFFE08A).copy(alpha = 0.60f), 1.6f, Offset(w * 0.86f, h * 0.38f))
                // Compass rose top-left — bigger, 8-point with a ring
                val cx = w * 0.16f; val cy = h * 0.16f; val cr = w * 0.085f
                drawCircle(Color(0xFFD9A05B).copy(alpha = 0.20f), cr * 1.15f, Offset(cx, cy), style = Stroke(0.8f))
                for (i in 0 until 4) {
                    val a = Math.toRadians(i * 90.0).toFloat()
                    drawLine(Color(0xFFD9A05B).copy(alpha = 0.60f), Offset(cx, cy), Offset(cx + kotlin.math.cos(a) * cr, cy + kotlin.math.sin(a) * cr), strokeWidth = 1.8f)
                    val b = Math.toRadians(i * 90.0 + 45.0).toFloat()
                    drawLine(Color(0xFFD9A05B).copy(alpha = 0.30f), Offset(cx, cy), Offset(cx + kotlin.math.cos(b) * cr * 0.6f, cy + kotlin.math.sin(b) * cr * 0.6f), strokeWidth = 0.9f)
                }
                drawCircle(Color(0xFFD9A05B).copy(alpha = 0.70f), 2.2f, Offset(cx, cy))
                // N-S-E-W ticks on the ring
                listOf(0f, 90f, 180f, 270f).forEach { ang ->
                    val a = Math.toRadians(ang.toDouble()).toFloat()
                    drawCircle(Color(0xFFE8B86D).copy(alpha = 0.50f), 1.2f, Offset(cx + kotlin.math.cos(a) * cr * 1.15f, cy + kotlin.math.sin(a) * cr * 1.15f))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD9A05B), badgeInk = Color(0xFF1E150C),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = PirataOneFontFamily, titleSize = 34.sp, titleLineHeight = 38.sp, titleColor = Color(0xFFF5E3C0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFD9A05B),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0CEB0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD9A05B).copy(alpha = 0.70f),
            layout = SignatureLayout.POSTER
        )
        // ═══ SERIES — cinematic streaming poster: play motif, film strip, glow ═══
        cat == "SERIES" -> SignatureDesign(
            bg = Color(0xFF0F0B14), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Deep midnight gradient
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1220), Color(0xFF0F0B14), Color(0xFF06030A))), size = Size(w, h))
                // Soft amber glow center-right (behind the play button)
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE8A040).copy(alpha = 0.18f), Color.Transparent)), radius = w * 0.35f, center = Offset(w * 0.76f, h * 0.30f))
                // Play button triangle — clean, iconic
                val pcx = w * 0.76f; val pcy = h * 0.30f; val pr = w * 0.06f
                drawCircle(Color.White.copy(alpha = 0.08f), pr * 1.6f, Offset(pcx, pcy))
                drawCircle(Color.White.copy(alpha = 0.12f), pr, Offset(pcx, pcy), style = Stroke(1.5f))
                drawPath(Path().apply {
                    moveTo(pcx - pr * 0.35f, pcy - pr * 0.5f)
                    lineTo(pcx - pr * 0.35f, pcy + pr * 0.5f)
                    lineTo(pcx + pr * 0.55f, pcy)
                    close()
                }, Color.White.copy(alpha = 0.22f))
                // Film strip down the left edge — thin, elegant
                drawRect(Color(0xFFC8C4BC).copy(alpha = 0.18f), Offset(w * 0.015f, h * 0.04f), Size(w * 0.012f, h * 0.92f))
                for (i in 0 until 10) {
                    val y = h * 0.06f + i * h * 0.092f
                    drawRect(Color(0xFF0F0B14), Offset(w * 0.017f, y), Size(w * 0.008f, h * 0.035f))
                }
                // Subtle horizontal scanlines (TV screen feel)
                for (i in 0 until 8) { drawLine(Color.White.copy(alpha = 0.015f), Offset(0f, h * 0.15f + i * h * 0.10f), Offset(w, h * 0.15f + i * h * 0.10f), strokeWidth = 0.5f) }
                // Episode tally dots — small, bottom-right, like a progress tracker
                for (i in 0 until 6) { drawCircle(if (i < 3) Color(0xFFE8A040).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f), 1.8f, Offset(w * 0.50f + i * w * 0.05f, h * 0.88f)) }
                drawLine(Color(0xFFE8A040).copy(alpha = 0.15f), Offset(w * 0.48f, h * 0.88f), Offset(w * 0.82f, h * 0.88f), strokeWidth = 0.5f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8A040), badgeInk = Color(0xFF0F0B14),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF0E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8A040),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C4D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8A040).copy(alpha = 0.70f)
        )
        // ═══ ANIME — sakura petals, sunburst, lens flare ═══
        cat == "ANIME" -> SignatureDesign(
            bg = Color(0xFF2B0F45), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Sunburst from top-left
                for (i in 0 until 18) {
                    val a = Math.toRadians(200.0 + i * 8.0).toFloat()
                    drawLine(Color(0xFFFFD9A0).copy(alpha = 0.10f), Offset(0f, 0f), Offset(kotlin.math.cos(a) * w * 1.3f, kotlin.math.sin(a) * h * 1.3f), strokeWidth = 1.2f)
                }
                // Falling sakura petals
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 16) {
                    val t = i / 15f
                    val x = ((s * (i+1) * 3571) % 10000) / 10000f * w
                    val y = h * 0.05f + t * h * 0.85f
                    drawOval(Color(0xFFFF9EC6).copy(alpha = 0.55f), Offset(x, y), Size(w * 0.025f, h * 0.012f))
                }
                // Lens flare
                drawCircle(Color(0xFFFF5FA2).copy(alpha = 0.30f), w * 0.05f, Offset(w * 0.84f, h * 0.20f))
                drawLine(Color(0xFFFF9EC6).copy(alpha = 0.45f), Offset(w * 0.80f, h * 0.20f), Offset(w * 0.88f, h * 0.20f), strokeWidth = 0.8f)
                drawLine(Color(0xFFFF9EC6).copy(alpha = 0.45f), Offset(w * 0.84f, h * 0.16f), Offset(w * 0.84f, h * 0.24f), strokeWidth = 0.8f)
                // Sparkle
                drawCircle(Color(0xFFFFD9A0).copy(alpha = 0.6f), 2f, Offset(w * 0.16f, h * 0.80f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF5FA2), badgeInk = Color(0xFF2B0F45),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFFFE8F2),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF9EC6),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFE0C4D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF5FA2).copy(alpha = 0.70f),
            layout = SignatureLayout.OVERLAY
        )
        // ═══ MANGA — black & white, bold speed lines, burst, screentone ═══
        cat == "MANGA" -> SignatureDesign(
            bg = Color(0xFF141414), cornerRadius = 4f,
            drawBackground = { w, h ->
                // Bold speed lines from top-right
                for (i in 0 until 20) {
                    val a = Math.toRadians(115.0 + i * 4.0).toFloat()
                    val thick = if (i % 5 == 0) 3f else 1.2f
                    drawLine(Color.White.copy(alpha = if (i % 5 == 0) 0.22f else 0.10f), Offset(w * 0.95f, h * 0.03f), Offset(w * 0.95f + kotlin.math.cos(a) * w * 1.2f, h * 0.03f + kotlin.math.sin(a) * h * 1.2f), strokeWidth = thick)
                }
                // Burst star center
                val bcx = w * 0.30f; val bcy = h * 0.30f
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0).toFloat()
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(bcx, bcy), Offset(bcx + kotlin.math.cos(a) * w * 0.09f, bcy + kotlin.math.sin(a) * w * 0.09f), strokeWidth = 2.2f)
                }
                drawCircle(Color.White.copy(alpha = 0.50f), 3f, Offset(bcx, bcy))
                // Red accent slash
                drawLine(Color(0xFFE23B3B).copy(alpha = 0.60f), Offset(w * 0.70f, h * 0.78f), Offset(w * 0.92f, h * 0.60f), strokeWidth = 4f)
                // Screentone halftone dots bottom-left
                for (i in 0 until 10) {
                    for (j in 0 until 10) {
                        val x = w * 0.03f + i * w * 0.030f
                        val y = h * 0.62f + j * h * 0.028f
                        if ((i + j) % 2 == 0) drawCircle(Color.White.copy(alpha = 0.20f), 1.4f, Offset(x, y))
                    }
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE23B3B), badgeInk = Color.White,
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color.White,
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE23B3B),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFD8D8D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE23B3B).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ MANHWA — pastel scene: arch, heart, sparkles, bokeh ═══
        cat == "MANHWA" -> SignatureDesign(
            bg = Color(0xFFF4ECFA), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Soft pastel gradient
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFBF5FE), Color(0xFFF4ECFA), Color(0xFFE8DCF2))), size = Size(w, h))
                // Bokeh blobs scattered
                drawCircle(Color(0xFFC9B8F0).copy(alpha = 0.30f), w * 0.20f, Offset(w * 0.18f, h * 0.22f))
                drawCircle(Color(0xFFFFC9B0).copy(alpha = 0.26f), w * 0.16f, Offset(w * 0.84f, h * 0.28f))
                drawCircle(Color(0xFFA8E6CF).copy(alpha = 0.26f), w * 0.18f, Offset(w * 0.74f, h * 0.80f))
                drawCircle(Color(0xFFFFE0A8).copy(alpha = 0.26f), w * 0.14f, Offset(w * 0.20f, h * 0.84f))
                // Rounded arch — the romance frame
                val ax = w * 0.30f; val aw = w * 0.34f; val ah = h * 0.40f
                drawArc(Color(0xFF8E7CC3).copy(alpha = 0.45f), 0f, 180f, false, Offset(ax, h * 0.30f), Size(aw, ah), style = Stroke(2.2f))
                drawLine(Color(0xFF8E7CC3).copy(alpha = 0.30f), Offset(ax, h * 0.70f), Offset(ax, h * 0.86f), strokeWidth = 2f)
                drawLine(Color(0xFF8E7CC3).copy(alpha = 0.30f), Offset(ax + aw, h * 0.70f), Offset(ax + aw, h * 0.86f), strokeWidth = 2f)
                // Heart floating inside the arch
                val hx = ax + aw * 0.5f; val hy = h * 0.48f; val hr = w * 0.030f
                drawPath(Path().apply {
                    moveTo(hx, hy + hr)
                    cubicTo(hx - hr * 1.2f, hy, hx - hr * 1.2f, hy - hr * 0.7f, hx - hr * 0.45f, hy - hr * 0.7f)
                    cubicTo(hx - hr * 0.1f, hy - hr * 0.7f, hx, hy - hr * 0.3f, hx, hy - hr * 0.1f)
                    cubicTo(hx, hy - hr * 0.3f, hx + hr * 0.1f, hy - hr * 0.7f, hx + hr * 0.45f, hy - hr * 0.7f)
                    cubicTo(hx + hr * 1.2f, hy - hr * 0.7f, hx + hr * 1.2f, hy, hx, hy + hr)
                    close()
                }, Color(0xFFFF9AC0).copy(alpha = 0.75f))
                // Sparkles orbiting the heart
                listOf(Offset(ax + aw * 0.5f, h * 0.30f), Offset(ax - w * 0.02f, h * 0.50f), Offset(ax + aw + w * 0.02f, h * 0.52f), Offset(w * 0.88f, h * 0.60f)).forEach { p ->
                    drawLine(Color(0xFF8E7CC3).copy(alpha = 0.50f), Offset(p.x - 4f, p.y), Offset(p.x + 4f, p.y), strokeWidth = 1f)
                    drawLine(Color(0xFF8E7CC3).copy(alpha = 0.50f), Offset(p.x, p.y - 4f), Offset(p.x, p.y + 4f), strokeWidth = 1f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8E7CC3), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2C55),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8E7CC3),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFF5A4A75).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF8E7CC3).copy(alpha = 0.65f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ GAMES — neon arcade: glowing D-pad, A/B buttons, coin, scanlines ═══
        cat == "GAMES" -> SignatureDesign(
            bg = Color(0xFF0A0A14), cornerRadius = 4f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0F0F1E), Color(0xFF0A0A14), Color(0xFF050508))), size = Size(w, h))
                // Neon glow behind the D-pad and coin
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF00CC66).copy(alpha = 0.18f), Color.Transparent)), radius = w * 0.30f, center = Offset(w * 0.30f, h * 0.70f))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF00CCFF).copy(alpha = 0.14f), Color.Transparent)), radius = w * 0.25f, center = Offset(w * 0.78f, h * 0.22f))
                // D-pad cross — iconic plus shape
                val dpx = w * 0.30f; val dpy = h * 0.70f; val dps = w * 0.05f
                drawRect(Color(0xFF00FF88).copy(alpha = 0.45f), Offset(dpx - dps * 0.3f, dpy - dps), Size(dps * 0.6f, dps * 2f))
                drawRect(Color(0xFF00FF88).copy(alpha = 0.45f), Offset(dpx - dps, dpy - dps * 0.3f), Size(dps * 2f, dps * 0.6f))
                drawRect(Color(0xFF0A0A14).copy(alpha = 0.6f), Offset(dpx - dps * 0.12f, dpy - dps * 0.12f), Size(dps * 0.24f, dps * 0.24f))
                // A/B buttons
                drawCircle(Color(0xFFFF3A6B).copy(alpha = 0.55f), dps * 0.4f, Offset(dpx + dps * 1.6f, dpy - dps * 0.3f))
                drawCircle(Color(0xFF00CCFF).copy(alpha = 0.55f), dps * 0.4f, Offset(dpx + dps * 2.2f, dpy + dps * 0.3f))
                // Gold coin top-right
                drawCircle(Color(0xFFFFD700).copy(alpha = 0.20f), w * 0.10f, Offset(w * 0.78f, h * 0.22f))
                drawCircle(Color(0xFFFFD700).copy(alpha = 0.55f), w * 0.06f, Offset(w * 0.78f, h * 0.22f), style = Stroke(2f))
                drawCircle(Color(0xFFFFD700).copy(alpha = 0.40f), w * 0.035f, Offset(w * 0.78f, h * 0.22f), style = Stroke(1.5f))
                // Pixel stars scattered
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 20) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    val sz = 1.5f + ((s * (i+1) * 3571) % 100) / 100f * 1.5f
                    drawRect(Color(0xFF00FF88).copy(alpha = 0.25f + ((s * (i+1) * 4201) % 100) / 100f * 0.15f), Offset(x, y), Size(sz, sz))
                }
                // CRT scanlines
                for (i in 0 until 30) { drawLine(Color(0xFF00FF88).copy(alpha = 0.020f), Offset(0f, i * h / 30f), Offset(w, i * h / 30f), strokeWidth = 0.5f) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF00CC66), badgeInk = Color(0xFF0A0A14),
            badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp,
            badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp,
            titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp,
            titleLineHeight = 34.sp, titleColor = Color(0xFF00FF88),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF00CC66),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFC0D0C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF00CC66).copy(alpha = 0.65f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ MYTHOLOGY — gold on dark marble, meander border, columns ═══
        cat == "MYTHOLOGY" -> SignatureDesign(
            bg = Color(0xFF17141C), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Greek key (meander) borders top & bottom
                for (e in listOf(0f, h * 0.94f)) {
                    for (i in 0 until 9) {
                        val x = w * 0.06f + i * w * 0.10f
                        drawLine(Color(0xFFD8B25C).copy(alpha = 0.35f), Offset(x, e), Offset(x + w * 0.04f, e), strokeWidth = 1f)
                        drawLine(Color(0xFFD8B25C).copy(alpha = 0.35f), Offset(x + w * 0.04f, e), Offset(x + w * 0.04f, e + h * 0.03f), strokeWidth = 1f)
                        drawLine(Color(0xFFD8B25C).copy(alpha = 0.35f), Offset(x, e + h * 0.03f), Offset(x + w * 0.02f, e + h * 0.03f), strokeWidth = 1f)
                    }
                }
                // Temple columns
                for (cx in listOf(w * 0.12f, w * 0.88f)) {
                    drawRect(Color(0xFFE8E2D4).copy(alpha = 0.14f), Offset(cx - w * 0.03f, h * 0.12f), Size(w * 0.06f, h * 0.70f))
                    drawRect(Color(0xFFE8E2D4).copy(alpha = 0.20f), Offset(cx - w * 0.045f, h * 0.10f), Size(w * 0.09f, h * 0.035f))
                    drawRect(Color(0xFFE8E2D4).copy(alpha = 0.20f), Offset(cx - w * 0.045f, h * 0.81f), Size(w * 0.09f, h * 0.025f))
                }
                // Laurel wreath bottom-center
                val wcx = w * 0.50f; val wcy = h * 0.86f; val wr = w * 0.07f
                for (i in 0 until 14) {
                    val a = Math.toRadians(i * 25.7).toFloat()
                    drawOval(Color(0xFF9AB25C).copy(alpha = 0.35f), Offset(wcx + kotlin.math.cos(a) * wr - 2f, wcy + kotlin.math.sin(a) * wr * 0.6f - 3f), Size(4f, 6f))
                }
                // Gold rays top-center
                for (i in 0 until 10) {
                    val a = Math.toRadians(170.0 + i * 4.0).toFloat()
                    drawLine(Color(0xFFD8B25C).copy(alpha = 0.10f), Offset(w * 0.50f, h * 0.06f), Offset(w * 0.50f + kotlin.math.cos(a) * w * 0.40f, h * 0.06f + kotlin.math.sin(a) * h * 0.30f), strokeWidth = 0.8f)
                }
            },
            padding = PaddingValues(horizontal = 24.dp, vertical = 22.dp), badgeColor = Color(0xFFD8B25C), badgeInk = Color(0xFF17141C),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF0EAD8),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFD8B25C),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD8D0BC).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD8B25C).copy(alpha = 0.60f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ SPORTS — arena floodlights, field stripes, trophy ═══
        cat == "SPORTS" -> SignatureDesign(
            bg = Color(0xFF0C2313), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Floodlight cones
                listOf(Offset(w * 0.22f, 0f), Offset(w * 0.78f, 0f)).forEach { c ->
                    val cone = Path().apply {
                        moveTo(c.x - w * 0.05f, 0f); lineTo(c.x - w * 0.16f, h * 0.34f)
                        lineTo(c.x + w * 0.16f, h * 0.34f); lineTo(c.x + w * 0.05f, 0f); close()
                    }
                    drawPath(cone, Color(0xFFFFF3D9).copy(alpha = 0.05f))
                }
                // Field stripes
                for (i in 0 until 6) { drawRect(Color(0xFF3FBF5A).copy(alpha = 0.10f), Offset(w * 0.06f, h * 0.60f + i * h * 0.055f), Size(w * 0.88f, h * 0.028f)) }
                // Center circle + line
                drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.20f), w * 0.13f, Offset(w * 0.50f, h * 0.72f), style = Stroke(1.5f))
                drawLine(Color(0xFFFFFFFF).copy(alpha = 0.22f), Offset(w * 0.06f, h * 0.72f), Offset(w * 0.94f, h * 0.72f), strokeWidth = 1f)
                // Motion streak
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.30f), Offset(w * 0.10f, h * 0.18f), Offset(w * 0.36f, h * 0.30f), strokeWidth = 2f)
                // Trophy silhouette top-right
                val tx = w * 0.84f; val ty = h * 0.16f
                drawArc(Color(0xFFE8C05C).copy(alpha = 0.55f), 180f, 180f, false, Offset(tx - w * 0.045f, ty), Size(w * 0.09f, h * 0.10f), style = Stroke(1.8f))
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.55f), Offset(tx, ty + h * 0.05f), Offset(tx, ty + h * 0.12f), strokeWidth = 1.8f)
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.55f), Offset(tx - w * 0.03f, ty + h * 0.12f), Offset(tx + w * 0.03f, ty + h * 0.12f), strokeWidth = 1.8f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF3FBF5A), badgeInk = Color(0xFF0C2313),
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 16.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFEAF5EA),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF3FBF5A),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFBFD8C4).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF3FBF5A).copy(alpha = 0.70f),
            layout = SignatureLayout.OVERLAY
        )
        // ═══ FOOD — overhead table: hero plate, steaming bowl, herbs ═══
        cat == "FOOD" -> SignatureDesign(
            bg = Color(0xFFFDF2E7), cornerRadius = 10f,
            drawBackground = { w, h ->
                // Warm cream gradient — a linen table
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFFBF2), Color(0xFFFDF2E7), Color(0xFFF2E2CC))), size = Size(w, h))
                // Hero plate, centered-left — rim, well, food
                val pcx = w * 0.32f; val pcy = h * 0.46f
                drawCircle(Color(0xFFE8D9C8), w * 0.16f, Offset(pcx, pcy))
                drawCircle(Color(0xFFFDF2E7), w * 0.12f, Offset(pcx, pcy))
                drawCircle(Color(0xFFD96C4A).copy(alpha = 0.45f), w * 0.085f, Offset(pcx, pcy))
                drawCircle(Color(0xFFE8A24F).copy(alpha = 0.55f), w * 0.055f, Offset(pcx - w * 0.02f, pcy - w * 0.015f))
                drawCircle(Color(0xFF4E8C4E).copy(alpha = 0.65f), w * 0.016f, Offset(pcx + w * 0.035f, pcy + w * 0.03f))
                drawCircle(Color(0xFFE8A24F).copy(alpha = 0.65f), w * 0.012f, Offset(pcx - w * 0.04f, pcy + w * 0.035f))
                // Steaming bowl, right — on the same table
                val bcx = w * 0.74f; val bcy = h * 0.50f; val br = w * 0.11f
                drawCircle(Color(0xFFE4572E).copy(alpha = 0.90f), br, Offset(bcx, bcy))
                drawCircle(Color(0xFFFDF2E7).copy(alpha = 0.9f), br * 0.72f, Offset(bcx, bcy))
                drawCircle(Color(0xFFE8A24F).copy(alpha = 0.60f), br * 0.52f, Offset(bcx, bcy))
                // Steam wisps rising from the bowl
                for (i in 0 until 3) {
                    val sx = bcx + (i - 1) * w * 0.03f
                    val steam = Path().apply { moveTo(sx, bcy - br); cubicTo(sx + 4f, bcy - br - h * 0.06f, sx - 4f, bcy - br - h * 0.10f, sx + 2f, bcy - br - h * 0.16f) }
                    drawPath(steam, Color(0xFF8A6A55).copy(alpha = 0.30f), style = Stroke(1.1f))
                }
                // Basil leaves tucked beside the plate
                listOf(Offset(w * 0.14f, h * 0.28f), Offset(w * 0.10f, h * 0.64f), Offset(w * 0.52f, h * 0.72f)).forEach { p ->
                    drawOval(Color(0xFF4E8C4E).copy(alpha = 0.55f), Offset(p.x - 5f, p.y - 3.5f), Size(10f, 7f))
                    drawLine(Color(0xFF2E6A3A).copy(alpha = 0.5f), Offset(p.x - 2f, p.y), Offset(p.x + 2f, p.y), strokeWidth = 0.7f)
                }
                // Crumb dots scattered on the table
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 10) {
                    val x = w * 0.06f + ((s * (i+1) * 3571) % 100) / 100f * w * 0.88f
                    val y = h * 0.80f + ((s * (i+1) * 4201) % 100) / 100f * h * 0.12f
                    drawCircle(Color(0xFFD9B45B).copy(alpha = 0.40f), 1.4f, Offset(x, y))
                }
            },
            padding = PaddingValues(horizontal = 28.dp, vertical = 24.dp), badgeColor = Color(0xFFE4572E), badgeInk = Color.White,
            badgeRadius = 16.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF4A2410),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE4572E),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF6A3A22).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE4572E).copy(alpha = 0.60f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ INTERNET — globe, network nodes, browser bar ═══
        cat == "INTERNET" -> SignatureDesign(
            bg = Color(0xFF081424), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Browser bar
                drawRoundRect(Color(0xFF12263E).copy(alpha = 0.90f), Offset(w * 0.10f, h * 0.08f), Size(w * 0.80f, h * 0.07f), CornerRadius(3f))
                drawCircle(Color(0xFF4E8BFF).copy(alpha = 0.8f), 2f, Offset(w * 0.14f, h * 0.115f))
                drawCircle(Color(0xFF3FD0E8).copy(alpha = 0.8f), 2f, Offset(w * 0.17f, h * 0.115f))
                drawLine(Color(0xFF3FD0E8).copy(alpha = 0.5f), Offset(w * 0.20f, h * 0.115f), Offset(w * 0.84f, h * 0.115f), strokeWidth = 1.5f)
                // Globe wireframe
                val gx = w * 0.30f; val gy = h * 0.55f; val gr = w * 0.14f
                drawCircle(Color(0xFF4E8BFF).copy(alpha = 0.30f), gr, Offset(gx, gy), style = Stroke(1.2f))
                drawLine(Color(0xFF4E8BFF).copy(alpha = 0.25f), Offset(gx - gr, gy), Offset(gx + gr, gy), strokeWidth = 0.8f)
                drawLine(Color(0xFF4E8BFF).copy(alpha = 0.25f), Offset(gx, gy - gr), Offset(gx, gy + gr), strokeWidth = 0.8f)
                drawOval(Color(0xFF4E8BFF).copy(alpha = 0.22f), Offset(gx - gr * 0.5f, gy - gr), Size(gr, gr * 2f), style = Stroke(0.8f))
                drawOval(Color(0xFF4E8BFF).copy(alpha = 0.22f), Offset(gx - gr, gy - gr), Size(gr * 2f, gr * 2f), style = Stroke(0.8f))
                // Network nodes
                listOf(Offset(w * 0.78f, h * 0.40f), Offset(w * 0.88f, h * 0.58f), Offset(w * 0.70f, h * 0.66f), Offset(w * 0.62f, h * 0.34f)).forEach { p ->
                    drawCircle(Color(0xFF3FD0E8).copy(alpha = 0.60f), 2.2f, p)
                    drawLine(Color(0xFF3FD0E8).copy(alpha = 0.25f), Offset(gx, gy), p, strokeWidth = 0.6f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4E8BFF), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD8E8FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4E8BFF),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFA8C4E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF4E8BFF).copy(alpha = 0.70f),
            layout = SignatureLayout.SIDE
        )
        // ═══ BIOLOGY — luminous DNA helix, cells, chromosomes ═══
        cat == "BIOLOGY" -> SignatureDesign(
            bg = Color(0xFF0C1F14), cornerRadius = 8f,
            drawBackground = { w, h ->
                // DNA double helix — hero, centered-right
                for (i in 0 until 42) {
                    val t = i / 41f
                    val y = h * 0.06f + t * h * 0.80f
                    val x1 = w * 0.72f + kotlin.math.sin(t * 6.28f * 2).toFloat() * w * 0.08f
                    val x2 = w * 0.72f - kotlin.math.sin(t * 6.28f * 2).toFloat() * w * 0.08f
                    drawCircle(Color(0xFF7FE8A0).copy(alpha = 0.80f), 2.8f, Offset(x1, y))
                    drawCircle(Color(0xFF3FA86A).copy(alpha = 0.80f), 2.4f, Offset(x2, y))
                    if (i % 2 == 0) drawLine(Color(0xFFE8D56B).copy(alpha = 0.35f), Offset(x1, y), Offset(x2, y), strokeWidth = 0.8f)
                }
                // Cell membrane circles
                for (i in 0 until 4) {
                    val cx = w * 0.10f + i * w * 0.09f
                    val cy = h * 0.16f + i * h * 0.14f
                    drawCircle(Color(0xFF7FE8A0).copy(alpha = 0.35f), w * 0.035f, Offset(cx, cy), style = Stroke(1.2f))
                    drawCircle(Color(0xFFE8D56B).copy(alpha = 0.40f), w * 0.012f, Offset(cx, cy))
                }
                // Chromosome X shapes
                listOf(Offset(w * 0.20f, h * 0.80f), Offset(w * 0.34f, h * 0.86f), Offset(w * 0.48f, h * 0.78f)).forEach { p ->
                    drawLine(Color(0xFF7FE8A0).copy(alpha = 0.40f), Offset(p.x - 4f, p.y - 5f), Offset(p.x + 4f, p.y + 5f), strokeWidth = 1.4f)
                    drawLine(Color(0xFF7FE8A0).copy(alpha = 0.40f), Offset(p.x + 4f, p.y - 5f), Offset(p.x - 4f, p.y + 5f), strokeWidth = 1.4f)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF7FE8A0), badgeInk = Color(0xFF0C1F14),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD8F5E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF7FE8A0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB8DCC4).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF7FE8A0).copy(alpha = 0.70f),
            layout = SignatureLayout.SIDE
        )
        // ═══ CHEMISTRY — connected hexagon lattice + water molecule ═══
        cat == "CHEMISTRY" -> SignatureDesign(
            bg = Color(0xFF0F1638), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Deep indigo-blue gradient
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E2A5E), Color(0xFF0F1638), Color(0xFF070B1E))), size = Size(w, h))
                // Cyan glow behind the lattice
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF3FD0E8).copy(alpha = 0.16f), Color.Transparent)), radius = w * 0.40f, center = Offset(w * 0.62f, h * 0.40f))
                // Hexagon ring helper
                fun hex(cx: Float, cy: Float, r: Float, stroke: Color, node: Color, doubleBond: Boolean = false) {
                    val pts = (0 until 6).map { k ->
                        val a = Math.toRadians((60.0 * k + 90.0)).toFloat()
                        Offset(cx + kotlin.math.cos(a) * r, cy + kotlin.math.sin(a) * r)
                    }
                    for (k in 0 until 6) drawLine(stroke, pts[k], pts[(k + 1) % 6], strokeWidth = 1.4f)
                    if (doubleBond) {
                        val mid = Offset((pts[0].x + pts[5].x) / 2, (pts[0].y + pts[5].y) / 2)
                        val nx = -(pts[5].y - pts[0].y); val ny = (pts[5].x - pts[0].x)
                        val len = kotlin.math.sqrt(nx * nx + ny * ny) + 0.0001f
                        val off = Offset(nx / len * r * 0.10f, ny / len * r * 0.10f)
                        drawLine(stroke.copy(alpha = stroke.alpha * 0.8f), Offset(mid.x - off.x, mid.y - off.y), Offset(mid.x + off.x, mid.y + off.y), strokeWidth = 1.0f)
                    }
                    pts.forEach { p -> drawCircle(node, r * 0.09f, p) }
                }
                // Honeycomb lattice on the right — rings share edges into one sheet
                val r = w * 0.034f
                val dx = r * 1.732f; val dy = r * 1.5f
                val lx = w * 0.56f; val ly = h * 0.28f
                for (row in 0 until 3) {
                    for (col in 0 until 3) {
                        val cx = lx + col * dx + (if (row % 2 == 1) dx * 0.5f else 0f)
                        val cy = ly + row * dy
                        val colr = if ((row + col) % 2 == 0) Color(0xFF3FD0E8) else Color(0xFF4FA8E8)
                        hex(cx, cy, r, colr.copy(alpha = 0.55f), colr.copy(alpha = 0.9f), doubleBond = (row + col) % 3 == 0)
                    }
                }
                // Hero benzene ring, left, with a soft glow
                val hx = w * 0.30f; val hy = h * 0.34f; val hr = w * 0.07f
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF3FD0E8).copy(alpha = 0.20f), Color.Transparent)), radius = w * 0.16f, center = Offset(hx, hy))
                hex(hx, hy, hr, Color(0xFF9FE8F8).copy(alpha = 0.85f), Color(0xFF3FD0E8), doubleBond = true)
                // Bond connecting the hero ring to the lattice
                val rightVertex = Offset(hx + hr * kotlin.math.cos(Math.toRadians(30.0).toFloat()), hy + hr * kotlin.math.sin(Math.toRadians(30.0).toFloat()))
                val leftLattice = Offset(lx + dx * 0.5f, ly + dy)
                drawLine(Color(0xFF3FD0E8).copy(alpha = 0.65f), rightVertex, leftLattice, strokeWidth = 1.4f)
                drawCircle(Color(0xFF3FD0E8).copy(alpha = 0.9f), w * 0.008f, leftLattice)
                // Water molecule, bottom-left — bonded atoms
                val ox = w * 0.20f; val oy = h * 0.76f
                drawCircle(Color(0xFFFF5FA2).copy(alpha = 0.85f), w * 0.020f, Offset(ox, oy))
                drawLine(Color(0xFF9FE0E8).copy(alpha = 0.6f), Offset(ox, oy), Offset(ox - w * 0.042f, oy - w * 0.032f), strokeWidth = 1.3f)
                drawLine(Color(0xFF9FE0E8).copy(alpha = 0.6f), Offset(ox, oy), Offset(ox + w * 0.042f, oy - w * 0.032f), strokeWidth = 1.3f)
                drawCircle(Color(0xFFB8F0F8).copy(alpha = 0.85f), w * 0.011f, Offset(ox - w * 0.042f, oy - w * 0.032f))
                drawCircle(Color(0xFFB8F0F8).copy(alpha = 0.85f), w * 0.011f, Offset(ox + w * 0.042f, oy - w * 0.032f))
                // Gentle vignette
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF04060C).copy(alpha = 0.5f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.9f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF3FD0E8), badgeInk = Color(0xFF0F1638),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE0F2F8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF3FD0E8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0C8E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF3FD0E8).copy(alpha = 0.70f)
        )
        // ═══ ANIMALS — forest canopy: paw trail, tree silhouette, moon glow ═══
        cat == "ANIMALS" -> SignatureDesign(
            bg = Color(0xFF142412), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E341C), Color(0xFF142412), Color(0xFF0A1208))), size = Size(w, h))
                // Moon glow top-right
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFE8B0).copy(alpha = 0.16f), Color.Transparent)), radius = w * 0.25f, center = Offset(w * 0.80f, h * 0.18f))
                drawCircle(Color(0xFFFFE8B0).copy(alpha = 0.35f), w * 0.055f, Offset(w * 0.80f, h * 0.18f))
                drawCircle(Color(0xFFFFE8B0).copy(alpha = 0.20f), w * 0.065f, Offset(w * 0.80f, h * 0.18f), style = Stroke(1f))
                // Large tree silhouette left — trunk + layered canopy
                drawLine(Color(0xFF2A1E12).copy(alpha = 0.65f), Offset(w * 0.14f, h * 0.85f), Offset(w * 0.14f, h * 0.42f), strokeWidth = 4f)
                // Branches
                drawLine(Color(0xFF2A1E12).copy(alpha = 0.45f), Offset(w * 0.14f, h * 0.50f), Offset(w * 0.06f, h * 0.40f), strokeWidth = 2f)
                drawLine(Color(0xFF2A1E12).copy(alpha = 0.40f), Offset(w * 0.14f, h * 0.46f), Offset(w * 0.22f, h * 0.36f), strokeWidth = 2f)
                // Canopy clusters
                drawCircle(Color(0xFF1A3A18).copy(alpha = 0.55f), w * 0.08f, Offset(w * 0.10f, h * 0.36f))
                drawCircle(Color(0xFF1A3A18).copy(alpha = 0.50f), w * 0.06f, Offset(w * 0.16f, h * 0.32f))
                drawCircle(Color(0xFF1A3A18).copy(alpha = 0.45f), w * 0.05f, Offset(w * 0.06f, h * 0.42f))
                drawCircle(Color(0xFF1A3A18).copy(alpha = 0.40f), w * 0.07f, Offset(w * 0.20f, h * 0.40f))
                // Paw print trail — w-relative, diagonal across lower-right
                listOf(Offset(w * 0.38f, h * 0.62f), Offset(w * 0.52f, h * 0.72f), Offset(w * 0.66f, h * 0.64f), Offset(w * 0.80f, h * 0.74f)).forEachIndexed { idx, p ->
                    val a = 0.50f - idx * 0.08f
                    drawCircle(Color(0xFFE0A458).copy(alpha = a), w * 0.012f, p)
                    listOf(-0.008f, -0.003f, 0.003f, 0.008f).forEach { ox -> drawCircle(Color(0xFFE0A458).copy(alpha = a * 0.8f), w * 0.006f, Offset(p.x + ox * w, p.y - w * 0.016f)) }
                }
                // Fireflies
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 12) {
                    val x = w * 0.30f + ((s * (i+1) * 7919) % 10000) / 10000f * w * 0.60f
                    val y = h * 0.20f + ((s * (i+1) * 6271) % 10000) / 10000f * h * 0.50f
                    drawCircle(Color(0xFFFFE8A0).copy(alpha = 0.45f), 1.5f, Offset(x, y))
                    drawCircle(Color(0xFFFFE8A0).copy(alpha = 0.12f), 3f, Offset(x, y))
                }
                // Grass blades bottom
                for (i in 0 until 18) {
                    val gx = w * 0.02f + i * w * 0.055f
                    drawPath(Path().apply { moveTo(gx, h); quadraticBezierTo(gx + 3f, h - h * 0.08f, gx - 2f, h - h * 0.14f) }, Color(0xFF3FBF5A).copy(alpha = 0.22f), style = Stroke(1f))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE0A458), badgeInk = Color(0xFF142412),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = GeomFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF0E8D0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE0A458),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8D8C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE0A458).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ PLANTS — botanical leaf, stems, water droplets ═══
        cat == "PLANTS" -> SignatureDesign(
            bg = Color(0xFFF1F7EC), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Big leaf with veins
                val leaf = Path().apply {
                    moveTo(w * 0.78f, h * 0.18f)
                    quadraticBezierTo(w * 0.96f, h * 0.42f, w * 0.76f, h * 0.68f)
                    quadraticBezierTo(w * 0.58f, h * 0.56f, w * 0.62f, h * 0.30f)
                    close()
                }
                drawPath(leaf, Color(0xFF4E8C4E).copy(alpha = 0.55f))
                drawPath(Path().apply { moveTo(w * 0.70f, h * 0.24f); quadraticBezierTo(w * 0.78f, h * 0.44f, w * 0.74f, h * 0.62f) }, Color(0xFF2E6A3A).copy(alpha = 0.7f), style = Stroke(1f))
                for (i in 0 until 4) {
                    val t = 0.3f + i * 0.15f
                    drawLine(Color(0xFF2E6A3A).copy(alpha = 0.5f), Offset(w * 0.70f + t * w * 0.10f, h * (0.24f + t * 0.5f)), Offset(w * 0.70f + t * w * 0.10f + w * 0.04f, h * (0.24f + t * 0.5f) - h * 0.05f), strokeWidth = 0.7f)
                }
                // Stems with small leaves
                drawLine(Color(0xFF6FB87F).copy(alpha = 0.60f), Offset(w * 0.14f, h * 0.90f), Offset(w * 0.22f, h * 0.60f), strokeWidth = 1.5f)
                drawOval(Color(0xFF6FB87F).copy(alpha = 0.60f), Offset(w * 0.18f, h * 0.66f), Size(w * 0.06f, h * 0.03f))
                drawOval(Color(0xFF6FB87F).copy(alpha = 0.50f), Offset(w * 0.24f, h * 0.76f), Size(w * 0.05f, h * 0.025f))
                // Water droplets
                listOf(Offset(w * 0.40f, h * 0.30f), Offset(w * 0.52f, h * 0.46f), Offset(w * 0.44f, h * 0.68f)).forEach { p ->
                    drawCircle(Color(0xFF7FB8E8).copy(alpha = 0.55f), 3f, p)
                    drawCircle(Color.White.copy(alpha = 0.7f), 1f, Offset(p.x - 1f, p.y - 1f))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4E8C4E), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF1E3A24),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF4E8C4E),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF3A4A3A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF4E8C4E).copy(alpha = 0.60f),
            layout = SignatureLayout.SIDE
        )
        // ═══ TECHNOLOGIES — circuit traces, chip, data streams ═══
        cat == "TECHNOLOGIES" -> SignatureDesign(
            bg = Color(0xFF070D1A), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Circuit traces
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 18) {
                    val x1 = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y1 = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    val x2 = x1 + ((s * (i+1) * 3571) % 200 - 100) / 100f * w * 0.14f
                    val y2 = y1 + ((s * (i+1) * 4201) % 200 - 100) / 100f * h * 0.10f
                    drawLine(Color(0xFF33E0FF).copy(alpha = 0.35f), Offset(x1, y1), Offset(x2, y1), strokeWidth = 0.8f)
                    drawLine(Color(0xFF33E0FF).copy(alpha = 0.35f), Offset(x2, y1), Offset(x2, y2), strokeWidth = 0.8f)
                    drawCircle(Color(0xFF33E0FF).copy(alpha = 0.50f), 2f, Offset(x2, y2))
                }
                // CPU chip
                drawRoundRect(Color(0xFF33E0FF).copy(alpha = 0.25f), Offset(w * 0.70f, h * 0.62f), Size(w * 0.22f, h * 0.26f), CornerRadius(3f), style = Stroke(1.2f))
                drawRoundRect(Color(0xFF33E0FF).copy(alpha = 0.30f), Offset(w * 0.74f, h * 0.68f), Size(w * 0.14f, h * 0.14f), CornerRadius(2f))
                for (i in 0 until 5) {
                    drawLine(Color(0xFF33E0FF).copy(alpha = 0.40f), Offset(w * 0.72f + i * w * 0.038f, h * 0.62f), Offset(w * 0.72f + i * w * 0.038f, h * 0.58f), strokeWidth = 0.8f)
                    drawLine(Color(0xFF33E0FF).copy(alpha = 0.40f), Offset(w * 0.72f + i * w * 0.038f, h * 0.88f), Offset(w * 0.72f + i * w * 0.038f, h * 0.92f), strokeWidth = 0.8f)
                }
                // Binary data streams
                for (i in 0 until 8) {
                    val x = w * 0.08f + i * w * 0.09f
                    for (j in 0 until 5) {
                        if ((i + j) % 3 != 0) drawCircle(Color(0xFF33E0FF).copy(alpha = 0.30f), 1.2f, Offset(x, h * 0.14f + j * h * 0.035f))
                    }
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF33E0FF), badgeInk = Color(0xFF070D1A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = GeomFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD8F2FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF33E0FF),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFA8C4DC).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF33E0FF).copy(alpha = 0.70f),
            layout = SignatureLayout.OVERLAY
        )
        // ═══ ASTRONOMY — spiral galaxy, ringed planet, star clusters ═══
        cat == "ASTRONOMY" -> SignatureDesign(
            bg = Color(0xFF0B0A1E), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Starfield
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 110) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    val r = 0.3f + ((s * (i+1) * 3571) % 100) / 100f * 2.4f
                    drawCircle(Color.White.copy(alpha = 0.12f + ((s * (i+1) * 4201) % 100) / 100f * 0.30f), r, Offset(x, y))
                }
                // Spiral galaxy bottom-left
                val gx = w * 0.26f; val gy = h * 0.62f
                for (i in 0 until 3) {
                    drawArc(Color(0xFF9A8CFF).copy(alpha = 0.40f - i * 0.08f), 40f, 220f, false, Offset(gx - w * (0.08f + i * 0.03f), gy - w * (0.08f + i * 0.03f)), Size(w * (0.16f + i * 0.06f), w * (0.16f + i * 0.06f)), style = Stroke(1.4f))
                }
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.55f), 2.5f, Offset(gx, gy))
                // Ringed planet top-right
                val px = w * 0.80f; val py = h * 0.16f
                drawCircle(Color(0xFF3A6A8A).copy(alpha = 0.60f), w * 0.055f, Offset(px, py))
                drawOval(Color(0xFF9A8CFF).copy(alpha = 0.35f), Offset(px - w * 0.09f, py - h * 0.012f), Size(w * 0.18f, h * 0.025f), style = Stroke(1.4f))
                // Nebula glow
                drawCircle(Color(0xFF4A2A8A).copy(alpha = 0.20f), w * 0.22f, Offset(w * 0.72f, h * 0.70f))
                drawCircle(Color(0xFF2A4A6A).copy(alpha = 0.18f), w * 0.18f, Offset(w * 0.16f, h * 0.20f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF9A8CFF), badgeInk = Color(0xFF0B0A1E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE0DAF8),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF9A8CFF),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB8B4D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF9A8CFF).copy(alpha = 0.70f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ HISTORY — parchment scroll, hourglass, timeline ═══
        cat == "HISTORY" -> SignatureDesign(
            bg = Color(0xFFF3E7CF), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Unrolled scroll
                drawRect(Color(0xFFE8D9B8).copy(alpha = 0.80f), Offset(w * 0.08f, h * 0.14f), Size(w * 0.84f, h * 0.30f))
                drawCircle(Color(0xFFC9A66B), w * 0.028f, Offset(w * 0.08f, h * 0.14f + h * 0.15f))
                drawCircle(Color(0xFFC9A66B), w * 0.028f, Offset(w * 0.92f, h * 0.14f + h * 0.15f))
                for (i in 0 until 5) { drawLine(Color(0xFF8A6A3A).copy(alpha = 0.35f), Offset(w * 0.14f, h * 0.20f + i * h * 0.045f), Offset(w * 0.86f, h * 0.20f + i * h * 0.045f), strokeWidth = 0.6f) }
                // Hourglass bottom-right
                val hx = w * 0.82f; val hy = h * 0.66f
                drawLine(Color(0xFF8A6A3A).copy(alpha = 0.5f), Offset(hx - w * 0.035f, hy), Offset(hx + w * 0.035f, hy), strokeWidth = 1.4f)
                drawLine(Color(0xFF8A6A3A).copy(alpha = 0.5f), Offset(hx - w * 0.035f, hy), Offset(hx, hy + h * 0.07f), strokeWidth = 1.2f)
                drawLine(Color(0xFF8A6A3A).copy(alpha = 0.5f), Offset(hx + w * 0.035f, hy), Offset(hx, hy + h * 0.07f), strokeWidth = 1.2f)
                drawLine(Color(0xFF8A6A3A).copy(alpha = 0.5f), Offset(hx, hy + h * 0.07f), Offset(hx - w * 0.035f, hy + h * 0.14f), strokeWidth = 1.2f)
                drawLine(Color(0xFF8A6A3A).copy(alpha = 0.5f), Offset(hx, hy + h * 0.07f), Offset(hx + w * 0.035f, hy + h * 0.14f), strokeWidth = 1.2f)
                drawLine(Color(0xFF8A6A3A).copy(alpha = 0.5f), Offset(hx - w * 0.035f, hy + h * 0.14f), Offset(hx + w * 0.035f, hy + h * 0.14f), strokeWidth = 1.4f)
                // Timeline dots
                for (i in 0 until 6) { drawCircle(Color(0xFFC9A66B).copy(alpha = 0.6f), 2.2f, Offset(w * 0.16f + i * w * 0.12f, h * 0.88f)) }
                drawLine(Color(0xFFC9A66B).copy(alpha = 0.4f), Offset(w * 0.16f, h * 0.88f), Offset(w * 0.76f, h * 0.88f), strokeWidth = 0.8f)
            },
            padding = PaddingValues(horizontal = 24.dp, vertical = 22.dp), badgeColor = Color(0xFF8A6A3A), badgeInk = Color(0xFFF3E7CF),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF4A3824),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8A6A3A),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF5A4828).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8A6A3A).copy(alpha = 0.60f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ GEOLOGY — rock strata, crystal shards, cross-section ═══
        cat == "GEOLOGY" -> SignatureDesign(
            bg = Color(0xFF221812), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Strata layers
                val strata = listOf(Color(0xFFC98A5B), Color(0xFF8A5A3A), Color(0xFFD9B45B), Color(0xFF6A4230))
                for (i in 0 until 4) {
                    val y = h * 0.62f + i * h * 0.075f
                    val layer = Path().apply {
                        moveTo(0f, y)
                        quadraticBezierTo(w * 0.25f, y - h * 0.02f, w * 0.5f, y + h * 0.01f)
                        quadraticBezierTo(w * 0.75f, y + h * 0.02f, w, y - h * 0.015f)
                        lineTo(w, y + h * 0.075f)
                        lineTo(0f, y + h * 0.075f)
                        close()
                    }
                    drawPath(layer, strata[i].copy(alpha = 0.20f))
                }
                // Crystal shards
                listOf(Offset(w * 0.20f, h * 0.24f), Offset(w * 0.34f, h * 0.34f), Offset(w * 0.50f, h * 0.22f)).forEach { p ->
                    val shard = Path().apply {
                        moveTo(p.x, p.y - h * 0.14f); lineTo(p.x + w * 0.035f, p.y); lineTo(p.x, p.y + h * 0.02f); lineTo(p.x - w * 0.035f, p.y); close()
                    }
                    drawPath(shard, Color(0xFFE8C8A0).copy(alpha = 0.40f), style = Stroke(1.2f))
                    drawLine(Color(0xFFE8C8A0).copy(alpha = 0.30f), Offset(p.x, p.y - h * 0.14f), Offset(p.x, p.y), strokeWidth = 0.8f)
                }
                // Contour arcs
                drawArc(Color(0xFFC98A5B).copy(alpha = 0.25f), 200f, 120f, false, Offset(w * 0.62f, h * 0.30f), Size(w * 0.30f, h * 0.34f), style = Stroke(1f))
                drawArc(Color(0xFFC98A5B).copy(alpha = 0.18f), 200f, 120f, false, Offset(w * 0.68f, h * 0.36f), Size(w * 0.24f, h * 0.26f), style = Stroke(1f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC98A5B), badgeInk = Color(0xFF221812),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = GeomFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF0E0C8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC98A5B),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD8C0A8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFC98A5B).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ MEDICINE — EKG trace, cross, capsule, pulse rings ═══
        cat == "MEDICINE" -> SignatureDesign(
            bg = Color(0xFF0D2226), cornerRadius = 6f,
            drawBackground = { w, h ->
                // EKG line
                drawPath(Path().apply {
                    moveTo(0f, h * 0.55f)
                    lineTo(w * 0.30f, h * 0.55f)
                    lineTo(w * 0.38f, h * 0.40f)
                    lineTo(w * 0.46f, h * 0.72f)
                    lineTo(w * 0.54f, h * 0.30f)
                    lineTo(w * 0.62f, h * 0.60f)
                    lineTo(w * 0.72f, h * 0.55f)
                    lineTo(w, h * 0.55f)
                }, Color(0xFF4FD8C8).copy(alpha = 0.80f), style = Stroke(1.6f))
                // Pulse rings top-right
                drawCircle(Color(0xFF4FD8C8).copy(alpha = 0.15f), w * 0.10f, Offset(w * 0.84f, h * 0.18f), style = Stroke(1f))
                drawCircle(Color(0xFF4FD8C8).copy(alpha = 0.20f), w * 0.07f, Offset(w * 0.84f, h * 0.18f), style = Stroke(1f))
                drawCircle(Color(0xFF4FD8C8).copy(alpha = 0.30f), w * 0.035f, Offset(w * 0.84f, h * 0.18f))
                // Capsule
                drawRoundRect(Color(0xFFE8F2F0).copy(alpha = 0.25f), Offset(w * 0.14f, h * 0.20f), Size(w * 0.18f, h * 0.07f), CornerRadius(10f))
                drawLine(Color(0xFFE8F2F0).copy(alpha = 0.30f), Offset(w * 0.23f, h * 0.20f), Offset(w * 0.23f, h * 0.27f), strokeWidth = 1f)
                // Cross, subtle
                drawLine(Color(0xFFE8F2F0).copy(alpha = 0.18f), Offset(w * 0.88f, h * 0.80f), Offset(w * 0.96f, h * 0.80f), strokeWidth = 2f)
                drawLine(Color(0xFFE8F2F0).copy(alpha = 0.18f), Offset(w * 0.92f, h * 0.76f), Offset(w * 0.92f, h * 0.84f), strokeWidth = 2f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FD8C8), badgeInk = Color(0xFF0D2226),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFDCF2EE),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4FD8C8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0CCC8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF4FD8C8).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ PSYCHOLOGY — head silhouette with neural network + thought cloud ═══
        cat == "PSYCHOLOGY" -> SignatureDesign(
            bg = Color(0xFF171030), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E1438), Color(0xFF171030), Color(0xFF0C0820))), size = Size(w, h))
                // Head profile silhouette — a smooth side-profile path
                val hpx = w * 0.72f; val hpy = h * 0.18f
                val head = Path().apply {
                    moveTo(hpx, hpy)
                    cubicTo(hpx + w * 0.10f, hpy, hpx + w * 0.14f, hpy + h * 0.16f, hpx + w * 0.08f, hpy + h * 0.24f)
                    cubicTo(hpx + w * 0.06f, hpy + h * 0.28f, hpx + w * 0.06f, hpy + h * 0.34f, hpx + w * 0.02f, hpy + h * 0.36f)
                    cubicTo(hpx - w * 0.02f, hpy + h * 0.38f, hpx - w * 0.04f, hpy + h * 0.42f, hpx - w * 0.04f, hpy + h * 0.48f)
                    lineTo(hpx - w * 0.12f, hpy + h * 0.48f)
                    lineTo(hpx - w * 0.12f, hpy + h * 0.50f)
                    cubicTo(hpx - w * 0.12f, hpy + h * 0.54f, hpx - w * 0.08f, hpy + h * 0.56f, hpx - w * 0.06f, hpy + h * 0.56f)
                    cubicTo(hpx - w * 0.04f, hpy + h * 0.60f, hpx - w * 0.08f, hpy + h * 0.64f, hpx - w * 0.06f, hpy + h * 0.66f)
                    cubicTo(hpx - w * 0.04f, hpy + h * 0.68f, hpx, hpy + h * 0.66f, hpx, hpy + h * 0.62f)
                    close()
                }
                drawPath(head, Color(0xFFC4B0F0).copy(alpha = 0.12f))
                // Neural network inside the head — nodes + connections
                val nodes = listOf(Offset(hpx + w * 0.04f, hpy + h * 0.16f), Offset(hpx + w * 0.02f, hpy + h * 0.28f), Offset(hpx - w * 0.02f, hpy + h * 0.22f), Offset(hpx + w * 0.06f, hpy + h * 0.34f), Offset(hpx - w * 0.04f, hpy + h * 0.36f))
                for (i in nodes.indices) {
                    for (j in i + 1 until nodes.size) {
                        drawLine(Color(0xFFC4B0F0).copy(alpha = 0.25f), nodes[i], nodes[j], strokeWidth = 0.6f)
                    }
                    drawCircle(Color(0xFFE8E0F8).copy(alpha = 0.65f), 2.5f, nodes[i])
                }
                // Thought cloud — stacked rounded bubbles top-left
                drawCircle(Color(0xFFE8E0F8).copy(alpha = 0.12f), w * 0.08f, Offset(w * 0.20f, h * 0.20f), style = Stroke(1.2f))
                drawCircle(Color(0xFFE8E0F8).copy(alpha = 0.18f), w * 0.05f, Offset(w * 0.14f, h * 0.30f), style = Stroke(1f))
                drawCircle(Color(0xFFE8E0F8).copy(alpha = 0.22f), w * 0.025f, Offset(w * 0.10f, h * 0.36f), style = Stroke(0.8f))
                // Soft connecting dots
                for (i in 0 until 4) { drawCircle(Color(0xFFC4B0F0).copy(alpha = 0.30f - i * 0.05f), 1.5f, Offset(w * 0.12f + i * w * 0.015f, h * 0.40f + i * h * 0.01f)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC4B0F0), badgeInk = Color(0xFF171030),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF0E8F8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC4B0F0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C4E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFC4B0F0).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ MATHEMATICS — golden spiral, geometry, coordinate grid ═══
        cat == "MATHEMATICS" -> SignatureDesign(
            bg = Color(0xFF101526), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Coordinate grid
                for (i in 0 until 14) { drawLine(Color(0xFF2A3A5A).copy(alpha = 0.25f), Offset(i * w * 0.075f, 0f), Offset(i * w * 0.075f, h), strokeWidth = 0.3f) }
                for (i in 0 until 11) { drawLine(Color(0xFF2A3A5A).copy(alpha = 0.25f), Offset(0f, i * h * 0.1f), Offset(w, i * h * 0.1f), strokeWidth = 0.3f) }
                // Golden spiral
                var sx = w * 0.30f; var sy = h * 0.40f; var r = w * 0.012f
                for (i in 0 until 8) {
                    val (start, sweep) = when (i % 4) { 0 -> 90f to 180f; 1 -> 0f to 90f; 2 -> 270f to 360f; else -> 180f to 270f }
                    drawArc(Color(0xFFE8C05C).copy(alpha = 0.55f), start, sweep, false, Offset(sx - r, sy - r), Size(r * 2f, r * 2f), style = Stroke(1.2f))
                    when (i % 4) { 0 -> sy += r; 1 -> sx += r; 2 -> sy -= r; else -> sx -= r }
                    r *= 1.3f
                }
                // Geometry shapes
                drawCircle(Color(0xFF6FA8FF).copy(alpha = 0.35f), w * 0.05f, Offset(w * 0.82f, h * 0.20f), style = Stroke(1.2f))
                drawPath(Path().apply {
                    moveTo(w * 0.74f, h * 0.42f); lineTo(w * 0.90f, h * 0.42f); lineTo(w * 0.82f, h * 0.30f); close()
                }, Color(0xFF6FA8FF).copy(alpha = 0.35f), style = Stroke(1.2f))
                drawRect(Color(0xFF6FA8FF).copy(alpha = 0.30f), Offset(w * 0.76f, h * 0.52f), Size(w * 0.14f, h * 0.12f), style = Stroke(1.2f))
                drawArc(Color(0xFFE8C05C).copy(alpha = 0.30f), 0f, 180f, false, Offset(w * 0.12f, h * 0.70f), Size(w * 0.16f, h * 0.12f), style = Stroke(1.2f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8C05C), badgeInk = Color(0xFF101526),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8E4F8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8C05C),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB8B4D0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8C05C).copy(alpha = 0.70f),
            layout = SignatureLayout.SIDE
        )
        // ═══ ECONOMICS — rising chart, coin stack, trend arrow ═══
        cat == "ECONOMICS" -> SignatureDesign(
            bg = Color(0xFF0D2015), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Grid
                for (i in 0 until 12) { drawLine(Color(0xFF2A4A3A).copy(alpha = 0.25f), Offset(0f, i * h * 0.09f), Offset(w, i * h * 0.09f), strokeWidth = 0.3f) }
                // Rising bars
                listOf(0.30f, 0.42f, 0.55f, 0.70f, 0.88f).forEachIndexed { i, bh ->
                    drawRect(Color(0xFF3FBF5A).copy(alpha = 0.40f), Offset(w * 0.12f + i * w * 0.10f, h * 0.88f - bh * h * 0.55f), Size(w * 0.05f, bh * h * 0.55f))
                }
                // Trend line + arrow
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.70f), Offset(w * 0.12f, h * 0.70f), Offset(w * 0.60f, h * 0.36f), strokeWidth = 1.6f)
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.70f), Offset(w * 0.60f, h * 0.36f), Offset(w * 0.54f, h * 0.36f), strokeWidth = 1.6f)
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.70f), Offset(w * 0.60f, h * 0.36f), Offset(w * 0.60f, h * 0.42f), strokeWidth = 1.6f)
                // Coin stack
                for (i in 0 until 3) {
                    drawCircle(Color(0xFFE8C05C).copy(alpha = 0.55f), w * 0.032f, Offset(w * 0.84f, h * 0.66f - i * h * 0.045f))
                    drawCircle(Color(0xFF0D2015).copy(alpha = 0.5f), w * 0.02f, Offset(w * 0.84f, h * 0.66f - i * h * 0.045f))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF3FBF5A), badgeInk = Color(0xFF0D2015),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFDCF2E2),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF3FBF5A),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB8D8C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF3FBF5A).copy(alpha = 0.70f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ LANGUAGE — speech bubbles, calligraphy strokes ═══
        cat == "LANGUAGE" -> SignatureDesign(
            bg = Color(0xFFF6EDE3), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Speech bubbles
                drawRoundRect(Color(0xFFD96C4A).copy(alpha = 0.30f), Offset(w * 0.10f, h * 0.12f), Size(w * 0.34f, h * 0.22f), CornerRadius(8f))
                drawPath(Path().apply { moveTo(w * 0.20f, h * 0.34f); lineTo(w * 0.16f, h * 0.44f); lineTo(w * 0.28f, h * 0.34f); close() }, Color(0xFFD96C4A).copy(alpha = 0.30f))
                drawRoundRect(Color(0xFF3A2A22).copy(alpha = 0.25f), Offset(w * 0.56f, h * 0.24f), Size(w * 0.34f, h * 0.18f), CornerRadius(8f))
                drawPath(Path().apply { moveTo(w * 0.66f, h * 0.42f); lineTo(w * 0.62f, h * 0.50f); lineTo(w * 0.74f, h * 0.42f); close() }, Color(0xFF3A2A22).copy(alpha = 0.25f))
                for (i in 0 until 3) { drawLine(Color(0xFFFFF8F0).copy(alpha = 0.55f), Offset(w * 0.15f, h * 0.18f + i * h * 0.05f), Offset(w * 0.38f, h * 0.18f + i * h * 0.05f), strokeWidth = 0.8f) }
                // Calligraphy strokes
                drawPath(Path().apply { moveTo(w * 0.16f, h * 0.68f); cubicTo(w * 0.40f, h * 0.56f, w * 0.60f, h * 0.80f, w * 0.86f, h * 0.64f) }, Color(0xFFD96C4A).copy(alpha = 0.45f), style = Stroke(2.5f))
                drawPath(Path().apply { moveTo(w * 0.30f, h * 0.86f); cubicTo(w * 0.50f, h * 0.78f, w * 0.70f, h * 0.92f, w * 0.88f, h * 0.82f) }, Color(0xFF3A2A22).copy(alpha = 0.30f), style = Stroke(1.5f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD96C4A), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF3A2A22),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFD96C4A),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF5A4636).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFD96C4A).copy(alpha = 0.60f),
            layout = SignatureLayout.SIDE
        )
        // ═══ ENGINEERING — blueprint grid, gear, dimension lines ═══
        cat == "ENGINEERING" -> SignatureDesign(
            bg = Color(0xFF0E1D38), cornerRadius = 6f,
            drawBackground = { w, h ->
                // Blueprint grid
                for (i in 0 until 20) { drawLine(Color(0xFF4A6A9A).copy(alpha = 0.20f), Offset(i * w * 0.05f, 0f), Offset(i * w * 0.05f, h), strokeWidth = 0.3f) }
                for (i in 0 until 15) { drawLine(Color(0xFF4A6A9A).copy(alpha = 0.20f), Offset(0f, i * h * 0.07f), Offset(w, i * h * 0.07f), strokeWidth = 0.3f) }
                // Gear with teeth
                val gx = w * 0.28f; val gy = h * 0.38f; val gr = w * 0.08f
                drawCircle(Color(0xFF6FA8FF).copy(alpha = 0.40f), gr, Offset(gx, gy), style = Stroke(1.4f))
                drawCircle(Color(0xFF6FA8FF).copy(alpha = 0.30f), gr * 0.45f, Offset(gx, gy), style = Stroke(1.2f))
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0).toFloat()
                    drawLine(Color(0xFF6FA8FF).copy(alpha = 0.40f), Offset(gx + kotlin.math.cos(a) * gr, gy + kotlin.math.sin(a) * gr), Offset(gx + kotlin.math.cos(a) * gr * 1.25f, gy + kotlin.math.sin(a) * gr * 1.25f), strokeWidth = 2f)
                }
                // Dimension lines
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.45f), Offset(w * 0.60f, h * 0.16f), Offset(w * 0.88f, h * 0.16f), strokeWidth = 1f)
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.45f), Offset(w * 0.60f, h * 0.13f), Offset(w * 0.60f, h * 0.19f), strokeWidth = 0.8f)
                drawLine(Color(0xFFE8C05C).copy(alpha = 0.45f), Offset(w * 0.88f, h * 0.13f), Offset(w * 0.88f, h * 0.19f), strokeWidth = 0.8f)
                // Protractor arc
                drawArc(Color(0xFF6FA8FF).copy(alpha = 0.30f), 180f, 180f, false, Offset(w * 0.60f, h * 0.66f), Size(w * 0.30f, h * 0.18f), style = Stroke(1.2f))
                drawLine(Color(0xFF6FA8FF).copy(alpha = 0.30f), Offset(w * 0.60f, h * 0.84f), Offset(w * 0.90f, h * 0.84f), strokeWidth = 1f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF6FA8FF), badgeInk = Color(0xFF0E1D38),
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = GeomFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD8E4FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF6FA8FF),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFA8BCDC).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF6FA8FF).copy(alpha = 0.70f),
            layout = SignatureLayout.OVERLAY
        )
        // ═══ OCEANS — sun shaft, fish school, coral bed ═══
        cat == "OCEANS" -> SignatureDesign(
            bg = Color(0xFF082A3E), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Deep azure gradient — brighter near the surface
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E5A7E), Color(0xFF082A3E), Color(0xFF04141F))), size = Size(w, h))
                // Sun shaft from the surface
                val shaft = Path().apply {
                    moveTo(w * 0.58f, 0f)
                    lineTo(w * 0.34f, h * 0.78f)
                    lineTo(w * 0.52f, h * 0.78f)
                    lineTo(w * 0.78f, 0f)
                    close()
                }
                drawPath(shaft, Color(0xFF9AE0F2).copy(alpha = 0.07f))
                // Surface shimmer line
                drawLine(Color(0xFF9AE0F2).copy(alpha = 0.35f), Offset(w * 0.03f, h * 0.04f), Offset(w * 0.97f, h * 0.04f), strokeWidth = 1.2f)
                for (i in 0 until 10) {
                    val sx = w * 0.05f + i * w * 0.095f
                    drawLine(Color(0xFF9AE0F2).copy(alpha = 0.18f), Offset(sx, h * 0.025f), Offset(sx + w * 0.02f, h * 0.055f), strokeWidth = 0.8f)
                }
                // Fish school swimming left
                fun fish(cx: Float, cy: Float, scale: Float, alpha: Float) {
                    drawOval(Color(0xFF9AE0F2).copy(alpha = alpha), Offset(cx - w * 0.035f * scale, cy - h * 0.012f * scale), Size(w * 0.06f * scale, h * 0.024f * scale))
                    drawPath(Path().apply { moveTo(cx + w * 0.025f * scale, cy); lineTo(cx + w * 0.05f * scale, cy - h * 0.014f * scale); lineTo(cx + w * 0.05f * scale, cy + h * 0.014f * scale); close() }, Color(0xFF9AE0F2).copy(alpha = alpha))
                    drawCircle(Color(0xFF082A3E).copy(alpha = 0.6f), 1f * scale, Offset(cx - w * 0.015f * scale, cy - h * 0.005f * scale))
                }
                fish(w * 0.72f, h * 0.30f, 1.3f, 0.55f)
                fish(w * 0.58f, h * 0.38f, 1.0f, 0.45f)
                fish(w * 0.82f, h * 0.44f, 0.8f, 0.40f)
                // Bubbles rising from the coral
                for (i in 0 until 7) {
                    val bx = w * 0.16f + i * w * 0.11f
                    val by = h * 0.88f - i * h * 0.09f
                    drawCircle(Color(0xFF9AE0F2).copy(alpha = 0.35f), 1.4f + (i % 3) * 0.7f, Offset(bx, by), style = Stroke(0.8f))
                }
                // Layered waves in the mid-water
                for (i in 0 until 3) {
                    val wave = Path().apply {
                        moveTo(0f, h * (0.60f + i * 0.12f))
                        for (j in 0..12) { lineTo(j * w / 12f, h * (0.60f + i * 0.12f) + kotlin.math.sin(j * 1.2f + i).toFloat() * h * 0.018f) }
                    }
                    drawPath(wave, Color(0xFF3FB8E8).copy(alpha = 0.22f - i * 0.05f), style = Stroke(1.2f))
                }
                // Coral + seaweed bed along the bottom
                drawOval(Color(0xFF2E6A9E).copy(alpha = 0.35f), Offset(0f, h * 0.86f), Size(w, h * 0.14f))
                listOf(Offset(w * 0.10f, h * 0.88f), Offset(w * 0.18f, h * 0.90f), Offset(w * 0.26f, h * 0.87f)).forEach { c ->
                    drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x - w * 0.012f, c.y - h * 0.05f); lineTo(c.x, c.y - h * 0.10f); lineTo(c.x + w * 0.012f, c.y - h * 0.05f); close() }, Color(0xFF6BB8E8).copy(alpha = 0.35f))
                }
                for (i in 0 until 8) {
                    val gx = w * 0.34f + i * w * 0.07f
                    drawLine(Color(0xFF4E8AAE).copy(alpha = 0.30f), Offset(gx, h * 0.92f), Offset(gx + w * 0.006f, h * 0.82f + (i % 3) * h * 0.012f), strokeWidth = 1.2f)
                }
                // Gentle vignette
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF020810).copy(alpha = 0.5f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.9f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF3FB8E8), badgeInk = Color(0xFF082A3E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFD8F2FC),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF3FB8E8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFA8D4E8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF3FB8E8).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ QUOTES — giant quote marks, gold rules, flourish ═══
        cat == "QUOTES" -> SignatureDesign(
            bg = Color(0xFFFAF6EC), cornerRadius = 10f,
            drawBackground = { w, h ->
                // Giant opening quote marks
                drawArc(Color(0xFF2A2418).copy(alpha = 0.30f), 200f, 140f, false, Offset(w * 0.08f, h * 0.08f), Size(w * 0.14f, h * 0.14f), style = Stroke(4f))
                drawCircle(Color(0xFF2A2418).copy(alpha = 0.30f), w * 0.03f, Offset(w * 0.16f, h * 0.14f))
                // Giant closing quote marks
                drawArc(Color(0xFF2A2418).copy(alpha = 0.22f), 20f, 140f, false, Offset(w * 0.78f, h * 0.70f), Size(w * 0.14f, h * 0.14f), style = Stroke(4f))
                drawCircle(Color(0xFF2A2418).copy(alpha = 0.22f), w * 0.03f, Offset(w * 0.70f, h * 0.76f))
                // Gold rules
                drawLine(Color(0xFFC9A227).copy(alpha = 0.45f), Offset(w * 0.12f, h * 0.40f), Offset(w * 0.44f, h * 0.40f), strokeWidth = 1f)
                drawLine(Color(0xFFC9A227).copy(alpha = 0.30f), Offset(w * 0.14f, h * 0.44f), Offset(w * 0.40f, h * 0.44f), strokeWidth = 0.6f)
                // Flourish
                drawPath(Path().apply { moveTo(w * 0.50f, h * 0.58f); cubicTo(w * 0.56f, h * 0.54f, w * 0.60f, h * 0.60f, w * 0.66f, h * 0.56f) }, Color(0xFFC9A227).copy(alpha = 0.40f), style = Stroke(1.2f))
            },
            padding = PaddingValues(horizontal = 28.dp, vertical = 24.dp), badgeColor = Color(0xFFC9A227), badgeInk = Color(0xFFFAF6EC),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 12.dp,
            titleFont = LoraFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF2A2418),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
            bodySize = 10.5f, bodyLineHeight = 1.65f, bodyColor = Color(0xFF4A4234).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A227).copy(alpha = 0.60f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ WILDCARD — comet, sparkles, glow orb (brand coral) ═══
        cat == "WILDCARD" -> SignatureDesign(
            bg = Color(0xFF1E1026), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Glow orb
                drawCircle(Color(0xFFFF7A6B).copy(alpha = 0.12f), w * 0.28f, Offset(w * 0.26f, h * 0.62f))
                drawCircle(Color(0xFFFF7A6B).copy(alpha = 0.20f), w * 0.14f, Offset(w * 0.26f, h * 0.62f))
                // Comet streak
                drawLine(Color(0xFFFFD9A0).copy(alpha = 0.50f), Offset(w * 0.86f, h * 0.12f), Offset(w * 0.60f, h * 0.30f), strokeWidth = 2f)
                drawLine(Color(0xFFFFD9A0).copy(alpha = 0.20f), Offset(w * 0.88f, h * 0.10f), Offset(w * 0.50f, h * 0.34f), strokeWidth = 4f)
                drawCircle(Color(0xFFFFD9A0).copy(alpha = 0.85f), 3f, Offset(w * 0.86f, h * 0.12f))
                // Sparkles
                listOf(Offset(w * 0.72f, h * 0.66f), Offset(w * 0.50f, h * 0.22f), Offset(w * 0.16f, h * 0.30f)).forEach { p ->
                    drawLine(Color(0xFFFFD9A0).copy(alpha = 0.45f), Offset(p.x - 5f, p.y), Offset(p.x + 5f, p.y), strokeWidth = 0.9f)
                    drawLine(Color(0xFFFFD9A0).copy(alpha = 0.45f), Offset(p.x, p.y - 5f), Offset(p.x, p.y + 5f), strokeWidth = 0.9f)
                }
                // Starfield
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 50) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color.White.copy(alpha = 0.15f), 1f, Offset(x, y))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF7A6B), badgeInk = Color(0xFF1E1026),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF8E0E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF7A6B),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0C4C8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF7A6B).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ FALLBACK — quiet deep neutral (unknown category names) ═══
        else -> SignatureDesign(
            bg = Color(0xFF14141C), cornerRadius = 6f,
            drawBackground = { w, h ->
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 60) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color.White.copy(alpha = 0.10f), 0.8f + (i % 3) * 0.4f, Offset(x, y))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF6A6A8A), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8E4F0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8A8AB0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8C4D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF8A8AB0).copy(alpha = 0.65f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DETAILED SIGNATURE DESIGNS (opt-in experiment — "Deepen signature card
// elements" in Settings → Experiments). Rich, layered background scenes:
// gradient atmospheres, glows, vignettes and hand-drawn art per category.
// ═══════════════════════════════════════════════════════════════════════
private fun signatureDesignDetailed(categoryName: String, family: CategoryFamily): SignatureDesign {
    val cat = categoryName.uppercase().trim()
    return when {
        // ═══ BOOKS — cozy library: stacked books, lamp glow, gold bands ═══
        cat == "BOOKS" -> SignatureDesign(
            bg = Color(0xFF1E160E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2A18), Color(0xFF1E160E), Color(0xFF0E0A05))), size = Size(w, h))
                // Warm lamp glow, top-left — a reading nook
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.26f), Color(0xFFF2C879).copy(alpha = 0f))), radius = w * 0.48f, center = Offset(w * 0.22f, h * 0.12f))
                // Floor lamp — pole + shade with a warm halo, left
                drawLine(Color(0xFF6A5638).copy(alpha = 0.7f), Offset(w * 0.10f, h * 0.10f), Offset(w * 0.10f, h * 0.62f), strokeWidth = 2f)
                drawLine(Color(0xFF6A5638).copy(alpha = 0.5f), Offset(w * 0.07f, h * 0.62f), Offset(w * 0.13f, h * 0.62f), strokeWidth = 2f)
                drawPath(Path().apply {
                    moveTo(w * 0.06f, h * 0.10f)
                    quadraticBezierTo(w * 0.10f, h * 0.05f, w * 0.14f, h * 0.10f)
                    lineTo(w * 0.12f, h * 0.14f)
                    lineTo(w * 0.08f, h * 0.14f)
                    close()
                }, Color(0xFFF2C879).copy(alpha = 0.85f))
                // Top shelf line with books
                drawLine(Color(0xFF8A6B4A).copy(alpha = 0.40f), Offset(w * 0.28f, h * 0.30f), Offset(w * 0.94f, h * 0.30f), strokeWidth = 2f)
                val books = listOf(
                    Triple(w * 0.32f, w * 0.10f, Color(0xFFC94F4F)), Triple(w * 0.43f, w * 0.08f, Color(0xFF4F7AC9)),
                    Triple(w * 0.52f, w * 0.12f, Color(0xFF4FA86B)), Triple(w * 0.65f, w * 0.09f, Color(0xFFC9A24F)),
                    Triple(w * 0.75f, w * 0.11f, Color(0xFF8A5AC9)), Triple(w * 0.87f, w * 0.08f, Color(0xFFC97A4F))
                )
                books.forEachIndexed { i, (bx, bw, col) ->
                    val bh = h * 0.26f + (i % 3) * h * 0.03f
                    drawRoundRect(col.copy(alpha = 0.85f), Offset(bx, h * 0.30f - bh), Size(bw, bh), CornerRadius(1.5f))
                    drawLine(Color(0xFFF2C879).copy(alpha = 0.6f), Offset(bx + w * 0.004f, h * 0.30f - bh + w * 0.008f), Offset(bx + bw - w * 0.004f, h * 0.30f - bh + w * 0.008f), strokeWidth = 1.2f)
                }
                // Lower shelf — shorter books + a small stack
                drawLine(Color(0xFF8A6B4A).copy(alpha = 0.35f), Offset(w * 0.28f, h * 0.52f), Offset(w * 0.94f, h * 0.52f), strokeWidth = 1.6f)
                listOf(
                    Triple(w * 0.34f, w * 0.09f, Color(0xFF4FA8A8)), Triple(w * 0.44f, w * 0.07f, Color(0xFFC94F8A)),
                    Triple(w * 0.52f, w * 0.10f, Color(0xFF4F7AC9)), Triple(w * 0.63f, w * 0.08f, Color(0xFFC9A24F))
                ).forEachIndexed { i, (bx, bw, col) ->
                    val bh = h * 0.16f + (i % 2) * h * 0.02f
                    drawRoundRect(col.copy(alpha = 0.80f), Offset(bx, h * 0.52f - bh), Size(bw, bh), CornerRadius(1.5f))
                }
                // Open book on the desk, bottom-right — page lines + spread
                val obx = w * 0.66f; val oby = h * 0.80f
                drawPath(Path().apply {
                    moveTo(obx - w * 0.24f, oby)
                    quadraticBezierTo(obx, oby - h * 0.05f, obx + w * 0.24f, oby)
                    lineTo(obx + w * 0.22f, oby + h * 0.09f)
                    quadraticBezierTo(obx, oby + h * 0.035f, obx - w * 0.22f, oby + h * 0.09f)
                    close()
                }, Color(0xFFF2E4C8).copy(alpha = 0.30f))
                drawLine(Color(0xFF8A6B4A).copy(alpha = 0.50f), Offset(obx, oby - h * 0.025f), Offset(obx, oby + h * 0.06f), strokeWidth = 1f)
                for (i in 0 until 3) {
                    drawLine(Color(0xFF8A6B4A).copy(alpha = 0.25f), Offset(obx - w * 0.18f, oby - h * 0.015f + i * h * 0.016f), Offset(obx - w * 0.02f, oby - h * 0.015f + i * h * 0.016f), strokeWidth = 0.7f)
                    drawLine(Color(0xFF8A6B4A).copy(alpha = 0.25f), Offset(obx + w * 0.02f, oby - h * 0.015f + i * h * 0.016f), Offset(obx + w * 0.18f, oby - h * 0.015f + i * h * 0.016f), strokeWidth = 0.7f)
                }
                // Armchair silhouette, bottom-left
                drawPath(Path().apply {
                    moveTo(w * 0.10f, h * 0.70f)
                    quadraticBezierTo(w * 0.16f, h * 0.68f, w * 0.24f, h * 0.70f)
                    lineTo(w * 0.27f, h * 0.82f)
                    lineTo(w * 0.08f, h * 0.82f)
                    close()
                }, Color(0xFF8A4A3A).copy(alpha = 0.60f))
                drawLine(Color(0xFF6A3A2E).copy(alpha = 0.55f), Offset(w * 0.08f, h * 0.82f), Offset(w * 0.27f, h * 0.82f), strokeWidth = 2f)
                // Gold leaf ornament, top-right
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.18f), w * 0.05f, Offset(w * 0.90f, h * 0.08f), style = Stroke(1.2f))
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.12f), w * 0.03f, Offset(w * 0.90f, h * 0.08f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF060402).copy(alpha = 0.5f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFF2C879), badgeInk = Color(0xFF1E160E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF2E9D8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFF2C879),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD9CDB4).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFF2C879).copy(alpha = 0.72f)
        )
        // ═══ FILMS — cinema: marquee lights, film reel, velvet curtain ═══
        cat == "FILMS" -> SignatureDesign(
            bg = Color(0xFF120A0E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2E1418), Color(0xFF120A0E), Color(0xFF070406))), size = Size(w, h))
                // Marquee light string across the top
                for (i in 0 until 14) {
                    val lx = w * 0.06f + i * w * 0.068f
                    drawCircle(if (i % 2 == 0) Color(0xFFFFD98A) else Color(0xFFFF6B6B), w * 0.008f, Offset(lx, h * 0.06f))
                }
                drawLine(Color(0xFF5E3A3A).copy(alpha = 0.6f), Offset(w * 0.04f, h * 0.055f), Offset(w * 0.96f, h * 0.055f), strokeWidth = 0.8f)
                // Velvet curtains framing BOTH sides
                listOf(0f, 1f).forEach { side ->
                    val path = Path().apply {
                        moveTo(side * w, 0f)
                        quadraticBezierTo(side * w + (if (side == 0f) 1f else -1f) * w * 0.10f, h * 0.5f, side * w, h)
                        lineTo(side * w, 0f)
                        close()
                    }
                    drawPath(path, Color(0xFF6B1A22).copy(alpha = 0.55f))
                    // Curtain seam highlight
                    drawLine(Color(0xFF8A2A30).copy(alpha = 0.4f), Offset(side * w + (if (side == 0f) 1f else -1f) * w * 0.10f, h * 0.2f), Offset(side * w + (if (side == 0f) 1f else -1f) * w * 0.08f, h * 0.8f), strokeWidth = 1f)
                }
                // Screen glow, centre
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFFFE8B8).copy(alpha = 0.06f), Color(0xFF120A0E))), topLeft = Offset(w * 0.28f, h * 0.16f), size = Size(w * 0.44f, h * 0.40f))
                drawRect(Color(0xFFD9B45B).copy(alpha = 0.12f), Offset(w * 0.28f, h * 0.16f), Size(w * 0.44f, h * 0.40f), style = Stroke(1f))
                // Film reel, right — circle with sprocket holes
                val rcx = w * 0.84f; val rcy = h * 0.68f
                drawCircle(Color(0xFFD9B45B).copy(alpha = 0.40f), w * 0.08f, Offset(rcx, rcy), style = Stroke(1.6f))
                drawCircle(Color(0xFFD9B45B).copy(alpha = 0.35f), w * 0.03f, Offset(rcx, rcy))
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0).toFloat()
                    drawCircle(Color(0xFFD9B45B).copy(alpha = 0.45f), 1.8f, Offset(rcx + kotlin.math.cos(a) * w * 0.055f, rcy + kotlin.math.sin(a) * w * 0.055f))
                }
                // Projector beam from the left
                drawPath(Path().apply {
                    moveTo(w * 0.12f, h * 0.22f)
                    lineTo(w * 0.40f, h * 0.10f)
                    lineTo(w * 0.40f, h * 0.18f)
                    lineTo(w * 0.12f, h * 0.30f)
                    close()
                }, Color(0xFFFFE8B8).copy(alpha = 0.08f))
                // Red carpet, bottom
                drawRect(Color(0xFF8B1A1A).copy(alpha = 0.35f), Offset(0f, h * 0.88f), Size(w, h * 0.12f))
                drawLine(Color(0xFFC94F4F).copy(alpha = 0.4f), Offset(0f, h * 0.88f), Offset(w, h * 0.88f), strokeWidth = 1f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF040203).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6B6B), badgeInk = Color(0xFF120A0E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 16.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF5E8E8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFFD98A),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFDCC8CC).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFFF6B6B).copy(alpha = 0.72f)
        )
        // ═══ GAMES — arcade: neon grid, controller, glowing coin ═══
        cat == "GAMES" -> SignatureDesign(
            bg = Color(0xFF0A0A1E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF1E1E4A), Color(0xFF0A0A1E), Color(0xFF04040E)), center = Offset(w * 0.5f, h * 0.4f), radius = w * 0.95f), size = Size(w, h))
                // Neon grid floor, bottom — perspective lines
                for (i in 0 until 8) {
                    val y = h * 0.72f + i * h * 0.035f
                    drawLine(Color(0xFF00FF88).copy(alpha = 0.20f), Offset(w * 0.10f, y), Offset(w * 0.90f, y), strokeWidth = 0.8f)
                }
                for (i in 0 until 7) {
                    val x = w * 0.12f + i * w * 0.12f
                    drawLine(Color(0xFF00FF88).copy(alpha = 0.14f), Offset(x, h * 0.72f), Offset(w * 0.5f + (x - w * 0.5f) * 0.5f, h * 0.88f), strokeWidth = 0.6f)
                }
                // Glowing coin, top-center
                val ccx = w * 0.30f; val ccy = h * 0.24f
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFD98A).copy(alpha = 0.30f), Color(0xFFFFD98A).copy(alpha = 0f))), radius = w * 0.14f, center = Offset(ccx, ccy))
                drawCircle(Color(0xFFE8C84F), w * 0.045f, Offset(ccx, ccy), style = Stroke(1.6f))
                drawCircle(Color(0xFFE8C84F), w * 0.03f, Offset(ccx, ccy))
                drawLine(Color(0xFFFFF3C4).copy(alpha = 0.8f), Offset(ccx - w * 0.008f, ccy), Offset(ccx + w * 0.008f, ccy), strokeWidth = 0.8f)
                // Controller, bottom-left — body + D-pad + 4 buttons
                val gx = w * 0.30f; val gy = h * 0.66f
                drawRoundRect(Color(0xFF3A3A6A).copy(alpha = 0.85f), Offset(gx - w * 0.18f, gy - w * 0.035f), Size(w * 0.36f, w * 0.08f), CornerRadius(6f))
                // D-pad cross
                drawLine(Color(0xFF00CCFF).copy(alpha = 0.9f), Offset(gx - w * 0.12f, gy - w * 0.012f), Offset(gx - w * 0.12f, gy + w * 0.012f), strokeWidth = 1.6f)
                drawLine(Color(0xFF00CCFF).copy(alpha = 0.9f), Offset(gx - w * 0.135f, gy), Offset(gx - w * 0.105f, gy), strokeWidth = 1.6f)
                // Four action buttons (A/B/X/Y)
                drawCircle(Color(0xFF00FF88).copy(alpha = 0.9f), w * 0.014f, Offset(gx - w * 0.02f, gy - w * 0.012f))
                drawCircle(Color(0xFFFF5FA2).copy(alpha = 0.9f), w * 0.014f, Offset(gx + w * 0.02f, gy - w * 0.012f))
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.9f), w * 0.014f, Offset(gx - w * 0.02f, gy + w * 0.012f))
                drawCircle(Color(0xFF8FD0FF).copy(alpha = 0.9f), w * 0.014f, Offset(gx + w * 0.02f, gy + w * 0.012f))
                // Pixel easter eggs top-right: mushroom, star, ghost, heart
                // Mushroom — red cap + white dots + cream stem
                val mx = w * 0.72f; val my = h * 0.16f
                drawPath(Path().apply {
                    moveTo(mx, my - w * 0.022f)
                    quadraticBezierTo(mx - w * 0.028f, my - w * 0.030f, mx - w * 0.022f, my - w * 0.012f)
                    lineTo(mx - w * 0.022f, my - w * 0.008f)
                    lineTo(mx + w * 0.022f, my - w * 0.008f)
                    lineTo(mx + w * 0.022f, my - w * 0.012f)
                    quadraticBezierTo(mx + w * 0.028f, my - w * 0.030f, mx, my - w * 0.022f)
                    close()
                }, Color(0xFFD93B3B).copy(alpha = 0.9f))
                drawRect(Color(0xFFF2D9A8), Offset(mx - w * 0.008f, my - w * 0.008f), Size(w * 0.016f, w * 0.016f))
                drawCircle(Color.White.copy(alpha = 0.8f), w * 0.005f, Offset(mx - w * 0.014f, my - w * 0.022f))
                drawCircle(Color.White.copy(alpha = 0.8f), w * 0.005f, Offset(mx + w * 0.012f, my - w * 0.018f))
                // Star — 4-point sparkle
                val stx = w * 0.84f; val sty = h * 0.14f
                drawPath(Path().apply {
                    moveTo(stx, sty - w * 0.020f)
                    lineTo(stx + w * 0.008f, sty - w * 0.008f)
                    lineTo(stx + w * 0.020f, sty)
                    lineTo(stx + w * 0.008f, sty + w * 0.008f)
                    lineTo(stx, sty + w * 0.020f)
                    lineTo(stx - w * 0.008f, sty + w * 0.008f)
                    lineTo(stx - w * 0.020f, sty)
                    lineTo(stx - w * 0.008f, sty - w * 0.008f)
                    close()
                }, Color(0xFFF2C879).copy(alpha = 0.9f))
                // Ghost — white dome + eyes
                val ghx = w * 0.76f; val ghy = h * 0.30f
                drawPath(Path().apply {
                    moveTo(ghx - w * 0.014f, ghy)
                    quadraticBezierTo(ghx - w * 0.016f, ghy - w * 0.018f, ghx, ghy - w * 0.018f)
                    quadraticBezierTo(ghx + w * 0.016f, ghy - w * 0.018f, ghx + w * 0.014f, ghy)
                    lineTo(ghx + w * 0.014f, ghy + w * 0.016f)
                    lineTo(ghx + w * 0.008f, ghy + w * 0.010f)
                    lineTo(ghx, ghy + w * 0.016f)
                    lineTo(ghx - w * 0.008f, ghy + w * 0.010f)
                    lineTo(ghx - w * 0.014f, ghy + w * 0.016f)
                    close()
                }, Color.White.copy(alpha = 0.75f))
                drawCircle(Color(0xFF0A0A1E), w * 0.0035f, Offset(ghx - w * 0.006f, ghy - w * 0.006f))
                drawCircle(Color(0xFF0A0A1E), w * 0.0035f, Offset(ghx + w * 0.006f, ghy - w * 0.006f))
                // Heart — two bumps
                val hx = w * 0.88f; val hy = h * 0.26f
                drawPath(Path().apply {
                    moveTo(hx, hy + w * 0.012f)
                    quadraticBezierTo(hx - w * 0.014f, hy - w * 0.004f, hx - w * 0.008f, hy - w * 0.012f)
                    quadraticBezierTo(hx - w * 0.002f, hy - w * 0.014f, hx, hy - w * 0.008f)
                    quadraticBezierTo(hx + w * 0.002f, hy - w * 0.014f, hx + w * 0.008f, hy - w * 0.012f)
                    quadraticBezierTo(hx + w * 0.014f, hy - w * 0.004f, hx, hy + w * 0.012f)
                    close()
                }, Color(0xFFFF5FA2).copy(alpha = 0.85f))
                // Floating pixel blocks between the easter eggs
                listOf(Offset(w * 0.64f, h * 0.30f), Offset(w * 0.90f, h * 0.40f)).forEach { p ->
                    drawRoundRect(Color(0xFF00CCFF).copy(alpha = 0.35f), Offset(p.x - w * 0.012f, p.y - w * 0.012f), Size(w * 0.024f, w * 0.024f), CornerRadius(3f), style = Stroke(1f))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF02020A).copy(alpha = 0.6f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF00FF88), badgeInk = Color(0xFF0A0A1E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFE8F8F0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF00CCFF),
            bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFC8D8D0).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF00FF88).copy(alpha = 0.72f)
        )
        // ═══ ARTISTS — singer at the mic under a warm spotlight, crowd edge ═══
        cat == "ARTISTS" -> SignatureDesign(
            bg = Color(0xFF120D2E), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Deep indigo concert hall, lit from above
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1E5C), Color(0xFF141030), Color(0xFF0A0718))), size = Size(w, h))
                // ── Light-rig truss with colored stage lights across the top ──
                val rigY = h * 0.075f
                drawLine(Color(0xFF6E5FA8).copy(alpha = 0.55f), Offset(w * 0.06f, rigY), Offset(w * 0.94f, rigY), strokeWidth = 2f)
                val rigCols = listOf(Color(0xFFF2C879), Color(0xFF9C8BFF), Color(0xFFFF9AB8), Color(0xFF6FE3C1), Color(0xFF9C8BFF))
                listOf(0.12f, 0.28f, 0.44f, 0.60f, 0.76f, 0.88f).forEachIndexed { i, fx ->
                    val lx = w * fx
                    val col = rigCols[i % rigCols.size]
                    // faint beam cone from each fixture
                    val beam = Path().apply {
                        moveTo(lx, rigY)
                        lineTo(lx - w * 0.07f, h * 0.55f)
                        lineTo(lx + w * 0.07f, h * 0.55f)
                        close()
                    }
                    drawPath(beam, col.copy(alpha = 0.05f))
                    drawCircle(brush = Brush.radialGradient(listOf(col.copy(alpha = 0.55f), col.copy(alpha = 0f))), radius = w * 0.028f, center = Offset(lx, rigY))
                    drawCircle(col.copy(alpha = 0.85f), w * 0.006f, Offset(lx, rigY))
                }
                // ── Main warm spotlight with a cool cross-beam ──
                val lx = w * 0.50f
                val beam = Path().apply {
                    moveTo(lx, -h * 0.02f)
                    lineTo(lx - w * 0.22f, h * 0.68f)
                    lineTo(lx + w * 0.24f, h * 0.68f)
                    close()
                }
                drawPath(beam, Color(0xFFF2C879).copy(alpha = 0.10f))
                drawPath(Path().apply {
                    moveTo(w * 0.30f, -h * 0.02f)
                    lineTo(w * 0.08f, h * 0.62f)
                    lineTo(w * 0.20f, h * 0.64f)
                    close()
                }, Color(0xFF9C8BFF).copy(alpha = 0.07f))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.32f), Color(0xFFF2C879).copy(alpha = 0f))), radius = w * 0.20f, center = Offset(lx, h * 0.58f))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.13f), Color(0xFFF2C879).copy(alpha = 0f))), radius = w * 0.36f, center = Offset(lx, h * 0.50f))
                // Cool counter-glow from the left
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF9C8BFF).copy(alpha = 0.16f), Color(0xFF9C8BFF).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.16f, h * 0.38f))
                // ── Stage floor with warm edge glow + sound-wave arcs ──
                drawRect(Brush.verticalGradient(listOf(Color(0xFF8F7BFF).copy(alpha = 0.0f), Color(0xFF8F7BFF).copy(alpha = 0.22f))), topLeft = Offset(0f, h * 0.84f), size = Size(w, h * 0.16f))
                drawLine(Color(0xFFF2C879).copy(alpha = 0.18f), Offset(w * 0.04f, h * 0.84f), Offset(w * 0.96f, h * 0.84f), strokeWidth = 3f)
                drawLine(Color(0xFFB9A8FF).copy(alpha = 0.50f), Offset(w * 0.04f, h * 0.84f), Offset(w * 0.96f, h * 0.84f), strokeWidth = 1.4f)
                listOf(0.30f, 0.22f, 0.15f).forEachIndexed { i, r ->
                    drawArc(Color(0xFF9C8BFF).copy(alpha = 0.16f - i * 0.04f), -70f, 60f, false, Offset(w * 0.16f - r * w * 0.5f, h * 0.70f), Size(w * r, w * r), style = Stroke(1f))
                }
                // ── Crowd silhouettes, some with raised hands ──
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 18) {
                    val hx = w * 0.03f + i * w * 0.055f
                    val r = w * 0.011f + (i % 3) * w * 0.002f
                    drawCircle(Color(0xFF0A0718).copy(alpha = 0.88f), r, Offset(hx, h * 0.90f - r * 0.6f))
                    drawRoundRect(Color(0xFF0A0718).copy(alpha = 0.88f), topLeft = Offset(hx - r * 0.7f, h * 0.90f - r * 0.4f), size = Size(r * 1.4f, r * 1.4f), cornerRadius = CornerRadius(2f))
                    if (i % 4 == 1) {
                        drawLine(Color(0xFF0A0718).copy(alpha = 0.80f), Offset(hx - r * 0.6f, h * 0.90f - r * 0.4f), Offset(hx - r * 0.9f, h * 0.90f - r * 1.5f), strokeWidth = 1.1f)
                        drawCircle(Color(0xFF0A0718).copy(alpha = 0.80f), r * 0.30f, Offset(hx - r * 0.9f, h * 0.90f - r * 1.5f))
                    }
                    if (i % 5 == 2) {
                        drawLine(Color(0xFF0A0718).copy(alpha = 0.80f), Offset(hx + r * 0.6f, h * 0.90f - r * 0.4f), Offset(hx + r * 1.0f, h * 0.90f - r * 1.6f), strokeWidth = 1.1f)
                        drawCircle(Color(0xFF0A0718).copy(alpha = 0.80f), r * 0.30f, Offset(hx + r * 1.0f, h * 0.90f - r * 1.6f))
                    }
                }
                // ── Singer with warm rim-light halo at the ball-head mic ──
                val sx = w * 0.42f; val sy = h * 0.60f
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.35f), Color(0xFFF2C879).copy(alpha = 0f))), radius = w * 0.09f, center = Offset(sx, sy - h * 0.05f))
                drawCircle(Color(0xFF241A4A), w * 0.030f, Offset(sx, sy - h * 0.05f))
                drawLine(Color(0xFF241A4A), Offset(sx, sy - h * 0.02f), Offset(sx, sy + h * 0.10f), strokeWidth = 3.4f)
                drawLine(Color(0xFF241A4A), Offset(sx, sy - h * 0.01f), Offset(sx - w * 0.045f, sy + h * 0.03f), strokeWidth = 2.6f)
                drawLine(Color(0xFF241A4A), Offset(sx, sy - h * 0.01f), Offset(sx + w * 0.045f, sy + h * 0.04f), strokeWidth = 2.6f)
                // Mic stand
                val mx = w * 0.56f
                drawLine(Color(0xFFEAE4FF).copy(alpha = 0.75f), Offset(mx, sy - h * 0.07f), Offset(mx, h * 0.86f), strokeWidth = 1.6f)
                drawLine(Color(0xFFEAE4FF).copy(alpha = 0.55f), Offset(mx - w * 0.020f, h * 0.86f), Offset(mx + w * 0.020f, h * 0.86f), strokeWidth = 1.6f)
                drawLine(Color(0xFFEAE4FF).copy(alpha = 0.40f), Offset(mx - w * 0.014f, h * 0.88f), Offset(mx + w * 0.014f, h * 0.88f), strokeWidth = 1.2f)
                drawCircle(Color(0xFFEAE4FF).copy(alpha = 0.90f), w * 0.020f, Offset(mx, sy - h * 0.09f))
                for (i in 0 until 4) {
                    val a = Math.toRadians((90.0 + i * 24)).toFloat()
                    drawLine(Color(0xFF120D2E).copy(alpha = 0.5f), Offset(mx + kotlin.math.cos(a) * w * 0.010f, sy - h * 0.09f + kotlin.math.sin(a) * w * 0.010f), Offset(mx + kotlin.math.cos(a) * w * 0.020f, sy - h * 0.09f + kotlin.math.sin(a) * w * 0.020f), strokeWidth = 0.8f)
                }
                // ── Confetti + sparkles in the beams ──
                val cols = listOf(Color(0xFFF2C879), Color(0xFF9C8BFF), Color(0xFFFF9AB8), Color(0xFF6FE3C1))
                for (i in 0 until 30) {
                    val x = w * 0.22f + ((s * (i+1) * 7919) % 100) / 100f * w * 0.56f
                    val y = h * 0.10f + ((s * (i+1) * 6271) % 100) / 100f * h * 0.62f
                    drawCircle(cols[i % 4].copy(alpha = 0.42f), 1.2f + (i % 3) * 0.5f, Offset(x, y))
                    if (i % 5 == 0) drawLine(cols[i % 4].copy(alpha = 0.25f), Offset(x, y - 3f), Offset(x + 2.5f, y + 1.5f), strokeWidth = 1f)
                    if (i % 7 == 3) {
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(x - 2.5f, y), Offset(x + 2.5f, y), strokeWidth = 0.8f)
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(x, y - 2.5f), Offset(x, y + 2.5f), strokeWidth = 0.8f)
                    }
                }
                // Haze + vignette
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF050308).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.42f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF9C8BFF), badgeInk = Color(0xFF120D2E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = BungeeFontFamily, titleSize = 27.sp, titleLineHeight = 30.sp, titleColor = Color(0xFFF3EFFF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF9C8BFF),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFDCD3FA).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF9C8BFF).copy(alpha = 0.72f)
        )
        // ═══ ALBUMS — vinyl spinning on a turntable under warm amber light ═══
        cat == "ALBUMS" -> SignatureDesign(
            bg = Color(0xFF171014), cornerRadius = 8f,
            layout = SignatureLayout.BOTTOM,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF3A2320), Color(0xFF171014), Color(0xFF0B0709)), center = Offset(w * 0.5f, h * 0.32f), radius = w * 0.9f), size = Size(w, h))
                // Warm amber glow behind the deck
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE8A84C).copy(alpha = 0.30f), Color(0xFFE8A84C).copy(alpha = 0f))), radius = w * 0.46f, center = Offset(w * 0.50f, h * 0.32f))
                val cx = w * 0.50f; val cy = h * 0.32f
                // ── Turntable plinth under the record ──
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF2E201C), Color(0xFF1A110E)), startY = cy - w * 0.24f), topLeft = Offset(cx - w * 0.30f, cy - w * 0.24f), size = Size(w * 0.60f, w * 0.40f), cornerRadius = CornerRadius(6f))
                drawRoundRect(Color(0xFFE8A84C).copy(alpha = 0.25f), topLeft = Offset(cx - w * 0.30f, cy - w * 0.24f), size = Size(w * 0.60f, 1.5f), cornerRadius = CornerRadius(1f))
                // Vinyl disc with sheen
                drawCircle(Color(0xFF120C0E), w * 0.215f, Offset(cx, cy))
                drawCircle(brush = Brush.linearGradient(listOf(Color(0xFF3A3234), Color(0xFF1A1416), Color(0xFF0D0A0B)), start = Offset(cx - w * 0.2f, cy - w * 0.2f), end = Offset(cx + w * 0.2f, cy + w * 0.2f)), radius = w * 0.205f, center = Offset(cx, cy))
                for (i in 0 until 20) {
                    drawCircle(Color(0xFFD9BFA6).copy(alpha = 0.16f), w * 0.20f - i * w * 0.009f, Offset(cx, cy), style = Stroke(0.7f))
                }
                // Light sheen arc across the grooves
                drawArc(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f), Color.White.copy(alpha = 0.10f)), start = Offset(cx - w * 0.18f, cy - w * 0.18f), end = Offset(cx + w * 0.18f, cy + w * 0.18f)), 300f, 110f, false, Offset(cx - w * 0.17f, cy - w * 0.17f), Size(w * 0.34f, w * 0.34f), style = Stroke(2.5f))
                // Crimson label + spindle
                drawCircle(Color(0xFFB93B2C), w * 0.082f, Offset(cx, cy))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE2634F), Color(0xFFB93B2C), Color(0xFF6E1B14)), center = Offset(cx - w * 0.02f, cy - w * 0.02f), radius = w * 0.09f), radius = w * 0.078f, center = Offset(cx, cy))
                drawCircle(Color(0xFFF2E4D0).copy(alpha = 0.50f), w * 0.013f, Offset(cx, cy))
                // ── Tonearm pivoting from the plinth's top-right ──
                val ax = cx + w * 0.26f; val ay = cy - w * 0.21f
                drawCircle(Color(0xFFB9A48C).copy(alpha = 0.9f), w * 0.014f, Offset(ax, ay))
                drawLine(Color(0xFFD9C4A8), Offset(ax, ay), Offset(cx - w * 0.10f, cy + w * 0.02f), strokeWidth = 2.2f)
                drawLine(Color(0xFFD9C4A8), Offset(cx - w * 0.10f, cy + w * 0.02f), Offset(cx - w * 0.135f, cy + w * 0.075f), strokeWidth = 1.6f)
                drawLine(Color(0xFF8F7B62), Offset(cx - w * 0.14f, cy + w * 0.085f), Offset(cx - w * 0.115f, cy + w * 0.075f), strokeWidth = 1.2f)
                drawCircle(Color(0xFFE8A84C).copy(alpha = 0.6f), w * 0.006f, Offset(cx - w * 0.127f, cy + w * 0.08f))
                // Echo arcs rippling to the right
                listOf(0.34f, 0.27f, 0.21f).forEachIndexed { i, r ->
                    drawArc(Color(0xFFE8A84C).copy(alpha = 0.14f - i * 0.03f), -50f, 85f, false, Offset(w * 0.72f + (0.03f * i) * w, h * 0.04f + (0.04f * i) * h), Size(w * r, w * r), style = Stroke(1.2f))
                }
                // Dust motes in the beam
                for (i in 0 until 16) {
                    val x = w * 0.58f + ((i * 3571) % 100) / 100f * w * 0.36f
                    val y = h * 0.04f + ((i * 4201) % 100) / 100f * h * 0.32f
                    drawCircle(Color(0xFFF2D9A8).copy(alpha = 0.20f), 0.8f + (i % 3) * 0.5f, Offset(x, y))
                }
                // Warm reflection pool under the deck
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE8A84C).copy(alpha = 0.08f), Color(0xFFE8A84C).copy(alpha = 0f))), radius = w * 0.14f, center = Offset(cx, cy + w * 0.18f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF060304).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.38f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFB93B2C), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF7E9E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE2634F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE2D2C8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8A84C).copy(alpha = 0.72f)
        )
        // ═══ SONGS — glowing waveform, floating notes, dusk bokeh ═══
        cat == "SONGS" -> SignatureDesign(
            bg = Color(0xFF261023), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3A1230), Color(0xFF221022), Color(0xFF120811))), size = Size(w, h))
                // One diffused orb, top-right — quiet, not busy
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFF5FA2).copy(alpha = 0.22f), Color(0xFFFF5FA2).copy(alpha = 0f))), radius = w * 0.34f, center = Offset(w * 0.82f, h * 0.16f))
                // One clean waveform — 12 rounded bars, soft gradient, no glow halos
                val bars = intArrayOf(4, 9, 14, 10, 20, 12, 24, 14, 18, 10, 8, 4)
                val bw = w * 0.045f
                val baseY = h * 0.80f
                bars.forEachIndexed { i, v ->
                    val x = w * 0.10f + i * (bw + w * 0.020f)
                    val bh = h * 0.28f * (v / 24f)
                    val top = baseY - bh
                    drawRoundRect(brush = Brush.verticalGradient(listOf(Color(0xFFFF9AB8), Color(0xFFFF5FA2), Color(0xFFB32D6B)), startY = top), topLeft = Offset(x, top), size = Size(bw, bh), cornerRadius = CornerRadius(bw / 2))
                }
                // Quiet baseline under the bars
                drawLine(Color(0xFFFF9AB8).copy(alpha = 0.16f), Offset(w * 0.08f, baseY + h * 0.025f), Offset(w * 0.92f, baseY + h * 0.025f), strokeWidth = 1f)
                // One floating note above the tallest bars
                fun note(cx: Float, cy: Float, s: Float, a: Float) {
                    drawCircle(Color(0xFFF7D9E8).copy(alpha = a), s * 0.34f, Offset(cx - s * 0.10f, cy - s * 0.05f))
                    drawLine(Color(0xFFF7D9E8).copy(alpha = a), Offset(cx + s * 0.24f, cy - s * 0.34f), Offset(cx + s * 0.24f, cy + s * 0.62f), strokeWidth = 1.4f)
                    drawPath(Path().apply { moveTo(cx + s * 0.24f, cy + s * 0.62f); cubicTo(cx + s * 0.24f, cy + s * 0.72f, cx + s * 0.02f, cy + s * 0.78f, cx - s * 0.06f, cy + s * 0.60f); cubicTo(cx - s * 0.14f, cy + s * 0.42f, cx + s * 0.02f, cy + s * 0.36f, cx + s * 0.24f, cy + s * 0.46f) }, Color(0xFFF7D9E8).copy(alpha = a))
                }
                note(w * 0.82f, h * 0.30f, w * 0.040f, 0.55f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF0A0409).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF5FA2), badgeInk = Color(0xFF261023),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFBDFEB),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF9AB8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE8C8D8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF5FA2).copy(alpha = 0.72f)
        )
        // ═══ DIRECTORS — clapperboard under a warm key light, gold trim ═══
        cat == "DIRECTORS" -> SignatureDesign(
            bg = Color(0xFF1B1713), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF3B3226), Color(0xFF1B1713), Color(0xFF0C0A08)), center = Offset(w * 0.5f, h * 0.42f), radius = w * 0.9f), size = Size(w, h))
                // Warm key glow behind the board
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.20f), Color(0xFFF2C879).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.52f, h * 0.50f))
                // Soft top light cone
                drawPath(Path().apply {
                    moveTo(w * 0.40f, -h * 0.02f)
                    lineTo(w * 0.30f, h * 0.55f)
                    lineTo(w * 0.74f, h * 0.55f)
                    close()
                }, Color(0xFFF2C879).copy(alpha = 0.05f))
                // Clapperboard — one clean focal piece, centered
                val cw = w * 0.40f; val ch = w * 0.15f; val cx = w * 0.52f; val cy = h * 0.52f
                drawPath(Path().apply { moveTo(cx, cy); lineTo(cx + cw, cy - ch * 0.25f); lineTo(cx + cw, cy + ch * 0.20f); lineTo(cx, cy + ch * 0.55f); close() }, Color(0xFF4A3A2A))
                drawPath(Path().apply { moveTo(cx, cy); lineTo(cx + cw, cy - ch * 0.25f); lineTo(cx + cw, cy - ch * 0.52f); lineTo(cx, cy - ch * 0.18f); close() }, Color(0xFF201812))
                // One clean gold diagonal on the flap
                drawLine(Color(0xFFE0C88F).copy(alpha = 0.85f), Offset(cx + cw * 0.10f, cy - ch * 0.14f), Offset(cx + cw * 0.38f, cy - ch * 0.30f), strokeWidth = 3f)
                // Faint board lines on the body
                drawLine(Color(0xFFE0C88F).copy(alpha = 0.22f), Offset(cx + cw * 0.10f, cy + ch * 0.12f), Offset(cx + cw * 0.90f, cy - ch * 0.10f), strokeWidth = 1.2f)
                drawLine(Color(0xFFE0C88F).copy(alpha = 0.22f), Offset(cx + cw * 0.10f, cy + ch * 0.28f), Offset(cx + cw * 0.90f, cy + ch * 0.06f), strokeWidth = 1.2f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF070503).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE0C88F), badgeInk = Color(0xFF1B1713),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF2E9D8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE0C88F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFDCD2BE).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE0C88F).copy(alpha = 0.72f)
        )
        // ═══ ANIMATED MOVIES — rainbow ribbon, comet trail, candy dusk ═══
        cat == "ANIMATED MOVIES" -> SignatureDesign(
            bg = Color(0xFF2A1A3E), cornerRadius = 10f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF4A245C), Color(0xFF241436), Color(0xFF12101F))), size = Size(w, h))
                // One diffused orb, top-right
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFF6B9D).copy(alpha = 0.18f), Color(0xFFFF6B9D).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.80f, h * 0.18f))
                // One elegant rainbow arc — thin even bands, shared center
                val swooshColors = listOf(Color(0xFFFF6B9D), Color(0xFFFFB86B), Color(0xFFFFE66B), Color(0xFF6BE3A0), Color(0xFF6BCBFF))
                val arcCx = w * 0.50f; val arcCy = h * 0.44f; val arcR = w * 0.42f
                swooshColors.forEachIndexed { i, col ->
                    val r = arcR - i * w * 0.011f
                    drawArc(col.copy(alpha = 0.85f - i * 0.06f), 190f, 140f, false, Offset(arcCx - r, arcCy - r), Size(r * 2, r * 2), style = Stroke(w * 0.010f))
                }
                // One star at the head of the arc
                drawStar(w * 0.24f, h * 0.50f, 3.0f, 1.3f, Color(0xFFFFF3C4))
                // Three tiny sparkles, placed, not scattered
                listOf(Pair(w * 0.30f, h * 0.28f), Pair(w * 0.68f, h * 0.66f), Pair(w * 0.80f, h * 0.40f)).forEach { (x, y) ->
                    drawStar(x, y, 1.4f, 0.6f, Color(0xFFFFF3C4).copy(alpha = 0.35f))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF0B0813).copy(alpha = 0.5f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6B9D), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF3E8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFFB86B),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFEBD6F5).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF9AC0).copy(alpha = 0.72f)
        )
        // ═══ AUTHORS — midnight study, inkwell, manuscript lines ═══
        cat == "AUTHORS" -> SignatureDesign(
            bg = Color(0xFF131A2E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF22355C), Color(0xFF131A2E), Color(0xFF0A0F1C))), size = Size(w, h))
                // Warm desk-lamp glow from the corner
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.22f), Color(0xFFF2C879).copy(alpha = 0f))), radius = w * 0.32f, center = Offset(w * 0.78f, h * 0.24f))
                // Manuscript ruled lines — few, faint
                for (i in 0 until 7) {
                    val y = h * 0.22f + i * h * 0.05f
                    drawLine(Color(0xFF9FB4D8).copy(alpha = 0.12f), Offset(w * 0.08f, y), Offset(w * 0.56f, y), strokeWidth = 0.8f)
                }
                // Red margin line
                drawLine(Color(0xFFC94F4F).copy(alpha = 0.35f), Offset(w * 0.13f, h * 0.22f), Offset(w * 0.13f, h * 0.66f), strokeWidth = 1f)
                // Refined inkwell with a gold rim
                val ix = w * 0.76f; val iy = h * 0.56f
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF2A3B66), Color(0xFF10182C)), center = Offset(ix, iy - w * 0.01f), radius = w * 0.10f), radius = w * 0.062f, center = Offset(ix, iy))
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.55f), w * 0.062f, Offset(ix, iy), style = Stroke(1f))
                drawOval(Color(0xFF0B1020), Offset(ix - w * 0.042f, iy + w * 0.042f), Size(w * 0.084f, w * 0.022f))
                // One clean quill arc
                drawPath(Path().apply { moveTo(ix + w * 0.02f, iy - w * 0.03f); cubicTo(ix + w * 0.10f, iy - w * 0.16f, ix + w * 0.18f, iy - w * 0.22f, ix + w * 0.24f, iy - w * 0.26f) }, Color(0xFFF2E4C8).copy(alpha = 0.60f), style = Stroke(1.6f))
                drawLine(Color(0xFFF2E4C8).copy(alpha = 0.35f), Offset(ix + w * 0.16f, iy - w * 0.20f), Offset(ix + w * 0.26f, iy - w * 0.28f), strokeWidth = 1f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF060912).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFF2C879), badgeInk = Color(0xFF131A2E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF2E9D8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFF2C879),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFC9D4EC).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFF2C879).copy(alpha = 0.72f)
        )
        // ═══ PAINTERS — easel with canvas, palette, brush ═══
        cat == "PAINTERS" -> SignatureDesign(
            bg = Color(0xFF201A14), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF4A3A26), Color(0xFF201A14), Color(0xFF0F0C08)), center = Offset(w * 0.5f, h * 0.30f), radius = w * 0.95f), size = Size(w, h))
                // Warm north-light glow from above
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2E4C8).copy(alpha = 0.14f), Color(0xFFF2E4C8).copy(alpha = 0f))), radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.22f))
                // Easel frame, right — A-frame with crossbar
                val ex = w * 0.70f
                drawLine(Color(0xFF8A6B4A).copy(alpha = 0.70f), Offset(ex - w * 0.06f, h * 0.24f), Offset(ex + w * 0.005f, h * 0.80f), strokeWidth = 2.6f)
                drawLine(Color(0xFF8A6B4A).copy(alpha = 0.70f), Offset(ex + w * 0.06f, h * 0.24f), Offset(ex + w * 0.005f, h * 0.80f), strokeWidth = 2.6f)
                drawLine(Color(0xFF8A6B4A).copy(alpha = 0.55f), Offset(ex - w * 0.045f, h * 0.60f), Offset(ex + w * 0.045f, h * 0.60f), strokeWidth = 1.8f)
                // Canvas resting on the easel — clean minimal abstract: sky, sun, one hill band
                val cx = ex + w * 0.005f; val cy = h * 0.40f; val cw = w * 0.22f; val ch = w * 0.28f
                drawRoundRect(brush = Brush.verticalGradient(listOf(Color(0xFFF5F0E4), Color(0xFFD8CDB4)), startY = cy - ch / 2), topLeft = Offset(cx - cw / 2, cy - ch / 2), size = Size(cw, ch), cornerRadius = CornerRadius(2f))
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF2C879), Color(0xFFE86B4F)), startY = cy - ch / 2), topLeft = Offset(cx - cw / 2 + w * 0.010f, cy - ch / 2 + w * 0.010f), size = Size(cw - w * 0.020f, ch * 0.52f))
                drawCircle(Color(0xFFF2E4C8).copy(alpha = 0.95f), cw * 0.075f, Offset(cx, cy - ch * 0.10f))
                drawOval(Color(0xFF3A2E4A), topLeft = Offset(cx - cw * 0.30f, cy + ch * 0.06f), size = Size(cw * 0.60f, ch * 0.22f))
                // Palette, bottom-left — clean, three wells
                val px = w * 0.24f; val py = h * 0.70f
                drawOval(brush = Brush.radialGradient(listOf(Color(0xFF8A6B4A), Color(0xFF5E452E)), center = Offset(px, py), radius = w * 0.13f), topLeft = Offset(px - w * 0.145f, py - w * 0.11f), size = Size(w * 0.30f, w * 0.16f))
                drawCircle(Color(0xFF201A14), w * 0.018f, Offset(px + w * 0.115f, py + w * 0.035f))
                listOf(Color(0xFFE8544F), Color(0xFF4FA8E8), Color(0xFFE8C84F)).forEachIndexed { i, col ->
                    drawCircle(col.copy(alpha = 0.95f), w * 0.010f, Offset(px - w * 0.075f + i * w * 0.045f, py - w * 0.03f))
                }
                // One clean brush across the canvas
                drawLine(Color(0xFFB08A4A), Offset(w * 0.50f, h * 0.30f), Offset(w * 0.66f, h * 0.46f), strokeWidth = 3.0f)
                drawLine(Color(0xFFC9B89A), Offset(w * 0.66f, h * 0.46f), Offset(w * 0.69f, h * 0.49f), strokeWidth = 2.0f)
                drawPath(Path().apply { moveTo(w * 0.69f, h * 0.49f); lineTo(w * 0.72f, h * 0.53f); lineTo(w * 0.68f, h * 0.52f); close() }, Color(0xFFE8544F).copy(alpha = 0.85f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF080603).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8C84F), badgeInk = Color(0xFF201A14),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF5EAD8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8C84F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D2BC).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8C84F).copy(alpha = 0.72f)
        )
        // ═══ ARTWORKS — quiet gallery, framed pieces BELOW the title zone ═══
        cat == "ARTWORKS" -> SignatureDesign(
            bg = Color(0xFF232327), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3E3E44), Color(0xFF232327), Color(0xFF131316))), size = Size(w, h))
                // One soft spotlight pool, center
                val cone = Path().apply {
                    moveTo(w * 0.42f, 0f)
                    lineTo(w * 0.30f, h * 0.62f)
                    lineTo(w * 0.70f, h * 0.62f)
                    close()
                }
                drawPath(cone, Color(0xFFFFF3C4).copy(alpha = 0.05f))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFF3C4).copy(alpha = 0.12f), Color(0xFFFFF3C4).copy(alpha = 0f))), radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.64f))
                // One framed piece, centered — clean minimal abstract: sun + hill
                val cx = w * 0.50f; val cy = h * 0.60f; val fw = w * 0.26f; val fh = w * 0.34f
                drawRoundRect(Color(0xFF8A8070), topLeft = Offset(cx - fw / 2, cy - fh / 2), size = Size(fw, fh), cornerRadius = CornerRadius(2f))
                drawRoundRect(Color(0xFF1C1C1F), topLeft = Offset(cx - fw / 2 + w * 0.010f, cy - fh / 2 + w * 0.010f), size = Size(fw - w * 0.020f, fh - w * 0.020f), cornerRadius = CornerRadius(1.5f))
                val mw = fw - w * 0.045f; val mh = fh - w * 0.045f
                drawRect(Brush.verticalGradient(listOf(Color(0xFFE86B4F), Color(0xFF7A2A5E)), startY = cy - mh / 2), topLeft = Offset(cx - mw / 2, cy - mh / 2), size = Size(mw, mh))
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.90f), mw * 0.10f, Offset(cx, cy - mh * 0.14f))
                drawOval(Color(0xFF2E2E4A), topLeft = Offset(cx - mw * 0.30f, cy + mh * 0.08f), size = Size(mw * 0.60f, mh * 0.24f))
                // Museum floor line
                drawLine(Color(0xFF8A8070).copy(alpha = 0.30f), Offset(w * 0.04f, h * 0.90f), Offset(w * 0.96f, h * 0.90f), strokeWidth = 1.2f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF0B0B0D).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE0C88F), badgeInk = Color(0xFF232327),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF2EDE2),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE0C88F),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD8D2C4).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE0C88F).copy(alpha = 0.72f)
        )
        // ═══ SCIENTISTS — blueprint grid, molecule, bubbling beaker ═══
        cat == "SCIENTISTS" -> SignatureDesign(
            bg = Color(0xFF14202E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF23405C), Color(0xFF14202E), Color(0xFF0B121B))), size = Size(w, h))
                // Soft teal glow, center-right — no grid lines
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF4FD8C8).copy(alpha = 0.14f), Color(0xFF4FD8C8).copy(alpha = 0f))), radius = w * 0.34f, center = Offset(w * 0.68f, h * 0.42f))
                // Molecule model — clean: central atom + 4 bonds
                val cx = w * 0.30f; val cy = h * 0.30f
                listOf(Offset(cx + w * 0.12f, cy + w * 0.05f), Offset(cx - w * 0.12f, cy + w * 0.04f), Offset(cx + w * 0.02f, cy - w * 0.11f), Offset(cx - w * 0.10f, cy - w * 0.06f)).forEach { p ->
                    drawLine(Color(0xFF9FD8E8).copy(alpha = 0.50f), Offset(cx, cy), p, strokeWidth = 1.4f)
                }
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE8F8FF), Color(0xFF6BB8E8)), center = Offset(cx - w * 0.012f, cy - w * 0.012f), radius = w * 0.045f), radius = w * 0.030f, center = Offset(cx, cy))
                listOf(Offset(cx + w * 0.12f, cy + w * 0.05f), Offset(cx - w * 0.12f, cy + w * 0.04f), Offset(cx + w * 0.02f, cy - w * 0.11f), Offset(cx - w * 0.10f, cy - w * 0.06f)).forEachIndexed { i, p ->
                    drawCircle(Color(if (i % 2 == 0) 0xFFE8544F else 0xFF4FD8C8), w * 0.015f, p)
                }
                // Conical beaker — clean, three rising bubbles
                val bx = w * 0.70f; val by = h * 0.52f
                val beaker = Path().apply {
                    moveTo(bx - w * 0.06f, by - w * 0.15f)
                    lineTo(bx - w * 0.055f, by - w * 0.135f)
                    lineTo(bx - w * 0.075f, by + w * 0.165f)
                    lineTo(bx + w * 0.075f, by + w * 0.165f)
                    lineTo(bx + w * 0.055f, by - w * 0.135f)
                    lineTo(bx + w * 0.06f, by - w * 0.15f)
                }
                drawPath(beaker, Color(0xFFC9E8F2).copy(alpha = 0.35f), style = Stroke(1.6f))
                drawPath(Path().apply { moveTo(bx - w * 0.065f, by + w * 0.09f); lineTo(bx + w * 0.065f, by + w * 0.09f); lineTo(bx + w * 0.075f, by + w * 0.165f); lineTo(bx - w * 0.075f, by + w * 0.165f); close() }, Color(0xFF4FD8C8).copy(alpha = 0.30f))
                listOf(1, 3, 5).forEach { i ->
                    drawCircle(Color(0xFF9FE8E0).copy(alpha = 0.55f), 1.3f, Offset(bx - w * 0.02f + i * w * 0.018f, by + w * 0.06f - i * w * 0.016f))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF070D14).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FD8C8), badgeInk = Color(0xFF0B121B),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFEAF6F8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4FD8C8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4DCE2).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF4FD8C8).copy(alpha = 0.72f)
        )
        // ═══ DISCOVERIES — explorer's map: compass, dotted trail, contours ═══
        cat == "DISCOVERIES" -> SignatureDesign(
            bg = Color(0xFF2B2416), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF5E4C26), Color(0xFF2B2416), Color(0xFF161206)), center = Offset(w * 0.6f, h * 0.35f), radius = w * 0.95f), size = Size(w, h))
                // Two aged-map contour rings, not five
                drawOval(Color(0xFFC9A24F).copy(alpha = 0.12f), topLeft = Offset(w * 0.06f, h * 0.52f), size = Size(w * 0.60f, w * 0.24f), style = Stroke(1f))
                drawOval(Color(0xFFC9A24F).copy(alpha = 0.08f), topLeft = Offset(w * 0.10f, h * 0.56f), size = Size(w * 0.52f, w * 0.20f), style = Stroke(1f))
                // Compass rose — the focal piece, center-right
                val cx = w * 0.78f; val cy = h * 0.64f
                drawCircle(Color(0xFFE8D9A8).copy(alpha = 0.28f), w * 0.10f, Offset(cx, cy), style = Stroke(1.2f))
                for (i in 0 until 8) {
                    val a = Math.toRadians((45.0 * i)).toFloat()
                    val r1 = w * 0.05f; val r2 = w * 0.095f
                    drawLine(Color(0xFFE8D9A8).copy(alpha = if (i % 4 == 0) 0.85f else 0.45f), Offset(cx, cy), Offset(cx + kotlin.math.cos(a) * r2, cy + kotlin.math.sin(a) * r2), strokeWidth = 1.4f)
                    drawLine(Color(0xFFE8D9A8).copy(alpha = if (i % 4 == 0) 0.85f else 0.45f), Offset(cx, cy), Offset(cx + kotlin.math.cos(a) * r1, cy + kotlin.math.sin(a) * r1), strokeWidth = 1.4f)
                }
                drawCircle(Color(0xFFE8C84F), w * 0.014f, Offset(cx, cy))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE8C84F).copy(alpha = 0.25f), Color(0xFFE8C84F).copy(alpha = 0f))), radius = w * 0.14f, center = Offset(cx, cy))
                // Short dotted trail into the compass
                for (i in 0 until 12) {
                    val t = i / 11f
                    val tx = w * 0.10f + t * (cx - w * 0.10f) + kotlin.math.sin(t * 3f) * w * 0.02f
                    val ty = h * 0.80f - kotlin.math.sin(t * 2.0f) * w * 0.06f
                    drawCircle(Color(0xFFE8C84F).copy(alpha = 0.55f), 1.5f, Offset(tx, ty))
                }
                // Four faint sun rays, top-left
                for (i in 0 until 4) {
                    val a = Math.toRadians((360.0 * i / 8)).toFloat()
                    drawLine(Color(0xFFF2D98A).copy(alpha = 0.12f), Offset(w * 0.12f, h * 0.10f), Offset(w * 0.12f + kotlin.math.cos(a) * w * 0.14f, h * 0.10f + kotlin.math.sin(a) * w * 0.14f), strokeWidth = 1.2f)
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF0D0A03).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8C84F), badgeInk = Color(0xFF2B2416),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF2E9CE),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8C84F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFDCD2B4).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8C84F).copy(alpha = 0.72f)
        )
        // ═══ SERIES — TV playing a scene, episode chips, progress bar ═══
        cat == "SERIES" -> SignatureDesign(
            bg = Color(0xFF1E1226), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF4A2A4A), Color(0xFF1E1226), Color(0xFF0D0812)), center = Offset(w * 0.55f, h * 0.35f), radius = w * 0.9f), size = Size(w, h))
                // Soft crimson glow behind the TV
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE84F4F).copy(alpha = 0.16f), Color(0xFFE84F4F).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.40f, h * 0.34f))
                // TV with a clean scene on screen
                val tvx = w * 0.24f; val tvy = h * 0.24f; val tvw = w * 0.52f; val tvh = w * 0.36f
                drawRoundRect(brush = Brush.verticalGradient(listOf(Color(0xFF5E4A5E), Color(0xFF2E2230)), startY = tvy), topLeft = Offset(tvx - w * 0.014f, tvy - w * 0.014f), size = Size(tvw + w * 0.028f, tvh + w * 0.028f), cornerRadius = CornerRadius(6f))
                // Screen: dusk gradient sky + a simple mountain silhouette
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE86B4F), Color(0xFF7A2A5E), Color(0xFF1E0A2E)), startY = tvy), topLeft = Offset(tvx, tvy), size = Size(tvw, tvh), cornerRadius = CornerRadius(4f))
                drawPath(Path().apply {
                    moveTo(tvx, tvy + tvh * 0.74f)
                    lineTo(tvx + tvw * 0.24f, tvy + tvh * 0.55f)
                    lineTo(tvx + tvw * 0.50f, tvy + tvh * 0.70f)
                    lineTo(tvx + tvw * 0.78f, tvy + tvh * 0.52f)
                    lineTo(tvx + tvw, tvy + tvh * 0.66f)
                    lineTo(tvx + tvw, tvy + tvh)
                    lineTo(tvx, tvy + tvh)
                    close()
                }, Color(0xFF140A1E))
                drawCircle(Color(0xFFF2C879).copy(alpha = 0.85f), tvw * 0.055f, Offset(tvx + tvw * 0.72f, tvy + tvh * 0.28f))
                // Progress bar at the bottom of the screen
                drawRoundRect(Color(0xFF3A2E4E), topLeft = Offset(tvx + tvw * 0.06f, tvy + tvh * 0.90f), size = Size(tvw * 0.88f, tvh * 0.045f), cornerRadius = CornerRadius(2f))
                drawRoundRect(Color(0xFFE84F4F), topLeft = Offset(tvx + tvw * 0.06f, tvy + tvh * 0.90f), size = Size(tvw * 0.48f, tvh * 0.045f), cornerRadius = CornerRadius(2f))
                // TV stand
                drawLine(Color(0xFF3E3240), Offset(tvx + tvw * 0.5f, tvy + tvh + w * 0.014f), Offset(tvx + tvw * 0.5f, tvy + tvh + w * 0.07f), strokeWidth = 3f)
                drawLine(Color(0xFF3E3240), Offset(tvx + tvw * 0.28f, tvy + tvh + w * 0.075f), Offset(tvx + tvw * 0.72f, tvy + tvh + w * 0.075f), strokeWidth = 3f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF070408).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE84F4F), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF5E6EA),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFF2C879),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0C8D2).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE84F4F).copy(alpha = 0.72f)
        )
        // ═══ ANIME — sakura petals, sunburst, lens flare ═══
        cat == "ANIME" -> SignatureDesign(
            bg = Color(0xFF3A1A4A), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF7A3A8A), Color(0xFF3A1A4A), Color(0xFF1A0E24))), size = Size(w, h))
                // Soft sun glow top-right, with fewer fainter rays
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFF3C4).copy(alpha = 0.28f), Color(0xFFFFF3C4).copy(alpha = 0f))), radius = w * 0.20f, center = Offset(w * 0.86f, h * 0.10f))
                for (i in 0 until 8) {
                    val a = Math.toRadians((360.0 * i / 8)).toFloat()
                    drawLine(Color(0xFFFFF3C4).copy(alpha = 0.12f), Offset(w * 0.86f, h * 0.10f), Offset(w * 0.86f + kotlin.math.cos(a) * w * 0.26f, h * 0.10f + kotlin.math.sin(a) * w * 0.26f), strokeWidth = 1.2f)
                }
                // One pink glow pocket, bottom-left
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFF6B9D).copy(alpha = 0.20f), Color(0xFFFF6B9D).copy(alpha = 0f))), radius = w * 0.26f, center = Offset(w * 0.18f, h * 0.34f))
                // Gentle falling petals — fewer, flowing on one diagonal
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 8) {
                    val t = i / 8f
                    val x = w * 0.14f + t * w * 0.72f + ((s * (i+1) * 7919) % 100) / 100f * w * 0.06f
                    val y = h * 0.14f + t * h * 0.62f
                    val r = 1.6f + (i % 3) * 0.6f
                    drawPath(Path().apply { moveTo(x - r, y); lineTo(x, y - r * 0.7f); lineTo(x + r, y); lineTo(x, y + r * 0.7f); close() }, Color(0xFFFFC0D8).copy(alpha = 0.55f))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF140718).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6B9D), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF0F5),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF9AC0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFF0D4E2).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF6B9D).copy(alpha = 0.72f)
        )
        // ═══ MANGA — bold ink, speed lines, screentone, red slash ═══
        cat == "MANGA" -> SignatureDesign(
            bg = Color(0xFFE8E4DA), cornerRadius = 6f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE8E4DA), Color(0xFFCFC8B8))), size = Size(w, h))
                // Compact screentone patch, top-right
                for (i in 0 until 30) {
                    val dx = w * 0.74f + (i % 8) * w * 0.022f
                    val dy = h * 0.06f + (i / 8) * h * 0.024f
                    drawCircle(Color(0xFF4A4A4A).copy(alpha = 0.35f), 0.7f, Offset(dx, dy))
                }
                // Ink burst base bottom-left — one focal point with crisp rays
                val bx = w * 0.16f; val by = h * 0.86f
                drawCircle(Color(0xFF1A1A1A), w * 0.026f, Offset(bx, by))
                for (i in 0 until 8) {
                    val a = Math.toRadians((360.0 * i / 8)).toFloat()
                    drawLine(Color(0xFF1A1A1A).copy(alpha = 0.80f), Offset(bx, by), Offset(bx + kotlin.math.cos(a) * w * 0.12f, by + kotlin.math.sin(a) * h * 0.07f), strokeWidth = 2.6f)
                }
                // A few bold speed lines radiating up-right
                for (i in 0 until 8) {
                    val a = Math.toRadians((-35.0 + i * 10)).toFloat()
                    val len = w * (0.30f + (i % 3) * 0.05f)
                    drawLine(Color(0xFF1A1A1A).copy(alpha = 0.70f), Offset(bx, by), Offset(bx + kotlin.math.cos(a) * len, by + kotlin.math.sin(a) * len), strokeWidth = 2.2f)
                }
                // Red diagonal slash — the bold focal stroke
                drawLine(Color(0xFFE8342E).copy(alpha = 0.85f), Offset(w * 0.80f, h * 0.06f), Offset(w * 0.56f, h * 0.30f), strokeWidth = 4f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF6A6454).copy(alpha = 0.26f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8342E), badgeInk = Color.White,
            badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF1A1A1A),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8342E),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF3A3A3A).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1A1A1A).copy(alpha = 0.65f)
        )
        // ═══ MANHWA — soft webtoon sky, dreamy arch, sparkles ═══
        cat == "MANHWA" -> SignatureDesign(
            bg = Color(0xFFE8E0F2), cornerRadius = 10f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE8F2), Color(0xFFE8E0F2), Color(0xFFD4E4F5))), size = Size(w, h))
                // One soft pastel cloud, top-left
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFD9E8).copy(alpha = 0.95f), Color(0xFFD9E8FF).copy(alpha = 0f))), radius = w * 0.14f, center = Offset(w * 0.18f, h * 0.20f))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFD9E8).copy(alpha = 0.95f), Color(0xFFFFD9E8).copy(alpha = 0f))), radius = w * 0.10f, center = Offset(w * 0.24f, h * 0.16f))
                // Dreamy arch — the focal piece
                val ax = w * 0.60f; val ay = h * 0.82f
                drawArc(Color(0xFF8A6BA8).copy(alpha = 0.32f), 180f, 180f, false, Offset(ax - w * 0.09f, ay - h * 0.22f), Size(w * 0.18f, w * 0.225f), style = Stroke(2.4f))
                drawLine(Color(0xFF8A6BA8).copy(alpha = 0.32f), Offset(ax - w * 0.09f, ay - h * 0.08f), Offset(ax - w * 0.09f, ay), strokeWidth = 2.4f)
                drawLine(Color(0xFF8A6BA8).copy(alpha = 0.32f), Offset(ax + w * 0.09f, ay - h * 0.08f), Offset(ax + w * 0.09f, ay), strokeWidth = 2.4f)
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFB8D8).copy(alpha = 0.6f), Color(0xFFFFB8D8).copy(alpha = 0f))), radius = w * 0.14f, center = Offset(ax, ay - h * 0.10f))
                // Heart rising from the arch
                val hx = ax; val hy = ay - h * 0.34f
                drawPath(Path().apply {
                    moveTo(hx, hy + w * 0.018f)
                    cubicTo(hx - w * 0.014f, hy - w * 0.012f, hx - w * 0.03f, hy - w * 0.002f, hx, hy + w * 0.020f)
                    cubicTo(hx + w * 0.03f, hy - w * 0.002f, hx + w * 0.014f, hy - w * 0.012f, hx, hy + w * 0.018f)
                }, Color(0xFFFF7AB0).copy(alpha = 0.80f))
                // Four soft sparkles, placed
                listOf(Pair(w * 0.30f, h * 0.30f), Pair(w * 0.80f, h * 0.22f), Pair(w * 0.40f, h * 0.60f), Pair(w * 0.84f, h * 0.52f)).forEach { (x, y) ->
                    drawStar(x, y, 1.4f, 0.6f, Color(0xFFB98BFF).copy(alpha = 0.50f))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF8A7A9A).copy(alpha = 0.22f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.9f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFB98BFF), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF4A3A5E),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8A5AA8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF5E5070).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF8A5AA8).copy(alpha = 0.70f)
        )
        // ═══ MYTHOLOGY — gold meander, temple columns on dark marble ═══
        cat == "MYTHOLOGY" -> SignatureDesign(
            bg = Color(0xFF1E1A14), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF4A4030), Color(0xFF1E1A14), Color(0xFF0E0C08)), center = Offset(w * 0.5f, h * 0.30f), radius = w * 0.9f), size = Size(w, h))
                // Gold glow behind the temple (no marble vein noise)
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE0C84F).copy(alpha = 0.22f), Color(0xFFE0C84F).copy(alpha = 0f))), radius = w * 0.36f, center = Offset(w * 0.30f, h * 0.38f))
                // Greek meander border along the top
                val my = h * 0.07f
                for (i in 0 until 8) {
                    val x = w * 0.06f + i * w * 0.11f
                    drawPath(Path().apply { moveTo(x, my); lineTo(x + w * 0.06f, my); lineTo(x + w * 0.06f, my + h * 0.03f); lineTo(x, my + h * 0.03f); lineTo(x, my + h * 0.012f); lineTo(x + w * 0.032f, my + h * 0.012f); lineTo(x + w * 0.032f, my + h * 0.021f); lineTo(x, my + h * 0.021f) }, Color(0xFFE0C84F).copy(alpha = 0.55f), style = Stroke(1.2f))
                }
                // Temple pediment + columns on a stylobate step
                drawPath(Path().apply { moveTo(w * 0.10f, h * 0.52f); lineTo(w * 0.30f, h * 0.34f); lineTo(w * 0.50f, h * 0.52f); close() }, Color(0xFFF2E9D8).copy(alpha = 0.14f), style = Stroke(1.4f))
                drawLine(Color(0xFFF2E9D8).copy(alpha = 0.20f), Offset(w * 0.10f, h * 0.52f), Offset(w * 0.50f, h * 0.52f), strokeWidth = 1.4f)
                for (i in 0 until 4) {
                    val cx = w * 0.13f + i * w * 0.08f
                    drawLine(Color(0xFFF2E9D8).copy(alpha = 0.30f), Offset(cx, h * 0.52f), Offset(cx, h * 0.72f), strokeWidth = 2.2f)
                    drawLine(Color(0xFFF2E9D8).copy(alpha = 0.18f), Offset(cx - w * 0.012f, h * 0.72f), Offset(cx + w * 0.012f, h * 0.72f), strokeWidth = 1.4f)
                }
                drawLine(Color(0xFFF2E9D8).copy(alpha = 0.22f), Offset(w * 0.09f, h * 0.74f), Offset(w * 0.51f, h * 0.74f), strokeWidth = 1.2f)
                // Laurel wreath at the temple base — five even leaves
                val lx = w * 0.30f; val ly = h * 0.84f
                for (i in 0 until 5) {
                    val a = Math.toRadians((180.0 * i / 4)).toFloat()
                    drawOval(Color(0xFFC9A24F).copy(alpha = 0.55f), topLeft = Offset(lx + kotlin.math.cos(a) * w * 0.060f - w * 0.008f, ly + kotlin.math.sin(a) * w * 0.035f - w * 0.013f), size = Size(w * 0.016f, w * 0.018f))
                }
                drawArc(Color(0xFFE0C84F).copy(alpha = 0.65f), 180f, 180f, false, Offset(lx - w * 0.060f, ly - w * 0.05f), Size(w * 0.12f, w * 0.070f), style = Stroke(1.4f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF080604).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE0C84F), badgeInk = Color(0xFF1E1A14),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF2EAD2),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE0C84F),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD8D0BA).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE0C84F).copy(alpha = 0.72f)
        )
        // ═══ SPORTS — floodlit stadium, field stripes, trophy ═══
        cat == "SPORTS" -> SignatureDesign(
            bg = Color(0xFF0E2A1E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E4A34), Color(0xFF0E2A1E), Color(0xFF06140D))), size = Size(w, h))
                // Floodlight cones from both top corners
                listOf(Pair(Offset(w * 0.10f, -h * 0.02f), Color(0xFFFFF3C4)), Pair(Offset(w * 0.90f, -h * 0.02f), Color(0xFFFFF3C4))).forEach { (c, col) ->
                    val cone = Path().apply { moveTo(c.x, c.y); lineTo(c.x - w * 0.14f, h * 0.60f); lineTo(c.x + w * 0.14f, h * 0.60f); close() }
                    drawPath(cone, col.copy(alpha = 0.06f))
                    drawCircle(brush = Brush.radialGradient(listOf(col.copy(alpha = 0.20f), col.copy(alpha = 0f))), radius = w * 0.18f, center = Offset(c.x, h * 0.50f))
                }
                // Floodlight heads
                drawCircle(Color(0xFFFFF3C4).copy(alpha = 0.9f), w * 0.016f, Offset(w * 0.10f, h * 0.06f))
                drawCircle(Color(0xFFFFF3C4).copy(alpha = 0.9f), w * 0.016f, Offset(w * 0.90f, h * 0.06f))
                // Field stripes — four clean lines
                for (i in 0 until 4) {
                    val y = h * 0.64f + i * h * 0.05f
                    drawLine(Color(0xFFFFFFFF).copy(alpha = 0.10f), Offset(w * 0.10f, y), Offset(w * 0.90f, y), strokeWidth = 1f)
                }
                drawLine(Color(0xFFF2E9D8).copy(alpha = 0.35f), Offset(w * 0.50f, h * 0.62f), Offset(w * 0.50f, h * 0.88f), strokeWidth = 1.6f)
                // Trophy bottom-left
                val tx = w * 0.20f; val ty = h * 0.68f
                drawPath(Path().apply { moveTo(tx - w * 0.028f, ty + h * 0.10f); lineTo(tx + w * 0.028f, ty + h * 0.10f); lineTo(tx + w * 0.020f, ty + h * 0.14f); lineTo(tx - w * 0.020f, ty + h * 0.14f); close() }, Color(0xFFE0C84F).copy(alpha = 0.75f))
                drawPath(Path().apply { moveTo(tx - w * 0.03f, ty + h * 0.06f); cubicTo(tx - w * 0.045f, ty - h * 0.01f, tx + w * 0.045f, ty - h * 0.01f, tx + w * 0.03f, ty + h * 0.06f); lineTo(tx - w * 0.03f, ty + h * 0.06f); close() }, Color(0xFFE0C84F).copy(alpha = 0.6f))
                drawLine(Color(0xFFE0C84F).copy(alpha = 0.8f), Offset(tx, ty - h * 0.02f), Offset(tx, ty + h * 0.02f), strokeWidth = 2.4f)
                // Gold glow on trophy
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE0C84F).copy(alpha = 0.18f), Color(0xFFE0C84F).copy(alpha = 0f))), radius = w * 0.12f, center = Offset(tx, ty))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF020A05).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE0C84F), badgeInk = Color(0xFF0E2A1E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF2F6E8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE0C84F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4D8CA).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE0C84F).copy(alpha = 0.72f)
        )
        // ═══ FOOD — overhead plate, steaming bowl, herbs, crumbs ═══
        cat == "FOOD" -> SignatureDesign(
            bg = Color(0xFF2E1E12), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF5E3820), Color(0xFF2E1E12), Color(0xFF180E06))), size = Size(w, h))
                // Warm glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2A84F).copy(alpha = 0.20f), Color(0xFFF2A84F).copy(alpha = 0f))), radius = w * 0.34f, center = Offset(w * 0.30f, h * 0.40f))
                // Overhead plate
                // Shared table line — plate and bowl both sit on it
                drawLine(Color(0xFF8A5A32).copy(alpha = 0.40f), Offset(w * 0.06f, h * 0.78f), Offset(w * 0.94f, h * 0.78f), strokeWidth = 1.2f)
                drawRect(Brush.verticalGradient(listOf(Color(0xFF8A5A32).copy(alpha = 0.0f), Color(0xFF8A5A32).copy(alpha = 0.12f)), startY = h * 0.78f), topLeft = Offset(0f, h * 0.78f), size = Size(w, h * 0.22f))
                val px = w * 0.30f; val py = h * 0.52f; val pr = w * 0.19f
                // Plate — centered on the table
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF5F0E4), Color(0xFFD8CDB4)), center = Offset(px, py), radius = pr), radius = pr, center = Offset(px, py))
                drawCircle(Color(0xFFE8DCC4), pr * 0.82f, Offset(px, py))
                drawCircle(Color(0xFFC94F3B), pr * 0.34f, Offset(px, py))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE8743B), Color(0xFFC94F3B)), center = Offset(px - pr * 0.1f, py - pr * 0.1f), radius = pr * 0.34f), radius = pr * 0.30f, center = Offset(px, py))
                // One basil leaf on the dish
                val lxx = px - pr * 0.10f; val lyy = py
                drawOval(Color(0xFF4FA84F).copy(alpha = 0.9f), topLeft = Offset(lxx - pr * 0.09f, lyy - pr * 0.05f), size = Size(pr * 0.18f, pr * 0.09f))
                drawLine(Color(0xFF2E6A2E), Offset(lxx - pr * 0.08f, lyy), Offset(lxx + pr * 0.08f, lyy), strokeWidth = 0.9f)
                // Steaming bowl beside it, on the same table
                val bx = w * 0.76f; val by = h * 0.58f
                drawPath(Path().apply { moveTo(bx - w * 0.055f, by); lineTo(bx - w * 0.04f, by + w * 0.085f); lineTo(bx + w * 0.04f, by + w * 0.085f); lineTo(bx + w * 0.055f, by); close() }, Color(0xFFE8DCC4).copy(alpha = 0.85f))
                drawOval(Color(0xFFE8743B).copy(alpha = 0.9f), topLeft = Offset(bx - w * 0.05f, by - w * 0.011f), size = Size(w * 0.10f, w * 0.014f))
                drawLine(Color(0xFFB06A3A).copy(alpha = 0.7f), Offset(bx - w * 0.055f, by), Offset(bx + w * 0.055f, by), strokeWidth = 1f)
                // Steam wisps
                listOf(Pair(Offset(bx - w * 0.02f, by - w * 0.03f), 0f), Pair(Offset(bx + w * 0.02f, by - w * 0.05f), 1f)).forEach { (c, phase) ->
                    val path = Path().apply { moveTo(c.x, c.y); cubicTo(c.x - w * 0.008f, c.y - w * 0.025f, c.x + w * 0.008f, c.y - w * 0.045f, c.x, c.y - w * 0.07f) }
                    drawPath(path, Color(0xFFFFF0E0).copy(alpha = 0.35f), style = Stroke(1.4f))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF0D0703).copy(alpha = 0.50f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFF2A84F), badgeInk = Color(0xFF2E1E12),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFBF0E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFF2A84F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE8D4BC).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFF2A84F).copy(alpha = 0.72f)
        )
        // ═══ INTERNET — globe wireframe, network nodes, browser bar ═══
        cat == "INTERNET" -> SignatureDesign(
            bg = Color(0xFF0E1E2E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E4A6E), Color(0xFF0E1E2E), Color(0xFF060E18))), size = Size(w, h))
                // Glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF4FA8E8).copy(alpha = 0.18f), Color(0xFF4FA8E8).copy(alpha = 0f))), radius = w * 0.36f, center = Offset(w * 0.32f, h * 0.40f))
                // Browser bar top
                drawRoundRect(Color(0xFF1A2E42), topLeft = Offset(w * 0.06f, h * 0.05f), size = Size(w * 0.88f, w * 0.0375f), cornerRadius = CornerRadius(4f))
                drawCircle(Color(0xFF4FA8E8), w * 0.008f, Offset(w * 0.09f, h * 0.075f))
                drawRoundRect(Color(0xFF2E4E6A).copy(alpha = 0.8f), topLeft = Offset(w * 0.13f, h * 0.066f), size = Size(w * 0.55f, w * 0.0135f), cornerRadius = CornerRadius(2f))
                // Globe wireframe
                val gx = w * 0.32f; val gy = h * 0.42f; val gr = w * 0.16f
                drawCircle(Color(0xFF9FC8E8).copy(alpha = 0.5f), gr, Offset(gx, gy), style = Stroke(1.4f))
                drawOval(Color(0xFF9FC8E8).copy(alpha = 0.35f), topLeft = Offset(gx - gr, gy - gr * 0.5f), size = Size(gr * 2, gr), style = Stroke(1f))
                drawOval(Color(0xFF9FC8E8).copy(alpha = 0.35f), topLeft = Offset(gx - gr * 0.5f, gy - gr), size = Size(gr, gr * 2), style = Stroke(1f))
                drawLine(Color(0xFF9FC8E8).copy(alpha = 0.4f), Offset(gx - gr, gy), Offset(gx + gr, gy), strokeWidth = 1f)
                drawLine(Color(0xFF9FC8E8).copy(alpha = 0.4f), Offset(gx, gy - gr), Offset(gx, gy + gr), strokeWidth = 1f)
                // Network nodes + links
                val nodes = listOf(Offset(w * 0.72f, h * 0.30f), Offset(w * 0.84f, h * 0.50f), Offset(w * 0.68f, h * 0.62f), Offset(w * 0.80f, h * 0.78f))
                nodes.forEachIndexed { i, n ->
                    drawLine(Color(0xFF4FA8E8).copy(alpha = 0.4f), Offset(gx, gy), n, strokeWidth = 1f)
                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF9FD8FF), Color(0xFF2E6A9E)), center = Offset(n.x, n.y), radius = w * 0.02f), radius = w * 0.013f, center = n)
                    if (i < nodes.size - 1) drawLine(Color(0xFF4FA8E8).copy(alpha = 0.3f), n, nodes[i + 1], strokeWidth = 0.8f)
                }
                // Signal dots
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 16) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color(0xFF9FC8E8).copy(alpha = 0.22f), 0.8f, Offset(x, y))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF030A12).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FA8E8), badgeInk = Color(0xFF060E18),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8F2FA),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4FA8E8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4D8E8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF4FA8E8).copy(alpha = 0.72f)
        )
        // ═══ BIOLOGY — luminous helix, cells, chromosomes on emerald ═══
        cat == "BIOLOGY" -> SignatureDesign(
            bg = Color(0xFF0E2418), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E4A30), Color(0xFF0E2418), Color(0xFF06120C))), size = Size(w, h))
                // Glow behind the helix
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF6BE3A0).copy(alpha = 0.22f), Color(0xFF6BE3A0).copy(alpha = 0f))), radius = w * 0.34f, center = Offset(w * 0.42f, h * 0.42f))
                // DNA double helix, right side
                val hx = w * 0.66f
                val strand1 = Path(); val strand2 = Path()
                for (i in 0..40) {
                    val t = i / 40f
                    val y = h * 0.12f + t * h * 0.76f
                    val x1 = hx + kotlin.math.sin(t * Math.PI.toFloat() * 4f) * w * 0.07f
                    val x2 = hx + kotlin.math.sin(t * Math.PI.toFloat() * 4f + Math.PI.toFloat()) * w * 0.07f
                    if (i == 0) { strand1.moveTo(x1, y); strand2.moveTo(x2, y) } else { strand1.lineTo(x1, y); strand2.lineTo(x2, y) }
                    if (i % 4 == 0) drawLine(Color(0xFF6BE3A0).copy(alpha = 0.25f), Offset(x1, y), Offset(x2, y), strokeWidth = 1f)
                }
                drawPath(strand1, Color(0xFF9FF0C0).copy(alpha = 0.85f), style = Stroke(2.2f))
                drawPath(strand2, Color(0xFF4FA85E).copy(alpha = 0.85f), style = Stroke(2.2f))
                // Cell-membrane circles, left side
                listOf(Pair(Offset(w * 0.18f, h * 0.30f), 0.06f), Pair(Offset(w * 0.10f, h * 0.62f), 0.045f)).forEach { (c, r) ->
                    drawCircle(Color(0xFF6BE3A0).copy(alpha = 0.35f), w * r, c, style = Stroke(1.2f))
                    drawCircle(Color(0xFF9FF0C0).copy(alpha = 0.5f), w * r * 0.3f, c)
                }
                // Chromosome X silhouettes
                listOf(Pair(Offset(w * 0.28f, h * 0.70f), 0.02f), Pair(Offset(w * 0.40f, h * 0.80f), 0.016f)).forEach { (c, r) ->
                    drawLine(Color(0xFF9FF0C0).copy(alpha = 0.45f), Offset(c.x - w * r, c.y - w * r), Offset(c.x + w * r, c.y + w * r), strokeWidth = 1.6f)
                    drawLine(Color(0xFF9FF0C0).copy(alpha = 0.45f), Offset(c.x - w * r, c.y + w * r), Offset(c.x + w * r, c.y - w * r), strokeWidth = 1.6f)
                }
                // Floating microbe dots
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 18) {
                    val x = ((s * (i+1) * 3571) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 4201) % 10000) / 10000f * h
                    drawCircle(Color(0xFF9FF0C0).copy(alpha = 0.2f), 0.8f + (i % 3) * 0.5f, Offset(x, y))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF020A05).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF6BE3A0), badgeInk = Color(0xFF0E2418),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8F8EE),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF6BE3A0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4E2D0).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF6BE3A0).copy(alpha = 0.72f)
        )
        // ═══ CHEMISTRY — connected hexagon lattice, benzene rings, molecules ═══
        cat == "CHEMISTRY" -> SignatureDesign(
            bg = Color(0xFF0E1A2E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E3A5E), Color(0xFF0E1A2E), Color(0xFF080C18))), size = Size(w, h))
                // Cyan glow behind the lattice
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF4FE8E8).copy(alpha = 0.18f), Color(0xFF4FE8E8).copy(alpha = 0f))), radius = w * 0.42f, center = Offset(w * 0.62f, h * 0.40f))
                // Hexagon helper — one ring, optionally with a double bond
                fun hexagon(cx: Float, cy: Float, r: Float, stroke: Color, node: Color, doubleBond: Boolean = false) {
                    val pts = (0 until 6).map { k ->
                        val a = Math.toRadians((60.0 * k + 90.0)).toFloat()
                        Offset(cx + kotlin.math.cos(a) * r, cy + kotlin.math.sin(a) * r)
                    }
                    for (k in 0 until 6) {
                        drawLine(stroke, pts[k], pts[(k + 1) % 6], strokeWidth = 1.6f)
                    }
                    if (doubleBond) {
                        val mid = Offset((pts[0].x + pts[5].x) / 2, (pts[0].y + pts[5].y) / 2)
                        val nx = -(pts[5].y - pts[0].y); val ny = (pts[5].x - pts[0].x)
                        val len = kotlin.math.sqrt(nx * nx + ny * ny) + 0.0001f
                        val off = Offset(nx / len * r * 0.10f, ny / len * r * 0.10f)
                        drawLine(stroke.copy(alpha = stroke.alpha * 0.85f), Offset(mid.x - off.x, mid.y - off.y), Offset(mid.x + off.x, mid.y + off.y), strokeWidth = 1.2f)
                    }
                    pts.forEach { p -> drawCircle(node, r * 0.09f, p) }
                }
                // Honeycomb lattice on the right — rings share edges, so the
                // bonds visibly connect into one crystal sheet
                val r = w * 0.038f
                val dx = r * 1.732f; val dy = r * 1.5f
                val lx = w * 0.56f; val ly = h * 0.30f
                for (row in 0 until 3) {
                    for (col in 0 until 3) {
                        val cx = lx + col * dx + (if (row % 2 == 1) dx * 0.5f else 0f)
                        val cy = ly + row * dy
                        val colr = if ((row + col) % 2 == 0) Color(0xFF4FE8E8) else Color(0xFF4FA8E8)
                        hexagon(cx, cy, r, colr.copy(alpha = 0.55f), colr.copy(alpha = 0.9f), doubleBond = (row + col) % 3 == 0)
                    }
                }
                // Hero benzene ring, left — with a glow and double bonds
                val hx = w * 0.28f; val hy = h * 0.38f; val hr = w * 0.075f
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF4FE8E8).copy(alpha = 0.22f), Color(0xFF4FE8E8).copy(alpha = 0f))), radius = w * 0.17f, center = Offset(hx, hy))
                hexagon(hx, hy, hr, Color(0xFF9FF0F0).copy(alpha = 0.90f), Color(0xFF4FE8E8), doubleBond = true)
                // Bond connecting the hero ring to the lattice
                val rightVertex = Offset(hx + hr * kotlin.math.cos(Math.toRadians(30.0).toFloat()), hy + hr * kotlin.math.sin(Math.toRadians(30.0).toFloat()))
                val leftLattice = Offset(lx + dx * 0.5f, ly + dy)
                drawLine(Color(0xFF4FE8E8).copy(alpha = 0.70f), rightVertex, leftLattice, strokeWidth = 1.6f)
                drawCircle(Color(0xFF4FE8E8).copy(alpha = 0.9f), w * 0.008f, leftLattice)
                // Water molecule, bottom-left — bonded to itself, not floating
                val ox = w * 0.16f; val oy = h * 0.74f
                drawCircle(Color(0xFFE8544F).copy(alpha = 0.9f), w * 0.022f, Offset(ox, oy))
                drawLine(Color(0xFF9FD8E8).copy(alpha = 0.6f), Offset(ox, oy), Offset(ox - w * 0.045f, oy - w * 0.035f), strokeWidth = 1.4f)
                drawLine(Color(0xFF9FD8E8).copy(alpha = 0.6f), Offset(ox, oy), Offset(ox + w * 0.045f, oy - w * 0.035f), strokeWidth = 1.4f)
                drawCircle(Color(0xFFB8F8F8).copy(alpha = 0.9f), w * 0.012f, Offset(ox - w * 0.045f, oy - w * 0.035f))
                drawCircle(Color(0xFFB8F8F8).copy(alpha = 0.9f), w * 0.012f, Offset(ox + w * 0.045f, oy - w * 0.035f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF04060C).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FE8E8), badgeInk = Color(0xFF0E1A2E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8F8F8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4FE8E8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4E0E0).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF4FE8E8).copy(alpha = 0.72f)
        )
        // ═══ ANIMALS — paw-print trail through a forest clearing ═══
        cat == "ANIMALS" -> SignatureDesign(
            bg = Color(0xFF14261A), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2E4A2E), Color(0xFF14261A), Color(0xFF0A120C))), size = Size(w, h))
                // Moonlight glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2E4C8).copy(alpha = 0.16f), Color(0xFFF2E4C8).copy(alpha = 0f))), radius = w * 0.28f, center = Offset(w * 0.80f, h * 0.14f))
                drawCircle(Color(0xFFF2E4C8).copy(alpha = 0.5f), w * 0.014f, Offset(w * 0.80f, h * 0.14f))
                // Tree silhouette, right
                drawLine(Color(0xFF0A140C), Offset(w * 0.86f, h * 0.30f), Offset(w * 0.86f, h * 0.90f), strokeWidth = 6f)
                drawPath(Path().apply { moveTo(w * 0.86f, h * 0.34f); lineTo(w * 0.72f, h * 0.46f); lineTo(w * 0.86f, h * 0.42f); close() }, Color(0xFF0A140C))
                drawPath(Path().apply { moveTo(w * 0.86f, h * 0.30f); lineTo(w * 0.74f, h * 0.34f); lineTo(w * 0.86f, h * 0.40f); close() }, Color(0xFF0A140C))
                // Grass blades bottom
                for (i in 0 until 20) {
                    val gx = w * 0.04f + i * w * 0.048f
                    drawLine(Color(0xFF3E8A4A).copy(alpha = 0.5f), Offset(gx, h * 0.90f), Offset(gx + w * 0.008f, h * 0.84f + (i % 3) * h * 0.01f), strokeWidth = 1.2f)
                }
                // Paw-print trail
                fun paw(cx: Float, cy: Float, s: Float) {
                    drawOval(Color(0xFFE8D9A8).copy(alpha = 0.7f), topLeft = Offset(cx - s * 0.5f, cy - s * 0.2f), size = Size(s * 1.1f, s * 0.7f))
                    drawCircle(Color(0xFFE8D9A8).copy(alpha = 0.7f), s * 0.22f, Offset(cx - s * 0.28f, cy - s * 0.55f))
                    drawCircle(Color(0xFFE8D9A8).copy(alpha = 0.7f), s * 0.22f, Offset(cx + s * 0.28f, cy - s * 0.55f))
                    drawCircle(Color(0xFFE8D9A8).copy(alpha = 0.7f), s * 0.20f, Offset(cx - s * 0.12f, cy - s * 0.66f))
                    drawCircle(Color(0xFFE8D9A8).copy(alpha = 0.7f), s * 0.20f, Offset(cx + s * 0.12f, cy - s * 0.66f))
                }
                paw(w * 0.24f, h * 0.72f, w * 0.018f)
                paw(w * 0.38f, h * 0.60f, w * 0.022f)
                paw(w * 0.54f, h * 0.50f, w * 0.026f)
                // Fireflies
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 12) {
                    val x = ((s * (i+1) * 3571) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 4201) % 10000) / 10000f * h * 0.6f
                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2E84F).copy(alpha = 0.5f), Color(0xFFF2E84F).copy(alpha = 0f))), radius = w * 0.014f, center = Offset(x, y))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF040A05).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8D9A8), badgeInk = Color(0xFF14261A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF2F6E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8D9A8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8D8BC).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8D9A8).copy(alpha = 0.72f)
        )
        // ═══ PLANTS — botanical leaf, veins, droplets, sunbeams ═══
        cat == "PLANTS" -> SignatureDesign(
            bg = Color(0xFF122A12), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2E5E2E), Color(0xFF122A12), Color(0xFF081408))), size = Size(w, h))
                // Sunbeams from top-left
                for (i in 0 until 7) {
                    val a = Math.toRadians((25.0 + i * 9)).toFloat()
                    drawLine(Color(0xFFF2F6C8).copy(alpha = 0.10f), Offset(w * 0.02f, h * 0.02f), Offset(w * 0.02f + kotlin.math.cos(a) * w * 0.8f, h * 0.02f + kotlin.math.sin(a) * w * 0.8f), strokeWidth = 1.6f)
                }
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2F6C8).copy(alpha = 0.2f), Color(0xFFF2F6C8).copy(alpha = 0f))), radius = w * 0.12f, center = Offset(w * 0.04f, h * 0.04f))
                // Large botanical leaf
                val lx = w * 0.55f; val ly = h * 0.42f
                drawPath(Path().apply {
                    moveTo(lx, ly - h * 0.16f)
                    cubicTo(lx + w * 0.16f, ly - h * 0.12f, lx + w * 0.20f, ly + h * 0.05f, lx + w * 0.02f, ly + h * 0.18f)
                    cubicTo(lx - w * 0.18f, ly + h * 0.08f, lx - w * 0.18f, ly - h * 0.10f, lx, ly - h * 0.16f)
                    close()
                }, Color(0xFF5EA85E).copy(alpha = 0.55f))
                drawPath(Path().apply {
                    moveTo(lx, ly - h * 0.16f)
                    cubicTo(lx + w * 0.16f, ly - h * 0.12f, lx + w * 0.20f, ly + h * 0.05f, lx + w * 0.02f, ly + h * 0.18f)
                    cubicTo(lx - w * 0.18f, ly + h * 0.08f, lx - w * 0.18f, ly - h * 0.10f, lx, ly - h * 0.16f)
                    close()
                }, Color(0xFF8AE88A).copy(alpha = 0.7f), style = Stroke(1.4f))
                drawLine(Color(0xFF8AE88A).copy(alpha = 0.65f), Offset(lx, ly - h * 0.15f), Offset(lx, ly + h * 0.16f), strokeWidth = 1.4f)
                for (i in 0 until 6) {
                    val ty = ly - h * 0.10f + i * h * 0.045f
                    drawLine(Color(0xFF8AE88A).copy(alpha = 0.45f), Offset(lx, ty), Offset(lx - w * 0.08f - i * w * 0.006f, ty - h * 0.014f), strokeWidth = 0.9f)
                    drawLine(Color(0xFF8AE88A).copy(alpha = 0.45f), Offset(lx, ty), Offset(lx + w * 0.08f + i * w * 0.006f, ty - h * 0.014f), strokeWidth = 0.9f)
                }
                // Water droplets
                listOf(Pair(Offset(w * 0.26f, h * 0.24f), 0.014f), Pair(Offset(w * 0.80f, h * 0.30f), 0.010f), Pair(Offset(w * 0.34f, h * 0.74f), 0.012f)).forEach { (c, r) ->
                    drawOval(Color(0xFFB8E8F2).copy(alpha = 0.65f), topLeft = Offset(c.x - w * r, c.y - w * r * 1.3f), size = Size(w * r * 2, w * r * 2.2f))
                    drawCircle(Color.White.copy(alpha = 0.7f), w * r * 0.4f, Offset(c.x - w * r * 0.3f, c.y - w * r * 0.8f))
                }
                // Small sprout bottom-left
                drawLine(Color(0xFF5EA85E), Offset(w * 0.12f, h * 0.86f), Offset(w * 0.12f, h * 0.78f), strokeWidth = 1.6f)
                drawPath(Path().apply { moveTo(w * 0.12f, h * 0.78f); cubicTo(w * 0.10f, h * 0.75f, w * 0.08f, h * 0.75f, w * 0.075f, h * 0.78f); cubicTo(w * 0.08f, h * 0.80f, w * 0.10f, h * 0.80f, w * 0.12f, h * 0.78f) }, Color(0xFF8AE88A))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF030A03).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8AE88A), badgeInk = Color(0xFF122A12),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF0F8E8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8AE88A),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4E2BC).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF8AE88A).copy(alpha = 0.72f)
        )
        // ═══ TECHNOLOGIES — circuit traces, CPU chip, binary streams ═══
        cat == "TECHNOLOGIES" -> SignatureDesign(
            bg = Color(0xFF0A1626), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF16345E), Color(0xFF0A1626), Color(0xFF040A12))), size = Size(w, h))
                // Electric cyan glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF4FE8FF).copy(alpha = 0.18f), Color(0xFF4FE8FF).copy(alpha = 0f))), radius = w * 0.36f, center = Offset(w * 0.30f, h * 0.34f))
                // Circuit traces
                val traces = listOf(Pair(Offset(0f, h * 0.20f), Offset(w * 0.30f, h * 0.20f)), Pair(Offset(w * 0.30f, h * 0.20f), Offset(w * 0.30f, h * 0.50f)), Pair(Offset(0f, h * 0.80f), Offset(w * 0.44f, h * 0.80f)), Pair(Offset(w * 0.44f, h * 0.80f), Offset(w * 0.44f, h * 0.58f)), Pair(Offset(w * 0.90f, 0f), Offset(w * 0.90f, h * 0.18f)))
                traces.forEach { (a, b) -> drawLine(Color(0xFF4FE8FF).copy(alpha = 0.55f), a, b, strokeWidth = 1.4f) }
                listOf(Offset(w * 0.30f, h * 0.20f), Offset(w * 0.30f, h * 0.50f), Offset(w * 0.44f, h * 0.80f), Offset(w * 0.44f, h * 0.58f), Offset(w * 0.90f, h * 0.18f)).forEach { n ->
                    drawCircle(Color(0xFF4FE8FF).copy(alpha = 0.8f), 1.8f, n)
                }
                // CPU chip
                val cx = w * 0.70f; val cy = h * 0.40f; val cs = w * 0.16f
                drawRoundRect(brush = Brush.linearGradient(listOf(Color(0xFF2E4E7A), Color(0xFF16304E)), start = Offset(cx - cs, cy - cs), end = Offset(cx + cs, cy + cs)), topLeft = Offset(cx - cs, cy - cs), size = Size(cs * 2, cs * 2), cornerRadius = CornerRadius(6f))
                for (i in 0 until 4) {
                    drawLine(Color(0xFF9FC8E8).copy(alpha = 0.6f), Offset(cx - cs, cy - cs + i * cs * 0.66f), Offset(cx - cs - w * 0.015f, cy - cs + i * cs * 0.66f), strokeWidth = 1.4f)
                    drawLine(Color(0xFF9FC8E8).copy(alpha = 0.6f), Offset(cx + cs, cy - cs + i * cs * 0.66f), Offset(cx + cs + w * 0.015f, cy - cs + i * cs * 0.66f), strokeWidth = 1.4f)
                }
                drawRoundRect(Color(0xFF4FE8FF).copy(alpha = 0.9f), topLeft = Offset(cx - cs * 0.4f, cy - cs * 0.4f), size = Size(cs * 0.8f, cs * 0.8f), cornerRadius = CornerRadius(2f))
                // Binary streams
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 24) {
                    val x = w * 0.14f + ((s * (i+1) * 3571) % 100) / 100f * w * 0.7f
                    val y = h * 0.55f + ((s * (i+1) * 4201) % 100) / 100f * h * 0.4f
                    drawCircle(if (i % 2 == 0) Color(0xFF4FE8FF) else Color(0xFF9FC8E8), if (i % 3 == 0) 1.6f else 1f, Offset(x, y).let { Offset(x, y) })
                    drawLine(Color(0xFF4FE8FF).copy(alpha = 0.25f), Offset(x, y), Offset(x, y + h * 0.02f), strokeWidth = 1f)
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF02060C).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FE8FF), badgeInk = Color(0xFF0A1626),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8F8FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4FE8FF),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0D8E8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF4FE8FF).copy(alpha = 0.72f)
        )
        // ═══ ASTRONOMY — spiral galaxy, ringed planet, nebula, stars ═══
        cat == "ASTRONOMY" -> SignatureDesign(
            bg = Color(0xFF0E1030), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF2A2A5E), Color(0xFF0E1030), Color(0xFF06061A)), center = Offset(w * 0.5f, h * 0.40f), radius = w * 0.95f), size = Size(w, h))
                // Nebula clouds
                listOf(Pair(Offset(w * 0.20f, h * 0.25f), Color(0xFF8A4FA8)), Pair(Offset(w * 0.82f, h * 0.70f), Color(0xFF2E6AA8)), Pair(Offset(w * 0.62f, h * 0.20f), Color(0xFFA84F6E))).forEach { (c, col) ->
                    drawCircle(brush = Brush.radialGradient(listOf(col.copy(alpha = 0.22f), col.copy(alpha = 0f))), radius = w * 0.24f, center = c)
                }
                // Starfield
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 90) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color.White.copy(alpha = 0.20f + (i % 4) * 0.08f), 0.7f + (i % 3) * 0.5f, Offset(x, y))
                }
                // Spiral galaxy
                val gx = w * 0.40f; val gy = h * 0.38f
                for (i in 0 until 3) {
                    drawArc(Color(0xFFC8B8F2).copy(alpha = 0.35f - i * 0.08f), -30f + i * 50f, 200f, false, Offset(gx - w * 0.12f - i * w * 0.02f, gy - w * 0.12f - i * w * 0.02f), Size(w * 0.24f + i * w * 0.04f, w * 0.24f + i * w * 0.04f), style = Stroke(2.2f))
                }
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2E8FF), Color(0xFF8A6AE8)), center = Offset(gx, gy), radius = w * 0.035f), radius = w * 0.028f, center = Offset(gx, gy))
                // Ringed planet, right
                val px = w * 0.78f; val py = h * 0.30f; val pr = w * 0.045f
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879), Color(0xFFA8682E)), center = Offset(px - pr * 0.3f, py - pr * 0.3f), radius = pr), radius = pr, center = Offset(px, py))
                drawOval(Color(0xFFE8D9A8).copy(alpha = 0.6f), topLeft = Offset(px - pr * 1.9f, py - pr * 0.55f), size = Size(pr * 3.8f, pr * 1.1f), style = Stroke(1.4f))
                drawOval(Color(0xFFE8D9A8).copy(alpha = 0.25f), topLeft = Offset(px - pr * 2.2f, py - pr * 0.8f), size = Size(pr * 4.4f, pr * 1.6f), style = Stroke(1f))
                // Shooting star
                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.16f, h * 0.10f), Offset(w * 0.24f, h * 0.16f), strokeWidth = 1.2f)
                drawLine(Color.White.copy(alpha = 0.25f), Offset(w * 0.24f, h * 0.16f), Offset(w * 0.30f, h * 0.20f), strokeWidth = 0.8f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF03030E).copy(alpha = 0.6f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8D9A8), badgeInk = Color(0xFF0E1030),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF2EEFF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC8B8F2),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C8E8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8D9A8).copy(alpha = 0.72f)
        )
        // ═══ HISTORY — parchment scroll, hourglass, timeline ═══
        cat == "HISTORY" -> SignatureDesign(
            bg = Color(0xFF2A1E12), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF5E4A2E), Color(0xFF2A1E12), Color(0xFF140E06)), center = Offset(w * 0.5f, h * 0.35f), radius = w * 0.9f), size = Size(w, h))
                // Warm candle glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2A84F).copy(alpha = 0.20f), Color(0xFFF2A84F).copy(alpha = 0f))), radius = w * 0.32f, center = Offset(w * 0.74f, h * 0.30f))
                // Unrolled scroll, bottom — w-sized so it keeps proportions
                val sx = w * 0.16f; val sy = h * 0.66f; val sw = w * 0.52f; val sh = w * 0.105f
                drawRoundRect(Color(0xFFD8C49A), topLeft = Offset(sx, sy), size = Size(sw, sh), cornerRadius = CornerRadius(3f))
                drawOval(Color(0xFFB89E6E), topLeft = Offset(sx - w * 0.02f, sy - w * 0.006f), size = Size(w * 0.04f, sh + w * 0.012f))
                drawOval(Color(0xFFB89E6E), topLeft = Offset(sx + sw - w * 0.02f, sy - w * 0.006f), size = Size(w * 0.04f, sh + w * 0.012f))
                // Script lines on the scroll
                for (i in 0 until 4) {
                    drawLine(Color(0xFF6E5A3A).copy(alpha = 0.5f), Offset(sx + w * 0.04f, sy + w * 0.018f + i * w * 0.0195f), Offset(sx + sw * (0.7f - i * 0.08f), sy + w * 0.018f + i * w * 0.0195f), strokeWidth = 1.1f)
                }
                // Hourglass
                val hx = w * 0.80f; val hy = h * 0.40f
                drawLine(Color(0xFFE8D9A8).copy(alpha = 0.8f), Offset(hx - w * 0.028f, hy - h * 0.14f), Offset(hx + w * 0.028f, hy - h * 0.14f), strokeWidth = 2f)
                drawLine(Color(0xFFE8D9A8).copy(alpha = 0.8f), Offset(hx - w * 0.028f, hy + h * 0.14f), Offset(hx + w * 0.028f, hy + h * 0.14f), strokeWidth = 2f)
                drawPath(Path().apply { moveTo(hx - w * 0.024f, hy - h * 0.12f); lineTo(hx, hy); lineTo(hx + w * 0.024f, hy - h * 0.12f); lineTo(hx - w * 0.024f, hy - h * 0.12f); close() }, Color(0xFFE8D9A8).copy(alpha = 0.5f), style = Stroke(1.2f))
                drawPath(Path().apply { moveTo(hx - w * 0.024f, hy + h * 0.12f); lineTo(hx, hy); lineTo(hx + w * 0.024f, hy + h * 0.12f); lineTo(hx - w * 0.024f, hy + h * 0.12f); close() }, Color(0xFFF2E4C8).copy(alpha = 0.35f))
                // Timeline dots along the bottom
                for (i in 0 until 8) {
                    val tx = w * 0.08f + i * w * 0.11f
                    drawCircle(Color(0xFFE8C88F).copy(alpha = 0.5f), 1.8f, Offset(tx, h * 0.92f))
                    if (i < 7) drawLine(Color(0xFFE8C88F).copy(alpha = 0.25f), Offset(tx + 2f, h * 0.92f), Offset(tx + w * 0.11f - 2f, h * 0.92f), strokeWidth = 0.8f)
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF0D0703).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8C88F), badgeInk = Color(0xFF2A1E12),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF2E8D2),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8C88F),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFDCCEB4).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE8C88F).copy(alpha = 0.72f)
        )
        // ═══ GEOLOGY — strata layers, crystal shards, contour rings ═══
        cat == "GEOLOGY" -> SignatureDesign(
            bg = Color(0xFF241A12), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF4E3A26), Color(0xFF241A12), Color(0xFF120C06))), size = Size(w, h))
                // Strata layers, wavy
                val strata = listOf(Pair(Color(0xFF8A6A3A), 0.62f), Pair(Color(0xFF6E4E2E), 0.70f), Pair(Color(0xFF5E3E24), 0.78f), Pair(Color(0xFF4A3018), 0.86f))
                strata.forEach { (col, topFrac) ->
                    val path = Path().apply {
                        moveTo(0f, h * topFrac)
                        for (i in 0..20) lineTo(i * w / 20f, h * topFrac + kotlin.math.sin(i * 0.9f) * h * 0.008f)
                        lineTo(w, h * topFrac + h * 0.08f)
                        lineTo(0f, h * topFrac + h * 0.08f)
                        close()
                    }
                    drawPath(path, col.copy(alpha = 0.55f))
                }
                // Crystal shards, right
                listOf(Pair(Offset(w * 0.74f, h * 0.24f), Color(0xFFE8D9A8)), Pair(Offset(w * 0.84f, h * 0.34f), Color(0xFFC8A85E))).forEach { (c, col) ->
                    drawPath(Path().apply { moveTo(c.x, c.y - h * 0.10f); lineTo(c.x + w * 0.022f, c.y); lineTo(c.x, c.y + h * 0.06f); lineTo(c.x - w * 0.022f, c.y); close() }, col.copy(alpha = 0.8f))
                    drawLine(Color.White.copy(alpha = 0.4f), Offset(c.x, c.y - h * 0.10f), Offset(c.x, c.y - h * 0.02f), strokeWidth = 0.8f)
                }
                // Contour rings bottom-left — w-sized
                for (i in 0 until 4) {
                    drawOval(Color(0xFFE8C88F).copy(alpha = 0.12f), topLeft = Offset(w * 0.06f - i * w * 0.015f, h * 0.70f - i * w * 0.011f), size = Size(w * 0.22f + i * w * 0.03f, w * 0.075f + i * w * 0.015f), style = Stroke(1.2f))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF0A0603).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8D9A8), badgeInk = Color(0xFF241A12),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF2EAD8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8D9A8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFDCCEBA).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8D9A8).copy(alpha = 0.72f)
        )
        // ═══ MEDICINE — EKG trace, capsule, pulse rings on clinical teal ═══
        cat == "MEDICINE" -> SignatureDesign(
            bg = Color(0xFF0E2226), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E4A4E), Color(0xFF0E2226), Color(0xFF060E10))), size = Size(w, h))
                // Teal glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF6BE8D8).copy(alpha = 0.18f), Color(0xFF6BE8D8).copy(alpha = 0f))), radius = w * 0.34f, center = Offset(w * 0.66f, h * 0.36f))
                // Pulse rings bottom-left
                for (i in 0 until 3) {
                    drawCircle(Color(0xFF6BE8D8).copy(alpha = 0.25f - i * 0.06f), w * (0.05f + i * 0.02f), Offset(w * 0.16f, h * 0.74f), style = Stroke(1.2f))
                }
                drawCircle(Color(0xFF6BE8D8).copy(alpha = 0.9f), w * 0.014f, Offset(w * 0.16f, h * 0.74f))
                // Glowing EKG trace
                val ekg = Path().apply {
                    moveTo(w * 0.10f, h * 0.42f)
                    lineTo(w * 0.22f, h * 0.42f)
                    lineTo(w * 0.28f, h * 0.38f)
                    lineTo(w * 0.33f, h * 0.46f)
                    lineTo(w * 0.37f, h * 0.30f)
                    lineTo(w * 0.41f, h * 0.56f)
                    lineTo(w * 0.45f, h * 0.40f)
                    lineTo(w * 0.52f, h * 0.40f)
                    lineTo(w * 0.56f, h * 0.34f)
                    lineTo(w * 0.60f, h * 0.46f)
                    lineTo(w * 0.64f, h * 0.36f)
                    lineTo(w * 0.68f, h * 0.42f)
                    lineTo(w * 0.90f, h * 0.42f)
                }
                drawPath(ekg, Color(0xFF6BE8D8).copy(alpha = 0.9f), style = Stroke(2.2f))
                drawPath(ekg, Color(0xFF6BE8D8).copy(alpha = 0.25f), style = Stroke(4.5f))
                // Capsule
                val capX = w * 0.70f; val capY = h * 0.62f
                drawRoundRect(Color(0xFFE8544F), topLeft = Offset(capX - w * 0.05f, capY), size = Size(w * 0.045f, w * 0.0225f), cornerRadius = CornerRadius(4f))
                drawRoundRect(Color(0xFFF2F6F2), topLeft = Offset(capX - w * 0.005f, capY), size = Size(w * 0.045f, w * 0.0225f), cornerRadius = CornerRadius(4f))
                drawLine(Color(0xFFE8544F).copy(alpha = 0.5f), Offset(capX, capY - h * 0.005f), Offset(capX, capY + h * 0.035f), strokeWidth = 1f)
                // Medical cross glow, top-right
                drawLine(Color(0xFF6BE8D8).copy(alpha = 0.5f), Offset(w * 0.88f, h * 0.08f), Offset(w * 0.88f, h * 0.16f), strokeWidth = 3f)
                drawLine(Color(0xFF6BE8D8).copy(alpha = 0.5f), Offset(w * 0.84f, h * 0.12f), Offset(w * 0.92f, h * 0.12f), strokeWidth = 3f)
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF020A0C).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF6BE8D8), badgeInk = Color(0xFF0E2226),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8F8F4),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF6BE8D8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4E2DC).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF6BE8D8).copy(alpha = 0.72f)
        )
        // ═══ PSYCHOLOGY — brain hemispheres, thought bubbles, calm violet ═══
        cat == "PSYCHOLOGY" -> SignatureDesign(
            bg = Color(0xFF1E1630), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3E2E5E), Color(0xFF1E1630), Color(0xFF100A1C))), size = Size(w, h))
                // Soft violet glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF9A8AFF).copy(alpha = 0.16f), Color(0xFF9A8AFF).copy(alpha = 0f))), radius = w * 0.36f, center = Offset(w * 0.36f, h * 0.40f))
                // Brain hemispheres — two lobe outlines
                val bx = w * 0.36f; val by = h * 0.46f; val br = w * 0.13f
                listOf(-1f, 1f).forEach { side ->
                    val path = Path().apply {
                        moveTo(bx, by - br * 0.8f)
                        cubicTo(bx + side * br * 0.7f, by - br * 1.3f, bx + side * br * 1.5f, by - br * 0.2f, bx + side * br * 0.8f, by + br * 0.9f)
                        cubicTo(bx + side * br * 0.2f, by + br * 1.1f, bx - side * br * 0.2f, by + br * 0.9f, bx, by + br * 0.6f)
                    }
                    drawPath(path, Color(0xFFC8BCF5).copy(alpha = 0.55f), style = Stroke(1.8f))
                    // Inner squiggles
                    for (i in 0 until 3) {
                        val sy0 = by - br * 0.6f + i * br * 0.45f
                        drawArc(Color(0xFFC8BCF5).copy(alpha = 0.30f), if (side > 0) 160f else 200f, 120f, false, Offset(bx + side * br * 0.35f, sy0), Size(br * 0.7f, br * 0.28f), style = Stroke(1f))
                    }
                }
                drawLine(Color(0xFFC8BCF5).copy(alpha = 0.4f), Offset(bx, by - br * 0.9f), Offset(bx, by + br * 0.7f), strokeWidth = 1.2f)
                // Thought bubbles
                listOf(Pair(Offset(w * 0.72f, h * 0.22f), 0.024f), Pair(Offset(w * 0.84f, h * 0.40f), 0.016f), Pair(Offset(w * 0.68f, h * 0.56f), 0.012f)).forEach { (c, r) ->
                    drawCircle(Color(0xFFC8BCF5).copy(alpha = 0.5f), w * r, c, style = Stroke(1.2f))
                    drawCircle(Color(0xFFC8BCF5).copy(alpha = 0.35f), w * r * 0.4f, c)
                }
                // Neural connection dots
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 16) {
                    val x = ((s * (i+1) * 3571) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 4201) % 10000) / 10000f * h
                    drawCircle(Color(0xFF9A8AFF).copy(alpha = 0.22f), 1f, Offset(x, y))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF080410).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF9A8AFF), badgeInk = Color(0xFF1E1630),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF0ECFF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF9A8AFF),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD4CCF0).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF9A8AFF).copy(alpha = 0.72f)
        )
        // ═══ MATHEMATICS — golden spiral, geometric solids, coordinate grid ═══
        cat == "MATHEMATICS" -> SignatureDesign(
            bg = Color(0xFF121A30), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF22345E), Color(0xFF121A30), Color(0xFF080C18))), size = Size(w, h))
                // Faint coordinate grid
                for (i in 0 until 14) drawLine(Color(0xFF8AA8E8).copy(alpha = 0.05f), Offset(i * w / 13f, 0f), Offset(i * w / 13f, h), strokeWidth = 0.6f)
                for (i in 0 until 18) drawLine(Color(0xFF8AA8E8).copy(alpha = 0.05f), Offset(0f, i * h / 17f), Offset(w, i * h / 17f), strokeWidth = 0.6f)
                // Gold glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE8C84F).copy(alpha = 0.14f), Color(0xFFE8C84F).copy(alpha = 0f))), radius = w * 0.34f, center = Offset(w * 0.42f, h * 0.38f))
                // Golden spiral (Fibonacci-ish arcs)
                var gx = w * 0.30f; var gy = h * 0.62f; var gr = w * 0.14f
                val arcs = listOf(Pair(-90f, 90f), Pair(0f, 90f), Pair(90f, 90f), Pair(180f, 90f), Pair(270f, 90f))
                arcs.forEachIndexed { i, (startA, sweep) ->
                    val rad = gr * (if (i % 2 == 0) 1f else 0.62f)
                    drawArc(Color(0xFFE8C84F).copy(alpha = 0.75f), startA, sweep, false, Offset(gx, gy), Size(rad * 2, rad * 2), style = Stroke(1.8f))
                    when (startA.toInt()) {
                        -90 -> { gx += rad * 2; gy += rad * 2 }
                        0 -> gx += rad * 2
                        90 -> gy -= rad * 2
                        180 -> gx -= rad * 2
                        270 -> gy += rad * 2
                    }
                }
                // Geometric solids, right
                val ptx = w * 0.78f; val pty = h * 0.32f; val ps = w * 0.055f
                drawPath(Path().apply { moveTo(ptx, pty - ps); lineTo(ptx + ps * 0.9f, pty - ps * 0.3f); lineTo(ptx + ps * 0.9f, pty + ps * 0.7f); lineTo(ptx, pty + ps * 1.4f); lineTo(ptx - ps * 0.9f, pty + ps * 0.7f); lineTo(ptx - ps * 0.9f, pty - ps * 0.3f); close() }, Color(0xFF8AA8E8).copy(alpha = 0.35f), style = Stroke(1.4f))
                drawCircle(Color(0xFF8AA8E8).copy(alpha = 0.4f), w * 0.04f, Offset(w * 0.82f, h * 0.62f), style = Stroke(1.3f))
                drawRect(Color(0xFF8AA8E8).copy(alpha = 0.35f), topLeft = Offset(w * 0.72f, h * 0.70f), size = Size(w * 0.05f, w * 0.05f), style = Stroke(1.3f))
                drawLine(Color(0xFF8AA8E8).copy(alpha = 0.35f), Offset(w * 0.72f, h * 0.70f), Offset(w * 0.77f, h * 0.75f), strokeWidth = 1.2f)
                // Pi symbol, top-right
                drawPath(Path().apply { moveTo(w * 0.88f, h * 0.12f); lineTo(w * 0.96f, h * 0.12f); moveTo(w * 0.92f, h * 0.12f); lineTo(w * 0.92f, h * 0.20f); moveTo(w * 0.88f, h * 0.16f); lineTo(w * 0.96f, h * 0.16f) }, Color(0xFFE8C84F).copy(alpha = 0.7f), style = Stroke(1.6f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF04060C).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8C84F), badgeInk = Color(0xFF121A30),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF0F4FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8C84F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8D4F0).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8C84F).copy(alpha = 0.72f)
        )
        // ═══ ECONOMICS — rising bars, trend arrow, coin stack ═══
        cat == "ECONOMICS" -> SignatureDesign(
            bg = Color(0xFF0E2418), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E4A34), Color(0xFF0E2418), Color(0xFF06120C))), size = Size(w, h))
                // Emerald/gold glows
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF6BE3A0).copy(alpha = 0.16f), Color(0xFF6BE3A0).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.30f, h * 0.40f))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE0C84F).copy(alpha = 0.14f), Color(0xFFE0C84F).copy(alpha = 0f))), radius = w * 0.26f, center = Offset(w * 0.78f, h * 0.30f))
                // Coordinate base line
                drawLine(Color(0xFFC4E2D0).copy(alpha = 0.3f), Offset(w * 0.08f, h * 0.72f), Offset(w * 0.92f, h * 0.72f), strokeWidth = 1f)
                // Rising bar chart
                val heights = intArrayOf(10, 16, 13, 22, 18, 28, 24, 34)
                heights.forEachIndexed { i, v ->
                    val x = w * 0.12f + i * w * 0.095f
                    val bh = h * 0.30f * (v / 34f)
                    drawRoundRect(brush = Brush.verticalGradient(listOf(Color(0xFF6BE3A0), Color(0xFF2E8A4E)), startY = h * 0.72f - bh), topLeft = Offset(x, h * 0.72f - bh), size = Size(w * 0.05f, bh), cornerRadius = CornerRadius(2f))
                }
                // Trend arrow
                drawLine(Color(0xFFE0C84F).copy(alpha = 0.9f), Offset(w * 0.14f, h * 0.68f), Offset(w * 0.82f, h * 0.36f), strokeWidth = 2f)
                drawPath(Path().apply { moveTo(w * 0.82f, h * 0.36f); lineTo(w * 0.76f, h * 0.40f); lineTo(w * 0.82f, h * 0.42f); close() }, Color(0xFFE0C84F).copy(alpha = 0.9f))
                drawPath(Path().apply { moveTo(w * 0.82f, h * 0.36f); lineTo(w * 0.84f, h * 0.42f); lineTo(w * 0.86f, h * 0.38f); close() }, Color(0xFFE0C84F).copy(alpha = 0.9f))
                // Coin stack
                listOf(Pair(Offset(w * 0.84f, h * 0.56f), 0.035f), Pair(Offset(w * 0.80f, h * 0.62f), 0.028f)).forEachIndexed { i, (c, r) ->
                    drawCircle(Color(0xFFE0C84F).copy(alpha = 0.9f), w * r, c)
                    drawCircle(Color(0xFFB8982E).copy(alpha = 0.7f), w * r * 0.72f, c, style = Stroke(1f))
                    drawLine(Color(0xFFFFF3C4).copy(alpha = 0.4f), Offset(c.x - w * r * 0.5f, c.y - w * r * 0.3f), Offset(c.x + w * r * 0.2f, c.y - w * r * 0.5f), strokeWidth = 1f)
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF020A05).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE0C84F), badgeInk = Color(0xFF0E2418),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF0F8E8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF6BE3A0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4E2D0).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE0C84F).copy(alpha = 0.72f)
        )
        // ═══ LANGUAGE — speech bubbles, calligraphy strokes, warm terracotta ═══
        cat == "LANGUAGE" -> SignatureDesign(
            bg = Color(0xFF2E1A12), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF5E3820), Color(0xFF2E1A12), Color(0xFF180C06))), size = Size(w, h))
                // Warm glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2A84F).copy(alpha = 0.16f), Color(0xFFF2A84F).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.70f, h * 0.36f))
                // Speech bubbles
                listOf(Pair(Offset(w * 0.26f, h * 0.28f), 1f), Pair(Offset(w * 0.20f, h * 0.56f), 0.7f)).forEach { (c, s) ->
                    val r = w * 0.075f * s
                    drawRoundRect(Color(0xFFF5EAD8).copy(alpha = 0.9f), topLeft = Offset(c.x - r, c.y - r * 0.7f), size = Size(r * 2, r * 1.4f), cornerRadius = CornerRadius(4f * s))
                    drawPath(Path().apply { moveTo(c.x - r * 0.3f, c.y + r * 0.65f); lineTo(c.x - r * 0.45f, c.y + r * 1.15f); lineTo(c.x + r * 0.15f, c.y + r * 0.7f); close() }, Color(0xFFF5EAD8).copy(alpha = 0.9f))
                    // Three dots inside
                    for (i in 0 until 3) drawCircle(Color(0xFF8A6A4A), r * 0.08f, Offset(c.x - r * 0.35f + i * r * 0.35f, c.y))
                }
                // Calligraphy brush strokes
                listOf(Pair(Color(0xFFE8544F), 0.30f), Pair(Color(0xFF4FA8E8), 0.42f)).forEachIndexed { i, (col, y) ->
                    val path = Path().apply {
                        moveTo(w * 0.60f, h * y)
                        cubicTo(w * 0.68f, h * (y - 0.04f), w * 0.74f, h * (y + 0.02f), w * 0.88f, h * (y - 0.06f))
                        cubicTo(w * 0.86f, h * (y + 0.01f), w * 0.82f, h * (y + 0.03f), w * 0.92f, h * (y + 0.02f))
                    }
                    drawPath(path, col.copy(alpha = 0.6f), style = Stroke(2.4f))
                }
                // Accent dots between strokes
                listOf(Offset(w * 0.70f, h * 0.50f), Offset(w * 0.76f, h * 0.56f), Offset(w * 0.82f, h * 0.46f)).forEach { c ->
                    drawCircle(Color(0xFFF2A84F).copy(alpha = 0.5f), 1.6f, c)
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF0D0703).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFF2A84F), badgeInk = Color(0xFF2E1A12),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF8F0E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFF2A84F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0BC).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFF2A84F).copy(alpha = 0.72f)
        )
        // ═══ ENGINEERING — blueprint grid, gear, dimension lines ═══
        cat == "ENGINEERING" -> SignatureDesign(
            bg = Color(0xFF0E1A2E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E3A5E), Color(0xFF0E1A2E), Color(0xFF060C18))), size = Size(w, h))
                // Blueprint grid
                for (i in 0 until 18) drawLine(Color(0xFF9FC8E8).copy(alpha = 0.06f), Offset(i * w / 17f, 0f), Offset(i * w / 17f, h), strokeWidth = 0.6f)
                for (i in 0 until 22) drawLine(Color(0xFF9FC8E8).copy(alpha = 0.06f), Offset(0f, i * h / 21f), Offset(w, i * h / 21f), strokeWidth = 0.6f)
                // Cyan glow
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF4FA8E8).copy(alpha = 0.14f), Color(0xFF4FA8E8).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.40f, h * 0.38f))
                // Gear, right
                val gx = w * 0.74f; val gy = h * 0.40f; val gr = w * 0.09f
                for (i in 0 until 12) {
                    val a = Math.toRadians((30.0 * i)).toFloat()
                    val c1 = Offset(gx + kotlin.math.cos(a) * gr * 0.82f, gy + kotlin.math.sin(a) * gr * 0.82f)
                    val c2 = Offset(gx + kotlin.math.cos(a) * gr * 1.12f, gy + kotlin.math.sin(a) * gr * 1.12f)
                    drawLine(Color(0xFF9FC8E8).copy(alpha = 0.6f), c1, c2, strokeWidth = 2.4f)
                }
                drawCircle(Color(0xFF9FC8E8).copy(alpha = 0.55f), gr, Offset(gx, gy), style = Stroke(1.6f))
                drawCircle(Color(0xFF9FC8E8).copy(alpha = 0.5f), gr * 0.5f, Offset(gx, gy), style = Stroke(1.2f))
                drawCircle(Color(0xFF9FC8E8).copy(alpha = 0.8f), gr * 0.12f, Offset(gx, gy))
                // Dimension lines, bottom-left
                drawLine(Color(0xFF9FC8E8).copy(alpha = 0.6f), Offset(w * 0.10f, h * 0.60f), Offset(w * 0.34f, h * 0.60f), strokeWidth = 1.2f)
                drawLine(Color(0xFF9FC8E8).copy(alpha = 0.6f), Offset(w * 0.10f, h * 0.57f), Offset(w * 0.10f, h * 0.63f), strokeWidth = 1f)
                drawLine(Color(0xFF9FC8E8).copy(alpha = 0.6f), Offset(w * 0.34f, h * 0.57f), Offset(w * 0.34f, h * 0.63f), strokeWidth = 1f)
                drawLine(Color(0xFF9FC8E8).copy(alpha = 0.4f), Offset(w * 0.22f, h * 0.60f), Offset(w * 0.22f, h * 0.78f), strokeWidth = 0.8f)
                // Drafting triangle, bottom-left
                drawPath(Path().apply { moveTo(w * 0.12f, h * 0.78f); lineTo(w * 0.34f, h * 0.78f); lineTo(w * 0.12f, h * 0.64f); close() }, Color(0xFF4FA8E8).copy(alpha = 0.3f), style = Stroke(1.3f))
                // Small circles (holes)
                drawCircle(Color(0xFF9FC8E8).copy(alpha = 0.5f), w * 0.008f, Offset(w * 0.22f, h * 0.30f))
                drawCircle(Color(0xFF9FC8E8).copy(alpha = 0.5f), w * 0.008f, Offset(w * 0.56f, h * 0.22f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF02060C).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FA8E8), badgeInk = Color(0xFF0E1A2E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8F2FA),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4FA8E8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4D8E8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF4FA8E8).copy(alpha = 0.72f)
        )
        // ═══ OCEANS — light shaft, fish school, coral bed ═══
        cat == "OCEANS" -> SignatureDesign(
            bg = Color(0xFF0A1A2E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E4A6E), Color(0xFF0A1A2E), Color(0xFF040A14))), size = Size(w, h))
                // Sun shaft from the surface, top-right
                val shaft = Path().apply {
                    moveTo(w * 0.60f, 0f)
                    lineTo(w * 0.30f, h * 0.75f)
                    lineTo(w * 0.50f, h * 0.75f)
                    lineTo(w * 0.80f, 0f)
                    close()
                }
                drawPath(shaft, Color(0xFF9FD8F2).copy(alpha = 0.06f))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF9FD8F2).copy(alpha = 0.22f), Color(0xFF9FD8F2).copy(alpha = 0f))), radius = w * 0.14f, center = Offset(w * 0.70f, 0f))
                // Surface shimmer line
                drawLine(Color(0xFF9FD8F2).copy(alpha = 0.30f), Offset(w * 0.02f, h * 0.06f), Offset(w * 0.98f, h * 0.06f), strokeWidth = 1f)
                // Deepening depth bands
                listOf(Pair(0.55f, 0.10f), Pair(0.68f, 0.16f), Pair(0.81f, 0.22f)).forEach { (y, a) ->
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF1E4A6E).copy(alpha = 0f), Color(0xFF061224).copy(alpha = a)), startY = h * y), topLeft = Offset(0f, h * y), size = Size(w, h * (1f - y)))
                }
                // Fish school swimming left, mid-left
                fun fish(cx: Float, cy: Float, scale: Float, alpha: Float) {
                    drawPath(Path().apply {
                        moveTo(cx - w * 0.03f * scale, cy)
                        cubicTo(cx - w * 0.012f * scale, cy - w * 0.022f * scale, cx + w * 0.03f * scale, cy - w * 0.018f * scale, cx + w * 0.038f * scale, cy)
                        cubicTo(cx + w * 0.03f * scale, cy + w * 0.018f * scale, cx - w * 0.012f * scale, cy + w * 0.022f * scale, cx - w * 0.03f * scale, cy)
                        close()
                    }, Color(0xFF9FD8F2).copy(alpha = alpha))
                    drawPath(Path().apply { moveTo(cx + w * 0.032f * scale, cy); lineTo(cx + w * 0.052f * scale, cy - w * 0.014f * scale); lineTo(cx + w * 0.052f * scale, cy + w * 0.014f * scale); close() }, Color(0xFF9FD8F2).copy(alpha = alpha))
                    drawCircle(Color(0xFF0A1A2E), w * 0.004f * scale, Offset(cx + w * 0.012f * scale, cy - w * 0.005f * scale))
                }
                fish(w * 0.34f, h * 0.34f, 1.2f, 0.55f)
                fish(w * 0.22f, h * 0.44f, 0.9f, 0.42f)
                fish(w * 0.44f, h * 0.48f, 0.8f, 0.38f)
                // Bubbles rising from the coral
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 12) {
                    val bx = w * 0.16f + ((s * (i+1) * 3571) % 100) / 100f * w * 0.14f
                    val by = h * 0.86f - ((s * (i+1) * 4201) % 100) / 100f * h * 0.5f
                    drawCircle(Color(0xFFB8E8F2).copy(alpha = 0.35f), 1.0f + (i % 3) * 0.6f, Offset(bx, by), style = Stroke(1f))
                }
                // Coral + seaweed bed along the bottom
                drawOval(Color(0xFF2E6A9E).copy(alpha = 0.35f), topLeft = Offset(0f, h * 0.84f), size = Size(w, h * 0.16f))
                listOf(Offset(w * 0.08f, h * 0.86f), Offset(w * 0.16f, h * 0.88f), Offset(w * 0.24f, h * 0.85f)).forEach { c ->
                    drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x - w * 0.010f, c.y - w * 0.05f); lineTo(c.x, c.y - w * 0.10f); lineTo(c.x + w * 0.010f, c.y - w * 0.05f); close() }, Color(0xFF6BB8E8).copy(alpha = 0.35f))
                }
                for (i in 0 until 8) {
                    val gx = w * 0.30f + i * w * 0.07f
                    drawLine(Color(0xFF4E8AAE).copy(alpha = 0.30f), Offset(gx, h * 0.90f), Offset(gx + w * 0.006f, h * 0.80f + (i % 3) * h * 0.012f), strokeWidth = 1.2f)
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF020810).copy(alpha = 0.6f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF9FD8F2), badgeInk = Color(0xFF0A1A2E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFEAF6FC),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF9FD8F2),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC4DCE8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF9FD8F2).copy(alpha = 0.72f)
        )
        // ═══ QUOTES — ivory page, giant quote marks, gold flourish ═══
        cat == "QUOTES" -> SignatureDesign(
            bg = Color(0xFFF2ECDC), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFFBF2), Color(0xFFF2ECDC), Color(0xFFE2D8C0))), size = Size(w, h))
                // Soft warm glow, center
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFF2C879).copy(alpha = 0.14f), Color(0xFFF2C879).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.5f, h * 0.30f))
                // Giant opening quote mark
                drawPath(Path().apply {
                    moveTo(w * 0.14f, h * 0.18f)
                    cubicTo(w * 0.06f, h * 0.22f, w * 0.05f, h * 0.34f, w * 0.12f, h * 0.38f)
                    cubicTo(w * 0.19f, h * 0.41f, w * 0.24f, h * 0.34f, w * 0.21f, h * 0.27f)
                    cubicTo(w * 0.19f, h * 0.20f, w * 0.15f, h * 0.16f, w * 0.10f, h * 0.16f)
                }, Color(0xFFC9A24F).copy(alpha = 0.85f), style = Stroke(2.6f))
                drawPath(Path().apply {
                    moveTo(w * 0.24f, h * 0.18f)
                    cubicTo(w * 0.16f, h * 0.22f, w * 0.15f, h * 0.34f, w * 0.22f, h * 0.38f)
                    cubicTo(w * 0.29f, h * 0.41f, w * 0.34f, h * 0.34f, w * 0.31f, h * 0.27f)
                    cubicTo(w * 0.29f, h * 0.20f, w * 0.25f, h * 0.16f, w * 0.20f, h * 0.16f)
                }, Color(0xFFC9A24F).copy(alpha = 0.85f), style = Stroke(2.6f))
                // Closing quote mark, mirrored, bottom-right
                drawPath(Path().apply {
                    moveTo(w * 0.86f, h * 0.78f)
                    cubicTo(w * 0.94f, h * 0.74f, w * 0.95f, h * 0.62f, w * 0.88f, h * 0.58f)
                    cubicTo(w * 0.81f, h * 0.55f, w * 0.76f, h * 0.62f, w * 0.79f, h * 0.69f)
                    cubicTo(w * 0.81f, h * 0.76f, w * 0.85f, h * 0.80f, w * 0.90f, h * 0.80f)
                }, Color(0xFFC9A24F).copy(alpha = 0.55f), style = Stroke(2.2f))
                // Gold rules
                drawLine(Color(0xFFC9A24F).copy(alpha = 0.5f), Offset(w * 0.10f, h * 0.50f), Offset(w * 0.90f, h * 0.50f), strokeWidth = 0.8f)
                drawLine(Color(0xFFC9A24F).copy(alpha = 0.3f), Offset(w * 0.18f, h * 0.53f), Offset(w * 0.82f, h * 0.53f), strokeWidth = 0.6f)
                // Flourish scroll, bottom
                drawPath(Path().apply {
                    moveTo(w * 0.20f, h * 0.88f)
                    cubicTo(w * 0.40f, h * 0.82f, w * 0.60f, h * 0.94f, w * 0.80f, h * 0.88f)
                    cubicTo(w * 0.72f, h * 0.86f, w * 0.64f, h * 0.87f, w * 0.60f, h * 0.90f)
                }, Color(0xFFC9A24F).copy(alpha = 0.45f), style = Stroke(1.4f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF8A7A52).copy(alpha = 0.18f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.9f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A24F), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 30.sp, titleLineHeight = 36.sp, titleColor = Color(0xFF3A2E18),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8A6A2E),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF4E4228).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A24F).copy(alpha = 0.65f)
        )
        // ═══ WILDCARD — comet streak, sparkles, coral glow orb ═══
        cat == "WILDCARD" -> SignatureDesign(
            bg = Color(0xFF16162A), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF3A2A5E), Color(0xFF16162A), Color(0xFF0A0A14)), center = Offset(w * 0.5f, h * 0.38f), radius = w * 0.95f), size = Size(w, h))
                // Coral glow orb
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFF7A6B).copy(alpha = 0.28f), Color(0xFFFF7A6B).copy(alpha = 0f))), radius = w * 0.26f, center = Offset(w * 0.74f, h * 0.30f))
                // Starfield
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 70) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color.White.copy(alpha = 0.15f + (i % 4) * 0.06f), 0.7f + (i % 3) * 0.4f, Offset(x, y))
                }
                // Comet streak
                drawLine(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color(0xFFFF7A6B).copy(alpha = 0.3f), Color.Transparent), start = Offset(w * 0.16f, h * 0.20f), end = Offset(w * 0.34f, h * 0.32f)), start = Offset(w * 0.16f, h * 0.20f), end = Offset(w * 0.36f, h * 0.34f), strokeWidth = 2f)
                drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color(0xFFFFC4B8)), center = Offset(w * 0.16f, h * 0.20f), radius = w * 0.014f), radius = w * 0.010f, center = Offset(w * 0.16f, h * 0.20f))
                // Sparkles
                for (i in 0 until 16) {
                    val x = ((s * (i+1) * 3571) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 4201) % 10000) / 10000f * h * 0.8f
                    drawStar(x, y, 2.2f, 1f, Color(0xFFFFD9C4).copy(alpha = 0.5f))
                }
                // Coral accent orbs
                listOf(Offset(w * 0.30f, h * 0.72f), Offset(w * 0.62f, h * 0.82f), Offset(w * 0.42f, h * 0.60f)).forEach { c ->
                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFF7A6B).copy(alpha = 0.22f), Color(0xFFFF7A6B).copy(alpha = 0f))), radius = w * 0.06f, center = c)
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF06060E).copy(alpha = 0.6f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF7A6B), badgeInk = Color(0xFF1E1026),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF8E0E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFF7A6B),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0C4C8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF7A6B).copy(alpha = 0.72f)
        )
        // ═══ FALLBACK — deep neutral atmosphere, quiet stars ═══
        else -> SignatureDesign(
            bg = Color(0xFF14141C), cornerRadius = 6f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2A38), Color(0xFF14141C), Color(0xFF0A0A10))), size = Size(w, h))
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF6A6A8A).copy(alpha = 0.14f), Color(0xFF6A6A8A).copy(alpha = 0f))), radius = w * 0.30f, center = Offset(w * 0.70f, h * 0.30f))
                val s = (w * 1000 + h).toInt()
                for (i in 0 until 50) {
                    val x = ((s * (i+1) * 7919) % 10000) / 10000f * w
                    val y = ((s * (i+1) * 6271) % 10000) / 10000f * h
                    drawCircle(Color.White.copy(alpha = 0.12f), 0.8f + (i % 3) * 0.4f, Offset(x, y))
                }
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF050508).copy(alpha = 0.55f)), center = Offset(w * 0.5f, h * 0.45f), radius = w * 0.85f), size = Size(w, h))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF6A6A8A), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8E4F0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8A8AB0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8C4D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF8A8AB0).copy(alpha = 0.65f)
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════
// STYLE 7 — CUSTOM (topic-specific unique design, 50+ topics)
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun CustomCard(
    display: String, topicName: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null
) {
    val body = quoteText ?: factText
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display
    val sig = topicVariant(topicName, family) ?: signatureDesign(categoryName, family)

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(sig.cornerRadius.dp)).background(sig.bg, RoundedCornerShape(sig.cornerRadius.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            sig.drawBackground(this, w, h)
        }
        Column(modifier = Modifier.fillMaxSize().padding(sig.padding)) {
            Surface(shape = RoundedCornerShape(sig.badgeRadius), color = sig.badgeColor) {
                Row(Modifier.padding(horizontal = sig.badgeHPadding, vertical = sig.badgeVPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CurioIcon(name = categoryGlyph, tint = sig.badgeInk, size = sig.badgeIconSize)
                    Text(categoryName.uppercase(), style = TextStyle(
                        fontFamily = GeomFontFamily, fontSize = sig.badgeFontSize,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = sig.badgeLetterSpacing,
                        color = sig.badgeInk
                    ), maxLines = 1)
                }
            }
            Spacer(Modifier.height(sig.titleTopSpacer))
            Text(title, style = TextStyle(
                fontFamily = sig.titleFont, fontSize = sig.titleSize,
                lineHeight = sig.titleLineHeight, color = sig.titleColor
            ), maxLines = 4, overflow = TextOverflow.Ellipsis)
            val metaParts = mutableListOf<String>()
            if (quoteText == null && byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(sig.metaSpacer))
                Text(metaParts.joinToString(sig.metaSeparator), style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = sig.metaSize, color = sig.metaColor
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            val bodySize = when {
                body.length > 350 -> sig.bodySize - 2.5f
                body.length > 260 -> sig.bodySize - 1.5f
                body.length > 180 -> sig.bodySize - 0.5f
                else -> sig.bodySize
            }.coerceAtLeast(7f)
            Text(body, style = TextStyle(
                fontFamily = LoraFontFamily, fontSize = bodySize.sp,
                lineHeight = (bodySize * sig.bodyLineHeight).sp,
                color = sig.bodyColor
            ), maxLines = if (aspect == ShareCardAspect.PORTRAIT) 14 else 10, overflow = TextOverflow.Ellipsis)
            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(6.dp))
                StarRow(ratingStars, palette)
            }
            Spacer(Modifier.height(sig.footerSpacer))
            Text(
                if (sharerName.isNotBlank()) "$sharerName \u00b7 Curio" else "Curio",
                style = TextStyle(fontFamily = sig.footerFont, fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold, color = sig.footerColor),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeaderRow(categoryName: String, glyph: String, palette: ShareCardPalette) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(14.dp), color = palette.accent.copy(alpha = 0.85f)) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CurioIcon(name = glyph, tint = Color.White, size = 14.dp)
                Text(categoryName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
        }
        CurioIcon(name = CurioIcons.Lightbulb, tint = palette.ink.copy(alpha = 0.30f), size = 18.dp)
    }
}

@Composable
private fun MiddleContent(
    display: String, factText: String, aspect: ShareCardAspect,
    palette: ShareCardPalette, ratingStars: Int?,
    quoteText: String?, qSize: TextUnit, quoteAuthor: String?,
    byline: String = "", year: String? = null,
    bodyScale: Float = 1f
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (quoteText != null) {
            CurioIcon(name = CurioIcons.FormatQuote, tint = palette.ink.copy(alpha = 0.20f), size = 32.dp)
            Text(quoteText, style = MaterialTheme.typography.titleLarge.copy(fontFamily = LoraFontFamily, fontSize = qSize, lineHeight = (qSize.value * 1.28f).sp), color = palette.ink, maxLines = if (aspect == ShareCardAspect.PORTRAIT) 12 else 8, overflow = TextOverflow.Ellipsis)
        } else {
            // Title
            Text(display, style = MaterialTheme.typography.headlineLarge.copy(fontFamily = ChangaOneFontFamily, lineHeight = 40.sp), color = palette.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
            // Metadata line — byline • year (e.g. "GUNS N' ROSES • 1987")
            val metaParts = mutableListOf<String>()
            if (byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            if (metaParts.isNotEmpty()) {
                Text(
                    metaParts.joinToString(" \u2022 "),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = LoraFontFamily, fontWeight = FontWeight.SemiBold),
                    color = palette.ink.copy(alpha = 0.50f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (ratingStars != null && ratingStars > 0) StarRow(ratingStars, palette)
            FrostPane(palette) {
                val qfs = if (aspect == ShareCardAspect.CLASSIC) quickFactFontSize34(factText.length) else quickFactFontSize(factText.length)
                val qfsScaled = (qfs.value * bodyScale).sp
                Text(factText, style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = LoraFontFamily, fontSize = qfsScaled,
                    lineHeight = (qfsScaled.value * 1.4f).sp
                ), color = palette.ink, maxLines = if (aspect == ShareCardAspect.PORTRAIT) 20 else 14, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FrostPane(palette: ShareCardPalette, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().drawBehind {
        val c = CornerRadius(18.dp.toPx())
        drawRoundRect(Color.Black.copy(alpha = 0.21f), Offset(0f, 3.dp.toPx()), Size(size.width, size.height), c)
        drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.35f))), Offset.Zero, Size(size.width, size.height), c)
        drawRoundRect(palette.accent.copy(alpha = 0.28f), Offset.Zero, Size(size.width, size.height), c, style = Stroke(0.8.dp.toPx()))
    }.padding(18.dp)) { content() }
}

@Composable
private fun StarRow(rating: Int, palette: ShareCardPalette) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(5) { i ->
                CurioIcon(name = if (i < rating) CurioIcons.Star else CurioIcons.StarOutline, tint = if (i < rating) palette.accent else palette.ink.copy(alpha = 0.25f), size = 20.dp)
            }
        }
    }
}

@Composable
private fun Footer(sharerName: String, quoteText: String?, quoteAuthor: String?, palette: ShareCardPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CurioIcon(name = CurioIcons.Lightbulb, tint = palette.ink.copy(alpha = 0.30f), size = 14.dp)
        Spacer(Modifier.height(3.dp))
        if (quoteText != null && !quoteAuthor.isNullOrBlank()) {
            Text("— $quoteAuthor", style = MaterialTheme.typography.labelMedium.copy(fontFamily = LoraFontFamily, fontWeight = FontWeight.SemiBold), color = palette.ink.copy(alpha = 0.70f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Spacer(Modifier.height(2.dp))
        }
        Text(
            if (quoteText != null) { if (sharerName.isNotBlank()) "$sharerName ~ Stay Curious" else "Stay Curious" }
            else { if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio" },
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = GeomFontFamily, fontWeight = FontWeight.SemiBold),
            color = palette.ink.copy(alpha = 0.60f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CANVAS DRAWING HELPERS
// ═══════════════════════════════════════════════════════════════════════
private fun DrawScope.drawPaperTexture(palette: ShareCardPalette) {
    val w = size.width; val h = size.height; val s = (w * 1000 + h).toInt()
    // Dense grain — many small dots at varying opacity (v... — grain bumped up)
    for (i in 0 until 220) {
        val x = ((s * (i + 1) * 7919) % 10000) / 10000f * w
        val y = ((s * (i + 1) * 6271) % 10000) / 10000f * h
        val a = 0.05f + ((s * (i + 1) * 3571) % 100) / 100f * 0.10f
        val r = 1.1f + ((s * (i + 1) * 4201) % 100) / 100f * 2.4f
        drawCircle(palette.ink.copy(alpha = a), r, Offset(x, y))
    }
    // Speckle — larger, sparser spots for paper fiber feel
    for (i in 0 until 46) {
        val x = ((s * (i + 1) * 9113) % 10000) / 10000f * w
        val y = ((s * (i + 1) * 5381) % 10000) / 10000f * h
        drawCircle(palette.ink.copy(alpha = 0.10f), 3.2f + ((s * (i + 1) * 7727) % 100) / 100f * 3.2f, Offset(x, y))
    }
}

/** Horizontal fiber lines for a realistic paper look. */
private fun DrawScope.drawPaperFibers(palette: ShareCardPalette) {
    val w = size.width; val h = size.height; val s = (w * 1000 + h).toInt()
    for (i in 0 until 12) {
        val y = ((s * (i + 1) * 4637) % 10000) / 10000f * h
        val x0 = ((s * (i + 1) * 2371) % 10000) / 10000f * w * 0.3f
        val x1 = x0 + ((s * (i + 1) * 6529) % 10000) / 10000f * w * 0.4f + w * 0.1f
        drawLine(palette.ink.copy(alpha = 0.07f), Offset(x0, y), Offset(x1, y), strokeWidth = 0.8f)
    }
}

private fun DrawScope.drawTornBottom(palette: ShareCardPalette) {
    val w = size.width; val h = size.height; val ty = h * 0.88f
    val tint = palette.ink.copy(alpha = 0.28f)
    fun tearY(x: Float): Float = ty + sin(x * 0.03f + 1.7f) * 10f + sin(x * 0.08f + 3.2f) * 4f
    val p = Path().apply {
        moveTo(0f, h); var x = 0f
        while (x <= w) { lineTo(x, tearY(x)); x += w / 50f }
        lineTo(w, h); close()
    }
    drawPath(p, tint)
}

private fun DrawScope.drawNaturalTearPanel(tearY: Float, top: Color, edge: Color, shadow: Color) {
    val w = size.width
    fun yAt(x: Float): Float {
        val t = x / w
        val tooth = ((t * 23f).toInt() % 3 - 1) * 5.5f
        return tearY + sin(t * 7.2f + 0.8f) * 11f + sin(t * 27f + 1.9f) * 5f +
            sin(t * 73f + 3.1f) * 2f + tooth
    }
    val shadowPath = Path().apply {
        moveTo(0f, yAt(0f) + 7f)
        var x = 0f
        while (x <= w) { lineTo(x, yAt(x) + 7f); x += w / 64f }
        lineTo(w, yAt(w) + 18f); lineTo(0f, yAt(0f) + 18f); close()
    }
    drawPath(shadowPath, shadow)
    val topPath = Path().apply {
        moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, yAt(w))
        var x = w
        while (x >= 0f) { lineTo(x, yAt(x)); x -= w / 72f }
        close()
    }
    drawPath(topPath, top)
    var x = 0f
    while (x <= w) {
        val y = yAt(x)
        drawLine(edge.copy(alpha = 0.55f), Offset(x, y + 1f), Offset((x + w / 90f).coerceAtMost(w), yAt((x + w / 90f).coerceAtMost(w)) + 1f), strokeWidth = 1.3f)
        if ((x / (w / 12f)).toInt() % 3 == 0) {
            drawLine(edge.copy(alpha = 0.35f), Offset(x, y + 2f), Offset((x + 10f).coerceAtMost(w), y + 5f), strokeWidth = 0.8f)
        }
        x += w / 48f
    }
}

private fun DrawScope.drawTornLine(y: Float, w: Float, above: Color, below: Color) {
    // Shadow
    val sp = Path().apply {
        moveTo(0f, y + 2f); var x = 0f
        while (x <= w) { lineTo(x, y + sin(x * 0.04f + 2.1f) * 3.5f + 2f); x += w / 35f }
        lineTo(w, y + 10f); lineTo(0f, y + 10f); close()
    }
    drawPath(sp, Color.Black.copy(alpha = 0.21f))
    // Tear
    val tp = Path().apply {
        moveTo(0f, y); var x = 0f
        while (x <= w) { lineTo(x, y + sin(x * 0.04f + 2.1f) * 3.5f); x += w / 35f }
        lineTo(w, y + 16f); lineTo(0f, y + 16f); close()
    }
    drawPath(tp, above)
    // Edge highlight
    val ep = Path().apply {
        moveTo(0f, y - 1f); var x = 0f
        while (x <= w) { lineTo(x, y + sin(x * 0.04f + 2.1f) * 3.5f - 1f); x += w / 35f }
    }
    drawPath(ep, Color.White.copy(alpha = 0.45f), style = Stroke(1.2f))
}

/** Vinyl record — partial, bleeding off right edge, with accent-colored label. */
private fun DrawScope.drawVinylPartial(labelColor: Color) {
    val cx = size.width * 0.5f; val cy = size.height * 0.5f
    val r = minOf(cx, cy) * 0.92f
    // Drop shadow
    drawCircle(Color.Black.copy(alpha = 0.35f), r + 5f, Offset(cx + 2f, cy + 3f))
    // Black disc
    drawCircle(Color(0xFF151515), r, Offset(cx, cy))
    // Grooves
    for (i in 8 downTo 1) drawCircle(Color.White.copy(alpha = 0.21f), r * (0.38f + i * 0.065f), Offset(cx, cy), style = Stroke(0.7f))
    // Highlight arc
    drawArc(Color.White.copy(alpha = 0.18f), -50f, 65f, false, Offset(cx - r * 0.82f, cy - r * 0.82f), Size(r * 1.64f, r * 1.64f), style = Stroke(r * 0.22f))
    // Label
    val lr = r * 0.28f
    drawCircle(Brush.radialGradient(listOf(labelColor, labelColor.copy(alpha = 0.70f)), center = Offset(cx - lr * 0.15f, cy - lr * 0.15f), radius = lr * 1.2f), lr, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.35f), lr * 0.78f, Offset(cx, cy), style = Stroke(1.2f))
    // Center hole
    drawCircle(Color(0xFF171717), r * 0.035f, Offset(cx, cy))
}

// ═══════════════════════════════════════════════════════════════════════
// WATERMARK
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun Watermark(family: CategoryFamily, glyph: String, tint: Color, seed: Int) {
    val symbols = CurioIcons.heroWatermarkSymbols(family)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value; val h = maxHeight.value
        val half = 21
        // Random tilts per glyph for organic feel — seeded from position
        val tilts = listOf(-12f, 8f, -5f, 15f, -18f)
        listOf(0.14f to 0.16f, 0.86f to 0.16f, 0.14f to 0.84f, 0.86f to 0.84f).forEachIndexed { i, (x, y) ->
            CurioIcon(name = symbols[i % symbols.size], tint = tint, size = 42.dp,
                modifier = Modifier.offset((w * x - half).dp, (h * y - half).dp)
                    .graphicsLayer { rotationZ = tilts[i % tilts.size] })
        }
        // Center watermark — fainter, larger, with its own tilt
        CurioIcon(name = symbols[seed.mod(symbols.size)], tint = tint.copy(alpha = tint.alpha * 0.5f), size = 80.dp,
            modifier = Modifier.offset((w * 0.5f - 40).dp, (h * 0.5f - 40).dp)
                .graphicsLayer { rotationZ = -7f })
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ARRANGE OVERLAY (Paper) — tap-and-hold edit mode
// ═══════════════════════════════════════════════════════════════════════
/**
 * Overlays the Paper card in arrange mode: a draggable TITLE handle, a
 * draggable + edge-resizable QUICK FACT box, and a small fact-size adjuster
 * with Done / Reset. Positions are written back as fractions so the exported
 * card matches exactly. Per-share state (driven by the sheet).
 */
@Composable
private fun ShareCardArrangeOverlay(
    arrangement: ShareCardArrangement,
    onArrangementChange: (ShareCardArrangement) -> Unit,
    onBodyScaleChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cw = maxWidth.value; val ch = maxHeight.value
        if (cw == 0f || ch == 0f) return@BoxWithConstraints

        var titleOff by remember { mutableStateOf(IntOffset((cw * arrangement.titleX).toInt() + 4, (ch * arrangement.titleY).toInt() - 20)) }
        var bodyOff by remember { mutableStateOf(IntOffset((cw * arrangement.bodyX).toInt(), (ch * arrangement.bodyY).toInt() - 6)) }
        var bodyW by remember(arrangement.bodyWidthFrac) { mutableStateOf((cw * arrangement.bodyWidthFrac).coerceIn(0.15f * cw, 0.98f * cw)) }

        // ── Title handle (drag to move) ──
        Box(
            Modifier
                .offset { titleOff }
                .width((cw * 0.46f).dp)
                .padding(2.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, d ->
                        change.consume(); titleOff += IntOffset(d.x.roundToInt(), d.y.roundToInt())
                        onArrangementChange(arrangement.copy(
                            titleX = (titleOff.x.toFloat() / cw).coerceIn(0f, 1f),
                            titleY = (titleOff.y.toFloat() / ch).coerceIn(0f, 0.6f)
                        ))
                    }
                }
        ) {
            Text("DRAG TITLE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(6.dp))
        }

        // ── Quick-fact box (drag to move) ──
        Box(
            Modifier
                .offset { bodyOff }
                .width(bodyW.dp)
                .heightIn(min = 44.dp)
                .padding(2.dp)
                .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, d ->
                        change.consume(); bodyOff += IntOffset(d.x.roundToInt(), d.y.roundToInt())
                        onArrangementChange(arrangement.copy(
                            bodyX = (bodyOff.x.toFloat() / cw).coerceIn(0f, 0.85f),
                            bodyY = (bodyOff.y.toFloat() / ch).coerceIn(0f, 0.95f)
                        ))
                    }
                }
        ) { }

        // ── Right-edge handle (drag to change box width/shape) ──
        Box(
            Modifier
                .offset { IntOffset(bodyOff.x + bodyW.toInt() - 8, bodyOff.y) }
                .size(18.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, d ->
                        change.consume()
                        bodyW = (bodyW + d.x).coerceIn(cw * 0.15f, cw * 0.98f)
                        onArrangementChange(arrangement.copy(bodyWidthFrac = (bodyW / cw).coerceIn(0.15f, 0.98f)))
                    }
                }
        ) { }

        // ── Controls: fact-size +/−, Done, Reset ──
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                Surface(onClick = { onBodyScaleChange((arrangement.bodyScale - 0.15f).coerceIn(0.5f, 1.8f)) },
                    shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text("\u2212", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimary), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
                Text("  Fact size  ", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                Surface(onClick = { onBodyScaleChange((arrangement.bodyScale + 0.15f).coerceIn(0.5f, 1.8f)) },
                    shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text("\u002b", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimary), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { onArrangementChange(ShareCardArrangement()); onBodyScaleChange(1f) }) { Text("Reset") }
                Button(onClick = onDone, shape = RoundedCornerShape(50)) { Text("Done") }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SHARE SHEET
// ═══════════════════════════════════════════════════════════════════════
/** Wraps the preview card with the tap-and-hold edit toggle + arrange overlay. */
@Composable
private fun ArrangeableCard(
    active: Boolean,
    arrangement: ShareCardArrangement,
    onArrangementChange: (ShareCardArrangement) -> Unit,
    onBodyScaleChange: (Float) -> Unit,
    editMode: Boolean,
    onToggleEdit: () -> Unit,
    card: @Composable () -> Unit
) {
    Box(modifier = Modifier.pointerInput(active) {
        if (active) detectTapGestures(onLongPress = { onToggleEdit() })
    }) {
        card()
        if (active && editMode) {
            ShareCardArrangeOverlay(arrangement, onArrangementChange, onBodyScaleChange, onToggleEdit)
        }
    }
}

@Composable
fun TopicShareSheet(
    topicName: String, categoryName: String, categoryGlyph: String,
    accent: Color, quickFact: String, authority: String,
    context: android.content.Context, savedSources: List<ShareCardContent> = emptyList(),
    onDismiss: () -> Unit, categoryFamily: CategoryFamily = CategoryFamily.WILDCARD,
    topicByline: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var aspect by rememberSaveable { mutableStateOf(ShareCardAspect.PORTRAIT) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var customText by rememberSaveable { mutableStateOf("") }
    var polaroidCaption by rememberSaveable { mutableStateOf("") }
    var styleIdx by rememberSaveable { mutableIntStateOf(0) }
    var classicDesign by rememberSaveable { mutableStateOf(false) }
    // Arrange mode (Paper) — per-share only; resets when the sheet closes.
    // Plain remember: the arrangement is a per-share tweak (not Bundle-saveable)
    // and the modal resets it each time, so it should not survive a rotation.
    var editMode by remember { mutableStateOf(false) }
    // Single source of truth for the arranged layout — bodyScale lives on
    // arrangement so the +/− fact-size control and the card stay in sync.
    var arrangement by remember { mutableStateOf(ShareCardArrangement()) }
    val sharer = AppPreferences.getDisplayName(context).ifBlank { "" }
    // Photo picker state — only used for Collage style
    var userPhoto by rememberSaveable { mutableStateOf<ImageBitmap?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(it)
                )
                bitmap?.let { bmp ->
                    // Center-crop to square for the polaroid
                    val size = minOf(bmp.width, bmp.height)
                    val x = (bmp.width - size) / 2
                    val y = (bmp.height - size) / 2
                    val cropped = android.graphics.Bitmap.createBitmap(bmp, x, y, size, size)
                    userPhoto = cropped.asImageBitmap()
                }
            } catch (_: Exception) { }
        }
    }

    val isQuotes = categoryName == "Quotes"
    val quoteText = if (isQuotes) topicName else quickFact
    val quick = ShareCardContent(QUICK_FACT_ID, "Quick fact", quickFact)
    val quote = ShareCardContent("quote", "Quote", quoteText)
    val custom = ShareCardContent(CUSTOM_FACT_ID, "Custom fact", "")
    // Custom fact + No Fact available for all styles except Quotes
    val noFact = ShareCardContent(NO_FACT_ID, "No fact", "")
    val available = if (isQuotes) listOf(quote) else listOf(quick, noFact) + savedSources + listOf(custom)
    val defaultId = if (isQuotes) quote.id else savedSources.firstOrNull { it.id == "quote" }?.id ?: quick.id
    val activeId = selectedId ?: defaultId
    val activeSource = when (activeId) {
        CUSTOM_FACT_ID -> custom.copy(text = customText.ifBlank { "Add your own fact about this discovery…" })
        NO_FACT_ID -> noFact
        else -> available.firstOrNull { it.id == activeId } ?: quick
    }

    val styles = availableStylesForFamily(categoryFamily, topicName)
    val safeIdx = styleIdx.coerceIn(0, styles.lastIndex)
    val currentStyle = styles[safeIdx]
    val arrangeActive = currentStyle == ShareCardStyle.PAPER
    val arrangeNow = arrangeActive && editMode

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Share this topic", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)

            // The card carousel IS the preview — no separate static card
            val pw = 280.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (styles.size > 1) {
                    // Style label
                    Text(currentStyle.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Swipeable card carousel — the card IS the preview
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = safeIdx) { styles.size }
                    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) { styleIdx = pagerState.currentPage }
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 36.dp),
                        pageSpacing = 16.dp,
                        userScrollEnabled = !editMode,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val isCenter = page == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .width(pw)
                                .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
                                .shadow(if (isCenter) 4.dp else 1.dp, RoundedCornerShape(6.dp))
                                .clip(RoundedCornerShape(6.dp))
                                .graphicsLayer {
                                    val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                                    val scale = 1f - 0.08f * kotlin.math.abs(pageOffset)
                                    scaleX = scale; scaleY = scale
                                    alpha = 1f - 0.25f * kotlin.math.abs(pageOffset)
                                }
                        ) {
                                            val isArrangingPage = arrangeNow && page == pagerState.currentPage
                                            ArrangeableCard(
                                                active = arrangeActive && page == pagerState.currentPage,
                                                arrangement = arrangement,
                                                onArrangementChange = { arrangement = it },
                                                onBodyScaleChange = { arrangement = arrangement.copy(bodyScale = it) },
                                                editMode = editMode,
                                                onToggleEdit = { editMode = !editMode }
                                            ) {
                                                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = styles[page], ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption, classicSignature = classicDesign, onPhotoTap = { photoPickerLauncher.launch("image/*") }, arrangement = if (isArrangingPage) arrangement else null, bodyScale = arrangement.bodyScale)
                                            }
                                        }
                    }
                    // Style dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        styles.forEachIndexed { i, _ ->
                            Box(Modifier.size(if (i == pagerState.currentPage) 7.dp else 5.dp).background(
                                if (i == pagerState.currentPage) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f), CircleShape
                            ))
                        }
                    }
                } else {
                    // Single style — just show the card directly
                    Box(
                        modifier = Modifier
                            .width(pw)
                            .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
                            .shadow(4.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        ArrangeableCard(
                            active = arrangeActive,
                            arrangement = arrangement,
                            onArrangementChange = { arrangement = it },
                            onBodyScaleChange = { arrangement = arrangement.copy(bodyScale = it) },
                            editMode = editMode,
                            onToggleEdit = { editMode = !editMode }
                        ) {
                            TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption, classicSignature = classicDesign, onPhotoTap = { photoPickerLauncher.launch("image/*") }, arrangement = if (arrangeNow) arrangement else null, bodyScale = arrangement.bodyScale)
                        }
                    }
                }
            }

            // Photo picker for Collage — compact row below card
            if (currentStyle == ShareCardStyle.COLLAGE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = { photoPickerLauncher.launch("image/*") }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(40.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CurioIcon(name = CurioIcons.PhotoLibrary, tint = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                            Text(
                                if (userPhoto != null) "Change" else "Photo",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedTextField(
                        value = polaroidCaption,
                        onValueChange = { polaroidCaption = it.take(36) },
                        placeholder = { Text(if (sharer.isNotBlank()) "$sharer \u00b7 via Curio" else "via Curio", style = MaterialTheme.typography.labelMedium) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(40.dp)
                    )
                }
            }

            // Favorite song — Vinyl corner chip (persisted to AppPreferences so
            // it sticks across shares).
            if (currentStyle == ShareCardStyle.VINYL) {
                OutlinedTextField(
                    value = AppPreferences.favoriteSongState,
                    onValueChange = { AppPreferences.setFavoriteSong(context, it.take(40)) },
                    placeholder = { Text("Your favorite song (shown on Vinyl)", style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Arrange hint (Paper) — tap & hold the card to move/resize
            if (arrangeActive && !editMode) {
                Text("Hold the card to move / resize the title & quick fact",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }

            // Aspect
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(ShareCardAspect.PORTRAIT.label, CurioIcons.Image, aspect == ShareCardAspect.PORTRAIT) { aspect = ShareCardAspect.PORTRAIT }
                Pill(ShareCardAspect.CLASSIC.label, CurioIcons.Image, aspect == ShareCardAspect.CLASSIC) { aspect = ShareCardAspect.CLASSIC }
            }

            // Design variant — Classic signature designs restored from the
            // f6dd7f19 redesign, available as an extra beside the current ones
            if (currentStyle == ShareCardStyle.SIGNATURE) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Current", CurioIcons.AutoAwesome, !classicDesign) { classicDesign = false }
                    Pill("Classic", CurioIcons.AutoAwesome, classicDesign) { classicDesign = true }
                }
            }

            // Sources
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                available.filter { !isQuotes || it.id != QUICK_FACT_ID }.forEach { opt ->
                    Pill(opt.label + (opt.rating?.takeIf { r -> r > 0 }?.let { " · " + "★".repeat(it) } ?: ""), CurioIcons.FormatText, opt.id == activeSource.id) { selectedId = opt.id }
                }
            }

            if (activeId == CUSTOM_FACT_ID) {
                OutlinedTextField(customText, { customText = it }, placeholder = { Text("Your custom fact", style = MaterialTheme.typography.bodyMedium) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(4.dp))
            // Save + Share buttons — side by side
            val eh = pw * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                // Save button
                OutlinedButton(onClick = {
                    shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                        TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption, arrangement = if (arrangeNow) arrangement else null, bodyScale = arrangement.bodyScale)
                    }, saveToGallery = true); onDismiss()
                }, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("\u2B07", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(4.dp))
                    Text("Save", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
                // Share button
                Button(onClick = {
                    shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                        TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption, classicSignature = classicDesign, arrangement = if (arrangeNow) arrangement else null, bodyScale = arrangement.bodyScale)
                    }); onDismiss()
                }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary), modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("Share", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }
    }
}

/** Hub body used by TopicShareSheet AND EntryShareSheet. */
@Composable
fun ShareHubBody(
    topicName: String, categoryName: String, categoryGlyph: String, accent: Color,
    sharerName: String, authority: String, context: android.content.Context,
    aspect: ShareCardAspect, onAspectChange: (ShareCardAspect) -> Unit,
    sources: List<ShareCardContent>, activeSource: ShareCardContent,
    onSelectSource: (String) -> Unit, customEditing: Boolean, customText: String,
    onCustomTextChange: (String) -> Unit, onShared: () -> Unit,
    categoryFamily: CategoryFamily = CategoryFamily.WILDCARD, topicByline: String = "",
    style: ShareCardStyle = ShareCardStyle.PAPER, onStyleChange: (Int) -> Unit = {},
    classicSignature: Boolean = false, onClassicSignatureChange: (Boolean) -> Unit = {}
) {
    val pw = 280.dp
    val isQ = activeSource.id == "quote"
    val styles = availableStylesForFamily(categoryFamily, topicName)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (styles.size > 1) {
            // Multi-style: carousel IS the preview
            val si = style.ordinal.coerceIn(0, styles.lastIndex)
            val hubPagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = si) { styles.size }
            androidx.compose.runtime.LaunchedEffect(hubPagerState.currentPage) { onStyleChange(hubPagerState.currentPage) }
            Text(style.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.pager.HorizontalPager(
                state = hubPagerState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val isCenter = page == hubPagerState.currentPage
                Box(
                    modifier = Modifier
                        .width(pw)
                        .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
                        .shadow(if (isCenter) 4.dp else 1.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .graphicsLayer {
                            val pageOffset = (hubPagerState.currentPage - page + hubPagerState.currentPageOffsetFraction)
                            val scale = 1f - 0.10f * kotlin.math.abs(pageOffset)
                            scaleX = scale; scaleY = scale
                            alpha = 1f - 0.3f * kotlin.math.abs(pageOffset)
                        }
                ) {
                    TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharerName, aspect = aspect, style = styles[page], ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (isQ) activeSource.text else null, quoteAuthor = if (isQ) topicByline.ifBlank { null } else null, byline = topicByline, classicSignature = classicSignature)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                styles.forEachIndexed { i, _ ->
                    Box(Modifier.size(if (i == hubPagerState.currentPage) 7.dp else 5.dp).background(
                        if (i == hubPagerState.currentPage) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f), CircleShape
                    ))
                }
            }
        } else {
            // Single style: just show the card directly
            Box(
                modifier = Modifier
                    .width(pw)
                    .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
            ) {
                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharerName, aspect = aspect, style = style, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (isQ) activeSource.text else null, quoteAuthor = if (isQ) topicByline.ifBlank { null } else null, byline = topicByline, classicSignature = classicSignature)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(ShareCardAspect.PORTRAIT.label, CurioIcons.Image, aspect == ShareCardAspect.PORTRAIT) { onAspectChange(ShareCardAspect.PORTRAIT) }
            Pill(ShareCardAspect.CLASSIC.label, CurioIcons.Image, aspect == ShareCardAspect.CLASSIC) { onAspectChange(ShareCardAspect.CLASSIC) }
        }
        if (style == ShareCardStyle.SIGNATURE) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Current", CurioIcons.AutoAwesome, !classicSignature) { onClassicSignatureChange(false) }
                Pill("Classic", CurioIcons.AutoAwesome, classicSignature) { onClassicSignatureChange(true) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            sources.filter { !isQ || it.id != QUICK_FACT_ID }.forEach { opt ->
                Pill(opt.label + (opt.rating?.takeIf { r -> r > 0 }?.let { " · " + "★".repeat(it) } ?: ""), CurioIcons.FormatText, opt.id == activeSource.id) { onSelectSource(opt.id) }
            }
        }
        if (customEditing) OutlinedTextField(customText, onCustomTextChange, placeholder = { Text("Your custom fact", style = MaterialTheme.typography.bodyMedium) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
        val eh = pw * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()
        // Save + Share side by side — Save writes the PNG to the gallery,
        // Share launches the chooser (the topic sheet's same two actions).
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = {
                shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                    TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharerName, aspect = aspect, style = style, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (isQ) activeSource.text else null, quoteAuthor = if (isQ) topicByline.ifBlank { null } else null, byline = topicByline, classicSignature = classicSignature)
                }, saveToGallery = true); onShared()
            }, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Save", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
            Button(onClick = {
                shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                    TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharerName, aspect = aspect, style = style, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (isQ) activeSource.text else null, quoteAuthor = if (isQ) topicByline.ifBlank { null } else null, byline = topicByline, classicSignature = classicSignature)
                }); onShared()
            }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary), modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Share", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
        }
    }
}

@Composable
private fun Pill(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(38.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CurioIcon(name = icon, tint = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant, size = 15.dp)
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

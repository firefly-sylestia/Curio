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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.curio.app.ui.theme.ChangaOneFontFamily
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.GeomFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.curio.app.ui.theme.PatrickHandFontFamily
import kotlin.math.sin
import androidx.compose.foundation.rememberScrollState
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
    polaroidCaption: String = ""
) {
    val display = topicName.substringBeforeLast(" (")
    // Extract year from trailing parentheses — "Appetite for Destruction (1987)" → "1987"
    val year = topicName.substringAfterLast("(").substringBeforeLast(")").takeIf { it.all { c -> c.isDigit() } && it.length == 4 }
    val palette = paletteFor(accent)
    when (style) {
        ShareCardStyle.PAPER -> PaperCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.VINYL -> VinylCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.COLLAGE -> CollageCard(display, topicName, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, userPhoto, byline, year, polaroidCaption)
        ShareCardStyle.NEUMORPHIC -> NeumorphicCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.EDITORIAL -> EditorialCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.MINIMAL -> MinimalCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.SIGNATURE -> SignatureCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
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
    byline: String = "", year: String? = null
) {
    val qSize = quoteText?.let { quoteFontSize(it.length) } ?: 0.sp
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

        Column(modifier = modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
            HeaderRow(categoryName, categoryGlyph, palette)
            MiddleContent(display, factText, aspect, palette, ratingStars, quoteText, qSize, quoteAuthor, byline, year)
            Footer(sharerName, quoteText, quoteAuthor, palette)
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
    val whiteLift = Shadow(Color.White.copy(alpha = 0.90f), Offset(0f, 1.4f), blurRadius = 4f)

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
            Text(display, style = TextStyle(
                fontFamily = ChangaOneFontFamily, fontSize = 28.sp,
                lineHeight = 32.sp, fontWeight = FontWeight.Normal, color = inkDark
            ), maxLines = 2, overflow = TextOverflow.Ellipsis)

            // Artist / byline
            if (byline.isNotBlank()) {
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
                factText.length > 280 -> 9.sp; factText.length > 180 -> 10.sp; else -> 10.5.sp
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFFDF0EE).copy(alpha = 0.85f),
                modifier = Modifier.widthIn(max = 220.dp)
            ) {
                Text(factText, style = TextStyle(
                    fontFamily = LoraFontFamily, fontSize = bodySize,
                    lineHeight = (bodySize.value * 1.50f).sp, color = inkDark.copy(alpha = 0.88f)
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

        // ── Info copy — bottom-left, cream box like body text ──
        val is34v = aspect == ShareCardAspect.CLASSIC
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFFDF0EE).copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = if (is34v) 12.dp else 16.dp, bottom = if (is34v) 36.dp else 30.dp)
                .widthIn(max = if (is34v) 150.dp else 180.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(if (is34v) 4.dp else 6.dp), modifier = Modifier.padding(horizontal = if (is34v) 6.dp else 8.dp, vertical = if (is34v) 4.dp else 6.dp)) {
                listOf(
                Triple("nightlight", "LATE-NIGHT", "Themes of reflection & memory"),
                Triple("headphones", "TRACKS", "Curated picks to explore & discover"),
                Triple("workspace_premium", "RECOGNITION", "Explore via Curio")
            ).forEach { (icon, label, detail) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (is34v) 5.dp else 7.dp)) {
                    CurioIcon(name = icon, tint = roseDusty.copy(alpha = 0.92f), size = if (is34v) 10.dp else 12.dp)
                    Column {
                        Text(label, style = TextStyle(fontFamily = GeomFontFamily, fontSize = if (is34v) 5.sp else 6.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp, color = inkDark.copy(alpha = 0.78f)))
                        Text(detail, style = TextStyle(
                            fontFamily = LoraFontFamily, fontSize = if (is34v) 5.5.sp else 7.sp, lineHeight = if (is34v) 7.5.sp else 9.5.sp,
                            color = inkDark.copy(alpha = 0.54f)))
                    }
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
    polaroidCaption: String = ""
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
                .clickable(enabled = userPhoto == null) { /* tap handled by caller */ }
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
                // Handwritten name below photo — more breathing room
                val tm = rememberTextMeasurer()
                val hs = TextStyle(fontFamily = PatrickHandFontFamily, fontWeight = FontWeight.Normal,
                    fontSize = (pW * 0.065f).coerceIn(11f, 15f).sp, color = inkDark,
                    lineHeight = (pW * 0.08f).sp)
                val tl = tm.measure(polaroidLabel, hs, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Canvas(Modifier.offset(8.dp, (pH * 0.78f).dp).size((pW - 16).dp, (pH * 0.18f).dp)) {
                    drawText(tl)
                }
            }

            // Title — large serif, dark green, top-left
            Column(modifier = Modifier.offset(22.dp, (ch * 0.055f).dp).width((cw * 0.56f).dp)) {
                Text(display, style = TextStyle(
                    fontFamily = ChangaOneFontFamily, fontSize = (cw * 0.08f).coerceIn(24f, 34f).sp,
                    lineHeight = (cw * 0.09f).coerceIn(28f, 38f).sp,
                    fontWeight = FontWeight.Normal, color = inkDark
                ), maxLines = 4, overflow = TextOverflow.Ellipsis)

                Spacer(Modifier.height(6.dp))

                // Metadata — small caps, letter-spaced
                val metaParts = mutableListOf<String>()
                if (byline.isNotBlank()) metaParts.add(byline.uppercase())
                if (year != null) metaParts.add(year)
                if (metaParts.isNotEmpty()) {
                    Text(metaParts.joinToString(" \u2022 "), style = TextStyle(
                        fontFamily = LoraFontFamily, fontSize = 9.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold,
                        color = inkDark.copy(alpha = 0.55f)
                    ), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                Spacer(Modifier.height(10.dp))

                // Quote — italic serif in curly quotes
                if (quoteText != null) {
                    Text("\u201c$quoteText\u201d", style = TextStyle(
                        fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                        fontSize = quoteFontSize(quoteText.length),
                        lineHeight = (quoteFontSize(quoteText.length).value * 1.35f).sp,
                        color = inkDark
                    ), maxLines = 3, overflow = TextOverflow.Ellipsis)
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
            val bodySize = when {
                factText.length > 280 -> 10.sp; factText.length > 180 -> 11.sp; else -> 12.sp
            }
            Text(factText, style = TextStyle(
                fontFamily = LoraFontFamily, fontSize = bodySize,
                lineHeight = (bodySize.value * 1.55f).sp, color = Color.White.copy(alpha = 0.92f)
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

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(categoryDeep, RoundedCornerShape(6.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(Brush.verticalGradient(listOf(categoryGlow.copy(alpha = 0.95f), categoryDeep, ink)))
            // Soft ambient glow top-left
            drawCircle(Color.White.copy(alpha = 0.10f), w * 0.72f, Offset(w * 0.12f, h * 0.12f))
            // Depth shadow bottom-right
            drawCircle(Color.Black.copy(alpha = 0.20f), w * 0.74f, Offset(w * 0.95f, h * 0.72f))
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
                Text(display, style = TextStyle(
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
                        letterSpacing = 1.3.sp, color = ink.copy(alpha = 0.56f)
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

        // Left vertical accent rule — bold editorial line
        Canvas(Modifier.padding(start = 22.dp).width(4.dp).fillMaxSize()) {
            drawRect(accentRule.copy(alpha = 0.80f), Offset.Zero, Size(size.width, size.height))
        }

        // Content — editorial layout
        Column(modifier = Modifier.fillMaxSize().padding(start = 36.dp, end = 22.dp, top = 20.dp, bottom = 18.dp)) {
            // Category tag — small caps
            Text(categoryName.uppercase(), style = TextStyle(
                fontFamily = GeomFontFamily, fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
                color = accentRule
            ), maxLines = 1)

            Spacer(Modifier.height(8.dp))

            // Title — large serif
            Text(display, style = TextStyle(
                fontFamily = ChangaOneFontFamily, fontSize = 28.sp,
                lineHeight = 32.sp, color = inkDark
            ), maxLines = 3, overflow = TextOverflow.Ellipsis)

            // Byline + year
            val metaParts = mutableListOf<String>()
            if (byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(metaParts.joinToString(" \u2014 "), style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 12.sp, color = inkDark.copy(alpha = 0.55f)
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(14.dp))

            // Horizontal divider
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(inkDark.copy(alpha = 0.12f), Offset.Zero, Offset(size.width, 0f))
            }

            Spacer(Modifier.height(12.dp))

            // Body text — clean serif
            val bodySize = when {
                body.length > 350 -> 8.5.sp; body.length > 260 -> 9.5.sp
                body.length > 180 -> 10.sp; else -> 11.sp
            }
            Text(body, style = TextStyle(
                fontFamily = LoraFontFamily, fontSize = bodySize,
                lineHeight = (bodySize.value * 1.55f).sp, color = inkDark.copy(alpha = 0.82f)
            ), maxLines = if (aspect == ShareCardAspect.PORTRAIT) 14 else 10, overflow = TextOverflow.Ellipsis)

            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(8.dp))
                StarRow(ratingStars, palette)
            }

            Spacer(Modifier.weight(1f))

            // Bottom credit — editorial style
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(inkDark.copy(alpha = 0.12f), Offset.Zero, Offset(size.width, 0f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (sharerName.isNotBlank()) "$sharerName \u2014 Curio" else "Curio",
                style = TextStyle(fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 10.sp, color = inkDark.copy(alpha = 0.50f)),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
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

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(bg, RoundedCornerShape(6.dp))) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
            // Tiny category dot + name
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).background(accent, CircleShape))
                Text(categoryName.uppercase(), style = TextStyle(
                    fontFamily = GeomFontFamily, fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
                    color = inkDark.copy(alpha = 0.45f)
                ), maxLines = 1)
            }

            Spacer(Modifier.height(20.dp))

            // Title — large, clean, lots of space
            Text(display, style = TextStyle(
                fontFamily = ChangaOneFontFamily, fontSize = 30.sp,
                lineHeight = 34.sp, color = inkDark
            ), maxLines = 3, overflow = TextOverflow.Ellipsis)

            // Byline
            if (byline.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(byline, style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 12.sp, color = inkDark.copy(alpha = 0.50f)
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (year != null) {
                Spacer(Modifier.height(6.dp))
                Text(year, style = TextStyle(
                    fontFamily = LoraFontFamily, fontSize = 12.sp,
                    color = inkDark.copy(alpha = 0.40f)
                ))
            }

            Spacer(Modifier.height(16.dp))

            // Accent divider
            Canvas(Modifier.width(40.dp).height(2.dp)) {
                drawRoundRect(accent, cornerRadius = CornerRadius(1f))
            }

            Spacer(Modifier.height(12.dp))

            // Body text — generous spacing
            val bodySize = when {
                body.length > 350 -> 8.sp; body.length > 260 -> 9.sp
                body.length > 180 -> 10.sp; else -> 11.sp
            }
            Text(body, style = TextStyle(
                fontFamily = LoraFontFamily, fontSize = bodySize,
                lineHeight = (bodySize.value * 1.60f).sp, color = inkDark.copy(alpha = 0.75f)
            ), maxLines = if (aspect == ShareCardAspect.PORTRAIT) 12 else 8, overflow = TextOverflow.Ellipsis)

            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(8.dp))
                StarRow(ratingStars, palette)
            }

            Spacer(Modifier.height(14.dp))

            // Minimal credit
            Text(
                if (sharerName.isNotBlank()) "$sharerName \u00b7 Curio" else "Curio",
                style = TextStyle(fontFamily = GeomFontFamily, fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold, color = inkDark.copy(alpha = 0.30f)),
                maxLines = 1
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
    byline: String = "", year: String? = null
) {
    val body = quoteText ?: factText
    // Get unique design per category
    val sig = signatureDesign(categoryName, family)

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(sig.cornerRadius.dp)).background(sig.bg, RoundedCornerShape(sig.cornerRadius.dp))) {
        // Background pattern/texture
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            sig.drawBackground(this, w, h)
        }

        // Content
        Column(modifier = Modifier.fillMaxSize().padding(sig.padding)) {
            // Category badge
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

            // Title
            Text(display, style = TextStyle(
                fontFamily = sig.titleFont, fontSize = sig.titleSize,
                lineHeight = sig.titleLineHeight, color = sig.titleColor
            ), maxLines = 4, overflow = TextOverflow.Ellipsis)

            // Byline
            val metaParts = mutableListOf<String>()
            if (byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(sig.metaSpacer))
                Text(metaParts.joinToString(sig.metaSeparator), style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = sig.metaSize, color = sig.metaColor
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.weight(1f))

            // Body text
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

            // Footer
            Text(
                if (sharerName.isNotBlank()) "$sharerName \u00b7 Curio" else "Curio",
                style = TextStyle(fontFamily = sig.footerFont, fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold, color = sig.footerColor),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Signature per-category design data ────────────────────────────────
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
    val footerSpacer: Dp, val footerFont: FontFamily, val footerColor: Color
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


private fun signatureDesign(categoryName: String, family: CategoryFamily): SignatureDesign {
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
    val sig = topicVariant(topicName, family) ?: signatureDesign(categoryName, family)

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(sig.cornerRadius.dp)).background(sig.bg, RoundedCornerShape(sig.cornerRadius.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            sig.drawBackground(this, w, h)
        }
        Column(modifier = modifier.fillMaxSize().padding(sig.padding)) {
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
            Text(display, style = TextStyle(
                fontFamily = sig.titleFont, fontSize = sig.titleSize,
                lineHeight = sig.titleLineHeight, color = sig.titleColor
            ), maxLines = 4, overflow = TextOverflow.Ellipsis)
            val metaParts = mutableListOf<String>()
            if (byline.isNotBlank()) metaParts.add(byline)
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
    byline: String = "", year: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (quoteText != null) {
            CurioIcon(name = CurioIcons.FormatQuote, tint = palette.ink.copy(alpha = 0.20f), size = 32.dp)
            Text(quoteText, style = MaterialTheme.typography.titleLarge.copy(fontFamily = LoraFontFamily, fontSize = qSize, lineHeight = (qSize.value * 1.28f).sp), color = palette.ink, maxLines = Int.MAX_VALUE, overflow = TextOverflow.Clip)
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
                Text(factText, style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = LoraFontFamily, fontSize = qfs,
                    lineHeight = (qfs.value * 1.4f).sp
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
    // Dense grain — many small dots at varying opacity
    for (i in 0 until 120) {
        val x = ((s * (i + 1) * 7919) % 10000) / 10000f * w
        val y = ((s * (i + 1) * 6271) % 10000) / 10000f * h
        val a = 0.035f + ((s * (i + 1) * 3571) % 100) / 100f * 0.05f
        val r = 1f + ((s * (i + 1) * 4201) % 100) / 100f * 2f
        drawCircle(palette.ink.copy(alpha = a), r, Offset(x, y))
    }
    // Speckle — larger, sparser spots for paper fiber feel
    for (i in 0 until 25) {
        val x = ((s * (i + 1) * 9113) % 10000) / 10000f * w
        val y = ((s * (i + 1) * 5381) % 10000) / 10000f * h
        drawCircle(palette.ink.copy(alpha = 0.07f), 3f + ((s * (i + 1) * 7727) % 100) / 100f * 3f, Offset(x, y))
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
        val tooth = ((t * 19f).toInt() % 3 - 1) * 2.7f
        return tearY + sin(t * 8.2f + 0.8f) * 7.5f + sin(t * 31f + 1.9f) * 3.2f + tooth
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
// SHARE SHEET
// ═══════════════════════════════════════════════════════════════════════
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
                            TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = styles[page], ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption)
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
                        TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption)
                    }
                }
            }

            // Photo picker for Collage — compact row below card
            if (currentStyle == ShareCardStyle.COLLAGE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = { photoPickerLauncher.launch("image/*") }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(34.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CurioIcon(name = CurioIcons.PhotoLibrary, tint = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 13.dp)
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
                        placeholder = { Text(if (sharer.isNotBlank()) "$sharer \u00b7 via Curio" else "via Curio", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).height(34.dp)
                    )
                }
            }

            // Aspect
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(ShareCardAspect.PORTRAIT.label, CurioIcons.Image, aspect == ShareCardAspect.PORTRAIT) { aspect = ShareCardAspect.PORTRAIT }
                Pill(ShareCardAspect.CLASSIC.label, CurioIcons.Image, aspect == ShareCardAspect.CLASSIC) { aspect = ShareCardAspect.CLASSIC }
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
                        TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption)
                    }); onDismiss()
                }, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("\u2B07", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(4.dp))
                    Text("Save", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
                // Share button
                Button(onClick = {
                    shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                        TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption)
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
    style: ShareCardStyle = ShareCardStyle.PAPER, onStyleChange: (Int) -> Unit = {}
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
                    TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharerName, aspect = aspect, style = styles[page], ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (isQ) activeSource.text else null, quoteAuthor = if (isQ) topicByline.ifBlank { null } else null, byline = topicByline)
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
                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharerName, aspect = aspect, style = style, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (isQ) activeSource.text else null, quoteAuthor = if (isQ) topicByline.ifBlank { null } else null, byline = topicByline)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(ShareCardAspect.PORTRAIT.label, CurioIcons.Image, aspect == ShareCardAspect.PORTRAIT) { onAspectChange(ShareCardAspect.PORTRAIT) }
            Pill(ShareCardAspect.CLASSIC.label, CurioIcons.Image, aspect == ShareCardAspect.CLASSIC) { onAspectChange(ShareCardAspect.CLASSIC) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            sources.filter { !isQ || it.id != QUICK_FACT_ID }.forEach { opt ->
                Pill(opt.label + (opt.rating?.takeIf { r -> r > 0 }?.let { " · " + "★".repeat(it) } ?: ""), CurioIcons.FormatText, opt.id == activeSource.id) { onSelectSource(opt.id) }
            }
        }
        if (customEditing) OutlinedTextField(customText, onCustomTextChange, placeholder = { Text("Your custom fact", style = MaterialTheme.typography.bodyMedium) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
        val eh = pw * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()
        Button(onClick = {
            shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharerName, aspect = aspect, style = style, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (isQ) activeSource.text else null, quoteAuthor = if (isQ) topicByline.ifBlank { null } else null, byline = topicByline)
            }); onShared()
        }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Share image card", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
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

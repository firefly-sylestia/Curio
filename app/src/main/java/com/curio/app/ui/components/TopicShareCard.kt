package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
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

// ─── Style enum ────────────────────────────────────────────────────────
enum class ShareCardStyle(val label: String, val glyph: String) {
    PAPER("Paper", "description"),
    VINYL("Vinyl", "album"),
    COLLAGE("Collage", "collections_bookmark"),
    NEUMORPHIC("Clean", "circle")
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
fun availableStylesForFamily(family: CategoryFamily): List<ShareCardStyle> = when (family) {
    CategoryFamily.MUSIC -> listOf(ShareCardStyle.PAPER, ShareCardStyle.VINYL, ShareCardStyle.COLLAGE)
    CategoryFamily.MOVIES -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC)
    CategoryFamily.BOOKS -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC)
    CategoryFamily.VISUAL_ART -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC)
    CategoryFamily.SCIENCE -> listOf(ShareCardStyle.PAPER, ShareCardStyle.NEUMORPHIC, ShareCardStyle.COLLAGE)
    CategoryFamily.ANIME_COMICS -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC)
    CategoryFamily.GAMES -> listOf(ShareCardStyle.PAPER, ShareCardStyle.NEUMORPHIC, ShareCardStyle.COLLAGE)
    CategoryFamily.MYTHOLOGY -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC)
    CategoryFamily.SPORTS -> listOf(ShareCardStyle.PAPER, ShareCardStyle.NEUMORPHIC, ShareCardStyle.COLLAGE)
    CategoryFamily.FOOD -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC)
    CategoryFamily.INTERNET -> listOf(ShareCardStyle.PAPER, ShareCardStyle.NEUMORPHIC, ShareCardStyle.COLLAGE)
    CategoryFamily.WILDCARD -> listOf(ShareCardStyle.PAPER, ShareCardStyle.COLLAGE, ShareCardStyle.NEUMORPHIC)
}

const val QUICK_FACT_ID = "quick_fact"
const val CUSTOM_FACT_ID = "custom_fact"

private fun quoteFontSize(length: Int): TextUnit = when {
    length > 900 -> 15.sp; length > 650 -> 17.sp; length > 420 -> 19.sp
    length > 260 -> 21.sp; else -> 24.sp
}

/** Dynamic font size for quick fact text — scales down for longer facts
 *  so the text never gets cut off. Short facts get a slightly larger,
 *  more impactful size. */
private fun quickFactFontSize(length: Int): TextUnit = when {
    length > 200 -> 12.sp
    length > 140 -> 13.sp
    length > 90 -> 14.sp
    length > 50 -> 15.sp
    else -> 16.sp
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
    byline: String = ""
) {
    val display = topicName.substringBeforeLast(" (")
    // Extract year from trailing parentheses — "Appetite for Destruction (1987)" → "1987"
    val year = topicName.substringAfterLast("(").substringBeforeLast(")").takeIf { it.all { c -> c.isDigit() } && it.length == 4 }
    val palette = paletteFor(accent)
    when (style) {
        ShareCardStyle.PAPER -> PaperCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.VINYL -> VinylCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
        ShareCardStyle.COLLAGE -> CollageCard(display, topicName, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, userPhoto)
        ShareCardStyle.NEUMORPHIC -> NeumorphicCard(display, categoryName, categoryGlyph, palette, factText, sharerName, aspect, modifier, ratingStars, categoryFamily, quoteText, quoteAuthor, byline, year)
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
// Right-side partial record + accent label + text left
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun VinylCard(
    display: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null
) {
    val qSize = quoteText?.let { quoteFontSize(it.length) } ?: 0.sp
    Box(
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF8F0E0), Color(0xFFEDE0C8), Color(0xFFDFD0B0))), RoundedCornerShape(6.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) { drawPaperTexture(palette) }

        // Vinyl record — RIGHT side, partially cropped off edge
        // Moved further right + down so text never overlaps
        Canvas(
            modifier = Modifier.align(Alignment.CenterEnd)
                .size(width = 300.dp, height = 300.dp)
                .offset(x = 110.dp, y = 60.dp)
        ) {
            drawVinylPartial(palette.accent)
        }

        Watermark(family, categoryGlyph, palette.ink.copy(alpha = 0.04f), display.hashCode())

        Column(modifier = Modifier.fillMaxSize().padding(24.dp).zIndex(1f), verticalArrangement = Arrangement.SpaceBetween) {
            HeaderRow(categoryName, categoryGlyph, palette)

            // Text on left side — wider area to avoid blending with record
            Column(modifier = Modifier.weight(1f).width(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (quoteText != null) {
                    // Quote mode — clean, readable quote
                    CurioIcon(name = CurioIcons.FormatQuote, tint = palette.accent.copy(alpha = 0.35f), size = 28.dp)
                    Text(text = quoteText, style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LoraFontFamily, fontSize = qSize, lineHeight = (qSize.value * 1.30f).sp
                    ), color = palette.ink, maxLines = 6, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(text = display, style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = ChangaOneFontFamily, lineHeight = 34.sp
                    ), color = palette.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    // Metadata line
                    val vMetaParts = mutableListOf<String>()
                    if (byline.isNotBlank()) vMetaParts.add(byline)
                    if (year != null) vMetaParts.add(year)
                    if (vMetaParts.isNotEmpty()) {
                        Text(vMetaParts.joinToString(" \u2022 "), style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = LoraFontFamily, fontWeight = FontWeight.SemiBold),
                            color = palette.ink.copy(alpha = 0.50f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Accent underline
                    Spacer(Modifier.height(4.dp))
                    Canvas(Modifier.size(width = 44.dp, height = 3.dp)) {
                        drawRoundRect(palette.accent, cornerRadius = CornerRadius(2f))
                    }
                    Spacer(Modifier.height(2.dp))
                    // Quick fact — dynamic font size based on length, Lora serif for depth
                    val qfSize = quickFactFontSize(factText.length)
                    Text(text = factText, style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = LoraFontFamily, fontSize = qfSize,
                        lineHeight = (qfSize.value * 1.35f).sp
                    ), color = palette.ink.copy(alpha = 0.75f), maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
                if (ratingStars != null && ratingStars > 0) StarRow(ratingStars, palette)
            }

            Footer(sharerName, quoteText, quoteAuthor, palette)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 2 — COLLAGE (torn paper layers + polaroid + tape)
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun CollageCard(
    display: String, topicName: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    userPhoto: androidx.compose.ui.graphics.ImageBitmap? = null
) {
    val layerTop = Color(0xFFF5EDE0)
    val layerMid = Color(0xFFE5D5BC)
    val layerBot = palette.accent.copy(alpha = 0.25f)

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(layerTop, RoundedCornerShape(6.dp))) {
        // Torn paper layers
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val midY = h * 0.50f; val botY = h * 0.68f
            drawRect(layerMid, Offset(0f, midY), Size(w, h - midY))
            drawRect(layerBot, Offset(0f, botY), Size(w, h - botY))
            drawTornLine(midY, w, layerTop, layerMid)
            drawTornLine(botY, w, layerMid, layerBot)
        }

        // Polaroid frame (top-right) — original position
        BoxWithConstraints(Modifier.fillMaxSize().zIndex(2f)) {
            val cw = maxWidth.value; val ch = maxHeight.value
            val pW = cw * 0.42f; val pH = pW * 1.18f
            val pX = cw * 0.52f; val pY = ch * 0.04f

            // Tape
            Canvas(Modifier.offset((pX + pW * 0.18f).dp, (pY - 3f).dp).size((pW * 0.5f).dp, 16.dp)) {
                drawRoundRect(Color(0xFFD9BE8A).copy(alpha = 0.65f), Offset.Zero, Size(size.width, size.height), CornerRadius(2.dp.toPx()))
            }
            // Polaroid white frame
            Canvas(Modifier.offset(pX.dp, pY.dp).size(pW.dp, pH.dp)) {
                val b = size.width * 0.07f
                drawRoundRect(Color.White, Offset.Zero, Size(size.width, size.height), CornerRadius(3.dp.toPx()))
                if (userPhoto != null) {
                    // User's photo fills the inner area
                    drawImage(userPhoto,
                        dstOffset = androidx.compose.ui.unit.IntOffset(b.toInt(), b.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize((size.width - b * 2).toInt(), (size.height * 0.70f).toInt()))
                } else {
                    // Placeholder — accent tinted area
                    drawRoundRect(palette.accent.copy(alpha = 0.10f), Offset(b, b), Size(size.width - b * 2, size.height * 0.70f), CornerRadius(2.dp.toPx()))
                    // Camera icon hint
                }
            }
            // Topic name — Lora italic for elegant cursive look
            val tm = rememberTextMeasurer()
            val hs = TextStyle(fontFamily = LoraFontFamily, fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                fontSize = (pW * 0.062f).coerceIn(11f, 16f).sp, color = palette.ink,
                lineHeight = (pW * 0.075f).sp)
            val tl = tm.measure(display, hs, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Canvas(Modifier.offset((pX + pW * 0.08f).dp, (pY + pH * 0.74f).dp).size((pW * 0.84f).dp, (pH * 0.22f).dp)) {
                drawText(tl)
            }
        }

        // Left column — category chip + glyph, then quick fact BELOW the polaroid area
        Column(modifier = Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 16.dp).zIndex(1f), verticalArrangement = Arrangement.Top) {
            Surface(shape = RoundedCornerShape(14.dp), color = palette.accentDark) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurioIcon(name = categoryGlyph, tint = Color.White, size = 14.dp)
                    Text(categoryName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Category glyph stamp
                Box(Modifier.size(40.dp).drawBehind {
                    drawCircle(palette.accent.copy(alpha = 0.18f), radius = size.minDimension / 2f)
                    drawCircle(palette.accent.copy(alpha = 0.35f), radius = size.minDimension / 2f, style = Stroke(1.4.dp.toPx()))
                }, contentAlignment = Alignment.Center) {
                    CurioIcon(name = categoryGlyph, contentDescription = null, tint = palette.accent, size = 20.dp)
                }
            }
            Spacer(Modifier.height(8.dp))
            // Quick fact / quote — positioned BELOW the polaroid area
            if (quoteText != null) {
                Text(quoteText, style = MaterialTheme.typography.titleMedium.copy(fontFamily = LoraFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = quoteFontSize(quoteText.length), lineHeight = (quoteFontSize(quoteText.length).value * 1.3f).sp), color = palette.ink, maxLines = 5, overflow = TextOverflow.Ellipsis)
            } else {
                val qfSize = quickFactFontSize(factText.length)
                Text(factText, style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = LoraFontFamily, fontSize = qfSize,
                    lineHeight = (qfSize.value * 1.35f).sp
                ), color = palette.ink.copy(alpha = 0.85f), maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            if (ratingStars != null && ratingStars > 0) StarRow(ratingStars, palette)
            Spacer(Modifier.weight(1f))
            // via Curio — Lora italic for a refined look
            Text(if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = LoraFontFamily,
                    fontStyle = FontStyle.Italic),
                color = palette.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 3 — NEUMORPHIC (clean light, embossed/debossed elements)
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun NeumorphicCard(
    display: String, categoryName: String, categoryGlyph: String,
    palette: ShareCardPalette, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier, ratingStars: Int?,
    family: CategoryFamily, quoteText: String?, quoteAuthor: String?,
    byline: String = "", year: String? = null
) {
    val bg = Color(0xFFF0F0F0)
    val shadowLight = Color.White
    val shadowDark = Color(0xFFD0D0D0)
    val qSize = quoteText?.let { quoteFontSize(it.length) } ?: 0.sp

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(bg, RoundedCornerShape(6.dp))) {
        // Neumorphic circle with glyph (top-right area) — larger, more visible
        Canvas(Modifier.align(Alignment.TopEnd).offset((-45).dp, 60.dp).size(170.dp)) {
            // Outer shadow (dark)
            drawCircle(shadowDark.copy(alpha = 0.45f), radius = size.minDimension / 2f + 5f, center = Offset(size.width / 2f + 3f, size.height / 2f + 3f))
            // Inner highlight (light)
            drawCircle(shadowLight, radius = size.minDimension / 2f - 3f, center = Offset(size.width / 2f - 2f, size.height / 2f - 2f))
            // Surface
            drawCircle(bg, radius = size.minDimension / 2f - 5f)
        }
        // Glyph inside the circle — larger and more visible
        Box(Modifier.align(Alignment.TopEnd).offset((-83).dp, 89.dp).size(100.dp), contentAlignment = Alignment.Center) {
            CurioIcon(name = categoryGlyph, tint = palette.accent.copy(alpha = 0.50f), size = 52.dp)
        }

        // Small neumorphic bar icon (bottom-right) — accent colored
        Canvas(Modifier.align(Alignment.BottomEnd).offset((-24).dp, (-80).dp).size(44.dp)) {
            drawRoundRect(shadowDark.copy(alpha = 0.4f), Offset(2f, 2f), Size(size.width, size.height), CornerRadius(10.dp.toPx()))
            drawRoundRect(shadowLight, Offset.Zero, Size(size.width - 2f, size.height - 2f), CornerRadius(10.dp.toPx()))
            drawRoundRect(palette.accent.copy(alpha = 0.12f), Offset(2f, 2f), Size(size.width - 4f, size.height - 4f), CornerRadius(9.dp.toPx()))
        }

        Column(modifier = Modifier.fillMaxSize().padding(28.dp).zIndex(1f), verticalArrangement = Arrangement.SpaceBetween) {
            // Category pill — light neumorphic
            Surface(shape = RoundedCornerShape(14.dp), color = bg, shadowElevation = 3.dp) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurioIcon(name = categoryGlyph, tint = palette.accent.copy(alpha = 0.70f), size = 14.dp)
                    Text(categoryName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = palette.ink)
                }
            }

            Column(modifier = Modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (quoteText != null) {
                    CurioIcon(name = CurioIcons.FormatQuote, tint = palette.accent.copy(alpha = 0.30f), size = 26.dp)
                    Text(quoteText, style = MaterialTheme.typography.titleLarge.copy(fontFamily = ChangaOneFontFamily, fontSize = qSize, lineHeight = (qSize.value * 1.28f).sp), color = palette.ink, maxLines = 5, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(display, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = ChangaOneFontFamily, lineHeight = 34.sp), color = palette.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    // Metadata line
                    val nMetaParts = mutableListOf<String>()
                    if (byline.isNotBlank()) nMetaParts.add(byline)
                    if (year != null) nMetaParts.add(year)
                    if (nMetaParts.isNotEmpty()) {
                        Text(nMetaParts.joinToString(" \u2022 "), style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = LoraFontFamily, fontWeight = FontWeight.SemiBold),
                            color = palette.ink.copy(alpha = 0.50f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Accent underline instead of gray
                    Canvas(Modifier.size(width = 40.dp, height = 2.dp)) { drawRoundRect(palette.accent.copy(alpha = 0.5f), cornerRadius = CornerRadius(1f)) }
                    // Quick fact — dynamic font size, Lora serif for depth
                    val qfSize = quickFactFontSize(factText.length)
                    Text(factText, style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = LoraFontFamily, fontSize = qfSize,
                        lineHeight = (qfSize.value * 1.35f).sp
                    ), color = palette.ink.copy(alpha = 0.70f), maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
                if (ratingStars != null && ratingStars > 0) StarRow(ratingStars, palette)
            }

            Footer(sharerName, quoteText, quoteAuthor, palette)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════
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
                Text(factText, style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = LoraFontFamily, fontSize = quickFactFontSize(factText.length),
                    lineHeight = (quickFactFontSize(factText.length).value * 1.4f).sp
                ), color = palette.ink, maxLines = if (aspect == ShareCardAspect.PORTRAIT) 12 else 10, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FrostPane(palette: ShareCardPalette, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().drawBehind {
        val c = CornerRadius(18.dp.toPx())
        drawRoundRect(Color.Black.copy(alpha = 0.06f), Offset(0f, 3.dp.toPx()), Size(size.width, size.height), c)
        drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.15f))), Offset.Zero, Size(size.width, size.height), c)
        drawRoundRect(palette.accent.copy(alpha = 0.08f), Offset.Zero, Size(size.width, size.height), c, style = Stroke(0.8.dp.toPx()))
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
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic),
            color = palette.ink.copy(alpha = 0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis
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
        drawCircle(palette.ink.copy(alpha = 0.02f), 3f + ((s * (i + 1) * 7727) % 100) / 100f * 3f, Offset(x, y))
    }
}

/** Horizontal fiber lines for a realistic paper look. */
private fun DrawScope.drawPaperFibers(palette: ShareCardPalette) {
    val w = size.width; val h = size.height; val s = (w * 1000 + h).toInt()
    for (i in 0 until 12) {
        val y = ((s * (i + 1) * 4637) % 10000) / 10000f * h
        val x0 = ((s * (i + 1) * 2371) % 10000) / 10000f * w * 0.3f
        val x1 = x0 + ((s * (i + 1) * 6529) % 10000) / 10000f * w * 0.4f + w * 0.1f
        drawLine(palette.ink.copy(alpha = 0.02f), Offset(x0, y), Offset(x1, y), strokeWidth = 0.8f)
    }
}

private fun DrawScope.drawTornBottom(palette: ShareCardPalette) {
    val w = size.width; val h = size.height; val ty = h * 0.92f
    val tint = palette.ink.copy(alpha = 0.05f)
    // Three-layer organic tear: broad wave + mid ripple + fine jitter
    fun tearY(x: Float): Float {
        val broad = sin(x * 0.025f + 1.7f) * 8f
        val mid = sin(x * 0.06f + 3.2f) * 3.5f
        val fine = sin(x * 0.14f + 0.9f) * 1.5f
        return ty + broad + mid + fine
    }
    // Shadow under the tear edge for depth
    val shadow = Path().apply {
        moveTo(0f, ty - 2f); var x = 0f
        while (x <= w) { lineTo(x, tearY(x) + 3f); x += w / 60f }
        lineTo(w, ty + 16f); lineTo(0f, ty + 16f); close()
    }
    drawPath(shadow, Color.Black.copy(alpha = 0.04f))
    // Main torn shape — lighter, shorter
    val p = Path().apply {
        moveTo(0f, h); var x = 0f
        while (x <= w) { lineTo(x, tearY(x)); x += w / 60f }
        lineTo(w, h); close()
    }
    drawPath(p, tint)
    // Edge highlight along the tear — connects left to right
    val edge = Path().apply {
        moveTo(0f, tearY(0f) - 1f); var x = 0f
        while (x <= w) { lineTo(x, tearY(x) - 1f); x += w / 60f }
    }
    drawPath(edge, Color.White.copy(alpha = 0.35f), style = Stroke(1f))
}

private fun DrawScope.drawTornLine(y: Float, w: Float, above: Color, below: Color) {
    // Shadow
    val sp = Path().apply {
        moveTo(0f, y + 2f); var x = 0f
        while (x <= w) { lineTo(x, y + sin(x * 0.04f + 2.1f) * 3.5f + 2f); x += w / 35f }
        lineTo(w, y + 10f); lineTo(0f, y + 10f); close()
    }
    drawPath(sp, Color.Black.copy(alpha = 0.06f))
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
    drawCircle(Color.Black.copy(alpha = 0.18f), r + 5f, Offset(cx + 2f, cy + 3f))
    // Black disc
    drawCircle(Color(0xFF151515), r, Offset(cx, cy))
    // Grooves
    for (i in 8 downTo 1) drawCircle(Color.White.copy(alpha = 0.06f), r * (0.38f + i * 0.065f), Offset(cx, cy), style = Stroke(0.7f))
    // Highlight arc
    drawArc(Color.White.copy(alpha = 0.05f), -50f, 65f, false, Offset(cx - r * 0.82f, cy - r * 0.82f), Size(r * 1.64f, r * 1.64f), style = Stroke(r * 0.22f))
    // Label
    val lr = r * 0.28f
    drawCircle(Brush.radialGradient(listOf(labelColor, labelColor.copy(alpha = 0.7f)), center = Offset(cx - lr * 0.15f, cy - lr * 0.15f), radius = lr * 1.2f), lr, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.18f), lr * 0.78f, Offset(cx, cy), style = Stroke(1.2f))
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
        // Larger watermark glyphs (42dp) positioned at corners
        val half = 21
        listOf(0.14f to 0.16f, 0.86f to 0.16f, 0.14f to 0.84f, 0.86f to 0.84f).forEachIndexed { i, (x, y) ->
            CurioIcon(name = symbols[i % symbols.size], tint = tint, size = 42.dp, modifier = Modifier.offset((w * x - half).dp, (h * y - half).dp))
        }
        // Center watermark — fainter, larger
        CurioIcon(name = symbols[seed.mod(symbols.size)], tint = tint.copy(alpha = tint.alpha * 0.5f), size = 80.dp, modifier = Modifier.offset((w * 0.5f - 40).dp, (h * 0.5f - 40).dp))
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
    val available = if (isQuotes) listOf(quote) else listOf(quick) + savedSources
    val defaultId = if (isQuotes) quote.id else savedSources.firstOrNull { it.id == "quote" }?.id ?: quick.id
    val activeId = selectedId ?: defaultId
    val activeSource = when (activeId) {
        CUSTOM_FACT_ID -> custom.copy(text = customText.ifBlank { "Add your own fact about this discovery…" })
        else -> available.firstOrNull { it.id == activeId } ?: quick
    }

    val styles = availableStylesForFamily(categoryFamily)
    val safeIdx = styleIdx.coerceIn(0, styles.lastIndex)
    val currentStyle = styles[safeIdx]

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Share this topic", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)

            // Card preview
            val pw = 280.dp
            Box(Modifier.width(pw).aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat()).shadow(2.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp))) {
                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline)
            }

            // Photo picker — only for Collage style
            if (currentStyle == ShareCardStyle.COLLAGE) {
                Surface(onClick = { photoPickerLauncher.launch("image/*") }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(38.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        CurioIcon(name = CurioIcons.PhotoLibrary, tint = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 15.dp)
                        Text(
                            if (userPhoto != null) "Change photo" else "Add photo to polaroid",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Style picker — only show available styles for this family
            if (styles.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.pointerInput(safeIdx) {
                    detectHorizontalDragGestures { _, drag ->
                        if (drag > 15f && safeIdx > 0) styleIdx = safeIdx - 1
                        else if (drag < -15f && safeIdx < styles.lastIndex) styleIdx = safeIdx + 1
                    }
                }) {
                    styles.forEachIndexed { i, s ->
                        val sel = i == safeIdx
                        Surface(onClick = { styleIdx = i }, shape = RoundedCornerShape(50), color = if (sel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(36.dp)) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                CurioIcon(name = s.glyph, tint = if (sel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                                Text(s.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (sel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
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

            val eh = pw * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()
            Button(onClick = {
                shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                    TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline)
                }); onDismiss()
            }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Share image card", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
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
    Box(Modifier.width(pw).aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat()).shadow(2.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp))) {
        TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharerName, aspect = aspect, style = style, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (isQ) activeSource.text else null, quoteAuthor = if (isQ) topicByline.ifBlank { null } else null, byline = topicByline)
    }
    val styles = availableStylesForFamily(categoryFamily)
    if (styles.size > 1) {
        val si = style.ordinal.coerceIn(0, styles.lastIndex)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            styles.forEachIndexed { i, s ->
                val sel = i == si
                Surface(onClick = { onStyleChange(i) }, shape = RoundedCornerShape(50), color = if (sel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(36.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        CurioIcon(name = s.glyph, tint = if (sel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                        Text(s.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (sel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
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

@Composable
private fun Pill(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(38.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CurioIcon(name = icon, tint = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant, size = 15.dp)
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

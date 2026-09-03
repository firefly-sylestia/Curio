package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Slider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
import com.curio.app.data.CurioQuests
import com.curio.app.data.LevelRewards
import com.curio.app.ui.theme.BungeeFontFamily
import com.curio.app.ui.theme.ChangaOneFontFamily
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.GeomFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.curio.app.ui.theme.PatrickHandFontFamily
import com.curio.app.ui.theme.PirataOneFontFamily
import com.curio.app.ui.theme.PlayfairDisplayFontFamily
import com.curio.app.ui.theme.CormorantGaramondFontFamily
import com.curio.app.ui.theme.BebasNeueFontFamily
import com.curio.app.ui.theme.SpaceMonoFontFamily
import com.curio.app.ui.theme.DMSerifDisplayFontFamily
import com.curio.app.ui.theme.SoraFontFamily
import com.curio.app.ui.theme.CorbenFontFamily
import com.curio.app.ui.theme.MavenProFontFamily
import com.curio.app.ui.theme.BioRhymeFontFamily
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.OxaniumFontFamily
import com.curio.app.ui.theme.LimelightFontFamily
import com.curio.app.ui.theme.RyeFontFamily
import com.curio.app.ui.theme.SpaceGroteskFontFamily
import com.curio.app.ui.theme.AntonFontFamily
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
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
    val inkFaint: Color,
    // v323 — human name shown by the Tone tool's swatches (Auto = rotation).
    val name: String = ""
)

// Curated tones — warm, beautiful, NOT derived from category colors.
// Each category maps to one of these by index, so every share card
// looks intentional and visually rich. The first FOUR are always
// available; the premium tones unlock with LEVEL REWARDS (v9.x — see
// [LevelRewards]), so XP visibly buys new looks.
private val curatedTones = listOf(
    // Warm Rose — deep muted rose on warm cream
    ShareCardPalette(
        bgBase = Color(0xFFFDF0EE), bgLight = Color(0xFFFDF6F5), bgMid = Color(0xFFF8E0DC),
        accent = Color(0xFFB85C6E), accentDark = Color(0xFF8A3A4C),
        ink = Color(0xFF2C1A1E), inkFaint = Color(0xFF8A6B72),
        name = "Warm Rose"
    ),
    // Soft Sage — deep teal on greenish cream
    ShareCardPalette(
        bgBase = Color(0xFFF0F5F0), bgLight = Color(0xFFF7FAF7), bgMid = Color(0xFFDCE8DC),
        accent = Color(0xFF5E8A72), accentDark = Color(0xFF3D6B52),
        ink = Color(0xFF1A2420), inkFaint = Color(0xFF6B8A7C),
        name = "Soft Sage"
    ),
    // Golden Ochre — warm gold on pale parchment
    ShareCardPalette(
        bgBase = Color(0xFFFAF5EB), bgLight = Color(0xFFFDFBF5), bgMid = Color(0xFFF0E6D0),
        accent = Color(0xFFB08840), accentDark = Color(0xFF8A6520),
        ink = Color(0xFF2A2010), inkFaint = Color(0xFF8A7A60),
        name = "Golden Ochre"
    ),
    // Deep Indigo — moody purple on cool parchment
    ShareCardPalette(
        bgBase = Color(0xFFF2EFF8), bgLight = Color(0xFFF8F6FC), bgMid = Color(0xFFE2DDEF),
        accent = Color(0xFF6A5A9A), accentDark = Color(0xFF4A3A7A),
        ink = Color(0xFF1C1630), inkFaint = Color(0xFF7A6A90),
        name = "Deep Indigo"
    ),
    // Midnight (Level 2) — deep navy with a silver-moon accent
    ShareCardPalette(
        bgBase = Color(0xFF1E2433), bgLight = Color(0xFF2A3142), bgMid = Color(0xFF171C29),
        accent = Color(0xFF9FB4D8), accentDark = Color(0xFF6E86B3),
        ink = Color(0xFFEDF1F8), inkFaint = Color(0xFF9AA6BE),
        name = "Midnight"
    ),
    // Forest (Level 8) — mossy pine on soft mushroom
    ShareCardPalette(
        bgBase = Color(0xFFEDF1E8), bgLight = Color(0xFFF6F8F2), bgMid = Color(0xFFD9E2D0),
        accent = Color(0xFF4E6E4A), accentDark = Color(0xFF33512F),
        ink = Color(0xFF1B2418), inkFaint = Color(0xFF64755E),
        name = "Forest"
    ),
    // Lavender (Level 15) — lilac on cool grey-cream
    ShareCardPalette(
        bgBase = Color(0xFFF3EFF7), bgLight = Color(0xFFFAF7FC), bgMid = Color(0xFFE3DBEF),
        accent = Color(0xFF8B74B8), accentDark = Color(0xFF664F93),
        ink = Color(0xFF221B31), inkFaint = Color(0xFF7C6E96),
        name = "Lavender"
    ),
    // Ember (Level 30) — charcoal with a molten orange accent
    ShareCardPalette(
        bgBase = Color(0xFF26211D), bgLight = Color(0xFF332C26), bgMid = Color(0xFF1C1815),
        accent = Color(0xFFE0853F), accentDark = Color(0xFFB4602A),
        ink = Color(0xFFFAF1E8), inkFaint = Color(0xFFA08E7E),
        name = "Ember"
    ),
    // v323 — six more premium tones unlocked by higher levels (see
    // [LevelRewards]): Ocean (12), Rose Gold (18), Moss (25), Storm (35),
    // Pearl (45), Sunburst (50).
    // Ocean — deep teal-blue on pale foam
    ShareCardPalette(
        bgBase = Color(0xFFEAF2F4), bgLight = Color(0xFFF4FAFB), bgMid = Color(0xFFD3E6EA),
        accent = Color(0xFF2E6E8E), accentDark = Color(0xFF1D4E68),
        ink = Color(0xFF0F242E), inkFaint = Color(0xFF5F7C8A),
        name = "Ocean"
    ),
    // Rose Gold — blush copper on warm cream
    ShareCardPalette(
        bgBase = Color(0xFFFAF1ED), bgLight = Color(0xFFFDF8F5), bgMid = Color(0xFFF0DDD4),
        accent = Color(0xFFC08A7A), accentDark = Color(0xFF9C6353),
        ink = Color(0xFF2B1B16), inkFaint = Color(0xFF8A6F66),
        name = "Rose Gold"
    ),
    // Moss — earthy olive on parchment
    ShareCardPalette(
        bgBase = Color(0xFFF4F1E8), bgLight = Color(0xFFFAF8F2), bgMid = Color(0xFFE2DCC8),
        accent = Color(0xFF7A6B3E), accentDark = Color(0xFF57491F),
        ink = Color(0xFF26210F), inkFaint = Color(0xFF7C7158),
        name = "Moss"
    ),
    // Storm — slate blue-grey on light mist
    ShareCardPalette(
        bgBase = Color(0xFFEDF0F4), bgLight = Color(0xFFF6F8FA), bgMid = Color(0xFFD5DBE3),
        accent = Color(0xFF5A6B80), accentDark = Color(0xFF3D4C5E),
        ink = Color(0xFF181E26), inkFaint = Color(0xFF667180),
        name = "Storm"
    ),
    // Pearl — ivory with a cool silver-lilac accent
    ShareCardPalette(
        bgBase = Color(0xFFF6F2F0), bgLight = Color(0xFFFCFAF9), bgMid = Color(0xFFE5DEDA),
        accent = Color(0xFF9A8FA8), accentDark = Color(0xFF6F647D),
        ink = Color(0xFF241F29), inkFaint = Color(0xFF7C727F),
        name = "Pearl"
    ),
    // Sunburst — warm amber on soft cream
    ShareCardPalette(
        bgBase = Color(0xFFFCF4E2), bgLight = Color(0xFFFFFAF0), bgMid = Color(0xFFF3E2BC),
        accent = Color(0xFFD98E2B), accentDark = Color(0xFFA9641A),
        ink = Color(0xFF2A1D08), inkFaint = Color(0xFF8F7A52),
        name = "Sunburst"
    )
)

/** How many tones the player may use at [level] (base 4 + level unlocks). */
fun unlockedToneCount(level: Int): Int = 4 + LevelRewards.unlockedPaletteCount(level)

private fun paletteFor(accent: Color, toneOverride: Int? = null): ShareCardPalette {
    // v323 — an explicit pick from the Tone tool wins; otherwise cycle
    // through the player's AVAILABLE tones using the accent hash. Premium
    // tones only join the rotation once their level reward lands, so
    // unlocking one visibly changes the share-card look.
    val count = unlockedToneCount(CurioQuests.levelForXp(CurioQuests.xpState))
        .coerceIn(1, curatedTones.size)
    val idx = toneOverride?.takeIf { it in curatedTones.indices && it < count }
        ?: Math.abs(accent.hashCode()) % count
    return curatedTones[idx]
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

// v3xx — every selectable element on the card: NONE = nothing selected
// (the edit chrome is hidden until the user taps a thing).
enum class ShareCardResizeTarget { NONE, TITLE, FACT, META, BADGE }

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
/**
 * Free-position nudges (in dp) that move the title and the quick-fact box
 * on ANY share card. Dragged by hand on the preview; threaded so the
 * exported image matches. Values are dp offsets applied via Modifier.offset.
 */
data class ShareCardMove(
    val titleDx: Float = 0f,
    val titleDy: Float = 0f,
    val factDx: Float = 0f,
    val factDy: Float = 0f,
    /** Box-size fractions (1f = unchanged). Title/fact width AND height are
     *  scaled by these so the user can crop/resize the boxes on the card. */
    val titleWidthFrac: Float = 1f,
    val titleHeightFrac: Float = 1f,
    val factWidthFrac: Float = 1f,
    val factHeightFrac: Float = 1f,
    /** Title text-size multiplier (1f = unchanged). */
    val titleScale: Float = 1f,
    /** Quick-fact TEXT format — font family override (null = the style's
     *  own font) and alignment (null = the style's own alignment). */
    val factFont: FontFamily? = null,
    val factAlign: TextAlign? = null,
    // v3xx — the font/format tools apply to EVERY selectable element:
    // title, fact, meta and badge each carry their own family, alignment
    // (title/fact) and bold/italic flags (null/false = the style's own).
    val titleFont: FontFamily? = null,
    val titleAlign: TextAlign? = null,
    val titleBold: Boolean = false,
    val titleItalic: Boolean = false,
    val factBold: Boolean = false,
    val factItalic: Boolean = false,
    val metaFont: FontFamily? = null,
    val metaBold: Boolean = false,
    val metaItalic: Boolean = false,
    val badgeFont: FontFamily? = null,
    val badgeBold: Boolean = false,
    val badgeItalic: Boolean = false,
    /** Offset applied to the card's INFO rows (byline, author, year,
     *  footer) — the M handle in edit mode. Info rows are movable but
     *  never type-editable. */
    val metaDx: Float = 0f,
    val metaDy: Float = 0f,
    /** Offset applied to the card's category badge/pill — the B handle
     *  in edit mode. The badge moves freely on the card. */
    val badgeDx: Float = 0f,
    val badgeDy: Float = 0f
)

/** Modifier that shifts a card's TITLE by the move offset + box size + scale and
 *  CROPS it to the box height (no-op when default). The crop uses
 *  fillMaxHeight+clipToBounds so the preview and the exported image match. */
private fun Modifier.moveTitle(m: ShareCardMove): Modifier {
    var mod = this
    if (m.titleScale != 1f) mod = mod.graphicsLayer { scaleX = m.titleScale; scaleY = m.titleScale }
    if (m.titleDx != 0f || m.titleDy != 0f) mod = mod.offset(x = m.titleDx.dp, y = m.titleDy.dp)
    if (m.titleWidthFrac != 1f) mod = mod.fillMaxWidth(m.titleWidthFrac.coerceIn(0.2f, 1f))
    // Height is NOT applied as a fillMaxHeight clip: that re-measures the
    // layout and physically shoves the text around. Height changes grow/
    // shrink the text by scaling its maxLines instead (see `lines()`), so
    // the text truly EXTENDS instead of jumping.
    return mod
}

/** Modifier that shifts a card's QUICK-FACT/BODY by the move offset + box size.
 *  Height behaves like [moveTitle]: it scales maxLines (no fillMaxHeight clamp).
 *  v3xx — fillMaxWidth is applied UNCONDITIONALLY (frac 1f = full width) so
 *  the width crop works on every style: with two competing fillMaxWidth
 *  modifiers in a chain the inner one must win, but removing the redundant
 *  outer fill removes all doubt (Paper's frost pane previously kept its
 *  full-width fill over the crop). */
private fun Modifier.moveFact(m: ShareCardMove): Modifier {
    var mod = this
    if (m.factDx != 0f || m.factDy != 0f) mod = mod.offset(x = m.factDx.dp, y = m.factDy.dp)
    mod = mod.fillMaxWidth(m.factWidthFrac.coerceIn(0.2f, 1f))
    return mod
}

/** Shifts the card's INFO rows (byline, author, year, footer) by the meta
 *  offset — the M handle in edit mode. Info rows move but are never
 *  type-editable. */
private fun Modifier.moveMeta(m: ShareCardMove): Modifier =
    if (m.metaDx != 0f || m.metaDy != 0f) this.offset(x = m.metaDx.dp, y = m.metaDy.dp) else this

/** Shifts the card's category badge/pill by the badge offset — the B
 *  handle in edit mode. */
private fun Modifier.moveBadge(m: ShareCardMove): Modifier =
    if (m.badgeDx != 0f || m.badgeDy != 0f) this.offset(x = m.badgeDx.dp, y = m.badgeDy.dp) else this

/** v3xx — user title font/alignment/bold/italic override (null = style's own). */
private fun titleStyle(base: TextStyle, m: ShareCardMove): TextStyle {
    var s = base
    if (m.titleFont != null) s = s.copy(fontFamily = m.titleFont)
    if (m.titleAlign != null) s = s.copy(textAlign = m.titleAlign)
    if (m.titleBold) s = s.copy(fontWeight = FontWeight.Bold)
    if (m.titleItalic) s = s.copy(fontStyle = FontStyle.Italic)
    return s
}

/** v3xx — user info-row (author/byline/year) font/bold/italic override. */
private fun metaStyle(base: TextStyle, m: ShareCardMove): TextStyle {
    var s = base
    if (m.metaFont != null) s = s.copy(fontFamily = m.metaFont)
    if (m.metaBold) s = s.copy(fontWeight = FontWeight.Bold)
    if (m.metaItalic) s = s.copy(fontStyle = FontStyle.Italic)
    return s
}

/** v3xx — user category-badge font/bold/italic override. */
private fun badgeStyle(base: TextStyle, m: ShareCardMove): TextStyle {
    var s = base
    if (m.badgeFont != null) s = s.copy(fontFamily = m.badgeFont)
    if (m.badgeBold) s = s.copy(fontWeight = FontWeight.Bold)
    if (m.badgeItalic) s = s.copy(fontStyle = FontStyle.Italic)
    return s
}

/** v316b — editor chrome: ONE uniform move grip + a darker coffee outline
 *  replace the old per-box letter handles (T/F/M/B) and their tinted
 *  borders. Coffee reads on the cream/white card surfaces. */
private val CoffeeChrome = Color(0xFF6D4C41)      // steady brown — unselected boxes
private val CoffeeChromeDeep = Color(0xFF3E2723)  // darker coffee — selected box / handle

/** Follows ONLY the title drag (dx/dy): an info row (author / year) sitting
 *  right under the title travels WITH it when the T handle drags — the M
 *  handle still nudges the row on its own afterwards. */
private fun Modifier.titleShift(m: ShareCardMove): Modifier =
    if (m.titleDx != 0f || m.titleDy != 0f) this.offset(x = m.titleDx.dp, y = m.titleDy.dp) else this

/** Follows ONLY the fact drag (dx/dy): the thin rules some styles draw ABOVE
 *  the quick fact slide along with the box instead of floating where the
 *  fact used to be. */
private fun Modifier.factShift(m: ShareCardMove): Modifier =
    if (m.factDx != 0f || m.factDy != 0f) this.offset(x = m.factDx.dp, y = m.factDy.dp) else this

/** Resolves the fact-text style with the user's format (font + align +
 *  bold/italic). The format is stored on [ShareCardMove] so every style AND
 *  the exported image honor it automatically. */
private fun factBodyStyle(base: TextStyle, m: ShareCardMove): TextStyle {
    var s = base
    if (m.factFont != null) s = s.copy(fontFamily = m.factFont)
    if (m.factAlign != null) s = s.copy(textAlign = m.factAlign)
    if (m.factBold) s = s.copy(fontWeight = FontWeight.Bold)
    if (m.factItalic) s = s.copy(fontStyle = FontStyle.Italic)
    return s
}

/**
 * Bounds callbacks every card style uses to REPORT where its title /
 * quick-fact / info rows actually render (window coordinates, px). The
 * inline-edit overlay reads these so its indicator boxes, handles and edge
 * tabs sit EXACTLY on the real text of every style — no fixed fractions.
 */
class EditBoundsCallbacks(
    val onTitle: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    val onFact: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    val onMeta: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    val onBadge: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    // v316b — the ACTUAL TextStyle the card used for its quick fact, so the
    // invisible typing field above it lays out with identical metrics
    // (family, size, line height) and the caret never drifts off the text.
    val onFactStyle: (TextStyle) -> Unit = {}
)

/** v329 — the Reading-progress share content: how many chapters are read of
 *  the book's total. When set, every card style draws a compact PROGRESS
 *  WIDGET in place of the quick-fact text (book icon + "N of M chapters" +
 *  a filled bar) instead of prose, and the inline editor treats the fact as
 *  a plain movable box (no text typing). */
data class ChapterProgressUi(val read: Int, val total: Int)

/**
 * v329 — the visual chapter-progress element every card style renders in its
 * quick-fact slot when the Reading-progress content is active. Callers pass
 * the three inks that read on THEIR surface (Paper's cream, Vinyl's dusty
 * rose box, Collage's sage field, the dark Clean/Neumorphic plate…), so the
 * widget keeps real contrast on every design. A small book glyph + "3 of 12
 * chapters" caption sits over a rounded track whose fill width = read/total.
 */
@Composable
private fun ChapterProgressBlock(
    progress: ChapterProgressUi,
    fill: Color,
    track: Color,
    ink: Color,
    modifier: Modifier = Modifier
) {
    val frac = if (progress.total > 0) (progress.read.toFloat() / progress.total).coerceIn(0f, 1f) else 0f
    val caption = when {
        progress.total <= 0 -> ""
        progress.read >= progress.total -> "Finished · all ${progress.total} chapters"
        progress.read <= 0 -> "${progress.total} chapters ahead of you"
        else -> "${progress.read} of ${progress.total} chapters"
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = CurioIcons.MenuBook,
                contentDescription = null,
                tint = fill,
                size = 12.dp
            )
            Text(
                caption,
                style = TextStyle(
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.2.sp,
                    color = ink
                ),
                maxLines = 1
            )
        }
        // Rounded track; the filled segment's width is the read fraction.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(track)
        ) {
            if (frac > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(5.dp)
                        .background(fill, RoundedCornerShape(50))
                )
            }
        }
    }
}

/** Scales maxLines with the user's height-crop frac so the text box truly
 *  grows (more lines show) or shrinks (fewer lines) instead of jumping.
 *  v229c — ROUNDS to the nearest line instead of flooring: maxLines is an
 *  integer count, so flooring a partial drag (e.g. 2.5 lines -> 2) made the
 *  height tab feel dead until the drag crossed a full extra line. Rounding
 *  makes each half-line of travel change the visible count. */
private fun lines(base: Int, frac: Float, max: Int = 28): Int =
    // v318b — CEIL (was round): rounding meant the height slider felt dead
    // over long stretches (a 12-line fact needed +8% before the line count
    // flipped). Now ANY slider movement changes the line count, so the box
    // visibly grows/shrinks with every tick.
    kotlin.math.ceil(base * frac).toInt().coerceIn(1, max)

/** v3xx — the font tool's catalog (13 families; null = the style's own). */
private data class ShareFont(val label: String, val family: FontFamily?)
private val shareFonts = listOf(
    ShareFont("Serif", null),
    ShareFont("Sans", SoraFontFamily),
    ShareFont("Mono", SpaceMonoFontFamily),
    ShareFont("Elegant", PlayfairDisplayFontFamily),
    ShareFont("Classic", DMSerifDisplayFontFamily),
    ShareFont("Old Style", CormorantGaramondFontFamily),
    ShareFont("Bookish", FrauncesFontFamily),
    ShareFont("Rounded", CorbenFontFamily),
    ShareFont("Handwritten", PatrickHandFontFamily),
    ShareFont("Condensed", BebasNeueFontFamily),
    ShareFont("Modern", MavenProFontFamily),
    ShareFont("Grotesk", SpaceGroteskFontFamily),
    ShareFont("Bouncy", BungeeFontFamily)
)

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
    bodyScale: Float = 1f,
    editedTitle: String? = null,
    editedFact: String? = null,
    move: ShareCardMove = ShareCardMove(),
    // v323 — explicit share-card tone pick (index into [curatedTones]); null
    // keeps the automatic per-category rotation. Only unlocked tones are
    // offered by the editor's Tone tool.
    toneIndex: Int? = null,
    // v324 — the Adjust tool: whole-card saturation/contrast (1f = neutral).
    // Applied via a single graphicsLayer color filter so EVERY style and the
    // exported image get the same treatment.
    saturation: Float = 1f,
    contrast: Float = 1f,
    // v329 — when set (Reading-progress content), styles render a visual
    // progress widget instead of [factText] prose.
    chapterProgress: ChapterProgressUi? = null,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks()
) {
    // v324 — thread the adjust filter into the card's own modifier chain so
    // the preview AND the exported image both render the adjusted look.
    val cardModifier = if (saturation == 1f && contrast == 1f) modifier
    else modifier.graphicsLayer {
        colorFilter = ColorFilter.colorMatrix(adjustColorMatrix(saturation, contrast))
    }
    val display = topicName.substringBeforeLast(" (")
    // Extract year from trailing parentheses — "Appetite for Destruction (1987)" → "1987"
    val year = topicName.substringAfterLast("(").substringBeforeLast(")").takeIf { it.all { c -> c.isDigit() } && it.length == 4 }
    val palette = paletteFor(accent, toneIndex)
    // Resolve per-share text edits once — every style renders the same values,
    // and quote cards show the edited quote as both the body and the quote.
    val shownDisplay = editedTitle ?: display
    val shownFact = editedFact ?: factText
    val shownQuote = quoteText?.let { shownFact }
    when (style) {
        ShareCardStyle.PAPER -> PaperCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, cardModifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, bodyScale, callbacks, move, chapterProgress)
        ShareCardStyle.VINYL -> VinylCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, cardModifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, bodyScale, callbacks, move, chapterProgress)
        ShareCardStyle.COLLAGE -> CollageCard(shownDisplay, topicName, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, cardModifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, userPhoto, byline, year, polaroidCaption, onPhotoTap, bodyScale, callbacks, move, chapterProgress)
        ShareCardStyle.NEUMORPHIC -> NeumorphicCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, cardModifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, bodyScale, callbacks, move, chapterProgress)
        ShareCardStyle.EDITORIAL -> EditorialCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, cardModifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, bodyScale, callbacks, move, chapterProgress)
        ShareCardStyle.MINIMAL -> MinimalCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, cardModifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, bodyScale, callbacks, move, chapterProgress)
        ShareCardStyle.SIGNATURE -> SignatureCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, cardModifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, classicSignature, bodyScale, callbacks, move, chapterProgress)
        ShareCardStyle.CUSTOM -> CustomCard(shownDisplay, topicName, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, cardModifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, bodyScale, callbacks, move, chapterProgress)
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
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
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

        Column(modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            HeaderRow(categoryName, categoryGlyph, palette, move, callbacks)
            MiddleContent(display, factText, aspect, palette, ratingStars, quoteText, qSize, quoteAuthor, byline, year, bodyScale, callbacks, move, chapterProgress)
            Footer(sharerName, quoteText, quoteAuthor, palette, move, callbacks)
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
    byline: String = "", year: String? = null,
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
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
            // Category pill + lightbulb icon. v3xx — ONLY the pill is the
            // movable badge (the decorative bulb stays anchored top-right);
            // the pill reports its own bounds so the B grip tracks the chip.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = roseDusty,
                    modifier = Modifier.moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        CurioIcon(name = categoryGlyph, tint = Color.White, size = 12.dp)
                        Text(categoryName, style = badgeStyle(TextStyle(fontFamily = LoraFontFamily, fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp), move), color = Color.White)
                    }
                }
                CurioIcon(name = CurioIcons.Lightbulb, tint = roseDusty.copy(alpha = 0.40f), size = 18.dp)
            }

            Spacer(Modifier.height(12.dp))

            // Title — strong serif
            Text(title, style = titleStyle(TextStyle(
                fontFamily = ChangaOneFontFamily, fontSize = 28.sp,
                lineHeight = 32.sp, fontWeight = FontWeight.Normal, color = inkDark
            ), move), maxLines = lines(2, move.titleHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveTitle(move).onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })

            // Artist / byline — info row: movable via M handle, not editable
            if (quoteText == null && byline.isNotBlank()) {
                Text(byline, style = metaStyle(TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 13.sp, color = roseDusty
                ), move), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
            } else if (year != null) {
                Text(year, style = metaStyle(TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 13.sp, color = roseDusty
                ), move), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
            }

            // Accent underline — v316b: belongs to the quick-fact block below,
            // so it slides WITH the fact box when the F handle drags.
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.size(width = 32.dp, height = 2.dp).factShift(move)) {
                drawRoundRect(roseDusty, cornerRadius = CornerRadius(1f))
            }

            Spacer(Modifier.height(10.dp))

            // Body text — Lora serif, with semi-transparent cream background for readability over vinyl
            val bodySize = when {
                body.length > 280 -> 9.sp; body.length > 180 -> 10.sp; else -> 10.5.sp
            }
            // v316b — the EXACT style the fact renders with is reported back to
            // the inline editor so its typing field matches the caret.
            val factStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize.value * bodyScale).sp,
                lineHeight = (bodySize.value * 1.50f * bodyScale).sp, color = inkDark.copy(alpha = 0.88f),
                fontStyle = if (quoteText != null) FontStyle.Italic else FontStyle.Normal
            ), move)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFFDF0EE).copy(alpha = 0.85f),
                modifier = Modifier.widthIn(max = 220.dp).moveFact(move)
            ) {
                // v316b — the invisible typing field anchors to the GLYPH box,
                // not the padded cream surface: the box's own 6/4dp padding
                // sits outside this Text, so boundsInWindow lands exactly on
                // the letters and the caret never sits inset from them.
                // v329 — Reading-progress content draws the chapter widget
                // here instead of the prose.
                Box(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    if (chapterProgress != null) {
                        ChapterProgressBlock(
                            progress = chapterProgress,
                            fill = roseDusty,
                            track = inkDark.copy(alpha = 0.14f),
                            ink = inkDark.copy(alpha = 0.88f),
                            modifier = Modifier.onGloballyPositioned {
                                callbacks.onFact(it.boundsInWindow())
                                callbacks.onFactStyle(factStyle)
                            }
                        )
                    } else {
                        Text(
                            body, style = factStyle,
                            maxLines = lines(10, move.factHeightFrac),
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.onGloballyPositioned {
                                callbacks.onFact(it.boundsInWindow())
                                callbacks.onFactStyle(factStyle)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Spacer(Modifier.weight(1f))

            // Footer — centered, subtle (FIXED: only the author/year row moves)
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
    onPhotoTap: (() -> Unit)? = null,
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
) {
    // v327 — the Collage card wears the picked TONE (the palette used to be
    // ignored here, so the Tone tool had no effect on this style): paper =
    // bgBase, lower field = accent, dark band = accentDark, ink/text =
    // palette ink, torn seam edge = bgMid. White polaroid + photo stay.
    val topCream = palette.bgBase
    val bottomSage = palette.accent
    val bottomDark = palette.accentDark
    val inkDark = palette.ink
    val tornEdge = palette.bgMid
    val sagePill = palette.accentDark

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(topCream, RoundedCornerShape(6.dp))) {
        // ── Layered paper + botanical lower field with a natural torn seam ──
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val tearY = h * 0.42f
            drawRect(bottomSage, Offset.Zero, Size(w, h))
            // Bottom-dark gradient band — wavy top edge (not a straight line)
            // so it blends naturally with the torn seam above.
            val darkPath = Path().apply {
                val bandY = h * 0.70f
                moveTo(0f, bandY)
                var x = 0f
                while (x <= w) {
                    val y = bandY + kotlin.math.sin(x * 0.03f + 1.5f) * 6f + kotlin.math.sin(x * 0.08f) * 3f
                    lineTo(x, y); x += w / 40f
                }
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(darkPath, Brush.verticalGradient(listOf(bottomSage, bottomDark), startY = h * 0.70f, endY = h))
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
            Column(modifier = Modifier.offset(22.dp, (ch * 0.05f).dp).width((cw * 0.60f).dp).moveTitle(move).onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) }) {
                Text(if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display, style = titleStyle(TextStyle(
                    fontFamily = BungeeFontFamily, fontSize = (cw * 0.072f).coerceIn(19f, 30f).sp,
                    lineHeight = (cw * 0.082f).coerceIn(22f, 34f).sp,
                    fontWeight = FontWeight.Normal, color = inkDark
                ), move), maxLines = lines(5, move.titleHeightFrac), overflow = TextOverflow.Ellipsis)

                Spacer(Modifier.height(6.dp))

                // Metadata — small caps, letter-spaced (info row: movable via M handle)
                val metaParts = mutableListOf<String>()
                if (quoteText == null && byline.isNotBlank()) metaParts.add(byline.uppercase())
                if (year != null) metaParts.add(year)
                if (metaParts.isNotEmpty()) {
                    Text(metaParts.joinToString(" \u2022 "), style = metaStyle(TextStyle(
                        fontFamily = LoraFontFamily, fontSize = 9.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold,
                        color = inkDark.copy(alpha = 0.55f)
                    ), move), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
                }
            }
        }

        // ── MIDDLE SECTION: category pill + body text + decorative bottom ──
        Column(modifier = Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = if (aspect == ShareCardAspect.PORTRAIT) 214.dp else 190.dp, bottom = 18.dp).zIndex(1f)) {
            // Category pill — reports real bounds so the B grip rides it.
            Surface(shape = RoundedCornerShape(14.dp), color = sagePill, modifier = Modifier.moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurioIcon(name = categoryGlyph, tint = Color.White, size = 14.dp)
                    Text(categoryName, style = badgeStyle(TextStyle(
                        fontFamily = LoraFontFamily, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp), move), color = Color.White)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Body text — serif, generous line height
            val body = quoteText ?: factText
            val bodySize = when {
                body.length > 280 -> 10.sp; body.length > 180 -> 11.sp; else -> 12.sp
            }
            val factStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize.value * bodyScale).sp,
                lineHeight = (bodySize.value * 1.55f * bodyScale).sp, color = Color.White.copy(alpha = 0.92f),
                fontStyle = if (quoteText != null) FontStyle.Italic else FontStyle.Normal
            ), move)
            // v329 — Reading-progress content draws the chapter widget (white
            // on the sage/dark field) instead of the prose.
            if (chapterProgress != null) {
                ChapterProgressBlock(
                    progress = chapterProgress,
                    fill = Color.White,
                    track = Color.White.copy(alpha = 0.22f),
                    ink = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    }
                )
            } else {
                Text(body, style = factStyle, maxLines = lines(18, move.factHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveFact(move).onGloballyPositioned {
                    callbacks.onFact(it.boundsInWindow())
                    callbacks.onFactStyle(factStyle)
                })
            }

            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(6.dp))
                StarRow(ratingStars, palette)
            }

            Spacer(Modifier.weight(1f))

            // ── Footer area ──
            Spacer(Modifier.height(8.dp))

            // Footer credit — FIXED: does NOT move (only the author/year row moves)
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
    byline: String = "", year: String? = null,
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
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



        // Oversized category glyph — signature element of Clean card.
        // Offset left of the edge so the full glyph stays visible.
        CurioIcon(
            name = categoryGlyph,
            tint = Color.White.copy(alpha = 0.18f),
            size = 150.dp,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = 18.dp)
                .graphicsLayer { rotationZ = -10f }
        )

        Box(Modifier.fillMaxSize().padding(22.dp).zIndex(1f)) {
            Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.72f), modifier = Modifier.align(Alignment.TopStart).moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }) {
                Text(categoryName.uppercase(), style = badgeStyle(TextStyle(fontFamily = GeomFontFamily, fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp), move), color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }

            Column(Modifier.align(Alignment.CenterStart).padding(start = 4.dp, end = 4.dp, top = 0.dp), horizontalAlignment = Alignment.Start) {
                Text(title, style = titleStyle(TextStyle(
                    fontFamily = ChangaOneFontFamily, fontSize = 31.sp, lineHeight = 33.sp,
                    fontWeight = FontWeight.Normal, color = Color.White
                ), move), maxLines = lines(5, move.titleHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.90f).moveTitle(move).onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
                val metaParts = mutableListOf<String>()
                if (byline.isNotBlank()) metaParts.add(byline.uppercase())
                if (year != null) metaParts.add(year)
                if (metaParts.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(metaParts.joinToString("  /  "), style = metaStyle(TextStyle(
                        fontFamily = GeomFontFamily, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.3.sp, color = Color.White.copy(alpha = 0.56f)
                    ), move), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
                }
            }

            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 16.dp)) {
                val bodySize = when { body.length > 350 -> 8.sp; body.length > 260 -> 9.sp; body.length > 180 -> 9.5.sp; else -> 10.5.sp }
                val factStyle = factBodyStyle(TextStyle(
                    fontFamily = LoraFontFamily,
                    fontStyle = if (quoteText != null) FontStyle.Italic else FontStyle.Normal,
                    fontSize = (bodySize.value * bodyScale).sp, lineHeight = (bodySize.value * 1.40f * bodyScale).sp,
                    color = Color.White.copy(alpha = 0.88f),
                    shadow = Shadow(Color.Black.copy(alpha = 0.62f), Offset(0f, 2f), 5f)
                ), move)
                // v329 — Reading-progress content draws the chapter widget
                // (white on the dark plate) instead of the prose.
                if (chapterProgress != null) {
                    ChapterProgressBlock(
                        progress = chapterProgress,
                        fill = Color.White,
                        track = Color.White.copy(alpha = 0.22f),
                        ink = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.moveFact(move).onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                            callbacks.onFactStyle(factStyle)
                        }
                    )
                } else {
                    Text(body, style = factStyle, maxLines = lines(if (aspect == ShareCardAspect.PORTRAIT) 8 else 6, move.factHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    })
                }
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
    byline: String = "", year: String? = null,
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }) {
                Box(Modifier.width(7.dp).height(15.dp).background(accentRule))
                Text(categoryName.uppercase(), style = badgeStyle(TextStyle(
                    fontFamily = BungeeFontFamily, fontSize = 13.sp,
                    letterSpacing = 2.4.sp, color = inkDark
                ), move), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(7.dp))
            // Double masthead rule — thick flag + hairline
            Canvas(Modifier.fillMaxWidth().height(3.dp)) { drawRect(inkDark.copy(alpha = 0.85f)) }
            Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawRect(inkDark.copy(alpha = 0.28f)) }

            Spacer(Modifier.height(16.dp))

            // Headline — retro Bungee, big
            Text(title, style = titleStyle(TextStyle(
                fontFamily = BungeeFontFamily, fontSize = 30.sp,
                lineHeight = 34.sp, color = inkDark
            ), move), maxLines = lines(4, move.titleHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveTitle(move).onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })

            // Byline — year deck (info row — movable via the M handle, not editable)
            val metaParts = mutableListOf<String>()
            if (quoteText == null && byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text(metaParts.joinToString(" \u2014 "), style = metaStyle(TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 12.sp, color = inkDark.copy(alpha = 0.55f)
                ), move), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
            }

            Spacer(Modifier.height(15.dp))
            // Hairline under the deck — v316b: this rule belongs to the quick
            // fact block, so it slides WITH the fact box when the F handle
            // drags instead of floating where the fact used to be.
            Canvas(Modifier.fillMaxWidth().height(1.dp).factShift(move)) {
                drawLine(inkDark.copy(alpha = 0.16f), Offset.Zero, Offset(size.width, 0f))
            }
            Spacer(Modifier.height(12.dp))

            // Body — clean serif with a standing Lora drop initial. The initial
            // spans 2 lines and the text WRAPS around it (measured split): the
            // first 2 lines run beside the big letter (top-aligned with it), the
            // rest continues full-width below. Honor the user's fact format
            // (font + align).
            val bodySize = when {
                body.length > 350 -> 8.5.sp; body.length > 260 -> 9.5.sp
                body.length > 180 -> 10.sp; else -> 11.sp
            }
            val bodyStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize.value * bodyScale).sp,
                lineHeight = (bodySize.value * 1.45f * bodyScale).sp, color = inkDark.copy(alpha = 0.82f),
                fontWeight = FontWeight.Medium
            ), move)
            val initial = body.take(1)
            val bodyRest = if (body.length > 1) body.drop(1) else ""
            // v329 — Reading-progress content draws the chapter widget
            // (accent bar on the cream page) instead of the drop-cap prose.
            if (chapterProgress != null) {
                ChapterProgressBlock(
                    progress = chapterProgress,
                    fill = accentRule,
                    track = inkDark.copy(alpha = 0.13f),
                    ink = inkDark.copy(alpha = 0.82f),
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(bodyStyle)
                    }
                )
            } else if (bodyRest.isEmpty()) {
                Text(body, style = bodyStyle,
                    maxLines = lines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, move.factHeightFrac), overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(bodyStyle)
                    })
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth().moveFact(move).onGloballyPositioned {
                    callbacks.onFact(it.boundsInWindow())
                    callbacks.onFactStyle(bodyStyle)
                }) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val measurer = rememberTextMeasurer()
                    val contentW = with(density) { maxWidth.toPx() }
                    // The drop-cap initial spans 2 body lines. Its lineHeight
                    // is set to 2× one body line so the initial column height
                    // EXACTLY matches the 2 wrap lines beside it — the old
                    // baseline alignment left a height mismatch that made the
                    // full-width rest text overlap the wrapped block.
                    // LineHeightStyle.Alignment.Top anchors the letter to the TOP
                    // of its 2-line box so the initial starts at the same
                    // position as the first text line (a 2× line height alone
                    // would vertically center the glyph in the box).
                    val bodyLineH = (bodySize.value * 1.45f * bodyScale).sp
                    val initialStyle = TextStyle(
                        fontFamily = LoraFontFamily, fontWeight = FontWeight.Bold,
                        fontSize = (bodySize.value * 2.0f * bodyScale).sp,
                        lineHeight = (bodyLineH.value * 2f).sp, color = accentRule,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Top,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                    val initialW = measurer.measure(
                        text = AnnotatedString(initial),
                        style = initialStyle
                    ).size.width.toFloat()
                    val gap = with(density) { 6.dp.toPx() }
                    val narrowW = (contentW - initialW - gap).toInt().coerceAtLeast(1)
                    // How much body text fits in the first 2 lines beside the
                    // initial. Measure at the NARROW width so the rendered wrap
                    // matches the split exactly (no overflow / overlap).
                    val wrap = measurer.measure(
                        text = AnnotatedString(bodyRest),
                        style = bodyStyle,
                        overflow = TextOverflow.Clip,
                        softWrap = true,
                        maxLines = 2,
                        constraints = Constraints(maxWidth = narrowW)
                    )
                    val wrapEnd = wrap.getLineEnd((wrap.lineCount - 1).coerceAtLeast(0))
                    val wrapText = bodyRest.take(wrapEnd)
                    val restText = bodyRest.drop(wrapEnd)
                    // Initial column: fixed width = initialW + gap, top-aligned
                    // so the big letter starts at the top of the block and its
                    // 2-line height matches the wrap column. Wrap column: the
                    // narrow width, 2 lines, clipped (never overlapping the
                    // initial because it's a sibling column, not baseline-
                    // aligned behind the letter).
                    //
                    // The wrap row and the full-width rest text live inside a
                    // COLUMN: BoxWithConstraints stacks its children at the
                    // same top-left slot, so a bare Row + Text pair rendered
                    // the rest text ON TOP of the wrapped block (the "quick
                    // fact text overlapping itself" bug). A Column lays them
                    // out top-to-bottom instead.
                    val initColW = with(density) { (initialW + gap).toDp() }
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth()) {
                            if (initial.isNotEmpty()) {
                                Text(initial, style = initialStyle,
                                    modifier = Modifier
                                        .width(initColW)
                                        .padding(end = 6.dp))
                            }
                            if (wrapText.isNotEmpty()) {
                                Text(wrapText, style = bodyStyle, maxLines = 2,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                        if (restText.isNotEmpty()) {
                            Spacer(Modifier.height(3.dp))
                            Text(restText, style = bodyStyle,
                                maxLines = lines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, move.factHeightFrac),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(7.dp))
                StarRow(ratingStars, palette)
            }

            Spacer(Modifier.weight(1f))

            // Colophon — thin rule + italic credit with an accent slug (FIXED:
            // only the author/year row moves)
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
    byline: String = "", year: String? = null,
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }) {
                Box(Modifier.size(8.dp).graphicsLayer { rotationZ = 45f }.background(accent))
                Text(categoryName.uppercase(), style = badgeStyle(TextStyle(
                    fontFamily = BungeeFontFamily, fontSize = 10.sp, letterSpacing = 3.sp,
                    color = inkDark.copy(alpha = 0.55f)
                ), move), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(26.dp))

            // Title — big retro Bungee
            Text(title, style = titleStyle(TextStyle(
                fontFamily = BungeeFontFamily, fontSize = 32.sp, lineHeight = 35.sp, color = inkDark
            ), move), maxLines = lines(5, move.titleHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveTitle(move).onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })

            // Byline / year — info row: movable via M handle, not editable
            if (byline.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(byline, style = metaStyle(TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = 12.sp, color = inkDark.copy(alpha = 0.50f)
                ), move), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
            } else if (year != null) {
                Spacer(Modifier.height(6.dp))
                Text(year, style = metaStyle(TextStyle(fontFamily = LoraFontFamily, fontSize = 12.sp, color = inkDark.copy(alpha = 0.40f)), move), modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
            }

            Spacer(Modifier.weight(1f))

            // Thick accent rule above the body block — v316b: rides the fact
            // box when the F handle drags (belongs to the quick-fact block).
            Box(Modifier.width(56.dp).height(4.dp).background(accent).factShift(move))
            Spacer(Modifier.height(16.dp))

            // Body — serif, bottom-anchored
            val bodySize = when { body.length > 350 -> 8.5.sp; body.length > 260 -> 9.5.sp; body.length > 180 -> 10.5.sp; else -> 11.5.sp }
            val factStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize.value * bodyScale).sp,
                lineHeight = (bodySize.value * 1.50f * bodyScale).sp, color = inkDark.copy(alpha = 0.78f)
            ), move)
            // v329 — Reading-progress content draws the chapter widget
            // (accent bar on the white page) instead of the prose.
            if (chapterProgress != null) {
                ChapterProgressBlock(
                    progress = chapterProgress,
                    fill = accent,
                    track = inkDark.copy(alpha = 0.12f),
                    ink = inkDark.copy(alpha = 0.78f),
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    }
                )
            } else {
                Text(body, style = factStyle, maxLines = lines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, move.factHeightFrac), overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    })
            }

            if (ratingStars != null && ratingStars > 0) {
                Spacer(Modifier.height(8.dp))
                StarRow(ratingStars, palette)
            }
            Spacer(Modifier.height(14.dp))

            // Credit — tiny, right-aligned for a deliberate off-balance
            // (FIXED: only the author/year row moves)
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
    classicSignature: Boolean = false,
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
) {
    val body = quoteText ?: factText
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display
    // Design pick: Classic (f6dd7f19 family designs) > current. v324 — the
    // Deepen experiment is GONE (user direction): only default + classic
    // remain, and a saturation/contrast Adjust tool replaces the heavy scenes.
    val sig = when {
        classicSignature -> signatureDesignClassic(categoryName, family)
        else -> signatureDesign(categoryName, family)
    }

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(sig.cornerRadius.dp)).background(sig.bg, RoundedCornerShape(sig.cornerRadius.dp))) {
        // Background pattern/texture
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            sig.drawBackground(this, w, h)
        }
        // Language category — faint multilingual words layered over the
        // background for a premium polyglot feel (drawText + TextMeasurer).
        if (categoryName.uppercase().trim() == "LANGUAGE") {
            val langMeasurer = rememberTextMeasurer()
            Canvas(Modifier.fillMaxSize()) {
                val words = listOf(
                    "言語" to Offset(0.10f, 0.58f),
                    "Sprache" to Offset(0.62f, 0.30f),
                    "langue" to Offset(0.16f, 0.84f),
                    "idioma" to Offset(0.70f, 0.86f),
                    "lingua" to Offset(0.80f, 0.50f),
                    "語" to Offset(0.40f, 0.20f),
                    "زبان" to Offset(0.30f, 0.74f),
                    "भाषा" to Offset(0.56f, 0.62f)
                )
                words.forEachIndexed { i, (word, pos) ->
                    val tint = if (i % 2 == 0) Color(0xFFC9B8E0).copy(alpha = 0.12f) else Color(0xFF8B7AB0).copy(alpha = 0.14f)
                    drawText(
                        langMeasurer,
                        word,
                        style = TextStyle(fontFamily = LoraFontFamily, fontSize = (11f + i % 3 * 2f).sp, color = tint),
                        topLeft = Offset(size.width * pos.x, size.height * pos.y)
                    )
                }
            }
        }

        // Minimal-style giant faint glyph watermark (a Material Symbols icon
        // or letter) for the redesigned signature categories — one unique
        // symbol per category instead of a drawn scene, bottom-right like the
        // Minimal card's giant faint initial.
        sig.watermark?.let { glyph ->
            CurioIcon(
                name = glyph,
                tint = sig.titleColor.copy(alpha = 0.12f),
                size = 120.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer { rotationZ = -6f }
                    .offset(y = 20.dp)
                    .padding(end = 8.dp)
            )
        }

        // Small real-icon crest top-right (replaces hand-drawn crests) —
        // an actual Material Symbols glyph, tilted like a foil stamp.
        sig.crest?.let { glyph ->
            CurioIcon(
                name = glyph,
                tint = sig.crestTint.takeIf { it != Color.Unspecified }?.copy(alpha = 0.85f) ?: sig.titleColor.copy(alpha = 0.30f),
                size = 26.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer { rotationZ = 8f }
                    .padding(top = 14.dp, end = 14.dp)
            )
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
                modifier = Modifier.moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }) {
                Row(Modifier.padding(horizontal = sig.badgeHPadding, vertical = sig.badgeVPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CurioIcon(name = categoryGlyph, tint = sig.badgeInk, size = sig.badgeIconSize)
                    Text(categoryName.uppercase(), style = badgeStyle(TextStyle(
                        fontFamily = GeomFontFamily, fontSize = sig.badgeFontSize,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = sig.badgeLetterSpacing,
                        color = sig.badgeInk
                    ), move), maxLines = 1)
                }
            }
        }

        // ── Title (shared) ─────────────────────────────────────
        @Composable
        fun TitleText(centered: Boolean = false) {
            Text(title, style = titleStyle(TextStyle(
                fontFamily = sig.titleFont, fontSize = sig.titleSize,
                lineHeight = sig.titleLineHeight, color = sig.titleColor
            ), move), maxLines = lines(4, move.titleHeightFrac), overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = (if (centered) Modifier.fillMaxWidth() else Modifier).moveTitle(move).onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
        }

        // ── Meta (shared) — info row: movable via M handle, not editable ──
        @Composable
        fun MetaText(centered: Boolean = false) {
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(sig.metaSpacer))
                Text(metaParts.joinToString(sig.metaSeparator), style = metaStyle(TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = sig.metaSize, color = sig.metaColor
                ), move), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    modifier = (if (centered) Modifier.fillMaxWidth() else Modifier).titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
            }
        }

        // ── Body (shared) ─────────────────────────────────────
        @Composable
        fun BodyText(centered: Boolean = false) {
            // v329 — Reading-progress content draws the chapter widget in the
            // signature surface's own tones (no ruled lines; the bar wears
            // the rule/badge tone) instead of the prose.
            if (chapterProgress != null) {
                ChapterProgressBlock(
                    progress = chapterProgress,
                    fill = sig.bodyRuleColor ?: sig.badgeColor,
                    track = sig.bodyColor.copy(alpha = 0.16f),
                    ink = sig.bodyColor,
                    modifier = (if (centered) Modifier.fillMaxWidth() else Modifier)
                        .moveFact(move)
                        .onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                        }
                )
                return
            }
            // User's format alignment wins; otherwise honor the layout's own
            // alignment (Signature has centered variants).
            val align = move.factAlign ?: if (centered) TextAlign.Center else TextAlign.Start
            val factStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize * bodyScale).sp,
                lineHeight = (bodySize * sig.bodyLineHeight * bodyScale).sp,
                color = sig.bodyColor
            ), move).copy(textAlign = align)
            // Book-cover ruled lines BEHIND the text, spaced at the body's own
            // line height so the facts sit exactly on the lines (drawn in the
            // same local space as the text, so they move with the box).
            val ruleColor = sig.bodyRuleColor
            Text(body, style = factStyle, maxLines = lines(bodyMaxLines, move.factHeightFrac), overflow = TextOverflow.Ellipsis,
                modifier = (if (centered) Modifier.fillMaxWidth() else Modifier)
                    .moveFact(move)
                    .then(if (ruleColor != null) Modifier.drawBehind {
                        val lh = factStyle.lineHeight.toPx()
                        if (lh > 0f) {
                            var y = lh * 0.80f
                            while (y < size.height) {
                                drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                                y += lh
                            }
                        }
                    } else Modifier)
                    .onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    })
        }

        // ── Footer (shared) — FIXED: never moves (only the author/year row
        // reports + follows the M handle). ──
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
    val layout: SignatureLayout = SignatureLayout.STANDARD,
    // Minimal-style giant faint initial (a letter/symbol) — rendered by the
    // SignatureCard composable over the background, instead of drawn scenes.
    val watermark: String? = null,
    // Small icon crest rendered top-right (a REAL icon-font glyph, not a
    // drawn path) — replaces hand-drawn crests with an actual symbol.
    val crest: String? = null,
    val crestTint: Color = Color.Unspecified,
    // When set, ruled lines are drawn BEHIND the fact text at the body's
    // own line height, so the facts sit exactly on the lines (printed-page
    // book-cover look).
    val bodyRuleColor: Color? = null
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

// Helper: quiet hairline frame inset from the card edges (the minimal
// signature treatment) — a thin rounded-rect outline in a single color.
private fun DrawScope.signatureHairlineFrame(w: Float, h: Float, color: Color) {
    val inset = kotlin.math.min(w, h) * 0.045f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(w - inset * 2f, h - inset * 2f),
        cornerRadius = CornerRadius(kotlin.math.min(w, h) * 0.05f),
        style = Stroke(width = 1f)
    )
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
        // ═══ INTERNET — circuit board + WiFi + binary ��══
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
        // ═══ ARTISTS — quiet stage: hairline frame + tiny spotlight crest ═══
        cat == "ARTISTS" -> SignatureDesign(
            bg = Color(0xFF16120E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF241C14), Color(0xFF16120E), Color(0xFF0B0806))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color.White.copy(alpha = 0.14f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8B878), badgeInk = Color(0xFF16120E),
            watermark = "brush",
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 16.dp,
            titleFont = BebasNeueFontFamily, titleSize = 42.sp, titleLineHeight = 44.sp, titleColor = Color(0xFFF5EAD8),
            metaSpacer = 6.dp, metaSeparator = " \u2022 ", metaSize = 11.sp, metaColor = Color(0xFFE8B878),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE2D6C2).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFE8B878).copy(alpha = 0.70f),
            layout = SignatureLayout.POSTER
        )
        // ═══ WILDCARD — coral glow, Bebas Neue, minimalist ═══
        cat == "WILDCARD" -> SignatureDesign(
            bg = Color(0xFF1A0E22), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1638), Color(0xFF1A0E22), Color(0xFF0E0614))), size = Size(w, h))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFF7A6B).copy(alpha = 0.14f), Color.Transparent)), w * 0.28f, Offset(w * 0.30f, h * 0.70f))
                val comet = Path().apply { moveTo(w * 0.72f, h * 0.16f); cubicTo(w * 0.55f, h * 0.30f, w * 0.40f, h * 0.40f, w * 0.20f, h * 0.52f); close() }
                drawPath(comet, Color(0xFFFF9A8B).copy(alpha = 0.10f))
                drawCircle(Color(0xFFFFC8A0).copy(alpha = 0.50f), w * 0.012f, Offset(w * 0.72f, h * 0.16f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF7A6B), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = BebasNeueFontFamily, titleSize = 40.sp, titleLineHeight = 42.sp, titleColor = Color(0xFFFFE8E0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFFF9A8B),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFF0D8D0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF9A8B).copy(alpha = 0.65f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ ANIMALS — quiet field note: hairline frame + tiny paw crest ═══
        cat == "ANIMALS" -> SignatureDesign(
            bg = Color(0xFFEFF3F0), cornerRadius = 10f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF8FAF7), Color(0xFFEFF3F0), Color(0xFFE1E8E2))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color(0xFF2E3A2C).copy(alpha = 0.16f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF5E7A5A), badgeInk = Color(0xFFEFF3F0),
            watermark = "pets",
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 16.dp,
            titleFont = LoraFontFamily, titleSize = 30.sp, titleLineHeight = 36.sp, titleColor = Color(0xFF2E3A2C),
            metaSpacer = 6.dp, metaSeparator = " \u2022 ", metaSize = 11.sp, metaColor = Color(0xFF5E7A5A),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF3A4A38).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF5E7A5A).copy(alpha = 0.65f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ ANIMATED FILMS — quiet storybook: hairline frame + tiny star ═══
        cat == "ANIMATED FILMS" || cat == "ANIMATED MOVIES" -> SignatureDesign(
            bg = Color(0xFFF6EFF7), cornerRadius = 10f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFDF5F4), Color(0xFFF6EFF7), Color(0xFFEDEAF6))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color(0xFF3A2E3A).copy(alpha = 0.16f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFA87BC0), badgeInk = Color.White,
            watermark = "movie_filter",
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 16.dp,
            titleFont = CorbenFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2E3A),
            metaSpacer = 6.dp, metaSeparator = " \u2022 ", metaSize = 11.sp, metaColor = Color(0xFFA87BC0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A3E4A).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFA87BC0).copy(alpha = 0.65f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ ANIME — quiet paper: hairline frame + tiny sun crest ═══
        cat == "ANIME" -> SignatureDesign(
            bg = Color(0xFFF5F6F8), cornerRadius = 10f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFCFCFD), Color(0xFFF5F6F8), Color(0xFFE8EAEF))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color(0xFF2A1E1E).copy(alpha = 0.16f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD84343), badgeInk = Color.White,
            watermark = "auto_awesome",
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 16.dp,
            titleFont = MavenProFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF2A1E1E),
            metaSpacer = 6.dp, metaSeparator = " \u2022 ", metaSize = 11.sp, metaColor = Color(0xFFD84343),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A3A38).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFD84343).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ ARTWORKS — quiet gallery: hairline frame + tiny frame crest ═══
        cat == "ARTWORKS" -> SignatureDesign(
            bg = Color(0xFFECEFF2), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF6F7F9), Color(0xFFECEFF2), Color(0xFFDDE1E6))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color(0xFF2E2C28).copy(alpha = 0.16f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF2E2C28), badgeInk = Color(0xFFECEFF2),
            watermark = "museum",
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp,
            titleFont = CormorantGaramondFontFamily, titleSize = 32.sp, titleLineHeight = 38.sp, titleColor = Color(0xFF1E1C18),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 11.sp, metaColor = Color(0xFF8A8278),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF3A3832).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = CormorantGaramondFontFamily, footerColor = Color(0xFF8A8278).copy(alpha = 0.70f),
            layout = SignatureLayout.STANDARD
        )
        // ═══ AUTHORS — quiet manuscript: hairline frame + tiny quill ═══
        cat == "AUTHORS" -> SignatureDesign(
            bg = Color(0xFFF1F3F6), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF9FAFC), Color(0xFFF1F3F6), Color(0xFFE4E8EE))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color(0xFF2A241A).copy(alpha = 0.16f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF7A3A2E), badgeInk = Color(0xFFF1F3F6),
            watermark = "edit_note",
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = PlayfairDisplayFontFamily, titleSize = 30.sp, titleLineHeight = 36.sp, titleColor = Color(0xFF2A241A),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 11.sp, metaColor = Color(0xFF7A3A2E),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF4A4034).copy(alpha = 0.85f),
            footerSpacer = 8.dp, footerFont = PlayfairDisplayFontFamily, footerColor = Color(0xFF7A3A2E).copy(alpha = 0.65f),
            layout = SignatureLayout.STANDARD
        )
        // ═══ BIOLOGY — quiet lab: hairline frame + tiny helix crest ═══
        cat == "BIOLOGY" -> SignatureDesign(
            bg = Color(0xFF0A1A18), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0F2C26), Color(0xFF0A1A18), Color(0xFF040C0A))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color.White.copy(alpha = 0.14f))
                // Tiny helix crest, top-right
                drawCircle(Color(0xFF9FF0C0).copy(alpha = 0.8f), 1.8f, Offset(w * 0.852f, h * 0.075f))
                drawCircle(Color(0xFF4FA85E).copy(alpha = 0.8f), 1.8f, Offset(w * 0.872f, h * 0.105f))
                drawLine(Color(0xFF6BE3A0).copy(alpha = 0.6f), Offset(w * 0.852f, h * 0.075f), Offset(w * 0.872f, h * 0.105f), strokeWidth = 1f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF6BE3A0), badgeInk = Color(0xFF0A1A18),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = BioRhymeFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE0F5E8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF6BE3A0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8E0D0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = BioRhymeFontFamily, footerColor = Color(0xFF6BE3A0).copy(alpha = 0.65f),
            layout = SignatureLayout.SIDE
        )
        // ═══ BOOKS — classic cloth hardcover: oxblood leather + gold foil
        // margins, fact text sitting on ruled lines like a printed page ═══
        cat == "BOOKS" -> SignatureDesign(
            bg = Color(0xFF4A1D24), cornerRadius = 8f,
            drawBackground = { w, h ->
                // Cloth cover — deep oxblood leather gradient
                drawRect(Brush.verticalGradient(listOf(Color(0xFF5A2430), Color(0xFF4A1D24), Color(0xFF2E0F15))), size = Size(w, h))
                // Gold foil margins — a book cover's double border rule
                val m1 = kotlin.math.min(w, h) * 0.05f
                val m2 = kotlin.math.min(w, h) * 0.068f
                drawRoundRect(Color(0xFFD9B45F).copy(alpha = 0.55f), Offset(m1, m1), Size(w - m1 * 2f, h - m1 * 2f), CornerRadius(3f), style = Stroke(1.2f))
                drawRoundRect(Color(0xFFD9B45F).copy(alpha = 0.30f), Offset(m2, m2), Size(w - m2 * 2f, h - m2 * 2f), CornerRadius(2f), style = Stroke(0.7f))
                // Spine band on the left edge — like the leather spine of a
                // real book, plus its gold hinge rules
                drawRect(Color(0xFFD9B45F).copy(alpha = 0.10f), Offset(w * 0.030f, m1), Size(w * 0.006f, h - m1 * 2f))
                drawRect(Color(0xFFD9B45F).copy(alpha = 0.16f), Offset(w * 0.036f, m1), Size(w * 0.006f, h - m1 * 2f))
                drawRect(Color(0xFFD9B45F).copy(alpha = 0.10f), Offset(w * 0.042f, m1), Size(w * 0.004f, h - m1 * 2f))
            },
            padding = PaddingValues(horizontal = 30.dp, vertical = 24.dp), badgeColor = Color(0xFFD9B45F), badgeInk = Color(0xFF3A151B),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = FrauncesFontFamily, titleSize = 30.sp, titleLineHeight = 36.sp, titleColor = Color(0xFFF5E8D0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFD9B45F),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFF2E6CE).copy(alpha = 0.92f),
            footerSpacer = 8.dp, footerFont = FrauncesFontFamily, footerColor = Color(0xFFD9B45F).copy(alpha = 0.70f),
            layout = SignatureLayout.STANDARD,
            watermark = "auto_stories",
            crest = "menu_book",
            crestTint = Color(0xFFD9B45F),
            bodyRuleColor = Color(0xFFD9B45F).copy(alpha = 0.35f)
        )
        // ═══ CHEMISTRY — quiet lab: hairline frame + tiny hexagon crest ═══
        cat == "CHEMISTRY" -> SignatureDesign(
            bg = Color(0xFF0A121E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF10243A), Color(0xFF0A121E), Color(0xFF04080E))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color.White.copy(alpha = 0.14f))
                // Tiny hexagon crest, top-right
                val hp = (0 until 6).map { k -> val a = Math.toRadians((60.0 * k + 90.0)).toFloat(); Offset(w * 0.86f + kotlin.math.cos(a) * w * 0.014f, h * 0.09f + kotlin.math.sin(a) * w * 0.014f) }
                for (k in 0 until 6) drawLine(Color(0xFF4FE8E8).copy(alpha = 0.7f), hp[k], hp[(k + 1) % 6], strokeWidth = 1.1f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FE8E8), badgeInk = Color(0xFF0A121E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = OxaniumFontFamily, titleSize = 24.sp, titleLineHeight = 30.sp, titleColor = Color(0xFFE0F0FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4FE8E8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8D8E8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = OxaniumFontFamily, footerColor = Color(0xFF4FE8E8).copy(alpha = 0.65f),
            layout = SignatureLayout.STANDARD
        )
        // ═══ DIRECTORS — quiet marquee: hairline frame + tiny board ═══
        cat == "DIRECTORS" -> SignatureDesign(
            bg = Color(0xFF14101A), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF241C30), Color(0xFF14101A), Color(0xFF08060E))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color.White.copy(alpha = 0.14f))
                // Tiny clapperboard crest, top-right
                drawPath(Path().apply { moveTo(w * 0.845f, h * 0.10f); lineTo(w * 0.875f, h * 0.085f); lineTo(w * 0.875f, h * 0.115f); lineTo(w * 0.845f, h * 0.13f); close() }, Color(0xFFC9A24F).copy(alpha = 0.6f))
                drawLine(Color(0xFFC9A24F).copy(alpha = 0.8f), Offset(w * 0.85f, h * 0.098f), Offset(w * 0.863f, h * 0.09f), strokeWidth = 1.2f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A24F), badgeInk = Color(0xFF14101A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 16.dp,
            titleFont = LimelightFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF0E0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9A24F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LimelightFontFamily, footerColor = Color(0xFFC9A24F).copy(alpha = 0.65f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ DISCOVERIES — quiet map: hairline frame + tiny compass crest ═══
        cat == "DISCOVERIES" -> SignatureDesign(
            bg = Color(0xFF17150E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF262210), Color(0xFF17150E), Color(0xFF0A0804))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color.White.copy(alpha = 0.14f))
                // Tiny compass crest, top-right
                for (i in 0 until 4) {
                    val a = Math.toRadians((45.0 * i)).toFloat()
                    drawLine(Color(0xFFE8C84F).copy(alpha = 0.7f), Offset(w * 0.86f, h * 0.09f), Offset(w * 0.86f + kotlin.math.cos(a) * w * 0.02f, h * 0.09f + kotlin.math.sin(a) * w * 0.02f), strokeWidth = 1.1f)
                }
                drawCircle(Color(0xFFE8C84F).copy(alpha = 0.85f), 1.6f, Offset(w * 0.86f, h * 0.09f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8C84F), badgeInk = Color(0xFF17150E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = RyeFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF5E8C8),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFE8C84F),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFE0D8B8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = RyeFontFamily, footerColor = Color(0xFFE8C84F).copy(alpha = 0.65f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ ECONOMICS — quiet markets: hairline frame + tiny arrow crest ═══
        cat == "ECONOMICS" -> SignatureDesign(
            bg = Color(0xFF0E1418), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF16222A), Color(0xFF0E1418), Color(0xFF060A0C))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color.White.copy(alpha = 0.14f))
                // Tiny rising-arrow crest, top-right
                drawLine(Color(0xFF4FE8C8).copy(alpha = 0.7f), Offset(w * 0.845f, h * 0.115f), Offset(w * 0.875f, h * 0.075f), strokeWidth = 1.3f)
                drawLine(Color(0xFF4FE8C8).copy(alpha = 0.7f), Offset(w * 0.865f, h * 0.078f), Offset(w * 0.875f, h * 0.075f), strokeWidth = 1.3f)
                drawLine(Color(0xFF4FE8C8).copy(alpha = 0.7f), Offset(w * 0.872f, h * 0.085f), Offset(w * 0.875f, h * 0.075f), strokeWidth = 1.3f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4FE8C8), badgeInk = Color(0xFF0E1418),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = SpaceGroteskFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFE0F0E8),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4FE8C8),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8D8D0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = SpaceGroteskFontFamily, footerColor = Color(0xFF4FE8C8).copy(alpha = 0.65f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ FILMS — quiet cinema: hairline frame + tiny film-strip crest ═══
        cat == "FILMS" -> SignatureDesign(
            bg = Color(0xFF10080E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E0C1C), Color(0xFF10080E), Color(0xFF060408))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color.White.copy(alpha = 0.14f))
                // Tiny film-strip crest, top-right
                drawRect(Color(0xFFE8A5A0).copy(alpha = 0.5f), Offset(w * 0.845f, h * 0.075f), Size(w * 0.028f, h * 0.045f))
                drawCircle(Color(0xFF10080E).copy(alpha = 0.8f), 1.1f, Offset(w * 0.853f, h * 0.085f))
                drawCircle(Color(0xFF10080E).copy(alpha = 0.8f), 1.1f, Offset(w * 0.865f, h * 0.085f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC2402E), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 16.dp,
            titleFont = AntonFontFamily, titleSize = 36.sp, titleLineHeight = 40.sp, titleColor = Color(0xFFFFF0E0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFE8A5A0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0C8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = AntonFontFamily, footerColor = Color(0xFFE8A5A0).copy(alpha = 0.65f),
            layout = SignatureLayout.POSTER
        )
        // ═══ FOOD — table, Corben title ═══
        cat == "FOOD" -> SignatureDesign(
            bg = Color(0xFF1A140E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2218), Color(0xFF1A140E), Color(0xFF0E0806))), size = Size(w, h))
                drawCircle(Color(0xFFFFE0A0).copy(alpha = 0.10f), w * 0.18f, Offset(w * 0.4f, h * 0.55f))
                drawCircle(Color(0xFFE8D0A0).copy(alpha = 0.40f), w * 0.08f, Offset(w * 0.4f, h * 0.60f))
                drawCircle(Color(0xFF6B8E4A).copy(alpha = 0.30f), w * 0.02f, Offset(w * 0.48f, h * 0.58f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD08840), badgeInk = Color(0xFF1A140E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = CorbenFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF0D8),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFD08840),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFE8D8C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = CorbenFontFamily, footerColor = Color(0xFFD08840).copy(alpha = 0.65f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ GEOLOGY — strata, Sora title ═══
        cat == "GEOLOGY" -> SignatureDesign(
            bg = Color(0xFF12101A), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1628), Color(0xFF12101A), Color(0xFF08060E))), size = Size(w, h))
                listOf(Pair(h * 0.68f, Color(0xFF6B5A8B).copy(alpha = 0.30f)), Pair(h * 0.74f, Color(0xFF8B7A5A).copy(alpha = 0.35f)), Pair(h * 0.80f, Color(0xFFA04030).copy(alpha = 0.30f))).forEach { (y, c) ->
                    drawRect(c, Offset(0f, y), Size(w, h * 0.06f))
                }
                drawPath(Path().apply { moveTo(w * 0.70f, h * 0.62f); lineTo(w * 0.76f, h * 0.54f); lineTo(w * 0.82f, h * 0.62f); close() }, Color(0xFFC9B8E0).copy(alpha = 0.25f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8B7A5A), badgeInk = Color(0xFF12101A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = SoraFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8E0F0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8B7A5A),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C8D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = SoraFontFamily, footerColor = Color(0xFF8B7A5A).copy(alpha = 0.65f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ HISTORY — timeline, Cormorant Garamond title ═══
        cat == "HISTORY" -> SignatureDesign(
            bg = Color(0xFF161210), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF221A18), Color(0xFF161210), Color(0xFF0A0806))), size = Size(w, h))
                drawLine(Color(0xFFC9A227).copy(alpha = 0.30f), Offset(w * 0.10f, h * 0.70f), Offset(w * 0.90f, h * 0.70f), strokeWidth = 1.2f)
                listOf(w * 0.20f, w * 0.40f, w * 0.60f, w * 0.80f).forEach { x ->
                    drawCircle(Color(0xFFC9A227).copy(alpha = 0.40f), 3f, Offset(x, h * 0.70f))
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A227), badgeInk = Color(0xFF161210),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = CormorantGaramondFontFamily, titleSize = 30.sp, titleLineHeight = 36.sp, titleColor = Color(0xFFF5E8D0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFE0D0B8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = CormorantGaramondFontFamily, footerColor = Color(0xFFC9A227).copy(alpha = 0.65f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ INTERNET — globe wireframe, Space Mono title ═══
        cat == "INTERNET" -> SignatureDesign(
            bg = Color(0xFF080E1A), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0E1828), Color(0xFF080E1A), Color(0xFF04060E))), size = Size(w, h))
                val cx = w * 0.5f; val cy = h * 0.42f; val r = w * 0.18f
                drawCircle(Color(0xFF6BD0FF).copy(alpha = 0.20f), r, Offset(cx, cy), style = Stroke(1.2f))
                drawLine(Color(0xFF6BD0FF).copy(alpha = 0.18f), Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 0.8f)
                drawArc(Color(0xFF6BD0FF).copy(alpha = 0.18f), 0f, 180f, false, Offset(cx - r * 0.5f, cy - r), Size(r, 2 * r), style = Stroke(0.8f))
                drawArc(Color(0xFF6BD0FF).copy(alpha = 0.18f), 180f, 180f, false, Offset(cx - r * 0.5f, cy - r), Size(r, 2 * r), style = Stroke(0.8f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4A9BCC), badgeInk = Color(0xFF080E1A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = SpaceMonoFontFamily, titleSize = 22.sp, titleLineHeight = 28.sp, titleColor = Color(0xFFE0F0FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4A9BCC),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8D8E8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = SpaceMonoFontFamily, footerColor = Color(0xFF4A9BCC).copy(alpha = 0.65f),
            layout = SignatureLayout.SIDE
        )
        // ═══ LANGUAGE — minimal night ink: many-language texts overlay, Patrick Hand title ═══
        cat == "LANGUAGE" -> SignatureDesign(
            bg = Color(0xFF12101A), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1828), Color(0xFF12101A), Color(0xFF08060E))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color(0xFFC9B8E0).copy(alpha = 0.14f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8B7AB0), badgeInk = Color(0xFF12101A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = PatrickHandFontFamily, titleSize = 32.sp, titleLineHeight = 36.sp, titleColor = Color(0xFFF0E8F5),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9B8E0),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFE0D8E8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = PatrickHandFontFamily, footerColor = Color(0xFFC9B8E0).copy(alpha = 0.65f),
            layout = SignatureLayout.SIDE
        )
        // ═══ MANGA — speedlines, ChangaOne title ═══
        cat == "MANGA" -> SignatureDesign(
            bg = Color(0xFF0E0E12), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.radialGradient(listOf(Color(0xFF1A1A22), Color(0xFF0E0E12), Color(0xFF060608)), center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.7f), size = Size(w, h))
                val cx = w * 0.5f; val cy = h * 0.5f
                for (i in 0 until 12) { val a = Math.toRadians((30.0 * i)).toFloat(); drawLine(Color.White.copy(alpha = 0.08f), Offset(cx + kotlin.math.cos(a) * w * 0.10f, cy + kotlin.math.sin(a) * w * 0.10f), Offset(cx + kotlin.math.cos(a) * w * 0.60f, cy + kotlin.math.sin(a) * w * 0.60f), strokeWidth = 1f) }
                drawLine(Color(0xFFC2402E).copy(alpha = 0.50f), Offset(w * 0.20f, h * 0.30f), Offset(w * 0.30f, h * 0.40f), strokeWidth = 2.5f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC2402E), badgeInk = Color.White,
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFFFFF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8A5A0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0E0E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = ChangaOneFontFamily, footerColor = Color(0xFFE8A5A0).copy(alpha = 0.70f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ MANHWA — dreamy arch, Playfair Display title ═══
        cat == "MANHWA" -> SignatureDesign(
            bg = Color(0xFF1A1420), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2E2238), Color(0xFF1A1420), Color(0xFF0E0A12))), size = Size(w, h))
                drawCircle(Color(0xFFFFD0E0).copy(alpha = 0.08f), w * 0.30f, Offset(w * 0.5f, h * 0.40f))
                drawArc(Color(0xFFFF9AB8).copy(alpha = 0.20f), 0f, 180f, false, Offset(w * 0.30f, h * 0.50f), Size(w * 0.40f, h * 0.24f), style = Stroke(1.5f))
                listOf(Offset(w * 0.25f, h * 0.30f), Offset(w * 0.70f, h * 0.40f), Offset(w * 0.40f, h * 0.25f), Offset(w * 0.60f, h * 0.32f)).forEach {
                    drawCircle(Color(0xFFFFE066).copy(alpha = 0.30f), 1.5f, it)
                }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF9AB8), badgeInk = Color(0xFF1A1420),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = PlayfairDisplayFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFFFF0F5),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFFF9AB8),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFF0D8E0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = PlayfairDisplayFontFamily, footerColor = Color(0xFFFF9AB8).copy(alpha = 0.65f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ MATHEMATICS — golden spiral, Space Mono title ═══
        cat == "MATHEMATICS" -> SignatureDesign(
            bg = Color(0xFF0A0E14), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF101820), Color(0xFF0A0E14), Color(0xFF040608))), size = Size(w, h))
                drawCircle(Color(0xFFE8C84A).copy(alpha = 0.15f), w * 0.02f, Offset(w * 0.50f, h * 0.50f))
                for (i in 0 until 5) { drawArc(Color(0xFFE8C84A).copy(alpha = 0.20f - i * 0.03f), 0f, 90f, false, Offset(w * 0.50f - w * 0.04f * (i + 1), h * 0.50f - w * 0.04f * (i + 1)), Size(w * 0.08f * (i + 1), w * 0.08f * (i + 1)), style = Stroke(1f)) }
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFE8C84A), badgeInk = Color(0xFF0A0E14),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = SpaceMonoFontFamily, titleSize = 22.sp, titleLineHeight = 28.sp, titleColor = Color(0xFFFFF8E0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE8C84A),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE8E0C8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = SpaceMonoFontFamily, footerColor = Color(0xFFE8C84A).copy(alpha = 0.65f),
            layout = SignatureLayout.SIDE
        )
        // ═══ MYTHOLOGY — gold meander, Pirata One title ═══
        cat == "MYTHOLOGY" -> SignatureDesign(
            bg = Color(0xFF12100E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E1A14), Color(0xFF12100E), Color(0xFF080604))), size = Size(w, h))
                val meander = Path()
                var x = w * 0.08f; val y = h * 0.72f; val step = w * 0.06f
                meander.moveTo(x, y)
                for (i in 0 until 12) { meander.lineTo(x + step * 0.5f, y - step * 0.4f); meander.lineTo(x + step, y); x += step }
                drawPath(meander, Color(0xFFC9A227).copy(alpha = 0.30f), style = Stroke(1.2f))
                drawLine(Color(0xFFC9A227).copy(alpha = 0.20f), Offset(w * 0.08f, h * 0.20f), Offset(w * 0.92f, h * 0.20f), strokeWidth = 0.8f)
                drawLine(Color(0xFFC9A227).copy(alpha = 0.20f), Offset(w * 0.08f, h * 0.24f), Offset(w * 0.92f, h * 0.24f), strokeWidth = 0.4f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A227), badgeInk = Color(0xFF12100E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = PirataOneFontFamily, titleSize = 26.sp, titleLineHeight = 30.sp, titleColor = Color(0xFFF5E8C8),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFE0D8B8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = PirataOneFontFamily, footerColor = Color(0xFFC9A227).copy(alpha = 0.65f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ PAINTERS — easel, Lora title ═══
        cat == "PAINTERS" -> SignatureDesign(
            bg = Color(0xFF14110E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E1A14), Color(0xFF14110E), Color(0xFF080604))), size = Size(w, h))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD8A0).copy(alpha = 0.10f), Color.Transparent)), w * 0.20f, Offset(w * 0.5f, h * 0.40f))
                drawRoundRect(Color(0xFF8B5A2B).copy(alpha = 0.30f), Offset(w * 0.28f, h * 0.55f), Size(w * 0.44f, h * 0.24f), CornerRadius(2.dp.toPx()), style = Stroke(1.2f))
                drawLine(Color(0xFF8B5A2B).copy(alpha = 0.30f), Offset(w * 0.30f, h * 0.79f), Offset(w * 0.40f, h * 0.90f), strokeWidth = 1.2f)
                drawLine(Color(0xFF8B5A2B).copy(alpha = 0.30f), Offset(w * 0.70f, h * 0.79f), Offset(w * 0.60f, h * 0.90f), strokeWidth = 1.2f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A227), badgeInk = Color(0xFF14110E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF5E8D0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFE0D0B8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A227).copy(alpha = 0.65f),
            layout = SignatureLayout.STANDARD
        )
        // ═══ PLANTS — botanical leaf, Lora title ═══
        cat == "PLANTS" -> SignatureDesign(
            bg = Color(0xFF0E1410), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF142018), Color(0xFF0E1410), Color(0xFF060A06))), size = Size(w, h))
                val leaf = Path().apply { moveTo(w * 0.5f, h * 0.55f); cubicTo(w * 0.70f, h * 0.50f, w * 0.70f, h * 0.75f, w * 0.50f, h * 0.80f); cubicTo(w * 0.30f, h * 0.75f, w * 0.30f, h * 0.50f, w * 0.50f, h * 0.55f); close() }
                drawPath(leaf, Color(0xFF6B8E4A).copy(alpha = 0.20f))
                drawLine(Color(0xFF6B8E4A).copy(alpha = 0.30f), Offset(w * 0.50f, h * 0.55f), Offset(w * 0.50f, h * 0.80f), strokeWidth = 0.8f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF6B8E4A), badgeInk = Color(0xFF0E1410),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = LoraFontFamily, titleSize = 30.sp, titleLineHeight = 36.sp, titleColor = Color(0xFFE8F0D8),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFF8AAA70),
            bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD0D8C0).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8AAA70).copy(alpha = 0.65f),
            layout = SignatureLayout.SIDE
        )
        // ═══ PSYCHOLOGY — mind profile, Sora title ═══
        cat == "PSYCHOLOGY" -> SignatureDesign(
            bg = Color(0xFF100E1A), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF18142A), Color(0xFF100E1A), Color(0xFF08060E))), size = Size(w, h))
                val profile = Path().apply { moveTo(w * 0.20f, h * 0.55f); cubicTo(w * 0.20f, h * 0.35f, w * 0.35f, h * 0.30f, w * 0.42f, h * 0.32f); cubicTo(w * 0.48f, h * 0.20f, w * 0.58f, h * 0.22f, w * 0.58f, h * 0.34f); lineTo(w * 0.58f, h * 0.55f); close() }
                drawPath(profile, Color(0xFF8B7AB0).copy(alpha = 0.15f))
                listOf(Offset(w * 0.30f, h * 0.40f), Offset(w * 0.40f, h * 0.36f), Offset(w * 0.48f, h * 0.42f), Offset(w * 0.36f, h * 0.46f), Offset(w * 0.44f, h * 0.48f)).forEach {
                    drawCircle(Color(0xFFC9B8E0).copy(alpha = 0.40f), 1.5f, it)
                }
                drawLine(Color(0xFFC9B8E0).copy(alpha = 0.20f), Offset(w * 0.30f, h * 0.40f), Offset(w * 0.40f, h * 0.36f), strokeWidth = 0.6f)
                drawLine(Color(0xFFC9B8E0).copy(alpha = 0.20f), Offset(w * 0.40f, h * 0.36f), Offset(w * 0.48f, h * 0.42f), strokeWidth = 0.6f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8B7AB0), badgeInk = Color(0xFF100E1A),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = SoraFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFF0E8F5),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9B8E0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D8E8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = SoraFontFamily, footerColor = Color(0xFFC9B8E0).copy(alpha = 0.65f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ QUOTES — giant marks, Playfair Display title ═══
        cat == "QUOTES" -> SignatureDesign(
            bg = Color(0xFF12100E), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1E1A14), Color(0xFF12100E), Color(0xFF080604))), size = Size(w, h))
                drawCircle(Color(0xFFC9A227).copy(alpha = 0.06f), w * 0.20f, Offset(w * 0.20f, h * 0.30f))
                drawCircle(Color(0xFFC9A227).copy(alpha = 0.06f), w * 0.20f, Offset(w * 0.80f, h * 0.70f))
                drawLine(Color(0xFFC9A227).copy(alpha = 0.20f), Offset(w * 0.10f, h * 0.50f), Offset(w * 0.90f, h * 0.50f), strokeWidth = 0.4f)
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A227), badgeInk = Color(0xFF12100E),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = PlayfairDisplayFontFamily, titleSize = 30.sp, titleLineHeight = 36.sp, titleColor = Color(0xFFF5E8D0),
            metaSpacer = 5.dp, metaSeparator = " \u2014 ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
            bodySize = 11f, bodyLineHeight = 1.65f, bodyColor = Color(0xFFE0D0B8).copy(alpha = 0.90f),
            footerSpacer = 8.dp, footerFont = PlayfairDisplayFontFamily, footerColor = Color(0xFFC9A227).copy(alpha = 0.60f),
            layout = SignatureLayout.CENTERED
        )
        // ═══ SCIENTISTS — blueprint, Space Mono title ═══
        cat == "SCIENTISTS" -> SignatureDesign(
            bg = Color(0xFF0A1218), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF102028), Color(0xFF0A1218), Color(0xFF04080A))), size = Size(w, h))
                for (i in 0 until 6) { drawLine(Color(0xFF4A9BCC).copy(alpha = 0.08f), Offset(0f, h * i / 6f), Offset(w, h * i / 6f), strokeWidth = 0.5f) }
                for (i in 0 until 4) { drawLine(Color(0xFF4A9BCC).copy(alpha = 0.08f), Offset(w * i / 4f, 0f), Offset(w * i / 4f, h), strokeWidth = 0.5f) }
                val cx = w * 0.5f; val cy = h * 0.55f; val r = w * 0.06f
                drawCircle(Color(0xFF6BD0FF).copy(alpha = 0.30f), r, Offset(cx, cy), style = Stroke(1f))
                drawLine(Color(0xFF6BD0FF).copy(alpha = 0.25f), Offset(cx - r * 1.5f, cy), Offset(cx + r * 1.5f, cy), strokeWidth = 0.8f)
                drawLine(Color(0xFF6BD0FF).copy(alpha = 0.25f), Offset(cx, cy - r * 1.5f), Offset(cx, cy + r * 1.5f), strokeWidth = 0.8f)
                drawCircle(Color(0xFF6BD0FF).copy(alpha = 0.30f), r * 0.5f, Offset(cx + r * 1.5f, cy))
                drawCircle(Color(0xFF6BD0FF).copy(alpha = 0.30f), r * 0.5f, Offset(cx - r * 1.5f, cy))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF4A9BCC), badgeInk = Color(0xFF0A1218),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = SpaceMonoFontFamily, titleSize = 22.sp, titleLineHeight = 28.sp, titleColor = Color(0xFFE0F0FF),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF4A9BCC),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC8D8E8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = SpaceMonoFontFamily, footerColor = Color(0xFF4A9BCC).copy(alpha = 0.65f),
            layout = SignatureLayout.SIDE
        )
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
        // ═══ DIRECTORS — one clapperboard under a warm key light ═══

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
        // ═══ MYTHOLOGY — gold on marble: meander, columns, laurel ═══

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
        // ═══ FOOD — quiet overhead table: plate, steaming bowl, basil ═══

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
        // ═══ ASTRONOMY — quiet sky: hairline frame + tiny star crest ═══
        cat == "ASTRONOMY" -> SignatureDesign(
            bg = Color(0xFF0B1020), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF121A30), Color(0xFF0B1020), Color(0xFF050812))), size = Size(w, h))
                signatureHairlineFrame(w, h, Color.White.copy(alpha = 0.14f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFFD8C88F), badgeInk = Color(0xFF0B1020),
            watermark = "nightlight",
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 16.dp,
            titleFont = SpaceMonoFontFamily, titleSize = 22.sp, titleLineHeight = 28.sp, titleColor = Color(0xFFD8E4F0),
            metaSpacer = 6.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFD8C88F),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB8C4D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = SpaceMonoFontFamily, footerColor = Color(0xFFD8C88F).copy(alpha = 0.70f),
            layout = SignatureLayout.BOTTOM
        )
        // ═══ HISTORY — parchment scroll, hourglass, timeline dots ═══

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
        // ═══ PSYCHOLOGY — mind profile with neural nodes + thought bubble ═══

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
        // ═══ QUOTES — elegant serif: quote marks, gold rules, flourish ═══
        // ═══ Default fallback — quiet neutral ═══
        else -> SignatureDesign(
            bg = Color(0xFF121016), cornerRadius = 8f,
            drawBackground = { w, h ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1820), Color(0xFF121016), Color(0xFF08060A))), size = Size(w, h))
                drawCircle(Brush.radialGradient(listOf(Color(0xFF8B7AB0).copy(alpha = 0.10f), Color.Transparent)), w * 0.25f, Offset(w * 0.5f, h * 0.40f))
            },
            padding = PaddingValues(22.dp), badgeColor = Color(0xFF8B7AB0), badgeInk = Color(0xFF121016),
            badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp,
            badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp,
            titleFont = SoraFontFamily, titleSize = 26.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE8E0F0),
            metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF8B7AB0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C8D8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = SoraFontFamily, footerColor = Color(0xFF8B7AB0).copy(alpha = 0.65f)
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
    byline: String = "", year: String? = null,
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
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
            Surface(shape = RoundedCornerShape(sig.badgeRadius), color = sig.badgeColor, modifier = Modifier.moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }) {
                Row(Modifier.padding(horizontal = sig.badgeHPadding, vertical = sig.badgeVPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CurioIcon(name = categoryGlyph, tint = sig.badgeInk, size = sig.badgeIconSize)
                    Text(categoryName.uppercase(), style = badgeStyle(TextStyle(
                        fontFamily = GeomFontFamily, fontSize = sig.badgeFontSize,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = sig.badgeLetterSpacing,
                        color = sig.badgeInk
                    ), move), maxLines = 1)
                }
            }
            Spacer(Modifier.height(sig.titleTopSpacer))
            Text(title, style = titleStyle(TextStyle(
                fontFamily = sig.titleFont, fontSize = sig.titleSize,
                lineHeight = sig.titleLineHeight, color = sig.titleColor
            ), move), maxLines = lines(4, move.titleHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveTitle(move).onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
            val metaParts = mutableListOf<String>()
            if (quoteText == null && byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(sig.metaSpacer))
                Text(metaParts.joinToString(sig.metaSeparator), style = metaStyle(TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = sig.metaSize, color = sig.metaColor
                ), move), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
            }
            Spacer(Modifier.weight(1f))
            val bodySize = when {
                body.length > 350 -> sig.bodySize - 2.5f
                body.length > 260 -> sig.bodySize - 1.5f
                body.length > 180 -> sig.bodySize - 0.5f
                else -> sig.bodySize
            }.coerceAtLeast(7f) * bodyScale
            val factStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = bodySize.sp,
                lineHeight = (bodySize * sig.bodyLineHeight).sp,
                color = sig.bodyColor
            ), move)
            // v329 — Reading-progress content draws the chapter widget in the
            // signature colors (bar in the badge tone, caption in body ink)
            // instead of the prose.
            if (chapterProgress != null) {
                ChapterProgressBlock(
                    progress = chapterProgress,
                    fill = sig.badgeColor,
                    track = sig.bodyColor.copy(alpha = 0.16f),
                    ink = sig.bodyColor,
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    }
                )
            } else {
                Text(body, style = factStyle, maxLines = lines(if (aspect == ShareCardAspect.PORTRAIT) 14 else 10, move.factHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveFact(move).onGloballyPositioned {
                    callbacks.onFact(it.boundsInWindow())
                    callbacks.onFactStyle(factStyle)
                })
            }
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
private fun HeaderRow(categoryName: String, glyph: String, palette: ShareCardPalette, move: ShareCardMove = ShareCardMove(), callbacks: EditBoundsCallbacks = EditBoundsCallbacks()) {
    // v3xx — ONLY the pill is the movable badge group: dragging the chip
    // moves the pill alone, the decorative Lightbulb stays anchored at the
    // top-right corner. The pill reports its own bounds (the row spans the
    // full card width — reporting it would lock the drag clamp).
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(14.dp), color = palette.accent.copy(alpha = 0.85f), modifier = Modifier.moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CurioIcon(name = glyph, tint = Color.White, size = 14.dp)
                Text(categoryName, style = badgeStyle(MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), move), color = Color.White)
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
    bodyScale: Float = 1f,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks(),
    move: ShareCardMove = ShareCardMove(),
    chapterProgress: ChapterProgressUi? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (quoteText != null) {
            CurioIcon(name = CurioIcons.FormatQuote, tint = palette.ink.copy(alpha = 0.20f), size = 32.dp)
            val qStyle = factBodyStyle(MaterialTheme.typography.titleLarge.copy(fontFamily = LoraFontFamily, fontSize = qSize, lineHeight = (qSize.value * 1.28f).sp), move)
            Text(quoteText, style = qStyle, color = palette.ink, maxLines = lines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 8, move.factHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveFact(move).onGloballyPositioned {
                callbacks.onFact(it.boundsInWindow())
                callbacks.onFactStyle(qStyle)
            })
        } else {
            // Title
            Text(display, style = titleStyle(MaterialTheme.typography.headlineLarge.copy(fontFamily = ChangaOneFontFamily, lineHeight = 40.sp), move), color = palette.ink, maxLines = lines(3, move.titleHeightFrac), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveTitle(move).onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
            // Metadata line — byline • year (e.g. "GUNS N' ROSES • 1987") — info row: movable, not editable.
            // v316b — sits right under the title, so it travels with the T drag too.
            val metaParts = mutableListOf<String>()
            if (byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            if (metaParts.isNotEmpty()) {
                Text(
                    metaParts.joinToString(" \u2022 "),
                    style = metaStyle(MaterialTheme.typography.labelSmall.copy(fontFamily = LoraFontFamily, fontWeight = FontWeight.SemiBold), move),
                    color = palette.ink.copy(alpha = 0.50f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) }
                )
            }
            if (ratingStars != null && ratingStars > 0) StarRow(ratingStars, palette)
            // v316b — style hoisted so the pane reports it to the editor.
            val qfs = if (aspect == ShareCardAspect.CLASSIC) quickFactFontSize34(factText.length) else quickFactFontSize(factText.length)
            val qfsScaled = (qfs.value * bodyScale).sp
            val frostStyle = factBodyStyle(MaterialTheme.typography.bodySmall.copy(
                fontFamily = LoraFontFamily, fontSize = qfsScaled,
                lineHeight = (qfsScaled.value * 1.4f).sp
            ), move)
            FrostPane(palette, Modifier.moveFact(move)) {
                // v329 — Reading-progress content draws the visual chapter
                // widget here (same bounds reporting, so the editor's box +
                // grip sit on the bar exactly as they would on text).
                if (chapterProgress != null) {
                    ChapterProgressBlock(
                        progress = chapterProgress,
                        fill = palette.accent,
                        track = palette.ink.copy(alpha = 0.14f),
                        ink = palette.ink,
                        modifier = Modifier.onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                            callbacks.onFactStyle(frostStyle)
                        }
                    )
                } else {
                    // v316b — the glyph box reports the bounds, NOT the frost
                    // pane: the pane's own 18dp padding would otherwise push
                    // the typing field's caret 18dp off the visible letters.
                    Text(
                        factText, style = frostStyle, color = palette.ink,
                        maxLines = lines(if (aspect == ShareCardAspect.PORTRAIT) 20 else 14, move.factHeightFrac),
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                            callbacks.onFactStyle(frostStyle)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FrostPane(palette: ShareCardPalette, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // v3xx — the caller's moveFact modifier is now the ONLY width driver
    // (it applies fillMaxWidth unconditionally), so the frost pane always
    // renders at the exact fact-width fraction — the fact width slider
    // visibly narrows Paper's pane instead of being overridden by the old
    // outer full-width fill.
    Box(modifier.drawBehind {
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
private fun Footer(sharerName: String, quoteText: String?, quoteAuthor: String?, palette: ShareCardPalette, move: ShareCardMove = ShareCardMove(), callbacks: EditBoundsCallbacks = EditBoundsCallbacks()) {
    // FIXED footer: only the author/year row (MiddleContent's meta line)
    // reports bounds and follows the M handle — the footer never moves.
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
// SHARE SHEET
// ═══════════════════════════════════════════════════════���═══════════════
/**
 * Wraps the preview card: tap-and-hold toggles inline editing, which works
 * on EVERY style. While editing: the QUICK-FACT becomes a transparent
 * BasicTextField IN PLACE on the card (type and the card updates live);
 * the TITLE is NOT type-editable (a hairline box marks its crop region,
 * but title text never changes via typing); and the INFO rows (author /
 * byline / year / footer) are movable but also never editable. Drag the
 * round T/F/M handles to move the title / fact / info boxes; drag the
 * edge handles to crop/resize width & height. The exported image matches
 * because the same move + box-size is threaded through every style.
 */
@Composable
private fun ArrangeableCard(
    active: Boolean,
    editMode: Boolean,
    quoteMode: Boolean,
    editFact: String,
    onFactChange: (String) -> Unit,
    // v323 — text editing for the quick fact is EXPLICIT: the field stays
    // inert until the "Edit text" tool turns this on (see the overlay below).
    factEditMode: Boolean = false,
    onFactEditModeChange: (Boolean) -> Unit = {},
    onToggleEdit: () -> Unit,
    onSelectResizeTarget: (ShareCardResizeTarget) -> Unit = {},
    selectedResizeTarget: ShareCardResizeTarget = ShareCardResizeTarget.NONE,
    move: ShareCardMove = ShareCardMove(),
    onMove: (ShareCardMove) -> Unit = {},
    // Metrics for the transparent typing field — must match the card's fact
    // rendering so the caret sits EXACTLY on the visible text (see the
    // compute in TopicShareSheet).
    factFieldStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    card: @Composable (EditBoundsCallbacks) -> Unit
) {
    // Bounds hub — every style reports where its title / fact / meta text
    // actually renders; the overlay below anchors its indicators to those
    // exact bounds instead of guessing fixed card fractions.
    val cardOrigin = androidx.compose.runtime.remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val titleRect = androidx.compose.runtime.remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val factRect = androidx.compose.runtime.remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val metaRect = androidx.compose.runtime.remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val badgeRect = androidx.compose.runtime.remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // v316b — the card reports the ACTUAL style its fact renders with, so the
    // invisible field above it uses identical metrics (family/size/line).
    val liveFactStyle = androidx.compose.runtime.remember { mutableStateOf(factFieldStyle) }
    val boundsHub = androidx.compose.runtime.remember {
        EditBoundsCallbacks(
            onTitle = { r -> titleRect.value = r },
            onFact = { r -> factRect.value = r },
            onMeta = { r -> metaRect.value = r },
            onBadge = { r -> badgeRect.value = r },
            onFactStyle = { s -> liveFactStyle.value = s }
        )
    }
    // v323 — tapping any other element must end the quick-fact's text editing:
    // clear focus (closes the keyboard) before switching the selection.
    val focusManager = LocalFocusManager.current
    val factRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .onGloballyPositioned { cardOrigin.value = it.positionInWindow() }
            .pointerInput(active, editMode) {
                // While editing, the gesture is disabled so taps reach the fields.
                if (active && !editMode) detectTapGestures(onLongPress = { onToggleEdit() })
            }
    ) {
        card(boundsHub)
        if (editMode) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val cw = maxWidth.value; val ch = maxHeight.value
                if (cw == 0f || ch == 0f) return@BoxWithConstraints
                val editDensity = androidx.compose.ui.platform.LocalDensity.current
                val origin = cardOrigin.value
                // Style-reported window bounds → card-local dp floats.
                fun local(r: androidx.compose.ui.geometry.Rect): androidx.compose.ui.geometry.Rect =
                    with(editDensity) {
                        androidx.compose.ui.geometry.Rect(
                            (r.left - origin.x).toDp().value,
                            (r.top - origin.y).toDp().value,
                            (r.right - origin.x).toDp().value,
                            (r.bottom - origin.y).toDp().value
                        )
                    }

                // v3xx — NEW selection model: nothing is shown when edit mode
                // starts (the user asked: no boxes/grips on hold). Tapping a
                // thing on the card selects it — the selected box gets the
                // darker-coffee outline + ONE uniform move grip, and the
                // toolbar below adapts to that element. Unselected boxes show
                // a FAINT outline so the user can see what's tappable.
                val sel = selectedResizeTarget
                val selBorder = { isSel: Boolean ->
                    if (isSel) CoffeeChromeDeep else CoffeeChrome.copy(alpha = 0.28f)
                }

                // v330 — tapping EMPTY card space (not another element)
                // auto-deselects the current box. Laid out FIRST so the
                // element boxes below it sit on top and keep their taps;
                // horizontal drags still reach the style pager.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                            onSelectResizeTarget(ShareCardResizeTarget.NONE)
                        }
                )

                // TITLE — tap selects; grip only when selected.
                if (!quoteMode) {
                    val t = local(titleRect.value)
                    val tOk = t.width > 0f && t.height > 0f
                    if (tOk) {
                        val isSel = sel == ShareCardResizeTarget.TITLE
                        Box(
                            modifier = Modifier
                                .offset(t.left.dp, t.top.dp)
                                .width(t.width.dp)
                                .height(t.height.dp)
                            .clickable {
                                focusManager.clearFocus()
                                onSelectResizeTarget(ShareCardResizeTarget.TITLE)
                            }
                            .border(1.dp, selBorder(isSel), RoundedCornerShape(8.dp))
                        )
                        if (isSel) {
                            MoveHandle(
                                x = t.left.dp,
                                y = t.top.dp,
                                onDelta = { dx, dy ->
                                    val nx = (move.titleDx + dx).coerceIn(-t.left, cw - t.left - t.width)
                                    val ny = (move.titleDy + dy).coerceIn(-t.top, ch - t.top - t.height)
                                    onMove(move.copy(titleDx = nx, titleDy = ny))
                                }
                            )
                        }
                    }
                }

                // Quick-fact — transparent field laid EXACTLY over the card's
                // own fact text (bounds AND style reported by the card, so the
                // caret sits on the visible glyphs of every font). Always
                // present (so tapping the fact focuses/selects it); its border
                // only appears once selected.
                val f = local(factRect.value)
                val fOk = f.width > 0f && f.height > 0f
                if (fOk) {
                    val isSel = sel == ShareCardResizeTarget.FACT
                    BasicTextField(
                        value = editFact,
                        onValueChange = onFactChange,
                        // v323 — the field is INERT until the "Edit text" tool
                        // arms it, so a plain tap can never hijack the selection
                        // into text editing (the overlay below owns taps).
                        enabled = factEditMode,
                        // Card-reported style (fallback: the sheet's base Lora
                        // metrics) with the text transparent — the caret + wrap
                        // use the card's real font, size and line height.
                        textStyle = liveFactStyle.value.copy(color = Color.Transparent),
                        cursorBrush = SolidColor(CoffeeChromeDeep),
                        singleLine = false,
                        maxLines = 24,
                        modifier = Modifier
                            .offset(f.left.dp, f.top.dp)
                            .width(f.width.dp)
                            .heightIn(min = f.height.dp)
                            .focusRequester(factRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    onSelectResizeTarget(ShareCardResizeTarget.FACT)
                                } else {
                                    // Leaving the field ends text-editing mode, so
                                    // the next tap selects the box for moving.
                                    onFactEditModeChange(false)
                                }
                            }
                            .border(1.dp, selBorder(isSel), RoundedCornerShape(8.dp)),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth()) {
                                if (editFact.isBlank()) Text("Edit the quick fact…", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                                inner()
                            }
                        }
                    )
                    // Tap-to-select layer: while text editing is OFF the field is
                    // inert, so an invisible box on top selects the fact for moving
                    // (grip appears) without popping the keyboard. Text editing
                    // starts only via the explicit "Edit text" tool.
                    if (!factEditMode) {
                        Box(
                            modifier = Modifier
                                .offset(f.left.dp, f.top.dp)
                                .width(f.width.dp)
                                .height(f.height.dp)
                                .clickable { onSelectResizeTarget(ShareCardResizeTarget.FACT) }
                        )
                    }
                    // When the "Edit text" tool arms the field, focus it so the
                    // keyboard opens right where the caret should sit.
                    androidx.compose.runtime.LaunchedEffect(factEditMode) {
                        if (factEditMode) factRequester.requestFocus()
                    }
                    // v330 — while TYPING the quick fact the grip hides
                    // (it used to hover on top of the letters being typed);
                    // it reappears the moment text editing ends.
                    if (isSel && !factEditMode) {
                        MoveHandle(
                            x = f.left.dp,
                            y = f.top.dp,
                            onDelta = { dx, dy ->
                                val nx = (move.factDx + dx).coerceIn(-f.left, cw - f.left - f.width)
                                val ny = (move.factDy + dy).coerceIn(-f.top, ch - f.top - f.height)
                                onMove(move.copy(factDx = nx, factDy = ny))
                            }
                        )
                    }
                }

                // AUTHOR / YEAR info rows — tap the row to select, grip only
                // when selected. The row's box spans its reported bounds.
                val m = local(metaRect.value)
                val mPad = 18f
                if (m.width > 0f && m.height > 0f) {
                    val isSel = sel == ShareCardResizeTarget.META
                    Box(
                        modifier = Modifier
                            .offset(m.left.dp, m.top.dp)
                            .width(m.width.dp)
                            .height(m.height.dp)
                            .clickable {
                                focusManager.clearFocus()
                                onSelectResizeTarget(ShareCardResizeTarget.META)
                            }
                            .border(1.dp, selBorder(isSel), RoundedCornerShape(8.dp))
                    )
                    if (isSel) {
                        MoveHandle(
                            x = (m.right + mPad).coerceAtMost(cw - 16f).dp,
                            y = m.bottom.dp.coerceAtMost((ch - 16f).dp),
                            onDelta = { dx, dy ->
                                val maxX = (cw - m.right - mPad).coerceAtLeast(-m.left + mPad)
                                val maxY = (ch - m.bottom - mPad).coerceAtLeast(-m.top + mPad)
                                val nx = (move.metaDx + dx).coerceIn(-m.left + mPad, maxX)
                                val ny = (move.metaDy + dy).coerceIn(-m.top + mPad, maxY)
                                onMove(move.copy(metaDx = nx, metaDy = ny))
                            }
                        )
                    }
                }

                // Badge — tap the chip to select, grip only when selected.
                // Anchored to the chip's REPORTED bounds (each style reports
                // its real category pill), so the grip sits on the chip
                // wherever it is — never a guessed corner.
                val b = local(badgeRect.value)
                val bOk = b.width > 0f && b.height > 0f
                if (bOk) {
                    val isSel = sel == ShareCardResizeTarget.BADGE
                    Box(
                        modifier = Modifier
                            .offset(b.left.dp, b.top.dp)
                            .width(b.width.dp)
                            .height(b.height.dp)
                            .clickable {
                                focusManager.clearFocus()
                                onSelectResizeTarget(ShareCardResizeTarget.BADGE)
                            }
                            .border(1.dp, selBorder(isSel), RoundedCornerShape(8.dp))
                    )
                    if (isSel) {
                        MoveHandle(
                            x = (b.left - 2f).coerceIn(0f, (cw - 24f).coerceAtLeast(0f)).dp,
                            y = (b.top - 2f).coerceIn(0f, (ch - 24f).coerceAtLeast(0f)).dp,
                            onDelta = { dx, dy ->
                                val nx = (move.badgeDx + dx).coerceIn(-b.left - 2f, cw - b.left - b.width + 2f)
                                val ny = (move.badgeDy + dy).coerceIn(-b.top - 2f, ch - b.top - b.height + 2f)
                                onMove(move.copy(badgeDx = nx, badgeDy = ny))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One box-size slider in the inline editor: an explicit label ("Title width")
 * + a live percent readout + snap steps, so the user always knows which
 * dimension they're editing and can hit exact sizes instead of an unlabeled
 * continuous drag. Widths step in 1%, heights in ~5% (they drive whole-line
 * counts, so finer steps just feel sticky).
 */
@Composable
private fun SizeSliderColumn(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier
) {
    // v316b — captured in the COMPOSABLE scope (not inside the slider's plain
    // callback lambdas) so the haptic calls compile, and so we can tick each
    // time the thumb crosses a snap step while dragging — not just on release.
    val haptics = LocalHapticFeedback.current
    val stepsTotal = steps + 1
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    fun stepIndex(v: Float): Int =
        (((v - range.start) / span) * stepsTotal).roundToInt().coerceIn(0, stepsTotal)
    // The step the thumb currently sits on, so we only tick on actual
    // step changes (a held drag over one step doesn't buzz repeatedly).
    var lastStep by remember { mutableIntStateOf(stepIndex(value)) }
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(value * 100f).roundToInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
        }
        Slider(
            value = value,
            onValueChange = { v ->
                onValueChange(v)
                val s = stepIndex(v)
                if (s != lastStep) {
                    lastStep = s
                    // Short tick per snap step — the size control feels
                    // precise because each 1%/step lands with feedback.
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onValueChangeFinished = {
                // A final confirm tick when the drag ends on a snap.
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            },
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TopicShareSheet(
    topicName: String, categoryName: String, categoryGlyph: String,
    accent: Color, quickFact: String, authority: String,
    context: android.content.Context, savedSources: List<ShareCardContent> = emptyList(),
    onDismiss: () -> Unit, categoryFamily: CategoryFamily = CategoryFamily.WILDCARD,
    topicByline: String = "",
    // v328 — BOOK share cards: when the caller hands the topic's chapters,
    // the editor offers a Chapter progress content (reads the BookNotes
    // reading-progress pref for this topic) and a Chapter review content
    // (written review tagged with a chosen chapter).
    bookChapters: List<com.curio.app.data.BookChapter> = emptyList(),
    // v229d — the sheet can open PRESELECTED: the Share Hub picks a design on
    // the grid and hands its style index + classic-signature flag here, so the
    // sheet opens on exactly the design the user picked (the reveal + detail
    // screens keep the defaults: first style, current signature).
    initialStyle: Int = 0,
    initialClassicSignature: Boolean = false,
    // v229d — "Share as text" lives IN the shared sheet (both reveal + detail
    // get it). When set, the caller supplies its own payload (the detail view
    // sends the entry's decorated text); otherwise the sheet builds a default
    // topic + fact payload from its own params.
    shareAsText: (() -> String)? = null
) {
    // Per-share state — plain remember (not Bundle-saveable): the modal
    // resets these each time it opens, and enums/ImageBitmap aren't Bundle-
    // saveable by default (crash on onSaveInstanceState).
    // v3xx — 3:4 (CLASSIC) is the default aspect; the aspect tool toggles.
    var aspect by remember { mutableStateOf(ShareCardAspect.CLASSIC) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var customText by rememberSaveable { mutableStateOf("") }
    var polaroidCaption by rememberSaveable { mutableStateOf("") }
    // v229d — seeded from [initialStyle] / [initialClassicSignature] so the
    // Share Hub can open the sheet on the picked design.
    var styleIdx by remember { mutableIntStateOf(initialStyle) }
    var classicDesign by remember { mutableStateOf(initialClassicSignature) }
    // Inline edit mode (Paper) — per-share only; resets when the sheet closes.
    // Plain remember: edits are a per-share tweak (not Bundle-saveable) and the
    // modal resets them each time, so they should not survive a rotation.
    var editMode by remember { mutableStateOf(false) }
    // v323 — text editing for the quick fact is EXPLICIT: tap the box to
    // select/move it, then the "Edit text" tool arms the field. Exiting edit
    // mode or selecting another element always drops back to select mode.
    var factEditMode by remember { mutableStateOf(false) }
    // v323 — explicit tone pick: -1 = automatic per-category rotation, else
    // an index into the unlocked [curatedTones] (offered by the Tone tool).
    var toneIndex by remember { mutableIntStateOf(-1) }
    // v324 — the Adjust tool: whole-card saturation/contrast (1f = neutral).
    var saturation by remember { mutableStateOf(1f) }
    var contrast by remember { mutableStateOf(1f) }
    // v323 — while editing, the sheet must NOT be dismissible (a swipe / back
    // / scrim tap would discard the user's card edits): confirmValueChange
    // blocks every move away from Expanded until edit mode ends.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { if (editMode) it == androidx.compose.material3.SheetValue.Expanded else true }
    )
    // v3xx — selection model: NOTHING selected when editing starts; the user
    // taps a thing on the card to select it (see ArrangeableCard).
    var selectedResizeTarget by remember { mutableStateOf(ShareCardResizeTarget.NONE) }
    // Which tool's small overlay panel is open under the toolbar (null = none).
    var toolOpen by remember { mutableStateOf<String?>(null) }
    var bodyScale by remember { mutableStateOf(1f) }
    // v330 — every STYLE keeps its OWN move/position edits: dragging the
    // title on Paper must not shove the title on Vinyl too. Keyed by style;
    // missing entries fall back to a clean ShareCardMove.
    var movesByStyle by remember { mutableStateOf<Map<ShareCardStyle, ShareCardMove>>(emptyMap()) }
    var editedTitle by remember { mutableStateOf<String?>(null) }
    var editedFact by remember { mutableStateOf<String?>(null) }
    // Restore saved edits for this topic on first composition
    androidx.compose.runtime.LaunchedEffect(topicName) {
        val saved = AppPreferences.loadShareCardEdits(context, topicName)
        if (saved != null) {
            fun parseMove(o: org.json.JSONObject) = ShareCardMove(
                titleDx = o.optDouble("titleDx", 0.0).toFloat(),
                titleDy = o.optDouble("titleDy", 0.0).toFloat(),
                factDx = o.optDouble("factDx", 0.0).toFloat(),
                factDy = o.optDouble("factDy", 0.0).toFloat(),
                metaDx = o.optDouble("metaDx", 0.0).toFloat(),
                metaDy = o.optDouble("metaDy", 0.0).toFloat(),
                badgeDx = o.optDouble("badgeDx", 0.0).toFloat(),
                badgeDy = o.optDouble("badgeDy", 0.0).toFloat(),
                titleWidthFrac = o.optDouble("titleWidthFrac", 1.0).toFloat(),
                titleHeightFrac = o.optDouble("titleHeightFrac", 1.0).toFloat(),
                factWidthFrac = o.optDouble("factWidthFrac", 1.0).toFloat(),
                factHeightFrac = o.optDouble("factHeightFrac", 1.0).toFloat(),
                titleScale = o.optDouble("titleScale", 1.0).toFloat()
            )
            val loaded = mutableMapOf<ShareCardStyle, ShareCardMove>()
            saved.optJSONObject("moves")?.let { mv ->
                ShareCardStyle.entries.forEach { st ->
                    mv.optJSONObject(st.name)?.let { o -> loaded[st] = parseMove(o) }
                }
            }
            // Legacy (pre-per-style) saves: one flat move applied to every
            // style, reproducing the old shared-move behaviour.
            if (loaded.isEmpty() && saved.has("titleDx")) {
                val legacy = parseMove(saved)
                ShareCardStyle.entries.forEach { loaded[it] = legacy }
            }
            movesByStyle = loaded
            bodyScale = saved.optDouble("bodyScale", 1.0).toFloat()
            editedTitle = saved.optString("editedTitle", null)?.takeIf { it.isNotBlank() }
            editedFact = saved.optString("editedFact", null)?.takeIf { it.isNotBlank() }
        }
    }
    val sharer = AppPreferences.getDisplayName(context).ifBlank { "" }
    // Photo picker state — only used for Collage style
    var userPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
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
    // v328 — BOOK share cards: Chapter progress + Chapter review contents,
    // offered only when the caller handed the topic's chapters. Progress
    // reads the BookNotes reading-progress pref (highest chapter read); the
    // review tags the user's own text with a chosen chapter chip.
    val hasBookChapters = bookChapters.isNotEmpty()
    val chaptersRead = if (hasBookChapters)
        (AppPreferences.bookReadingProgressState[topicName] ?: 0).coerceIn(0, bookChapters.size)
    else 0
    // Chapter review state: which chapter the review is about (defaults to
    // the current reading spot, or chapter 1).
    var reviewChapterNumber by remember { mutableIntStateOf(0) }
    val effectiveReviewChapter = when {
        !hasBookChapters -> null
        reviewChapterNumber > 0 && bookChapters.any { it.number == reviewChapterNumber } -> reviewChapterNumber
        chaptersRead > 0 -> chaptersRead
        else -> bookChapters.firstOrNull()?.number
    }
    val progressContent = ShareCardContent(
        id = "chapter_progress",
        label = "Reading progress",
        text = if (chaptersRead > 0) "I'm $chaptersRead of ${bookChapters.size} chapters in"
               else "${bookChapters.size} chapters to discover"
    )
    val reviewContent = ShareCardContent(
        id = "chapter_review",
        label = "Chapter review",
        text = ""
    )
    val available = if (isQuotes) listOf(quote)
        else listOf(quick, noFact) + savedSources + listOf(custom) +
            if (hasBookChapters) listOf(progressContent, reviewContent) else emptyList()
    val defaultId = if (isQuotes) quote.id else savedSources.firstOrNull { it.id == "quote" }?.id ?: quick.id
    val activeId = selectedId ?: defaultId
    val activeSource = when (activeId) {
        CUSTOM_FACT_ID -> custom.copy(text = customText.ifBlank { "Add your own fact about this discovery…" })
        NO_FACT_ID -> noFact
        "chapter_progress" -> progressContent
        "chapter_review" -> {
            val chip = effectiveReviewChapter?.let { num ->
                val ch = bookChapters.firstOrNull { c -> c.number == num }
                "CH $num" + (ch?.title?.takeIf { t -> t.isNotBlank() }?.let { t -> " · $t" } ?: "")
            }
            reviewContent.copy(
                text = buildString {
                    chip?.let { append(it).append("\n") }
                    append(customText.ifBlank { "Write your review of this chapter…" })
                }
            )
        }
        else -> available.firstOrNull { it.id == activeId } ?: quick
    }
    // v329 — when Reading progress is the active content the card draws a
    // VISUAL chapter widget (bar + caption) instead of the prose text; the
    // value comes from the live BookNotes pref so the picker updates the card.
    val progressForCard = if (activeId == "chapter_progress" && hasBookChapters)
        ChapterProgressUi(chaptersRead, bookChapters.size)
    else null

    val styles = availableStylesForFamily(categoryFamily, topicName)
    val safeIdx = styleIdx.coerceIn(0, styles.lastIndex)
    val currentStyle = styles[safeIdx]
    // v330 — the CURRENT style's move (each style keeps its own offsets).
    val move = movesByStyle[currentStyle] ?: ShareCardMove()
    fun updateMove(m: ShareCardMove) {
        movesByStyle = movesByStyle + (currentStyle to m)
    }
    // Inline-edit / Customise helpers
    val sourceOptions = available.filter { !isQuotes || it.id != QUICK_FACT_ID }
    // The transparent quick-fact typing field must lay out with the SAME
    // metrics as the card's fact text or the caret drifts off the visible
    // text. Facts across styles are Lora serif ~9–12sp with ~1.5× line
    // height, scaled by the fact-size dropdown (bodyScale); the old
    // 14sp/20sp bodyMedium wrapped whole lines away from the card.
    val factFieldStyle = TextStyle(
        fontFamily = LoraFontFamily,
        fontSize = 11.sp * bodyScale,
        lineHeight = 16.5.sp * bodyScale,
        color = Color.Transparent
    )
    // Hoisted pager state so the Customise panel can switch style via its chips.
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = safeIdx.coerceIn(0, styles.lastIndex)) { styles.size }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Satisfying haptics: confirm on Save/Share, light ticks on Reset/Done.
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    fun setStyle(i: Int) {
        styleIdx = i.coerceIn(0, styles.lastIndex)
        if (styles.size > 1) scope.launch { pagerState.animateScrollToPage(styleIdx) }
    }

    // v325 — persist the CURRENT edits (per-style moves + text + scale) so an
    // accidental exit can be resumed by reopening the sheet. Runs on
    // Save/Share AND on dismissal; only LEAVING the Topic Reveal screen
    // clears the topic's edits (see TopicRevealScreen).
    fun persistEdits() {
        val edit = org.json.JSONObject()
        val movesObj = org.json.JSONObject()
        movesByStyle.forEach { (style, m) ->
            val o = org.json.JSONObject().apply {
                put("titleDx", m.titleDx); put("titleDy", m.titleDy)
                put("factDx", m.factDx); put("factDy", m.factDy)
                put("metaDx", m.metaDx); put("metaDy", m.metaDy)
                put("badgeDx", m.badgeDx); put("badgeDy", m.badgeDy)
                put("titleWidthFrac", m.titleWidthFrac); put("titleHeightFrac", m.titleHeightFrac)
                put("factWidthFrac", m.factWidthFrac); put("factHeightFrac", m.factHeightFrac)
                put("titleScale", m.titleScale)
            }
            movesObj.put(style.name, o)
        }
        if (movesObj.length() > 0) edit.put("moves", movesObj)
        edit.put("bodyScale", bodyScale)
        if (editedTitle != null) edit.put("editedTitle", editedTitle)
        if (editedFact != null) edit.put("editedFact", editedFact)
        AppPreferences.saveShareCardEdits(context, topicName, edit)
    }

    // onDismissRequest also respects edit mode: the back button and scrim taps
    // are ignored while editing (the swipe path is blocked via the sheet
    // state's confirmValueChange above). v325 — dismissal persists the edits
    // first, so exiting by mistake resumes where you left off.
    ModalBottomSheet(onDismissRequest = { if (!editMode) { persistEdits(); onDismiss() } }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        // v325 — BACK cancels the Customise editor first, then (on a second
        // press) exits the sheet — it was previously swallowed while editing.
        BackHandler(enabled = editMode) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            editMode = false
            selectedResizeTarget = ShareCardResizeTarget.NONE
            factEditMode = false
            toolOpen = null
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // v316b — editing owns the whole sheet: the title (and the style
            // label / dots below) hide while edit mode is on so the card +
            // controls have plenty of room.
            if (!editMode) {
                Text("Share this topic", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
            }

            // The card carousel IS the preview — no separate static card
            val pw = 280.dp
            Box {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (styles.size > 1) {
                    // Style label — hidden while editing (see the header above).
                    if (!editMode) {
                        Text(currentStyle.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Swipeable card carousel — the card IS the preview
                    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) { styleIdx = pagerState.currentPage }
                    // v330 — swiping between styles now WORKS while editing:
                    // it is only locked while an element is selected (a drag
                    // there belongs to the move grip / typing field, not the
                    // pager). Tap outside to deselect, then swipe freely.
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 36.dp),
                        pageSpacing = 16.dp,
                        userScrollEnabled = !editMode || selectedResizeTarget == ShareCardResizeTarget.NONE,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        // v330 — each style renders with ITS OWN saved move,
                        // so swiping shows every card's individual layout.
                        val pageMove = movesByStyle[styles[page]] ?: ShareCardMove()
                        val isCenter = page == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .width(pw)
                                .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
                                .clip(RoundedCornerShape(6.dp))
                                .graphicsLayer {
                                    val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                                    val scale = 1f - 0.08f * kotlin.math.abs(pageOffset)
                                    scaleX = scale; scaleY = scale
                                    alpha = 1f - 0.25f * kotlin.math.abs(pageOffset)
                                    shadowElevation = 0f
                                }
                        ) {
                            ArrangeableCard(
                                active = page == pagerState.currentPage,
                                editMode = editMode && page == pagerState.currentPage,
                                quoteMode = isQuotes,
                                editFact = if (progressForCard != null) progressContent.text else editedFact ?: (if (activeId == CUSTOM_FACT_ID) customText else activeSource.text),
                                onFactChange = { editedFact = it },
                                factEditMode = factEditMode,
                                onFactEditModeChange = { factEditMode = it },
                                onToggleEdit = {
                                    editMode = !editMode
                                    toolOpen = null
                                    if (!editMode) {
                                        selectedResizeTarget = ShareCardResizeTarget.NONE
                                        factEditMode = false
                                    }
                                },
                                onSelectResizeTarget = { selectedResizeTarget = it },
                                selectedResizeTarget = selectedResizeTarget,
                                move = pageMove,
                                onMove = { movesByStyle = movesByStyle + (styles[page] to it) },
                                factFieldStyle = factFieldStyle
                            ) { cb ->
                                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = styles[page], ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, onPhotoTap = { photoPickerLauncher.launch("image/*") }, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = editedFact, move = pageMove, chapterProgress = progressForCard, callbacks = cb)
                            }
                        }
                    }
                    // Style dots — hidden while editing.
                    if (!editMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            styles.forEachIndexed { i, _ ->
                                Box(Modifier.size(if (i == pagerState.currentPage) 7.dp else 5.dp).background(
                                    if (i == pagerState.currentPage) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f), CircleShape
                                ))
                            }
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
                            active = true,
                            editMode = editMode,
                            quoteMode = isQuotes,
                            editFact = editedFact ?: (if (activeId == CUSTOM_FACT_ID) customText else activeSource.text),
                            onFactChange = { editedFact = it },
                            factEditMode = factEditMode,
                            onFactEditModeChange = { factEditMode = it },
                            onToggleEdit = {
                                editMode = !editMode
                                toolOpen = null
                                if (!editMode) {
                                    selectedResizeTarget = ShareCardResizeTarget.NONE
                                    factEditMode = false
                                }
                            },
                            onSelectResizeTarget = { selectedResizeTarget = it },
                            selectedResizeTarget = selectedResizeTarget,
                            move = move,
                            onMove = { updateMove(it) },
                            factFieldStyle = factFieldStyle
                        ) { cb ->
                            TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, onPhotoTap = { photoPickerLauncher.launch("image/*") }, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = editedFact, move = move, chapterProgress = progressForCard, callbacks = cb)
                        }
                    }
                }
            }

                // Floating Customise button — over the card, bottom-right.
                // v330 — the edit-mode actions (content selector + Reset +
                // Done) live in the bottom action bar where Save/Share/Text
                // sit; nothing floats over the card while editing anymore.
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        editMode = !editMode
                        toolOpen = null
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 6.dp)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CurioIcon(name = CurioIcons.Tune, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                        Text("Customise", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                }
            }

            // Edit hint — before editing: "hold"; while editing with nothing
            // selected: prompts the new tap-to-select model.
            if (!editMode) {
                Text("Hold to edit",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            } else if (selectedResizeTarget == ShareCardResizeTarget.NONE) {
                Text("Tap a thing to select · swipe for another design",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }

            // ── v3xx EDIT TOOLBAR — circular icon pills only. Each tool
            // opens ONE small overlay panel; the SELECTED element decides
            // what the size / box / font / align / format tools act on. ──
            if (editMode) {
                val sel = selectedResizeTarget
                val isTitle = sel == ShareCardResizeTarget.TITLE
                val isFact = sel == ShareCardResizeTarget.FACT
                val isSizable = isTitle || isFact
                val selName = when (sel) {
                    ShareCardResizeTarget.TITLE -> "Title"
                    ShareCardResizeTarget.FACT -> "Quick fact"
                    ShareCardResizeTarget.META -> "Info row"
                    ShareCardResizeTarget.BADGE -> "Category chip"
                    ShareCardResizeTarget.NONE -> "Nothing selected"
                }
                val elementFont = when (sel) {
                    ShareCardResizeTarget.TITLE -> move.titleFont
                    ShareCardResizeTarget.FACT -> move.factFont
                    ShareCardResizeTarget.META -> move.metaFont
                    ShareCardResizeTarget.BADGE -> move.badgeFont
                    ShareCardResizeTarget.NONE -> null
                }
                val elementAlign = when (sel) {
                    ShareCardResizeTarget.TITLE -> move.titleAlign
                    ShareCardResizeTarget.FACT -> move.factAlign
                    else -> null
                }
                val elementBold = when (sel) {
                    ShareCardResizeTarget.TITLE -> move.titleBold
                    ShareCardResizeTarget.FACT -> move.factBold
                    ShareCardResizeTarget.META -> move.metaBold
                    ShareCardResizeTarget.BADGE -> move.badgeBold
                    ShareCardResizeTarget.NONE -> false
                }
                val elementItalic = when (sel) {
                    ShareCardResizeTarget.TITLE -> move.titleItalic
                    ShareCardResizeTarget.FACT -> move.factItalic
                    ShareCardResizeTarget.META -> move.metaItalic
                    ShareCardResizeTarget.BADGE -> move.badgeItalic
                    ShareCardResizeTarget.NONE -> false
                }
                val setElementFont: (FontFamily?) -> Unit = { fam ->
                    updateMove(when (sel) {
                        ShareCardResizeTarget.TITLE -> move.copy(titleFont = fam)
                        ShareCardResizeTarget.FACT -> move.copy(factFont = fam)
                        ShareCardResizeTarget.META -> move.copy(metaFont = fam)
                        ShareCardResizeTarget.BADGE -> move.copy(badgeFont = fam)
                        ShareCardResizeTarget.NONE -> move
                    })
                }
                val setElementAlign: (TextAlign?) -> Unit = { a ->
                    updateMove(when (sel) {
                        ShareCardResizeTarget.TITLE -> move.copy(titleAlign = a)
                        ShareCardResizeTarget.FACT -> move.copy(factAlign = a)
                        else -> move
                    })
                }
                val setElementBold: (Boolean) -> Unit = { v ->
                    updateMove(when (sel) {
                        ShareCardResizeTarget.TITLE -> move.copy(titleBold = v)
                        ShareCardResizeTarget.FACT -> move.copy(factBold = v)
                        ShareCardResizeTarget.META -> move.copy(metaBold = v)
                        ShareCardResizeTarget.BADGE -> move.copy(badgeBold = v)
                        ShareCardResizeTarget.NONE -> move
                    })
                }
                val setElementItalic: (Boolean) -> Unit = { v ->
                    updateMove(when (sel) {
                        ShareCardResizeTarget.TITLE -> move.copy(titleItalic = v)
                        ShareCardResizeTarget.FACT -> move.copy(factItalic = v)
                        ShareCardResizeTarget.META -> move.copy(metaItalic = v)
                        ShareCardResizeTarget.BADGE -> move.copy(badgeItalic = v)
                        ShareCardResizeTarget.NONE -> move
                    })
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    // ── Tool pills row (scrollable) ────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // v330 — Edit-text moved back into the toolbar row:
                        // the floating cluster over the card is gone (the
                        // bottom bar now owns Reset + Done + the content
                        // selector). Only the quick fact can be typed, so the
                        // pill appears just when the fact is selected.
                        if (isFact && progressForCard == null) {
                            EditToolPill(
                                glyph = CurioIcons.Edit,
                                description = "Edit text",
                                active = factEditMode,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    factEditMode = !factEditMode
                                    if (!factEditMode) focusManager.clearFocus()
                                }
                            )
                        }
                        EditToolPill(
                            glyph = CurioIcons.AutoAwesome,
                            description = "Design",
                            active = toolOpen == "style",
                            onClick = { toolOpen = if (toolOpen == "style") null else "style" }
                        )
                        // Aspect — v3xx: NO options list, tapping toggles
                        // between 3:4 and 9:16 instantly.
                        EditToolPill(
                            glyph = CurioIcons.AspectRatio,
                            description = "Aspect 3:4 \u2194 9:16",
                            active = false,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                aspect = if (aspect == ShareCardAspect.CLASSIC) ShareCardAspect.PORTRAIT else ShareCardAspect.CLASSIC
                                toolOpen = null
                            }
                        )
                        EditToolPill(
                            glyph = "text_increase",
                            description = "Text size",
                            active = toolOpen == "size",
                            onClick = { toolOpen = if (toolOpen == "size") null else "size" }
                        )
                        EditToolPill(
                            glyph = CurioIcons.Crop,
                            description = "Box size",
                            active = toolOpen == "box",
                            onClick = { toolOpen = if (toolOpen == "box") null else "box" }
                        )
                        EditToolPill(
                            glyph = "title",
                            description = "Font",
                            active = toolOpen == "font",
                            onClick = { toolOpen = if (toolOpen == "font") null else "font" }
                        )
                        EditToolPill(
                            glyph = CurioIcons.Palette,
                            description = "Card tone",
                            active = toolOpen == "tone",
                            onClick = { toolOpen = if (toolOpen == "tone") null else "tone" }
                        )
                        EditToolPill(
                            glyph = CurioIcons.Contrast,
                            description = "Saturation / contrast",
                            active = toolOpen == "adjust",
                            onClick = { toolOpen = if (toolOpen == "adjust") null else "adjust" }
                        )
                        EditToolPill(
                            glyph = "notes",
                            description = "Alignment",
                            active = toolOpen == "align",
                            onClick = { toolOpen = if (toolOpen == "align") null else "align" }
                        )
                        EditToolPill(
                            glyph = CurioIcons.FormatBold,
                            description = "Bold / italic",
                            active = toolOpen == "format",
                            onClick = { toolOpen = if (toolOpen == "format") null else "format" }
                        )
                        EditToolPill(
                            glyph = CurioIcons.Edit,
                            description = "Content (source, custom fact, photo)",
                            active = toolOpen == "source",
                            onClick = { toolOpen = if (toolOpen == "source") null else "source" }
                        )
                        // v330 — Reset + Done live in the bottom action bar
                        // while editing (see below); the floating cluster over
                        // the card is gone.
                    }

                    // ── One small overlay for the open tool ────────────
                    when (toolOpen) {
                        "style" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Design", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                            ) {
                                // v323 — panels STAY OPEN while picking options; tap
                                // the tool icon again to close.
                                styles.forEachIndexed { i, st ->
                                    Pill(st.label, CurioIcons.AutoAwesome, st == currentStyle) { setStyle(i) }
                                }
                            }
                            if (currentStyle == ShareCardStyle.SIGNATURE) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Pill("Current", CurioIcons.AutoAwesome, !classicDesign) { classicDesign = false }
                                    Pill("Classic", CurioIcons.AutoAwesome, classicDesign) { classicDesign = true }
                                }
                            }
                        }
                        "size" -> if (isSizable) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("$selName size", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                                ) {
                                    val opts = if (isTitle) listOf(0.75f, 0.9f, 1.0f, 1.15f, 1.3f, 1.5f)
                                                 else listOf(0.5f, 0.7f, 0.85f, 1.0f, 1.15f, 1.3f, 1.5f, 1.8f)
                                    val cur = if (isTitle) move.titleScale else bodyScale
                                    opts.forEach { s ->
                                        val label = (Math.round(s * 100f) / 100f).toString() + "\u00d7"
                                        Pill(label, "text_increase", s == cur) {
                                            if (isTitle) updateMove(move.copy(titleScale = s)) else bodyScale = s
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Tap a thing on the card first", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        "box" -> if (isSizable) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("$selName box", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (isTitle) {
                                    SizeSliderColumn("Title width", move.titleWidthFrac, { updateMove(move.copy(titleWidthFrac = it)) }, 0.3f..1f, steps = 69, modifier = Modifier.fillMaxWidth())
                                    SizeSliderColumn("Title height", move.titleHeightFrac, { updateMove(move.copy(titleHeightFrac = it)) }, 0.35f..2.5f, steps = 26, modifier = Modifier.fillMaxWidth())
                                } else {
                                    SizeSliderColumn("Fact width", move.factWidthFrac, { updateMove(move.copy(factWidthFrac = it)) }, 0.3f..1f, steps = 69, modifier = Modifier.fillMaxWidth())
                                    SizeSliderColumn("Fact height", move.factHeightFrac, { updateMove(move.copy(factHeightFrac = it)) }, 0.35f..2.5f, steps = 26, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        } else {
                            Text("Tap a thing on the card first", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        "font" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Font \u2014 $selName", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                            ) {
                                shareFonts.forEach { f ->
                                    Pill(f.label, "title", elementFont == f.family) { setElementFont(f.family) }
                                }
                            }
                        }
                        // v323 — the Tone tool: every UNLOCKED tone as a swatch
                        // (level rewards visibly buy new card looks). Auto keeps
                        // the per-category rotation. Panels stay open while
                        // switching (tap the tool icon to close).
                        "tone" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tone \u2014 level unlocks", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                            ) {
                                ToneSwatch(
                                    name = "Auto",
                                    bg = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ring = MaterialTheme.colorScheme.secondary,
                                    selected = toneIndex < 0
                                ) { toneIndex = -1 }
                                val toneCount = unlockedToneCount(CurioQuests.levelForXp(CurioQuests.xpState)).coerceIn(1, curatedTones.size)
                                curatedTones.take(toneCount).forEachIndexed { i, p ->
                                    ToneSwatch(
                                        name = p.name,
                                        bg = p.bgBase,
                                        ring = p.accent,
                                        selected = toneIndex == i
                                    ) { toneIndex = i }
                                }
                            }
                        }
                        // v324 — the Adjust tool: saturation + contrast sliders
                        // over the WHOLE card (replaces the removed Deepen
                        // experiment's heavy scenes with a clean edit). Applies
                        // to every style and the exported image.
                        "adjust" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdjustSliderRow("Saturation", saturation, 0.5f..1.5f) { saturation = it }
                            AdjustSliderRow("Contrast", contrast, 0.5f..1.5f) { contrast = it }
                            Text(
                                "Fine-tune the card's look \u2014 applies to every style and the saved image.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "align" -> if (sel == ShareCardResizeTarget.TITLE || sel == ShareCardResizeTarget.FACT) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("$selName alignment", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Pill("Left", "notes", elementAlign == null) { setElementAlign(null) }
                                    Pill("Center", "notes", elementAlign == TextAlign.Center) { setElementAlign(TextAlign.Center) }
                                    Pill("Right", "notes", elementAlign == TextAlign.End) { setElementAlign(TextAlign.End) }
                                    Pill("Justify", "notes", elementAlign == TextAlign.Justify) { setElementAlign(TextAlign.Justify) }
                                }
                            }
                        } else {
                            Text("Tap the title or quick fact first", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        "format" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Format \u2014 $selName", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill("Bold", CurioIcons.FormatBold, elementBold) {
                                    setElementBold(!elementBold)
                                }
                                Pill("Italic", CurioIcons.FormatItalic, elementItalic) {
                                    setElementItalic(!elementItalic)
                                }
                            }
                        }
                        "source" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Content", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            // v330 — the content pills (Quick fact / No fact /
                            // sources / + Custom fact) moved into the bottom
                            // bar while editing; this panel keeps the extras
                            // only (custom text, book chapter, photo, song).
                            // v328 — BOOK chapter contents: pick the chapter
                            // (CH 1..N) that the progress/review refers to.
                            // For Reading progress the pick WRITES the
                            // BookNotes pref (progress only moves forward), so
                            // the card and the notes reader stay in sync; for
                            // Chapter review it tags the review text.
                            if (hasBookChapters &&
                                (activeId == "chapter_progress" || activeId == "chapter_review")
                            ) {
                                val chosen = if (activeId == "chapter_progress") chaptersRead
                                             else effectiveReviewChapter ?: 0
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                                ) {
                                    Text(
                                        "Chapter",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    bookChapters.forEach { ch ->
                                        Pill(
                                            "CH ${ch.number}",
                                            CurioIcons.MenuBook,
                                            chosen == ch.number
                                        ) {
                                            if (activeId == "chapter_progress") {
                                                AppPreferences.setBookReadingProgress(context, topicName, ch.number)
                                            } else {
                                                reviewChapterNumber = ch.number
                                            }
                                        }
                                    }
                                }
                                Text(
                                    if (activeId == "chapter_progress")
                                        "Also updates your Book Notes reading progress for this book."
                                    else "Your review is tagged \"CH ${effectiveReviewChapter ?: ""}\" on the card.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") {
                                OutlinedTextField(
                                    customText,
                                    { customText = it },
                                    placeholder = {
                                        Text(
                                            if (activeId == "chapter_review") "Write your review of this chapter…"
                                            else "Your custom fact",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (currentStyle == ShareCardStyle.COLLAGE) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(onClick = { photoPickerLauncher.launch("image/*") }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(40.dp)) {
                                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            CurioIcon(name = CurioIcons.PhotoLibrary, tint = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                                            Text(if (userPhoto != null) "Change" else "Photo", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    OutlinedTextField(value = polaroidCaption, onValueChange = { polaroidCaption = it.take(36) }, placeholder = { Text(if (sharer.isNotBlank()) "$sharer \u00b7 via Curio" else "via Curio", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) }, singleLine = true, textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(50), modifier = Modifier.weight(1f))
                                }
                            }
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
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            val eh = pw * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()
            if (editMode) {
                // ── v330 — edit-mode bottom bar ─────────────────────────
                // While customising, this row REPLACES Save/Share/Text: the
                // content selector (quick fact / no fact / saved sources /
                // + custom fact) plus Reset and Done. Tapping Done brings
                // the save/share actions straight back.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        sourceOptions.forEach { opt ->
                            Pill(
                                opt.label + (opt.rating?.takeIf { r -> r > 0 }?.let { " · " + "\u2605".repeat(it) } ?: ""),
                                if (opt.id == CUSTOM_FACT_ID) CurioIcons.Add else CurioIcons.FormatText,
                                opt.id == activeId
                            ) { selectedId = opt.id }
                        }
                    }
                    EditToolPill(
                        glyph = CurioIcons.Refresh,
                        description = "Reset all edits",
                        active = false,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            editedTitle = null; editedFact = null; bodyScale = 1f
                            saturation = 1f; contrast = 1f
                            selectedResizeTarget = ShareCardResizeTarget.NONE
                            factEditMode = false
                            movesByStyle = emptyMap()
                            toolOpen = null
                        }
                    )
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            editMode = false
                            toolOpen = null
                            selectedResizeTarget = ShareCardResizeTarget.NONE
                            factEditMode = false
                            focusManager.clearFocus()
                        },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurioIcon(name = CurioIcons.Check, tint = MaterialTheme.colorScheme.onPrimary, size = 14.dp)
                            Text("Done", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            } else {
                // Save + Share + Share as text — all in one row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // Save button
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                            TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = editedFact, move = move, chapterProgress = progressForCard)
                        }, saveToGallery = true)
                        persistEdits()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Row(Modifier.padding(horizontal = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CurioIcon(name = CurioIcons.Download, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 16.dp)
                        Text("Save", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }
                // Share button
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, card = {
                        TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = activeSource.text, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = editedFact, move = move, chapterProgress = progressForCard)
                    })
                        persistEdits()
                        onDismiss()
                }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary), modifier = Modifier.weight(1f).height(44.dp)) {
                    Text("Share", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
                // Share as text button
                Surface(onClick = {
                    val text = shareAsText?.invoke() ?: buildString {
                        append(topicName).append("\n")
                        append(activeSource.text).append("\n\n")
                        append(categoryName).append(" · via Curio — Stay curious")
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, topicName)
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                    onDismiss()
                }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(44.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CurioIcon(name = CurioIcons.Share, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
                        Text("Text", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun CustomiseLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * A draggable grip the user drags to reposition a box (title / quick fact /
 * meta / category badge) on the preview card. v316b — ONE uniform move icon
 * (a grip) in a darker coffee circle replaces the old per-box T / F / M / B
 * letter handles and their separate colors. Each drag delta is converted
 * from px to dp and fed back via onDelta so both the live card and the
 * export move together.
 */
@Composable
private fun BoxScope.MoveHandle(
    x: Dp, y: Dp,
    onDelta: (dx: Float, dy: Float) -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val latestDelta by rememberUpdatedState(onDelta)
    // v325 — while dragging, the knob shrinks and fades so the text it moves
    // stays readable underneath (it used to sit right on the glyphs).
    var dragging by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(22.dp)
            .graphicsLayer {
                shadowElevation = 2.dp.toPx(); shape = CircleShape; clip = false
                alpha = if (dragging) 0.55f else 1f
            }
            .background(CoffeeChromeDeep, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { _, dragAmount ->
                    val dx = with(density) { dragAmount.x.toDp().value }
                    val dy = with(density) { dragAmount.y.toDp().value }
                    latestDelta(dx, dy)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(
            name = CurioIcons.DragHandle,
            contentDescription = "Move",
            tint = Color.White,
            size = if (dragging) 11.dp else 13.dp
        )
    }
}

/**
 * v323 — one tone swatch in the editor's Tone tool: a circle in the tone's
 * base fill with its accent ring; the picked tone wears a bold ring + check.
 * Only unlocked tones are offered (the toolbar computes [unlockedToneCount]).
 */
@Composable
private fun ToneSwatch(
    name: String,
    bg: Color,
    ring: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(54.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = bg,
            shadowElevation = if (selected) 3.dp else 0.dp,
            modifier = Modifier
                .size(38.dp)
                .border(if (selected) 2.5.dp else 1.dp, if (selected) ring else ring.copy(alpha = 0.5f), CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (selected) {
                    CurioIcon(
                        name = CurioIcons.Check,
                        contentDescription = null,
                        size = 16.dp,
                        tint = if (bg.luminance() > 0.5f) ring else Color.White
                    )
                }
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * v324 — one labeled slider row for the Adjust tool (Saturation / Contrast).
 * Range 0.5–1.5, neutral at 1.0; the % readout shows the offset.
 */
@Composable
private fun AdjustSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(92.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${(value * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(46.dp)
        )
    }
}

/**
 * v324 — combined saturation × contrast 4×5 color matrix for the Adjust
 * tool. Saturation is luma-weighted; contrast pivots around 0.5. The
 * matrices multiply so the filter applies saturation first, then contrast.
 */
private fun adjustColorMatrix(saturation: Float, contrast: Float): ColorMatrix {
    val inv = 1f - saturation
    val lr = 0.213f * inv
    val lg = 0.715f * inv
    val lb = 0.072f * inv
    val sat = ColorMatrix(floatArrayOf(
        lr + saturation, lg, lb, 0f, 0f,
        lr, lg + saturation, lb, 0f, 0f,
        lr, lg, lb + saturation, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    val t = (1f - contrast) * 0.5f
    val con = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, t,
        0f, contrast, 0f, 0f, t,
        0f, 0f, contrast, 0f, t,
        0f, 0f, 0f, 1f, 0f
    ))
    con.timesAssign(sat)
    return con
}

/** v3xx — one circular icon tool button in the edit toolbar (icons only). */
@Composable
private fun EditToolPill(
    glyph: String,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = glyph,
                contentDescription = description,
                size = 19.dp,
                tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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

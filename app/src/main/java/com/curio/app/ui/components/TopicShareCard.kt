package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Switch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
import androidx.compose.ui.layout.layout
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
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.text.style.TextDecoration
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
import com.curio.app.data.TextSpan
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
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.toHsl
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
import androidx.compose.foundation.BorderStroke
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

/** v370 — quick-fact TEXT LAYOUT modes (offered by the Format tool when the
 *  fact box is selected, on EVERY card style). */
enum class ShareCardFactFormat(val label: String) {
    STANDARD("Standard"),
    CONDENSED("Condensed"),
    BOOK("Book page"),
    EDITORIAL("Editorial")
}

/** v370 — EDITORIAL drop-cap variant (only read when the fact layout is
 *  [ShareCardFactFormat.EDITORIAL]). */
enum class ShareCardFactDropCap(val label: String) {
    NONE("No cap"),
    LETTER("Big first letter"),
    WORD("Big first word")
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
    val name: String = "",
    // v369 — level gate for the Tone tool: null = ALWAYS available, an int
    // = the player level that unlocks it (matches LevelRewards PALETTE
    // rewards). The always-available set is the 4 base tones + the new dark
    // variants; premium tones unlock at their reward level.
    val unlockLevel: Int? = null
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
        name = "Midnight", unlockLevel = 2
    ),
    // Forest (Level 8) — mossy pine on soft mushroom
    ShareCardPalette(
        bgBase = Color(0xFFEDF1E8), bgLight = Color(0xFFF6F8F2), bgMid = Color(0xFFD9E2D0),
        accent = Color(0xFF4E6E4A), accentDark = Color(0xFF33512F),
        ink = Color(0xFF1B2418), inkFaint = Color(0xFF64755E),
        name = "Forest", unlockLevel = 8
    ),
    // Lavender (Level 15) — lilac on cool grey-cream
    ShareCardPalette(
        bgBase = Color(0xFFF3EFF7), bgLight = Color(0xFFFAF7FC), bgMid = Color(0xFFE3DBEF),
        accent = Color(0xFF8B74B8), accentDark = Color(0xFF664F93),
        ink = Color(0xFF221B31), inkFaint = Color(0xFF7C6E96),
        name = "Lavender", unlockLevel = 15
    ),
    // Ember (Level 30) — charcoal with a molten orange accent
    ShareCardPalette(
        bgBase = Color(0xFF26211D), bgLight = Color(0xFF332C26), bgMid = Color(0xFF1C1815),
        accent = Color(0xFFE0853F), accentDark = Color(0xFFB4602A),
        ink = Color(0xFFFAF1E8), inkFaint = Color(0xFFA08E7E),
        name = "Ember", unlockLevel = 30
    ),
    // v323 — six more premium tones unlocked by higher levels (see
    // [LevelRewards]): Ocean (12), Rose Gold (18), Moss (25), Storm (35),
    // Pearl (45), Sunburst (50).
    // Ocean — deep teal-blue on pale foam
    ShareCardPalette(
        bgBase = Color(0xFFEAF2F4), bgLight = Color(0xFFF4FAFB), bgMid = Color(0xFFD3E6EA),
        accent = Color(0xFF2E6E8E), accentDark = Color(0xFF1D4E68),
        ink = Color(0xFF0F242E), inkFaint = Color(0xFF5F7C8A),
        name = "Ocean", unlockLevel = 12
    ),
    // Rose Gold — blush copper on warm cream
    ShareCardPalette(
        bgBase = Color(0xFFFAF1ED), bgLight = Color(0xFFFDF8F5), bgMid = Color(0xFFF0DDD4),
        accent = Color(0xFFC08A7A), accentDark = Color(0xFF9C6353),
        ink = Color(0xFF2B1B16), inkFaint = Color(0xFF8A6F66),
        name = "Rose Gold", unlockLevel = 18
    ),
    // Moss — earthy olive on parchment
    ShareCardPalette(
        bgBase = Color(0xFFF4F1E8), bgLight = Color(0xFFFAF8F2), bgMid = Color(0xFFE2DCC8),
        accent = Color(0xFF7A6B3E), accentDark = Color(0xFF57491F),
        ink = Color(0xFF26210F), inkFaint = Color(0xFF7C7158),
        name = "Moss", unlockLevel = 25
    ),
    // Storm — slate blue-grey on light mist
    ShareCardPalette(
        bgBase = Color(0xFFEDF0F4), bgLight = Color(0xFFF6F8FA), bgMid = Color(0xFFD5DBE3),
        accent = Color(0xFF5A6B80), accentDark = Color(0xFF3D4C5E),
        ink = Color(0xFF181E26), inkFaint = Color(0xFF667180),
        name = "Storm", unlockLevel = 35
    ),
    // Pearl — ivory with a cool silver-lilac accent
    ShareCardPalette(
        bgBase = Color(0xFFF6F2F0), bgLight = Color(0xFFFCFAF9), bgMid = Color(0xFFE5DEDA),
        accent = Color(0xFF9A8FA8), accentDark = Color(0xFF6F647D),
        ink = Color(0xFF241F29), inkFaint = Color(0xFF7C727F),
        name = "Pearl", unlockLevel = 45
    ),
    // Sunburst — warm amber on soft cream
    ShareCardPalette(
        bgBase = Color(0xFFFCF4E2), bgLight = Color(0xFFFFFAF0), bgMid = Color(0xFFF3E2BC),
        accent = Color(0xFFD98E2B), accentDark = Color(0xFFA9641A),
        ink = Color(0xFF2A1D08), inkFaint = Color(0xFF8F7A52),
        name = "Sunburst", unlockLevel = 50
    ),
    // v369 — the dark-variant tones. ALL immediately available (no level
    // gate) so the tone picker has real range from the start: deep,
    // moody canvases with light ink auto-derived for contrast (the ink
    // below is written explicitly but matches the bg luminance so text
    // always reads).
    // Onyx — warm near-black charcoal with an amber glow
    ShareCardPalette(
        bgBase = Color(0xFF191613), bgLight = Color(0xFF242019), bgMid = Color(0xFF131009),
        accent = Color(0xFFC9925E), accentDark = Color(0xFFA06A38),
        ink = Color(0xFFF4EFE8), inkFaint = Color(0xFF9A9186),
        name = "Onyx"
    ),
    // Noir — cool true black with a silver edge
    ShareCardPalette(
        bgBase = Color(0xFF0E0E11), bgLight = Color(0xFF1A1A1F), bgMid = Color(0xFF0A0A0C),
        accent = Color(0xFFC7C9D4), accentDark = Color(0xFF8F93A3),
        ink = Color(0xFFF2F2F5), inkFaint = Color(0xFF8F8F99),
        name = "Noir"
    ),
    // Wine — deep burgundy with a dusty-rose accent
    ShareCardPalette(
        bgBase = Color(0xFF2A1118), bgLight = Color(0xFF3A1821), bgMid = Color(0xFF200B10),
        accent = Color(0xFFE08A94), accentDark = Color(0xFFB35662),
        ink = Color(0xFFFAEFF0), inkFaint = Color(0xFFB09097),
        name = "Wine"
    ),
    // Deep Sea — dark teal-navy with an aqua accent
    ShareCardPalette(
        bgBase = Color(0xFF0E2228), bgLight = Color(0xFF17333C), bgMid = Color(0xFF091A1F),
        accent = Color(0xFF6FC3D0), accentDark = Color(0xFF4A97A8),
        ink = Color(0xFFEEF7F9), inkFaint = Color(0xFF87A9B2),
        name = "Deep Sea"
    ),
    // Cocoa — dark chocolate with a tan accent
    ShareCardPalette(
        bgBase = Color(0xFF241710), bgLight = Color(0xFF32221A), bgMid = Color(0xFF1A100A),
        accent = Color(0xFFD9A87E), accentDark = Color(0xFFB07F54),
        ink = Color(0xFFFAF3EC), inkFaint = Color(0xFFA8927F),
        name = "Cocoa"
    ),
    // Forest Night — dark pine with a sage accent
    ShareCardPalette(
        bgBase = Color(0xFF12201A), bgLight = Color(0xFF1B2F26), bgMid = Color(0xFF0C1612),
        accent = Color(0xFF8FBF9C), accentDark = Color(0xFF649C74),
        ink = Color(0xFFF0F7F1), inkFaint = Color(0xFF8FA89A),
        name = "Forest Night"
    ),
    // Plum — dark violet with a lilac accent
    ShareCardPalette(
        bgBase = Color(0xFF1F1229), bgLight = Color(0xFF2C1B3A), bgMid = Color(0xFF160B1F),
        accent = Color(0xFFC3A1E8), accentDark = Color(0xFF9B73C9),
        ink = Color(0xFFF7F1FC), inkFaint = Color(0xFFA998BE),
        name = "Plum"
    ),
    // Graphite — cool dark grey with a steel-blue accent
    ShareCardPalette(
        bgBase = Color(0xFF17181C), bgLight = Color(0xFF23242A), bgMid = Color(0xFF101114),
        accent = Color(0xFF9FB4C8), accentDark = Color(0xFF7390AA),
        ink = Color(0xFFF1F2F5), inkFaint = Color(0xFF8F96A3),
        name = "Graphite"
    )
)

/** How many tones the player may use at [level]: always-available tones
 *  (null [ShareCardPalette.unlockLevel]) + every premium tone whose reward
 *  level has been reached. */
fun unlockedToneCount(level: Int): Int =
    curatedTones.count { it.unlockLevel == null || it.unlockLevel <= level }

/** The tones offered at the player's CURRENT level (used by the Tone tool
 *  and the automatic per-category rotation alike). */
private fun availableTones(): List<ShareCardPalette> {
    val level = CurioQuests.levelForXp(CurioQuests.xpState)
    return curatedTones.filter { it.unlockLevel == null || it.unlockLevel <= level }
}

private fun paletteFor(accent: Color, toneOverride: Int? = null): ShareCardPalette {
    // v323 — an explicit pick from the Tone tool wins (an index into the
    // player's available tones, so a level-unlocked premium tone CAN be
    // picked by hand).
    // v374 — the AUTOMATIC per-category rotation only ever cycles the
    // always-available BASE tones: a level-locked premium tone must not
    // show up on a card the user never asked for (it only appears when
    // explicitly picked in the Tone tool).
    val all = availableTones()
    if (all.isEmpty()) return curatedTones.first()
    if (toneOverride != null && toneOverride in all.indices) return all[toneOverride]
    // v378 — AUTO matches the tone to the TOPIC: among the light base
    // quartet (Warm Rose / Soft Sage / Golden Ochre / Deep Indigo) pick the
    // one whose accent is CLOSEST to the topic's own category accent, so a
    // book (golden category accent) always Auto-lands on Golden Ochre — the
    // old accent.hashCode() % size rotation could drop a book onto a dark
    // premium tone (Onyx / Noir / Deep Sea …) and read as an unexplained
    // "midnight" default. Explicit premium picks are untouched.
    val light = curatedTones.take(4)
    if (light.isEmpty()) return curatedTones.first()
    val ca = accent.toArgb()
    val cr = (ca shr 16) and 0xFF
    val cg = (ca shr 8) and 0xFF
    val cb = ca and 0xFF
    return light.minByOrNull { p ->
        val pa = p.accent.toArgb()
        val dr = ((pa shr 16) and 0xFF) - cr
        val dg = ((pa shr 8) and 0xFF) - cg
        val db = (pa and 0xFF) - cb
        dr * dr + dg * dg + db * db
    } ?: light.first()
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
enum class ShareCardResizeTarget { NONE, TITLE, FACT, META, BADGE, COVER, FAVTRACKS, POLAROID }

/**
 * v3xx — an EMOJI STICKER placed on the share card by the full-screen
 * editor. Positions are FRACTIONS of the card (0..1) so the preview, the
 * sheet and the exported PNG render identically at any size; [sizeFrac] is
 * the emoji's font size as a fraction of the card WIDTH. List order is the
 * stacking order: later entries draw ON TOP (Bring to front moves an entry
 * to the end, Send to back moves it to the front). Stickers are decoration
 * only — they never affect the text layout.
 */
data class ShareSticker(
    val emoji: String,
    /** Card-local x as a fraction of the card width (0..1, top-left). */
    val x: Float = 0.5f,
    /** Card-local y as a fraction of the card height (0..1, top-left). */
    val y: Float = 0.5f,
    /** Emoji font size as a fraction of the card width (e.g. 0.22 = 22%). */
    val sizeFrac: Float = 0.22f,
    // v3xx — IMPORTED image cutout: when set, the sticker renders the PNG at
    // [imagePath] (an absolute path under the app's files/stickers dir) instead
    // of the [emoji] glyph — sized by [sizeFrac] × card width, aspect-preserving,
    // transparency kept (a real PNG cutout with a see-through background).
    // Persisted by path; the renderer decodes + caches it per path.
    val imagePath: String? = null,
    /** v3xx — rotation in degrees (clockwise). Pinch-rotate or the slider. */
    val rotation: Float = 0f
)

/** v3xx — one COLLAGE polaroid look (index = [ShareCardMove.polaroidStyle]):
 *  the frame + tape colours, the print's tilt and the hairline finish.
 *  0 Classic (white + gold tape) · 1 Retro (aged cream) · 2 Sunglow
 *  (sun-warmed ivory + honey tape, replacing the old flat butter-yellow
 *  that read as a cheap sticker) · 3 Vintage (faded beige) · 4 Dashed
 *  (white + cool tape, dotted hairline) · 5 Candy (rose-tinted white +
 *  blush tape) · 6 Noir (deep charcoal + silver tape). */
private data class PolaroidLook(
    val frame: Color,
    val tape: Color,
    val tilt: Float,
    val finish: Color
)

private val polaroidLooks = listOf(
    PolaroidLook(Color.White, Color(0xFFD9BE8A), -3f, Color(0xFF70543A)),
    PolaroidLook(Color(0xFFF3EAD9), Color(0xFFC9A97C), -5f, Color(0xFF6B5638)),
    // v3xx — SUNGLOW rework: the old flat #F7D98B butter fill looked like a
    // cheap sticker and the gold tape vanished into it. The new look keeps
    // the sunny idea but reads like a real print: warm ivory paper, a honey
    // tape that actually contrasts, a soft golden sheen finish.
    PolaroidLook(Color(0xFFFDF3DC), Color(0xFFE3A93E), 4f, Color(0xFFB57B1E)),
    PolaroidLook(Color(0xFFDCD2C0), Color(0xFFB0986E), 6f, Color(0xFF4A3B28)),
    PolaroidLook(Color.White, Color(0xFFB8C4D8), -2f, Color(0xFF5A6A80)),
    // v3xx — CANDY: rose-tinted paper + blush tape (soft feminine look).
    PolaroidLook(Color(0xFFFFF1F3), Color(0xFFF2A7B3), -4f, Color(0xFFC26A78)),
    // v3xx — NOIR: dark charcoal print + silver tape (moody, film-noir look;
    // the caption ink stays warm so it still reads on the dark frame).
    PolaroidLook(Color(0xFF2B2B2E), Color(0xFF9AA0A6), 3f, Color(0xFFD8DCE0))
)

// v3xx — the Polaroid panel's style + filter pickers (indices map straight
// into [polaroidLooks] / the filter switch in CollageCard).
private val polaroidStyleNames = listOf("Classic", "Retro", "Sunglow", "Vintage", "Dashed", "Candy", "Noir")
private val polaroidFilterNames = listOf("None", "Noise", "Nostalgia", "B&W", "Warm")

/**
 * v3xx — the polaroid PRINT (frame + film window + photo filters + tape +
 * caption), extracted from CollageCard so EVERY style can wear it: Collage
 * calls it inline as part of its scrapbook design, and TopicShareCard
 * renders it as a shared overlay on the other styles once a user photo is
 * on the card (same style / filter / size / drag controls everywhere). All
 * geometry (position + size) is computed by the caller from the card's dp
 * dimensions — the print only draws itself at [pX]/[pY].
 */
@Composable
private fun PolaroidPrint(
    pW: Float,
    pH: Float,
    pX: Float,
    pY: Float,
    photoH: Float,
    look: PolaroidLook,
    pStyle: Int,
    pFilter: Int,
    userPhoto: androidx.compose.ui.graphics.ImageBitmap?,
    caption: String,
    captionInk: Color,
    onPhotoTap: (() -> Unit)?,
    callbacks: EditBoundsCallbacks,
    // v3xx — cast a drop shadow behind the print? The Collage turns it OFF:
    // the dark blur behind its TILTED cream strip read as a "background
    // showing behind the strip" (in the preview AND the export); the tilt +
    // the washi tape already give the print its scrapbook depth. Every
    // other style keeps the shadow for lift over its busy artwork.
    shadow: Boolean = true
) {
    Box(Modifier.offset(pX.dp, pY.dp).size(pW.dp, pH.dp)
        .graphicsLayer {
            rotationZ = look.tilt
            // v3xx — no shadow on the Collage's tilted print (see [shadow]).
            if (shadow) shadowElevation = 6f
            shape = RoundedCornerShape(3.dp)
            clip = false
        }
        .background(look.frame, RoundedCornerShape(3.dp))
        .clickable(enabled = onPhotoTap != null && userPhoto == null) { onPhotoTap?.invoke() }
        // v3xx — report the print's bounds so the editor can select
        // and drag it like the cover/jacket.
        .onGloballyPositioned { callbacks.onPolaroid(it.boundsInWindow()) }
    ) {
        // Photo area — the print's film window (tappable when empty).
        // v3xx — the window is CLIPPED to the same 2dp corners as the
        // finish hairline, so the photo (and the empty-film fill) corners
        // line up exactly with the outline instead of a square photo under
        // a rounded frame.
        Canvas(Modifier
            .offset(5.dp, 5.dp)
            .size((pW - 10).dp, photoH.dp)
            .clip(RoundedCornerShape(2.dp))
        ) {
            val zw = size.width; val zh = size.height
            if (userPhoto != null) {
                // Contain-fit the photo inside the window (the window
                // already matches the photo's aspect, so this fills it
                // edge-to-edge without cropping or stretching).
                val ar = userPhoto.width.toFloat() / userPhoto.height.toFloat()
                val zA = zw / zh
                val (dW, dH) = if (ar > zA) zw to zw / ar else zh * ar to zh
                val dx = (zw - dW) / 2f; val dy = (zh - dH) / 2f
                drawImage(
                    userPhoto,
                    dstOffset = androidx.compose.ui.unit.IntOffset(dx.roundToInt(), dy.roundToInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(dW.roundToInt(), dH.roundToInt()),
                    colorFilter = when (pFilter) {
                        2 -> ColorFilter.colorMatrix(sepiaMatrix)
                        3 -> ColorFilter.colorMatrix(grayscaleMatrix)
                        4 -> ColorFilter.colorMatrix(warmMatrix)
                        else -> null
                    }
                )
                // v3xx — FILTER overlays: grain for Noise, a soft
                // warm cast + vignette for Nostalgia / Warm.
                if (pFilter == 1) {
                    val rnd = java.util.Random((zw * 131 + zh * 17).toLong())
                    repeat(240) {
                        drawCircle(
                            if (it % 2 == 0) Color.White.copy(alpha = rnd.nextFloat() * 0.09f)
                            else Color.Black.copy(alpha = rnd.nextFloat() * 0.09f),
                            rnd.nextFloat() * 1.1f + 0.3f,
                            Offset(rnd.nextFloat() * zw, rnd.nextFloat() * zh)
                        )
                    }
                }
                if (pFilter == 2) drawRect(Color(0xFFE8C58A).copy(alpha = 0.10f))
                if (pFilter == 4) drawRect(Color(0xFFF0B060).copy(alpha = 0.15f))
                if (pFilter == 2 || pFilter == 4) {
                    drawRect(Brush.radialGradient(
                        listOf(Color.Transparent, Color.Transparent, Color(0xFF3A2410).copy(alpha = 0.20f)),
                        center = Offset(zw / 2f, zh / 2f), radius = zw * 0.78f
                    ))
                }
                drawRect(Color(0xFFD4A574).copy(alpha = 0.10f))
            } else {
                drawRoundRect(Color(0xFFE0D8CC), Offset.Zero, Size(zw, zh), CornerRadius(2.dp.toPx()))
                // Hint — camera icon + text when no photo
                val cx = zw / 2f
                val cy = zh / 2f
                // Camera body
                val camW = zw * 0.30f
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
                    .offset(5.dp, (photoH * 0.42f).dp)
                    .size((pW - 10).dp, (photoH * 0.30f).dp),
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
        // Polaroid finish — hairline frame tracing the film window +
        // a glassy sheen over the photo. v3xx — the OUTLINE is now
        // ACCURATE: it hugs the window from INSIDE (inset by half the
        // stroke) instead of straddling the edge, and it is drawn AFTER
        // the sheen so the white gloss never washes the hairline out —
        // the old order painted the sheen over the line's inner half,
        // which read as a broken/fuzzy frame ("square dark outline but
        // inaccurate"). The Dashed style keeps its dotted hairline (it
        // is the style's identifier), and the finish now ADAPTS to the
        // photo filter + wears a per-style stroke personality.
        Canvas(Modifier.fillMaxSize()) {
            val pws = size.width
            val winH = photoH.dp.toPx()
            val fw = pws - 10f
            // v3xx — per-style stroke: Vintage reads thinner + fainter
            // (aged print), Noir a touch heavier (bold silver frame),
            // the rest the standard 1.3dp.
            val strokeW = when (pStyle) {
                3 -> 1.0.dp.toPx()      // Vintage
                6 -> 1.5.dp.toPx()      // Noir
                else -> 1.3.dp.toPx()
            }
            val half = strokeW / 2f
            // The sheen FIRST, so the hairline above it stays crisp.
            val sheen = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.0f), Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.0f)),
                startY = 5f, endY = winH
            )
            drawRect(sheen, Offset(5f, 5f), Size(fw, winH))
            // v3xx — FILTER-ADAPTIVE finish: the hairline follows the
            // photo treatment (B&W → gray line, Nostalgia/Warm → warmed
            // line, Noise/None → the style's own finish).
            val finishBase = look.finish
            val finish = when (pFilter) {
                3 -> androidx.compose.ui.graphics.lerp(finishBase, Color.Gray, 0.80f)   // B&W
                2, 4 -> androidx.compose.ui.graphics.lerp(finishBase, Color(0xFFB07A45), 0.45f) // Nostalgia/Warm
                else -> finishBase
            }.copy(alpha = if (pStyle == 3) 0.26f else 0.40f)
            val corner = 2.dp.toPx()
            if (pStyle == 4) {
                drawRoundRect(
                    finish,
                    Offset(5f + half, 5f + half),
                    Size(fw - strokeW, winH - strokeW),
                    CornerRadius((corner - half).coerceAtLeast(0f)),
                    style = Stroke(strokeW, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                )
            } else {
                drawRoundRect(
                    finish,
                    Offset(5f + half, 5f + half),
                    Size(fw - strokeW, winH - strokeW),
                    CornerRadius((corner - half).coerceAtLeast(0f)),
                    style = Stroke(strokeW)
                )
            }
        }
        // v3xx — TAPE: drawn LAST (sits ON the photo) and PEERING out
        // past the top edge of the white frame (offset y = -5dp), so it
        // reads as real washi tape sticking the print to the paper —
        // never boxed in by the outline.
        Canvas(
            Modifier
                .offset((pW * 0.24f).dp, (-5).dp)
                .size((pW * 0.52f).dp, 14.dp)
                .graphicsLayer { rotationZ = 2f }
        ) {
            drawRect(look.tape.copy(alpha = 0.72f))
            // Subtle lighter core so the tape reads as translucent.
            drawRect(Color.White.copy(alpha = 0.10f), Offset.Zero, Size(size.width, size.height * 0.42f))
        }

        // Handwritten name below photo — constrained width + lineHeight
        // >= fontSize so long captions ellipsize (not clip) and never squish.
        val capFont = (pW * 0.065f).coerceIn(11f, 15f)
        Text(caption, style = TextStyle(
            fontFamily = PatrickHandFontFamily, fontWeight = FontWeight.Normal,
            fontSize = capFont.sp, color = captionInk,
            lineHeight = (capFont * 1.2f).sp
        ), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(8.dp, (photoH + 6f).dp).width((pW - 16).dp))
    }
}

// v3xx — normalizes an angle to -180..180 (used by pinch-rotate so the
// sticker rotation slider's range stays valid).
private fun normDegrees(r: Float): Float = ((r + 180f) % 360f + 360f) % 360f - 180f

// v3xx — imported sticker PNGs decode once per path (they live in the app's
// own files/stickers dir and can't change under us). Downscaled on decode so
// a huge gallery PNG doesn't sit in memory at full res.
private val stickerBitmapCache = java.util.concurrent.ConcurrentHashMap<String, androidx.compose.ui.graphics.ImageBitmap>()

private fun decodeStickerBitmap(path: String): androidx.compose.ui.graphics.ImageBitmap? {
    stickerBitmapCache[path]?.let { return it }
    val bmp = runCatching {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / (sample * 2) >= 512 && opts.outHeight / (sample * 2) >= 512) sample *= 2
        val full = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        android.graphics.BitmapFactory.decodeFile(path, full)
    }.getOrNull() ?: return null
    val img = bmp.asImageBitmap()
    stickerBitmapCache[path] = img
    return img
}

/** v3xx — copies a picked gallery image into the app's stickers dir as a PNG
 *  (re-encode strips any weird source format and guarantees transparency is
 *  preserved for cutouts) and returns the new absolute path. */
private fun importStickerPng(context: android.content.Context, uri: android.net.Uri): String? = runCatching {
    val dir = java.io.File(context.filesDir, "stickers").apply { mkdirs() }
    val file = java.io.File(dir, "sticker_${System.currentTimeMillis()}.png")
    val src = context.contentResolver.openInputStream(uri) ?: return@runCatching null
    val bmp = android.graphics.BitmapFactory.decodeStream(src)
    src.close()
    if (bmp == null) return@runCatching null
    java.io.FileOutputStream(file).use { out ->
        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
    }
    file.absolutePath
}.getOrNull()

// v3xx — polaroid PHOTO filters (color matrices for Nostalgia / B&W / Warm).
private val sepiaMatrix = ColorMatrix(floatArrayOf(
    0.393f, 0.769f, 0.189f, 0f, 0f,
    0.349f, 0.686f, 0.168f, 0f, 0f,
    0.272f, 0.534f, 0.131f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
))
private val grayscaleMatrix = ColorMatrix(floatArrayOf(
    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
))
private val warmMatrix = ColorMatrix(floatArrayOf(
    1.06f, 0f, 0f, 0f, 0.02f,
    0f, 1.02f, 0f, 0f, 0.01f,
    0f, 0f, 0.92f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
))

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

/** v374 — smart auto-fit delta: how a long quick/custom fact is made to fit
 *  the WHOLE card. Every adjustment rides the SAME channel a slider drives —
 *  [heightFrac] multiplies the fact-box HEIGHT (the Fact-height slider),
 *  [widthFrac] multiplies the fact-box WIDTH (the Fact-width slider) and
 *  [textScale] multiplies the quick-fact TEXT size (the bodyScale the Size
 *  slider sets) — so there is NO hidden size adjuster: the fit simply
 *  nudges the same values the user's own sliders would. Identity = none. */
private class ShareAutoFitDelta(
    val heightFrac: Float = 1f,
    val widthFrac: Float = 1f,
    val textScale: Float = 1f
)

/** v3xx — how much a long fact grows its box, by MEASURED wrap lines alone
 *  (one curve, no presets): [rememberFactWrapLines] reports the fact's REAL
 *  line count at the measurement width, so the curve keys on how the text
 *  actually wraps (a long URL or run of long words counts its many lines,
 *  short prose its few) instead of a character-count guess. The collision
 *  budget ([factFitBudget]) then clamps the grown box per style.
 *  v3xx — the curve's TOP END rises to 3.2×: the free-middle clamp can
 *  force the text scale down on long facts, and the render's maxLines
 *  (≈ base×frac÷scale) must stay ≥ the real wrap count — Clean/NEUMORPHIC's
 *  narrow 224dp content needs frac ≥ ~2.6 on 9:16 for that, which the old
 *  2.4 cap couldn't give (long Clean facts got cut by a line or two). */
private fun autoFitGrowByWrap(wrapLines: Int): Float = when {
    wrapLines > 34 -> 3.2f
    wrapLines > 25 -> 2.8f
    wrapLines > 20 -> 2.2f
    wrapLines > 15 -> 1.7f
    wrapLines > 11 -> 1.45f
    wrapLines > 7 -> 1.25f
    wrapLines > 4 -> 1.12f
    else -> 1f
}

/** v374 — WHOLE-CARD collision budget per design: (max fact-box height ×,
 *  min fact-text scale). The max height is how far the fact box may grow
 *  before it hits the title above, the info rows/footer below or the card
 *  edge on that style — derived from each layout's real room (Collage's
 *  fixed band barely grows; Editorial's byline→colophon slot a little;
 *  bottom-anchored Clean/Minimal grow up into the free middle; Paper/Vinyl/
 *  Signature/Custom sit mid-flow with modest slack). Past the cap the fit
 *  stops growing the box and shrinks the TEXT via the bodyScale channel
 *  down to [minTextScale] instead of letting anything leave the card. */
private fun factFitBudget(style: ShareCardStyle, aspect: ShareCardAspect): Pair<Float, Float> {
    val tall = aspect == ShareCardAspect.PORTRAIT
    return when (style) {
        // The fact lives inside a fixed dark band under the category pill —
        // v378 it shrinks the TEXT more eagerly so a long fact stays inside
        // the band instead of ballooning its box down over the footer
        // decoration ("box higher + text smaller" collage fit). v3xx — the
        // 9:16 band is taller, so the tall cap rises a little with it.
        ShareCardStyle.COLLAGE -> if (tall) 1.5f to 0.62f else 1.3f to 0.75f
        // Runs between the byline and the colophon — grows a little, then
        // the text shrinks.
        ShareCardStyle.EDITORIAL -> if (tall) 1.8f to 0.66f else 1.35f to 0.85f
        // Bottom-anchored facts grow UP into the free middle of the card.
        // v3xx — the 9:16 canvas has ~1.6× the free middle of 3:4, so the
        // tall cap rises to match (Clean grows to ~2.9× and the text barely
        // shrinks for long facts — the old 1.7 left the tall card with a
        // short box AND over-shrunk text; the free-middle clamp below keeps
        // the grown box clear of the credit regardless). The 3:4 caps stay
        // at the validated values.
        ShareCardStyle.NEUMORPHIC -> if (tall) 2.9f to 0.62f else 1.55f to 0.83f
        ShareCardStyle.MINIMAL -> if (tall) 2.7f to 0.64f else 1.5f to 0.85f
        // v378 — PAPER gets its own budget: its mid-flow body has room to
        // EXPAND (the frost pane can take more lines), so the fit favours
        // growing the box and barely touches the text — the old shared
        // mid-flow budget shrank Paper's type while the height could have
        // absorbed the text. v3xx — the tall cap rises to use the 9:16
        // canvas; the free-middle clamp still keeps the box off the footer.
        ShareCardStyle.PAPER -> if (tall) 2.6f to 0.80f else 1.85f to 0.96f
        // Mid-flow facts between the header/title and the footer (Vinyl /
        // Signature / Custom). v3xx — the tall cap rises with the taller
        // canvas; the free-middle clamp keeps the box clear of the footer.
        else -> if (tall) 2.2f to 0.62f else 1.45f to 0.83f
    }
}

/** v3xx — the FREE-MIDDLE height each style's fact box may occupy on the
 *  280dp-base preview (dp): the gap between the fixed header block (badge /
 *  title / info rows) and the fixed footer block (stars + credit). The fit
 *  clamps the text scale so the grown box's height (real wraps × line
 *  height × scale) never exceeds this — the "footer never moves, the box
 *  never hides behind the bottom design" guarantee. These are ONE-LINE-
 *  TITLE estimates (a 2-line title costs ~35dp more; the 1.15 safety in
 *  the clamp absorbs it). The 9:16 canvases are ~1.6× the 3:4 middle —
 *  this is why the tall budgets above are so much larger. */
private fun factAvailHeightDp(style: ShareCardStyle, aspect: ShareCardAspect): Float {
    val tall = aspect == ShareCardAspect.PORTRAIT
    return when (style) {
        ShareCardStyle.NEUMORPHIC -> if (tall) 340f else 205f
        ShareCardStyle.MINIMAL -> if (tall) 320f else 195f
        ShareCardStyle.PAPER -> if (tall) 300f else 185f
        ShareCardStyle.VINYL -> if (tall) 250f else 155f
        ShareCardStyle.SIGNATURE -> if (tall) 290f else 180f
        ShareCardStyle.CUSTOM -> if (tall) 290f else 180f
        ShareCardStyle.EDITORIAL -> if (tall) 230f else 140f
        ShareCardStyle.COLLAGE -> if (tall) 150f else 110f
    }
}

/** v3xx — each style's rendered fact LINE HEIGHT at scale 1.0 (dp) on the
 *  280dp-base preview, from the style's body size ladder × line-height
 *  multiplier. Used by the free-middle clamp and the auto-9:16 detection
 *  to convert a wrap count into a rendered height. Rounded UP (the fit
 *  must never under-estimate height — over-estimating only shrinks text a
 *  little more). */
private fun factLineHeightDp(style: ShareCardStyle): Float = when (style) {
    // Clean: 11sp × 1.40 (falls to ~9sp for the longest facts — the
    // estimate stays conservative for short-to-mid facts where the box
    // first approaches the footer).
    ShareCardStyle.NEUMORPHIC -> 16f
    // Minimal: 12sp × 1.50
    ShareCardStyle.MINIMAL -> 18f
    ShareCardStyle.PAPER -> 16f
    ShareCardStyle.VINYL -> 16f
    ShareCardStyle.EDITORIAL -> 17f
    ShareCardStyle.SIGNATURE -> 17f
    ShareCardStyle.CUSTOM -> 17f
    ShareCardStyle.COLLAGE -> 15f
}

/** v3xx — the hard floor the no-clip pass may shrink the fact text to.
 *  Below this the type is unreadable, so the fit never goes further (and
 *  no realistic fact needs to — the floor covers ~100+ wrapped lines on
 *  every style's box). The design floors in [factFitBudget] are SOFT
 *  targets; the no-clip pass may go below them, but never past this.
 *  v3xx — the floor matches the Text-size slider's 0.5× lower bound so
 *  the smart-fit text size is ALWAYS visible in the slider thumb (the old
 *  0.45 floor let the card render smaller than the slider could show —
 *  the "fit hidden below the slider" case). The 9:16 canvases + the
 *  free-middle clamp keep every realistic fact above it. */
private const val FactFitHardFloor = 0.5f

/** v3xx — the REAL width the sheet + full-screen previews render the card
 *  fact at (280dp base minus the canonical 14+14 side padding), used for
 *  the wrap-count measurement. The OLD measurement used the DESIGN width
 *  (405/450dp), so a fact that wrapped to ~13 lines on the 280dp preview
 *  measured ~8.6 — the fit under-grew the box and under-shrunk the text
 *  by ~1.5× and long facts got CUT (worst on Clean/NEUMORPHIC's 30/26
 *  padding). Per-style paddings differ by a little (224–252dp); the 0.85
 *  fit margin absorbs the difference. */
private const val FactWrapMeasureWidth = 252f

/** v3xx — each style's natural fact-box LINE capacity (the [fitLines]
 *  base at heightFrac = 1, per aspect) — what the box holds before any
 *  fit. The no-clip pass compares this against the measured wrap count so
 *  it sizes the text to the style's REAL room, not a character guess. */
private fun factBoxBaseLines(style: ShareCardStyle, aspect: ShareCardAspect): Int = when (style) {
    ShareCardStyle.COLLAGE -> 18
    ShareCardStyle.SIGNATURE -> if (aspect == ShareCardAspect.PORTRAIT) 14 else 10
    ShareCardStyle.CUSTOM -> if (aspect == ShareCardAspect.PORTRAIT) 20 else 14
    ShareCardStyle.VINYL -> if (aspect == ShareCardAspect.PORTRAIT) 8 else 6
    ShareCardStyle.NEUMORPHIC -> if (aspect == ShareCardAspect.PORTRAIT) 8 else 6
    // Paper's frost pane holds 10 lines at rest; Editorial / Minimal sit
    // between the two.
    ShareCardStyle.PAPER -> 10
    else -> if (aspect == ShareCardAspect.PORTRAIT) 12 else 9
}

/** v3xx — how many MORE lines a style's fact box wraps than the canonical
 *  [rememberFactWrapLines] measurement (now the REAL 280dp-base preview
 *  content width, 252dp at 11sp — see [FactWrapMeasureWidth]). Only
 *  Vinyl's deliberately NARROW pane (max 220dp) wraps significantly more
 *  (252/220 ≈ 1.15×); every other style's box is ~full width (their real
 *  content is 224–252dp), so their factor is 1.0 and the 0.85 fit margin
 *  absorbs the small per-style differences. The old 1.7× was tuned against
 *  the DESIGN-width measurement (377dp) — with the real-width measurement
 *  it over-counted Vinyl's wraps by ~1.5× and shrank its text for no
 *  reason. */
private fun factWrapFactor(style: ShareCardStyle): Float = when (style) {
    ShareCardStyle.VINYL -> 1.15f
    else -> 1.0f
}

/**
 * v374/v379e/v3xx — SHARED auto-fit SIZING for a long quick/custom fact:
 * how much the text shrinks and the box grows, from the MEASURED wrap-lines
 * curve ([autoFitGrowByWrap], fed by [rememberFactWrapLines]) + the
 * design's collision budget alone. v379e is TEXT-FIRST: by default the
 * fact box KEEPS ITS FULL HEIGHT and the TEXT shrinks inversely with the
 * wrap ratio (a fact that wraps to 1.3× the box's lines renders at ~0.77×
 * in the same footprint). Only when that shrink would pass the design's
 * text floor does the BOX grow to absorb the remainder — and only up to
 * the budget's height cap. v3xx — NO-CLIP GUARANTEE: past the cap the
 * TEXT keeps shrinking (below the design floor when necessary, down to
 * [FactFitHardFloor]) until the WHOLE fact fits the box — a long fact
 * NEVER ellipsizes or gets cut, and the box never grows past the cap (so
 * it never reaches the footer / title / card edge). The fit solves
 * capacity(base × H) ≥ text(effective × S) for S, so the text always
 * fits the box the user sees. One continuous curve, no hidden per-style
 * logic.
 */
private fun autoFitShape(style: ShareCardStyle, aspect: ShareCardAspect, wrapLines: Int): ShareAutoFitDelta {
    val (maxHeightFrac, _) = factFitBudget(style, aspect)
    val grow = autoFitGrowByWrap(wrapLines)
    if (grow <= 1f) return ShareAutoFitDelta()
    val base = factBoxBaseLines(style, aspect).toFloat()
    // The style's REAL wrapped line count (narrow panes wrap more).
    val eff = wrapLines * factWrapFactor(style)
    // The largest text scale that still fits the whole fact in a box of
    // [h]× base lines: capacity(base×h/S) ≥ lines(eff×S) → S² ≤ base·h/eff.
    // The 0.85 margin absorbs the canonical-vs-real measurement slack (the
    // real previews render at 224–252dp content vs the 252dp canonical
    // measurement — up to ~1.13× more wraps) so the guarantee holds even
    // when the style's box differs a little. (The old 0.92 margin + design-
    // width measurement together under-counted wraps by ~1.5× and long
    // facts got CUT.)
    // v3xx — TWO more bounds ride on top of the capacity model:
    //  · the render's maxLines cap (≈ base×h÷scale) must stay ≥ the REAL
    //    wrap count — the width-factor difference (252 measured vs 224
    //    rendered on Clean) breaks the plain model in the mid-range, so
    //    the scale is capped at base·h/(eff×1.2) (the 1.2 absorbs every
    //    style's real-content width);
    //  · the free-middle height bound below.
    fun fitScaleFor(h: Float): Float {
        val capacity = kotlin.math.sqrt(base * h / eff.coerceAtLeast(1f)) * 0.85f
        val maxLinesBound = base * h / (eff.coerceAtLeast(1f) * 1.2f)
        return capacity.coerceAtMost(maxLinesBound).coerceIn(FactFitHardFloor, 1f)
    }
    // v3xx — HEIGHT-FIRST (user direction 2026-09-08): the box grows toward
    // the style's FULL budget cap whenever the fact exceeds the box's
    // natural capacity, and the TEXT sizes to fit the grown box. The old
    // text-first solver shrank the type while the box sat at its natural
    // height, so a longer fact never LOOKED taller even when the card had
    // room below ("not just text-size decrease but also height increase in
    // accordance with the bottom area"; the height wouldn't expand even
    // with plenty of space below). The fit scale still guarantees the whole
    // fact fits the grown box (no clip, no overlap), down to the hard floor.
    val h = grow.coerceIn(1f, maxHeightFrac)
    var s = fitScaleFor(h)
    // v3xx — FREE-MIDDLE BOUND (the "footer never moves, box never hides
    // behind the bottom design" guarantee): the grown box's RENDERED height
    // is realWraps × lineHeight × scale, and it must stay within the
    // style's free middle (between the header and the credit/footer). The
    // capacity model above only guarantees the text fits its own box — it
    // knows nothing about the footer. Clamp the scale so the box's height
    // clears the footer even on the LONGEST facts; the 1.15 factor covers
    // the per-style measurement slack + a 2-line title. Past the floor the
    // aspect can't hold the text at all — the auto-layout pill detects
    // exactly that ([autoTall]) and flips the card to 9:16.
    val maxByRoom = factAvailHeightDp(style, aspect) /
        (eff.coerceAtLeast(1f) * factLineHeightDp(style) * 1.15f)
    if (maxByRoom < s) s = maxOf(maxByRoom, FactFitHardFloor).coerceAtMost(s)
    return ShareAutoFitDelta(heightFrac = h, widthFrac = 1f, textScale = s)
}

/**
 * v374/v3xx — the RENDER-TIME smart fit: [autoFitShape] gated by the Smart
 * fit toggle and the "manual edits win" rule. v3xx — the input is the
 * MEASURED wrap line count ([rememberFactWrapLines]) instead of a raw
 * character count, so a long URL counts by the lines it actually wraps
 * into, not by its length. v379e — the Fit toggle turning ON clears a
 * previously manual box (see the Fit panel), so the fit can always "fix
 * the box" again after the user has resized it.
 */
private fun smartAutoFitDelta(
    move: ShareCardMove,
    wrapLines: Int,
    style: ShareCardStyle,
    aspect: ShareCardAspect
): ShareAutoFitDelta {
    if (!AppPreferences.shareAutoFitState) return ShareAutoFitDelta()
    val touched = move.factDx != 0f || move.factDy != 0f ||
        move.factWidthFrac != 1f || move.factHeightFrac != 1f ||
        move.factScale != 1f || move.factBoxScale != 1f
    if (touched) return ShareAutoFitDelta()
    return autoFitShape(style, aspect, wrapLines)
}

/**
 * v3xx — the MEASURED wrap estimate that feeds the smart fit. The old
 * pipeline guessed the fact's footprint from its CHARACTER COUNT; this
 * measures the fact text with a real [TextMeasurer] at the card's content
 * width at scale 1.0 (canonical 11sp Lora body — representative of the
 * per-style fact styles) and returns how many LINES it actually wraps
 * into. A 60-char URL wraps to several lines; 300 chars of short prose
 * wraps to few — exactly the case the character buckets got wrong.
 * [primary] is the fact body ([editedFact] ?: [factText]) and [secondary]
 * the chapter text; whichever wraps taller drives the fit (mirrors the old
 * maxOf(length, length) input). Pure (text, width) measurement — no
 * rendered Rects, one deterministic pass.
 * v3xx — the width is the REAL 280dp-base preview content width
 * ([FactWrapMeasureWidth]), NOT the design width: the sheet + full-screen
 * previews render the card at 280dp (aspect changes only the HEIGHT), so
 * a design-width measurement under-counted wraps by ~1.5× and long facts
 * got cut. The [aspect] param stays for call-site symmetry but no longer
 * changes the width — 9:16 and 3:4 cards wrap identically wide.
 */
@Composable
private fun rememberFactWrapLines(primary: String, secondary: String, aspect: ShareCardAspect): Int {
    val measurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    return remember(primary, secondary) {
        fun wrapOf(text: String): Int {
            if (text.isBlank()) return 0
            val widthPx = with(density) { FactWrapMeasureWidth.dp.toPx() }.toInt().coerceAtLeast(1)
            val res = measurer.measure(
                text = AnnotatedString(text),
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = LoraFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 15.5.sp
                ),
                softWrap = true,
                overflow = TextOverflow.Clip,
                constraints = Constraints(maxWidth = widthPx)
            )
            return res.lineCount.coerceAtLeast(1)
        }
        maxOf(wrapOf(primary), wrapOf(secondary)).coerceAtLeast(1)
    }
}

// ═══════════════════════════════════════════════════════════════════════
/** v379e — ink that actually READS on Paper's quick-fact pane. The pane is
 *  the palette background blended with FrostPane's translucent white (35%),
 *  so on a DARK premium tone (Onyx / Midnight / Wine …) the effective pane
 *  is still dark — text must go near-WHITE there; on the light paper tones
 *  the pane is light and text stays near-black. Never the theme colour, and
 *  never a palette ink that fights the blend (Paper's background follows
 *  the palette, so the old theme-onSurface text rendered BLACK on the dark
 *  premium tones and the palette ink was tuned for the raw background, not
 *  the blended pane). */
private fun ShareCardPalette.frostInk(): Color {
    val pane = androidx.compose.ui.graphics.lerp(bgMid, Color.White, 0.35f)
    val l = pane.red * 0.299f + pane.green * 0.587f + pane.blue * 0.114f
    return if (l < 0.5f) Color(0xFFFBF6EF) else Color(0xFF241D14)
}

// ═══════════════════════════════════════════════════════════════════════
// v379d — AUTO-LAYOUT pill: the sparkle button floating over the card
// re-runs a WHOLE-CARD fit on tap — box growth + text sizing ride the SAME
// slider channels the smart fit uses (fact-height ×, fact text via
// [ShareCardMove.factScale]) and commit into the per-style move, so the
// preview, the saved card and the export all match. Tapping again cycles
// through alternative ARRANGEMENTS (standard fit → condensed → book page →
// tall 9:16 when the current aspect can't hold the text). Manual position
// drags (title/fact/cover offsets) are never overwritten — the plan only
// touches the box/text channels and the aspect when it must.
// ═══════════════════════════════════════════════════════════════════════
/** One auto-layout candidate: what the pill commits for a given attempt.
 *  Zero fields mean "leave that channel alone". */
    private data class ShareAutoLayoutPlan(
    val heightFrac: Float = 0f,
    val textScale: Float = 0f,
    /** Title moves UP by this many dp (always >= 0). */
    val titleLift: Float = 0f,
    /** Fact box pushed DOWN by this many dp (always >= 0). */
    val factLift: Float = 0f,
    /** v3xx — ADVANCED solver: when the title can't lift any further (the
     *  category pill / card edge above it) and the fact can't be pushed down
     *  any further (the card bottom), the RESIDUAL overlap is resolved by
     *  SHRINKING the fact box by this many dp (applied to factHeightFrac, so
     *  the preview + export match). 0f = no shrink needed. */
    val factShrinkDp: Float = 0f,
    /** INFO-ROWS lift (SIGNED dp): negative lifts the author/year rows UP
     *  back between the title and the quick fact; positive pushes them DOWN
     *  clear of a fact that sits above them. */
    val metaLift: Float = 0f,
    /** Favorites strip pushed DOWN by this many dp (always >= 0). */
    val favLift: Float = 0f,
    /** CATEGORY-PILL lift (SIGNED dp): negative lifts the badge UP when it
     *  has been dragged down onto the title; positive pushes it DOWN clear
     *  of a fact/fav that grew over it from above. */
    val badgeLift: Float = 0f,
    val format: ShareCardFactFormat? = null,
    val factDropCap: ShareCardFactDropCap? = null,

    // v3xx — OUT-OF-CARD repair deltas (card-local dp, applied to the
    // element's own dx/dy): bring anything that drifted past the card edge
    // back INSIDE on the same sparkle tap (never re-centring it — just
    // enough to clear the edge).
    val fixTitleX: Float = 0f,
    val fixTitleY: Float = 0f,
    val fixFactX: Float = 0f,
    val fixFactY: Float = 0f,
    val fixMetaX: Float = 0f,
    val fixMetaY: Float = 0f,
    val fixFavX: Float = 0f,
    val fixFavY: Float = 0f,
    val fixBadgeX: Float = 0f,
    val fixBadgeY: Float = 0f,

    // v3xx — TRUE when the title was MANUALLY dragged to overlap the fact
    // (or the info rows): the sparkle RESETS the manual vertical offset so
    // the title returns to its natural spot at the top of the card — the
    // dragged-over repair the user asked for (no minimal-lift guess, the
    // title goes all the way back).
    val resetTitleY: Boolean = false,

    // v3xx — TRUE when the QUICK-FACT BOX was manually dragged onto the
    // title / info rows / favorites strip, or off the card: the sparkle
    // RESETS its manual offsets back to the natural spot (the title-reset
    // twin for the fact) — the box auto-moves to a proper place on the
    // same tap.
    val resetFact: Boolean = false,

    // true → switch the CARD itself to 9:16 (only offered when the current
    // aspect is 3:4 and the text still overflows a fully-fitted 3:4 card).
    val tall: Boolean = false
)

private fun ShareAutoLayoutPlan.withCollisionRepair(
    titleLift: Float,
    factLift: Float,
    metaLift: Float,
    favLift: Float,
    badgeLift: Float
): ShareAutoLayoutPlan = if (titleLift <= 0f && factLift <= 0f && metaLift == 0f && favLift <= 0f && badgeLift == 0f) this else copy(
    titleLift = titleLift.coerceAtLeast(0f),
    factLift = factLift.coerceAtLeast(0f),
    // v3xx — SIGNED: negative lifts the info rows UP (back between the
    // title and the fact), positive pushes them down clear of a fact that
    // sits above them.
    metaLift = metaLift,
    favLift = favLift.coerceAtLeast(0f),
    badgeLift = badgeLift
)

/** v384 — the pill's attempt → plan table, layered over the TEXT-FIRST
 *  [autoFitShape]. Every attempt re-fits from the CURRENT text length on the
 *  CURRENT aspect, then layers a layout idea on top:
 *  0 = plain fit; 1 = CONDENSED lines; 2 = BOOK columns; 3 = EDITORIAL drop
 *  cap; 4 = maximal space (box to the budget cap + text at the floor);
 *  5 = a TALL 9:16 card (only when a 3:4 card is box-capped); 6 = back to
 *  the STANDARD paragraph. The caller skips attempts that would not change
 *  anything, so every effective tap advances the card. The whole plan rides
 *  through [withCollisionRepair], so an overlap created by manual dragging
 *  (title/fact/info rows) is repaired on the SAME tap that fits the box. */
private fun autoLayoutPlan(
    style: ShareCardStyle,
    aspect: ShareCardAspect,
    len: Int,
    attempt: Int,
    currentFormat: ShareCardFactFormat,
    move: ShareCardMove,
    // v384 — live measured card-local bounds of the current card. Zero rects
    // = element absent; the repair compares REAL boxes, so a grown fact box
    // covering the info rows (invisible to offset estimates) is caught too.
    titleRect: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero,
    factRect: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero,
    metaRect: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero,
    favRect: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero,
    badgeRect: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero,
    // v3xx — the card's dp dimensions (the measured rects are card-local),
    // for clamping anything that drifted OUTSIDE the card back inside.
    cardW: Float = 0f,
    cardH: Float = 0f,
    // v3xx — the MEASURED wrap line count of the current fact text (see
    // [rememberFactWrapLines]); the fit sizes from real wraps, not chars.
    wrapLines: Int = 0
): ShareAutoLayoutPlan {
    val shape = autoFitShape(style, aspect, wrapLines)
    // v3xx — PROSPECTIVE title lift, MEASURED: a bottom-anchored fact box
    // (Clean / Minimal) grows UP into the free middle when smart fit grows
    // it, so the grown box's top rises by (heightFrac−1)×its height and can
    // swallow the title on the SAME tap. When the live rects are available,
    // lift the title by exactly the amount needed to clear the grown fact
    // top — the old (heightFrac−1)×28dp guess under-lifted tall boxes and
    // left the title sitting inside the quick-fact area until a SECOND
    // sparkle tap re-measured the real overlap. Mid-flow styles keep the
    // fact's top fixed (they grow DOWN), so the measured overlap alone is
    // their lift. Rects not yet measured → the old heuristic fallback.
    val growsUp = style == ShareCardStyle.NEUMORPHIC || style == ShareCardStyle.MINIMAL
    val growthPx = if (growsUp && factRect.height > 0f && shape.heightFrac > 1f)
        (shape.heightFrac - 1f) * factRect.height else 0f
    val sizeLift = if (titleRect.width > 0f && titleRect.height > 0f &&
        factRect.width > 0f && factRect.height > 0f
    ) {
        val grownFactTop = (factRect.top - growthPx).coerceAtLeast(0f)
        (titleRect.bottom - grownFactTop).coerceIn(0f, 96f)
    } else {
        ((shape.heightFrac - 1f) * 28f).coerceIn(0f, 56f)
    }
    // Manual dragging is allowed to create an overlap — the sparkle tap is
    // the explicit repair gesture. The reference flow stacks TITLE → INFO
    // rows → FACT (a style may re-order them), so each shared collision is
    // read off the MEASURED boxes: how far a box's bottom sits below the box
    // beneath it. A lower box is pushed DOWN by exactly the overlap (capped),
    // the title is lifted up instead of the fact being shoved when the two
    // overlap (reads better), and — v3xx — the INFO ROWS are LIFTED UP back
    // between the title and the fact when a grown/dragged fact covers them
    // (never dumped below it). When the rects are still empty (not yet
    // measured), fall back to the drag-offset estimates.
    val t = titleRect; val f = factRect; val m = metaRect; val v = favRect; val b = badgeRect
    fun bottomOverlap(upper: androidx.compose.ui.geometry.Rect, lower: androidx.compose.ui.geometry.Rect): Float {
        if (upper.width <= 0f || upper.height <= 0f || lower.width <= 0f || lower.height <= 0f) return 0f
        val hOverlap = minOf(upper.right, lower.right) - maxOf(upper.left, lower.left)
        if (hOverlap <= 0f) return 0f
        return (upper.bottom - lower.top).coerceAtLeast(0f)
    }
    // v3xx — [bottomOverlap] ASSUMES the first box sits ABOVE the second;
    // calling it on a box that is actually BELOW produces a giant false
    // positive (lower.bottom - upper.top ≈ the whole gap). The strip vs the
    // fact flips order per style (bottom corner on Paper/Vinyl, top-middle on
    // Collage/Signature), so guard each pair by the real vertical order.
    fun pokeAbove(upper: androidx.compose.ui.geometry.Rect, lower: androidx.compose.ui.geometry.Rect): Float =
        if (upper.top < lower.top) bottomOverlap(upper, lower) else 0f
    val titleFactOverlap = bottomOverlap(t, f)
    // v3xx — the info rows SNAP back between the title and the quick fact
    // on the sparkle tap, TOUCHING the title: whatever pushed them down (a
    // grown or hand-dragged fact, an earlier nudge) is undone by lifting
    // the strip up so its top meets the title's bottom — the natural
    // title → info → fact flow. The fact may sit anywhere below; the strip
    // always ends up ABOVE it (="between"). Only fires when both rects
    // exist and the gap is real (> 2dp).
    val metaSnapLift = if (t.width > 0f && t.height > 0f && m.width > 0f && m.height > 0f) {
        val gap = (m.top - t.bottom).coerceAtLeast(0f)
        if (gap > 2f) -gap else 0f
    } else 0f
    val collisionTitleLift = maxOf(titleFactOverlap, bottomOverlap(t, m)).coerceAtMost(72f)
    // v3xx — a title the user DRAGGED into the fact (or the info rows) is
    // the one repair the fit must not guess at: the sparkle RESETS its
    // manual vertical offset so it returns to its natural spot at the top.
    // (The size lift for a fact grown up into the title's natural spot
    // still applies on top — the reset restores the position, the lift
    // keeps the fit clear.)
    val manualTitleOverlap = move.titlePlaced && (titleFactOverlap > 0f || bottomOverlap(t, m) > 0f)
    // v3xx — a fav strip parked ABOVE the fact (the Collage / Signature top
    // placement) that has grown/dragged INTO the fact pushes the FACT down by
    // the overlap — never the strip down into it (adding it to the fav lift
    // made the overlap worse: it shoved the strip INTO the box below). The
    // order guard also kills the false positives that used to shove the strip
    // around on the bottom-corner styles.
    val favOverFact = pokeAbove(v, f)
    // v3xx — the CATEGORY PILL is a small floating element that must never
    // be buried under a grown fact box or a lifted title: a badge sitting
    // ABOVE the fact/meta/fav pushes those DOWN (same model as the fav
    // strip), and the TITLE LIFT is capped at the badge's bottom edge so a
    // size-grown box can never shove the title up into the pill above it
    // (the shortfall falls back to pushing the fact down instead).
    val badgeOverFact = pokeAbove(b, f)
    val badgeOverMeta = pokeAbove(b, m)
    val titleLiftCap = when {
        b.width > 0f && b.height > 0f && t.width > 0f && t.height > 0f ->
            (t.top - b.bottom - 4f).coerceAtLeast(0f)
        // v3xx — the badge rect isn't measured yet (first composition): an
        // unbounded cap let the sparkle shove the title up over the
        // category icon on that first tap. The title may never rise above
        // its own natural top — the badge zone lives above it, so capping
        // the lift at the title's current top keeps the icon clear and the
        // shortfall falls back to pushing the fact down instead.
        t.width > 0f && t.height > 0f -> (t.top - 4f).coerceAtLeast(0f)
        else -> Float.MAX_VALUE
    }
    // The lift only "spends" itself on the MEASURED title collision — the
    // size lift is prospective (the new box hasn't grown yet), so it must
    // not absorb a fav/badge overlap that only a fact push can fix.
    // A dragged-over title is RESET to its natural spot, so only the
    // prospective grown-fact lift applies to it (never the collision lift
    // — that would shove the title ABOVE where it started).
    val titleLift = if (manualTitleOverlap)
        sizeLift.coerceAtMost(titleLiftCap)
    else minOf(maxOf(sizeLift, collisionTitleLift), titleLiftCap)
    val titleLiftSpent = minOf(collisionTitleLift, titleLift)
    // v3xx — ADVANCED overlap solver: the collision is resolved in THREE
    // steps, in order of preference — (1) the title lifts (capped by the
    // pill / card edge) or, when the title was dragged over the fact, is
    // RESET to the top entirely, (2) the fact pushes DOWN but never past
    // the card's bottom edge, (3) whatever is still overlapping SHRINKS
    // the fact box. The old two-step solver (lift + push, both
    // hard-capped) could leave a residual overlap on very tall grown boxes
    // and then declared the tap "done" — the overlap the user kept seeing.
    // The title-vs-fact overlap is excluded from the fact push when the
    // reset resolves it (pushing the fact down for it would shove the box
    // into the footer).
    val totalCollision = maxOf(
        if (manualTitleOverlap) 0f else titleFactOverlap,
        favOverFact, badgeOverFact
    )
    val spaceBelow = if (f.width > 0f && f.height > 0f) (cardH - f.bottom - 2f).coerceAtLeast(0f) else 0f
    val collisionFactLift = (totalCollision - titleLiftSpent).coerceIn(0f, spaceBelow.coerceAtMost(72f))
    val residualCollision = (totalCollision - titleLiftSpent - collisionFactLift).coerceAtLeast(0f)
    // Shrink the box by the residual (capped at 40% of its height so a
    // monstrous fact is reduced, never obliterated).
    val factShrinkDp = if (f.height > 0f) residualCollision.coerceAtMost(f.height * 0.40f) else 0f
    val collisionFavLift = maxOf(
        pokeAbove(t, v), pokeAbove(f, v), pokeAbove(m, v), pokeAbove(b, v)
    ).coerceAtMost(120f)
    // v3xx — the pill itself is movable, so it joins the repair on both
    // sides: a badge dragged DOWN onto the title is lifted back UP (-), and
    // a fact/fav grown or dragged OVER it from above pushes the pill down
    // (+). The badge is small, so the travel is capped tighter than the big
    // boxes.
    val badgeAboveTitle = pokeAbove(b, t)
    val upperOverBadge = maxOf(
        pokeAbove(t, b), pokeAbove(f, b), pokeAbove(m, b), pokeAbove(v, b)
    )
    val collisionBadgeLift = when {
        badgeAboveTitle > 0f -> -badgeAboveTitle.coerceAtMost(48f)
        upperOverBadge > 0f -> upperOverBadge.coerceAtMost(64f)
        else -> 0f
    }
    // v3xx — the info rows SNAP back between the title and the quick fact,
    // but only when no badge overlap is pushing them the other way (a
    // badge covering the strip wins: it must be pushed clear, never
    // snapped up INTO the pill).
    val collisionMetaLift = if (badgeOverMeta > 0f) badgeOverMeta.coerceAtMost(40f) else metaSnapLift

    // v3xx — a QUICK-FACT BOX the user DRAGGED onto the title / info rows /
    // favorites strip, or off the card edge, is reset to its natural spot
    // on the sparkle tap (the title-reset twin): the manual offsets zero
    // out and the fit re-sizes the box into that spot. Only fires for a
    // MANUALLY moved box (the fit never yanks a box the user placed by
    // choice) and only when there's a real collision to repair.
    val manualFact = move.factDx != 0f || move.factDy != 0f
    val factHangsOff = manualFact && f.width > 0f && f.height > 0f &&
        (f.left < 0f || f.right > cardW || f.bottom > cardH)
    val factOverTitle = manualFact && t.width > 0f && t.height > 0f &&
        f.width > 0f && f.height > 0f && pokeAbove(t, f) > 0f
    val factOverMeta = manualFact && m.width > 0f && m.height > 0f &&
        f.width > 0f && f.height > 0f && pokeAbove(m, f) > 0f
    val factOverFav = manualFact && v.width > 0f && v.height > 0f &&
        f.width > 0f && f.height > 0f && pokeAbove(v, f) > 0f
    val resetFact = manualFact && (factHangsOff || factOverTitle || factOverMeta || factOverFav)

    // v3xx — anything that drifted OUTSIDE the card gets pulled back inside
    // on the same tap (never re-centred — just enough to clear the edge).
    // The measured rects are card-local, so a negative left/top or a
    // right/bottom past the card dimensions is exactly how far it hangs off.
    fun clampIn(r: androidx.compose.ui.geometry.Rect): Pair<Float, Float> {
        if (r.width <= 0f || r.height <= 0f) return 0f to 0f
        val dx = when {
            r.left < 0f -> -r.left
            r.right > cardW -> cardW - r.right
            else -> 0f
        }
        val dy = when {
            r.top < 0f -> -r.top
            r.bottom > cardH -> cardH - r.bottom
            else -> 0f
        }
        return dx to dy
    }
    val (fixTx, fixTy) = if (t.width > 0f && t.height > 0f) clampIn(t) else 0f to 0f
    val (fixFx, fixFy) = clampIn(f)
    val (fixMx, fixMy) = clampIn(m)
    val (fixVx, fixVy) = clampIn(v)
    val (fixBx, fixBy) = clampIn(b)

    if (shape.heightFrac == 0f && titleLift == 0f && collisionFactLift == 0f &&
        collisionMetaLift == 0f && collisionFavLift == 0f && collisionBadgeLift == 0f &&
        factShrinkDp == 0f && !manualTitleOverlap &&
        fixTx == 0f && fixTy == 0f && fixFx == 0f && fixFy == 0f &&
        fixMx == 0f && fixMy == 0f && fixVx == 0f && fixVy == 0f &&
        fixBx == 0f && fixBy == 0f
    ) return ShareAutoLayoutPlan()

    val (maxHeightFrac, minTextScale) = factFitBudget(style, aspect)
    val capped = shape.heightFrac >= maxHeightFrac - 0.01f && shape.heightFrac > 1f
    val repaired = (when ((attempt % 7 + 7) % 7) {
        0 -> ShareAutoLayoutPlan(heightFrac = shape.heightFrac, textScale = shape.textScale)
        1 -> ShareAutoLayoutPlan(heightFrac = shape.heightFrac, textScale = shape.textScale, format = ShareCardFactFormat.CONDENSED)
        2 -> ShareAutoLayoutPlan(
            heightFrac = shape.heightFrac, textScale = shape.textScale,
            format = if (len >= 150) ShareCardFactFormat.BOOK else ShareCardFactFormat.STANDARD
        )
        3 -> ShareAutoLayoutPlan(
            heightFrac = shape.heightFrac, textScale = shape.textScale,
            format = ShareCardFactFormat.EDITORIAL,
            factDropCap = ShareCardFactDropCap.LETTER
        )
        4 -> ShareAutoLayoutPlan(
            heightFrac = maxHeightFrac,
            textScale = minTextScale,
            format = if (len >= 150) ShareCardFactFormat.BOOK else ShareCardFactFormat.EDITORIAL
        )
        5 -> when {
            // v380/v3xx — a 9:16 flip must buy READABILITY, not just length:
            // hand the taller canvas the REAL 9:16 fit (the tall budget cap +
            // the tall fit's text, both now sized from the measured wraps and
            // the free-middle clamp) — the old 1.4×/1.18× guess left the
            // tall card with an arbitrary box and oversized text that still
            // overflowed on long facts.
            aspect == ShareCardAspect.CLASSIC && capped -> {
                val tallShape = autoFitShape(style, ShareCardAspect.PORTRAIT, wrapLines)
                ShareAutoLayoutPlan(
                    tall = true,
                    heightFrac = tallShape.heightFrac.coerceIn(1.6f, 3.2f),
                    textScale = tallShape.textScale.coerceIn(FactFitHardFloor, 1.18f)
                )
            }
            else -> ShareAutoLayoutPlan(heightFrac = shape.heightFrac, textScale = shape.textScale)
        }
        else -> ShareAutoLayoutPlan(
            heightFrac = shape.heightFrac, textScale = shape.textScale,
            format = ShareCardFactFormat.STANDARD,
            factDropCap = ShareCardFactDropCap.NONE
        )
    }).withCollisionRepair(titleLift, collisionFactLift, collisionMetaLift, collisionFavLift, collisionBadgeLift)
    // v3xx — a dragged-over title carries the RESET flag on the plan so the
    // commit zeroes its manual vertical offset (back to the natural top).
    val withReset = if (manualTitleOverlap) repaired.copy(resetTitleY = true) else repaired
    val withShrink = if (factShrinkDp > 0f) withReset.copy(factShrinkDp = factShrinkDp) else withReset
    return if (fixTx == 0f && fixTy == 0f && fixFx == 0f && fixFy == 0f &&
        fixMx == 0f && fixMy == 0f && fixVx == 0f && fixVy == 0f &&
        fixBx == 0f && fixBy == 0f
    ) withShrink else withShrink.copy(
        fixTitleX = fixTx, fixTitleY = fixTy,
        fixFactX = fixFx, fixFactY = fixFy,
        fixMetaX = fixMx, fixMetaY = fixMy,
        fixFavX = fixVx, fixFavY = fixVy,
        fixBadgeX = fixBx, fixBadgeY = fixBy
    )
}

/** v379d — the floating sparkle pill: round button at the card's top-end
 *  corner. Shows on BOTH the resting preview and the edit mode; tap = run
 *  the next auto-layout attempt (see [autoLayoutPlan]). v380 — the old
 *  TRANSLUCENT surface + shadow elevation painted a soft dark halo that
 *  read as a "solid fill glitch" over busy cards (translucent fills bleed
 *  elevation shadows); it is now a fully OPAQUE pill with a hairline ring
 *  and no shadow — crisp over any card, and the same size it always was. */
@Composable
private fun AutoLayoutPill(onTap: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onTap,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = "auto_awesome",
                contentDescription = "Auto layout — tap to fit the card, tap again for another arrangement",
                size = 18.dp,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
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
    /** Info-row (author/byline/year) box size — same crop model as the
     *  title/fact boxes. Width is a fill fraction; height scales the row's
     *  max lines (1f = 2 lines allowed so long info wraps instead of
     *  clipping, 0.5f = 1 line, ellipsized). */
    val metaWidthFrac: Float = 1f,
    val metaHeightFrac: Float = 1f,
    /** Offset applied to the card's category badge/pill — the B handle
     *  in edit mode. The badge moves freely on the card. */
    val badgeDx: Float = 0f,
    val badgeDy: Float = 0f,
    /** Offset applied to the BOOK-COVER jacket badge (top-right corner of
     *  book share cards) — the cover drags like the badge does, so the user
     *  can place the jacket anywhere on the card. Persisted per style with
     *  the rest of the move edits. */
    val coverDx: Float = 0f,
    val coverDy: Float = 0f,
    /** v353 — offset applied to the ALBUM favorite-tracks strip, so the user
     *  can drag it anywhere on the card (mirrors coverDx/coverDy). Persisted
     *  per style with the rest of the move edits. */
    val favDx: Float = 0f,
    val favDy: Float = 0f,
    /** v384 — how many favorite tracks the strip shows (0 = auto: the
     *  default 3, or ALL tracks when No fact expands the strip). */
    val favCount: Int = 0,
    /** v370 — ALBUM favorite-tracks strip SIZE (1f = unchanged). Width is a
     *  fill fraction of the strip's natural max width; height scales how
     *  many track rows the strip shows. Mirrors the box-size sliders. */
    val favWidthFrac: Float = 1f,
    val favHeightFrac: Float = 1f,
    /** v373 — INDEPENDENT whole-box scales (1f = unchanged). Each multiplies
     *  the element's width AND height fractions TOGETHER so the whole box
     *  grows/shrinks as one, WITHOUT touching the width/height slider
     *  values — the old "Whole box" slider borrowed the height fraction as
     *  its backing value, so dragging width or height yanked the Whole-box
     *  thumb around and the two were never independent. */
    val titleBoxScale: Float = 1f,
    val factBoxScale: Float = 1f,
    val favBoxScale: Float = 1f,
    /** v3xx — POLAROID (Collage card only): the instant-print is a movable,
     *  scalable element like the cover / favorites strip. The grip drags it
     *  via [polaroidDx]/[polaroidDy] (card-local dp), [polaroidScale] grows /
     *  shrinks the whole print (1f = the design default), [polaroidStyle]
     *  picks the frame + tape + tilt look (0 Classic · 1 Retro · 2 Sunglow ·
     *  3 Vintage · 4 Dashed) and [polaroidFilter] picks the photo treatment
     *  (0 None · 1 Noise · 2 Nostalgia · 3 B&W · 4 Warm). Persisted per style
     *  with the rest of the move edits; Reset layout clears the position +
     *  scale but keeps the style/filter choices. */
    val polaroidDx: Float = 0f,
    val polaroidDy: Float = 0f,
    val polaroidScale: Float = 1f,
    val polaroidStyle: Int = 0,
    val polaroidFilter: Int = 0,
    /** v370 — quick-fact TEXT LAYOUT: how the fact body is composed on the
     *  card. STANDARD = the style's default paragraph. CONDENSED tightens
     *  the line spacing (words keep normal gaps). BOOK = a two-column
     *  book-page flow: the text breaks at the middle and continues on the
     *  other side. EDITORIAL = paragraph with a drop cap (see
     *  [factDropCap]). Applied per-style inside each card's fact render.
     */
    val factFormat: ShareCardFactFormat = ShareCardFactFormat.STANDARD,
    /** v370 — EDITORIAL drop-cap variant: NONE = no cap, LETTER = the first
     *  letter enlarges, WORD = the first word enlarges. Only read when
     *  [factFormat] is EDITORIAL (harmless otherwise). */
    val factDropCap: ShareCardFactDropCap = ShareCardFactDropCap.NONE,
    /** v371 — the auto-adjuster's FACT-font shrink, captured into the move on
     *  the first manual grab (the same handoff that seeds the box height) so
     *  handing auto-fit off doesn't make the text jump back to full size and
     *  clip mid-drag. 1f = full size. v379d — this is now the ONLY whole-fact
     *  text channel: the old v371 corner-zoom `factZoom` (orphaned since the
     *  corner grip was removed) was folded into this field on load and
     *  deleted, so the render multiplier is bodyScale × autoFit × factScale
     *  — no invisible phantom factor. */
    val factScale: Float = 1f,
    /** v379 — the BOOK-page fact layout's COLUMN GAP multiplier (1f = the
     *  default 12dp gutter between the two columns; 0.3f hugs them, 2.5f
     *  spreads the page). Read only when [factFormat] is BOOK. */
    val factGutter: Float = 1f,
    /** v371 — whole-fact UNDERLINE + HIGHLIGHT (rich-text-lite, per element). */
    val factUnderline: Boolean = false,
    val factHighlight: Color? = null,
    /** v371 — whole-title UNDERLINE + HIGHLIGHT. */
    val titleUnderline: Boolean = false,
    val titleHighlight: Color? = null,
    /** v376 — AUTO LIFT (dp): how far the TITLE block is pushed UP so a
     *  manually grown quick-fact box (height / width / whole-box sliders)
     *  clears it. Computed from the live measured boxes while editing and
     *  SAVED with the rest of the move, so the pushed layout persists to the
     *  sheet preview and the exported image; lowering the box again settles
     *  the title back down (the value tracks the needed clearance, not a
     *  one-way shove). The glued cover sits inside the title block, so it
     *  rides the lift automatically. 0f = no push. */
    val titleLift: Float = 0f,
    /** v380 — TRUE once the user has DRAGGED the title by hand. A hand-placed
     *  title owns its spot: the auto-lift effect never shoves it again, so
     *  the user may deliberately overlap the quick-fact box and the layout
     *  stays exactly where they left it (no bounce-back on release). Only a
     *  drag sets it — the FACT handle pushing the title out of its way while
     *  travelling does not, so a pushed-but-never-dragged title still gets
     *  auto-lifted when the fact box grows into it. Reset layout clears it. */
    val titlePlaced: Boolean = false,
    /** v3xx — POLAROID OPT-IN (non-Collage styles): the shared print on
     *  other styles only renders while this is TRUE (default FALSE — never
     *  always-on; the user turns it on per card in the Polaroid panel, and
     *  turns it off the same way to hide the print). Collage always renders
     *  its own inline print (part of the design, unaffected). Persisted with
     *  the move; Reset layout returns to the default (no print). */
    val polaroidOnCard: Boolean = false
)

/** v378 — layout-only reset for [ShareCardMove]: zeroes every POSITION
 *  (drags), box DIMENSION (width/height fractions + whole-box scales) and
 *  the auto LIFT — the stuff the Customise chrome moves/resizes — while
 *  KEEPING every text edit (fonts, aligns, bold/italic/underline/
 *  highlight, title size and fact layout/drop cap), so "Reset layout" never
 *  undoes the user's typography. v379d — the whole-fact text channel
 *  [factScale] CLEARS too: it is only ever written by the smart-fit handoff
 *  seed or the auto-layout pill (never by a user text slider — the Size
 *  tool drives bodyScale), so keeping it after Reset left the auto-shrunk
 *  text in place AND permanently disabled future smart fit (the seed reads
 *  as "manually touched"). Reset now returns the card to natural
 *  auto-fit behaviour. v380 — the hand-placed-title marker clears too, so
 *  the title goes back to riding the auto lift for grown fact boxes. */
private fun ShareCardMove.resetLayout(): ShareCardMove = ShareCardMove(
    titleFont = titleFont, factFont = factFont, metaFont = metaFont, badgeFont = badgeFont,
    titleAlign = titleAlign, factAlign = factAlign,
    titleBold = titleBold, titleItalic = titleItalic,
    factBold = factBold, factItalic = factItalic,
    metaBold = metaBold, metaItalic = metaItalic,
    badgeBold = badgeBold, badgeItalic = badgeItalic,
    titleUnderline = titleUnderline, titleHighlight = titleHighlight,
    factUnderline = factUnderline, factHighlight = factHighlight,
    titleScale = titleScale,
    factFormat = factFormat, factDropCap = factDropCap, factGutter = factGutter
)

/** Modifier that shifts a card's TITLE by the move offset + box size + scale and
 *  CROPS it to the box height (no-op when default). The crop uses
 *  fillMaxHeight+clipToBounds so the preview and the exported image match. */
private fun Modifier.moveTitle(m: ShareCardMove): Modifier {
    var mod = this
    if (m.titleScale != 1f) mod = mod.graphicsLayer { scaleX = m.titleScale; scaleY = m.titleScale }
    // v376 — the saved AUTO LIFT rides the title's own offset: the title
    // block (and any glued cover inside it) moves UP by [titleLift] dp so a
    // manually grown quick-fact box clears it. Applies everywhere (sheet
    // preview + export) because it's stored on the move.
    if (m.titleDx != 0f || m.titleDy != 0f || m.titleLift != 0f) mod = mod.offset(x = m.titleDx.dp, y = (m.titleDy - m.titleLift).dp)
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
    // v383 — the fact can grow PAST the design's content column: the Fact-width
    // slider now runs to 1.2x. fillMaxWidth caps at the column width, so past
    // 1.0 the body is measured at columnWidth x frac inside a custom layout and
    // recentred — the pane eats the side gutters that used to sit empty. At
    // <= 1.0 the layout is exactly the old fillMaxWidth behaviour (measured
    // against a maxWidth of target, child wraps naturally, placed at 0), so
    // every existing card renders pixel-for-pixel as before.
    val w = m.factWidthFrac.coerceIn(0.2f, 1.2f)
    mod = mod.layout { measurable, constraints ->
        val maxW = constraints.maxWidth
        val target = (maxW * w).roundToInt()
        if (target <= maxW) {
            val placeable = measurable.measure(
                androidx.compose.ui.unit.Constraints(
                    minWidth = 0, maxWidth = target,
                    minHeight = 0, maxHeight = constraints.maxHeight
                )
            )
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            val placeable = measurable.measure(
                androidx.compose.ui.unit.Constraints(
                    // v3xx — minWidth DROPPED (was = target): forcing the child
                    // to the full target width made a SHORT fact measure the
                    // whole wide box, so its one line hugged the box's left
                    // edge with a big gap on the right ("the text shifts left").
                    // The child now measures at its natural wrap width and is
                    // CENTERED inside the widened box below.
                    minWidth = 0, maxWidth = target,
                    minHeight = 0, maxHeight = constraints.maxHeight
                )
            )
            val over = (target - maxW) / 2
            // v3xx — center the CHILD within the widened box (the box itself
            // is already centred on the card): a short fact stays centred
            // instead of hugging the left; a full-width paragraph fills the
            // box exactly as before.
            val childShift = ((target - placeable.width) / 2f).roundToInt()
            layout(target, placeable.height) { placeable.place(-over + childShift, 0) }
        }
    }
    return mod
}

/** Shifts the card's INFO rows (byline, author, year, footer) by the meta
 *  offset — the M handle in edit mode. Info rows move but are never
 *  type-editable. */
private fun Modifier.moveMeta(m: ShareCardMove): Modifier {
    var mod = this
    if (m.metaDx != 0f || m.metaDy != 0f) mod = mod.offset(x = m.metaDx.dp, y = m.metaDy.dp)
    if (m.metaWidthFrac != 1f) mod = mod.fillMaxWidth(m.metaWidthFrac.coerceIn(0.2f, 1f))
    return mod
}

/** Shifts the card's category badge/pill by the badge offset — the B
 *  handle in edit mode. */
private fun Modifier.moveBadge(m: ShareCardMove): Modifier =
    if (m.badgeDx != 0f || m.badgeDy != 0f) this.offset(x = m.badgeDx.dp, y = m.badgeDy.dp) else this

/** v353 — shifts the album favorite-tracks strip by the fav offset — the
 *  strip drags exactly like the cover/jacket badge. */
private fun Modifier.moveFav(m: ShareCardMove): Modifier =
    if (m.favDx != 0f || m.favDy != 0f) this.offset(x = m.favDx.dp, y = m.favDy.dp) else this

/** v3xx — user title font/alignment/bold/italic override (null = style's own). */
private fun titleStyle(base: TextStyle, m: ShareCardMove): TextStyle {
    var s = base
    if (m.titleFont != null) s = s.copy(fontFamily = m.titleFont)
    if (m.titleAlign != null) s = s.copy(textAlign = m.titleAlign)
    if (m.titleBold) s = s.copy(fontWeight = FontWeight.Bold)
    if (m.titleItalic) s = s.copy(fontStyle = FontStyle.Italic)
    if (m.titleUnderline) s = s.copy(textDecoration = TextDecoration.Underline)
    if (m.titleHighlight != null) s = s.copy(background = m.titleHighlight)
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

/** v370b — how far the title block shifts RIGHT when a cover sits on the
 *  LEFT of the card: the cover width + a small gap, so the title clears the
 *  jacket exactly (v373 — derived from the cover size instead of the old
 *  fixed 108f for the 92dp cover, so the smaller book/album jackets sit
 *  snug instead of leaving a void). */

/** v316b — editor chrome: ONE uniform move grip + a darker coffee outline
 *  replace the old per-box letter handles (T/F/M/B) and their tinted
 *  borders. Coffee reads on the cream/white card surfaces. */
private val CoffeeChrome = Color(0xFF6D4C41)      // steady brown — unselected boxes
private val CoffeeChromeDeep = Color(0xFF3E2723)  // darker coffee — selected box / handle

/** Follows ONLY the title drag (dx/dy): an info row (author / year) sitting
 *  right under the title travels WITH it when the T handle drags — the M
 *  handle still nudges the row on its own afterwards. v376 — the auto LIFT
 *  rides too, so the byline stays glued under a lifted title. */
private fun Modifier.titleShift(m: ShareCardMove): Modifier =
    if (m.titleDx != 0f || m.titleDy != 0f || m.titleLift != 0f) this.offset(x = m.titleDx.dp, y = (m.titleDy - m.titleLift).dp) else this

/** v376 — moves a GLUED cover+title ROW: applies ONLY the title drag offset
 *  and the auto LIFT to the whole group, so the jacket rides the title
 *  (drag / collision / auto lift) exactly like the user asked. The title
 *  TEXT's own scale / width crop stay on the text (see [titleSize]) — a
 *  title font tweak must never stretch the cover with it. */
private fun Modifier.glueTitleMove(m: ShareCardMove): Modifier =
    if (m.titleDx != 0f || m.titleDy != 0f || m.titleLift != 0f) this.offset(x = m.titleDx.dp, y = (m.titleDy - m.titleLift).dp) else this

/** v376 — the TITLE TEXT half of the glued title block: font scale + width
 *  crop only (no offset — the Row owns the movement via [glueTitleMove]).
 *  Mirrors [moveTitle]'s size behaviour; width is applied unconditionally
 *  so the crop works inside the weighted Column (see [moveFact]). */
private fun Modifier.titleSize(m: ShareCardMove): Modifier {
    var mod = this
    if (m.titleScale != 1f) mod = mod.graphicsLayer { scaleX = m.titleScale; scaleY = m.titleScale }
    return mod.fillMaxWidth(m.titleWidthFrac.coerceIn(0.2f, 1f))
}

/** Follows ONLY the fact drag (dx/dy): the thin rules some styles draw ABOVE
 *  the quick fact slide along with the box instead of floating where the
 *  fact used to be. */
private fun Modifier.factShift(m: ShareCardMove): Modifier =
    if (m.factDx != 0f || m.factDy != 0f) this.offset(x = m.factDx.dp, y = m.factDy.dp) else this

/** Resolves the fact-text style with the user's format (font + align +
 *  bold/italic). The format is stored on [ShareCardMove] so every style AND
 *  the exported image honor it automatically.
 *  v370b — CONDENSED: tighter spacing BETWEEN LINES (line height ~18% less
 *  + the lines pulled together slightly), words keep their normal gaps. */
private fun factBodyStyle(base: TextStyle, m: ShareCardMove): TextStyle {
    var s = base
    if (m.factFont != null) s = s.copy(fontFamily = m.factFont)
    if (m.factAlign != null) s = s.copy(textAlign = m.factAlign)
    if (m.factBold) s = s.copy(fontWeight = FontWeight.Bold)
    if (m.factItalic) s = s.copy(fontStyle = FontStyle.Italic)
    if (m.factUnderline) s = s.copy(textDecoration = TextDecoration.Underline)
    if (m.factHighlight != null) s = s.copy(background = m.factHighlight)
    if (m.factFormat == ShareCardFactFormat.CONDENSED) {
        // v371 — this Compose version has no TextStyle.paragraphStyle /
        // lineSpacing params (they landed in a later release), so the
        // tight spacing is done purely via the line-height factor.
        val lh = s.lineHeight
        if (lh.isSp) s = s.copy(lineHeight = (lh.value * 0.80f).sp)
    }
    return s
}

/** v370b — the fact body renderer shared by every style: honors the
 *  picker's fact LAYOUT. STANDARD/CONDENSED render as a plain paragraph
 *  (CONDENSED's tighter line spacing already lives in [factBodyStyle]);
 *  BOOK renders as a two-column book-page flow (see [BookPageText]);
 *  EDITORIAL as a drop-cap paragraph (see [EditorialDropCapBlock]). The
 *  caller's modifier (moveFact + bounds reporting) rides the same node so
 *  the preview and the exported image match. */
// v375 — translucent amber marker for span HIGHLIGHTS on the card's fact
// text (Save-your-take's paper-amber highlighter idea, tuned a touch deeper
// so it reads over the cream card surfaces too).
private val ShareFactMarker = Color(0x7DF2B70A)

/**
 * v375 — slices [spans] (offsets into the FULL [text]) down to a substring
 * piece [from, to) (with [trimStart] leading chars of that slice dropped,
 * e.g. after a `.trimStart()` split) and rebuilds an AnnotatedString for the
 * piece. The BOOK two-column and EDITORIAL drop-cap flows split the fact
 * into separate Texts, so each piece gets the runs that land on it.
 * Returns null when no span lands on the piece → callers render the plain
 * substring exactly as before.
 */
private fun richSlice(
    text: String,
    spans: List<TextSpan>,
    marker: Color,
    from: Int,
    to: Int,
    trimStart: Int = 0
): AnnotatedString? {
    val s = from + trimStart
    if (s >= to || spans.isEmpty()) return null
    val shifted = spans.mapNotNull { sp ->
        val a = maxOf(sp.start, s)
        val b = minOf(sp.end, to)
        if (b > a) TextSpan(a - s, b - s, sp.bold, sp.italic, sp.highlight, sp.fontSizeSp, sp.underline) else null
    }
    return if (shifted.isEmpty()) null
    else buildRichAnnotated(text.substring(s, to), shifted, marker)
}

/** v375 — serializes the share card's fact spans for saveShareCardEdits. */
private fun spansToJson(spans: List<TextSpan>): org.json.JSONArray {
    val arr = org.json.JSONArray()
    spans.forEach { sp ->
        val o = org.json.JSONObject()
        o.put("s", sp.start)
        o.put("e", sp.end)
        if (sp.bold) o.put("b", true)
        if (sp.italic) o.put("i", true)
        if (sp.highlight) o.put("h", true)
        if (sp.underline) o.put("u", true)
        sp.fontSizeSp?.let { o.put("fs", it.toDouble()) }
        arr.put(o)
    }
    return arr
}

/**
 * v375 — shifts span offsets by [delta] (positive → later in the text).
 * The CARD prepends the "CH n · title" chip line to a chapter review's
 * text, so the runs the editor wrote against the bare note must move past
 * that prefix before FactBody renders the full text. 0 → the spans are
 * returned untouched (no allocation churn on the common no-chip paths).
 */
private fun shiftSpans(spans: List<TextSpan>, delta: Int): List<TextSpan> {
    if (delta == 0 || spans.isEmpty()) return spans
    return spans.map { it.copy(start = it.start + delta, end = it.end + delta) }
}

/** v375 — parses the persisted fact spans (null/empty → no spans). */
private fun spansFromJson(arr: org.json.JSONArray?): List<TextSpan> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val s = o.optInt("s", 0)
        val e = o.optInt("e", 0)
        if (e <= s) null
        else TextSpan(
            s, e,
            o.optBoolean("b", false),
            o.optBoolean("i", false),
            o.optBoolean("h", false),
            o.optDouble("fs", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
            o.optBoolean("u", false)
        )
    }
}

@Composable
private fun FactBody(
    text: String,
    style: TextStyle,
    format: ShareCardFactFormat,
    dropCap: ShareCardFactDropCap,
    aspect: ShareCardAspect,
    maxLines: Int,
    // v379 — the BOOK-page two-column layout's gutter multiplier (1f = the
    // default 12dp gap; ignored by every other layout).
    gutterFrac: Float = 1f,
    // v375 — rich-text runs over [text] (bold/italic/highlight/underline).
    // Empty = the plain paragraph exactly as before; [marker] is the
    // highlighter tone.
    spans: List<TextSpan> = emptyList(),
    marker: Color = ShareFactMarker,
    modifier: Modifier = Modifier
) {
    when (format) {
        ShareCardFactFormat.BOOK -> BookPageText(text, spans, marker, style, gutterFrac, modifier, maxLines)
        ShareCardFactFormat.EDITORIAL -> EditorialDropCapBlock(
            text, spans, marker, style, dropCap, modifier = modifier, maxLines = maxLines
        )
        else -> {
            if (spans.isEmpty()) {
                Text(text, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier)
            } else {
                // Styled runs render over the same paragraph style — the
                // preview AND the exported image use this identical path.
                Text(
                    buildRichAnnotated(text, spans, marker),
                    style = style,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier
                )
            }
        }
    }
}

/** v370b — BOOK PAGE: the fact breaks in the MIDDLE and continues on the
 *  other side like a book spread — the left column holds the first half of
 *  the lines, the right column the rest. The split is measured at the real
 *  column width (on-screen px, so preview + export match), lands on a word
 *  edge and never overlaps. Both columns share one [maxLines] budget, like
 *  the plain paragraph. */
@Composable
private fun BookPageText(
    text: String,
    // v375 — span support: the split pieces re-slice the runs (see
    // [richSlice]) so bold/italic/highlight/underline survive the two-column
    // flow.
    spans: List<TextSpan>,
    marker: Color,
    style: TextStyle,
    // v379 — column-gap multiplier (1f = the default 12dp gutter).
    gutterFrac: Float = 1f,
    modifier: Modifier = Modifier,
    maxLines: Int
) {
    BoxWithConstraints(modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val measurer = rememberTextMeasurer()
        val contentW = with(density) { maxWidth.toPx() }
        val gapDp = 12.dp * gutterFrac.coerceIn(0.2f, 3f)
        val gap = with(density) { gapDp.toPx() }
        val colW = ((contentW - gap) / 2f).toInt().coerceAtLeast(1)
        val cap = maxLines.coerceAtLeast(1)
        val full = measurer.measure(
            text = AnnotatedString(text),
            style = style,
            softWrap = true,
            overflow = TextOverflow.Clip,
            maxLines = cap,
            constraints = Constraints(maxWidth = colW)
        )
        val totalLines = full.lineCount
        val leftLines = ((totalLines + 1) / 2).coerceIn(1, cap)
        val left = measurer.measure(
            text = AnnotatedString(text),
            style = style,
            softWrap = true,
            overflow = TextOverflow.Clip,
            maxLines = leftLines,
            constraints = Constraints(maxWidth = colW)
        )
        val split = left.getLineEnd((left.lineCount - 1).coerceAtLeast(0))
        val leftText = text.take(split)
        val rawRight = text.drop(split)
        val rightText = rawRight.trimStart()
        val trimmed = rawRight.length - rightText.length
        Row(Modifier.fillMaxWidth()) {
            val leftAnn = richSlice(text, spans, marker, 0, split)
            if (leftAnn != null) {
                Text(
                    leftAnn, style = style, maxLines = leftLines,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    leftText, style = style, maxLines = leftLines,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.width(gapDp))
            val rightAnn = richSlice(text, spans, marker, split, text.length, trimStart = trimmed)
            if (rightAnn != null) {
                Text(
                    rightAnn, style = style,
                    maxLines = (cap - leftLines).coerceAtLeast(1),
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    rightText, style = style,
                    maxLines = (cap - leftLines).coerceAtLeast(1),
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** v370b — EDITORIAL paragraph: a drop-cap block (big first LETTER or big
 *  first WORD per [dropCap]; NONE = the plain paragraph) on ANY card style.
 *  The cap spans two body lines and the text wraps around it; when the
 *  wrapped block is full the rest continues full-width below (the same
 *  measured split as the EditorialCard design, hoisted so every style can
 *  wear it). */
@Composable
private fun EditorialDropCapBlock(
    text: String,
    // v375 — span support: the wrapped head + tail pieces re-slice the runs
    // (see [richSlice]) so bold/italic/highlight survive the drop-cap flow.
    spans: List<TextSpan>,
    marker: Color,
    bodyStyle: TextStyle,
    dropCap: ShareCardFactDropCap,
    capColor: Color? = null,
    modifier: Modifier = Modifier,
    maxLines: Int
) {
    if (dropCap == ShareCardFactDropCap.NONE || text.isBlank()) {
        if (spans.isEmpty()) {
            Text(text, style = bodyStyle, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier)
        } else {
            Text(
                buildRichAnnotated(text, spans, marker),
                style = bodyStyle, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier
            )
        }
        return
    }
    BoxWithConstraints(modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val measurer = rememberTextMeasurer()
        val contentW = with(density) { maxWidth.toPx() }
        val bodyLineH = bodyStyle.lineHeight
        val bodySize = bodyStyle.fontSize
        val initial = when (dropCap) {
            ShareCardFactDropCap.WORD -> text.trimStart().split(" ", limit = 2).firstOrNull() ?: text
            else -> text.take(1)
        }
        val bodyRest = text.drop(initial.length)
        val base = initial.length
        // The drop-cap initial spans 2 body lines. Its lineHeight is set to
        // 2× one body line so the initial column height EXACTLY matches the
        // 2 wrap lines beside it; Top alignment anchors the glyph to the TOP
        // of its 2-line box (centering would push the letter down).
        val initialFontMult = if (dropCap == ShareCardFactDropCap.WORD) 1.55f else 2.0f
        val initialStyle = bodyStyle.copy(
            fontWeight = FontWeight.Bold,
            fontSize = (bodySize.value * initialFontMult).sp,
            lineHeight = (bodyLineH.value * 2f).sp,
            color = capColor ?: bodyStyle.color,
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
        val initColW = with(density) { (initialW + gap).toDp() }
        val cap = maxLines.coerceAtLeast(1)
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                if (initial.isNotEmpty()) {
                    Text(
                        initial, style = initialStyle,
                        modifier = Modifier
                            .width(initColW)
                            .padding(end = 6.dp)
                    )
                }
                if (wrapText.isNotEmpty()) {
                    val wrapAnn = richSlice(text, spans, marker, base, base + wrapEnd)
                    if (wrapAnn != null) {
                        Text(wrapAnn, style = bodyStyle, maxLines = 2,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f))
                    } else {
                        Text(wrapText, style = bodyStyle, maxLines = 2,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f))
                    }
                }
            }
            if (restText.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                val restAnn = richSlice(text, spans, marker, base + wrapEnd, text.length)
                if (restAnn != null) {
                    Text(
                        restAnn, style = bodyStyle,
                        maxLines = (cap - 2).coerceAtLeast(1),
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        restText, style = bodyStyle,
                        maxLines = (cap - 2).coerceAtLeast(1),
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
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
    // v335 — the book-cover jacket badge reports its bounds so the editor
    // can select + drag it like the other movable elements.
    val onCover: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    // v353 — the album favorite-tracks strip reports its bounds so the
    // editor can select + drag it like the cover/jacket.
    val onFavTrack: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    // v3xx — the COLLAGE polaroid reports its bounds so the editor can
    // select + drag it like the cover/jacket.
    val onPolaroid: (androidx.compose.ui.geometry.Rect) -> Unit = {},
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

/** v335 — the fact/body text cap scaled to the CURRENT font multiplier so
 *  the fact box keeps its footprint when only the TEXT size changes. The
 *  card lays the fact box out at the DEFAULT body size (bodyScale = 1f);
 *  shrinking the font (0.5×) used to shrink the box with it (the cap stayed
 *  at the default line count, so the paragraph still didn't fit and the box
 *  looked smaller), and growing it overflowed. Reserving the same pixel
 *  height and letting the LINE COUNT move inversely keeps the box stable:
 *  smaller text fits MORE lines in the same box, larger text fewer. */
private fun fitLines(base: Int, frac: Float, fontScale: Float, max: Int = 48): Int =
    kotlin.math.ceil(lines(base, frac, max) / fontScale.coerceIn(0.25f, 4f)).toInt().coerceIn(1, max)

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
    // v334 — BOOK share cards: the authored/fetched cover. Rendered as a
    // small jacket badge at the top-right on every style (Collage feeds it
    // into the polaroid photo slot instead, so the two never double up).
    bookCover: androidx.compose.ui.graphics.ImageBitmap? = null,
    // v373 — ALBUM covers are SQUARE artwork (1:1) while books/series
    // posters are 2:3 portraits, so the jacket badge must not stretch them
    // to the same rectangle: true renders the cover square (66×66), false
    // keeps the 2:3 book-jacket shape (44×66).
    isSquareCover: Boolean = false,
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
    // v335 — when the user stacks a CUSTOM FACT under the Reading-progress
    // widget, this carries that text (rendered below the bar by every
    // style). Blank = progress only, no prose under the bar.
    chapterFact: String = "",
    // v375 — rich-text runs (bold/italic/highlight) over the FACT body:
    // offsets are into the fact text the card renders ([factText] with any
    // [editedFact] applied). Empty = plain rendering exactly as before. The
    // runs render through every style AND the exported image (same path).
    factSpans: List<TextSpan> = emptyList(),
    // v3xx — EMOJI STICKERS placed in the full-screen editor: rendered as a
    // top layer over every style so the sheet preview and the exported image
    // match the editor exactly. Empty = no stickers (nothing changes).
    stickers: List<ShareSticker> = emptyList(),
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks()
) {
    // v324/v369 — the Adjust tool's saturation/contrast now applies to the
    // card's BACKGROUND only (never the text, the polaroid or the book
    // cover): the matrix is threaded into each style and applied on its
    // background layer, so the preview AND the exported image match.
    val bgFilter = if (saturation == 1f && contrast == 1f) null
    else ColorFilter.colorMatrix(adjustColorMatrix(saturation, contrast))
    val display = topicName.substringBeforeLast(" (")
    // v373 — cover badge size per family: books and series posters are 2:3
    // portraits (44×66), album art is square (66×66) so it never stretches.
    val coverW = if (isSquareCover) 66.dp else 44.dp
    val coverH = 66.dp
    // v369 — SMART AUTO-FIT: long quick/custom fact text auto-shrinks
    // (per-style font steps), auto-GROWS the fact box and nudges it (plus
    // the info rows that travel with it) up so the text fits the tall card.
    // The title lifts only as far as each style's room allows (never over a
    // pill, masthead, byline or the card edge) and a long title shrinks when
    // the fact needs the space. Manual edits of the fact box hand it over
    // entirely ("manual wins"); a manually placed title keeps its spot.
    val factWrap = rememberFactWrapLines(editedFact ?: factText, chapterFact, aspect)
    val autoFit = smartAutoFitDelta(move, factWrap, style, aspect)
    // v374 — the smart fit adjusts ONLY the fact box (height + width) and
    // the fact TEXT (bodyScale) — the same channels the box/size sliders
    // drive — so there is no hidden nudge or font multiplier.
    val effectiveMove = if (autoFit.heightFrac == 1f && autoFit.widthFrac == 1f) move
    else move.copy(
        factHeightFrac = move.factHeightFrac * autoFit.heightFrac,
        // v383 — the width cap rises to 1.2 so a manual Fact-width setting
        // survives smart-fit growth (auto growth itself stays <= 1, and the
        // untouched-fact seed below still clamps at 1).
        factWidthFrac = (move.factWidthFrac * autoFit.widthFrac).coerceIn(0.2f, 1.2f)
    )
    // v373 — the INDEPENDENT whole-box scale multiplies BOTH dimensions on
    // top of the width/height fractions (and any auto-fit growth), so the
    // Whole-box slider scales the box as one without re-reading the width
    // or height values. v383 — width now clamps at the new 1.2x cap (the
    // Fact-width slider runs past the design column into the side gutters).
    val boxScaledMove = effectiveMove.copy(
        titleWidthFrac = (effectiveMove.titleWidthFrac * move.titleBoxScale).coerceIn(0.2f, 1f),
        titleHeightFrac = effectiveMove.titleHeightFrac * move.titleBoxScale,
        factWidthFrac = (effectiveMove.factWidthFrac * move.factBoxScale).coerceIn(0.2f, 1.2f),
        factHeightFrac = effectiveMove.factHeightFrac * move.factBoxScale,
        favWidthFrac = (effectiveMove.favWidthFrac * move.favBoxScale).coerceIn(0.3f, 1.2f),
        favHeightFrac = effectiveMove.favHeightFrac * move.favBoxScale
    )
    // v370b — COVER SIDE LAYOUT: when a cover (book / album / series) is on
    // the card, it sits on the LEFT like the synopsis page — the title (and
    // the author/year under it, which follows via titleShift) shifts right
    // into the remaining width, wraps a little narrower and auto-shrinks so
    // everything aligns without overlap. Removing the cover (or having none)
    // restores the per-style corner pockets.
    // v373 — the shift / width crop / title shrink are DERIVED from the
    // cover's real width (44dp book/series jacket, 66dp square album) so a
    // smaller cover hugs the title instead of leaving the old 92dp void.
    // v376 — GLUED side layout: Paper / Vinyl / Clean / Editorial / Minimal
    // render the cover INSIDE their own layout as the leading item of the
    // title block (see [GluedCover] at each style's title area), so the
    // jacket always sits exactly beside the title wherever the design's flow
    // puts it — and it rides every title move (drag, collision, auto lift)
    // automatically. Those styles need no synthetic dx shift here; the
    // remaining styles (Signature / Custom) still use the overlay shift.
    val coverActive = bookCover != null && style != ShareCardStyle.COLLAGE
    val glueCoverStyle = style == ShareCardStyle.PAPER || style == ShareCardStyle.VINYL ||
        style == ShareCardStyle.NEUMORPHIC || style == ShareCardStyle.EDITORIAL ||
        style == ShareCardStyle.MINIMAL || style == ShareCardStyle.SIGNATURE
    val coverSideShift = coverW.value + 16f
    val coverTitleWidthFactor =
        ((aspect.widthDp - coverW.value - 16f) / aspect.widthDp).coerceIn(0.6f, 0.95f)
    val coverTitleScale = (0.90f + (92f - coverW.value) / 92f * 0.06f).coerceIn(0.9f, 0.97f)
    val layoutMove = if (coverActive && !glueCoverStyle) boxScaledMove.copy(
        titleDx = boxScaledMove.titleDx + coverSideShift,
        titleWidthFrac = (boxScaledMove.titleWidthFrac * coverTitleWidthFactor).coerceIn(0.2f, 1f),
        titleScale = boxScaledMove.titleScale * coverTitleScale
    ) else boxScaledMove
    // v374 — the smart fit's text shrink applies on top of the user's
    // body-size setting THROUGH the same bodyScale channel (there is no
    // separate font adjuster): every style renders its fact text at
    // (style base × bodyScale × fit.textScale). Once the user grabs the
    // fact, the shrink is captured into move.factScale (see the first-grab
    // seed) so the handoff doesn't make the text jump or clip.
    // v379d — factZoom is gone: whole-fact text = base × fit × factScale.
    val effectiveBodyScale = bodyScale * autoFit.textScale * move.factScale
    // Extract year from trailing parentheses — "Appetite for Destruction (1987)" → "1987"
    val year = topicName.substringAfterLast("(").substringBeforeLast(")").takeIf { it.all { c -> c.isDigit() } && it.length == 4 }
    val palette = paletteFor(accent, toneIndex)
    // Resolve per-share text edits once — every style renders the same values,
    // and quote cards show the edited quote as both the body and the quote.
    val shownDisplay = editedTitle ?: display
    val shownFact = editedFact ?: factText
    val shownQuote = quoteText?.let { shownFact }
    // v336 — the album's heart-picked favorite tracks (multi-select hearts in
    // the album track-list sheet). Read reactively so the Vinyl card strip
    // updates the moment a heart is toggled; empty for non-album topics.
    val albumFavTracks = AppPreferences.albumFavTracksState[topicName].orEmpty()
    // v376 — GLUED cover styles receive the cover artwork and render it
    // INSIDE their title block (see each style's title area): the jacket
    // then always sits beside the title and rides every title move. The
    // non-glued styles (Signature/Custom) keep the overlay slot below.
    val gluedCover = if (coverActive && glueCoverStyle) bookCover else null
    Box {
        when (style) {
            ShareCardStyle.PAPER -> PaperCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, modifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, effectiveBodyScale, callbacks, layoutMove, chapterProgress, chapterFact, bgFilter, factSpans = factSpans, coverArt = gluedCover, coverW = coverW, coverH = coverH)
            ShareCardStyle.VINYL -> VinylCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, modifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, effectiveBodyScale, callbacks, layoutMove, chapterProgress, chapterFact, hideTypedFavSong = albumFavTracks.isNotEmpty() && AppPreferences.albumFavStripVisibleState, bgFilter, factSpans = factSpans, coverArt = gluedCover, coverW = coverW, coverH = coverH)
            ShareCardStyle.COLLAGE -> CollageCard(shownDisplay, topicName, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, modifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, userPhoto ?: bookCover, byline, year, polaroidCaption, onPhotoTap, effectiveBodyScale, callbacks, layoutMove, chapterProgress, chapterFact, bgFilter, factSpans = factSpans)
            ShareCardStyle.NEUMORPHIC -> NeumorphicCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, modifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, effectiveBodyScale, callbacks, layoutMove, chapterProgress, chapterFact, bgFilter, factSpans = factSpans, coverArt = gluedCover, coverW = coverW, coverH = coverH)
            ShareCardStyle.EDITORIAL -> EditorialCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, modifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, effectiveBodyScale, callbacks, layoutMove, chapterProgress, chapterFact, bgFilter, factSpans = factSpans, coverArt = gluedCover, coverW = coverW, coverH = coverH)
            ShareCardStyle.MINIMAL -> MinimalCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, modifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, effectiveBodyScale, callbacks, layoutMove, chapterProgress, chapterFact, bgFilter, factSpans = factSpans, coverArt = gluedCover, coverW = coverW, coverH = coverH,
                // v3xx — FAVORITES ROOM: when the (visible) album favorites
                // strip is on this card, its bottom-left slot reserves space
                // under the fact so the list-style strip never overlaps the
                // fact's lower lines (smart fit + default fit both render
                // clear by construction). Chips mode is taller than the list,
                // so it reserves a little more.
                favReserveDp = if (style == ShareCardStyle.MINIMAL &&
                    albumFavTracks.isNotEmpty() && AppPreferences.albumFavStripVisibleState
                ) (if (AppPreferences.albumFavRowsState) 96.dp else 76.dp) else 0.dp)
            ShareCardStyle.SIGNATURE -> SignatureCard(shownDisplay, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, modifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, classicSignature, effectiveBodyScale, callbacks, layoutMove, chapterProgress, chapterFact, bgFilter, factSpans = factSpans, coverArt = gluedCover, coverW = coverW, coverH = coverH)
            ShareCardStyle.CUSTOM -> CustomCard(shownDisplay, topicName, categoryName, categoryGlyph, palette, shownFact, sharerName, aspect, modifier, ratingStars, categoryFamily, shownQuote, quoteAuthor, byline, year, effectiveBodyScale, callbacks, layoutMove, chapterProgress, chapterFact, bgFilter, factSpans = factSpans)
        }
        // v3xx — the POLAROID on every style: Collage draws its own inline
        // print (part of the scrapbook design); the other styles wear the
        // SAME movable print once the user turns it on for this card
        // (move.polaroidOnCard — default FALSE, so the print is never
        // always-on; the Polaroid panel's "Show on card" switch shows/hides
        // it). A photo is NOT required: without one the print renders its
        // empty frame with a "Tap to add photo" hint, exactly like Collage.
        // Style / filter / size / drag controls work everywhere. Defaults
        // to the right side, upper-middle (Collage parks it higher; here
        // the top-right usually holds the cover pocket or headline art).
        if (style != ShareCardStyle.COLLAGE && move.polaroidOnCard) {
            BoxWithConstraints(Modifier.matchParentSize()) {
                val cwV = maxWidth.value; val chV = maxHeight.value
                if (cwV <= 0f || chV <= 0f) return@BoxWithConstraints
                val pStyleV = move.polaroidStyle.coerceIn(0, polaroidLooks.lastIndex)
                val pFilterV = move.polaroidFilter.coerceIn(0, 4)
                val lookV = polaroidLooks[pStyleV]
                val basePW = (cwV * 0.34f * move.polaroidScale).coerceIn(cwV * 0.20f, cwV * 0.44f)
                // v3xx — the caption band must NEVER clip the handwritten
                // name: the caption sits 6dp below the photo with a 11–15sp
                // line (×1.2), so the band is the max of the proportional
                // look (20% of print width) and that FIXED need (~19–24dp).
                // The old purely-proportional band shrank on narrow/small
                // prints and the caption spilled past the print's bottom
                // edge — "cut from bottom".
                val capFont = (basePW * 0.065f).coerceIn(11f, 15f)
                val capH = maxOf(basePW * 0.20f, 6f + capFont * 1.2f)
                var pW = basePW
                var pH = basePW * 1.18f
                if (userPhoto != null && userPhoto.width > 0 && userPhoto.height > 0) {
                    val ar = userPhoto.width.toFloat() / userPhoto.height.toFloat()
                    pH = pW / ar + capH
                    val maxH = chV * 0.55f
                    if (pH > maxH) {
                        pW = ((maxH - capH) * ar).coerceAtMost(basePW)
                        pH = pW / ar + capH
                    }
                    pW = pW.coerceAtMost(cwV * 0.44f)
                }
                val photoH = (pH - capH).coerceAtLeast(pW * 0.4f)
                val pX = (cwV * 0.60f + move.polaroidDx).coerceIn(0f, (cwV - pW - 6f).coerceAtLeast(0f))
                // v3xx — 5dp top inset: the washi tape pokes 5dp ABOVE the
                // frame, so a print clamped to y=0 had its tape sliced by
                // the card's rounded clip (same "cut" family as the caption).
                val pY = (chV * 0.30f + move.polaroidDy).coerceIn(5f, (chV - pH - 6f).coerceAtLeast(5f))
                PolaroidPrint(
                    pW = pW, pH = pH, pX = pX, pY = pY, photoH = photoH,
                    look = lookV, pStyle = pStyleV, pFilter = pFilterV,
                    userPhoto = userPhoto,
                    caption = polaroidCaption.ifBlank {
                        if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio"
                    },
                    captionInk = if (pStyleV == polaroidLooks.lastIndex) Color(0xFFF0ECE4) else Color(0xFF40301F),
                    onPhotoTap = onPhotoTap, callbacks = callbacks
                )
            }
        }
        // v334 — the cover badge rides on top of every style EXCEPT Collage
        // (there it feeds the polaroid photo slot above).
        // v335 — the jacket is a movable element like the badge: its offset
        // comes from [move.coverDx]/[move.coverDy] and it reports its bounds
        // so the editor's chrome can select + drag it.
        // v340 — the DEFAULT pocket is per-style: each design anchors the
        // jacket where ITS top-right art (or headline/crest) leaves a gap,
        // so a fresh cover lands in that style's natural spot instead of
        // always overlapping the same corner decoration. The user can still
        // drag it anywhere (move offsets apply on top).
        if (bookCover != null && style != ShareCardStyle.COLLAGE && !glueCoverStyle) {
            // v370b — COVER SIDE LAYOUT (cover present): the cover anchors
            // LEFT, aligned with each design's title block (Clean centres
            // its title, so the cover centres beside it). The default per-
            // style corner pockets only apply when NO cover is on the card.
            // v376 — only the non-glued styles (Signature / Custom) reach
            // this overlay now; the glued styles render their own cover.
            val coverSlot = if (coverActive) when (style) {
                ShareCardStyle.NEUMORPHIC -> Alignment.CenterStart to PaddingValues(start = 18.dp)
                ShareCardStyle.EDITORIAL -> Alignment.TopStart to PaddingValues(top = 64.dp, start = 18.dp)
                ShareCardStyle.MINIMAL -> Alignment.TopStart to PaddingValues(top = 74.dp, start = 18.dp)
                // v381 — Signature is GLUED now (the jacket lives in its own
                // title block per layout); only Custom uses the overlay.
                ShareCardStyle.CUSTOM -> Alignment.TopStart to PaddingValues(top = 36.dp, start = 18.dp)
                else -> Alignment.TopStart to PaddingValues(top = 24.dp, start = 18.dp)
            } else when (style) {
                ShareCardStyle.PAPER -> Alignment.TopEnd to PaddingValues(top = 64.dp, end = 16.dp)
                ShareCardStyle.VINYL -> Alignment.TopEnd to PaddingValues(top = 70.dp, end = 18.dp)
                // Clean wears a huge rotated glyph top-right: drop the jacket
                // BELOW it instead of over the category art.
                ShareCardStyle.NEUMORPHIC -> Alignment.TopEnd to PaddingValues(top = 186.dp, end = 20.dp)
                // Editorial: masthead rules + headline own the top; the
                // bottom-right sits beside the colophon's left-aligned slug.
                ShareCardStyle.EDITORIAL -> Alignment.BottomEnd to PaddingValues(bottom = 26.dp, end = 18.dp)
                ShareCardStyle.MINIMAL -> Alignment.TopEnd to PaddingValues(top = 48.dp, end = 16.dp)
                // Custom keeps the overlay corner pocket (no crest/art there).
                ShareCardStyle.CUSTOM -> Alignment.TopEnd to PaddingValues(top = 56.dp, end = 16.dp)
                ShareCardStyle.COLLAGE -> Alignment.TopEnd to PaddingValues(top = 34.dp, end = 14.dp)
            }
            BookCoverBadge(
                cover = bookCover,
                // v373 — the cover renders at its natural shape (2:3
                // book/series jacket, 1:1 album square) instead of the old
                // 92×136 stretch that distorted square album art.
                coverWidth = coverW,
                coverHeight = coverH,
                modifier = Modifier
                    .align(coverSlot.first)
                    .offset(x = move.coverDx.dp, y = move.coverDy.dp)
                    .padding(coverSlot.second),
                callbacks = callbacks
            )
        }
        // v337 — heart-picked album tracks render as a per-style corner
        // sticker above every style. v340 — each design wears its OWN visual
        // tokens + corner (see FavoriteTracksBadge) so the strip belongs to
        // the card instead of reading as a vinyl sticker everywhere. Empty
        // for non-album topics, so other cards never change.
        // v353 — the strip hides entirely when the user turns it off in the
        // share editor (global preference, default on).
        if (albumFavTracks.isNotEmpty() && AppPreferences.albumFavStripVisibleState) {
            // v353 — per-style POSITIONING pass: the strip parks where each
            // design actually leaves room (Minimal moved off the top-end
            // where its glyph + the cover jacket sit; Editorial cleared the
            // colophon slug; the rest nudge clear of footers).
            val favSlot = when (style) {
                ShareCardStyle.NEUMORPHIC -> Alignment.TopStart to
                    PaddingValues(top = 88.dp, start = 22.dp, end = 22.dp)
                ShareCardStyle.EDITORIAL -> Alignment.BottomStart to
                    PaddingValues(start = 28.dp, bottom = 80.dp)
                ShareCardStyle.MINIMAL -> Alignment.BottomStart to
                    PaddingValues(start = 26.dp, bottom = 34.dp)
                // Vinyl: bottom-start is where the typed song chip lives; it
                // is suppressed (below) so the album strip owns that corner.
                ShareCardStyle.VINYL, ShareCardStyle.PAPER -> Alignment.BottomStart to
                    PaddingValues(start = if (aspect == ShareCardAspect.CLASSIC) 14.dp else 18.dp, bottom = if (aspect == ShareCardAspect.CLASSIC) 28.dp else 26.dp)
                // v384 — COLLAGE favorites moved OUT of the dark bottom band
                // into the free middle. v3xx — raised a touch more, so the
                // strip parks just below the title/author rows and its LAST
                // track row no longer slides over the torn seam into the dark
                // band (the old 112dp top put the strip's bottom right ON the
                // seam on 3:4 cards, splitting the dark text over the two
                // collage tones — "divided"): fully on the cream top paper
                // now, clear of the seam + the category pill below.
                ShareCardStyle.COLLAGE -> Alignment.TopStart to
                    PaddingValues(start = 22.dp, top = if (aspect == ShareCardAspect.CLASSIC) 100.dp else 98.dp)
                // v3xx — SIGNATURE favorites moved from the bottom corner
                // (where they overlapped the bottom-anchored quick fact) up
                // to just below the title/author block — the strip now reads
                // as part of the headline group instead of fighting the body
                // text. Still movable; the sparkle repairs any residual
                // overlap on tap.
                ShareCardStyle.SIGNATURE -> Alignment.TopStart to
                    PaddingValues(start = 22.dp, top = if (aspect == ShareCardAspect.CLASSIC) 138.dp else 130.dp)
                // Custom keeps the bottom corner pocket (its own tone-following
                // ink + layout; only Signature moved up).
                ShareCardStyle.CUSTOM -> Alignment.BottomStart to
                    PaddingValues(start = if (aspect == ShareCardAspect.CLASSIC) 18.dp else 22.dp, bottom = if (aspect == ShareCardAspect.CLASSIC) 98.dp else 94.dp)
            }
            // v3xx — SIGNATURE favorites wear the design's OWN body ink
            // (each signature scene tunes its text colour for its background;
            // the tone's palette.ink can clash — e.g. near-black ink on
            // Mario's red or Pikachu's yellow). Collage keeps the tone ink
            // (it now parks on the cream top paper) with stronger alphas.
            val sigFavInk = if (style == ShareCardStyle.SIGNATURE) {
                (if (classicSignature) signatureDesignClassic(categoryName, categoryFamily)
                else signatureDesign(categoryName, categoryFamily)).bodyColor
            } else null
            FavoriteTracksBadge(
                tracks = albumFavTracks,
                style = style,
                palette = palette,
                classic = aspect == ShareCardAspect.CLASSIC,
                // v370 — the strip's box-size fractions ride on the move so
                // each style keeps its own width/height edits; v373 — the
                // whole-box scale multiplies on top. v384 — the explicit song
                // count (0 = auto), the no-fact expansion flag and the global
                // list/rows mode ride along too. v3xx — the signature ink
                // override rides along (null = palette ink, as before).
                widthFrac = boxScaledMove.favWidthFrac,
                heightFrac = boxScaledMove.favHeightFrac,
                count = move.favCount,
                noFact = shownFact.isBlank() && shownQuote == null,
                rowsMode = AppPreferences.albumFavRowsState,
                inkOverride = sigFavInk,
                // v353 — the strip is a movable element like the cover: its
                // offset comes from [move.favDx]/[move.favDy] and it reports
                // its bounds so the editor can select + drag it.
                modifier = Modifier
                    .align(favSlot.first)
                    .padding(favSlot.second)
                    .moveFav(move)
                    .onGloballyPositioned { callbacks.onFavTrack(it.boundsInWindow()) }
            )
        }
        // v3xx — EMOJI / IMAGE STICKER layer: drawn LAST so stickers always
        // sit on top of the card content, the cover badge and the favorites
        // strip. Positions/sizes are card fractions, so the sheet preview and
        // the exported PNG render them identically at any resolution. v3xx —
        // an imported PNG cutout renders as an aspect-preserving image
        // (width = sizeFrac × card width, transparency kept) instead of the
        // emoji glyph, and every sticker wears its [ShareSticker.rotation].
        if (stickers.isNotEmpty()) {
            BoxWithConstraints(Modifier.matchParentSize()) {
                val cw = maxWidth
                val ch = maxHeight
                stickers.forEach { st ->
                    val px = cw.value * st.sizeFrac.coerceIn(0.05f, 0.8f)
                    val rot = st.rotation
                    val img = st.imagePath?.let { p ->
                        // Decoded once per path (files under the app's own
                        // stickers dir can't change under us).
                        androidx.compose.runtime.remember(p) { decodeStickerBitmap(p) }
                    }
                    if (img != null) {
                        val ratio = img.height.toFloat() / img.width.toFloat()
                        androidx.compose.foundation.Image(
                            bitmap = img,
                            contentDescription = null,
                            modifier = Modifier
                                .offset(x = cw * st.x.coerceIn(0f, 1f), y = ch * st.y.coerceIn(0f, 1f))
                                .width(px.dp)
                                .height((px * ratio).dp)
                                .graphicsLayer { rotationZ = rot }
                        )
                    } else {
                        Text(
                            st.emoji,
                            fontSize = px.sp,
                            softWrap = false,
                            modifier = Modifier
                                .offset(x = cw * st.x.coerceIn(0f, 1f), y = ch * st.y.coerceIn(0f, 1f))
                                .graphicsLayer { rotationZ = rot }
                        )
                    }
                }
            }
        }
    }
}

/** v334 — book-jacket badge: the cover with a spine + sheen overlay so it
 *  reads as a real jacket sitting on the card. v370b — the size is a
 *  parameter (the side-layout cover renders beside the title); v373 — the
 *  sheet passes the cover's NATURAL shape: 44×66 for 2:3 book/series
 *  posters, 66×66 for square album art (never stretched). */
@Composable
private fun BookCoverBadge(
    cover: androidx.compose.ui.graphics.ImageBitmap,
    coverWidth: Dp = 44.dp,
    coverHeight: Dp = 66.dp,
    modifier: Modifier = Modifier,
    callbacks: EditBoundsCallbacks = EditBoundsCallbacks()
) {
    Box(
        modifier = modifier
            .width(coverWidth)
            .height(coverHeight)
            .shadow(3.dp, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White)
            // v335 — report the jacket's bounds so the editor can select and
            // drag it (the cover sits on top of every non-Collage style).
            .onGloballyPositioned { callbacks.onCover(it.boundsInWindow()) }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawImage(cover, dstOffset = androidx.compose.ui.unit.IntOffset(0, 0), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
        }
        // Spine shadow on the left + a vertical sheen so the photo reads as
        // a physical cover rather than a flat crop.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(Color.Black.copy(alpha = 0.14f), Offset.Zero, Size(w * 0.07f, h))
            drawRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Transparent)),
                Offset.Zero, Size(w, h * 0.45f)
            )
        }
    }
}

/** v376 — horizontal gap between the glued cover and the title text
 *  (snug, per the ask: title sits closer to the cover on Paper / Vinyl /
 *  Clean / Editorial / Minimal). */
private val CoverTitleGap = 12.dp

/**
 * v376 — the GLUED cover: rendered INSIDE a style's own layout as the
 * leading item of the title block, so it always sits exactly beside the
 * title wherever that design's flow puts it (Paper's centered middle,
 * Clean's centered column, Editorial/Minimal's top flow, …) — never in a
 * fixed corner that drifts from the title. The caller wraps its title +
 * meta in the SAME Row that carries [moveTitle] (see each style), so:
 *  - dragging the title moves cover + title together,
 *  - any collision push / auto lift of the title carries the cover too,
 *  - the cover keeps its OWN fine-position offset ([move.coverDx/coverDy])
 *    so selecting it and dragging it still nudges just the jacket.
 */
@Composable
private fun GluedCover(
    coverArt: androidx.compose.ui.graphics.ImageBitmap,
    coverW: Dp,
    coverH: Dp,
    move: ShareCardMove,
    callbacks: EditBoundsCallbacks,
    // vertical optical offset: lowers the jacket a touch inside its column
    // so its top reads level with the title's first glyph line (each title
    // font carries its own leading).
    topPad: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    Column(modifier.width(coverW)) {
        Spacer(Modifier.height(topPad))
        BookCoverBadge(
            cover = coverArt,
            coverWidth = coverW,
            coverHeight = coverH,
            // The cover's own stored drag offset applies INSIDE the glued
            // group (title drag offsets live on the wrapping Row).
            modifier = modifier.offset(x = move.coverDx.dp, y = move.coverDy.dp),
            callbacks = callbacks
        )
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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v369 — the Adjust tool's sat/contrast, applied to the BACKGROUND layer
    // only (never the text/polaroid/cover above it).
    bgFilter: ColorFilter? = null,
    // v375 — rich-text runs over this card's fact body (threaded from
    // TopicShareCard so FactBody can render the styled runs).
    factSpans: List<TextSpan> = emptyList(),
    // v376 — GLUED cover: artwork rendered inside the title block (see
    // [GluedCover]). Null = no cover → the layout is exactly as before.
    coverArt: androidx.compose.ui.graphics.ImageBitmap? = null,
    coverW: Dp = 44.dp,
    coverH: Dp = 66.dp
) {
    val qSize = quoteText?.let { quoteFontSize(it.length) } ?: 0.sp
    Box(
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
            .shadow(4.dp, RoundedCornerShape(6.dp))
    ) {
        // v369 — background layer: fill + paper texture + watermark wear the
        // sat/contrast filter; the content column below is NOT filtered.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { colorFilter = bgFilter }
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
        }

        // v3xx — the bottom padding deepens (28 → 46dp) so the footer clears
        // the torn bottom strip (drawn at ~12% up the card): the old 28dp
        // sat the credit text INSIDE the tear zone, so it read as hidden
        // behind the bottom decoration (and a grown fact box could shove it
        // past the card edge).
        Column(modifier = Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 46.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            HeaderRow(categoryName, categoryGlyph, palette, move, callbacks)
            MiddleContent(display, factText, aspect, palette, ratingStars, quoteText, qSize, quoteAuthor, byline, year, bodyScale, callbacks, move, chapterProgress, chapterFact, coverArt, coverW, coverH, factSpans = factSpans)
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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v341 — hide the typed favorite-song chip when heart-picked album
    // tracks exist: both would fight over the bottom-start corner (the
    // TopicShareCard-level FavoriteTracksBadge owns that spot then).
    hideTypedFavSong: Boolean = false,
    // v369 — the Adjust tool's sat/contrast, applied to the BACKGROUND layer
    // only (never the text/polaroid/cover above it).
    bgFilter: ColorFilter? = null,
    // v375 — rich-text runs over this card's fact body (threaded from
    // TopicShareCard so FactBody can render the styled runs).
    factSpans: List<TextSpan> = emptyList(),
    // v376 — GLUED cover: artwork rendered inside the title block (see
    // [GluedCover]). Null = no cover → the layout is exactly as before.
    coverArt: androidx.compose.ui.graphics.ImageBitmap? = null,
    coverW: Dp = 44.dp,
    coverH: Dp = 66.dp
) {
    val roseBg = Color(0xFFF5E6E0)
    val roseDusty = Color(0xFFD4A0A0)
    val roseLight = Color(0xFFF0D0C8)
    val inkDark = Color(0xFF3A2820)
    val roseFaint = Color(0xFFE8C8C0)
    // For quote cards, the title is the author/byline — display IS the quote.
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display
    val body = quoteText ?: factText

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
        // v369 — background layer (paper fill + textures + vinyl art + faint
        // watermark) wears the sat/contrast filter; the content column below
        // is NOT filtered, so text and photos never shift.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { colorFilter = bgFilter }
                .background(roseBg, RoundedCornerShape(6.dp))
        ) {
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
        }

        // ── Content layout ──
        Column(modifier = Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 14.dp)) {
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

            // v376 — title/cover sit a touch lower so neither crowds the
            // category pill row above.
            Spacer(Modifier.height(if (coverArt != null) 18.dp else 12.dp))

            // Title — strong serif. v376 — with a cover the title + artist
            // become a Row (cover | title+byline) that carries the title's
            // move offsets, so the jacket is glued beside the title and
            // rides every title drag / collision push / auto lift.
            val vinylMeta: @Composable () -> Unit = {
                if (quoteText == null && byline.isNotBlank()) {
                    Text(byline, style = metaStyle(TextStyle(
                        fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                        fontSize = 13.sp, color = roseDusty
                    ), move), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .then(if (coverArt == null) Modifier.titleShift(move) else Modifier)
                            .moveMeta(move)
                            .onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
                } else if (year != null) {
                    Text(year, style = metaStyle(TextStyle(
                        fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                        fontSize = 13.sp, color = roseDusty
                    ), move), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .then(if (coverArt == null) Modifier.titleShift(move) else Modifier)
                            .moveMeta(move)
                            .onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
                }
            }
            val vinylTitle: @Composable (Modifier) -> Unit = { titleMod ->
                Text(title, style = titleStyle(TextStyle(
                    fontFamily = ChangaOneFontFamily, fontSize = 28.sp,
                    lineHeight = 32.sp, fontWeight = FontWeight.Normal, color = inkDark
                ), move), maxLines = lines(2, move.titleHeightFrac), overflow = TextOverflow.Ellipsis,
                    modifier = titleMod.onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
            }
            if (coverArt != null) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.glueTitleMove(move)
                ) {
                    GluedCover(coverArt, coverW, coverH, move, callbacks, topPad = 6.dp)
                    Spacer(Modifier.width(CoverTitleGap))
                    Column(Modifier.weight(1f)) {
                        vinylTitle(Modifier.titleSize(move))
                        vinylMeta()
                    }
                }
            } else {
                vinylTitle(Modifier.moveTitle(move))
                vinylMeta()
            }

            // Accent underline ��� v316b: belongs to the quick-fact block below,
            // so it slides WITH the fact box when the F handle drags.
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.size(width = 32.dp, height = 2.dp).factShift(move)) {
                drawRoundRect(roseDusty, cornerRadius = CornerRadius(1f))
            }

            // v376 — a touch more air so the quick fact sits slightly lower.
            Spacer(Modifier.height(if (coverArt != null) 14.dp else 10.dp))

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
            // v3xx — re-report the fact style on EVERY composition: the smart
            // fit can shrink the text while the box is clipped at its maxLines
            // (the node's bounds don't move), and the old position-driven
            // report went stale — the inline field kept the old size and the
            // caret landed off the visible glyphs.
            androidx.compose.runtime.LaunchedEffect(factStyle) { callbacks.onFactStyle(factStyle) }
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
                        // v335 — a custom fact stacks UNDER the progress widget.
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.onGloballyPositioned {
                                callbacks.onFact(it.boundsInWindow())
                                callbacks.onFactStyle(factStyle)
                            }
                        ) {
                            ChapterProgressBlock(
                                progress = chapterProgress,
                                fill = roseDusty,
                                track = inkDark.copy(alpha = 0.14f),
                                ink = inkDark.copy(alpha = 0.88f)
                            )
                            if (chapterFact.isNotBlank()) {
                                FactBody(text = chapterFact, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(10, move.factHeightFrac, bodyScale))
                            }
                        }
                    } else {
                        // v335 — line cap tracks the font multiplier so the
                        // fact box keeps its footprint while text resizes.
                        FactBody(text = body, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(10, move.factHeightFrac, bodyScale), modifier = Modifier.onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                            callbacks.onFactStyle(factStyle)
                        })
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
                CurioIcon(name = CurioIcons.Lightbulb, tint = roseDusty.copy(alpha = 0.35f), size = 10.dp)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (sharerName.isNotBlank()) "$sharerName \u00b7 via Curio" else "via Curio",
                    style = TextStyle(fontFamily = GeomFontFamily, fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold, color = roseDusty.copy(alpha = 0.55f)),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Favorite song — small user-set corner chip (replaces the old info box) ──
        // v337 — the album's heart-picked tracks no longer render HERE: they
        // ride every card style as one shared corner strip drawn at the
        // TopicShareCard level (FavoriteTracksBadge below). This Vinyl chip
        // stays the plain typed favorite line.
        val is34v = aspect == ShareCardAspect.CLASSIC
        val favSong = AppPreferences.favoriteSongState.trim()
        // v341 — suppressed when the heart-picked album strip owns the corner.
        if (favSong.isNotEmpty() && !hideTypedFavSong) {
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

/** v337 — FAVORITE TRACKS strip: the album's heart-picked tracks render on
 *  EVERY card style as a sticker drawn at the TopicShareCard level, above
 *  whatever style is active. v340 — the sticker is no longer one shared
 *  cream/rose vinyl chip: each style gets its OWN visual language + corner
 *  (colors/typography/shape picked from that design), so the album favorites
 *  read as native to the card instead of a vinyl sticker pasted on top.
 *  Up to three songs, then "+N more".
 */
/** v353 — the leading glyph a favorite-tracks strip uses per card style:
 *  NOTE = filled eighth note, VINYL = tiny record disc, EQ = equalizer bars,
 *  STAR = masthead ornament, DOT = quiet full-stop dot. */
private enum class FavGlyph { NOTE, VINYL, EQ, STAR, DOT }

private data class FavStripTokens(
    val bg: Color,
    val border: Color?,
    val labelInk: Color,
    val bodyInk: Color,
    val heart: Color,
    val serifBody: Boolean,
    val capsSpacing: Boolean,
    val radius: Dp,
    val alpha: Float,
    val glyph: FavGlyph = FavGlyph.NOTE,
    // v384 — TRUE renders the strip as PLAIN TYPE (no surface, no border):
    // the favorites read as part of the card's own ink instead of a sticker.
    // Used by Collage and Signature/Custom.
    val noBox: Boolean = false
)

@Composable
private fun FavoriteTracksBadge(
    tracks: List<String>,
    style: ShareCardStyle,
    palette: ShareCardPalette,
    classic: Boolean,
    modifier: Modifier = Modifier,
    // v370 — the strip's own box-size fractions (the fav box width/height
    // sliders): width scales the strip's natural max width, height scales
    // how many track rows show (1f = the default 3).
    widthFrac: Float = 1f,
    heightFrac: Float = 1f,
    // v384 — explicit song count (0 = auto: the default 3 rows, or ALL
    // tracks when [noFact] expands the strip into the freed fact space),
    // and the ROWS (chip) layout mode (global preference).
    count: Int = 0,
    noFact: Boolean = false,
    rowsMode: Boolean = false,
    // v3xx — SIGNATURE ink override: the strip wears the signature design's
    // OWN body ink instead of the tone's palette.ink (which can clash with a
    // signature scene's background). Null = palette ink as before.
    inkOverride: Color? = null
) {
    // v353 — the strip is no longer the same sticker on every card: the
    // boxless designs (Editorial, Minimal) render the favorites as plain
    // type with no surface, everything else keeps its (now style-true)
    // badge. v384 — Collage/Signature/Custom join the plain-type family.
    val autoRows = if (noFact) tracks.size.coerceAtLeast(1)
    else kotlin.math.round(3f * heightFrac).toInt().coerceIn(1, tracks.size.coerceAtLeast(1))
    val rows = if (count > 0) count.coerceIn(1, tracks.size.coerceAtLeast(1)) else autoRows
    when (style) {
        ShareCardStyle.EDITORIAL -> EditorialFavStrip(tracks, palette, classic, modifier, widthFrac, rows, noFact, rowsMode)
        ShareCardStyle.MINIMAL -> MinimalFavStrip(tracks, palette, classic, modifier, widthFrac, rows, noFact, rowsMode)
        else -> BoxedFavStrip(tracks, style, palette, classic, modifier, widthFrac, rows, noFact, rowsMode, inkOverride)
    }
}

/** v353 — the boxed favorite-tracks badge (Paper, Vinyl, Collage,
 *  Neumorphic, Signature, Custom): per-style tokens + a per-style leading
 *  glyph instead of the same heart everywhere. */
@Composable
private fun BoxedFavStrip(
    tracks: List<String>,
    style: ShareCardStyle,
    palette: ShareCardPalette,
    classic: Boolean,
    modifier: Modifier = Modifier,
    // v370 — fav-box fractions (see FavoriteTracksBadge): widthFrac scales
    // the natural max width, rows caps how many tracks render.
    widthFrac: Float = 1f,
    rows: Int = 3,
    // v384 — no-fact expansion: when the fact is hidden the strip renders
    // LARGER type (it takes over the freed fact space).
    noFact: Boolean = false,
    rowsMode: Boolean = false,
    // v3xx — signature ink override (see FavoriteTracksBadge).
    inkOverride: Color? = null
) {
    val shownFavs = tracks.take(rows)
    val extra = tracks.size - shownFavs.size
    // v384 — larger type + room when the fact is hidden.
    val typeScale = if (noFact) 1.3f else 1f
    // v340 — per-style tokens: colors, borders, type and radius match the
    // design underneath so the strip belongs to the card it sits on.
    // v353 — each style's leading glyph: Paper a music note, Vinyl a tiny
    // record disc, Collage a note, Neumorphic equalizer bars, Signature /
    // Custom a note.
    val tok: FavStripTokens = when (style) {
        ShareCardStyle.PAPER -> FavStripTokens(
            bg = palette.bgBase, border = palette.ink.copy(alpha = 0.14f),
            labelInk = palette.ink.copy(alpha = 0.80f), bodyInk = palette.ink.copy(alpha = 0.84f),
            heart = palette.accent, serifBody = true, capsSpacing = true,
            radius = 7.dp, alpha = 0.94f, glyph = FavGlyph.NOTE
        )
        ShareCardStyle.VINYL -> FavStripTokens(
            bg = Color(0xFFFDF0EE), border = Color(0xFFD4A0A0).copy(alpha = 0.35f),
            labelInk = Color(0xFF3A2A20).copy(alpha = 0.78f), bodyInk = Color(0xFF3A2A20).copy(alpha = 0.82f),
            heart = Color(0xFFD4A0A0), serifBody = true, capsSpacing = true,
            radius = 7.dp, alpha = 0.92f, glyph = FavGlyph.VINYL
        )
        // v359 — the collage's bottom is a dark band under the torn seam, so
        // the strip is a translucent DARK slip with white serif type and the
        // polaroid's gold-tape notes — it reads on the band AND anywhere the
        // user drags it (a dark plate over any paper).
        // v384 — COLLAGE favorites are PLAIN TYPE now (no dark slip): the
        // strip wears the card's own ink so it reads as part of the paper
        // instead of a sticker. It also moved out of the dark bottom band
        // into the free middle (see the favSlot pass), above the category
        // pill + quick fact and below the title/info. v3xx — stronger alphas
        // now that it parks on the cream top paper, so the label + rows stay
        // legible next to the Bungee title.
        ShareCardStyle.COLLAGE -> FavStripTokens(
            bg = Color.Transparent, border = null,
            labelInk = palette.ink.copy(alpha = 0.66f), bodyInk = palette.ink.copy(alpha = 0.92f),
            heart = palette.accent.copy(alpha = 0.95f), serifBody = true, capsSpacing = false,
            radius = 3.dp, alpha = 1f, glyph = FavGlyph.NOTE, noBox = true
        )
        ShareCardStyle.NEUMORPHIC -> FavStripTokens(
            bg = Color.Black.copy(alpha = 0.62f), border = Color.White.copy(alpha = 0.16f),
            labelInk = Color.White.copy(alpha = 0.82f), bodyInk = Color.White.copy(alpha = 0.88f),
            heart = palette.accent.copy(alpha = 0.9f), serifBody = false, capsSpacing = true,
            radius = 50.dp, alpha = 1f, glyph = FavGlyph.EQ
        )
        // v384 — Signature/Custom favorites are PLAIN TYPE too: no stamp
        // pill, no border — the strip wears the card's own ink. v3xx — on
        // SIGNATURE the ink override is the DESIGN's body colour (tuned to
        // each scene); Custom keeps the tone's palette ink.
        else -> FavStripTokens(
            bg = Color.Transparent, border = null,
            labelInk = (inkOverride ?: palette.ink).copy(alpha = 0.62f),
            bodyInk = (inkOverride ?: palette.ink).copy(alpha = 0.90f),
            heart = if (inkOverride != null) inkOverride.copy(alpha = 0.85f) else palette.accent,
            serifBody = true, capsSpacing = false,
            radius = 8.dp, alpha = 1f, glyph = FavGlyph.NOTE, noBox = true
        )
    }
    val widthMod = modifier
        .widthIn(max = (if (classic) 168.dp else 210.dp) * widthFrac * if (noFact) 1.35f else 1f)
    val strip: @Composable (Modifier) -> Unit = { inner ->
        Column(
            verticalArrangement = Arrangement.spacedBy(if (tok.serifBody) 2.dp else 1.dp),
            modifier = inner.padding(if (tok.noBox) PaddingValues(0.dp) else PaddingValues(horizontal = 8.dp, vertical = 6.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShareMusicGlyph(
                    variant = tok.glyph,
                    color = tok.labelInk.copy(alpha = 0.85f),
                    iconSize = if (classic) 7.dp else 9.dp
                )
                Text(
                    "FAVORITE TRACKS",
                    style = TextStyle(
                        fontFamily = if (tok.capsSpacing) GeomFontFamily else LoraFontFamily,
                        fontSize = (if (classic) 5.sp else 6.sp) * typeScale,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = if (tok.capsSpacing) 1.1.sp else 0.6.sp,
                        color = tok.labelInk
                    )
                )
            }
            if (rowsMode) {
                FavTrackChips(
                    tracks = shownFavs,
                    classic = classic,
                    ink = tok.bodyInk,
                    accent = tok.heart,
                    serif = tok.serifBody,
                    noBox = tok.noBox,
                    typeScale = typeScale
                )
            } else {
                // v384 — the rows HUG the longest track title (no
                // fillMaxWidth/weight): short titles make a short strip.
                shownFavs.forEach { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ShareMusicGlyph(
                            variant = tok.glyph,
                            color = tok.heart.copy(alpha = 0.95f),
                            iconSize = if (classic) 5.5.dp else 7.dp
                        )
                        Text(
                            t,
                            style = if (tok.serifBody) TextStyle(
                                fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                                fontSize = (if (classic) 6.5.sp else 8.sp) * typeScale, lineHeight = (if (classic) 8.sp else 10.sp) * typeScale,
                                color = tok.bodyInk
                            ) else TextStyle(
                                fontFamily = GeomFontFamily,
                                fontSize = (if (classic) 6.sp else 7.5.sp) * typeScale, lineHeight = (if (classic) 7.5.sp else 9.sp) * typeScale,
                                fontWeight = FontWeight.SemiBold, color = tok.bodyInk
                            ),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (extra > 0) {
                Text(
                    "+$extra more",
                    style = if (tok.serifBody) TextStyle(
                        fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                        fontSize = (if (classic) 6.sp else 7.5.sp) * typeScale, color = tok.heart
                    ) else TextStyle(
                        fontFamily = GeomFontFamily, fontWeight = FontWeight.Bold,
                        fontSize = (if (classic) 6.sp else 7.5.sp) * typeScale, color = tok.heart
                    ),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    // v384 — plain-type styles (Collage / Signature / Custom) skip the
    // Surface entirely: the strip is just text in the card's own ink.
    if (tok.noBox) {
        strip(widthMod)
    } else {
        Surface(
            shape = RoundedCornerShape(tok.radius),
            color = tok.bg.copy(alpha = tok.alpha),
            border = tok.border?.let { BorderStroke(0.8.dp, it) },
            modifier = widthMod
        ) {
            strip(Modifier)
        }
    }
}

/** v384 — ROWS layout for the favorites strip: the tracks render as
 *  side-by-side chips that wrap, instead of one per line. [noBox] styles
 *  keep the chips borderless — plain text in the card's ink. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FavTrackChips(
    tracks: List<String>,
    classic: Boolean,
    ink: Color,
    accent: Color,
    serif: Boolean,
    noBox: Boolean,
    typeScale: Float = 1f
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.widthIn(max = (if (classic) 200.dp else 250.dp) * typeScale)
    ) {
        tracks.forEach { t ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = if (noBox) {
                    Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                } else {
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ink.copy(alpha = 0.10f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                }
            ) {
                ShareMusicGlyph(
                    variant = FavGlyph.NOTE,
                    color = accent.copy(alpha = 0.9f),
                    iconSize = if (classic) 5.dp else 6.dp
                )
                Text(
                    t,
                    style = TextStyle(
                        fontFamily = if (serif) LoraFontFamily else GeomFontFamily,
                        fontStyle = if (serif) FontStyle.Italic else FontStyle.Normal,
                        fontSize = (if (classic) 6.sp else 7.5.sp) * typeScale,
                        fontWeight = if (serif) FontWeight.Normal else FontWeight.SemiBold,
                        color = ink
                    ),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** v353 — EDITORIAL: no box. A masthead rule + caps label, then each track
 *  as a serif-italic line with a small note ornament — type only, like the
 *  rest of the masthead design. */
@Composable
private fun EditorialFavStrip(
    tracks: List<String>,
    palette: ShareCardPalette,
    classic: Boolean,
    modifier: Modifier = Modifier,
    widthFrac: Float = 1f,
    rows: Int = 3,
    noFact: Boolean = false,
    rowsMode: Boolean = false
) {
    val ink = Color(0xFF1C1814)
    val shownFavs = tracks.take(rows)
    val extra = tracks.size - shownFavs.size
    val typeScale = if (noFact) 1.3f else 1f
    Column(
        verticalArrangement = Arrangement.spacedBy(if (classic) 2.dp else 3.dp),
        modifier = modifier.widthIn(max = (if (classic) 230.dp else 290.dp) * widthFrac * if (noFact) 1.35f else 1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(if (classic) 12.dp else 16.dp)
                    .height(1.5.dp)
                    .background(ink.copy(alpha = 0.55f))
            )
            ShareMusicGlyph(
                variant = FavGlyph.STAR,
                color = palette.accent.copy(alpha = 0.9f),
                iconSize = if (classic) 6.dp else 7.5.dp
            )
            Text(
                "FAVORITE TRACKS",
                style = TextStyle(
                    fontFamily = LoraFontFamily,
                    fontSize = (if (classic) 5.5.sp else 6.5.sp) * typeScale,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = ink.copy(alpha = 0.85f)
                )
            )
        }
        if (rowsMode) {
            FavTrackChips(
                tracks = shownFavs,
                classic = classic,
                ink = ink.copy(alpha = 0.85f),
                accent = palette.accent.copy(alpha = 0.9f),
                serif = true,
                noBox = true,
                typeScale = typeScale
            )
        } else {
            // v384 — dynamic width: the rows hug the longest track title.
            shownFavs.forEach { t ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ShareMusicGlyph(
                        variant = FavGlyph.NOTE,
                        color = palette.accent.copy(alpha = 0.70f),
                        iconSize = if (classic) 5.dp else 6.5.dp
                    )
                    Text(
                        t,
                        style = TextStyle(
                            fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                            fontSize = (if (classic) 7.5.sp else 9.sp) * typeScale, lineHeight = (if (classic) 9.sp else 11.sp) * typeScale,
                            color = ink.copy(alpha = 0.82f)
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (extra > 0) {
            Text(
                "+$extra more",
                style = TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = (if (classic) 6.5.sp else 8.sp) * typeScale, color = palette.accent.copy(alpha = 0.95f)
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** v353 — MINIMAL: no box, no per-row icons. A quiet caps label with a dot,
 *  then the tracks joined by dots on ONE line — type only, as quiet as the
 *  design underneath. */
@Composable
private fun MinimalFavStrip(
    tracks: List<String>,
    palette: ShareCardPalette,
    classic: Boolean,
    modifier: Modifier = Modifier,
    widthFrac: Float = 1f,
    rows: Int = 3,
    noFact: Boolean = false,
    rowsMode: Boolean = false
) {
    val ink = Color(0xFF1A1A1A)
    val shownFavs = tracks.take(rows)
    val extra = tracks.size - shownFavs.size
    val typeScale = if (noFact) 1.3f else 1f
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.widthIn(max = (if (classic) 250.dp else 330.dp) * widthFrac * if (noFact) 1.35f else 1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ShareMusicGlyph(
                variant = FavGlyph.DOT,
                color = palette.accent.copy(alpha = 0.9f),
                iconSize = if (classic) 6.dp else 7.dp
            )
            Text(
                "FAVORITES",
                style = TextStyle(
                    fontFamily = GeomFontFamily,
                    fontSize = (if (classic) 5.5.sp else 6.5.sp) * typeScale,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                    color = ink.copy(alpha = 0.72f)
                )
            )
        }
        if (rowsMode) {
            FavTrackChips(
                tracks = shownFavs,
                classic = classic,
                ink = ink.copy(alpha = 0.80f),
                accent = palette.accent.copy(alpha = 0.9f),
                serif = false,
                noBox = true,
                typeScale = typeScale
            )
        } else {
            // v3xx — LIST look: one quiet track per line with a dot, so the
            // favorites read as a real list instead of one dot-joined line
            // that hid the track names. The design keeps its no-box, no-row-
            // icon language (caps label + type only).
            shownFavs.forEach { track ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShareMusicGlyph(
                        variant = FavGlyph.DOT,
                        color = palette.accent.copy(alpha = 0.85f),
                        iconSize = if (classic) 4.dp else 5.dp
                    )
                    Text(
                        track,
                        style = TextStyle(
                            fontFamily = GeomFontFamily,
                            fontSize = (if (classic) 6.5.sp else 8.sp) * typeScale,
                            lineHeight = (if (classic) 8.sp else 10.sp) * typeScale,
                            fontWeight = FontWeight.SemiBold, color = ink.copy(alpha = 0.72f)
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        if (extra > 0) {
            Text(
                "+$extra more",
                style = TextStyle(
                    fontFamily = GeomFontFamily, fontWeight = FontWeight.Bold,
                    fontSize = (if (classic) 5.5.sp else 7.sp) * typeScale, color = palette.accent.copy(alpha = 0.9f)
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** v336 — a tiny heart drawn directly for the share card (the bundled
 *  Material Symbols subset has no "favorite" ligature, and the software
 *  export pipeline rasterizes Canvas identically to the preview). */
@Composable
private fun ShareHeartGlyph(
    color: Color,
    iconSize: Dp,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        val heart = Path().apply {
            moveTo(w * 0.50f, h * 0.30f)
            cubicTo(w * 0.50f, h * 0.21f, w * 0.44f, h * 0.14f, w * 0.33f, h * 0.14f)
            cubicTo(w * 0.17f, h * 0.14f, w * 0.10f, h * 0.25f, w * 0.10f, h * 0.36f)
            cubicTo(w * 0.10f, h * 0.52f, w * 0.24f, h * 0.65f, w * 0.50f, h * 0.92f)
            cubicTo(w * 0.76f, h * 0.65f, w * 0.90f, h * 0.52f, w * 0.90f, h * 0.36f)
            cubicTo(w * 0.90f, h * 0.25f, w * 0.83f, h * 0.14f, w * 0.67f, h * 0.14f)
            cubicTo(w * 0.56f, h * 0.14f, w * 0.50f, h * 0.21f, w * 0.50f, h * 0.30f)
            close()
        }
        if (filled) {
            drawPath(heart, color)
        } else {
            drawPath(heart, color, style = Stroke(width = w * 0.10f))
        }
    }
}

/** v353 — per-style leading glyphs for the favorite-tracks strip, drawn
 *  directly (Canvas rasterizes identically in the export pipeline): a
 *  filled music note, a tiny vinyl disc, equalizer bars, a masthead star
 *  and a quiet dot. */
@Composable
private fun ShareMusicGlyph(
    variant: FavGlyph,
    color: Color,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        when (variant) {
            FavGlyph.NOTE -> {
                // Filled eighth note: oval head + stem + curved flag.
                drawOval(color, Offset(w * 0.28f, h * 0.62f), Size(w * 0.44f, h * 0.30f))
                drawRoundRect(color, Offset(w * 0.52f, h * 0.08f), Size(w * 0.12f, h * 0.72f), CornerRadius(w * 0.06f))
                val flag = Path().apply {
                    moveTo(w * 0.64f, h * 0.10f)
                    quadraticBezierTo(w * 0.92f, h * 0.30f, w * 0.66f, h * 0.54f)
                    quadraticBezierTo(w * 0.84f, h * 0.44f, w * 0.78f, h * 0.18f)
                    close()
                }
                drawPath(flag, color)
            }
            FavGlyph.VINYL -> {
                // Record disc: solid platter + lighter label + spindle hole.
                drawCircle(color, radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(Color.White.copy(alpha = 0.55f), radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.5f))
            }
            FavGlyph.EQ -> {
                // Three equalizer bars, staggered heights.
                val bw = w * 0.18f
                drawRoundRect(color, Offset(w * 0.06f, h * 0.45f), Size(bw, h * 0.45f), CornerRadius(bw * 0.4f))
                drawRoundRect(color, Offset(w * 0.41f, h * 0.12f), Size(bw, h * 0.78f), CornerRadius(bw * 0.4f))
                drawRoundRect(color, Offset(w * 0.76f, h * 0.30f), Size(bw, h * 0.60f), CornerRadius(bw * 0.4f))
            }
            FavGlyph.STAR -> {
                val cx = w * 0.5f; val cy = h * 0.5f
                val outer = w * 0.48f; val inner = outer * 0.42f
                val star = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outer else inner
                    val ang = Math.toRadians(-90.0 + i * 36.0)
                    val x = cx + (r * Math.cos(ang)).toFloat()
                    val y = cy + (r * Math.sin(ang)).toFloat()
                    if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
                }
                star.close()
                drawPath(star, color)
            }
            FavGlyph.DOT -> {
                drawCircle(color, radius = w * 0.30f, center = Offset(w * 0.5f, h * 0.5f))
            }
        }
    }
}

// STYLE 2 — COLLAGE (torn paper + polaroid + observatory)
// ═══════════════════════════════════════════════════════════════════════
/** v335 — desaturate a color to [keep]× its original saturation (keep < 1 =
 *  more muted) while preserving hue and lightness. Used by the Collage card
 *  so the picked tone colors its large fields as soft pastels instead of
 *  raw saturated fills. */
private fun collageMute(color: Color, keep: Float): Color {
    val hsl = toHsl(color)
    return fromHsl(hsl.h, (hsl.s * keep).coerceIn(0f, 1f), hsl.l)
}

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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v369 — the Adjust tool's sat/contrast, applied to the BACKGROUND layer
    // only (the polaroid + photo + text stay untouched).
    bgFilter: ColorFilter? = null,
    // v375 — rich-text runs over this card's fact body (threaded from
    // TopicShareCard so FactBody can render the styled runs).
    factSpans: List<TextSpan> = emptyList()
) {
    // v327 — the Collage card wears the picked TONE (the palette used to be
    // ignored here, so the Tone tool had no effect on this style): paper =
    // bgBase, lower field = accent, dark band = accentDark, ink/text =
    // palette ink, torn seam edge = bgMid. White polaroid + photo stay.
    // v335 — the tone accent filled the whole lower field RAW, so the
    // saturated level tones (Ember, Rose Gold, Ocean, Sunburst…) looked
    // garish on the collage; the field, band and pill are now desaturated
    // toward muted pastels (hue + lightness kept) so the collage keeps its
    // soft scrapbook feel while still wearing the picked tone.
    val topCream = palette.bgBase
    // v380 — DARK-tone aware field colours. On dark premium tones (Midnight /
    // Ember / …) the palette's ACCENT is a LIGHT contrast colour (silver-moon,
    // molten orange) and bgBase is the dark paper — using the accent raw for
    // the lower "sage" field put the LIGHTEST colour at the BOTTOM of the
    // card (inverted, garish) and left the white fact text unreadable on it.
    // The collage now CRUSHES the accent's lightness on dark tones so the
    // layers always darken top → bottom (paper → field → band → footer) and
    // the white body text keeps contrast. Light tones are untouched.
    val bgLuma = palette.bgBase.red * 0.299f + palette.bgBase.green * 0.587f + palette.bgBase.blue * 0.114f
    val darkTone = bgLuma < 0.55f
    val bottomSage = if (!darkTone) collageMute(palette.accent, 0.42f)
    else collageMute(androidx.compose.ui.graphics.lerp(palette.accent, Color.Black, 0.62f), 0.55f)
    val bottomDark = if (!darkTone) collageMute(palette.accentDark, 0.30f)
    else collageMute(androidx.compose.ui.graphics.lerp(palette.accentDark, Color.Black, 0.58f), 0.55f)
    val inkDark = palette.ink
    val tornEdge = if (!darkTone) palette.bgMid
    // The seam must READ against the dark paper (a near-black bgMid on a
    // near-black paper is invisible): pull it slightly toward the accent hue.
    else androidx.compose.ui.graphics.lerp(palette.bgMid, palette.accent, 0.22f)
    val sagePill = if (!darkTone) collageMute(palette.accentDark, 0.55f)
    else collageMute(androidx.compose.ui.graphics.lerp(palette.accentDark, Color.Black, 0.42f), 0.60f)
    // The polaroid is always WHITE paper, so its handwritten caption must be a
    // fixed warm dark ink — palette.ink on dark tones is near-WHITE (invisible
    // on the white polaroid). The NOIR print is the exception: its dark frame
    // needs the near-white ink instead.
    val captionInk = if (move.polaroidStyle == 6) Color(0xFFF0ECE4) else Color(0xFF40301F)

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
        // v369 — background layer (paper + botanical field + watermark) wears
        // the sat/contrast filter; the polaroid + photo + text stay untouched.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { colorFilter = bgFilter }
                .background(topCream, RoundedCornerShape(6.dp))
        ) {
        // ── Layered paper + botanical lower field with a natural torn seam ──
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            // v3xx — the torn seam rises a touch (0.42 → 0.40) so the dark
            // band owns a little more of the card and the favorites strip
            // (parked above the seam) reads cleanly on the cream paper.
            val tearY = h * 0.40f
            drawRect(bottomSage, Offset.Zero, Size(w, h))
            // Bottom zone — v380 BLENDED: the old two hard wavy paths (a sin-
            // edged dark band + a solid footer wedge with a cut-off top) met in
            // a busy, high-contrast seam that read as "weird". It is now ONE
            // soft zone: the field deepens into the band and footer through a
            // single vertical gradient whose top edge FADES IN (starts
            // transparent, so there is no line at all), and the footer wave
            // survives only as a subtle darker shade with a feathered top.
            val bandY = h * 0.72f
            val bandPath = Path().apply {
                moveTo(0f, bandY)
                var x = 0f
                while (x <= w) {
                    // Low-amplitude soft undulation (no hard cut).
                    val y = bandY + kotlin.math.sin(x * 0.025f + 1.5f) * 3.5f + kotlin.math.sin(x * 0.06f) * 2f
                    lineTo(x, y); x += w / 40f
                }
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(
                bandPath,
                Brush.verticalGradient(
                    listOf(bottomSage.copy(alpha = 0f), bottomSage.copy(alpha = 0.55f), bottomSage, bottomDark),
                    startY = bandY - 26f, endY = h
                )
            )
            drawNaturalTearPanel(tearY = tearY, top = topCream, edge = tornEdge, shadow = bottomDark.copy(alpha = 0.22f))
            // Footer wave — feathered top so it melts into the zone above.
            val footerY = h * 0.86f
            drawPath(
                Path().apply {
                    moveTo(0f, footerY + 4f)
                    cubicTo(w * 0.22f, footerY - 18f, w * 0.43f, footerY + 12f, w * 0.62f, footerY - 8f)
                    cubicTo(w * 0.78f, footerY - 24f, w * 0.90f, footerY + 2f, w, footerY - 14f)
                    lineTo(w, h); lineTo(0f, h); close()
                },
                Brush.verticalGradient(
                    listOf(bottomDark.copy(alpha = 0f), bottomDark.copy(alpha = 0.38f), bottomDark.copy(alpha = 0.9f)),
                    startY = footerY - 34f, endY = h
                )
            )
        }

        // Watermark glyphs on both paper and green field so the collage feels
        // intentional. v3xx — the CENTER glyph is dropped on the collage: the
        // 80dp category icon floating in the middle read as clutter over the
        // photo/field (the corner set stays).
        Watermark(family, categoryGlyph, inkDark.copy(alpha = 0.045f), display.hashCode(), center = false)
        Watermark(family, categoryGlyph, Color.White.copy(alpha = 0.045f), display.hashCode() + 17, center = false)
        }

        // ── TOP SECTION: title + metadata + quote (left) + polaroid (right) ──
        BoxWithConstraints(Modifier.fillMaxSize().zIndex(2f)) {
            val cw = maxWidth.value; val ch = maxHeight.value

            // v3xx — POLAROID rework: the instant-print is now a movable /
            // scalable element (grip drag + size slider in the editor), wears
            // a pickable STYLE (frame + tape + tilt: Classic · Retro ·
            // Sunglow · Vintage · Dashed) and its photo takes a FILTER
            // (None · Noise · Nostalgia · B&W · Warm). The tape PEERS out
            // past the white frame (drawn LAST, so it sits ON the photo) and
            // the frame ADAPTS to the photo's aspect — a landscape print is
            // wide and short, a portrait one tall, like a real instant-print.
            val pStyle = move.polaroidStyle.coerceIn(0, polaroidLooks.lastIndex)
            val pFilter = move.polaroidFilter.coerceIn(0, 4)
            val look = polaroidLooks[pStyle]
            // v3xx — the upper clamp widened (0.36 → 0.44 of card width) so the
            // print-size slider has real travel: it used to stop growing at
            // scale ≈ 1.06, leaving most of the slider dead.
            val basePW = (cw * 0.34f * move.polaroidScale).coerceIn(cw * 0.20f, cw * 0.44f)
            val capH = basePW * 0.20f
            // Frame adapts to the photo's aspect (landscape → wide/short,
            // portrait → tall); no photo → the classic 1.18 print.
            var pW = basePW
            var pH = basePW * 1.18f
            if (userPhoto != null && userPhoto.width > 0 && userPhoto.height > 0) {
                val ar = userPhoto.width.toFloat() / userPhoto.height.toFloat()
                pH = pW / ar + capH
                val maxH = ch * 0.55f
                if (pH > maxH) {
                    pW = ((maxH - capH) * ar).coerceAtMost(basePW)
                    pH = pW / ar + capH
                }
                pW = pW.coerceAtMost(cw * 0.44f)
            }
            val photoH = if (userPhoto != null) (pH - capH).coerceAtLeast(pW * 0.4f) else pH * 0.68f
            val pX = (cw * 0.62f + move.polaroidDx).coerceIn(0f, (cw - pW - 6f).coerceAtLeast(0f))
            val pY = (ch * 0.035f + move.polaroidDy).coerceIn(0f, (ch - pH - 6f).coerceAtLeast(0f))
            val polaroidLabel = polaroidCaption.ifBlank {
                if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio"
            }

            PolaroidPrint(
                pW = pW, pH = pH, pX = pX, pY = pY, photoH = photoH,
                look = look, pStyle = pStyle, pFilter = pFilter,
                userPhoto = userPhoto, caption = polaroidLabel, captionInk = captionInk,
                onPhotoTap = onPhotoTap, callbacks = callbacks,
                // v3xx — no dark shadow behind the tilted cream print on the
                // collage (see [PolaroidPrint.shadow]).
                shadow = false
            )

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
                    ), move), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis, modifier = Modifier.moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
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

            // Body text — serif, generous line height. v370 — slightly
            // LARGER floors so long facts read clearly while the auto-fit
            // grows the box into the middle of the card (the old 10sp floor
            // made long text stay tiny instead of expanding).
            val body = quoteText ?: factText
            val bodySize = when {
                body.length > 340 -> 10.5.sp; body.length > 260 -> 11.sp
                body.length > 180 -> 11.5.sp; else -> 12.5.sp
            }
            val factStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize.value * bodyScale).sp,
                lineHeight = (bodySize.value * 1.55f * bodyScale).sp, color = Color.White.copy(alpha = 0.92f),
                fontStyle = if (quoteText != null) FontStyle.Italic else FontStyle.Normal
            ), move)
            // v3xx — re-report on every composition (see Vinyl's comment): a
            // clipped box keeps its bounds while the fit shrinks the text, so
            // the position-driven report would go stale and the inline field's
            // caret would drift off the visible glyphs.
            androidx.compose.runtime.LaunchedEffect(factStyle) { callbacks.onFactStyle(factStyle) }
            // v329 — Reading-progress content draws the chapter widget (white
            // on the sage/dark field) instead of the prose.
            // v335 — a custom fact stacks UNDER the progress widget.
            if (chapterProgress != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    }
                ) {
                    ChapterProgressBlock(
                        progress = chapterProgress,
                        fill = Color.White,
                        track = Color.White.copy(alpha = 0.22f),
                        ink = Color.White.copy(alpha = 0.92f)
                    )
                    if (chapterFact.isNotBlank()) {
                        FactBody(text = chapterFact, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(18, move.factHeightFrac, bodyScale))
                    }
                }
            } else {
                FactBody(text = body, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(18, move.factHeightFrac, bodyScale), modifier = Modifier.moveFact(move).onGloballyPositioned {
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
                style = TextStyle(fontFamily = GeomFontFamily, fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = Color.White.copy(alpha = 0.55f)),
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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v369 — the Adjust tool's sat/contrast, applied to the BACKGROUND layer
    // only (never the text above it).
    bgFilter: ColorFilter? = null,
    // v375 — rich-text runs over this card's fact body (threaded from
    // TopicShareCard so FactBody can render the styled runs).
    factSpans: List<TextSpan> = emptyList(),
    // v376 — GLUED cover: artwork rendered inside the title block (see
    // [GluedCover]). Null = no cover → the layout is exactly as before.
    coverArt: androidx.compose.ui.graphics.ImageBitmap? = null,
    coverW: Dp = 44.dp,
    coverH: Dp = 66.dp
) {
    val ink = Color(0xFF101010)
    val paper = Color(0xFFF8F6EF)
    val categoryGlow = palette.accent
    val categoryDeep = palette.accentDark
    val body = quoteText ?: factText
    // For quote cards, the title is the author/byline — display IS the quote, so showing both duplicates it.
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
        // v369 — background layer (plate fill + gradient + giant glyph) wears
        // the sat/contrast filter; the text content below is NOT filtered.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { colorFilter = bgFilter }
                .background(categoryDeep, RoundedCornerShape(6.dp))
        ) {
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
        }

        Box(Modifier.fillMaxSize().padding(22.dp)) {
            Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.72f), modifier = Modifier.align(Alignment.TopStart).moveBadge(move).onGloballyPositioned { callbacks.onBadge(it.boundsInWindow()) }) {
                Text(categoryName.uppercase(), style = badgeStyle(TextStyle(fontFamily = GeomFontFamily, fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp), move), color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }

            // v376 — GLUED cover: the title block becomes a Row (cover |
            // title+author) carrying [moveTitle], so the jacket sits exactly
            // beside the title and rides every title move (drag / collision
            // / auto lift) automatically. Clean centres its title block on
            // the card, so the Row is center-aligned exactly like the old
            // cover slot.
            val ncTitle: @Composable (Modifier) -> Unit = { titleMod ->
                Text(title, style = titleStyle(TextStyle(
                    fontFamily = ChangaOneFontFamily, fontSize = 31.sp, lineHeight = 33.sp,
                    fontWeight = FontWeight.Normal, color = Color.White
                ), move), maxLines = lines(5, move.titleHeightFrac), overflow = TextOverflow.Ellipsis,
                    modifier = titleMod.onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
            }
            val ncMeta: @Composable () -> Unit = {
                val metaParts = mutableListOf<String>()
                if (byline.isNotBlank()) metaParts.add(byline.uppercase())
                if (year != null) metaParts.add(year)
                if (metaParts.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(metaParts.joinToString("  /  "), style = metaStyle(TextStyle(
                        fontFamily = GeomFontFamily, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.3.sp, color = Color.White.copy(alpha = 0.56f)
                    ), move), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .then(if (coverArt == null) Modifier.titleShift(move) else Modifier)
                            .moveMeta(move)
                            .onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
                }
            }
            if (coverArt != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp, end = 4.dp)
                        .glueTitleMove(move)
                ) {
                    GluedCover(coverArt, coverW, coverH, move, callbacks, topPad = 0.dp)
                    Spacer(Modifier.width(CoverTitleGap))
                    Column(Modifier.weight(1f)) {
                        ncTitle(Modifier.titleSize(move))
                        ncMeta()
                    }
                }
            } else {
                Column(Modifier.align(Alignment.CenterStart).padding(start = 4.dp, end = 4.dp, top = 0.dp), horizontalAlignment = Alignment.Start) {
                    ncTitle(Modifier.fillMaxWidth(0.90f).moveTitle(move))
                    ncMeta()
                }
            }

            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 16.dp)) {
                // v370 — slightly LARGER floors: the bottom-anchored fact
                // grows up naturally and the title lifts to make room, so
                // long text stays readable instead of pinching to 8sp.
                val bodySize = when { body.length > 350 -> 8.5.sp; body.length > 260 -> 9.5.sp; body.length > 180 -> 10.sp; else -> 11.sp }
                val factStyle = factBodyStyle(TextStyle(
                    fontFamily = LoraFontFamily,
                    fontStyle = if (quoteText != null) FontStyle.Italic else FontStyle.Normal,
                    fontSize = (bodySize.value * bodyScale).sp, lineHeight = (bodySize.value * 1.40f * bodyScale).sp,
                    color = Color.White.copy(alpha = 0.88f),
                    shadow = Shadow(Color.Black.copy(alpha = 0.62f), Offset(0f, 2f), 5f)
                ), move)
                // v3xx — re-report on every composition (see Vinyl's comment):
                // the fit can shrink the text inside a bounds-fixed clipped
                // box, leaving the position-driven report stale and the
                // inline field's caret off the glyphs.
                androidx.compose.runtime.LaunchedEffect(factStyle) { callbacks.onFactStyle(factStyle) }
                // v329 — Reading-progress content draws the chapter widget
                // (white on the dark plate) instead of the prose.
                // v335 — a custom fact stacks UNDER the progress widget.
                if (chapterProgress != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.moveFact(move).onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                            callbacks.onFactStyle(factStyle)
                        }
                    ) {
                        ChapterProgressBlock(
                            progress = chapterProgress,
                            fill = Color.White,
                            track = Color.White.copy(alpha = 0.22f),
                            ink = Color.White.copy(alpha = 0.88f)
                        )
                        if (chapterFact.isNotBlank()) {
                            FactBody(text = chapterFact, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 8 else 6, move.factHeightFrac, bodyScale))
                        }
                    }
                } else {
                    FactBody(text = body, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 8 else 6, move.factHeightFrac, bodyScale), modifier = Modifier.moveFact(move).onGloballyPositioned {
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
                    if (quoteText != null && !quoteAuthor.isNullOrBlank()) "$quoteAuthor" else if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio",
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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v369 — the Adjust tool's sat/contrast, applied to the BACKGROUND layer
    // only (never the text above it).
    bgFilter: ColorFilter? = null,
    // v375 — rich-text runs over this card's fact body (threaded from
    // TopicShareCard so FactBody can render the styled runs).
    factSpans: List<TextSpan> = emptyList(),
    // v376 — GLUED cover: artwork rendered inside the title block (see
    // [GluedCover]). Null = no cover → the layout is exactly as before.
    coverArt: androidx.compose.ui.graphics.ImageBitmap? = null,
    coverW: Dp = 44.dp,
    coverH: Dp = 66.dp
) {
    val cream = Color(0xFFFAF7F0)
    val inkDark = Color(0xFF1C1814)
    val accentRule = palette.accent
    val body = quoteText ?: factText
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
        // v369 — background layer (paper fill + texture speckle) wears the
        // sat/contrast filter; the masthead + text below are NOT filtered.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { colorFilter = bgFilter }
                .background(cream, RoundedCornerShape(6.dp))
        ) {
        // Subtle texture
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val s = (w * 1000 + h).toInt()
            for (i in 0 until 60) {
                val x = ((s * (i + 1) * 7919) % 10000) / 10000f * w
                val y = ((s * (i + 1) * 6271) % 10000) / 10000f * h
                drawCircle(Color(0xFFD0C8B8).copy(alpha = 0.04f), 1.5f, Offset(x, y))
            }
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

            // Byline — year deck (info row — movable via the M handle, not editable)
            val metaParts = mutableListOf<String>()
            if (quoteText == null && byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            val edMetaLine: @Composable () -> Unit = {
                if (metaParts.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text(metaParts.joinToString(" · "), style = metaStyle(TextStyle(
                        fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                        fontSize = 12.sp, color = inkDark.copy(alpha = 0.55f)
                    ), move), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .then(if (coverArt == null) Modifier.titleShift(move) else Modifier)
                            .moveMeta(move)
                            .onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
                }
            }
            val edTitleLine: @Composable (Modifier) -> Unit = { titleMod ->
                // Headline — retro Bungee, big
                Text(title, style = titleStyle(TextStyle(
                    fontFamily = BungeeFontFamily, fontSize = 30.sp,
                    lineHeight = 34.sp, color = inkDark
                ), move), maxLines = lines(4, move.titleHeightFrac), overflow = TextOverflow.Ellipsis,
                    modifier = titleMod.onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
            }
            // v376 — GLUED cover: the jacket is the leading item of the
            // masthead's title block, so it always sits exactly beside the
            // headline at its natural flow position and rides every title
            // move (drag / collision / auto lift) via [glueTitleMove] on the
            // Row — only the title's own font scale/width crop stays on the
            // text ([titleSize]), so a size tweak never stretches the jacket.
            if (coverArt != null) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.glueTitleMove(move)
                ) {
                    GluedCover(coverArt, coverW, coverH, move, callbacks, topPad = 5.dp)
                    Spacer(Modifier.width(CoverTitleGap))
                    Column(Modifier.weight(1f)) {
                        edTitleLine(Modifier.titleSize(move))
                        edMetaLine()
                    }
                }
            } else {
                edTitleLine(Modifier.moveTitle(move))
                edMetaLine()
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
            // v370 — slightly LARGER floors: the fact grows down to the
            // colophon and the title keeps its spot, so long text reads at a
            // comfortable size instead of pinching to 8.5sp.
            val bodySize = when {
                body.length > 350 -> 9.sp; body.length > 260 -> 9.5.sp
                body.length > 180 -> 10.5.sp; else -> 11.5.sp
            }
            val bodyStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize.value * bodyScale).sp,
                lineHeight = (bodySize.value * 1.45f * bodyScale).sp, color = inkDark.copy(alpha = 0.82f),
                fontWeight = FontWeight.Medium
            ), move)
            // v3xx — re-report on every composition (see Vinyl's comment): the
            // drop-cap flow's box can stay put while the fit shrinks the type.
            androidx.compose.runtime.LaunchedEffect(bodyStyle) { callbacks.onFactStyle(bodyStyle) }
            val bodyRest = if (body.length > 1) body.drop(1) else ""
            // v329 — Reading-progress content draws the chapter widget
            // (accent bar on the cream page) instead of the drop-cap prose.
            // v335 — a custom fact stacks UNDER the progress widget.
            if (chapterProgress != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(bodyStyle)
                    }
                ) {
                    ChapterProgressBlock(
                        progress = chapterProgress,
                        fill = accentRule,
                        track = inkDark.copy(alpha = 0.13f),
                        ink = inkDark.copy(alpha = 0.82f)
                    )
                    if (chapterFact.isNotBlank()) {
                        FactBody(text = chapterFact, style = bodyStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, move.factHeightFrac, bodyScale))
                    }
                }
            } else if (move.factFormat == ShareCardFactFormat.BOOK ||
                move.factFormat == ShareCardFactFormat.CONDENSED ||
                (move.factFormat == ShareCardFactFormat.EDITORIAL && move.factDropCap == ShareCardFactDropCap.NONE)
            ) {
                // v370b — BOOK / CONDENSED / no-cap render as the shared plain
                // (or two-column) paragraph; the drop-cap block only shows for
                // the classic Editorial look (STANDARD = big first letter) or
                // the EDITORIAL format with a cap picked.
                FactBody(
                    text = body, style = bodyStyle,
                    format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter,
                    spans = factSpans, aspect = aspect,
                    maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, move.factHeightFrac, bodyScale),
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(bodyStyle)
                    }
                )
            } else {
                // v370b — the classic Editorial drop cap, hoisted into the
                // shared block so the cap variant pick applies here too.
                EditorialDropCapBlock(
                    text = body, spans = factSpans, marker = ShareFactMarker,
                    bodyStyle = bodyStyle,
                    dropCap = if (move.factFormat == ShareCardFactFormat.EDITORIAL) move.factDropCap else ShareCardFactDropCap.LETTER,
                    capColor = accentRule,
                    maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, move.factHeightFrac, bodyScale),
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(bodyStyle)
                    }
                )
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
                    if (sharerName.isNotBlank()) "$sharerName · Curio" else "Curio",
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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v369 — the Adjust tool's sat/contrast, applied to the BACKGROUND layer
    // only (never the text above it).
    bgFilter: ColorFilter? = null,
    // v375 — rich-text runs over this card's fact body (threaded from
    // TopicShareCard so FactBody can render the styled runs).
    factSpans: List<TextSpan> = emptyList(),
    // v376 — GLUED cover: artwork rendered inside the title block (see
    // [GluedCover]). Null = no cover → the layout is exactly as before.
    coverArt: androidx.compose.ui.graphics.ImageBitmap? = null,
    coverW: Dp = 44.dp,
    coverH: Dp = 66.dp,
    // v3xx — FAVORITES ROOM: when the album favorites strip is on this
    // card (its bottom-left slot), the fact box's bottom edge reserves this
    // much extra space so the strip — a real LIST now, taller than the old
    // one-line strip — never sits over the fact's lower lines. Structural:
    // the default smart fit AND the sparkle both render clear of the strip
    // with no extra collision math. 0.dp = favorites off → layout unchanged.
    favReserveDp: Dp = 0.dp
) {
    val bg = Color(0xFFFFFDF9)
    val inkDark = Color(0xFF1A1A1A)
    val accent = palette.accent
    val body = quoteText ?: factText
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
        // v369 — background layer (fill + hairline frame + faint initial)
        // wears the sat/contrast filter; the text content below is NOT.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { colorFilter = bgFilter }
                .background(bg, RoundedCornerShape(6.dp))
        ) {
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

            // Byline / year — info row: movable via M handle, not editable
            val mnMetaLine: @Composable () -> Unit = {
                if (byline.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(byline, style = metaStyle(TextStyle(
                        fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                        fontSize = 12.sp, color = inkDark.copy(alpha = 0.50f)
                    ), move), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .then(if (coverArt == null) Modifier.titleShift(move) else Modifier)
                            .moveMeta(move)
                            .onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
                } else if (year != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(year, style = metaStyle(TextStyle(fontFamily = LoraFontFamily, fontSize = 12.sp, color = inkDark.copy(alpha = 0.40f)), move), maxLines = lines(2, move.metaHeightFrac, max = 2),
                        modifier = Modifier
                            .then(if (coverArt == null) Modifier.titleShift(move) else Modifier)
                            .moveMeta(move)
                            .onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
                }
            }
            val mnTitleLine: @Composable (Modifier) -> Unit = { titleMod ->
                // Title — big retro Bungee
                Text(title, style = titleStyle(TextStyle(
                    fontFamily = BungeeFontFamily, fontSize = 32.sp, lineHeight = 35.sp, color = inkDark
                ), move), maxLines = lines(5, move.titleHeightFrac), overflow = TextOverflow.Ellipsis,
                    modifier = titleMod.onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
            }
            // v376 — GLUED cover: the jacket is the leading item of the title
            // block so it always sits exactly beside the title and rides every
            // title move (drag / collision / auto lift) via [glueTitleMove] on
            // the Row — the author/year column stays next to it.
            if (coverArt != null) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.glueTitleMove(move)
                ) {
                    GluedCover(coverArt, coverW, coverH, move, callbacks, topPad = 4.dp)
                    Spacer(Modifier.width(CoverTitleGap))
                    Column(Modifier.weight(1f)) {
                        mnTitleLine(Modifier.titleSize(move))
                        mnMetaLine()
                    }
                }
            } else {
                mnTitleLine(Modifier.moveTitle(move))
                mnMetaLine()
            }

            Spacer(Modifier.weight(1f))

            // Body — serif, bottom-anchored
            // v370 — slightly LARGER floors: the bottom-anchored fact grows
            // up and the title lifts to clear it, so long text stays readable.
            val bodySize = when { body.length > 350 -> 9.sp; body.length > 260 -> 9.5.sp; body.length > 180 -> 11.sp; else -> 12.sp }
            val factStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize.value * bodyScale).sp,
                lineHeight = (bodySize.value * 1.50f * bodyScale).sp, color = inkDark.copy(alpha = 0.78f)
            ), move)
            // v3xx — re-report on every composition (see Vinyl's comment): the
            // bottom-anchored box can be clipped at its cap while the fit
            // shrinks the text further — the old position-driven report went
            // stale and the inline field's caret drifted off the glyphs.
            androidx.compose.runtime.LaunchedEffect(factStyle) { callbacks.onFactStyle(factStyle) }
            // v359 — the accent rule, the gap and the fact text are ONE move
            // group: the rule above the quick/custom fact travels with the
            // box on drag (and stays glued while the box resizes) instead of
            // floating where the fact used to be.
            Column(modifier = Modifier.moveFact(move)) {
                Box(Modifier.width(56.dp).height(4.dp).background(accent))
                Spacer(Modifier.height(16.dp))
                // v329 — Reading-progress content draws the chapter widget
                // (accent bar on the white page) instead of the prose.
                // v335 — a custom fact stacks UNDER the progress widget.
                if (chapterProgress != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                            callbacks.onFactStyle(factStyle)
                        }
                    ) {
                        ChapterProgressBlock(
                            progress = chapterProgress,
                            fill = accent,
                            track = inkDark.copy(alpha = 0.12f),
                            ink = inkDark.copy(alpha = 0.78f)
                        )
                        if (chapterFact.isNotBlank()) {
                            FactBody(text = chapterFact, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, move.factHeightFrac, bodyScale))
                        }
                    }
                } else {
                    FactBody(text = body, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 9, move.factHeightFrac, bodyScale), modifier = Modifier.onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    })
                }
            }

            // v3xx — FAVORITES ROOM: reserve the strip's slot below the
            // fact (see [favReserveDp]) so the bottom-anchored fact can
            // never grow over the list-strip in the bottom-left corner.
            if (favReserveDp > 0.dp) Spacer(Modifier.height(favReserveDp))

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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v369 — the Adjust tool's sat/contrast, applied to the BACKGROUND layer
    // only (never the text/crest above it).
    bgFilter: ColorFilter? = null,
    // v375 — rich-text runs over this card's fact body (threaded from
    // TopicShareCard so FactBody can render the styled runs).
    factSpans: List<TextSpan> = emptyList(),
    // v381 — GLUED cover: the jacket is rendered INSIDE the title block so
    // it sits exactly beside the title wherever each signature layout flows
    // (never the floating top-left badge that overlapped the badge/title).
    // It rides every title move (drag / collision / auto lift) via the same
    // [glueTitleMove] row the other glued styles use; null = no cover → the
    // layouts render exactly as before. See [GluedCover].
    coverArt: androidx.compose.ui.graphics.ImageBitmap? = null,
    coverW: Dp = 44.dp,
    coverH: Dp = 66.dp
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

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(sig.cornerRadius.dp))) {
        // v369 — background layer (paper fill + drawn scene + faint watermark
        // + language words) wears the sat/contrast filter; text is NOT.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { colorFilter = bgFilter }
                .background(sig.bg, RoundedCornerShape(sig.cornerRadius.dp))
        ) {
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
        // v381 — `glued` = inside the cover Row: the title keeps only its
        // scale/width crop ([titleSize]); the Row owns the title drag offset
        // and auto lift ([glueTitleMove]), so the jacket never stretches and
        // the pair moves together.
        @Composable
        fun TitleText(centered: Boolean = false, glued: Boolean = false) {
            val mod = (if (centered) Modifier.fillMaxWidth() else Modifier)
                .then(if (glued) Modifier.titleSize(move) else Modifier.moveTitle(move))
            Text(title, style = titleStyle(TextStyle(
                fontFamily = sig.titleFont, fontSize = sig.titleSize,
                lineHeight = sig.titleLineHeight, color = sig.titleColor
            ), move), maxLines = lines(4, move.titleHeightFrac), overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = mod.onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) })
        }

        // ── Meta (shared) — info row: movable via M handle, not editable ──
        @Composable
        fun MetaText(centered: Boolean = false, glued: Boolean = false) {
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(sig.metaSpacer))
                Text(metaParts.joinToString(sig.metaSeparator), style = metaStyle(TextStyle(
                    fontFamily = LoraFontFamily, fontStyle = FontStyle.Italic,
                    fontSize = sig.metaSize, color = sig.metaColor
                ), move), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    // Glued: the title Row carries the title drag, so the meta
                    // must not double-shift with [titleShift] (the M handle's
                    // own fine offset still applies via [moveMeta]).
                    modifier = (if (centered) Modifier.fillMaxWidth() else Modifier)
                        .then(if (glued) Modifier else Modifier.titleShift(move))
                        .moveMeta(move)
                        .onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
            }
        }

        // ── Title + meta, GLUED to the cover when one is on the card ──
        // v381 — the jacket is the leading item of the title block, so a
        // book/album cover always sits EXACTLY beside the title wherever the
        // design's flow puts it (badge below it on STANDARD / centred with the
        // title on CENTERED / OVERLAY / POSTER) — never floating over the
        // badge/crest. No cover → identical to the old TitleText()+MetaText()
        // calls each layout made.
        @Composable
        fun TitleAndMeta(centered: Boolean = false) {
            if (coverArt == null) {
                TitleText(centered)
                MetaText(centered)
            } else {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth().glueTitleMove(move)
                ) {
                    GluedCover(coverArt, coverW, coverH, move, callbacks, topPad = 2.dp)
                    Spacer(Modifier.width(CoverTitleGap))
                    Column(Modifier.weight(1f)) {
                        TitleText(centered, glued = true)
                        MetaText(centered, glued = true)
                    }
                }
            }
        }

        // ── Body (shared) ─────────────────────────────────────
        @Composable
        fun BodyText(centered: Boolean = false) {
            // User's format alignment wins; otherwise honor the layout's own
            // alignment (Signature has centered variants). Hoisted above the
            // progress branch so a stacked custom fact (v335) wears the same
            // style the prose would.
            val align = move.factAlign ?: if (centered) TextAlign.Center else TextAlign.Start
            val factStyle = factBodyStyle(TextStyle(
                fontFamily = LoraFontFamily, fontSize = (bodySize * bodyScale).sp,
                lineHeight = (bodySize * sig.bodyLineHeight * bodyScale).sp,
                color = sig.bodyColor
            ), move).copy(textAlign = align)
            // v3xx — re-report on every composition (see Vinyl's comment): a
            // clipped box keeps its bounds while the fit shrinks the type, so
            // the position-driven report would go stale and the caret drift.
            androidx.compose.runtime.LaunchedEffect(factStyle) { callbacks.onFactStyle(factStyle) }
            // v329 — Reading-progress content draws the chapter widget in the
            // signature surface's own tones (no ruled lines; the bar wears
            // the rule/badge tone) instead of the prose.
            // v335 — a custom fact stacks UNDER the progress widget.
            if (chapterProgress != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = (if (centered) Modifier.fillMaxWidth() else Modifier)
                        .moveFact(move)
                        .onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                        }
                ) {
                    ChapterProgressBlock(
                        progress = chapterProgress,
                        fill = sig.bodyRuleColor ?: sig.badgeColor,
                        track = sig.bodyColor.copy(alpha = 0.16f),
                        ink = sig.bodyColor
                    )
                    if (chapterFact.isNotBlank()) {
                        FactBody(text = chapterFact, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(bodyMaxLines, move.factHeightFrac, bodyScale))
                    }
                }
                return
            }
            // Book-cover ruled lines BEHIND the text, spaced at the body's own
            // line height so the facts sit exactly on the lines (drawn in the
            // same local space as the text, so they move with the box).
            val ruleColor = sig.bodyRuleColor
            FactBody(text = body, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(bodyMaxLines, move.factHeightFrac, bodyScale),
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
            // ═══ STANDARD — badge top, title, spacer, body bottom, footer ���══
            SignatureLayout.STANDARD -> {
                Column(modifier = Modifier.fillMaxSize().padding(sig.padding)) {
                    CategoryBadge()
                    Spacer(Modifier.height(sig.titleTopSpacer))
                    TitleAndMeta()
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
                    TitleAndMeta(centered = true)
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
                    TitleAndMeta()
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
                    // Left panel — badge, title, meta, footer. The panel is
                    // too narrow for a side-by-side jacket, so the cover sits
                    // CENTERED above the title when one is on the card.
                    Column(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                        CategoryBadge()
                        Spacer(Modifier.height(sig.titleTopSpacer))
                        if (coverArt != null) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                GluedCover(coverArt, coverW, coverH, move, callbacks)
                            }
                        }
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
                    TitleAndMeta(centered = true)
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
                    TitleAndMeta(centered = true)
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF7AA060),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF4FC3F7),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF1565C0),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF9A7AD0),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF8A7A40),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFFF6D00),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF8A8AA0),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFFF8F00),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF00BCD4),
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
        if (t.contains("BEATLES")) return SignatureDesign(bg = Color(0xFF2A0A3E), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 6) { drawCircle(Color(0xFFFF6D00).copy(alpha = 0.21f), w * 0.05f + i * w * 0.015f, Offset(w * 0.20f + i * w * 0.09f, h * 0.15f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF6D00), badgeInk = Color(0xFF2A0A3E), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFB080), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFFF6D00), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0C0E0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF6D00).copy(alpha = 0.65f))
        if (t.contains("TAYLOR SWIFT")) return SignatureDesign(bg = Color(0xFFFFF0F5), cornerRadius = 8f, drawBackground = { w, h -> for (i in 0 until 12) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h; drawStar(x, y, 3f, 1.5f, Color(0xFFE91E63).copy(alpha = 0.35f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFE91E63), badgeInk = Color.White, badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF880E4F), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE91E63), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFF6A3040).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE91E63).copy(alpha = 0.55f))
        if (t.contains("MICHAEL JACKSON")) return SignatureDesign(bg = Color(0xFF0A0A0A), cornerRadius = 6f, drawBackground = { w, h -> val path = Path().apply { moveTo(w * 0.50f, 0f); lineTo(w * 0.30f, h * 0.60f); lineTo(w * 0.70f, h * 0.60f); close() }; drawPath(path, Color(0xFFFFD700).copy(alpha = 0.21f)); for (i in 0 until 6) { val x = w * 0.42f + ((i * 3571) % 100) / 100f * w * 0.16f; val y = h * 0.05f + ((i * 4201) % 100) / 100f * h * 0.25f; drawCircle(Color(0xFFFFD700).copy(alpha = 0.35f), 1.5f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFD700), badgeInk = Color(0xFF0A0A0A), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFFFD700), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC0A030), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFD0D0D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFFD700).copy(alpha = 0.65f))
        if (t.contains("PINK FLOYD")) return SignatureDesign(bg = Color(0xFF0A0A2E), cornerRadius = 6f, drawBackground = { w, h -> val cx = w * 0.75f; val cy = h * 0.20f; val s2 = w * 0.07f; val path = Path().apply { moveTo(cx, cy - s2); lineTo(cx + s2, cy + s2); lineTo(cx - s2, cy + s2); close() }; drawPath(path, Color.White.copy(alpha = 0.28f), style = Stroke(1.5f)); val colors = listOf(Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF0088FF), Color(0xFF8800FF)); colors.forEachIndexed { i, c -> drawLine(c.copy(alpha = 0.25f), Offset(cx + s2, cy), Offset(cx + s2 + w * 0.12f, cy - s2 * 0.5f + i * s2 * 0.17f), strokeWidth = 1f) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF7B1FA2), badgeInk = Color.White, badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFE0D0F0), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF7B1FA2), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFC0B0D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF7B1FA2).copy(alpha = 0.55f))
        if (t.contains("NIRVANA")) return SignatureDesign(bg = Color(0xFF0A0A0A), cornerRadius = 4f, drawBackground = { w, h -> val s = (w * 1000 + h).toInt(); for (i in 0 until 60) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.14f), 1.5f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFEB3B), badgeInk = Color(0xFF0A0A0A), badgeRadius = 2.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFFFEB3B), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC0B020), bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFFD0D0B0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFC0B020).copy(alpha = 0.65f))
        if (t.contains("DAVID BOWIE")) return SignatureDesign(bg = Color(0xFF1565C0), cornerRadius = 6f, drawBackground = { w, h -> val path = Path().apply { moveTo(w * 0.75f, h * 0.04f); lineTo(w * 0.70f, h * 0.28f); lineTo(w * 0.78f, h * 0.24f); lineTo(w * 0.72f, h * 0.50f) }; drawPath(path, Color(0xFFD32F2F).copy(alpha = 0.30f), style = Stroke(2.5f)); drawStar(w * 0.20f, h * 0.10f, 3f, 1.5f, Color(0xFFD32F2F).copy(alpha = 0.35f)) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFD32F2F), badgeInk = Color.White, badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFE0F0FF), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFD32F2F), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0D8F0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFD32F2F).copy(alpha = 0.65f))
        if (t.contains("ELVIS")) return SignatureDesign(bg = Color(0xFFFFD700), cornerRadius = 8f, drawBackground = { w, h -> for (i in 0 until 3) { val x = w * 0.10f + i * w * 0.30f; val path = Path().apply { moveTo(x, h * 0.05f); lineTo(x - 4f, h * 0.18f); lineTo(x + 4f, h * 0.16f); lineTo(x - 2f, h * 0.30f) }; drawPath(path, Color(0xFF0A0A0A).copy(alpha = 0.21f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF0A0A0A), badgeInk = Color(0xFFFFD700), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2810), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF0A0A0A), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A3820).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF0A0A0A).copy(alpha = 0.65f))
    }

    // ══ ANIME ══
    if (family == CategoryFamily.ANIME_COMICS) {
        if (t.contains("NARUTO")) return SignatureDesign(bg = Color(0xFFFF6D00), cornerRadius = 4f, drawBackground = { w, h -> for (i in 0 until 10) { val angle = i * 36f; val rad = Math.toRadians(angle.toDouble()).toFloat(); val r = w * 0.015f + i * w * 0.004f; drawCircle(Color(0xFF1565C0).copy(alpha = 0.35f), 1.5f, Offset(w * 0.80f + kotlin.math.cos(rad) * r, h * 0.15f + kotlin.math.sin(rad) * r)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF1565C0), badgeInk = Color.White, badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 30.sp, titleLineHeight = 34.sp, titleColor = Color(0xFF3A2010), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1565C0), bodySize = 10f, bodyLineHeight = 1.50f, bodyColor = Color(0xFF5A3010).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1565C0).copy(alpha = 0.65f))
        if (t.contains("DRAGON BALL")) return SignatureDesign(bg = Color(0xFFFF6D00), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 7) { val x = w * 0.15f + (i % 4) * w * 0.16f; val y = h * 0.08f + (i / 4) * h * 0.10f; drawCircle(Color(0xFFFFD700).copy(alpha = 0.30f), w * 0.025f, Offset(x, y)); drawStar(x, y, 2f, 1f, Color(0xFFFFD700).copy(alpha = 0.28f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF1565C0), badgeInk = Color.White, badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2010), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1565C0), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF5A3010).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1565C0).copy(alpha = 0.65f))
        if (t.contains("ONE PIECE")) return SignatureDesign(bg = Color(0xFFC62828), cornerRadius = 4f, drawBackground = { w, h -> for (i in 0 until 3) { val path = Path().apply { moveTo(0f, h * 0.80f + i * h * 0.04f); for (j in 0..15) { lineTo(j * w / 15f, h * 0.80f + i * h * 0.04f + kotlin.math.sin(j * 0.8f + i).toFloat() * h * 0.02f) } }; drawPath(path, Color(0xFF1565C0).copy(alpha = 0.28f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF1565C0), badgeInk = Color.White, badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF0E0), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF1565C0), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0C0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF1565C0).copy(alpha = 0.65f))
        if (t.contains("DEMON SLAYER")) return SignatureDesign(bg = Color(0xFF1A0A0A), cornerRadius = 4f, drawBackground = { w, h -> drawArc(Color(0xFFFF3D00).copy(alpha = 0.25f), 200f, 160f, false, Offset(w * 0.55f, h * 0.02f), Size(w * 0.35f, h * 0.28f), style = Stroke(1.5f)); val path = Path().apply { moveTo(w * 0.05f, h * 0.85f); for (i in 0..12) { lineTo(w * 0.05f + i * w * 0.07f, h * 0.85f + kotlin.math.sin(i * 0.8f).toFloat() * h * 0.025f) } }; drawPath(path, Color(0xFF00897B).copy(alpha = 0.35f), style = Stroke(1.5f)) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFF3D00), badgeInk = Color.White, badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFF0E0), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFFF3D00), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0C0B0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFF3D00).copy(alpha = 0.65f))
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
        if (t.contains("EGYPT") || t.contains("PYRAMID")) return SignatureDesign(bg = Color(0xFFF0E0C0), cornerRadius = 8f, drawBackground = { w, h -> val path = Path().apply { moveTo(w * 0.60f, h * 0.08f); lineTo(w * 0.50f, h * 0.35f); lineTo(w * 0.70f, h * 0.35f); close() }; drawPath(path, Color(0xFFC9A959).copy(alpha = 0.20f)); drawPath(path, Color(0xFFC9A959).copy(alpha = 0.20f), style = Stroke(1f)); for (i in 0 until 15) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h * 0.3f; drawCircle(Color(0xFFC9A959).copy(alpha = 0.15f), 1f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A959), badgeInk = Color(0xFF3A2810), badgeRadius = 8.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF5A4020), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9A959), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF6A5030).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A959).copy(alpha = 0.65f))
        // FIX: use "ROME " with context to avoid "Romeo" false positive
        if (t.contains("COLISEUM") || t.contains("ROME ") || t.startsWith("ROME")) return SignatureDesign(bg = Color(0xFFF0EDE8), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 3) { val cx = w * 0.20f + i * w * 0.30f; drawLine(Color(0xFF8B0000).copy(alpha = 0.15f), Offset(cx, h * 0.08f), Offset(cx, h * 0.85f), strokeWidth = 2f) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF8B0000), badgeInk = Color.White, badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2020), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF8B0000), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF5A3030).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFF8B0000).copy(alpha = 0.65f))
        if (t.contains("SAMURAI")) return SignatureDesign(bg = Color(0xFF1A0A0A), cornerRadius = 4f, drawBackground = { w, h -> drawLine(Color(0xFFC62828).copy(alpha = 0.30f), Offset(w * 0.75f, h * 0.05f), Offset(w * 0.75f, h * 0.90f), strokeWidth = 1.5f); for (i in 0 until 5) { val x = w * 0.10f + i * w * 0.04f; drawCircle(Color(0xFFFF80AB).copy(alpha = 0.15f), 3f, Offset(x, h * 0.10f + i * h * 0.03f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC62828), badgeInk = Color.White, badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFCDD2), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC62828), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0C0C0).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFC62828).copy(alpha = 0.65f))
        if (t.contains("VIKING")) return SignatureDesign(bg = Color(0xFF1A1A2E), cornerRadius = 4f, drawBackground = { w, h -> for (i in 0 until 8) { val x = ((i * 7919) % 10000) / 10000f * w; val y = ((i * 6271) % 10000) / 10000f * h; drawCircle(Color(0xFFB0BEC5).copy(alpha = 0.18f), 1.5f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFB0BEC5), badgeInk = Color(0xFF1A1A2E), badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFB0BEC5), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFB0BEC5), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0C8D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFB0BEC5).copy(alpha = 0.65f))
    }

    // ══ SPORTS ══
    if (family == CategoryFamily.SPORTS) {
        if (t.contains("OLYMPICS")) return SignatureDesign(bg = Color(0xFF0D1B3E), cornerRadius = 6f, drawBackground = { w, h -> val ringColors = listOf(Color(0xFF0088FF), Color(0xFFFFEB3B), Color(0xFF000000), Color(0xFF00C853), Color(0xFFF44336)); for (i in 0 until 5) { drawCircle(ringColors[i].copy(alpha = 0.25f), w * 0.03f, Offset(w * 0.30f + i * w * 0.08f, h * 0.15f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFFFD700), badgeInk = Color(0xFF0D1B3E), badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFFFFD700), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFFFD700), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFC0C8E0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFFFD700).copy(alpha = 0.65f))
        if (t.contains("CRICKET")) return SignatureDesign(bg = Color(0xFFF5F5F0), cornerRadius = 6f, drawBackground = { w, h -> drawLine(Color(0xFFC62828).copy(alpha = 0.20f), Offset(w * 0.50f, h * 0.05f), Offset(w * 0.50f, h * 0.95f), strokeWidth = 1f); drawCircle(Color(0xFFC62828).copy(alpha = 0.15f), w * 0.03f, Offset(w * 0.65f, h * 0.30f)) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC62828), badgeInk = Color.White, badgeRadius = 6.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF333333), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFC62828), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A4A4A).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFFC62828).copy(alpha = 0.65f))
    }

    // ══ INTERNET & TECH ══
    if (family == CategoryFamily.INTERNET) {
        if (t.contains("BITCOIN")) return SignatureDesign(bg = Color(0xFFF7931A), cornerRadius = 6f, drawBackground = { w, h -> val s = (w * 1000 + h).toInt(); for (i in 0 until 20) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color(0xFF0A0A0A).copy(alpha = 0.10f), 1f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF0A0A0A), badgeInk = Color(0xFFF7931A), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF3A2810), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFF0A0A0A), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFF4A3820).copy(alpha = 0.85f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF0A0A0A).copy(alpha = 0.65f))
        if (t.contains("SPACEX") || t.contains("NASA")) return SignatureDesign(bg = Color(0xFF0A0A14), cornerRadius = 4f, drawBackground = { w, h -> val s = (w * 1000 + h).toInt(); for (i in 0 until 50) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.20f), 1f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF42A5F5), badgeInk = Color(0xFF0A0A14), badgeRadius = 4.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 2.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF90CAF9), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF42A5F5), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0C0D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF42A5F5).copy(alpha = 0.65f))
    }

    // ══ WILDCARD ══
    if (family == CategoryFamily.WILDCARD) {
        if (t.contains("PHILOSOPHY")) return SignatureDesign(bg = Color(0xFF1A1A2E), cornerRadius = 6f, drawBackground = { w, h -> drawLine(Color(0xFFC9A959).copy(alpha = 0.20f), Offset(w * 0.12f, h * 0.08f), Offset(w * 0.12f, h * 0.85f), strokeWidth = 2f) }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFC9A959), badgeInk = Color(0xFF1A1A2E), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFC9A959), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9A959), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD0C8A0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFC9A959).copy(alpha = 0.55f))
        if (t.contains("PSYCHOLOGY")) return SignatureDesign(bg = Color(0xFF1A0A2E), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 8) { val angle = i * 45f; val rad = Math.toRadians(angle.toDouble()).toFloat(); drawLine(Color(0xFFE91E63).copy(alpha = 0.15f), Offset(w * 0.80f, h * 0.15f), Offset(w * 0.80f + kotlin.math.cos(rad) * w * 0.08f, h * 0.15f + kotlin.math.sin(rad) * h * 0.08f), strokeWidth = 0.8f) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFFE91E63), badgeInk = Color.White, badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = LoraFontFamily, titleSize = 28.sp, titleLineHeight = 34.sp, titleColor = Color(0xFFF8BBD0), metaSpacer = 5.dp, metaSeparator = " \u2022 ", metaSize = 10.sp, metaColor = Color(0xFFE91E63), bodySize = 10f, bodyLineHeight = 1.60f, bodyColor = Color(0xFFD0C0D8).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = LoraFontFamily, footerColor = Color(0xFFE91E63).copy(alpha = 0.55f))
        if (t.contains("OCEAN") || t.contains("SEA")) return SignatureDesign(bg = Color(0xFF0A1428), cornerRadius = 6f, drawBackground = { w, h -> for (i in 0 until 3) { val wavePath = Path().apply { moveTo(0f, h * 0.70f + i * h * 0.06f); for (j in 0..20) { lineTo(j * w / 20f, h * 0.70f + i * h * 0.06f + kotlin.math.sin(j * 0.5f + i).toFloat() * h * 0.02f) } }; drawPath(wavePath, Color(0xFF00BCD4).copy(alpha = 0.20f - i * 0.05f), style = Stroke(1.5f)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF00BCD4), badgeInk = Color(0xFF0A1428), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF80DEEA), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF00BCD4), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0D0E0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF00BCD4).copy(alpha = 0.65f))
        if (t.contains("SPACE")) return SignatureDesign(bg = Color(0xFF0A0A14), cornerRadius = 6f, drawBackground = { w, h -> val s = (w * 1000 + h).toInt(); for (i in 0 until 60) { val x = ((s * (i+1) * 7919) % 10000) / 10000f * w; val y = ((s * (i+1) * 6271) % 10000) / 10000f * h; drawCircle(Color.White.copy(alpha = 0.20f), 1f, Offset(x, y)) } }, padding = PaddingValues(22.dp), badgeColor = Color(0xFF42A5F5), badgeInk = Color(0xFF0A0A14), badgeRadius = 14.dp, badgeHPadding = 10.dp, badgeVPadding = 5.dp, badgeIconSize = 12.dp, badgeFontSize = 8.sp, badgeLetterSpacing = 1.5.sp, titleTopSpacer = 14.dp, titleFont = ChangaOneFontFamily, titleSize = 28.sp, titleLineHeight = 32.sp, titleColor = Color(0xFF90CAF9), metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF42A5F5), bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFB0C0D0).copy(alpha = 0.88f), footerSpacer = 8.dp, footerFont = GeomFontFamily, footerColor = Color(0xFF42A5F5).copy(alpha = 0.65f))
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF8B1A1A),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF5C3317).copy(alpha = 0.65f),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF6A7AAA),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF8B7420),
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
            metaSpacer = 6.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF8A8A8A),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF8A6B42),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFFF9A8B),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 11.sp, metaColor = Color(0xFF8A8278),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 11.sp, metaColor = Color(0xFF7A3A2E),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFD9B45F),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9A24F),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFE8C84F),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFE8A5A0),
            bodySize = 10f, bodyLineHeight = 1.55f, bodyColor = Color(0xFFE0D0C8).copy(alpha = 0.88f),
            footerSpacer = 8.dp, footerFont = AntonFontFamily, footerColor = Color(0xFFE8A5A0).copy(alpha = 0.65f),
            layout = SignatureLayout.POSTER
        )
        // ══�� FOOD — table, Corben title ═══
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFD08840),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9B8E0),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFFF9AB8),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFF8AAA70),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9B8E0),
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
            metaSpacer = 5.dp, metaSeparator = " · ", metaSize = 10.sp, metaColor = Color(0xFFC9A227),
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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v369 — the Adjust tool's sat/contrast, applied to the BACKGROUND layer
    // only (never the text above it).
    bgFilter: ColorFilter? = null,
    // v375 — rich-text runs over this card's fact body (threaded from
    // TopicShareCard so FactBody can render the styled runs).
    factSpans: List<TextSpan> = emptyList()
) {
    val body = quoteText ?: factText
    val title = if (quoteText != null) (quoteAuthor?.takeIf { it.isNotBlank() } ?: byline.ifBlank { "Quote" }) else display
    val sig = topicVariant(topicName, family) ?: signatureDesign(categoryName, family)

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(sig.cornerRadius.dp))) {
        // v369 — background layer (fill + drawn scene) wears the sat/contrast
        // filter; the content column below is NOT filtered.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { colorFilter = bgFilter }
                .background(sig.bg, RoundedCornerShape(sig.cornerRadius.dp))
        ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            sig.drawBackground(this, w, h)
        }
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
                ), move), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis, modifier = Modifier.titleShift(move).moveMeta(move).onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) })
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
            // v3xx — re-report on every composition (see Vinyl's comment).
            androidx.compose.runtime.LaunchedEffect(factStyle) { callbacks.onFactStyle(factStyle) }
            // v329 — Reading-progress content draws the chapter widget in the
            // signature colors (bar in the badge tone, caption in body ink)
            // instead of the prose.
            // v335 — a custom fact stacks UNDER the progress widget.
            if (chapterProgress != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.moveFact(move).onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(factStyle)
                    }
                ) {
                    ChapterProgressBlock(
                        progress = chapterProgress,
                        fill = sig.badgeColor,
                        track = sig.bodyColor.copy(alpha = 0.16f),
                        ink = sig.bodyColor
                    )
                    if (chapterFact.isNotBlank()) {
                        FactBody(text = chapterFact, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 14 else 10, move.factHeightFrac, bodyScale))
                    }
                }
            } else {
                FactBody(text = body, style = factStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 14 else 10, move.factHeightFrac, bodyScale), modifier = Modifier.moveFact(move).onGloballyPositioned {
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
    chapterProgress: ChapterProgressUi? = null,
    // v335 — a stacked custom fact rendered below the progress widget.
    chapterFact: String = "",
    // v376 — GLUED cover: rendered beside the title in the block below.
    coverArt: androidx.compose.ui.graphics.ImageBitmap? = null,
    coverW: Dp = 44.dp,
    coverH: Dp = 66.dp,
    // v375 — rich-text runs threaded from PaperCard (the only caller).
    factSpans: List<TextSpan> = emptyList()
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (quoteText != null) {
            CurioIcon(name = CurioIcons.FormatQuote, tint = palette.ink.copy(alpha = 0.20f), size = 32.dp)
            // v379d/e — the quote + frost body MUST carry an explicit ink that
            // reads on Paper's blended pane (see [frostInk]): they used to
            // inherit the APP theme's onSurface, which went dark-on-dark on
            // dark premium tones and wrong in dark mode.
            val qStyle = factBodyStyle(MaterialTheme.typography.titleLarge.copy(
                fontFamily = LoraFontFamily, fontSize = qSize,
                lineHeight = (qSize.value * 1.28f).sp, color = palette.frostInk()
            ), move)
            // v3xx — re-report on every composition (see Vinyl's comment).
            androidx.compose.runtime.LaunchedEffect(qStyle) { callbacks.onFactStyle(qStyle) }
            FactBody(text = quoteText, style = qStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = lines(if (aspect == ShareCardAspect.PORTRAIT) 12 else 8, move.factHeightFrac), modifier = Modifier.moveFact(move).onGloballyPositioned {
                callbacks.onFact(it.boundsInWindow())
                callbacks.onFactStyle(qStyle)
            })
        } else {
            // Metadata (byline • year / author • date) — reused by both the
            // plain and the glued-cover title layouts below.
            val metaParts = mutableListOf<String>()
            if (byline.isNotBlank()) metaParts.add(byline)
            if (year != null) metaParts.add(year)
            val metaLine: @Composable () -> Unit = {
                if (metaParts.isNotEmpty()) {
                    Text(
                        metaParts.joinToString(" \u2022 "),
                        style = metaStyle(MaterialTheme.typography.labelSmall.copy(fontFamily = LoraFontFamily, fontWeight = FontWeight.SemiBold), move),
                        color = palette.ink.copy(alpha = 0.50f), maxLines = lines(2, move.metaHeightFrac, max = 2), overflow = TextOverflow.Ellipsis,
                        // v316b — sits right under the title, so it travels
                        // with the T drag too (in the plain layout via
                        // [titleShift]; the glued layout is inside the moved
                        // row so only its own M offset applies).
                        modifier = Modifier
                            .then(if (coverArt == null) Modifier.titleShift(move) else Modifier)
                            .moveMeta(move)
                            .onGloballyPositioned { callbacks.onMeta(it.boundsInWindow()) }
                    )
                }
            }
            val titleLine: @Composable (Modifier) -> Unit = { titleMod ->
                Text(
                    display,
                    style = titleStyle(MaterialTheme.typography.headlineLarge.copy(fontFamily = ChangaOneFontFamily, lineHeight = 40.sp), move),
                    color = palette.ink, maxLines = lines(3, move.titleHeightFrac),
                    overflow = TextOverflow.Ellipsis,
                    modifier = titleMod.onGloballyPositioned { callbacks.onTitle(it.boundsInWindow()) }
                )
            }
            // v376 — GLUED cover: with a cover the title block becomes a Row
            // (cover | title+author) whose move offsets apply to the WHOLE
            // group — the jacket always sits beside the title and rides
            // every title drag / collision push / auto lift.
            if (coverArt != null) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.glueTitleMove(move)
                ) {
                    GluedCover(coverArt, coverW, coverH, move, callbacks, topPad = 6.dp)
                    Spacer(Modifier.width(CoverTitleGap))
                    Column(Modifier.weight(1f)) {
                        titleLine(Modifier.titleSize(move))
                        metaLine()
                    }
                }
            } else {
                titleLine(Modifier.moveTitle(move))
                metaLine()
            }
            if (ratingStars != null && ratingStars > 0) StarRow(ratingStars, palette)
            // v316b — style hoisted so the pane reports it to the editor.
            val qfs = if (aspect == ShareCardAspect.CLASSIC) quickFactFontSize34(factText.length) else quickFactFontSize(factText.length)
            val qfsScaled = (qfs.value * bodyScale).sp
            val frostStyle = factBodyStyle(MaterialTheme.typography.bodySmall.copy(
                fontFamily = LoraFontFamily, fontSize = qfsScaled,
                lineHeight = (qfsScaled.value * 1.4f).sp,
                // v379d/e — see the quote above: explicit ink matched to the
                // pane's blended luminance (white on dark premium tones).
                color = palette.frostInk()
            ), move)
            // v3xx — re-report on every composition (see Vinyl's comment): the
            // frost pane clips at its maxLines, so a fit-shrunk text keeps
            // the same bounds and the position-driven report goes stale.
            androidx.compose.runtime.LaunchedEffect(frostStyle) { callbacks.onFactStyle(frostStyle) }
            FrostPane(palette, Modifier.moveFact(move)) {
                // v329 — Reading-progress content draws the visual chapter
                // widget here (same bounds reporting, so the editor's box +
                // grip sit on the bar exactly as they would on text).
                if (chapterProgress != null) {
                    // v335 — a custom fact stacks UNDER the progress widget
                    // (progress stays a separate bar; the fact is prose).
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.onGloballyPositioned {
                            callbacks.onFact(it.boundsInWindow())
                            callbacks.onFactStyle(frostStyle)
                        }
                    ) {
                        ChapterProgressBlock(
                            progress = chapterProgress,
                            fill = palette.accent,
                            track = palette.ink.copy(alpha = 0.14f),
                            ink = palette.ink
                        )
                        if (chapterFact.isNotBlank()) {
                            FactBody(text = chapterFact, style = frostStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 20 else 14, move.factHeightFrac, bodyScale))
                        }
                    }
                } else {
                    // v316b — the glyph box reports the bounds, NOT the frost
                    // pane: the pane's own 18dp padding would otherwise push
                    // the typing field's caret 18dp off the visible letters.
                    // v335 — the cap tracks the font multiplier so the pane
                    // keeps its footprint while the TEXT size changes: smaller
                    // fonts fit more lines, larger fonts fewer.
                    FactBody(text = factText, style = frostStyle, format = move.factFormat, dropCap = move.factDropCap, gutterFrac = move.factGutter, spans = factSpans, aspect = aspect, maxLines = fitLines(if (aspect == ShareCardAspect.PORTRAIT) 20 else 14, move.factHeightFrac, bodyScale), modifier = Modifier.onGloballyPositioned {
                        callbacks.onFact(it.boundsInWindow())
                        callbacks.onFactStyle(frostStyle)
                    })
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
    // v3xx — SMALLER: the icon, quote-author line and credit all dropped a
    // size, so the footer is a quiet caption that fits in the space above
    // the torn bottom strip instead of crowding it (or being pushed past
    // the card edge by a grown fact box).
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CurioIcon(name = CurioIcons.Lightbulb, tint = palette.ink.copy(alpha = 0.26f), size = 12.dp)
        Spacer(Modifier.height(2.dp))
        if (quoteText != null && !quoteAuthor.isNullOrBlank()) {
            Text("$quoteAuthor", style = MaterialTheme.typography.labelSmall.copy(fontFamily = LoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 9.sp), color = palette.ink.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Spacer(Modifier.height(2.dp))
        }
        Text(
            if (quoteText != null) { if (sharerName.isNotBlank()) "$sharerName · Stay curious" else "Stay curious" }
            else { if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio" },
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = GeomFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
            color = palette.ink.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CANVAS DRAWING HELPERS
// ═════════════════════════════════��═════════════════════════════════════
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
private fun Watermark(family: CategoryFamily, glyph: String, tint: Color, seed: Int, center: Boolean = true) {
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
        // Center watermark — fainter, larger, with its own tilt. v3xx —
        // optional: the Collage card turns it OFF (its "big category icon in
        // the middle" read as clutter over the photo/field).
        if (center) {
            CurioIcon(name = symbols[seed.mod(symbols.size)], tint = tint.copy(alpha = tint.alpha * 0.5f), size = 80.dp,
                modifier = Modifier.offset((w * 0.5f - 40).dp, (h * 0.5f - 40).dp)
                    .graphicsLayer { rotationZ = -7f })
        }
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
    // v378 — DOUBLE-TAP the fact text arms inline editing directly (the
    // same path the "Edit text" tool uses): no tool-dance for a quick
    // typo fix. The sheet decides what "edit this" means (it converts the
    // default quick fact into a custom fact so the typed text sticks).
    onRequestInlineFactEdit: () -> Unit = {},
    onToggleEdit: () -> Unit,
    onSelectResizeTarget: (ShareCardResizeTarget) -> Unit = {},
    selectedResizeTarget: ShareCardResizeTarget = ShareCardResizeTarget.NONE,
    move: ShareCardMove = ShareCardMove(),
    onMove: (ShareCardMove) -> Unit = {},
    // Metrics for the transparent typing field — must match the card's fact
    // rendering so the caret sits EXACTLY on the visible text (see the
    // compute in TopicShareSheet).
    factFieldStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    // v369 — smart auto-fit delta (computed by the sheet from the current
    // fact length): the fact handle seeds it into the manual move on the
    // first grab so the box doesn't jump when auto-fit hands off.
    autoFitDelta: ShareAutoFitDelta = ShareAutoFitDelta(),
    // v369 — the chapter-review fact box renders a one-line chapter chip
    // above the review text; the inline field shifts down one line so its
    // caret sits on the review glyphs, not the chip.
    factFieldChipShift: Boolean = false,
    factFieldPlaceholder: String = "Edit the quick fact…",
    // v375 — FULLSCREEN selection formatting (Save-your-take style): when
    // [onFormatFactSelection] is provided, the inline fact field keeps a real
    // selection (TextFieldValue) and floats the B / I / highlighter bar over
    // the selected text; taps call back with the selection + flag so the
    // sheet can toggle spans. [factSpans] drives the bar's active states.
    richFactTools: Boolean = false,
    factSpans: List<TextSpan> = emptyList(),
    onFormatFactSelection: ((s: Int, e: Int, flag: RichFlag) -> Unit)? = null,
    // v379 — per-letter UNDERLINE: the floating bar's U button uses its own
    // span channel (see [toggleSpanUnderline]) so the Save-your-take
    // RichFlag set (bold / italic / highlight) stays untouched.
    onToggleFactUnderline: ((s: Int, e: Int, add: Boolean) -> Unit)? = null,
    // v384 — live measured card-local bounds (title / fact / info rows /
    // favorites strip / category pill) reported so the sheet's sparkle can
    // repair REAL overlaps — a grown fact box covering the info rows or the
    // category pill is invisible to offset-based estimates. Rect.Zero =
    // element absent on this card.
    onMeasuredBounds: ((androidx.compose.ui.geometry.Rect, androidx.compose.ui.geometry.Rect, androidx.compose.ui.geometry.Rect, androidx.compose.ui.geometry.Rect, androidx.compose.ui.geometry.Rect) -> Unit)? = null,
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
    // v335 — the book-cover jacket badge's bounds (0 until a cover exists).
    val coverRect = androidx.compose.runtime.remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // v353 — the album favorite-tracks strip's bounds (0 while hidden).
    val favRect = androidx.compose.runtime.remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // v3xx — the COLLAGE polaroid's bounds (0 until a polaroid exists).
    val polaroidRect = androidx.compose.runtime.remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // v316b — the card reports the ACTUAL style its fact renders with, so the
    // invisible field above it uses identical metrics (family/size/line).
    val liveFactStyle = androidx.compose.runtime.remember { mutableStateOf(factFieldStyle) }
    val boundsHub = androidx.compose.runtime.remember {
        EditBoundsCallbacks(
            onTitle = { r -> titleRect.value = r },
            onFact = { r -> factRect.value = r },
            onMeta = { r -> metaRect.value = r },
            onBadge = { r -> badgeRect.value = r },
            onCover = { r -> coverRect.value = r },
            onFavTrack = { r -> favRect.value = r },
            onPolaroid = { r -> polaroidRect.value = r },
            onFactStyle = { s -> liveFactStyle.value = s }
        )
    }
    // v323 — tapping any other element must end the quick-fact's text editing:
    // clear focus (closes the keyboard) before switching the selection.
    val focusManager = LocalFocusManager.current
    val factRequester = remember { FocusRequester() }
    // v375 — rich-fact editing state for the floating selection bar: the
    // inline field keeps its own TextFieldValue (so a text selection is
    // visible + actionable) and reports its text layout so the bar can float
    // over the selected characters. Only engaged when [richFactTools];
    // v379c — BOTH the bottom-sheet preview and the full-screen dialog pass
    // richFactTools = true, so a selection gets the floating B / I / U /
    // highlight bar on every editing surface (quotes + reading progress
    // carry no spans, so they fall back to plain caret editing). The
    // selection survives recompositions because it lives HERE, not in the
    // sheet — typing echoes text up but the TextFieldValue (and its
    // selection/caret) is what BasicTextField draws.
    var richFactTfv by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var factTextLayout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    // Re-seed only when the sheet pushes DIFFERENT text (e.g. switching the
    // active content) — typing echoes back the same text so the selection /
    // caret survive every keystroke (the RichTextEditor pattern).
    // v3xx — the seed carries the SAME rich runs the card renders
    // (bold/italic/highlight/underline + per-run font sizes from the
    // enlarge editor): the card draws those runs at different sizes, and a
    // plain-text field would wrap differently on those runs — the caret
    // would sit off the visible glyphs. The field stays transparent; the
    // annotated string only fixes its METRICS. [factSpans] here are the
    // un-shifted runs (the field edits the bare review text, without the
    // chapter chip the card prefixes).
    androidx.compose.runtime.LaunchedEffect(editFact, richFactTools, factSpans) {
        if (richFactTools && richFactTfv.text != editFact) {
            richFactTfv = androidx.compose.ui.text.input.TextFieldValue(
                if (factSpans.isEmpty()) androidx.compose.ui.text.AnnotatedString(editFact)
                else buildRichAnnotated(editFact, factSpans, ShareFactMarker),
                androidx.compose.ui.text.TextRange(editFact.length)
            )
        } else if (richFactTools && factSpans.isNotEmpty() && richFactTfv.annotatedString.spanStyles != buildRichAnnotated(editFact, factSpans, ShareFactMarker).spanStyles) {
            // Text unchanged but the RUNS changed (a bold/italic/highlight /
            // size toggle): refresh the annotation so the field's metrics
            // follow the card — keeping the user's live selection/caret.
            richFactTfv = androidx.compose.ui.text.input.TextFieldValue(
                buildRichAnnotated(editFact, factSpans, ShareFactMarker),
                richFactTfv.selection
            )
        }
    }
    Box(
        modifier = Modifier
            .onGloballyPositioned { cardOrigin.value = it.positionInWindow() }
            .pointerInput(active, editMode) {
                // While editing, the gesture is disabled so taps reach the fields.
                if (active && !editMode) detectTapGestures(onLongPress = { onToggleEdit() })
            }
    ) {
        card(boundsHub)
        // v3xx — report the live card-local bounds to the sheet's sparkle in
        // BOTH modes (the old wiring only ran while editing, so a tap on the
        // resting preview repaired nothing — a size-grown box or a lifted
        // title could overlap the category pill unseen). The origin is only
        // known once the card has laid out; until then the sheet keeps its
        // last rects.
        val reportDensity = androidx.compose.ui.platform.LocalDensity.current
        androidx.compose.runtime.LaunchedEffect(
            titleRect.value, factRect.value, metaRect.value, badgeRect.value, favRect.value, cardOrigin.value
        ) {
            val origin = cardOrigin.value
            if (origin != androidx.compose.ui.geometry.Offset.Zero) {
                fun local(r: androidx.compose.ui.geometry.Rect): androidx.compose.ui.geometry.Rect =
                    with(reportDensity) {
                        androidx.compose.ui.geometry.Rect(
                            (r.left - origin.x).toDp().value,
                            (r.top - origin.y).toDp().value,
                            (r.right - origin.x).toDp().value,
                            (r.bottom - origin.y).toDp().value
                        )
                    }
                onMeasuredBounds?.invoke(
                    if (!quoteMode) local(titleRect.value) else androidx.compose.ui.geometry.Rect.Zero,
                    local(factRect.value),
                    local(metaRect.value),
                    local(favRect.value),
                    local(badgeRect.value)
                )
            }
        }
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

                // v341/v342 — PicsArt-style alignment guides: while a selected
                // box is dragged, its edges and centre align with the
                // card's own edges + centre lines AND with the other boxes'
                // edges and centres; v353 — GUIDE-ONLY (the magnet pull is
                // gone): the overlay draws a faint hint line full-card while
                // aligned, and the box never sticks.
                //
                // v342 — every selectable element's CURRENT card-local bounds,
                // computed ONCE here so a drag can align to the card frame AND
                // to the other boxes (see alignOthers / hCands / vCands below).
                // A zero rect = that element is absent on this style, which the
                // helpers skip.
                val rTitle = if (!quoteMode) local(titleRect.value) else androidx.compose.ui.geometry.Rect.Zero
                val rFact = local(factRect.value)
                val rMeta = local(metaRect.value)
                val rBadge = local(badgeRect.value)
                val rCover = local(coverRect.value)
                val rFav = local(favRect.value)
                val rPolaroid = local(polaroidRect.value)
                val rAll = listOf(rTitle, rFact, rMeta, rBadge, rCover, rFav, rPolaroid)

                // v342 — the OTHER boxes a live drag can magnet-align to
                // (excluding the dragged box itself by instance identity).
                fun alignOthers(dragged: androidx.compose.ui.geometry.Rect): List<androidx.compose.ui.geometry.Rect> =
                    rAll.filter { it.width > 0f && it.height > 0f && it !== dragged }

                // Horizontal candidates: (move offset that lands the dragged
                // box, card-local guide line) for every other box's left /
                // centre / right edge.
                fun hCands(
                    others: List<androidx.compose.ui.geometry.Rect>,
                    base: Float, w: Float
                ): List<Pair<Float, Float>> = others.flatMap { r ->
                    listOf(
                        r.left - base to r.left,
                        r.center.x - base - w / 2f to r.center.x,
                        r.right - base - w to r.right
                    )
                }

                // Vertical twin of hCands: top / centre / bottom edges.
                fun vCands(
                    others: List<androidx.compose.ui.geometry.Rect>,
                    base: Float, h: Float
                ): List<Pair<Float, Float>> = others.flatMap { r ->
                    listOf(
                        r.top - base to r.top,
                        r.center.y - base - h / 2f to r.center.y,
                        r.bottom - base - h to r.bottom
                    )
                }

                var dragGuides by remember { mutableStateOf(DragGuides()) }
                var dragActive by remember { mutableStateOf(false) }
                // v384 — the auto-lift and the drag collision-push are GONE:
                // manual placement may freely overlap the title, the fact box
                // and the info rows (whatever the user does wins). Only the
                // sparkle pill repairs an overlap now — it estimates the
                // collisions from the drag offsets and moves the title / fact
                // / info rows clear in one commit.

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

                // TITLE — tap selects; grip only when selected. When title and
                // fact overlap, the currently selected box owns the shared hit
                // region; tapping it toggles to the other box. This preserves
                // the simple overlapping-box behavior from the pre-magnetic
                // layout editor while keeping both targets reachable.
                val titleFactOverlap = !quoteMode &&
                    rTitle.width > 0f && rTitle.height > 0f &&
                    rFact.width > 0f && rFact.height > 0f &&
                    rTitle.overlaps(rFact)
                if (!quoteMode) {
                    val t = rTitle
                    val tOk = t.width > 0f && t.height > 0f
                    if (tOk) {
                        val isSel = sel == ShareCardResizeTarget.TITLE
                        Box(
                            modifier = Modifier
                                .offset(t.left.dp, t.top.dp)
                                .width(t.width.dp)
                                .height(t.height.dp)
                                .zIndex(if (isSel && titleFactOverlap) 2f else 0f)
                                // v3xx — no ripple/indication: selecting (and
                                // toggling between overlapping boxes) was slow
                                // and flashy because every tap painted a press
                                // ripple on a selection tap — instant, clean
                                // selection now, like the fact's own layer.
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusManager.clearFocus()
                                    onSelectResizeTarget(
                                        if (titleFactOverlap && isSel) ShareCardResizeTarget.FACT
                                        else ShareCardResizeTarget.TITLE
                                    )
                                }
                                .border(1.dp, selBorder(isSel), RoundedCornerShape(8.dp))
                        )
                        // v369 — the grip + corner scale are drawn LAST (see
                        // the handles section below the element boxes), so a
                        // handle always wins the touch over a box it overlaps.
                    }
                }

                // v369 — the chapter-review box renders a one-line chapter
                // chip ABOVE the review text; the inline field shifts down by
                // one line so its caret sits on the review glyphs.
                val chipShiftPx = if (factFieldChipShift)
                    with(editDensity) { liveFactStyle.value.lineHeight.toDp().value } else 0f

                // Quick-fact — transparent field laid EXACTLY over the card's
                // own fact text (bounds AND style reported by the card, so the
                // caret sits on the visible glyphs of every font). Always
                // present (so tapping the fact focuses/selects it); its border
                // only appears once selected.
                val f = rFact
                val fOk = f.width > 0f && f.height > 0f
                if (fOk) {
                    val isSel = sel == ShareCardResizeTarget.FACT
                    // v375 — RICH (full-screen) editing keeps a real text
                    // SELECTION in the transparent field (see [richFactTfv]) so
                    // the floating B / I / highlighter bar can format exactly
                    // the selected characters. Plain (bottom-sheet) editing
                    // stays as-is: a caret-only field that routes raw text.
                    val fieldModifier = Modifier
                        .offset(f.left.dp, (f.top + chipShiftPx).dp)
                        .width(f.width.dp)
                        // v3xx — the field is EXACTLY the visible fact box:
                        // the old heightIn(min = …) let the transparent field
                        // grow to its 60-line budget while the card clipped
                        // the text at fitLines, so the caret could sit BELOW
                        // the visible glyphs and the selection outline grew
                        // past the box ("the cursor and text position are
                        // wrong/misleading", "the box outline misleads").
                        // The card's smart fit re-measures on every
                        // keystroke and grows the box, so the box and this
                        // field grow together — the caret stays on the
                        // glyphs and the outline hugs the box.
                        .heightIn(min = f.height.dp, max = f.height.dp)
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
                        // v371 — while typing, the selection chrome hides:
                        // no outline/highlight around the fact (the caret
                        // shows where you're typing); it returns when text
                        // editing ends.
                        .border(1.dp, if (factEditMode) Color.Transparent else selBorder(isSel), RoundedCornerShape(8.dp))
                    if (richFactTools && onFormatFactSelection != null) {
                        BasicTextField(
                            value = richFactTfv,
                            onValueChange = { nv ->
                                val prevText = richFactTfv.text
                                richFactTfv = nv
                                // Only text edits (not caret/selection moves)
                                // route up; the sheet rebases the fact spans.
                                if (nv.text != prevText) onFactChange(nv.text)
                            },
                            // v323 — INERT until the "Edit text" tool arms it.
                            enabled = factEditMode,
                            textStyle = liveFactStyle.value.copy(color = Color.Transparent),
                            cursorBrush = SolidColor(CoffeeChromeDeep),
                            singleLine = false,
                            maxLines = 60,
                            // v375 — text layout feeds the floating bar's anchor
                            // (caret rect of the live selection).
                            onTextLayout = { factTextLayout = it },
                            modifier = fieldModifier,
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth()) {
                                    if (richFactTfv.text.isBlank()) Text(factFieldPlaceholder, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                                    inner()
                                }
                            }
                        )
                    } else {
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
                            // v369 — the inline field grows with the box (long
                            // texts edit in place on the tall card).
                            maxLines = 60,
                            modifier = fieldModifier,
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth()) {
                                    if (editFact.isBlank()) Text(factFieldPlaceholder, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                                    inner()
                                }
                            }
                        )
                    }
                    // Tap-to-select layer: while text editing is OFF the field is
                    // inert, so an invisible box on top selects the fact for moving
                    // (grip appears) without popping the keyboard. Text editing
                    // starts only via the explicit "Edit text" tool — or a
                    // DOUBLE-TAP (v378), which arms the inline field directly.
                    if (!factEditMode) {
                        Box(
                            modifier = Modifier
                                .offset(f.left.dp, f.top.dp)
                                .width(f.width.dp)
                                .height(f.height.dp)
                                .zIndex(if (isSel && titleFactOverlap) 2f else 0f)
                                .combinedClickable(
                                    interactionSource = remember {
                                        androidx.compose.foundation.interaction.MutableInteractionSource()
                                    },
                                    indication = null,
                                    onClick = {
                                        onSelectResizeTarget(
                                            if (titleFactOverlap && isSel) ShareCardResizeTarget.TITLE
                                            else ShareCardResizeTarget.FACT
                                        )
                                    },
                                    onDoubleClick = {
                                        onSelectResizeTarget(ShareCardResizeTarget.FACT)
                                        onRequestInlineFactEdit()
                                    }
                                )
                        )
                    }
                    // When the "Edit text" tool arms the field, focus it so the
                    // keyboard opens right where the caret should sit.
                    androidx.compose.runtime.LaunchedEffect(factEditMode) {
                        if (factEditMode) factRequester.requestFocus()
                    }
                    // v375/v379c — RICH selection editing (full screen AND the
                    // bottom-sheet preview): while a real text selection is live
                    // in the transparent field, float the Save-your-take format
                    // bar (B / I / U / highlight) just above the selected
                    // letters. Taps toggle [factSpans] over exactly
                    // the selection via [onFormatFactSelection], so the format
                    // applies to the selected characters ONLY — never the whole
                    // fact (the whole-element toggles in the Text tools panel
                    // stay for whole-element formatting). Anchored like the
                    // RichTextEditor's own floating bar: a non-focusable Popup
                    // at the overlay origin, offset by the field position + the
                    // live selection caret (both in real px via [editDensity]).
                    if (richFactTools && (onFormatFactSelection != null || onToggleFactUnderline != null) &&
                        factEditMode && !richFactTfv.selection.collapsed
                    ) {
                        val fsLayout = factTextLayout
                        val fsSel = richFactTfv.selection
                        if (fsLayout != null) {
                            val fsD = editDensity
                            val fsStartCaret = runCatching { fsLayout.getCursorRect(fsSel.min) }.getOrNull()
                            val fsEndCaret = runCatching { fsLayout.getCursorRect(fsSel.max) }.getOrNull()
                            if (fsEndCaret != null) {
                                // v379 — 4 tools now (B / I / U / highlight):
                                // the bar grew past the old 132dp width.
                                val barW = with(fsD) { 172.dp.toPx() }
                                val barH = with(fsD) { 42.dp.toPx() }
                                val gap = with(fsD) { 6.dp.toPx() }
                                val edge = with(fsD) { 6.dp.toPx() }
                                val fieldX = with(fsD) { f.left.dp.toPx() }
                                val fieldY = with(fsD) { (f.top + chipShiftPx).dp.toPx() }
                                // Float ABOVE the selection's first line; drop
                                // below it when the selection is near the very
                                // top of the field.
                                val selTop = fsStartCaret?.top ?: fsEndCaret.top
                                var fsY = fieldY + selTop - barH - gap
                                if (fsY < 0f) fsY = fieldY + fsEndCaret.bottom + gap
                                val fsMaxX = (with(fsD) { cw.dp.toPx() } - barW - edge).coerceAtLeast(edge)
                                val fsX = (fieldX + (fsStartCaret?.left ?: fsEndCaret.left) - barW / 2f)
                                    .coerceIn(edge, fsMaxX)
                                androidx.compose.ui.window.Popup(
                                    alignment = androidx.compose.ui.Alignment.TopStart,
                                    offset = androidx.compose.ui.unit.IntOffset(fsX.roundToInt(), fsY.roundToInt()),
                                    properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                                ) {
                                    val s = fsSel.min
                                    val e = fsSel.max
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // v379b — the bar's visibility gate is
                                            // now `format != null || underline !=
                                            // null`, so the format callback is NO
                                            // longer smart-cast here: safe-invoke +
                                            // per-button enable like the U button.
                                            FormatToolButton(
                                                icon = CurioIcons.FormatBold,
                                                label = "Bold",
                                                active = spansFullyCovered(factSpans, s, e, RichFlag.BOLD),
                                                accent = MaterialTheme.colorScheme.primary,
                                                enabled = onFormatFactSelection != null,
                                                onClick = { onFormatFactSelection?.invoke(s, e, RichFlag.BOLD) }
                                            )
                                            FormatToolButton(
                                                icon = CurioIcons.FormatItalic,
                                                label = "Italic",
                                                active = spansFullyCovered(factSpans, s, e, RichFlag.ITALIC),
                                                accent = MaterialTheme.colorScheme.primary,
                                                enabled = onFormatFactSelection != null,
                                                onClick = { onFormatFactSelection?.invoke(s, e, RichFlag.ITALIC) }
                                            )
                                            // v379 — UNDERLINE rides the same
                                            // floating bar, toggling its own
                                            // span channel (per selected
                                            // letters, like B / I / highlight).
                                            FormatToolButton(
                                                icon = CurioIcons.FormatUnderline,
                                                label = "Underline",
                                                active = spansUnderlineCovered(factSpans, s, e),
                                                accent = MaterialTheme.colorScheme.primary,
                                                enabled = onToggleFactUnderline != null,
                                                onClick = { onToggleFactUnderline?.invoke(s, e, !spansUnderlineCovered(factSpans, s, e)) }
                                            )
                                            FormatToolButton(
                                                icon = CurioIcons.FormatHighlight,
                                                label = "Highlight",
                                                active = spansFullyCovered(factSpans, s, e, RichFlag.HIGHLIGHT),
                                                accent = MaterialTheme.colorScheme.primary,
                                                enabled = onFormatFactSelection != null,
                                                onClick = { onFormatFactSelection?.invoke(s, e, RichFlag.HIGHLIGHT) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // v369 — the grip + corner scale are drawn LAST (see the
                    // handles section below), so they always win the touch.
                }

                // AUTHOR / YEAR info rows — tap the row to select, grip only
                // when selected. The row's box spans its reported bounds.
                val m = rMeta
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
                    // v369 — the grip + corner scale are drawn LAST (see the
                    // handles section below), so they always win the touch.
                }

                // Badge — tap the chip to select, grip only when selected.
                // Anchored to the chip's REPORTED bounds (each style reports
                // its real category pill), so the grip sits on the chip
                // wherever it is — never a guessed corner.
                val b = rBadge
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
                    // v369 — the grip is drawn LAST (see the handles section
                    // below), so it always wins the touch.
                }

                // Book cover — tap the jacket to select, grip only when
                // selected. The jacket reports its own bounds (each style's
                // badge overlay), so the chrome sits on the real cover even
                // after it has been dragged around the card.
                val cv = rCover
                val cvOk = cv.width > 0f && cv.height > 0f
                if (cvOk) {
                    val isSel = sel == ShareCardResizeTarget.COVER
                    Box(
                        modifier = Modifier
                            .offset(cv.left.dp, cv.top.dp)
                            .width(cv.width.dp)
                            .height(cv.height.dp)
                            .clickable {
                                focusManager.clearFocus()
                                onSelectResizeTarget(ShareCardResizeTarget.COVER)
                            }
                            .border(1.dp, selBorder(isSel), RoundedCornerShape(4.dp))
                    )
                    // v369 — the grip is drawn LAST (see the handles section
                    // below), so it always wins the touch.
                }

                // v353 — the album favorite-tracks strip is a movable element
                // too: tap to select, then a single grip drags it around the
                // card (mirrors the cover/jacket block above).
                val rf = rFav
                val rfOk = rf.width > 0f && rf.height > 0f
                if (rfOk) {
                    val isSel = sel == ShareCardResizeTarget.FAVTRACKS
                    Box(
                        modifier = Modifier
                            .offset(rf.left.dp, rf.top.dp)
                            .width(rf.width.dp)
                            .height(rf.height.dp)
                            .clickable {
                                focusManager.clearFocus()
                                onSelectResizeTarget(ShareCardResizeTarget.FAVTRACKS)
                            }
                            .border(1.dp, selBorder(isSel), RoundedCornerShape(6.dp))
                    )
                    // v369 — the grip is drawn LAST (see the handles section
                    // below), so it always wins the touch.
                }

                // v3xx — the COLLAGE polaroid is a movable element too: tap
                // to select, then a single grip drags the print around the
                // card (mirrors the cover/jacket block above). Its scale +
                // style + filter live in the full-screen Polaroid panel.
                val rp = rPolaroid
                val rpOk = rp.width > 0f && rp.height > 0f
                if (rpOk) {
                    val isSel = sel == ShareCardResizeTarget.POLAROID
                    // Tap target = the print's own rect (no border on it).
                    Box(
                        modifier = Modifier
                            .offset(rp.left.dp, rp.top.dp)
                            .width(rp.width.dp)
                            .height(rp.height.dp)
                            .clickable {
                                focusManager.clearFocus()
                                onSelectResizeTarget(ShareCardResizeTarget.POLAROID)
                            }
                    )
                    // v3xx — the selection OUTLINE floats OUTSIDE the print
                    // (8dp pad): the old tight border drew straight across the
                    // tilted frame corners and the tape that peers past the
                    // top edge, which read as the print being "cut" by its own
                    // outline box.
                    if (isSel) {
                        Box(
                            modifier = Modifier
                                .offset((rp.left - 8).dp, (rp.top - 8).dp)
                                .width((rp.width + 16).dp)
                                .height((rp.height + 16).dp)
                                .border(1.dp, selBorder(true), RoundedCornerShape(6.dp))
                        )
                    }
                    // v369 — the grip is drawn LAST (see the handles section
                    // below), so it always wins the touch.
                }

                // ── v369 HANDLES (drawn LAST) ────────────────────────────
                // Every selected element's grip + corner scale render AFTER
                // all the selectable boxes, so a handle ALWAYS wins the touch
                // over a box it overlaps (the old inline placement let a
                // later-drawn selectable box steal the drag from an earlier
                // handle — the "handle doesn't work" bug).
                when (sel) {
                    ShareCardResizeTarget.TITLE -> if (!quoteMode) {
                        val t = rTitle
                        if (t.width > 0f && t.height > 0f) {
                            MoveHandle(
                                x = t.left.dp,
                                y = t.top.dp,
                                onDelta = { dx, dy ->
                                    // v340 — clamp against the UNMOVED rect
                                    // (base = reported minus current offset).
                                    // v353 — guide-only alignment (no magnet).
                                    // v376 — the measured rect already includes
                                    // the auto LIFT, so add it back to recover
                                    // the true natural top before clamping.
                                    val bx = t.left - move.titleDx
                                    val by = t.top - move.titleDy + move.titleLift
                                    val othersT = alignOthers(t)
                                    val xs = magnetAxis(bx, t.width, cw, -bx, cw - bx - t.width, (move.titleDx + dx).coerceIn(
                                        minOf(-bx, cw - bx - t.width),
                                        maxOf(-bx, cw - bx - t.width)
                                    ), snap = SNAP_REACH, hint = HINT_REACH, extra = hCands(othersT, bx, t.width))
                                    val ys = magnetAxis(by, t.height, ch, -by, ch - by - t.height, (move.titleDy + dy).coerceIn(
                                        minOf(-by, ch - by - t.height),
                                        maxOf(-by, ch - by - t.height)
                                    ), snap = SNAP_REACH, hint = HINT_REACH, extra = vCands(othersT, by, t.height))
                                    dragGuides = DragGuides(vx = xs.snapLine, hy = ys.snapLine, hintVx = xs.hintLine, hintHy = ys.hintLine)
                                    onMove(move.copy(titleDx = xs.offset, titleDy = ys.offset))
                                },
                                onDragStart = { dragActive = true },
                                onDragEnd = {
                                    // v380 — the drag owns the title's spot
                                    // (overlap freedom): fold any prior sparkle
                                    // lift into the position so the title
                                    // freezes EXACTLY where the finger left it.
                                    onMove(
                                        move.copy(
                                            titlePlaced = true,
                                            titleDy = move.titleDy - move.titleLift,
                                            titleLift = 0f
                                        )
                                    )
                                    dragActive = false
                                    dragGuides = DragGuides()
                                }
                            )
                        }
                    }
                    ShareCardResizeTarget.FACT -> if (!factEditMode) {
                        // v371 — while typing, the grip + corner scale hide so
                        // nothing fights the caret; they return when text
                        // editing ends (the field itself stays, border-less).
                        val f = rFact
                        if (f.width > 0f && f.height > 0f) {
                            MoveHandle(
                                x = f.left.dp,
                                y = f.top.dp,
                                onDelta = { dx, dy ->
                                    val bx = f.left - move.factDx
                                    val by = f.top - move.factDy
                                    val othersF = alignOthers(f)
                                    val xs = magnetAxis(bx, f.width, cw, -bx, cw - bx - f.width, (move.factDx + dx).coerceIn(
                                        minOf(-bx, cw - bx - f.width),
                                        maxOf(-bx, cw - bx - f.width)
                                    ), snap = SNAP_REACH, hint = HINT_REACH, extra = hCands(othersF, bx, f.width))
                                    val ys = magnetAxis(by, f.height, ch, -by, ch - by - f.height, (move.factDy + dy).coerceIn(
                                        minOf(-by, ch - by - f.height),
                                        maxOf(-by, ch - by - f.height)
                                    ), snap = SNAP_REACH, hint = HINT_REACH, extra = vCands(othersF, by, f.height))
                                    dragGuides = DragGuides(vx = xs.snapLine, hy = ys.snapLine, hintVx = xs.hintLine, hintHy = ys.hintLine)
                                    // v384 — the COLLISION-PUSH is gone: dragging
                                    // the fact box may freely overlap the title
                                    // and the info rows — manual placement wins.
                                    // The sparkle pill is the only repair now.
                                    onMove(move.copy(factDx = xs.offset, factDy = ys.offset))
                                },
                                onDragStart = {
                                    dragActive = true
                                    // v369 — seed the smart auto-fit nudge into
                                    // the manual move on the first grab so the
                                    // box doesn't jump when auto-fit hands off
                                    // (manual edits start where auto-fit left).
                                    val untouched = move.factDx == 0f && move.factDy == 0f &&
                                        move.factWidthFrac == 1f && move.factHeightFrac == 1f &&
                                        move.factScale == 1f
                                    if (untouched && (autoFitDelta.heightFrac != 1f || autoFitDelta.widthFrac != 1f ||
                                        autoFitDelta.textScale != 1f)
                                    ) {
                                        onMove(move.copy(
                                            factHeightFrac = autoFitDelta.heightFrac,
                                            factWidthFrac = (move.factWidthFrac * autoFitDelta.widthFrac).coerceIn(0.2f, 1f),
                                            // v371/v374 — carry the smart fit's text
                                            // shrink into the move so the text doesn't
                                            // pop back to full size (and clip) the
                                            // moment auto-fit hands over.
                                            factScale = move.factScale * autoFitDelta.textScale
                                        ))
                                    }
                                },
                                onDragEnd = { dragActive = false; dragGuides = DragGuides() }
                            )
                            // v378 — the CORNER whole-box scale grip is GONE:
                            // the corner icon duplicated the Crop tool's
                            // width/height sliders and its outline read as a
                            // box glitch in full screen. Resizing happens via
                            // the sliders only.
                        }
                    }
                    ShareCardResizeTarget.META -> {
                        val m = rMeta
                        if (m.width > 0f && m.height > 0f) {
                            MoveHandle(
                                // v360 — the grip parks just past the row's
                                // right edge (same as every other handle).
                                x = (m.right + 18f).coerceAtMost(cw - 16f).dp,
                                y = m.bottom.dp.coerceAtMost((ch - 16f).dp),
                                onDelta = { dx, dy ->
                                    val bx = m.left - move.metaDx
                                    val by = m.top - move.metaDy
                                    val othersM = alignOthers(m)
                                    val xs = magnetAxis(bx, m.width, cw, -bx, cw - bx - m.width, (move.metaDx + dx).coerceIn(-bx, cw - bx - m.width), snap = SNAP_REACH, hint = HINT_REACH, extra = hCands(othersM, bx, m.width))
                                    val ys = magnetAxis(by, m.height, ch, -by, ch - by - m.height, (move.metaDy + dy).coerceIn(-by, ch - by - m.height), snap = SNAP_REACH, hint = HINT_REACH, extra = vCands(othersM, by, m.height))
                                    dragGuides = DragGuides(vx = xs.snapLine, hy = ys.snapLine, hintVx = xs.hintLine, hintHy = ys.hintLine)
                                    onMove(move.copy(metaDx = xs.offset, metaDy = ys.offset))
                                },
                                onDragStart = { dragActive = true },
                                onDragEnd = { dragActive = false; dragGuides = DragGuides() }
                            )
                            // v378 — corner scale grip removed (see the fact
                            // branch): the Crop tool's sliders own sizing.
                        }
                    }
                    ShareCardResizeTarget.BADGE -> {
                        val b = rBadge
                        if (b.width > 0f && b.height > 0f) {
                            MoveHandle(
                                x = (b.left - 2f).coerceIn(0f, (cw - 24f).coerceAtLeast(0f)).dp,
                                y = (b.top - 2f).coerceIn(0f, (ch - 24f).coerceAtLeast(0f)).dp,
                                onDelta = { dx, dy ->
                                    val bx = b.left - move.badgeDx
                                    val by = b.top - move.badgeDy
                                    val othersB = alignOthers(b)
                                    val xs = magnetAxis(bx, b.width, cw, -bx - 2f, cw - bx - b.width + 2f, (move.badgeDx + dx).coerceIn(-bx - 2f, cw - bx - b.width + 2f), snap = SNAP_REACH, hint = HINT_REACH, extra = hCands(othersB, bx, b.width))
                                    val ys = magnetAxis(by, b.height, ch, -by - 2f, ch - by - b.height + 2f, (move.badgeDy + dy).coerceIn(-by - 2f, ch - by - b.height + 2f), snap = SNAP_REACH, hint = HINT_REACH, extra = vCands(othersB, by, b.height))
                                    dragGuides = DragGuides(vx = xs.snapLine, hy = ys.snapLine, hintVx = xs.hintLine, hintHy = ys.hintLine)
                                    onMove(move.copy(badgeDx = xs.offset, badgeDy = ys.offset))
                                },
                                onDragStart = { dragActive = true },
                                onDragEnd = { dragActive = false; dragGuides = DragGuides() }
                            )
                        }
                    }
                    ShareCardResizeTarget.COVER -> {
                        val cv = rCover
                        if (cv.width > 0f && cv.height > 0f) {
                            MoveHandle(
                                x = (cv.right - 24f).coerceIn(0f, (cw - 24f).coerceAtLeast(0f)).dp,
                                y = (cv.bottom - 24f).coerceIn(0f, (ch - 24f).coerceAtLeast(0f)).dp,
                                onDelta = { dx, dy ->
                                    val bx = cv.left - move.coverDx
                                    val by = cv.top - move.coverDy
                                    val othersC = alignOthers(cv)
                                    val xs = magnetAxis(bx, cv.width, cw, -bx, cw - bx - cv.width, (move.coverDx + dx).coerceIn(-bx, cw - bx - cv.width), snap = SNAP_REACH, hint = HINT_REACH, extra = hCands(othersC, bx, cv.width))
                                    val ys = magnetAxis(by, cv.height, ch, -by, ch - by - cv.height, (move.coverDy + dy).coerceIn(-by, ch - by - cv.height), snap = SNAP_REACH, hint = HINT_REACH, extra = vCands(othersC, by, cv.height))
                                    dragGuides = DragGuides(vx = xs.snapLine, hy = ys.snapLine, hintVx = xs.hintLine, hintHy = ys.hintLine)
                                    onMove(move.copy(coverDx = xs.offset, coverDy = ys.offset))
                                },
                                onDragStart = { dragActive = true },
                                onDragEnd = { dragActive = false; dragGuides = DragGuides() }
                            )
                        }
                    }
                    ShareCardResizeTarget.FAVTRACKS -> {
                        val rf = rFav
                        if (rf.width > 0f && rf.height > 0f) {
                            MoveHandle(
                                x = (rf.right - 24f).coerceIn(0f, (cw - 24f).coerceAtLeast(0f)).dp,
                                y = (rf.bottom - 24f).coerceIn(0f, (ch - 24f).coerceAtLeast(0f)).dp,
                                onDelta = { dx, dy ->
                                    val bx = rf.left - move.favDx
                                    val by = rf.top - move.favDy
                                    val othersF = alignOthers(rf)
                                    val xs = magnetAxis(bx, rf.width, cw, -bx, cw - bx - rf.width, (move.favDx + dx).coerceIn(-bx, cw - bx - rf.width), snap = SNAP_REACH, hint = HINT_REACH, extra = hCands(othersF, bx, rf.width))
                                    val ys = magnetAxis(by, rf.height, ch, -by, ch - by - rf.height, (move.favDy + dy).coerceIn(-by, ch - by - rf.height), snap = SNAP_REACH, hint = HINT_REACH, extra = vCands(othersF, by, rf.height))
                                    dragGuides = DragGuides(vx = xs.snapLine, hy = ys.snapLine, hintVx = xs.hintLine, hintHy = ys.hintLine)
                                    onMove(move.copy(favDx = xs.offset, favDy = ys.offset))
                                },
                                onDragStart = { dragActive = true },
                                onDragEnd = { dragActive = false; dragGuides = DragGuides() }
                            )
                            // v378 — corner scale grip removed (see the fact
                            // branch): the Crop tool's sliders own sizing.
                        }
                    }
                    ShareCardResizeTarget.POLAROID -> {
                        val rp = rPolaroid
                        if (rp.width > 0f && rp.height > 0f) {
                            MoveHandle(
                                x = (rp.right - 24f).coerceIn(0f, (cw - 24f).coerceAtLeast(0f)).dp,
                                y = (rp.bottom - 24f).coerceIn(0f, (ch - 24f).coerceAtLeast(0f)).dp,
                                onDelta = { dx, dy ->
                                    val bx = rp.left - move.polaroidDx
                                    val by = rp.top - move.polaroidDy
                                    val othersP = alignOthers(rp)
                                    val xs = magnetAxis(bx, rp.width, cw, -bx, cw - bx - rp.width, (move.polaroidDx + dx).coerceIn(-bx, cw - bx - rp.width), snap = SNAP_REACH, hint = HINT_REACH, extra = hCands(othersP, bx, rp.width))
                                    val ys = magnetAxis(by, rp.height, ch, -by, ch - by - rp.height, (move.polaroidDy + dy).coerceIn(-by, ch - by - rp.height), snap = SNAP_REACH, hint = HINT_REACH, extra = vCands(othersP, by, rp.height))
                                    dragGuides = DragGuides(vx = xs.snapLine, hy = ys.snapLine, hintVx = xs.hintLine, hintHy = ys.hintLine)
                                    onMove(move.copy(polaroidDx = xs.offset, polaroidDy = ys.offset))
                                },
                                onDragStart = { dragActive = true },
                                onDragEnd = { dragActive = false; dragGuides = DragGuides() }
                            )
                        }
                    }
                    ShareCardResizeTarget.NONE -> {}
                }

                // v341/v342 — PicsArt-style alignment guides. Always composed
                // but only draws while a box drag is live: a FAINT centre
                // crosshair appears the moment any grip is pulled (centering
                // hint); near an alignment line the box first shows a faint
                // full-card HINT (same faint line — v353 the bright snapped
                // guides are gone with the magnet). Reads happen inside the draw scope
                // so per-frame drag updates only redraw this canvas, never
                // recompose the overlay. Halo + bright core keeps the lines
                // visible on light and dark cards alike.
                Canvas(Modifier.fillMaxSize()) {
                    if (dragActive || dragGuides.vx != null || dragGuides.hy != null ||
                        dragGuides.hintVx != null || dragGuides.hintHy != null
                    ) {
                        val gd = editDensity
                        // darkAlpha / lightAlpha / lightW let HINTS draw thin
                        // and faint while SNAPPED guides keep the bright halo.
                        fun guide(vertical: Boolean, at: Float, darkAlpha: Float, lightAlpha: Float, lightW: Float) {
                            val px = if (vertical) with(gd) { at.dp.toPx() } else 0f
                            val py = if (vertical) 0f else with(gd) { at.dp.toPx() }
                            val ex = if (vertical) px else size.width
                            val ey = if (vertical) size.height else py
                            drawLine(Color.Black.copy(alpha = darkAlpha), Offset(px, py), Offset(ex, ey), strokeWidth = with(gd) { 3.dp.toPx() })
                            drawLine(Color.White.copy(alpha = lightAlpha), Offset(px, py), Offset(ex, ey), strokeWidth = with(gd) { lightW.dp.toPx() })
                        }
                        if (dragActive) {
                            // Centering hint: faint crosshair while any box moves.
                            val cx = with(gd) { (cw / 2f).dp.toPx() }
                            val cy = with(gd) { (ch / 2f).dp.toPx() }
                            drawLine(Color.White.copy(alpha = 0.28f), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = with(gd) { 0.7.dp.toPx() })
                            drawLine(Color.White.copy(alpha = 0.28f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = with(gd) { 0.7.dp.toPx() })
                        }
                        // Faint alignment hints first (near a line but not
                        // snapped), then the bright snapped guides on top.
                        dragGuides.hintVx?.let { guide(true, it, 0.15f, 0.32f, 0.6f) }
                        dragGuides.hintHy?.let { guide(false, it, 0.15f, 0.32f, 0.6f) }
                        dragGuides.vx?.let { guide(true, it, 0.30f, 0.95f, 1f) }
                        dragGuides.hy?.let { guide(false, it, 0.30f, 0.95f, 1f) }
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

/** v340 — TEXT-size slider: a continuous 0.5×–2.0× range in 1% steps with a
 *  haptic tick per step and a live × readout, replacing the old fixed-step
 *  buttons (0.5/0.7/0.85/1.0… jumped straight from too-small to cut-off for
 *  long titles/facts). The value is rounded to the nearest 0.01 so the card
 *  never renders a visually-aliased in-between size, and every tick lands
 *  with haptic feedback exactly like the box-size sliders. */
@Composable
private fun TextSizeSliderColumn(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val range = 0.5f..2.0f
    // 1% = 0.01 over the 1.5 span → 150 snap steps.
    val stepsTotal = 150
    fun stepIndex(v: Float): Int =
        (((v - range.start) / (range.endInclusive - range.start)) * stepsTotal)
            .roundToInt().coerceIn(0, stepsTotal)
    fun stepValue(v: Float): Float =
        (range.start + (range.endInclusive - range.start) * stepIndex(v) / stepsTotal)
            .let { kotlin.math.round(it * 100f) / 100f }
    var lastStep by remember { mutableIntStateOf(stepIndex(value)) }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${stepValue(value)}×", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = { v ->
                val snapped = stepValue(v)
                onValueChange(snapped)
                val s = stepIndex(v)
                if (s != lastStep) {
                    lastStep = s
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onValueChangeFinished = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            },
            valueRange = range,
            steps = stepsTotal - 1,
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
    // v334 — BOOK share cards: the authored cover + fetched Google Books
    // rating, so the card can show the cover (fetch / override in the editor)
    // and the real ★ rating without a second lookup.
    bookImageUrl: String = "",
    bookRating: Double? = null,
    bookRatingCount: Int = 0,
    // v370b — ALBUM / SERIES share cards: same cover flow as books — the
    // editor's Content panel offers Fetch (keyless iTunes→MusicBrainz art
    // for albums, TVMaze→iTunes posters for series), Refetch and Remove,
    // and the cover renders LEFT of the title like the synopsis page.
    isAlbumTopic: Boolean = false,
    isSeriesTopic: Boolean = false,
    // v229d — the sheet can open PRESELECTED: the Share Hub picks a design on
    // the grid and hands its style index + classic-signature flag here, so the
    // sheet opens on exactly the design the user picked (the reveal + detail
    // screens keep the defaults: first style, current signature).
    initialStyle: Int = 0,
    initialClassicSignature: Boolean = false,
    // v371 — the sheet can open PRE-SEEDED from the Book Notes sheet's
    // "Share as review" action: the caller hands the review text (the saved
    // chapter note) and the chapter number it belongs to. These WIN over any
    // restored edits (the restore effect below applies them first and the
    // seeds override): the share card opens on the Chapter review content
    // with the note text + its chapter chip already set, so the user only
    // picks the design and shares.
    seedReviewText: String = "",
    seedReviewChapter: Int = 0,
    // v375 — the chapter note's rich-text spans ride with [seedReviewText]
    // so bold/italic/highlight written in the note editor survive onto the
    // share card's Chapter review (and the export).
    seedReviewSpans: List<TextSpan> = emptyList(),
    // v229d — "Share as text" lives IN the shared sheet (both reveal + detail
    // get it). When set, the caller supplies its own payload (the detail view
    // sends the entry's decorated text); otherwise the sheet builds a default
    // topic + fact payload from its own params.
    shareAsText: (() -> String)? = null,
    // v383 — LINK share: the caller supplies the URL a Link share posts (see
    // [com.curio.app.data.shareLinkForTopic]: music topics → the music
    // service the user picked in Settings; everything else → Google search
    // of the topic). Evaluated at share time so the current service wins.
    // When null the sheet falls back to a plain Google search of the topic
    // name. The Link share opens a small caption editor: the user types the
    // message words and the link rides at the end — receiving apps show the
    // URL as the tap-to-open link (the OS can't hide a URL behind custom
    // text; the caption is what the sender controls).
    shareLinkUrl: (() -> String)? = null
) {
    // Per-share state — plain remember (not Bundle-saveable): the modal
    // resets these each time it opens, and enums/ImageBitmap aren't Bundle-
    // saveable by default (crash on onSaveInstanceState).
    // v3xx — 3:4 (CLASSIC) is the default aspect; the aspect tool toggles.
    var aspect by remember { mutableStateOf(ShareCardAspect.CLASSIC) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    // v335 — Reading progress can stay on while the user picks a Custom
    // fact: the progress bar keeps rendering with the fact stacked below it.
    var showChapterProgress by remember { mutableStateOf(false) }
    var customText by rememberSaveable { mutableStateOf("") }
    var polaroidCaption by rememberSaveable { mutableStateOf("") }
    // v342 — which chapter the chapter-review chip tags. Declared with the
    // other sheet state so the restore effect below can seed it (the review
    // text lives in [customText] and persists the same way).
    var reviewChapterNumber by remember { mutableIntStateOf(0) }
    // v369 — optional chapter-title override for the review chip ("CH 5 · My
    // Title"): blank keeps the book's authored chapter title. Edited next to
    // the chapter picker in the Content panel; the chip itself stays above
    // the review text at the fact-box position.
    var reviewChapterTitle by remember { mutableStateOf("") }
    // v229d — seeded from [initialStyle] / [initialClassicSignature] so the
    // Share Hub can open the sheet on the picked design.
    var styleIdx by remember { mutableIntStateOf(initialStyle) }
    var classicDesign by remember { mutableStateOf(initialClassicSignature) }
    // v383 — LINK share: the caption editor dialog + the user's message.
    // rememberSaveable so the typed words survive a rotation while the sheet
    // is open (same convention as [customText]).
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkCaption by rememberSaveable { mutableStateOf("") }
    // v384 — the link option moved INTO the default Share dialog: a switch
    // (on = post your words + the topic's link as text; off = just share the
    // picture, optionally with the caption attached).
    var includeLink by remember { mutableStateOf(false) }
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
    // v371 — the FULL-SCREEN text editor: the card renders large on the
    // category-tint wash with floating tools (one Aa pill + dropdown).
    var fullscreenEdit by remember { mutableStateOf(false) }
    // v370b — the ENLARGE writing sheet: a full white writing surface for
    // the active fact text (quick / custom / chapter review), opened from
    // the writing box below the tools.
    var showWriteSheet by remember { mutableStateOf(false) }
    var bodyScale by remember { mutableStateOf(1f) }
    // v330 — every STYLE keeps its OWN move/position edits: dragging the
    // title on Paper must not shove the title on Vinyl too. Keyed by style;
    // missing entries fall back to a clean ShareCardMove.
    var movesByStyle by remember { mutableStateOf<Map<ShareCardStyle, ShareCardMove>>(emptyMap()) }
    var editedTitle by remember { mutableStateOf<String?>(null) }
    var editedFact by remember { mutableStateOf<String?>(null) }
    // v375 — rich-text spans (bold/italic/highlight runs) parallel to the
    // TEXT states above: editedFactSpans styles an edited QUICK fact,
    // customSpans styles the custom fact / chapter review. The card renders
    // them through every style + the export (FactBody), empty = plain text.
    var editedFactSpans by remember { mutableStateOf<List<TextSpan>>(emptyList()) }
    var customSpans by remember { mutableStateOf<List<TextSpan>>(emptyList()) }
    // v3xx — EMOJI STICKERS (full-screen editor only): each is a fraction-
    // positioned emoji on top of the card. Persisted with the other edits so
    // a reopened sheet / the export keep them exactly where they were left.
    var stickers by remember { mutableStateOf<List<ShareSticker>>(emptyList()) }
    // v3xx — the full-screen editor's sticker TOOL: open = the top-bar
    // Stickers panel is showing and the card's sticker layer is interactive
    // (tap to select, drag to move, resize / z-order / delete in the panel).
    var stickerToolsOpen by remember { mutableStateOf(false) }
    // v3xx — which sticker the full-screen editor has selected (index into
    // [stickers]; -1 = none).
    var selectedSticker by remember { mutableIntStateOf(-1) }
    // v3xx — the full-screen editor's POLAROID tool (Collage card only):
    // the panel picks the print's style / photo filter / size; the grip on
    // the card moves it.
    var polaroidToolsOpen by remember { mutableStateOf(false) }
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
                coverDx = o.optDouble("coverDx", 0.0).toFloat(),
                coverDy = o.optDouble("coverDy", 0.0).toFloat(),
                favDx = o.optDouble("favDx", 0.0).toFloat(),
                favDy = o.optDouble("favDy", 0.0).toFloat(),
                favCount = o.optInt("favCount", 0),
                polaroidDx = o.optDouble("polaroidDx", 0.0).toFloat(),
                polaroidDy = o.optDouble("polaroidDy", 0.0).toFloat(),
                polaroidScale = o.optDouble("polaroidScale", 1.0).toFloat(),
                polaroidStyle = o.optInt("polaroidStyle", 0),
                polaroidFilter = o.optInt("polaroidFilter", 0),
                titleWidthFrac = o.optDouble("titleWidthFrac", 1.0).toFloat(),
                titleHeightFrac = o.optDouble("titleHeightFrac", 1.0).toFloat(),
                factWidthFrac = o.optDouble("factWidthFrac", 1.0).toFloat(),
                factHeightFrac = o.optDouble("factHeightFrac", 1.0).toFloat(),
                metaWidthFrac = o.optDouble("metaWidthFrac", 1.0).toFloat(),
                metaHeightFrac = o.optDouble("metaHeightFrac", 1.0).toFloat(),
                titleScale = o.optDouble("titleScale", 1.0).toFloat(),
                favWidthFrac = o.optDouble("favWidthFrac", 1.0).toFloat(),
                favHeightFrac = o.optDouble("favHeightFrac", 1.0).toFloat(),
                titleBoxScale = o.optDouble("titleBoxScale", 1.0).toFloat(),
                factBoxScale = o.optDouble("factBoxScale", 1.0).toFloat(),
                favBoxScale = o.optDouble("favBoxScale", 1.0).toFloat(),
                // v379d — the legacy corner-zoom `factZoom` folds into
                // [factScale] on load (both multiply the same channel; the
                // field is gone from this version). Old saves keep their
                // effective size, new saves write factScale only.
                factScale = (o.optDouble("factScale", 1.0) * o.optDouble("factZoom", 1.0)).toFloat(),
                factGutter = o.optDouble("factGutter", 1.0).toFloat(),
                titleLift = o.optDouble("titleLift", 0.0).toFloat(),
                titlePlaced = o.optBoolean("titlePlaced", false),
                factUnderline = o.optBoolean("factUnderline", false),
                factHighlight = o.optInt("factHighlight", 0).takeIf { it != 0 }?.let { Color(it) },
                titleUnderline = o.optBoolean("titleUnderline", false),
                titleHighlight = o.optInt("titleHighlight", 0).takeIf { it != 0 }?.let { Color(it) },
                polaroidOnCard = o.optBoolean("polaroidOnCard", false),
                // v370 — the fact LAYOUT (condensed / book page / editorial
                // + drop cap) restores by name; unknown names keep the
                // style's default.
                factFormat = ShareCardFactFormat.entries.firstOrNull {
                    it.name == o.optString("factFormat", "")
                } ?: ShareCardFactFormat.STANDARD,
                factDropCap = ShareCardFactDropCap.entries.firstOrNull {
                    it.name == o.optString("factDropCap", "")
                } ?: ShareCardFactDropCap.NONE
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
            editedFactSpans = spansFromJson(saved.optJSONArray("editedFactSpans"))
            if (editedFact == null || editedFact.isNullOrBlank()) editedFactSpans = emptyList()
            // v342 — restore the picked CONTENT and its live text too (custom
            // fact, chapter review + its chip, the reading-progress toggle,
            // collage caption), so Save/Share keeps them exactly like the
            // size/position edits instead of resetting to the quick fact.
            // The restored id is re-validated against what this topic offers
            // at render time (see the activeId derivation below), so a stale
            // save can never render a phantom content.
            saved.optString("selectedId", null)?.takeIf { it.isNotBlank() }
                ?.let { selectedId = it }
            customText = saved.optString("customText", "")
            customSpans = spansFromJson(saved.optJSONArray("customSpans"))
            if (customText.isBlank()) customSpans = emptyList()
            polaroidCaption = saved.optString("polaroidCaption", "")
            showChapterProgress = saved.optBoolean("showChapterProgress", false)
            reviewChapterNumber = saved.optInt("reviewChapterNumber", 0)
            reviewChapterTitle = saved.optString("reviewChapterTitle", "")
            // v3xx — the emoji stickers restore with the other edits.
            stickers = saved.optJSONArray("stickers")?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    ShareSticker(
                        emoji = o.optString("emoji"),
                        x = o.optDouble("x", 0.5).toFloat(),
                        y = o.optDouble("y", 0.5).toFloat(),
                        sizeFrac = o.optDouble("sizeFrac", 0.22).toFloat(),
                        imagePath = o.optString("imagePath", null)?.takeIf { it.isNotBlank() },
                        rotation = o.optDouble("rotation", 0.0).toFloat()
                    )
                }
            } ?: emptyList()
        }
        // v371 — the Book Notes "Share as review" seed WINS over the saved
        // edits: the note text becomes the chapter-review text, the review
        // content is selected, and the chapter chip tags the note's chapter.
        if (seedReviewText.isNotBlank()) {
            customText = seedReviewText
            customSpans = seedReviewSpans
            selectedId = "chapter_review"
            showChapterProgress = false
            if (seedReviewChapter > 0) reviewChapterNumber = seedReviewChapter
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
    // v334 — BOOK cover on the share card, overridable from the gallery (or
    // removed). v335 — the cover is NEVER visible by default: the card starts
    // clean and the jacket only appears after the user taps "Fetch" in the
    // editor's Content panel (so a share card never carries a half-loaded /
    // wrong cover by surprise). The editor's Content panel offers the fetch,
    // a gallery pick and Remove.
    var bookCover by remember { mutableStateOf<ImageBitmap?>(null) }
    var coverLoadFailed by remember { mutableStateOf(false) }
    // v334 — attempt counter: 0 uses the topic's authored cover; every
    // manual fetch/refetch skips it and hits the keyless providers (Open
    // Library / Google Books) so the user can pull a different cover.
    var coverAttempt by remember { mutableIntStateOf(0) }
    // v335 — covers load ONLY when the user asks: this flips true on the
    // first Fetch tap and stays true, so Refetch after that keeps working.
    var coverFetchRequested by remember { mutableStateOf(false) }
    val isBookTopic = bookChapters.isNotEmpty() || bookImageUrl.isNotBlank()
    androidx.compose.runtime.LaunchedEffect(coverFetchRequested, coverAttempt, coverLoadFailed) {
        if (!(isBookTopic || isAlbumTopic || isSeriesTopic) || !coverFetchRequested || bookCover != null || coverLoadFailed) return@LaunchedEffect
        // v370b — resolve the cover URL per category: books use the authored
        // cover first then the keyless provider cascade; albums and series
        // use their own keyless resolvers (iTunes→MusicBrainz / TVMaze→
        // iTunes). The bitmap load below is shared.
        val url = when {
            // v371 — books: authored cover first, then iTunes search
            // (title+author — the same keyless source the Settings hub uses),
            // then the Open Library title cover as the last resort. Google
            // Books was removed as a cover source.
            isBookTopic -> if (coverAttempt == 0) bookImageUrl.takeIf { it.isNotBlank() }
                else com.curio.app.features.settings.BookCoverFetch.resolveCoverUrl(
                    context, topicName, topicByline, "",
                    com.curio.app.features.settings.BookCoverFetch.BookCoverProvider.ITUNES
                ) ?: com.curio.app.features.settings.BookCoverFetch.coverCandidates(topicName, "").firstOrNull()
            isAlbumTopic -> com.curio.app.features.reveal.AlbumArtFetch.resolveArtworkUrl(topicName, topicByline)
            isSeriesTopic -> com.curio.app.features.reveal.SeriesPosterFetch.resolvePosterUrl(topicName)
            else -> null
        }
        if (url.isNullOrBlank()) { coverLoadFailed = true; return@LaunchedEffect }
        val bmp = runCatching {
            suspendCancellableCoroutine<ImageBitmap?> { cont ->
                val loader = context.imageLoader
                val request = coil.request.ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(false)
                    // Export crash fix (same recipe as MoodBoardExport): the
                    // share sheet's Save/Share rasterizes the card through a
                    // SOFTWARE canvas (shareComposableCard → composeView.draw),
                    // and Coil decodes HARDWARE bitmaps by default on API 26+.
                    // Drawing a hardware bitmap into that software canvas
                    // throws "Software rendering doesn't support hardware
                    // bitmaps" (IllegalArgumentException) the moment a fetched
                    // cover is on the card. Decode software so both the
                    // on-screen preview and the PNG export paths can draw it.
                    // Memory cache DISABLED so a prior hardware decode of the
                    // same URL (e.g. the reveal poster) can never be handed
                    // back from the cache as this software request's result.
                    .allowHardware(false)
                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                    .listener(
                        onSuccess = { _, result ->
                            cont.resume(result.drawable.toBitmap().asImageBitmap())
                        },
                        onError = { _, _ -> cont.resume(null) }
                    )
                    .build()
                val disp = loader.enqueue(request)
                cont.invokeOnCancellation { disp.dispose() }
            }
        }.getOrNull()
        if (bmp != null) bookCover = bmp else coverLoadFailed = true
    }
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(it)
                )
                bitmap?.let { bmp ->
                    val size = minOf(bmp.width, bmp.height)
                    val x = (bmp.width - size) / 2
                    val y = (bmp.height - size) / 2
                    val cropped = android.graphics.Bitmap.createBitmap(bmp, x, y, size, size)
                    bookCover = cropped.asImageBitmap()
                    coverLoadFailed = false
                }
            } catch (_: Exception) { }
        }
    }
    // v3xx — IMPORTED STICKER cutouts: the picked PNG (transparent cutout)
    // is copied into the app's stickers dir and added as an image sticker,
    // so the emoji picker is no longer the only sticker source.
    val stickerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = importStickerPng(context, it)
            if (path != null) {
                stickers = stickers + ShareSticker(emoji = "", imagePath = path, x = 0.30f, y = 0.28f)
                selectedSticker = stickers.lastIndex
            }
        }
    }

    val isQuotes = categoryName == "Quotes"
    val quoteText = if (isQuotes) topicName else quickFact
    // v334 — BOOK topics carry the fetched Google Books rating on the quick
    // fact content, so the star row shows on the card (the reveal already
    // fetched it; the sheet never re-fetches).
    val quick = ShareCardContent(
        QUICK_FACT_ID, "Quick fact", quickFact,
        rating = if (isBookTopic) bookRating?.roundToInt()?.takeIf { it > 0 } else null
    )
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
    // v342 — [reviewChapterNumber] now lives with the other sheet state at
    // the top of this function so the saved-edit restore can seed it; here it
    // only feeds the review chip tag (defaults to the current reading spot,
    // or chapter 1).
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
    // v342 — a restored [selectedId] is only honoured when it names one of
    // this topic's current contents (the same guard that keeps a stale save
    // from rendering a phantom pick); anything else falls back to the default.
    val activeId = if (selectedId != null && available.any { it.id == selectedId }) selectedId!! else defaultId
    // v375 — the chapter-review chip line, hoisted OUT of the per-style
    // build so its LENGTH is known here: the CARD prepends the chip to the
    // review text, so the note's spans must shift past it before rendering
    // (see [cardFactRenderSpans] below). Blank when the topic has no
    // chapters / nothing read — the text then starts at the review itself.
    val reviewChipText: String = effectiveReviewChapter?.let { num ->
        val ch = bookChapters.firstOrNull { c -> c.number == num }
        // v369 — the user's chapter-title override wins; otherwise the
        // book's authored title. The chip stays at the fact-box position
        // above the review text.
        val titlePart = reviewChapterTitle.ifBlank {
            ch?.title?.takeIf { t -> t.isNotBlank() } ?: ""
        }
        "CH $num" + (titlePart.takeIf { t -> t.isNotBlank() }?.let { t -> " · $t" } ?: "")
    } ?: ""
    // The chip renders as its own first line, so the prefix is chip + \n.
    val reviewChipPrefixLen = if (reviewChipText.isNotBlank()) reviewChipText.length + 1 else 0
    // v334 — the card's fact text is ONE source of truth per content type:
    // custom facts + chapter reviews always render the LIVE customText (empty
    // stays empty — no placeholder text bleeding onto the card), the default
    // quick fact renders the per-share inline edit, and everything else its
    // authored text.
    val activeSource = when (activeId) {
        CUSTOM_FACT_ID -> custom.copy(text = customText)
        NO_FACT_ID -> noFact
        "chapter_progress" -> progressContent
        "chapter_review" -> {
            reviewContent.copy(
                text = buildString {
                    if (reviewChipText.isNotBlank()) append(reviewChipText).append("\n")
                    append(customText.ifBlank { "Write your review of this chapter…" })
                }
            )
        }
        else -> available.firstOrNull { it.id == activeId } ?: quick
    }
    // v329 — when Reading progress is the active content the card draws a
    // VISUAL chapter widget (bar + caption) instead of the prose text; the
    // value comes from the live BookNotes pref so the picker updates the card.
    // v335 — the widget also renders while a Custom fact stacks under it
    // (picking Reading progress sets showChapterProgress; picking the Custom
    // fact keeps it on so the two combine on the card).
    val progressForCard = if (showChapterProgress && hasBookChapters)
        ChapterProgressUi(chaptersRead, bookChapters.size)
    else null
    // v335 — the prose stacked BELOW the progress bar. Reading progress
    // itself carries its own caption inside the widget.
    // v374 — the QUICK fact stacks under the bar exactly like the custom
    // fact: with Reading progress on, picking the Quick fact (or the
    // default) shows the progress widget with the quick-fact text beneath it.
    val chapterFactForCard = if (progressForCard != null) when (activeId) {
        CUSTOM_FACT_ID -> customText
        QUICK_FACT_ID -> editedFact ?: quick.text
        else -> ""
    } else ""
    // v334 — what the CARD renders (unified for every TopicShareCard call):
    // custom facts + chapter reviews always render the LIVE customText (empty
    // stays empty — no placeholder bleeding onto the card); everything else
    // renders the per-share inline edit over the authored text.
    val cardFactText = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review")
        activeSource.text else editedFact ?: activeSource.text
    // v334 — what the inline field binds to (mirror of [cardFactText] so the
    // caret sits on the visible glyphs) and how its edits are routed: custom
    // facts and chapter reviews write customText, everything else editedFact.
    // v369 — the chapter-review field binds the REVIEW text ONLY (the chip
    // is the prefix the CARD renders); the field shifts down one line in
    // ArrangeableCard so the caret lands on the review glyphs, not the chip.
    val factFieldText = when (activeId) {
        CUSTOM_FACT_ID, "chapter_review" -> customText
        else -> editedFact ?: activeSource.text
    }
    // v375 — the CARD's span source mirrors [cardFactText]: custom facts +
    // chapter reviews carry customSpans, everything else editedFactSpans.
    // These offsets are relative to the FIELD text (customText / the inline
    // edit) — what the transparent field + the full-screen selection bar
    // work with.
    val cardFactSpans = when (activeId) {
        CUSTOM_FACT_ID, "chapter_review" -> customSpans
        else -> editedFactSpans
    }
    // v375 — the spans as the CARD must render them: for a chapter review
    // the style's FactBody draws the full text (chip line + note), so the
    // runs shift past the chip prefix; every other content renders its text
    // verbatim, so the offsets are already right.
    val cardFactRenderSpans = when (activeId) {
        "chapter_review" -> shiftSpans(cardFactSpans, reviewChipPrefixLen)
        else -> cardFactSpans
    }
    // v375 — what the ENLARGE editor binds (mirror of [factFieldText]).
    val factFieldSpans = when (activeId) {
        CUSTOM_FACT_ID, "chapter_review" -> customSpans
        else -> editedFactSpans
    }
    // v375 — plain inline typing rebases the existing spans across the edit
    // (the same common-prefix/suffix logic as Save your take's editor) so
    // formatting typed around survives; span-free text stays span-free.
    fun routeFactChange(newText: String) {
        when (activeId) {
            CUSTOM_FACT_ID, "chapter_review" -> {
                customSpans = if (customSpans.isEmpty()) emptyList()
                else rebaseSpans(customText, newText, customSpans)
                customText = newText
            }
            else -> {
                editedFactSpans = if (editedFactSpans.isEmpty()) emptyList()
                else rebaseSpans(editedFact ?: activeSource.text, newText, editedFactSpans)
                editedFact = newText
            }
        }
    }
    // v3xx — TEXT HISTORY (see TextHistory.kt): a GLOBAL, persistent feed of
    // every text snapshot. Captures fire when typing pauses, at every 10th
    // word, and when the editor leaves the screen; the feed lives outside the
    // card edits so Reset Layout / a new topic never clears it. The pill opens
    // the same browser from the sheet tools corner, the full-screen editor's
    // top bar and the Enlarge writing sheet — Restore writes the picked
    // snapshot back into the ACTIVE field via [routeFactChange].
    val historyField = when (activeId) {
        CUSTOM_FACT_ID -> "Custom fact"
        "chapter_review" -> "Chapter review"
        "chapter_progress" -> "Reading progress"
        "quote" -> "Quote"
        else -> "Quick fact"
    }
    var historyOpen by remember { mutableStateOf(false) }
    rememberTextHistoryCapture(context, historyField, factFieldText, "$topicName\u00b7$activeId")
    rememberTextHistoryCapture(context, "Photo caption", polaroidCaption, topicName)
    // v3xx — the TITLE joins the feed too, once it's actually edited: the
    // blank/unedited value is skipped by the store, so an untouched card
    // never spams "Title" snapshots.
    rememberTextHistoryCapture(context, "Title", editedTitle ?: "", "$topicName\u00b7title")
    if (historyOpen) {
        TextHistoryBrowser(
            ctx = context,
            activeField = historyField,
            onRestore = { routeFactChange(it) },
            onDismiss = { historyOpen = false }
        )
    }
    // v375 — RICH edits (the Enlarge editor / selection bar) carry the text
    // AND its spans together, so formatting lands on the card immediately.
    fun routeRichFact(newText: String, spans: List<TextSpan>) {
        when (activeId) {
            CUSTOM_FACT_ID, "chapter_review" -> {
                customText = newText
                customSpans = if (newText.isBlank()) emptyList() else spans
            }
            else -> {
                editedFact = newText
                editedFactSpans = if (newText.isBlank()) emptyList() else spans
            }
        }
    }
    // v375 — the FULL-SCREEN selection bar calls this: it toggles [flag]
    // over exactly [s, e) of the ACTIVE fact's span list (custom fact /
    // chapter review → customSpans, everything else → editedFactSpans), so
    // bold / italic / highlight apply ONLY to the selected letters — never
    // the whole element. Reads the live state each call (the lambda is built
    // once per composition but must toggle the CURRENT spans).
    fun toggleFactSelectionFormat(s: Int, e: Int, flag: RichFlag) {
        if (e <= s) return
        val cur = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") customSpans else editedFactSpans
        val add = !spansFullyCovered(cur, s, e, flag)
        val next = toggleSpanFlag(cur, s, e, flag, add)
        if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") customSpans = next else editedFactSpans = next
    }
    // v379 — per-letter UNDERLINE twin (the floating bar's U button): the
    // card's own underline span channel, mirrored onto the active fact's
    // span list exactly like the flag toggle above.
    fun toggleFactSelectionUnderline(s: Int, e: Int, add: Boolean) {
        if (e <= s) return
        val cur = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") customSpans else editedFactSpans
        val next = toggleSpanUnderline(cur, s, e, add)
        if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") customSpans = next else editedFactSpans = next
    }

    val styles = availableStylesForFamily(categoryFamily, topicName)
    val safeIdx = styleIdx.coerceIn(0, styles.lastIndex)
    val currentStyle = styles[safeIdx]
    // v330 — the CURRENT style's move (each style keeps its own offsets).
    val move = movesByStyle[currentStyle] ?: ShareCardMove()
    fun updateMove(m: ShareCardMove) {
        movesByStyle = movesByStyle + (currentStyle to m)
    }
    // v379d — hoisted here (before [runAutoLayout]) so the auto-layout pill
    // can tick haptics; the toolbar haptics below reuses the same val.
    val haptics = LocalHapticFeedback.current
    // v379d — AUTO-LAYOUT pill state: attempt counter (-1 = never run; each
    // tap advances). The plan COMMITS into the per-style [move] (box height
    // + whole-fact text via the same channels the sliders drive), so the
    // saved card and the exported image match the preview, and Reset Layout
    // clears it back to natural smart-fit behaviour.
    var autoLayoutIdx by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    // v384 — live measured card-local bounds of the CURRENT card (title /
    // fact / info rows / favorites strip), fed by ArrangeableCard so the
    // sparkle repairs REAL overlaps instead of guessing from drag offsets.
    var measuredTitle by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var measuredFact by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var measuredMeta by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var measuredFav by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // v3xx — the CATEGORY PILL's measured card-local bounds: the sparkle now
    // keeps the lifted title / grown fact clear of it (the old repair only
    // watched title / fact / info rows / fav strip, so a grown box or a
    // lifted title could bury the pill at the top of the card).
    var measuredBadge by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // v3xx — the sparkle's smart fit now sizes from the MEASURED wrap lines
    // of the current fact text (see [rememberFactWrapLines]) instead of a
    // character count, so a long URL counts by the lines it wraps into.
    val autoLayoutWrapLines = rememberFactWrapLines(factFieldText, chapterFactForCard, aspect)
    fun runAutoLayout() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val len = maxOf(factFieldText.length, chapterFactForCard.length)
        if (len == 0 && chapterFactForCard.isBlank() && progressForCard == null) return
        var attempt = autoLayoutIdx + 1
        // v3xx — AUTO 9:16 DETECTION: when the text is so long that even the
        // hard-floor text scale can't fit the 3:4 free middle (the render
        // would overflow into the footer — the fit is already clamped there),
        // the very NEXT tap jumps straight to the tall 9:16 canvas instead
        // of cycling through the five 3:4 arrangements first. The 1.1 factor
        // makes the detection slightly eager so the user lands on the tall
        // card exactly when the text genuinely needs the height.
        val autoTall = aspect == ShareCardAspect.CLASSIC &&
            autoLayoutWrapLines * factWrapFactor(currentStyle) * factLineHeightDp(currentStyle) * 1.1f * FactFitHardFloor >
                factAvailHeightDp(currentStyle, ShareCardAspect.CLASSIC)
        if (autoTall && attempt < 5) attempt = 5
        // v384 — seven arrangements + the collision repairs; the lookahead is
        // wide enough to always land on a change (attempts that alter nothing
        // are skipped, so every effective tap advances the card).
        // v3xx — the card-local dp box the measured rects live in: the
        // preview is always 280dp wide (sheet + full screen), so the height
        // follows the aspect ratio.
        val cardW = 280f
        val cardH = 280f * aspect.heightDp / aspect.widthDp
        repeat(12) {
            val plan = autoLayoutPlan(currentStyle, aspect, len, attempt, move.factFormat, move, measuredTitle, measuredFact, measuredMeta, measuredFav, measuredBadge, cardW, cardH, wrapLines = autoLayoutWrapLines)
            val wantsTall = plan.tall && aspect == ShareCardAspect.CLASSIC
            val h = plan.heightFrac
            val s = plan.textScale
            val hChanges = h > 0f && kotlin.math.abs(h - move.factHeightFrac) > 0.02f
            val sChanges = s > 0f && kotlin.math.abs(s - move.factScale) > 0.02f
            val fmtChanges = plan.format != null && plan.format != move.factFormat
            val dropCapChanges = plan.factDropCap != null && plan.factDropCap != move.factDropCap
            val titleLiftChanges = kotlin.math.abs(plan.titleLift - move.titleLift) > 1f
            val factLiftChanges = plan.factLift > 0f
            val metaLiftChanges = plan.metaLift != 0f
            val favLiftChanges = plan.favLift > 0f
            // v3xx — the category pill rides the same repair: its signed
            // lift (back up off a dragged-over title / down clear of a fact
            // grown over it) counts as a change like the other elements.
            val badgeLiftChanges = plan.badgeLift != 0f
            // v3xx — ADVANCED solver: a residual overlap (title can't lift
            // past the pill, fact can't push past the card bottom) shrinks
            // the fact BOX, so the tap always resolves the collision.
            val shrinkChanges = plan.factShrinkDp > 0f
            // v3xx — a dragged-over title is RESET to its natural spot on
            // the same tap (the manual vertical offset is zeroed).
            val resetTitleChanges = plan.resetTitleY
            // v3xx — a dragged quick-fact BOX is RESET to its natural spot
            // on the same tap (manual offsets zeroed, like the title reset).
            val resetFactChanges = plan.resetFact
            // v3xx — out-of-card clamp changes (anything that hung off the
            // card edge is pulled back inside on the same tap).
            val fixChanges = plan.fixTitleX != 0f || plan.fixTitleY != 0f ||
                plan.fixFactX != 0f || plan.fixFactY != 0f ||
                plan.fixMetaX != 0f || plan.fixMetaY != 0f ||
                plan.fixFavX != 0f || plan.fixFavY != 0f ||
                plan.fixBadgeX != 0f || plan.fixBadgeY != 0f
            if (wantsTall || hChanges || sChanges || fmtChanges || dropCapChanges ||
                titleLiftChanges || factLiftChanges || metaLiftChanges || favLiftChanges ||
                badgeLiftChanges || shrinkChanges || resetTitleChanges || resetFactChanges ||
                fixChanges
            ) {
                updateMove(move.copy(
                    // v3xx — a residual overlap shrinks the fact box on the
                    // same tap (heightFrac scales with the shrink so the
                    // preview and the exported image match).
                    factHeightFrac = (if (h > 0f) h.coerceIn(0.35f, 6f) else move.factHeightFrac) *
                        (if (shrinkChanges && measuredFact.height > 0f)
                            (1f - (plan.factShrinkDp / measuredFact.height).coerceIn(0f, 0.4f)) else 1f),
                    factScale = if (s > 0f) s.coerceIn(0.5f, 2f) else move.factScale,
                    // v3xx — a dragged fact box is RESET to its natural spot
                    // on the sparkle tap (manual offsets zeroed — the
                    // title-reset twin).
                    factDy = if (plan.resetFact) 0f
                    else move.factDy + plan.factLift.coerceIn(0f, 72f) + plan.fixFactY,
                    factDx = if (plan.resetFact) 0f else move.factDx + plan.fixFactX,
                    factFormat = plan.format ?: move.factFormat,
                    factDropCap = plan.factDropCap ?: move.factDropCap,
                    titleLift = plan.titleLift.coerceIn(0f, 96f),
                    // v3xx — a dragged-over title is RESET to its natural
                    // spot at the top: zero the manual offsets (the lift
                    // above still clears a fact grown up into that spot).
                    titleDx = if (plan.resetTitleY) 0f else move.titleDx + plan.fixTitleX,
                    titleDy = if (plan.resetTitleY) 0f else move.titleDy + plan.fixTitleY,
                    // The manual-placement flag clears with the reset — the
                    // title is back to the design position.
                    titlePlaced = if (plan.resetTitleY) false else move.titlePlaced,
                    // v3xx — SIGNED info-rows repair: negative lifts the
                    // author/year rows UP to touch the title (the snap-to-
                    // title behaviour — the strip can drift FAR below the
                    // fact, so the negative travel is widened so ONE sparkle
                    // tap brings it all the way back between the title and
                    // the quick fact); positive still pushes them down clear
                    // of a fact that sits above them. The out-of-card clamp
                    // rides along.
                    metaDy = move.metaDy + plan.metaLift.coerceIn(-240f, 72f) + plan.fixMetaY,
                    metaDx = move.metaDx + plan.fixMetaX,
                    // v384 — the favorites strip rides the same repair: it is
                    // pushed clear of the title / fact / info rows so it never
                    // overlaps by default (and the sparkle fixes it on tap).
                    favDy = move.favDy + plan.favLift.coerceIn(0f, 120f) + plan.fixFavY,
                    favDx = move.favDx + plan.fixFavX,
                    // v3xx — the category pill rides the same repair: its
                    // signed lift (negative = back up off a title dragged over
                    // it, positive = clear of a fact grown above it) plus the
                    // out-of-card clamp, committed into the badge's own move
                    // offsets so the preview, the saved card and the export
                    // all match.
                    badgeDy = move.badgeDy + plan.badgeLift + plan.fixBadgeY,
                    badgeDx = move.badgeDx + plan.fixBadgeX
                ))
                // A 3:4 card that still overflows fully-fitted gets the tall
                // 9:16 canvas (the one remaining way to add room).
                if (wantsTall) aspect = ShareCardAspect.PORTRAIT
                autoLayoutIdx = attempt
                return
            }
            attempt += 1
        }
        // Nothing could change (e.g. a short fact on a default card) — note
        // the lookahead so the next tap starts one past where we stopped.
        autoLayoutIdx = autoLayoutIdx + 1
    }
    // v379e — the displayed title (edited or the topic name, year stripped):
    // whether the Crop/Box tool shows the Title-height slider (it only means
    // something once the title can wrap to 2+ lines).
    val editedTitleOrDisplay = (editedTitle ?: topicName.substringBeforeLast(" (")).trim()
    // v378 — DOUBLE-TAP a fact arms inline editing: the default quick fact
    // converts into a custom fact (so the typed text sticks), the box is
    // selected, and the transparent inline field takes focus. Mirrors the
    // "Edit text" tool exactly; unused contents (quotes, the progress
    // caption alone) simply select the box as a single tap would.
    fun requestFactInlineEdit() {
        if (activeId != CUSTOM_FACT_ID && activeId != "chapter_review" && progressForCard == null) {
            selectedId = CUSTOM_FACT_ID
            customText = activeSource.text
            editedFact = null
        }
        selectedResizeTarget = ShareCardResizeTarget.FACT
        factEditMode = true
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
    // v377 — the carousel's HorizontalPager drives design changes (swiping;
    // the old design-option panel is gone). [pagerState] is hoisted so the
    // style label/dots can read the page and swipes update [styleIdx].
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = safeIdx.coerceIn(0, styles.lastIndex)) { styles.size }
    // Satisfying haptics: confirm on Save/Share, light ticks on Reset/Done.
    // (the val itself is declared above, next to [runAutoLayout])
    val focusManager = LocalFocusManager.current

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
                put("coverDx", m.coverDx); put("coverDy", m.coverDy)
                put("favDx", m.favDx); put("favDy", m.favDy)
                put("favWidthFrac", m.favWidthFrac); put("favHeightFrac", m.favHeightFrac)
                put("polaroidDx", m.polaroidDx); put("polaroidDy", m.polaroidDy)
                put("polaroidScale", m.polaroidScale)
                put("polaroidStyle", m.polaroidStyle)
                put("polaroidFilter", m.polaroidFilter)
                if (m.polaroidOnCard) put("polaroidOnCard", true)
                if (m.favCount > 0) put("favCount", m.favCount)
                put("titleWidthFrac", m.titleWidthFrac); put("titleHeightFrac", m.titleHeightFrac)
                put("factWidthFrac", m.factWidthFrac); put("factHeightFrac", m.factHeightFrac)
                put("metaWidthFrac", m.metaWidthFrac); put("metaHeightFrac", m.metaHeightFrac)
                put("titleBoxScale", m.titleBoxScale); put("factBoxScale", m.factBoxScale); put("favBoxScale", m.favBoxScale)
                put("titleScale", m.titleScale)
                put("factFormat", m.factFormat.name); put("factDropCap", m.factDropCap.name)
                put("factScale", m.factScale)
                put("factGutter", m.factGutter)
                if (m.titleLift != 0f) put("titleLift", m.titleLift.toDouble())
                if (m.titlePlaced) put("titlePlaced", true)
                put("factUnderline", m.factUnderline)
                m.factHighlight?.let { put("factHighlight", it.toArgb()) }
                put("titleUnderline", m.titleUnderline)
                m.titleHighlight?.let { put("titleHighlight", it.toArgb()) }
            }
            movesObj.put(style.name, o)
        }
        if (movesObj.length() > 0) edit.put("moves", movesObj)
        edit.put("bodyScale", bodyScale)
        if (editedTitle != null) edit.put("editedTitle", editedTitle)
        if (editedFact != null) edit.put("editedFact", editedFact)
        if (editedFactSpans.isNotEmpty()) edit.put("editedFactSpans", spansToJson(editedFactSpans))
        // v342 — the picked content + its live text persist exactly like the
        // moves: which content is shown, the custom-fact / chapter-review
        // text, the review's chapter chip, the reading-progress toggle and
        // the collage caption all come back when the sheet reopens.
        if (selectedId != null) edit.put("selectedId", selectedId)
        edit.put("customText", customText)
        if (customSpans.isNotEmpty()) edit.put("customSpans", spansToJson(customSpans))
        edit.put("polaroidCaption", polaroidCaption)
        edit.put("showChapterProgress", showChapterProgress)
        edit.put("reviewChapterNumber", reviewChapterNumber)
        edit.put("reviewChapterTitle", reviewChapterTitle)
        // v3xx — emoji stickers persist with the rest of the card edits.
        if (stickers.isNotEmpty()) {
            val arr = org.json.JSONArray()
            stickers.forEach { st ->
                val o = org.json.JSONObject()
                o.put("emoji", st.emoji)
                st.imagePath?.let { o.put("imagePath", it) }
                o.put("x", st.x.toDouble())
                o.put("y", st.y.toDouble())
                o.put("sizeFrac", st.sizeFrac.toDouble())
                o.put("rotation", st.rotation.toDouble())
                arr.put(o)
            }
            edit.put("stickers", arr)
        }
        AppPreferences.saveShareCardEdits(context, topicName, edit)
    }

    // onDismissRequest also respects edit mode: the back button and scrim taps
    // are ignored while editing (the swipe path is blocked via the sheet
    // state's confirmValueChange above). v325 — dismissal persists the edits
    // first, so exiting by mistake resumes where you left off.
    // v378 — while editing, sheet DRAG GESTURES are disabled entirely: the
    // old state only blocked the collapse target, so a swipe-down still
    // launched the sheet's drag animation and snapped back (the jumpy,
    // tool-freezing glitch). With gestures off the sheet simply never moves
    // until Done/back drops edit mode (then normal swipe-dismiss returns);
    // content scrolling and the card carousel keep working throughout.
    ModalBottomSheet(onDismissRequest = { if (!editMode) { persistEdits(); onDismiss() } }, sheetState = sheetState, sheetGesturesEnabled = !editMode, containerColor = MaterialTheme.colorScheme.surface, dragHandle = { BottomSheetDefaults.DragHandle() }) {
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
                                // v369 — the chapter-progress widget binds its
                                // own caption; everything else (incl. the
                                // custom fact stacked under progress) binds the
                                // LIVE field text so it edits inline.
                                editFact = if (activeId == "chapter_progress") progressContent.text else factFieldText,
                                onFactChange = { routeFactChange(it) },
                                factEditMode = factEditMode,
                                onFactEditModeChange = { factEditMode = it },
                                onRequestInlineFactEdit = { requestFactInlineEdit() },
                                onToggleEdit = {
                                    editMode = !editMode
                                    toolOpen = null
                                    if (!editMode) {
                                        selectedResizeTarget = ShareCardResizeTarget.NONE
                                        factEditMode = false
                                    }
                                },
                                onSelectResizeTarget = { target ->
                                    // v334 — tapping the quick fact while the
                                    // DEFAULT quick fact is showing auto-arms a
                                    // custom fact seeded with that text, so the
                                    // user can immediately edit it.
                                    if (target == ShareCardResizeTarget.FACT &&
                                        activeId != CUSTOM_FACT_ID && activeId != "chapter_review" &&
                                        progressForCard == null
                                    ) {
                                        selectedId = CUSTOM_FACT_ID
                                        customText = activeSource.text
                                        editedFact = null
                                    }
                                    selectedResizeTarget = target
                                    // v3xx — selecting the favorites strip
                                    // auto-opens its Crop tool (strip width /
                                    // songs / List-Rows) so the user lands
                                    // straight in the sizing controls.
                                    if (target == ShareCardResizeTarget.FAVTRACKS) toolOpen = "box"
                                    // v3xx — selecting the polaroid auto-opens
                                    // its customisation panel (style / filter /
                                    // size), mirroring the favorites strip's
                                    // tap-to-Crop flow.
                                    if (target == ShareCardResizeTarget.POLAROID) toolOpen = "polaroid"
                                },
                                selectedResizeTarget = selectedResizeTarget,
                                move = pageMove,
                                onMove = { movesByStyle = movesByStyle + (styles[page] to it) },
                                factFieldStyle = factFieldStyle,
                                autoFitDelta = smartAutoFitDelta(pageMove, rememberFactWrapLines(factFieldText, chapterFactForCard, aspect), styles[page], aspect),
                                factFieldChipShift = activeId == "chapter_review",
                                factFieldPlaceholder = if (activeId == "chapter_review") "Write your review…" else "Edit the quick fact…",
                                // v379c — the bottom-sheet preview hosts the
                                // SAME rich selection bar as full screen: when
                                // a real text selection is live on the card
                                // (Edit-text tool or double-tap), B / I / U /
                                // highlight float above the letters and style
                                // ONLY the selection. Quotes + reading
                                // progress carry no spans (bar stays dark).
                                richFactTools = true,
                                factSpans = if (isQuotes || activeId == "chapter_progress") emptyList() else cardFactSpans,
                                onFormatFactSelection = if (isQuotes || activeId == "chapter_progress") null
                                else { s, e, flag -> toggleFactSelectionFormat(s, e, flag) },
                                onToggleFactUnderline = if (isQuotes || activeId == "chapter_progress") null
                                else { s, e, add -> toggleFactSelectionUnderline(s, e, add) },
                                // v384 — the CURRENT (center) page feeds the
                                // sparkle's measured-bounds repair; neighbours
                                // stay silent so they can't overwrite it.
                                onMeasuredBounds = if (isCenter) { t, f, m, v, b ->
                                    measuredTitle = t; measuredFact = f; measuredMeta = m; measuredFav = v; measuredBadge = b
                                } else null
                            ) { cb ->
                                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = cardFactText, sharerName = sharer, aspect = aspect, style = styles[page], ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, bookCover = bookCover, isSquareCover = isAlbumTopic, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, onPhotoTap = { photoPickerLauncher.launch("image/*") }, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") null else editedFact, move = pageMove, chapterProgress = progressForCard, chapterFact = chapterFactForCard, factSpans = if (isQuotes) emptyList() else cardFactRenderSpans, stickers = stickers, callbacks = cb)
                            }
                            // v379d — AUTO-LAYOUT pill, current page only (it
                            // must not clutter the peeked neighbours).
                            if (isCenter) {
                                AutoLayoutPill(
                                    onTap = { runAutoLayout() },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 8.dp)
                                )
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
                            // v369 — see the pager branch: chapter-progress
                            // binds its caption, everything else the live text.
                            editFact = if (activeId == "chapter_progress") progressContent.text else factFieldText,
                            onFactChange = { routeFactChange(it) },
                            factEditMode = factEditMode,
                            onFactEditModeChange = { factEditMode = it },
                            onRequestInlineFactEdit = { requestFactInlineEdit() },
                            onToggleEdit = {
                                editMode = !editMode
                                toolOpen = null
                                if (!editMode) {
                                    selectedResizeTarget = ShareCardResizeTarget.NONE
                                    factEditMode = false
                                }
                            },
                            onSelectResizeTarget = { target ->
                                // v334 — see the pager branch above: tapping
                                // the fact auto-converts the default quick fact
                                // into a custom fact ready to edit.
                                if (target == ShareCardResizeTarget.FACT &&
                                    activeId != CUSTOM_FACT_ID && activeId != "chapter_review" &&
                                    progressForCard == null
                                ) {
                                    selectedId = CUSTOM_FACT_ID
                                    customText = activeSource.text
                                    editedFact = null
                                }
                                selectedResizeTarget = target
                                // v3xx — selecting the favorites strip
                                // auto-opens its Crop tool (see the pager
                                // branch above).
                                if (target == ShareCardResizeTarget.FAVTRACKS) toolOpen = "box"
                            },
                            selectedResizeTarget = selectedResizeTarget,
                            move = move,
                            onMove = { updateMove(it) },
                            factFieldStyle = factFieldStyle,
                            autoFitDelta = smartAutoFitDelta(move, rememberFactWrapLines(factFieldText, chapterFactForCard, aspect), currentStyle, aspect),
                            factFieldChipShift = activeId == "chapter_review",
                            factFieldPlaceholder = if (activeId == "chapter_review") "Write your review…" else "Edit the quick fact…",
                            // v379c — rich selection bar on the sheet preview
                            // too (see the pager branch for the rationale).
                            richFactTools = true,
                            factSpans = if (isQuotes || activeId == "chapter_progress") emptyList() else cardFactSpans,
                            onFormatFactSelection = if (isQuotes || activeId == "chapter_progress") null
                            else { s, e, flag -> toggleFactSelectionFormat(s, e, flag) },
                            onToggleFactUnderline = if (isQuotes || activeId == "chapter_progress") null
                            else { s, e, add -> toggleFactSelectionUnderline(s, e, add) },
                            // v384 — feed the sparkle's measured-bounds repair.
                            onMeasuredBounds = { t, f, m, v, b ->
                                measuredTitle = t; measuredFact = f; measuredMeta = m; measuredFav = v; measuredBadge = b
                            }
                        ) { cb ->
                            TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = cardFactText, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, bookCover = bookCover, isSquareCover = isAlbumTopic, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, onPhotoTap = { photoPickerLauncher.launch("image/*") }, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") null else editedFact, move = move, chapterProgress = progressForCard, chapterFact = chapterFactForCard, factSpans = if (isQuotes) emptyList() else cardFactRenderSpans, stickers = stickers, callbacks = cb)
                        }
                        // v379d — AUTO-LAYOUT sparkle pill: floats over the
                        // card top-end in BOTH modes (resting preview + the
                        // Customise editor) and is never part of the layout.
                        AutoLayoutPill(
                            onTap = { runAutoLayout() },
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 8.dp)
                        )
                    }
                }
            }

                // Floating Customise button — over the card, bottom-right.
                // v331 — the Customise pill shows ONLY when NOT editing: while
                // editing, the bottom action bar owns Reset + Done + the
                // content toggle (the pill must not hover over the card
                // mid-edit; Done brings the save/share actions back).
                // Tap-and-hold on the card is the other way in.
                // v373 — the FULL-SCREEN pill stays visible in BOTH modes so
                // the user can jump to the large precise editor mid-customise.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 6.dp)
                ) {
                    if (!editMode) {
                        Surface(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                editMode = !editMode
                                toolOpen = null
                            },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 6.dp
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CurioIcon(name = CurioIcons.Tune, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                                Text("Customise", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }
                    }
                    // v371 — a FULL-SCREEN button joins the Customise pill:
                    // it opens the card large on the category-tint wash with
                    // floating text tools (one Aa pill + dropdown), so text
                    // editing is precise and the layout never changes.
                    // v373 — restyled to MATCH the Customise pill (same
                    // surface/ink) so it no longer reads as a transparent
                    // secondary chip, and stays visible while editing.
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            fullscreenEdit = true
                            // Full screen starts in edit mode so tapping
                            // the title/fact selects + edits it precisely.
                            editMode = true
                            toolOpen = null
                        },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 6.dp
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurioIcon(name = CurioIcons.Fullscreen, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                            Text("Full screen", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            }

            // Edit hint — before editing: "hold". v377 — the mid-edit hint is
            // gone: the toolbar's under-icon captions name each open tool.
            if (!editMode) {
                Text("Hold to edit",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }

            // ── v3xx/v377 EDIT TOOLBAR — circular icon pills; every pill
            // carries a tiny ALWAYS-VISIBLE name under it (v379: Text · Size ·
            // Crop · Fit · Font · Color · Adjust · Align · Format · Content
            // plus the live ratio / Signature labels — see [ToolWithCaption]).
            // Each tool opens ONE small overlay panel; the SELECTED element
            // decides what the size / box / font / align / format tools act
            // on. ──
            if (editMode) {
                val sel = selectedResizeTarget
                val isTitle = sel == ShareCardResizeTarget.TITLE
                val isFact = sel == ShareCardResizeTarget.FACT
                val isMeta = sel == ShareCardResizeTarget.META
                val isFav = sel == ShareCardResizeTarget.FAVTRACKS
                val isPolaroid = sel == ShareCardResizeTarget.POLAROID
                // Box sliders act on title / fact / meta / fav strips / the
                // polaroid; the TEXT-size slider stays title / fact / meta
                // only (a fav strip or polaroid sizes as a box, not as type).
                val isSizable = isTitle || isFact || isMeta || isFav || isPolaroid
                val isTextSizable = isTitle || isFact || isMeta
                val selName = when (sel) {
                    ShareCardResizeTarget.TITLE -> "Title"
                    ShareCardResizeTarget.FACT -> "Quick fact"
                    ShareCardResizeTarget.META -> "Info row"
                    ShareCardResizeTarget.BADGE -> "Category chip"
                    ShareCardResizeTarget.COVER -> "Book cover"
                    ShareCardResizeTarget.FAVTRACKS -> "Favorite tracks"
                    ShareCardResizeTarget.POLAROID -> "Polaroid"
                    ShareCardResizeTarget.NONE -> "Nothing selected"
                }
                val elementFont = when (sel) {
                    ShareCardResizeTarget.TITLE -> move.titleFont
                    ShareCardResizeTarget.FACT -> move.factFont
                    ShareCardResizeTarget.META -> move.metaFont
                    ShareCardResizeTarget.BADGE -> move.badgeFont
                    ShareCardResizeTarget.COVER -> null
                    ShareCardResizeTarget.FAVTRACKS -> null
                    ShareCardResizeTarget.POLAROID -> null
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
                    ShareCardResizeTarget.COVER -> false
                    ShareCardResizeTarget.FAVTRACKS -> false
                    ShareCardResizeTarget.POLAROID -> false
                    ShareCardResizeTarget.NONE -> false
                }
                val elementItalic = when (sel) {
                    ShareCardResizeTarget.TITLE -> move.titleItalic
                    ShareCardResizeTarget.FACT -> move.factItalic
                    ShareCardResizeTarget.META -> move.metaItalic
                    ShareCardResizeTarget.BADGE -> move.badgeItalic
                    ShareCardResizeTarget.COVER -> false
                    ShareCardResizeTarget.FAVTRACKS -> false
                    ShareCardResizeTarget.POLAROID -> false
                    ShareCardResizeTarget.NONE -> false
                }
                // v379 — whole-element UNDERLINE exists for the title + the
                // fact (meta / badge / cover carry no underline field).
                val elementUnderline = when (sel) {
                    ShareCardResizeTarget.TITLE -> move.titleUnderline
                    ShareCardResizeTarget.FACT -> move.factUnderline
                    else -> false
                }
                val setElementFont: (FontFamily?) -> Unit = { fam ->
                    updateMove(when (sel) {
                        ShareCardResizeTarget.TITLE -> move.copy(titleFont = fam)
                        ShareCardResizeTarget.FACT -> move.copy(factFont = fam)
                        ShareCardResizeTarget.META -> move.copy(metaFont = fam)
                        ShareCardResizeTarget.BADGE -> move.copy(badgeFont = fam)
                        ShareCardResizeTarget.COVER -> move
                        ShareCardResizeTarget.FAVTRACKS -> move
                        ShareCardResizeTarget.POLAROID -> move
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
                        ShareCardResizeTarget.COVER -> move
                        ShareCardResizeTarget.FAVTRACKS -> move
                        ShareCardResizeTarget.POLAROID -> move
                        ShareCardResizeTarget.NONE -> move
                    })
                }
                val setElementItalic: (Boolean) -> Unit = { v ->
                    updateMove(when (sel) {
                        ShareCardResizeTarget.TITLE -> move.copy(titleItalic = v)
                        ShareCardResizeTarget.FACT -> move.copy(factItalic = v)
                        ShareCardResizeTarget.META -> move.copy(metaItalic = v)
                        ShareCardResizeTarget.BADGE -> move.copy(badgeItalic = v)
                        ShareCardResizeTarget.COVER -> move
                        ShareCardResizeTarget.FAVTRACKS -> move
                        ShareCardResizeTarget.POLAROID -> move
                        ShareCardResizeTarget.NONE -> move
                    })
                }
                val setElementUnderline: (Boolean) -> Unit = { v ->
                    updateMove(when (sel) {
                        ShareCardResizeTarget.TITLE -> move.copy(titleUnderline = v)
                        ShareCardResizeTarget.FACT -> move.copy(factUnderline = v)
                        else -> move
                    })
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    // v3xx — the TEXT-HISTORY pill sits at the sheet corner
                    // (top-right of the tools) and opens the global browser;
                    // see the block near routeFactChange above.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextHistoryPill(onClick = { historyOpen = true })
                    }
                    // ── Tool pills row (scrollable) ────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // v330 — Edit-text lives in the toolbar row (the
                        // bottom bar owns Reset + Done + the content
                        // selector); it appears when the fact is selected.
                        // v377/v379 — the tool's tiny name sits under its
                        // pill permanently (see [ToolWithCaption]).
                        if (isFact && progressForCard == null) {
                            ToolWithCaption(caption = "Text") {
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
                        }
                        // v377 — the DESIGN options are gone from the toolbar:
                        // other designs switch by swiping the card carousel.
                        // The Style tool exists ONLY on a Signature card, where
                        // a tap flips between the two Signature looks instantly
                        // (no panel). Its caption is the ACTIVE variant.
                        if (currentStyle == ShareCardStyle.SIGNATURE) {
                            ToolWithCaption(caption = if (classicDesign) "Classic" else "Current") {
                                EditToolPill(
                                    glyph = ShareCardStyle.SIGNATURE.glyph,
                                    description = if (classicDesign) "Signature \u00b7 Classic — tap for the current design"
                                    else "Signature \u00b7 Current — tap for the classic design",
                                    active = false,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        classicDesign = !classicDesign
                                    }
                                )
                            }
                        }
                        // Aspect — v3xx: NO options list, tapping toggles
                        // between 3:4 and 9:16 instantly. v377 — the ACTIVE
                        // ratio reads under the icon, and the toggle no longer
                        // closes whatever tool panel is open.
                        ToolWithCaption(caption = aspect.label) {
                            EditToolPill(
                                glyph = CurioIcons.AspectRatio,
                                description = "Card dimensions " + aspect.label,
                                active = false,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    aspect = if (aspect == ShareCardAspect.CLASSIC) ShareCardAspect.PORTRAIT else ShareCardAspect.CLASSIC
                                }
                            )
                        }
                        ToolWithCaption(caption = "Size") {
                            EditToolPill(
                                glyph = "text_increase",
                                description = "Text size",
                                active = toolOpen == "size",
                                onClick = { toolOpen = if (toolOpen == "size") null else "size" }
                            )
                        }
                        ToolWithCaption(caption = "Crop") {
                            EditToolPill(
                                glyph = CurioIcons.Crop,
                                description = "Box size",
                                active = toolOpen == "box",
                                onClick = { toolOpen = if (toolOpen == "box") null else "box" }
                            )
                        }
                        // v374 — Smart fit moved OUT of the Size tool into its
                        // own toggle: long facts grow their box and shrink
                        // their text (through the same sliders) so they fit
                        // the whole card, and the toggle is a separate switch
                        // instead of a Size-tool extra.
                        ToolWithCaption(caption = "Fit") {
                            EditToolPill(
                                glyph = CurioIcons.PhotoSizeSelectLarge,
                                description = "Smart fit",
                                active = toolOpen == "smartfit",
                                onClick = { toolOpen = if (toolOpen == "smartfit") null else "smartfit" }
                            )
                        }
                        ToolWithCaption(caption = "Font") {
                            EditToolPill(
                                glyph = "title",
                                description = "Font",
                                active = toolOpen == "font",
                                onClick = { toolOpen = if (toolOpen == "font") null else "font" }
                            )
                        }
                        ToolWithCaption(caption = "Color") {
                            EditToolPill(
                                glyph = CurioIcons.Palette,
                                description = "Card tone",
                                active = toolOpen == "tone",
                                onClick = { toolOpen = if (toolOpen == "tone") null else "tone" }
                            )
                        }
                        ToolWithCaption(caption = "Adjust") {
                            EditToolPill(
                                glyph = CurioIcons.Contrast,
                                description = "Saturation / contrast",
                                active = toolOpen == "adjust",
                                onClick = { toolOpen = if (toolOpen == "adjust") null else "adjust" }
                            )
                        }
                        ToolWithCaption(caption = "Align") {
                            EditToolPill(
                                glyph = "notes",
                                description = "Alignment + fact layout",
                                active = toolOpen == "align",
                                onClick = { toolOpen = if (toolOpen == "align") null else "align" }
                            )
                        }
                        ToolWithCaption(caption = "Format") {
                            EditToolPill(
                                glyph = CurioIcons.FormatBold,
                                description = "Bold / italic",
                                active = toolOpen == "format",
                                onClick = { toolOpen = if (toolOpen == "format") null else "format" }
                            )
                        }
                        ToolWithCaption(caption = "Content") {
                            EditToolPill(
                                glyph = CurioIcons.Edit,
                                description = "Content (source, custom fact, photo)",
                                active = toolOpen == "source",
                                onClick = { toolOpen = if (toolOpen == "source") null else "source" }
                            )
                        }
                        // v3xx — POLAROID tool in the bottom-sheet toolbar
                        // (frame style / photo filter / print size) — it used
                        // to live full-screen only. Available on EVERY style
                        // (Collage always wears its print; the other styles
                        // opt in via the panel's "Show on card" switch — no
                        // photo needed, the empty frame invites one).
                        ToolWithCaption(caption = "Polaroid") {
                            EditToolPill(
                                glyph = CurioIcons.PhotoLibrary,
                                description = "Polaroid style / filter / size",
                                active = toolOpen == "polaroid",
                                onClick = { toolOpen = if (toolOpen == "polaroid") null else "polaroid" }
                            )
                        }
                        // v330 — Reset + Done live in the bottom action bar
                        // while editing (see below); the floating cluster over
                        // the card is gone.
                    }

                    // v370b — WRITING BOX below the tools: a compact field
                    // for the ACTIVE text content (quick fact / custom fact /
                    // chapter review) so typing never needs the tap-the-box /
                    // Edit-text dance — plus an Enlarge button that opens a
                    // full white writing sheet for long text. This also gives
                    // the custom fact (and the review under Reading progress)
                    // a real input, not just the inline caret.
                    val writeBoxTarget = when (activeId) {
                        QUICK_FACT_ID, CUSTOM_FACT_ID, "chapter_review" -> activeId
                        else -> null
                    }
                    if (writeBoxTarget != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = factFieldText,
                                onValueChange = { routeFactChange(it) },
                                placeholder = {
                                    Text(
                                        when (writeBoxTarget) {
                                            "chapter_review" -> "Write your review…"
                                            CUSTOM_FACT_ID -> "Custom fact…"
                                            else -> "Quick fact…"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                },
                                maxLines = 3,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showWriteSheet = true
                                },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CurioIcon(name = CurioIcons.Fullscreen, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 14.dp)
                                    Text("Enlarge", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                    // v370b — the ENLARGE sheet (same state as the compact
                    // box — everything stays in sync live). v375 — it hosts
                    // the Save-your-take rich editor (Format dock) on the
                    // theme surface; formatting lands on the card + export.
                    if (showWriteSheet) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showWriteSheet = false },
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            // v375 — the ENLARGE sheet hosts the SAME
                            // Save-your-take editor as the capture formats: a
                            // Format dock (bold / italic / highlighter /
                            // letter size) styles the SELECTION, and the
                            // styled text lands on the card + export live
                            // (spans ride the fact state). The old plain
                            // outlined field had no formatting and its M3
                            // outline looked foreign. Rendered on the theme
                            // surface so the editor's dock reads in BOTH
                            // themes (the old white sheet broke dark mode).
                            Surface(
                                Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                // v3xx — imePadding lifts the sheet above the
                                // keyboard and the editor area scrolls, so a
                                // long note's text can always be reached and
                                // selected without closing the keyboard.
                                Column(Modifier.fillMaxSize().imePadding().padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            when (writeBoxTarget) {
                                                "chapter_review" -> "Chapter review"
                                                CUSTOM_FACT_ID -> "Custom fact"
                                                else -> "Quick fact"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // v3xx — history pill in the writing
                                        // sheet's corner (restore writes into
                                        // this same field).
                                        TextHistoryPill(onClick = { historyOpen = true })
                                        Spacer(Modifier.width(10.dp))
                                        TextButton(onClick = { showWriteSheet = false }) {
                                            Text("Done", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    // v375 — bold/italic/highlight + size on
                                    // the selection; every change keeps text
                                    // AND spans in sync (routeRichFact).
                                    // v3xx — the editor area scrolls above the
                                    // keyboard: the weight is on a scrollable
                                    // wrapper now (weight inside a scrollable
                                    // Column is illegal), so a long note is
                                    // always reachable and selectable.
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        RichTextEditor(
                                            text = factFieldText,
                                            spans = factFieldSpans,
                                            onRichTextChange = { t, sp -> routeRichFact(t, sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = "Start writing…",
                                            minHeight = 140.dp,
                                            accent = MaterialTheme.colorScheme.primary,
                                            ink = MaterialTheme.colorScheme.onSurface,
                                            surface = MaterialTheme.colorScheme.surfaceVariant,
                                            showFieldBorder = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // v371 — the FULL-SCREEN text editor: the card renders
                    // LARGE on the category-tint wash, floating tools stay
                    // minimal (Close left, ONE Aa pill right that opens the
                    // dropdown with every text tool), and inline editing is
                    // precise because the card is big. Same TopicShareCard,
                    // so the export matches the full-screen preview exactly.
                    if (fullscreenEdit) {
                        val fullBg = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, accent, 0.12f)
                        // v379 — the whole-element Highlight swatch row is gone
                        // from the text tools (highlight lives only on the
                        // floating bar over a live selection), so no presets
                        // list is kept here.
                        var fsToolsOpen by remember { mutableStateOf(false) }
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = {
                                fullscreenEdit = false
                                // v3xx — the sticker + polaroid tools reset with
                                // the editor.
                                stickerToolsOpen = false
                                selectedSticker = -1
                                polaroidToolsOpen = false
                            },
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            // v375 — the Dialog is a COLUMN (top bar, tools
                            // panel, card area) instead of an overlay row with
                            // a DropdownMenu popup: the popup measured the
                            // tools' verticalScroll with unbounded height
                            // (infinite-constraint crash) and its scrollable
                            // fought slider/swipe drags.
                            // v3xx-CI — the Dialog root is a BOX with an inner
                            // page Column: the tool panels (Text / Stickers /
                            // Polaroid) live as Box children so they truly
                            // FLOAT over the bottom of the card (align
                            // BottomCenter) instead of joining the Column flow
                            // — where their height would re-shrink the
                            // weight(1f) card area and re-zoom the preview.
                            // v3xx — imePadding lifts the whole editor (top
                            // bar, card, floating panels) ABOVE the keyboard
                            // while typing: the card re-zooms into the
                            // visible space and everything stays reachable
                            // and selectable without closing the keyboard.
                            Box(Modifier.fillMaxSize().background(fullBg).imePadding()) {
                            Column(Modifier.fillMaxSize()) {
                                // ── Top bar: Close (left) · Aa pill (right) ─
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        onClick = {
                                            fullscreenEdit = false
                                            // v3xx — the sticker + polaroid tools
                                            // reset with the editor.
                                            stickerToolsOpen = false
                                            selectedSticker = -1
                                            polaroidToolsOpen = false
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                        shadowElevation = 4.dp
                                    ) {
                                        // v3xx — ICON-ONLY top pills: the text labels made the top
                                        // bar crowd on narrow screens; every button is now a
                                        // compact circle (desc rides the icon's semantics).
                                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                            CurioIcon(name = CurioIcons.Close, contentDescription = "Close full screen", tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 19.dp)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // v373 — DIMENSIONS button: toggles the
                                        // card aspect (3:4 ↔ 9:16) right from
                                        // full screen, mirroring the sheet's
                                        // Aspect tool.
                                        Surface(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                aspect = if (aspect == ShareCardAspect.CLASSIC) ShareCardAspect.PORTRAIT else ShareCardAspect.CLASSIC
                                            },
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                CurioIcon(name = CurioIcons.AspectRatio, contentDescription = "Card dimensions (" + aspect.label + ")", tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 19.dp)
                                            }
                                        }
                                        // v378 — RESET LAYOUT (full screen only): a
                                        // one-tap layout-only reset — positions /
                                        // box crops / auto-lift return to the
                                        // design defaults while every text edit
                                        // (fonts, sizes, formats, colours) stays.
                                        Surface(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                updateMove(move.resetLayout())
                                            },
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                CurioIcon(name = CurioIcons.Refresh, contentDescription = "Reset layout (keeps text edits)", tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 19.dp)
                                            }
                                        }
                                        // v3xx — STICKERS button (full screen
                                        // only): opens the emoji sticker panel —
                                        // tap a sticker to add it, then drag it
                                        // on the card, resize it and re-stack it.
                                        Surface(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                stickerToolsOpen = !stickerToolsOpen
                                                fsToolsOpen = false
                                                polaroidToolsOpen = false
                                            },
                                            shape = RoundedCornerShape(50),
                                            color = if (stickerToolsOpen) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                CurioIcon(name = CurioIcons.AutoAwesome, contentDescription = "Stickers", tint = if (stickerToolsOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, size = 19.dp)
                                            }
                                        }
                                        // v3xx — POLAROID button (every style):
                                        // opens the print's style / filter /
                                        // size panel; the grip on the card
                                        // moves it. The polaroid renders on
                                        // Collage always; on other styles it is
                                        // OPT-IN — the panel's "Show on card"
                                        // switch turns the print on per card
                                        // (no photo required; the empty frame
                                        // invites one).
                                        Surface(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                polaroidToolsOpen = !polaroidToolsOpen
                                                stickerToolsOpen = false
                                                fsToolsOpen = false
                                            },
                                            shape = RoundedCornerShape(50),
                                            color = if (polaroidToolsOpen) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                CurioIcon(name = CurioIcons.PhotoLibrary, contentDescription = "Polaroid", tint = if (polaroidToolsOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, size = 19.dp)
                                            }
                                        }
                                        Surface(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                fsToolsOpen = !fsToolsOpen
                                                stickerToolsOpen = false
                                                polaroidToolsOpen = false
                                            },
                                            shape = RoundedCornerShape(50),
                                            color = if (fsToolsOpen) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                CurioIcon(name = CurioIcons.FormatText, contentDescription = "Text tools", tint = if (fsToolsOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, size = 19.dp)
                                            }
                                        }
                                        // v3xx — text-history pill (top-right of
                                        // the full-screen editor).
                                        TextHistoryPill(onClick = { historyOpen = true })
                                    }
                                }
                                // v3xx — the full-screen tool panels (Text /
                                // Stickers / Polaroid) render BELOW the card,
                                // at the bottom of the editor (see after the
                                // card area) so the tools sit under the thumb.
                                // ── The big card, centered ──────────────
                                // v373 — the card is rendered at the SAME base
                                // size as the bottom-sheet preview (280dp) and
                                // zoomed through a scaled Density, so text
                                // sizes, spacing and placements scale TOGETHER
                                // and the full-screen preview is an exact zoom
                                // of the sheet card. (The old approach rendered
                                // the dp layout in a much bigger box: text
                                // stayed tiny and the SpaceBetween flow
                                // re-spread the spacing, so it never matched.)
                                // v375 — the card area is the Column's leftover
                                // space (weight), below the top bar and panel.
                                // v3xx — the tool panels (Text / Stickers /
                                // Polaroid) now FLOAT OVER the card instead of
                                // pushing it: the card's BoxWithConstraints
                                // keeps the FULL leftover height (fillMaxSize
                                // inside a weight(1f) Box), so opening a panel
                                // never shrinks the preview, re-wraps its text
                                // or changes its look — the old in-flow panels
                                // ate the card's height and re-zoomed it.
                                Box(Modifier.weight(1f)) {
                                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val ratio = aspect.widthDp.toFloat() / aspect.heightDp.toFloat()
                                    val baseW = 280f
                                    val baseH = baseW / ratio
                                    val availW = (maxWidth.value - 24f).coerceAtLeast(120f)
                                    val availH = (maxHeight.value - 20f).coerceAtLeast(120f)
                                    val zoom = minOf(availW / baseW, availH / baseH)
                                    val sheetDensity = androidx.compose.ui.platform.LocalDensity.current
                                    // v378 — the zoom lives in the DENSITY ONLY
                                    // (fontScale stays untouched): dp and sp both
                                    // scale through density, so multiplying
                                    // fontScale too made sp TEXT zoom twice and
                                    // read as oversized — the "zoomed and cut
                                    // from below" preview bug.
                                    val cardDensity = androidx.compose.ui.unit.Density(sheetDensity.density * zoom, sheetDensity.fontScale)
                                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides cardDensity) {
                                        Box(
                                            Modifier
                                                .width(baseW.dp)
                                                .aspectRatio(ratio)
                                                .shadow(4.dp, RoundedCornerShape(6.dp))
                                                .clip(RoundedCornerShape(6.dp))
                                        ) {
                                        ArrangeableCard(
                                            active = true,
                                            editMode = true,
                                            quoteMode = isQuotes,
                                            editFact = if (activeId == "chapter_progress") progressContent.text else factFieldText,
                                            onFactChange = { routeFactChange(it) },
                                            factEditMode = factEditMode,
                                            onFactEditModeChange = { factEditMode = it },
                                            onRequestInlineFactEdit = { requestFactInlineEdit() },
                                            onToggleEdit = {},                            onSelectResizeTarget = { target ->
                                if (target == ShareCardResizeTarget.FACT &&
                                    activeId != CUSTOM_FACT_ID && activeId != "chapter_review" &&
                                    progressForCard == null
                                ) {
                                    selectedId = CUSTOM_FACT_ID
                                    customText = activeSource.text
                                    editedFact = null
                                }
                                selectedResizeTarget = target
                                // v3xx — selecting the polaroid auto-opens its
                                // full-screen customisation panel.
                                if (target == ShareCardResizeTarget.POLAROID) polaroidToolsOpen = true
                            },
                                            selectedResizeTarget = selectedResizeTarget,
                                            move = move,
                                            onMove = { updateMove(it) },
                                            factFieldStyle = factFieldStyle,
                                            autoFitDelta = smartAutoFitDelta(move, rememberFactWrapLines(factFieldText, chapterFactForCard, aspect), currentStyle, aspect),
                                            factFieldChipShift = activeId == "chapter_review",
                                            factFieldPlaceholder = if (activeId == "chapter_review") "Write your review…" else "Edit the quick fact…",
                                            // v375 — the full-screen editor keeps a REAL text
                                            // SELECTION in its transparent fact field and floats
                                            // the Save-your-take format bar (B / I / highlight)
                                            // over the selection; taps toggle [cardFactSpans] so
                                            // the formatting applies ONLY to the selected letters
                                            // (Quote cards + the chapter-progress caption keep
                                            // whole-element formatting only — no selection bar).
                                            richFactTools = true,
                                            factSpans = if (isQuotes || activeId == "chapter_progress") emptyList() else cardFactSpans,
                                            onFormatFactSelection = if (isQuotes || activeId == "chapter_progress") null
                                            else { s, e, flag -> toggleFactSelectionFormat(s, e, flag) },
                                            onToggleFactUnderline = if (isQuotes || activeId == "chapter_progress") null
                                            else { s, e, add -> toggleFactSelectionUnderline(s, e, add) }
                                        ) { cb ->
                                            TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = cardFactText, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, bookCover = bookCover, isSquareCover = isAlbumTopic, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, onPhotoTap = { photoPickerLauncher.launch("image/*") }, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") null else editedFact, move = move, chapterProgress = progressForCard, chapterFact = chapterFactForCard, factSpans = if (isQuotes) emptyList() else cardFactRenderSpans, stickers = stickers, callbacks = cb)
                                        }
                                        // v3xx — the STICKER edit layer (full
                                        // screen only): every sticker is
                                        // tappable / draggable and the selected
                                        // one wears the coffee chrome. Drawn
                                        // OVER the ArrangeableCard chrome, so a
                                        // drag here belongs to the sticker.
                                        // v3xx — always live while stickers
                                        // exist (the old gate on the panel
                                        // being OPEN left a placed emoji stuck:
                                        // closing the panel made it untappable,
                                        // so an emoji overlapping a box could
                                        // never be selected again).
                                        if (stickers.isNotEmpty()) {
                                            StickerEditOverlay(
                                                stickers = stickers,
                                                selectedIndex = selectedSticker,
                                                onSelect = { selectedSticker = it },
                                                onMove = { idx, dxFrac, dyFrac ->
                                                    val lst = stickers.toMutableList()
                                                    val st = lst[idx]
                                                    lst[idx] = st.copy(
                                                        x = (st.x + dxFrac).coerceIn(0f, (1f - st.sizeFrac).coerceAtLeast(0f)),
                                                        y = (st.y + dyFrac).coerceIn(0f, (1f - st.sizeFrac).coerceAtLeast(0f))
                                                    )
                                                    stickers = lst
                                                },
                                                // v3xx — pinch scales the emoji
                                                // (the Size slider stays for fine
                                                // control).
                                                onResize = { idx, zoom ->
                                                    val lst = stickers.toMutableList()
                                                    val st = lst[idx]
                                                    lst[idx] = st.copy(
                                                        sizeFrac = (st.sizeFrac * zoom).coerceIn(0.08f, 0.6f)
                                                    )
                                                    stickers = lst
                                                },
                                                // v3xx — two-finger twist turns
                                                // the sticker (the Rotation
                                                // slider stays for fine control;
                                                // the angle normalizes to
                                                // -180..180 so the slider thumb
                                                // stays valid).
                                                onRotate = { idx, deg ->
                                                    val lst = stickers.toMutableList()
                                                    val st = lst[idx]
                                                    lst[idx] = st.copy(
                                                        rotation = normDegrees(st.rotation + deg)
                                                    )
                                                    stickers = lst
                                                }
                                            )
                                        }
                                    }
                                    }
                                }
                                }
                                }
                            // Fully-qualified: an enclosing ColumnScope
                            // elsewhere in the sheet would otherwise hijack
                            // these calls into ColumnScope.AnimatedVisibility
                            // ("cannot be called with an implicit receiver") —
                            // the panels are Box children, so the TOP-LEVEL
                            // animation function is the one we want.
                            androidx.compose.animation.AnimatedVisibility(
                                visible = fsToolsOpen,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                enter = fadeIn(tween(170)) + expandVertically(tween(170)),
                                exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
                            ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                    ) {
                                        Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(vertical = 4.dp)) {
                                                val fsSel = selectedResizeTarget
                                                val fsIsTitle = fsSel == ShareCardResizeTarget.TITLE
                                                val fsIsFact = fsSel == ShareCardResizeTarget.FACT
                                                val fsIsText = fsIsTitle || fsIsFact
                                                // Arm precise inline editing (fact).
                                                Pill("Edit fact text", CurioIcons.Edit, factEditMode) {
                                                    if (selectedResizeTarget != ShareCardResizeTarget.FACT) {
                                                        if (activeId != CUSTOM_FACT_ID && activeId != "chapter_review" && progressForCard == null) {
                                                            selectedId = CUSTOM_FACT_ID
                                                            customText = activeSource.text
                                                            editedFact = null
                                                        }
                                                        selectedResizeTarget = ShareCardResizeTarget.FACT
                                                    }
                                                    factEditMode = true
                                                }
                                                if (!fsIsText) {
                                                    Text("Tap the title or fact on the card first", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                                } else {
                                                    // Font
                                                    Text("Font", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                                    Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        shareFonts.forEach { f ->
                                                            val selF = if (fsIsTitle) move.titleFont == f.family else move.factFont == f.family
                                                            Pill(f.label, CurioIcons.FormatText, selF) {
                                                                updateMove(if (fsIsTitle) move.copy(titleFont = f.family) else move.copy(factFont = f.family))
                                                            }
                                                        }
                                                    }
                                                    // Size — v378 the FACT thumb
                                                    // shows the rendered size with
                                                    // smart fit folded in (same as
                                                    // the sheet's Size tool).
                                                    Box(Modifier.padding(horizontal = 12.dp)) {
                                                        val fsFit = if (!fsIsTitle) smartAutoFitDelta(
                                                            move,
                                                            rememberFactWrapLines(factFieldText, chapterFactForCard, aspect),
                                                            currentStyle, aspect
                                                        ) else null
                                                        // v379d — cover title shrink folded
                                                        // into the thumb (see the sheet's
                                                        // Size tool for the formula).
                                                        val fsCw = if (isAlbumTopic) 66f else 44f
                                                        val fsCoverF = if (bookCover != null &&
                                                            (currentStyle == ShareCardStyle.SIGNATURE || currentStyle == ShareCardStyle.CUSTOM)
                                                        ) (0.90f + (92f - fsCw) / 92f * 0.06f).coerceIn(0.9f, 0.97f) else 1f
                                                        val fsCur = if (fsIsTitle) (move.titleScale * fsCoverF).coerceIn(0.5f, 2f)
                                                            else (bodyScale * fsFit!!.textScale * move.factScale).coerceIn(0.5f, 2f)
                                                        TextSizeSliderColumn(
                                                            label = if (fsIsTitle) "Title size" else "Fact size",
                                                            value = fsCur,
                                                            onValueChange = { v ->
                                                                if (fsIsTitle) updateMove(move.copy(titleScale = (v / fsCoverF).coerceIn(0.5f, 2f)))
                                                                else bodyScale = (v / (fsFit!!.textScale * move.factScale)).coerceIn(0.5f, 2f)
                                                            }
                                                        )
                                                    }
                                                    // Bold / Italic / Underline —
                                                    // v379: ICON-ONLY round toggles
                                                    // (the old label pills doubled
                                                    // the glyph with the letter and
                                                    // looked off), and the whole-
                                                    // element Highlight swatch row
                                                    // is gone (highlight now lives
                                                    // only on the floating bar over
                                                    // a selection).
                                                    val fsBold = if (fsIsTitle) move.titleBold else move.factBold
                                                    val fsItalic = if (fsIsTitle) move.titleItalic else move.factItalic
                                                    val fsUnder = if (fsIsTitle) move.titleUnderline else move.factUnderline
                                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Format", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Spacer(Modifier.weight(1f))
                                                        EditToolPill(glyph = CurioIcons.FormatBold, description = "Bold", active = fsBold, onClick = { updateMove(if (fsIsTitle) move.copy(titleBold = !fsBold) else move.copy(factBold = !fsBold)) })
                                                        EditToolPill(glyph = CurioIcons.FormatItalic, description = "Italic", active = fsItalic, onClick = { updateMove(if (fsIsTitle) move.copy(titleItalic = !fsItalic) else move.copy(factItalic = !fsItalic)) })
                                                        EditToolPill(glyph = CurioIcons.FormatUnderline, description = "Underline", active = fsUnder, onClick = { updateMove(if (fsIsTitle) move.copy(titleUnderline = !fsUnder) else move.copy(factUnderline = !fsUnder)) })
                                                    }
                                                    // Align (v379 — Justify joins
                                                    // Left / Center / Right here,
                                                    // matching the sheet's Align).
                                                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Align", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        val curAlign = if (fsIsTitle) move.titleAlign else move.factAlign
                                                        listOf("Left" to TextAlign.Start, "Center" to TextAlign.Center, "Right" to TextAlign.End, "Justify" to TextAlign.Justify).forEach { (label, ta) ->
                                                            Pill(label, CurioIcons.FormatText, curAlign == ta) {
                                                                updateMove(if (fsIsTitle) move.copy(titleAlign = ta) else move.copy(factAlign = ta))
                                                            }
                                                        }
                                                    }
                                                    // Fact format (quick/custom fact only)
                                                    if (fsIsFact) {
                                                        Text("Fact layout", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                                        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            ShareCardFactFormat.entries.forEach { fmt ->
                                                                Pill(fmt.label, CurioIcons.FormatText, move.factFormat == fmt) {
                                                                    updateMove(move.copy(factFormat = fmt))
                                                                }
                                                            }
                                                        }
                                                        // v379 — BOOK-page column-gap
                                                        // slider (see the sheet's
                                                        // Align tool for the twin).
                                                        if (move.factFormat == ShareCardFactFormat.BOOK) {
                                                            Column(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                                                                SizeSliderColumn(
                                                                    "Column gap",
                                                                    move.factGutter,
                                                                    { updateMove(move.copy(factGutter = it)) },
                                                                    0.3f..2.5f, steps = 21,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            }
                                                        }
                                                        if (move.factFormat == ShareCardFactFormat.EDITORIAL) {
                                                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                ShareCardFactDropCap.entries.forEach { dc ->
                                                                    Pill(dc.label, CurioIcons.FormatText, move.factDropCap == dc) {
                                                                        updateMove(move.copy(factDropCap = dc))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    // v373/v379e — BOX SIZE editor in
                                                    // full screen (the sheet's Crop
                                                    // tool): width / height sliders
                                                    // only — the Whole-box scale is
                                                    // gone from the UI (its v373 role
                                                    // is covered by Smart fit and the
                                                    // auto-layout pill).
                                                    Text("Box size", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                                        if (fsIsTitle) {
                                                            SizeSliderColumn("Title width", move.titleWidthFrac, { updateMove(move.copy(titleWidthFrac = it)) }, 0.3f..1f, steps = 69, modifier = Modifier.fillMaxWidth())
                                                            // v379e — Title height caps wrapped
                                                            // lines, so it hides for short
                                                            // one-line titles (see the sheet's
                                                            // Crop tool for the rationale).
                                                            if (editedTitleOrDisplay.length > 22) {
                                                                SizeSliderColumn("Title height", move.titleHeightFrac, { updateMove(move.copy(titleHeightFrac = it)) }, 0.35f..2.5f, steps = 26, modifier = Modifier.fillMaxWidth())
                                                            }
                                                        } else {
                                                            SizeSliderColumn("Fact width", move.factWidthFrac, { updateMove(move.copy(factWidthFrac = it)) }, 0.3f..1.2f, steps = 89, modifier = Modifier.fillMaxWidth())
                                                            // v379d/v3xx — folded height thumb: shows the smart-fit-grown
                                                            // height (WYSIWYG). v3xx — the WRITE captures the rendered value
                                                            // directly instead of dividing back through the stale fit: the
                                                            // old (v / fsFitH) collapsed the box (~2.4× → ~1×) on the very
                                                            // first drag tick, because writing factHeightFrac disengages the
                                                            // render-time fit (touched) and the divided-back base then
                                                            // rendered unfitted. The first tick now sets the base to the
                                                            // thumb position AND captures the fit's text shrink into
                                                            // factScale (the grip-seed recipe) so the type doesn't pop back
                            // to full size — the handoff is seamless, later ticks map 1:1.
                                                            val fsFitH = smartAutoFitDelta(
                                                                move,
                                                                rememberFactWrapLines(factFieldText, chapterFactForCard, aspect),
                                                                currentStyle, aspect
                                                            ).heightFrac
                                                            val fsFitT = smartAutoFitDelta(
                                                                move,
                                                                rememberFactWrapLines(factFieldText, chapterFactForCard, aspect),
                                                                currentStyle, aspect
                                                            ).textScale
                                                            val fsUntouched = move.factHeightFrac == 1f && move.factScale == 1f
                                                            SizeSliderColumn(
                                                                "Fact height",
                                                                (move.factHeightFrac * fsFitH).coerceIn(0.35f, 6f),
                                                                { v -> updateMove(move.copy(
                                                                    factHeightFrac = v.coerceIn(0.35f, 6f),
                                                                    factScale = if (fsUntouched && fsFitT != 1f) move.factScale * fsFitT else move.factScale
                                                                )) },
                                                                0.35f..6f, steps = 56, modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        }
                                // v3xx — the STICKER panel (full screen only):
                                // an emoji picker row + the selected sticker's
                                // size / z-order / delete controls. Rendered
                                // inline like the text tools so its scroll is
                                // bounded and sliders drag cleanly. v3xx —
                                // opens/closes with a smooth fade + slide.
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = stickerToolsOpen,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    enter = fadeIn(tween(170)) + expandVertically(tween(170)),
                                    exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                    ) {
                                        Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(vertical = 10.dp, horizontal = 12.dp)) {
                                            Text("Stickers", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.height(8.dp))
                                            Text("Tap a sticker to add it, then drag it anywhere on the card", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Pinch it on the card to resize", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.height(8.dp))
                                            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                stickerEmojis.forEach { emoji ->
                                                    Surface(
                                                        onClick = {
                                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            stickers = stickers + ShareSticker(emoji = emoji, x = 0.30f, y = 0.28f)
                                                            selectedSticker = stickers.lastIndex
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        modifier = Modifier.size(42.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(emoji, fontSize = 21.sp)
                                                        }
                                                    }
                                                }
                                            }
                                            // v3xx — IMPORT your own PNG cutout
                                            // (transparent sticker image): copied
                                            // into the app's stickers dir and
                                            // dropped on the card like an emoji.
                                            Spacer(Modifier.height(10.dp))
                                            Surface(
                                                onClick = { stickerPickerLauncher.launch("image/*") },
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier.height(38.dp)
                                            ) {
                                                Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    CurioIcon(name = CurioIcons.Image, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 15.dp)
                                                    Text("Import PNG cutout", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            if (stickers.isNotEmpty()) {
                                                Spacer(Modifier.height(12.dp))
                                                Text("Tap a sticker on the card to select it, drag to move, pinch to resize and twist to rotate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                            if (selectedSticker in stickers.indices) {
                                                val sel = selectedSticker
                                                val selSt = stickers[sel]
                                                // Size — the sticker's emoji size as a
                                                // fraction of the card width.
                                                SizeSliderColumn(
                                                    label = "Sticker size",
                                                    value = selSt.sizeFrac,
                                                    onValueChange = { v ->
                                                        stickers = stickers.toMutableList().also { lst ->
                                                            lst[sel] = lst[sel].copy(sizeFrac = v)
                                                        }
                                                    },
                                                    range = 0.08f..0.6f,
                                                    steps = 52,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                // v3xx — ROTATION: fine control via the
                                                // slider (a two-finger twist on the
                                                // card also turns it).
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                                    Text("Rotation", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Slider(
                                                        value = selSt.rotation,
                                                        onValueChange = { v ->
                                                            stickers = stickers.toMutableList().also { lst ->
                                                                lst[sel] = lst[sel].copy(rotation = v)
                                                            }
                                                        },
                                                        valueRange = -180f..180f,
                                                        steps = 34,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text("${selSt.rotation.roundToInt()}°", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                                    Pill("0°", CurioIcons.Refresh, selSt.rotation == 0f) {
                                                        stickers = stickers.toMutableList().also { lst ->
                                                            lst[sel] = lst[sel].copy(rotation = 0f)
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                    // Z-order: to front (end of the list = drawn on top)
                                                    Surface(
                                                        onClick = {
                                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            val lst = stickers.toMutableList()
                                                            val it = lst.removeAt(sel)
                                                            lst.add(it)
                                                            stickers = lst
                                                            selectedSticker = lst.lastIndex
                                                        },
                                                        shape = RoundedCornerShape(50),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        modifier = Modifier.height(36.dp)
                                                    ) {
                                                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                            CurioIcon(name = CurioIcons.ArrowUpward, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 13.dp)
                                                            Text("To front", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    // Z-order: to back (front of the list = drawn below)
                                                    Surface(
                                                        onClick = {
                                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            val lst = stickers.toMutableList()
                                                            val it = lst.removeAt(sel)
                                                            lst.add(0, it)
                                                            stickers = lst
                                                            selectedSticker = 0
                                                        },
                                                        shape = RoundedCornerShape(50),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        modifier = Modifier.height(36.dp)
                                                    ) {
                                                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                            CurioIcon(name = CurioIcons.ArrowDownward, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 13.dp)
                                                            Text("To back", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Spacer(Modifier.weight(1f))
                                                    Surface(
                                                        onClick = {
                                                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                                            stickers = stickers.toMutableList().also { it.removeAt(sel) }
                                                            selectedSticker = -1
                                                        },
                                                        shape = RoundedCornerShape(50),
                                                        color = MaterialTheme.colorScheme.errorContainer,
                                                        modifier = Modifier.height(36.dp)
                                                    ) {
                                                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                            CurioIcon(name = CurioIcons.Delete, tint = MaterialTheme.colorScheme.onErrorContainer, size = 13.dp)
                                                            Text("Delete", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onErrorContainer)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // v3xx — POLAROID panel (every style the
                                // print can appear on): the print's STYLE
                                // (frame + tape + tilt), PHOTO FILTER and SIZE
                                // pickers live here; the grip on the card moves
                                // the print. Opens/closes with a smooth fade +
                                // slide, and now FLOATS over the card (it no
                                // longer shrinks the preview).
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = polaroidToolsOpen,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    enter = fadeIn(tween(170)) + expandVertically(tween(170)),
                                    exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                    ) {
                                        Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(vertical = 10.dp, horizontal = 12.dp)) {
                                            Text("Polaroid", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.height(8.dp))
                                            // v3xx — OPT-IN: on non-Collage styles
                                            // the print only renders while this
                                            // switch is on (never always-on).
                                            if (currentStyle != ShareCardStyle.COLLAGE) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Show on card", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                                    Switch(
                                                        checked = move.polaroidOnCard,
                                                        onCheckedChange = { on -> updateMove(move.copy(polaroidOnCard = on)) }
                                                    )
                                                }
                                                Spacer(Modifier.height(12.dp))
                                            }
                                            Text("Style", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.height(6.dp))
                                            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                polaroidStyleNames.forEachIndexed { i, name ->
                                                    Pill(name, CurioIcons.Palette, move.polaroidStyle == i) {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        updateMove(move.copy(polaroidStyle = i))
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(12.dp))
                                            Text("Photo filter", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.height(6.dp))
                                            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                polaroidFilterNames.forEachIndexed { i, name ->
                                                    Pill(name, CurioIcons.PhotoSizeSelectLarge, move.polaroidFilter == i) {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        updateMove(move.copy(polaroidFilter = i))
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(12.dp))
                                            SizeSliderColumn(
                                                label = "Print size",
                                                value = move.polaroidScale,
                                                onValueChange = { v ->
                                                    updateMove(move.copy(polaroidScale = v.coerceIn(0.6f, 1.6f)))
                                                },
                                                range = 0.6f..1.6f,
                                                steps = 20,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }}
                        }
                    }

                    // ── One small overlay for the open tool ────────────
                    // v377 — the "style" panel is GONE: designs switch by
                    // swiping the card carousel, and a Signature card's two
                    // looks flip from the toolbar's Style toggle instead.
                    when (toolOpen) {
                        // v340 — text size is a PRECISE slider now (the old
                        // fixed % buttons jumped 0.15–0.3× at a time, so long
                        // text went from too small to cut off with no middle
                        // ground). Continuous 1% steps with a haptic tick per
                        // step and a live × readout.
                        "size" -> if (isTextSizable) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("$selName size", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                // v378 — the quick-fact slider shows (and
                                // drives) the size the CARD ACTUALLY RENDERS:
                                // smart fit's shrink is folded into the thumb
                                // (bodyScale × fit.textScale × …), so checking
                                // the size never reads 1× while the text sits
                                // at 0.8×. Dragging writes the base back
                                // through the fit (WYSIWYG) — the fit itself
                                // stays engaged and keeps clipping-safe.
                                val sizeFit = if (!isTitle) smartAutoFitDelta(
                                    move,
                                    rememberFactWrapLines(factFieldText, chapterFactForCard, aspect),
                                    currentStyle, aspect
                                ) else null
                                // v379d — on Signature / Custom cards WITH a
                                // glued-out cover, the renderer also multiplies
                                // the title by coverTitleScale (~0.9–0.97) so
                                // the headline fits beside the jacket. Fold it
                                // into the thumb (the size shown IS what the
                                // card renders) and write the base back.
                                // Only Signature / Custom apply the side-cover
                                // shrink (the glued designs render the cover
                                // INSIDE the title block, Collage feeds it into
                                // the photo).
                                val cwV = if (isAlbumTopic) 66f else 44f
                                val coverF = if (bookCover != null &&
                                    (currentStyle == ShareCardStyle.SIGNATURE || currentStyle == ShareCardStyle.CUSTOM)
                                ) (0.90f + (92f - cwV) / 92f * 0.06f).coerceIn(0.9f, 0.97f) else 1f
                                val cur = when {
                                    isTitle -> (move.titleScale * coverF).coerceIn(0.5f, 2f)
                                    isFact -> (bodyScale * sizeFit!!.textScale * move.factScale).coerceIn(0.5f, 2f)
                                    else -> bodyScale
                                }
                                TextSizeSliderColumn(
                                    label = when {
                                        isTitle -> "Title text"
                                        isFact -> "Quick-fact text"
                                        else -> "Text size"
                                    },
                                    value = cur,
                                    onValueChange = { s ->
                                        when {
                                            isTitle -> updateMove(move.copy(titleScale = (s / coverF).coerceIn(0.5f, 2f)))
                                            isFact -> bodyScale = (s / (sizeFit!!.textScale * move.factScale)).coerceIn(0.5f, 2f)
                                            else -> bodyScale = s
                                        }
                                    }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Reset restores the BASE slider to 1×; when
                                    // smart fit is shrinking long text the thumb
                                    // stays at the fitted size (no hidden state).
                                    val baseAt1 = if (isTitle) move.titleScale == 1f
                                        else bodyScale == 1f && move.factScale == 1f
                                    Pill("Reset to 1\u00d7", CurioIcons.Refresh, baseAt1) {
                                        if (isTitle) updateMove(move.copy(titleScale = 1f)) else {
                                            bodyScale = 1f
                                            updateMove(move.copy(factScale = 1f))
                                        }
                                    }
                                    Text(
                                        if (isTitle) "Longer titles auto-fit their box — this scales the type precisely."
                                        else "The shown size is what the card renders — smart fit folds its auto size into this thumb.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                    // v379e — the Title-height slider only means
                                    // something once the title can wrap (it caps
                                    // how many lines show); hidden for short
                                    // one-line titles so it never reads as a dead
                                    // control (long titles: nudge it to allow /
                                    // ellipsize more wrapped lines).
                                    if (editedTitleOrDisplay.length > 22) {
                                        SizeSliderColumn("Title height", move.titleHeightFrac, { updateMove(move.copy(titleHeightFrac = it)) }, 0.35f..2.5f, steps = 26, modifier = Modifier.fillMaxWidth())
                                    }
                                } else if (isFact) {
                                    SizeSliderColumn("Fact width", move.factWidthFrac, { updateMove(move.copy(factWidthFrac = it)) }, 0.3f..1.2f, steps = 89, modifier = Modifier.fillMaxWidth())
                                    // v369/v370 — the fact box height range runs
                                    // to 6x so tall 9:16 cards can expand a long
                                    // fact far past the old 2.5x cap.
                                    // v379d/v3xx — the thumb shows the smart-fit
                                    // GROWN height (like the text thumb): while
                                    // smart fit is engaged the box renders at
                                    // move.factHeightFrac × fit.heightFrac, so the
                                    // thumb would otherwise read 1× while the box
                                    // sat at 2×. v3xx — the WRITE captures the
                                    // rendered value directly (no ÷ fitH): the old
                                    // division collapsed the box on the first drag
                                    // tick because writing factHeightFrac
                                    // disengages the fit — the base then rendered
                                    // unfitted and shrank ~2.4× → ~1×. The first
                                    // tick now sets the base to the thumb position
                                    // AND captures the fit's text shrink into
                                    // factScale (the grip-seed recipe) so the type
                                    // doesn't pop back to full size and clip;
                                    // later ticks map 1:1.
                                    val fitH = if (isFact) smartAutoFitDelta(
                                        move,
                                        rememberFactWrapLines(factFieldText, chapterFactForCard, aspect),
                                        currentStyle, aspect
                                    ).heightFrac else 1f
                                    val fitT = if (isFact) smartAutoFitDelta(
                                        move,
                                        rememberFactWrapLines(factFieldText, chapterFactForCard, aspect),
                                        currentStyle, aspect
                                    ).textScale else 1f
                                    val fitUntouched = move.factHeightFrac == 1f && move.factScale == 1f
                                    SizeSliderColumn(
                                        "Fact height",
                                        (move.factHeightFrac * fitH).coerceIn(0.35f, 6f),
                                        { v -> updateMove(move.copy(
                                            factHeightFrac = v.coerceIn(0.35f, 6f),
                                            factScale = if (fitUntouched && fitT != 1f) move.factScale * fitT else move.factScale
                                        )) },
                                        0.35f..6f, steps = 56, modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (isPolaroid) {
                                    // v3xx — COLLAGE polaroid: the SIZE slider
                                    // scales the whole print (frame + photo +
                                    // caption) as one.
                                    SizeSliderColumn(
                                        "Polaroid size",
                                        move.polaroidScale,
                                        { v -> updateMove(move.copy(polaroidScale = v.coerceIn(0.6f, 1.6f))) },
                                        0.6f..1.6f, steps = 20, modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (isFav) {
                                    // v370/v384 — ALBUM favorite-tracks strip:
                                    // width scales the strip's max width; the
                                    // SONGS slider picks how many tracks show
                                    // (1..all; auto = the default 3, or ALL
                                    // when No fact expands the strip).
                                    SizeSliderColumn("Strip width", move.favWidthFrac, { updateMove(move.copy(favWidthFrac = it)) }, 0.3f..1.2f, steps = 89, modifier = Modifier.fillMaxWidth())
                                    val favTotal = AppPreferences.albumFavTracksState[topicName].orEmpty().size.coerceAtLeast(1)
                                    val favNoFact = activeSource.id == NO_FACT_ID
                                    val favAuto = if (favNoFact) favTotal else 3
                                    val favShown = (if (move.favCount > 0) move.favCount else favAuto).coerceIn(1, favTotal)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Songs shown", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$favShown", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    }
                                    Slider(
                                        value = favShown.toFloat(),
                                        onValueChange = { updateMove(move.copy(favCount = it.roundToInt().coerceIn(1, favTotal))) },
                                        valueRange = 1f..favTotal.toFloat(),
                                        steps = (favTotal - 1).coerceAtLeast(0),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    // v384 — LIST vs ROWS (chips) layout,
                                    // global across every design.
                                    val favRows = AppPreferences.albumFavRowsState
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Layout", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                        Surface(onClick = { AppPreferences.setAlbumFavRows(context, false) }, shape = RoundedCornerShape(50), color = if (!favRows) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(32.dp)) {
                                            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // v3xx — drag_handle (three stacked lines) = LIST, grid_view = ROWS: the old
                                                // raw view_agenda/view_module strings aren't in the bundled icon subset, so they
                                                // rendered as literal text ("view_agenda") next to the label.
                                                CurioIcon(name = CurioIcons.DragHandle, tint = if (!favRows) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 13.dp)
                                                Text("List", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (!favRows) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Surface(onClick = { AppPreferences.setAlbumFavRows(context, true) }, shape = RoundedCornerShape(50), color = if (favRows) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(32.dp)) {
                                            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                CurioIcon(name = CurioIcons.GridView, tint = if (favRows) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 13.dp)
                                                Text("Rows", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (favRows) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                } else {
                                    SizeSliderColumn("Info width", move.metaWidthFrac, { updateMove(move.copy(metaWidthFrac = it)) }, 0.3f..1f, steps = 69, modifier = Modifier.fillMaxWidth())
                                    SizeSliderColumn("Info lines", move.metaHeightFrac, { updateMove(move.copy(metaHeightFrac = it)) }, 0.5f..1f, steps = 4, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        } else {
                            Text("Tap a thing on the card first", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        // v374 — Smart fit's OWN toggle (moved out of the Size
                        // tool): a single switch, no intensity presets. It
                        // applies to the quick/custom fact on every style.
                        "smartfit" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Smart fit", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                    // v379e — TEXT-FIRST wording: the box keeps
                                    // its full height and the text size shrinks
                                    // with the length; the box only grows when
                                    // the text can't shrink any further.
                                    Text("Long facts shrink their text to fit (the box keeps its height) — the box grows only when the text can't shrink more. Turning this back ON re-fits the box even after you resized it.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = AppPreferences.shareAutoFitState,
                                    onCheckedChange = { on ->
                                        AppPreferences.setShareAutoFitEnabled(context, on)
                                        // v379e — switching ON again re-fits the
                                        // box: clear any manual box + fit-seed so
                                        // the render-time fit re-engages ("it
                                        // fixes the box"). Manual position stays.
                                        if (on) updateMove(move.copy(
                                            factWidthFrac = 1f,
                                            factHeightFrac = 1f,
                                            factScale = 1f,
                                            factBoxScale = 1f
                                        ))
                                    }
                                )
                            }
                        }
                        "font" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$selName font", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Tone · level unlocks", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                // v369 — always-available tones (base + the
                                // new dark variants) show immediately; the
                                // premium tones join the row at their level.
                                availableTones().forEachIndexed { i, p ->
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
                                "Applies to every style and the saved image.",
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
                                // v377 — the FACT TEXT LAYOUT presets moved in
                                // here from the Format tool (v370's Standard /
                                // Condensed / Book page / Editorial recompose
                                // how the fact body is set on every style).
                                if (isFact) {
                                    Text("Fact layout", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                                    ) {
                                        ShareCardFactFormat.entries.forEach { f ->
                                            Pill(f.label, "notes", move.factFormat == f) {
                                                updateMove(move.copy(factFormat = f))
                                            }
                                        }
                                    }
                                    // v379 — BOOK-page layout: the gap
                                    // between the two columns is adjustable
                                    // (hugged to spread like a real page).
                                    if (move.factFormat == ShareCardFactFormat.BOOK) {
                                        SizeSliderColumn(
                                            "Column gap",
                                            move.factGutter,
                                            { updateMove(move.copy(factGutter = it)) },
                                            0.3f..2.5f, steps = 21,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    // Editorial-only: the drop-cap variant,
                                    // visible only while the layout is
                                    // Editorial so it never clutters.
                                    if (move.factFormat == ShareCardFactFormat.EDITORIAL) {
                                        Text("Drop cap", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ShareCardFactDropCap.entries.forEach { c ->
                                                Pill(c.label, "title", move.factDropCap == c) {
                                                    updateMove(move.copy(factDropCap = c))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Tap the title or quick fact first", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        "format" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$selName format", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill("Bold", CurioIcons.FormatBold, elementBold) {
                                    setElementBold(!elementBold)
                                }
                                Pill("Italic", CurioIcons.FormatItalic, elementItalic) {
                                    setElementItalic(!elementItalic)
                                }
                                // v379 — Underline joins the whole-element
                                // Bold / Italic row (title + fact only; the
                                // info row / badge have no underline field).
                                if (isTitle || isFact) {
                                    Pill("Underline", CurioIcons.FormatUnderline, elementUnderline) {
                                        setElementUnderline(!elementUnderline)
                                    }
                                }
                            }
                        }
                        "source" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Content", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            // v331 — the content SOURCE pills live HERE again
                            // (the panel that both the toolbar Content tool and
                            // the bottom-bar content toggle open). Pick Quick
                            // fact / No fact / a saved source / + Custom fact.
                            if (sourceOptions.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                                ) {
                                    sourceOptions.forEach { opt ->
                                        val picked = opt.id == activeId || (opt.id == "chapter_progress" && showChapterProgress)
                                        // v335 — the content pills drive the progress bar
                                        // separately from the fact: Reading progress turns
                                        // the bar on, Custom fact keeps it on (fact stacks
                                        // below), anything else turns it off.
                                        val onPick: () -> Unit = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            when (opt.id) {
                                                "chapter_progress" -> {
                                                    showChapterProgress = true
                                                    selectedId = opt.id
                                                }
                                                // v374 — the custom fact AND the
                                                // quick fact both stack under the
                                                // Reading-progress bar, so picking
                                                // either keeps the bar on.
                                                CUSTOM_FACT_ID, QUICK_FACT_ID -> {
                                                    selectedId = opt.id
                                                }
                                                else -> {
                                                    showChapterProgress = false
                                                    selectedId = opt.id
                                                }
                                            }
                                        }
                                        // v377 — "No fact" is an icon-ONLY pill: the
                                        // eye-cross hides the fact box with no words.
                                        if (opt.id == NO_FACT_ID) {
                                            IconPill(
                                                icon = CurioIcons.VisibilityOff,
                                                contentDesc = "No fact — hide the fact box",
                                                selected = picked,
                                                onClick = onPick
                                            )
                                        } else {
                                            Pill(
                                                opt.label + (opt.rating?.takeIf { r -> r > 0 }?.let { " · " + "\u2605".repeat(it) } ?: ""),
                                                if (opt.id == CUSTOM_FACT_ID) CurioIcons.Add else CurioIcons.FormatText,
                                                picked,
                                                onClick = onPick
                                            )
                                        }
                                    }
                                }
                            }
                            // v328 — BOOK chapter contents: pick the chapter
                            // (CH 1..N) that the progress/review refers to.
                            // For Reading progress the pick WRITES the
                            // BookNotes pref (progress only moves forward), so
                            // the card and the notes reader stay in sync; for
                            // Chapter review it tags the review text.
                            if (hasBookChapters &&
                                (activeId == "chapter_progress" || activeId == "chapter_review")
                            ) {
                                if (activeId == "chapter_progress") {
                                    // v334 — progress is a SLIDER (a chapter
                                    // count can run into the hundreds; chips
                                    // don't scale). It writes the exact count
                                    // BOTH ways so a drag back undoes chapters,
                                    // and stays in sync with Book Notes.
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "Reading progress",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "$chaptersRead of ${bookChapters.size} chapters",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Slider(
                                            value = chaptersRead.toFloat().coerceIn(0f, bookChapters.size.toFloat()),
                                            onValueChange = { v ->
                                                val n = v.roundToInt().coerceIn(0, bookChapters.size)
                                                if (n != chaptersRead) {
                                                    AppPreferences.setBookReadingProgressExact(context, topicName, n)
                                                }
                                            },
                                            valueRange = 0f..bookChapters.size.toFloat(),
                                            steps = (bookChapters.size - 1).coerceAtLeast(0)
                                        )
                                        Text(
                                            "Also updates your Book Notes reading progress for this book — drag back to undo.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    // Chapter review: the review is about ONE
                                    // chapter, so a chip row is the right picker.
                                    val chosen = effectiveReviewChapter ?: 0
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
                                                reviewChapterNumber = ch.number
                                            }
                                        }
                                    }
                                    Text(
                                        "Your review is tagged \"CH ${effectiveReviewChapter ?: ""}\" on the card.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    // v369 — the chapter TITLE is a separate
                                    // edit (blank keeps the book's authored
                                    // title); the review text itself is typed
                                    // inline on the card, not in a toolbar box.
                                    OutlinedTextField(
                                        value = reviewChapterTitle,
                                        onValueChange = { reviewChapterTitle = it.take(48) },
                                        placeholder = { Text("Chapter title (blank = the book's own)", style = MaterialTheme.typography.labelMedium) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            // v369 — custom fact / chapter review text is typed
                            // INLINE on the card (tap the box, then Edit text) —
                            // the old toolbar text box is gone so the caret
                            // always sits on the visible glyphs.
                            if (activeId == CUSTOM_FACT_ID) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CurioIcon(
                                        name = CurioIcons.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 15.dp
                                    )
                                    Text(
                                        "Select the box on the card and tap Edit text to type it inline.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // v334 — BOOK topics: the cover controls (gallery /
                            // fetch / remove) replace the generic photo row;
                            // the polaroid caption stays for Collage.
                            // v335 — the cover only appears when the user taps
                            // Fetch (never on open); the gallery pick and Remove
                            // are still one tap away.
                            // v370b — ALBUMS and SERIES get the same Fetch /
                            // Refetch / Remove row (the gallery pick stays
                            // book-only — the cover is fetched artwork, not a
                            // user photo).
                            if (isBookTopic || isAlbumTopic || isSeriesTopic) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (isBookTopic) {
                                        Surface(onClick = { coverPickerLauncher.launch("image/*") }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(40.dp)) {
                                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                CurioIcon(name = CurioIcons.PhotoLibrary, tint = if (bookCover != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                                                Text(if (bookCover != null) "Change" else "Gallery", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (bookCover != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    // Fetch (first time) / Try again (after a
                                    // failed fetch) / Refetch (cover showing).
                                    // The first fetch uses the topic's authored
                                    // cover; every fetch after a failure or a
                                    // shown cover moves to the keyless providers.
                                    Surface(
                                        onClick = {
                                            if (coverLoadFailed || bookCover != null) coverAttempt += 1
                                            coverLoadFailed = false
                                            coverFetchRequested = true
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            CurioIcon(
                                                name = CurioIcons.Refresh,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                size = 14.dp
                                            )
                                            Text(
                                                when {
                                                    bookCover == null && coverLoadFailed -> "Try again"
                                                    bookCover == null -> "Fetch"
                                                    else -> "Refetch"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                    if (bookCover != null) {
                                        // v335 — removing the cover also disarms the fetch so a
                                        // later tap reads as a fresh "Fetch" again.
                                        Surface(onClick = { bookCover = null; coverLoadFailed = false; coverFetchRequested = false }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(40.dp)) {
                                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                CurioIcon(name = CurioIcons.Close, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                                                Text("Remove", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    if (currentStyle == ShareCardStyle.COLLAGE) {
                                        OutlinedTextField(value = polaroidCaption, onValueChange = { polaroidCaption = it.take(36) }, placeholder = { Text(if (sharer.isNotBlank()) "$sharer \u00b7 via Curio" else "via Curio", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) }, singleLine = true, textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(50), modifier = Modifier.weight(1f))
                                    }
                                }
                            } else if (currentStyle == ShareCardStyle.COLLAGE) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(onClick = { photoPickerLauncher.launch("image/*") }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.height(40.dp)) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            CurioIcon(name = CurioIcons.PhotoLibrary, tint = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                                            Text(if (userPhoto != null) "Change" else "Photo", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (userPhoto != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    OutlinedTextField(value = polaroidCaption, onValueChange = { polaroidCaption = it.take(36) }, placeholder = { Text(if (sharer.isNotBlank()) "$sharer \u00b7 via Curio" else "via Curio", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) }, singleLine = true, textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(50), modifier = Modifier.weight(1f))
                                }
                            }
                            val albumFavs = AppPreferences.albumFavTracksState[topicName].orEmpty()
                            if (currentStyle == ShareCardStyle.VINYL && albumFavs.isEmpty()) {
                                // v336/v337 — the typed favorite-song line
                                // only shows when no heart-picks exist (the
                                // heart strip owns that corner otherwise).
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
                            if (albumFavs.isNotEmpty()) {
                                // v353 — the heart-picked strip's editor row
                                // (any style): explains the pick and offers a
                                // Show / Hide toggle so the user can drop the
                                // strip from the card without clearing picks.
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CurioIcon(
                                        name = CurioIcons.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 16.dp
                                    )
                                    Text(
                                        "Favorite tracks are the songs you heart-picked in the album's track list",
                                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val stripVisible = AppPreferences.albumFavStripVisibleState
                                    Surface(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            AppPreferences.setAlbumFavStripVisible(context, !stripVisible)
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = if (stripVisible) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            ShareHeartGlyph(
                                                color = if (stripVisible) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                                iconSize = 11.dp,
                                                filled = stripVisible
                                            )
                                            Text(
                                                if (stripVisible) "On card" else "Hidden",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (stripVisible) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // v3xx — POLAROID customisation in the bottom sheet
                        // (was full-screen only): frame style / photo filter /
                        // print size, mirroring the full-screen panel. Opens
                        // automatically when the polaroid on the card is
                        // selected (or via the toolbar's Polaroid pill).
                        "polaroid" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Polaroid", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            // v3xx — OPT-IN: on non-Collage styles the print
                            // only renders while this switch is on (never
                            // always-on); Collage always wears its own print.
                            if (currentStyle != ShareCardStyle.COLLAGE) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Show on card", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = move.polaroidOnCard,
                                        onCheckedChange = { on -> updateMove(move.copy(polaroidOnCard = on)) }
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                polaroidStyleNames.forEachIndexed { i, name ->
                                    Pill(name, CurioIcons.Palette, move.polaroidStyle == i) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        updateMove(move.copy(polaroidStyle = i))
                                    }
                                }
                            }
                            Text("Photo filter", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                polaroidFilterNames.forEachIndexed { i, name ->
                                    Pill(name, CurioIcons.PhotoSizeSelectLarge, move.polaroidFilter == i) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        updateMove(move.copy(polaroidFilter = i))
                                    }
                                }
                            }
                            SizeSliderColumn(
                                label = "Print size",
                                value = move.polaroidScale,
                                onValueChange = { v ->
                                    updateMove(move.copy(polaroidScale = v.coerceIn(0.6f, 1.6f)))
                                },
                                range = 0.6f..1.6f,
                                steps = 20,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            val eh = pw * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()
            if (editMode) {
                // ── v331 — edit-mode bottom bar ─────────────────────────
                // While customising, this row REPLACES Save/Share/Text: a
                // single CONTENT TOGGLE (shows what the card is currently
                // saying) plus Reset and Done. Tapping the toggle opens the
                // content options (Quick fact / No fact / saved sources /
                // + Custom fact) in the same panel the toolbar's Content
                // tool opens, where they opened before. Tapping Done brings
                // the save/share actions straight back.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            toolOpen = if (toolOpen == "source") null else "source"
                        },
                        shape = RoundedCornerShape(50),
                        color = if (toolOpen == "source") MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // v377 — when No fact is active the toggle shows the
                            // eye-cross icon alone (no words): it HIDES the fact
                            // box, and the icon says so.
                            val noFactActive = activeSource.id == NO_FACT_ID
                            CurioIcon(
                                name = if (noFactActive) CurioIcons.VisibilityOff else CurioIcons.FormatText,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp
                            )
                            if (noFactActive) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                Text(
                                    activeSource.label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            CurioIcon(name = CurioIcons.KeyboardArrowDown, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
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
                            // v3xx — the sticker layer clears with the rest of
                            // the card edits.
                            stickers = emptyList()
                            selectedSticker = -1
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
                            TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = cardFactText, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, bookCover = bookCover, isSquareCover = isAlbumTopic, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") null else editedFact, move = move, chapterProgress = progressForCard, chapterFact = chapterFactForCard, factSpans = if (isQuotes) emptyList() else cardFactRenderSpans, stickers = stickers)
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
                // Share button — opens the share dialog (v384): pick whether
                // to ride a link + message, or just share the picture (with a
                // caption or none).
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    if (linkCaption.isBlank()) {
                        linkCaption = topicName.substringBeforeLast(" (").trim()
                    }
                    showLinkDialog = true
                }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary), modifier = Modifier.weight(1f).height(44.dp)) {
                    Text("Share", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
                // Share as text button
                Surface(onClick = {
                    val text = shareAsText?.invoke() ?: buildString {
                        append(topicName).append("\n")
                        append(activeSource.text).append("\n\n")
                        append(categoryName).append(" · via Curio · Stay curious")
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
            // v384 — the SHARE dialog (a separate window; placed here so it
            // dismisses cleanly above the sheet, like the Enlarge editor). It
            // replaces the old Link pill: every Share tap lands here, and the
            // user chooses whether to ride the topic's link + message (text
            // share) or just share the picture (with the caption or none).
            if (showLinkDialog) {
                // Evaluated on open so the CURRENT Settings choice wins.
                val linkUrl = shareLinkUrl?.invoke()
                    ?: ("https://www.google.com/search?q=" + android.net.Uri.encode(topicName.substringBeforeLast(" (").trim()))
                // Display name without the trailing " (author)" note, reused below so
                // the message template carries no nested-quote escapes.
                val displayTopic = topicName.substringBeforeLast(" (").trim()
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showLinkDialog = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = true)
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Share", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Say something about $displayTopic — your words ride with the picture, and the topic's link rides along too when you turn it on.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = linkCaption,
                                onValueChange = { linkCaption = it },
                                label = { Text("Your message (optional)") },
                                placeholder = { Text("e.g. Loved this one — you should check it out") },
                                minLines = 2, maxLines = 5,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            // v384 — the include-link switch: on = your words +
                            // the topic's link post as a text message; off = just
                            // the picture (with the caption attached or none).
                            if (shareLinkUrl != null) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Include a link", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                        Text("Google search — or your music service for songs", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Switch(
                                        checked = includeLink,
                                        onCheckedChange = { includeLink = it }
                                    )
                                }
                                if (includeLink) {
                                    Spacer(Modifier.height(8.dp))
                                    // Link preview — the tap-to-open URL that rides along.
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        CurioIcon(name = "link", tint = MaterialTheme.colorScheme.primary, size = 14.dp)
                                        Text(
                                            linkUrl,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showLinkDialog = false }) {
                                    Text("Cancel", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                        val caption = linkCaption.trim()
                                        if (includeLink) {
                                            // Link share: your words + the CARD PICTURE, with the
                                            // topic's link riding as Telegram-style hidden-link text —
                                            // [Topic](url) renders as "Topic" with the URL hidden
                                            // behind the word, so the picture is never lost to a
                                            // bare link post.
                                            val body = buildString {
                                                if (caption.isNotEmpty()) {
                                                    append(caption)
                                                    append("\n\n")
                                                }
                                                append("[").append(displayTopic).append("](").append(linkUrl).append(")")
                                            }
                                            shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, shareText = body, card = {
                                                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = cardFactText, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, bookCover = bookCover, isSquareCover = isAlbumTopic, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") null else editedFact, move = move, chapterProgress = progressForCard, chapterFact = chapterFactForCard, factSpans = if (isQuotes) emptyList() else cardFactRenderSpans, stickers = stickers)
                                            })
                                        } else {
                                            // Plain share: the picture, with the caption attached (or none).
                                            shareComposableCard(context = context, cardSize = androidx.compose.ui.unit.DpSize(pw, eh), authority = authority, exportDensity = 4f, shareText = caption.ifBlank { null }, card = {
                                                TopicShareCard(topicName = topicName, categoryName = categoryName, categoryGlyph = categoryGlyph, accent = accent, factText = cardFactText, sharerName = sharer, aspect = aspect, style = currentStyle, ratingStars = activeSource.rating, categoryFamily = categoryFamily, quoteText = if (activeSource.id == "quote") activeSource.text else null, quoteAuthor = if (activeSource.id == "quote") topicByline.ifBlank { null } else null, userPhoto = userPhoto, bookCover = bookCover, isSquareCover = isAlbumTopic, byline = topicByline, polaroidCaption = polaroidCaption,                        classicSignature = classicDesign, toneIndex = toneIndex.takeIf { it >= 0 }, saturation = saturation, contrast = contrast, bodyScale = bodyScale, editedTitle = editedTitle, editedFact = if (activeId == CUSTOM_FACT_ID || activeId == "chapter_review") null else editedFact, move = move, chapterProgress = progressForCard, chapterFact = chapterFactForCard, factSpans = if (isQuotes) emptyList() else cardFactRenderSpans, stickers = stickers)
                                            })
                                        }
                                        persistEdits()
                                        showLinkDialog = false
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                                ) {
                                    Text("Share", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                                }
                            }
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

/** v3xx — the emoji sticker palette offered by the full-screen sticker
 *  tool: a small curated set that reads on light and dark card surfaces. */
private val stickerEmojis = listOf(
    "🌟", "✨", "🔥", "💖", "🎉", "🌈", "⭐", "🎯", "🚀", "🌺",
    "🌸", "🌙", "☀️", "🍀", "🦋", "🐝", "🌻", "🍓", "🍒", "🎵",
    "🎶", "🎸", "🎨", "📚", "💡", "🔮", "💎", "🏆", "🎁", "💫",
    "🫧", "🧸", "🌵", "🐙", "🦄", "🐳", "🌊", "🍁", "❄️", "☁️",
    "🔔", "💌", "🎀", "🧩", "♟️", "🎲", "🍕", "🥑"
)

/**
 * v3xx — the full-screen editor's STICKER edit layer: renders the card's
 * emoji stickers on top of everything with the same fraction positions /
 * sizes [TopicShareCard] uses (so the chrome hugs the glyphs exactly), and
 * makes them interactive — tap to select, drag to move. Drawn only while
 * the sticker tool is open; the panel's slider + To front / To back /
 * Delete buttons drive size and stacking.
 */
@Composable
private fun BoxScope.StickerEditOverlay(
    stickers: List<ShareSticker>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onMove: (index: Int, dxFrac: Float, dyFrac: Float) -> Unit,
    // v3xx — PINCH-to-resize + rotate: the transform gesture reports a zoom
    // factor AND a rotation delta; the caller scales the sticker's sizeFrac /
    // turns its rotation (see the wiring).
    onResize: (index: Int, zoom: Float) -> Unit,
    onRotate: (index: Int, degrees: Float) -> Unit
) {
    if (stickers.isEmpty()) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    // v3xx — tapping the card OUTSIDE every sticker deselects (the selected
    // sticker's handles disappear). The tap layer is the FIRST (bottom-most)
    // child: sibling hit-testing only routes a tap to the top sticker under
    // the finger, so empty-area taps land here and sticker taps never do.
    // Only armed while a sticker is selected — with nothing selected the
    // layer stays fully transparent to the card chrome below.
    val deselectArmed = selectedIndex in stickers.indices
    BoxWithConstraints(Modifier.matchParentSize()) {
        val cw = maxWidth
        val ch = maxHeight
        if (deselectArmed) {
            val tapTargets = stickers.map { st ->
                val w = st.sizeFrac.coerceIn(0.05f, 0.8f)
                val imgRatio = st.imagePath?.let { p ->
                    decodeStickerBitmap(p)?.let { b -> b.height.toFloat() / b.width.toFloat() }
                }
                val h = if (imgRatio != null) w * imgRatio * cw.value / ch.value else w * cw.value / ch.value
                Triple(st.x.coerceIn(0f, 1f), st.y.coerceIn(0f, 1f), w to h)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(stickers) {
                        detectTapGestures { off ->
                            val fx = off.x / cw.value
                            val fy = off.y / ch.value
                            val overSticker = tapTargets.any { (sx, sy, wh) ->
                                fx >= sx && fx <= sx + wh.first && fy >= sy && fy <= sy + wh.second
                            }
                            if (!overSticker) onSelect(-1)
                        }
                    }
            )
        }
        stickers.forEachIndexed { idx, st ->
            val isSel = idx == selectedIndex
            val px = cw.value * st.sizeFrac.coerceIn(0.05f, 0.8f)
            val img = st.imagePath?.let { p ->
                androidx.compose.runtime.remember(p) { decodeStickerBitmap(p) }
            }
            val sizeMod = if (img != null) {
                val ratio = img.height.toFloat() / img.width.toFloat()
                Modifier.width(px.dp).height((px * ratio).dp)
            } else Modifier
            Box(
                modifier = Modifier
                    .offset(x = cw * st.x.coerceIn(0f, 1f), y = ch * st.y.coerceIn(0f, 1f))
                    .then(sizeMod)
                    .graphicsLayer { rotationZ = st.rotation }
                    .border(
                        if (isSel) 1.5.dp else 0.dp,
                        if (isSel) CoffeeChromeDeep else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    // v3xx — ONE transform recognizer handles tap (select),
                    // drag (move), pinch (resize) AND two-finger rotation: the
                    // gesture reports a centroid pan, a zoom factor and a
                    // rotation delta, so a one-finger drag still moves the
                    // sticker, a pinch scales it and a two-finger twist turns
                    // it (a plain tap selects).
                    .pointerInput(idx) {
                        detectTransformGestures { _, pan, zoom, rot ->
                            onSelect(idx)
                            if (cw.value > 0f && ch.value > 0f) {
                                val dx = with(density) { pan.x.toDp().value }
                                val dy = with(density) { pan.y.toDp().value }
                                if (dx != 0f || dy != 0f) onMove(idx, dx / cw.value, dy / ch.value)
                            }
                            if (zoom != 1f) onResize(idx, zoom)
                            if (rot != 0f) onRotate(idx, rot)
                        }
                    }
            ) {
                if (img != null) {
                    androidx.compose.foundation.Image(bitmap = img, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text(st.emoji, fontSize = px.sp, softWrap = false)
                }
            }
        }
    }
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
    onDelta: (dx: Float, dy: Float) -> Unit,
    // v341 — drag-lifecycle hooks: the editor's alignment guides render while
    // the grip is live and clear the instant it lifts (see ArrangeableCard).
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val latestDelta by rememberUpdatedState(onDelta)
    val latestStart by rememberUpdatedState(onDragStart)
    val latestEnd by rememberUpdatedState(onDragEnd)
    // v325 — while dragging, the knob shrinks and fades so the text it moves
    // stays readable underneath (it used to sit right on the glyphs).
    var dragging by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(22.dp)
            // v3xx — the handle ALWAYS wins the touch: a selected box that
            // overlaps its neighbours is drawn with zIndex 2 (see
            // ArrangeableCard), which previously sat ABOVE the handle (zIndex
            // 0) and stole its drags — the "handle doesn't work when boxes
            // overlap" bug. A top zIndex here puts the grip in front of every
            // box in every state, so tapping/dragging it always moves the box.
            .zIndex(10f)
            .graphicsLayer {
                shadowElevation = 2.dp.toPx(); shape = CircleShape; clip = false
                alpha = if (dragging) 0.55f else 1f
            }
            .background(CoffeeChromeDeep, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true; latestStart() },
                    onDragEnd = { dragging = false; latestEnd() },
                    onDragCancel = { dragging = false; latestEnd() }
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

// v342 — snap (stick) vs hint (preview) reach for the alignment guides, in
// card-local dp floats. The snap reach is HALF the old 6 dp so boxes only
// grab a guide when the user is actually aiming at it; the hint band keeps
// the line visible slightly earlier so alignment is still discoverable.
private const val SNAP_REACH = 3f
private const val HINT_REACH = 8f

/** v353 — one-axis alignment helper (NO magnet). The drag position is always
 *  applied as-is — the box follows the finger exactly, nothing sticks. The
 *  helper only watches whether the element ALIGNS with the card's edges /
 *  centre or with another element's edges / centre (the [extra] candidates):
 *  when a candidate sits within the [hint] band it reports a faint guide
 *  line so the user still SEES the alignment, without any pull. */
private fun magnetAxis(
    base: Float, size: Float, cardSize: Float,
    min: Float, max: Float, cur: Float,
    snap: Float, hint: Float,
    extra: List<Pair<Float, Float>> = emptyList()
): AxisSnap {
    var best: Pair<Float, Float>? = null
    var bestDist = hint
    // (offset that lands the element on the line, card-local line position)
    val candidates = listOf(
        -base to 0f,
        cardSize / 2f - base - size / 2f to cardSize / 2f,
        cardSize - base - size to cardSize
    ) + extra
    for ((cand, line) in candidates) {
        if (cand < min - 0.01f || cand > max + 0.01f) continue
        val d = kotlin.math.abs(cand - cur)
        if (d <= bestDist) { bestDist = d; best = cand to line }
    }
    val b = best ?: return AxisSnap(cur)
    // v353 — NEVER snap: offsets stay exactly where the finger put them.
    // The line only shows as a faint hint inside the band.
    return AxisSnap(offset = cur, hintLine = b.second)
}

/** v342/v353 — one-axis alignment result: the offset to apply (always the
 *  drag position — no snap anymore) and the faint hint line (card-local dp,
 *  null when there is none). The bright snapped line is never emitted now. */
private class AxisSnap(val offset: Float, val snapLine: Float? = null, val hintLine: Float? = null)

/** v342 — the guide lines a live drag currently sits on, as card-local dp
 *  floats (null = not aligned on that axis). [vx]/[hy] are kept for symmetry
 *  but never set now; [hintVx]/[hintHy] are the faint near-miss hints that
 *  preview the alignment without grabbing the box. */
private class DragGuides(
    val vx: Float? = null,
    val hy: Float? = null,
    val hintVx: Float? = null,
    val hintHy: Float? = null
)

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
 * tool. Saturation is luma-weighted. The matrices multiply so the filter
 * applies saturation first, then contrast.
 * v335 — contrast pivots ASYMMETRICALLY: raising contrast (the user's
 * "make it deeper" direction) pivots near the top of the range (~0.85) so
 * the light parchment fields slide DOWN toward richer cream instead of
 * blowing out to white — the old 0.5 pivot pushed every cream tone (all
 * above 0.5) toward white, which is why the card looked washed out.
 * Lowering contrast keeps the neutral 0.5 pivot for a clean soft wash.
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
    val pivot = if (contrast >= 1f) 0.85f else 0.5f
    val t = (1f - contrast) * pivot
    val con = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, t,
        0f, contrast, 0f, 0f, t,
        0f, 0f, contrast, 0f, t,
        0f, 0f, 0f, 1f, 0f
    ))
    con.timesAssign(sat)
    return con
}

/**
 * v377/v379 — a toolbar tool cell: the 44dp icon pill. v3xx — the caption
 * text is GONE (user direction: icon-only pills, no text under the icon);
 * the caller still passes a caption only to satisfy the API — the pill's
 * contentDescription (passed through to EditToolPill) keeps the tool
 * discoverable for accessibility.
 */
@Composable
private fun ToolWithCaption(caption: String, content: @Composable () -> Unit) {
    content()
}

/** v3xx/v377 — one circular icon tool button in the edit toolbar (icons only;
 *  the [ToolWithCaption] wrapper may add its short name underneath). */
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

/** v377 — an icon-ONLY pill (no label) for the Content panel's "No fact"
 *  option: an eye-crossed glyph hides the fact box with no words. */
@Composable
private fun IconPill(icon: String, contentDesc: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = icon,
                contentDescription = contentDesc,
                size = 18.dp,
                tint = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

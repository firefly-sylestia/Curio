package com.curio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.curio.app.data.AppPreferences
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Curio's color palette.
 *
 * Warm brand foundation (coral / butter / mint / cream) plus the researched
 * category palette: Tailwind-700 harmonized accents with light 300-level
 * ink twins (see [com.curio.app.ui.theme.categoryInk]). All colors are
 * opaque; card surfaces use solid category gradients with shadow elevation
 * for depth.
 */
object CurioColors {

    // ── Warm pastel foundation ─────────────────────────────────────────
    val CoralBlush       = Color(0xFFFF8FA3)  // Soft pink — primary
    val ButterYellow     = Color(0xFFFFD97D)  // Warm butter — secondary
    // v8.33 — deep gold ink for LIGHT surfaces: ButterYellow is a pale pastel
    // that vanishes on the cream background, so bonus/rank text + icons wear
    // this readable dark gold in light mode (dark mode keeps the bright butter).
    val GoldInk          = Color(0xFFB8860B)  // Dark goldenrod — readable on light
    // v20 — deep rose ink for LIGHT surfaces: CoralBlush is a pale pastel
    // that vanishes on the cream background, so coral icons/text/progress
    // accents wear this readable deep rose in light mode (dark mode keeps
    // the bright coral).
    val CoralInk         = Color(0xFFE2556B)  // Deep rose — readable on light
    val SkyMint          = Color(0xFF8FE3CF)  // Soft mint — tertiary
    val CreamWhite       = Color(0xFFFFFBF5)  // Warm white — ink on primary/error fills + decorative accents
    val SoftCream        = Color(0xFFF7F0E4)  // Soft cream — light-mode background (user-preferred, less white)
    val SoftSand         = Color(0xFFF6EFE4)  // Warm sand — surface container
    val WarmCoralRed     = Color(0xFFE4626F)  // Soft coral-red — error
    val DeepPlum         = Color(0xFF3B0A17)  // Deep maroon — on-primary

    // ── Category accents (researched palette) ──────────────────────────
    // Tailwind-700 harmonized shades: deep enough that WHITE content clears
    // WCAG AA (>= 4.5:1) on every accent, yet vivid enough to stay rich on
    // the cream paper surface. Each deep accent pairs with a light 300-level
    // "ink" twin for accent-colored text/icons on the midnight dark surfaces
    // (resolved theme-aware via categoryInk()).
    val CategoryIndigo   = Color(0xFF4338CA)  // Music — Artists
    val CategoryAlbum    = Color(0xFF5F4DCB)  // Music — Albums (indigo twin, a touch lighter + warmer)
    val CategorySong     = Color(0xFF0E7490)  // Music — Songs (cyan — distinct from the indigo family)
    val CategoryRose     = Color(0xFFBE123C)  // Movies — Directors / Films
    val CategorySeries   = Color(0xFFBE185D)  // Movies — Series (pink-magenta, distinct from rose)
    val CategoryAmber    = Color(0xFFB45309)  // Books — Authors / Books
    val CategoryTeal     = Color(0xFF0F766E)  // Visual Art — Painters / Artworks
    val CategorySky      = Color(0xFF0369A1)  // Science — Scientists / Discoveries
    val CategoryViolet   = Color(0xFF7E22CE)  // Anime & Comics — Anime
    val CategoryManga    = Color(0xFF5B21B6)  // Anime & Comics — Manga (deeper violet)
    val CategoryManhwa   = Color(0xFF9333EA)  // Anime & Comics — Manhwa (brighter orchid purple)
    val CategoryFuchsia  = Color(0xFFA21CAF)  // Games
    val CategoryOrange   = Color(0xFFC2410C)  // Mythology
    val CategoryEmerald  = Color(0xFF047857)  // Sports
    val CategoryRed      = Color(0xFFB91C1C)  // Food
    val CategoryBlue     = Color(0xFF1D4ED8)  // Internet culture
    val CategoryCoral    = CoralBlush  // Wildcard — the app's brand primary, not a deep accent
    // v27i — deep accents for the 15 new lanes
    val CategoryGreen       = Color(0xFF15803D)  // Biology
    val CategoryLime        = Color(0xFF4D7C0F)  // Chemistry
    val CategoryBrown       = Color(0xFF78350F)  // Animals
    val CategoryForest      = Color(0xFF065F46)  // Plants
    val CategorySlate       = Color(0xFF334155)  // Technologies
    val CategoryNavy        = Color(0xFF1E3A8A)  // Astronomy
    val CategorySepia       = Color(0xFF854D0E)  // History
    val CategoryStone       = Color(0xFF57534E)  // Geology
    val CategoryCrimson     = Color(0xFF9F1239)  // Medicine
    val CategoryPeriwinkle  = Color(0xFF6D28D9)  // Psychology
    val CategoryIndigoBlue  = Color(0xFF1E40AF)  // Mathematics
    val CategoryGold        = Color(0xFFA16207)  // Economics
    val CategoryTeal600     = Color(0xFF0D9488)  // Language
    val CategoryZinc        = Color(0xFF3F3F46)  // Engineering
    val CategoryDeepCyan    = Color(0xFF0C4A6E)  // Oceans

    /** Light 300-level twins for accent-colored ink on dark surfaces. */
    val CategoryIndigoInk = Color(0xFFA5B4FC)
    val CategoryAlbumInk  = Color(0xFFA5B4FC)  // albums share the indigo twin — family coherence
    val CategorySongInk   = Color(0xFF67E8F9)  // cyan-300
    val CategoryRoseInk   = Color(0xFFFDA4AF)
    val CategorySeriesInk = Color(0xFFF9A8D4)  // pink-300
    val CategoryAmberInk  = Color(0xFFFCD34D)
    val CategoryTealInk   = Color(0xFF5EEAD4)
    val CategorySkyInk    = Color(0xFF7DD3FC)
    val CategoryVioletInk = Color(0xFFC4B5FD)
    val CategoryMangaInk  = Color(0xFFA78BFA)  // violet-400
    val CategoryManhwaInk = Color(0xFFD8B4FE)  // purple-300
    val CategoryFuchsiaInk = Color(0xFFF0ABFC)
    val CategoryEmeraldInk = Color(0xFF6EE7B7)
    val CategoryOrangeInk  = Color(0xFFFDBA74)
    val CategoryRedInk     = Color(0xFFFCA5A5)
    val CategoryBlueInk    = Color(0xFF93C5FD)
    val CategoryCoralInk  = Color(0xFFFFC2CE)  // light coral twin for dark-surface ink
    // v27i — the 15 new lanes (Tailwind-700 accents + 300-level ink twins)
    val CategoryGreenInk     = Color(0xFF86EFAC)  // Biology
    val CategoryLimeInk      = Color(0xFFBEF264)  // Chemistry
    val CategoryBrownInk     = Color(0xFFFDE68A)  // Animals
    val CategoryForestInk    = Color(0xFF6EE7B7)  // Plants
    val CategorySlateInk     = Color(0xFFCBD5E1)  // Technologies
    val CategoryNavyInk      = Color(0xFF93C5FD)  // Astronomy
    val CategorySepiaInk     = Color(0xFFFDE68A)  // History
    val CategoryStoneInk     = Color(0xFFD6D3D1)  // Geology
    val CategoryCrimsonInk   = Color(0xFFFDA4AF)  // Medicine
    val CategoryPeriwinkleInk = Color(0xFFC4B5FD) // Psychology
    val CategoryIndigoBlueInk = Color(0xFFA5B4FC) // Mathematics
    val CategoryGoldInk      = Color(0xFFFDE047)  // Economics
    val CategoryTeal600Ink   = Color(0xFF5EEAD4)  // Language
    val CategoryZincInk      = Color(0xFFD4D4D8)  // Engineering
    val CategoryDeepCyanInk  = Color(0xFF7DD3FC)  // Oceans

    /** Tinted (20% alpha) washes of the researched category accents. */
    val CategoryIndigoTint = CategoryIndigo.copy(alpha = 0.20f)
    val CategoryAlbumTint  = CategoryAlbum.copy(alpha = 0.20f)
    val CategorySongTint   = CategorySong.copy(alpha = 0.20f)
    val CategoryRoseTint   = CategoryRose.copy(alpha = 0.20f)
    val CategorySeriesTint = CategorySeries.copy(alpha = 0.20f)
    val CategoryAmberTint  = CategoryAmber.copy(alpha = 0.20f)
    val CategoryTealTint   = CategoryTeal.copy(alpha = 0.20f)
    val CategorySkyTint    = CategorySky.copy(alpha = 0.20f)
    val CategoryVioletTint = CategoryViolet.copy(alpha = 0.20f)
    val CategoryMangaTint  = CategoryManga.copy(alpha = 0.20f)
    val CategoryManhwaTint = CategoryManhwa.copy(alpha = 0.20f)
    val CategoryFuchsiaTint = CategoryFuchsia.copy(alpha = 0.20f)
    val CategoryEmeraldTint = CategoryEmerald.copy(alpha = 0.20f)
    val CategoryOrangeTint  = CategoryOrange.copy(alpha = 0.20f)
    val CategoryRedTint     = CategoryRed.copy(alpha = 0.20f)
    val CategoryBlueTint    = CategoryBlue.copy(alpha = 0.20f)
    val CategoryCoralTint  = CategoryCoral.copy(alpha = 0.20f)
    // v27i — tint washes for the 15 new lanes
    val CategoryGreenTint     = CategoryGreen.copy(alpha = 0.20f)
    val CategoryLimeTint      = CategoryLime.copy(alpha = 0.20f)
    val CategoryBrownTint     = CategoryBrown.copy(alpha = 0.20f)
    val CategoryForestTint    = CategoryForest.copy(alpha = 0.20f)
    val CategorySlateTint     = CategorySlate.copy(alpha = 0.20f)
    val CategoryNavyTint      = CategoryNavy.copy(alpha = 0.20f)
    val CategorySepiaTint     = CategorySepia.copy(alpha = 0.20f)
    val CategoryStoneTint     = CategoryStone.copy(alpha = 0.20f)
    val CategoryCrimsonTint   = CategoryCrimson.copy(alpha = 0.20f)
    val CategoryPeriwinkleTint = CategoryPeriwinkle.copy(alpha = 0.20f)
    val CategoryIndigoBlueTint = CategoryIndigoBlue.copy(alpha = 0.20f)
    val CategoryGoldTint      = CategoryGold.copy(alpha = 0.20f)
    val CategoryTeal600Tint   = CategoryTeal600.copy(alpha = 0.20f)
    val CategoryZincTint      = CategoryZinc.copy(alpha = 0.20f)
    val CategoryDeepCyanTint  = CategoryDeepCyan.copy(alpha = 0.20f)

    /**
     * Legacy warm pastels — retained ONLY for brand/decorative use
     * (profile stat icons, wildcard rainbow gradient). Categories now use
     * the researched [CategoryIndigo]..[CategorySky] tokens above plus the
     * brand-primary [CategoryCoral] used by the Wildcard.
     */
    val Lilac            = Color(0xFFC9A6F2)  // legacy soft purple
    val DustyBlue        = Color(0xFF9BB8E8)  // legacy soft blue
    val Sage             = Color(0xFFA8C99A)  // legacy soft green
    // v20 — deep sage ink for LIGHT surfaces: Sage is a pale pastel that
    // vanishes on the cream background, so "done"/mastered icons + text
    // wear this readable deep green in light mode (dark keeps the soft sage).
    val SageInk          = Color(0xFF55803F)  // Deep fern — readable on light
    val Peach            = Color(0xFFFFB585)  // legacy soft orange
    val Teal             = Color(0xFF6FC7BE)  // legacy soft teal

    /** Vivid fire orange — the streak accent (flame icon) on coral heroes. */
    val FireOrange       = Color(0xFFFF8A00)

    /**
     * Soft dusty rose — the Home quest hero's banner accent. A calm,
     * beautiful rose (v7.36 — moved off the brownish terracotta): a gentle
     * rose hue lifted to an airy lightness, so the hero reads soft and
     * pretty instead of earthy brown, while the dark maroon ink stays
     * readable on it in non-pastel mode. Pastel mode (default) resolves
     * its airy pink pastel twin via [pastelAccent] — see HomeScreen's
     * hero fill.
     */
    val HomeRosewood     = Color(0xFFCF8B94)
    /** v81 — the dark-mode torn-hero twin: the same rosewood hue at a deep,
     *  slightly desaturated lightness (a NEW SHADE of the same spectrum). */
    val HomeRosewoodDark = Color(0xFF6E3A44)
    // v27l — the optional sky-azure hero variant: a brighter, fresher azure
    // (the Science/Sky hue at sky-300-ish saturation, held a touch softer
    // than neon).
    val HomeAzure        = Color(0xFF9ED2EF)
    /** v81 — the dark-mode azure hero twin (same hue, deep shade). */
    val HomeAzureDark    = Color(0xFF2F5163)

    /** Tinted (20% alpha) versions of the legacy accents for backgrounds. */
    val LilacTint     = Lilac.copy(alpha = 0.20f)
    val DustyBlueTint = DustyBlue.copy(alpha = 0.20f)
    val SageTint      = Sage.copy(alpha = 0.20f)
    val PeachTint     = Peach.copy(alpha = 0.20f)
    val TealTint      = Teal.copy(alpha = 0.20f)

    /**
     * Warm taupe-gray watermark ink for the light surface. The onSurface
     * maroon reads muddy at watermark sizes over cream, so the backdrop
     * uses this instead in light mode (drawn at ~16% alpha). Dark mode
     * keeps the near-white onSurface ghosts.
     */
    val WarmWatermarkInk = Color(0xFF8E8177)
}

/**
 * HSL components of a color, computed from its RGBA channels. Internal
 * (shared by [CurioMixedDeck]'s premium blends, [CurioGradients.hslGradientStops],
 * and the pastel-mode ink helpers in CategoryInk.kt — [pastelFillInk], [deepHueInk]).
 */
internal data class Hsl(val h: Float, val s: Float, val l: Float)

/** Standard RGB → HSL conversion (channels in [0,1], hue in degrees). */
internal fun toHsl(color: Color): Hsl {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    val s = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2f * l - 1f))
    val h = when {
        d == 0f -> 0f
        max == r -> ((g - b) / d) % 6f
        max == g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } * 60f
    return Hsl((h + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

/** Standard HSL → RGB conversion (hue in degrees, s/l in [0,1]). */
internal fun fromHsl(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = h / 60f
    val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
    val (r, g, b) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(r + m, g + m, b + m)
}

/**
 * OKLab coordinates (Björn Ottosson) — a perceptually uniform L*a*b-style
 * space. Used for gradient / blend interpolation: RGB and HSL interpolation
 * pass through muddy foreign-hue bands (grey for RGB, the olive dead zone
 * for amber↔teal in HSL), while OKLab's cube-root-nonlinearity keeps hue
 * and lightness perceptually even along the whole path. v87 — the dark-mode
 * mixed-deck gradient research (OKLab is the interpolation gold standard:
 * Photoshop's default for gradients, CSS Color 4 `color-mix(in oklab)`, the
 * Tailwind migration issue) landed this local implementation.
 */
internal data class Oklab(val l: Float, val a: Float, val b: Float)

/** sRGB (gamma) → OKLab, via the canonical Ottosson matrices. */
internal fun toOklab(color: Color): Oklab {
    val r = srgbToLinear(color.red)
    val g = srgbToLinear(color.green)
    val b = srgbToLinear(color.blue)
    // Linear sRGB → LMS (M1)
    val l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
    val m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
    val s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b
    // Cube-root non-linearity
    val l1 = cbrt(l)
    val m1 = cbrt(m)
    val s1 = cbrt(s)
    // LMS → OKLab (M2)
    return Oklab(
        0.2104542553f * l1 + 0.7936177850f * m1 - 0.0040720468f * s1,
        1.9779984951f * l1 - 2.4285922050f * m1 + 0.4505937099f * s1,
        0.0259040371f * l1 + 0.7827717662f * m1 - 0.8086757660f * s1
    )
}

/** OKLab → sRGB (gamma), clamped to the displayable gamut. */
internal fun fromOklab(lab: Oklab): Color {
    // OKLab → LMS'
    val l1 = lab.l + 0.3963377774f * lab.a + 0.2158037573f * lab.b
    val m1 = lab.l - 0.1055613458f * lab.a - 0.0638541728f * lab.b
    val s1 = lab.l - 0.0894841775f * lab.a - 1.2914855480f * lab.b
    // Cube
    val l = l1 * l1 * l1
    val m = m1 * m1 * m1
    val s = s1 * s1 * s1
    // LMS → linear sRGB (M1⁻¹)
    val r = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
    val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
    val b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    return Color(
        linearToSrgb(r).coerceIn(0f, 1f),
        linearToSrgb(g).coerceIn(0f, 1f),
        linearToSrgb(b).coerceIn(0f, 1f)
    )
}

/** sRGB gamma channel → linear light. */
private fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

/** Linear light → sRGB gamma channel. */
private fun linearToSrgb(c: Float): Float =
    if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f

/**
 * Perceptual midpoint of two colors in OKLab — the premium blend fallback
 * for unmapped mixed-deck pairs (replaces the HSL midpoint, which could
 * swing through foreign hues).
 */
internal fun oklabBlend(a: Color, b: Color): Color {
    val oa = toOklab(a)
    val ob = toOklab(b)
    return fromOklab(Oklab((oa.l + ob.l) / 2f, (oa.a + ob.a) / 2f, (oa.b + ob.b) / 2f))
}

/**
 * Order-independent perceptual mean of several colors in OKLab — the
 * premium 4+ accent blend (replaces the HSL circular-hue mean, whose
 * lightness averaging was non-perceptual).
 */
internal fun oklabCentroid(colors: List<Color>): Color {
    val oks = colors.map { toOklab(it) }
    val n = oks.size.toFloat()
    return fromOklab(
        Oklab(
            oks.sumOf { it.l.toDouble() }.toFloat() / n,
            oks.sumOf { it.a.toDouble() }.toFloat() / n,
            oks.sumOf { it.b.toDouble() }.toFloat() / n
        )
    )
}

/**
 * Evenly-spaced gradient stops between [from] and [to] interpolated in OKLab
 * (including both endpoints) — the perceptually-smooth replacement for
 * [CurioGradients.hslGradientStops]. RGB/HSL interpolation between a deep
 * accent and black/white passes through muddy grey or swings hue; OKLab
 * keeps lightness + hue even along the whole path, so dark-mode fades to
 * black hold their hue instead of banding.
 */
internal fun oklabGradientStops(from: Color, to: Color, steps: Int = 9): List<Color> {
    require(steps >= 2)
    val a = toOklab(from)
    val b = toOklab(to)
    return List(steps) { i ->
        val t = i / (steps - 1).toFloat()
        fromOklab(
            Oklab(
                a.l + (b.l - a.l) * t,
                a.a + (b.a - a.a) * t,
                a.b + (b.b - a.b) * t
            )
        )
    }
}

/**
 * Hue-preserving light tint of a category accent, for LIGHT-mode page washes
 * and tinted surfaces.
 *
 * The old RGB recipe (cream blended with a splash of the accent) let the
 * cream's warm hue dominate the mix, so cool accents drifted off-family:
 * teal and sky washes turned grey-GREEN and the detail hero's red glide
 * passed through a yellow band before settling on the wash. Building the
 * tint from the accent's OWN hue in HSL keeps every shade on the accent's
 * hue family, so the hero's accent → wash fade stays on-hue (deep teal →
 * pale teal, sky → pale azure, red → rose) with no foreign-color band.
 *
 * The defaults were raised from the original (0.22/0.88) so the pastel
 * actually READS as its category color: at the old values red/teal/sky
 * washes all landed within a few RGB points of each other (near-white
 * beige), so a Movies page, a Visual Art page and a Science page looked
 * the same pale wash and the detail hero's blend melted into it instead
 * of into the category color. (0.32/0.85) keeps each family visibly
 * distinct — rose, mint, azure — while staying light enough for the dark
 * maroon ink on top (≥ 10.8:1 contrast everywhere).
 *
 * @param saturation Saturation of the pastel tint (0..1).
 * @param lightness  Lightness of the pastel tint (0..1) — airy like the
 *   cream paper, so tinted pages stay light for the maroon ink on top.
 */
internal fun lightAccentTint(
    accent: Color,
    saturation: Float = 0.32f,
    lightness: Float = 0.85f
): Color {
    val a = toHsl(accent)
    return fromHsl(a.h, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
}

/**
 * Pastel twin of a category accent for the Pastel color mode (v7.5).
 *
 * Light mode: an AIRY pastel — the accent's own hue at high lightness with
 * lightly-held saturation, so indigo becomes periwinkle, rose soft pink,
 * teal mint, sky azure. Fills wearing this read with the DEEP accent as ink
 * ([com.curio.app.ui.theme.CurioCategory.onAccent]).
 *
 * Dark mode: a MUTED DEEP pastel — desaturated and pulled down in lightness
 * so the soft look stays understated over the midnight surface (per user
 * direction) while light ink stays readable on top.
 */
internal fun pastelAccent(accent: Color, dark: Boolean): Color {
    val a = toHsl(accent)
    return if (dark) {
        // v7.5 — muted deep pastel: desaturated, gently deepened so the
        // soft look stays understated over midnight while light ink reads.
        fromHsl(a.h, (a.s * 0.55f).coerceIn(0f, 0.55f), 0.42f)
    } else {
        // v7.36 — airy pastel, softened: the accent's own hue at high
        // lightness with a LOWER saturation hold so the brightest families
        // (teal, mint, sky — and the green-heavy mixed-deck blends) read
        // calm and pretty instead of neon-bright. Indigo still becomes
        // periwinkle, rose soft pink, teal a gentle mint, sky soft azure.
        fromHsl(a.h, (a.s * 0.70f).coerceIn(0f, 0.60f), 0.80f)
    }
}

/**
 * Solid gradient definitions for card surfaces. Every card gradient opens on
 * the same deepened accent used by the flat category cards ([categoryCardFill])
 * and fades toward the active theme's background — white in light mode, black
 * in dark — so cards always echo the app surface behind them.
 */
object CurioGradients {
    /** Warm sunset spectrum for the Wildcard — cohesive with the brand palette (decorative use only). */
    val WildcardGradientStops = listOf(
        CurioColors.CoralBlush,
        CurioColors.Peach,
        CurioColors.ButterYellow
    )

    /**
     * The flat fill used on category cards/chips — the same color every card
     * gradient opens on, so tiles and big cards can never drift apart. A
     * shallow deepen toward black keeps the hue rich while softening
     * brightness for the full-width tile treatment. Non-pastel dark mode uses
     * a deeper 28% treatment so hero cards do not glow against midnight.
     *
     * v7.8.1 — pastel mode keeps the PURE pastel accent (no black deepen):
     * the 10% deepen on an already-airy pastel dulled the fill and made the
     * pastel deck read dimmer than it should (especially the shuffle main
     * card). The pastel accent is already soft enough for white-free ink.
     */
    fun categoryCardFill(accent: Color): Color = when {
        AppPreferences.pastelColorsState -> accent
        else -> lerp(accent, Color.Black, 0.10f)
    }

    /**
     * Interpolates [from] → [to] in HSL space (shortest hue path) and
     * returns [steps] evenly-spaced colors INCLUDING both endpoints. Naive
     * RGB lerp between a deep accent and a light/dark page wash passes
     * through muddy grey midtones; HSL keeps the hue saturated along the
     * whole path, so gradient blends glide through proper blended colors
     * instead of a grey band.
     */
    fun hslGradientStops(from: Color, to: Color, steps: Int = 9): List<Color> {
        require(steps >= 2)
        val a = toHsl(from)
        val b = toHsl(to)
        // Achromatic endpoints (pure black/white/grey) carry no meaningful
        // hue — anchor the path on the chromatic endpoint's hue so the
        // blend simply darkens/lightens instead of swinging through foreign
        // hues (e.g. a deep accent fading to AMOLED black stays on-hue).
        val hueFrom = if (a.s <= 0.001f) b.h else a.h
        val hueTo = if (b.s <= 0.001f) hueFrom else b.h
        var dh = hueTo - hueFrom
        if (dh > 180f) dh -= 360f
        if (dh < -180f) dh += 360f
        return List(steps) { i ->
            val t = i / (steps - 1).toFloat()
            fromHsl(
                (hueFrom + dh * t + 360f) % 360f,
                (a.s + (b.s - a.s) * t).coerceIn(0f, 1f),
                (a.l + (b.l - a.l) * t).coerceIn(0f, 1f)
            )
        }
    }

    /**
     * Theme-aware category card gradient: opens on [categoryCardFill] (the
     * category card color) and softens toward the theme surface — the soft
     * cream background in light mode, black in dark — so the card background
     * always matches the app's background shade (the hero card must not
     * wash out to pure white on the cream surface).
     *
     * v12 — the Material style wears the SAME rich category gradient as the
     * rest of the app (the old device-color + faint category "whisper" cards
     * read muddy and grey). The Material identity lives in the hue-locked
     * scheme surfaces and heroes, not in desaturated cards.
     */
    @Composable
    fun cardGradient(accent: Color): List<Color> {
        // v9.x — AMOLED cards are PROPER pitch black now: a pure black base
        // (was the surfaceContainerHigh grey) with only a quiet category-
        // color bloom, and the card edge carries the black-glass shine (see
        // Modifier.categoryEdgeShine). Power-friendly and coherent everywhere.
        // v78 — the AMOLED early-return is gone with dark mode. End on the
        // active theme's background so cards always echo the surface behind
        // them (cream). v25 — Pastel crown depth PASSED: always ON, so its
        // toggle was removed from Experiments and the pastel crown read is
        // fixed.
        // v185 — Material theme: the muted family fill over the NEUTRAL
        // scheme background (M3: surfaces stay neutral, one restrained
        // accent family per lane — no vivid per-lane gradients).
        if (materialThemeOn) {
            val dark = isCurioDarkTheme()
            val familyStart = accent.materialAccentFor(dark)
            val start = lerp(familyStart, Color.Black, if (dark) 0.08f else 0.03f)
            val end = MaterialTheme.colorScheme.background
            return listOf(start, lerp(start, end, 0.30f))
        }
        val start = if (AppPreferences.pastelColorsState) {
            // v7.12 — subtle 5% black deepen at the very top of pastel
            // gradients so every pastel card reads with a gentle darker
            // crown instead of a uniform pastel from edge to edge.
            lerp(categoryCardFill(accent), Color.Black, 0.05f)
        } else {
            categoryCardFill(accent)
        }
        // v81 — dark mode: the card melts into the PITCH-BLACK page (no
        // tint wash in dark), so the gradient ends on the black background
        // with the dark accent crown on top.
        val end = if (isCurioDarkTheme()) {
            MaterialTheme.colorScheme.background
        } else if (AppPreferences.tintWashEffective()) {
            // v7.8 — on tint-washed Curio pages the card melts into the
            // washed background on the category's OWN hue (same recipe as
            // the page wash), not the raw cream that dragged cool accents
            // off-family.
            if (AppPreferences.pastelColorsState) lightAccentTint(accent, saturation = 0.22f, lightness = 0.80f)
            else lightAccentTint(accent)
        } else {
            MaterialTheme.colorScheme.background
        }
        return listOf(start, lerp(start, end, 0.30f))
    }

    /**
     * v10 — Dual-accent blend hero gradient: the category accent meets a
     * warm golden companion in HSL space for a richer, more sophisticated
     * multi-tone gradient. The blend creates a premium duotone effect —
     * accent at the top melting into warm gold at the bottom — that reads
     * beautifully across all theme styles.
     *
     * The companion is a warm golden amber (hue ~42°, saturation ~0.85)
     * that complements every researched accent without clashing: indigo →
     * gold reads royal, rose → gold reads cinematic, teal → gold reads
     * luxurious, sky → gold reads sunrise, amber → deepened gold reads
     * cohesive, and coral → gold reads warm-fire. In dark/AMOLED modes the
     * companion deepens; in pastel mode it softens.
     */
    @Composable
    fun heroBlendGradient(accent: Color): List<Color> {
        // v185 — Material theme: a quiet two-tone family gradient over the
        // neutral scheme background (no golden companion, no vivid crown).
        if (materialThemeOn) {
            val dark = isCurioDarkTheme()
            val family = accent.materialAccentFor(dark)
            val crown = lerp(family, Color.White, if (dark) 0.05f else 0.10f)
            return listOf(crown, family, MaterialTheme.colorScheme.background)
        }
        val pastel = AppPreferences.pastelColorsState
        val dark = isCurioDarkTheme()

        // v81 — dark: the golden companion deepens (the old dark behavior)
        // so the duotone reads rich on black instead of glowing neon.
        val companionBase = when {
            dark && pastel -> lerp(CurioColors.ButterYellow, Color.Black, 0.30f)
            dark -> lerp(CurioColors.GoldInk, Color.Black, 0.25f)
            pastel -> CurioColors.ButterYellow
            else -> CurioColors.GoldInk
        }

        // Top crown — a whisper of light at the very top for a premium
        // lit-surface feel. Dark: a softer white whisper so the dark crown
        // never washes out.
        val crown = if (dark) lerp(accent, Color.White, 0.08f)
        else if (pastel) lerp(accent, Color.White, 0.10f)
        else lerp(accent, Color.White, 0.16f)

        // The accent stop — the card fill (theme-aware), already pastel in
        // pastel mode and already dark in dark mode.
        val accentStop = when {
            dark -> lerp(categoryCardFill(accent), Color.Black, 0.06f)
            pastel && AppPreferences.pastelCrownDepthState -> lerp(categoryCardFill(accent), Color.Black, 0.05f)
            else -> categoryCardFill(accent)
        }

        return listOf(crown, accentStop, companionBase)
    }
}

/**
 * Mixed-deck color system for multi-category spins.
 *
 * When a user selects several categories in the picker, the deck no longer
 * speaks with the first category's accent — it blends all the chosen accents
 * into one premium color story:
 *
 *  - **Peek cards + spin button + confetti** use [mixedDeckAccent]: a curated
 *    blend of every selected accent (deduped), so the deck visibly mixes the
 *    user's picks.
 *  - **Hero card** uses [mixedDeckGradient]: a multi-accent gradient across
 *    the selected colors (Spotify/duotone-style), or the standard theme-aware
 *    single-accent gradient when only one distinct category is active.
 *
 * Every pair and triple blend below was verified against a canonical HSL
 * computation: pairs are shortest-hue-path midpoints with a saturation boost
 * (naive RGB/RGB-lerp midpoints pass through muddy gray, and teal↔amber /
 * sky↔amber cross the olive-green dead zone, so those are deliberately
 * steered to a richer jade/teal instead); triples are the order-independent
 * HSL centroid of the three accents. Four+ accents use the runtime
 * [oklabCentroid] — the perceptually-uniform mean (v87 — replaces the old
 * HSL circular-hue centroid, whose numeric lightness averaging swung
 * through foreign hues).
 *
 * v114 — the blends are VIVID by design, NOT white-contrast deepened: the
 * deck cards never put crisp white directly on the raw blend — the peeks
 * deepen each stop per-card (HSL lightness drop for the reel hierarchy),
 * the spin button / peek ink is the same-hue deep ink
 * ([pastelFillInk]'s light branch), and the hero's white ink rides the
 * theme-resolved gradient like every single-category deck. The earlier
 * "deepen every blend until it clears 4.5:1 against white" rule produced
 * near-black mud (Rose+Teal sat at ~11% lightness) — the green/teal,
 * magenta/purple and blue mixes were retuned to vivid, clean mid-tones.
 */
object CurioMixedDeck {

    // v114 — the green/teal, magenta/purple and blue mixes were retuned
    // from near-black "white-contrast deepened" blends (0xFF4A12A8 sat at
    // ~11% lightness — mud) to VIVID, clean mid-tones in the same hue
    // families. The deck cards never needed the white-contrast deepening:
    // the peeks deepen each stop per-card (HSL lightness drop) and the
    // spin button / peek ink is the same-hue deep ink, so vivid blends read
    // bright and premium like the red family (which was never over-darkened
    // and was the family the user liked).
    private val PairBlends: Map<Set<Color>, Color> = mapOf(
        // Indigo family mixes — violet, pink, azure, blue
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose)  to Color(0xFF8B5CF6),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryAmber) to Color(0xFFDB2777),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryTeal)  to Color(0xFF2563EB),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategorySky)   to Color(0xFF1D4ED8),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryCoral) to Color(0xFFC026D3),
        // Rose family mixes — ember, violet, violet-blue, blush
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryAmber) to Color(0xFFBF1E14),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryTeal)  to Color(0xFF6D28D9),
        setOf(CurioColors.CategoryRose,   CurioColors.CategorySky)   to Color(0xFF7C3AED),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryCoral) to Color(0xFFEA1142),
        // Amber mixes — ember, vivid jade (off the olive dead zone), flame
        setOf(CurioColors.CategoryAmber,  CurioColors.CategoryTeal)  to Color(0xFF0BA36D),
        setOf(CurioColors.CategoryAmber,  CurioColors.CategorySky)   to Color(0xFF0FA3A3),
        setOf(CurioColors.CategoryAmber,  CurioColors.CategoryCoral) to Color(0xFFE32D0F),
        // Teal / Sky / Coral family mixes
        setOf(CurioColors.CategoryTeal,   CurioColors.CategorySky)   to Color(0xFF0A9CB8),
        setOf(CurioColors.CategoryTeal,   CurioColors.CategoryCoral) to Color(0xFF8B5CF6),
        setOf(CurioColors.CategorySky,    CurioColors.CategoryCoral) to Color(0xFF9333EA)
    )

    private val TripleBlends: Map<Set<Color>, Color> = mapOf(
        // Indigo-anchored triples — pink, periwinkle, violet-blue, azure
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose,  CurioColors.CategoryAmber) to Color(0xFFDB2777),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose,  CurioColors.CategoryTeal)  to Color(0xFF7C6CF0),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose,  CurioColors.CategorySky)   to Color(0xFF8B5CF6),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose,  CurioColors.CategoryCoral) to Color(0xFFDB2777),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryAmber, CurioColors.CategoryTeal)  to Color(0xFF2563EB),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryAmber, CurioColors.CategorySky)   to Color(0xFF6D5EF0),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryAmber, CurioColors.CategoryCoral) to Color(0xFFE11D48),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryTeal,  CurioColors.CategorySky)   to Color(0xFF0B8BD0),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryTeal,  CurioColors.CategoryCoral) to Color(0xFF7C6CF0),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategorySky,   CurioColors.CategoryCoral) to Color(0xFF8158F6),
        // Rose-anchored triples — burnt orange, crimson, azure, pink, fuchsia
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryAmber, CurioColors.CategoryTeal)  to Color(0xFFD4450D),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryAmber, CurioColors.CategorySky)   to Color(0xFFEC0630),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryAmber, CurioColors.CategoryCoral) to Color(0xFFEE0505),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryTeal,  CurioColors.CategorySky)   to Color(0xFF1D6FE0),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryTeal,  CurioColors.CategoryCoral) to Color(0xFFDB2777),
        setOf(CurioColors.CategoryRose,   CurioColors.CategorySky,   CurioColors.CategoryCoral) to Color(0xFFC026D3),
        // Amber / Teal / Sky triples — vivid teal, ember, crimson, azure
        setOf(CurioColors.CategoryAmber,  CurioColors.CategoryTeal,  CurioColors.CategorySky)   to Color(0xFF0D9488),
        setOf(CurioColors.CategoryAmber,  CurioColors.CategoryTeal,  CurioColors.CategoryCoral) to Color(0xFFCF4B06),
        setOf(CurioColors.CategoryAmber,  CurioColors.CategorySky,   CurioColors.CategoryCoral) to Color(0xFFEE001A),
        setOf(CurioColors.CategoryTeal,   CurioColors.CategorySky,   CurioColors.CategoryCoral) to Color(0xFF1D6FE0)
    )

    /**
     * The single blend color a mixed deck wears on peeks, the spin button and
     * confetti. Takes the RAW category accents (not theme-resolved) so the
     * curated pair/triple tables — keyed on the raw researched accents — hit
     * in every theme. A single distinct accent resolves to its theme-aware
     * shade; pairs and triples use the curated tables (perceptual OKLab
     * fallback); four+ use the order-independent [oklabCentroid], so every
     * mix stays vivid, white-label safe, and never depends on selection order.
     *
     * v87 — dark-mode mixed colors: the OLD code had the caller pre-resolve
     * every accent to its dark shade, so the table keys never matched and
     * every dark mix silently fell back to the HSL midpoint (foreign-hue
     * swings, muddy olive midpoints). Now the blend is computed from the RAW
     * accents (table hits restored) and then re-shaded per theme — dark
     * blends wear the same [darkAccent] "new shade of the same spectrum"
     * recipe as the single accents, so every stop of a dark mixed deck reads
     * as one family instead of riding light-designed deep blends.
     *
     * Pastel color mode (v7.5): pass `pastel = true` + the active dark state
     * so the DEEP curated pair/triple blends soften to the theme-aware pastel
     * twin ([pastelAccent]) — airy pastels in light mode, muted deep pastels
     * in dark.
     */
    fun mixedDeckAccent(accents: List<Color>, pastel: Boolean = false, dark: Boolean = false): Color {
        // v185 — Material theme: every lane collapses to its muted M3 family
        // and the mix blends the FAMILY tones (perceptual centroid), so a
        // multi-category deck reads as restrained M3 tones, never a vivid
        // rainbow.
        if (materialThemeOn) {
            val d = accents.distinct()
            if (d.isEmpty()) return CurioColors.CategoryCoral
            val families = d.map { materialFamilyFor(it) }.distinct()
            if (families.size == 1) return families.first().fill(dark)
            return oklabCentroid(families.map { it.fill(dark) })
        }
        // Color is a value class — value-based equality means distinct() alone
        // dedupes (toArgb() isn't part of the Compose BOM resolved here).
        val distinct = accents.distinct()
        val raw = when (distinct.size) {
            0 -> CurioColors.CategoryCoral
            1 -> distinct.first()
            2 -> PairBlends[distinct.toSet()] ?: oklabBlend(distinct[0], distinct[1])
            3 -> TripleBlends[distinct.toSet()] ?: oklabCentroid(distinct)
            else -> oklabCentroid(distinct)
        }
        // v87 — resolve the blend per theme (the old code re-softened only
        // pairs/triples in pastel and left dark blends completely untouched).
        return if (!pastel) (if (dark) darkAccent(raw) else raw) else pastelAccent(raw, dark)
    }

    /**
     * Hero-card gradient stops. Single accent → the standard theme-aware
     * [CurioGradients.cardGradient]. Two+ accents → a SMOOTH multi-accent
     * sweep: each RAW accent is theme-resolved, then fine OKLab steps are
     * interpolated between consecutive accents so the hero's diagonal /
     * radial / reversed-diagonal brush (picked per-deck by
     * [mixedDeckHeroBrush]) glides through the hue story with NO band lines.
     * The old accent→seam→accent stop list (a handful of saturated stops
     * with the curated pair blends between them) painted visible STRIPES
     * across the hero card — "gradients with lines". v114 replaces the seams
     * with ~7 fine OKLab steps between accents (perceptual — no muddy RGB
     * midpoints, and teal↔amber / sky↔amber never cross the olive dead
     * zone), so the sweep reads as a premium duotone→multi glide. Supports
     * up to four accents — beyond that a single sweep slides into rainbow
     * regardless of interpolation.
     *
     * v87 — dark-mode mixed gradients: every stop wears the theme shade
     * ([darkAccent] at night, [pastelAccent] in pastel mode) — one coherent
     * color family per deck, no light seams on dark cards.
     */
    @Composable
    fun mixedDeckGradient(accents: List<Color>): List<Color> {
        // Color is a value class — value-based equality means distinct() alone
        // dedupes (toArgb() isn't part of the Compose BOM resolved here).
        val distinct = accents.distinct()
        // v185 — Material theme: blend the muted FAMILY tones (no vivid
        // multi-accent sweep — M3 restraint).
        if (materialThemeOn) {
            val dark = isCurioDarkTheme()
            if (distinct.size <= 1) {
                return CurioGradients.cardGradient(mixedDeckAccent(distinct, dark = dark))
            }
            val fam = distinct.map { materialFamilyFor(it) }.distinct().take(4)
            val stops = mutableListOf<Color>()
            fam.forEachIndexed { i, f ->
                if (i == 0) stops.add(f.fill(dark))
                if (i < fam.size - 1) {
                    stops.addAll(oklabGradientStops(f.fill(dark), fam[i + 1].fill(dark), steps = 7).drop(1))
                }
            }
            return stops
        }
        val pastel = AppPreferences.pastelColorsState
        val dark = isCurioDarkTheme()
        if (distinct.size <= 1) {
            return CurioGradients.cardGradient(mixedDeckAccent(distinct, pastel = pastel, dark = dark))
        }
        val resolved = distinct.take(4).map { accent ->
            // Theme shade of the RAW accent — the same recipe as the deck's
            // single-accent resolution (themedAccentFor).
            if (!pastel) (if (dark) darkAccent(accent) else accent) else pastelAccent(accent, dark)
        }
        val stops = mutableListOf<Color>()
        resolved.forEachIndexed { i, c ->
            if (i == 0) stops.add(c)
            if (i < resolved.size - 1) {
                // Fine OKLab steps to the next accent — no seams, no bands.
                // (7 steps including both endpoints; drop the duplicated
                // start so consecutive pairs chain seamlessly.)
                stops.addAll(oklabGradientStops(c, resolved[i + 1], steps = 7).drop(1))
            }
        }
        return stops
    }


    /**
     * The mixed deck's page wash — the Spin screen wears THE blended color
     * the mix resolves to (not the first category's wash), so the whole page
     * reads in the deck's mixed color story. Unlike the faint single-category
     * wash, this is a strong, unmistakable tint: the page is DOMINATED by the
     * blend color — a soft pastel twin in light mode so the maroon ink stays
     * readable, the deep blend over midnight in dark so white ink pops — so
     * switching mixes visibly repaints the page (two different decks never
     * wash to the same near-background color). Honors the manual tint toggle
     * and theme style like the category wash.
     */
    @Composable
    fun mixedDeckWash(blend: Color): Color {
        val background = MaterialTheme.colorScheme.background
        // v185 — Material theme: the page stays NEUTRAL (M3: surfaces stay
        // neutral, one primary carries the brand — no strong page tints).
        if (materialThemeOn) return background
        if (!AppPreferences.tintWashEffective()) return background
        // v81 — dark mode: NO page tint — pitch black, the same rule as the
        // single-category wash (the watermark + deck carry the mix color).
        if (isCurioDarkTheme()) return background
        // Light: a pastel twin of the blend over cream at high strength —
        // the hue is unmistakable per mix while staying light enough for
        // the deep maroon ink on top (pastel mode: a moderate wash over
        // cream so green-heavy mixes don't flood the screen with bright
        // mint).
        return if (AppPreferences.pastelColorsState) lerp(background, blend, 0.72f)
        else lerp(background, lerp(blend, Color.White, 0.40f), 0.85f)
    }

    /**
     * Lays the mixed-deck hero stops out in a non-linear arrangement picked
     * deterministically from [seed] (the deck's sorted category ids), so
     * different mixes get different treatments — a diagonal sweep for some,
     * a reversed diagonal for others, a radial glow for the rest — while a
     * given deck always renders the same way. [widthPx]/[heightPx] are the
     * hero card's pixel size, so the brush geometry matches the card.
     */
    fun mixedDeckHeroBrush(
        stops: List<Color>,
        widthPx: Float,
        heightPx: Float,
        seed: Int
    ): Brush {
        val idx = ((seed % 3) + 3) % 3
        return when (idx) {
            0 -> Brush.linearGradient(
                stops,
                start = Offset(0f, 0f),
                end = Offset(widthPx, heightPx)
            )
            1 -> Brush.linearGradient(
                stops,
                start = Offset(0f, heightPx),
                end = Offset(widthPx, 0f)
            )
            else -> Brush.radialGradient(
                stops,
                // Glow from behind the watermark glyph (center-right) out to
                // the card's far corner, so the last stop fills every edge.
                center = Offset(widthPx * 0.72f, heightPx * 0.42f),
                radius = widthPx.coerceAtLeast(heightPx) * 0.95f
            )
        }
    }

}

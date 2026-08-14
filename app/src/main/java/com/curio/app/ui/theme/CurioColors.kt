package com.curio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.curio.app.data.AppPreferences
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
    /** Dark-mode companion for the shared Home/Profile/Settings hero family. */
    val HomeRosewoodDark  = Color(0xFF713849)
    // v27l — the optional sky-azure hero variant: a brighter, fresher azure
    // (the Science/Sky hue at sky-300-ish saturation, held a touch softer
    // than neon) for light, and a deep muted azure companion for dark mode
    // (same L/S as HomeRosewoodDark).
    val HomeAzure        = Color(0xFF9ED2EF)
    val HomeAzureDark    = Color(0xFF385C71)

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
    fun categoryCardFill(accent: Color, dark: Boolean = false): Color = when {
        // AMOLED cards are intentionally near-black. The category identity is
        // carried by a restrained accent trace, not a bright jewel-tone fill.
        AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED ->
            lerp(Color.Black, accent, 0.12f)
        AppPreferences.pastelColorsState -> accent
        else -> {
            // Non-pastel dark heroes need a true jewel-tone depth; the old
            // 10% deepen left coral/sky/amber fills too close to bright raw
            // accents on the midnight surface. Light mode keeps its existing
            // treatment; callers opt into the dark value explicitly.
            lerp(accent, Color.Black, if (dark) 0.28f else 0.10f)
        }
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
        if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
            val base = Color.Black
            val accentTrace = lerp(base, accent, 0.18f)
            return listOf(
                lerp(base, accent, 0.08f),
                accentTrace
            )
        }
        // End on the ACTIVE theme's background so cards always echo the
        // surface behind them — cream in light, midnight in dark, pure
        // black in AMOLED, the device's dynamic background in Material.
        // v25 — Pastel crown depth PASSED: always ON, so its toggle was
        // removed from Experiments and the pastel crown read is fixed.
        val start = if (AppPreferences.pastelColorsState) {
            // v7.12 — subtle 5% black deepen at the very top of pastel
            // gradients so every pastel card reads with a gentle darker
            // crown instead of a uniform pastel from edge to edge.
            lerp(categoryCardFill(accent, isCurioDarkTheme()), Color.Black, 0.05f)
        } else {
            categoryCardFill(accent, isCurioDarkTheme())
        }
        // v7.8 — on tint-washed Curio pages the card melts into the washed
        // background on the category's OWN hue (same recipe as the page
        // wash), not the raw cream that dragged cool accents off-family.
        val end = if (AppPreferences.tintWashEffective() && !isCurioDarkTheme()) {
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
        val dark = isCurioDarkTheme()
        val pastel = AppPreferences.pastelColorsState
        val amoled = AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED

        // The warm golden companion — a rich amber-gold that pairs with
        // every category accent. In pastel mode it softens to a butter
        // cream; in dark/AMOLED it deepens to a burnished bronze.
        val companionBase = when {
            amoled -> lerp(Color.Black, CurioColors.GoldInk, 0.28f)
            pastel && !dark -> CurioColors.ButterYellow
            dark -> lerp(CurioColors.GoldInk, Color.Black, 0.35f)
            else -> CurioColors.GoldInk
        }

        // Top crown — a whisper of light at the very top for a premium
        // lit-surface feel (muted in dark/AMOLED, crisp in light).
        val crown = when {
            amoled -> lerp(Color.Black, accent, 0.10f)
            dark -> lerp(accent, Color.White, 0.06f)
            pastel -> lerp(accent, Color.White, 0.10f)
            else -> lerp(accent, Color.White, 0.16f)
        }

        // The accent stop — the card fill (theme-aware), which is already
        // pastel in pastel mode and black-tinted in AMOLED.
        val accentStop = when {
            amoled -> lerp(Color.Black, accent, 0.12f)
            pastel && AppPreferences.pastelCrownDepthState ->
                lerp(categoryCardFill(accent, dark), Color.Black, 0.05f)
            else -> categoryCardFill(accent, dark)
        }

        // The golden companion stop — the warm accent at the bottom.
        val companionStop = when {
            amoled -> lerp(Color.Black, companionBase, 0.10f)
            else -> companionBase
        }

        return listOf(crown, accentStop, companionStop)
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
 * HSL centroid of the three accents. Every blend clears WCAG AA (4.5:1)
 * against white — any that fell short were deepened to the brightest shade
 * that still clears it, keeping mixes vivid with crisp white labels. Four+
 * accents use the runtime [hslCentroid] with the same 4.5:1 steering.
 */
object CurioMixedDeck {

    /** Curated premium blends for every pair of the six researched accents. */
    private val PairBlends: Map<Set<Color>, Color> = mapOf(
        // Indigo family mixes — violet-magenta, azure, petrol
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose)  to Color(0xFFA72CD6),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryAmber) to Color(0xFFA926B5),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryTeal)  to Color(0xFF1F62A8),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategorySky)   to Color(0xFF1649C4),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryCoral) to Color(0xFFBE39CE),
        // Rose family mixes — ember, violet, blush
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryAmber) to Color(0xFFBF1E14),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryTeal)  to Color(0xFF4A12A8),
        setOf(CurioColors.CategoryRose,   CurioColors.CategorySky)   to Color(0xFF6D0BB8),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryCoral) to Color(0xFFEA1142),
        // Amber mixes — ember, jade (steered off the olive dead zone), flame
        setOf(CurioColors.CategoryAmber,  CurioColors.CategoryTeal)  to Color(0xFF15875A),
        setOf(CurioColors.CategoryAmber,  CurioColors.CategorySky)   to Color(0xFF0B8484),
        setOf(CurioColors.CategoryAmber,  CurioColors.CategoryCoral) to Color(0xFFE32D0F),
        // Teal / Sky / Coral family mixes
        setOf(CurioColors.CategoryTeal,   CurioColors.CategorySky)   to Color(0xFF067E94),
        setOf(CurioColors.CategoryTeal,   CurioColors.CategoryCoral) to Color(0xFF6C18F5),
        setOf(CurioColors.CategorySky,    CurioColors.CategoryCoral) to Color(0xFF9E1BFF)
    )

    /** Curated premium blends for every triple of the six researched accents. */
    private val TripleBlends: Map<Set<Color>, Color> = mapOf(
        // Indigo-anchored triples — magenta-rose, periwinkle, azure
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose,  CurioColors.CategoryAmber) to Color(0xFFE41772),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose,  CurioColors.CategoryTeal)  to Color(0xFF7262EB),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose,  CurioColors.CategorySky)   to Color(0xFF815AF1),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryRose,  CurioColors.CategoryCoral) to Color(0xFFDD129E),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryAmber, CurioColors.CategoryTeal)  to Color(0xFF2572E7),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryAmber, CurioColors.CategorySky)   to Color(0xFF6563F4),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryAmber, CurioColors.CategoryCoral) to Color(0xFFE70F66),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryTeal,  CurioColors.CategorySky)   to Color(0xFF1479CB),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategoryTeal,  CurioColors.CategoryCoral) to Color(0xFF6F61F1),
        setOf(CurioColors.CategoryIndigo, CurioColors.CategorySky,   CurioColors.CategoryCoral) to Color(0xFF8158F6),
        // Rose-anchored triples — burnt orange, crimson, magenta
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryAmber, CurioColors.CategoryTeal)  to Color(0xFFD4450D),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryAmber, CurioColors.CategorySky)   to Color(0xFFEC0630),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryAmber, CurioColors.CategoryCoral) to Color(0xFFEE0505),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryTeal,  CurioColors.CategorySky)   to Color(0xFF0B76DC),
        setOf(CurioColors.CategoryRose,   CurioColors.CategoryTeal,  CurioColors.CategoryCoral) to Color(0xFFE90A57),
        setOf(CurioColors.CategoryRose,   CurioColors.CategorySky,   CurioColors.CategoryCoral) to Color(0xFFE20291),
        // Amber / Teal / Sky triples — jade, ember, azure (steered off olive)
        setOf(CurioColors.CategoryAmber,  CurioColors.CategoryTeal,  CurioColors.CategorySky)   to Color(0xFF058673),
        setOf(CurioColors.CategoryAmber,  CurioColors.CategoryTeal,  CurioColors.CategoryCoral) to Color(0xFFCF4B06),
        setOf(CurioColors.CategoryAmber,  CurioColors.CategorySky,   CurioColors.CategoryCoral) to Color(0xFFEE001A),
        setOf(CurioColors.CategoryTeal,   CurioColors.CategorySky,   CurioColors.CategoryCoral) to Color(0xFF0478D3)
    )

    /**
     * The single blend color a mixed deck wears on peeks, the spin button and
     * confetti. A single distinct accent returns itself unchanged; pairs and
     * triples use the curated tables; four+ use the order-independent
     * [hslCentroid] (circular hue mean), so every mix stays vivid, white-label
     * safe, and never depends on selection order.
     *
     * Pastel color mode (v7.5): pass `pastel = true` + the active dark state
     * so the DEEP curated pair/triple blends soften to the theme-aware pastel
     * twin ([pastelAccent]) — airy pastels in light mode, muted deep pastels
     * in dark. Single accents arrive already pastel (callers resolve them via
     * [com.curio.app.ui.theme.CurioCategory.themedAccent]) and the 4+ path
     * ([hslCentroid]) keeps pastel inputs at their mean lightness, so neither
     * is re-softened here.
     */
    fun mixedDeckAccent(accents: List<Color>, pastel: Boolean = false, dark: Boolean = false): Color {
        // Color is a value class — value-based equality means distinct() alone
        // dedupes (toArgb() isn't part of the Compose BOM resolved here).
        val distinct = accents.distinct()
        val blend = when (distinct.size) {
            0 -> CurioColors.CategoryCoral
            1 -> distinct.first()
            2 -> PairBlends[distinct.toSet()] ?: hslBlend(distinct[0], distinct[1])
            3 -> TripleBlends[distinct.toSet()] ?: hslCentroid(distinct, pastel)
            else -> hslCentroid(distinct, pastel)
        }
        if (pastel && distinct.size in 2..3) return pastelAccent(blend, dark)
        return blend
    }

    /**
     * Hero-card gradient stops. Single accent → the standard theme-aware
     * [CurioGradients.cardGradient]. Two+ accents → a smooth multi-accent
     * sweep: each accent followed by the curated pair blend with its
     * neighbor, so the gradient glides through premium blended hues instead
     * of banding through muddy RGB midpoints (the old raw-stop version —
     * teal↔amber and sky↔amber cross the olive dead zone). Leaner than the
     * original (no redundant HSL intermediates on either side of each
     * blend), so the sweep reads as a duotone glide instead of a
     * stop-heavy rainbow ribbon. Supports up to four accents — beyond that
     * a single sweep slides into rainbow regardless of interpolation.
     */
    @Composable
    fun mixedDeckGradient(accents: List<Color>): List<Color> {
        // Color is a value class — value-based equality means distinct() alone
        // dedupes (toArgb() isn't part of the Compose BOM resolved here).
        val distinct = accents.distinct()
        if (distinct.size <= 1) {
            return CurioGradients.cardGradient(mixedDeckAccent(distinct))
        }
        val dark = isCurioDarkTheme()
        val pastel = AppPreferences.pastelColorsState
        val stops = mutableListOf<Color>()
        distinct.take(4).forEachIndexed { i, accent ->
            stops.add(accent)
            // Seam through the curated pair blend (saturation-boosted,
            // dead-zone steered) so the transition stays vivid rather than
            // graying out. v7.5 — pastel mode softens the DEEP seam blends
            // to their pastel twin so the whole sweep reads pastel end-to-end
            // (the accent stops arrive already pastel via themedAccent()).
            if (i < 3 && i < distinct.size - 1) {
                var seam = mixedDeckAccent(listOf(accent, distinct[i + 1]))
                if (pastel) seam = pastelAccent(seam, dark)
                stops.add(seam)
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
        if (!AppPreferences.tintWashEffective()) return background
        return if (isCurioDarkTheme()) {
            if (AppPreferences.pastelColorsState) {
                // Pastel mode: the blend is already a muted deep pastel — a
                // moderate-strength wash over midnight keeps the soft hue on
                // the page instead of deepening it toward a jewel tone.
                // v46 — a touch deeper (42% instead of 55%) so pastel-dark
                // pages stay sleek and dark with real contrast.
                lerp(background, blend, 0.42f)
            } else {
                // v46 — a deep, muted jewel tone: the blend is blackened hard
                // (50%) and mixed at a moderate strength (42%) over midnight,
                // so every mix's hue stays distinguishable while the page
                // reads as a sleek high-contrast dark background for the
                // white ink and paper cards (the old 35%/45% still floated
                // lighter than the single-category pages).
                lerp(background, lerp(blend, Color.Black, 0.50f), 0.42f)
            }
        } else {
            if (AppPreferences.pastelColorsState) {
                // Pastel mode: the blend is already an airy pastel — a
                // moderate wash over cream keeps the pastel hue clearly on
                // the page without glowing (v7.36 — 72% instead of 80% so
                // green-heavy mixes don't flood the screen with bright mint).
                lerp(background, blend, 0.72f)
            } else {
                // A pastel twin of the blend over cream at high strength — the
                // hue is unmistakable per mix while staying light enough for the
                // dark maroon ink on top.
                lerp(background, lerp(blend, Color.White, 0.40f), 0.85f)
            }
        }
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

    /**
     * HSL midpoint blend along the shortest hue path with a small saturation
     * boost — the premium way to mix two deep accents without a muddy middle.
     *
     * Uses local HSL conversion ([toHsl], [fromHsl]) built only on the bedrock
     * RGBA channels of [Color], so the blend stays version-proof across the
     * Compose BOM this project resolves (no hue/saturation/lightness
     * accessors, which aren't part of that API surface).
     */
    private fun hslBlend(a: Color, b: Color): Color {
        val ha = toHsl(a)
        val hb = toHsl(b)
        var dh = hb.h - ha.h
        if (dh > 180f) dh -= 360f
        if (dh < -180f) dh += 360f
        val hue = ((ha.h + dh / 2f) + 360f) % 360f
        // Boosted midpoint saturation keeps the blend vivid (naive averaging
        // can drift toward gray when the endpoints differ in lightness).
        val sat = ((ha.s + hb.s) / 2f + 0.05f).coerceIn(0f, 1f)
        val light = (ha.l + hb.l) / 2f
        return fromHsl(hue, sat, light)
    }

    /**
     * Order-independent blend for 4+ accents (and the fallback for any
     * unmapped pair/triple): circular mean of the hues, mean saturation with
     * a small boost, then lightness steered to the brightest shade that still
     * clears WCAG AA (4.5:1) against white — matching the curated tables.
     *
     * Pastel mode keeps the inputs' mean lightness (the accents are already
     * pastel / muted pastel) instead of steering toward a deep shade that
     * clears 4.5:1 against white — the white-label steering only applies to
     * the deep accents.
     */
    private fun hslCentroid(colors: List<Color>, pastel: Boolean = false): Color {
        val hs = colors.map { toHsl(it) }
        val n = hs.size.toFloat()
        var sx = 0f
        var sy = 0f
        var lightnessSum = 0f
        for (h in hs) {
            val rad = h.h * (kotlin.math.PI.toFloat() / 180f)
            sx += kotlin.math.cos(rad)
            sy += kotlin.math.sin(rad)
            lightnessSum += h.l
        }
        val hue = (kotlin.math.atan2(sy, sx) * (180f / kotlin.math.PI.toFloat()) + 360f) % 360f
        val sat = (hs.sumOf { it.s.toDouble() }.toFloat() / n + 0.05f).coerceIn(0f, 1f)
        if (pastel) {
            return fromHsl(hue, sat.coerceIn(0f, 1f), (lightnessSum / n).coerceIn(0f, 1f))
        }
        return steerLightness(hue, sat, 4.5f)
    }

    /** Brightest shade of (hue, sat) that still clears [target] contrast vs white. */
    private fun steerLightness(hue: Float, sat: Float, target: Float): Color {
        var lo = 0f
        var hi = 1f
        repeat(32) {
            val mid = (lo + hi) / 2f
            if (contrastVsWhite(fromHsl(hue, sat, mid)) >= target) lo = mid else hi = mid
        }
        return fromHsl(hue, sat, lo)
    }

    /** WCAG contrast ratio of [color] against white. */
    private fun contrastVsWhite(color: Color): Float {
        val l = 0.2126f * toLinear(color.red) + 0.7152f * toLinear(color.green) + 0.0722f * toLinear(color.blue)
        return 1.05f / (l + 0.05f)
    }

    /** sRGB channel → linear light (WCAG relative luminance). */
    private fun toLinear(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

}

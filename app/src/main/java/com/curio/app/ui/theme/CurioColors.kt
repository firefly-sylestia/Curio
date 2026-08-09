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
    val CategoryIndigo   = Color(0xFF4338CA)  // Music — Artists / Albums
    val CategoryRose     = Color(0xFFBE123C)  // Movies — Directors / Films
    val CategoryAmber    = Color(0xFFB45309)  // Books — Authors / Books
    val CategoryTeal     = Color(0xFF0F766E)  // Visual Art — Painters / Artworks
    val CategorySky      = Color(0xFF0369A1)  // Science — Scientists / Discoveries
    val CategoryCoral    = CoralBlush  // Wildcard — the app's brand primary, not a deep accent

    /** Light 300-level twins for accent-colored ink on dark surfaces. */
    val CategoryIndigoInk = Color(0xFFA5B4FC)
    val CategoryRoseInk   = Color(0xFFFDA4AF)
    val CategoryAmberInk  = Color(0xFFFCD34D)
    val CategoryTealInk   = Color(0xFF5EEAD4)
    val CategorySkyInk    = Color(0xFF7DD3FC)
    val CategoryCoralInk  = Color(0xFFFFC2CE)  // light coral twin for dark-surface ink

    /** Tinted (20% alpha) washes of the researched category accents. */
    val CategoryIndigoTint = CategoryIndigo.copy(alpha = 0.20f)
    val CategoryRoseTint   = CategoryRose.copy(alpha = 0.20f)
    val CategoryAmberTint  = CategoryAmber.copy(alpha = 0.20f)
    val CategoryTealTint   = CategoryTeal.copy(alpha = 0.20f)
    val CategorySkyTint    = CategorySky.copy(alpha = 0.20f)
    val CategoryCoralTint  = CategoryCoral.copy(alpha = 0.20f)

    /**
     * Legacy warm pastels — retained ONLY for brand/decorative use
     * (profile stat icons, wildcard rainbow gradient). Categories now use
     * the researched [CategoryIndigo]..[CategorySky] tokens above plus the
     * brand-primary [CategoryCoral] used by the Wildcard.
     */
    val Lilac            = Color(0xFFC9A6F2)  // legacy soft purple
    val DustyBlue        = Color(0xFF9BB8E8)  // legacy soft blue
    val Sage             = Color(0xFFA8C99A)  // legacy soft green
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
     * Lightness floor for material stops — see [floorForWhiteInk].
     * 0.30 luminance keeps white large/bold card titles ≥ ~3:1 (WCAG large
     * text AA); the small teaser copy sits near the card's darker base.
     */
    private const val WhiteInkLightnessFloor = 0.30f

    /**
     * Floors a material stop's luminance to [WhiteInkLightnessFloor] so
     * white card content stays readable on it. v7.37 — used on the device
     * M3 primary in the redesigned Material card gradient: dark-mode
     * dynamic primaries are pastel-pale and would wash out white text, so
     * they're pulled down here (light dynamic primaries are already dark
     * enough — a no-op there). The material HUE is untouched; only
     * lightness is pulled down.
     */
    private fun floorForWhiteInk(c: Color): Color {
        val lum = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
        if (lum <= WhiteInkLightnessFloor) return c
        return lerp(c, Color.Black, (lum - WhiteInkLightnessFloor) / lum)
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
     * v7.37 — Material card blend redesign: when "Material card blends" is on
     * (default), every category card wears a clean TWO-color gradient — the
     * device's M3 PRIMARY (the least-pastel of the Material You trio) at
     * ~90-95%, with only a 5-10% "sprinkle" of the category accent. The old
     * 6-band wheel that mixed all three M3 colors with 12-52% category tints
     * read as a loud blend; this reads as the device's own color with just a
     * whisper of the category so the palette stays true to the device.
     *
     * The sprinkle eases from 5% at the top of the card to 10% at the
     * bottom, so the category presence is subtle but present. In dark mode
     * the device's pale dynamic primary is floored for white card content
     * (hue untouched) instead of being drowned in category tint, and the
     * sprinkle strengthens a little (10% → 18%) so categories stay
     * distinguishable on the muted mid-tone device color. Pastel mode
     * softens both the device color and the sprinkle to their pastel
     * twins. Off / non-Material style: falls through to the classic
     * two-stop card gradient below.
     */
    @Composable
    fun cardGradient(accent: Color): List<Color> {
        // AMOLED uses the same near-black card language everywhere: a raised
        // black surface with only a quiet category-color bloom. This keeps
        // category cards, heroes, and peek cards power-friendly and coherent.
        if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
            val base = MaterialTheme.colorScheme.surfaceContainerHigh
            val accentTrace = lerp(base, accent, 0.18f)
            return listOf(
                lerp(base, accent, 0.08f),
                accentTrace
            )
        }
        if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL &&
            AppPreferences.materialCardBlendsState
        ) {
            val scheme = MaterialTheme.colorScheme
            val pastel = AppPreferences.pastelColorsState
            val dark = isCurioDarkTheme()
            // Material cards use the semantic primary-container pair as their
            // readable base, then add a restrained category trace. This keeps
            // the dynamic wallpaper palette coherent without breaking the
            // onPrimaryContainer contrast contract.
            val deviceRaw = scheme.primaryContainer
            // v7.37 — floor the device color's lightness for WHITE card
            // content when it's too pale (dark-mode dynamic palettes are
            // pastel-pale and would wash out white text; light dynamic
            // primaries are already dark enough, so floorForWhiteInk is a
            // no-op there). Only lightness moves — the device hue stays.
            val device = if (pastel) {
                pastelAccent(deviceRaw, dark)
            } else {
                deviceRaw
            }
            // The category "sprinkle" — the single accent presence on the
            // card, slightly deepened (or pastel twin in pastel mode) so it
            // reads as a solid whisper rather than a flat wash.
            val catStop = if (pastel) {
                pastelAccent(accent, dark)
            } else {
                lerp(accent, Color.Black, if (dark) 0.22f else 0.08f)
            }
            // TWO-color gradient: ~90-95% device color with a category
            // sprinkle easing down the card. Light mode (and pastel mode)
            // hold the pure 5% → 10% requested sprinkle — the device palette
            // dominates and the category is a subtle accent. Dark mode
            // (non-pastel) raises it to 10% → 18% because the floored device
            // primary is a muted mid-tone there: at the light 5/10 ratios
            // every category would read as the same device color and the
            // deck/picker would lose its color-coding. The device color
            // still owns ~82-90% of the card.
            val sprinkleTop = if (dark && !pastel) 0.12f else 0.07f
            val sprinkleBottom = if (dark && !pastel) 0.20f else 0.14f
            return listOf(
                lerp(device, catStop, sprinkleTop),
                lerp(device, catStop, sprinkleBottom)
            )
        }
        // End on the ACTIVE theme's background so cards always echo the
        // surface behind them — cream in light, midnight in dark, pure
        // black in AMOLED, the device's dynamic background in Material.
        val start = if (AppPreferences.pastelColorsState && AppPreferences.pastelCrownDepthState) {
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
                lerp(background, blend, 0.55f)
            } else {
                // A deep, muted jewel tone: the blend darkened toward black
                // (~35%) and mixed at a moderate strength (~45%) over midnight.
                // The old 70% PURE blend rendered as a loud, saturated banner
                // (e.g. a vivid purple page) in dark mode; this keeps each mix's
                // hue clearly distinguishable while reading as a tasteful dark
                // background the white ink and paper cards can sit on.
                lerp(background, lerp(blend, Color.Black, 0.35f), 0.45f)
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

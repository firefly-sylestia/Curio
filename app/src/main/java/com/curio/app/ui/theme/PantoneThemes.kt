package com.curio.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * v411 — THE PANTONE THEMES.
 *
 * Three color themes built from real Pantone numbers. Each one is a PAGE, a
 * HERO/CARD/BUTTON colour and an INK, and the hex values are the published
 * sRGB equivalents of the Pantone references the member gave (a Pantone
 * number is an ink, so its screen value is the closest sRGB match — the
 * Pantone code is carried in each entry's own doc so the number, not the
 * hex, stays the source of truth):
 *
 *  [PANTONE_CREAM]      page P 7-9 U · hero/cards P 109-10 U · ink P 101-16 U
 *  [PANTONE_TERRACOTTA] page P 24-9 U · hero/cards 2350 U · ink P 14-8 U
 *  [PANTONE_LIME]       page P 1-3 C · hero/cards P 163-8 C · ink P 102-6 C
 *
 * ── TWO RULES THAT KEEP A PANTONE THEME HONEST ─────────────────────────
 *
 * 1. **A DARK TWIN PER THEME.** These are light palettes, but a member on
 *    dark mode must not lose their theme: [schemeFor] builds the dark side
 *    from the SAME three numbers — the page becomes a deep shade of its own
 *    hue, the hero/card colour deepens but keeps its hue and saturation, and
 *    the ink lightens into a pale tint of itself. Nothing is invented from a
 *    neutral grey, so "Pantone Cream" is still creamy at night.
 *
 * 2. **THE INK IS AN ACCENT; BODY TEXT GETS A READABLE TWIN.** Two of the
 *    three Pantone inks cannot carry body text on their own page (P 14-8 U is
 *    an orange on cream, P 102-6 C an indigo on lime — both well under 4.5:1),
 *    so [inkOn] resolves per surface: the Pantone ink itself where it reads
 *    (icons, headings, labels, accents — the member asked for exactly that),
 *    the app's own deep same-hue ink ([readableLightInk]) for body text on a
 *    light fill, and a light tint of it for text on a dark fill (the
 *    terracotta hero). Member's own decision: "Pantone ink for accents, a
 *    deeper shade for body text".
 *
 * ── TWO MORE RULES FROM THE MEMBER ("dont use any border for cards and
 * dont use transparent colors" in the new accents) ────────────────────
 *
 * 3. **EVERY COLOUR IN THESE SCHEMES IS OPAQUE.** No `copy(alpha = …)`
 *    anywhere below: a faded ink is mixed INTO its surface with [lerp]
 *    instead, so a Pantone page shows no colour bleeding through a card, a
 *    hairline or a label. (The app-wide alpha-role convention is untouched —
 *    this is the rule for the Pantone palettes themselves.)
 *
 * 4. **CARDS WEAR NO BORDER HERE.** `outlineVariant` is the app's card-edge
 *    colour, and these three themes drop that edge entirely: their cards
 *    separate by the hero-fill lightness ladder ALONE (see
 *    [lightScheme]'s `surfaceContainer*` steps). `outlineVariant` below is
 *    therefore the *divider* hairline — the colour of an inset rule drawn on
 *    a hero-filled card — and the shared `curioCardEdgeColor` helper answers
 *    "no edge" under these themes so the card components never draw one.
 */
internal enum class PantoneTheme(
    /** The stored id (see `AppPreferences.COLOR_THEME_*`). */
    val id: String,
    /** The sheet's row title. */
    val label: String,
    /** The Pantone references, printed in the sheet under the preview. */
    val reference: String,
    /** The page — a Pantone background. */
    val page: Color,
    /** The hero, the cards and the buttons — one Pantone fill. */
    val hero: Color,
    /** The Pantone text + icon colour (the accent; see [inkOn]). */
    val ink: Color
) {
    /** P 7-9 U page · P 109-10 U hero/cards · P 101-16 U ink. */
    PANTONE_CREAM(
        id = "pantone-cream",
        label = "Pantone Cream",
        reference = "P 7-9 U · P 109-10 U · P 101-16 U",
        page = Color(0xFFF7E9C6),
        hero = Color(0xFFB6CADF),
        ink = Color(0xFF4D424C)
    ),

    /** P 24-9 U page · 2350 U hero/cards · P 14-8 U ink. */
    PANTONE_TERRACOTTA(
        id = "pantone-terracotta",
        label = "Pantone Terracotta",
        reference = "P 24-9 U · 2350 U · P 14-8 U",
        page = Color(0xFFF5E1C5),
        hero = Color(0xFF9E483F),
        ink = Color(0xFFEE9C44)
    ),

    /** P 1-3 C page · P 163-8 C hero/cards · P 102-6 C ink. */
    PANTONE_LIME(
        id = "pantone-lime",
        label = "Pantone Lime",
        reference = "P 1-3 C · P 163-8 C · P 102-6 C",
        page = Color(0xFFFAF4D3),
        hero = Color(0xFFCDD325),
        ink = Color(0xFF5F62A1)
    );

    /** The page for [dark] — the light page itself, or a deep shade of its hue. */
    fun pageFor(dark: Boolean): Color = if (!dark) page else shade(page, saturation = 0.34f, lightness = 0.10f)

    /** The hero/card/button fill for [dark]. */
    fun heroFor(dark: Boolean): Color = if (!dark) hero else shade(hero, saturation = 0.55f, lightness = 0.26f)

    /** Body text on the PAGE ([dark]) — readable, and a tint of the Pantone ink. */
    fun pageInkFor(dark: Boolean): Color = if (!dark) inkOn(page, ink) else inkOn(darkPage(), ink)

    /** Body text on a CARD ([dark]). */
    fun cardInkFor(dark: Boolean): Color = inkOn(heroFor(dark), ink)

    /** Icons, headings and accents on the PAGE — the Pantone ink where it reads. */
    fun accentFor(dark: Boolean): Color =
        if (contrast(ink, pageFor(dark)) >= 3f) ink else inkOn(pageFor(dark), ink).let {
            if (dark) lerp(it, Color.White, 0.25f) else it
        }

    /** The readable ink ON the hero fill — what the hero's words wear. */
    fun onHeroFor(dark: Boolean): Color = inkOn(heroFor(dark), ink)

    /** The theme's own [ColorScheme] for [dark]. */
    fun schemeFor(dark: Boolean): ColorScheme = if (dark) darkScheme() else lightScheme()

    private fun darkPage(): Color = shade(page, saturation = 0.34f, lightness = 0.10f)

    /**
     * The LIGHT scheme: the Pantone page, the Pantone hero as the card/button
     * fill, and the ink ladder above it. The card steps DEEPEN the hero by a
     * few points of lightness each (a block inside a card, a pill inside
     * that), which is the app's own ladder rule read from a Pantone fill
     * instead of from cream — a card always separates from what holds it.
     */
    private fun lightScheme(): ColorScheme = lightColorScheme(
        primary = hero,
        onPrimary = inkOn(hero, ink),
        primaryContainer = hero,
        onPrimaryContainer = inkOn(hero, ink),

        secondary = hero,
        onSecondary = inkOn(hero, ink),
        secondaryContainer = hero,
        onSecondaryContainer = inkOn(hero, ink),

        tertiary = hero,
        onTertiary = inkOn(hero, ink),

        background = page,
        onBackground = inkOn(page, ink),

        surface = page,
        onSurface = inkOn(page, ink),
        surfaceVariant = hero,
        // Opaque (rule 3): the muted role is the ink mixed into its own fill.
        onSurfaceVariant = lerp(inkOn(hero, ink), hero, 0.22f),

        surfaceContainerLowest = page,
        surfaceContainerLow = hero,
        surfaceContainer = deepen(hero, 0.05f),
        surfaceContainerHigh = deepen(hero, 0.10f),
        surfaceContainerHighest = deepen(hero, 0.15f),

        error = CurioColors.WarmCoralRed,
        onError = CurioColors.CreamWhite,

        // `outline` = an edge ON THE PAGE; `outlineVariant` = the inset
        // DIVIDER hairline on a hero-filled card (rule 4 — cards have no
        // edge of their own under these themes). Both opaque (rule 3).
        outline = lerp(inkOn(page, ink), page, 0.62f),
        outlineVariant = lerp(inkOn(hero, ink), hero, 0.86f)
    )

    /**
     * The DARK twin: the same three Pantones read at night. The page is a deep
     * shade of ITS OWN hue (a warm near-black for the two cream pages, an
     * olive near-black for the lime one), the hero/card fill deepens but keeps
     * its hue and saturation, and every ink is a pale tint of the Pantone ink.
     */
    private fun darkScheme(): ColorScheme {
        val deepPage = darkPage()
        val deepHero = heroFor(dark = true)
        val pageInk = paleInk(ink)
        return darkColorScheme(
            primary = deepHero,
            onPrimary = inkOn(deepHero, ink),
            primaryContainer = deepHero,
            onPrimaryContainer = paleInk(ink),

            secondary = deepHero,
            onSecondary = inkOn(deepHero, ink),
            secondaryContainer = deepHero,
            onSecondaryContainer = paleInk(ink),

            tertiary = deepHero,
            onTertiary = inkOn(deepHero, ink),

            background = deepPage,
            onBackground = pageInk,

            surface = deepPage,
            onSurface = pageInk,
            surfaceVariant = lift(deepHero, 0.05f),
            // Opaque (rule 3): the ink mixed into the night page.
            onSurfaceVariant = lerp(pageInk, deepPage, 0.20f),

            surfaceContainerLowest = deepPage,
            surfaceContainerLow = deepHero,
            surfaceContainer = lift(deepHero, 0.05f),
            surfaceContainerHigh = lift(deepHero, 0.10f),
            surfaceContainerHighest = lift(deepHero, 0.15f),

            error = Color(0xFFE0706A),
            onError = Color(0xFF2A0A08),

            outline = lerp(pageInk, deepPage, 0.66f),
            outlineVariant = lerp(inkOn(deepHero, ink), deepHero, 0.86f)
        )
    }

    /** The theme by its stored [id], or null when [id] is not a Pantone theme. */
    companion object {
        fun fromId(id: String): PantoneTheme? = entries.firstOrNull { it.id == id }
    }
}

// ── Ink resolution ──────────────────────────────────────────────────────────

/**
 * The ink that reads on [fill], starting from the theme's Pantone [ink]:
 * the Pantone ink itself when it clears ~4:1 on the fill, the app's own deep
 * same-hue body ink ([readableLightInk]) when the fill is LIGHT, and a pale
 * tint of it when the fill is dark.
 */
private fun inkOn(fill: Color, ink: Color): Color = when {
    contrast(ink, fill) >= 4f -> ink
    fill.luminance() > 0.28f -> readableLightInk(ink)
    else -> lerp(ink, Color.White, 0.80f)
}

/** A pale tint of [ink] for dark surfaces (the dark twin's text). */
private fun paleInk(ink: Color): Color = lerp(ink, Color.White, 0.62f)

/** [color] shifted to the given saturation/lightness, hue kept. */
private fun shade(color: Color, saturation: Float, lightness: Float): Color {
    val hsl = toHsl(color)
    return fromHsl(hsl.h, hsl.s.coerceAtMost(saturation), lightness)
}

/** [color] deepened by [amount] of lightness (the card ladder's inward step). */
private fun deepen(color: Color, amount: Float): Color {
    val hsl = toHsl(color)
    return fromHsl(hsl.h, hsl.s, (hsl.l - amount).coerceIn(0.06f, 1f))
}

/** [color] lifted by [amount] of lightness (the dark ladder's outward step). */
private fun lift(color: Color, amount: Float): Color {
    val hsl = toHsl(color)
    return fromHsl(hsl.h, hsl.s, (hsl.l + amount).coerceAtMost(0.96f))
}

/** WCAG contrast ratio between two opaque colors (1f..21f). */
internal fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

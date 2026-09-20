package com.curio.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * v420 — THE NAMED THEMES.
 *
 * Five full themes, each built from ONE hue, offered beside Curio rose, Azure,
 * Material and Adaptive Hero in the Appearance page's Color theme sheet. They
 * are not accent swaps: a named theme paints the PAGE, its CARD LADDER, the
 * HERO/BUTTON fill, the second and third fills, the ink and the error colour —
 * and it ships a DARK TWIN, because a page that ignores the night is only half a
 * theme.
 *
 * ── WHY ONE HUE IS ENOUGH ───────────────────────────────────────────────
 *
 * Everything here is a reading of the theme's own hue (its [hue]) at a chosen
 * lightness and saturation — never a second colour family. Two adjacent hues are
 * derived from it for the scheme's second and third fills ([second], [third]),
 * so the palette has depth without ever leaving the theme. The route a tone
 * takes is the same one the app's own rose takes (a page, a card ladder that
 * climbs AWAY from it, a deep hero, a pale container twin, a deep body ink).
 *
 * ── THE TWO MODES ───────────────────────────────────────────────────────
 *
 *  * **Light** — an airy page (a soft pastel of the hue), cards near-white and
 *    climbing up their own hue, a DEEP hero (the banner and every button) with
 *    white or near-white ink on it, and a near-black body ink carrying the hue.
 *  * **Dark (a deep JEWEL, by the member's choice)** — the page is a rich,
 *    saturated dark of the same hue (NOT a neutral near-black), cards stepping
 *    up it, and the hero a LIT jewel tone with a deep ink on it, which is the
 *    app's own dark language (bright accents on a dark page).
 *
 * Every pair was chosen to clear the app's readability bars: body ink ≥ 4.5:1
 * on the page and on the deepest card step, hero ink ≥ 4.5:1 on the hero, and
 * accent inks ≥ 3:1 on the page (the bar icons and large labels are held to).
 * If a tone is re-tuned, re-measure it — [contrast] is `internal` in this
 * package (see `CurioTheme.kt`).
 */
enum class CurioNamedTheme(
    /** The stored id (see `AppPreferences.COLOR_THEME_JADE` …). */
    val id: String,
    /** The sheet's row title. */
    val label: String,
    /** The one hue the whole theme is read from, in degrees. */
    val hue: Float,
    /** The sheet's one-line blurb. */
    val hint: String
) {
    JADE("named-jade", "Jade", 158f, "Calm jade on warm paper"),
    ORCHID("named-orchid", "Orchid", 318f, "Soft orchid on blush paper"),
    OCEAN("named-ocean", "Ocean", 191f, "Clear teal on cool paper"),
    SAND("named-sand", "Sand", 36f, "Warm ochre on sand paper"),
    EMBER("named-ember", "Ember", 12f, "Warm ember on a warm page");

    /** The hue the scheme's SECOND fill reads from — a step round the wheel. */
    private val second: Float get() = (hue + 38f) % 360f

    /** The hue the scheme's THIRD fill reads from — a step the other way. */
    private val third: Float get() = (hue - 46f + 360f) % 360f

    // ── THE LIGHT ANCHORS ─────────────────────────────────────────────────

    /** The page by day: a soft pastel of the theme's own hue. */
    fun pageFor(dark: Boolean): Color =
        if (dark) tone(hue, 0.45f, 0.12f) else tone(hue, 0.40f, 0.93f)

    /** The hero fill (the torn banner, buttons, selected rails). */
    fun heroFor(dark: Boolean): Color =
        if (dark) tone(hue, 0.60f, 0.56f) else tone(hue, 0.50f, 0.40f)

    /** The ink that reads on [heroFor]. */
    fun onHeroFor(dark: Boolean): Color =
        readableOn(heroFor(dark), bodyFor(dark))

    /**
     * Icons, headings and small accents ON a plain surface. A deeper reading by
     * day (so it reads on a light page), a lifted one at night.
     */
    fun accentFor(dark: Boolean): Color =
        if (dark) tone(hue, 0.50f, 0.72f) else tone(hue, 0.52f, 0.34f)

    /** Body text on the page. */
    private fun bodyFor(dark: Boolean): Color =
        if (dark) tone(hue, 0.22f, 0.92f) else tone(hue, 0.30f, 0.18f)

    /** The theme's own [ColorScheme] for [dark]. */
    fun schemeFor(dark: Boolean): ColorScheme = if (dark) darkScheme() else lightScheme()

    /**
     * The LIGHT scheme: the airy page, a near-white card ladder climbing the
     * theme's own hue, a deep hero, and a near-black body ink.
     */
    private fun lightScheme(): ColorScheme = lightColorScheme(
        primary = heroFor(false),
        onPrimary = onHeroFor(false),
        primaryContainer = tone(hue, 0.32f, 0.90f),
        onPrimaryContainer = tone(hue, 0.48f, 0.24f),

        secondary = tone(second, 0.42f, 0.42f),
        onSecondary = readableOn(tone(second, 0.42f, 0.42f), bodyFor(false)),
        secondaryContainer = tone(second, 0.30f, 0.91f),
        onSecondaryContainer = tone(second, 0.45f, 0.25f),

        tertiary = tone(third, 0.40f, 0.44f),
        onTertiary = readableOn(tone(third, 0.40f, 0.44f), bodyFor(false)),
        tertiaryContainer = tone(third, 0.30f, 0.91f),
        onTertiaryContainer = tone(third, 0.45f, 0.25f),

        background = pageFor(false),
        onBackground = bodyFor(false),

        surface = pageFor(false),
        onSurface = bodyFor(false),
        surfaceVariant = tone(hue, 0.26f, 0.92f),
        onSurfaceVariant = tone(hue, 0.24f, 0.38f),

        // The card ladder climbs ABOVE the page (the app's own rule): a card is
        // lighter than the page it sits on, and each nesting step descends back
        // toward it. Nothing here is ever a dark block.
        surfaceContainerLowest = tone(hue, 0.16f, 0.995f),
        surfaceContainerLow = tone(hue, 0.20f, 0.975f),
        surfaceContainer = tone(hue, 0.22f, 0.945f),
        surfaceContainerHigh = tone(hue, 0.24f, 0.905f),
        surfaceContainerHighest = tone(hue, 0.26f, 0.86f),

        error = tone(6f, 0.62f, 0.42f),
        onError = Color.White,
        errorContainer = tone(6f, 0.45f, 0.90f),
        onErrorContainer = tone(6f, 0.55f, 0.26f),

        outline = tone(hue, 0.22f, 0.70f),
        outlineVariant = tone(hue, 0.18f, 0.86f),
        scrim = Color.Black
    )

    /**
     * The DARK twin: a deep JEWEL of the theme's hue (the member's choice — a
     * rich dark, not a neutral near-black), cards stepping up it, and a lit
     * jewel hero with a deep ink on it.
     */
    private fun darkScheme(): ColorScheme = darkColorScheme(
        primary = heroFor(true),
        onPrimary = onHeroFor(true),
        primaryContainer = tone(hue, 0.48f, 0.26f),
        onPrimaryContainer = tone(hue, 0.42f, 0.90f),

        secondary = tone(second, 0.55f, 0.58f),
        onSecondary = tone(second, 0.50f, 0.15f),
        secondaryContainer = tone(second, 0.45f, 0.26f),
        onSecondaryContainer = tone(second, 0.40f, 0.90f),

        tertiary = tone(third, 0.52f, 0.60f),
        onTertiary = tone(third, 0.48f, 0.15f),
        tertiaryContainer = tone(third, 0.44f, 0.26f),
        onTertiaryContainer = tone(third, 0.38f, 0.90f),

        background = pageFor(true),
        onBackground = bodyFor(true),

        surface = pageFor(true),
        onSurface = bodyFor(true),
        surfaceVariant = tone(hue, 0.32f, 0.20f),
        onSurfaceVariant = tone(hue, 0.22f, 0.70f),

        // The night ladder: small steps up the jewel page (the stride the app's
        // own dark scheme uses), so a card reads as a plate rather than a
        // smudge while the page keeps the theme's colour.
        surfaceContainerLowest = tone(hue, 0.38f, 0.16f),
        surfaceContainerLow = tone(hue, 0.38f, 0.19f),
        surfaceContainer = tone(hue, 0.38f, 0.22f),
        surfaceContainerHigh = tone(hue, 0.38f, 0.25f),
        surfaceContainerHighest = tone(hue, 0.38f, 0.28f),

        error = tone(6f, 0.60f, 0.62f),
        onError = tone(6f, 0.50f, 0.14f),
        errorContainer = tone(6f, 0.45f, 0.28f),
        onErrorContainer = tone(6f, 0.40f, 0.90f),

        outline = tone(hue, 0.28f, 0.34f),
        outlineVariant = tone(hue, 0.26f, 0.24f),
        scrim = Color.Black
    )

    companion object {
        /** The theme by its stored [id], or null when [id] is not a named theme. */
        fun fromId(id: String): CurioNamedTheme? = entries.firstOrNull { it.id == id }
    }
}

// ── Palette maths ───────────────────────────────────────────────────────────

/**
 * One tone of a theme's own ramp: the given hue (wrapped into 0..360), the given
 * saturation and lightness. Every colour in a named theme is made here, which is
 * what keeps a theme to ONE hue.
 */
private fun tone(hue: Float, saturation: Float, lightness: Float): Color =
    fromHsl(((hue % 360f) + 360f) % 360f, saturation, lightness)

/**
 * The most readable of white and the theme's body ink on [fill] — tried in that
 * order, so a filled block gets white whenever white works, and the theme's own
 * ink when it is the only thing that reads.
 */
private fun readableOn(fill: Color, body: Color): Color = when {
    contrast(Color.White, fill) >= 4.5f -> Color.White
    contrast(body, fill) >= 4.5f -> body
    else -> if (contrast(Color.White, fill) >= contrast(body, fill)) Color.White else body
}

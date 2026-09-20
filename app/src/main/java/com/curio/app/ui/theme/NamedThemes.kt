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
 *    climbing up their own hue, and an AIRY hero (the banner and every button)
 *    wearing the theme's own DEEP ink, which is exactly the app's own light
 *    language (the pastel rose banner with deep plum words on it).
 *  * **Dark (a deep JEWEL, by the member's choice)** — the page is a rich,
 *    calm dark of the same hue (NOT a neutral near-black), cards stepping up
 *    it, and the hero a DEEP jewel tone carrying a pale ink — bright-on-dark
 *    done the right way round, so the banner is the calmest thing on the page
 *    rather than the brightest.
 *
 * v421 — WHERE THE TWO HEROES SIT, and why.
 *
 * v420 shipped a DEEP light hero (lightness 0.40) and a LIT dark hero (0.56).
 * Both were the wrong way round for the app they live in: the light heroes read
 * as heavy saturated slabs beside Curio's own airy rose banner (member: "the
 * hero feels too deep in light mode"), and the dark heroes were so bright that
 * a pale ink could not read on them at all — at hue 158 the pale ink landed at
 * about 2:1, which is precisely why the member's list of worst offenders ran
 * "jade is the worst, then orchid", and why the whole dark twin felt "off".
 * A dark hero is also the one block LIGHTNESS cannot separate from its page by
 * very much, so it does not need to be bright to read as a banner: it needs to
 * be the theme's hue at a depth that can carry the pale ink.
 *
 * The two heroes now sit at 0.66 (light) and 0.33 (dark) — the light one airy
 * with the deep themed ink on it, the dark one a deep jewel with the pale ink.
 * Both are inside the ranges that clear 4.5:1 for their own ink at every hue
 * in this enum, measured with [contrast] (see the numbers on the two functions).
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

    /**
     * The page: a soft pastel of the theme's own hue by day, a rich but QUIET
     * dark of it at night.
     *
     * v425 — THE NIGHT WENT MUTED. The dark page and everything above it wore
     * daylight chroma (0.36 here, 0.34 on the card ladder) and the member read
     * the result as "their card colors in settings, profile and many places,
     * some are too bright in dark mode only". A night fill wants little chroma:
     * the hue is legible at 0.30, and every point of chroma above that is
     * brightness the dark did not ask for.
     */
    fun pageFor(dark: Boolean): Color =
        if (dark) tone(hue, 0.30f, 0.10f) else tone(hue, 0.38f, 0.93f)

    /**
     * The hero fill (the torn banner, buttons, selected rails) — and the pair
     * to its ink is a measurement, not a taste: at 0.66 the light hero carries
     * [bodyFor] over 4.5:1 at the brightest hue here (Jade, ≈ 6.5:1).
     *
     * v425 — the NIGHT hero is a jewel, not a slab: 0.50 chroma at 0.33 was
     * still "too bright" to the member (a lit, saturated block beside a deep
     * page), so it drops to 0.36 chroma at 0.30. Measured against the pale ink
     * it carries, that is 5.5:1 at Jade and 7.6:1 at Ember — better than the
     * 4.2:1 the old tone managed at Jade, which was under the bar all along.
     */
    fun heroFor(dark: Boolean): Color =
        if (dark) tone(hue, 0.36f, 0.30f) else tone(hue, 0.46f, 0.66f)

    /** The ink that reads on [heroFor]. */
    fun onHeroFor(dark: Boolean): Color =
        readableOn(heroFor(dark), bodyFor(dark))

    /**
     * Icons, headings and small accents ON a plain surface. A deeper reading by
     * day (so it reads on a light page), a lifted one at night.
     */
    fun accentFor(dark: Boolean): Color =
        // v421 — the dark accent was 0.72, which on the deepest card step came
        // out around 4.2:1 — under the bar, and the member read the result as
        // "the button and texts blend".
        //
        // v423 — AND 0.78 WAS THE OTHER FAILURE: it cleared the contrast bar and
        // read as NEON on a deep page (member: "those new accnets are still bad
        // only is dark mode its too bright"). A night accent wants to be a
        // JEWEL, not a highlight, so it left 0.78 for 0.69.
        //
        // v425 — THE CHROMA WAS THE NEON, NOT THE LIGHTNESS. At 0.40 chroma the
        // 0.69 accent was still the loudest thing on a page of muted cards, and
        // it measured only 3.79–4.05:1 on the TOP card step — under the 4.5 bar
        // on the one surface it is drawn on most. Chroma drops to 0.30 and
        // lightness lands at 0.70, which is both quieter AND readable: 4.64:1
        // (Orchid, the worst hue) to 5.08:1 (Jade) on that step, measured with
        // [contrast]. Its LIGHT twin is untouched: the light page needed the
        // deeper reading and still has it.
        if (dark) tone(hue, 0.30f, 0.70f) else tone(hue, 0.52f, 0.34f)

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
        // v423 — A LADDER, NOT A WASH. The five steps were 0.03 of lightness
        // apart on one hue at one chroma, which is why every card on a page read
        // as the same shade of the same theme (member: "why every elements gets
        // the same shades of that theme specaily those settings cards"). Each
        // step now has its own stride AND its own chroma — calmer and paler as it
        // climbs — so nesting reads as planes rather than as one flat plate.
        surfaceContainerLowest = tone(hue, 0.14f, 0.995f),
        surfaceContainerLow = tone(hue, 0.18f, 0.972f),
        surfaceContainer = tone(hue, 0.22f, 0.940f),
        surfaceContainerHigh = tone(hue, 0.26f, 0.900f),
        surfaceContainerHighest = tone(hue, 0.30f, 0.850f),

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
     * rich dark, not a neutral near-black), cards stepping up it, and a deep
     * jewel hero with a pale ink on it.
     *
     * v425 — THE WHOLE NIGHT IS MUTED. Everything below the page's hue used to
     * be read at a chroma borrowed from daylight (0.46/0.44 on the second and
     * third fills, 0.34 → 0.24 up the ladder, 0.42 on the primary container),
     * and the result was a set of cards BRIGHTER than the scheme the app is
     * tuned against: a named dark card topped out at HSL 0.30 while the app's
     * own dark ladder tops out at #2C2C2C, more than twice its luminance —
     * which is the member's "their card colors in settings, profile and many
     * places, some are too bright in dark mode only … keep the colors muted".
     *
     * Two rules now hold every night fill down. The CHROMA is halved-and-then-
     * some (a night tone carries its hue at ≤ 0.38, never at daylight's 0.46),
     * and the LADDER sits lower with the same 0.03 stride, so a card is a quiet
     * plate on a jewel page instead of a lit one. Light mode is untouched.
     */
    private fun darkScheme(): ColorScheme = darkColorScheme(
        primary = heroFor(true),
        onPrimary = onHeroFor(true),
        primaryContainer = tone(hue, 0.34f, 0.20f),
        onPrimaryContainer = tone(hue, 0.32f, 0.88f),

        // v423 — the night's second and third fills are muted with the accent
        // (0.46/0.48 of lightness was the same neon on a deep page).
        // v425 — chroma 0.36/0.34 at a lower lightness, and their inks are now
        // MEASURED ([readableOn]) instead of assumed: at these fills white reads
        // 4.97:1 at its worst (Sand's second), so white is what they carry.
        secondary = tone(second, 0.36f, 0.34f),
        onSecondary = readableOn(tone(second, 0.36f, 0.34f), bodyFor(true)),
        secondaryContainer = tone(second, 0.34f, 0.20f),
        onSecondaryContainer = tone(second, 0.32f, 0.88f),

        tertiary = tone(third, 0.34f, 0.36f),
        onTertiary = readableOn(tone(third, 0.34f, 0.36f), bodyFor(true)),
        tertiaryContainer = tone(third, 0.32f, 0.20f),
        onTertiaryContainer = tone(third, 0.30f, 0.88f),

        background = pageFor(true),
        onBackground = bodyFor(true),

        surface = pageFor(true),
        onSurface = bodyFor(true),
        surfaceVariant = tone(hue, 0.24f, 0.15f),
        onSurfaceVariant = tone(hue, 0.20f, 0.72f),

        // The night ladder: small, CALM steps up the jewel page (the stride the
        // app's own dark scheme uses), so a card reads as a plate rather than a
        // smudge while the page keeps the theme's colour. v421 — the hold is
        // lower than the light ladder's and the hue saturates less hard: at the
        // top of the old ladder the deepest card sat close enough to a bright
        // accent ink that the two read as one smudge.
        // v423 — the same ladder rule as the light twin (see its own note): five
        // steps at one chroma and 0.03 of lightness apart read as ONE shade of
        // theme, which is what a page of settings cards looked like. The stride
        // is wider and the chroma eases as the card climbs, so a page has planes.
        // v425 — the whole ladder drops 0.045 of lightness and eases its chroma
        // further (this is the member's settings/profile card complaint). The
        // stride is unchanged at 0.03, so nesting is exactly as legible as it
        // was — one shade calmer. The TOP step is what the accent ink is measured
        // against; at 4.64:1 worst-case it clears the 4.5 bar there now.
        surfaceContainerLowest = tone(hue, 0.24f, 0.135f),
        surfaceContainerLow = tone(hue, 0.22f, 0.165f),
        surfaceContainer = tone(hue, 0.20f, 0.195f),
        surfaceContainerHigh = tone(hue, 0.18f, 0.225f),
        surfaceContainerHighest = tone(hue, 0.16f, 0.255f),

        error = tone(6f, 0.50f, 0.60f),
        onError = tone(6f, 0.46f, 0.14f),
        errorContainer = tone(6f, 0.38f, 0.24f),
        onErrorContainer = tone(6f, 0.36f, 0.88f),

        outline = tone(hue, 0.24f, 0.30f),
        outlineVariant = tone(hue, 0.22f, 0.20f),
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

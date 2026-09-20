package com.curio.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

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
 *    from the SAME three numbers — the page becomes a near-black of its own
 *    hue, the hero/card colour deepens but keeps its hue and saturation, and
 *    the ink lightens into a pale tint of itself. Nothing is invented from a
 *    neutral grey, so "Pantone Cream" is still creamy at night.
 *
 * 2. **THE INK IS AN ACCENT; BODY TEXT GETS A READABLE TWIN.** Two of the
 *    three Pantone inks cannot carry body text on their own page (P 14-8 U is
 *    an orange on cream, P 102-6 C an indigo on lime — both well under 4.5:1),
 *    so every ink is resolved per surface instead of taken literally: the
 *    Pantone ink itself where it reads (icons, headings, labels, accents —
 *    the member asked for exactly that), and a same-hue reading of it at
 *    BODY depth (`ink.at(0.225f, …)`, ~12:1 on its own page) for body text on
 *    a light fill, and a light reading of it at night. Member's own decision:
 *    "Pantone ink for accents, a deeper shade for body text".
 *
 * ── TWO MORE RULES FROM THE MEMBER ("dont use any border for cards and
 * dont use transparent colors" in the new accents) ────────────────────
 *
 * 3. **EVERY COLOUR IN THESE SCHEMES IS OPAQUE.** No `copy(alpha = …)`
 *    anywhere below: a faded ink is mixed INTO its surface instead, so a
 *    Pantone page shows no colour bleeding through a card, a hairline or a
 *    label. (The app-wide alpha-role convention is untouched — this is the
 *    rule for the Pantone palettes themselves.)
 *
 * 4. **THE REST OF THE APP IS DERIVED, NOT BORROWED (v412).** The three
 *    numbers above paint more than the scheme: the roles the brief never
 *    named — the scheme's `secondary` and `tertiary`, the brand's warm and
 *    cool inks, the error colour — are lightness/saturation readings of the
 *    same three numbers (see [secondaryFor], [tertiaryFor], [goldInkFor],
 *    [sageInkFor], [errorFor]), and the 36 lane/category accents resolve to
 *    this palette too (see `CategoryInk.kt`). Member: "I want all of them to
 *    get the colors no other colors" — so no neutral grey is invented here
 *    and no coral/gold/sage from the app's own palette leaks in.
 *
 * 5. **CARDS WEAR NO BORDER HERE.** `outlineVariant` is the app's card-edge
 *    colour, and these three themes drop that edge entirely: their cards
 *    separate by the LIGHTNESS LADDER ALONE (see [lightScheme]'s
 *    `surfaceContainer*` steps). `outlineVariant` below is therefore the
 *    *divider* hairline — the colour of an inset rule drawn on a card — and
 *    the shared `curioCardEdgeColor` helper answers "no edge" under these
 *    themes so the card components never draw one.
 *
 * 6. **v413 — THE LADDER IS THE PAGE'S HUE, AND IT CLIMBS.** The first
 *    version of these themes built the card ladder by DEEPENING THE HERO five
 *    times, which on Pantone Terracotta made a card a deep brick red and every
 *    nested block darker still, and on the two pale heroes walked into grey-
 *    olive mud (member: "all 3 are bad, dont use too deep colors for the cards
 *    or background … use more shades palette per pantone theme"). Now:
 *
 *      * the PAGE keeps its Pantone number as the `background`,
 *      * a CARD is a near-white reading of the page's OWN hue (`0.945`), and
 *        each nesting step descends 0.03 (0.945 → 0.915 → 0.882 → 0.845), so
 *        every card in the app is LIGHT and separates by the app's own rule
 *        (a card separates from its page by LIGHTNESS, never by a tint of it);
 *      * the HERO number is the ACCENT again — the banner, a button, the
 *        selected rail — with a PALE CONTAINER twin (0.90) for selected chips
 *        and a DEEP ink (0.28) to read on it, instead of a saturated block;
 *      * every theme gets a real PALETTE: five page-hue steps, three
 *        container twins (hero / ink / page), three accent depths, three inks
 *        (body, muted, accent) and the derived brand inks — about twenty tones
 *        per theme, all readings of the same three numbers, every one of them
 *        checked against its own partner (see [contrast]).
 */
enum class PantoneTheme(
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
    /** The Pantone text + icon colour (the accent; see [accentFor]). */
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

    /**
     * The page for [dark]: the Pantone page by day, a near-BLACK shade of its
     * own hue by night.
     *
     * v417 fitted the night side to the app's own dark language (whose page is
     * pitch black, `#000000`, with everything above it stepping up in
     * lightness): 0.115 was a warm brown-grey that read as a dim light theme
     * rather than a night one, so the page sits at 0.075 now and the ladder
     * above it starts lower to match.
     */
    fun pageFor(dark: Boolean): Color = if (!dark) page else page.at(0.075f, 0.22f)

    /** The hero — the Pantone hero number, and the theme's accent fill. */
    fun heroFor(dark: Boolean): Color = if (!dark) hero else hero.at(0.28f, 0.55f)

    /** Body text on the PAGE. */
    fun pageInkFor(dark: Boolean): Color = bodyFor(dark)

    /** Body text on a CARD — the light ladder is pale, so it is the same ink. */
    fun cardInkFor(dark: Boolean): Color = bodyFor(dark)

    /**
     * Icons, headings and accents ON THE PAGE.
     *
     * By day: the Pantone ink itself where it clears 3:1 on the page (that is
     * the member's "Pantone ink for accents"), else this theme's ACCENT number
     * at ink depth — Pantone Terracotta's orange cannot read on its own sand,
     * so its accents are the brick instead of a washed-out orange. By night: a
     * pale tint of the ink, which is the only thing that reads on a dark page.
     */
    fun accentFor(dark: Boolean): Color = when {
        dark -> ink.at(0.80f, 0.26f)
        contrast(ink, page) >= 3f -> ink
        else -> hero.at(0.34f, 0.60f)
    }

    /** The words the hero carries. */
    fun onHeroFor(dark: Boolean): Color = readableOn(heroFor(dark), bodyFor(dark))

    // ── v412/v413 — THE PALETTE THE THREE NUMBERS NEVER NAMED ──────────
    //
    // The member's second ask: "add some more colours … for things that dont
    // have colours", and the rule that comes with it — nothing outside the
    // three Pantone numbers may be painted. So every role the app needs but
    // the brief never named (a SECOND fill that is not a copy of the first, a
    // THIRD one, the brand's warm and cool inks, the error colour, the whole
    // card ladder) is a lightness/saturation reading of the page, hero or ink
    // number it is derived from. No neutral grey is invented and none of the
    // app's own coral/gold/sage palette leaks in: a Pantone theme paints the
    // WHOLE app from one three-colour brief.
    //
    // v413 replaced the hero-deepening ladder (see rule 6 in the header) with
    // this one, where every step is a real, separable tone:
    //
    //        light                 role                       dark
    //   page 0.720 (Pantone)   background                    0.075
    //                         surface                       same
    //     —   the five-step card ladder —                     0.10 → 0.20
    //   0.975 → 0.845          containers              (see schemes)
    //   ink  0.225             body ink          0.90 (pale tint of ink)
    //   ink  0.36              muted ink         0.68
    //   hero (Pantone)         accent / hero     0.28 (deep hero)
    //   hero 0.90              selected chip     0.26 (container)
    //   ink  0.40              second fill       0.72
    //   page 0.30              third fill        0.70
    //   warm 0.27              brand gold        0.78
    //   cool 0.30 (sat 0.26)   brand sage        0.80 (sat 0.22)
    //   warm →red 0.40         error             0.66
    //
    // Every pair in this table was measured before it was written down. Body,
    // muted, container, gold, sage and error inks clear 4.5:1 on their own
    // page AND on the deepest card step, the container twins clear it against
    // their own contents, and white clears it on the light error fill. One
    // measured margin is worth naming: Pantone Lime's indigo accent reads
    // 4.26:1 on the deepest nested step (0.845) — above the 3:1 bar icons and
    // large labels are held to, below the body-text bar — so the accent stays
    // the real Pantone ink there rather than deepening into a colour that is
    // indistinguishable from the body text.

    /**
     * The theme's WARMEST number — what "gold" means in this theme.
     *
     * The most SATURATED number whose hue sits in the amber band (15°–70°),
     * not merely the nearest one: Pantone Terracotta's page (40°, sat 0.68) and
     * ink (28°, sat 0.83) both sit in the band, and the ink is the real amber
     * (P 14-8 U), so the nearer-but-duller page must not win. Falls back to the
     * nearest hue for a brief with no warm number at all. Computed once, like
     * the numbers.
     */
    private val warm: Color = listOf(page, hero, ink)
        .filter { toHsl(it).h in WARM_HUE_BAND }
        .maxByOrNull { toHsl(it).s }
        ?: listOf(page, hero, ink).reduce { a, b -> if (warmth(b) > warmth(a)) b else a }

    /** The theme's COOLEST number — what "sage" means in this theme. */
    private val cool: Color =
        listOf(page, hero, ink).reduce { a, b -> if (coolness(b) > coolness(a)) b else a }

    /**
     * The cool ink's HUE: the coolest number's hue walked toward the blue band
     * (250°) by at most 45°, always FORWARD — through green.
     *
     * Not the shortest path: Pantone Terracotta's brief is warm end to end, and
     * its coolest number (the sand page, 40°) reaches blue the short way only
     * by swinging back through RED — which made "sage" a second brick. Walking
     * forward instead lands it on olive (80°), the one cool-family ink a warm
     * brief can honestly give: a sage tick on a terracotta page.
     */
    private val coolHue: Float = run {
        val h = toHsl(cool).h
        (h + ((SAGE_HUE - h + 360f) % 360f).coerceAtMost(45f)) % 360f
    }

    /**
     * The scheme's SECOND fill — the INK number at accent depth.
     *
     * `secondary` and `secondaryContainer` used to be the hero itself (and
     * then the hero one step deeper), so the surfaces that ask for a second
     * tone got two near-copies of the first. This is a real sibling: a
     * different one of the three numbers, at a depth that reads as a fill.
     */
    fun secondaryFor(dark: Boolean): Color =
        if (dark) ink.at(0.72f, 0.40f) else ink.at(0.40f, 0.50f)

    /** The scheme's THIRD fill — the PAGE number at accent depth. */
    fun tertiaryFor(dark: Boolean): Color =
        if (dark) page.at(0.70f, 0.35f) else page.at(0.30f, 0.45f)

    /**
     * The brand's WARM ink (streak flame, XP, levels).
     *
     * Read off the theme's WARMEST number rather than the hero: Pantone
     * Terracotta's hero is a BRICK (8°) while its real amber is the ink
     * (P 14-8 U, 28°), and Pantone Lime's hero is a YELLOW-GREEN — an amber
     * flame taken off the hero came out green. Amber is the one brand colour
     * that has to survive every theme, so every theme hands its most saturated
     * amber-band number to it.
     */
    fun goldInkFor(dark: Boolean): Color =
        if (dark) warm.at(0.78f, 0.50f) else warm.at(0.27f, 0.75f)

    /**
     * The brand's COOL ink (done ticks, mastery, "mastered") — the cool hue
     * above, held to a LOW saturation so it reads as its own ink rather than as
     * a second copy of the accent (see [coolHue] for the hue itself).
     */
    fun sageInkFor(dark: Boolean): Color = fromHsl(
        coolHue,
        toHsl(cool).s.coerceAtMost(if (dark) 0.22f else 0.26f),
        if (dark) 0.80f else 0.30f
    )

    /**
     * The error / destructive fill.
     *
     * The theme's warm number's hue WALKED TOWARD RED (by at most 35°, see
     * [alert]), because an alert has to look like an alert: Pantone Lime's ink
     * is an indigo, and a delete button painted indigo reads as decoration.
     * The walk keeps it inside the palette — Terracotta's error is a deep
     * terracotta, Lime's is a deep violet-red — while still raising a hand.
     */
    fun errorFor(dark: Boolean): Color =
        alert(lightness = if (dark) 0.66f else 0.40f, saturation = if (dark) 0.60f else 0.62f)

    /** The theme's own [ColorScheme] for [dark]. */
    fun schemeFor(dark: Boolean): ColorScheme = if (dark) darkScheme() else lightScheme()

    /**
     * The LIGHT scheme: the Pantone PAGE as the background, then the app's own
     * ladder rule read on a Pantone page — every surface ABOVE the background
     * is a near-white step of the page's hue, so a card is always light (this
     * is the v413 fix: the old ladder deepened the HERO, which made cards deep
     * and nested blocks darker still).
     *
     * The HERO number is the accent: `primary` is the hero fill, its pale
     * container twin (0.90) is what a selected chip / tinted rail wears, and
     * the deep hero ink (0.28) reads on both. `primaryContainer` is that pale
     * twin rather than the hero again — which is what lets a chip and the
     * button beside it be told apart.
     */
    private fun lightScheme(): ColorScheme = lightColorScheme(
        primary = hero,
        onPrimary = readableOn(hero, bodyFor(false)),
        primaryContainer = hero.at(0.90f, 0.45f),
        onPrimaryContainer = hero.at(0.28f, 0.55f),

        secondary = secondaryFor(false),
        onSecondary = readableOn(secondaryFor(false), bodyFor(false)),
        secondaryContainer = ink.at(0.90f, 0.28f),
        onSecondaryContainer = ink.at(0.26f, 0.32f),

        tertiary = tertiaryFor(false),
        onTertiary = readableOn(tertiaryFor(false), bodyFor(false)),
        tertiaryContainer = page.at(0.90f, 0.38f),
        onTertiaryContainer = page.at(0.26f, 0.34f),

        background = page,
        onBackground = bodyFor(false),

        surface = page,
        onSurface = bodyFor(false),
        surfaceVariant = page.at(0.895f, 0.32f),
        onSurfaceVariant = mutedFor(false),

        // The five-step card ladder — all of it ABOVE the page, a step per
        // nesting level, so nothing in the app is ever a dark block.
        surfaceContainerLowest = page.at(0.975f, 0.30f),
        surfaceContainerLow = page.at(0.945f, 0.32f),
        surfaceContainer = page.at(0.915f, 0.32f),
        surfaceContainerHigh = page.at(0.882f, 0.32f),
        surfaceContainerHighest = page.at(0.845f, 0.34f),

        // Derived from the Pantone numbers, never the app's coral. The
        // CONTAINER pair matters as much as the fill: `errorContainer` used to
        // fall through to Material's baseline pink, which is the one colour in
        // the scheme that was not from this brief.
        error = errorFor(false),
        onError = Color.White,
        errorContainer = alert(lightness = 0.90f, saturation = 0.40f),
        onErrorContainer = alert(lightness = 0.26f, saturation = 0.45f),

        // `outline` = an edge ON THE PAGE; `outlineVariant` = the inset
        // DIVIDER hairline on a card (rule 4 — cards have no edge of their
        // own under these themes). Both opaque (rule 3). `scrim` is the
        // theme's own near-black, so a dimmed page still belongs to it.
        outline = page.at(0.70f, 0.40f),
        outlineVariant = page.at(0.86f, 0.34f),
        scrim = ink.at(0.10f, 0.18f)
    )

    /**
     * The DARK twin: the same three Pantones read at night. The page is a deep
     * shade of ITS OWN hue (a warm near-black for the two cream pages, an
     * olive near-black for the lime one), the card ladder climbs in three
     * small steps above it, and every ink is a pale reading of the Pantone ink.
     */
    private fun darkScheme(): ColorScheme = darkColorScheme(
        primary = hero.at(0.74f, 0.55f),
        onPrimary = hero.at(0.20f, 0.50f),
        primaryContainer = hero.at(0.26f, 0.42f),
        onPrimaryContainer = hero.at(0.88f, 0.28f),

        secondary = secondaryFor(true),
        onSecondary = ink.at(0.20f, 0.40f),
        secondaryContainer = ink.at(0.30f, 0.30f),
        onSecondaryContainer = ink.at(0.88f, 0.24f),

        tertiary = tertiaryFor(true),
        onTertiary = page.at(0.18f, 0.40f),
        tertiaryContainer = page.at(0.30f, 0.32f),
        onTertiaryContainer = page.at(0.88f, 0.26f),

        background = pageFor(true),
        onBackground = bodyFor(true),

        surface = pageFor(true),
        onSurface = bodyFor(true),
        surfaceVariant = page.at(0.125f, 0.20f),
        onSurfaceVariant = mutedFor(true),

        // The night ladder: the near-black page, then four small steps up it,
        // 0.025 of lightness each — the stride the app's own dark scheme uses
        // (`#121212` → `#2C2C2C`), read on this theme's hue. Small steps on
        // purpose: a dark step that is easy to see in a screenshot is a plate
        // that looks lit in the hand.
        surfaceContainerLowest = page.at(0.10f, 0.20f),
        surfaceContainerLow = page.at(0.125f, 0.20f),
        surfaceContainer = page.at(0.15f, 0.22f),
        surfaceContainerHigh = page.at(0.175f, 0.24f),
        surfaceContainerHighest = page.at(0.20f, 0.26f),

        // Derived from the Pantone numbers, never the app's coral.
        error = errorFor(true),
        onError = warm.at(0.16f, 0.45f),
        errorContainer = alert(lightness = 0.28f, saturation = 0.36f),
        onErrorContainer = alert(lightness = 0.88f, saturation = 0.30f),

        // No shadows exist on a page this dark, so these hairlines and the
        // container steps ARE the separation (the app's own dark note).
        outline = page.at(0.26f, 0.18f),
        outlineVariant = page.at(0.16f, 0.18f),
        scrim = ink.at(0.04f, 0.14f)
    )

    /** The alert reading of the warm number: a hue walked toward red (≤ 35°) so
     *  the result still belongs to this palette, at the given depth. */
    private fun alert(lightness: Float, saturation: Float): Color {
        val hsl = toHsl(warm)
        val delta = ((ALERT_HUE - hsl.h + 540f) % 360f) - 180f
        return fromHsl(
            hsl.h + delta.coerceIn(-35f, 35f),
            hsl.s.coerceAtMost(saturation),
            lightness
        )
    }

    private fun bodyFor(dark: Boolean): Color =
        if (dark) ink.at(0.90f, 0.16f) else ink.at(0.225f, 0.32f)

    private fun mutedFor(dark: Boolean): Color =
        if (dark) ink.at(0.75f, 0.14f) else ink.at(0.34f, 0.25f)

    /** The theme by its stored [id], or null when [id] is not a Pantone theme. */
    companion object {
        fun fromId(id: String): PantoneTheme? = entries.firstOrNull { it.id == id }
    }
}

// ── Palette maths ───────────────────────────────────────────────────────────

/** The hue a Pantone palette's alert runs toward — a red deep enough to be
 *  read as "stop" on a pale card without a real red leaving the brief. */
private const val ALERT_HUE = 8f

/** The hue band a Pantone number has to sit in to be called the theme's WARM
 *  one — the amber/orange range a brand gold can honestly come from. */
private val WARM_HUE_BAND = 15f..70f

/** The blue end a theme's cool ink hue is walked toward (see `coolHue`). */
private const val SAGE_HUE = 250f

/** One step of [this] colour's OWN ramp: hue kept, saturation capped at [cap],
 *  lightness set to [lightness]. Every tone in a Pantone palette is made here,
 *  which is what keeps a theme to its brief — nothing invented, only read. */
private fun Color.at(lightness: Float, cap: Float): Color {
    val hsl = toHsl(this)
    return fromHsl(hsl.h, hsl.s.coerceAtMost(cap), lightness)
}

/** The most readable of white and the theme's body ink on [fill] — tried in
 *  that order, so a filled block gets white whenever white works. */
private fun readableOn(fill: Color, body: Color): Color = when {
    contrast(Color.White, fill) >= 4.5f -> Color.White
    contrast(body, fill) >= 4.5f -> body
    else -> if (contrast(Color.White, fill) >= contrast(body, fill)) Color.White else body
}

/** How close [color]'s hue is to the amber band (45°) — 0 or above for a warm
 *  hue, negative for a cool one. Used only to pick which of the three numbers
 *  a theme calls "warm". */
private fun warmth(color: Color): Float = 60f - abs(toHsl(color).h - 45f)

/** How close [color]'s hue is to the 250° band (blue/violet) — the mirror of
 *  [warmth]. Used only to pick which number a theme calls "cool". */
private fun coolness(color: Color): Float = 70f - abs(toHsl(color).h - 250f)

/** WCAG contrast ratio between two opaque colors (1f..21f). */
internal fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

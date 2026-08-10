package com.curio.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CurioCategory

/**
 * Theme-aware ink color for a category's accent-colored TEXT and ICONS that
 * sit on plain surfaces.
 *
 * The researched category accents (Tailwind-700 depth) read beautifully as
 * *fills* — cards, chips, buttons, gradients — with white content on top.
 * But used as *ink* on the midnight dark surfaces they fall below readable
 * contrast (e.g. indigo-700 text on #111722 ≈ 1.9:1). Each category pairs
 * its deep accent with a light 300-level twin ([CurioCategory.lightAccent]);
 * this extension resolves the correct one for the active theme, so accent
 * text/icons stay readable in both light and dark mode.
 */
@Composable
fun CurioCategory.categoryInk(): Color {
    // The DEEP accent (not themedAccent) in light mode: pastel mode softens
    // themedAccent but text/icons on plain surfaces must stay deep to read.
    if (isCurioDarkTheme()) return lightAccent
    // v7.5 — pastel mode: accents that are already pale (the mixed-deck
    // blend, the wildcard coral) can't serve as their own ink on the light
    // surfaces — a pastel-on-pastel would wash out (e.g. a mixed deck's
    // non-selected pills). Return a deep twin of the same hue, mirroring
    // [onAccent]'s pale-accent rule. Deep accents stay themselves.
    if (AppPreferences.pastelColorsState && accent.isPale()) return deepHueInk(accent)
    return accent
}

/**
 * The accent color a category WEARS in the active theme style.
 *
 *  - Curio (default) and AMOLED: the researched accent unchanged — category
 *    identity stays exact.
 *  - Material: the category accent is used UNCHANGED too. It used to be
 *    blended ~40% toward the device's dynamic Material primary so accents
 *    would "read as a shade of the device palette" — but an RGB lerp toward
 *    a different-hue primary does NOT keep the hue: with a blue wallpaper
 *    the red (Movies) accent turned purple, amber turned mauve, and with an
 *    orange wallpaper teal and sky sank to grey-olive — every blended
 *    gradient in the Material style looked muddy and wrong. The Material
 *    style now keeps its device identity through the dynamic color scheme
 *    (surfaces, backgrounds, controls come from the wallpaper) while every
 *    category keeps its true researched color, so cards, heroes and
 *    gradients stay vivid and recognizable.
 */
@Composable
fun CurioCategory.themedAccent(): Color {
    // Pastel color mode (v7.5) — every accent softens to its pastel twin so
    // fills, gradients, chips and the mixed-deck blends all read pastel. The
    // twin is theme-aware: an airy pastel on the cream surface in light mode,
    // a muted deep pastel over midnight in dark mode. Content on these fills
    // flips to [onAccent] (deep ink in light, light twin in dark).
    if (!AppPreferences.pastelColorsState) return accent
    return pastelAccent(accent, isCurioDarkTheme())
}

/**
 * v8.28 — text/icon ink for category accents on PLAIN surfaces in EVERY
 * light theme: like [categoryInk], but pale accents (e.g. the wildcard
 * coral, which is pastel by nature) get their deep hue twin even when
 * pastel mode is OFF. Used by small label text and glyphs (quest passport
 * stamps, saved bookmarks) that must never wash out in plain light mode
 * either. Dark mode still resolves the light twin, exactly like
 * [categoryInk].
 */
@Composable
fun CurioCategory.readableAccentInk(): Color {
    if (isCurioDarkTheme()) return lightAccent
    return if (accent.isPale()) deepHueInk(accent) else accent
}

/**
 * Ink color for content sitting ON an accent fill (buttons, selected cards,
 * chips, hero gradients).
 *
 * Deep accents carry white content (the pre-pastel contract). In pastel mode
 * the fills lighten, so light mode flips the ink to the DEEP accent (dark ink
 * on the pastel fill) and dark mode to the light twin (light ink on the muted
 * pastel) — the same dark/light resolution as [categoryInk]. Returns White
 * when pastel mode is off, so existing call sites keep their exact look.
 */
@Composable
fun CurioCategory.onAccent(): Color = when {
    AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED ->
        MaterialTheme.colorScheme.onSurface
    // v12 — Material wears the same rich category fills as the rest of the
    // app (cards, heroes, chips), so its content ink is the same pastel-aware
    // ink as [cardContentInk]/[themedButtonInk]: white on deep accents,
    // deep same-hue ink on airy pastels. Pale accents (wildcard coral) get
    // their deep hue twin even off pastel mode, mirroring [categoryInk]. The
    // old scheme onPrimaryContainer (a device-hue role) read muddy or lost
    // on the category fills.
    AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL ->
        if (accent.isPale()) deepHueInk(accent) else pastelFillInk(themedAccent())
    !AppPreferences.pastelColorsState -> Color.White
    isCurioDarkTheme() -> lightAccent
    // The wildcard's accent is ALREADY a pastel pink — a deep hue twin (the
    // brand maroon) reads on it, not the pale accent itself.
    accent.isPale() -> CurioColors.DeepPlum
    else -> accent
}

/**
 * v12 — fill for CATEGORY ACCENT BUTTONS (deck control pills, CTAs, the
 * reveal CTA): every style wears the TRUE category accent (pastel-aware), so
 * Material buttons carry the same rich category identity as the cards
 * instead of a muddy device+accent blend. AMOLED plates override to black
 * via [com.curio.app.ui.components.curioButtonColors] where used.
 */
@Composable
fun CurioCategory.themedButtonFill(): Color = themedAccent()

/** Content ink for [themedButtonFill] — pastel-aware like the card fills. */
@Composable
fun CurioCategory.themedButtonInk(): Color = when (AppPreferences.themeStyleState) {
    AppPreferences.THEME_STYLE_MATERIAL ->
        if (accent.isPale()) deepHueInk(accent) else pastelFillInk(themedAccent())
    else -> onAccent()
}

/**
 * Ink for content sitting on a category card's fill in the active theme
 * style. Material (v12) wears the same rich category fills as the rest of
 * the app, so its content ink is the same pastel-aware ink as the Curio
 * cards (white on deep, deep on airy pastel); AMOLED cards sit on black
 * glass so their content pairs with the theme onSurface. Curio default
 * resolves [onAccent].
 */
@Composable
fun CurioCategory.cardContentInk(): Color = when (AppPreferences.themeStyleState) {
    AppPreferences.THEME_STYLE_AMOLED -> MaterialTheme.colorScheme.onSurface
    // v12 — Material cards wear the category fills now, so their content
    // ink is the same pastel-aware ink as the Curio-style cards (white on
    // deep, deep on airy pastel) instead of the device onPrimary. Pale
    // accents (wildcard coral) get their deep hue twin off pastel mode too.
    AppPreferences.THEME_STYLE_MATERIAL ->
        if (accent.isPale()) deepHueInk(accent) else pastelFillInk(themedAccent())
    else -> onAccent()
}

/**
 * Ink that reads on an accent FILL in pastel mode — used by deck surfaces
 * that don't map 1:1 to a category (mixed-deck blends, peek cards, the spin
 * button). Deep fills keep white; pastel light-mode fills get a deep ink of
 * the SAME hue; muted dark pastels get a light tint. Returns White when
 * pastel mode is off, preserving today's look exactly.
 */
@Composable
fun pastelFillInk(fill: Color): Color = when {
    !AppPreferences.pastelColorsState -> Color.White
    isCurioDarkTheme() -> lerp(fill, Color.White, 0.85f)
    else -> {
        val a = toHsl(fill)
        fromHsl(a.h, a.s.coerceIn(0.15f, 0.60f), 0.30f)
    }
}

/**
 * Non-composable twin of [CurioCategory.categoryInk] — the exact same
 * resolution, parameterized by the two states that drive it (pastel mode +
 * dark theme). The watermark backdrops use these inside `remember` blocks
 * (whose calculation lambdas are @DisallowComposableCalls), so the 11-color
 * accent map can be cached per theme change instead of rebuilt on every
 * recomposition of animated screens. v7.94.
 */
internal fun CurioCategory.categoryInkFor(pastel: Boolean, dark: Boolean): Color = when {
    dark -> lightAccent
    pastel && accent.isPale() -> deepHueInk(accent)
    else -> accent
}

/**
 * Non-composable twin of [CurioCategory.themedAccent] — same resolution,
 * parameterized by pastel mode + dark theme (see [categoryInkFor]).
 */
internal fun CurioCategory.themedAccentFor(pastel: Boolean, dark: Boolean): Color =
    if (!pastel) accent else pastelAccent(accent, dark)

/** Whether a color is pale enough to need a deep ink twin instead of itself. */
private fun Color.isPale(): Boolean {
    val lum = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return lum > 0.50f
}

/**
 * Deep ink of the SAME hue as a pale/pastel color — the light-mode ink twin
 * used by [categoryInk] (and equivalent to [pastelFillInk]'s light branch,
 * but unconditional so it never resolves to white). Shared with the Spin
 * screen's orbit dots, which deepen non-pastel light accents the same way.
 */
internal fun deepHueInk(color: Color): Color {
    val a = toHsl(color)
    return fromHsl(a.h, a.s.coerceIn(0.15f, 0.60f), 0.30f)
}

/**
 * Theme-aware wash color for a category-aware page BACKGROUND (Spin, Topic
 * Reveal, Save/Capture, Cabinet filter).
 *
 * Light mode: a hue-preserving pastel of the accent (see [lightAccentTint]) —
 * airy like the cream paper, but built from the accent's OWN hue family. The
 * old cream-blended recipe let cream's warm hue dominate, so cool accents
 * drifted off-family (teal/sky pages washed grey-GREEN and the detail hero's
 * glide swung through green/yellow); the on-hue tint keeps the page AND the
 * hero's fade-into-it exactly on the category's color story. The pastel is
 * strong enough to READ (0.32/0.85 — see [lightAccentTint]) so a red page, a
 * teal page and a sky page are visibly different and the hero's blend ends
 * on the real category color, never a near-white beige.
 *
 * Dark mode: the deep Tailwind-700 accent alone at 20% reads muddy (amber
 * goes brown, teal goes grey-green), while its light 300-level twin at any
 * useful fraction reads WHITE-WASHED over the midnight surface. So this
 * builds a saturated mid-tone — the accent lerped partway toward its light
 * twin (≈ the 500-level shade) — and washes it at a moderate fraction. The
 * page keeps the category's hue with real color, never a washed-out grey-white.
 *
 * A few families need extra contrast: at the default 50% midpoint, rose
 * (movies), sky (science), amber (books — brown) and especially coral
 * (wildcard — its accent is already a pastel pink) read too
 * pale/white-washed. Those pull the mid-tone closer to the deep accent
 * (or a deep hue twin, for pale accents) and blend a touch stronger, so
 * the hue survives over midnight instead of flattening to grey-white.
 */
@Composable
fun CurioCategory.categoryBackgroundWash(): Color {
    val background = MaterialTheme.colorScheme.background
    // Settings toggle (v6.4): when the category tint is turned off, pages use
    // the plain theme background (cream in light, midnight in dark) exactly
    // as they did before the wash rollout.
    if (!AppPreferences.tintWashEffective()) return background
    return if (isCurioDarkTheme()) {
        val tuning = DARK_WASH_TUNING[family] ?: DEFAULT_DARK_WASH
        // v7.5 — pastel mode: the mid-tone is built from the muted deep
        // pastel accent instead of the deep accent, so dark pages read
        // pastel too instead of deep jewel tones.
        val accentBase = if (AppPreferences.pastelColorsState) pastelAccent(accent, true) else accent
        val midTone = tuning.resolveMidTone(accentBase, lightAccent)
        lerp(background, midTone, tuning.blendFraction)
    } else {
        // v7.5 — pastel mode: a whisper pastel page (lighter + less
        // saturated than the standard wash) so the airy pastel fills pop
        // instead of melting into the background.
        if (AppPreferences.pastelColorsState) lightAccentTint(accent, saturation = 0.20f, lightness = 0.90f)
        else lightAccentTint(accent)
    }
}

/**
 * Theme-aware surface color for CARDS that sit on a tinted page background.
 *
 * Cards used plain theme surfaces (cream in light, midnight grey in dark),
 * which look out of place sitting on a category-tinted page. This resolves
 * the same per-family mid-tone as [categoryBackgroundWash] but blends a
 * little stronger (markedly stronger in dark mode, where the wash stays
 * deep), so a card reads as a tinted elevated surface instead of a foreign
 * cream block. Honors the Settings tint toggle — when it's off, [base] is
 * returned unchanged so cards go back to the plain theme surface.
 */
@Composable
fun CurioCategory.categorySurface(base: Color = MaterialTheme.colorScheme.surfaceContainerLow): Color {
    if (!AppPreferences.tintWashEffective()) return base
    return if (isCurioDarkTheme()) {
        val tuning = DARK_WASH_TUNING[family] ?: DEFAULT_DARK_WASH
        val accentBase = if (AppPreferences.pastelColorsState) pastelAccent(accent, true) else accent
        val midTone = tuning.resolveMidTone(accentBase, lightAccent)
        // Dark cards blend the proper dark mid-tone much harder than the
        // page wash (which stays deep) — same "cards = wash's stronger
        // sibling" relationship as light mode — so tiles and chips visibly
        // wear their category tint on the midnight page instead of sinking
        // into a near-invisible +0.10 whisper. In pastel mode the mid-tone
        // is the muted pastel twin, so the cards read soft pastel too.
        lerp(base, midTone, tuning.blendFraction + 0.30f)
    } else {
        lightSurfaceTint(accent)
    }
}

/**
 * The mood board's tinted canvas — same resolution as [categorySurface]
 * but NOT gated by the theme STYLE: the AMOLED style blacks out category
 * tints app-wide, and the mood board's tinted surface is its identity, so
 * it keeps wearing the category mid-tone even on the pure-black style.
 * The manual Settings tint toggle is still honored — turning it off here
 * returns [base] unchanged just like [categorySurface].
 */
@Composable
fun CurioCategory.categorySurfaceMoodBoard(base: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color {
    if (!AppPreferences.tintWashEnabledState) return base
    return if (isCurioDarkTheme()) {
        val tuning = DARK_WASH_TUNING[family] ?: DEFAULT_DARK_WASH
        val accentBase = if (AppPreferences.pastelColorsState) pastelAccent(accent, true) else accent
        val midTone = tuning.resolveMidTone(accentBase, lightAccent)
        lerp(base, midTone, tuning.blendFraction + 0.30f)
    } else {
        lightSurfaceTint(accent)
    }
}

/**
 * Theme-aware surface color for SMALL CATEGORY CHIPS (Cabinet filter pills).
 *
 * Chips sit directly on the washed page, so they need to read as distinct
 * tappable pills without shouting. Light mode matches [categorySurface]'s
 * soft cream tint. Dark mode deliberately differs from cards: the family
 * mid-tone is desaturated toward a neutral grey (deep accents otherwise
 * read muddy over midnight) and blended a touch stronger than the page wash
 * so the chip LIFTS off the tinted background instead of sinking into it —
 * less saturated, more contrast. The crisp edge comes from
 * [categoryBorder]'s light-twin hairline. Honors the Settings tint toggle —
 * when it's off, [base] is returned unchanged so chips go back to the plain
 * theme surface.
 */
@Composable
fun CurioCategory.categoryChipSurface(base: Color = MaterialTheme.colorScheme.surfaceContainerLow): Color {
    if (!AppPreferences.tintWashEffective()) return base
    return if (isCurioDarkTheme()) {
        val tuning = DARK_WASH_TUNING[family] ?: DEFAULT_DARK_WASH
        val accentBase = if (AppPreferences.pastelColorsState) pastelAccent(accent, true) else accent
        val midTone = tuning.resolveMidTone(accentBase, lightAccent)
        // Pull the mid-tone toward neutral grey (less saturated), then blend
        // harder than the page wash (which uses blendFraction) so the chip
        // reads brighter than the tinted background for contrast.
        val desaturated = lerp(midTone, Color(0xFF9AA3B0), 0.40f)
        lerp(base, desaturated, tuning.blendFraction + 0.40f)
    } else {
        lightSurfaceTint(accent)
    }
}

/**
 * Light-mode surface tint for CARDS and CHIPS — the page wash's stronger
 * sibling: a touch more saturated than [lightAccentTint]'s defaults so the
 * surface reads as an elevated card on the tinted page rather than melting
 * into it (and slightly lighter, so cards lift off the wash). Single
 * definition keeps the three surface families in sync.
 */
private fun lightSurfaceTint(accent: Color): Color =
    // v7.5 — pastel mode: cards/chips stay a touch stronger than the whisper
    // page wash (0.28/0.86) so tinted tiles read elevated under the pastel
    // fills instead of dissolving into the background.
    if (AppPreferences.pastelColorsState) lightAccentTint(accent, saturation = 0.28f, lightness = 0.86f)
    else lightAccentTint(accent, saturation = 0.36f, lightness = 0.86f)

/**
 * Theme-aware border for CARDS and BUTTONS that wear a tinted surface on a
 * tinted page background.
 *
 * Tinted surfaces ([categorySurface], `category.tint`, etc.) sit on a
 * category-washed page, so without a rule they can visually melt into the
 * background. This returns a slim theme-aware edge — deep accent in light
 * mode, light twin in dark (same resolution as [categoryInk]) — at a low
 * alpha so the card/button reads as a distinct surface without a hard line.
 *
 * Honors the Settings tint toggle: when it's off, [fallback] is returned
 * (null by default = no border), so plain-theme pages keep their exact
 * pre-tint look.
 */
@Composable
fun CurioCategory.categoryBorder(fallback: BorderStroke? = null): BorderStroke? {
    if (!AppPreferences.tintWashEffective()) {
        // Material keeps a quiet accent hairline so cards/pills read defined
        // on the hue-neutral surfaces (AMOLED wears its own black-glass
        // borders; Curio-with-tint-off falls back to the caller's default).
        if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL) {
            return BorderStroke(1.dp, categoryInk().copy(alpha = 0.26f))
        }
        return fallback
    }
    return BorderStroke(1.dp, categoryInk().copy(alpha = 0.30f))
}

/**
 * Per-family dark-mode wash tuning.
 *
 * @param midToneFactor How far the mid-tone is pulled from the deep accent
 *   toward its light twin (lower = stays closer to the deep accent = darker).
 * @param blendFraction How strongly the mid-tone is blended over midnight.
 * @param darken Extra darkening of the mid-tone toward [deepTwin] (or black
 *   when no twin is given) — needed for families whose accent is itself
 *   pale (e.g. wildcard coral), where no mid-tone pull can reach a real
 *   shade on its own.
 * @param deepTwin A deeper shade of the same hue to darken toward. Falling
 *   back to black for pale accents (coral) produced a muddy grey-pink over
 *   midnight; a real deep pink twin keeps the hue while going dark.
 */
private class DarkWashTuning(
    val midToneFactor: Float,
    val blendFraction: Float,
    val darken: Float = 0f,
    val deepTwin: Color? = null
) {
    /** The wash mid-tone for this family — deepened toward [deepTwin]/black when tuned. */
    fun resolveMidTone(accent: Color, lightAccent: Color): Color {
        val midTone = lerp(accent, lightAccent, midToneFactor)
        if (darken <= 0f) return midTone
        return lerp(midTone, deepTwin ?: Color.Black, darken)
    }
}

private val DEFAULT_DARK_WASH = DarkWashTuning(0.5f, 0.15f)

private val DARK_WASH_TUNING: Map<CategoryFamily, DarkWashTuning> = mapOf(
    // Rose (movies, red) read whitewashed over midnight — hug the deep
    // accent (low factor) and deepen toward the shared deep-rose twin so
    // the wash is a dark #5E0034 burgundy instead of a pale rose.
    CategoryFamily.MOVIES  to DarkWashTuning(0.10f, 0.22f, darken = 0.60f, deepTwin = Color(0xFF5E0034)),
    // Sky (science, light blue) — slightly darker than the earlier deep-pull:
    // keep the azure hue but nudge the mid-tone a bit toward black so the
    // wash doesn't float pale-blue over midnight.
    CategoryFamily.SCIENCE to DarkWashTuning(0.12f, 0.20f, darken = 0.10f),
    // Amber (books, brown) — the accent is already a warm brown, but the
    // default 50% midpoint pulled it toward its gold twin and washed out.
    // Keep it near the deep amber and deepen toward a dark coffee brown.
    CategoryFamily.BOOKS   to DarkWashTuning(0.15f, 0.22f, darken = 0.35f, deepTwin = Color(0xFF78350F)),
    // Coral (wildcard, pink) is a pastel accent — no mid-tone pull gets it
    // dark, and deepening toward black turned it a muddy grey-pink. Deepen
    // toward the same deep-rose twin as the movies family so the wash is a
    // dark #5E0034 burgundy instead of a pale rose-pink.
    CategoryFamily.WILDCARD to DarkWashTuning(0.10f, 0.24f, darken = 0.60f, deepTwin = Color(0xFF5E0034)),
    // Violet (anime/comics) — hug the deep accent and deepen toward a dark
    // plum so the wash is a midnight violet instead of a floating pale lilac.
    CategoryFamily.ANIME_COMICS to DarkWashTuning(0.12f, 0.20f, darken = 0.35f, deepTwin = Color(0xFF4C1D95)),
    // Fuchsia (games) — deepen toward a dark magenta so it reads jewel-toned.
    CategoryFamily.GAMES to DarkWashTuning(0.12f, 0.20f, darken = 0.35f, deepTwin = Color(0xFF701A75)),
    // Emerald (sports) is already deep — keep it near the accent, deepen
    // toward a dark forest twin so the wash doesn't grey out.
    CategoryFamily.SPORTS to DarkWashTuning(0.15f, 0.22f, darken = 0.30f, deepTwin = Color(0xFF064E3B)),
    // Orange (mythology) — warm and already mid-tone; deepen toward a dark
    // ember brown so the page wash stays rich over midnight.
    CategoryFamily.MYTHOLOGY to DarkWashTuning(0.15f, 0.22f, darken = 0.40f, deepTwin = Color(0xFF7C2D12)),
    // Red (food) — red reads well over dark; hug the accent and deepen
    // toward a dark crimson so it stays saturated.
    CategoryFamily.FOOD to DarkWashTuning(0.12f, 0.22f, darken = 0.35f, deepTwin = Color(0xFF7F1D1D)),
    // Blue (internet culture) — deepen toward a dark navy so the wash is a
    // midnight indigo instead of a pale azure float.
    CategoryFamily.INTERNET to DarkWashTuning(0.12f, 0.20f, darken = 0.30f, deepTwin = Color(0xFF1E3A8A))
)

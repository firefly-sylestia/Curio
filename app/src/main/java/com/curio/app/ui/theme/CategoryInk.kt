package com.curio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
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
    // v81 — dark mode: the LIGHT 300-level twin is the text/icon ink on the
    // pitch-black page (the "lighter color is now the text" reversal).
    if (isCurioDarkTheme()) return lightAccent
    // The DEEP accent (not themedAccent): pastel mode softens themedAccent
    // but text/icons on plain surfaces must stay deep to read.
    // Mid-lightness accents (the new green/lime lanes, the old
    // sky/amber/red/blue families, the pale wildcard coral) can't serve as
    // their own ink on the light surfaces — the washed page and pastel
    // fills drop them below ~4.5:1 (e.g. green text on the green page wash
    // reads ~3.8:1). Return a deep twin of the SAME hue so accent text and
    // icons stay readable; genuinely deep accents (brown, navy, indigo,
    // plum, tech slate…) keep themselves exactly as before.
    return if (accent.needsLightDeepInk()) readableLightInk(accent) else accent
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
    if (!AppPreferences.pastelColorsState) {
        // v81 — dark mode: the researched accent is too deep to read as a
        // FILL on pitch black (~1.9:1), so it resolves to a NEW SHADE of the
        // same hue — a readable dark jewel tone ([darkAccent]). Light mode
        // keeps the exact researched accent.
        return if (isCurioDarkTheme()) darkAccent(accent) else accent
    }
    return pastelAccent(accent, isCurioDarkTheme())
}

/**
 * v27j — the torn-hero HEADER fill accent. Resolves [themedAccent], then
 * deepens it slightly when the "Deeper header color" preference is on
 * (the default). Only the banner fill color changes — watermark glyphs,
 * ink and everything else keep resolving from [themedAccent] exactly as
 * before, so toggling never moves a watermark or a text color, only the
 * painted paper under the torn edge. The darkening is a hue-preserving
 * lightness drop: deep accents stay recognizable, pale pastels (wildcard
 * coral) deepen instead of washing out.
 */
@Composable
fun CurioCategory.headerAccent(): Color {
    val base = themedAccent()
    // v32 — non-pastel category banners were TOO VIVID (blinding) next to
    // the calm pastel headers: pull saturation ~15% so a vivid lane accent
    // reads rich but not neon. Pastel accents are already airy — unchanged.
    // The calming applies even when the Deeper header color toggle is off.
    val calm = if (AppPreferences.pastelColorsState) base else {
        val b = toHsl(base)
        fromHsl(b.h, (b.s * 0.85f).coerceAtMost(0.60f), b.l)
    }
    // v81 — dark mode: the torn hero wears a NEW SHADE of the same color
    // spectrum — a deep, gently desaturated twin (lightness ~0.34) so the
    // banner reads clearly as dark-mode against the pitch-black page while
    // the category hue stays recognizable (never the light shade).
    if (isCurioDarkTheme()) {
        val a = toHsl(calm)
        return fromHsl(a.h, (a.s * 0.85f).coerceAtMost(0.55f), 0.34f)
    }
    if (!AppPreferences.headerDeepState) return calm
    // Hue-preserving deepen: pull lightness down rather than lerping toward
    // black (which would grey the hue). Light mode deepens a touch more so
    // the banner reads a shade richer on the cream page.
    val hsl = toHsl(calm)
    return fromHsl(hsl.h, hsl.s, hsl.l * 0.88f)
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
    // v81 — dark mode resolves the light twin, exactly like [categoryInk].
    if (isCurioDarkTheme()) return lightAccent
    return if (accent.needsLightDeepInk()) readableLightInk(accent) else accent
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
    !AppPreferences.pastelColorsState -> Color.White
    isCurioDarkTheme() -> lightAccent
    // The wildcard's accent is ALREADY a pastel pink — a deep hue twin (the
    // brand maroon) reads on it, not the pale accent itself.
    accent.isPale() -> CurioColors.DeepPlum
    // v27p — pastel light: content on the airy pastel fills flips to a DEEP
    // same-hue ink (the design contract), but the old `accent` as ink still
    // dropped even deep accents below 4.5:1 on the light pastel (indigo
    // 4.1, violet 3.7, green 3.6). The deep twin pins lightness low enough
    // that every category's pastel text passes.
    else -> readableLightInk(accent)
}

/**
 * v28 — ink for HERO HEADER title text/icons sitting on a category banner
 * fill, on EVERY screen: light mode keeps the pastel-aware [onAccent]
 * resolution exactly as before (deep ink on airy pastel fills, white on
 * deep accents), but DARK mode always reads WHITE/creamish — the same
 * cream-white blend the shared rose heroes already use ([pastelFillInk]).
 * A category-tinted hero (Cabinet's active-filter banner, the saved-entry
 * detail hero, Home's hero-tint experiment) must never show its tinted
 * light twin as title text over midnight; the cream keeps the hue whisper
 * while staying crisp light-on-deep. The cream is blended from the themed
 * accent (the banner fill family), mirroring [settingsReadableInk].
 */
@Composable
fun CurioCategory.heroHeaderInk(): Color {
    // v81 — dark mode always reads cream-white on the dark banner (the same
    // blend the shared rose heroes use), never the tinted light twin.
    if (isCurioDarkTheme()) return pastelFillInk(themedAccent())
    // Light: the pastel-aware [onAccent].
    return onAccent()
}

/**
 * v12 — fill for CATEGORY ACCENT BUTTONS (deck control pills, CTAs, the
 * reveal CTA): every style wears the TRUE category accent (pastel-aware), so
 * Material buttons carry the same rich category identity as the cards
 * instead of a muddy device+accent blend. AMOLED plates override to black
 * via [com.curio.app.ui.components.curioButtonColors] where used.
 */
@Composable
fun CurioCategory.themedButtonFill(): Color {
    // v81 — dark mode: [themedAccent] already resolves the dark same-hue
    // shade, so buttons wear a dark fill; [themedButtonInk] flips to the
    // light twin — the exact light-mode reversal the user asked for.
    return themedAccent()
}

/** Content ink for [themedButtonFill] — pastel-aware like the card fills. */
@Composable
fun CurioCategory.themedButtonInk(): Color = onAccent()

/**
 * Ink for content sitting on a category card's fill in the active theme
 * style. Material (v12) wears the same rich category fills as the rest of
 * the app, so its content ink is the same pastel-aware ink as the Curio
 * cards (white on deep, deep on airy pastel); AMOLED cards sit on black
 * glass so their content pairs with the theme onSurface. Curio default
 * resolves [onAccent].
 */
@Composable
fun CurioCategory.cardContentInk(): Color = onAccent()

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
    // v81 — dark mode: the muted dark fills get a light tint (85% toward
    // white keeps the hue whisper while staying crisp light-on-dark).
    isCurioDarkTheme() -> lerp(fill, Color.White, 0.85f)
    else -> {
        // v27p — deepen the light pastel-fill ink (0.30 -> 0.24 lightness)
        // so even green/yellow pastels (Chemistry, Biology, the mixed-deck
        // blends) keep their dark ink above 4.5:1.
        val a = toHsl(fill)
        fromHsl(a.h, a.s.coerceIn(0.15f, 0.60f), 0.24f)
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
    // v27p — same light-mode rule as [categoryInk]: deepen mid-lightness
    // accents so watermark glyphs track the text ink exactly.
    else -> if (accent.needsLightDeepInk()) readableLightInk(accent) else accent
}

/**
 * Non-composable twin of [CurioCategory.themedAccent] — same resolution,
 * parameterized by pastel mode + dark theme (see [categoryInkFor]).
 */
internal fun CurioCategory.themedAccentFor(pastel: Boolean, dark: Boolean): Color =
    if (!pastel) (if (dark) darkAccent(accent) else accent) else pastelAccent(accent, dark)

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
 * v27p — LIGHT-mode readable ink of the SAME hue as an accent. Mid-lightness
 * accents (green, lime, sky, amber, emerald, teal, red, fuchsia, blue — and
 * the pale wildcard coral) read far lighter than their hue suggests once
 * gamma is applied, so the old L=0.30 deep twin still left e.g. green text
 * at ~4:1 on the washed page. Pinning lightness to 0.24 gets every
 * category's accent text over 4.5:1 on the light page, card and pastel
 * fills (checked against the whole palette, old + new lanes).
 */
internal fun readableLightInk(color: Color): Color {
    val a = toHsl(color)
    return fromHsl(a.h, a.s.coerceIn(0.20f, 0.55f), 0.24f)
}

/**
 * v27p — whether an accent needs its deep twin as LIGHT-mode ink: accents
 * whose gamma-corrected luminance passes ~0.105 read below ~4.5:1 on the
 * light surfaces (green/lime/sky/amber/emerald/teal/red/fuchsia/blue and
 * the pale wildcard coral). Deep accents (brown, navy, indigo, plum, tech
 * slate, forest…) stay under the bar and keep themselves as ink.
 */
private fun Color.needsLightDeepInk(): Boolean = luminance() > 0.105f

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
    // the plain theme background (cream) exactly as they did before the wash
    // rollout.
    if (!AppPreferences.tintWashEffective()) return background
    // v81 — dark mode: NO background tint — the page is pitch black (the
    // user's spec), so the wash collapses to the pure black background and
    // the watermark carries the category identity instead.
    if (isCurioDarkTheme()) return background
    // Light: a whisper pastel page (lighter + less saturated than the
    // standard wash in pastel mode) so the airy pastel fills pop instead of
    // melting into the background.
    return if (AppPreferences.pastelColorsState) lightAccentTint(accent, saturation = 0.20f, lightness = 0.90f)
    else lightAccentTint(accent)
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
    // v81 — dark mode: a near-black card tinted with the category's hue
    // (elevation reads as lighter surfaces on the black page — the dark
    // best practice), so cards carry the lane color on pitch black.
    if (isCurioDarkTheme()) return darkSurfaceTint(accent)
    // Light: the wash's stronger sibling, so tiles and chips read as a
    // tinted elevated surface on the tinted page.
    return lightSurfaceTint(accent)
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
    // v81 — dark mode: the mood board keeps its category tint even on the
    // pitch-black page via the dark surface shade.
    if (isCurioDarkTheme()) return darkSurfaceTint(accent)
    // Light: the wash's stronger sibling.
    return lightSurfaceTint(accent)
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
 * less saturated, more contrast. Honors the Settings tint toggle —
 * when it's off, [base] is returned unchanged so chips go back to the plain
 * theme surface.
 */
@Composable
fun CurioCategory.categoryChipSurface(base: Color = MaterialTheme.colorScheme.surfaceContainerLow): Color {
    if (!AppPreferences.tintWashEffective()) return base
    // v81 — dark mode: a touch lighter + more desaturated than cards so the
    // chip lifts off the near-black cards (deep accents otherwise read
    // muddy over black).
    if (isCurioDarkTheme()) return darkChipTint(accent)
    // Light: the wash's stronger sibling, so chips read as tappable pills.
    return lightSurfaceTint(accent)
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

// ── v81 dark-mode shade helpers ────────────────────────────────────────
/**
 * The dark-mode ACCENT shade — the SAME hue at a readable dark lightness
 * (~0.44) with ~20% less saturation (saturated colors vibrate on black —
 * the dark-mode color research). Fills, gradients and button fills wear
 * this on the pitch-black page; content on top flips to the light twin
 * ([categoryInk]/[onAccent]).
 */
internal fun darkAccent(color: Color): Color {
    val a = toHsl(color)
    return fromHsl(a.h, (a.s * 0.80f).coerceAtMost(0.52f), 0.44f)
}

/** v81 — dark-mode CARD surface: the accent's hue at near-black lightness
 *  (elevation reads as lighter surfaces on the black page). */
private fun darkSurfaceTint(accent: Color): Color {
    val a = toHsl(accent)
    return fromHsl(a.h, (a.s * 0.55f).coerceAtMost(0.40f), 0.22f)
}

/** v81 — dark-mode CHIP surface: a touch lighter and more desaturated than
 *  cards so chips read as tappable pills on the near-black cards. */
private fun darkChipTint(accent: Color): Color {
    val a = toHsl(accent)
    return fromHsl(a.h, (a.s * 0.45f).coerceAtMost(0.32f), 0.28f)
}

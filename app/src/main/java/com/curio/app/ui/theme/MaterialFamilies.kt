package com.curio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategory

/**
 * v185 — the Material theme's category-color treatment, per the M3
 * multi-color guideline (m3.material.io/styles/color/system/overview):
 *
 * > Material's color system is built on one primary key color with neutral
 * > surfaces; supporting colors (secondary/tertiary) provide restrained
 * > accents. Giving every section its own vivid hue is NOT the system —
 * > the guideline for multi-color products is restraint.
 *
 * The app's 36 lane accents (Tailwind-700 vivid hues) therefore collapse
 * into **6 muted hue families** — each family keeps ONE recognizable hue
 * at M3-style low chroma, so lanes are no longer identical (the user's
 * "one color per family, muted") but nothing shouts a saturated rainbow
 * over the neutral M3 scheme. Family colors are tonal-palette tones of
 * the family hue (fills T40 light / T80 dark, ink near-white light /
 * deep dark), so they read as members of the M3 scheme, not foreign
 * brand colors.
 */

/** The six M3-aligned hue families the 36 lanes collapse into. */
enum class MaterialFamily(val hue: Float, val saturation: Float) {
    ROSE(347f, 0.85f),    // movies, series, crimson, food red…
    AMBER(34f, 0.80f),    // books, history, mythology, animals brown…
    GREEN(150f, 0.75f),   // biology, chemistry lime, plants, sports…
    BLUE(215f, 0.80f),    // science, astronomy, oceans, tech slate…
    PURPLE(270f, 0.80f),  // anime, games, psychology, fuchsia…
    NEUTRAL(30f, 0.05f)   // geology stone, engineering zinc — near-grey
}

/** Map a lane's researched accent to its muted M3 family by hue. */
internal fun materialFamilyFor(accent: Color): MaterialFamily {
    val h = toHsl(accent).h
    val s = toHsl(accent).s
    // Near-achromatic accents (stone, zinc, slate) are the NEUTRAL family —
    // they stay grey, exactly what M3's neutral roles want.
    if (s < 0.28f) return MaterialFamily.NEUTRAL
    return when {
        h < 18f || h >= 320f -> MaterialFamily.ROSE       // reds, magentas, warm pinks
        h < 60f -> MaterialFamily.AMBER                  // oranges, ambers, golds, browns
        h < 100f -> MaterialFamily.GREEN                 // limes (yellow-green)
        h < 190f -> MaterialFamily.GREEN                 // greens, teals, emeralds
        h < 270f -> MaterialFamily.BLUE                  // cyans, blues, indigos
        else -> MaterialFamily.PURPLE                    // violets, purples, fuchsias
    }
}

/** The family's muted FILL tone (what category cards/chips/fills wear).
 *  Pure color math — non-composable so watermark/remember paths can use it. */
internal fun MaterialFamily.fill(dark: Boolean): Color =
    materialTone(hue, saturation, if (dark) 80 else 40)

/** Ink that reads ON the family fill (near-white light, deep dark). */
internal fun MaterialFamily.onFill(dark: Boolean): Color =
    materialTone(hue, saturation, if (dark) 20 else 100)

/** Muted accent INK for text/icons on plain neutral surfaces. */
internal fun MaterialFamily.ink(dark: Boolean): Color =
    materialTone(hue, saturation, if (dark) 80 else 45)

/** The torn-hero banner fill — the family tone deepened slightly. */
internal fun MaterialFamily.headerFill(dark: Boolean): Color {
    val base = fill(dark)
    if (dark) return base
    val a = toHsl(base)
    return fromHsl(a.h, a.s, (a.l * 0.88f).coerceAtLeast(0.30f))
}

/**
 * Non-composable twins for the remember-block paths (watermark backdrops,
 * mixed-deck blends): the family tone directly — same resolution as the
 * composable accessors since v198 (all six families use their own tonal
 * tone; no scheme-role branches).
 */
internal fun CurioCategory.materialAccentFor(dark: Boolean): Color =
    // v198 — the neutral-family branch used different tones (50/70 vs
    // fill's 40/80); align it to the family fill so watermarks/blends
    // match the chips and buttons exactly.
    materialFamilyFor(accent).fill(dark)

internal fun CurioCategory.materialOnAccentFor(dark: Boolean): Color =
    materialFamilyFor(accent).onFill(dark)

internal fun CurioCategory.materialInkFor(dark: Boolean): Color =
    materialFamilyFor(accent).ink(dark)

/**
 * The family a lane belongs to, resolved through the ACTIVE M3 scheme
 * where the scheme itself carries the color (secondary/tertiary are the
 * scheme's own seeded accents; the extra families stay tonal tones of
 * their own hue so all six stay distinguishable). Only meaningful when
 * the Material theme toggle is on — callers gate on it first.
 */
@Composable
internal fun CurioCategory.materialFamily(): MaterialFamily =
    materialFamilyFor(accent)

/**
 * The single muted color a lane wears under the Material theme — the
 * family's own tonal tone (T40 light / T80 dark), the SAME fill the
 * cards and gradients resolve ([CurioGradients.cardGradient] uses the
 * family tone directly).
 *
 * v198 — the scheme-role branches are GONE: rose lanes wore the scheme
 * SECONDARY (an amber companion in the baseline seed) and green lanes
 * the scheme TERTIARY (mint), so every button, chip and filter under
 * Material painted a DIFFERENT hue than the lane's family-toned cards
 * (a Movies deck: rose card, amber button), and neutral lanes rendered
 * as translucent onSurfaceVariant. The family tone is the lane's
 * Material color everywhere (user verdict: the family-toned cards are
 * good — the buttons and filters must match them). Pastel mode softens
 * the fill to its pastel twin like the cards.
 */
@Composable
fun CurioCategory.materialAccent(): Color {
    val dark = isCurioDarkTheme()
    val fill = materialFamily().fill(dark)
    return if (AppPreferences.pastelColorsState) pastelAccent(fill, dark) else fill
}

/**
 * Ink that reads on a lane's Material fill — the family's on-fill tone
 * (near-white on the T40 light fill, deep on the T80 dark fill), the
 * same pairing the cards use. v198 — the scheme-role ink branches are
 * gone (same reason as [materialAccent]); pastel mode flips to the deep
 * same-hue ink on the airy light pastels and the light twin on the
 * muted dark pastels — the exact pastel ink language of
 * [CurioCategory.onAccent].
 */
@Composable
fun CurioCategory.materialOnAccent(): Color {
    val dark = isCurioDarkTheme()
    val fill = materialFamily().fill(dark)
    return when {
        !AppPreferences.pastelColorsState -> materialFamily().onFill(dark)
        dark -> lerp(fill, Color.White, 0.85f)
        else -> readableLightInk(fill)
    }
}

/**
 * Muted accent ink for category text/icons on plain surfaces under the
 * Material theme — the family's own ink tone (T45 light / T80 dark),
 * hue-consistent with the family fills. v198 — the scheme-role branches
 * (rose→secondary, green→tertiary) painted rose-family TEXT in the
 * scheme's amber/mint companions — off-hue next to the family-toned
 * fills; the family ink tone keeps every text/icon on the lane's own
 * muted hue.
 */
@Composable
fun CurioCategory.materialInk(): Color {
    val dark = isCurioDarkTheme()
    return materialFamily().ink(dark)
}

/**
 * Torn-hero banner fill under the Material theme.
 *
 * DARK keeps the scheme containers (deep T30 tones — crisp under the light
 * ink). LIGHT — v190: the pale T90 containers washed out under the old
 * near-white ink (user verdict: "washed out along with the glyphs and the
 * texts"), so the banner now wears the RICH family color — the family
 * fill lifted to a deep pastel (lightness 0.70, saturation held) — paired
 * with the dark [materialHeroInk]. The material hue stays; the hero reads.
 */
@Composable
fun CurioCategory.materialHeaderAccent(): Color {
    if (isCurioDarkTheme()) {
        return when (materialFamily()) {
            MaterialFamily.ROSE -> MaterialTheme.colorScheme.secondaryContainer
            MaterialFamily.GREEN -> MaterialTheme.colorScheme.tertiaryContainer
            MaterialFamily.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> materialFamily().headerFill(true)
        }
    }
    val fill = materialFamily().fill(false)
    val a = toHsl(fill)
    return fromHsl(a.h, a.s.coerceAtMost(0.55f), 0.70f)
}

/**
 * v190 — dark ink that reads on the LIGHT-mode [materialHeaderAccent]
 * banner: a deep same-hue twin of the family color (the app's light-mode
 * deep-ink language, [readableLightInk]). Pairs with the rich family
 * banner — the old near-white container ink vanished on the pale T90
 * containers (user verdict).
 */
@Composable
fun CurioCategory.materialHeroInk(): Color =
    readableLightInk(materialFamily().fill(false))

// Convenience gate — is the proper M3 Material theme toggle on?
internal val materialThemeOn: Boolean
    get() = AppPreferences.materialThemeState

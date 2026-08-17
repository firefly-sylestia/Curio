package com.curio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
 * mixed-deck blends): the family tone WITHOUT the scheme roles — the two
 * scheme-expressed families (rose→secondary, green→tertiary) fall back to
 * their own tonal tone here, a negligible difference on watermarks/blends.
 */
internal fun CurioCategory.materialAccentFor(dark: Boolean): Color {
    val f = materialFamilyFor(accent)
    return when (f) {
        MaterialFamily.NEUTRAL -> materialTone(f.hue, f.saturation, if (dark) 70 else 50)
        else -> f.fill(dark)
    }
}

internal fun CurioCategory.materialOnAccentFor(dark: Boolean): Color =
    MaterialFamily.forAccent(accent).onFill(dark)

internal fun CurioCategory.materialInkFor(dark: Boolean): Color =
    MaterialFamily.forAccent(accent).ink(dark)

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
 * family fill, or the scheme's own secondary/tertiary accents for the
 * two families the seed palette already expresses (rose→secondary,
 * green→tertiary), so those lanes read as native M3 roles.
 */
@Composable
fun CurioCategory.materialAccent(): Color {
    val dark = isCurioDarkTheme()
    return when (materialFamily()) {
        MaterialFamily.ROSE -> MaterialTheme.colorScheme.secondary
        MaterialFamily.GREEN -> MaterialTheme.colorScheme.tertiary
        MaterialFamily.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        else -> materialFamily().fill(dark)
    }
}

/** Ink on a lane's Material fill — pairs with [materialAccent]. */
@Composable
fun CurioCategory.materialOnAccent(): Color {
    val dark = isCurioDarkTheme()
    return when (materialFamily()) {
        MaterialFamily.ROSE -> MaterialTheme.colorScheme.onSecondary
        MaterialFamily.GREEN -> MaterialTheme.colorScheme.onTertiary
        MaterialFamily.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> materialFamily().onFill(dark)
    }
}

/** Muted accent ink for category text/icons under the Material theme. */
@Composable
fun CurioCategory.materialInk(): Color {
    val dark = isCurioDarkTheme()
    return when (materialFamily()) {
        MaterialFamily.ROSE -> MaterialTheme.colorScheme.secondary
        MaterialFamily.GREEN -> MaterialTheme.colorScheme.tertiary
        MaterialFamily.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> materialFamily().ink(dark)
    }
}

/** Torn-hero banner fill under the Material theme. */
@Composable
fun CurioCategory.materialHeaderAccent(): Color =
    when (materialFamily()) {
        MaterialFamily.ROSE -> MaterialTheme.colorScheme.secondaryContainer
        MaterialFamily.GREEN -> MaterialTheme.colorScheme.tertiaryContainer
        MaterialFamily.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> materialFamily().headerFill(isCurioDarkTheme())
    }

// Convenience gate — is the proper M3 Material theme toggle on?
internal val materialThemeOn: Boolean
    get() = AppPreferences.materialThemeState

package com.curio.app.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * v185 — the proper M3 Material color system (opt-in via
 * [com.curio.app.data.AppPreferences.materialThemeState]).
 *
 * M3 color anatomy (per m3.material.io/styles/color/system/overview): a
 * scheme is built from five KEY colors (primary, secondary, tertiary,
 * neutral, error), each backed by a 13-tone tonal palette; components read
 * ~45 roles. The system's core rule for multi-color products: surfaces stay
 * neutral, ONE primary carries the brand, and secondary/tertiary provide
 * restrained accents — NOT a rainbow per section. That is exactly what the
 * app's 36 category lanes become here: they collapse to ~6 muted hue
 * families (see [MaterialFamilies]) resolved through this scheme's tones.
 *
 * When available (Android 12+), the scheme is the DEVICE's dynamic
 * Material You palette (wallpaper-derived) — the M3-recommended
 * personalization. Older devices fall back to a baseline scheme SEEDED
 * from the brand coral, generated with the standard M3 tone→role mapping
 * (primary T40 light / T80 dark, containers T90/T30, neutrals stepped
 * through the surfaceContainer ladder) so the fallback is M3-correct too.
 */

/** M3 tone → perceptual-lightness map (HCT tone ladder, rounded). */
internal fun materialToneLightness(tone: Int): Float = when {
    tone <= 0 -> 0.00f
    tone < 10 -> 0.06f
    tone < 20 -> 0.11f
    tone < 30 -> 0.21f
    tone < 40 -> 0.31f
    tone < 50 -> 0.42f
    tone < 60 -> 0.52f
    tone < 70 -> 0.62f
    tone < 80 -> 0.71f
    tone < 90 -> 0.80f
    tone < 95 -> 0.90f
    tone < 98 -> 0.95f
    tone < 100 -> 0.98f
    else -> 1.00f
}

/**
 * A tone of [hue]'s tonal palette. Saturation tapers toward the extremes
 * (M3 keeps chroma high only around the mid tones), so tone 10 and tone 95
 * of a hue read as near-neutral tints while tone 40/80 carry the hue.
 */
internal fun materialTone(hue: Float, saturation: Float, tone: Int): Color {
    val l = materialToneLightness(tone)
    // M3 chroma envelope: ~60% of the source saturation at mid tones,
    // dropping to near-zero at the light/dark extremes.
    val taper = when {
        tone <= 10 -> 0.15f
        tone <= 20 -> 0.35f
        tone <= 40 -> 0.75f
        tone <= 60 -> 1.00f
        tone <= 80 -> 0.85f
        tone <= 90 -> 0.55f
        else -> 0.25f
    }
    return fromHsl(hue, (saturation * taper).coerceIn(0.0f, 0.75f), l)
}

/** The brand coral's hue (0xFFFF8FA3 ≈ 347°) — the fallback seed. */
private const val BrandSeedHue = 347f
private const val BrandSeedSaturation = 0.90f

/**
 * Baseline LIGHT scheme seeded from the brand coral. Primary carries the
 * brand at T40; secondary is a warm amber-shifted companion (the Curio
 * butter family, M3-style muted); tertiary a soft mint (the Curio mint
 * family); surfaces ride the standard M3 neutral ladder warmed toward
 * cream so the fallback still sits comfortably beside the Curio look.
 */
internal val MaterialBaselineLightScheme: ColorScheme = lightColorScheme(
    primary = materialTone(BrandSeedHue, BrandSeedSaturation, 40),
    onPrimary = materialTone(BrandSeedHue, BrandSeedSaturation, 100),
    primaryContainer = materialTone(BrandSeedHue, BrandSeedSaturation, 90),
    onPrimaryContainer = materialTone(BrandSeedHue, BrandSeedSaturation, 10),

    secondary = materialTone(38f, 0.75f, 40),
    onSecondary = materialTone(38f, 0.75f, 100),
    secondaryContainer = materialTone(38f, 0.75f, 90),
    onSecondaryContainer = materialTone(38f, 0.75f, 10),

    tertiary = materialTone(168f, 0.70f, 40),
    onTertiary = materialTone(168f, 0.70f, 100),
    tertiaryContainer = materialTone(168f, 0.70f, 90),
    onTertiaryContainer = materialTone(168f, 0.70f, 10),

    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF201A1C),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF201A1C),
    surfaceVariant = Color(0xFFF2DEE1),
    onSurfaceVariant = Color(0xFF524346),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFEF3EF),
    surfaceContainer = Color(0xFFF9EDE9),
    surfaceContainerHigh = Color(0xFFF3E7E3),
    surfaceContainerHighest = Color(0xFFEDE2DE),

    error = materialTone(5f, 0.90f, 40),
    onError = materialTone(5f, 0.90f, 100),
    outline = Color(0xFF857377),
    outlineVariant = Color(0xFFD6C2C6)
)

/**
 * Baseline DARK scheme from the same coral seed. M3 dark flips the tones:
 * primary T80 (bright enough on black), containers T30, surfaces stepped
 * up through near-black so elevation reads via lightness.
 */
internal val MaterialBaselineDarkScheme: ColorScheme = darkColorScheme(
    primary = materialTone(BrandSeedHue, BrandSeedSaturation, 80),
    onPrimary = materialTone(BrandSeedHue, BrandSeedSaturation, 20),
    primaryContainer = materialTone(BrandSeedHue, BrandSeedSaturation, 30),
    onPrimaryContainer = materialTone(BrandSeedHue, BrandSeedSaturation, 90),

    secondary = materialTone(38f, 0.75f, 80),
    onSecondary = materialTone(38f, 0.75f, 20),
    secondaryContainer = materialTone(38f, 0.75f, 30),
    onSecondaryContainer = materialTone(38f, 0.75f, 90),

    tertiary = materialTone(168f, 0.70f, 80),
    onTertiary = materialTone(168f, 0.70f, 20),
    tertiaryContainer = materialTone(168f, 0.70f, 30),
    onTertiaryContainer = materialTone(168f, 0.70f, 90),

    background = Color(0xFF141213),
    onBackground = Color(0xFFEBDFE0),
    surface = Color(0xFF141213),
    onSurface = Color(0xFFEBDFE0),
    surfaceVariant = Color(0xFF524346),
    onSurfaceVariant = Color(0xFFD6C2C6),
    surfaceContainerLowest = Color(0xFF0F0D0E),
    surfaceContainerLow = Color(0xFF1C1A1B),
    surfaceContainer = Color(0xFF211F20),
    surfaceContainerHigh = Color(0xFF2B292A),
    surfaceContainerHighest = Color(0xFF363334),

    error = materialTone(5f, 0.90f, 80),
    onError = materialTone(5f, 0.90f, 20),
    outline = Color(0xFFA08C90),
    outlineVariant = Color(0xFF524346)
)

/**
 * The [ColorScheme] the app wears when the Material theme toggle is on —
 * dynamic (Material You, Android 12+) with the seeded baseline fallback.
 * Mirrors [curioColorScheme]'s dark/light resolution via
 * [isCurioDarkTheme] (theme MODE is shared with the Curio style).
 */
@Composable
fun materialColorScheme(): ColorScheme {
    val context = LocalContext.current
    val dark = isCurioDarkTheme()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) MaterialBaselineDarkScheme else MaterialBaselineLightScheme
    }
}

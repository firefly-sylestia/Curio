package com.curio.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.curio.app.data.AppPreferences
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Curio's M3 theme wrapper.
 *
 * Pastel-warm palette: soft pink primary, warm butter secondary, mint tertiary.
 * No blue tones — dark mode uses deep maroon family, light mode uses warm
 * cream/sand surfaces.
 */

private val CurioLightColorScheme = lightColorScheme(
    primary           = CurioColors.CoralBlush,
    onPrimary         = CurioColors.CreamWhite,
    primaryContainer  = CurioColors.CoralBlush.copy(alpha = 0.18f),
    onPrimaryContainer = CurioColors.DeepPlum,

    secondary           = CurioColors.ButterYellow,
    onSecondary         = CurioColors.DeepPlum,
    secondaryContainer  = CurioColors.ButterYellow.copy(alpha = 0.30f),
    onSecondaryContainer = CurioColors.DeepPlum,

    tertiary           = CurioColors.SkyMint,
    onTertiary         = CurioColors.DeepPlum,
    tertiaryContainer  = CurioColors.SkyMint.copy(alpha = 0.30f),
    onTertiaryContainer = CurioColors.DeepPlum,

    background = CurioColors.SoftCream,
    onBackground = CurioColors.DeepPlum,

    surface                  = CurioColors.SoftCream,
    onSurface                = CurioColors.DeepPlum,
    surfaceVariant           = Color(0xFFECE2CE),
    onSurfaceVariant         = CurioColors.DeepPlum.copy(alpha = 0.75f),
    surfaceContainerLowest   = CurioColors.SoftCream,
    surfaceContainerLow      = Color(0xFFF0E8D6),
    surfaceContainer         = Color(0xFFECE2CE),
    surfaceContainerHigh     = Color(0xFFE4D7BF),
    surfaceContainerHighest  = Color(0xFFDCCDB2),

    error             = CurioColors.WarmCoralRed,
    onError           = CurioColors.CreamWhite,

    outline           = CurioColors.DeepPlum.copy(alpha = 0.15f),
    outlineVariant    = CurioColors.DeepPlum.copy(alpha = 0.08f)
)

private val CurioDarkColorScheme = darkColorScheme(
    primary           = CurioColors.CoralBlush,
    onPrimary         = CurioColors.DeepPlum,
    primaryContainer  = CurioColors.CoralBlush.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,

    secondary           = CurioColors.ButterYellow,
    onSecondary         = CurioColors.DeepPlum,
    secondaryContainer  = CurioColors.ButterYellow.copy(alpha = 0.18f),
    onSecondaryContainer = Color.White,

    tertiary           = CurioColors.SkyMint,
    onTertiary         = CurioColors.DeepPlum,
    tertiaryContainer  = CurioColors.SkyMint.copy(alpha = 0.18f),
    onTertiaryContainer = Color.White,

    // Android 17-style midnight layers: darker, cleaner, and more
    // dimensional while preserving the light palette untouched.
    background = Color(0xFF0B1018),
    onBackground = Color(0xFFF7F2FA),

    surface                  = Color(0xFF111722),
    onSurface                = Color(0xFFF7F2FA),
    surfaceVariant           = Color(0xFF1C2432),
    onSurfaceVariant         = Color(0xFFD4CAD3),
    surfaceContainerLowest   = Color(0xFF070B11),
    surfaceContainerLow      = Color(0xFF0E141E),
    surfaceContainer         = Color(0xFF141B27),
    surfaceContainerHigh     = Color(0xFF1D2634),
    surfaceContainerHighest  = Color(0xFF283244),

    error             = CurioColors.WarmCoralRed,
    onError           = Color.White,

    outline           = Color.White.copy(alpha = 0.15f),
    outlineVariant    = Color.White.copy(alpha = 0.08f)
)

/**
 * AMOLED theme style — true black. Always dark; background and surfaces are
 * pure black so OLED pixels switch fully off, with only the faintest grey
 * steps keeping cards/sheets distinguishable. Category tints are off (plain
 * theme surfaces) but the warm pastel accents stay so cards still pop.
 */
private val CurioAmoledColorScheme = darkColorScheme(
    primary           = CurioColors.CoralBlush,
    onPrimary         = CurioColors.DeepPlum,
    primaryContainer  = CurioColors.CoralBlush.copy(alpha = 0.22f),
    onPrimaryContainer = Color.White,

    secondary           = CurioColors.ButterYellow,
    onSecondary         = CurioColors.DeepPlum,
    secondaryContainer  = CurioColors.ButterYellow.copy(alpha = 0.16f),
    onSecondaryContainer = Color.White,

    tertiary           = CurioColors.SkyMint,
    onTertiary         = CurioColors.DeepPlum,
    tertiaryContainer  = CurioColors.SkyMint.copy(alpha = 0.16f),
    onTertiaryContainer = Color.White,

    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFB4B4B4),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020),

    error = CurioColors.WarmCoralRed,
    onError = Color.White,

    outline = Color.White.copy(alpha = 0.14f),
    outlineVariant = Color.White.copy(alpha = 0.07f)
)

/**
 * App-theme-aware dark check. Reads the current theme mode reactively from
 * [AppPreferences.themeModeState] so that toggling Light/Dark/System in
 * settings takes effect immediately without restarting the app.
 */
@Composable
fun isCurioDarkTheme(): Boolean {
    // AMOLED is always dark by definition (pure-black surfaces).
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) return true
    return when (AppPreferences.themeModeState) {
        "light"  -> false
        "dark"   -> true
        else     -> isSystemInDarkTheme()
    }
}

/**
 * v20 — the brand coral as INK, theme-aware: bright CoralBlush on dark
 * surfaces, a deep readable CoralInk on light cream (CoralBlush is a pale
 * pastel that vanishes on the light background). Use for icons, text and
 * progress accents that sit on cards/cream — the light-mode wash-out fix.
 */
@Composable
fun curioRoseInk(): Color =
    if (isCurioDarkTheme()) CurioColors.CoralBlush else CurioColors.CoralInk

/**
 * v20 — the brand butter as INK, theme-aware: bright ButterYellow on dark
 * surfaces, deep GoldInk on light cream (ButterYellow vanishes on the
 * light background). Gold twin of [curioRoseInk].
 */
@Composable
fun curioGoldInk(): Color =
    if (isCurioDarkTheme()) CurioColors.ButterYellow else CurioColors.GoldInk

/**
 * v20 — the soft sage as INK, theme-aware: soft Sage on dark surfaces, deep
 * SageInk on light cream (Sage vanishes on the light background). For
 * "done"/mastered icons, text and progress accents.
 */
@Composable
fun curioSageInk(): Color =
    if (isCurioDarkTheme()) CurioColors.Sage else CurioColors.SageInk

/**
 * Non-composable dark check for services/workers — mirrors [isCurioDarkTheme]
 * but reads the system night flag from [Context] instead of the @Composable
 * [isSystemInDarkTheme], so plain functions (e.g. notification tinting in
 * [com.curio.app.infrastructure.ExploreSessionService]) can resolve the same
 * dark/light state the UI uses.
 */
fun isCurioDarkThemeForContext(context: Context): Boolean {
    // AMOLED is always dark by definition (pure-black surfaces).
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) return true
    return when (AppPreferences.themeModeState) {
        "light" -> false
        "dark" -> true
        else -> (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

/**
 * The Material style's HUE-LOCKED palette — the device's Material You
 * identity kept as a single HUE (from the wallpaper's dynamic primary), then
 * the whole palette is BUILT from that hue with Curio-tuned saturation and
 * lightness. The raw dynamic scheme's colors are often muddy or washed-out
 * (a brown wallpaper can render the app in dull olive-grey), so this drops
 * them and derives every role from the wallpaper's hue instead — the result
 * is always vivid and coherent no matter what the wallpaper contributes.
 *
 *  - Light mode: near-white surfaces with a whisper of the device hue, an
 *    airy tinted primary-container, and a deep vivid primary + same-hue ink.
 *  - Dark mode: a deep tinted midnight from the same hue, bright readable
 *    primary, and light same-hue container ink.
 *  - Secondary / tertiary are the same hue family offset ±38°, so every
 *    scheme color belongs to one harmonious story.
 */
private fun calmMaterialColorScheme(dynamic: ColorScheme, dark: Boolean): ColorScheme {
    // The wallpaper's identity hue — a near-achromatic wallpaper falls back
    // to hue 0 (warm rose, on-brand).
    val hue = toHsl(dynamic.primary).h
    val secondaryHue = (hue + 38f) % 360f
    val tertiaryHue = (hue + 322f) % 360f

    return if (dark) darkColorScheme(
        primary = fromHsl(hue, 0.58f, 0.72f),
        onPrimary = fromHsl(hue, 0.55f, 0.16f),
        primaryContainer = fromHsl(hue, 0.40f, 0.28f),
        onPrimaryContainer = fromHsl(hue, 0.55f, 0.86f),
        secondary = fromHsl(secondaryHue, 0.46f, 0.70f),
        onSecondary = fromHsl(secondaryHue, 0.50f, 0.18f),
        secondaryContainer = fromHsl(secondaryHue, 0.36f, 0.28f),
        onSecondaryContainer = fromHsl(secondaryHue, 0.48f, 0.84f),
        tertiary = fromHsl(tertiaryHue, 0.40f, 0.72f),
        onTertiary = fromHsl(tertiaryHue, 0.46f, 0.20f),
        tertiaryContainer = fromHsl(tertiaryHue, 0.34f, 0.28f),
        onTertiaryContainer = fromHsl(tertiaryHue, 0.44f, 0.84f),
        background = fromHsl(hue, 0.07f, 0.055f),
        onBackground = fromHsl(hue, 0.08f, 0.93f),
        surface = fromHsl(hue, 0.07f, 0.06f),
        onSurface = fromHsl(hue, 0.08f, 0.93f),
        surfaceVariant = fromHsl(hue, 0.07f, 0.10f),
        onSurfaceVariant = fromHsl(hue, 0.06f, 0.74f),
        surfaceContainerLowest = fromHsl(hue, 0.06f, 0.045f),
        surfaceContainerLow = fromHsl(hue, 0.07f, 0.065f),
        surfaceContainer = fromHsl(hue, 0.07f, 0.08f),
        surfaceContainerHigh = fromHsl(hue, 0.07f, 0.10f),
        surfaceContainerHighest = fromHsl(hue, 0.08f, 0.125f),
        error = CurioColors.WarmCoralRed,
        onError = Color.White,
        outline = fromHsl(hue, 0.08f, 0.93f).copy(alpha = 0.16f),
        outlineVariant = fromHsl(hue, 0.08f, 0.93f).copy(alpha = 0.08f)
    ) else lightColorScheme(
        primary = fromHsl(hue, 0.60f, 0.46f),
        onPrimary = Color.White,
        primaryContainer = fromHsl(hue, 0.44f, 0.90f),
        onPrimaryContainer = fromHsl(hue, 0.58f, 0.26f),
        secondary = fromHsl(secondaryHue, 0.48f, 0.47f),
        onSecondary = Color.White,
        secondaryContainer = fromHsl(secondaryHue, 0.42f, 0.88f),
        onSecondaryContainer = fromHsl(secondaryHue, 0.52f, 0.28f),
        tertiary = fromHsl(tertiaryHue, 0.42f, 0.50f),
        onTertiary = Color.White,
        tertiaryContainer = fromHsl(tertiaryHue, 0.38f, 0.88f),
        onTertiaryContainer = fromHsl(tertiaryHue, 0.48f, 0.30f),
        background = fromHsl(hue, 0.05f, 0.985f),
        onBackground = fromHsl(hue, 0.10f, 0.16f),
        surface = fromHsl(hue, 0.05f, 0.985f),
        onSurface = fromHsl(hue, 0.10f, 0.16f),
        surfaceVariant = fromHsl(hue, 0.05f, 0.94f),
        onSurfaceVariant = fromHsl(hue, 0.06f, 0.42f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = fromHsl(hue, 0.05f, 0.965f),
        surfaceContainer = fromHsl(hue, 0.05f, 0.945f),
        surfaceContainerHigh = fromHsl(hue, 0.05f, 0.91f),
        surfaceContainerHighest = fromHsl(hue, 0.06f, 0.87f),
        error = CurioColors.WarmCoralRed,
        onError = Color.White,
        outline = fromHsl(hue, 0.10f, 0.16f).copy(alpha = 0.16f),
        outlineVariant = fromHsl(hue, 0.10f, 0.16f).copy(alpha = 0.08f)
    )
}

/**
 * The [ColorScheme] the active theme style wears — Curio (warm cream /
 * midnight), AMOLED (pure black), or the device's Material hues calmed
 * into muted pastels (see [calmMaterialColorScheme]). Shared by
 * [CurioTheme] and the floating explore bubble, which renders outside an
 * Activity window and therefore can't use the [CurioTheme] window
 * SideEffect.
 */
@Composable
fun curioColorScheme(): ColorScheme {
    val context = LocalContext.current
    val isDark = isCurioDarkTheme()
    // Theme style decides the color scheme:
    //  - Curio (default): the warm cream/midnight palettes, unchanged.
    //  - AMOLED: the pure-black scheme (always dark).
    //  - Material: the device's Material You hues from the wallpaper,
    //    CALMED into non-vibrant pastels on light airy surfaces (light) or
    //    a soft pastel-tinted dark (dark) — still following the
    //    Light/Dark/System setting.
    return when (AppPreferences.themeStyleState) {
        AppPreferences.THEME_STYLE_AMOLED -> CurioAmoledColorScheme
        AppPreferences.THEME_STYLE_MATERIAL ->
            // Material You's dynamic palette requires API 31 (Android 12);
            // on older devices fall back to the Curio palettes so the
            // style toggle stays harmless everywhere.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val dynamic = if (isDark) dynamicDarkColorScheme(context)
                              else dynamicLightColorScheme(context)
                calmMaterialColorScheme(dynamic, isDark)
            } else {
                if (isDark) CurioDarkColorScheme else CurioLightColorScheme
            }
        else -> if (isDark) CurioDarkColorScheme else CurioLightColorScheme
    }
}

@Composable
fun CurioTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = curioColorScheme()
    // The SideEffect block below is NOT a @Composable context, so resolve
    // the theme-mode dark check here (isCurioDarkTheme is @Composable).
    val isDark = isCurioDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = AndroidColor.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = AndroidColor.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CurioTypography,
        shapes      = CurioShapes,
        content     = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared dialog styling — one container, shape and action ink for every
// AlertDialog in the app, so dialogs match the card language (24dp corners)
// and the pastel-tinted page instead of floating a foreign cream panel.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The card-matching corner radius every AlertDialog wears — the same 24dp
 * medium token as the cards and the Topic Reveal explore dialog.
 */
val CurioDialogShape: RoundedCornerShape = RoundedCornerShape(24.dp)

/**
 * Theme-aware AlertDialog container. Curio LIGHT mode (pastel and plain)
 * blends the surface container toward the soft cream background so the
 * dialog melts into the tinted page instead of floating a deeper yellow-
 * cream panel; dark and Material keep the scheme's own elevated surface.
 */
@Composable
fun curioDialogContainerColor(): Color {
    // v15 — AMOLED dialogs wear the sleek pure-black glass (the scheme's
    // #181818 container reads grey next to the app's true-black cards).
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
        return Color.Black
    }
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL) {
        return MaterialTheme.colorScheme.surfaceContainerHigh
    }
    if (isCurioDarkTheme()) {
        return MaterialTheme.colorScheme.surfaceContainerHigh
    }
    // v11 — light: a soft near-background sheet that matches the page wash
    // family (the cream background) instead of the deeper #E4D7BF container.
    return lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.background,
        0.60f
    )
}

/**
 * Readable dialog ACTION ink. In light mode the scheme primary is the pale
 * coral-pink brand color that washes out on a light dialog, so actions flip
 * to a deep same-hue rose for real contrast; dark and Material keep the
 * scheme primary. v15 — AMOLED uses clean white (the scheme primary is the
 * coral brand color that would read as a red accent on pure black).
 */
@Composable
fun curioDialogActionColor(): Color {
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
        return MaterialTheme.colorScheme.onSurface
    }
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL) {
        return MaterialTheme.colorScheme.primary
    }
    if (isCurioDarkTheme()) {
        return MaterialTheme.colorScheme.primary
    }
    val a = toHsl(MaterialTheme.colorScheme.primary)
    return fromHsl(a.h, a.s.coerceIn(0.35f, 0.60f), 0.36f)
}

/** TextButton colors for dialog actions — dark readable ink in light mode. */
@Composable
fun curioDialogActionButtonColors(): ButtonColors =
    ButtonDefaults.textButtonColors(contentColor = curioDialogActionColor())

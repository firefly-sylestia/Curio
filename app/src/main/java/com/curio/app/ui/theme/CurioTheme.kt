package com.curio.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.curio.app.data.AppPreferences

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

/**
 * v81 — the reimagined dark scheme. The PAGE is PITCH BLACK (OLED);
 * surfaces step up through near-black greys so elevation reads via
 * lightness (the dark-mode best practice — surfaces get lighter the
 * higher they sit). Accent roles stay BRIGHT (the pale coral / butter /
 * mint — Samsung-style bright accents on black) with dark ink on them.
 */
private val CurioDarkColorScheme = darkColorScheme(
    primary           = CurioColors.CoralBlush,
    onPrimary         = CurioColors.DeepPlum,
    primaryContainer  = CurioColors.CoralBlush.copy(alpha = 0.16f),
    onPrimaryContainer = CurioColors.CoralBlush,

    secondary           = CurioColors.ButterYellow,
    onSecondary         = Color(0xFF3A2E0A),
    secondaryContainer  = CurioColors.ButterYellow.copy(alpha = 0.16f),
    onSecondaryContainer = CurioColors.ButterYellow,

    tertiary           = CurioColors.SkyMint,
    onTertiary         = Color(0xFF0C2A2A),
    tertiaryContainer  = CurioColors.SkyMint.copy(alpha = 0.16f),
    onTertiaryContainer = CurioColors.SkyMint,

    background = Color.Black,
    onBackground = Color(0xFFEDE7DC),

    surface                  = Color(0xFF0D0D0D),
    onSurface                = Color(0xFFEDE7DC),
    surfaceVariant           = Color(0xFF1C1C1E),
    onSurfaceVariant         = Color(0xFFB8B2A8),
    surfaceContainerLowest   = Color.Black,
    surfaceContainerLow      = Color(0xFF101010),
    surfaceContainer         = Color(0xFF161616),
    surfaceContainerHigh     = Color(0xFF1D1D1D),
    surfaceContainerHighest  = Color(0xFF252525),

    error             = Color(0xFFE0706A),
    onError           = Color(0xFF2A0A08),

    outline           = Color(0xFFEDE7DC).copy(alpha = 0.18f),
    outlineVariant    = Color(0xFFEDE7DC).copy(alpha = 0.10f)
)

/**
 * App-theme-aware dark check. Reads the theme mode reactively from
 * [AppPreferences.themeModeState] so toggling Light/Dark/System in
 * settings takes effect immediately without restarting the app. v81 —
 * the reimagined dark mode: pitch-black pages, dark same-hue hero
 * shades and the Samsung-style inner glow (no AMOLED/Material styles).
 */
@Composable
fun isCurioDarkTheme(): Boolean = when (AppPreferences.themeModeState) {
    AppPreferences.THEME_DARK -> true
    AppPreferences.THEME_SYSTEM -> isSystemInDarkTheme()
    else -> false
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
 * Non-composable dark check for services/workers — mirrors
 * [isCurioDarkTheme] but reads the system night flag from [Context]
 * instead of the @Composable [isSystemInDarkTheme], so plain functions
 * (e.g. notification tinting in [ExploreSessionService]) resolve the
 * same dark/light state the UI uses.
 */
fun isCurioDarkThemeForContext(context: Context): Boolean = when (AppPreferences.themeModeState) {
    AppPreferences.THEME_DARK -> true
    AppPreferences.THEME_SYSTEM -> (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    else -> false
}

/**
 * The [ColorScheme] the app wears — v81: Curio Light or the reimagined
 * Dark (pitch-black pages, dark surfaces, bright accent roles). Shared
 * by [CurioTheme] and the floating explore bubble, which renders outside
 * an Activity window and therefore can't use the [CurioTheme] window
 * SideEffect.
 */
@Composable
fun curioColorScheme(): ColorScheme =
    if (isCurioDarkTheme()) CurioDarkColorScheme else CurioLightColorScheme

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
 * Theme-aware AlertDialog container. LIGHT (v170): the theme's own
 * elevated container (surfaceContainerHigh — the warm tan the floating
 * pills/chips wear) instead of the old 72% blend toward the cream
 * background, which read as a cream-white panel (the user: dialogs
 * should be theme-aware, not cream white). DARK (v81):
 * toward the pitch-black background, so dialogs read as near-black glass
 * on the black page (with the hero pill edge-glow on top). v116 — the dark
 * fill is now the SETTINGS OPTION CARD construction (the same
 * `lerp(surfaceContainerLow, tintLift, 0.30f)` as [CurioSettingsCard], with
 * the hero-rose whisper as the tint lift) so dialogs match the option-card
 * black glass exactly instead of reading as a separate grey slab.
 */
@Composable
fun curioDialogContainerColor(): Color {
    if (isCurioDarkTheme()) {
        return lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            // The dark tint lift mirrors settingsCardTintLift: a whisper of
            // the brand rose into black (the dialog floats over any page, so
            // it uses the neutral rose rather than a lane accent).
            lerp(Color.Black, curioRoseInk(), 0.20f),
            0.30f
        )
    }
    // v170 — light: the THEME-AWARE elevated container itself (the same
    // surfaceContainerHigh the floating pills and chips wear) — the old
    // 72% blend toward the cream background read as a cream-white panel.
    return MaterialTheme.colorScheme.surfaceContainerHigh
}

/**
 * The hero glass-pill lift — the color the ink-glass hero pills
 * ([SettingsHeroActionPill], [CabinetHeroActionPill], the sort dropdown)
 * are lerped toward for their frosted fill. LIGHT mode lifts toward the
 * page background (the soft cream) so pills read as a small tint of the
 * background shade instead of stark white-cream. v31.
 */
@Composable
fun curioPillLift(): Color =
    if (isCurioDarkTheme()) Color.White else MaterialTheme.colorScheme.background

/**
 * v42 — the COLOR-TINTED glass lift for settings/profile-family controls:
 * the shared buttons/cards lift toward a whisper of the brand rose instead
 * of plain cream, so they stop reading as flat cream blocks. LIGHT: the
 * background rose-tinted ~8%. DARK (v81): a near-white rose-kissed glass
 * (the frosted-glass lift on black), so pills and cards read as bright
 * glass against the pitch-black page.
 */
@Composable
fun curioPillTintLift(): Color =
    if (isCurioDarkTheme()) lerp(Color.White, curioRoseInk(), 0.12f)
    // v92 — light mode wears the SAME bright frosted glass as dark: the
    // hero pills / plates lift toward a rose-kissed WHITE instead of the
    // cream background, so Profile/Settings/hero pills read as the same
    // frosted-glass family in light as they do in dark (the One UI light
    // look — white glass popping off the tinted hero).
    else lerp(Color.White, curioRoseInk(), 0.10f)

/**
 * Readable dialog ACTION ink. LIGHT: the scheme primary is the pale
 * coral-pink brand color that washes out on a light dialog, so actions
 * flip to a deep same-hue rose for real contrast. DARK (v81): the bright
 * pale coral reads crisp on the near-black dialog, so actions use it
 * directly.
 */
@Composable
fun curioDialogActionColor(): Color {
    if (isCurioDarkTheme()) return MaterialTheme.colorScheme.primary
    val a = toHsl(MaterialTheme.colorScheme.primary)
    return fromHsl(a.h, a.s.coerceIn(0.35f, 0.60f), 0.36f)
}

/**
 * TextButton colors for dialog actions — dark readable ink in light mode.
 * v27u — optional [containerColor] turns the transparent TextButton into a
 * visible soft-tinted pill (the explore dialog's two action pills).
 * `Color.Unspecified` (the default) means no container fill.
 */
@Composable
fun curioDialogActionButtonColors(containerColor: Color = Color.Unspecified): ButtonColors =
    ButtonDefaults.textButtonColors(
        contentColor = curioDialogActionColor(),
        containerColor = containerColor
    )

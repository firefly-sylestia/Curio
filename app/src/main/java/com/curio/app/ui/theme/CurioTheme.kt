package com.curio.app.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

/**
 * App-theme-aware dark check. v78 — dark mode is REMOVED (light only):
 * this is the single seam the future dark system will drive. Returns
 * false today; keep every call site reading this function so the new
 * system only flips this one check.
 */
@Composable
fun isCurioDarkTheme(): Boolean = false

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
 * [isCurioDarkTheme]. v78 — light only, the same seam as the UI check
 * (the [context] parameter is kept for API compatibility).
 */
fun isCurioDarkThemeForContext(context: Context): Boolean = false

/**
 * The [ColorScheme] the app wears — v78: Curio LIGHT only (dark mode,
 * AMOLED and Material styles removed). Shared by [CurioTheme] and the
 * floating explore bubble, which renders outside an Activity window and
 * therefore can't use the [CurioTheme] window SideEffect.
 */
@Composable
fun curioColorScheme(): ColorScheme = CurioLightColorScheme

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
 * Theme-aware AlertDialog container — v78: Curio LIGHT mode (pastel and
 * plain) blends the surface container toward the soft cream background so
 * the dialog melts into the tinted page instead of floating a deeper
 * yellow-cream panel.
 */
@Composable
fun curioDialogContainerColor(): Color {
    // v11 — light: a soft near-background sheet that matches the page wash
    // family (the cream background) instead of the deeper #E4D7BF container.
    // v31 — pulled a touch further toward the background (0.60 → 0.72) so
    // the light dialog stops reading as a separate cream panel.
    return lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.background,
        0.72f
    )
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
 * of plain cream, so they stop reading as flat cream blocks. v78 — light
 * only: the background rose-tinted ~8% (the "small tint of the background
 * shade" language from [curioPillLift], now with color).
 */
@Composable
fun curioPillTintLift(): Color = lerp(
    MaterialTheme.colorScheme.background,
    curioRoseInk(),
    0.08f
)

/**
 * Readable dialog ACTION ink — v78: light mode always. The scheme primary
 * is the pale coral-pink brand color that washes out on a light dialog, so
 * actions flip to a deep same-hue rose for real contrast.
 */
@Composable
fun curioDialogActionColor(): Color {
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

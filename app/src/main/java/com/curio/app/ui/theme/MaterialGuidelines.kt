package com.curio.app.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences

/**
 * v185 — the opt-in "Material guidelines" system (default OFF, independent
 * of the Material THEME toggle — it layers full M3 on the CURRENT style,
 * Curio colors included).
 *
 * Per the M3 spec (m3.material.io): a theme is color + typography + shapes
 * + spacing/elevation. The Material theme toggle redoes COLOR; this system
 * redoes the REST:
 *
 *  - **Typography** — the M3 type scale (display/headline/title/body/label
 *    × large/medium/small) at the default M3 sizes/leading (Roboto
 *    57/64 … 11/16). The default [Typography]() IS that scale, so the swap
 *    is one line.
 *  - **Shapes** — the M3 shape scale (extraSmall 4 / small 8 / medium 12 /
 *    large 16 / extraLarge 24), replacing Curio's rounder 8/16/24/32/48.
 *  - **Spacing** — the M3 spacing grid: 4dp base unit, 8dp standard, 16dp
 *    containers, 24dp page margins, 32/48 for large surfaces. Components
 *    that read [CurioSpacing] tokens flip with the toggle; hardcoded brand
 *    paddings stay Curio-branded under "keep chrome".
 *
 * The chrome sub-option ([AppPreferences.materialChromeFullState]) decides
 * how far the swap goes on brand chrome: full = the M3 NavigationBar and
 * the nav labels drop the Changa One display face; keep = the floating
 * pill bar and brand fonts stay.
 */

/** Gate — is the full M3 guidelines toggle on? */
internal val materialGuidelinesOn: Boolean
    get() = AppPreferences.materialGuidelinesState

/** Gate — full M3 chrome (M3 nav bar, no brand display fonts on chrome). */
internal val materialChromeFullOn: Boolean
    get() = materialGuidelinesOn && AppPreferences.materialChromeFullState

/** The M3 type scale — the default [Typography]() IS the M3 spec values. */
val MaterialTypography: Typography = Typography()

/** The M3 shape scale — 4 / 8 / 12 / 16 / 24 (Curio's is 8/16/24/32/48). */
val MaterialShapes: Shapes = Shapes()

/**
 * The M3 spacing grid. 4dp is the base unit; 8dp standard gutters; 16dp
 * container padding; 24dp page margins; 32/48dp for large surfaces.
 * Components read these tokens so a future spacing re-tune (or the
 * guidelines layout pass) changes layout in one place.
 */
object CurioSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

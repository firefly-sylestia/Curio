package com.curio.app.ui.adaptive

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive layout contract (tablet & landscape) — see Curio adaptive layout
 * contract in app/AGENTS.md.
 *
 * Curio uses the Material window-size-class breakpoints to switch between
 * the phone layout (compact) and the wide layout (medium + expanded):
 *
 *  - **Compact** (< 600dp wide): the familiar phone layout — bottom
 *    NavigationBar, full-width content.
 *  - **Medium / Expanded** (>= 600dp): a `NavigationRail` on the left edge
 *    and page content centered in a comfortable column ([CurioContentMaxWidth]).
 *
 * The wide layout is always-on (automatic): it engages on tablets and in
 * landscape without any Settings toggle, and phones are unaffected.
 */

/** The max width of centered page content on wide windows. v27t — raised
 *  from 720dp so tablets/landscape actually use the screen: the old cap left
 *  big dead gutters on 10"+ windows, so content now breathes to 880dp and
 *  the watermark collage fills the remaining edge. */
val CurioContentMaxWidth: Dp = 880.dp

/**
 * The comfortable INNER content column on wide windows (tablet / landscape /
 * desktop) — slightly narrower than the 880dp shell cap so phone-designed
 * rows and cards never stretch into disconnected, gap-filled plates. The
 * extra margin shows the full-bleed watermark collage around the content.
 * v27t — raised from 640dp so shelves, grids and settings rows spread into
 * the tablet's width instead of floating in a narrow centered strip.
 */
val WideContentMaxWidth: Dp = 800.dp

/**
 * Horizontal edge padding that keeps the standard 16dp on phones but centers
 * a [WideContentMaxWidth] column inside the 720dp shell on wide windows.
 * Pass as the start/end of a LazyColumn's contentPadding — the LazyColumn
 * stays full-bleed (so scrollbars/insets behave) while its rows land in the
 * comfortable centered column.
 */
@Composable
fun wideContentEdgePadding(): Dp {
    val wide = windowWidthSizeClass().isWide
    return if (wide) (CurioContentMaxWidth - WideContentMaxWidth) / 2 else 16.dp
}

/**
 * The current window's width size class, recomposed on configuration
 * changes (rotation, resize, multi-window). Uses the canonical
 * `calculateWindowSizeClass(activity)` API of material3-window-size-class.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun windowWidthSizeClass(): WindowWidthSizeClass {
    // LocalActivity is nullable; outside an Activity composition (e.g. a
    // preview) fall back to the compact phone layout.
    val activity = LocalActivity.current ?: return WindowWidthSizeClass.Compact
    return calculateWindowSizeClass(activity).widthSizeClass
}

/**
 * True when the window is medium or expanded — tablets, landscape phones,
 * large split-screen halves. Wide windows engage the rail + centered-column
 * layouts.
 */
val WindowWidthSizeClass.isWide: Boolean
    get() = this != WindowWidthSizeClass.Compact

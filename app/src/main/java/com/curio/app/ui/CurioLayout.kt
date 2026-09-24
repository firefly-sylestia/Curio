package com.curio.app.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import com.curio.app.data.AppPreferences

/**
 * ── v468 — THE ONE PLACE THAT ANSWERS "IS THIS A SMALL WINDOW?" ───────────
 *
 * The member: *\"everything in landscape needs to be more smaller and compact … find
 * proper landscape audit\"*. The audit found **no landscape code anywhere in the
 * app**: zero orientation branches, zero window-size classes, and the only
 * `screenWidthDp` reads in the tree belonged to the settings rail's viewport and
 * the reader's mark locale. What the tree DOES have is a large, consistent
 * `BoxWithConstraints` vocabulary — the reveal dock already tiers itself at
 * 340/440dp — so the compact layouts need a *signal*, not a new layout system.
 * This object is that signal, and it is deliberately the ONLY place the app asks.
 *
 * **TWO QUESTIONS, ONE READER EACH, AND NO SCREEN READS THE CONFIG ITSELF.**
 * A screen that asks `LocalConfiguration.current.orientation` is a screen that
 * cannot also be reached by every other path — a small foldable in portrait, a
 * split-screen window, a free-form desktop window. Both readers are
 * `@ReadOnlyComposable`, so they are free to call from any recomposition and
 * cannot emit anything of their own.
 *
 * **[isCompact] IS ABOUT THE AXIS, NOT THE ORIENTATION.** A phone in landscape is
 * compact because its HEIGHT is the scarce axis and every hero, deck and header in
 * the app is sized against height — so the test is "landscape, OR a window shorter
 * than [COMPACT_HEIGHT_DP] in either orientation". That second half is what makes
 * a split-screen phone and a small foldable compact too, which is the same answer
 * the member asked for (*\"small and compact screen as well\"*).
 *
 * **[floatingPillHeader] IS THE HEADER RULE, AND IT IS WHY THIS FILE EXISTS.** The
 * member wants a small detached pill instead of a header, **always in landscape and
 * available everywhere** behind the Experiments switch — so one reader ORs the two
 * sources and every header asks this instead of the preference. A header that read
 * the preference directly would forget the landscape half; a header that read the
 * orientation directly could never honor the switch.
 *
 * ⚠️ **THE COMPACT PASS IS DELIBERATELY STILL ARRIVING SCREEN BY SCREEN.** The
 * primitive and the headers landed first (the app-wide `CurioGlassToolbar` wears the
 * pill now, and the eleven back buttons wear the chevron); the per-screen compact
 * layouts are the passes recorded in `Prompt.md` §62. A surface that has not been
 * through its pass keeps today's layout in landscape — which is why this file's
 * readers must be *asked*, never assumed.
 */
object CurioLayout {

    /**
     * A window whose height is the scarce axis: take the compact layout.
     *
     * 520dp is a phone in landscape (the shortest common phone viewport is ~360dp
     * tall) with room to spare for a device whose navigation bar eats more, and it
     * is comfortably below every tablet's landscape height, so a tablet in landscape
     * keeps its full layout — it has the room the compact pass exists to save.
     */
    const val COMPACT_HEIGHT_DP = 520

    /** True when the window is short enough that the compact layout is the right one. */
    @Composable
    @ReadOnlyComposable
    fun isCompact(): Boolean {
        val config = LocalConfiguration.current
        return config.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            config.screenHeightDp < COMPACT_HEIGHT_DP
    }

    /**
     * True when a header should be the small floating pill rather than the hero or
     * the content-height glass bar — the member's switch, OR the compact window.
     */
    @Composable
    @ReadOnlyComposable
    fun floatingPillHeader(): Boolean =
        AppPreferences.floatingPillHeadersState || isCompact()
}

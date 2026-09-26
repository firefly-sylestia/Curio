package com.curio.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import kotlin.math.roundToInt

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
 * the app is sized against height — so the test is \"landscape, OR a window shorter
 * than [COMPACT_HEIGHT_DP] in either orientation\". That second half is what makes
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
 *
 * ── v479 — THE RESERVE, AND WHY THE WINDOW IS REMEMBERED HERE ─────────────
 *
 * The member, of the floating pill header: *\"also it leaves a huge blank sace below
 * it fix those bruh\"*. Every screen reserved its **hero's** height as the scroll
 * content's top padding (`SettingsHeroTotalHeight` and friends, ~150–200dp), which
 * is right for the torn banner and the glass bar and absurd for the 48dp pill: the
 * page's first hundred-odd dp sat empty under it. The reserve is computed by PLAIN
 * (non-composable) functions — `settingsHeroTotalHeight()` is called from property
 * getters as well as composables — so the one fact they need, *is this window
 * compact*, cannot be read through a `@Composable` reader.
 *
 * [noteWindow] is the answer: the app's root calls it once per composition (it is
 * an ancestor of every screen and re-reads on every configuration change) and it
 * remembers the two facts the reserves need as **snapshot state**, so a screen that
 * reads them in composition still recomposes when the window changes. The readers
 * below are the non-composable twins of [isCompact]/[floatingPillHeader], and they
 * are the only reason this object holds state at all.
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

    /**
     * v479 — THE FLOATING PILL HEADER'S OWN BAR HEIGHT.
     *
     * The pill (`CurioPillHeader`) is [PILL_HEADER_HEIGHT_DP] tall, and this constant
     * is the one place that number lives: the pill draws itself with it and
     * [PillHeaderReserve] adds the pill's own margins and the status bar it sits
     * under, so the drawing and the reservation can never drift apart.
     */
    const val PILL_HEADER_HEIGHT_DP = 48

    /** The pill's own top/bottom margin inside its host (see `CurioPillHeader`). */
    private const val PILL_HEADER_VERTICAL_DP = 8

    /** The window as [noteWindow] last saw it, for the non-composable readers below. */
    private var compactNow by mutableStateOf(false)
    private var statusTopDp by mutableStateOf(0)

    /**
     * Remember the window for the non-composable reserve readers (see this file's
     * v479 note). Called ONCE by the app's root (`CurioNavHost`), which is an ancestor
     * of every screen and recomposes on every configuration change.
     */
    @Composable
    fun noteWindow() {
        compactNow = isCompact()
        statusTopDp = WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()
            .value
            .roundToInt()
    }

    /**
     * The non-composable twin of [floatingPillHeader]: the same answer, read from the
     * window [noteWindow] remembered instead of from `LocalConfiguration`.
     */
    fun floatingPillHeaderNow(): Boolean =
        AppPreferences.floatingPillHeadersState || compactNow

    /**
     * The floating pill header's whole footprint: its bar, its own margins, and the
     * status bar it sits under, so a screen that reserves this puts its first row
     * exactly under the pill.
     */
    val PillHeaderReserve: Dp
        get() = (PILL_HEADER_HEIGHT_DP + PILL_HEADER_VERTICAL_DP * 2 + statusTopDp).dp

    /**
     * [full], or [PillHeaderReserve] when the floating pill is the header this screen
     * will actually get. This is what a screen reserves instead of guessing — see this
     * file's v479 note.
     */
    fun reserveForPill(full: Dp): Dp =
        if (floatingPillHeaderNow()) PillHeaderReserve else full

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

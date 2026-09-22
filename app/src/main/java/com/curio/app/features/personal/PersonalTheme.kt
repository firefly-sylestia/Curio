package com.curio.app.features.personal

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.features.settings.settingsAccentInk
import com.curio.app.features.settings.settingsReadableInk
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * THE PERSONAL FAMILY'S ACCENT — ONE place, so the journals, the shelf, the
 * book page and the writing canvas can never drift apart.
 *
 * These pages used `MaterialTheme.colorScheme.primary` throughout, which is
 * the app's fixed rose however the member set their theme up: someone on the
 * azure hero — or "the hero follows the Spin lane" — saw a rose that belonged
 * to nobody. They now wear the SAME accent the settings hero wears.
 *
 * AND THEY WEAR IT BY ROLE, which is the second half of the rule:
 *
 *  · [personalAccent] — FILLS: buttons, pills, rails, borders. The hero's own
 *    airy tone, which is what a fill wants.
 *  · [personalAccentInk] — TEXT and GLYPHS: a DEEP shade of the same hue. The
 *    pale fill as ink is the washed-out, cheap-looking accent the member
 *    called out; a heading or a 16dp glyph takes the darker shade.
 *  · low-alpha washes — HIGHLIGHTS and tracks only, never a foreground.
 *
 * [personalOnAccent] stays the ink that reads ON an accent fill (dark on the
 * airy fill, cream on a deep lane accent), and [personalIconTint] is kept as
 * the name every call site already uses for a glyph's tone — it now returns
 * the accent's ink rather than a paler tone.
 */
@Composable
internal fun personalAccent(): Color = settingsRoseAccent()

/** The accent as TEXT/GLYPH ink — the darker shade of the same shade. */
@Composable
internal fun personalAccentInk(): Color = settingsAccentInk()

/** Ink for text sitting ON the accent fill. */
@Composable
internal fun personalOnAccent(): Color = settingsReadableInk(personalAccent())

/**
 * v437 — INK FOR TEXT SITTING ON AN ARBITRARY FILL.
 *
 * The page's own colour is a colour the member picked off a wheel, so the theme's
 * "on accent" answer does not hold for it: a near-white page colour under light
 * ink is a pill nobody can read. Named here beside [personalOnAccent] so a page
 * control that wears the page's own colour has one thing to ask.
 *
 * v443 — AND IT MEASURES THE FILL, which it did not: it forwarded to
 * [settingsReadableInk], which answers from the THEME (a named theme's own
 * `onPrimary` pair, the pastel flag, the light/dark branch) and never looks at
 * `fill` at all. So the one case this function exists for was the one case it got
 * wrong — the member: *"changing color automatically adjusts the text color … so
 * the date pill or anything else doesnt get the weird unredable text"*. The rule
 * is now the one the journal's own knocked-out ink already uses ([journalInkOn]):
 * the fill's OWN luminance decides.
 */
@Composable
internal fun journalOn(fill: Color): Color = journalInkOn(fill)

/**
 * THE MEASUREMENT ITSELF — a plain function, not a theme role.
 *
 * The fill here is a colour SOMEBODY CHOSE, so there is no theme role that can
 * answer for it, and it is asked for from more than a composition (a draw pass
 * and a gesture both want it). Dark ink on a light fill, the journal's own cream
 * on a deep one — the same pairing [journalInk] was written for and the reason a
 * lit tool can wear any accent a member can pick.
 */
internal fun journalInkOn(fill: Color): Color =
    if (fill.luminance() > 0.55f) Color(0xFF1B1613) else Color(0xFFF7F2E8)

/** Glyph tone on an accent wash or a bare accent surface. */
@Composable
internal fun personalIconTint(accent: Color): Color = settingsAccentInk()

// ── v411 — THE JOURNAL'S OWN COLOURS ────────────────────────────────────────

/**
 * THE PAGE'S OWN COLOUR, AS ITS CONTROLS KNOW IT.
 *
 * Provided by the PAGE's host (see `PersonalWritingPage`), because the page's
 * colour is read in several places owned by four different composables and
 * threading it through all of them would have made "the page's colour" a
 * parameter of every journal surface that draws a background.
 *
 * v443 — AND IT NEVER PAINTS THE PAPER. v429 grew a `painted` flag so the member
 * could turn the page itself into their colour, and v440b restored it at their
 * request; their answer now is the other one — *"in journal the paint the page
 * remove that option"*, and, asked what removing it should do, **never paint the
 * page**. So the flag is gone and the paper is the theme's again (see
 * [journalPaper]), while the colour stays where it belongs: on the page's DOORS
 * (Home's chips, the list's spine, the palette door) and as the tint the page's
 * own controls wear ([journalPaperRaised]).
 *
 * [PersonalNoteEntity.pagePainted] is still stored and still written — a member's
 * earlier answer is not erased from their own file, the app simply no longer asks.
 */
internal data class JournalPagePaint(
    val argb: Int = JOURNAL_ACCENT_THEME
) {
    /** The colour to tint the page's own controls with, or null when the theme's
     *  own accent applies. */
    val own: Color? get() = if (argb != JOURNAL_ACCENT_THEME) Color(argb) else null
}

/** The page's own paint, provided by the page's host (see [JournalPagePaint]). */
internal val LocalJournalPagePaint = staticCompositionLocalOf { JournalPagePaint() }

/**
 * THE JOURNAL'S PAPER (v411, and v429 for the page's own colour).
 *
 * A WARM parchment carrying a whisper of the member's own accent (so it follows
 * a lane-following hero or the rose without knowing which), and a warm near-black
 * in dark mode — ink on a cold black page reads as a screen, ink on a warm one
 * reads as a book.
 *
 * v411 — THE MEMBER ASKED FOR THE JOURNAL TO HAVE ITS OWN COLOURS ("elevation
 * depth also for journal introduce its own colors"): it was dressed in the app's
 * generic container steps, the same fill as a settings row, and it has its own
 * paper now.
 *
 * v443 — AND THE PAPER IS THE THEME'S, ALWAYS. v429 let it take the page's own
 * colour at a real tint (0.20 light / 0.28 dark), and the member has since asked
 * for the option to go: *"in journal the paint the page remove that option"* →
 * **never paint the page**. The whisper of the app's accent is what is left, so a
 * journal looks the same whatever colour the member gave its doors, and the
 * member's colour cannot make their own words hard to read.
 */
@Composable
internal fun journalPaper(): Color {
    val warm = if (isCurioDarkTheme()) Color(0xFF17130F) else Color(0xFFFDF9F0)
    val strength = if (isCurioDarkTheme()) 0.10f else 0.05f
    return lerp(warm, personalAccent(), strength)
}

/**
 * v433 — ONE CAPSULE FOR THE PAGE'S OWN CONTROLS.
 *
 * The member: *"make the today and eye pen pill more capsule like and same for
 * the how did the day feel same capsule style as now they are too thin, use one
 * unified capsule style, so they look good"*. The date pill, the eye/pen switch
 * and the mood pill are the three controls that sit ON the page and say what the
 * page IS — the day, the side you are on, and how it felt — and each had grown
 * its own height, radius and padding, so a row of them read as three controls
 * that happened to be near each other. They are ONE capsule now, from this
 * token, so they cannot drift apart again. The chips the mood pill opens wear the
 * same shape, which is what makes the panel under it read as the same thing.
 */
internal object JournalCapsule {
    /** The height every page-level capsule shares — a thumb's own size, so the
     *  three read as controls rather than as labels. */
    val Height = 46.dp

    /** A real capsule: half the height, everywhere (never a rounded box). */
    val Shape = RoundedCornerShape(50)

    /** The room a capsule's content gets at either end. */
    val Pad = 15.dp
}

/**
 * The journal card's fill where a control needs one step of separation.
 *
 * v437 — AND IT IS TINTED TOWARD THE PAGE'S OWN COLOUR, not the theme's ink.
 * The member: *"still the journal page and ts buttons dont get the color by
 * chnaging it"*. This is the fill every page-level capsule wears (the date pill,
 * the eye/pen switch, the dock, the copy box), and it was lerped toward
 * [personalAccentInk] — the THEME's accent — whatever colour the page had been
 * given. So a page coloured indigo still grew rose paper under its own controls,
 * which is why the colour looked like it never arrived.
 */
@Composable
internal fun journalPaperRaised(): Color =
    lerp(
        journalPaper(),
        LocalJournalPagePaint.current.own ?: personalAccentInk(),
        if (isCurioDarkTheme()) 0.10f else 0.04f
    )

/**
 * The ink the journal writes with — the page's own onSurface, named so a journal
 * surface never reaches past this file for it.
 *
 * v443 — AND THAT ANSWER IS NOW THE THEME'S, ALWAYS, because the paper can no
 * longer be the page's own colour (see [journalPaper]): the theme's ink IS
 * measured against the theme's paper, and a page that never paints itself is the
 * one surface where that measurement holds. The members that used to need the
 * v429 branch — a white page on a dark theme, a near-black one on a light theme —
 * cannot happen; what a member's colour DOES carry now is a fill, and a fill asks
 * [journalOn], which measures it.
 */
@Composable
internal fun journalInk(): Color = MaterialTheme.colorScheme.onSurface

/** The notebook's hairline — the rules and dividers INSIDE a journal card.
 *  (Cards themselves draw no border: a soft shadow is the edge now.) */
@Composable
internal fun journalRule(): Color = MaterialTheme.colorScheme.outlineVariant

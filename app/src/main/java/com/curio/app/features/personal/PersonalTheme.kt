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

/** Glyph tone on an accent wash or a bare accent surface. */
@Composable
internal fun personalIconTint(accent: Color): Color = settingsAccentInk()

// ── v411 — THE JOURNAL'S OWN COLOURS ────────────────────────────────────────

/**
 * v429 — WHETHER THIS PAGE PAINTS ITS PAPER WITH ITS OWN COLOUR.
 *
 * Provided by the PAGE's host (see `PersonalWritingPage`), because the paper is
 * drawn in five places owned by four different composables and threading a
 * colour through all of them would have made "the page's colour" a parameter of
 * every journal surface that draws a background.
 *
 * [painted] is only ever true when the page HAS a colour of its own and the
 * member turned the option on ([PersonalNoteEntity.pagePainted], stored with the
 * page): a page following the theme keeps the theme's own whisper, which is what
 * every journal written before this existed already looks like.
 */
internal data class JournalPagePaint(
    val argb: Int = JOURNAL_ACCENT_THEME,
    val painted: Boolean = false
) {
    /** The colour to tint with, or null when the theme's own accent applies. */
    val own: Color? get() = if (painted && argb != JOURNAL_ACCENT_THEME) Color(argb) else null
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
 * v429 — AND WHEN THE PAGE HAS A COLOUR OF ITS OWN AND [JournalPagePaint.painted]
 * IS ON, the paper takes THAT colour at a REAL tint (0.20 light / 0.28 dark)
 * rather than the theme's whisper (the member: *"the journal page color also
 * needs to chnage with the color chnage"*), and that is why [journalInk] has to
 * answer for itself below — a paper this coloured is a paper the theme's ink may
 * no longer read on.
 */
@Composable
internal fun journalPaper(): Color {
    val warm = if (isCurioDarkTheme()) Color(0xFF17130F) else Color(0xFFFDF9F0)
    val paint = LocalJournalPagePaint.current
    val own = paint.own
    val tint = own ?: personalAccent()
    val strength = when {
        own != null -> if (isCurioDarkTheme()) 0.28f else 0.20f
        else -> if (isCurioDarkTheme()) 0.10f else 0.05f
    }
    return lerp(warm, tint, strength)
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

/** The journal card's fill where a control needs one step of separation. */
@Composable
internal fun journalPaperRaised(): Color =
    lerp(journalPaper(), personalAccentInk(), if (isCurioDarkTheme()) 0.10f else 0.04f)

/**
 * The ink the journal writes with — the page's own onSurface, named so a journal
 * surface never reaches past this file for it.
 *
 * v429 — AND IT ANSWERS FOR A PAINTED PAGE. The theme's ink is measured against
 * the theme's surfaces, not against a colour the member picked off a wheel: a
 * WHITE page on a dark theme would have written light ink on light paper, and a
 * near-black page on a light theme the mirror of that. When the page paints its
 * own colour, the theme's ink is kept only while it still contrasts with the
 * paper; otherwise the page's own readable ink (dark on a light page, light on a
 * dark one) stands in, so the one thing a journal must never do — become
 * unreadable because it was made pretty — cannot happen.
 */
@Composable
internal fun journalInk(): Color {
    val paper = journalPaper()
    val themeInk = MaterialTheme.colorScheme.onSurface
    if (LocalJournalPagePaint.current.own == null) return themeInk
    val paperIsLight = paper.luminance() > 0.5f
    val inkIsLight = themeInk.luminance() > 0.5f
    return when {
        paperIsLight && inkIsLight -> Color(0xFF1B1613)
        !paperIsLight && !inkIsLight -> Color(0xFFF7F2E8)
        else -> themeInk
    }
}

/** The notebook's hairline — the rules and dividers INSIDE a journal card.
 *  (Cards themselves draw no border: a soft shadow is the edge now.) */
@Composable
internal fun journalRule(): Color = MaterialTheme.colorScheme.outlineVariant

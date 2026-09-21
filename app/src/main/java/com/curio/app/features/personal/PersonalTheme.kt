package com.curio.app.features.personal

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
 * THE JOURNAL'S PAPER (v411).
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
 * ── v438 — AND THE PAPER IS THE THEME'S AGAIN, ALWAYS ───────────────────
 *
 * v429 let a page PAINT its own paper with the colour the member gave that day
 * ("the journal page color also needs to chnage with the color chnage"), behind
 * a switch in the colour sheet, and v437 pushed that colour into the page's
 * controls and its date pill as well. The member has withdrawn it in one line:
 * *"ykw remove the paint the page so the journal tools etc dont get the color
 * they stay like before only the preview gets the color"*.
 *
 * The colour is back where v428 put it — on the DOORS (Home's journal chips, the
 * journal list's spine, the palette tool in the dock, the swatch the picker
 * shows), which is what "the preview" means: a page's colour is a fact about
 * which page it is, and it belongs on the thing that OPENS the page rather than
 * on the paper. The withdrawn option also had a real fault the member could see:
 * a paper tinted toward an arbitrary pick left the page's own ink and its
 * neighbouring surfaces at contrasts nothing had measured, so words blended into
 * the fill. Nothing in this file asks a page's colour for a TINT again.
 *
 * `PersonalNoteEntity.pagePainted` and its column are LEFT IN PLACE (no schema
 * change, no migration, and the value still round-trips at its default) — the
 * same treatment `profiles.avatar_style` got when the portrait picker went.
 */
@Composable
internal fun journalPaper(): Color {
    val warm = if (isCurioDarkTheme()) Color(0xFF17130F) else Color(0xFFFDF9F0)
    return lerp(warm, personalAccent(), if (isCurioDarkTheme()) 0.10f else 0.05f)
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
    lerp(journalPaper(), personalAccentInk(), if (isCurioDarkTheme()) 0.10f else 0.04f)

/**
 * The ink the journal writes with — the page's own onSurface, named so a journal
 * surface never reaches past this file for it.
 *
 * v438 — AND IT IS JUST THE THEME'S INK AGAIN. It carried a v429 branch that
 * answered for a page painted with a colour off a wheel (dark ink on a light
 * page, light ink on a dark one). The page cannot be painted any more (see
 * [journalPaper]), so the branch has nothing to answer for — and with it gone, the
 * one ink a journal reads with is the theme's own, which is measured against the
 * surfaces it is actually drawn on.
 */
@Composable
internal fun journalInk(): Color = MaterialTheme.colorScheme.onSurface

/** The notebook's hairline — the rules and dividers INSIDE a journal card.
 *  (Cards themselves draw no border: a soft shadow is the edge now.) */
@Composable
internal fun journalRule(): Color = MaterialTheme.colorScheme.outlineVariant

package com.curio.app.features.personal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
 * The member: "elevation depth also for journal introduce its own colors". The
 * journal is where Curio's writing happens, and it was dressed in the app's
 * generic container steps — the same fill as a settings row, on the same flat
 * elevation. It has its own paper now: a WARM parchment that carries a whisper
 * of the member's own accent ([personalAccent], so it follows a lane-following
 * hero or the rose without knowing which), rather than the
 * neutral cream every other card wears.
 *
 * It answers DARK mode with a warm near-black instead of a grey one, because ink
 * on a cold black page reads as a screen and ink on a warm one reads as a book.
 */
@Composable
internal fun journalPaper(): Color {
    val warm = if (isCurioDarkTheme()) Color(0xFF17130F) else Color(0xFFFDF9F0)
    return lerp(warm, personalAccent(), if (isCurioDarkTheme()) 0.10f else 0.05f)
}

/** The journal card's fill where a control needs one step of separation. */
@Composable
internal fun journalPaperRaised(): Color =
    lerp(journalPaper(), personalAccentInk(), if (isCurioDarkTheme()) 0.10f else 0.04f)

/** The ink the journal writes with — the page's own onSurface, named so a
 *  journal surface never reaches past this file for it. */
@Composable
internal fun journalInk(): Color = MaterialTheme.colorScheme.onSurface

/** The notebook's hairline — the rules and dividers INSIDE a journal card.
 *  (Cards themselves draw no border: a soft shadow is the edge now.) */
@Composable
internal fun journalRule(): Color = MaterialTheme.colorScheme.outlineVariant

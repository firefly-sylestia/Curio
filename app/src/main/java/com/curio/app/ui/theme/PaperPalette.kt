package com.curio.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.curio.app.data.NotePaperColor

/**
 * Paper palette for the quotes entry (Marginalia journal + quote cards) and
 * the format text fields that wear rich text. Deliberately theme-agnostic —
 * a "note paper" look instead of the category tint:
 *
 * The paper text boxes are NOT theme-aware: they wear the same warm cream
 * paper (with its dark ink, faint ruled lines, amber highlighter, and soft
 * hairline edge) in BOTH light and dark mode, so a note reads as a physical
 * slip of paper regardless of the app theme.
 *
 * The highlighter marker is a translucent amber that reads on the cream
 * paper.
 */
@Composable
fun paperSurface(): Color = Color(0xFFFBF4E3)

/** Warm dark ink that reads on the cream [paperSurface]. */
@Composable
fun paperInk(): Color = Color(0xFF3B3124)

/** Faint ruled line on the paper — the notebook texture. */
@Composable
fun paperRule(): Color = Color(0xFFE2D6BC)

/** Highlighter marker color — translucent amber on the cream paper. */
@Composable
fun paperHighlight(): Color = Color(0x99FFC933)

/** Warm amber accent for paper-mode controls (the Ruled/Torn style chips,
 *  format tools, cursor) — reads clearly on cream and harmonizes with the
 *  note-paper look in both themes, unlike the theme's tertiary tint. */
@Composable
fun paperAccent(): Color = Color(0xFF9A7B2F)

/**
 * Theme-aware paper control accent — v78: light only (the brighter dark/
 * AMOLED amber is gone with dark mode). The paper slips themselves stay the
 * same warm cream (a note reads as a physical slip); the toolbar + cursor +
 * style chips sit on the cream page and wear the standard amber.
 */
@Composable
fun paperControlAccent(): Color = paperAccent()

/** Hairline edge so paper cards read as distinct notecards — a warm tan
 *  with real contrast against the cream surface (the older near-cream edge
 *  was effectively invisible). */
@Composable
fun paperBorder(): Color = Color(0xFFCBB98F)

// ─────────────────────────────────────────────────────────────────────────────
// Note-paper COLORS — a per-text-box swatch picker next to the Ruled/Torn
// toggle. [notePaperSurface] resolves the chosen [NotePaperColor] to its
// paper sheet; [notePaperInk] keeps a dark ink readable on every pastel
// with a whisper of the sheet's hue; [notePaperHighlight] gives each sheet
// its OWN matching marker tone (amber on cream, rose on pink, mint green on
// mint...) so a colored note's highlight reads as a marker that belongs to
// that page; [notePaperRule] and [notePaperBorder] derive the ruled lines
// and hairline edge from the sheet so each color stays coherent. All are
// theme-agnostic — the paper is the same in both modes, whatever the swatch.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun notePaperSurface(color: NotePaperColor): Color = when (color) {
    NotePaperColor.CREAM -> Color(0xFFFBF4E3)
    NotePaperColor.BUTTER -> Color(0xFFFDF0C8)
    NotePaperColor.PINK -> Color(0xFFFBE5E0)
    NotePaperColor.MINT -> Color(0xFFE4EFDC)
    NotePaperColor.SKY -> Color(0xFFE0EDF5)
    NotePaperColor.LILAC -> Color(0xFFEDE4F4)
}

/** The ink stays warm-dark for readability on every pastel, but carries a
 *  whisper of the sheet's hue (golden on butter, deep rose on pink, deep
 *  green on mint...) so colored notes read as written with an ink that
 *  belongs to their page — not a generic brown. */
@Composable
fun notePaperInk(color: NotePaperColor): Color = when (color) {
    NotePaperColor.CREAM -> Color(0xFF3B3124)
    NotePaperColor.BUTTER -> Color(0xFF3E3521)
    NotePaperColor.PINK -> Color(0xFF432A26)
    NotePaperColor.MINT -> Color(0xFF2A3529)
    NotePaperColor.SKY -> Color(0xFF232F3B)
    NotePaperColor.LILAC -> Color(0xFF322A40)
}

/** Highlighter MARKER color matching the sheet — each note-paper color gets
 *  its own translucent marker tone (amber on cream, warm gold on butter,
 *  rose on pink, mint green on mint, sky blue on sky, lavender on lilac),
 *  so a highlighted phrase on a colored note reads as a marker that belongs
 *  to that page. Translucent like [paperHighlight] so the dark ink stays
 *  readable through it. */
@Composable
fun notePaperHighlight(color: NotePaperColor): Color = when (color) {
    // Each marker is a DEEPER, more opaque tone than its sheet — a pastel
    // page needs a clearly darker stroke to read as a highlighter. The old
    // same-hue pale tones (rose on pink, mint on mint...) vanished against
    // the colored paper.
    NotePaperColor.CREAM -> Color(0x99FFC933)
    NotePaperColor.BUTTER -> Color(0x99EE9E2D)
    NotePaperColor.PINK -> Color(0x99E97E72)
    NotePaperColor.MINT -> Color(0x997FB877)
    NotePaperColor.SKY -> Color(0x996DA4D9)
    NotePaperColor.LILAC -> Color(0x99A585D9)
}

/** Ruled lines derived from the sheet — slightly darker than the paper. */
@Composable
fun notePaperRule(color: NotePaperColor): Color = when (color) {
    NotePaperColor.CREAM -> Color(0xFFE2D6BC)
    NotePaperColor.BUTTER -> Color(0xFFE4D59E)
    NotePaperColor.PINK -> Color(0xFFE5C9C0)
    NotePaperColor.MINT -> Color(0xFFC4D5B8)
    NotePaperColor.SKY -> Color(0xFFBFD3E2)
    NotePaperColor.LILAC -> Color(0xFFD5C6E2)
}

/** Hairline edge derived from the sheet — a touch darker than the rules. */
@Composable
fun notePaperBorder(color: NotePaperColor): Color = when (color) {
    NotePaperColor.CREAM -> Color(0xFFCBB98F)
    NotePaperColor.BUTTER -> Color(0xFFD8C578)
    NotePaperColor.PINK -> Color(0xFFDBB3A8)
    NotePaperColor.MINT -> Color(0xFFAFC39F)
    NotePaperColor.SKY -> Color(0xFFA9C2D6)
    NotePaperColor.LILAC -> Color(0xFFC3B0D4)
}

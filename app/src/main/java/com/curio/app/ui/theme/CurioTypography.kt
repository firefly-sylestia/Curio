package com.curio.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.curio.app.R

/**
 * Curio's typography — see Curio typography contract.
 *
 * Display / headline: Curio's bundled `geom.ttf` variable font. Heavy weight
 * (700+) for headlines and titles.
 *
 * Body / UI text: a clean neutral sans (M3 default) for readability in
 * long essay/journal entries.
 *
 * Rule of thumb: `geom` for anything short and emotional (titles, empty-state
 * copy, button labels). Neutral sans for anything long or functional.
 */

/** Display/headline font family — geom.ttf. Variable font, all weights. */
val GeomFontFamily: FontFamily = FontFamily(
    Font(R.font.geom, FontWeight.Normal),
    Font(R.font.geom, FontWeight.Medium),
    Font(R.font.geom, FontWeight.SemiBold),
    Font(R.font.geom, FontWeight.Bold),
    Font(R.font.geom, FontWeight.ExtraBold)
)

/** Material Symbols glyph font family — for CurioIcon rendering. */
val MaterialSymbolsFontFamily: FontFamily = FontFamily(
    Font(R.font.material_symbols_outlined, FontWeight.Normal),
    Font(R.font.material_symbols_outlined, FontWeight.Bold)
)

/**
 * Patrick Hand — the handwritten note font for the paper text fields
 * (journal, quotes, review, notes, captions, field notes). Google Fonts
 * ships Patrick Hand as a SINGLE regular file (there is no bold or italic
 * TTF), so the family declares ONLY that one face — and Compose's font
 * matcher therefore can never find an exact match for a Bold / Italic
 * request. When a request mismatches the loaded font, the text stack's
 * `fontSynthesis` (set explicitly on styled spans in `buildRichAnnotated`)
 * applies FAKE BOLD (stroke) and OBLIQUE from the single file.
 *
 * This must stay a single entry: declaring Bold / Italic entries that all
 * point at the SAME regular TTF makes every request match an "exact"
 * descriptor whose glyphs are the regular face — the mismatch that triggers
 * synthesis never happens, and bold/italic silently render as regular
 * (the "bold/italic stopped working" regression).
 *
 * The ruled-line cadence stays on `bodyLarge.lineHeight` (24sp), so notes
 * keep their notebook alignment on paper.
 */
val PatrickHandFontFamily: FontFamily = FontFamily(
    Font(R.font.patrick_hand_regular)
)

/**
 * Lora — the editorial serif voice (OFL). A variable font (wght 400–700)
 * bundled at `res/font/lora.ttf` (v35). Used for long-form reading text —
 * the Topic Reveal teaser/quick-fact, action-card instructions, and intro
 * copy — so Curio's UI pairs the geometric geom titles with a warm,
 * curated-magazine body. Same multi-entry pattern as [GeomFontFamily]: the
 * file is variable, so each weight entry resolves via the wght axis.
 */
val LoraFontFamily: FontFamily = FontFamily(
    Font(R.font.lora, FontWeight.Normal),
    Font(R.font.lora, FontWeight.Medium),
    Font(R.font.lora, FontWeight.SemiBold),
    Font(R.font.lora, FontWeight.Bold)
)

/**
 * The editorial body voice — Lora at a relaxed reading size/leading, for
 * quick-facts, instructions and long intro copy (v35). Slightly larger
 * than bodyLarge with zero tracking for the serif's natural rhythm.
 */
val CurioEditorialBody: TextStyle = TextStyle(
    fontFamily = LoraFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 27.sp,
    letterSpacing = 0.sp
)

/** The editorial LEAD — a bolder serif line for the first sentence of a
 *  fact or a highlighted takeaway (v35). */
val CurioEditorialLead: TextStyle = TextStyle(
    fontFamily = LoraFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 29.sp,
    letterSpacing = 0.sp
)

/**
 * Curio typography set — Material 3 defaults overridden with geom where appropriate.
 *
 * Display family uses geom (700+ weight). Body family uses M3 default sans.
 */
val CurioTypography: Typography = Typography(
    // Display — big hero numbers, app name in splash, section openers
    displayLarge = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    // Headline — screen titles, big buttons
    headlineLarge = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    // Title — section headers, card titles, dialog titles
    // v35 — stepped up to Bold for stronger section-title hierarchy.
    titleLarge = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Body — long-form content, form fields, settings copy (M3 default neutral)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        // v35 — tightened 0.5 → 0.3sp for a calmer, more modern read.
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Label — buttons, chips, captions
    labelLarge = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GeomFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
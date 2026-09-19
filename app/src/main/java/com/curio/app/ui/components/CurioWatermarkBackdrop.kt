package com.curio.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.categoryInkFor
import com.curio.app.ui.theme.isCurioDarkThemeForContext
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.themedAccentFor
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * v407 — the default depth of the page glyph backdrop. The alphas in
 * [watermarkAlpha] are the DEEP look (what the collage shipped with); the
 * default multiplies them down to a whisper, because at full strength the
 * scatter read as distracting behind flat content on every screen.
 * Appearance → "Glyph backdrop" → Deep restores 1f exactly.
 */
internal const val GlyphBackdropSubtleScale = 0.32f

/**
 * v407 — the depth multiplier shared by EVERY decorative glyph watermark: the
 * page backdrop ([CurioWatermarkBackdrop]), the mood-board collage
 * ([CurioMoodBoardBackdrop]) and the torn heroes' mirrored symbol pairs
 * (Home, Profile, Cabinet, Settings, Topic History, Onboarding, the saved-entry
 * hero). Subtle ([GlyphBackdropSubtleScale]) unless Appearance → "Glyph
 * backdrop" is Deep. Each surface multiplies its OWN soft alphas by this, so
 * one switch quiets the whole app's glyph decoration together and "Deep"
 * restores every one of them exactly.
 */
@Composable
fun glyphWatermarkDepthScale(): Float =
    if (AppPreferences.glyphBackdropDeepState) 1f else GlyphBackdropSubtleScale

/**
 * Decorative backdrop pinned behind screen content: all eleven category
 * glyphs scattered around the screen edges, each tinted with its own
 * category's accent — the exact color that drives that category's main-card
 * gradient — so the quiet background carries the same palette as the cards
 * without competing with the content on top. The [activeCat]'s glyph gets a
 * stronger whisper so the page subtly echoes the category you're browsing.
 *
 * Shared by the Spin page and Home so the design language carries across
 * screens.
 *
 * Placement uses [Alignment] bias (-1..1 across the Box) so the collage
 * stays inside the screen on any device; the center is left free for the
 * main content.
 *
 * When [topClearance] is set (screens with a hero banner), the layout
 * switches to a LOWER-BAND mode: the glyphs are offset-placed and sized
 * within the area below the clearance so none can cross it or the screen
 * edges, and the active category's glyph is always present.
 *
 * Glyphs mirror `CurioCategories.all` in data/Category.kt (11 categories,
 * verified 1:1 at startup) — if the catalog ever grows, add a tile here.
 *
 * Theme-aware alpha: dark mode keeps the glyphs as soft color ghosts over
 * the midnight surface (raised from the old near-invisible values so the
 * palette still reads); light mode stays a touch stronger over the warm
 * cream surface.
 *
 * v407 — DEPTH. Those alphas are the DEEP look and read as a busy scatter
 * behind flat content, so a page draws them at [GlyphBackdropSubtleScale] of
 * that unless Appearance → "Glyph backdrop" is set to Deep (see
 * `AppPreferences.glyphBackdropDeepState` and [glyphWatermarkDepthScale]).
 * The per-screen [alphaScale] rides on top of the depth either way, so every
 * call site keeps its own tuning without knowing about the toggle.
 */
@Composable
fun CurioWatermarkBackdrop(
    activeCat: CurioCategory,
    modifier: Modifier = Modifier,
    // When set, every glyph stays ENTIRELY below this line (e.g. below a
    // hero banner) — the layout switches to a lower-band slot set tuned so
    // no glyph crosses the clearance or the screen edges on any screen.
    topClearance: Dp = 0.dp,
    // Per-screen readability tuning. Detail pages, Profile, and the Settings
    // family pass a quieter scale so flat content and saved entries remain
    // the visual priority without removing the glyphs entirely.
    alphaScale: Float = 1f
) {
    val context = LocalContext.current
    val isDark = isCurioDarkThemeForContext(context)
    // v407 — the backdrop's DEPTH: the shipped alphas were tuned for the deep
    // look, so a page draws them at a whisper by default and the Appearance
    // toggle restores them. Read from the reactive pref state, so flipping the
    // switch repaints every screen's collage on the spot.
    val glyphScale = alphaScale * glyphWatermarkDepthScale()
    // Every glyph maps to its category accent — the same colors that open
    // the main-card gradients — so the backdrop palette always matches the
    // deck. Wildcard's glyph picks up the brand coral automatically.
    // v7.94 — the map is now cached with remember keyed on the ONLY inputs
    // it depends on (pastel mode + dark theme; the Material style's
    // device-color blend updates when the theme style changes, which
    // re-resolves both). The old build-on-every-recomposition recomputed
    // 11 category colors per FRAME on screens that animate heavily (Spin,
    // Home), adding measurable jank to the very animations this request
    // is about. Behavior is identical — same inputs, same output. (Uses
    // the non-composable categoryInkFor/themedAccentFor twins because a
    // remember calculation lambda is @DisallowComposableCalls.)
    // v7.7 — pastel mode: the airy pastel accents melt into the pale pastel
    // page wash, so the glyphs switch to the category's INK twins (deep
    // accent in light, light twin in dark) to stay visible — and get a
    // modest alpha bump (see [watermarkAlpha]).
    val pastelMode = AppPreferences.pastelColorsState
    val accentByGlyph = remember(pastelMode, isDark) {
        CurioCategories.all.associate {
            it.iconGlyph to if (pastelMode) {
                it.categoryInkFor(pastel = true, dark = isDark)
            } else {
                it.themedAccentFor(pastel = false, dark = isDark)
            }
        }
    }

    if (topClearance > 0.dp) {
        // Lower-band layout (screens with a hero banner): the glyphs are
        // distributed through the area below [topClearance] and sized from
        // the band, so a fixed bias set can't clip at the clearance line or
        // the screen edges, and none can overlap (verified across band
        // sizes). The active category's glyph is always present, boosted.
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val bandHeight = (maxHeight - topClearance).coerceAtLeast(0.dp)
            if (bandHeight <= 0.dp) return@BoxWithConstraints
            val shortSide = minOf(maxWidth, bandHeight)
            val glyphs = listOf(
                activeCat.iconGlyph, "album", "videocam", "movie",
                "edit_note", "brush", "science", "casino"
            ).distinct()
            val slots = listOf(
                LowerBandSlot(-0.93f, -0.86f, 0.20f, -12f),
                LowerBandSlot(0.93f, -0.84f, 0.17f, 10f),
                LowerBandSlot(-0.95f, 0.34f, 0.21f, -8f),
                LowerBandSlot(0.94f, -0.28f, 0.22f, -14f),
                LowerBandSlot(-0.40f, 0.22f, 0.19f, 8f),
                LowerBandSlot(0.92f, 0.45f, 0.16f, -6f),
                LowerBandSlot(-0.85f, 0.78f, 0.18f, 12f),
                LowerBandSlot(0.15f, 0.95f, 0.14f, -10f)
            )
            glyphs.forEachIndexed { i, glyph ->
                val s = slots[i % slots.size]
                LowerBandGlyph(
                    glyph = glyph,
                    biasX = s.biasX,
                    biasY = s.biasY,
                    sizeFactor = s.sizeFactor,
                    rotation = s.rotation,
                    shortSide = shortSide,
                    bandTop = topClearance,
                    activeCat = activeCat,
                    accentByGlyph = accentByGlyph,
                    isDark = isDark,
                    alphaScale = glyphScale
                )
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            WatermarkGlyph("person", BiasAlignment(-0.92f, -0.88f), 92.dp, -12f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("album", BiasAlignment(0.62f, -0.92f), 64.dp, 10f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("videocam", BiasAlignment(0.95f, -0.55f), 108.dp, -8f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("movie", BiasAlignment(0.82f, -0.15f), 56.dp, 16f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("edit_note", BiasAlignment(0.9f, 0.38f), 80.dp, -14f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("menu_book", BiasAlignment(-0.88f, -0.38f), 72.dp, 8f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("brush", BiasAlignment(-0.92f, 0.15f), 96.dp, -6f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("palette", BiasAlignment(-0.78f, 0.68f), 88.dp, 12f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("science", BiasAlignment(0.75f, 0.62f), 104.dp, -12f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("lightbulb", BiasAlignment(0.05f, 0.92f), 76.dp, 6f, activeCat, accentByGlyph, isDark, glyphScale)
            WatermarkGlyph("casino", BiasAlignment(0.3f, -0.2f), 92.dp, -4f, activeCat, accentByGlyph, isDark, glyphScale)
        }
    }
}

/**
 * One scattered glyph — its category's accent at a low alpha, or a stronger
 * whisper of the same color if it's the active category's glyph.
 */
@Composable
private fun BoxScope.WatermarkGlyph(
    glyph: String,
    alignment: Alignment,
    size: Dp,
    rotation: Float,
    activeCat: CurioCategory,
    accentByGlyph: Map<String, Color>,
    isDark: Boolean,
    alphaScale: Float
) {
    val active = glyph == activeCat.iconGlyph
    val accent = accentByGlyph[glyph] ?: CurioColors.WarmWatermarkInk
    // Dark mode: glyphs are muted ghosts but must stay visible (raised from
    // 0.07/0.15 which barely read on the midnight surface). Light mode gets
    // a modest bump too so the palette stays present over cream. Pastel
    // mode raises both a step further — its washes are paler (light) and
    // the ink-twins read lighter over them, so the old values vanished.
    val alpha = watermarkAlpha(active, isDark, AppPreferences.pastelColorsState)
    CurioIcon(
        name = glyph,
        contentDescription = null,
        tint = accent.copy(alpha = (alpha * alphaScale).coerceIn(0f, 1f)),
        size = size,
        modifier = Modifier
            .align(alignment)
            .graphicsLayer { rotationZ = rotation }
    )
}

/** One lower-band slot: bias -1..1 across the band, size as a fraction of
 *  the band's short side, and a rotation. */
private data class LowerBandSlot(
    val biasX: Float,
    val biasY: Float,
    val sizeFactor: Float,
    val rotation: Float
)

/**
 * One glyph in the lower-band layout — offset math keeps it strictly inside
 * the band below [bandTop] (it can never cross the clearance line or the
 * screen edges), and its size derives from the band's short side so the
 * collage adapts to any screen height without overlapping. Same muted
 * accent-tinted styling as [WatermarkGlyph].
 */
@Composable
private fun BoxWithConstraintsScope.LowerBandGlyph(
    glyph: String,
    biasX: Float,
    biasY: Float,
    sizeFactor: Float,
    rotation: Float,
    shortSide: Dp,
    bandTop: Dp,
    activeCat: CurioCategory,
    accentByGlyph: Map<String, Color>,
    isDark: Boolean,
    alphaScale: Float
) {
    val active = glyph == activeCat.iconGlyph
    val accent = accentByGlyph[glyph] ?: CurioColors.WarmWatermarkInk
    // Same pastel-aware alpha as [WatermarkGlyph].
    val alpha = watermarkAlpha(active, isDark, AppPreferences.pastelColorsState)
    val size = (shortSide * sizeFactor).coerceIn(36.dp, 88.dp)
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val bandTopPx = with(density) { bandTop.toPx() }
    val bandWpx = with(density) { maxWidth.toPx() }
    val bandHpx = with(density) { (maxHeight - bandTop).toPx() }
    // Offset shifts the icon's top-left; (band - size) * (1 + bias) / 2
    // places it inside the band biased -1..1 — the extremes sit flush at
    // the band's edges, never past them.
    val x = ((bandWpx - sizePx) * (1f + biasX) / 2f).roundToInt()
    val y = (bandTopPx + (bandHpx - sizePx) * (1f + biasY) / 2f).roundToInt()
    CurioIcon(
        name = glyph,
        contentDescription = null,
        tint = accent.copy(alpha = (alpha * alphaScale).coerceIn(0f, 1f)),
        size = size,
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .graphicsLayer { rotationZ = rotation }
    )
}

/**
 * Theme + pastel-aware alpha for watermark glyphs. Pastel mode's page
 * washes are paler (light) and its glyph tints switch to the category ink
 * twins, so both the active boost and the inactive ghosts get a modest
 * raise — the old values rendered pastel-on-pastel and nearly invisible.
 */
private fun watermarkAlpha(active: Boolean, isDark: Boolean, pastel: Boolean): Float = when {
    active -> if (isDark) (if (pastel) 0.28f else 0.22f) else (if (pastel) 0.38f else 0.30f)
    else -> if (isDark) (if (pastel) 0.15f else 0.11f) else (if (pastel) 0.22f else 0.15f)
}

/**
 * Decorative mood-board backdrop: a scatter of category glyphs laid out by
 * a [seed]-driven, DISTANCE-CHECKED pattern — every glyph is placed with a
 * guaranteed minimum gap from every other glyph (in canvas-relative space,
 * scaled by both glyph sizes), so the collage can never overlap no matter
 * the board size or the seed. The old fixed-bias slot ring only LOOKED
 * collision-free: slots crowd at the corners, the ±0.05 jitter let
 * neighbours drift into each other, and the two centre slots sat close
 * enough to collide on smaller canvases.
 *
 * Layout: a quiet INTERIOR RING of small glyphs sits just outside the tiny
 * centre core (only the exact middle stays clear, for the tiles), and the
 * bigger glyphs ring the edges — the collage fills the whole canvas instead
 * of hugging the perimeter (the old far-first sampler filled the edges
 * before ever reaching the interior, leaving the expanded board's middle a
 * huge empty band). Glyph size grows toward the edges and shrinks toward
 * the middle, so it reads as a natural scatter that stays quiet where the
 * content sits. v7.17 — the pattern is built ONCE in a reference 360x460dp
 * space and rendered at any canvas size as a pure zoom (same glyphs, same
 * positions, sizes scaled by the short side), so the inline card and the
 * full-screen expanded board show the IDENTICAL collage.
 *
 * Theme-aware (muted in dark mode, slightly stronger in light mode) and
 * centred on [accent] tones so it stays legible behind tiles. The seed is
 * stable per board — derive it from the entry id when re-rendering a saved
 * board, or a fresh random value when creating one.
 *
 * v7.9 — pastel mode: the dominant glyph tint switches from the airy pastel
 * accent (pastel-on-pastel over the tinted canvas — faint) to the accent's
 * INK — deep hue twin in light mode, light tint in dark — so the collage
 * reads as ink-on-pastel, matching the page backdrops.
 */
@Composable
fun CurioMoodBoardBackdrop(
    seed: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isCurioDarkThemeForContext(context)
    // v407 — the mood board is glyph decoration too, so its base alphas ride
    // the same Depth switch as the page backdrop and the hero collages.
    val boardDepth = glyphWatermarkDepthScale()
    // v7.94 — accent map cached (same treatment as [CurioWatermarkBackdrop]):
    // keyed on the two real inputs (pastel mode + dark theme) so the mood
    // board stops recomputing 11 category colors on every frame while its
    // glyph animations run. Output is identical; uses the non-composable
    // color twins (remember calculation is @DisallowComposableCalls).
    val pastelMode = AppPreferences.pastelColorsState
    val accentByGlyph = remember(pastelMode, isDark) {
        CurioCategories.all.associate {
            it.iconGlyph to if (pastelMode) {
                it.categoryInkFor(pastel = true, dark = isDark)
            } else {
                it.themedAccentFor(pastel = false, dark = isDark)
            }
        }
    }
    // v7.9 — pastel mode: the DOMINANT (80%) glyph tint switches from the
    // airy pastel accent (which melts into the tinted pastel canvas) to the
    // accent's INK — deep hue twin in light, light tint in dark — so the
    // whole collage reads instead of just the 20% category-glyph accents.
    val boardTint = if (pastelMode) pastelFillInk(accent) else accent
    val glyphs = remember {
        CurioCategories.all.map { it.iconGlyph }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val canvasW = maxWidth
        val canvasH = maxHeight
        val density = LocalDensity.current
        // v7.17 — the pattern is built ONCE from the seed in a reference
        // 360x460dp space (NOT keyed on the canvas size) and rendered at any
        // canvas size as a PURE ZOOM: the same glyphs at the same relative
        // positions, sizes scaled by the short side. The old area-scaled
        // count + canvas-keyed sampling made the full-screen expanded board
        // a genuinely DIFFERENT pattern from the inline card.
        val pattern = remember(seed, boardTint, accentByGlyph, glyphs) {
            buildMoodBoardPattern(seed, boardTint, accentByGlyph, glyphs)
        }
        // Glyph sizes are fractions of the reference short side (~360dp);
        // scale them to THIS canvas's short side at render time.
        val shortSide = minOf(canvasW, canvasH)
        pattern.forEach { p ->
            // Theme-aware base alpha × the glyph's seeded boost, computed at
            // draw time so light/dark toggles apply immediately. Dark mode
            // was raised from 0.05 so the collage is actually visible on the
            // midnight surface (bumped again 0.10→0.12 / 0.14→0.16 with the
            // denser collage so the interior ring reads too). v7.7 — pastel
            // mode bumps both bases a step (ink twins over paler washes);
            // v7.9 — bumped again (0.22→0.26 light / 0.15→0.18 dark) so the
            // ink-tinted collage clearly reads on the tinted pastel canvas.
            val baseAlpha = (if (isDark) (if (pastelMode) 0.18f else 0.12f)
                             else (if (pastelMode) 0.26f else 0.16f)) * boardDepth
            val alpha = baseAlpha * p.alphaBoost
            // Anchor the icon on its CENTRE: the pattern's (xFrac, yFrac)
            // is the glyph centre, but [Modifier.offset] shifts the icon's
            // top-left — so back off by half the glyph's pixel size. Without
            // this, every glyph renders down-right of its placement and the
            // edge glyphs clip past the board's right/bottom borders.
            val half = with(density) { ((shortSide * p.sizeFrac).toPx() / 2f).roundToInt() }
            val x = with(density) { (p.xFrac * canvasW.toPx()).roundToInt() } - half
            val y = with(density) { (p.yFrac * canvasH.toPx()).roundToInt() } - half
            CurioIcon(
                name = p.glyph,
                contentDescription = null,
                tint = p.tint.copy(alpha = alpha),
                size = shortSide * p.sizeFrac,
                modifier = Modifier
                    .offset { IntOffset(x, y) }
                    .graphicsLayer { rotationZ = p.rotation }
            )
        }
    }
}

/**
 * Builds the seeded, distance-checked glyph pattern for
 * [CurioMoodBoardBackdrop]. Pure function of the SEED (plus tints) — the
 * layout is computed ONCE in a reference 360x460dp space, so the same
 * board lays out identically at ANY canvas size.
 *
 * Positions are NORMALIZED canvas fractions (0..1 from the top-left corner)
 * and glyph sizes are fractions of the SHORT side, so the render loop can
 * draw the pattern at any canvas size as a pure zoom — the inline card and
 * the full-screen expanded board show the EXACT SAME collage. Placement is
 * a jittered-grid, Poisson-disc-style sampler in TWO phases: the interior
 * band just outside the tiny centre core is seeded FIRST with small glyphs
 * (so the middle of the collage is never bare), then the perimeter fills
 * far-cells-first up to the target count. Every glyph is accepted ONLY if
 * its circle clears the centre core AND stays a radius-sum times a 1.06
 * spacing MARGIN away from every already-placed glyph (checked in reference
 * dp; since positions AND sizes scale together, the guarantee holds at any
 * rendered size).
 *
 * The old fixed-bias slot ring only LOOKED collision-free: slots crowd at
 * the corners, the jitter let neighbours drift into each other, and the two
 * centre slots sat close enough to collide on smaller canvases — which is
 * what the user kept seeing. (v7.16's area-scaled count also made the
 * expanded board a genuinely different pattern — v7.17 keeps the count
 * constant so small and expanded are identical.)
 */
private fun buildMoodBoardPattern(
    seed: Int,
    accent: Color,
    accentByGlyph: Map<String, Color>,
    glyphs: List<String>
): List<WatermarkPlacement> {
    // Reference canvas — the inline card's ~360x460dp. Laid out here once;
    // the render loop scales positions (fractions) and sizes (short-side
    // fractions) to the actual canvas.
    val w = MoodBoardRefShortDp
    val h = 460f
    val short = minOf(w, h)
    // Constant count (9-12) — NOT area-scaled: scaling the count with the
    // canvas area made the expanded board a different pattern. A pure zoom
    // keeps the same glyphs at the same fractional positions.
    val targetCount = 9 + Random(seed).nextInt(4)

    // Geometry as fractions of the SHORT side — kept tight enough that the
    // grid always has room for the full requested count (validated).
    val marginFrac = 0.095f  // glyphs stay fully inside the canvas (>= largest glyph radius)
    val cellFrac = 0.17f     // grid cell size (jitter stays inside the cell)
    val jitter = 0.34f       // of the cell
    val coreFrac = 0.10f     // tiny centre core exclusion — only the exact middle stays clear
    val interiorBandE = 2.2f // eccentricity ceiling of the quiet interior ring
    val spacingMargin = 1.06f // min centre gap = (r1+r2) × this
    val marginX = marginFrac * short / w
    val marginY = marginFrac * short / h
    val cellX = cellFrac * short / w
    val cellY = cellFrac * short / h
    val exclX = coreFrac * short / w
    val exclY = coreFrac * short / h

    val rng = Random(seed * 7919 + 13)
    // Centres + radii in canvas dp for the exact distance checks. The
    // placement (xFrac, yFrac) is the glyph CENTRE — the draw loop must
    // anchor the icon on that point, not its top-left (see caller).
    data class Placed(val x: Float, val y: Float, val rDp: Float, val placement: WatermarkPlacement)
    val placed = mutableListOf<Placed>()

    fun clearsAll(x: Float, y: Float, rDp: Float): Boolean {
        val cx = x * w
        val cy = y * h
        return placed.all {
            val dx = cx - it.x * w
            val dy = cy - it.y * h
            val rr = (rDp + it.rDp) * spacingMargin
            dx * dx + dy * dy >= rr * rr
        }
    }

    /** Sizes, tints and pushes one glyph at (x, y) if it clears [clearsAll]. */
    fun placeGlyph(x: Float, y: Float, e: Float): Boolean {
        // Bigger glyphs toward the edges, smaller near the middle — reads
        // as a natural collage instead of uniform dots. The cap keeps the
        // radius <= the canvas margin, so even the biggest corner glyph
        // stays fully in bounds on any canvas width.
        // v7.17 — sizes are computed in REFERENCE dp and stored as fractions
        // of the reference short side ([MoodBoardRefShortDp]); the render
        // loop scales them to the actual canvas short side, so the pattern
        // is a pure zoom at any board size. Fixed 26..54dp sizes made the
        // expanded glyphs read proportionally smaller on the tall
        // full-screen canvas — the old mismatch.
        val t = ((e - 1f) / 5.0f).coerceIn(0f, 1f)
        val sizeDp = (26f + t * 28f)
            .coerceAtMost(2f * marginFrac * short) // 26..54 dp in reference space
        if (!clearsAll(x, y, sizeDp / 2f)) return false
        val glyph = glyphs[rng.nextInt(glyphs.size)]
        placed.add(
            Placed(
                x, y, sizeDp / 2f,
                WatermarkPlacement(
                    glyph = glyph,
                    xFrac = x,
                    yFrac = y,
                    sizeFrac = sizeDp / short,
                    rotation = -14f + rng.nextFloat() * 28f,
                    tint = if (rng.nextFloat() < 0.80f) accent else (accentByGlyph[glyph] ?: accent),
                    alphaBoost = 0.85f + rng.nextFloat() * 0.35f
                )
            )
        )
        return true
    }

    // ── Jittered grid over the whole canvas; e = distance from the centre
    // core in core radii (e < 1 = inside the core).
    val cells = mutableListOf<Triple<Float, Float, Float>>() // x, y, eccentricity
    val nx = (1f / cellX).toInt()
    val ny = (1f / cellY).toInt()
    for (gx in 0 until nx) {
        for (gy in 0 until ny) {
            val x = (gx + 0.5f) * cellX + (rng.nextFloat() * 2f - 1f) * cellX * jitter
            val y = (gy + 0.5f) * cellY + (rng.nextFloat() * 2f - 1f) * cellY * jitter
            if (x !in marginX..(1f - marginX)) continue
            if (y !in marginY..(1f - marginY)) continue
            val e = kotlin.math.hypot((x - 0.5f) / exclX, (y - 0.5f) / exclY)
            cells.add(Triple(x, y, e))
        }
    }
    // ── Two-phase placement ────────────────────────────────────────────
    // Phase A — INTERIOR: the quiet band just outside the tiny core is
    // seeded FIRST with small glyphs, so the middle of the collage is never
    // bare. (A single far-first pass fills the perimeter before ever
    // reaching the interior — that's what left the expanded board's middle
    // a huge empty band.)
    val interiorQuota = maxOf(2, (targetCount * 0.22f).roundToInt())
    cells.sortBy { it.third }
    for ((x, y, e) in cells) {
        if (placed.size >= interiorQuota) break
        if (e < 1f || e > interiorBandE) continue
        placeGlyph(x, y, e)
    }
    // Phase B — PERIMETER: the rest, far cells first, until the target.
    // The interior ring is already in, so the outer ring fills around it
    // easily (the interior glyphs are small and far away).
    cells.sortByDescending { it.third }
    for ((x, y, e) in cells) {
        if (placed.size >= targetCount) break
        if (e < 1f) continue
        placeGlyph(x, y, e)
    }
    return placed.map { it.placement }
}

/** Reference short side (dp) of the inline mood-board card — the space the
 *  watermark pattern is laid out in once, so any canvas (incl. the
 *  full-screen expanded board) renders the same pattern as a pure zoom. */
private const val MoodBoardRefShortDp = 360f

/** One seeded glyph placement for [CurioMoodBoardBackdrop]. [xFrac]/[yFrac]
 *  are canvas fractions of the glyph CENTRE; [sizeFrac] is the glyph size as
 *  a fraction of the canvas SHORT side (so the render loop scales it per
 *  canvas while keeping the pattern identical). */
private data class WatermarkPlacement(
    val glyph: String,
    val xFrac: Float,
    val yFrac: Float,
    val sizeFrac: Float,
    val rotation: Float,
    val tint: Color,
    val alphaBoost: Float
)

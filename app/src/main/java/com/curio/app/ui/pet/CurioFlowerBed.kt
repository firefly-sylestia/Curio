package com.curio.app.ui.pet

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlin.math.sin

/**
 * The pet's flower bed (v8.8 → v8.14) — a cozy pixel diorama: a wooden bed
 * with a headboard, a flower pillow, a coral blanket and a tiny lamp, on a
 * grass base. This is the pet's HOME: when the app opens the pet is asleep
 * in the bed and stays asleep until tapped (spec §10.3). Once awake, the bed
 * sits vacant while the pet floats around the app; [onTap] wakes it (or, in
 * the hero, re-opens the check-in when it's already up).
 *
 * v8.14 — the bed lives in a TIME-OF-DAY diorama: the sky behind it wears
 * the hour's palette (warm morning, bright day, amber evening, moonlit
 * night with stars), a sun or moon floats in the corner, and while the pet
 * sleeps a tiny dream bubble with a symbol pops above the bed.
 *
 * [petInside] draws the pet on the mattress (curled up asleep when
 * [sleeping], sitting up otherwise — the sprite wears the curled pose +
 * nightcap + Z's on its own). [celebrateKey] forwards the one-shot
 * celebration hop to the pet in the bed.
 *
 * v8.10 — no accent param: the pet's scarf wears the fixed Curio brand
 * coral (one theme, one color), so the bed stops tinting with category
 * pastels or dark mode.
 */
@Composable
fun CurioFlowerBed(
    petInside: Boolean,
    sleeping: Boolean,
    modifier: Modifier = Modifier,
    bedSize: Dp = 76.dp,
    onTap: (() -> Unit)? = null,
    celebrateKey: Int = 0,
    contentDescription: String? = null,
    // v9.3 — custom bed rows override the default diorama (home editor).
    customRows: List<String>? = null
) {
    val context = LocalContext.current
    // Reactively read the saved bed design; the param wins for explicit
    // call-site overrides, otherwise fall through to the preference.
    val bedRows: List<String> = customRows?.takeIf {
        it.size == BED_GRID_H && it.all { row -> row.length == BED_GRID_W }
    } ?: AppPreferences.bedDesignRowsState?.takeIf {
        it.size == BED_GRID_H && it.all { row -> row.length == BED_GRID_W }
    } ?: BED_ROWS
    val ink = Color(0xFF4A3426)
    val wood = Color(0xFFB98A5E)
    val woodShade = Color(0xFF8A5A33)
    val mattress = Color(0xFFFFF6E6)
    val mattressShade = Color(0xFFF0E4CE)
    val petal = Color(0xFFF7B8D0)
    val petalShade = Color(0xFFE89AB8)
    val gold = Color(0xFFFFD97D)
    val grass = Color(0xFF9CCB8B)
    val grassDeep = Color(0xFF7FB56F)
    // v8.14 — the cozy additions: a coral blanket and its fold shade.
    val blanket = Color(0xFFF2A6B3)
    val blanketShade = Color(0xFFDC8A99)
    // v8.14 — the diorama sky + celestial bodies per time of day.
    val skyMorning = Color(0xFFFFE7C2)
    val skyAfternoon = Color(0xFFBCDFF5)
    val skyEveningTop = Color(0xFFF3B98A)
    val skyEveningBottom = Color(0xFFC98FB8)
    val skyNight = Color(0xFF232A52)
    val moon = Color(0xFFF6F1D8)
    val star = Color(0xFFFFF3C4)
    val sun = Color(0xFFFFD97D)
    val sunEvening = Color(0xFFFFA05A)

    val mood = if (sleeping) CurioPet.Mood.SLEEPY
    else CurioPet.mood(context, CurioQuests.categoriesState)
    // v8.14 — the diorama reads the device clock.
    val tod = CurioPet.timeOfDay()

    // Dream bubble cycle — the phase is a plain Float in both branches (a
    // stable 0f when not sleeping, since the bubble is never composed then).
    // The spec type is annotated (InfiniteRepeatableSpec<Float>) so T binds
    // — tween/infiniteRepeatable are generic and unannotated vals leave it
    // unbound ("Cannot infer type for type parameter 'T'").
    val dreamSpec: InfiniteRepeatableSpec<Float> = remember {
        infiniteRepeatable(tween(6000, easing = LinearEasing))
    }
    val dreamPhase: Float = if (sleeping) {
        val t = rememberInfiniteTransition(label = "petDream")
        val p by t.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = dreamSpec,
            label = "petDreamPhase"
        )
        p
    } else 0f
    // Star twinkle phase — a real animated value so the night sky sparkles.
    val twinkleSpec: InfiniteRepeatableSpec<Float> = remember {
        infiniteRepeatable(tween(2200, easing = LinearEasing))
    }
    val twinkle by rememberInfiniteTransition(label = "petTwinkle").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = twinkleSpec,
        label = "petTwinklePhase"
    )

    val desc = contentDescription
    Box(
        modifier = modifier
            .size(bedSize)
            .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
            .then(if (desc != null) Modifier.semantics { this.contentDescription = desc } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val px = size.width / BED_GRID_W
            fun drawPx(col: Int, row: Int, color: Color, alpha: Float = 1f) {
                if (col !in 0 until BED_GRID_W || row !in 0 until BED_GRID_H) return
                drawRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(col * px, row * px),
                    size = Size(px + 0.02f, px + 0.02f)
                )
            }

            // ── The diorama sky (v8.14) — fills the scene behind the bed ──
            when (tod) {
                CurioPet.TimeOfDay.MORNING -> drawRoundRect(
                    color = skyMorning,
                    cornerRadius = CornerRadius(px * 2f)
                )
                CurioPet.TimeOfDay.AFTERNOON -> drawRoundRect(
                    color = skyAfternoon,
                    cornerRadius = CornerRadius(px * 2f)
                )
                CurioPet.TimeOfDay.EVENING -> drawRoundRect(
                    brush = Brush.verticalGradient(listOf(skyEveningTop, skyEveningBottom)),
                    cornerRadius = CornerRadius(px * 2f)
                )
                CurioPet.TimeOfDay.NIGHT -> drawRoundRect(
                    color = skyNight,
                    cornerRadius = CornerRadius(px * 2f)
                )
            }

            // Stars at night — a few twinkling dots.
            if (tod == CurioPet.TimeOfDay.NIGHT) {
                val tw = (sin(twinkle * 2f * kotlin.math.PI.toFloat()) * 0.5f + 0.5f)
                drawPx(2, 2, star, 0.4f + 0.6f * tw)
                drawPx(6, 6, star, 0.3f + 0.5f * (1f - tw))
                drawPx(26, 4, star, 0.4f + 0.6f * (1f - tw))
                drawPx(30, 10, star, 0.3f + 0.5f * tw)
            }

            // The celestial body — sun by day, moon by night.
            when (tod) {
                CurioPet.TimeOfDay.MORNING -> drawCircle(sun.copy(alpha = 0.9f), radius = 2f * px, center = Offset(4.8f * px, 4.4f * px))
                CurioPet.TimeOfDay.AFTERNOON -> drawCircle(sun.copy(alpha = 0.95f), radius = 2.2f * px, center = Offset(26.4f * px, 4f * px))
                CurioPet.TimeOfDay.EVENING -> drawCircle(sunEvening.copy(alpha = 0.9f), radius = 2.2f * px, center = Offset(16f * px, 13f * px))
                CurioPet.TimeOfDay.NIGHT -> {
                    drawCircle(moon.copy(alpha = 0.95f), radius = 1.8f * px, center = Offset(5.2f * px, 4.4f * px))
                    // A little crescent carve so it reads as a moon, not a sun.
                    drawCircle(skyNight, radius = 1.4f * px, center = Offset(6.4f * px, 3.6f * px))
                }
            }

            // The bed itself.
            bedRows.forEachIndexed { row, line ->
                line.forEachIndexed { col, ch ->
                    when (ch) {
                        'w' -> drawPx(col, row, wood)
                        'l' -> drawPx(col, row, woodShade)
                        'm' -> drawPx(col, row, mattress)
                        'M' -> drawPx(col, row, mattressShade)
                        'F' -> drawPx(col, row, petal)
                        'f' -> drawPx(col, row, petalShade)
                        'g' -> drawPx(col, row, gold)
                        'k' -> drawPx(col, row, blanket)
                        'K' -> drawPx(col, row, blanketShade)
                        'G' -> drawPx(col, row, grass)
                        'D' -> drawPx(col, row, grassDeep)
                    }
                }
            }

            // v8.36 — at night the bed settles into the dark: a soft navy
            // wash over the bed keeps the lamp glow as the only warm light.
            if (tod == CurioPet.TimeOfDay.NIGHT) {
                drawRoundRect(
                    color = skyNight.copy(alpha = 0.22f),
                    topLeft = Offset(2f * px, 1f * px),
                    size = Size(28f * px, 15f * px),
                    cornerRadius = CornerRadius(px * 2f)
                )
            }

            // A warm glow around the lamp — stronger at night.
            val lampGlow = if (tod == CurioPet.TimeOfDay.NIGHT) 0.4f else 0.16f
            drawCircle(
                color = gold.copy(alpha = lampGlow),
                radius = 2.6f * px,
                center = Offset(27f * px, 4.4f * px)
            )

            // A soft shadow under the bed so it reads as standing on the page.
            drawRoundRect(
                color = ink.copy(alpha = 0.10f),
                topLeft = Offset(6 * px, (BED_GRID_H - 1) * px),
                size = Size(20 * px, px),
                cornerRadius = CornerRadius(px * 0.5f)
            )
        }
        if (petInside) {
            CurioPetSprite(
                stage = CurioPet.currentStage(),
                mood = mood,
                spriteSize = bedSize * 0.52f,
                celebrateKey = celebrateKey,
                contentDescription = if (sleeping) "Curie asleep in its flower bed. Tap to wake" else null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = bedSize * 0.05f, bottom = bedSize * 0.18f)
            )
        }
        // v8.14 — dream bubbles: while asleep, a tiny bubble with a symbol
        // pops above the bed, rises and fades (repeats every ~6s).
        if (sleeping) {
            val dreamT = ((dreamPhase * 3f) % 1f)
            val dreamVisible = dreamPhase in 0.03f..0.30f
            if (dreamVisible) {
                val glyph = listOf("auto_awesome", "music_note", "menu_book", "star")
                    .getOrElse(((dreamPhase * 6f).toInt() % 4).coerceAtLeast(0)) { CurioIcons.Star }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = bedSize * 0.05f)
                        .size(bedSize * 0.17f)
                        .graphicsLayer {
                            alpha = (1f - dreamT).coerceIn(0.35f, 1f)
                            translationY = (-7f * dreamT).dp.toPx()
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = glyph,
                        contentDescription = null,
                        tint = Color(0xFFE88FB0),
                        size = bedSize * 0.10f
                    )
                }
            }
        }
    }
}

// v8.36 — the home grew up: a 32-column detailed diorama (was 16).
internal const val BED_GRID_W = 32
internal const val BED_GRID_H = 18

/**
 * The bed — a cozy v8.14 diorama: headboard (top), a tiny lamp on the
 * headboard's right, a flower pillow, a coral blanket folded over the
 * mattress, and a grass base. Keys: '.' empty, 'w' wood, 'l' wood shade,
 * 'm' mattress, 'M' mattress shade, 'F' petal, 'f' petal shade,
 * 'g' gold center, 'k' blanket, 'K' blanket shade, 'G' grass,
 * 'D' grass deep.
 */
internal val BED_ROWS: List<String> = listOf(
    "............wwwwwwww............",
    "..........wwwwwwwwwwww..........",
    "..........wwwwwlwwlwwlw.........",
    "..........wwwwwlwwlwwlw.........",
    "......wwwwwwwwwwwwwwwwwwwwww....",
    ".......wmmFgFmFgFmFgFmmFgFmmw...",
    ".......wmmFfFmFfFmFfFmmFfFmmw...",
    "......wmmmmmmmmmmmmmmmmmmmmmmw..",
    "......wmmmmmkkkkkkkkkkkkkkkkmw..",
    "......wmmmmmKKKKKKKKKKKKKKKKmw..",
    "......wmmmmmkkkkkkkkkkkkkkkkmw..",
    "......wmmmmmmmmmmmmmmmmmmmmmmw..",
    ".....wwwwwwwwwwwwwwwwwwwwww.....",
    "....wwwwwwwwwwwwwwwwwwwwwwww....",
    "..GGGGGGGGGGGGGGGGGGGGGGGGGGGG..",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    ".D.D..DD..D.D..DD..D.D..DD..D.D.",
    "................................"
)

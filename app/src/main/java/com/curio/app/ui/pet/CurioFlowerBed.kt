package com.curio.app.ui.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests

/**
 * The pet's flower bed (v8.8) — a tiny pixelated wooden bed with a flower
 * pillow and a grass base, drawn entirely in Compose. This is the pet's
 * HOME, replacing the old static pet spots: when the app opens the pet is
 * asleep in the bed and stays asleep until it is tapped (spec §10.3). Once
 * awake, the bed sits vacant while the pet floats around the app; [onTap]
 * wakes it (or, in the hero, re-opens the check-in when it's already up).
 *
 * [petInside] draws the pet on the mattress (asleep when [sleeping], with
 * drifting Z's; sitting up otherwise). [celebrateKey] forwards the one-shot
 * celebration hop to the pet in the bed.
 */
@Composable
fun CurioFlowerBed(
    petInside: Boolean,
    sleeping: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    bedSize: Dp = 76.dp,
    onTap: (() -> Unit)? = null,
    celebrateKey: Int = 0,
    contentDescription: String? = null
) {
    val context = LocalContext.current
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

    val mood = if (sleeping) CurioPet.Mood.SLEEPY
    else CurioPet.mood(context, CurioQuests.categoriesState)

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
            BED_ROWS.forEachIndexed { row, line ->
                line.forEachIndexed { col, ch ->
                    when (ch) {
                        'w' -> drawPx(col, row, wood)
                        'l' -> drawPx(col, row, woodShade)
                        'm' -> drawPx(col, row, mattress)
                        'M' -> drawPx(col, row, mattressShade)
                        'F' -> drawPx(col, row, petal)
                        'f' -> drawPx(col, row, petalShade)
                        'g' -> drawPx(col, row, gold)
                        'G' -> drawPx(col, row, grass)
                        'D' -> drawPx(col, row, grassDeep)
                    }
                }
            }
            // A soft shadow under the bed so it reads as standing on the page.
            drawRoundRect(
                color = ink.copy(alpha = 0.10f),
                topLeft = Offset(3 * px, (BED_GRID_H - 1) * px),
                size = Size(10 * px, px),
                cornerRadius = CornerRadius(px * 0.5f)
            )
        }
        if (petInside) {
            CurioPetSprite(
                stage = CurioPet.currentStage(),
                mood = mood,
                accent = accent,
                spriteSize = bedSize * 0.52f,
                celebrateKey = celebrateKey,
                contentDescription = if (sleeping) "Curio asleep in its flower bed — tap to wake" else null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = bedSize * 0.05f, bottom = bedSize * 0.18f)
            )
        }
    }
}

private const val BED_GRID_W = 16
private const val BED_GRID_H = 13

/**
 * The bed — a wooden frame with a flower pillow (left) and a grass strip at
 * the base. Keys: '.' empty, 'w' wood, 'l' wood shade, 'm' mattress,
 * 'M' mattress shade, 'F' petal, 'f' petal shade, 'g' gold center,
 * 'G' grass, 'D' grass deep.
 */
private val BED_ROWS: List<String> = listOf(
    "......FFFF......",
    ".....FfFfF......",
    "......FgF.......",
    "................",
    "..wwwwwwwwwwww..",
    "..wmmmmmmmmmmw..",
    "..wmmmmmmmmmmw..",
    "..wmmmmmmmmmmw..",
    "..wwwwwwwwwwww..",
    "..GGGGGGGGGGGG..",
    "...ll......ll...",
    "..lll......lll..",
    "................"
)

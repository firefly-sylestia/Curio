package com.curio.app.features.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * The 16 code-drawn social portraits.
 *
 * Everything lives in a disc of one warm ground colour with a soft inner rim,
 * so an avatar reads as a pressed paper button rather than a flat blob; the
 * character on top is drawn with three tones (garment, skin, hair) plus ink
 * for the face, and each of the 16 is a DIFFERENT character — a beanie, a bob,
 * a bun with glasses, headphones, a wizard hat, an explorer's goggles, a space
 * helmet. Nothing is uploaded and nothing is stored: `avatar_style` is a single
 * small integer on the profile row.
 *
 * Drawn rather than shipped as assets on purpose: 16 styles cost no download,
 * scale to any size, and can never carry a photo.
 */
private class AvatarArt(
    val ground: Color,
    val garment: Color,
    val skin: Color,
    val hair: Color,
    /** Which character to draw — see [drawCharacter]. */
    val kind: Int
)

// The palette is the app's own warm paper family with a few lane accents, so
// the discs sit naturally beside the rose settings family and the lane chips.
private val AVATARS: List<AvatarArt> = listOf(
    AvatarArt(Color(0xFFE9A9A2), Color(0xFF6C8FBF), Color(0xFFF3D2B6), Color(0xFF4A3B33), 0),  // beanie
    AvatarArt(Color(0xFF9FB8E8), Color(0xFFE7C6A8), Color(0xFFF6DCC4), Color(0xFF8C4A2F), 1),  // bob
    AvatarArt(Color(0xFFA9C7A0), Color(0xFFEFE3D2), Color(0xFFEFC9A6), Color(0xFF3B2F2A), 2),  // bun + glasses
    AvatarArt(Color(0xFFE8C583), Color(0xFFDA7F63), Color(0xFFE9BE97), Color(0xFF3A2B24), 3),  // curls
    AvatarArt(Color(0xFFB9A4E0), Color(0xFF4E5A78), Color(0xFFF2D3B8), Color(0xFF32261F), 4),  // cap + headphones
    AvatarArt(Color(0xFF9FD0CB), Color(0xFFE4A15C), Color(0xFFF4D7BE), Color(0xFF5B3A24), 5),  // long + earring
    AvatarArt(Color(0xFFD8A98F), Color(0xFF6B5A4E), Color(0xFFE8C09A), Color(0xFF2E2622), 6),  // beard
    AvatarArt(Color(0xFFF0AEC4), Color(0xFF8E7CC3), Color(0xFFF6DCC6), Color(0xFF7A4A2C), 7),  // pigtails
    AvatarArt(Color(0xFF8E93C9), Color(0xFF3F4E86), Color(0xFFF1CFA9), Color(0xFFEDE6D6), 8),  // wizard hat
    AvatarArt(Color(0xFFA8BE8C), Color(0xFF5F7A4A), Color(0xFFEBC49C), Color(0xFF4A6B33), 9),  // leaf crown
    AvatarArt(Color(0xFF9FB0BE), Color(0xFF7A5B45), Color(0xFFF2D2B0), Color(0xFF3A2E27), 10), // goggles
    AvatarArt(Color(0xFFC5989C), Color(0xFF7E4A52), Color(0xFFF5D9C0), Color(0xFF3C2A2A), 11), // beret
    AvatarArt(Color(0xFFEFA785), Color(0xFF4F7F72), Color(0xFFF7DEC6), Color(0xFF6B3F26), 12), // top bun + bow
    AvatarArt(Color(0xFFE3CFA6), Color(0xFF5E7F9C), Color(0xFFF4D8BC), Color(0xFFC98A3E), 13), // short + freckles
    AvatarArt(Color(0xFFAE8FBC), Color(0xFF3E4A5C), Color(0xFFEEF0F4), Color(0xFF2B3038), 14), // hood
    AvatarArt(Color(0xFF6E7699), Color(0xFFD9DEEA), Color(0xFFF2D6BE), Color(0xFF9FB8E8), 15)  // space helmet
)

/**
 * The code-drawn social portrait for [style] (0–15).
 *
 * @param avatarSize the disc's diameter.
 * @param onClick  when set the disc is tappable (used by the picker and by
 *                 every author row that opens a profile).
 * @param ring     the soft inner rim. Keep it on for avatars that sit on a
 *                 card or a page background; turn it off inside a busy
 *                 composed row where the extra stroke reads as noise.
 */
@Composable
internal fun SocialAvatar(
    style: Int,
    avatarSize: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    ring: Boolean = true
) {
    val art = AVATARS[style.coerceIn(0, AVATARS.lastIndex)]
    Box(
        modifier = Modifier
            .size(avatarSize)
            .background(art.ground, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(avatarSize)) {
            // The face is drawn on a 100×100 grid and scaled to the disc, so
            // every style stays proportional at any size (a 24dp chip avatar
            // and a 96dp profile portrait are the same drawing).
            val unit = size.minDimension / 100f
            fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
            fun r(value: Float) = value * unit

            if (ring) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = size.minDimension / 2f - r(1.5f),
                    style = Stroke(width = r(2f))
                )
            }
            drawCharacter(art, ::p, ::r)
        }
    }
}

/**
 * The character itself. [p] maps the 100×100 design grid to the canvas and [r]
 * scales a design radius, so each style reads the same at 24dp and at 120dp.
 *
 * Layout (design units): shoulders from y=76, head circle centred (50, 44)
 * with r=22, so a hat or hair mass sits above y=22 and the garment fills the
 * bottom of the disc.
 */
private fun DrawScope.drawCharacter(
    art: AvatarArt,
    p: (Float, Float) -> Offset,
    r: (Float) -> Float
) {
    val ink = Color(0xFF2A2320)
    val skin = art.skin
    val hair = art.hair

    // ── shoulders / garment ───────────────────────────────────────────────
    drawRoundRect(
        color = art.garment,
        topLeft = p(20f, 74f),
        size = Size(r(60f), r(44f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(22f), r(22f))
    )
    // neck
    drawRoundRect(
        color = skin,
        topLeft = p(41f, 58f),
        size = Size(r(18f), r(16f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(7f), r(7f))
    )

    // ── back hair / helmet shell (drawn before the face) ──────────────────
    when (art.kind) {
        1, 5 -> drawRoundRect(  // bob / long hair: a soft mass behind the head
            color = hair,
            topLeft = p(24f, 20f),
            size = Size(r(52f), r(62f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(26f), r(26f))
        )
        7 -> {                  // pigtails
            drawCircle(hair, r(12f), p(22f, 50f))
            drawCircle(hair, r(12f), p(78f, 50f))
        }
        15 -> drawCircle(       // helmet shell
            color = Color(0x33FFFFFF),
            radius = r(38f),
            center = p(50f, 46f)
        )
    }

    // ── head ──────────────────────────────────────────────────────────────
    drawCircle(skin, r(22f), p(50f, 44f))
    // ears
    drawCircle(skin, r(4.5f), p(28f, 46f))
    drawCircle(skin, r(4.5f), p(72f, 46f))

    // ── hair / hat ────────────────────────────────────────────────────────
    when (art.kind) {
        0 -> {                  // beanie with a folded brim
            drawArc(
                color = hair, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = p(26f, 20f), size = Size(r(48f), r(48f))
            )
            drawRoundRect(
                color = hair,
                topLeft = p(25f, 30f), size = Size(r(50f), r(9f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(4.5f), r(4.5f))
            )
            drawCircle(Color.White.copy(alpha = 0.55f), r(4f), p(50f, 17f))
        }
        1 -> drawArc(           // bob fringe
            color = hair, startAngle = 190f, sweepAngle = 160f, useCenter = true,
            topLeft = p(27f, 21f), size = Size(r(46f), r(40f))
        )
        2 -> drawArc(           // bun + glasses
            color = hair, startAngle = 185f, sweepAngle = 170f, useCenter = true,
            topLeft = p(27f, 21f), size = Size(r(46f), r(38f))
        )
        3 -> {                  // curls
            for ((x, y, radius) in listOf(
                Triple(34f, 26f, 10f), Triple(50f, 20f, 11f), Triple(66f, 26f, 10f),
                Triple(28f, 38f, 8f), Triple(72f, 38f, 8f)
            )) {
                drawCircle(hair, r(radius), p(x, y))
            }
        }
        4 -> {                  // cap crown + headphones
            drawArc(
                color = hair, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = p(26f, 18f), size = Size(r(48f), r(46f))
            )
            drawRoundRect(      // brim
                color = art.garment,
                topLeft = p(30f, 28f), size = Size(r(46f), r(6f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(3f), r(3f))
            )
            drawArc(            // headband
                color = Color(0xFF3A3A3C), startAngle = 150f, sweepAngle = 240f, useCenter = false,
                topLeft = p(24f, 24f), size = Size(r(52f), r(48f)),
                style = Stroke(width = r(4f), cap = StrokeCap.Round)
            )
            drawRoundRect(
                color = Color(0xFF3A3A3C),
                topLeft = p(21f, 40f), size = Size(r(9f), r(15f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(4f), r(4f))
            )
            drawRoundRect(
                color = Color(0xFF3A3A3C),
                topLeft = p(70f, 40f), size = Size(r(9f), r(15f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(4f), r(4f))
            )
        }
        5 -> drawArc(           // centre-split fringe
            color = hair, startAngle = 195f, sweepAngle = 150f, useCenter = true,
            topLeft = p(27f, 22f), size = Size(r(46f), r(36f))
        )
        6 -> {                  // balding crown + full beard
            drawArc(
                color = hair, startAngle = 200f, sweepAngle = 140f, useCenter = true,
                topLeft = p(29f, 24f), size = Size(r(42f), r(26f))
            )
            drawArc(
                color = hair, startAngle = 0f, sweepAngle = 180f, useCenter = true,
                topLeft = p(30f, 44f), size = Size(r(40f), r(34f))
            )
        }
        7 -> {                  // fringe + bows
            drawArc(
                color = hair, startAngle = 190f, sweepAngle = 160f, useCenter = true,
                topLeft = p(27f, 21f), size = Size(r(46f), r(38f))
            )
            drawCircle(art.garment, r(5f), p(22f, 42f))
            drawCircle(art.garment, r(5f), p(78f, 42f))
        }
        8 -> {                  // wizard hat
            val hat = Path().apply {
                moveTo(p(50f, 4f).x, p(50f, 4f).y)
                quadraticBezierTo(
                    p(28f, 26f).x, p(28f, 26f).y,
                    p(30f, 30f).x, p(30f, 30f).y
                )
                lineTo(p(70f, 30f).x, p(70f, 30f).y)
                quadraticBezierTo(
                    p(72f, 26f).x, p(72f, 26f).y,
                    p(50f, 4f).x, p(50f, 4f).y
                )
                close()
            }
            drawPath(hat, hair)
            drawRoundRect(      // brim
                color = hair,
                topLeft = p(22f, 28f), size = Size(r(56f), r(7f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(3.5f), r(3.5f))
            )
            drawCircle(Color(0xFFF2C14E), r(2.6f), p(57f, 20f))
        }
        9 -> {                  // leaf crown
            for ((x, y) in listOf(30f to 24f, 40f to 18f, 50f to 16f, 60f to 18f, 70f to 24f)) {
                drawOval(
                    color = hair,
                    topLeft = p(x - 5f, y - 3f),
                    size = Size(r(10f), r(8f))
                )
            }
        }
        10 -> {                 // explorer goggles
            drawArc(
                color = hair, startAngle = 185f, sweepAngle = 170f, useCenter = true,
                topLeft = p(27f, 21f), size = Size(r(46f), r(36f))
            )
            drawRoundRect(
                color = art.garment,
                topLeft = p(28f, 29f), size = Size(r(44f), r(7f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(3.5f), r(3.5f))
            )
        }
        11 -> {                 // beret
            drawOval(
                color = hair,
                topLeft = p(25f, 15f), size = Size(r(50f), r(20f))
            )
            drawCircle(hair, r(3.4f), p(50f, 15f))
        }
        12 -> {                 // top bun + bow
            drawArc(
                color = hair, startAngle = 190f, sweepAngle = 160f, useCenter = true,
                topLeft = p(27f, 22f), size = Size(r(46f), r(36f))
            )
            drawCircle(hair, r(9f), p(50f, 13f))
            drawRoundRect(
                color = art.garment,
                topLeft = p(41f, 18f), size = Size(r(18f), r(6f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(3f), r(3f))
            )
        }
        13 -> drawArc(          // short crop
            color = hair, startAngle = 185f, sweepAngle = 170f, useCenter = true,
            topLeft = p(28f, 22f), size = Size(r(44f), r(32f))
        )
        14 -> {                 // hood up
            drawArc(
                color = hair, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = p(22f, 14f), size = Size(r(56f), r(62f))
            )
            drawArc(
                color = art.garment, startAngle = 0f, sweepAngle = 180f, useCenter = true,
                topLeft = p(30f, 54f), size = Size(r(40f), r(30f))
            )
        }
        15 -> {                 // helmet visor
            drawArc(
                color = Color(0x55FFFFFF), startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = p(28f, 28f), size = Size(r(44f), r(34f)),
                style = Stroke(width = r(3f))
            )
        }
    }

    // ── face ──────────────────────────────────────────────────────────────
    if (art.kind == 10) {
        // Goggles instead of eyes: two lenses over the face.
        drawCircle(ink, r(7f), p(40f, 44f))
        drawCircle(ink, r(7f), p(60f, 44f))
        drawCircle(Color.White.copy(alpha = 0.35f), r(2.4f), p(37f, 41f))
        drawCircle(Color.White.copy(alpha = 0.35f), r(2.4f), p(57f, 41f))
        drawRoundRect(
            color = art.garment,
            topLeft = p(45f, 42f), size = Size(r(10f), r(4f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(2f), r(2f))
        )
    } else if (art.kind == 15) {
        drawCircle(ink, r(2.6f), p(42f, 46f))
        drawCircle(ink, r(2.6f), p(58f, 46f))
    } else {
        drawCircle(ink, r(2.6f), p(42f, 46f))
        drawCircle(ink, r(2.6f), p(58f, 46f))
    }

    when (art.kind) {
        2 -> {                  // round glasses
            drawCircle(ink, r(8.5f), p(42f, 46f), style = Stroke(width = r(1.6f)))
            drawCircle(ink, r(8.5f), p(58f, 46f), style = Stroke(width = r(1.6f)))
            drawLine(ink, p(50f, 46f), p(50f, 46f), strokeWidth = r(1.6f))
        }
        5 -> {                  // earring
            drawCircle(Color(0xFFF2C14E), r(2.4f), p(26f, 53f))
        }
        13 -> {                 // freckles
            for ((x, y) in listOf(38f to 51f, 43f to 53f, 34f to 55f, 62f to 51f, 57f to 53f, 66f to 55f)) {
                drawCircle(art.hair.copy(alpha = 0.75f), r(1.2f), p(x, y))
            }
        }
        6 -> {                  // beard covers the mouth
            drawArc(
                color = hair, startAngle = 0f, sweepAngle = 180f, useCenter = true,
                topLeft = p(33f, 48f), size = Size(r(34f), r(24f))
            )
            return
        }
        14 -> {                 // hood shadow, no mouth
            return
        }
    }
    // mouth
    drawArc(
        color = ink.copy(alpha = 0.85f),
        startAngle = 15f, sweepAngle = 150f, useCenter = false,
        topLeft = p(43f, 50f), size = Size(r(14f), r(10f)),
        style = Stroke(width = r(2.2f), cap = StrokeCap.Round)
    )
}

/** The avatar picker's tile: the portrait, with a check when it is the chosen one. */
@Composable
internal fun AvatarPickerIcon(style: Int, selected: Boolean, onClick: () -> Unit) {
    SocialAvatar(style, 38.dp, onClick, ring = !selected)
    if (selected) {
        CurioIcon(CurioIcons.Check, null, tint = MaterialTheme.colorScheme.onSurface, size = 12.dp)
    }
}

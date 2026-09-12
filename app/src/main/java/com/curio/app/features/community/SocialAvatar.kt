package com.curio.app.features.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * The 28 code-drawn social portraits.
 *
 * Everything lives in a disc of one warm ground colour, lit from the top-left
 * with a soft inner rim, so an avatar reads as a pressed paper button rather
 * than a flat blob; the
 * character on top is drawn with three tones (garment, skin, hair) plus ink
 * for the face, and each of the 28 is a DIFFERENT character — a beanie, a bob,
 * a bun with glasses, headphones, a wizard hat, an explorer's goggles, a space
 * helmet, plus the twelve soft, feminine silhouettes added in v3xx52 (long
 * waves, a high ponytail, twin braids, a hime cut, a puff with a bow, a flower
 * crown …). Nothing is uploaded and nothing is stored: `avatar_style` is a
 * single small integer on the profile row.
 *
 * Drawn rather than shipped as assets on purpose: 28 styles cost no download,
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
    AvatarArt(Color(0xFF6E7699), Color(0xFFD9DEEA), Color(0xFFF2D6BE), Color(0xFF9FB8E8), 15), // space helmet
    // ── v3xx52 — the SOFT SET: twelve feminine silhouettes (waves,
    // ponytails, braids, puffs, flower crowns, bows) in the same pastel
    // family, with a wider spread of skin and hair tones so every member can
    // pick something that looks like them.
    AvatarArt(Color(0xFFF2B8CE), Color(0xFFB98FD8), Color(0xFFF7DFC8), Color(0xFF6B4A3A), 16), // waves + flower
    AvatarArt(Color(0xFFF7C9A9), Color(0xFF7FB4C9), Color(0xFFF3D4B4), Color(0xFFE0A44E), 17), // high ponytail
    AvatarArt(Color(0xFFBFD9F0), Color(0xFFE7A6B5), Color(0xFFF6DCC2), Color(0xFF3E2E28), 18), // twin braids
    AvatarArt(Color(0xFFD9C6EE), Color(0xFF6E7FB8), Color(0xFFF8E0C9), Color(0xFF241E1C), 19), // hime cut
    AvatarArt(Color(0xFFF6D5C0), Color(0xFF9ED0B8), Color(0xFFEFC8A4), Color(0xFF8A4E2E), 20), // bob + bow
    AvatarArt(Color(0xFFE6E0F5), Color(0xFFC98FA8), Color(0xFFF5D8BE), Color(0xFF4A3A32), 21), // half-up bun
    AvatarArt(Color(0xFFD6E7C6), Color(0xFFE9A5BE), Color(0xFFF7DCC0), Color(0xFF7A4A2C), 22), // flower crown
    AvatarArt(Color(0xFFF0C4D8), Color(0xFF6C8FBF), Color(0xFFF3D2B6), Color(0xFF2E2622), 23), // waves + star clips
    AvatarArt(Color(0xFFCCE3E8), Color(0xFF8E7CC3), Color(0xFFF6DCC6), Color(0xFFC98A3E), 24), // pixie + heart clip
    AvatarArt(Color(0xFFF3D9B0), Color(0xFF5E8C7A), Color(0xFFE9BE97), Color(0xFF3A2B24), 25), // waves + glasses
    AvatarArt(Color(0xFFE2D2F0), Color(0xFFF0A88C), Color(0xFF8C5A3C), Color(0xFF2A211D), 26), // puff + bow
    AvatarArt(Color(0xFFF7CFA8), Color(0xFF7E5AA0), Color(0xFFF2D3B8), Color(0xFF5B3A24), 27)  // braided crown
)

// The style count lives in the data layer (SocialApi.SOCIAL_AVATAR_STYLE_COUNT)
// because every read and write clamps against it AND because it must stay in
// step with the `avatar_style between 0 and …` check in supabase/schema.sql.

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
            val radius = size.minDimension / 2f
            val ink = Color(0xFF2A2320)

            // ── the disc ───────────────────────────────────────────────────
            // A soft LIGHT from the top-left instead of one flat fill, with a
            // deepened lower edge: the portrait sits in its own light rather
            // than reading as a sticker cut out of the page.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lerp(art.ground, Color.White, 0.24f),
                        art.ground,
                        lerp(art.ground, ink, 0.22f)
                    ),
                    center = Offset(size.width * 0.32f, size.height * 0.28f),
                    radius = radius * 1.4f
                ),
                radius = radius
            )

            drawCharacter(art, ::p, ::r)
            drawAvatarPolish(art, ::p, ::r)

            if (ring) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = radius - r(1.5f),
                    style = Stroke(width = r(2f))
                )
            }
        }
    }
}

/**
 * The finishing pass EVERY portrait gets.
 *
 * The 16 characters are different drawings, so what makes them one family is
 * the light: a shadow where the head meets the shoulders, a warm blush on the
 * cheeks, and a rim light along the side the light falls on. Without this the
 * faces read flat and slightly stuck-on at large sizes (a profile portrait),
 * which is exactly where a drawn avatar is judged.
 */
private fun DrawScope.drawAvatarPolish(
    art: AvatarArt,
    p: (Float, Float) -> Offset,
    r: (Float) -> Float
) {
    val ink = Color(0xFF2A2320)
    // The head is ON the shoulders: one soft oval of shade under the chin.
    drawOval(
        color = ink.copy(alpha = 0.15f),
        topLeft = p(33f, 57f),
        size = Size(r(34f), r(11f))
    )
    // Cheeks — a quiet warmth, never a painted-on dot.
    val blush = Color(0xFFDE6E52).copy(alpha = 0.20f)
    drawCircle(blush, r(5.5f), p(36.5f, 52.5f))
    drawCircle(blush, r(5.5f), p(63.5f, 52.5f))
    // Rim light, upper-left, exactly where the disc's own light comes from.
    drawArc(
        color = Color.White.copy(alpha = 0.28f),
        startAngle = 198f,
        sweepAngle = 74f,
        useCenter = false,
        topLeft = p(29f, 23f),
        size = Size(r(42f), r(42f)),
        style = Stroke(width = r(2.4f), cap = StrokeCap.Round)
    )
}

/** v3xx52 — a five-petal flower (a soft-set accessory: the hair flower, the
 *  flower crown). Petals ring a warm core, all in the design grid's units. */
private fun DrawScope.avatarFlower(center: Offset, petal: Float, petalColor: Color, coreColor: Color) {
    for (i in 0 until 5) {
        val a = -Math.PI / 2.0 + i * 2.0 * Math.PI / 5.0
        drawCircle(
            color = petalColor,
            radius = petal,
            center = Offset(
                center.x + (petal * 0.92f * kotlin.math.cos(a)).toFloat(),
                center.y + (petal * 0.92f * kotlin.math.sin(a)).toFloat()
            )
        )
    }
    drawCircle(coreColor, petal * 0.62f, center)
}

/** v3xx52 — a small ribbon bow: two loops either side of a knot. */
private fun DrawScope.avatarBow(center: Offset, half: Float, color: Color) {
    val left = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - half * 1.5f, center.y - half * 0.85f)
        lineTo(center.x - half * 1.5f, center.y + half * 0.85f)
        close()
    }
    val right = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x + half * 1.5f, center.y - half * 0.85f)
        lineTo(center.x + half * 1.5f, center.y + half * 0.85f)
        close()
    }
    drawPath(left, color)
    drawPath(right, color)
    drawPath(left, Color.White.copy(alpha = 0.55f), style = Stroke(width = half * 0.22f))
    drawPath(right, Color.White.copy(alpha = 0.55f), style = Stroke(width = half * 0.22f))
    drawCircle(color, half * 0.42f, center)
}

/** v3xx52 — a five-point star path centred at [center] (the hair clips). */
private fun fivePointStarPath(center: Offset, outer: Float): Path {
    val inner = outer * 0.44f
    return Path().apply {
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val a = -Math.PI / 2.0 + i * Math.PI / 5.0
            val x = center.x + (radius * kotlin.math.cos(a)).toFloat()
            val y = center.y + (radius * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** v3xx52 — a small heart path centred at [center] (the heart hair clip). */
private fun heartClipPath(center: Offset, half: Float): Path = Path().apply {
    moveTo(center.x, center.y + half * 0.78f)
    cubicTo(
        center.x - half * 1.22f, center.y - half * 0.14f,
        center.x - half * 0.60f, center.y - half * 1.06f,
        center.x, center.y - half * 0.34f
    )
    cubicTo(
        center.x + half * 0.60f, center.y - half * 1.06f,
        center.x + half * 1.22f, center.y - half * 0.14f,
        center.x, center.y + half * 0.78f
    )
    close()
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
        // ── v3xx52 — the soft set's silhouettes, drawn BEHIND the head so
        //    they frame the face instead of sitting on top of it. ─────────
        16, 23, 25, 27 -> {     // long waves past the shoulders, curled at the foot
            drawRoundRect(
                color = hair,
                topLeft = p(21f, 17f),
                size = Size(r(58f), r(70f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(29f), r(29f))
            )
            drawCircle(hair, r(13f), p(27f, 84f))
            drawCircle(hair, r(13f), p(73f, 84f))
        }
        17 -> {                 // high ponytail sweeping down the right
            drawRoundRect(
                color = hair,
                topLeft = p(24f, 18f),
                size = Size(r(52f), r(58f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(26f), r(26f))
            )
            drawCircle(hair, r(11f), p(76f, 32f))
            drawCircle(hair, r(10f), p(82f, 47f))
            drawCircle(hair, r(9f), p(84f, 63f))
            drawCircle(hair, r(7f), p(82f, 77f))
        }
        18 -> {                 // twin braids — five plaits each side
            drawRoundRect(
                color = hair,
                topLeft = p(25f, 18f),
                size = Size(r(50f), r(52f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(25f), r(25f))
            )
            for (i in 0 until 5) {
                val plait = 7.5f - i * 0.9f
                drawCircle(hair, r(plait), p(23f - i * 0.8f, 62f + i * 10f))
                drawCircle(hair, r(plait), p(77f + i * 0.8f, 62f + i * 10f))
            }
        }
        19 -> drawRoundRect(    // hime cut: straight sheets past the jaw
            color = hair,
            topLeft = p(22f, 18f),
            size = Size(r(56f), r(74f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(24f), r(24f))
        )
        20 -> drawRoundRect(    // bob
            color = hair,
            topLeft = p(23f, 19f),
            size = Size(r(54f), r(58f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(27f), r(27f))
        )
        21 -> drawRoundRect(    // half-up: the loose hair behind the shoulders
            color = hair,
            topLeft = p(24f, 19f),
            size = Size(r(52f), r(56f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(26f), r(26f))
        )
        22 -> {                 // curls behind the flower crown
            for ((x, y, radius) in listOf(
                Triple(33f, 26f, 11f), Triple(50f, 19f, 12f), Triple(67f, 26f, 11f),
                Triple(26f, 40f, 9f), Triple(74f, 40f, 9f)
            )) {
                drawCircle(hair, r(radius), p(x, y))
            }
        }
        26 -> drawCircle(hair, r(34f), p(50f, 38f))   // afro puff
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
        // ── v3xx52 — the soft set's front hair + accessories ─────────────
        16 -> {                 // side-swept waves + a flower above the ear
            drawArc(
                color = hair, startAngle = 190f, sweepAngle = 165f, useCenter = true,
                topLeft = p(27f, 21f), size = Size(r(46f), r(38f))
            )
            avatarFlower(p(31f, 30f), r(5f), Color(0xFFF7B7CE), Color(0xFFF6D27A))
        }
        17 -> {                 // smooth crown + a scrunchie
            drawArc(
                color = hair, startAngle = 200f, sweepAngle = 150f, useCenter = true,
                topLeft = p(29f, 22f), size = Size(r(42f), r(30f))
            )
            drawArc(
                color = art.garment, startAngle = 118f, sweepAngle = 120f, useCenter = false,
                topLeft = p(66f, 20f), size = Size(r(16f), r(14f)),
                style = Stroke(width = r(3.4f), cap = StrokeCap.Round)
            )
        }
        18 -> drawArc(          // centre-parted fringe
            color = hair, startAngle = 178f, sweepAngle = 184f, useCenter = true,
            topLeft = p(28f, 21f), size = Size(r(44f), r(36f))
        )
        19 -> {                 // blunt bangs over the straight sheets
            drawRoundRect(
                color = hair,
                topLeft = p(26f, 21f), size = Size(r(48f), r(15f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r(6f), r(6f))
            )
        }
        20 -> {                 // bob fringe + a headband bow
            drawArc(
                color = hair, startAngle = 190f, sweepAngle = 160f, useCenter = true,
                topLeft = p(27f, 21f), size = Size(r(46f), r(38f))
            )
            avatarBow(p(50f, 16f), r(8f), art.garment)
        }
        21 -> {                 // fringe + the top bun with its tie
            drawArc(
                color = hair, startAngle = 190f, sweepAngle = 160f, useCenter = true,
                topLeft = p(27f, 21f), size = Size(r(46f), r(38f))
            )
            drawCircle(hair, r(10f), p(50f, 12f))
            drawCircle(art.garment, r(3.4f), p(50f, 12f))
        }
        22 -> {                 // a crown of five petals along the hairline
            drawArc(
                color = hair, startAngle = 190f, sweepAngle = 165f, useCenter = true,
                topLeft = p(27f, 21f), size = Size(r(46f), r(36f))
            )
            for ((x, y) in listOf(31f to 26f, 40f to 21f, 50f to 19f, 60f to 21f, 69f to 26f)) {
                avatarFlower(p(x, y), r(4.6f), Color(0xFFF7B7CE), Color(0xFFFBE9A8))
            }
        }
        23 -> {                 // waves + two star clips
            drawArc(
                color = hair, startAngle = 195f, sweepAngle = 155f, useCenter = true,
                topLeft = p(27f, 22f), size = Size(r(46f), r(36f))
            )
            drawPath(fivePointStarPath(p(33f, 28f), r(4.6f)), Color(0xFFF6D27A))
            drawPath(fivePointStarPath(p(45f, 24f), r(3.6f)), Color(0xFFF6D27A))
        }
        24 -> {                 // short crop + a heart clip
            drawArc(
                color = hair, startAngle = 188f, sweepAngle = 168f, useCenter = true,
                topLeft = p(28f, 23f), size = Size(r(44f), r(30f))
            )
            drawPath(heartClipPath(p(62f, 25f), r(5.4f)), Color(0xFFE86A8C))
        }
        25 -> drawArc(          // centre split (the glasses come with the face)
            color = hair, startAngle = 195f, sweepAngle = 150f, useCenter = true,
            topLeft = p(27f, 22f), size = Size(r(46f), r(36f))
        )
        26 -> {                 // a smooth crown + a bow
            drawArc(
                color = hair, startAngle = 196f, sweepAngle = 160f, useCenter = true,
                topLeft = p(28f, 22f), size = Size(r(44f), r(32f))
            )
            avatarBow(p(68f, 24f), r(7f), art.garment)
        }
        27 -> {                 // a braided crown band across the hairline
            drawArc(
                color = hair, startAngle = 190f, sweepAngle = 165f, useCenter = true,
                topLeft = p(27f, 21f), size = Size(r(46f), r(38f))
            )
            for (i in 0 until 5) {
                drawCircle(
                    lerp(hair, Color.White, 0.18f), r(3.6f),
                    p(31f + i * 9.5f, 25f - i * 1.4f)
                )
            }
        }
    }

    // ── hair sheen ────────────────────────────────────────────────────────
    // A soft light across the top-left of the hair keeps a dark mass from
    // reading as flat ink (the goggles and the helmet carry their own).
    if (art.kind != 10 && art.kind != 15) {
        drawArc(
            color = Color.White.copy(alpha = 0.18f),
            startAngle = 205f, sweepAngle = 85f, useCenter = false,
            topLeft = p(30f, 19f), size = Size(r(40f), r(32f)),
            style = Stroke(width = r(3.6f), cap = StrokeCap.Round)
        )
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
        // v3xx52 — EXPRESSIVE EYES. The old two ink dots read blank at any
        // size; each eye is now a warm iris inside a white almond with a lash
        // line and a catch-light, with a soft brow above — the face finally
        // carries an expression instead of two specks.
        val iris = lerp(hair, ink, 0.45f)
        listOf(42f to -1f, 58f to 1f).forEach { (ex, side) ->
            drawOval(
                color = Color.White.copy(alpha = 0.94f),
                topLeft = p(ex - 5.4f, 42f),
                size = Size(r(10.8f), r(9.2f))
            )
            drawCircle(iris, r(4.4f), p(ex + side * 0.4f, 46.6f))
            drawCircle(ink, r(2.3f), p(ex + side * 0.4f, 46.6f))
            drawCircle(Color.White.copy(alpha = 0.95f), r(1.35f), p(ex - 1.6f, 44.9f))
            drawArc(
                color = ink.copy(alpha = 0.9f),
                startAngle = 196f, sweepAngle = 148f, useCenter = false,
                topLeft = p(ex - 5.6f, 41.4f), size = Size(r(11.2f), r(9f)),
                style = Stroke(width = r(1.5f), cap = StrokeCap.Round)
            )
            drawArc(
                color = ink.copy(alpha = 0.42f),
                startAngle = 205f, sweepAngle = 130f, useCenter = false,
                topLeft = p(ex - 6.4f, 33.6f), size = Size(r(12.8f), r(7f)),
                style = Stroke(width = r(1.5f), cap = StrokeCap.Round)
            )
        }
        drawCircle(ink.copy(alpha = 0.20f), r(1.5f), p(50f, 51.5f))
    }

    when (art.kind) {
        2, 25 -> {              // round glasses, with their temple arms
            drawCircle(ink, r(8.6f), p(41.5f, 46f), style = Stroke(width = r(1.6f)))
            drawCircle(ink, r(8.6f), p(58.5f, 46f), style = Stroke(width = r(1.6f)))
            drawLine(ink.copy(alpha = 0.7f), p(33f, 44f), p(28f, 42f), strokeWidth = r(1.5f), cap = StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.7f), p(67f, 44f), p(72f, 42f), strokeWidth = r(1.5f), cap = StrokeCap.Round)
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

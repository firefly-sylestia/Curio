package com.curio.app.features.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The 28 code-drawn social portraits.
 *
 * Everything lives in a disc of one warm ground colour, lit from the top-left
 * with a soft inner rim, so an avatar reads as a pressed paper button rather
 * than a flat blob; the character on top is drawn in the garment / skin / hair
 * tones of its row plus a warm ink, and each of the 28 is a DIFFERENT
 * character — a beanie, a bob, a bun with glasses, headphones, a wizard hat,
 * an explorer's goggles, a space helmet, plus the twelve soft silhouettes of
 * the v3xx52 set (long waves, a high ponytail, twin braids, a hime cut, a puff
 * with a bow, a flower crown …). Nothing is uploaded and nothing is stored:
 * `avatar_style` is a single small integer on the profile row.
 *
 * v386 — the DETAIL PASS. The first construction was deliberately simple (a
 * circle head, hair as one rounded rectangle, a mouth as a single arc) and at
 * 40dp it read as a toy; a portrait is judged at hero size, and that is where
 * it fell apart. Every style is now built from the same four layers:
 *
 *  1. a BUST — a real shoulder silhouette with a collar trim, fold lines and a
 *     shoulder highlight, over a neck that carries the jaw's shadow;
 *  2. a HEAD — an oval skull tapering into a jaw and chin, with ears, a cheek
 *     warmth, a forehead that sits in the hair's shade, an eyelid crease, a
 *     brow shaped per face, a nose with a bridge shade and nostrils, and a
 *     REAL mouth (upper lip line + lower lip + a highlight) unless the
 *     character hides it behind a beard or a hood;
 *  3. HAIR — a back mass plus the fringe, each with its own inner highlight
 *     and shaded roots, and locks/strand lights on the silhouettes that fall;
 *  4. the ACCESSORY it is known for, drawn with its own material (a ribbed
 *     beanie cuff and a fuzzy pompom, a bent wizard cone with a stitched band,
 *     headphone cups with a mic boom, goggles with lens glass, a helmet with a
 *     glare sweep and a riveted collar, braids with interlocking plaits).
 *
 * The tone tables are untouched: shading is derived from each row's own
 * garment / skin / hair colours (`darken`, `lighten`), so 28 detailed
 * characters still cost 28 small integers — no new assets, no new fields.
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

/** The warm ink every portrait is outlined and shadowed in. One colour, so 28
 *  differently-coloured characters still share one drawing language. */
private val INK = Color(0xFF2A2320)

/** A whisper of the skin's own red, for lips and cheeks. */
private val BLUSH = Color(0xFFDE6E52)
private val LIP = Color(0xFFC4625C)

// ── the design grid ─────────────────────────────────────────────────────────
// Every portrait is drawn on a 100×100 grid and scaled to the disc, so the same
// drawing is a 24dp chip and a 120dp hero portrait. [u] is the grid's scale in
// canvas pixels per unit: 1 design unit = u px.

/** A design-grid point → a canvas offset. */
private fun o(x: Float, y: Float, u: Float) = Offset(x * u, y * u)

/** A design-grid length → a canvas length. */
private fun s(v: Float, u: Float) = v * u

/** A design-grid corner radius. */
private fun cr(v: Float, u: Float) = CornerRadius(s(v, u), s(v, u))

// Path builders in design units (the canvas Path API takes pixels).
private fun Path.mv(x: Float, y: Float, u: Float) = moveTo(x * u, y * u)
private fun Path.ln(x: Float, y: Float, u: Float) = lineTo(x * u, y * u)
private fun Path.qd(cx: Float, cy: Float, x: Float, y: Float, u: Float) =
    quadraticBezierTo(cx * u, cy * u, x * u, y * u)

private fun Path.cu(
    x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float, u: Float
) = cubicTo(x1 * u, y1 * u, x2 * u, y2 * u, x * u, y * u)

/**
 * A WARM SHADOW of a skin tone.
 *
 * The channels are SCALED (red least, blue most), never mixed toward [INK]:
 * mixing skin with a near-black desaturates it, and a pale face's neck came out
 * grey — a different-coloured neck on the same head. Scaling keeps the hue and
 * only deepens the tone, which is what skin in shadow actually does.
 */
private fun shadeSkin(color: Color, amount: Float): Color = Color(
    red = color.red * (1f - 0.10f * amount),
    green = color.green * (1f - 0.17f * amount),
    blue = color.blue * (1f - 0.26f * amount),
    alpha = color.alpha
)

/** Darken a tone toward the portrait's ink (hair roots, jaw shadow, folds). */
private fun darken(color: Color, amount: Float) = lerp(color, INK, amount)

/** Lighten a tone toward white (hair sheen, cheekbones, collar trim). */
private fun lighten(color: Color, amount: Float) = lerp(color, Color.White, amount)

/**
 * The code-drawn social portrait for [style] (0–27).
 *
 * @param avatarSize the disc's diameter.
 * @param onClick  when set the disc is tappable (used by the picker and by
 *                 every author row that opens a profile).
 * @param ring     the soft inner rim. Keep it on for avatars that sit on a
 *                 card or a page background; turn it off inside a busy
 *                 composed row where the extra stroke reads as noise.
 * @param online   draws the live-presence dot in the lower-right corner. Only
 *                 ever set from a last-active stamp that is honestly fresh
 *                 (`CurioPerson.isActiveNow`) — an indicator that guesses is
 *                 worse than no indicator.
 */
@Composable
internal fun SocialAvatar(
    style: Int,
    avatarSize: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    ring: Boolean = true,
    online: Boolean = false
) {
    val art = AVATARS[style.coerceIn(0, AVATARS.lastIndex)]
    Box(
        modifier = Modifier
            .size(avatarSize)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(avatarSize)) {
            // One grid scale for the whole drawing (see the helpers above).
            val u = size.minDimension / 100f
            val radius = size.minDimension / 2f

            // ── the disc ───────────────────────────────────────────────────
            // A soft LIGHT from the top-left instead of one flat fill, with a
            // deepened lower edge: the portrait sits in its own light rather
            // than reading as a sticker cut out of the page.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lighten(art.ground, 0.24f),
                        art.ground,
                        darken(art.ground, 0.22f)
                    ),
                    center = Offset(size.width * 0.32f, size.height * 0.28f),
                    radius = radius * 1.4f
                ),
                radius = radius
            )

            // The character is CLIPPED to the disc. The canvas is a square,
            // so a bust drawn to the bottom of the grid would otherwise show
            // its corners poking out of the circle.
            clipPath(
                Path().apply { addOval(Rect(Offset.Zero, Size(size.width, size.height))) }
            ) {
                drawCharacter(art, u)
                drawAvatarLight(u)
            }

            if (ring) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = radius - s(1.5f, u),
                    style = Stroke(width = s(2f, u))
                )
            }
        }

        if (online) {
            // CUT OUT of the portrait, not painted over it: the ring of page
            // colour separates the dot from the art, which is what keeps it
            // readable on all 28 grounds in both themes.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(avatarSize * 0.30f)
                    .clip(CircleShape)
                    .background(ActiveDot)
                    .border(
                        width = (avatarSize * 0.05f).coerceAtLeast(1.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * The live-presence dot. One colour on purpose: it means "a stamp younger
 * than five minutes", and a second shade for "recently" would turn a yes/no
 * into something the user has to interpret.
 */
private val ActiveDot = Color(0xFF2FBF71)

/**
 * The finishing light EVERY portrait gets.
 *
 * The 28 characters are different drawings, so what makes them one family is
 * the light: a rim along the side the disc's own light falls on, and a soft
 * lift where the shoulder catches it. Without this the faces read flat and
 * slightly stuck-on at large sizes (a profile portrait), which is exactly
 * where a drawn avatar is judged.
 */
private fun DrawScope.drawAvatarLight(u: Float) {
    // Rim light, upper-left, exactly where the disc's own light comes from.
    // It rides the head/temple so it survives every hairstyle.
    drawArc(
        color = Color.White.copy(alpha = 0.26f),
        startAngle = 198f, sweepAngle = 74f, useCenter = false,
        topLeft = o(29f, 23f, u), size = Size(s(42f, u), s(42f, u)),
        style = Stroke(width = s(2.4f, u), cap = StrokeCap.Round)
    )
    // A hint of the same light on the near shoulder.
    drawArc(
        color = Color.White.copy(alpha = 0.14f),
        startAngle = 214f, sweepAngle = 42f, useCenter = false,
        topLeft = o(20f, 80f, u), size = Size(s(34f, u), s(22f, u)),
        style = Stroke(width = s(2.6f, u), cap = StrokeCap.Round)
    )
}

// ── the shared body ─────────────────────────────────────────────────────────

/**
 * The bust: a real shoulder silhouette with a crew neckline, its collar trim,
 * two fold lines and a highlight on the lit shoulder. The first construction
 * used one rounded rectangle for the whole body, which is what made every
 * portrait read as a toy at large sizes.
 */
private fun DrawScope.drawShoulders(art: AvatarArt, u: Float) {
    val garment = art.garment
    val shade = darken(garment, 0.22f)
    val bust = Path().apply {
        mv(13f, 100f, u)
        cu(15f, 89f, 25f, 80.5f, 38f, 78.6f, u)
        cu(43f, 77.8f, 45f, 80.6f, 50f, 80.6f, u)
        cu(55f, 80.6f, 57f, 77.8f, 62f, 78.6f, u)
        cu(75f, 80.5f, 85f, 89f, 87f, 100f, u)
        close()
    }
    drawPath(bust, garment)
    // The collar: a light trim riding the neckline, so the bust reads as a
    // garment with an opening rather than a block of colour.
    drawPath(
        Path().apply {
            mv(38.8f, 79.6f, u)
            cu(43.6f, 78.8f, 45.6f, 81.6f, 50f, 81.6f, u)
            cu(54.4f, 81.6f, 56.4f, 78.8f, 61.2f, 79.6f, u)
        },
        lighten(garment, 0.30f).copy(alpha = 0.9f),
        style = Stroke(width = s(2.2f, u), cap = StrokeCap.Round)
    )
    // Two soft folds falling from the shoulders.
    listOf(33f to 30f, 67f to 70f).forEach { (fromX, toX) ->
        drawPath(
            Path().apply {
                mv(fromX, 84f, u)
                cu(fromX - 1f, 89f, toX + 1f, 93f, toX, 99f, u)
            },
            shade.copy(alpha = 0.45f),
            style = Stroke(width = s(1.4f, u), cap = StrokeCap.Round)
        )
    }
    // Lit shoulder.
    drawArc(
        color = Color.White.copy(alpha = 0.20f),
        startAngle = 205f, sweepAngle = 50f, useCenter = false,
        topLeft = o(19f, 79f, u), size = Size(s(32f, u), s(20f, u)),
        style = Stroke(width = s(2.4f, u), cap = StrokeCap.Round)
    )
}

/**
 * The neck, in the shade tone (the jaw holds the light), with the jaw's own
 * shadow crossing it and a sliver of light down the throat. Drawn BEFORE the
 * head, so the chin overlaps it instead of the shadow sitting on the face.
 */
private fun DrawScope.drawNeck(art: AvatarArt, u: Float) {
    val skin = art.skin
    val neck = Path().apply {
        mv(40.5f, 57f, u)
        ln(59.5f, 57f, u)
        cu(60f, 66f, 61f, 72f, 63f, 77f, u)
        cu(56f, 78.6f, 44f, 78.6f, 37f, 77f, u)
        cu(39f, 72f, 40f, 66f, 40.5f, 57f, u)
        close()
    }
    drawPath(neck, shadeSkin(skin, 0.62f))
    // The jaw's shadow across the top of the neck: the neck's OWN deeper tone
    // rather than an ink wash, and a thinner stroke — a 3.6-unit black band was
    // the "weird tint" running under every chin.
    drawPath(
        Path().apply { mv(39.5f, 62f, u); cu(44f, 66f, 56f, 66f, 60.5f, 62f, u) },
        shadeSkin(skin, 1f).copy(alpha = 0.75f),
        style = Stroke(width = s(2.6f, u), cap = StrokeCap.Round)
    )
    // Light down the throat.
    drawPath(
        Path().apply { mv(47f, 62f, u); cu(46f, 68f, 46f, 73f, 47f, 77f, u) },
        lighten(skin, 0.20f).copy(alpha = 0.55f),
        style = Stroke(width = s(2f, u), cap = StrokeCap.Round)
    )
}

/**
 * The head: ears, an oval skull that tapers into a jaw and a chin, cheek
 * warmth, and the forehead sitting in the hair's shade. Replaces the plain
 * circle the first sixteen wore.
 */
private fun DrawScope.drawHead(art: AvatarArt, u: Float) {
    val skin = art.skin
    val earShade = shadeSkin(skin, 0.7f)
    // Ears first — the head covers their inner halves.
    listOf(25.2f to false, 68.6f to true).forEach { (x, right) ->
        drawOval(skin, topLeft = o(x, 41.5f, u), size = Size(s(6.2f, u), s(10.6f, u)))
        val inner = if (right) 72.9f else 27.1f
        val drift = if (right) 1f else -1f
        drawPath(
            Path().apply {
                mv(inner, 44.4f, u)
                qd(inner + drift, 47.6f, inner - drift * 0.4f, 50.6f, u)
            },
            earShade.copy(alpha = 0.85f),
            style = Stroke(width = s(1.1f, u), cap = StrokeCap.Round)
        )
    }
    val head = Path().apply {
        mv(50f, 21f, u)
        cu(63.5f, 21f, 71.5f, 30.5f, 71.5f, 42.5f, u)
        cu(71.5f, 52f, 65.5f, 61f, 57f, 65.2f, u)
        cu(54.2f, 66.9f, 52.2f, 67.6f, 50f, 67.6f, u)
        cu(47.8f, 67.6f, 45.8f, 66.9f, 43f, 65.2f, u)
        cu(34.5f, 61f, 28.5f, 52f, 28.5f, 42.5f, u)
        cu(28.5f, 30.5f, 36.5f, 21f, 50f, 21f, u)
        close()
    }
    drawPath(head, skin)
    // Cheek warmth, over the skin and under the features.
    drawCircle(BLUSH.copy(alpha = 0.16f), s(6.6f, u), o(36.2f, 52.6f, u))
    drawCircle(BLUSH.copy(alpha = 0.16f), s(6.6f, u), o(63.8f, 52.6f, u))
    // The jaw's shadow on the lit side's far edge, so the head has a back.
    drawPath(
        Path().apply { mv(70f, 46f, u); cu(68.5f, 55f, 63f, 62f, 55f, 65.4f, u) },
        shadeSkin(skin, 1f).copy(alpha = 0.30f),
        style = Stroke(width = s(2.6f, u), cap = StrokeCap.Round)
    )
    // The forehead sits in the hair's shade.
    drawPath(
        Path().apply { mv(33.5f, 36.5f, u); cu(40f, 31.5f, 60f, 31.5f, 66.5f, 36.5f, u) },
        INK.copy(alpha = 0.07f),
        style = Stroke(width = s(6f, u), cap = StrokeCap.Round)
    )
}

/**
 * The face — brows, eyes with an eyelid crease and a lower lid, a nose with a
 * bridge shade and nostrils, and a real mouth (upper lip line, lower lip and a
 * highlight). [withMouth] goes false for the two characters that hide it (the
 * beard, the hood).
 */
/**
 * The brows — a tapered arch each side.
 *
 * Drawn BEFORE the hair and the headwear (see [drawCharacter]): brows belong
 * UNDER a fringe and under a beanie cuff, so painting them with the rest of
 * the face would stamp dark arcs on top of a hat, or across a light fringe.
 */
private fun DrawScope.drawBrows(art: AvatarArt, u: Float) {
    // A BROW is a short warm mark, not a hair-coloured arc: mixing the hair
    // toward the ink turned light-haired characters grey, and the two arcs were
    // mirrored in X only, so one brow rose toward the nose while the other fell
    // away from it — the "not matching eyebrows". Both brows are now built from
    // the SAME three points mirrored about the face's midline, and they end at
    // the same height.
    val brow = if (art.hair.luminance() > 0.62f) {
        // Light hair (white, platinum, silver) needs a brow that reads AGAINST
        // the skin and still belongs to the hair.
        lerp(art.hair, INK, 0.55f)
    } else {
        lerp(art.hair, INK, 0.18f)
    }.copy(alpha = 0.92f)
    listOf(42.6f to -1f, 57.4f to 1f).forEach { (ex, side) ->
        drawPath(
            Path().apply {
                // Outer end, a whisper lower than the inner one — the gentlest
                // lift — mirrored through `side` so both brows agree.
                mv(ex + side * 5.2f, 39.6f, u)
                qd(ex + side * 0.6f, 36.4f, ex - side * 5.2f, 39.2f, u)
            },
            brow,
            style = Stroke(width = s(2.2f, u), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawFace(art: AvatarArt, u: Float, withMouth: Boolean = true) {
    val iris = darken(art.hair, 0.45f)
    // ── eyes ──────────────────────────────────────────────────────────────
    // CHIBI EYES. The detailed pair — a white sclera, a lash line, a lid crease
    // and a lower lid — was four grey lines smeared together at 40dp, and the
    // whites made every face read startled at hero size. A chibi eye is ONE big
    // dark shape with two highlights; its character comes from the size and the
    // tilt, not from the number of lines inside it.
    listOf(42.6f to -1f, 57.4f to 1f).forEach { (ex, side) ->
        val cy = 47.4f
        // Taller than wide, tilted a touch outward: open, friendly, and still
        // exactly the shape the glasses' round lenses expect.
        drawOval(
            color = iris,
            topLeft = o(ex - 4.6f, cy - 6.6f, u),
            size = Size(s(9.2f, u), s(13.2f, u))
        )
        // A deeper centre, so the eye has an inside rather than reading flat.
        drawCircle(INK, s(3.1f, u), o(ex, cy + 0.6f, u))
        // The big top highlight and one lower spark: two is the chibi
        // signature, and at chip size they are what keeps the eye from closing.
        drawCircle(Color.White.copy(alpha = 0.95f), s(2.7f, u), o(ex - 1.5f, cy - 3.4f, u))
        drawCircle(Color.White.copy(alpha = 0.72f), s(1.25f, u), o(ex + 1.5f, cy + 3.2f, u))
        // The upper lid: ONE stroke, the outer corner a little heavier (the
        // `side` swing), so the eye stays defined when the whole portrait is
        // 24dp in a chat row.
        drawArc(
            color = INK.copy(alpha = 0.80f),
            startAngle = if (side < 0f) 206f else 194f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = o(ex - 4.9f, cy - 7.2f, u),
            size = Size(s(9.8f, u), s(8.6f, u)),
            style = Stroke(width = s(1.5f, u), cap = StrokeCap.Round)
        )
    }
    // ── nose ──────────────────────────────────────────────────────────────
    // A chibi nose is a hint: one short warm shadow under the tip. The bridge
    // line and the two nostril dots were three more marks that only ever
    // muddied a small portrait.
    drawPath(
        Path().apply { mv(47.8f, 55.2f, u); qd(50f, 56.8f, 52.2f, 55.2f, u) },
        shadeSkin(art.skin, 1f).copy(alpha = 0.55f),
        style = Stroke(width = s(1.3f, u), cap = StrokeCap.Round)
    )
    // ── mouth ─────────────────────────────────────────────────────────────
    if (!withMouth) return
    // Lower lip, filled.
    drawPath(
        Path().apply {
            mv(45.8f, 59.8f, u)
            cu(47.5f, 62.9f, 52.5f, 62.9f, 54.2f, 59.8f, u)
            cu(52.4f, 61.1f, 47.6f, 61.1f, 45.8f, 59.8f, u)
            close()
        },
        lerp(art.skin, LIP, 0.55f)
    )
    drawCircle(Color.White.copy(alpha = 0.32f), s(1.1f, u), o(49.4f, 60.7f, u))
    // Upper lip line with its cupid's bow.
    drawPath(
        Path().apply {
            mv(44.8f, 59.4f, u)
            qd(47.3f, 58.2f, 50f, 59f, u)
            qd(52.7f, 58.2f, 55.2f, 59.4f, u)
        },
        INK.copy(alpha = 0.72f),
        style = Stroke(width = s(1.4f, u), cap = StrokeCap.Round)
    )
}

/** Long hair's inner highlight — a soft arc of light across the crown. */
private fun DrawScope.hairSheen(
    u: Float,
    left: Float = 31f, top: Float = 20f, width: Float = 38f, height: Float = 30f,
    startAngle: Float = 205f, sweepAngle: Float = 82f,
    alpha: Float = 0.18f
) {
    drawArc(
        color = Color.White.copy(alpha = alpha),
        startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false,
        topLeft = o(left, top, u), size = Size(s(width, u), s(height, u)),
        style = Stroke(width = s(3.4f, u), cap = StrokeCap.Round)
    )
}

/** Two or three fine strand lights following a falling lock. */
private fun DrawScope.hairStrands(
    u: Float,
    hair: Color,
    points: List<Triple<Float, Float, Float>>
) {
    val light = lighten(hair, 0.32f).copy(alpha = 0.42f)
    points.forEach { (x, y, bend) ->
        drawPath(
            Path().apply {
                mv(x, y, u)
                cu(x + bend, y + 12f, x - bend * 0.6f, y + 24f, x + bend * 0.3f, y + 34f, u)
            },
            light,
            style = Stroke(width = s(1.5f, u), cap = StrokeCap.Round)
        )
    }
}

/** A lock of hair falling past the shoulder, with its curl at the foot. */
private fun DrawScope.hairLock(hair: Color, u: Float, x: Float, y: Float, width: Float, length: Float) {
    drawRoundRect(
        color = hair,
        topLeft = o(x, y, u),
        size = Size(s(width, u), s(length, u)),
        cornerRadius = cr(width / 2f, u)
    )
    drawCircle(hair, s(width * 0.55f, u), o(x + width / 2f, y + length - width * 0.2f, u))
}

// ── the character ───────────────────────────────────────────────────────────

/**
 * The character itself, in FOUR passes so every style reads as the same
 * drawing with a different silhouette:
 *
 *  1. [drawHairBack]  — everything BEHIND the head (hair masses, hoods, the
 *     helmet's glass, ponytails and braids), so it frames the face;
 *  2. the bust + neck + head (shared, identical for all 28);
 *  3. [drawHairFront] — the fringe, the covering hat or helmet, the crown and
 *     every head accessory;
 *  4. [drawFace] + the face accessories (glasses, freckles, an earring).
 */
private fun DrawScope.drawCharacter(art: AvatarArt, u: Float) {
    val hair = art.hair
    drawHairBack(art, u)
    drawShoulders(art, u)
    drawNeck(art, u)
    drawHead(art, u)
    drawBrows(art, u)
    drawHairFront(art, u)
    // The hood swallows the mouth; the beard covers it (its own hair pass
    // draws the moustache), so the face knows before it draws.
    drawFace(art, u, withMouth = art.kind != 6 && art.kind != 14)
    when (art.kind) {
        2, 25 -> {                       // round glasses with their temple arms
            drawCircle(INK, s(8.8f, u), o(41.6f, 46.4f, u), style = Stroke(width = s(1.6f, u)))
            drawCircle(INK, s(8.8f, u), o(58.4f, 46.4f, u), style = Stroke(width = s(1.6f, u)))
            // Glass: a soft diagonal glare in each lens.
            drawLine(
                Color.White.copy(alpha = 0.30f),
                o(38.4f, 43.4f, u), o(43.4f, 41.6f, u),
                strokeWidth = s(1.6f, u), cap = StrokeCap.Round
            )
            drawLine(
                Color.White.copy(alpha = 0.30f),
                o(55.2f, 43.4f, u), o(60.2f, 41.6f, u),
                strokeWidth = s(1.6f, u), cap = StrokeCap.Round
            )
            drawLine(
                INK.copy(alpha = 0.75f),
                o(49.8f, 45.4f, u), o(50.2f, 45.4f, u),
                strokeWidth = s(1.6f, u), cap = StrokeCap.Round
            )
            drawLine(
                INK.copy(alpha = 0.7f),
                o(32.8f, 44f, u), o(27.8f, 41.8f, u),
                strokeWidth = s(1.5f, u), cap = StrokeCap.Round
            )
            drawLine(
                INK.copy(alpha = 0.7f),
                o(67.2f, 44f, u), o(72.2f, 41.8f, u),
                strokeWidth = s(1.5f, u), cap = StrokeCap.Round
            )
        }
        5 -> {                           // a gold hoop on the near ear
            drawCircle(Color(0xFFF2C14E), s(2.6f, u), o(26.4f, 53.4f, u), style = Stroke(width = s(1.6f, u)))
            drawCircle(Color(0xFFF2C14E), s(0.9f, u), o(26.4f, 51.6f, u))
        }
        13 -> {                          // freckles across the nose and cheeks
            listOf(
                38.6f to 51.4f, 42.4f to 53.2f, 35.4f to 54.6f,
                47.4f to 53.4f, 52.6f to 53.4f,
                61.4f to 51.4f, 57.6f to 53.2f, 64.6f to 54.6f
            ).forEach { (x, y) ->
                drawCircle(darken(hair, 0.25f).copy(alpha = 0.62f), s(1.05f, u), o(x, y, u))
            }
        }
    }
}

/**
 * Everything BEHIND the head. Each family gets its own silhouette (a shaped
 * mass rather than one rounded rectangle), its roots in the shade tone and its
 * sheen — this is the layer that decides whether a portrait looks drawn or
 * assembled.
 */
private fun DrawScope.drawHairBack(art: AvatarArt, u: Float) {
    val hair = art.hair
    val shade = darken(hair, 0.30f)
    when (art.kind) {
        1, 20 -> {                       // bob: an A-line mass past the jaw
            drawRoundRect(
                color = hair,
                topLeft = o(24f, 19f, u), size = Size(s(52f, u), s(60f, u)),
                cornerRadius = cr(25f, u)
            )
            // The ends curl inward — what makes a bob a bob.
            drawCircle(hair, s(8f, u), o(28f, 76f, u))
            drawCircle(hair, s(8f, u), o(72f, 76f, u))
            drawCircle(shade.copy(alpha = 0.5f), s(4.2f, u), o(28f, 78f, u))
            drawCircle(shade.copy(alpha = 0.5f), s(4.2f, u), o(72f, 78f, u))
        }
        2, 11, 12, 21 -> {               // a mass behind, the rest is front hair
            drawRoundRect(
                color = hair,
                topLeft = o(26f, 20f, u), size = Size(s(48f, u), s(46f, u)),
                cornerRadius = cr(24f, u)
            )
        }
        3, 22, 26 -> {                   // a curl / puff cluster
            val curl = listOf(
                Triple(33f, 25f, 11f), Triple(50f, 18.5f, 12.5f), Triple(67f, 25f, 11f),
                Triple(25.5f, 39f, 9.5f), Triple(74.5f, 39f, 9.5f),
                Triple(21.5f, 55f, 8.5f), Triple(78.5f, 55f, 8.5f)
            )
            curl.forEach { (x, y, radius) ->
                drawCircle(hair, s(radius, u), o(x, y, u))
            }
            // Roots: a shade pass along the inside of the cluster.
            curl.forEachIndexed { index, (x, y, radius) ->
                if (index % 2 == 0) {
                    drawCircle(shade.copy(alpha = 0.30f), s(radius * 0.5f, u), o(x - 1.5f, y + radius * 0.7f, u))
                }
            }
        }
        5, 16, 23, 25 -> {               // long waves over the shoulders
            drawRoundRect(
                color = hair,
                topLeft = o(20.5f, 16.5f, u), size = Size(s(59f, u), s(72f, u)),
                cornerRadius = cr(29f, u)
            )
            hairLock(hair, u, x = 20f, y = 62f, width = 15f, length = 26f)
            hairLock(hair, u, x = 65f, y = 62f, width = 15f, length = 26f)
            drawCircle(shade.copy(alpha = 0.35f), s(6.4f, u), o(24f, 88f, u))
            drawCircle(shade.copy(alpha = 0.35f), s(6.4f, u), o(76f, 88f, u))
        }
        7 -> {                           // pigtails: a tail each side
            listOf(20.5f, 79.5f).forEach { x ->
                drawCircle(hair, s(12.5f, u), o(x, 48f, u))
                drawCircle(hair, s(11f, u), o(x, 60f, u))
                drawCircle(hair, s(9.5f, u), o(x, 71f, u))
                drawCircle(hair, s(7.5f, u), o(x, 81f, u))
                drawCircle(shade.copy(alpha = 0.35f), s(5f, u), o(x, 82f, u))
            }
        }
        10 -> {                          // a short mass under the goggles
            drawRoundRect(
                color = hair,
                topLeft = o(25.5f, 20f, u), size = Size(s(49f, u), s(50f, u)),
                cornerRadius = cr(24.5f, u)
            )
        }
        14 -> {                          // the hood's outer shell
            drawPath(
                Path().apply {
                    mv(50f, 12f, u)
                    cu(71f, 12f, 81f, 30f, 81f, 52f, u)
                    cu(81f, 72f, 70f, 84f, 50f, 84f, u)
                    cu(30f, 84f, 19f, 72f, 19f, 52f, u)
                    cu(19f, 30f, 29f, 12f, 50f, 12f, u)
                    close()
                },
                hair
            )
        }
        15 -> {                          // the helmet's glass shell
            drawCircle(Color.White.copy(alpha = 0.16f), s(38.5f, u), o(50f, 46f, u))
            drawCircle(
                Color(0xFFAFC4E8).copy(alpha = 0.35f), s(38.5f, u), o(50f, 46f, u),
                style = Stroke(width = s(2.2f, u))
            )
        }
        17 -> {                          // a high ponytail sweeping down the right
            drawRoundRect(
                color = hair,
                topLeft = o(24f, 18f, u), size = Size(s(52f, u), s(54f, u)),
                cornerRadius = cr(26f, u)
            )
            val tail = Path().apply {
                mv(66f, 30f, u)
                cu(82f, 32f, 88f, 48f, 85f, 64f, u)
                cu(83f, 76f, 79f, 84f, 74f, 90f, u)
                cu(70f, 84f, 70f, 72f, 72f, 60f, u)
                cu(73f, 48f, 70f, 38f, 66f, 30f, u)
                close()
            }
            drawPath(tail, hair)
        }
        18 -> {                          // twin braids
            drawRoundRect(
                color = hair,
                topLeft = o(25f, 18.5f, u), size = Size(s(50f, u), s(50f, u)),
                cornerRadius = cr(25f, u)
            )
            listOf(23f to 1f, 77f to -1f).forEach { (x, side) ->
                for (i in 0 until 5) {
                    val plait = 7.8f - i * 1.05f
                    drawCircle(hair, s(plait, u), o(x - i * 0.9f * side, 60f + i * 9.5f, u))
                    drawCircle(
                        lighten(hair, 0.18f).copy(alpha = 0.5f), s(plait * 0.42f, u),
                        o(x - i * 0.9f * side - 1.4f * side, 58.5f + i * 9.5f, u)
                    )
                }
                drawRoundRect(   // the tie
                    color = art.garment,
                    topLeft = o(x - 3.4f, 94f, u), size = Size(s(6.8f, u), s(4f, u)),
                    cornerRadius = cr(2f, u)
                )
            }
        }
        19 -> {                          // hime cut: straight sheets past the jaw
            drawRoundRect(
                color = hair,
                topLeft = o(22f, 18f, u), size = Size(s(56f, u), s(74f, u)),
                cornerRadius = cr(22f, u)
            )
            drawRoundRect(   // the straight, blunt ends
                color = shade.copy(alpha = 0.35f),
                topLeft = o(23.5f, 84f, u), size = Size(s(53f, u), s(7f, u)),
                cornerRadius = cr(3f, u)
            )
        }
        24 -> {                          // a short crop, a hint of mass behind
            drawRoundRect(
                color = hair,
                topLeft = o(27f, 21f, u), size = Size(s(46f, u), s(40f, u)),
                cornerRadius = cr(23f, u)
            )
        }
        27 -> {                          // a mass under the braided crown
            drawRoundRect(
                color = hair,
                topLeft = o(23.5f, 18f, u), size = Size(s(53f, u), s(64f, u)),
                cornerRadius = cr(26.5f, u)
            )
        }
    }
}

/**
 * Everything IN FRONT of the head: the fringe, the hat or helmet a style is
 * known for, and the small ornaments (bows, flowers, clips) that finish it.
 */
private fun DrawScope.drawHairFront(art: AvatarArt, u: Float) {
    val hair = art.hair
    val shade = darken(hair, 0.28f)
    val light = lighten(hair, 0.22f)
    // A plain fringe arc shared by most styles — the style's own block then
    // adds its fringe shape, sheen and ornaments on top. The height stops the
    // arc just ABOVE the brows (y≈40), so a fringe leaves them visible instead
    // of swallowing the whole forehead.
    fun fringe(top: Float, height: Float) {
        drawArc(
            color = hair, startAngle = 190f, sweepAngle = 160f, useCenter = true,
            topLeft = o(27f, top, u), size = Size(s(46f, u), s(height, u))
        )
    }
    when (art.kind) {
        // ── hats and headwear ─────────────────────────────────────────────
        0 -> {                            // knitted beanie with a ribbed cuff
            drawArc(
                color = hair, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = o(26f, 19f, u), size = Size(s(48f, u), s(50f, u))
            )
            drawCircle(light.copy(alpha = 0.5f), s(8f, u), o(38f, 26f, u))
            // The cuff: a folded band with its ribs.
            drawRoundRect(
                color = hair,
                topLeft = o(24.5f, 30f, u), size = Size(s(51f, u), s(10f, u)),
                cornerRadius = cr(5f, u)
            )
            for (i in 0 until 9) {
                drawLine(
                    shade.copy(alpha = 0.55f),
                    o(26.5f + i * 5.5f, 31.4f, u), o(26.5f + i * 5.5f, 38.6f, u),
                    strokeWidth = s(0.9f, u), cap = StrokeCap.Round
                )
            }
            // The pompom, with a fuzz of its own.
            drawCircle(light, s(5.6f, u), o(50f, 15.5f, u))
            listOf(-4.6f to -1.6f, 4.6f to -1.6f, 0f to 4.6f, -3.2f to 3.4f, 3.2f to 3.4f).forEach { (dx, dy) ->
                drawCircle(light.copy(alpha = 0.85f), s(3f, u), o(50f + dx * 0.7f, 15.5f + dy * 0.6f, u))
            }
            drawCircle(darken(light, 0.10f).copy(alpha = 0.5f), s(2.2f, u), o(48.4f, 16.6f, u))
        }
        4 -> {                            // cap + headphones
            drawArc(
                color = hair, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = o(26f, 18f, u), size = Size(s(48f, u), s(48f, u))
            )
            drawCircle(Color.White.copy(alpha = 0.16f), s(6.5f, u), o(37f, 26f, u))
            // Visor: a curved brim, front-on.
            drawPath(
                Path().apply {
                    mv(29f, 29f, u)
                    qd(50f, 36.5f, 71f, 29f, u)
                    qd(50f, 32f, 29f, 29f, u)
                    close()
                },
                art.garment
            )
            drawLine(
                lighten(art.garment, 0.30f),
                o(30f, 29.6f, u), o(70f, 29.6f, u),
                strokeWidth = s(1f, u), cap = StrokeCap.Round
            )
            // Headphone band, ear cups with a ring and a mic boom.
            drawArc(
                color = Color(0xFF3A3A3C), startAngle = 150f, sweepAngle = 240f, useCenter = false,
                topLeft = o(22.5f, 22.5f, u), size = Size(s(55f, u), s(50f, u)),
                style = Stroke(width = s(4.4f, u), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF5A5A5E), startAngle = 160f, sweepAngle = 70f, useCenter = false,
                topLeft = o(23.5f, 23.5f, u), size = Size(s(53f, u), s(48f, u)),
                style = Stroke(width = s(1.6f, u), cap = StrokeCap.Round)
            )
            listOf(18.6f, 71.4f).forEach { x ->
                drawRoundRect(
                    color = Color(0xFF3A3A3C),
                    topLeft = o(x, 39f, u), size = Size(s(10f, u), s(17f, u)),
                    cornerRadius = cr(4.5f, u)
                )
                drawRoundRect(
                    color = Color(0xFF55555A),
                    topLeft = o(x + 2.2f, 42f, u), size = Size(s(5.6f, u), s(11f, u)),
                    cornerRadius = cr(2.8f, u)
                )
            }
            drawPath(
                Path().apply {
                    mv(71f, 54f, u)
                    cu(66f, 60f, 60f, 62f, 56.5f, 60f, u)
                },
                Color(0xFF3A3A3C),
                style = Stroke(width = s(1.8f, u), cap = StrokeCap.Round)
            )
            drawCircle(Color(0xFF3A3A3C), s(2.4f, u), o(55.6f, 60f, u))
        }
        8 -> {                            // wizard hat: a bent cone, a stitched band
            val cone = Path().apply {
                mv(52.5f, 3.5f, u)
                cu(46f, 12f, 40f, 22f, 36.5f, 30f, u)
                ln(64.5f, 30f, u)
                cu(62f, 21f, 59f, 12f, 52.5f, 3.5f, u)
                close()
            }
            drawPath(cone, hair)
            // A soft light down the cone's front, and the fold of the bend.
            drawPath(
                Path().apply { mv(52f, 6f, u); cu(47.5f, 14f, 43.5f, 22f, 41.5f, 29f, u) },
                light.copy(alpha = 0.5f),
                style = Stroke(width = s(2.4f, u), cap = StrokeCap.Round)
            )
            drawPath(
                Path().apply { mv(57f, 21f, u); qd(60f, 24f, 63f, 29.5f, u) },
                shade.copy(alpha = 0.45f),
                style = Stroke(width = s(1.8f, u), cap = StrokeCap.Round)
            )
            // The curved brim + its stitching.
            drawPath(
                Path().apply {
                    mv(22f, 30f, u)
                    cu(32f, 26.5f, 68f, 26.5f, 78f, 30f, u)
                    cu(68f, 34f, 32f, 34f, 22f, 30f, u)
                    close()
                },
                hair
            )
            val band = darken(hair, 0.42f)
            drawRoundRect(
                color = band,
                topLeft = o(35.6f, 26.6f, u), size = Size(s(28.8f, u), s(4.6f, u)),
                cornerRadius = cr(2.3f, u)
            )
            for (i in 0 until 6) {
                drawCircle(light.copy(alpha = 0.7f), s(0.7f, u), o(37.4f + i * 4.6f, 28.9f, u))
            }
            drawPath(fivePointStarPath(o(60.5f, 8.5f, u), s(3.2f, u)), Color(0xFFF2C14E))
            drawPath(fivePointStarPath(o(46.5f, 15f, u), s(2f, u)), Color(0xFFF2C14E).copy(alpha = 0.85f))
        }
        9 -> {                            // laurel crown: leaves + berries
            for (i in 0 until 5) {
                val x = 30f + i * 10f
                val y = 24f - kotlin.math.abs(2.5f - i) * 2.4f
                avatarLeaf(o(x - 3.4f, y - 3.4f, u), s(7.4f, u), s(5f, u), -26f, hair)
                avatarLeaf(o(x + 0.4f, y - 4.4f, u), s(7.4f, u), s(5f, u), 22f, light.copy(alpha = 0.92f))
            }
            drawCircle(Color(0xFFE9615E), s(2.1f, u), o(34f, 20.5f, u))
            drawCircle(Color(0xFFE9615E), s(2.1f, u), o(66f, 20.5f, u))
            drawCircle(Color.White.copy(alpha = 0.35f), s(0.8f, u), o(33.3f, 19.8f, u))
        }
        10 -> {                           // explorer goggles
            val strap = darken(hair, 0.15f)
            drawRoundRect(   // the strap across the head
                color = strap,
                topLeft = o(26f, 28.4f, u), size = Size(s(48f, u), s(7.4f, u)),
                cornerRadius = cr(3.7f, u)
            )
            listOf(37.4f, 62.6f).forEach { x ->
                drawCircle(darken(strap, 0.30f), s(3.4f, u), o(x, 32f, u))
            }
            // Lens rings with glass, bridge and rivets.
            listOf(40f, 60f).forEach { x ->
                drawCircle(Color(0xFF3A3A3C), s(9.4f, u), o(x, 44.5f, u))
                drawCircle(art.garment, s(8.2f, u), o(x, 44.5f, u))
                drawCircle(Color(0x33FFFFFF), s(6.4f, u), o(x, 44.5f, u))
                drawCircle(Color(0xFF3A3A3C), s(6.4f, u), o(x, 44.5f, u), style = Stroke(width = s(1.4f, u)))
                drawCircle(Color.White.copy(alpha = 0.35f), s(2f, u), o(x - 2.6f, 41.6f, u))
            }
            drawRoundRect(
                color = Color(0xFF3A3A3C),
                topLeft = o(46.6f, 42.6f, u), size = Size(s(6.8f, u), s(3.8f, u)),
                cornerRadius = cr(1.9f, u)
            )
        }
        11 -> {                           // beret
            drawOval(hair, topLeft = o(24f, 14f, u), size = Size(s(52f, u), s(22f, u)))
            drawOval(
                light.copy(alpha = 0.45f),
                topLeft = o(28f, 16.4f, u), size = Size(s(24f, u), s(8f, u))
            )
            drawPath(
                Path().apply { mv(26f, 30f, u); cu(36f, 27.4f, 64f, 27.4f, 74f, 30f, u) },
                darken(hair, 0.35f),
                style = Stroke(width = s(3.2f, u), cap = StrokeCap.Round)
            )
            drawCircle(darken(hair, 0.20f), s(2.2f, u), o(50f, 13.6f, u))
        }
        14 -> {                           // the hood's rim, its shadow + strings
            drawPath(
                Path().apply {
                    mv(50f, 20f, u)
                    cu(72f, 20f, 79f, 38f, 75f, 58f, u)
                    cu(70f, 76f, 62f, 84f, 50f, 84f, u)
                    cu(38f, 84f, 30f, 76f, 25f, 58f, u)
                    cu(21f, 38f, 28f, 20f, 50f, 20f, u)
                    close()
                },
                darken(hair, 0.18f)
            )
            // The opening: a dark oval so the face sits INSIDE the hood.
            drawOval(
                color = INK.copy(alpha = 0.20f),
                topLeft = o(29f, 25f, u), size = Size(s(42f, u), s(54f, u))
            )
            drawPath(
                Path().apply { mv(28f, 30f, u); cu(33f, 24f, 40f, 21f, 50f, 21f, u) },
                lighten(hair, 0.30f).copy(alpha = 0.5f),
                style = Stroke(width = s(3f, u), cap = StrokeCap.Round)
            )
            // Drawstrings.
            listOf(42f, 58f).forEach { x ->
                drawPath(
                    Path().apply { mv(x, 62f, u); cu(x - 1.4f, 70f, x + 1.4f, 76f, x, 82f, u) },
                    lighten(art.garment, 0.35f),
                    style = Stroke(width = s(2.2f, u), cap = StrokeCap.Round)
                )
                drawCircle(lighten(art.garment, 0.20f), s(1.9f, u), o(x, 83.4f, u))
            }
        }
        15 -> {                           // the helmet's visor + collar ring
            drawPath(
                Path().apply {
                    mv(50f, 22f, u)
                    cu(66f, 22f, 74f, 32f, 74f, 45f, u)
                    cu(74f, 58f, 64f, 66f, 50f, 66f, u)
                    cu(36f, 66f, 26f, 58f, 26f, 45f, u)
                    cu(26f, 32f, 34f, 22f, 50f, 22f, u)
                    close()
                },
                Color(0x44FFFFFF)
            )
            // The glare sweep — what makes it read as glass.
            drawPath(
                Path().apply {
                    mv(31f, 34f, u)
                    cu(38f, 27f, 50f, 25f, 57f, 27f, u)
                    cu(48f, 30f, 38f, 38f, 33f, 46f, u)
                    cu(31f, 42f, 30f, 38f, 31f, 34f, u)
                    close()
                },
                Color.White.copy(alpha = 0.32f)
            )
            drawOval(
                color = Color.White.copy(alpha = 0.30f),
                topLeft = o(61f, 44f, u), size = Size(s(8f, u), s(12f, u))
            )
            // Collar ring with rivets.
            drawRoundRect(
                color = art.garment,
                topLeft = o(28f, 62f, u), size = Size(s(44f, u), s(9f, u)),
                cornerRadius = cr(4.5f, u)
            )
            drawRoundRect(
                color = lighten(art.garment, 0.30f),
                topLeft = o(28f, 62f, u), size = Size(s(44f, u), s(2.6f, u)),
                cornerRadius = cr(1.3f, u)
            )
            for (i in 0 until 5) {
                drawCircle(darken(art.garment, 0.35f), s(1.2f, u), o(34f + i * 8f, 67f, u))
            }
            // The antenna's little nub.
            drawCircle(art.garment, s(2.2f, u), o(75.5f, 30f, u))
            drawCircle(Color(0xFFE9615E), s(1.2f, u), o(75.5f, 30f, u))
        }
        // ── hair styles ───────────────────────────────────────────────────
        1 -> {                            // bob: a soft fringe + a sheen band
            fringe(21f, 30f)
            hairSheen(u)
            drawPath(
                Path().apply { mv(30f, 30f, u); cu(38f, 24f, 62f, 24f, 70f, 30f, u) },
                shade.copy(alpha = 0.30f),
                style = Stroke(width = s(2.2f, u), cap = StrokeCap.Round)
            )
        }
        2 -> {                            // fringe + the bun at the back of the crown
            fringe(21f, 30f)
            hairSheen(u)
            drawCircle(hair, s(10.5f, u), o(50f, 15f, u))
            drawPath(
                Path().apply { mv(48.6f, 11f, u); cu(50f, 13.4f, 51.6f, 15.4f, 51.4f, 18f, u) },
                shade.copy(alpha = 0.5f),
                style = Stroke(width = s(1.4f, u), cap = StrokeCap.Round)
            )
        }
        3 -> {                            // curls across the hairline + a sheen
            listOf(34f to 27f, 50f to 21f, 66f to 27f, 28f to 37f, 72f to 37f).forEach { (x, y) ->
                drawCircle(hair, s(9.5f, u), o(x, y, u))
                drawCircle(light.copy(alpha = 0.45f), s(3.4f, u), o(x - 2f, y - 2.4f, u))
            }
            drawCircle(shade.copy(alpha = 0.30f), s(4.4f, u), o(50f, 25f, u))
        }
        5 -> {                            // centre-split long hair + strands
            drawArc(
                color = hair, startAngle = 208f, sweepAngle = 124f, useCenter = true,
                topLeft = o(27f, 21f, u), size = Size(s(46f, u), s(38f, u))
            )
            drawPath(
                Path().apply { mv(50f, 22f, u); cu(49f, 27f, 48.4f, 31f, 47.6f, 35f, u) },
                shade.copy(alpha = 0.45f),
                style = Stroke(width = s(1.4f, u), cap = StrokeCap.Round)
            )
            hairSheen(u, left = 29f, top = 19f, width = 42f, height = 34f, sweepAngle = 94f)
            hairStrands(u, hair, listOf(Triple(25.5f, 40f, 1.6f), Triple(74.5f, 40f, -1.6f)))
        }
        6 -> {                            // balding crown + a full beard
            drawArc(
                color = hair, startAngle = 200f, sweepAngle = 140f, useCenter = true,
                topLeft = o(30f, 25f, u), size = Size(s(40f, u), s(24f, u))
            )
            drawPath(
                Path().apply {
                    mv(31f, 40f, u)
                    cu(29f, 52f, 34f, 62f, 42f, 66f, u)
                    cu(46f, 68.4f, 54f, 68.4f, 58f, 66f, u)
                    cu(66f, 62f, 71f, 52f, 69f, 40f, u)
                    cu(66f, 50f, 60f, 55f, 50f, 55f, u)
                    cu(40f, 55f, 34f, 50f, 31f, 40f, u)
                    close()
                },
                hair
            )
            // A moustache, and the mouth's gap between beard and lip.
            drawPath(
                Path().apply {
                    mv(43.6f, 56.6f, u)
                    cu(46f, 58.4f, 54f, 58.4f, 56.4f, 56.6f, u)
                    cu(57f, 58.6f, 53f, 60.6f, 50f, 60.6f, u)
                    cu(47f, 60.6f, 43f, 58.6f, 43.6f, 56.6f, u)
                    close()
                },
                darken(hair, 0.12f)
            )
            drawPath(
                Path().apply { mv(30.6f, 44f, u); cu(31f, 54f, 38f, 63f, 50f, 64f, u) },
                lighten(hair, 0.22f).copy(alpha = 0.35f),
                style = Stroke(width = s(2.2f, u), cap = StrokeCap.Round)
            )
        }
        7 -> {                            // pigtail fringe + two bows
            fringe(21f, 30f)
            hairSheen(u)
            listOf(20.5f to true, 79.5f to false).forEach { (x, left) ->
                drawRoundRect(   // the tie
                    color = art.garment,
                    topLeft = o(x - 3.6f, 42f, u), size = Size(s(7.2f, u), s(4.4f, u)),
                    cornerRadius = cr(2.2f, u)
                )
                avatarBow(o(x, 44.6f, u), s(5.4f, u), art.garment, if (left) 200f else -20f)
            }
        }
        12 -> {                           // top bun + a band bow
            fringe(22f, 30f)
            hairSheen(u, left = 30f, top = 21f, width = 40f, height = 30f)
            drawCircle(hair, s(9.5f, u), o(50f, 13f, u))
            drawPath(
                Path().apply { mv(46.4f, 10.4f, u); cu(49f, 13f, 52f, 14.6f, 53.4f, 17.4f, u) },
                shade.copy(alpha = 0.5f),
                style = Stroke(width = s(1.4f, u), cap = StrokeCap.Round)
            )
            drawRoundRect(
                color = art.garment,
                topLeft = o(41f, 18.4f, u), size = Size(s(18f, u), s(5.4f, u)),
                cornerRadius = cr(2.7f, u)
            )
            avatarBow(o(50f, 21f, u), s(4.6f, u), art.garment, 0f)
        }
        13, 24 -> {                       // short crop
            drawArc(
                color = hair, startAngle = 186f, sweepAngle = 168f, useCenter = true,
                topLeft = o(28f, 22.5f, u), size = Size(s(44f, u), s(31f, u))
            )
            hairSheen(u, left = 32f, top = 23f, width = 36f, height = 26f, sweepAngle = 96f)
            if (art.kind == 24) {         // a heart clip, with its shadow
                drawPath(heartClipPath(o(63f, 26f, u), s(5.2f, u)), INK.copy(alpha = 0.14f))
                drawPath(heartClipPath(o(63f, 25.2f, u), s(5.2f, u)), Color(0xFFE86A8C))
                drawCircle(Color.White.copy(alpha = 0.45f), s(1.2f, u), o(61.4f, 23.6f, u))
            }
        }
        16 -> {                           // side-swept waves + a flower
            drawArc(
                color = hair, startAngle = 196f, sweepAngle = 158f, useCenter = true,
                topLeft = o(27f, 21f, u), size = Size(s(46f, u), s(32f, u))
            )
            hairSheen(u)
            hairStrands(u, hair, listOf(Triple(23.5f, 38f, 1.8f), Triple(76.5f, 38f, -1.8f)))
            avatarFlower(o(31.5f, 30.5f, u), s(5f, u), Color(0xFFF7B7CE), Color(0xFFF6D27A))
        }
        17 -> {                           // smooth crown + a scrunchie
            drawArc(
                color = hair, startAngle = 202f, sweepAngle = 146f, useCenter = true,
                topLeft = o(29f, 22f, u), size = Size(s(42f, u), s(30f, u))
            )
            hairSheen(u, left = 32f, top = 22f, width = 36f, height = 26f)
            // The tie, then the tail's strands.
            drawCircle(art.garment, s(4.6f, u), o(72f, 31f, u))
            for (i in 0 until 3) {
                drawLine(
                    darken(art.garment, 0.25f),
                    o(69.6f + i * 2.4f, 28f, u), o(71f + i * 2.4f, 34f, u),
                    strokeWidth = s(0.9f, u), cap = StrokeCap.Round
                )
            }
            hairStrands(u, hair, listOf(Triple(81f, 40f, 2.2f), Triple(84f, 52f, 2f), Triple(80f, 64f, 1.6f)))
        }
        18 -> {                           // centre-parted fringe + braid ties
            drawArc(
                color = hair, startAngle = 178f, sweepAngle = 184f, useCenter = true,
                topLeft = o(28f, 21f, u), size = Size(s(44f, u), s(36f, u))
            )
            hairSheen(u)
            drawPath(
                Path().apply { mv(50f, 22f, u); cu(49.4f, 27f, 48.8f, 31f, 48.4f, 35f, u) },
                shade.copy(alpha = 0.40f),
                style = Stroke(width = s(1.3f, u), cap = StrokeCap.Round)
            )
        }
        19 -> {                           // blunt bangs over the straight sheets
            drawRoundRect(
                color = hair,
                topLeft = o(26f, 21f, u), size = Size(s(48f, u), s(15.5f, u)),
                cornerRadius = cr(6f, u)
            )
            drawRoundRect(
                color = light.copy(alpha = 0.35f),
                topLeft = o(31f, 22.4f, u), size = Size(s(18f, u), s(3.4f, u)),
                cornerRadius = cr(1.7f, u)
            )
            drawLine(
                shade.copy(alpha = 0.55f),
                o(27f, 35.6f, u), o(73f, 35.6f, u),
                strokeWidth = s(1.3f, u), cap = StrokeCap.Round
            )
        }
        20 -> {                           // bob fringe + a headband bow
            fringe(21f, 30f)
            hairSheen(u)
            drawRoundRect(
                color = darken(hair, 0.30f),
                topLeft = o(27f, 29f, u), size = Size(s(46f, u), s(3.4f, u)),
                cornerRadius = cr(1.7f, u)
            )
            avatarBow(o(50f, 17.5f, u), s(7.4f, u), art.garment, 0f)
        }
        21 -> {                           // fringe + the top bun's tie
            fringe(21f, 30f)
            hairSheen(u)
            drawCircle(hair, s(10f, u), o(50f, 12.5f, u))
            drawPath(
                Path().apply { mv(47f, 9.6f, u); cu(49.4f, 12.4f, 52f, 14f, 53f, 17.4f, u) },
                shade.copy(alpha = 0.5f),
                style = Stroke(width = s(1.4f, u), cap = StrokeCap.Round)
            )
            drawCircle(art.garment, s(3f, u), o(50f, 12.5f, u))
            drawCircle(art.garment, s(1.1f, u), o(50f, 12.5f, u))
        }
        22 -> {                           // a crown of five blooms
            fringe(21f, 30f)
            for ((x, y) in listOf(31f to 26f, 40f to 20.5f, 50f to 18.5f, 60f to 20.5f, 69f to 26f)) {
                avatarFlower(o(x, y, u), s(4.6f, u), Color(0xFFF7B7CE), Color(0xFFFBE9A8))
            }
            // Two buds tucked into the band.
            drawCircle(Color(0xFFC7E3A6), s(1.6f, u), o(35.6f, 22.4f, u))
            drawCircle(Color(0xFFC7E3A6), s(1.6f, u), o(64.4f, 22.4f, u))
        }
        23 -> {                           // waves + two star clips
            drawArc(
                color = hair, startAngle = 196f, sweepAngle = 154f, useCenter = true,
                topLeft = o(27f, 22f, u), size = Size(s(46f, u), s(31f, u))
            )
            hairSheen(u, left = 30f, top = 21f, width = 40f, height = 32f)
            hairStrands(u, hair, listOf(Triple(24.5f, 40f, 1.8f), Triple(75.5f, 40f, -1.8f)))
            listOf(Triple(33f, 28f, 4.8f), Triple(44f, 24f, 3.6f)).forEach { (x, y, star) ->
                // A clipped-in pin sits ON the hair: a soft contact shadow
                // under it, then the metal and its highlight.
                drawCircle(INK.copy(alpha = 0.12f), s(star * 0.85f, u), o(x + 0.6f, y + 1.1f, u))
                drawPath(fivePointStarPath(o(x, y, u), s(star, u)), Color(0xFFF6D27A))
                drawPath(
                    fivePointStarPath(o(x, y, u), s(star, u)),
                    Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = s(0.7f, u))
                )
            }
        }
        25 -> {                           // centre-split waves (the glasses are on the face)
            drawArc(
                color = hair, startAngle = 204f, sweepAngle = 132f, useCenter = true,
                topLeft = o(27f, 22f, u), size = Size(s(46f, u), s(36f, u))
            )
            hairSheen(u, left = 30f, top = 21f, width = 40f, height = 32f)
            drawPath(
                Path().apply { mv(50f, 22f, u); cu(49.2f, 27f, 48.6f, 31f, 48f, 35f, u) },
                shade.copy(alpha = 0.40f),
                style = Stroke(width = s(1.3f, u), cap = StrokeCap.Round)
            )
            hairStrands(u, hair, listOf(Triple(24f, 40f, 1.8f), Triple(76f, 40f, -1.8f)))
        }
        26 -> {                           // a smooth crown + a bow
            drawArc(
                color = hair, startAngle = 198f, sweepAngle = 154f, useCenter = true,
                topLeft = o(28f, 22f, u), size = Size(s(44f, u), s(32f, u))
            )
            hairSheen(u, left = 31f, top = 21f, width = 38f, height = 28f)
            avatarBow(o(68.5f, 24f, u), s(6.6f, u), art.garment, 18f)
        }
        27 -> {                           // a braided crown band
            drawArc(
                color = hair, startAngle = 190f, sweepAngle = 165f, useCenter = true,
                topLeft = o(27f, 21f, u), size = Size(s(46f, u), s(32f, u))
            )
            for (i in 0 until 6) {
                val x = 30.5f + i * 7.8f
                val y = 25.5f - i * 1.1f
                drawCircle(darken(hair, 0.18f), s(3.7f, u), o(x, y, u))
                drawCircle(lighten(hair, 0.30f).copy(alpha = 0.8f), s(2.2f, u), o(x - 0.9f, y - 1f, u))
                drawPath(
                    Path().apply { mv(x - 3.6f, y + 1.6f, u); qd(x, y + 4.4f, x + 3.6f, y + 1.6f, u) },
                    darken(hair, 0.30f).copy(alpha = 0.4f),
                    style = Stroke(width = s(0.8f, u), cap = StrokeCap.Round)
                )
            }
            hairSheen(u, left = 29f, top = 30f, width = 42f, height = 30f, sweepAngle = 60f)
        }
    }
}

// ── accessories ─────────────────────────────────────────────────────────────

/** A five-petal flower (the hair flower, the flower crown): petals with a
 *  shaded outer edge ring a warm core. */
private fun DrawScope.avatarFlower(center: Offset, petal: Float, petalColor: Color, coreColor: Color) {
    for (i in 0 until 5) {
        val a = -PI / 2.0 + i * 2.0 * PI / 5.0
        val x = center.x + (petal * 0.92f * cos(a)).toFloat()
        val y = center.y + (petal * 0.92f * sin(a)).toFloat()
        drawCircle(petalColor, petal, Offset(x, y))
        drawCircle(darken(petalColor, 0.14f).copy(alpha = 0.55f), petal * 0.42f, Offset(x + petal * 0.2f, y + petal * 0.3f))
        drawCircle(lighten(petalColor, 0.35f).copy(alpha = 0.6f), petal * 0.3f, Offset(x - petal * 0.24f, y - petal * 0.28f))
    }
    drawCircle(coreColor, petal * 0.62f, center)
    drawCircle(darken(coreColor, 0.20f).copy(alpha = 0.5f), petal * 0.3f, Offset(center.x + petal * 0.12f, center.y + petal * 0.16f))
}

/** A ribbon bow: two shaded loops around a knot, with two short tails. It can
 *  be rotated ([rotation]°) so it can sit on a band, a pigtail or a crown. */
private fun DrawScope.avatarBow(center: Offset, half: Float, color: Color, rotation: Float = 0f) {
    val rad = rotation * PI.toFloat() / 180f
    fun aPoint(dx: Float, dy: Float): Offset {
        val cosR = cos(rad)
        val sinR = sin(rad)
        return Offset(center.x + dx * cosR - dy * sinR, center.y + dx * sinR + dy * cosR)
    }
    val shade = darken(color, 0.22f)
    // The bow is built in CANVAS pixels (it is placed in a rotated frame), so
    // it uses the raw Path API rather than the design-unit helpers above.
    // Tails first, so the loops cover their tops.
    listOf(-1f, 1f).forEach { side ->
        val tailOut = aPoint(side * half * 1.1f, half * 1.5f)
        val tailIn = aPoint(side * half * 0.5f, half * 1.7f)
        drawPath(
            Path().apply {
                moveTo(center.x, center.y)
                lineTo(tailOut.x, tailOut.y)
                lineTo(tailIn.x, tailIn.y)
                close()
            },
            shade
        )
    }
    listOf(-1f, 1f).forEach { side ->
        val top = aPoint(side * half * 1.6f, -half * 0.9f)
        val bottom = aPoint(side * half * 1.5f, half * 0.95f)
        val loop = Path().apply {
            moveTo(center.x, center.y)
            lineTo(top.x, top.y)
            lineTo(bottom.x, bottom.y)
            close()
        }
        drawPath(loop, color)
        drawPath(
            loop,
            lighten(color, 0.35f).copy(alpha = 0.5f),
            style = Stroke(width = half * 0.22f)
        )
    }
    // The knot, with its fold.
    drawCircle(color, half * 0.46f, center)
    drawCircle(shade.copy(alpha = 0.65f), half * 0.2f, Offset(center.x + half * 0.12f, center.y + half * 0.14f))
}

/** One laurel leaf: an oval, rotated, with its vein. */
private fun DrawScope.avatarLeaf(center: Offset, width: Float, height: Float, rotation: Float, color: Color) {
    val rad = rotation * PI.toFloat() / 180f
    val left = Offset(
        center.x - (width / 2f) * cos(rad) + (height / 2f) * sin(rad),
        center.y - (width / 2f) * sin(rad) - (height / 2f) * cos(rad)
    )
    drawOval(color = color, topLeft = left, size = Size(width, height))
    drawLine(
        darken(color, 0.28f).copy(alpha = 0.5f),
        left,
        Offset(left.x + width * cos(rad), left.y + width * sin(rad)),
        strokeWidth = height * 0.09f,
        cap = StrokeCap.Round
    )
}

/** A five-point star path centred at [center] (the hair clips). */
private fun fivePointStarPath(center: Offset, outer: Float): Path {
    val inner = outer * 0.44f
    return Path().apply {
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val a = -PI / 2.0 + i * PI / 5.0
            val x = center.x + (radius * cos(a)).toFloat()
            val y = center.y + (radius * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** A small heart path centred at [center] (the heart hair clip). */
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
 * The avatar picker's tile.
 *
 * v3xx52 — the chosen portrait is marked by a ROUNDED SELECTION RING (a soft
 * accent halo plus a 2dp circular outline, with the portrait stepping up a
 * little), not by a tick badge laid over the face: the face is the point of
 * the tile, and a check covering its chin read as an error mark on a picker
 * where every option is valid. The ring hugs the disc exactly like the app's
 * other selected chips (RoundedCornerShape(50) = a circle here), so the picker
 * speaks the same language as the rails and filter pills.
 */
@Composable
internal fun AvatarPickerIcon(
    style: Int,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(if (selected) 48.dp else 44.dp)
            .clip(RoundedCornerShape(50))
            .then(
                if (selected) {
                    Modifier
                        .background(accent.copy(alpha = 0.14f))
                        .border(2.dp, accent, RoundedCornerShape(50))
                } else {
                    Modifier
                }
            )
            // A picker with a write in flight goes inert rather than lying:
            // the same rule the username field follows while it saves.
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // The click lives on the tile (above), so the portrait itself is not
        // also clickable — one tap target, one haptic, no double ripple.
        SocialAvatar(
            style = style,
            avatarSize = if (selected) 38.dp else 36.dp,
            onClick = null,
            ring = !selected
        )
    }
}

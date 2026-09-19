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
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * THE SOCIAL AVATARS, REDRAWN FROM ZERO (v395) — twenty COZY-MINIMAL portraits
 * and eight COZY-MINIMAL icons.
 *
 * ## Why this file was thrown away and rewritten
 *
 * The previous cast was built up in passes — a construction pass, a detail pass,
 * an illustrated pass, a pose pass — and every pass added shapes to the SAME
 * drawing. Twenty-eight characters ended up as one bust, one skull, one nose and
 * one mouth with different hats on, and at avatar size the accumulated detail
 * collapsed into mush (user report: "the current avatars are so bad, like
 * genuinely so bad, no detail and all … fully redraw them from scratch"). A
 * portrait is judged at 40 dp where a bust is about 34 px tall: at that size
 * THREE clean shapes beat thirty small ones, and a face reads from the EYES and
 * the SILHOUETTE alone.
 *
 * So nothing here is carried over. The whole drawing is:
 *
 *  1. a WARM DISC — one radial gradient lit from the top-left, so the avatar
 *     reads as a pressed paper button rather than a sticker;
 *  2. a BUST — one soft shoulder path with a collar, drawn ONCE for the cast;
 *  3. a HEAD — one oval, one ear pair, a jaw shade and a blush;
 *  4. a FACE — two eyes, two brows, a nose tick and a mouth, all from a small
 *     named vocabulary of five gazes and four mouths;
 *  5. ONE SILHOUETTE per character — hair or headwear drawn as a single clean
 *     mass with an ink contour, which is what actually tells twenty people
 *     apart at a glance;
 *  6. ONE PROP — the small thing that character is known by (a pompom, a
 *     headphone cup, a laurel, an earring, a flower).
 *
 * Every mass is drawn through [fill] or [bar] / [ring] / [oval] / [dot], so the
 * ink contour, the round caps and the grid are decided in ONE place — the way a
 * minimal set stays consistent across 28 rows instead of drifting per shape.
 *
 * ## What an emoji row is
 *
 * The eight icons are the same disc and the same warm palette with a single
 * object on it — a crescent moon, a sun, a star, a cloud, a rainbow, a mountain,
 * a cup, a heart — drawn with the same primitives as the portraits. That is why they sit
 * in the same picker and in the same bubble as a portrait without looking like a
 * different app: same ground, same light, same ink.
 *
 * ## The contract that must not drift
 *
 * `avatar_style` is a single small integer on the profile row, and it is an
 * INDEX into this file. [PORTRAITS] (20) followed by [ICONS] (8) must therefore
 * add up to `SOCIAL_AVATAR_STYLE_COUNT`, and `supabase/schema.sql`'s
 * `avatar_style between 0 and …` check must be widened in the SAME commit that
 * adds a style here, or a member's new pick is rejected on write.
 */

// ────────────────────────────────────────────────────────────────────────────
// The cast
// ────────────────────────────────────────────────────────────────────────────

/**
 * One portrait's palette and character.
 *
 * @param ground   the disc behind the character.
 * @param garment  the bust, and the material of any headwear the row wears.
 * @param skin     the face, ears and neck.
 * @param hair     the hair mass — also the beard, the brows of some rows and any
 *                 prop drawn in the character's own material.
 * @param kind     WHICH character to draw (see [drawHairBack] / [drawHairFront] /
 *                 [drawProp]). Kept separate from the row's position so the
 *                 picker's order can change without redrawing anybody.
 * @param face     the gaze (see the `FACE_*` table).
 * @param mouth    the mouth (see the `MOUTH_*` table).
 * @param tilt     the head, in degrees, turned about the top of the neck.
 * @param lean     the whole character, in grid units, off the disc's centre.
 */
private class AvatarArt(
    val ground: Color,
    val garment: Color,
    val skin: Color,
    val hair: Color,
    val kind: Int,
    val face: Int,
    val mouth: Int,
    val tilt: Float = 0f,
    val lean: Float = 0f
)

// The palettes are the app's own warm paper family with a few lane accents, so a
// disc sits naturally beside the rose settings family and the lane chips.
//
// v395 — ten men, ten women, and each shelf is a deliberate set: the men are a
// cap, a full beard, springy curls with glasses, a knit beanie, headphones, a
// wizard, a hood, a top knot, a laurel and a side part; the women are a bob,
// long waves, a high bun, pigtails, braids, a hime cut, a flower crown, a
// beret, a headband and space buns. No two rows share a silhouette, a gaze or a
// mouth, and every row leans its own way.
//
// v398 — AND EVERY ROW LEANS A LITTLE. The tilts ran to eight degrees, which is
// enough to walk the head off its own collar and read as a broken neck rather
// than a pose (user report: "the poses are weird"). They are now within four
// degrees, in the same directions, so the cast still turns its own way without
// any row looking bent.
private val PORTRAITS: List<AvatarArt> = listOf(
    // ── the men ───────────────────────────────────────────────────────────
    AvatarArt(Color(0xFFE9A9A2), Color(0xFF6C8FBF), Color(0xFFF3D2B6), Color(0xFF4A3B33), KIND_CAP, FACE_OPEN, MOUTH_SMILE, -3f, 1f),
    AvatarArt(Color(0xFFD8A98F), Color(0xFF6B5A4E), Color(0xFFE8C09A), Color(0xFF463832), KIND_BEARD, FACE_CALM, MOUTH_SMILE, -1f, 0f),
    AvatarArt(Color(0xFFE8C583), Color(0xFFDA7F63), Color(0xFFE9BE97), Color(0xFF4A362C), KIND_CURLS, FACE_OPEN, MOUTH_GRIN, 3f, 2f),
    AvatarArt(Color(0xFFB9A4E0), Color(0xFF4E5A78), Color(0xFFF2D3B8), Color(0xFF443222), KIND_BEANIE, FACE_SLEEPY, MOUTH_SOFT, -3f, -1f),
    AvatarArt(Color(0xFF8E93C9), Color(0xFF3F4E86), Color(0xFFF1CFA9), Color(0xFF2F2723), KIND_HEADPHONES, FACE_HAPPY, MOUTH_GRIN, -4f, -2f),
    AvatarArt(Color(0xFFA8BE8C), Color(0xFF5F7A4A), Color(0xFFEBC49C), Color(0xFFD9D2C4), KIND_WIZARD, FACE_OPEN, MOUTH_OPEN, 2f, -1f),
    AvatarArt(Color(0xFF9FB0BE), Color(0xFF7A5B45), Color(0xFFF2D2B0), Color(0xFF453730), KIND_HOOD, FACE_SLEEPY, MOUTH_SOFT, -4f, -1f),
    AvatarArt(Color(0xFFE3CFA6), Color(0xFF5E7F9C), Color(0xFFF4D8BC), Color(0xFF8C5A34), KIND_TOPKNOT, FACE_CALM, MOUTH_SMILE, 1f, 1f),
    AvatarArt(Color(0xFFAE8FBC), Color(0xFF4A5870), Color(0xFFEFE2D2), Color(0xFF3C4553), KIND_LAUREL, FACE_HAPPY, MOUTH_GRIN, -2f, 1f),
    AvatarArt(Color(0xFFF0AEC4), Color(0xFF4F7F72), Color(0xFFF6DCC6), Color(0xFF5B3A24), KIND_SIDE_PART, FACE_WINK, MOUTH_GRIN, 3f, 2f),
    // ── the women ─────────────────────────────────────────────────────────
    AvatarArt(Color(0xFF9FB8E8), Color(0xFFE7C6A8), Color(0xFFF6DCC4), Color(0xFF8C4A2F), KIND_BOB, FACE_OPEN, MOUTH_SOFT, 2f, -1f),
    AvatarArt(Color(0xFF9FD0CB), Color(0xFFE4A15C), Color(0xFFF4D7BE), Color(0xFF5B3A24), KIND_WAVES, FACE_HAPPY, MOUTH_SMILE, 2f, -2f),
    AvatarArt(Color(0xFFC5989C), Color(0xFF7E4A52), Color(0xFFF5D9C0), Color(0xFF4A3634), KIND_HIGH_BUN, FACE_CALM, MOUTH_SOFT, 1f, -1f),
    AvatarArt(Color(0xFFEFA785), Color(0xFF7FB4C9), Color(0xFFF7DEC6), Color(0xFF6B3F26), KIND_PIGTAILS, FACE_WINK, MOUTH_GRIN, -4f, 2f),
    AvatarArt(Color(0xFF6E7699), Color(0xFFD9DEEA), Color(0xFFF2D6BE), Color(0xFF40332E), KIND_BRAIDS, FACE_HAPPY, MOUTH_SMILE, 2f, -2f),
    AvatarArt(Color(0xFFF2B8CE), Color(0xFFB98FD8), Color(0xFFF7DFC8), Color(0xFF6B4A3A), KIND_HIME, FACE_SLEEPY, MOUTH_SOFT, -1f, 1f),
    AvatarArt(Color(0xFFF7C9A9), Color(0xFFE7A6B5), Color(0xFFF3D4B4), Color(0xFFE0A44E), KIND_FLOWERS, FACE_OPEN, MOUTH_SMILE, 3f, -2f),
    AvatarArt(Color(0xFFBFD9F0), Color(0xFF6E7FB8), Color(0xFFF6DCC2), Color(0xFF4A3730), KIND_BERET, FACE_CALM, MOUTH_SOFT, -3f, 2f),
    AvatarArt(Color(0xFFD9C6EE), Color(0xFF5E7F9C), Color(0xFFF8E0C9), Color(0xFF7A4A2C), KIND_HEADBAND, FACE_HAPPY, MOUTH_SMILE, -3f, 2f),
    AvatarArt(Color(0xFFF5C9B0), Color(0xFF6B8F7A), Color(0xFFEFD3B8), Color(0xFF3F3330), KIND_SPACE_BUNS, FACE_OPEN, MOUTH_GRIN, 2f, 0f)
)

/**
 * One icon avatar — the same disc, one cozy object on it.
 *
 * [main] is the object, [light] its highlight or its second material, [deep] the
 * shade that gives it depth (a crater, a snow shadow, the coffee in the cup).
 */
private class IconArt(
    val ground: Color,
    val main: Color,
    val light: Color,
    val deep: Color,
    val kind: Int
)

private val ICONS: List<IconArt> = listOf(
    IconArt(Color(0xFF3B4166), Color(0xFFF6E7C5), Color(0xFFFFF8E7), Color(0xFFE0C79A), ICON_MOON),
    IconArt(Color(0xFFF6C98A), Color(0xFFF2A03D), Color(0xFFFBD98F), Color(0xFFD97B22), ICON_SUN),
    IconArt(Color(0xFF9FB8E8), Color(0xFFF7DC8E), Color(0xFFFFF3CB), Color(0xFFE0B44F), ICON_STAR),
    IconArt(Color(0xFFA9CBE8), Color(0xFFF9F5EC), Color(0xFFFFFFFF), Color(0xFFD8E4F0), ICON_CLOUD),
    IconArt(Color(0xFFAFCBE6), Color(0xFFE4657E), Color(0xFFF3C64E), Color(0xFF7AA05A), ICON_RAINBOW),
    IconArt(Color(0xFF8FA6C9), Color(0xFF6E7B8C), Color(0xFFF7F7F4), Color(0xFF4C5563), ICON_MOUNTAIN),
    IconArt(Color(0xFFD9B08C), Color(0xFFEFE6DA), Color(0xFFFFFDF8), Color(0xFF8E5A3C), ICON_CUP),
    IconArt(Color(0xFFF3B8C6), Color(0xFFE4657E), Color(0xFFF7A0B0), Color(0xFFB84258), ICON_HEART)
)

// ── the silhouettes ───────────────────────────────────────────────────────
// Named, because the table above is read as a cast list: "the beanie has the
// pompom" says something a bare 3 does not.
private const val KIND_CAP = 0
private const val KIND_BEARD = 1
private const val KIND_CURLS = 2
private const val KIND_BEANIE = 3
private const val KIND_HEADPHONES = 4
private const val KIND_WIZARD = 5
private const val KIND_HOOD = 6
private const val KIND_TOPKNOT = 7
private const val KIND_LAUREL = 8
private const val KIND_SIDE_PART = 9
private const val KIND_BOB = 10
private const val KIND_WAVES = 11
private const val KIND_HIGH_BUN = 12
private const val KIND_PIGTAILS = 13
private const val KIND_BRAIDS = 14
private const val KIND_HIME = 15
private const val KIND_FLOWERS = 16
private const val KIND_BERET = 17
private const val KIND_HEADBAND = 18
private const val KIND_SPACE_BUNS = 19

// ── the five gazes ────────────────────────────────────────────────────────
private const val FACE_OPEN = 0     // two round eyes, the default
private const val FACE_HAPPY = 1    // both eyes closed into a smile
private const val FACE_SLEEPY = 2   // half lids, unbothered
private const val FACE_WINK = 3     // one eye round, one closed
private const val FACE_CALM = 4     // two level lash lines

// ── the four mouths ───────────────────────────────────────────────────────
private const val MOUTH_SMILE = 0   // one soft arc
private const val MOUTH_GRIN = 1    // a wider arc with a lip line
private const val MOUTH_SOFT = 2    // a short, quiet arc
private const val MOUTH_OPEN = 3    // a small open o

// ── the icons ─────────────────────────────────────────────────────────────
private const val ICON_MOON = 0
private const val ICON_SUN = 1
private const val ICON_STAR = 2
private const val ICON_CLOUD = 3
private const val ICON_RAINBOW = 4
private const val ICON_MOUNTAIN = 5
private const val ICON_CUP = 6
private const val ICON_HEART = 7

/**
 * The one ink everything is outlined, shaded and dotted in, so twenty-eight
 * differently-coloured rows still share one drawing language.
 */
private val INK = Color(0xFF2A2320)

/** A whisper of the skin's own red, for cheeks and lips. */
private val BLUSH = Color(0xFFDE6E52)
private val LIP = Color(0xFFC4625C)

// ── the grid ──────────────────────────────────────────────────────────────
// Everything is measured in grid units on a 100×100 canvas: `u` is one unit, so
// any avatar size renders the same drawing. The disc is a circle of radius 50
// centred at (50, 50), and the head sits above the bust's shoulder line.
private const val HEAD_CX = 50f
private const val HEAD_CY = 38f
private const val HEAD_RX = 19f
private const val HEAD_RY = 21f
private const val EYE_Y = 39.5f
private const val EYE_DX = 8f
private const val MOUTH_Y = 49.5f
private const val NECK_TOP = 50f
private const val NECK_BOTTOM = 68f

// ── small drawing vocabulary ──────────────────────────────────────────────
// One place for every primitive, so the ink contour, the round cap and the grid
// cannot drift between twenty-eight rows.

private fun o(x: Float, y: Float, u: Float) = Offset(x * u, y * u)

private fun s(v: Float, u: Float) = v * u

private fun darken(color: Color, amount: Float) = lerp(color, INK, amount)

private fun lighten(color: Color, amount: Float) = lerp(color, Color.White, amount)

/** A filled disc. */
private fun DrawScope.dot(cx: Float, cy: Float, r: Float, color: Color, u: Float) =
    drawCircle(color, r * u, o(cx, cy, u))

/** A filled ellipse — the workhorse for eyes, buns, blushes and icons. */
private fun DrawScope.oval(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    color: Color,
    u: Float,
    alpha: Float = 1f
) = drawOval(
    color = color,
    topLeft = Offset((cx - rx) * u, (cy - ry) * u),
    size = Size(2f * rx * u, 2f * ry * u),
    alpha = alpha
)

/** A round-capped stroke: every line, lash, braid, strand, ray, steam and stem. */
private fun DrawScope.bar(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    w: Float,
    color: Color,
    u: Float
) = drawLine(
    color = color,
    start = o(x0, y0, u),
    end = o(x1, y1, u),
    strokeWidth = w * u,
    cap = StrokeCap.Round
)

/** A stroked circle. */
private fun DrawScope.ring(cx: Float, cy: Float, r: Float, w: Float, color: Color, u: Float) =
    drawCircle(color, r * u, o(cx, cy, u), style = Stroke(width = w * u))

/** A stroked arc. `start`/`sweep` are degrees, 0 = east, positive = clockwise. */
private fun DrawScope.arc(
    cx: Float,
    cy: Float,
    r: Float,
    start: Float,
    sweep: Float,
    w: Float,
    color: Color,
    u: Float
) = drawArc(
    color = color,
    startAngle = start,
    sweepAngle = sweep,
    useCenter = false,
    topLeft = Offset((cx - r) * u, (cy - r) * u),
    size = Size(2f * r * u, 2f * r * u),
    style = Stroke(width = w * u, cap = StrokeCap.Round)
)

/** A rounded bar — a cuff, a tie, a mug's body, a hat's band. */
private fun DrawScope.slab(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    r: Float,
    color: Color,
    u: Float
) = drawRoundRect(
    color = color,
    topLeft = o(x0, y0, u),
    size = Size((x1 - x0) * u, (y1 - y0) * u),
    cornerRadius = CornerRadius(r * u, r * u)
)

/**
 * A filled mass with its own ink contour.
 *
 * The contour is the point: a filled shape reads as a coloured blob, and the
 * same shape with a 1.5-unit ink line reads as a DRAWING. It is the one trick
 * that makes twenty minimal silhouettes look like one set.
 */
private fun DrawScope.fill(path: Path, color: Color, u: Float, ink: Float = 0.5f) {
    drawPath(path, color)
    if (ink > 0f) {
        drawPath(
            path = path,
            color = INK.copy(alpha = ink),
            style = Stroke(width = 1.5f * u, join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
    }
}

/** A closed path built from grid points, for the one-off silhouettes. */
private fun pathOf(vararg points: Float, closed: Boolean = true): Path = Path().apply {
    moveTo(points[0], points[1])
    var i = 2
    while (i + 1 < points.size) {
        lineTo(points[i], points[i + 1])
        i += 2
    }
    if (closed) close()
}

/**
 * A grid path, scaled onto the canvas.
 *
 * Every mass above is written at unit scale — `moveTo(50f, 60f)` reads as "the
 * top of the chest" — and scaled ONCE, here, so the point lists stay readable
 * as the shape they describe instead of arithmetic.
 */
private fun Path.scaled(u: Float): Path = apply {
    transform(Matrix().apply { scale(x = u, y = u) })
}

// ── the public shape ──────────────────────────────────────────────────────

/**
 * The code-drawn social avatar for [style] (0–27).
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
    Box(
        modifier = Modifier
            .size(avatarSize)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(avatarSize)) { drawSocialAvatar(style, ring) }

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
 * THE DRAWING ITSELF, as a plain [`DrawScope`] function.
 *
 * It is separated from the composable for ONE reason: the wallpaper of a
 * notification needs the avatar as an `android.graphics.Bitmap`, and a
 * notification is posted from a receiver that has no composition to draw in.
 * Drawing the character a second time (a notification-only lookalike) would
 * drift from the avatars the app shows — so both call THIS: the composable draws
 * it on a Compose canvas, and `socialAvatarBitmap` (`SocialNotifications.kt`)
 * draws it into an off-screen `ImageBitmap`.
 */
internal fun DrawScope.drawSocialAvatar(style: Int, ring: Boolean = true) {
    val index = style.coerceIn(0, PORTRAITS.size + ICONS.size - 1)
    val u = size.minDimension / 100f
    val radius = size.minDimension / 2f
    val ground = if (index < PORTRAITS.size) PORTRAITS[index].ground
    else ICONS[index - PORTRAITS.size].ground

    // ── the disc ──────────────────────────────────────────────────────────
    // A soft light from the top-left instead of one flat fill, with a deepened
    // lower edge: the avatar sits in its own light rather than reading as a
    // sticker cut out of the page.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(lighten(ground, 0.22f), ground, darken(ground, 0.16f)),
            center = Offset(size.width * 0.34f, size.height * 0.30f),
            radius = radius * 1.5f
        ),
        radius = radius
    )

    // Everything is clipped to the disc: the canvas is a square, so a bust drawn
    // to the bottom of the grid would otherwise show its corners.
    clipPath(
        Path().apply { addOval(Rect(Offset.Zero, Size(size.width, size.height))) }
    ) {
        if (index < PORTRAITS.size) {
            drawPortrait(PORTRAITS[index], u)
        } else {
            drawIcon(ICONS[index - PORTRAITS.size], u)
        }
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

/**
 * The live-presence dot. One colour on purpose: it means "a stamp younger than
 * five minutes", and a second shade for "recently" would turn a yes/no into
 * something the user has to interpret.
 */
private val ActiveDot = Color(0xFF2FBF71)

/**
 * The finishing light every avatar gets: a wide, soft highlight from the
 * top-left corner, tinting only the TOPS of the shapes it crosses. It is what
 * makes a flat set of minimal shapes read as one lit room instead of a collage.
 */
private fun DrawScope.drawAvatarLight(u: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width * 0.30f, size.height * 0.24f),
            radius = size.minDimension * 0.62f
        ),
        radius = size.minDimension * 0.62f,
        center = Offset(size.width * 0.30f, size.height * 0.24f)
    )
    // A last, very soft inner rim just inside the edge: the disc's own edge
    // catches the light, which is the difference between a button and a hole.
    drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        radius = size.minDimension * 0.46f,
        center = Offset(size.width * 0.5f, size.height * 0.5f),
        style = Stroke(width = s(3f, u))
    )
}

// ────────────────────────────────────────────────────────────────────────────
// A portrait
// ────────────────────────────────────────────────────────────────────────────

/**
 * One character, assembled in the order a drawing is made: the bust, then the
 * head's own group (back hair, neck, head, face, front hair, prop).
 *
 * The HEAD GROUP rotates ([AvatarArt.tilt]) about [NECK_BOTTOM] — the base of
 * the neck — so a tilted head keeps its chin over its own collar, and the bust
 * itself does not move with it. The whole character translates by
 * [AvatarArt.lean] so twenty rows do not all stand on the same centre line.
 */
private fun DrawScope.drawPortrait(art: AvatarArt, u: Float) {
    translate(left = art.lean * u, top = 0f) {
        drawBust(art, u)
        rotate(degrees = art.tilt, pivot = o(HEAD_CX, NECK_BOTTOM, u)) {
            // The back hair is drawn AFTER the bust on purpose: a mass that
            // falls past the shoulder line has to lie on top of it, which is
            // what long waves, braids and pigtails do.
            drawHairBack(art, u)
            drawNeck(art, u)
            drawHead(art, u)
            drawFace(art, u)
            drawHairFront(art, u)
            drawProp(art, u)
        }
    }
}

/**
 * THE BUST — one shoulder path for the whole cast.
 *
 * It rises from the disc's left edge to the top of the chest and falls away
 * again, with a collar, a neckline shade and one fold line. Drawn once, so a
 * change of shoulder shape is one edit for twenty-eight rows.
 */
private fun DrawScope.drawBust(art: AvatarArt, u: Float) {
    val garment = art.garment
    // v398 — NO INK CONTOUR ON THE SHOULDERS, AND THE COLLAR SITS ON THEM. The
    // stroked outline of this path was a dark rule running across the disc a few
    // units ABOVE the shoulders, and the collar band floated above the line it
    // was supposed to be part of — which is exactly what the member saw ("theres
    // a line above shoulder"). The garment's own tone against the disc is edge
    // enough at avatar size, and the collar now sits where a collar sits: from
    // the shoulder line down, with the neck drawn over its middle.
    val shoulders = Path().apply {
        moveTo(-4f, 104f)
        quadraticTo(2f, 78f, 24f, 71f)
        quadraticTo(39f, 66f, 50f, 66f)
        quadraticTo(61f, 66f, 76f, 71f)
        quadraticTo(98f, 78f, 104f, 104f)
        close()
    }
    fill(shoulders.scaled(u), garment, u, ink = 0f)
    // v403 — THE COLLAR BAND WAS THE HAIRLINE ABOVE THE SHOULDER.
    //
    // The band started one unit ABOVE the shoulder edge (y 65 against a shoulder
    // top of 66) and was drawn in a LIGHTER tone than the garment, so a pale
    // strip crossed the chest with its own top edge sitting just over the
    // shoulder line — which is what the member kept seeing on every avatar ("i
    // still see that line hair above shoulder in each avatar"). It now starts
    // BELOW the shoulder line and is DARKENED like every other seam: a neckline
    // is a fold in the garment, not a bright band laid on top of it, and nothing
    // in the bust has an edge above the shoulder any more.
    slab(40f, 70f, 60f, 77f, 6f, darken(garment, 0.12f), u)
    // The neckline's own shadow and one fold from the shoulder — both well below
    // the collar so neither can read as a rule.
    arc(44f, 78f, 8f, 200f, 140f, 1.2f, darken(garment, 0.20f), u)
    arc(30f, 90f, 13f, 205f, 55f, 1.2f, darken(garment, 0.16f), u)
}

/** The neck, with the jaw's own shadow across its top. */
private fun DrawScope.drawNeck(art: AvatarArt, u: Float) {
    slab(43f, NECK_TOP, 57f, NECK_BOTTOM, 6f, darken(art.skin, 0.10f), u)
    arc(50f, NECK_TOP + 1f, 7f, 200f, 140f, 2.6f, darken(art.skin, 0.26f), u)
}

/**
 * THE HEAD — one oval, two ears, a jaw shade and a forehead light.
 *
 * Minimal by design: the features do the identifying, so the skull only has to
 * be the right size and sit in the right light. Ears are drawn BEFORE the head
 * so only their outer half shows, and the face's paint is left to [drawFace].
 */
private fun DrawScope.drawHead(art: AvatarArt, u: Float) {
    oval(HEAD_CX - HEAD_RX, HEAD_CY + 2f, 3.2f, 4.6f, art.skin, u)
    oval(HEAD_CX + HEAD_RX, HEAD_CY + 2f, 3.2f, 4.6f, art.skin, u)
    drawOval(
        color = art.skin,
        topLeft = Offset((HEAD_CX - HEAD_RX) * u, (HEAD_CY - HEAD_RY) * u),
        size = Size(2f * HEAD_RX * u, 2f * HEAD_RY * u)
    )
    drawOval(
        color = INK.copy(alpha = 0.4f),
        topLeft = Offset((HEAD_CX - HEAD_RX) * u, (HEAD_CY - HEAD_RY) * u),
        size = Size(2f * HEAD_RX * u, 2f * HEAD_RY * u),
        style = Stroke(width = 1.4f * u)
    )
    // The cheek's warmth, low and wide — the one piece of colour that makes a
    // minimal face feel alive.
    oval(HEAD_CX - EYE_DX - 5f, HEAD_CY + 7f, 4.6f, 3f, BLUSH, u, alpha = 0.20f)
    oval(HEAD_CX + EYE_DX + 5f, HEAD_CY + 7f, 4.6f, 3f, BLUSH, u, alpha = 0.20f)
}

/**
 * THE FACE — two eyes, two brows, a nose tick and a mouth.
 *
 * The whole expressive range of the cast lives here, in five gazes and four
 * mouths: minimal art is read at 40 dp, where a face is two shapes and a line,
 * and every extra lash is one more thing that turns to noise.
 */
private fun DrawScope.drawFace(art: AvatarArt, u: Float) {
    val ink = INK.copy(alpha = 0.88f)
    val leftX = HEAD_CX - EYE_DX
    val rightX = HEAD_CX + EYE_DX
    // v398 — EVERY GAZE IS A SHAPE THAT MEANS SOMETHING. The first pass mixed
    // bars, flat ovals and half-lids, and a straight bar across the eye reads as
    // a rule drawn THROUGH the face rather than a closed eye — which is what the
    // member noticed ("some avatars are weird looking with the eyes"). The five
    // gazes are now five readable pairs, and the open eye carries a catch-light:
    // one highlight is the difference between a living eye and a dead dot.
    val openEye: (Float) -> Unit = { cx ->
        oval(cx, EYE_Y, 3f, 3.6f, ink, u)
        dot(cx - 1.1f, EYE_Y - 1.3f, 1.1f, PAPER, u)
    }
    val closedEye: (Float) -> Unit = { cx ->
        arc(cx, EYE_Y + 1.4f, 3.4f, 200f, 140f, 2.1f, ink, u)
    }
    when (art.face) {
        FACE_HAPPY -> {
            closedEye(leftX)
            closedEye(rightX)
        }
        FACE_SLEEPY -> {
            // A lidded eye: the lid sits ON the eye with a pupil under it, so
            // this reads as unbothered rather than as a pair of sunglasses (two
            // flat ovals with a bar floating above them).
            oval(leftX, EYE_Y + 0.6f, 3.2f, 2.4f, ink, u)
            oval(rightX, EYE_Y + 0.6f, 3.2f, 2.4f, ink, u)
            bar(leftX - 3.8f, EYE_Y - 1.9f, leftX + 3.8f, EYE_Y - 1.4f, 2f, ink, u)
            bar(rightX - 3.8f, EYE_Y - 1.4f, rightX + 3.8f, EYE_Y - 1.9f, 2f, ink, u)
        }
        FACE_WINK -> {
            openEye(leftX)
            closedEye(rightX)
        }
        FACE_CALM -> {
            // Looking down: the LOWER half of a circle is a whole expression —
            // two level bars are a face with something wrong with it.
            arc(leftX, EYE_Y - 0.6f, 3.2f, 20f, 140f, 2f, ink, u)
            arc(rightX, EYE_Y - 0.6f, 3.2f, 20f, 140f, 2f, ink, u)
        }
        else -> {
            openEye(leftX)
            openEye(rightX)
        }
    }
    // Brows sit close above the gaze and lean with it, which is where the
    // difference between a kind face and a stern one actually comes from.
    val browLift = if (art.face == FACE_HAPPY) 2.5f else 1.5f
    bar(leftX - 5f, EYE_Y - 8f - browLift, leftX + 4.5f, EYE_Y - 8.5f, 1.8f, INK.copy(alpha = 0.5f), u)
    bar(rightX - 4.5f, EYE_Y - 8.5f, rightX + 5f, EYE_Y - 8f - browLift, 1.8f, INK.copy(alpha = 0.5f), u)
    // The nose: one short tick. A real nose at this size is a smudge.
    bar(HEAD_CX, EYE_Y + 2f, HEAD_CX - 1.2f, EYE_Y + 5f, 1.4f, darken(art.skin, 0.20f), u)
    when (art.mouth) {
        MOUTH_GRIN -> {
            // A wide grin WITH its own lip line and two corners, so it is not the
            // soft smile drawn bigger — every mouth used to be the same arc
            // (user report: "that smile in each of them").
            arc(HEAD_CX, MOUTH_Y - 3.2f, 6.2f, 25f, 130f, 2.1f, LIP, u)
            bar(HEAD_CX - 5.4f, MOUTH_Y - 0.2f, HEAD_CX + 5.4f, MOUTH_Y - 0.2f, 1.3f, LIP.copy(alpha = 0.55f), u)
            dot(HEAD_CX - 6.8f, MOUTH_Y + 0.8f, 1f, LIP.copy(alpha = 0.65f), u)
            dot(HEAD_CX + 6.8f, MOUTH_Y + 0.8f, 1f, LIP.copy(alpha = 0.65f), u)
        }
        MOUTH_SOFT -> arc(HEAD_CX, MOUTH_Y - 1.8f, 3f, 40f, 100f, 1.7f, LIP, u)
        MOUTH_OPEN -> {
            oval(HEAD_CX, MOUTH_Y, 2.8f, 3.4f, LIP, u)
            oval(HEAD_CX - 0.8f, MOUTH_Y - 1.3f, 0.9f, 0.7f, PAPER, u, alpha = 0.7f)
        }
        else -> arc(HEAD_CX, MOUTH_Y - 2.6f, 4.6f, 25f, 130f, 1.9f, LIP, u)
    }
}

// ── the silhouettes ───────────────────────────────────────────────────────
//
// Two phases per character: the mass BEHIND the head ([drawHairBack]) and the
// mass in FRONT of it ([drawHairFront] — fringes, beards, the locks that fall
// over a shoulder), plus the ONE prop it is known by ([drawProp]). Every shape
// is a filled path or a round bar, so the cast reads as one drawing language.

/** The skull's own short-hair cap: the base every short kind wears. */
private fun DrawScope.hairCap(art: AvatarArt, u: Float, drop: Float = 0f) {
    val path = Path().apply {
        moveTo(HEAD_CX - 21f, HEAD_CY + 6f + drop)
        quadraticTo(HEAD_CX - 22f, HEAD_CY - 24f, HEAD_CX, HEAD_CY - 24f)
        quadraticTo(HEAD_CX + 22f, HEAD_CY - 24f, HEAD_CX + 21f, HEAD_CY + 6f + drop)
        quadraticTo(HEAD_CX + 14f, HEAD_CY - 4f, HEAD_CX, HEAD_CY - 5f)
        quadraticTo(HEAD_CX - 14f, HEAD_CY - 4f, HEAD_CX - 21f, HEAD_CY + 6f + drop)
        close()
    }
    fill(path.scaled(u), art.hair, u, ink = 0.45f)
}

/**
 * A short fringe that STOPS ABOVE THE BROWS, for the rows wearing a hat or a
 * crown.
 *
 * The front mass of a hat row is not the skull's cap: a cap reaches to grid 42,
 * which is exactly where the eyes are (a gaze sits at 39.5 and brows at 30), so
 * a front cap on those rows buried the whole face under its own hair. This is
 * as much hair as fits between the hat's band and the brow line.
 */
private fun DrawScope.hairFringe(art: AvatarArt, u: Float) {
    val fringe = Path().apply {
        moveTo(HEAD_CX - 20f, HEAD_CY - 14f)
        quadraticTo(HEAD_CX - 19f, HEAD_CY - 25f, HEAD_CX + 2f, HEAD_CY - 25f)
        quadraticTo(HEAD_CX + 18f, HEAD_CY - 25f, HEAD_CX + 20f, HEAD_CY - 16f)
        quadraticTo(HEAD_CX + 6f, HEAD_CY - 23f, HEAD_CX - 6f, HEAD_CY - 19f)
        quadraticTo(HEAD_CX - 14f, HEAD_CY - 21f, HEAD_CX - 20f, HEAD_CY - 14f)
        close()
    }
    fill(fringe.scaled(u), art.hair, u, ink = 0.45f)
}

private fun DrawScope.drawHairBack(art: AvatarArt, u: Float) {
    when (art.kind) {
        // A cap, curls, a top knot, a laurel, a side part, a bob: a short cap
        // behind the head is all these need, because their silhouette is at the
        // front.
        KIND_CAP, KIND_CURLS, KIND_TOPKNOT, KIND_LAUREL, KIND_SIDE_PART -> hairCap(art, u)
        KIND_BEARD -> hairCap(art, u)
        KIND_BEANIE, KIND_BERET, KIND_SPACE_BUNS -> hairCap(art, u, drop = -1f)
        KIND_WIZARD, KIND_HOOD -> {
            // Both wear something over the crown, so the hair only shows as two
            // tufts beside the jaw.
            oval(HEAD_CX - 20f, HEAD_CY + 8f, 6f, 12f, art.hair, u)
            oval(HEAD_CX + 20f, HEAD_CY + 8f, 6f, 12f, art.hair, u)
        }
        KIND_HEADPHONES -> {
            // A wavy mass to the shoulders: the headphones sit ON this, so the
            // hair has to be more than a cap.
            val path = Path().apply {
                moveTo(HEAD_CX - 24f, HEAD_CY - 4f)
                quadraticTo(HEAD_CX - 30f, HEAD_CY + 22f, HEAD_CX - 21f, HEAD_CY + 34f)
                quadraticTo(HEAD_CX, HEAD_CY + 26f, HEAD_CX + 21f, HEAD_CY + 34f)
                quadraticTo(HEAD_CX + 30f, HEAD_CY + 22f, HEAD_CX + 24f, HEAD_CY - 4f)
                close()
            }
            fill(path.scaled(u), art.hair, u, ink = 0.45f)
        }
        KIND_BOB -> {
            // Chin length, tips turned in: the bob's whole identity.
            val path = Path().apply {
                moveTo(HEAD_CX - 23f, HEAD_CY - 6f)
                quadraticTo(HEAD_CX - 27f, HEAD_CY + 16f, HEAD_CX - 19f, HEAD_CY + 24f)
                quadraticTo(HEAD_CX - 16f, HEAD_CY + 18f, HEAD_CX - 16f, HEAD_CY + 10f)
                quadraticTo(HEAD_CX, HEAD_CY + 16f, HEAD_CX + 16f, HEAD_CY + 10f)
                quadraticTo(HEAD_CX + 16f, HEAD_CY + 18f, HEAD_CX + 19f, HEAD_CY + 24f)
                quadraticTo(HEAD_CX + 27f, HEAD_CY + 16f, HEAD_CX + 23f, HEAD_CY - 6f)
                close()
            }
            fill(path.scaled(u), art.hair, u, ink = 0.45f)
        }
        KIND_HIGH_BUN -> {
            hairCap(art, u)
            // The bun sits ON the crown, low enough that the disc does not cut
            // it: the top of the art is the disc's own edge, not a crop.
            dot(HEAD_CX, HEAD_CY - 26f, 8f, art.hair, u)
            ring(HEAD_CX, HEAD_CY - 26f, 8f, 1.4f, INK.copy(alpha = 0.45f), u)
        }
        KIND_PIGTAILS -> {
            hairCap(art, u)
            // Two low pigtails, drawn as thick round bars so the tail has a
            // taper the eye reads without a taper being drawn.
            bar(HEAD_CX - 18f, HEAD_CY + 12f, HEAD_CX - 26f, HEAD_CY + 40f, 11f, art.hair, u)
            bar(HEAD_CX + 18f, HEAD_CY + 12f, HEAD_CX + 26f, HEAD_CY + 40f, 11f, art.hair, u)
            dot(HEAD_CX - 26f, HEAD_CY + 40f, 5.2f, art.hair, u)
            dot(HEAD_CX + 26f, HEAD_CY + 40f, 5.2f, art.hair, u)
        }
        KIND_BRAIDS -> {
            hairCap(art, u)
            // A braid is a chain of round links — the cleanest way to say
            // "plait" with one primitive.
            for (i in 0 until 5) {
                val t = i / 4f
                dot(
                    HEAD_CX - 17f - t * 7f,
                    HEAD_CY + 12f + t * 30f,
                    5.4f - t * 1.2f,
                    if (i % 2 == 0) art.hair else darken(art.hair, 0.16f),
                    u
                )
                dot(
                    HEAD_CX + 17f + t * 7f,
                    HEAD_CY + 12f + t * 30f,
                    5.4f - t * 1.2f,
                    if (i % 2 == 0) art.hair else darken(art.hair, 0.16f),
                    u
                )
            }
        }
        KIND_HIME -> {
            hairCap(art, u)
            // One straight sheet to the chest with a blunt hem: the hime cut.
            val path = Path().apply {
                moveTo(HEAD_CX - 23f, HEAD_CY - 6f)
                quadraticTo(HEAD_CX - 29f, HEAD_CY + 22f, HEAD_CX - 26f, HEAD_CY + 48f)
                lineTo(HEAD_CX + 26f, HEAD_CY + 48f)
                quadraticTo(HEAD_CX + 29f, HEAD_CY + 22f, HEAD_CX + 23f, HEAD_CY - 6f)
                close()
            }
            fill(path.scaled(u), art.hair, u, ink = 0.45f)
        }
        KIND_WAVES -> {
            hairCap(art, u)
            // Two long falls with a wave cut into each outer edge.
            val left = Path().apply {
                moveTo(HEAD_CX - 22f, HEAD_CY - 8f)
                quadraticTo(HEAD_CX - 34f, HEAD_CY + 20f, HEAD_CX - 26f, HEAD_CY + 52f)
                quadraticTo(HEAD_CX - 30f, HEAD_CY + 40f, HEAD_CX - 20f, HEAD_CY + 44f)
                quadraticTo(HEAD_CX - 24f, HEAD_CY + 24f, HEAD_CX - 16f, HEAD_CY + 6f)
                close()
            }
            val right = Path().apply {
                moveTo(HEAD_CX + 22f, HEAD_CY - 8f)
                quadraticTo(HEAD_CX + 34f, HEAD_CY + 20f, HEAD_CX + 26f, HEAD_CY + 52f)
                quadraticTo(HEAD_CX + 30f, HEAD_CY + 40f, HEAD_CX + 20f, HEAD_CY + 44f)
                quadraticTo(HEAD_CX + 24f, HEAD_CY + 24f, HEAD_CX + 16f, HEAD_CY + 6f)
                close()
            }
            fill(left.scaled(u), art.hair, u, ink = 0.45f)
            fill(right.scaled(u), art.hair, u, ink = 0.45f)
        }
        KIND_FLOWERS, KIND_HEADBAND -> {
            hairCap(art, u)
            val path = Path().apply {
                moveTo(HEAD_CX - 22f, HEAD_CY - 4f)
                quadraticTo(HEAD_CX - 28f, HEAD_CY + 20f, HEAD_CX - 20f, HEAD_CY + 38f)
                quadraticTo(HEAD_CX - 8f, HEAD_CY + 30f, HEAD_CX - 6f, HEAD_CY + 18f)
                close()
            }
            val mirror = Path().apply {
                moveTo(HEAD_CX + 22f, HEAD_CY - 4f)
                quadraticTo(HEAD_CX + 28f, HEAD_CY + 20f, HEAD_CX + 20f, HEAD_CY + 38f)
                quadraticTo(HEAD_CX + 8f, HEAD_CY + 30f, HEAD_CX + 6f, HEAD_CY + 18f)
                close()
            }
            fill(path.scaled(u), art.hair, u, ink = 0.45f)
            fill(mirror.scaled(u), art.hair, u, ink = 0.45f)
        }
        else -> hairCap(art, u)
    }
}

private fun DrawScope.drawHairFront(art: AvatarArt, u: Float) {
    when (art.kind) {
        KIND_BEARD -> {
            // A full beard that stops UNDER the mouth, so the smile stays the
            // face's anchor; plus a moustache either side of it.
            val beard = Path().apply {
                moveTo(HEAD_CX - 19f, HEAD_CY + 2f)
                quadraticTo(HEAD_CX - 20f, HEAD_CY + 22f, HEAD_CX, HEAD_CY + 27f)
                quadraticTo(HEAD_CX + 20f, HEAD_CY + 22f, HEAD_CX + 19f, HEAD_CY + 2f)
                // The inner edge sits BELOW the mouth's own dip (the smile's
                // lowest stroke lands at about grid 51): a beard that closes
                // over the smile eats the face's only expression.
                quadraticTo(HEAD_CX + 10f, HEAD_CY + 15f, HEAD_CX, HEAD_CY + 16f)
                quadraticTo(HEAD_CX - 10f, HEAD_CY + 15f, HEAD_CX - 19f, HEAD_CY + 2f)
                close()
            }
            fill(beard.scaled(u), art.hair, u, ink = 0.45f)
            oval(HEAD_CX - 6.2f, MOUTH_Y - 2.8f, 3.6f, 1.6f, art.hair, u)
            oval(HEAD_CX + 6.2f, MOUTH_Y - 2.8f, 3.6f, 1.6f, art.hair, u)
        }
        KIND_CURLS -> {
            // A fringe, not the skull's cap: the curls draw the crown, and a
            // front cap would cover the brows it sits above (see [hairFringe]).
            hairFringe(art, u)
            // Springy: nine small circles round the crown instead of one mass.
            for (i in 0 until 9) {
                val a = (196f + i * 18f) * PI.toFloat() / 180f
                dot(
                    HEAD_CX + cos(a) * 20f,
                    HEAD_CY - 7f + sin(a) * 19f,
                    5.2f,
                    if (i % 2 == 0) art.hair else lighten(art.hair, 0.10f),
                    u
                )
            }
        }
        KIND_CAP, KIND_BEANIE, KIND_WIZARD, KIND_BERET, KIND_SPACE_BUNS -> {
            // A short fringe under the hat's brim keeps the head from reading
            // bald under a hat — and stops above the brows, so it can never
            // cover the eyes (see [hairFringe]).
            hairFringe(art, u)
        }
        KIND_HOOD -> {
            // The hood's own rim: a crescent around the face opening, so the
            // character is looking out of something.
            arc(HEAD_CX, HEAD_CY + 1f, 24f, 150f, 240f, 8f, art.garment, u)
            arc(HEAD_CX, HEAD_CY + 1f, 24f, 150f, 240f, 1.6f, INK.copy(alpha = 0.4f), u)
            bar(HEAD_CX - 23f, HEAD_CY + 20f, HEAD_CX - 19f, HEAD_CY + 32f, 1.6f, lighten(art.garment, 0.22f), u)
            bar(HEAD_CX + 23f, HEAD_CY + 20f, HEAD_CX + 19f, HEAD_CY + 32f, 1.6f, lighten(art.garment, 0.22f), u)
        }
        KIND_TOPKNOT -> {
            // Slicked back: one smooth sweep with a side part line.
            val sweep = Path().apply {
                moveTo(HEAD_CX - 21f, HEAD_CY - 2f)
                quadraticTo(HEAD_CX - 18f, HEAD_CY - 22f, HEAD_CX + 2f, HEAD_CY - 22f)
                quadraticTo(HEAD_CX + 20f, HEAD_CY - 21f, HEAD_CX + 21f, HEAD_CY - 2f)
                quadraticTo(HEAD_CX + 6f, HEAD_CY - 12f, HEAD_CX - 21f, HEAD_CY - 2f)
                close()
            }
            fill(sweep.scaled(u), art.hair, u, ink = 0.45f)
            bar(HEAD_CX - 12f, HEAD_CY - 20f, HEAD_CX + 2f, HEAD_CY - 22f, 1.4f, darken(art.hair, 0.2f), u)
        }
        KIND_SIDE_PART -> {
            val sweep = Path().apply {
                moveTo(HEAD_CX - 21f, HEAD_CY - 1f)
                quadraticTo(HEAD_CX - 22f, HEAD_CY - 23f, HEAD_CX, HEAD_CY - 23f)
                quadraticTo(HEAD_CX + 20f, HEAD_CY - 23f, HEAD_CX + 21f, HEAD_CY - 4f)
                quadraticTo(HEAD_CX + 8f, HEAD_CY - 16f, HEAD_CX - 4f, HEAD_CY - 6f)
                quadraticTo(HEAD_CX - 12f, HEAD_CY - 12f, HEAD_CX - 21f, HEAD_CY - 1f)
                close()
            }
            fill(sweep.scaled(u), art.hair, u, ink = 0.45f)
            bar(HEAD_CX - 4f, HEAD_CY - 22f, HEAD_CX - 2f, HEAD_CY - 8f, 1.6f, darken(art.hair, 0.24f), u)
        }
        KIND_BOB, KIND_HIGH_BUN, KIND_PIGTAILS, KIND_BRAIDS, KIND_HEADPHONES -> {
            // A soft side-swept fringe: the mass is behind, so the front only
            // needs the brow line's own shade.
            val fringe = Path().apply {
                moveTo(HEAD_CX - 20f, HEAD_CY - 4f)
                quadraticTo(HEAD_CX - 19f, HEAD_CY - 22f, HEAD_CX + 2f, HEAD_CY - 22f)
                quadraticTo(HEAD_CX + 18f, HEAD_CY - 22f, HEAD_CX + 20f, HEAD_CY - 6f)
                quadraticTo(HEAD_CX + 4f, HEAD_CY - 20f, HEAD_CX - 8f, HEAD_CY - 8f)
                quadraticTo(HEAD_CX - 14f, HEAD_CY - 14f, HEAD_CX - 20f, HEAD_CY - 4f)
                close()
            }
            fill(fringe.scaled(u), art.hair, u, ink = 0.45f)
        }
        KIND_HIME -> {
            // A blunt fringe: a straight hem across the brow is the whole
            // reason a hime cut is recognisable.
            val fringe = pathOf(
                HEAD_CX - 20f, HEAD_CY - 6f,
                HEAD_CX - 22f, HEAD_CY - 22f,
                HEAD_CX + 22f, HEAD_CY - 22f,
                HEAD_CX + 20f, HEAD_CY - 6f,
                HEAD_CX + 6f, HEAD_CY - 4f,
                HEAD_CX + 2f, HEAD_CY - 16f,
                HEAD_CX - 6f, HEAD_CY - 5f
            )
            fill(fringe.scaled(u), art.hair, u, ink = 0.45f)
        }
        KIND_WAVES, KIND_FLOWERS -> {
            val fringe = Path().apply {
                moveTo(HEAD_CX - 21f, HEAD_CY - 3f)
                quadraticTo(HEAD_CX - 20f, HEAD_CY - 23f, HEAD_CX, HEAD_CY - 23f)
                quadraticTo(HEAD_CX + 20f, HEAD_CY - 23f, HEAD_CX + 21f, HEAD_CY - 3f)
                quadraticTo(HEAD_CX + 10f, HEAD_CY - 14f, HEAD_CX - 2f, HEAD_CY - 12f)
                quadraticTo(HEAD_CX - 12f, HEAD_CY - 10f, HEAD_CX - 21f, HEAD_CY - 3f)
                close()
            }
            fill(fringe.scaled(u), art.hair, u, ink = 0.45f)
        }
        else -> hairFringe(art, u)
    }
}

/** The ONE prop each character is known by. Drawn last, over everything. */
private fun DrawScope.drawProp(art: AvatarArt, u: Float) {
    when (art.kind) {
        KIND_CAP -> {
            // A flat cap: a soft crown with a brim, worn a little back.
            rotate(degrees = -4f, pivot = o(HEAD_CX, HEAD_CY, u)) {
                val crown = Path().apply {
                    moveTo(HEAD_CX - 21f, HEAD_CY - 12f)
                    quadraticTo(HEAD_CX - 18f, HEAD_CY - 30f, HEAD_CX, HEAD_CY - 30f)
                    quadraticTo(HEAD_CX + 18f, HEAD_CY - 30f, HEAD_CX + 21f, HEAD_CY - 12f)
                    close()
                }
                fill(crown.scaled(u), art.garment, u, ink = 0.45f)
                slab(HEAD_CX - 25f, HEAD_CY - 13f, HEAD_CX + 27f, HEAD_CY - 8f, 2.6f, darken(art.garment, 0.18f), u)
                dot(HEAD_CX, HEAD_CY - 30f, 1.6f, lighten(art.garment, 0.3f), u)
            }
        }
        KIND_CURLS -> {
            // Round glasses: two rings and a bridge, the row's own ink.
            ring(HEAD_CX - EYE_DX, EYE_Y, 7f, 1.6f, INK.copy(alpha = 0.55f), u)
            ring(HEAD_CX + EYE_DX, EYE_Y, 7f, 1.6f, INK.copy(alpha = 0.55f), u)
            bar(HEAD_CX - 1.6f, EYE_Y, HEAD_CX + 1.6f, EYE_Y, 1.6f, INK.copy(alpha = 0.55f), u)
        }
        KIND_BEANIE -> {
            // A knit beanie: dome, folded cuff with ribs, and a pompom.
            val dome = Path().apply {
                moveTo(HEAD_CX - 21f, HEAD_CY - 10f)
                quadraticTo(HEAD_CX - 22f, HEAD_CY - 30f, HEAD_CX, HEAD_CY - 30f)
                quadraticTo(HEAD_CX + 22f, HEAD_CY - 30f, HEAD_CX + 21f, HEAD_CY - 10f)
                close()
            }
            fill(dome.scaled(u), art.garment, u, ink = 0.45f)
            slab(HEAD_CX - 23f, HEAD_CY - 13f, HEAD_CX + 23f, HEAD_CY - 5f, 4f, lighten(art.garment, 0.12f), u)
            for (i in 0 until 5) {
                val x = HEAD_CX - 17f + i * 8.5f
                bar(x, HEAD_CY - 12f, x, HEAD_CY - 6f, 1.4f, darken(art.garment, 0.22f), u)
            }
            dot(HEAD_CX, HEAD_CY - 31f, 5f, lighten(art.garment, 0.32f), u)
        }
        KIND_HEADPHONES -> {
            // Band over the crown, a cup each side, and a mic boom — the boom is
            // what makes it music rather than earmuffs.
            arc(HEAD_CX, HEAD_CY, 23f, 200f, 140f, 5f, art.garment, u)
            slab(HEAD_CX - 28f, HEAD_CY - 4f, HEAD_CX - 19f, HEAD_CY + 12f, 4f, art.garment, u)
            slab(HEAD_CX + 19f, HEAD_CY - 4f, HEAD_CX + 28f, HEAD_CY + 12f, 4f, art.garment, u)
            slab(HEAD_CX - 26f, HEAD_CY + 1f, HEAD_CX - 21f, HEAD_CY + 7f, 2f, lighten(art.garment, 0.22f), u)
            slab(HEAD_CX + 21f, HEAD_CY + 1f, HEAD_CX + 26f, HEAD_CY + 7f, 2f, lighten(art.garment, 0.22f), u)
            val boom = Path().apply {
                moveTo(HEAD_CX - 26f, HEAD_CY + 10f)
                quadraticTo(HEAD_CX - 20f, HEAD_CY + 22f, HEAD_CX - 8f, HEAD_CY + 20f)
            }
            drawPath(boom.scaled(u), darken(art.garment, 0.15f), style = Stroke(1.8f * u, cap = StrokeCap.Round))
            dot(HEAD_CX - 7f, HEAD_CY + 20f, 2.2f, darken(art.garment, 0.25f), u)
        }
        KIND_WIZARD -> {
            // A bent cone with a band and a star, sat on the brow.
            // A cone that LEANS: a tall hat has to bend to fit the disc, and a
            // bent tip is what makes a wizard a wizard rather than a traffic
            // cone.
            val cone = Path().apply {
                moveTo(HEAD_CX - 20f, HEAD_CY - 12f)
                quadraticTo(HEAD_CX - 16f, HEAD_CY - 30f, HEAD_CX + 6f, HEAD_CY - 36f)
                quadraticTo(HEAD_CX + 2f, HEAD_CY - 26f, HEAD_CX + 20f, HEAD_CY - 12f)
                close()
            }
            fill(cone.scaled(u), art.garment, u, ink = 0.45f)
            slab(HEAD_CX - 23f, HEAD_CY - 14f, HEAD_CX + 23f, HEAD_CY - 8f, 2.4f, darken(art.garment, 0.24f), u)
            star(HEAD_CX + 7f, HEAD_CY - 30f, 4.4f, 1.9f, PAPER, u)
        }
        KIND_LAUREL -> {
            // Five leaves a side on one stem, in the row's own green.
            for (i in 0 until 5) {
                val t = i / 4f
                leaf(HEAD_CX - 14f - t * 8f, HEAD_CY - 20f + t * 13f, -38f - t * 6f, 5.2f, 2.4f, art.garment, u)
                leaf(HEAD_CX + 14f + t * 8f, HEAD_CY - 20f + t * 13f, 38f + t * 6f, 5.2f, 2.4f, art.garment, u)
            }
        }
        KIND_TOPKNOT -> {
            // The knot itself, drawn last so it sits over the slicked-back
            // sweep, with a tie band at its base.
            dot(HEAD_CX, HEAD_CY - 27f, 8.5f, art.hair, u)
            ring(HEAD_CX, HEAD_CY - 27f, 8.5f, 1.4f, INK.copy(alpha = 0.45f), u)
            slab(HEAD_CX - 7f, HEAD_CY - 22f, HEAD_CX + 7f, HEAD_CY - 16f, 2f, art.garment, u)
        }
        KIND_HIGH_BUN -> {
            // A hairpin through the bun: one thin gold line does the whole job.
            bar(HEAD_CX - 10f, HEAD_CY - 32f, HEAD_CX + 9f, HEAD_CY - 20f, 1.8f, Color(0xFFE0B44F), u)
            dot(HEAD_CX + 9f, HEAD_CY - 20f, 2f, Color(0xFFF2D68A), u)
        }
        KIND_PIGTAILS -> {
            slab(HEAD_CX - 22f, HEAD_CY + 10f, HEAD_CX - 14f, HEAD_CY + 16f, 2.4f, art.garment, u)
            slab(HEAD_CX + 14f, HEAD_CY + 10f, HEAD_CX + 22f, HEAD_CY + 16f, 2.4f, art.garment, u)
        }
        KIND_BRAIDS -> {
            slab(HEAD_CX - 29f, HEAD_CY + 42f, HEAD_CX - 20f, HEAD_CY + 48f, 2.4f, art.garment, u)
            slab(HEAD_CX + 20f, HEAD_CY + 42f, HEAD_CX + 29f, HEAD_CY + 48f, 2.4f, art.garment, u)
        }
        KIND_HIME -> {
            slab(HEAD_CX + 12f, HEAD_CY - 16f, HEAD_CX + 20f, HEAD_CY - 11f, 2.2f, Color(0xFFE0B44F), u)
        }
        KIND_FLOWERS -> {
            flower(HEAD_CX - 16f, HEAD_CY - 20f, 4.2f, lighten(BLUSH, 0.22f), Color(0xFFF6D68A), u)
            flower(HEAD_CX - 4f, HEAD_CY - 25f, 3.6f, lighten(Color(0xFFB98FD8), 0.24f), Color(0xFFF6D68A), u)
            flower(HEAD_CX + 11f, HEAD_CY - 21f, 3.2f, lighten(Color(0xFF7FB4C9), 0.22f), Color(0xFFF6D68A), u)
        }
        KIND_BERET -> {
            rotate(degrees = -12f, pivot = o(HEAD_CX, HEAD_CY, u)) {
                val cap = Path().apply {
                    moveTo(HEAD_CX - 23f, HEAD_CY - 12f)
                    quadraticTo(HEAD_CX - 24f, HEAD_CY - 30f, HEAD_CX + 6f, HEAD_CY - 30f)
                    quadraticTo(HEAD_CX + 27f, HEAD_CY - 30f, HEAD_CX + 25f, HEAD_CY - 12f)
                    close()
                }
                fill(cap.scaled(u), art.garment, u, ink = 0.45f)
                slab(HEAD_CX - 23f, HEAD_CY - 14f, HEAD_CX + 25f, HEAD_CY - 10f, 2.2f, darken(art.garment, 0.22f), u)
                bar(HEAD_CX + 2f, HEAD_CY - 30f, HEAD_CX + 5f, HEAD_CY - 34f, 2.2f, darken(art.garment, 0.22f), u)
            }
        }
        KIND_HEADBAND -> {
            arc(HEAD_CX, HEAD_CY - 2f, 22f, 195f, 150f, 3.6f, lighten(art.garment, 0.18f), u)
            dot(HEAD_CX - 19f, HEAD_CY + 10f, 2.4f, Color(0xFFE0B44F), u)
        }
        KIND_SPACE_BUNS -> {
            dot(HEAD_CX - 13f, HEAD_CY - 28f, 7f, art.hair, u)
            dot(HEAD_CX + 13f, HEAD_CY - 28f, 7f, art.hair, u)
            ring(HEAD_CX - 13f, HEAD_CY - 28f, 7f, 1.4f, INK.copy(alpha = 0.45f), u)
            ring(HEAD_CX + 13f, HEAD_CY - 28f, 7f, 1.4f, INK.copy(alpha = 0.45f), u)
            star(HEAD_CX + 19f, HEAD_CY - 12f, 3.6f, 1.5f, Color(0xFFF6D68A), u)
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// An icon
// ────────────────────────────────────────────────────────────────────────────
//
// The same disc, the same light and the same ink with ONE object on it. Cozy
// minimal is a discipline: each of the eight is a handful of shapes and not one
// more, so an icon is legible at 26 dp where a bubble's avatar lives.

private fun DrawScope.drawIcon(art: IconArt, u: Float) {
    when (art.kind) {
        ICON_MOON -> {
            // v398 — A CRESCENT, MADE BY TAKING ONE CIRCLE OUT OF ANOTHER. The
            // first pass drew it as a stroked arc sweeping 280°, which is a
            // broken ring: at avatar size its two ends nearly met and the whole
            // tile read as the letter C (user report: "why the mon is C"). Two
            // overlapping discs leave a solid crescent — an outline can never
            // read as anything but a line.
            val disc = Path().apply { addOval(Rect(21f, 22f, 75f, 76f)) }
            val bite = Path().apply { addOval(Rect(37f, 12f, 89f, 64f)) }
            val crescent = Path().apply { op(disc, bite, PathOperation.Difference) }
            fill(crescent.scaled(u), art.main, u, ink = 0f)
            // Two craters on the body it kept, and the night around it.
            dot(34f, 58f, 2.6f, art.deep, u)
            dot(31f, 44f, 1.7f, art.deep, u)
            star(72f, 30f, 4f, 1.6f, art.light, u)
            star(80f, 56f, 2.8f, 1.1f, art.light, u)
        }
        ICON_SUN -> {
            dot(50f, 50f, 24f, art.main.copy(alpha = 0.18f), u)
            for (i in 0 until 12) {
                val a = i * 30f * PI.toFloat() / 180f
                val x0 = 50f + cos(a) * 18f
                val y0 = 50f + sin(a) * 18f
                val x1 = 50f + cos(a) * 27f
                val y1 = 50f + sin(a) * 27f
                bar(x0, y0, x1, y1, 3.4f, art.main, u)
            }
            dot(50f, 50f, 15f, art.main, u)
            dot(45f, 45f, 6f, art.light, u)
        }
        ICON_STAR -> {
            star(50f, 52f, 27f, 11.5f, art.main, u)
            star(50f, 52f, 11f, 4.5f, art.light, u)
            star(76f, 24f, 4.4f, 1.8f, art.light, u)
        }
        ICON_CLOUD -> {
            dot(38f, 52f, 14f, art.main, u)
            dot(53f, 45f, 17f, art.main, u)
            dot(66f, 53f, 13f, art.main, u)
            slab(24f, 50f, 79f, 66f, 8f, art.main, u)
            slab(30f, 60f, 73f, 66f, 3f, art.deep, u)
            bar(44f, 70f, 42f, 76f, 2.4f, art.light, u)
            bar(52f, 70f, 50f, 78f, 2.4f, art.light, u)
            bar(60f, 70f, 58f, 75f, 2.4f, art.light, u)
        }
        ICON_RAINBOW -> {
            // v398 — the LEAF's tile (the member's own ask: "for the leaf icon its
            // bad change it … change the leaf to crescent moon", and the crescent
            // now stands in the moon's own slot), so this one carries three bands
            // of one arch instead: reads instantly, and never as a leaf.
            arc(50f, 70f, 30f, 180f, 180f, 8f, art.main, u)
            arc(50f, 70f, 22.5f, 180f, 180f, 7.5f, art.light, u)
            arc(50f, 70f, 15.5f, 180f, 180f, 6f, art.deep, u)
            dot(21f, 70f, 7.5f, PAPER, u)
            dot(79f, 70f, 7.5f, PAPER, u)
        }
        ICON_MOUNTAIN -> {
            dot(74f, 30f, 9f, art.light, u)
            val far = pathOf(40f, 74f, 62f, 34f, 86f, 74f)
            fill(far.scaled(u), art.deep, u, ink = 0f)
            val near = pathOf(12f, 74f, 38f, 30f, 66f, 74f)
            fill(near.scaled(u), art.main, u, ink = 0f)
            val snowFar = pathOf(56f, 46f, 62f, 34f, 69f, 47f)
            fill(snowFar.scaled(u), art.light, u, ink = 0f)
            val snowNear = pathOf(30f, 47f, 38f, 30f, 47f, 47f)
            fill(snowNear.scaled(u), art.light, u, ink = 0f)
            bar(12f, 74f, 88f, 74f, 3f, art.deep, u)
        }
        ICON_CUP -> {
            // A mug on a saucer, with two curls of steam.
            slab(30f, 40f, 64f, 72f, 6f, art.main, u)
            slab(33f, 43f, 61f, 50f, 3f, art.deep, u)
            arc(70f, 56f, 10f, 95f, 170f, 4.6f, art.main, u)
            val steamA = Path().apply {
                moveTo(41f, 32f)
                quadraticTo(46f, 26f, 41f, 20f)
            }
            val steamB = Path().apply {
                moveTo(53f, 33f)
                quadraticTo(58f, 27f, 53f, 21f)
            }
            drawPath(steamA.scaled(u), art.light, style = Stroke(2.2f * u, cap = StrokeCap.Round))
            drawPath(steamB.scaled(u), art.light, style = Stroke(2.2f * u, cap = StrokeCap.Round))
            slab(22f, 72f, 74f, 78f, 3f, art.deep, u)
        }
        else -> {
            // The heart: two lobes and a point, filled in one colour.
            dot(41f, 44f, 14.5f, art.main, u)
            dot(59f, 44f, 14.5f, art.main, u)
            fill(pathOf(27.5f, 47f, 72.5f, 47f, 50f, 76f).scaled(u), art.main, u, ink = 0f)
            oval(43f, 40f, 4.6f, 3.2f, art.light, u, alpha = 0.55f)
            bar(64f, 34f, 70f, 28f, 2.6f, art.light, u)
        }
    }
}

// ── the icon vocabulary ───────────────────────────────────────────────────

/** A four/five-point star, drawn from the circle it is inscribed in. */
private fun DrawScope.star(
    cx: Float,
    cy: Float,
    outer: Float,
    inner: Float,
    color: Color,
    u: Float,
    points: Int = 5
) {
    val path = Path()
    val step = PI.toFloat() / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI.toFloat() / 2f + i * step
        val x = cx + cos(a) * r
        val y = cy + sin(a) * r
        if (i == 0) path.moveTo(x * u, y * u) else path.lineTo(x * u, y * u)
    }
    path.close()
    drawPath(path, color)
}

/** A five-petal flower with a core — the cozy mark a crown and a garden share. */
private fun DrawScope.flower(
    cx: Float,
    cy: Float,
    petal: Float,
    color: Color,
    core: Color,
    u: Float
) {
    for (i in 0 until 5) {
        val a = i * 72f * PI.toFloat() / 180f
        dot(cx + cos(a) * petal * 0.8f, cy + sin(a) * petal * 0.8f, petal * 0.72f, color, u)
    }
    dot(cx, cy, petal * 0.52f, core, u)
}

/** A leaf, as one oval turned off the vertical with the stem's own tip. */
private fun DrawScope.leaf(
    cx: Float,
    cy: Float,
    degrees: Float,
    rx: Float,
    ry: Float,
    color: Color,
    u: Float
) = rotate(degrees = degrees, pivot = o(cx, cy, u)) {
    oval(cx, cy, rx, ry, color, u)
    ring(cx, cy, rx, 1.2f, INK.copy(alpha = 0.35f), u)
}

/** A white paper tone, for the marks that must read on any ground. */
private val PAPER = Color(0xFFFBF4E8)

// ────────────────────────────────────────────────────────────────────────────
// The picker
// ────────────────────────────────────────────────────────────────────────────

/**
 * The avatar picker's tile.
 *
 * The chosen avatar is marked by a ROUNDED SELECTION RING (a soft accent halo
 * plus a 2dp circular outline, with the avatar stepping up a little), not by a
 * tick badge laid over the art: the art is the point of the tile, and a check
 * covering its chin reads as an error mark on a picker where every option is
 * valid. The ring hugs the disc exactly like the app's other selected chips
 * (RoundedCornerShape(50) = a circle here), so the picker speaks the same
 * language as the rails and filter pills.
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
        // The click lives on the tile (above), so the avatar itself is not
        // also clickable — one tap target, one haptic, no double ripple.
        SocialAvatar(
            style = style,
            avatarSize = if (selected) 38.dp else 36.dp,
            onClick = null,
            ring = !selected
        )
    }
}

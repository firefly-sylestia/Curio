package com.curio.app.features.cabinet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.components.CurioDoodleEmptyState
import com.curio.app.ui.components.rememberCurioPressSource
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.isCurioDarkTheme

// ────────────────────────────────────────────────────────────────────────
// Built-in shelf model (v3xx — Cabinet folders)
// ────────────────────────────────────────────────────────────────────────

/** The seven always-present Cabinet shelves. */
enum class V2ShelfId {
    FAVORITES,
    CURRENTLY_READING,
    WANT_TO_READ,
    SAVED,
    COMPLETED,
    NOTES,
    PERSONAL
}

/** One tone family — pastel in light mode, deep muted in dark mode
 *  (mirrors the JSX's `.darkMode` card treatment). */
data class V2ShelfTone(val light: Long, val dark: Long) {
    @Composable
    fun fill(): Color = if (isCurioDarkTheme()) Color(dark) else Color(light)
}

/** One built-in shelf: JSX-style card identity (title, glyph, tone, art). */
data class V2Shelf(
    val id: V2ShelfId,
    val title: String,
    val icon: String,
    val tone: V2ShelfTone,
    val art: V2ShelfArtType,
    /** The persisted [com.curio.app.data.CurioCollection] id when this shelf
     *  is a seeded starter shelf (editable), null when it is virtual
     *  (computed from live data). */
    val seededCollectionId: String? = null
)

/** v3xx — the decorative CARD ART each shelf wears. Drawn responsively so
 *  the same scene scales from a foot strip to a full-card background.
 *  CONSTELLATION (Favorites' star-map), PEAK (the Completed redesign:
 *  sun-arc summit scene) and the four MINIMAL_* scenes borrow the Minimal
 *  share card's sparse line language for plenty of variety. */
enum class V2ShelfArtType {
    STAR, CONSTELLATION, READING, BOOKS, MOUNTAIN, PEAK, NOTES, WINDOW, PHOTOS,
    MINIMAL_SUN, MINIMAL_RINGS, MINIMAL_WAVE, MINIMAL_DOTS
}

/** The seven built-ins, in the user's requested order. The four seeded
 *  shelves carry a stable `shelf:` collection id; the three virtual ones
 *  (Favorites / Saved entries / Notes) compute their members live. */
val builtInShelves: List<V2Shelf> = listOf(
    V2Shelf(
        id = V2ShelfId.FAVORITES,
        title = "Favorites",
        icon = CurioIcons.Star,
        tone = V2ShelfTone(light = 0xFFD8D1EE, dark = 0xFF4A4164),
        // v3xx — the star-map scene reads better as a full-card background
        // than a single star; the classic glowing STAR stays in the variety
        // pool for user collections.
        art = V2ShelfArtType.CONSTELLATION
    ),
    V2Shelf(
        id = V2ShelfId.CURRENTLY_READING,
        // v3xx33 — the shelf is the Curio verb now: "Curiying now" holds
        // books, series AND albums (the old "Currently Reading" was
        // book-only, so series/albums felt out of place).
        title = "Curiying now",
        icon = CurioIcons.MenuBook,
        tone = V2ShelfTone(light = 0xFFD0DFC7, dark = 0xFF3C5140),
        art = V2ShelfArtType.READING,
        seededCollectionId = "shelf:currently-reading"
    ),
    V2Shelf(
        id = V2ShelfId.WANT_TO_READ,
        title = "Want to Read",
        icon = CurioIcons.Bookmark,
        tone = V2ShelfTone(light = 0xFFF1C6CA, dark = 0xFF693F4D),
        art = V2ShelfArtType.BOOKS,
        seededCollectionId = "shelf:want-to-read"
    ),
    V2Shelf(
        id = V2ShelfId.SAVED,
        title = "Saved entries",
        icon = CurioIcons.Inventory2,
        tone = V2ShelfTone(light = 0xFFC3DDEB, dark = 0xFF345363),
        art = V2ShelfArtType.PHOTOS
    ),
    V2Shelf(
        id = V2ShelfId.COMPLETED,
        title = "Completed",
        // v3xx — the old task_alt glyph read squished inside the 20dp frosted
        // tile (its wide check-circle ink barely filled the box); the plain
        // Check mark matches the JSX's "completed: icon: check" and renders
        // crisp at tile size next to the filled sibling glyphs.
        icon = CurioIcons.Check,
        tone = V2ShelfTone(light = 0xFFCFE4D5, dark = 0xFF385345),
        // v3xx — the FULL Completed redesign: a sun-arc summit scene with a
        // planted flag (the old plain mountain read as an afterthought).
        art = V2ShelfArtType.PEAK,
        seededCollectionId = "shelf:completed"
    ),
    V2Shelf(
        id = V2ShelfId.NOTES,
        title = "Notes",
        icon = CurioIcons.Note,
        tone = V2ShelfTone(light = 0xFFEFD7D4, dark = 0xFF5E3F45),
        art = V2ShelfArtType.NOTES
    ),
    V2Shelf(
        id = V2ShelfId.PERSONAL,
        title = "Personal",
        icon = CurioIcons.Person,
        tone = V2ShelfTone(light = 0xFFECD5B8, dark = 0xFF62502F),
        art = V2ShelfArtType.WINDOW,
        seededCollectionId = "shelf:personal"
    )
)

/** The seeded starter-shelf collection ids (editable, persisted). */
val seededShelfIds: Set<String> = setOf(
    "shelf:currently-reading",
    "shelf:want-to-read",
    "shelf:completed",
    "shelf:personal"
)

/** The three virtual shelves open into these levels. */
const val SHELF_LEVEL_FAVORITES = "shelf:favorites"
const val SHELF_LEVEL_SAVED = "shelf:saved"
const val SHELF_LEVEL_NOTES = "shelf:notes"

// ────────────────────────────────────────────────────────────────────────
// JSX-style shelf card
// ────────────────────────────────────────────────────────────────────────

/** The JSX "CollectionCard" — pastel tone fill, frosted icon tile, ⋮ when
 *  editable, title + item count, and the shelf's decorative art at the foot.
 *  Built-in shelves pass their [V2Shelf] fields; user collections cycle a
 *  tone + art palette so every shelf looks hand-picked. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun V2ShelfCard(
    title: String,
    icon: String,
    tone: V2ShelfTone,
    art: V2ShelfArtType,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** v3xx — the ⋮ opens an ANCHORED dropdown (Rename / Delete) right at
     *  the dots — no more centre-of-screen overlay. Tapping the card still
     *  opens the shelf; long-pressing the dots opens the same menu. */
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dark = isCurioDarkTheme()
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF553E42)
    val muted = ink.copy(alpha = 0.62f)
    var moreOpen by remember { mutableStateOf(false) }
    val hasMenu = onRename != null || onDelete != null
    // v3xx46 — the collection cards squish + tick on press. Material3's
    // clickable Surface owns the gesture, so the press source is handed to it
    // and the scale rides its modifier chain.
    val press = rememberCurioPressSource(pressedScale = 0.98f)
    Surface(
        modifier = modifier
            .then(press.modifier)
            .combinedClickable(
                interactionSource = press.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = if (hasMenu) ({ moreOpen = true }) else null
            ),
        shape = RoundedCornerShape(22.dp),
        color = tone.fill()
    ) {
        // v3xx — the JSX "CollectionCard" layout is back (the full-card
        // whisper-alpha art read as INVISIBLE on the cards — the user could
        // only see the designs in the create-collection sheet previews).
        // Tone fill + frosted icon tile + title/count, with the REDRAWN art
        // as a VISIBLE foot strip (the same responsive scenes, full alpha).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp, top = 13.dp, bottom = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (dark) Color.White.copy(alpha = 0.16f)
                            else Color.White.copy(alpha = 0.40f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = icon,
                        contentDescription = null,
                        tint = ink,
                        size = 20.dp
                    )
                }
                Spacer(Modifier.weight(1f))
                if (hasMenu) {
                    // v3xx — the ⋮ is an ANCHORED DropdownMenu (renamed /
                    // deleted from the dots themselves) instead of the old
                    // centre-screen CurioHoldPill overlay.
                    Box {
                        // v3xx — the app-wide kebab: a slightly bigger 40dp
                        // frosted circle (matches the icon tile's glass).
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (dark) Color.White.copy(alpha = 0.16f)
                                    else Color.White.copy(alpha = 0.36f)
                                )
                                .combinedClickable(
                                    onClick = { moreOpen = true },
                                    onLongClick = { moreOpen = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                name = CurioIcons.MoreVert,
                                contentDescription = "Rename or delete",
                                tint = ink.copy(alpha = 0.78f),
                                size = 20.dp
                            )
                        }
                        DropdownMenu(
                            expanded = moreOpen,
                            onDismissRequest = { moreOpen = false }
                        ) {
                            if (onRename != null) {
                                DropdownMenuItem(
                                    text = { Text("Rename", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        CurioIcon(name = CurioIcons.Edit, contentDescription = null, tint = ink, size = 17.dp)
                                    },
                                    onClick = { moreOpen = false; onRename() }
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete collection", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        CurioIcon(name = CurioIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, size = 17.dp)
                                    },
                                    onClick = { moreOpen = false; onDelete() }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 19.sp
                ),
                color = ink,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "$count item${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = muted,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            // v3xx — the REDRAWN scene, back as a VISIBLE foot strip (the
            // responsive scenes scale down to the strip exactly like the old
            // ones did — full alpha so the design actually shows on the card).
            V2ShelfArt(
                art = art,
                dark = dark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
            )
        }
    }
}
// ────────────────────────────────────────────────────────────────────────
// Shelf art — RESPONSIVE CARD SCENES: every scene is drawn with
// proportional Canvas geometry, so the SAME art scales from the card's
// foot strip to the create-collection sheet's preview tiles. v3xx-pass-3
// redrew the named scenes in the flask doodle family (pastel fills +
// white outlines) and REMOVED the shared ✦/✧ sparkle patterns — each
// scene now uses its own accents (dots, rays, steam, badges): Favorites
// is a big filled heart with a golden star + shooting-star arc, Curiying
// now a layered open book with a steaming mug, Completed a sun-ray
// summit with a check badge, Saved entries a fanned polaroid stack, Notes
// a spiral-bound notebook, plus a night-sky star, layered mountains and
// the four MINIMAL scenes. Want to Read (spines) and Personal (window)
// keep their shapes.
// ────────────────────────────────────────────────────────────────────────

/**
 * v3xx40 — the shelf scenes REBALANCED: the hero of every card is now
 * bigger and CENTRED (the old scenes crowded their subject off to one side
 * with far-off filler dots), with a soft grounding shadow tying the art to
 * the card's foot. Each subject is drawn with more detail — real page
 * curves, photo inner shadows, layered ridges — in the same pastel-fill +
 * white-outline doodle family. No sparkle grids anywhere.
 */

@Composable
fun V2ShelfArt(
    art: V2ShelfArtType,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (art) {
            V2ShelfArtType.STAR -> StarArt(dark)
            V2ShelfArtType.CONSTELLATION -> ConstellationArt(dark)
            V2ShelfArtType.READING -> ReadingArt(dark)
            V2ShelfArtType.BOOKS -> BooksArt(dark)
            V2ShelfArtType.MOUNTAIN -> MountainArt(dark)
            V2ShelfArtType.PEAK -> PeakArt(dark)
            V2ShelfArtType.NOTES -> NotesArt(dark)
            V2ShelfArtType.WINDOW -> WindowArt(dark)
            V2ShelfArtType.PHOTOS -> PhotosArt(dark)
            V2ShelfArtType.MINIMAL_SUN -> MinimalSunArt(dark)
            V2ShelfArtType.MINIMAL_RINGS -> MinimalRingsArt(dark)
            V2ShelfArtType.MINIMAL_WAVE -> MinimalWaveArt(dark)
            V2ShelfArtType.MINIMAL_DOTS -> MinimalDotsArt(dark)
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// v3xx51 — THE SCENE BOX + the redrawn scene kit.
//
// Every scene used to be drawn straight against the raw canvas. The card's
// art strip is WIDE and short (~1.6:1 on a phone, ~3.5:1 in a tablet's 2-up
// grid) while the create-collection sheet's preview tiles are near-PORTRAIT
// (52x60dp) — so the same drawing read squashed in one place and scattered
// in the other: the "broken and weird" shelf art. Scenes now draw inside a
// centred design box with a FIXED aspect, so the art is IDENTICAL on every
// surface (just letterboxed into the tone fill), and each subject was
// redrawn with proper proportions and detail.
// ────────────────────────────────────────────────────────────────────────

/** A centred scene box: the design canvas the art is composed against.
 *  [ox]/[oy] are the box's offset inside the real canvas. */
private data class ShelfScene(val ox: Float, val oy: Float, val w: Float, val h: Float)

/** The design aspect all scenes are composed for (a comfortable landscape
 *  plate — close to the phone strip, still readable in the portrait tile). */
private const val SHELF_SCENE_ASPECT = 1.25f

private fun shelfScene(canvasW: Float, canvasH: Float): ShelfScene {
    val w = minOf(canvasW, canvasH * SHELF_SCENE_ASPECT)
    val h = w / SHELF_SCENE_ASPECT
    return ShelfScene((canvasW - w) / 2f, (canvasH - h) / 2f, w, h)
}

/** Runs [draw] inside the centred scene box (see [shelfScene]). */
@Composable
private fun BoxScope.ShelfSceneCanvas(draw: DrawScope.(ShelfScene) -> Unit) {
    Canvas(Modifier.fillMaxSize()) {
        val scene = shelfScene(size.width, size.height)
        translate(scene.ox, scene.oy) { draw(scene) }
    }
}

/** A 4-point twinkle — the doodle family's sparkle (never a generic dot). */
private fun DrawScope.sparkle(cx: Float, cy: Float, r: Float, alpha: Float = 0.85f) {
    val p = Path().apply {
        moveTo(cx, cy - r)
        quadraticTo(cx + r * 0.20f, cy - r * 0.20f, cx + r, cy)
        quadraticTo(cx + r * 0.20f, cy + r * 0.20f, cx, cy + r)
        quadraticTo(cx - r * 0.20f, cy + r * 0.20f, cx - r, cy)
        quadraticTo(cx - r * 0.20f, cy - r * 0.20f, cx, cy - r)
        close()
    }
    drawPath(p, Color.White.copy(alpha = alpha))
}

/** A soft grounding shadow — an ellipse under a subject centred at (cx, cy)
 *  with the given half-width; ties every scene to the card's foot. */
private fun DrawScope.groundShadow(cx: Float, cy: Float, halfW: Float, h: Float, dark: Boolean) {
    drawOval(
        (if (dark) Color.Black else Color(0xFF553E42)).copy(alpha = if (dark) 0.18f else 0.10f),
        topLeft = androidx.compose.ui.geometry.Offset(cx - halfW, cy),
        size = androidx.compose.ui.geometry.Size(halfW * 2f, h)
    )
}

/** A rotated rectangle path (centre-less; rotates about the top-left). */
private fun rotRect(x: Float, y: Float, w: Float, h: Float, rot: Float): Path = Path().apply {
    val cos = kotlin.math.cos(rot); val sin = kotlin.math.sin(rot)
    val pts = listOf(
        androidx.compose.ui.geometry.Offset(0f, 0f),
        androidx.compose.ui.geometry.Offset(w, 0f),
        androidx.compose.ui.geometry.Offset(w, h),
        androidx.compose.ui.geometry.Offset(0f, h)
    )
    val p0 = androidx.compose.ui.geometry.Offset(x, y)
    pts.forEachIndexed { i, p ->
        val rx = p.x * cos - p.y * sin
        val ry = p.x * sin + p.y * cos
        if (i == 0) moveTo(p0.x + rx, p0.y + ry)
        else lineTo(p0.x + rx, p0.y + ry)
    }
    close()
}

/** A 5-point star polygon centred at (cx, cy). */
private fun fiveStar(cx: Float, cy: Float, outer: Float): Path {
    val inner = outer * 0.42f
    return Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = -Math.PI / 2.0 + i * Math.PI / 5.0
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** World-space point for a print-local offset rotated by [rot] about the
 *  print's top-left (x0, y0) — keeps inset doodles glued to their frame. */
private fun rotPoint(x0: Float, y0: Float, rot: Float, lx: Float, ly: Float): androidx.compose.ui.geometry.Offset =
    androidx.compose.ui.geometry.Offset(
        x0 + lx * kotlin.math.cos(rot) - ly * kotlin.math.sin(rot),
        y0 + lx * kotlin.math.sin(rot) + ly * kotlin.math.cos(rot)
    )

/** A rotated polaroid print: white paper body + a photo inset that shares
 *  the body's rotation, so the inset never slips out of the frame. */
private fun DrawScope.polaroidPrint(
    x: Float, y: Float, tw: Float, th: Float, rot: Float,
    photo: Color, stroke: Float
) {
    drawPath(rotRect(x, y, tw, th, rot), Color.White.copy(alpha = 0.92f))
    drawPath(rotRect(x, y, tw, th, rot), Color.White.copy(alpha = 0.95f), style = Stroke(width = stroke * 0.5f))
    val m = tw * 0.055f
    val ix = x + m * kotlin.math.cos(rot) - m * kotlin.math.sin(rot)
    val iy = y + m * kotlin.math.sin(rot) + m * kotlin.math.cos(rot)
    drawPath(rotRect(ix, iy, tw - 2 * m, th * 0.70f, rot), photo.copy(alpha = 0.85f))
    drawPath(rotRect(ix, iy, tw - 2 * m, th * 0.70f, rot), Color.White.copy(alpha = 0.8f), style = Stroke(width = stroke * 0.4f))
}

/** CUSTOM — the LONE STAR (redrawn v3xx51): one big five-point star with a
 *  soft halo, a two-tone inner echo and three twinkles, grounded by a shadow
 *  — the hero of the custom palette, drawn with real star geometry. */
@Composable
private fun BoxScope.StarArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val gold = if (dark) Color(0xFFC79A45) else Color(0xFFE8A33D)
    val r = h * 0.33f
    val cx = w * 0.46f; val cy = h * 0.48f
    groundShadow(w * 0.46f, h * 0.92f, r * 0.72f, h * 0.045f, dark)
    // Halo — a soft disc behind the star so it lifts off the tone fill.
    drawCircle(Color.White.copy(alpha = 0.15f), radius = r * 1.55f, center = Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.22f), radius = r * 1.22f, center = Offset(cx, cy), style = Stroke(width = stroke * 0.4f))
    val star = fiveStar(cx, cy, r)
    drawPath(star, gold.copy(alpha = 0.94f))
    drawPath(star, Color.White.copy(alpha = 0.96f), style = Stroke(width = stroke * 0.6f))
    val inner = fiveStar(cx, cy, r * 0.50f)
    drawPath(inner, Color.White.copy(alpha = 0.26f))
    drawPath(inner, Color.White.copy(alpha = 0.6f), style = Stroke(width = stroke * 0.35f))
    sparkle(cx + r * 1.42f, cy - r * 0.86f, r * 0.21f)
    sparkle(cx - r * 1.30f, cy + r * 0.42f, r * 0.13f, alpha = 0.6f)
    drawCircle(Color.White.copy(alpha = 0.8f), radius = r * 0.055f, center = Offset(cx - r * 0.98f, cy - r * 1.12f))
}

/** FAVORITES (redrawn v3xx51) — a HEART drawn from the real heart curve and
 *  rendered as a constellation: a soft glowing body, evenly spaced star dots
 *  along the outline joined by hairline chords, a solid gold star at the
 *  right lobe and a shooting star sweeping in from the top-left. */
@Composable
private fun BoxScope.ConstellationArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val rose = if (dark) Color(0xFFC95E7E) else Color(0xFFE86A8C)
    val gold = if (dark) Color(0xFFE8C27A) else Color(0xFFF2B45C)
    // The classic heart curve, sampled — accurate lobes and a real point.
    val k = h * 0.0215f
    val cx = w * 0.50f; val cy = h * 0.50f
    fun heartAt(t: Float): Offset {
        val x = 16f * sin(t) * sin(t) * sin(t)
        val y = 13f * cos(t) - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t)
        return Offset(cx + x * k, cy - y * k)
    }
    val samples = 72
    val fill = Path().apply {
        for (i in 0..samples) {
            val p = heartAt(i.toFloat() / samples * 2f * Math.PI.toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(fill, rose.copy(alpha = 0.26f))
    // Outline + the constellation net (every 6th sample = a star node).
    var prev: Offset? = null
    val nodes = mutableListOf<Offset>()
    for (i in 0..samples) {
        val p = heartAt(i.toFloat() / samples * 2f * Math.PI.toFloat())
        if (prev != null) drawLine(Color.White.copy(alpha = 0.32f), prev!!, p, strokeWidth = stroke * 0.3f)
        prev = p
        if (i % 6 == 0) nodes.add(p)
    }
    nodes.forEach { p ->
        drawCircle(Color.White.copy(alpha = 0.95f), radius = h * 0.017f, center = p)
        drawCircle(rose.copy(alpha = 0.55f), radius = h * 0.028f, center = p, style = Stroke(width = stroke * 0.3f))
    }
    // The bright north star on the right lobe.
    val node = nodes.getOrNull(nodes.size / 5)
    val starC = node ?: Offset(cx + w * 0.18f, cy - h * 0.16f)
    val outer = fiveStar(starC.x, starC.y, h * 0.085f)
    drawPath(outer, gold.copy(alpha = 0.95f))
    drawPath(outer, Color.White.copy(alpha = 0.9f), style = Stroke(width = stroke * 0.4f))
    // Shooting star — a tapered trail with a bright head, top-left.
    val trail = Path().apply {
        moveTo(w * 0.06f, h * 0.30f)
        quadraticTo(w * 0.20f, h * 0.06f, w * 0.42f, h * 0.14f)
    }
    drawPath(trail, Color.White.copy(alpha = 0.45f), style = Stroke(width = stroke * 0.35f, cap = StrokeCap.Round))
    drawCircle(Color.White.copy(alpha = 0.95f), radius = h * 0.026f, center = Offset(w * 0.42f, h * 0.14f))
    sparkle(w * 0.86f, h * 0.24f, h * 0.05f, alpha = 0.7f)
}

/** CURIYING NOW (redrawn v3xx51) — a real OPEN BOOK from the front: a cover
 *  slab with rounded corners and a bottom lip, three stepped page sheets per
 *  side (so each page block shows its own thickness at the outer edge AND
 *  the foot), a shaded gutter, ruled text with a paragraph indent, a
 *  bookmark ribbon and a page-corner curl. Grounded by a soft shadow. */
@Composable
private fun BoxScope.ReadingArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val page = if (dark) Color(0xFFE8DCC8) else Color(0xFFFCF6EB)
    val pageShade = if (dark) Color(0xFFD0BFA3) else Color(0xFFE7D8BE)
    val cover = if (dark) Color(0xFF6E4A3C) else Color(0xFFA9785C)
    val coverDim = if (dark) Color(0xFF5A3C31) else Color(0xFF95674D)
    val line = if (dark) Color(0xFF9DB58F) else Color(0xFF7B9074)
    val ribbon = if (dark) Color(0xFFC98A6D) else Color(0xFFC8604F)

    val gx = w * 0.50f              // the gutter (spine)
    val top = h * 0.28f
    val bot = h * 0.82f
    val outL = w * 0.06f
    val outR = w * 0.94f

    // One page sheet: pinched at the gutter, sagging out to its outer edge.
    fun pagePath(side: Float, outer: Float, liftX: Float, liftY: Float): Path = Path().apply {
        val ox = if (side < 0f) outer - liftX else outer + liftX
        moveTo(gx, top)
        cubicTo(gx + side * w * 0.17f, top + h * 0.035f, ox - side * w * 0.12f, top + h * 0.012f, ox, top + h * 0.085f + liftY)
        lineTo(ox, bot + liftY)
        cubicTo(ox - side * w * 0.13f, bot + h * 0.05f, gx + side * w * 0.15f, bot + h * 0.055f, gx, bot - h * 0.02f)
        close()
    }

    groundShadow(gx, bot + h * 0.085f, w * 0.42f, h * 0.045f, dark)
    // ── The cover slab, a whisper wider than the pages, with its own crease.
    drawRoundRect(
        cover.copy(alpha = 0.95f),
        topLeft = Offset(outL - w * 0.022f, top + h * 0.055f),
        size = Size((outR - outL) + w * 0.044f, (bot + h * 0.055f) - (top + h * 0.055f)),
        cornerRadius = CornerRadius(w * 0.022f)
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.88f),
        topLeft = Offset(outL - w * 0.022f, top + h * 0.055f),
        size = Size((outR - outL) + w * 0.044f, (bot + h * 0.055f) - (top + h * 0.055f)),
        cornerRadius = CornerRadius(w * 0.022f),
        style = Stroke(width = stroke * 0.5f)
    )
    // Cover foot shading — the block's thickness below the pages.
    drawLine(coverDim.copy(alpha = 0.8f), Offset(outL - w * 0.018f, bot + h * 0.038f), Offset(outR + w * 0.018f, bot + h * 0.038f), strokeWidth = stroke * 0.5f)
    // ── Three page sheets per side: bottom (furthest out + lowest) to top.
    listOf(
        Triple(0.030f, h * 0.026f, pageShade.copy(alpha = 0.95f)),
        Triple(0.015f, h * 0.013f, page.copy(alpha = 0.95f)),
        Triple(0f, 0f, page)
    ).forEach { (lx, ly, fill) ->
        listOf(-1f, 1f).forEach { side ->
            val p = pagePath(side, if (side < 0f) outL else outR, w * lx, ly)
            drawPath(p, fill)
            drawPath(p, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.45f))
        }
    }
    // ── Page block edges: two hairlines just inside each outer edge.
    listOf(-1f, 1f).forEach { side ->
        val edge = if (side < 0f) outL else outR
        listOf(0.012f, 0.024f).forEach { dx ->
            drawLine(
                pageShade.copy(alpha = 0.75f),
                Offset(edge - side * w * dx, top + h * 0.10f),
                Offset(edge - side * w * dx, bot - h * 0.02f),
                strokeWidth = stroke * 0.3f
            )
        }
    }
    // ── Ruled text: two paragraphs, indented first lines, ragged last lines.
    for (i in 0 until 4) {
        val ly = top + h * (0.19f + i * 0.115f)
        val leftStart = gx - w * 0.365f + (if (i == 0) w * 0.035f else 0f)
        val leftEnd = gx - w * 0.055f - (if (i == 3) w * 0.10f else 0f)
        drawLine(line.copy(alpha = 0.55f), Offset(leftStart, ly), Offset(leftEnd, ly), strokeWidth = 1.0f)
        val rightStart = gx + w * 0.055f + (if (i == 0) w * 0.035f else 0f)
        val rightEnd = gx + w * 0.365f - (if (i == 3) w * 0.13f else 0f)
        drawLine(line.copy(alpha = 0.55f), Offset(rightStart, ly), Offset(rightEnd, ly), strokeWidth = 1.0f)
    }
    // ── Gutter: a white join plus a soft inner crease shadow each side.
    drawLine(Color.White.copy(alpha = 0.9f), Offset(gx, top + h * 0.01f), Offset(gx, bot - h * 0.03f), strokeWidth = 1.6f)
    drawLine(line.copy(alpha = 0.28f), Offset(gx + w * 0.010f, top + h * 0.06f), Offset(gx + w * 0.010f, bot - h * 0.06f), strokeWidth = stroke * 0.4f)
    drawLine(line.copy(alpha = 0.28f), Offset(gx - w * 0.010f, top + h * 0.06f), Offset(gx - w * 0.010f, bot - h * 0.06f), strokeWidth = stroke * 0.4f)
    // ── Bookmark ribbon over the right page, V-notched tail.
    val rb = Path().apply {
        moveTo(gx + w * 0.012f, top + h * 0.02f)
        lineTo(gx + w * 0.062f, top + h * 0.02f)
        lineTo(gx + w * 0.062f, top + h * 0.40f)
        lineTo(gx + w * 0.037f, top + h * 0.33f)
        lineTo(gx + w * 0.012f, top + h * 0.40f)
        close()
    }
    drawPath(rb, ribbon.copy(alpha = 0.94f))
    drawPath(rb, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.4f))
    // ── Page-corner curl at the left page's outer bottom — a small lift.
    val curl = Path().apply {
        moveTo(outL, bot - h * 0.12f)
        quadraticTo(outL + w * 0.045f, bot - h * 0.09f, outL + w * 0.030f, bot - h * 0.02f)
        close()
    }
    drawPath(curl, Color.White.copy(alpha = 0.55f))
    drawPath(curl, Color.White.copy(alpha = 0.8f), style = Stroke(width = stroke * 0.35f))
    sparkle(w * 0.13f, h * 0.16f, h * 0.045f, alpha = 0.55f)
}

/** WANT TO READ (redrawn v3xx51) — three upright spines of different heights
 *  with title bands, a crest and a peek-out ribbon, plus a book lying FLAT
 *  across the front so the shelf reads as a real row of books rather than
 *  three bars. Pastel fills + white outlines (doodle family). */
@Composable
private fun BoxScope.BooksArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val tones = if (dark) listOf(0xFF7A5C4C, 0xFF9A7560, 0xFFC09379)
    else listOf(0xFF9C7562, 0xFFB98D79, 0xFFD39F91)
    val flatTone = if (dark) Color(0xFF8A6A58) else Color(0xFFB1866F)
    val base = h * 0.85f

    groundShadow(w * 0.48f, base + h * 0.095f, w * 0.42f, h * 0.038f, dark)

    // ── Upright spines — tall, medium, short.
    val centers = listOf(0.20f, 0.45f, 0.69f)
    val widths = listOf(0.155f, 0.175f, 0.135f)
    val heights = listOf(0.52f, 0.64f, 0.44f)
    centers.forEachIndexed { i, c ->
        val bwid = w * widths[i]
        val bhei = h * heights[i]
        val x = w * c - bwid / 2f
        val y = base - bhei
        val spine = Path().apply {
            moveTo(x, base)
            lineTo(x, y)
            lineTo(x + bwid, y)
            lineTo(x + bwid, base)
            close()
        }
        drawPath(spine, Color(tones[i]).copy(alpha = 0.94f))
        drawPath(spine, Color.White.copy(alpha = 0.92f), style = Stroke(width = stroke * 0.55f))
        // Title band + author ticks near the top.
        val bandY = y + bhei * 0.24f
        drawLine(Color.White.copy(alpha = 0.88f), Offset(x + bwid * 0.22f, bandY), Offset(x + bwid * 0.78f, bandY), strokeWidth = 1.1f)
        drawLine(Color.White.copy(alpha = 0.55f), Offset(x + bwid * 0.22f, bandY - bhei * 0.075f), Offset(x + bwid * 0.58f, bandY - bhei * 0.075f), strokeWidth = 1.0f)
        // Publisher's foot band.
        drawLine(Color.White.copy(alpha = 0.5f), Offset(x + bwid * 0.14f, base - bhei * 0.085f), Offset(x + bwid * 0.86f, base - bhei * 0.085f), strokeWidth = stroke * 0.3f)
        if (i == 1) {
            // A crest + a ribbon peeking over the tallest spine.
            drawCircle(Color.White.copy(alpha = 0.30f), radius = bwid * 0.19f, center = Offset(x + bwid * 0.5f, y + bhei * 0.47f))
            drawCircle(Color.White.copy(alpha = 0.85f), radius = bwid * 0.19f, center = Offset(x + bwid * 0.5f, y + bhei * 0.47f), style = Stroke(width = stroke * 0.35f))
            val rib = Path().apply {
                moveTo(x + bwid * 0.30f, y)
                lineTo(x + bwid * 0.42f, y)
                lineTo(x + bwid * 0.42f, y - h * 0.075f)
                lineTo(x + bwid * 0.36f, y - h * 0.052f)
                lineTo(x + bwid * 0.30f, y - h * 0.075f)
                close()
            }
            drawPath(rib, Color(if (dark) 0xFFC98A6D else 0xFFC8604F).copy(alpha = 0.92f))
            drawPath(rib, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.35f))
        }
    }

    // ── A book LYING FLAT across the front — its cover block + page edges.
    val fx = w * 0.13f
    val fw = w * 0.74f
    val ftop = base - h * 0.075f
    val fbot = base + h * 0.055f
    val flat = Path().apply {
        moveTo(fx, ftop + h * 0.012f)
        lineTo(fx + fw, ftop - h * 0.006f)
        lineTo(fx + fw - w * 0.014f, fbot - h * 0.006f)
        lineTo(fx + w * 0.014f, fbot + h * 0.006f)
        close()
    }
    drawPath(flat, flatTone.copy(alpha = 0.96f))
    drawPath(flat, Color.White.copy(alpha = 0.92f), style = Stroke(width = stroke * 0.55f))
    // The page block sitting on the flat cover.
    val pages = Path().apply {
        moveTo(fx + w * 0.016f, ftop + h * 0.026f)
        lineTo(fx + fw - w * 0.016f, ftop + h * 0.008f)
        lineTo(fx + fw - w * 0.022f, ftop + h * 0.040f)
        lineTo(fx + w * 0.022f, ftop + h * 0.056f)
        close()
    }
    drawPath(pages, Color.White.copy(alpha = 0.9f))
    drawPath(pages, Color.White.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.35f))
    drawLine(
        Color.White.copy(alpha = 0.55f),
        Offset(fx + w * 0.05f, ftop + h * 0.003f),
        Offset(fx + fw - w * 0.05f, ftop - h * 0.012f),
        strokeWidth = stroke * 0.35f
    )
}

/** CUSTOM — hand-drawn layered mountains with soft CURVED peaks, a
 *  snow-capped summit, doodle pine trees and flower dots — pastel fills +
 *  white outlines (doodle family). */
@Composable
private fun BoxScope.MountainArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val farC = if (dark) Color(0xFF54707F) else Color(0xFFBBD3DE)
    val midC = if (dark) Color(0xFF4E6F60) else Color(0xFFA2C1AC)
    val nearC = if (dark) Color(0xFF37503F) else Color(0xFF7FA68D)
    val pineC = if (dark) Color(0xFF2D4433) else Color(0xFF6D9779)
    val sunC = if (dark) Color(0xFFB98F52) else Color(0xFFF6C46A)

    groundShadow(w * 0.50f, h * 0.965f, w * 0.36f, h * 0.03f, dark)
    // Low sun with a halo ring.
    val sc = Offset(w * 0.80f, h * 0.21f)
    drawCircle(sunC.copy(alpha = 0.5f), radius = h * 0.135f, center = sc)
    drawCircle(Color.White.copy(alpha = 0.92f), radius = h * 0.082f, center = sc)
    drawCircle(Color.White.copy(alpha = 0.7f), radius = h * 0.115f, center = sc, style = Stroke(width = stroke * 0.4f))
    // Birds.
    listOf(0.30f to 0.16f, 0.40f to 0.10f).forEach { (bx, by) ->
        val b = Offset(w * bx, h * by)
        drawLine(Color.White.copy(alpha = 0.8f), Offset(b.x - w * 0.035f, b.y + h * 0.012f), b, strokeWidth = 1.1f)
        drawLine(Color.White.copy(alpha = 0.8f), b, Offset(b.x + w * 0.035f, b.y + h * 0.012f), strokeWidth = 1.1f)
    }
    // Far ridge.
    val far = Path().apply {
        moveTo(-w * 0.02f, h * 0.78f)
        quadraticTo(w * 0.24f, h * 0.38f, w * 0.52f, h * 0.70f)
        quadraticTo(w * 0.78f, h * 0.48f, w * 1.02f, h * 0.74f)
        lineTo(w * 1.02f, h * 0.88f)
        lineTo(-w * 0.02f, h * 0.88f)
        close()
    }
    drawPath(far, farC.copy(alpha = 0.8f))
    drawPath(far, Color.White.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.45f))
    // Mid peak with a snow cap.
    val mid = Path().apply {
        moveTo(-w * 0.05f, h * 0.90f)
        lineTo(w * 0.20f, h * 0.52f)
        lineTo(w * 0.32f, h * 0.40f)
        lineTo(w * 0.44f, h * 0.54f)
        lineTo(w * 0.72f, h * 0.90f)
        close()
    }
    drawPath(mid, midC.copy(alpha = 0.85f))
    drawPath(mid, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.5f))
    val midCap = Path().apply {
        moveTo(w * 0.245f, h * 0.49f)
        lineTo(w * 0.32f, h * 0.40f)
        lineTo(w * 0.395f, h * 0.49f)
        lineTo(w * 0.355f, h * 0.465f)
        lineTo(w * 0.32f, h * 0.495f)
        lineTo(w * 0.285f, h * 0.465f)
        close()
    }
    drawPath(midCap, Color.White.copy(alpha = 0.9f))
    // Near ridge (darker, a soft curve) with its own snow dusting.
    val near = Path().apply {
        moveTo(-w * 0.04f, h * 0.94f)
        quadraticTo(w * 0.30f, h * 0.74f, w * 0.62f, h * 0.86f)
        quadraticTo(w * 0.84f, h * 0.80f, w * 1.04f, h * 0.90f)
        lineTo(w * 1.04f, h * 0.96f)
        lineTo(-w * 0.04f, h * 0.96f)
        close()
    }
    drawPath(near, nearC.copy(alpha = 0.9f))
    drawPath(near, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.5f))
    // Pines standing on the near ridge.
    listOf(0.14f, 0.26f, 0.76f).forEach { fx ->
        val px = w * fx; val py = h * 0.93f
        val tree = Path().apply {
            moveTo(px, py - h * 0.20f)
            lineTo(px + w * 0.045f, py - h * 0.105f)
            lineTo(px + w * 0.026f, py - h * 0.105f)
            lineTo(px + w * 0.062f, py - h * 0.02f)
            lineTo(px + w * 0.024f, py - h * 0.02f)
            lineTo(px, py)
            close()
        }
        drawPath(tree, pineC.copy(alpha = 0.85f))
        drawPath(tree, Color.White.copy(alpha = 0.75f), style = Stroke(width = stroke * 0.35f))
    }
}

/** COMPLETED — kept MINIMAL (v3xx42): ONE clean summit silhouette with a
 *  small snow cap, a planted flag and a thin ground band — the busy sun,
 *  the second ridge, the birds and the badge circle are gone. */
@Composable
private fun BoxScope.PeakArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val nearC = if (dark) Color(0xFF5E7E6C) else Color(0xFF8FB4A0)
    val sunC = if (dark) Color(0xFFB98F52) else Color(0xFFF6C46A)

    // A small sun behind the summit + a ground band under it.
    val solC = Offset(w * 0.76f, h * 0.30f)
    drawCircle(sunC.copy(alpha = 0.45f), radius = h * 0.11f, center = solC)
    drawCircle(Color.White.copy(alpha = 0.85f), radius = h * 0.062f, center = solC)
    drawLine(nearC.copy(alpha = 0.45f), Offset(w * 0.08f, h * 0.90f), Offset(w * 0.92f, h * 0.90f), strokeWidth = w * 0.010f)
    // One clean peak, sharp apex at the centre.
    val peak = Path().apply {
        moveTo(w * 0.06f, h * 0.91f)
        quadraticTo(w * 0.26f, h * 0.62f, w * 0.42f, h * 0.42f)
        quadraticTo(w * 0.47f, h * 0.345f, w * 0.50f, h * 0.32f)
        quadraticTo(w * 0.53f, h * 0.345f, w * 0.58f, h * 0.42f)
        quadraticTo(w * 0.74f, h * 0.62f, w * 0.94f, h * 0.91f)
        close()
    }
    drawPath(peak, nearC.copy(alpha = 0.72f))
    drawPath(peak, Color.White.copy(alpha = 0.92f), style = Stroke(width = stroke * 0.6f))
    // A ridge line inside the peak — the shape's own shading.
    drawPath(
        Path().apply {
            moveTo(w * 0.50f, h * 0.34f)
            quadraticTo(w * 0.42f, h * 0.58f, w * 0.30f, h * 0.86f)
        },
        nearC.copy(alpha = 0.4f), style = Stroke(width = stroke * 0.35f)
    )
    // Snow cap with a jagged hem.
    val cap = Path().apply {
        moveTo(w * 0.437f, h * 0.405f)
        quadraticTo(w * 0.475f, h * 0.335f, w * 0.50f, h * 0.32f)
        quadraticTo(w * 0.525f, h * 0.335f, w * 0.563f, h * 0.405f)
        lineTo(w * 0.537f, h * 0.382f)
        lineTo(w * 0.515f, h * 0.405f)
        lineTo(w * 0.487f, h * 0.378f)
        lineTo(w * 0.463f, h * 0.405f)
        close()
    }
    drawPath(cap, Color.White.copy(alpha = 0.92f))
    // The planted flag: pole, pennant, a tiny base.
    val fx = w * 0.50f; val fy = h * 0.32f
    drawLine(Color.White.copy(alpha = 0.95f), Offset(fx, fy), Offset(fx, fy - h * 0.17f), strokeWidth = 1.5f)
    val pennant = Path().apply {
        moveTo(fx, fy - h * 0.17f)
        quadraticTo(fx + w * 0.065f, fy - h * 0.145f, fx + w * 0.10f, fy - h * 0.105f)
        lineTo(fx, fy - h * 0.05f)
        close()
    }
    drawPath(pennant, Color.White.copy(alpha = 0.95f))
    drawPath(pennant, Color.White.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.35f))
}

/** NOTES — a hand-drawn SPIRAL notebook: cream sheet with rings binding
 *  the top edge, ruled lines, a heart doodle and a pencil resting on it —
 *  pastel fills + white outlines (doodle family). */
@Composable
private fun BoxScope.NotesArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val paper = if (dark) Color(0xFFEAD9BE) else Color(0xFFFFFCF4)
    val paperShade = if (dark) Color(0xFFD3C2A4) else Color(0xFFE7DAC0)
    val rule = if (dark) Color(0xFFA08FC0) else Color(0xFF9C86C4)
    val margin = if (dark) Color(0xFFCE8A8A) else Color(0xFFDE9090)
    val heart = if (dark) Color(0xFFC98A6D) else Color(0xFFC8604F)
    val wood = if (dark) Color(0xFFE3C0A8) else Color(0xFFD9A98F)
    val metal = if (dark) Color(0xFFB9B2A6) else Color(0xFFCFC7BA)
    val eraser = if (dark) Color(0xFFE3A9A0) else Color(0xFFE58E8E)
    val lead = if (dark) Color(0xFF2E2622) else Color(0xFF443833)

    // ── The sheet.
    val sx0 = w * 0.09f; val sy0 = h * 0.17f
    val sw = w * 0.52f; val sh = h * 0.70f
    groundShadow(w * 0.35f, sy0 + sh + h * 0.055f, w * 0.25f, h * 0.028f, dark)
    drawRoundRect(paper, Offset(sx0, sy0), Size(sw, sh), CornerRadius(w * 0.022f))
    drawRoundRect(Color.White.copy(alpha = 0.92f), Offset(sx0, sy0), Size(sw, sh), CornerRadius(w * 0.022f), style = Stroke(width = stroke * 0.6f))
    // ── Spiral binding: five wire rings over the top edge.
    for (i in 0 until 5) {
        val hx = sx0 + sw * (0.15f + 0.70f * i / 4f)
        val hy = sy0
        drawCircle(paperShade.copy(alpha = 0.95f), radius = sw * 0.042f, center = Offset(hx, hy))
        drawCircle(Color.White.copy(alpha = 0.85f), radius = sw * 0.042f, center = Offset(hx, hy), style = Stroke(width = stroke * 0.45f))
        drawArc(
            Color.White.copy(alpha = 0.9f), startAngle = 190f, sweepAngle = 165f, useCenter = false,
            topLeft = Offset(hx - sw * 0.030f, hy - sh * 0.045f),
            size = Size(sw * 0.060f, sh * 0.090f),
            style = Stroke(width = stroke * 0.4f)
        )
    }
    // ── Margin rule + ruled writing lines.
    drawLine(margin.copy(alpha = 0.6f), Offset(sx0 + sw * 0.14f, sy0 + sh * 0.12f), Offset(sx0 + sw * 0.14f, sy0 + sh * 0.94f), strokeWidth = stroke * 0.35f)
    for (i in 0 until 5) {
        val ly = sy0 + sh * (0.26f + i * 0.145f)
        drawLine(rule.copy(alpha = 0.5f), Offset(sx0 + sw * 0.18f, ly), Offset(sx0 + sw * 0.90f, ly), strokeWidth = 0.9f)
    }
    // ── A heart doodled in the bottom corner of the page.
    val hx = sx0 + sw * 0.32f; val hy = sy0 + sh * 0.86f; val hs = sw * 0.085f
    val heartPath = Path().apply {
        moveTo(hx, hy + hs * 0.58f)
        cubicTo(hx - hs * 0.92f, hy - hs * 0.10f, hx - hs * 0.46f, hy - hs * 0.80f, hx, hy - hs * 0.26f)
        cubicTo(hx + hs * 0.46f, hy - hs * 0.80f, hx + hs * 0.92f, hy - hs * 0.10f, hx, hy + hs * 0.58f)
        close()
    }
    drawPath(heartPath, heart.copy(alpha = 0.85f))
    drawPath(heartPath, Color.White.copy(alpha = 0.75f), style = Stroke(width = stroke * 0.35f))
    // ── The pencil resting across the lower right: body, ferrule, eraser,
    //    sharpened tip — drawn as a real tapered pencil.
    val px0 = w * 0.64f; val py0 = h * 0.30f
    val px1 = w * 0.94f; val py1 = h * 0.82f
    val dx = px1 - px0; val dy = py1 - py0
    val len = kotlin.math.sqrt(dx * dx + dy * dy)
    val nx = -dy / len; val ny = dx / len
    val half = w * 0.021f
    fun at(t: Float, off: Float = 0f) = Offset(
        px0 + dx * t + nx * off,
        py0 + dy * t + ny * off
    )
    drawLine(eraser, at(0.04f), at(0.13f), strokeWidth = half * 1.7f, cap = StrokeCap.Round)
    drawLine(metal, at(0.13f), at(0.20f), strokeWidth = half * 1.8f)
    drawLine(wood, at(0.19f), at(0.90f), strokeWidth = half * 2f, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.55f), at(0.24f, -half * 0.45f), at(0.88f, -half * 0.45f), strokeWidth = stroke * 0.4f)
    drawLine(Color.Black.copy(alpha = 0.10f), at(0.24f, half * 0.5f), at(0.88f, half * 0.5f), strokeWidth = stroke * 0.35f)
    val tip = Path().apply {
        val a = at(0.88f, -half)
        val b = at(0.88f, half)
        val c = at(1.0f)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close()
    }
    drawPath(tip, wood)
    drawPath(tip, Color.White.copy(alpha = 0.8f), style = Stroke(width = stroke * 0.3f))
    drawCircle(lead, radius = half * 0.42f, center = at(0.995f))
    sparkle(w * 0.90f, h * 0.14f, h * 0.045f, alpha = 0.6f)
}

/** PERSONAL — a hand-drawn window with a moon and a plant on the sill:
 *  pastel sky fill + white frame outlines (doodle family). */
@Composable
private fun BoxScope.WindowArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val sky = if (dark) Brush.verticalGradient(listOf(Color(0xFF2E3A50), Color(0xFF4A6064)))
    else Brush.verticalGradient(listOf(Color(0xFFF8DAAE), Color(0xFFC2D5C3)))
    val frame = if (dark) Color(0xFF6E4A3C) else Color(0xFFA9785C)
    val moonFill = if (dark) Color(0xFFF6E7BE) else Color(0xFFFFF4D9)
    val leaf = if (dark) Color(0xFF9DB58F) else Color(0xFF7C9C74)
    val pot = if (dark) Color(0xFF8A5A4A) else Color(0xFFB3796A)

    val fx = w * 0.08f; val fy = h * 0.12f
    val fw = w * 0.56f; val fh = h * 0.62f
    groundShadow(w * 0.36f, fy + fh + h * 0.085f, w * 0.26f, h * 0.028f, dark)
    // ── The pane: a real sky, then the frame + sill over it.
    drawRoundRect(sky, Offset(fx, fy), Size(fw, fh), CornerRadius(w * 0.026f))
    listOf(0.18f to 0.18f, 0.76f to 0.13f, 0.60f to 0.66f, 0.30f to 0.74f).forEach { (sx, sy) ->
        sparkle(fx + fw * sx, fy + fh * sy, h * 0.024f, alpha = 0.75f)
    }
    // Full moon with two faint maria.
    val mc = Offset(fx + fw * 0.70f, fy + fh * 0.28f)
    drawCircle(moonFill, radius = fw * 0.115f, center = mc)
    drawCircle(frame.copy(alpha = 0.16f), radius = fw * 0.030f, center = Offset(mc.x - fw * 0.035f, mc.y - fw * 0.015f))
    drawCircle(frame.copy(alpha = 0.12f), radius = fw * 0.019f, center = Offset(mc.x + fw * 0.030f, mc.y + fw * 0.040f))
    // Muntins — one vertical, one horizontal.
    drawLine(frame.copy(alpha = 0.8f), Offset(fx + fw * 0.5f, fy), Offset(fx + fw * 0.5f, fy + fh), strokeWidth = stroke * 0.6f)
    drawLine(frame.copy(alpha = 0.8f), Offset(fx, fy + fh * 0.5f), Offset(fx + fw, fy + fh * 0.5f), strokeWidth = stroke * 0.6f)
    // Frame + a white hairline just inside it.
    drawRoundRect(frame, Offset(fx, fy), Size(fw, fh), CornerRadius(w * 0.026f), style = Stroke(width = stroke * 0.9f))
    drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(fx + w * 0.008f, fy + w * 0.008f), Size(fw - w * 0.016f, fh - w * 0.016f), CornerRadius(w * 0.020f), style = Stroke(width = stroke * 0.45f))
    // Sill.
    drawRoundRect(
        frame.copy(alpha = 0.95f),
        Offset(fx - w * 0.030f, fy + fh),
        Size(fw + w * 0.060f, h * 0.055f),
        CornerRadius(w * 0.014f)
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.85f),
        Offset(fx - w * 0.030f, fy + fh),
        Size(fw + w * 0.060f, h * 0.055f),
        CornerRadius(w * 0.014f),
        style = Stroke(width = stroke * 0.4f)
    )
    // ── A plant on the sill: pot + three leaves.
    val pcx = w * 0.78f
    val potTop = fy + fh - h * 0.09f
    drawLine(leaf.copy(alpha = 0.9f), Offset(pcx, potTop), Offset(pcx, potTop - h * 0.20f), strokeWidth = stroke * 0.7f)
    val leafL = Path().apply {
        moveTo(pcx, potTop - h * 0.10f)
        quadraticTo(pcx - w * 0.075f, potTop - h * 0.14f, pcx - w * 0.045f, potTop - h * 0.20f)
        quadraticTo(pcx - w * 0.010f, potTop - h * 0.17f, pcx, potTop - h * 0.10f)
        close()
    }
    drawPath(leafL, leaf.copy(alpha = 0.8f))
    drawPath(leafL, Color.White.copy(alpha = 0.8f), style = Stroke(width = stroke * 0.35f))
    val leafR = Path().apply {
        moveTo(pcx, potTop - h * 0.14f)
        quadraticTo(pcx + w * 0.075f, potTop - h * 0.18f, pcx + w * 0.045f, potTop - h * 0.24f)
        quadraticTo(pcx + w * 0.010f, potTop - h * 0.21f, pcx, potTop - h * 0.14f)
        close()
    }
    drawPath(leafR, leaf.copy(alpha = 0.9f))
    drawPath(leafR, Color.White.copy(alpha = 0.8f), style = Stroke(width = stroke * 0.35f))
    val potPath = Path().apply {
        moveTo(pcx - w * 0.055f, potTop)
        lineTo(pcx + w * 0.055f, potTop)
        lineTo(pcx + w * 0.038f, potTop + h * 0.11f)
        lineTo(pcx - w * 0.038f, potTop + h * 0.11f)
        close()
    }
    drawPath(potPath, pot.copy(alpha = 0.92f))
    drawPath(potPath, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.5f))
    drawLine(Color.White.copy(alpha = 0.5f), Offset(pcx - w * 0.050f, potTop + h * 0.028f), Offset(pcx + w * 0.050f, potTop + h * 0.028f), strokeWidth = stroke * 0.3f)
}

/** SAVED ENTRIES (redrawn v3xx51) — a stack of three instant prints: two
 *  smaller tilted ones behind (a crescent-moon night and a sunset) and ONE
 *  big front polaroid carrying a real sun-over-hills scene with two birds,
 *  held by angled washi tape with a handwritten caption line and a small
 *  gold star on its lip. Real print proportions (thicker bottom lip), so it
 *  reads as saved photos rather than abstract blocks. */
@Composable
private fun BoxScope.PhotosArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val cool = if (dark) Color(0xFF46657A) else Color(0xFF7FB2CC)
    val deep = if (dark) Color(0xFF37596E) else Color(0xFF5E9CC2)
    val warm = if (dark) Color(0xFFB08A5E) else Color(0xFFE8B77A)
    val gold = if (dark) Color(0xFFD8B46A) else Color(0xFFF0B94A)

    groundShadow(w * 0.50f, h * 0.945f, w * 0.33f, h * 0.038f, dark)

    // ── Back-left print — a crest moon over a night sky (tilted left).
    val blw = w * 0.28f; val blh = h * 0.50f
    val blx = w * 0.11f; val bly = h * 0.34f
    val blr = -0.22f
    polaroidPrint(blx, bly, blw, blh, blr, cool, stroke)
    drawCircle(gold.copy(alpha = 0.92f), radius = blw * 0.115f, center = rotPoint(blx, bly, blr, blw * 0.50f, blh * 0.27f))
    drawCircle(cool.copy(alpha = 0.96f), radius = blw * 0.098f, center = rotPoint(blx, bly, blr, blw * 0.57f, blh * 0.225f))
    listOf(0.24f to 0.51f, 0.78f to 0.42f).forEach { (sx, sy) ->
        val sp = rotPoint(blx, bly, blr, blw * sx, blh * sy)
        sparkle(sp.x, sp.y, blw * 0.030f, alpha = 0.75f)
    }
    drawLine(
        Color.White.copy(alpha = 0.55f),
        rotPoint(blx, bly, blr, blw * 0.12f, blh * 0.585f),
        rotPoint(blx, bly, blr, blw * 0.50f, blh * 0.585f),
        strokeWidth = 1.0f
    )

    // ── Back-right print — a low sun over a hill line (tilted right).
    val brw = w * 0.26f; val brh = h * 0.47f
    val brx = w * 0.63f; val bry = h * 0.36f
    val brr = 0.22f
    polaroidPrint(brx, bry, brw, brh, brr, warm, stroke)
    drawCircle(gold.copy(alpha = 0.5f), radius = brw * 0.15f, center = rotPoint(brx, bry, brr, brw * 0.52f, brh * 0.26f))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = brw * 0.095f, center = rotPoint(brx, bry, brr, brw * 0.52f, brh * 0.26f))
    drawPath(
        Path().apply {
            val a = rotPoint(brx, bry, brr, brw * 0.10f, brh * 0.52f)
            val b = rotPoint(brx, bry, brr, brw * 0.52f, brh * 0.38f)
            val c = rotPoint(brx, bry, brr, brw * 0.94f, brh * 0.52f)
            moveTo(a.x, a.y); quadraticTo(b.x, b.y, c.x, c.y)
        },
        Color.White.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.4f)
    )

    // ── Front print — the hero, nearly straight, with a real scene inside.
    val tw = w * 0.46f; val th = h * 0.76f
    val fr = 0.03f
    val fx = w * 0.50f - tw * 0.5f
    val fy = h * 0.90f - th
    polaroidPrint(fx, fy, tw, th, fr, deep, stroke)
    val wx0 = fx + tw * 0.058f; val wy0 = fy + tw * 0.058f
    val ww = tw * 0.884f; val wh = th * 0.70f
    // Sun with a thin halo.
    val sunC = Offset(wx0 + ww * 0.23f, wy0 + wh * 0.31f)
    drawCircle(gold.copy(alpha = 0.95f), radius = ww * 0.105f, center = sunC)
    drawCircle(Color.White.copy(alpha = 0.85f), radius = ww * 0.148f, center = sunC, style = Stroke(width = stroke * 0.35f))
    // Two hills + a horizon hairline.
    val hillL = Path().apply {
        moveTo(wx0, wy0 + wh)
        quadraticTo(wx0 + ww * 0.30f, wy0 + wh * 0.30f, wx0 + ww * 0.64f, wy0 + wh)
        close()
    }
    drawPath(hillL, deep.copy(alpha = 0.5f))
    drawPath(hillL, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.4f))
    val hillR = Path().apply {
        moveTo(wx0 + ww * 0.36f, wy0 + wh)
        quadraticTo(wx0 + ww * 0.68f, wy0 + wh * 0.46f, wx0 + ww, wy0 + wh)
        close()
    }
    drawPath(hillR, deep.copy(alpha = 0.72f))
    drawPath(hillR, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.4f))
    drawLine(Color.White.copy(alpha = 0.55f), Offset(wx0, wy0 + wh), Offset(wx0 + ww, wy0 + wh), strokeWidth = stroke * 0.3f)
    // Two birds above the hills.
    listOf(0.60f to 0.30f, 0.78f to 0.23f).forEach { (bx, by) ->
        val b = Offset(wx0 + ww * bx, wy0 + wh * by)
        drawLine(Color.White.copy(alpha = 0.85f), Offset(b.x - ww * 0.045f, b.y + ww * 0.014f), b, strokeWidth = 1.1f)
        drawLine(Color.White.copy(alpha = 0.85f), b, Offset(b.x + ww * 0.045f, b.y + ww * 0.014f), strokeWidth = 1.1f)
    }
    // Photo inner shadow just inside the window.
    drawPath(
        rotRect(wx0 - tw * 0.012f, wy0 - tw * 0.012f, ww + tw * 0.024f, wh + tw * 0.024f, fr),
        Color.Black.copy(alpha = 0.10f),
        style = Stroke(width = stroke * 0.5f)
    )
    // Caption line written on the lip + the little gold favourite star.
    drawLine(Color.White.copy(alpha = 0.5f), Offset(fx + tw * 0.12f, fy + th * 0.86f), Offset(fx + tw * 0.60f, fy + th * 0.86f), strokeWidth = 1.1f)
    val sc = Offset(fx + tw * 0.79f, fy + th * 0.855f)
    val starPath = fiveStar(sc.x, sc.y, w * 0.020f)
    drawPath(starPath, gold.copy(alpha = 0.95f))
    drawPath(starPath, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.3f))
    // Washi tape — two angled strips holding the front print's top corners.
    listOf(0.10f to -0.30f, 0.66f to 0.30f).forEach { (tx, trot) ->
        val tp = rotPoint(fx, fy, fr, tw * tx, -th * 0.015f)
        drawPath(rotRect(tp.x, tp.y, tw * 0.24f, th * 0.085f, fr + trot), Color.White.copy(alpha = 0.72f))
        drawPath(rotRect(tp.x, tp.y, tw * 0.24f, th * 0.085f, fr + trot), Color.White.copy(alpha = 0.55f), style = Stroke(width = stroke * 0.3f))
    }
}

/** MINIMAL custom — a hot-air BALLOON (v3xx42): the old generic sun is
 *  now a distinct scene — an envelope with a centre band + seam stitch, a
 *  small basket hanging on three ropes, a drifting cloud and a grounding
 *  shadow. */
@Composable
private fun BoxScope.MinimalSunArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    val band = if (dark) Color(0xFFD8A25E) else Color(0xFFEFAE55)
    val basket = if (dark) Color(0xFFB98A5C) else Color(0xFFC98F5C)

    val cx = w * 0.42f
    val topY = h * 0.09f
    val midY = h * 0.44f
    val eh = w * 0.19f
    groundShadow(w * 0.42f, h * 0.95f, w * 0.17f, h * 0.026f, dark)
    // ── Envelope: a wide hot-air teardrop.
    val env = Path().apply {
        moveTo(cx, topY)
        cubicTo(cx + eh * 1.00f, topY + h * 0.09f, cx + eh * 1.10f, midY - h * 0.075f, cx + eh * 0.28f, midY)
        quadraticTo(cx, midY + h * 0.035f, cx - eh * 0.28f, midY)
        cubicTo(cx - eh * 1.10f, midY - h * 0.075f, cx - eh * 1.00f, topY + h * 0.09f, cx, topY)
        close()
    }
    drawPath(env, band.copy(alpha = 0.8f))
    drawPath(env, Color.White.copy(alpha = 0.92f), style = Stroke(width = stroke * 0.6f))
    // Gore seams + the burn band.
    listOf(-0.62f, 0f, 0.62f).forEach { g ->
        drawPath(
            Path().apply {
                moveTo(cx + eh * g * 0.34f, topY + h * 0.045f)
                quadraticTo(cx + eh * g * 1.02f, topY + h * 0.22f, cx + eh * g * 0.30f, midY - h * 0.005f)
            },
            Color.White.copy(alpha = 0.6f), style = Stroke(width = stroke * 0.35f)
        )
    }
    drawLine(Color.White.copy(alpha = 0.85f), Offset(cx - eh * 0.86f, h * 0.30f), Offset(cx + eh * 0.86f, h * 0.30f), strokeWidth = stroke * 0.5f)
    // ── Neck + four ropes down to the basket.
    drawLine(Color.White.copy(alpha = 0.8f), Offset(cx - eh * 0.10f, midY), Offset(cx - eh * 0.10f, midY + h * 0.035f), strokeWidth = stroke * 0.4f)
    drawLine(Color.White.copy(alpha = 0.8f), Offset(cx + eh * 0.10f, midY), Offset(cx + eh * 0.10f, midY + h * 0.035f), strokeWidth = stroke * 0.4f)
    listOf(-0.22f, -0.08f, 0.08f, 0.22f).forEach { r ->
        drawLine(
            ink.copy(alpha = 0.45f),
            Offset(cx + eh * r, midY + h * 0.02f),
            Offset(cx + eh * r * 0.34f, midY + h * 0.145f),
            strokeWidth = 1.0f
        )
    }
    // ── The basket: a woven trapezoid with a rim.
    val bTop = midY + h * 0.145f
    val bHalf = eh * 0.26f
    val bkt = Path().apply {
        moveTo(cx - bHalf, bTop)
        lineTo(cx + bHalf, bTop)
        lineTo(cx + bHalf * 0.78f, bTop + h * 0.105f)
        lineTo(cx - bHalf * 0.78f, bTop + h * 0.105f)
        close()
    }
    drawPath(bkt, basket.copy(alpha = 0.92f))
    drawPath(bkt, Color.White.copy(alpha = 0.9f), style = Stroke(width = stroke * 0.5f))
    drawLine(Color.White.copy(alpha = 0.6f), Offset(cx - bHalf * 0.92f, bTop + h * 0.034f), Offset(cx + bHalf * 0.92f, bTop + h * 0.034f), strokeWidth = stroke * 0.3f)
    drawLine(Color.White.copy(alpha = 0.6f), Offset(cx - bHalf * 0.85f, bTop + h * 0.068f), Offset(cx + bHalf * 0.85f, bTop + h * 0.068f), strokeWidth = stroke * 0.3f)
    // ── A cloud + a bird drifting past.
    val cloud = Path().apply {
        moveTo(w * 0.70f, h * 0.30f)
        quadraticTo(w * 0.70f, h * 0.235f, w * 0.765f, h * 0.245f)
        quadraticTo(w * 0.805f, h * 0.185f, w * 0.865f, h * 0.245f)
        quadraticTo(w * 0.925f, h * 0.265f, w * 0.905f, h * 0.305f)
        close()
    }
    drawPath(cloud, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.45f))
    listOf(0.74f to 0.66f, 0.86f to 0.60f).forEach { (bx, by) ->
        val b = Offset(w * bx, h * by)
        drawLine(Color.White.copy(alpha = 0.8f), Offset(b.x - w * 0.032f, b.y + h * 0.012f), b, strokeWidth = 1.1f)
        drawLine(Color.White.copy(alpha = 0.8f), b, Offset(b.x + w * 0.032f, b.y + h * 0.012f), strokeWidth = 1.1f)
    }
}

/** MINIMAL custom — a PLANET with its ring (v3xx42): a distinct solar
 *  scene — a globe with an atmosphere line, a tilted ring that passes IN
 *  FRONT of the planet (back arc + front arc), a small orbiting moon and
 *  two twinkle stars. */
@Composable
private fun BoxScope.MinimalRingsArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val globe = if (dark) Color(0xFF6D7E9A) else Color(0xFFA3B9D1)
    val ring = if (dark) Color(0xFFD8B46A) else Color(0xFFE8BE72)
    val cx = w * 0.47f; val cy = h * 0.48f; val r = h * 0.205f

    listOf(0.13f to 0.20f, 0.30f to 0.84f, 0.85f to 0.20f, 0.92f to 0.62f).forEach { (sx, sy) ->
        sparkle(w * sx, h * sy, h * 0.026f, alpha = 0.7f)
    }
    val rw = r * 2.6f; val rh = r * 0.90f
    val ringTL = Offset(cx - rw / 2f, cy - rh / 2f)
    val ringSize = Size(rw, rh)
    // The far half of the ring passes BEHIND the globe.
    drawArc(ring.copy(alpha = 0.72f), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = ringTL, size = ringSize, style = Stroke(width = stroke * 0.75f))
    // The globe + a lit polar cap and its terminator.
    drawCircle(globe.copy(alpha = 0.96f), radius = r, center = Offset(cx, cy))
    drawArc(Color.White.copy(alpha = 0.20f), startAngle = 195f, sweepAngle = 150f, useCenter = false, topLeft = Offset(cx - r * 0.84f, cy - r * 0.84f), size = Size(r * 1.68f, r * 1.68f), style = Stroke(width = r * 0.26f))
    drawLine(globe.copy(alpha = 0.35f), Offset(cx - r * 0.80f, cy + r * 0.42f), Offset(cx + r * 0.80f, cy + r * 0.42f), strokeWidth = stroke * 0.35f)
    drawCircle(Color.White.copy(alpha = 0.92f), radius = r, center = Offset(cx, cy), style = Stroke(width = stroke * 0.6f))
    // The near half sweeps ACROSS the globe.
    drawArc(ring, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = ringTL, size = ringSize, style = Stroke(width = stroke * 0.85f))
    // A cratered moon on its own orbit.
    val mc = Offset(cx + r * 1.62f, cy - r * 1.10f)
    drawCircle(globe.copy(alpha = 0.9f), radius = w * 0.028f, center = mc)
    drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.028f, center = mc, style = Stroke(width = stroke * 0.4f))
    drawCircle(globe, radius = w * 0.009f, center = Offset(mc.x - w * 0.008f, mc.y + w * 0.006f))
}

/** MINIMAL custom — a SAILBOAT (v3xx42): a distinct scene — a hull with
 *  a mast, a big triangular sail and a small pennant, riding two soft
 *  wave strokes with a puff of wind behind it. */
@Composable
private fun BoxScope.MinimalWaveArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    val sailC = if (dark) Color(0xFFE7DAC6) else Color(0xFFFCF5EA)
    val hullC = if (dark) Color(0xFF8A6A58) else Color(0xFFB98D79)
    val sunC = if (dark) Color(0xFFD8A25E) else Color(0xFFF6C46A)
    val water = if (dark) Color(0xFF6E93A8) else Color(0xFF9EC6D6)

    // Sun + a gull high in the sky.
    val solC = Offset(w * 0.80f, h * 0.20f)
    drawCircle(sunC.copy(alpha = 0.45f), radius = h * 0.115f, center = solC)
    drawCircle(Color.White.copy(alpha = 0.9f), radius = h * 0.072f, center = solC)
    val gull = Offset(w * 0.24f, h * 0.17f)
    drawLine(Color.White.copy(alpha = 0.85f), Offset(gull.x - w * 0.045f, gull.y + h * 0.014f), gull, strokeWidth = 1.1f)
    drawLine(Color.White.copy(alpha = 0.85f), gull, Offset(gull.x + w * 0.045f, gull.y + h * 0.014f), strokeWidth = 1.1f)
    // The sea line.
    drawLine(water.copy(alpha = 0.75f), Offset(w * 0.04f, h * 0.78f), Offset(w * 0.96f, h * 0.78f), strokeWidth = stroke * 0.6f)
    // Hull — a proper dinghy with a deck line and a keel.
    val hull = Path().apply {
        moveTo(w * 0.30f, h * 0.62f)
        lineTo(w * 0.70f, h * 0.62f)
        quadraticTo(w * 0.70f, h * 0.72f, w * 0.58f, h * 0.74f)
        lineTo(w * 0.40f, h * 0.74f)
        quadraticTo(w * 0.29f, h * 0.72f, w * 0.30f, h * 0.62f)
        close()
    }
    drawPath(hull, hullC.copy(alpha = 0.95f))
    drawPath(hull, Color.White.copy(alpha = 0.92f), style = Stroke(width = stroke * 0.55f))
    drawLine(Color.White.copy(alpha = 0.75f), Offset(w * 0.325f, h * 0.655f), Offset(w * 0.675f, h * 0.655f), strokeWidth = stroke * 0.35f)
    // Mast + main sail + jib.
    val mx = w * 0.47f
    drawLine(Color.White.copy(alpha = 0.9f), Offset(mx, h * 0.62f), Offset(mx, h * 0.12f), strokeWidth = 1.4f)
    val main = Path().apply {
        moveTo(mx + w * 0.010f, h * 0.145f)
        quadraticTo(w * 0.625f, h * 0.34f, w * 0.655f, h * 0.595f)
        lineTo(mx + w * 0.010f, h * 0.595f)
        close()
    }
    drawPath(main, sailC.copy(alpha = 0.95f))
    drawPath(main, Color.White.copy(alpha = 0.9f), style = Stroke(width = stroke * 0.5f))
    val jib = Path().apply {
        moveTo(mx - w * 0.010f, h * 0.20f)
        quadraticTo(w * 0.355f, h * 0.36f, w * 0.325f, h * 0.595f)
        lineTo(mx - w * 0.010f, h * 0.595f)
        close()
    }
    drawPath(jib, sailC.copy(alpha = 0.8f))
    drawPath(jib, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.45f))
    // Pennant at the masthead.
    val pennant = Path().apply {
        moveTo(mx, h * 0.12f)
        lineTo(mx + w * 0.085f, h * 0.155f)
        lineTo(mx, h * 0.19f)
        close()
    }
    drawPath(pennant, hullC.copy(alpha = 0.95f))
    drawPath(pennant, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.35f))
    // Foreground waves lapping over the hull's waterline.
    listOf(0.80f to 0.06f, 0.86f to 0.34f).forEach { (wy, off) ->
        val wave = Path().apply {
            moveTo(w * (0.10f + off), h * wy)
            cubicTo(w * (0.24f + off), h * (wy - 0.045f), w * (0.36f + off), h * (wy + 0.045f), w * (0.50f + off), h * wy)
        }
        drawPath(wave, water.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.6f, cap = StrokeCap.Round))
        drawPath(wave, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.35f, cap = StrokeCap.Round))
    }
}

/** MINIMAL custom — a KITE (v3xx42): a distinct scene — a diamond kite
 *  with cross spars, a wavy wind line down to a small hand, a tail of
 *  little bows and one soft cloud behind. */
@Composable
private fun BoxScope.MinimalDotsArt(dark: Boolean) = ShelfSceneCanvas { s ->
    val w = s.w; val h = s.h
    val stroke = 1.8.dp.toPx()
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    val body = if (dark) Color(0xFFD8A25E) else Color(0xFFEFAE55)
    val bow = if (dark) Color(0xFFC98A6D) else Color(0xFFC8604F)

    // A soft cloud drifting behind the kite.
    val cloud = Path().apply {
        moveTo(w * 0.08f, h * 0.32f)
        quadraticTo(w * 0.08f, h * 0.245f, w * 0.155f, h * 0.255f)
        quadraticTo(w * 0.195f, h * 0.185f, w * 0.255f, h * 0.255f)
        quadraticTo(w * 0.325f, h * 0.275f, w * 0.305f, h * 0.325f)
        close()
    }
    drawPath(cloud, Color.White.copy(alpha = 0.8f), style = Stroke(width = stroke * 0.45f))

    // ── The kite: a diamond with bowed edges, a cross spar and a spine.
    val kx = w * 0.60f; val ky = h * 0.28f
    val kw = w * 0.185f; val kh = h * 0.225f
    val kiteTail = ky + kh * 1.20f
    val kite = Path().apply {
        moveTo(kx, ky - kh)
        quadraticTo(kx + kw * 0.56f, ky - kh * 0.56f, kx + kw, ky)
        quadraticTo(kx + kw * 0.52f, ky + kh * 0.86f, kx, kiteTail)
        quadraticTo(kx - kw * 0.52f, ky + kh * 0.86f, kx - kw, ky)
        quadraticTo(kx - kw * 0.56f, ky - kh * 0.56f, kx, ky - kh)
        close()
    }
    drawPath(kite, body.copy(alpha = 0.9f))
    drawPath(kite, Color.White.copy(alpha = 0.94f), style = Stroke(width = stroke * 0.6f))
    drawLine(Color.White.copy(alpha = 0.9f), Offset(kx - kw, ky), Offset(kx + kw, ky), strokeWidth = stroke * 0.4f)
    drawLine(Color.White.copy(alpha = 0.9f), Offset(kx, ky - kh), Offset(kx, kiteTail), strokeWidth = stroke * 0.4f)
    // Two sail bands echoing the spars.
    drawLine(Color.White.copy(alpha = 0.45f), Offset(kx - kw * 0.52f, ky - kh * 0.30f), Offset(kx + kw * 0.52f, ky - kh * 0.30f), strokeWidth = stroke * 0.3f)
    drawLine(Color.White.copy(alpha = 0.45f), Offset(kx - kw * 0.40f, ky + kh * 0.45f), Offset(kx + kw * 0.40f, ky + kh * 0.45f), strokeWidth = stroke * 0.3f)

    // ── The tail: a real cubic curve with three bows tied along it.
    val p0 = Offset(kx, kiteTail)
    val p1 = Offset(kx - w * 0.10f, h * 0.52f)
    val p2 = Offset(kx - w * 0.20f, h * 0.66f)
    val p3 = Offset(kx - w * 0.10f, h * 0.82f)
    fun tailAt(t: Float): Offset {
        val mt = 1f - t
        val a = mt * mt * mt; val b = 3f * mt * mt * t; val c = 3f * mt * t * t; val d = t * t * t
        return Offset(a * p0.x + b * p1.x + c * p2.x + d * p3.x, a * p0.y + b * p1.y + c * p2.y + d * p3.y)
    }
    val tail = Path().apply {
        moveTo(p0.x, p0.y)
        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
    }
    drawPath(tail, Color.White.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.35f, cap = StrokeCap.Round))
    listOf(0.26f, 0.54f, 0.80f).forEach { t ->
        val c = tailAt(t)
        val br = w * 0.016f
        drawCircle(bow.copy(alpha = 0.75f), radius = br, center = c)
        drawCircle(Color.White.copy(alpha = 0.9f), radius = br, center = c, style = Stroke(width = stroke * 0.35f))
        val l1 = tailAt(t - 0.05f); val l2 = tailAt(t + 0.05f)
        drawLine(Color.White.copy(alpha = 0.65f), l1, l2, strokeWidth = stroke * 0.3f)
    }
    // The small hand holding the line at the end of the tail.
    drawCircle(ink.copy(alpha = 0.35f), radius = w * 0.012f, center = p3)
    drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.012f, center = p3, style = Stroke(width = stroke * 0.35f))
    sparkle(w * 0.90f, h * 0.72f, h * 0.038f, alpha = 0.55f)
}

/** COLLECTION DETAIL empty state — the app-wide doodle scene + a primary
 *  "+ Add a capture" pill (see CurioDoodleEmptyState). */
@Composable
fun V2CollectionEmptyState(onAdd: () -> Unit) {
    CurioDoodleEmptyState(
        headline = "Nothing here yet",
        subtext = "Save something from your discoveries, or add a capture here.",
        ctaGlyph = CurioIcons.Add,
        ctaLabel = "Add a capture",
        onCtaClick = onAdd
    )
}

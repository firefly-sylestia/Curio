package com.curio.app.features.cabinet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = if (hasMenu) ({ moreOpen = true }) else null),
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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { moreOpen = true },
                                    onLongClick = { moreOpen = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                name = CurioIcons.MoreVert,
                                contentDescription = "Rename or delete",
                                tint = ink.copy(alpha = 0.66f),
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
// foot strip to the create-collection sheet's preview tiles. v3xx-pass-2
// redrew the disliked scenes in the flask doodle family (pastel fills +
// white outlines + drawn ✦ twinkles — the old fixed-dp Text sparkles
// misplaced on narrow cards and vanished in the sheet previews):
// Favorites is a heart CONSTELLATION, Curiying now a curved open book,
// Saved entries a fanned polaroid stack, Completed a curved snow-capped
// summit, Notes a spiral-bound notebook, plus a night-sky star cluster,
// layered mountains and the four MINIMAL scenes. Want to Read (spines)
// and Personal (window) were left exactly as they were.
// ────────────────────────────────────────────────────────────────────────

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

/** A 4-point twinkle star centred at (cx, cy) — the ✦ mark drawn as a
 *  path so it scales with the scene (fixed-dp Text sparkles misplaced on
 *  narrow cards and vanished in the sheet previews). */
private fun fourStar(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0 until 8) {
        val ang = -Math.PI / 2.0 + i * Math.PI / 4.0
        val rad = if (i % 2 == 0) r else r * 0.30f
        val x = cx + (rad * kotlin.math.cos(ang)).toFloat()
        val y = cy + (rad * kotlin.math.sin(ang)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
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

/** CUSTOM — a night-sky star cluster: one big golden star with a comet
 *  trail, a small echo star and two twinkles — pastel fills + white
 *  outlines (doodle family). */
@Composable
private fun BoxScope.StarArt(dark: Boolean) {
    val star = if (dark) Color(0xFFC79A45) else Color(0xFFE8A33D)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val u = minOf(w, h)
        // Big star — centre-right, soft fill + clean outline.
        val cx = w * 0.70f; val cy = h * 0.50f
        val outer = fiveStar(cx, cy, u * 0.34f)
        drawPath(outer, star.copy(alpha = 0.55f))
        drawPath(outer, Color.White.copy(alpha = 0.9f), style = Stroke(width = stroke))
        // Comet trail — a thin curved line rising off the big star.
        val comet = Path().apply {
            moveTo(cx + u * 0.20f, cy - u * 0.18f)
            quadraticTo(cx + u * 0.44f, cy - u * 0.42f, cx + u * 0.66f, cy - u * 0.30f)
        }
        drawPath(comet, Color.White.copy(alpha = 0.55f), style = Stroke(width = stroke * 0.6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = u * 0.030f, center = androidx.compose.ui.geometry.Offset(cx + u * 0.66f, cy - u * 0.30f))
        // Small echo star, bottom-left.
        val small = fiveStar(w * 0.24f, h * 0.62f, u * 0.16f)
        drawPath(small, star.copy(alpha = 0.45f))
        drawPath(small, Color.White.copy(alpha = 0.8f), style = Stroke(width = stroke * 0.7f))
        // Twinkles + a lone dot.
        drawCircle(Color.White.copy(alpha = 0.85f), radius = u * 0.028f, center = androidx.compose.ui.geometry.Offset(w * 0.33f, h * 0.30f))
        drawPath(fourStar(w * 0.90f, h * 0.22f, u * 0.050f), Color.White.copy(alpha = 0.9f))
        drawPath(fourStar(w * 0.12f, h * 0.18f, u * 0.036f), Color.White.copy(alpha = 0.7f))
    }
}

/** FAVORITES — the heart drawn as a CONSTELLATION: rose twinkle-stars
 *  joined by fine connecting lines (the favorites symbol as a star map),
 *  with two loose stars — the doodle family's white strokes + pastel
 *  fills. */
@Composable
private fun BoxScope.ConstellationArt(dark: Boolean) {
    val heart = if (dark) Color(0xFFC95E7E) else Color(0xFFE05A7C)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val u = minOf(w, h)
        // Heart nodes, as fractions of the canvas.
        val xs = floatArrayOf(0.30f, 0.42f, 0.50f, 0.58f, 0.70f, 0.71f, 0.50f, 0.29f)
        val ys = floatArrayOf(0.36f, 0.24f, 0.34f, 0.24f, 0.36f, 0.56f, 0.80f, 0.56f)
        val pts = List(8) { androidx.compose.ui.geometry.Offset(w * xs[it], h * ys[it]) }
        // Connecting lines — thin white, tracing the heart outline.
        for (i in pts.indices) {
            val a = pts[i]; val b = pts[(i + 1) % pts.size]
            drawLine(Color.White.copy(alpha = 0.45f), a, b, strokeWidth = stroke * 0.55f)
        }
        // Rose twinkle-stars at every node.
        pts.forEach { p ->
            drawPath(fourStar(p.x, p.y, u * 0.055f), heart.copy(alpha = 0.75f))
            drawPath(fourStar(p.x, p.y, u * 0.055f), Color.White.copy(alpha = 0.9f), style = Stroke(width = stroke * 0.5f))
        }
        // Two loose stars for the night-sky feel.
        drawPath(fourStar(w * 0.86f, h * 0.22f, u * 0.040f), Color.White.copy(alpha = 0.9f))
        drawCircle(Color.White.copy(alpha = 0.8f), radius = u * 0.022f, center = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.20f))
        drawCircle(Color.White.copy(alpha = 0.7f), radius = u * 0.016f, center = androidx.compose.ui.geometry.Offset(w * 0.93f, h * 0.40f))
    }
}

/** CURIYING NOW — a hand-drawn open book with gently CURVED pages, page
 *  lines, a knotted ribbon bookmark, a grounding shadow and a twinkle
 *  above the spine — pastel fills + white outlines (doodle family). */
@Composable
private fun BoxScope.ReadingArt(dark: Boolean) {
    val page = if (dark) Color(0xFFE8DCC8) else Color(0xFFF9F2E6)
    val line = if (dark) Color(0xFF9DB58F) else Color(0xFF769070)
    val ribbon = if (dark) Color(0xFFC98A6D) else Color(0xFFC07A5A)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val u = minOf(w, h)
        val cxm = w * 0.50f          // spine
        val baseY = h * 0.96f
        val topY = h * 0.30f         // page tops
        val half = w * 0.40f         // one page width
        // Grounding shadow — a soft ellipse under the book.
        drawOval(
            Color.White.copy(alpha = 0.28f),
            topLeft = androidx.compose.ui.geometry.Offset(cxm - half * 0.92f, baseY - h * 0.035f),
            size = androidx.compose.ui.geometry.Size(half * 1.84f, h * 0.07f)
        )
        // Left page — curved outer edge.
        val left = Path().apply {
            moveTo(cxm, baseY)
            lineTo(cxm, topY + h * 0.12f)
            quadraticTo(cxm - half * 0.22f, topY - h * 0.04f, cxm - half * 0.98f, topY + h * 0.02f)
            quadraticTo(cxm - half * 1.06f, h * 0.52f, cxm - half * 0.94f, baseY)
            close()
        }
        drawPath(left, page.copy(alpha = 0.92f))
        drawPath(left, Color.White.copy(alpha = 0.9f), style = Stroke(width = stroke * 0.7f))
        // Right page (mirrored, slightly shaded).
        val right = Path().apply {
            moveTo(cxm, baseY)
            lineTo(cxm, topY + h * 0.12f)
            quadraticTo(cxm + half * 0.22f, topY - h * 0.04f, cxm + half * 0.98f, topY + h * 0.02f)
            quadraticTo(cxm + half * 1.06f, h * 0.52f, cxm + half * 0.94f, baseY)
            close()
        }
        drawPath(right, page.copy(alpha = 0.75f))
        drawPath(right, Color.White.copy(alpha = 0.9f), style = Stroke(width = stroke * 0.7f))
        // Page lines — three per side, fanning out down the page.
        for (i in 1..3) {
            val t = i / 4f
            val ly = topY + h * 0.12f + (baseY - topY - h * 0.12f) * t * 0.82f
            val spread = 0.14f + t * 0.16f
            drawLine(line.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(cxm - half * spread - half * 0.10f, ly), androidx.compose.ui.geometry.Offset(cxm - half * 0.08f, ly), strokeWidth = 0.9f)
            drawLine(line.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(cxm + half * 0.08f, ly), androidx.compose.ui.geometry.Offset(cxm + half * spread + half * 0.10f, ly), strokeWidth = 0.9f)
        }
        // Spine — a clean white join.
        drawLine(Color.White.copy(alpha = 0.85f), androidx.compose.ui.geometry.Offset(cxm, topY + h * 0.12f), androidx.compose.ui.geometry.Offset(cxm, baseY), strokeWidth = 1.4f)
        // Knotted ribbon bookmark hanging from the spine top.
        val ribbonPath = Path().apply {
            moveTo(cxm - w * 0.030f, topY + h * 0.02f)
            lineTo(cxm + w * 0.030f, topY + h * 0.02f)
            lineTo(cxm + w * 0.030f, topY + h * 0.20f)
            lineTo(cxm, topY + h * 0.14f)
            lineTo(cxm - w * 0.030f, topY + h * 0.20f)
            close()
        }
        drawPath(ribbonPath, ribbon.copy(alpha = 0.9f))
        drawPath(ribbonPath, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.5f))
        // Twinkle above the spine.
        drawPath(fourStar(cxm, h * 0.09f, u * 0.045f), Color.White.copy(alpha = 0.9f))
    }
}

/** WANT TO READ — three hand-drawn book spines standing side by side on
 *  the bottom edge: pastel fills + white outlines (doodle family), title
 *  ticks and a ribbon on the tallest spine. */
@Composable
private fun BoxScope.BooksArt(dark: Boolean) {
    val spines = if (dark) listOf(0xFF7A5C4C, 0xFF9A7560, 0xFFC09379)
    else listOf(0xFF9C7562, 0xFFB98D79, 0xFFD39F91)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val baseY = h * 0.97f
        val cx = listOf(0.18f, 0.50f, 0.80f)
        val bw = listOf(0.20f, 0.22f, 0.17f)
        val bh = listOf(0.46f, 0.60f, 0.36f)
        spines.forEachIndexed { i, c ->
            val x = w * cx[i] - w * bw[i] / 2f
            val y = baseY - h * bh[i]
            val spine = Path().apply {
                moveTo(x, baseY)
                lineTo(x, y)
                lineTo(x + w * bw[i], y)
                lineTo(x + w * bw[i], baseY)
                close()
            }
            drawPath(spine, Color(c).copy(alpha = 0.85f))
            drawPath(spine, Color.White.copy(alpha = 0.9f), style = Stroke(width = stroke * 0.65f))
            // title ticks — two short white lines near the top of each spine
            val t1 = y + h * bh[i] * 0.16f
            val t2 = y + h * bh[i] * 0.25f
            drawLine(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.24f, t1), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.76f, t1), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.24f, t2), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.58f, t2), strokeWidth = 1f)
        }
        // a small ribbon on the tallest spine — fill + outline
        val ribbonPath = Path().apply {
            val x = w * 0.50f - w * 0.22f / 2f
            moveTo(x + w * 0.22f * 0.42f, h * 0.97f - h * 0.60f)
            lineTo(x + w * 0.22f * 0.58f, h * 0.97f - h * 0.60f)
            lineTo(x + w * 0.22f * 0.58f, h * 0.97f - h * 0.60f + h * 0.16f)
            lineTo(x + w * 0.22f * 0.50f, h * 0.97f - h * 0.60f + h * 0.11f)
            lineTo(x + w * 0.22f * 0.42f, h * 0.97f - h * 0.60f + h * 0.16f)
            close()
        }
        drawPath(ribbonPath, (if (dark) Color(0xFFC98A6D) else Color(0xFFC07A5A)).copy(alpha = 0.9f))
        drawPath(ribbonPath, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.5f))
    }
    Text(
        text = "✦",
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
        color = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.offset(x = 82.dp, y = 2.dp)
    )
    Text(
        text = "✧",
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 9.sp),
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.offset(x = 6.dp, y = 44.dp)
    )
}

/** CUSTOM — hand-drawn layered mountains with soft CURVED peaks, a
 *  snow-capped summit, doodle pine trees and flower dots — pastel fills +
 *  white outlines (doodle family). */
@Composable
private fun BoxScope.MountainArt(dark: Boolean) {
    val hills = if (dark) Color(0xFF5E7E6C) else Color(0xFF8FB4A0)
    val farHill = if (dark) Color(0xFF4A6A5C) else Color(0xFFA9C7B4)
    val pines = if (dark) Color(0xFF3C5140) else Color(0xFF7DA48E)
    val flowers = if (dark) Color(0xFFE9F2E4) else Color(0xFFFDFEFC)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        // Low sun, high in the sky.
        val sunR = w * 0.085f
        val sunC = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.20f)
        drawCircle(Color(0xFFF2B36B).copy(alpha = 0.55f), radius = sunR, center = sunC)
        drawCircle(Color.White.copy(alpha = 0.85f), radius = sunR, center = sunC, style = Stroke(width = stroke * 0.6f))
        // Far swell — one smooth rise past both edges.
        val far = Path().apply {
            moveTo(-w * 0.06f, h * 0.80f)
            quadraticTo(w * 0.30f, h * 0.30f, w * 0.62f, h * 0.66f)
            quadraticTo(w * 0.90f, h * 0.44f, w * 1.06f, h * 0.78f)
            lineTo(w * 1.06f, h * 0.94f)
            lineTo(-w * 0.06f, h * 0.94f)
            close()
        }
        drawPath(far, farHill.copy(alpha = 0.55f))
        drawPath(far, Color.White.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.5f))
        // Near peak with a snow cap.
        val near = Path().apply {
            moveTo(-w * 0.06f, h * 0.94f)
            quadraticTo(w * 0.22f, h * 0.72f, w * 0.40f, h * 0.40f)
            quadraticTo(w * 0.70f, h * 0.72f, w * 1.06f, h * 0.90f)
            close()
        }
        drawPath(near, hills.copy(alpha = 0.68f))
        drawPath(near, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.6f))
        // Snow cap on the summit.
        val cap = Path().apply {
            moveTo(w * 0.36f, h * 0.48f)
            quadraticTo(w * 0.40f, h * 0.40f, w * 0.44f, h * 0.50f)
            quadraticTo(w * 0.41f, h * 0.52f, w * 0.36f, h * 0.48f)
            close()
        }
        drawPath(cap, Color.White.copy(alpha = 0.85f))
        // Doodle pine trees at the foot.
        listOf(0.16f, 0.52f, 0.84f).forEach { fx ->
            val px = w * fx; val py = h * 0.94f
            val tree = Path().apply {
                moveTo(px, py - h * 0.20f)
                lineTo(px + w * 0.05f, py - h * 0.10f)
                lineTo(px + w * 0.03f, py - h * 0.10f)
                lineTo(px + w * 0.07f, py - h * 0.02f)
                lineTo(px + w * 0.03f, py - h * 0.02f)
                lineTo(px, py)
                close()
            }
            drawPath(tree, pines.copy(alpha = 0.7f))
            drawPath(tree, Color.White.copy(alpha = 0.8f), style = Stroke(width = stroke * 0.4f))
        }
        // Flower dots.
        listOf(0.26f, 0.68f).forEachIndexed { i, fx ->
            val cx2 = w * fx
            val cy2 = h * (0.84f + 0.02f * i)
            drawCircle(flowers.copy(alpha = 0.9f), radius = 2.8f, center = androidx.compose.ui.geometry.Offset(cx2, cy2))
            drawCircle(Color.White.copy(alpha = 0.7f), radius = 2.8f, center = androidx.compose.ui.geometry.Offset(cx2, cy2), style = Stroke(width = 1f))
        }
    }
}

/** COMPLETED — a hand-drawn summit scene: a rising sun, two smooth CURVED
 *  ridgelines (the near one snow-capped), a planted flag and two birds —
 *  pastel fills + white outlines (doodle family). The ridges run a little
 *  PAST the canvas edges so they read as continuing mountains. */
@Composable
private fun BoxScope.PeakArt(dark: Boolean) {
    val far = if (dark) Color(0xFF4A6A5C) else Color(0xFFA9C7B4)
    val near = if (dark) Color(0xFF5E7E6C) else Color(0xFF8FB4A0)
    val ground = if (dark) Color(0xFF3C5140) else Color(0xFF7DA48E)
    val sun = if (dark) Color(0xFFF4C768) else Color(0xFFF2B36B)
    val flag = if (dark) Color(0xFFE9F2E4) else Color(0xFFFDFEFC)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        // Rising sun — high enough to clear every ridge.
        val sunR = w * 0.10f
        val sunC = androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.26f)
        drawCircle(sun.copy(alpha = 0.6f), radius = sunR, center = sunC)
        drawCircle(Color.White.copy(alpha = 0.9f), radius = sunR, center = sunC, style = Stroke(width = stroke * 0.6f))
        // Far ridge — one smooth swell past both edges.
        val farPath = Path().apply {
            moveTo(-w * 0.10f, h * 0.78f)
            quadraticTo(w * 0.16f, h * 0.18f, w * 0.42f, h * 0.52f)
            quadraticTo(w * 0.58f, h * 0.70f, w * 1.10f, h * 0.34f)
            lineTo(w * 1.10f, h * 0.95f)
            lineTo(-w * 0.10f, h * 0.95f)
            close()
        }
        drawPath(farPath, far.copy(alpha = 0.5f))
        drawPath(farPath, Color.White.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.5f))
        // Near ridge — a second swell with a flat summit for the flag.
        val nearPath = Path().apply {
            moveTo(-w * 0.10f, h * 0.90f)
            quadraticTo(w * 0.28f, h * 0.62f, w * 0.60f, h * 0.48f)
            quadraticTo(w * 0.92f, h * 0.60f, w * 1.10f, h * 0.84f)
            lineTo(w * 1.10f, h * 0.97f)
            lineTo(-w * 0.10f, h * 0.97f)
            close()
        }
        drawPath(nearPath, near.copy(alpha = 0.68f))
        drawPath(nearPath, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.6f))
        // Ground band — ties the ridges to the bottom edge.
        drawRect(ground.copy(alpha = 0.55f), topLeft = androidx.compose.ui.geometry.Offset(-w * 0.10f, h * 0.94f), size = androidx.compose.ui.geometry.Size(w * 1.2f, h * 0.08f))
        drawRect(Color.White.copy(alpha = 0.6f), topLeft = androidx.compose.ui.geometry.Offset(-w * 0.10f, h * 0.94f), size = androidx.compose.ui.geometry.Size(w * 1.2f, h * 0.08f), style = Stroke(width = stroke * 0.4f))
        // Snow cap on the near peak.
        val cap = Path().apply {
            moveTo(w * 0.50f, h * 0.50f)
            quadraticTo(w * 0.60f, h * 0.44f, w * 0.70f, h * 0.52f)
            quadraticTo(w * 0.64f, h * 0.56f, w * 0.56f, h * 0.54f)
            quadraticTo(w * 0.52f, h * 0.52f, w * 0.50f, h * 0.50f)
            close()
        }
        drawPath(cap, Color.White.copy(alpha = 0.85f))
        // Summit flag — white pole + filled pennant.
        val fx = w * 0.60f; val fy = h * 0.48f
        drawLine(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(fx, fy), androidx.compose.ui.geometry.Offset(fx, fy - h * 0.16f), strokeWidth = 1.5f)
        val pennant = Path().apply {
            moveTo(fx, fy - h * 0.16f)
            lineTo(fx + w * 0.09f, fy - h * 0.11f)
            lineTo(fx, fy - h * 0.06f)
            close()
        }
        drawPath(pennant, flag.copy(alpha = 0.9f))
        drawPath(pennant, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.5f))
        // Two birds.
        drawLine(flag.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.22f), androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.18f), strokeWidth = 1.2f)
        drawLine(flag.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.18f), androidx.compose.ui.geometry.Offset(w * 0.92f, h * 0.22f), strokeWidth = 1.2f)
        drawLine(flag.copy(alpha = 0.65f), androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.30f), androidx.compose.ui.geometry.Offset(w * 0.81f, h * 0.27f), strokeWidth = 1.1f)
        drawLine(flag.copy(alpha = 0.65f), androidx.compose.ui.geometry.Offset(w * 0.81f, h * 0.27f), androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.30f), strokeWidth = 1.1f)
    }
}

/** NOTES — a hand-drawn SPIRAL notebook: cream sheet with rings binding
 *  the top edge, ruled lines, a heart doodle and a pencil resting on it —
 *  pastel fills + white outlines (doodle family). */
@Composable
private fun BoxScope.NotesArt(dark: Boolean) {
    val paper = if (dark) Color(0xFFEAD9BE) else Color(0xFFFFFBF2)
    val paperShade = if (dark) Color(0xFFD8C8AC) else Color(0xFFEADFC8)
    val line = if (dark) Color(0xFFA08FC0) else Color(0xFF9C86C4)
    val pen = if (dark) Color(0xFFE3B7A8) else Color(0xFFB3796A)
    val eraser = if (dark) Color(0xFFE3A9A0) else Color(0xFFE58E8E)
    val lead = if (dark) Color(0xFF2E2622) else Color(0xFF4A3B35)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val nw = w * 0.46f; val nh = h * 0.62f
        val x0 = w * 0.10f; val y0 = h * 0.94f - nh
        val rad = nw * 0.045f
        // Sheet — rounded corners, cream fill + white outline.
        drawRoundRect(paper.copy(alpha = 0.92f), androidx.compose.ui.geometry.Offset(x0, y0), androidx.compose.ui.geometry.Size(nw, nh), androidx.compose.ui.geometry.CornerRadius(rad))
        drawRoundRect(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(x0, y0), androidx.compose.ui.geometry.Size(nw, nh), androidx.compose.ui.geometry.CornerRadius(rad), style = Stroke(width = stroke * 0.7f))
        // Spiral binding — five rings along the top edge.
        val holes = 5
        for (i in 0 until holes) {
            val hx = x0 + nw * (0.16f + 0.68f * i / (holes - 1))
            val hy = y0 + nh * 0.045f
            drawCircle(paperShade.copy(alpha = 0.95f), radius = nw * 0.045f, center = androidx.compose.ui.geometry.Offset(hx, hy))
            drawCircle(Color.White.copy(alpha = 0.85f), radius = nw * 0.045f, center = androidx.compose.ui.geometry.Offset(hx, hy), style = Stroke(width = stroke * 0.5f))
            drawCircle(Color.White.copy(alpha = 0.9f), radius = nw * 0.014f, center = androidx.compose.ui.geometry.Offset(hx, hy))
        }
        // Ruled lines.
        for (i in 1..4) {
            val ly = y0 + nh * 0.22f + i * nh * 0.15f
            drawLine(line.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(x0 + nw * 0.10f, ly), androidx.compose.ui.geometry.Offset(x0 + nw * 0.90f, ly), strokeWidth = 0.9f)
        }
        // A little heart doodle at the foot of the sheet.
        val hx = x0 + nw * 0.30f; val hy = y0 + nh * 0.82f; val hs = nw * 0.11f
        val heartPath = Path().apply {
            moveTo(hx, hy + hs * 0.60f)
            cubicTo(hx - hs * 0.92f, hy - hs * 0.10f, hx - hs * 0.46f, hy - hs * 0.80f, hx, hy - hs * 0.26f)
            cubicTo(hx + hs * 0.46f, hy - hs * 0.80f, hx + hs * 0.92f, hy - hs * 0.10f, hx, hy + hs * 0.60f)
            close()
        }
        drawPath(heartPath, pen.copy(alpha = 0.75f))
        drawPath(heartPath, Color.White.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.4f))
        // Pencil resting diagonally at the bottom-right — thick body with a
        // white highlight, pink eraser cap and dark lead tip.
        val p0x = w * 0.62f; val p0y = h * 0.34f
        val p1x = w * 0.955f; val p1y = h * 0.88f
        drawLine(pen.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(p0x, p0y), androidx.compose.ui.geometry.Offset(p1x, p1y), strokeWidth = w * 0.030f)
        drawLine(Color.White.copy(alpha = 0.7f), androidx.compose.ui.geometry.Offset(p0x, p0y), androidx.compose.ui.geometry.Offset(p1x, p1y), strokeWidth = stroke * 0.5f)
        drawCircle(eraser, radius = w * 0.017f, center = androidx.compose.ui.geometry.Offset(p0x, p0y))
        drawCircle(lead, radius = w * 0.011f, center = androidx.compose.ui.geometry.Offset(p1x, p1y))
        // Twinkle above the sheet.
        drawPath(fourStar(w * 0.94f, h * 0.12f, w * 0.032f), Color.White.copy(alpha = 0.9f))
    }
}

/** PERSONAL — a hand-drawn window with a moon and a plant on the sill:
 *  pastel sky fill + white frame outlines (doodle family). */
@Composable
private fun BoxScope.WindowArt(dark: Boolean) {
    val sky = if (dark) Brush.verticalGradient(listOf(Color(0xFF3E4A5E), Color(0xFF5E7F6E)))
    else Brush.verticalGradient(listOf(Color(0xFFF3CFA8), Color(0xFFA9B9A2)))
    val frame = if (dark) Color(0xFF3E2E28) else Color(0xFFA87F6B)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val ww = w * 0.40f; val wh = h * 0.54f
        val x0 = w * 0.70f - ww / 2f; val y0 = h * 0.92f - wh
        // window pane — sky fill + white outline
        drawRoundRect(sky, androidx.compose.ui.geometry.Offset(x0, y0), androidx.compose.ui.geometry.Size(ww, wh), androidx.compose.ui.geometry.CornerRadius(ww * 0.06f))
        drawRoundRect(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(x0, y0), androidx.compose.ui.geometry.Size(ww, wh), androidx.compose.ui.geometry.CornerRadius(ww * 0.06f), style = Stroke(width = stroke * 0.8f))
        // moon — fill + outline
        val moonR = ww * 0.13f
        val moonC = androidx.compose.ui.geometry.Offset(x0 + ww * 0.72f, y0 + wh * 0.28f)
        drawCircle(Color(0xFFF8E8C8), radius = moonR, center = moonC)
        drawCircle(Color.White.copy(alpha = 0.9f), radius = moonR, center = moonC, style = Stroke(width = stroke * 0.5f))
        // window muntins — white
        drawLine(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(x0 + ww / 2f, y0), androidx.compose.ui.geometry.Offset(x0 + ww / 2f, y0 + wh), strokeWidth = stroke * 0.55f)
        drawLine(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(x0, y0 + wh / 2f), androidx.compose.ui.geometry.Offset(x0 + ww, y0 + wh / 2f), strokeWidth = stroke * 0.55f)
        // plant on the sill — stem + leaf circles + pot, fill + outline
        val leaf = if (dark) Color(0xFF9DB58F) else Color(0xFF71896A)
        drawLine(leaf.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.90f), androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.64f), strokeWidth = 1.6f)
        drawCircle(leaf.copy(alpha = 0.85f), radius = w * 0.024f, center = androidx.compose.ui.geometry.Offset(w * 0.13f, h * 0.62f))
        drawCircle(Color.White.copy(alpha = 0.8f), radius = w * 0.024f, center = androidx.compose.ui.geometry.Offset(w * 0.13f, h * 0.62f), style = Stroke(width = stroke * 0.4f))
        drawCircle(leaf.copy(alpha = 0.85f), radius = w * 0.019f, center = androidx.compose.ui.geometry.Offset(w * 0.19f, h * 0.64f))
        drawCircle(Color.White.copy(alpha = 0.8f), radius = w * 0.019f, center = androidx.compose.ui.geometry.Offset(w * 0.19f, h * 0.64f), style = Stroke(width = stroke * 0.4f))
        val pot = Path().apply {
            moveTo(w * 0.10f, h * 0.92f)
            lineTo(w * 0.22f, h * 0.92f)
            lineTo(w * 0.19f, h * 0.82f)
            lineTo(w * 0.13f, h * 0.82f)
            close()
        }
        drawPath(pot, frame.copy(alpha = 0.7f))
        drawPath(pot, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.5f))
    }
    Text(
        text = "✦",
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
        color = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.offset(x = 82.dp, y = 2.dp)
    )
    Text(
        text = "✧",
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 9.sp),
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.offset(x = 6.dp, y = 44.dp)
    )
}

/** SAVED ENTRIES — a fanned stack of three polaroid prints: white paper
 *  frames holding a sun-and-ridge photo, a heart photo and a star photo,
 *  with tape on the front print — pastel fills + white outlines (doodle
 *  family). */
@Composable
private fun BoxScope.PhotosArt(dark: Boolean) {
    val photos = if (dark) listOf(Color(0xFF46657A), Color(0xFF5A7B8C), Color(0xFF6F92A3))
    else listOf(Color(0xFF9CC3D9), Color(0xFF7FB0CE), Color(0xFF5E9CC2))
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val tw = w * 0.30f; val th = h * 0.46f
        val cx = w * 0.72f; val cy = h * 0.80f
        // Back print — a sun-and-ridge photo, tilted left.
        polaroidPrint(cx - tw * 0.26f, cy - th, tw, th, -0.14f, photos[0], stroke)
        drawCircle(Color(0xFFF4C768).copy(alpha = 0.9f), radius = tw * 0.10f,
            center = rotPoint(cx - tw * 0.26f, cy - th, -0.14f, tw * 0.60f, th * 0.30f))
        drawLine(Color.White.copy(alpha = 0.8f),
            rotPoint(cx - tw * 0.26f, cy - th, -0.14f, tw * 0.20f, th * 0.62f),
            rotPoint(cx - tw * 0.26f, cy - th, -0.14f, tw * 0.80f, th * 0.52f),
            strokeWidth = stroke * 0.5f)
        // Middle print — a heart photo.
        polaroidPrint(cx - tw * 0.10f, cy - th * 0.94f, tw, th, -0.04f, photos[1], stroke)
        val hm = rotPoint(cx - tw * 0.10f, cy - th * 0.94f, -0.04f, tw * 0.52f, th * 0.34f)
        val hs = tw * 0.14f
        val heartPath = Path().apply {
            moveTo(hm.x, hm.y + hs * 0.55f)
            cubicTo(hm.x - hs * 0.9f, hm.y - hs * 0.08f, hm.x - hs * 0.45f, hm.y - hs * 0.78f, hm.x, hm.y - hs * 0.24f)
            cubicTo(hm.x + hs * 0.45f, hm.y - hs * 0.78f, hm.x + hs * 0.9f, hm.y - hs * 0.08f, hm.x, hm.y + hs * 0.55f)
            close()
        }
        drawPath(heartPath, Color(0xFFE8A3A3).copy(alpha = 0.9f))
        drawPath(heartPath, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.4f))
        // Front print — a star photo, tilted right, with tape.
        polaroidPrint(cx + tw * 0.08f, cy - th * 0.90f, tw, th, 0.10f, photos[2], stroke)
        val sm = rotPoint(cx + tw * 0.08f, cy - th * 0.90f, 0.10f, tw * 0.55f, th * 0.34f)
        drawPath(fourStar(sm.x, sm.y, tw * 0.16f), Color.White.copy(alpha = 0.9f))
        // Tape strips on the front print's top edge.
        val tape1 = rotPoint(cx + tw * 0.08f, cy - th * 0.90f, 0.10f, tw * 0.12f, -th * 0.02f)
        val tape2 = rotPoint(cx + tw * 0.08f, cy - th * 0.90f, 0.10f, tw * 0.66f, -th * 0.02f)
        listOf(tape1, tape2).forEach { tp ->
            drawPath(rotRect(tp.x, tp.y, tw * 0.22f, th * 0.10f, 0.10f), Color.White.copy(alpha = 0.75f))
            drawPath(rotRect(tp.x, tp.y, tw * 0.22f, th * 0.10f, 0.10f), Color.White.copy(alpha = 0.6f), style = Stroke(width = stroke * 0.35f))
        }
    }
}

/** MINIMAL — sun arc + ray ticks + horizon + lone dot (the Minimal share
 *  card's sparse language), white-outlined in the doodle family. */
@Composable
private fun BoxScope.MinimalSunArt(dark: Boolean) {
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val cx = w * 0.68f; val cy = h * 0.34f; val r = w * 0.17f
        drawArc(ink.copy(alpha = 0.5f), startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f))
        drawArc(Color.White.copy(alpha = 0.85f), startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
            style = Stroke(width = stroke * 0.6f))
        // Ray ticks above the arc.
        listOf(-0.6f, 0f, 0.6f).forEach { off ->
            val rx = cx + r * off
            drawLine(Color.White.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(rx, cy - r - h * 0.04f), androidx.compose.ui.geometry.Offset(rx, cy - r - h * 0.09f), strokeWidth = stroke * 0.5f)
        }
        drawLine(ink.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.88f), androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.88f), strokeWidth = w * 0.012f)
        drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.02f, center = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.62f))
        drawPath(fourStar(w * 0.24f, h * 0.22f, w * 0.028f), Color.White.copy(alpha = 0.85f))
    }
}

/** MINIMAL — two rings with a drifting dot inside + a baseline + a lone
 *  twinkle, white-outlined doodle style. */
@Composable
private fun BoxScope.MinimalRingsArt(dark: Boolean) {
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val cx = w * 0.68f; val cy = h * 0.50f
        drawCircle(ink.copy(alpha = 0.20f), radius = w * 0.17f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.8f), radius = w * 0.17f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(width = stroke * 0.5f))
        drawCircle(ink.copy(alpha = 0.40f), radius = w * 0.10f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.10f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(width = stroke * 0.6f))
        // A small filled dot drifting inside the inner ring.
        drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.018f, center = androidx.compose.ui.geometry.Offset(cx + w * 0.04f, cy - w * 0.06f))
        drawLine(ink.copy(alpha = 0.40f), androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.88f), androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.88f), strokeWidth = w * 0.008f)
        drawPath(fourStar(w * 0.28f, h * 0.26f, w * 0.030f), Color.White.copy(alpha = 0.9f))
    }
}

/** MINIMAL — one soft wave with a small ripple under it + a dot, a lone
 *  twinkle and a baseline, white-outlined doodle style. */
@Composable
private fun BoxScope.MinimalWaveArt(dark: Boolean) {
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        val wave = Path().apply {
            moveTo(0f, h * 0.58f)
            cubicTo(w * 0.20f, h * 0.28f, w * 0.34f, h * 0.84f, w * 0.52f, h * 0.60f)
            cubicTo(w * 0.66f, h * 0.42f, w * 0.80f, h * 0.78f, w, h * 0.50f)
        }
        drawPath(wave, ink.copy(alpha = 0.35f), style = Stroke(width = w * 0.030f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(wave, Color.White.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        // Small ripple under the wave's right hump.
        val ripple = Path().apply {
            moveTo(w * 0.56f, h * 0.76f)
            cubicTo(w * 0.62f, h * 0.68f, w * 0.70f, h * 0.68f, w * 0.76f, h * 0.76f)
        }
        drawPath(ripple, Color.White.copy(alpha = 0.5f), style = Stroke(width = stroke * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.018f, center = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.22f))
        drawPath(fourStar(w * 0.82f, h * 0.16f, w * 0.028f), Color.White.copy(alpha = 0.85f))
        drawLine(ink.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.90f), androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.90f), strokeWidth = w * 0.008f)
    }
}

/** MINIMAL — a loose scatter of dots drifting along a gentle arc, with a
 *  lone twinkle — white-dotted in the doodle family. */
@Composable
private fun BoxScope.MinimalDotsArt(dark: Boolean) {
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val stroke = 1.8.dp.toPx()
        listOf(0.20f to 0.62f, 0.34f to 0.44f, 0.50f to 0.36f, 0.66f to 0.40f, 0.80f to 0.54f).forEachIndexed { i, (fx, fy) ->
            val r = if (i % 2 == 0) w * 0.022f else w * 0.014f
            val c = androidx.compose.ui.geometry.Offset(w * fx, h * fy)
            drawCircle(ink.copy(alpha = 0.45f), radius = r, center = c)
            drawCircle(Color.White.copy(alpha = 0.9f), radius = r, center = c, style = Stroke(width = if (i % 2 == 0) stroke * 0.5f else stroke * 0.35f))
        }
        drawPath(fourStar(w * 0.90f, h * 0.22f, w * 0.030f), Color.White.copy(alpha = 0.85f))
        drawCircle(Color.White.copy(alpha = 0.6f), radius = w * 0.010f, center = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.30f))
    }
}

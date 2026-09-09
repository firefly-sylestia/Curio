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
// Shelf art — v3xx RESPONSIVE CARD SCENES: every scene is drawn with
// proportional Canvas geometry, so the SAME art scales from the card's
// foot strip to the create-collection sheet's preview tiles. The classic
// favourites (star, open book, spines, mountain, notes, window, photos)
// were refined with more detail, Completed got a FULL redesign (PEAK:
// sun-arc summit + planted flag), and the four MINIMAL_* scenes borrow
// the Minimal share card's sparse line language for plenty of variety.
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

private fun mountainPath(w: Float, h: Float): Path = Path().apply {
    moveTo(0f, h)
    lineTo(w * 0.06f, h * 0.42f)
    lineTo(w * 0.22f, h * 0.70f)
    lineTo(w * 0.36f, h * 0.22f)
    lineTo(w * 0.56f, h * 0.80f)
    lineTo(w * 0.74f, h * 0.50f)
    lineTo(w * 0.94f, h * 0.30f)
    lineTo(w, h * 0.42f)
    lineTo(w, h)
    close()
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

/** FAVORITES — a glowing golden star with a soft halo + scattered star
 *  dust (richer than the old lone star over a mountain). */
@Composable
private fun BoxScope.StarArt(dark: Boolean) {
    val star = if (dark) Color(0xFFF4C768) else Color(0xFFE8A33D)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val cx = w * 0.78f; val cy = h * 0.30f
        drawCircle(star.copy(alpha = 0.10f), radius = w * 0.46f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        drawCircle(star.copy(alpha = 0.16f), radius = w * 0.28f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        drawPath(fiveStar(cx, cy, w * 0.18f), color = star)
        val dust = listOf(0.08f to 0.16f, 0.24f to 0.62f, 0.55f to 0.82f, 0.32f to 0.36f, 0.90f to 0.74f, 0.05f to 0.88f, 0.62f to 0.14f)
        dust.forEachIndexed { i, (fx, fy) ->
            val r = if (i % 3 == 0) w * 0.022f else w * 0.013f
            drawCircle(star.copy(alpha = 0.55f), radius = r, center = androidx.compose.ui.geometry.Offset(w * fx, h * fy))
        }
    }
}

/** FAVORITES — a warm heart (the universal favorites symbol) with a soft
 *  halo and a few sparkles, sitting on the bottom edge. Instantly reads as
 *  "favorites" — the old star-map constellation read as random dots. */
@Composable
private fun BoxScope.ConstellationArt(dark: Boolean) {
    val heart = if (dark) Color(0xFFE8A3B4) else Color(0xFFE05A7C)
    val spark = if (dark) Color(0xFFF4C768) else Color(0xFFE8A33D)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val hx = w * 0.62f; val hy = h * 0.66f; val hs = w * 0.30f
        // halo
        drawCircle(heart.copy(alpha = 0.12f), radius = hs * 1.25f, center = androidx.compose.ui.geometry.Offset(hx, hy - hs * 0.15f))
        // heart — classic two-lobe shape with a pointed tip, bottom-anchored
        drawPath(Path().apply {
            moveTo(hx, hy + hs * 0.62f)
            cubicTo(hx - hs * 0.95f, hy - hs * 0.10f, hx - hs * 0.48f, hy - hs * 0.82f, hx, hy - hs * 0.28f)
            cubicTo(hx + hs * 0.48f, hy - hs * 0.82f, hx + hs * 0.95f, hy - hs * 0.10f, hx, hy + hs * 0.62f)
            close()
        }, color = heart)
        // sparkles
        drawCircle(spark, radius = w * 0.022f, center = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.30f))
        drawCircle(spark.copy(alpha = 0.55f), radius = w * 0.013f, center = androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.20f))
        drawCircle(spark.copy(alpha = 0.45f), radius = w * 0.011f, center = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.52f))
    }
}

/** CURIYING NOW — a clean open book: two pages joined at the spine, page
 *  lines, a ribbon bookmark — everything CONNECTED and sitting on the
 *  bottom edge (the old steam curl + floating bookmark read as random). */
@Composable
private fun BoxScope.ReadingArt(dark: Boolean) {
    val page = if (dark) Color(0xFFF2E8DA) else Color(0xFFF9F2E6)
    val line = if (dark) Color(0xFF9DB58F) else Color(0xFF769070)
    val spine = if (dark) Color(0xFF7C9688) else Color(0xFF5E7A6C)
    val ribbon = if (dark) Color(0xFFC98A6D) else Color(0xFFC07A5A)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val baseY = h * 0.97f
        val cxm = w * 0.50f          // spine
        val topY = h * 0.26f         // page tops
        val half = w * 0.34f         // one page width
        // left page — a quad from the spine up to its top edge, then down to
        // the bottom, so the book sits ON the strip's bottom edge.
        drawPath(Path().apply {
            moveTo(cxm, baseY)
            lineTo(cxm, topY + h * 0.10f)
            lineTo(cxm - half * 0.86f, topY)
            lineTo(cxm - half, baseY)
            close()
        }, color = page)
        // right page (mirrored, slightly shaded)
        drawPath(Path().apply {
            moveTo(cxm, baseY)
            lineTo(cxm, topY + h * 0.10f)
            lineTo(cxm + half * 0.86f, topY)
            lineTo(cxm + half, baseY)
            close()
        }, color = page.copy(alpha = 0.85f))
        // page lines — three per side, reading left-to-right
        for (i in 1..3) {
            val ly = topY + h * 0.10f + i * (baseY - topY - h * 0.10f) * 0.20f
            drawLine(line.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(cxm - half * 0.74f, ly), androidx.compose.ui.geometry.Offset(cxm - w * 0.045f, ly), strokeWidth = 0.9f)
            drawLine(line.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(cxm + w * 0.045f, ly), androidx.compose.ui.geometry.Offset(cxm + half * 0.74f, ly), strokeWidth = 0.9f)
        }
        // spine — the two pages visibly join here
        drawLine(spine, androidx.compose.ui.geometry.Offset(cxm, topY + h * 0.10f), androidx.compose.ui.geometry.Offset(cxm, baseY), strokeWidth = 1.5f)
        // ribbon bookmark hanging from the spine top
        drawPath(Path().apply {
            moveTo(cxm - w * 0.028f, topY + h * 0.02f)
            lineTo(cxm + w * 0.028f, topY + h * 0.02f)
            lineTo(cxm + w * 0.028f, topY + h * 0.22f)
            lineTo(cxm, topY + h * 0.15f)
            lineTo(cxm - w * 0.028f, topY + h * 0.22f)
            close()
        }, color = ribbon)
    }
}

/** WANT TO READ — three book spines standing side by side on the bottom
 *  edge with title ticks, a soft grounding shadow and a ribbon on the
 *  tallest spine (clean, connected — the old tilted floating spines read
 *  random). */
@Composable
private fun BoxScope.BooksArt(dark: Boolean) {
    val spines = if (dark) listOf(0xFF7A5C4C, 0xFF9A7560, 0xFFC09379)
    else listOf(0xFF9C7562, 0xFFB98D79, 0xFFD39F91)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val baseY = h * 0.97f
        // soft grounding shadow under the row
        drawOval(
            (if (dark) Color.Black else Color(0xFF6B4A3A)).copy(alpha = 0.12f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.19f, baseY - h * 0.045f),
            size = androidx.compose.ui.geometry.Size(w * 0.62f, h * 0.09f)
        )
        val cx = listOf(0.18f, 0.50f, 0.80f)
        val bw = listOf(0.20f, 0.22f, 0.17f)
        val bh = listOf(0.46f, 0.60f, 0.36f)
        spines.forEachIndexed { i, c ->
            val x = w * cx[i] - w * bw[i] / 2f
            val y = baseY - h * bh[i]
            drawPath(Path().apply {
                moveTo(x, baseY)
                lineTo(x, y)
                lineTo(x + w * bw[i], y)
                lineTo(x + w * bw[i], baseY)
                close()
            }, color = Color(c))
            // spine highlight — a thin lighter strip near the left edge
            drawLine(Color.White.copy(alpha = 0.28f), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.14f, y), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.14f, baseY), strokeWidth = 1f)
            // title ticks — two short lines near the top of each spine
            val t1 = y + h * bh[i] * 0.16f
            val t2 = y + h * bh[i] * 0.25f
            drawLine(Color.White.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.24f, t1), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.76f, t1), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.28f), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.24f, t2), androidx.compose.ui.geometry.Offset(x + w * bw[i] * 0.58f, t2), strokeWidth = 1f)
        }
        // a small ribbon on the tallest spine
        drawPath(Path().apply {
            val x = w * 0.50f - w * 0.22f / 2f
            moveTo(x + w * 0.22f * 0.42f, h * 0.97f - h * 0.60f)
            lineTo(x + w * 0.22f * 0.58f, h * 0.97f - h * 0.60f)
            lineTo(x + w * 0.22f * 0.58f, h * 0.97f - h * 0.60f + h * 0.16f)
            lineTo(x + w * 0.22f * 0.50f, h * 0.97f - h * 0.60f + h * 0.11f)
            lineTo(x + w * 0.22f * 0.42f, h * 0.97f - h * 0.60f + h * 0.16f)
            close()
        }, color = if (dark) Color(0xFFC98A6D) else Color(0xFFC07A5A))
    }
}

/** MOUNTAIN — layered peak silhouette + a low sun + flower dots. */
@Composable
private fun BoxScope.MountainArt(dark: Boolean) {
    val hills = if (dark) Color(0xFF5E7E6C) else Color(0xFF8FB4A0)
    val flowers = if (dark) Color(0xFFE9F2E4) else Color(0xFFFDFEFC)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawCircle(Color(0xFFF2B36B).copy(alpha = 0.5f), radius = w * 0.09f, center = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.24f))
        drawPath(mountainPath(w, h), color = hills)
        listOf(0.16f, 0.30f, 0.44f, 0.62f, 0.80f).forEachIndexed { i, fx ->
            val cx = w * fx
            val cy = h * (0.86f + 0.03f * (i % 3))
            drawCircle(color = flowers, radius = 3.2f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        }
    }
}

/** COMPLETED — a rising sun over two ridgelines, a flag planted at the
 *  summit and a small bird, with a solid GROUND BAND across the bottom so
 *  the whole scene connects to the card's bottom edge. The ridges run a
 *  little PAST the canvas edges so they read as continuing mountains, not
 *  shapes chopped by the frame. */
@Composable
private fun BoxScope.PeakArt(dark: Boolean) {
    val far = if (dark) Color(0xFF4A6A5C) else Color(0xFFA9C7B4)
    val near = if (dark) Color(0xFF5E7E6C) else Color(0xFF8FB4A0)
    val ground = if (dark) Color(0xFF3C5140) else Color(0xFF7DA48E)
    val sun = if (dark) Color(0xFFF4C768) else Color(0xFFF2B36B)
    val flag = if (dark) Color(0xFFE9F2E4) else Color(0xFFFDFEFC)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        // rising sun over the far ridge
        drawCircle(sun.copy(alpha = 0.85f), radius = w * 0.11f, center = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.44f))
        drawCircle(sun.copy(alpha = 0.10f), radius = w * 0.24f, center = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.44f))
        // far ridge — extends past BOTH edges so the ends never look cut
        drawPath(Path().apply {
            moveTo(-w * 0.08f, h * 0.70f)
            lineTo(w * 0.18f, h * 0.30f)
            lineTo(w * 0.34f, h * 0.54f)
            lineTo(w * 0.52f, h * 0.26f)
            lineTo(w * 0.72f, h * 0.62f)
            lineTo(w * 1.08f, h * 0.44f)
            lineTo(w * 1.08f, h * 0.88f)
            lineTo(-w * 0.08f, h * 0.88f)
            close()
        }, color = far)
        // near ridge — same treatment
        drawPath(Path().apply {
            moveTo(-w * 0.08f, h * 0.82f)
            lineTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.52f, h * 0.74f)
            lineTo(w * 0.74f, h * 0.46f)
            lineTo(w * 1.08f, h * 0.72f)
            lineTo(w * 1.08f, h * 0.92f)
            lineTo(-w * 0.08f, h * 0.92f)
            close()
        }, color = near)
        // ground band — ties the ridges to the very bottom edge
        drawRect(ground, topLeft = androidx.compose.ui.geometry.Offset(-w * 0.08f, h * 0.90f), size = androidx.compose.ui.geometry.Size(w * 1.16f, h * 0.10f))
        // summit flag
        val fx = w * 0.52f; val fy = h * 0.26f
        drawLine(if (dark) Color(0xFF3C3A2E) else Color(0xFF6B5A44), androidx.compose.ui.geometry.Offset(fx, fy), androidx.compose.ui.geometry.Offset(fx, fy - h * 0.14f), strokeWidth = 1.6f)
        drawPath(Path().apply {
            moveTo(fx, fy - h * 0.14f)
            lineTo(fx + w * 0.09f, fy - h * 0.10f)
            lineTo(fx, fy - h * 0.06f)
            close()
        }, color = flag)
        // bird
        drawLine(flag.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.20f), androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.16f), strokeWidth = 1.2f)
        drawLine(flag.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.16f), androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.20f), strokeWidth = 1.2f)
    }
}

/** NOTES — a proper NOTE: a cream notepad sheet (with a second sheet
 *  peeking out behind) with a folded dog-ear, ruled writing lines, a
 *  little heart doodle and a pencil resting against it — instantly reads
 *  as a note (the old tilted slip-stack read as random shapes). */
@Composable
private fun BoxScope.NotesArt(dark: Boolean) {
    val paper = if (dark) Color(0xFFF0E4D0) else Color(0xFFFFFBF2)
    val paperShade = if (dark) Color(0xFFD8C8AC) else Color(0xFFEADFC8)
    val line = if (dark) Color(0xFFA08FC0) else Color(0xFF9C86C4)
    val pen = if (dark) Color(0xFFE3B7A8) else Color(0xFFB3796A)
    val eraser = if (dark) Color(0xFFE3A9A0) else Color(0xFFE58E8E)
    val lead = if (dark) Color(0xFF2E2622) else Color(0xFF4A3B35)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val nw = w * 0.44f; val nh = h * 0.60f
        val x0 = w * 0.08f; val y0 = h * 0.97f - nh
        // second sheet peeking out bottom-right — a notepad stack
        drawRoundRect(paperShade.copy(alpha = 0.85f),
            androidx.compose.ui.geometry.Offset(x0 + nw * 0.07f, y0 + nh * 0.10f),
            androidx.compose.ui.geometry.Size(nw, nh),
            androidx.compose.ui.geometry.CornerRadius(nw * 0.05f))
        // sheet with a folded (dog-ear) top-right corner
        drawPath(Path().apply {
            moveTo(x0, y0)
            lineTo(x0 + nw * 0.72f, y0)
            lineTo(x0 + nw, y0 + nh * 0.16f)   // fold
            lineTo(x0 + nw, y0 + nh)
            lineTo(x0, y0 + nh)
            close()
        }, color = paper)
        // the folded flap — a darker triangle tucked under the fold line
        drawPath(Path().apply {
            moveTo(x0 + nw * 0.72f, y0)
            lineTo(x0 + nw, y0 + nh * 0.16f)
            lineTo(x0 + nw, y0)
            close()
        }, color = paperShade)
        // fold shadow line
        drawLine(if (dark) Color(0xFFB3A48C) else Color(0xFFD9CBB2), androidx.compose.ui.geometry.Offset(x0 + nw * 0.72f, y0), androidx.compose.ui.geometry.Offset(x0 + nw, y0 + nh * 0.16f), strokeWidth = 1f)
        // ruled writing lines (stop before the fold)
        for (i in 1..4) {
            val ly = y0 + nh * 0.18f + i * (nh - nh * 0.18f) * 0.19f
            drawLine(line.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(x0 + nw * 0.10f, ly), androidx.compose.ui.geometry.Offset(x0 + nw * 0.88f, ly), strokeWidth = 0.9f)
        }
        // a little heart doodle at the foot of the sheet
        val hx = x0 + nw * 0.26f; val hy = y0 + nh * 0.84f; val hs = nw * 0.10f
        drawPath(Path().apply {
            moveTo(hx, hy + hs * 0.60f)
            cubicTo(hx - hs * 0.92f, hy - hs * 0.10f, hx - hs * 0.46f, hy - hs * 0.80f, hx, hy - hs * 0.26f)
            cubicTo(hx + hs * 0.46f, hy - hs * 0.80f, hx + hs * 0.92f, hy - hs * 0.10f, hx, hy + hs * 0.60f)
            close()
        }, color = pen.copy(alpha = 0.75f))
        // pencil resting diagonally at the bottom right — body + eraser + lead
        val p0x = w * 0.70f; val p0y = h * 0.42f
        val p1x = w * 0.945f; val p1y = h * 0.86f
        drawLine(pen, androidx.compose.ui.geometry.Offset(p0x, p0y), androidx.compose.ui.geometry.Offset(p1x, p1y), strokeWidth = w * 0.030f)
        drawCircle(eraser, radius = w * 0.017f, center = androidx.compose.ui.geometry.Offset(p0x, p0y))
        drawCircle(lead, radius = w * 0.011f, center = androidx.compose.ui.geometry.Offset(p1x, p1y))
    }
}

/** PERSONAL — a window with a moon and a plant on the sill. */
@Composable
private fun BoxScope.WindowArt(dark: Boolean) {
    val sky = if (dark) Brush.verticalGradient(listOf(Color(0xFF3E4A5E), Color(0xFF5E7F6E)))
    else Brush.verticalGradient(listOf(Color(0xFFF3CFA8), Color(0xFFA9B9A2)))
    val frame = if (dark) Color(0xFF3E2E28) else Color(0xFFA87F6B)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val ww = w * 0.40f; val wh = h * 0.54f
        val x0 = w * 0.70f - ww / 2f; val y0 = h * 0.92f - wh
        drawRoundRect(sky, androidx.compose.ui.geometry.Offset(x0, y0), androidx.compose.ui.geometry.Size(ww, wh), androidx.compose.ui.geometry.CornerRadius(ww * 0.06f))
        drawCircle(Color(0xFFF8E8C8), radius = ww * 0.13f, center = androidx.compose.ui.geometry.Offset(x0 + ww * 0.72f, y0 + wh * 0.28f))
        drawRoundRect(frame.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(x0, y0), androidx.compose.ui.geometry.Size(ww, wh), androidx.compose.ui.geometry.CornerRadius(ww * 0.06f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = ww * 0.05f))
        drawLine(frame.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(x0 + ww / 2f, y0), androidx.compose.ui.geometry.Offset(x0 + ww / 2f, y0 + wh), strokeWidth = ww * 0.035f)
        drawLine(frame.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(x0, y0 + wh / 2f), androidx.compose.ui.geometry.Offset(x0 + ww, y0 + wh / 2f), strokeWidth = ww * 0.035f)
        // plant on the sill
        val leaf = if (dark) Color(0xFF9DB58F) else Color(0xFF71896A)
        drawLine(leaf, androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.90f), androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.64f), strokeWidth = 1.6f)
        drawCircle(leaf, radius = w * 0.024f, center = androidx.compose.ui.geometry.Offset(w * 0.13f, h * 0.62f))
        drawCircle(leaf, radius = w * 0.019f, center = androidx.compose.ui.geometry.Offset(w * 0.19f, h * 0.64f))
        drawPath(Path().apply {
            moveTo(w * 0.10f, h * 0.92f)
            lineTo(w * 0.22f, h * 0.92f)
            lineTo(w * 0.19f, h * 0.82f)
            lineTo(w * 0.13f, h * 0.82f)
            close()
        }, color = frame.copy(alpha = 0.7f))
    }
}

/** SAVED ENTRIES — layered photo tiles with a ♡ and a sun on one print. */
@Composable
private fun BoxScope.PhotosArt(dark: Boolean) {
    val fills = if (dark) listOf(Color(0xFF46657A), Color(0xFF5A7B8C), Color(0xFF6F92A3))
    else listOf(Color(0xFF9CC3D9), Color(0xFF7FB0CE), Color(0xFF5E9CC2))
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val tw = w * 0.30f; val th = h * 0.40f
        val cx = w * 0.78f; val cy = h * 0.76f
        fills.forEachIndexed { i, c ->
            drawPath(rotRect(cx - i * tw * 0.16f, cy - th + i * th * 0.06f, tw, th, -0.10f + i * 0.06f), color = c)
        }
        drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.018f, center = androidx.compose.ui.geometry.Offset(cx + tw * 0.30f, cy - th * 0.64f))
        drawCircle(Color(0xFFF4C768).copy(alpha = 0.8f), radius = tw * 0.10f, center = androidx.compose.ui.geometry.Offset(cx - tw * 0.66f, cy - th * 0.78f))
    }
}

/** MINIMAL — sun arc + horizon + lone dot (the Minimal share card's
 *  sparse line language). */
@Composable
private fun BoxScope.MinimalSunArt(dark: Boolean) {
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val cx = w * 0.72f; val cy = h * 0.34f; val r = w * 0.17f
        drawArc(ink.copy(alpha = 0.85f), startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.014f))
        drawLine(ink.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.88f), androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.88f), strokeWidth = w * 0.012f)
        drawCircle(ink, radius = w * 0.02f, center = androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.64f))
    }
}

/** MINIMAL — two thin rings + a dot + a baseline. */
@Composable
private fun BoxScope.MinimalRingsArt(dark: Boolean) {
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val cx = w * 0.68f; val cy = h * 0.50f
        drawCircle(ink.copy(alpha = 0.30f), radius = w * 0.17f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.010f))
        drawCircle(ink.copy(alpha = 0.55f), radius = w * 0.10f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.010f))
        drawCircle(ink, radius = w * 0.02f, center = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.28f))
        drawLine(ink.copy(alpha = 0.40f), androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.88f), androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.88f), strokeWidth = w * 0.008f)
    }
}

/** MINIMAL — one thin wave + a dot. */
@Composable
private fun BoxScope.MinimalWaveArt(dark: Boolean) {
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawPath(Path().apply {
            moveTo(0f, h * 0.60f)
            cubicTo(w * 0.20f, h * 0.30f, w * 0.34f, h * 0.86f, w * 0.52f, h * 0.62f)
            cubicTo(w * 0.66f, h * 0.44f, w * 0.80f, h * 0.80f, w, h * 0.52f)
        }, color = ink.copy(alpha = 0.75f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.013f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawCircle(ink, radius = w * 0.018f, center = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.22f))
        drawLine(ink.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.90f), androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.90f), strokeWidth = w * 0.008f)
    }
}

/** MINIMAL — a sparse dot grid. */
@Composable
private fun BoxScope.MinimalDotsArt(dark: Boolean) {
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF6B5A52)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val xs = listOf(0.24f, 0.50f, 0.76f)
        val ys = listOf(0.30f, 0.52f, 0.74f)
        xs.forEachIndexed { i, fx ->
            ys.forEachIndexed { j, fy ->
                val alt = (i + j) % 2 == 0
                drawCircle(ink.copy(alpha = if (alt) 0.85f else 0.40f), radius = if (alt) w * 0.020f else w * 0.012f, center = androidx.compose.ui.geometry.Offset(w * fx, h * fy))
            }
        }
    }
}

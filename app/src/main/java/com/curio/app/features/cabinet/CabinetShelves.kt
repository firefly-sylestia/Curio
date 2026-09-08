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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** The JSX decorative art each shelf card wears at its foot. */
enum class V2ShelfArtType { STAR, READING, BOOKS, MOUNTAIN, NOTES, WINDOW, PHOTOS }

/** The seven built-ins, in the user's requested order. The four seeded
 *  shelves carry a stable `shelf:` collection id; the three virtual ones
 *  (Favorites / Saved entries / Notes) compute their members live. */
val builtInShelves: List<V2Shelf> = listOf(
    V2Shelf(
        id = V2ShelfId.FAVORITES,
        title = "Favorites",
        icon = CurioIcons.Star,
        tone = V2ShelfTone(light = 0xFFD8D1EE, dark = 0xFF4A4164),
        art = V2ShelfArtType.STAR
    ),
    V2Shelf(
        id = V2ShelfId.CURRENTLY_READING,
        title = "Currently Reading",
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
        icon = CurioIcons.TaskAlt,
        tone = V2ShelfTone(light = 0xFFCFE4D5, dark = 0xFF385345),
        art = V2ShelfArtType.MOUNTAIN,
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
    onLongPress: (() -> Unit)? = null
) {
    val dark = isCurioDarkTheme()
    val ink = if (dark) Color(0xFFF3E9E2) else Color(0xFF553E42)
    val muted = ink.copy(alpha = 0.62f)
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(22.dp),
        color = tone.fill()
    ) {
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
                if (onLongPress != null) {
                    CurioIcon(
                        name = CurioIcons.MoreVert,
                        contentDescription = "Rename or delete",
                        tint = ink.copy(alpha = 0.66f),
                        size = 20.dp
                    )
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
            V2ShelfArt(
                art = art,
                dark = dark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Shelf art — the JSX decorative foot art, drawn with Canvas + glyphs
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun V2ShelfArt(
    art: V2ShelfArtType,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (art) {
            V2ShelfArtType.STAR -> StarArt(dark)
            V2ShelfArtType.READING -> ReadingArt(dark)
            V2ShelfArtType.BOOKS -> BooksArt(dark)
            V2ShelfArtType.MOUNTAIN -> MountainArt(dark)
            V2ShelfArtType.NOTES -> NotesArt(dark)
            V2ShelfArtType.WINDOW -> WindowArt(dark)
            V2ShelfArtType.PHOTOS -> PhotosArt(dark)
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

@Composable
private fun BoxScope.StarArt(dark: Boolean) {
    val mountain = if (dark) Color(0xFF6F68A8) else Color(0xFF9992D2)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawPath(mountainPath(size.width, size.height), color = mountain)
    }
    Text(
        text = "\u2605",
        fontSize = 56.sp,
        color = Color(0xFFF4C768),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = (-6).dp, y = 0.dp)
    )
}

@Composable
private fun BoxScope.ReadingArt(dark: Boolean) {
    val bookFill = if (dark) Color(0xFFF2E8DA) else Color(0xFFF7EEE1)
    val leaf = if (dark) Color(0xFF9DB58F) else Color(0xFF769070)
    // Open book — two page halves with a spine gap.
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 10.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 3.dp, bottomStart = 7.dp, bottomEnd = 3.dp))
                .background(bookFill)
        )
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 7.dp))
                .background(bookFill.copy(alpha = 0.82f))
        )
    }
    CurioIcon(
        name = CurioIcons.LocalCafe,
        contentDescription = null,
        tint = if (dark) Color(0xFFE8D3B8) else Color(0xFFA8805F),
        size = 22.dp,
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp)
    )
    CurioIcon(
        name = "spa",
        contentDescription = null,
        tint = leaf,
        size = 30.dp,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun BoxScope.BooksArt(dark: Boolean) {
    // Stacked book spines.
    val spines = if (dark) {
        listOf(0xFF7A5C4C to 30f, 0xFF9A7560 to 40f, 0xFFC09379 to 52f)
    } else {
        listOf(0xFF9C7562 to 30f, 0xFFB98D79 to 40f, 0xFFD39F91 to 52f)
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 12.dp, bottom = 9.dp)
    ) {
        spines.forEachIndexed { i, (c, h) ->
            Box(
                modifier = Modifier
                    .width((22 + i * 8).dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(Color(c))
                    .rotate(if (i == 1) 0f else if (i == 0) -6f else 5f)
            )
        }
    }
    // Paper note.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 4.dp, end = 6.dp)
            .rotate(7f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (dark) Color(0xFF3A312C) else Color(0xFFF8EAD6))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = "Someday",
            fontSize = 10.sp,
            fontStyle = FontStyle.Italic,
            color = if (dark) Color(0xFFEADCC9) else Color(0xFF7E5A4E)
        )
        Text(
            text = "\u2661",
            fontSize = 10.sp,
            color = if (dark) Color(0xFFE3B7A8) else Color(0xFFB3796A)
        )
    }
}

@Composable
private fun BoxScope.MountainArt(dark: Boolean) {
    val hills = if (dark) Color(0xFF5E7E6C) else Color(0xFF8FB4A0)
    val flowers = if (dark) Color(0xFFE9F2E4) else Color(0xFFFDFEFC)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawPath(mountainPath(size.width, size.height), color = hills)
        // Small flowers on the lower ridge.
        val xs = listOf(0.16f, 0.30f, 0.44f, 0.62f, 0.80f)
        xs.forEachIndexed { i, fx ->
            val cx = size.width * fx
            val cy = size.height * (0.86f + 0.03f * (i % 3))
            drawCircle(color = flowers, radius = 3.2f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        }
    }
}

@Composable
private fun BoxScope.NotesArt(dark: Boolean) {
    // Paper stack — three rotated slips.
    val stack = listOf(
        if (dark) 0xFF8A7BA8 else 0xFFB7A9D7,
        if (dark) 0xFFA08FC0 else 0xFFD3C6E7,
        if (dark) 0xFFC9B9D8 else 0xFFF0E1D5
    )
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 18.dp, bottom = 14.dp)
    ) {
        stack.forEachIndexed { i, c ->
            Box(
                modifier = Modifier
                    .offset(x = (i * 10).dp, y = (-i * 2).dp)
                    .width(46.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(c))
                    .rotate((-8 + i * 4).toFloat())
            )
        }
    }
    // Idea note.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 8.dp, bottom = 12.dp)
            .rotate(4f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (dark) Color(0xFF3A312C) else Color(0xFFF8EAD9))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = "Ideas",
            fontSize = 10.sp,
            fontStyle = FontStyle.Italic,
            color = if (dark) Color(0xFFEADCC9) else Color(0xFF7E5A4E)
        )
        Text(
            text = "\u2661",
            fontSize = 10.sp,
            color = if (dark) Color(0xFFE3B7A8) else Color(0xFFB3796A)
        )
    }
}

@Composable
private fun BoxScope.WindowArt(dark: Boolean) {
    val sky = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF8A5F3F), Color(0xFF6E7F6A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF3CFA8), Color(0xFFA9B9A2)))
    }
    val frame = if (dark) Color(0xFF3E2E28) else Color(0xFFA87F6B)
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 12.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(sky)
        )
        // Window frame + cross bars.
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(52.dp)
                .border(4.dp, frame.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(5.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .background(frame.copy(alpha = 0.55f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = 4.dp)
                .background(frame.copy(alpha = 0.55f))
        )
    }
    CurioIcon(
        name = "spa",
        contentDescription = null,
        tint = if (dark) Color(0xFF9DB58F) else Color(0xFF71896A),
        size = 30.dp,
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 6.dp)
    )
}

@Composable
private fun BoxScope.PhotosArt(dark: Boolean) {
    // Layered photo tiles with a little ♡ on the front one.
    val fills = if (dark) {
        listOf(Color(0xFF46657A), Color(0xFF5A7B8C), Color(0xFF6F92A3))
    } else {
        listOf(Color(0xFF9CC3D9), Color(0xFF7FB0CE), Color(0xFF5E9CC2))
    }
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 14.dp, bottom = 10.dp)
    ) {
        fills.forEachIndexed { i, c ->
            Box(
                modifier = Modifier
                    .offset(x = (-(i * 9)).dp, y = (i * 2).dp)
                    .width(44.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(c)
                    .rotate((-6 + i * 6).toFloat())
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .rotate(-6f)
        ) {
            Text(
                text = "\u2661",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.92f)
            )
        }
    }
    Text(
        text = "Shelf",
        fontSize = 10.sp,
        fontStyle = FontStyle.Italic,
        color = if (dark) Color(0xFFD8E6EE) else Color(0xFF4E7A96),
        textAlign = TextAlign.End,
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 12.dp)
    )
}
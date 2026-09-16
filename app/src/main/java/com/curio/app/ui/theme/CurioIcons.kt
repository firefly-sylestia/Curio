package com.curio.app.ui.theme

import com.curio.app.R
import com.curio.app.data.CategoryFamily
import com.curio.app.data.JournalMood
import com.curio.app.data.MusicService
import com.curio.app.data.SearchEngine
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.material3.Text
import com.curio.app.data.CurioAlivePreferences

/**
 * Curio's icon system — see Curio icon contract.
 *
 * **NO emoji anywhere** in the app. All icons come from the Material Symbols
 * variable font bundled directly in Curio at
 * `app/src/main/res/font/material_symbols_outlined.ttf`.
 *
 * The renderer below is intentionally defensive: a few social glyph names
 * landed after the last font subset pass. They are remapped at render time to
 * already-bundled equivalents until the subset is regenerated, so one missing
 * ligature can never make a whole social surface disappear.
 */
object CurioIcons {
    const val Music = "album"
    const val Movies = "movie"
    const val Books = "menu_book"
    const val VisualArt = "palette"
    const val Science = "science"
    const val Wildcard = "casino"

    const val Menu = "menu"
    const val Home = "home"
    const val Person = "person"
    const val Search = "search"
    const val Settings = "settings"
    const val MoreVert = "more_vert"
    const val Close = "close"
    const val ArrowBack = "arrow_back"
    const val ArrowForward = "arrow_forward"
    const val ChevronLeft = "chevron_left"
    const val ChevronRight = "chevron_right"
    const val Check = "check"
    const val Add = "add"
    const val Remove = "remove"
    const val AutoAwesome = "auto_awesome"
    const val Tune = "tune"
    const val Inventory2 = "inventory_2"
    const val SearchOff = "search_off"
    const val History = "history"
    const val DragHandle = "drag_handle"
    const val Info = "info"
    const val Edit = "edit"
    const val Shuffle = "shuffle"
    const val GridView = "grid_view"
    const val Apps = "apps"
    const val Lock = "lock"
    const val VisibilityOff = "visibility_off"
    const val PushPin = "push_pin"
    const val Crop = "crop"
    const val Share = "share"
    const val Lightbulb = "lightbulb"
    const val Delete = "delete"
    const val Replay = "replay"
    const val Refresh = "refresh"
    const val Star = "star"
    const val StarOutline = "star"
    const val Bookmark = "bookmark"
    const val BookmarkBorder = "bookmark"
    const val ThumbUp = "thumb_up"
    const val ThumbDown = "thumb_down"

    /**
     * The HEART — one glyph, two states.
     *
     * There is deliberately no `FavoriteBorder`: Curio's bundled Material
     * Symbols subset (see [safeGlyphName]) carries `favorite` but not
     * `favorite_border`, so an outlined heart would render as the literal word.
     * An un-hearted control draws THIS glyph in a muted tone instead.
     */
    const val Favorite = "favorite"
    const val FormatQuote = "format_quote"
    const val FormatBold = "format_bold"
    const val FormatItalic = "format_italic"
    const val FormatUnderline = "format_underlined"
    const val FormatHighlight = "format_color_fill"
    const val FormatText = "text_fields"
    const val TextIncrease = "text_increase"
    const val TextDecrease = "text_decrease"
    const val Mic = "mic"
    const val MicNone = "mic"
    const val Image = "image"
    const val Fullscreen = "fullscreen"
    const val AspectRatio = "aspect_ratio"
    const val PhotoSizeSelectLarge = "photo_size_select_large"
    const val PlayArrow = "play_arrow"
    const val TravelExplore = "travel_explore"
    const val YouTubeActivity = "youtube_activity"
    const val MusicNote = "music_note"
    const val OpenInNew = "open_in_new"
    const val PlayCircle = "play_circle"
    const val ContentCopy = "content_copy"
    const val Pause = "pause"
    const val Stop = "stop"
    const val Timer = "timer"
    const val KeyboardArrowDown = "keyboard_arrow_down"
    const val KeyboardArrowUp = "keyboard_arrow_up"
    const val ArrowUpward = "arrow_upward"
    const val ArrowDownward = "arrow_downward"
    const val Casino = "casino"
    const val Album = "album"
    const val Movie = "movie"
    const val MenuBook = "menu_book"
    const val Palette = "palette"
    const val ScienceGlyph = "science"
    const val Colorize = "colorize"
    const val Undo = "undo"
    const val Redo = "redo"
    const val Layers = "layers"

    const val LocalCafe = "local_cafe"
    const val FoldedCorner = "auto_stories"
    const val RedMarginLine = "border_clear"

    const val Backup = "backup"
    const val Restore = "history"

    const val ErrorOutline = "error"
    const val BugReport = "bug_report"
    const val Warning = "warning"
    const val Download = "download"
    const val Notifications = "notifications"
    const val BubbleChart = "bubble_chart"
    const val Schedule = "schedule"
    const val LocalFire = "local_fire_department"
    const val DarkMode = "dark_mode"
    const val LightMode = "light_mode"
    const val Contrast = "contrast"

    const val MoodCalm = "self_improvement"
    const val MoodHappy = "sentiment_satisfied"
    const val MoodCurious = "psychology"
    const val MoodInspired = "lightbulb"
    const val MoodTired = "bedtime"
    const val MoodOverwhelmed = "mood_bad"

    const val CalendarToday = "calendar_today"

    const val EmojiEvents = "workspace_premium"
    const val Flag = "flag"
    const val WorkspacePremium = "workspace_premium"
    const val Pets = "pets"
    const val Brush = "brush"
    const val Fill = "format_paint"
    const val Eraser = "ink_eraser"
    const val Keyboard = "keyboard"
    const val Wallpaper = "wallpaper"
    const val TaskAlt = "task_alt"
    const val Database = "database"
    const val SupportAgent = "support_agent"
    const val Note = "edit_note"

    const val Hub = "hub"
    const val Chats = "chat_bubble"
    const val Friends = "groups"
    const val Send = "send"
    const val MoreHoriz = "more_horiz"
    const val Public = "public"
    const val Notes = "notes"
    const val Screenshot = "photo_camera"
    const val PhotoLibrary = "photo_library"

    fun heroWatermarkSymbols(family: CategoryFamily): List<String> = when (family) {
        CategoryFamily.MUSIC -> listOf("music_note", "library_music", "headphones", "mic", "album", "equalizer", "piano", "radio", "music_video", "queue_music")
        CategoryFamily.MOVIES -> listOf("movie", "videocam", "theater_comedy", "local_movies", "movie_filter", "play_circle", "slow_motion_video", "video_library", "theaters", "smart_display")
        CategoryFamily.BOOKS -> listOf("menu_book", "auto_stories", "library_books", "edit_note", "book", "format_quote", "import_contacts", "local_library", "edit", "menu_open")
        CategoryFamily.VISUAL_ART -> listOf("brush", "palette", "colorize", "photo_library", "museum", "photo_camera", "wallpaper", "architecture", "photo", "landscape")
        CategoryFamily.SCIENCE -> listOf("science", "biotech", "lightbulb", "functions", "psychology", "bubble_chart", "explore", "hub", "online_prediction", "genetics")
        CategoryFamily.ANIME_COMICS -> listOf("smart_display", "movie_filter", "auto_stories", "import_contacts", "menu_book", "play_circle", "theaters", "video_library", "library_books", "star")
        CategoryFamily.GAMES -> listOf("sports_esports", "videogame_asset", "casino", "diamond", "bolt", "workspace_premium", "star", "rocket_launch", "auto_awesome", "explore")
        CategoryFamily.MYTHOLOGY -> listOf("auto_awesome", "star", "nightlight", "public", "spa", "diamond", "bolt", "explore", "rocket_launch", "psychology")
        CategoryFamily.SPORTS -> listOf("sports_soccer", "flag", "workspace_premium", "local_fire_department", "star", "bolt", "public", "explore", "rocket_launch", "spa")
        CategoryFamily.FOOD -> listOf("restaurant", "local_cafe", "local_fire_department", "spa", "star", "auto_awesome", "diamond", "public", "bolt", "explore")
        CategoryFamily.INTERNET -> listOf("public", "hub", "bolt", "star", "auto_awesome", "explore", "rocket_launch", "diamond", "spa", "nightlight")
        CategoryFamily.WILDCARD -> listOf("casino", "auto_awesome", "explore", "bolt", "star", "nightlight", "public", "spa", "diamond", "rocket_launch")
    }

    fun settingsHeroSymbols(): List<String> = listOf("settings", "tune", "dark_mode", "light_mode", "contrast", "palette", "colorize", "backup", "notifications", "layers")

    fun historyHeroSymbols(): List<String> = listOf("history", "schedule", "restore", "replay", "refresh", "timer", "calendar_today", "undo", "auto_stories", "menu_book")

    fun drawerHeroSymbols(): List<String> = listOf("menu", "explore", "auto_awesome", "star", "diamond", "bolt")
}

val JournalMood.glyph: String
    get() = when (this) {
        JournalMood.CALM -> CurioIcons.MoodCalm
        JournalMood.HAPPY -> CurioIcons.MoodHappy
        JournalMood.CURIOUS -> CurioIcons.MoodCurious
        JournalMood.INSPIRED -> CurioIcons.MoodInspired
        JournalMood.TIRED -> CurioIcons.MoodTired
        JournalMood.OVERWHELMED -> CurioIcons.MoodOverwhelmed
    }

private fun safeGlyphName(name: String): String = when (name) {
    // These were introduced by the social layer after the last font subset.
    // Remap to glyphs known to exist in Curio's bundled font so social icons
    // remain visible without reverting newer icon work.
    "chat_bubble" -> "notes"
    "groups" -> "person"
    "send" -> "arrow_forward"
    "more_horiz" -> "more_vert"
    "notes" -> "edit_note"
    "lock" -> "visibility_off"
    else -> name
}

@Composable
fun CurioIcon(
    name: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    weight: FontWeight = FontWeight.Normal
) {
    val iconSp = (size.value / LocalDensity.current.fontScale.coerceAtLeast(1f)).sp
    val alive = CurioAlivePreferences.isEnabled(LocalContext.current)
    val navIcon = contentDescription in setOf("Home", "Shuffle", "Cabinet", "Social")
    val navSelected = navIcon && tint != MaterialTheme.colorScheme.onSurfaceVariant
    val navScale by animateFloatAsState(
        targetValue = if (alive && navSelected) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "aliveNavIconScale"
    )
    val navRotation by animateFloatAsState(
        targetValue = if (alive && navSelected) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 430f),
        label = "aliveNavIconRotation"
    )

    var inkShiftPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) this.contentDescription = contentDescription
                this.role = Role.Image
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = safeGlyphName(name),
            fontFamily = MaterialSymbolsFontFamily,
            fontWeight = weight,
            fontSize = iconSp,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                lineHeight = iconSp,
                platformStyle = PlatformTextStyle(includeFontPadding = true)
            ),
            onTextLayout = { layout ->
                val inkBounds = runCatching { layout.getBoundingBox(0) }.getOrNull()
                if (inkBounds != null && layout.size.height > 0) {
                    inkShiftPx = layout.size.height / 2f -
                        (inkBounds.top + inkBounds.bottom) / 2f +
                        layout.size.height * 0.04f
                }
            },
            modifier = Modifier
                .offset { IntOffset(0, inkShiftPx.roundToInt()) }
                .graphicsLayer {
                    scaleX = navScale
                    scaleY = navScale
                    rotationZ = navRotation
                }
        )
    }
}

@Composable
fun Modifier.curioGlyphInkNudge(yDp: Float): Modifier {
    val fontScale = LocalDensity.current.fontScale.coerceAtMost(1f)
    return this.offset(y = yDp.dp * fontScale)
}

fun SearchEngine.brandTile(): Pair<Color, String> = when (this) {
    SearchEngine.GOOGLE -> Color(0xFF4285F4) to "G"
    SearchEngine.DUCKDUCKGO -> Color(0xFFDE5833) to "D"
    SearchEngine.BING -> Color(0xFF008373) to "B"
    SearchEngine.BRAVE -> Color(0xFFFB542B) to "B"
    SearchEngine.ECOSIA -> Color(0xFF008A52) to "E"
    SearchEngine.STARTPAGE -> Color(0xFF5469EC) to "S"
    SearchEngine.YAHOO -> Color(0xFF6001D2) to "Y"
}

fun MusicService.brandTile(): Pair<Color, String> = when (this) {
    MusicService.YOUTUBE -> Color(0xFFFF0000) to CurioIcons.YouTubeActivity
    MusicService.YOUTUBE_MUSIC -> Color(0xFFFF0000) to CurioIcons.YouTubeActivity
    MusicService.APPLE_MUSIC -> Color(0xFFFA2D48) to CurioIcons.MusicNote
    MusicService.SPOTIFY -> Color(0xFF1DB954) to CurioIcons.PlayCircle
}

val MusicService.brandRes: Int
    get() = when (this) {
        MusicService.YOUTUBE -> R.drawable.ic_music_youtube
        MusicService.YOUTUBE_MUSIC -> R.drawable.ic_music_youtube_music
        MusicService.APPLE_MUSIC -> R.drawable.ic_music_apple_music
        MusicService.SPOTIFY -> R.drawable.ic_music_spotify
    }

@Composable
fun BrandMonogram(
    tileColor: Color,
    glyph: String? = null,
    letter: String? = null,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.2f))
            .background(tileColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            glyph != null -> CurioIcon(
                name = glyph,
                contentDescription = null,
                tint = Color.White,
                size = size * 0.62f
            )
            letter != null -> Text(
                letter,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.52f).sp,
                maxLines = 1
            )
        }
    }
}

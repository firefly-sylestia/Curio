package com.curio.app.ui.theme

import com.curio.app.data.CategoryFamily
import com.curio.app.data.JournalMood
import com.curio.app.data.MusicService
import com.curio.app.data.SearchEngine
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Curio's icon system — see Curio icon contract.
 *
 * **NO emoji anywhere** in the app. All icons come from the Material Symbols
 * variable font bundled directly in Curio at
 * `app/src/main/res/font/material_symbols_outlined.ttf`.
 *
 * Icons are rendered as Text composables using the ligature names from the
 * Material Symbols glyph catalog. The font is bound to [MaterialSymbolsFontFamily]
 * (see CurioTypography.kt).
 *
 * Glyph constants live in [CurioIcons]. Glyph names use snake_case to match
 * the Material Symbols catalog exactly.
 */
object CurioIcons {

    // ── Category glyphs (Curio icon contract — used everywhere a category appears)
    const val Music       = "album"        // vinyl record
    const val Movies      = "movie"        // clapperboard
    const val Books       = "menu_book"    // open book
    const val VisualArt   = "palette"      // artist palette
    const val Science     = "science"      // atom/flask
    const val Wildcard    = "casino"       // die

    // ── UI affordance glyphs (from Material Symbols catalog)
    const val Menu        = "menu"             // ☰ — top-left
    const val Home        = "home"             // house — bottom nav Home tab
    const val Person      = "person"           // top-right avatar
    const val Search      = "search"           // top-right magnifier
    const val Settings    = "settings"         // cog
    const val MoreVert    = "more_vert"        // ⋮ — overflow
    const val Close       = "close"            // X
    const val ArrowBack   = "arrow_back"       // ← — legacy top-left back arrow
    const val ArrowForward = "arrow_forward"
    const val ChevronLeft  = "chevron_left"    // ‹ — unified back arrow
    const val ChevronRight = "chevron_right"   // › — unified forward arrow
    const val Check       = "check"            // ✓
    const val Add         = "add"              // +
    const val AutoAwesome = "auto_awesome"     // sparkles / logomark
    const val Tune         = "tune"            // sliders — Preferences settings entry
    const val Inventory2  = "inventory_2"      // cabinet empty state
    const val SearchOff   = "search_off"       // no-results state
    const val History     = "history"          // topic history empty
    const val DragHandle  = "drag_handle"      // ⋮ — manage categories drag
    const val Info        = "info"
    const val Edit        = "edit"
    const val Share       = "share"
    const val Delete      = "delete"
    const val Replay      = "replay"
    const val Refresh     = "refresh"
    const val Star        = "star"
    const val StarOutline = "star_outline"
    const val Bookmark    = "bookmark"           // filled — pinned topic
    const val BookmarkBorder = "bookmark_border" // outline — not pinned
    const val ThumbUp     = "thumb_up"            // 👍 — liked topic
    const val ThumbDown   = "thumb_down"          // 👎 — disliked topic
    const val FormatQuote = "format_quote"
    // ── Rich-text formatting (Marginalia journal/quotes + format fields)
    const val FormatBold = "format_bold"           // B — bold
    const val FormatItalic = "format_italic"       // I — italic
    const val FormatUnderline = "format_underline" // U — underline
    const val FormatHighlight = "format_color_fill" // highlighter marker
    const val FormatText = "text_fields"           // small toggle for other fields
    const val TextIncrease = "text_increase"       // A+ — enlarge selection
    const val TextDecrease = "text_decrease"       // A− — shrink selection
    const val Mic         = "mic"
    const val MicNone     = "mic_none"
    const val Image       = "image"
    const val Fullscreen  = "fullscreen"   // ⤢ — expand mood board
    const val AspectRatio = "aspect_ratio" // ▭ — Smart Spin layout (small-screen fit)
    const val PhotoSizeSelectLarge = "photo_size_select_large" // ⤢ size — Smart density layout (deck scale)
    const val PlayArrow   = "play_arrow"
    const val TravelExplore = "travel_explore"   // globe + magnifier — explore in the user's chosen engine
    const val YouTubeActivity = "youtube_activity" // rounded play tile — explore in YouTube
    const val MusicNote = "music_note"          // ♪ — Apple Music / music rows
    const val PlayCircle = "play_circle"        // ▶ in a ring — Spotify
    const val Pause       = "pause"
    const val Stop        = "stop"
    const val Timer       = "timer"
    const val KeyboardArrowDown = "keyboard_arrow_down"  // ▼ — chevron
    const val KeyboardArrowUp   = "keyboard_arrow_up"    // ▲ — chevron
    const val ArrowUpward   = "arrow_upward"    // ⬆ — sort oldest-first
    const val ArrowDownward = "arrow_downward"  // ⬇ — sort newest-first
    const val Casino      = "casino"
    const val Album       = "album"
    const val Movie       = "movie"
    const val MenuBook    = "menu_book"
    const val Palette     = "palette"
    const val ScienceGlyph = "science"
    const val Colorize    = "colorize"   // eyedropper — Pastel colors mode
    const val Undo        = "undo"      // ↩ — pet designer undo
    const val Redo        = "redo"      // ↪ — pet designer redo
    const val Layers      = "layers"     // stacked cards — Deck cards look (v7.7)

    // ── Note-paper style chips (Ruled/Torn/Coffee/Folded/Red-margin)
    const val LocalCafe     = "local_cafe"        // coffee-stain paper
    const val FoldedCorner  = "auto_stories"      // folded page (dog-ear)
    const val RedMarginLine = "border_clear"      // ruled with red margin

    // ── Backup / restore glyphs (Settings → Backup & restore)
    const val Backup       = "backup"        // cloud upload — export data
    const val Restore      = "restore"       // cloud download — import data

    // ── Status / report glyphs
    const val ErrorOutline = "error_outline"
    const val BugReport     = "bug_report"
    const val Warning       = "warning"
    const val Download      = "download"     // ⬇ — check for updates
    const val Notifications = "notifications"
    const val BubbleChart   = "bubble_chart"   // floating explore bubble
    const val Schedule      = "schedule"
    const val LocalFire     = "local_fire_department"
    const val DarkMode      = "dark_mode"
    const val LightMode     = "light_mode"      // sun — onboarding theme picker
    const val Contrast      = "contrast"        // half-filled circle — system theme

    // ── Journal mood glyphs (Marginalia editor + saved-entry meta card)
    const val MoodCalm       = "self_improvement"    // meditating figure
    const val MoodHappy      = "sentiment_satisfied" // smiley
    const val MoodCurious    = "psychology"          // head with gears
    const val MoodInspired   = "lightbulb"           // bulb
    const val MoodTired      = "bedtime"             // crescent moon
    const val MoodOverwhelmed = "mood_bad"           // frowning face

    // ── Entry meta card glyph (date & time segment)
    const val CalendarToday  = "calendar_today"

    // ── Quests / levels / achievements (v7.40)
    const val EmojiEvents     = "workspace_premium"     // trophy — achievements shelf (v7.89: glyph verified in font subset; "emoji_events" was tofu)
    const val Flag            = "flag"                  // journey marker
    const val WorkspacePremium = "workspace_premium"    // badge — level milestones
    // v8.34 — paw glyph for the Pet designer Settings entry (Material Symbols "pets").
    const val Pets = "pets"
    // v8.35 — pet designer tool glyphs (verified present in the bundled font subset).
    const val Brush = "brush"                 // paint brush — paint tool
    const val Fill = "format_paint"           // paint roller — fill bucket tool
    const val Eraser = "ink_eraser"           // eraser — erase tool
    const val Keyboard = "keyboard"           // typing reaction
    const val Wallpaper = "wallpaper"         // PNG export
    const val TaskAlt         = "task_alt"              // current quest
    const val Database        = "database"              // v7.89 — Browse Topics drawer entry (verified in font subset)
    const val SupportAgent    = "support_agent"          // v7.89 — Support & diagnostics drawer entry (verified in font subset)
    // v27 — explore-session attachments (all verified in the bundled font
    // subset: edit_note + photo_camera + photo_library ship in the hero
    // watermark family lists).
    const val Note            = "edit_note"             // shared session note
    const val Screenshot      = "photo_camera"          // capture the screen
    const val PhotoLibrary    = "photo_library"         // add screenshots from the gallery

    /**
     * Per-family symbol sets for the saved-entry hero's decorative watermark
     * scatter — instruments for Music, camera kit for Movies, books for
     * Books, art tools for Visual Art, lab symbols for Science, curiosities
     * for Wildcard. Standard Material Symbols OUTLINED ligature names, so a
     * Music entry's hero scatters music notes/pianos, an Artists entry
     * scatters instruments, etc.
     */
    fun heroWatermarkSymbols(family: CategoryFamily): List<String> = when (family) {
        // Exactly 10 — one per hero scatter slot, so no glyph repeats.
        // v7.33 — swapped the three names missing from the bundled Material
        // Symbols font (audiotrack / ondemand_video / create rendered as
        // empty tofu boxes) for glyphs that exist in the font.
        CategoryFamily.MUSIC -> listOf(
            "music_note", "library_music", "headphones", "mic", "album",
            "equalizer", "piano", "radio", "music_video", "queue_music"
        )
        CategoryFamily.MOVIES -> listOf(
            "movie", "videocam", "theater_comedy", "local_movies", "movie_filter",
            "play_circle", "slow_motion_video", "video_library", "theaters", "smart_display"
        )
        CategoryFamily.BOOKS -> listOf(
            "menu_book", "auto_stories", "library_books", "edit_note", "book",
            "format_quote", "import_contacts", "local_library", "edit", "menu_open"
        )
        CategoryFamily.VISUAL_ART -> listOf(
            "brush", "palette", "colorize", "photo_library", "museum",
            "photo_camera", "wallpaper", "architecture", "photo", "landscape"
        )
        CategoryFamily.SCIENCE -> listOf(
            "science", "biotech", "lightbulb", "functions", "psychology",
            "bubble_chart", "explore", "hub", "online_prediction", "genetics"
        )
        CategoryFamily.ANIME_COMICS -> listOf(
            "smart_display", "movie_filter", "auto_stories", "import_contacts", "menu_book",
            "play_circle", "theaters", "video_library", "library_books", "star"
        )
        CategoryFamily.GAMES -> listOf(
            "sports_esports", "videogame_asset", "casino", "diamond", "bolt",
            "workspace_premium", "star", "rocket_launch", "auto_awesome", "explore"
        )
        CategoryFamily.MYTHOLOGY -> listOf(
            "auto_awesome", "star", "nightlight", "public", "spa",
            "diamond", "bolt", "explore", "rocket_launch", "psychology"
        )
        CategoryFamily.SPORTS -> listOf(
            "sports_soccer", "flag", "workspace_premium", "local_fire_department", "star",
            "bolt", "public", "explore", "rocket_launch", "spa"
        )
        CategoryFamily.FOOD -> listOf(
            "restaurant", "local_cafe", "local_fire_department", "spa", "star",
            "auto_awesome", "diamond", "public", "bolt", "explore"
        )
        CategoryFamily.INTERNET -> listOf(
            "public", "hub", "bolt", "star", "auto_awesome",
            "explore", "rocket_launch", "diamond", "spa", "nightlight"
        )
        CategoryFamily.WILDCARD -> listOf(
            "casino", "auto_awesome", "explore", "bolt", "star",
            "nightlight", "public", "spa", "diamond", "rocket_launch"
        )
    }
}

/** The Material Symbols glyph a journal mood wears. */
val JournalMood.glyph: String
    get() = when (this) {
        JournalMood.CALM -> CurioIcons.MoodCalm
        JournalMood.HAPPY -> CurioIcons.MoodHappy
        JournalMood.CURIOUS -> CurioIcons.MoodCurious
        JournalMood.INSPIRED -> CurioIcons.MoodInspired
        JournalMood.TIRED -> CurioIcons.MoodTired
        JournalMood.OVERWHELMED -> CurioIcons.MoodOverwhelmed
    }

/** Shared painted-ink correction for the Material Symbols font's low visual baseline. */
private val MaterialSymbolsOpticalLift = 1.dp

/**
 * Renders a Material Symbols glyph via ligature.
 *
 * @param name Material Symbols ligature name (e.g. "play_arrow"). See [CurioIcons].
 * @param contentDescription Accessibility description.
 * @param modifier Standard [Modifier].
 * @param tint Glyph tint. Defaults to [LocalContentColor.current].
 * @param size Glyph box size (also used for sp size).
 * @param weight Font weight (Normal/Bold). Defaults to Normal.
 */
@Composable
fun CurioIcon(
    name: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    weight: FontWeight = FontWeight.Normal
) {
    Box(
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                this.role = Role.Image
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontFamily = MaterialSymbolsFontFamily,
            fontWeight = weight,
            fontSize = size.value.sp,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.graphicsLayer {
                // Material Symbols reserve a little more visual space below
                // the glyph than above it. Nudge only the painted ink up by
                // one dp; the icon's layout box stays perfectly centered.
                // This keeps icons visually centered in compact pills without
                // changing their touch target or measured box.
                translationY = -MaterialSymbolsOpticalLift.toPx()
            },
            style = TextStyle(
                lineHeight = size.value.sp,
                // Material Symbols are font glyphs, not vector Icons. Remove
                // the platform font padding and center the line box so their
                // visible ink sits in the same vertical center as adjacent
                // text and button content.
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )
    }
}

/**
 * v27s — the search-engine pill tile: brand color + letter monogram. The
 * icon font has no logo glyphs, so each engine is a colored rounded tile
 * carrying its initial — readable at 18dp and brand-recognizable.
 */
fun SearchEngine.brandTile(): Pair<Color, String> = when (this) {
    SearchEngine.GOOGLE -> Color(0xFF4285F4) to "G"
    SearchEngine.DUCKDUCKGO -> Color(0xFFDE5833) to "D"
    SearchEngine.BING -> Color(0xFF008373) to "B"
    SearchEngine.BRAVE -> Color(0xFFFB542B) to "B"
    SearchEngine.ECOSIA -> Color(0xFF008A52) to "E"
    SearchEngine.STARTPAGE -> Color(0xFF5469EC) to "S"
    SearchEngine.YAHOO -> Color(0xFF6001D2) to "Y"
}

/**
 * v27s — the music-service pill tile: brand color + a Material glyph (the
 * font has no Apple/Spotify logos, so the services use their recognizable
 * glyph + color instead).
 */
fun MusicService.brandTile(): Pair<Color, String> = when (this) {
    MusicService.YOUTUBE_MUSIC -> Color(0xFFFF0000) to CurioIcons.YouTubeActivity
    MusicService.APPLE_MUSIC -> Color(0xFFFA2D48) to CurioIcons.MusicNote
    MusicService.SPOTIFY -> Color(0xFF1DB954) to CurioIcons.PlayCircle
}

/**
 * v27s — a small brand tile: a rounded square in the brand color carrying
 * either a Material glyph ([glyph]) or a letter monogram ([letter]).
 * Retired from the explore dialog (v27u uses clean glyph pills instead)
 * but kept as a general-purpose brand chip helper.
 */
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
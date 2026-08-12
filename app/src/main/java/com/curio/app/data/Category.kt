package com.curio.app.data

import androidx.compose.ui.graphics.Color
import com.curio.app.ui.theme.CurioColors

/**
 * Curio's 11 content categories — see Curio design contract.
 *
 * Each "domain" (Music / Movies / Books / Visual Art / Science) is split
 * into TWO first-class categories so the user has granular control over
 * the kind of exploration:
 *
 *   Music          →  ARTISTS, ALBUMS
 *   Movies         →  DIRECTORS, FILMS
 *   Books          →  AUTHORS, BOOKS
 *   Visual Art     →  PAINTERS, ARTWORKS
 *   Science        →  SCIENTISTS, DISCOVERIES
 *   (standalone)   →  WILDCARD
 *
 * Total = 11 top-level chips. The Wildcard category reuses the brand-
 * primary coral accent ([com.curio.app.ui.theme.CurioColors.CategoryCoral]);
 * its cards share the same themed gradient as the named categories, so no
 * special-casing is needed for it.
 *
 * The id enum uses UPPER_SNAKE so it survives serialization later
 * (we don't have a real DB yet, but this lets us swap to Room/DataStore
 * cleanly when logic lands in the next phase).
 *
 * Per-category routeSlug (used in navigation routes like
 * `spin/artists` or `reveal/authors/{name}`) is declared in [routeSlug]
 * below.
 */
enum class CategoryId {
    ARTISTS,
    ALBUMS,
    DIRECTORS,
    FILMS,
    AUTHORS,
    BOOKS,
    PAINTERS,
    ARTWORKS,
    SCIENTISTS,
    DISCOVERIES,
    SONGS,
    SERIES,
    ANIME,
    MANGA,
    MANHWA,
    GAMES,
    MYTHOLOGY,
    SPORTS,
    FOOD,
    INTERNET,
    // ── v27i — 15 new lanes (content expansion pass, authored in batches) ──
    BIOLOGY,
    CHEMISTRY,
    ANIMALS,
    PLANTS,
    TECHNOLOGIES,
    ASTRONOMY,
    HISTORY,
    GEOLOGY,
    MEDICINE,
    PSYCHOLOGY,
    MATHEMATICS,
    ECONOMICS,
    LANGUAGE,
    ENGINEERING,
    OCEANS,
    WILDCARD;

    companion object {
        fun fromRoute(slug: String): CategoryId? =
            values().firstOrNull { it.routeSlug == slug }

        /**
         * v27i — the 15 lanes added in the content-expansion pass. The
         * category picker splits its deck grid into the original 21 lanes
         * and these new lanes (a swipeable second page).
         */
        val newLanes: Set<CategoryId> = setOf(
            BIOLOGY, CHEMISTRY, ANIMALS, PLANTS, TECHNOLOGIES,
            ASTRONOMY, HISTORY, GEOLOGY, MEDICINE, PSYCHOLOGY,
            MATHEMATICS, ECONOMICS, LANGUAGE, ENGINEERING, OCEANS
        )

        /** Default chip order on Home + Category Picker. Wildcard stays last. */
        val defaultOrder: List<CategoryId> = listOf(
            ARTISTS, ALBUMS, SONGS,
            DIRECTORS, FILMS, SERIES,
            AUTHORS, BOOKS,
            PAINTERS, ARTWORKS,
            SCIENTISTS, DISCOVERIES,
            ANIME, MANGA, MANHWA,
            GAMES,
            MYTHOLOGY,
            SPORTS,
            FOOD,
            INTERNET,
            BIOLOGY, CHEMISTRY, ANIMALS, PLANTS, TECHNOLOGIES,
            ASTRONOMY, HISTORY, GEOLOGY, MEDICINE, PSYCHOLOGY,
            MATHEMATICS, ECONOMICS, LANGUAGE, ENGINEERING, OCEANS,
            WILDCARD
        )
    }

    /** URL-safe slug used in navigation routes (`spin/artists`, `picker/from-home`). */
    val routeSlug: String get() = when (this) {
        ARTISTS     -> "artists"
        ALBUMS      -> "albums"
        DIRECTORS   -> "directors"
        FILMS       -> "films"
        AUTHORS     -> "authors"
        BOOKS       -> "books"
        PAINTERS    -> "painters"
        ARTWORKS    -> "artworks"
        SCIENTISTS  -> "scientists"
        DISCOVERIES -> "discoveries"
        SONGS       -> "songs"
        SERIES      -> "series"
        ANIME       -> "anime"
        MANGA       -> "manga"
        MANHWA      -> "manhwa"
        GAMES       -> "games"
        MYTHOLOGY   -> "mythology"
        SPORTS      -> "sports"
        FOOD        -> "food"
        INTERNET    -> "internet"
        BIOLOGY     -> "biology"
        CHEMISTRY   -> "chemistry"
        ANIMALS     -> "animals"
        PLANTS      -> "plants"
        TECHNOLOGIES -> "technologies"
        ASTRONOMY   -> "astronomy"
        HISTORY     -> "history"
        GEOLOGY     -> "geology"
        MEDICINE    -> "medicine"
        PSYCHOLOGY  -> "psychology"
        MATHEMATICS -> "mathematics"
        ECONOMICS   -> "economics"
        LANGUAGE    -> "language"
        ENGINEERING -> "engineering"
        OCEANS      -> "oceans"
        WILDCARD    -> "wildcard"
    }
}

/**
 * A logical "family" for grouping the 11 categories into 6 visual
 * themes (used for color tinting + Wildcard pool composition).
 *
 * Within a family, sub-categories share the same accent color (e.g.
 * Artists + Albums both use indigo) so the user intuitively reads them
 * as "related domains" even though they're independent chips.
 */
enum class CategoryFamily {
    MUSIC,
    MOVIES,
    BOOKS,
    VISUAL_ART,
    SCIENCE,
    ANIME_COMICS,
    GAMES,
    MYTHOLOGY,
    SPORTS,
    FOOD,
    INTERNET,
    WILDCARD;

    companion object {
        fun of(id: CategoryId): CategoryFamily = when (id) {
            CategoryId.ARTISTS, CategoryId.ALBUMS, CategoryId.SONGS -> MUSIC
            CategoryId.DIRECTORS, CategoryId.FILMS, CategoryId.SERIES -> MOVIES
            CategoryId.AUTHORS, CategoryId.BOOKS -> BOOKS
            CategoryId.PAINTERS, CategoryId.ARTWORKS -> VISUAL_ART
            CategoryId.SCIENTISTS, CategoryId.DISCOVERIES -> SCIENCE
            CategoryId.ANIME, CategoryId.MANGA, CategoryId.MANHWA -> ANIME_COMICS
            CategoryId.GAMES -> GAMES
            CategoryId.MYTHOLOGY -> MYTHOLOGY
            CategoryId.SPORTS -> SPORTS
            CategoryId.FOOD -> FOOD
            CategoryId.INTERNET -> INTERNET
            // v27i — the STEM-heavy new lanes join the SCIENCE family (they
            // share the science tint story + wildcard pool); the wordy ones
            // join BOOKS. No new family values — CurioIcons' exhaustive
            // when(family) glyph map stays untouched.
            CategoryId.BIOLOGY, CategoryId.CHEMISTRY, CategoryId.ANIMALS,
            CategoryId.PLANTS, CategoryId.ASTRONOMY, CategoryId.GEOLOGY,
            CategoryId.MEDICINE, CategoryId.PSYCHOLOGY, CategoryId.MATHEMATICS,
            CategoryId.OCEANS, CategoryId.TECHNOLOGIES, CategoryId.ENGINEERING -> SCIENCE
            CategoryId.HISTORY, CategoryId.LANGUAGE, CategoryId.ECONOMICS -> BOOKS
            CategoryId.WILDCARD -> WILDCARD
        }
    }
}

/**
 * A category as rendered to the user — accent color, glyph, display name.
 * Visibility (hidden via §13.4 Manage Categories) is part of the state layer
 * but defaults to visible here.
 *
 * Two boolean flags, with different owners:
 *
 * - `isHidden` — **user-controlled**, set via Settings → Manage Categories
 *   (§13.4). When true, the category is filtered out of the Home chip row,
 *   Category Picker, and Cabinet filter chips. Past entries in hidden
 *   categories are preserved — they reappear the moment the user re-enables
 *   the category. Defaults to `false`.
 *
 * - `isReady` — **data-layer-controlled**, set when 100+ topics are authored
 *   + reviewed per the category visibility spec. When false, the
 *   category is filtered out of the chip row + Picker and surfaces as a
 *   "Coming soon" empty-state slot. Defaults to `false`; never flip to
 *   `true` without a corresponding data drop in `assets/topics/{id}.json`.
 *
 * The two flags are independent — a category can be `isReady = true` (data
 * shipped) and `isHidden = true` (user hid it) at the same time.
 *
 * @property family Which logical family this category belongs to (drives
 *   Wildcard pool composition + visual grouping).
 * @property defaultFormat Which capture body to render on Save/Capture
 *   for this category. The 6 format bodies (Sound Bite / Reel Notes /
 *   Marginalia / Gallery Wall / Field Notes / Open Notebook) are reused
 *   across categories — see [CaptureFormat].
 * @property accent Deep fill color for cards, chips and buttons — white
 *   content stays >= 4.5:1 on every accent (researched Tailwind-700 set).
 * @property lightAccent Light 300-level twin of [accent] for accent-colored
 *   text/icons on dark surfaces — resolved via [com.curio.app.ui.theme.categoryInk].
 */
data class CurioCategory(
    val id: CategoryId,
    val displayName: String,
    val accent: Color,
    val tint: Color,
    val iconGlyph: String,
    val family: CategoryFamily,
    val defaultFormat: CaptureFormat,
    val isHidden: Boolean = false,
    val isReady: Boolean = false,
    // Kept LAST so positional constructions never shift the mid-constructor
    // defaults (all current call sites use named args; appending keeps that safe).
    val lightAccent: Color = accent
)

/**
 * The canonical 11 Curio categories in the default order.
 *
 * Display names are resolved by the calling screen from string resources so
 * they can be localised; this list uses English placeholders so the
 * placeholder-phase previews have something to render. Once a string resource
 * pass lands these will become resource IDs.
 *
 * Capture format mapping:
 *   ARTISTS     → SoundBite    (voice note about the artist / one track)
 *   ALBUMS      → ReelNotes    (review + tracklist + optional rating)
 *   DIRECTORS   → ReelNotes    (review of a director's body of work)
 *   FILMS       → Marginalia   (film journal + favorite quote/scene cards)
 *   AUTHORS     → Marginalia   (author bio + favorite quote from their work)
 *   BOOKS       → Marginalia   (book journal + quote cards)
 *   PAINTERS    → GalleryWall  (moodboard of paintings)
 *   ARTWORKS    → GalleryWall  (single artwork deep-dive)
 *   SCIENTISTS  → FieldNotes   (observed / surprised / next)
 *   DISCOVERIES → FieldNotes   (what was discovered + why it matters)
 *   WILDCARD    → OpenNotebook (pick your own format from the 5 above)
 */
object CurioCategories {

    val all: List<CurioCategory> = listOf(
        // ── Music family (Indigo) ───────────────────────────────────────
        CurioCategory(
            id            = CategoryId.ARTISTS,
            displayName   = "Artists",
            accent        = CurioColors.CategoryIndigo,
            lightAccent   = CurioColors.CategoryIndigoInk,
            tint          = CurioColors.CategoryIndigoTint,
            iconGlyph     = "person",
            family        = CategoryFamily.MUSIC,
            defaultFormat = CaptureFormat.SoundBite
        ),
        CurioCategory(
            id            = CategoryId.ALBUMS,
            displayName   = "Albums",
            accent        = CurioColors.CategoryAlbum,
            lightAccent   = CurioColors.CategoryAlbumInk,
            tint          = CurioColors.CategoryAlbumTint,
            iconGlyph     = "album",
            family        = CategoryFamily.MUSIC,
            defaultFormat = CaptureFormat.ReelNotes
        ),
        // ── Movies family (Rose) ────────────────────────────────────────
        CurioCategory(
            id            = CategoryId.DIRECTORS,
            displayName   = "Directors",
            accent        = CurioColors.CategoryRose,
            lightAccent   = CurioColors.CategoryRoseInk,
            tint          = CurioColors.CategoryRoseTint,
            iconGlyph     = "videocam",
            family        = CategoryFamily.MOVIES,
            defaultFormat = CaptureFormat.ReelNotes
        ),
        CurioCategory(
            id            = CategoryId.FILMS,
            displayName   = "Films",
            accent        = CurioColors.CategoryRose,
            lightAccent   = CurioColors.CategoryRoseInk,
            tint          = CurioColors.CategoryRoseTint,
            iconGlyph     = "movie",
            family        = CategoryFamily.MOVIES,
            defaultFormat = CaptureFormat.Marginalia
        ),
        // ── Books family (Amber) ────────────────────────────────────────
        CurioCategory(
            id            = CategoryId.AUTHORS,
            displayName   = "Authors",
            accent        = CurioColors.CategoryAmber,
            lightAccent   = CurioColors.CategoryAmberInk,
            tint          = CurioColors.CategoryAmberTint,
            iconGlyph     = "edit_note",
            family        = CategoryFamily.BOOKS,
            defaultFormat = CaptureFormat.Marginalia
        ),
        CurioCategory(
            id            = CategoryId.BOOKS,
            displayName   = "Books",
            accent        = CurioColors.CategoryAmber,
            lightAccent   = CurioColors.CategoryAmberInk,
            tint          = CurioColors.CategoryAmberTint,
            iconGlyph     = "menu_book",
            family        = CategoryFamily.BOOKS,
            defaultFormat = CaptureFormat.Marginalia
        ),
        // ── Visual Art family (Teal) ────────────────────────────────────
        CurioCategory(
            id            = CategoryId.PAINTERS,
            displayName   = "Painters",
            accent        = CurioColors.CategoryTeal,
            lightAccent   = CurioColors.CategoryTealInk,
            tint          = CurioColors.CategoryTealTint,
            iconGlyph     = "brush",
            family        = CategoryFamily.VISUAL_ART,
            defaultFormat = CaptureFormat.GalleryWall
        ),
        CurioCategory(
            id            = CategoryId.ARTWORKS,
            displayName   = "Artworks",
            accent        = CurioColors.CategoryTeal,
            lightAccent   = CurioColors.CategoryTealInk,
            tint          = CurioColors.CategoryTealTint,
            iconGlyph     = "palette",
            family        = CategoryFamily.VISUAL_ART,
            defaultFormat = CaptureFormat.GalleryWall
        ),
        // ── Science family (Sky) ────────────────────────────────────────
        CurioCategory(
            id            = CategoryId.SCIENTISTS,
            displayName   = "Scientists",
            accent        = CurioColors.CategorySky,
            lightAccent   = CurioColors.CategorySkyInk,
            tint          = CurioColors.CategorySkyTint,
            iconGlyph     = "science",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        CurioCategory(
            id            = CategoryId.DISCOVERIES,
            displayName   = "Discoveries",
            accent        = CurioColors.CategorySky,
            lightAccent   = CurioColors.CategorySkyInk,
            tint          = CurioColors.CategorySkyTint,
            iconGlyph     = "lightbulb",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        // ── Music family (Indigo) — Songs joins Artists/Albums ───────────
        CurioCategory(
            id            = CategoryId.SONGS,
            displayName   = "Songs",
            accent        = CurioColors.CategorySong,
            lightAccent   = CurioColors.CategorySongInk,
            tint          = CurioColors.CategorySongTint,
            iconGlyph     = "queue_music",
            family        = CategoryFamily.MUSIC,
            defaultFormat = CaptureFormat.SoundBite
        ),
        // ── Movies family (Rose) — Series joins Directors/Films ──────────
        CurioCategory(
            id            = CategoryId.SERIES,
            displayName   = "Series",
            accent        = CurioColors.CategorySeries,
            lightAccent   = CurioColors.CategorySeriesInk,
            tint          = CurioColors.CategorySeriesTint,
            iconGlyph     = "tv",
            family        = CategoryFamily.MOVIES,
            defaultFormat = CaptureFormat.Marginalia
        ),
        // ── Anime & Comics family (Violet) ───────────────────────────────
        CurioCategory(
            id            = CategoryId.ANIME,
            displayName   = "Anime",
            accent        = CurioColors.CategoryViolet,
            lightAccent   = CurioColors.CategoryVioletInk,
            tint          = CurioColors.CategoryVioletTint,
            iconGlyph     = "smart_display",
            family        = CategoryFamily.ANIME_COMICS,
            defaultFormat = CaptureFormat.Marginalia
        ),
        CurioCategory(
            id            = CategoryId.MANGA,
            displayName   = "Manga",
            accent        = CurioColors.CategoryManga,
            lightAccent   = CurioColors.CategoryMangaInk,
            tint          = CurioColors.CategoryMangaTint,
            iconGlyph     = "auto_stories",
            family        = CategoryFamily.ANIME_COMICS,
            defaultFormat = CaptureFormat.Marginalia
        ),
        CurioCategory(
            id            = CategoryId.MANHWA,
            displayName   = "Manhwa",
            accent        = CurioColors.CategoryManhwa,
            lightAccent   = CurioColors.CategoryManhwaInk,
            tint          = CurioColors.CategoryManhwaTint,
            iconGlyph     = "import_contacts",
            family        = CategoryFamily.ANIME_COMICS,
            defaultFormat = CaptureFormat.Marginalia
        ),
        // ── Games family (Fuchsia) ───────────────────────────────────────
        CurioCategory(
            id            = CategoryId.GAMES,
            displayName   = "Games",
            accent        = CurioColors.CategoryFuchsia,
            lightAccent   = CurioColors.CategoryFuchsiaInk,
            tint          = CurioColors.CategoryFuchsiaTint,
            iconGlyph     = "sports_esports",
            family        = CategoryFamily.GAMES,
            defaultFormat = CaptureFormat.ReelNotes
        ),
        // ── Mythology family (Orange) ────────────────────────────────────
        CurioCategory(
            id            = CategoryId.MYTHOLOGY,
            displayName   = "Mythology",
            accent        = CurioColors.CategoryOrange,
            lightAccent   = CurioColors.CategoryOrangeInk,
            tint          = CurioColors.CategoryOrangeTint,
            iconGlyph     = "auto_awesome",
            family        = CategoryFamily.MYTHOLOGY,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        // ── Sports family (Emerald) ──────────────────────────────────────
        CurioCategory(
            id            = CategoryId.SPORTS,
            displayName   = "Sports",
            accent        = CurioColors.CategoryEmerald,
            lightAccent   = CurioColors.CategoryEmeraldInk,
            tint          = CurioColors.CategoryEmeraldTint,
            iconGlyph     = "sports_soccer",
            family        = CategoryFamily.SPORTS,
            defaultFormat = CaptureFormat.ReelNotes
        ),
        // ── Food family (Red) ────────────────────────────────────────────
        CurioCategory(
            id            = CategoryId.FOOD,
            displayName   = "Food",
            accent        = CurioColors.CategoryRed,
            lightAccent   = CurioColors.CategoryRedInk,
            tint          = CurioColors.CategoryRedTint,
            iconGlyph     = "restaurant",
            family        = CategoryFamily.FOOD,
            defaultFormat = CaptureFormat.GalleryWall
        ),
        // ── Internet culture family (Blue) ───────────────────────────────
        CurioCategory(
            id            = CategoryId.INTERNET,
            displayName   = "Internet",
            accent        = CurioColors.CategoryBlue,
            lightAccent   = CurioColors.CategoryBlueInk,
            tint          = CurioColors.CategoryBlueTint,
            iconGlyph     = "public",
            family        = CategoryFamily.INTERNET,
            defaultFormat = CaptureFormat.OpenNotebook
        ),
        // ── v27i — 15 new lanes (content expansion pass) ────────────────
        CurioCategory(
            id            = CategoryId.BIOLOGY,
            displayName   = "Biology",
            accent        = CurioColors.CategoryGreen,
            lightAccent   = CurioColors.CategoryGreenInk,
            tint          = CurioColors.CategoryGreenTint,
            iconGlyph     = "science",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes,
            isReady       = true  // v27i — 159 topics shipped in assets/topics/biology.json
        ),
        CurioCategory(
            id            = CategoryId.CHEMISTRY,
            displayName   = "Chemistry",
            accent        = CurioColors.CategoryLime,
            lightAccent   = CurioColors.CategoryLimeInk,
            tint          = CurioColors.CategoryLimeTint,
            iconGlyph     = "colorize",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes,
            isReady       = true  // v27i — 577 topics shipped in assets/topics/chemistry.json
        ),
        CurioCategory(
            id            = CategoryId.ANIMALS,
            displayName   = "Animals",
            accent        = CurioColors.CategoryBrown,
            lightAccent   = CurioColors.CategoryBrownInk,
            tint          = CurioColors.CategoryBrownTint,
            iconGlyph     = "pets",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes,
            isReady       = true  // v27i — 1016 topics shipped in assets/topics/animals.json
        ),
        CurioCategory(
            id            = CategoryId.PLANTS,
            displayName   = "Plants",
            accent        = CurioColors.CategoryForest,
            lightAccent   = CurioColors.CategoryForestInk,
            tint          = CurioColors.CategoryForestTint,
            iconGlyph     = "landscape",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes,
            isReady       = true  // v27i — 1030 topics shipped in assets/topics/plants.json
        ),
        CurioCategory(
            id            = CategoryId.TECHNOLOGIES,
            displayName   = "Technologies",
            accent        = CurioColors.CategorySlate,
            lightAccent   = CurioColors.CategorySlateInk,
            tint          = CurioColors.CategorySlateTint,
            iconGlyph     = "smart_display",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        CurioCategory(
            id            = CategoryId.ASTRONOMY,
            displayName   = "Astronomy",
            accent        = CurioColors.CategoryNavy,
            lightAccent   = CurioColors.CategoryNavyInk,
            tint          = CurioColors.CategoryNavyTint,
            iconGlyph     = "dark_mode",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        CurioCategory(
            id            = CategoryId.HISTORY,
            displayName   = "History",
            accent        = CurioColors.CategorySepia,
            lightAccent   = CurioColors.CategorySepiaInk,
            tint          = CurioColors.CategorySepiaTint,
            iconGlyph     = "history",
            family        = CategoryFamily.BOOKS,
            defaultFormat = CaptureFormat.Marginalia,
            isReady       = true  // v27i — 117 topics shipped in assets/topics/history.json
        ),
        CurioCategory(
            id            = CategoryId.GEOLOGY,
            displayName   = "Geology",
            accent        = CurioColors.CategoryStone,
            lightAccent   = CurioColors.CategoryStoneInk,
            tint          = CurioColors.CategoryStoneTint,
            iconGlyph     = "layers",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        CurioCategory(
            id            = CategoryId.MEDICINE,
            displayName   = "Medicine",
            accent        = CurioColors.CategoryCrimson,
            lightAccent   = CurioColors.CategoryCrimsonInk,
            tint          = CurioColors.CategoryCrimsonTint,
            iconGlyph     = "self_improvement",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        CurioCategory(
            id            = CategoryId.PSYCHOLOGY,
            displayName   = "Psychology",
            accent        = CurioColors.CategoryPeriwinkle,
            lightAccent   = CurioColors.CategoryPeriwinkleInk,
            tint          = CurioColors.CategoryPeriwinkleTint,
            iconGlyph     = "psychology",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        CurioCategory(
            id            = CategoryId.MATHEMATICS,
            displayName   = "Mathematics",
            accent        = CurioColors.CategoryIndigoBlue,
            lightAccent   = CurioColors.CategoryIndigoBlueInk,
            tint          = CurioColors.CategoryIndigoBlueTint,
            iconGlyph     = "equalizer",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        CurioCategory(
            id            = CategoryId.ECONOMICS,
            displayName   = "Economics",
            accent        = CurioColors.CategoryGold,
            lightAccent   = CurioColors.CategoryGoldInk,
            tint          = CurioColors.CategoryGoldTint,
            iconGlyph     = "public",
            family        = CategoryFamily.BOOKS,
            defaultFormat = CaptureFormat.Marginalia
        ),
        CurioCategory(
            id            = CategoryId.LANGUAGE,
            displayName   = "Language",
            accent        = CurioColors.CategoryTeal600,
            lightAccent   = CurioColors.CategoryTeal600Ink,
            tint          = CurioColors.CategoryTeal600Tint,
            iconGlyph     = "format_quote",
            family        = CategoryFamily.BOOKS,
            defaultFormat = CaptureFormat.Marginalia
        ),
        CurioCategory(
            id            = CategoryId.ENGINEERING,
            displayName   = "Engineering",
            accent        = CurioColors.CategoryZinc,
            lightAccent   = CurioColors.CategoryZincInk,
            tint          = CurioColors.CategoryZincTint,
            iconGlyph     = "settings",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        CurioCategory(
            id            = CategoryId.OCEANS,
            displayName   = "Oceans",
            accent        = CurioColors.CategoryDeepCyan,
            lightAccent   = CurioColors.CategoryDeepCyanInk,
            tint          = CurioColors.CategoryDeepCyanTint,
            iconGlyph     = "bubble_chart",
            family        = CategoryFamily.SCIENCE,
            defaultFormat = CaptureFormat.FieldNotes
        ),
        // ── Wildcard (brand coral; cards use the themed coral gradient) ──
        CurioCategory(
            id            = CategoryId.WILDCARD,
            displayName   = "Wildcard",
            accent        = CurioColors.CategoryCoral,  // brand primary; cards use the themed gradient
            lightAccent   = CurioColors.CategoryCoralInk,
            tint          = CurioColors.CategoryCoralTint,
            iconGlyph     = "casino",
            family        = CategoryFamily.WILDCARD,
            defaultFormat = CaptureFormat.OpenNotebook
        )
    )

    init {
        // Fail at app startup (not deep inside a screen mid-session) if the
        // data layer drifts out of sync with the CategoryId enum. Without
        // this, a new CategoryId value added without a matching entry in
        // `all` would scatter NoSuchElementExceptions across every screen.
        check(all.size == CategoryId.values().size) {
            "CurioCategories.all has ${all.size} entries but CategoryId " +
            "has ${CategoryId.values().size} values; keep them in sync."
        }
    }

    /**
     * Returns the category for [id].
     *
     * The return type is non-nullable because [CategoryId.values()] is
     * 1:1 covered by [all] (verify at startup via the [init] block) — no
     * valid [CategoryId] can be missing a category entry. An unknown id is
     * a bug (new enum value added without a matching entry in `all`), and
     * we want to crash LOUDLY with a descriptive message rather than
     * silently render an unstyled UI or scatter opaque
     * `NoSuchElementException`s across screens.
     *
     * **Maintenance contract (important):** when adding a new [CategoryId]
     * value, you MUST also add a matching entry to `all` in the SAME commit.
     * The [init] check above will turn this from a runtime crash into a
     * startup-time crash message that names the mismatch directly.
     *
     * If you genuinely need "unknown id" handling (e.g. when parsing a route
     * slug from user input or external JSON), use [byRouteSlug] instead —
     * it stays nullable for that reason.
     */
    fun byId(id: CategoryId): CurioCategory =
        all.firstOrNull { it.id == id }
            ?: error(
                "CurioCategories.all has no entry for CategoryId.${id.name}. " +
                "Add a matching CurioCategory(...) to `all` so the count " +
                "matches CategoryId.values().size (currently " +
                "${CategoryId.values().size})."
            )

    /** Returns the category whose routeSlug matches [slug], or null. */
    fun byRouteSlug(slug: String): CurioCategory? =
        all.firstOrNull { it.id.routeSlug == slug }

    /**
     * Visible-only list — for Home/Cabinet chip rows, the Category Picker,
     * and the Spin category sheet. v7.94 — this is now a REACTIVE getter
     * backed by the persisted Manage Categories state in
     * [com.curio.app.data.AppPreferences]: the user's hidden set is
     * filtered out and the custom order is applied (falling back to the
     * default order when none is saved). Reads [AppPreferences.hiddenCategoriesState]
     * / [AppPreferences.categoryOrderState] inside composition, so screens
     * recompose the moment a category is hidden/shown or reordered.
     * (The old `isHidden` field on [CurioCategory] remains as the
     * data-layer default — always false — and is superseded by the
     * persisted user state.)
     */
    val visible: List<CurioCategory>
        get() {
            val hidden = AppPreferences.hiddenCategoriesState
            val order = AppPreferences.categoryOrderState
            val base = if (order.isEmpty()) {
                all
            } else {
                order.mapNotNull { id -> all.firstOrNull { it.id == id } } +
                    all.filter { it.id !in order }
            }
            // v27i — new lanes stay out of the chip rows / Spin sheet / Cabinet
            // filters until their topic content actually ships (isReady = true,
            // set on the CurioCategory entry when assets/topics/{slug}.json
            // reaches 100+ topics). They only surface as "Coming soon" tiles on
            // the Category Picker's New-lanes page, which reads `all` directly
            // instead of `visible`.
            return base
                .filterNot { it.id in hidden }
                .filterNot { it.id in CategoryId.newLanes && !it.isReady }
        }

    /** Returns all categories in the given [family], in default order. */
    fun byFamily(family: CategoryFamily): List<CurioCategory> =
        all.filter { it.family == family }
}
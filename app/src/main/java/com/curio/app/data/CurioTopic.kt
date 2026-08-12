package com.curio.app.data

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The unified Curio topic schema.
 *
 * Topics are loaded from `assets/topics/{categoryId}.json` at runtime via
 * [TopicJsonLoader]. The JSON schema mirrors this data class 1:1 — see
 * [TopicJsonLoader] for the deserialization code.
 *
 * The previous `musicGenre` field (an enum) has been replaced with a generic
 * `tags: List<String>` field. This lets the Spin screen dynamically
 * generate filter chips for *any* category (e.g. genres for Artists,
 * eras for Films, mediums for Painters) without hardcoding enums. The
 * quality of curation lives in the JSON, not in code.
 *
 * Consumers: SpinScreen, TopicRevealScreen, SaveCaptureScreen,
 * CabinetScreen, EntryDetailScreen, TopicHistoryScreen, LightboxScreen.
 *
 * @property tags Free-form string tags for the Spin screen's dynamic
 *   filter chip row. Tags are category-specific: Artists might use
 *   ["Rock", "1970s"], Films might use ["Drama", "1990s"], Painters
 *   might use ["Impressionism", "Oil"]. Empty list = no filters.
 * @property tier Quality tier for the random picker. 1 = human-curated
 *   marquee (highest quality, surfaces most often). 2+ = AI-generated
 *   long tail (still good, just less hand-tended). Default = 1 so all
 *   loaded topics are presumed marquee unless tagged otherwise.
 */
data class CurioTopic(
    val id: String,
    val categoryId: CategoryId,
    /** "Album" / "Artist" / "Movie" / "Director" / "Painting" / "Movement" / "Book" / "Author" / "Field" / etc. */
    val subtype: String,
    val name: String,
    /** 1–2 sentence intriguing teaser (per CURIO_SPEC §6). */
    val teaser: String,
    /** Future image URL — empty string for placeholder phase. */
    val imageUrl: String,
    val exploreAction: ExploreAction,
    /** Free-form tags for dynamic Spin filter chips. Empty = no filters. */
    val tags: List<String> = emptyList(),
    /** Quality tier (1 = marquee, 2+ = long tail). */
    val tier: Int = 1,
    /**
     * Creator byline shown as a tag on the Topic Reveal hero card
     * ("The Beatles", "George Orwell", "Christopher Nolan").
     * Albums = artist, Books = author, Films = director, Artworks =
     * painter. Blank = no byline pill. Optional — defaults to "" so
     * legacy JSON and hand-built topics need no migration.
     */
    val byline: String = ""
) {
    init {
        require(id.isNotBlank()) { "CurioTopic id must not be blank." }
        require(name.isNotBlank()) { "CurioTopic name must not be blank." }
        require(teaser.isNotBlank()) { "CurioTopic teaser must not be blank for '$id'." }
        require(tier in 1..3) {
            "CurioTopic tier must be 1, 2, or 3 (got $tier for '$id')."
        }
    }
}

/**
 * What the user should DO with the topic — per CURIO_SPEC §6 ("the
 * Explore Action" / "scratchpad area").
 *
 * Concretely this surfaces on TopicRevealScreen as the "Listen to /
 * Watch / Read / Look at / Explore ..." prompt with a concrete target
 * (album name, film name, museum collection, etc.) and a one-paragraph
 * instruction on what to look for.
 */
data class ExploreAction(
    val verb: String,
    val targetName: String,
    val durationMinutes: Int,
    val instruction: String
)

/**
 * One captured entry shown in the Cabinet grid + EntryDetail.
 *
 * Phase 4 wires real Room persistence via the same shape; Phase 0 uses
 * [TopicCatalog.sampleEntries] as the visual mock.
 *
 * @property format Which capture format the entry used (Sound Bite,
 *   Reel Notes, Marginalia, Gallery Wall, Field Notes, or Open
 *   Notebook for Wildcard). Drives the EntryDetail render body.
 * @property bodyPreview One-line preview shown on the Cabinet card.
 * @property bodyContent Multi-line content shown on EntryDetail.
 *   For Sound Bite format, this is a transcript-style caption
 *   (real audio lands with the asset pipeline phase).
 */
data class CurioEntry(
    val id: String,
    val topic: CurioTopic,
    val format: CaptureFormat,
    val captureData: CaptureData,
    val title: String? = null,
    val capturedAtMillis: Long = System.currentTimeMillis(),
    /**
     * v17 — how long the user explored this topic before saving (the
     * explore session's pause-aware elapsed time at save). 0 = no session
     * was recorded (imports, samples, older entries) — the UI hides the
     * label then.
     */
    val sessionTimeMillis: Long = 0L,
    /**
     * Free-form user tags added on the save page (v7.17) — searchable in
     * the Cabinet and shown as chips on the entry detail page. Stored in
     * Room's `tagsJson` column; legacy entries default to empty.
     */
    val tags: List<String> = emptyList(),
    /**
     * Explicit provenance marker. This is set by the FieldMind restore path
     * and persisted separately from the synthetic topic/category used to
     * render the imported entry.
     */
    val isLegacy: Boolean = false,
    /**
     * v26 — recycle bin: when non-null, this entry sits in the recycle bin
     * (deleted at that timestamp) instead of the Cabinet. Set by the DAO's
     * soft-delete; the recycle bin screen shows it with a restore/purge.
     */
    val deletedAt: Long? = null
) {
    /** One-line preview for Cabinet cards. */
    val bodyPreview: String get() = captureData.toPreview()
    /** Full multi-line content for EntryDetail. */
    val bodyContent: String get() = captureData.toFullContent()
    /**
     * Calendar days since capture in the device's local timezone (for display).
     * Comparing calendar dates instead of elapsed 24-hour blocks keeps a
     * capture from yesterday labeled "Yesterday" immediately after midnight.
     */
    val capturedAtDaysAgo: Int get() {
        val zone = ZoneId.systemDefault()
        val capturedDate = Instant.ofEpochMilli(capturedAtMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(capturedDate, today).toInt().coerceAtLeast(0)
    }

    /** True when this entry was explicitly restored from FieldMind. */
}

/**
 * The six capture formats from Curio design contract section 8.
 *
 * 11 categories map onto these 6 format bodies — see
 * [CurioCategories] for the mapping. Two categories from the same
 * family share a format when the capture experience is similar
 * (Artists → SoundBite, Albums → ReelNotes; Films + Authors + Books
 * all share Marginalia since reading journals work the same way).
 */
enum class CaptureFormat {
    SoundBite,    // Voice note
    ReelNotes,    // Review + collage
    Marginalia,   // Journal + quotes
    GalleryWall,  // Moodboard
    FieldNotes,   // 3-section report
    OpenNotebook  // Wildcard: pick your own format
}

/**
 * Short display name for a [CaptureFormat] — used by the universal format
 * picker chips, the detail-page section switcher, and [CaptureData.toPreview].
 * UI-safe (pure string, no icons).
 */
val CaptureFormat.shortName: String
    get() = when (this) {
        CaptureFormat.SoundBite -> "Voice"
        CaptureFormat.ReelNotes -> "Review"
        CaptureFormat.Marginalia -> "Journal"
        CaptureFormat.GalleryWall -> "Moodboard"
        CaptureFormat.FieldNotes -> "Field notes"
        CaptureFormat.OpenNotebook -> "Wildcard"
    }